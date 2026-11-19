package arcadeos;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * MISSION 08: MULTIPROGRAMMING & CONCURRENCY
 * THEME: RETRO OVERCOOKED KITCHEN DISPATCHER
 *
 * Implements the EXACT Classroom Multiprogramming Processor Sharing Model:
 *  - GIVEN: Table of JOB#, AT (Arrival Time), BT (Burst Time)
 *  - Formulas from Classroom Syllabus:
 *      • time: arrival time (if arrive = ar); terminate time (if terminate = et + prev time)
 *      • event: a/s (arrive & started); t (terminate)
 *      • %cpu/nj: = 1 / nj (equal CPU sharing among active jobs)
 *      • et: = present time - prev time (elapsed time)
 *      • hw/job: = et * (%cpu/nj) (work performed per active job)
 *      • timeleft: = prev timeleft - hw/job (remaining CPU burst)
 *      • TAT = FT - AT; WT = TAT - BT; Average TAT & WT
 */
public class MultiprogrammingPanel extends JPanel {

    // Data Model: Input Job Definition
    public static class JobInput {
        public String name;
        public double at;
        public double bt;
        public int dishType; // 0=Burger, 1=Pizza, 2=Ramen, 3=Sushi, 4=Steak, 5=Cake

        public JobInput(String name, double at, double bt, int dishType) {
            this.name = name;
            this.at = at;
            this.bt = bt;
            this.dishType = dishType;
        }

        public JobInput copy() {
            return new JobInput(name, at, bt, dishType);
        }
    }

    public static class BlockJobItem {
        public String name;
        public double timeLeft;
        public BlockJobItem(String name, double timeLeft) {
            this.name = name;
            this.timeLeft = timeLeft;
        }
    }

    // Data Model: Event Trace Step in the Multiprogramming Execution Table
    public static class EventStep {
        public double time;
        public String event; // "a/s", "t"
        public String details; // e.g. "J3 arrives" or "J5 terminates"
        public int nj; // Number of jobs active during this interval
        public double cpuPct; // 1 / nj
        public double et; // present time - prev time
        public double hwJob; // et * (%cpu/nj)
        public Map<String, Double> timeLeftMap = new LinkedHashMap<>(); // Remaining time per job
        public Set<String> activeJobNames = new LinkedHashSet<>();
        public Set<String> terminatedThisStep = new LinkedHashSet<>();
        public List<String> eventList = new ArrayList<>();
        public List<BlockJobItem> intervalJobs = new ArrayList<>();
    }

    // Data Model: Final Job Summary
    public static class JobSummary {
        public String name;
        public double at;
        public double bt;
        public double ft; // Finish / Terminate time
        public double tat; // Turnaround Time = FT - AT
        public double wt; // Waiting Time = TAT - BT
        public int dishType;

        public JobSummary(String name, double at, double bt, double ft, double tat, double wt, int dishType) {
            this.name = name;
            this.at = at;
            this.bt = bt;
            this.ft = ft;
            this.tat = tat;
            this.wt = wt;
            this.dishType = dishType;
        }
    }

    // Authentic Culinary Hierarchy: Ordered by realistic cooking duration
    // Types: 0=Burger, 1=Pizza, 2=Ramen, 3=Sushi, 4=Steak, 5=Cake
    public static final String[] DISH_NAMES = {"Burger", "Pizza", "Ramen", "Sushi", "Steak", "Cake"};
    public static final String[] DISH_COOK_PROFILES = {
        "Burger [Quick Grill ~5m]",     // 0
        "Pizza [Oven Bake ~10m]",      // 1
        "Ramen [Broth Simmer ~15m]",   // 2
        "Sushi [Fast Prep ~2m]",       // 3
        "Steak [Slow Sear ~25m]",      // 4
        "Cake [Long Bake ~45m]"        // 5
    };

    /**
     * Assigns culinary dish types to jobs based on their cooking duration / burst time!
     * Real-world cooking hierarchy:
     * - Fast Prep (~2 mins): Fresh Sushi Rolls (Raw, no stove cooking - fastest!)
     * - Quick Grill (~5 mins): Classic Burger patty on griddle
     * - Stone Oven (~10 mins): Pizza Slice
     * - Simmering (~15 mins): Ramen Bowl with rich broth & noodles
     * - Sizzling Sear (~25 mins): Thick Cast-Iron Steak Platter
     * - Gourmet Bake (~45 mins): Layered Strawberry Cake (Slowest / longest to bake!)
     */
    public static void assignCulinaryDishesByDuration(List<JobInput> jobs) {
        if (jobs == null || jobs.isEmpty()) return;

        // Sort a copy by Burst Time (cooking work needed); tie-break with Arrival Time (earlier finishes earlier)
        List<JobInput> sortedByDuration = new ArrayList<>(jobs);
        sortedByDuration.sort(Comparator.comparingDouble((JobInput j) -> j.bt).thenComparingDouble(j -> j.at));

        int totalJobs = jobs.size();
        for (int rank = 0; rank < totalJobs; rank++) {
            JobInput j = sortedByDuration.get(rank);
            j.dishType = getDishTypeByCookingDuration(rank, totalJobs);
        }
    }

    public static int getDishTypeByCookingDuration(int rank, int totalJobs) {
        if (totalJobs <= 1) return 0; // Default: Burger
        if (totalJobs == 2) {
            // Shorter: Burger (0), Longer: Steak (4)
            return (rank == 0) ? 0 : 4;
        }
        if (totalJobs == 3) {
            // Shortest: Sushi (3), Medium: Pizza (1), Longest: Steak (4)
            int[] d3 = {3, 1, 4};
            return d3[Math.max(0, Math.min(2, rank))];
        }
        if (totalJobs == 4) {
            // Shortest: Sushi (3), Fast: Burger (0), Simmer: Ramen (2), Longest: Steak (4)
            int[] d4 = {3, 0, 2, 4};
            return d4[Math.max(0, Math.min(3, rank))];
        }
        if (totalJobs == 5) {
            // Exact Class 7.4 problem set:
            // J5 (BT 0.1) -> Sushi (3, ~2m raw prep - finishes 1st)
            // J4 (BT 0.2) -> Burger (0, ~5m fast grill - finishes 2nd)
            // J3 (BT 0.3) -> Pizza (1, ~10m oven bake - finishes 3rd)
            // J1 (BT 0.4) -> Ramen (2, ~15m simmer - finishes 4th)
            // J2 (BT 0.6) -> Steak (4, ~25m slow sear - finishes last / takes longest!)
            int[] d5 = {3, 0, 1, 2, 4};
            return d5[Math.max(0, Math.min(4, rank))];
        }

        // 6 or more jobs: Map ranks across full 6-tier culinary spectrum
        // 0: Sushi(3), 1: Burger(0), 2: Pizza(1), 3: Ramen(2), 4: Steak(4), 5: Cake(5)
        int[] d6 = {3, 0, 1, 2, 4, 5};
        int idx = (int) Math.round((rank / (double)(totalJobs - 1)) * (d6.length - 1));
        return d6[Math.max(0, Math.min(d6.length - 1, idx))];
    }

    // UI Mode Toggle for Table Presentation
    private boolean isVerticalClassroomMode = true; // Default: Classroom Excel Vertical Format (Matching Professor's Layout!)
    private boolean useEmptySpacesMode = false;
    private final List<Integer> rowToStepMap = new ArrayList<>();
    private final Set<Integer> blockEndRows = new HashSet<>();
    private final List<JobInput> currentJobList = new ArrayList<>();
    private final List<EventStep> computedSteps = new ArrayList<>();
    private final List<JobSummary> computedSummaries = new ArrayList<>();
    private int currentStepIndex = -1; // -1 = Ready/Not started
    private boolean isPlaying = false;
    private double speedMultiplier = 1.0;
    private javax.swing.Timer playTimer;

    // Challenge Mode State
    public boolean isChallengeMode = false;
    public boolean awaitingChallengeChoice = false;
    public String challengePromptText = "";
    private JButton btnModeToggle;
    private JPanel dispatchRow;
    private JButton btnAdmit, btnHold;

    // UI Widgets - Left Deck
    private DefaultTableModel givenTableModel;
    private JTable givenTable;
    private String selectedCategory = "CLASS 7.4";
    private JButton btnCatClass, btnCatExcel, btnCatRush;
    private JLabel lblCategoryRule;
    private boolean isUpdatingTable = false;
    private JButton btnAddJob, btnRemoveJob;
    private JButton btnPlay, btnStep, btnReset, btnSpeed;

    // UI Widgets - Top HUDs
    private JLabel lblStatusPill;
    private JLabel lblHudActiveNj, lblHudCpuShare;
    private JLabel lblHudTime, lblHudEtHw;
    private JLabel lblHudAvgTat, lblHudAvgWt;

    // UI Widgets - Center Canvas & Tables
    private OvercookedKitchenCanvas canvas;
    private JTabbedPane resultTabbedPane;
    private DefaultTableModel traceTableModel;
    private JTable traceTable;
    private DefaultTableModel summaryTableModel;
    private JTable summaryTable;
    private JLabel lblFooterDetails;

    public MultiprogrammingPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(8, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 1. Left Panel (Given Input Table + Formulas + Dispatch Controls)
        JPanel leftDeck = createLeftDeck();
        leftDeck.setPreferredSize(new Dimension(360, 0));
        add(leftDeck, BorderLayout.WEST);

        // 2. Right Arena (HUDs + Retro Kitchen Canvas + Execution Table + Summary)
        JPanel rightArena = createRightArena();
        add(rightArena, BorderLayout.CENTER);

        // Playback Timer
        playTimer = new javax.swing.Timer(1200, e -> stepForward());

        // Setup Keyboard Shortcuts
        setupKeyboardShortcuts();

        // Load Default Preset: The Exact Class Slide Given (J1..J5)
        loadClassSlidePreset();
    }

