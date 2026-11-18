package arcadeos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * MISSION 04 // SUPER MARIO PIPE CACHE RUNNER
 * Standalone playable 2D retro arcade minigame for Super Mario Cache Replacement.
 * Control Mario, manage the green warp pipes, stomp Goombas, and master FIFO/LRU memory management!
 */
public class MarioCacheRunnerDialog extends JDialog {

    // Gameplay States
    private static final int PHASE_IDLE = 0;
    private static final int PHASE_REQUEST = 1;
    private static final int PHASE_CELEBRATE = 2;

    // Stream & Configuration
    private int[] stream;
    private int streamIndex = 0;
    private final int capacity;
    private final String algorithm; // FIFO, LRU, OPTIMAL

    // Cache State: Pipe Slots (array of size capacity)
    private static class PipeSlot {
        int page = -1;           // -1 means empty
        long loadTick = 0;       // For FIFO
        long lastAccessTick = 0; // For LRU
        float x;                 // Center x coordinate
        float y = 430f;          // Ground level y
        float w = 110f;          // Pipe width
        float h = 100f;          // Pipe height
        boolean isVictim = false;
        boolean isHit = false;
        float warpAnim = 0f;     // For flush/load visual effect
    }

    private final PipeSlot[] pipes;
    private final Queue<Integer> fifoQueue = new LinkedList<>();

    // Player (Mario) Physics & State
    private float marioX = 220f;
    private float marioY = 480f; // On the floor
    private float marioVx = 0f;
    private float marioVy = 0f;
    private boolean isGrounded = true;
    private boolean facingRight = true;
    private boolean isDashing = false;
    private boolean isCrouching = false;
    private PipeSlot standingPipe = null; // Pipe that Mario is currently standing on top of
    private int animTick = 0;

    // Input States
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean jumpRequested = false;

    // Active Request & Timing
    private int currentReqPage = -1;
    private int targetHitPipe = -1;
    private int victimPipeIndex = -1;
    private float evictionTimer = 0f;
    private static final float EVICTION_TIMEOUT = 4.2f; // Seconds allowed to flush
    private float nextRequestDelay = 0f;

    // Hazards: Walking Goombas
    private static class Goomba {
        float x;
        float y = 502f;
        float vx = -1.5f;
        boolean isDead = false;
        int deathTicks = 0;

        Goomba(float x) {
            this.x = x;
        }
    }
    private final List<Goomba> goombas = new ArrayList<>();

    // Classic Super Mario Overworld Clouds
    private static class MarioCloud {
        float x, y;
        float speed;
        int puffs;
        float scale;

        MarioCloud(float x, float y, float speed, int puffs, float scale) {
            this.x = x; this.y = y; this.speed = speed; this.puffs = puffs; this.scale = scale;
        }
    }
    private final List<MarioCloud> clouds = new ArrayList<>();

    // Juice & FX
    private int lives = 3;
    private int score = 0;
    private int combo = 0;
    private int totalHits = 0;
    private int totalFaults = 0;
    private boolean isGameOver = false;
    private boolean isVictory = false;
    private boolean soundEnabled = true;
    private int starModeTicks = 0; // Invincibility & double score mode
    private int screenShakeTicks = 0;
    private int screenFlashTicks = 0;
    private Color screenFlashColor = new Color(255, 40, 60, 60);

    // Floating text & particles
    private static class FloatingText {
        float x, y;
        String text;
        Color color;
        int life;
        FloatingText(float x, float y, String text, Color c, int life) {
            this.x = x; this.y = y; this.text = text; this.color = c; this.life = life;
        }
    }
    private final List<FloatingText> floaters = new ArrayList<>();

