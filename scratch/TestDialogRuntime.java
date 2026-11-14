package scratch;

import arcadeos.ArcadeOS;
import arcadeos.ArchiveManualDialog;
import javax.swing.*;
import java.awt.*;

public class TestDialogRuntime {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ArcadeOS os = new ArcadeOS();
            ArchiveManualDialog dialog = new ArchiveManualDialog(os, null, 0);
            dialog.pack();
            dialog.setSize(840, 580);
            dialog.setVisible(false);
            
            // Measure viewport
            findComponents(dialog.getContentPane(), "");
            System.exit(0);
        });
    }

    private static void findComponents(Component c, String indent) {
        if (c instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) c;
            System.out.println(indent + "JScrollPane width: " + sp.getWidth() + ", viewport width: " + sp.getViewport().getWidth());
        } else if (c instanceof JTextPane) {
            JTextPane tp = (JTextPane) c;
            FontMetrics fm = tp.getFontMetrics(tp.getFont());
            int tw = fm.stringWidth("╔════════════════════════════════════════════════════════════════════════╗");
            System.out.println(indent + "JTextPane width: " + tp.getWidth() + ", insets: " + tp.getInsets() + ", textWidth: " + tw);
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                findComponents(child, indent + "  ");
            }
        }
    }
}

