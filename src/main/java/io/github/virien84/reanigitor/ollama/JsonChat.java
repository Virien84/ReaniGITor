package io.github.virien84.reanigitor.ollama;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** A model that answers a system + user prompt with a JSON object shaped by a JSON schema. */
public interface JsonChat {

    /**
     * @return the JSON object the model wrote; callers must still validate it
     * @throws OllamaException when the model cannot be reached or does not return a JSON object
     */
    JsonNode chat(String system, String user, ObjectNode schema);
}
