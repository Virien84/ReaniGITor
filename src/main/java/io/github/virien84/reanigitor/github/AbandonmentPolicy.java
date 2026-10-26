package io.github.virien84.reanigitor.github;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

/**
 * Decides which repos are dead: the user's own repos (not forks) with no push for more than a
 * number of months.
 */
public final class AbandonmentPolicy {

    private final int months;
    private final Instant cutoff;

    public AbandonmentPolicy(int months, Clock clock) {
        if (months <= 0) {
            throw new IllegalArgumentException("months must be positive: " + months);
        }
        this.months = months;
        this.cutoff = clock.instant().atZone(ZoneOffset.UTC).minusMonths(months).toInstant();
    }

    public int months() {
        return months;
    }

    public boolean isAbandoned(Repo repo) {
        Instant last = repo.lastSignOfLife();
        return !repo.fork() && last != null && last.isBefore(cutoff);
    }

    /** The abandoned repos, longest dead first, at most {@code max} of them. */
    public List<Repo> select(List<Repo> repos, int max) {
        return repos.stream()
                .filter(this::isAbandoned)
                .sorted(Comparator.comparing(Repo::lastSignOfLife))
                .limit(max)
                .toList();
    }
}
