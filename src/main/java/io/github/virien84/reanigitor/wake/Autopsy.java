package io.github.virien84.reanigitor.wake;

import java.util.List;

/**
 * The coroner's report: cause of death, epitaph and a three-step resurrection prophecy.
 * {@code fallback} is true when the model failed and the text is a stock one.
 */
public record Autopsy(String causeOfDeath, String epitaph, List<String> resurrection, boolean fallback) {

    public Autopsy {
        resurrection = List.copyOf(resurrection);
    }
}
