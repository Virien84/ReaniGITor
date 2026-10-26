package io.github.virien84.reanigitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.virien84.reanigitor.wake.Lang;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OptionsTest {

    @Test
    void defaults() {
        Options options = Options.parse("ghost");
        assertEquals("ghost", options.user());
        assertEquals(12, options.months());
        assertEquals(12, options.max());
    }

    @Test
    void thresholdAndLimitCanBeChanged() {
        Options options = Options.parse("--months", "24", "ghost", "--max", "3");
        assertEquals("ghost", options.user());
        assertEquals(24, options.months());
        assertEquals(3, options.max());
    }

    @Test
    void numbersMustBePositiveIntegers() {
        UsageException e = assertThrows(UsageException.class, () -> Options.parse("ghost", "--months", "0"));
        assertTrue(e.getMessage().contains("--months"), e.getMessage());
        assertThrows(UsageException.class, () -> Options.parse("ghost", "--max", "many"));
        assertThrows(UsageException.class, () -> Options.parse("ghost", "--max"));
    }

    @Test
    void languageModelAndOutputFolder() {
        Options defaults = Options.parse("ghost");
        assertEquals(Lang.EN, defaults.lang());
        assertEquals("gemma4:e4b-it-qat", defaults.model());
        assertEquals(Path.of("output"), defaults.out());
        assertFalse(defaults.redraw());
        assertTrue(Options.parse("ghost", "--redraw").redraw());
        Options narrate = Options.parse("ghost", "--narrate", "--dry-run");
        assertTrue(narrate.narrate() && narrate.dryRun());
        assertThrows(UsageException.class, () -> Options.parse("ghost", "--dry-run"));

        Options options = Options.parse("ghost", "--lang", "ES", "--model", "gemma4:e2b", "--out", "graves");
        assertEquals(Lang.ES, options.lang());
        assertEquals("gemma4:e2b", options.model());
        assertEquals(Path.of("graves"), options.out());

        assertThrows(UsageException.class, () -> Options.parse("ghost", "--lang", "fr"));
        assertThrows(UsageException.class, () -> Options.parse("ghost", "--model", "x y"));
    }

    @Test
    void exactlyOneUser() {
        assertThrows(UsageException.class, () -> Options.parse());
        assertThrows(UsageException.class, () -> Options.parse("ghost", "casper"));
    }

    @Test
    void unknownOption() {
        UsageException e = assertThrows(UsageException.class, () -> Options.parse("ghost", "--nope"));
        assertEquals("Unknown option: --nope", e.getMessage());
    }

    @Test
    void invalidUsername() {
        UsageException e = assertThrows(UsageException.class, () -> Options.parse("-ghost-"));
        assertTrue(e.getMessage().startsWith("Unknown option") || e.getMessage().startsWith("Not a valid"));
        assertThrows(UsageException.class, () -> Options.parse("gh--ost"));
        assertThrows(UsageException.class, () -> Options.parse("a".repeat(40)));
    }
}
