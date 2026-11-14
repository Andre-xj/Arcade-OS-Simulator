package scratch;

import arcadeos.RetroTheme;
import java.awt.*;
import javax.swing.*;

public class TestFontMetrics {
    public static void main(String[] args) {
        RetroTheme.applyGlobalUIDefaults();
        JTextPane pane = new JTextPane();
        
        Font defaultFont = pane.getFont();
        Font consolasFont = new Font("Consolas", Font.PLAIN, 12);
        
        String boxLine = "╔════════════════════════════════════════════════════════════════════════╗";
        
        FontMetrics fmDefault = pane.getFontMetrics(defaultFont);
        FontMetrics fmConsolas = pane.getFontMetrics(consolasFont);
        
        System.out.println("Default font: " + defaultFont);
        System.out.println("Default font stringWidth: " + fmDefault.stringWidth(boxLine));
        System.out.println("Consolas font stringWidth: " + fmConsolas.stringWidth(boxLine));
    }
}

