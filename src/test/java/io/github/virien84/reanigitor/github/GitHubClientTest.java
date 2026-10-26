package io.github.virien84.reanigitor.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GitHubClientTest {

    private static final String REPOS = "/users/ghost/repos?type=owner&sort=pushed&per_page=100&page=";

    private FakeGitHub github;

    @BeforeEach
    void start() throws Exception {
        github = new FakeGitHub();
    }

    @AfterEach
    void stop() {
        github.close();
    }

    public static String repoJson(String name, String pushedAt) {
        return """
                {"name":"%s","full_name":"ghost/%s","description":"A <b>bold</b> idea",
                 "html_url":"https://github.com/ghost/%s","language":"Java",
                 "stargazers_count":3,"forks_count":1,"open_issues_count":2,
                 "created_at":"2019-01-01T00:00:00Z","pushed_at":"%s",
                 "fork":false,"archived":false,"size":120,"topics":["cli","halloween"],
                 "license":{"spdx_id":"MIT"},"default_branch":"main"}
                """.formatted(name, name, name, pushedAt);
    }

    @Test
    void listsAllPagesOfRepos() {
        StringBuilder page1 = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            page1.append(i == 0 ? "" : ",").append(repoJson("r" + i, "2020-01-01T00:00:00Z"));
        }
        page1.append("]");
        github.on(REPOS + "1", 200, page1.toString());
        github.on(REPOS + "2", 200, "[" + repoJson("last", "2021-05-06T07:08:09Z") + "]");

        List<Repo> repos = new GitHubClient(github.baseUri(), null).listRepos("ghost");

        assertEquals(101, repos.size());
        Repo last = repos.get(100);
        assertEquals("last", last.name());
        assertEquals("ghost/last", last.fullName());
        assertEquals("A <b>bold</b> idea", last.description());
        assertEquals("Java", last.language());
        assertEquals(3, last.stars());
        assertEquals(2, last.openIssues());
        assertEquals(Instant.parse("2021-05-06T07:08:09Z"), last.pushedAt());
        assertEquals(List.of("cli", "halloween"), last.topics());
        assertEquals("MIT", last.license());
        assertFalse(last.fork());
    }

    @Test
    void onlyEverSendsGetRequestsWithTheGitHubHeaders() {
        github.on(REPOS + "1", 200, "[]");

        new GitHubClient(github.baseUri(), "secret-token").listRepos("ghost");

        FakeGitHub.Request request = github.requests.get(0);
        assertEquals("GET", request.method());
        assertEquals("application/vnd.github+json", request.accept());
        assertEquals("Bearer secret-token", request.authorization());
    }

    @Test
    void sendsNoAuthorizationWithoutAToken() {
        github.on(REPOS + "1", 200, "[]");

        new GitHubClient(github.baseUri(), null).listRepos("ghost");

        assertNull(github.requests.get(0).authorization());
    }

    @Test
    void unknownUserIsAClearError() {
        GitHubException e = assertThrows(GitHubException.class,
                () -> new GitHubClient(github.baseUri(), null).listRepos("ghost"));
        assertTrue(e.getMessage().contains("No GitHub user named ghost"), e.getMessage());
    }

    @Test
    void rateLimitIsAClearError() {
        github.on(REPOS + "1", 403, "{\"message\":\"API rate limit exceeded\"}",
                Map.of("x-ratelimit-remaining", "0", "x-ratelimit-reset", "1700000000"));

        GitHubException e = assertThrows(GitHubException.class,
                () -> new GitHubClient(github.baseUri(), null).listRepos("ghost"));
        assertTrue(e.getMessage().contains("rate limit"), e.getMessage());
        assertTrue(e.getMessage().contains("GITHUB_TOKEN"), e.getMessage());
    }

    @Test
    void rejectsUsernamesThatAreNotGitHubUsernames() {
        assertThrows(IllegalArgumentException.class,
                () -> new GitHubClient(github.baseUri(), null).listRepos("../admin"));
    }

    @Test
    void exhumesReadmeLastCommitAndOpenIssues() {
        github.on(REPOS + "1", 200, "[" + repoJson("tomb", "2020-01-01T00:00:00Z") + "]");
        github.on("/repos/ghost/tomb/readme", 200, "# Tomb\nThe best\u0007 project ever.");
        github.on("/repos/ghost/tomb/commits?per_page=1", 200, """
                [{"commit":{"message":"fix: final fix (for real this time)\\n\\nlong body",
                  "committer":{"date":"2020-01-01T00:00:00Z"}}}]
                """);
        github.on("/repos/ghost/tomb/issues?state=open&per_page=5", 200, """
                [{"title":"It does not compile"},{"title":"Add tests","pull_request":{}}]
                """);
        GitHubClient client = new GitHubClient(github.baseUri(), null);
        Repo repo = client.listRepos("ghost").get(0);

        Remains remains = client.exhume(repo);

        assertEquals("# Tomb\nThe best project ever.", remains.readme());
        assertEquals("fix: final fix (for real this time)", remains.lastCommitMessage());
        assertEquals(Instant.parse("2020-01-01T00:00:00Z"), remains.lastCommitAt());
        assertEquals(List.of("It does not compile", "PR: Add tests"), remains.openIssueTitles());
        assertTrue(github.requests.stream()
                .filter(r -> r.pathAndQuery().endsWith("/readme"))
                .allMatch(r -> r.accept().equals("application/vnd.github.raw+json")));
    }

    @Test
    void exhumingAnEmptyRepoLeavesTheRemainsBlank() {
        String empty = repoJson("void", "2020-01-01T00:00:00Z").replace("\"open_issues_count\":2", "\"open_issues_count\":0");
        github.on(REPOS + "1", 200, "[" + empty + "]");
        github.on("/repos/ghost/void/commits?per_page=1", 409, "{\"message\":\"Git Repository is empty.\"}");
        GitHubClient client = new GitHubClient(github.baseUri(), null);
        Repo repo = client.listRepos("ghost").get(0);

        Remains remains = client.exhume(repo);

        assertNull(remains.readme());
        assertNull(remains.lastCommitMessage());
        assertEquals(List.of(), remains.openIssueTitles());
        assertFalse(github.requests.stream().anyMatch(r -> r.pathAndQuery().contains("/issues")),
                "no issue request when the repo has none open");
    }
}
