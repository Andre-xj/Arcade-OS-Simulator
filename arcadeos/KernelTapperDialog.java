package arcadeos;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * KERNEL TAPPER // CPU DISPATCH RUNWAY
 * -------------------------------------------------------------
 * Retro 16-Bit Arcade Scheduling Game inspired by 1983 Bally Midway Tapper.
 * Features 4 distinct scheduling algorithm modes:
 * - FCFS: First-Come, First-Served (Strict Arrival Queue order)
 * - SJF: Shortest Job First (Greedy Non-Preemptive based on Burst Time)
 * - SRT: Shortest Remaining Time (Preemptive SJF)
 * - ROUND ROBIN: Time Quantum Slices with Circular Queue pushback
 */
public class KernelTapperDialog extends JDialog {

    public enum AlgoMode {
        FCFS("FCFS", "FIRST-COME, FIRST-SERVED",
                "Serve aliens in strict arrival order (AT). Beware the Convoy Effect!"),
        SJF("SJF", "SHORTEST JOB FIRST", "Non-preemptive: Serve the alien with the smallest Burst Time (BT) first!"),
        SRT("SRT", "SHORTEST REMAINING TIME",
                "Preemptive: If a new alien with shorter remaining BT arrives, switch immediately!"),
        ROUND_ROBIN("ROUND ROBIN", "ROUND ROBIN (Q=1.5s)",
                "Time-shared: Each slice gives 1.5s CPU. Unfinished aliens push back to queue!");

        public final String code;
        public final String title;
        public final String rule;

        AlgoMode(String c, String t, String r) {
            this.code = c;
            this.title = t;
            this.rule = r;
        }
    }

    // Lane definitions (4 runways)
    private static final int NUM_LANES = 4;
    private static final int LANE_Y_START = 150;
    private static final int LANE_HEIGHT = 90;
    private static final int COUNTER_X = 140;
    private static final int RUNWAY_END_X = 890;

    private AlgoMode currentMode;
    private List<CPUSchedulingPanel.SimJob> sourceJobs;
    private ArcadeCanvas canvas;
    private javax.swing.Timer gameTimer;

    // Game Simulation State
    private int playerLane = 0; // 0 to 3
    private int lives = 3;
    private int score = 0;
    private double simTime = 0.0;
    private boolean isGameOver = false;
    private boolean isVictory = false;
    private String feedbackBanner = "MISSION READY: DISPATCH RUNWAY INITIALIZED";
    private Color bannerColor = RetroTheme.ACCENT_CYAN;
    private int bannerTimer = 0;

    // Entities
    private List<AlienRunner> aliens = new ArrayList<>();
    private List<EnergyBolt> bolts = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();
    private BufferedImage playerSprite;

    public KernelTapperDialog(JFrame parent, String initialAlgo, List<CPUSchedulingPanel.SimJob> customJobs) {
        super(parent, "CAPCOM // KERNEL TAPPER: CPU DISPATCH RUNWAY", true);
        setSize(960, 680);
        setResizable(false);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout());

        this.sourceJobs = (customJobs != null && !customJobs.isEmpty()) ? customJobs : getDefaultDemoJobs();
        this.currentMode = parseAlgo(initialAlgo);
        this.playerSprite = PixelSprites.createPlayerSprite(36);

        // 1. Top HUD Header
        add(createTopControlHeader(), BorderLayout.NORTH);

        // 2. Main Arcade Canvas
        canvas = new ArcadeCanvas();
        add(canvas, BorderLayout.CENTER);

        // 3. Bottom Controls Bar
        add(createBottomControlsBar(), BorderLayout.SOUTH);

        // Keyboard bindings
        setupKeyboardControls();

