package io.github.virien84.reanigitor.ollama;

/** Talking to the local Ollama server failed. */
public class OllamaException extends RuntimeException {

    public OllamaException(String message) {
        super(message);
    }

    public OllamaException(String message, Throwable cause) {
        super(message, cause);
    }
}
