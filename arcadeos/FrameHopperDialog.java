package arcadeos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * FRAME HOPPER // MMU MEMORY BUS CROSSING (V3)
 * Full Overhaul addressing all Priority 1 Bugs & Priority 2 Polish:
 * 1. Key-down hop handling: Held-key auto-repeat does not add extra hops.
 * 2. Deliveries-Focused Scoring: Points awarded strictly for successful frame routing & page-in disk swaps.
 * 3. Floor-Plate Lane Labels: Background lane pills that NEVER cover moving hazard sprites.
 * 4. Rich Panic Screen Teaching Moment: Shows [YOU ROUTED TO] vs [CORRECT TARGET] + exact cause & lesson.
 * 5. Consistent KaTeX / Theme Colors: Page (Cyan), Offset (Yellow), Frame (Green), Missed (Red).
 * 6. High-Visibility Neon Guide Line with proximity-based pulsing.
 */
public class FrameHopperDialog extends JDialog {

    public enum GameMode {
        STANDARD("STANDARD", "Active Paging Config", "Normal memory clock & realistic virtual page requests"),
        TURBO_BUS("TURBO BUS", "Overclocked Bus", "Overclocked 1.6x bus speed & dense I/O traffic! Quick reflexes required"),
        FAULT_CHAOS("FAULT CHAOS", "Virtual Thrashing", "Frequent unmapped pages! Fast Page Fault trap handling required");

        public final String code;
        public final String title;
        public final String tooltip;

        GameMode(String c, String t, String tip) {
            this.code = c;
            this.title = t;
            this.tooltip = tip;
        }
    }

    // Fixed Logical Dimensions for Pixel-Perfect Nearest-Neighbor Scaling
    public static final int VIRTUAL_W = 940;
    public static final int VIRTUAL_H = 600;
    private static final int GUTTER_W = 118;
    private static final int NUM_ROWS = 9;
    private static final int ROW_H = 50;
    private static final int TOP_OFFSET_Y = 100;

    // Rows
    private static final int ROW_DOCKS = 0;          // Physical Frame Docks & Fault Chute
    private static final int ROW_RIVER_TOP = 1;      // DMA Cache Stream (River)
    private static final int ROW_RIVER_MID = 2;      // TLB Fast-Path (River)
    private static final int ROW_RIVER_BOT = 3;      // Data Bus Stream (River)
    private static final int ROW_MEDIAN = 4;         // Safe MMU Pipeline Median
    private static final int ROW_ROAD_TOP = 5;       // IRQ Interrupt Spikes (Road)
    private static final int ROW_ROAD_MID = 6;       // Voltage Glitch Waves (Road)
    private static final int ROW_ROAD_BOT = 7;       // I/O DMA Traffic (Road)
    private static final int ROW_START = 8;          // Virtual Memory Launchpad (Safe)

    private static final Font FONT_BOLD_08 = new Font("Consolas", Font.BOLD, 8);
    private static final Font FONT_BOLD_09 = new Font("Consolas", Font.BOLD, 9);
    private static final Font FONT_BOLD_10 = new Font("Consolas", Font.BOLD, 10);
    private static final Font FONT_BOLD_11 = new Font("Consolas", Font.BOLD, 11);
    private static final Font FONT_BOLD_12 = new Font("Consolas", Font.BOLD, 12);
    private static final Font FONT_BOLD_13 = new Font("Consolas", Font.BOLD, 13);
    private static final Font FONT_BOLD_14 = new Font("Consolas", Font.BOLD, 14);
    private static final Font FONT_BOLD_16 = new Font("Consolas", Font.BOLD, 16);
    private static final Font FONT_BOLD_18 = new Font("Consolas", Font.BOLD, 18);
    private static final Font FONT_PLAIN_09 = new Font("Consolas", Font.PLAIN, 9);
    private static final Font FONT_PLAIN_10 = new Font("Consolas", Font.PLAIN, 10);
    private static final Font FONT_PLAIN_11 = new Font("Consolas", Font.PLAIN, 11);
    private static final Font FONT_PLAIN_12 = new Font("Consolas", Font.PLAIN, 12);

    // Hop input advances once per key-down; turbo has an independent short cooldown.
    private long lastTurboTimestamp = 0;
    private static final long TURBO_COOLDOWN_MS = 850;
    private final Set<Integer> heldKeys = new HashSet<>();

    // Game Configuration
    private int pageSize;
    private int pageTableSize;
    private Map<Integer, Integer> pageTable;
    private GameMode currentMode = GameMode.STANDARD;
    private HopperCanvas canvas;
    private javax.swing.Timer gameTimer;
    private ScreenDim dim;

    // Mode Buttons for header
    private JButton[] modeButtons;

    // Player State
    private float playerX = VIRTUAL_W / 2f;
    private int playerRow = ROW_START;
    private int lives = 3;
    private int score = 0;
    private int completedQueries = 0;
    private int totalTargetQueries = 6;
    private double simTime = 0.0;
    private boolean isGameOver = false;
    private boolean isVictory = false;

    // Visual Sprites
    private BufferedImage frogSprite;
    private BufferedImage irqChipSprite;
    private BufferedImage glitchHazardSprite;
    private BufferedImage truckHazardSprite;

    // Screen Shake State
    private int shakeDuration = 0;
    private int shakeIntensity = 0;
    private int shakeOffsetX = 0;
    private int shakeOffsetY = 0;

    // Tutorial Overlay State
    private boolean showTutorial = true;
    public void dismissTutorial() { this.showTutorial = false; }

    // Disk Swap Loading State (Slow disk I/O simulation on Page Fault)
    private boolean isSwappingDisk = false;
    private float diskSwapProgress = 0f;

    // Active Mission Query
    public static class MemoryRequest {
        public int logicalAddr;
        public int pageNumber;
        public int offset;
        public int targetFrame; // -1 if Page Fault
        public boolean isPageFault;
        public boolean completedSuccessfully;
        public String playerDestination = "";
        public String failureReason = "";

        public MemoryRequest(int addr, int pageSize, Map<Integer, Integer> table) {
            this.logicalAddr = addr;
            this.pageNumber = addr / pageSize;
            this.offset = addr % pageSize;
            if (table.containsKey(pageNumber)) {
                this.targetFrame = table.get(pageNumber);
                this.isPageFault = false;
            } else {
                this.targetFrame = -1;
                this.isPageFault = true;
            }
        }
    }

    private MemoryRequest currentQuery;
    private List<MemoryRequest> queryQueue = new ArrayList<>();
    private List<Attempt> recapHistory = new ArrayList<>();

    private static final class Attempt {
        final int addr, page, offset, targetFrame;
        final boolean pageFault, ok;
        final String dest, reason;
        int count = 1;

        Attempt(MemoryRequest q, boolean ok, String dest, String reason) {
            addr = q.logicalAddr;
            page = q.pageNumber;
            offset = q.offset;
            targetFrame = q.targetFrame;
            pageFault = q.isPageFault;
            this.ok = ok;
            this.dest = dest;
            this.reason = reason;
        }
    }

    // Moving Entities
    public static class MovingEntity {
        public float x;
        public int row;
        public float width;
        public float height;
        public float speed;
        public Color color;
        public String label;
        public int hazardType; // 0: IRQ, 1: GLITCH, 2: TRUCK

        public MovingEntity(float x, int row, float w, float h, float speed, Color c, String lbl, int type) {
            this.x = x;
            this.row = row;
            this.width = w;
            this.height = h;
            this.speed = speed;
            this.color = c;
            this.label = lbl;
            this.hazardType = type;
        }

        public Rectangle getBounds() {
            int y = TOP_OFFSET_Y + row * ROW_H + (int) ((ROW_H - height) / 2);
            return new Rectangle((int) x, y, (int) width, (int) height);
        }
    }

    // Top Frame Bay Dock
    public static class FrameBay {
        public int frameNumber;
        public int baseAddress;
        public int x;
        public int y;
        public int width;
        public int height;
        public boolean isFaultChute;

        public FrameBay(int f, int base, int x, int y, int w, int h, boolean fault) {
            this.frameNumber = f;
            this.baseAddress = base;
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.isFaultChute = fault;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    // Floating Score Popups
    public static class FloatingText {
        public float x, y;
        public String text;
        public Color color;
        public int life;

        public FloatingText(float x, float y, String text, Color c) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = c;
            this.life = 45;
        }
    }

    private List<MovingEntity> busBuffers = new ArrayList<>();
    private List<MovingEntity> roadHazards = new ArrayList<>();
    private List<FrameBay> frameBays = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();
    private List<FloatingText> floatingTexts = new ArrayList<>();

