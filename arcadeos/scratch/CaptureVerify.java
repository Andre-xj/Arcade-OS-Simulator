package arcadeos.scratch;

import arcadeos.ArcadeOS;
import arcadeos.RetroTheme;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class CaptureVerify {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                ArcadeOS frame = new ArcadeOS();
                frame.setSize(1280, 860);
                frame.setLocationRelativeTo(null);
                frame.showScreen(ArcadeOS.SCREEN_SIMULATOR);
                frame.setVisible(true);

                Timer t = new Timer(600, e -> {
                    try {
                        BufferedImage img = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = img.createGraphics();
                        frame.paint(g2);
                        g2.dispose();

                        File out = new File("C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/cpu_gui_verified.png");
                        ImageIO.write(img, "png", out);
                        System.out.println("CAPTURE_SAVED: " + out.getAbsolutePath());
                        System.exit(0);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.exit(1);
                    }
                });
                t.setRepeats(false);
                t.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        });
    }
}
