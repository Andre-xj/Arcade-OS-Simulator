package scratch;

import java.awt.*;
import javax.swing.*;

public class CheckConsolasWidth {
    public static void main(String[] args) {
        JTextPane tp = new JTextPane();
        tp.setFont(new Font("Consolas", Font.PLAIN, 12));
        FontMetrics fm = tp.getFontMetrics(tp.getFont());
        String line = "╔════════════════════════════════════════════════════════════════════════╗";
        System.out.println("Line length: " + line.length());
        System.out.println("String width in Consolas 12: " + fm.stringWidth(line));
        System.out.println("charWidth in Consolas 12: " + fm.charWidth('═'));
        System.out.println("charWidth * 74: " + (fm.charWidth('═') * 74));
    }
}

