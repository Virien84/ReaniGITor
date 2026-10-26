package io.github.virien84.reanigitor.cemetery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.virien84.reanigitor.github.Remains;
import io.github.virien84.reanigitor.github.Repo;
import io.github.virien84.reanigitor.wake.Autopsy;
import io.github.virien84.reanigitor.wake.Calaverita;
import io.github.virien84.reanigitor.wake.Grave;
import io.github.virien84.reanigitor.wake.Graveyard;
import io.github.virien84.reanigitor.wake.Lang;
import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CemeteryTest {

    private static final String EVIL = "<script>alert('boo')</script>\" onload=\"x";

    static Grave grave(String name, String pushedAt, String url, String text, boolean fallback) {
        Repo repo = new Repo(name, "ghost/" + name, text, url, "Java", 3, 1, 2,
                Instant.parse("2019-02-03T00:00:00Z"), Instant.parse(pushedAt),
                false, false, 120, List.of(), "MIT");
        Remains remains = new Remains(repo, text, text, Instant.parse(pushedAt), List.of(text));
        Autopsy autopsy = new Autopsy("Acute " + text, "Here lies " + text, List.of("One " + text, "Two", "Three"), fallback);
        Calaverita poem = new Calaverita("Poem " + text, List.of("Line 1 " + text, "Line 2", "Line 3", "Line 4"),
                Lang.ES, fallback);
        return new Grave(remains, autopsy, poem);
    }

    private static Graveyard graveyard(Grave... graves) {
        return new Graveyard("ghost", Instant.parse("2026-10-26T12:00:00Z"), 12, "gemma4:e4b-it-qat", Lang.ES,
                List.of(graves));
    }

    @Test
    void escapesEverythingThatComesFromReposAndTheModel() {
        String html = Cemetery.render(graveyard(
                grave("tomb", "2020-01-01T00:00:00Z", "https://github.com/ghost/tomb", EVIL, false)));

        assertFalse(html.contains("<script"), "no script tags at all");
        assertFalse(html.contains("onload=\"x"), html);
        assertTrue(html.contains("&lt;script&gt;alert(&#39;boo&#39;)&lt;/script&gt;&quot; onload=&quot;x"));
    }

    @Test
    void hasAStrictContentSecurityPolicy() {
        String html = Cemetery.render(graveyard());
        assertTrue(html.contains("http-equiv=\"Content-Security-Policy\""), html);
        assertTrue(html.contains("default-src 'none'"), html);
        assertFalse(html.contains("script-src"), "scripts are never allowed");
    }

    @Test
    void linksOnlyToGitHub() {
        String html = Cemetery.render(graveyard(
                grave("good", "2020-01-01T00:00:00Z", "https://github.com/ghost/good", "x", false),
                grave("evil", "2020-01-01T00:00:00Z", "javascript:alert(1)", "x", false)));

        assertTrue(html.contains("href=\"https://github.com/ghost/good\""), html);
        assertFalse(html.contains("javascript:"), html);
    }

    @Test
    void longerDeadReposGetTallerStones() {
        String html = Cemetery.render(graveyard(
                grave("ancient", "2014-01-01T00:00:00Z", "https://github.com/ghost/ancient", "x", false),
                grave("recent", "2025-06-01T00:00:00Z", "https://github.com/ghost/recent", "x", false)));

        assertTrue(stoneHeight(html, "ancient") > stoneHeight(html, "recent"));
    }

    @Test
    void marksTheCalaveritaLanguageAndStockTexts() {
        String html = Cemetery.render(graveyard(
                grave("tomb", "2020-01-01T00:00:00Z", "https://github.com/ghost/tomb", "x", true)));

        assertTrue(html.contains("lang=\"es\""), html);
        assertTrue(html.contains("stock"), html);
    }

    @Test
    void anEmptyCemeteryInvitesToComeBack() {
        String html = Cemetery.render(graveyard());
        assertTrue(html.contains("No abandoned repos"), html);
    }

    @Test
    void escapeCoversTheFiveHtmlCharacters() {
        assertEquals("&lt;a href=&quot;x&quot;&gt;Tom &amp; Jerry&#39;s&lt;/a&gt;",
                Cemetery.escape("<a href=\"x\">Tom & Jerry's</a>"));
        assertEquals("", Cemetery.escape(null));
    }

    private static int stoneHeight(String html, String name) {
        Matcher m = Pattern.compile("id=\"grave-" + name + "\"[^>]*--stone-h:\\s*(\\d+)px").matcher(html);
        assertTrue(m.find(), "stone height for " + name);
        return Integer.parseInt(m.group(1));
    }
}
