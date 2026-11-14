package scratch;

import arcadeos.ArcadeOS;
import arcadeos.RetroTheme;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

public class CaptureNewGUI {
    public static void main(String[] args) throws Exception {
        RetroTheme.applyGlobalUIDefaults();
        SwingUtilities.invokeAndWait(() -> {
            try {
                ArcadeOS os = new ArcadeOS();
                os.setSize(1200, 720);
                os.showScreen(ArcadeOS.SCREEN_SIMULATOR);
                os.doLayout();
                os.validate();

                BufferedImage img = new BufferedImage(1200, 720, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = img.createGraphics();
                os.paint(g2);
                g2.dispose();

                ImageIO.write(img, "png", new File("scratch/new_gui_preview.png"));
                System.out.println("Preview saved to scratch/new_gui_preview.png");
                os.dispose();
                System.exit(0);
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        });
    }
}
