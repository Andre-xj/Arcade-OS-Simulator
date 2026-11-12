package arcadeos;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class MultiprogrammingPanel extends JPanel {
    private DefaultTableModel procModel;
    private JProgressBar barCPU, barRAM;
    private JLabel lblStatus;
    private JTextArea txtLog;
    private JTextField txtTotalRam;

    public MultiprogrammingPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        ctrl.setBackground(RetroTheme.PANEL_BG);
        ctrl.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        JLabel lblR = new JLabel("Total System RAM (MB):");
        lblR.setFont(RetroTheme.RETRO_FONT_BOLD);
        lblR.setForeground(RetroTheme.ACCENT_CYAN);
        ctrl.add(lblR);

        txtTotalRam = RetroTheme.createRetroTextField("1024", 5);
        ctrl.add(txtTotalRam);

        JButton btnAdd = RetroTheme.createRetroButton("+ Add Program", new Color(40, 26, 74), RetroTheme.ACCENT_CYAN);
        JButton btnRemove = RetroTheme.createRetroButton("- Remove", new Color(40, 26, 74), RetroTheme.ACCENT_PINK);
        JButton btnSmall = RetroTheme.createRetroButton("Preset: 3 Games", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnLarge = RetroTheme.createRetroButton("Preset: 7 Games", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnSim = RetroTheme.createRetroButton("▶ RUN CONCURRENCY TICK", RetroTheme.ACCENT_YELLOW, Color.BLACK);

        ctrl.add(btnAdd);
        ctrl.add(btnRemove);
        ctrl.add(btnSmall);
        ctrl.add(btnLarge);
        ctrl.add(btnSim);
        add(ctrl, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 8, 8));
        center.setOpaque(false);

        procModel = new DefaultTableModel(new String[]{"Game / Program", "Mem Req (MB)", "CPU Share (%)", "State"}, 0);
        JTable table = RetroTheme.createRetroTable(procModel);
        JScrollPane scroll = RetroTheme.createRetroScroll(table);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR), " [ CONCURRENT GAME INSTANCES ] ",
                TitledBorder.LEFT, TitledBorder.TOP, RetroTheme.RETRO_FONT_BOLD, RetroTheme.ACCENT_CYAN
        ));
        center.add(scroll);

        JPanel statPanel = new JPanel(new BorderLayout(8, 8));
        statPanel.setBackground(RetroTheme.PANEL_BG);
        statPanel.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        JPanel bars = new JPanel(new GridLayout(4, 1, 4, 4));
        bars.setOpaque(false);
        bars.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel lblC = new JLabel("CPU Utilization Load:");
        lblC.setFont(RetroTheme.RETRO_FONT_BOLD);
        lblC.setForeground(RetroTheme.ACCENT_CYAN);
        bars.add(lblC);

        barCPU = new JProgressBar(0, 100);
        barCPU.setStringPainted(true);
        barCPU.setForeground(RetroTheme.ACCENT_PINK);
        bars.add(barCPU);

        JLabel lblM = new JLabel("RAM Memory Footprint:");
        lblM.setFont(RetroTheme.RETRO_FONT_BOLD);
        lblM.setForeground(RetroTheme.ACCENT_CYAN);
        bars.add(lblM);

        barRAM = new JProgressBar(0, 100);
        barRAM.setStringPainted(true);
        barRAM.setForeground(RetroTheme.ACCENT_YELLOW);
        bars.add(barRAM);

        statPanel.add(bars, BorderLayout.NORTH);

        lblStatus = new JLabel(" SYSTEM STATUS: Ready ");
        lblStatus.setFont(new Font("Consolas", Font.BOLD, 12));
        lblStatus.setForeground(RetroTheme.TEXT_WHITE);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 8));
        statPanel.add(lblStatus, BorderLayout.CENTER);

        txtLog = new JTextArea();
        txtLog.setBackground(RetroTheme.INPUT_BG);
        txtLog.setForeground(RetroTheme.TEXT_WHITE);
        txtLog.setFont(RetroTheme.RETRO_FONT);
        txtLog.setEditable(false);
        JScrollPane scrollLog = RetroTheme.createRetroScroll(txtLog);
        scrollLog.setPreferredSize(new Dimension(300, 180));
        statPanel.add(scrollLog, BorderLayout.SOUTH);

        center.add(statPanel);
        add(center, BorderLayout.CENTER);

        btnAdd.addActionListener(e -> procModel.addRow(new Object[]{"Game_" + (procModel.getRowCount() + 1), "256", "25", "Ready"}));
        btnRemove.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) procModel.removeRow(r);
        });
        btnSmall.addActionListener(e -> loadSmallPreset());
        btnLarge.addActionListener(e -> loadLargePreset());
        btnSim.addActionListener(e -> runMultiprogramming());

        loadSmallPreset();
    }

    private void loadSmallPreset() {
        txtTotalRam.setText("1024");
        procModel.setRowCount(0);
        procModel.addRow(new Object[]{"Pacman_Engine", "256", "30", "Ready"});
        procModel.addRow(new Object[]{"Sound_Synthesizer", "128", "20", "Ready"});
        procModel.addRow(new Object[]{"Attract_Mode_Demo", "384", "40", "Ready"});
    }

    private void loadLargePreset() {
        txtTotalRam.setText("2048");
        procModel.setRowCount(0);
        procModel.addRow(new Object[]{"Pacman_ROM",       "256", "20", "Ready"});
        procModel.addRow(new Object[]{"Galaga_ROM",       "384", "25", "Ready"});
        procModel.addRow(new Object[]{"DonkeyKong_ROM",   "512", "30", "Ready"});
        procModel.addRow(new Object[]{"Audio_DSP",        "128", "10", "Ready"});
        procModel.addRow(new Object[]{"Network_Scores",   "64",  "5",  "Ready"});
        procModel.addRow(new Object[]{"CRT_Renderer",     "400", "20", "Ready"});
        procModel.addRow(new Object[]{"Coin_Sensor_Poller","32",  "5",  "Ready"});
    }

    private void runMultiprogramming() {
        try {
            int totalRam = Integer.parseInt(txtTotalRam.getText().trim());
            if (totalRam <= 0) throw new Exception("Total RAM must be > 0");

            int usedRam = 0;
            int usedCPU = 0;
            int runningCount = 0;
            int throttledCount = 0;

            StringBuilder sb = new StringBuilder();
            sb.append("=== MULTIPROGRAMMING DISPATCHER CYCLE ===\n");

            for (int i = 0; i < procModel.getRowCount(); i++) {
                String name = procModel.getValueAt(i, 0).toString();
                int mem = Integer.parseInt(procModel.getValueAt(i, 1).toString().trim());
                int cpu = Integer.parseInt(procModel.getValueAt(i, 2).toString().trim());

                if (usedRam + mem <= totalRam) {
                    usedRam += mem;
                    usedCPU += cpu;
                    runningCount++;
                    procModel.setValueAt("RUNNING (Active)", i, 3);
                    sb.append(String.format("✔ %s admitted to RAM (+%d MB, +%d%% CPU)\n", name, mem, cpu));
                } else {
                    throttledCount++;
                    procModel.setValueAt("SWAPPED / BLOCKED", i, 3);
                    sb.append(String.format("✘ %s blocked: Insufficient RAM (Requires %d MB)\n", name, mem));
                }
            }

            int effectiveCpu = Math.min(100, usedCPU);
            int ramPct = (int) (((double) usedRam / totalRam) * 100);

            barCPU.setValue(effectiveCpu);
            barCPU.setString(effectiveCpu + "%");
            barRAM.setValue(ramPct);
            barRAM.setString(String.format("%d / %d MB (%d%%)", usedRam, totalRam, ramPct));

            sb.append("\n=== SYSTEM RESOURCE SUMMARY ===\n");
            sb.append(String.format("Degree of Multiprogramming : %d Active / %d Total\n", runningCount, procModel.getRowCount()));
            sb.append(String.format("Total RAM Consumed          : %d MB (%d%%)\n", usedRam, ramPct));
            sb.append(String.format("Total CPU Demand            : %d%% (Load factor: %.2f)", usedCPU, usedCPU / 100.0));

            lblStatus.setText(String.format(" DEGREE: %d Active | Swapped: %d | RAM: %d%% | CPU: %d%% ",
                    runningCount, throttledCount, ramPct, effectiveCpu));
            lblStatus.setForeground(usedCPU > 100 ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_CYAN);
            txtLog.setText(sb.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Multiprogramming Error: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
