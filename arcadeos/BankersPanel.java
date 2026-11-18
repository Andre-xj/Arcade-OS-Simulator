package arcadeos;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;

/**
 * Screen: MISSION 02 - BANKER'S ALGORITHM (CYBER VAULT CLEARINGHOUSE)
 * -----------------------------------------------------------------------------
 * 100% Mathematically-Accurate Operating System Banker's Algorithm Simulator.
 * Unique Game-Like Concept:
 * - The Central Cyber Vault: 3 visual physical reserve tanks (Gold, Data, Plasma).
 * - 5 Client Teller Booths: Distinct retro operatives holding collateral and requesting loans.
 * - Interactive Loan Clearance: Click eligible teller booths to disburse loans and reclaim collateral.
 * - The Official Banker's Audit Ledger: Complete textbook matrix table (Alloc, Max, Need, Work, Verdict).
 * - What-If Loan Request Simulator: Tests the textbook Resource-Request Algorithm.
 */
public class BankersPanel extends JPanel {

    // Algorithm & System State
    private int[] initialAvailable = {3, 3, 2};
    private int[] currentAvailable = {3, 3, 2};

    private boolean isPlaying = false;
    private int speedMultiplier = 1;
    private boolean isPlayerChallengeMode = false;
    private int currentStepIndex = 0;
    private boolean isTableUpdating = false;

    // Datasets
    private List<ProcessData> processList = new ArrayList<>();
    private List<SafetyStep> safetyTrace = new ArrayList<>();
    private List<String> safeSequence = new ArrayList<>();
    private boolean isSafeState = true;

    // UI Subsystems
    private javax.swing.Timer simTimer;
    private DefaultTableModel ledgerTableModel;
    private JTable ledgerTable;
    private JScrollPane scrollLedger;

    private JTextField txtAvailA;
    private JTextField txtAvailB;
    private JTextField txtAvailC;
    private JSpinner spinClientCount;

    private JLabel lblMissionStatus;
    private JLabel lblSafeSequence;
    private JLabel lblVaultStatus;
    private JLabel lblSolvencyRate;
    private JLabel lblClientCount;

    private JButton btnPlay;
    private JButton btnStep;
    private JButton btnReset;
    private JButton btnSpeed;
    private JButton btnModeToggle;
    private JButton btnLoanRequest;
    private JButton btnPlayArcade;
    private JButton btnAddClient;
    private JButton btnRemoveClient;

    private BankVaultArenaCanvas vaultArenaCanvas;

