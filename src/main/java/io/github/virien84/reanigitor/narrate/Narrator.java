package io.github.virien84.reanigitor.narrate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.virien84.reanigitor.wake.Calaverita;
import io.github.virien84.reanigitor.wake.Grave;
import io.github.virien84.reanigitor.wake.Graveyard;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Optional garnish: a ghostly reading of each calaverita and a looping graveyard ambience, made
 * with ElevenLabs. Only the calaverita (public text the model already wrote) is sent, never repo
 * data. Files that already exist are skipped, so credits are never spent twice.
 */
public final class Narrator {

    public static final String AUDIO_DIR = "audio";
    public static final String AMBIENCE = "ambience.mp3";

    static final String AMBIENCE_PROMPT = "Mexican cemetery at night on the Day of the Dead: crickets, "
            + "a soft warm wind, candles crackling, a distant church bell, faint far-away guitar";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final AudioMaker studio;
    private final PrintStream out;

    public Narrator(AudioMaker studio, PrintStream out) {
        this.studio = studio;
        this.out = out;
    }

    /** @return how many audio files were made */
    public int narrate(Graveyard graveyard, Path dir) throws IOException {
        Path audio = dir.resolve(AUDIO_DIR);
        Files.createDirectories(audio);
        int made = 0;
        for (Grave grave : graveyard.graves()) {
            Path file = audio.resolve(fileName(grave.remains().repo().name()));
            if (make(file, () -> studio.speech(speechRequest(grave.calaverita()), file))) {
                made++;
            }
        }
        Path ambience = audio.resolve(AMBIENCE);
        if (make(ambience, () -> studio.soundEffect(ambienceRequest(), ambience))) {
            made++;
        }
        return made;
    }

    private interface Job {
        void run() throws IOException;
    }

    private boolean make(Path file, Job job) {
        if (Files.exists(file)) {
            out.println("  already there: " + file.getFileName());
            return false;
        }
        out.println("  ElevenLabs: " + file.getFileName());
        try {
            job.run();
            return true;
        } catch (IOException e) {
            out.println("  could not record " + file.getFileName() + ": " + e.getMessage());
            return false;
        }
    }

    static ObjectNode speechRequest(Calaverita poem) {
        StringBuilder text = new StringBuilder("[whispers] ").append(poem.title()).append(".\n");
        for (String verse : poem.verses()) {
            text.append(verse).append('\n');
        }
        text.append("[laughs]");
        ObjectNode body = JSON.createObjectNode();
        body.put("text", text.toString());
        body.put("model_id", "eleven_v4");
        body.put("language_code", poem.lang().code());
        ObjectNode effects = body.putObject("audio_effects");
        effects.put("filter_preset_id", "old_radio");
        effects.put("environment_id", "hall");
        effects.put("send_level", 0.35);
        effects.put("distance", 0.2);
        return body;
    }

    static ObjectNode ambienceRequest() {
        ObjectNode body = JSON.createObjectNode();
        body.put("text", AMBIENCE_PROMPT);
        body.put("model_id", "eleven_text_to_sound_v2");
        body.put("duration_seconds", 22.0);
        body.put("prompt_influence", 0.4);
        body.put("loop", true);
        return body;
    }

    public static String fileName(String repoName) {
        return slug(repoName) + ".mp3";
    }

    /** A file-name-safe version of a repo name: no separators, no "..". */
    static String slug(String name) {
        String slug = name.replaceAll("[^A-Za-z0-9._-]", "_").replaceAll("\\.{2,}", "_");
        return slug.isEmpty() ? "_" : slug;
    }
}
