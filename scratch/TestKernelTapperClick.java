package scratch;

import arcadeos.ArcadeOS;
import arcadeos.CPUSchedulingPanel;
import arcadeos.KernelTapperDialog;
import arcadeos.RetroTheme;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class TestKernelTapperClick {
    public static void main(String[] args) {
        RetroTheme.applyGlobalUIDefaults();
        SwingUtilities.invokeLater(() -> {
            try {
                ArcadeOS os = new ArcadeOS();
                os.setVisible(true);
                os.showScreen(ArcadeOS.SCREEN_SIMULATOR);

                // Let's find CPUSchedulingPanel
                // And test opening KernelTapperDialog
                List<CPUSchedulingPanel.SimJob> jobs = new ArrayList<>();
                jobs.add(new CPUSchedulingPanel.SimJob("J1", 0.0, 3.0, 1));
                KernelTapperDialog dlg = new KernelTapperDialog(os, "FCFS", jobs);
                System.out.println("Dialog created successfully");

                // Close after 2 seconds
                new Timer(2000, e -> {
                    dlg.dispose();
                    os.dispose();
                    System.out.println("Test completed without error");
                    System.exit(0);
                }).start();

                dlg.setVisible(true);
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        });
    }
}
