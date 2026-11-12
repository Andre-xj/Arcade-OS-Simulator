package arcadeos;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.Queue;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Screen 4: MISSION 01 - CPU SCHEDULING
 * 100% Matching Screenshot 4 (Input Console + Live Output Flight Path)
 */
public class CPUSchedulingPanel extends JPanel {
    private DefaultTableModel inputModel, outputModel;
    private JTable inputTable, outputTable;
    private String selectedAlgo = "FCFS";
    private JButton btnFcfs, btnSjf, btnSrt, btnRr;
    private JLabel lblAvgWt, lblAvgTat;
    private JLabel lblMissionStatus;
    private GanttCanvas ganttCanvas;
    private PipelineCanvas pipelineCanvas;
    private JProgressBar queueProgress;

    public CPUSchedulingPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new GridLayout(1, 2, 12, 0));
        setBorder(BorderFactory.createEmptyBorder(6, 12, 10, 12));

        // Left Column: INPUT CONSOLE / PROCESS DECK
        add(createLeftInputConsole());

        // Right Column: LIVE OUTPUT / FLIGHT PATH
        add(createRightLiveOutput());

        // Load Default Dataset (from Screenshot 4)
        loadDefaultMissionData();
        runSimulation();
    }

    private JPanel createLeftInputConsole() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // 1. Header: Joystick INPUT CONSOLE / PROCESS DECK
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel icon = new JLabel("🕹️");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        JLabel title = new JLabel("INPUT CONSOLE // PROCESS DECK");
        title.setFont(new Font("Consolas", Font.BOLD, 12));
        title.setForeground(Color.WHITE);
        header.add(icon); header.add(title);
        p.add(header);

        p.add(Box.createVerticalStrut(4));

        // 2. Mission Card (Clear the ready queue)
        JPanel missionCard = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 20, 52));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(70, 40, 100));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        missionCard.setOpaque(false);
        missionCard.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        missionCard.setMaximumSize(new Dimension(800, 56));
        missionCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel missionText = new JPanel();
        missionText.setOpaque(false);
        missionText.setLayout(new BoxLayout(missionText, BoxLayout.Y_AXIS));

        JLabel mTitle = new JLabel("MISSION 01: CLEAR THE READY QUEUE");
        mTitle.setFont(new Font("Consolas", Font.BOLD, 11));
        mTitle.setForeground(RetroTheme.ACCENT_YELLOW);
        missionText.add(mTitle);

        JLabel mSub = new JLabel("Schedule every alien job without a system crash.");
        mSub.setFont(new Font("Consolas", Font.PLAIN, 9));
        mSub.setForeground(RetroTheme.TEXT_DIM);
        missionText.add(mSub);

        missionCard.add(missionText, BorderLayout.WEST);

        JLabel xpBadge = new JLabel(" +125 XP ");
        xpBadge.setFont(new Font("Consolas", Font.BOLD, 12));
        xpBadge.setForeground(RetroTheme.ACCENT_GREEN);
        missionCard.add(xpBadge, BorderLayout.EAST);

        p.add(missionCard);

        p.add(Box.createVerticalStrut(8));

        // 3. Algorithm Switch Buttons
        JPanel algoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        algoRow.setOpaque(false);
        algoRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblAlgo = new JLabel("ALGORITHM SWITCH:");
        lblAlgo.setFont(new Font("Consolas", Font.BOLD, 10));
        lblAlgo.setForeground(RetroTheme.TEXT_DIM);
        algoRow.add(lblAlgo);

        btnFcfs = createAlgoTabBtn("FCFS");
        btnSjf = createAlgoTabBtn("SJF");
        btnSrt = createAlgoTabBtn("SRT");
        btnRr = createAlgoTabBtn("ROUND ROBIN");

        algoRow.add(btnFcfs); algoRow.add(btnSjf); algoRow.add(btnSrt); algoRow.add(btnRr);
        p.add(algoRow);

        p.add(Box.createVerticalStrut(8));

        // 4. Editable Process Queue Header + Add Job Button
        JPanel queueHeader = new JPanel(new BorderLayout());
        queueHeader.setOpaque(false);
        queueHeader.setMaximumSize(new Dimension(800, 24));
        queueHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel qTitle = new JLabel("EDITABLE PROCESS QUEUE");
        qTitle.setFont(new Font("Consolas", Font.BOLD, 10));
        qTitle.setForeground(RetroTheme.ACCENT_CYAN);
        queueHeader.add(qTitle, BorderLayout.WEST);

        JButton btnAddJob = RetroTheme.createRetroButton("+ ADD JOB", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnAddJob.setFont(new Font("Consolas", Font.BOLD, 9));
        btnAddJob.addActionListener(e -> {
            int n = inputModel.getRowCount() + 1;
            inputModel.addRow(new Object[]{"J" + n, "0.0", "2.0", "1"});
            runSimulation();
        });
        queueHeader.add(btnAddJob, BorderLayout.EAST);
        p.add(queueHeader);

        p.add(Box.createVerticalStrut(4));

        // 5. Input Process Table with Avatar Icons
        inputModel = new DefaultTableModel(new String[]{"JOB", "AT", "BT", "PRIO"}, 0);
        inputTable = RetroTheme.createRetroTable(inputModel);
        inputTable.setRowHeight(28);

        // Custom Cell Renderer to paint alien avatars in column 0
        inputTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, val, isSel, hasFocus, row, col);
                l.setIcon(new ImageIcon(PixelSprites.getJobAvatar(row, 18)));
                l.setFont(new Font("Consolas", Font.BOLD, 11));
                l.setForeground(Color.WHITE);
                l.setHorizontalTextPosition(SwingConstants.RIGHT);
                return l;
            }
        });

        JScrollPane scrollInput = RetroTheme.createRetroScroll(inputTable);
        scrollInput.setPreferredSize(new Dimension(480, 180));
        scrollInput.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(scrollInput);

        p.add(Box.createVerticalStrut(8));

        // 6. Tip Box
        JPanel tipBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        tipBox.setOpaque(false);
        tipBox.setMaximumSize(new Dimension(800, 30));
        tipBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        tipBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 30, 65), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        JLabel tipTag = new JLabel("TIP:");
        tipTag.setFont(new Font("Consolas", Font.BOLD, 9));
        tipTag.setForeground(RetroTheme.ACCENT_CYAN);
        JLabel tipText = new JLabel("Arrival time (AT) decides when each alien enters the ready queue.");
        tipText.setFont(new Font("Consolas", Font.PLAIN, 9));
        tipText.setForeground(RetroTheme.TEXT_DIM);
        tipBox.add(tipTag); tipBox.add(tipText);
        p.add(tipBox);

        p.add(Box.createVerticalStrut(8));

        // 7. Action Controls: ▶ PLAY, ⏩ NEXT, 🔄 RESET + Progress Bar
        JPanel controlsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        controlsRow.setOpaque(false);
        controlsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnPlay = RetroTheme.createRetroButton("▶ PLAY", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        JButton btnNext = RetroTheme.createRetroButton("⏩ NEXT", RetroTheme.BTN_DARK, RetroTheme.TEXT_WHITE);
        JButton btnReset = RetroTheme.createRetroButton("🔄 RESET", RetroTheme.BTN_DARK, RetroTheme.ACCENT_PINK);

        btnPlay.addActionListener(e -> runSimulation());
        btnNext.addActionListener(e -> runSimulation());
        btnReset.addActionListener(e -> loadDefaultMissionData());

        controlsRow.add(btnPlay);
        controlsRow.add(btnNext);
        controlsRow.add(btnReset);

        JProgressBar animBar = new JProgressBar(0, 100);
        animBar.setValue(85);
        animBar.setPreferredSize(new Dimension(100, 10));
        animBar.setForeground(RetroTheme.ACCENT_PINK);
        animBar.setBackground(RetroTheme.INPUT_BG);
        controlsRow.add(animBar);

        p.add(controlsRow);

        p.add(Box.createVerticalStrut(8));

        // 8. Big Pink Glowing Button: ★ START SCHEDULING MISSION
        JButton btnStartMission = new JButton("★  START SCHEDULING MISSION") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnStartMission.setFont(new Font("Consolas", Font.BOLD, 12));
        btnStartMission.setForeground(Color.WHITE);
        btnStartMission.setFocusPainted(false);
        btnStartMission.setOpaque(false);
        btnStartMission.setContentAreaFilled(false);
        btnStartMission.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        btnStartMission.setMaximumSize(new Dimension(800, 38));
        btnStartMission.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnStartMission.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnStartMission.addActionListener(e -> {
            runSimulation();
            AppState.addScore(125);
            JOptionPane.showMessageDialog(this,
                    "★ MISSION CLEARED! ★\nAll alien processes scheduled successfully!\n+125 XP added to Score.",
                    "Mission Victory", JOptionPane.INFORMATION_MESSAGE);
        });
        p.add(btnStartMission);

        return p;
    }

    private JPanel createRightLiveOutput() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // 1. Header: LIVE OUTPUT / FCFS FLIGHT PATH + pill
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(800, 26));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel leftHead = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftHead.setOpaque(false);
        JLabel star = new JLabel("✦");
        star.setFont(new Font("Consolas", Font.BOLD, 12));
        star.setForeground(RetroTheme.ACCENT_CYAN);
        lblMissionStatus = new JLabel("LIVE OUTPUT // FCFS FLIGHT PATH");
        lblMissionStatus.setFont(new Font("Consolas", Font.BOLD, 11));
        lblMissionStatus.setForeground(Color.WHITE);
        leftHead.add(star); leftHead.add(lblMissionStatus);
        header.add(leftHead, BorderLayout.WEST);

        JLabel pill = new JLabel(" [ SIMULATION READY ] ");
        pill.setFont(new Font("Consolas", Font.BOLD, 9));
        pill.setForeground(RetroTheme.ACCENT_GREEN);
        pill.setBorder(BorderFactory.createLineBorder(RetroTheme.ACCENT_GREEN, 1));
        header.add(pill, BorderLayout.EAST);
        p.add(header);

        p.add(Box.createVerticalStrut(4));

        // 2. Stats Summary Card
        JPanel statsCard = new JPanel(new BorderLayout(8, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(24, 18, 44));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(60, 40, 90));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        statsCard.setOpaque(false);
        statsCard.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        statsCard.setMaximumSize(new Dimension(800, 85));
        statsCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Top info row
        JPanel topInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        topInfo.setOpaque(false);

        JLabel lvl = new JLabel("LEVEL: CPU-01");
        lvl.setFont(new Font("Consolas", Font.BOLD, 9));
        lvl.setForeground(RetroTheme.TEXT_DIM);
        topInfo.add(lvl);

        JLabel qLbl = new JLabel("QUEUE CLEARANCE: ");
        qLbl.setFont(new Font("Consolas", Font.BOLD, 9));
        qLbl.setForeground(RetroTheme.TEXT_DIM);
        topInfo.add(qLbl);

        queueProgress = new JProgressBar(0, 100);
        queueProgress.setValue(100);
        queueProgress.setStringPainted(true);
        queueProgress.setPreferredSize(new Dimension(140, 12));
        queueProgress.setForeground(RetroTheme.ACCENT_CYAN);
        queueProgress.setBackground(RetroTheme.INPUT_BG);
        topInfo.add(queueProgress);

        JLabel combo = new JLabel("COMBO: x1");
        combo.setFont(new Font("Consolas", Font.BOLD, 9));
        combo.setForeground(RetroTheme.ACCENT_YELLOW);
        topInfo.add(combo);

        JLabel missId = new JLabel("MISSION ID: FCFS-01");
        missId.setFont(new Font("Consolas", Font.BOLD, 9));
        missId.setForeground(RetroTheme.TEXT_DIM);
        topInfo.add(missId);

        statsCard.add(topInfo, BorderLayout.NORTH);

        // Big Numbers Row
        JPanel numRow = new JPanel(new BorderLayout());
        numRow.setOpaque(false);

        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 4));
        metrics.setOpaque(false);

        JPanel wtBox = new JPanel();
        wtBox.setOpaque(false);
        wtBox.setLayout(new BoxLayout(wtBox, BoxLayout.Y_AXIS));
        JLabel lblWtTitle = new JLabel("AVG. WAITING TIME");
        lblWtTitle.setFont(new Font("Consolas", Font.BOLD, 9));
        lblWtTitle.setForeground(RetroTheme.TEXT_DIM);
        lblAvgWt = new JLabel("0.910 sec");
        lblAvgWt.setFont(new Font("Consolas", Font.BOLD, 20));
        lblAvgWt.setForeground(Color.WHITE);
        wtBox.add(lblWtTitle); wtBox.add(lblAvgWt);
        metrics.add(wtBox);

        JPanel tatBox = new JPanel();
        tatBox.setOpaque(false);
        tatBox.setLayout(new BoxLayout(tatBox, BoxLayout.Y_AXIS));
        JLabel lblTatTitle = new JLabel("AVG. TURNAROUND");
        lblTatTitle.setFont(new Font("Consolas", Font.BOLD, 9));
        lblTatTitle.setForeground(RetroTheme.TEXT_DIM);
        lblAvgTat = new JLabel("2.372 sec");
        lblAvgTat.setFont(new Font("Consolas", Font.BOLD, 20));
        lblAvgTat.setForeground(Color.WHITE);
        tatBox.add(lblTatTitle); tatBox.add(lblAvgTat);
        metrics.add(tatBox);

        numRow.add(metrics, BorderLayout.WEST);

        JLabel formulas = new JLabel("<html><font color='#8C8CAA' size='2'>WT = TAT - BT<br>TAT = CT - AT</font></html>");
        formulas.setFont(new Font("Consolas", Font.PLAIN, 9));
        numRow.add(formulas, BorderLayout.EAST);

        statsCard.add(numRow, BorderLayout.CENTER);
        p.add(statsCard);

        p.add(Box.createVerticalStrut(6));

        // 3. Visual Runway / Pipeline Canvas
        pipelineCanvas = new PipelineCanvas();
        pipelineCanvas.setPreferredSize(new Dimension(480, 80));
        pipelineCanvas.setMaximumSize(new Dimension(800, 80));
        pipelineCanvas.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(pipelineCanvas);

        p.add(Box.createVerticalStrut(6));

        // 4. CPU Execution Timeline (Gantt Chart)
        JPanel timelineHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        timelineHeader.setOpaque(false);
        timelineHeader.setMaximumSize(new Dimension(800, 18));
        timelineHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblGantt = new JLabel("CPU EXECUTION TIMELINE");
        lblGantt.setFont(new Font("Consolas", Font.BOLD, 10));
        lblGantt.setForeground(RetroTheme.ACCENT_CYAN);
        timelineHeader.add(lblGantt);
        p.add(timelineHeader);

        p.add(Box.createVerticalStrut(2));

        ganttCanvas = new GanttCanvas();
        ganttCanvas.setPreferredSize(new Dimension(480, 70));
        ganttCanvas.setMaximumSize(new Dimension(800, 70));
        ganttCanvas.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(ganttCanvas);

        p.add(Box.createVerticalStrut(6));

        // 5. Detailed Metrics Output Table
        outputModel = new DefaultTableModel(new String[]{"JOB #", "AT", "BT", "START (RT)", "FINISH (CT)", "WAIT (WT)", "TURN (TAT)"}, 0);
        outputTable = RetroTheme.createRetroTable(outputModel);
        outputTable.setRowHeight(22);

        JScrollPane scrollOut = RetroTheme.createRetroScroll(outputTable);
        scrollOut.setPreferredSize(new Dimension(480, 120));
        scrollOut.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(scrollOut);

        return p;
    }

    private JButton createAlgoTabBtn(String algo) {
        JButton btn = new JButton(algo) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (selectedAlgo.equals(algo)) {
                    g2.setColor(new Color(60, 25, 80));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                } else {
                    g2.setColor(RetroTheme.BTN_DARK);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                    g2.setColor(RetroTheme.BORDER_COLOR);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Consolas", Font.BOLD, 9));
        btn.setForeground(selectedAlgo.equals(algo) ? RetroTheme.ACCENT_PINK : RetroTheme.TEXT_WHITE);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            selectedAlgo = algo;
            btnFcfs.repaint(); btnSjf.repaint(); btnSrt.repaint(); btnRr.repaint();
            lblMissionStatus.setText("LIVE OUTPUT // " + algo + " FLIGHT PATH");
            runSimulation();
        });
        return btn;
    }

    private void loadDefaultMissionData() {
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"J1", "1.000",  "1.250", "1"});
        inputModel.addRow(new Object[]{"J2", "2.117",  "0.567", "1"});
        inputModel.addRow(new Object[]{"J3", "5.633",  "3.200", "1"});
        inputModel.addRow(new Object[]{"J4", "11.717", "2.000", "1"});
        inputModel.addRow(new Object[]{"J5", "13.117", "1.667", "1"});
        inputModel.addRow(new Object[]{"J6", "15.317", "0.717", "1"});
        inputModel.addRow(new Object[]{"J7", "17.533", "1.983", "1"});
    }

    public static class SimJob {
        String pid; double at, bt, rt, ct, tat, wt, remBt; int prio;
        public SimJob(String pid, double at, double bt, int prio) {
            this.pid = pid; this.at = at; this.bt = bt; this.prio = prio; this.remBt = bt;
        }
    }

    public static class GanttSlot {
        String label; double start, end;
        public GanttSlot(String l, double s, double e) { this.label = l; this.start = s; this.end = e; }
    }

    private void runSimulation() {
        if (inputModel.getRowCount() == 0) return;

        List<SimJob> jobs = new ArrayList<>();
        try {
            for (int i = 0; i < inputModel.getRowCount(); i++) {
                String pid = inputModel.getValueAt(i, 0).toString().trim();
                double at = Double.parseDouble(inputModel.getValueAt(i, 1).toString().trim());
                double bt = Double.parseDouble(inputModel.getValueAt(i, 2).toString().trim());
                int prio = Integer.parseInt(inputModel.getValueAt(i, 3).toString().trim());
                jobs.add(new SimJob(pid, at, bt, prio));
            }
        } catch (Exception ex) {
            return;
        }

        List<GanttSlot> gantt = new ArrayList<>();
        List<SimJob> results = new ArrayList<>();

        if (selectedAlgo.equals("FCFS")) {
            jobs.sort(Comparator.comparingDouble((SimJob j) -> j.at).thenComparing(j -> j.pid));
            double time = 0.0;
            for (SimJob j : jobs) {
                if (time < j.at) {
                    gantt.add(new GanttSlot("[IDLE]", time, j.at));
                    time = j.at;
                }
                j.rt = time;
                double start = time;
                time += j.bt;
                j.ct = time;
                j.tat = j.ct - j.at;
                j.wt = j.tat - j.bt;
                gantt.add(new GanttSlot(j.pid, start, time));
                results.add(j);
            }
        } else if (selectedAlgo.equals("SJF")) {
            double time = 0.0;
            List<SimJob> rem = new ArrayList<>(jobs);
            while (!rem.isEmpty()) {
                List<SimJob> avail = new ArrayList<>();
                for (SimJob j : rem) if (j.at <= time) avail.add(j);
                if (avail.isEmpty()) {
                    double nxt = rem.stream().mapToDouble(j -> j.at).min().orElse(time + 1.0);
                    gantt.add(new GanttSlot("[IDLE]", time, nxt));
                    time = nxt;
                    continue;
                }
                avail.sort(Comparator.comparingDouble((SimJob j) -> j.bt).thenComparingDouble(j -> j.at));
                SimJob pick = avail.get(0);
                rem.remove(pick);
                pick.rt = time;
                double start = time;
                time += pick.bt;
                pick.ct = time;
                pick.tat = pick.ct - pick.at;
                pick.wt = pick.tat - pick.bt;
                gantt.add(new GanttSlot(pick.pid, start, time));
                results.add(pick);
            }
        } else if (selectedAlgo.equals("SRT")) {
            double time = 0.0;
            List<SimJob> rem = new ArrayList<>(jobs);
            for (SimJob j : rem) j.remBt = j.bt;
            SimJob last = null;
            double segStart = 0.0;

            while (!rem.isEmpty()) {
                List<SimJob> avail = new ArrayList<>();
                for (SimJob j : rem) if (j.at <= time) avail.add(j);
                if (avail.isEmpty()) {
                    if (last != null) { gantt.add(new GanttSlot(last.pid, segStart, time)); last = null; }
                    double nxt = rem.stream().mapToDouble(j -> j.at).min().orElse(time + 0.5);
                    gantt.add(new GanttSlot("[IDLE]", time, nxt));
                    time = nxt; segStart = nxt;
                    continue;
                }
                avail.sort(Comparator.comparingDouble((SimJob j) -> j.remBt).thenComparingDouble(j -> j.at));
                SimJob curr = avail.get(0);

                if (last != curr) {
                    if (last != null) gantt.add(new GanttSlot(last.pid, segStart, time));
                    segStart = time; last = curr;
                }

                double step = 0.1;
                curr.remBt -= step;
                time += step;

                if (curr.remBt <= 0.001) {
                    curr.ct = time;
                    curr.tat = curr.ct - curr.at;
                    curr.wt = Math.max(0, curr.tat - curr.bt);
                    results.add(curr);
                    rem.remove(curr);
                    gantt.add(new GanttSlot(curr.pid, segStart, time));
                    last = null; segStart = time;
                }
            }
        } else {
            // Round Robin (slice = 1.0)
            double quantum = 1.0;
            jobs.sort(Comparator.comparingDouble(j -> j.at));
            Queue<SimJob> q = new LinkedList<>();
            double time = 0.0;
            int arrivedIdx = 0;
            while (arrivedIdx < jobs.size() && jobs.get(arrivedIdx).at <= time) {
                q.add(jobs.get(arrivedIdx++));
            }
            while (!q.isEmpty() || arrivedIdx < jobs.size()) {
                if (q.isEmpty()) {
                    time = jobs.get(arrivedIdx).at;
                    while (arrivedIdx < jobs.size() && jobs.get(arrivedIdx).at <= time) {
                        q.add(jobs.get(arrivedIdx++));
                    }
                }
                SimJob curr = q.poll();
                double slice = Math.min(quantum, curr.remBt);
                gantt.add(new GanttSlot(curr.pid, time, time + slice));
                time += slice;
                curr.remBt -= slice;

                while (arrivedIdx < jobs.size() && jobs.get(arrivedIdx).at <= time) {
                    q.add(jobs.get(arrivedIdx++));
                }
                if (curr.remBt > 0.001) {
                    q.add(curr);
                } else {
                    curr.ct = time;
                    curr.tat = curr.ct - curr.at;
                    curr.wt = Math.max(0, curr.tat - curr.bt);
                    results.add(curr);
                }
            }
        }

        // Render Results Table
        outputModel.setRowCount(0);
        double totWt = 0.0, totTat = 0.0;
        for (SimJob j : results) {
            outputModel.addRow(new Object[]{
                    j.pid,
                    String.format("%.3f", j.at),
                    String.format("%.3f", j.bt),
                    String.format("%.3f", j.rt),
                    String.format("%.3f", j.ct),
                    String.format("%.3f", j.wt),
                    String.format("%.3f", j.tat)
            });
            totWt += j.wt;
            totTat += j.tat;
        }

        double avgWt = results.isEmpty() ? 0.0 : totWt / results.size();
        double avgTat = results.isEmpty() ? 0.0 : totTat / results.size();
        lblAvgWt.setText(String.format("%.3f sec", avgWt));
        lblAvgTat.setText(String.format("%.3f sec", avgTat));

        ganttCanvas.setSlots(gantt);
        pipelineCanvas.setJobs(jobs);
    }

    /** Custom Visual Runway / Process Pipeline Graphic */
    public static class PipelineCanvas extends JPanel {
        private List<SimJob> jobs = new ArrayList<>();

        public PipelineCanvas() {
            setOpaque(false);
            setBorder(BorderFactory.createLineBorder(new Color(40, 30, 60), 1));
        }

        public void setJobs(List<SimJob> j) {
            this.jobs = j;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background
            g2.setColor(new Color(16, 12, 32));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

            // Subtitle
            g2.setFont(new Font("Consolas", Font.BOLD, 8));
            g2.setColor(RetroTheme.TEXT_DIM);
            g2.drawString("READY QUEUE", 12, 16);
            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.drawString("CPU WAITING FOR ARRIVAL", 150, 16);
            g2.setColor(RetroTheme.TEXT_DIM);
            g2.drawString("COMPLETED", getWidth() - 70, 16);

            // Runway Dashed Line
            g2.setColor(new Color(60, 40, 90));
            Stroke dashed = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4f, 4f}, 0.0f);
            g2.setStroke(dashed);
            g2.drawLine(20, 42, getWidth() - 100, 42);

            // Draw Alien Avatars queued up along line
            int x = 20;
            for (int i = 0; i < Math.min(jobs.size(), 7); i++) {
                BufferedImage icon = PixelSprites.getJobAvatar(i, 20);
                g2.drawImage(icon, x, 30, null);
                g2.setFont(new Font("Consolas", Font.PLAIN, 8));
                g2.setColor(Color.WHITE);
                g2.drawString(jobs.get(i).pid, x + 2, 60);
                x += 38;
            }

            // CPU Block Box (Pink outline box on right)
            int cpuW = 68;
            int cpuH = 44;
            int cpuX = getWidth() - cpuW - 12;
            int cpuY = 22;

            g2.setColor(new Color(30, 15, 45));
            g2.fillRoundRect(cpuX, cpuY, cpuW, cpuH, 6, 6);
            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(cpuX, cpuY, cpuW, cpuH, 6, 6);

            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.drawString("CPU", cpuX + 24, cpuY + 16);
            g2.setColor(Color.WHITE);
            g2.drawString("0.00 - 1.25", cpuX + 6, cpuY + 34);

            // Status message
            g2.setFont(new Font("Consolas", Font.PLAIN, 8));
            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.drawString("CPU IDLE:", 12, 74);
            g2.setColor(RetroTheme.TEXT_DIM);
            g2.drawString("Waiting for next job arrival before scheduling CPU core.", 65, 74);
        }
    }

    /** Custom Gantt Timeline with Retro Hatched Patterns */
    public static class GanttCanvas extends JPanel {
        private List<GanttSlot> slots = new ArrayList<>();
        private final Color[] PALETTE = {
            new Color(5, 217, 232),   // cyan
            new Color(140, 70, 255),  // purple
            new Color(255, 211, 25),  // yellow
            new Color(255, 42, 141),  // pink
            new Color(57, 255, 20),   // green
            new Color(255, 120, 0),   // orange
            new Color(100, 200, 255)  // ice blue
        };

        public GanttCanvas() {
            setOpaque(false);
            setBorder(BorderFactory.createLineBorder(new Color(40, 30, 60), 1));
        }

        public void setSlots(List<GanttSlot> s) {
            this.slots = s;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (slots.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            double maxTime = slots.get(slots.size() - 1).end;
            int w = getWidth() - 40;
            int startX = 20;
            double scale = maxTime > 0 ? (double) w / maxTime : 1.0;

            Map<String, Color> colorMap = new HashMap<>();
            int cIdx = 0;

            for (GanttSlot s : slots) {
                int x1 = startX + (int) (s.start * scale);
                int x2 = startX + (int) (s.end * scale);
                int bw = Math.max(x2 - x1, 2);

                if (s.label.equals("[IDLE]")) {
                    // Hatched dark pattern for IDLE
                    g2.setColor(new Color(30, 24, 46));
                    g2.fillRect(x1, 10, bw, 28);
                    g2.setColor(new Color(50, 40, 70));
                    for (int hx = x1; hx < x1 + bw; hx += 6) {
                        g2.drawLine(hx, 10, hx + 6, 38);
                    }
                    g2.setColor(RetroTheme.BORDER_COLOR);
                    g2.drawRect(x1, 10, bw, 28);
                    if (bw > 24) {
                        g2.setFont(new Font("Consolas", Font.BOLD, 8));
                        g2.setColor(Color.GRAY);
                        g2.drawString("IDLE", x1 + bw / 2 - 10, 26);
                    }
                } else {
                    if (!colorMap.containsKey(s.label)) {
                        colorMap.put(s.label, PALETTE[cIdx % PALETTE.length]);
                        cIdx++;
                    }
                    Color fill = colorMap.get(s.label);
                    g2.setColor(fill);
                    g2.fillRect(x1, 10, bw, 28);
                    g2.setColor(Color.WHITE);
                    g2.drawRect(x1, 10, bw, 28);

                    if (bw > 12) {
                        g2.setFont(new Font("Consolas", Font.BOLD, 9));
                        g2.setColor(Color.BLACK);
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(s.label, x1 + (bw - fm.stringWidth(s.label)) / 2, 26);
                    }
                }

                // Time ticks below
                g2.setFont(new Font("Consolas", Font.PLAIN, 8));
                g2.setColor(RetroTheme.TEXT_DIM);
                g2.drawString(String.format("%.1f", s.start), x1, 50);
            }
            g2.drawString(String.format("%.1f", maxTime), startX + (int) (maxTime * scale) - 6, 50);
        }
    }
}
