package arcadeos;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;

public class RetroTheme {
    public static final Color BG_DARK = new Color(13, 11, 26);
    public static final Color PANEL_BG = new Color(24, 18, 43);
    public static final Color ACCENT_CYAN = new Color(5, 217, 232);
    public static final Color ACCENT_PINK = new Color(255, 42, 141);
    public static final Color ACCENT_YELLOW = new Color(255, 211, 25);
    public static final Color TEXT_WHITE = new Color(238, 240, 242);
    public static final Color BORDER_COLOR = new Color(53, 38, 82);
    public static final Font RETRO_FONT = new Font("Consolas", Font.PLAIN, 12);
    public static final Font RETRO_FONT_BOLD = new Font("Consolas", Font.BOLD, 12);

    public static JButton createRetroButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(RETRO_FONT_BOLD);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fg, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        return btn;
    }

    public static JTextField createRetroTextField(String text, int cols) {
        JTextField tf = new JTextField(text, cols);
        tf.setFont(RETRO_FONT);
        tf.setBackground(new Color(32, 24, 56));
        tf.setForeground(TEXT_WHITE);
        tf.setCaretColor(ACCENT_YELLOW);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        return tf;
    }

    public static JTable createRetroTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(new Color(20, 15, 36));
        table.setForeground(TEXT_WHITE);
        table.setFont(RETRO_FONT);
        table.setGridColor(BORDER_COLOR);
        table.setRowHeight(24);
        table.getTableHeader().setBackground(PANEL_BG);
        table.getTableHeader().setForeground(ACCENT_CYAN);
        table.getTableHeader().setFont(RETRO_FONT_BOLD);
        return table;
    }
}
