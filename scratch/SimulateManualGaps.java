package scratch;

import arcadeos.ArchiveManualData;
import arcadeos.ArchiveManualDialog;
import arcadeos.RetroTheme;
import java.awt.*;
import javax.swing.*;

public class SimulateManualGaps {
    public static void main(String[] args) throws Exception {
        RetroTheme.applyGlobalUIDefaults();
        SwingUtilities.invokeAndWait(() -> {
            ArchiveManualDialog dlg = new ArchiveManualDialog(null, null, 0);
            dlg.pack();
            dlg.setSize(840, 580);
            dlg.doLayout();
            
            dumpComponent(dlg.getContentPane(), 0);
            dlg.dispose();
            System.exit(0);
        });
    }

    static void dumpComponent(Component c, int depth) {
        if (c instanceof JTextPane) {
            JTextPane tp = (JTextPane) c;
            int w = tp.getWidth();
            Insets in = tp.getInsets();
            Font consolas = new Font("Consolas", Font.PLAIN, 12);
            FontMetrics fm = tp.getFontMetrics(consolas);
            int textW = fm.stringWidth("╔════════════════════════════════════════════════════════════════════════╗");
            int leftGap = in.left;
            int rightGap = w - (in.left + textW);
            System.out.println("JTextPane: w=" + w + " textW=" + textW + " Insets=" + in + " -> LeftGap=" + leftGap + " RightGap=" + rightGap + " Diff=" + Math.abs(leftGap - rightGap));
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                dumpComponent(child, depth + 1);
            }
        }
    }
}
