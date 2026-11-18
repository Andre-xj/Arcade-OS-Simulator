package arcadeos;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class RetroTheme {
    // ── Core Palette ─────────────────────────────────────────────────────────
    public static final Color BG_DARK       = new Color(10, 8, 20);
    public static final Color BG_DEEPER     = new Color(6, 4, 14);
    public static final Color PANEL_BG      = new Color(20, 15, 38);
    public static final Color CARD_BG       = new Color(26, 18, 50);
    public static final Color ACCENT_CYAN   = new Color(5, 217, 232);
    public static final Color ACCENT_PINK   = new Color(255, 42, 141);
    public static final Color ACCENT_YELLOW = new Color(255, 211, 25);
    public static final Color ACCENT_GREEN  = new Color(57, 255, 20);
    public static final Color ACCENT_PURPLE = new Color(180, 0, 255);
    public static final Color TEXT_WHITE    = new Color(238, 240, 242);
    public static final Color TEXT_DIM      = new Color(140, 140, 170);
    public static final Color BORDER_COLOR  = new Color(53, 38, 82);
    public static final Color BORDER_BRIGHT = new Color(80, 55, 120);
    public static final Color BTN_DARK      = new Color(30, 20, 60);
    public static final Color BTN_PRESET    = new Color(35, 28, 65);
    public static final Color INPUT_BG      = new Color(14, 10, 30);
    public static final Color ROW_EVEN      = new Color(18, 13, 36);
    public static final Color ROW_ODD       = new Color(22, 16, 44);
    // Red tint used to flag conflicting table rows (e.g. overlapping segments).
    public static final Color OVERLAP_BG    = new Color(74, 16, 28);
    public static final Color OVERLAP_FG    = new Color(255, 138, 158);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    public static final Font FONT_XL    = new Font("Consolas", Font.BOLD, 22);
    public static final Font FONT_LG    = new Font("Consolas", Font.BOLD, 16);
    public static final Font FONT_MD    = new Font("Consolas", Font.BOLD, 13);
    public static final Font FONT_SM    = new Font("Consolas", Font.BOLD, 11);
    public static final Font FONT_XS    = new Font("Consolas", Font.BOLD, 9);
    public static final Font FONT_BODY  = new Font("Consolas", Font.PLAIN, 11);
    public static final Font FONT_MONO  = new Font("Consolas", Font.PLAIN, 10);
    // Legacy aliases kept for compatibility
    public static final Font RETRO_FONT      = FONT_BODY;
    public static final Font RETRO_FONT_BOLD = FONT_SM;

    // ── Button Factory ───────────────────────────────────────────────────────
    public static class ArcadeButton extends JButton {
        private Color baseBg;
        private Color baseFg;
        private boolean isHovered = false;
        private boolean isPressed = false;

        public ArcadeButton(String text, Color bg, Color fg) {
            super(text);
            this.baseBg = (bg != null) ? bg : BTN_DARK;
            this.baseFg = (fg != null) ? fg : TEXT_WHITE;
            setFont(new Font("Consolas", Font.BOLD, 11));
            setForeground(this.baseFg);
            setBackground(this.baseBg);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorderPainted(false);
            setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    isPressed = false;
                    repaint();
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEnabled()) {
                        isPressed = true;
                        repaint();
                    }
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressed = false;
                    repaint();
                }
            });
        }

        @Override
        public void setBackground(Color bg) {
            super.setBackground(bg);
            this.baseBg = bg;
            repaint();
        }

        @Override
        public void setForeground(Color fg) {
            super.setForeground(fg);
            this.baseFg = fg;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(Math.max(d.width + 4, 32), Math.max(d.height + 2, 26));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

            int w = getWidth();
            int h = getHeight();
            int arc = 8;
            int yOffset = isPressed ? 2 : 0;
            int plateH = h - 3;

            Color bg = (baseBg != null) ? baseBg : BTN_DARK;
            Color fg = (baseFg != null) ? baseFg : TEXT_WHITE;

            double lum = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255.0;
            boolean isLightBg = lum >= 0.45;

            // 1. Ambient Glow Halo on Hover
            if (isHovered && isEnabled()) {
                Color glowC = isLightBg ? bg : fg;
                g2.setColor(new Color(glowC.getRed(), glowC.getGreen(), glowC.getBlue(), 55));
                g2.fillRoundRect(0, 0, w, h, arc + 4, arc + 4);
            }

            // 2. 3D Bottom Bevel / Drop Shadow Lip
            int shadowY = yOffset + plateH;
            if (shadowY <= h) {
                Color shadowC = new Color(
                        Math.max(0, (int)(bg.getRed() * 0.40)),
                        Math.max(0, (int)(bg.getGreen() * 0.40)),
                        Math.max(0, (int)(bg.getBlue() * 0.40)),
                        240
                );
                g2.setColor(shadowC);
                g2.fillRoundRect(1, yOffset + 2, w - 2, h - yOffset - 2, arc, arc);
            }

            // 3. Saturated Arcade Gradient Plate
            Color topC, botC;
            if (isHovered && isEnabled()) {
                topC = new Color(
                        Math.min(255, (int)(bg.getRed() * 1.25 + 18)),
                        Math.min(255, (int)(bg.getGreen() * 1.25 + 18)),
                        Math.min(255, (int)(bg.getBlue() * 1.25 + 18))
                );
                botC = new Color(
                        Math.min(255, (int)(bg.getRed() * 0.95)),
                        Math.min(255, (int)(bg.getGreen() * 0.95)),
                        Math.min(255, (int)(bg.getBlue() * 0.95))
                );
            } else {
                topC = new Color(
                        Math.min(255, (int)(bg.getRed() * 1.10)),
                        Math.min(255, (int)(bg.getGreen() * 1.10)),
                        Math.min(255, (int)(bg.getBlue() * 1.10))
                );
                botC = new Color(
                        Math.max(0, (int)(bg.getRed() * 0.70)),
                        Math.max(0, (int)(bg.getGreen() * 0.70)),
                        Math.max(0, (int)(bg.getBlue() * 0.70))
                );
            }

            GradientPaint gp = new GradientPaint(0, yOffset, topC, 0, yOffset + plateH, botC);
            g2.setPaint(gp);
            g2.fillRoundRect(1, yOffset, w - 2, plateH, arc, arc);

            // 4. Top Glass / Gloss Reflection Highlight (Top 40%)
            int glossH = Math.max(3, plateH / 2 - 1);
            GradientPaint glossGp = new GradientPaint(
                    0, yOffset + 1, new Color(255, 255, 255, isLightBg ? 65 : 40),
                    0, yOffset + glossH, new Color(255, 255, 255, 4)
            );
            g2.setPaint(glossGp);
            g2.fillRoundRect(2, yOffset + 1, w - 4, glossH, arc - 2, arc - 2);

            // 5. Tactile 3D Border & Neon Edge
            if (isLightBg) {
                g2.setColor(isHovered ? Color.WHITE : new Color(15, 10, 25, 220));
                g2.setStroke(new BasicStroke(isHovered ? 1.8f : 1.2f));
            } else {
                Color borderC = isHovered ? fg : new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 160);
                g2.setColor(borderC);
                g2.setStroke(new BasicStroke(isHovered ? 1.8f : 1.2f));
            }
            g2.drawRoundRect(1, yOffset, w - 3, plateH - 1, arc, arc);

            // 6. Icon and Text Rendering with 3D Emboss
            Icon icon = getIcon();
            String text = getText();
            Font font = getFont();
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();

            int iconW = (icon != null) ? icon.getIconWidth() : 0;
            int gap = (icon != null && text != null && !text.isEmpty()) ? getIconTextGap() : 0;
            int textW = (text != null && !text.isEmpty()) ? fm.stringWidth(text) : 0;
            int totalContentW = iconW + gap + textW;

            int startX = (w - totalContentW) / 2;

            // Draw Icon
            if (icon != null) {
                int iconY = yOffset + (plateH - icon.getIconHeight()) / 2;
                icon.paintIcon(this, g2, startX, iconY);
            }

            // Draw Crisp Text (Clean, high-contrast, razor-sharp 8-bit text without muddy offset shadows)
            if (text != null && !text.isEmpty()) {
                int textX = startX + iconW + gap;
                int textY = yOffset + (plateH + fm.getAscent() - fm.getDescent()) / 2;

                g2.setColor(isLightBg ? new Color(15, 10, 25) : (isHovered ? Color.WHITE : fg));
                g2.drawString(text, textX, textY);
            }

            g2.dispose();
        }
    }

    public static JButton createRetroButton(String text, Color bg, Color fg) {
        return new ArcadeButton(text, bg, fg);
    }

    /** Large primary action button (e.g., SIGN IN, START MISSION) */
    public static JButton createPrimaryButton(String text) {
        ArcadeButton btn = new ArcadeButton(text, ACCENT_PINK, Color.WHITE);
        btn.setFont(FONT_MD);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        return btn;
    }

    /** Outlined secondary button */
    public static JButton createOutlineButton(String text, Color borderFg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_SM);
        btn.setBackground(new Color(0, 0, 0, 0));
        btn.setForeground(borderFg);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderFg, 1),
                BorderFactory.createEmptyBorder(5, 14, 5, 14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Nav-bar small tab-style button with Text/Emoji */
    public static JButton createNavButton(String icon, String label, Color fg) {
        JButton btn = new JButton(icon.isEmpty() ? label : (icon + " " + label));
        btn.setFont(FONT_XS);
        btn.setBackground(BTN_DARK);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Nav-bar small button with Pixel Icon sprite (guaranteed no broken glyphs!) */
    public static JButton createNavButton(Icon icon, String label, Color fg) {
        JButton btn = new JButton(label);
        if (icon != null) {
            btn.setIcon(icon);
            btn.setIconTextGap(4);
        }
        btn.setFont(FONT_XS);
        btn.setBackground(BTN_DARK);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── TextField Factory ─────────────────────────────────────────────────────
    public static JTextField createRetroTextField(String text, int cols) {
        JTextField tf = new JTextField(text, cols);
        tf.setFont(FONT_BODY);
        tf.setBackground(INPUT_BG);
        tf.setForeground(TEXT_WHITE);
        tf.setCaretColor(ACCENT_YELLOW);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return tf;
    }

    public static JPasswordField createRetroPasswordField(int cols) {
        JPasswordField pf = new JPasswordField(cols);
        pf.setFont(FONT_BODY);
        pf.setBackground(INPUT_BG);
        pf.setForeground(TEXT_WHITE);
        pf.setCaretColor(ACCENT_YELLOW);
        pf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return pf;
    }

    // ── Table Factory ─────────────────────────────────────────────────────────
    public static JTable createRetroTable(javax.swing.table.DefaultTableModel model) {
        return createRetroTable(model, null);
    }

    /**
     * Table variant that tints "flagged" rows red (e.g. segments whose
     * [base, base+limit) range overlaps another segment). Pass {@code null}
     * for {@code flaggedRow} to get the standard look.
     */
    public static JTable createRetroTable(javax.swing.table.DefaultTableModel model,
                                          java.util.function.IntPredicate flaggedRow) {
        JTable table = new JTable(model) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    if (flaggedRow != null && flaggedRow.test(row)) {
                        c.setBackground(OVERLAP_BG);
                        c.setForeground(OVERLAP_FG);
                    } else {
                        c.setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                        c.setForeground(TEXT_WHITE);
                    }
                } else {
                    c.setBackground(new Color(80, 30, 100));
                    c.setForeground(ACCENT_CYAN);
                }
                return c;
            }
        };
        table.setBackground(INPUT_BG);
        table.setForeground(TEXT_WHITE);
        table.setFont(FONT_BODY);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(26);
        table.setSelectionBackground(new Color(80, 30, 100));
        table.setSelectionForeground(ACCENT_CYAN);
        table.getTableHeader().setBackground(PANEL_BG);
        table.getTableHeader().setForeground(ACCENT_CYAN);
        table.getTableHeader().setFont(FONT_SM);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        return table;
    }

    // ── Label Helpers ─────────────────────────────────────────────────────────
    public static JLabel createLabel(String text, Font f, Color fg) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(f);
        lbl.setForeground(fg);
        return lbl;
    }

    public static JLabel createRetroLabel(String text, Color fg) {
        return createLabel(text, FONT_SM, fg);
    }

    // ── ComboBox Styling ─────────────────────────────────────────────────────
    public static void styleComboBox(JComboBox<?> combo) {
        combo.setFont(FONT_BODY);
        combo.setBackground(INPUT_BG);
        combo.setForeground(TEXT_WHITE);
    }

    // ── Titled Border ─────────────────────────────────────────────────────────
    public static Border createSectionBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                " " + title + " ",
                TitledBorder.LEFT, TitledBorder.TOP,
                FONT_XS, ACCENT_CYAN
        );
    }

    /** Glowing panel border (matte pink bottom) */
    public static Border createGlowBorder(Color color) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        );
    }

    // ── Standard ScrollPane ───────────────────────────────────────────────────
    public static JScrollPane createRetroScroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBackground(INPUT_BG);
        sp.getViewport().setBackground(INPUT_BG);
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        sp.getVerticalScrollBar().setBackground(PANEL_BG);
        sp.getHorizontalScrollBar().setBackground(PANEL_BG);
        return sp;
    }

    // ── UIManager Defaults ────────────────────────────────────────────────────
    public static void applyGlobalUIDefaults() {
        UIManager.put("TabbedPane.selected",           PANEL_BG);
        UIManager.put("TabbedPane.background",         BG_DARK);
        UIManager.put("TabbedPane.foreground",         TEXT_WHITE);
        UIManager.put("TabbedPane.tabAreaBackground",  BG_DARK);
        UIManager.put("ComboBox.background",           INPUT_BG);
        UIManager.put("ComboBox.foreground",           TEXT_WHITE);
        UIManager.put("ComboBox.selectionBackground",  ACCENT_PINK);
        UIManager.put("ComboBox.selectionForeground",  Color.WHITE);
        UIManager.put("ScrollBar.background",          PANEL_BG);
        UIManager.put("ScrollBar.thumb",               BORDER_COLOR);
        UIManager.put("ScrollBar.track",               BG_DARK);
        UIManager.put("ScrollBar.width",               8);
        UIManager.put("SplitPane.background",          BG_DARK);
        UIManager.put("SplitPane.dividerSize",         4);
        UIManager.put("OptionPane.background",         PANEL_BG);
        UIManager.put("OptionPane.messageForeground",  TEXT_WHITE);
        UIManager.put("Panel.background",              BG_DARK);
        UIManager.put("ToolTip.background",            CARD_BG);
        UIManager.put("ToolTip.foreground",            ACCENT_CYAN);
        UIManager.put("ToolTip.font",                  FONT_XS);
        UIManager.put("Table.alternateRowColor",       ROW_ODD);
    }

    /**
     * Calculates a dialog size that never exceeds the usable desktop bounds
     * (accounting for multi-monitors, taskbars, and display scaling).
     */
    public static Dimension getClampedDialogSize(Component parent, int targetW, int targetH) {
        GraphicsConfiguration gc = (parent != null) ? parent.getGraphicsConfiguration()
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle b = (gc != null) ? gc.getBounds() : new Rectangle(0, 0, 1024, 768);
        Insets insets = (gc != null) ? Toolkit.getDefaultToolkit().getScreenInsets(gc) : new Insets(0, 0, 0, 0);
        int maxW = Math.max(400, b.width - insets.left - insets.right - 20);
        int maxH = Math.max(300, b.height - insets.top - insets.bottom - 20);
        return new Dimension(Math.min(targetW, maxW), Math.min(targetH, maxH));
    }
}
