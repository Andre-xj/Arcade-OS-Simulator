package arcadeos;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen: QUIZ DIFFICULTY SELECTION // ARCADE SECURITY CLEARANCE GATE
 * -----------------------------------------------------------------------------
 * Ultra-Polished 8-Bit Retro Arcade Edition:
 * - Perfectly Aligned, Authentic Retro Arcade Cabinets (Recruit, Veteran, Master)
 * - Custom-Drawn Glowing LED Segment Meters (Zero Alignment or Font Tofu Glitches)
 * - Centered Pixel Art CRT Monitor Portraits
 * - 3D Tactile Arcade Push Buttons with Click Feedback
 * - Hotkeys: [1/E] Easy, [2/A] Average, [3/X] Expert, [ESC] Return
 */
public class QuizDifficultyPanel extends JPanel {
    private final ArcadeOS parentFrame;
    private boolean crtEnabled = false;
    private boolean soundEnabled = true;
    private int targetMissionIdx = 0;

    // Ambient Cyber Animation
    private final List<CyberStar> stars = new ArrayList<>();
    private float pulsePhase = 0.0f;
    private boolean pulseDir = true;
    private javax.swing.Timer animTimer;

    // Header Badges
    private JLabel lblXpScore;
    private JButton btnCrtToggle;
    private JButton btnSfxToggle;
    private JLabel lblTargetMissionSub;

    private static class CyberStar {
        float x, y, vy, size, alpha;
        Color color;
    }

    private static class StatGauge {
        final String label;
        final int activeSegments; // 1 to 5
        final String valueText;

        StatGauge(String l, int s, String v) {
            this.label = l;
            this.activeSegments = s;
            this.valueText = v;
        }
    }

    public QuizDifficultyPanel(ArcadeOS parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout());
        setBackground(RetroTheme.BG_DARK);

        // Spawn ambient arcade stars
        Color[] starColors = {
                new Color(5, 217, 232), new Color(255, 42, 141),
                new Color(57, 255, 60),  new Color(255, 211, 25), Color.WHITE
        };
        for (int i = 0; i < 50; i++) {
            CyberStar s = new CyberStar();
            s.x = (float) (Math.random() * 1400);
            s.y = (float) (Math.random() * 900);
            s.vy = (float) (-0.25 - Math.random() * 0.45);
            s.size = (float) (1.2 + Math.random() * 2.2);
            s.alpha = (float) (0.25 + Math.random() * 0.65);
            s.color = starColors[(int) (Math.random() * starColors.length)];
            stars.add(s);
        }

        // Animation Loop
        animTimer = new javax.swing.Timer(33, e -> {
            if (pulseDir) {
                pulsePhase += 0.04f;
                if (pulsePhase >= 1.0f) pulseDir = false;
            } else {
                pulsePhase -= 0.04f;
                if (pulsePhase <= 0.0f) pulseDir = true;
            }

            for (CyberStar s : stars) {
                s.y += s.vy;
                if (s.y < 0) {
                    s.y = getHeight() > 0 ? getHeight() : 860;
                    s.x = (float) (Math.random() * (getWidth() > 0 ? getWidth() : 1280));
                }
            }
            repaint();
        });
        animTimer.start();

        // 1. Top Bar
        add(createTopBar(), BorderLayout.NORTH);

        // 2. Centered Arcade Deck
        add(createCenteredArcadeDeck(), BorderLayout.CENTER);

        // 3. Bottom Console Strip
        add(createBottomConsole(), BorderLayout.SOUTH);

