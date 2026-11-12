package arcadeos;

import java.awt.*;
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
    public static JButton createRetroButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_SM);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fg, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /** Large primary action button (e.g., SIGN IN, START MISSION) */
    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_MD);
        btn.setBackground(ACCENT_PINK);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

    /** Nav-bar small tab-style button */
    public static JButton createNavButton(String icon, String label, Color fg) {
        JButton btn = new JButton(icon + " " + label);
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
        JTable table = new JTable(model) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                    c.setForeground(TEXT_WHITE);
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
}
