package io.github.virien84.reanigitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnvTest {

    @Test
    void parsesDotEnvLines() {
        Map<String, String> values = Env.parseDotEnv(List.of(
                "# a comment",
                "",
                "GITHUB_TOKEN=abc123",
                "export ELEVENLABS_API_KEY = \"quoted value\"",
                "EMPTY=",
                "not a pair"));
        assertEquals("abc123", values.get("GITHUB_TOKEN"));
        assertEquals("quoted value", values.get("ELEVENLABS_API_KEY"));
        assertNull(values.get("EMPTY"));
        assertEquals(2, values.size());
    }

    @Test
    void theEnvironmentWinsOverTheDotEnvFile() {
        Env env = new Env(Map.of("GITHUB_TOKEN", "from-env"), Map.of("GITHUB_TOKEN", "from-file", "OTHER", "x"));
        assertEquals("from-env", env.get("GITHUB_TOKEN"));
        assertEquals("x", env.get("OTHER"));
        assertNull(env.get("MISSING"));
    }

    @Test
    void blankEnvironmentValuesCountAsMissing() {
        Env env = new Env(Map.of("GITHUB_TOKEN", "  "), Map.of());
        assertNull(env.get("GITHUB_TOKEN"));
    }
}