        // Keyboard Controls
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_1 || code == KeyEvent.VK_NUMPAD1 || code == KeyEvent.VK_E) {
                    selectDifficulty(QuizManager.Difficulty.EASY);
                } else if (code == KeyEvent.VK_2 || code == KeyEvent.VK_NUMPAD2 || code == KeyEvent.VK_A) {
                    selectDifficulty(QuizManager.Difficulty.AVERAGE);
                } else if (code == KeyEvent.VK_3 || code == KeyEvent.VK_NUMPAD3 || code == KeyEvent.VK_X) {
                    selectDifficulty(QuizManager.Difficulty.EXPERT);
                } else if (code == KeyEvent.VK_ESCAPE) {
                    returnBack();
                } else if (code == KeyEvent.VK_C) {
                    toggleCrt();
                } else if (code == KeyEvent.VK_S) {
                    toggleSfx();
                }
            }
        });

        addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override public void ancestorAdded(javax.swing.event.AncestorEvent e) { onShow(); }
            @Override public void ancestorRemoved(javax.swing.event.AncestorEvent e) {}
            @Override public void ancestorMoved(javax.swing.event.AncestorEvent e) {}
        });
    }

    public void setTargetMission(int missionIdx) {
        if (missionIdx < 0 || missionIdx >= 8) missionIdx = 0;
        this.targetMissionIdx = missionIdx;
        updateTargetMissionUI();
    }

    private void updateTargetMissionUI() {
        if (lblTargetMissionSub != null) {
            String game = QuizStageSelectPanel.MOD_GAMES[targetMissionIdx];
            String algo = QuizStageSelectPanel.MOD_FULL_NAMES[targetMissionIdx];
            lblTargetMissionSub.setText(String.format("TARGET: MISSION %02d // %s (%s)", targetMissionIdx + 1, game, algo));
        }
    }

    public void onShow() {
        if (lblXpScore != null) {
            lblXpScore.setText("XP: " + AppState.getScoreFormatted());
        }
        updateTargetMissionUI();
        playTone(520, 20);
        requestFocusInWindow();
    }

    private void selectDifficulty(QuizManager.Difficulty diff) {
        if (soundEnabled) {
            playTone(480, 20);
            playTone(660, 30);
            playTone(920, 45);
        }
        if (parentFrame != null) {
            parentFrame.startQuizWithMissionAndDifficulty(targetMissionIdx, diff);
        }
    }

    private void returnBack() {
        playTone(380, 25);
        if (parentFrame != null) {
            parentFrame.showScreen(ArcadeOS.SCREEN_QUIZ_STAGE_SELECT);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. TOP HEADER BAR
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 18, 6, 18));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JButton btnBack = RetroTheme.createRetroButton("< STAGE SELECT [ESC]", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnBack.setFont(new Font("Consolas", Font.BOLD, 11));
        btnBack.setFocusable(false);
        btnBack.addActionListener(e -> returnBack());
        left.add(btnBack);

        JLabel lblGate = new JLabel(":: SECURITY CLEARANCE GATE ::");
        lblGate.setFont(new Font("Consolas", Font.BOLD, 12));
        lblGate.setForeground(new Color(230, 240, 255));
        left.add(lblGate);

        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        lblXpScore = new JLabel("XP: " + AppState.getScoreFormatted());
        lblXpScore.setFont(new Font("Consolas", Font.BOLD, 11));
        lblXpScore.setForeground(RetroTheme.ACCENT_YELLOW);
        lblXpScore.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        right.add(lblXpScore);

        btnCrtToggle = RetroTheme.createRetroButton(crtEnabled ? "CRT: ON" : "CRT: OFF", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnCrtToggle.setFont(new Font("Consolas", Font.BOLD, 10));
        btnCrtToggle.setFocusable(false);
        btnCrtToggle.addActionListener(e -> toggleCrt());
        right.add(btnCrtToggle);

        btnSfxToggle = RetroTheme.createRetroButton(soundEnabled ? "SFX: ON" : "SFX: OFF", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnSfxToggle.setFont(new Font("Consolas", Font.BOLD, 10));
        btnSfxToggle.setFocusable(false);
        btnSfxToggle.addActionListener(e -> toggleSfx());
        right.add(btnSfxToggle);

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void toggleCrt() {
        crtEnabled = !crtEnabled;
        btnCrtToggle.setText(crtEnabled ? "CRT: ON" : "CRT: OFF");
        btnCrtToggle.setForeground(crtEnabled ? RetroTheme.ACCENT_GREEN : RetroTheme.TEXT_DIM);
        repaint();
        requestFocusInWindow();
    }

    private void toggleSfx() {
        soundEnabled = !soundEnabled;
        btnSfxToggle.setText(soundEnabled ? "SFX: ON" : "SFX: OFF");
        btnSfxToggle.setForeground(soundEnabled ? RetroTheme.ACCENT_GREEN : RetroTheme.TEXT_DIM);
        if (soundEnabled) playTone(600, 30);
        requestFocusInWindow();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. CENTERED RETRO ARCADE DECK (3 PROPORTIONED CABINETS)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel createCenteredArcadeDeck() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);

        JPanel centerBox = new JPanel(new BorderLayout(0, 10));
        centerBox.setOpaque(false);
        centerBox.setPreferredSize(new Dimension(1160, 520));

        // Marquee Arcade Header Banner
        centerBox.add(createArcadeMarqueeHeader(), BorderLayout.NORTH);

        // 3 Distinct Side-by-Side Arcade Cabinets
        JPanel cabinetsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        cabinetsRow.setOpaque(false);

        // Cabinet 1: EASY [ RECRUIT TIER ]
        cabinetsRow.add(createArcadeCabinetCard(
                QuizManager.Difficulty.EASY,
                "EASY",
                "RECRUIT TIER",
                new Color(57, 255, 60),  // Emerald Acid Green
                new Color(12, 36, 18),
                "[1]",
                PixelSprites.createShieldSprite(42, new Color(57, 255, 60)),
                "+100 XP",
                "1.0X SCORE",
                new StatGauge[]{
                        new StatGauge("TABLE ROWS",  3, "3 - 4 ROWS"),
                        new StatGauge("CIPHER LOCK", 2, "1 BLANK/ROW"),
                        new StatGauge("ASSISTANCE",  4, "BASE CLUES"),
                        new StatGauge("HAZARD RISK", 1, "LOW RISK")
                },
                "DEPLOY RECRUIT [1 / E]"
        ));

        // Cabinet 2: AVERAGE [ VETERAN TIER ]
        cabinetsRow.add(createArcadeCabinetCard(
                QuizManager.Difficulty.AVERAGE,
                "AVERAGE",
                "VETERAN TIER",
                new Color(255, 211, 25), // Golden Amber
                new Color(42, 32, 10),
                "[2]",
                PixelSprites.createSpaceShootShip(42, new Color(255, 211, 25)),
                "+250 XP",
                "1.5X SCORE",
                new StatGauge[]{
                        new StatGauge("TABLE ROWS",  4, "5 - 6 ROWS"),
                        new StatGauge("CIPHER LOCK", 3, "2 BLANKS/ROW"),
                        new StatGauge("ASSISTANCE",  3, "STANDARD OS"),
                        new StatGauge("HAZARD RISK", 3, "MODERATE")
                },
                "DEPLOY VETERAN [2 / A]"
        ));

        // Cabinet 3: EXPERT [ CYBER MASTER TIER ]
        cabinetsRow.add(createArcadeCabinetCard(
                QuizManager.Difficulty.EXPERT,
                "EXPERT",
                "CYBER MASTER",
                new Color(255, 42, 141), // Hot Magenta / Crimson
                new Color(46, 12, 30),
                "[3]",
                PixelSprites.createAlienSprite(42, new Color(255, 42, 141)),
                "+500 XP",
                "2.5X SCORE",
                new StatGauge[]{
                        new StatGauge("TABLE ROWS",  5, "7 - 8 ROWS"),
                        new StatGauge("CIPHER LOCK", 5, "FULL MATRIX"),
                        new StatGauge("ASSISTANCE",  1, "STRICT TRAPS"),
                        new StatGauge("HAZARD RISK", 5, "NIGHTMARE")
                },
                "DEPLOY MASTER [3 / X]"
        ));

        centerBox.add(cabinetsRow, BorderLayout.CENTER);
        wrapper.add(centerBox);
        return wrapper;
    }

    /**
     * Glowing Arcade Marquee Header Banner (Zero Tofu Font Glyphs)
     */
    private JPanel createArcadeMarqueeHeader() {
        JPanel marquee = new JPanel(new GridLayout(2, 1, 0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Dark Metallic Frame
                g2.setColor(new Color(18, 12, 34, 245));
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                // Bevel Highlight
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillRoundRect(1, 1, w - 2, (h / 2) - 1, 7, 7);

                // Neon Cyber Border
                int glowAlpha = (int) (120 + 60 * pulsePhase);
                g2.setColor(new Color(5, 217, 232, glowAlpha));
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                // Circuit Corner Brackets
                int notch = 10;
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.drawLine(2, 2, 2 + notch, 2); g2.drawLine(2, 2, 2, 2 + notch);
                g2.drawLine(w - 3, 2, w - 3 - notch, 2); g2.drawLine(w - 3, 2, w - 3, 2 + notch);
                g2.drawLine(2, h - 3, 2 + notch, h - 3); g2.drawLine(2, h - 3, 2, h - 3 - notch);
                g2.drawLine(w - 3, h - 3, w - 3 - notch, h - 3); g2.drawLine(w - 3, h - 3, w - 3, h - 3 - notch);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        marquee.setOpaque(false);
        marquee.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        // Row 1: Target Mission (Left) + Credits (Right)
        JPanel row1 = new JPanel(new BorderLayout());
        row1.setOpaque(false);

        lblTargetMissionSub = new JLabel("TARGET: MISSION 01 SPACE INVADERS // CPU SCHEDULING (FCFS)");
        lblTargetMissionSub.setFont(new Font("Consolas", Font.BOLD, 11));
        lblTargetMissionSub.setForeground(RetroTheme.ACCENT_CYAN);
        row1.add(lblTargetMissionSub, BorderLayout.WEST);

        JLabel lCredits = new JLabel("CREDITS: [ FREE PLAY ]");
        lCredits.setFont(new Font("Consolas", Font.BOLD, 10));
        lCredits.setForeground(RetroTheme.ACCENT_GREEN);
        lCredits.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 120, 60), 1),
                BorderFactory.createEmptyBorder(1, 6, 1, 6)
        ));
        row1.add(lCredits, BorderLayout.EAST);
        marquee.add(row1);

        // Row 2: Choose Difficulty Title (Left) + Deploy Prompt (Right)
        JPanel row2 = new JPanel(new BorderLayout());
        row2.setOpaque(false);

        JLabel lTitle = new JLabel(">>> CHOOSE QUIZ SIMULATION DIFFICULTY <<<");
        lTitle.setFont(new Font("Consolas", Font.BOLD, 15));
        lTitle.setForeground(Color.WHITE);
        row2.add(lTitle, BorderLayout.WEST);

        JLabel lPrompt = new JLabel("► PRESS [1] [2] [3] TO DEPLOY ◄");
        lPrompt.setFont(new Font("Consolas", Font.BOLD, 10));
        lPrompt.setForeground(RetroTheme.ACCENT_YELLOW);
        row2.add(lPrompt, BorderLayout.EAST);
        marquee.add(row2);

        return marquee;
    }

    /**
     * Miniature Retro Arcade Cabinet Card (Tight, Well-Framed, Beautiful)
     */
    private JPanel createArcadeCabinetCard(
            QuizManager.Difficulty diff,
            String title,
            String subTitle,
            Color accentColor,
            Color bgTint,
            String slotNum,
            BufferedImage mascotSprite,
            String xpBonus,
            String multiplier,
            StatGauge[] gauges,
            String btnLabel
    ) {
        final boolean[] isHovered = { false };

        JPanel cabinet = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Cabinet Shell Gradient
                Color topC = isHovered[0]
                        ? new Color(bgTint.getRed() + 25, bgTint.getGreen() + 25, bgTint.getBlue() + 30, 250)
                        : new Color(18, 13, 30, 245);
                Color botC = isHovered[0]
                        ? new Color(bgTint.getRed() + 10, bgTint.getGreen() + 10, bgTint.getBlue() + 15, 250)
                        : new Color(10, 6, 18, 250);
                GradientPaint gp = new GradientPaint(0, 0, topC, 0, h, botC);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 12, 12);

                // Chrome Top Bevel Highlight
                g2.setColor(new Color(255, 255, 255, isHovered[0] ? 35 : 18));
                g2.fillRoundRect(1, 1, w - 2, 38, 11, 11);

                // Neon Cyber Border
                int bAlpha = isHovered[0] ? 255 : (int) (100 + 70 * pulsePhase);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), bAlpha));
                g2.setStroke(new BasicStroke(isHovered[0] ? 2.2f : 1.3f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

                // Corner Brackets
                int cl = 12;
                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawLine(2, 2, 2 + cl, 2); g2.drawLine(2, 2, 2, 2 + cl);
                g2.drawLine(w - 3, 2, w - 3 - cl, 2); g2.drawLine(w - 3, 2, w - 3, 2 + cl);
                g2.drawLine(2, h - 3, 2 + cl, h - 3); g2.drawLine(2, h - 3, 2, h - 3 - cl);
                g2.drawLine(w - 3, h - 3, w - 3 - cl, h - 3); g2.drawLine(w - 3, h - 3, w - 3, h - 3 - cl);

                // Rivet Studs
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 140));
                g2.fillRect(5, 5, 2, 2);
                g2.fillRect(w - 7, 5, 2, 2);
                g2.fillRect(5, h - 7, 2, 2);
                g2.fillRect(w - 7, h - 7, 2, 2);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        cabinet.setOpaque(false);
        cabinet.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        cabinet.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ── TOP: Marquee Header Plate ──
        JPanel topBox = new JPanel(new BorderLayout());
        topBox.setOpaque(false);

        JLabel lBadge = new JLabel(slotNum + " " + subTitle);
        lBadge.setFont(new Font("Consolas", Font.BOLD, 10));
        lBadge.setForeground(accentColor);
        topBox.add(lBadge, BorderLayout.CENTER);

        JLabel lMult = new JLabel(multiplier);
        lMult.setFont(new Font("Consolas", Font.BOLD, 9));
        lMult.setForeground(Color.WHITE);
        lMult.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 110), 1),
                BorderFactory.createEmptyBorder(1, 5, 1, 5)
        ));
        topBox.add(lMult, BorderLayout.EAST);
        cabinet.add(topBox, BorderLayout.NORTH);

        // ── CENTER BODY: Mascot Monitor + Title + LED Gauges ──
        JPanel midPanel = new JPanel();
        midPanel.setOpaque(false);
        midPanel.setLayout(new BoxLayout(midPanel, BoxLayout.Y_AXIS));

        midPanel.add(Box.createVerticalStrut(2));

        // Centered Pixel Mascot CRT Screen (Well-proportioned portrait frame)
        JPanel monitor = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int mw = getWidth();
                int mh = getHeight();

                // CRT Monitor Inset
                g2.setColor(new Color(6, 4, 12, 255));
                g2.fillRoundRect(0, 0, mw, mh, 8, 8);

                // Phosphor Radial Aura
                Color cGlow = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), isHovered[0] ? 55 : 25);
                RadialGradientPaint rgp = new RadialGradientPaint(
                        new Point2D.Float(mw / 2f, mh / 2f), Math.max(mw, mh) * 0.65f,
                        new float[]{ 0.0f, 1.0f },
                        new Color[]{ cGlow, new Color(0, 0, 0, 0) }
                );
                g2.setPaint(rgp);
                g2.fillRoundRect(0, 0, mw, mh, 8, 8);

                // Scanlines
                g2.setColor(new Color(0, 0, 0, 30));
                for (int y = 0; y < mh; y += 3) g2.drawLine(0, y, mw, y);

                // Inner Monitor Rim
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), isHovered[0] ? 200 : 100));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, mw - 1, mh - 1, 8, 8);

                // Mascot Icon Centered
                if (mascotSprite != null) {
                    int sx = (mw - mascotSprite.getWidth()) / 2;
                    int sy = (mh - mascotSprite.getHeight()) / 2;
                    g2.drawImage(mascotSprite, sx, sy, null);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        monitor.setOpaque(false);
        monitor.setPreferredSize(new Dimension(100, 64));
        monitor.setMaximumSize(new Dimension(100, 64));
        monitor.setAlignmentX(Component.CENTER_ALIGNMENT);
        midPanel.add(monitor);

        midPanel.add(Box.createVerticalStrut(4));

        // Bold Tier Title
        JLabel lTitle = new JLabel(title);
        lTitle.setFont(new Font("Consolas", Font.BOLD, 20));
        lTitle.setForeground(Color.WHITE);
        lTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        midPanel.add(lTitle);

        midPanel.add(Box.createVerticalStrut(1));

        // Target Reward Chip
        JLabel lReward = new JLabel("TARGET REWARD: " + xpBonus);
        lReward.setFont(new Font("Consolas", Font.BOLD, 11));
        lReward.setForeground(accentColor);
        lReward.setAlignmentX(Component.CENTER_ALIGNMENT);
        midPanel.add(lReward);

        midPanel.add(Box.createVerticalStrut(8));

        // Perfectly Aligned LED Segment Gauges Box
        JPanel gaugesBox = createLedGaugesBox(gauges, accentColor);
        gaugesBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        midPanel.add(gaugesBox);

        cabinet.add(midPanel, BorderLayout.CENTER);

        // ── BOTTOM: Tactile 3D Arcade Push Button ──
        JButton btnDeploy = createTactileArcadeButton(btnLabel, accentColor);
        btnDeploy.addActionListener(e -> selectDifficulty(diff));
        cabinet.add(btnDeploy, BorderLayout.SOUTH);

        // Hover & Click Handlers
        cabinet.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered[0] = true;
                if (soundEnabled) playTone(460, 15);
                cabinet.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered[0] = false;
                cabinet.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                selectDifficulty(diff);
            }
        });

        return cabinet;
    }

    /**
     * Custom-Drawn Perfectly Aligned LED Segment Gauges
     */
    private JPanel createLedGaugesBox(StatGauge[] gauges, Color accentColor) {
        JPanel box = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(12, 8, 22, 230));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(45, 32, 68));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        box.setLayout(new GridLayout(gauges.length, 1, 0, 5));

        for (StatGauge sg : gauges) {
            box.add(createSingleLedRow(sg, accentColor));
        }
        return box;
    }

    /**
     * Single Strictly-Aligned LED Row: Label (90px) | 5 LED Blocks (68px) | Value (80px)
     */
    private JPanel createSingleLedRow(StatGauge sg, Color accentColor) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);

        // 1. Left Label
        JLabel lblName = new JLabel(sg.label);
        lblName.setFont(new Font("Consolas", Font.BOLD, 10));
        lblName.setForeground(new Color(185, 195, 220));
        lblName.setPreferredSize(new Dimension(78, 16));
        row.add(lblName, BorderLayout.WEST);

        // 2. Center LED Segments (Drawn strictly with Graphics2D)
        JComponent ledBars = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int segW = 10;
                int segH = 8;
                int gap = 3;
                int startX = 0;
                int startY = (getHeight() - segH) / 2;

                for (int i = 0; i < 5; i++) {
                    int x = startX + i * (segW + gap);
                    boolean active = (i < sg.activeSegments);

                    if (active) {
                        g2.setColor(accentColor);
                        g2.fillRoundRect(x, startY, segW, segH, 2, 2);
                        g2.setColor(new Color(255, 255, 255, 120));
                        g2.fillRect(x + 1, startY, segW - 2, 2);
                    } else {
                        g2.setColor(new Color(28, 20, 42));
                        g2.fillRoundRect(x, startY, segW, segH, 2, 2);
                        g2.setColor(new Color(48, 35, 70));
                        g2.setStroke(new BasicStroke(0.8f));
                        g2.drawRoundRect(x, startY, segW - 1, segH - 1, 2, 2);
                    }
                }
                g2.dispose();
            }
        };
        ledBars.setPreferredSize(new Dimension(64, 16));
        row.add(ledBars, BorderLayout.CENTER);

        // 3. Right Value Text
        JLabel lblVal = new JLabel(sg.valueText, SwingConstants.RIGHT);
        lblVal.setFont(new Font("Consolas", Font.BOLD, 9));
        lblVal.setForeground(Color.WHITE);
        lblVal.setPreferredSize(new Dimension(96, 16));
        row.add(lblVal, BorderLayout.EAST);

        return row;
    }

    /**
     * Tactile 3D Arcade Push Button
     */
    private JButton createTactileArcadeButton(String text, Color baseColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int bh = getHeight();
                boolean hover = getModel().isRollover();
                boolean pressed = getModel().isPressed();

                int yOff = pressed ? 2 : 0;

                // 3D Drop Shadow
                if (!pressed) {
                    g2.setColor(new Color(4, 2, 8, 220));
                    g2.fillRoundRect(2, 4, w - 4, bh - 4, 8, 8);
                }

                // Base Gradient
                Color topC = hover ? baseColor.brighter() : baseColor;
                Color botC = hover ? baseColor : baseColor.darker();
                GradientPaint gp = new GradientPaint(0, yOff, topC, 0, bh - 4 + yOff, botC);
                g2.setPaint(gp);
                g2.fillRoundRect(1, yOff, w - 2, bh - 4, 8, 8);

                // Laser Highlight
                g2.setColor(new Color(255, 255, 255, hover ? 85 : 45));
                g2.fillRoundRect(2, yOff + 1, w - 4, (bh / 2) - 1, 7, 7);

                // Outer Rim
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(1, yOff, w - 3, bh - 5, 8, 8);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("Consolas", Font.BOLD, 11));
        boolean isPink = baseColor.getRed() > 200 && baseColor.getBlue() > 100;
        btn.setForeground(isPink ? Color.WHITE : new Color(10, 8, 20));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(0, 40));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. BOTTOM ARCADE STATUS & SHORTCUTS CONSOLE
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel createBottomConsole() {
        JPanel b = new JPanel(new BorderLayout());
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 24, 12, 24));

        JLabel lKeys = new JLabel("SHORTCUT KEYS: [1 / E] EASY RECRUIT    [2 / A] AVERAGE VETERAN    [3 / X] EXPERT MASTER    [ESC] RETURN");
        lKeys.setFont(new Font("Consolas", Font.BOLD, 10));
        lKeys.setForeground(new Color(180, 190, 220));
        b.add(lKeys, BorderLayout.WEST);

        JLabel lInfo = new JLabel(":: ARCADE SIMULATOR MATRIX // PROCEDURAL ENGINE ONLINE ::");
        lInfo.setFont(new Font("Consolas", Font.BOLD, 10));
        lInfo.setForeground(RetroTheme.ACCENT_CYAN);
        b.add(lInfo, BorderLayout.EAST);

        return b;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AMBIENT ARCADE PAINTING: STARS, GRID & CRT FILTER
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Radial Atmosphere
        Point2D center = new Point2D.Float(w * 0.50f, h * 0.45f);
        float radius = Math.max(w, h) * 0.70f;
        Color glowColor = new Color(55, 20, 85, (int) (40 * pulsePhase));
        Color fadeColor = new Color(RetroTheme.BG_DARK.getRed(), RetroTheme.BG_DARK.getGreen(), RetroTheme.BG_DARK.getBlue(), 0);

        RadialGradientPaint rgp = new RadialGradientPaint(center, radius, new float[]{ 0.0f, 1.0f }, new Color[]{ glowColor, fadeColor });
        g2.setPaint(rgp);
        g2.fillRect(0, 0, w, h);

        // Cyber Grid Lines
        g2.setColor(new Color(90, 60, 130, 18));
        g2.setStroke(new BasicStroke(1.0f));
        int gridSpacing = 44;
        for (int x = 0; x < w; x += gridSpacing) g2.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += gridSpacing) g2.drawLine(0, y, w, y);

        // Floating Arcade Stars
        for (CyberStar s : stars) {
            g2.setColor(new Color(s.color.getRed(), s.color.getGreen(), s.color.getBlue(), (int) (s.alpha * 200)));
            g2.fillRect((int) s.x, (int) s.y, (int) s.size, (int) s.size);
        }

        // CRT Raster Scanlines
        if (crtEnabled) {
            g2.setColor(new Color(0, 0, 0, 22));
            for (int sy = 0; sy < h; sy += 3) g2.drawLine(0, sy, w, sy);
            RadialGradientPaint vignette = new RadialGradientPaint(
                    new Point2D.Float(w / 2f, h / 2f), Math.max(w, h) * 0.65f,
                    new float[]{ 0.0f, 0.75f, 1.0f },
                    new Color[]{ new Color(0, 0, 0, 0), new Color(0, 0, 0, 30), new Color(0, 0, 0, 85) }
            );
            g2.setPaint(vignette);
            g2.fillRect(0, 0, w, h);
        }

        g2.dispose();
    }

    private void playTone(int hz, int msec) {
        if (!soundEnabled) return;
        new Thread(() -> {
            try {
                float sampleRate = 8000f;
                byte[] buf = new byte[(int) (sampleRate * (msec / 1000f))];
                for (int i = 0; i < buf.length; i++) {
                    double angle = i / (sampleRate / hz) * 2.0 * Math.PI;
                    buf[i] = (byte) (Math.sin(angle) * 70);
                }
                AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();
                sdl.write(buf, 0, buf.length);
                sdl.drain();
                sdl.close();
            } catch (Exception ignored) {}
        }).start();
    }
}
