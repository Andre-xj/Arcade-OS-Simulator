package scratch;

import java.awt.*;
import javax.swing.*;

public class Measure {
    public static void main(String[] args) {
        Font f = new Font("Consolas", Font.PLAIN, 12);
        Canvas c = new Canvas();
        FontMetrics fm = c.getFontMetrics(f);
        int charW = fm.charWidth('A');
        int strW = fm.stringWidth("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("Char width: " + charW);
        System.out.println("String width (74 chars): " + strW);
    }
}