    public BankersPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));

        // Auto-play timer (steps forward every 900ms / speedMultiplier)
        simTimer = new javax.swing.Timer(900, e -> {
            if (isPlaying) {
                if (currentStepIndex < safetyTrace.size()) {
                    stepSimulationForward();
                } else {
                    pauseSimulation();
                    if (isSafeState) {
                        AppState.addScore(100);
                    }
                }
            }
        });

        // Top Control Deck & Status Strip
        add(createTopControlDeck(), BorderLayout.NORTH);

        // Center Arena & Ledger Split
        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setOpaque(false);

        // 1. The Central Vault & Teller Booths Game Arena (Height: 330px)
        vaultArenaCanvas = new BankVaultArenaCanvas(this);
        vaultArenaCanvas.setPreferredSize(new Dimension(1200, 330));
        vaultArenaCanvas.setMinimumSize(new Dimension(880, 310));
        centerPanel.add(vaultArenaCanvas, BorderLayout.CENTER);

        // 2. The Official Banker's Audit Ledger (Textbook Table & Inputs)
        JPanel bottomDeck = createAuditLedgerDeck();
        bottomDeck.setPreferredSize(new Dimension(1200, 240));
        centerPanel.add(bottomDeck, BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);

        // Load Classic Galvin 5P, 3R dataset
        loadClassicGalvinPreset();
    }

    // =========================================================================
    // DATA STRUCTURES
    // =========================================================================
    public static class ProcessData {
        public String pid;
        public int[] alloc;
        public int[] max;
        public int[] need;

        public ProcessData(String pid, int[] alloc, int[] max) {
            this.pid = pid;
            this.alloc = Arrays.copyOf(alloc, alloc.length);
            this.max = Arrays.copyOf(max, max.length);
            this.need = new int[alloc.length];
            for (int i = 0; i < alloc.length; i++) {
                this.need[i] = Math.max(0, max[i] - alloc[i]);
            }
        }
    }

    public static class SafetyStep {
        public String pid;
        public int[] workBefore;
        public int[] allocReleased;
        public int[] workAfter;
        public int[] need;
        public boolean passed;
        public String evalText;

        public SafetyStep(String pid, int[] wb, int[] ar, int[] wa, int[] nd, boolean passed, String eval) {
            this.pid = pid;
            this.workBefore = Arrays.copyOf(wb, wb.length);
            this.allocReleased = Arrays.copyOf(ar, ar.length);
            this.workAfter = Arrays.copyOf(wa, wa.length);
            this.need = Arrays.copyOf(nd, nd.length);
            this.passed = passed;
            this.evalText = eval;
        }
    }

    // =========================================================================
    // 1. TOP CONTROL DECK & STATUS BAR
    // =========================================================================
    private JPanel createTopControlDeck() {
        JPanel bar = new JPanel(new BorderLayout(10, 4));
        bar.setOpaque(false);

        // Row 1: Mission Header & Top-Right Header Arcade Controls
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JPanel leftTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftTitle.setOpaque(false);
        JLabel icon = new JLabel("🏦");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        leftTitle.add(icon);

        lblMissionStatus = new JLabel("MISSION 02: BANKER'S ALGORITHM // SPACE SHOOT VAULT");
        lblMissionStatus.setFont(new Font("Consolas", Font.BOLD, 13));
        lblMissionStatus.setForeground(Color.WHITE);
        leftTitle.add(lblMissionStatus);
        topHeader.add(leftTitle, BorderLayout.WEST);

        // East Header: Clean Status Text (No square border!) + Play Space Shoot Arcade Button
        JPanel eastHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        eastHeader.setOpaque(false);

        lblSolvencyRate = new JLabel(" [ 100% SOLVENT // SAFE SEQUENCE CONFIRMED ] ");
        lblSolvencyRate.setFont(new Font("Consolas", Font.BOLD, 11));
        lblSolvencyRate.setForeground(RetroTheme.ACCENT_GREEN);
        eastHeader.add(lblSolvencyRate);

        btnPlayArcade = RetroTheme.createRetroButton("PLAY SPACE SHOOT ARCADE", new Color(42, 16, 24), RetroTheme.ACCENT_YELLOW);
        btnPlayArcade.setFont(new Font("Consolas", Font.BOLD, 10));
        btnPlayArcade.setIcon(new ImageIcon(PixelSprites.createSpaceShootShip(14, RetroTheme.ACCENT_YELLOW)));
        btnPlayArcade.setIconTextGap(6);
        btnPlayArcade.setToolTipText("Launch playable Space Shoot arcade minigame!");
        btnPlayArcade.addActionListener(e -> {
            pauseSimulation();
            VaultPanicDialog dlg = new VaultPanicDialog(
                    (JFrame) SwingUtilities.getWindowAncestor(this),
                    this,
                    processList,
                    initialAvailable
            );
            dlg.setVisible(true);
        });
        eastHeader.add(btnPlayArcade);

        topHeader.add(eastHeader, BorderLayout.EAST);
        bar.add(topHeader, BorderLayout.NORTH);

        // Row 2: Controls & Preset Shortcuts
        JPanel controlsRow = new JPanel(new BorderLayout(8, 0));
        controlsRow.setOpaque(true);
        controlsRow.setBackground(new Color(22, 16, 42));
        controlsRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // Action Buttons on Left (Rich Arcade Buttons with Icons and Bevels)
        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftBtns.setOpaque(false);

        btnPlay = CPUSchedulingPanel.createArcadeButton("AUTO SOLVE", new Color(0, 150, 60), Color.WHITE, 28);
        btnPlay.setIcon(RetroIcons.createPlayIcon(11, Color.WHITE));
        btnPlay.setFont(new Font("Consolas", Font.BOLD, 10));
        btnPlay.addActionListener(e -> toggleSimulationPlay());

        btnStep = CPUSchedulingPanel.createArcadeButton("STEP >>", new Color(0, 110, 170), Color.WHITE, 28);
        btnStep.setIcon(RetroIcons.createStepIcon(11, Color.WHITE));
        btnStep.setFont(new Font("Consolas", Font.BOLD, 10));
        btnStep.addActionListener(e -> stepSimulationForward());

        btnReset = CPUSchedulingPanel.createArcadeButton("RESET", new Color(145, 28, 56), Color.WHITE, 28);
        btnReset.setIcon(RetroIcons.createResetIcon(11, Color.WHITE));
        btnReset.setFont(new Font("Consolas", Font.BOLD, 10));
        btnReset.addActionListener(e -> resetToBeginning());

        btnSpeed = CPUSchedulingPanel.createArcadeButton("SPEED: 1.0X", new Color(48, 34, 78), RetroTheme.ACCENT_YELLOW, 28);
        btnSpeed.setFont(new Font("Consolas", Font.BOLD, 10));
        btnSpeed.addActionListener(e -> {
            if (speedMultiplier == 1) speedMultiplier = 2;
            else if (speedMultiplier == 2) speedMultiplier = 4;
            else speedMultiplier = 1;
            btnSpeed.setText("SPEED: " + speedMultiplier + ".0X");
            simTimer.setDelay(900 / speedMultiplier);
        });

        btnModeToggle = CPUSchedulingPanel.createArcadeButton("CHALLENGE: OFF", new Color(38, 26, 68), RetroTheme.ACCENT_CYAN, 28);
        btnModeToggle.setFont(new Font("Consolas", Font.BOLD, 10));
        btnModeToggle.addActionListener(e -> {
            isPlayerChallengeMode = !isPlayerChallengeMode;
            btnModeToggle.setText(isPlayerChallengeMode ? "CHALLENGE: ON" : "CHALLENGE: OFF");
            btnModeToggle.setForeground(isPlayerChallengeMode ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_CYAN);
            pauseSimulation();
            vaultArenaCanvas.setChallengeMode(isPlayerChallengeMode);
        });

        btnLoanRequest = CPUSchedulingPanel.createArcadeButton("⚡ WHAT-IF LOAN", new Color(140, 100, 10), Color.WHITE, 28);
        btnLoanRequest.setFont(new Font("Consolas", Font.BOLD, 10));
        btnLoanRequest.addActionListener(e -> openResourceRequestDialog());

        leftBtns.add(btnPlay);
        leftBtns.add(btnStep);
        leftBtns.add(btnReset);
        leftBtns.add(btnSpeed);
        leftBtns.add(btnModeToggle);
        leftBtns.add(btnLoanRequest);

        controlsRow.add(leftBtns, BorderLayout.WEST);

        // Presets on Right
        JPanel rightPresets = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightPresets.setOpaque(false);
        JLabel lPres = new JLabel("SCENARIOS:");
        lPres.setFont(new Font("Consolas", Font.BOLD, 9));
        lPres.setForeground(RetroTheme.ACCENT_CYAN);
        rightPresets.add(lPres);

        JButton p1 = createMiniPresetBtn("GALVIN 5P", () -> loadClassicGalvinPreset());
        JButton p2 = createMiniPresetBtn("DEADLOCK", () -> loadDeadlockTrapPreset());
        JButton p3 = createMiniPresetBtn("STRESS 6P", () -> loadStressTestPreset());
        JButton p4 = createMiniPresetBtn("TIE-BREAK", () -> loadTieBreakerPreset());

        rightPresets.add(p1);
        rightPresets.add(p2);
        rightPresets.add(p3);
        rightPresets.add(p4);

        controlsRow.add(rightPresets, BorderLayout.EAST);
        bar.add(controlsRow, BorderLayout.SOUTH);

        return bar;
    }

    private JButton createMiniPresetBtn(String label, Runnable action) {
        JButton b = CPUSchedulingPanel.createArcadeButton(label, new Color(36, 26, 64), Color.WHITE, 26);
        b.setFont(new Font("Consolas", Font.BOLD, 9));
        b.addActionListener(e -> action.run());
        return b;
    }

    // =========================================================================
    // 2. THE OFFICIAL BANKER'S AUDIT LEDGER (TEXTBOOK MATRIX TABLE & INPUTS)
    // =========================================================================
    private JPanel createAuditLedgerDeck() {
        JPanel deck = new JPanel(new BorderLayout(0, 4));
        deck.setOpaque(false);

        // Header Strip: Live Safe Sequence + Table Scale Controls + Vault Input Values
        JPanel ledgerHeader = new JPanel(new BorderLayout(8, 0));
        ledgerHeader.setOpaque(true);
        ledgerHeader.setBackground(new Color(18, 14, 32));
        ledgerHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        // Left controls: Add/Remove clients, client count spinner
        JPanel leftSeq = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftSeq.setOpaque(false);

        btnAddClient = CPUSchedulingPanel.createArcadeButton("+ ADD CLIENT", new Color(0, 110, 150), Color.WHITE, 22);
        btnAddClient.setFont(new Font("Consolas", Font.BOLD, 9));
        btnAddClient.setToolTipText("Add a new client process row (unlimited tables)");
        btnAddClient.addActionListener(e -> addClientProcess());

        btnRemoveClient = CPUSchedulingPanel.createArcadeButton("- REMOVE", new Color(135, 28, 55), Color.WHITE, 22);
        btnRemoveClient.setFont(new Font("Consolas", Font.BOLD, 9));
        btnRemoveClient.setToolTipText("Remove selected or last client process row");
        btnRemoveClient.addActionListener(e -> removeClientProcess());

        JLabel lCount = new JLabel("CLIENTS:");
        lCount.setFont(new Font("Consolas", Font.BOLD, 10));
        lCount.setForeground(RetroTheme.ACCENT_YELLOW);

        // Custom Retro Stepper Panel: [ - ]  [ 5 ]  [ + ] (100% on-theme, eliminates ugly white box!)
        JButton btnDecClient = CPUSchedulingPanel.createArcadeButton("-", new Color(42, 28, 70), Color.WHITE, 22);
        btnDecClient.setFont(new Font("Consolas", Font.BOLD, 12));
        btnDecClient.setPreferredSize(new Dimension(26, 22));
        btnDecClient.setToolTipText("Remove client process (min 1)");
        btnDecClient.addActionListener(e -> removeClientProcess());

        lblClientCount = new JLabel(String.valueOf(processList.size() > 0 ? processList.size() : 5), SwingConstants.CENTER);
        lblClientCount.setFont(new Font("Consolas", Font.BOLD, 12));
        lblClientCount.setForeground(RetroTheme.ACCENT_YELLOW);
        lblClientCount.setOpaque(true);
        lblClientCount.setBackground(new Color(15, 10, 28));
        lblClientCount.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        lblClientCount.setPreferredSize(new Dimension(36, 22));

        JButton btnIncClient = CPUSchedulingPanel.createArcadeButton("+", new Color(0, 120, 160), Color.WHITE, 22);
        btnIncClient.setFont(new Font("Consolas", Font.BOLD, 12));
        btnIncClient.setPreferredSize(new Dimension(26, 22));
        btnIncClient.setToolTipText("Add client process");
        btnIncClient.addActionListener(e -> addClientProcess());

        // Internal spinner instance kept for backwards compatibility
        spinClientCount = new JSpinner(new SpinnerNumberModel(processList.size() > 0 ? processList.size() : 5, 1, 999, 1));
        spinClientCount.setVisible(false);

        leftSeq.add(btnAddClient);
        leftSeq.add(btnRemoveClient);
        leftSeq.add(Box.createHorizontalStrut(6));
        leftSeq.add(lCount);
        leftSeq.add(btnDecClient);
        leftSeq.add(lblClientCount);
        leftSeq.add(btnIncClient);
        ledgerHeader.add(leftSeq, BorderLayout.WEST);

        // Center: Live Safe Sequence Display (Spacious, Centered, Zero Collisions)
        JPanel centerSeq = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        centerSeq.setOpaque(false);
        JLabel lTag = new JLabel("SAFE SEQUENCE:");
        lTag.setFont(new Font("Consolas", Font.BOLD, 10));
        lTag.setForeground(RetroTheme.ACCENT_CYAN);

        lblSafeSequence = new JLabel("< NOT AUDITED >");
        lblSafeSequence.setFont(new Font("Consolas", Font.BOLD, 11));
        lblSafeSequence.setForeground(RetroTheme.ACCENT_YELLOW);

        centerSeq.add(lTag);
        centerSeq.add(lblSafeSequence);
        ledgerHeader.add(centerSeq, BorderLayout.CENTER);

        // Initial Available Input Fields on Right
        JPanel rightAvail = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightAvail.setOpaque(false);
        JLabel lAv = new JLabel("VAULT ASSETS:");
        lAv.setFont(new Font("Consolas", Font.BOLD, 10));
        lAv.setForeground(Color.WHITE);
        rightAvail.add(lAv);

        txtAvailA = createTokenField("3", RetroTheme.ACCENT_YELLOW);
        txtAvailB = createTokenField("3", RetroTheme.ACCENT_CYAN);
        txtAvailC = createTokenField("2", RetroTheme.ACCENT_PINK);

        rightAvail.add(createTokenBox("A:", txtAvailA, RetroTheme.ACCENT_YELLOW));
        rightAvail.add(createTokenBox("B:", txtAvailB, RetroTheme.ACCENT_CYAN));
        rightAvail.add(createTokenBox("C:", txtAvailC, RetroTheme.ACCENT_PINK));

        JButton btnApply = RetroTheme.createRetroButton("APPLY", RetroTheme.BTN_DARK, RetroTheme.ACCENT_GREEN);
        btnApply.setFont(new Font("Consolas", Font.BOLD, 9));
        btnApply.addActionListener(e -> recomputeSafety());
        rightAvail.add(btnApply);

        ledgerHeader.add(rightAvail, BorderLayout.EAST);
        deck.add(ledgerHeader, BorderLayout.NORTH);

        // Textbook Matrix Table (Complete Academic Columns)
        String[] cols = {
                "PID / CLIENT", "ALLOCATION (A B C) [EDIT]", "MAX CLAIM (A B C) [EDIT]",
                "NEED (Max - Alloc)", "WORK / VAULT (A B C)", "CAN SATISFY? (Need <= Work)", "CLEARANCE STATUS"
        };
        ledgerTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                // User can directly edit Allocation (column 1) and Max Claim (column 2)
                return c == 1 || c == 2;
            }
        };

        // Table Model Listener to capture user cell edits and recompute safety
        ledgerTableModel.addTableModelListener(e -> {
            if (isTableUpdating) return;
            int r = e.getFirstRow();
            int c = e.getColumn();
            if (r >= 0 && r < processList.size() && (c == 1 || c == 2)) {
                Object valObj = ledgerTableModel.getValueAt(r, c);
                if (valObj != null) {
                    int[] parsed = parseVector(valObj.toString());
                    if (parsed != null) {
                        ProcessData p = processList.get(r);
                        if (c == 1) {
                            p.alloc = parsed;
                        } else {
                            p.max = parsed;
                        }
                        for (int j = 0; j < 3; j++) {
                            p.need[j] = Math.max(0, p.max[j] - p.alloc[j]);
                        }
                        recomputeSafety();
                    }
                }
            }
        });

        ledgerTable = new JTable(ledgerTableModel);
        ledgerTable.setFont(new Font("Consolas", Font.BOLD, 11));
        ledgerTable.setRowHeight(26);
        ledgerTable.setBackground(new Color(16, 12, 30));
        ledgerTable.setForeground(Color.WHITE);
        ledgerTable.setSelectionBackground(new Color(48, 28, 76));
        ledgerTable.setSelectionForeground(RetroTheme.ACCENT_CYAN);
        ledgerTable.setGridColor(new Color(45, 32, 65));
        ledgerTable.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 11));
        ledgerTable.getTableHeader().setBackground(new Color(28, 20, 50));
        ledgerTable.getTableHeader().setForeground(RetroTheme.ACCENT_CYAN);
        ledgerTable.getTableHeader().setPreferredSize(new Dimension(0, 28));

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return c;
            }
        };
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < ledgerTable.getColumnCount(); i++) {
            ledgerTable.getColumnModel().getColumn(i).setCellRenderer(centerRender);
        }

        ledgerTable.getColumnModel().getColumn(0).setPreferredWidth(95);
        ledgerTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        ledgerTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        ledgerTable.getColumnModel().getColumn(3).setPreferredWidth(130);
        ledgerTable.getColumnModel().getColumn(4).setPreferredWidth(130);
        ledgerTable.getColumnModel().getColumn(5).setPreferredWidth(165);
        ledgerTable.getColumnModel().getColumn(6).setPreferredWidth(145);

        scrollLedger = RetroTheme.createRetroScroll(ledgerTable);
        scrollLedger.getViewport().setBackground(new Color(16, 12, 30));
        scrollLedger.setCorner(JScrollPane.LOWER_RIGHT_CORNER, new JPanel() {{ setBackground(new Color(16, 12, 30)); }});
        deck.add(scrollLedger, BorderLayout.CENTER);

        return deck;
    }

    private JPanel createTokenBox(String label, JTextField txt, Color accent) {
        JPanel p = new JPanel(new BorderLayout(2, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Consolas", Font.BOLD, 10));
        l.setForeground(accent);
        p.add(l, BorderLayout.WEST);
        p.add(txt, BorderLayout.CENTER);
        return p;
    }

    private JTextField createTokenField(String val, Color accent) {
        JTextField tf = new JTextField(val, 2);
        tf.setFont(new Font("Consolas", Font.BOLD, 11));
        tf.setBackground(new Color(14, 10, 26));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(accent);
        tf.setHorizontalAlignment(JTextField.CENTER);
        tf.setBorder(BorderFactory.createLineBorder(accent, 1));
        tf.addActionListener(e -> recomputeSafety());
        return tf;
    }

    // =========================================================================
    // PRESETS
    // =========================================================================
    private void updateClientCountDisplay() {
        int count = processList.size();
        if (lblClientCount != null) {
            lblClientCount.setText(String.valueOf(count));
        }
        if (spinClientCount != null) {
            spinClientCount.setValue(count);
        }
    }

    public void loadClassicGalvinPreset() {
        txtAvailA.setText("3");
        txtAvailB.setText("3");
        txtAvailC.setText("2");

        processList.clear();
        processList.add(new ProcessData("P0", new int[]{0, 1, 0}, new int[]{7, 5, 3}));
        processList.add(new ProcessData("P1", new int[]{2, 0, 0}, new int[]{3, 2, 2}));
        processList.add(new ProcessData("P2", new int[]{3, 0, 2}, new int[]{9, 0, 2}));
        processList.add(new ProcessData("P3", new int[]{2, 1, 1}, new int[]{2, 2, 2}));
        processList.add(new ProcessData("P4", new int[]{0, 0, 2}, new int[]{4, 3, 3}));

        updateClientCountDisplay();
        recomputeSafety();
    }

    public void loadDeadlockTrapPreset() {
        txtAvailA.setText("1");
        txtAvailB.setText("0");
        txtAvailC.setText("0");

        processList.clear();
        processList.add(new ProcessData("P0", new int[]{1, 0, 0}, new int[]{2, 1, 1}));
        processList.add(new ProcessData("P1", new int[]{0, 1, 1}, new int[]{1, 2, 2}));
        processList.add(new ProcessData("P2", new int[]{1, 1, 0}, new int[]{3, 2, 1}));
        processList.add(new ProcessData("P3", new int[]{0, 0, 1}, new int[]{2, 1, 2}));

        updateClientCountDisplay();
        recomputeSafety();
    }

    public void loadStressTestPreset() {
        txtAvailA.setText("2");
        txtAvailB.setText("1");
        txtAvailC.setText("1");

        processList.clear();
        processList.add(new ProcessData("P0", new int[]{1, 0, 1}, new int[]{4, 3, 2}));
        processList.add(new ProcessData("P1", new int[]{2, 1, 0}, new int[]{2, 2, 1}));
        processList.add(new ProcessData("P2", new int[]{3, 0, 2}, new int[]{9, 0, 2}));
        processList.add(new ProcessData("P3", new int[]{1, 1, 1}, new int[]{2, 2, 2}));
        processList.add(new ProcessData("P4", new int[]{0, 0, 2}, new int[]{4, 3, 3}));
        processList.add(new ProcessData("P5", new int[]{1, 2, 0}, new int[]{3, 4, 2}));

        updateClientCountDisplay();
        recomputeSafety();
    }

    public void loadTieBreakerPreset() {
        txtAvailA.setText("3");
        txtAvailB.setText("2");
        txtAvailC.setText("2");

        processList.clear();
        processList.add(new ProcessData("P0", new int[]{1, 0, 0}, new int[]{2, 1, 1}));
        processList.add(new ProcessData("P1", new int[]{1, 1, 0}, new int[]{2, 2, 1}));
        processList.add(new ProcessData("P2", new int[]{0, 1, 1}, new int[]{1, 2, 2}));
        processList.add(new ProcessData("P3", new int[]{0, 0, 1}, new int[]{1, 1, 2}));

        updateClientCountDisplay();
        recomputeSafety();
    }

    public void addClientProcess() {
        int nextId = processList.size();
        processList.add(new ProcessData("P" + nextId, new int[]{0, 0, 0}, new int[]{1, 1, 1}));
        updateClientCountDisplay();
        recomputeSafety();
    }

    public void removeClientProcess() {
        int sel = (ledgerTable != null) ? ledgerTable.getSelectedRow() : -1;
        if (sel >= 0 && processList.size() > 1) {
            processList.remove(sel);
        } else if (processList.size() > 1) {
            processList.remove(processList.size() - 1);
        }
        updateClientCountDisplay();
        recomputeSafety();
    }

    // =========================================================================
    // MATHEMATICAL SAFETY ALGORITHM
    // =========================================================================
    public void recomputeSafety() {
        pauseSimulation();
        readInitialAvailable();
        calculateSafetySequence();
        resetToBeginning();
    }

    private void readInitialAvailable() {
        try {
            initialAvailable[0] = Integer.parseInt(txtAvailA.getText().trim());
            initialAvailable[1] = Integer.parseInt(txtAvailB.getText().trim());
            initialAvailable[2] = Integer.parseInt(txtAvailC.getText().trim());
        } catch (Exception ex) {
            initialAvailable = new int[]{3, 3, 2};
        }
    }

    private void calculateSafetySequence() {
        safetyTrace.clear();
        safeSequence.clear();

        int n = processList.size();
        if (n == 0) {
            isSafeState = true;
            return;
        }

        int[] work = Arrays.copyOf(initialAvailable, 3);
        boolean[] finish = new boolean[n];

        boolean found;
        do {
            found = false;
            for (int i = 0; i < n; i++) {
                if (!finish[i]) {
                    ProcessData p = processList.get(i);
                    boolean canRun = true;
                    for (int j = 0; j < 3; j++) {
                        if (p.need[j] > work[j]) {
                            canRun = false;
                            break;
                        }
                    }

                    if (canRun) {
                        int[] workBefore = Arrays.copyOf(work, 3);
                        for (int j = 0; j < 3; j++) {
                            work[j] += p.alloc[j];
                        }
                        int[] workAfter = Arrays.copyOf(work, 3);
                        finish[i] = true;
                        safeSequence.add(p.pid);

                        String evalStr = String.format("[%s] <= [%s] (YES)",
                                vectorToStr(p.need), vectorToStr(workBefore));

                        safetyTrace.add(new SafetyStep(
                                p.pid, workBefore, p.alloc, workAfter, p.need, true, evalStr
                        ));
                        found = true;
                    }
                }
            }
        } while (found);

        isSafeState = (safeSequence.size() == n);
    }

    public String vectorToStr(int[] vec) {
        return vec[0] + " " + vec[1] + " " + vec[2];
    }

    private int[] parseVector(String s) {
        if (s == null) return null;
        String clean = s.replaceAll("[\\[\\],]", " ").trim();
        String[] parts = clean.split("\\s+");
        if (parts.length < 3) return null;
        try {
            return new int[]{
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
            };
        } catch (Exception ex) {
            return null;
        }
    }

    // =========================================================================
    // STEPPING & CONTROLS
    // =========================================================================
    private void toggleSimulationPlay() {
        if (isPlaying) {
            pauseSimulation();
        } else {
            startSimulation();
        }
    }

    private void startSimulation() {
        if (safetyTrace.isEmpty()) calculateSafetySequence();
        if (currentStepIndex >= safetyTrace.size()) {
            currentStepIndex = 0;
        }
        isPlaying = true;
        btnPlay.setText("PAUSE");
        btnPlay.setIcon(RetroIcons.createPauseIcon(11, Color.WHITE));
        btnPlay.setBackground(new Color(180, 110, 0));
        simTimer.start();
    }

    private void pauseSimulation() {
        isPlaying = false;
        btnPlay.setText("AUTO SOLVE");
        btnPlay.setIcon(RetroIcons.createPlayIcon(11, Color.WHITE));
        btnPlay.setBackground(new Color(0, 150, 60));
        simTimer.stop();
    }

    public void stepSimulationForward() {
        if (safetyTrace.isEmpty()) calculateSafetySequence();

        if (currentStepIndex < safetyTrace.size()) {
            currentStepIndex++;
        } else {
            pauseSimulation();
        }
        updateLiveDisplay();
    }

    private void resetToBeginning() {
        pauseSimulation();
        currentStepIndex = 0;
        updateLiveDisplay();
    }

    private void updateLiveDisplay() {
        if (currentStepIndex == 0) {
            currentAvailable = Arrays.copyOf(initialAvailable, 3);
        } else {
            SafetyStep lastCompleted = safetyTrace.get(currentStepIndex - 1);
            currentAvailable = Arrays.copyOf(lastCompleted.workAfter, 3);
        }

        Set<String> completedPids = new HashSet<>();
        for (int i = 0; i < currentStepIndex; i++) {
            completedPids.add(safetyTrace.get(i).pid);
        }

        SafetyStep activeStep = (currentStepIndex > 0 && currentStepIndex <= safetyTrace.size())
                ? safetyTrace.get(currentStepIndex - 1) : null;

        // 1. Update Safe Sequence Header
        if (currentStepIndex == 0) {
            lblSafeSequence.setText("< NOT AUDITED >");
            lblSafeSequence.setForeground(RetroTheme.TEXT_WHITE);
        } else {
            List<String> sub = new ArrayList<>();
            for (int i = 0; i < currentStepIndex; i++) {
                sub.add(safetyTrace.get(i).pid);
            }
            lblSafeSequence.setText("< " + String.join(" ➔ ", sub) + " >");
            lblSafeSequence.setForeground(RetroTheme.ACCENT_CYAN);
        }

        int total = processList.size();
        int completed = completedPids.size();
        if (currentStepIndex == 0) {
            lblSolvencyRate.setText(" [ AWAITING AUDIT // CLICK ▶ AUTO SOLVE OR ⏩ STEP ] ");
            lblSolvencyRate.setForeground(RetroTheme.ACCENT_CYAN);
        } else if (isSafeState && completed == total) {
            lblSolvencyRate.setText(" [ 100% SOLVENT // ALL LOANS SAFELY REPAID ✔ ] ");
            lblSolvencyRate.setForeground(RetroTheme.ACCENT_GREEN);
        } else if (!isSafeState && currentStepIndex == safetyTrace.size()) {
            lblSolvencyRate.setText(" [ ⚠️ BANK INSOLVENCY // DEADLOCK DETECTED ] ");
            lblSolvencyRate.setForeground(RetroTheme.ACCENT_PINK);
        } else {
            int pct = total == 0 ? 0 : (completed * 100 / total);
            lblSolvencyRate.setText(String.format(" [ CLEARING: %d%% (%d/%d LOANS SETTLED) ] ", pct, completed, total));
            lblSolvencyRate.setForeground(RetroTheme.ACCENT_YELLOW);
        }

        // 2. Update Vault Arena Canvas (pass currentStepIndex so it knows whether it's at start)
        vaultArenaCanvas.updateState(processList, currentAvailable, completedPids, activeStep, isSafeState, currentStepIndex);

        // 3. Update Audit Ledger Table
        isTableUpdating = true;
        ledgerTableModel.setRowCount(0);
        for (ProcessData p : processList) {
            boolean isCompleted = completedPids.contains(p.pid);
            boolean isRunning = activeStep != null && activeStep.pid.equals(p.pid);

            String needStr;
            String workStr;
            String evalStr;
            String statusStr;

            if (currentStepIndex == 0) {
                // INITIAL STATE: Identified columns MUST NOT have any answers in them yet!
                needStr = "-";
                workStr = "-";
                evalStr = "-";
                statusStr = "-";
            } else if (isCompleted) {
                needStr = vectorToStr(p.need);
                workStr = "-";
                evalStr = "-";
                for (SafetyStep s : safetyTrace) {
                    if (s.pid.equals(p.pid)) {
                        workStr = vectorToStr(s.workBefore);
                        evalStr = s.evalText;
                        break;
                    }
                }
                statusStr = "✔ REPAID & RETURNED";
            } else if (isRunning) {
                needStr = vectorToStr(p.need);
                workStr = vectorToStr(currentAvailable);
                evalStr = String.format("[%s] <= [%s] (PASS ✔)", vectorToStr(p.need), vectorToStr(currentAvailable));
                statusStr = "⚡ DISBURSING LOAN";
            } else {
                needStr = "-";
                workStr = "-";
                evalStr = "-";
                statusStr = "WAITING...";
            }

            ledgerTableModel.addRow(new Object[]{
                    p.pid + (isRunning ? " [RUN]" : (isCompleted ? " [DONE]" : "")),
                    vectorToStr(p.alloc),
                    vectorToStr(p.max),
                    needStr,
                    workStr,
                    evalStr,
                    statusStr
            });
        }
        isTableUpdating = false;
    }

    // =========================================================================
    // WHAT-IF RESOURCE REQUEST MODAL
    // =========================================================================
    private void openResourceRequestDialog() {
        pauseSimulation();

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "⚡ WHAT-IF CLIENT LOAN REQUEST SIMULATOR", true);
        dlg.setSize(480, 360);
        dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(new Color(18, 12, 34));
        dlg.setLayout(new BorderLayout(10, 10));

        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel("TEST CLIENT LOAN-REQUEST ALGORITHM");
        title.setFont(new Font("Consolas", Font.BOLD, 13));
        title.setForeground(RetroTheme.ACCENT_CYAN);
        p.add(title);

        JLabel sub = new JLabel("Evaluate if an immediate client loan request can be safely granted right now.");
        sub.setFont(new Font("Consolas", Font.PLAIN, 10));
        sub.setForeground(RetroTheme.TEXT_WHITE);
        p.add(sub);

        p.add(Box.createVerticalStrut(12));

        // Client Process combo
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1.setOpaque(false);
        JLabel lPid = new JLabel("BORROWER / CLIENT:");
        lPid.setFont(new Font("Consolas", Font.BOLD, 11));
        lPid.setForeground(RetroTheme.ACCENT_YELLOW);
        JComboBox<String> comboPid = new JComboBox<>();
        for (ProcessData pr : processList) comboPid.addItem(pr.pid);
        comboPid.setFont(new Font("Consolas", Font.BOLD, 11));
        row1.add(lPid);
        row1.add(comboPid);
        p.add(row1);

        p.add(Box.createVerticalStrut(10));

        // Request Vector Inputs (A B C)
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.setOpaque(false);
        JLabel lReq = new JLabel("LOAN REQUEST (A B C):");
        lReq.setFont(new Font("Consolas", Font.BOLD, 11));
        lReq.setForeground(Color.WHITE);
        JTextField tfA = createTokenField("1", RetroTheme.ACCENT_YELLOW);
        JTextField tfB = createTokenField("0", RetroTheme.ACCENT_CYAN);
        JTextField tfC = createTokenField("2", RetroTheme.ACCENT_PINK);
        row2.add(lReq);
        row2.add(createTokenBox("A:", tfA, RetroTheme.ACCENT_YELLOW));
        row2.add(createTokenBox("B:", tfB, RetroTheme.ACCENT_CYAN));
        row2.add(createTokenBox("C:", tfC, RetroTheme.ACCENT_PINK));
        p.add(row2);

        p.add(Box.createVerticalStrut(12));

        // Output Result Area
        JTextArea txtResult = new JTextArea(5, 36);
        txtResult.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtResult.setBackground(new Color(12, 8, 24));
        txtResult.setForeground(Color.WHITE);
        txtResult.setBorder(BorderFactory.createLineBorder(new Color(65, 45, 95)));
        txtResult.setEditable(false);
        txtResult.setText("Click [AUDIT SAFETY] below to simulate banker approval decision...");
        p.add(txtResult);

        dlg.add(p, BorderLayout.CENTER);

        // Buttons
        JPanel bRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        bRow.setOpaque(false);

        JButton btnTest = RetroTheme.createRetroButton("AUDIT SAFETY", RetroTheme.BTN_DARK, RetroTheme.ACCENT_YELLOW);
        btnTest.setFont(new Font("Consolas", Font.BOLD, 11));
        btnTest.addActionListener(e -> {
            try {
                int pIdx = comboPid.getSelectedIndex();
                if (pIdx < 0 || pIdx >= processList.size()) return;
                ProcessData target = processList.get(pIdx);

                int[] req = new int[]{
                        Integer.parseInt(tfA.getText().trim()),
                        Integer.parseInt(tfB.getText().trim()),
                        Integer.parseInt(tfC.getText().trim())
                };

                // Rule 1: Request <= Need
                for (int j = 0; j < 3; j++) {
                    if (req[j] > target.need[j]) {
                        txtResult.setText(String.format(
                                "❌ LOAN REJECTED:\nClient %s exceeded their maximum credit limit!\nRequest: [%s] > Need: [%s]",
                                target.pid, vectorToStr(req), vectorToStr(target.need)
                        ));
                        txtResult.setForeground(RetroTheme.ACCENT_PINK);
                        return;
                    }
                }

                // Rule 2: Request <= Available
                for (int j = 0; j < 3; j++) {
                    if (req[j] > initialAvailable[j]) {
                        txtResult.setText(String.format(
                                "⏳ LOAN DEFERRED / MUST WAIT:\nVault lacks sufficient assets to disburse right now!\nRequest: [%s] > Available: [%s]",
                                vectorToStr(req), vectorToStr(initialAvailable)
                        ));
                        txtResult.setForeground(RetroTheme.ACCENT_YELLOW);
                        return;
                    }
                }

                // Rule 3: Pretend allocation and run Safety Algorithm
                int[] testAvail = Arrays.copyOf(initialAvailable, 3);
                for (int j = 0; j < 3; j++) testAvail[j] -= req[j];

                List<ProcessData> testProcesses = new ArrayList<>();
                for (int i = 0; i < processList.size(); i++) {
                    ProcessData orig = processList.get(i);
                    int[] a = Arrays.copyOf(orig.alloc, 3);
                    int[] m = Arrays.copyOf(orig.max, 3);
                    if (i == pIdx) {
                        for (int j = 0; j < 3; j++) a[j] += req[j];
                    }
                    testProcesses.add(new ProcessData(orig.pid, a, m));
                }

                int n = testProcesses.size();
                int[] work = Arrays.copyOf(testAvail, 3);
                boolean[] finish = new boolean[n];
                List<String> testSeq = new ArrayList<>();
                boolean found;
                do {
                    found = false;
                    for (int i = 0; i < n; i++) {
                        if (!finish[i]) {
                            ProcessData tp = testProcesses.get(i);
                            boolean ok = true;
                            for (int j = 0; j < 3; j++) {
                                if (tp.need[j] > work[j]) { ok = false; break; }
                            }
                            if (ok) {
                                for (int j = 0; j < 3; j++) work[j] += tp.alloc[j];
                                finish[i] = true;
                                testSeq.add(tp.pid);
                                found = true;
                            }
                        }
                    }
                } while (found);

                if (testSeq.size() == n) {
                    txtResult.setText(String.format(
                            "✔ LOAN SAFELY APPROVED!\nBank remains in a solvent SAFE STATE.\nResulting Safe Sequence: < %s >",
                            String.join(" -> ", testSeq)
                    ));
                    txtResult.setForeground(RetroTheme.ACCENT_GREEN);
                } else {
                    txtResult.setText(
                            "⚠️ LOAN DENIED TO PREVENT DEADLOCK!\nDisbursing this loan would cause Bank Insolvency!"
                    );
                    txtResult.setForeground(RetroTheme.ACCENT_PINK);
                }
            } catch (Exception ex) {
                txtResult.setText("Invalid input: " + ex.getMessage());
            }
        });

        JButton btnClose = RetroTheme.createRetroButton("CLOSE", RetroTheme.BTN_DARK, RetroTheme.TEXT_WHITE);
        btnClose.setFont(new Font("Consolas", Font.BOLD, 10));
        btnClose.addActionListener(e -> dlg.dispose());

        bRow.add(btnTest);
        bRow.add(btnClose);
        dlg.add(bRow, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    // =========================================================================
    // 3. THE GRAND CYBER VAULT & TELLER BOOTHS GAME ARENA
    // =========================================================================
    public static class BankVaultArenaCanvas extends JPanel {
        private final BankersPanel parentPanel;

        private List<ProcessData> processes = new ArrayList<>();
        private int[] available = {3, 3, 2};
        private Set<String> completedPids = new HashSet<>();
        private SafetyStep activeStep = null;
        private boolean isSafeState = true;
        private int stepIndex = 0;

        private int tickCount = 0;
        private boolean challengeMode = false;
        private List<Rectangle> clickableBoothCards = new ArrayList<>();
        private List<ProcessData> clickableProcesses = new ArrayList<>();

        private int scrollX = 0;
        private int maxScrollX = 0;
        private Rectangle btnPrevRect = new Rectangle();
        private Rectangle btnNextRect = new Rectangle();

        private String feedbackBanner = "";
        private Color feedbackColor = RetroTheme.ACCENT_CYAN;
        private int feedbackTimer = 0;

        public BankVaultArenaCanvas(BankersPanel parentPanel) {
            this.parentPanel = parentPanel;
            setBackground(new Color(13, 9, 24));
            setOpaque(true);
            setBorder(BorderFactory.createLineBorder(new Color(65, 45, 95), 1));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (btnPrevRect.contains(e.getPoint())) {
                        scrollX = Math.max(0, scrollX - 175);
                        repaint();
                        return;
                    }
                    if (btnNextRect.contains(e.getPoint())) {
                        scrollX = Math.min(maxScrollX, scrollX + 175);
                        repaint();
                        return;
                    }
                    for (int i = 0; i < clickableBoothCards.size(); i++) {
                        if (clickableBoothCards.get(i).contains(e.getPoint())) {
                            ProcessData clicked = clickableProcesses.get(i);
                            handlePlayerBoothClick(clicked);
                            return;
                        }
                    }
                }
            });

            addMouseWheelListener(e -> {
                if (maxScrollX > 0) {
                    scrollX = Math.max(0, Math.min(maxScrollX, scrollX + e.getWheelRotation() * 50));
                    repaint();
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
                feedbackBanner = "INTERACTIVE MODE: Click an eligible client booth whose Need <= Vault to disburse!";
                feedbackColor = RetroTheme.ACCENT_YELLOW;
                feedbackTimer = 90;
            } else {
                feedbackBanner = "";
            }
            repaint();
        }

        private void handlePlayerBoothClick(ProcessData clicked) {
            if (completedPids.contains(clicked.pid)) {
                feedbackBanner = "ALREADY SETTLED: " + clicked.pid + " has already repaid its loan and returned collateral!";
                feedbackColor = RetroTheme.TEXT_WHITE;
                feedbackTimer = 50;
                repaint();
                return;
            }

            boolean canFund = true;
            for (int j = 0; j < 3; j++) {
                if (clicked.need[j] > available[j]) {
                    canFund = false;
                    break;
                }
            }

            if (canFund) {
                feedbackBanner = "PERFECT LOAN DISBURSEMENT! " + clicked.pid + " repaid in full! (+50 XP)";
                feedbackColor = RetroTheme.ACCENT_GREEN;
                feedbackTimer = 60;
                AppState.addScore(50);
                parentPanel.stepSimulationForward();
            } else {
                feedbackBanner = String.format("DEADLOCK VIOLATION! %s Needs [%s] but Vault only holds [%s]!",
                        clicked.pid, parentPanel.vectorToStr(clicked.need), parentPanel.vectorToStr(available));
                feedbackColor = RetroTheme.ACCENT_PINK;
                feedbackTimer = 70;
            }
            repaint();
        }

        public void updateState(List<ProcessData> procs, int[] avail, Set<String> completed,
                                SafetyStep step, boolean isSafe, int stepIdx) {
            this.processes = procs != null ? procs : new ArrayList<>();
            this.available = avail != null ? Arrays.copyOf(avail, 3) : new int[]{0, 0, 0};
            this.completedPids = completed != null ? completed : new HashSet<>();
            this.activeStep = step;
            this.isSafeState = isSafe;
            this.stepIndex = stepIdx;
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

            // 1. Cyber Security Floor Grid
            g2.setColor(new Color(15, 11, 28));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(28, 20, 48));
            for (int gx = 0; gx < w; gx += 32) g2.drawLine(gx, 0, gx, h);
            for (int gy = 0; gy < h; gy += 32) g2.drawLine(0, gy, w, gy);

            // =================================================================
            // 2. THE GRAND CENTRAL VAULT (CENTER-TOP STAGE)
            // =================================================================
            int vaultW = 540;
            int vaultH = 100;
            int vaultX = (w - vaultW) / 2;
            int vaultY = 12;

            boolean isTransferring = (activeStep != null);
            Color vBorderCol = isTransferring ? RetroTheme.ACCENT_YELLOW : (isSafeState ? RetroTheme.ACCENT_CYAN : RetroTheme.ACCENT_PINK);

            // Ambient glow around Vault
            if (isTransferring) {
                float pulse = (float)(Math.sin(tickCount * 0.4) * 0.5 + 0.5);
                g2.setColor(new Color(255, 211, 25, (int)(25 + pulse * 65)));
                g2.fillRoundRect(vaultX - 6, vaultY - 6, vaultW + 12, vaultH + 12, 16, 16);
            }

            // Heavy Vault Door Shell
            g2.setColor(new Color(22, 16, 42));
            g2.fillRoundRect(vaultX, vaultY, vaultW, vaultH, 12, 12);
            g2.setColor(vBorderCol);
            g2.setStroke(new BasicStroke(isTransferring ? 2.5f : 1.5f));
            g2.drawRoundRect(vaultX, vaultY, vaultW, vaultH, 12, 12);

            // Vault Header Tag
            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("🔒 THE GRAND CENTRAL CYBER VAULT", vaultX + 16, vaultY + 22);

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString(isSafeState ? "● SYSTEM SOLVENT // AVAILABLE RESERVES" : "● DEADLOCK HAZARD ACTIVE", vaultX + 260, vaultY + 22);

            // Three Physical Resource Deposit Chests / Vault Drawers (A, B, C)
            int chestW = 150;
            int chestH = 58;
            int chestY = vaultY + 32;
            int chestStartX = vaultX + 20;

            String[] resNames = {"🪙 CORE A (GOLD)", "💎 CORE B (DATA)", "⚡ CORE C (PLASMA)"};
            Color[] resColors = {RetroTheme.ACCENT_YELLOW, RetroTheme.ACCENT_CYAN, RetroTheme.ACCENT_PINK};

            for (int r = 0; r < 3; r++) {
                int cx = chestStartX + r * (chestW + 24);

                // Drawer background
                g2.setColor(new Color(14, 10, 26));
                g2.fillRoundRect(cx, chestY, chestW, chestH, 8, 8);
                g2.setColor(resColors[r]);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(cx, chestY, chestW, chestH, 8, 8);

                // Token Count Large Display
                g2.setFont(new Font("Consolas", Font.BOLD, 22));
                g2.setColor(Color.WHITE);
                String valStr = String.valueOf(available[r]);
                g2.drawString(valStr, cx + 16, chestY + 34);

                // Token bar gauge
                int barMaxW = 60;
                float frac = Math.min(1.0f, available[r] / 12.0f);
                int barFillW = (int)(barMaxW * frac);
                g2.setColor(new Color(35, 25, 60));
                g2.fillRoundRect(cx + 70, chestY + 18, barMaxW, 14, 3, 3);
                g2.setColor(resColors[r]);
                g2.fillRoundRect(cx + 70, chestY + 18, barFillW, 14, 3, 3);

                // Subtitle
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(resColors[r]);
                g2.drawString(resNames[r], cx + 12, chestY + 50);
            }

            // =================================================================
            // 3. THE CLIENT TELLER BOOTHS (PROCESS SUITES) - RESPONSIVE SCALING
            // =================================================================
            clickableBoothCards.clear();
            clickableProcesses.clear();

            int boothCount = Math.max(1, processes.size());
            int boothH = 152;
            int boothY = 142;

            // Responsive width & gap calculation ensuring unlimited tables (1 to 100+) fit seamlessly
            int availW = w - 48;
            int boothGap = 12;
            int maxCardW = (boothCount <= 3) ? 200 : ((boothCount == 4) ? 185 : 168);
            int boothW = Math.min(maxCardW, (availW - (boothCount - 1) * boothGap) / boothCount);

            if (boothW < 160) {
                // Unlimited tables mode: keep cards beautifully sized (165px) with smooth horizontal scroll
                boothW = 165;
                boothGap = 10;
                int totalBoothsW = boothCount * boothW + (boothCount - 1) * boothGap;
                maxScrollX = Math.max(0, totalBoothsW - (w - 72));
            } else {
                maxScrollX = 0;
                scrollX = 0;
            }

            int totalBoothsW = boothCount * boothW + (boothCount - 1) * boothGap;
            int startX = (maxScrollX > 0) ? (36 - scrollX) : Math.max(16, (w - totalBoothsW) / 2);

            // Auto-scroll to active step if executing
            if (maxScrollX > 0 && activeStep != null) {
                for (int k = 0; k < boothCount; k++) {
                    if (processes.get(k).pid.equals(activeStep.pid)) {
                        int cardCenterX = 36 + k * (boothW + boothGap) + boothW / 2;
                        scrollX = Math.max(0, Math.min(maxScrollX, cardCenterX - w / 2));
                        startX = 36 - scrollX;
                        break;
                    }
                }
            }

            int activeBoothCenterX = -1;

            for (int i = 0; i < boothCount; i++) {
                ProcessData p = processes.get(i);
                int bx = startX + i * (boothW + boothGap);

                boolean isSettled = completedPids.contains(p.pid);
                boolean isCurrentFunding = (activeStep != null && activeStep.pid.equals(p.pid));

                boolean canFund = true;
                for (int j = 0; j < 3; j++) {
                    if (p.need[j] > available[j]) { canFund = false; break; }
                }

                Color boothBg = isSettled ? new Color(18, 32, 26) : (isCurrentFunding ? new Color(48, 22, 52) : new Color(24, 18, 42));
                Color borderCol = isSettled ? RetroTheme.ACCENT_GREEN : (isCurrentFunding ? RetroTheme.ACCENT_PINK : (canFund ? RetroTheme.ACCENT_YELLOW : new Color(75, 50, 105)));

                // Booth Outer Shell
                g2.setColor(boothBg);
                g2.fillRoundRect(bx, boothY, boothW, boothH, 10, 10);
                g2.setColor(borderCol);
                g2.setStroke(new BasicStroke(isCurrentFunding ? 2.5f : (canFund && !isSettled ? 2.0f : 1.2f)));
                g2.drawRoundRect(bx, boothY, boothW, boothH, 10, 10);

                // Booth Header: Teller Window Tag
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                if (stepIndex == 0) {
                    g2.setColor(RetroTheme.ACCENT_CYAN);
                    g2.drawString("EDITABLE CLIENT", bx + 10, boothY + 18);
                } else if (isSettled) {
                    g2.setColor(RetroTheme.ACCENT_GREEN);
                    g2.drawString("✔ LOAN SETTLED", bx + 10, boothY + 18);
                } else if (isCurrentFunding) {
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    g2.drawString("⚡ DISBURSING", bx + 10, boothY + 18);
                } else if (canFund) {
                    g2.setColor(RetroTheme.ACCENT_YELLOW);
                    g2.drawString("★ CAN FUND", bx + 10, boothY + 18);
                } else {
                    g2.setColor(new Color(160, 160, 190));
                    g2.drawString("🔒 SHORT FUNDS", bx + 10, boothY + 18);
                }

                // Process Avatar & Identifier
                int avSize = 28;
                BufferedImage avatar = PixelSprites.getJobAvatar(i, avSize);
                float bob = (canFund && !isSettled && stepIndex > 0) ? (float)(Math.sin(tickCount * 0.3 + i) * 3) : 0;
                g2.drawImage(avatar, bx + 10, (int)(boothY + 26 + bob), null);

                int textLeft = bx + 10 + avSize + 8;
                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.setColor(Color.WHITE);
                g2.drawString("CLIENT " + p.pid, textLeft, boothY + 39);

                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(new Color(180, 180, 210));
                g2.drawString("MAX: " + parentPanel.vectorToStr(p.max), textLeft, boothY + 52);

                // Divider line inside booth
                g2.setColor(new Color(55, 40, 80));
                g2.drawLine(bx + 8, boothY + 60, bx + boothW - 8, boothY + 60);

                // 💼 Collateral Held Badge (Allocation)
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.drawString("HELD (ALLOC):", bx + 10, boothY + 77);

                String allocStr = parentPanel.vectorToStr(p.alloc);
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                int allocStrW = g2.getFontMetrics().stringWidth(allocStr);
                g2.setColor(Color.WHITE);
                g2.drawString(allocStr, bx + boothW - allocStrW - 10, boothY + 77);

                // 🔒 Loan Demanded Badge (Need)
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                String needLabel = (stepIndex == 0) ? "NEED (IDENTIFY):" : "NEED (M - A):";
                if (stepIndex == 0) {
                    g2.setColor(new Color(160, 160, 190));
                    g2.drawString(needLabel, bx + 10, boothY + 95);

                    String needVal = "[ ? ? ? ]";
                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    int needW = g2.getFontMetrics().stringWidth(needVal);
                    g2.setColor(RetroTheme.ACCENT_YELLOW);
                    g2.drawString(needVal, bx + boothW - needW - 10, boothY + 95);
                } else {
                    g2.setColor(isSettled ? RetroTheme.ACCENT_GREEN : (canFund ? RetroTheme.ACCENT_YELLOW : RetroTheme.ACCENT_PINK));
                    g2.drawString(needLabel, bx + 10, boothY + 95);

                    String needVal = parentPanel.vectorToStr(p.need);
                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    int needW = g2.getFontMetrics().stringWidth(needVal);
                    g2.drawString(needVal, bx + boothW - needW - 10, boothY + 95);
                }

                // Bottom Action Button / Status Badge inside Booth
                int btnBoxY = boothY + 110;
                int btnBoxH = 28;
                int btnBoxW = boothW - 20;
                g2.setColor(stepIndex == 0 ? new Color(24, 18, 36) : (isSettled ? new Color(20, 50, 30) : (canFund ? new Color(60, 45, 15) : new Color(28, 20, 40))));
                g2.fillRoundRect(bx + 10, btnBoxY, btnBoxW, btnBoxH, 6, 6);
                g2.setColor(borderCol);
                g2.drawRoundRect(bx + 10, btnBoxY, btnBoxW, btnBoxH, 6, 6);

                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                String btnText;
                if (stepIndex == 0) {
                    g2.setColor(RetroTheme.ACCENT_CYAN);
                    btnText = "⏳ PENDING AUDIT";
                } else if (isSettled) {
                    g2.setColor(RetroTheme.ACCENT_GREEN);
                    btnText = "✔ SETTLED";
                } else if (isCurrentFunding) {
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    btnText = "⚡ DISBURSING";
                } else if (canFund) {
                    g2.setColor(RetroTheme.ACCENT_YELLOW);
                    btnText = "👉 APPROVE";
                } else {
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    btnText = "❌ DEFICIT";
                }
                int btnTextW = g2.getFontMetrics().stringWidth(btnText);
                g2.drawString(btnText, bx + 10 + (btnBoxW - btnTextW) / 2, btnBoxY + 18);

                clickableBoothCards.add(new Rectangle(bx, boothY, boothW, boothH));
                clickableProcesses.add(p);

                if (isCurrentFunding) {
                    activeBoothCenterX = bx + boothW / 2;
                }
            }

            // Draw Horizontal Scroll Buttons if in unlimited tables mode
            if (maxScrollX > 0) {
                btnPrevRect.setBounds(6, boothY + (boothH - 40) / 2, 26, 40);
                g2.setColor(new Color(22, 14, 38, 240));
                g2.fillRoundRect(btnPrevRect.x, btnPrevRect.y, btnPrevRect.width, btnPrevRect.height, 6, 6);
                g2.setColor((scrollX > 0) ? RetroTheme.ACCENT_CYAN : new Color(70, 50, 95));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(btnPrevRect.x, btnPrevRect.y, btnPrevRect.width, btnPrevRect.height, 6, 6);
                g2.setStroke(new BasicStroke(1.0f));
                g2.setFont(new Font("Consolas", Font.BOLD, 13));
                g2.drawString("◀", btnPrevRect.x + 7, btnPrevRect.y + 25);

                btnNextRect.setBounds(w - 32, boothY + (boothH - 40) / 2, 26, 40);
                g2.setColor(new Color(22, 14, 38, 240));
                g2.fillRoundRect(btnNextRect.x, btnNextRect.y, btnNextRect.width, btnNextRect.height, 6, 6);
                g2.setColor((scrollX < maxScrollX) ? RetroTheme.ACCENT_CYAN : new Color(70, 50, 95));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(btnNextRect.x, btnNextRect.y, btnNextRect.width, btnNextRect.height, 6, 6);
                g2.setStroke(new BasicStroke(1.0f));
                g2.setFont(new Font("Consolas", Font.BOLD, 13));
                g2.drawString("▶", btnNextRect.x + 7, btnNextRect.y + 25);

                // Small Scroll Hint below the central vault
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(new Color(170, 150, 210));
                String scrollHint = String.format("◄ SCROLL MORE CLIENTS (%d TABLES) ►", boothCount);
                int hintW = g2.getFontMetrics().stringWidth(scrollHint);
                g2.drawString(scrollHint, (w - hintW) / 2, boothY - 6);
            } else {
                btnPrevRect.setBounds(0, 0, 0, 0);
                btnNextRect.setBounds(0, 0, 0, 0);
            }

            // =================================================================
            // 4. ANIMATED VACUUM CHUTE / PNEUMATIC LASER TUBE
            // =================================================================
            if (activeBoothCenterX > 0) {
                float pulse = (float)(Math.sin(tickCount * 0.5) * 0.5 + 0.5);
                g2.setColor(new Color(255, 211, 25, (int)(160 + pulse * 95)));
                g2.setStroke(new BasicStroke(3.5f));

                // Laser conduit connects Central Vault to Active Booth
                int vCenterX = w / 2;
                g2.drawLine(vCenterX, vaultY + vaultH, activeBoothCenterX, boothY);

                // Animated glowing energy packet flying along conduit
                float packetFrac = (tickCount % 20) / 20.0f;
                int px = (int)(vCenterX + (activeBoothCenterX - vCenterX) * packetFrac);
                int py = (int)((vaultY + vaultH) + (boothY - (vaultY + vaultH)) * packetFrac);
                g2.setColor(Color.WHITE);
                g2.fillOval(px - 5, py - 5, 10, 10);
            }

            // =================================================================
            // 5. BOTTOM ARENA BANNER (TICKER TAPE)
            // =================================================================
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("BANK CLEARING LOG:", 16, h - 14);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            String note;
            if (activeStep != null) {
                note = String.format("Client %s loan cleared! (Need [%s] <= Vault [%s]). Returned collateral [%s] -> Vault grew to [%s]!",
                        activeStep.pid, parentPanel.vectorToStr(activeStep.need), parentPanel.vectorToStr(activeStep.workBefore),
                        parentPanel.vectorToStr(activeStep.allocReleased), parentPanel.vectorToStr(activeStep.workAfter));
            } else if (feedbackTimer > 0 && !feedbackBanner.isEmpty()) {
                note = feedbackBanner;
            } else if (isSafeState) {
                note = "Central Vault is fully solvent. Click any [CAN FUND] client booth to disburse loans in safe order!";
            } else {
                note = "CRITICAL DEADLOCK ALERT: Bank reserves depleted. No remaining client holds enough collateral to proceed!";
            }
            int maxNoteW = w - 180;
            FontMetrics fm = g2.getFontMetrics();
            if (fm.stringWidth(note) > maxNoteW) {
                while (note.length() > 3 && fm.stringWidth(note + "...") > maxNoteW) {
                    note = note.substring(0, note.length() - 1);
                }
                note += "...";
            }
            g2.drawString(note, 165, h - 14);
        }
    }
}
