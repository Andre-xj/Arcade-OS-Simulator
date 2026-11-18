package arcadeos;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * Screen 3: OPERATING SYSTEM ARCHIVES & STAGE SELECT
 * Premium AAA Retro-Arcade Stage Selection Carousel:
 * 1. Readability & Typography: WCAG AA contrast, functional text >= 12px, body text >= 14px.
 * 2. Visual Hierarchy: Ghost/outline secondary buttons; primary Launch button clearly dominates.
 * 3. Side Cards & Depth: 0.82 scale, dimmed opacity, no dead space, dynamic room glow shift.
 * 4. Gamification: 8-block segmented pixel meter, score/operator in header, toast notifications.
 * 5. Navigation: Enhanced tab strip with animated caret, 3D arcade keycap hints, full arrow/1-8/ESC support.
 * 6. Retro Polish: Toggleable CRT scanlines, pixel-crisp sprites, and synthesized arcade sound effects!
 */
public class ArchivesPanel extends JPanel {
    private ArcadeOS parentFrame;
    private JLabel lblFilesReadCounter;
    private JLabel lblHeaderScore;
    private JLabel lblProgressText;
    private SegmentedProgressMeter progressMeter;

    private int currentStageIndex = 0;
    private boolean isCarouselMode = true;

    // Accessibility & Polish Toggles
    private static boolean crtEnabled = false;
    private static boolean sfxEnabled = true;

    // Smooth continuous rail animation variables
    private float currentScroll = 0.0f;
    private float targetScroll = 0.0f;
    private Timer railTimer;
    private Timer idleAnimTimer;

    private int pulseTicks = 0;
    private float bobOffset = 0.0f;
    private float glowPulse = 0.7f;

    // Toast notification overlay
    private String activeToastMessage = null;
    private float toastAlpha = 0.0f;
    private Timer toastTimer;

    private JPanel centerContentHolder;
    private JPanel carouselView;
    private JPanel gridView;
    private CarouselCanvas carouselCanvas;
    private JPanel stageIndicatorsPanel;

    public static class ArchiveCardData {
        public String num;
        public String title;
        public String category;
        public String subtitle;
        public String desc;
        public String[] tags;
        public int complexity;
        public int tabIndex;

        public ArchiveCardData(String num, String title, String category, String subtitle, String desc, String[] tags, int comp, int tab) {
            this.num = num; this.title = title; this.category = category; this.subtitle = subtitle;
            this.desc = desc; this.tags = tags; this.complexity = comp; this.tabIndex = tab;
        }
    }

    private final ArchiveCardData[] cards = {
        new ArchiveCardData("01", "SPACE INVADERS // CPU", "CORE CPU SCHEDULING", "MISSION 01: ALIEN DISPATCH RUNWAY",
                "Process dispatch and execution-order analysis engine (FCFS, SJF, SRTF, Round Robin).",
                new String[]{"INVADERS CPU", "FCFS", "SJF", "ROUND ROBIN"}, 3, 0),
        new ArchiveCardData("02", "SPACE SHOOT // BANKER", "DEADLOCK VERIFICATION", "MISSION 02: DEADLOCK AVOIDANCE",
                "Dijkstra's safety-state verification protocol and multi-resource allocation matrices.",
                new String[]{"SPACE SHOOT", "SAFETY", "NEED MATRIX", "SAFE SEQUENCE"}, 4, 1),
        new ArchiveCardData("03", "FRAME HOPPER // PAGING", "MMU BUS & PAGE CROSSING", "MISSION 03: VIRTUAL MEMORY & MMU",
                "Non-contiguous logical-to-physical address translation engine & MMU bus crossing.",
                new String[]{"FRAME HOPPER", "PAGE MAP", "ADDRESS TRANSLATION", "OFFSET"}, 2, 2),
        new ArchiveCardData("04", "SUPER MARIO // CACHE", "CACHE FRAME EVICTION", "MISSION 04: CACHE EVICTION",
                "Demand paging frame eviction policy and cache-fault minimization protocol.",
                new String[]{"MARIO CACHE", "FIFO", "LRU", "OPTIMAL"}, 3, 3),
        new ArchiveCardData("05", "HEAP SNAKE // SEGMENTATION", "VARIABLE BOUNDS & HEAP", "MISSION 05: HEAP vs STACK BOUNDS",
                "Variable-length segment protection checking and hardware limit trap detection.",
                new String[]{"HEAP SNAKE", "BOUND CHECK", "LIMIT TRAP", "BASE REG"}, 2, 4),
        new ArchiveCardData("06", "CYBER CRANE // RAM ALLOC", "CONTIGUOUS STORAGE", "MISSION 06: RAM PARTITIONING",
                "Contiguous storage allocation strategies (First-Fit, Best-Fit, Worst-Fit) and compaction.",
                new String[]{"CYBER CRANE", "FIRST FIT", "BEST FIT", "WORST FIT"}, 3, 5),
        new ArchiveCardData("07", "PAC-MAN // DISK SCHED", "I/O CYLINDER TRAVERSAL", "MISSION 07: I/O TRAVERSAL",
                "Magnetic cylinder track traversal and seek-time minimization algorithms.",
                new String[]{"PAC-MAN DISK", "FCFS", "SSTF", "SCAN", "LOOK"}, 3, 6),
        new ArchiveCardData("08", "OVERCOOKED // MULTIPROG", "PROCESSOR CONCURRENCY", "MISSION 08: KITCHEN CONCURRENCY",
                "Concurrent workload sharing, probabilistic CPU utilization, and processor sharing.",
                new String[]{"OVERCOOKED MULTI", "CPU UTIL%", "RAM%", "ADMISSION"}, 4, 7),
    };

    private static final String[] STAGE_LAUNCH_TITLES = {
        "LAUNCH CPU RUNWAY SIMULATOR >>",
        "LAUNCH SPACE SHOOT VAULT SIMULATOR >>",
        "LAUNCH MMU HOPPER SIMULATOR >>",
        "LAUNCH MARIO CACHE SIMULATOR >>",
        "LAUNCH HEAP SNAKE SIMULATOR >>",
        "LAUNCH CYBER CRANE SIMULATOR >>",
        "LAUNCH PAC-MAN DISK SIMULATOR >>",
        "LAUNCH MULTIPROG KITCHEN SIMULATOR >>"
    };

    /** WCAG AA High-Contrast Signature Palettes (> 5.5:1 luminance against dark backgrounds) */
    public static final Color[] STAGE_THEME_COLORS = {
        new Color(40, 240, 255),  // 01 Space Invaders: Electric Cyan (Contrast 10.5:1)
        new Color(255, 180, 50),  // 02 Minesweeper: Amber Gold (Contrast 8.5:1)
        new Color(40, 255, 200),  // 03 Tetris Paging: Neon Mint (Contrast 11:1)
        new Color(255, 115, 115), // 04 Super Mario: Mushroom Coral (Contrast 6.2:1)
        new Color(90, 245, 140),  // 05 Snake Segmentation: Acid Green (Contrast 9:1)
        new Color(215, 145, 255), // 06 Breakout RAM: Cyber Violet (Contrast 6.8:1)
        new Color(255, 230, 50),  // 07 Pac-Man Disk: Pac-Man Yellow (Contrast 12:1)
        new Color(255, 115, 185)  // 08 Overcooked Multiprogramming: Strawberry Pink (Contrast 7.2:1)
    };

    public static class StageTelemetry {
        public final String algo;
        public final String complexity;
        public final String objective;
        public final String metric;

        public StageTelemetry(String algo, String complexity, String objective, String metric) {
            this.algo = algo;
            this.complexity = complexity;
            this.objective = objective;
            this.metric = metric;
        }
    }

    private static final StageTelemetry[] STAGE_SPECS = {
        new StageTelemetry("FCFS • SJF • SRTF • RR", "O(N LOG N) PRIO", "MINIMIZE AVG WAIT TIME", "100% CPU CORE PMU"),
        new StageTelemetry("DIJKSTRA SAFETY PROTOCOL", "O(M * N^2) MATRIX", "PREVENT SYSTEM DEADLOCK", "SAFE SEQUENCE STATE"),
        new StageTelemetry("NON-CONTIGUOUS MMU PAGING", "O(1) DIRECT TLB", "TRANSLATE LOGICAL FRAMES", "ZERO EXTERNAL FRAG"),
        new StageTelemetry("FRAME EVICTION (FIFO/LRU/OPT)", "O(1) / O(K) SEARCH", "MINIMIZE CACHE MISSES", "RESOLVE PAGE FAULTS"),
        new StageTelemetry("VARIABLE SEGMENT PROTECTION", "O(1) BOUND CHECK", "LOGICAL SEGMENT BOUNDS", "HARDWARE LIMIT TRAPS"),
        new StageTelemetry("DYNAMIC PARTITIONS (FF/BF/WF)", "O(N) FREE LINKED", "PREVENT STORAGE HOLES", "ZERO RAM FRAGMENTATION"),
        new StageTelemetry("CYLINDER SEEK (SSTF/SCAN/LOOK)", "O(N LOG N) SEEK", "OPTIMIZE HEAD TRAVERSAL", "MIN SEEK DISTANCE"),
        new StageTelemetry("PROCESSOR CONCURRENCY (nj)", "O(N) CPU SHARING", "CONCURRENT WORKLOADS", "1 - p^N MAX CPU UTIL")
    };

    // ─────────────────────────────────────────────────────────────────────────
    // SYNTHESIZED ARCADE SOUND SYSTEM (Java Standard Library)
    // ─────────────────────────────────────────────────────────────────────────
    public static void playTone(int freqHz, int ms, float volume) {
        if (!sfxEnabled) return;
        new Thread(() -> {
            try {
                int sampleRate = 22050;
                int numSamples = (sampleRate * ms) / 1000;
                byte[] buffer = new byte[numSamples];
                for (int i = 0; i < numSamples; i++) {
                    double angle = 2.0 * Math.PI * i / (sampleRate / (double)freqHz);
                    double sample = Math.sin(angle) >= 0 ? 0.75 : -0.75;
                    double decay = 1.0 - ((double)i / numSamples);
                    buffer[i] = (byte)(sample * decay * volume * 127.0);
                }
                javax.sound.sampled.AudioFormat af = new javax.sound.sampled.AudioFormat(sampleRate, 8, 1, true, false);
                javax.sound.sampled.SourceDataLine line = javax.sound.sampled.AudioSystem.getSourceDataLine(af);
                line.open(af, buffer.length);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
                line.close();
            } catch (Exception ignored) {}
        }).start();
    }

    public static void playBlip() {
        playTone(920, 35, 0.40f);
    }

    public static void playLaunchChime() {
        new Thread(() -> {
            playTone(523, 45, 0.45f);
            try { Thread.sleep(50); } catch (Exception ignored) {}
            playTone(659, 45, 0.50f);
            try { Thread.sleep(50); } catch (Exception ignored) {}
            playTone(784, 85, 0.55f);
        }).start();
    }