        // 60 FPS Game Loop
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
                if (gameTimer != null)
                    gameTimer.stop();
            }
        });
    }

    private AlgoMode parseAlgo(String name) {
        if (name == null)
            return AlgoMode.FCFS;
        switch (name.toUpperCase().trim()) {
            case "SJF":
                return AlgoMode.SJF;
            case "SRT":
                return AlgoMode.SRT;
            case "ROUND ROBIN":
            case "RR":
                return AlgoMode.ROUND_ROBIN;
            default:
                return AlgoMode.FCFS;
        }
    }

    private List<CPUSchedulingPanel.SimJob> getDefaultDemoJobs() {
        List<CPUSchedulingPanel.SimJob> list = new ArrayList<>();
        list.add(new CPUSchedulingPanel.SimJob("J1", 0.0, 3.0, 1));
        list.add(new CPUSchedulingPanel.SimJob("J2", 1.5, 5.0, 1));
        list.add(new CPUSchedulingPanel.SimJob("J3", 3.0, 2.0, 1));
        list.add(new CPUSchedulingPanel.SimJob("J4", 5.0, 4.0, 1));
        list.add(new CPUSchedulingPanel.SimJob("J5", 7.0, 1.5, 1));
        list.add(new CPUSchedulingPanel.SimJob("J6", 9.0, 3.5, 1));
        return list;
    }

    private JPanel createTopControlHeader() {
        JPanel bar = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(24, 16, 44));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(65, 45, 95));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        // Left Tag
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel titleLbl = new JLabel("KERNEL TAPPER // CPU RUNWAY");
        titleLbl.setFont(new Font("Consolas", Font.BOLD, 13));
        titleLbl.setForeground(RetroTheme.ACCENT_YELLOW);
        left.add(titleLbl);

        // Algorithm Switch Tabs (Strictly Separate Modes!)
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        tabs.setOpaque(false);
        for (AlgoMode m : AlgoMode.values()) {
            JButton btn = RetroTheme.createRetroButton(m.code, RetroTheme.BTN_DARK, RetroTheme.TEXT_WHITE);
            btn.setFont(new Font("Consolas", Font.BOLD, 10));
            btn.addActionListener(e -> {
                this.currentMode = m;
                resetGameSession();
                canvas.requestFocusInWindow();
            });
            tabs.add(btn);
        }

        // Right HUD stats
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        JLabel livesLbl = new JLabel("CPU CORES: ");
        livesLbl.setFont(new Font("Consolas", Font.BOLD, 11));
        livesLbl.setForeground(RetroTheme.TEXT_DIM);
        right.add(livesLbl);

        bar.add(left, BorderLayout.WEST);
        bar.add(tabs, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createBottomControlsBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(18, 12, 32));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(60, 42, 88));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JLabel help = new JLabel(
                "[W]/[S] or [UP]/[DOWN] Switch Lane  |  [SPACEBAR] Dispatch Quantum Slice  |  [R] Restart  |  [ESC] Exit");
        help.setFont(new Font("Consolas", Font.PLAIN, 10));
        help.setForeground(RetroTheme.TEXT_DIM);
        bar.add(help, BorderLayout.WEST);

        JButton btnClose = RetroTheme.createRetroButton("RETURN TO OS [ESC]", RetroTheme.BTN_DARK,
                RetroTheme.ACCENT_PINK);
        btnClose.setFont(new Font("Consolas", Font.BOLD, 10));
        btnClose.addActionListener(e -> dispose());
        bar.add(btnClose, BorderLayout.EAST);

        return bar;
    }

    private void setupKeyboardControls() {
        InputMap im = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = canvas.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "UP");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "UP");
        am.put("UP", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playerLane > 0)
                    playerLane--;
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "DOWN");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "DOWN");
        am.put("DOWN", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playerLane < NUM_LANES - 1)
                    playerLane++;
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "DISPATCH");
        am.put("DISPATCH", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispatchQuantumEnergy();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "RESTART");
        am.put("RESTART", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetGameSession();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CLOSE");
        am.put("CLOSE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    /**
     * Initializes or restarts the game round based on the active scheduling
     * algorithm.
     */
    private void resetGameSession() {
        aliens.clear();
        bolts.clear();
        particles.clear();
        playerLane = 0;
        lives = 3;
        score = 0;
        simTime = 0.0;
        isGameOver = false;
        isVictory = false;

        // Distribute source jobs across the 4 lanes round-robin or by arrival
        int lIdx = 0;
        for (int i = 0; i < sourceJobs.size(); i++) {
            CPUSchedulingPanel.SimJob sj = sourceJobs.get(i);
            int lane = lIdx % NUM_LANES;
            lIdx++;

            AlienRunner ar = new AlienRunner();
            ar.pid = sj.pid;
            ar.origBt = sj.bt;
            ar.remBt = sj.bt;
            ar.at = sj.at;
            ar.lane = lane;
            // Spawn position staggered beyond runway end
            ar.x = RUNWAY_END_X + (float) (sj.at * 50.0);
            ar.y = LANE_Y_START + lane * LANE_HEIGHT + 18;
            ar.speed = 0.55f;
            ar.avatar = PixelSprites.getJobAvatar(i, 28);
            ar.active = true;
            aliens.add(ar);
        }

        feedbackBanner = "MODE: " + currentMode.title + " // READY!";
        bannerColor = RetroTheme.ACCENT_CYAN;
        bannerTimer = 180;
    }

    /**
     * Dispatches an Energy Quantum Slice down the player's active lane.
     */
    private void dispatchQuantumEnergy() {
        if (isGameOver || isVictory)
            return;

        double quantum = (currentMode == AlgoMode.ROUND_ROBIN) ? 1.5 : 2.0;
        EnergyBolt bolt = new EnergyBolt();
        bolt.lane = playerLane;
        bolt.x = COUNTER_X + 28;
        bolt.y = LANE_Y_START + playerLane * LANE_HEIGHT + 28;
        bolt.speed = 10.5f;
        bolt.quantum = quantum;
        bolts.add(bolt);

        // Small muzzle spark particles
        for (int i = 0; i < 5; i++) {
            particles.add(new Particle(bolt.x, bolt.y, (float) (Math.random() * 2 - 1), (float) (Math.random() * 2 - 1),
                    RetroTheme.ACCENT_CYAN));
        }
    }

    /**
     * 60 FPS Game Loop: Physics, Collision, Scheduling Rules, Starvation
     */
    private void updateGameLoop() {
        if (isGameOver || isVictory) {
            canvas.repaint();
            return;
        }

        simTime += 0.016; // Real-time sim seconds

        // 1. Update Bolts
        Iterator<EnergyBolt> bIt = bolts.iterator();
        while (bIt.hasNext()) {
            EnergyBolt b = bIt.next();
            b.x += b.speed;
            if (b.x > RUNWAY_END_X + 40) {
                bIt.remove();
            }
        }

        // 2. Update Aliens & Check Bolt Collisions
        boolean allFinished = true;
        for (AlienRunner a : aliens) {
            if (a.completed)
                continue;
            allFinished = false;

            // Move alien forward toward counter if arrived
            if (simTime >= a.at) {
                a.x -= a.speed;
            }

            // Check Starvation: alien reached the CPU counter before completion!
            if (a.x <= COUNTER_X + 20) {
                lives--;
                triggerExplosion(a.x, a.y, RetroTheme.ACCENT_PINK, 20);
                feedbackBanner = "★ STARVATION HAZARD! " + a.pid + " REACHED CPU! -1 CORE";
                bannerColor = RetroTheme.ACCENT_PINK;
                bannerTimer = 120;
                a.x = RUNWAY_END_X - 100; // Knock back on starvation strike

                if (lives <= 0) {
                    isGameOver = true;
                    feedbackBanner = "GAME OVER: CPU CORES DEPLETED BY STARVATION!";
                    bannerColor = RetroTheme.ACCENT_PINK;
                }
            }

            // Collision with Energy Bolts on the SAME lane
            for (Iterator<EnergyBolt> boltIt = bolts.iterator(); boltIt.hasNext();) {
                EnergyBolt b = boltIt.next();
                if (b.lane == a.lane && Math.abs(b.x - a.x) < 22 && a.x <= RUNWAY_END_X) {
                    boltIt.remove(); // Consume bolt

                    // Evaluate Scheduling Correctness according to Active Algorithm
                    boolean isCorrectAlgoChoice = checkSchedulingChoice(a);
                    if (!isCorrectAlgoChoice) {
                        feedbackBanner = "ALGO MISMATCH! CHECK " + currentMode.code + " RULES!";
                        bannerColor = RetroTheme.ACCENT_YELLOW;
                        bannerTimer = 90;
                        score = Math.max(0, score - 20);
                    } else {
                        score += 50;
                    }

                    // Apply Quantum energy reduction
                    double cut = Math.min(a.remBt, b.quantum);
                    a.remBt -= cut;
                    triggerExplosion(a.x, a.y, RetroTheme.ACCENT_YELLOW, 8);

                    if (a.remBt <= 0.001) {
                        // Completed!
                        a.completed = true;
                        score += 150;
                        AppState.addScore(15);
                        triggerExplosion(a.x, a.y, RetroTheme.ACCENT_GREEN, 25);
                        feedbackBanner = "✔ PROCESS " + a.pid + " TERMINATED! +150 PTS";
                        bannerColor = RetroTheme.ACCENT_GREEN;
                        bannerTimer = 90;
                    } else {
                        // Preemption / Round Robin push back!
                        if (currentMode == AlgoMode.ROUND_ROBIN) {
                            a.x = Math.min(RUNWAY_END_X - 60, a.x + 130); // Pushed back to circular queue!
                            feedbackBanner = "RR QUANTUM EXPIRED: " + a.pid + " PUSHED BACK IN QUEUE!";
                            bannerColor = RetroTheme.ACCENT_CYAN;
                            bannerTimer = 80;
                        } else if (currentMode == AlgoMode.SRT) {
                            a.x = Math.min(RUNWAY_END_X - 60, a.x + 60);
                        }
                    }
                    break;
                }
            }
        }

        // 3. Victory Condition
        if (allFinished && !aliens.isEmpty()) {
            isVictory = true;
            feedbackBanner = "★ VICTORY! ALL JOBS DISPATCHED UNDER " + currentMode.code + "! ★";
            bannerColor = RetroTheme.ACCENT_GREEN;
            AppState.addScore(150);
        }

        // 4. Update Particles
        Iterator<Particle> pIt = particles.iterator();
        while (pIt.hasNext()) {
            Particle p = pIt.next();
            p.x += p.vx;
            p.y += p.vy;
            p.life--;
            if (p.life <= 0)
                pIt.remove();
        }

        if (bannerTimer > 0)
            bannerTimer--;

        canvas.repaint();
    }

    /**
     * Checks if the alien being hit is the mathematically optimal choice under the
     * active algorithm.
     */
    private boolean checkSchedulingChoice(AlienRunner target) {
        List<AlienRunner> readyQueue = new ArrayList<>();
        for (AlienRunner a : aliens) {
            if (!a.completed && a.x <= RUNWAY_END_X && simTime >= a.at) {
                readyQueue.add(a);
            }
        }
        if (readyQueue.isEmpty())
            return true;

        if (currentMode == AlgoMode.FCFS) {
            // Must be the oldest arrival time
            readyQueue.sort(Comparator.comparingDouble(a -> a.at));
            return readyQueue.get(0) == target;
        } else if (currentMode == AlgoMode.SJF) {
            // Must have the smallest original burst time
            readyQueue.sort(Comparator.comparingDouble(a -> a.origBt));
            return readyQueue.get(0) == target;
        } else if (currentMode == AlgoMode.SRT) {
            // Must have the smallest remaining burst time
            readyQueue.sort(Comparator.comparingDouble(a -> a.remBt));
            return readyQueue.get(0) == target;
        }
        // Round Robin: Any active job can receive quantum time
        return true;
    }

    private void triggerExplosion(float x, float y, Color c, int count) {
        for (int i = 0; i < count; i++) {
            float vx = (float) (Math.sin(i * 0.4) * (1.5 + Math.random() * 2.5));
            float vy = (float) (Math.cos(i * 0.4) * (1.5 + Math.random() * 2.5));
            particles.add(new Particle(x, y, vx, vy, c));
        }
    }

    // =========================================================================
    // ARCADE CANVAS RENDERER
    // =========================================================================
    private class ArcadeCanvas extends JPanel {
        public ArcadeCanvas() {
            setBackground(new Color(10, 8, 20));
            setFocusable(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Retro CRT Grid & Background Glow
            g2.setColor(new Color(15, 12, 28));
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(25, 20, 45));
            for (int gx = 0; gx < w; gx += 40)
                g2.drawLine(gx, 0, gx, h);
            for (int gy = 0; gy < h; gy += 40)
                g2.drawLine(0, gy, w, gy);

            // 2. Algorithm Rule Banner
            g2.setColor(new Color(22, 16, 42));
            g2.fillRoundRect(20, 10, w - 40, 46, 8, 8);
            g2.setColor(new Color(60, 45, 90));
            g2.drawRoundRect(20, 10, w - 40, 46, 8, 8);

            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("CURRENT POLICY: " + currentMode.title, 34, 30);

            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            g2.setColor(RetroTheme.TEXT_WHITE);
            g2.drawString("RULE: " + currentMode.rule, 34, 46);

            // Score & Clock on top right
            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString(String.format("SCORE: %05d", score), w - 230, 30);
            g2.setColor(Color.WHITE);
            g2.drawString(String.format("SIM TIME: %.1fs", simTime), w - 230, 46);

            // CPU Core Hearts
            for (int i = 0; i < 3; i++) {
                int px = w - 100 + i * 24;
                int py = 22;
                if (i < lives) {
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    g2.fillRoundRect(px, py, 18, 14, 4, 4);
                } else {
                    g2.setColor(new Color(50, 35, 65));
                    g2.drawRoundRect(px, py, 18, 14, 4, 4);
                }
            }

            // 3. Render 4 Runway Counters (Lanes)
            for (int i = 0; i < NUM_LANES; i++) {
                int ly = LANE_Y_START + i * LANE_HEIGHT;

                // Runway Counter Surface
                g2.setColor(new Color(28, 20, 52));
                g2.fillRect(COUNTER_X, ly + 40, RUNWAY_END_X - COUNTER_X, 16);
                g2.setColor(new Color(75, 52, 115));
                g2.drawRect(COUNTER_X, ly + 40, RUNWAY_END_X - COUNTER_X, 16);

                // Neon track divider line
                g2.setColor(new Color(45, 32, 75));
                g2.drawLine(COUNTER_X, ly + 48, RUNWAY_END_X, ly + 48);

                // Lane Label
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(new Color(110, 85, 160));
                g2.drawString("QUEUE #" + (i + 1), COUNTER_X + 10, ly + 34);

                // CPU Dispatch Pad on the left
                g2.setColor(new Color(40, 26, 70));
                g2.fillRoundRect(COUNTER_X - 50, ly + 14, 44, 44, 8, 8);
                g2.setColor(new Color(85, 58, 130));
                g2.drawRoundRect(COUNTER_X - 50, ly + 14, 44, 44, 8, 8);

                // Starvation Hazard Zone line
                g2.setColor(new Color(255, 65, 105, 90));
                g2.drawLine(COUNTER_X + 24, ly + 10, COUNTER_X + 24, ly + 65);
            }

            // 4. Render Player (CPU Dispatcher) at active lane
            int py = LANE_Y_START + playerLane * LANE_HEIGHT + 14;
            int px = COUNTER_X - 46;
            if (playerSprite != null) {
                g2.drawImage(playerSprite, px, py, null);
            }
            // Active Core Glow
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawRoundRect(px - 2, py - 2, 40, 40, 6, 6);

            // 5. Render Energy Bolts
            for (EnergyBolt b : bolts) {
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.fillRoundRect((int) b.x, (int) b.y, 20, 8, 4, 4);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect((int) b.x + 4, (int) b.y + 2, 12, 4, 2, 2);
            }

            // 6. Render Aliens (Processes)
            for (AlienRunner a : aliens) {
                if (a.completed)
                    continue;

                int ax = (int) a.x;
                int ay = (int) a.y;

                if (a.avatar != null) {
                    g2.drawImage(a.avatar, ax, ay, null);
                }

                // Process Badge: [PID | BT: X.X]
                g2.setColor(new Color(20, 15, 36, 210));
                g2.fillRoundRect(ax - 12, ay - 20, 56, 16, 4, 4);
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawRoundRect(ax - 12, ay - 20, 56, 16, 4, 4);

                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(Color.WHITE);
                g2.drawString(a.pid, ax - 8, ay - 8);

                // Burst Time meter
                g2.setColor((currentMode == AlgoMode.SRT) ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_CYAN);
                g2.drawString(String.format("%.1fs", a.remBt), ax + 12, ay - 8);
            }

            // 7. Render Particles
            for (Particle pt : particles) {
                g2.setColor(pt.color);
                g2.fillRect((int) pt.x, (int) pt.y, 3, 3);
            }

            // 8. Flight Feedback Banner (Bottom area)
            g2.setColor(new Color(20, 15, 35, 230));
            g2.fillRoundRect(w / 2 - 250, h - 85, 500, 30, 8, 8);
            g2.setColor(bannerColor);
            g2.drawRoundRect(w / 2 - 250, h - 85, 500, 30, 8, 8);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int btx = w / 2 - fm.stringWidth(feedbackBanner) / 2;
            g2.drawString(feedbackBanner, btx, h - 66);

            // 9. Overlay Screens (Victory / Game Over)
            if (isVictory) {
                renderOverlayCard(g2, w, h, "★ FLIGHT CLEARED: S-RANK DISPATCH ★",
                        "All alien processes cleared under " + currentMode.code + " rules!",
                        "FINAL SCORE: " + score + " PTS  |  BONUS: +150 XP AWARDED",
                        RetroTheme.ACCENT_GREEN);
            } else if (isGameOver) {
                renderOverlayCard(g2, w, h, "☠ SYSTEM CRASH: CPU STARVATION ☠",
                        "Processes collided with the counter before getting CPU time.",
                        "PRESS [R] TO RETRY MISSION OR [ESC] TO EXIT",
                        RetroTheme.ACCENT_PINK);
            }

            g2.dispose();
        }

        private void renderOverlayCard(Graphics2D g2, int w, int h, String title, String sub, String act, Color col) {
            g2.setColor(new Color(10, 8, 20, 220));
            g2.fillRect(0, 0, w, h);

            int cw = 560;
            int ch = 180;
            int cx = (w - cw) / 2;
            int cy = (h - ch) / 2;

            g2.setColor(new Color(25, 18, 45));
            g2.fillRoundRect(cx, cy, cw, ch, 12, 12);
            g2.setColor(col);
            g2.drawRoundRect(cx, cy, cw, ch, 12, 12);

            g2.setFont(new Font("Consolas", Font.BOLD, 16));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, cx + (cw - fm.stringWidth(title)) / 2, cy + 45);

            g2.setFont(new Font("Consolas", Font.PLAIN, 12));
            g2.setColor(Color.WHITE);
            fm = g2.getFontMetrics();
            g2.drawString(sub, cx + (cw - fm.stringWidth(sub)) / 2, cy + 85);

            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            fm = g2.getFontMetrics();
            g2.drawString(act, cx + (cw - fm.stringWidth(act)) / 2, cy + 125);

            g2.setFont(new Font("Consolas", Font.ITALIC, 10));
            g2.setColor(RetroTheme.TEXT_DIM);
            String hint = "[R] Play Again  |  [1]-[4] Try Another Algorithm  |  [ESC] Close";
            fm = g2.getFontMetrics();
            g2.drawString(hint, cx + (cw - fm.stringWidth(hint)) / 2, cy + 155);
        }
    }

    // =========================================================================
    // HELPER ENTITIES
    // =========================================================================
    private static class AlienRunner {
        String pid;
        double origBt;
        double remBt;
        double at;
        int lane;
        float x, y;
        float speed;
        BufferedImage avatar;
        boolean active = true;
        boolean completed = false;
    }

    private static class EnergyBolt {
        int lane;
        float x, y;
        float speed;
        double quantum;
    }

    private static class Particle {
        float x, y, vx, vy;
        int life, maxLife;
        Color color;

        Particle(float x, float y, float vx, float vy, Color c) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = c;
            this.maxLife = 20 + (int) (Math.random() * 15);
            this.life = maxLife;
        }
    }
}
