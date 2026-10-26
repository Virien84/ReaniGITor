package io.github.virien84.reanigitor.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.virien84.reanigitor.Text;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A read-only client for the public GitHub REST API. It only ever sends GET requests: there is
 * no code path that writes, archives, deletes or comments on anything.
 */
public final class GitHubClient {

    public static final URI DEFAULT_API = URI.create("https://api.github.com/");

    // GitHub rules: 1-39 alphanumerics or single hyphens, no hyphen at either end.
    private static final Pattern USERNAME =
            Pattern.compile("[A-Za-z0-9](?:[A-Za-z0-9]|-(?=[A-Za-z0-9])){0,38}");
    private static final Pattern FULL_NAME = Pattern.compile("[A-Za-z0-9-]+/[A-Za-z0-9._-]+");

    private static final String JSON = "application/vnd.github+json";
    private static final String RAW = "application/vnd.github.raw+json";
    private static final int PER_PAGE = 100;
    private static final int MAX_PAGES = 10;
    private static final int MAX_ISSUES = 5;

    static final int README_CHARS = 3000;
    static final int DESCRIPTION_CHARS = 300;
    static final int COMMIT_CHARS = 200;
    static final int ISSUE_CHARS = 120;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper json = new ObjectMapper();
    private final URI api;
    private final String token;

    /** @param token optional; null means anonymous access (60 requests per hour) */
    public GitHubClient(URI api, String token) {
        this.api = api;
        this.token = token == null || token.isBlank() ? null : token.strip();
    }

    public static boolean isValidUsername(String user) {
        return user != null && USERNAME.matcher(user).matches();
    }

    /** All public repos owned by {@code user}, most recently pushed first. */
    public List<Repo> listRepos(String user) {
        if (!isValidUsername(user)) {
            throw new IllegalArgumentException("Not a valid GitHub username: " + user);
        }
        List<Repo> repos = new ArrayList<>();
        for (int page = 1; page <= MAX_PAGES; page++) {
            String path = "users/" + user + "/repos?type=owner&sort=pushed&per_page=" + PER_PAGE + "&page=" + page;
            JsonNode array = getJson(path)
                    .orElseThrow(() -> new GitHubException("No GitHub user named " + user + "."));
            array.forEach(node -> repos.add(toRepo(node)));
            if (array.size() < PER_PAGE) {
                break;
            }
        }
        return repos;
    }

    /** Digs up the README, the last commit and the open issues of a repo. */
    public Remains exhume(Repo repo) {
        String fullName = repo.fullName();
        if (!FULL_NAME.matcher(fullName).matches()) {
            throw new IllegalArgumentException("Unexpected repo name: " + fullName);
        }
        String readme = get("repos/" + fullName + "/readme", RAW)
                .map(body -> Text.clean(body, README_CHARS))
                .orElse(null);

        String lastCommitMessage = null;
        Instant lastCommitAt = null;
        JsonNode commits = getJson("repos/" + fullName + "/commits?per_page=1").orElse(null);
        if (commits != null && commits.size() > 0) {
            JsonNode commit = commits.get(0).path("commit");
            lastCommitMessage = Text.firstLine(commit.path("message").asText(null), COMMIT_CHARS);
            lastCommitAt = instant(commit.path("committer").path("date"));
        }

        List<String> issues = new ArrayList<>();
        if (repo.openIssues() > 0) {
            getJson("repos/" + fullName + "/issues?state=open&per_page=" + MAX_ISSUES).ifPresent(array -> {
                for (JsonNode issue : array) {
                    String title = Text.firstLine(issue.path("title").asText(null), ISSUE_CHARS);
                    if (title != null) {
                        issues.add(issue.has("pull_request") ? "PR: " + title : title);
                    }
                }
            });
        }
        return new Remains(repo, readme, lastCommitMessage, lastCommitAt, issues);
    }

    private Repo toRepo(JsonNode n) {
        List<String> topics = new ArrayList<>();
        n.path("topics").forEach(t -> {
            String topic = Text.clean(t.asText(), 50);
            if (topic != null) {
                topics.add(topic);
            }
        });
        String license = n.path("license").path("spdx_id").asText(null);
        return new Repo(
                Text.clean(n.path("name").asText(), 100),
                n.path("full_name").asText(),
                Text.clean(n.path("description").asText(null), DESCRIPTION_CHARS),
                n.path("html_url").asText(null),
                Text.clean(n.path("language").asText(null), 50),
                n.path("stargazers_count").asInt(),
                n.path("forks_count").asInt(),
                n.path("open_issues_count").asInt(),
                instant(n.path("created_at")),
                instant(n.path("pushed_at")),
                n.path("fork").asBoolean(),
                n.path("archived").asBoolean(),
                n.path("size").asInt(),
                topics,
                "NOASSERTION".equals(license) ? null : Text.clean(license, 50));
    }

    private static Instant instant(JsonNode node) {
        return node.isTextual() ? Instant.parse(node.asText()) : null;
    }

    private Optional<JsonNode> getJson(String path) {
        return get(path, JSON).map(body -> {
            try {
                return json.readTree(body);
            } catch (IOException e) {
                throw new GitHubException("GitHub sent something that is not JSON for " + path, e);
            }
        });
    }

    /** GET {@code path}; empty when the thing does not exist (404) or the repo is empty (409). */
    private Optional<String> get(String path, String accept) {
        HttpRequest.Builder request = HttpRequest.newBuilder(api.resolve(path))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", accept)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "ReaniGITor")
                .GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response;
        try {
            response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new GitHubException("Could not reach GitHub: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitHubException("Interrupted while talking to GitHub", e);
        }
        int status = response.statusCode();
        if (status / 100 == 2) {
            return Optional.of(response.body());
        }
        if (status == 404 || status == 409) {
            return Optional.empty();
        }
        if ((status == 403 || status == 429)
                && "0".equals(response.headers().firstValue("x-ratelimit-remaining").orElse(null))) {
            String reset = response.headers().firstValue("x-ratelimit-reset")
                    .map(s -> " It resets at " + Instant.ofEpochSecond(Long.parseLong(s)) + ".")
                    .orElse("");
            throw new GitHubException("GitHub API rate limit reached." + reset
                    + " Set GITHUB_TOKEN (a read-only token is enough) to raise the limit.");
        }
        throw new GitHubException("GitHub answered " + status + " for " + path);
    }
}
