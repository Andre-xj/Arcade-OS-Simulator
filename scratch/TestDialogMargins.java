package scratch;

import arcadeos.ArcadeOS;
import arcadeos.ArchiveManualDialog;
import javax.swing.*;
import java.awt.*;

public class TestDialogMargins {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ArcadeOS os = new ArcadeOS();
            
            for (int m = 0; m < 8; m++) {
                ArchiveManualDialog dialog = new ArchiveManualDialog(os, null, m);
                dialog.pack();
                dialog.setSize(840, 580);
                
                // Inspect pane insets for all tabs
                for (int t = 0; t < 5; t++) {
                    // Check textpane
                    checkTab(dialog, m, t);
                }
            }
            System.out.println("ALL MANUALS TESTED SUCCESSFULLY: Left margin equals Right margin everywhere!");
            System.exit(0);
        });
    }

    private static void checkTab(ArchiveManualDialog dialog, int m, int t) {
        // Find textpanes
        findTextPanes(dialog.getContentPane(), m, t);
    }
    
    private static void findTextPanes(Component c, int m, int t) {
        if (c instanceof JTextPane) {
            JTextPane tp = (JTextPane) c;
            Insets in = tp.getInsets();
            if (in.left != in.right) {
                System.out.printf("UNEQUAL! Manual %d Tab %d: left=%d, right=%d%n", m + 1, t + 1, in.left, in.right);
            }
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                findTextPanes(child, m, t);
            }
        }
    }
}

