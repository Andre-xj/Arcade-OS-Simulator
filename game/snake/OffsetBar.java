package game.snake;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * B3: A reusable thermometer bar that shows an offset inside a [0, limit) range.
 *
 * <ul>
 *   <li>Valid zone (0 .. limit-1) is drawn in {@link SegmentStyle#VALID_COLOR}.</li>
 *   <li>Trap zone (limit .. maximum) is drawn in {@link SegmentStyle#TRAP_COLOR} and
 *       diagonally hatched.</li>
 *   <li>A white horizontal line marks the current offset.</li>
 *   <li>The limit boundary is a red horizontal line at the top of the valid zone.</li>
 *   <li>Labels "BASE" and "LMT" are drawn in a small helper font beside the bar.</li>
 * </ul>
 *
 * The bar is vertical by default (tall and narrow). Call {@link #setOffset(int)} to
 * update the pointer position; the component repaints itself automatically.
 */
public final class OffsetBar extends JComponent {

    // Visual constants
    private static final Color BG         = new Color(12, 9, 26);
    private static final Color BORDER     = new Color(48, 40, 84);
    private static final Color VALID_FILL = new Color(SegmentStyle.VALID_COLOR.getRed(),
                                                       SegmentStyle.VALID_COLOR.getGreen(),
                                                       SegmentStyle.VALID_COLOR.getBlue(), 140);
    private static final Color TRAP_FILL  = new Color(SegmentStyle.TRAP_COLOR.getRed(),
                                                       SegmentStyle.TRAP_COLOR.getGreen(),
                                                       SegmentStyle.TRAP_COLOR.getBlue(), 90);
    private static final Color LIMIT_LINE = new Color(255, 70, 100);
    private static final Color POINTER    = Color.WHITE;

    private int offset  = 0;
    private int limit   = 400;
    private int maximum = 600;   // slider extends past limit for the fault demo

    /** Label drawn at the base of the bar (bottom). */
    private String labelBase = "BASE";
    /** Label drawn at the limit tick. */
    private String labelLimit = "LMT";

    public OffsetBar() {
        setPreferredSize(new Dimension(26, 120));
        setMinimumSize(new Dimension(16, 60));
        setOpaque(false);
    }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setOffset(int offset) {
        this.offset = offset;
        repaint();
    }

    public void setLimit(int limit) {
        this.limit = Math.max(1, limit);
        repaint();
    }

    public void setMaximum(int maximum) {
        this.maximum = Math.max(limit + 1, maximum);
        repaint();
    }

    public void setLabels(String base, String limit) {
        this.labelBase  = base  != null ? base  : "BASE";
        this.labelLimit = limit != null ? limit : "LMT";
        repaint();
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getOffset()  { return offset; }
    public int getLimit()   { return limit; }
    public int getMaximum() { return maximum; }

    /** True when the current offset is in the valid zone [0, limit). */
    public boolean isValid() { return offset >= 0 && offset < limit; }

    // ── Paint ─────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int W = getWidth();
        int H = getHeight();
        if (W < 4 || H < 4) { g2.dispose(); return; }

        // ── Background ──
        g2.setColor(BG);
        g2.fillRect(0, 0, W, H);
        g2.setColor(BORDER);
        g2.drawRect(0, 0, W - 1, H - 1);

        int innerH = H - 2;
        int innerW = W - 2;
        int max = Math.max(maximum, limit + 1);

        // ── Valid fill (from bottom up to the limit fraction) ──
        int limitY  = H - 2 - (int) Math.round((double) limit  / max * innerH);
        int offsetY = H - 2 - (int) Math.round((double) Math.max(0, offset) / max * innerH);

        // Valid zone (green, bottom to limitY)
        g2.setColor(VALID_FILL);
        g2.fillRect(1, limitY, innerW, H - 1 - limitY);

        // Trap zone (red hatch, limitY to top)
        g2.setColor(TRAP_FILL);
        g2.fillRect(1, 1, innerW, Math.max(0, limitY - 1));

        // Diagonal hatch over the trap zone
        g2.setColor(new Color(SegmentStyle.TRAP_COLOR.getRed(),
                               SegmentStyle.TRAP_COLOR.getGreen(),
                               SegmentStyle.TRAP_COLOR.getBlue(), 60));
        g2.setStroke(new BasicStroke(1f));
        int trapH = Math.max(0, limitY - 1);
        for (int d = -trapH; d < innerW + trapH; d += 5) {
            int x1 = 1 + d, y1 = 1;
            int x2 = 1 + d + trapH, y2 = 1 + trapH;
            g2.drawLine(Math.max(1, x1), y1, Math.min(W - 2, x2), y2);
        }
        g2.setStroke(new BasicStroke(1f));

        // ── Limit line (red at limitY) ──
        g2.setColor(LIMIT_LINE);
        g2.drawLine(0, limitY, W - 1, limitY);

        // ── Offset pointer (white horizontal line) ──
        int pY = clamp(offsetY, 1, H - 2);
        g2.setColor(POINTER);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(0, pY, W - 1, pY);
        g2.setStroke(new BasicStroke(1f));

        // ── Labels (outside the bar, to the right) — handled by parent layout ──

        g2.dispose();
    }

    // ── Tooltip override ─────────────────────────────────────────────────────

    @Override
    public String getToolTipText() {
        return "offset " + offset + " / limit " + limit
                + " -> PA = base + " + offset + (isValid() ? "  [OK]" : "  [TRAP]");
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
