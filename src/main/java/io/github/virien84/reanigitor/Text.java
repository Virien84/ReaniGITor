package io.github.virien84.reanigitor;

/** Small helpers for untrusted text (repo data and model output). */
public final class Text {

    private Text() {
    }

    /**
     * Removes control characters (keeping newlines and tabs), trims, and cuts the text to at
     * most {@code max} characters, ending with an ellipsis when it was cut. Returns null for
     * null or blank input.
     */
    public static String clean(String text, int max) {
        if (text == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(Math.min(text.length(), max));
        text.codePoints()
                .filter(cp -> cp == '\n' || cp == '\t' || !Character.isISOControl(cp))
                .filter(cp -> Character.getType(cp) != Character.FORMAT || cp == 0x200D)
                .forEach(sb::appendCodePoint);
        String result = sb.toString().replace("\r", "").strip();
        if (result.isEmpty()) {
            return null;
        }
        if (result.length() > max) {
            result = result.substring(0, Math.max(0, max - 1)).stripTrailing() + "…";
        }
        return result;
    }

    /** The first line of {@code text}, cleaned and cut to {@code max} characters. */
    public static String firstLine(String text, int max) {
        String cleaned = clean(text, Integer.MAX_VALUE);
        if (cleaned == null) {
            return null;
        }
        int newline = cleaned.indexOf('\n');
        return clean(newline < 0 ? cleaned : cleaned.substring(0, newline), max);
    }
}
