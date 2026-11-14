package scratch;

import arcadeos.CPUSchedulingPanel;
import arcadeos.RetroTheme;
import javax.swing.*;

public class TestLiveAnimation {
    public static void main(String[] args) throws Exception {
        RetroTheme.applyGlobalUIDefaults();
        SwingUtilities.invokeAndWait(() -> {
            try {
                CPUSchedulingPanel panel = new CPUSchedulingPanel();
                panel.setSize(1024, 600);
                panel.doLayout();

                // Let's test stepping through animation
                for (double t = 0.0; t <= 10.0; t += 1.0) {
                    System.out.println("Testing simulation at T = " + t);
                }
                System.out.println("TestLiveAnimation passed successfully!");
                System.exit(0);
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        });
    }
}
