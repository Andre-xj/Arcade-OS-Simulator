package scratch;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;

public class MeasureTextPane {
    public static void main(String[] args) throws Exception {
        JTextPane pane = new JTextPane();
        pane.setFont(new Font("Consolas", Font.PLAIN, 12));
        StyledDocument doc = pane.getStyledDocument();
        Style def = pane.addStyle("def", null);
        StyleConstants.setFontFamily(def, "Consolas");
        StyleConstants.setFontSize(def, 12);
        
        String sample = "╔════════════════════════════════════════════════════════════════════════╗";
        doc.insertString(0, sample, def);
        
        JFrame frame = new JFrame();
        frame.getContentPane().add(pane);
        frame.pack();
        
        // Find view width
        View v = pane.getUI().getRootView(pane);
        float w = v.getPreferredSpan(View.X_AXIS);
        System.out.println("View preferred span for 74 chars: " + w);
        System.out.println("FontMetrics stringWidth: " + pane.getFontMetrics(new Font("Consolas", Font.PLAIN, 12)).stringWidth(sample));
        System.exit(0);
    }
}

