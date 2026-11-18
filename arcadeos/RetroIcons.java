package arcadeos;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.Icon;

/**
 * 100% Vector-rendered retro arcade icons for Swing components.
 * Zero font dependencies, zero missing glyph boxes (⎕), crisp rendering at any DPI.
 */
public class RetroIcons {

    public static Icon createPlayIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color != null ? color : c.getForeground());
                
                int pad = Math.max(2, size / 5);
                Path2D path = new Path2D.Double();
                path.moveTo(x + pad, y + pad);
                path.lineTo(x + size - pad, y + size / 2.0);
                path.lineTo(x + pad, y + size - pad);
                path.closePath();
                g2.fill(path);
                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon createPauseIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color != null ? color : c.getForeground());

                int barW = Math.max(2, size / 4);
                int h = size - 6;
                g2.fillRoundRect(x + 2, y + 3, barW, h, 2, 2);
                g2.fillRoundRect(x + size - barW - 2, y + 3, barW, h, 2, 2);
                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon createStepIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color != null ? color : c.getForeground());

                int pad = Math.max(2, size / 6);
                int midX = x + (size * 2 / 3);
                Path2D tri = new Path2D.Double();
                tri.moveTo(x + pad, y + pad);
                tri.lineTo(midX - 1, y + size / 2.0);
                tri.lineTo(x + pad, y + size - pad);
                tri.closePath();
                g2.fill(tri);

                g2.fillRect(midX + 1, y + pad, Math.max(2, size / 7), size - 2 * pad);
                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon createResetIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color != null ? color : c.getForeground());

                int pad = 3;
                int arcSize = size - 2 * pad;
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(x + pad, y + pad, arcSize, arcSize, 45, 270);

                // Arrow head at 45 deg
                int tipX = x + pad + arcSize / 2 + 2;
                int tipY = y + pad;
                Path2D arrow = new Path2D.Double();
                arrow.moveTo(tipX, tipY - 3);
                arrow.lineTo(tipX + 4, tipY + 2);
                arrow.lineTo(tipX - 2, tipY + 3);
                arrow.closePath();
                g2.fill(arrow);
                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon createAddIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color != null ? color : c.getForeground());

                int stroke = Math.max(2, size / 6);
                int midX = x + size / 2;
                int midY = y + size / 2;
                int span = size / 2 - 2;

                g2.fillRect(midX - stroke / 2, y + 2, stroke, size - 4);
                g2.fillRect(x + 2, midY - stroke / 2, size - 4, stroke);
                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon createDeleteIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color != null ? color : c.getForeground());
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int pad = 3;
                g2.drawLine(x + pad, y + pad, x + size - pad, y + size - pad);
                g2.drawLine(x + size - pad, y + pad, x + pad, y + size - pad);
                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    public static Icon createCompareIcon(int size, Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color != null ? color : c.getForeground());

                int barH = Math.max(2, size / 6);
                int w = size - 4;
                g2.fillRect(x + 2, y + 2, w * 3 / 4, barH);
                g2.fillRect(x + 2, y + 2 + barH + 2, w, barH);
                g2.fillRect(x + 2, y + 2 + (barH + 2) * 2, w / 2, barH);
                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }
}

