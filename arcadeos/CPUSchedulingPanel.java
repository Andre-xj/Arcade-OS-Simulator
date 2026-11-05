package arcadeos;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;

public class CPUSchedulingPanel extends JPanel {
    private DefaultTableModel inputModel, outputModel;
    private JComboBox<String> algoCombo;
    private JTextField txtQuantum;
    private JLabel lblStats;
    private GanttCanvas ganttCanvas;

    public CPUSchedulingPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        ctrl.setBackground(RetroTheme.PANEL_BG);
        ctrl.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        JLabel lblAlgo = new JLabel("Algorithm:");
        lblAlgo.setFont(RetroTheme.RETRO_FONT_BOLD);
        lblAlgo.setForeground(RetroTheme.ACCENT_CYAN);
        ctrl.add(lblAlgo);

        algoCombo = new JComboBox<>(new String[]{
                "FCFS", "SJF (Non-Preemptive)", "SJF (Preemptive / SRTF)", "Priority (Non-Preemptive)", "Round Robin"
        });
        algoCombo.setFont(RetroTheme.RETRO_FONT);
        ctrl.add(algoCombo);

        JLabel lblQ = new JLabel("Quantum:");
        lblQ.setFont(RetroTheme.RETRO_FONT);
        lblQ.setForeground(RetroTheme.TEXT_WHITE);
        ctrl.add(lblQ);

        txtQuantum = RetroTheme.createRetroTextField("2", 3);
        ctrl.add(txtQuantum);

        JButton btnAdd = RetroTheme.createRetroButton("+ Add Process", new Color(40, 26, 74), RetroTheme.ACCENT_CYAN);
        JButton btnRemove = RetroTheme.createRetroButton("- Remove", new Color(40, 26, 74), RetroTheme.ACCENT_PINK);
        JButton btnSmall = RetroTheme.createRetroButton("Preset: Small (3)", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnLarge = RetroTheme.createRetroButton("Preset: Large (8)", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnRun = RetroTheme.createRetroButton("▶ RUN SIMULATION", RetroTheme.ACCENT_YELLOW, Color.BLACK);

        ctrl.add(btnAdd);
        ctrl.add(btnRemove);
        ctrl.add(btnSmall);
        ctrl.add(btnLarge);
        ctrl.add(btnRun);
        add(ctrl, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        centerPanel.setOpaque(false);

        inputModel = new DefaultTableModel(new String[]{"Process ID", "Arrival Time", "Burst Time", "Priority"}, 0);
        JTable inputTable = RetroTheme.createRetroTable(inputModel);
        JScrollPane scrollInput = new JScrollPane(inputTable);
        scrollInput.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR), " [ DYNAMIC PROCESS QUEUE INPUT ] ",
                TitledBorder.LEFT, TitledBorder.TOP, RetroTheme.RETRO_FONT_BOLD, RetroTheme.ACCENT_CYAN
        ));
        centerPanel.add(scrollInput);

        JPanel lowerPanel = new JPanel(new BorderLayout(4, 4));
        lowerPanel.setBackground(RetroTheme.PANEL_BG);
        lowerPanel.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        lblStats = new JLabel(" Total: -- Procs | Avg WT: -- ticks | Avg TAT: -- ticks ");
        lblStats.setFont(RetroTheme.RETRO_FONT_BOLD);
        lblStats.setForeground(RetroTheme.ACCENT_YELLOW);
        lowerPanel.add(lblStats, BorderLayout.NORTH);

        ganttCanvas = new GanttCanvas();
        lowerPanel.add(ganttCanvas, BorderLayout.CENTER);

        outputModel = new DefaultTableModel(new String[]{"PID", "Arrival", "Burst", "Completion", "Turnaround", "Waiting"}, 0);
        JTable outputTable = RetroTheme.createRetroTable(outputModel);
        JScrollPane scrollOutput = new JScrollPane(outputTable);
        scrollOutput.setPreferredSize(new Dimension(800, 140));
        lowerPanel.add(scrollOutput, BorderLayout.SOUTH);

        centerPanel.add(lowerPanel);
        add(centerPanel, BorderLayout.CENTER);

        btnAdd.addActionListener(e -> inputModel.addRow(new Object[]{"P" + (inputModel.getRowCount() + 1), "0", "4", "1"}));
        btnRemove.addActionListener(e -> {
            int r = inputTable.getSelectedRow();
            if (r >= 0) inputModel.removeRow(r);
        });
        btnSmall.addActionListener(e -> loadSmallPreset());
        btnLarge.addActionListener(e -> loadLargePreset());
        btnRun.addActionListener(e -> runCPUSimulation());

