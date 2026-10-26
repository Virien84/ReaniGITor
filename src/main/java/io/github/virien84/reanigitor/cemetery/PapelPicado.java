package io.github.virien84.reanigitor.cemetery;

/**
 * A string of papel picado (cut tissue-paper flags) as inline SVG. Each flag is one path with
 * the even-odd fill rule: the skull, diamonds and dots are holes, and the skull's eyes, nose and
 * teeth are paper again inside the skull hole.
 */
final class PapelPicado {

    private static final String[] COLORS = {"#e4007c", "#f2a20c", "#14a3a0", "#7b3fa0", "#6cbf3a"};
    private static final int FLAGS = 13;
    private static final int STEP = 110;

    private PapelPicado() {
    }

    static String svg() {
        StringBuilder sb = new StringBuilder();
        int width = FLAGS * STEP + 10;
        sb.append("<svg class=\"papel\" viewBox=\"0 0 ").append(width)
                .append(" 150\" preserveAspectRatio=\"xMidYMin slice\" aria-hidden=\"true\" focusable=\"false\">")
                .append("<defs><path id=\"flag\" fill-rule=\"evenodd\" d=\"").append(flagPath()).append("\"/></defs>")
                .append("<path d=\"M0 8 Q").append(width / 2).append(" 22 ").append(width)
                .append(" 8\" fill=\"none\" stroke=\"#b8a7c7\" stroke-width=\"2\"/>");
        for (int i = 0; i < FLAGS; i++) {
            int x = 10 + i * STEP;
            // Follow the sag of the string: lower in the middle.
            double t = (x + 50.0) / width;
            int y = (int) Math.round(8 + 4 * 14 * t * (1 - t));
            int tilt = (i % 3) - 1;
            sb.append("<use href=\"#flag\" fill=\"").append(COLORS[i % COLORS.length])
                    .append("\" transform=\"translate(").append(x).append(' ').append(y)
                    .append(") rotate(").append(tilt).append(" 50 0)\"/>");
        }
        return sb.append("</svg>").toString();
    }

    private static String flagPath() {
        StringBuilder d = new StringBuilder();
        // The paper, with a zigzag bottom edge.
        d.append("M0 0H100V112L90 124L80 112L70 124L60 112L50 124L40 112L30 124L20 112L10 124L0 112Z");
        // A row of small dots along the top.
        for (int x = 14; x <= 86; x += 12) {
            circle(d, x, 12, 3);
        }
        // Diamonds in the corners.
        for (int[] p : new int[][] {{16, 34}, {84, 34}, {16, 96}, {84, 96}}) {
            d.append("M").append(p[0]).append(' ').append(p[1] - 7)
                    .append("l7 7l-7 7l-7 -7z");
        }
        // The skull: a hole shaped like a head and jaw...
        d.append("M31 62A20 20 0 1 1 69 62L65 72V86Q65 92 59 92H41Q35 92 35 86V72Z");
        // ...with paper eyes, nose and teeth inside it.
        circle(d, 42, 60, 6.5);
        circle(d, 58, 60, 6.5);
        d.append("M50 67l-3.5 7h7z");
        for (int x = 42; x <= 56; x += 5) {
            d.append("M").append(x).append(" 80h2v9h-2z");
        }
        return d.toString();
    }

    private static void circle(StringBuilder d, double cx, double cy, double r) {
        d.append("M").append(fmt(cx - r)).append(' ').append(fmt(cy))
                .append("a").append(fmt(r)).append(' ').append(fmt(r)).append(" 0 1 0 ").append(fmt(2 * r)).append(" 0")
                .append("a").append(fmt(r)).append(' ').append(fmt(r)).append(" 0 1 0 ").append(fmt(-2 * r)).append(" 0z");
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : Double.toString(v);
    }
}
