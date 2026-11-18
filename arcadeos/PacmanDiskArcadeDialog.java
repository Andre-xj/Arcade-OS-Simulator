package arcadeos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * MISSION 07 // PAC-MAN DISK SEEK ARCADE (OPEN CYBER PLATTER ARENA)
 *
 * Open Platter Gameplay:
 * - Spacious 3-track Magnetic Platter Arena with vertical seek crossovers (No tight maze walls or choke points)!
 * - Total freedom of movement: Pac-Man can easily steer around any cylinder without accidental collisions!
 * - Continuously roaming Ghosts ("Aliens"): Blinky, Pinky, Inky & Clyde patrol tracks and vertical lanes seamlessly without EVER getting stuck!
 * - Strict Step-by-Step Disk Scheduling: Must seek and chomp the active target cylinder first (FCFS / SSTF / SCAN / LOOK)!
 * - Touching a wrong cylinder triggers an ERROR penalty (-50 PTS, screen shake, red flash, buzzer)!
 * - Pac-Dots along tracks with authentic "waka-waka" SFX, 4 corner Power Energizers, and Fruit Cherries bonus!
 */
public class PacmanDiskArcadeDialog extends JDialog {

    // Configuration & Disk State
    private int[] initialQueue;
    private int initialHead = 53;
    private int currentHead = 53;
    private int maxCylinder = 199;
    private String algorithm = "SSTF";
    private int totalHeadMovement = 0;

    // Step-by-Step Algorithm Schedule Order
    private final List<Integer> scheduledOrder = new ArrayList<>();
    private int currentStep = 0;
    private int wrongTouchCooldown = 0;

    // Cylinder Sector Pellets positioned across the 3 Platter Tracks
    private static class CylinderSector {
        int cylinder;
        float x, y;
        int trackIndex;

        CylinderSector(int cyl, int trackIndex) {
            this.cylinder = cyl;
            this.trackIndex = trackIndex;
        }
    }
    private final List<CylinderSector> cylinderSectors = new ArrayList<>();
    private final List<CylinderSector> servicedSectors = new ArrayList<>();

    // Pac-Dots along the tracks
    private final List<Point2D.Float> dots = new ArrayList<>();

    // 4 Corner Power Pellets (Energizers)
    private static class PowerPellet {
        float x, y;
        boolean eaten = false;
        PowerPellet(float x, float y) { this.x = x; this.y = y; }
    }
    private final List<PowerPellet> powerPellets = new ArrayList<>();

    // Pac-Man Player State (Starts at the bottom center of the arena)
    private float pacX = 500f;
    private float pacY = 555f;
    private float pacVx = 0f;
    private float pacVy = 0f;
    private int currentDir = 3; // 0=Right, 1=Down, 2=Left, 3=Up (facing up towards arena)
    private float mouthAngle = 35f;
    private boolean isDashing = false;
    private int invincibleTicks = 90; // Respawn grace period (blinking / invulnerable)

    // Input States
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean moveUp = false;
    private boolean moveDown = false;

    // Ghosts ("Aliens") that continuously roam the platter tracks
    private static class PlatterGhost {
        float x, y;
        float vx, vy;
        int type; // 0=Blinky(Red), 1=Pinky(Pink), 2=Inky(Cyan), 3=Clyde(Orange)
        int currentTrack;
        boolean isFrightened = false;
        boolean isEyes = false;
        int switchCooldown = 0;

        PlatterGhost(float x, float y, int type, int track) {
            this.x = x; this.y = y; this.type = type; this.currentTrack = track;
            this.vx = (type % 2 == 0) ? -2.2f : 2.2f;
            this.vy = 0f;
        }
    }
    private final List<PlatterGhost> ghosts = new ArrayList<>();

    // Cherry Bonus
    private float cherryX = -100f;
    private float cherryY = -100f;
    private boolean cherryActive = false;
    private int cherrySpawnTimer = 350;

    // Juice & FX
    private static class FloatingText {
        float x, y;
        String text;
        Color color;
        int life = 50;
        FloatingText(float x, float y, String text, Color color) {
            this.x = x; this.y = y; this.text = text; this.color = color;
        }
    }
    private final List<FloatingText> floaters = new ArrayList<>();

