package io.github.virien84.reanigitor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads settings (and secrets) from the environment, falling back to a git-ignored
 * {@code .env} file in the working directory. Values are never printed or logged.
 */
public final class Env {

    private final Map<String, String> environment;
    private final Map<String, String> dotEnv;

    Env(Map<String, String> environment, Map<String, String> dotEnv) {
        this.environment = environment;
        this.dotEnv = dotEnv;
    }

    public static Env load() {
        Path file = Path.of(".env");
        Map<String, String> dotEnv = Map.of();
        if (Files.isRegularFile(file)) {
            try {
                dotEnv = parseDotEnv(Files.readAllLines(file, StandardCharsets.UTF_8));
            } catch (IOException e) {
                System.err.println("Warning: could not read .env, ignoring it");
            }
        }
        return new Env(System.getenv(), dotEnv);
    }

    /** The value of {@code name}, or null when it is missing or blank. */
    public String get(String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            value = dotEnv.get(name);
        }
        return value == null || value.isBlank() ? null : value.strip();
    }

    static Map<String, String> parseDotEnv(List<String> lines) {
        Map<String, String> values = new HashMap<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).strip();
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).strip();
            String value = line.substring(eq + 1).strip();
            if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                    || value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            if (!value.isEmpty()) {
                values.put(key, value);
            }
        }
        return values;
    }
}
