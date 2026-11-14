package arcadeos;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.Queue;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 * Screen 4: MISSION 01 - CPU SCHEDULING (KAWAII ARCADE EDITION)
 * -----------------------------------------------------------------------------
 * 100% Mathematically-Accurate CPU Scheduling Simulator & Arcade Dispatch Arena.
 * Completely redesigned to eliminate all empty/dead space, optimize runway
 * distribution, provide high-contrast HUD cards, and ensure maximum readability.
 */
public class CPUSchedulingPanel extends JPanel {

    // Simulation & Policy State
    private String selectedAlgo = "FCFS";
    private double timeQuantum = 2.0;
    private double currentSimTime = 0.0;
    private double maxSimTime = 20.0;
    private boolean isPlaying = false;
    private int speedMultiplier = 1;
    private boolean isPlayerChallengeMode = false;

    // Subsystems & Components
    private javax.swing.Timer simTimer;
    private DefaultTableModel inputModel;
    private JTable inputTable;
    private JScrollPane scrollInput;
    private DefaultTableModel ganttTableModel;
    private JTable ganttTable;
    private JScrollPane scrollGantt;

    private JLabel lblMissionStatus;
    private JLabel lblAvgWt;
    private JLabel lblAvgTat;
    private JLabel lblCpuClearance;
    private JLabel lblIntelTitle;
    private JLabel lblIntelRule;
    private JLabel lblIntelAdv;
    private JPanel quantumCard;
    private JSpinner spinQuantum;

    private JButton btnPlay;
    private JButton btnStep;
    private JButton btnReset;
    private JButton btnSpeed;
    private JButton btnModeToggle;

    private CPUReadyQueueCanvas runwayCanvas;
    private GanttCanvas ganttCanvas;

    // Calculation datasets
    private List<SimJob> currentJobsList = new ArrayList<>();
    private List<GanttSlot> fullGanttSlots = new ArrayList<>();
    private List<SimJob> fullResultsList = new ArrayList<>();