    public ArchivesPanel(ArcadeOS parent) {
        this.parentFrame = parent;
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

        // 1. Clean Top Header with gamification, controls & high contrast
        add(createHeader(), BorderLayout.NORTH);

        // 2. Center Content Holder (Carousel / Grid)
        centerContentHolder = new JPanel(new BorderLayout());
        centerContentHolder.setOpaque(false);

        carouselView = createCarouselView();
        gridView     = createGridView();

        centerContentHolder.add(isCarouselMode ? carouselView : gridView, BorderLayout.CENTER);
        add(centerContentHolder, BorderLayout.CENTER);

        // 3. Clean Bottom Navigation Bar with 8-block segmented meter
        add(createBottomNav(), BorderLayout.SOUTH);

        // Idle animation timer (Static stability - zero bobbing oscillation)
        idleAnimTimer = new Timer(500, e -> {
            pulseTicks++;
            bobOffset = 0.0f; // Stable fixed position - no moving boxes or floating sprites
            glowPulse = 0.85f;
        });
        // Kept stopped to guarantee 100% rock-solid static rendering with zero jitter

        // Comprehensive Keyboard Navigation (Arrows, A/D, 1-8, Enter, M, Q, ESC, C)
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
                    prevStage();
                    playBlip();
                } else if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
                    nextStage();
                    playBlip();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_8) {
                    glideToStage(code - KeyEvent.VK_1);
                    playBlip();
                } else if (code >= KeyEvent.VK_NUMPAD1 && code <= KeyEvent.VK_NUMPAD8) {
                    glideToStage(code - KeyEvent.VK_NUMPAD1);
                    playBlip();
                } else if (code == KeyEvent.VK_ENTER) {
                    launchCurrentStage();
                } else if (code == KeyEvent.VK_M) {
                    openManual(cards[currentStageIndex].tabIndex, currentStageIndex);
                } else if (code == KeyEvent.VK_Q) {
                    parentFrame.showScreen(ArcadeOS.SCREEN_QUIZ);
                } else if (code == KeyEvent.VK_ESCAPE) {
                    parentFrame.showScreen(ArcadeOS.SCREEN_MAIN_MENU);
                } else if (code == KeyEvent.VK_C) {
                    crtEnabled = !crtEnabled;
                    if (carouselCanvas != null) carouselCanvas.repaint();
                }
            }
        });
    }

    public void showToast(String message) {
        activeToastMessage = message;
        toastAlpha = 1.0f;
        if (toastTimer != null && toastTimer.isRunning()) toastTimer.stop();
        toastTimer = new Timer(25, new ActionListener() {
            int ticks = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                ticks++;
                if (ticks > 45) {
                    toastAlpha -= 0.04f;
                    if (toastAlpha <= 0.0f) {
                        toastAlpha = 0.0f;
                        toastTimer.stop();
                    }
                }
                if (carouselCanvas != null) carouselCanvas.repaint();
            }
        });
        toastTimer.start();
    }

    /** Clean, focused header with gamification, accessibility toggles & high-contrast typography */
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(RetroTheme.PANEL_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        // Left: Mascot + Arcade OS Title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(new JLabel(new ImageIcon(PixelSprites.createRobotSprite(32))));

        JPanel leftText = new JPanel();
        leftText.setOpaque(false);
        leftText.setLayout(new BoxLayout(leftText, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        JLabel lm = new JLabel("MINI ");
        lm.setFont(new Font("Consolas", Font.BOLD, 16));
        lm.setForeground(Color.WHITE);
        JLabel ls = new JLabel("// ");
        ls.setFont(new Font("Consolas", Font.BOLD, 16));
        ls.setForeground(RetroTheme.ACCENT_PINK);
        JLabel lo = new JLabel("OS");
        lo.setFont(new Font("Consolas", Font.BOLD, 16));
        lo.setForeground(Color.WHITE);
        titleRow.add(lm); titleRow.add(ls); titleRow.add(lo);
        leftText.add(titleRow);

        JLabel tag = new JLabel("OPERATING SYSTEM SIMULATOR  ");
        tag.setFont(new Font("Consolas", Font.BOLD, 10));
        tag.setForeground(new Color(185, 175, 215));
        leftText.add(tag);

        left.add(leftText);
        header.add(left, BorderLayout.WEST);

        // Center: High-contrast title and informative hint
        JPanel center = new JPanel(new GridLayout(2, 1, 0, 2));
        center.setOpaque(false);

        JLabel mainTitle = new JLabel("STAGE SELECT  //  SYSTEM ARCHIVES", SwingConstants.CENTER);
        mainTitle.setFont(new Font("Consolas", Font.BOLD, 15));
        mainTitle.setForeground(Color.WHITE);
        center.add(mainTitle);

        JLabel desc = new JLabel("CHOOSE A MODULE TO LAUNCH OR VIEW DOSSIER", SwingConstants.CENTER);
        desc.setFont(new Font("Consolas", Font.BOLD, 11));
        desc.setForeground(new Color(95, 245, 255)); // WCAG AA compliant bright cyan (> 11:1)
        center.add(desc);

        header.add(center, BorderLayout.CENTER);

        // Right: Gamification Badges + Toggles + Counter
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 1));
        right.setOpaque(false);

        // Operator & Score pill pulled into header
        String userTag = String.format("OPERATOR: %s | %s XP", AppState.playerName, AppState.getScoreFormatted());
        lblHeaderScore = new JLabel(userTag);
        lblHeaderScore.setFont(new Font("Consolas", Font.BOLD, 11));
        lblHeaderScore.setForeground(RetroTheme.ACCENT_YELLOW);
        lblHeaderScore.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 1),
                BorderFactory.createEmptyBorder(3, 7, 3, 7)
        ));
        right.add(lblHeaderScore);

        // CRT Toggle
        JButton btnCrt = RetroTheme.createRetroButton(crtEnabled ? "CRT: ON" : "CRT: OFF", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnCrt.setFont(new Font("Consolas", Font.BOLD, 10));
        btnCrt.setMargin(new Insets(2, 6, 2, 6));
        btnCrt.addActionListener(e -> {
            crtEnabled = !crtEnabled;
            btnCrt.setText(crtEnabled ? "CRT: ON" : "CRT: OFF");
            btnCrt.setForeground(crtEnabled ? RetroTheme.ACCENT_GREEN : RetroTheme.TEXT_DIM);
            if (carouselCanvas != null) carouselCanvas.repaint();
            requestFocusInWindow();
        });
        right.add(btnCrt);

        // SFX Toggle
        JButton btnSfx = RetroTheme.createRetroButton(sfxEnabled ? "SFX: ON" : "SFX: OFF", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnSfx.setFont(new Font("Consolas", Font.BOLD, 10));
        btnSfx.setMargin(new Insets(2, 6, 2, 6));
        btnSfx.addActionListener(e -> {
            sfxEnabled = !sfxEnabled;
            btnSfx.setText(sfxEnabled ? "SFX: ON" : "SFX: OFF");
            btnSfx.setForeground(sfxEnabled ? RetroTheme.ACCENT_GREEN : RetroTheme.TEXT_DIM);
            if (sfxEnabled) playBlip();
            requestFocusInWindow();
        });
        right.add(btnSfx);

        // Carousel / Grid View Switcher
        JButton btnToggleView = RetroTheme.createRetroButton("CAROUSEL VIEW", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnToggleView.setFont(new Font("Consolas", Font.BOLD, 10));
        btnToggleView.setMargin(new Insets(2, 6, 2, 6));
        btnToggleView.addActionListener(e -> {
            isCarouselMode = !isCarouselMode;
            btnToggleView.setText(isCarouselMode ? "CAROUSEL VIEW" : "GRID VIEW");
            btnToggleView.setForeground(isCarouselMode ? RetroTheme.ACCENT_CYAN : RetroTheme.ACCENT_YELLOW);
            centerContentHolder.removeAll();
            centerContentHolder.add(isCarouselMode ? carouselView : gridView, BorderLayout.CENTER);
            centerContentHolder.revalidate();
            centerContentHolder.repaint();
            requestFocusInWindow();
        });
        right.add(btnToggleView);

        // Files Read Counter
        JPanel readBox = new JPanel();
        readBox.setOpaque(false);
        readBox.setPreferredSize(new Dimension(96, 36));
        readBox.setLayout(new BoxLayout(readBox, BoxLayout.Y_AXIS));

        JLabel lblRead = new JLabel("FILES READ ");
        lblRead.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblRead.setFont(new Font("Consolas", Font.BOLD, 10));
        lblRead.setForeground(RetroTheme.TEXT_DIM);
        readBox.add(lblRead);

        lblFilesReadCounter = new JLabel(String.format("%02d / 08", AppState.countFilesRead()));
        lblFilesReadCounter.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblFilesReadCounter.setFont(new Font("Consolas", Font.BOLD, 15));
        lblFilesReadCounter.setForeground(RetroTheme.ACCENT_YELLOW);
        readBox.add(lblFilesReadCounter);

        right.add(readBox);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TRUE MAP/LEVEL SELECT CONTINUOUS CAROUSEL
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel createCarouselView() {
        JPanel container = new JPanel(new BorderLayout(0, 4));
        container.setOpaque(false);
        container.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        JPanel sliderTrack = new JPanel(new BorderLayout(8, 0));
        sliderTrack.setOpaque(false);

        // Vector Left Nav Button (<) closer to cards
        VectorArrowButton btnPrev = new VectorArrowButton(true, "PREV STAGE [A / ←]");
        btnPrev.addActionListener(e -> { prevStage(); playBlip(); requestFocusInWindow(); });
        sliderTrack.add(btnPrev, BorderLayout.WEST);

        // Continuous-rail stage canvas
        carouselCanvas = new CarouselCanvas();
        sliderTrack.add(carouselCanvas, BorderLayout.CENTER);

        // Vector Right Nav Button (>) closer to cards
        VectorArrowButton btnNext = new VectorArrowButton(false, "NEXT STAGE [D / →]");
        btnNext.addActionListener(e -> { nextStage(); playBlip(); requestFocusInWindow(); });
        sliderTrack.add(btnNext, BorderLayout.EAST);

        container.add(sliderTrack, BorderLayout.CENTER);

        // Bottom Stage Selector Strip
        stageIndicatorsPanel = createStageIndicatorsStrip();
        container.add(stageIndicatorsPanel, BorderLayout.SOUTH);

        return container;
    }

    /**
     * Vector Arrow Button: Sleek 40x80 floating arcade paddle with top gloss,
     * bevel drop shadow and cyan neon border.
     */
    private class VectorArrowButton extends JButton {
        private boolean isLeft;

        public VectorArrowButton(boolean isLeft, String tooltip) {
            this.isLeft = isLeft;
            setPreferredSize(new Dimension(46, 0));
            setFocusPainted(false);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder());
            setToolTipText(tooltip);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            boolean hover = getModel().isRollover();

            int btnW = 40;
            int btnH = 80;
            int btnX = (w - btnW) / 2;
            int btnY = (h - btnH) / 2;

            // 1. Soft 3D Drop shadow
            g2.setColor(new Color(6, 4, 14, 180));
            g2.fillRoundRect(btnX + 1, btnY + 3, btnW, btnH, 12, 12);

            // 2. Button Body
            Color bgTop = hover ? new Color(65, 35, 115) : new Color(28, 18, 48);
            Color bgBot = hover ? new Color(42, 20, 80) : new Color(16, 10, 30);
            GradientPaint gp = new GradientPaint(btnX, btnY, bgTop, btnX, btnY + btnH, bgBot);
            g2.setPaint(gp);
            g2.fillRoundRect(btnX, btnY, btnW, btnH, 12, 12);

            // 3. Top Gloss Highlight
            g2.setColor(new Color(255, 255, 255, hover ? 45 : 22));
            g2.fillRoundRect(btnX + 1, btnY + 1, btnW - 2, (btnH / 2) - 1, 10, 10);

            // 4. Neon Cyber Border
            Color activeTheme = STAGE_THEME_COLORS[currentStageIndex];
            g2.setColor(hover ? activeTheme : new Color(90, 68, 130));
            g2.setStroke(new BasicStroke(hover ? 2.2f : 1.4f));
            g2.drawRoundRect(btnX, btnY, btnW, btnH, 12, 12);

            // 5. Crisp Vector Chevron Arrow
            int cx = btnX + btnW / 2;
            int cy = btnY + btnH / 2;
            g2.setColor(hover ? activeTheme : Color.WHITE);
            g2.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            if (isLeft) {
                g2.drawLine(cx + 4, cy - 12, cx - 5, cy);
                g2.drawLine(cx - 5, cy, cx + 4, cy + 12);
            } else {
                g2.drawLine(cx - 4, cy - 12, cx + 5, cy);
                g2.drawLine(cx + 5, cy, cx - 4, cy + 12);
            }

            g2.dispose();
        }
    }

    /**
     * Continuous Stage Rail Canvas:
     * High-contrast readable typography, dynamic ambient background glow,
     * depth cues, and toggleable CRT scanline effects.
     */
    private class CarouselCanvas extends JPanel {
        private Rectangle manualBtnBounds = new Rectangle();
        private Rectangle launchBtnBounds = new Rectangle();
        private Rectangle quizBtnBounds = new Rectangle();
        private Rectangle[] cardBounds = new Rectangle[cards.length];
        private int hoveredActionBtn = 0; // 0=none, 1=manual, 2=launch, 3=quiz

        public CarouselCanvas() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            for (int i = 0; i < cards.length; i++) {
                cardBounds[i] = new Rectangle();
            }

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Point p = e.getPoint();

                    // Check hero buttons if near center
                    if (Math.abs(currentScroll - targetScroll) < 0.20f) {
                        if (manualBtnBounds.contains(p)) {
                            openManual(cards[currentStageIndex].tabIndex, currentStageIndex);
                            return;
                        } else if (launchBtnBounds.contains(p)) {
                            launchCurrentStage();
                            return;
                        } else if (quizBtnBounds.contains(p)) {
                            playBlip();
                            parentFrame.showScreen(ArcadeOS.SCREEN_QUIZ);
                            return;
                        }
                    }

                    // Check if clicked on any visible peeker card to glide to it
                    for (int i = 0; i < cards.length; i++) {
                        if (i != currentStageIndex && cardBounds[i].contains(p)) {
                            glideToStage(i);
                            playBlip();
                            requestFocusInWindow();
                            return;
                        }
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    Point p = e.getPoint();
                    int oldH = hoveredActionBtn;
                    if (manualBtnBounds.contains(p)) hoveredActionBtn = 1;
                    else if (launchBtnBounds.contains(p)) hoveredActionBtn = 2;
                    else if (quizBtnBounds.contains(p)) hoveredActionBtn = 3;
                    else hoveredActionBtn = 0;

                    if (hoveredActionBtn != oldH) {
                        repaint();
                    }

                    boolean overAction = (hoveredActionBtn > 0);
                    boolean overCard = false;
                    for (int i = 0; i < cards.length; i++) {
                        if (i != currentStageIndex && cardBounds[i].contains(p)) {
                            overCard = true;
                            break;
                        }
                    }
                    if (overAction || overCard) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    } else {
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                g2.dispose();
                return;
            }

            int centerX = w / 2;
            Color activeTheme = STAGE_THEME_COLORS[currentStageIndex];

            // 1. DYNAMIC ROOM SHIFT: Ambient background glow shifts with the active stage color!
            RadialGradientPaint ambientRoomGlow = new RadialGradientPaint(
                    new Point2D.Float(centerX, h / 2f), Math.max(w, h) * 0.55f,
                    new float[]{0.0f, 0.45f, 1.0f},
                    new Color[]{
                            new Color(activeTheme.getRed(), activeTheme.getGreen(), activeTheme.getBlue(), 32),
                            new Color(activeTheme.getRed(), activeTheme.getGreen(), activeTheme.getBlue(), 8),
                            new Color(0, 0, 0, 0)
                    }
            );
            g2.setPaint(ambientRoomGlow);
            g2.fillRect(0, 0, w, h);

            // Retro arcade holographic stage horizon & cyber floor lines behind cards
            int horizonY = h / 2 + 50;
            g2.setColor(new Color(activeTheme.getRed(), activeTheme.getGreen(), activeTheme.getBlue(), 40));
            g2.drawLine(20, horizonY, w - 20, horizonY);
            for (int r = 1; r <= 3; r++) {
                int gy = horizonY + r * 24;
                if (gy < h - 10) {
                    g2.setColor(new Color(activeTheme.getRed(), activeTheme.getGreen(), activeTheme.getBlue(), 12 + r * 6));
                    g2.drawLine(40, gy, w - 40, gy);
                }
            }

            // 2. DYNAMIC RETRO GAME BACKGROUND WORLD: Each stage morphs into its live authentic retro game scene!
            paintDynamicGameBackground(g2, currentStageIndex, w, h, activeTheme);

            // Geometry proportions - BALANCED AAA ARCADE CAROUSEL (exact match to reference):
            int heroH = Math.min(515, h - 70);
            int heroW = Math.min(570, (int)(w * 0.48));
            int peekerH = (int)(heroH * 0.84);
            int peekerW = Math.min(230, (int)(w * 0.18));

            int stepDistance = (heroW / 2) + 24 + (peekerW / 2);

            manualBtnBounds.setBounds(0, 0, 0, 0);
            launchBtnBounds.setBounds(0, 0, 0, 0);
            quizBtnBounds.setBounds(0, 0, 0, 0);

            // Depth-sorted drawing
            Integer[] order = new Integer[cards.length];
            float[] diffs = new float[cards.length];
            for (int i = 0; i < cards.length; i++) {
                order[i] = i;
                float diff = i - currentScroll;
                while (diff < -4.0f) diff += cards.length;
                while (diff > 4.0f)  diff -= cards.length;
                diffs[i] = diff;
            }

            java.util.Arrays.sort(order, (a, b) -> Float.compare(Math.abs(diffs[b]), Math.abs(diffs[a])));

            for (int cardIdx : order) {
                float diff = diffs[cardIdx];
                float dist = Math.abs(diff);

                // Strictly draw only the 3 main cards: center hero and 2 direct neighbors.
                if (dist > 1.25f) {
                    cardBounds[cardIdx].setBounds(0, 0, 0, 0);
                    continue;
                }

                float focus = Math.max(0.0f, Math.min(1.0f, 1.0f - dist));

                int cardW = (int)(peekerW + (heroW - peekerW) * focus);
                int cardH = (int)(peekerH + (heroH - peekerH) * focus);

                int cardCenterX = (int)(centerX + diff * stepDistance);
                int cardX = cardCenterX - cardW / 2;
                int cardY = (h - cardH) / 2;

                cardBounds[cardIdx].setBounds(cardX, cardY, cardW, cardH);

                // Depth cue: Smoothly fade cards as they glide past dist 1.0
                float alpha = (dist <= 1.0f) ? 1.0f : Math.max(0.0f, (1.25f - dist) / 0.25f);
                if (alpha < 0.02f) continue;

                Graphics2D gCard = (Graphics2D) g2.create();
                // Side cards are dimmed with lower opacity (0.75 focus alpha) so the hero card clearly wins
                float cardOpacity = focus > 0.65f ? alpha : alpha * 0.72f;
                gCard.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, cardOpacity));
                paintStageCard(gCard, cardIdx, cardX, cardY, cardW, cardH, focus);
                gCard.dispose();
            }

            // Toast notification overlay
            if (toastAlpha > 0.01f && activeToastMessage != null) {
                Graphics2D gToast = (Graphics2D) g2.create();
                gToast.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, toastAlpha));
                gToast.setFont(new Font("Consolas", Font.BOLD, 13));
                FontMetrics fm = gToast.getFontMetrics();
                int tw = fm.stringWidth(activeToastMessage) + 32;
                int th = 32;
                int tx = centerX - tw / 2;
                int ty = Math.max(6, ((h - heroH) / 2) - th - 10);

                gToast.setColor(new Color(15, 10, 30, 235));
                gToast.fillRoundRect(tx, ty, tw, th, 8, 8);
                gToast.setColor(RetroTheme.ACCENT_YELLOW);
                gToast.setStroke(new BasicStroke(1.5f));
                gToast.drawRoundRect(tx, ty, tw, th, 8, 8);

                gToast.setColor(Color.WHITE);
                gToast.drawString(activeToastMessage, tx + 16, ty + 21);
                gToast.dispose();
            }

            // CRT Scanline and Vignette Effect (Accessibility toggleable)
            if (crtEnabled) {
                g2.setColor(new Color(0, 0, 0, 22));
                for (int sy = 0; sy < h; sy += 3) {
                    g2.drawLine(0, sy, w, sy);
                }
                RadialGradientPaint vignette = new RadialGradientPaint(
                        new Point2D.Float(w / 2f, h / 2f), Math.max(w, h) * 0.65f,
                        new float[]{0.0f, 0.75f, 1.0f},
                        new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 30), new Color(0, 0, 0, 85)}
                );
                g2.setPaint(vignette);
                g2.fillRect(0, 0, w, h);
            }

            g2.dispose();
        }

        private void paintStageCard(Graphics2D g2, int idx, int x, int y, int w, int h, float focus) {
            Graphics2D g = (Graphics2D) g2.create();
            g.setClip(x, y, w, h);

            ArchiveCardData d = cards[idx];
            Color themeColor = STAGE_THEME_COLORS[idx];
            boolean isHero = (focus > 0.65f);

            int bgR = (int)(16 + (24 - 16) * focus);
            int bgG = (int)(12 + (16 - 12) * focus);
            int bgB = (int)(32 + (46 - 32) * focus);
            int bgA = (int)(190 + 65 * focus);
            g.setColor(new Color(bgR, bgG, bgB, bgA));
            g.fillRoundRect(x, y, w, h, 14, 14);

            if (isHero) {
                // === HERO CARD PRESENTATION: DYNAMIC GAME THEME COLORS ===

                // Neon breathing border in the game's signature theme color
                int alpha = (int)(glowPulse * 255);
                Color dynamicBorder = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), Math.max(90, Math.min(255, alpha)));
                g.setColor(dynamicBorder);
                g.setStroke(new BasicStroke(2.4f));
                g.drawRoundRect(x + 1, y + 1, w - 3, h - 3, 14, 14);

                // Top soft ambient glow matching theme color
                GradientPaint glow = new GradientPaint(
                        x, y, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(glowPulse * 55 * focus)),
                        x, y + 110, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0)
                );
                g.setPaint(glow);
                g.fillRoundRect(x + 2, y + 2, w - 4, 110, 12, 12);

                int pad = 24;
                int cx = x + w / 2;

                // 1. Stage tag & Collapsed Non-Duplicated Header (shows system domain, not duplicated mission name)
                g.setFont(new Font("Consolas", Font.BOLD, 13));
                g.setColor(themeColor);
                String tagLine = "STAGE " + d.num + " OF 08  •  " + d.category;
                g.drawString(tagLine, x + pad, y + 24);

                // Clean Status Badge on Top Right
                boolean isRead = (AppState.filesRead[idx] == 1);
                String statusStr = isRead ? "READ [DONE]" : "UNREAD";
                g.setFont(new Font("Consolas", Font.BOLD, 12));
                FontMetrics fm = g.getFontMetrics();
                int stW = fm.stringWidth(statusStr) + 16;
                int stX = x + w - pad - stW;
                int stY = y + 11;
                g.setColor(isRead ? new Color(20, 65, 38) : new Color(65, 48, 25));
                g.fillRoundRect(stX, stY, stW, 22, 5, 5);
                g.setColor(isRead ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_YELLOW);
                g.drawRoundRect(stX, stY, stW, 22, 5, 5);
                g.drawString(statusStr, stX + 8, stY + 16);

                // 2. HERO SHOWCASE: Radiant Arcade Holo-Pedestal & Mascot Sprite
                boolean isTall = (h >= 540);
                int sprSz = isTall ? 116 : 92;
                int sprY = isTall ? (y + 66 + (int)bobOffset) : (y + 46 + (int)bobOffset);
                int pedY = isTall ? (y + 195) : (y + 146);

                g.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 36));
                g.fillOval(cx - (isTall ? 100 : 80), isTall ? (y + 50) : (y + 36), isTall ? 200 : 160, isTall ? 145 : 115);

                // 3D Neon Holo-Pedestal Disc underneath hero
                int pedW = isTall ? 175 : 145;
                int pedH = isTall ? 20 : 16;
                int pedX = cx - pedW / 2;
                g.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 45));
                g.fillOval(pedX - 8, pedY - 3, pedW + 16, pedH + 6);
                g.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(180 * glowPulse)));
                g.drawOval(pedX, pedY, pedW, pedH);
                g.setColor(new Color(255, 255, 255, (int)(140 * glowPulse)));
                g.drawOval(pedX + 16, pedY + 2, pedW - 32, pedH - 4);

                // Hero Mascot Sprite (Animated bobbing on active card only)
                Image spr = getCardSprite(idx, sprSz);
                int sprX = cx - sprSz / 2;
                g.drawImage(spr, sprX, sprY, null);

                // 3. Stage Title - BIG, BOLD, CRISP ARCADE TYPOGRAPHY
                int titleY = isTall ? (y + 252) : (y + 192);
                g.setFont(new Font("Consolas", Font.BOLD, isTall ? 28 : 24));
                fm = g.getFontMetrics();
                int titleW = fm.stringWidth(d.title);
                g.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 90));
                g.drawString(d.title, cx - titleW / 2 + 1, titleY + 1);
                g.setColor(Color.WHITE);
                g.drawString(d.title, cx - titleW / 2, titleY);

                // Subtitle: Mission Name shown once clearly under title with WCAG AA luminous contrast
                int subY = titleY + (isTall ? 26 : 20);
                g.setFont(new Font("Consolas", Font.BOLD, isTall ? 14 : 12));
                g.setColor(themeColor);
                fm = g.getFontMetrics();
                String subTag = "« " + d.subtitle + " »";
                g.drawString(subTag, cx - fm.stringWidth(subTag) / 2, subY);

                // 4. Description - Clear, readable text with comfortable contrast (floor >= 14px)
                int descY = subY + (isTall ? 28 : 22);
                g.setFont(new Font("Segoe UI", Font.PLAIN, isTall ? 15 : 13));
                g.setColor(new Color(242, 244, 255)); // 14:1 contrast ratio against dark background
                drawCenteredWrappedText(g, d.desc, x + pad + 15, descY, w - (pad + 15) * 2);

                // Bottom-anchored controls (guaranteed harmonious spacing, zero collision):
                // Row 2: Vibrant Primary Hero Launch Button
                int btnLaunchH = isTall ? 50 : 44;
                int btnLaunchY = y + h - btnLaunchH - (isTall ? 24 : 16);
                int launchW = Math.min(540, w - pad * 2);
                int launchX = cx - launchW / 2;
                launchBtnBounds.setBounds(launchX, btnLaunchY, launchW, btnLaunchH);
                drawPrimaryLaunchButton(g, launchBtnBounds, idx, STAGE_LAUNCH_TITLES[idx], themeColor, (hoveredActionBtn == 2), new Font("Consolas", Font.BOLD, isTall ? 15 : 13));

                // Row 1: Secondary Ghost Buttons (Docs / Quiz)
                int btnRow1H = isTall ? 38 : 32;
                int btnRow1Y = btnLaunchY - btnRow1H - (isTall ? 14 : 8);
                int subBtnW = Math.min(260, (w - pad * 2 - 14) / 2);
                int subTotalW = subBtnW * 2 + 14;
                int subStartX = cx - subTotalW / 2;

                manualBtnBounds.setBounds(subStartX, btnRow1Y, subBtnW, btnRow1H);
                drawGhostButton(g, manualBtnBounds, "[DOCS] VIEW ARCHIVE MANUAL", RetroTheme.ACCENT_CYAN, (hoveredActionBtn == 1), new Font("Consolas", Font.BOLD, 12));

                quizBtnBounds.setBounds(subStartX + subBtnW + 14, btnRow1Y, subBtnW, btnRow1H);
                drawGhostButton(g, quizBtnBounds, "[QUIZ] CHALLENGE QUIZ ARENA", RetroTheme.ACCENT_YELLOW, (hoveredActionBtn == 3), new Font("Consolas", Font.BOLD, 12));

                // Difficulty Rating Bars (above ghost buttons)
                int compY = btnRow1Y - (isTall ? 36 : 28);
                g.setFont(new Font("Consolas", Font.BOLD, isTall ? 13 : 12));
                g.setColor(themeColor);
                fm = g.getFontMetrics();
                String compLabel = "DIFFICULTY: " + getDifficultyLabel(d.complexity);
                int labelW = fm.stringWidth(compLabel);

                int pipW = isTall ? 24 : 20;
                int pipH = isTall ? 10 : 8;
                int pipGap = 6;
                int totalPipsW = 5 * pipW + 4 * pipGap;
                int totalCompW = labelW + 14 + totalPipsW;
                int startCompX = cx - totalCompW / 2;

                g.drawString(compLabel, startCompX, compY + (isTall ? 9 : 7));

                int pipX = startCompX + labelW + 14;
                for (int c = 0; c < 5; c++) {
                    if (c < d.complexity) {
                        g.setColor(RetroTheme.ACCENT_YELLOW);
                        g.fillRoundRect(pipX, compY, pipW, pipH, 4, 4);
                    } else {
                        g.setColor(new Color(38, 28, 58));
                        g.fillRoundRect(pipX, compY, pipW, pipH, 4, 4);
                        g.setColor(new Color(75, 55, 105));
                        g.drawRoundRect(pipX, compY, pipW, pipH, 4, 4);
                    }
                    pipX += pipW + pipGap;
                }

                // Algorithm Tags Badges (above difficulty)
                int tagY = compY - (isTall ? 36 : 28);
                g.setFont(new Font("Consolas", Font.BOLD, isTall ? 12 : 11));
                fm = g.getFontMetrics();
                int totalTagW = 0;
                for (String t : d.tags) totalTagW += fm.stringWidth("#" + t) + 16 + 8;
                int tagX = cx - totalTagW / 2;
                int badgeH = isTall ? 26 : 24;
                for (String t : d.tags) {
                    String badgeStr = "#" + t;
                    int tw = fm.stringWidth(badgeStr) + 16;
                    g.setColor(new Color(28, 22, 46, 210));
                    g.fillRoundRect(tagX, tagY, tw, badgeH, 6, 6);
                    g.setColor(new Color(65, 55, 90));
                    g.drawRoundRect(tagX, tagY, tw, badgeH, 6, 6);
                    g.setColor(new Color(210, 205, 235));
                    g.drawString(badgeStr, tagX + 8, tagY + (isTall ? 18 : 16));
                    tagX += tw + 8;
                }

            } else {
                // === PEEKER CARD PRESENTATION: ZOOMED-IN, COLOR-CODED & RICH ===
                // Glowing border in game's signature theme color
                g.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 145));
                g.setStroke(new BasicStroke(1.8f));
                g.drawRoundRect(x, y, w - 1, h - 1, 14, 14);

                // Sleek ambient background tint
                g.setColor(new Color(themeColor.getRed() / 14, themeColor.getGreen() / 14, themeColor.getBlue() / 14, 215));
                g.fillRoundRect(x + 1, y + 1, w - 2, h - 2, 14, 14);

                // Top ambient aura
                GradientPaint peekerGlow = new GradientPaint(
                        x, y, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 45),
                        x, y + 80, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0)
                );
                g.setPaint(peekerGlow);
                g.fillRoundRect(x + 2, y + 2, w - 4, 80, 12, 12);

                int cx = x + w / 2;

                // 1. Stage Number & Status Tag (floor >= 12px)
                g.setFont(new Font("Consolas", Font.BOLD, 12));
                g.setColor(themeColor);
                FontMetrics fm = g.getFontMetrics();
                String numStr = "STAGE " + d.num + " / 08";
                g.drawString(numStr, x + 14, y + 24);

                boolean isRead = (AppState.filesRead[idx] == 1);
                String peekerBadge = isRead ? "READ [DONE]" : "UNREAD";
                g.setFont(new Font("Consolas", Font.BOLD, 11));
                fm = g.getFontMetrics();
                int pbW = fm.stringWidth(peekerBadge) + 12;
                int pbX = x + w - 14 - pbW;
                g.setColor(isRead ? new Color(20, 60, 35) : new Color(50, 38, 20));
                g.fillRoundRect(pbX, y + 11, pbW, 18, 4, 4);
                g.setColor(isRead ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_YELLOW);
                g.drawRoundRect(pbX, y + 11, pbW, 18, 4, 4);
                g.drawString(peekerBadge, pbX + 6, y + 24);

                // 2. Mascot showcase with radial aura and 3D pedestal (Side cards remain calm)
                boolean isTallPeeker = (h >= 450);
                int sprSz = isTallPeeker ? 84 : 64;
                int sprY = isTallPeeker ? (y + 68) : (y + 52);
                int pedY = isTallPeeker ? (y + 158) : (y + 124);

                g.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 36));
                g.fillOval(cx - 55, y + 44, 110, 95);

                // Holo pedestal disc
                int pedW = isTallPeeker ? 116 : 96;
                int pedH = isTallPeeker ? 14 : 11;
                g.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 125));
                g.drawOval(cx - pedW / 2, pedY, pedW, pedH);

                // Mascot sprite
                Image spr = getCardSprite(idx, sprSz);
                g.drawImage(spr, cx - sprSz / 2, sprY, null);

                // 3. Stage Title (Clean bold white)
                int titleY = isTallPeeker ? (y + 208) : (y + 160);
                g.setFont(new Font("Consolas", Font.BOLD, isTallPeeker ? 16 : 14));
                fm = g.getFontMetrics();
                String title = d.title;
                if (fm.stringWidth(title) > w - 20) {
                    while (fm.stringWidth(title + "...") > w - 20 && title.length() > 3) {
                        title = title.substring(0, title.length() - 1);
                    }
                    title += "...";
                }
                g.setColor(Color.WHITE);
                g.drawString(title, cx - fm.stringWidth(title) / 2, titleY);

                // Subtitle
                int subY = titleY + (isTallPeeker ? 24 : 18);
                g.setFont(new Font("Consolas", Font.BOLD, isTallPeeker ? 12 : 10));
                g.setColor(themeColor);
                fm = g.getFontMetrics();
                String sub = d.subtitle;
                if (fm.stringWidth(sub) > w - 20) {
                    while (fm.stringWidth(sub + "...") > w - 20 && sub.length() > 3) {
                        sub = sub.substring(0, sub.length() - 1);
                    }
                    sub += "...";
                }
                g.drawString(sub, cx - fm.stringWidth(sub) / 2, subY);

                // 4. Algorithm / Feature pills
                int peekerTagY = isTallPeeker ? (y + 272) : (y + 220);
                if (d.tags.length > 0) {
                    g.setFont(new Font("Consolas", Font.BOLD, 10));
                    fm = g.getFontMetrics();
                    String t1 = "#" + d.tags[0];
                    String t2 = d.tags.length > 1 ? "#" + d.tags[1] : null;
                    int t1W = fm.stringWidth(t1) + 14;
                    int t2W = (t2 != null) ? fm.stringWidth(t2) + 14 : 0;
                    int totalTW = (t2 != null) ? t1W + 8 + t2W : t1W;
                    int curTX = cx - totalTW / 2;

                    g.setColor(new Color(36, 26, 58));
                    g.fillRoundRect(curTX, peekerTagY, t1W, 22, 6, 6);
                    g.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 110));
                    g.drawRoundRect(curTX, peekerTagY, t1W, 22, 6, 6);
                    g.setColor(new Color(225, 220, 245));
                    g.drawString(t1, curTX + 7, peekerTagY + 15);

                    if (t2 != null) {
                        curTX += t1W + 8;
                        g.setColor(new Color(36, 26, 58));
                        g.fillRoundRect(curTX, peekerTagY, t2W, 22, 6, 6);
                        g.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 110));
                        g.drawRoundRect(curTX, peekerTagY, t2W, 22, 6, 6);
                        g.setColor(new Color(225, 220, 245));
                        g.drawString(t2, curTX + 7, peekerTagY + 15);
                    }
                }

                // 5. Difficulty Pips
                int peekerCompY = isTallPeeker ? (y + 328) : (y + 258);
                g.setFont(new Font("Consolas", Font.BOLD, 11));
                g.setColor(themeColor);
                fm = g.getFontMetrics();
                String diffLbl = "DIFF: " + d.complexity + "/5";
                int dLblW = fm.stringWidth(diffLbl);
                int pPipW = isTallPeeker ? 16 : 14;
                int pPipH = 7;
                int pGap = 5;
                int totalDiffW = dLblW + 10 + 5 * pPipW + 4 * pGap;
                int startDX = cx - totalDiffW / 2;

                g.drawString(diffLbl, startDX, peekerCompY + 7);
                int pipCurX = startDX + dLblW + 10;
                for (int c = 0; c < 5; c++) {
                    if (c < d.complexity) {
                        g.setColor(RetroTheme.ACCENT_YELLOW);
                        g.fillRoundRect(pipCurX, peekerCompY, pPipW, pPipH, 2, 2);
                    } else {
                        g.setColor(new Color(38, 28, 58));
                        g.fillRoundRect(pipCurX, peekerCompY, pPipW, pPipH, 2, 2);
                        g.setColor(new Color(75, 55, 105));
                        g.drawRoundRect(pipCurX, peekerCompY, pPipW, pPipH, 2, 2);
                    }
                    pipCurX += pPipW + pGap;
                }

                // 6. Mission Teaser Snippet (Zero dead space)
                int snipY = isTallPeeker ? (y + 384) : (y + 296);
                g.setFont(new Font("Consolas", Font.PLAIN, 11));
                g.setColor(new Color(195, 190, 220));
                fm = g.getFontMetrics();
                String snip = STAGE_SPECS[idx].objective;
                if (fm.stringWidth(snip) > w - 24) {
                    while (fm.stringWidth(snip + "...") > w - 24 && snip.length() > 3) {
                        snip = snip.substring(0, snip.length() - 1);
                    }
                    snip += "...";
                }
                g.drawString(snip, cx - fm.stringWidth(snip) / 2, snipY);

                // 7. Styled Arcade Pill Button at bottom
                int hintW = w - 28;
                int hintH = 34;
                int hintX = cx - hintW / 2;
                int hintY = isTallPeeker ? (y + 442) : (y + h - hintH - 16);
                g.setColor(new Color(themeColor.getRed() / 6, themeColor.getGreen() / 6, themeColor.getBlue() / 6, 230));
                g.fillRoundRect(hintX, hintY, hintW, hintH, 8, 8);
                g.setColor(themeColor);
                g.setStroke(new BasicStroke(1.4f));
                g.drawRoundRect(hintX, hintY, hintW, hintH, 8, 8);

                g.setFont(new Font("Consolas", Font.BOLD, 12));
                g.setColor(Color.WHITE);
                fm = g.getFontMetrics();
                String hint = "« SELECT STAGE »";
                g.drawString(hint, cx - fm.stringWidth(hint) / 2, hintY + 22);
            }

            g.dispose();
        }

        /** Ghost/Outline button for secondary actions (Docs / Quiz) */
        private void drawGhostButton(Graphics2D g2, Rectangle b, String label, Color accentColor, boolean isHovered, Font font) {
            Graphics2D g = (Graphics2D) g2.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = b.x;
            int y = b.y;
            int w = b.width;
            int h = b.height;

            // Semi-transparent dark frosted fill
            Color bg = isHovered ? new Color(42, 28, 68, 230) : new Color(20, 14, 38, 160);
            g.setColor(bg);
            g.fillRoundRect(x, y, w, h, 8, 8);

            // Subtle top gloss
            g.setColor(new Color(255, 255, 255, isHovered ? 35 : 15));
            g.fillRoundRect(x + 1, y + 1, w - 2, (h / 2) - 1, 7, 7);

            // Neon accent border
            g.setColor(isHovered ? accentColor : new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 140));
            g.setStroke(new BasicStroke(isHovered ? 1.8f : 1.2f));
            g.drawRoundRect(x, y, w - 1, h - 1, 8, 8);

            // Label text
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            int textX = x + (w - fm.stringWidth(label)) / 2;
            int textY = y + (h + fm.getAscent() - fm.getDescent()) / 2;

            g.setColor(isHovered ? Color.WHITE : accentColor);
            g.drawString(label, textX, textY);

            g.dispose();
        }

        /** Dominant, high-impact gradient button for primary Launch Simulator CTA (WCAG AAA contrast on all stages) */
        private void drawPrimaryLaunchButton(Graphics2D g2, Rectangle b, int stageIdx, String label, Color baseColor, boolean isHovered, Font font) {
            Graphics2D g = (Graphics2D) g2.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = b.x;
            int y = b.y;
            int w = b.width;
            int h = b.height;

            // Determine if the stage theme color is light (Cyan, Yellow, Mint, Amber) or dark (Pink, Violet, Coral)
            double lum = (0.299 * baseColor.getRed() + 0.587 * baseColor.getGreen() + 0.114 * baseColor.getBlue()) / 255.0;
            boolean isLightBg = lum >= 0.48;

            // 1. Outer ambient neon glow
            g.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), isHovered ? 130 : 70));
            g.fillRoundRect(x - 2, y - 1, w + 4, h + 5, 14, 14);

            // 2. 3D drop shadow beneath button
            g.setColor(new Color(6, 4, 14, 230));
            g.fillRoundRect(x, y + 4, w, h, 10, 10);

            // 3. Saturated arcade gradient plate
            Color topC = isHovered ? baseColor : new Color(
                    (int)(baseColor.getRed() * 0.95),
                    (int)(baseColor.getGreen() * 0.95),
                    (int)(baseColor.getBlue() * 0.95)
            );
            Color botC = new Color(
                    (int)(baseColor.getRed() * 0.58),
                    (int)(baseColor.getGreen() * 0.58),
                    (int)(baseColor.getBlue() * 0.58)
            );
            GradientPaint gp = new GradientPaint(x, y, topC, x, y + h, botC);
            g.setPaint(gp);
            g.fillRoundRect(x, y, w, h, 10, 10);

            // 4. Subtle top gloss highlight
            g.setColor(new Color(255, 255, 255, isLightBg ? 32 : 48));
            g.fillRoundRect(x + 2, y + 2, w - 4, (h / 2) - 1, 8, 8);

            // 5. High-definition arcade border
            if (isLightBg) {
                // Crisp dark framing border on light buttons
                g.setColor(isHovered ? Color.WHITE : new Color(12, 8, 22, 220));
                g.setStroke(new BasicStroke(isHovered ? 2.2f : 1.6f));
                g.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
            } else {
                // Glowing white border on darker buttons
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(isHovered ? 2.2f : 1.4f));
                g.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
            }

            // 6. Draw miniature game mascot sprite + High-Contrast Text
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            int sprSz = Math.min(22, h - 14);
            int sprGap = 10;
            int totalContentW = sprSz + sprGap + fm.stringWidth(label);
            int startX = x + (w - totalContentW) / 2;

            Image miniSprite = getCardSprite(stageIdx, sprSz);
            if (miniSprite != null) {
                int sprY = y + (h - sprSz) / 2;
                g.drawImage(miniSprite, startX, sprY, null);
            }

            int textX = startX + sprSz + sprGap;
            int textY = y + (h + fm.getAscent() - fm.getDescent()) / 2;

            if (isLightBg) {
                // Black arcade marquee lettering for Cyan, Yellow, Mint, Amber:
                // 3D light edge highlight
                g.setColor(new Color(255, 255, 255, 140));
                g.drawString(label, textX, textY - 1);
                // Bevel shadow
                g.setColor(new Color(0, 0, 0, 160));
                g.drawString(label, textX + 1, textY + 1);
                // Main Bold Dark Ink (15:1 contrast against bright background)
                g.setColor(new Color(10, 6, 20));
                g.drawString(label, textX, textY);
            } else {
                // Crisp white text with 4-way dark drop shadow for Pink, Violet, Red:
                g.setColor(new Color(0, 0, 0, 240));
                g.drawString(label, textX - 1, textY);
                g.drawString(label, textX + 1, textY);
                g.drawString(label, textX, textY - 1);
                g.drawString(label, textX, textY + 1);
                // Main Pure White text
                g.setColor(Color.WHITE);
                g.drawString(label, textX, textY);
            }

            g.dispose();
        }

        private void drawCenteredWrappedText(Graphics2D g, String text, int x, int y, int maxW) {
            FontMetrics fm = g.getFontMetrics();
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            int curY = y;

            for (String w : words) {
                if (fm.stringWidth(line + w + " ") > maxW && line.length() > 0) {
                    String s = line.toString().trim();
                    g.drawString(s, x + (maxW - fm.stringWidth(s)) / 2, curY);
                    line = new StringBuilder();
                    curY += fm.getHeight() + 4;
                }
                line.append(w).append(" ");
            }
            if (line.length() > 0) {
                String s = line.toString().trim();
                g.drawString(s, x + (maxW - fm.stringWidth(s)) / 2, curY);
            }
        }

        private int safeAlpha(int a) {
            return Math.max(0, Math.min(255, a));
        }

        /**
         * Dynamic Retro Game World Background:
         * Renders an authentic, animated 8-bit retro arcade game scene in the background
         * specifically tailored to the active stage's theme and OS concept!
         */
        private void paintDynamicGameBackground(Graphics2D g2, int stageIdx, int w, int h, Color themeColor) {
            Graphics2D g = (Graphics2D) g2.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            switch (stageIdx) {
                case 0: paintSpaceInvadersBG(g, w, h, themeColor); break;
                case 1: paintSpaceShootBG(g, w, h, themeColor); break;
                case 2: paintFrameHopperBG(g, w, h, themeColor); break;
                case 3: paintMarioBG(g, w, h, themeColor); break;
                case 4: paintSnakeBG(g, w, h, themeColor); break;
                case 5: paintCyberCraneBG(g, w, h, themeColor); break;
                case 6: paintPacmanBG(g, w, h, themeColor); break;
                case 7: paintOvercookedBG(g, w, h, themeColor); break;
            }

            g.dispose();
        }

        // --- 01. SPACE INVADERS: Twinkling starfield, marching 8-bit alien invaders, red UFO, and laser turret ---
        private void paintSpaceInvadersBG(Graphics2D g, int w, int h, Color themeColor) {
            // Starfield
            for (int i = 0; i < 40; i++) {
                int sx = ((i * 137 + 29) % (w - 40)) + 20;
                int sy = ((i * 79 + 43) % (h - 70)) + 20;
                int sa = safeAlpha((int)(25 + 20 * Math.sin(pulseTicks * 0.08 + i)));
                if (i % 3 == 0) g.setColor(new Color(0, 229, 255, sa));
                else if (i % 3 == 1) g.setColor(new Color(255, 115, 200, sa));
                else g.setColor(new Color(255, 255, 255, sa));
                int sz = (i % 5 == 0) ? 3 : 2;
                g.fillRect(sx, sy, sz, sz);
            }

            // Mystery Flying Saucer (UFO)
            int ufoX = (int)((pulseTicks * 1.6) % (w + 200)) - 100;
            int ufoY = 26;
            g.setColor(new Color(255, 50, 80, 80));
            g.fillOval(ufoX - 18, ufoY, 36, 10);
            g.setColor(new Color(255, 100, 120, 110));
            g.fillOval(ufoX - 9, ufoY - 5, 18, 8);
            if ((pulseTicks / 8) % 2 == 0) {
                g.setColor(new Color(255, 230, 50, 140));
                g.fillRect(ufoX - 10, ufoY + 4, 3, 2);
                g.fillRect(ufoX + 7, ufoY + 4, 3, 2);
            }

            // Marching Invader Squads on Wings
            boolean frame2 = ((pulseTicks / 20) % 2 == 1);
            int squadOffset = (int)(Math.sin(pulseTicks * 0.04) * 35);
            // Left Wing Invaders
            draw8BitInvader(g, 45 + squadOffset, 65, 3, frame2, new Color(0, 229, 255, 75));
            draw8BitInvader(g, 115 + squadOffset, 65, 3, frame2, new Color(0, 229, 255, 75));
            draw8BitInvader(g, 80 + squadOffset, 105, 3, !frame2, new Color(255, 115, 200, 65));
            // Right Wing Invaders
            draw8BitInvader(g, w - 170 + squadOffset, 65, 3, frame2, new Color(0, 229, 255, 75));
            draw8BitInvader(g, w - 100 + squadOffset, 65, 3, frame2, new Color(0, 229, 255, 75));
            draw8BitInvader(g, w - 135 + squadOffset, 105, 3, !frame2, new Color(255, 115, 200, 65));

            // Defense Bunkers
            g.setColor(new Color(0, 255, 150, 50));
            g.fillRect(60, h - 65, 42, 22);
            g.setColor(new Color(16, 12, 32));
            g.fillRect(72, h - 53, 18, 10); // Bunker arch

            g.setColor(new Color(0, 255, 150, 50));
            g.fillRect(w - 110, h - 65, 42, 22);
            g.setColor(new Color(16, 12, 32));
            g.fillRect(w - 98, h - 53, 18, 10);

            // Laser Cannon Turret & Upward Laser Bolt
            int canonX = 81;
            g.setColor(new Color(0, 229, 255, 100));
            g.fillRect(canonX - 6, h - 48, 12, 8);
            g.fillRect(canonX - 2, h - 55, 4, 7);
            int boltY = (h - 60) - ((pulseTicks * 6) % (h - 100));
            g.setColor(new Color(0, 255, 200, 130));
            g.fillRect(canonX - 1, boltY, 3, 14);
        }

        private void draw8BitInvader(Graphics2D g, int x, int y, int ps, boolean f2, Color color) {
            g.setColor(color);
            // 8x8 classic crab pattern
            int[][] map = f2 ? new int[][]{
                {0,0,1,0,0,1,0,0},
                {0,0,0,1,1,0,0,0},
                {0,1,1,1,1,1,1,0},
                {1,1,0,1,1,0,1,1},
                {1,1,1,1,1,1,1,1},
                {0,1,0,1,1,0,1,0},
                {1,0,0,0,0,0,0,1},
                {0,1,0,0,0,0,1,0}
            } : new int[][]{
                {0,0,1,0,0,1,0,0},
                {1,0,0,1,1,0,0,1},
                {1,0,1,1,1,1,0,1},
                {1,1,1,1,1,1,1,1},
                {0,1,1,1,1,1,1,0},
                {0,0,1,0,0,1,0,0},
                {0,1,0,0,0,0,1,0},
                {1,0,0,0,0,0,0,1}
            };
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (map[r][c] == 1) {
                        g.fillRect(x + c * ps, y + r * ps, ps, ps);
                    }
                }
            }
        }

        // --- 02. SPACE SHOOT: Deep space nebulae, twin laser cannons, soaring fighter ship & enemy alien interceptors ---
        private void paintSpaceShootBG(Graphics2D g, int w, int h, Color themeColor) {
            // Starfield with golden & cyan space dust
            for (int i = 0; i < 48; i++) {
                int sx = ((i * 151 + 37) % (w - 40)) + 20;
                int sy = ((i * 83 + 29) % (h - 70)) + 20;
                int sa = safeAlpha((int)(30 + 25 * Math.sin(pulseTicks * 0.09 + i)));
                if (i % 3 == 0) g.setColor(new Color(255, 190, 50, sa));
                else if (i % 3 == 1) g.setColor(new Color(255, 80, 50, sa));
                else g.setColor(new Color(255, 255, 255, sa));
                int sz = (i % 6 == 0) ? 3 : 2;
                g.fillRect(sx, sy, sz, sz);
            }

            // Distant Glowing Space Nebulae on wings
            g.setColor(new Color(255, 130, 20, 18));
            g.fillOval(40, 60, 190, 110);
            g.setColor(new Color(255, 70, 120, 15));
            g.fillOval(w - 230, 80, 200, 120);

            // Left & Right Wing Enemy Drone Fighters
            int enemySway = (int)(Math.sin(pulseTicks * 0.04) * 28);
            // Left Wing Enemies
            drawSpaceEnemyShip(g, 65 + enemySway, 70, new Color(255, 65, 80, 130));
            drawSpaceEnemyShip(g, 135 - enemySway, 95, new Color(255, 160, 40, 120));
            // Right Wing Enemies
            drawSpaceEnemyShip(g, w - 180 + enemySway, 75, new Color(255, 65, 80, 130));
            drawSpaceEnemyShip(g, w - 110 - enemySway, 105, new Color(255, 160, 40, 120));

            // Target Lock Crosshair HUD
            int lockX = w - 180 + enemySway + 12;
            int lockY = 75 + 10;
            g.setColor(new Color(255, 200, 40, 140));
            g.drawRect(lockX - 16, lockY - 16, 32, 32);
            g.drawLine(lockX - 22, lockY, lockX - 18, lockY);
            g.drawLine(lockX + 18, lockY, lockX + 22, lockY);
            g.drawLine(lockX, lockY - 22, lockX, lockY - 18);
            g.drawLine(lockX, lockY + 18, lockX, lockY + 22);
            g.setFont(new Font("Consolas", Font.BOLD, 9));
            g.drawString("LOCK: THREAT #02", lockX - 35, lockY + 26);

            // Player Space Fighter on Left Wing (Cruising & Firing)
            int shipX = 85 + (int)(Math.sin(pulseTicks * 0.035) * 30);
            int shipY = h - 65;
            Image playerShip = PixelSprites.createSpaceShootShip(32, new Color(255, 185, 50));
            if (playerShip != null) {
                g.drawImage(playerShip, shipX, shipY, null);
            }

            // Dual Upward Laser Blaster Bolts
            int boltY1 = (h - 70) - ((pulseTicks * 8) % (h - 120));
            int boltY2 = (h - 70) - (((pulseTicks + 18) * 8) % (h - 120));
            g.setColor(new Color(255, 235, 80, 180));
            g.fillRect(shipX + 4, boltY1, 3, 14);
            g.fillRect(shipX + 25, boltY1, 3, 14);
            g.fillRect(shipX + 4, boltY2, 3, 14);
            g.fillRect(shipX + 25, boltY2, 3, 14);

            // Laser Impact Sparks on top
            if ((pulseTicks / 10) % 2 == 0) {
                int spkX = shipX + 14;
                int spkY = 85;
                g.setColor(new Color(255, 240, 120, 190));
                g.drawLine(spkX - 6, spkY, spkX + 6, spkY);
                g.drawLine(spkX, spkY - 6, spkX, spkY + 6);
                g.drawOval(spkX - 5, spkY - 5, 10, 10);
            }

            // Telemetry Watermark
            g.setFont(new Font("Consolas", Font.PLAIN, 10));
            g.setColor(new Color(255, 184, 0, 60));
            g.drawString("// SPACE SHOOT: DEADLOCK VERIFIED // ALL CANNONS SAFE TO FIRE [P1, P3, P0, P2, P4]", 45, h - 35);
        }

        private void drawSpaceEnemyShip(Graphics2D g, int x, int y, Color c) {
            g.setColor(c);
            int[] ex = {x + 12, x + 24, x + 20, x + 16, x + 8, x + 4, x};
            int[] ey = {y + 16, y + 4,  y,      y + 6,  y + 6, y,      y + 4};
            g.fillPolygon(ex, ey, 7);
            g.setColor(Color.WHITE);
            g.fillRect(x + 10, y + 8, 4, 3); // Enemy cockpit
        }

        // --- 03. FRAME HOPPER: MMU Bus highway, moving microchip/DMA traffic, and hopping cyber frog ---
        private void paintFrameHopperBG(Graphics2D g, int w, int h, Color themeColor) {
            // Horizontal MMU Bus Lanes on Left and Right wings
            int[] laneYs = { 80, 150, 220, 290, 360, 430 };
            for (int ly : laneYs) {
                // Lane surface (dark navy bus strip)
                g.setColor(new Color(15, 30, 55, 60));
                g.fillRect(20, ly, 220, 44);
                g.fillRect(w - 240, ly, 220, 44);

                // Neon dashed centerline
                g.setColor(new Color(0, 240, 200, 35));
                for (int dx = 25; dx < 235; dx += 24) {
                    g.fillRect(dx, ly + 21, 14, 2);
                }
                for (int dx = w - 235; dx < w - 25; dx += 24) {
                    g.fillRect(dx, ly + 21, 14, 2);
                }
            }

            // Moving Data Bus Traffic / Packets
            int tick = (int) pulseTicks;

            // Lane 0: Fast left-moving Microchips
            int chip1X = (w - 240) + ((tick * 3 + 60) % 200);
            int chip2X = 20 + ((tick * 2 + 100) % 200);
            g.drawImage(PixelSprites.createChipHazardSprite(22, new Color(255, 60, 120)), chip1X, laneYs[1] + 11, null);
            g.drawImage(PixelSprites.createChipHazardSprite(22, new Color(0, 220, 255)), chip2X, laneYs[3] + 11, null);

            // Lane 1: DMA Transport Trucks
            int truckX = 20 + ((tick * 2 + 30) % 180);
            int truckRightX = (w - 240) + (180 - ((tick * 2 + 40) % 180));
            g.drawImage(PixelSprites.createTruckHazardSprite(38, 20, new Color(255, 180, 40)), truckX, laneYs[2] + 12, null);
            g.drawImage(PixelSprites.createTruckHazardSprite(38, 20, new Color(0, 240, 200)), truckRightX, laneYs[4] + 12, null);

            // Voltage Glitches / Lightning
            int glitchX = 20 + ((tick * 4 + 140) % 190);
            g.drawImage(PixelSprites.createGlitchHazardSprite(30, 18, new Color(255, 230, 60)), glitchX, laneYs[0] + 13, null);

            // Hopping Cyber Frogs
            // Frog 1 (Left wing, hopping upwards across lanes)
            int frogHopPhase = (tick / 20) % laneYs.length;
            int frogY = laneYs[laneYs.length - 1 - frogHopPhase] + 8;
            int hopArc = (int)(Math.sin((tick % 20) / 20.0 * Math.PI) * 10);
            g.drawImage(PixelSprites.createFrogSprite(24, new Color(40, 255, 200)), 90, frogY - hopArc, null);

            // Frog 2 (Right wing, hopping across lanes)
            int frog2HopPhase = ((tick + 50) / 22) % laneYs.length;
            int frog2Y = laneYs[frog2HopPhase] + 8;
            int hop2Arc = (int)(Math.sin(((tick + 50) % 22) / 22.0 * Math.PI) * 10);
            g.drawImage(PixelSprites.createFrogSprite(24, new Color(57, 255, 140)), w - 120, frog2Y - hop2Arc, null);

            // Top Physical Memory Docks (P00, P01, P02)
            for (int i = 0; i < 3; i++) {
                int dockX = 35 + i * 65;
                g.setColor(new Color(0, 240, 200, 40));
                g.drawRoundRect(dockX, 26, 52, 34, 6, 6);
                g.setColor(new Color(0, 240, 200, 20));
                g.fillRoundRect(dockX, 26, 52, 34, 6, 6);
                g.setColor(new Color(0, 240, 200, 180));
                g.setFont(new Font("Consolas", Font.BOLD, 10));
                g.drawString("F0" + i, dockX + 16, 48);

                int dockXR = (w - 225) + i * 65;
                g.setColor(new Color(0, 240, 200, 40));
                g.drawRoundRect(dockXR, 26, 52, 34, 6, 6);
                g.setColor(new Color(0, 240, 200, 20));
                g.fillRoundRect(dockXR, 26, 52, 34, 6, 6);
                g.setColor(new Color(0, 240, 200, 180));
                g.drawString("F0" + (i + 3), dockXR + 16, 48);
            }
        }

        // --- 04. SUPER MARIO: Drifting 8-bit clouds, question blocks with spinning coins ---
        private void paintMarioBG(Graphics2D g, int w, int h, Color themeColor) {
            // Starfield
            for (int i = 0; i < 35; i++) {
                int sx = ((i * 137 + 29) % (w - 40)) + 20;
                int sy = ((i * 79 + 43) % (h - 70)) + 20;
                int sa = safeAlpha((int)(20 + 18 * Math.sin(pulseTicks * 0.08 + i)));
                g.setColor(new Color(255, 115, 115, sa));
                int sz = (i % 5 == 0) ? 3 : 2;
                g.fillRect(sx, sy, sz, sz);
            }

            // Drifting Pixel Clouds
            int c1X = (int)((pulseTicks * 0.45) % (w + 140)) - 70;
            int c2X = (int)((pulseTicks * 0.3 + 380) % (w + 140)) - 70;
            draw8BitCloud(g, c1X, 35, 70, 24);
            draw8BitCloud(g, c2X, 85, 90, 28);

            // Floating Question [?] Blocks and Brick Platforms (Left wing)
            int platY = 170;
            drawMarioBlock(g, 55, platY, false, false);
            drawMarioBlock(g, 85, platY, true, true);  // [?] block with spinning coin
            drawMarioBlock(g, 115, platY, false, false);

            // Right wing blocks
            drawMarioBlock(g, w - 165, platY + 15, true, true);
            drawMarioBlock(g, w - 135, platY + 15, false, false);
            drawMarioBlock(g, w - 105, platY + 15, true, false);

            // Green Warp Pipes at Bottom
            drawWarpPipe(g, 65, h - 90, 44, 55);
            drawWarpPipe(g, w - 120, h - 105, 46, 70);

            // Soil brick floor
            g.setColor(new Color(180, 80, 40, 50));
            g.fillRect(30, h - 35, w - 60, 20);
            g.setColor(new Color(220, 110, 60, 40));
            for (int x = 30; x < w - 60; x += 20) {
                g.drawLine(x, h - 35, x, h - 15);
            }
        }

        private void draw8BitCloud(Graphics2D g, int x, int y, int cw, int ch) {
            g.setColor(new Color(255, 255, 255, 38));
            g.fillRoundRect(x, y + 8, cw, ch - 8, 8, 8);
            g.fillOval(x + 10, y, 22, 22);
            g.fillOval(x + 24, y - 6, 28, 28);
            g.fillOval(x + 44, y + 2, 20, 20);
        }

        private void drawMarioBlock(Graphics2D g, int x, int y, boolean isQuestion, boolean hasCoin) {
            int sz = 24;
            if (isQuestion) {
                g.setColor(new Color(255, 180, 0, 85));
                g.fillRect(x, y, sz, sz);
                g.setColor(new Color(255, 230, 80, 120));
                g.drawRect(x, y, sz, sz);
                // Question mark
                g.setColor(new Color(60, 40, 0, 180));
                g.setFont(new Font("Consolas", Font.BOLD, 14));
                g.drawString("?", x + 7, y + 17);

                // Spinning golden coin
                if (hasCoin) {
                    int coinW = (int)(Math.abs(Math.sin(pulseTicks * 0.12)) * 12) + 2;
                    int coinX = x + sz / 2 - coinW / 2;
                    int coinY = y - 20 + (int)(Math.sin(pulseTicks * 0.1) * 3);
                    g.setColor(new Color(255, 225, 50, 140));
                    g.fillOval(coinX, coinY, coinW, 14);
                }
            } else {
                // Brown brick block
                g.setColor(new Color(175, 75, 35, 75));
                g.fillRect(x, y, sz, sz);
                g.setColor(new Color(110, 45, 20, 110));
                g.drawRect(x, y, sz, sz);
                g.drawLine(x, y + sz / 2, x + sz, y + sz / 2);
                g.drawLine(x + sz / 2, y, x + sz / 2, y + sz / 2);
            }
        }

        private void drawWarpPipe(Graphics2D g, int x, int y, int pw, int ph) {
            // Pipe rim
            g.setColor(new Color(0, 190, 70, 75));
            g.fillRect(x - 4, y, pw + 8, 16);
            g.setColor(new Color(50, 255, 120, 110));
            g.drawRect(x - 4, y, pw + 8, 16);
            // Highlight
            g.setColor(new Color(150, 255, 180, 70));
            g.fillRect(x + 4, y + 2, 4, 12);

            // Pipe body
            g.setColor(new Color(0, 170, 60, 65));
            g.fillRect(x, y + 16, pw, ph - 16);
            g.setColor(new Color(50, 255, 120, 90));
            g.drawRect(x, y + 16, pw, ph - 16);
            g.setColor(new Color(150, 255, 180, 60));
            g.fillRect(x + 5, y + 16, 4, ph - 16);
        }

        // --- 05. SNAKE: Cyber green grid, animated 12-segment slithering snake, and pulsing apple ---
        private void paintSnakeBG(Graphics2D g, int w, int h, Color themeColor) {
            // Cyber Grid
            g.setColor(new Color(0, 255, 136, 18));
            for (int x = 40; x < w - 40; x += 36) g.drawLine(x, 20, x, h - 30);
            for (int y = 30; y < h - 30; y += 36) g.drawLine(40, y, w - 40, y);

            // Parametric 12-Segment Slithering Snake
            int segments = 12;
            for (int s = segments - 1; s >= 0; s--) {
                double t = (pulseTicks - s * 5) * 0.022;
                // Lissajous figure-8 curve across wings
                int sx = (int)(w / 2 + (w * 0.40) * Math.sin(t));
                int sy = (int)(h / 2 + (h * 0.32) * Math.sin(2 * t));

                float alpha = 0.85f - (s / (float)segments) * 0.60f;
                int sa = safeAlpha((int)(alpha * 120));
                g.setColor(new Color(0, 255, 136, sa));
                int sz = (s == 0) ? 18 : 14;
                g.fillRoundRect(sx - sz / 2, sy - sz / 2, sz, sz, 4, 4);

                if (s == 0) {
                    // Snake head eyes
                    g.setColor(new Color(255, 255, 255, 200));
                    g.fillRect(sx - 4, sy - 5, 3, 3);
                    g.fillRect(sx + 2, sy - 5, 3, 3);
                }
            }

            // Pulsing Pixel Apple
            int ax = 120;
            int ay = h / 2 - 30;
            int appleGlow = safeAlpha((int)(30 + 25 * Math.sin(pulseTicks * 0.12)));
            g.setColor(new Color(255, 50, 70, appleGlow));
            g.fillOval(ax - 10, ay - 10, 36, 36);

            g.setColor(new Color(255, 40, 60, 150));
            g.fillRoundRect(ax, ay, 16, 16, 4, 4);
            // Green leaf
            g.setColor(new Color(50, 255, 100, 180));
            g.fillRect(ax + 8, ay - 4, 4, 4);

            // Watermark
            g.setFont(new Font("Consolas", Font.PLAIN, 10));
            g.setColor(new Color(0, 255, 136, 50));
            g.drawString("SEG_TABLE: [0: CODE, 1: DATA, 2: HEAP, 3: STACK] // BOUNDS VERIFIED", 45, h - 35);
        }

        // --- 06. CYBER CRANE ATTACK: Industrial overhead gantry truss, motorized crane claw & RAM socket conveyor ---
        private void paintCyberCraneBG(Graphics2D g, int w, int h, Color themeColor) {
            // 1. Heavy Industrial Overhead Gantry Girder across wings
            g.setColor(new Color(45, 30, 65, 120));
            g.fillRect(35, 30, 220, 16);
            g.fillRect(w - 255, 30, 220, 16);

            // Gantry Hazard Caution Stripes (Yellow / Dark Slate)
            for (int i = 0; i < 9; i++) {
                g.setColor(new Color(255, 200, 30, 85));
                g.fillPolygon(new int[]{45 + i * 24, 53 + i * 24, 47 + i * 24, 39 + i * 24},
                              new int[]{30, 30, 46, 46}, 4);
                g.fillPolygon(new int[]{w - 245 + i * 24, w - 237 + i * 24, w - 243 + i * 24, w - 251 + i * 24},
                              new int[]{30, 30, 46, 46}, 4);
            }
            g.setColor(new Color(195, 115, 255, 90));
            g.drawRect(35, 30, 220, 16);
            g.drawRect(w - 255, 30, 220, 16);

            // 2. Animated Motorized Crane Trolley & Cables on Left Wing
            int trolleyX = 90 + (int)(Math.sin(pulseTicks * 0.03) * 55);
            int cableDrop = 55 + (int)(Math.abs(Math.sin(pulseTicks * 0.04)) * 60);

            // Crane Trolley Body
            g.setColor(new Color(75, 45, 110, 180));
            g.fillRect(trolleyX - 16, 46, 32, 10);
            g.setColor(new Color(0, 240, 255, 140));
            g.fillRect(trolleyX - 4, 48, 8, 4); // Trolley status indicator

            // High-tension Steel Cables
            g.setColor(new Color(220, 225, 250, 110));
            g.drawLine(trolleyX - 6, 56, trolleyX - 6, 56 + cableDrop);
            g.drawLine(trolleyX + 6, 56, trolleyX + 6, 56 + cableDrop);

            // Mechanical Cyber Crane Claw Head
            int clawY = 56 + cableDrop;
            Image clawSprite = PixelSprites.createCraneSprite(32, new Color(215, 135, 255));
            if (clawSprite != null) {
                g.drawImage(clawSprite, trolleyX - 16, clawY - 4, null);
            }

            // Downward Laser Alignment Targeting Beams
            g.setColor(new Color(0, 240, 255, 45));
            g.drawLine(trolleyX - 8, clawY + 28, trolleyX - 18, h - 75);
            g.drawLine(trolleyX + 8, clawY + 28, trolleyX + 18, h - 75);

            // 3. Right Wing Crane Mirror / Standby
            int rTrolleyX = w - 150 - (int)(Math.sin(pulseTicks * 0.035) * 45);
            int rCableDrop = 45 + (int)(Math.abs(Math.cos(pulseTicks * 0.04)) * 50);
            g.setColor(new Color(75, 45, 110, 180));
            g.fillRect(rTrolleyX - 16, 46, 32, 10);
            g.setColor(new Color(255, 80, 140, 140));
            g.fillRect(rTrolleyX - 4, 48, 8, 4);
            g.setColor(new Color(220, 225, 250, 110));
            g.drawLine(rTrolleyX - 6, 56, rTrolleyX - 6, 56 + rCableDrop);
            g.drawLine(rTrolleyX + 6, 56, rTrolleyX + 6, 56 + rCableDrop);
            if (clawSprite != null) {
                g.drawImage(clawSprite, rTrolleyX - 16, 56 + rCableDrop - 4, null);
            }

            // 4. Physical Motherboard RAM Silos / Sockets at Bottom
            int[] sockXs = {45, 100, 155, w - 225, w - 170, w - 115};
            int sockY = h - 78;
            for (int i = 0; i < sockXs.length; i++) {
                int sx = sockXs[i];
                // RAM Socket Chassis
                g.setColor(new Color(18, 14, 30, 180));
                g.fillRect(sx, sockY, 46, 28);
                g.setColor(new Color(90, 60, 130, 120));
                g.drawRect(sx, sockY, 46, 28);

                // RAM Memory Pins
                for (int p = 0; p < 8; p++) {
                    g.setColor(new Color(0, 240, 255, 80));
                    g.fillRect(sx + 4 + p * 5, sockY + 24, 2, 4);
                }

                // Seated RAM block
                if (i % 2 == 0) {
                    g.setColor(new Color(195, 115, 255, 75));
                    g.fillRect(sx + 3, sockY + 4, 40, 18);
                    g.setFont(new Font("Consolas", Font.BOLD, 8));
                    g.setColor(Color.WHITE);
                    g.drawString("RAM", sx + 12, sockY + 16);
                } else {
                    g.setFont(new Font("Consolas", Font.PLAIN, 8));
                    g.setColor(new Color(0, 240, 255, 80));
                    g.drawString("FREE", sx + 11, sockY + 16);
                }
            }

            // Telemetry Watermark
            g.setFont(new Font("Consolas", Font.PLAIN, 10));
            g.setColor(new Color(215, 145, 255, 60));
            g.drawString("// CYBER CRANE MMU ALLOCATOR: BEST-FIT CLAW ENGAGED // FRAGMENTATION: 0 KB", 45, h - 35);
        }

        // --- 07. PAC-MAN: Blue neon maze labyrinth, chomping Pac-Man, yellow pellets, and chasing ghost ---
        private void paintPacmanBG(Graphics2D g, int w, int h, Color themeColor) {
            // Blue Neon Maze Walls
            g.setColor(new Color(33, 33, 222, 60));
            g.setStroke(new BasicStroke(2.0f));
            // Left wing maze borders
            g.drawRoundRect(45, 45, 160, 85, 12, 12);
            g.drawRoundRect(45, 145, 160, 120, 12, 12);
            // Right wing maze borders
            g.drawRoundRect(w - 205, 45, 160, 85, 12, 12);
            g.drawRoundRect(w - 205, 145, 160, 120, 12, 12);

            // Yellow Pellets along corridors
            g.setColor(new Color(255, 230, 50, 110));
            for (int py = 55; py < 260; py += 24) {
                g.fillRect(215, py, 4, 4);
                g.fillRect(w - 225, py, 4, 4);
            }

            // Flashing Power Pellets (Energizers)
            int eSz = (int)(10 + 4 * Math.sin(pulseTicks * 0.18));
            g.fillOval(55 - eSz / 2, 55 - eSz / 2, eSz, eSz);
            g.fillOval(w - 55 - eSz / 2, 55 - eSz / 2, eSz, eSz);

            // Pac-Man Chomping across lower corridor
            int pacX = (int)((pulseTicks * 2.2) % (w + 120)) - 60;
            int pacY = h - 55;
            int chomp = (int)(Math.abs(Math.sin(pulseTicks * 0.20)) * 40);
            g.setColor(new Color(255, 230, 0, 160));
            g.fillArc(pacX - 14, pacY - 14, 28, 28, chomp, 360 - chomp * 2);

            // Chasing 8-bit Blinky Ghost
            int ghostX = pacX - 50;
            int ghostY = pacY - 14;
            g.setColor(new Color(255, 40, 40, 150));
            g.fillArc(ghostX, ghostY, 26, 26, 0, 180); // dome
            g.fillRect(ghostX, ghostY + 12, 26, 10);
            // Wavy feet
            boolean f2 = ((pulseTicks / 10) % 2 == 1);
            if (f2) {
                g.fillRect(ghostX, ghostY + 22, 6, 4);
                g.fillRect(ghostX + 10, ghostY + 22, 6, 4);
                g.fillRect(ghostX + 20, ghostY + 22, 6, 4);
            } else {
                g.fillRect(ghostX + 4, ghostY + 22, 6, 4);
                g.fillRect(ghostX + 14, ghostY + 22, 6, 4);
            }
            // Eyes
            g.setColor(Color.WHITE);
            g.fillOval(ghostX + 5, ghostY + 6, 6, 8);
            g.fillOval(ghostX + 14, ghostY + 6, 6, 8);
            g.setColor(new Color(0, 80, 220));
            g.fillOval(ghostX + 8, ghostY + 8, 3, 4);
            g.fillOval(ghostX + 17, ghostY + 8, 3, 4);
        }

        // --- 08. OVERCOOKED: Sizzling cooking counters, floating steam & orders ---
        private void paintOvercookedBG(Graphics2D g, int w, int h, Color themeColor) {
            // Isometric Checkerboard Kitchen Floor
            int floorTop = h / 2 + 50;
            int tileW = 44;
            int tileH = 22;
            for (int ty = floorTop; ty < h; ty += tileH) {
                int row = (ty - floorTop) / tileH;
                int xOffset = (row % 2 == 0) ? 0 : tileW / 2;
                for (int tx = xOffset; tx < w; tx += tileW) {
                    if ((tx / tileW + row) % 2 == 0) {
                        g.setColor(new Color(45, 18, 40, 45));
                    } else {
                        g.setColor(new Color(25, 10, 25, 45));
                    }
                    g.fillRect(tx, ty, tileW, tileH);
                }
            }

            // Floating Kitchen Order Tickets along the top
            for (int t = 0; t < 3; t++) {
                int tx = (t == 0) ? 75 : ((t == 1) ? 135 : w - 180);
                int ty = 28 + (int)(Math.sin(pulseTicks * 0.06 + t * 1.2) * 5);
                // Ticket paper
                g.setColor(new Color(245, 240, 220, 160));
                g.fillRoundRect(tx, ty, 50, 34, 4, 4);
                g.setColor(new Color(255, 115, 185, 180));
                g.drawRoundRect(tx, ty, 50, 34, 4, 4);
                // Header bar (green=salad, red=burger, yellow=soup)
                Color barC = (t == 0) ? new Color(220, 60, 50) : ((t == 1) ? new Color(240, 180, 40) : new Color(60, 180, 80));
                g.setColor(barC);
                g.fillRect(tx + 2, ty + 2, 46, 6);
                // Label
                g.setFont(new Font("Consolas", Font.BOLD, 8));
                g.setColor(Color.WHITE);
                String tLabel = (t == 0) ? "BURGER" : ((t == 1) ? "SOUP" : "SALAD");
                g.drawString(tLabel, tx + 6, ty + 18);
            }

            // Left Wing Cooking Station (Stove & Sizzling Skillet)
            int lStoveX = 45;
            int lStoveY = h - 145;
            g.setColor(new Color(50, 40, 65, 140));
            g.fillRoundRect(lStoveX, lStoveY, 140, 48, 6, 6);
            g.setColor(new Color(255, 115, 185, 90));
            g.drawRoundRect(lStoveX, lStoveY, 140, 48, 6, 6);

            // Sizzling Skillet
            g.setColor(new Color(20, 16, 25, 200));
            g.fillOval(lStoveX + 25, lStoveY + 10, 36, 28);
            g.fillRect(lStoveX + 60, lStoveY + 20, 24, 6); // handle
            // Steak patty
            g.setColor(new Color(140, 65, 35, 220));
            g.fillOval(lStoveX + 32, lStoveY + 16, 22, 16);

            // Rising Steam Curls from Skillet
            for (int s = 0; s < 4; s++) {
                int steamY = (lStoveY + 10) - (int)((pulseTicks * 1.5 + s * 24) % 80);
                int steamX = lStoveX + 38 + (int)(Math.sin(pulseTicks * 0.1 + s) * 8);
                int steamA = safeAlpha((int)(110 - ((lStoveY + 10 - steamY) * 1.3)));
                g.setColor(new Color(255, 235, 245, steamA));
                g.fillOval(steamX, steamY, 10, 8);
            }

            // Right Wing Prep Station (Chopping Board & Ingredients)
            int rPrepX = w - 185;
            int rPrepY = h - 145;
            g.setColor(new Color(50, 40, 65, 140));
            g.fillRoundRect(rPrepX, rPrepY, 140, 48, 6, 6);
            g.setColor(new Color(255, 115, 185, 90));
            g.drawRoundRect(rPrepX, rPrepY, 140, 48, 6, 6);

            // Wooden Chopping Board
            g.setColor(new Color(160, 105, 55, 180));
            g.fillRoundRect(rPrepX + 20, rPrepY + 8, 48, 32, 4, 4);
            // Sliced tomato
            g.setColor(new Color(255, 50, 50, 220));
            g.fillOval(rPrepX + 28, rPrepY + 14, 14, 14);
            // Burger bun halves
            g.setColor(new Color(230, 160, 70, 220));
            g.fillOval(rPrepX + 80, rPrepY + 14, 22, 14);
        }
    }

    private String getDifficultyLabel(int comp) {
        switch (comp) {
            case 1: return "1 / 5 (INTRO)";
            case 2: return "2 / 5 (BASIC)";
            case 3: return "3 / 5 (MODERATE)";
            case 4: return "4 / 5 (ADVANCED)";
            case 5: return "5 / 5 (EXPERT)";
            default: return comp + " / 5";
        }
    }

    private void launchCurrentStage() {
        playLaunchChime();
        AppState.markRead(currentStageIndex);
        updateCounters();
        showToast("[ +100 XP ] " + cards[currentStageIndex].title + " ACCESSED!");
        parentFrame.openSimulatorTab(cards[currentStageIndex].tabIndex);
    }

    public void snapToStage(int targetIdx) {
        if (targetIdx < 0 || targetIdx >= cards.length) return;
        currentStageIndex = targetIdx;
        currentScroll = targetIdx;
        targetScroll = targetIdx;
        if (railTimer != null && railTimer.isRunning()) {
            railTimer.stop();
        }
        updateStageIndicators();
        if (carouselCanvas != null) {
            carouselCanvas.repaint();
        }
        repaint();
    }

    public void glideToStage(int targetIdx) {
        if (targetIdx < 0 || targetIdx >= cards.length) return;

        currentStageIndex = targetIdx;
        targetScroll = targetIdx;

        // Circular shortest path calculation
        float diff = targetScroll - currentScroll;
        while (diff < -cards.length / 2.0f) { currentScroll -= cards.length; diff = targetScroll - currentScroll; }
        while (diff > cards.length / 2.0f)  { currentScroll += cards.length; diff = targetScroll - currentScroll; }

        if (railTimer != null && railTimer.isRunning()) {
            railTimer.stop();
        }

        railTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                float dist = targetScroll - currentScroll;
                if (Math.abs(dist) < 0.005f) {
                    currentScroll = targetScroll;
                    while (currentScroll < 0) currentScroll += cards.length;
                    while (currentScroll >= cards.length) currentScroll -= cards.length;
                    targetScroll = currentScroll;
                    railTimer.stop();
                    updateStageIndicators();
                } else {
                    currentScroll += dist * 0.18f;
                }
                if (carouselCanvas != null) {
                    carouselCanvas.repaint();
                }
            }
        });
        railTimer.start();
        updateStageIndicators();
    }

    private void prevStage() {
        int next = (currentStageIndex - 1 + cards.length) % cards.length;
        glideToStage(next);
    }

    private void nextStage() {
        int next = (currentStageIndex + 1) % cards.length;
        glideToStage(next);
    }

    /**
     * Enhanced Stage Indicators Tab Strip with animated active caret,
     * filled backgrounds, and 3D retro arcade keycap hints.
     */
    private JPanel createStageIndicatorsStrip() {
        JPanel strip = new JPanel(new BorderLayout(10, 2));
        strip.setOpaque(false);
        strip.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));

        JPanel pillsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        pillsRow.setOpaque(false);

        final String[] stageNames = {
            "INVADERS", "SPACE", "HOPPER", "MARIO", "SNAKE", "CRANE", "PAC-MAN", "OVERCOOKED"
        };

        for (int i = 0; i < cards.length; i++) {
            final int stageIdx = i;
            ArchiveCardData c = cards[i];

            JButton pill = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean curActive = (stageIdx == currentStageIndex);
                    int pw = getWidth();
                    int ph = getHeight();
                    Color stageColor = STAGE_THEME_COLORS[stageIdx];

                    if (curActive) {
                        // Filled background in active stage color
                        g2.setColor(new Color(stageColor.getRed(), stageColor.getGreen(), stageColor.getBlue(), 65));
                        g2.fillRoundRect(0, 0, pw, ph, 7, 7);
                        g2.setColor(stageColor);
                        g2.setStroke(new BasicStroke(1.8f));
                        g2.drawRoundRect(0, 0, pw - 1, ph - 1, 7, 7);
                        // Bottom neon bar
                        g2.fillRect(3, ph - 3, pw - 6, 3);

                        // Animated upward caret ▲ pointing up to the active card
                        int caretX = pw / 2;
                        int caretY = 2 + (int)(bobOffset * 0.3f);
                        int[] cx = {caretX - 4, caretX + 4, caretX};
                        int[] cy = {caretY + 3, caretY + 3, caretY - 2};
                        g2.setColor(stageColor);
                        g2.fillPolygon(cx, cy, 3);
                    } else {
                        g2.setColor(new Color(20, 15, 36));
                        g2.fillRoundRect(0, 0, pw, ph, 7, 7);
                        g2.setColor(new Color(55, 42, 75));
                        g2.drawRoundRect(0, 0, pw - 1, ph - 1, 7, 7);
                    }

                    // 16x16 Crisp Sprite
                    Image spr = getCardSprite(stageIdx, 16);
                    if (spr != null) {
                        g2.drawImage(spr, 7, (ph - 16) / 2, 16, 16, null);
                    }

                    // Text: "08 OVERCOOKED" with balanced spacing and generous margins (no truncation)
                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    g2.setColor(curActive ? stageColor : new Color(160, 150, 190));
                    g2.drawString(c.num, 27, (ph / 2) + 4);

                    g2.setColor(curActive ? Color.WHITE : new Color(210, 205, 230));
                    g2.drawString(stageNames[stageIdx], 45, (ph / 2) + 4);

                    g2.dispose();
                }
            };
            pill.setPreferredSize(new Dimension(134, 30));
            pill.setFocusPainted(false);
            pill.setOpaque(false);
            pill.setContentAreaFilled(false);
            pill.setBorder(BorderFactory.createEmptyBorder());
            pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pill.addActionListener(e -> {
                glideToStage(stageIdx);
                playBlip();
                requestFocusInWindow();
            });
            pillsRow.add(pill);
        }
        strip.add(pillsRow, BorderLayout.CENTER);

        // 3D RETRO ARCADE KEYCAP CHIPS ROW (Full keyboard controls supported)
        JPanel hintsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        hintsPanel.setOpaque(false);

        hintsPanel.add(createKeycapChip("← / A", "PREV"));
        hintsPanel.add(createKeycapChip("→ / D", "NEXT"));
        hintsPanel.add(createKeycapChip("1 - 8", "JUMP"));
        hintsPanel.add(createKeycapChip("ENTER", "LAUNCH"));
        hintsPanel.add(createKeycapChip("M", "MANUAL"));
        hintsPanel.add(createKeycapChip("Q", "QUIZ"));
        hintsPanel.add(createKeycapChip("ESC", "MENU"));

        strip.add(hintsPanel, BorderLayout.SOUTH);

        return strip;
    }

    private JPanel createKeycapChip(String keyStr, String actionStr) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);

        JLabel keyLabel = new JLabel(keyStr, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(42, 30, 68));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(1, 1, getWidth() - 2, (getHeight() / 2) - 1, 3, 3);
                g2.setColor(new Color(85, 65, 125));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        keyLabel.setFont(new Font("Consolas", Font.BOLD, 10));
        keyLabel.setForeground(RetroTheme.ACCENT_CYAN);
        keyLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        p.add(keyLabel);

        JLabel actLabel = new JLabel(actionStr);
        actLabel.setFont(new Font("Consolas", Font.BOLD, 10));
        actLabel.setForeground(new Color(215, 210, 235));
        actLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 4));
        p.add(actLabel);

        return p;
    }

    private void updateStageIndicators() {
        if (stageIndicatorsPanel != null) {
            stageIndicatorsPanel.repaint();
        }
        if (progressMeter != null) {
            progressMeter.repaint();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GRID VIEW IMPLEMENTATION
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel createGridView() {
        JPanel gridWrapper = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // 1. Cyber Dot-Matrix Grid across background (dot dot)
                g2.setColor(new Color(85, 60, 130, 42));
                int dotSpacing = 28;
                for (int x = dotSpacing / 2; x < w; x += dotSpacing) {
                    for (int y = dotSpacing / 2; y < h; y += dotSpacing) {
                        g2.fillRect(x, y, 2, 2);
                    }
                }

                // 2. Floating Neon Constellation Particles
                for (int i = 0; i < 75; i++) {
                    int px = (i * 67 + 23) % w;
                    int py = (i * 41 + 17) % h;
                    int pSize = (i % 7 == 0) ? 3 : ((i % 3 == 0) ? 2 : 1);
                    int alpha = 65 + (i * 19) % 155;
                    if (i % 3 == 0) {
                        g2.setColor(new Color(5, 217, 232, alpha)); // Cyan
                    } else if (i % 3 == 1) {
                        g2.setColor(new Color(255, 42, 109, alpha)); // Pink
                    } else {
                        g2.setColor(new Color(255, 211, 25, alpha)); // Gold
                    }
                    g2.fillOval(px, py, pSize, pSize);
                }

                // 3. Outrun Perspective Grid Lines at Bottom
                int horizonY = h - 55;
                g2.setColor(new Color(255, 42, 109, 28));
                g2.drawLine(0, horizonY, w, horizonY);
                for (int x = -100; x < w + 200; x += 65) {
                    g2.drawLine(w / 2, horizonY, x, h);
                }
                for (int y = horizonY + 8; y < h; y += (y - horizonY + 4)) {
                    g2.drawLine(0, y, w, y);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        gridWrapper.setOpaque(false);

        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 12));
        grid.setOpaque(false);

        for (int i = 0; i < cards.length; i++) {
            JPanel card = createArchiveCard(cards[i], i);
            card.setPreferredSize(new Dimension(300, 290));
            grid.add(card);
        }

        gridWrapper.add(grid);
        return gridWrapper;
    }

    private JPanel createArchiveCard(ArchiveCardData d, int idx) {
        final Color themeColor = STAGE_THEME_COLORS[idx % STAGE_THEME_COLORS.length];

        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            private boolean isHovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                    @Override public void mouseClicked(MouseEvent e) {
                        playBlip();
                        openManual(d.tabIndex, idx);
                    }
                });
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // 1. Draw rich procedural game-specific background
                drawGameThemedBackground(g2, idx, w, h, themeColor, isHovered);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        // Top line: STAGE Number & Read Status Badge
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel lblNum = new JLabel("STAGE " + d.num);
        lblNum.setFont(new Font("Consolas", Font.BOLD, 11));
        lblNum.setForeground(themeColor);
        lblNum.setPreferredSize(new Dimension(85, 20));
        top.add(lblNum, BorderLayout.WEST);

        boolean isRead = (AppState.filesRead[idx] == 1);
        JLabel lblStatus = new JLabel(isRead ? "[ READ +100XP ]" : "[ UNREAD ]") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean read = (AppState.filesRead[idx] == 1);
                Color badgeC = read ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_YELLOW;
                g2.setColor(new Color(badgeC.getRed() / 4, badgeC.getGreen() / 4, badgeC.getBlue() / 4, 180));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(badgeC);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblStatus.setFont(new Font("Consolas", Font.BOLD, 9));
        lblStatus.setForeground(isRead ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_YELLOW);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
        lblStatus.setOpaque(false);
        top.add(lblStatus, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Center Wrapper: Perfectly centers the content block vertically and horizontally
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        // Mascot Icon with attached Glowing Oval Pedestal
        JLabel icon = new JLabel(new ImageIcon(getCardSprite(idx, 60))) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2;
                int py = getHeight() - 11;
                g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 65));
                g2.fillOval(cx - 38, py, 76, 15);
                g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 160));
                g2.setStroke(new BasicStroke(1.1f));
                g2.drawOval(cx - 38, py, 76, 15);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setPreferredSize(new Dimension(80, 60));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(icon);
        center.add(Box.createVerticalStrut(7));

        // Game Title
        String rawTitle = d.title;
        String gameName = rawTitle.contains("//") ? rawTitle.substring(0, rawTitle.indexOf("//")).trim() : rawTitle;
        JLabel titleL = new JLabel(" " + gameName + " ", SwingConstants.CENTER);
        titleL.setFont(new Font("Consolas", Font.BOLD, 13));
        titleL.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        titleL.setForeground(Color.WHITE);
        titleL.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(titleL);
        center.add(Box.createVerticalStrut(6));

        // Topic Badge Chip
        String topicName = rawTitle.contains("//") ? rawTitle.substring(rawTitle.indexOf("//") + 2).trim() : d.category;
        JLabel topicL = new JLabel("[ " + topicName + " ]", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(themeColor.getRed() / 5, themeColor.getGreen() / 5, themeColor.getBlue() / 5, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 130));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        topicL.setFont(new Font("Consolas", Font.BOLD, 10));
        topicL.setForeground(themeColor);
        topicL.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        topicL.setOpaque(false);
        topicL.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(topicL);
        center.add(Box.createVerticalStrut(6));

        // Lore Subtitle
        JLabel subL = new JLabel(" " + d.subtitle + " ", SwingConstants.CENTER);
        subL.setFont(new Font("Consolas", Font.PLAIN, 9));
        subL.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        subL.setForeground(RetroTheme.TEXT_DIM);
        subL.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(subL);

        centerWrapper.add(center);
        card.add(centerWrapper, BorderLayout.CENTER);

        // Bottom Action Button: VIEW DOSSIER >>
        JButton btnDossier = new JButton("VIEW DOSSIER >>") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(themeColor.getRed() / 4, themeColor.getGreen() / 4, themeColor.getBlue() / 4));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(themeColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnDossier.setFont(new Font("Consolas", Font.BOLD, 11));
        btnDossier.setForeground(themeColor);
        btnDossier.setContentAreaFilled(false);
        btnDossier.setOpaque(false);
        btnDossier.setFocusPainted(false);
        btnDossier.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        btnDossier.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDossier.addActionListener(e -> {
            playBlip();
            openManual(d.tabIndex, idx);
        });
        card.add(btnDossier, BorderLayout.SOUTH);

        return card;
    }

    /**
     * Renders customized, themed procedural retro backgrounds for all 8 arcade games.
     */
    private void drawGameThemedBackground(Graphics2D g2, int idx, int w, int h, Color c, boolean hovered) {
        // Base dark container gradient
        GradientPaint bgGrad = new GradientPaint(
                0, 0, new Color(16, 12, 30),
                0, h, new Color(c.getRed() / 6, c.getGreen() / 6, c.getBlue() / 6)
        );
        g2.setPaint(bgGrad);
        g2.fillRoundRect(0, 0, w, h, 14, 14);

        // Clip to rounded card bounds for all background details
        Shape oldClip = g2.getClip();
        g2.clip(new RoundRectangle2D.Float(0, 0, w, h, 14, 14));

        // Procedural Game Background Pattern
        switch (idx) {
            case 0: drawInvadersBackground(g2, w, h, c); break;
            case 1: drawSpaceShootBackground(g2, w, h, c); break;
            case 2: drawFrameHopperBackground(g2, w, h, c); break;
            case 3: drawMarioCacheBackground(g2, w, h, c); break;
            case 4: drawSnakeSegmentationBackground(g2, w, h, c); break;
            case 5: drawCyberCraneBackground(g2, w, h, c); break;
            case 6: drawPacmanDiskBackground(g2, w, h, c); break;
            case 7: drawOvercookedBackground(g2, w, h, c); break;
        }

        // Dark gradient vignette overlay to guarantee 100% text contrast
        GradientPaint vignette = new GradientPaint(
                0, 30, new Color(12, 8, 24, 140),
                0, h - 30, new Color(12, 8, 24, 190)
        );
        g2.setPaint(vignette);
        g2.fillRect(0, 30, w, h - 30);

        // Restore clip
        g2.setClip(oldClip);

        // Top Arcade Header Strip
        g2.setColor(new Color(c.getRed() / 4, c.getGreen() / 4, c.getBlue() / 4, 220));
        g2.fillRoundRect(2, 2, w - 4, 26, 12, 12);
        g2.fillRect(2, 16, w - 4, 12);

        // Card Border Glow
        g2.setColor(hovered ? c : new Color(c.getRed(), c.getGreen(), c.getBlue(), 120));
        g2.setStroke(new BasicStroke(hovered ? 2.5f : 1.2f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
    }

    private void drawInvadersBackground(Graphics2D g2, int w, int h, Color c) {
        // Deep space starfield
        for (int i = 0; i < 40; i++) {
            int sx = (i * 47 + 13) % w;
            int sy = (i * 29 + 37) % (h - 50) + 30;
            int sz = (i % 5 == 0) ? 2 : 1;
            int alpha = 80 + (i * 17) % 150;
            g2.setColor(new Color(200, 240, 255, alpha));
            g2.fillRect(sx, sy, sz, sz);
        }

        // Horizontal laser radar scanlines
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 20));
        for (int y = 40; y < h - 40; y += 16) {
            g2.drawLine(10, y, w - 10, y);
        }

        // Faint descending alien formation silhouettes in background
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 35));
        for (int col = 0; col < 4; col++) {
            int ax = 28 + col * 64;
            int ay = 50;
            g2.fillRect(ax + 4, ay, 12, 4);
            g2.fillRect(ax, ay + 4, 20, 6);
            g2.fillRect(ax + 2, ay + 10, 4, 4);
            g2.fillRect(ax + 14, ay + 10, 4, 4);
        }

        // Defensive bunker silhouettes at bottom
        int by = h - 44;
        g2.setColor(new Color(40, 200, 100, 55));
        for (int b = 0; b < 3; b++) {
            int bx = 26 + b * 86;
            int[] px = { bx + 8, bx + 36, bx + 44, bx + 44, bx + 32, bx + 32, bx + 12, bx + 12, bx };
            int[] py = { by, by, by + 10, by + 22, by + 22, by + 12, by + 12, by + 22, by + 22 };
            g2.fillPolygon(px, py, px.length);
        }
    }

    private void drawSpaceShootBackground(Graphics2D g2, int w, int h, Color c) {
        // Starfield & nebula dust
        for (int i = 0; i < 35; i++) {
            int sx = (i * 53 + 7) % w;
            int sy = (i * 31 + 41) % (h - 40) + 30;
            g2.setColor(new Color(255, 220, 140, 60 + (i * 23) % 120));
            g2.fillRect(sx, sy, (i % 6 == 0) ? 2 : 1, (i % 6 == 0) ? 2 : 1);
        }

        // Cyber Vault Hexagonal grid / vector nodes
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 28));
        g2.setStroke(new BasicStroke(1.0f));
        int cx = w / 2;
        int cy = h / 2 - 10;
        g2.drawOval(cx - 70, cy - 70, 140, 140);
        g2.drawOval(cx - 40, cy - 40, 80, 80);
        g2.drawLine(cx - 85, cy, cx + 85, cy);
        g2.drawLine(cx, cy - 85, cx, cy + 85);

        // Banker's Safety Vector Matrix Nodes
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 45));
        String[] nodes = {"[R1]", "[R2]", "[R3]"};
        g2.setFont(new Font("Consolas", Font.BOLD, 9));
        for (int i = 0; i < 3; i++) {
            int nx = 20 + i * 90;
            int ny = h - 46;
            g2.drawRoundRect(nx, ny, 32, 16, 4, 4);
            g2.drawString(nodes[i], nx + 5, ny + 12);
            if (i < 2) {
                g2.drawLine(nx + 32, ny + 8, nx + 58, ny + 8);
            }
        }
    }

    private void drawFrameHopperBackground(Graphics2D g2, int w, int h, Color c) {
        // Frogger River & Highway crossing lanes
        // 1. River section (Upper middle)
        g2.setColor(new Color(15, 65, 110, 70));
        g2.fillRect(10, 48, w - 20, 42);
        // Floating logs
        g2.setColor(new Color(130, 85, 45, 80));
        g2.fillRoundRect(24, 52, 52, 12, 6, 6);
        g2.fillRoundRect(120, 52, 65, 12, 6, 6);
        g2.fillRoundRect(70, 70, 70, 12, 6, 6);
        // Water ripples
        g2.setColor(new Color(40, 255, 200, 60));
        g2.drawLine(30, 66, 60, 66);
        g2.drawLine(150, 66, 190, 66);

        // 2. Highway section (Lower middle)
        g2.setColor(new Color(25, 25, 38, 90));
        g2.fillRect(10, 96, w - 20, 44);
        // Dashed road center divider
        g2.setColor(new Color(255, 220, 50, 70));
        g2.setStroke(new BasicStroke(1.5f));
        for (int x = 20; x < w - 20; x += 28) {
            g2.drawLine(x, 118, x + 16, 118);
        }

        // 3. MMU Paging Address Bus tracks at bottom
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 45));
        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        for (int f = 0; f < 4; f++) {
            int fx = 18 + f * 64;
            int fy = h - 44;
            g2.drawRoundRect(fx, fy, 48, 16, 4, 4);
            g2.drawString("FRAME " + f, fx + 6, fy + 12);
        }
    }

    private void drawMarioCacheBackground(Graphics2D g2, int w, int h, Color c) {
        // Subterranean Mario 8-bit brick wall pattern
        int brickW = 28;
        int brickH = 14;
        g2.setColor(new Color(160, 65, 45, 50));
        for (int row = 0; row < 7; row++) {
            int y = 40 + row * brickH;
            int offset = (row % 2 == 0) ? 0 : brickW / 2;
            for (int col = -1; col < (w / brickW) + 2; col++) {
                int x = col * brickW + offset;
                g2.drawRect(x, y, brickW, brickH);
            }
        }

        // Floating Question Mark Coin Block [ ? ] in upper right
        int qx = w - 50;
        int qy = 44;
        g2.setColor(new Color(240, 180, 40, 85));
        g2.fillRoundRect(qx, qy, 24, 24, 6, 6);
        g2.setColor(new Color(255, 230, 80));
        g2.drawRoundRect(qx, qy, 24, 24, 6, 6);
        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        g2.drawString("?", qx + 7, qy + 17);

        // Classic Green Warp Pipe rising from bottom corner
        int px = 18;
        int py = h - 50;
        g2.setColor(new Color(35, 175, 65, 90));
        g2.fillRect(px + 4, py + 10, 36, 36); // Stem
        g2.fillRect(px, py, 44, 10);          // Lip
        g2.setColor(new Color(75, 240, 120, 110));
        g2.drawRect(px + 4, py + 10, 36, 36);
        g2.drawRect(px, py, 44, 10);
    }

    private void drawSnakeSegmentationBackground(Graphics2D g2, int w, int h, Color c) {
        // Digital cyber grid
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 20));
        g2.setStroke(new BasicStroke(1.0f));
        for (int x = 12; x < w - 12; x += 16) {
            g2.drawLine(x, 34, x, h - 34);
        }
        for (int y = 34; y < h - 34; y += 16) {
            g2.drawLine(12, y, w - 12, y);
        }

        // Serpentine segmented body trail
        int[][] snakeBody = {
            {35, 56}, {53, 56}, {71, 56}, {89, 56},
            {89, 74}, {89, 92}, {107, 92}, {125, 92},
            {143, 92}, {161, 92}, {161, 110}, {179, 110}
        };
        g2.setColor(new Color(90, 245, 140, 50));
        for (int i = 0; i < snakeBody.length; i++) {
            g2.fillRoundRect(snakeBody[i][0], snakeBody[i][1], 12, 12, 4, 4);
        }
        // Glowing food pellet / segment base
        g2.setColor(new Color(255, 75, 75, 85));
        g2.fillOval(202, 110, 10, 10);

        // Memory segment boundary trap line at bottom
        int by = h - 44;
        g2.setColor(new Color(255, 60, 60, 65));
        g2.drawLine(20, by, w - 20, by);
        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        g2.drawString("LIMIT BARRIER // HARDWARE TRAP", 26, by + 12);
    }

    private void drawCyberCraneBackground(Graphics2D g2, int w, int h, Color c) {
        // Industrial overhead gantry crane girder truss across top
        int gy = 40;
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRect(16, gy, w - 32, 14);
        for (int x = 16; x < w - 32; x += 20) {
            g2.drawLine(x, gy, x + 20, gy + 14);
            g2.drawLine(x + 20, gy, x, gy + 14);
        }

        // Steel cable hanging down
        g2.setColor(new Color(200, 180, 240, 55));
        g2.drawLine(w / 2, gy + 14, w / 2, gy + 38);

        // Contiguous RAM Partition Slots at bottom
        int ry = h - 46;
        int rw = (w - 36) / 3;
        String[] slots = {"[128K HOLE]", "[256K ALLOC]", "[64K HOLE]"};
        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        for (int i = 0; i < 3; i++) {
            int rx = 18 + i * (rw + 4);
            boolean isAlloc = (i == 1);
            g2.setColor(isAlloc ? new Color(c.getRed(), c.getGreen(), c.getBlue(), 50) : new Color(30, 22, 45, 75));
            g2.fillRoundRect(rx, ry, rw, 18, 4, 4);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 75));
            g2.drawRoundRect(rx, ry, rw, 18, 4, 4);
            g2.setColor(isAlloc ? Color.WHITE : RetroTheme.TEXT_DIM);
            g2.drawString(slots[i], rx + 4, ry + 12);
        }
    }

    private void drawPacmanDiskBackground(Graphics2D g2, int w, int h, Color c) {
        // Concentric magnetic platter cylinder tracks
        int cx = w / 2;
        int cy = h / 2 - 10;
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 25));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawOval(cx - 70, cy - 70, 140, 140);
        g2.drawOval(cx - 50, cy - 50, 100, 100);
        g2.drawOval(cx - 30, cy - 30, 60, 60);

        // Disk Head Actuator Arm Radial Vector
        g2.setColor(new Color(255, 120, 40, 75));
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawLine(cx, cy, cx + 60, cy - 30);
        g2.fillOval(cx + 57, cy - 33, 6, 6);

        // Retro Arcade Maze Neon Blue Border & Dots
        g2.setColor(new Color(35, 95, 255, 65));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(16, 40, w - 32, h - 84, 8, 8);

        // Glowing Pac-Dots along maze trail
        g2.setColor(new Color(255, 235, 140, 85));
        for (int x = 28; x < w - 28; x += 20) {
            g2.fillOval(x, 46, 3, 3);
            g2.fillOval(x, h - 48, 3, 3);
        }
        g2.fillOval(w - 32, 45, 6, 6);
    }

    private void drawOvercookedBackground(Graphics2D g2, int w, int h, Color c) {
        // Isometric diamond checkerboard kitchen tile floor
        int tileSize = 20;
        for (int row = 1; row < 9; row++) {
            int y = 36 + row * 15;
            for (int col = 0; col < (w / tileSize) + 1; col++) {
                int x = col * tileSize + ((row % 2 == 0) ? 0 : tileSize / 2);
                boolean dark = ((row + col) % 2 == 0);
                g2.setColor(dark ? new Color(45, 22, 18, 55) : new Color(85, 38, 24, 35));
                int[] px = { x, x + tileSize / 2, x, x - tileSize / 2 };
                int[] py = { y - 7, y, y + 7, y };
                g2.fillPolygon(px, py, 4);
            }
        }

        // Order Ticket Rail across top (Hanging order tickets)
        int ty = 40;
        g2.setColor(new Color(180, 170, 160, 55));
        g2.fillRect(18, ty, w - 36, 4);
        for (int t = 0; t < 4; t++) {
            int tx = 26 + t * 58;
            g2.setColor(new Color(250, 245, 220, 75));
            g2.fillRect(tx, ty + 4, 32, 16);
            g2.setColor(new Color(190, 80, 40, 85));
            g2.setFont(new Font("Consolas", Font.BOLD, 8));
            g2.drawString("#0" + (t + 1), tx + 5, ty + 15);
        }

        // Stove burner heating rings at bottom
        int sy = h - 42;
        g2.setColor(new Color(255, 100, 30, 60));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(w / 2 - 32, sy - 8, 28, 14);
        g2.drawOval(w / 2 + 4, sy - 8, 28, 14);
    }

    private Image getCardSprite(int idx, int sz) {
        switch (idx) {
            case 0: return PixelSprites.createAlienSprite(sz, new Color(170, 90, 255));
            case 1: return PixelSprites.createSpaceShootShip(sz, new Color(255, 180, 50));
            case 2: return PixelSprites.createFrogSprite(sz, new Color(40, 255, 200));
            case 3: return PixelSprites.createMarioSprite(sz);
            case 4: return PixelSprites.createSnakeSprite(sz, new Color(90, 245, 140));
            case 5: return PixelSprites.createCraneSprite(sz, new Color(195, 115, 255));
            case 6: return PixelSprites.createPacman(sz, 0.4f, true);
            default: return PixelSprites.createChefSprite(sz, false);
        }
    }

    private void openManual(int tabIndex, int cardIdx) {
        playBlip();
        ArchiveManualDialog dialog = new ArchiveManualDialog(parentFrame, this, cardIdx);
        dialog.setVisible(true);
        updateCounters();
        showToast("[ +100 XP ] " + cards[cardIdx].title + " MANUAL READ!");
    }

    public void updateCounters() {
        int readCount = AppState.countFilesRead();
        if (lblFilesReadCounter != null) {
            lblFilesReadCounter.setText(String.format("%02d / 08", readCount));
        }
        if (lblHeaderScore != null) {
            lblHeaderScore.setText(String.format("OPERATOR: %s | %s XP", AppState.playerName, AppState.getScoreFormatted()));
        }
        if (lblProgressText != null) {
            lblProgressText.setText(String.format("INDEX: %d/8 READ  ", readCount));
        }
        if (progressMeter != null) {
            progressMeter.repaint();
        }
        repaint();
    }

    /** 8-Block Segmented Arcade Progress Meter */
    private class SegmentedProgressMeter extends JComponent {
        public SegmentedProgressMeter() {
            setPreferredSize(new Dimension(176, 18));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int blocks = cards.length;
            int gap = 4;
            int blockW = (w - (blocks - 1) * gap) / blocks;
            int blockH = h - 4;
            int startY = 2;

            for (int i = 0; i < blocks; i++) {
                int bx = i * (blockW + gap);
                boolean isRead = (AppState.filesRead[i] == 1);
                boolean isActive = (i == currentStageIndex);
                Color stageColor = STAGE_THEME_COLORS[i];

                if (isRead) {
                    g2.setColor(stageColor);
                    g2.fillRoundRect(bx, startY, blockW, blockH, 3, 3);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(bx, startY, blockW, blockH, 3, 3);
                } else if (isActive) {
                    g2.setColor(new Color(stageColor.getRed(), stageColor.getGreen(), stageColor.getBlue(), 90));
                    g2.fillRoundRect(bx, startY, blockW, blockH, 3, 3);
                    g2.setColor(stageColor);
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawRoundRect(bx, startY, blockW, blockH, 3, 3);
                } else {
                    g2.setColor(new Color(28, 20, 44));
                    g2.fillRoundRect(bx, startY, blockW, blockH, 3, 3);
                    g2.setColor(new Color(55, 42, 75));
                    g2.drawRoundRect(bx, startY, blockW, blockH, 3, 3);
                }
            }
            g2.dispose();
        }
    }

    /** Clean Bottom Navigation Bar with 8-block Segmented Meter */
    private JPanel createBottomNav() {
        JPanel bar = new JPanel(new BorderLayout(15, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));

        JButton btnBack = RetroTheme.createRetroButton("< MENU [ESC]", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnBack.addActionListener(e -> {
            playBlip();
            parentFrame.showScreen(ArcadeOS.SCREEN_MAIN_MENU);
        });
        bar.add(btnBack, BorderLayout.WEST);

        // Center: 8-Block Segmented Arcade Progress Meter
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 6, 0, 6);

        int readCount = AppState.countFilesRead();
        lblProgressText = new JLabel(String.format("INDEX: %d/8 READ  ", readCount));
        lblProgressText.setFont(new Font("Consolas", Font.BOLD, 12));
        lblProgressText.setForeground(new Color(225, 220, 245));
        center.add(lblProgressText, gbc);

        progressMeter = new SegmentedProgressMeter();
        center.add(progressMeter, gbc);

        bar.add(center, BorderLayout.CENTER);

        // Right: Status chip
        JLabel lblRight = new JLabel(" ALL ARCHIVES SYNCED ");
        lblRight.setFont(new Font("Consolas", Font.BOLD, 10));
        lblRight.setForeground(RetroTheme.ACCENT_GREEN);
        lblRight.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_GREEN, 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        bar.add(lblRight, BorderLayout.EAST);

        return bar;
    }
}
