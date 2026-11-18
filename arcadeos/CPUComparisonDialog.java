package arcadeos;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.Queue;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;

/**
 * Educational Side-by-Side Algorithm Comparison Mode.
 * Runs FCFS, SJF, SRT, and Round Robin on the exact same workload and displays:
 * 1. Stacked Synchronized Gantt Timelines.
 * 2. Ranked Performance Scoreboard (Best WT / TAT highlighted).
 * 3. Educational Insights (Convoy effect, context switch overhead).
 * 4. CSV & PNG Export options.
 */
public class CPUComparisonDialog extends JDialog {

    public static class AlgoResult {
        public String name;
        public List<CPUSchedulingPanel.GanttSlot> gantt;
        public List<CPUSchedulingPanel.SimJob> jobs;
        public double avgWt;
        public double avgTat;
        public int contextSwitches;
        public double cpuUtilization;
        public double totalTime;
        public int rank;
    }

    private final List<AlgoResult> results = new ArrayList<>();
    private double maxTimelineTime = 10.0;

    public CPUComparisonDialog(JFrame parent, List<CPUSchedulingPanel.SimJob> inputJobs, double rrQuantum) {
        super(parent, "ALGORITHM COMPARISON ARENA // BENCHMARK WORKLOAD", true);
        setSize(1100, 750);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(10, 10));

        // 1. Compute Simulation For All 4 Algorithms
        computeAllAlgorithms(inputJobs, rrQuantum);

