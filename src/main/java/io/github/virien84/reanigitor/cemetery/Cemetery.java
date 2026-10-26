package io.github.virien84.reanigitor.cemetery;

import io.github.virien84.reanigitor.github.Remains;
import io.github.virien84.reanigitor.github.Repo;
import io.github.virien84.reanigitor.wake.Grave;
import io.github.virien84.reanigitor.wake.Graveyard;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Draws the cemetery: one static HTML page with a tombstone per abandoned repo. Every piece of
 * text from GitHub or from the model goes through {@link #escape}; the page has no JavaScript
 * and a Content Security Policy that forbids it.
 */
public final class Cemetery {

    public static final String FILE_NAME = "index.html";

    private static final String CSP = "default-src 'none'; "
            + "style-src 'unsafe-inline' https://fonts.googleapis.com; "
            + "font-src https://fonts.gstatic.com; img-src data:; media-src 'self'; "
            + "base-uri 'none'; form-action 'none'";
    private static final String FONTS = "https://fonts.googleapis.com/css2"
            + "?family=Alegreya:ital,wght@0,400;0,700;1,400&family=Sancreek&display=swap";
    private static final Pattern GITHUB_URL = Pattern.compile("https://github\\.com/[A-Za-z0-9-]+/[A-Za-z0-9._-]+");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    static final int MIN_STONE = 240;
    static final int MAX_STONE = 420;
    static final int STONE_PER_YEAR = 18;

    private Cemetery() {
    }

    public static Path write(Graveyard graveyard, Path dir) throws IOException {
        Files.createDirectories(dir);
        Path file = dir.resolve(FILE_NAME);
        Files.writeString(file, render(graveyard), StandardCharsets.UTF_8);
        return file;
    }

    public static String render(Graveyard graveyard) {
        String user = escape(graveyard.user());
        StringBuilder html = new StringBuilder(32_000);
        html.append("""
                <!doctype html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <meta http-equiv="Content-Security-Policy" content="%s">
                <meta name="referrer" content="no-referrer">
                <title>Here lie the repos of %s</title>
                <meta name="description" content="A Día de los Muertos cemetery for the abandoned GitHub repos of %s.">
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link rel="stylesheet" href="%s">
                <style>
                %s</style>
                </head>
                <body>
                <header class="hero">
                %s
                <h1>Here lie the repos of <span class="user">%s</span></h1>
                <p class="lede">%s</p>
                </header>
                """.formatted(CSP, user, user, escape(FONTS), css(), PapelPicado.svg(), user, lede(graveyard)));

        if (graveyard.graves().isEmpty()) {
            html.append("<main class=\"empty\"><p>No abandoned repos. Every repo of ")
                    .append(user)
                    .append(" saw a push in the last ")
                    .append(graveyard.months())
                    .append(" months. Come back next Día de Muertos.</p></main>\n");
        } else {
            html.append("<main class=\"path\">\n");
            for (Grave grave : graveyard.graves()) {
                tomb(html, grave, graveyard.dugAt());
            }
            html.append("</main>\n");
        }

        html.append("""
                <footer>
                <p>Every word on these stones was written by %s running on the mourner's own \
                machine, with no cloud AI. The remains came from the public GitHub API, read only, \
                on %s.</p>
                <p>Dug up with <a href="https://github.com/Virien84/ReaniGITor">ReaniGITor</a>, \
                open source under the MIT license.</p>
                </footer>
                </body>
                </html>
                """.formatted(escape(graveyard.model()), escape(day(graveyard.dugAt()))));
        return html.toString();
    }

    private static String lede(Graveyard graveyard) {
        int count = graveyard.graves().size();
        String repos = count == 1 ? "One repo" : count + " repos";
        return repos + " went more than " + graveyard.months() + " months without a push. "
                + "Light a candle, read " + (count == 1 ? "its calaverita" : "their calaveritas")
                + ", and maybe bring one back from the dead.";
    }

    private static void tomb(StringBuilder html, Grave grave, Instant dugAt) {
        Remains remains = grave.remains();
        Repo repo = remains.repo();
        html.append("<article class=\"grave\" id=\"grave-").append(escape(repo.name()))
                .append("\" style=\"--stone-h: ").append(stoneHeight(repo.lastSignOfLife(), dugAt)).append("px\">\n");

        html.append("<div class=\"plot\">\n<div class=\"stone\">\n<p class=\"rip\">R.I.P.</p>\n<h2>")
                .append(escape(repo.name())).append("</h2>\n<p class=\"dates\">")
                .append(year(repo.createdAt())).append(" – ").append(year(repo.lastSignOfLife()))
                .append("</p>\n<p class=\"epitaph\">“").append(escape(grave.autopsy().epitaph())).append("”</p>\n</div>\n")
                .append(ofrenda(repo.name())).append("</div>\n");

        html.append("<div class=\"wake\">\n<section class=\"calaverita\" lang=\"")
                .append(grave.calaverita().lang().code()).append("\">\n<h3>")
                .append(escape(grave.calaverita().title())).append("</h3>\n<p class=\"verses\">");
        for (String verse : grave.calaverita().verses()) {
            html.append("<span>").append(escape(verse)).append("</span>");
        }
        html.append("</p>\n</section>\n");

        html.append("<section>\n<h3>Cause of death</h3>\n<p>")
                .append(escape(grave.autopsy().causeOfDeath())).append("</p>\n</section>\n");

        html.append("<section>\n<h3>How to bring it back</h3>\n<ol class=\"prophecy\">\n");
        for (String step : grave.autopsy().resurrection()) {
            html.append("<li>").append(escape(step)).append("</li>\n");
        }
        html.append("</ol>\n</section>\n");

        html.append("<p class=\"facts\">").append(facts(remains)).append("</p>\n");
        if (grave.autopsy().fallback() || grave.calaverita().fallback()) {
            html.append("<p class=\"stock\">The local model did not answer properly for this repo, ")
                    .append("so part of this stone carries a stock text.</p>\n");
        }
        html.append("</div>\n</article>\n");
    }

    private static String facts(Remains remains) {
        Repo repo = remains.repo();
        StringBuilder sb = new StringBuilder();
        sb.append(repo.language() == null ? "No language" : escape(repo.language()))
                .append(", ").append(plural(repo.stars(), "star"))
                .append(", ").append(plural(repo.openIssues(), "open issue")).append(".");
        if (remains.lastCommitMessage() != null) {
            sb.append(" Last words: “").append(escape(remains.lastCommitMessage())).append("”.");
        }
        if (repo.htmlUrl() != null && GITHUB_URL.matcher(repo.htmlUrl()).matches()) {
            sb.append(" <a href=\"").append(escape(repo.htmlUrl())).append("\" rel=\"noopener\">Visit ")
                    .append(escape(repo.name())).append(" on GitHub</a>");
        }
        return sb.toString();
    }

    /** Two to four candles between marigolds, varied per repo so the rows do not repeat. */
    private static String ofrenda(String seed) {
        int h = seed.hashCode() & 0x7fffffff;
        int candles = 2 + h % 3;
        StringBuilder sb = new StringBuilder("<div class=\"ofrenda\" aria-hidden=\"true\"><span class=\"flower\"></span>");
        for (int i = 0; i < candles; i++) {
            int height = 26 + (h >> (i * 4)) % 20;
            sb.append("<span class=\"candle\" style=\"--h: ").append(height).append("px\"></span>")
                    .append("<span class=\"flower\"></span>");
        }
        return sb.append("</div>\n").toString();
    }

    static int stoneHeight(Instant lastSignOfLife, Instant now) {
        if (lastSignOfLife == null) {
            return MIN_STONE;
        }
        long years = ChronoUnit.DAYS.between(lastSignOfLife, now) / 365;
        return (int) Math.max(MIN_STONE, Math.min(MAX_STONE, MIN_STONE + years * STONE_PER_YEAR));
    }

    private static String plural(int n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }

    private static String year(Instant instant) {
        return instant == null ? "?" : Integer.toString(LocalDate.ofInstant(instant, ZoneOffset.UTC).getYear());
    }

    private static String day(Instant instant) {
        return instant == null ? "an unknown day" : DAY.format(LocalDate.ofInstant(instant, ZoneOffset.UTC));
    }

    /** Escapes text for HTML element content and quoted attribute values. */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String css() {
        try (InputStream in = Cemetery.class.getResourceAsStream("/cemetery/cemetery.css")) {
            if (in == null) {
                throw new IllegalStateException("cemetery.css is missing from the jar");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
