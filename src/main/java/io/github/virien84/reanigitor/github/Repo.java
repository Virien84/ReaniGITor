package io.github.virien84.reanigitor.github;

import java.time.Instant;
import java.util.List;

/** A repository as listed by the GitHub API. Every text field is untrusted. */
public record Repo(
        String name,
        String fullName,
        String description,
        String htmlUrl,
        String language,
        int stars,
        int forks,
        int openIssues,
        Instant createdAt,
        Instant pushedAt,
        boolean fork,
        boolean archived,
        int sizeKb,
        List<String> topics,
        String license) {

    public Repo {
        topics = List.copyOf(topics);
    }

    /** When the repo last showed signs of life: its last push, or its creation if never pushed. */
    public Instant lastSignOfLife() {
        return pushedAt != null ? pushedAt : createdAt;
    }
}