        // 2. Build UI
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainContent(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 6, 16));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);

        JLabel sub = new JLabel("EDUCATIONAL BENCHMARK ARENA // SAME WORKLOAD ACROSS 4 POLICIES");
        sub.setFont(new Font("Consolas", Font.BOLD, 10));
        sub.setForeground(RetroTheme.ACCENT_PINK);

        JLabel title = new JLabel("SIDE-BY-SIDE CPU SCHEDULING COMPARISON");
        title.setFont(new Font("Consolas", Font.BOLD, 16));
        title.setForeground(Color.WHITE);

        titleBox.add(sub);
        titleBox.add(title);
        header.add(titleBox, BorderLayout.WEST);

        // Best performer pill
        AlgoResult best = results.isEmpty() ? null : results.get(0);
        if (best != null) {
            JLabel winnerPill = new JLabel(" OPTIMAL: " + best.name + " (" + String.format("%.2fs", best.avgWt) + " WT) ");
            winnerPill.setFont(new Font("Consolas", Font.BOLD, 12));
            winnerPill.setForeground(RetroTheme.ACCENT_GREEN);
            winnerPill.setBorder(BorderFactory.createLineBorder(RetroTheme.ACCENT_GREEN, 1));
            header.add(winnerPill, BorderLayout.EAST);
        }

        return header;
    }

    private JPanel createMainContent() {
        JPanel main = new JPanel();
        main.setOpaque(false);
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(4, 16, 6, 16));

        // 1. Stacked Gantt Canvas (Height: 250px)
        StackedGanttCanvas stackedCanvas = new StackedGanttCanvas(results, maxTimelineTime);
        stackedCanvas.setPreferredSize(new Dimension(1050, 250));
        stackedCanvas.setMinimumSize(new Dimension(800, 250));
        stackedCanvas.setMaximumSize(new Dimension(1600, 250));
        main.add(stackedCanvas);

        main.add(Box.createVerticalStrut(10));

        // 2. Ranked Scoreboard (4 Leaderboard Cards)
        JPanel scoreboard = new JPanel(new GridLayout(1, 4, 10, 0));
        scoreboard.setOpaque(false);
        scoreboard.setMaximumSize(new Dimension(1600, 150));

        for (int i = 0; i < results.size(); i++) {
            AlgoResult res = results.get(i);
            boolean isWinner = (i == 0);
            scoreboard.add(createScoreCard(res, isWinner));
        }
        main.add(scoreboard);

        main.add(Box.createVerticalStrut(10));

        // 3. Educational Insights Bar
        JPanel insightCard = new JPanel(new BorderLayout());
        insightCard.setBackground(new Color(24, 18, 44));
        insightCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        insightCard.setMaximumSize(new Dimension(1600, 65));

        JLabel insTitle = new JLabel("EDUCATIONAL ANALYSIS & SCHEDULING INSIGHT:");
        insTitle.setFont(new Font("Consolas", Font.BOLD, 11));
        insTitle.setForeground(RetroTheme.ACCENT_CYAN);

        String insightText = generateInsightText();
        JLabel insBody = new JLabel("<html>" + insightText + "</html>");
        insBody.setFont(new Font("Consolas", Font.PLAIN, 11));
        insBody.setForeground(Color.WHITE);

        insightCard.add(insTitle, BorderLayout.NORTH);
        insightCard.add(insBody, BorderLayout.CENTER);
        main.add(insightCard);

        return main;
    }

    private JPanel createScoreCard(AlgoResult res, boolean isWinner) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(isWinner ? new Color(15, 38, 28) : new Color(24, 18, 44));
        Color borderCol = isWinner ? RetroTheme.ACCENT_GREEN : (res.rank == 2 ? RetroTheme.ACCENT_CYAN : new Color(75, 52, 110));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderCol, isWinner ? 2 : 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        // Top Header
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel rankLbl = new JLabel(res.rank == 1 ? "1ST [WINNER]" : res.rank + (res.rank == 2 ? "ND" : (res.rank == 3 ? "RD" : "TH")));
        rankLbl.setFont(new Font("Consolas", Font.BOLD, 11));
        rankLbl.setForeground(isWinner ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_YELLOW);

        JLabel nameLbl = new JLabel(res.name);
        nameLbl.setFont(new Font("Consolas", Font.BOLD, 13));
        nameLbl.setForeground(Color.WHITE);
        top.add(rankLbl, BorderLayout.WEST);
        top.add(nameLbl, BorderLayout.EAST);
        card.add(top);

        card.add(Box.createVerticalStrut(6));

        // Metrics
        JLabel wt = new JLabel("AVG WT : " + String.format("%.2f s", res.avgWt));
        wt.setFont(new Font("Consolas", Font.BOLD, 14));
        wt.setForeground(isWinner ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_CYAN);
        card.add(wt);

        JLabel tat = new JLabel("AVG TAT: " + String.format("%.2f s", res.avgTat));
        tat.setFont(new Font("Consolas", Font.BOLD, 12));
        tat.setForeground(RetroTheme.ACCENT_YELLOW);
        card.add(tat);

        card.add(Box.createVerticalStrut(4));

        JLabel ctx = new JLabel("SWITCHES: " + res.contextSwitches);
        ctx.setFont(new Font("Consolas", Font.PLAIN, 10));
        ctx.setForeground(RetroTheme.TEXT_WHITE);
        card.add(ctx);

        JLabel util = new JLabel("CPU UTIL: " + String.format("%.1f%%", res.cpuUtilization));
        util.setFont(new Font("Consolas", Font.PLAIN, 10));
        util.setForeground(RetroTheme.TEXT_DIM);
        card.add(util);

        return card;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));

        JButton btnCopyCsv = CPUSchedulingPanel.createArcadeButton("COPY BENCHMARK CSV", new Color(42, 28, 80), Color.WHITE, 30);
        btnCopyCsv.setFont(new Font("Consolas", Font.BOLD, 11));
        btnCopyCsv.addActionListener(e -> {
            copyBenchmarkCsv();
            JOptionPane.showMessageDialog(this, "Benchmark results copied to clipboard!", "Arcade OS Benchmark", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnExport = CPUSchedulingPanel.createArcadeButton("EXPORT PNG CHART", new Color(0, 120, 160), Color.WHITE, 30);
        btnExport.setFont(new Font("Consolas", Font.BOLD, 11));
        btnExport.addActionListener(e -> exportComparisonPng());

        JButton btnClose = CPUSchedulingPanel.createArcadeButton("CLOSE", new Color(160, 25, 70), Color.WHITE, 30);
        btnClose.setFont(new Font("Consolas", Font.BOLD, 11));
        btnClose.addActionListener(e -> dispose());

        footer.add(btnCopyCsv);
        footer.add(btnExport);
        footer.add(btnClose);
        return footer;
    }

    private String generateInsightText() {
        if (results.size() < 2) return "Run more algorithms to see comparative insights.";
        AlgoResult best = results.get(0);
        AlgoResult fcfs = null;
        for (AlgoResult r : results) if (r.name.equals("FCFS")) fcfs = r;

        if (fcfs != null && !best.name.equals("FCFS")) {
            double savedPct = ((fcfs.avgWt - best.avgWt) / Math.max(0.01, fcfs.avgWt)) * 100.0;
            return "<b>" + best.name + "</b> beat FCFS by reducing average wait time by <b>" + String.format("%.1f%%", savedPct)
                    + "</b> (" + String.format("%.2fs", fcfs.avgWt - best.avgWt) + "s saved per process). "
                    + "FCFS suffers from the <i>Convoy Effect</i> when short jobs queue behind long CPU bursts.";
        } else {
            return "<b>FCFS</b> performed optimally because processes arrived in order of their burst requirements with zero idle gaps.";
        }
    }

    private void copyBenchmarkCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("Rank,Algorithm,Avg_Wait_Sec,Avg_Turnaround_Sec,Context_Switches,CPU_Utilization_Pct,Total_Time_Sec\n");
        for (AlgoResult r : results) {
            sb.append(String.format("%d,%s,%.3f,%.3f,%d,%.1f,%.3f\n",
                    r.rank, r.name, r.avgWt, r.avgTat, r.contextSwitches, r.cpuUtilization, r.totalTime));
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
    }

    private void exportComparisonPng() {
        try {
            BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            paint(g2);
            g2.dispose();

            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("cpu_algorithm_benchmark.png"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                ImageIO.write(img, "png", chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "Exported successfully to " + chooser.getSelectedFile().getName());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void computeAllAlgorithms(List<CPUSchedulingPanel.SimJob> baseJobs, double quantum) {
        results.clear();
        double maxTime = 0.0;

        String[] algos = {"FCFS", "SJF", "SRT", "ROUND ROBIN"};
        for (String algo : algos) {
            AlgoResult r = simulateWorkload(algo, cloneJobs(baseJobs), quantum);
            results.add(r);
            if (r.totalTime > maxTime) maxTime = r.totalTime;
        }

        this.maxTimelineTime = Math.max(10.0, maxTime);

        // Sort by lowest Average Waiting Time
        results.sort(Comparator.comparingDouble((AlgoResult r) -> r.avgWt).thenComparingDouble(r -> r.avgTat));
        for (int i = 0; i < results.size(); i++) {
            results.get(i).rank = i + 1;
        }
    }

    private List<CPUSchedulingPanel.SimJob> cloneJobs(List<CPUSchedulingPanel.SimJob> list) {
        List<CPUSchedulingPanel.SimJob> copy = new ArrayList<>();
        for (CPUSchedulingPanel.SimJob j : list) {
            copy.add(new CPUSchedulingPanel.SimJob(j.pid, j.at, j.bt, j.prio));
        }
        return copy;
    }

    private AlgoResult simulateWorkload(String algo, List<CPUSchedulingPanel.SimJob> jobs, double quantum) {
        AlgoResult res = new AlgoResult();
        res.name = algo.equals("ROUND ROBIN") ? "ROUND ROBIN (Q=" + String.format("%.1f", quantum) + ")" : algo;
        List<CPUSchedulingPanel.GanttSlot> gantt = new ArrayList<>();
        List<CPUSchedulingPanel.SimJob> finished = new ArrayList<>();
        int switches = 0;

        if (algo.equals("FCFS")) {
            jobs.sort(Comparator.comparingDouble((CPUSchedulingPanel.SimJob j) -> j.at).thenComparing(j -> j.pid));
            double time = 0.0;
            for (CPUSchedulingPanel.SimJob j : jobs) {
                if (time < j.at) {
                    gantt.add(new CPUSchedulingPanel.GanttSlot("[IDLE]", time, j.at));
                    time = j.at;
                }
                double start = time;
                time += j.bt;
                j.ct = time;
                j.tat = j.ct - j.at;
                j.wt = j.tat - j.bt;
                gantt.add(new CPUSchedulingPanel.GanttSlot(j.pid, start, time));
                finished.add(j);
                switches++;
            }
        } else if (algo.equals("SJF")) {
            double time = 0.0;
            List<CPUSchedulingPanel.SimJob> rem = new ArrayList<>(jobs);
            while (!rem.isEmpty()) {
                List<CPUSchedulingPanel.SimJob> avail = new ArrayList<>();
                for (CPUSchedulingPanel.SimJob j : rem) if (j.at <= time) avail.add(j);
                if (avail.isEmpty()) {
                    double nxt = rem.stream().mapToDouble(j -> j.at).min().orElse(time + 1.0);
                    gantt.add(new CPUSchedulingPanel.GanttSlot("[IDLE]", time, nxt));
                    time = nxt;
                    continue;
                }
                avail.sort(Comparator.comparingDouble((CPUSchedulingPanel.SimJob j) -> j.bt).thenComparingDouble(j -> j.at));
                CPUSchedulingPanel.SimJob pick = avail.get(0);
                rem.remove(pick);
                double start = time;
                time += pick.bt;
                pick.ct = time;
                pick.tat = pick.ct - pick.at;
                pick.wt = pick.tat - pick.bt;
                gantt.add(new CPUSchedulingPanel.GanttSlot(pick.pid, start, time));
                finished.add(pick);
                switches++;
            }
        } else if (algo.equals("SRT")) {
            double time = 0.0;
            List<CPUSchedulingPanel.SimJob> rem = new ArrayList<>(jobs);
            for (CPUSchedulingPanel.SimJob j : rem) j.remBt = j.bt;
            CPUSchedulingPanel.SimJob last = null;
            double segStart = 0.0;

            while (!rem.isEmpty()) {
                List<CPUSchedulingPanel.SimJob> avail = new ArrayList<>();
                for (CPUSchedulingPanel.SimJob j : rem) if (j.at <= time) avail.add(j);
                if (avail.isEmpty()) {
                    if (last != null) { gantt.add(new CPUSchedulingPanel.GanttSlot(last.pid, segStart, time)); last = null; }
                    double nxt = rem.stream().mapToDouble(j -> j.at).min().orElse(time + 0.5);
                    gantt.add(new CPUSchedulingPanel.GanttSlot("[IDLE]", time, nxt));
                    time = nxt; segStart = nxt;
                    continue;
                }
                avail.sort(Comparator.comparingDouble((CPUSchedulingPanel.SimJob j) -> j.remBt).thenComparingDouble(j -> j.at));
                CPUSchedulingPanel.SimJob curr = avail.get(0);

                if (last != curr) {
                    if (last != null) {
                        gantt.add(new CPUSchedulingPanel.GanttSlot(last.pid, segStart, time));
                        switches++;
                    }
                    segStart = time; last = curr;
                }

                double step = 0.05;
                curr.remBt -= step;
                time += step;

                if (curr.remBt <= 0.001) {
                    curr.ct = time;
                    curr.tat = curr.ct - curr.at;
                    curr.wt = Math.max(0, curr.tat - curr.bt);
                    finished.add(curr);
                    rem.remove(curr);
                    gantt.add(new CPUSchedulingPanel.GanttSlot(curr.pid, segStart, time));
                    last = null; segStart = time;
                    switches++;
                }
            }
        } else {
            // Round Robin
            jobs.sort(Comparator.comparingDouble(j -> j.at));
            for (CPUSchedulingPanel.SimJob j : jobs) j.remBt = j.bt;
            Queue<CPUSchedulingPanel.SimJob> q = new LinkedList<>();
            double time = 0.0;
            int arrivedIdx = 0;
            while (arrivedIdx < jobs.size() && jobs.get(arrivedIdx).at <= time) q.add(jobs.get(arrivedIdx++));

            while (!q.isEmpty() || arrivedIdx < jobs.size()) {
                if (q.isEmpty()) {
                    time = jobs.get(arrivedIdx).at;
                    while (arrivedIdx < jobs.size() && jobs.get(arrivedIdx).at <= time) q.add(jobs.get(arrivedIdx++));
                }
                CPUSchedulingPanel.SimJob curr = q.poll();
                double slice = Math.min(quantum, curr.remBt);
                gantt.add(new CPUSchedulingPanel.GanttSlot(curr.pid, time, time + slice));
                time += slice;
                curr.remBt -= slice;
                switches++;

                while (arrivedIdx < jobs.size() && jobs.get(arrivedIdx).at <= time) q.add(jobs.get(arrivedIdx++));
                if (curr.remBt > 0.001) {
                    q.add(curr);
                } else {
                    curr.ct = time;
                    curr.tat = curr.ct - curr.at;
                    curr.wt = Math.max(0, curr.tat - curr.bt);
                    finished.add(curr);
                }
            }
        }

        res.gantt = gantt;
        res.jobs = finished;
        res.totalTime = gantt.isEmpty() ? 0.0 : gantt.get(gantt.size() - 1).end;
        res.contextSwitches = Math.max(0, switches - 1);

        double totalBt = 0.0, totalWt = 0.0, totalTat = 0.0;
        for (CPUSchedulingPanel.SimJob j : finished) {
            totalBt += j.bt;
            totalWt += j.wt;
            totalTat += j.tat;
        }
        int count = Math.max(1, finished.size());
        res.avgWt = totalWt / count;
        res.avgTat = totalTat / count;
        res.cpuUtilization = res.totalTime > 0.0 ? Math.min(100.0, (totalBt / res.totalTime) * 100.0) : 0.0;
        return res;
    }

    /**
     * Stacked Gantt Canvas painting all 4 timelines aligned on one master scale.
     */
    private static class StackedGanttCanvas extends JPanel {
        private final List<AlgoResult> results;
        private final double maxTime;

        public StackedGanttCanvas(List<AlgoResult> results, double maxTime) {
            this.results = results;
            this.maxTime = maxTime;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background pod
            g2.setColor(new Color(18, 14, 32));
            g2.fillRoundRect(0, 0, w, h, 10, 10);
            g2.setColor(new Color(60, 42, 90));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

            if (results.isEmpty() || maxTime <= 0) {
                g2.dispose();
                return;
            }

            int marginL = 120;
            int marginR = 30;
            int marginT = 20;
            int usableW = w - marginL - marginR;
            int rowH = 38;
            int gap = 16;

            // Draw time gridlines
            g2.setColor(new Color(50, 40, 75, 120));
            g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 3.0f}, 0.0f));
            for (double t = 0; t <= maxTime; t += Math.max(1.0, Math.round(maxTime / 10.0))) {
                int gx = marginL + (int) ((t / maxTime) * usableW);
                g2.drawLine(gx, marginT, gx, marginT + (rowH + gap) * results.size() - gap);
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(new Color(140, 130, 170));
                g2.drawString(String.format("%.0fs", t), gx - 8, marginT + (rowH + gap) * results.size() + 2);
                g2.setColor(new Color(50, 40, 75, 120));
            }

            // Draw each algorithm's Gantt timeline
            for (int r = 0; r < results.size(); r++) {
                AlgoResult res = results.get(r);
                int ry = marginT + r * (rowH + gap);

                // Row label
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(res.rank == 1 ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_CYAN);
                g2.drawString(res.name, 12, ry + rowH / 2 + 4);

                // Row track background
                g2.setColor(new Color(10, 8, 20));
                g2.fillRoundRect(marginL, ry, usableW, rowH, 6, 6);

                // Draw slots
                for (CPUSchedulingPanel.GanttSlot slot : res.gantt) {
                    int sx = marginL + (int) ((slot.start / maxTime) * usableW);
                    int ex = marginL + (int) ((slot.end / maxTime) * usableW);
                    int slotW = Math.max(2, ex - sx);

                    if (slot.label.equals("[IDLE]")) {
                        g2.setColor(new Color(30, 24, 45));
                        g2.fillRoundRect(sx, ry, slotW, rowH, 4, 4);
                        g2.setColor(new Color(80, 70, 100));
                        g2.setFont(new Font("Consolas", Font.BOLD, 9));
                        if (slotW > 25) g2.drawString("IDLE", sx + slotW / 2 - 10, ry + rowH / 2 + 3);
                    } else {
                        Color col = getJobColor(slot.label);
                        g2.setColor(col);
                        g2.fillRoundRect(sx, ry, slotW, rowH, 4, 4);
                        g2.setColor(Color.BLACK);
                        g2.setStroke(new BasicStroke(1.0f));
                        g2.drawRoundRect(sx, ry, slotW, rowH, 4, 4);

                        // Label
                        if (slotW > 16) {
                            g2.setColor(Color.BLACK);
                            g2.setFont(new Font("Consolas", Font.BOLD, 10));
                            g2.drawString(slot.label, sx + slotW / 2 - 6, ry + rowH / 2 + 4);
                        }
                    }
                }
            }

            g2.dispose();
        }

        private Color getJobColor(String pid) {
            switch (pid) {
                case "J1": return RetroTheme.ACCENT_CYAN;
                case "J2": return new Color(180, 90, 255);
                case "J3": return RetroTheme.ACCENT_YELLOW;
                case "J4": return RetroTheme.ACCENT_PINK;
                case "J5": return RetroTheme.ACCENT_GREEN;
                case "J6": return new Color(255, 140, 40);
                case "J7": return new Color(100, 240, 200);
                default: return new Color(220, 100, 200);
            }
        }
    }
}
