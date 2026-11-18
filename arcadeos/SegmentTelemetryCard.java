package arcadeos;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Hardware Segment Registers & Allocation Telemetry Card for Mission 05.
 * Displays CPU segment registers (CS, DS, SS, HS), memory utilization meter,
 * and hardware isolation/overlap protection status.
 */
public class SegmentTelemetryCard extends JPanel {

    public static class SegRegInfo {
        public String tag;
        public String name;
        public int base;
        public int limit;
        public Color color;

        public SegRegInfo(String tag, String name, int base, int limit, Color color) {
            this.tag = tag;
            this.name = name;
            this.base = base;
            this.limit = limit;
            this.color = color;
        }
    }

    private final List<SegRegInfo> registerList = new ArrayList<>();
    private int totalAllocated = 1500;
    private int totalRam = 7000;
    private boolean hasOverlaps = false;

    public SegmentTelemetryCard() {
        setBackground(RetroTheme.PANEL_BG);
        setOpaque(true);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR), " [ HARDWARE SEGMENT REGISTERS & ALLOCATION ] ",
                TitledBorder.LEFT, TitledBorder.TOP, RetroTheme.RETRO_FONT_BOLD, RetroTheme.ACCENT_CYAN
        ));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(450, 135);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(320, 110);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 160);
    }

    public void updateTelemetry(int allocated, int ram, boolean overlaps, List<SegRegInfo> segs) {
        this.totalAllocated = Math.max(0, allocated);
        this.totalRam = Math.max(1, ram);
        this.hasOverlaps = overlaps;
        this.registerList.clear();
        if (segs != null) {
            this.registerList.addAll(segs);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int pad = 12;

        // 1. CPU Segment Registers Row (CS, DS, SS, HS or active segments)
        int regY = 22;
        int regH = 34;
        int cardCount = Math.max(1, Math.min(4, registerList.isEmpty() ? 4 : registerList.size()));
        int regGap = 8;
        int regW = (w - pad * 2 - regGap * (cardCount - 1)) / cardCount;

        String[] defaultTags = { "CS", "DS", "SS", "HS" };
        String[] defaultNames = { "Code", "Data", "Stack", "Heap" };
        Color[] defaultColors = {
                new Color(5, 217, 232),
                new Color(255, 42, 141),
                new Color(255, 211, 25),
                new Color(57, 255, 140)
        };

        for (int i = 0; i < cardCount; i++) {
            int rx = pad + i * (regW + regGap);
            String tag = (i < registerList.size()) ? registerList.get(i).tag : defaultTags[i];
            String name = (i < registerList.size()) ? registerList.get(i).name : defaultNames[i];
            int bVal = (i < registerList.size()) ? registerList.get(i).base : (i * 1500);
            int lVal = (i < registerList.size()) ? registerList.get(i).limit : 500;
            Color col = (i < registerList.size()) ? registerList.get(i).color : defaultColors[i];

            // Card background
            g2.setColor(new Color(22, 16, 42));
            g2.fill(new RoundRectangle2D.Float(rx, regY, regW, regH, 6, 6));

            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 120));
            g2.draw(new RoundRectangle2D.Float(rx, regY, regW, regH, 6, 6));

            // Tag badge pill
            g2.setColor(col);
            g2.fillRoundRect(rx + 4, regY + 4, 22, 12, 3, 3);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            g2.drawString(tag, rx + 6, regY + 13);

            // Name
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            String shortName = name.length() > 8 ? name.substring(0, 7) + ".." : name;
            g2.drawString(shortName, rx + 30, regY + 13);

            // Base and Limit info
            g2.setColor(new Color(185, 180, 215));
            g2.setFont(new Font("Consolas", Font.PLAIN, 9));
            g2.drawString("B:" + bVal + " L:" + lVal, rx + 6, regY + 27);
        }

        // 2. Physical RAM Allocation Bar
        int barY = regY + regH + 8;
        int barH = 16;
        int barW = w - pad * 2;

        // Dark track
        g2.setColor(new Color(14, 10, 26));
        g2.fill(new RoundRectangle2D.Float(pad, barY, barW, barH, 4, 4));

        // Draw allocated segment blocks inside bar
        int curBX = pad;
        if (!registerList.isEmpty()) {
            for (SegRegInfo r : registerList) {
                float segRatio = (float) r.limit / totalRam;
                int segBW = Math.max(4, (int) (segRatio * barW));
                if (curBX + segBW > pad + barW) segBW = pad + barW - curBX;
                if (segBW <= 0) break;

                g2.setColor(r.color);
                g2.fillRect(curBX, barY + 1, segBW, barH - 2);
                curBX += segBW;
            }
        } else {
            float allocRatio = Math.min(1.0f, (float) totalAllocated / totalRam);
            int allocBW = (int) (allocRatio * barW);
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.fillRect(pad, barY + 1, allocBW, barH - 2);
        }

        // Frame border
        g2.setColor(new Color(65, 48, 100));
        g2.draw(new RoundRectangle2D.Float(pad, barY, barW, barH, 4, 4));

        // Bar text overlay
        float pct = ((float) totalAllocated / totalRam) * 100f;
        String utilStr = String.format("RAM ALLOCATION: %d B / %d B (%.1f%% USED)  |  FREE: %d B",
                totalAllocated, totalRam, pct, Math.max(0, totalRam - totalAllocated));
        g2.setFont(new Font("Consolas", Font.BOLD, 9));
        g2.setColor(Color.WHITE);
        g2.drawString(utilStr, pad + 8, barY + 11);

        // 3. Hardware Isolation / Protection Status Row
        int statY = barY + barH + 16;
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        if (hasOverlaps) {
            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.drawString("[!] HARDWARE ALERT: SEGMENT ADDRESS RANGE OVERLAP DETECTED!", pad, statY);
        } else {
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString("[OK] HARDWARE ISOLATION SECURED: ZERO OVERLAPS  |  MMU BOUNDS TRAP: ARMED", pad, statY);
        }

        g2.dispose();
    }
}
