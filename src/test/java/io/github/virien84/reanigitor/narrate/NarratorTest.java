package io.github.virien84.reanigitor.narrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.virien84.reanigitor.github.Remains;
import io.github.virien84.reanigitor.github.Repo;
import io.github.virien84.reanigitor.wake.Autopsy;
import io.github.virien84.reanigitor.wake.Calaverita;
import io.github.virien84.reanigitor.wake.Grave;
import io.github.virien84.reanigitor.wake.Graveyard;
import io.github.virien84.reanigitor.wake.Lang;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NarratorTest {

    record Call(String kind, ObjectNode body, Path out) {
    }

    /** Records the requests and writes a fake MP3, like the CLI would. */
    static final class FakeStudio implements AudioMaker {
        final List<Call> calls = new ArrayList<>();
        String failOn;

        @Override
        public void speech(ObjectNode body, Path out) throws IOException {
            record("speech", body, out);
        }

        @Override
        public void soundEffect(ObjectNode body, Path out) throws IOException {
            record("sfx", body, out);
        }

        private void record(String kind, ObjectNode body, Path out) throws IOException {
            calls.add(new Call(kind, body, out));
            if (failOn != null && out.getFileName().toString().startsWith(failOn)) {
                throw new IOException("401 invalid_api_key");
            }
            Files.write(out, new byte[] {1, 2, 3});
        }
    }

    static Grave grave(String name, Lang lang) {
        Repo repo = new Repo(name, "ghost/" + name, "SECRET-DESCRIPTION", "https://github.com/ghost/" + name,
                "Java", 0, 0, 0, Instant.parse("2019-01-01T00:00:00Z"), Instant.parse("2020-01-01T00:00:00Z"),
                false, false, 1, List.of(), null);
        Remains remains = new Remains(repo, "SECRET-README", "SECRET-COMMIT", null, List.of("SECRET-ISSUE"));
        return new Grave(remains,
                new Autopsy("SECRET-CAUSE", "SECRET-EPITAPH", List.of("a", "b", "c"), false),
                new Calaverita("La Flaca llegó", List.of("Verso uno,", "verso dos.", "Verso tres", "y cuatro."),
                        lang, false));
    }

    static Graveyard graveyard(Lang lang, Grave... graves) {
        return new Graveyard("ghost", Instant.parse("2026-10-26T12:00:00Z"), 12, "gemma4:e4b-it-qat", lang,
                List.of(graves));
    }

    private final ByteArrayOutputStream log = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(log, true, StandardCharsets.UTF_8);

    @Test
    void sendsOnlyThePublicCalaveritaText(@TempDir Path dir) throws Exception {
        FakeStudio studio = new FakeStudio();

        new Narrator(studio, out).narrate(graveyard(Lang.ES, grave("tomb", Lang.ES)), dir);

        Call speech = studio.calls.stream().filter(c -> c.kind().equals("speech")).findFirst().orElseThrow();
        String body = speech.body().toString();
        assertFalse(body.contains("SECRET"), "no repo data and no autopsy goes to ElevenLabs: " + body);
        String text = speech.body().path("text").asText();
        assertTrue(text.startsWith("[whispers]"), text);
        assertTrue(text.contains("La Flaca llegó"), text);
        assertTrue(text.contains("Verso uno,"), text);
        assertEquals("es", speech.body().path("language_code").asText());
        assertEquals("eleven_v4", speech.body().path("model_id").asText());
        assertEquals("old_radio", speech.body().path("audio_effects").path("filter_preset_id").asText());
        assertEquals("hall", speech.body().path("audio_effects").path("environment_id").asText());
        assertEquals(dir.resolve("audio").resolve("tomb.mp3"), speech.out());
    }

    @Test
    void makesOneLoopingAmbienceTrack(@TempDir Path dir) throws Exception {
        FakeStudio studio = new FakeStudio();

        new Narrator(studio, out).narrate(graveyard(Lang.EN, grave("tomb", Lang.EN)), dir);

        Call sfx = studio.calls.stream().filter(c -> c.kind().equals("sfx")).findFirst().orElseThrow();
        assertTrue(sfx.body().path("loop").asBoolean(), sfx.body().toString());
        assertTrue(sfx.body().path("duration_seconds").asDouble() <= 30);
        assertEquals(dir.resolve("audio").resolve(Narrator.AMBIENCE), sfx.out());
    }

    @Test
    void neverPaysTwiceForTheSameFile(@TempDir Path dir) throws Exception {
        Files.createDirectories(dir.resolve("audio"));
        Files.write(dir.resolve("audio").resolve("tomb.mp3"), new byte[] {9});
        Files.write(dir.resolve("audio").resolve(Narrator.AMBIENCE), new byte[] {9});
        FakeStudio studio = new FakeStudio();

        int made = new Narrator(studio, out).narrate(graveyard(Lang.EN, grave("tomb", Lang.EN), grave("other", Lang.EN)), dir);

        assertEquals(1, made);
        assertEquals(1, studio.calls.size());
        assertEquals(dir.resolve("audio").resolve("other.mp3"), studio.calls.get(0).out());
    }

    @Test
    void keepsGoingWhenOneFileFails(@TempDir Path dir) throws Exception {
        FakeStudio studio = new FakeStudio();
        studio.failOn = "aaa";

        int made = new Narrator(studio, out).narrate(graveyard(Lang.EN, grave("aaa", Lang.EN), grave("bbb", Lang.EN)), dir);

        assertEquals(2, made, "bbb and the ambience");
        assertTrue(log.toString(StandardCharsets.UTF_8).contains("401 invalid_api_key"));
        assertFalse(Files.exists(dir.resolve("audio").resolve("aaa.mp3")));
    }

    @Test
    void fileNamesAreSafeSlugs() {
        assertEquals("my-repo.js", Narrator.slug("my-repo.js"));
        String sneaky = Narrator.slug("/../etc/passwd");
        assertFalse(sneaky.contains("/") || sneaky.contains("\\") || sneaky.contains(".."), sneaky);
        assertFalse(Narrator.slug("..").contains(".."));
    }
}
