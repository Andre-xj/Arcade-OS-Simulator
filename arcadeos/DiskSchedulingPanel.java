package arcadeos;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * MISSION 07: DISK SCHEDULING (PAC-MAN HEAD RUNNER & MAZE WARP EDITION)
 * 
 * Features:
 * - Full algorithm suite: FCFS, SSTF, SCAN, C-SCAN, LOOK, C-LOOK.
 * - Pac-Man disk head character gliding smoothly across the linear cylinder platter.
 * - Left & Right iconic Neon Blue Warp Tunnels for C-SCAN / C-LOOK wrap-around jumps!
 * - Color-coded Pac-Man ghosts (Blinky, Pinky, Inky, Clyde) & fruit targets on cylinders.
 * - Real-time input synchronization, live HUD pods (Total Movement, Avg Seek, Current Cylinder).
 * - Interactive Challenge Mode with click-to-chomp and automated learning feedback.
 * - Responsive table maximizing 100% width with zero empty gaps.
 */
public class DiskSchedulingPanel extends JPanel {

    // Presets
    public static final String PRESET_STANDARD = "98 183 37 122 14 124 65 67";
    public static final String PRESET_SMALL    = "90 120 30 70 140";
    public static final String PRESET_LARGE    = "98 183 37 122 14 124 65 67 15 190 40 85";

    // Algorithm & Configuration State
    private String selectedAlgo = "FCFS";
    private int initialHead = 53;
    private int maxCylinder = 199;
    private boolean initialDirectionUp = true; // true = UP (towards Max), false = DOWN (towards 0)
    private int[] cylinderRequests = new int[0];
    private String rawRequestString = PRESET_STANDARD;

    // Simulation Execution State
    private final List<DiskStep> simulationSteps = new ArrayList<>();
    private int currentStepIdx = -1; // -1 = standby / ready
    private boolean isPlaying = false;
    private double speedMultiplier = 1.0;
    private javax.swing.Timer simTimer;
    private javax.swing.Timer challengeTimer;

    // Challenge Mode State
    private boolean isChallengeMode = false;
    private boolean awaitingChallengeChoice = false;

    // UI Widgets - Left Column
    private JButton btnFcfs, btnSstf, btnScan, btnCScan, btnLook, btnCLook;
    private JTextField txtInitHead, txtMaxCyl, txtRequests;
    private JComboBox<String> comboDir;
    private JLabel lblAlgoRule;
    private JButton btnPlay, btnPrev, btnStep, btnReset, btnModeToggle, btnSpeed, btnSteerLeft, btnSteerRight;
    private boolean isUpdatingQuietly = false;

    // UI Widgets - Right Arena
    private JLabel lblMissionStatus;
    private JLabel lblStatusPill;
    private JLabel lblTotalMovement, lblTotalSub;
    private JLabel lblAvgSeek, lblAvgSub;
    private JLabel lblCurrentHead, lblCurrentSub;
    private JLabel lblFooterTotal, lblFooterAvg, lblFooterFormula, lblFooterReqs;
    private JButton btnViewMode;

    // Center Canvas & Table
    private PacmanDiskCanvas canvas;
    private DefaultTableModel traceModel;
    private JTable traceTable;
    private JScrollPane traceScroll;

    // Data Structure for a step
    public static class DiskStep {
        public int stepIndex;        // 1-based index
        public int fromCyl;
        public int toCyl;
        public int seekDist;
        public int cumulativeSeek;
        public String direction;     // "UP" or "DOWN"
        public boolean isWarp;       // True for C-SCAN / C-LOOK wrap around
        public boolean isBoundary;   // True for 0 or maxCyl turnaround
        public String note;
        public int[] servicedSoFar;  // Snapshot of cylinders serviced up to this point
    }

