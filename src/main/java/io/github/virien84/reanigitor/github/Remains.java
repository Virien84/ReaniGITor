package io.github.virien84.reanigitor.github;

import java.time.Instant;
import java.util.List;

/**
 * Everything ReaniGITor digs up about one abandoned repo. Every text field is untrusted repo
 * data; the nullable ones are null when GitHub has nothing (no README, an empty repo).
 */
public record Remains(
        Repo repo,
        String readme,
        String lastCommitMessage,
        Instant lastCommitAt,
        List<String> openIssueTitles) {

    public Remains {
        openIssueTitles = List.copyOf(openIssueTitles);
    }
}