    // =========================================================================
    // 1. LEFT PANEL: GIVEN INPUT TABLE + CLASSROOM FORMULAS + PLAY CONTROLS
    // =========================================================================
    private JPanel createLeftDeck() {
        JPanel deck = new JPanel();
        deck.setOpaque(true);
        deck.setBackground(new Color(18, 12, 32));
        deck.setLayout(new BoxLayout(deck, BoxLayout.Y_AXIS));
        deck.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        // 1. Header with Icon Box (Matches Invaders & Pac-Man standard)
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(800, 36));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel iconBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(38, 20, 60));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBox.setPreferredSize(new Dimension(30, 30));
        iconBox.setOpaque(false);
        iconBox.setLayout(new GridBagLayout());
        JLabel icon = new JLabel(RetroIcons.createPlayIcon(14, RetroTheme.ACCENT_PINK));
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
        titleBox.add(subTag);
        titleBox.add(title);
        header.add(titleBox);
        deck.add(header);

        deck.add(Box.createVerticalStrut(6));

        // 2. Category / Scenario Selector (Matches Invaders, Pac-Man, Mario standard)
        JPanel catCard = new JPanel(new BorderLayout(0, 3));
        catCard.setOpaque(false);
        catCard.setMaximumSize(new Dimension(800, 52));
        catCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblCatTitle = new JLabel("CONCURRENCY SCENARIO // CATEGORY");
        lblCatTitle.setFont(new Font("Consolas", Font.BOLD, 10));
        lblCatTitle.setForeground(RetroTheme.ACCENT_CYAN);
        catCard.add(lblCatTitle, BorderLayout.NORTH);

        JPanel catGrid = new JPanel(new GridLayout(1, 3, 4, 0));
        catGrid.setOpaque(false);

        btnCatClass = createCategoryBtn("CLASS 7.4");
        btnCatExcel = createCategoryBtn("EXCEL 10.0");
        btnCatRush = createCategoryBtn("LUNCH RUSH");

        btnCatClass.addActionListener(e -> loadClassSlidePreset());
        btnCatExcel.addActionListener(e -> loadExcelSlidePreset());
        btnCatRush.addActionListener(e -> loadLunchRushPreset());

        catGrid.add(btnCatClass);
        catGrid.add(btnCatExcel);
        catGrid.add(btnCatRush);
        catCard.add(catGrid, BorderLayout.CENTER);
        deck.add(catCard);

        deck.add(Box.createVerticalStrut(4));

        lblCategoryRule = new JLabel("CLASS 7.4: Standard classroom problem set (T=7.40 to 9.10).");
        lblCategoryRule.setFont(new Font("Consolas", Font.ITALIC, 9));
        lblCategoryRule.setForeground(new Color(210, 220, 240));
        lblCategoryRule.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblCategoryRule.setMaximumSize(new Dimension(800, 16));
        deck.add(lblCategoryRule);

        JLabel lblFoodRule = new JLabel("🍳 FOOD MAP: Fast jobs = Sushi/Burger • Longest job = Sizzling Steak!");
        lblFoodRule.setFont(new Font("Consolas", Font.BOLD, 9));
        lblFoodRule.setForeground(new Color(255, 140, 190));
        lblFoodRule.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblFoodRule.setMaximumSize(new Dimension(800, 16));
        deck.add(lblFoodRule);

        deck.add(Box.createVerticalStrut(6));

        // Section Title: GIVEN TABLE with compact [+] and [-] mini buttons
        JPanel givenHeader = new JPanel(new BorderLayout());
        givenHeader.setOpaque(false);
        givenHeader.setMaximumSize(new Dimension(800, 24));
        givenHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel gLbl = new JLabel("GIVEN: JOB LIST (EDITABLE):");
        gLbl.setFont(new Font("Consolas", Font.BOLD, 10));
        gLbl.setForeground(RetroTheme.ACCENT_YELLOW);
        givenHeader.add(gLbl, BorderLayout.WEST);

        JPanel miniJobBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        miniJobBtns.setOpaque(false);

        btnAddJob = CPUSchedulingPanel.createArcadeButton("+", new Color(0, 140, 70), Color.WHITE, 18);
        btnAddJob.setPreferredSize(new Dimension(28, 20));
        btnAddJob.setFont(new Font("Consolas", Font.BOLD, 12));
        btnAddJob.setToolTipText("Add new Job row");
        btnAddJob.addActionListener(e -> addNewJobRow());

        btnRemoveJob = CPUSchedulingPanel.createArcadeButton("—", new Color(160, 40, 50), Color.WHITE, 18);
        btnRemoveJob.setPreferredSize(new Dimension(28, 20));
        btnRemoveJob.setFont(new Font("Consolas", Font.BOLD, 12));
        btnRemoveJob.setToolTipText("Remove selected Job row");
        btnRemoveJob.addActionListener(e -> removeSelectedJobRow());

        miniJobBtns.add(btnAddJob);
        miniJobBtns.add(btnRemoveJob);
        givenHeader.add(miniJobBtns, BorderLayout.EAST);
        deck.add(givenHeader);

        deck.add(Box.createVerticalStrut(4));

        // Given JTable (JOB#, AT, BT)
        String[] givenCols = {"JOB#", "AT", "BT"};
        givenTableModel = new DefaultTableModel(givenCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return true; // Allow editing AT and BT directly
            }
        };

        givenTableModel.addTableModelListener(e -> {
            if (!isUpdatingTable && e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                solveAndSimulate();
            }
        });

        givenTable = new JTable(givenTableModel);
        givenTable.setFont(new Font("Consolas", Font.BOLD, 12));
        givenTable.setRowHeight(24);
        givenTable.setBackground(new Color(15, 10, 28));
        givenTable.setForeground(new Color(0, 255, 255)); // Neon Cyan
        givenTable.setSelectionBackground(new Color(45, 30, 75));
        givenTable.setSelectionForeground(Color.WHITE);
        givenTable.setGridColor(new Color(55, 40, 80));
        givenTable.setShowGrid(true);
        applyRetroHeaderRenderer(givenTable);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        givenTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        givenTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        givenTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        JScrollPane givenScroll = new JScrollPane(givenTable);
        givenScroll.getViewport().setBackground(new Color(15, 10, 28));
        givenScroll.setBorder(BorderFactory.createLineBorder(new Color(65, 45, 95), 1));
        givenScroll.setPreferredSize(new Dimension(290, 145));
        givenScroll.setMaximumSize(new Dimension(800, 155));
        givenScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        deck.add(givenScroll);

        deck.add(Box.createVerticalStrut(6));

        // Classroom Formula Reference Card
        JPanel formulaCard = new JPanel();
        formulaCard.setLayout(new BoxLayout(formulaCard, BoxLayout.Y_AXIS));
        formulaCard.setOpaque(true);
        formulaCard.setBackground(new Color(24, 18, 42));
        formulaCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 180, 240), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        formulaCard.setMaximumSize(new Dimension(800, 125));
        formulaCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel fTitle = new JLabel("SYLLABUS FORMULAS (EXCEL MODEL):");
        fTitle.setFont(new Font("Consolas", Font.BOLD, 9));
        fTitle.setForeground(RetroTheme.ACCENT_YELLOW);
        formulaCard.add(fTitle);

        formulaCard.add(Box.createVerticalStrut(2));

        String[] formulas = {
                "• time: if arrive = ar; if term = et + prev",
                "• event: a/s (arrive & start) | t (terminate)",
                "• %cpu/nj: = 1 / nj (Equal CPU share)",
                "• et: = present time - prev time",
                "• hw/job: = et * (%cpu/nj)",
                "• timeleft: = prev timeleft - hw/job"
        };
        for (String f : formulas) {
            JLabel lbl = new JLabel(f);
            lbl.setFont(new Font("Consolas", Font.PLAIN, 9));
            lbl.setForeground(new Color(210, 225, 245));
            formulaCard.add(lbl);
        }
        deck.add(formulaCard);

        deck.add(Box.createVerticalStrut(8));

        // Simulation Step & Play Controls (Matches Invaders & Pac-Man standard)
        btnPlay = CPUSchedulingPanel.createArcadeButton("START SIMULATION", new Color(0, 150, 75), Color.WHITE, 30);
        btnPlay.setFont(new Font("Consolas", Font.BOLD, 11));
        btnPlay.setMaximumSize(new Dimension(800, 30));
        btnPlay.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPlay.addActionListener(e -> togglePlayPause());
        deck.add(btnPlay);

        deck.add(Box.createVerticalStrut(4));

        JPanel ctrlCard = new JPanel(new GridLayout(2, 2, 4, 4));
        ctrlCard.setOpaque(false);
        ctrlCard.setMaximumSize(new Dimension(800, 54));
        ctrlCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnPrev = CPUSchedulingPanel.createArcadeButton("STEP PREV (<-)", new Color(34, 25, 55), Color.WHITE, 25);
        btnPrev.setFont(new Font("Consolas", Font.BOLD, 10));
        btnPrev.setToolTipText("Step backward one interval in timeline");
        btnPrev.addActionListener(e -> stepBackward());

        btnStep = CPUSchedulingPanel.createArcadeButton("STEP NEXT (->)", new Color(42, 30, 70), Color.WHITE, 25);
        btnStep.setFont(new Font("Consolas", Font.BOLD, 10));
        btnStep.setToolTipText("Advance forward one interval in trace table");
        btnStep.addActionListener(e -> stepForward());

        btnReset = CPUSchedulingPanel.createArcadeButton("RESET", new Color(75, 20, 35), Color.WHITE, 25);
        btnReset.setFont(new Font("Consolas", Font.BOLD, 10));
        btnReset.setToolTipText("Reset simulation timeline to beginning");
        btnReset.addActionListener(e -> resetSimulation());

        btnSpeed = CPUSchedulingPanel.createArcadeButton("SPEED: 1.0x", new Color(32, 22, 50), Color.WHITE, 24);
        btnSpeed.setFont(new Font("Consolas", Font.BOLD, 10));
        btnSpeed.setToolTipText("Cycle playback speed multiplier");
        btnSpeed.addActionListener(e -> cycleSpeed());

        ctrlCard.add(btnPrev);
        ctrlCard.add(btnStep);
        ctrlCard.add(btnReset);
        ctrlCard.add(btnSpeed);
        deck.add(ctrlCard);

        deck.add(Box.createVerticalStrut(8));

        // Bottom Left Fill: Classroom Metrics Summary Card (Eliminates empty void!)
        JPanel perfCard = new JPanel(new GridLayout(4, 1, 0, 4));
        perfCard.setOpaque(true);
        perfCard.setBackground(new Color(24, 18, 42));
        perfCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        perfCard.setMaximumSize(new Dimension(800, 95));
        perfCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel pTitle = new JLabel("SYSTEM STATUS & UTILIZATION:");
        pTitle.setFont(new Font("Consolas", Font.BOLD, 10));
        pTitle.setForeground(RetroTheme.ACCENT_YELLOW);
        perfCard.add(pTitle);

        JLabel pTat = new JLabel("• AVG TURNAROUND (TAT): 0.720");
        pTat.setFont(new Font("Consolas", Font.PLAIN, 10));
        pTat.setForeground(Color.WHITE);
        perfCard.add(pTat);

        JLabel pWt = new JLabel("• AVG WAITING TIME (WT): 0.400");
        pWt.setFont(new Font("Consolas", Font.PLAIN, 10));
        pWt.setForeground(Color.WHITE);
        perfCard.add(pWt);

        JLabel pUtil = new JLabel("• CPU & RAM UTILIZATION: 100.0% (BUSY)");
        pUtil.setFont(new Font("Consolas", Font.PLAIN, 10));
        pUtil.setForeground(new Color(0, 240, 140));
        perfCard.add(pUtil);

        deck.add(perfCard);

        return deck;
    }

    // =========================================================================
    // 2. RIGHT ARENA: STATUS + 3 HUD PODS + RETRO CANVAS + 2 RESULT TABS
    // =========================================================================
    private JPanel createRightArena() {
        JPanel arena = new JPanel(new BorderLayout(0, 6));
        arena.setOpaque(false);
        arena.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        JPanel topSection = new JPanel(new BorderLayout(0, 4));
        topSection.setOpaque(false);

        // 1. Mission Status Header Strip - Spans 100% full width (No empty space under docs manual!)
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel("LIVE OUTPUT // OVERCOOKED KITCHEN CONCURRENCY & CPU PMU");
        lblTitle.setFont(new Font("Consolas", Font.BOLD, 13));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle, BorderLayout.WEST);

        JPanel eastHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        eastHeader.setOpaque(false);

        JButton btnPlayArcadeHeader = RetroTheme.createRetroButton("PLAY OVERCOOKED KITCHEN", new Color(38, 20, 60), RetroTheme.ACCENT_YELLOW);
        btnPlayArcadeHeader.setFont(new Font("Consolas", Font.BOLD, 10));
        btnPlayArcadeHeader.setIcon(new ImageIcon(PixelSprites.createChefSprite(14, false)));
        btnPlayArcadeHeader.setIconTextGap(6);
        btnPlayArcadeHeader.setToolTipText("Launch standalone playable arcade game where you control the chef directly!");
        btnPlayArcadeHeader.addActionListener(e -> launchOvercookedGame());
        eastHeader.add(btnPlayArcadeHeader);

        lblStatusPill = new JLabel(); // Retained in memory to prevent NPEs, but no green border on screen

        header.add(eastHeader, BorderLayout.EAST);

        // 2. 3 Glowing HUD Pods - Spans 100% full width across top (Directly under DOCS // MANUAL!)
        JPanel statsCard = new JPanel(new GridLayout(1, 3, 10, 0));
        statsCard.setOpaque(false);
        statsCard.setPreferredSize(new Dimension(860, 68));

        // Pod 1: Active Concurrency nj and %cpu/nj
        JPanel p1 = createGlassHudPod("ACTIVE CONCURRENCY (nj)", RetroTheme.ACCENT_CYAN);
        lblHudActiveNj = new JLabel("nj = 0 JOBS");
        lblHudActiveNj.setFont(new Font("Consolas", Font.BOLD, 19));
        lblHudActiveNj.setForeground(RetroTheme.ACCENT_CYAN);
        lblHudCpuShare = new JLabel("CPU SHARE: %cpu/nj = 0.0%");
        lblHudCpuShare.setFont(new Font("Consolas", Font.BOLD, 10));
        lblHudCpuShare.setForeground(Color.WHITE);
        p1.add(lblHudActiveNj);
        p1.add(lblHudCpuShare);
        statsCard.add(p1);

        // Pod 2: Current Time & Interval
        JPanel p2 = createGlassHudPod("CURRENT TIME & ELAPSED (et)", RetroTheme.ACCENT_YELLOW);
        lblHudTime = new JLabel("TIME: 0.00");
        lblHudTime.setFont(new Font("Consolas", Font.BOLD, 19));
        lblHudTime.setForeground(RetroTheme.ACCENT_YELLOW);
        lblHudEtHw = new JLabel("et = 0.00 | hw/job = 0.000");
        lblHudEtHw.setFont(new Font("Consolas", Font.BOLD, 10));
        lblHudEtHw.setForeground(Color.WHITE);
        p2.add(lblHudTime);
        p2.add(lblHudEtHw);
        statsCard.add(p2);

        // Pod 3: Overall Performance
        JPanel p3 = createGlassHudPod("AVERAGE PERFORMANCE", new Color(0, 240, 140));
        lblHudAvgTat = new JLabel("AVG TAT: 0.00");
        lblHudAvgTat.setFont(new Font("Consolas", Font.BOLD, 19));
        lblHudAvgTat.setForeground(new Color(0, 240, 140));
        lblHudAvgWt = new JLabel("AVG WT: 0.00 | CPU UTIL: 100%");
        lblHudAvgWt.setFont(new Font("Consolas", Font.BOLD, 10));
        lblHudAvgWt.setForeground(Color.WHITE);
        p3.add(lblHudAvgTat);
        p3.add(lblHudAvgWt);
        statsCard.add(p3);

        JPanel upperBar = new JPanel(new BorderLayout(0, 4));
        upperBar.setOpaque(false);
        upperBar.add(header, BorderLayout.NORTH);
        upperBar.add(statsCard, BorderLayout.CENTER);

        topSection.add(upperBar, BorderLayout.NORTH);

        // 3. Central Overcooked Pixel Kitchen Canvas
        canvas = new OvercookedKitchenCanvas(this);
        canvas.setPreferredSize(new Dimension(860, 235));
        canvas.setMinimumSize(new Dimension(600, 200));
        topSection.add(canvas, BorderLayout.CENTER);

        arena.add(topSection, BorderLayout.NORTH);

        // 4. Custom Arcade Cyber Tabs (Eliminates JTabbedPane native Windows white glitches!)
        CardLayout cardLayout = new CardLayout();
        JPanel tabCards = new JPanel(cardLayout);
        tabCards.setOpaque(true);
        tabCards.setBackground(new Color(15, 10, 28));

        JPanel tabExecution = createExecutionTablePanel();
        JPanel tabSummary = createSummaryTablePanel();
        tabCards.add(tabExecution, "TAB_EXEC");
        tabCards.add(tabSummary, "TAB_SUM");

        JPanel tabHeaderBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        tabHeaderBar.setOpaque(true);
        tabHeaderBar.setBackground(new Color(15, 10, 28));

        JButton btnTabExec = CPUSchedulingPanel.createArcadeButton("EXECUTION TRACE TABLE (EXCEL MODEL)", new Color(0, 100, 160), Color.WHITE, 26);
        JButton btnTabSum = CPUSchedulingPanel.createArcadeButton("JOB PERFORMANCE METRICS (TAT / WT)", new Color(30, 22, 50), RetroTheme.ACCENT_CYAN, 26);

        btnTabExec.addActionListener(e -> {
            cardLayout.show(tabCards, "TAB_EXEC");
            btnTabExec.setBackground(new Color(0, 100, 160));
            btnTabExec.setForeground(Color.WHITE);
            btnTabSum.setBackground(new Color(30, 22, 50));
            btnTabSum.setForeground(RetroTheme.ACCENT_CYAN);
        });

        btnTabSum.addActionListener(e -> {
            cardLayout.show(tabCards, "TAB_SUM");
            btnTabSum.setBackground(new Color(0, 100, 160));
            btnTabSum.setForeground(Color.WHITE);
            btnTabExec.setBackground(new Color(30, 22, 50));
            btnTabExec.setForeground(RetroTheme.ACCENT_CYAN);
        });

        tabHeaderBar.add(btnTabExec);
        tabHeaderBar.add(btnTabSum);

        JPanel tableArea = new JPanel(new BorderLayout(0, 2));
        tableArea.setOpaque(true);
        tableArea.setBackground(new Color(15, 10, 28));
        tableArea.add(tabHeaderBar, BorderLayout.NORTH);
        tableArea.add(tabCards, BorderLayout.CENTER);

        arena.add(tableArea, BorderLayout.CENTER);

        return arena;
    }

    private JPanel createExecutionTablePanel() {
        JPanel container = new JPanel(new BorderLayout(0, 4));
        container.setOpaque(true);
        container.setBackground(new Color(15, 10, 28));

        // Header Strip with Title & Mode Toggle Button (Guaranteed Dark Theme!)
        JPanel headerStrip = new JPanel(new BorderLayout());
        headerStrip.setOpaque(true);
        headerStrip.setBackground(new Color(24, 18, 44));
        headerStrip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        JLabel tLbl = new JLabel("MULTIPROGRAMMING TRACE // EVENT-DRIVEN EXECUTION");
        tLbl.setFont(new Font("Consolas", Font.BOLD, 10));
        tLbl.setForeground(RetroTheme.ACCENT_YELLOW);
        headerStrip.add(tLbl, BorderLayout.WEST);

        JButton btnToggleMode = CPUSchedulingPanel.createArcadeButton(
                isVerticalClassroomMode ? "FORMAT: CLASSROOM EXCEL (VERTICAL)" : "FORMAT: WIDE MATRIX (J1..J5)",
                isVerticalClassroomMode ? new Color(0, 110, 170) : new Color(38, 25, 68), Color.WHITE, 22);
        btnToggleMode.setFont(new Font("Consolas", Font.BOLD, 10));
        btnToggleMode.addActionListener(e -> {
            isVerticalClassroomMode = !isVerticalClassroomMode;
            btnToggleMode.setText(isVerticalClassroomMode ? "FORMAT: CLASSROOM EXCEL (VERTICAL)" : "FORMAT: WIDE MATRIX (J1..J5)");
            btnToggleMode.setBackground(isVerticalClassroomMode ? new Color(0, 110, 170) : new Color(38, 25, 68));
            btnToggleMode.setForeground(isVerticalClassroomMode ? Color.WHITE : RetroTheme.ACCENT_CYAN);
            populateTraceTable();
        });
        headerStrip.add(btnToggleMode, BorderLayout.EAST);
        container.add(headerStrip, BorderLayout.NORTH);

        traceTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        traceTable = new JTable(traceTableModel);
        traceTable.setFont(new Font("Consolas", Font.BOLD, 12));
        traceTable.setRowHeight(26); // Clean legible row height
        traceTable.setFillsViewportHeight(true);
        traceTable.setRowSelectionAllowed(false);
        traceTable.setCellSelectionEnabled(false);
        traceTable.setFocusable(false);
        traceTable.setBackground(new Color(15, 10, 28));
        traceTable.setForeground(Color.WHITE);
        traceTable.setGridColor(new Color(50, 36, 75));
        traceTable.setShowGrid(true);
        applyRetroHeaderRenderer(traceTable);

        JScrollPane scroll = new JScrollPane(traceTable);
        scroll.getViewport().setBackground(new Color(15, 10, 28));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(55, 40, 80), 1));
        container.add(scroll, BorderLayout.CENTER);

        // Footer Card with Current Step Details & Summary Metrics
        JPanel footerCard = new JPanel(new GridLayout(2, 1, 0, 4));
        footerCard.setOpaque(true);
        footerCard.setBackground(new Color(16, 11, 28));
        footerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        lblFooterDetails = new JLabel("READY: Click SOLVE or AUTO PLAY to begin step-by-step multiprogramming execution.");
        lblFooterDetails.setFont(new Font("Consolas", Font.BOLD, 11));
        lblFooterDetails.setForeground(RetroTheme.ACCENT_CYAN);
        footerCard.add(lblFooterDetails);

        JLabel lblQuickSummary = new JLabel("OVERALL METRICS: AVG TAT = 0.720 | AVG WT = 0.400 | TOTAL ELAPSED TIME = 1.70 (100% CPU)");
        lblQuickSummary.setFont(new Font("Consolas", Font.BOLD, 11));
        lblQuickSummary.setForeground(RetroTheme.ACCENT_YELLOW);
        footerCard.add(lblQuickSummary);

        container.add(footerCard, BorderLayout.SOUTH);

        return container;
    }

    private JPanel createSummaryTablePanel() {
        JPanel container = new JPanel(new BorderLayout(0, 4));
        container.setOpaque(false);

        String[] cols = {
                "JOB#", "DISH TYPE", "ARRIVAL (AT)", "BURST (BT)", "FINISH (FT)", "TURNAROUND (TAT = FT - AT)", "WAITING (WT = TAT - BT)"
        };

        summaryTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        summaryTable = new JTable(summaryTableModel);
        summaryTable.setFont(new Font("Consolas", Font.BOLD, 12));
        summaryTable.setRowHeight(24);
        summaryTable.setBackground(new Color(15, 10, 28));
        summaryTable.setForeground(Color.WHITE);
        summaryTable.setGridColor(new Color(55, 40, 80));
        summaryTable.setShowGrid(true);
        applyRetroHeaderRenderer(summaryTable);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < cols.length; i++) {
            summaryTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scroll = new JScrollPane(summaryTable);
        scroll.getViewport().setBackground(new Color(15, 10, 28));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(55, 40, 80), 1));
        container.add(scroll, BorderLayout.CENTER);

        return container;
    }

    public static void applyRetroHeaderRenderer(JTable table) {
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                l.setOpaque(true);
                l.setBackground(new Color(26, 18, 48)); // Solid Cyber Purple
                l.setForeground(new Color(0, 255, 255)); // Bright Electric Neon Cyan
                l.setFont(new Font("Consolas", Font.BOLD, 11));
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(75, 55, 115), 1),
                        BorderFactory.createEmptyBorder(5, 4, 5, 4)
                ));
                return l;
            }
        });
        table.getTableHeader().repaint();
    }

    public static JPanel createGlassHudPod(String title, Color accent) {
        JPanel pod = new JPanel();
        pod.setLayout(new BoxLayout(pod, BoxLayout.Y_AXIS));
        pod.setOpaque(true);
        pod.setBackground(new Color(24, 18, 42));
        pod.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Consolas", Font.BOLD, 10));
        t.setForeground(accent);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        pod.add(t);
        pod.add(Box.createVerticalStrut(2));

        return pod;
    }

    // =========================================================================
    // 3. CATEGORIES, PRESETS & INPUT MANAGEMENT
    // =========================================================================
    private JButton createCategoryBtn(String name) {
        JButton b = CPUSchedulingPanel.createArcadeButton(name, new Color(38, 25, 68), new Color(180, 180, 210), 24);
        b.setFont(new Font("Consolas", Font.BOLD, 10));
        return b;
    }

    private void updateCategoryButtons(String cat) {
        this.selectedCategory = cat;
        updateCatBtn(btnCatClass, "CLASS 7.4".equals(cat));
        updateCatBtn(btnCatExcel, "EXCEL 10.0".equals(cat));
        updateCatBtn(btnCatRush, "LUNCH RUSH".equals(cat));
        if (lblCategoryRule != null) {
            if ("CLASS 7.4".equals(cat)) {
                lblCategoryRule.setText("CLASS 7.4: Standard classroom problem set (T=7.40 to 9.10).");
            } else if ("EXCEL 10.0".equals(cat)) {
                lblCategoryRule.setText("EXCEL 10.0: Classroom problem set 2 with simultaneous finish (T=10.00 to 11.40).");
            } else if ("LUNCH RUSH".equals(cat)) {
                lblCategoryRule.setText("LUNCH RUSH: High concurrency fast orders kitchen rush challenge.");
            }
        }
    }

    private void updateCatBtn(JButton b, boolean active) {
        if (b == null) return;
        b.setBackground(active ? RetroTheme.ACCENT_PINK : new Color(38, 25, 68));
        b.setForeground(active ? Color.WHITE : new Color(180, 180, 210));
    }

    public void loadClassSlidePreset() {
        pauseSimulation();
        updateCategoryButtons("CLASS 7.4");
        isUpdatingTable = true;
        try {
            givenTableModel.setRowCount(0);
            // The Exact Given from Image 1:
            // J1: AT 7.4, BT 0.4
            // J2: AT 7.9, BT 0.6
            // J3: AT 8.1, BT 0.3
            // J4: AT 8.3, BT 0.2
            // J5: AT 8.4, BT 0.1
            givenTableModel.addRow(new Object[]{"J1", "7.4", "0.4"});
            givenTableModel.addRow(new Object[]{"J2", "7.9", "0.6"});
            givenTableModel.addRow(new Object[]{"J3", "8.1", "0.3"});
            givenTableModel.addRow(new Object[]{"J4", "8.3", "0.2"});
            givenTableModel.addRow(new Object[]{"J5", "8.4", "0.1"});
        } finally {
            isUpdatingTable = false;
        }

        solveAndSimulate();
    }

    public void loadExcelSlidePreset() {
        pauseSimulation();
        updateCategoryButtons("EXCEL 10.0");
        isUpdatingTable = true;
        try {
            givenTableModel.setRowCount(0);
            // Exact Given Dataset from Classroom Excel Screenshot (Media 1794742107745)
            givenTableModel.addRow(new Object[]{"J1", "10.0", "0.3"});
            givenTableModel.addRow(new Object[]{"J2", "10.2", "0.5"});
            givenTableModel.addRow(new Object[]{"J3", "10.4", "0.1"});
            givenTableModel.addRow(new Object[]{"J4", "10.5", "0.4"});
            givenTableModel.addRow(new Object[]{"J5", "10.8", "0.1"});
        } finally {
            isUpdatingTable = false;
        }

        solveAndSimulate();
    }

    public void loadLunchRushPreset() {
        pauseSimulation();
        updateCategoryButtons("LUNCH RUSH");
        isUpdatingTable = true;
        try {
            givenTableModel.setRowCount(0);
            // Fast-paced kitchen lunch rush concurrency challenge
            givenTableModel.addRow(new Object[]{"J1", "1.0", "0.6"});
            givenTableModel.addRow(new Object[]{"J2", "1.3", "0.5"});
            givenTableModel.addRow(new Object[]{"J3", "1.5", "0.4"});
            givenTableModel.addRow(new Object[]{"J4", "1.8", "0.3"});
            givenTableModel.addRow(new Object[]{"J5", "2.0", "0.2"});
        } finally {
            isUpdatingTable = false;
        }

        solveAndSimulate();
    }

    /** Launches the standalone Overcooked Kitchen Arcade Game (Mission 08). */
    public void launchOvercookedGame() {
        pauseSimulation();
        solveAndSimulate(); // Ensure latest givenTableModel rows (including user-defined jobs) are parsed!
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        OvercookedKitchenDialog dlg = new OvercookedKitchenDialog(topFrame, new ArrayList<>(currentJobList));
        dlg.setVisible(true);
    }

    public void loadRandomPreset() {
        pauseSimulation();
        updateCategoryButtons("RANDOM");
        isUpdatingTable = true;
        try {
            givenTableModel.setRowCount(0);
            java.util.Random rnd = new java.util.Random();
            int n = 4 + rnd.nextInt(3); // 4 to 6 jobs
            double t = 1.0 + rnd.nextInt(5);
            for (int i = 1; i <= n; i++) {
                double at = Math.round(t * 10.0) / 10.0;
                double bt = Math.round((0.2 + rnd.nextDouble() * 0.6) * 10.0) / 10.0;
                givenTableModel.addRow(new Object[]{"J" + i, String.format(Locale.US, "%.1f", at), String.format(Locale.US, "%.1f", bt)});
                t += (0.1 + rnd.nextDouble() * 0.4);
            }
        } finally {
            isUpdatingTable = false;
        }

        solveAndSimulate();
    }

    public void toggleChallengeMode() {
        isChallengeMode = !isChallengeMode;
        btnModeToggle.setText(isChallengeMode ? "CHALLENGE: ON" : "CHALLENGE: OFF");
        btnModeToggle.setBackground(isChallengeMode ? new Color(180, 100, 20) : new Color(38, 25, 68));
        btnModeToggle.repaint();

        if (dispatchRow != null) {
            dispatchRow.setVisible(isChallengeMode);
            dispatchRow.revalidate();
            dispatchRow.repaint();
        }

        if (isChallengeMode) {
            if (currentStepIndex < 0 || currentStepIndex >= computedSteps.size() - 1) {
                if (computedSteps.isEmpty()) solveAndSimulate();
                currentStepIndex = 0;
                updateHuds();
            }
            pauseSimulation();
            setupChallengeForStep(currentStepIndex);
        } else {
            awaitingChallengeChoice = false;
            challengePromptText = "";
            if (lblStatusPill != null) {
                lblStatusPill.setText(" [ STATUS: READY // CLICK SOLVE ] ");
                lblStatusPill.setForeground(new Color(0, 240, 100));
                lblStatusPill.setBorder(BorderFactory.createLineBorder(new Color(0, 240, 100), 1));
            }
        }
        if (canvas != null) canvas.repaint();
    }

    public void setupChallengeForStep(int stepIdx) {
        if (!isChallengeMode || stepIdx < 0 || stepIdx >= computedSteps.size()) return;

        EventStep step = computedSteps.get(stepIdx);
        awaitingChallengeChoice = true;
        if (!isPlaying) {
            pauseSimulation();
        }

        if ("a/s".equalsIgnoreCase(step.event)) {
            if (btnAdmit != null) btnAdmit.setText("◄ ADMIT DISH");
            if (btnHold != null) btnHold.setText("HOLD QUEUE ►");

            // Find which job arrived at this step
            String arrivingJob = "";
            for (JobInput ji : currentJobList) {
                if (Math.abs(ji.at - step.time) < 1e-4) {
                    arrivingJob = ji.name + " (" + DISH_NAMES[ji.dishType % 6] + ")";
                    break;
                }
            }
            if (arrivingJob.isEmpty() && !step.activeJobNames.isEmpty()) {
                for (String s : step.activeJobNames) arrivingJob = s;
            }
            challengePromptText = String.format("[ORDER ARRIVAL T=%.2f]: %s arrived! ADMIT (◄) to Kitchen or HOLD (►)?", step.time, arrivingJob);
            if (lblStatusPill != null) {
                lblStatusPill.setText(" [ STATUS: CHALLENGE // ADMIT OR HOLD ARRIVING ORDER ] ");
                lblStatusPill.setForeground(RetroTheme.ACCENT_YELLOW);
                lblStatusPill.setBorder(BorderFactory.createLineBorder(RetroTheme.ACCENT_YELLOW, 1));
            }
        } else {
            // Terminate event
            if (btnAdmit != null) btnAdmit.setText("◄ SERVE / NEXT");
            if (btnHold != null) btnHold.setText("STEP NEXT ►");

            String servedNames = step.terminatedThisStep.isEmpty() ? "Order" : String.join(" & ", step.terminatedThisStep);
            challengePromptText = String.format("[TERMINATION T=%.2f]: %s finished cooking! Active remaining nj = %d. CONFIRM SERVE (◄) or STEP (►)?",
                    step.time, servedNames, step.activeJobNames.size());
            if (lblStatusPill != null) {
                lblStatusPill.setText(" [ STATUS: CHALLENGE // CONFIRM " + servedNames + " COMPLETION ] ");
                lblStatusPill.setForeground(new Color(0, 240, 180));
                lblStatusPill.setBorder(BorderFactory.createLineBorder(new Color(0, 240, 180), 1));
            }
        }
        if (canvas != null) canvas.repaint();
    }

    public void answerChallenge(boolean admit) {
        if (!isChallengeMode) {
            stepForward();
            return;
        }

        awaitingChallengeChoice = false;
        AppState.addScore(admit ? 100 : 50);

        if (lblStatusPill != null) {
            lblStatusPill.setText(admit ? " [ +100 XP! DISH ADMITTED // CONCURRENCY ADVANCED ] " : " [ +50 XP! ORDER HELD IN QUEUE ] ");
            lblStatusPill.setForeground(new Color(255, 230, 0));
            lblStatusPill.setBorder(BorderFactory.createLineBorder(new Color(255, 230, 0), 1));
        }

        // Advance step
        if (currentStepIndex < computedSteps.size() - 1) {
            stepForward();
        } else {
            challengePromptText = "[LEVEL CLEAR]: All orders served! 100% Multiprogramming Efficiency!";
            if (lblStatusPill != null) {
                lblStatusPill.setText(" [ STATUS: LEVEL CLEAR // COMPLETED! +100XP ] ");
                lblStatusPill.setForeground(new Color(255, 220, 0));
                lblStatusPill.setBorder(BorderFactory.createLineBorder(new Color(255, 220, 0), 2));
            }
            if (canvas != null) canvas.repaint();
        }
    }

    private void addNewJobRow() {
        isUpdatingTable = true;
        try {
            int nextNum = givenTableModel.getRowCount() + 1;
            double nextAt = 8.5;
            if (givenTableModel.getRowCount() > 0) {
                try {
                    double lastAt = Double.parseDouble(String.valueOf(givenTableModel.getValueAt(givenTableModel.getRowCount() - 1, 1)));
                    nextAt = Math.round((lastAt + 0.2) * 100.0) / 100.0;
                } catch (Exception ignored) {}
            }
            givenTableModel.addRow(new Object[]{"J" + nextNum, String.valueOf(nextAt), "0.3"});
        } finally {
            isUpdatingTable = false;
        }
        solveAndSimulate();
    }

    private void removeSelectedJobRow() {
        int row = givenTable.getSelectedRow();
        if (row < 0 && givenTableModel.getRowCount() > 0) row = givenTableModel.getRowCount() - 1;
        if (row >= 0 && givenTableModel.getRowCount() > 0) {
            isUpdatingTable = true;
            try {
                givenTableModel.removeRow(row);
            } finally {
                isUpdatingTable = false;
            }
            solveAndSimulate();
        }
    }

    // =========================================================================
    // 4. MULTIPROGRAMMING PROCESSOR SHARING SOLVER ALGORITHM
    // =========================================================================
    public void solveAndSimulate() {
        pauseSimulation();
        currentJobList.clear();

        // 1. Parse Given Table
        for (int r = 0; r < givenTableModel.getRowCount(); r++) {
            String name = String.valueOf(givenTableModel.getValueAt(r, 0)).trim();
            if (name.isEmpty()) name = "J" + (r + 1);

            double at = 0.0;
            double bt = 0.1;
            try {
                at = Double.parseDouble(String.valueOf(givenTableModel.getValueAt(r, 1)).trim());
                bt = Double.parseDouble(String.valueOf(givenTableModel.getValueAt(r, 2)).trim());
            } catch (Exception e) {
                continue;
            }
            if (bt <= 0) bt = 0.1;

            currentJobList.add(new JobInput(name, at, bt, 0));
        }

        if (currentJobList.isEmpty()) {
            computedSteps.clear();
            computedSummaries.clear();
            populateTraceTable();
            populateSummaryTable(0.0, 0.0);
            currentStepIndex = 0;
            updateHuds();
            if (canvas != null) canvas.repaint();
            return;
        }

        // Sort by Arrival Time
        currentJobList.sort(Comparator.comparingDouble(j -> j.at));

        // Assign authentic culinary dishes based on cooking duration / burst time!
        // Quickest jobs get fast prep foods (Sushi, Burger); longest jobs get slow cook foods (Steak, Cake)!
        assignCulinaryDishesByDuration(currentJobList);

        // 2. Run Exact Event-Driven Simulation Algorithm
        computedSteps.clear();
        computedSummaries.clear();

        class SimJob {
            String name;
            double at;
            double bt;
            double timeLeft;
            Double finishTime = null;
            int dishType;

            SimJob(JobInput inp) {
                this.name = inp.name;
                this.at = inp.at;
                this.bt = inp.bt;
                this.timeLeft = inp.bt;
                this.dishType = inp.dishType;
            }
        }

        List<SimJob> simJobs = new ArrayList<>();
        for (JobInput inp : currentJobList) simJobs.add(new SimJob(inp));

        int jobIdx = 0;
        int nTotal = simJobs.size();
        List<SimJob> active = new ArrayList<>();
        double prevTime = simJobs.get(0).at;

        while (jobIdx < nTotal || !active.isEmpty()) {
            if (active.isEmpty()) {
                // System was idle until next job arrival
                SimJob nextJ = simJobs.get(jobIdx);
                double arrivalTime = nextJ.at;
                double et = Math.round((arrivalTime - prevTime) * 1000.0) / 1000.0;
                active.add(nextJ);
                jobIdx++;

                EventStep step = new EventStep();
                step.time = arrivalTime;
                step.event = nextJ.name + " a/s";
                step.eventList.add(nextJ.name + " a/s");
                step.details = nextJ.name + " arrives and starts";

                // Catch any simultaneous initial arrivals
                while (jobIdx < nTotal && Math.abs(simJobs.get(jobIdx).at - arrivalTime) < 1e-6) {
                    SimJob simultaneous = simJobs.get(jobIdx);
                    active.add(simultaneous);
                    step.details += ", " + simultaneous.name + " arrives";
                    step.eventList.add(simultaneous.name + " a/s");
                    jobIdx++;
                }

                step.nj = 0; // In Excel, nj=0 before first processing interval
                step.cpuPct = 0.0;
                step.et = et;
                step.hwJob = 0.0; // Idle interval

                for (SimJob j : active) {
                    step.intervalJobs.add(new BlockJobItem(j.name, j.timeLeft));
                    step.activeJobNames.add(j.name);
                    step.timeLeftMap.put(j.name, j.timeLeft);
                }

                computedSteps.add(step);
                prevTime = arrivalTime;
                continue;
            }

            int currentNj = active.size();
            double cpuShare = 1.0 / currentNj;

            // Find minimum remaining burst among active jobs
            double minTimeLeft = Double.MAX_VALUE;
            for (SimJob j : active) {
                if (j.timeLeft < minTimeLeft) minTimeLeft = j.timeLeft;
            }

            // Time needed for the earliest active job to terminate
            double timeToFinish = minTimeLeft / cpuShare;
            double termTime = Math.round((prevTime + timeToFinish) * 10000.0) / 10000.0;

            double nextArrTime = (jobIdx < nTotal) ? simJobs.get(jobIdx).at : Double.MAX_VALUE;

            double eventTime = Math.min(termTime, nextArrTime);
            double et = Math.round((eventTime - prevTime) * 1000.0) / 1000.0;
            double hwJob = Math.round((et * cpuShare) * 1000.0) / 1000.0;

            // Deduct work done from all active jobs during this interval
            for (SimJob j : active) {
                j.timeLeft = Math.max(0, Math.round((j.timeLeft - hwJob) * 1000.0) / 1000.0);
            }

            // Identify arrivals at eventTime
            List<SimJob> arrivingThisStep = new ArrayList<>();
            List<String> arrivingNames = new ArrayList<>();
            while (jobIdx < nTotal && Math.abs(simJobs.get(jobIdx).at - eventTime) < 1e-4) {
                SimJob arr = simJobs.get(jobIdx);
                arrivingThisStep.add(arr);
                arrivingNames.add(arr.name);
                jobIdx++;
            }

            // Identify terminations at eventTime
            List<SimJob> terminatingThisStep = new ArrayList<>();
            List<String> termNames = new ArrayList<>();
            for (SimJob j : active) {
                if (j.timeLeft <= 1e-6) {
                    j.timeLeft = 0.0;
                    j.finishTime = eventTime;
                    terminatingThisStep.add(j);
                    termNames.add(j.name);
                }
            }

            EventStep step = new EventStep();
            step.time = eventTime;
            step.nj = currentNj; // Active during this interval
            step.cpuPct = cpuShare;
            step.et = et;
            step.hwJob = hwJob;

            for (String a : arrivingNames) step.eventList.add(a + " a/s");
            for (String t : termNames) step.eventList.add(t + " t");
            step.event = String.join(", ", step.eventList);
            step.details = String.join(", ", step.eventList);
            step.terminatedThisStep.addAll(termNames);

            // Populate interval jobs for classroom table display: active running jobs + newly arrived
            for (SimJob j : active) {
                step.intervalJobs.add(new BlockJobItem(j.name, j.timeLeft));
            }
            for (SimJob arr : arrivingThisStep) {
                step.intervalJobs.add(new BlockJobItem(arr.name, arr.timeLeft));
            }

            for (SimJob sj : simJobs) {
                if (sj.finishTime != null && sj.finishTime <= step.time) {
                    step.timeLeftMap.put(sj.name, 0.0);
                } else if (active.contains(sj) || arrivingThisStep.contains(sj)) {
                    step.timeLeftMap.put(sj.name, sj.timeLeft);
                    step.activeJobNames.add(sj.name);
                }
            }

            computedSteps.add(step);

            // Update active list for next step: remove terminated, add arriving
            active.removeAll(terminatingThisStep);
            active.addAll(arrivingThisStep);
            prevTime = eventTime;
        }

        // 3. Compute Summary Metrics (TAT, WT, Averages)
        double totalTat = 0;
        double totalWt = 0;
        for (SimJob sj : simJobs) {
            double ft = (sj.finishTime != null) ? sj.finishTime : prevTime;
            double tat = Math.round((ft - sj.at) * 1000.0) / 1000.0;
            double wt = Math.round((tat - sj.bt) * 1000.0) / 1000.0;
            totalTat += tat;
            totalWt += wt;
            computedSummaries.add(new JobSummary(sj.name, sj.at, sj.bt, ft, tat, wt, sj.dishType));
        }

        double avgTat = totalTat / simJobs.size();
        double avgWt = totalWt / simJobs.size();

        // 4. Populate Result Tables
        populateTraceTable();
        populateSummaryTable(avgTat, avgWt);

        // Reset step pointer to start
        currentStepIndex = 0;
        updateHuds();
        if (canvas != null) canvas.repaint();
    }

    private void populateTraceTable() {
        rowToStepMap.clear();
        blockEndRows.clear();

        Vector<String> colNames = new Vector<>();
        Vector<Vector<Object>> data = new Vector<>();

        if (isVerticalClassroomMode) {
            // 1. CLASSROOM VERTICAL EXCEL FORMAT (Exact layout requested by user!)
            colNames.add("TIME");
            colNames.add("EVENT");
            colNames.add("nj");
            colNames.add("%cpu/job");
            colNames.add("et");
            colNames.add("hw/job");
            colNames.add("JOB#");
            colNames.add("TIME LEFT");

            int rowIndex = 0;
            for (int s = 0; s < computedSteps.size(); s++) {
                EventStep step = computedSteps.get(s);
                int numRows = Math.max(1, Math.max(step.intervalJobs.size(), step.eventList.size()));

                for (int r = 0; r < numRows; r++) {
                    Vector<Object> row = new Vector<>();
                    row.add((r == 0) ? String.format("%.2f", step.time) : "");

                    String evStr = (r < step.eventList.size()) ? step.eventList.get(r) : "";
                    if (evStr.contains(" t")) {
                        evStr = evStr + " [FINISHED]";
                    }
                    row.add(evStr);

                    row.add((r == 0) ? String.valueOf(step.nj) : "");
                    row.add((r == 0) ? (step.nj > 0 ? String.format("%.3f", step.cpuPct) : "0") : "");
                    row.add((r == 0) ? String.format("%.2f", step.et) : "");
                    row.add((r == 0) ? String.format("%.3f", step.hwJob) : "");
                    row.add((r < step.intervalJobs.size()) ? step.intervalJobs.get(r).name : "");

                    String tlStr = "";
                    if (r < step.intervalJobs.size()) {
                        double tlVal = step.intervalJobs.get(r).timeLeft;
                        if (tlVal <= 1e-6) {
                            tlStr = "0.000 [FINISHED]";
                        } else {
                            tlStr = String.format("%.3f", tlVal);
                        }
                    }
                    row.add(tlStr);

                    data.add(row);
                    rowToStepMap.add(s);
                    if (r == numRows - 1) {
                        blockEndRows.add(rowIndex);
                    }
                    rowIndex++;
                }
            }

        } else {
            // 2. WIDE MATRIX FORMAT (Horizontal J1..J5 Columns)
            colNames.add("TIME");
            colNames.add("EVENT");
            colNames.add("nj");
            colNames.add("%cpu/nj");
            colNames.add("et");
            colNames.add("hw/job");

            for (JobInput j : currentJobList) {
                colNames.add(j.name);
            }

            for (int s = 0; s < computedSteps.size(); s++) {
                EventStep step = computedSteps.get(s);
                Vector<Object> row = new Vector<>();
                row.add(String.format("%.2f", step.time));
                row.add(step.event.contains(" t") ? step.event + " [FINISHED]" : step.event);
                row.add(step.nj);
                row.add(String.format("%.3f", step.cpuPct));
                row.add(String.format("%.2f", step.et));
                row.add(String.format("%.3f", step.hwJob));

                for (JobInput j : currentJobList) {
                    JobSummary summary = getJobSummary(j.name);
                    boolean hasTerminatedBefore = (summary != null && step.time > summary.ft + 1e-6);
                    boolean isTerminatingThisStep = (summary != null && Math.abs(step.time - summary.ft) < 1e-6);
                    boolean hasNotArrived = (step.time < j.at - 1e-6);

                    if (useEmptySpacesMode) {
                        if (hasNotArrived || hasTerminatedBefore) {
                            row.add("");
                        } else if (isTerminatingThisStep) {
                            row.add("0.000 [FINISHED]");
                        } else {
                            Double tl = step.timeLeftMap.get(j.name);
                            row.add(String.format("%.3f", (tl != null ? tl : j.bt)));
                        }
                    } else {
                        if (hasNotArrived) {
                            row.add(String.format("%.3f", j.bt));
                        } else if (hasTerminatedBefore || isTerminatingThisStep) {
                            row.add("0.000 [FINISHED]");
                        } else {
                            Double tl = step.timeLeftMap.get(j.name);
                            row.add(String.format("%.3f", (tl != null ? tl : j.bt)));
                        }
                    }
                }
                data.add(row);
                rowToStepMap.add(s);
                blockEndRows.add(s);
            }
        }

        traceTableModel.setDataVector(data, colNames);
        applyRetroHeaderRenderer(traceTable);

        traceTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        if (isVerticalClassroomMode) {
            if (traceTable.getColumnCount() >= 8) {
                traceTable.getColumnModel().getColumn(0).setPreferredWidth(70); // TIME
                traceTable.getColumnModel().getColumn(1).setPreferredWidth(85); // EVENT
                traceTable.getColumnModel().getColumn(2).setPreferredWidth(45); // nj
                traceTable.getColumnModel().getColumn(3).setPreferredWidth(85); // %cpu/job
                traceTable.getColumnModel().getColumn(4).setPreferredWidth(55); // et
                traceTable.getColumnModel().getColumn(5).setPreferredWidth(75); // hw/job
                traceTable.getColumnModel().getColumn(6).setPreferredWidth(65); // JOB#
                traceTable.getColumnModel().getColumn(7).setPreferredWidth(95); // TIME LEFT
            }
        } else {
            if (traceTable.getColumnCount() >= 6) {
                traceTable.getColumnModel().getColumn(0).setPreferredWidth(55);
                traceTable.getColumnModel().getColumn(1).setPreferredWidth(50);
                traceTable.getColumnModel().getColumn(2).setPreferredWidth(35);
                traceTable.getColumnModel().getColumn(3).setPreferredWidth(65);
                traceTable.getColumnModel().getColumn(4).setPreferredWidth(45);
                traceTable.getColumnModel().getColumn(5).setPreferredWidth(60);
                for (int i = 6; i < traceTable.getColumnCount(); i++) {
                    traceTable.getColumnModel().getColumn(i).setPreferredWidth(80);
                }
            }
        }

        // Apply our custom Dark/Cyber Block Renderer to EVERY column!
        TraceTableCellRenderer renderer = new TraceTableCellRenderer();
        for (int i = 0; i < traceTable.getColumnCount(); i++) {
            traceTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        traceTable.repaint();
    }

    private class TraceTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, false, false, row, col);
            setHorizontalAlignment(SwingConstants.CENTER);

            int stepForThisRow = (row >= 0 && row < rowToStepMap.size()) ? rowToStepMap.get(row) : -1;
            boolean isCurrentStep = (stepForThisRow == currentStepIndex);
            boolean isBlockEnd = blockEndRows.contains(row);

            if (isCurrentStep) {
                c.setBackground(new Color(65, 45, 15)); // Radiant Deep Amber/Gold
                c.setForeground(new Color(255, 230, 80)); // Bright Neon Gold
                setFont(getFont().deriveFont(Font.BOLD, 12f));
            } else {
                Color blockBg = (stepForThisRow % 2 == 0) ? new Color(20, 14, 36) : new Color(26, 18, 46);
                c.setBackground(blockBg);

                if (isVerticalClassroomMode) {
                    if (col == 0) { // TIME
                        c.setForeground(RetroTheme.ACCENT_YELLOW);
                        setFont(getFont().deriveFont(Font.BOLD, 12f));
                    } else if (col == 1) { // EVENT
                        String s = String.valueOf(value);
                        if (s.contains("a/s")) {
                            c.setForeground(RetroTheme.ACCENT_CYAN);
                        } else if (s.contains("t") || s.contains("FINISHED") || s.contains("COMPLETED")) {
                            c.setForeground(new Color(0, 255, 140)); // Emerald Green for termination
                        } else {
                            c.setForeground(Color.WHITE);
                        }
                        setFont(getFont().deriveFont(Font.BOLD, 11f));
                    } else if (col == 6) { // JOB#
                        c.setForeground(RetroTheme.ACCENT_CYAN);
                        setFont(getFont().deriveFont(Font.BOLD, 12f));
                    } else if (col == 7) { // TIME LEFT
                        String s = String.valueOf(value);
                        if (s.contains("FINISHED") || s.contains("COMPLETED") || "0.000".equals(s) || "0".equals(s)) {
                            c.setForeground(new Color(0, 255, 140)); // Bright Neon Green for completed
                            c.setBackground(isCurrentStep ? new Color(65, 45, 15) : new Color(14, 45, 26)); // Glowing dark green
                            setFont(getFont().deriveFont(Font.BOLD, 12f));
                        } else if (!s.isEmpty()) {
                            c.setForeground(new Color(0, 230, 255)); // Active Timeleft
                            setFont(getFont().deriveFont(Font.BOLD, 12f));
                        } else {
                            c.setForeground(Color.WHITE);
                        }
                    } else {
                        c.setForeground(new Color(220, 225, 245));
                        setFont(getFont().deriveFont(Font.PLAIN, 12f));
                    }
                } else {
                    if (col == 0) {
                        c.setForeground(RetroTheme.ACCENT_YELLOW);
                        setFont(getFont().deriveFont(Font.BOLD, 12f));
                    } else if (col == 1) {
                        String s = String.valueOf(value);
                        c.setForeground(s.startsWith("a/s") ? RetroTheme.ACCENT_CYAN : new Color(0, 255, 140));
                        setFont(getFont().deriveFont(Font.BOLD, 11f));
                    } else if (col >= 6) {
                        String s = String.valueOf(value);
                        if (s.isEmpty()) {
                            c.setForeground(new Color(100, 90, 130));
                        } else if (s.contains("FINISHED") || s.contains("COMPLETED") || "0.000".equals(s)) {
                            c.setForeground(new Color(0, 255, 140));
                            c.setBackground(isCurrentStep ? new Color(65, 45, 15) : new Color(14, 45, 26));
                            setFont(getFont().deriveFont(Font.BOLD, 12f));
                        } else {
                            c.setForeground(new Color(0, 255, 255));
                            setFont(getFont().deriveFont(Font.BOLD, 12f));
                        }
                    } else {
                        c.setForeground(new Color(220, 225, 245));
                        setFont(getFont().deriveFont(Font.PLAIN, 12f));
                    }
                }
            }

            // Crisp cyber divider separating each event block
            if (isBlockEnd) {
                ((JComponent) c).setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, isCurrentStep ? new Color(255, 215, 0) : new Color(110, 80, 160)));
            } else {
                ((JComponent) c).setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 28, 62)));
            }

            return c;
        }
    }

    public JobSummary getJobSummary(String name) {
        for (JobSummary s : computedSummaries) {
            if (s.name.equalsIgnoreCase(name)) return s;
        }
        return null;
    }

    private void populateSummaryTable(double avgTat, double avgWt) {
        summaryTableModel.setRowCount(0);

        for (JobSummary s : computedSummaries) {
            summaryTableModel.addRow(new Object[]{
                    s.name,
                    DISH_NAMES[s.dishType % 6],
                    String.format("%.2f", s.at),
                    String.format("%.2f", s.bt),
                    String.format("%.2f", s.ft),
                    String.format("%.2f", s.tat),
                    String.format("%.2f", s.wt)
            });
        }

        // Summary Average Row
        summaryTableModel.addRow(new Object[]{
                "AVERAGE",
                "-",
                "-",
                "-",
                "-",
                String.format("AVG TAT = %.3f", avgTat),
                String.format("AVG WT = %.3f", avgWt)
        });
    }

    // =========================================================================
    // 5. STEPPING & PLAYBACK CONTROLS
    // =========================================================================
    public void togglePlayPause() {
        if (isPlaying) {
            pauseSimulation();
        } else {
            startSimulation();
        }
    }

    public void startSimulation() {
        if (computedSteps.isEmpty()) solveAndSimulate();
        if (currentStepIndex >= computedSteps.size() - 1) {
            currentStepIndex = 0;
            updateHuds();
            if (canvas != null) canvas.repaint();
            traceTable.repaint();
        }
        isPlaying = true;
        btnPlay.setText("PAUSE");
        btnPlay.setIcon(RetroIcons.createPauseIcon(14, Color.WHITE));
        btnPlay.setBackground(new Color(180, 110, 0));
        playTimer.start();
    }

    public void pauseSimulation() {
        isPlaying = false;
        btnPlay.setText("START SIMULATION");
        btnPlay.setIcon(RetroIcons.createPlayIcon(14, Color.WHITE));
        btnPlay.setBackground(new Color(0, 150, 75));
        playTimer.stop();
    }

    public void stepForward() {
        if (computedSteps.isEmpty()) solveAndSimulate();
        if (currentStepIndex < computedSteps.size() - 1) {
            currentStepIndex++;
            updateHuds();
            if (canvas != null) canvas.repaint();
            traceTable.repaint();

            // Reached final completion step: immediately stop timer so it NEVER auto-loops
            if (currentStepIndex >= computedSteps.size() - 1) {
                pauseSimulation();
                triggerLevelClear();
            } else if (isChallengeMode && !isPlaying) {
                setupChallengeForStep(currentStepIndex);
            }
        } else {
            pauseSimulation();
            triggerLevelClear();
        }
    }

    private void triggerLevelClear() {
        if (btnPlay != null) {
            btnPlay.setText("REPLAY LEVEL");
            btnPlay.setIcon(RetroIcons.createPlayIcon(14, Color.WHITE));
            btnPlay.setBackground(new Color(0, 120, 180));
        }
        if (lblStatusPill != null) {
            lblStatusPill.setText(" [ STATUS: CONGRATULATIONS // ALL ORDERS FINISHED! +100XP ] ");
            lblStatusPill.setForeground(new Color(255, 220, 0));
            lblStatusPill.setBorder(BorderFactory.createLineBorder(new Color(255, 220, 0), 2));
        }
        AppState.addScore(100);
        if (canvas != null) canvas.repaint();
    }

    public void stepBackward() {
        if (currentStepIndex > 0) {
            currentStepIndex--;
            updateHuds();
            if (canvas != null) canvas.repaint();
            traceTable.repaint();
        }
    }

    public void resetSimulation() {
        pauseSimulation();
        currentStepIndex = 0;
        if (canvas != null) canvas.kitchenFaults = 0;
        updateHuds();
        isChallengeMode = false;
        awaitingChallengeChoice = false;
        challengePromptText = "";
        if (btnPlay != null) {
            btnPlay.setText("START SIMULATION");
            btnPlay.setIcon(RetroIcons.createPlayIcon(14, Color.WHITE));
            btnPlay.setBackground(new Color(0, 150, 75));
        }
        if (lblStatusPill != null) {
            lblStatusPill.setText(" [ STATUS: READY // CLICK START ] ");
            lblStatusPill.setForeground(new Color(0, 240, 100));
            lblStatusPill.setBorder(BorderFactory.createLineBorder(new Color(0, 240, 100), 1));
        }
        if (canvas != null) canvas.repaint();
        traceTable.repaint();
    }

    private void cycleSpeed() {
        if (speedMultiplier == 1.0) {
            speedMultiplier = 2.0;
            btnSpeed.setText("SPEED: 2.0x");
            playTimer.setDelay(600);
        } else if (speedMultiplier == 2.0) {
            speedMultiplier = 0.5;
            btnSpeed.setText("SPEED: 0.5x");
            playTimer.setDelay(2000);
        } else {
            speedMultiplier = 1.0;
            btnSpeed.setText("SPEED: 1.0x");
            playTimer.setDelay(1200);
        }
    }

    private void updateHuds() {
        if (computedSteps.isEmpty() || currentStepIndex < 0 || currentStepIndex >= computedSteps.size()) {
            lblHudActiveNj.setText("nj = 0 JOBS");
            lblHudCpuShare.setText("CPU SHARE: %cpu/nj = 0.0%");
            lblHudTime.setText("TIME: 0.00");
            lblHudEtHw.setText("et = 0.00 | hw/job = 0.000");
            return;
        }

        EventStep step = computedSteps.get(currentStepIndex);
        lblHudActiveNj.setText("nj = " + step.nj + " JOBS");
        int memPoolMB = 512;
        int memPerJob = (step.nj > 0) ? (memPoolMB / step.nj) : 0;
        lblHudCpuShare.setText(String.format("CPU: %.1f%% | RAM: %dMB/JOB", step.cpuPct * 100.0, memPerJob));

        lblHudTime.setText(String.format("TIME: %.2f [%s]", step.time, step.event));
        lblHudEtHw.setText(String.format("et = %.2f | hw/job = %.3f", step.et, step.hwJob));

        if (!computedSummaries.isEmpty()) {
            double totalTat = 0, totalWt = 0;
            for (JobSummary s : computedSummaries) {
                totalTat += s.tat;
                totalWt += s.wt;
            }
            double aTat = totalTat / computedSummaries.size();
            double aWt = totalWt / computedSummaries.size();
            lblHudAvgTat.setText(String.format("AVG TAT: %.2f", aTat));
            lblHudAvgWt.setText(String.format("AVG WT: %.2f | STEP %d/%d", aWt, (currentStepIndex + 1), computedSteps.size()));
        }

        boolean isAllDone = (currentStepIndex == computedSteps.size() - 1);
        if (isAllDone) {
            double avgTat = !computedSummaries.isEmpty() ? computedSummaries.stream().mapToDouble(s -> s.tat).average().orElse(0) : 0;
            double avgWt = !computedSummaries.isEmpty() ? computedSummaries.stream().mapToDouble(s -> s.wt).average().orElse(0) : 0;
            lblFooterDetails.setText(String.format("CONGRATULATIONS! (T=%.2f) - All %d orders finished successfully! (AVG TAT: %.2f, AVG WT: %.2f)",
                    step.time, currentJobList.size(), avgTat, avgWt));
            lblFooterDetails.setForeground(new Color(0, 255, 140));

            if (lblStatusPill != null) {
                lblStatusPill.setText(" [ STATUS: CONGRATULATIONS // ALL ORDERS FINISHED! +100XP ] ");
                lblStatusPill.setForeground(new Color(255, 220, 0));
                lblStatusPill.setBorder(BorderFactory.createLineBorder(new Color(255, 220, 0), 2));
            }
        } else {
            lblFooterDetails.setText(String.format("EVENT: [ %s at T=%.2f ] - %s. (Active nj=%d, hw/job=%.3f)",
                    step.event, step.time, step.details, step.nj, step.hwJob));
            lblFooterDetails.setForeground(RetroTheme.ACCENT_CYAN);
        }

        // Auto scroll trace table to highlight current step row/block
        if (traceTable != null && currentStepIndex >= 0) {
            int targetRow = -1;
            for (int r = 0; r < rowToStepMap.size(); r++) {
                if (rowToStepMap.get(r) == currentStepIndex) {
                    targetRow = r;
                    break;
                }
            }
            if (targetRow >= 0 && targetRow < traceTable.getRowCount()) {
                traceTable.scrollRectToVisible(traceTable.getCellRect(targetRow, 0, true));
            }
            traceTable.repaint();
        }
    }

    /**
     * Interactive Order Selection from Kitchen Canvas
     */
    public void selectJobFromCanvas(int jobIdx) {
        if (jobIdx < 0 || jobIdx >= currentJobList.size()) return;
        JobInput job = currentJobList.get(jobIdx);

        // Update status pill with selected job diagnostics
        if (lblStatusPill != null) {
            JobSummary sum = getJobSummary(job.name);
            EventStep step = (currentStepIndex >= 0 && currentStepIndex < computedSteps.size()) ? computedSteps.get(currentStepIndex) : null;
            boolean isDone = (sum != null && step != null && step.time >= sum.ft - 1e-6);
            String stat = isDone ? ("SERVED (FT: " + String.format("%.2f", sum.ft) + ")")
                    : ("BURST: " + String.format("%.2f", job.bt) + "s (AT: " + String.format("%.2f", job.at) + ")");
            lblStatusPill.setText(" [ ORDER SELECTED: " + job.name + " // " + stat + " ] ");
            lblStatusPill.setForeground(RetroTheme.ACCENT_CYAN);
            lblStatusPill.setBorder(BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 1));
        }

        // Highlight corresponding column in execution trace table if in wide matrix mode
        if (!isVerticalClassroomMode && traceTable != null && jobIdx + 6 < traceTable.getColumnCount()) {
            traceTable.scrollRectToVisible(traceTable.getCellRect(Math.max(0, currentStepIndex), jobIdx + 6, true));
        }
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

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "STEP_NEXT");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "STEP_NEXT");
        getActionMap().put("STEP_NEXT", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!isShowing()) return;
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof javax.swing.text.JTextComponent) return;
                stepForward();
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "STEP_PREV");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "STEP_PREV");
        getActionMap().put("STEP_PREV", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!isShowing()) return;
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (focusOwner instanceof javax.swing.text.JTextComponent) return;
                stepBackward();
            }
        });

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
    // 6. RETRO OVERCOOKED CHEF CANVAS (PROCESSOR SHARING VISUALIZER & INTERACTIVE KITCHEN)
    // =========================================================================
    public static class OvercookedKitchenCanvas extends JPanel {
        private final MultiprogrammingPanel parent;
        private int animTick = 0;
        private final javax.swing.Timer repaintTimer;

        // Visual FX Entities
        static class KitchenParticle {
            double x, y;
            double vx, vy;
            Color color;
            int life, maxLife;
            int size;

            KitchenParticle(double x, double y, double vx, double vy, Color color, int maxLife, int size) {
                this.x = x; this.y = y; this.vx = vx; this.vy = vy;
                this.color = color; this.life = maxLife; this.maxLife = maxLife;
                this.size = size;
            }
        }

        static class FloatingText {
            double x, y;
            String text;
            Color color;
            int life, maxLife;

            FloatingText(double x, double y, String text, Color color) {
                this.x = x; this.y = y; this.text = text; this.color = color;
                this.life = 45; this.maxLife = 45;
            }
        }

        private final List<KitchenParticle> particles = new ArrayList<>();
        private final List<FloatingText> floatingTexts = new ArrayList<>();

        // Moving Chef (CPU) Dynamics
        private double chefCurrentX = 28.0;
        private double chefCurrentY = 152.0;
        private double chefTargetX = 28.0;
        private double chefTargetY = 152.0;
        private boolean facingRight = true;
        private boolean isWalking = false;
        private int walkBob = 0;

        // Interactive Dispatching & Multi-Tasking Patrol
        private int userAssignedJobIdx = -1;
        private int userClickTimeout = 0;
        private int autoCycleTimer = 0;
        private int autoCyclePtr = 0;
        private String chefDialogue = "READY // WAITING FOR ORDERS";
        private int hoveredJobIdx = -1;
        private int selectedJobIdx = -1;
        private boolean hoveredChef = false;
        public int kitchenFaults = 0;

        public OvercookedKitchenCanvas(MultiprogrammingPanel parent) {
            this.parent = parent;
            setOpaque(true);
            setBackground(new Color(15, 10, 28));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)
            ));

            // Mouse Interactive Listeners (Click orders to direct Chef & earn XP)
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleCanvasClick(e.getX(), e.getY());
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    handleCanvasHover(e.getX(), e.getY());
                }
            });

            // 40ms high-framerate animation loop (~25 FPS)
            repaintTimer = new javax.swing.Timer(40, e -> {
                animTick++;
                updateCanvasPhysics();
                repaint();
            });
            repaintTimer.start();
        }

        private int computeSlotWidth(int w, int numJobs) {
            int minLeft = 85;
            int totalAvailableWidth = w - minLeft - 15;
            return Math.max(68, Math.min(145, totalAvailableWidth / Math.max(1, numJobs)));
        }

        private int computeCounterStartX(int w, int numJobs, int slotWidth) {
            int minLeft = 85;
            int rightMargin = 15;
            int totalAvailableWidth = w - minLeft - rightMargin;
            int totalSpan = numJobs * slotWidth;
            if (totalSpan < totalAvailableWidth) {
                return minLeft + (totalAvailableWidth - totalSpan) / 2;
            }
            return minLeft;
        }

        private void updateCanvasPhysics() {
            // 1. Update Particles
            Iterator<KitchenParticle> itP = particles.iterator();
            while (itP.hasNext()) {
                KitchenParticle p = itP.next();
                p.x += p.vx;
                p.y += p.vy;
                p.life--;
                if (p.life <= 0) itP.remove();
            }

            // 2. Update Floating Texts
            Iterator<FloatingText> itT = floatingTexts.iterator();
            while (itT.hasNext()) {
                FloatingText t = itT.next();
                t.y -= 0.7;
                t.life--;
                if (t.life <= 0) itT.remove();
            }

            // 3. User Target Timeout
            if (userClickTimeout > 0) {
                userClickTimeout--;
                if (userClickTimeout <= 0) {
                    userAssignedJobIdx = -1;
                }
            }

            // 4. Determine Chef Target Position
            int w = getWidth();
            int numJobs = Math.max(1, parent.currentJobList.size());
            int slotWidth = computeSlotWidth(w, numJobs);
            int counterStartX = computeCounterStartX(w, numJobs, slotWidth);

            EventStep step = (parent.currentStepIndex >= 0 && parent.currentStepIndex < parent.computedSteps.size())
                    ? parent.computedSteps.get(parent.currentStepIndex) : null;

            if (userAssignedJobIdx >= 0 && userAssignedJobIdx < parent.currentJobList.size()) {
                // User manually clicked this order!
                int sx = counterStartX + userAssignedJobIdx * slotWidth;
                chefTargetX = sx + (slotWidth - 10) / 2 - 22;
                JobInput j = parent.currentJobList.get(userAssignedJobIdx);
                chefDialogue = "COOKING " + j.name + " (" + DISH_NAMES[j.dishType % 6] + ")!";
            } else if (step == null || step.activeJobNames.isEmpty()) {
                // Idle State -> Return to Master Chef Base on Left
                chefTargetX = 28.0;
                if (parent.currentStepIndex >= 0 && parent.currentStepIndex == parent.computedSteps.size() - 1) {
                    chefDialogue = "CONGRATS! ALL ORDERS FINISHED!";
                } else {
                    chefDialogue = "IDLE // READY FOR ORDERS";
                }
            } else {
                // Active Concurrency (nj > 0)
                List<Integer> activeIndices = new ArrayList<>();
                for (int i = 0; i < parent.currentJobList.size(); i++) {
                    if (step.activeJobNames.contains(parent.currentJobList.get(i).name)) {
                        activeIndices.add(i);
                    }
                }

                if (activeIndices.size() == 1) {
                    // Single Active Job
                    int idx = activeIndices.get(0);
                    int sx = counterStartX + idx * slotWidth;
                    chefTargetX = sx + (slotWidth - 10) / 2 - 22;
                    JobInput j = parent.currentJobList.get(idx);
                    chefDialogue = "COOKING " + j.name + " (" + DISH_NAMES[j.dishType % 6] + " - 100% CPU)";
                } else if (!activeIndices.isEmpty()) {
                    // MULTIPROGRAMMING TIME-SHARING! Chef patrols between active stations!
                    autoCycleTimer++;
                    if (autoCycleTimer >= 32) { // Every ~1.3 seconds, cycle to next active station!
                        autoCycleTimer = 0;
                        autoCyclePtr = (autoCyclePtr + 1) % activeIndices.size();
                    }
                    if (autoCyclePtr >= activeIndices.size()) autoCyclePtr = 0;
                    int idx = activeIndices.get(autoCyclePtr);
                    int sx = counterStartX + idx * slotWidth;
                    chefTargetX = sx + (slotWidth - 10) / 2 - 22;
                    JobInput j = parent.currentJobList.get(idx);
                    chefDialogue = String.format("SHARING CPU (%.0f%%) ➔ %s (%s)", step.cpuPct * 100.0, j.name, DISH_NAMES[j.dishType % 6]);
                }
            }

            // 5. Smooth Movement Interpolation
            double dx = chefTargetX - chefCurrentX;
            if (Math.abs(dx) > 2.0) {
                chefCurrentX += dx * 0.18;
                facingRight = (dx > 0);
                isWalking = true;
                walkBob = (int)(Math.abs(Math.sin(animTick * 0.45)) * 6);
            } else {
                chefCurrentX = chefTargetX;
                isWalking = false;
                walkBob = 0;
            }

            // 6. Ambient Spawning: Footstep Dust when walking, Cooking Steam when stationary at stove
            if (isWalking && animTick % 4 == 0) {
                double footX = chefCurrentX + (facingRight ? 8 : 34);
                particles.add(new KitchenParticle(footX, chefCurrentY + 40, (Math.random() - 0.5) * 0.8, -0.4,
                        new Color(100, 80, 130, 140), 15, 3));
            } else if (!isWalking && step != null && step.nj > 0 && animTick % 3 == 0) {
                double potX = chefCurrentX + (facingRight ? 36 : 6);
                particles.add(new KitchenParticle(potX, chefCurrentY + 12, (Math.random() - 0.5) * 0.6, -1.0 - Math.random() * 0.5,
                        new Color(240, 245, 255, 170), 20, 4));
                if (Math.random() < 0.25) {
                    particles.add(new KitchenParticle(potX + (Math.random() - 0.5) * 8, chefCurrentY + 14,
                            (Math.random() - 0.5) * 1.5, -1.2, new Color(255, 200, 40), 16, 3));
                }
            }
        }

        private void handleCanvasClick(int mx, int my) {
            int w = getWidth();
            int h = getHeight();
            int numJobs = Math.max(1, parent.currentJobList.size());
            int slotWidth = computeSlotWidth(w, numJobs);
            int counterStartX = computeCounterStartX(w, numJobs, slotWidth);
            int slotHeight = 88;
            int counterY = 38;

            // 1. Click on Challenge Prompt Box Banner (Allows clicking anywhere on the yellow prompt box to advance!)
            if (parent.isChallengeMode && parent.awaitingChallengeChoice && !parent.challengePromptText.isEmpty()) {
                int bannerW = Math.min(740, w - 24);
                int bannerH = 30;
                int bannerX = (w - bannerW) / 2;
                int bannerY = (h - 24) - 30;
                Rectangle bannerRect = new Rectangle(bannerX, bannerY, bannerW, bannerH);
                if (bannerRect.contains(mx, my)) {
                    parent.answerChallenge(true);
                    repaint();
                    return;
                }
            }

            // 2. Click on Chef Sprite (Jump & Easter Egg Voice)
            Rectangle chefRect = new Rectangle((int)chefCurrentX, (int)chefCurrentY - walkBob, 44, 44);
            if (chefRect.contains(mx, my)) {
                walkBob = 12;
                floatingTexts.add(new FloatingText(chefCurrentX - 10, chefCurrentY - 24, "YES, CHEF!", new Color(255, 230, 0)));
                chefDialogue = "HEAD CHEF CPU: 100% READY!";
                AppState.addScore(10);
                for (int p = 0; p < 8; p++) {
                    particles.add(new KitchenParticle(chefCurrentX + 22, chefCurrentY + 15,
                            (Math.random() - 0.5) * 3, (Math.random() - 0.5) * 3, new Color(255, 220, 50), 22, 4));
                }
                repaint();
                return;
            }

            // 3. Click on Order Ticket Card or Stove
            EventStep step = (parent.currentStepIndex >= 0 && parent.currentStepIndex < parent.computedSteps.size())
                    ? parent.computedSteps.get(parent.currentStepIndex) : null;

            // Find the immediate next incoming job index in chronological queue
            int nextIncomingJobIdx = -1;
            for (int k = 0; k < parent.currentJobList.size(); k++) {
                if (step != null && step.time < parent.currentJobList.get(k).at - 1e-6) {
                    nextIncomingJobIdx = k;
                    break;
                }
            }

            for (int i = 0; i < parent.currentJobList.size(); i++) {
                int sx = counterStartX + i * slotWidth;
                Rectangle cardRect = new Rectangle(sx, counterY, slotWidth - 10, slotHeight + 45);
                if (cardRect.contains(mx, my)) {
                    selectedJobIdx = i;

                    JobInput job = parent.currentJobList.get(i);
                    JobSummary summary = parent.getJobSummary(job.name);
                    boolean isCompleted = (summary != null && step != null && step.time >= summary.ft - 1e-6);
                    boolean isWaiting = (step != null && step.time < job.at - 1e-6);
                    boolean isActive = (step != null && step.activeJobNames.contains(job.name));
                    boolean isTerminatedNow = (step != null && step.terminatedThisStep.contains(job.name));

                    Double timeLeft = null;
                    if (isCompleted) {
                        timeLeft = 0.0;
                    } else if (step != null) {
                        timeLeft = step.timeLeftMap.get(job.name);
                    }
                    if (timeLeft == null) timeLeft = Double.valueOf(job.bt);

                    if (isTerminatedNow) {
                        // CASE A1: Dish just finished cooking on this exact step! Ready to be served to customer!
                        userAssignedJobIdx = -1;
                        AppState.addScore(50);
                        floatingTexts.add(new FloatingText(sx + 6, counterY + 25, "+50 XP SERVED " + job.name + "! ➔", new Color(0, 240, 140)));
                        chefDialogue = job.name + " (" + DISH_NAMES[job.dishType % 6] + ") served to customer! Order up!";

                        for (int p = 0; p < 14; p++) {
                            particles.add(new KitchenParticle(sx + (slotWidth - 10) / 2, counterY + 45,
                                    (Math.random() - 0.5) * 4.0, (Math.random() - 0.5) * 4.0, new Color(0, 240, 180), 28, 4));
                        }

                        if (parent.isChallengeMode) {
                            parent.answerChallenge(true);
                        } else {
                            parent.stepForward();
                        }
                        parent.selectJobFromCanvas(i);
                        repaint();
                        return;

                    } else if (isCompleted) {
                        // CASE A2: Already served in an earlier step -> Informative click, does NOT advance timeline
                        AppState.addScore(5);
                        floatingTexts.add(new FloatingText(sx + 6, counterY + 25, job.name + " ALREADY SERVED!", new Color(0, 240, 140)));
                        chefDialogue = job.name + " was delivered at T=" + String.format("%.2f", summary != null ? summary.ft : 0.0) + "!";

                        for (int p = 0; p < 6; p++) {
                            particles.add(new KitchenParticle(sx + (slotWidth - 10) / 2, counterY + 45,
                                    (Math.random() - 0.5) * 2.0, (Math.random() - 0.5) * 2.0, new Color(0, 240, 100), 20, 3));
                        }
                        parent.selectJobFromCanvas(i);
                        repaint();
                        return;

                    } else if (isActive) {
                        // CASE B: Clicked on an active dish currently cooking -> Focuses chef to chop & stir without skipping timeline!
                        userAssignedJobIdx = i;
                        userClickTimeout = 85;
                        chefTargetX = sx + (slotWidth - 10) / 2 - 22;
                        AppState.addScore(10);
                        floatingTexts.add(new FloatingText(sx + 6, counterY + 25, "+10 XP STIR & CHOP! (Left: " + String.format("%.2f", timeLeft) + "s)", RetroTheme.ACCENT_CYAN));
                        chefDialogue = "FOCUSING ON " + job.name + " (" + DISH_NAMES[job.dishType % 6] + ")! Still cooking...";

                        for (int p = 0; p < 12; p++) {
                            particles.add(new KitchenParticle(sx + (slotWidth - 10) / 2, counterY + 45,
                                    (Math.random() - 0.5) * 3.5, (Math.random() - 0.5) * 3.5,
                                    (p % 2 == 0 ? new Color(0, 255, 255) : new Color(255, 210, 30)), 24, 4));
                        }

                        parent.selectJobFromCanvas(i);
                        repaint();
                        return;

                    } else if (i == nextIncomingJobIdx) {
                        // CASE C: Clicked on the NEXT incoming order in queue -> Dispatches / Admits it immediately!
                        userAssignedJobIdx = i;
                        userClickTimeout = 85;
                        chefTargetX = sx + (slotWidth - 10) / 2 - 22;
                        AppState.addScore(50);
                        floatingTexts.add(new FloatingText(sx + 6, counterY + 25, "+50 XP ADMITTED! ➔", new Color(255, 230, 0)));
                        chefDialogue = "DISPATCHING " + job.name + " TO KITCHEN STOVE!";

                        for (int p = 0; p < 12; p++) {
                            particles.add(new KitchenParticle(sx + (slotWidth - 10) / 2, counterY + 45,
                                    (Math.random() - 0.5) * 3.5, (Math.random() - 0.5) * 3.5, new Color(255, 210, 30), 25, 4));
                        }

                        if (parent.isChallengeMode) {
                            parent.answerChallenge(true);
                        } else {
                            parent.stepForward();
                        }
                        parent.selectJobFromCanvas(i);
                        repaint();
                        return;

                    } else if (nextIncomingJobIdx >= 0 && i > nextIncomingJobIdx) {
                        // CASE D: User skipped ahead in queue -> Give friendly guidance
                        floatingTexts.add(new FloatingText(sx + 6, counterY + 25, "SERVE " + parent.currentJobList.get(nextIncomingJobIdx).name + " FIRST!", new Color(255, 120, 100)));
                        chefDialogue = "Customer for " + parent.currentJobList.get(nextIncomingJobIdx).name + " is next in line!";
                        for (int p = 0; p < 6; p++) {
                            particles.add(new KitchenParticle(sx + (slotWidth - 10) / 2, counterY + 45,
                                    (Math.random() - 0.5) * 2.0, -Math.random() * 2.0, new Color(110, 100, 120), 25, 4));
                        }
                        repaint();
                        return;
                    }
                }
            }
        }

        private void handleCanvasHover(int mx, int my) {
            int w = getWidth();
            int numJobs = Math.max(1, parent.currentJobList.size());
            int slotWidth = computeSlotWidth(w, numJobs);
            int counterStartX = computeCounterStartX(w, numJobs, slotWidth);
            int slotHeight = 88;
            int counterY = 38;

            Rectangle chefRect = new Rectangle((int)chefCurrentX, (int)chefCurrentY - walkBob, 44, 44);
            hoveredChef = chefRect.contains(mx, my);

            int prevHovered = hoveredJobIdx;
            hoveredJobIdx = -1;

            for (int i = 0; i < parent.currentJobList.size(); i++) {
                int sx = counterStartX + i * slotWidth;
                Rectangle cardRect = new Rectangle(sx, counterY, slotWidth - 10, slotHeight + 45);
                if (cardRect.contains(mx, my)) {
                    hoveredJobIdx = i;
                    break;
                }
            }

            if (hoveredJobIdx >= 0) {
                JobInput j = parent.currentJobList.get(hoveredJobIdx);
                setToolTipText(String.format("<html><b>%s: %s</b><br>%s<br>Burst Time: %.2fs | Arrival: %.2fs</html>",
                        j.name, DISH_NAMES[j.dishType % 6], DISH_COOK_PROFILES[j.dishType % 6], j.bt, j.at));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else if (hoveredChef) {
                setToolTipText("Head Chef (CPU Core) - Dispatches and shares time among active orders!");
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            } else {
                setToolTipText(null);
                setCursor(Cursor.getDefaultCursor());
            }

            if (prevHovered != hoveredJobIdx) {
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Checkerboard Kitchen Tile Floor
            int tileSize = 20;
            for (int y = 0; y < h; y += tileSize) {
                for (int x = 0; x < w; x += tileSize) {
                    boolean alt = ((x / tileSize) + (y / tileSize)) % 2 == 0;
                    g2.setColor(alt ? new Color(22, 16, 40) : new Color(17, 12, 32));
                    g2.fillRect(x, y, tileSize, tileSize);
                }
            }

            // 2. Kitchen Header Order Rail (Top Bar)
            g2.setColor(new Color(30, 20, 55));
            g2.fillRect(10, 6, w - 20, 28);
            g2.setColor(new Color(75, 55, 110));
            g2.drawRect(10, 6, w - 20, 28);

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("KITCHEN ORDER RAIL // DISHES SHARING CPU & MEMORY RESOURCES (N_j)", 20, 22);

            // Concurrency Status & Kitchen Fault Indicator
            EventStep step = (parent.currentStepIndex >= 0 && parent.currentStepIndex < parent.computedSteps.size())
                    ? parent.computedSteps.get(parent.currentStepIndex) : null;

            int nj = (step != null) ? step.nj : 0;
            double cpuShare = (step != null) ? step.cpuPct : 0.0;
            int ramPerJob = nj > 0 ? (512 / nj) : 0;

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            String statStr = String.format("CONCURRENCY: nj = %d | CPU = %.1f%% | RAM = %dMB/JOB", nj, cpuShare * 100.0, ramPerJob);
            if (kitchenFaults > 0) {
                statStr += String.format(" | FAULTS: %d/3", kitchenFaults);
                g2.setColor(new Color(255, 90, 110));
            } else {
                g2.setColor(RetroTheme.ACCENT_CYAN);
            }
            g2.drawString(statStr, w - 340, 22);

            // 3. Left Desk: Executive Chef Base / Order Bell
            int baseW = 66;
            int baseY = 40;
            int baseH = 94;
            g2.setColor(new Color(25, 18, 42));
            g2.fillRect(10, baseY, baseW, baseH);
            g2.setColor(new Color(65, 45, 95));
            g2.drawRect(10, baseY, baseW, baseH);

            // Desk Bell Icon (Ding!)
            g2.setColor(new Color(255, 215, 0));
            g2.fillArc(32, baseY + 18, 22, 16, 0, 180);
            g2.fillRect(30, baseY + 32, 26, 4);
            g2.fillRect(41, baseY + 14, 4, 4); // Bell knob

            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            g2.setColor(Color.WHITE);
            g2.drawString("CHEF BASE", 15, baseY + 54);
            g2.setColor(nj > 0 ? RetroTheme.ACCENT_CYAN : RetroTheme.ACCENT_YELLOW);
            g2.drawString(nj > 0 ? "DISPATCH" : "IDLE", 20, baseY + 68);

            g2.setFont(new Font("Consolas", Font.PLAIN, 8));
            g2.setColor(new Color(140, 130, 170));
            g2.drawString("[BELL]", 26, baseY + 84);

            // 4. Order Ticket Cards & Prep Counters
            int numJobs = Math.max(1, parent.currentJobList.size());
            int slotWidth = computeSlotWidth(w, numJobs);
            int counterStartX = computeCounterStartX(w, numJobs, slotWidth);
            int slotHeight = 88;
            int counterY = 38;

            // Identify the next incoming order in queue
            int nextIncomingJobIdx = -1;
            for (int k = 0; k < parent.currentJobList.size(); k++) {
                if (step != null && step.time < parent.currentJobList.get(k).at - 1e-6) {
                    nextIncomingJobIdx = k;
                    break;
                }
            }

            for (int i = 0; i < parent.currentJobList.size(); i++) {
                JobInput job = parent.currentJobList.get(i);
                int sx = counterStartX + i * slotWidth;

                // Job Status & Lifecycle Logic
                JobSummary summary = parent.getJobSummary(job.name);
                boolean isCompleted = (summary != null && step != null && step.time >= summary.ft - 1e-6);
                boolean isWaiting = (step != null && step.time < job.at - 1e-6);
                boolean isActive = (step != null && step.activeJobNames.contains(job.name));
                boolean isTerminatedNow = (step != null && step.terminatedThisStep.contains(job.name));
                boolean isHovered = (hoveredJobIdx == i);
                boolean isSelected = (selectedJobIdx == i);

                boolean isAllJobsDone = (parent.currentStepIndex >= 0 && parent.currentStepIndex == parent.computedSteps.size() - 1);
                boolean needsServeAction = (isTerminatedNow && parent.isChallengeMode && !isAllJobsDone);

                Double timeLeft = null;
                if (isCompleted) {
                    timeLeft = 0.0;
                } else if (step != null) {
                    timeLeft = step.timeLeftMap.get(job.name);
                }
                if (timeLeft == null) timeLeft = Double.valueOf(job.bt);

                // Card Background
                Color cardBg;
                if (isHovered) {
                    cardBg = new Color(50, 36, 80);
                } else if (isSelected) {
                    cardBg = new Color(40, 30, 70);
                } else if (isCompleted) {
                    cardBg = new Color(18, 42, 28);
                } else if (isActive) {
                    cardBg = new Color(34, 25, 60);
                } else {
                    cardBg = new Color(20, 15, 36);
                }
                g2.setColor(cardBg);
                g2.fillRect(sx, counterY, slotWidth - 10, slotHeight);

                // Card Border (Glows Gold on Hover or When Terminated / Ready to Serve!)
                if (isHovered) {
                    g2.setColor(new Color(255, 230, 0)); // Bright Neon Gold
                    g2.setStroke(new BasicStroke(2));
                } else if (needsServeAction) {
                    g2.setColor(new Color(255, 220, 0)); // Pulsing Bright Gold for ready to serve in challenge mode!
                    g2.setStroke(new BasicStroke(2));
                } else if (isCompleted) {
                    g2.setColor(new Color(0, 240, 100)); // Emerald Green for all completed dishes!
                    g2.setStroke(new BasicStroke(2));
                } else if (isSelected) {
                    g2.setColor(RetroTheme.ACCENT_CYAN);
                    g2.setStroke(new BasicStroke(2));
                } else if (isActive) {
                    g2.setColor(RetroTheme.ACCENT_CYAN);
                    g2.setStroke(new BasicStroke(2));
                } else {
                    g2.setColor(new Color(55, 40, 80));
                    g2.setStroke(new BasicStroke(1));
                }
                g2.drawRect(sx, counterY, slotWidth - 10, slotHeight);

                // Top-Right Status Badge for Finished Dishes
                if (isCompleted) {
                    int badgeW = Math.min(54, slotWidth - 14);
                    g2.setColor(new Color(0, 180, 80));
                    g2.fillRoundRect(sx + slotWidth - 10 - badgeW - 4, counterY + 4, badgeW, 13, 4, 4);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Consolas", Font.BOLD, (badgeW < 45) ? 7 : 8));
                    g2.drawString("FINISHED", sx + slotWidth - 10 - badgeW + 2, counterY + 13);
                }

                // Hanging Metallic Clip at Top
                g2.setColor(new Color(180, 185, 200));
                g2.fillRect(sx + (slotWidth - 10) / 2 - 8, counterY - 3, 16, 5);

                // Dish Pixel Sprite
                int iconSz = (slotWidth < 95) ? 22 : 28;
                BufferedImage dishSprite = PixelSprites.createDishSprite(job.dishType, iconSz);
                g2.drawImage(dishSprite, sx + (slotWidth - 10 - iconSz) / 2, counterY + 6, null);

                // Job Name & Dish Label
                g2.setFont(new Font("Consolas", Font.BOLD, (slotWidth < 95) ? 9 : 10));
                g2.setColor(Color.WHITE);
                String jobLabel = (slotWidth < 95) ? job.name : (job.name + " (" + DISH_NAMES[job.dishType % 6] + ")");
                g2.drawString(jobLabel, sx + 5, counterY + 42);

                // Burst / Timeleft
                g2.setFont(new Font("Consolas", Font.BOLD, (slotWidth < 95) ? 8 : 9));
                if (isCompleted) {
                    g2.setColor(new Color(0, 240, 100));
                    g2.drawString(String.format("DONE (%.2f)", summary != null ? summary.ft : 0.0), sx + 5, counterY + 54);
                } else if (isWaiting) {
                    g2.setColor(new Color(140, 130, 170));
                    g2.drawString(String.format("WAIT (AT:%.1f)", job.at), sx + 5, counterY + 54);
                } else {
                    g2.setColor(RetroTheme.ACCENT_YELLOW);
                    g2.drawString(String.format(slotWidth < 95 ? "%.2fs" : "LEFT: %.3fs", timeLeft), sx + 5, counterY + 54);
                }

                // Progress Bar
                double origBt = job.bt;
                double curTl = isCompleted ? 0.0 : timeLeft;
                double progress = isCompleted ? 1.0 : Math.max(0.0, Math.min(1.0, 1.0 - (curTl / origBt)));

                int barW = slotWidth - 20;
                int barH = 6;
                int barX = sx + 5;
                int barY = counterY + 60;

                g2.setColor(new Color(15, 10, 25));
                g2.fillRect(barX, barY, barW, barH);
                g2.setColor(isCompleted ? new Color(0, 240, 100) : (curTl <= 1e-6 ? new Color(0, 240, 100) : RetroTheme.ACCENT_CYAN));
                g2.fillRect(barX, barY, (int)(barW * progress), barH);
                g2.setColor(new Color(75, 55, 110));
                g2.drawRect(barX, barY, barW, barH);

                // Bottom Status Pill or Interactive Hover Cue
                if (isHovered) {
                    g2.setFont(new Font("Consolas", Font.BOLD, (slotWidth < 95) ? 8 : 9));
                    g2.setColor(new Color(255, 230, 0));
                    if (needsServeAction) {
                        g2.drawString(slotWidth < 95 ? "[ SERVE ]" : "[ CLICK TO SERVE ]", sx + 4, counterY + 78);
                    } else if (isCompleted) {
                        g2.drawString("SERVED", sx + 4, counterY + 78);
                    } else if (isActive) {
                        g2.drawString(slotWidth < 95 ? "[ FOCUS ]" : "[ FOCUS CHEF ]", sx + 4, counterY + 78);
                    } else if (i == nextIncomingJobIdx) {
                        g2.drawString(slotWidth < 95 ? "[ ADMIT ]" : "[ CLICK TO ADMIT ]", sx + 4, counterY + 78);
                    } else {
                        g2.drawString(String.format("AT:%.1f", job.at), sx + 4, counterY + 78);
                    }
                } else if (needsServeAction) {
                    g2.setFont(new Font("Consolas", Font.BOLD, (slotWidth < 95) ? 8 : 9));
                    g2.setColor(new Color(255, 220, 0));
                    g2.drawString("SERVE NOW!", sx + 4, counterY + 78);
                } else if (isCompleted) {
                    g2.setFont(new Font("Consolas", Font.BOLD, (slotWidth < 95) ? 8 : 9));
                    g2.setColor(new Color(0, 240, 100));
                    g2.drawString(slotWidth < 95 ? "SERVED" : "FINISHED", sx + 4, counterY + 78);
                } else if (isActive) {
                    g2.setFont(new Font("Consolas", Font.PLAIN, (slotWidth < 95) ? 8 : 9));
                    g2.setColor(RetroTheme.ACCENT_CYAN);
                    g2.drawString(String.format(slotWidth < 95 ? "-%.2f/t" : "-%.3f/tick", (step != null ? step.hwJob : 0.0)), sx + 4, counterY + 78);
                } else if (i == nextIncomingJobIdx) {
                    g2.setFont(new Font("Consolas", Font.BOLD, (slotWidth < 95) ? 8 : 9));
                    g2.setColor(new Color(255, 230, 0));
                    g2.drawString(slotWidth < 95 ? "NEXT" : ">> NEXT", sx + 4, counterY + 78);
                } else if (isWaiting) {
                    g2.setFont(new Font("Consolas", Font.PLAIN, (slotWidth < 95) ? 8 : 9));
                    g2.setColor(new Color(140, 130, 170));
                    g2.drawString(slotWidth < 95 ? "QUEUE" : "IN QUEUE", sx + 4, counterY + 78);
                }

                // 5. Retro Cooking Stove / Prep Station Below Each Ticket
                int stoveX = sx + (slotWidth - 10 - 26) / 2;
                int stoveY = counterY + slotHeight + 2;
                BufferedImage stove = PixelSprites.createStoveSprite(26, isActive);
                g2.drawImage(stove, stoveX, stoveY, null);

                // Small Stainless Countertop Plate
                g2.setColor(new Color(70, 65, 85));
                g2.fillRect(sx + 4, stoveY + 24, slotWidth - 18, 3);
            }

            // 6. Concurrency Sharing Beams (From Chef to all active stoves)
            if (step != null && !step.activeJobNames.isEmpty()) {
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, (animTick * 2) % 8));
                g2.setColor(new Color(0, 255, 255, 130));

                for (int i = 0; i < parent.currentJobList.size(); i++) {
                    JobInput job = parent.currentJobList.get(i);
                    if (step.activeJobNames.contains(job.name)) {
                        int targetX = counterStartX + i * slotWidth + (slotWidth - 10) / 2;
                        int targetY = counterY + slotHeight + 14;
                        g2.drawLine((int)chefCurrentX + 22, (int)chefCurrentY + 16 - walkBob, targetX, targetY);
                    }
                }
            }

            // 7. Render The Moving Chef (CPU Dispatcher)
            boolean isChopping = !isWalking && (nj > 0) && (animTick % 6 < 3);
            BufferedImage chefSprite = PixelSprites.createChefSprite(44, isChopping);
            int chefDrawX = (int)chefCurrentX;
            int chefDrawY = (int)chefCurrentY - walkBob;

            if (!facingRight) {
                // Flip horizontally when walking left
                g2.drawImage(chefSprite, chefDrawX + 44, chefDrawY, -44, 44, null);
            } else {
                g2.drawImage(chefSprite, chefDrawX, chefDrawY, 44, 44, null);
            }

            // 8. Chef Speech / Thought Bubble (Dynamic Dialogue)
            if (chefDialogue != null && !chefDialogue.isEmpty()) {
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                FontMetrics fm = g2.getFontMetrics();
                int bubbleW = fm.stringWidth(chefDialogue) + 12;
                int bubbleH = 16;
                int bubbleX = Math.max(10, Math.min(w - bubbleW - 10, chefDrawX + 22 - bubbleW / 2));
                int bubbleY = chefDrawY - 20;

                g2.setColor(new Color(18, 12, 32, 230));
                g2.fillRoundRect(bubbleX, bubbleY, bubbleW, bubbleH, 6, 6);
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(bubbleX, bubbleY, bubbleW, bubbleH, 6, 6);

                // Bubble Tail
                int tailX = chefDrawX + 22;
                Polygon tail = new Polygon();
                tail.addPoint(tailX - 3, bubbleY + bubbleH);
                tail.addPoint(tailX + 3, bubbleY + bubbleH);
                tail.addPoint(tailX, bubbleY + bubbleH + 4);
                g2.setColor(new Color(18, 12, 32));
                g2.fillPolygon(tail);
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawLine(tailX - 3, bubbleY + bubbleH, tailX, bubbleY + bubbleH + 4);
                g2.drawLine(tailX + 3, bubbleY + bubbleH, tailX, bubbleY + bubbleH + 4);

                g2.setColor(new Color(255, 235, 120));
                g2.drawString(chefDialogue, bubbleX + 6, bubbleY + 12);
            }

            // 9. Render Particles (Footstep Dust, Cooking Steam, Sparkles)
            for (KitchenParticle p : particles) {
                float alpha = Math.max(0f, Math.min(1f, (float)p.life / p.maxLife));
                Color c = new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int)(p.color.getAlpha() * alpha));
                g2.setColor(c);
                g2.fillOval((int)p.x, (int)p.y, p.size, p.size);
            }

            // 10. Render Floating Texts (+100 XP, +10 XP COOK!)
            for (FloatingText t : floatingTexts) {
                float alpha = Math.max(0f, Math.min(1f, (float)t.life / t.maxLife));
                Color c = new Color(t.color.getRed(), t.color.getGreen(), t.color.getBlue(), (int)(255 * alpha));
                g2.setColor(c);
                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.drawString(t.text, (int)t.x, (int)t.y);
            }

            int timeY = h - 24;

            // 11. Interactive Challenge Prompt Box or LEVEL COMPLETE VICTORY BANNER
            boolean isAllJobsDone = (parent.currentStepIndex >= 0 &&
                    parent.currentStepIndex == parent.computedSteps.size() - 1);

            if (isAllJobsDone) {
                int vicW = Math.min(520, w - 60);
                int vicH = 54;
                int vicX = (w - vicW) / 2;
                int vicY = Math.max(145, timeY - vicH - 8);

                // Pulsing glow background & border
                float vicPulse = (float)(0.75 + 0.25 * Math.sin(animTick * 0.15));
                g2.setColor(new Color(15, 10, 28, 248));
                g2.fillRoundRect(vicX, vicY, vicW, vicH, 10, 10);

                // Marching Dashed Neon Yellow Border (Pac-Man Style)
                g2.setColor(new Color(255, 220, 0, (int)(255 * vicPulse)));
                float vPhase = (animTick * 1.5f) % 8.0f;
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{6f, 4f}, vPhase));
                g2.drawRoundRect(vicX, vicY, vicW, vicH, 10, 10);
                g2.setStroke(new BasicStroke(1.0f));

                // Animated Victory Cherries on left and right (Exactly like Pac-Man screenshot!)
                BufferedImage vicCherry = PixelSprites.createPacmanCherry(26);
                if (vicCherry != null) {
                    float cherryBob = (float)(Math.sin(animTick * 0.2) * 3);
                    g2.drawImage(vicCherry, vicX + 16, (int)(vicY + 16 + cherryBob), null);
                    g2.drawImage(vicCherry, vicX + vicW - 42, (int)(vicY + 16 - cherryBob), null);
                }

                // Line 1: Bold Yellow Arcade Header
                g2.setFont(new Font("Consolas", Font.BOLD, 14));
                g2.setColor(new Color(255, 220, 0));
                String vTitle = "[*] CONGRATULATIONS! ALL ORDERS FINISHED [*]";
                FontMetrics vfm = g2.getFontMetrics();
                g2.drawString(vTitle, vicX + (vicW - vfm.stringWidth(vTitle)) / 2, vicY + 26);

                // Line 2: Neon Cyan Arcade Subtitle
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(RetroTheme.ACCENT_CYAN);
                String vSub = "MULTIPROGRAMMING EXECUTION COMPLETED // +100 SCORE BONUS";
                FontMetrics vsfm = g2.getFontMetrics();
                g2.drawString(vSub, vicX + (vicW - vsfm.stringWidth(vSub)) / 2, vicY + 45);

                // Celebratory particles around Chef
                if (animTick % 3 == 0) {
                    particles.add(new KitchenParticle(chefCurrentX + 22 + (Math.random() - 0.5) * 50, chefCurrentY + 5,
                            (Math.random() - 0.5) * 3.5, -Math.random() * 2.5,
                            (animTick % 2 == 0 ? new Color(0, 255, 140) : new Color(255, 220, 50)), 28, 4));
                }
            } else if (parent.isChallengeMode && parent.awaitingChallengeChoice && !parent.challengePromptText.isEmpty()) {
                int bannerW = Math.min(740, w - 24);
                int bannerH = 26;
                int bannerX = (w - bannerW) / 2;
                int bannerY = timeY - 30;

                // Pulsing cyber amber box
                float pulse = (float)(0.82 + 0.18 * Math.sin(animTick * 0.25));
                g2.setColor(new Color(45, 30, 8, (int)(235 * pulse)));
                g2.fillRoundRect(bannerX, bannerY, bannerW, bannerH, 8, 8);
                g2.setColor(new Color(255, (int)(220 * pulse), 0));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(bannerX, bannerY, bannerW, bannerH, 8, 8);

                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(new Color(255, 240, 80));
                String msg = parent.challengePromptText + " [CLICK ORDER TO ADMIT]";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, bannerX + (bannerW - fm.stringWidth(msg)) / 2, bannerY + 17);
            }

            // 12. Timeline Strip at Bottom of Canvas
            g2.setColor(new Color(25, 18, 45));
            g2.fillRect(10, timeY, w - 20, 20);
            g2.setColor(new Color(65, 45, 95));
            g2.drawRect(10, timeY, w - 20, 20);

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("SIMULATION TIMELINE:", 20, timeY + 14);

            if (step != null) {
                g2.setColor(Color.WHITE);
                g2.drawString(String.format("TIME: %.2f | INTERVAL: et = %.2f | WORK PER JOB: hw/job = %.3f",
                        step.time, step.et, step.hwJob), 160, timeY + 14);
            }

            g2.dispose();
        }
    }
}
