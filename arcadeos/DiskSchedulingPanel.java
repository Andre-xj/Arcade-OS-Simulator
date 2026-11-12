package arcadeos;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class DiskSchedulingPanel extends JPanel {
    private JTextField txtRequests, txtInitHead, txtDiskLimit;
    private JComboBox<String> algoCombo, dirCombo;
    private DefaultTableModel outModel;
    private JLabel lblHeadMovement;
    private JTextArea txtTrace;

    public DiskSchedulingPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        ctrl.setBackground(RetroTheme.PANEL_BG);
        ctrl.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        JLabel lblA = new JLabel("Algo:");
        lblA.setFont(RetroTheme.RETRO_FONT_BOLD);
        lblA.setForeground(RetroTheme.ACCENT_CYAN);
        ctrl.add(lblA);

        algoCombo = new JComboBox<>(new String[]{"FCFS", "SSTF", "SCAN", "C-SCAN", "LOOK", "C-LOOK"});
        algoCombo.setFont(RetroTheme.RETRO_FONT);
        algoCombo.setBackground(new Color(32, 24, 56));
        algoCombo.setForeground(RetroTheme.TEXT_WHITE);
        ctrl.add(algoCombo);

        JLabel lblH = new JLabel("Init Head:");
        lblH.setFont(RetroTheme.RETRO_FONT);
        lblH.setForeground(RetroTheme.TEXT_WHITE);
        ctrl.add(lblH);

        txtInitHead = RetroTheme.createRetroTextField("53", 4);
        ctrl.add(txtInitHead);

        JLabel lblD = new JLabel("Max Cylinder:");
        lblD.setFont(RetroTheme.RETRO_FONT);
        lblD.setForeground(RetroTheme.TEXT_WHITE);
        ctrl.add(lblD);

        txtDiskLimit = RetroTheme.createRetroTextField("199", 4);
        ctrl.add(txtDiskLimit);

        JLabel lblDir = new JLabel("Dir:");
        lblDir.setFont(RetroTheme.RETRO_FONT);
        lblDir.setForeground(RetroTheme.TEXT_WHITE);
        ctrl.add(lblDir);

        dirCombo = new JComboBox<>(new String[]{"UP (High)", "DOWN (Low)"});
        dirCombo.setFont(RetroTheme.RETRO_FONT);
        dirCombo.setBackground(new Color(32, 24, 56));
        dirCombo.setForeground(RetroTheme.TEXT_WHITE);
        ctrl.add(dirCombo);

        JLabel lblR = new JLabel("Requests:");
        lblR.setFont(RetroTheme.RETRO_FONT);
        lblR.setForeground(RetroTheme.TEXT_WHITE);
        ctrl.add(lblR);

        txtRequests = RetroTheme.createRetroTextField("98 183 37 122 14 124 65 67", 18);
        ctrl.add(txtRequests);

        JButton btnSmall = RetroTheme.createRetroButton("Preset: Small (5)", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnLarge = RetroTheme.createRetroButton("Preset: Large (12)", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnRun = RetroTheme.createRetroButton("▶ RUN LASER HEAD", RetroTheme.ACCENT_YELLOW, Color.BLACK);

        ctrl.add(btnSmall);
        ctrl.add(btnLarge);
        ctrl.add(btnRun);
        add(ctrl, BorderLayout.NORTH);

        JPanel outPanel = new JPanel(new GridLayout(1, 2, 8, 8));
        outPanel.setOpaque(false);

        JPanel leftOut = new JPanel(new BorderLayout(4, 4));
        leftOut.setBackground(RetroTheme.PANEL_BG);
        leftOut.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        lblHeadMovement = new JLabel(" Total Head Movement: -- Cylinders ");
        lblHeadMovement.setFont(new Font("Consolas", Font.BOLD, 13));
        lblHeadMovement.setForeground(RetroTheme.ACCENT_YELLOW);
        lblHeadMovement.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        leftOut.add(lblHeadMovement, BorderLayout.NORTH);

        outModel = new DefaultTableModel(new String[]{"Step", "From Cylinder", "To Cylinder", "Seek Distance"}, 0);
        JTable table = RetroTheme.createRetroTable(outModel);
        JScrollPane scroll = RetroTheme.createRetroScroll(table);
        leftOut.add(scroll, BorderLayout.CENTER);
        outPanel.add(leftOut);

        txtTrace = new JTextArea();
        txtTrace.setBackground(RetroTheme.INPUT_BG);
        txtTrace.setForeground(RetroTheme.TEXT_WHITE);
        txtTrace.setFont(RetroTheme.RETRO_FONT);
        txtTrace.setEditable(false);
        JScrollPane scrollLog = RetroTheme.createRetroScroll(txtTrace);
        scrollLog.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR), " [ DISK TRAVERSAL PATH ] ",
                TitledBorder.LEFT, TitledBorder.TOP, RetroTheme.RETRO_FONT_BOLD, RetroTheme.ACCENT_CYAN
        ));
        outPanel.add(scrollLog);

        add(outPanel, BorderLayout.CENTER);

        btnSmall.addActionListener(e -> {
            txtInitHead.setText("50");
            txtRequests.setText("90 120 30 70 140");
        });
        btnLarge.addActionListener(e -> {
            txtInitHead.setText("53");
            txtRequests.setText("98 183 37 122 14 124 65 67 15 190 40 85");
        });
        btnRun.addActionListener(e -> runDiskScheduling());
    }

    private void runDiskScheduling() {
        try {
            int initHead = Integer.parseInt(txtInitHead.getText().trim());
            int maxCyl = Integer.parseInt(txtDiskLimit.getText().trim());
            String[] tokens = txtRequests.getText().trim().split("\\s+");
            List<Integer> reqs = new ArrayList<>();
            for (String t : tokens) {
                int c = Integer.parseInt(t);
                if (c < 0 || c > maxCyl) throw new Exception("Request " + c + " out of disk bounds (0 - " + maxCyl + ")");
                reqs.add(c);
            }

            String algo = algoCombo.getSelectedItem().toString();
            boolean goUp = dirCombo.getSelectedItem().toString().startsWith("UP");
            List<Integer> seekSequence = new ArrayList<>();
            seekSequence.add(initHead);

            if (algo.equals("FCFS")) {
                seekSequence.addAll(reqs);
            } else if (algo.equals("SSTF")) {
                List<Integer> pool = new ArrayList<>(reqs);
                int current = initHead;
                while (!pool.isEmpty()) {
                    int closest = -1, minDiff = Integer.MAX_VALUE;
                    for (int r : pool) {
                        int diff = Math.abs(r - current);
                        if (diff < minDiff) { minDiff = diff; closest = r; }
                    }
                    seekSequence.add(closest);
                    current = closest;
                    pool.remove(Integer.valueOf(closest));
                }
            } else if (algo.equals("SCAN")) {
                List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
                for (int r : reqs) {
                    if (r < initHead) left.add(r);
                    else right.add(r);
                }
                Collections.sort(left);
                Collections.sort(right);
                if (goUp) {
                    seekSequence.addAll(right);
                    if (!left.isEmpty()) {
                        seekSequence.add(maxCyl);
                        Collections.reverse(left);
                        seekSequence.addAll(left);
                    }
                } else {
                    Collections.reverse(left);
                    seekSequence.addAll(left);
                    if (!right.isEmpty()) {
                        seekSequence.add(0);
                        seekSequence.addAll(right);
                    }
                }
            } else if (algo.equals("C-SCAN")) {
                List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
                for (int r : reqs) {
                    if (r < initHead) left.add(r);
                    else right.add(r);
                }
                Collections.sort(left);
                Collections.sort(right);
                if (goUp) {
                    seekSequence.addAll(right);
                    seekSequence.add(maxCyl);
                    seekSequence.add(0);
                    seekSequence.addAll(left);
                } else {
                    Collections.reverse(left);
                    seekSequence.add(0);
                    seekSequence.add(maxCyl);
                    Collections.reverse(right);
                    seekSequence.addAll(right);
                }
            } else if (algo.equals("LOOK")) {
                List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
                for (int r : reqs) {
                    if (r < initHead) left.add(r);
                    else right.add(r);
                }
                Collections.sort(left);
                Collections.sort(right);
                if (goUp) {
                    seekSequence.addAll(right);
                    Collections.reverse(left);
                    seekSequence.addAll(left);
                } else {
                    Collections.reverse(left);
                    seekSequence.addAll(right);
                }
            } else {
                List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
                for (int r : reqs) {
                    if (r < initHead) left.add(r);
                    else right.add(r);
                }
                Collections.sort(left);
                Collections.sort(right);
                if (goUp) {
                    seekSequence.addAll(right);
                    seekSequence.addAll(left);
                } else {
                    Collections.reverse(left);
                    Collections.reverse(right);
                    seekSequence.addAll(right);
                }
            }

            outModel.setRowCount(0);
            int totalMovement = 0;
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Algorithm: %s | Start Head: %d | Bounds: 0 to %d\n", algo, initHead, maxCyl));
            sb.append("Full Head Traversal Order:\n  ");

            for (int i = 0; i < seekSequence.size() - 1; i++) {
                int from = seekSequence.get(i);
                int to = seekSequence.get(i + 1);
                int dist = Math.abs(to - from);
                totalMovement += dist;
                outModel.addRow(new Object[]{i + 1, from, to, dist + " cyl"});
                sb.append(from).append(" -> ");
            }
            sb.append(seekSequence.get(seekSequence.size() - 1)).append("\n\n");
            sb.append("Total Head Movement = ").append(totalMovement).append(" Cylinders\n");
            sb.append(String.format("Average Seek Length = %.2f Cylinders", (double) totalMovement / reqs.size()));

            lblHeadMovement.setText(String.format(" Total Head Movement: %d Cylinders (Avg: %.2f) ",
                    totalMovement, (double) totalMovement / reqs.size()));
            txtTrace.setText(sb.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: Check numbers and verify head/requests <= max cylinder.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
