package io.github.virien84.reanigitor.wake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Everyone buried in one run, saved as {@code graves.json} so the cemetery can be drawn again
 * without asking the model again.
 */
public record Graveyard(String user, Instant dugAt, int months, String model, Lang lang, List<Grave> graves) {

    public static final String FILE_NAME = "graves.json";

    private static final ObjectMapper JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public Graveyard {
        graves = List.copyOf(graves);
    }

    public void write(Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        JSON.writeValue(file.toFile(), this);
    }

    public static Graveyard read(Path file) throws IOException {
        return JSON.readValue(file.toFile(), Graveyard.class);
    }
}
