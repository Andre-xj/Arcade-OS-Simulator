package scratch;

import arcadeos.ArchiveManualDialog;
import arcadeos.RetroTheme;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

public class TestDialogRender {
    public static void main(String[] args) throws Exception {
        RetroTheme.applyGlobalUIDefaults();
        SwingUtilities.invokeAndWait(() -> {
            try {
                ArchiveManualDialog dlg = new ArchiveManualDialog(null, null, 0);
                dlg.setSize(860, 600);
                dlg.validate();
                dlg.setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
