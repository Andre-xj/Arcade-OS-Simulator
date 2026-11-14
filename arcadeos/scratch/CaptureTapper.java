package arcadeos.scratch;

import arcadeos.CPUSchedulingPanel;
import arcadeos.KernelTapperDialog;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

public class CaptureTapper {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                JFrame parent = new JFrame();
                parent.setSize(1000, 700);
                parent.setLocationRelativeTo(null);

                KernelTapperDialog dlg = new KernelTapperDialog(parent, "FCFS", new ArrayList<>());
                dlg.setLocationRelativeTo(null);

                Timer t = new Timer(600, e -> {
                    try {
                        BufferedImage img = new BufferedImage(dlg.getWidth(), dlg.getHeight(),
                                BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = img.createGraphics();
                        dlg.paint(g2);
                        g2.dispose();

                        File out = new File(
                                "C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/kernel_tapper_verified.png");
                        ImageIO.write(img, "png", out);
                        System.out.println("TAPPER_SAVED: " + out.getAbsolutePath());
                        System.exit(0);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.exit(1);
                    }
                });
                t.setRepeats(false);
                t.start();
                dlg.setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        });
    }
}
