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
 * Screen 4: MISSION 01 - CPU SCHEDULING (ARCADE EDITION)
 * -----------------------------------------------------------------------------
 * 100% Mathematically-Accurate CPU Scheduling Simulator & Arcade Dispatch Arena.
 * Features:
 * - Real 3D Tactile Arcade Buttons (Solid fills, bevel highlights, press effects).
 * - Clean CPU Reactor Core (Zero annoying scanning lines).
 * - Enclosed, perfectly proportioned Left Cyber Console (Zero empty gaps, zero text clipping).
 * - Dynamic Maglev Runway with Alien cards spanning all the way to the CPU Reactor.
 * - Symmetrical 3-Pod Glassmorphic HUD with high-contrast 22pt key metrics.
 */
public class CPUSchedulingPanel extends JPanel {

    // Simulation & Policy State
    private String selectedAlgo = "FCFS";
    private double timeQuantum = 2.0;
    private double currentSimTime = 0.0;
    private double maxSimTime = 20.0;
    private boolean isPlaying = false;
    private double speedMultiplier = 0.5; // Calm, readable simulation speed by default
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
    private JLabel sub1;
    private JLabel sub2;
    private JLabel sub3;
    private JLabel lblGanttTitle;
    private JLabel lblIntelTitle;
    private JLabel lblIntelRule;
    private JLabel lblIntelAdv;
    private JPanel quantumCard;
    private JSpinner spinQuantum;
    private boolean isRecomputing = false;

    private JButton btnPlay;
    private JButton btnStep;
    private JButton btnReset;
    private JButton btnSpeed;
    private JButton btnModeToggle;
    private JButton[] algoButtons = new JButton[5];

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

        // 60 FPS live simulation timer (Smooth, calm pacing so the user can easily observe animations)
        simTimer = new javax.swing.Timer(30, e -> {
            if (isPlaying) {
                currentSimTime += 0.02 * speedMultiplier;
                if (currentSimTime >= maxSimTime) {
                    currentSimTime = maxSimTime;
                    pauseSimulation();
                    AppState.addScore(100);
                }
                updateLiveDisplay(currentSimTime);
            }
        });

        // 1. Left Column: Enclosed Cyber Console Deck (Width: 360px)
        JPanel leftCol = createLeftControlDeck();
        leftCol.setPreferredSize(new Dimension(360, 0));
        add(leftCol, BorderLayout.WEST);

        // 2. Right Column: Big Game Arena & Execution Timeline
        JPanel rightCol = createRightGameArena();
        add(rightCol, BorderLayout.CENTER);

