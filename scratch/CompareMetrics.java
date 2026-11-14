package scratch;

import java.awt.*;
import javax.swing.*;

public class CompareMetrics {
    public static void main(String[] args) {
        String boxLine = "╔════════════════════════════════════════════════════════════════════════╗";
        Font f = new Font("Consolas", Font.PLAIN, 12);
        Canvas c = new Canvas();
        FontMetrics fm = c.getFontMetrics(f);
        System.out.println("String len: " + boxLine.length());
        System.out.println("fm.stringWidth(boxLine): " + fm.stringWidth(boxLine));
        for (int i = 0; i < boxLine.length(); i++) {
            char ch = boxLine.charAt(i);
            if (i == 0 || i == 1 || i == boxLine.length() - 1) {
                System.out.println("char '" + ch + "' (\\u" + Integer.toHexString(ch) + ") width=" + fm.charWidth(ch));
            }
        }
    }
}

