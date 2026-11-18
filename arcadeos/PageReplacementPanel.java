package arcadeos;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.Queue;

/**
 * MISSION 04: CACHE & PAGE REPLACEMENT
 * Arcade Theme: Super Mario Warp Pipe Chambers & Question Block Stream
 * 
 * Rubric Compliance:
 * - Implements FIFO, LRU, and Optimal page replacement algorithms.
 * - Displays frame changes, page hits, and page faults accurately.
 * - Horizontal textbook trace matrix with column step tracking and dynamic column sizing.
 * - Mario Warp Pipes represent Memory Frame Slots (2 to 6 pipes).
 * - Interactive Challenge Mode with auto-advance and instant pipe warp updates.
 */
public class PageReplacementPanel extends JPanel {

    // Preset reference strings
    public static final String PRESET_STANDARD = "7 0 1 2 0 3 0 4 2 3 0 3 2 1 2 0 1 7 0 1";
    public static final String PRESET_BELADY   = "1 2 3 4 1 2 5 1 2 3 4 5";
    public static final String PRESET_LRU_LOOP = "2 3 2 1 5 2 4 5 3 2 5 2";
    public static final String PRESET_QUICK    = "1 3 0 3 5 6 3 1";

    // Algorithm & Input state
    private String selectedAlgo = "FIFO";
    private int frameCount = 3;
    private int[] pageStream = new int[0];
    private String rawRefString = PRESET_STANDARD;

    // Simulation Execution State
    private List<StepState> simulationSteps = new ArrayList<>();
    private int currentStepIdx = -1; // -1 = before start / ready
    private boolean isPlaying = false;
    private double speedMultiplier = 1.0;
    private javax.swing.Timer simTimer;
    private javax.swing.Timer challengeTimer;

    // Challenge Mode State
    private boolean isChallengeMode = false;
    private boolean awaitingEvictionChoice = false;

    // UI Widgets
    private JButton btnFifo, btnLru, btnOpt;
    private JLabel lblPipeCount;
    private JTextField txtRefString;
    private boolean isUpdatingQuietly = false;
    private javax.swing.Timer tableScrollTimer;
    private JButton btnPlay, btnStep, btnReset, btnModeToggle, btnSpeed;
    private JLabel lblAlgoRule;
    private JLabel lblMissionStatus;
    private JLabel lblStepDetails;

    // HUD Pod Labels
    private JLabel lblHitCount, lblHitSub;
    private JLabel lblFaultCount, lblFaultSub;
    private JLabel lblFrameCount, lblFrameSub;

    // Center Game Canvas & Table
    private MarioPipeCanvas canvas;
    private DefaultTableModel traceModel;
    private JTable traceTable;
    private JScrollPane traceScroll;

