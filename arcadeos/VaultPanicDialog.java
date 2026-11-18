package arcadeos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * NAMCO // METEOR DEFENDER: DEADLOCK ASSAULT [16-BIT RETRO ARCADE]
 * -----------------------------------------------------------------------------
 * Authentic Space Shooter for Dijkstra's Banker's Algorithm:
 * - Processes are INCOMING METEORS descending from the top of space.
 * - Meteors glow GREEN (SOLVENT: Need <= Vault) or RED (INSOLVENT: Need > Vault).
 * - Shooting a Green Meteor safely destroys it:
 *   ALLOCATED RESOURCES ARE AUTOMATICALLY ABSORBED INTO THE VAULT!
 *   Homing energy orbs auto-fly into the player ship with zero manual pickup hassle!
 * - Newly acquired resources instantly update the Vault and turn newly-affordable
 *   meteors GREEN in real-time!
 * - Shooting a Red Meteor triggers a DEADLOCK VIOLATION (-1 Shield).
 */
public class VaultPanicDialog extends JDialog {

    public enum GameMode {
        GALVIN("GALVIN 5P", "CLASSIC 5-PROCESS METEOR SHOWER", "Standard textbook scenario: P1 -> P3 -> P4 -> P0 -> P2"),
        DEADLOCK_TRAP("DEADLOCK TRAP", "TRICKY GREEDY METEORS", "High-demand rogue meteors: One mistake causes instant Deadlock!"),
        ENDLESS("ENDLESS SHOWER", "ENDLESS SURVIVAL WAVES", "Endless waves of process meteors with scaling resource rewards!");

        public final String code;
        public final String title;
        public final String rule;

        GameMode(String c, String t, String r) {
            this.code = c;
            this.title = t;
            this.rule = r;
        }
    }

    public static class ProcessMeteor {
        public String pid;
        public int[] alloc;
        public int[] max;
        public int[] need;
        public float x, y;
        public float vx, vy;
        public float radius = 38.0f;
        public float rotation = 0.0f;
        public float rotSpeed = 0.02f;
        public boolean cleared = false;
        public Color meteorColor;
        public int avatarIndex;

        public ProcessMeteor(String pid, int[] alloc, int[] max, int[] need,
                             float x, float y, float vx, float vy, Color color, int avatarIdx) {
            this.pid = pid;
            this.alloc = Arrays.copyOf(alloc, 3);
            this.max = Arrays.copyOf(max, 3);
            this.need = Arrays.copyOf(need, 3);
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.meteorColor = color;
            this.avatarIndex = avatarIdx;
            this.rotSpeed = (float) (Math.random() * 0.04 - 0.02);
        }

        public boolean canFund(int[] vault) {
            return need[0] <= vault[0] && need[1] <= vault[1] && need[2] <= vault[2];
        }
    }

    public static class PlayerTorpedo {
        public float x, y;
        public float speed = 14.0f;
        public PlayerTorpedo(float x, float y) {
            this.x = x; this.y = y;
        }
    }

    public static class HomingResourceOrb {
        public float x, y;
        public float vx, vy;
        public int type; // 0=A, 1=B, 2=C
        public int amount;
        public Color color;
        public boolean arrived = false;

        public HomingResourceOrb(float x, float y, int type, int amount, Color color) {
            this.x = x; this.y = y;
            this.type = type;
            this.amount = amount;
            this.color = color;
            float ang = (float) (Math.random() * Math.PI * 2);
            float spd = (float) (Math.random() * 3 + 2);
            this.vx = (float) Math.cos(ang) * spd;
            this.vy = (float) Math.sin(ang) * spd;
        }

        public void updateTowards(float targetX, float targetY) {
            float dx = targetX - x;
            float dy = targetY - y;
            float dist = (float) Math.hypot(dx, dy);

            if (dist < 26.0f) {
                arrived = true;
            } else {
                float pull = 0.85f;
                vx += (dx / dist) * pull;
                vy += (dy / dist) * pull;
                vx *= 0.92f;
                vy *= 0.92f;
                x += vx;
                y += vy;
            }
        }
    }

