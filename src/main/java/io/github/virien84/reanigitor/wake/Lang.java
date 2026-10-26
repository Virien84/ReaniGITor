package io.github.virien84.reanigitor.wake;

/** The language of the calaveritas. Everything else is written in English. */
public enum Lang {
    EN("en", "English"),
    ES("es", "Mexican Spanish");

    private final String code;
    private final String language;

    Lang(String code, String language) {
        this.code = code;
        this.language = language;
    }

    public String code() {
        return code;
    }

    public String language() {
        return language;
    }

    /** @return the language for {@code code} ("en" or "es"), or null when there is none */
    public static Lang fromCode(String code) {
        for (Lang lang : values()) {
            if (lang.code.equalsIgnoreCase(code)) {
                return lang;
            }
        }
        return null;
    }
}
