package io.github.virien84.reanigitor.narrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ElevenLabsCliTest {

    @Test
    void theTextGoesThroughStdinNeverThroughTheCommandLine() {
        List<String> command = new ElevenLabsCli(false).command(
                List.of("text-to-speech", "convert", "--voice-id", ElevenLabsCli.VOICE), Path.of("audio", "tomb.mp3"));

        assertTrue(command.contains("@elevenlabs/cli@" + ElevenLabsCli.CLI_VERSION), "pinned version: " + command);
        assertFalse(command.stream().anyMatch(a -> a.contains("latest")), command.toString());
        int json = command.indexOf("--json");
        assertEquals("-", command.get(json + 1));
        assertFalse(command.contains("--text"), command.toString());
        assertFalse(command.contains("--dry-run"));
    }

    @Test
    void dryRunValidatesWithoutSpending() {
        List<String> command = new ElevenLabsCli(true).command(List.of("text-to-speech", "convert"), Path.of("x.mp3"));
        assertTrue(command.contains("--dry-run"), command.toString());
    }
}
