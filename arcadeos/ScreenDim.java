package arcadeos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;

/** Dims an owner window while a modal dialog is open. */
final class ScreenDim {
    private final JRootPane root;
    private final Component previousGlass;
    private final boolean previousGlassVisible;
    private final JComponent veil;

    ScreenDim(JFrame owner, int alpha) {
        root = owner.getRootPane();
        previousGlass = root.getGlassPane();
        previousGlassVisible = previousGlass.isVisible();
        veil = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(4, 2, 12, alpha));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        veil.addMouseListener(new MouseAdapter() { });
    }

    void on() {
        root.setGlassPane(veil);
        veil.setVisible(true);
    }

    void off() {
        veil.setVisible(false);
        if (root.getGlassPane() == veil) {
            root.setGlassPane(previousGlass);
            previousGlass.setVisible(previousGlassVisible);
        }
    }
}
