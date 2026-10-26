package io.github.virien84.reanigitor.wake;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GraveyardTest {

    @Test
    void survivesARoundTripThroughJson(@TempDir Path dir) throws Exception {
        Grave grave = new Grave(PromptsTest.remains("# Tomb"), Fallbacks.autopsy(), Fallbacks.calaverita(Lang.ES));
        Graveyard graveyard = new Graveyard("ghost", Instant.parse("2026-10-26T12:00:00Z"), 12,
                "gemma4:e4b-it-qat", Lang.ES, List.of(grave));
        Path file = dir.resolve("ghost").resolve(Graveyard.FILE_NAME);

        graveyard.write(file);

        assertEquals(graveyard, Graveyard.read(file));
    }
}