        // 3. Global Keyboard Shortcuts (Space=Play/Pause, Right/S=Step, R=Reset, C=Compare)
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "keySpace");
        am.put("keySpace", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!isShowing()) return;
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof javax.swing.text.JTextComponent) return;
                toggleSimulationPlay();
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "keyStep");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "keyStep");
        am.put("keyStep", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!isShowing()) return;
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof javax.swing.text.JTextComponent) return;
                stepSimulationForward();
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "keyReset");
        am.put("keyReset", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!isShowing()) return;
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof javax.swing.text.JTextComponent) return;
                resetToBeginning();
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "keyCompare");
        am.put("keyCompare", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!isShowing()) return;
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof javax.swing.text.JTextComponent) return;
                openComparisonArena();
            }
        });

        // Automatically pause if screen is hidden or switched away
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (!isShowing() && isPlaying) {
                    pauseSimulation();
                }
            }
        });

        // Load Default Dataset & compute initial static preview at T=0.0 (Paused)
        loadDefaultMissionData();
        runSimulationMath();
        resetToBeginning();
    }

    // =========================================================================
    // 1. LEFT COLUMN: ENCLOSED CYBER CONTROL DECK (NO EMPTY GAPS, LARGE TEXT & BUTTONS)
    // =========================================================================
    private JPanel createLeftControlDeck() {
        JPanel deck = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 15, 38));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(75, 52, 110));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.fillRoundRect(0, 10, 4, 36, 2, 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        deck.setOpaque(false);
        deck.setLayout(new BoxLayout(deck, BoxLayout.Y_AXIS));
        deck.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Header Title
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(800, 38));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel iconBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(45, 25, 75));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(130, 70, 195));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBox.setPreferredSize(new Dimension(32, 32));
        iconBox.setOpaque(false);
        iconBox.setLayout(new GridBagLayout());
        JLabel icon = new JLabel(RetroIcons.createPlayIcon(16, RetroTheme.ACCENT_PINK));
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
        deck.add(header);

        deck.add(Box.createVerticalStrut(6));

        // 2. Algorithm Switcher (Tactile 3D Buttons - Larger Height 32px)
        JPanel algoCard = new JPanel(new BorderLayout(0, 3));
        algoCard.setOpaque(false);
        algoCard.setMaximumSize(new Dimension(800, 56));
        algoCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblAlgo = new JLabel("TURRET TARGETING POLICY // SCHEDULER");
        lblAlgo.setFont(new Font("Consolas", Font.BOLD, 10));
        lblAlgo.setForeground(RetroTheme.ACCENT_CYAN);
        algoCard.add(lblAlgo, BorderLayout.NORTH);

        JPanel algoGrid = new JPanel(new GridLayout(1, 5, 3, 0));
        algoGrid.setOpaque(false);
        String[] algos = {"FCFS", "SJF", "SRT", "RR", "PRIO"};
        for (int i = 0; i < algos.length; i++) {
            algoButtons[i] = createAlgoArcadeBtn(algos[i]);
            algoGrid.add(algoButtons[i]);
        }
        algoCard.add(algoGrid, BorderLayout.CENTER);
        deck.add(algoCard);

        deck.add(Box.createVerticalStrut(5));

        // 3. Time Quantum Row (Visible ONLY when Round Robin is selected)
        quantumCard = new JPanel(new BorderLayout(8, 0));
        quantumCard.setOpaque(true);
        quantumCard.setBackground(new Color(35, 26, 55));
        quantumCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_YELLOW, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        quantumCard.setMaximumSize(new Dimension(800, 30));
        quantumCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        quantumCard.setVisible(false);

        JLabel qLbl = new JLabel("TIME QUANTUM (Q):");
        qLbl.setFont(new Font("Consolas", Font.BOLD, 11));
        qLbl.setForeground(RetroTheme.ACCENT_YELLOW);
        quantumCard.add(qLbl, BorderLayout.WEST);

        spinQuantum = new JSpinner(new SpinnerNumberModel(2.0, 0.5, 10.0, 0.5));
        spinQuantum.setFont(new Font("Consolas", Font.BOLD, 12));
        spinQuantum.addChangeListener(e -> {
            timeQuantum = (Double) spinQuantum.getValue();
            recomputeAll();
        });
        quantumCard.add(spinQuantum, BorderLayout.EAST);
        deck.add(quantumCard);

        deck.add(Box.createVerticalStrut(5));

        // 4. Editable Process Queue Header + Explicit Add/Delete Actions
        JPanel queueHeader = new JPanel(new BorderLayout());
        queueHeader.setOpaque(false);
        queueHeader.setMaximumSize(new Dimension(800, 26));
        queueHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel qTitle = new JLabel("JOB QUEUE ");
        qTitle.setFont(new Font("Consolas", Font.BOLD, 12));
        qTitle.setForeground(Color.WHITE);
        queueHeader.add(qTitle, BorderLayout.WEST);

        JPanel qRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        qRight.setOpaque(false);

        JButton btnAddJob = createArcadeButton("ADD", new Color(0, 120, 160), Color.WHITE, 24);
        btnAddJob.setIcon(RetroIcons.createAddIcon(11, Color.WHITE));
        btnAddJob.setFont(new Font("Consolas", Font.BOLD, 10));
        btnAddJob.setToolTipText("Add new job (Max 12)");
        btnAddJob.addActionListener(e -> {
            if (inputModel.getRowCount() >= 12) {
                JOptionPane.showMessageDialog(this, "Queue capacity reached (Max 12 jobs).", "Capacity Limit", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int n = inputModel.getRowCount() + 1;
            if (selectedAlgo.equals("PRIO")) {
                inputModel.addRow(new Object[]{"J" + n, "0.0", "2.0", "1"});
            } else {
                inputModel.addRow(new Object[]{"J" + n, "0.0", "2.0"});
            }
            recomputeAll();
        });

        JButton btnRemoveJob = createArcadeButton("DEL", new Color(160, 25, 70), Color.WHITE, 24);
        btnRemoveJob.setIcon(RetroIcons.createDeleteIcon(11, Color.WHITE));
        btnRemoveJob.setFont(new Font("Consolas", Font.BOLD, 10));
        btnRemoveJob.setToolTipText("Delete selected job or last job in queue");
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

        JButton btnCsv = createArcadeButton("CSV", new Color(45, 30, 80), Color.WHITE, 24);
        btnCsv.setFont(new Font("Consolas", Font.BOLD, 10));
        btnCsv.setToolTipText("Import or Paste Workload CSV");
        btnCsv.addActionListener(e -> openCsvImportDialog());

        qRight.add(btnAddJob);
        qRight.add(btnRemoveJob);
        qRight.add(btnCsv);
        queueHeader.add(qRight, BorderLayout.EAST);
        deck.add(queueHeader);

        deck.add(Box.createVerticalStrut(4));

        // 5. Editable Jobs Table: 3 relevant columns (PRIO removed for FCFS/SJF/SRT/RR), with strict input validation
        String[] cols = {"JOB", "ARRIVAL (s)", "BURST (s)"};
        inputModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column > 0;
            }
        };

        inputTable = new JTable(inputModel);
        inputTable.setFont(new Font("Consolas", Font.BOLD, 12));
        inputTable.setRowHeight(24);
        inputTable.setBackground(new Color(16, 12, 32));
        inputTable.setForeground(Color.WHITE);
        inputTable.setGridColor(new Color(45, 32, 68));
        inputTable.setSelectionBackground(new Color(110, 40, 150));
        inputTable.setSelectionForeground(Color.WHITE);
        inputTable.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 11));
        inputTable.getTableHeader().setBackground(new Color(28, 20, 52));
        inputTable.getTableHeader().setForeground(RetroTheme.ACCENT_CYAN);

        inputTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        inputTable.getColumnModel().getColumn(1).setPreferredWidth(95);
        inputTable.getColumnModel().getColumn(2).setPreferredWidth(95);

        // Input validation: positive arrival time, positive non-zero burst time
        inputTable.getModel().addTableModelListener(e -> {
            if (isRecomputing) return;
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row >= 0 && col > 0) {
                try {
                    String val = String.valueOf(inputModel.getValueAt(row, col)).trim();
                    double d = Double.parseDouble(val);
                    if (col == 1 && d < 0.0) {
                        isRecomputing = true;
                        inputModel.setValueAt("0.0", row, col);
                        isRecomputing = false;
                    } else if (col == 2 && d < 0.1) {
                        isRecomputing = true;
                        inputModel.setValueAt("0.1", row, col);
                        isRecomputing = false;
                    }
                } catch (Exception ex) {
                    isRecomputing = true;
                    inputModel.setValueAt(col == 1 ? "0.0" : "1.0", row, col);
                    isRecomputing = false;
                }
            }
            recomputeAll();
        });

        scrollInput = RetroTheme.createRetroScroll(inputTable);
        scrollInput.setPreferredSize(new Dimension(315, 185));
        scrollInput.setMaximumSize(new Dimension(800, 195));
        scrollInput.setAlignmentX(Component.LEFT_ALIGNMENT);
        deck.add(scrollInput);

        deck.add(Box.createVerticalStrut(6));

        // 6. Algorithm Policy Intel Card (Clear 12pt readable text, 72px height)
        JPanel intelCard = new JPanel();
        intelCard.setLayout(new BoxLayout(intelCard, BoxLayout.Y_AXIS));
        intelCard.setOpaque(true);
        intelCard.setBackground(new Color(26, 18, 50));
        intelCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(75, 50, 110), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        intelCard.setMaximumSize(new Dimension(800, 72));
        intelCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblIntelTitle = new JLabel("POLICY: FIRST-COME FIRST-SERVED");
        lblIntelTitle.setFont(new Font("Consolas", Font.BOLD, 11));
        lblIntelTitle.setForeground(RetroTheme.ACCENT_CYAN);

        lblIntelRule = new JLabel("<html>• <b>Rule:</b> FIFO arrival order. Non-preemptive.</html>");
        lblIntelRule.setFont(new Font("Consolas", Font.BOLD, 12));
        lblIntelRule.setForeground(Color.WHITE);

        lblIntelAdv = new JLabel("<html>• <b>Warning:</b> Convoy effect on long bursts.</html>");
        lblIntelAdv.setFont(new Font("Consolas", Font.BOLD, 12));
        lblIntelAdv.setForeground(new Color(255, 230, 60));

        intelCard.add(lblIntelTitle);
        intelCard.add(Box.createVerticalStrut(2));
        intelCard.add(lblIntelRule);
        intelCard.add(Box.createVerticalStrut(2));
        intelCard.add(lblIntelAdv);
        deck.add(intelCard);

        deck.add(Box.createVerticalStrut(8));

        // 8. PRIMARY SIMULATION ACTION: UNCLUTTERED, DOMINANT 40px GREEN BUTTON
        btnPlay = createArcadeButton("START SIMULATION", new Color(0, 160, 70), Color.WHITE, 40);
        btnPlay.setIcon(RetroIcons.createPlayIcon(16, Color.WHITE));
        btnPlay.setFont(new Font("Consolas", Font.BOLD, 14));
        btnPlay.addActionListener(e -> toggleSimulationPlay());
        deck.add(btnPlay);

        deck.add(Box.createVerticalStrut(5));

        // 9. BENCHMARK COMPARISON ACTION: FULL-WIDTH, WELL-PROPORTIONED 34px CYAN BUTTON
        JButton btnCompare = createArcadeButton("COMPARE ALL POLICIES", new Color(0, 125, 155), Color.WHITE, 34);
        btnCompare.setIcon(RetroIcons.createCompareIcon(14, Color.WHITE));
        btnCompare.setFont(new Font("Consolas", Font.BOLD, 12));
        btnCompare.setToolTipText("Run FCFS, SJF, SRT, and RR side-by-side on this workload");
        btnCompare.addActionListener(e -> openComparisonArena());
        deck.add(btnCompare);

        deck.add(Box.createVerticalStrut(5));

        // 10. Secondary Controls Row: STEP, RESET, SPEED (Symmetrical 3 Columns)
        JPanel controlsRow = new JPanel(new GridLayout(1, 3, 5, 0));
        controlsRow.setOpaque(false);
        controlsRow.setMaximumSize(new Dimension(800, 32));
        controlsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnStep = createArcadeButton("STEP", new Color(28, 55, 105), Color.WHITE, 32);
        btnStep.setIcon(RetroIcons.createStepIcon(12, Color.WHITE));
        btnReset = createArcadeButton("RESET", new Color(150, 25, 75), Color.WHITE, 32);
        btnReset.setIcon(RetroIcons.createResetIcon(12, Color.WHITE));
        btnSpeed = createArcadeButton("0.5X SLOW", new Color(130, 90, 15), Color.WHITE, 32);

        btnStep.setFont(new Font("Consolas", Font.BOLD, 11));
        btnReset.setFont(new Font("Consolas", Font.BOLD, 11));
        btnSpeed.setFont(new Font("Consolas", Font.BOLD, 11));

        btnStep.addActionListener(e -> stepSimulationForward());
        btnReset.addActionListener(e -> resetToBeginning());
        btnSpeed.addActionListener(e -> {
            if (speedMultiplier == 0.5) {
                speedMultiplier = 1.0;
                btnSpeed.setText("1X NORMAL");
            } else if (speedMultiplier == 1.0) {
                speedMultiplier = 2.0;
                btnSpeed.setText("2X FAST");
            } else if (speedMultiplier == 2.0) {
                speedMultiplier = 0.25;
                btnSpeed.setText("0.25X SLOW");
            } else {
                speedMultiplier = 0.5;
                btnSpeed.setText("0.5X SLOW");
            }
        });

        controlsRow.add(btnStep);
        controlsRow.add(btnReset);
        controlsRow.add(btnSpeed);
        deck.add(controlsRow);

        deck.add(Box.createVerticalStrut(5));

        // 11. Interactive Dispatcher Mode Toggle (Clean 26px pill button)
        btnModeToggle = createArcadeButton("CHALLENGE MODE: OFF", new Color(38, 25, 68), Color.WHITE, 26);
        btnModeToggle.setFont(new Font("Consolas", Font.BOLD, 10));
        btnModeToggle.setToolTipText("Toggle Interactive Dispatcher Mode (Click ready alien in queue to dispatch)");
        btnModeToggle.addActionListener(e -> toggleChallengeMode());
        deck.add(btnModeToggle);

        deck.add(Box.createVerticalStrut(6));

        // 12. Hardware Telemetry Strip (Clean text, robust font)
        JPanel telemetryCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        telemetryCard.setOpaque(true);
        telemetryCard.setBackground(new Color(15, 11, 28));
        telemetryCard.setBorder(BorderFactory.createLineBorder(new Color(50, 35, 75), 1));
        telemetryCard.setMaximumSize(new Dimension(800, 26));
        telemetryCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel t1 = new JLabel("[*] 1x CORE ");
        t1.setFont(new Font("Consolas", Font.BOLD, 10));
        t1.setForeground(RetroTheme.ACCENT_GREEN);

        JLabel t2 = new JLabel("3.80GHz ");
        t2.setFont(new Font("Consolas", Font.BOLD, 10));
        t2.setForeground(RetroTheme.TEXT_WHITE);

        JLabel t3 = new JLabel("ONLINE ");
        t3.setFont(new Font("Consolas", Font.BOLD, 10));
        t3.setForeground(RetroTheme.ACCENT_CYAN);

        telemetryCard.add(t1);
        telemetryCard.add(t2);
        telemetryCard.add(t3);
        deck.add(telemetryCard);

        return deck;
    }

    // =========================================================================
    // 2. RIGHT COLUMN: BIG GAME ARENA + GANTT TIMELINE + METRICS
    // =========================================================================
    private JPanel createRightGameArena() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 4));

        // Top Status Strip & Top-Right Arcade Minigame Launcher
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(1400, 26));
        header.setMaximumSize(new Dimension(1400, 26));
        header.setMinimumSize(new Dimension(100, 26));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblMissionStatus = new JLabel("LIVE OUTPUT // DEFENSE TURRET & CREEP LANE");
        lblMissionStatus.setFont(new Font("Consolas", Font.BOLD, 13));
        lblMissionStatus.setForeground(Color.WHITE);
        lblMissionStatus.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        header.add(lblMissionStatus, BorderLayout.WEST);

        JButton btnPlayArcade = RetroTheme.createRetroButton("PLAY KERNEL TAPPER ARCADE", new Color(48, 20, 65), RetroTheme.ACCENT_YELLOW);
        btnPlayArcade.setFont(new Font("Consolas", Font.BOLD, 10));
        btnPlayArcade.setIcon(new ImageIcon(PixelSprites.createAlienSprite(14, RetroTheme.ACCENT_YELLOW)));
        btnPlayArcade.setIconTextGap(6);
        btnPlayArcade.setPreferredSize(new Dimension(205, 24));
        btnPlayArcade.setMaximumSize(new Dimension(205, 24));
        btnPlayArcade.setToolTipText("Launch 16-bit 3-lane retro arcade game (Control dispatcher with Arrow Keys & Spacebar!)");
        btnPlayArcade.addActionListener(e -> {
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            KernelTapperDialog dlg = new KernelTapperDialog(topFrame, selectedAlgo, new ArrayList<>(currentJobsList));
            dlg.setVisible(true);
        });
        header.add(btnPlayArcade, BorderLayout.EAST);
        p.add(header);

        p.add(Box.createVerticalStrut(4));

        // 3 High-Impact Glowing HUD Cards (Symmetrical, Zero empty gap, WCAG AA compliant)
        JPanel statsCard = new JPanel(new GridLayout(1, 3, 10, 0));
        statsCard.setOpaque(false);
        statsCard.setMaximumSize(new Dimension(1400, 72));
        statsCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Pod 1: Waiting Time
        JPanel p1 = createGlassHudPod("AVG WAIT TIME (WT)", RetroTheme.ACCENT_CYAN);
        lblAvgWt = new JLabel("0.00 sec");
        lblAvgWt.setFont(new Font("Consolas", Font.BOLD, 20));
        lblAvgWt.setForeground(RetroTheme.ACCENT_CYAN);
        sub1 = new JLabel("WT = TAT - BT ");
        sub1.setFont(new Font("Consolas", Font.BOLD, 10));
        sub1.setForeground(Color.WHITE);
        p1.add(lblAvgWt);
        p1.add(sub1);
        statsCard.add(p1);

        // Pod 2: Turnaround Time
        JPanel p2 = createGlassHudPod("AVG TURNAROUND (TAT)", RetroTheme.ACCENT_YELLOW);
        lblAvgTat = new JLabel("0.00 sec");
        lblAvgTat.setFont(new Font("Consolas", Font.BOLD, 20));
        lblAvgTat.setForeground(RetroTheme.ACCENT_YELLOW);
        sub2 = new JLabel("TAT = CT - AT ");
        sub2.setFont(new Font("Consolas", Font.BOLD, 10));
        sub2.setForeground(Color.WHITE);
        p2.add(lblAvgTat);
        p2.add(sub2);
        statsCard.add(p2);

        // Pod 3: Queue Clearance & CPU Load
        JPanel p3 = createGlassHudPod("LANE CLEARANCE", RetroTheme.ACCENT_GREEN);
        lblCpuClearance = new JLabel("100% CLEAR");
        lblCpuClearance.setFont(new Font("Consolas", Font.BOLD, 20));
        lblCpuClearance.setForeground(RetroTheme.ACCENT_GREEN);
        sub3 = new JLabel("7/7 JOBS // 3.80 GHz ");
        sub3.setFont(new Font("Consolas", Font.BOLD, 10));
        sub3.setForeground(Color.WHITE);
        p3.add(lblCpuClearance);
        p3.add(sub3);
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
        ganttBox.setMaximumSize(new Dimension(1400, 95));
        ganttBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel ganttHead = new JPanel(new BorderLayout());
        ganttHead.setOpaque(false);
        lblGanttTitle = new JLabel("GANTT TIMELINE & METRICS [PREVIEW]");
        lblGanttTitle.setFont(new Font("Consolas", Font.BOLD, 12));
        lblGanttTitle.setForeground(RetroTheme.ACCENT_CYAN);
        JLabel gr = new JLabel("LIVE RECORDER");
        gr.setFont(new Font("Consolas", Font.BOLD, 11));
        gr.setForeground(Color.WHITE);
        ganttHead.add(lblGanttTitle, BorderLayout.WEST);
        ganttHead.add(gr, BorderLayout.EAST);
        ganttBox.add(ganttHead, BorderLayout.NORTH);

        ganttCanvas = new GanttCanvas();
        ganttCanvas.setPreferredSize(new Dimension(880, 75));
        ganttCanvas.setMinimumSize(new Dimension(880, 75));
        ganttBox.add(ganttCanvas, BorderLayout.CENTER);
        p.add(ganttBox);

        p.add(Box.createVerticalStrut(6));

        // 5. Textbook Scheduling Results Table (Clean, Fits all 7 rows without cut-off)
        String[] rCols = {"JOB", "ARRIVAL (AT)", "BURST (BT)", "START (RT)", "FINISH (CT)", "WAIT (WT)", "TURN (TAT)"};
        ganttTableModel = new DefaultTableModel(rCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ganttTable = new JTable(ganttTableModel);
        ganttTable.setFont(new Font("Consolas", Font.BOLD, 12));
        ganttTable.setRowHeight(24);
        ganttTable.setBackground(new Color(16, 12, 30));
        ganttTable.setForeground(Color.WHITE);
        ganttTable.setGridColor(new Color(45, 32, 65));

        ganttTable.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBackground(new Color(32, 22, 58));
                l.setForeground(new Color(0, 240, 255));
                l.setFont(new Font("Consolas", Font.BOLD, 12));
                l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(55, 40, 80)));
                return l;
            }
        });

        DefaultTableCellRenderer customGanttRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                String jobVal = String.valueOf(table.getValueAt(row, 0));
                boolean isRunning = jobVal.contains("[RUN]");

                if (isSelected) {
                    c.setBackground(new Color(110, 45, 150));
                    c.setForeground(Color.WHITE);
                } else if (isRunning) {
                    c.setBackground(new Color(60, 22, 75));
                    c.setForeground(column == 0 ? RetroTheme.ACCENT_YELLOW : Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? new Color(18, 14, 34) : new Color(26, 19, 48));
                    c.setForeground(column == 0 ? RetroTheme.ACCENT_CYAN : Color.WHITE);
                }
                return c;
            }
        };
        for (int i = 0; i < ganttTable.getColumnCount(); i++) {
            ganttTable.getColumnModel().getColumn(i).setCellRenderer(customGanttRender);
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
        pod.setLayout(new GridLayout(3, 1, 0, 1));
        pod.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 8));

        JLabel l = new JLabel(title);
        l.setFont(new Font("Consolas", Font.BOLD, 9));
        l.setForeground(RetroTheme.TEXT_DIM);
        pod.add(l);
        return pod;
    }

    // =========================================================================
    // REAL TACTILE 3D ARCADE PUSH BUTTON HELPER (SOLID FILL, BEVEL, DROP SHADOW)
    // =========================================================================
    public static JButton createArcadeButton(String text, Color baseColor, Color textColor, int height) {
        JButton btn = new JButton(text) {
            private boolean isHovered = false;
            private boolean isPressed = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { isHovered = false; isPressed = false; repaint(); }
                    @Override public void mousePressed(MouseEvent e) { isPressed = true; repaint(); }
                    @Override public void mouseReleased(MouseEvent e) { isPressed = false; repaint(); }
                });
                addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) { repaint(); }
                    @Override public void focusLost(FocusEvent e) { repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                Color bg = baseColor;
                if (isPressed) {
                    bg = bg.darker();
                } else if (isHovered) {
                    bg = new Color(
                            Math.min(255, bg.getRed() + 35),
                            Math.min(255, bg.getGreen() + 35),
                            Math.min(255, bg.getBlue() + 35)
                    );
                }

                // 1. Bottom Drop Shadow (Gives 3D depth)
                g2.setColor(new Color(10, 6, 20));
                g2.fillRoundRect(0, 3, w, h - 3, 6, 6);

                // 2. Main Button Body (shifts 2px down when pressed)
                int yOff = isPressed ? 2 : 0;
                int bH = h - 3;
                g2.setColor(bg);
                g2.fillRoundRect(0, yOff, w, bH, 6, 6);

                // 3. Top Highlight Edge (Physical 3D bevel)
                g2.setColor(new Color(255, 255, 255, isPressed ? 25 : 65));
                g2.drawLine(3, yOff + 1, w - 4, yOff + 1);

                // 4. Bottom Shade Edge
                g2.setColor(new Color(0, 0, 0, 90));
                g2.drawLine(3, yOff + bH - 1, w - 4, yOff + bH - 1);

                // 5. Outer Bevel Border
                g2.setColor(new Color(255, 255, 255, 45));
                g2.drawRoundRect(0, yOff, w - 1, bH - 1, 6, 6);

                // Visible focus state (Accessibility requirement)
                if (hasFocus()) {
                    g2.setColor(RetroTheme.ACCENT_CYAN);
                    g2.setStroke(new BasicStroke(2.0f));
                    g2.drawRoundRect(1, yOff + 1, w - 3, bH - 3, 6, 6);
                }

                // 6. Icon and Text with Drop Shadow for Arcade Marquee Feel
                Icon icon = getIcon();
                String text = getText();
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();

                int iconW = (icon != null) ? icon.getIconWidth() : 0;
                int gap = (icon != null && text != null && !text.isEmpty()) ? getIconTextGap() : 0;
                int textW = (text != null && !text.isEmpty()) ? fm.stringWidth(text) : 0;
                int totalContentW = iconW + gap + textW;

                int startX = (w - totalContentW) / 2;

                if (icon != null) {
                    int iconY = yOff + (bH - icon.getIconHeight()) / 2;
                    icon.paintIcon(this, g2, startX, iconY);
                }

                if (text != null && !text.isEmpty()) {
                    int tx = startX + iconW + gap;
                    int ty = yOff + (bH - fm.getHeight()) / 2 + fm.getAscent();

                    g2.setColor(new Color(0, 0, 0, 180));
                    g2.drawString(text, tx + 1, ty + 1);

                    g2.setColor(textColor != null ? textColor : Color.WHITE);
                    g2.drawString(text, tx, ty);
                }

                g2.dispose();
            }
        };
        btn.setFont(new Font("Consolas", Font.BOLD, 10));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (height > 0) {
            btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, height));
            btn.setMaximumSize(new Dimension(800, height));
        }
        return btn;
    }

    private JButton createAlgoArcadeBtn(String algo) {
        JButton btn = new JButton(algo) {
            private boolean isHovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                boolean isSel = selectedAlgo.equals(algo) || (algo.equals("RR") && selectedAlgo.equals("ROUND ROBIN"));

                Color bg = isSel ? new Color(255, 42, 141) : (isHovered ? new Color(55, 38, 85) : new Color(38, 26, 62));

                // Drop shadow
                g2.setColor(new Color(10, 6, 20));
                g2.fillRoundRect(0, 3, w, h - 3, 6, 6);

                // Body
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h - 3, 6, 6);

                // Top highlight
                g2.setColor(new Color(255, 255, 255, isSel ? 90 : 45));
                g2.drawLine(3, 1, w - 4, 1);

                // Border
                g2.setColor(isSel ? Color.WHITE : new Color(85, 60, 120));
                g2.setStroke(new BasicStroke(isSel ? 1.8f : 1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 4, 6, 6);

                // Text
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = (h - 3 - fm.getHeight()) / 2 + fm.getAscent();

                if (isSel) {
                    g2.setColor(new Color(0, 0, 0, 140));
                    g2.drawString(getText(), tx + 1, ty + 1);
                }
                g2.setColor(Color.WHITE);
                g2.drawString(getText(), tx, ty);

                g2.dispose();
            }
        };
        btn.setFont(new Font("Consolas", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 32));
        btn.setMaximumSize(new Dimension(800, 32));
        btn.addActionListener(e -> {
            selectedAlgo = algo.equals("RR") ? "ROUND ROBIN" : algo;
            quantumCard.setVisible(selectedAlgo.equals("ROUND ROBIN"));
            updateColumnsForAlgo(selectedAlgo);
            lblMissionStatus.setText("LIVE OUTPUT // " + selectedAlgo + " READY QUEUE & CPU REACTOR");
            updatePolicyIntel(selectedAlgo);
            recomputeAll();
            for (JButton b : algoButtons) { if (b != null) b.repaint(); }
        });
        return btn;
    }

    private void updateColumnsForAlgo(String algo) {
        isRecomputing = true;
        List<SimJob> current = getJobsFromTable();
        if (algo.equals("PRIO")) {
            inputModel.setColumnIdentifiers(new String[]{"JOB", "ARRIVAL (s)", "BURST (s)", "PRIO"});
            inputModel.setRowCount(0);
            for (SimJob j : current) {
                inputModel.addRow(new Object[]{j.pid, String.format("%.1f", j.at), String.format("%.1f", j.bt), String.valueOf(j.prio > 0 ? j.prio : 1)});
            }
        } else {
            inputModel.setColumnIdentifiers(new String[]{"JOB", "ARRIVAL (s)", "BURST (s)"});
            inputModel.setRowCount(0);
            for (SimJob j : current) {
                inputModel.addRow(new Object[]{j.pid, String.format("%.1f", j.at), String.format("%.1f", j.bt)});
            }
        }
        isRecomputing = false;
    }

    public void openComparisonArena() {
        pauseSimulation();
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        List<SimJob> currentJobs = getJobsFromTable();
        CPUComparisonDialog dlg = new CPUComparisonDialog(topFrame, currentJobs, timeQuantum);
        dlg.setVisible(true);
    }

    private void openCsvImportDialog() {
        String sample = "J1, 0.0, 3.0\nJ2, 1.5, 2.0\nJ3, 3.0, 1.0\nJ4, 5.0, 4.0";
        JTextArea ta = new JTextArea(sample, 8, 25);
        ta.setFont(new Font("Consolas", Font.PLAIN, 12));
        ta.setBackground(new Color(18, 12, 32));
        ta.setForeground(Color.WHITE);
        ta.setCaretColor(RetroTheme.ACCENT_YELLOW);
        int opt = JOptionPane.showConfirmDialog(this, new JScrollPane(ta), "Paste Workload CSV (JOB, ARRIVAL, BURST)", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            String text = ta.getText().trim();
            if (!text.isEmpty()) {
                parseAndLoadCsv(text);
            }
        }
    }

    private void parseAndLoadCsv(String csv) {
        try {
            isRecomputing = true;
            inputModel.setRowCount(0);
            String[] lines = csv.split("\n");
            int jIdx = 1;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.toLowerCase().startsWith("job")) continue;
                String[] parts = line.split("[,\\t]+");
                if (parts.length >= 2) {
                    String pid;
                    double at, bt;
                    int prio = 1;
                    if (parts.length == 2) {
                        pid = "J" + jIdx;
                        at = Double.parseDouble(parts[0].trim());
                        bt = Double.parseDouble(parts[1].trim());
                    } else {
                        pid = parts[0].trim();
                        at = Double.parseDouble(parts[1].trim());
                        bt = Double.parseDouble(parts[2].trim());
                        if (parts.length >= 4) {
                            try { prio = Integer.parseInt(parts[3].trim()); } catch (Exception ignored) {}
                        }
                    }
                    if (selectedAlgo.equals("PRIO")) {
                        inputModel.addRow(new Object[]{pid, String.format("%.1f", Math.max(0, at)), String.format("%.1f", Math.max(0.1, bt)), String.valueOf(prio)});
                    } else {
                        inputModel.addRow(new Object[]{pid, String.format("%.1f", Math.max(0, at)), String.format("%.1f", Math.max(0.1, bt))});
                    }
                    jIdx++;
                    if (inputModel.getRowCount() >= 12) break;
                }
            }
            isRecomputing = false;
            recomputeAll();
        } catch (Exception ex) {
            isRecomputing = false;
            JOptionPane.showMessageDialog(this, "Failed to parse CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updatePolicyIntel(String algo) {
        switch (algo) {
            case "FCFS":
                lblIntelTitle.setText("TARGETING: FIRST (FCFS POLICY)");
                lblIntelRule.setText("<html>&gt; <b>Turret Target:</b> Frontmost creep in lane (FIFO order).<br>&nbsp;&nbsp;Non-preemptive beam lock.</html>");
                lblIntelAdv.setText("<html>&gt; <b>Hazard:</b> High-HP tank creeps block the lane (Convoy Effect).</html>");
                break;
            case "SJF":
                lblIntelTitle.setText("TARGETING: WEAKEST (SJF POLICY)");
                lblIntelRule.setText("<html>&gt; <b>Turret Target:</b> Lowest HP / Burst creep first.<br>&nbsp;&nbsp;Non-preemptive execution.</html>");
                lblIntelAdv.setText("<html>&gt; <b>Advantage:</b> Clears creeps rapidly, minimizing queue congestion.</html>");
                break;
            case "SRT":
                lblIntelTitle.setText("TARGETING: PREEMPTIVE WEAKEST (SRT)");
                lblIntelRule.setText("<html>&gt; <b>Turret Target:</b> Retargets if lower-HP creep appears.<br>&nbsp;&nbsp;Preemptive target switching.</html>");
                lblIntelAdv.setText("<html>&gt; <b>Advantage:</b> Fastest response; frequent beam retargeting.</html>");
                break;
            case "ROUND ROBIN":
                lblIntelTitle.setText("TARGETING: ROTATING VOLLEY (ROUND ROBIN)");
                lblIntelRule.setText("<html>&gt; <b>Turret Target:</b> Fires quantum Q volley then rotates.<br>&nbsp;&nbsp;Fair circular damage to all lane creeps.</html>");
                lblIntelAdv.setText("<html>&gt; <b>Feature:</b> Prevents starvation; unfinished creeps push to tail.</html>");
                break;
            case "PRIO":
                lblIntelTitle.setText("TARGETING: HIGH THREAT (PRIORITY)");
                lblIntelRule.setText("<html>&gt; <b>Turret Target:</b> Priority 1 boss creeps eliminated first.<br>&nbsp;&nbsp;Threat hierarchy defense.</html>");
                lblIntelAdv.setText("<html>&gt; <b>Note:</b> Standard minions wait until threats are eliminated.</html>");
                break;
        }
    }

    // =========================================================================
    // DATASET LOADERS (Clean 1-decimal rounding, intuitive to read and edit)
    // =========================================================================
    private void loadDefaultMissionData() {
        isRecomputing = true;
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"J1", "1.5", "1.3"});
        inputModel.addRow(new Object[]{"J2", "2.1", "0.6"});
        inputModel.addRow(new Object[]{"J3", "5.6", "3.2"});
        inputModel.addRow(new Object[]{"J4", "11.7", "2.0"});
        inputModel.addRow(new Object[]{"J5", "12.4", "1.5"});
        inputModel.addRow(new Object[]{"J6", "13.1", "0.7"});
        inputModel.addRow(new Object[]{"J7", "15.5", "0.9"});
        isRecomputing = false;
        recomputeAll();
    }

    private void loadConvoyDataset() {
        isRecomputing = true;
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"J1", "0.0", "10.0"});
        inputModel.addRow(new Object[]{"J2", "1.0", "1.0"});
        inputModel.addRow(new Object[]{"J3", "2.0", "1.0"});
        inputModel.addRow(new Object[]{"J4", "3.0", "1.0"});
        isRecomputing = false;
        recomputeAll();
    }

    private void loadSjfRushDataset() {
        isRecomputing = true;
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"J1", "0.0", "4.0"});
        inputModel.addRow(new Object[]{"J2", "1.0", "1.0"});
        inputModel.addRow(new Object[]{"J3", "1.5", "2.0"});
        inputModel.addRow(new Object[]{"J4", "2.0", "0.5"});
        inputModel.addRow(new Object[]{"J5", "3.0", "1.2"});
        isRecomputing = false;
        recomputeAll();
    }

    private void loadRoundRobinDataset() {
        isRecomputing = true;
        inputModel.setRowCount(0);
        inputModel.addRow(new Object[]{"J1", "0.0", "5.0"});
        inputModel.addRow(new Object[]{"J2", "1.0", "4.0"});
        inputModel.addRow(new Object[]{"J3", "2.0", "2.0"});
        inputModel.addRow(new Object[]{"J4", "3.0", "1.0"});
        isRecomputing = false;
        if (!selectedAlgo.equals("ROUND ROBIN")) {
            selectedAlgo = "ROUND ROBIN";
            quantumCard.setVisible(true);
            lblMissionStatus.setText("LIVE OUTPUT // ROUND ROBIN READY QUEUE & CPU REACTOR");
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
                int prio = inputModel.getColumnCount() > 3 ? Integer.parseInt(inputModel.getValueAt(i, 3).toString().trim()) : 1;
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

    public void addJobDirect(String pid, double at, double bt, int prio) {
        if (selectedAlgo.equals("PRIO")) {
            inputModel.addRow(new Object[]{pid, String.format("%.1f", at), String.format("%.1f", bt), String.valueOf(prio)});
        } else {
            inputModel.addRow(new Object[]{pid, String.format("%.1f", at), String.format("%.1f", bt)});
        }
        recomputeAll();
    }

    public void toggleSimulationPlay() {
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
        btnPlay.setText("PAUSE SIMULATION");
        btnPlay.setIcon(RetroIcons.createPauseIcon(16, Color.WHITE));
        simTimer.start();
    }

    public void toggleChallengeMode() {
        isPlayerChallengeMode = !isPlayerChallengeMode;
        btnModeToggle.setText(isPlayerChallengeMode ? "CHALLENGE MODE: ON" : "CHALLENGE MODE: OFF");
        btnModeToggle.setBackground(isPlayerChallengeMode ? new Color(180, 100, 20) : new Color(38, 25, 68));
        btnModeToggle.repaint();
        pauseSimulation();
        if (runwayCanvas != null) {
            runwayCanvas.setChallengeMode(isPlayerChallengeMode);
        }
    }

    public CPUReadyQueueCanvas getRunwayCanvas() {
        return runwayCanvas;
    }

    public void pauseSimulation() {
        isPlaying = false;
        btnPlay.setText("START SIMULATION");
        btnPlay.setIcon(RetroIcons.createPlayIcon(16, Color.WHITE));
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
        } else if (selectedAlgo.equals("PRIO")) {
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
                avail.sort(Comparator.comparingInt((SimJob j) -> j.prio).thenComparingDouble(j -> j.at));
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

    public void updateLiveDisplay(double time) {
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

        boolean isPreview = (time <= 0.001);
        if (lblGanttTitle != null) {
            lblGanttTitle.setText(isPreview ? "CPU EXECUTION TIMELINE & METRICS [THEORETICAL PREVIEW]"
                    : String.format("CPU EXECUTION TIMELINE & METRICS [LIVE MONITOR: T=%.2fs]", time));
        }

        // Update Metrics Table
        ganttTableModel.setRowCount(0);
        int finishedCount = 0;
        double liveWt = 0.0, liveTat = 0.0;

        for (SimJob j : fullResultsList) {
            boolean isFinished = (time >= j.ct && j.ct > 0.001);
            boolean isRunning = (activeSlot != null && activeSlot.label.equals(j.pid));
            if (isFinished) {
                finishedCount++;
                liveWt += j.wt;
                liveTat += j.tat;
            }

            String pidCol = j.pid + (isRunning ? " [RUN]" : "");
            String atCol = String.format("%.1f", j.at);
            String btCol = String.format("%.1f", j.bt);
            String rtCol = isPreview ? String.format("%.1f", j.rt) : ((time >= j.rt && j.rt >= 0) ? String.format("%.1f", j.rt) : "--");
            String ctCol = isPreview ? String.format("%.1f", j.ct) : (isFinished ? String.format("%.1f", j.ct) : "--");
            String wtCol = isPreview ? String.format("%.1f", j.wt) : (isFinished ? String.format("%.1f", j.wt) : (time >= j.at ? String.format("%.1f*", Math.max(0, time - j.at)) : "--"));
            String tatCol = isPreview ? String.format("%.1f", j.tat) : (isFinished ? String.format("%.1f", j.tat) : "--");

            ganttTableModel.addRow(new Object[]{pidCol, atCol, btCol, rtCol, ctCol, wtCol, tatCol});
        }

        // Ensure table shows row 0 without unintended vertical scroll
        if (scrollGantt != null && scrollGantt.getViewport() != null) {
            scrollGantt.getViewport().setViewPosition(new Point(0, 0));
        }

        int totalJobs = fullResultsList.size();
        int pct = totalJobs == 0 ? 0 : (finishedCount * 100 / totalJobs);
        lblCpuClearance.setText(pct + "% CLEAR (" + finishedCount + "/" + totalJobs + ")");
        lblCpuClearance.setForeground(pct == 100 ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_YELLOW);

        // Update HUD Pods: Clearly differentiate between theoretical preview and live execution
        double totalWt = 0.0, totalTat = 0.0;
        for (SimJob j : fullResultsList) { totalWt += j.wt; totalTat += j.tat; }
        int count = Math.max(1, fullResultsList.size());

        if (isPreview) {
            lblAvgWt.setText(String.format("~%.2f s", totalWt / count));
            if (sub1 != null) sub1.setText("PREDICTED [THEORETICAL]");
            lblAvgTat.setText(String.format("~%.2f s", totalTat / count));
            if (sub2 != null) sub2.setText("PREDICTED [THEORETICAL]");
            if (sub3 != null) sub3.setText("READY (0/" + totalJobs + " IN LANE)");
        } else {
            if (finishedCount > 0) {
                lblAvgWt.setText(String.format("%.2f s", liveWt / finishedCount));
                if (sub1 != null) sub1.setText(finishedCount + "/" + totalJobs + " DONE (AIM: ~" + String.format("%.2f", totalWt / count) + "s)");
                lblAvgTat.setText(String.format("%.2f s", liveTat / finishedCount));
                if (sub2 != null) sub2.setText(finishedCount + "/" + totalJobs + " DONE (AIM: ~" + String.format("%.2f", totalTat / count) + "s)");
            } else {
                lblAvgWt.setText(String.format("~%.2f s", totalWt / count));
                if (sub1 != null) sub1.setText("TARGET (0/" + totalJobs + " DONE)");
                lblAvgTat.setText(String.format("~%.2f s", totalTat / count));
                if (sub2 != null) sub2.setText("TARGET (0/" + totalJobs + " DONE)");
            }
            if (sub3 != null) sub3.setText(finishedCount + "/" + totalJobs + " JOBS // 3.80 GHz");
        }
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

        private int hoveredCardIdx = -1;
        private String clickedCardPid = null;
        private SimJob clickedJobRef = null;
        private int cardFlashTimer = 0;
        private Color cardFlashColor = RetroTheme.ACCENT_GREEN;
        private String cardFlashTag = "";
        private List<Point> clickParticles = new ArrayList<>();

        private BufferedImage playerSprite;
        private BufferedImage robotIdleSprite;

        public CPUReadyQueueCanvas(CPUSchedulingPanel parent) {
            this.parent = parent;
            setBackground(new Color(14, 10, 26));
            setOpaque(true);
            setBorder(BorderFactory.createLineBorder(new Color(65, 45, 95), 1));
            playerSprite = PixelSprites.createPlayerSprite(36);
            robotIdleSprite = PixelSprites.createRobotSprite(32);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!challengeMode) return;
                    for (int i = 0; i < clickableCards.size(); i++) {
                        Rectangle r = clickableCards.get(i);
                        if (r.contains(e.getPoint())) {
                            SimJob clicked = clickableJobs.get(i);
                            clickedJobRef = clicked;
                            clickedCardPid = clicked.pid;
                            cardFlashTimer = 14;

                            clickParticles.clear();
                            for (int p = 0; p < 8; p++) {
                                double angle = (p * Math.PI * 2) / 8.0;
                                int vx = (int)(Math.cos(angle) * 16);
                                int vy = (int)(Math.sin(angle) * 16);
                                clickParticles.add(new Point(vx, vy));
                            }

                            handlePlayerDispatchClick(clicked);
                            return;
                        }
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (!challengeMode) {
                        if (hoveredCardIdx != -1) {
                            hoveredCardIdx = -1;
                            setCursor(Cursor.getDefaultCursor());
                            repaint();
                        }
                        return;
                    }
                    int found = -1;
                    for (int i = 0; i < clickableCards.size(); i++) {
                        if (clickableCards.get(i).contains(e.getPoint())) {
                            found = i;
                            break;
                        }
                    }
                    if (found != hoveredCardIdx) {
                        hoveredCardIdx = found;
                        setCursor(found != -1 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                        repaint();
                    }
                }
            });

            new javax.swing.Timer(50, e -> {
                tickCount++;
                if (feedbackTimer > 0) {
                    feedbackTimer--;
                    if (feedbackTimer == 0) feedbackBanner = "";
                }
                if (cardFlashTimer > 0) {
                    cardFlashTimer--;
                    if (cardFlashTimer == 0) {
                        clickedCardPid = null;
                        clickedJobRef = null;
                    }
                }
                repaint();
            }).start();
        }

        public void setChallengeMode(boolean active) {
            this.challengeMode = active;
            if (active) {
                feedbackBanner = "[TURRET MANUAL OVERRIDE] Click the creep matching active policy to FIRE!";
                feedbackColor = RetroTheme.ACCENT_YELLOW;
                feedbackTimer = 100;
            } else {
                feedbackBanner = "";
                hoveredCardIdx = -1;
                clickedCardPid = null;
                cardFlashTimer = 0;
                setCursor(Cursor.getDefaultCursor());
            }
            repaint();
        }

        public void simulateCardClick(int cardIndex) {
            if (cardIndex >= 0 && cardIndex < clickableCards.size()) {
                Rectangle r = clickableCards.get(cardIndex);
                SimJob clicked = clickableJobs.get(cardIndex);
                clickedJobRef = clicked;
                clickedCardPid = clicked.pid;
                cardFlashTimer = 14;

                clickParticles.clear();
                for (int p = 0; p < 8; p++) {
                    double angle = (p * Math.PI * 2) / 8.0;
                    int vx = (int)(Math.cos(angle) * 16);
                    int vy = (int)(Math.sin(angle) * 16);
                    clickParticles.add(new Point(vx, vy));
                }

                handlePlayerDispatchClick(clicked);
                repaint();
            }
        }

        private void handlePlayerDispatchClick(SimJob clicked) {
            if (readyQueue.isEmpty()) {
                if (clicked.at > currentTime) {
                    feedbackBanner = "[LANE WAIT] " + clicked.pid + " has not entered the lane yet! Arrives at T=" + String.format("%.1fs", clicked.at) + " (Current T=" + String.format("%.1fs", currentTime) + ")";
                    feedbackColor = RetroTheme.ACCENT_YELLOW;
                    feedbackTimer = 75;
                    cardFlashColor = RetroTheme.ACCENT_YELLOW;
                    cardFlashTag = "LANE WAIT";
                } else {
                    feedbackBanner = "[LANE CLEAR] Ready lane empty. Click START or STEP >> to advance time!";
                    feedbackColor = RetroTheme.ACCENT_CYAN;
                    feedbackTimer = 65;
                    cardFlashColor = RetroTheme.ACCENT_CYAN;
                    cardFlashTag = "LANE EMPTY";
                }
                repaint();
                return;
            }

            SimJob expected = readyQueue.get(0);
            if (clicked == expected) {
                feedbackBanner = "[DIRECT HIT!] Target " + clicked.pid + " locked and engaged by Defense Turret! (+50 XP)";
                feedbackColor = RetroTheme.ACCENT_GREEN;
                feedbackTimer = 65;
                cardFlashColor = RetroTheme.ACCENT_GREEN;
                cardFlashTag = "DIRECT HIT! +50XP";
                AppState.addScore(50);
                
                // Allow player to see the bright green hit animation and ripple on the card!
                javax.swing.Timer advanceTimer = new javax.swing.Timer(280, evt -> {
                    parent.stepSimulationForward();
                });
                advanceTimer.setRepeats(false);
                advanceTimer.start();
            } else if (clicked.at > currentTime) {
                feedbackBanner = "[OUT OF RANGE] " + clicked.pid + " has not reached runway! Arrives at T=" + String.format("%.1fs", clicked.at);
                feedbackColor = RetroTheme.ACCENT_YELLOW;
                feedbackTimer = 75;
                cardFlashColor = RetroTheme.ACCENT_YELLOW;
                cardFlashTag = "NOT ARRIVED";
            } else {
                String policyRule = "";
                if (algoName.equals("FCFS")) {
                    policyRule = "FIRST (Frontmost creep " + expected.pid + " arrived earliest at AT=" + String.format("%.1fs", expected.at) + "s)";
                } else if (algoName.equals("SJF")) {
                    policyRule = "WEAKEST (Lowest HP creep " + expected.pid + " with HP=" + String.format("%.1fs", expected.bt) + "s)";
                } else if (algoName.equals("SRT")) {
                    policyRule = "PREEMPT (Lowest remaining HP creep " + expected.pid + " with HP=" + String.format("%.1fs", expected.bt) + "s)";
                } else if (algoName.equals("ROUND ROBIN")) {
                    policyRule = "ROTATING (Creep " + expected.pid + " is next in circular queue)";
                } else if (algoName.equals("PRIO")) {
                    policyRule = "THREAT (Creep " + expected.pid + " has highest threat priority " + expected.prio + ")";
                }
                feedbackBanner = "[TARGET MISMATCH] Turret targets: " + policyRule;
                feedbackColor = RetroTheme.ACCENT_PINK;
                feedbackTimer = 90;
                cardFlashColor = RetroTheme.ACCENT_PINK;
                cardFlashTag = "POLICY MISMATCH!";
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
            g2.setColor(new Color(0, 240, 255));
            g2.drawString("READY QUEUE RUNWAY (" + readyQueue.size() + " READY)", 16, 20);

            // High-contrast, framed center banner (No missing emoji glyphs)
            int bannerX = 230;
            int bannerY = 4;
            int bannerH = 22;
            if (challengeMode) {
                String msg;
                if (readyQueue.isEmpty()) {
                    msg = "[CHALLENGE ACTIVE] Waiting for arrivals. Click START or STEP >>";
                } else {
                    SimJob nextJob = readyQueue.get(0);
                    msg = "[CHALLENGE ACTIVE] Click " + nextJob.pid + " (>> DISPATCH) to run!";
                }
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                FontMetrics bfm = g2.getFontMetrics();
                int bW = bfm.stringWidth(msg) + 20;

                g2.setColor(new Color(60, 40, 10, 230));
                g2.fillRoundRect(bannerX, bannerY, bW, bannerH, 6, 6);
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(bannerX, bannerY, bW, bannerH, 6, 6);

                g2.setColor(new Color(255, 235, 70));
                g2.drawString(msg, bannerX + 10, bannerY + 15);
            } else {
                String msg = activePid.equals("[IDLE]") ? "[AUTO SIMULATION] Click START to run execution" : "[CPU EXECUTING] " + activePid;
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                FontMetrics bfm = g2.getFontMetrics();
                int bW = bfm.stringWidth(msg) + 20;

                g2.setColor(new Color(22, 16, 42, 220));
                g2.fillRoundRect(bannerX, bannerY, bW, bannerH, 6, 6);
                g2.setColor(new Color(85, 60, 130));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(bannerX, bannerY, bW, bannerH, 6, 6);

                g2.setColor(Color.WHITE);
                g2.drawString(msg, bannerX + 10, bannerY + 15);
            }

            if (feedbackTimer > 0 && !feedbackBanner.isEmpty()) {
                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                FontMetrics fmBanner = g2.getFontMetrics();
                int fbW = fmBanner.stringWidth(feedbackBanner) + 24;
                int fbX = Math.max(16, (w - fbW) / 2);
                int fbY = 28;

                g2.setColor(new Color(16, 12, 28, 240));
                g2.fillRoundRect(fbX, fbY, fbW, 22, 6, 6);
                g2.setColor(feedbackColor);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(fbX, fbY, fbW, 22, 6, 6);
                g2.drawString(feedbackBanner, fbX + 12, fbY + 16);
            }

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(new Color(0, 255, 136));
            g2.drawString("COMPLETED (" + completedList.size() + "/" + allJobs.size() + ")", w - 165, 20);

            // 3. Layout Geometry (Evenly Distributed Runway across full width)
            int cpuW = 248;
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
            if (clickedJobRef != null && cardFlashTimer > 0 && !displayList.contains(clickedJobRef)) {
                displayList.add(0, clickedJobRef);
            }
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
                boolean isClicked = (clickedCardPid != null && clickedCardPid.equals(qj.pid) && cardFlashTimer > 0);
                boolean isHovered = (challengeMode && hoveredCardIdx == i && !isClicked);

                // Tactile physical button depression and shake on error!
                int pressOffset = (isClicked && cardFlashTimer > 10) ? 2 : 0;
                int actualCardY = cardY + pressOffset;
                boolean isWrongClick = (isClicked && cardFlashColor == RetroTheme.ACCENT_PINK);
                int shakeX = isWrongClick ? (cardFlashTimer % 2 == 0 ? 3 : -3) : 0;
                int actualCardX = qx + shakeX;

                // Card background with rounded glow & click flash
                Color cardBg;
                Color cardBorder;
                if (isClicked) {
                    cardBg = new Color(cardFlashColor.getRed(), cardFlashColor.getGreen(), cardFlashColor.getBlue(), 130);
                    cardBorder = cardFlashColor;
                } else if (isHovered) {
                    cardBg = new Color(38, 52, 95);
                    cardBorder = new Color(0, 240, 255);
                } else {
                    cardBg = isArrived ? new Color(28, 38, 68) : new Color(24, 18, 42);
                    cardBorder = isNextToRun ? RetroTheme.ACCENT_YELLOW : (isArrived ? RetroTheme.ACCENT_CYAN : new Color(75, 52, 110));
                }

                g2.setColor(cardBg);
                g2.fillRoundRect(actualCardX, actualCardY, cardW, cardH, 8, 8);
                g2.setColor(cardBorder);
                g2.setStroke(new BasicStroke(isClicked ? 3.0f : (isHovered ? 2.5f : (isNextToRun ? 2.5f : 1.2f))));
                g2.drawRoundRect(actualCardX, actualCardY, cardW, cardH, 8, 8);

                // Click Shockwave Ripple Rings & Particle Burst
                if (isClicked) {
                    int ripple = (14 - cardFlashTimer) * 4;
                    int alpha = Math.max(0, Math.min(255, cardFlashTimer * 18));
                    g2.setColor(new Color(cardFlashColor.getRed(), cardFlashColor.getGreen(), cardFlashColor.getBlue(), alpha));
                    g2.setStroke(new BasicStroke(2.0f));
                    g2.drawRoundRect(actualCardX - ripple, actualCardY - ripple, cardW + ripple * 2, cardH + ripple * 2, 12, 12);

                    int cX = actualCardX + cardW / 2;
                    int cY = actualCardY + cardH / 2;
                    float progress = (14 - cardFlashTimer) / 14.0f;
                    g2.setColor(new Color(cardFlashColor.getRed(), cardFlashColor.getGreen(), cardFlashColor.getBlue(), Math.min(255, cardFlashTimer * 20)));
                    for (Point pt : clickParticles) {
                        int px = cX + (int)(pt.x * progress * 2.2);
                        int py = cY + (int)(pt.y * progress * 2.2);
                        g2.fillRect(px - 1, py - 1, 3, 3);
                    }
                }

                // Top Dispatch Queue Tag (Clean text, no broken emoji glyphs)
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                if (isNextToRun) {
                    float pulse = (float)(Math.sin(tickCount * 0.4) * 0.5 + 0.5);
                    Color glowNext = new Color(255, (int)(180 + pulse * 75), (int)(pulse * 80));
                    g2.setColor(glowNext);
                    String tag = challengeMode ? ">> FIRE" : "LOCK-ON";
                    g2.drawString(tag, actualCardX + (challengeMode ? 14 : 12), actualCardY + 13);
                } else {
                    g2.setColor(isArrived ? new Color(130, 240, 255) : new Color(180, 180, 210));
                    g2.drawString(isArrived ? "IN LANE" : "WAIT", actualCardX + (isArrived ? 14 : 22), actualCardY + 13);
                }

                // Floating status / click indicator pill right on top of the card
                if (isClicked) {
                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    FontMetrics tfm = g2.getFontMetrics();
                    int tagW = tfm.stringWidth(cardFlashTag) + 14;
                    int tagX = actualCardX + (cardW - tagW) / 2;
                    int tagY = actualCardY - 20;

                    g2.setColor(new Color(12, 8, 24, 245));
                    g2.fillRoundRect(tagX, tagY, tagW, 17, 6, 6);
                    g2.setColor(cardFlashColor);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(tagX, tagY, tagW, 17, 6, 6);
                    g2.drawString(cardFlashTag, tagX + 7, tagY + 12);
                } else if (isHovered) {
                    g2.setFont(new Font("Consolas", Font.BOLD, 9));
                    FontMetrics hfm = g2.getFontMetrics();
                    String hTag = "[ CLICK TO FIRE ]";
                    int tagW = hfm.stringWidth(hTag) + 10;
                    int tagX = actualCardX + (cardW - tagW) / 2;
                    int tagY = actualCardY - 16;

                    g2.setColor(new Color(15, 10, 30, 230));
                    g2.fillRoundRect(tagX, tagY, tagW, 14, 4, 4);
                    g2.setColor(new Color(0, 240, 255));
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(tagX, tagY, tagW, 14, 4, 4);
                    g2.drawString(hTag, tagX + 5, tagY + 11);
                }

                // 32x32 Animated Alien Avatar with gentle bobbing
                BufferedImage icon = PixelSprites.getJobAvatar(Math.max(0, avatarIdx), 32);
                float aBob = isNextToRun ? (float)(Math.sin(tickCount * 0.3 + i) * 3) : 0;
                g2.drawImage(icon, actualCardX + (cardW - 32) / 2, (int)(actualCardY + 17 + aBob), null);

                // PID Header (Large bold 12pt with drop shadow for crisp readability)
                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                FontMetrics pfm = g2.getFontMetrics();
                int pidX = actualCardX + (cardW - pfm.stringWidth(qj.pid)) / 2;
                int pidY = actualCardY + 59;
                g2.setColor(new Color(0, 0, 0, 190));
                g2.drawString(qj.pid, pidX + 1, pidY + 1);
                g2.setColor(Color.WHITE);
                g2.drawString(qj.pid, pidX, pidY);

                // SOLID HIGH-CONTRAST STATS BADGE & HP BAR AT BOTTOM OF CARD
                int badgeX = actualCardX + 4;
                int badgeY = actualCardY + 63;
                int badgeW = cardW - 8;
                int badgeH = 29;

                g2.setColor(new Color(10, 7, 22));
                g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);
                g2.setColor(isNextToRun ? RetroTheme.ACCENT_YELLOW : (isArrived ? new Color(70, 50, 110) : new Color(45, 30, 70)));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);

                // Creep Health Bar (representing Burst Time HP)
                int hpBarX = badgeX + 4;
                int hpBarY = badgeY + 3;
                int hpBarW = badgeW - 8;
                int hpBarH = 4;
                g2.setColor(new Color(25, 18, 38));
                g2.fillRect(hpBarX, hpBarY, hpBarW, hpBarH);

                float hpFraction = (float)Math.min(1.0, Math.max(0.12, qj.bt / 6.0));
                Color hpColor = qj.bt > 4.0 ? RetroTheme.ACCENT_PINK : (qj.bt > 2.0 ? RetroTheme.ACCENT_YELLOW : RetroTheme.ACCENT_GREEN);
                g2.setColor(hpColor);
                g2.fillRect(hpBarX, hpBarY, (int)(hpBarW * hpFraction), hpBarH);

                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                FontMetrics bfm = g2.getFontMetrics();

                // Arrival Time (AT)
                String atStr = String.format("AT:%.1fs", qj.at);
                g2.setColor(isArrived ? new Color(0, 240, 255) : new Color(175, 215, 255));
                g2.drawString(atStr, badgeX + (badgeW - bfm.stringWidth(atStr)) / 2, badgeY + 16);

                // Burst / Creep HP
                String hpStr = algoName.equals("PRIO") ? String.format("HP:%.1f P:%d", qj.bt, qj.prio) : String.format("HP:%.1fs", qj.bt);
                g2.setColor(new Color(255, 230, 60));
                g2.drawString(hpStr, badgeX + (badgeW - bfm.stringWidth(hpStr)) / 2, badgeY + 26);

                // Make all alien cards clickable so player receives direct guidance
                clickableCards.add(new Rectangle(actualCardX, actualCardY, cardW, cardH));
                clickableJobs.add(qj);

                qx += cardW + cardGap;
            }

            // 5. THE MASSIVE CPU REACTOR CORE (NO ANNOYING SCANNING VERTICAL LINE)
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

            // Corner Brackets for High-Tech Mainframe Feel
            g2.setColor(isCpuBusy ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_CYAN);
            g2.setStroke(new BasicStroke(2.0f));
            int bLen = 10;
            g2.drawLine(cpuX + 4, cpuY + 4, cpuX + 4 + bLen, cpuY + 4);
            g2.drawLine(cpuX + 4, cpuY + 4, cpuX + 4, cpuY + 4 + bLen);
            g2.drawLine(cpuX + cpuW - 4, cpuY + 4, cpuX + cpuW - 4 - bLen, cpuY + 4);
            g2.drawLine(cpuX + cpuW - 4, cpuY + 4, cpuX + cpuW - 4, cpuY + 4 + bLen);
            g2.drawLine(cpuX + 4, cpuY + cpuH - 4, cpuX + 4 + bLen, cpuY + cpuH - 4);
            g2.drawLine(cpuX + 4, cpuY + cpuH - 4, cpuX + 4, cpuY + cpuH - 4 - bLen);
            g2.drawLine(cpuX + cpuW - 4, cpuY + cpuH - 4, cpuX + cpuW - 4 - bLen, cpuY + cpuH - 4);
            g2.drawLine(cpuX + cpuW - 4, cpuY + cpuH - 4, cpuX + cpuW - 4, cpuY + cpuH - 4 - bLen);

            if (isCpuBusy) {
                int aIdx = 0;
                try { aIdx = Integer.parseInt(activePid.replaceAll("\\D+", "")) - 1; } catch (Exception ignored) {}
                BufferedImage icon = PixelSprites.getJobAvatar(Math.max(0, aIdx), 44);
                g2.drawImage(icon, cpuX + 16, cpuY + 40, null);

                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.drawString("DEFENSE TURRET", cpuX + 70, cpuY + 26);

                g2.setFont(new Font("Consolas", Font.BOLD, 13));
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawString("LOCK: " + activePid, cpuX + 70, cpuY + 42);

                double rem = Math.max(0.0, activeEnd - currentTime);
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(RetroTheme.ACCENT_CYAN);
                if (algoName.equals("ROUND ROBIN")) {
                    g2.drawString(String.format("VOLLEY REM: %.2fs", rem), cpuX + 70, cpuY + 58);
                } else {
                    g2.drawString(String.format("TARGET HP: %.2fs", rem), cpuX + 70, cpuY + 58);
                }

                // Progress Bar inside the Core
                int barW = 158;
                double slotDur = Math.max(0.001, activeEnd - activeStart);
                double elapsed = Math.max(0.0, Math.min(slotDur, currentTime - activeStart));
                double progress = elapsed / slotDur;

                g2.setColor(new Color(15, 10, 26));
                g2.fillRoundRect(cpuX + 70, cpuY + 68, barW, 13, 4, 4);
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.fillRoundRect(cpuX + 70, cpuY + 68, (int)(barW * progress), 13, 4, 4);
                g2.setColor(Color.WHITE);
                g2.drawRoundRect(cpuX + 70, cpuY + 68, barW, 13, 4, 4);

                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(Color.WHITE);
                g2.drawString(String.format("TIME: %.2fs - %.2fs", activeStart, activeEnd), cpuX + 70, cpuY + 98);

                // Laser Clock frequency indicator (WCAG AA 12px, clean painted dot)
                g2.setColor(RetroTheme.ACCENT_GREEN);
                g2.fillOval(cpuX + 70, cpuY + 115, 6, 6);
                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.drawString("TURRET ONLINE: 3.80 GHz", cpuX + 80, cpuY + 122);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.drawString("TARGET LOCK ENGAGED", cpuX + 70, cpuY + 142);
            } else {
                // CLEAN IDLE DISPLAY: Clean Microchip/Robot Sprite, NO annoying vertical sweep line!
                if (robotIdleSprite != null) {
                    g2.drawImage(robotIdleSprite, cpuX + (cpuW - 32) / 2, cpuY + 14, null);
                }

                g2.setFont(new Font("Consolas", Font.BOLD, 16));
                g2.setColor(RetroTheme.ACCENT_PINK);
                FontMetrics fm1 = g2.getFontMetrics();
                String t1 = "DEFENSE TURRET (1x)";
                g2.drawString(t1, cpuX + (cpuW - fm1.stringWidth(t1)) / 2, cpuY + 66);

                g2.setFont(new Font("Consolas", Font.BOLD, 13));
                g2.setColor(Color.WHITE);
                FontMetrics fm2 = g2.getFontMetrics();
                String t2 = "[ SCANNING LANE // IDLE ]";
                g2.drawString(t2, cpuX + (cpuW - fm2.stringWidth(t2)) / 2, cpuY + 94);

                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.setColor(RetroTheme.ACCENT_CYAN);
                FontMetrics fm3 = g2.getFontMetrics();
                String t3 = "AWAITING INCOMING CREEPS";
                g2.drawString(t3, cpuX + (cpuW - fm3.stringWidth(t3)) / 2, cpuY + 122);

                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.setColor(RetroTheme.ACCENT_GREEN);
                FontMetrics fm4 = g2.getFontMetrics();
                String t4 = "TURRET READY // 3.80 GHz";
                g2.drawString(t4, cpuX + (cpuW - fm4.stringWidth(t4)) / 2, cpuY + 152);
            }

            // 6. Subtitle Status Strip at Bottom of Arena (WCAG AA 12px bold)
            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(isCpuBusy ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_CYAN);
            g2.drawString(isCpuBusy ? "TURRET ACTIVE:" : "TURRET STATUS:", 16, h - 12);

            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(Color.WHITE);
            String status = isCpuBusy ? String.format("%s locked by Defense Turret (Engaged until T=%.2fs)", activePid, activeEnd)
                    : (currentTime < 0.05 ? "DEFENSE STANDBY: Click START SIMULATION to activate turret." : "Turret Idle: Scanning runway lane.");
            g2.drawString(status, 125, h - 12);

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
            setToolTipText("");
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            if (slots.isEmpty() || maxTime <= 0.001) return null;
            int marginX = 20;
            int drawW = getWidth() - marginX * 2;
            int x = e.getX(), y = e.getY();
            int barY = 6, barH = 34;
            if (y >= barY && y <= barY + barH && x >= marginX && x <= marginX + drawW) {
                double tHover = ((double)(x - marginX) / drawW) * maxTime;
                for (GanttSlot slot : slots) {
                    if (tHover >= slot.start && tHover <= slot.end) {
                        if (slot.label.equals("[IDLE]")) {
                            return String.format("CPU IDLE: %.1fs to %.1fs (%.1fs)", slot.start, slot.end, slot.end - slot.start);
                        } else {
                            return String.format("%s: Start=%.1fs, Finish=%.1fs, Burst=%.1fs", slot.label, slot.start, slot.end, slot.end - slot.start);
                        }
                    }
                }
            }
            return null;
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
            int barY = 6;
            int barH = 34;

            // 1. Subtle Background Vertical Gridlines across Time
            g2.setColor(new Color(38, 28, 58));
            g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3f, 3f}, 0.0f));
            int step = (maxTime <= 10) ? 1 : ((maxTime <= 25) ? 2 : 5);
            for (int t = 0; t <= (int)maxTime; t += step) {
                int gx = marginX + (int)((t / maxTime) * drawW);
                g2.drawLine(gx, barY, gx, barY + barH + 5);
            }
            g2.setStroke(new BasicStroke(1.0f));

            // 2. Draw Gantt Block Slots with Progressive Reveal
            for (GanttSlot slot : slots) {
                int x1 = marginX + (int)((slot.start / maxTime) * drawW);
                int x2 = marginX + (int)((slot.end / maxTime) * drawW);
                int slotW = Math.max(2, x2 - x1);

                boolean isPast = (slot.end <= currentTime + 0.001);
                boolean isRunning = (slot.start <= currentTime && currentTime < slot.end);
                boolean isFuture = (slot.start > currentTime);

                int pIdx = 0;
                try { pIdx = Integer.parseInt(slot.label.replaceAll("\\D+", "")) - 1; } catch (Exception ignored) {}
                Color c = slot.label.equals("[IDLE]") ? new Color(28, 22, 45) : PALETTE[Math.abs(pIdx) % PALETTE.length];

                if (currentTime <= 0.001) {
                    // PREVIEW MODE: Shaded with preview soft tone
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 175));
                    g2.fillRect(x1, barY, slotW, barH);
                } else if (isPast) {
                    // COMPLETED: Solid vibrant color
                    g2.setColor(c);
                    g2.fillRect(x1, barY, slotW, barH);
                } else if (isRunning) {
                    // CURRENTLY RUNNING: Solid up to currentTime, dimmed remainder
                    int runX2 = marginX + (int)((currentTime / maxTime) * drawW);
                    int ranW = Math.max(1, Math.min(slotW, runX2 - x1));
                    g2.setColor(c);
                    g2.fillRect(x1, barY, ranW, barH);
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
                    g2.fillRect(x1 + ranW, barY, slotW - ranW, barH);
                } else {
                    // FUTURE PREDICTED: Dimmed outline
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 45));
                    g2.fillRect(x1, barY, slotW, barH);
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 120));
                    g2.drawRect(x1, barY, slotW, barH);
                }

                if (slot.label.equals("[IDLE]")) {
                    g2.setColor(new Color(55, 40, 80));
                    for (int hx = x1; hx < x2; hx += 8) g2.drawLine(hx, barY, hx + barH, barY + barH);
                    g2.setColor(RetroTheme.TEXT_WHITE);
                    g2.setFont(new Font("Consolas", Font.BOLD, 11));
                    if (slotW > 35) g2.drawString("IDLE", x1 + (slotW - 25) / 2, barY + 22);
                } else {
                    if (isPast || isRunning || currentTime <= 0.001) {
                        if (slotW >= 42) {
                            BufferedImage icon = PixelSprites.getJobAvatar(Math.max(0, pIdx), 16);
                            g2.drawImage(icon, x1 + 4, barY + (barH - 16) / 2, null);
                        }
                        g2.setColor(Color.BLACK);
                        g2.setFont(new Font("Consolas", Font.BOLD, 12));
                        FontMetrics fm = g2.getFontMetrics();
                        int tx = (slotW >= 42) ? x1 + 24 : x1 + (slotW - fm.stringWidth(slot.label)) / 2;
                        if (slotW > 16) g2.drawString(slot.label, tx, barY + 22);
                    } else {
                        g2.setColor(new Color(200, 200, 220, 180));
                        g2.setFont(new Font("Consolas", Font.BOLD, 10));
                        if (slotW > 16) g2.drawString(slot.label, x1 + 4, barY + 22);
                    }
                }

                g2.setColor(new Color(15, 10, 24));
                g2.drawRect(x1, barY, slotW, barH);
            }

            // 3. Smart Non-Colliding Axis Boundary Ticks & Labels
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            FontMetrics tfm = g2.getFontMetrics();
            int lastLabelRight = -100;

            Set<Double> tickSet = new TreeSet<>();
            tickSet.add(0.0);
            for (GanttSlot s : slots) {
                tickSet.add(s.start);
                tickSet.add(s.end);
            }

            for (double tVal : tickSet) {
                int tx = marginX + (int)((tVal / maxTime) * drawW);
                g2.setColor(new Color(130, 95, 185));
                g2.drawLine(tx, barY + barH, tx, barY + barH + 5);

                String tStr = String.format("%.1f", tVal);
                int strW = tfm.stringWidth(tStr);
                int lx = tx - strW / 2;

                if (lx >= lastLabelRight + 8 && lx + strW <= w - 6) {
                    g2.setColor(Color.WHITE);
                    g2.drawString(tStr, lx, barY + barH + 18);
                    lastLabelRight = lx + strW;
                }
            }

            // 4. Active Laser Cursor (Live execution playhead)
            if (currentTime > 0.001) {
                int curX = marginX + (int)((Math.min(currentTime, maxTime) / maxTime) * drawW);
                g2.setColor(new Color(255, 42, 141, 150));
                g2.fillRect(curX - 2, barY - 2, 4, barH + 4);
                g2.setColor(Color.WHITE);
                g2.drawLine(curX, barY - 4, curX, barY + barH + 6);
                g2.fillOval(curX - 4, barY - 6, 8, 8);

                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(RetroTheme.ACCENT_CYAN);
                String curStr = String.format("T=%.2fs", currentTime);
                g2.drawString(curStr, Math.max(10, Math.min(w - 70, curX - 22)), barY + barH + 30);
            }
        }
    }
}
