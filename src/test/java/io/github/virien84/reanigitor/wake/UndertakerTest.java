package io.github.virien84.reanigitor.wake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.virien84.reanigitor.ollama.JsonChat;
import io.github.virien84.reanigitor.ollama.OllamaException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.Test;

class UndertakerTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC);

    /** A scripted model: answers with the queued replies, in order. */
    static final class ScriptedModel implements JsonChat {
        final Deque<Object> replies = new ArrayDeque<>();
        final List<ObjectNode> schemas = new ArrayList<>();
        final List<String> systems = new ArrayList<>();

        ScriptedModel then(String json) {
            replies.add(json);
            return this;
        }

        ScriptedModel thenFail() {
            replies.add(new OllamaException("the spirits are silent"));
            return this;
        }

        @Override
        public JsonNode chat(String system, String user, ObjectNode schema) {
            systems.add(system);
            schemas.add(schema);
            Object next = replies.poll();
            if (next instanceof RuntimeException e) {
                throw e;
            }
            try {
                return JSON.readTree((String) next);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
    }

    private static final String AUTOPSY = """
            {"cause_of_death":"Died of a final fix that was not final.",
             "epitaph":"It compiled on my machine.",
             "resurrection":["Fix the build.","Answer issue #1.","Write one test."]}
            """;
    private static final String POEM = """
            {"title":"The last fix","verses":["Line one","Line two","Line three","Line four",
             "Line five","Line six","Line seven","Line eight"]}
            """;

    @Test
    void writesTheAutopsyAndTheCalaverita() {
        ScriptedModel model = new ScriptedModel().then(AUTOPSY).then(POEM);

        Grave grave = new Undertaker(model, Lang.ES, NOW).bury(PromptsTest.remains("readme"));

        assertEquals("Died of a final fix that was not final.", grave.autopsy().causeOfDeath());
        assertEquals("It compiled on my machine.", grave.autopsy().epitaph());
        assertEquals(List.of("Fix the build.", "Answer issue #1.", "Write one test."), grave.autopsy().resurrection());
        assertEquals("The last fix", grave.calaverita().title());
        assertEquals(8, grave.calaverita().verses().size());
        assertEquals(Lang.ES, grave.calaverita().lang());
        assertFalse(grave.autopsy().fallback());
        assertTrue(model.systems.get(1).contains("Mexican Spanish"));
        assertEquals("object", model.schemas.get(0).path("type").asText());
        assertEquals(3, model.schemas.get(0).path("properties").path("resurrection").path("minItems").asInt());
    }

    @Test
    void retriesOnceWhenTheAnswerBreaksTheRules() {
        String twoSteps = AUTOPSY.replace("\"Answer issue #1.\",", "");
        ScriptedModel model = new ScriptedModel().then(twoSteps).then(AUTOPSY).then(POEM);

        Grave grave = new Undertaker(model, Lang.EN, NOW).bury(PromptsTest.remains(null));

        assertEquals(3, grave.autopsy().resurrection().size());
        assertFalse(grave.autopsy().fallback());
    }

    @Test
    void fallsBackToPlainTextWhenTheModelKeepsFailing() {
        ScriptedModel model = new ScriptedModel().thenFail().then("{\"nope\":1}").thenFail().thenFail();

        Grave grave = new Undertaker(model, Lang.EN, NOW).bury(PromptsTest.remains(null));

        assertTrue(grave.autopsy().fallback());
        assertTrue(grave.calaverita().fallback());
        assertEquals(3, grave.autopsy().resurrection().size());
        assertFalse(grave.calaverita().verses().isEmpty());
    }

    @Test
    void cleansAndCapsWhatTheModelWrites() {
        String messy = """
                {"cause_of_death":"\\u001b[2JDied.%s",
                 "epitaph":"  Here lies <script>alert(1)</script>  ",
                 "resurrection":["a","b","c"]}
                """.formatted("x".repeat(1000));
        String poem = """
                {"title":"T","verses":["one\\ntwo","three","four",""]}
                """;
        ScriptedModel model = new ScriptedModel().then(messy).then(poem);

        Grave grave = new Undertaker(model, Lang.EN, NOW).bury(PromptsTest.remains(null));

        assertFalse(grave.autopsy().causeOfDeath().contains("\u001b"));
        assertTrue(grave.autopsy().causeOfDeath().length() <= Undertaker.CAUSE_CHARS);
        // Kept as text: the HTML writer escapes it.
        assertEquals("Here lies <script>alert(1)</script>", grave.autopsy().epitaph());
        assertEquals(List.of("one", "two", "three", "four"), grave.calaverita().verses());
    }
}