    // Feedback Ticker
    private String feedbackBanner = "MISSION READY: ROUTE VIRTUAL ADDRESS PACKETS TO PHYSICAL FRAMES!";
    private Color bannerColor = RetroTheme.ACCENT_GREEN;
    private int bannerTimer = 180;

    public FrameHopperDialog(JFrame parent, int pageSize, Map<Integer, Integer> pageTable) {
        this(parent, pageSize, pageTable, inferPageTableSize(pageTable));
    }

    public FrameHopperDialog(JFrame parent, int pageSize, Map<Integer, Integer> pageTable, int pageTableSize) {
        super(parent, "PIXELFORGE // FRAME HOPPER: MMU MEMORY BUS CROSSING", true);
        setSize(RetroTheme.getClampedDialogSize(parent, 980, 740));
        setMinimumSize(new Dimension(840, 600));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout());

        this.pageSize = (pageSize > 0) ? pageSize : 1024;
        this.pageTable = (pageTable != null && !pageTable.isEmpty()) ? new LinkedHashMap<>(pageTable) : getDefaultDemoTable();
        this.pageTableSize = Math.max(pageTableSize, this.pageTable.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1);

        // Pixel Sprites (Clean ASCII, no missing glyphs)
        this.frogSprite = PixelSprites.createFrogSprite(32, RetroTheme.ACCENT_GREEN);
        this.irqChipSprite = PixelSprites.createChipHazardSprite(30, RetroTheme.ACCENT_PINK);
        this.glitchHazardSprite = PixelSprites.createGlitchHazardSprite(36, 24, RetroTheme.ACCENT_YELLOW);
        this.truckHazardSprite = PixelSprites.createTruckHazardSprite(46, 28, new Color(5, 217, 232));

        // 1. Top HUD Header
        add(createTopControlHeader(), BorderLayout.NORTH);

        // 2. Scaled Pixel Canvas
        canvas = new HopperCanvas();
        add(canvas, BorderLayout.CENTER);

        // 3. Bottom Controls Bar
        add(createBottomControlsBar(), BorderLayout.SOUTH);

        // Keyboard Controls with Debounce
        setupKeyboardControls();

