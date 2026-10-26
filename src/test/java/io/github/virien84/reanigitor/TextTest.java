package io.github.virien84.reanigitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void stripsControlAndInvisibleFormatCharactersButKeepsLines() {
        assertEquals("red\nline\tok", Text.clean("\u001b[31mred\u0000\r\nline\tok‮", 100)
                .replace("[31m", ""));
    }

    @Test
    void cutsLongTextWithAnEllipsis() {
        assertEquals("abcd…", Text.clean("abcdefgh", 5));
        assertEquals("abc", Text.clean("abc", 3));
    }

    @Test
    void blankIsNull() {
        assertNull(Text.clean(null, 10));
        assertNull(Text.clean(" \n\t ", 10));
    }

    @Test
    void firstLineHandlesHugeLimits() {
        assertEquals("fix it", Text.firstLine("fix it\n\nbody", Integer.MAX_VALUE));
    }
}