    public DiskSchedulingPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(14, 0));
        setBorder(BorderFactory.createEmptyBorder(6, 12, 8, 12));

        // 1. Simulation Ticker Timer (40ms ticks)
        simTimer = new javax.swing.Timer(40, e -> updateSimulationTick());

        // 2. Left Column: Cyber Deck Controls (370px width)
        JPanel leftDeck = createLeftCyberDeck();
        leftDeck.setPreferredSize(new Dimension(370, 0));
        leftDeck.setMinimumSize(new Dimension(340, 0));
        add(leftDeck, BorderLayout.WEST);

        // 3. Right Arena: Header + HUD Pods + Pacman Canvas + Trace Table
        JPanel rightArena = createRightArena();
        add(rightArena, BorderLayout.CENTER);

        // Initial Computation
        loadRequestString(rawRequestString);

        // Keyboard Shortcuts
        setupKeyboardShortcuts();
    }

    // =========================================================================
    // 1. LEFT CYBER DECK PANEL
    // =========================================================================
    private JPanel createLeftCyberDeck() {
        JPanel deck = new JPanel();
        deck.setOpaque(true);
        deck.setBackground(new Color(18, 12, 32));
        deck.setLayout(new BoxLayout(deck, BoxLayout.Y_AXIS));
        deck.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(10, 10, 8, 10)
        ));

        // Header Title
        JLabel title = new JLabel("PAC-MAN DISK CONTROLLER");
        title.setFont(new Font("Consolas", Font.BOLD, 13));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        deck.add(title);

        JLabel sub = new JLabel("SECTOR SEEK & TRAVERSAL PMU");
        sub.setFont(new Font("Consolas", Font.BOLD, 9));
        sub.setForeground(RetroTheme.ACCENT_CYAN);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        deck.add(sub);

        deck.add(Box.createVerticalStrut(8));

        // Algorithm Selection Header
        JLabel aLbl = new JLabel("SEEK POLICY (ALGORITHM)");
        aLbl.setFont(new Font("Consolas", Font.BOLD, 10));
        aLbl.setForeground(RetroTheme.ACCENT_YELLOW);
        aLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        deck.add(aLbl);

        deck.add(Box.createVerticalStrut(4));

        // 6 Algorithm Buttons in a 2x3 Grid
        JPanel algoGrid = new JPanel(new GridLayout(2, 3, 4, 4));
        algoGrid.setOpaque(false);
        algoGrid.setMaximumSize(new Dimension(800, 56));
        algoGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnFcfs = CPUSchedulingPanel.createArcadeButton("FCFS", new Color(0, 140, 200), Color.WHITE, 26);
        btnSstf = CPUSchedulingPanel.createArcadeButton("SSTF", new Color(38, 25, 68), Color.WHITE, 26);
        btnScan = CPUSchedulingPanel.createArcadeButton("SCAN", new Color(38, 25, 68), Color.WHITE, 26);
        btnCScan = CPUSchedulingPanel.createArcadeButton("C-SCAN", new Color(38, 25, 68), Color.WHITE, 26);
        btnLook = CPUSchedulingPanel.createArcadeButton("LOOK", new Color(38, 25, 68), Color.WHITE, 26);
        btnCLook = CPUSchedulingPanel.createArcadeButton("C-LOOK", new Color(38, 25, 68), Color.WHITE, 26);

        btnFcfs.addActionListener(e -> setAlgorithm("FCFS"));
        btnSstf.addActionListener(e -> setAlgorithm("SSTF"));
        btnScan.addActionListener(e -> setAlgorithm("SCAN"));
        btnCScan.addActionListener(e -> setAlgorithm("C-SCAN"));
        btnLook.addActionListener(e -> setAlgorithm("LOOK"));
        btnCLook.addActionListener(e -> setAlgorithm("C-LOOK"));

        algoGrid.add(btnFcfs);
        algoGrid.add(btnSstf);
        algoGrid.add(btnScan);
        algoGrid.add(btnCScan);
        algoGrid.add(btnLook);
        algoGrid.add(btnCLook);
        deck.add(algoGrid);

        deck.add(Box.createVerticalStrut(4));

        // Algorithm Rule Explanation Note
        lblAlgoRule = new JLabel("FCFS: Services disk cylinders strictly by arrival order.");
        lblAlgoRule.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblAlgoRule.setForeground(RetroTheme.TEXT_DIM);
        lblAlgoRule.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblAlgoRule.setMaximumSize(new Dimension(800, 18));
        deck.add(lblAlgoRule);

        deck.add(Box.createVerticalStrut(8));

        // Disk Geometry Settings: Initial Head, Max Cylinder, Direction
        JPanel geomCard = new JPanel(new GridLayout(3, 2, 6, 6));
        geomCard.setOpaque(true);
        geomCard.setBackground(new Color(24, 18, 42));
        geomCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        geomCard.setMaximumSize(new Dimension(800, 96));
        geomCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Row 1: Initial Head
        JLabel hLbl = new JLabel("INITIAL HEAD:");
        hLbl.setFont(new Font("Consolas", Font.BOLD, 10));
        hLbl.setForeground(Color.WHITE);
        geomCard.add(hLbl);

        txtInitHead = new JTextField(String.valueOf(initialHead));
        txtInitHead.setFont(new Font("Consolas", Font.BOLD, 13));
        txtInitHead.setBackground(new Color(15, 10, 28));
        txtInitHead.setForeground(Color.WHITE);
        txtInitHead.setCaretColor(Color.WHITE);
        txtInitHead.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 180, 240), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        txtInitHead.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { tryQuietConfig(); }
            public void removeUpdate(DocumentEvent e) { tryQuietConfig(); }
            public void changedUpdate(DocumentEvent e) { tryQuietConfig(); }
        });
        geomCard.add(txtInitHead);

        // Row 2: Max Cylinder (Disk Boundary)
        JLabel bLbl = new JLabel("MAX CYLINDER:");
        bLbl.setFont(new Font("Consolas", Font.BOLD, 10));
        bLbl.setForeground(Color.WHITE);
        geomCard.add(bLbl);

        txtMaxCyl = new JTextField(String.valueOf(maxCylinder));
        txtMaxCyl.setFont(new Font("Consolas", Font.BOLD, 13));
        txtMaxCyl.setBackground(new Color(15, 10, 28));
        txtMaxCyl.setForeground(RetroTheme.ACCENT_YELLOW);
        txtMaxCyl.setCaretColor(Color.WHITE);
        txtMaxCyl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_YELLOW, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        txtMaxCyl.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { tryQuietConfig(); }
            public void removeUpdate(DocumentEvent e) { tryQuietConfig(); }
            public void changedUpdate(DocumentEvent e) { tryQuietConfig(); }
        });
        geomCard.add(txtMaxCyl);

        // Row 3: Initial Direction
        JLabel dLbl = new JLabel("SEEK DIRECTION:");
        dLbl.setFont(new Font("Consolas", Font.BOLD, 10));
        dLbl.setForeground(Color.WHITE);
        geomCard.add(dLbl);

        comboDir = new JComboBox<>(new String[]{"UP (High ->)", "DOWN (Low <-)"});
        comboDir.setFont(new Font("Consolas", Font.BOLD, 11));
        comboDir.setBackground(new Color(32, 24, 56));
        comboDir.setForeground(Color.WHITE);
        comboDir.addActionListener(e -> {
            initialDirectionUp = comboDir.getSelectedIndex() == 0;
            recomputeSimulation();
        });
        geomCard.add(comboDir);

        deck.add(geomCard);

        deck.add(Box.createVerticalStrut(8));

        // Cylinder Requests Input Field
        JLabel rLbl = new JLabel("DISK REQUEST STREAM (CYLINDERS):");
        rLbl.setFont(new Font("Consolas", Font.BOLD, 11));
        rLbl.setForeground(RetroTheme.ACCENT_YELLOW);
        rLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        deck.add(rLbl);

        deck.add(Box.createVerticalStrut(4));

        txtRequests = new JTextField(rawRequestString);
        txtRequests.setFont(new Font("Consolas", Font.BOLD, 14));
        txtRequests.setBackground(new Color(15, 10, 28));
        txtRequests.setForeground(new Color(0, 255, 255));
        txtRequests.setCaretColor(Color.WHITE);
        txtRequests.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 220, 255), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        txtRequests.setMaximumSize(new Dimension(800, 36));
        txtRequests.setPreferredSize(new Dimension(800, 36));
        txtRequests.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtRequests.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { tryQuietStream(); }
            public void removeUpdate(DocumentEvent e) { tryQuietStream(); }
            public void changedUpdate(DocumentEvent e) { tryQuietStream(); }
        });
        deck.add(txtRequests);

        deck.add(Box.createVerticalStrut(6));

        // Preset Quick Buttons
        JPanel presetGrid = new JPanel(new GridLayout(1, 4, 4, 0));
        presetGrid.setOpaque(false);
        presetGrid.setMaximumSize(new Dimension(800, 24));
        presetGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton pStd = CPUSchedulingPanel.createArcadeButton("STD 8", new Color(38, 25, 68), Color.WHITE, 22);
        JButton pSml = CPUSchedulingPanel.createArcadeButton("SML 5", new Color(38, 25, 68), Color.WHITE, 22);
        JButton pLrg = CPUSchedulingPanel.createArcadeButton("LRG 12", new Color(38, 25, 68), Color.WHITE, 22);
        JButton pRnd = CPUSchedulingPanel.createArcadeButton("RANDOM", new Color(38, 25, 68), RetroTheme.ACCENT_YELLOW, 22);

        pStd.addActionListener(e -> { txtInitHead.setText("53"); loadRequestString(PRESET_STANDARD); });
        pSml.addActionListener(e -> { txtInitHead.setText("50"); loadRequestString(PRESET_SMALL); });
        pLrg.addActionListener(e -> { txtInitHead.setText("53"); loadRequestString(PRESET_LARGE); });
        pRnd.addActionListener(e -> generateRandomStream());

        presetGrid.add(pStd);
        presetGrid.add(pSml);
        presetGrid.add(pLrg);
        presetGrid.add(pRnd);
        deck.add(presetGrid);

        deck.add(Box.createVerticalStrut(10));

        // Simulation Action Buttons
        btnPlay = CPUSchedulingPanel.createArcadeButton("START SIMULATION", new Color(0, 160, 60), Color.WHITE, 34);
        btnPlay.setFont(new Font("Consolas", Font.BOLD, 12));
        btnPlay.setMaximumSize(new Dimension(800, 34));
        btnPlay.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPlay.addActionListener(e -> togglePlayPause());
        deck.add(btnPlay);

        deck.add(Box.createVerticalStrut(6));

        // PAC-MAN ARCADE DIRECTIONAL CONTROLLER
        JPanel ctrlCard = new JPanel();
        ctrlCard.setLayout(new BoxLayout(ctrlCard, BoxLayout.Y_AXIS));
        ctrlCard.setOpaque(true);
        ctrlCard.setBackground(new Color(24, 18, 42));
        ctrlCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
        ctrlCard.setMaximumSize(new Dimension(800, 76));
        ctrlCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel ctrlTitle = new JLabel("PAC-MAN DIRECTION CONTROLLER");
        ctrlTitle.setFont(new Font("Consolas", Font.BOLD, 9));
        ctrlTitle.setForeground(RetroTheme.ACCENT_YELLOW);
        ctrlTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        ctrlCard.add(ctrlTitle);
        ctrlCard.add(Box.createVerticalStrut(4));

        JPanel dpadRow = new JPanel(new GridLayout(1, 2, 6, 0));
        dpadRow.setOpaque(false);

        btnSteerLeft = CPUSchedulingPanel.createArcadeButton("◄ STEER LEFT", new Color(0, 130, 200), Color.WHITE, 26);
        btnSteerLeft.setFont(new Font("Consolas", Font.BOLD, 10));
        btnSteerLeft.setToolTipText("Steer Pac-Man LEFT (Left Arrow / A key)");
        btnSteerLeft.addActionListener(e -> steerPacman(false));

        btnSteerRight = CPUSchedulingPanel.createArcadeButton("STEER RIGHT ►", new Color(200, 130, 0), Color.WHITE, 26);
        btnSteerRight.setFont(new Font("Consolas", Font.BOLD, 10));
        btnSteerRight.setToolTipText("Steer Pac-Man RIGHT (Right Arrow / D key)");
        btnSteerRight.addActionListener(e -> steerPacman(true));

        dpadRow.add(btnSteerLeft);
        dpadRow.add(btnSteerRight);
        ctrlCard.add(dpadRow);
        ctrlCard.add(Box.createVerticalStrut(4));

        JPanel subRow = new JPanel(new GridLayout(1, 3, 4, 0));
        subRow.setOpaque(false);
        btnPrev = CPUSchedulingPanel.createArcadeButton("<< PREV", new Color(45, 30, 75), Color.WHITE, 20);
        btnPrev.setFont(new Font("Consolas", Font.BOLD, 8));
        btnPrev.setToolTipText("Step backward");
        btnPrev.addActionListener(e -> stepBackward());

        btnStep = CPUSchedulingPanel.createArcadeButton("AUTO STEP >>", new Color(0, 100, 150), Color.WHITE, 20);
        btnStep.setFont(new Font("Consolas", Font.BOLD, 8));
        btnStep.setToolTipText("Auto step to next target");
        btnStep.addActionListener(e -> stepForward());

        btnReset = CPUSchedulingPanel.createArcadeButton("RESET", new Color(130, 35, 45), Color.WHITE, 20);
        btnReset.setFont(new Font("Consolas", Font.BOLD, 8));
        btnReset.setToolTipText("Reset simulation (R key)");
        btnReset.addActionListener(e -> resetSimulation());

        subRow.add(btnPrev);
        subRow.add(btnStep);
        subRow.add(btnReset);
        ctrlCard.add(subRow);

        deck.add(ctrlCard);

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
        btnModeToggle.setToolTipText("Toggle Interactive Pac-Man Challenge Mode");
        btnModeToggle.addActionListener(e -> toggleChallengeMode());

        speedRow.add(btnSpeed);
        speedRow.add(btnModeToggle);
        deck.add(speedRow);

        deck.add(Box.createVerticalStrut(8));

        // Tactical Shortcuts Cheat Sheet Card
        JPanel infoCard = new JPanel(new GridLayout(6, 1, 0, 2));
        infoCard.setOpaque(true);
        infoCard.setBackground(new Color(24, 18, 42));
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        infoCard.setMaximumSize(new Dimension(800, 118));
        infoCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iHead = new JLabel("KEYBOARD GAME CONTROLLER");
        iHead.setFont(new Font("Consolas", Font.BOLD, 10));
        iHead.setForeground(RetroTheme.ACCENT_YELLOW);
        infoCard.add(iHead);

        JLabel k1 = new JLabel("• LEFT ARROW (<-) / A: STEER PAC-MAN LEFT");
        k1.setFont(new Font("Consolas", Font.PLAIN, 9));
        k1.setForeground(RetroTheme.ACCENT_CYAN);
        infoCard.add(k1);

        JLabel k2 = new JLabel("• RIGHT ARROW (->) / D: STEER PAC-MAN RIGHT");
        k2.setFont(new Font("Consolas", Font.PLAIN, 9));
        k2.setForeground(RetroTheme.ACCENT_YELLOW);
        infoCard.add(k2);

        JLabel k2b = new JLabel("• SPACE / S: PLAY/PAUSE OR AUTO-STEP");
        k2b.setFont(new Font("Consolas", Font.PLAIN, 9));
        k2b.setForeground(Color.WHITE);
        infoCard.add(k2b);

        JLabel k3 = new JLabel("• R KEY: RESET RUN");
        k3.setFont(new Font("Consolas", Font.PLAIN, 9));
        k3.setForeground(Color.WHITE);
        infoCard.add(k3);

        JLabel k4 = new JLabel("• CLICK ALIEN: DIRECT CHOMP IN CHALLENGE");
        k4.setFont(new Font("Consolas", Font.PLAIN, 9));
        k4.setForeground(new Color(0, 240, 100));
        infoCard.add(k4);

        deck.add(infoCard);

        deck.add(Box.createVerticalGlue());

        // Hardware Telemetry Strip at Bottom
        JPanel telemetry = new JPanel(new BorderLayout());
        telemetry.setOpaque(true);
        telemetry.setBackground(new Color(12, 8, 22));
        telemetry.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 32, 70), 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        telemetry.setMaximumSize(new Dimension(800, 22));
        telemetry.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tLbl = new JLabel("[*] DISK PMU // 7200 RPM // SEEK BUS OK");
        tLbl.setFont(new Font("Consolas", Font.BOLD, 9));
        tLbl.setForeground(new Color(0, 240, 100));
        telemetry.add(tLbl, BorderLayout.WEST);

        JLabel vLbl = new JLabel("IOPS: 120");
        vLbl.setFont(new Font("Consolas", Font.BOLD, 9));
        vLbl.setForeground(RetroTheme.TEXT_DIM);
        telemetry.add(vLbl, BorderLayout.EAST);

        deck.add(telemetry);

        return deck;
    }

    // =========================================================================
    // 2. RIGHT ARENA: HEADER + 3 GLOWING HUDS + PAC-MAN CANVAS + TRACE TABLE
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
        header.setMaximumSize(new Dimension(1400, 26));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblMissionStatus = new JLabel("LIVE OUTPUT // PAC-MAN DISK SEEK PMU");
        lblMissionStatus.setFont(new Font("Consolas", Font.BOLD, 13));
        lblMissionStatus.setForeground(Color.WHITE);
        lblMissionStatus.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        header.add(lblMissionStatus, BorderLayout.WEST);

        JPanel headerControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        headerControls.setOpaque(false);

        btnViewMode = CPUSchedulingPanel.createArcadeButton("VIEW: 2D TRAJECTORY", new Color(0, 140, 200), Color.WHITE, 22);
        btnViewMode.addActionListener(e -> {
            if (canvas != null) {
                canvas.toggleViewMode();
                btnViewMode.setText(canvas.is2DView() ? "VIEW: 2D TRAJECTORY" : "VIEW: 1D LINEAR PLATTER");
                btnViewMode.setBackground(canvas.is2DView() ? new Color(0, 140, 200) : new Color(180, 80, 0));
            }
        });
        headerControls.add(btnViewMode);

        JButton btnPlayPacmanHeader = RetroTheme.createRetroButton("PLAY PAC-MAN ARCADE", new Color(48, 38, 12), RetroTheme.ACCENT_YELLOW);
        btnPlayPacmanHeader.setFont(new Font("Consolas", Font.BOLD, 10));
        btnPlayPacmanHeader.setIcon(new ImageIcon(PixelSprites.createPacman(14, 35f, true)));
        btnPlayPacmanHeader.setIconTextGap(6);
        btnPlayPacmanHeader.setToolTipText("Launch playable Pac-Man Disk Seek Runner arcade minigame!");
        btnPlayPacmanHeader.addActionListener(e -> launchPacmanArcadeGame());
        headerControls.add(btnPlayPacmanHeader);

        lblStatusPill = new JLabel(); // Retained in memory to prevent NPEs, but square status removed from UI

        header.add(headerControls, BorderLayout.EAST);
        topSection.add(header);

        topSection.add(Box.createVerticalStrut(4));

        // 2. 3 High-Impact Glowing HUD Pods
        JPanel statsCard = new JPanel(new GridLayout(1, 3, 10, 0));
        statsCard.setOpaque(false);
        statsCard.setMaximumSize(new Dimension(1400, 72));
        statsCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Pod 1: Total Head Movement
        JPanel p1 = createGlassHudPod("TOTAL HEAD MOVEMENT", RetroTheme.ACCENT_YELLOW);
        lblTotalMovement = new JLabel("0 CYLINDERS");
        lblTotalMovement.setFont(new Font("Consolas", Font.BOLD, 20));
        lblTotalMovement.setForeground(RetroTheme.ACCENT_YELLOW);
        lblTotalSub = new JLabel("TOTAL SEEK TRAVERSAL");
        lblTotalSub.setFont(new Font("Consolas", Font.BOLD, 10));
        lblTotalSub.setForeground(Color.WHITE);
        p1.add(lblTotalMovement);
        p1.add(lblTotalSub);
        statsCard.add(p1);

        // Pod 2: Average Seek Length
        JPanel p2 = createGlassHudPod("SEEK EFFICIENCY", new Color(0, 240, 180));
        lblAvgSeek = new JLabel("0.0 CYL / REQ");
        lblAvgSeek.setFont(new Font("Consolas", Font.BOLD, 20));
        lblAvgSeek.setForeground(new Color(0, 240, 180));
        lblAvgSub = new JLabel("AVERAGE SEEK LENGTH [LOW=BETTER]");
        lblAvgSub.setFont(new Font("Consolas", Font.BOLD, 10));
        lblAvgSub.setForeground(Color.WHITE);
        p2.add(lblAvgSeek);
        p2.add(lblAvgSub);
        statsCard.add(p2);

        // Pod 3: Current Head Position
        JPanel p3 = createGlassHudPod("CURRENT HEAD POSITION", RetroTheme.ACCENT_PINK);
        lblCurrentHead = new JLabel("CYLINDER #" + initialHead);
        lblCurrentHead.setFont(new Font("Consolas", Font.BOLD, 20));
        lblCurrentHead.setForeground(RetroTheme.ACCENT_PINK);
        lblCurrentSub = new JLabel("PAC-MAN HEAD // DIRECTION: UP ->");
        lblCurrentSub.setFont(new Font("Consolas", Font.BOLD, 10));
        lblCurrentSub.setForeground(Color.WHITE);
        p3.add(lblCurrentHead);
        p3.add(lblCurrentSub);
        statsCard.add(p3);

        topSection.add(statsCard);

        topSection.add(Box.createVerticalStrut(6));

        // 3. Pac-Man Disk Head Runner Game Canvas
        canvas = new PacmanDiskCanvas(this);
        canvas.setPreferredSize(new Dimension(800, 310));
        canvas.setMinimumSize(new Dimension(500, 280));
        canvas.setMaximumSize(new Dimension(1600, 340));
        canvas.setAlignmentX(Component.LEFT_ALIGNMENT);
        topSection.add(canvas);

        arena.add(topSection, BorderLayout.NORTH);

        // 4. Matrix / Trace Table: Placed in CENTER so it expands to fill remaining space
        JPanel tableContainer = createTraceTableContainer();
        arena.add(tableContainer, BorderLayout.CENTER);

        return arena;
    }

    private JPanel createGlassHudPod(String title, Color borderCol) {
        JPanel pod = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 15, 34, 235));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(borderCol);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        pod.setOpaque(false);
        pod.setLayout(new BoxLayout(pod, BoxLayout.Y_AXIS));
        pod.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Consolas", Font.BOLD, 9));
        t.setForeground(borderCol);
        pod.add(t);
        pod.add(Box.createVerticalStrut(2));

        return pod;
    }

    private JPanel createTraceTableContainer() {
        JPanel container = new JPanel(new BorderLayout(0, 4));
        container.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JLabel title = new JLabel("DISK TRAVERSAL PATH & SEEK STEP MATRIX");
        title.setFont(new Font("Consolas", Font.BOLD, 11));
        title.setForeground(RetroTheme.ACCENT_CYAN);
        header.add(title, BorderLayout.WEST);

        JLabel formulaHeader = new JLabel("TEXTBOOK FORMULA: Σ(TRACKS TRAVERSED) ÷ TOTAL REQUESTS = AVERAGE SEEK LENGTH");
        formulaHeader.setFont(new Font("Consolas", Font.BOLD, 9));
        formulaHeader.setForeground(RetroTheme.TEXT_DIM);
        header.add(formulaHeader, BorderLayout.EAST);

        container.add(header, BorderLayout.NORTH);

        // Table Model: EXACT MATCHING OF USER LECTURE SLIDE COLUMNS
        String[] cols = new String[]{
                "STEP #", 
                "CURRENT TRACK", 
                "NEXT TRACK ACCESSED", 
                "NUMBER OF TRACKS TRAVERSED", 
                "SEEK DIRECTION", 
                "CUMULATIVE SEEK", 
                "ACTION / NOTE"
        };
        traceModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        traceTable = new JTable(traceModel);
        traceTable.setBackground(new Color(15, 10, 28));
        traceTable.setForeground(Color.WHITE);
        traceTable.setFont(new Font("Consolas", Font.BOLD, 11));
        traceTable.setRowHeight(24);
        traceTable.setShowGrid(true);
        traceTable.setGridColor(new Color(45, 30, 70));
        traceTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        traceTable.getTableHeader().setReorderingAllowed(false);
        traceTable.getTableHeader().setBackground(new Color(28, 18, 50));
        traceTable.getTableHeader().setForeground(RetroTheme.ACCENT_CYAN);
        traceTable.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 10));

        // Center all cell contents with custom renderer
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, val, sel, foc, r, c);
                setHorizontalAlignment(SwingConstants.CENTER);

                boolean isActive = (r == currentStepIdx);
                if (isActive) {
                    comp.setBackground(new Color(45, 60, 20));
                    comp.setForeground(new Color(255, 238, 0));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (r % 2 == 0) {
                    comp.setBackground(new Color(18, 12, 32));
                    comp.setForeground(Color.WHITE);
                } else {
                    comp.setBackground(new Color(12, 8, 24));
                    comp.setForeground(new Color(210, 210, 230));
                }

                if (c == 2) {
                    if (!isActive) setForeground(Color.WHITE);
                } else if (c == 3) {
                    if (!isActive) setForeground(new Color(0, 240, 180));
                } else if (c == 5) {
                    if (!isActive) setForeground(RetroTheme.ACCENT_YELLOW);
                } else if (c == 6) {
                    String s = (val != null) ? val.toString() : "";
                    if (s.contains("WARP")) setForeground(RetroTheme.ACCENT_PINK);
                    else if (s.contains("CHOMP")) setForeground(new Color(0, 240, 100));
                }
                return comp;
            }
        };

        for (int c = 0; c < traceTable.getColumnCount(); c++) {
            traceTable.getColumnModel().getColumn(c).setCellRenderer(centerRender);
        }

        traceTable.getColumnModel().getColumn(0).setPreferredWidth(55);  // STEP #
        traceTable.getColumnModel().getColumn(1).setPreferredWidth(105); // CURRENT TRACK
        traceTable.getColumnModel().getColumn(2).setPreferredWidth(145); // NEXT TRACK ACCESSED
        traceTable.getColumnModel().getColumn(3).setPreferredWidth(185); // NUMBER OF TRACKS TRAVERSED
        traceTable.getColumnModel().getColumn(4).setPreferredWidth(110); // SEEK DIRECTION
        traceTable.getColumnModel().getColumn(5).setPreferredWidth(125); // CUMULATIVE SEEK
        traceTable.getColumnModel().getColumn(6).setPreferredWidth(200); // ACTION / NOTE

        traceScroll = new JScrollPane(traceTable);
        traceScroll.getViewport().setBackground(new Color(15, 10, 28));
        traceScroll.setBorder(BorderFactory.createLineBorder(new Color(55, 40, 80), 1));
        container.add(traceScroll, BorderLayout.CENTER);

        // Dedicated Summary Footer matching lecture slide
        JPanel footerCard = new JPanel(new GridLayout(1, 2, 12, 0));
        footerCard.setOpaque(true);
        footerCard.setBackground(new Color(16, 11, 28));
        footerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // Left pod: Total Tracks Traversed
        JPanel leftPod = new JPanel(new GridLayout(2, 1, 0, 2));
        leftPod.setOpaque(false);
        lblFooterTotal = new JLabel("TOTAL TRACKS TRAVERSED: 0 CYLINDERS");
        lblFooterTotal.setFont(new Font("Consolas", Font.BOLD, 12));
        lblFooterTotal.setForeground(RetroTheme.ACCENT_YELLOW);
        lblFooterReqs = new JLabel("REQUESTS SERVICED: 0 / 0");
        lblFooterReqs.setFont(new Font("Consolas", Font.BOLD, 9));
        lblFooterReqs.setForeground(RetroTheme.ACCENT_CYAN);
        leftPod.add(lblFooterTotal);
        leftPod.add(lblFooterReqs);
        footerCard.add(leftPod);

        // Right pod: Average Seek Length
        JPanel rightPod = new JPanel(new GridLayout(2, 1, 0, 2));
        rightPod.setOpaque(false);
        lblFooterAvg = new JLabel("AVERAGE SEEK LENGTH: 0.0 CYLINDERS");
        lblFooterAvg.setFont(new Font("Consolas", Font.BOLD, 12));
        lblFooterAvg.setForeground(new Color(0, 240, 180));
        lblFooterFormula = new JLabel("FORMULA: Σ(TRAVERSED TRACKS) ÷ TOTAL REQUESTS");
        lblFooterFormula.setFont(new Font("Consolas", Font.BOLD, 9));
        lblFooterFormula.setForeground(RetroTheme.TEXT_DIM);
        rightPod.add(lblFooterAvg);
        rightPod.add(lblFooterFormula);
        footerCard.add(rightPod);

        container.add(footerCard, BorderLayout.SOUTH);

        return container;
    }

    // =========================================================================
    // 3. SIMULATION ENGINE: FCFS, SSTF, SCAN, C-SCAN, LOOK, C-LOOK
    // =========================================================================
    public void recomputeSimulation() {
        simulationSteps.clear();
        if (cylinderRequests == null || cylinderRequests.length == 0) return;

        List<Integer> reqList = new ArrayList<>();
        for (int r : cylinderRequests) reqList.add(r);

        List<Integer> seekSequence = new ArrayList<>();
        seekSequence.add(initialHead);

        int maxCyl = maxCylinder;
        int initHead = initialHead;
        boolean goUp = initialDirectionUp;

        if ("FCFS".equals(selectedAlgo)) {
            seekSequence.addAll(reqList);

        } else if ("SSTF".equals(selectedAlgo)) {
            List<Integer> pool = new ArrayList<>(reqList);
            int current = initHead;
            while (!pool.isEmpty()) {
                int closest = -1, minDiff = Integer.MAX_VALUE;
                for (int r : pool) {
                    int diff = Math.abs(r - current);
                    if (diff < minDiff) {
                        minDiff = diff;
                        closest = r;
                    }
                }
                seekSequence.add(closest);
                current = closest;
                pool.remove(Integer.valueOf(closest));
            }

        } else if ("SCAN".equals(selectedAlgo)) {
            List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
            for (int r : reqList) {
                if (r < initHead) left.add(r);
                else right.add(r);
            }
            Collections.sort(left);
            Collections.sort(right);

            if (goUp) {
                seekSequence.addAll(right);
                if (!left.isEmpty()) {
                    seekSequence.add(maxCyl); // Touches boundary wall
                    Collections.reverse(left);
                    seekSequence.addAll(left);
                }
            } else {
                Collections.reverse(left);
                seekSequence.addAll(left);
                if (!right.isEmpty()) {
                    seekSequence.add(0); // Touches boundary wall 0
                    seekSequence.addAll(right);
                }
            }

        } else if ("C-SCAN".equals(selectedAlgo)) {
            List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
            for (int r : reqList) {
                if (r < initHead) left.add(r);
                else right.add(r);
            }
            Collections.sort(left);
            Collections.sort(right);

            if (goUp) {
                seekSequence.addAll(right);
                seekSequence.add(maxCyl); // Touches right boundary
                seekSequence.add(0);      // Warps to left boundary
                seekSequence.addAll(left);
            } else {
                Collections.reverse(left);
                seekSequence.add(0);      // Touches left boundary
                seekSequence.add(maxCyl);  // Warps to right boundary
                Collections.reverse(right);
                seekSequence.addAll(right);
            }

        } else if ("LOOK".equals(selectedAlgo)) {
            List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
            for (int r : reqList) {
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

        } else if ("C-LOOK".equals(selectedAlgo)) {
            List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
            for (int r : reqList) {
                if (r < initHead) left.add(r);
                else right.add(r);
            }
            Collections.sort(left);
            Collections.sort(right);

            if (goUp) {
                seekSequence.addAll(right);
                seekSequence.addAll(left); // Warps directly to lowest request
            } else {
                Collections.reverse(left);
                Collections.reverse(right);
                seekSequence.addAll(right); // Warps directly to highest request
            }
        }

        // Build DiskStep list
        int totalMovement = 0;
        Set<Integer> serviced = new HashSet<>();

        for (int i = 0; i < seekSequence.size() - 1; i++) {
            DiskStep ds = new DiskStep();
            ds.stepIndex = i + 1;
            ds.fromCyl = seekSequence.get(i);
            ds.toCyl = seekSequence.get(i + 1);
            ds.seekDist = Math.abs(ds.toCyl - ds.fromCyl);
            totalMovement += ds.seekDist;
            ds.cumulativeSeek = totalMovement;
            ds.direction = (ds.toCyl >= ds.fromCyl) ? "UP (->)" : "DOWN (<-)";

            // Check if boundary turnaround or warp
            boolean isCScanWarp = ("C-SCAN".equals(selectedAlgo) && ((ds.fromCyl == maxCyl && ds.toCyl == 0) || (ds.fromCyl == 0 && ds.toCyl == maxCyl)));
            boolean isCLookWarp = ("C-LOOK".equals(selectedAlgo) && Math.abs(ds.toCyl - ds.fromCyl) > (maxCyl / 2) && !reqList.contains(ds.toCyl) && !reqList.contains(ds.fromCyl));
            ds.isWarp = isCScanWarp || isCLookWarp;
            ds.isBoundary = (ds.toCyl == 0 || ds.toCyl == maxCyl) && !ds.isWarp;

            if (ds.isWarp) {
                ds.note = "WARP TUNNEL RESET -> [Cyl " + ds.toCyl + "]";
            } else if (ds.isBoundary) {
                ds.note = "BOUNDARY REVERSE -> [Cyl " + ds.toCyl + "]";
            } else {
                ds.note = "CHOMP REQUEST -> [Cyl " + ds.toCyl + "]";
                serviced.add(ds.toCyl);
            }

            int[] sArray = new int[serviced.size()];
            int idx = 0;
            for (int s : serviced) sArray[idx++] = s;
            ds.servicedSoFar = sArray;

            simulationSteps.add(ds);
        }

        // Populate Table Model
        traceModel.setRowCount(0);
        for (DiskStep ds : simulationSteps) {
            traceModel.addRow(new Object[]{
                    "#" + ds.stepIndex,
                    ds.fromCyl,
                    ds.toCyl,
                    ds.seekDist + " cyl",
                    ds.direction,
                    ds.cumulativeSeek + " cyl",
                    ds.note
            });
        }

        // Reset to initial ready state
        resetSimulation();
    }

    // =========================================================================
    // 4. PLAYBACK & CHALLENGE CONTROLS
    // =========================================================================
    public void togglePlayPause() {
        if (isPlaying) {
            pauseSimulation();
        } else {
            startSimulation();
        }
    }

    public void startSimulation() {
        tryQuietConfig();
        tryQuietStream();
        if (isChallengeMode) {
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
        tryQuietConfig();
        tryQuietStream();
        if (isChallengeMode && awaitingChallengeChoice) {
            if (canvas != null) {
                canvas.setBannerMsg("[CHALLENGE ACTIVE]: Click the target cylinder on the track to answer!");
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

    public void stepBackward() {
        tryQuietConfig();
        tryQuietStream();
        if (isChallengeMode && awaitingChallengeChoice) {
            return;
        }
        if (currentStepIdx > 0) {
            currentStepIdx--;
            updateDisplayToStep(currentStepIdx);
        } else if (currentStepIdx == 0) {
            resetSimulation();
        }
    }

    public void resetSimulation() {
        pauseSimulation();
        if (challengeTimer != null && challengeTimer.isRunning()) {
            challengeTimer.stop();
        }
        currentStepIdx = -1;
        awaitingChallengeChoice = false;
        updateDisplayToStep(-1);
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
            lblTotalMovement.setText("0 CYLINDERS");
            lblTotalSub.setText("TOTAL SEEK TRAVERSAL");
            lblAvgSeek.setText("0.0 CYL / REQ");
            lblAvgSub.setText("AVERAGE SEEK LENGTH [LOW=BETTER]");
            lblCurrentHead.setText("CYLINDER #" + initialHead);
            lblCurrentSub.setText("PAC-MAN HEAD // DIRECTION: " + (initialDirectionUp ? "UP ->" : "DOWN <-"));

            if (lblStatusPill != null) {
                lblStatusPill.setText(" [ STATUS: READY // CLICK START ] ");
                lblStatusPill.setForeground(new Color(0, 240, 100));
                lblStatusPill.setBorder(BorderFactory.createLineBorder(new Color(0, 240, 100), 1));
            }
            if (btnPlay != null && !isPlaying) {
                btnPlay.setText("START SIMULATION");
                btnPlay.setIcon(RetroIcons.createPlayIcon(14, Color.WHITE));
                btnPlay.setBackground(new Color(0, 160, 60));
            }

            int totalFinal = (simulationSteps.isEmpty()) ? 0 : simulationSteps.get(simulationSteps.size() - 1).cumulativeSeek;
            int reqCount = cylinderRequests.length > 0 ? cylinderRequests.length : 1;
            double avgFinal = (double) totalFinal / reqCount;
            if (lblFooterTotal != null) {
                lblFooterTotal.setText("TOTAL TRACKS TRAVERSED: " + totalFinal + " CYLINDERS (PLANNED)");
                lblFooterReqs.setText("READY // " + simulationSteps.size() + " STEPS COMPUTED");
                lblFooterAvg.setText(String.format("AVERAGE SEEK LENGTH: %.2f CYLINDERS", avgFinal));
                lblFooterFormula.setText(String.format("FORMULA: %d cyl ÷ %d requests = %.2f cyl/req", totalFinal, reqCount, avgFinal));
            }

            if (canvas != null) {
                canvas.updateState(-1, initialHead, initialHead, new int[0], false, false, "STANDBY: Ready to simulate Pac-Man disk head traversal.");
            }
            if (traceTable != null) {
                traceTable.clearSelection();
                traceTable.repaint();
            }
            return;
        }

        DiskStep ds = simulationSteps.get(stepIdx);

        if (isChallengeMode) {
            awaitingChallengeChoice = true;
            if (isPlaying) {
                pauseSimulation();
            }
            if (lblStatusPill != null) {
                lblStatusPill.setText(" [ STATUS: CHALLENGE // CHOOSE NEXT CYLINDER ] ");
                lblStatusPill.setForeground(RetroTheme.ACCENT_YELLOW);
                lblStatusPill.setBorder(BorderFactory.createLineBorder(RetroTheme.ACCENT_YELLOW, 1));
            }
            if (canvas != null) {
                // In challenge mode: stop Pac-Man at fromCyl, prompt user to pick the next toCyl!
                int[] prevServiced = (stepIdx > 0) ? simulationSteps.get(stepIdx - 1).servicedSoFar : new int[0];
                canvas.updateChallengeState(stepIdx, ds.fromCyl, ds.toCyl, prevServiced, ds.isWarp, ds.isBoundary,
                        "[CHALLENGE]: Next target under " + selectedAlgo + "? Click the target cylinder Pac-Man should seek next:");
            }
            updateHud(ds, stepIdx);
            return; // Halt and wait for user's click!
        }

        // Normal Simulation Mode:
        awaitingChallengeChoice = false;
        boolean isFinished = (stepIdx >= simulationSteps.size() - 1);

        if (lblStatusPill != null) {
            if (isFinished) {
                lblStatusPill.setText(" [ STATUS: LEVEL CLEAR // COMPLETED! +100XP ] ");
                lblStatusPill.setForeground(new Color(255, 220, 0));
                lblStatusPill.setBorder(BorderFactory.createLineBorder(new Color(255, 220, 0), 1));
            } else {
                lblStatusPill.setText(" [ STATUS: SEEKING // STEP " + (stepIdx + 1) + "/" + simulationSteps.size() + " ] ");
                lblStatusPill.setForeground(RetroTheme.ACCENT_CYAN);
                lblStatusPill.setBorder(BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 1));
            }
        }

        if (isFinished && !isPlaying) {
            btnPlay.setText("REPLAY RUN");
            btnPlay.setIcon(RetroIcons.createPlayIcon(14, Color.WHITE));
            btnPlay.setBackground(new Color(180, 110, 0));
        }

        if (canvas != null) {
            String banner = isFinished
                    ? "LEVEL CLEAR! All " + simulationSteps.size() + " cylinder requests serviced successfully!"
                    : "STEP #" + ds.stepIndex + " // Seek Cyl " + ds.fromCyl + " -> " + ds.toCyl + " (" + ds.seekDist + " cyl, " + ds.direction + ")";
            canvas.updateState(stepIdx, ds.fromCyl, ds.toCyl, ds.servicedSoFar, ds.isWarp, ds.isBoundary, banner);
        }
        updateHud(ds, stepIdx);
    }

    public void onChallengeChoiceCompleted(boolean correct) {
        awaitingChallengeChoice = false;
        if (currentStepIdx >= 0 && currentStepIdx < simulationSteps.size()) {
            DiskStep ds = simulationSteps.get(currentStepIdx);
            updateHud(ds, currentStepIdx);
        }

        if (challengeTimer != null && challengeTimer.isRunning()) challengeTimer.stop();

        if (correct) {
            // Auto-advance after Pac-Man finishes eating the pellets (1.4s delay)
            challengeTimer = new javax.swing.Timer((int)(1400 / speedMultiplier), e -> {
                if (isChallengeMode && currentStepIdx < simulationSteps.size() - 1) {
                    currentStepIdx++;
                    updateDisplayToStep(currentStepIdx);
                } else if (isChallengeMode && currentStepIdx >= simulationSteps.size() - 1) {
                    if (canvas != null) {
                        canvas.setBannerMsg("CHALLENGE COMPLETE! Perfect disk scheduling mastery! (+100 XP)");
                        AppState.addScore(100);
                    }
                }
            });
            challengeTimer.setRepeats(false);
            challengeTimer.start();
        } else {
            // WRONG CHOICE: Show error and return to beginning ("balik sa umpisa")!
            challengeTimer = new javax.swing.Timer(1600, e -> {
                resetSimulation();
                if (canvas != null) {
                    canvas.setBannerMsg("[CHALLENGE RESTARTED]: Wrong alien! Read the policy rule and try again from the beginning!");
                }
            });
            challengeTimer.setRepeats(false);
            challengeTimer.start();
        }
    }

    private void updateHud(DiskStep ds, int stepIdx) {
        lblTotalMovement.setText(ds.cumulativeSeek + " CYLINDERS");
        lblTotalSub.setText("TOTAL SEEK // STEP " + (stepIdx + 1) + "/" + simulationSteps.size());

        int reqCount = cylinderRequests.length > 0 ? cylinderRequests.length : 1;
        double avg = (double) ds.cumulativeSeek / reqCount;
        lblAvgSeek.setText(String.format("%.2f CYL / REQ", avg));
        lblAvgSub.setText("AVERAGE SEEK LENGTH [LOW=BETTER]");

        lblCurrentHead.setText("CYLINDER #" + ds.toCyl);
        lblCurrentSub.setText("PAC-MAN HEAD // " + ds.direction);

        // Update footer summary
        if (lblFooterTotal != null) {
            lblFooterTotal.setText("TOTAL TRACKS TRAVERSED: " + ds.cumulativeSeek + " CYLINDERS");
            lblFooterReqs.setText("REQUESTS SERVICED: " + (stepIdx + 1) + " / " + simulationSteps.size());
            lblFooterAvg.setText(String.format("AVERAGE SEEK LENGTH: %.2f CYLINDERS", avg));
            lblFooterFormula.setText(String.format("FORMULA: %d cyl ÷ %d requests = %.2f cyl/req", ds.cumulativeSeek, reqCount, avg));
        }

        // Highlight active row in table and scroll to it
        if (traceTable != null) {
            if (stepIdx >= 0 && stepIdx < traceTable.getRowCount()) {
                traceTable.setRowSelectionInterval(stepIdx, stepIdx);
                traceTable.scrollRectToVisible(traceTable.getCellRect(stepIdx, 0, true));
            }
            traceTable.repaint();
        }
    }

    public boolean isAwaitingChallengeChoice() {
        return awaitingChallengeChoice;
    }

    // =========================================================================
    // 5. INPUT & PRESET HELPERS
    // =========================================================================
    public void setAlgorithm(String algo) {
        this.selectedAlgo = algo;
        updateAlgoButtons();
        if (algo.equals("FCFS")) {
            lblAlgoRule.setText("FCFS: Services disk cylinders strictly by arrival order.");
        } else if (algo.equals("SSTF")) {
            lblAlgoRule.setText("SSTF: Greedily moves to the nearest cylinder request.");
        } else if (algo.equals("SCAN")) {
            lblAlgoRule.setText("SCAN: Sweeps in direction to disk boundary, then reverses.");
        } else if (algo.equals("C-SCAN")) {
            lblAlgoRule.setText("C-SCAN: Sweeps to boundary, warps to opposite end, repeats.");
        } else if (algo.equals("LOOK")) {
            lblAlgoRule.setText("LOOK: Sweeps to farthest request in direction, then reverses.");
        } else if (algo.equals("C-LOOK")) {
            lblAlgoRule.setText("C-LOOK: Sweeps to highest request, warps to lowest request.");
        }
        recomputeSimulation();
    }

    private void updateAlgoButtons() {
        updateBtn(btnFcfs, "FCFS".equals(selectedAlgo));
        updateBtn(btnSstf, "SSTF".equals(selectedAlgo));
        updateBtn(btnScan, "SCAN".equals(selectedAlgo));
        updateBtn(btnCScan, "C-SCAN".equals(selectedAlgo));
        updateBtn(btnLook, "LOOK".equals(selectedAlgo));
        updateBtn(btnCLook, "C-LOOK".equals(selectedAlgo));
    }

    private void updateBtn(JButton b, boolean active) {
        if (b == null) return;
        b.setBackground(active ? new Color(0, 140, 200) : new Color(38, 25, 68));
        b.setForeground(active ? Color.WHITE : new Color(180, 180, 210));
    }

    public void loadRequestString(String str) {
        this.rawRequestString = str.trim();
        if (txtRequests != null) {
            isUpdatingQuietly = true;
            txtRequests.setText(rawRequestString);
            isUpdatingQuietly = false;
        }
        parseRequests();
    }

    private void parseRequests() {
        try {
            String[] tokens = rawRequestString.split("\\s+");
            List<Integer> list = new ArrayList<>();
            for (String t : tokens) {
                if (!t.isEmpty()) list.add(Integer.parseInt(t));
            }
            cylinderRequests = new int[list.size()];
            for (int i = 0; i < list.size(); i++) cylinderRequests[i] = list.get(i);
            recomputeSimulation();
        } catch (Exception ignored) {
            // Wait for complete numbers
        }
    }

    private void tryQuietStream() {
        if (isUpdatingQuietly || txtRequests == null) return;
        try {
            String s = txtRequests.getText().trim();
            if (s.isEmpty() || s.equals(this.rawRequestString)) return;
            this.rawRequestString = s;
            parseRequests();
        } catch (Exception ignored) {}
    }

    private void tryQuietConfig() {
        if (isUpdatingQuietly || txtInitHead == null || txtMaxCyl == null) return;
        try {
            int h = Integer.parseInt(txtInitHead.getText().trim());
            int m = Integer.parseInt(txtMaxCyl.getText().trim());
            if (m > 0 && h >= 0 && h <= m) {
                if (h == this.initialHead && m == this.maxCylinder) return;
                this.initialHead = h;
                this.maxCylinder = m;
                recomputeSimulation();
            }
        } catch (Exception ignored) {}
    }

    private void generateRandomStream() {
        Random rng = new Random();
        int max = maxCylinder > 0 ? maxCylinder : 199;
        StringBuilder sb = new StringBuilder();
        int len = 8 + rng.nextInt(5);
        for (int i = 0; i < len; i++) {
            sb.append(rng.nextInt(max + 1)).append(i == len - 1 ? "" : " ");
        }
        loadRequestString(sb.toString());
    }

    public void steerPacman(boolean goRight) {
        tryQuietConfig();
        tryQuietStream();
        if (canvas != null) {
            canvas.handleSteerDirection(goRight);
        }
    }

    public void launchPacmanArcadeGame() {
        tryQuietConfig();
        tryQuietStream();
        Window ancestor = SwingUtilities.getWindowAncestor(this);
        JFrame owner = (ancestor instanceof JFrame) ? (JFrame) ancestor : null;
        PacmanDiskArcadeDialog dlg = new PacmanDiskArcadeDialog(owner, cylinderRequests, initialHead, maxCylinder, selectedAlgo);
        dlg.setVisible(true);
    }

    private void setupKeyboardShortcuts() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "SPACE_PLAY");
        getActionMap().put("SPACE_PLAY", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!isShowing()) return;
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof javax.swing.text.JTextComponent) return;
                togglePlayPause();
            }
        });

        // STEER LEFT: Left Arrow (<-) / A Key
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "STEER_LEFT");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "STEER_LEFT");
        getActionMap().put("STEER_LEFT", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!isShowing()) return;
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof javax.swing.text.JTextComponent) return;
                steerPacman(false);
            }
        });

        // STEER RIGHT: Right Arrow (->) / D Key
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "STEER_RIGHT");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "STEER_RIGHT");
        getActionMap().put("STEER_RIGHT", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!isShowing()) return;
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof javax.swing.text.JTextComponent) return;
                steerPacman(true);
            }
        });

        // Auto-step: S key / Enter
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "KEY_STEP_AUTO");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "KEY_STEP_AUTO");
        getActionMap().put("KEY_STEP_AUTO", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!isShowing()) return;
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof javax.swing.text.JTextComponent) return;
                stepForward();
            }
        });

        // Reset: R key
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "KEY_RESET");
        getActionMap().put("KEY_RESET", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
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
    // 6. PAC-MAN DISK HEAD RUNNER & MAZE TUNNEL CANVAS
    // =========================================================================
    public static class PacmanDiskCanvas extends JPanel {
        private final DiskSchedulingPanel parent;

        private int currentStep = -1;
        private int fromCyl = 53;
        private int toCyl = 53;
        private int[] servicedCyls = new int[0];
        private boolean isWarp = false;
        private boolean isBoundary = false;
        private String bannerMsg = "STANDBY: Ready to simulate Pac-Man disk head traversal.";

        // Animation & Lerp
        private int animTick = 0;
        private float pacmanX = 0f;
        private float targetPacmanX = 0f;
        private float mouthAngle = 45f;
        private boolean facingRight = true;
        private float moveProgress = 1.0f; // 0.0 to 1.0 along active seek segment
        private boolean isAnimatingStep = false;

        // Challenge Mode Interaction
        private boolean challengeMode = false;
        private int challengeExpectedCyl = -1;
        private int hoveredTargetCyl = -1;
        private int clickedTargetCyl = -1;
        private int flashTimer = 0;
        private Color flashColor = new Color(0, 240, 100);
        private String flashTag = "";

        // Target Hitboxes along the track
        private final Map<Rectangle, Integer> targetHitboxes = new HashMap<>();
        private Rectangle leftControllerBox = null;
        private Rectangle rightControllerBox = null;

        public PacmanDiskCanvas(DiskSchedulingPanel parent) {
            this.parent = parent;
            setBackground(new Color(10, 8, 22));
            setOpaque(true);
            setBorder(BorderFactory.createLineBorder(new Color(65, 45, 95), 1));

            // Mouse Click & Hover for Interactive Challenge Mode
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (leftControllerBox != null && leftControllerBox.contains(e.getPoint())) {
                        handleSteerDirection(false);
                        return;
                    }
                    if (rightControllerBox != null && rightControllerBox.contains(e.getPoint())) {
                        handleSteerDirection(true);
                        return;
                    }
                    if (!challengeMode || !parent.isAwaitingChallengeChoice()) return;
                    for (Map.Entry<Rectangle, Integer> entry : targetHitboxes.entrySet()) {
                        if (entry.getKey().contains(e.getPoint())) {
                            handleTargetClick(entry.getValue());
                            return;
                        }
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if ((leftControllerBox != null && leftControllerBox.contains(e.getPoint())) ||
                        (rightControllerBox != null && rightControllerBox.contains(e.getPoint()))) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        return;
                    }
                    if (!challengeMode || !parent.isAwaitingChallengeChoice()) {
                        if (hoveredTargetCyl != -1) {
                            hoveredTargetCyl = -1;
                            setCursor(Cursor.getDefaultCursor());
                            repaint();
                        }
                        return;
                    }
                    int found = -1;
                    for (Map.Entry<Rectangle, Integer> entry : targetHitboxes.entrySet()) {
                        if (entry.getKey().contains(e.getPoint())) {
                            found = entry.getValue();
                            break;
                        }
                    }
                    if (found != hoveredTargetCyl) {
                        hoveredTargetCyl = found;
                        setCursor(found != -1 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                        repaint();
                    }
                }
            });

            // Smooth 30ms Animation Loop (Wakka-wakka mouth chomp, gliding & continuous eating motion)
            new javax.swing.Timer(30, e -> {
                animTick++;

                // Mouth Chomp Cycle
                mouthAngle = (float)(15 + Math.abs(Math.sin(animTick * 0.35)) * 48);

                // Flash timer
                if (flashTimer > 0) {
                    flashTimer--;
                    if (flashTimer == 0) clickedTargetCyl = -1;
                }

                // Smooth Glide lerp for 1D platter
                float diff = targetPacmanX - pacmanX;
                if (Math.abs(diff) > 0.5f) {
                    pacmanX += diff * 0.22f;
                } else {
                    pacmanX = targetPacmanX;
                }

                // Smooth Continuous Step Motion for 2D Trajectory Eating Pellets
                if (is2DTrajectoryView && isAnimatingStep) {
                    moveProgress += 0.045f * (float) parent.speedMultiplier;
                    if (moveProgress >= 1.0f) {
                        moveProgress = 1.0f;
                        isAnimatingStep = false;
                    }
                }

                repaint();
            }).start();
        }

        public void setChallengeMode(boolean c) {
            this.challengeMode = c;
            this.hoveredTargetCyl = -1;
            this.clickedTargetCyl = -1;
            setCursor(Cursor.getDefaultCursor());
            repaint();
        }

        private boolean is2DTrajectoryView = true;

        public void toggleViewMode() {
            this.is2DTrajectoryView = !this.is2DTrajectoryView;
            repaint();
        }

        public boolean is2DView() {
            return is2DTrajectoryView;
        }

        public void setView2D(boolean v) {
            this.is2DTrajectoryView = v;
            repaint();
        }

        public void setBannerMsg(String msg) {
            this.bannerMsg = msg;
            repaint();
        }

        public void updateChallengeState(int step, int from, int to, int[] serviced, boolean warp, boolean boundary, String msg) {
            this.currentStep = step;
            this.fromCyl = from;
            this.toCyl = from; // In challenge mode, Pac-Man stays at fromCyl until user clicks!
            this.servicedCyls = (serviced != null) ? serviced.clone() : new int[0];
            this.isWarp = false;
            this.isBoundary = false;
            this.challengeExpectedCyl = to;
            this.bannerMsg = msg != null ? msg : "";

            this.facingRight = (to >= from);
            this.moveProgress = 0.0f;
            this.isAnimatingStep = false; // Waiting for user choice
            updatePacmanTargetX();
            repaint();
        }

        public void updateState(int step, int from, int to, int[] serviced, boolean warp, boolean boundary, String msg) {
            this.currentStep = step;
            this.fromCyl = from;
            this.toCyl = to;
            this.servicedCyls = (serviced != null) ? serviced.clone() : new int[0];
            this.isWarp = warp;
            this.isBoundary = boundary;
            this.challengeExpectedCyl = -1;
            this.bannerMsg = msg != null ? msg : "";

            this.facingRight = (to >= from);
            if (step >= 0) {
                this.moveProgress = 0.0f;
                this.isAnimatingStep = true;
            } else {
                this.moveProgress = 1.0f;
                this.isAnimatingStep = false;
            }
            updatePacmanTargetX();
            repaint();
        }

        private void updatePacmanTargetX() {
            int w = getWidth();
            if (w <= 0) return;
            int trackStartX = 110;
            int trackEndX = w - 110;
            int trackW = trackEndX - trackStartX;
            int max = parent.maxCylinder > 0 ? parent.maxCylinder : 199;

            float ratio = (float) toCyl / max;
            targetPacmanX = trackStartX + ratio * trackW;
        }

        public void handleSteerDirection(boolean goRight) {
            if (challengeMode && parent.isAwaitingChallengeChoice()) {
                boolean expectedRight = (challengeExpectedCyl >= fromCyl);
                clickedTargetCyl = challengeExpectedCyl;
                flashTimer = 40;

                if (goRight == expectedRight) {
                    flashColor = new Color(0, 240, 100);
                    flashTag = "[*] CORRECT! STEERED " + (goRight ? "RIGHT (->)" : "LEFT (<-)") + " TO #" + challengeExpectedCyl + "! +50 XP [*]";
                    bannerMsg = "CORRECT! Steered " + (goRight ? "RIGHT" : "LEFT") + " to Cylinder #" + challengeExpectedCyl + " under " + parent.selectedAlgo + "! (+50 XP)";
                    AppState.addScore(50);

                    // Start continuous eating animation towards this cylinder!
                    this.toCyl = challengeExpectedCyl;
                    this.facingRight = (toCyl >= fromCyl);
                    this.moveProgress = 0.0f;
                    this.isAnimatingStep = true;
                    updatePacmanTargetX();

                    parent.onChallengeChoiceCompleted(true);
                } else {
                    flashColor = RetroTheme.ACCENT_PINK;
                    this.facingRight = goRight; // Pac-Man turns towards steered direction
                    flashTag = "[-] WRONG DIRECTION! " + parent.selectedAlgo + " SEEKS " + (expectedRight ? "RIGHT (->) #" + challengeExpectedCyl : "LEFT (<-) #" + challengeExpectedCyl) + "! RESETTING [-]";
                    bannerMsg = "DIRECTION ERROR! Under " + parent.selectedAlgo + ", Pac-Man must steer " + (expectedRight ? "RIGHT (->)" : "LEFT (<-)") + " to #" + challengeExpectedCyl + ", not " + (goRight ? "RIGHT" : "LEFT") + "! Resetting challenge...";

                    parent.onChallengeChoiceCompleted(false);
                }
                repaint();
                return;
            }

            // Normal Simulation Mode:
            if (parent.simulationSteps.isEmpty()) return;
            int nextIdx = currentStep + 1;
            if (nextIdx < parent.simulationSteps.size()) {
                DiskStep nextDs = parent.simulationSteps.get(nextIdx);
                boolean seekRight = (nextDs.toCyl >= nextDs.fromCyl);
                if (goRight == seekRight) {
                    parent.stepForward();
                } else {
                    flashTimer = 25;
                    flashColor = new Color(255, 180, 0);
                    flashTag = "[POLICY SEEK]: " + parent.selectedAlgo + " seeks " + (seekRight ? "RIGHT (->)" : "LEFT (<-)") + " to Cyl #" + nextDs.toCyl + "!";
                    bannerMsg = "HINT: " + parent.selectedAlgo + " is scheduled to seek " + (seekRight ? "RIGHT (->)" : "LEFT (<-)") + " to Cylinder #" + nextDs.toCyl + "!";
                    repaint();
                }
            } else if (currentStep >= parent.simulationSteps.size() - 1) {
                parent.stepForward();
            }
        }

        private void handleTargetClick(int cylinder) {
            if (!challengeMode || !parent.isAwaitingChallengeChoice()) return;

            clickedTargetCyl = cylinder;
            flashTimer = 35;

            boolean correct = (cylinder == challengeExpectedCyl);
            if (correct) {
                flashColor = new Color(0, 240, 100);
                flashTag = "[*] CORRECT! CHOMPING CYLINDER #" + cylinder + "! +50 XP [*]";
                bannerMsg = "CORRECT! Pac-Man sought Cylinder #" + cylinder + " under " + parent.selectedAlgo + "! (+50 XP)";
                AppState.addScore(50);

                // Start continuous eating animation towards this cylinder!
                this.toCyl = challengeExpectedCyl;
                this.facingRight = (toCyl >= fromCyl);
                this.moveProgress = 0.0f;
                this.isAnimatingStep = true;
                updatePacmanTargetX();

                parent.onChallengeChoiceCompleted(true);
            } else {
                flashColor = RetroTheme.ACCENT_PINK;
                flashTag = "[-] WRONG ALIEN (#" + cylinder + ")! EXPECTED #" + challengeExpectedCyl + "! RETURNING TO START [-]";
                bannerMsg = "POLICY ERROR! Under " + parent.selectedAlgo + ", Cylinder #" + challengeExpectedCyl + " was the correct choice! Resetting challenge...";

                parent.onChallengeChoiceCompleted(false);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            targetHitboxes.clear();

            if (is2DTrajectoryView) {
                paint2DTrajectory(g2, w, h);
            } else {
                paint1DPlatter(g2, w, h);
            }

            g2.dispose();
        }

        // =====================================================================
        // VIEW A: TEXTBOOK 2D DESCENDING SEEK TRAJECTORY GRAPH (PAC-MAN THEMED)
        // =====================================================================
        private void paint2DTrajectory(Graphics2D g2, int w, int h) {
            // Background Synthwave Grid
            g2.setColor(new Color(24, 18, 42));
            for (int y = 0; y < h; y += 22) g2.drawLine(0, y, w, y);
            for (int x = 0; x < w; x += 32) g2.drawLine(x, 0, x, h);

            int startX = 70;
            int endX = w - 70;
            int trackW = endX - startX;
            int maxCyl = parent.maxCylinder > 0 ? parent.maxCylinder : 199;

            // 1. Top Queue Header Box
            g2.setColor(new Color(18, 12, 34, 240));
            g2.fillRoundRect(startX - 15, 6, trackW + 30, 22, 5, 5);
            g2.setColor(new Color(65, 45, 95));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(startX - 15, 6, trackW + 30, 22, 5, 5);

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("QUEUE = [ " + parent.rawRequestString + " ]", startX - 5, 21);

            g2.setColor(RetroTheme.ACCENT_YELLOW);
            String headInfo = "HEAD STARTS AT " + parent.initialHead;
            FontMetrics hfm = g2.getFontMetrics();
            int hX = (w - hfm.stringWidth(headInfo)) / 2;
            g2.drawString(headInfo, hX, 21);

            g2.setColor(new Color(0, 240, 100));
            String polInfo = "POLICY: " + parent.selectedAlgo;
            FontMetrics pfm = g2.getFontMetrics();
            g2.drawString(polInfo, endX + 15 - pfm.stringWidth(polInfo), 21);

            // 2. Top Ruler Axis
            int rulerY = 56;
            g2.setColor(new Color(33, 33, 222)); // Pac-Man Neon Blue Rail
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawLine(startX - 4, rulerY, endX + 4, rulerY);

            // Collect and sort unique cylinders for ruler axis
            TreeSet<Integer> uniqueSet = new TreeSet<>();
            uniqueSet.add(0);
            uniqueSet.add(maxCyl);
            uniqueSet.add(parent.initialHead);
            for (int r : parent.cylinderRequests) {
                if (r >= 0 && r <= maxCyl) uniqueSet.add(r);
            }
            List<Integer> sortedCyls = new ArrayList<>(uniqueSet);

            for (int i = 0; i < sortedCyls.size(); i++) {
                int cyl = sortedCyls.get(i);
                float rx = startX + ((float) cyl / maxCyl) * trackW;
                int irx = Math.round(rx);

                // Tick line on ruler
                g2.setColor(new Color(120, 100, 180));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawLine(irx, rulerY - 5, irx, rulerY + 5);

                // Faint vertical lane line dropping down through maze
                g2.setColor(new Color(45, 30, 75, 110));
                g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3f, 6f}, 0f));
                g2.drawLine(irx, rulerY + 6, irx, h - 38);

                // Stagger labels on 2 vertical levels so close numbers never collide
                boolean upper = (i % 2 == 1);
                int lblY = upper ? (rulerY - 14) : (rulerY - 26);

                // Stem tick line to label
                g2.setColor(new Color(65, 45, 95));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawLine(irx, lblY + 2, irx, rulerY - 5);

                String sCyl = String.valueOf(cyl);
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                FontMetrics cfm = g2.getFontMetrics();
                int lx = irx - cfm.stringWidth(sCyl) / 2;

                if (cyl == parent.initialHead) {
                    g2.setColor(RetroTheme.ACCENT_YELLOW);
                } else if (cyl == 0 || cyl == maxCyl) {
                    g2.setColor(RetroTheme.ACCENT_CYAN);
                } else {
                    g2.setColor(Color.WHITE);
                }
                g2.drawString(sCyl, lx, lblY);

                // Register Ruler Hitbox for Challenge Mode
                if (challengeMode && parent.isAwaitingChallengeChoice()) {
                    targetHitboxes.put(new Rectangle(irx - 14, rulerY - 32, 28, 42), cyl);
                    if (hoveredTargetCyl == cyl) {
                        g2.setColor(new Color(255, 238, 0));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRect(irx - 14, rulerY - 32, 28, 42);
                    }
                }
            }

            // 3. 2D Descending Trajectory
            int numSteps = parent.simulationSteps.size();
            int graphStartY = 76;
            int graphEndY = h - 42;
            float stepH = (numSteps > 0) ? (graphEndY - graphStartY) / (float) numSteps : 20f;

            // Pass 1: Traversal Lines, Directional Arrows & Food Pellets
            for (int i = 0; i < numSteps; i++) {
                DiskStep ds = parent.simulationSteps.get(i);
                float x1 = startX + ((float) ds.fromCyl / maxCyl) * trackW;
                float y1 = graphStartY + i * stepH;
                float x2 = startX + ((float) ds.toCyl / maxCyl) * trackW;
                float y2 = graphStartY + (i + 1) * stepH;

                boolean isTraversed = (i < currentStep);
                boolean isCurrent = (i == currentStep);

                if (ds.isWarp) {
                    // Hyper-Warp pink dashed line
                    g2.setColor(new Color(255, 60, 160, (isTraversed || isCurrent) ? 240 : 80));
                    float wPhase = Math.abs((12.0f - ((animTick * 2.0f) % 12.0f)) % 12.0f);
                    g2.setStroke(new BasicStroke((isTraversed || isCurrent) ? 2.5f : 1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{6f, 6f}, (isTraversed || isCurrent) ? wPhase : 0f));
                    g2.drawLine((int) x1, (int) y1, (int) x2, (int) y2);

                    int mx = (int) (x1 + x2) / 2;
                    int my = (int) (y1 + y2) / 2;
                    g2.setColor(new Color(18, 12, 34, 240));
                    g2.fillRoundRect(mx - 32, my - 7, 64, 14, 3, 3);
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(mx - 32, my - 7, 64, 14, 3, 3);
                    g2.setFont(new Font("Consolas", Font.BOLD, 8));
                    g2.drawString("<< WARP <<", mx - 24, my + 4);

                } else if (isTraversed) {
                    // Fully traversed past step: solid cyan seek path with arrow
                    g2.setColor(new Color(0, 240, 255, 60));
                    g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine((int) x1, (int) y1, (int) x2, (int) y2);

                    g2.setColor(new Color(0, 240, 255));
                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine((int) x1, (int) y1, (int) x2, (int) y2);

                    drawArrowHead(g2, x1, y1, x2, y2, new Color(0, 240, 255));

                } else if (isCurrent) {
                    // ACTIVE STEP BEING CHOMPED: Draw seek path and food pellets!
                    g2.setColor(new Color(255, 238, 0, 70));
                    g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine((int) x1, (int) y1, (int) x2, (int) y2);

                    g2.setColor(new Color(255, 238, 0, 180));
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine((int) x1, (int) y1, (int) x2, (int) y2);

                    // FOOD PELLETS along the line: Pac-Man eats them as moveProgress increases!
                    double segDist = Math.hypot(x2 - x1, y2 - y1);
                    int numPellets = Math.max(4, (int) (segDist / 16.0));
                    for (int p = 1; p < numPellets; p++) {
                        float t = (float) p / numPellets;
                        if (t > moveProgress) {
                            float px = x1 + (x2 - x1) * t;
                            float py = y1 + (y2 - y1) * t;
                            // Uneaten classic Pac-Man food pellet
                            g2.setColor(new Color(255, 184, 151));
                            g2.fillOval((int) px - 3, (int) py - 3, 6, 6);
                            g2.setColor(new Color(255, 238, 0, 180));
                            g2.fillOval((int) px - 1, (int) py - 1, 2, 2);
                        }
                    }

                    // Arrow head at 65% of segment
                    drawArrowHead(g2, x1, y1, x2, y2, new Color(255, 238, 0));

                } else {
                    // Future planned path: subtle dotted guide line
                    g2.setColor(new Color(65, 45, 95));
                    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{4f, 4f}, 0f));
                    g2.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
                }
            }

            // Pass 2: Nodes (Eaten Pellets vs Alive Aliens / Ghosts)
            for (int k = 0; k <= numSteps; k++) {
                int cyl = (k == 0) ? parent.initialHead : parent.simulationSteps.get(k - 1).toCyl;
                float nx = startX + ((float) cyl / maxCyl) * trackW;
                float ny = graphStartY + k * stepH;
                int inx = Math.round(nx);
                int iny = Math.round(ny);

                boolean isPastVisited = (k <= currentStep && (k < currentStep || moveProgress >= 1.0f));

                if (isPastVisited) {
                    // Eaten node: classic green dot with checkmark
                    g2.setColor(new Color(0, 240, 100));
                    g2.fillOval(inx - 5, iny - 5, 10, 10);
                    g2.setColor(new Color(12, 8, 24));
                    g2.setFont(new Font("Consolas", Font.BOLD, 8));
                    g2.drawString("✓", inx - 2, iny + 3);

                } else if (k > 0) {
                    // Alive Target Alien / Ghost waiting to be chomped
                    int ghostIdx = (k - 1) % 4;
                    int spriteSize = 18;
                    BufferedImage spr = (k % 4 == 0) ? PixelSprites.createPacmanCherry(spriteSize) : PixelSprites.createPacmanGhost(spriteSize, ghostIdx);
                    if (spr != null) {
                        float bob = (float)(Math.sin((animTick + k * 2) * 0.2) * 2);
                        g2.drawImage(spr, inx - spriteSize / 2, (int)(iny - spriteSize / 2 + bob), null);
                    }
                    g2.setFont(new Font("Consolas", Font.BOLD, 8));
                    g2.setColor(new Color(200, 190, 230));
                    g2.drawString("#" + cyl, inx + 10, iny + 3);

                    // IN CHALLENGE MODE: Make ALL upcoming unserviced aliens clickable!
                    if (challengeMode && parent.isAwaitingChallengeChoice() && k > currentStep) {
                        targetHitboxes.put(new Rectangle(inx - 20, iny - 20, 40, 40), cyl);

                        // Highlight hovered alien with targeting reticle and prompt
                        if (hoveredTargetCyl == cyl) {
                            g2.setColor(new Color(255, 238, 0));
                            float rPhase = (animTick * 2.0f) % 8.0f;
                            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{4f, 4f}, rPhase));
                            g2.drawOval(inx - 18, iny - 18, 36, 36);

                            int hbw = 140;
                            g2.setColor(new Color(18, 12, 34, 250));
                            g2.fillRoundRect(inx - hbw / 2, iny - 38, hbw, 18, 4, 4);
                            g2.setColor(RetroTheme.ACCENT_YELLOW);
                            g2.setStroke(new BasicStroke(1.2f));
                            g2.drawRoundRect(inx - hbw / 2, iny - 38, hbw, 18, 4, 4);
                            g2.setFont(new Font("Consolas", Font.BOLD, 8));
                            String hTxt = ">> CHOMP ALIEN #" + cyl + " <<";
                            FontMetrics thfm = g2.getFontMetrics();
                            g2.drawString(hTxt, inx - thfm.stringWidth(hTxt) / 2, iny - 26);
                        }
                    }
                }
            }

            // Pass 3: DYNAMIC CHOMPING PAC-MAN (Smooth Continuous Eating Along Traversal)
            float pacX, pacY;
            boolean pacFacingRight;
            int displayHeadCyl;

            if (currentStep >= 0 && currentStep < numSteps) {
                DiskStep curDs = parent.simulationSteps.get(currentStep);
                float fx = startX + ((float) curDs.fromCyl / maxCyl) * trackW;
                float fy = graphStartY + currentStep * stepH;
                float tx = startX + ((float) curDs.toCyl / maxCyl) * trackW;
                float ty = graphStartY + (currentStep + 1) * stepH;

                pacX = fx + (tx - fx) * moveProgress;
                pacY = fy + (ty - fy) * moveProgress;
                pacFacingRight = (tx >= fx);
                displayHeadCyl = (moveProgress >= 0.95f) ? curDs.toCyl : curDs.fromCyl;
            } else {
                pacX = startX + ((float) parent.initialHead / maxCyl) * trackW;
                pacY = graphStartY;
                pacFacingRight = parent.initialDirectionUp;
                displayHeadCyl = parent.initialHead;
            }

            int pacSize = 28;
            BufferedImage pacImg = PixelSprites.createPacman(pacSize, mouthAngle, pacFacingRight);
            if (pacImg != null) {
                g2.drawImage(pacImg, (int) pacX - pacSize / 2, (int) pacY - pacSize / 2, null);
            }

            // Glowing Aura around Pac-Man
            g2.setColor(new Color(255, 238, 0, 160));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval((int) pacX - 16, (int) pacY - 16, 32, 32);

            // Floating Head Badge above Pac-Man
            int headBadgeW = 66;
            int headBadgeH = 15;
            int headBadgeX = (int) pacX - headBadgeW / 2;
            int headBadgeY = (int) pacY - 26;
            g2.setColor(new Color(18, 12, 34, 240));
            g2.fillRoundRect(headBadgeX, headBadgeY, headBadgeW, headBadgeH, 4, 4);
            g2.setColor(new Color(255, 238, 0));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(headBadgeX, headBadgeY, headBadgeW, headBadgeH, 4, 4);
            g2.setFont(new Font("Consolas", Font.BOLD, 8));
            g2.drawString("HEAD: #" + displayHeadCyl, headBadgeX + 6, headBadgeY + 11);

            // Controller Guidance Arrows around Pac-Man when waiting for direction choice
            if (challengeMode && parent.isAwaitingChallengeChoice()) {
                float cPulse = (float)(0.75 + 0.25 * Math.sin(animTick * 0.25));
                int arrowW = 86;
                int arrowH = 22;

                // Left Arrow Box (to the left of Pac-Man)
                int leftX = (int) pacX - arrowW - 20;
                int leftY = (int) pacY - arrowH / 2;
                leftControllerBox = new Rectangle(leftX, leftY, arrowW, arrowH);

                g2.setColor(new Color(15, 10, 30, 245));
                g2.fillRoundRect(leftX, leftY, arrowW, arrowH, 5, 5);
                g2.setColor(new Color(0, 220, 255, (int)(255 * cPulse)));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(leftX, leftY, arrowW, arrowH, 5, 5);
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.drawString("◄ [<- LEFT]", leftX + 8, leftY + 15);

                // Right Arrow Box (to the right of Pac-Man)
                int rightX = (int) pacX + 22;
                int rightY = (int) pacY - arrowH / 2;
                rightControllerBox = new Rectangle(rightX, rightY, arrowW, arrowH);

                g2.setColor(new Color(15, 10, 30, 245));
                g2.fillRoundRect(rightX, rightY, arrowW, arrowH, 5, 5);
                g2.setColor(new Color(255, 220, 0, (int)(255 * cPulse)));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(rightX, rightY, arrowW, arrowH, 5, 5);
                g2.drawString("[RIGHT ->] ►", rightX + 8, rightY + 15);

                // Top Floating Guidance Prompt
                String guide = "USE CONTROLLER [<-] / [->] TO STEER PAC-MAN!";
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                FontMetrics gfm = g2.getFontMetrics();
                int gWidth = gfm.stringWidth(guide) + 16;
                int gx = (int) pacX - gWidth / 2;
                int gy = (int) pacY - 38;
                g2.setColor(new Color(18, 12, 34, 240));
                g2.fillRoundRect(gx, gy, gWidth, 16, 4, 4);
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawRoundRect(gx, gy, gWidth, 16, 4, 4);
                g2.drawString(guide, gx + 8, gy + 12);
            } else {
                leftControllerBox = null;
                rightControllerBox = null;
            }

            // 4. Bottom Illustration Shows Total Head Movement Banner
            int bannerY = h - 16;
            int totalMov = (currentStep >= 0 && currentStep < numSteps) ? parent.simulationSteps.get(currentStep).cumulativeSeek : (numSteps > 0 ? parent.simulationSteps.get(numSteps - 1).cumulativeSeek : 0);
            int reqCount = parent.cylinderRequests.length > 0 ? parent.cylinderRequests.length : 1;
            double avgSeek = (double) totalMov / reqCount;
            boolean isCompleted = (numSteps > 0 && currentStep == numSteps - 1 && moveProgress >= 1.0f);
            String banner = isCompleted
                    ? "[*] LEVEL CLEAR! TOTAL HEAD MOVEMENT: " + totalMov + " CYLINDERS // AVERAGE SEEK LENGTH: " + String.format("%.1f", avgSeek) + " CYL/REQ [*]"
                    : "[*] ILLUSTRATION SHOWS TOTAL HEAD MOVEMENT OF " + totalMov + " CYLINDERS // AVERAGE SEEK LENGTH: " + String.format("%.1f", avgSeek) + " CYL/REQ [*]";
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            FontMetrics bfm = g2.getFontMetrics();
            int bw = bfm.stringWidth(banner) + 24;
            int bx = Math.max(16, (w - bw) / 2);
            g2.setColor(new Color(18, 12, 34, 245));
            g2.fillRoundRect(bx, bannerY - 12, bw, 18, 5, 5);
            g2.setColor(isCompleted ? new Color(255, 220, 0) : new Color(0, 240, 180));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(bx, bannerY - 12, bw, 18, 5, 5);
            g2.drawString(banner, bx + 12, bannerY + 1);

            // 5. Level Clear Arcade Victory Overlay
            if (isCompleted) {
                int vicW = Math.min(480, w - 80);
                int vicH = 58;
                int vicX = (w - vicW) / 2;
                int vicY = (graphStartY + graphEndY) / 2 - vicH / 2;

                // Pulsing glow background & border
                float vicPulse = (float)(0.75 + 0.25 * Math.sin(animTick * 0.15));
                g2.setColor(new Color(15, 10, 28, 245));
                g2.fillRoundRect(vicX, vicY, vicW, vicH, 10, 10);

                g2.setColor(new Color(255, 220, 0, (int)(255 * vicPulse)));
                float vPhase = (animTick * 1.5f) % 8.0f;
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{6f, 4f}, vPhase));
                g2.drawRoundRect(vicX, vicY, vicW, vicH, 10, 10);

                // Animated Victory Cherries on left and right
                BufferedImage vicCherry = PixelSprites.createPacmanCherry(26);
                if (vicCherry != null) {
                    float cherryBob = (float)(Math.sin(animTick * 0.2) * 3);
                    g2.drawImage(vicCherry, vicX + 16, (int)(vicY + 16 + cherryBob), null);
                    g2.drawImage(vicCherry, vicX + vicW - 42, (int)(vicY + 16 - cherryBob), null);
                }

                g2.setFont(new Font("Consolas", Font.BOLD, 14));
                g2.setColor(new Color(255, 220, 0));
                String vTitle = "[*] LEVEL CLEAR! ALL CYLINDERS SERVICED [*]";
                FontMetrics vfm = g2.getFontMetrics();
                g2.drawString(vTitle, vicX + (vicW - vfm.stringWidth(vTitle)) / 2, vicY + 26);

                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(RetroTheme.ACCENT_CYAN);
                String vSub = "SEEK TRAVERSAL COMPLETED PERFECTLY // +100 SCORE BONUS";
                FontMetrics vsfm = g2.getFontMetrics();
                g2.drawString(vSub, vicX + (vicW - vsfm.stringWidth(vSub)) / 2, vicY + 45);
            }

            // Screen Flash & Banner Feedback in Challenge Mode
            if (flashTimer > 0) {
                if (flashColor == RetroTheme.ACCENT_PINK) {
                    // Full Red Screen Error Tint when wrong alien is clicked!
                    g2.setColor(new Color(255, 30, 80, Math.min(65, flashTimer * 2)));
                    g2.fillRect(0, 0, w, h);
                }

                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                FontMetrics tfm = g2.getFontMetrics();
                int tagW = tfm.stringWidth(flashTag) + 24;
                int tagX = (w - tagW) / 2;

                g2.setColor(new Color(12, 8, 24, 250));
                g2.fillRoundRect(tagX, 34, tagW, 22, 6, 6);
                g2.setColor(flashColor);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(tagX, 34, tagW, 22, 6, 6);
                g2.drawString(flashTag, tagX + 12, 49);
            }
        }

        private void drawArrowHead(Graphics2D g2, float x1, float y1, float x2, float y2, Color color) {
            double dx = x2 - x1;
            double dy = y2 - y1;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 10) return;

            double angle = Math.atan2(dy, dx);
            int arrowLen = 9;
            float mx = x1 + (float) dx * 0.65f;
            float my = y1 + (float) dy * 0.65f;

            int xA = (int) (mx - arrowLen * Math.cos(angle - Math.PI / 6));
            int yA = (int) (my - arrowLen * Math.sin(angle - Math.PI / 6));
            int xB = (int) (mx - arrowLen * Math.cos(angle + Math.PI / 6));
            int yB = (int) (my - arrowLen * Math.sin(angle + Math.PI / 6));

            g2.setColor(color);
            Polygon arrow = new Polygon();
            arrow.addPoint((int) mx, (int) my);
            arrow.addPoint(xA, yA);
            arrow.addPoint(xB, yB);
            g2.fill(arrow);
        }

        // =====================================================================
        // VIEW B: 1D LINEAR DISK PLATTER (WARP TUNNEL & MAZE RAILS)
        // =====================================================================
        private void paint1DPlatter(Graphics2D g2, int w, int h) {
            // Background Synthwave Grid
            g2.setColor(new Color(24, 18, 42));
            for (int y = 0; y < h; y += 22) g2.drawLine(0, y, w, y);
            for (int x = 0; x < w; x += 32) g2.drawLine(x, 0, x, h);

            int trackStartX = 110;
            int trackEndX = w - 110;
            int trackW = trackEndX - trackStartX;
            int trackY = 120;
            int maxCyl = parent.maxCylinder > 0 ? parent.maxCylinder : 199;

            // Target Hitboxes registry
            targetHitboxes.clear();

            // 1. LEFT & RIGHT NEON BLUE WARP TUNNELS
            int tunnelW = 85;
            int tunnelH = 80;
            int tunnelY = trackY - tunnelH / 2;

            // Left Warp Tunnel (Cylinder 0)
            int leftTunnelX = 12;
            g2.setColor(new Color(15, 10, 32));
            g2.fillRoundRect(leftTunnelX, tunnelY, tunnelW, tunnelH, 8, 8);
            g2.setColor(new Color(33, 33, 222)); // Pac-Man Neon Blue
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(leftTunnelX, tunnelY, tunnelW, tunnelH, 8, 8);
            g2.setColor(new Color(0, 0, 80));
            g2.fillRoundRect(leftTunnelX + tunnelW - 14, tunnelY + 12, 14, tunnelH - 24, 6, 6);
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("WARP 0", leftTunnelX + 16, tunnelY + 28);
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            g2.setColor(new Color(0, 240, 100));
            g2.drawString("TUNNEL", leftTunnelX + 16, tunnelY + 44);
            g2.setColor(Color.WHITE);
            g2.drawString("CYL 0", leftTunnelX + 22, tunnelY + 62);

            // Right Warp Tunnel (Max Cylinder)
            int rightTunnelX = w - tunnelW - 12;
            g2.setColor(new Color(15, 10, 32));
            g2.fillRoundRect(rightTunnelX, tunnelY, tunnelW, tunnelH, 8, 8);
            g2.setColor(new Color(33, 33, 222));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(rightTunnelX, tunnelY, tunnelW, tunnelH, 8, 8);
            g2.setColor(new Color(0, 0, 80));
            g2.fillRoundRect(rightTunnelX, tunnelY + 12, 14, tunnelH - 24, 6, 6);
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("WARP MAX", rightTunnelX + 16, tunnelY + 28);
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            g2.setColor(new Color(0, 240, 100));
            g2.drawString("TUNNEL", rightTunnelX + 22, tunnelY + 44);
            g2.setColor(Color.WHITE);
            g2.drawString("CYL " + maxCyl, rightTunnelX + 18, tunnelY + 62);

            // 2. PAC-MAN MAZE RAILS & RULER TICKS
            g2.setColor(new Color(33, 33, 222));
            g2.setStroke(new BasicStroke(3.0f));
            g2.drawLine(trackStartX - 6, trackY - 20, trackEndX + 6, trackY - 20);
            g2.drawLine(trackStartX - 6, trackY + 20, trackEndX + 6, trackY + 20);

            // Center dotted pellet guide line
            g2.setColor(new Color(255, 184, 151, 100));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{4f, 8f}, 0f));
            g2.drawLine(trackStartX, trackY, trackEndX, trackY);

            // Ruler Calibration Ticks
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            int stepInterval = (maxCyl <= 100) ? 10 : (maxCyl <= 300 ? 25 : 50);
            for (int tick = 0; tick <= maxCyl; tick += stepInterval) {
                float rx = trackStartX + ((float) tick / maxCyl) * trackW;
                g2.setColor(new Color(65, 50, 100));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawLine((int) rx, trackY + 22, (int) rx, trackY + 28);

                String tStr = String.valueOf(tick);
                FontMetrics rfm = g2.getFontMetrics();
                g2.setColor(new Color(160, 150, 190));
                g2.drawString(tStr, (int) rx - rfm.stringWidth(tStr) / 2, trackY + 38);
            }

            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("0", trackStartX - 3, trackY + 38);
            String mStr = String.valueOf(maxCyl);
            FontMetrics mfm = g2.getFontMetrics();
            g2.drawString(mStr, trackEndX - mfm.stringWidth(mStr) / 2, trackY + 38);

            // 3. TARGET CYLINDERS (GHOSTS & PAC-MAN FRUITS)
            int[] reqs = parent.cylinderRequests;
            Set<Integer> servicedSet = new HashSet<>();
            for (int s : servicedCyls) servicedSet.add(s);

            for (int i = 0; i < reqs.length; i++) {
                int cyl = reqs[i];
                float cx = trackStartX + ((float) cyl / maxCyl) * trackW;
                int cy = trackY;
                boolean isServiced = servicedSet.contains(cyl);

                Rectangle tBox = new Rectangle((int) cx - 16, cy - 38, 32, 54);
                targetHitboxes.put(tBox, cyl);

                boolean isHovered = (challengeMode && hoveredTargetCyl == cyl);

                int bW = 28;
                int bH = 14;
                int bX = (int) cx - bW / 2;
                int bY = cy - 36;

                g2.setColor(new Color(15, 10, 30, 240));
                g2.fillRoundRect(bX, bY, bW, bH, 4, 4);
                g2.setColor(isHovered ? RetroTheme.ACCENT_YELLOW : (isServiced ? new Color(0, 160, 80) : RetroTheme.ACCENT_CYAN));
                g2.setStroke(new BasicStroke(isHovered ? 1.8f : 1.0f));
                g2.drawRoundRect(bX, bY, bW, bH, 4, 4);

                g2.setFont(new Font("Consolas", Font.BOLD, 8));
                FontMetrics cbfm = g2.getFontMetrics();
                String cStr = String.valueOf(cyl);
                g2.setColor(isServiced ? new Color(140, 240, 160) : Color.WHITE);
                g2.drawString(cStr, (int) cx - cbfm.stringWidth(cStr) / 2, bY + 10);

                int spriteSize = 22;
                int sprX = (int) cx - spriteSize / 2;
                int sprY = cy - spriteSize / 2;

                if (isServiced) {
                    g2.setColor(new Color(0, 240, 100, 140));
                    g2.fillOval((int) cx - 4, cy - 4, 8, 8);
                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    g2.drawString("✓", (int) cx - 3, cy + 16);
                } else {
                    BufferedImage spr;
                    if (i % 5 == 4) {
                        spr = PixelSprites.createPacmanCherry(spriteSize);
                    } else {
                        spr = PixelSprites.createPacmanGhost(spriteSize, i);
                    }
                    if (spr != null) {
                        float bob = (float)(Math.sin((animTick + i * 2) * 0.2) * 3);
                        g2.drawImage(spr, sprX, (int)(sprY + bob), null);
                    }
                }

                if (challengeMode && parent.isAwaitingChallengeChoice()) {
                    if (isHovered) {
                        g2.setColor(new Color(252, 228, 0));
                        g2.setFont(new Font("Consolas", Font.BOLD, 8));
                        String hStr = ">> CHOMP <<";
                        FontMetrics hfm = g2.getFontMetrics();
                        g2.drawString(hStr, (int) cx - hfm.stringWidth(hStr) / 2, cy + 26);
                    }
                }
            }

            // 4. SEEK TRAVERSAL TRAIL & WARP ARCS
            if (currentStep >= 0) {
                float x1 = trackStartX + ((float) fromCyl / maxCyl) * trackW;
                float x2 = trackStartX + ((float) toCyl / maxCyl) * trackW;

                if (isWarp) {
                    int startWarpX = (fromCyl >= toCyl) ? (int) x1 : (int) x2;
                    int endWarpX = (fromCyl >= toCyl) ? (int) x2 : (int) x1;

                    QuadCurve2D.Float warpArc = new QuadCurve2D.Float(
                            startWarpX, trackY - 22,
                            (startWarpX + endWarpX) / 2, 28,
                            endWarpX, trackY - 22
                    );

                    g2.setColor(new Color(255, 60, 160, 100));
                    g2.setStroke(new BasicStroke(6.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(warpArc);

                    g2.setColor(RetroTheme.ACCENT_PINK);
                    g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(warpArc);

                    float wPhase = Math.abs((14.0f - ((animTick * 2.5f) % 14.0f)) % 14.0f);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{8f, 6f}, wPhase));
                    g2.draw(warpArc);

                    int pillW = 150;
                    int pillX = (w - pillW) / 2;
                    g2.setColor(new Color(15, 10, 30, 245));
                    g2.fillRoundRect(pillX, 36, pillW, 18, 5, 5);
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(pillX, 36, pillW, 18, 5, 5);
                    g2.setFont(new Font("Consolas", Font.BOLD, 9));
                    g2.drawString(">> C-SCAN WARP TUNNEL <<", pillX + 10, 49);

                } else {
                    g2.setColor(new Color(255, 238, 0, 110));
                    g2.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine((int) x1, trackY, (int) x2, trackY);

                    g2.setColor(new Color(255, 238, 0));
                    g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine((int) x1, trackY, (int) x2, trackY);
                }
            }

            // 5. ANIMATED PAC-MAN DISK HEAD SPRITE
            if (pacmanX == 0f) {
                float ratio = (float) toCyl / maxCyl;
                pacmanX = trackStartX + ratio * trackW;
                targetPacmanX = pacmanX;
            }

            int pacSize = 34;
            BufferedImage pacImg = PixelSprites.createPacman(pacSize, mouthAngle, facingRight);
            if (pacImg != null) {
                int px = (int) pacmanX - pacSize / 2;
                int py = trackY - pacSize / 2;
                g2.drawImage(pacImg, px, py, null);
            }

            int headBadgeW = 86;
            int headBadgeX = (int) pacmanX - headBadgeW / 2;
            int headBadgeY = trackY - 48;
            g2.setColor(new Color(18, 12, 34, 240));
            g2.fillRoundRect(headBadgeX, headBadgeY, headBadgeW, 18, 4, 4);
            g2.setColor(new Color(255, 238, 0));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(headBadgeX, headBadgeY, headBadgeW, 18, 4, 4);

            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            g2.drawString("HEAD: #" + toCyl, headBadgeX + 8, headBadgeY + 12);

            if (flashTimer > 0) {
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                FontMetrics tfm = g2.getFontMetrics();
                int tagW = tfm.stringWidth(flashTag) + 16;
                int tagX = (w - tagW) / 2;

                g2.setColor(new Color(12, 8, 24, 250));
                g2.fillRoundRect(tagX, 36, tagW, 20, 6, 6);
                g2.setColor(flashColor);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(tagX, 36, tagW, 20, 6, 6);
                g2.drawString(flashTag, tagX + 8, 50);
            }

            int bannerY = h - 16;
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            FontMetrics bfm = g2.getFontMetrics();
            int bw = bfm.stringWidth(bannerMsg) + 24;
            int bx = Math.max(16, (w - bw) / 2);

            g2.setColor(new Color(18, 12, 34, 245));
            g2.fillRoundRect(bx, bannerY - 13, bw, 20, 6, 6);
            g2.setColor(challengeMode ? RetroTheme.ACCENT_YELLOW : new Color(0, 240, 180));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(bx, bannerY - 13, bw, 20, 6, 6);
            g2.drawString(bannerMsg, bx + 12, bannerY + 2);
        }
    }
}
