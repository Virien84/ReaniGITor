package io.github.virien84.reanigitor.ollama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OllamaClientTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer server;
    private final Deque<String> chatReplies = new ArrayDeque<>();
    private final List<JsonNode> chatRequests = new ArrayList<>();
    private String tags = "{\"models\":[{\"name\":\"gemma4:e4b-it-qat\"}]}";

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", exchange -> {
            synchronized (this) {
                chatRequests.add(JSON.readTree(exchange.getRequestBody()));
            }
            reply(exchange, 200, chatReplies.isEmpty() ? "{}" : chatReplies.poll());
        });
        server.createContext("/api/tags", exchange -> reply(exchange, 200, tags));
        server.start();
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private URI base() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    private static String chatReply(String content) throws Exception {
        ObjectNode reply = JSON.createObjectNode();
        reply.putObject("message").put("role", "assistant").put("content", content);
        reply.put("done", true);
        return JSON.writeValueAsString(reply);
    }

    @Test
    void sendsSystemAndUserMessagesWithTheSchemaAndNoThinkingOrTools() throws Exception {
        chatReplies.add(chatReply("{\"epitaph\":\"Here lies main\"}"));
        ObjectNode schema = JSON.createObjectNode().put("type", "object");

        JsonNode answer = new OllamaClient(base(), "gemma4:e4b-it-qat").chat("be a poet", "<repo>x</repo>", schema);

        assertEquals("Here lies main", answer.path("epitaph").asText());
        JsonNode request = chatRequests.get(0);
        assertEquals("gemma4:e4b-it-qat", request.path("model").asText());
        assertFalse(request.path("stream").asBoolean(true));
        assertFalse(request.path("think").asBoolean(true));
        assertEquals(schema, request.path("format"));
        assertFalse(request.has("tools"), "the model never gets tools");
        assertEquals("system", request.path("messages").get(0).path("role").asText());
        assertEquals("be a poet", request.path("messages").get(0).path("content").asText());
        assertEquals("user", request.path("messages").get(1).path("role").asText());
        assertEquals("<repo>x</repo>", request.path("messages").get(1).path("content").asText());
    }

    @Test
    void contentThatIsNotAJsonObjectIsAnError() throws Exception {
        chatReplies.add(chatReply("Sure! Here is your poem"));

        OllamaException e = assertThrows(OllamaException.class,
                () -> new OllamaClient(base(), "gemma4:e4b-it-qat").chat("s", "u", JSON.createObjectNode()));
        assertTrue(e.getMessage().contains("not a JSON object"), e.getMessage());
    }

    @Test
    void refusesAnyHostThatIsNotThisMachine() {
        assertThrows(IllegalArgumentException.class,
                () -> new OllamaClient(URI.create("http://example.com:11434/"), "gemma4:e4b-it-qat"));
        assertThrows(IllegalArgumentException.class,
                () -> new OllamaClient(URI.create("https://ollama.com/"), "gemma4:e4b-it-qat"));
        new OllamaClient(URI.create("http://localhost:11434/"), "gemma4:e4b-it-qat");
        new OllamaClient(URI.create("http://[::1]:11434/"), "gemma4:e4b-it-qat");
    }

    @Test
    void checksThatTheModelIsInstalled() {
        new OllamaClient(base(), "gemma4:e4b-it-qat").requireModel();

        OllamaException e = assertThrows(OllamaException.class,
                () -> new OllamaClient(base(), "gemma4:e2b-it-qat").requireModel());
        assertTrue(e.getMessage().contains("ollama pull gemma4:e2b-it-qat"), e.getMessage());
    }

    @Test
    void saysHowToStartOllamaWhenItIsNotRunning() {
        server.stop(0);
        OllamaException e = assertThrows(OllamaException.class,
                () -> new OllamaClient(base(), "gemma4:e4b-it-qat").requireModel());
        assertTrue(e.getMessage().contains("ollama serve"), e.getMessage());
    }
}
