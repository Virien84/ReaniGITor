package io.github.virien84.reanigitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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
}