    private static class MarioParticle {
        float x, y, vx, vy, size;
        Color color;
        int life;
        MarioParticle(float x, float y, float vx, float vy, Color c, int life, float size) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.color = c; this.life = life; this.size = size;
        }
    }
    private final List<MarioParticle> particles = new ArrayList<>();

    private static class DashGhost {
        float x, y;
        boolean facingRight;
        int alpha = 160;
        DashGhost(float x, float y, boolean facingRight) {
            this.x = x; this.y = y; this.facingRight = facingRight;
        }
    }
    private final List<DashGhost> dashGhosts = new ArrayList<>();

    private javax.swing.Timer gameTimer;
    private MarioArcadeCanvas canvas;

    public MarioCacheRunnerDialog(JFrame owner, int[] streamInput, int capacityInput, String algoInput) {
        super(owner, "MISSION 04 // SUPER MARIO PIPE CACHE ARCADE", false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);

        this.capacity = Math.max(3, Math.min(4, capacityInput));
        this.algorithm = (algoInput != null && !algoInput.isEmpty()) ? algoInput : "FIFO";

        if (streamInput != null && streamInput.length > 0) {
            this.stream = streamInput.clone();
        } else {
            this.stream = new int[]{7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1, 2, 0, 1, 7, 0, 1};
        }

        this.pipes = new PipeSlot[this.capacity];
        for (int i = 0; i < this.capacity; i++) {
            this.pipes[i] = new PipeSlot();
        }

        Dimension targetDim = RetroTheme.getClampedDialogSize(owner, 1080, 760);
        setSize(targetDim);
        setMinimumSize(new Dimension(880, 620));
        setLocationRelativeTo(owner);

        initUI();
        initKeyboard();
        restartGame();
        startGameLoop();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(12, 10, 24));

        // Top Retro Arcade Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(true);
        header.setBackground(new Color(22, 14, 38));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(75, 45, 115)),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftHeader.setOpaque(false);

        JLabel lblTitle = new JLabel("SUPER MARIO // PIPE CACHE ARCADE");
        lblTitle.setFont(new Font("Consolas", Font.BOLD, 13));
        lblTitle.setForeground(RetroTheme.ACCENT_YELLOW);
        lblTitle.setIcon(new ImageIcon(PixelSprites.createMarioSprite(16)));
        lblTitle.setIconTextGap(6);
        leftHeader.add(lblTitle);

        JLabel lblAlgoBadge = new JLabel("[" + algorithm + " POLICY]");
        lblAlgoBadge.setFont(new Font("Consolas", Font.BOLD, 10));
        lblAlgoBadge.setForeground(RetroTheme.ACCENT_CYAN);
        leftHeader.add(lblAlgoBadge);
        header.add(leftHeader, BorderLayout.WEST);

        JLabel lblTag = new JLabel("[ A/D: MOVE | W / SPACE: JUMP | S / DOWN: WARP PIPE FLUSH | SHIFT: SPRINT ]");
        lblTag.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblTag.setForeground(new Color(210, 200, 240));
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

        canvas = new MarioArcadeCanvas();
        canvas.setFocusable(true);
        root.add(canvas, BorderLayout.CENTER);

        setContentPane(root);
    }

    private void initKeyboard() {
        InputMap im = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = canvas.getActionMap();

        bindPressRelease(im, am, KeyEvent.VK_LEFT, () -> { movingLeft = true; facingRight = false; }, () -> movingLeft = false);
        bindPressRelease(im, am, KeyEvent.VK_A, () -> { movingLeft = true; facingRight = false; }, () -> movingLeft = false);

        bindPressRelease(im, am, KeyEvent.VK_RIGHT, () -> { movingRight = true; facingRight = true; }, () -> movingRight = false);
        bindPressRelease(im, am, KeyEvent.VK_D, () -> { movingRight = true; facingRight = true; }, () -> movingRight = false);

        // Jump: UP, W, and SPACE ALWAYS JUMP! (Never hijacked by flush)
        bindAction(im, am, KeyEvent.VK_UP, "JUMP_UP", this::triggerJump);
        bindAction(im, am, KeyEvent.VK_W, "JUMP_W", this::triggerJump);
        bindAction(im, am, KeyEvent.VK_SPACE, "JUMP_SPACE", this::triggerJump);

        // Crouch / Pipe Stomp Flush: Press DOWN or S while standing on top of pipe
        bindPressRelease(im, am, KeyEvent.VK_DOWN, () -> { isCrouching = true; handlePipeFlushAction(); }, () -> isCrouching = false);
        bindPressRelease(im, am, KeyEvent.VK_S, () -> { isCrouching = true; handlePipeFlushAction(); }, () -> isCrouching = false);

        // Sprint Dash
        bindPressRelease(im, am, KeyEvent.VK_SHIFT, () -> isDashing = true, () -> isDashing = false);

        // R = Restart, ESC = Exit
        bindAction(im, am, KeyEvent.VK_R, "KEY_RESTART", this::restartGame);
        bindAction(im, am, KeyEvent.VK_ESCAPE, "KEY_ESC", this::dispose);

        // Mouse click navigation: move Mario, no accidental flush
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                canvas.requestFocusInWindow();
                marioX = Math.max(80f, Math.min(canvas.getWidth() - 110f, e.getX() - 24f));
                if (isGrounded && e.getY() < 460) {
                    triggerJump();
                }
            }
        });
    }

    private void bindPressRelease(InputMap im, ActionMap am, int keyCode, Runnable onPress, Runnable onRelease) {
        String pKey = "PRESS_" + keyCode;
        String rKey = "REL_" + keyCode;
        im.put(KeyStroke.getKeyStroke(keyCode, 0, false), pKey);
        im.put(KeyStroke.getKeyStroke(keyCode, 0, true), rKey);
        am.put(pKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { onPress.run(); }
        });
        am.put(rKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { onRelease.run(); }
        });
    }

    private void bindAction(InputMap im, ActionMap am, int keyCode, String key, Runnable action) {
        im.put(KeyStroke.getKeyStroke(keyCode, 0, false), key);
        am.put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void playSfx(int freq, int ms, float vol) {
        if (!soundEnabled) return;
        try {
            ArchivesPanel.playTone(freq, ms, vol);
        } catch (Exception ignored) {}
    }

    private void startGameLoop() {
        gameTimer = new javax.swing.Timer(16, e -> {
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

    private void restartGame() {
        streamIndex = 0;
        score = 0;
        combo = 0;
        lives = 3;
        totalHits = 0;
        totalFaults = 0;
        isGameOver = false;
        isVictory = false;
        starModeTicks = 0;
        standingPipe = null;
        nextRequestDelay = 0.5f;

        marioX = 90f;
        marioY = 480f;
        marioVx = 0f;
        marioVy = 0f;
        isGrounded = true;

        fifoQueue.clear();
        for (PipeSlot p : pipes) {
            p.page = -1;
            p.loadTick = 0;
            p.lastAccessTick = 0;
            p.isVictim = false;
            p.isHit = false;
            p.warpAnim = 0f;
        }

        goombas.clear();
        floaters.clear();
        particles.clear();
        dashGhosts.clear();

        initClouds();

        currentReqPage = -1;
        targetHitPipe = -1;
        victimPipeIndex = -1;
        evictionTimer = 0f;

        spawnNextPageRequest();
        playSfx(440, 80, 0.4f);
    }

    private void initClouds() {
        clouds.clear();
        clouds.add(new MarioCloud(80f, 175f, 0.35f, 3, 1.1f));
        clouds.add(new MarioCloud(340f, 255f, 0.22f, 2, 0.85f));
        clouds.add(new MarioCloud(600f, 165f, 0.30f, 3, 1.0f));
        clouds.add(new MarioCloud(860f, 240f, 0.20f, 2, 0.8f));
        clouds.add(new MarioCloud(1140f, 185f, 0.38f, 3, 1.15f));
    }

    private void spawnNextPageRequest() {
        if (streamIndex >= stream.length) {
            // All pages in reference stream cleared!
            isVictory = true;
            playSfx(587, 100, 0.5f);
            playSfx(784, 150, 0.6f);
            playSfx(1046, 300, 0.7f);
            return;
        }

        currentReqPage = stream[streamIndex];
        targetHitPipe = -1;
        victimPipeIndex = -1;

        // Check if page already exists in any pipe (HIT)
        for (int i = 0; i < capacity; i++) {
            if (pipes[i].page == currentReqPage) {
                targetHitPipe = i;
                pipes[i].isHit = true;
                pipes[i].lastAccessTick = animTick;
                break;
            }
        }

        if (targetHitPipe != -1) {
            // PAGE HIT!
            totalHits++;
            combo++;
            int pts = 100 * (starModeTicks > 0 ? 2 : 1) + (combo * 15);
            score += pts;

            addFloating(pipes[targetHitPipe].x, 320, String.format("★ CACHE HIT! +%d ★", pts), RetroTheme.ACCENT_GREEN);
            addPipeBurst(pipes[targetHitPipe].x, 420, RetroTheme.ACCENT_GREEN, 18);
            playSfx(988, 80, 0.5f);
            playSfx(1318, 120, 0.6f);

            if (combo >= 3 && starModeTicks <= 0) {
                starModeTicks = 240; // ~4 seconds Starman mode
                addFloating(marioX, marioY - 30, "★ STARMAN FEVER! x2 SCORE! ★", RetroTheme.ACCENT_YELLOW);
            }

            nextRequestDelay = 2.2f; // Celebration delay before next page
        } else {
            // PAGE FAULT / MISS
            totalFaults++;
            combo = 0;

            // Check if vacant pipe exists (Cold Start Miss)
            int vacantIdx = -1;
            for (int i = 0; i < capacity; i++) {
                if (pipes[i].page == -1) {
                    vacantIdx = i;
                    break;
                }
            }

            if (vacantIdx != -1) {
                // Auto-route or prompt user to touch vacant pipe
                victimPipeIndex = vacantIdx;
                pipes[vacantIdx].isVictim = true;
                evictionTimer = EVICTION_TIMEOUT;
                addFloating(pipes[vacantIdx].x, 330, String.format("PAGE P%d ARRIVED // LOAD IN PIPE #%d!", currentReqPage, vacantIdx), RetroTheme.ACCENT_CYAN);
                playSfx(523, 70, 0.45f);
            } else {
                // PIPES ARE FULL: EVICTION REQUIRED!
                int victim = determineVictimPipe();
                victimPipeIndex = victim;
                pipes[victim].isVictim = true;
                evictionTimer = EVICTION_TIMEOUT;

                addFloating(pipes[victim].x, 320, String.format("! FAULT P%d: FLUSH PIPE #%d [%s] !", currentReqPage, victim, algorithm), RetroTheme.ACCENT_PINK);
                playSfx(330, 80, 0.5f);
            }
        }
    }

    private int determineVictimPipe() {
        if ("LRU".equalsIgnoreCase(algorithm)) {
            long oldestAccess = Long.MAX_VALUE;
            int victim = 0;
            for (int i = 0; i < capacity; i++) {
                if (pipes[i].lastAccessTick < oldestAccess) {
                    oldestAccess = pipes[i].lastAccessTick;
                    victim = i;
                }
            }
            return victim;
        } else if ("OPTIMAL".equalsIgnoreCase(algorithm)) {
            int farthestNext = -1;
            int victim = 0;
            for (int i = 0; i < capacity; i++) {
                int p = pipes[i].page;
                int nextUse = Integer.MAX_VALUE;
                for (int s = streamIndex + 1; s < stream.length; s++) {
                    if (stream[s] == p) {
                        nextUse = s;
                        break;
                    }
                }
                if (nextUse > farthestNext) {
                    farthestNext = nextUse;
                    victim = i;
                }
            }
            return victim;
        } else {
            // FIFO default: Evict the oldest loaded pipe
            long earliestLoad = Long.MAX_VALUE;
            int victim = 0;
            for (int i = 0; i < capacity; i++) {
                if (pipes[i].loadTick < earliestLoad) {
                    earliestLoad = pipes[i].loadTick;
                    victim = i;
                }
            }
            return victim;
        }
    }

    private void updateGameLogic() {
        if (canvas == null) return;
        int w = canvas.getWidth();

        // 1. Mario Movement & Platformer Physics
        float maxSpd = isDashing ? 9.8f : 6.4f;
        float targetVx = 0f;
        if (movingLeft) targetVx -= maxSpd;
        if (movingRight) targetVx += maxSpd;

        marioVx = marioVx * 0.70f + targetVx * 0.30f;
        float oldMarioX = marioX;
        marioX += marioVx;

        // Apply Gravity
        float oldMarioY = marioY;
        marioVy += 0.85f;
        marioY += marioVy;

        // Ground Floor Collision (Floor level y = 480)
        float floorY = 480f;
        boolean onPipe = false;
        PipeSlot currentStandingPipe = null;

        // Pipe Top Landing Collision: Mario can jump and land on top of any Green Warp Pipe!
        for (PipeSlot p : pipes) {
            float pipeLeft = p.x - p.w / 2f + 4f;
            float pipeRight = p.x + p.w / 2f - 4f;
            float pipeTop = p.y - 12f;

            float marioFeet = marioY + 44f;
            float oldFeet = oldMarioY + 44f;
            float marioCenterX = marioX + 22f;

            if (marioCenterX >= pipeLeft && marioCenterX <= pipeRight) {
                // If Mario is falling onto pipe top rim
                if (marioVy >= 0 && marioFeet >= pipeTop && oldFeet <= pipeTop + 16f) {
                    marioY = pipeTop - 44f;
                    marioVy = 0f;
                    isGrounded = true;
                    onPipe = true;
                    currentStandingPipe = p;
                    break;
                }
            }
        }

        standingPipe = onPipe ? currentStandingPipe : null;

        if (!onPipe) {
            if (marioY >= floorY) {
                marioY = floorY;
                marioVy = 0f;
                isGrounded = true;
            } else {
                isGrounded = false;
            }
        }

        // Horizontal Solid Collision with Warp Pipes:
        // Pipes are SOLID OBSTACLES on the ground. Mario CANNOT walk into the middle of them!
        // Mario can only get past them by jumping over or landing on top.
        for (PipeSlot p : pipes) {
            float pipeLeft = p.x - p.w / 2f;
            float pipeRight = p.x + p.w / 2f;
            float pipeTop = p.y - 12f;

            float mLeft = marioX + 6f;
            float mRight = marioX + 38f;
            float mFeet = marioY + 44f;

            // If Mario is below the top rim, the pipe is a solid wall
            if (mFeet > pipeTop + 4f) {
                // Check if Mario overlaps pipe horizontally
                if (mRight > pipeLeft && mLeft < pipeRight) {
                    if (marioVx > 0 && oldMarioX + 38f <= pipeLeft + 12f) {
                        // Walking right into pipe's left wall -> STOPPED!
                        marioX = pipeLeft - 38f;
                        marioVx = 0f;
                    } else if (marioVx < 0 && oldMarioX + 6f >= pipeRight - 12f) {
                        // Walking left into pipe's right wall -> STOPPED!
                        marioX = pipeRight - 6f;
                        marioVx = 0f;
                    } else {
                        // Eject Mario to closest outer side if somehow overlapping
                        float distToLeft = Math.abs(mRight - pipeLeft);
                        float distToRight = Math.abs(mLeft - pipeRight);
                        if (distToLeft < distToRight) {
                            marioX = pipeLeft - 38f;
                        } else {
                            marioX = pipeRight - 6f;
                        }
                        marioVx = 0f;
                    }
                }
            }
        }

        // Aerial Stomp / Ground-Pound into Pipe:
        // If Mario lands on a pipe while holding DOWN or S, immediately trigger the flush action!
        if (onPipe && isCrouching && standingPipe != null && victimPipeIndex != -1) {
            handlePipeFlushAction();
        }

        // Screen Boundaries
        marioX = Math.max(65f, Math.min(w - 95f, marioX));

        // Sprint Dash Trail
        if (isDashing && Math.abs(marioVx) > 0.8f && animTick % 3 == 0) {
            dashGhosts.add(new DashGhost(marioX, marioY, facingRight));
        }

        // Star Mode Rainbow Sparkles
        if (starModeTicks > 0) {
            starModeTicks--;
            if (animTick % 3 == 0) {
                Color rainbow = Color.getHSBColor((animTick * 8 % 360) / 360f, 0.85f, 1.0f);
                particles.add(new MarioParticle(
                        marioX + 16 + (float)(Math.random() - 0.5) * 20,
                        marioY + 24 + (float)(Math.random() - 0.5) * 20,
                        (float)(Math.random() - 0.5) * 1.5f, -1.2f,
                        rainbow, 18, 4.5f));
            }
        }

        // 2. Goombas Walking Hazard (Walks across the entire floor across the pipes!)
        if (animTick % 220 == 0 && goombas.size() < 3) {
            goombas.add(new Goomba(w - 70));
        }

        for (Goomba g : goombas) {
            if (!g.isDead) {
                g.x += g.vx;
                if (g.x < 80) g.vx = Math.abs(g.vx);
                if (g.x > w - 100) g.vx = -Math.abs(g.vx);

                // Mario Collision with Goomba
                float mLeft = marioX + 4;
                float mRight = marioX + 36;
                float mBottom = marioY + 44;

                if (mRight >= g.x && mLeft <= g.x + 28 && mBottom >= g.y && mBottom <= g.y + 24) {
                    // Stomp from above!
                    if (marioVy > 0) {
                        g.isDead = true;
                        marioVy = -11.0f; // Bounce
                        score += 30;
                        addFloating(g.x, g.y - 15, "+30 STOMP!", RetroTheme.ACCENT_YELLOW);
                        playSfx(659, 60, 0.5f);
                    } else if (starModeTicks > 0) {
                        // Starman kills on touch!
                        g.isDead = true;
                        score += 50;
                        addFloating(g.x, g.y - 15, "★ STAR HIT! +50 ★", RetroTheme.ACCENT_CYAN);
                        playSfx(784, 60, 0.5f);
                    } else {
                        // Hurt Mario!
                        takeDamage("GOOMBA BUMP! -1 LIFE");
                    }
                }
            } else {
                g.deathTicks++;
            }
        }
        goombas.removeIf(g -> g.isDead && g.deathTicks > 30);

        // 3. Cache Eviction Timer Countdown
        if (victimPipeIndex != -1) {
            evictionTimer -= 0.016f;

            // Warning pulses
            if (evictionTimer <= 1.5f && animTick % 12 == 0) {
                playSfx(280, 40, 0.35f);
            }

            if (evictionTimer <= 0f) {
                // Timeout! Penalize and auto-flush
                takeDamage("EVICTION TIMEOUT! -1 LIFE");
                executePipeFlush(victimPipeIndex, true);
            }
        }

        // 4. Advance Next Stream Item
        if (nextRequestDelay > 0f) {
            nextRequestDelay -= 0.016f;
            if (nextRequestDelay <= 0f) {
                for (PipeSlot p : pipes) {
                    p.isHit = false;
                }
                streamIndex++;
                spawnNextPageRequest();
            }
        }
    }

    private void triggerJump() {
        if (isGrounded) {
            marioVy = -14.8f;
            isGrounded = false;
            standingPipe = null;
            playSfx(440, 60, 0.45f);
            playSfx(587, 80, 0.45f);

            // Cloud puff at feet
            for (int i = 0; i < 4; i++) {
                particles.add(new MarioParticle(
                        marioX + 16, marioY + 44,
                        (float)(Math.random() - 0.5) * 3f, -0.6f,
                        new Color(230, 230, 255, 180), 14, 4f));
            }
        }
    }

    private void handlePipeFlushAction() {
        if (victimPipeIndex == -1) return;

        // Player MUST be physically standing on top of a pipe to flush/dive!
        // Pressing keys while standing on the ground will NOT flush!
        if (standingPipe == null) {
            return;
        }

        int standingIdx = -1;
        for (int i = 0; i < capacity; i++) {
            if (pipes[i] == standingPipe) {
                standingIdx = i;
                break;
            }
        }

        if (standingIdx == victimPipeIndex) {
            // Correct Flush / Load!
            int pts = (standingPipe.page == -1) ? 50 : 80;
            pts *= (starModeTicks > 0 ? 2 : 1);
            score += pts;

            String msg = (standingPipe.page == -1)
                    ? String.format("PAGE P%d LOADED! +%d", currentReqPage, pts)
                    : String.format("FLUSHED P%d -> LOADED P%d! +%d", standingPipe.page, currentReqPage, pts);

            addFloating(standingPipe.x, 320, msg, RetroTheme.ACCENT_CYAN);
            addPipeBurst(standingPipe.x, 420, RetroTheme.ACCENT_CYAN, 20);
            playSfx(494, 60, 0.5f);
            playSfx(740, 100, 0.6f);

            executePipeFlush(standingIdx, false);
        } else {
            // Stomped on the WRONG pipe!
            takeDamage("WRONG PIPE! CACHE FAULT! -1 LIFE");
        }
    }

    private void executePipeFlush(int pipeIdx, boolean wasTimeout) {
        PipeSlot p = pipes[pipeIdx];
        p.page = currentReqPage;
        p.loadTick = animTick;
        p.lastAccessTick = animTick;
        p.isVictim = false;
        p.warpAnim = 1.0f;

        victimPipeIndex = -1;
        evictionTimer = 0f;
        nextRequestDelay = 1.8f; // Delay before next page arrives
    }

    private void takeDamage(String reason) {
        lives--;
        screenShakeTicks = 16;
        screenFlashTicks = 8;
        screenFlashColor = new Color(255, 40, 60, 75);
        combo = 0;

        addFloating(marioX, marioY - 24, reason, RetroTheme.ACCENT_PINK);
        playSfx(196, 120, 0.6f);

        if (lives <= 0) {
            isGameOver = true;
            playSfx(147, 300, 0.7f);
        }
    }

    private PipeSlot findNearestPipe(float x) {
        PipeSlot best = null;
        float bestDist = 120f;
        for (PipeSlot p : pipes) {
            float dist = Math.abs(p.x - (x + 20));
            if (dist < bestDist) {
                bestDist = dist;
                best = p;
            }
        }
        return best;
    }

    private void addFloating(float x, float y, String text, Color c) {
        floaters.removeIf(f -> Math.abs(f.x - x) < 60f && Math.abs(f.y - y) < 24f);
        floaters.add(new FloatingText(x, y, text, c, 60));
    }

    private void addPipeBurst(float x, float y, Color c, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            float spd = (float)(2.0 + Math.random() * 4.5);
            particles.add(new MarioParticle(x, y, (float)(Math.cos(angle) * spd), (float)(Math.sin(angle) * spd) - 1.5f, c, 26, 6f));
        }
    }

    private void updateEffects() {
        if (screenShakeTicks > 0) screenShakeTicks--;
        if (screenFlashTicks > 0) screenFlashTicks--;

        dashGhosts.removeIf(g -> {
            g.alpha -= 14;
            return g.alpha <= 0;
        });

        floaters.removeIf(f -> {
            f.y -= 0.75f;
            f.life--;
            return f.life <= 0;
        });

        particles.removeIf(p -> {
            p.x += p.vx;
            p.y += p.vy;
            p.size += 0.10f;
            p.life--;
            return p.life <= 0;
        });

        for (PipeSlot p : pipes) {
            if (p.warpAnim > 0f) p.warpAnim -= 0.05f;
        }

        // Drifting Super Mario Clouds
        int canvasW = (canvas != null && canvas.getWidth() > 300) ? canvas.getWidth() : 1000;
        for (MarioCloud c : clouds) {
            c.x += c.speed;
            if (c.x > canvasW + 160) {
                c.x = -160;
            }
        }
    }

    @Override
    public void dispose() {
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop();
        }
        super.dispose();
    }

    // =========================================================================
    // RETRO SUPER MARIO ARCADE CANVAS
    // =========================================================================
    private class MarioArcadeCanvas extends JPanel {

        public MarioArcadeCanvas() {
            setBackground(new Color(12, 10, 24));
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (screenShakeTicks > 0) {
                int ox = (int)((Math.random() - 0.5) * 10);
                int oy = (int)((Math.random() - 0.5) * 10);
                g2.translate(ox, oy);
            }

            int w = getWidth();
            int h = getHeight();

            // 0. Update Pipe Layout Centering
            updatePipeLayout(w);

            // 1. Atmosphere: Underground World 1-2 Brick Cavern & Ceiling Pipes
            paintCavernBackground(g2, w, h);

            // 2. Top HUD Bar: Score, Lives, Hits, Faults, Stream Index
            paintHUD(g2, w);

            // 3. Overhead Page Reference Stream Rail & Target Mystery Block
            paintPageStreamRail(g2, w);

            // 4. The 3 (or 4) Iconic Green Warp Pipes with Resident Page Sprites
            paintWarpPipes(g2);

            // 5. Walking Goombas & Hazards
            paintGoombas(g2);

            // 6. Dash Ghosts & Particles
            paintDashGhosts(g2);
            paintParticles(g2);

            // 7. Mario Character
            paintMario(g2);

            // 8. Floating Notifications
            paintFloaters(g2);

            // 9. Screen Flash Juice
            if (screenFlashTicks > 0) {
                g2.setColor(screenFlashColor);
                g2.fillRect(0, 0, w, h);
            }

            // 10. Overlays
            if (isVictory) {
                paintVictoryScreen(g2, w, h);
            } else if (isGameOver) {
                paintGameOverScreen(g2, w, h);
            }

            g2.dispose();
        }

        private void updatePipeLayout(int w) {
            float spacing = (capacity == 4) ? 175f : 210f;
            float totalSpan = (capacity - 1) * spacing;
            float startX = (w - totalSpan) / 2f;

            for (int i = 0; i < capacity; i++) {
                pipes[i].x = startX + i * spacing;
                pipes[i].y = 430f;
            }

            // On initial layout, position Mario safely on the left platform in front of Pipe #0
            if (animTick <= 1 && marioX >= (pipes[0].x - pipes[0].w / 2f) - 30f) {
                marioX = Math.max(70f, (pipes[0].x - pipes[0].w / 2f) - 65f);
            }
        }

        private void paintCavernBackground(Graphics2D g2, int w, int h) {
            // Authentic Super Mario World 1-1 Sky Gradient
            GradientPaint skyGrad = new GradientPaint(
                    0, 0, new Color(55, 110, 225),
                    0, 520, new Color(105, 172, 255)
            );
            g2.setPaint(skyGrad);
            g2.fillRect(0, 0, w, 520);

            // Floating Super Mario Clouds
            paintClouds(g2);

            // Ground Platform Floor (y: 520..h)
            GradientPaint groundGrad = new GradientPaint(
                    0, 520, new Color(180, 76, 20),
                    0, h, new Color(110, 42, 12)
            );
            g2.setPaint(groundGrad);
            g2.fillRect(0, 520, w, h - 520);

            // Ground Brick Edging
            g2.setColor(new Color(240, 120, 48));
            g2.fillRect(0, 520, w, 5);
            g2.setColor(new Color(80, 28, 8));
            g2.fillRect(0, 525, w, 3);
            for (int x = 0; x < w; x += 32) {
                g2.setColor(new Color(130, 50, 16));
                g2.drawRect(x, 528, 32, 24);
                g2.drawRect(x + 16, 552, 32, 24);
            }
        }

        private void paintClouds(Graphics2D g2) {
            for (MarioCloud c : clouds) {
                drawMarioCloud(g2, c.x, c.y, c.puffs, c.scale);
            }
        }

        private void drawMarioCloud(Graphics2D g2, float cx, float cy, int puffs, float scale) {
            Graphics2D cg = (Graphics2D) g2.create();
            cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            cg.translate(cx, cy);
            cg.scale(scale, scale);

            int totalW = (puffs == 3) ? 96 : 72;
            int h = 30;
            int halfW = totalW / 2;

            // Soft cyan shadow behind bottom of cloud
            cg.setColor(new Color(175, 215, 250));
            cg.fillRoundRect(-halfW - 2, -h / 2 - 2, totalW + 4, h + 8, 22, 22);

            // Fluffy white puffs
            cg.setColor(Color.WHITE);
            cg.fillRoundRect(-halfW, -h / 2, totalW, h, 20, 20);

            if (puffs == 3) {
                // Left puff
                cg.fillOval(-halfW + 4, -h / 2 - 12, 28, 28);
                // Center puff (tallest)
                cg.fillOval(-18, -h / 2 - 20, 36, 36);
                // Right puff
                cg.fillOval(halfW - 32, -h / 2 - 12, 28, 28);
            } else {
                // Left puff
                cg.fillOval(-halfW + 6, -h / 2 - 14, 30, 30);
                // Right puff
                cg.fillOval(halfW - 36, -h / 2 - 14, 30, 30);
            }

            // Crisp dark outline (Mario arcade style)
            cg.setColor(new Color(24, 48, 80));
            cg.setStroke(new BasicStroke(2.2f));
            cg.drawRoundRect(-halfW, -h / 2, totalW, h, 20, 20);
            if (puffs == 3) {
                cg.drawArc(-halfW + 4, -h / 2 - 12, 28, 28, 45, 180);
                cg.drawArc(-18, -h / 2 - 20, 36, 36, 30, 150);
                cg.drawArc(halfW - 32, -h / 2 - 12, 28, 28, -25, 170);
            } else {
                cg.drawArc(-halfW + 6, -h / 2 - 14, 30, 30, 35, 175);
                cg.drawArc(halfW - 36, -h / 2 - 14, 30, 30, -15, 175);
            }

            // Iconic Mario Cloud Eyes (Two vertical black pills with white twinkle)
            cg.setColor(new Color(16, 24, 40));
            cg.fillRoundRect(-6, -4, 3, 8, 2, 2);
            cg.fillRoundRect(3, -4, 3, 8, 2, 2);

            cg.setColor(Color.WHITE);
            cg.fillRect(-6, -4, 2, 2);
            cg.fillRect(3, -4, 2, 2);

            cg.dispose();
        }

        private void paintHUD(Graphics2D g2, int w) {
            int hudY = 10;

            // Telemetry Box Left: Algorithm & Hit Rate
            int boxW = 340;
            g2.setColor(new Color(20, 14, 38, 235));
            g2.fillRoundRect(16, hudY, boxW, 40, 8, 8);
            g2.setColor(new Color(85, 60, 125));
            g2.drawRoundRect(16, hudY, boxW, 40, 8, 8);

            int totalProcessed = totalHits + totalFaults;
            double hitPct = (totalProcessed > 0) ? ((double) totalHits / totalProcessed * 100.0) : 0.0;

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString(String.format("CACHE // %s POLICY | CAPACITY: %d PIPES", algorithm, capacity), 26, hudY + 16);

            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            g2.setColor(Color.WHITE);
            g2.drawString(String.format("HITS: %d | FAULTS: %d | HIT RATE: %.1f%%", totalHits, totalFaults, hitPct), 26, hudY + 31);

            // Center Callout: Stream Step & Current Page
            int centerW = 280;
            int centerX = (w - centerW) / 2;
            g2.setColor(new Color(25, 16, 44, 235));
            g2.fillRoundRect(centerX, hudY, centerW, 40, 8, 8);
            g2.setColor(starModeTicks > 0 ? RetroTheme.ACCENT_YELLOW : new Color(180, 85, 240, 180));
            g2.drawRoundRect(centerX, hudY, centerW, 40, 8, 8);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(starModeTicks > 0 ? RetroTheme.ACCENT_YELLOW : RetroTheme.ACCENT_PINK);
            String callout = (starModeTicks > 0)
                    ? "★ STARMAN FEVER x2! ★"
                    : String.format("REF STREAM STEP %d / %d", Math.min(streamIndex + 1, stream.length), stream.length);
            g2.drawString(callout, centerX + 18, hudY + 16);

            g2.setFont(new Font("Consolas", Font.PLAIN, 9));
            g2.setColor(new Color(215, 205, 245));
            g2.drawString("HIT = AUTO COIN | FAULT = STOMP VICTIM PIPE!", centerX + 12, hudY + 31);

            // Right Box: Score & Lives
            int rightBoxW = 260;
            int rightBoxX = w - rightBoxW - 16;
            g2.setColor(new Color(20, 14, 38, 235));
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

        private void paintPageStreamRail(Graphics2D g2, int w) {
            int railY = 56;

            // Conveyor rail track
            g2.setColor(new Color(85, 70, 115));
            g2.fillRect(40, railY, w - 80, 5);
            g2.setColor(new Color(135, 120, 165));
            g2.fillRect(40, railY, w - 80, 2);

            // Mystery '?' Lucky Blocks for Upcoming Stream Pages
            int visibleCount = Math.min(8, stream.length - streamIndex);
            int blockSz = 44;
            int gap = 14;
            int totalRailW = visibleCount * blockSz + (visibleCount - 1) * gap;
            int startX = (w - totalRailW) / 2;

            for (int i = 0; i < visibleCount; i++) {
                int sIdx = streamIndex + i;
                int pageNum = stream[sIdx];
                int bx = startX + i * (blockSz + gap);
                int by = railY + 8;

                boolean isCurrent = (i == 0);

                if (isCurrent) {
                    // Floating golden bob animation for the active page
                    int bob = (int)(Math.sin(animTick * 0.3) * 4);
                    by += bob;

                    // Pulsing golden aura
                    float pulse = (float)(0.6 + 0.4 * Math.sin(animTick * 0.25));
                    g2.setColor(new Color(255, 215, 0, (int)(110 * pulse)));
                    g2.fillRoundRect(bx - 4, by - 4, blockSz + 8, blockSz + 8, 8, 8);
                }

                // Mystery Block Body
                Color blockColor = isCurrent ? new Color(245, 160, 24) : new Color(50, 36, 75);
                g2.setColor(blockColor);
                g2.fillRoundRect(bx, by, blockSz, blockSz, 6, 6);
                g2.setColor(isCurrent ? RetroTheme.ACCENT_YELLOW : new Color(85, 65, 120));
                g2.setStroke(new BasicStroke(isCurrent ? 2f : 1f));
                g2.drawRoundRect(bx, by, blockSz, blockSz, 6, 6);
                g2.setStroke(new BasicStroke(1f));

                // Rivets on block corners
                g2.setColor(isCurrent ? new Color(180, 100, 10) : new Color(30, 20, 45));
                g2.fillRect(bx + 3, by + 3, 2, 2);
                g2.fillRect(bx + blockSz - 5, by + 3, 2, 2);
                g2.fillRect(bx + 3, by + blockSz - 5, 2, 2);
                g2.fillRect(bx + blockSz - 5, by + blockSz - 5, 2, 2);

                // Page Item Sprite inside block
                BufferedImage item = PixelSprites.getMarioItem(pageNum, 24);
                if (item != null) {
                    g2.drawImage(item, bx + (blockSz - 24) / 2, by + 4, null);
                }

                // Page Number Tag
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(Color.WHITE);
                String pStr = "P" + pageNum;
                int strW = g2.getFontMetrics().stringWidth(pStr);
                g2.drawString(pStr, bx + (blockSz - strW) / 2, by + blockSz - 5);
            }
        }

        private void paintWarpPipes(Graphics2D g2) {
            for (int i = 0; i < capacity; i++) {
                PipeSlot p = pipes[i];
                int px = (int) (p.x - p.w / 2f);
                int py = (int) p.y;
                int pw = (int) p.w;
                int ph = (int) p.h;

                // 1. Eviction / Hit Warning Indicator (Clean floating text, no clunky square box)
                if (p.isHit) {
                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    g2.setColor(RetroTheme.ACCENT_GREEN);
                    String hitStr = "★ CACHE HIT! ★";
                    int sw = g2.getFontMetrics().stringWidth(hitStr);
                    g2.drawString(hitStr, px + (pw - sw) / 2, py - 34);
                } else if (p.isVictim) {
                    float pulse = (float)(0.7 + 0.3 * Math.sin(animTick * 0.35));
                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    g2.setColor(new Color(255, (int)(60 * pulse), 60));
                    String flushStr = String.format("! FLUSH %.1fs !", evictionTimer);
                    int sw = g2.getFontMetrics().stringWidth(flushStr);
                    g2.drawString(flushStr, px + (pw - sw) / 2, py - 34);
                }

                // 2. Eviction Warning Bar (When marked as victim)
                if (p.isVictim) {
                    int barW = pw - 10;
                    int barH = 4;
                    int barX = px + 5;
                    int barY = py - 26;

                    g2.setColor(new Color(20, 10, 15));
                    g2.fillRect(barX, barY, barW, barH);
                    float pct = Math.max(0f, evictionTimer / EVICTION_TIMEOUT);
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    g2.fillRect(barX, barY, (int)(barW * pct), barH);
                }

                // 3. Pipe Ambient Floor Shadow
                g2.setColor(new Color(0, 0, 0, 110));
                g2.fillOval(px - 6, py + ph - 8, pw + 12, 18);

                // 4. Warp Pipe Body (Classic 8-Bit Nintendo Emerald Green with Bevel)
                Color pipeGreenLight = new Color(0, 220, 40);
                Color pipeGreenMid = new Color(0, 168, 0);
                Color pipeGreenDark = new Color(0, 96, 0);

                // Pipe Stem
                int stemW = pw - 16;
                int stemX = px + 8;
                int stemY = py + 24;
                int stemH = ph - 24;

                g2.setColor(pipeGreenMid);
                g2.fillRect(stemX, stemY, stemW, stemH);
                // Highlight strip on stem
                g2.setColor(pipeGreenLight);
                g2.fillRect(stemX + 6, stemY, 14, stemH);
                // Shadow strip on stem
                g2.setColor(pipeGreenDark);
                g2.fillRect(stemX + stemW - 12, stemY, 12, stemH);

                // Pipe Rim (The wider top flange)
                g2.setColor(pipeGreenMid);
                g2.fillRoundRect(px, py, pw, 26, 6, 6);
                // Rim highlight
                g2.setColor(pipeGreenLight);
                g2.fillRect(px + 8, py, 16, 26);
                // Rim shadow
                g2.setColor(pipeGreenDark);
                g2.fillRect(px + pw - 14, py, 14, 26);
                // Rim border
                g2.setColor(new Color(0, 48, 0));
                g2.drawRoundRect(px, py, pw, 26, 6, 6);
                g2.drawRect(stemX, stemY, stemW, stemH);

                // Pipe Dark Mouth
                g2.setColor(new Color(8, 28, 8));
                g2.fillOval(px + 10, py + 2, pw - 20, 12);

                // 5. Resident Page Item Floating in Pipe
                if (p.page != -1) {
                    int itemBob = (int)(Math.sin(animTick * 0.2 + i) * 3);
                    BufferedImage pageItem = PixelSprites.getMarioItem(p.page, 32);
                    if (pageItem != null) {
                        g2.drawImage(pageItem, (int)(p.x - 16), py - 18 + itemBob, null);
                    }

                    // Floating Item Page Number Badge
                    g2.setColor(new Color(20, 14, 32, 220));
                    g2.fillRoundRect((int)(p.x - 14), py + 12, 28, 12, 3, 3);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Consolas", Font.BOLD, 9));
                    g2.drawString("P" + p.page, (int)(p.x - 8), py + 21);
                }

                // 6. Interaction Prompt when Mario is standing on top of pipe
                if (standingPipe == p) {
                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    if (p.isVictim) {
                        float pulse = (float)(0.7 + 0.3 * Math.sin(animTick * 0.4));
                        g2.setColor(new Color(255, (int)(230 * pulse), 0));
                        g2.drawString("▼ PRESS S / DOWN TO FLUSH! ▼", px - 22, py - 56);
                    } else if (p.page == -1) {
                        g2.setColor(new Color(0, 240, 255));
                        g2.drawString("▼ PRESS S / DOWN TO LOAD! ▼", px - 20, py - 56);
                    }
                } else if (p.isVictim) {
                    g2.setFont(new Font("Consolas", Font.BOLD, 9));
                    g2.setColor(new Color(255, 190, 80));
                    g2.drawString("▲ JUMP ON TOP TO FLUSH ▲", px - 6, py - 56);
                }
            }
        }

        private void paintGoombas(Graphics2D g2) {
            for (Goomba g : goombas) {
                int gx = (int) g.x;
                int gy = (int) g.y;

                if (g.isDead) {
                    // Squished Goomba
                    g2.setColor(new Color(180, 80, 20));
                    g2.fillOval(gx, gy + 14, 28, 10);
                    g2.setColor(Color.WHITE);
                    g2.drawLine(gx + 6, gy + 18, gx + 10, gy + 18);
                    g2.drawLine(gx + 18, gy + 18, gx + 22, gy + 18);
                } else {
                    // Walking 16-bit Goomba
                    int wBob = (animTick % 10 < 5) ? 0 : 2;

                    // Ambient shadow
                    g2.setColor(new Color(0, 0, 0, 90));
                    g2.fillOval(gx, gy + 22, 28, 6);

                    // Brown Mushroom Cap
                    g2.setColor(new Color(168, 70, 16));
                    g2.fillRoundRect(gx + 2, gy + 2, 24, 16, 8, 8);
                    // Tan Stem Face
                    g2.setColor(new Color(248, 208, 152));
                    g2.fillRoundRect(gx + 6, gy + 12, 16, 10, 4, 4);

                    // Angry Eyebrows & Eyes
                    g2.setColor(Color.BLACK);
                    g2.fillRect(gx + 8, gy + 13, 3, 4);
                    g2.fillRect(gx + 17, gy + 13, 3, 4);
                    g2.drawLine(gx + 7, gy + 12, gx + 11, gy + 14);
                    g2.drawLine(gx + 20, gy + 12, gx + 16, gy + 14);

                    // Little Black Feet
                    g2.setColor(Color.BLACK);
                    g2.fillOval(gx + 4 - wBob, gy + 20, 8, 6);
                    g2.fillOval(gx + 16 + wBob, gy + 20, 8, 6);
                }
            }
        }

        private void paintDashGhosts(Graphics2D g2) {
            for (DashGhost g : dashGhosts) {
                BufferedImage mario = PixelSprites.createMarioSprite(44);
                if (mario != null) {
                    Graphics2D gGhost = (Graphics2D) g2.create();
                    gGhost.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, g.alpha / 255f));
                    if (g.facingRight) {
                        gGhost.drawImage(mario, (int) g.x, (int) g.y, null);
                    } else {
                        gGhost.drawImage(mario, (int) g.x + 44, (int) g.y, -44, 44, null);
                    }
                    gGhost.dispose();
                }
            }
        }

        private void paintParticles(Graphics2D g2) {
            for (MarioParticle p : particles) {
                g2.setColor(p.color);
                int sz = (int) p.size;
                g2.fillOval((int) p.x - sz / 2, (int) p.y - sz / 2, sz, sz);
            }
        }

        private void paintMario(Graphics2D g2) {
            int mx = (int) marioX;
            int my = (int) marioY;

            // Ambient Shadow
            g2.setColor(new Color(0, 0, 0, 110));
            g2.fillOval(mx + 4, (int)(marioY + 40), 36, 10);

            // Sprite Rendering
            BufferedImage mario = PixelSprites.createMarioSprite(44);
            if (mario != null) {
                if (facingRight) {
                    g2.drawImage(mario, mx, my, null);
                } else {
                    g2.drawImage(mario, mx + 44, my, -44, 44, null);
                }
            }

            // Star Power Overhead Sparkles
            if (starModeTicks > 0) {
                g2.setColor(new Color(255, 230, 0));
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.drawString("★ STAR MODE ★", mx - 10, my - 6);
            }
        }

        private void paintFloaters(Graphics2D g2) {
            for (FloatingText f : floaters) {
                float alpha = Math.min(1f, f.life / 20f);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.setColor(new Color(0, 0, 0, 180));
                g2.drawString(f.text, f.x - 29, f.y + 1);
                g2.setColor(f.color);
                g2.drawString(f.text, f.x - 30, f.y);
            }
            g2.setComposite(AlphaComposite.SrcOver);
        }

        private void paintVictoryScreen(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(10, 6, 20, 225));
            g2.fillRect(0, 0, w, h);

            int boxW = 540;
            int boxH = 320;
            int bx = (w - boxW) / 2;
            int by = (h - boxH) / 2;

            g2.setColor(new Color(24, 16, 44));
            g2.fillRoundRect(bx, by, boxW, boxH, 16, 16);
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(bx, by, boxW, boxH, 16, 16);
            g2.setStroke(new BasicStroke(1f));

            g2.setFont(new Font("Consolas", Font.BOLD, 22));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("★ WORLD CLEAR! CACHE MASTER! ★", bx + 45, by + 46);

            g2.setFont(new Font("Consolas", Font.PLAIN, 12));
            g2.setColor(Color.WHITE);
            int totalProcessed = totalHits + totalFaults;
            double hitPct = (totalProcessed > 0) ? ((double) totalHits / totalProcessed * 100.0) : 0.0;

            g2.drawString(String.format("ALGORITHM: %s POLICY", algorithm), bx + 50, by + 95);
            g2.drawString(String.format("FINAL SCORE: %06d PTS", score), bx + 50, by + 125);
            g2.drawString(String.format("CACHE HITS: %d | PAGE FAULTS: %d", totalHits, totalFaults), bx + 50, by + 155);
            g2.drawString(String.format("HIT RATIO: %.2f%%", hitPct), bx + 50, by + 185);

            g2.setFont(new Font("Consolas", Font.BOLD, 13));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("PRESS [R] TO PLAY AGAIN   |   [ESC] TO RETURN", bx + 70, by + 265);
        }

        private void paintGameOverScreen(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(15, 6, 12, 230));
            g2.fillRect(0, 0, w, h);

            int boxW = 500;
            int boxH = 260;
            int bx = (w - boxW) / 2;
            int by = (h - boxH) / 2;

            g2.setColor(new Color(36, 16, 26));
            g2.fillRoundRect(bx, by, boxW, boxH, 16, 16);
            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect(bx, by, boxW, boxH, 16, 16);
            g2.setStroke(new BasicStroke(1f));

            g2.setFont(new Font("Consolas", Font.BOLD, 24));
            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.drawString("☠ GAME OVER // MEMORY THRASH ☠", bx + 35, by + 48);

            g2.setFont(new Font("Consolas", Font.PLAIN, 12));
            g2.setColor(Color.WHITE);
            g2.drawString(String.format("SCORE ACHIEVED: %06d PTS", score), bx + 50, by + 105);
            g2.drawString(String.format("STREAM CLEARED: %d / %d PAGES", streamIndex, stream.length), bx + 50, by + 135);

            g2.setFont(new Font("Consolas", Font.BOLD, 13));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("PRESS [R] TO RETRY   |   [ESC] TO EXIT", bx + 95, by + 215);
        }
    }
}