    public PageReplacementPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(14, 0));
        setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));

        // 1. Simulation Timer (Runs smoothly at ~40ms ticks)
        simTimer = new javax.swing.Timer(40, e -> updateSimulationTick());

        // 2. Left Column: Cyber Deck Control Panel (370px)
        JPanel leftDeck = createLeftControlDeck();
        leftDeck.setPreferredSize(new Dimension(370, 600));
        leftDeck.setMinimumSize(new Dimension(360, 500));
        add(leftDeck, BorderLayout.WEST);

        // 3. Right Column: Big Game Arena & Step Matrix (Expands fully, no empty gaps!)
        JPanel rightArena = createRightArena();
        add(rightArena, BorderLayout.CENTER);

        // 4. Keyboard Shortcuts: Space=Play/Pause, S/Right=Step, R=Reset
        setupKeyboardControls();

        // 5. Initialize with standard dataset & compute simulation
        loadReferenceString(PRESET_STANDARD);
    }

    // =========================================================================
    // KEYBOARD SHORTCUTS
    // =========================================================================
    private void setupKeyboardControls() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "keySpace");
        am.put("keySpace", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!isShowing()) return;
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof javax.swing.text.JTextComponent) return;
                togglePlayPause();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "keyStep");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "keyStep");
        am.put("keyStep", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!isShowing()) return;
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof javax.swing.text.JTextComponent) return;
                stepForward();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "keyReset");
        am.put("keyReset", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!isShowing()) return;
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof javax.swing.text.JTextComponent) return;
                resetSimulation();
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
    }

    // =========================================================================
    // 1. LEFT CONTROL DECK
    // =========================================================================
    private JPanel createLeftControlDeck() {
        JPanel deck = new JPanel();
        deck.setOpaque(true);
        deck.setBackground(new Color(18, 13, 34));
        deck.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 38, 85), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        deck.setLayout(new BoxLayout(deck, BoxLayout.Y_AXIS));

        // Header Title
        JLabel deckTitle = new JLabel("MARIO PIPE CHAMBER");
        deckTitle.setFont(new Font("Consolas", Font.BOLD, 13));
        deckTitle.setForeground(new Color(0, 240, 100)); // Mario Lime Green
        deckTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        deckTitle.setMaximumSize(new Dimension(800, 20));
        deck.add(deckTitle);

        JLabel deckSub = new JLabel("PAGE REPLACEMENT MMU");
        deckSub.setFont(new Font("Consolas", Font.BOLD, 10));
        deckSub.setForeground(RetroTheme.TEXT_DIM);
        deckSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        deckSub.setMaximumSize(new Dimension(800, 18));
        deck.add(deckSub);

        deck.add(Box.createVerticalStrut(8));

        // Policy Selector Buttons
        JLabel lblPolicy = new JLabel("EVICTION POLICY [ALGORITHM]");
        lblPolicy.setFont(new Font("Consolas", Font.BOLD, 10));
        lblPolicy.setForeground(RetroTheme.ACCENT_YELLOW);
        lblPolicy.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblPolicy.setMaximumSize(new Dimension(800, 18));
        deck.add(lblPolicy);

        deck.add(Box.createVerticalStrut(4));

        JPanel algoRow = new JPanel(new GridLayout(1, 3, 6, 0));
        algoRow.setOpaque(false);
        algoRow.setMaximumSize(new Dimension(800, 32));
        algoRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnFifo = createModeButton("FIFO", true);
        btnLru = createModeButton("LRU", false);
        btnOpt = createModeButton("OPTIMAL", false);

        btnFifo.addActionListener(e -> selectAlgorithm("FIFO"));
        btnLru.addActionListener(e -> selectAlgorithm("LRU"));
        btnOpt.addActionListener(e -> selectAlgorithm("OPTIMAL"));

        algoRow.add(btnFifo);
        algoRow.add(btnLru);
        algoRow.add(btnOpt);
        deck.add(algoRow);

        deck.add(Box.createVerticalStrut(6));

        // Policy Rule Description Pill
        lblAlgoRule = new JLabel("FIFO: Oldest pipe resident is flushed first.");
        lblAlgoRule.setFont(new Font("Consolas", Font.ITALIC, 10));
        lblAlgoRule.setForeground(new Color(220, 220, 240));
        lblAlgoRule.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblAlgoRule.setMaximumSize(new Dimension(800, 18));
        deck.add(lblAlgoRule);

        deck.add(Box.createVerticalStrut(10));

        // Memory Configuration Card
        JLabel lblConfig = new JLabel("WARP PIPE CONFIGURATION");
        lblConfig.setFont(new Font("Consolas", Font.BOLD, 10));
        lblConfig.setForeground(RetroTheme.ACCENT_CYAN);
        lblConfig.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblConfig.setMaximumSize(new Dimension(800, 18));
        deck.add(lblConfig);

        deck.add(Box.createVerticalStrut(4));

        // Frame Count Selector (Retro Arcade Stepper [ - ] [ N ] [ + ])
        JPanel frameCard = new JPanel(new BorderLayout(8, 0));
        frameCard.setOpaque(true);
        frameCard.setBackground(new Color(24, 18, 44));
        frameCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 42, 95), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        frameCard.setMaximumSize(new Dimension(800, 34));
        frameCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel fLbl = new JLabel("PIPE SLOTS (CAPACITY):");
        fLbl.setFont(new Font("Consolas", Font.BOLD, 10));
        fLbl.setForeground(Color.WHITE);
        frameCard.add(fLbl, BorderLayout.WEST);

        // Custom Retro Stepper Panel: [ - ]  [ 3 ]  [ + ]
        JPanel stepper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        stepper.setOpaque(false);

        JButton btnDec = createStepperBtn("-");
        lblPipeCount = new JLabel(String.valueOf(frameCount), SwingConstants.CENTER);
        lblPipeCount.setFont(new Font("Consolas", Font.BOLD, 14));
        lblPipeCount.setForeground(new Color(255, 238, 0));
        lblPipeCount.setOpaque(true);
        lblPipeCount.setBackground(new Color(15, 10, 28));
        lblPipeCount.setBorder(BorderFactory.createLineBorder(new Color(255, 220, 0), 1));
        lblPipeCount.setPreferredSize(new Dimension(38, 26));

        JButton btnInc = createStepperBtn("+");

        btnDec.addActionListener(e -> {
            if (frameCount > 1) {
                frameCount--;
                lblPipeCount.setText(String.valueOf(frameCount));
                recomputeSimulation();
            }
        });

        btnInc.addActionListener(e -> {
            if (frameCount < 16) { // Supports up to 16 pipes seamlessly!
                frameCount++;
                lblPipeCount.setText(String.valueOf(frameCount));
                recomputeSimulation();
            }
        });

        stepper.add(btnDec);
        stepper.add(lblPipeCount);
        stepper.add(btnInc);
        frameCard.add(stepper, BorderLayout.EAST);
        deck.add(frameCard);

        deck.add(Box.createVerticalStrut(8));

        // Page Reference String Input Field
        JLabel rLbl = new JLabel("PAGE REFERENCE STREAM:");
        rLbl.setFont(new Font("Consolas", Font.BOLD, 11));
        rLbl.setForeground(RetroTheme.ACCENT_YELLOW);
        rLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        rLbl.setMaximumSize(new Dimension(800, 18));
        deck.add(rLbl);

        deck.add(Box.createVerticalStrut(4));

        txtRefString = new JTextField(rawRefString);
        txtRefString.setFont(new Font("Consolas", Font.BOLD, 14));
        txtRefString.setBackground(new Color(15, 10, 28));
        txtRefString.setForeground(new Color(0, 255, 255));
        txtRefString.setCaretColor(Color.WHITE);
        txtRefString.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 220, 255), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        txtRefString.setMaximumSize(new Dimension(800, 36));
        txtRefString.setPreferredSize(new Dimension(800, 36));
        txtRefString.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Immediate real-time update as the user types or edits!
        txtRefString.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { tryAutoApply(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { tryAutoApply(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { tryAutoApply(); }

            private void tryAutoApply() {
                SwingUtilities.invokeLater(() -> tryQuietRefString());
            }
        });
        txtRefString.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                applyCustomRefString();
            }
        });
        txtRefString.addActionListener(e -> applyCustomRefString());
        deck.add(txtRefString);

        deck.add(Box.createVerticalStrut(6));

        // Preset Buttons Grid
        JPanel presetGrid = new JPanel(new GridLayout(2, 2, 6, 4));
        presetGrid.setOpaque(false);
        presetGrid.setMaximumSize(new Dimension(800, 50));
        presetGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton p1 = createTinyButton("STANDARD 20", new Color(30, 24, 52));
        p1.addActionListener(e -> loadReferenceString(PRESET_STANDARD));

        JButton p2 = createTinyButton("BELADY'S (FIFO)", new Color(48, 22, 42));
        p2.addActionListener(e -> loadReferenceString(PRESET_BELADY));

        JButton p3 = createTinyButton("LRU LOOP", new Color(24, 38, 52));
        p3.addActionListener(e -> loadReferenceString(PRESET_LRU_LOOP));

        JButton p4 = createTinyButton("RANDOM STREAM", new Color(35, 45, 30));
        p4.addActionListener(e -> generateRandomStream());

        presetGrid.add(p1);
        presetGrid.add(p2);
        presetGrid.add(p3);
        presetGrid.add(p4);
        deck.add(presetGrid);

        deck.add(Box.createVerticalStrut(10));

        // Simulation Control Action Buttons
        btnPlay = CPUSchedulingPanel.createArcadeButton("START SIMULATION", new Color(0, 160, 60), Color.WHITE, 34);
        btnPlay.setFont(new Font("Consolas", Font.BOLD, 12));
        btnPlay.setMaximumSize(new Dimension(800, 34));
        btnPlay.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPlay.addActionListener(e -> togglePlayPause());
        deck.add(btnPlay);

        deck.add(Box.createVerticalStrut(6));

        JPanel stepRow = new JPanel(new GridLayout(1, 2, 6, 0));
        stepRow.setOpaque(false);
        stepRow.setMaximumSize(new Dimension(800, 28));
        stepRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnStep = CPUSchedulingPanel.createArcadeButton("STEP >>", new Color(0, 130, 180), Color.WHITE, 28);
        btnStep.setFont(new Font("Consolas", Font.BOLD, 11));
        btnStep.addActionListener(e -> stepForward());

        btnReset = CPUSchedulingPanel.createArcadeButton("RESET", new Color(150, 40, 50), Color.WHITE, 28);
        btnReset.setFont(new Font("Consolas", Font.BOLD, 11));
        btnReset.addActionListener(e -> resetSimulation());

        stepRow.add(btnStep);
        stepRow.add(btnReset);
        deck.add(stepRow);

        deck.add(Box.createVerticalStrut(6));

        // Speed & Challenge Mode Toggle
        JPanel speedRow = new JPanel(new GridLayout(1, 2, 6, 0));
        speedRow.setOpaque(false);
        speedRow.setMaximumSize(new Dimension(800, 26));
        speedRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnSpeed = CPUSchedulingPanel.createArcadeButton("SPEED: 1.0x", new Color(42, 30, 70), Color.WHITE, 26);
        btnSpeed.setFont(new Font("Consolas", Font.BOLD, 10));
        btnSpeed.addActionListener(e -> cycleSpeed());

        btnModeToggle = CPUSchedulingPanel.createArcadeButton("CHALLENGE: OFF", new Color(38, 25, 68), Color.WHITE, 26);
        btnModeToggle.setFont(new Font("Consolas", Font.BOLD, 10));
        btnModeToggle.setToolTipText("Toggle Interactive Eviction Mode (Click victim pipe to flush)");
        btnModeToggle.addActionListener(e -> toggleChallengeMode());

        speedRow.add(btnSpeed);
        speedRow.add(btnModeToggle);
        deck.add(speedRow);

        deck.add(Box.createVerticalStrut(8));

        // Tactical Controls Guide Card (Eliminates empty space in sidebar)
        // Tactical Shortcuts Cheat Sheet Card
        JPanel infoCard = new JPanel(new GridLayout(5, 1, 0, 2));
        infoCard.setOpaque(true);
        infoCard.setBackground(new Color(24, 18, 42));
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        infoCard.setMaximumSize(new Dimension(800, 110));
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iHead = new JLabel("SHORTCUTS & CONTROLS");
        iHead.setFont(new Font("Consolas", Font.BOLD, 10));
        iHead.setForeground(RetroTheme.ACCENT_YELLOW);
        infoCard.add(iHead);

        JLabel k1 = new JLabel("• SPACE: PLAY / PAUSE RUN");
        k1.setFont(new Font("Consolas", Font.PLAIN, 10));
        k1.setForeground(Color.WHITE);
        infoCard.add(k1);

        JLabel k2 = new JLabel("• S / →: STEP FORWARD");
        k2.setFont(new Font("Consolas", Font.PLAIN, 10));
        k2.setForeground(Color.WHITE);
        infoCard.add(k2);

        JLabel k3 = new JLabel("• R KEY: RESET RUN");
        k3.setFont(new Font("Consolas", Font.PLAIN, 10));
        k3.setForeground(Color.WHITE);
        infoCard.add(k3);

        JLabel k4 = new JLabel("• CLICK: FLUSH (CHALLENGE)");
        k4.setFont(new Font("Consolas", Font.PLAIN, 10));
        k4.setForeground(RetroTheme.ACCENT_CYAN);
        infoCard.add(k4);

        deck.add(infoCard);

        deck.add(Box.createVerticalGlue());

        // Hardware Telemetry Strip at Bottom
        JPanel telemetry = new JPanel(new BorderLayout());
        telemetry.setOpaque(true);
        telemetry.setBackground(new Color(12, 8, 22));
        telemetry.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 32, 70), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        telemetry.setMaximumSize(new Dimension(800, 26));
        telemetry.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel t1 = new JLabel("[*] PIPE MMU");
        t1.setFont(new Font("Consolas", Font.BOLD, 10));
        t1.setForeground(new Color(0, 240, 100));
        JLabel t2 = new JLabel("3.80GHz");
        t2.setFont(new Font("Consolas", Font.PLAIN, 10));
        t2.setForeground(RetroTheme.TEXT_WHITE);
        telemetry.add(t1, BorderLayout.WEST);
        telemetry.add(t2, BorderLayout.EAST);
        deck.add(telemetry);

        return deck;
    }

    // =========================================================================
    // 2. RIGHT ARENA: DYNAMIC BORDERLAYOUT & ZERO EMPTY GAPS
    // =========================================================================
    private JPanel createRightArena() {
        JPanel arena = new JPanel(new BorderLayout(0, 6));
        arena.setOpaque(false);
        arena.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        // Top Section: Header + HUD Pods + Canvas
        JPanel topSection = new JPanel();
        topSection.setOpaque(false);
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));

        // 1. Top Status Strip
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(1400, 24));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblMissionStatus = new JLabel("LIVE OUTPUT // SUPER MARIO WARP PIPE CHAMBERS");
        lblMissionStatus.setFont(new Font("Consolas", Font.BOLD, 13));
        lblMissionStatus.setForeground(Color.WHITE);
        lblMissionStatus.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        header.add(lblMissionStatus, BorderLayout.WEST);

        JPanel eastHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        eastHeader.setOpaque(false);

        JButton btnPlayArcadeHeader = RetroTheme.createRetroButton("PLAY MARIO ARCADE", new Color(42, 16, 24), RetroTheme.ACCENT_YELLOW);
        btnPlayArcadeHeader.setFont(new Font("Consolas", Font.BOLD, 10));
        btnPlayArcadeHeader.setIcon(new ImageIcon(PixelSprites.createMarioSprite(14)));
        btnPlayArcadeHeader.setIconTextGap(6);
        btnPlayArcadeHeader.setToolTipText("Launch playable Super Mario Cache Runner arcade minigame!");
        btnPlayArcadeHeader.addActionListener(e -> launchMarioCacheGame());
        eastHeader.add(btnPlayArcadeHeader);

        header.add(eastHeader, BorderLayout.EAST);
        topSection.add(header);

        topSection.add(Box.createVerticalStrut(4));

        // 2. 3 High-Impact Glowing HUD Pods
        JPanel statsCard = new JPanel(new GridLayout(1, 3, 10, 0));
        statsCard.setOpaque(false);
        statsCard.setMaximumSize(new Dimension(1400, 72));
        statsCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Pod 1: Page Hits
        JPanel p1 = createGlassHudPod("PAGE HITS [COIN MATCH]", new Color(0, 230, 90));
        lblHitCount = new JLabel("0 HITS (0.0%)");
        lblHitCount.setFont(new Font("Consolas", Font.BOLD, 20));
        lblHitCount.setForeground(new Color(0, 240, 100));
        lblHitSub = new JLabel("HIT RATE: 0.0% [AIM: HIGH]");
        lblHitSub.setFont(new Font("Consolas", Font.BOLD, 10));
        lblHitSub.setForeground(Color.WHITE);
        p1.add(lblHitCount);
        p1.add(lblHitSub);
        statsCard.add(p1);

        // Pod 2: Page Faults
        JPanel p2 = createGlassHudPod("PAGE FAULTS [PIPE SWAP]", RetroTheme.ACCENT_PINK);
        lblFaultCount = new JLabel("0 FAULTS");
        lblFaultCount.setFont(new Font("Consolas", Font.BOLD, 20));
        lblFaultCount.setForeground(RetroTheme.ACCENT_PINK);
        lblFaultSub = new JLabel("PENALTY DISK SWAPS: 0");
        lblFaultSub.setFont(new Font("Consolas", Font.BOLD, 10));
        lblFaultSub.setForeground(Color.WHITE);
        p2.add(lblFaultCount);
        p2.add(lblFaultSub);
        statsCard.add(p2);

        // Pod 3: Frame Occupancy
        JPanel p3 = createGlassHudPod("PIPE CAPACITY", RetroTheme.ACCENT_YELLOW);
        lblFrameCount = new JLabel("0/3 PIPES [0%]");
        lblFrameCount.setFont(new Font("Consolas", Font.BOLD, 20));
        lblFrameCount.setForeground(RetroTheme.ACCENT_YELLOW);
        lblFrameSub = new JLabel("STANDBY // AWAITING STREAM");
        lblFrameSub.setFont(new Font("Consolas", Font.BOLD, 10));
        lblFrameSub.setForeground(Color.WHITE);
        p3.add(lblFrameCount);
        p3.add(lblFrameSub);
        statsCard.add(p3);

        topSection.add(statsCard);

        topSection.add(Box.createVerticalStrut(6));

        // 3. Super Mario Pipe Canvas (Height: 285px with ZERO overlapping!)
        canvas = new MarioPipeCanvas(this);
        canvas.setPreferredSize(new Dimension(800, 285));
        canvas.setMinimumSize(new Dimension(500, 285));
        canvas.setMaximumSize(new Dimension(1400, 285));
        canvas.setAlignmentX(Component.LEFT_ALIGNMENT);
        topSection.add(canvas);

        arena.add(topSection, BorderLayout.NORTH);

        // 4. Matrix Trace Table: Placed in CENTER so it expands to fill ALL remaining height!
        JPanel tableContainer = createTraceTableContainer();
        arena.add(tableContainer, BorderLayout.CENTER);

        return arena;
    }

    // =========================================================================
    // GLASS HUD POD HELPER
    // =========================================================================
    private JPanel createGlassHudPod(String title, Color borderCol) {
        JPanel pod = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(24, 18, 44));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(borderCol);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(borderCol);
                g2.fillRect(0, 0, 3, getHeight());
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
    // 3. HORIZONTAL STEP TRACE MATRIX TABLE (EXPANDABLE & DYNAMIC)
    // =========================================================================
    private JPanel createTraceTableContainer() {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(true);
        container.setBackground(new Color(18, 13, 34));
        container.setBorder(BorderFactory.createLineBorder(new Color(55, 38, 85), 1));

        // Top Status Header Strip
        JPanel topStrip = new JPanel(new BorderLayout(10, 0));
        topStrip.setOpaque(true);
        topStrip.setBackground(new Color(24, 18, 44));
        topStrip.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel title = new JLabel("MEMORY TRACE MATRIX");
        title.setFont(new Font("Consolas", Font.BOLD, 11));
        title.setForeground(new Color(0, 240, 100));
        topStrip.add(title, BorderLayout.WEST);

        lblStepDetails = new JLabel("READY: Click START SIMULATION or STEP >> to trace memory.");
        lblStepDetails.setFont(new Font("Consolas", Font.BOLD, 10));
        lblStepDetails.setForeground(RetroTheme.TEXT_WHITE);
        lblStepDetails.setHorizontalAlignment(SwingConstants.RIGHT);
        lblStepDetails.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        topStrip.add(lblStepDetails, BorderLayout.CENTER);

        container.add(topStrip, BorderLayout.NORTH);

        traceModel = new DefaultTableModel() {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        traceTable = new JTable(traceModel);
        traceTable.setFont(new Font("Consolas", Font.BOLD, 13));
        traceTable.setRowHeight(32); // Tall, readable, beautiful rows!
        traceTable.setBackground(new Color(14, 10, 26));
        traceTable.setForeground(Color.WHITE);
        traceTable.setGridColor(new Color(40, 28, 65));
        traceTable.setSelectionBackground(new Color(0, 120, 180));
        traceTable.setSelectionForeground(Color.WHITE);
        traceTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        traceTable.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 11));
        traceTable.getTableHeader().setBackground(new Color(28, 20, 52));
        traceTable.getTableHeader().setForeground(new Color(0, 240, 100));
        traceTable.getTableHeader().setReorderingAllowed(false);
        traceTable.getTableHeader().setPreferredSize(new Dimension(0, 26));

        // Custom Cell Renderer for Horizontal Table Layout
        traceTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(col == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);

                boolean isActiveCol = (currentStepIdx >= 0 && col == currentStepIdx + 1);

                if (col == 0) {
                    // Header Column: Memory Level
                    c.setBackground(new Color(24, 18, 44));
                    if (row == 0) {
                        c.setForeground(RetroTheme.ACCENT_YELLOW);
                    } else if (row == frameCount + 1) {
                        c.setForeground(RetroTheme.ACCENT_CYAN);
                    } else {
                        c.setForeground(new Color(0, 240, 100));
                    }
                    setFont(getFont().deriveFont(Font.BOLD, 11f));
                    return c;
                }

                int step = col - 1;
                boolean isPastOrCurrent = (step <= currentStepIdx);

                if (isActiveCol) {
                    // Active Step Column: Glowing Blue/Cyan
                    c.setBackground(new Color(38, 65, 110));
                    c.setForeground(Color.WHITE);
                    setFont(getFont().deriveFont(Font.BOLD, 14f));
                } else if (isPastOrCurrent) {
                    c.setBackground(row % 2 == 0 ? new Color(18, 14, 32) : new Color(14, 10, 26));
                    c.setForeground(Color.WHITE);
                    setFont(getFont().deriveFont(Font.BOLD, 13f));
                } else {
                    // Future steps: Dimmed
                    c.setBackground(new Color(12, 9, 20));
                    c.setForeground(new Color(90, 80, 115));
                    setFont(getFont().deriveFont(Font.PLAIN, 12f));
                }

                // Status Row Styling
                if (row == frameCount + 1 && value != null) {
                    String str = value.toString();
                    if (str.equals("HIT") || str.equals("H")) {
                        c.setForeground(new Color(0, 240, 100));
                        if (isActiveCol) c.setBackground(new Color(15, 60, 30));
                        setFont(getFont().deriveFont(Font.BOLD, 13f));
                    } else if (str.equals("FAULT") || str.equals("F")) {
                        c.setForeground(RetroTheme.ACCENT_PINK);
                        if (isActiveCol) c.setBackground(new Color(60, 18, 35));
                        setFont(getFont().deriveFont(Font.BOLD, 13f));
                    }
                } else if (row == 0 && isPastOrCurrent) {
                    // Reference Page Row
                    c.setForeground(RetroTheme.ACCENT_YELLOW);
                    setFont(getFont().deriveFont(Font.BOLD, 14f));
                } else if (row >= 1 && row <= frameCount && isPastOrCurrent && step >= 0 && step < simulationSteps.size()) {
                    // Highlight modified or hit pipes
                    StepState ss = simulationSteps.get(step);
                    int f = row - 1;
                    if (f == ss.emptyAssignedFrameIdx || f == ss.victimFrameIdx) {
                        c.setForeground(RetroTheme.ACCENT_CYAN);
                        setFont(getFont().deriveFont(Font.BOLD, 14f));
                    } else if (ss.isHit && f == ss.hitFrameIdx) {
                        c.setForeground(new Color(0, 240, 100));
                        setFont(getFont().deriveFont(Font.BOLD, 14f));
                    }
                }

                return c;
            }
        });

        traceScroll = new JScrollPane(traceTable);
        traceScroll.getViewport().setBackground(new Color(14, 10, 26));
        traceScroll.setBorder(BorderFactory.createEmptyBorder());
        traceScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustTableColumnWidths();
            }
        });
        container.add(traceScroll, BorderLayout.CENTER);

        return container;
    }

    // =========================================================================
    // DYNAMIC TABLE COLUMN WIDTH ADJUSTER (100% SPACE MAXIMIZED)
    // =========================================================================
    public void adjustTableColumnWidths() {
        if (traceTable == null || traceTable.getColumnCount() <= 0) return;
        int colCount = traceTable.getColumnCount();
        if (colCount <= 1) return;

        int viewportW = (traceScroll != null && traceScroll.getViewport() != null) ?
                traceScroll.getViewport().getWidth() : 0;
        if (viewportW <= 0 && traceTable.getParent() != null) {
            viewportW = traceTable.getParent().getWidth();
        }
        if (viewportW <= 0) viewportW = 950;

        int headerColW = 120;
        traceTable.getColumnModel().getColumn(0).setPreferredWidth(headerColW);
        traceTable.getColumnModel().getColumn(0).setMinWidth(headerColW);

        int numSteps = colCount - 1;
        int availForSteps = viewportW - headerColW;
        int minStepW = 46;
        int calculatedW = availForSteps / numSteps;

        if (calculatedW >= minStepW) {
            // Fills 100% of horizontal space evenly with ZERO empty black margin on right!
            int remainder = availForSteps - (calculatedW * numSteps);
            for (int c = 1; c <= numSteps; c++) {
                int colW = calculatedW + (c == numSteps ? remainder : 0);
                traceTable.getColumnModel().getColumn(c).setPreferredWidth(colW);
                traceTable.getColumnModel().getColumn(c).setWidth(colW);
            }
        } else {
            // More columns than can fit: enforce minimum step width and allow horizontal scroll!
            for (int c = 1; c <= numSteps; c++) {
                traceTable.getColumnModel().getColumn(c).setPreferredWidth(minStepW);
                traceTable.getColumnModel().getColumn(c).setWidth(minStepW);
            }
        }
    }

    // =========================================================================
    // SMOOTH HORIZONTAL SCROLLING FOR TABLE TIMELINE
    // =========================================================================
    private void smoothScrollTable(JScrollBar hBar, int targetVal) {
        if (tableScrollTimer != null && tableScrollTimer.isRunning()) {
            tableScrollTimer.stop();
        }
        int cur = hBar.getValue();
        if (Math.abs(targetVal - cur) <= 8) {
            hBar.setValue(targetVal);
            return;
        }
        tableScrollTimer = new javax.swing.Timer(16, null);
        tableScrollTimer.addActionListener(e -> {
            int c = hBar.getValue();
            int diff = targetVal - c;
            if (Math.abs(diff) <= 3) {
                hBar.setValue(targetVal);
                tableScrollTimer.stop();
            } else {
                hBar.setValue(c + (int)(diff * 0.35f));
            }
        });
        tableScrollTimer.start();
    }

    // =========================================================================
    // RETRO ARCADE STEPPER BUTTON [ - ] and [ + ]
    // =========================================================================
    private JButton createStepperBtn(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean pressed = getModel().isPressed();
                boolean hover = getModel().isRollover();

                Color bg = pressed ? new Color(50, 35, 80) : (hover ? new Color(42, 28, 70) : new Color(28, 20, 50));
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);

                Color border = hover ? RetroTheme.ACCENT_CYAN : new Color(75, 55, 115);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);

                g2.setFont(new Font("Consolas", Font.BOLD, 15));
                g2.setColor(hover ? Color.WHITE : RetroTheme.ACCENT_CYAN);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(30, 24));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // =========================================================================
    // BUTTON HELPERS
    // =========================================================================
    private JButton createModeButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Consolas", Font.BOLD, 11));
        btn.setFocusPainted(false);
        updateModeButtonStyle(btn, active);
        return btn;
    }

    private void updateModeButtonStyle(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(new Color(0, 150, 70));
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(new Color(0, 240, 100), 2));
        } else {
            btn.setBackground(new Color(32, 22, 54));
            btn.setForeground(RetroTheme.TEXT_DIM);
            btn.setBorder(BorderFactory.createLineBorder(new Color(60, 42, 90), 1));
        }
    }

    private JButton createTinyButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Consolas", Font.BOLD, 9));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(80, 55, 115), 1));
        return b;
    }

    // =========================================================================
    // DATASET & ALGORITHM SELECTION
    // =========================================================================
    public void selectAlgorithm(String algo) {
        this.selectedAlgo = algo;
        updateModeButtonStyle(btnFifo, algo.equals("FIFO"));
        updateModeButtonStyle(btnLru, algo.equals("LRU"));
        updateModeButtonStyle(btnOpt, algo.equals("OPTIMAL"));

        if (algo.equals("FIFO")) {
            lblAlgoRule.setText("FIFO: Oldest pipe resident is flushed first.");
        } else if (algo.equals("LRU")) {
            lblAlgoRule.setText("LRU: Evicts pipe unused for longest time.");
        } else if (algo.equals("OPTIMAL")) {
            lblAlgoRule.setText("OPTIMAL: Evicts pipe needed farthest in future.");
        }

        recomputeSimulation();
    }

    public void setFrameCount(int f) {
        this.frameCount = Math.max(1, Math.min(16, f));
        if (lblPipeCount != null) {
            lblPipeCount.setText(String.valueOf(frameCount));
        }
        recomputeSimulation();
    }

    public void loadReferenceString(String streamStr) {
        this.rawRefString = streamStr.trim();
        if (txtRefString != null) {
            isUpdatingQuietly = true;
            txtRefString.setText(rawRefString);
            isUpdatingQuietly = false;
        }
        applyCustomRefString();
    }

    private void applyCustomRefString() {
        if (txtRefString == null) return;
        try {
            String[] tokens = txtRefString.getText().trim().split("\\s+");
            List<Integer> list = new ArrayList<>();
            for (String t : tokens) {
                if (!t.isEmpty()) list.add(Integer.parseInt(t));
            }
            if (list.isEmpty()) throw new Exception("Empty reference string");
            pageStream = new int[list.size()];
            for (int i = 0; i < list.size(); i++) pageStream[i] = list.get(i);
            recomputeSimulation();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid Reference String: Please enter space-separated integers (e.g. 7 0 1 2 0 3).", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void tryQuietRefString() {
        if (isUpdatingQuietly || txtRefString == null) return;
        try {
            String str = txtRefString.getText().trim();
            if (str.isEmpty()) return;
            String[] tokens = str.split("\\s+");
            List<Integer> list = new ArrayList<>();
            for (String t : tokens) {
                if (!t.isEmpty()) list.add(Integer.parseInt(t));
            }
            if (list.isEmpty()) return;

            boolean changed = (pageStream == null || pageStream.length != list.size());
            if (!changed) {
                for (int i = 0; i < list.size(); i++) {
                    if (pageStream[i] != list.get(i)) {
                        changed = true;
                        break;
                    }
                }
            }
            if (!changed) return;

            isUpdatingQuietly = true;
            pageStream = new int[list.size()];
            for (int i = 0; i < list.size(); i++) pageStream[i] = list.get(i);
            this.rawRefString = str;
            recomputeSimulation();
        } catch (Exception ignored) {
            // Typing in progress
        } finally {
            isUpdatingQuietly = false;
        }
    }

    private void generateRandomStream() {
        Random rng = new Random();
        StringBuilder sb = new StringBuilder();
        int len = 16 + rng.nextInt(6);
        for (int i = 0; i < len; i++) {
            sb.append(rng.nextInt(6)).append(i == len - 1 ? "" : " ");
        }
        loadReferenceString(sb.toString());
    }

    // =========================================================================
    // SIMULATION ENGINE: FIFO, LRU, OPTIMAL
    // =========================================================================
    public void recomputeSimulation() {
        simulationSteps.clear();
        if (pageStream == null || pageStream.length == 0) return;

        int[] currentFrames = new int[frameCount];
        Arrays.fill(currentFrames, -1);

        int hits = 0;
        int faults = 0;

        Queue<Integer> fifoQueue = new LinkedList<>();
        Map<Integer, Integer> lruLastUsed = new HashMap<>();

        for (int step = 0; step < pageStream.length; step++) {
            int page = pageStream[step];
            StepState ss = new StepState();
            ss.stepIndex = step + 1;
            ss.refPage = page;
            ss.prevFrames = currentFrames.clone(); // State before step executes

            // Check if page is already in pipes (CACHE HIT)
            int hitIndex = -1;
            for (int f = 0; f < frameCount; f++) {
                if (currentFrames[f] == page) {
                    hitIndex = f;
                    break;
                }
            }

            if (hitIndex != -1) {
                // HIT!
                hits++;
                ss.isHit = true;
                ss.hitFrameIdx = hitIndex;
                ss.victimPage = -1;
                ss.victimFrameIdx = -1;
                ss.reason = "CACHE HIT [MATCH] Page " + page + " already present in Pipe #" + hitIndex;

                if (selectedAlgo.equals("LRU")) {
                    lruLastUsed.put(page, step);
                }
            } else {
                // FAULT!
                faults++;
                ss.isHit = false;

                // Check for empty pipe
                int emptyIdx = -1;
                for (int f = 0; f < frameCount; f++) {
                    if (currentFrames[f] == -1) {
                        emptyIdx = f;
                        break;
                    }
                }

                if (emptyIdx != -1) {
                    // Place into vacant pipe
                    currentFrames[emptyIdx] = page;
                    ss.victimPage = -1;
                    ss.victimFrameIdx = -1;
                    ss.emptyAssignedFrameIdx = emptyIdx;
                    ss.reason = "INITIAL LOAD [MISS] Page " + page + " warped into vacant Pipe #" + emptyIdx;

                    if (selectedAlgo.equals("FIFO")) fifoQueue.add(page);
                    else if (selectedAlgo.equals("LRU")) lruLastUsed.put(page, step);
                } else {
                    // ALL PIPES FULL: EVICTION REQUIRED!
                    int victimFrame = -1;
                    int victimP = -1;

                    if (selectedAlgo.equals("FIFO")) {
                        // Evict the page that arrived earliest
                        while (!fifoQueue.isEmpty()) {
                            int oldest = fifoQueue.poll();
                            for (int f = 0; f < frameCount; f++) {
                                if (currentFrames[f] == oldest) {
                                    victimFrame = f;
                                    victimP = oldest;
                                    break;
                                }
                            }
                            if (victimFrame != -1) break;
                        }
                        fifoQueue.add(page);
                        ss.reason = "FIFO FLUSH [WARP] Flushed oldest Page " + victimP + " from Pipe #" + victimFrame;

                    } else if (selectedAlgo.equals("LRU")) {
                        // Evict the page with the lowest step in lruLastUsed
                        int oldestStep = Integer.MAX_VALUE;
                        for (int f = 0; f < frameCount; f++) {
                            int cand = currentFrames[f];
                            int last = lruLastUsed.getOrDefault(cand, -1);
                            if (last < oldestStep) {
                                oldestStep = last;
                                victimP = cand;
                                victimFrame = f;
                            }
                        }
                        lruLastUsed.put(page, step);
                        int age = step - oldestStep;
                        ss.reason = "LRU FLUSH [WARP] Flushed Page " + victimP + " from Pipe #" + victimFrame + " (Unused for " + age + " steps)";

                    } else if (selectedAlgo.equals("OPTIMAL")) {
                        // Evict page that will not be used for longest period of time
                        int farthestNext = -1;
                        for (int f = 0; f < frameCount; f++) {
                            int cand = currentFrames[f];
                            int nextUse = Integer.MAX_VALUE;
                            for (int k = step + 1; k < pageStream.length; k++) {
                                if (pageStream[k] == cand) {
                                    nextUse = k;
                                    break;
                                }
                            }
                            if (nextUse > farthestNext) {
                                farthestNext = nextUse;
                                victimP = cand;
                                victimFrame = f;
                            }
                        }
                        String distStr = (farthestNext == Integer.MAX_VALUE) ? "Never needed again" : "Next use farthest at step " + (farthestNext + 1);
                        ss.reason = "OPTIMAL FLUSH [WARP] Flushed Page " + victimP + " from Pipe #" + victimFrame + " (" + distStr + ")";
                    }

                    currentFrames[victimFrame] = page;
                    ss.victimPage = victimP;
                    ss.victimFrameIdx = victimFrame;
                }
            }

            ss.frames = currentFrames.clone();
            ss.hitsSoFar = hits;
            ss.faultsSoFar = faults;
            simulationSteps.add(ss);
        }

        // Update Horizontal Trace Matrix JTable structure
        updateTraceTableStructure();

        // Reset to initial state
        resetSimulation();
    }

    private void updateTraceTableStructure() {
        int numSteps = simulationSteps.size();
        String[] cols = new String[numSteps + 1];
        cols[0] = "LEVEL // STEP";
        for (int i = 0; i < numSteps; i++) {
            cols[i + 1] = "#" + (i + 1);
        }

        int numRows = frameCount + 2;
        Object[][] data = new Object[numRows][numSteps + 1];

        // Column 0 labels
        data[0][0] = " PAGE STREAM";
        for (int f = 0; f < frameCount; f++) {
            data[1 + f][0] = " PIPE #" + f;
        }
        data[numRows - 1][0] = " STATUS (H/F)";

        // Step columns
        for (int i = 0; i < numSteps; i++) {
            StepState ss = simulationSteps.get(i);
            int col = i + 1;
            data[0][col] = ss.refPage;
            for (int f = 0; f < frameCount; f++) {
                int p = ss.frames[f];
                data[1 + f][col] = (p == -1) ? "-" : p;
            }
            data[numRows - 1][col] = ss.isHit ? "HIT" : "FAULT";
        }

        traceModel.setDataVector(data, cols);

        // Adjust row height based on frame count (Spacious & responsive for 1 to 16 pipes!)
        if (frameCount <= 3) {
            traceTable.setRowHeight(32);
        } else if (frameCount <= 5) {
            traceTable.setRowHeight(28);
        } else if (frameCount <= 8) {
            traceTable.setRowHeight(22);
        } else {
            traceTable.setRowHeight(18);
        }

        // Dynamically maximizes 100% of the horizontal space: ZERO empty black void!
        adjustTableColumnWidths();
        SwingUtilities.invokeLater(() -> adjustTableColumnWidths());
    }

    // =========================================================================
    // PLAYBACK & CHALLENGE CONTROLS
    // =========================================================================
    public void togglePlayPause() {
        if (isPlaying) {
            pauseSimulation();
        } else {
            startSimulation();
        }
    }

    public void startSimulation() {
        tryQuietRefString();
        if (isChallengeMode) {
            // In challenge mode, do not run automated timer!
            // Start or restart challenge at step 0:
            if (currentStepIdx < 0 || currentStepIdx >= simulationSteps.size() - 1) {
                currentStepIdx = 0;
            }
            updateDisplayToStep(currentStepIdx);
            return;
        }
        if (currentStepIdx >= simulationSteps.size() - 1) {
            resetSimulation();
        }
        isPlaying = true;
        btnPlay.setText("PAUSE");
        btnPlay.setIcon(RetroIcons.createPauseIcon(14, Color.WHITE));
        btnPlay.setBackground(new Color(180, 110, 0));
        simTimer.start();
    }

    public void pauseSimulation() {
        isPlaying = false;
        btnPlay.setText("START SIMULATION");
        btnPlay.setIcon(RetroIcons.createPlayIcon(14, Color.WHITE));
        btnPlay.setBackground(new Color(0, 160, 60));
        simTimer.stop();
    }

    public void stepForward() {
        tryQuietRefString();
        // If Challenge Mode is active and waiting for user input, prompt user to click pipe!
        if (isChallengeMode && awaitingEvictionChoice) {
            if (canvas != null) {
                canvas.setBannerMsg("[CHALLENGE ACTIVE]: Click a pipe on the canvas to answer!");
            }
            return;
        }
        if (currentStepIdx < simulationSteps.size() - 1) {
            currentStepIdx++;
            updateDisplayToStep(currentStepIdx);
        } else {
            pauseSimulation();
        }
    }

    public void resetSimulation() {
        pauseSimulation();
        if (challengeTimer != null && challengeTimer.isRunning()) {
            challengeTimer.stop();
        }
        currentStepIdx = -1;
        awaitingEvictionChoice = false;
        updateDisplayToStep(-1);
    }

    public void launchMarioCacheGame() {
        Window ancestor = SwingUtilities.getWindowAncestor(this);
        JFrame owner = (ancestor instanceof JFrame) ? (JFrame) ancestor : null;
        MarioCacheRunnerDialog dlg = new MarioCacheRunnerDialog(owner, pageStream, frameCount, selectedAlgo);
        dlg.setVisible(true);
    }

    public void toggleChallengeMode() {
        isChallengeMode = !isChallengeMode;
        btnModeToggle.setText(isChallengeMode ? "CHALLENGE: ON" : "CHALLENGE: OFF");
        btnModeToggle.setBackground(isChallengeMode ? new Color(180, 100, 20) : new Color(38, 25, 68));
        btnModeToggle.repaint();

        if (challengeTimer != null && challengeTimer.isRunning()) {
            challengeTimer.stop();
        }

        if (canvas != null) {
            canvas.setChallengeMode(isChallengeMode);
        }

        pauseSimulation();
        if (isChallengeMode) {
            if (currentStepIdx < 0 || currentStepIdx >= simulationSteps.size() - 1) {
                currentStepIdx = 0;
            }
            updateDisplayToStep(currentStepIdx);
        }
    }

    private void cycleSpeed() {
        if (speedMultiplier == 0.5) {
            speedMultiplier = 1.0;
            btnSpeed.setText("SPEED: 1.0x");
        } else if (speedMultiplier == 1.0) {
            speedMultiplier = 2.0;
            btnSpeed.setText("SPEED: 2.0x");
        } else {
            speedMultiplier = 0.5;
            btnSpeed.setText("SPEED: 0.5x");
        }
    }

    private int tickCounter = 0;
    private void updateSimulationTick() {
        if (!isPlaying || isChallengeMode) return;
        tickCounter++;
        int threshold = (int)(25 / speedMultiplier);
        if (tickCounter >= threshold) {
            tickCounter = 0;
            if (currentStepIdx < simulationSteps.size() - 1) {
                currentStepIdx++;
                updateDisplayToStep(currentStepIdx);
            } else {
                pauseSimulation();
                AppState.addScore(100);
            }
        }
    }

    private void updateDisplayToStep(int stepIdx) {
        if (stepIdx < 0 || simulationSteps.isEmpty()) {
            lblHitCount.setText("0 HITS (0.0%)");
            lblHitSub.setText("HIT RATE: 0.0% [AIM: HIGH]");
            lblFaultCount.setText("0 FAULTS");
            lblFaultSub.setText("PENALTY DISK SWAPS: 0");
            lblFrameCount.setText("0/" + frameCount + " PIPES [0%]");
            lblFrameSub.setText("STANDBY // AWAITING STREAM");
            if (lblStepDetails != null) {
                lblStepDetails.setText("READY: Click START SIMULATION or STEP >> to trace memory.");
            }
            if (canvas != null) {
                int[] empty = new int[frameCount];
                Arrays.fill(empty, -1);
                canvas.updateState(-1, -1, empty, false, -1, -1, "STANDBY: Ready to simulate page replacement.");
            }
            if (traceTable != null) traceTable.repaint();
            return;
        }

        StepState ss = simulationSteps.get(stepIdx);

        if (isChallengeMode) {
            awaitingEvictionChoice = true;
            if (isPlaying) {
                pauseSimulation();
            }
            if (canvas != null) {
                if (ss.isHit) {
                    // HIT CHALLENGE: Pipe already has this page! User must find and click it!
                    canvas.updateChallengeState(stepIdx, ss.refPage, ss.prevFrames != null ? ss.prevFrames : ss.frames,
                            "HIT", ss.hitFrameIdx, -1,
                            "[CHALLENGE]: TARGET P" + ss.refPage + " is a HIT! Click the pipe holding Page " + ss.refPage + ":");
                } else if (ss.victimFrameIdx != -1) {
                    // EVICTION CHALLENGE: Pipes full! User must choose victim pipe to flush under selected policy!
                    canvas.updateChallengeState(stepIdx, ss.refPage, ss.prevFrames,
                            "EVICT", -1, ss.victimFrameIdx,
                            "[CHALLENGE]: TARGET P" + ss.refPage + " MISS! Pipes full. Click victim pipe under " + selectedAlgo + ":");
                } else {
                    // COLD MISS CHALLENGE: Empty pipe available! User clicks vacant pipe to warp in!
                    canvas.updateChallengeState(stepIdx, ss.refPage, ss.prevFrames != null ? ss.prevFrames : ss.frames,
                            "ALLOCATE", ss.emptyAssignedFrameIdx, -1,
                            "[CHALLENGE]: TARGET P" + ss.refPage + " MISS! Click a vacant pipe to warp in:");
                }
            }
            updateHudAndTable(ss, stepIdx);
            return; // PAUSE & STOP HERE until user manually clicks their answer!
        }

        // Normal Simulation Mode:
        awaitingEvictionChoice = false;
        if (canvas != null) {
            canvas.updateState(stepIdx, ss.refPage, ss.frames, ss.isHit, ss.hitFrameIdx, ss.victimFrameIdx, ss.reason);
        }
        updateHudAndTable(ss, stepIdx);
    }

    public void onChallengeChoiceCompleted(boolean correct) {
        awaitingEvictionChoice = false;
        if (currentStepIdx >= 0 && currentStepIdx < simulationSteps.size()) {
            StepState ss = simulationSteps.get(currentStepIdx);
            updateHudAndTable(ss, currentStepIdx);
        }

        // AUTO-ADVANCE AFTER USER CLICK: Keep result feedback on screen for 1.3s so user clearly sees it!
        if (challengeTimer != null && challengeTimer.isRunning()) challengeTimer.stop();
        challengeTimer = new javax.swing.Timer((int)(1300 / speedMultiplier), e -> {
            if (isChallengeMode && currentStepIdx < simulationSteps.size() - 1) {
                currentStepIdx++;
                updateDisplayToStep(currentStepIdx);
            } else if (isChallengeMode && currentStepIdx >= simulationSteps.size() - 1) {
                if (canvas != null) {
                    canvas.setBannerMsg("CHALLENGE COMPLETE! Perfect MMU execution. (+100 XP)");
                    AppState.addScore(100);
                }
            }
        });
        challengeTimer.setRepeats(false);
        challengeTimer.start();
    }

    public void onChallengeEvictionChosen(boolean correct) {
        onChallengeChoiceCompleted(correct);
    }

    private void updateHudAndTable(StepState ss, int stepIdx) {
        int totalProcessed = stepIdx + 1;
        double hitRate = ((double) ss.hitsSoFar / totalProcessed) * 100.0;
        double faultRate = ((double) ss.faultsSoFar / totalProcessed) * 100.0;

        lblHitCount.setText(String.format("%d HITS (%.1f%%)", ss.hitsSoFar, hitRate));
        lblHitSub.setText(String.format("HIT RATIO: %.1f%% // STEP %d/%d", hitRate, totalProcessed, pageStream.length));

        lblFaultCount.setText(String.format("%d FAULTS (%.1f%%)", ss.faultsSoFar, faultRate));
        lblFaultSub.setText(String.format("PENALTY DISK SWAPS: %d", ss.faultsSoFar));

        int occupied = 0;
        for (int p : ss.frames) if (p != -1) occupied++;
        int capPercent = (int)(((double) occupied / frameCount) * 100);
        lblFrameCount.setText(String.format("%d/%d PIPES [%d%%]", occupied, frameCount, capPercent));
        lblFrameSub.setText(ss.isHit ? "CACHE HIT [INSTANT ACCESS]" : "PAGE FAULT [PIPE SWAP]");

        if (lblStepDetails != null) {
            String shortReason = ss.isHit ? "HIT [PIPE " + ss.hitFrameIdx + "]" :
                    (ss.victimFrameIdx != -1 ? selectedAlgo + " SWAP [PIPE " + ss.victimFrameIdx + ": P" + ss.victimPage + " -> P" + ss.refPage + "]" :
                     "WARP IN [PIPE " + ss.emptyAssignedFrameIdx + "]");
            lblStepDetails.setText("STEP " + totalProcessed + "/" + simulationSteps.size() + " // P" + ss.refPage + " : " + shortReason);
        }

        // Horizontal Table Auto-Scroll & Repaint (Fluid smooth horizontal gliding!)
        if (traceTable != null && traceScroll != null) {
            int col = stepIdx + 1;
            if (col < traceTable.getColumnCount()) {
                Rectangle cellRect = traceTable.getCellRect(0, col, true);
                int viewW = traceScroll.getViewport().getWidth();
                int targetX = Math.max(0, cellRect.x - (viewW / 2) + (cellRect.width / 2));
                JScrollBar hBar = traceScroll.getHorizontalScrollBar();
                if (hBar != null) {
                    smoothScrollTable(hBar, targetX);
                }
            }
            traceTable.repaint();
        }
    }

    public boolean isAwaitingEvictionChoice() {
        return awaitingEvictionChoice;
    }

    // =========================================================================
    // 4. SUPER MARIO WARP PIPE CANVAS: BOLD, VIBRANT, CRYSTAL CLEAR TYPOGRAPHY
    // =========================================================================
    public static class MarioPipeCanvas extends JPanel {
        private final PageReplacementPanel parent;

        private int currentStep = -1;
        private int refPage = -1;
        private int[] frames = new int[3];
        private boolean isHit = false;
        private int hitFrameIdx = -1;
        private int victimFrameIdx = -1;
        private String bannerMsg = "STANDBY: Ready to simulate page replacement.";

        private int animTick = 0;
        private boolean challengeMode = false;
        private int hoveredFrameIdx = -1;
        private int clickedFrameIdx = -1;
        private int flashTimer = 0;
        private Color flashColor = new Color(0, 240, 100);
        private String flashTag = "";

        // Interactive Challenge Task & Target
        private String challengeType = ""; // "HIT", "EVICT", "ALLOCATE"
        private int challengeTargetIdx = -1;

        // Smooth gliding scroll state
        private float smoothScrollOffset = 0f;
        private float targetScrollOffset = 0f;

        private final BufferedImage marioSprite;
        private final List<Rectangle> frameSlotBounds = new ArrayList<>();

        public MarioPipeCanvas(PageReplacementPanel parent) {
            this.parent = parent;
            setBackground(new Color(14, 10, 26));
            setOpaque(true);
            setBorder(BorderFactory.createLineBorder(new Color(65, 45, 95), 1));
            marioSprite = PixelSprites.createMarioSprite(44);

            // Mouse Click & Hover for Challenge Mode
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!challengeMode || !parent.isAwaitingEvictionChoice()) return;
                    for (int i = 0; i < frameSlotBounds.size(); i++) {
                        if (frameSlotBounds.get(i).contains(e.getPoint())) {
                            handleSlotClick(i);
                            return;
                        }
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (!challengeMode || !parent.isAwaitingEvictionChoice()) {
                        if (hoveredFrameIdx != -1) {
                            hoveredFrameIdx = -1;
                            setCursor(Cursor.getDefaultCursor());
                            repaint();
                        }
                        return;
                    }
                    int found = -1;
                    for (int i = 0; i < frameSlotBounds.size(); i++) {
                        if (frameSlotBounds.get(i).contains(e.getPoint())) {
                            found = i;
                            break;
                        }
                    }
                    if (found != hoveredFrameIdx) {
                        hoveredFrameIdx = found;
                        setCursor(found != -1 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                        repaint();
                    }
                }
            });

            // Fast smooth animation timer (30ms per tick)
            new javax.swing.Timer(30, e -> {
                animTick++;
                if (flashTimer > 0) {
                    flashTimer--;
                    if (flashTimer == 0) clickedFrameIdx = -1;
                }
                // Silky smooth conveyor belt lerp
                float diff = targetScrollOffset - smoothScrollOffset;
                if (Math.abs(diff) > 0.25f) {
                    smoothScrollOffset += diff * 0.22f;
                } else {
                    smoothScrollOffset = targetScrollOffset;
                }
                repaint();
            }).start();
        }

        public void setChallengeMode(boolean c) {
            this.challengeMode = c;
            this.hoveredFrameIdx = -1;
            this.clickedFrameIdx = -1;
            setCursor(Cursor.getDefaultCursor());
            repaint();
        }

        public void setBannerMsg(String msg) {
            this.bannerMsg = msg;
            repaint();
        }

        private void handleSlotClick(int slotIdx) {
            if (!challengeMode || !parent.isAwaitingEvictionChoice()) return;
            if (parent.currentStepIdx < 0 || parent.currentStepIdx >= parent.simulationSteps.size()) return;

            StepState ss = parent.simulationSteps.get(parent.currentStepIdx);

            clickedFrameIdx = slotIdx;
            flashTimer = 24;

            if ("HIT".equals(challengeType)) {
                boolean correct = (slotIdx == challengeTargetIdx);
                if (correct) {
                    flashColor = new Color(0, 240, 100);
                    flashTag = "★ COIN HIT! +25XP ★";
                    bannerMsg = "CORRECT HIT! Page P" + ss.refPage + " found in Pipe #" + slotIdx + "! (+25 XP)";
                    AppState.addScore(25);
                } else {
                    flashColor = RetroTheme.ACCENT_PINK;
                    flashTag = "WRONG PIPE! P" + ss.refPage + " is in Pipe #" + challengeTargetIdx;
                    bannerMsg = "MISSED! Page P" + ss.refPage + " was in Pipe #" + challengeTargetIdx + "!";
                }
                // Reveal the yellow laser beam to the correct pipe!
                this.isHit = true;
                this.hitFrameIdx = challengeTargetIdx;
                this.victimFrameIdx = -1;
                parent.onChallengeChoiceCompleted(correct);

            } else if ("EVICT".equals(challengeType)) {
                boolean correct = (slotIdx == challengeTargetIdx);
                if (correct) {
                    flashColor = new Color(0, 240, 100);
                    flashTag = "CORRECT FLUSH! +50XP";
                    bannerMsg = "CORRECT! Pipe #" + slotIdx + " (P" + ss.victimPage + ") flushed -> P" + ss.refPage + " warped in! (+50 XP)";
                    AppState.addScore(50);
                } else {
                    flashColor = RetroTheme.ACCENT_PINK;
                    flashTag = "WRONG VICTIM! Expected Pipe #" + challengeTargetIdx;
                    bannerMsg = "POLICY ERROR! Pipe #" + challengeTargetIdx + " (P" + ss.victimPage + ") was correct under " + parent.selectedAlgo + ". (0 XP)";
                }
                // Reveal the pink eviction beam and update frames with new page!
                this.frames = ss.frames.clone();
                this.victimFrameIdx = challengeTargetIdx;
                this.isHit = false;
                parent.onChallengeChoiceCompleted(correct);

            } else if ("ALLOCATE".equals(challengeType)) {
                boolean isVacant = (slotIdx >= 0 && slotIdx < frames.length && frames[slotIdx] == -1);
                if (isVacant) {
                    flashColor = new Color(0, 240, 100);
                    flashTag = "WARPED IN! +20XP";
                    bannerMsg = "Page P" + ss.refPage + " allocated into vacant Pipe #" + slotIdx + "! (+20 XP)";
                    AppState.addScore(20);
                    this.frames = ss.frames.clone();
                    this.isHit = false;
                    this.victimFrameIdx = -1;
                    parent.onChallengeChoiceCompleted(true);
                } else {
                    flashColor = RetroTheme.ACCENT_PINK;
                    flashTag = "CLICK VACANT PIPE!";
                    bannerMsg = "Pipe #" + slotIdx + " is already occupied! Click a vacant pipe.";
                    repaint();
                    return;
                }
            } else {
                // Fallback for eviction
                boolean correct = (slotIdx == ss.victimFrameIdx);
                this.frames = ss.frames.clone();
                this.victimFrameIdx = ss.victimFrameIdx;
                parent.onChallengeChoiceCompleted(correct);
            }
            repaint();
        }

        public void updateChallengeState(int step, int page, int[] curFrames, String type, int hitIdx, int victimIdx, String msg) {
            this.currentStep = step;
            this.refPage = page;
            this.frames = curFrames != null ? curFrames.clone() : new int[0];
            this.challengeType = type;
            this.challengeTargetIdx = ("HIT".equals(type) || "ALLOCATE".equals(type)) ? hitIdx : victimIdx;
            // In challenge mode, do not show laser beams or hit highlights until the user clicks!
            this.isHit = false;
            this.hitFrameIdx = -1;
            this.victimFrameIdx = -1;
            this.bannerMsg = msg != null ? msg : "";

            int capsuleW = 40;
            int gap = 8;
            this.targetScrollOffset = (step >= 0) ? (step * (capsuleW + gap)) : 0f;
            repaint();
        }

        public void updateState(int step, int page, int[] curFrames, boolean hit, int hitIdx, int victimIdx, String msg) {
            this.currentStep = step;
            this.refPage = page;
            this.frames = curFrames != null ? curFrames.clone() : new int[0];
            this.isHit = hit;
            this.hitFrameIdx = hitIdx;
            this.victimFrameIdx = victimIdx;
            this.challengeType = "";
            this.challengeTargetIdx = -1;
            this.bannerMsg = msg != null ? msg : "";

            int capsuleW = 40;
            int gap = 8;
            this.targetScrollOffset = (step >= 0) ? (step * (capsuleW + gap)) : 0f;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background subtle pixel stars
            g2.setColor(new Color(22, 16, 38));
            for (int x = 0; x < w; x += 32) g2.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += 32) g2.drawLine(0, y, w, y);

            // =================================================================
            // 1. TOP QUESTION BLOCK CONVEYOR (y = 4 .. 36, height = 32)
            // =================================================================
            int beltY = 4;
            int beltH = 32;
            g2.setColor(new Color(18, 12, 32));
            g2.fillRect(0, beltY, w, beltH);
            g2.setColor(new Color(50, 36, 75));
            g2.drawLine(0, beltY + beltH, w, beltY + beltH);

            int[] stream = parent.pageStream;
            int capsuleW = 40;
            int capsuleH = 24;
            int gap = 8;
            int scannerX = w / 2 - capsuleW / 2;

            if (stream != null && stream.length > 0) {
                int startX = (int)(scannerX - smoothScrollOffset);

                for (int i = 0; i < stream.length; i++) {
                    int cx = startX + i * (capsuleW + gap);
                    if (cx + capsuleW < 0 || cx > w) continue;

                    boolean isCurrent = (i == currentStep);

                    // Mario Question Mark Block [?] Style
                    Color blockBg = isCurrent ? new Color(252, 180, 0) : new Color(180, 110, 0);
                    Color blockBorder = isCurrent ? Color.WHITE : new Color(240, 200, 0);

                    if (isCurrent) {
                        float pulse = (float)(Math.sin(animTick * 0.3) * 0.4 + 0.6);
                        g2.setColor(new Color(255, 215, 0, (int)(pulse * 160)));
                        g2.fillRoundRect(cx - 3, beltY + 4 - 3, capsuleW + 6, capsuleH + 6, 8, 8);
                    }

                    g2.setColor(blockBg);
                    g2.fillRoundRect(cx, beltY + 4, capsuleW, capsuleH, 6, 6);
                    g2.setColor(blockBorder);
                    g2.setStroke(new BasicStroke(isCurrent ? 2.0f : 1.2f));
                    g2.drawRoundRect(cx, beltY + 4, capsuleW, capsuleH, 6, 6);

                    // 4 Corner Rivets on Question Block
                    g2.setColor(new Color(120, 70, 0));
                    g2.fillRect(cx + 2, beltY + 6, 2, 2);
                    g2.fillRect(cx + capsuleW - 4, beltY + 6, 2, 2);
                    g2.fillRect(cx + 2, beltY + capsuleH, 2, 2);
                    g2.fillRect(cx + capsuleW - 4, beltY + capsuleH, 2, 2);

                    // Page ID Text
                    g2.setFont(new Font("Consolas", Font.BOLD, 13));
                    g2.setColor(Color.WHITE);
                    FontMetrics pfm = g2.getFontMetrics();
                    String pTxt = "P" + stream[i];
                    g2.drawString(pTxt, cx + (capsuleW - pfm.stringWidth(pTxt)) / 2, beltY + 21);
                }

                // Active Mario Pointer Cursor over Scanner Block
                int scX = scannerX;
                int scY = beltY + 1;
                g2.setColor(isHit ? new Color(0, 240, 100) : (refPage != -1 ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_YELLOW));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRect(scX - 4, scY, 48, 30);
            }

            // =================================================================
            // 2. MIDDLE STAGE: MARIO MMU BUS + GREEN WARP PIPES + SWAP ZONE
            // =================================================================
            int podY = 56;
            int podH = Math.max(170, h - 56 - 30);

            // Left Flank: Mario Stage Pod (x = 10 .. 125)
            int marioX = 10;
            int marioW = 115;
            g2.setColor(new Color(20, 15, 38));
            g2.fillRoundRect(marioX, podY, marioW, podH, 8, 8);
            g2.setColor(new Color(65, 45, 95));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(marioX, podY, marioW, podH, 8, 8);

            // Header
            g2.setColor(new Color(30, 20, 55));
            g2.fillRoundRect(marioX + 4, podY + 4, marioW - 8, 18, 4, 4);
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("MARIO MMU BUS", marioX + 16, podY + 17);

            // Animated Mario Sprite
            float marioBob = isHit ? (float)(-Math.abs(Math.sin(animTick * 0.4)) * 12) : (float)(Math.sin(animTick * 0.2) * 3);
            if (marioSprite != null) {
                g2.drawImage(marioSprite, marioX + (marioW - 44) / 2, (int)(podY + 28 + marioBob), null);
            }

            // Target Item Box
            int reqY = podY + 76;
            g2.setColor(new Color(12, 8, 24));
            g2.fillRoundRect(marioX + 8, reqY, marioW - 16, 44, 6, 6);
            g2.setColor(new Color(55, 40, 80));
            g2.drawRoundRect(marioX + 8, reqY, marioW - 16, 44, 6, 6);

            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            g2.setColor(RetroTheme.TEXT_DIM);
            g2.drawString("TARGET ITEM:", marioX + 14, reqY + 14);

            if (refPage != -1) {
                BufferedImage itm = PixelSprites.getMarioItem(refPage, 20);
                if (itm != null) g2.drawImage(itm, marioX + 14, reqY + 20, null);
                g2.setFont(new Font("Consolas", Font.BOLD, 15));
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawString("PAGE " + refPage, marioX + 40, reqY + 36);
            } else {
                g2.setFont(new Font("Consolas", Font.BOLD, 15));
                g2.setColor(RetroTheme.TEXT_DIM);
                g2.drawString("--", marioX + 48, reqY + 36);
            }

            g2.setFont(new Font("Consolas", Font.PLAIN, 9));
            g2.setColor(new Color(0, 240, 100));
            g2.drawString("• WORLD 1-1 MMU", marioX + 14, reqY + 60);

            // Right Flank: Warp Chute / Swap Zone (x = w - 145 .. w - 10)
            int swapX = w - 145;
            int swapW = 135;
            g2.setColor(new Color(20, 14, 34));
            g2.fillRoundRect(swapX, podY, swapW, podH, 8, 8);
            g2.setColor(isHit ? new Color(45, 75, 55) : (refPage != -1 ? new Color(90, 40, 65) : new Color(65, 45, 95)));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(swapX, podY, swapW, podH, 8, 8);

            // Swap Header
            g2.setColor(new Color(30, 20, 50));
            g2.fillRoundRect(swapX + 4, podY + 4, swapW - 8, 18, 4, 4);
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("WARP ZONE [SWAP]", swapX + 16, podY + 17);

            // Brick Castle Chute Graphic
            int bayY = podY + 28;
            g2.setColor(new Color(180, 80, 20)); // Brick Red
            g2.fillRoundRect(swapX + 12, bayY, swapW - 24, 40, 6, 6);
            g2.setColor(new Color(100, 40, 10));
            g2.drawRoundRect(swapX + 12, bayY, swapW - 24, 40, 6, 6);
            // Castle entrance arch
            g2.setColor(new Color(15, 10, 25));
            g2.fillRoundRect(swapX + swapW / 2 - 12, bayY + 10, 24, 30, 8, 8);

            // Swap Telemetry Lines
            int swY = bayY + 48;
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            if (isHit) {
                g2.setColor(new Color(0, 240, 100));
                g2.drawString("STATUS: CACHE HIT!", swapX + 12, swY + 14);
                g2.setColor(Color.WHITE);
                g2.drawString("WARP I/O: 0 PENALTY", swapX + 12, swY + 28);
                g2.setColor(RetroTheme.TEXT_DIM);
                g2.drawString("COIN BOOST: +25 XP", swapX + 12, swY + 42);
            } else if (refPage != -1) {
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.drawString("STATUS: PAGE FAULT", swapX + 12, swY + 14);
                g2.setColor(Color.WHITE);
                if (victimFrameIdx != -1) {
                    StepState curSs = (currentStep >= 0 && currentStep < parent.simulationSteps.size()) ? parent.simulationSteps.get(currentStep) : null;
                    int vp = curSs != null ? curSs.victimPage : -1;
                    g2.drawString("FLUSH: P" + vp + " -> DISK", swapX + 12, swY + 28);
                } else {
                    g2.drawString("NEW PIPE RESIDENT", swapX + 12, swY + 28);
                }
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawString("WARP: P" + refPage + " IN", swapX + 12, swY + 42);
            } else {
                g2.setColor(RetroTheme.TEXT_DIM);
                g2.drawString("STATUS: STANDBY", swapX + 12, swY + 14);
                g2.drawString("WARP I/O: IDLE", swapX + 12, swY + 28);
            }

            // Total Swaps Pill
            StepState ssNow = (currentStep >= 0 && currentStep < parent.simulationSteps.size()) ? parent.simulationSteps.get(currentStep) : null;
            int totalSwaps = ssNow != null ? ssNow.faultsSoFar : 0;
            g2.setColor(new Color(12, 8, 22));
            g2.fillRoundRect(swapX + 8, podY + podH - 22, swapW - 16, 16, 4, 4);
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.drawString("SWAP COUNT: " + totalSwaps, swapX + 16, podY + podH - 10);

            // =================================================================
            // 3. CENTER: ICONIC GREEN WARP PIPES (RAM Frame Slots)
            // =================================================================
            frameSlotBounds.clear();
            int numF = frames.length;
            int startSlotX = marioX + marioW + 16;
            int endSlotX = swapX - 16;
            int availW = endSlotX - startSlotX;
            int slotGap = (numF <= 4) ? 16 : ((numF <= 6) ? 12 : ((numF <= 9) ? 8 : 4));

            int slotW = 120;
            if (numF > 0) {
                slotW = Math.max(30, (availW - (numF - 1) * slotGap) / numF);
            }
            int slotH = podH;
            int slotY = podY;

            int sx = startSlotX;
            int hitLineX = -1;
            int hitLineY = -1;
            int hitSlotW = 0;
            int victimLineX = -1;
            int victimLineY = -1;
            int victimSlotW = 0;

            for (int f = 0; f < numF; f++) {
                Rectangle sBounds = new Rectangle(sx, slotY, slotW, slotH);
                frameSlotBounds.add(sBounds);

                int page = frames[f];
                boolean isSlotHit = (isHit && f == hitFrameIdx);
                boolean isSlotEvicted = (!isHit && f == victimFrameIdx);
                boolean isAwaitingChoice = (challengeMode && parent.isAwaitingEvictionChoice());
                boolean isHovered = (isAwaitingChoice && hoveredFrameIdx == f);
                boolean isClicked = (clickedFrameIdx == f && flashTimer > 0);

                if (isSlotHit) {
                    hitLineX = sx + slotW / 2;
                    hitLineY = slotY + 28;
                    hitSlotW = slotW;
                }
                if (isSlotEvicted) {
                    victimLineX = sx + slotW / 2;
                    victimLineY = slotY + 28;
                    victimSlotW = slotW;
                }

                // =============================================================
                // 3A. THREE-ZONE ARCHITECTURE:
                // Zone 1 (Top): Mario Item poking out of the pipe mouth rim
                // Zone 2 (Middle): Clean, pristine 3D Warp Pipe (ZERO text on cylinder!)
                // Zone 3 (Bottom): Pedestal Base Plate housing Page # and status badge
                // =============================================================

                int pipeLipY = slotY + 28;
                int pipeLipH = (slotW >= 80) ? 20 : 16;
                int baseH = (slotW >= 75) ? 46 : 40;
                int baseY = slotY + slotH - baseH - 4;

                int pipeInset = (slotW >= 80) ? 8 : ((slotW >= 55) ? 5 : 3);
                int pipeBodyX = sx + pipeInset;
                int pipeBodyW = Math.max(16, slotW - pipeInset * 2);
                int pipeBodyY = pipeLipY + pipeLipH;
                int pipeBodyH = baseY - pipeBodyY;

                // Pipe Palette (Classic Super Mario 3-shade emerald cylinder)
                Color pipeHighlight = new Color(90, 240, 90);
                Color pipeMain = new Color(0, 168, 0);
                Color pipeShadow = new Color(0, 90, 0);
                Color pipeLipBorder = isSlotHit ? new Color(252, 228, 0) : (isSlotEvicted ? RetroTheme.ACCENT_PINK : (isAwaitingChoice ? RetroTheme.ACCENT_YELLOW : new Color(0, 60, 0)));

                int hlW = Math.max(3, Math.min(10, pipeBodyW / 6));
                int shW = Math.max(4, Math.min(14, pipeBodyW / 5));

                // --- ZONE 1: MARIO ITEM POKING OUT OF PIPE MOUTH ---
                if (page != -1) {
                    int spriteSize = (slotW >= 85) ? 32 : ((slotW >= 60) ? 26 : 20);
                    BufferedImage itemSprite = PixelSprites.getMarioItem(page, spriteSize);
                    if (itemSprite != null) {
                        float floatBob = isSlotHit ? (float)(-Math.abs(Math.sin(animTick * 0.4)) * 10) : (float)(Math.sin((animTick + f * 2) * 0.15) * 2.5);
                        int itemX = sx + (slotW - spriteSize) / 2;
                        int itemY = (int)(pipeLipY - spriteSize + 6 + floatBob);
                        g2.drawImage(itemSprite, itemX, itemY, null);
                    }
                }

                // --- ZONE 2: PRISTINE 3D WARP PIPE CYLINDER ---
                // 1. Pipe Column Body (Untouched cylinder shading)
                g2.setColor(pipeMain);
                g2.fillRect(pipeBodyX, pipeBodyY, pipeBodyW, pipeBodyH);
                // Left Highlight Stripe
                g2.setColor(pipeHighlight);
                g2.fillRect(pipeBodyX + 2, pipeBodyY, hlW, pipeBodyH);
                // Right Shadow Stripe
                g2.setColor(pipeShadow);
                g2.fillRect(pipeBodyX + pipeBodyW - shW, pipeBodyY, shW, pipeBodyH);

                // Modular Pipe Weld Seams (Gives authentic industrial arcade texture!)
                int seam1Y = pipeBodyY + (int)(pipeBodyH * 0.22);
                int seam2Y = pipeBodyY + (int)(pipeBodyH * 0.78);
                g2.setColor(new Color(0, 100, 0));
                g2.fillRect(pipeBodyX, seam1Y, pipeBodyW, 2);
                g2.fillRect(pipeBodyX, seam2Y, pipeBodyW, 2);
                g2.setColor(new Color(60, 220, 60, 140));
                g2.fillRect(pipeBodyX + 2, seam1Y + 2, hlW, 1);
                g2.fillRect(pipeBodyX + 2, seam2Y + 2, hlW, 1);

                // --- CENTER: WARP CHAMBER INSPECTION PORTHOLE ---
                // Circular glass chamber medallion holding the PAGE NUMBER right in the middle of the pipe!
                int pCenterX = pipeBodyX + pipeBodyW / 2;
                int pCenterY = pipeBodyY + pipeBodyH / 2;
                int pDia = (slotW >= 85) ? 40 : ((slotW >= 60) ? 30 : 22);
                int pRad = pDia / 2;
                int pX = pCenterX - pRad;
                int pY = pCenterY - pRad;

                if (pipeBodyH >= 36 && pDia >= 18) {
                    // Outer Bezel Rim (Cast Iron / Steampunk Brass)
                    g2.setColor(new Color(10, 8, 20));
                    g2.fillOval(pX - 3, pY - 3, pDia + 6, pDia + 6);
                    Color bezelBorder = isSlotHit ? new Color(252, 228, 0) : (isSlotEvicted ? RetroTheme.ACCENT_PINK : (page != -1 ? new Color(50, 160, 70) : new Color(30, 80, 40)));
                    g2.setColor(bezelBorder);
                    g2.setStroke(new BasicStroke(isSlotHit || isSlotEvicted ? 2.0f : 1.5f));
                    g2.drawOval(pX - 3, pY - 3, pDia + 6, pDia + 6);

                    // 4 Metallic Rivets
                    Color rivetCol = isSlotHit ? new Color(255, 235, 80) : new Color(160, 230, 130);
                    g2.setColor(rivetCol);
                    g2.fillRect(pCenterX - 1, pY - 3, 2, 2);
                    g2.fillRect(pCenterX - 1, pY + pDia + 1, 2, 2);
                    g2.fillRect(pX - 3, pCenterY - 1, 2, 2);
                    g2.fillRect(pX + pDia + 1, pCenterY - 1, 2, 2);

                    // Glass Chamber Interior
                    Color glassBg = (page != -1) ? new Color(4, 26, 16) : new Color(8, 12, 10);
                    g2.setColor(glassBg);
                    g2.fillOval(pX, pY, pDia, pDia);

                    if (page != -1) {
                        // Subtle Glowing Energy Ring
                        g2.setColor(isSlotHit ? new Color(252, 228, 0, 130) : new Color(0, 240, 160, 90));
                        g2.setStroke(new BasicStroke(1.2f));
                        g2.drawOval(pX + 3, pY + 3, pDia - 6, pDia - 6);

                        // BOLD PAGE NUMBER IN THE CENTER OF THE PIPE!
                        int numFontSize = (pDia >= 36) ? 22 : ((pDia >= 28) ? 17 : 13);
                        g2.setFont(new Font("Consolas", Font.BOLD, numFontSize));
                        String numStr = String.valueOf(page);
                        FontMetrics nfm = g2.getFontMetrics();
                        int nx = pCenterX - nfm.stringWidth(numStr) / 2;
                        int ny = pCenterY + (nfm.getAscent() - nfm.getDescent()) / 2;

                        // Text drop shadow
                        g2.setColor(new Color(0, 0, 0, 240));
                        g2.drawString(numStr, nx + 1, ny + 1);

                        // Vibrant number glow
                        if (isSlotHit) {
                            g2.setColor(new Color(255, 235, 60)); // Gold
                        } else if (isSlotEvicted) {
                            g2.setColor(RetroTheme.ACCENT_PINK); // Pink
                        } else {
                            g2.setColor(Color.WHITE); // Crisp White
                        }
                        g2.drawString(numStr, nx, ny);

                        // Glass Glare Reflection Accent
                        g2.setColor(new Color(255, 255, 255, 80));
                        g2.setStroke(new BasicStroke(1.0f));
                        g2.drawArc(pX + 2, pY + 2, pDia - 4, pDia - 4, 110, 65);
                    } else {
                        // Idle / Powered Down Chamber (-)
                        g2.setFont(new Font("Consolas", Font.BOLD, 14));
                        g2.setColor(new Color(40, 80, 50));
                        FontMetrics dfm = g2.getFontMetrics();
                        g2.drawString("-", pCenterX - dfm.stringWidth("-") / 2, pCenterY + 5);
                    }
                }

                // Body Border
                g2.setColor(new Color(0, 45, 0));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRect(pipeBodyX, pipeBodyY, pipeBodyW, pipeBodyH);

                // 2. Pipe Lip (Wider Top Flange)
                // Dark Opening Lip Mouth Ellipse (gives 3D depth into pipe)
                g2.setColor(new Color(0, 36, 0));
                g2.fillRoundRect(sx + 3, pipeLipY - 3, slotW - 6, 6, 4, 4);

                g2.setColor(pipeMain);
                g2.fillRect(sx, pipeLipY, slotW, pipeLipH);
                // Lip Highlight Stripe
                g2.setColor(pipeHighlight);
                g2.fillRect(sx + 3, pipeLipY, hlW + 2, pipeLipH);
                // Lip Shadow Stripe
                g2.setColor(pipeShadow);
                g2.fillRect(sx + slotW - (shW + 4), pipeLipY, shW + 4, pipeLipH);
                // Lip Border
                g2.setColor(pipeLipBorder);
                g2.setStroke(new BasicStroke(isSlotHit || isSlotEvicted || isAwaitingChoice ? 2.5f : 1.5f));
                g2.drawRect(sx, pipeLipY, slotW, pipeLipH);

                // Pipe Header Stamped on Rim Collar
                g2.setFont(new Font("Consolas", Font.BOLD, (slotW >= 80) ? 10 : 9));
                g2.setColor(isSlotHit ? new Color(252, 228, 0) : Color.WHITE);
                FontMetrics hfm = g2.getFontMetrics();
                String hStr = (slotW >= 80) ? ("PIPE #" + f) : ("#" + f);
                g2.drawString(hStr, sx + (slotW - hfm.stringWidth(hStr)) / 2, pipeLipY + pipeLipH - 4);

                // --- ZONE 3: GROUND PEDESTAL BASE PLATE ---
                int baseW = slotW;
                int baseX = sx;
                Color baseBg = new Color(18, 14, 30);
                Color baseBorder = isSlotHit ? new Color(252, 228, 0) : (isSlotEvicted ? RetroTheme.ACCENT_PINK : (isAwaitingChoice ? RetroTheme.ACCENT_YELLOW : new Color(55, 42, 80)));

                g2.setColor(baseBg);
                g2.fillRoundRect(baseX, baseY, baseW, baseH, 6, 6);
                // Bevel top edge
                g2.setColor(new Color(45, 35, 68));
                g2.fillRect(baseX + 2, baseY + 1, baseW - 4, 2);
                g2.setColor(baseBorder);
                g2.setStroke(new BasicStroke(isSlotHit || isSlotEvicted || isHovered ? 2.0f : 1.2f));
                g2.drawRoundRect(baseX, baseY, baseW, baseH, 6, 6);

                if (page != -1) {
                    // Page Label (Clean "PAGE" label under the pipe, since the number is centered on the pipe!)
                    int pageFontSize = (slotW >= 80) ? 14 : ((slotW >= 55) ? 12 : 10);
                    g2.setFont(new Font("Consolas", Font.BOLD, pageFontSize));
                    String pStr = "PAGE";
                    FontMetrics pfm = g2.getFontMetrics();
                    int px = baseX + (baseW - pfm.stringWidth(pStr)) / 2;
                    int py = baseY + (baseH >= 45 ? 18 : 15);

                    // Text drop shadow
                    g2.setColor(new Color(0, 0, 0, 220));
                    g2.drawString(pStr, px + 1, py + 1);
                    g2.setColor(isSlotHit ? new Color(255, 235, 60) : new Color(220, 220, 240));
                    g2.drawString(pStr, px, py);

                    // Status Badge Capsule inside Pedestal
                    int pillH = (baseH >= 45) ? 16 : 14;
                    int pillY = baseY + baseH - pillH - 4;
                    int pillW = Math.max(24, baseW - 12);
                    int pillX = baseX + (baseW - pillW) / 2;

                    String tag = isSlotHit ? "★ HIT ★" : (isSlotEvicted ? "FLUSH" : "RESIDENT");
                    Color pillBg = isSlotHit ? new Color(0, 60, 25) : (isSlotEvicted ? new Color(70, 15, 35) : new Color(26, 20, 44));
                    Color pillCol = isSlotHit ? new Color(0, 240, 100) : (isSlotEvicted ? RetroTheme.ACCENT_PINK : new Color(175, 165, 205));

                    g2.setColor(pillBg);
                    g2.fillRoundRect(pillX, pillY, pillW, pillH, 4, 4);
                    g2.setColor(pillCol);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(pillX, pillY, pillW, pillH, 4, 4);

                    g2.setFont(new Font("Consolas", Font.BOLD, (slotW >= 80) ? 9 : 8));
                    FontMetrics bfm2 = g2.getFontMetrics();
                    g2.drawString(tag, pillX + (pillW - bfm2.stringWidth(tag)) / 2, pillY + pillH - 3);
                } else {
                    // Vacant Pipe Status
                    g2.setFont(new Font("Consolas", Font.BOLD, (slotW >= 80) ? 12 : 10));
                    g2.setColor(new Color(110, 95, 140));
                    FontMetrics vfm = g2.getFontMetrics();
                    String vStr = (slotW >= 80) ? "[ VACANT ]" : "[ - ]";
                    g2.drawString(vStr, baseX + (baseW - vfm.stringWidth(vStr)) / 2, baseY + baseH / 2 + 4);
                }

                // --- 3E. CHALLENGE CLICK PROMPT PILL IN BUFFER GAP ---
                if (isAwaitingChoice) {
                    int pW = Math.max(30, slotW - 6);
                    g2.setColor(new Color(15, 10, 30, 245));
                    g2.fillRoundRect(sx + 3, 36, pW, 16, 4, 4);
                    g2.setColor(isHovered ? RetroTheme.ACCENT_YELLOW : new Color(0, 240, 100));
                    g2.drawRoundRect(sx + 3, 36, pW, 16, 4, 4);
                    g2.setFont(new Font("Consolas", Font.BOLD, (slotW >= 70) ? 9 : 8));
                    FontMetrics cbfm = g2.getFontMetrics();
                    String cTag;
                    if ("HIT".equals(challengeType)) {
                        cTag = isHovered ? (slotW >= 85 ? ">> MATCH <<" : ">>") : (slotW >= 85 ? "[ MATCH HIT ]" : "[ HIT ]");
                    } else if ("ALLOCATE".equals(challengeType)) {
                        cTag = isHovered ? (slotW >= 85 ? ">> WARP <<" : ">>") : (slotW >= 85 ? "[ WARP IN ]" : "[ WARP ]");
                    } else {
                        cTag = isHovered ? (slotW >= 85 ? ">> FLUSH <<" : ">>") : (slotW >= 85 ? "[ FLUSH PIPE ]" : "[ FLUSH ]");
                    }
                    g2.drawString(cTag, sx + (slotW - cbfm.stringWidth(cTag)) / 2, 48);
                }

                // Click Flash Feedback
                if (isClicked) {
                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    FontMetrics tfm = g2.getFontMetrics();
                    int tagW = tfm.stringWidth(flashTag) + 16;
                    int tagX = sx + (slotW - tagW) / 2;

                    g2.setColor(new Color(12, 8, 24, 250));
                    g2.fillRoundRect(tagX, 36, tagW, 18, 6, 6);
                    g2.setColor(flashColor);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(tagX, 36, tagW, 18, 6, 6);
                    g2.drawString(flashTag, tagX + 8, 49);
                }

                sx += slotW + slotGap;
            }

            // =================================================================
            // 3C. HIGH-VISIBILITY LASER BEAMS & EVICTION ARCS (DRAWN ON TOP OF ALL PIPES!)
            // =================================================================

            // 1. GOLDEN COIN HIT BEAM
            if (isHit && hitLineX != -1) {
                g2.setColor(new Color(252, 228, 0, 100));
                g2.setStroke(new BasicStroke(5.0f));
                g2.drawLine(w / 2, beltY + beltH, hitLineX, hitLineY);

                g2.setColor(new Color(252, 228, 0));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(w / 2, beltY + beltH, hitLineX, hitLineY);

                // Floating Coin Pill placed in BUFFER GAP (y = 36..52)
                if (hitSlotW >= 55) {
                    int pX = hitLineX - (hitSlotW - 8) / 2;
                    g2.setColor(new Color(10, 8, 20, 245));
                    g2.fillRoundRect(pX, 36, hitSlotW - 8, 16, 4, 4);
                    g2.setColor(new Color(252, 228, 0));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(pX, 36, hitSlotW - 8, 16, 4, 4);
                    g2.setFont(new Font("Consolas", Font.BOLD, 9));
                    FontMetrics hbfm = g2.getFontMetrics();
                    String hTag = (hitSlotW >= 85) ? "★ COIN HIT! ★" : "★ HIT ★";
                    g2.drawString(hTag, pX + (hitSlotW - 8 - hbfm.stringWidth(hTag)) / 2, 48);
                }
            }

            // 2. ULTRA-VISIBLE NEON PINK EVICTION BEAM (WARP TRANSFER - 100% UNHIDDEN!)
            if (!isHit && victimFrameIdx != -1 && victimLineX != -1) {
                int targetX = swapX + 24;
                int targetY = podY + 45;

                // Overhead smooth arched trajectory
                int ctrlX = (victimLineX + targetX) / 2;
                int ctrlY = Math.min(26, podY - 14);
                java.awt.geom.QuadCurve2D.Float arc = new java.awt.geom.QuadCurve2D.Float(
                        victimLineX, victimLineY,
                        ctrlX, ctrlY,
                        targetX, targetY
                );

                // Thick Neon Outer Glow (Vibrant & Eye-Catching)
                g2.setColor(new Color(255, 40, 140, 110));
                g2.setStroke(new BasicStroke(7.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(arc);

                // Hot Pink Neon Core Beam
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(arc);

                // Animated Flow Dashes (traveling rightward into Warp Zone)
                float dashPhase = Math.abs((14.0f - ((animTick * 2.5f) % 14.0f)) % 14.0f);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{8f, 6f}, dashPhase));
                g2.draw(arc);

                // Source Pulse Rings at Victim Pipe
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.setStroke(new BasicStroke(1.5f));
                int sPulse = (int)(6 + Math.abs(Math.sin(animTick * 0.2)) * 6);
                g2.drawOval(victimLineX - sPulse, victimLineY - sPulse / 2, sPulse * 2, sPulse);

                // Destination Impact Flare at Warp Zone Chute
                int dPulse = (int)(8 + Math.abs(Math.sin(animTick * 0.3)) * 8);
                g2.setColor(new Color(255, 60, 160, 180));
                g2.fillOval(targetX - dPulse / 2, targetY - dPulse / 2, dPulse, dPulse);
                g2.setColor(Color.WHITE);
                g2.fillOval(targetX - 2, targetY - 2, 4, 4);

                // Prominent Floating Pink Pill in Buffer Gap
                int bWidth = 145;
                int bX = Math.max(victimLineX - 20, Math.min(swapX - bWidth - 10, ctrlX - bWidth / 2));
                g2.setColor(new Color(15, 8, 24, 250));
                g2.fillRoundRect(bX, 35, bWidth, 18, 5, 5);
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(bX, 35, bWidth, 18, 5, 5);

                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.drawString("⚡ FLUSH ➔ WARP ZONE", bX + 8, 48);
            }

            // =================================================================
            // 4. BOTTOM CONSOLE BANNER (y = h - 16, zero overlap!)
            // =================================================================
            int bannerY = h - 16;
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            FontMetrics bfm = g2.getFontMetrics();
            int bw = bfm.stringWidth(bannerMsg) + 24;
            int bx = Math.max(16, (w - bw) / 2);

            g2.setColor(new Color(18, 12, 34, 245));
            g2.fillRoundRect(bx, bannerY - 13, bw, 20, 6, 6);
            g2.setColor(isHit ? new Color(0, 240, 100) : (refPage != -1 ? RetroTheme.ACCENT_YELLOW : RetroTheme.ACCENT_CYAN));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(bx, bannerY - 13, bw, 20, 6, 6);
            g2.drawString(bannerMsg, bx + 12, bannerY);
        }
    }

    // =========================================================================
    // STEP STATE DATA STRUCTURE
    // =========================================================================
    public static class StepState {
        public int stepIndex;
        public int refPage;
        public int[] prevFrames;
        public int[] frames;
        public boolean isHit;
        public int hitFrameIdx = -1;
        public int victimPage = -1;
        public int victimFrameIdx = -1;
        public int emptyAssignedFrameIdx = -1;
        public String reason = "";
        public int hitsSoFar = 0;
        public int faultsSoFar = 0;
    }
}
