package arcadeos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * NAMCO // RAM DEFRAGGER: CYBER STACKER [16-BIT RETRO ARCADE]
 *
 * Dedicated retro arcade game for Mission 06: Memory Allocation & Fragmentation.
 * Gameplay:
 * - Player commands an overhead Gantry Cyber Crane carrying incoming Process Data Blocks.
 * - Move Left/Right ([A]/[D], Left/Right arrows, or mouse) over Physical RAM Silos.
 * - Press [SPACE] or [S] to Drop / Allocate the Process Block into the targeted Silo!
 *
 * OS Educational Rules & Scoring:
 * 1. FIT RULE: Process demands P_size. Silo holds S_capacity.
 * 2. BEST FIT BONUS (+150 PTS): Dropping into the silo with the MINIMUM leftover fragment
 *    triggers a PERFECT FIT bonus and increments the COMBO multiplier!
 * 3. INTERNAL FRAGMENTATION: Any remaining space inside the silo becomes internal fragmentation
 *    (rendered with amber hazard stripes).
 * 4. BUFFER OVERFLOW FAULT: If player drops into a Silo where P_size > S_capacity,
 *    an ILLEGAL MEMORY FAULT occurs (-1 Shield, screen shake, alarm siren!).
 * 5. COMPACTION LASER [C]: Sweeps all silos, coalesces fragmented holes, purges completed jobs,
 *    and clears external fragmentation (+250 XP)!
 * 6. BEST-FIT SCANNER [B]: Highlights the mathematically optimal silo.
 */
public class DefragPanicDialog extends JDialog {

    public enum GameMode {
        FIRST_FIT("FIRST FIT", "First vacant silo (left-to-right) that fits"),
        BEST_FIT("BEST FIT", "Vacant silo leaving minimum internal frag"),
        NEXT_FIT("NEXT FIT", "Next vacant silo scanning from pointer");

        public final String code;
        public final String rule;
        GameMode(String c, String r) { this.code = c; this.rule = r; }
    }

    private final FragmentationPanel parentPanel;
    private final List<Integer> sourceBlocks;
    private final List<Integer> sourceProcs;

    private GameMode currentMode = GameMode.FIRST_FIT;
    private JButton[] modeButtons;
    private javax.swing.Timer gameLoop;

    // Game Physics & State
    private int score = 0;
    private int combo = 1;
    private int lives = 3;
    private int compactionCharges = 2;
    private int lastAllocatedIndex = 0;

    private boolean isGameOver = false;
    private boolean isVictory = false;
    private int tickCount = 0;
    private double gameTime = 0.0;
    private int screenShake = 0;

    // Gantry Crane
    private float craneX = 520.0f;
    private float targetCraneX = 520.0f;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private final float CRANE_SPEED = 9.0f;
    private final float CRANE_Y = 125.0f;

    // Falling Block / Active Process
    public static class ProcessJob {
        public String pid;
        public int sizeKb;
        public Color themeColor;
        public int avatarIndex;
        public float currentHoleFrag = 0;

        public ProcessJob(String pid, int sizeKb, Color themeColor, int avatarIndex) {
            this.pid = pid;
            this.sizeKb = sizeKb;
            this.themeColor = themeColor;
            this.avatarIndex = avatarIndex;
        }
    }

    public static class FallingPayload {
        public ProcessJob job;
        public float x;
        public float y;
        public float vy = 0.0f;
        public int targetSiloIndex = -1;
        public boolean landed = false;

        public FallingPayload(ProcessJob job, float x, float y, int targetSiloIndex) {
            this.job = job;
            this.x = x;
            this.y = y;
            this.targetSiloIndex = targetSiloIndex;
        }
    }

    // Physical RAM Silo Bank
    public static class RamSilo {
        public int id;
        public int capacityKb;
        public int x;
        public int y;
        public int width;
        public int height;
        public ProcessJob allocatedJob = null;
        public int internalFragKb = 0;
        public float executionProgress = 0.0f; // 0.0 to 1.0
        public boolean isCompactedGlow = false;

        public RamSilo(int id, int capacityKb, int x, int y, int width, int height) {
            this.id = id;
            this.capacityKb = capacityKb;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public boolean isFree() {
            return allocatedJob == null;
        }
    }

    // Visual FX Particles & Stars
    public static class Star {
        public float x, y, speed;
        public int colIdx;
        public Star(float x, float y, float speed, int colIdx) {
            this.x = x; this.y = y; this.speed = speed; this.colIdx = colIdx;
        }
    }

    public static class DefragParticle {
        public float x, y, vx, vy;
        public Color color;
        public int life;
        public int maxLife;
        public DefragParticle(float x, float y, float vx, float vy, Color color, int life) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.color = color;
            this.life = life; this.maxLife = life;
        }
    }

    private final List<RamSilo> silos = new ArrayList<>();
    private final Queue<ProcessJob> incomingQueue = new LinkedList<>();
    private ProcessJob craneHeldJob = null;
    private FallingPayload fallingPayload = null;

    // Dedicated External Fragmentation / Unallocated Wait Bay
    private static final int WAIT_BAY_INDEX = -99;
    private int waitBayX = 0;
    private int waitBayY = 0;
    private int waitBayW = 0;
    private int waitBayH = 0;
    private final List<ProcessJob> waitingJobs = new ArrayList<>();

    private final List<Star> stars = new ArrayList<>();
    private final List<DefragParticle> particles = new ArrayList<>();
    private float laserCompactionSweepY = -100.0f;
    private boolean isLaserCompacting = false;

    // UI & HUD Elements
    private DefragArcadeCanvas canvas;
    private JLabel lblRadarBadge;
    private JLabel lblRadarMessage;
    private JLabel lblRuleFormula;
    private JLabel lblControlsGuide;

    private String feedbackBanner = "SYSTEM READY // USE [A]/[D] TO STEER CRANE, [SPACE] TO ALLOCATE!";
    private Color bannerColor = RetroTheme.ACCENT_CYAN;
    private int bannerTimer = 180;

    public DefragPanicDialog(JFrame parent, FragmentationPanel parentPanel,
                             List<Integer> blocks, List<Integer> procs) {
        super(parent, "NAMCO // RAM DEFRAGGER: CYBER CRANE ATTACK [16-BIT RETRO ARCADE]", true);
        this.parentPanel = parentPanel;
        this.sourceBlocks = (blocks != null && !blocks.isEmpty()) ? new ArrayList<>(blocks) : getDefaultBlocks();
        this.sourceProcs = (procs != null && !procs.isEmpty()) ? new ArrayList<>(procs) : getDefaultProcs();

        setSize(1040, 730);
        setResizable(false);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout());

