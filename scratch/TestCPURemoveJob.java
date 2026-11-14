package scratch;

import arcadeos.CPUSchedulingPanel;
import arcadeos.RetroTheme;
import javax.swing.*;

public class TestCPURemoveJob {
    public static void main(String[] args) throws Exception {
        RetroTheme.applyGlobalUIDefaults();
        SwingUtilities.invokeAndWait(() -> {
            CPUSchedulingPanel panel = new CPUSchedulingPanel();
            System.out.println("CPUSchedulingPanel initialized successfully.");
            System.exit(0);
        });
    }
}