        loadSmallPreset();
    }

    private void loadSmallPreset() {
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"P1_Pacman", "0", "4", "2"});
        inputModel.addRow(new Object[]{"P2_Galaga", "1", "3", "1"});
        inputModel.addRow(new Object[]{"P3_DKong",  "2", "2", "3"});
    }

    private void loadLargePreset() {
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"P1_Pacman", "0", "5", "3"});
        inputModel.addRow(new Object[]{"P2_Galaga", "1", "3", "1"});
        inputModel.addRow(new Object[]{"P3_DKong",  "2", "8", "4"});
        inputModel.addRow(new Object[]{"P4_Frogger","3", "2", "2"});
        inputModel.addRow(new Object[]{"P5_Centip", "4", "4", "5"});
        inputModel.addRow(new Object[]{"P6_DigDug", "5", "3", "2"});
        inputModel.addRow(new Object[]{"P7_Qbert",  "6", "6", "3"});
        inputModel.addRow(new Object[]{"P8_Defend", "7", "1", "1"});
    }

    static class ProcessItem {
        String pid; int at, bt, prio, ct, tat, wt, remBt;
        ProcessItem(String pid, int at, int bt, int prio) {
            this.pid = pid; this.at = at; this.bt = bt; this.prio = prio; this.remBt = bt;
        }
    }

    static class GanttBlock {
        String pid; int start, end;
        GanttBlock(String pid, int s, int e) { this.pid = pid; this.start = s; this.end = e; }
    }

    private void runCPUSimulation() {
        if (inputModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Mag-lagay muna ng processes sa table!", "Empty Queue", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<ProcessItem> list = new ArrayList<>();
        try {
            for (int i = 0; i < inputModel.getRowCount(); i++) {
                String pid = inputModel.getValueAt(i, 0).toString().trim();
                int at = Integer.parseInt(inputModel.getValueAt(i, 1).toString().trim());
                int bt = Integer.parseInt(inputModel.getValueAt(i, 2).toString().trim());
                int prio = Integer.parseInt(inputModel.getValueAt(i, 3).toString().trim());
                if (at < 0 || bt <= 0) throw new NumberFormatException("Negative or Zero BT");
                list.add(new ProcessItem(pid, at, bt, prio));
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error sa input: Integers only, BT > 0, AT >= 0.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String algo = algoCombo.getSelectedItem().toString();
        List<GanttBlock> gantt = new ArrayList<>();
        List<ProcessItem> results = new ArrayList<>();

        if (algo.equals("FCFS")) {
            list.sort(Comparator.comparingInt((ProcessItem p) -> p.at).thenComparing(p -> p.pid));
            int time = 0;
            for (ProcessItem p : list) {
                if (time < p.at) {
                    gantt.add(new GanttBlock("[IDLE]", time, p.at));
                    time = p.at;
                }
                int start = time;
                time += p.bt;
                p.ct = time;
                p.tat = p.ct - p.at;
                p.wt = p.tat - p.bt;
                gantt.add(new GanttBlock(p.pid, start, time));
                results.add(p);
            }
        } else if (algo.equals("SJF (Non-Preemptive)")) {
            int time = 0;
            List<ProcessItem> rem = new ArrayList<>(list);
            while (!rem.isEmpty()) {
                List<ProcessItem> avail = new ArrayList<>();
                for (ProcessItem p : rem) if (p.at <= time) avail.add(p);
                if (avail.isEmpty()) {
                    int nxt = rem.stream().mapToInt(p -> p.at).min().orElse(time + 1);
                    gantt.add(new GanttBlock("[IDLE]", time, nxt));
                    time = nxt;
                    continue;
                }
                avail.sort(Comparator.comparingInt((ProcessItem p) -> p.bt).thenComparingInt(p -> p.at));
                ProcessItem pick = avail.get(0);
                rem.remove(pick);
                int start = time;
                time += pick.bt;
                pick.ct = time;
                pick.tat = pick.ct - pick.at;
                pick.wt = pick.tat - pick.bt;
                gantt.add(new GanttBlock(pick.pid, start, time));
                results.add(pick);
            }
        } else if (algo.equals("Round Robin")) {
            int quantum;
            try {
                quantum = Integer.parseInt(txtQuantum.getText().trim());
                if (quantum <= 0) throw new NumberFormatException();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Quantum must be integer > 0", "Invalid Quantum", JOptionPane.ERROR_MESSAGE);
                return;
            }
            list.sort(Comparator.comparingInt(p -> p.at));
            Queue<ProcessItem> q = new LinkedList<>();
            int time = 0;
            int arrivedIdx = 0;
            while (arrivedIdx < list.size() && list.get(arrivedIdx).at <= time) {
                q.add(list.get(arrivedIdx++));
            }

            while (!q.isEmpty() || arrivedIdx < list.size()) {
                if (q.isEmpty()) {
                    time = list.get(arrivedIdx).at;
                    while (arrivedIdx < list.size() && list.get(arrivedIdx).at <= time) {
                        q.add(list.get(arrivedIdx++));
                    }
                }
                ProcessItem curr = q.poll();
                int slice = Math.min(quantum, curr.remBt);
                gantt.add(new GanttBlock(curr.pid, time, time + slice));
                time += slice;
                curr.remBt -= slice;

                while (arrivedIdx < list.size() && list.get(arrivedIdx).at <= time) {
                    q.add(list.get(arrivedIdx++));
                }
                if (curr.remBt > 0) {
                    q.add(curr);
                } else {
                    curr.ct = time;
                    curr.tat = curr.ct - curr.at;
                    curr.wt = curr.tat - curr.bt;
                    results.add(curr);
                }
            }
        } else {
            int time = 0;
            List<ProcessItem> rem = new ArrayList<>(list);
            while (!rem.isEmpty()) {
                List<ProcessItem> avail = new ArrayList<>();
                for (ProcessItem p : rem) if (p.at <= time) avail.add(p);
                if (avail.isEmpty()) {
                    int nxt = rem.stream().mapToInt(p -> p.at).min().orElse(time + 1);
                    gantt.add(new GanttBlock("[IDLE]", time, nxt));
                    time = nxt;
                    continue;
                }
                avail.sort(Comparator.comparingInt((ProcessItem p) -> p.prio).thenComparingInt(p -> p.at));
                ProcessItem pick = avail.get(0);
                rem.remove(pick);
                int start = time;
                time += pick.bt;
                pick.ct = time;
                pick.tat = pick.ct - pick.at;
                pick.wt = pick.tat - pick.bt;
                gantt.add(new GanttBlock(pick.pid, start, time));
                results.add(pick);
            }
        }

        outputModel.setRowCount(0);
        double totWt = 0, totTat = 0;
        for (ProcessItem p : results) {
            outputModel.addRow(new Object[]{p.pid, p.at, p.bt, p.ct, p.tat, p.wt});
            totWt += p.wt;
            totTat += p.tat;
        }
        double avgWt = results.isEmpty() ? 0 : totWt / results.size();
        double avgTat = results.isEmpty() ? 0 : totTat / results.size();
        lblStats.setText(String.format(" Total: %d Procs | Avg WT: %.2f ticks | Avg TAT: %.2f ticks ",
                results.size(), avgWt, avgTat));

        ganttCanvas.setBlocks(gantt);
    }

    static class GanttCanvas extends JPanel {
        private List<GanttBlock> blocks = new ArrayList<>();
        private final Color[] PALETTE = {
                new Color(255, 42, 141), new Color(5, 217, 232), new Color(255, 211, 25),
                new Color(0, 255, 102), new Color(217, 0, 255), new Color(255, 119, 0)
        };

        public GanttCanvas() {
            setBackground(new Color(10, 6, 18));
            setPreferredSize(new Dimension(800, 75));
            setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));
        }

        public void setBlocks(List<GanttBlock> b) {
            this.blocks = b;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (blocks.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int maxTime = blocks.get(blocks.size() - 1).end;
            int w = getWidth() - 40;
            int startX = 20;
            double scale = maxTime > 0 ? (double) w / maxTime : 1.0;

            Map<String, Color> colorMap = new HashMap<>();
            int cIdx = 0;

            for (GanttBlock b : blocks) {
                int x1 = startX + (int) (b.start * scale);
                int x2 = startX + (int) (b.end * scale);
                int bw = Math.max(x2 - x1, 1);

                Color fill;
                if (b.pid.equals("[IDLE]")) {
                    fill = new Color(50, 50, 70);
                } else {
                    if (!colorMap.containsKey(b.pid)) {
                        colorMap.put(b.pid, PALETTE[cIdx % PALETTE.length]);
                        cIdx++;
                    }
                    fill = colorMap.get(b.pid);
                }

                g2.setColor(fill);
                g2.fillRect(x1, 12, bw, 32);
                g2.setColor(Color.WHITE);
                g2.drawRect(x1, 12, bw, 32);

                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                String str = b.pid;
                if (fm.stringWidth(str) < bw) {
                    g2.setColor(b.pid.equals("[IDLE]") ? Color.LIGHT_GRAY : Color.BLACK);
                    g2.drawString(str, x1 + (bw - fm.stringWidth(str)) / 2, 32);
                }

                g2.setColor(RetroTheme.TEXT_WHITE);
                g2.setFont(new Font("Consolas", Font.PLAIN, 9));
                g2.drawString(String.valueOf(b.start), x1, 56);
            }
            g2.drawString(String.valueOf(maxTime), startX + (int) (maxTime * scale), 56);
        }
    }
}