    private static class PacParticle {
        float x, y, vx, vy;
        Color color;
        int life;
        float size;
        PacParticle(float x, float y, float vx, float vy, Color color, int life, float size) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.color = color; this.life = life; this.size = size;
        }
    }
    private final List<PacParticle> particles = new ArrayList<>();

    // Game Loop & Stats
    private javax.swing.Timer gameTimer;
    private PlatterArcadeCanvas canvas;
    private int score = 0;
    private int lives = 3;
    private int combo = 0;
    private int frightenedTicks = 0;
    private int ghostsEatenStreak = 0;
    private int animTick = 0;
    private boolean isGameOver = false;
    private boolean isVictory = false;
    private boolean soundEnabled = true;
    private int screenShakeTicks = 0;
    private int screenFlashTicks = 0;
    private Color screenFlashColor = new Color(255, 0, 50, 110);

    public PacmanDiskArcadeDialog(JFrame owner, int[] queueInput, int initHeadInput, int maxCylInput, String algoInput) {
        super(owner, "MISSION 07 // PAC-MAN DISK SEEK ARCADE", false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);

        this.initialHead = (initHeadInput > 0) ? initHeadInput : 53;
        this.currentHead = this.initialHead;
        this.maxCylinder = (maxCylInput > 0) ? maxCylInput : 199;
        this.algorithm = (algoInput != null && !algoInput.isEmpty()) ? algoInput : "SSTF";

        if (queueInput != null && queueInput.length > 0) {
            this.initialQueue = queueInput.clone();
        } else {
            this.initialQueue = new int[]{98, 183, 37, 122, 14, 124, 65, 67};
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
        root.setBackground(new Color(10, 8, 24));

        // Top Retro Arcade Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(true);
        header.setBackground(new Color(18, 12, 34));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(75, 45, 125)),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));

        JPanel leftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftHeader.setOpaque(false);

        JLabel lblTitle = new JLabel("PAC-MAN // DISK SEEK ARCADE");
        lblTitle.setFont(new Font("Consolas", Font.BOLD, 14));
        lblTitle.setForeground(RetroTheme.ACCENT_YELLOW);
        lblTitle.setIcon(new ImageIcon(PixelSprites.createPacman(16, 35f, true)));
        lblTitle.setIconTextGap(8);
        leftHeader.add(lblTitle);

        JLabel lblAlgoBadge = new JLabel("[" + algorithm + " SEEK POLICY]");
        lblAlgoBadge.setFont(new Font("Consolas", Font.BOLD, 11));
        lblAlgoBadge.setForeground(RetroTheme.ACCENT_CYAN);
        leftHeader.add(lblAlgoBadge);
        header.add(leftHeader, BorderLayout.WEST);

        JLabel lblTag = new JLabel("[ STEP-BY-STEP SEEK: CHOMP HIGHLIGHTED TARGET FIRST! | WRONG = ERROR ]");
        lblTag.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblTag.setForeground(new Color(255, 220, 150));
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

        canvas = new PlatterArcadeCanvas();
        canvas.setFocusable(true);
        root.add(canvas, BorderLayout.CENTER);

        setContentPane(root);
    }

    private void initKeyboard() {
        InputMap im = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = canvas.getActionMap();

        bindPressRelease(im, am, KeyEvent.VK_LEFT,  () -> { moveLeft = true; currentDir = 2; }, () -> moveLeft = false);
        bindPressRelease(im, am, KeyEvent.VK_A,     () -> { moveLeft = true; currentDir = 2; }, () -> moveLeft = false);

        bindPressRelease(im, am, KeyEvent.VK_RIGHT, () -> { moveRight = true; currentDir = 0; }, () -> moveRight = false);
        bindPressRelease(im, am, KeyEvent.VK_D,     () -> { moveRight = true; currentDir = 0; }, () -> moveRight = false);

        bindPressRelease(im, am, KeyEvent.VK_UP,    () -> { moveUp = true; currentDir = 3; }, () -> moveUp = false);
        bindPressRelease(im, am, KeyEvent.VK_W,     () -> { moveUp = true; currentDir = 3; }, () -> moveUp = false);

        bindPressRelease(im, am, KeyEvent.VK_DOWN,  () -> { moveDown = true; currentDir = 1; }, () -> moveDown = false);
        bindPressRelease(im, am, KeyEvent.VK_S,     () -> { moveDown = true; currentDir = 1; }, () -> moveDown = false);

        bindPressRelease(im, am, KeyEvent.VK_SHIFT, () -> isDashing = true, () -> isDashing = false);
        bindAction(im, am, KeyEvent.VK_R, "KEY_RESTART", this::restartGame);
        bindAction(im, am, KeyEvent.VK_ESCAPE, "KEY_ESC", this::dispose);
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

    private void bindAction(InputMap im, ActionMap am, int keyCode, String name, Runnable action) {
        im.put(KeyStroke.getKeyStroke(keyCode, 0, false), name);
        am.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void computeScheduledOrder() {
        scheduledOrder.clear();
        List<Integer> pool = new ArrayList<>();
        for (int q : initialQueue) pool.add(q);

        int head = initialHead;

        if ("SSTF".equalsIgnoreCase(algorithm)) {
            while (!pool.isEmpty()) {
                int bestIdx = 0;
                int minDist = Integer.MAX_VALUE;
                for (int i = 0; i < pool.size(); i++) {
                    int dist = Math.abs(pool.get(i) - head);
                    if (dist < minDist) {
                        minDist = dist;
                        bestIdx = i;
                    }
                }
                int chosen = pool.remove(bestIdx);
                scheduledOrder.add(chosen);
                head = chosen;
            }
        } else if ("SCAN".equalsIgnoreCase(algorithm) || "LOOK".equalsIgnoreCase(algorithm)) {
            List<Integer> up = new ArrayList<>();
            List<Integer> down = new ArrayList<>();
            for (int q : pool) {
                if (q >= head) up.add(q);
                else down.add(q);
            }
            Collections.sort(up);
            Collections.sort(down, Collections.reverseOrder());
            scheduledOrder.addAll(up);
            scheduledOrder.addAll(down);
        } else {
            // FCFS default: Original queue order
            scheduledOrder.addAll(pool);
        }
    }

    private int getCurrentRequiredCylinder() {
        if (currentStep >= 0 && currentStep < scheduledOrder.size()) {
            return scheduledOrder.get(currentStep);
        }
        return -1;
    }

    private void restartGame() {
        score = 0;
        combo = 0;
        lives = 3;
        totalHeadMovement = 0;
        currentHead = initialHead;
        currentStep = 0;
        wrongTouchCooldown = 0;
        isGameOver = false;
        isVictory = false;
        frightenedTicks = 0;
        ghostsEatenStreak = 0;

        computeScheduledOrder();

        int w = (canvas != null && canvas.getWidth() > 300) ? canvas.getWidth() : 1000;
        pacX = w / 2f;
        pacY = 555f;
        pacVx = 0f;
        pacVy = 0f;
        currentDir = 3; // Facing UP from the bottom
        invincibleTicks = 90;

        layoutPlatterElements();

        floaters.clear();
        particles.clear();

        playSfx(494, 80, 0.4f);
        playSfx(587, 80, 0.45f);
    }

    private void layoutPlatterElements() {
        int w = (canvas != null && canvas.getWidth() > 300) ? canvas.getWidth() : 1000;
        float trackMinX = 90f;
        float trackMaxX = w - 90f;
        float trackW = trackMaxX - trackMinX;

        float[] trackYs = new float[]{230f, 360f, 490f};

        // Layout Cylinder Sectors with massive breathing room across the 3 tracks
        cylinderSectors.clear();
        servicedSectors.clear();

        for (int i = 0; i < initialQueue.length; i++) {
            int cyl = initialQueue[i];
            int trackIdx = i % 3;
            CylinderSector cs = new CylinderSector(cyl, trackIdx);
            float ratio = Math.max(0f, Math.min(1f, (float) cyl / (float) maxCylinder));
            cs.x = trackMinX + ratio * trackW;
            cs.y = trackYs[trackIdx];
            cylinderSectors.add(cs);
        }

        // 4 Corner Power Energizers (Placed in outer corners of dot field)
        powerPellets.clear();
        powerPellets.add(new PowerPellet(trackMinX + 25f, 165f));
        powerPellets.add(new PowerPellet(trackMaxX - 25f, 165f));
        powerPellets.add(new PowerPellet(trackMinX + 25f, 555f));
        powerPellets.add(new PowerPellet(trackMaxX - 25f, 555f));

        // Layout Pac-Dots: Pure dot-grid paths with zero solid lines (Authentic Pac-Man style)
        dots.clear();
        float[] dotRowYs = new float[]{165f, 230f, 295f, 360f, 425f, 490f, 555f};
        float[] dotColXs = new float[]{
                trackMinX + 25f,
                trackMinX + trackW * 0.25f,
                w / 2f,
                trackMinX + trackW * 0.75f,
                trackMaxX - 25f
        };

        // 1. Horizontal dot paths along all 7 lanes
        for (float ry : dotRowYs) {
            for (float rx = trackMinX + 25f; rx <= trackMaxX - 25f; rx += 22f) {
                addDotSafe(rx, ry);
            }
        }

        // 2. Vertical connector dot paths along all 5 columns
        for (float cx : dotColXs) {
            for (float cy = dotRowYs[0]; cy <= dotRowYs[dotRowYs.length - 1]; cy += 22f) {
                addDotSafe(cx, cy);
            }
        }

        // Pac-Man starts safely at the bottom center of the arena
        pacX = w / 2f;
        pacY = 555f;
        pacVx = 0f;
        pacVy = 0f;
        currentDir = 3; // Facing UP
        invincibleTicks = 90; // Grace period on game start

        // Ghosts ("Aliens") spread across upper tracks with active velocities!
        resetGhostPositions(w, trackMinX, trackMaxX, trackYs);

        cherryActive = false;
        cherrySpawnTimer = 300;
    }

    private void resetGhostPositions(int w, float trackMinX, float trackMaxX, float[] trackYs) {
        ghosts.clear();
        ghosts.add(new PlatterGhost(trackMaxX - 140f, trackYs[0], 0, 0)); // Blinky (Red) on Track #1 (y=230)
        ghosts.add(new PlatterGhost(trackMinX + 180f, trackYs[1], 1, 1)); // Pinky (Pink) on Track #2 (y=360)
        ghosts.add(new PlatterGhost(trackMaxX - 260f, trackYs[0], 2, 0)); // Inky (Cyan) on Track #1 (y=230)
        ghosts.add(new PlatterGhost(w / 2f, trackYs[1], 3, 1));           // Clyde (Orange) on Track #2 (y=360)
    }

    private void addDotSafe(float x, float y) {
        // Prevent duplicate overlapping dots at column/row intersections
        for (Point2D.Float existing : dots) {
            if (Point2D.distance(x, y, existing.x, existing.y) < 11f) return;
        }
        // Avoid placing dot directly under corner Power Pellets
        for (PowerPellet pp : powerPellets) {
            if (Point2D.distance(x, y, pp.x, pp.y) < 22f) return;
        }
        // Avoid placing dot directly under Cylinder Sectors
        for (CylinderSector cs : cylinderSectors) {
            if (Point2D.distance(x, y, cs.x, cs.y) < 26f) return;
        }
        // Avoid placing dot right on cherry spawn center
        if (Point2D.distance(x, y, 500f, 360f) < 20f) return;

        dots.add(new Point2D.Float(x, y));
    }

    private void startGameLoop() {
        gameTimer = new javax.swing.Timer(16, e -> {
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
        if (canvas == null) return;
        int w = canvas.getWidth();
        int h = canvas.getHeight();

        float trackMinX = 90f;
        float trackMaxX = w - 90f;
        float[] trackYs = new float[]{230f, 360f, 490f};

        if (wrongTouchCooldown > 0) wrongTouchCooldown--;
        if (invincibleTicks > 0) invincibleTicks--;

        // 1. Pac-Man Ultra-Smooth 2D Steering
        float speed = isDashing ? 7.8f : 5.2f;
        float targetVx = 0f;
        float targetVy = 0f;

        if (moveLeft) targetVx -= speed;
        if (moveRight) targetVx += speed;
        if (moveUp) targetVy -= speed;
        if (moveDown) targetVy += speed;

        pacVx = pacVx * 0.72f + targetVx * 0.28f;
        pacVy = pacVy * 0.72f + targetVy * 0.28f;

        pacX += pacVx;
        pacY += pacVy;

        // Boundaries within the platter arena (Allow Pac-Man full roam along all dot rows)
        pacX = Math.max(trackMinX + 15f, Math.min(trackMaxX - 15f, pacX));
        pacY = Math.max(155f, Math.min(565f, pacY));

        float moveMag = (float) Math.hypot(pacVx, pacVy);
        if (moveMag > 0.4f) {
            mouthAngle = Math.abs((animTick % 12) - 6) * 8.5f;
            if (Math.abs(pacVx) > Math.abs(pacVy)) {
                currentDir = (pacVx > 0) ? 0 : 2; // Right or Left
            } else {
                currentDir = (pacVy > 0) ? 1 : 3; // Down or Up
            }
        } else {
            mouthAngle = 18f;
        }

        // 2. Chomp Dots
        dots.removeIf(dot -> {
            float d = (float) Point2D.distance(pacX, pacY, dot.x, dot.y);
            if (d < 18f) {
                score += 10;
                if (animTick % 6 == 0) {
                    playSfx((animTick % 12 < 6) ? 440 : 520, 25, 0.25f);
                }
                return true;
            }
            return false;
        });

        // 3. Power Energizers
        for (PowerPellet pp : powerPellets) {
            if (!pp.eaten) {
                float d = (float) Point2D.distance(pacX, pacY, pp.x, pp.y);
                if (d < 24f) {
                    pp.eaten = true;
                    frightenedTicks = 420; // ~7 seconds
                    ghostsEatenStreak = 0;
                    for (PlatterGhost g : ghosts) {
                        if (!g.isEyes) g.isFrightened = true;
                    }
                    score += 50;
                    addFloating(pp.x, pp.y - 15, "POWER ENERGIZER! ⚡", RetroTheme.ACCENT_YELLOW);
                    playSfx(880, 110, 0.5f);
                }
            }
        }

        // 4. Strict Step-by-Step Disk Scheduling Logic:
        // Must seek the current target cylinder first! If wrong cylinder touched -> ERROR!
        int targetCyl = getCurrentRequiredCylinder();
        CylinderSector touchedSector = null;

        for (CylinderSector cs : cylinderSectors) {
            float dist = (float) Point2D.distance(pacX, pacY, cs.x, cs.y);
            if (dist < 26f) {
                touchedSector = cs;
                break;
            }
        }

        if (touchedSector != null) {
            if (touchedSector.cylinder == targetCyl) {
                // CORRECT TARGET CYLINDER!
                cylinderSectors.remove(touchedSector);
                servicedSectors.add(touchedSector);

                int seekDist = Math.abs(touchedSector.cylinder - currentHead);
                totalHeadMovement += seekDist;
                currentHead = touchedSector.cylinder;
                currentStep++;

                combo++;
                int pts = 150 + combo * 25;
                score += pts;

                addFloating(touchedSector.x, touchedSector.y - 18,
                        String.format("✓ STEP %d: C%d SERVICED! +%d", currentStep, touchedSector.cylinder, pts),
                        RetroTheme.ACCENT_YELLOW);

                // Golden spark particles
                for (int i = 0; i < 18; i++) {
                    particles.add(new PacParticle(
                            touchedSector.x, touchedSector.y,
                            (float)(Math.random() - 0.5) * 5f,
                            (float)(Math.random() - 0.5) * 5f,
                            RetroTheme.ACCENT_YELLOW, 25, 5.5f
                    ));
                }

                playSfx(659, 90, 0.5f);
                playSfx(880, 140, 0.6f);

                // Check Victory
                if (currentStep >= scheduledOrder.size()) {
                    isVictory = true;
                    playSfx(587, 100, 0.5f);
                    playSfx(784, 150, 0.6f);
                    playSfx(1046, 300, 0.7f);
                }
            } else if (wrongTouchCooldown == 0) {
                // WRONG CYLINDER TOUCHED!
                // Trigger ERROR: Penalize score, screen shake, red flash, error buzzer!
                wrongTouchCooldown = 40;
                score = Math.max(0, score - 50);
                combo = 0;
                screenShakeTicks = 12;
                screenFlashTicks = 8;
                screenFlashColor = new Color(255, 0, 50, 110);

                addFloating(touchedSector.x, touchedSector.y - 18,
                        String.format("❌ ERROR: WRONG TRACK! SEEK C%d FIRST! (-50)", targetCyl),
                        RetroTheme.ACCENT_PINK);

                playSfx(180, 140, 0.65f);
                playSfx(130, 180, 0.7f);
            }
        }

        // 5. Cherry Bonus
        if (!cherryActive && cherrySpawnTimer > 0) {
            cherrySpawnTimer--;
            if (cherrySpawnTimer <= 0) {
                cherryActive = true;
                cherryX = w / 2f;
                cherryY = trackYs[1];
            }
        }

        if (cherryActive) {
            float dist = (float) Point2D.distance(pacX, pacY, cherryX, cherryY);
            if (dist < 26f) {
                cherryActive = false;
                cherrySpawnTimer = 450;
                score += 300;
                addFloating(cherryX, cherryY - 18, "🍒 CHERRY BONUS +300! 🍒", RetroTheme.ACCENT_PINK);
                playSfx(784, 90, 0.5f);
                playSfx(1046, 120, 0.6f);
            }
        }

        // 6. Frightened Timer Countdown
        if (frightenedTicks > 0) {
            frightenedTicks--;
            if (frightenedTicks <= 0) {
                for (PlatterGhost g : ghosts) g.isFrightened = false;
            }
        }

        // 7. Ghosts ("Aliens") Continuously Roaming Movement (NEVER STUCK!)
        float trackW = trackMaxX - trackMinX;
        float[] crossoverXs = new float[]{
                trackMinX + 25f,
                trackMinX + trackW * 0.25f,
                w / 2f,
                trackMinX + trackW * 0.75f,
                trackMaxX - 25f
        };

        for (PlatterGhost g : ghosts) {
            float gSpeed = g.isEyes ? 5.5f : (g.isFrightened ? 1.6f : 2.5f);

            if (g.switchCooldown > 0) g.switchCooldown--;

            // Move ghost horizontally along current rail
            g.x += (g.vx > 0 ? 1 : -1) * gSpeed;

            // Bounce at left/right edges of rail
            if (g.x < trackMinX + 15f) {
                g.x = trackMinX + 15f;
                g.vx = Math.abs(g.vx);
            } else if (g.x > trackMaxX - 15f) {
                g.x = trackMaxX - 15f;
                g.vx = -Math.abs(g.vx);
            }

            // Chance to switch tracks at vertical crossover points
            if (g.switchCooldown == 0 && !g.isEyes) {
                for (float cx : crossoverXs) {
                    if (Math.abs(g.x - cx) < 8f) {
                        // At crossover lane: decide whether to change track
                        boolean changeTrack = (Math.random() < 0.35);
                        if (changeTrack) {
                            int targetTrack = (g.currentTrack == 0) ? 1 : ((g.currentTrack == 2) ? 1 : (Math.random() < 0.5 ? 0 : 2));
                            g.currentTrack = targetTrack;
                            g.switchCooldown = 80;
                            break;
                        }
                    }
                }
            }

            // Smoothly glide towards current track Y level
            float targetY = trackYs[g.currentTrack];
            g.y = g.y * 0.88f + targetY * 0.12f;

            // If ghost is in Eyes mode, zoom to center and revive
            if (g.isEyes) {
                if (Math.abs(g.x - w / 2f) < 15f && Math.abs(g.y - trackYs[1]) < 15f) {
                    g.isEyes = false;
                    g.isFrightened = false;
                    g.currentTrack = 1;
                }
            }

            // Pac-Man collision with Ghost
            float collisionDist = (float) Point2D.distance(pacX, pacY, g.x, g.y);
            if (collisionDist < 26f) {
                if (g.isFrightened && !g.isEyes) {
                    // Chomp Ghost!
                    g.isEyes = true;
                    g.isFrightened = false;
                    ghostsEatenStreak++;
                    int ghostPts = 200 * (1 << (ghostsEatenStreak - 1));
                    score += ghostPts;
                    addFloating(g.x, g.y - 18, "GHOST +" + ghostPts + "!", RetroTheme.ACCENT_CYAN);
                    playSfx(698, 90, 0.6f);
                    playSfx(932, 110, 0.65f);
                } else if (!g.isEyes && invincibleTicks == 0) {
                    // Pac-Man hit by ghost!
                    takeDamage();
                    break;
                }
            }
        }
    }

    private void takeDamage() {
        lives--;
        screenShakeTicks = 14;
        screenFlashTicks = 12;
        screenFlashColor = new Color(255, 30, 60, 130);
        combo = 0;
        playSfx(220, 160, 0.65f);
        playSfx(164, 220, 0.7f);

        if (lives <= 0) {
            isGameOver = true;
        } else {
            // Respawn Pac-Man safely at bottom center
            int w = (canvas != null && canvas.getWidth() > 300) ? canvas.getWidth() : 1000;
            float trackMinX = 90f;
            float trackMaxX = w - 90f;
            float[] trackYs = new float[]{230f, 360f, 490f};

            pacX = w / 2f;
            pacY = 555f;
            pacVx = 0f;
            pacVy = 0f;
            currentDir = 3; // Facing UP
            invincibleTicks = 110; // ~1.8 seconds grace period

            // Reset ghosts back to upper tracks to give player breathing room!
            resetGhostPositions(w, trackMinX, trackMaxX, trackYs);

            addFloating(pacX, pacY - 20, "SEEK HEAD CRASH! -1 LIFE", RetroTheme.ACCENT_PINK);
        }
    }

    private void updateEffects() {
        floaters.removeIf(f -> {
            f.y -= 0.6f;
            f.life--;
            return f.life <= 0;
        });

        particles.removeIf(p -> {
            p.x += p.vx;
            p.y += p.vy;
            p.size = Math.max(0f, p.size - 0.14f);
            p.life--;
            return p.life <= 0;
        });

        if (screenShakeTicks > 0) screenShakeTicks--;
        if (screenFlashTicks > 0) screenFlashTicks--;
    }

    private void addFloating(float x, float y, String text, Color color) {
        floaters.add(new FloatingText(x, y, text, color));
    }

    private void playSfx(int freq, int durationMs, float vol) {
        if (!soundEnabled) return;
        new Thread(() -> {
            try {
                byte[] buf = new byte[durationMs * 8];
                for (int i = 0; i < buf.length; i++) {
                    double angle = i / (8000.0 / freq) * 2.0 * Math.PI;
                    buf[i] = (byte) (Math.sin(angle) * 127.0 * vol);
                }
                javax.sound.sampled.AudioFormat af = new javax.sound.sampled.AudioFormat(8000f, 8, 1, true, false);
                javax.sound.sampled.SourceDataLine sdl = javax.sound.sampled.AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();
                sdl.write(buf, 0, buf.length);
                sdl.drain();
                sdl.close();
            } catch (Exception ignored) {}
        }).start();
    }

    @Override
    public void dispose() {
        if (gameTimer != null && gameTimer.isRunning()) {
            gameTimer.stop();
        }
        super.dispose();
    }

    // =========================================================================
    // SPACIOUS CYBER PLATTER ARENA CANVAS
    // =========================================================================
    private class PlatterArcadeCanvas extends JPanel {
        public PlatterArcadeCanvas() {
            setBackground(new Color(8, 6, 20));
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (screenShakeTicks > 0) {
                int ox = (int)((Math.random() - 0.5) * 8);
                int oy = (int)((Math.random() - 0.5) * 8);
                g2.translate(ox, oy);
            }

            int w = getWidth();
            int h = getHeight();

            float trackMinX = 90f;
            float trackMaxX = w - 90f;
            float[] trackYs = new float[]{230f, 360f, 490f};

            // 1. Cyber Platter Atmosphere & Background
            paintPlatterAtmosphere(g2, w, h, trackMinX, trackMaxX, trackYs);

            // 2. Top HUD Bar
            paintHUD(g2, w);

            // 3. Overhead Step-by-Step Queue Rail
            paintQueueRail(g2, w);

            // 4. Pac-Dots
            paintDots(g2);

            // 5. 4 Corner Power Energizers
            paintPowerPellets(g2);

            // 6. Cherry Bonus
            if (cherryActive) {
                BufferedImage cherry = PixelSprites.createPacmanCherry(28);
                if (cherry != null) {
                    g2.drawImage(cherry, (int) cherryX - 14, (int) cherryY - 14, null);
                }
            }

            // 7. Cylinder Sector Pellets (C14, C37, C65...) with Optimal Reticle
            paintCylinderSectors(g2);

            // 8. Active Roaming Ghosts ("Aliens")
            paintGhosts(g2);

            // 9. Particles
            for (PacParticle p : particles) {
                g2.setColor(p.color);
                int sz = (int) p.size;
                g2.fillOval((int) p.x - sz / 2, (int) p.y - sz / 2, sz, sz);
            }

            // 10. Pac-Man Character
            paintPacman(g2);

            // 11. Floating Text Popups
            paintFloaters(g2);

            // 12. Red Screen Flash on Error / Damage
            if (screenFlashTicks > 0) {
                g2.setColor(screenFlashColor);
                g2.fillRect(0, 0, w, h);
            }

            // 13. Overlays
            if (isVictory) {
                paintVictoryScreen(g2, w, h);
            } else if (isGameOver) {
                paintGameOverScreen(g2, w, h);
            }

            g2.dispose();
        }

        private void paintPlatterAtmosphere(Graphics2D g2, int w, int h, float minX, float maxX, float[] trackYs) {
            // Clean retro dark arcade background
            GradientPaint gp = new GradientPaint(
                    0, 0, new Color(10, 7, 22),
                    0, h, new Color(16, 12, 32)
            );
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            // Outer Classic Pac-Man Arcade Frame (Thin Double Blue Rounded Border - NO solid lines inside!)
            g2.setColor(new Color(33, 33, 222));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRoundRect((int) minX - 14, 142, (int)(maxX - minX) + 28, 436, 16, 16);
            g2.setColor(new Color(0, 0, 160, 140));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect((int) minX - 18, 138, (int)(maxX - minX) + 36, 444, 20, 20);

            // Subtle Track labels on left margin (outside the dot field)
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            g2.setColor(new Color(110, 95, 160));
            g2.drawString("TRACK 1", minX - 68, 234);
            g2.drawString("TRACK 2", minX - 68, 364);
            g2.drawString("TRACK 3", minX - 68, 494);

            // Current Head Marker at Top (Non-solid, elegant pointer with faint dashed guide)
            float ratio = Math.max(0f, Math.min(1f, (float) currentHead / (float) maxCylinder));
            float headX = minX + ratio * (maxX - minX);

            // Subtle dashed vertical guide line (non-solid)
            Stroke oldStroke = g2.getStroke();
            Stroke dashed = new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4.0f, 6.0f}, 0.0f);
            g2.setStroke(dashed);
            g2.setColor(new Color(255, 230, 0, 50));
            g2.drawLine((int) headX, 150, (int) headX, 570);
            g2.setStroke(oldStroke);

            // Top Pointer Tag
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            String headStr = "▼ HEAD: #" + currentHead;
            int hw = g2.getFontMetrics().stringWidth(headStr);
            g2.drawString(headStr, headX - hw / 2, 138);
        }

        private void paintHUD(Graphics2D g2, int w) {
            int hudY = 8;

            // 1. Left Card: 1UP & Score
            g2.setColor(new Color(20, 14, 38));
            g2.fillRoundRect(14, hudY, 260, 42, 6, 6);
            g2.setColor(new Color(60, 45, 95));
            g2.drawRoundRect(14, hudY, 260, 42, 6, 6);

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.drawString("1UP // SCORE", 24, hudY + 16);
            g2.setFont(new Font("Consolas", Font.BOLD, 15));
            g2.setColor(Color.WHITE);
            g2.drawString(String.format("%06d", score), 24, hudY + 34);

            // 2. Center Card: Policy & Disk Seek Head Traversal
            int centerW = 340;
            int centerX = (w - centerW) / 2;
            g2.setColor(new Color(20, 14, 38));
            g2.fillRoundRect(centerX, hudY, centerW, 42, 6, 6);
            g2.setColor(new Color(60, 45, 95));
            g2.drawRoundRect(centerX, hudY, centerW, 42, 6, 6);

            int targetCyl = getCurrentRequiredCylinder();
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("POLICY: " + algorithm + " | HEAD: #" + currentHead, centerX + 14, hudY + 16);

            if (targetCyl != -1) {
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                String stepTxt = String.format("STEP %d/%d ➜ MUST CHOMP C%d FIRST!", (currentStep + 1), scheduledOrder.size(), targetCyl);
                g2.drawString(stepTxt, centerX + 14, hudY + 34);
            } else {
                g2.setFont(new Font("Consolas", Font.BOLD, 12));
                g2.setColor(RetroTheme.ACCENT_GREEN);
                g2.drawString("ALL DISK CYLINDERS SERVICED!", centerX + 14, hudY + 34);
            }

            // 3. Right Card: Seek Traversed & Lives
            int rightW = 260;
            int rightX = w - rightW - 14;
            g2.setColor(new Color(20, 14, 38));
            g2.fillRoundRect(rightX, hudY, rightW, 42, 6, 6);
            g2.setColor(new Color(60, 45, 95));
            g2.drawRoundRect(rightX, hudY, rightW, 42, 6, 6);

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("SEEK TRAVERSED: " + totalHeadMovement + " CYL", rightX + 14, hudY + 16);

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(Color.WHITE);
            g2.drawString("LIVES:", rightX + 14, hudY + 33);
            for (int i = 0; i < 3; i++) {
                if (i < lives) {
                    BufferedImage pacIcon = PixelSprites.createPacman(14, 30f, true);
                    if (pacIcon != null) {
                        g2.drawImage(pacIcon, rightX + 58 + i * 20, hudY + 22, null);
                    }
                } else {
                    g2.setColor(new Color(80, 50, 70));
                    g2.drawString("✕", rightX + 60 + i * 20, hudY + 33);
                }
            }
        }

        private void paintQueueRail(Graphics2D g2, int w) {
            int railY = 56;
            int total = scheduledOrder.size();
            int itemW = 54;
            int gap = 8;
            int totalW = total * itemW + (total - 1) * gap;
            int startX = Math.max(20, (w - totalW) / 2);

            g2.setColor(new Color(36, 24, 56));
            g2.fillRect(startX - 8, railY + 9, totalW + 16, 4);

            for (int i = 0; i < total; i++) {
                int cyl = scheduledOrder.get(i);
                int bx = startX + i * (itemW + gap);
                int by = railY;

                boolean isDone = (i < currentStep);
                boolean isCurrent = (i == currentStep);

                Color bg = isDone ? new Color(18, 42, 24) : (isCurrent ? new Color(75, 58, 14) : new Color(28, 18, 44));
                Color border = isDone ? RetroTheme.ACCENT_GREEN : (isCurrent ? RetroTheme.ACCENT_YELLOW : new Color(65, 45, 95));

                g2.setColor(bg);
                g2.fillRoundRect(bx, by, itemW, 22, 4, 4);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(isCurrent ? 2f : 1f));
                g2.drawRoundRect(bx, by, itemW, 22, 4, 4);
                g2.setStroke(new BasicStroke(1f));

                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(isDone ? new Color(140, 255, 170) : (isCurrent ? Color.YELLOW : Color.WHITE));
                String tag = "#" + cyl + (isDone ? " ✓" : (isCurrent ? " ★" : ""));
                int strW = g2.getFontMetrics().stringWidth(tag);
                g2.drawString(tag, bx + (itemW - strW) / 2, by + 15);
            }
        }

        private void paintDots(Graphics2D g2) {
            // Authentic Pac-Man Pellet color (peach/cream)
            g2.setColor(new Color(255, 184, 151));
            for (Point2D.Float dot : dots) {
                g2.fillRect((int) dot.x - 2, (int) dot.y - 2, 4, 4);
            }
        }

        private void paintPowerPellets(Graphics2D g2) {
            for (PowerPellet pp : powerPellets) {
                if (!pp.eaten) {
                    float pulse = (float)(0.7 + 0.3 * Math.sin(animTick * 0.3));
                    int sz = (int)(16 * pulse);
                    g2.setColor(new Color(255, 220, 150, 70));
                    g2.fillOval((int) pp.x - sz / 2 - 2, (int) pp.y - sz / 2 - 2, sz + 4, sz + 4);
                    g2.setColor(new Color(255, 240, 200));
                    g2.fillOval((int) pp.x - sz / 2, (int) pp.y - sz / 2, sz, sz);
                }
            }
        }

        private void paintCylinderSectors(Graphics2D g2) {
            int targetCyl = getCurrentRequiredCylinder();

            for (CylinderSector cs : cylinderSectors) {
                boolean isCurrentTarget = (cs.cylinder == targetCyl);

                if (isCurrentTarget) {
                    // Pulsing golden targeting reticle
                    float pulse = (float)(0.6 + 0.4 * Math.sin(animTick * 0.28));
                    int reticleSz = 48;

                    g2.setColor(new Color(255, 215, 0, (int)(80 * pulse)));
                    g2.fillOval((int) cs.x - reticleSz / 2, (int) cs.y - reticleSz / 2, reticleSz, reticleSz);

                    g2.setColor(Color.YELLOW);
                    g2.drawOval((int) cs.x - reticleSz / 2, (int) cs.y - reticleSz / 2, reticleSz, reticleSz);
                    // Reticle Crosshairs
                    g2.drawLine((int)(cs.x - 28), (int) cs.y, (int)(cs.x - 14), (int) cs.y);
                    g2.drawLine((int)(cs.x + 14), (int) cs.y, (int)(cs.x + 28), (int) cs.y);
                    g2.drawLine((int) cs.x, (int)(cs.y - 28), (int) cs.x, (int)(cs.y - 14));
                    g2.drawLine((int) cs.x, (int)(cs.y + 14), (int) cs.x, (int)(cs.y + 28));
                }

                // Sector Platter Pellet Body
                int diskSz = 26;
                g2.setColor(isCurrentTarget ? Color.YELLOW : RetroTheme.ACCENT_CYAN);
                g2.fillOval((int) cs.x - diskSz / 2, (int) cs.y - diskSz / 2, diskSz, diskSz);
                g2.setColor(Color.WHITE);
                g2.fillOval((int) cs.x - diskSz / 4, (int) cs.y - diskSz / 4, diskSz / 2, diskSz / 2);

                // Cylinder Tag
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(isCurrentTarget ? Color.YELLOW : Color.WHITE);
                String tag = "C" + cs.cylinder;
                int strW = g2.getFontMetrics().stringWidth(tag);
                g2.drawString(tag, (int) cs.x - strW / 2, (int) cs.y - diskSz / 2 - 4);
            }
        }

        private void paintGhosts(Graphics2D g2) {
            int ghostSz = 28;

            for (PlatterGhost g : ghosts) {
                if (g.isEyes) {
                    // Floating white sclera with pupils
                    g2.setColor(Color.WHITE);
                    g2.fillOval((int) g.x - 7, (int) g.y - 4, 6, 8);
                    g2.fillOval((int) g.x + 1, (int) g.y - 4, 6, 8);
                    g2.setColor(new Color(0, 0, 200));
                    g2.fillRect((int) g.x - 5, (int) g.y - 2, 3, 3);
                    g2.fillRect((int) g.x + 3, (int) g.y - 2, 3, 3);
                } else if (frightenedTicks > 0 && g.isFrightened) {
                    // Blue Frightened Ghost
                    boolean flashWhite = (frightenedTicks < 110 && animTick % 10 < 5);
                    Color blueCol = flashWhite ? Color.WHITE : new Color(33, 33, 222);

                    g2.setColor(blueCol);
                    g2.fillRoundRect((int) g.x - ghostSz / 2, (int) g.y - ghostSz / 2, ghostSz, (int)(ghostSz * 0.85f), 10, 10);
                    int wBob = (animTick % 8 < 4) ? 0 : 2;
                    for (int s = 0; s < 3; s++) {
                        g2.fillOval((int) g.x - ghostSz / 2 + s * (ghostSz / 3), (int) (g.y + ghostSz * 0.25f) - wBob, ghostSz / 3, ghostSz / 3);
                    }
                    g2.setColor(flashWhite ? Color.RED : Color.WHITE);
                    g2.fillRect((int) g.x - 5, (int) g.y - 3, 3, 3);
                    g2.fillRect((int) g.x + 2, (int) g.y - 3, 3, 3);
                    g2.drawLine((int) g.x - 6, (int) g.y + 4, (int) g.x + 6, (int) g.y + 4);
                } else {
                    // Authentic Pac-Man Ghosts (Blinky, Pinky, Inky, Clyde)
                    BufferedImage ghImg = PixelSprites.createPacmanGhost(ghostSz, g.type);
                    if (ghImg != null) {
                        g2.drawImage(ghImg, (int) g.x - ghostSz / 2, (int) g.y - ghostSz / 2, null);
                    }
                }
            }
        }

        private void paintPacman(Graphics2D g2) {
            int pacSz = 32;
            BufferedImage pac = PixelSprites.createPacman(pacSz, mouthAngle, true);
            if (pac != null) {
                Graphics2D gPac = (Graphics2D) g2.create();

                if (invincibleTicks > 0) {
                    float alpha = (animTick % 8 < 4) ? 0.45f : 0.85f;
                    gPac.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                }

                double angleRad = currentDir * (Math.PI / 2.0);
                AffineTransform at = new AffineTransform();
                at.translate(pacX, pacY);
                at.rotate(angleRad);
                at.translate(-pacSz / 2.0, -pacSz / 2.0);

                gPac.drawImage(pac, at, null);
                gPac.dispose();
            }
        }

        private void paintFloaters(Graphics2D g2) {
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            for (FloatingText f : floaters) {
                g2.setColor(new Color(f.color.getRed(), f.color.getGreen(), f.color.getBlue(), Math.min(255, f.life * 5)));
                int strW = g2.getFontMetrics().stringWidth(f.text);
                g2.drawString(f.text, f.x - strW / 2, f.y);
            }
        }

        private void paintVictoryScreen(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(6, 4, 18, 225));
            g2.fillRect(0, 0, w, h);

            int boxW = 540;
            int boxH = 300;
            int bx = (w - boxW) / 2;
            int by = (h - boxH) / 2;

            g2.setColor(new Color(22, 14, 38));
            g2.fillRoundRect(bx, by, boxW, boxH, 12, 12);
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(bx, by, boxW, boxH, 12, 12);
            g2.setStroke(new BasicStroke(1f));

            g2.setFont(new Font("Consolas", Font.BOLD, 22));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            String title = "★ DISK SEEK STAGE COMPLETE! ★";
            g2.drawString(title, bx + (boxW - g2.getFontMetrics().stringWidth(title)) / 2, by + 45);

            g2.setFont(new Font("Consolas", Font.BOLD, 13));
            g2.setColor(Color.WHITE);
            String sub = "All cylinder sectors serviced in " + algorithm + " order!";
            g2.drawString(sub, bx + (boxW - g2.getFontMetrics().stringWidth(sub)) / 2, by + 75);

            g2.setFont(new Font("Consolas", Font.PLAIN, 12));
            g2.setColor(new Color(210, 200, 240));
            int startY = by + 115;
            g2.drawString("• FINAL SCORE: " + score + " PTS", bx + 60, startY);
            g2.drawString("• TOTAL HEAD TRAVERSAL: " + totalHeadMovement + " CYLINDERS", bx + 60, startY + 26);
            double avgSeek = (servicedSectors.isEmpty()) ? 0.0 : (double) totalHeadMovement / servicedSectors.size();
            g2.drawString(String.format("• AVERAGE SEEK DISTANCE: %.2f CYL/REQ", avgSeek), bx + 60, startY + 52);

            String rank = (totalHeadMovement < 240) ? "S-RANK (OPTIMAL SEEKER!)" : ((totalHeadMovement < 380) ? "A-RANK (FAST DISK)" : "B-RANK (SOLID RUN)");
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.setFont(new Font("Consolas", Font.BOLD, 13));
            g2.drawString("PERFORMANCE: " + rank, bx + 60, startY + 84);

            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            String restartMsg = "PRESS [ R ] TO PLAY AGAIN  |  [ ESC ] TO RETURN TO SIMULATOR";
            g2.drawString(restartMsg, bx + (boxW - g2.getFontMetrics().stringWidth(restartMsg)) / 2, by + 265);
        }

        private void paintGameOverScreen(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(6, 4, 18, 225));
            g2.fillRect(0, 0, w, h);

            int boxW = 500;
            int boxH = 260;
            int bx = (w - boxW) / 2;
            int by = (h - boxH) / 2;

            g2.setColor(new Color(36, 14, 26));
            g2.fillRoundRect(bx, by, boxW, boxH, 12, 12);
            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(bx, by, boxW, boxH, 12, 12);
            g2.setStroke(new BasicStroke(1f));

            g2.setFont(new Font("Consolas", Font.BOLD, 22));
            g2.setColor(RetroTheme.ACCENT_PINK);
            String title = "☠ DISK HEAD CRASH! GAME OVER ☠";
            g2.drawString(title, bx + (boxW - g2.getFontMetrics().stringWidth(title)) / 2, by + 50);

            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(Color.WHITE);
            String sub = "The Ghosts caught Pac-Man before all sectors were serviced!";
            g2.drawString(sub, bx + (boxW - g2.getFontMetrics().stringWidth(sub)) / 2, by + 85);

            g2.setFont(new Font("Consolas", Font.PLAIN, 12));
            g2.setColor(new Color(220, 200, 230));
            g2.drawString("• FINAL SCORE: " + score, bx + 60, by + 125);
            g2.drawString("• CYLINDERS CHOMPED: " + servicedSectors.size() + " / " + scheduledOrder.size(), bx + 60, by + 150);
            g2.drawString("• HEAD TRAVERSAL: " + totalHeadMovement + " CYLINDERS", bx + 60, by + 175);

            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            String prompt = "PRESS [ R ] TO RETRY  |  [ ESC ] TO EXIT";
            g2.drawString(prompt, bx + (boxW - g2.getFontMetrics().stringWidth(prompt)) / 2, by + 225);
        }
    }
}
