package arcadeos;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * KERNEL TAPPER // 4-LANE CPU DISPATCH RUNWAY
 * -------------------------------------------------------------
 * Premium 16-Bit Retro Arcade Scheduling Cabinet.
 * Features 4 distinct scheduling algorithm modes:
 * - FCFS: First-Come, First-Served (Strict Arrival Queue order)
 * - SJF: Shortest Job First (Greedy Non-Preemptive based on Burst Time)
 * - SRT: Shortest Remaining Time (Preemptive SJF)
 * - ROUND ROBIN: Time Quantum Slices with Circular Queue pushback
 */
public class KernelTapperDialog extends JDialog {

    public enum AlgoMode {
        FCFS("FCFS", "FIRST-COME, FIRST-SERVED",
                "Serve aliens in strict arrival order (AT). Target the frontmost/oldest creep!"),
        SJF("SJF", "SHORTEST JOB FIRST",
                "Non-preemptive: Target the alien with the smallest Burst Time (HP) first!"),
        SRT("SRT", "SHORTEST REMAINING TIME",
                "Preemptive: If an alien with shorter remaining HP arrives, switch targets immediately!"),
        ROUND_ROBIN("ROUND ROBIN", "ROUND ROBIN (Q=1.5s)",
                "Time-shared: Each slice gives 1.5s CPU. Unfinished aliens push back into queue!");

        public final String code;
        public final String title;
        public final String rule;

        AlgoMode(String c, String t, String r) {
            this.code = c;
            this.title = t;
            this.rule = r;
        }
    }

    // Lane definitions (3 spacious glowing maglev runways)
    private static final int NUM_LANES = 3;
    private static final int LANE_Y_START = 85;
    private static final int LANE_HEIGHT = 124;
    private static final int COUNTER_X = 145;
    private static final int RUNWAY_END_X = 910;

    private AlgoMode currentMode;
    private List<CPUSchedulingPanel.SimJob> sourceJobs;
    private ArcadeCanvas canvas;
    private javax.swing.Timer gameTimer;
    private JButton[] algoTabs = new JButton[4];

    // Game Simulation State
    private int playerLane = 0; // 0 to 3
    private int lives = 3;
    private int score = 0;
    private int faultCount = 0; // 3 Policy Faults = -1 Core Damage!
    private double simTime = 0.0;
    private int animTick = 0;
    private boolean isGameOver = false;
    private boolean isVictory = false;
    private String feedbackBanner = "MISSION READY: 3-LANE CPU RUNWAY INITIALIZED // ARMED";
    private Color bannerColor = RetroTheme.ACCENT_CYAN;
    private int bannerTimer = 0;

    // Entities
    private List<AlienRunner> aliens = new ArrayList<>();
    private List<EnergyBolt> bolts = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();
    private List<ClickPing> clickPings = new ArrayList<>();
    private BufferedImage playerSprite;

    public KernelTapperDialog(JFrame parent, String initialAlgo, List<CPUSchedulingPanel.SimJob> customJobs) {
        super(parent, "CAPCOM // KERNEL TAPPER: 3-LANE CPU DISPATCH RUNWAY", true);
        setSize(1020, 690);
        setResizable(false);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout());

        this.sourceJobs = (customJobs != null && !customJobs.isEmpty()) ? customJobs : getDefaultDemoJobs();
        this.currentMode = parseAlgo(initialAlgo);
        this.playerSprite = PixelSprites.createPlayerSprite(36);

