package arcadeos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * MISSION 08 // OVERCOOKED RETRO KITCHEN ARCADE (PERFECTED 2D EDITION)
 *
 * Visual & Gameplay Fixes:
 * - Fixed Chef Y-boundary: Chef is 100% visible, NEVER buried under the Service Pass counter!
 * - Fixed Stove Displays: Dedicated Digital LED status plate above each stove without text clipping or knob overlap!
 * - Fixed Overhead Lamps: Each lamp hangs directly above its matching stove station (1-to-1 alignment)!
 * - Fixed Header: Clean title & controls that never truncate against header buttons!
 * - Kitchen Environment: Center island cutting board & ingredient crates eliminate the middle floor void!
 * - Plated Dishes Alignment: Delivered plates align directly with their order's column on the Service Pass!
 */
public class OvercookedKitchenDialog extends JDialog {

    // Job Cooking & Multiprogramming Dispatch States
    public static final int STATE_PENDING = 0;       // Waiting for arrival time (simTime < at)
    public static final int STATE_ARRIVED = 1;       // Arrived at simTime >= at; waiting for Chef to admit onto stove!
    public static final int STATE_COOKING = 2;       // Chef admitted! Sizzling on stove, sharing CPU (1/nj)
    public static final int STATE_READY = 3;         // Done cooking (timeLeft <= 0)! Burn countdown ticking!
    public static final int STATE_OVERCOOKED = 4;    // Burned to ashes! Fire hazard!
    public static final int STATE_SERVED = 5;        // Picked up / successfully served to pass counter!

    public static class OrderItem {
        public String name;
        public String dishName;
        public int dishType;
        public double at;
        public double bt;
        public double timeLeft;
        public int state;
        public float stoveX;
        public float cookProgress;
        public float burnTimer;
        public float burnMax;
        public boolean isDelivered;
        public float sizzlePhase;
        public int stoveIndex = 0;
        public float currentCardX = -1f;

        public OrderItem(String name, String dishName, int dishType, double at, double bt) {
            this.name = name;
            this.dishName = dishName;
            this.dishType = dishType;
            this.at = at;
            this.bt = bt;
            this.timeLeft = bt;
            this.state = STATE_PENDING;
            this.cookProgress = 0f;
            this.burnMax = 5.2f;
            this.burnTimer = this.burnMax;
            this.isDelivered = false;
            this.sizzlePhase = (float)(Math.random() * Math.PI * 2);
        }
    }

    public static class FloatingEffect {
        public float x, y;
        public String text;
        public Color color;
        public int life;
        public int maxLife;

