package io.github.virien84.reanigitor.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class AbandonmentPolicyTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC);

    private static Repo repo(String name, String pushedAt, boolean fork) {
        return new Repo(name, "ghost/" + name, null, "https://github.com/ghost/" + name, null,
                0, 0, 0, Instant.parse("2015-01-01T00:00:00Z"), Instant.parse(pushedAt),
                fork, false, 1, List.of(), null);
    }

    private final AbandonmentPolicy policy = new AbandonmentPolicy(12, NOW);

    @Test
    void aRepoWithoutPushesForLongerThanTheThresholdIsAbandoned() {
        assertTrue(policy.isAbandoned(repo("old", "2025-10-25T12:00:00Z", false)));
    }

    @Test
    void aRepoPushedWithinTheThresholdIsAlive() {
        assertFalse(policy.isAbandoned(repo("alive", "2025-10-27T12:00:00Z", false)));
    }

    @Test
    void forksAreNeverOurDeadToBury() {
        assertFalse(policy.isAbandoned(repo("fork", "2010-01-01T00:00:00Z", true)));
    }

    @Test
    void theProfileReadmeRepoIsNotAProjectToBury() {
        assertFalse(policy.isAbandoned(repo("Ghost", "2010-01-01T00:00:00Z", false)));
    }

    @Test
    void selectsTheLongestDeadFirstUpToTheLimit() {
        List<Repo> repos = List.of(
                repo("recent-dead", "2024-06-01T00:00:00Z", false),
                repo("alive", "2026-10-01T00:00:00Z", false),
                repo("ancient", "2016-03-01T00:00:00Z", false),
                repo("old", "2020-01-01T00:00:00Z", false));

        List<Repo> chosen = policy.select(repos, 2);

        assertEquals(List.of("ancient", "old"), chosen.stream().map(Repo::name).toList());
    }

    @Test
    void thresholdMustBePositive() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new AbandonmentPolicy(0, NOW));
    }
}
