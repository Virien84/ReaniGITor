package io.github.virien84.reanigitor.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

/**
 * Talks to an Ollama server on this machine. The model only gets a system prompt, a user
 * prompt and a JSON schema for its answer: no tools, no thinking, no streaming.
 */
public final class OllamaClient implements JsonChat {

    public static final URI DEFAULT_URL = URI.create("http://127.0.0.1:11434/");
    public static final String DEFAULT_MODEL = "gemma4:e4b-it-qat";

    private static final Set<String> LOOPBACK = Set.of("127.0.0.1", "localhost", "[::1]", "::1");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper json = new ObjectMapper();
    private final URI base;
    private final String model;

    public OllamaClient(URI base, String model) {
        if (!"http".equals(base.getScheme()) || base.getHost() == null || !LOOPBACK.contains(base.getHost())) {
            throw new IllegalArgumentException("Ollama must run on this machine (http://127.0.0.1), not " + base);
        }
        this.base = base;
        this.model = model;
    }

    public String model() {
        return model;
    }

    /** Fails with a helpful message when Ollama is not running or the model is not pulled. */
    public void requireModel() {
        JsonNode tags = parse(send(HttpRequest.newBuilder(base.resolve("api/tags"))
                .timeout(Duration.ofSeconds(10)).GET().build()));
        for (JsonNode m : tags.path("models")) {
            if (model.equals(m.path("name").asText()) || model.equals(m.path("model").asText())) {
                return;
            }
        }
        throw new OllamaException("The model " + model + " is not installed. Run: ollama pull " + model);
    }

    @Override
    public JsonNode chat(String system, String user, ObjectNode schema) {
        ObjectNode body = json.createObjectNode();
        body.put("model", model);
        body.put("stream", false);
        body.put("think", false);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", system);
        messages.addObject().put("role", "user").put("content", user);
        body.set("format", schema);
        body.putObject("options")
                .put("temperature", 0.75)
                .put("num_ctx", 4096)
                .put("num_predict", 700);
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(base.resolve("api/chat"))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                    .build();
        } catch (IOException e) {
            throw new OllamaException("Could not build the request", e);
        }
        String content = parse(send(request)).path("message").path("content").asText("");
        JsonNode answer;
        try {
            answer = json.readTree(content);
        } catch (IOException e) {
            answer = null;
        }
        if (answer == null || !answer.isObject()) {
            throw new OllamaException("The model answered with something that is not a JSON object");
        }
        return answer;
    }

    private String send(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new OllamaException("Cannot reach Ollama at " + base + ". Start it with: ollama serve", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OllamaException("Interrupted while waiting for Ollama", e);
        }
        if (response.statusCode() / 100 != 2) {
            throw new OllamaException("Ollama answered " + response.statusCode() + ": "
                    + response.body().substring(0, Math.min(200, response.body().length())));
        }
        return response.body();
    }

    private JsonNode parse(String body) {
        try {
            return json.readTree(body);
        } catch (IOException e) {
            throw new OllamaException("Ollama sent something that is not JSON", e);
        }
    }
}
