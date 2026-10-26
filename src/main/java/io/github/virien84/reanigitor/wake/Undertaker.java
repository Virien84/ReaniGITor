package io.github.virien84.reanigitor.wake;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.virien84.reanigitor.Text;
import io.github.virien84.reanigitor.github.Remains;
import io.github.virien84.reanigitor.ollama.JsonChat;
import io.github.virien84.reanigitor.ollama.OllamaException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Asks the model for the autopsy and the calaverita of a repo, then validates and cleans the
 * answers. The model only writes text: the code decides everything else.
 */
public final class Undertaker {

    static final int ATTEMPTS = 2;
    static final int CAUSE_CHARS = 400;
    static final int EPITAPH_CHARS = 140;
    static final int STEP_CHARS = 220;
    static final int TITLE_CHARS = 80;
    static final int VERSE_CHARS = 120;
    static final int MIN_VERSES = 4;
    static final int MAX_VERSES = 16;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final JsonChat model;
    private final Lang lang;
    private final Clock clock;

    public Undertaker(JsonChat model, Lang lang, Clock clock) {
        this.model = model;
        this.lang = lang;
        this.clock = clock;
    }

    public Grave bury(Remains remains) {
        String evidence = Prompts.evidence(remains, clock.instant());
        Autopsy autopsy = ask(Prompts.autopsySystem(), evidence, autopsySchema(), Undertaker::toAutopsy);
        Calaverita calaverita = ask(Prompts.calaveritaSystem(lang), evidence, calaveritaSchema(), this::toCalaverita);
        return new Grave(remains,
                autopsy != null ? autopsy : Fallbacks.autopsy(),
                calaverita != null ? calaverita : Fallbacks.calaverita(lang));
    }

    /** @return the validated answer, or null when every attempt failed */
    private <T> T ask(String system, String user, ObjectNode schema, Function<JsonNode, T> validate) {
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                T result = validate.apply(model.chat(system, user, schema));
                if (result != null) {
                    return result;
                }
            } catch (OllamaException e) {
                // Try again; the fallback covers the last failure.
            }
        }
        return null;
    }

    private static Autopsy toAutopsy(JsonNode answer) {
        String cause = Text.clean(answer.path("cause_of_death").asText(null), CAUSE_CHARS);
        String epitaph = Text.firstLine(answer.path("epitaph").asText(null), EPITAPH_CHARS);
        List<String> steps = new ArrayList<>();
        for (JsonNode step : answer.path("resurrection")) {
            String text = Text.clean(step.asText(null), STEP_CHARS);
            if (text != null) {
                steps.add(text.replace('\n', ' '));
            }
        }
        if (cause == null || epitaph == null || steps.size() != 3) {
            return null;
        }
        return new Autopsy(cause, epitaph, steps, false);
    }

    private Calaverita toCalaverita(JsonNode answer) {
        String title = Text.firstLine(answer.path("title").asText(null), TITLE_CHARS);
        List<String> verses = new ArrayList<>();
        for (JsonNode verse : answer.path("verses")) {
            String text = Text.clean(verse.asText(null), VERSE_CHARS * 4);
            if (text == null) {
                continue;
            }
            for (String line : text.split("\n")) {
                String clean = Text.clean(line, VERSE_CHARS);
                if (clean != null) {
                    verses.add(clean);
                }
            }
        }
        if (title == null || verses.size() < MIN_VERSES || verses.size() > MAX_VERSES) {
            return null;
        }
        return new Calaverita(title, verses, lang, false);
    }

    static ObjectNode autopsySchema() {
        ObjectNode schema = object();
        ObjectNode props = schema.putObject("properties");
        props.putObject("cause_of_death").put("type", "string");
        props.putObject("epitaph").put("type", "string");
        stringArray(props.putObject("resurrection"), 3, 3);
        schema.putArray("required").add("cause_of_death").add("epitaph").add("resurrection");
        return schema;
    }

    static ObjectNode calaveritaSchema() {
        ObjectNode schema = object();
        ObjectNode props = schema.putObject("properties");
        props.putObject("title").put("type", "string");
        stringArray(props.putObject("verses"), 8, 8);
        schema.putArray("required").add("title").add("verses");
        return schema;
    }

    private static ObjectNode object() {
        ObjectNode schema = JSON.createObjectNode();
        schema.put("type", "object");
        return schema;
    }

    private static void stringArray(ObjectNode node, int min, int max) {
        node.put("type", "array");
        node.putObject("items").put("type", "string");
        node.put("minItems", min);
        node.put("maxItems", max);
    }
}
