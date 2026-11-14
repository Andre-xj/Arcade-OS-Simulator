package scratch;

import arcadeos.ArchiveManualDialog;
import javax.swing.*;
import java.awt.*;

public class MeasureDialog {
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setSize(840, 580);
        ArchiveManualDialog dialog = new ArchiveManualDialog(frame, 0);
        dialog.pack();
        dialog.setSize(840, 580);
        
        System.out.println("Dialog size: " + dialog.getSize());
        
        // Find JScrollPane and JTextPane
        findComponents(dialog.getContentPane(), "");
        System.exit(0);
    }
    
    private static void findComponents(Component c, String indent) {
        if (c instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) c;
            System.out.println(indent + "JScrollPane bounds: " + sp.getBounds() + ", viewport: " + sp.getViewport().getBounds());
        } else if (c instanceof JTextPane) {
            JTextPane tp = (JTextPane) c;
            System.out.println(indent + "JTextPane bounds: " + tp.getBounds() + ", insets: " + tp.getInsets());
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                findComponents(child, indent + "  ");
            }
        }
    }
}