    public static class SpaceParticle {
        public float x, y, vx, vy;
        public Color color;
        public int life, maxLife;
        public SpaceParticle(float x, float y, float vx, float vy, Color color, int life) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.color = color; this.life = life; this.maxLife = life;
        }
        public void update() {
            x += vx; y += vy; life--;
        }
    }

    public static class FloatingAlert {
        public String text;
        public float x, y, vy;
        public Color color;
        public int life;
        public FloatingAlert(String text, float x, float y, Color color) {
            this.text = text; this.x = x; this.y = y; this.vy = -1.2f;
            this.color = color; this.life = 45;
        }
    }

    public static class Star {
        public float x, y, speed;
        public int colIdx;
        public Star(float x, float y, float speed, int colIdx) {
            this.x = x; this.y = y; this.speed = speed; this.colIdx = colIdx;
        }
    }

    private final BankersPanel parentPanel;
    private final List<BankersPanel.ProcessData> sourceProcesses;
    private final int[] startAvailable;

    private GameMode currentMode = GameMode.GALVIN;
    private int[] currentVault = {3, 3, 2};

    // Player State
    private float playerX = 480;
    private static final float PLAYER_Y = 550;
    private static final float SHIP_SPEED = 8.5f;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private int lives = 3;
    private int score = 0;
    private int combo = 1;
    private int smartBombs = 3;
    private boolean isGameOver = false;
    private boolean isVictory = false;
    private int screenShake = 0;

    // Discovered Safe Sequence
    private final List<String> discoveredSequence = new ArrayList<>();

    // Entities
    private final List<ProcessMeteor> meteors = new ArrayList<>();
    private final List<PlayerTorpedo> torpedoes = new ArrayList<>();
    private final List<HomingResourceOrb> homingOrbs = new ArrayList<>();
    private final List<SpaceParticle> particles = new ArrayList<>();
    private final List<FloatingAlert> floaters = new ArrayList<>();
    private final List<Star> stars = new ArrayList<>();

    private ArcadeSpaceCanvas canvas;
    private javax.swing.Timer gameLoop;

    private String feedbackBanner = "MISSION READY: DESTROY GREEN SOLVENT METEORS (NEED <= VAULT)!";
    private Color bannerColor = RetroTheme.ACCENT_CYAN;
    private JLabel lblRadarBadge;
    private JLabel lblRadarMessage;
    private JLabel lblControlsGuide;
    private JLabel lblRuleFormula;

    public VaultPanicDialog(JFrame parent, BankersPanel parentPanel,
                            List<BankersPanel.ProcessData> procs, int[] avail) {
        super(parent, "NAMCO // SPACE SHOOT: DEADLOCK ASSAULT [16-BIT RETRO ARCADE]", true);
        this.parentPanel = parentPanel;
        this.sourceProcesses = (procs != null && !procs.isEmpty()) ? procs : getDefaultProcesses();
        this.startAvailable = (avail != null) ? Arrays.copyOf(avail, 3) : new int[]{3, 3, 2};

        setSize(1040, 730);
        setResizable(false);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(new Color(6, 10, 16));
        setLayout(new BorderLayout());

        // Starfield
        Random r = new Random();
        for (int i = 0; i < 110; i++) {
            stars.add(new Star(r.nextInt(1020), r.nextInt(650), r.nextFloat() * 1.6f + 0.4f, r.nextInt(3)));
        }

        // Top Control Bar
        add(createTopControlHeader(), BorderLayout.NORTH);

        // Center Arcade Canvas
        canvas = new ArcadeSpaceCanvas();
        add(canvas, BorderLayout.CENTER);

        // Bottom Controls Bar
        add(createBottomControlsBar(), BorderLayout.SOUTH);

        // Keyboard & Mouse Setup
        setupControls();

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

    private List<BankersPanel.ProcessData> getDefaultProcesses() {
        List<BankersPanel.ProcessData> list = new ArrayList<>();
        list.add(new BankersPanel.ProcessData("P0", new int[]{0, 1, 0}, new int[]{7, 5, 3}));
        list.add(new BankersPanel.ProcessData("P1", new int[]{2, 0, 0}, new int[]{3, 2, 2}));
        list.add(new BankersPanel.ProcessData("P2", new int[]{3, 0, 2}, new int[]{9, 0, 2}));
        list.add(new BankersPanel.ProcessData("P3", new int[]{2, 1, 1}, new int[]{2, 2, 2}));
        list.add(new BankersPanel.ProcessData("P4", new int[]{0, 0, 2}, new int[]{4, 3, 3}));
        return list;
    }

    // =========================================================================
    // 1. TOP CONTROL HEADER
    // =========================================================================
    private JPanel createTopControlHeader() {
        JPanel bar = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(16, 22, 34));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(40, 60, 85));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        JLabel titleLbl = new JLabel("🚀 SPACE SHOOT // DEADLOCK DEFENSE ARCADE");
        titleLbl.setFont(new Font("Consolas", Font.BOLD, 12));
        titleLbl.setForeground(RetroTheme.ACCENT_YELLOW);

        // Center Mode Buttons
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        tabs.setOpaque(false);
        for (GameMode m : GameMode.values()) {
            JButton btn = RetroTheme.createRetroButton(m.code, RetroTheme.BTN_DARK, RetroTheme.TEXT_WHITE);
            btn.setFont(new Font("Consolas", Font.BOLD, 10));
            btn.addActionListener(e -> {
                this.currentMode = m;
                resetGameSession();
                canvas.requestFocusInWindow();
            });
            tabs.add(btn);
        }

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

        bar.add(titleLbl, BorderLayout.WEST);
        bar.add(tabs, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // =========================================================================
    // 2. BOTTOM CONTROLS & RADAR LOG
    // =========================================================================
    private JPanel createBottomControlsBar() {
        JPanel bar = new JPanel(new GridLayout(2, 1, 0, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(12, 16, 24));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(35, 50, 70));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16));

        JPanel row1 = new JPanel(new BorderLayout(12, 0));
        row1.setOpaque(false);

        JPanel row1Left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1Left.setOpaque(false);
        lblRadarBadge = new JLabel("⚡ RADAR:");
        lblRadarBadge.setFont(new Font("Consolas", Font.BOLD, 11));
        lblRadarBadge.setForeground(RetroTheme.ACCENT_CYAN);

        lblRadarMessage = new JLabel(feedbackBanner);
        lblRadarMessage.setFont(new Font("Consolas", Font.BOLD, 11));
        lblRadarMessage.setForeground(bannerColor);
        row1Left.add(lblRadarBadge);
        row1Left.add(lblRadarMessage);

        lblRuleFormula = new JLabel("BANKER'S RULE: Shoot GREEN Meteors (Need <= Vault)! Resources Auto-Absorb!");
        lblRuleFormula.setFont(new Font("Consolas", Font.BOLD, 11));
        lblRuleFormula.setForeground(RetroTheme.ACCENT_GREEN);

        row1.add(row1Left, BorderLayout.WEST);
        row1.add(lblRuleFormula, BorderLayout.EAST);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        row2.setOpaque(false);
        lblControlsGuide = new JLabel("CONTROLS: [A]/[D] or Mouse to Move Ship  •  [SPACE] or Left-Click to Fire  •  [Q] Smart Bomb  •  [R] Restart");
        lblControlsGuide.setFont(new Font("Consolas", Font.BOLD, 11));
        lblControlsGuide.setForeground(new Color(160, 205, 240));
        row2.add(lblControlsGuide);

        bar.add(row1);
        bar.add(row2);
        return bar;
    }

    private void updateBottomBannerUI() {
        if (lblRadarMessage != null) {
            String msg = feedbackBanner;
            if (msg.length() > 65) msg = msg.substring(0, 62) + "...";
            lblRadarMessage.setText(msg);
            lblRadarMessage.setForeground(bannerColor);
        }
    }

    // =========================================================================
    // 3. CONTROLS SETUP
    // =========================================================================
    private void setupControls() {
        InputMap im = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = canvas.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "LEFT_P");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "LEFT_P");
        am.put("LEFT_P", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { moveLeft = true; } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0, true), "LEFT_R");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), "LEFT_R");
        am.put("LEFT_R", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { moveLeft = false; } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "RIGHT_P");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "RIGHT_P");
        am.put("RIGHT_P", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { moveRight = true; } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0, true), "RIGHT_R");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), "RIGHT_R");
        am.put("RIGHT_R", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { moveRight = false; } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "FIRE");
        am.put("FIRE", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { fireTorpedo(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0), "BOMB");
        am.put("BOMB", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { triggerSmartBomb(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "RESTART");
        am.put("RESTART", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { resetGameSession(); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "EXIT");
        am.put("EXIT", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { dispose(); } });

        // Mouse motion support
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                playerX = Math.max(30, Math.min(canvas.getWidth() - 60, e.getX() - 15));
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                playerX = Math.max(30, Math.min(canvas.getWidth() - 60, e.getX() - 15));
            }
        });

        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    triggerSmartBomb();
                } else {
                    fireTorpedo();
                }
            }
        });
    }

    // =========================================================================
    // 4. RESET & INITIALIZATION
    // =========================================================================
    private void resetGameSession() {
        meteors.clear();
        torpedoes.clear();
        homingOrbs.clear();
        particles.clear();
        floaters.clear();
        discoveredSequence.clear();

        currentVault = Arrays.copyOf(startAvailable, 3);
        playerX = 480;
        moveLeft = false;
        moveRight = false;
        lives = 3;
        score = 0;
        combo = 1;
        smartBombs = 3;
        isGameOver = false;
        isVictory = false;
        screenShake = 0;

        List<BankersPanel.ProcessData> procs = (currentMode == GameMode.DEADLOCK_TRAP) ?
                createTrapFleet() : sourceProcesses;

        Color[] colors = {
                RetroTheme.ACCENT_CYAN, RetroTheme.ACCENT_GREEN, RetroTheme.ACCENT_YELLOW,
                RetroTheme.ACCENT_PINK, new Color(180, 100, 255), new Color(255, 150, 50)
        };

        // Spawn descending process meteors across the top
        int count = procs.size();
        float spacingX = (1000.0f - 160.0f) / Math.max(1, count - 1);

        for (int i = 0; i < count; i++) {
            BankersPanel.ProcessData p = procs.get(i);
            float mx = 80.0f + i * spacingX;
            float my = 155.0f + (i % 2 == 0 ? 0 : 35.0f); // staggered
            float vy = 0.38f; // Slow, enjoyable descent
            meteors.add(new ProcessMeteor(p.pid, p.alloc, p.max, p.need, mx, my, 0, vy,
                    colors[i % colors.length], i % 6));
        }

        feedbackBanner = "MODE: " + currentMode.title + " // TARGET GREEN SOLVENT METEORS (NEED <= VAULT)!";
        bannerColor = RetroTheme.ACCENT_CYAN;
        updateBottomBannerUI();
    }

    private List<BankersPanel.ProcessData> createTrapFleet() {
        List<BankersPanel.ProcessData> list = new ArrayList<>();
        list.add(new BankersPanel.ProcessData("T0", new int[]{2, 0, 0}, new int[]{6, 4, 3}));
        list.add(new BankersPanel.ProcessData("T1", new int[]{1, 1, 1}, new int[]{2, 2, 2}));
        list.add(new BankersPanel.ProcessData("T2", new int[]{0, 2, 0}, new int[]{5, 5, 5}));
        list.add(new BankersPanel.ProcessData("T3", new int[]{2, 1, 1}, new int[]{3, 3, 2}));
        list.add(new BankersPanel.ProcessData("T4", new int[]{1, 0, 2}, new int[]{5, 4, 4}));
        return list;
    }

    // =========================================================================
    // 5. ACTION TRIGGERS (TORPEDO & SMART BOMB)
    // =========================================================================
    private void fireTorpedo() {
        if (isGameOver || isVictory) return;
        torpedoes.add(new PlayerTorpedo(playerX + 6, PLAYER_Y - 8));
        torpedoes.add(new PlayerTorpedo(playerX + 24, PLAYER_Y - 8));

        for (int i = 0; i < 4; i++) {
            particles.add(new SpaceParticle(playerX + 15, PLAYER_Y,
                    (float) (Math.random() * 2 - 1), (float) (Math.random() * 2 + 1),
                    RetroTheme.ACCENT_CYAN, 12));
        }
    }

    private void triggerSmartBomb() {
        if (isGameOver || isVictory) return;
        if (smartBombs <= 0) {
            feedbackBanner = "NO SMART BOMBS REMAINING!";
            bannerColor = RetroTheme.ACCENT_PINK;
            updateBottomBannerUI();
            return;
        }

        smartBombs--;
        screenShake = 14;

        // Auto-destroy the first available SOLVENT meteor!
        boolean foundSolvent = false;
        for (ProcessMeteor m : meteors) {
            if (!m.cleared && m.canFund(currentVault)) {
                handleMeteorHit(m);
                foundSolvent = true;
                break;
            }
        }

        if (foundSolvent) {
            feedbackBanner = "⚡ SMART BOMB DETONATED! Solvent meteor safely disintegrated & resources absorbed!";
            bannerColor = RetroTheme.ACCENT_YELLOW;
        } else {
            feedbackBanner = "⚡ SMART BOMB DETONATED! Shockwave cleared incoming debris!";
            bannerColor = RetroTheme.ACCENT_CYAN;
        }
        updateBottomBannerUI();
    }

    // =========================================================================
    // 6. 60 FPS PHYSICS & COLLISION ENGINE
    // =========================================================================
    private void updateGamePhysics() {
        if (isGameOver || isVictory) {
            canvas.repaint();
            return;
        }

        if (screenShake > 0) screenShake--;

        // Move Stars
        for (Star s : stars) {
            s.y += s.speed;
            if (s.y > 650) {
                s.y = 0;
                s.x = new Random().nextInt(1020);
            }
        }

        // Steer Player Ship
        if (moveLeft) playerX = Math.max(30, playerX - SHIP_SPEED);
        if (moveRight) playerX = Math.min(970, playerX + SHIP_SPEED);

        // Move Torpedoes Upward
        for (int i = torpedoes.size() - 1; i >= 0; i--) {
            PlayerTorpedo t = torpedoes.get(i);
            t.y -= t.speed;
            if (t.y < 135) torpedoes.remove(i);
        }

        // Move Homing Resource Orbs (Auto-home straight into player ship!)
        for (int i = homingOrbs.size() - 1; i >= 0; i--) {
            HomingResourceOrb orb = homingOrbs.get(i);
            orb.updateTowards(playerX + 15, PLAYER_Y);

            // Particles trailing orb
            particles.add(new SpaceParticle(orb.x, orb.y, 0, 0, orb.color, 8));

            if (orb.arrived) {
                // Auto absorbed!
                score += 50;
                triggerExplosion(orb.x, orb.y, orb.color, 8);
                homingOrbs.remove(i);
            }
        }

        // Move Particles
        for (int i = particles.size() - 1; i >= 0; i--) {
            SpaceParticle p = particles.get(i);
            p.update();
            if (p.life <= 0) particles.remove(i);
        }

        // Move Floaters
        for (int i = floaters.size() - 1; i >= 0; i--) {
            FloatingAlert f = floaters.get(i);
            f.y += f.vy;
            f.life--;
            if (f.life <= 0) floaters.remove(i);
        }

        // Update Meteors & Check Torpedo Hits
        boolean allCleared = true;

        for (ProcessMeteor m : meteors) {
            if (m.cleared) continue;
            allCleared = false;

            // Slow gentle descent & spin
            m.y += m.vy;
            m.rotation += m.rotSpeed;

            // Check if meteor reached Defense Line
            if (m.y >= PLAYER_Y - 25) {
                if (m.canFund(currentVault)) {
                    // Safe auto-dock / auto-clear
                    handleMeteorHit(m);
                } else {
                    // Insolvent meteor crashed into defense perimeter!
                    m.cleared = true;
                    lives--;
                    screenShake = 16;
                    triggerExplosion(m.x, m.y, RetroTheme.ACCENT_PINK, 30);
                    floaters.add(new FloatingAlert("PERIMETER BREACH! -1 SHIELD", m.x, m.y, RetroTheme.ACCENT_PINK));
                    feedbackBanner = "🚨 DEADLOCK BREACH! Insolvent meteor " + m.pid + " breached the perimeter! (-1 Shield)";
                    bannerColor = RetroTheme.ACCENT_PINK;
                    updateBottomBannerUI();

                    if (lives <= 0) {
                        isGameOver = true;
                        feedbackBanner = "💀 DEFENSE CRUSHED: ALL SHIELDS DEPLETED. SYSTEM DEADLOCK!";
                    }
                }
                continue;
            }

            // Check Torpedo Collision
            for (int ti = torpedoes.size() - 1; ti >= 0; ti--) {
                PlayerTorpedo t = torpedoes.get(ti);
                float dist = (float) Math.hypot(t.x - m.x, t.y - m.y);

                if (dist <= m.radius + 6) {
                    torpedoes.remove(ti);
                    handleMeteorHit(m);
                    break;
                }
            }
        }

        // Victory Check
        if (allCleared && !isVictory && !isGameOver) {
            isVictory = true;
            score += 500;
            AppState.addScore(500);
            feedbackBanner = "🎉 100% SAFE SEQUENCE SECURED! ALL PROCESS METEORS DISPATCHED (+500 XP)!";
            bannerColor = RetroTheme.ACCENT_GREEN;
            updateBottomBannerUI();
            triggerExplosion(510, 300, RetroTheme.ACCENT_YELLOW, 60);
        }

        canvas.repaint();
    }

    private void handleMeteorHit(ProcessMeteor m) {
        if (m.canFund(currentVault)) {
            // SAFE DISPATCH!
            m.cleared = true;
            combo++;
            int pts = 200 * combo;
            score += pts;
            AppState.addScore(pts);
            discoveredSequence.add(m.pid);

            // Asteroid explosion
            screenShake = 8;
            triggerExplosion(m.x, m.y, RetroTheme.ACCENT_GREEN, 30);
            floaters.add(new FloatingAlert("+" + pts + " SAFE DISPATCH!", m.x, m.y - 15, RetroTheme.ACCENT_GREEN));

            // AUTOMATIC RESOURCE ABSORPTION (Instantly adds to vault!)
            Color[] resCols = {RetroTheme.ACCENT_YELLOW, RetroTheme.ACCENT_CYAN, RetroTheme.ACCENT_PINK};
            for (int r = 0; r < 3; r++) {
                if (m.alloc[r] > 0) {
                    currentVault[r] += m.alloc[r]; // INSTANT AUTO-ADD!

                    // Spawn visual homing orbs that zip into ship
                    for (int k = 0; k < m.alloc[r]; k++) {
                        homingOrbs.add(new HomingResourceOrb(m.x, m.y, r, 1, resCols[r]));
                    }
                }
            }

            floaters.add(new FloatingAlert(String.format("+[%d %d %d] TO VAULT!", m.alloc[0], m.alloc[1], m.alloc[2]),
                    m.x, m.y - 30, RetroTheme.ACCENT_YELLOW));

            feedbackBanner = String.format("✔ %s DISINTEGRATED! Absorbed +[%d %d %d] Alloc -> Vault grew to [%d %d %d]!",
                    m.pid, m.alloc[0], m.alloc[1], m.alloc[2], currentVault[0], currentVault[1], currentVault[2]);
            bannerColor = RetroTheme.ACCENT_GREEN;
            updateBottomBannerUI();

        } else {
            // DEADLOCK VIOLATION! (Shot an Insolvent Red Meteor)
            combo = 1;
            lives--;
            screenShake = 14;
            triggerExplosion(m.x, m.y, RetroTheme.ACCENT_PINK, 25);
            floaters.add(new FloatingAlert("DEADLOCK VIOLATION! -1 SHIELD", m.x, m.y, RetroTheme.ACCENT_PINK));

            feedbackBanner = String.format("❌ DEADLOCK VIOLATION! %s Need [%d %d %d] > Vault [%d %d %d]! (-1 Shield)",
                    m.pid, m.need[0], m.need[1], m.need[2], currentVault[0], currentVault[1], currentVault[2]);
            bannerColor = RetroTheme.ACCENT_PINK;
            updateBottomBannerUI();

            if (lives <= 0) {
                isGameOver = true;
                feedbackBanner = "💀 SYSTEM CRASH: DEADLOCK LIMIT REACHED! PRESS [R] TO REBOOT";
            }
        }
    }

    private void triggerExplosion(float x, float y, Color c, int count) {
        for (int i = 0; i < count; i++) {
            float ang = (float) (Math.random() * Math.PI * 2);
            float spd = (float) (Math.random() * 6 + 1);
            particles.add(new SpaceParticle(x, y,
                    (float) Math.cos(ang) * spd, (float) Math.sin(ang) * spd,
                    c, (int) (Math.random() * 25 + 15)));
        }
    }


    // =========================================================================
    // 7. MAIN ARCADE CANVAS (ASTEROID & VAULT VISUALS)
    // =========================================================================
    private class ArcadeSpaceCanvas extends JPanel {
        public ArcadeSpaceCanvas() {
            setBackground(new Color(6, 10, 16));
            setOpaque(true);
            setFocusable(true);
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
                int ox = (int) ((Math.random() * 2 - 1) * screenShake);
                int oy = (int) ((Math.random() * 2 - 1) * screenShake);
                g2.translate(ox, oy);
            }

            // 1. Starfield
            drawStarfield(g2);

            // 2. Central Vault HUD (Top)
            drawVaultHUD(g2, w);

            // 3. Defense Perimeter Line
            drawDefensePerimeter(g2, w);

            // 4. Incoming Process Meteors
            drawMeteors(g2);

            // 5. Torpedoes & Homing Orbs
            drawProjectiles(g2);

            // 6. Player Ship
            drawPlayerShip(g2);

            // 7. Particles & Floaters
            drawFX(g2);

            // 8. Overlays
            if (isVictory) {
                drawOverlay(g2, w, h, "100% SAFE SEQUENCE SECURED!",
                        "ALL PROCESS METEORS CLEARED WITHOUT DEADLOCK (+500 XP)", RetroTheme.ACCENT_GREEN);
            } else if (isGameOver) {
                drawOverlay(g2, w, h, "SYSTEM DEADLOCK!",
                        "DEFENSE BREACHED: ALL SHIELDS DEPLETED. PRESS [R] TO REBOOT", RetroTheme.ACCENT_PINK);
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

        private void drawVaultHUD(Graphics2D g2, int w) {
            // Container Panel
            g2.setColor(new Color(16, 22, 34, 240));
            g2.fillRoundRect(14, 6, w - 28, 116, 8, 8);
            g2.setColor(new Color(40, 65, 95));
            g2.drawRoundRect(14, 6, w - 28, 116, 8, 8);

            // Title, Shields, Score
            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("THE CENTRAL CYBER VAULT  //  AVAILABLE ASSETS", 26, 22);

            g2.setColor(Color.WHITE);
            g2.drawString("SHIELDS: ", w - 340, 22);
            for (int i = 0; i < 3; i++) {
                g2.setColor(i < lives ? RetroTheme.ACCENT_CYAN : new Color(50, 40, 60));
                g2.drawString("▲", w - 275 + i * 16, 22);
            }

            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.drawString(String.format("SCORE: %05d (x%d)", score, combo), w - 190, 22);

            // 3 Resource Tanks
            int siloW = (w - 74) / 3;
            int siloY = 30;
            int siloH = 50;

            String[] resNames = {"CORE A (GOLD)", "CORE B (DATA)", "CORE C (PLASMA)"};
            Color[] resColors = {RetroTheme.ACCENT_YELLOW, RetroTheme.ACCENT_CYAN, RetroTheme.ACCENT_PINK};

            for (int r = 0; r < 3; r++) {
                int sx = 24 + r * (siloW + 12);
                g2.setColor(new Color(24, 32, 48));
                g2.fillRoundRect(sx, siloY, siloW, siloH, 6, 6);
                g2.setColor(resColors[r]);
                g2.drawRoundRect(sx, siloY, siloW, siloH, 6, 6);

                g2.setFont(new Font("Consolas", Font.BOLD, 20));
                g2.setColor(Color.WHITE);
                g2.drawString(String.valueOf(currentVault[r]), sx + 14, siloY + 34);

                int barMaxW = siloW - 85;
                float fillFrac = Math.min(1.0f, currentVault[r] / 12.0f);
                int barFillW = (int) (barMaxW * fillFrac);

                g2.setColor(new Color(35, 45, 65));
                g2.fillRoundRect(sx + 65, siloY + 14, barMaxW, 10, 3, 3);
                g2.setColor(resColors[r]);
                g2.fillRoundRect(sx + 65, siloY + 14, barFillW, 10, 3, 3);

                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(new Color(170, 200, 230));
                g2.drawString(resNames[r], sx + 65, siloY + 38);
            }

            // Safe Sequence Tape
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.TEXT_DIM);
            g2.drawString("DISCOVERED SAFE SEQUENCE:", 26, 108);

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_GREEN);
            String seqStr = discoveredSequence.isEmpty() ? "< AWAITING FIRST SOLVENT CLEARANCE >" :
                    "< " + String.join(" -> ", discoveredSequence) + " >";
            g2.drawString(seqStr, 195, 108);

            g2.setColor(RetroTheme.TEXT_DIM);
            g2.drawString(String.format("SMART BOMBS [Q]: %d CHARGES", smartBombs), w - 240, 108);
        }

        private void drawDefensePerimeter(Graphics2D g2, int w) {
            g2.setColor(new Color(40, 60, 90, 80));
            g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{6, 6}, 0.0f));
            g2.drawLine(20, (int) PLAYER_Y - 25, w - 20, (int) PLAYER_Y - 25);
            g2.setStroke(new BasicStroke(1.0f));
        }

        private void drawMeteors(Graphics2D g2) {
            for (ProcessMeteor m : meteors) {
                if (m.cleared) continue;

                int cx = (int) m.x;
                int cy = (int) m.y;
                int r = (int) m.radius;
                boolean safe = m.canFund(currentVault);

                // Aura Ring: GREEN if Solvent, RED if Insolvent
                Color auraColor = safe ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_PINK;
                g2.setColor(new Color(auraColor.getRed(), auraColor.getGreen(), auraColor.getBlue(), 45));
                g2.fillOval(cx - r - 6, cy - r - 6, (r + 6) * 2, (r + 6) * 2);

                g2.setColor(auraColor);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawOval(cx - r - 6, cy - r - 6, (r + 6) * 2, (r + 6) * 2);

                // Asteroid Body (Rocky polygon)
                g2.setColor(new Color(24, 32, 48));
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                g2.setColor(m.meteorColor);
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);

                // Process Avatar
                BufferedImage avatar = PixelSprites.getJobAvatar(m.avatarIndex, 22);
                if (avatar != null) {
                    g2.drawImage(avatar, cx - 11, cy - 22, null);
                }

                // PID
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(Color.WHITE);
                g2.drawString(m.pid, cx - 7, cy + 8);

                // Need Tag [A B C]
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(auraColor);
                String needStr = String.format("N:[%d %d %d]", m.need[0], m.need[1], m.need[2]);
                g2.drawString(needStr, cx - g2.getFontMetrics().stringWidth(needStr) / 2, cy + 22);

                // Status Badge above meteor
                g2.setFont(new Font("Consolas", Font.BOLD, 8));
                if (safe) {
                    g2.setColor(RetroTheme.ACCENT_GREEN);
                    String sTxt = "✔ SOLVENT";
                    g2.drawString(sTxt, cx - g2.getFontMetrics().stringWidth(sTxt) / 2, cy - r - 10);
                } else {
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    String sTxt = "✖ INSOLVENT";
                    g2.drawString(sTxt, cx - g2.getFontMetrics().stringWidth(sTxt) / 2, cy - r - 10);
                }
            }
        }

        private void drawProjectiles(Graphics2D g2) {
            // Torpedoes
            g2.setColor(RetroTheme.ACCENT_CYAN);
            for (PlayerTorpedo t : torpedoes) {
                g2.fillRect((int) t.x - 2, (int) t.y - 8, 4, 12);
                g2.setColor(Color.WHITE);
                g2.fillRect((int) t.x - 1, (int) t.y - 6, 2, 8);
            }

            // Homing Resource Orbs
            for (HomingResourceOrb orb : homingOrbs) {
                g2.setColor(new Color(orb.color.getRed(), orb.color.getGreen(), orb.color.getBlue(), 80));
                g2.fillOval((int) orb.x - 8, (int) orb.y - 8, 16, 16);
                g2.setColor(orb.color);
                g2.fillOval((int) orb.x - 5, (int) orb.y - 5, 10, 10);
                g2.setColor(Color.WHITE);
                g2.fillOval((int) orb.x - 2, (int) orb.y - 2, 4, 4);
            }
        }

        private void drawPlayerShip(Graphics2D g2) {
            int sx = (int) playerX;
            int sy = (int) PLAYER_Y;

            // Retro Starfighter Hull
            int[] xPoints = {sx + 15, sx + 28, sx + 20, sx + 15, sx + 10, sx + 2};
            int[] yPoints = {sy, sy + 24, sy + 20, sy + 26, sy + 20, sy + 24};
            g2.setColor(new Color(18, 28, 45));
            g2.fillPolygon(xPoints, yPoints, 6);
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawPolygon(xPoints, yPoints, 6);

            // Cockpit glass
            g2.setColor(Color.WHITE);
            g2.fillOval(sx + 13, sy + 8, 4, 8);

            // Engine Thrusters
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.fillRect(sx + 7, sy + 22, 4, 6);
            g2.fillRect(sx + 19, sy + 22, 4, 6);
        }

        private void drawFX(Graphics2D g2) {
            for (SpaceParticle p : particles) {
                float a = (float) p.life / p.maxLife;
                g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int) (a * 240)));
                g2.fillRect((int) p.x, (int) p.y, 3, 3);
            }

            for (FloatingAlert f : floaters) {
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(f.color);
                g2.drawString(f.text, f.x - 20, f.y);
            }
        }

        private void drawOverlay(Graphics2D g2, int w, int h, String title, String sub, Color c) {
            g2.setColor(new Color(6, 10, 16, 220));
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(16, 24, 38));
            g2.fillRoundRect(w / 2 - 270, h / 2 - 75, 540, 150, 12, 12);
            g2.setColor(c);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(w / 2 - 270, h / 2 - 75, 540, 150, 12, 12);
            g2.setStroke(new BasicStroke(1.0f));

            g2.setFont(new Font("Consolas", Font.BOLD, 20));
            g2.setColor(c);
            g2.drawString(title, w / 2 - g2.getFontMetrics().stringWidth(title) / 2, h / 2 - 25);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            g2.drawString(sub, w / 2 - g2.getFontMetrics().stringWidth(sub) / 2, h / 2 + 8);

            g2.setColor(RetroTheme.ACCENT_YELLOW);
            String restart = "PRESS [R] TO REBOOT  |  [ESC] TO EXIT";
            g2.drawString(restart, w / 2 - g2.getFontMetrics().stringWidth(restart) / 2, h / 2 + 42);
        }
    }
}