    public CPUSchedulingPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(14, 0));
        setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));

        // 60 FPS live simulation timer (Starts strictly STOPPED at T=0.0)
        simTimer = new javax.swing.Timer(30, e -> {
            if (isPlaying) {
                currentSimTime += 0.05 * speedMultiplier;
                if (currentSimTime >= maxSimTime) {
                    currentSimTime = maxSimTime;
                    pauseSimulation();
                    AppState.addScore(100);
                }
                updateLiveDisplay(currentSimTime);
            }
        });

        // 1. Left Column: Compact Process Control Deck (Width: 320px)
        JPanel leftCol = createLeftControlDeck();
        leftCol.setPreferredSize(new Dimension(320, 0));
        add(leftCol, BorderLayout.WEST);

        // 2. Right Column: Big Game Arena & Execution Timeline
        JPanel rightCol = createRightGameArena();
        add(rightCol, BorderLayout.CENTER);

        // Load Default Dataset & compute initial static preview at T=0.0 (Paused)
        loadDefaultMissionData();
        runSimulationMath();
        resetToBeginning();
    }

    // =========================================================================
    // 1. LEFT COLUMN: COMPACT PROCESS CONTROL DECK
    // =========================================================================
    private JPanel createLeftControlDeck() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // Header Title
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel iconBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(40, 24, 65));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(110, 60, 170));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBox.setPreferredSize(new Dimension(28, 28));
        iconBox.setOpaque(false);
        JLabel icon = new JLabel("🕹️");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        iconBox.add(icon);
        header.add(iconBox);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel subTag = new JLabel("INPUT CONSOLE");
        subTag.setFont(new Font("Consolas", Font.BOLD, 10));
        subTag.setForeground(RetroTheme.ACCENT_PINK);
        JLabel title = new JLabel("PROCESS CONTROL DECK");
        title.setFont(new Font("Consolas", Font.BOLD, 13));
        title.setForeground(Color.WHITE);
        titleBox.add(subTag); titleBox.add(title);
        header.add(titleBox);
        p.add(header);

        p.add(Box.createVerticalStrut(4));

        // Algorithm Switcher
        JPanel algoCard = new JPanel(new BorderLayout(0, 3));
        algoCard.setOpaque(false);
        algoCard.setMaximumSize(new Dimension(800, 48));
        algoCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblAlgo = new JLabel("ALGORITHM SELECTOR");
        lblAlgo.setFont(new Font("Consolas", Font.BOLD, 10));
        lblAlgo.setForeground(RetroTheme.ACCENT_CYAN);
        algoCard.add(lblAlgo, BorderLayout.NORTH);

        JPanel algoGrid = new JPanel(new GridLayout(1, 4, 3, 0));
        algoGrid.setOpaque(false);
        algoGrid.add(createAlgoTabBtn("FCFS"));
        algoGrid.add(createAlgoTabBtn("SJF"));
        algoGrid.add(createAlgoTabBtn("SRT"));
        algoGrid.add(createAlgoTabBtn("RR"));
        algoCard.add(algoGrid, BorderLayout.CENTER);
        p.add(algoCard);

        p.add(Box.createVerticalStrut(3));

        // Time Quantum Row (Visible ONLY when Round Robin is selected)
        quantumCard = new JPanel(new BorderLayout(8, 0));
        quantumCard.setOpaque(false);
        quantumCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_YELLOW, 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        quantumCard.setMaximumSize(new Dimension(800, 26));
        quantumCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        quantumCard.setVisible(false);

        JLabel qLbl = new JLabel("TIME QUANTUM (Q):");
        qLbl.setFont(new Font("Consolas", Font.BOLD, 10));
        qLbl.setForeground(RetroTheme.ACCENT_YELLOW);
        quantumCard.add(qLbl, BorderLayout.WEST);

        spinQuantum = new JSpinner(new SpinnerNumberModel(2.0, 0.5, 10.0, 0.5));
        spinQuantum.setFont(new Font("Consolas", Font.BOLD, 10));
        spinQuantum.addChangeListener(e -> {
            timeQuantum = (Double) spinQuantum.getValue();
            recomputeAll();
        });
        quantumCard.add(spinQuantum, BorderLayout.EAST);
        p.add(quantumCard);

        p.add(Box.createVerticalStrut(3));

        // Editable Process Queue Table
        JPanel queueHeader = new JPanel(new BorderLayout());
        queueHeader.setOpaque(false);
        queueHeader.setMaximumSize(new Dimension(800, 22));
        queueHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel qTitle = new JLabel("EDITABLE JOBS");
        qTitle.setFont(new Font("Consolas", Font.BOLD, 10));
        qTitle.setForeground(Color.WHITE);
        queueHeader.add(qTitle, BorderLayout.WEST);

        JPanel qRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        qRight.setOpaque(false);

        JButton btnAddJob = RetroTheme.createRetroButton("+ ADD", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnAddJob.setFont(new Font("Consolas", Font.BOLD, 9));
        btnAddJob.addActionListener(e -> {
            int n = inputModel.getRowCount() + 1;
            inputModel.addRow(new Object[]{"J" + n, "0.0", "2.0", "1"});
            recomputeAll();
        });

        JButton btnRemoveJob = RetroTheme.createRetroButton("- REMOVE", RetroTheme.BTN_DARK, RetroTheme.ACCENT_PINK);
        btnRemoveJob.setFont(new Font("Consolas", Font.BOLD, 9));
        btnRemoveJob.addActionListener(e -> {
            int sel = inputTable.getSelectedRow();
            if (sel >= 0 && inputModel.getRowCount() > 1) {
                inputModel.removeRow(sel);
                recomputeAll();
            } else if (inputModel.getRowCount() > 1) {
                inputModel.removeRow(inputModel.getRowCount() - 1);
                recomputeAll();
            }
        });

        qRight.add(btnAddJob);
        qRight.add(btnRemoveJob);
        queueHeader.add(qRight, BorderLayout.EAST);
        p.add(queueHeader);

        p.add(Box.createVerticalStrut(2));

        String[] cols = {"JOB", "AT (s)", "BT (s)", "PRIO"};
        inputModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column > 0;
            }
        };

        inputTable = new JTable(inputModel);
        inputTable.setFont(new Font("Consolas", Font.BOLD, 11));
        inputTable.setRowHeight(21);
        inputTable.setBackground(new Color(18, 14, 34));
        inputTable.setForeground(Color.WHITE);
        inputTable.setGridColor(new Color(45, 32, 68));
        inputTable.setSelectionBackground(new Color(110, 40, 150));
        inputTable.setSelectionForeground(Color.WHITE);
        inputTable.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 10));
        inputTable.getTableHeader().setBackground(new Color(28, 20, 50));
        inputTable.getTableHeader().setForeground(RetroTheme.ACCENT_CYAN);

        inputTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        inputTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        inputTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        inputTable.getColumnModel().getColumn(3).setPreferredWidth(45);

        inputTable.getModel().addTableModelListener(e -> recomputeAll());

        scrollInput = RetroTheme.createRetroScroll(inputTable);
        scrollInput.setPreferredSize(new Dimension(310, 175));
        scrollInput.setMaximumSize(new Dimension(800, 190));
        scrollInput.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(scrollInput);

        p.add(Box.createVerticalStrut(3));

        // Action Controls (START, STEP, RESET, SPEED)
        JPanel controlsRow = new JPanel(new GridLayout(1, 4, 3, 0));
        controlsRow.setOpaque(false);
        controlsRow.setMaximumSize(new Dimension(800, 26));
        controlsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnPlay = RetroTheme.createRetroButton("▶ START", RetroTheme.BTN_DARK, RetroTheme.ACCENT_GREEN);
        btnStep = RetroTheme.createRetroButton("⏩ STEP", RetroTheme.BTN_DARK, RetroTheme.TEXT_WHITE);
        btnReset = RetroTheme.createRetroButton("🔄 RESET", RetroTheme.BTN_DARK, RetroTheme.ACCENT_PINK);
        btnSpeed = RetroTheme.createRetroButton("1X", RetroTheme.BTN_DARK, RetroTheme.ACCENT_YELLOW);

        btnPlay.setFont(new Font("Consolas", Font.BOLD, 10));
        btnStep.setFont(new Font("Consolas", Font.BOLD, 10));
        btnReset.setFont(new Font("Consolas", Font.BOLD, 10));
        btnSpeed.setFont(new Font("Consolas", Font.BOLD, 10));

        btnPlay.addActionListener(e -> toggleSimulationPlay());
        btnStep.addActionListener(e -> stepSimulationForward());
        btnReset.addActionListener(e -> resetToBeginning());
        btnSpeed.addActionListener(e -> {
            if (speedMultiplier == 1) speedMultiplier = 2;
            else if (speedMultiplier == 2) speedMultiplier = 4;
            else speedMultiplier = 1;
            btnSpeed.setText(speedMultiplier + "X");
        });

        controlsRow.add(btnPlay);
        controlsRow.add(btnStep);
        controlsRow.add(btnReset);
        controlsRow.add(btnSpeed);
        p.add(controlsRow);

        p.add(Box.createVerticalStrut(3));

        // Interactive Dispatch Mode Button
        btnModeToggle = new JButton("🕹️  INTERACTIVE PLAYER MODE: OFF") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isPlayerChallengeMode ? new Color(180, 20, 90) : new Color(30, 22, 54));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(isPlayerChallengeMode ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_CYAN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnModeToggle.setFont(new Font("Consolas", Font.BOLD, 10));
        btnModeToggle.setForeground(Color.WHITE);
        btnModeToggle.setFocusPainted(false);
        btnModeToggle.setOpaque(false);
        btnModeToggle.setContentAreaFilled(false);
        btnModeToggle.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        btnModeToggle.setMaximumSize(new Dimension(800, 24));
        btnModeToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnModeToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnModeToggle.addActionListener(e -> {
            isPlayerChallengeMode = !isPlayerChallengeMode;
            btnModeToggle.setText(isPlayerChallengeMode ? "🕹️  INTERACTIVE PLAYER MODE: ON" : "🕹️  INTERACTIVE PLAYER MODE: OFF");
            btnModeToggle.repaint();
            pauseSimulation();
            runwayCanvas.setChallengeMode(isPlayerChallengeMode);
        });
        p.add(btnModeToggle);

        p.add(Box.createVerticalStrut(3));

        // Dedicated 16-Bit Arcade Cabinet Button
        JButton btnArcadeTapper = new JButton("👾  PLAY KERNEL TAPPER // 16-BIT ARCADE") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(42, 20, 68));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnArcadeTapper.setFont(new Font("Consolas", Font.BOLD, 10));
        btnArcadeTapper.setForeground(RetroTheme.ACCENT_YELLOW);
        btnArcadeTapper.setFocusPainted(false);
        btnArcadeTapper.setOpaque(false);
        btnArcadeTapper.setContentAreaFilled(false);
        btnArcadeTapper.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        btnArcadeTapper.setMaximumSize(new Dimension(800, 24));
        btnArcadeTapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnArcadeTapper.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnArcadeTapper.addActionListener(e -> {
            pauseSimulation();
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(CPUSchedulingPanel.this);
            List<SimJob> currentJobs = getJobsFromTable();
            KernelTapperDialog dlg = new KernelTapperDialog(topFrame, selectedAlgo, currentJobs);
            dlg.setVisible(true);
        });
        p.add(btnArcadeTapper);

        p.add(Box.createVerticalStrut(3));

        // Benchmark Presets
        JPanel presetCard = new JPanel(new BorderLayout(0, 2));
        presetCard.setOpaque(false);
        presetCard.setMaximumSize(new Dimension(800, 38));
        presetCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel presetTitle = new JLabel("⚡ SCENARIO BENCHMARK PRESETS");
        presetTitle.setFont(new Font("Consolas", Font.BOLD, 9));
        presetTitle.setForeground(RetroTheme.ACCENT_CYAN);
        presetCard.add(presetTitle, BorderLayout.NORTH);

        JPanel presetBtns = new JPanel(new GridLayout(1, 4, 3, 0));
        presetBtns.setOpaque(false);

        JButton p1 = RetroTheme.createRetroButton("DEFAULT", RetroTheme.BTN_DARK, RetroTheme.TEXT_WHITE);
        p1.setFont(new Font("Consolas", Font.BOLD, 9));
        p1.addActionListener(e -> loadDefaultMissionData());

        JButton p2 = RetroTheme.createRetroButton("CONVOY", RetroTheme.BTN_DARK, RetroTheme.ACCENT_PINK);
        p2.setFont(new Font("Consolas", Font.BOLD, 9));
        p2.addActionListener(e -> loadConvoyDataset());

        JButton p3 = RetroTheme.createRetroButton("SJF RUSH", RetroTheme.BTN_DARK, RetroTheme.ACCENT_YELLOW);
        p3.setFont(new Font("Consolas", Font.BOLD, 9));
        p3.addActionListener(e -> loadSjfRushDataset());

        JButton p4 = RetroTheme.createRetroButton("RR TEST", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        p4.setFont(new Font("Consolas", Font.BOLD, 9));
        p4.addActionListener(e -> loadRoundRobinDataset());

        presetBtns.add(p1); presetBtns.add(p2); presetBtns.add(p3); presetBtns.add(p4);
        presetCard.add(presetBtns, BorderLayout.CENTER);
        p.add(presetCard);

        p.add(Box.createVerticalStrut(3));

        // Algorithm Policy Intel
        JPanel intelCard = new JPanel();
        intelCard.setLayout(new BoxLayout(intelCard, BoxLayout.Y_AXIS));
        intelCard.setOpaque(true);
        intelCard.setBackground(new Color(22, 16, 42));
        intelCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        intelCard.setMaximumSize(new Dimension(800, 52));
        intelCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblIntelTitle = new JLabel("POLICY: FIRST-COME, FIRST-SERVED");
        lblIntelTitle.setFont(new Font("Consolas", Font.BOLD, 10));
        lblIntelTitle.setForeground(RetroTheme.ACCENT_CYAN);

        lblIntelRule = new JLabel("• Rule: FIFO Ready Queue. Non-preemptive.");
        lblIntelRule.setFont(new Font("Consolas", Font.PLAIN, 9));
        lblIntelRule.setForeground(RetroTheme.TEXT_WHITE);

        lblIntelAdv = new JLabel("• Warning: Convoy Effect on long CPU bursts.");
        lblIntelAdv.setFont(new Font("Consolas", Font.PLAIN, 9));
        lblIntelAdv.setForeground(RetroTheme.ACCENT_YELLOW);

        intelCard.add(lblIntelTitle);
        intelCard.add(lblIntelRule);
        intelCard.add(lblIntelAdv);
        p.add(intelCard);

        p.add(Box.createVerticalStrut(3));

        // Hardware Telemetry Strip
        JPanel telemetryCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        telemetryCard.setOpaque(true);
        telemetryCard.setBackground(new Color(15, 12, 28));
        telemetryCard.setBorder(BorderFactory.createLineBorder(new Color(45, 32, 65), 1));
        telemetryCard.setMaximumSize(new Dimension(800, 20));
        telemetryCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel t1 = new JLabel("● 1x CPU CORE");
        t1.setFont(new Font("Consolas", Font.BOLD, 9));
        t1.setForeground(RetroTheme.ACCENT_GREEN);

        JLabel t2 = new JLabel("3.8GHz");
        t2.setFont(new Font("Consolas", Font.BOLD, 9));
        t2.setForeground(RetroTheme.TEXT_WHITE);

        JLabel t3 = new JLabel("RUNWAY READY");
        t3.setFont(new Font("Consolas", Font.BOLD, 9));
        t3.setForeground(RetroTheme.ACCENT_CYAN);

        telemetryCard.add(t1);
        telemetryCard.add(t2);
        telemetryCard.add(t3);
        p.add(telemetryCard);

        return p;
    }

    // =========================================================================
    // 2. RIGHT COLUMN: BIG GAME ARENA + GANTT TIMELINE + METRICS
    // =========================================================================
    private JPanel createRightGameArena() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        // Top Status Strip
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(1400, 24));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblMissionStatus = new JLabel("✦ LIVE OUTPUT // FCFS READY QUEUE & CPU REACTOR");
        lblMissionStatus.setFont(new Font("Consolas", Font.BOLD, 13));
        lblMissionStatus.setForeground(Color.WHITE);
        header.add(lblMissionStatus, BorderLayout.WEST);

        JLabel pill = new JLabel(" [ READY // CLICK ▶ START ] ");
        pill.setFont(new Font("Consolas", Font.BOLD, 11));
        pill.setForeground(RetroTheme.ACCENT_GREEN);
        pill.setBorder(BorderFactory.createLineBorder(RetroTheme.ACCENT_GREEN, 1));
        header.add(pill, BorderLayout.EAST);
        p.add(header);

        p.add(Box.createVerticalStrut(4));

        // 3 High-Impact Glowing HUD Cards (Symmetrical, Zero empty gap)
        JPanel statsCard = new JPanel(new GridLayout(1, 3, 10, 0));
        statsCard.setOpaque(false);
        statsCard.setMaximumSize(new Dimension(1400, 70));
        statsCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Pod 1: Waiting Time
        JPanel p1 = createGlassHudPod("⏱️ AVG WAITING (WT)", RetroTheme.ACCENT_CYAN);
        lblAvgWt = new JLabel("0.000 sec");
        lblAvgWt.setFont(new Font("Consolas", Font.BOLD, 22));
        lblAvgWt.setForeground(RetroTheme.ACCENT_CYAN);
        JLabel sub1 = new JLabel("WT = TAT - BT");
        sub1.setFont(new Font("Consolas", Font.BOLD, 10));
        sub1.setForeground(new Color(175, 175, 205));
        p1.add(lblAvgWt); p1.add(sub1);
        statsCard.add(p1);

        // Pod 2: Turnaround Time
        JPanel p2 = createGlassHudPod("🔄 AVG TURNAROUND", RetroTheme.ACCENT_YELLOW);
        lblAvgTat = new JLabel("0.000 sec");
        lblAvgTat.setFont(new Font("Consolas", Font.BOLD, 22));
        lblAvgTat.setForeground(RetroTheme.ACCENT_YELLOW);
        JLabel sub2 = new JLabel("TAT = CT - AT");
        sub2.setFont(new Font("Consolas", Font.BOLD, 10));
        sub2.setForeground(new Color(175, 175, 205));
        p2.add(lblAvgTat); p2.add(sub2);
        statsCard.add(p2);

        // Pod 3: Queue Clearance & CPU Load
        JPanel p3 = createGlassHudPod("⚡ QUEUE CLEARANCE", RetroTheme.ACCENT_GREEN);
        lblCpuClearance = new JLabel("100% CLEAR");
        lblCpuClearance.setFont(new Font("Consolas", Font.BOLD, 22));
        lblCpuClearance.setForeground(RetroTheme.ACCENT_GREEN);
        JLabel sub3 = new JLabel("7/7 JOBS (1x 3.8GHz CORE)");
        sub3.setFont(new Font("Consolas", Font.BOLD, 10));
        sub3.setForeground(new Color(175, 175, 205));
        p3.add(lblCpuClearance); p3.add(sub3);
        statsCard.add(p3);

        p.add(statsCard);

        p.add(Box.createVerticalStrut(6));

        // 3. THE BIG GAME CANVAS: CPU DISPATCH ARENA (Height 270px)
        runwayCanvas = new CPUReadyQueueCanvas(this);
        runwayCanvas.setPreferredSize(new Dimension(880, 270));
        runwayCanvas.setMinimumSize(new Dimension(880, 270));
        runwayCanvas.setMaximumSize(new Dimension(1400, 270));
        runwayCanvas.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(runwayCanvas);

        p.add(Box.createVerticalStrut(6));

        // 4. Gantt Execution Timeline
        JPanel ganttBox = new JPanel(new BorderLayout());
        ganttBox.setOpaque(false);
        ganttBox.setMaximumSize(new Dimension(1400, 80));
        ganttBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel ganttHead = new JPanel(new BorderLayout());
        ganttHead.setOpaque(false);
        JLabel gh = new JLabel("CPU EXECUTION TIMELINE (GANTT RECORDER)");
        gh.setFont(new Font("Consolas", Font.BOLD, 11));
        gh.setForeground(RetroTheme.ACCENT_CYAN);
        JLabel gr = new JLabel("LIVE FLIGHT TAPE");
        gr.setFont(new Font("Consolas", Font.BOLD, 10));
        gr.setForeground(RetroTheme.TEXT_WHITE);
        ganttHead.add(gh, BorderLayout.WEST);
        ganttHead.add(gr, BorderLayout.EAST);
        ganttBox.add(ganttHead, BorderLayout.NORTH);

        ganttCanvas = new GanttCanvas();
        ganttCanvas.setPreferredSize(new Dimension(880, 64));
        ganttCanvas.setMinimumSize(new Dimension(880, 64));
        ganttBox.add(ganttCanvas, BorderLayout.CENTER);
        p.add(ganttBox);

        p.add(Box.createVerticalStrut(6));

        // 5. Textbook Scheduling Results Table (Clean, Fits all 7 rows without cut-off)
        String[] rCols = {"JOB #", "ARRIVAL (AT)", "BURST (BT)", "START (RT)", "FINISH (CT)", "WAIT (WT)", "TURN (TAT)"};
        ganttTableModel = new DefaultTableModel(rCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ganttTable = new JTable(ganttTableModel);
        ganttTable.setFont(new Font("Consolas", Font.BOLD, 11));
        ganttTable.setRowHeight(22);
        ganttTable.setBackground(new Color(16, 12, 30));
        ganttTable.setForeground(Color.WHITE);
        ganttTable.setGridColor(new Color(45, 32, 65));
        ganttTable.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 11));
        ganttTable.getTableHeader().setBackground(new Color(28, 20, 50));
        ganttTable.getTableHeader().setForeground(RetroTheme.ACCENT_CYAN);

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < ganttTable.getColumnCount(); i++) {
            ganttTable.getColumnModel().getColumn(i).setCellRenderer(centerRender);
        }

        scrollGantt = RetroTheme.createRetroScroll(ganttTable);
        scrollGantt.setPreferredSize(new Dimension(880, 185));
        scrollGantt.setMaximumSize(new Dimension(1400, 200));
        scrollGantt.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(scrollGantt);

        return p;
    }

    private JPanel createGlassHudPod(String title, Color borderCol) {
        JPanel pod = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(24, 18, 44));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(borderCol.getRed(), borderCol.getGreen(), borderCol.getBlue(), 120));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(borderCol);
                g2.fillRoundRect(0, 3, 3, getHeight() - 6, 2, 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pod.setOpaque(false);
        pod.setLayout(new BoxLayout(pod, BoxLayout.Y_AXIS));
        pod.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 10));

        JLabel l = new JLabel(title);
        l.setFont(new Font("Consolas", Font.BOLD, 10));
        l.setForeground(Color.WHITE);
        pod.add(l);
        return pod;
    }

    // =========================================================================
    // BUTTON BUILDERS & ALGORITHM SWITCHING
    // =========================================================================
    private JButton createAlgoTabBtn(String algo) {
        JButton btn = new JButton(algo) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isSel = selectedAlgo.equals(algo) || (algo.equals("RR") && selectedAlgo.equals("ROUND ROBIN"));
                g2.setColor(isSel ? new Color(120, 20, 75) : new Color(30, 20, 50));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(isSel ? RetroTheme.ACCENT_PINK : new Color(65, 45, 95));
                g2.setStroke(new BasicStroke(isSel ? 2.0f : 1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Consolas", Font.BOLD, 10));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            selectedAlgo = algo.equals("RR") ? "ROUND ROBIN" : algo;
            quantumCard.setVisible(selectedAlgo.equals("ROUND ROBIN"));
            lblMissionStatus.setText("✦ LIVE OUTPUT // " + selectedAlgo + " READY QUEUE & CPU REACTOR");
            updatePolicyIntel(selectedAlgo);
            recomputeAll();
            repaint();
        });
        return btn;
    }

    private void updatePolicyIntel(String algo) {
        switch (algo) {
            case "FCFS":
                lblIntelTitle.setText("POLICY: FIRST-COME, FIRST-SERVED");
                lblIntelRule.setText("• Rule: FIFO Ready Queue. Non-preemptive.");
                lblIntelAdv.setText("• Warning: Convoy Effect on long CPU bursts.");
                break;
            case "SJF":
                lblIntelTitle.setText("POLICY: SHORTEST JOB FIRST");
                lblIntelRule.setText("• Rule: Smallest Burst Time (BT) executed first.");
                lblIntelAdv.setText("• Advantage: Optimal minimal average waiting time.");
                break;
            case "SRT":
                lblIntelTitle.setText("POLICY: SHORTEST REMAINING TIME");
                lblIntelRule.setText("• Rule: Preemptive SJF. Switches if new arrival is shorter.");
                lblIntelAdv.setText("• Note: Frequent context switching on burst arrivals.");
                break;
            case "ROUND ROBIN":
                lblIntelTitle.setText("POLICY: ROUND ROBIN (TIME-SHARED)");
                lblIntelRule.setText("• Rule: Fixed Time Quantum Q per CPU turn.");
                lblIntelAdv.setText("• Fair time sharing: Unfinished jobs push to tail.");
                break;
        }
    }

    // =========================================================================
    // DATASET LOADERS
    // =========================================================================
    private void loadDefaultMissionData() {
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"J1", "1.500", "1.250", "1"});
        inputModel.addRow(new Object[]{"J2", "2.117", "0.567", "1"});
        inputModel.addRow(new Object[]{"J3", "5.633", "3.200", "1"});
        inputModel.addRow(new Object[]{"J4", "11.717", "2.000", "1"});
        inputModel.addRow(new Object[]{"J5", "12.417", "1.467", "1"});
        inputModel.addRow(new Object[]{"J6", "13.117", "0.717", "1"});
        inputModel.addRow(new Object[]{"J7", "15.533", "0.933", "1"});
        recomputeAll();
    }

    private void loadConvoyDataset() {
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"J1", "0.0", "10.0", "1"});
        inputModel.addRow(new Object[]{"J2", "1.0", "1.0", "1"});
        inputModel.addRow(new Object[]{"J3", "2.0", "1.0", "1"});
        inputModel.addRow(new Object[]{"J4", "3.0", "1.0", "1"});
        recomputeAll();
    }

    private void loadSjfRushDataset() {
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"J1", "0.0", "4.0", "1"});
        inputModel.addRow(new Object[]{"J2", "1.0", "1.0", "1"});
        inputModel.addRow(new Object[]{"J3", "1.5", "2.0", "1"});
        inputModel.addRow(new Object[]{"J4", "2.0", "0.5", "1"});
        inputModel.addRow(new Object[]{"J5", "3.0", "1.2", "1"});
        recomputeAll();
    }

    private void loadRoundRobinDataset() {
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"J1", "0.0", "5.0", "1"});
        inputModel.addRow(new Object[]{"J2", "1.0", "4.0", "1"});
        inputModel.addRow(new Object[]{"J3", "2.0", "2.0", "1"});
        inputModel.addRow(new Object[]{"J4", "3.0", "1.0", "1"});
        if (!selectedAlgo.equals("ROUND ROBIN")) {
            selectedAlgo = "ROUND ROBIN";
            quantumCard.setVisible(true);
            lblMissionStatus.setText("✦ LIVE OUTPUT // ROUND ROBIN READY QUEUE & CPU REACTOR");
            updatePolicyIntel("ROUND ROBIN");
        }
        recomputeAll();
    }

    // =========================================================================
    // SIMULATION ENGINE & ALGORITHM MATH
    // =========================================================================
    public static class SimJob {
        public String pid;
        public double at, bt, rt, ct, tat, wt, remBt;
        public int prio;
        public SimJob(String pid, double at, double bt, int prio) {
            this.pid = pid; this.at = at; this.bt = bt; this.prio = prio; this.remBt = bt;
        }
    }

    public static class GanttSlot {
        public String label;
        public double start, end;
        public GanttSlot(String l, double s, double e) { this.label = l; this.start = s; this.end = e; }
    }

    public List<SimJob> getJobsFromTable() {
        List<SimJob> list = new ArrayList<>();
        for (int i = 0; i < inputModel.getRowCount(); i++) {
            try {
                String pid = inputModel.getValueAt(i, 0).toString().trim();
                double at = Double.parseDouble(inputModel.getValueAt(i, 1).toString().trim());
                double bt = Double.parseDouble(inputModel.getValueAt(i, 2).toString().trim());
                int prio = Integer.parseInt(inputModel.getValueAt(i, 3).toString().trim());
                list.add(new SimJob(pid, at, bt, prio));
            } catch (Exception ignored) {}
        }
        return list;
    }

    public void recomputeAll() {
        pauseSimulation();
        runSimulationMath();
        resetToBeginning();
    }

    private void toggleSimulationPlay() {
        if (isPlaying) {
            pauseSimulation();
        } else {
            startSimulation();
        }
    }

    private void startSimulation() {
        if (fullGanttSlots.isEmpty()) runSimulationMath();
        if (currentSimTime >= maxSimTime) currentSimTime = 0.0;
        isPlaying = true;
        btnPlay.setText("⏸ PAUSE");
        btnPlay.setForeground(RetroTheme.ACCENT_YELLOW);
        simTimer.start();
    }

    private void pauseSimulation() {
        isPlaying = false;
        btnPlay.setText("▶ START");
        btnPlay.setForeground(RetroTheme.ACCENT_GREEN);
        simTimer.stop();
    }

    private void stepSimulationForward() {
        pauseSimulation();
        if (fullGanttSlots.isEmpty()) runSimulationMath();
        if (currentSimTime >= maxSimTime) {
            currentSimTime = 0.0;
        } else {
            double nextTarget = currentSimTime + 0.5;
            for (GanttSlot s : fullGanttSlots) {
                if (s.start > currentSimTime + 0.01) {
                    nextTarget = Math.min(nextTarget, s.start);
                    break;
                } else if (s.end > currentSimTime + 0.01) {
                    nextTarget = Math.min(nextTarget, s.end);
                    break;
                }
            }
            currentSimTime = Math.min(maxSimTime, nextTarget);
        }
        updateLiveDisplay(currentSimTime);
    }

    private void resetToBeginning() {
        pauseSimulation();
        currentSimTime = 0.0;
        updateLiveDisplay(0.0);
    }

    /**
     * Exact Mathematical CPU Scheduling Algorithms:
     * - FCFS: Non-preemptive, no quantum.
     * - SJF: Non-preemptive, no quantum.
     * - SRT: Preemptive on shorter remaining arrival, no quantum.
     * - ROUND ROBIN: Time quantum Q, circular queue.
     */
    private void runSimulationMath() {
        List<SimJob> jobs = getJobsFromTable();
        if (jobs.isEmpty()) return;

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

                double step = 0.05;
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
            // Round Robin with Quantum Q
            double quantum = timeQuantum;
            jobs.sort(Comparator.comparingDouble(j -> j.at));
            for (SimJob j : jobs) j.remBt = j.bt;
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

        this.fullGanttSlots = gantt;
        this.fullResultsList = results;
        this.currentJobsList = jobs;
        this.maxSimTime = gantt.isEmpty() ? 0.0 : gantt.get(gantt.size() - 1).end;

        // Compute Averages
        double sumWt = 0.0, sumTat = 0.0;
        for (SimJob j : fullResultsList) {
            sumWt += j.wt;
            sumTat += j.tat;
        }
        int count = Math.max(1, fullResultsList.size());
        lblAvgWt.setText(String.format("%.3f sec", sumWt / count));
        lblAvgTat.setText(String.format("%.3f sec", sumTat / count));
    }

    private void updateLiveDisplay(double time) {
        if (fullGanttSlots.isEmpty()) return;

        GanttSlot activeSlot = null;
        for (GanttSlot s : fullGanttSlots) {
            if (time >= s.start && time < s.end) {
                activeSlot = s;
                break;
            }
        }
        if (activeSlot == null && !fullGanttSlots.isEmpty()) {
            activeSlot = fullGanttSlots.get(fullGanttSlots.size() - 1);
        }

        // Update Big Canvas Runway & CPU Core
        runwayCanvas.updateState(time, activeSlot, currentJobsList, selectedAlgo, timeQuantum, maxSimTime);

        // Update Gantt Canvas Timeline
        ganttCanvas.setSimulationState(fullGanttSlots, time, maxSimTime);

        // Update Metrics Table
        ganttTableModel.setRowCount(0);
        int finishedCount = 0;
        for (SimJob j : fullResultsList) {
            boolean isFinished = (time >= j.ct);
            boolean isRunning = (activeSlot != null && activeSlot.label.equals(j.pid));
            if (isFinished) finishedCount++;

            String statusTag = isFinished ? "✔ DONE" : (isRunning ? "⚡ RUNNING" : "WAITING");

            ganttTableModel.addRow(new Object[]{
                    j.pid + (isRunning ? " [RUN]" : ""),
                    String.format("%.3f", j.at),
                    String.format("%.3f", j.bt),
                    String.format("%.3f", j.rt),
                    String.format("%.3f", j.ct),
                    String.format("%.3f", j.wt),
                    String.format("%.3f", j.tat)
            });
        }

        // Ensure table shows row 0 without unintended vertical scroll
        if (scrollGantt != null && scrollGantt.getViewport() != null) {
            scrollGantt.getViewport().setViewPosition(new Point(0, 0));
        }

        int totalJobs = fullResultsList.size();
        int pct = totalJobs == 0 ? 0 : (finishedCount * 100 / totalJobs);
        lblCpuClearance.setText(pct + "% CLEAR (" + finishedCount + "/" + totalJobs + ")");
        lblCpuClearance.setForeground(pct == 100 ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_YELLOW);
    }

    // =========================================================================
    // 3. THE BIG GAME CANVAS: CPU DISPATCH RUNWAY & CPU REACTOR MAIN ARENA
    // =========================================================================
    public static class CPUReadyQueueCanvas extends JPanel {
        private final CPUSchedulingPanel parent;

        private double currentTime = 0.0;
        private String activePid = "[IDLE]";
        private double activeStart = 0.0;
        private double activeEnd = 0.0;
        private List<SimJob> readyQueue = new ArrayList<>();
        private List<SimJob> allJobs = new ArrayList<>();
        private List<SimJob> completedList = new ArrayList<>();
        private String algoName = "FCFS";
        private double quantum = 2.0;
        private double maxTime = 20.0;
        private int tickCount = 0;

        private boolean challengeMode = false;
        private List<Rectangle> clickableCards = new ArrayList<>();
        private List<SimJob> clickableJobs = new ArrayList<>();
        private String feedbackBanner = "";
        private Color feedbackColor = RetroTheme.ACCENT_CYAN;
        private int feedbackTimer = 0;

        private BufferedImage playerSprite;

        public CPUReadyQueueCanvas(CPUSchedulingPanel parent) {
            this.parent = parent;
            setBackground(new Color(14, 10, 26));
            setOpaque(true);
            setBorder(BorderFactory.createLineBorder(new Color(65, 45, 95), 1));
            playerSprite = PixelSprites.createPlayerSprite(36);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!challengeMode) return;
                    for (int i = 0; i < clickableCards.size(); i++) {
                        if (clickableCards.get(i).contains(e.getPoint())) {
                            SimJob clicked = clickableJobs.get(i);
                            handlePlayerDispatchClick(clicked);
                            return;
                        }
                    }
                }
            });

            new javax.swing.Timer(50, e -> {
                tickCount++;
                if (feedbackTimer > 0) {
                    feedbackTimer--;
                    if (feedbackTimer == 0) feedbackBanner = "";
                }
                repaint();
            }).start();
        }

        public void setChallengeMode(boolean active) {
            this.challengeMode = active;
            if (active) {
                feedbackBanner = "INTERACTIVE MODE: Click the next Alien to dispatch by policy!";
                feedbackColor = RetroTheme.ACCENT_YELLOW;
                feedbackTimer = 90;
            } else {
                feedbackBanner = "";
            }
            repaint();
        }

        private void handlePlayerDispatchClick(SimJob clicked) {
            if (readyQueue.isEmpty()) return;
            SimJob expected = readyQueue.get(0);
            if (clicked == expected) {
                feedbackBanner = "PERFECT DISPATCH! (+50 XP)";
                feedbackColor = RetroTheme.ACCENT_GREEN;
                feedbackTimer = 50;
                AppState.addScore(50);
            } else {
                feedbackBanner = "POLICY VIOLATION! Expected: " + expected.pid;
                feedbackColor = RetroTheme.ACCENT_PINK;
                feedbackTimer = 60;
            }
            repaint();
        }

        public void updateState(double time, GanttSlot slot, List<SimJob> jobs, String algo, double q, double maxTime) {
            this.currentTime = time;
            this.allJobs = jobs != null ? jobs : new ArrayList<>();
            this.algoName = algo;
            this.quantum = q;
            this.maxTime = maxTime;

            if (slot != null) {
                this.activePid = slot.label;
                this.activeStart = slot.start;
                this.activeEnd = slot.end;
            } else {
                this.activePid = "[IDLE]";
            }

            // Build Active Ready Queue
            List<SimJob> queue = new ArrayList<>();
            List<SimJob> comp = new ArrayList<>();

            for (SimJob j : allJobs) {
                if (time >= j.ct && j.ct > 0.001) {
                    comp.add(j);
                } else if (j.at <= time) {
                    if (!activePid.equals(j.pid)) {
                        queue.add(j);
                    }
                }
            }

            // Sort queue by active algorithm policy
            if (algo.equals("FCFS")) {
                queue.sort(Comparator.comparingDouble(j -> j.at));
            } else if (algo.equals("SJF") || algo.equals("SRT")) {
                queue.sort(Comparator.comparingDouble(j -> j.bt));
            }

            this.readyQueue = queue;
            this.completedList = comp;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Cyber Grid Background with High-Tech Floor
            g2.setColor(new Color(15, 11, 28));
            g2.fillRoundRect(0, 0, w, h, 8, 8);
            g2.setColor(new Color(28, 20, 48));
            for (int gx = 0; gx < w; gx += 28) g2.drawLine(gx, 0, gx, h);
            for (int gy = 0; gy < h; gy += 28) g2.drawLine(0, gy, w, gy);

            // 2. Top Arena HUD Banner
            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("READY QUEUE RUNWAY (" + readyQueue.size() + " READY)", 16, 20);

            if (challengeMode) {
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawString("🕹️ [CLICK THE ALIEN IN QUEUE TO DISPATCH ACCORDING TO " + algoName + "]", 220, 20);
            } else {
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(Color.WHITE);
                g2.drawString(activePid.equals("[IDLE]") ? "CPU STATUS: IDLE / SCANNING" : "CPU EXECUTING: " + activePid, 220, 20);
            }

            if (feedbackTimer > 0 && !feedbackBanner.isEmpty()) {
                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.setColor(feedbackColor);
                g2.drawString(feedbackBanner, w / 2 - 120, 38);
            }

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString("COMPLETED (" + completedList.size() + "/" + allJobs.size() + ")", w - 165, 20);

            // 3. Layout Geometry (Evenly Distributed Runway across full width)
            int cpuW = 230;
            int cpuH = 175;
            int cpuX = w - cpuW - 14;
            int lineY = 135;
            int cpuY = lineY - 80;

            int runwayStartX = 72;
            int runwayEndX = cpuX - 10;
            int runwayWidth = runwayEndX - runwayStartX;

            // Draw High-Tech Maglev Dual Rails (Top and Bottom glow tracks)
            g2.setColor(new Color(60, 40, 95));
            g2.fillRect(runwayStartX - 6, lineY - 60, runwayWidth + 12, 120);

            g2.setColor(new Color(20, 15, 38));
            g2.fillRect(runwayStartX - 4, lineY - 58, runwayWidth + 8, 116);

            // Glowing Dual Rail Lines
            g2.setColor(new Color(110, 70, 170));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawLine(runwayStartX - 4, lineY - 58, cpuX - 2, lineY - 58); // Top Rail
            g2.drawLine(runwayStartX - 4, lineY + 58, cpuX - 2, lineY + 58); // Bottom Rail

            // Animated Maglev Directional Chevrons along runway
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(new Color(90, 60, 140, 140));
            int chevStep = 55;
            int chevShift = (tickCount * 2) % chevStep;
            for (int cx = runwayStartX + chevShift; cx < runwayEndX - 10; cx += chevStep) {
                g2.drawString(">>>", cx, lineY - 50);
                g2.drawString(">>>", cx, lineY + 54);
            }

            // Center dashed conveyor track
            g2.setColor(new Color(130, 85, 195));
            Stroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{8f, 6f}, (float)(tickCount % 14));
            g2.setStroke(dashed);
            g2.drawLine(runwayStartX, lineY, cpuX - 8, lineY);

            // Draw Animated Dispatcher Control Station on Left
            int playerX = 14;
            int playerY = lineY - 26;
            float bob = (float)(Math.sin(tickCount * 0.25) * 3);

            // Dispatcher Console Pod
            g2.setColor(new Color(28, 20, 52));
            g2.fillRoundRect(playerX - 4, lineY - 48, 52, 96, 8, 8);
            g2.setColor(new Color(85, 55, 120));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(playerX - 4, lineY - 48, 52, 96, 8, 8);

            if (playerSprite != null) {
                g2.drawImage(playerSprite, playerX + 4, (int)(playerY + bob - 10), null);
            }
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("GATE", playerX + 8, lineY + 30);
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("AGENT", playerX + 6, lineY + 42);

            // 4. Render Alien Cards Evenly Spaced across the Runway
            clickableCards.clear();
            clickableJobs.clear();

            List<SimJob> displayList = new ArrayList<>(readyQueue);
            for (SimJob j : allJobs) {
                if (j.at > currentTime && !displayList.contains(j)) {
                    displayList.add(j);
                }
            }

            int cardCount = Math.min(displayList.size(), 7);
            int cardW = 76;
            int cardH = 96;
            int cardY = lineY - 48;

            // Compute dynamic spacing to fill the entire runway with zero awkward gaps
            int cardGap = 14;
            if (cardCount > 1) {
                int totalCardsW = cardCount * cardW;
                cardGap = Math.max(8, (runwayWidth - totalCardsW) / (cardCount + 1));
            }
            int qx = runwayStartX + cardGap;

            for (int i = 0; i < cardCount; i++) {
                SimJob qj = displayList.get(i);
                int avatarIdx = 0;
                try { avatarIdx = Integer.parseInt(qj.pid.replaceAll("\\D+", "")) - 1; } catch (Exception ignored) {}

                boolean isArrived = (qj.at <= currentTime);
                boolean isNextToRun = (!readyQueue.isEmpty() && readyQueue.get(0) == qj);

                // Card background with rounded glow
                Color cardBg = isArrived ? new Color(28, 38, 68) : new Color(24, 18, 42);
                Color cardBorder = isNextToRun ? RetroTheme.ACCENT_YELLOW : (isArrived ? RetroTheme.ACCENT_CYAN : new Color(75, 52, 110));

                g2.setColor(cardBg);
                g2.fillRoundRect(qx, cardY, cardW, cardH, 8, 8);
                g2.setColor(cardBorder);
                g2.setStroke(new BasicStroke(isNextToRun ? 2.5f : 1.2f));
                g2.drawRoundRect(qx, cardY, cardW, cardH, 8, 8);

                // Top Star / Queue Tag
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(isNextToRun ? RetroTheme.ACCENT_YELLOW : RetroTheme.TEXT_WHITE);
                g2.drawString(isNextToRun ? "★ NEXT" : (isArrived ? "READY" : "WAIT"), qx + 8, cardY + 11);

                // 32x32 Animated Alien Avatar with gentle bobbing
                BufferedImage icon = PixelSprites.getJobAvatar(Math.max(0, avatarIdx), 32);
                float aBob = isNextToRun ? (float)(Math.sin(tickCount * 0.3 + i) * 3) : 0;
                g2.drawImage(icon, qx + (cardW - 32) / 2, (int)(cardY + 16 + aBob), null);

                // PID Header (Large bold 13pt)
                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(qj.pid, qx + (cardW - fm.stringWidth(qj.pid)) / 2, cardY + 60);

                // Badges at bottom (Arrival @ or Burst)
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                if (!isArrived) {
                    g2.setColor(new Color(180, 180, 210));
                    String atStr = String.format("AT:%.1fs", qj.at);
                    g2.drawString(atStr, qx + (cardW - fm.stringWidth(atStr)) / 2, cardY + cardH - 18);
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    String btStr = String.format("BT:%.1fs", qj.bt);
                    g2.drawString(btStr, qx + (cardW - fm.stringWidth(btStr)) / 2, cardY + cardH - 6);
                } else {
                    g2.setColor(RetroTheme.ACCENT_CYAN);
                    String atStr = String.format("AT:%.1fs", qj.at);
                    g2.drawString(atStr, qx + (cardW - fm.stringWidth(atStr)) / 2, cardY + cardH - 18);
                    g2.setColor(RetroTheme.ACCENT_YELLOW);
                    String btStr = String.format("BT:%.2fs", qj.bt);
                    g2.drawString(btStr, qx + (cardW - fm.stringWidth(btStr)) / 2, cardY + cardH - 6);
                }

                if (isArrived) {
                    clickableCards.add(new Rectangle(qx, cardY, cardW, cardH));
                    clickableJobs.add(qj);
                }

                qx += cardW + cardGap;
            }

            // 5. THE MASSIVE CPU REACTOR CORE (230x175)
            boolean isCpuBusy = !activePid.equals("[IDLE]");
            Color coreBorder = isCpuBusy ? RetroTheme.ACCENT_PINK : new Color(85, 55, 120);

            if (isCpuBusy) {
                // Pulsing plasma glow around the chamber
                float pulse = (float)(Math.sin(tickCount * 0.4) * 0.5 + 0.5);
                g2.setColor(new Color(255, 42, 141, (int)(45 + pulse * 95)));
                g2.fillRoundRect(cpuX - 6, cpuY - 6, cpuW + 12, cpuH + 12, 14, 14);

                // Laser transfer beam into core
                g2.setColor(new Color(255, 211, 25, 200));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(cpuX - 16, lineY, cpuX, lineY);
            }

            g2.setColor(new Color(22, 15, 42));
            g2.fillRoundRect(cpuX, cpuY, cpuW, cpuH, 10, 10);
            g2.setColor(coreBorder);
            g2.setStroke(new BasicStroke(isCpuBusy ? 2.5f : 1.5f));
            g2.drawRoundRect(cpuX, cpuY, cpuW, cpuH, 10, 10);

            if (isCpuBusy) {
                int aIdx = 0;
                try { aIdx = Integer.parseInt(activePid.replaceAll("\\D+", "")) - 1; } catch (Exception ignored) {}
                BufferedImage icon = PixelSprites.getJobAvatar(Math.max(0, aIdx), 44);
                g2.drawImage(icon, cpuX + 16, cpuY + 40, null);

                g2.setFont(new Font("Consolas", Font.BOLD, 15));
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.drawString("CPU CORE // " + activePid, cpuX + 70, cpuY + 32);

                double rem = Math.max(0.0, activeEnd - currentTime);
                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                if (algoName.equals("ROUND ROBIN")) {
                    g2.drawString(String.format("QUANTUM: %.2fs", rem), cpuX + 70, cpuY + 54);
                } else {
                    g2.drawString(String.format("BURST: %.2fs", rem), cpuX + 70, cpuY + 54);
                }

                // Progress Bar inside the Core
                int barW = 145;
                double slotDur = Math.max(0.001, activeEnd - activeStart);
                double elapsed = Math.max(0.0, Math.min(slotDur, currentTime - activeStart));
                double progress = elapsed / slotDur;

                g2.setColor(new Color(15, 10, 26));
                g2.fillRoundRect(cpuX + 70, cpuY + 68, barW, 14, 4, 4);
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.fillRoundRect(cpuX + 70, cpuY + 68, (int)(barW * progress), 14, 4, 4);
                g2.setColor(Color.WHITE);
                g2.drawRoundRect(cpuX + 70, cpuY + 68, barW, 14, 4, 4);

                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(Color.WHITE);
                g2.drawString(String.format("TIME: %.2fs - %.2fs", activeStart, activeEnd), cpuX + 70, cpuY + 104);

                // Laser Clock frequency indicator
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(RetroTheme.ACCENT_GREEN);
                g2.drawString("● 3.8GHz CORE ACTIVE", cpuX + 70, cpuY + 128);

                g2.setColor(RetroTheme.TEXT_WHITE);
                g2.drawString("EXECUTION IN PROGRESS", cpuX + 70, cpuY + 146);
            } else {
                g2.setFont(new Font("Consolas", Font.BOLD, 17));
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.drawString("CPU REACTOR (1x)", cpuX + 32, cpuY + 46);

                g2.setFont(new Font("Consolas", Font.BOLD, 14));
                g2.setColor(Color.WHITE);
                g2.drawString("[ READY // IDLE ]", cpuX + 42, cpuY + 78);

                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(RetroTheme.TEXT_WHITE);
                g2.drawString("AWAITING JOB ARRIVAL", cpuX + 36, cpuY + 108);

                // Animated radar sweep line
                float radarX = (float)(Math.sin(tickCount * 0.1) * 45 + cpuX + cpuW / 2);
                g2.setColor(new Color(5, 217, 232, 120));
                g2.drawLine((int)radarX, cpuY + 24, (int)radarX, cpuY + cpuH - 24);

                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(RetroTheme.ACCENT_GREEN);
                g2.drawString("CORE FREQ: 3.80 GHz", cpuX + 40, cpuY + 142);
            }

            // 6. Subtitle Status Strip at Bottom of Arena
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(isCpuBusy ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_CYAN);
            g2.drawString(isCpuBusy ? "CPU BUSY:" : "CPU STATUS:", 16, h - 12);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            String status = isCpuBusy ? String.format("%s executing in CPU Core (Finishes slice at T=%.2fs)", activePid, activeEnd)
                    : (currentTime < 0.05 ? "SIMULATION PAUSED: Click ▶ START to begin execution." : "CPU Idle: Waiting for next arrival.");
            g2.drawString(status, 105, h - 12);

            // Completed Pods Mini Preview
            int cx = w - 240;
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString("COMPLETED DOCK:", cx - 100, h - 12);
            for (int i = 0; i < Math.min(completedList.size(), 6); i++) {
                int cIdx = 0;
                try { cIdx = Integer.parseInt(completedList.get(i).pid.replaceAll("\\D+", "")) - 1; } catch (Exception ignored) {}
                BufferedImage cIcon = PixelSprites.getJobAvatar(Math.max(0, cIdx), 16);
                g2.drawImage(cIcon, cx + i * 22, h - 22, null);
            }
        }
    }

    // =========================================================================
    // 4. GANTT RECORDER TIMELINE CANVAS
    // =========================================================================
    public static class GanttCanvas extends JPanel {
        private List<GanttSlot> slots = new ArrayList<>();
        private double currentTime = 0.0;
        private double maxTime = 0.0;
        private final Color[] PALETTE = {
                new Color(5, 217, 232),   // cyan
                new Color(170, 75, 255),  // purple
                new Color(255, 211, 25),  // yellow
                new Color(255, 42, 141),  // pink
                new Color(57, 255, 20),   // green
                new Color(255, 120, 40),  // orange
                new Color(80, 200, 255)   // sky
        };

        public GanttCanvas() {
            setBackground(new Color(12, 10, 24));
            setOpaque(true);
            setBorder(BorderFactory.createLineBorder(new Color(65, 45, 95), 1));
        }

        public void setSimulationState(List<GanttSlot> slots, double curr, double max) {
            this.slots = slots != null ? slots : new ArrayList<>();
            this.currentTime = curr;
            this.maxTime = max;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            if (slots.isEmpty() || maxTime <= 0.001) {
                g2.setColor(RetroTheme.TEXT_WHITE);
                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.drawString("GANTT RECORDER INITIALIZED: No execution data.", 20, h / 2 + 4);
                return;
            }

            int marginX = 20;
            int drawW = w - marginX * 2;
            int barY = 8;
            int barH = 32;

            // Draw Slots
            for (GanttSlot slot : slots) {
                int x1 = marginX + (int)((slot.start / maxTime) * drawW);
                int x2 = marginX + (int)((slot.end / maxTime) * drawW);
                int slotW = Math.max(2, x2 - x1);

                if (slot.label.equals("[IDLE]")) {
                    g2.setColor(new Color(28, 22, 45));
                    g2.fillRect(x1, barY, slotW, barH);
                    g2.setColor(new Color(55, 40, 80));
                    for (int hx = x1; hx < x2; hx += 8) g2.drawLine(hx, barY, hx + barH, barY + barH);
                    g2.setColor(RetroTheme.TEXT_WHITE);
                    g2.setFont(new Font("Consolas", Font.BOLD, 11));
                    if (slotW > 35) g2.drawString("IDLE", x1 + (slotW - 25) / 2, barY + 20);
                } else {
                    int pIdx = 0;
                    try { pIdx = Integer.parseInt(slot.label.replaceAll("\\D+", "")) - 1; } catch (Exception ignored) {}
                    Color c = PALETTE[Math.abs(pIdx) % PALETTE.length];
                    g2.setColor(c);
                    g2.fillRect(x1, barY, slotW, barH);

                    // Avatar icon inside slot if space permits
                    if (slotW >= 42) {
                        BufferedImage icon = PixelSprites.getJobAvatar(Math.max(0, pIdx), 16);
                        g2.drawImage(icon, x1 + 4, barY + (barH - 16) / 2, null);
                    }

                    g2.setColor(Color.BLACK);
                    g2.setFont(new Font("Consolas", Font.BOLD, 11));
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (slotW >= 42) ? x1 + 24 : x1 + (slotW - fm.stringWidth(slot.label)) / 2;
                    if (slotW > 16) g2.drawString(slot.label, tx, barY + 20);
                }

                g2.setColor(new Color(15, 10, 24));
                g2.drawRect(x1, barY, slotW, barH);

                // Timestamp labels below
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(RetroTheme.TEXT_WHITE);
                String tStr = String.format("%.1f", slot.start);
                g2.drawString(tStr, x1 - 6, barY + barH + 15);
            }

            // Final timestamp
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.TEXT_WHITE);
            g2.drawString(String.format("%.1f", maxTime), marginX + drawW - 8, barY + barH + 15);

            // Active Laser Cursor
            if (currentTime > 0.001) {
                int curX = marginX + (int)((Math.min(currentTime, maxTime) / maxTime) * drawW);
                g2.setColor(new Color(255, 42, 141, 140));
                g2.fillRect(curX - 2, barY - 2, 4, barH + 4);
                g2.setColor(Color.WHITE);
                g2.drawLine(curX, barY - 4, curX, barY + barH + 4);
                g2.fillOval(curX - 4, barY - 6, 8, 8);

                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.drawString(String.format("T=%.2fs", currentTime), Math.max(10, curX - 20), barY + barH + 15);
            }
        }
    }
}
