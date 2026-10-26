package io.github.virien84.reanigitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.virien84.reanigitor.cemetery.Cemetery;
import io.github.virien84.reanigitor.github.FakeGitHub;
import io.github.virien84.reanigitor.github.GitHubClientTest;
import io.github.virien84.reanigitor.ollama.JsonChat;
import io.github.virien84.reanigitor.ollama.OllamaException;
import io.github.virien84.reanigitor.wake.Graveyard;
import io.github.virien84.reanigitor.wake.Lang;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReaniGITorTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        return ReaniGITor.run(args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    private String out() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return err.toString(StandardCharsets.UTF_8);
    }

    @Test
    void helpPrintsUsageAndSucceeds() {
        assertEquals(0, run("--help"));
        assertTrue(out().contains("Usage: reanigitor"), out());
    }

    @Test
    void versionPrintsTheProjectVersion() {
        assertEquals(0, run("--version"));
        assertTrue(out().matches("ReaniGITor \\d+\\.\\d+\\.\\d+.*\\R"), out());
    }

    @Test
    void noArgumentsIsAUsageError() {
        assertEquals(2, run());
        assertTrue(err().contains("Usage: reanigitor"), err());
    }

    @Test
    void unknownOptionIsAUsageError() {
        assertEquals(2, run("--resurrect-everything"));
        assertTrue(err().contains("Unknown option: --resurrect-everything"), err());
    }

    @Test
    void invalidUsernameIsRejected() {
        assertEquals(2, run("not a user"));
        assertTrue(err().contains("Not a valid GitHub username"), err());
    }

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC);
    private static final String REPOS = "/users/ghost/repos?type=owner&sort=pushed&per_page=100&page=1";

    @TempDir
    Path outDir;

    /** A model that always gives the same autopsy and poem. */
    private static final JsonChat MODEL = (system, user, schema) -> {
        try {
            return new ObjectMapper().readTree(schema.path("properties").has("epitaph")
                    ? "{\"cause_of_death\":\"Too many TODOs.\",\"epitaph\":\"TODO: live\","
                            + "\"resurrection\":[\"One\",\"Two\",\"Three\"]}"
                    : "{\"title\":\"Poem\",\"verses\":[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\"]}");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    };

    private int runAgainst(FakeGitHub github, String... args) {
        String[] withOut = Arrays.copyOf(args, args.length + 2);
        withOut[args.length] = "--out";
        withOut[args.length + 1] = outDir.toString();
        return ReaniGITor.run(withOut,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8),
                github.baseUri(), null, NOW, model -> MODEL);
    }

    @Test
    void writesTheGraveyardFile() throws Exception {
        try (FakeGitHub github = new FakeGitHub()) {
            github.on(REPOS, 200, "[" + GitHubClientTest.repoJson("tomb", "2020-01-01T00:00:00Z") + "]");

            assertEquals(0, runAgainst(github, "ghost", "--lang", "es"));

            Graveyard graveyard = Graveyard.read(outDir.resolve("ghost").resolve(Graveyard.FILE_NAME));
            assertEquals("ghost", graveyard.user());
            assertEquals(Lang.ES, graveyard.lang());
            assertEquals(1, graveyard.graves().size());
            assertEquals("TODO: live", graveyard.graves().get(0).autopsy().epitaph());
            assertTrue(out().contains("TODO: live"), out());
            String html = Files.readString(outDir.resolve("ghost").resolve(Cemetery.FILE_NAME));
            assertTrue(html.contains("TODO: live"), html);
        }
    }

    @Test
    void redrawsTheCemeteryFromTheSavedGraves() throws Exception {
        try (FakeGitHub github = new FakeGitHub()) {
            github.on(REPOS, 200, "[" + GitHubClientTest.repoJson("tomb", "2020-01-01T00:00:00Z") + "]");
            assertEquals(0, runAgainst(github, "ghost"));
            Path html = outDir.resolve("ghost").resolve(Cemetery.FILE_NAME);
            Files.delete(html);
            int requests = github.requests.size();

            assertEquals(0, runAgainst(github, "ghost", "--redraw"));

            assertTrue(Files.exists(html));
            assertEquals(requests, github.requests.size(), "a redraw never calls GitHub");
        }
    }

    @Test
    void redrawWithoutSavedGravesSaysWhatToDo() throws Exception {
        try (FakeGitHub github = new FakeGitHub()) {
            assertEquals(1, runAgainst(github, "ghost", "--redraw"));
            assertTrue(err().contains("Run it once without --redraw"), err());
        }
    }

    @Test
    void ollamaProblemsAreReportedBeforeDigging() throws Exception {
        try (FakeGitHub github = new FakeGitHub()) {
            github.on(REPOS, 200, "[" + GitHubClientTest.repoJson("tomb", "2020-01-01T00:00:00Z") + "]");

            int exit = ReaniGITor.run(new String[] {"ghost", "--out", outDir.toString()},
                    new PrintStream(out, true, StandardCharsets.UTF_8),
                    new PrintStream(err, true, StandardCharsets.UTF_8),
                    github.baseUri(), null, NOW, model -> {
                        throw new OllamaException("Start it with: ollama serve");
                    });

            assertEquals(1, exit);
            assertTrue(err().contains("ollama serve"), err());
            assertFalse(github.requests.stream().anyMatch(r -> r.pathAndQuery().contains("/readme")));
        }
    }

    @Test
    void buriesTheAbandonedRepos() throws Exception {
        try (FakeGitHub github = new FakeGitHub()) {
            github.on(REPOS, 200, "[" + GitHubClientTest.repoJson("alive", "2026-10-01T00:00:00Z") + ","
                    + GitHubClientTest.repoJson("tomb", "2020-01-01T00:00:00Z") + "]");
            github.on("/repos/ghost/tomb/commits?per_page=1", 200,
                    "[{\"commit\":{\"message\":\"wip\\u001b[31m\",\"committer\":{\"date\":\"2020-01-01T00:00:00Z\"}}}]");

            assertEquals(0, runAgainst(github, "ghost"));

            String census = out();
            assertTrue(census.contains("2 repos, 1 abandoned"), census);
            assertTrue(census.contains("tomb"), census);
            assertTrue(census.contains("2020-01-01"), census);
            assertTrue(census.contains("Cause of death: Too many TODOs."), census);
            assertFalse(census.contains("\u001b"), census);
            assertFalse(census.contains("alive"), census);
        }
    }

    @Test
    void saysSoWhenNothingIsDead() throws Exception {
        try (FakeGitHub github = new FakeGitHub()) {
            github.on(REPOS, 200, "[" + GitHubClientTest.repoJson("alive", "2026-10-01T00:00:00Z") + "]");

            assertEquals(0, runAgainst(github, "ghost"));

            assertTrue(out().contains("No abandoned repos"), out());
        }
    }

    @Test
    void gitHubErrorsAreReportedWithoutAStackTrace() throws Exception {
        try (FakeGitHub github = new FakeGitHub()) {
            assertEquals(1, runAgainst(github, "ghost"));
            assertTrue(err().contains("No GitHub user named ghost"), err());
        }
    }
}