        // Starfield
        Random r = new Random();
        for (int i = 0; i < 110; i++) {
            stars.add(new Star(r.nextInt(1020), r.nextInt(650), r.nextFloat() * 1.6f + 0.4f, r.nextInt(3)));
        }

        // Top Control Header
        add(createTopControlHeader(), BorderLayout.NORTH);

        // Center Arcade Canvas
        canvas = new DefragArcadeCanvas();
        add(canvas, BorderLayout.CENTER);

        // Bottom Controls Bar (2-Row Zero-Collision Guaranteed)
        add(createBottomControlsBar(), BorderLayout.SOUTH);

        // Keyboard Setup
        setupKeyboardControls();

        // Initialize Session
        resetGameSession();

        // 60 FPS Game Loop
        gameLoop = new javax.swing.Timer(16, e -> updateGamePhysics());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                gameLoop.start();
                canvas.requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (gameLoop != null) gameLoop.stop();
            }
        });
    }

    private List<Integer> getDefaultBlocks() {
        return Arrays.asList(100, 500, 200, 300, 600);
    }

    private List<Integer> getDefaultProcs() {
        return Arrays.asList(212, 417, 112, 426, 80, 250, 180);
    }

    // =========================================================================
    // 1. TOP HEADER & HUD CONTROLS
    // =========================================================================
    private JPanel createTopControlHeader() {
        JPanel bar = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(18, 12, 36));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(65, 45, 95));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        // Left Title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel titleLbl = new JLabel("🏗️ CYBER CRANE ATTACK // RAM ALLOCATION ARCADE");
        titleLbl.setFont(new Font("Consolas", Font.BOLD, 12));
        titleLbl.setForeground(RetroTheme.ACCENT_YELLOW);
        left.add(titleLbl);

        // Center Mode Tabs
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        tabs.setOpaque(false);
        modeButtons = new JButton[GameMode.values().length];
        for (int i = 0; i < GameMode.values().length; i++) {
            GameMode m = GameMode.values()[i];
            JButton btn = RetroTheme.createRetroButton(m.code, RetroTheme.BTN_DARK, RetroTheme.TEXT_WHITE);
            btn.setFont(new Font("Consolas", Font.BOLD, 10));
            btn.addActionListener(e -> {
                this.currentMode = m;
                updateModeButtonsUI();
                resetGameSession();
                canvas.requestFocusInWindow();
            });
            modeButtons[i] = btn;
            tabs.add(btn);
        }
        updateModeButtonsUI();

        // Right Actions
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton btnRestart = RetroTheme.createRetroButton("RESTART [R]", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnRestart.setFont(new Font("Consolas", Font.BOLD, 10));
        btnRestart.addActionListener(e -> {
            resetGameSession();
            canvas.requestFocusInWindow();
        });

        JButton btnExit = RetroTheme.createRetroButton("EXIT [ESC]", RetroTheme.BTN_DARK, RetroTheme.ACCENT_PINK);
        btnExit.setFont(new Font("Consolas", Font.BOLD, 10));
        btnExit.addActionListener(e -> dispose());

        right.add(btnRestart);
        right.add(btnExit);

        bar.add(left, BorderLayout.WEST);
        bar.add(tabs, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void updateModeButtonsUI() {
        if (modeButtons == null) return;
        for (int i = 0; i < GameMode.values().length; i++) {
            GameMode m = GameMode.values()[i];
            JButton btn = modeButtons[i];
            if (btn == null) continue;
            if (m == currentMode) {
                btn.setBackground(RetroTheme.ACCENT_CYAN);
                btn.setForeground(new Color(10, 14, 24));
            } else {
                btn.setBackground(RetroTheme.BTN_DARK);
                btn.setForeground(RetroTheme.TEXT_WHITE);
            }
        }
    }

    // =========================================================================
    // 2. 2-ROW BOTTOM CONTROLS & RADAR LOG (ZERO COLLISION GUARANTEED)
    // =========================================================================
    private JPanel createBottomControlsBar() {
        JPanel container = new JPanel(new GridLayout(2, 1, 0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(14, 9, 24));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(55, 38, 80));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        container.setOpaque(false);
        container.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

        // Row 1: Left = Radar Log Banner, Right = Dynamic Algorithm Law
        JPanel row1 = new JPanel(new BorderLayout(14, 0));
        row1.setOpaque(false);

        JPanel row1Left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1Left.setOpaque(false);

        lblRadarBadge = new JLabel("⚡ MMU RADAR:");
        lblRadarBadge.setFont(new Font("Consolas", Font.BOLD, 11));
        lblRadarBadge.setForeground(RetroTheme.ACCENT_CYAN);

        lblRadarMessage = new JLabel(feedbackBanner);
        lblRadarMessage.setFont(new Font("Consolas", Font.BOLD, 11));
        lblRadarMessage.setForeground(bannerColor);

        row1Left.add(lblRadarBadge);
        row1Left.add(lblRadarMessage);

        lblRuleFormula = new JLabel("RULE [" + currentMode.code + "]: " + currentMode.rule);
        lblRuleFormula.setFont(new Font("Consolas", Font.BOLD, 11));
        lblRuleFormula.setForeground(RetroTheme.ACCENT_GREEN);

        row1.add(row1Left, BorderLayout.WEST);
        row1.add(lblRuleFormula, BorderLayout.EAST);

        // Row 2: Controls Centered Across Entire Width (Zero Collision Guaranteed)
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row2.setOpaque(false);

        lblControlsGuide = new JLabel("CONTROLS: [A]/[D] Move Crane  •  [SPACE] Drop / Allocate  •  [W] Park in Wait Bay  •  [C] Compaction Laser  •  [R] Restart");
        lblControlsGuide.setFont(new Font("Consolas", Font.BOLD, 11));
        lblControlsGuide.setForeground(new Color(165, 205, 245));

        row2.add(lblControlsGuide);

        container.add(row1);
        container.add(row2);
        return container;
    }

    private void updateBottomBannerUI() {
        if (lblRadarMessage != null) {
            String msg = feedbackBanner;
            if (msg.length() > 62) {
                msg = msg.substring(0, 59) + "...";
            }
            lblRadarMessage.setText(msg);
            lblRadarMessage.setForeground(bannerColor);
        }
        if (lblRuleFormula != null && currentMode != null) {
            lblRuleFormula.setText("RULE [" + currentMode.code + "]: " + currentMode.rule);
        }
    }

    // =========================================================================
    // 3. KEYBOARD CONTROLS SETUP
    // =========================================================================
    private void setupKeyboardControls() {
        InputMap im = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = canvas.getActionMap();

        // Left Press
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "LEFT_P");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "LEFT_P");
        am.put("LEFT_P", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { moveLeft = true; }
        });

        // Left Release
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, true), "LEFT_R");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), "LEFT_R");
        am.put("LEFT_R", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { moveLeft = false; }
        });

        // Right Press
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "RIGHT_P");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "RIGHT_P");
        am.put("RIGHT_P", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { moveRight = true; }
        });

        // Right Release
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, true), "RIGHT_R");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), "RIGHT_R");
        am.put("RIGHT_R", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { moveRight = false; }
        });

        // Drop / Allocate
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "DROP");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "DROP");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "DROP");
        am.put("DROP", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dropCurrentPayload(); }
        });

        // Park into Wait Bay / Blocked Slot [W]
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "WAIT_BAY");
        am.put("WAIT_BAY", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (craneHeldJob != null && fallingPayload == null) {
                    targetCraneX = waitBayX + waitBayW / 2.0f;
                    craneX = targetCraneX;
                    dropCurrentPayload();
                }
            }
        });

        // Compaction Laser [C]
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "COMPACT");
        am.put("COMPACT", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { triggerCompactionLaser(); }
        });

        // Restart [R]
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "RESTART");
        am.put("RESTART", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { resetGameSession(); }
        });

        // Exit [ESC]
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "EXIT");
        am.put("EXIT", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });
    }

    // =========================================================================
    // 4. GAME SESSION INITIALIZATION & SCENARIOS
    // =========================================================================
    private void resetGameSession() {
        score = 0;
        combo = 1;
        lives = 3;
        compactionCharges = 2;
        lastAllocatedIndex = 0;
        isGameOver = false;
        isVictory = false;
        tickCount = 0;
        gameTime = 0.0;
        screenShake = 0;
        fallingPayload = null;
        particles.clear();
        silos.clear();
        waitingJobs.clear();
        incomingQueue.clear();

        // 1. Build Physical RAM Silos and Dedicated Unallocated / Wait Bay
        List<Integer> bSizes = new ArrayList<>();
        if (sourceBlocks != null && sourceBlocks.size() >= 4) {
            bSizes.addAll(sourceBlocks);
        } else if (currentMode == GameMode.NEXT_FIT) {
            bSizes.addAll(Arrays.asList(120, 400, 250, 320, 550));
        } else {
            bSizes.addAll(Arrays.asList(100, 500, 200, 300, 600));
        }

        int siloCount = bSizes.size();
        int siloGap = 12;
        int waitBayGap = 20;
        int waitBayWidth = 140;
        int maxTotalW = 960;
        int availableForSilos = maxTotalW - waitBayWidth - waitBayGap - (siloCount - 1) * siloGap;
        int siloW = Math.min(142, availableForSilos / siloCount);
        int totalWidth = siloCount * siloW + (siloCount - 1) * siloGap + waitBayGap + waitBayWidth;
        int startX = Math.max(20, (1040 - totalWidth) / 2);
        int siloY = 220;
        int siloH = 260;

        for (int i = 0; i < siloCount; i++) {
            silos.add(new RamSilo(i + 1, bSizes.get(i), startX + i * (siloW + siloGap), siloY, siloW, siloH));
        }

        // Dedicated Wait Bay geometry
        waitBayX = startX + siloCount * (siloW + siloGap) - siloGap + waitBayGap;
        waitBayY = siloY;
        waitBayW = waitBayWidth;
        waitBayH = siloH;

        // 2. Build Incoming Process Queue
        Color[] pColors = {
                RetroTheme.ACCENT_CYAN, RetroTheme.ACCENT_GREEN, RetroTheme.ACCENT_YELLOW,
                RetroTheme.ACCENT_PINK, new Color(180, 90, 255), new Color(255, 140, 50),
                new Color(90, 230, 255), new Color(255, 110, 180)
        };

        List<Integer> pSizes = new ArrayList<>();
        if (sourceProcs != null && sourceProcs.size() >= 4) {
            pSizes.addAll(sourceProcs);
        } else {
            pSizes.addAll(Arrays.asList(212, 417, 112, 426, 95, 180, 250, 140));
        }

        for (int i = 0; i < pSizes.size(); i++) {
            incomingQueue.add(new ProcessJob("P" + (i + 1), pSizes.get(i), pColors[i % pColors.length], i));
        }

        craneHeldJob = incomingQueue.poll();
        craneX = silos.get(silos.size() / 2).x + siloW / 2.0f;
        targetCraneX = craneX;

        feedbackBanner = "CRANE ENGAGED // Move crane over Silos or Wait Bay & press [SPACE] to drop.";
        bannerColor = RetroTheme.ACCENT_CYAN;
        bannerTimer = 160;
        updateBottomBannerUI();
        updateModeButtonsUI();
    }

    // =========================================================================
    // 5. ACTION TRIGGERS (DROP PAYLOAD, COMPACTION, SCANNER)
    // =========================================================================
    private void dropCurrentPayload() {
        if (isGameOver || isVictory) return;
        if (fallingPayload != null || craneHeldJob == null) return;

        // Check if crane is aligned over Wait Bay or a Silo
        int targetIdx = getAlignedSiloIndex(craneX);
        if (targetIdx == WAIT_BAY_INDEX) {
            fallingPayload = new FallingPayload(craneHeldJob, craneX, CRANE_Y + 18, WAIT_BAY_INDEX);
            craneHeldJob = null;
            return;
        }
        if (targetIdx < 0 || targetIdx >= silos.size()) return;

        fallingPayload = new FallingPayload(craneHeldJob, craneX, CRANE_Y + 18, targetIdx);
        craneHeldJob = null;
    }

    private int getAlignedSiloIndex(float x) {
        int bestIdx = -1;
        float minD = Float.MAX_VALUE;
        for (int i = 0; i < silos.size(); i++) {
            RamSilo s = silos.get(i);
            float siloMid = s.x + s.width / 2.0f;
            float d = Math.abs(x - siloMid);
            if (d < minD) {
                minD = d;
                bestIdx = i;
            }
        }
        // Check distance to Wait Bay
        float waitMid = waitBayX + waitBayW / 2.0f;
        float waitD = Math.abs(x - waitMid);
        if (waitD < minD) {
            return WAIT_BAY_INDEX;
        }
        return bestIdx;
    }

    private int findTargetSiloIndex(ProcessJob job) {
        if (job == null || silos.isEmpty()) return -1;
        switch (currentMode) {
            case FIRST_FIT:
                for (int i = 0; i < silos.size(); i++) {
                    RamSilo s = silos.get(i);
                    if (s.isFree() && s.capacityKb >= job.sizeKb) {
                        return i;
                    }
                }
                return -1;

            case BEST_FIT:
                int bestIdx = -1;
                int minHole = Integer.MAX_VALUE;
                for (int i = 0; i < silos.size(); i++) {
                    RamSilo s = silos.get(i);
                    if (s.isFree() && s.capacityKb >= job.sizeKb) {
                        int leftover = s.capacityKb - job.sizeKb;
                        if (leftover < minHole) {
                            minHole = leftover;
                            bestIdx = i;
                        }
                    }
                }
                return bestIdx;

            case NEXT_FIT:
                int n = silos.size();
                for (int step = 0; step < n; step++) {
                    int idx = (lastAllocatedIndex + step) % n;
                    RamSilo s = silos.get(idx);
                    if (s.isFree() && s.capacityKb >= job.sizeKb) {
                        return idx;
                    }
                }
                return -1;
        }
        return -1;
    }

    private void triggerCompactionLaser() {
        if (isGameOver || isVictory) return;
        if (compactionCharges <= 0) {
            feedbackBanner = "NO COMPACTION CHARGES REMAINING!";
            bannerColor = RetroTheme.ACCENT_PINK;
            bannerTimer = 50;
            updateBottomBannerUI();
            return;
        }

        compactionCharges--;
        isLaserCompacting = true;
        laserCompactionSweepY = 200.0f;
        screenShake = 10;

        int freedSilos = 0;
        int reclaimedFrag = 0;
        for (RamSilo s : silos) {
            if (!s.isFree()) {
                reclaimedFrag += s.internalFragKb;
                s.allocatedJob = null;
                s.internalFragKb = 0;
                s.executionProgress = 0.0f;
                s.isCompactedGlow = true;
                freedSilos++;
            }
        }

        int bonus = freedSilos * 100 + 250;
        score += bonus;
        AppState.addScore(bonus);

        if (craneHeldJob == null && !waitingJobs.isEmpty()) {
            craneHeldJob = waitingJobs.remove(0);
            feedbackBanner = String.format("⚡ COMPACTION DEFRAG COMPLETE! Purged %d silos & %s reloaded to crane! (+%d XP)",
                    freedSilos, craneHeldJob.pid, bonus);
        } else {
            feedbackBanner = String.format("⚡ COMPACTION DEFRAG COMPLETE! Purged %d silos & reclaimed %d KB frag! (+%d XP)",
                    freedSilos, reclaimedFrag, bonus);
        }
        bannerColor = RetroTheme.ACCENT_GREEN;
        bannerTimer = 110;
        updateBottomBannerUI();

        for (int i = 0; i < 45; i++) {
            particles.add(new DefragParticle(
                    (float)(Math.random() * 900 + 60), 340.0f,
                    (float)(Math.random() * 8 - 4), (float)(Math.random() * 8 - 4),
                    RetroTheme.ACCENT_CYAN, 30
            ));
        }
    }

    // =========================================================================
    // 6. 60 FPS PHYSICS, MOVEMENT & COLLISION ENGINE
    // =========================================================================
    private void updateGamePhysics() {
        if (isGameOver || isVictory) {
            canvas.repaint();
            return;
        }

        tickCount++;
        gameTime += 0.016;

        if (screenShake > 0) screenShake--;
        if (bannerTimer > 0) {
            bannerTimer--;
            if (bannerTimer == 0) {
                feedbackBanner = "CRANE READY // Align over target silo and press [SPACE] to allocate.";
                bannerColor = RetroTheme.ACCENT_CYAN;
                updateBottomBannerUI();
            }
        }

        // Laser Compaction sweep animation
        if (isLaserCompacting) {
            laserCompactionSweepY += 12.0f;
            if (laserCompactionSweepY > 520.0f) {
                isLaserCompacting = false;
            }
        }

        // Crane Smooth Movement
        if (moveLeft) targetCraneX -= CRANE_SPEED;
        if (moveRight) targetCraneX += CRANE_SPEED;

        float minX = silos.get(0).x + silos.get(0).width / 2.0f;
        float maxX = waitBayX + waitBayW / 2.0f;
        targetCraneX = Math.max(minX, Math.min(maxX, targetCraneX));
        craneX += (targetCraneX - craneX) * 0.35f;

        // Falling Payload Physics
        if (fallingPayload != null) {
            fallingPayload.vy += 0.65f;
            fallingPayload.y += fallingPayload.vy;

            if (fallingPayload.targetSiloIndex == WAIT_BAY_INDEX) {
                float landingY = waitBayY + waitBayH - 42;
                if (fallingPayload.y >= landingY) {
                    fallingPayload.y = landingY;
                    handleWaitBayLanding(fallingPayload);
                    fallingPayload = null;
                }
            } else if (fallingPayload.targetSiloIndex >= 0 && fallingPayload.targetSiloIndex < silos.size()) {
                RamSilo targetSilo = silos.get(fallingPayload.targetSiloIndex);
                float landingY = targetSilo.y + targetSilo.height - 42;

                if (fallingPayload.y >= landingY) {
                    fallingPayload.y = landingY;
                    handlePayloadLanding(fallingPayload, targetSilo);
                    fallingPayload = null;
                }
            } else {
                fallingPayload = null;
            }
        }

        // Particle physics
        for (int i = particles.size() - 1; i >= 0; i--) {
            DefragParticle p = particles.get(i);
            p.x += p.vx;
            p.y += p.vy;
            p.life--;
            if (p.life <= 0) particles.remove(i);
        }

        canvas.repaint();
    }

    private void handlePayloadLanding(FallingPayload payload, RamSilo silo) {
        ProcessJob job = payload.job;

        // Check if fits in chosen silo
        if (silo.isFree() && silo.capacityKb >= job.sizeKb) {
            int targetIdx = findTargetSiloIndex(job);
            boolean isCorrectAlgo = (silo.id - 1 == targetIdx);

            if (isCorrectAlgo) {
                // Correct algorithm match!
                silo.allocatedJob = job;
                silo.internalFragKb = silo.capacityKb - job.sizeKb;
                silo.executionProgress = 0.0f;
                lastAllocatedIndex = silo.id - 1;

                combo++;
                int pts = 200 * combo;
                score += pts;
                AppState.addScore(pts);
                triggerExplosion(silo.x + silo.width / 2.0f, silo.y + silo.height - 24, RetroTheme.ACCENT_YELLOW, 25);
                feedbackBanner = String.format("🎯 PERFECT %s! (%s -> Silo %d) Heat Frag: %d KB! (+%d x%d)",
                        currentMode.code, job.pid, silo.id, silo.internalFragKb, pts, combo);
                bannerColor = RetroTheme.ACCENT_GREEN;
                bannerTimer = 110;
                updateBottomBannerUI();

                // Advance to next job in queue
                advanceProcessQueue();
            } else {
                // Wrong box according to active mode! It should be WRONG!
                combo = 1;
                lives--;
                screenShake = 12;
                triggerExplosion(silo.x + silo.width / 2.0f, silo.y + 30, RetroTheme.ACCENT_PINK, 20);

                String requiredSilo = (targetIdx >= 0) ? ("Silo " + (targetIdx + 1)) : "Wait Bay";
                String reason = "";
                if (targetIdx == -1) {
                    reason = "No free silo fits! Should park in Wait Bay [W]!";
                } else if (currentMode == GameMode.FIRST_FIT) {
                    reason = String.format("First free silo from left was %s!", requiredSilo);
                } else if (currentMode == GameMode.BEST_FIT) {
                    int optimalFrag = (targetIdx >= 0) ? (silos.get(targetIdx).capacityKb - job.sizeKb) : 0;
                    reason = String.format("%s left minimal internal frag (%d KB)!", requiredSilo, optimalFrag);
                } else if (currentMode == GameMode.NEXT_FIT) {
                    reason = String.format("Next fit scan from pointer required %s!", requiredSilo);
                }

                feedbackBanner = String.format("❌ WRONG BOX! [%s] %s (-1 Shield)",
                        currentMode.code, reason);
                bannerColor = RetroTheme.ACCENT_PINK;
                bannerTimer = 130;
                updateBottomBannerUI();

                if (lives <= 0) {
                    isGameOver = true;
                    feedbackBanner = "💀 SYSTEM CRASH: RAM SHIELDS DEPLETED! PRESS [R] TO REBOOT";
                    bannerColor = RetroTheme.ACCENT_PINK;
                    updateBottomBannerUI();
                } else {
                    // Recover held job on the crane so player can try again
                    craneHeldJob = job;
                }
            }
        } else {
            // BUFFER OVERFLOW / ILLEGAL FIT!
            combo = 1;
            lives--;
            screenShake = 14;
            triggerExplosion(silo.x + silo.width / 2.0f, silo.y + 20, RetroTheme.ACCENT_PINK, 30);

            if (silo.capacityKb < job.sizeKb) {
                feedbackBanner = String.format("💥 BUFFER OVERFLOW! %s (%d KB) > Silo %d Capacity (%d KB)! (-1 Shield)",
                        job.pid, job.sizeKb, silo.id, silo.capacityKb);
            } else {
                feedbackBanner = String.format("💥 COLLISION FAULT! Silo %d is already occupied by %s! (-1 Shield)",
                        silo.id, silo.allocatedJob.pid);
            }
            bannerColor = RetroTheme.ACCENT_PINK;
            bannerTimer = 110;
            updateBottomBannerUI();

            if (lives <= 0) {
                isGameOver = true;
                feedbackBanner = "💀 SYSTEM CRASH: RAM SHIELDS DEPLETED! PRESS [R] TO REBOOT";
                bannerColor = RetroTheme.ACCENT_PINK;
                updateBottomBannerUI();
            } else {
                // Recover held job so player can retry
                craneHeldJob = job;
            }
        }
    }

    private void handleWaitBayLanding(FallingPayload payload) {
        ProcessJob job = payload.job;
        int targetIdx = findTargetSiloIndex(job);

        if (targetIdx == -1) {
            // Correct external fragmentation identification!
            waitingJobs.add(job);
            combo++;
            int pts = 250 * combo;
            score += pts;
            AppState.addScore(pts);
            triggerExplosion(waitBayX + waitBayW / 2.0f, waitBayY + waitBayH - 24, RetroTheme.ACCENT_YELLOW, 25);
            feedbackBanner = String.format("🎯 EXTERNAL FRAG IDENTIFIED! (%s -> WAIT BAY) No free partition fits %d KB! (+%d x%d)",
                    job.pid, job.sizeKb, pts, combo);
            bannerColor = RetroTheme.ACCENT_GREEN;
            bannerTimer = 130;
            updateBottomBannerUI();

            advanceProcessQueue();
        } else {
            // Illegal placement into Wait Bay! A free silo CAN fit this process!
            combo = 1;
            lives--;
            screenShake = 12;
            triggerExplosion(waitBayX + waitBayW / 2.0f, waitBayY + 30, RetroTheme.ACCENT_PINK, 20);

            feedbackBanner = String.format("❌ ILLEGAL WAIT! Free Silo %d can still fit %s (%d KB)! (-1 Shield)",
                    (targetIdx + 1), job.pid, job.sizeKb);
            bannerColor = RetroTheme.ACCENT_PINK;
            bannerTimer = 130;
            updateBottomBannerUI();

            if (lives <= 0) {
                isGameOver = true;
                feedbackBanner = "💀 SYSTEM CRASH: RAM SHIELDS DEPLETED! PRESS [R] TO REBOOT";
                bannerColor = RetroTheme.ACCENT_PINK;
                updateBottomBannerUI();
            } else {
                craneHeldJob = job; // Recover held job so player can allocate correctly
            }
        }
    }

    private void advanceProcessQueue() {
        if (!incomingQueue.isEmpty()) {
            craneHeldJob = incomingQueue.poll();
        } else {
            // Check Victory or spawn new wave
            isVictory = true;
            score += 350;
            AppState.addScore(350);
            if (!waitingJobs.isEmpty()) {
                feedbackBanner = String.format("🏆 ROUND COMPLETE! All active jobs allocated, %d queued in Wait Bay (+350 XP)", waitingJobs.size());
            } else {
                feedbackBanner = "🏆 ALL PROCESSES SAFELY ALLOCATED! RAM INTEGRITY 100% (+350 XP)";
            }
            bannerColor = RetroTheme.ACCENT_GREEN;
            updateBottomBannerUI();
        }
    }

    private void triggerExplosion(float x, float y, Color c, int count) {
        for (int i = 0; i < count; i++) {
            float ang = (float)(Math.random() * Math.PI * 2);
            float spd = (float)(Math.random() * 6 + 1);
            particles.add(new DefragParticle(
                    x, y,
                    (float)Math.cos(ang) * spd, (float)Math.sin(ang) * spd,
                    c, (int)(Math.random() * 25 + 15)
            ));
        }
    }

    // =========================================================================
    // 7. MAIN ARCADE SPACE CANVAS
    // =========================================================================
    private class DefragArcadeCanvas extends JPanel {
        public DefragArcadeCanvas() {
            setBackground(new Color(8, 6, 18));
            setOpaque(true);
            setFocusable(true);

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    targetCraneX = e.getX();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    dropCurrentPayload();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (screenShake > 0) {
                int ox = (int)((Math.random() * 2 - 1) * screenShake);
                int oy = (int)((Math.random() * 2 - 1) * screenShake);
                g2.translate(ox, oy);
            }

            // 1. Starfield Background
            drawStarfield(g2);

            // 2. Cyber RAM Ceiling Track
            drawCeilingTrack(g2, w);

            // 3. Top HUD Status & Metrics
            drawTopHUD(g2, w);

            // 4. Physical RAM Silos (Chambers)
            drawRamSilos(g2);

            // 4b. Dedicated Unallocated / Wait Bay Slot (External Fragmentation Buffer)
            drawWaitBay(g2);

            // 5. Laser Compaction Beam (if active)
            if (isLaserCompacting) {
                drawLaserCompaction(g2, w);
            }

            // 6. Gantry Crane & Held Payload
            drawGantryCrane(g2);

            // 7. Falling Payload
            if (fallingPayload != null) {
                drawFallingPayload(g2);
            }

            // 8. Particles
            drawParticles(g2);

            // 9. Overlay Cards (Victory / Game Over)
            if (isVictory) {
                drawOverlayCard(g2, w, h, "100% ALLOCATION COMPLETED!", "ALL PROCESSES PACKED WITH OPTIMAL MEMORY EFFICIENCY (+350 XP)", RetroTheme.ACCENT_GREEN);
            } else if (isGameOver) {
                drawOverlayCard(g2, w, h, "SYSTEM OUT OF MEMORY!", "RAM FAULT LIMIT REACHED. PRESS [R] TO REBOOT", RetroTheme.ACCENT_PINK);
            }

            g2.dispose();
        }

        private void drawStarfield(Graphics2D g2) {
            Color[] starCols = {Color.WHITE, RetroTheme.ACCENT_CYAN, RetroTheme.ACCENT_YELLOW};
            for (Star s : stars) {
                g2.setColor(starCols[s.colIdx]);
                int sz = (int) Math.max(1, s.speed);
                g2.fillRect((int) s.x, (int) s.y, sz, sz);
            }
        }

        private void drawCeilingTrack(Graphics2D g2, int w) {
            g2.setColor(new Color(25, 18, 42));
            g2.fillRect(16, 95, w - 32, 16);
            g2.setColor(new Color(65, 45, 95));
            g2.drawRect(16, 95, w - 32, 16);

            // Track Teeth / Gears
            g2.setColor(new Color(85, 60, 120));
            for (int gx = 24; gx < w - 32; gx += 16) {
                g2.fillRect(gx, 97, 8, 12);
            }
        }

        private void drawTopHUD(Graphics2D g2, int w) {
            // Container Panel
            g2.setColor(new Color(18, 12, 36, 235));
            g2.fillRoundRect(16, 8, w - 32, 78, 8, 8);
            g2.setColor(new Color(65, 45, 95));
            g2.drawRoundRect(16, 8, w - 32, 78, 8, 8);

            // Title & Mode Pill
            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("THE PHYSICAL RAM ALLOCATION MATRIX", 28, 25);

            // Active Mode Pill
            String modeStr = "MODE: " + currentMode.code;
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            int pillW = g2.getFontMetrics().stringWidth(modeStr) + 16;
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.fillRoundRect(295, 12, pillW, 18, 4, 4);
            g2.setColor(new Color(10, 14, 24));
            g2.drawString(modeStr, 303, 25);

            // Shields / Lives
            g2.setColor(RetroTheme.TEXT_WHITE);
            g2.drawString("SHIELDS: ", w - 340, 25);
            for (int i = 0; i < 3; i++) {
                g2.setColor(i < lives ? RetroTheme.ACCENT_CYAN : new Color(60, 40, 70));
                g2.drawString("▲", w - 280 + i * 16, 25);
            }

            // Score & Combo
            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.drawString(String.format("SCORE: %05d (x%d)", score, combo), w - 195, 25);

            // Metrics Strip in HUD
            int totalFrag = 0;
            int totalAlloc = 0;
            for (RamSilo s : silos) {
                if (!s.isFree()) {
                    totalAlloc++;
                    totalFrag += s.internalFragKb;
                }
            }

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString(String.format("ALLOCATED SILOS: %d / %d", totalAlloc, silos.size()), 28, 48);

            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString(String.format("TOTAL INTERNAL FRAG: %d KB", totalFrag), 220, 48);

            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString(String.format("COMPACTION [C]: %d CHARGES", compactionCharges), w - 340, 48);

            // Active Pointer in Next-Fit or Policy Target Status (No hints!)
            g2.setColor(new Color(170, 160, 210));
            if (currentMode == GameMode.NEXT_FIT) {
                g2.drawString("POINTER: Silo " + (lastAllocatedIndex + 1), w - 165, 48);
            } else {
                g2.drawString("STATUS: ACTIVE", w - 165, 48);
            }

            // Next In Queue Preview + Waiting Bay Counter
            g2.setColor(RetroTheme.TEXT_DIM);
            g2.drawString("NEXT IN QUEUE: " + (incomingQueue.isEmpty() ? "< QUEUE CLEAR >" :
                    incomingQueue.peek().pid + " [" + incomingQueue.peek().sizeKb + " KB]"), 28, 70);

            if (!waitingJobs.isEmpty()) {
                g2.setColor(new Color(255, 170, 0));
                g2.drawString(String.format("⚡ BLOCKED IN WAIT BAY: %d JOB(S)", waitingJobs.size()), 260, 70);
            }
        }

        private void drawRamSilos(Graphics2D g2) {
            for (int i = 0; i < silos.size(); i++) {
                RamSilo s = silos.get(i);

                // Silo Chamber Outer Shell
                g2.setColor(new Color(16, 12, 30));
                g2.fillRoundRect(s.x, s.y, s.width, s.height, 8, 8);

                // Clean Cyber Border (Zero Hints)
                Color borderC = s.isCompactedGlow ? RetroTheme.ACCENT_CYAN : new Color(55, 40, 80);
                g2.setColor(borderC);
                g2.drawRoundRect(s.x, s.y, s.width, s.height, 8, 8);
                g2.setStroke(new BasicStroke(1.0f));

                // Silo Header
                g2.setColor(new Color(24, 18, 42));
                g2.fillRoundRect(s.x + 2, s.y + 2, s.width - 4, 26, 6, 6);
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.drawString("SILO " + s.id, s.x + 8, s.y + 19);

                // Capacity Tag
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(RetroTheme.TEXT_WHITE);
                String capStr = s.capacityKb + "K";
                int capW = g2.getFontMetrics().stringWidth(capStr);
                g2.drawString(capStr, s.x + s.width - capW - 8, s.y + 19);

                // Contents inside Silo
                int chamberY = s.y + 30;
                int chamberH = s.height - 34;

                if (s.isFree()) {
                    // Empty Silo Cyber Grid (No hints!)
                    g2.setColor(new Color(22, 16, 36));
                    g2.fillRect(s.x + 4, chamberY, s.width - 8, chamberH);

                    g2.setColor(new Color(36, 26, 56));
                    for (int gy = chamberY; gy < chamberY + chamberH; gy += 20) {
                        g2.drawLine(s.x + 4, gy, s.x + s.width - 4, gy);
                    }

                    g2.setFont(new Font("Consolas", Font.BOLD, 11));
                    g2.setColor(new Color(110, 90, 140));
                    g2.drawString("FREE", s.x + (s.width - 28) / 2, chamberY + chamberH / 2);
                } else {
                    // Allocated Job Block + Internal Frag Section
                    ProcessJob job = s.allocatedJob;
                    float allocFraction = (float) job.sizeKb / s.capacityKb;
                    int jobH = (int)(chamberH * allocFraction);
                    int fragH = chamberH - jobH;

                    int jobY = chamberY + fragH; // Job sits at the bottom

                    // 1. Process Block at bottom
                    g2.setColor(job.themeColor.darker().darker());
                    g2.fillRect(s.x + 4, jobY, s.width - 8, jobH);
                    g2.setColor(job.themeColor);
                    g2.drawRect(s.x + 4, jobY, s.width - 8, jobH);

                    // Process Icon & PID
                    BufferedImage avatar = PixelSprites.getJobAvatar(job.avatarIndex, 24);
                    if (avatar != null && jobH > 28) {
                        g2.drawImage(avatar, s.x + 8, jobY + 4, null);
                    }

                    g2.setFont(new Font("Consolas", Font.BOLD, 11));
                    g2.setColor(Color.WHITE);
                    g2.drawString(job.pid, s.x + 36, jobY + 16);

                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    g2.setColor(RetroTheme.ACCENT_CYAN);
                    g2.drawString(job.sizeKb + " KB", s.x + 36, jobY + 28);

                    // 2. Internal Frag Striped Pattern (Waste) at top of chamber (CLIPPED TO PREVENT BLEEDING)
                    if (fragH > 8) {
                        g2.setColor(new Color(45, 20, 20));
                        g2.fillRect(s.x + 4, chamberY, s.width - 8, fragH);

                        Shape oldClip = g2.getClip();
                        g2.clipRect(s.x + 4, chamberY, s.width - 8, fragH);
                        g2.setColor(new Color(255, 42, 141, 100));
                        for (int sx = s.x - fragH - 12; sx < s.x + s.width + 12; sx += 12) {
                            g2.drawLine(sx, chamberY + fragH, sx + 12, chamberY);
                        }
                        g2.setClip(oldClip);

                        g2.setFont(new Font("Consolas", Font.BOLD, 9));
                        g2.setColor(RetroTheme.ACCENT_PINK);
                        g2.drawString(String.format("WASTE: %dK", s.internalFragKb), s.x + 8, chamberY + 14);
                    }
                }
            }
        }

        private void drawWaitBay(Graphics2D g2) {
            if (waitBayW <= 0) return;

            // Outer Shell
            g2.setColor(new Color(22, 16, 12));
            g2.fillRoundRect(waitBayX, waitBayY, waitBayW, waitBayH, 8, 8);

            // Border - Amber Cyber
            g2.setColor(new Color(255, 170, 0));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(waitBayX, waitBayY, waitBayW, waitBayH, 8, 8);
            g2.setStroke(new BasicStroke(1.0f));

            // Header Tag
            g2.setColor(new Color(48, 28, 10));
            g2.fillRoundRect(waitBayX + 2, waitBayY + 2, waitBayW - 4, 26, 6, 6);
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(new Color(255, 200, 60));
            g2.drawString("WAIT BAY", waitBayX + 8, waitBayY + 19);

            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            g2.setColor(RetroTheme.ACCENT_PINK);
            String tag = "EXT FRAG";
            int tagW = g2.getFontMetrics().stringWidth(tag);
            g2.drawString(tag, waitBayX + waitBayW - tagW - 8, waitBayY + 19);

            int chamberY = waitBayY + 30;
            int chamberH = waitBayH - 34;

            if (waitingJobs.isEmpty()) {
                // Empty Wait Bay - Subtle diagonal striped warning
                g2.setColor(new Color(28, 20, 14));
                g2.fillRect(waitBayX + 4, chamberY, waitBayW - 8, chamberH);

                Shape oldClip = g2.getClip();
                g2.clipRect(waitBayX + 4, chamberY, waitBayW - 8, chamberH);
                g2.setColor(new Color(70, 48, 20, 100));
                for (int wx = waitBayX - chamberH - 12; wx < waitBayX + waitBayW + 12; wx += 16) {
                    g2.drawLine(wx, chamberY + chamberH, wx + 16, chamberY);
                }
                g2.setClip(oldClip);

                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(new Color(220, 170, 80));
                g2.drawString("BLOCKED QUEUE", waitBayX + (waitBayW - 76) / 2, chamberY + chamberH / 2 - 8);
                g2.setFont(new Font("Consolas", Font.PLAIN, 9));
                g2.setColor(new Color(160, 120, 60));
                g2.drawString("[PARK P_FAIL]", waitBayX + (waitBayW - 74) / 2, chamberY + chamberH / 2 + 8);
                g2.drawString("PRESS [W]", waitBayX + (waitBayW - 48) / 2, chamberY + chamberH / 2 + 22);
            } else {
                // Stack of waiting jobs
                int cardH = Math.min(50, (chamberH - 8) / Math.max(1, waitingJobs.size()));
                for (int i = 0; i < waitingJobs.size(); i++) {
                    ProcessJob wJob = waitingJobs.get(i);
                    int itemY = chamberY + 6 + i * (cardH + 4);

                    g2.setColor(new Color(42, 24, 16));
                    g2.fillRoundRect(waitBayX + 6, itemY, waitBayW - 12, cardH, 4, 4);
                    g2.setColor(new Color(255, 170, 0));
                    g2.drawRoundRect(waitBayX + 6, itemY, waitBayW - 12, cardH, 4, 4);

                    BufferedImage avatar = PixelSprites.getJobAvatar(wJob.avatarIndex, 20);
                    if (avatar != null && cardH > 24) {
                        g2.drawImage(avatar, waitBayX + 10, itemY + (cardH - 20) / 2, null);
                    }

                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    g2.setColor(Color.WHITE);
                    g2.drawString(wJob.pid + " [" + wJob.sizeKb + "K]", waitBayX + 34, itemY + 16);

                    g2.setFont(new Font("Consolas", Font.BOLD, 8));
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    g2.drawString("WAITING (EXT FRAG)", waitBayX + 34, itemY + 28);
                }
            }
        }

        private void drawLaserCompaction(Graphics2D g2, int w) {
            float y = laserCompactionSweepY;
            g2.setColor(new Color(5, 217, 232, 220));
            g2.setStroke(new BasicStroke(4.0f));
            g2.drawLine(10, (int) y, w - 10, (int) y);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawLine(10, (int) y, w - 10, (int) y);
            g2.setStroke(new BasicStroke(1.0f));
        }

        private void drawGantryCrane(Graphics2D g2) {
            int cx = (int) craneX;
            int cy = (int) CRANE_Y;

            // Target Guide Beam to Silo or Wait Bay
            int alignedSilo = getAlignedSiloIndex(craneX);
            if (alignedSilo == WAIT_BAY_INDEX) {
                g2.setColor(new Color(255, 170, 0, 35));
                g2.fillRect(waitBayX, waitBayY, waitBayW, waitBayH);

                g2.setColor(new Color(255, 170, 0, 90));
                g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4, 4}, 0.0f));
                g2.drawLine(cx, cy + 25, waitBayX + waitBayW / 2, waitBayY);
                g2.setStroke(new BasicStroke(1.0f));
            } else if (alignedSilo >= 0 && alignedSilo < silos.size()) {
                RamSilo s = silos.get(alignedSilo);
                g2.setColor(new Color(5, 217, 232, 35));
                g2.fillRect(s.x, s.y, s.width, s.height);

                g2.setColor(new Color(5, 217, 232, 75));
                g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4, 4}, 0.0f));
                g2.drawLine(cx, cy + 25, s.x + s.width / 2, s.y);
                g2.setStroke(new BasicStroke(1.0f));
            }

            // Overhead Crane Trolley Housing
            g2.setColor(new Color(35, 25, 55));
            g2.fillRoundRect(cx - 24, cy - 20, 48, 22, 6, 6);
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawRoundRect(cx - 24, cy - 20, 48, 22, 6, 6);

            // Mechanical Cable & Claws
            g2.setColor(Color.WHITE);
            g2.drawLine(cx, cy + 2, cx, cy + 16);

            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawLine(cx - 16, cy + 16, cx, cy + 12);
            g2.drawLine(cx + 16, cy + 16, cx, cy + 12);

            // Held Payload Process Block
            if (craneHeldJob != null && fallingPayload == null) {
                drawJobCard(g2, craneHeldJob, cx - 45, cy + 18, 90, 40);
            }
        }

        private void drawFallingPayload(Graphics2D g2) {
            drawJobCard(g2, fallingPayload.job, (int) fallingPayload.x - 45, (int) fallingPayload.y, 90, 40);
        }

        private void drawJobCard(Graphics2D g2, ProcessJob job, int x, int y, int w, int h) {
            g2.setColor(new Color(24, 16, 42));
            g2.fillRoundRect(x, y, w, h, 6, 6);
            g2.setColor(job.themeColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y, w, h, 6, 6);
            g2.setStroke(new BasicStroke(1.0f));

            BufferedImage avatar = PixelSprites.getJobAvatar(job.avatarIndex, 20);
            if (avatar != null) {
                g2.drawImage(avatar, x + 6, y + 10, null);
            }

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            g2.drawString(job.pid, x + 30, y + 18);

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString(job.sizeKb + " KB", x + 30, y + 31);
        }

        private void drawParticles(Graphics2D g2) {
            for (DefragParticle p : particles) {
                float alpha = (float) p.life / p.maxLife;
                Color c = new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int)(alpha * 255));
                g2.setColor(c);
                g2.fillRect((int) p.x, (int) p.y, 3, 3);
            }
        }

        private void drawOverlayCard(Graphics2D g2, int w, int h, String title, String subtitle, Color c) {
            g2.setColor(new Color(8, 6, 18, 220));
            g2.fillRect(0, 0, w, h);

            int cw = 560;
            int ch = 160;
            int cx = (w - cw) / 2;
            int cy = (h - ch) / 2;

            g2.setColor(new Color(22, 14, 38));
            g2.fillRoundRect(cx, cy, cw, ch, 12, 12);
            g2.setColor(c);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(cx, cy, cw, ch, 12, 12);
            g2.setStroke(new BasicStroke(1.0f));

            g2.setFont(new Font("Consolas", Font.BOLD, 20));
            g2.setColor(c);
            int tw = g2.getFontMetrics().stringWidth(title);
            g2.drawString(title, cx + (cw - tw) / 2, cy + 50);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            int sw = g2.getFontMetrics().stringWidth(subtitle);
            g2.drawString(subtitle, cx + (cw - sw) / 2, cy + 85);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            String action = "PRESS [R] TO PLAY AGAIN   •   PRESS [ESC] TO EXIT";
            int aw = g2.getFontMetrics().stringWidth(action);
            g2.drawString(action, cx + (cw - aw) / 2, cy + 125);
        }
    }
}
