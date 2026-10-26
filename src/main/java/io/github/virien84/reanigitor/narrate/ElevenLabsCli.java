package io.github.virien84.reanigitor.narrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Makes audio with the official ElevenLabs CLI, pinned to one version. The request body (with
 * the text) goes through stdin as JSON, never through the command line, so no text can be
 * interpreted by a shell. The command line only has fixed words and a path the code built.
 */
public final class ElevenLabsCli implements AudioMaker {

    static final String CLI_VERSION = "1.7.1";
    /** George: a warm, narrative voice that also speaks Spanish with eleven_v4. */
    static final String VOICE = "JBFqnCBsd6RMkjVDRZzb";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final boolean WINDOWS = System.getProperty("os.name", "").toLowerCase().startsWith("win");

    private final boolean dryRun;
    private final Map<String, String> extraEnv;

    /** @param dryRun validate each request locally without sending it (costs nothing) */
    public ElevenLabsCli(boolean dryRun) {
        this(dryRun, Map.of());
    }

    /** @param extraEnv variables for the CLI process (the API key read from .env); never logged */
    public ElevenLabsCli(boolean dryRun, Map<String, String> extraEnv) {
        this.dryRun = dryRun;
        this.extraEnv = Map.copyOf(extraEnv);
    }

    @Override
    public void speech(ObjectNode body, Path out) throws IOException {
        run(List.of("text-to-speech", "convert", "--voice-id", VOICE), body, out);
    }

    @Override
    public void soundEffect(ObjectNode body, Path out) throws IOException {
        run(List.of("text-to-sound-effects", "convert"), body, out);
    }

    List<String> command(List<String> operation, Path out) {
        List<String> command = new ArrayList<>();
        command.add(WINDOWS ? "npx.cmd" : "npx");
        command.add("-y");
        command.add("@elevenlabs/cli@" + CLI_VERSION);
        command.addAll(operation);
        command.add("--json");
        command.add("-");
        command.add("--output");
        command.add(out.toString());
        command.add("--quiet");
        if (dryRun) {
            command.add("--dry-run");
        }
        return command;
    }

    private void run(List<String> operation, ObjectNode body, Path out) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command(operation, out)).redirectErrorStream(true);
        builder.environment().putAll(extraEnv);
        Process process = builder.start();
        try (OutputStream stdin = process.getOutputStream()) {
            stdin.write(JSON.writeValueAsBytes(body));
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        try {
            if (!process.waitFor(3, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                throw new IOException("the ElevenLabs CLI took more than 3 minutes");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted while waiting for the ElevenLabs CLI", e);
        }
        if (process.exitValue() != 0) {
            throw new IOException("the ElevenLabs CLI failed: " + lastLine(output));
        }
    }

    private static String lastLine(String output) {
        String[] lines = output.split("\\R");
        return lines.length == 0 ? "(no output)" : lines[lines.length - 1];
    }
}
