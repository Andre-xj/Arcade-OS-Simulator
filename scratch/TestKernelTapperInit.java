package scratch;

import arcadeos.CPUSchedulingPanel;
import arcadeos.KernelTapperDialog;
import arcadeos.RetroTheme;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class TestKernelTapperInit {
    public static void main(String[] args) throws Exception {
        RetroTheme.applyGlobalUIDefaults();
        SwingUtilities.invokeAndWait(() -> {
            List<CPUSchedulingPanel.SimJob> jobs = new ArrayList<>();
            jobs.add(new CPUSchedulingPanel.SimJob("J1", 0.0, 3.0, 1));
            jobs.add(new CPUSchedulingPanel.SimJob("J2", 2.0, 4.0, 1));

            KernelTapperDialog dlg = new KernelTapperDialog(null, "FCFS", jobs);
            dlg.pack();
            System.out.println("KernelTapperDialog created and packed successfully: " + dlg.getTitle());
            dlg.dispose();
            System.exit(0);
        });
    }
}