        // 1. Top Arcade Marquee Header
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
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Dark glass header with subtle gradient
                GradientPaint gp = new GradientPaint(0, 0, new Color(26, 18, 48), 0, getHeight(), new Color(16, 12, 32));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(75, 50, 110));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

        // Left Arcade Title Badge
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel icon = new JLabel(RetroIcons.createPlayIcon(14, RetroTheme.ACCENT_PINK));
        JLabel titleLbl = new JLabel("KERNEL TAPPER // 16-BIT ARCADE");
        titleLbl.setFont(new Font("Consolas", Font.BOLD, 13));
        titleLbl.setForeground(RetroTheme.ACCENT_YELLOW);
        left.add(icon);
        left.add(titleLbl);

        // Algorithm Switch Tabs (Symmetrical Tactile Buttons with Active Glow)
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        tabs.setOpaque(false);
        AlgoMode[] modes = AlgoMode.values();
        for (int i = 0; i < modes.length; i++) {
            AlgoMode m = modes[i];
            JButton btn = new JButton(m.code) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean isCur = (currentMode == m);
                    Color bg = isCur ? new Color(75, 30, 110) : new Color(28, 20, 50);
                    Color border = isCur ? RetroTheme.ACCENT_CYAN : new Color(70, 50, 100);
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.setColor(border);
                    g2.setStroke(new BasicStroke(isCur ? 2.0f : 1.0f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(new Font("Consolas", Font.BOLD, 11));
            btn.setForeground(Color.WHITE);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(m == AlgoMode.ROUND_ROBIN ? 110 : 64, 28));
            btn.addActionListener(e -> {
                this.currentMode = m;
                updateAlgoButtons();
                resetGameSession();
                canvas.requestFocusInWindow();
            });
            algoTabs[i] = btn;
            tabs.add(btn);
        }

        // Right HUD Telemetry Pods (Symmetrical Glass Capsules)
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        JLabel liveScore = new JLabel("TARGET: 3 LANES");
        liveScore.setFont(new Font("Consolas", Font.BOLD, 11));
        liveScore.setForeground(RetroTheme.ACCENT_CYAN);
        right.add(liveScore);

        bar.add(left, BorderLayout.WEST);
        bar.add(tabs, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void updateAlgoButtons() {
        for (JButton b : algoTabs) {
            if (b != null) b.repaint();
        }
    }

    private JPanel createBottomControlsBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(15, 10, 26));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(60, 42, 88));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

        // Keycap style help instructions
        JPanel keycaps = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        keycaps.setOpaque(false);

        keycaps.add(createKeyCap("[ W ] / [ S ] or CLICK"));
        JLabel t1 = new JLabel("Switch Lane & Fire  ");
        t1.setFont(new Font("Consolas", Font.BOLD, 11));
        t1.setForeground(Color.WHITE);
        keycaps.add(t1);

        keycaps.add(createKeyCap("[ SPACEBAR ]"));
        JLabel t2 = new JLabel("Dispatch CPU Slice  ");
        t2.setFont(new Font("Consolas", Font.BOLD, 11));
        t2.setForeground(Color.WHITE);
        keycaps.add(t2);

        keycaps.add(createKeyCap("[ R ]"));
        JLabel t3 = new JLabel("Restart  ");
        t3.setFont(new Font("Consolas", Font.BOLD, 11));
        t3.setForeground(Color.WHITE);
        keycaps.add(t3);

        bar.add(keycaps, BorderLayout.WEST);

        JButton btnClose = CPUSchedulingPanel.createArcadeButton("RETURN TO OS [ESC]", new Color(130, 25, 70), Color.WHITE, 28);
        btnClose.setFont(new Font("Consolas", Font.BOLD, 10));
        btnClose.addActionListener(e -> dispose());
        bar.add(btnClose, BorderLayout.EAST);

        return bar;
    }

    private JLabel createKeyCap(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Consolas", Font.BOLD, 11));
        l.setForeground(RetroTheme.ACCENT_CYAN);
        l.setBackground(new Color(32, 22, 54));
        l.setOpaque(true);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(85, 60, 125), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        return l;
    }

    private void setupKeyboardControls() {
        InputMap im = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = canvas.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "UP");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "UP");
        am.put("UP", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playerLane > 0) playerLane--;
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "DOWN");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "DOWN");
        am.put("DOWN", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playerLane < NUM_LANES - 1) playerLane++;
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

    private void resetGameSession() {
        aliens.clear();
        bolts.clear();
        particles.clear();
        clickPings.clear();
        playerLane = 0;
        lives = 3;
        score = 0;
        faultCount = 0;
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
            ar.x = RUNWAY_END_X + (float) (sj.at * 55.0);
            ar.y = LANE_Y_START + lane * LANE_HEIGHT + 32;
            // Balanced Alien Speed: 0.38f (gives player comfortable reaction time to strategize!)
            ar.speed = (float) Math.max(0.32, 0.44 - (sj.bt * 0.02));
            ar.avatar = PixelSprites.getJobAvatar(i, 32);
            ar.active = true;
            aliens.add(ar);
        }

        feedbackBanner = "ACTIVE DIRECTIVE: " + currentMode.title + " // DEFEND CPU CORES!";
        bannerColor = RetroTheme.ACCENT_CYAN;
        bannerTimer = 180;
    }

    private void dispatchQuantumEnergy() {
        if (isGameOver || isVictory) return;

        double quantum = (currentMode == AlgoMode.ROUND_ROBIN) ? 1.5 : 2.0;
        EnergyBolt bolt = new EnergyBolt();
        bolt.lane = playerLane;
        bolt.x = COUNTER_X + 24;
        bolt.y = LANE_Y_START + playerLane * LANE_HEIGHT + 48;
        bolt.speed = 12.0f;
        bolt.quantum = quantum;
        bolts.add(bolt);

        // Muzzle spark burst particles
        for (int i = 0; i < 7; i++) {
            particles.add(new Particle(bolt.x, bolt.y, (float) (Math.random() * 3 - 1), (float) (Math.random() * 3 - 1.5), RetroTheme.ACCENT_CYAN));
        }
    }

    private void updateGameLoop() {
        animTick++;
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
            if (a.completed) continue;
            allFinished = false;

            // Move alien forward toward counter if arrived
            if (simTime >= a.at) {
                a.x -= a.speed;
            }

            // Check Starvation: alien reached the CPU counter before completion!
            if (a.x <= COUNTER_X + 26) {
                lives--;
                triggerExplosion(a.x, a.y + 16, RetroTheme.ACCENT_PINK, 25);
                feedbackBanner = "CRITICAL HAZARD! " + a.pid + " STARVED CPU! -1 CORE";
                bannerColor = RetroTheme.ACCENT_PINK;
                bannerTimer = 120;
                a.x = RUNWAY_END_X - 100; // Knock back on starvation strike

                if (lives <= 0) {
                    isGameOver = true;
                    feedbackBanner = "SYSTEM HALTED: ALL CPU CORES DEPLETED BY STARVATION!";
                    bannerColor = RetroTheme.ACCENT_PINK;
                }
            }

            // Collision with Energy Bolts on the SAME lane
            for (Iterator<EnergyBolt> boltIt = bolts.iterator(); boltIt.hasNext();) {
                EnergyBolt b = boltIt.next();
                if (b.lane == a.lane && Math.abs(b.x - a.x) < 24 && a.x <= RUNWAY_END_X) {
                    boltIt.remove(); // Consume bolt

                    // Evaluate Scheduling Correctness according to Active Algorithm
                    boolean isCorrectAlgoChoice = checkSchedulingChoice(a);

                    if (!isCorrectAlgoChoice) {
                        // POLICY FAULT: Shield deflects the laser! Maling alien ang binaril -> ZERO damage!
                        triggerExplosion(a.x, a.y + 16, RetroTheme.ACCENT_PINK, 15);
                        faultCount++;
                        score = Math.max(0, score - 30);
                        bannerColor = RetroTheme.ACCENT_PINK;
                        bannerTimer = 100;

                        AlienRunner expected = getExpectedTarget();
                        String expectedPid = expected != null ? expected.pid : "earliest";
                        if (faultCount >= 3) {
                            faultCount = 0;
                            lives--;
                            triggerExplosion(COUNTER_X, a.y + 16, RetroTheme.ACCENT_PINK, 25);
                            feedbackBanner = "3 POLICY FAULTS: CPU DAMAGED! Target was " + expectedPid + "! (-1 CORE)";
                            if (lives <= 0) {
                                isGameOver = true;
                                feedbackBanner = "SYSTEM CRASH: CPU CORES DEPLETED BY POLICY FAULTS!";
                            }
                        } else {
                            feedbackBanner = "[POLICY FAULT " + faultCount + "/3] Shot Deflected! " + a.pid + " is NOT next! Target " + expectedPid + " under " + currentMode.code + "!";
                        }
                        break; // Bolt consumed, alien takes zero damage!
                    }

                    // Correct Target Hit!
                    score += 50;

                    // Apply Quantum energy reduction
                    double cut = Math.min(a.remBt, b.quantum);
                    a.remBt -= cut;
                    triggerExplosion(a.x, a.y + 16, RetroTheme.ACCENT_YELLOW, 10);

                    if (a.remBt <= 0.001) {
                        // Completed!
                        a.completed = true;
                        score += 150;
                        AppState.addScore(25);
                        triggerExplosion(a.x, a.y + 16, RetroTheme.ACCENT_GREEN, 30);
                        feedbackBanner = "DIRECT HIT! " + a.pid + " TERMINATED! +150 PTS";
                        bannerColor = RetroTheme.ACCENT_GREEN;
                        bannerTimer = 90;
                    } else {
                        // Preemption / Round Robin push back!
                        if (currentMode == AlgoMode.ROUND_ROBIN) {
                            a.x = Math.min(RUNWAY_END_X - 60, a.x + 130);
                            feedbackBanner = "RR VOLLEY FINISHED: " + a.pid + " ROTATED BACK IN QUEUE!";
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
            feedbackBanner = "VICTORY! ALL JOBS DISPATCHED UNDER " + currentMode.code + "!";
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
            if (p.life <= 0) pIt.remove();
        }

        // 5. Update Click Pings
        Iterator<ClickPing> cpIt = clickPings.iterator();
        while (cpIt.hasNext()) {
            ClickPing cp = cpIt.next();
            cp.life--;
            if (cp.life <= 0) cpIt.remove();
        }

        if (bannerTimer > 0) bannerTimer--;

        canvas.repaint();
    }

    private AlienRunner getExpectedTarget() {
        List<AlienRunner> readyQueue = new ArrayList<>();
        for (AlienRunner a : aliens) {
            if (!a.completed && a.x <= RUNWAY_END_X && simTime >= a.at) {
                readyQueue.add(a);
            }
        }
        if (readyQueue.isEmpty()) return null;

        if (currentMode == AlgoMode.FCFS) {
            readyQueue.sort(Comparator.comparingDouble(a -> a.at));
        } else if (currentMode == AlgoMode.SJF) {
            readyQueue.sort(Comparator.comparingDouble(a -> a.origBt));
        } else if (currentMode == AlgoMode.SRT) {
            readyQueue.sort(Comparator.comparingDouble(a -> a.remBt));
        }
        return readyQueue.get(0);
    }

    private boolean checkSchedulingChoice(AlienRunner target) {
        if (currentMode == AlgoMode.ROUND_ROBIN) return true;
        AlienRunner exp = getExpectedTarget();
        return exp == null || exp == target;
    }

    private void triggerExplosion(float x, float y, Color c, int count) {
        for (int i = 0; i < count; i++) {
            float vx = (float) (Math.sin(i * 0.4) * (1.5 + Math.random() * 2.8));
            float vy = (float) (Math.cos(i * 0.4) * (1.5 + Math.random() * 2.8));
            particles.add(new Particle(x, y, vx, vy, c));
        }
    }

    // =========================================================================
    // ARCADE CANVAS RENDERER (VIBRANT RETRO 16-BIT CABINET)
    // =========================================================================
    private class ArcadeCanvas extends JPanel {
        public ArcadeCanvas() {
            setBackground(new Color(10, 8, 20));
            setFocusable(true);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isGameOver || isVictory) return;
                    int my = e.getY();
                    int lane = (my - LANE_Y_START) / LANE_HEIGHT;
                    if (lane >= 0 && lane < NUM_LANES) {
                        playerLane = lane;
                        dispatchQuantumEnergy();
                        clickPings.add(new ClickPing(e.getX(), e.getY()));
                        repaint();
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int my = e.getY();
                    int lane = (my - LANE_Y_START) / LANE_HEIGHT;
                    if (lane >= 0 && lane < NUM_LANES && e.getX() >= COUNTER_X && e.getX() <= RUNWAY_END_X + 80) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Deep Space CRT Grid Background
            g2.setColor(new Color(14, 10, 26));
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(24, 18, 42));
            for (int gx = 0; gx < w; gx += 32) g2.drawLine(gx, 0, gx, h);
            for (int gy = 0; gy < h; gy += 32) g2.drawLine(0, gy, w, gy);

            // 2. Algorithm Policy Intel Strip (Glassmorphic Top Banner)
            int bannerY = 10;
            int bannerH = 54;
            g2.setColor(new Color(22, 15, 44, 230));
            g2.fillRoundRect(20, bannerY, w - 40, bannerH, 8, 8);
            g2.setColor(new Color(75, 52, 115));
            g2.drawRoundRect(20, bannerY, w - 40, bannerH, 8, 8);

            // Left Neon Accent Bar
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.fillRoundRect(22, bannerY + 4, 4, bannerH - 8, 2, 2);

            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("POLICY DIRECTIVE // " + currentMode.title, 36, bannerY + 20);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            g2.drawString(currentMode.rule, 36, bannerY + 40);

            // Symmetrical HUD Cards on the Right: SCORE, TIME, CPU CORES
            int rightBoxX = w - 310;
            g2.setColor(new Color(15, 10, 28));
            g2.fillRoundRect(rightBoxX, bannerY + 6, 280, 42, 6, 6);
            g2.setColor(new Color(60, 42, 90));
            g2.drawRoundRect(rightBoxX, bannerY + 6, 280, 42, 6, 6);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString(String.format("SCORE: %05d", score), rightBoxX + 12, bannerY + 22);

            g2.setColor(Color.WHITE);
            g2.drawString(String.format("TIME: %.1fs", simTime), rightBoxX + 12, bannerY + 38);

            // 3 High-Tech Battery/Core Cells
            int coreBaseX = rightBoxX + 160;
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.TEXT_DIM);
            g2.drawString("CORES:", coreBaseX, bannerY + 20);

            for (int i = 0; i < 3; i++) {
                int cx = coreBaseX + 44 + i * 22;
                int cy = bannerY + 12;
                boolean isAlive = (i < lives);

                g2.setColor(new Color(25, 18, 42));
                g2.fillRoundRect(cx, cy, 16, 20, 3, 3);
                g2.setColor(isAlive ? RetroTheme.ACCENT_GREEN : new Color(60, 40, 75));
                g2.drawRoundRect(cx, cy, 16, 20, 3, 3);

                if (isAlive) {
                    float pulse = (float)(Math.sin(animTick * 0.2 + i) * 0.3 + 0.7);
                    g2.setColor(new Color(0, (int)(200 * pulse + 55), 100));
                    g2.fillRoundRect(cx + 3, cy + 3, 10, 14, 2, 2);
                } else {
                    g2.setColor(new Color(120, 30, 60));
                    g2.drawLine(cx + 3, cy + 3, cx + 13, cy + 17);
                    g2.drawLine(cx + 13, cy + 3, cx + 3, cy + 17);
                }
            }

            // 3. Render 3 Spacious Futuristic Maglev Runways (Lanes)
            int laneW = RUNWAY_END_X - COUNTER_X;
            int laneH = 92;

            // Vertical Mainframe Rail connecting the stations on the left
            g2.setColor(new Color(32, 22, 58));
            g2.fillRect(COUNTER_X - 54, LANE_Y_START + 6, 8, NUM_LANES * LANE_HEIGHT - 20);
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawRect(COUNTER_X - 54, LANE_Y_START + 6, 8, NUM_LANES * LANE_HEIGHT - 20);

            for (int i = 0; i < NUM_LANES; i++) {
                int ly = LANE_Y_START + i * LANE_HEIGHT;
                boolean isPlayerActiveLane = (i == playerLane);

                // A. Runway Floor (Metallic gradient track)
                GradientPaint trackGp = new GradientPaint(
                        COUNTER_X, ly + 6, new Color(24, 18, 48),
                        COUNTER_X, ly + 6 + laneH, new Color(14, 10, 26)
                );
                g2.setPaint(trackGp);
                g2.fillRect(COUNTER_X, ly + 6, laneW, laneH);

                // Top & Bottom Glowing Rails
                g2.setColor(isPlayerActiveLane ? RetroTheme.ACCENT_CYAN : new Color(65, 45, 95));
                g2.setStroke(new BasicStroke(isPlayerActiveLane ? 2.0f : 1.0f));
                g2.drawLine(COUNTER_X, ly + 6, RUNWAY_END_X, ly + 6);
                g2.setColor(isPlayerActiveLane ? RetroTheme.ACCENT_PINK : new Color(50, 35, 75));
                g2.drawLine(COUNTER_X, ly + 6 + laneH, RUNWAY_END_X, ly + 6 + laneH);

                // Animated Conveyor Chevrons moving left toward player
                g2.setColor(new Color(55, 40, 85, isPlayerActiveLane ? 180 : 100));
                g2.setStroke(new BasicStroke(1.5f));
                int chevronOffset = (animTick * 2) % 40;
                for (int cx = RUNWAY_END_X - chevronOffset; cx > COUNTER_X + 40; cx -= 40) {
                    int cy = ly + 46;
                    g2.drawLine(cx, cy - 8, cx - 8, cy);
                    g2.drawLine(cx - 8, cy, cx, cy + 8);
                }

                // B. Station Terminal Pod on the left (Completely separated from Player)
                int stationX = COUNTER_X - 115;
                int stationY = ly + 12;
                int stationW = 52;
                int stationH = 68;
                g2.setColor(isPlayerActiveLane ? new Color(45, 25, 75) : new Color(24, 16, 42));
                g2.fillRoundRect(stationX, stationY, stationW, stationH, 8, 8);
                g2.setColor(isPlayerActiveLane ? RetroTheme.ACCENT_CYAN : new Color(75, 52, 110));
                g2.setStroke(new BasicStroke(isPlayerActiveLane ? 2.0f : 1.2f));
                g2.drawRoundRect(stationX, stationY, stationW, stationH, 8, 8);

                // Station Label
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(isPlayerActiveLane ? RetroTheme.ACCENT_YELLOW : new Color(170, 150, 210));
                g2.drawString("LANE #" + (i + 1), stationX + 6, stationY + 24);

                // LED status light
                g2.setColor(isPlayerActiveLane ? RetroTheme.ACCENT_GREEN : new Color(80, 60, 110));
                g2.fillOval(stationX + 22, stationY + 44, 8, 8);

                // C. Starvation Tripwire Hazard Warning Zone
                int hazX = COUNTER_X + 28;
                g2.setColor(new Color(255, 40, 80, 140));
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{4, 4}, 0));
                g2.drawLine(hazX, ly + 6, hazX, ly + 6 + laneH);
                g2.setStroke(new BasicStroke(1.0f));

                // D. Memory Spawn Portal Pylon on the right
                g2.setColor(new Color(40, 25, 75));
                g2.fillRoundRect(RUNWAY_END_X - 4, ly + 8, 14, laneH - 4, 4, 4);
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.drawRoundRect(RUNWAY_END_X - 4, ly + 8, 14, laneH - 4, 4, 4);
            }

            // 4. Render Player Dispatcher & Targeting Sights at Active Lane
            int activeLy = LANE_Y_START + playerLane * LANE_HEIGHT;
            int py = activeLy + 26;
            int px = COUNTER_X - 46;

            // Targeting Laser Sight extending from dispatcher down the runway
            g2.setColor(new Color(0, 240, 255, 65));
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{6, 6}, animTick % 12));
            g2.drawLine(COUNTER_X, activeLy + 46, RUNWAY_END_X, activeLy + 46);
            g2.setStroke(new BasicStroke(1.0f));

            // Plasma glow around dispatcher
            float pulse = (float)(Math.sin(animTick * 0.3) * 0.5 + 0.5);
            g2.setColor(new Color(255, 42, 141, (int)(40 + pulse * 70)));
            g2.fillRoundRect(px - 4, py - 4, 44, 44, 10, 10);

            if (playerSprite != null) {
                g2.drawImage(playerSprite, px, py, null);
            }
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(px - 2, py - 2, 40, 40, 8, 8);

            // 5. Render Energy Bolts (Glowing Plasma Projectiles)
            for (EnergyBolt b : bolts) {
                int bx = (int) b.x;
                int by = (int) b.y;
                // Outer Cyan Aura
                g2.setColor(new Color(0, 240, 255, 120));
                g2.fillRoundRect(bx - 4, by - 3, 28, 14, 6, 6);
                // Cyan Core
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.fillRoundRect(bx, by, 20, 8, 4, 4);
                // White Laser Core
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(bx + 4, by + 2, 12, 4, 2, 2);
            }

            // 6. Find mathematically expected target under active policy to show reticle
            AlienRunner currentTarget = getExpectedTarget();

            // 7. Render Aliens (Creeps) on the Runway
            for (AlienRunner a : aliens) {
                if (a.completed || a.x > RUNWAY_END_X) continue;

                int ax = (int) a.x;
                int ay = (int) a.y;
                float aBob = (float)(Math.sin(animTick * 0.2 + a.lane) * 2.5);

                if (a.avatar != null) {
                    g2.drawImage(a.avatar, ax, (int)(ay + aBob), null);
                }

                // If this is the mathematically expected target, draw glowing lock reticle!
                if (a == currentTarget) {
                    float lockPulse = (float)(Math.sin(animTick * 0.4) * 0.5 + 0.5);
                    g2.setColor(new Color(255, (int)(180 + lockPulse * 75), 0));
                    g2.setStroke(new BasicStroke(2.0f));
                    int rx = ax - 6;
                    int ry = (int)(ay + aBob - 6);
                    int rSize = 44;
                    // Corner brackets
                    int cLen = 7;
                    g2.drawLine(rx, ry, rx + cLen, ry);
                    g2.drawLine(rx, ry, rx, ry + cLen);
                    g2.drawLine(rx + rSize, ry, rx + rSize - cLen, ry);
                    g2.drawLine(rx + rSize, ry, rx + rSize, ry + cLen);
                    g2.drawLine(rx, ry + rSize, rx + cLen, ry + rSize);
                    g2.drawLine(rx, ry + rSize, rx, ry + rSize - cLen);
                    g2.drawLine(rx + rSize, ry + rSize, rx + rSize - cLen, ry + rSize);
                    g2.drawLine(rx + rSize, ry + rSize, rx + rSize, ry + rSize - cLen);

                    g2.setFont(new Font("Consolas", Font.BOLD, 9));
                    g2.drawString("LOCK", rx + 10, ry - 3);
                }

                // Creep Health Bar above sprite
                int hpBarW = 44;
                int hpBarH = 4;
                int hpBarX = ax - 6;
                int hpBarY = (int)(ay + aBob - 18);

                g2.setColor(new Color(15, 10, 24));
                g2.fillRoundRect(hpBarX, hpBarY, hpBarW, hpBarH, 2, 2);

                float hpFrac = (float) Math.min(1.0, Math.max(0.1, a.remBt / 5.0));
                Color hpCol = a.remBt > 3.0 ? RetroTheme.ACCENT_PINK : (a.remBt > 1.5 ? RetroTheme.ACCENT_YELLOW : RetroTheme.ACCENT_GREEN);
                g2.setColor(hpCol);
                g2.fillRoundRect(hpBarX, hpBarY, (int)(hpBarW * hpFrac), hpBarH, 2, 2);

                // Process Badge: [PID | HP: X.X]
                g2.setColor(new Color(18, 12, 32, 230));
                g2.fillRoundRect(ax - 8, (int)(ay + aBob + 34), 48, 16, 4, 4);
                g2.setColor(a == currentTarget ? RetroTheme.ACCENT_YELLOW : new Color(75, 52, 110));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(ax - 8, (int)(ay + aBob + 34), 48, 16, 4, 4);

                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(Color.WHITE);
                g2.drawString(a.pid, ax - 4, (int)(ay + aBob + 46));

                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawString(String.format("%.1f", a.remBt), ax + 18, (int)(ay + aBob + 46));
            }

            // 8. Render Particles
            for (Particle pt : particles) {
                g2.setColor(pt.color);
                g2.fillRect((int) pt.x, (int) pt.y, 3, 3);
            }

            // 8b. Render Tactical Click Reticles
            for (ClickPing cp : clickPings) {
                float frac = (10 - cp.life) / 10.0f;
                int r = (int)(frac * 24) + 6;
                int alpha = Math.max(0, Math.min(255, (int)(cp.life * 25)));
                g2.setColor(new Color(0, 240, 255, alpha));
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawOval(cp.x - r, cp.y - r, r * 2, r * 2);
                g2.drawLine(cp.x - r - 5, cp.y, cp.x + r + 5, cp.y);
                g2.drawLine(cp.x, cp.y - r - 5, cp.x, cp.y + r + 5);
            }

            // 9. Flight Feedback Ticker (Bottom Area)
            int tickerW = 620;
            int tickerH = 32;
            int tickerX = (w - tickerW) / 2;
            int tickerY = h - 68;

            g2.setColor(new Color(20, 14, 38, 235));
            g2.fillRoundRect(tickerX, tickerY, tickerW, tickerH, 6, 6);
            g2.setColor(bannerColor);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(tickerX, tickerY, tickerW, tickerH, 6, 6);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int btx = (w - fm.stringWidth(feedbackBanner)) / 2;
            g2.setColor(Color.WHITE);
            g2.drawString(feedbackBanner, btx, tickerY + 20);

            // 10. Overlay Screens (Victory / Game Over)
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
            g2.setColor(new Color(10, 8, 20, 225));
            g2.fillRect(0, 0, w, h);

            int cw = 580;
            int ch = 190;
            int cx = (w - cw) / 2;
            int cy = (h - ch) / 2;

            g2.setColor(new Color(25, 18, 45));
            g2.fillRoundRect(cx, cy, cw, ch, 12, 12);
            g2.setColor(col);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRoundRect(cx, cy, cw, ch, 12, 12);

            g2.setFont(new Font("Consolas", Font.BOLD, 17));
            g2.setColor(col);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, cx + (cw - fm.stringWidth(title)) / 2, cy + 45);

            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(Color.WHITE);
            fm = g2.getFontMetrics();
            g2.drawString(sub, cx + (cw - fm.stringWidth(sub)) / 2, cy + 85);

            g2.setFont(new Font("Consolas", Font.BOLD, 12));
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            fm = g2.getFontMetrics();
            g2.drawString(act, cx + (cw - fm.stringWidth(act)) / 2, cy + 125);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(RetroTheme.TEXT_DIM);
            String hint = "[R] Play Again  |  [ESC] Return to OS";
            fm = g2.getFontMetrics();
            g2.drawString(hint, cx + (cw - fm.stringWidth(hint)) / 2, cy + 160);
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

    private static class ClickPing {
        int x, y;
        int life = 10;
        ClickPing(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
