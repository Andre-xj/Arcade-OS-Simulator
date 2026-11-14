package arcadeos.scratch;

import arcadeos.CPUSchedulingPanel;
import javax.swing.*;

public class TestAutoRunComplete {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CPUSchedulingPanel panel = new CPUSchedulingPanel();
            panel.setSize(1200, 800);

            // Access arena tick for 300 frames simulating auto-solve at 4X speed
            CPUSchedulingPanel.KernelTapperArena arena = null;
            for (java.awt.Component c : panel.getComponents()) {
                if (c instanceof JPanel) {
                    for (java.awt.Component c2 : ((JPanel) c).getComponents()) {
                        if (c2 instanceof CPUSchedulingPanel.KernelTapperArena) {
                            arena = (CPUSchedulingPanel.KernelTapperArena) c2;
                        }
                    }
                }
            }

            if (arena != null) {
                for (int f = 0; f < 300; f++) {
                    arena.tick(true, 4);
                }
                System.out.println("TICK_TEST_SUCCESS! SimTime=" + arena.getSimTime());
            } else {
                System.out.println("ARENA_NOT_FOUND");
            }
            System.exit(0);
        });
    }
}
