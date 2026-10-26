package io.github.virien84.reanigitor.wake;

import java.util.List;

/**
 * A calaverita literaria: a short satirical poem in which Death comes for the repo.
 * {@code fallback} is true when the model failed and the poem is a stock one.
 */
public record Calaverita(String title, List<String> verses, Lang lang, boolean fallback) {

    public Calaverita {
        verses = List.copyOf(verses);
    }
}
