package game.snake;

import java.awt.Color;

/**
 * B1: Shared visual tokens and sentence templates used by both the game panel
 * (SnakeGamePanel) and the manual simulator (SegmentationPanel).
 *
 * This class has no Swing dependencies — it is pure data and strings — so it
 * can safely be imported from the arcadeos package without coupling the two
 * subsystems any further.
 */
public final class SegmentStyle {

    private SegmentStyle() {}

    // ── Colour palette (index → segment colour, stable across the app) ───────
    private static final Color[] PALETTE = {
        new Color(80,  190, 255),   // 0  cyan-blue
        new Color(255, 211,  25),   // 1  amber
        new Color( 57, 255,  20),   // 2  neon green
        new Color(255,  42, 141),   // 3  hot pink
        new Color(255, 130,  60),   // 4  orange
        new Color(160, 100, 255),   // 5  violet
        new Color(  0, 230, 200),   // 6  teal
        new Color(255,  80,  80),   // 7  red
    };

    /**
     * Returns the stable palette colour for a segment index.
     * Index &lt; 0 falls back to the first palette entry.
     */
    public static Color segmentColor(int index) {
        return index < 0 ? PALETTE[0] : PALETTE[index % PALETTE.length];
    }

    // ── Semantic colours ─────────────────────────────────────────────────────
    /** Shared green for a valid translation / OK result. */
    public static final Color VALID_COLOR = new Color(57, 255, 20);
    /** Shared red for a trap / fault result. */
    public static final Color TRAP_COLOR  = new Color(255, 50, 80);

    // ── Sentence templates (B4: identical wording in both game and simulator) ─

    /**
     * One-line sentence for a successful translation, e.g.:
     * "offset 79 &lt; limit 400 → PA = 1400 + 79 = 1479  [OK]"
     */
    public static String validSentence(int offset, int limit, int base) {
        return "offset " + offset + " < limit " + limit
                + " -> PA = " + base + " + " + offset + " = " + (base + offset) + "  [OK]";
    }

    /**
     * One-line sentence for a trap, e.g.:
     * "offset 500 >= limit 400 → SEGMENTATION FAULT  [TRAP]"
     */
    public static String trapSentence(int offset, int limit) {
        return "offset " + offset + " >= limit " + limit + " -> SEGMENTATION FAULT  [TRAP]";
    }

    /**
     * Short form used in the kernel log: "malloc +1 @ offset O → PA PA (base B + O)".
     */
    public static String mallocLogLine(int offset, int base) {
        int pa = base + offset;
        return "malloc +1 @ offset " + offset + " -> PA " + pa
                + " (base " + base + " + " + offset + ")";
    }

    /**
     * Short form used in the kernel fault log.
     */
    public static String faultLogLine(int offset, int limit) {
        return "SEGFAULT: offset " + offset + " >= limit " + limit;
    }
}
