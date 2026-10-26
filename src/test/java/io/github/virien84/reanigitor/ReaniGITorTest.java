package io.github.virien84.reanigitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.virien84.reanigitor.github.FakeGitHub;
import io.github.virien84.reanigitor.github.GitHubClientTest;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

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

    private int runAgainst(FakeGitHub github, String... args) {
        return ReaniGITor.run(args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8),
                github.baseUri(), null, NOW);
    }

    @Test
    void listsTheAbandonedReposWithTheirRemains() throws Exception {
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
            assertTrue(census.contains("wip[31m"), "control characters are stripped: " + census);
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