        public FloatingEffect(float x, float y, String text, Color color, int life) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
            this.life = life;
            this.maxLife = life;
        }
    }

    public static class KitchenParticle {
        public float x, y, vx, vy;
        public Color color;
        public int life;
        public int maxLife;
        public float size;
        public boolean isFire;

        public KitchenParticle(float x, float y, float vx, float vy, Color color, int life, float size, boolean isFire) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.life = life;
            this.maxLife = life;
            this.size = size;
            this.isFire = isFire;
        }
    }

    public static class DashGhost {
        public float x, y;
        public boolean facingRight;
        public int heldDishType;
        public int alpha;

        public DashGhost(float x, float y, boolean facingRight, int heldDishType) {
            this.x = x;
            this.y = y;
            this.facingRight = facingRight;
            this.heldDishType = heldDishType;
            this.alpha = 140;
        }
    }

    private final List<OrderItem> orders = new ArrayList<>();
    private final List<FloatingEffect> floaters = new ArrayList<>();
    private final List<KitchenParticle> particles = new ArrayList<>();
    private final List<DashGhost> dashGhosts = new ArrayList<>();
    private final List<String> eventLog = new ArrayList<>();

    // 2D Player State
    private float chefX = 320f;
    private float chefY = 380f; // Safely in middle of floor
    private float chefVx = 0f;
    private float chefVy = 0f;
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean movingUp = false;
    private boolean movingDown = false;
    private boolean isDashing = false;
    private boolean facingRight = true;
    private boolean isStirring = false;
    private int animTick = 0;

    // Carrying state
    private int heldDishType = -1;
    private String heldDishName = null;
    private String heldJobName = null;
    private boolean isHoldingBurntPan = false;

    // Juice & FX
    private int bellRingAnim = 0;
    private int screenFlashTicks = 0;
    private Color screenFlashColor = new Color(0, 255, 120, 50);
    private int screenShakeTicks = 0;

    // Game Metrics
    private int lives = 3;
    private int score = 0;
    private int combo = 0;
    private double simTime = 7.40;
    private double simStartTime = 7.40;
    private boolean isGameOver = false;
    private boolean isVictory = false;
    private boolean soundEnabled = true;

    private static final double SIM_SPEED = 0.05 / 60.0;

    private Timer gameTimer;
    private KitchenGameCanvas canvas;

    public OvercookedKitchenDialog(JFrame owner, List<MultiprogrammingPanel.JobInput> inputJobs) {
        super(owner, "MISSION 08 // OVERCOOKED RETRO KITCHEN ARCADE", false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);

        Dimension targetDim = RetroTheme.getClampedDialogSize(owner, 1080, 760);
        setSize(targetDim);
        setMinimumSize(new Dimension(880, 620));
        setLocationRelativeTo(owner);

        initOrders(inputJobs);
        initUI();
        initKeyboard();
        startGameLoop();
    }

    private void initOrders(List<MultiprogrammingPanel.JobInput> inputJobs) {
        orders.clear();
        if (inputJobs != null && !inputJobs.isEmpty()) {
            for (MultiprogrammingPanel.JobInput j : inputJobs) {
                String dName = MultiprogrammingPanel.DISH_NAMES[j.dishType % 6];
                orders.add(new OrderItem(j.name, dName, j.dishType, j.at, j.bt));
            }
        } else {
            // Classroom Problem Set (Class 7.4)
            orders.add(new OrderItem("J1", "Ramen", 2, 7.4, 0.4));
            orders.add(new OrderItem("J2", "Steak", 4, 7.9, 0.6));
            orders.add(new OrderItem("J3", "Pizza", 1, 8.1, 0.3));
            orders.add(new OrderItem("J4", "Burger", 0, 8.3, 0.2));
            orders.add(new OrderItem("J5", "Sushi", 3, 8.4, 0.1));
        }

        double minAt = Double.MAX_VALUE;
        for (OrderItem o : orders) {
            if (o.at < minAt) minAt = o.at;
        }
        simStartTime = minAt;
        simTime = simStartTime;

        int stationCount = orders.size();
        for (int i = 0; i < stationCount; i++) {
            OrderItem o = orders.get(i);
            o.stoveIndex = i;
            o.currentCardX = -1f;
        }
        updateStationLayout(1080);

        eventLog.clear();
        eventLog.add(String.format("[T=%.2f] KITCHEN RANGE ONLINE // %d MULTIPROGRAMMING STOVES", simTime, stationCount));
    }

    private void updateStationLayout(int w) {
        int n = orders.size();
        if (n <= 0) return;
        if (n == 1) {
            orders.get(0).stoveIndex = 0;
            orders.get(0).stoveX = w / 2f - 28f;
            return;
        }

        float maxSpacing;
        if (n <= 3) maxSpacing = 185f;
        else if (n == 4) maxSpacing = 175f;
        else if (n == 5) maxSpacing = 160f;
        else if (n == 6) maxSpacing = 142f;
        else if (n == 7) maxSpacing = 122f;
        else maxSpacing = 106f;

        // Safety margin from left vegetable crate (ends at 66) and right chopping block (starts at w - 70)
        float minSideMargin = (n >= 6) ? 75f : 100f;
        float usableW = w - (minSideMargin * 2f);
        float spacing = Math.min(maxSpacing, usableW / (n - 1));
        spacing = Math.max(88f, spacing); // minimum safe spacing so stoves don't collide

        float totalSpan = spacing * (n - 1);
        float startX = (w - totalSpan) / 2f - 28f;

        for (int i = 0; i < n; i++) {
            OrderItem o = orders.get(i);
            o.stoveIndex = i;
            o.stoveX = startX + i * spacing;
        }
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(10, 6, 18));

        // Top Retro Arcade Header Bar
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(true);
        header.setBackground(new Color(16, 10, 28));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(75, 45, 115)),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftHeader.setOpaque(false);

        JLabel lblTitle = new JLabel("OVERCOOKED KITCHEN");
        lblTitle.setFont(new Font("Consolas", Font.BOLD, 13));
        lblTitle.setForeground(RetroTheme.ACCENT_YELLOW);
        leftHeader.add(lblTitle);
        header.add(leftHeader, BorderLayout.WEST);

        JLabel lblTag = new JLabel("[ WASD: MOVE  |  SHIFT: DASH  |  SPACE: ADMIT / PICK UP / SERVE ]");
        lblTag.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblTag.setForeground(new Color(190, 180, 225));
        lblTag.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(lblTag, BorderLayout.CENTER);

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHeader.setOpaque(false);

        JButton btnSound = RetroTheme.createRetroButton("SOUND: ON", new Color(28, 18, 46), RetroTheme.ACCENT_CYAN);
        btnSound.setFont(new Font("Consolas", Font.BOLD, 10));
        btnSound.setFocusable(false);
        btnSound.addActionListener(e -> {
            soundEnabled = !soundEnabled;
            btnSound.setText(soundEnabled ? "SOUND: ON" : "SOUND: MUTED");
            btnSound.setForeground(soundEnabled ? RetroTheme.ACCENT_CYAN : RetroTheme.TEXT_DIM);
        });
        rightHeader.add(btnSound);

        JButton btnRestart = RetroTheme.createRetroButton("RESTART (R)", new Color(48, 18, 36), RetroTheme.ACCENT_PINK);
        btnRestart.setFont(new Font("Consolas", Font.BOLD, 10));
        btnRestart.setFocusable(false);
        btnRestart.addActionListener(e -> restartGame());
        rightHeader.add(btnRestart);

        JButton btnExit = RetroTheme.createRetroButton("EXIT (ESC)", new Color(34, 24, 52), Color.WHITE);
        btnExit.setFont(new Font("Consolas", Font.BOLD, 10));
        btnExit.setFocusable(false);
        btnExit.addActionListener(e -> dispose());
        rightHeader.add(btnExit);

        header.add(rightHeader, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        canvas = new KitchenGameCanvas();
        canvas.setFocusable(true);
        root.add(canvas, BorderLayout.CENTER);

        setContentPane(root);
    }

    private void initKeyboard() {
        InputMap im = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = canvas.getActionMap();

        // 2D Movement
        bindPressRelease(im, am, KeyEvent.VK_LEFT, () -> { movingLeft = true; facingRight = false; }, () -> movingLeft = false);
        bindPressRelease(im, am, KeyEvent.VK_A, () -> { movingLeft = true; facingRight = false; }, () -> movingLeft = false);

        bindPressRelease(im, am, KeyEvent.VK_RIGHT, () -> { movingRight = true; facingRight = true; }, () -> movingRight = false);
        bindPressRelease(im, am, KeyEvent.VK_D, () -> { movingRight = true; facingRight = true; }, () -> movingRight = false);

        bindPressRelease(im, am, KeyEvent.VK_UP, () -> movingUp = true, () -> movingUp = false);
        bindPressRelease(im, am, KeyEvent.VK_W, () -> movingUp = true, () -> movingUp = false);

        bindPressRelease(im, am, KeyEvent.VK_DOWN, () -> movingDown = true, () -> movingDown = false);
        bindPressRelease(im, am, KeyEvent.VK_S, () -> movingDown = true, () -> movingDown = false);

        // Sprint Dash
        bindPressRelease(im, am, KeyEvent.VK_SHIFT, () -> isDashing = true, () -> isDashing = false);

        // Interact
        bindAction(im, am, KeyEvent.VK_SPACE, "INTERACT_SPACE", this::handleInteractAction);
        bindAction(im, am, KeyEvent.VK_ENTER, "INTERACT_ENTER", this::handleInteractAction);

        // Stir boost
        bindPressRelease(im, am, KeyEvent.VK_J, () -> { isStirring = true; handleStirAction(); }, () -> isStirring = false);

        // R = Restart, ESC = Exit
        bindAction(im, am, KeyEvent.VK_R, "KEY_RESTART", this::restartGame);
        bindAction(im, am, KeyEvent.VK_ESCAPE, "KEY_ESC", this::dispose);

        // Mouse click navigation
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                canvas.requestFocusInWindow();
                chefX = Math.max(70f, Math.min(canvas.getWidth() - 85f, e.getX() - 28f));
                chefY = Math.max(295f, Math.min(440f, e.getY() - 28f));
                handleInteractAction();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                canvas.requestFocusInWindow();
            }
        });
    }

    private void bindAction(InputMap im, ActionMap am, int key, String name, Runnable r) {
        im.put(KeyStroke.getKeyStroke(key, 0), name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        });
    }

    private void bindPressRelease(InputMap im, ActionMap am, int key, Runnable onPress, Runnable onRelease) {
        String pressName = "press-" + key;
        String releaseName = "release-" + key;

        im.put(KeyStroke.getKeyStroke(key, 0, false), pressName);
        im.put(KeyStroke.getKeyStroke(key, 0, true), releaseName);

        am.put(pressName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onPress.run();
            }
        });
        am.put(releaseName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRelease.run();
            }
        });
    }

    private void playSfx(int freq, int ms, float vol) {
        if (!soundEnabled) return;
        try {
            ArchivesPanel.playTone(freq, ms, vol);
        } catch (Exception ignored) {}
    }

    private void startGameLoop() {
        gameTimer = new Timer(16, e -> {
            if (!isShowing()) return;
            animTick++;

            if (!isGameOver && !isVictory) {
                updateGameLogic();
            }

            updateEffects();
            canvas.repaint();
        });
        gameTimer.start();
    }

    private void updateGameLogic() {
        simTime += SIM_SPEED;

        // 2D Movement Physics
        float baseSpeed = isDashing ? 9.2f : 6.2f;
        float targetVx = 0f;
        float targetVy = 0f;

        if (movingLeft) targetVx -= baseSpeed;
        if (movingRight) targetVx += baseSpeed;
        if (movingUp) targetVy -= baseSpeed;
        if (movingDown) targetVy += baseSpeed;

        if (targetVx != 0 && targetVy != 0) {
            targetVx *= 0.7071f;
            targetVy *= 0.7071f;
        }

        chefVx = chefVx * 0.60f + targetVx * 0.40f;
        chefVy = chefVy * 0.60f + targetVy * 0.40f;

        chefX += chefVx;
        chefY += chefVy;

        // STRICT CHEF BOUNDARIES: Never enter stove counters, NEVER buried under pass counter!
        // Top counter ends at y=305, pass counter begins at y=505.
        // Chef height is 54px. Feet are at chefY + 50.
        // chefY min = 295 (head near stove edge), chefY max = 442 (feet at 492, well above y=505!).
        chefX = Math.max(70f, Math.min(canvas.getWidth() - 85f, chefX));
        chefY = Math.max(295f, Math.min(442f, chefY));

        // Sprint Dash Ghosting Trail & Speed Sparks
        boolean isMoving = (Math.abs(chefVx) > 0.5f || Math.abs(chefVy) > 0.5f);
        if (isDashing && isMoving && animTick % 3 == 0) {
            dashGhosts.add(new DashGhost(chefX, chefY, facingRight, heldDishType));
            particles.add(new KitchenParticle(
                    chefX + 28, chefY + 50,
                    (float)(Math.random() - 0.5) * 3f, (float)(Math.random() - 0.5) * 1.5f,
                    new Color(0, 240, 255, 200), 16, 4.5f, false));
        }

        // Fever / Fire Mode Particles when Combo >= 2
        if (combo >= 2 && animTick % 3 == 0) {
            particles.add(new KitchenParticle(
                    chefX + 16 + (float)(Math.random() * 26),
                    chefY + 4,
                    (float)(Math.random() - 0.5) * 1.2f, -1.8f,
                    new Color(255, 200, 40, 220), 18, 4.5f, true));
        }

        // Carrying steam wisps
        if (heldDishType != -1 && animTick % 3 == 0) {
            particles.add(new KitchenParticle(
                    chefX + 28 + (float)(Math.random() - 0.5) * 14,
                    chefY - 14,
                    (float)(Math.random() - 0.5) * 0.8f, -2.2f,
                    new Color(255, 255, 255, 190), 22, 5f, false));
        }

        // 1. ARRIVAL: Check if pending job reaches its arrival time (AT)
        for (OrderItem o : orders) {
            o.sizzlePhase += 0.2f;
            if (o.state == STATE_PENDING && simTime >= o.at - 1e-4) {
                o.state = STATE_ARRIVED;
                eventLog.add(String.format("[T=%.2f] ORDER ARRIVED: %s (%s) // WAITING ADMISSION", simTime, o.name, o.dishName));
                addFloating(o.stoveX + 32, 170, "[ORDER ARRIVED: " + o.dishName + "!]", RetroTheme.ACCENT_YELLOW);
                addStoveBurst(o.stoveX + 32, 275, RetroTheme.ACCENT_YELLOW, 12);
                playSfx(587, 80, 0.45f);
            }
        }

        // 2. ACTIVE CONCURRENCY (nj) - Count jobs actively cooking on stoves
        int nj = 0;
        for (OrderItem o : orders) {
            if (o.state == STATE_COOKING) nj++;
        }

        // 3. PROCESSOR SHARING: Equal 1.0 / nj share per active stove
        double cpuPct = (nj > 0) ? (1.0 / nj) : 0.0;
        double workDoneThisTick = SIM_SPEED * cpuPct;

        for (OrderItem o : orders) {
            if (o.state == STATE_COOKING) {
                o.timeLeft = Math.max(0.0, o.timeLeft - workDoneThisTick);
                o.cookProgress = (float) Math.max(0.0, Math.min(1.0, 1.0 - (o.timeLeft / o.bt)));

                // Steam & Sizzle particles
                if (animTick % 3 == 0) {
                    particles.add(new KitchenParticle(
                            o.stoveX + 32 + (float)(Math.random() - 0.5) * 20,
                            255,
                            (float)(Math.random() - 0.5) * 0.8f, -2.2f - (float) Math.random() * 1.2f,
                            new Color(90, 220, 255, 200), 22, 5.5f, false));
                }

                // 4. COOKING FINISHED (Burst Time exhausted under 1/nj processor sharing)!
                if (o.timeLeft <= 1e-5) {
                    o.timeLeft = 0.0;
                    o.cookProgress = 1.0f;
                    o.state = STATE_READY;
                    o.burnTimer = o.burnMax;
                    eventLog.add(String.format("[T=%.2f] %s (%s) COOKED! PICK UP BEFORE OVERCOOK!", simTime, o.name, o.dishName));
                    addFloating(o.stoveX + 32, 170, "[READY! PICK UP [SPACE]]", RetroTheme.ACCENT_GREEN);
                    addStoveBurst(o.stoveX + 32, 255, RetroTheme.ACCENT_GREEN, 18);
                    playSfx(880, 120, 0.55f);
                }

            } else if (o.state == STATE_READY) {
                // 5. OVERCOOKED BURN HAZARD
                o.burnTimer -= 0.016f;

                // Golden embers
                if (animTick % 3 == 0) {
                    particles.add(new KitchenParticle(
                            o.stoveX + 32 + (float)(Math.random() - 0.5) * 16,
                            255,
                            (float)(Math.random() - 0.5) * 1.4f, -2.5f,
                            new Color(255, 220, 70, 230), 18, 5f, true));
                }

                // Danger warning: Under 2.0s left!
                if (o.burnTimer <= 2.0f) {
                    if (animTick % 2 == 0) {
                        particles.add(new KitchenParticle(
                                o.stoveX + 32 + (float)(Math.random() - 0.5) * 16,
                                250,
                                (float)(Math.random() - 0.5) * 1.6f, -3.2f,
                                new Color(40, 25, 45, 240), 28, 8f, true));
                    }
                    if (animTick % 12 == 0) {
                        playSfx(330, 40, 0.45f);
                    }
                }

                // OVERCOOKED! Burned to charcoal!
                if (o.burnTimer <= 0f) {
                    o.state = STATE_OVERCOOKED;
                    lives--;
                    combo = 0;
                    screenShakeTicks = 20;
                    screenFlashTicks = 8;
                    screenFlashColor = new Color(255, 40, 60, 80);
                    eventLog.add(String.format("[T=%.2f] %s (%s) OVERCOOKED & BURNT! -1 LIFE", simTime, o.name, o.dishName));
                    addFloating(o.stoveX + 32, 170, "[OVERCOOKED! BURNT! -1 LIFE]", RetroTheme.ACCENT_PINK);
                    addStoveBurst(o.stoveX + 32, 260, RetroTheme.ACCENT_PINK, 28);
                    playSfx(140, 260, 0.75f);

                    if (lives <= 0) {
                        isGameOver = true;
                        playSfx(110, 500, 0.85f);
                    }
                }
            }
        }

        // 6. Check Victory
        boolean allDelivered = true;
        for (OrderItem o : orders) {
            if (!o.isDelivered) {
                allDelivered = false;
                break;
            }
        }
        if (allDelivered && !orders.isEmpty() && lives > 0) {
            isVictory = true;
            screenFlashTicks = 12;
            screenFlashColor = new Color(0, 255, 120, 80);
        }
    }

    private void handleInteractAction() {
        if (isGameOver || isVictory) return;

        // Check A: SERVICE PASS COUNTER (Bottom)
        // Chef standing near bottom counter
        if (chefY >= 420f && chefX >= 80f && chefX <= canvas.getWidth() - 170f) {
            if (heldDishType != -1) {
                combo++;
                int bonus = 200 + (combo * 60);
                score += bonus;
                bellRingAnim = 16;
                screenFlashTicks = 6;
                screenFlashColor = new Color(0, 255, 140, 70);

                for (OrderItem o : orders) {
                    if (o.name.equals(heldJobName)) {
                        o.isDelivered = true;
                        break;
                    }
                }

                eventLog.add(String.format("[T=%.2f] SERVED %s (%s) // +%d PTS [COMBO x%d]",
                        simTime, heldJobName, heldDishName, bonus, combo));
                addFloating(chefX + 28, chefY - 20, String.format("+%d PTS! [COMBO x%d]", bonus, combo), RetroTheme.ACCENT_GREEN);
                addStoveBurst(chefX + 28, chefY + 20, RetroTheme.ACCENT_GREEN, 24);

                playSfx(1046, 90, 0.60f);
                playSfx(1318, 150, 0.65f);

                heldDishType = -1;
                heldDishName = null;
                heldJobName = null;
                return;
            }
        }

        // Check B: TRASH DUMP (Bottom Right)
        if (chefY >= 420f && chefX >= canvas.getWidth() - 180f) {
            if (isHoldingBurntPan) {
                isHoldingBurntPan = false;
                addFloating(chefX + 20, chefY - 20, "[BURNT ASHES INCINERATED]", RetroTheme.TEXT_DIM);
                addStoveBurst(chefX + 20, chefY + 10, new Color(75, 65, 95), 14);
                playSfx(220, 80, 0.4f);
                return;
            }
        }

        // Check C: STOVE STATIONS (Top)
        OrderItem nearStove = findNearestStove(chefX, chefY);
        if (nearStove != null) {
            // 1. ADMIT ARRIVED JOB ONTO STOVE
            if (nearStove.state == STATE_ARRIVED && heldDishType == -1 && !isHoldingBurntPan) {
                nearStove.state = STATE_COOKING;
                int curNj = 0;
                for (OrderItem o : orders) if (o.state == STATE_COOKING) curNj++;
                eventLog.add(String.format("[T=%.2f] ADMITTED %s (%s) // STOVE ACTIVE [nj=%d]",
                        simTime, nearStove.name, nearStove.dishName, curNj));
                addFloating(nearStove.stoveX + 32, 170, String.format("[ADMITTED! nj=%d (%.1f%% CPU)]", curNj, (100.0 / curNj)), RetroTheme.ACCENT_CYAN);
                addStoveBurst(nearStove.stoveX + 32, 275, RetroTheme.ACCENT_CYAN, 14);
                playSfx(587, 80, 0.45f);
                return;

            // 2. JOB HAS NOT ARRIVED YET
            } else if (nearStove.state == STATE_PENDING && heldDishType == -1 && !isHoldingBurntPan) {
                addFloating(nearStove.stoveX + 32, 170, String.format("[NOT ARRIVED! ARRIVES AT T=%.1fs]", nearStove.at), RetroTheme.TEXT_DIM);
                playSfx(220, 50, 0.35f);
                return;

            // 3. JOB IS STILL COOKING (CANNOT SERVE RAW DISH!)
            } else if (nearStove.state == STATE_COOKING && heldDishType == -1 && !isHoldingBurntPan) {
                addFloating(nearStove.stoveX + 32, 170, String.format("[COOKING: %.2fs BT LEFT - CANNOT SERVE RAW!]", nearStove.timeLeft), RetroTheme.ACCENT_YELLOW);
                playSfx(370, 60, 0.35f);
                return;

            // 4. DONE COOKING: PICK UP HOT FINISHED DISH ONTO CERAMIC PLATE!
            } else if (nearStove.state == STATE_READY && heldDishType == -1 && !isHoldingBurntPan) {
                heldDishType = nearStove.dishType;
                heldDishName = nearStove.dishName;
                heldJobName = nearStove.name;
                nearStove.state = STATE_SERVED;

                addFloating(nearStove.stoveX + 32, 170, "[PICKED UP: " + nearStove.dishName + "! RUSH TO PASS!]", RetroTheme.ACCENT_GREEN);
                addStoveBurst(nearStove.stoveX + 32, 260, RetroTheme.ACCENT_GREEN, 14);
                playSfx(784, 80, 0.45f);
                eventLog.add(String.format("[T=%.2f] PICKED UP %s // RUSH TO SERVICE PASS!", simTime, nearStove.name));
                return;

            // 5. OVERCOOKED: PICK UP BURNT PAN TO CLEAR STOVE
            } else if (nearStove.state == STATE_OVERCOOKED && heldDishType == -1 && !isHoldingBurntPan) {
                isHoldingBurntPan = true;
                nearStove.state = STATE_SERVED;
                nearStove.isDelivered = true;

                addFloating(nearStove.stoveX + 32, 170, "[BURNT PAN ACQUIRED -> DUMP IN INCINERATOR!]", RetroTheme.ACCENT_PINK);
                addStoveBurst(nearStove.stoveX + 32, 260, new Color(55, 35, 45), 16);
                playSfx(180, 100, 0.45f);
                return;
            }
        }
    }

    private void handleStirAction() {
        if (isGameOver || isVictory || heldDishType != -1) return;
        OrderItem nearStove = findNearestStove(chefX, chefY);
        if (nearStove != null && nearStove.state == STATE_COOKING) {
            score += 2;
            addFloating(chefX + 28, chefY - 24, "STIRRING...", RetroTheme.ACCENT_YELLOW);
            addStoveBurst(nearStove.stoveX + 32, 260, RetroTheme.ACCENT_YELLOW, 5);
            playSfx(659, 30, 0.25f);
        }
    }

    private OrderItem findNearestStove(float x, float y) {
        OrderItem best = null;
        float bestDist = (orders.size() >= 6) ? 75f : 95f;
        for (OrderItem o : orders) {
            float dist = (float) Math.hypot((o.stoveX + 28) - (x + 28), 295 - (y + 28));
            if (dist < bestDist) {
                bestDist = dist;
                best = o;
            }
        }
        return best;
    }

    private void addFloating(float x, float y, String text, Color c) {
        floaters.removeIf(f -> Math.abs(f.x - x) < 60f && Math.abs(f.y - y) < 28f);
        floaters.add(new FloatingEffect(x, y, text, c, 55));
    }

    private void addStoveBurst(float x, float y, Color c, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            float spd = (float)(1.5 + Math.random() * 4.5);
            particles.add(new KitchenParticle(x, y, (float)(Math.cos(angle) * spd), (float)(Math.sin(angle) * spd) - 1.2f, c, 24, 6f, false));
        }
    }

    private void updateEffects() {
        if (screenShakeTicks > 0) screenShakeTicks--;
        if (screenFlashTicks > 0) screenFlashTicks--;
        if (bellRingAnim > 0) bellRingAnim--;

        dashGhosts.removeIf(g -> {
            g.alpha -= 16;
            return g.alpha <= 0;
        });

        floaters.removeIf(f -> {
            f.y -= 0.8f;
            f.life--;
            return f.life <= 0;
        });

        particles.removeIf(p -> {
            p.x += p.vx;
            p.y += p.vy;
            p.size += 0.12f;
            p.life--;
            return p.life <= 0;
        });
    }

    private void restartGame() {
        simTime = simStartTime;
        score = 0;
        combo = 0;
        lives = 3;
        isGameOver = false;
        isVictory = false;
        chefX = 320f;
        chefY = 380f;
        chefVx = 0f;
        chefVy = 0f;
        heldDishType = -1;
        heldDishName = null;
        heldJobName = null;
        isHoldingBurntPan = false;
        floaters.clear();
        particles.clear();
        dashGhosts.clear();
        eventLog.clear();
        eventLog.add(String.format("[T=%.2f] KITCHEN RANGE REBOOTED // 100%% CPU CAPACITY", simTime));

        for (OrderItem o : orders) {
            o.state = STATE_PENDING;
            o.timeLeft = o.bt;
            o.cookProgress = 0f;
            o.burnTimer = o.burnMax;
            o.isDelivered = false;
            o.currentCardX = -1f;
        }
        canvas.requestFocusInWindow();
    }

    @Override
    public void dispose() {
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop();
        }
        super.dispose();
    }

    // =========================================================================
    // RETRO 16-BIT BADASS KITCHEN CANVAS
    // =========================================================================
    private class KitchenGameCanvas extends JPanel {

        public KitchenGameCanvas() {
            setBackground(new Color(10, 6, 18));
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (screenShakeTicks > 0) {
                int ox = (int)((Math.random() - 0.5) * 12);
                int oy = (int)((Math.random() - 0.5) * 12);
                g2.translate(ox, oy);
            }

            int w = getWidth();
            int h = getHeight();

            // Dynamic responsive adjustment for all stoves and stations
            updateStationLayout(w);

            // 1. Atmosphere: Subway Brick Wall, Overhead Lamps, Polished Counter & 2D Floor
            paintKitchenEnvironment(g2, w, h);

            // 2. Top HUD Bar
            paintKitchenHUD(g2, w);

            // 3. Hanging Ticket Rail & Orders
            paintOrderRail(g2, w);

            // 4. Stoves, Sizzling Pots & Dedicated Digital LED Display Plates
            paintStoves(g2);

            // 5. Floor Island Elements: Prep table & Ingredient Crates (Eliminating the empty void!)
            paintFloorKitchenProps(g2, w);

            // 6. Dash Ghosts & Particles
            paintDashGhosts(g2);
            paintParticles(g2);

            // 7. The Player Chef (Rendered cleanly on the floor)
            paintChef(g2);

            // 8. Service Pass Counter & Trash Incinerator
            paintServicePassCounter(g2, w, h);

            // 9. Live PMU Oscilloscope CRT Terminal
            paintOscilloscopeTerminal(g2, w, h);

            // 10. Floating Notifications
            paintFloaters(g2);

            // 11. Screen Flash Juice
            if (screenFlashTicks > 0) {
                g2.setColor(screenFlashColor);
                g2.fillRect(0, 0, w, h);
            }

            // 12. Overlays
            if (isVictory) {
                paintVictoryScreen(g2, w, h);
            } else if (isGameOver) {
                paintGameOverScreen(g2, w, h);
            }

            g2.dispose();
        }

        private void paintKitchenEnvironment(Graphics2D g2, int w, int h) {
            // Upper wall: Deep cyber purple subway tile wall (y: 0..245)
            g2.setColor(new Color(18, 12, 30));
            g2.fillRect(0, 0, w, 245);

            // Retro Subway Tiles Pattern with Bevel
            g2.setColor(new Color(28, 18, 44));
            for (int y = 145; y < 245; y += 18) {
                int row = (y - 145) / 18;
                int xOffset = (row % 2 == 0) ? 0 : 18;
                for (int x = -18 + xOffset; x < w; x += 36) {
                    g2.drawRect(x, y, 36, 18);
                }
            }

            // Polished Copper Gas Pipe running across the kitchen wall (y: 208)
            g2.setColor(new Color(160, 95, 45));
            g2.fillRect(20, 208, w - 40, 5);
            g2.setColor(new Color(230, 150, 75));
            g2.fillRect(20, 209, w - 40, 2);

            // Pressure Gauges directly on gas line
            for (OrderItem o : orders) {
                int gx = (int) o.stoveX + 22;
                g2.setColor(new Color(210, 160, 60));
                g2.fillOval(gx, 204, 14, 14);
                g2.setColor(Color.WHITE);
                g2.fillOval(gx + 2, 206, 10, 10);
                g2.setColor(Color.RED);
                g2.drawLine(gx + 7, 211, gx + 10, 208);
            }

            // Top Cooking Counter Base (y: 245..315)
            GradientPaint counterGrad = new GradientPaint(
                    0, 245, new Color(58, 48, 80),
                    0, 315, new Color(34, 25, 50)
            );
            g2.setPaint(counterGrad);
            g2.fillRect(0, 245, w, 70);

            // Brushed Stainless Steel Chrome Edges
            g2.setColor(new Color(125, 110, 160));
            g2.fillRect(0, 245, w, 4);
            g2.setColor(new Color(85, 72, 115));
            g2.fillRect(0, 311, w, 4);

            // High-Gloss 2.5D Checkerboard Floor Arena (y: 315..505)
            int tileSize = 38;
            for (int y = 315; y < 505; y += tileSize) {
                for (int x = 0; x < w; x += tileSize) {
                    boolean alt = ((x / tileSize) + (y / tileSize)) % 2 == 0;
                    g2.setColor(alt ? new Color(26, 18, 44) : new Color(16, 10, 28));
                    g2.fillRect(x, y, tileSize, tileSize);
                    g2.setColor(new Color(36, 25, 56));
                    g2.drawRect(x, y, tileSize, tileSize);
                }
            }

            // Overhead Industrial Heat Lamps: Exactly 1-to-1 centered above EACH STOVE!
            for (OrderItem o : orders) {
                int lx = (int) o.stoveX + 28;

                // Cord hanging straight down from ticket rail to lamp shade
                g2.setColor(new Color(70, 58, 90));
                g2.fillRect(lx - 1, 148, 3, 34);

                // Brass Lamp Shade at y=182..194
                g2.setColor(new Color(230, 180, 50));
                g2.fillPolygon(new int[]{lx - 14, lx + 14, lx + 8, lx - 8}, new int[]{194, 194, 182, 182}, 4);
                g2.setColor(new Color(255, 230, 120));
                g2.fillOval(lx - 6, 192, 12, 5); // Bulb rim

                // Soft warm radial light cone shining down directly onto stove & station
                RadialGradientPaint lampLight = new RadialGradientPaint(
                        new Point2D.Float(lx, 260), 105f,
                        new float[]{0.0f, 0.6f, 1.0f},
                        new Color[]{new Color(255, 230, 130, 40), new Color(255, 180, 50, 12), new Color(0, 0, 0, 0)}
                );
                g2.setPaint(lampLight);
                g2.fillOval(lx - 95, 215, 190, 170);
            }
        }

        private void paintKitchenHUD(Graphics2D g2, int w) {
            int hudY = 10;

            int activeNj = 0;
            int arrivedOrders = 0;
            int waitingFuture = 0;
            for (OrderItem o : orders) {
                if (o.state == STATE_COOKING) activeNj++;
                else if (o.state == STATE_ARRIVED) arrivedOrders++;
                else if (o.state == STATE_PENDING) waitingFuture++;
            }
            double cpuPct = (activeNj > 0) ? (1.0 / activeNj) : 0.0;

            // Telemetry Pod
            int boxW = 390;
            g2.setColor(new Color(20, 14, 38, 240));
            g2.fillRoundRect(16, hudY, boxW, 40, 8, 8);
            g2.setColor(new Color(85, 60, 125));
            g2.drawRoundRect(16, hudY, boxW, 40, 8, 8);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString(String.format("MULTIPROGRAMMING nj = %d | ARRIVED: %d | IN LINE: %d", activeNj, arrivedOrders, waitingFuture), 26, hudY + 16);

            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            g2.setColor(Color.WHITE);
            g2.drawString(String.format("EQUAL CPU SLICE (1/nj): %.1f%% / STOVE  |  T: %.2fs", cpuPct * 100.0, simTime), 26, hudY + 31);

            // Center Callout / Combo Pod
            int centerW = 280;
            int centerX = (w - centerW) / 2;
            g2.setColor(new Color(25, 16, 44, 235));
            g2.fillRoundRect(centerX, hudY, centerW, 40, 8, 8);
            g2.setColor(combo > 1 ? RetroTheme.ACCENT_PINK : new Color(180, 85, 240, 180));
            g2.drawRoundRect(centerX, hudY, centerW, 40, 8, 8);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(combo > 1 ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_YELLOW);
            String comboText = (combo > 1) ? String.format("★ COMBO x%d FEVER MODE! ★", combo) : "PROCESSOR TIME-SHARING";
            g2.drawString(comboText, centerX + 18, hudY + 16);

            g2.setFont(new Font("Consolas", Font.PLAIN, 9));
            g2.setColor(new Color(215, 205, 245));
            g2.drawString("ORDERS ARRIVE AT AT -> ADMIT [SPACE] -> SERVE!", centerX + 10, hudY + 31);

            // Right Box: Score & Lives
            int rightBoxW = 260;
            int rightBoxX = w - rightBoxW - 16;
            g2.setColor(new Color(20, 14, 38, 240));
            g2.fillRoundRect(rightBoxX, hudY, rightBoxW, 40, 8, 8);
            g2.setColor(new Color(85, 60, 125));
            g2.drawRoundRect(rightBoxX, hudY, rightBoxW, 40, 8, 8);

            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("SCORE: " + String.format("%06d", score), rightBoxX + 14, hudY + 16);

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(Color.WHITE);
            g2.drawString("LIVES: ", rightBoxX + 14, hudY + 31);

            int heartX = rightBoxX + 60;
            g2.setFont(new Font("Dialog", Font.BOLD, 14));
            for (int i = 0; i < 3; i++) {
                if (i < lives) {
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    g2.drawString("\u2665", heartX + i * 18, hudY + 33);
                } else {
                    g2.setColor(new Color(80, 50, 75));
                    g2.drawString("\u2661", heartX + i * 18, hudY + 33);
                }
            }
        }

        private void paintOrderRail(Graphics2D g2, int w) {
            int railY = 56;

            g2.setColor(new Color(85, 70, 115));
            g2.fillRect(40, railY, w - 80, 5);
            g2.setColor(new Color(135, 120, 165));
            g2.fillRect(40, railY, w - 80, 2);

            // 1. Gather all active / arrived orders (NO PLACEHOLDER CARDS!)
            List<OrderItem> activeOrders = new ArrayList<>();
            for (OrderItem o : orders) {
                if (o.state != STATE_PENDING) {
                    activeOrders.add(o);
                }
            }

            int count = activeOrders.size();
            if (count == 0) {
                // If waiting for the very first order
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(new Color(110, 95, 145));
                String waitTxt = String.format("AWAITING FIRST CUSTOMER // ARRIVES AT T=%.1fs...", simStartTime);
                int tw = g2.getFontMetrics().stringWidth(waitTxt);
                g2.drawString(waitTxt, (w - tw) / 2, railY + 28);
                return;
            }

            // 2. DYNAMIC RESPONSIVE TICKET RAIL LAYOUT:
            // Dynamically scales card width and gap so up to 8+ tickets fit smoothly!
            int cardW;
            int gap;
            if (count <= 4) {
                cardW = 142;
                gap = 14;
            } else if (count == 5) {
                cardW = 132;
                gap = 10;
            } else if (count == 6) {
                cardW = 118;
                gap = 8;
            } else if (count == 7) {
                cardW = 104;
                gap = 6;
            } else {
                cardW = Math.max(88, (w - 120 - (count - 1) * 6) / count);
                gap = 5;
            }
            int cardH = 86;
            int totalW = count * cardW + (count - 1) * gap;
            int startX = (w - totalW) / 2;

            for (int i = 0; i < count; i++) {
                OrderItem o = activeOrders.get(i);
                float targetX = startX + i * (cardW + gap);

                // Smooth sliding interpolation
                if (o.currentCardX < 0) {
                    o.currentCardX = targetX;
                } else {
                    o.currentCardX += (targetX - o.currentCardX) * 0.25f;
                }

                int tx = (int) o.currentCardX;
                int ty = railY + 6;

                Color borderC, bgC;
                if (o.isDelivered) {
                    bgC = new Color(16, 42, 26);
                    borderC = new Color(0, 240, 100);
                } else if (o.state == STATE_OVERCOOKED) {
                    bgC = new Color(45, 15, 20);
                    borderC = RetroTheme.ACCENT_PINK;
                } else if (o.state == STATE_READY) {
                    if (o.burnTimer <= 2.0f) {
                        float pulse = (float)(0.6 + 0.4 * Math.sin(animTick * 0.4));
                        bgC = new Color((int)(60 * pulse), 15, 25);
                        borderC = new Color(255, (int)(50 * pulse), 50);
                    } else {
                        bgC = new Color(32, 48, 25);
                        borderC = new Color(0, 255, 120);
                    }
                } else if (o.state == STATE_COOKING) {
                    bgC = new Color(28, 20, 52);
                    borderC = RetroTheme.ACCENT_CYAN;
                } else if (o.state == STATE_ARRIVED) {
                    float pulse = (float)(0.6 + 0.4 * Math.sin(animTick * 0.25));
                    bgC = new Color((int)(45 * pulse), (int)(35 * pulse), 18);
                    borderC = new Color(255, (int)(215 * pulse), 30);
                } else {
                    bgC = new Color(18, 14, 30);
                    borderC = new Color(55, 40, 75);
                }

                g2.setColor(bgC);
                g2.fillRoundRect(tx, ty, cardW, cardH, 6, 6);

                g2.setColor(borderC);
                g2.setStroke(new BasicStroke(o.state == STATE_READY || o.state == STATE_OVERCOOKED || o.state == STATE_ARRIVED ? 2f : 1.2f));
                g2.drawRoundRect(tx, ty, cardW, cardH, 6, 6);
                g2.setStroke(new BasicStroke(1f));

                // Hanging Clip
                g2.setColor(new Color(195, 200, 215));
                g2.fillRect(tx + cardW / 2 - 6, ty - 4, 12, 5);

                int iconSz = (cardW < 110) ? 20 : 24;
                BufferedImage icon = PixelSprites.createDishSprite(o.dishType, iconSz);
                if (icon != null) {
                    g2.drawImage(icon, tx + 5, ty + 6, null);
                }

                int textX = (cardW < 110) ? (tx + 28) : (tx + 34);
                g2.setFont(new Font("Consolas", Font.BOLD, (cardW < 120) ? 9 : 10));
                g2.setColor(Color.WHITE);
                String title = (cardW < 105) ? o.name : (o.name + " (" + o.dishName + ")");
                g2.drawString(title, textX, ty + 16);

                g2.setFont(new Font("Consolas", Font.BOLD, (cardW < 120) ? 8 : 9));
                g2.setColor(new Color(255, 215, 0));
                g2.drawString(String.format("STOVE #%d [AT:%.1f]", (o.stoveIndex + 1), o.at), textX, ty + 28);

                // Progress Bar
                int barW = cardW - 16;
                int barH = 5;
                int barX = tx + 8;
                int barY = ty + 38;

                g2.setColor(new Color(12, 8, 20));
                g2.fillRect(barX, barY, barW, barH);

                if (o.state == STATE_COOKING) {
                    g2.setColor(RetroTheme.ACCENT_CYAN);
                    g2.fillRect(barX, barY, (int)(barW * o.cookProgress), barH);
                } else if (o.state == STATE_READY) {
                    float burnPct = Math.max(0f, o.burnTimer / o.burnMax);
                    Color barColor = (o.burnTimer <= 2.0f) ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_YELLOW;
                    g2.setColor(barColor);
                    g2.fillRect(barX, barY, (int)(barW * burnPct), barH);
                } else if (o.isDelivered) {
                    g2.setColor(new Color(0, 240, 100));
                    g2.fillRect(barX, barY, barW, barH);
                } else if (o.state == STATE_OVERCOOKED) {
                    g2.setColor(new Color(80, 20, 30));
                    g2.fillRect(barX, barY, barW, barH);
                } else if (o.state == STATE_ARRIVED) {
                    g2.setColor(new Color(255, 200, 30));
                    g2.fillRect(barX, barY, barW, barH);
                }

                g2.setColor(new Color(65, 45, 95));
                g2.drawRect(barX, barY, barW, barH);

                // Action Callout Badge on Card
                g2.setFont(new Font("Consolas", Font.BOLD, (cardW < 120) ? 8 : 9));
                if (o.isDelivered) {
                    g2.setColor(new Color(0, 240, 100));
                    g2.drawString("DELIVERED [OK]", tx + 8, ty + 70);
                } else if (o.state == STATE_OVERCOOKED) {
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    g2.drawString(cardW < 120 ? "OVERCOOKED!" : "OVERCOOKED! [SPACE]", tx + 8, ty + 70);
                } else if (o.state == STATE_READY) {
                    if (o.burnTimer <= 2.0f) {
                        float pulse = (float)(0.7 + 0.3 * Math.sin(animTick * 0.4));
                        g2.setColor(new Color(255, (int)(60 * pulse), 60));
                        g2.drawString(String.format("BURNING! %.1fs", o.burnTimer), tx + 8, ty + 70);
                    } else {
                        float pulse = (float)(0.7 + 0.3 * Math.sin(animTick * 0.25));
                        g2.setColor(new Color((int)(100 * pulse), 255, 120));
                        g2.drawString(String.format("PICK UP! (%.1fs)", o.burnTimer), tx + 8, ty + 70);
                    }
                } else if (o.state == STATE_COOKING) {
                    g2.setColor(RetroTheme.ACCENT_CYAN);
                    g2.drawString(String.format(cardW < 120 ? "COOK (%.1fs)" : "COOKING (%.2fs)", o.timeLeft), tx + 8, ty + 70);
                } else if (o.state == STATE_ARRIVED) {
                    float pulse = (float)(0.7 + 0.3 * Math.sin(animTick * 0.25));
                    g2.setColor(new Color(255, (int)(215 * pulse), 0));
                    g2.drawString(cardW < 120 ? "ADMIT! [SP]" : "ADMIT! [SPACE]", tx + 8, ty + 70);
                }
            }
        }

        private void paintStoves(Graphics2D g2) {
            int stoveY = 252;

            for (OrderItem o : orders) {
                int sx = (int) o.stoveX;

                // 1. Ambient Floor Shadow beneath stove
                g2.setColor(new Color(0, 0, 0, 95));
                g2.fillOval(sx - 10, stoveY + 54, 88, 18);

                // 2. DEDICATED DIGITAL LED STATUS PLATE (Mounted ABOVE the stove, y: 228..244)
                // This eliminates any overlapping text or collision with knobs!
                g2.setColor(new Color(12, 8, 20));
                g2.fillRoundRect(sx - 6, 228, 80, 16, 4, 4);
                g2.setColor(new Color(55, 40, 75));
                g2.drawRoundRect(sx - 6, 228, 80, 16, 4, 4);

                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                if (o.isDelivered) {
                    g2.setColor(new Color(0, 240, 100));
                    g2.drawString("DELIVERED", sx + 4, 240);
                } else if (o.state == STATE_OVERCOOKED) {
                    g2.setColor(new Color(255, 40, 60));
                    g2.drawString("!OVERCOOK!", sx + 2, 240);
                } else if (o.state == STATE_READY) {
                    g2.setColor(new Color(255, 220, 0));
                    g2.drawString(String.format("READY! %.1fs", o.burnTimer), sx - 2, 240);
                } else if (o.state == STATE_COOKING) {
                    g2.setColor(new Color(0, 240, 255));
                    g2.drawString(String.format("HEAT:%d%%", (int)(100 * o.cookProgress)), sx + 2, 240);
                } else if (o.state == STATE_ARRIVED) {
                    float pulse = (float)(0.7 + 0.3 * Math.sin(animTick * 0.3));
                    g2.setColor(new Color(255, (int)(215 * pulse), 0));
                    g2.drawString("!ADMIT " + o.name + "!", sx + 4, 240);
                } else {
                    g2.setColor(new Color(75, 65, 95));
                    g2.drawString("STOVE #" + (o.stoveIndex + 1), sx + 12, 240);
                }

                // 3. Stove Platform
                g2.setColor(new Color(42, 32, 60));
                g2.fillRoundRect(sx - 10, stoveY + 28, 76, 32, 6, 6);
                g2.setColor(new Color(80, 65, 110));
                g2.drawRoundRect(sx - 10, stoveY + 28, 76, 32, 6, 6);

                // 4. Dual Gas Burner Knobs (Neat and cleanly positioned on platform front)
                g2.setColor(new Color(185, 175, 210));
                g2.fillOval(sx + 8, stoveY + 44, 10, 10);
                g2.fillOval(sx + 38, stoveY + 44, 10, 10);
                g2.setColor(new Color(30, 20, 45));
                g2.fillOval(sx + 12, stoveY + 48, 2, 2);
                g2.fillOval(sx + 42, stoveY + 48, 2, 2);

                // 5. Crisp Stove Sprite with Active Flame
                boolean activeFlame = (o.state == STATE_COOKING || o.state == STATE_READY || o.state == STATE_OVERCOOKED);
                BufferedImage stove = PixelSprites.createStoveSprite(54, activeFlame);
                if (stove != null) {
                    g2.drawImage(stove, sx + 2, stoveY, null);
                }

                // 6. Roaring Fire Animation when Overcooked!
                if (o.state == STATE_OVERCOOKED) {
                    g2.setColor(new Color(255, 40, 60, 210));
                    int fireH = (int)(24 + Math.sin(animTick * 0.5) * 8);
                    g2.fillOval(sx + 12, stoveY - fireH + 10, 32, fireH);
                    g2.setColor(new Color(255, 210, 0, 230));
                    g2.fillOval(sx + 18, stoveY - fireH + 18, 20, fireH - 8);
                }

                // 7. Sizzling Dish inside pan (Rendered ONLY when actively cooking or ready!)
                if (o.state == STATE_COOKING || o.state == STATE_READY || o.state == STATE_OVERCOOKED) {
                    BufferedImage dish = PixelSprites.createDishSprite(o.dishType, 28);
                    if (dish != null) {
                        if (o.state == STATE_OVERCOOKED) {
                            g2.setColor(new Color(25, 18, 22, 240));
                            g2.fillOval(sx + 15, stoveY - 4, 24, 24);
                        } else {
                            g2.drawImage(dish, sx + 14, stoveY - 6, null);
                        }
                    }
                }

                // 8. Glowing Interaction Ring when Chef is close (Only for active stoves, never for IDLE ones!)
                float dist = (float) Math.hypot((o.stoveX + 28) - (chefX + 28), 295 - (chefY + 28));
                float ringDist = (orders.size() >= 6) ? 75f : 95f;
                if (dist < ringDist && o.state != STATE_PENDING) {
                    Color ringColor;
                    if (o.state == STATE_ARRIVED) {
                        ringColor = RetroTheme.ACCENT_YELLOW;
                    } else if (o.state == STATE_READY) {
                        ringColor = RetroTheme.ACCENT_GREEN;
                    } else if (o.state == STATE_OVERCOOKED) {
                        ringColor = RetroTheme.ACCENT_PINK;
                    } else {
                        ringColor = RetroTheme.ACCENT_CYAN;
                    }
                    g2.setColor(ringColor);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{5f, 5f}, (animTick * 1.5f) % 10f));
                    g2.drawOval(sx - 4, stoveY + 54, 68, 18);
                    g2.setStroke(new BasicStroke(1f));
                }
            }
        }

        private void paintFloorKitchenProps(Graphics2D g2, int w) {
            // Floor Props: Inset Prep Counter & Ingredient Crates to eliminate empty void!

            // 1. Left Prep Station (x: 24, y: 350..430)
            g2.setColor(new Color(45, 30, 20));
            g2.fillRoundRect(20, 355, 50, 75, 6, 6);
            g2.setColor(new Color(90, 60, 40));
            g2.drawRoundRect(20, 355, 50, 75, 6, 6);
            // Wooden crate slats
            g2.setColor(new Color(140, 95, 60));
            g2.drawLine(24, 380, 66, 380);
            g2.drawLine(24, 405, 66, 405);
            // Food items in crates
            g2.setColor(new Color(255, 50, 50));
            g2.fillOval(28, 362, 12, 12); // Tomatoes
            g2.fillOval(44, 364, 12, 12);
            g2.setColor(new Color(240, 240, 240));
            g2.fillRoundRect(28, 388, 32, 12, 3, 3); // Rice sack

            // 2. Right Prep Station: Chopping Block (x: w - 74, y: 355..430)
            int rx = w - 70;
            g2.setColor(new Color(55, 38, 28));
            g2.fillRoundRect(rx, 355, 50, 75, 6, 6);
            g2.setColor(new Color(110, 75, 55));
            g2.drawRoundRect(rx, 355, 50, 75, 6, 6);
            // Cutting block top
            g2.setColor(new Color(210, 160, 110));
            g2.fillRoundRect(rx + 6, 362, 38, 22, 4, 4);
            // Kitchen Cleaver knife
            g2.setColor(new Color(180, 190, 210));
            g2.fillRect(rx + 16, 368, 18, 10);
            g2.setColor(new Color(80, 50, 30));
            g2.fillRect(rx + 12, 371, 6, 4); // Handle
        }

        private void paintDashGhosts(Graphics2D g2) {
            for (DashGhost g : dashGhosts) {
                BufferedImage chef = PixelSprites.createChefSprite(54, false);
                if (chef != null) {
                    Graphics2D gGhost = (Graphics2D) g2.create();
                    gGhost.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, g.alpha / 255f));
                    if (g.facingRight) {
                        gGhost.drawImage(chef, (int) g.x, (int) g.y, null);
                    } else {
                        gGhost.drawImage(chef, (int) g.x + 54, (int) g.y, -54, 54, null);
                    }
                    gGhost.dispose();
                }
            }
        }

        private void paintChef(Graphics2D g2) {
            boolean isMoving = (Math.abs(chefVx) > 0.5f || Math.abs(chefVy) > 0.5f);
            float bobY = isMoving ? (float)(Math.sin(animTick * 0.45) * 4) : 0f;
            int cx = (int) chefX;
            int cy = (int) (chefY + bobY);

            // Floor Ambient Occlusion Shadow
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillOval(cx + 6, (int)(chefY + 48), 44, 14);

            // Chef Sprite
            BufferedImage chef = PixelSprites.createChefSprite(54, isStirring);
            if (chef != null) {
                if (facingRight) {
                    g2.drawImage(chef, cx, cy, null);
                } else {
                    g2.drawImage(chef, cx + 54, cy, -54, 54, null);
                }
            }

            // PHYSICALLY CARRYING ITEM OVERHEAD!
            if (heldDishType != -1) {
                BufferedImage plateDish = PixelSprites.createDishSprite(heldDishType, 32);
                if (plateDish != null) {
                    int plateX = cx + 11;
                    int plateY = cy - 22;

                    // Shiny White Ceramic Plate with Rim
                    g2.setColor(new Color(245, 245, 255));
                    g2.fillOval(plateX - 4, plateY + 14, 38, 14);
                    g2.setColor(new Color(190, 190, 215));
                    g2.drawOval(plateX - 4, plateY + 14, 38, 14);

                    g2.drawImage(plateDish, plateX, plateY, null);

                    // Glowing Overhead Badge
                    g2.setColor(new Color(20, 14, 36, 210));
                    g2.fillRoundRect(plateX - 20, plateY - 14, 80, 14, 4, 4);
                    g2.setColor(RetroTheme.ACCENT_YELLOW);
                    g2.drawRoundRect(plateX - 20, plateY - 14, 80, 14, 4, 4);

                    g2.setFont(new Font("Consolas", Font.BOLD, 9));
                    g2.setColor(RetroTheme.ACCENT_YELLOW);
                    g2.drawString("HOLD: " + heldDishName, plateX - 16, plateY - 3);
                }
            } else if (isHoldingBurntPan) {
                int panX = cx + 14;
                int panY = cy - 18;
                g2.setColor(new Color(35, 20, 25));
                g2.fillOval(panX, panY, 28, 16);
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.drawString("BURNT PAN", panX - 6, panY - 4);
            }

            // Stirring Action Indicator
            if (isStirring) {
                g2.setColor(new Color(255, 230, 0));
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.drawString("⚡ OVERCLOCKING! ⚡", cx - 18, cy - 8);
            }
        }

        private void paintServicePassCounter(Graphics2D g2, int w, int h) {
            int passY = 505;

            // Rich Mahogany Wood Service Counter
            GradientPaint woodGrad = new GradientPaint(
                    0, passY, new Color(64, 30, 44),
                    0, passY + 70, new Color(36, 16, 26)
            );
            g2.setPaint(woodGrad);
            g2.fillRect(0, passY, w, 70);

            // Polished Brass Edges
            g2.setColor(new Color(230, 180, 75));
            g2.fillRect(0, passY, w, 4);
            g2.setColor(new Color(145, 105, 42));
            g2.fillRect(0, passY + 66, w, 4);

            // Large Golden Service Bell with Impact Shockwaves
            int bellX = 50;
            int bellY = passY + 12;
            if (bellRingAnim > 0) {
                g2.setColor(new Color(255, 230, 100, (int)(bellRingAnim * 15)));
                g2.drawOval(bellX - 10, bellY - 8, 52, 40);
                g2.drawOval(bellX - 18, bellY - 14, 68, 52);
            }

            g2.setColor(new Color(245, 195, 75));
            g2.fillOval(bellX, bellY, 32, 24);
            g2.setColor(new Color(255, 235, 130));
            g2.fillOval(bellX + 12, bellY - 4, 10, 8);

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("BELL [SPACE]", 30, passY + 44);

            // Delivered Ceramic Plates Rack: EXACTLY ALIGNED WITH EACH STOVE'S COLUMN!
            for (OrderItem o : orders) {
                if (o.isDelivered) {
                    int plateX = (int) o.stoveX + 12;
                    g2.setColor(new Color(245, 245, 255));
                    g2.fillOval(plateX - 4, passY + 16, 36, 16);
                    g2.setColor(new Color(180, 180, 215));
                    g2.drawOval(plateX - 4, passY + 16, 36, 16);

                    BufferedImage dish = PixelSprites.createDishSprite(o.dishType, 24);
                    if (dish != null) {
                        g2.drawImage(dish, plateX + 2, passY + 8, null);
                    }

                    g2.setColor(new Color(0, 240, 100));
                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    g2.drawString(o.name, plateX + 8, passY + 44);
                }
            }

            // High-Tech Trash Incinerator on the far right
            int trashX = w - 125;
            g2.setColor(new Color(45, 38, 56));
            g2.fillRoundRect(trashX, passY + 8, 86, 46, 6, 6);
            g2.setColor(new Color(85, 75, 105));
            g2.drawRoundRect(trashX, passY + 8, 86, 46, 6, 6);

            // Glowing Ash Grill
            g2.setColor(new Color(255, 80, 20, 180));
            g2.fillRect(trashX + 8, passY + 36, 70, 4);

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.TEXT_DIM);
            g2.drawString("INCINERATOR", trashX + 10, passY + 24);
            g2.setFont(new Font("Consolas", Font.PLAIN, 8));
            g2.drawString("[DUMP BURNT ASHES]", trashX + 4, passY + 48);
        }

        private void paintOscilloscopeTerminal(Graphics2D g2, int w, int h) {
            int termY = 580;
            int termH = Math.max(80, h - termY - 8);

            // Retro Green Phosphor CRT Terminal Frame
            g2.setColor(new Color(6, 12, 8));
            g2.fillRect(16, termY, w - 32, termH);
            g2.setColor(new Color(18, 80, 45));
            g2.drawRect(16, termY, w - 32, termH);

            // CRT Scanlines
            g2.setColor(new Color(0, 255, 100, 10));
            for (int y = termY; y < termY + termH; y += 4) {
                g2.drawLine(16, y, w - 16, y);
            }

            // Oscilloscope Waveform Grid Box (Right side of terminal)
            int waveBoxW = 220;
            int waveBoxX = w - waveBoxW - 30;
            int waveBoxY = termY + 8;
            int waveBoxH = termH - 16;

            g2.setColor(new Color(10, 24, 15));
            g2.fillRect(waveBoxX, waveBoxY, waveBoxW, waveBoxH);
            g2.setColor(new Color(25, 95, 55));
            g2.drawRect(waveBoxX, waveBoxY, waveBoxW, waveBoxH);

            // Dancing CPU Waveform
            int activeNj = 0;
            for (OrderItem o : orders) if (o.state == STATE_COOKING) activeNj++;
            g2.setColor(new Color(0, 255, 140));
            int midY = waveBoxY + waveBoxH / 2;
            int prevX = waveBoxX;
            int prevY = midY;

            for (int x = 0; x < waveBoxW; x += 3) {
                float wave = (float) Math.sin((x + animTick * 4) * 0.08) * (6f + activeNj * 3.5f);
                int curY = (int)(midY + wave);
                if (x > 0) {
                    g2.drawLine(prevX, prevY, waveBoxX + x, curY);
                }
                prevX = waveBoxX + x;
                prevY = curY;
            }

            g2.setFont(new Font("Consolas", Font.BOLD, 8));
            g2.setColor(new Color(0, 255, 140));
            g2.drawString("PMU WAVE: nj=" + activeNj + " FREQ", waveBoxX + 6, waveBoxY + 12);

            // Terminal Telemetry Readout
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(new Color(0, 255, 120));
            g2.drawString(">> MULTIPROGRAMMING OS DISPATCHER // PROCESSOR SHARING PMU", 28, termY + 16);

            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            g2.setColor(new Color(160, 240, 180));
            g2.drawString("FORMULA: %CPU = 1.0 / nj  |  Equal time-sharing among active stoves, NOT FIFO!", 28, termY + 30);
            g2.drawString("DISPATCH: [SPACE] Admit arrived order -> Stove cooks at 1/nj -> [SPACE] Pick up cooked food & deliver!", 28, termY + 44);

            int logCount = eventLog.size();
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(new Color(255, 230, 80));
            if (logCount > 0) {
                g2.drawString("> " + eventLog.get(logCount - 1), 28, termY + 60);
            }
            if (logCount > 1 && termH > 75) {
                g2.setColor(new Color(180, 200, 220));
                g2.drawString("  " + eventLog.get(logCount - 2), 28, termY + 74);
            }
        }

        private void paintParticles(Graphics2D g2) {
            for (KitchenParticle p : particles) {
                int alpha = (int)(255 * (p.life / (float) p.maxLife));
                Color c = new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), Math.max(0, Math.min(255, alpha)));
                g2.setColor(c);
                g2.fillOval((int) p.x, (int) p.y, (int) p.size, (int) p.size);
            }
        }

        private void paintFloaters(Graphics2D g2) {
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            for (FloatingEffect f : floaters) {
                int a = (int)(255 * Math.min(1.0f, f.life / 30f));
                int txtW = fm.stringWidth(f.text);
                int px = (int)(f.x - txtW / 2);
                int py = (int) f.y;

                g2.setColor(new Color(12, 8, 24, (int)(a * 0.90f)));
                g2.fillRoundRect(px - 6, py - 11, txtW + 12, 16, 5, 5);
                g2.setColor(new Color(f.color.getRed(), f.color.getGreen(), f.color.getBlue(), a));
                g2.drawRoundRect(px - 6, py - 11, txtW + 12, 16, 5, 5);

                g2.setColor(new Color(255, 255, 255, a));
                g2.drawString(f.text, px, py + 1);
            }
        }

        private void paintVictoryScreen(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(8, 6, 16, 235));
            g2.fillRect(0, 0, w, h);

            int boxW = 620;
            int boxH = 280;
            int boxX = (w - boxW) / 2;
            int boxY = (h - boxH) / 2;

            g2.setColor(new Color(24, 16, 42));
            g2.fillRoundRect(boxX, boxY, boxW, boxH, 12, 12);
            g2.setColor(new Color(0, 240, 100));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(boxX, boxY, boxW, boxH, 12, 12);
            g2.setStroke(new BasicStroke(1f));

            g2.setFont(new Font("Consolas", Font.BOLD, 20));
            g2.setColor(new Color(0, 240, 100));
            String title = "★ VICTORY! ALL DISHES MASTERED! ★";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, boxX + (boxW - fm.stringWidth(title)) / 2, boxY + 45);

            g2.setFont(new Font("Consolas", Font.BOLD, 14));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            String sc = String.format("FINAL SCORE: %d XP  |  HIGHEST COMBO: x%d  |  LIVES: %d/3", score, combo, lives);
            FontMetrics sfm = g2.getFontMetrics();
            g2.drawString(sc, boxX + (boxW - sfm.stringWidth(sc)) / 2, boxY + 80);

            g2.setFont(new Font("Consolas", Font.PLAIN, 11));
            g2.setColor(Color.WHITE);
            String info1 = "Multiprogramming Processor Sharing completed with maximum throughput!";
            String info2 = "You balanced concurrent cooking and served every order before overcooking!";
            FontMetrics ifm = g2.getFontMetrics();
            g2.drawString(info1, boxX + (boxW - ifm.stringWidth(info1)) / 2, boxY + 120);
            g2.drawString(info2, boxX + (boxW - ifm.stringWidth(info2)) / 2, boxY + 140);

            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            String retry = "PRESS [R] TO REPLAY KITCHEN  |  PRESS [ESC] TO EXIT TO LAB";
            FontMetrics rfm = g2.getFontMetrics();
            g2.drawString(retry, boxX + (boxW - rfm.stringWidth(retry)) / 2, boxY + 205);
        }

        private void paintGameOverScreen(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(18, 6, 14, 235));
            g2.fillRect(0, 0, w, h);

            int boxW = 560;
            int boxH = 250;
            int boxX = (w - boxW) / 2;
            int boxY = (h - boxH) / 2;

            g2.setColor(new Color(38, 14, 24));
            g2.fillRoundRect(boxX, boxY, boxW, boxH, 12, 12);
            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(boxX, boxY, boxW, boxH, 12, 12);
            g2.setStroke(new BasicStroke(1f));

            g2.setFont(new Font("Consolas", Font.BOLD, 18));
            g2.setColor(RetroTheme.ACCENT_PINK);
            String title = "GAME OVER // KITCHEN OVERCOOKED & BURNED!";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, boxX + (boxW - fm.stringWidth(title)) / 2, boxY + 50);

            g2.setFont(new Font("Consolas", Font.PLAIN, 11));
            g2.setColor(Color.WHITE);
            String sub1 = "Too many dishes overcooked on the stoves without being served!";
            String sub2 = "Tip: Watch the shortest burst times (e.g. Sushi BT 0.1) that finish earliest!";
            FontMetrics ifm = g2.getFontMetrics();
            g2.drawString(sub1, boxX + (boxW - ifm.stringWidth(sub1)) / 2, boxY + 95);
            g2.drawString(sub2, boxX + (boxW - ifm.stringWidth(sub2)) / 2, boxY + 115);

            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            String retry = "PRESS [R] TO RETRY  |  PRESS [ESC] TO EXIT TO LAB";
            FontMetrics rfm = g2.getFontMetrics();
            g2.drawString(retry, boxX + (boxW - rfm.stringWidth(retry)) / 2, boxY + 180);
        }
    }
}