        // 60 FPS Fixed-Timestep Loop
        gameTimer = new javax.swing.Timer(16, e -> updateGameLoop());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                resetGameSession();
                gameTimer.start();
                canvas.requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (gameTimer != null) gameTimer.stop();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                if (gameTimer != null) gameTimer.stop();
            }
        });
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                heldKeys.clear();
            }
        });
    }

    private static int inferPageTableSize(Map<Integer, Integer> table) {
        int maxPage = table == null ? -1 : table.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
        return maxPage + 1;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible && !isVisible() && getOwner() instanceof JFrame && dim == null) {
            dim = new ScreenDim((JFrame) getOwner(), 190);
            dim.on();
        }
        try {
            super.setVisible(visible);
        } finally {
            if (!visible || !isVisible()) restoreOwner();
        }
    }

    private void restoreOwner() {
        if (dim != null) {
            dim.off();
            dim = null;
        }
    }

    @Override
    public void dispose() {
        restoreOwner();
        if (gameTimer != null) gameTimer.stop();
        super.dispose();
    }

    private static boolean routed(String dest) {
        return dest != null && (dest.startsWith("FRAME") || dest.startsWith("DISK"));
    }

    private Map<Integer, Integer> getDefaultDemoTable() {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        map.put(0, 5);
        map.put(1, 6);
        map.put(2, 1);
        map.put(3, 2);
        return map;
    }

    private JPanel createTopControlHeader() {
        JPanel bar = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(14, 26, 20));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(35, 75, 55));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        // Left Tag with frog pixel icon (ASCII clean title)
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel iconLbl = new JLabel(new ImageIcon(PixelSprites.createFrogSprite(22, RetroTheme.ACCENT_GREEN)));
        left.add(iconLbl);

        JLabel titleLbl = new JLabel("FRAME HOPPER // MMU BUS");
        titleLbl.setFont(FONT_BOLD_13);
        titleLbl.setForeground(RetroTheme.ACCENT_GREEN);
        left.add(titleLbl);

        // Center: Mode Switch Buttons with Active Highlight and Tooltips
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        tabs.setOpaque(false);

        GameMode[] modes = GameMode.values();
        modeButtons = new JButton[modes.length];
        for (int i = 0; i < modes.length; i++) {
            final GameMode m = modes[i];
            JButton btn = new JButton(m.code) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean isSel = (currentMode == m);
                    if (isSel) {
                        g2.setColor(new Color(25, 60, 42));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                        g2.setColor(RetroTheme.ACCENT_GREEN);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                        g2.fillRect(4, getHeight() - 3, getWidth() - 8, 3);
                    } else {
                        g2.setColor(RetroTheme.BTN_DARK);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(FONT_BOLD_10);
            btn.setForeground((currentMode == m) ? RetroTheme.ACCENT_GREEN : RetroTheme.TEXT_DIM);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
            btn.setToolTipText(m.tooltip);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                this.currentMode = m;
                updateModeButtonStyles();
                resetGameSession();
                canvas.requestFocusInWindow();
            });
            modeButtons[i] = btn;
            tabs.add(btn);
        }

        // Right HUD stats
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        JLabel pgLbl = new JLabel(String.format("PAGE SIZE: %d B", pageSize));
        pgLbl.setFont(FONT_BOLD_11);
        pgLbl.setForeground(RetroTheme.ACCENT_CYAN);
        right.add(pgLbl);

        bar.add(left, BorderLayout.WEST);
        bar.add(tabs, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void updateModeButtonStyles() {
        if (modeButtons == null) return;
        GameMode[] modes = GameMode.values();
        for (int i = 0; i < modes.length; i++) {
            boolean isSel = (currentMode == modes[i]);
            modeButtons[i].setForeground(isSel ? RetroTheme.ACCENT_GREEN : RetroTheme.TEXT_DIM);
            modeButtons[i].repaint();
        }
    }

    private JPanel createBottomControlsBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(12, 18, 16));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(30, 60, 45));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JLabel help = new JLabel(
                "[W][A][S][D] / [ARROWS] Hop  |  [SPACE] Turbo Leap  |  [R] Restart Level  |  [ESC] Exit");
        help.setFont(FONT_BOLD_12);
        help.setForeground(RetroTheme.TEXT_WHITE);
        bar.add(help, BorderLayout.WEST);

        JButton btnClose = RetroTheme.createRetroButton("RETURN TO OS [ESC]", RetroTheme.BTN_DARK, RetroTheme.ACCENT_PINK);
        btnClose.setFont(FONT_BOLD_11);
        btnClose.addActionListener(e -> dispose());
        bar.add(btnClose, BorderLayout.EAST);

        return bar;
    }

    private void setupKeyboardControls() {
        InputMap im = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = canvas.getActionMap();

        bindKey(KeyEvent.VK_UP, () -> hop(0, -1));
        bindKey(KeyEvent.VK_W, () -> hop(0, -1));
        bindKey(KeyEvent.VK_DOWN, () -> hop(0, 1));
        bindKey(KeyEvent.VK_S, () -> hop(0, 1));
        bindKey(KeyEvent.VK_LEFT, () -> hop(-1, 0));
        bindKey(KeyEvent.VK_A, () -> hop(-1, 0));
        bindKey(KeyEvent.VK_RIGHT, () -> hop(1, 0));
        bindKey(KeyEvent.VK_D, () -> hop(1, 0));
        bindKey(KeyEvent.VK_SPACE, () -> {
            if (showTutorial) showTutorial = false;
            else turboHopWithCooldown();
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "RESTART");
        am.put("RESTART", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { if (canvas.isShowing()) resetGameSession(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CLOSE");
        am.put("CLOSE", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { if (canvas.isShowing()) dispose(); } });
    }

    private void bindKey(int code, Runnable onPress) {
        InputMap im = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = canvas.getActionMap();
        String press = "press-" + code;
        String release = "release-" + code;
        im.put(KeyStroke.getKeyStroke(code, 0, false), press);
        im.put(KeyStroke.getKeyStroke(code, 0, true), release);
        am.put(press, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!canvas.isShowing()) return;
                if (heldKeys.add(code)) onPress.run();
            }
        });
        am.put(release, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (!canvas.isShowing()) return;
                heldKeys.remove(code);
            }
        });
    }

    private void turboHopWithCooldown() {
        long now = System.currentTimeMillis();
        if (now - lastTurboTimestamp < TURBO_COOLDOWN_MS) return;
        lastTurboTimestamp = now;
        turboHop();
    }

    private void hop(int dx, int dy) {
        if (showTutorial) { showTutorial = false; return; }
        if (isGameOver || isVictory || isSwappingDisk) return;

        int newRow = playerRow + dy;
        if (newRow >= ROW_DOCKS && newRow <= ROW_START) {
            playerRow = newRow;
        }

        playerX += dx * 42.0f;
        if (playerX < GUTTER_W) playerX = GUTTER_W;
        if (playerX > VIRTUAL_W - 50) playerX = VIRTUAL_W - 50;

        // Spark particles at feet
        for (int i = 0; i < 4; i++) {
            particles.add(new Particle(playerX + 16, getPlayerPixelY() + 28,
                    (float) (Math.random() * 2 - 1), (float) (Math.random() * 2 - 1), RetroTheme.ACCENT_GREEN));
        }

        if (playerRow == ROW_DOCKS) {
            checkDockLanding();
        }
    }

    private void turboHop() {
        if (isGameOver || isVictory || isSwappingDisk) return;
        if (playerRow > ROW_DOCKS + 1) {
            playerRow -= 2;
            for (int i = 0; i < 10; i++) {
                particles.add(new Particle(playerX + 16, getPlayerPixelY() + 16,
                        (float) (Math.random() * 4 - 2), (float) (Math.random() * 4 - 2), RetroTheme.ACCENT_CYAN));
            }
            if (playerRow == ROW_DOCKS) {
                checkDockLanding();
            }
        }
    }

    private int getPlayerPixelY() {
        return TOP_OFFSET_Y + playerRow * ROW_H + (ROW_H - 32) / 2;
    }

    private void resetGameSession() {
        playerX = VIRTUAL_W / 2f - 16;
        playerRow = ROW_START;
        lives = 3;
        score = 0;
        completedQueries = 0;
        simTime = 0.0;
        isGameOver = false;
        isVictory = false;
        isSwappingDisk = false;
        particles.clear();
        floatingTexts.clear();
        recapHistory.clear();

        setupLevelGeometry();
        generateQueryQueue();
        nextQuery();

        if (currentMode == GameMode.FAULT_CHAOS && !hasUnmappedPages()) {
            feedbackBanner = "NO UNMAPPED PAGES: FAULT CHAOS NEEDS [X] ROWS";
            bannerColor = RetroTheme.ACCENT_PINK;
        } else {
            feedbackBanner = "MODE: " + currentMode.title + " // ROUTE PACKET TO TARGET FRAME!";
            bannerColor = RetroTheme.ACCENT_GREEN;
        }
        bannerTimer = 180;
    }

    private void setupLevelGeometry() {
        busBuffers.clear();
        roadHazards.clear();
        frameBays.clear();

        float speedMult = (currentMode == GameMode.TURBO_BUS) ? 1.55f : 1.0f;

        // 1. Build Top Frame Bays with Base Addresses
        List<Integer> distinctFrames = new ArrayList<>(new LinkedHashSet<>(pageTable.values()));
        if (distinctFrames.size() > 6) {
            distinctFrames = new ArrayList<>(distinctFrames.subList(0, 6));
        }
        Collections.sort(distinctFrames);
        int numBays = distinctFrames.size();
        int bayW = 88;
        int bayH = 46;
        int startX = 125;
        int faultX = VIRTUAL_W - 130;
        int spacing = (numBays > 1)
                ? Math.max(0, (faultX - 32 - startX - numBays * bayW) / (numBays - 1))
                : 0;

        for (int i = 0; i < numBays; i++) {
            int f = distinctFrames.get(i);
            int base = f * pageSize;
            int bx = startX + i * (bayW + spacing);
            frameBays.add(new FrameBay(f, base, bx, TOP_OFFSET_Y + 2, bayW, bayH, false));
        }

        // Add Disk Swap / Page Fault Chute on the right
        frameBays.add(new FrameBay(-1, 0, faultX, TOP_OFFSET_Y + 2, 110, bayH, true));

        // 2. Bus River Moving Buffers (Rows 1, 2, 3)
        // Row 1: DMA Cache Stream (Moves Right)
        for (int i = 0; i < 4; i++) {
            busBuffers.add(new MovingEntity(i * 240, ROW_RIVER_TOP, 130, 34, 1.9f * speedMult,
                    new Color(24, 75, 95), "DMA-CACHE", 0));
        }
        // Row 2: TLB Buffer Stream (Moves Left)
        for (int i = 0; i < 3; i++) {
            busBuffers.add(new MovingEntity(i * 320, ROW_RIVER_MID, 170, 34, -2.4f * speedMult,
                    new Color(45, 90, 70), "TLB-FASTPATH", 1));
        }
        // Row 3: Data Bus RAM Chunks (Moves Right)
        for (int i = 0; i < 4; i++) {
            busBuffers.add(new MovingEntity(i * 260, ROW_RIVER_BOT, 140, 34, 1.4f * speedMult,
                    new Color(30, 80, 110), "DATA-BUS", 2));
        }

        // 3. Highway Hazards (Distinct Silhouettes & Sizes)
        // Row 5: Spiky IRQ Interrupt Chips (Moves Left)
        for (int i = 0; i < 4; i++) {
            roadHazards.add(new MovingEntity(i * 240, ROW_ROAD_TOP, 30, 30, -2.4f * speedMult,
                    RetroTheme.ACCENT_PINK, "IRQ", 0));
        }
        // Row 6: Jagged Voltage Glitches (Moves Right)
        for (int i = 0; i < 3; i++) {
            roadHazards.add(new MovingEntity(i * 310, ROW_ROAD_MID, 36, 24, 3.0f * speedMult,
                    RetroTheme.ACCENT_YELLOW, "GLITCH", 1));
        }
        // Row 7: Heavy DMA / I/O Transport Trucks (Moves Left)
        for (int i = 0; i < 4; i++) {
            roadHazards.add(new MovingEntity(i * 250, ROW_ROAD_BOT, 46, 28, -1.9f * speedMult,
                    new Color(5, 217, 232), "I/O-TRUCK", 2));
        }
    }

    private void generateQueryQueue() {
        queryQueue.clear();
        List<Integer> dockFrames = new ArrayList<>();
        for (FrameBay bay : frameBays) {
            if (!bay.isFaultChute) dockFrames.add(bay.frameNumber);
        }
        Set<Integer> dockFrameSet = new HashSet<>(dockFrames);
        List<Integer> pages = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : pageTable.entrySet()) {
            if (dockFrameSet.contains(entry.getValue())) pages.add(entry.getKey());
        }

        List<Integer> faultPages = new ArrayList<>();
        for (int p = 0; p < pageTableSize; p++) {
            if (!pageTable.containsKey(p)) faultPages.add(p);
        }

        Random rand = new Random();
        for (int i = 0; i < totalTargetQueries; i++) {
            boolean makeFault = !faultPages.isEmpty()
                    && ((currentMode == GameMode.FAULT_CHAOS) ? (i % 2 == 1) : (i == totalTargetQueries - 2));

            if (makeFault) {
                int faultPage = faultPages.get(rand.nextInt(faultPages.size()));
                int off = rand.nextInt(pageSize);
                queryQueue.add(new MemoryRequest(faultPage * pageSize + off, pageSize, pageTable));
            } else {
                int p = pages.get(rand.nextInt(pages.size()));
                int off = rand.nextInt(pageSize);
                queryQueue.add(new MemoryRequest(p * pageSize + off, pageSize, pageTable));
            }
        }
    }

    private boolean hasUnmappedPages() {
        for (int p = 0; p < pageTableSize; p++) {
            if (!pageTable.containsKey(p)) return true;
        }
        return false;
    }

    private void nextQuery() {
        if (queryQueue.isEmpty()) {
            isVictory = true;
            feedbackBanner = "[*] VICTORY! ALL MEMORY PACKETS DELIVERED CLEANLY! [*]";
            bannerColor = RetroTheme.ACCENT_YELLOW;
            return;
        }
        currentQuery = queryQueue.remove(0);
        playerRow = ROW_START;
        playerX = VIRTUAL_W / 2f - 16;
    }

    private void checkDockLanding() {
        int cx = (int) playerX + 16;
        int cy = getPlayerPixelY() + 16;
        FrameBay landedBay = null;

        for (FrameBay bay : frameBays) {
            if (bay.getBounds().contains(cx, cy)) {
                landedBay = bay;
                break;
            }
        }

        if (landedBay == null) {
            playerRow = ROW_MEDIAN;
            triggerShake(6, 4);
            feedbackBanner = "[!] BOUNCED! HIT FRAME BARRIER! AIM FOR A FRAME DOCK [!]";
            bannerColor = RetroTheme.ACCENT_YELLOW;
            bannerTimer = 60;
            return;
        }

        if (currentQuery.isPageFault) {
            if (landedBay.isFaultChute) {
                currentQuery.completedSuccessfully = true;
                currentQuery.playerDestination = "DISK SWAP CHUTE";
                recordAttempt(true, currentQuery.playerDestination, "");
                score += 350; // Deliveries reward
                completedQueries++;
                triggerExplosion(playerX + 16, getPlayerPixelY() + 16, RetroTheme.ACCENT_YELLOW, 25);
                floatingTexts.add(new FloatingText(playerX, getPlayerPixelY() + 56, "+350 PTS [PAGE FAULT HANDLED]", RetroTheme.ACCENT_YELLOW));

                isSwappingDisk = true;
                diskSwapProgress = 0f;
                feedbackBanner = String.format("[OK] PAGE FAULT HANDLED: Page %d Routed to Disk Chute (+350 PTS)", currentQuery.pageNumber);
                bannerColor = RetroTheme.ACCENT_YELLOW;
                bannerTimer = 120;
            } else {
                currentQuery.completedSuccessfully = false;
                currentQuery.playerDestination = "FRAME " + landedBay.frameNumber;
                currentQuery.failureReason = String.format("Mismatched landing! Page %d is unmapped; must go to Disk Chute!", currentQuery.pageNumber);
                recordAttempt(false, currentQuery.playerDestination, currentQuery.failureReason);
                lives--;
                triggerShake(12, 8);
                triggerExplosion(playerX + 16, getPlayerPixelY() + 16, RetroTheme.ACCENT_PINK, 25);
                feedbackBanner = String.format("[!] INVALID PAGE FAULT: Page %d is NOT in Table! -1 Core", currentQuery.pageNumber);
                bannerColor = RetroTheme.ACCENT_PINK;
                bannerTimer = 120;
                respawnPlayer();
            }
        } else {
            if (landedBay.frameNumber == currentQuery.targetFrame) {
                currentQuery.completedSuccessfully = true;
                currentQuery.playerDestination = "FRAME " + landedBay.frameNumber;
                recordAttempt(true, currentQuery.playerDestination, "");
                int physicalAddr = (currentQuery.targetFrame * pageSize) + currentQuery.offset;
                score += 250; // Deliveries reward
                completedQueries++;
                triggerExplosion(playerX + 16, getPlayerPixelY() + 16, RetroTheme.ACCENT_GREEN, 25);
                floatingTexts.add(new FloatingText(playerX, getPlayerPixelY() + 56, "+250 PTS [TRANSLATION OK]", RetroTheme.ACCENT_GREEN));

                feedbackBanner = String.format("[OK] TRANSLATION SUCCESS: Page %d -> Frame %d [Phys: %d B] (+250 PTS)",
                        currentQuery.pageNumber, currentQuery.targetFrame, physicalAddr);
                bannerColor = RetroTheme.ACCENT_GREEN;
                bannerTimer = 120;
                nextQuery();
            } else if (landedBay.isFaultChute) {
                currentQuery.completedSuccessfully = false;
                currentQuery.playerDestination = "DISK SWAP CHUTE";
                currentQuery.failureReason = String.format("Spurious Trap! Page %d is valid and maps to Frame %d!", currentQuery.pageNumber, currentQuery.targetFrame);
                recordAttempt(false, currentQuery.playerDestination, currentQuery.failureReason);
                lives--;
                triggerShake(10, 6);
                triggerExplosion(playerX + 16, getPlayerPixelY() + 16, RetroTheme.ACCENT_PINK, 20);
                feedbackBanner = String.format("[!] SPURIOUS TRAP: Page %d is valid and mapped to Frame %d! -1 Core",
                        currentQuery.pageNumber, currentQuery.targetFrame);
                bannerColor = RetroTheme.ACCENT_PINK;
                bannerTimer = 120;
                respawnPlayer();
            } else {
                currentQuery.completedSuccessfully = false;
                currentQuery.playerDestination = "FRAME " + landedBay.frameNumber;
                currentQuery.failureReason = String.format("Mismapped! Page %d maps to Frame %d, not Frame %d!", currentQuery.pageNumber, currentQuery.targetFrame, landedBay.frameNumber);
                recordAttempt(false, currentQuery.playerDestination, currentQuery.failureReason);
                lives--;
                triggerShake(10, 6);
                triggerExplosion(playerX + 16, getPlayerPixelY() + 16, RetroTheme.ACCENT_PINK, 20);
                feedbackBanner = String.format("[!] MISMAPPED FRAME: Page %d maps to Frame %d, not Frame %d! -1 Core",
                        currentQuery.pageNumber, currentQuery.targetFrame, landedBay.frameNumber);
                bannerColor = RetroTheme.ACCENT_PINK;
                bannerTimer = 120;
                respawnPlayer();
            }
        }

        if (lives <= 0) {
            isGameOver = true;
            feedbackBanner = "[!] KERNEL PANIC: OUT OF PACKETS / UNRECOVERABLE TRAP! [!]";
            bannerColor = RetroTheme.ACCENT_PINK;
        }
    }

    private void respawnPlayer() {
        playerRow = ROW_START;
        playerX = VIRTUAL_W / 2f - 16;
    }

    private void triggerShake(int duration, int intensity) {
        this.shakeDuration = duration;
        this.shakeIntensity = intensity;
    }

    private void updateGameLoop() {
        if (showTutorial) {
            canvas.repaint();
            return;
        }

        if (isGameOver || isVictory) {
            canvas.repaint();
            return;
        }

        simTime += 0.016;

        if (shakeDuration > 0) {
            shakeDuration--;
            shakeOffsetX = (int) ((Math.random() * 2 - 1) * shakeIntensity);
            shakeOffsetY = (int) ((Math.random() * 2 - 1) * shakeIntensity);
        } else {
            shakeOffsetX = 0;
            shakeOffsetY = 0;
        }

        if (isSwappingDisk) {
            diskSwapProgress += 0.025f;
            if (diskSwapProgress >= 1.0f) {
                isSwappingDisk = false;
                diskSwapProgress = 0f;
                nextQuery();
            }
            canvas.repaint();
            return;
        }

        // 1. Update Bus Buffers (River)
        for (MovingEntity b : busBuffers) {
            b.x += b.speed;
            if (b.speed > 0 && b.x > VIRTUAL_W + 50) b.x = -b.width - 40;
            if (b.speed < 0 && b.x < -b.width - 50) b.x = VIRTUAL_W + 40;
        }

        // 2. Update Road Hazards (Traffic)
        for (MovingEntity h : roadHazards) {
            h.x += h.speed;
            if (h.speed > 0 && h.x > VIRTUAL_W + 40) h.x = -h.width - 30;
            if (h.speed < 0 && h.x < -h.width - 40) h.x = VIRTUAL_W + 30;
        }

        // 3. River Floating Logic (Rows 1, 2, 3)
        if (playerRow >= ROW_RIVER_TOP && playerRow <= ROW_RIVER_BOT) {
            Rectangle pRect = new Rectangle((int) playerX, getPlayerPixelY(), 32, 32);
            boolean onBuffer = false;
            for (MovingEntity b : busBuffers) {
                if (b.row == playerRow && b.getBounds().intersects(pRect)) {
                    playerX += b.speed;
                    onBuffer = true;
                    break;
                }
            }
            if (!onBuffer) {
                lives--;
                triggerShake(10, 6);
                triggerExplosion(playerX + 16, getPlayerPixelY() + 16, RetroTheme.ACCENT_CYAN, 20);
                feedbackBanner = "[!] BUS DRIFT ERROR: Packet fell into unbuffered void! -1 Core";
                bannerColor = RetroTheme.ACCENT_CYAN;
                bannerTimer = 120;
                recordHazardLoss("UNBUFFERED BUS VOID", "Fell between cache lines into unbuffered memory space.");
                respawnPlayer();
            } else if (playerX + 16 < GUTTER_W || playerX > VIRTUAL_W - 32) {
                lives--;
                triggerShake(10, 6);
                triggerExplosion(playerX + 16, getPlayerPixelY() + 16, RetroTheme.ACCENT_PINK, 20);
                feedbackBanner = "[!] BUS OVERFLOW: Packet drifted out of memory range! -1 Core";
                bannerColor = RetroTheme.ACCENT_PINK;
                bannerTimer = 120;
                recordHazardLoss("BUS OVERFLOW", "Carried out of physical memory range by bus buffer.");
                respawnPlayer();
            }
        }

        // 4. Highway Collision Logic (Rows 5, 6, 7)
        if (playerRow >= ROW_ROAD_TOP && playerRow <= ROW_ROAD_BOT) {
            Rectangle pRect = new Rectangle((int) playerX + 4, getPlayerPixelY() + 4, 24, 24);
            for (MovingEntity h : roadHazards) {
                if (h.row == playerRow && h.getBounds().intersects(pRect)) {
                    lives--;
                    triggerShake(12, 8);
                    triggerExplosion(playerX + 16, getPlayerPixelY() + 16, RetroTheme.ACCENT_PINK, 25);
                    feedbackBanner = "[!] " + h.label + " HAZARD: Corrupted by Bus Collision! -1 Core";
                    bannerColor = RetroTheme.ACCENT_PINK;
                    bannerTimer = 120;
                    recordHazardLoss(h.label + " COLLISION", "Collided with " + h.label + " hardware interrupt on road.");
                    respawnPlayer();
                    break;
                }
            }
        }

        if (lives <= 0) {
            isGameOver = true;
            feedbackBanner = "[!] KERNEL PANIC: MEMORY CORRUPTED - GAME OVER (PRESS [R])";
            bannerColor = RetroTheme.ACCENT_PINK;
        }

        // 5. Update Particles
        Iterator<Particle> pIt = particles.iterator();
        while (pIt.hasNext()) {
            Particle p = pIt.next();
            p.x += p.vx;
            p.y += p.vy;
            p.life--;
            if (p.life <= 0) pIt.remove();
        }

        // 6. Update Floating Text
        Iterator<FloatingText> fIt = floatingTexts.iterator();
        while (fIt.hasNext()) {
            FloatingText ft = fIt.next();
            ft.y -= 0.8f;
            ft.life--;
            if (ft.life <= 0) fIt.remove();
        }

        if (bannerTimer > 0) bannerTimer--;

        canvas.repaint();
    }

    private void recordHazardLoss(String dest, String reason) {
        if (currentQuery != null) {
            currentQuery.completedSuccessfully = false;
            currentQuery.playerDestination = dest;
            currentQuery.failureReason = reason;
            recordAttempt(false, dest, reason);
        }
    }

    private void recordAttempt(boolean ok, String dest, String reason) {
        if (currentQuery != null) {
            if (!recapHistory.isEmpty()) {
                Attempt last = recapHistory.get(recapHistory.size() - 1);
                if (last.addr == currentQuery.logicalAddr && last.ok == ok
                        && last.dest.equals(dest)) {
                    last.count++;
                    return;
                }
            }
            recapHistory.add(new Attempt(currentQuery, ok, dest, reason));
        }
    }

    private void triggerExplosion(float x, float y, Color c, int count) {
        for (int i = 0; i < count; i++) {
            float ang = (float) (Math.random() * Math.PI * 2);
            float spd = (float) (Math.random() * 4.5 + 1.0);
            particles.add(new Particle(x, y, (float) Math.cos(ang) * spd, (float) Math.sin(ang) * spd, c));
        }
    }

    public static class Particle {
        public float x, y, vx, vy;
        public Color color;
        public int life;

        public Particle(float x, float y, float vx, float vy, Color c) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = c;
            this.life = 20 + (int) (Math.random() * 20);
        }
    }

    // =========================================================================
    // PIXEL SCALED ARCADE CANVAS
    // =========================================================================
    private class HopperCanvas extends JPanel {
        private BufferedImage virtualBuffer;

        public HopperCanvas() {
            setBackground(new Color(8, 6, 18));
            setFocusable(true);
            virtualBuffer = new BufferedImage(VIRTUAL_W, VIRTUAL_H, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D vg = virtualBuffer.createGraphics();
            vg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            vg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

            renderScene(vg);
            vg.dispose();

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            int panelW = getWidth();
            int panelH = getHeight();
            g2.setColor(getBackground());
            g2.fillRect(0, 0, panelW, panelH);
            double raw = Math.min(panelW / (double) VIRTUAL_W, panelH / (double) VIRTUAL_H);
            double scale = raw >= 1.0 ? Math.floor(raw) : raw;
            int drawW = (int) Math.round(VIRTUAL_W * scale);
            int drawH = (int) Math.round(VIRTUAL_H * scale);
            int destX = (panelW - drawW) / 2 + shakeOffsetX;
            int destY = (panelH - drawH) / 2 + shakeOffsetY;
            g2.drawImage(virtualBuffer, destX, destY, drawW, drawH, null);
            g2.dispose();
        }

        private void renderScene(Graphics2D g2) {
            int w = VIRTUAL_W;
            int h = VIRTUAL_H;

            // Background CRT Grid
            g2.setColor(new Color(10, 8, 22));
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(22, 16, 42));
            for (int gx = 0; gx < w; gx += 40) g2.drawLine(gx, 0, gx, h);
            for (int gy = 0; gy < h; gy += 40) g2.drawLine(0, gy, w, gy);

            // 1. Level Lanes & Directional Background Tracks (Rendered FIRST as floor plates!)
            renderLanes(g2, w);

            // 2. Frame Bays (Top Row 0)
            renderFrameBays(g2);

            // Keep sprites out of the lane-label gutter, then repaint opaque labels above.
            Shape oldClip = g2.getClip();
            g2.setClip(GUTTER_W, 0, VIRTUAL_W - GUTTER_W, VIRTUAL_H);

            // 3. Moving River Buffers
            renderBusBuffers(g2);

            // 4. Moving Road Hazards (Rendered in FOREGROUND on top of lane floor plates!)
            renderRoadHazards(g2);
            g2.setClip(oldClip);

            // Keep lane names legible over moving sprites.
            renderLaneLabels(g2);

            // 5. High-Visibility Neon Guide Line
            renderGuideLine(g2);

            // 6. Player Hopper
            renderPlayer(g2);

            // 7. Particles
            for (Particle pt : particles) {
                g2.setColor(pt.color);
                g2.fillRect((int) pt.x, (int) pt.y, 3, 3);
            }

            // 8. Multi-Line Non-Overlapping HUD Header
            renderMultiLineHud(g2, w);

            // Keep score popups below the HUD and inside the playfield.
            for (FloatingText ft : floatingTexts) {
                g2.setFont(FONT_BOLD_12);
                int tw = g2.getFontMetrics().stringWidth(ft.text);
                int x = (int) Math.max(GUTTER_W, Math.min(ft.x, VIRTUAL_W - tw - 12));
                g2.setColor(new Color(10, 8, 20, 200));
                g2.fillRect(x - 3, (int) ft.y - 12, tw + 6, 16);
                g2.setColor(ft.color);
                g2.drawString(ft.text, x, (int) ft.y);
            }

            // 10. Bottom Ticker
            renderFeedbackTicker(g2, w, h);

            // 11. Disk Swap Progress Overlay
            if (isSwappingDisk) {
                renderDiskSwapLoading(g2, w, h);
            }

            // 12. 4-Step Tutorial Overlay
            if (showTutorial) {
                renderTutorialOverlay(g2, w, h);
            }

            // 13. Victory or Game Over Overlay
            if (isVictory) renderRoundSummary(g2, w, h, true);
            else if (isGameOver) renderRoundSummary(g2, w, h, false);
        }

        private void renderMultiLineHud(Graphics2D g2, int w) {
            g2.setColor(new Color(14, 22, 28, 240));
            g2.fillRoundRect(14, 8, w - 28, 78, 8, 8);
            g2.setColor(new Color(40, 85, 95));
            g2.drawRoundRect(14, 8, w - 28, 78, 8, 8);

            if (currentQuery != null) {
                // ROW 1: Query Address | Score (Deliveries strictly) | Cores
                g2.setFont(FONT_BOLD_12);
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawString(String.format("1. QUERY: L-ADDR [ %d ]", currentQuery.logicalAddr), 26, 26);

                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawString(String.format("SCORE: %05d", score), w - 240, 26);

                for (int i = 0; i < 3; i++) {
                    int px = w - 85 + i * 20;
                    int py = 16;
                    if (i < lives) {
                        g2.setColor(RetroTheme.ACCENT_GREEN);
                        g2.fillRoundRect(px, py, 14, 12, 3, 3);
                    } else {
                        g2.setColor(new Color(40, 40, 60));
                        g2.drawRoundRect(px, py, 14, 12, 3, 3);
                    }
                }

                // ROW 2: Decomposition Calculation | Progress
                g2.setFont(FONT_PLAIN_11);
                g2.setColor(RetroTheme.TEXT_WHITE);
                g2.drawString(String.format("2. CALC:  Page %d (%d / %d B)  |  Offset: %d Bytes",
                        currentQuery.pageNumber, currentQuery.logicalAddr, pageSize, currentQuery.offset), 26, 46);

                g2.setFont(FONT_PLAIN_10);
                g2.setColor(Color.WHITE);
                g2.drawString(String.format("DELIVERED: %d / %d", completedQueries, totalTargetQueries), w - 240, 46);

                // ROW 3: Dedicated Target Frame Line + BRIGHT Key Lesson Notice!
                g2.setFont(FONT_BOLD_11);
                if (currentQuery.isPageFault) {
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    g2.drawString("3. TARGET: [ PAGE FAULT DETECTED ] => ROUTE TO DISK SWAP CHUTE ON RIGHT!", 26, 68);
                    g2.setFont(FONT_BOLD_10);
                    g2.setColor(RetroTheme.ACCENT_YELLOW);
                    g2.drawString("[KEY LESSON: valid bit = 0 -> trap to OS -> load page from disk]", w - 410, 68);
                } else {
                    int phys = (currentQuery.targetFrame * pageSize) + currentQuery.offset;
                    g2.setColor(RetroTheme.ACCENT_GREEN);
                    g2.drawString(String.format("3. TARGET: FRAME %d [Phys = (%d * %d) + %d = %d B]",
                            currentQuery.targetFrame, currentQuery.targetFrame, pageSize, currentQuery.offset, phys), 26, 68);

                    g2.setFont(FONT_BOLD_10);
                    g2.setColor(RetroTheme.ACCENT_YELLOW);
                    g2.drawString(String.format("[KEY LESSON: Offset (%d B) remains identical in RAM!]", currentQuery.offset), w - 380, 68);
                }
            }
        }

        private void renderLanes(Graphics2D g2, int w) {
            // Row 0: Docks Zone
            int y0 = TOP_OFFSET_Y + ROW_DOCKS * ROW_H;
            g2.setColor(new Color(14, 18, 30));
            g2.fillRect(0, y0, w, ROW_H);

            renderLaneFloorTag(g2, 8, y0 + 14, "RAM FRAMES", new Color(30, 40, 65, 160), RetroTheme.TEXT_WHITE);

            // Rows 1-3: River Zone (High-Speed Memory Bus)
            int yRiver = TOP_OFFSET_Y + ROW_RIVER_TOP * ROW_H;
            g2.setColor(new Color(10, 22, 38));
            g2.fillRect(0, yRiver, w, ROW_H * 3);

            String[] riverPills = {"CACHE (>>)", "TLB-FAST (<<)", "DATA-BUS (>>)"};
            for (int r = ROW_RIVER_TOP; r <= ROW_RIVER_BOT; r++) {
                int ry = TOP_OFFSET_Y + r * ROW_H;
                g2.setColor(new Color(20, 44, 75));
                g2.drawLine(0, ry + ROW_H - 1, w, ry + ROW_H - 1);

                renderLaneFloorTag(g2, 8, ry + 14, riverPills[r - ROW_RIVER_TOP], new Color(16, 42, 60, 150), RetroTheme.ACCENT_CYAN);

                boolean right = (r != ROW_RIVER_MID);
                String arrow = right ? ">>  >>  >>  >>  >>  >>  >>  >>  >>  >>" : "<<  <<  <<  <<  <<  <<  <<  <<  <<  <<";
                g2.setFont(FONT_PLAIN_10);
                g2.setColor(new Color(18, 42, 68));
                g2.drawString(arrow, 160, ry + ROW_H / 2 + 4);
                g2.drawString(arrow, 540, ry + ROW_H / 2 + 4);
            }

            // Row 4: Median (MMU Pipeline Safe Zone)
            int yMed = TOP_OFFSET_Y + ROW_MEDIAN * ROW_H;
            g2.setColor(new Color(24, 32, 54));
            g2.fillRect(0, yMed, w, ROW_H);
            g2.setColor(new Color(55, 75, 120));
            g2.drawRect(0, yMed, w, ROW_H);

            renderStickyPageTableStrip(g2, yMed);

            // Rows 5-7: Road Zone (Interrupt Highway)
            int yRoad = TOP_OFFSET_Y + ROW_ROAD_TOP * ROW_H;
            g2.setColor(new Color(16, 12, 28));
            g2.fillRect(0, yRoad, w, ROW_H * 3);

            String[] roadPills = {"IRQ-CHIP (<<)", "GLITCH (>>)", "I/O-TRUCK (<<)"};
            for (int r = ROW_ROAD_TOP; r <= ROW_ROAD_BOT; r++) {
                int ry = TOP_OFFSET_Y + r * ROW_H;

                renderLaneFloorTag(g2, 8, ry + 14, roadPills[r - ROW_ROAD_TOP], new Color(38, 18, 48, 150), RetroTheme.ACCENT_PINK);

                boolean right = (r == ROW_ROAD_MID);
                String arrow = right ? ">>  >>  >>  >>  >>  >>  >>  >>  >>  >>" : "<<  <<  <<  <<  <<  <<  <<  <<  <<  <<";
                g2.setFont(FONT_PLAIN_10);
                g2.setColor(new Color(38, 22, 58));
                g2.drawString(arrow, 160, ry + ROW_H / 2 + 4);
                g2.drawString(arrow, 540, ry + ROW_H / 2 + 4);
            }

            // Row 8: Virtual Address Space Launchpad
            int yStart = TOP_OFFSET_Y + ROW_START * ROW_H;
            g2.setColor(new Color(18, 30, 24));
            g2.fillRect(0, yStart, w, ROW_H);
            g2.setColor(new Color(40, 80, 55));
            g2.drawRect(0, yStart, w, ROW_H);

            g2.setFont(FONT_BOLD_10);
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString("PROCESS VIRTUAL ADDRESS RUNWAY (SAFE LAUNCHPAD)", 24, yStart + 30);
        }

        // Floor-Plate style tag drawn in background behind sprites
        private void renderLaneFloorTag(Graphics2D g2, int x, int y, String label, Color bg, Color fg) {
            int pillW = 105;
            int pillH = 20;
            g2.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue()));
            g2.fillRoundRect(x, y, pillW, pillH, 4, 4);
            g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 90));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(x, y, pillW, pillH, 4, 4);

            g2.setFont(FONT_BOLD_10);
            g2.setColor(fg);
            g2.drawString(label, x + 6, y + 14);
        }

        private void renderLaneLabels(Graphics2D g2) {
            String[] riverPills = {"CACHE (>>)", "TLB-FAST (<<)", "DATA-BUS (>>)"};
            for (int row = ROW_RIVER_TOP; row <= ROW_RIVER_BOT; row++) {
                int ry = TOP_OFFSET_Y + row * ROW_H;
                renderLaneFloorTag(g2, 8, ry + 14, riverPills[row - ROW_RIVER_TOP],
                        new Color(16, 42, 60), RetroTheme.ACCENT_CYAN);
            }
            String[] roadPills = {"IRQ-CHIP (<<)", "GLITCH (>>)", "I/O-TRUCK (<<)"};
            for (int row = ROW_ROAD_TOP; row <= ROW_ROAD_BOT; row++) {
                int ry = TOP_OFFSET_Y + row * ROW_H;
                renderLaneFloorTag(g2, 8, ry + 14, roadPills[row - ROW_ROAD_TOP],
                        new Color(38, 18, 48), RetroTheme.ACCENT_PINK);
            }
        }

        private void renderStickyPageTableStrip(Graphics2D g2, int yMed) {
            g2.setFont(FONT_BOLD_11);
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("MMU PAGE TABLE:", 20, yMed + 30);

            int startX = 145;
            int curX = startX;
            g2.setFont(FONT_BOLD_12);
            FontMetrics fm0 = g2.getFontMetrics();
            int total = 0;
            for (Map.Entry<Integer, Integer> e : pageTable.entrySet()) {
                total += fm0.stringWidth(String.format("[P%d->F%d]", e.getKey(), e.getValue())) + 14;
            }
            boolean cut = startX + total > VIRTUAL_W - 80;
            int limitX = cut ? VIRTUAL_W - 240 : VIRTUAL_W - 80;

            for (Map.Entry<Integer, Integer> e : pageTable.entrySet()) {
                int p = e.getKey();
                int f = e.getValue();
                boolean isActive = (currentQuery != null && !currentQuery.isPageFault && currentQuery.pageNumber == p);

                String text = String.format("[P%d->F%d]", p, f);
                int tw = fm0.stringWidth(text);
                if (curX + tw > limitX) break;

                if (isActive) {
                    g2.setColor(new Color(80, 65, 10));
                    g2.fillRoundRect(curX - 4, yMed + 12, tw + 8, 24, 6, 6);
                    g2.setColor(RetroTheme.ACCENT_YELLOW);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(curX - 4, yMed + 12, tw + 8, 24, 6, 6);
                    g2.drawString(text, curX, yMed + 29);
                } else {
                    g2.setColor(RetroTheme.TEXT_WHITE);
                    g2.drawString(text, curX, yMed + 29);
                }

                curX += tw + 14;
            }
            if (cut && currentQuery != null && !currentQuery.isPageFault) {
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawString(String.format("... NOW: [P%d->F%d]", currentQuery.pageNumber, currentQuery.targetFrame),
                        VIRTUAL_W - 230, yMed + 29);
            }
        }

        private void renderFrameBays(Graphics2D g2) {
            float pulse = (float) (Math.sin(simTime * 6) * 0.5 + 0.5);

            for (FrameBay bay : frameBays) {
                boolean isTarget = (currentQuery != null &&
                        ((currentQuery.isPageFault && bay.isFaultChute) ||
                         (!currentQuery.isPageFault && bay.frameNumber == currentQuery.targetFrame)));
                boolean playerHere = !bay.isFaultChute
                        && bay.getBounds().contains((int) playerX + 16, bay.y + bay.height / 2);

                if (bay.isFaultChute) {
                    g2.setColor(new Color(45, 18, 32));
                    g2.fillRoundRect(bay.x, bay.y, bay.width, bay.height, 6, 6);

                    Color border = isTarget ? new Color(255, 42, 141, (int) (180 + pulse * 75)) : new Color(130, 40, 70);
                    g2.setColor(border);
                    g2.setStroke(new BasicStroke(isTarget ? 2.5f : 1.2f));
                    g2.drawRoundRect(bay.x, bay.y, bay.width, bay.height, 6, 6);

                    g2.setFont(FONT_BOLD_10);
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    g2.drawString("DISK SWAP", bay.x + 18, bay.y + 18);
                    g2.setFont(FONT_PLAIN_09);
                    g2.setColor(Color.WHITE);
                    g2.drawString("[PAGE FAULT]", bay.x + 14, bay.y + 32);
                } else {
                    g2.setColor(new Color(18, 32, 44));
                    g2.fillRoundRect(bay.x, bay.y, bay.width, bay.height, 6, 6);

                    Color border = isTarget ? new Color(57, 255, 20, (int) (180 + pulse * 75))
                            : (playerHere ? Color.WHITE : new Color(45, 80, 110));
                    g2.setColor(border);
                    g2.setStroke(new BasicStroke(playerHere || isTarget ? 2.5f : 1.2f));
                    g2.drawRoundRect(bay.x, bay.y, bay.width, bay.height, 6, 6);

                    g2.setFont(FONT_BOLD_11);
                    g2.setColor(isTarget ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_CYAN);
                    g2.drawString(String.format("FRAME %d", bay.frameNumber), bay.x + 14, bay.y + 18);

                    g2.setFont(FONT_BOLD_10);
                    g2.setColor(RetroTheme.TEXT_DIM);
                    g2.drawString(String.format("Base: %d B", bay.baseAddress), bay.x + 14, bay.y + 32);
                    if (playerHere) {
                        g2.setColor(Color.WHITE);
                        g2.fillPolygon(new int[]{bay.x + bay.width / 2 - 5, bay.x + bay.width / 2 + 5, bay.x + bay.width / 2},
                                new int[]{bay.y - 5, bay.y - 5, bay.y + 1}, 3);
                    }
                }
            }
        }

        private void renderGuideLine(Graphics2D g2) {
            if (currentQuery == null) return;
            FrameBay target = null;
            for (FrameBay bay : frameBays) {
                if (currentQuery.isPageFault && bay.isFaultChute) { target = bay; break; }
                if (!currentQuery.isPageFault && bay.frameNumber == currentQuery.targetFrame) { target = bay; break; }
            }
            if (target != null) {
                // High-visibility bright neon guide line
                int targetCenterX = target.x + target.width / 2;
                float distFactor = Math.max(0.4f, 1.0f - (playerRow / (float) ROW_START));
                float pulse = (float) (Math.sin(simTime * (6 + distFactor * 6)) * 0.5 + 0.5);
                int alpha = (int) (150 + pulse * 105);

                g2.setColor(new Color(57, 255, 20, alpha));
                Stroke old = g2.getStroke();
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6, 4}, 0));
                g2.drawLine(targetCenterX, target.y + target.height, targetCenterX, getPlayerPixelY());
                g2.setStroke(old);
                // Bright landing marker at the base of the target guide, above the hazards.
                int markerY = target.y + target.height + 3;
                g2.setColor(new Color(57, 255, 20, 90));
                g2.fillOval(targetCenterX - 9, markerY - 9, 18, 18);
                g2.setColor(Color.WHITE);
                g2.fillOval(targetCenterX - 4, markerY - 4, 8, 8);
                g2.setColor(RetroTheme.ACCENT_GREEN);
                g2.drawOval(targetCenterX - 9, markerY - 9, 18, 18);
            }
        }

        private void renderBusBuffers(Graphics2D g2) {
            for (MovingEntity b : busBuffers) {
                Rectangle r = b.getBounds();
                g2.setColor(b.color);
                g2.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
                g2.setColor(new Color(100, 190, 220));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);

                g2.setColor(new Color(255, 255, 255, 180));
                g2.setFont(FONT_BOLD_10);
                g2.drawString(b.label, r.x + 12, r.y + 22);
            }
        }

        private void renderRoadHazards(Graphics2D g2) {
            for (MovingEntity h : roadHazards) {
                Rectangle r = h.getBounds();
                if (h.hazardType == 0 && irqChipSprite != null) {
                    g2.drawImage(irqChipSprite, r.x, r.y, null);
                } else if (h.hazardType == 1 && glitchHazardSprite != null) {
                    g2.drawImage(glitchHazardSprite, r.x, r.y, null);
                } else if (h.hazardType == 2 && truckHazardSprite != null) {
                    g2.drawImage(truckHazardSprite, r.x, r.y, null);
                } else {
                    g2.setColor(h.color);
                    g2.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
                }

            }
        }

        private void renderPlayer(Graphics2D g2) {
            int py = getPlayerPixelY();
            int px = (int) playerX;

            if (frogSprite != null) {
                g2.drawImage(frogSprite, px, py, null);
            }

            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawRoundRect(px - 2, py - 2, 36, 36, 6, 6);

            if (currentQuery != null) {
                g2.setColor(new Color(12, 10, 24, 210));
                g2.fillRoundRect(px - 14, py - 18, 60, 14, 4, 4);
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawRoundRect(px - 14, py - 18, 60, 14, 4, 4);

                g2.setFont(FONT_BOLD_09);
                g2.setColor(Color.WHITE);
                String tag = (playerRow < ROW_MEDIAN && !currentQuery.isPageFault)
                        ? String.format("F%d : %d", currentQuery.targetFrame, currentQuery.offset)
                        : String.format("P%d : %d", currentQuery.pageNumber, currentQuery.offset);
                g2.drawString(tag, px - 10, py - 7);
            }
        }

        private void renderFeedbackTicker(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(14, 18, 28, 230));
            g2.fillRoundRect(w / 2 - 280, h - 38, 560, 26, 6, 6);
            g2.setColor(bannerColor);
            g2.drawRoundRect(w / 2 - 280, h - 38, 560, 26, 6, 6);

            g2.setFont(FONT_BOLD_11);
            FontMetrics fm = g2.getFontMetrics();
            int tx = w / 2 - fm.stringWidth(feedbackBanner) / 2;
            g2.drawString(feedbackBanner, tx, h - 21);
        }

        private void renderDiskSwapLoading(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(10, 8, 20, 210));
            g2.fillRect(0, 0, w, h);

            int boxW = 440;
            int boxH = 140;
            int bx = (w - boxW) / 2;
            int by = (h - boxH) / 2;

            g2.setColor(new Color(24, 16, 36));
            g2.fillRoundRect(bx, by, boxW, boxH, 10, 10);
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawRoundRect(bx, by, boxW, boxH, 10, 10);

            g2.setFont(FONT_BOLD_14);
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("[*] DISK I/O: SWAPPING PAGE TO RAM...", bx + 30, by + 40);

            int barW = boxW - 60;
            int barH = 18;
            g2.setColor(new Color(12, 10, 20));
            g2.fillRoundRect(bx + 30, by + 65, barW, barH, 4, 4);

            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.fillRoundRect(bx + 30, by + 65, (int) (barW * diskSwapProgress), barH, 4, 4);
            g2.drawRoundRect(bx + 30, by + 65, barW, barH, 4, 4);

            g2.setFont(FONT_PLAIN_10);
            g2.setColor(Color.WHITE);
            g2.drawString(String.format("Secondary Storage Page-in: %d%%", (int) (diskSwapProgress * 100)), bx + 30, by + 105);
        }

        private void renderTutorialOverlay(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(8, 6, 18, 220));
            g2.fillRect(0, 0, w, h);

            int boxW = 560;
            int boxH = 270;
            int bx = (w - boxW) / 2;
            int by = (h - boxH) / 2;

            g2.setColor(new Color(20, 28, 40));
            g2.fillRoundRect(bx, by, boxW, boxH, 12, 12);
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(bx, by, boxW, boxH, 12, 12);

            g2.setFont(FONT_BOLD_16);
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString("HOW TO PLAY // 4-STEP MMU DISPATCH", bx + 30, by + 36);

            g2.setFont(FONT_PLAIN_12);
            g2.setColor(RetroTheme.TEXT_WHITE);
            g2.drawString("1. LOOK AT THE HUD: Read Query Address -> Target Frame", bx + 30, by + 75);
            g2.drawString("2. CONSTANT OFFSET: The Offset stays identical in RAM!", bx + 30, by + 105);
            g2.drawString("3. AVOID HAZARDS: Hop over IRQ/Glitch hazards, ride Bus Buffers", bx + 30, by + 135);
            g2.drawString("4. PAGE FAULT TRAP: Route unmapped pages to [DISK SWAP]", bx + 30, by + 165);

            g2.setFont(FONT_BOLD_13);
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("PRESS [SPACE] OR AN ARROW / WASD KEY TO START!", bx + 30, by + 225);
        }

        private void renderRoundSummary(Graphics2D g2, int w, int h, boolean victory) {
            g2.setColor(new Color(10, 8, 20, 240));
            g2.fillRect(0, 0, w, h);

            int boxW = 620;
            int boxH = 430;
            int bx = (w - boxW) / 2;
            int by = (h - boxH) / 2;

            g2.setColor(new Color(20, 18, 38));
            g2.fillRoundRect(bx, by, boxW, boxH, 12, 12);
            g2.setColor(victory ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_PINK);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(bx, by, boxW, boxH, 12, 12);

            // Title
            g2.setFont(FONT_BOLD_18);
            g2.setColor(victory ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_PINK);
            String title = victory ? "[*] MISSION ACCOMPLISHED: MMU CERTIFIED [*]" : "[!] KERNEL PANIC: MEMORY CORRUPTED [!]";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, bx + (boxW - fm.stringWidth(title)) / 2, by + 30);

            // Teaching Moment: Prominent failed query & correct answer
            if (!victory && currentQuery != null) {
                g2.setColor(new Color(40, 18, 30));
                g2.fillRoundRect(bx + 20, by + 46, boxW - 40, 82, 6, 6);
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.drawRoundRect(bx + 20, by + 46, boxW - 40, 82, 6, 6);

                g2.setFont(FONT_BOLD_11);
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                int expectedPhys = (!currentQuery.isPageFault) ? ((currentQuery.targetFrame * pageSize) + currentQuery.offset) : -1;
                String targetStr = currentQuery.isPageFault ? "DISK SWAP (PAGE FAULT)" : String.format("FRAME %d (%d B)", currentQuery.targetFrame, expectedPhys);
                g2.drawString(String.format("FAILED QUERY: L-Addr %d (Page %d, Offset %d B)", currentQuery.logicalAddr, currentQuery.pageNumber, currentQuery.offset), bx + 28, by + 64);

                g2.setColor(RetroTheme.ACCENT_GREEN);
                g2.drawString("CORRECT TARGET: " + targetStr, bx + 28, by + 82);

                g2.setFont(FONT_PLAIN_10);
                g2.setColor(RetroTheme.TEXT_WHITE);
                String youGot = currentQuery.playerDestination.isEmpty() ? "COLLIDED WITH HAZARD" : currentQuery.playerDestination;
                String label = routed(youGot) ? "YOU ROUTED TO: " : "YOU LOST TO: ";
                g2.drawString(label + youGot, bx + 28, by + 100);
                g2.setColor(RetroTheme.ACCENT_PINK);
                String cause = currentQuery.failureReason.isEmpty()
                        ? "Missed target dock or hit hazard." : currentQuery.failureReason;
                g2.drawString("CAUSE: " + cause, bx + 28, by + 116);
            }

            // Translation Recap List with KaTeX / Consistent Colors
            int startListY = victory ? (by + 60) : (by + 146);
            g2.setFont(FONT_BOLD_11);
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("TRANSLATION ATTEMPTS RECAP:", bx + 20, startListY);

            int ry = startListY + 16;
            g2.setFont(FONT_PLAIN_10);
            int maxRows = victory ? 6 : 5;
            int firstAttempt = Math.max(0, recapHistory.size() - maxRows);
            for (int i = firstAttempt; i < recapHistory.size(); i++) {
                renderRecapAttempt(g2, recapHistory.get(i), bx + 20, ry);
                ry += 18;
            }

            // Accurate Score & Rank Breakdown
            int statY = Math.max(ry + 24, by + 250);
            g2.setFont(FONT_BOLD_12);
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString(String.format("SCORE: %05d PTS  (Completed Deliveries: %d / %d)", score, completedQueries, totalTargetQueries), bx + 20, statY);

            String grade;
            if (completedQueries == 0) {
                grade = "RANK: F (KERNEL PANIC - 0 DELIVERIES)";
                g2.setColor(RetroTheme.ACCENT_PINK);
            } else if (completedQueries >= 5) {
                grade = "RANK: S (KERNEL ARCHITECT)";
                g2.setColor(RetroTheme.ACCENT_GREEN);
            } else if (completedQueries >= 3) {
                grade = "RANK: A (MMU DISPATCHER)";
                g2.setColor(RetroTheme.ACCENT_CYAN);
            } else {
                grade = "RANK: B (MEMORY APPRENTICE)";
                g2.setColor(RetroTheme.ACCENT_YELLOW);
            }
            g2.drawString(grade, bx + 20, statY + 22);

            g2.setFont(FONT_PLAIN_11);
            g2.setColor(Color.WHITE);
            g2.drawString(String.format("Delivered: %d / %d  |  Page Size: %d Bytes  |  Sim Time: %.1fs",
                    completedQueries, totalTargetQueries, pageSize, simTime), bx + 20, statY + 44);

            // Replay Prompt
            g2.setFont(FONT_BOLD_13);
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            String prompt = "[R] PLAY AGAIN      |      [ESC] RETURN TO OS WORKBENCH";
            FontMetrics fm3 = g2.getFontMetrics();
            g2.drawString(prompt, bx + (boxW - fm3.stringWidth(prompt)) / 2, by + boxH - 30);
        }

        private void renderRecapAttempt(Graphics2D g2, Attempt attempt, int x, int y) {
            FontMetrics fm = g2.getFontMetrics();
            int dx = x;
            g2.setColor(RetroTheme.TEXT_DIM);
            String address = String.format("L-Addr %-5d | Page ", attempt.addr);
            g2.drawString(address, dx, y);
            dx += fm.stringWidth(address);
            dx = drawRecapToken(g2, Integer.toString(attempt.page), RetroTheme.ACCENT_CYAN, dx, y, fm);
            String offset = String.format(" | Off %-3d", attempt.offset);
            dx = drawRecapToken(g2, offset, RetroTheme.ACCENT_YELLOW, dx, y, fm);

            if (attempt.count > 1) {
                dx = drawRecapToken(g2, " x" + attempt.count, RetroTheme.TEXT_DIM, dx, y, fm);
            }

            if (!attempt.ok && !routed(attempt.dest)) {
                dx = drawRecapToken(g2, " -> LOST: " + attempt.dest + " ", RetroTheme.ACCENT_PINK, dx, y, fm);
            } else if (attempt.pageFault) {
                dx = drawRecapToken(g2, " -> PAGE FAULT / DISK SWAP ", RetroTheme.ACCENT_PINK, dx, y, fm);
            } else {
                dx = drawRecapToken(g2, " -> Frame ", RetroTheme.TEXT_DIM, dx, y, fm);
                dx = drawRecapToken(g2, Integer.toString(attempt.targetFrame), RetroTheme.ACCENT_GREEN, dx, y, fm);
                int phys = attempt.targetFrame * pageSize + attempt.offset;
                dx = drawRecapToken(g2, String.format(" [Phys: %d B] ", phys), RetroTheme.TEXT_DIM, dx, y, fm);
            }
            String status = attempt.pageFault && attempt.ok
                    ? "[HANDLED]" : (attempt.ok ? "[SUCCESS]" : "[MISSED]");
            drawRecapToken(g2, status, attempt.ok ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_PINK, dx, y, fm);
        }

        private int drawRecapToken(Graphics2D g2, String text, Color color, int x, int y, FontMetrics fm) {
            g2.setColor(color);
            g2.drawString(text, x, y);
            return x + fm.stringWidth(text);
        }
    }
}
