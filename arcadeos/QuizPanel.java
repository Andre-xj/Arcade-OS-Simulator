package arcadeos;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;

/**
 * Screen: QUIZ MODE // OPERATING SYSTEM ALGORITHM MASTERY CONSOLE
 * -----------------------------------------------------------------------------
 * 100% USEFUL, COHESIVE, HIGH-TECH ARCADE INTERFACE:
 *
 * 1. TOP ARCADE NAVIGATION & CARTRIDGE SELECTOR:
 *    - 8 quick-switch module cartridges (FCFS, Banker, Paging, Cache, Seg, Fit, Disk, Util).
 *    - Quick controls: Stage Select, CRT filter, Audio SFX, Rank & XP Badges.
 *
 * 2. MISSION DIRECTIVE & OPERATOR COCKPIT:
 *    - Algorithm Objective Briefing & Real-World Operating System Context.
 *    - Live Operator Profile (Callsign, Shield Integrity, Accuracy Rating, Combo Streak).
 *    - Difficulty Tier Toggle: [ FILL MISSING ] vs [ SOLVE ALL ].
 *
 * 3. INTERACTIVE CIPHER MATRIX (THE WORKSHEET):
 *    - Ample width, zero truncated text, auto-fitted readable columns.
 *    - Glowing neon key slots [ ? ] with auto-clear on double-click.
 *    - Real-time cell decryption status (Emerald ✔ / Crimson ✘).
 *
 * 4. CONTEXTUAL STEP HELPER & LIVE OS METRICS DECK (HIGH UTILITY!):
 *    - Dynamic Cell Inspector: Automatically detects which cell you click and explains the
 *      exact formula and row variables needed to calculate that specific cell!
 *    - 3 Live Operating System Diagnostic Pods:
 *      • Pod 1: Live Algorithm Performance Metrics (Avg Wait, Fault Rate, Saturation).
 *      • Pod 2: Hardware Dispatch Policy & Core Architecture Rules.
 *      • Pod 3: Solution Integrity & XP Reward Potential.
 *
 * 5. TACTICAL INTEL & FULL-SIZED CRT PHOSPHOR AUDIT TERMINAL:
 *    - Silicon IC Chip Formula Reference.
 *    - Live Hacker Audit Log reporting exact calculation mismatches and complete derivations.
 *
 * 6. ELEVATED 3D ARCADE ACTION CONTROLS & CELEBRATION ENGINE:
 *    - [ ⚡ VERIFY / SUBMIT ], [ 🎲 RE-ROLL (R) ], [ 💡 SOLUTION (SPACE) ], [ 🔄 REBOOT ], [ 🕹 MINIGAME ].
 *    - S-Rank confetti celebration, synth audio feedback, and dynamic room lighting.
 */
public class QuizPanel extends JPanel {
    private final ArcadeOS parentFrame;
    private QuizManager.Difficulty currentDifficulty = QuizManager.Difficulty.AVERAGE;
    private QuizManager.Mode currentMode = QuizManager.Mode.FILL_MISSING;
    private int currentModuleIdx = 0; // 0 to 7
    private QuizManager.QuizQuestion activeQuestion;
    private JButton btnDifficultySelect;

    // Module Cartridge Titles & Colors
    private final JButton[] modButtons = new JButton[8];
    private final boolean[] stageCleared = new boolean[8];
    private JPanel stageProgressStrip;
    private JButton btnNextStage;
    private JButton btnPrevStage;
    private static final String[] MOD_TITLES = {
            "01 INVADERS", "02 SPACE", "03 HOPPER", "04 MARIO",
            "05 SNAKE", "06 CRANE", "07 PAC-MAN", "08 OVERCOOKED"
    };

    public static final Color[] MOD_COLORS = {
            new Color(40, 240, 255),  // 01 Invaders Cyan
            new Color(255, 180, 50),  // 02 Space Shoot Amber Gold
            new Color(40, 255, 200),  // 03 Frame Hopper Neon Mint
            new Color(255, 115, 115), // 04 Mario Coral
            new Color(90, 245, 140),  // 05 Cyber Snake Acid Green
            new Color(215, 145, 255), // 06 Cyber Crane Violet
            new Color(255, 230, 50),  // 07 Pac-Man Yellow
            new Color(255, 130, 60)   // 08 Overcooked Strawberry Orange
    };

    private static final String[] MOD_OBJECTIVES = {
            "Calculate Start, Finish, Waiting, and Turnaround times.",
            "Derive Need Matrix (Max - Alloc) and safe sequence.",
            "Compute Page # (p), Offset (d), and Physical Frame address.",
            "Evaluate Page Faults, Hits, and Cache Hit Ratio.",
            "Check Offset against Segment Limit bound to trap SegFaults.",
            "Determine socket allocation and internal fragmentation.",
            "Calculate cylinder seek hops and cumulative travel distance.",
            "Compute multiprogramming slice (%CPU/job) and completion times."
    };

    // Header Elements
    private JLabel lblBreadcrumb;
    private JLabel lblXpScore;
    private JLabel lblRankBadge;
    private JButton btnCrtToggle;
    private JButton btnSfxToggle;

    // Cockpit Elements
    private JLabel lblMissionTitle;
    private JLabel lblMissionDesc;
    private JLabel lblPlayerShields;
    private JLabel lblComboStreak;
    private JLabel lblAccuracyRate;
    private JButton btnModeFill;
    private JButton btnModeSolve;
    private int playerShieldPct = 100;

    // Interactive Cipher Matrix
    private JTable cipherTable;
    private DefaultTableModel tableModel;
    private JProgressBar cipherProgressBar;
    private JLabel lblProgressText;
    private JLabel lblStatusBadge;
    private JLabel lblFeedback;

    // System Archives Dossier Elements
    private JLabel lblDossierTitle;
    private JLabel lblDossierCategory;
    private JLabel lblDossierDesc;

    public static final String[] STAGE_CATEGORIES = {
        "CORE CPU SCHEDULING",
        "DEADLOCK VERIFICATION",
        "MMU BUS & PAGE CROSSING",
        "CACHE FRAME EVICTION",
        "VARIABLE BOUNDS & HEAP",
        "CONTIGUOUS STORAGE",
        "I/O CYLINDER TRAVERSAL",
        "PROCESSOR CONCURRENCY"
    };

    public static final String[] ARCHIVE_STAGE_TITLES = {
        "STAGE 01: INVADERS // CPU DISPATCH",
        "STAGE 02: ASTEROIDS // BANKER'S SAFETY",
        "STAGE 03: HOPPER // MMU PAGING",
        "STAGE 04: MARIO // PAGE REPLACEMENT",
        "STAGE 05: SNAKE // SEGMENTATION",
        "STAGE 06: CRANE // DYNAMIC PARTITIONS",
        "STAGE 07: PAC-MAN // DISK SEEK",
        "STAGE 08: OVERCOOKED // CONCURRENCY"
    };

    public static final Color[] STAGE_THEME_COLORS = {
        new Color(40, 240, 255),  // 01 Space Invaders: Electric Cyan
        new Color(255, 180, 50),  // 02 Space Shoot: Amber Gold
        new Color(40, 255, 200),  // 03 Frame Hopper: Neon Mint
        new Color(255, 115, 115), // 04 Super Mario: Mushroom Coral
        new Color(90, 245, 140),  // 05 Heap Snake: Acid Green
        new Color(215, 145, 255), // 06 Cyber Crane: Cyber Violet
        new Color(255, 230, 50),  // 07 Pac-Man: Pac-Man Yellow
        new Color(255, 115, 185)  // 08 Overcooked: Strawberry Pink
    };

    // Contextual Step Helper & OS Diagnostic Deck
    private JScrollPane scrollTable;
    private JLabel lblDifficultyBadge;
    private JLabel lblStepTargetTag = new JLabel();
    private JLabel lblStepFormulaBox = new JLabel();
    private JLabel lblStepDerivationBox = new JLabel();
    private JLabel lblStepConceptBox = new JLabel();
    private JLabel lblStepTipBox = new JLabel();

    // Interactive Sequence Order Challenge Deck
    private JLabel lblSequenceTitle = new JLabel("⚡ SEQUENCE CHALLENGE");
    private JLabel lblSequencePrompt = new JLabel("Arrange the execution order:");
    private JLabel lblSequenceTape = new JLabel();
    private JPanel pnlSequenceTokens = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
    private JLabel lblSequenceFeedback = new JLabel("STATUS: Click tokens above in dispatch order.");
    private final List<String> currentSequenceList = new ArrayList<>();
    private boolean sequenceVerified = false;
    private final Map<String, JButton> tokenButtonMap = new LinkedHashMap<>();

    // Quick Math Register / Scratchpad
    private JTextField txtScratchA = new JTextField("0.0", 5);
    private JComboBox<String> cmbScratchOp = new JComboBox<>(new String[]{ "+", "-", "×", "÷", "%" });
    private JTextField txtScratchB = new JTextField("0.0", 5);
    private JLabel lblScratchResult = new JLabel("= 0.0");
    private boolean updatingScratchpad = false;
    private JPanel pnlCategoryVisualizer;

    private CardLayout cardsInputMode;
    private JPanel pnlInputSwitch;
    private JPanel pnlMathCalcRow;
    private JPanel pnlWordSelectRow;
    private JButton btnTool1;
    private JButton btnTool2;
    private JButton btnTool3;
    private JButton btnNextBlank;

    private JLabel lblTableVaultAssets;
    private JLabel lblTableDossierTag;
    private JLabel lblSequenceHint;

    private JPanel pnlMarioPolicyStrip;
    private JButton btnPolicyFifo;
    private JButton btnPolicyLru;
    private JButton btnPolicyOpt;

    private JPanel pnlCranePolicyStrip;
    private JButton btnPolicyFirstFit;
    private JButton btnPolicyBestFit;
    private JButton btnPolicyNextFit;

    private JPanel pnlPacmanPolicyStrip;
    private JButton btnPolicyFcfs;
    private JButton btnPolicySstf;
    private JButton btnPolicyScan;
    private JButton btnPolicyCScan;
    private JButton btnPolicyLook;
    private JButton btnPolicyCLook;

    private JPanel pnlKitchenStrip;

    private JLabel lblInspectorTarget = new JLabel();
    private JLabel lblInspectorFormula = new JLabel();
    private JLabel lblInspectorVars = new JLabel();
    private JLabel lblMetricPod1Title = new JLabel();
    private JLabel lblMetricPod1Val = new JLabel();
    private JLabel lblMetricPod2Title = new JLabel();
    private JLabel lblMetricPod2Val = new JLabel();
    private JLabel lblMetricPod3Title = new JLabel();
    private JLabel lblMetricPod3Val = new JLabel();

    // Right Side: Silicon Formula Chips & CRT Terminal
    private JPanel formulaPanel;
    private JTextArea txtTerminalLog = new JTextArea();

    private int[][] cellValidationState;
    public static boolean soundEnabled = true;
    private static boolean crtEnabled = false;
    private int streakCount = 0;
    private boolean updatingTable = false;

    // 60 FPS Animation Engine
    private final javax.swing.Timer animTimer;
    private int pulseTicks = 0;
    private float glowPulse = 0.7f;

    // Parallax Dust Particles
    private static class CyberParticle {
        float x, y, speed, alpha, size;
    }
    private final List<CyberParticle> particles = new ArrayList<>();

    // Toast HUD
    private String activeToastMessage = null;
    private float toastAlpha = 0.0f;
    private javax.swing.Timer toastTimer;

    // Confetti Celebration
    private static class ConfettiParticle {
        float x, y, vx, vy, size;
        Color color;
        float alpha;
    }
    private final List<ConfettiParticle> confetti = new ArrayList<>();

    public QuizPanel(ArcadeOS parent) {
        this.parentFrame = parent;
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(0, 4));
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        for (int i = 0; i < 35; i++) {
            CyberParticle cp = new CyberParticle();
            cp.x = (float) (Math.random() * 1280);
            cp.y = (float) (Math.random() * 800);
            cp.speed = (float) (0.3 + Math.random() * 0.7);
            cp.size = (float) (1.5 + Math.random() * 2.5);
            cp.alpha = (float) (0.15 + Math.random() * 0.35);
            particles.add(cp);
        }

        // 1. Top Navigation Bar
        add(createTopNavBar(), BorderLayout.NORTH);

        // 2. Center Battle Arena (Cockpit + Worksheet + Contextual Helper + Terminal)
        add(createBattleArenaCenter(), BorderLayout.CENTER);

        // 3. Bottom Elevated 3D Arcade Push Buttons
        add(createBottomActionDeck(), BorderLayout.SOUTH);

        // 60 FPS Animation Timer
        animTimer = new javax.swing.Timer(16, e -> {
            pulseTicks++;
            glowPulse = (float) (0.60 + 0.38 * Math.sin(pulseTicks * 0.05));

            for (CyberParticle p : particles) {
                p.y -= p.speed;
                if (p.y < 0) {
                    p.y = getHeight() > 0 ? getHeight() : 800;
                    p.x = (float) (Math.random() * (getWidth() > 0 ? getWidth() : 1280));
                }
            }

            for (int i = confetti.size() - 1; i >= 0; i--) {
                ConfettiParticle cp = confetti.get(i);
                cp.x += cp.vx;
                cp.y += cp.vy;
                cp.vy += 0.16f;
                cp.alpha -= 0.015f;
                if (cp.alpha <= 0.0f || cp.y > getHeight()) {
                    confetti.remove(i);
                }
            }
            repaint();
        });
        animTimer.start();

        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_N || code == KeyEvent.VK_RIGHT) {
                    advanceToNextMission();
                } else if (code == KeyEvent.VK_P || code == KeyEvent.VK_LEFT) {
                    goToPreviousMission();
                } else if (code == KeyEvent.VK_R) {
                    randomizeCurrentQuestion();
                } else if (code == KeyEvent.VK_ENTER && (cipherTable == null || !cipherTable.isEditing())) {
                    checkAnswers();
                } else if (code == KeyEvent.VK_SPACE && (cipherTable == null || !cipherTable.isEditing())) {
                    revealAnswers();
                } else if (code == KeyEvent.VK_M) {
                    launchActiveMinigame();
                } else if (code == KeyEvent.VK_D && parentFrame != null) {
                    playTone(520, 20);
                    parentFrame.showScreen(ArcadeOS.SCREEN_QUIZ_DIFFICULTY);
                } else if (code == KeyEvent.VK_C) {
                    toggleCrt();
                } else if (code == KeyEvent.VK_S) {
                    toggleSfx();
                } else if (code == KeyEvent.VK_ESCAPE && parentFrame != null) {
                    playTone(480, 20);
                    parentFrame.selectQuizMission(currentModuleIdx);
                }
            }
        });

        addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override public void ancestorAdded(javax.swing.event.AncestorEvent e) { onShowQuizScreen(); }
            @Override public void ancestorRemoved(javax.swing.event.AncestorEvent e) {}
            @Override public void ancestorMoved(javax.swing.event.AncestorEvent e) {}
        });

        loadQuestion(0, currentDifficulty, QuizManager.Mode.FILL_MISSING, true);
    }

    public void startMission(int modIdx, QuizManager.Difficulty diff) {
        if (diff == null) diff = QuizManager.Difficulty.AVERAGE;
        if (modIdx < 0 || modIdx >= 8) modIdx = 0;
        this.currentDifficulty = diff;
        this.currentModuleIdx = modIdx;
        this.streakCount = 0;
        this.playerShieldPct = 100;
        for (int i = 0; i < 8; i++) stageCleared[i] = false;
        updateDifficultyUI();
        if (btnPrevStage != null) btnPrevStage.setEnabled(currentModuleIdx > 0);
        if (btnNextStage != null) {
            btnNextStage.setText("NEXT MISSION ▶ [N]");
            btnNextStage.setBackground(new Color(0, 140, 210));
        }
        updateStageProgressStrip();
        loadQuestion(currentModuleIdx, currentDifficulty, currentMode, true);
    }

    public void startCampaign(QuizManager.Difficulty diff) {
        startMission(0, diff);
    }

    public void setDifficulty(QuizManager.Difficulty diff) {
        if (diff == null) diff = QuizManager.Difficulty.AVERAGE;
        this.currentDifficulty = diff;
        updateDifficultyUI();
        loadQuestion(currentModuleIdx, currentDifficulty, currentMode, true);
    }

    private void updateDifficultyUI() {
        Color col = (currentDifficulty == QuizManager.Difficulty.EASY) ? RetroTheme.ACCENT_GREEN
                : (currentDifficulty == QuizManager.Difficulty.AVERAGE ? RetroTheme.ACCENT_YELLOW : RetroTheme.ACCENT_PINK);
        if (btnDifficultySelect != null) {
            btnDifficultySelect.setText("TIER: " + currentDifficulty.label + " ▾");
            btnDifficultySelect.setForeground(col);
        }
        if (lblDifficultyBadge != null) {
            lblDifficultyBadge.setText(" [ " + currentDifficulty.tier.toUpperCase() + " // " + currentDifficulty.label + " ] ");
            lblDifficultyBadge.setBackground(col);
            lblDifficultyBadge.setForeground(RetroTheme.BG_DARK);
        }
    }

    public void onShowQuizScreen() {
        updateDifficultyUI();
        randomizeCurrentQuestion();
        showToast("⚡ CONSOLE ONLINE // " + currentDifficulty.label + " [" + currentDifficulty.tier + "] ARMED");
        requestFocusInWindow();
    }

    public void showToast(String message) {
        activeToastMessage = message;
        toastAlpha = 1.0f;
        if (toastTimer != null && toastTimer.isRunning()) toastTimer.stop();
        toastTimer = new javax.swing.Timer(25, new ActionListener() {
            int ticks = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                ticks++;
                if (ticks > 55) {
                    toastAlpha -= 0.05f;
                    if (toastAlpha <= 0.0f) {
                        toastAlpha = 0.0f;
                        toastTimer.stop();
                    }
                }
                repaint();
            }
        });
        toastTimer.start();
    }

    private void triggerCelebrationConfetti() {
        confetti.clear();
        int w = getWidth() > 0 ? getWidth() : 1000;
        int h = getHeight() > 0 ? getHeight() : 700;
        Color[] festiveColors = {
                new Color(255, 230, 50), new Color(40, 240, 255), new Color(255, 115, 185),
                new Color(57, 255, 60),  new Color(255, 140, 50), new Color(215, 145, 255), Color.WHITE
        };
        for (int i = 0; i < 70; i++) {
            ConfettiParticle cp = new ConfettiParticle();
            cp.x = (float) (w * 0.25 + Math.random() * (w * 0.50));
            cp.y = (float) (h * 0.15 + Math.random() * (h * 0.35));
            cp.vx = (float) (-8.0 + Math.random() * 16.0);
            cp.vy = (float) (-10.0 + Math.random() * 8.0);
            cp.size = (float) (4.0 + Math.random() * 6.0);
            cp.color = festiveColors[(int) (Math.random() * festiveColors.length)];
            cp.alpha = 1.0f;
            confetti.add(cp);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        Color activeTheme = MOD_COLORS[currentModuleIdx];
        Point2D center = new Point2D.Float(w * 0.50f, h * 0.35f);
        float radius = Math.max(w, h) * 0.70f;
        int glowAlpha = (int) (35 * glowPulse);
        Color glowColor = new Color(activeTheme.getRed(), activeTheme.getGreen(), activeTheme.getBlue(), glowAlpha);
        Color fadeColor = new Color(RetroTheme.BG_DARK.getRed(), RetroTheme.BG_DARK.getGreen(), RetroTheme.BG_DARK.getBlue(), 0);

        RadialGradientPaint rgp = new RadialGradientPaint(center, radius, new float[]{ 0.0f, 1.0f }, new Color[]{ glowColor, fadeColor });
        g2.setPaint(rgp);
        g2.fillRect(0, 0, w, h);

        g2.setColor(new Color(activeTheme.getRed(), activeTheme.getGreen(), activeTheme.getBlue(), 10));
        g2.setStroke(new BasicStroke(1.0f));
        int gridSpacing = 40;
        for (int x = 0; x < w; x += gridSpacing) g2.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += gridSpacing) g2.drawLine(0, y, w, y);

        for (CyberParticle p : particles) {
            g2.setColor(new Color(activeTheme.getRed(), activeTheme.getGreen(), activeTheme.getBlue(), (int) (p.alpha * 200)));
            g2.fillOval((int) p.x, (int) p.y, (int) p.size, (int) p.size);
        }
        g2.dispose();
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        for (ConfettiParticle cp : confetti) {
            g2.setColor(new Color(cp.color.getRed(), cp.color.getGreen(), cp.color.getBlue(), (int) (cp.alpha * 240)));
            g2.fillRect((int) cp.x, (int) cp.y, (int) cp.size, (int) cp.size);
        }

        // Floating Toast Notification HUD
        if (activeToastMessage != null && toastAlpha > 0.01f) {
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int msgW = fm.stringWidth(activeToastMessage) + 36;
            int msgH = 30;
            int tx = (w - msgW) / 2;
            int ty = h - 68;

            g2.setColor(new Color(0, 0, 0, (int) (160 * toastAlpha)));
            g2.fillRoundRect(tx + 2, ty + 2, msgW, msgH, 8, 8);

            g2.setColor(new Color(16, 12, 34, (int) (245 * toastAlpha)));
            g2.fillRoundRect(tx, ty, msgW, msgH, 8, 8);

            Color activeTheme = MOD_COLORS[currentModuleIdx];
            g2.setColor(new Color(activeTheme.getRed(), activeTheme.getGreen(), activeTheme.getBlue(), (int) (240 * toastAlpha)));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(tx, ty, msgW, msgH, 8, 8);

            g2.setColor(Color.WHITE);
            g2.drawString(activeToastMessage, tx + 18, ty + 20);
        }

        if (crtEnabled) {
            g2.setColor(new Color(0, 0, 0, 20));
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

    // ─────────────────────────────────────────────────────────────────────────
    // 1. TOP ARCADE NAVIGATION & MISSION PROGRESSION HUD
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel createTopNavBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        // LEFT: Return to Difficulty Selection Gate, Breadcrumb, Docs/Manual
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JButton btnBack = RetroTheme.createRetroButton("< DIFFICULTY [ESC]", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnBack.setFont(new Font("Consolas", Font.BOLD, 10));
        btnBack.setFocusable(false);
        btnBack.setToolTipText("Return to level difficulty selection gate [ESC]");
        btnBack.addActionListener(e -> {
            playTone(480, 20);
            if (parentFrame != null) parentFrame.selectQuizMission(currentModuleIdx);
        });
        left.add(btnBack);

        lblBreadcrumb = new JLabel("OS CAMPAIGN // STAGE 01 / 08");
        lblBreadcrumb.setFont(new Font("Consolas", Font.BOLD, 11));
        lblBreadcrumb.setForeground(Color.WHITE);
        left.add(lblBreadcrumb);

        JButton btnManualDocs = RetroTheme.createRetroButton("DOCS // MANUAL", RetroTheme.BTN_DARK, RetroTheme.ACCENT_YELLOW);
        btnManualDocs.setFont(new Font("Consolas", Font.BOLD, 10));
        btnManualDocs.setFocusable(false);
        btnManualDocs.addActionListener(e -> {
            playTone(520, 20);
            new ArchiveManualDialog(parentFrame, null, currentModuleIdx).setVisible(true);
        });
        left.add(btnManualDocs);
        bar.add(left, BorderLayout.WEST);

        // RIGHT: Stats HUD & Settings
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);

        lblRankBadge = new JLabel("RANK: UNRANKED");
        lblRankBadge.setFont(new Font("Consolas", Font.BOLD, 10));
        lblRankBadge.setForeground(RetroTheme.TEXT_DIM);
        lblRankBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 48, 85), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        right.add(lblRankBadge);

        lblXpScore = new JLabel("XP: " + AppState.getScoreFormatted());
        lblXpScore.setFont(new Font("Consolas", Font.BOLD, 10));
        lblXpScore.setForeground(RetroTheme.ACCENT_YELLOW);
        lblXpScore.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
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

        btnDifficultySelect = RetroTheme.createRetroButton("TIER: " + currentDifficulty.label + " ▾", RetroTheme.BTN_DARK, RetroTheme.ACCENT_GREEN);
        btnDifficultySelect.setFont(new Font("Consolas", Font.BOLD, 10));
        btnDifficultySelect.setFocusable(false);
        btnDifficultySelect.setToolTipText("Change simulation difficulty tier [D]");
        btnDifficultySelect.addActionListener(e -> {
            playTone(520, 20);
            if (parentFrame != null) parentFrame.showScreen(ArcadeOS.SCREEN_QUIZ_DIFFICULTY);
        });
        right.add(btnDifficultySelect);

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createStageProgressStrip() {
        stageProgressStrip = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        stageProgressStrip.setOpaque(false);
        updateStageProgressStrip();
        return stageProgressStrip;
    }

    private void updateStageProgressStrip() {
        if (stageProgressStrip == null) return;
        stageProgressStrip.removeAll();

        for (int i = 0; i < 8; i++) {
            final int idx = i;
            boolean active = (idx == currentModuleIdx);
            boolean cleared = stageCleared[idx];
            Color col = MOD_COLORS[idx];

            String txt = cleared ? "✔ M" + (idx + 1) : (active ? "► M" + (idx + 1) : "M" + (idx + 1));
            JLabel pill = new JLabel(txt) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (active) {
                        g2.setColor(new Color(col.getRed() / 4, col.getGreen() / 4, col.getBlue() / 4, 240));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                        g2.setColor(col);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                    } else if (cleared) {
                        g2.setColor(new Color(12, 45, 24, 220));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                        g2.setColor(new Color(57, 255, 60, 160));
                        g2.setStroke(new BasicStroke(1.0f));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                    } else {
                        g2.setColor(new Color(18, 14, 32, 190));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                        g2.setColor(new Color(55, 42, 80));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            pill.setFont(new Font("Consolas", Font.BOLD, 10));
            pill.setForeground(active ? Color.WHITE : (cleared ? new Color(57, 255, 60) : new Color(130, 120, 160)));
            pill.setHorizontalAlignment(SwingConstants.CENTER);
            pill.setPreferredSize(new Dimension(active ? 58 : 42, 22));
            pill.setToolTipText("Mission 0" + (idx + 1) + ": " + MOD_TITLES[idx] + (cleared ? " [CLEARED]" : (active ? " [ACTIVE]" : " [PENDING]")));
            stageProgressStrip.add(pill);
        }
        stageProgressStrip.revalidate();
        stageProgressStrip.repaint();
    }

    public void selectModule(int idx) {
        if (idx < 0 || idx >= 8) return;
        if (cipherTable != null && cipherTable.isEditing() && cipherTable.getCellEditor() != null) {
            cipherTable.getCellEditor().cancelCellEditing();
        }
        currentModuleIdx = idx;
        if (btnPrevStage != null) btnPrevStage.setEnabled(currentModuleIdx > 0);
        if (btnNextStage != null) {
            if (currentModuleIdx == 7) {
                btnNextStage.setText("FINISH CAMPAIGN 🏆");
                btnNextStage.setBackground(new Color(255, 170, 0));
            } else {
                btnNextStage.setText("NEXT MISSION ▶ [N]");
                btnNextStage.setBackground(new Color(0, 140, 210));
            }
        }
        updateStageProgressStrip();
        playTone(440 + idx * 40, 25);
        loadQuestion(currentModuleIdx, currentDifficulty, QuizManager.Mode.FILL_MISSING, true);
        requestFocusInWindow();
    }

    public void advanceToNextMission() {
        if (currentModuleIdx < 7) {
            selectModule(currentModuleIdx + 1);
            showToast("★ " + ARCHIVE_STAGE_TITLES[currentModuleIdx] + " ENGAGED ★");
        } else {
            showCampaignVictoryDialog();
        }
    }

    public void goToPreviousMission() {
        if (currentModuleIdx > 0) {
            selectModule(currentModuleIdx - 1);
            showToast("◀ RETURNED TO STAGE 0" + (currentModuleIdx + 1));
        }
    }

    private void showCampaignVictoryDialog() {
        playVictorySound();
        triggerCelebrationConfetti();

        JDialog dlg = new JDialog(parentFrame, "MINI // OS - CAMPAIGN COMPLETE // 8/8 MISSIONS CLEARED", true);
        dlg.setLayout(new BorderLayout());
        dlg.getContentPane().setBackground(new Color(12, 8, 24));
        dlg.setSize(560, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel p = new JPanel(new BorderLayout(0, 16)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(20, 12, 42), 0, getHeight(), new Color(8, 4, 18));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(new Color(255, 215, 0, 180));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(new Color(5, 217, 232, 100));
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Header
        JPanel headPnl = new JPanel(new GridLayout(2, 1, 0, 4));
        headPnl.setOpaque(false);
        JLabel lTrophy = new JLabel("🏆 OS SIMULATION MASTER // ALL 8 MISSIONS CLEARED! 🏆", SwingConstants.CENTER);
        lTrophy.setFont(new Font("Consolas", Font.BOLD, 14));
        lTrophy.setForeground(RetroTheme.ACCENT_YELLOW);
        headPnl.add(lTrophy);

        JLabel lSub = new JLabel("CLEARANCE RECORD: S-RANK OPERATOR // CAMPAIGN COMPLETE", SwingConstants.CENTER);
        lSub.setFont(new Font("Consolas", Font.BOLD, 11));
        lSub.setForeground(RetroTheme.ACCENT_CYAN);
        headPnl.add(lSub);
        p.add(headPnl, BorderLayout.NORTH);

        // Body stats
        JPanel statsBox = new JPanel(new GridLayout(4, 1, 0, 8));
        statsBox.setOpaque(true);
        statsBox.setBackground(new Color(22, 16, 44));
        statsBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 55, 120), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JLabel s1 = new JLabel("● DIFFICULTY TIER  : " + currentDifficulty.tier + " [" + currentDifficulty.label + "]");
        s1.setFont(new Font("Consolas", Font.BOLD, 12));
        s1.setForeground(Color.WHITE);
        statsBox.add(s1);

        JLabel s2 = new JLabel("● TOTAL XP EARNED  : " + AppState.getScoreFormatted() + " XP");
        s2.setFont(new Font("Consolas", Font.BOLD, 12));
        s2.setForeground(RetroTheme.ACCENT_YELLOW);
        statsBox.add(s2);

        JLabel s3 = new JLabel("● FINAL ACCURACY   : " + lblAccuracyRate.getText());
        s3.setFont(new Font("Consolas", Font.BOLD, 12));
        s3.setForeground(RetroTheme.ACCENT_GREEN);
        statsBox.add(s3);

        JLabel s4 = new JLabel("● MAX WINNING COMBO: " + streakCount + "X STREAK");
        s4.setFont(new Font("Consolas", Font.BOLD, 12));
        s4.setForeground(RetroTheme.ACCENT_CYAN);
        statsBox.add(s4);
        p.add(statsBox, BorderLayout.CENTER);

        // Buttons
        JPanel btnPnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnPnl.setOpaque(false);

        JButton btnMenu = RetroTheme.createRetroButton("◀ MAIN MENU", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnMenu.setFont(new Font("Consolas", Font.BOLD, 11));
        btnMenu.setPreferredSize(new Dimension(140, 36));
        btnMenu.addActionListener(e -> {
            dlg.dispose();
            if (parentFrame != null) parentFrame.showScreen(ArcadeOS.SCREEN_MAIN_MENU);
        });
        btnPnl.add(btnMenu);

        JButton btnDiff = RetroTheme.createRetroButton("⚙ CHANGE TIER", RetroTheme.BTN_DARK, RetroTheme.ACCENT_YELLOW);
        btnDiff.setFont(new Font("Consolas", Font.BOLD, 11));
        btnDiff.setPreferredSize(new Dimension(150, 36));
        btnDiff.addActionListener(e -> {
            dlg.dispose();
            if (parentFrame != null) parentFrame.showScreen(ArcadeOS.SCREEN_QUIZ_DIFFICULTY);
        });
        btnPnl.add(btnDiff);

        JButton btnReplay = RetroTheme.createRetroButton("↺ REPLAY [M1]", RetroTheme.BTN_DARK, RetroTheme.ACCENT_GREEN);
        btnReplay.setFont(new Font("Consolas", Font.BOLD, 11));
        btnReplay.setPreferredSize(new Dimension(140, 36));
        btnReplay.addActionListener(e -> {
            dlg.dispose();
            startCampaign(currentDifficulty);
        });
        btnPnl.add(btnReplay);

        p.add(btnPnl, BorderLayout.SOUTH);

        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    private void updateModuleButtons() {
        updateStageProgressStrip();
        repaint();
    }

    private void toggleCrt() {
        crtEnabled = !crtEnabled;
        btnCrtToggle.setText(crtEnabled ? "CRT: ON" : "CRT: OFF");
        btnCrtToggle.setForeground(crtEnabled ? RetroTheme.ACCENT_GREEN : RetroTheme.TEXT_DIM);
        showToast(crtEnabled ? "CRT RASTER FILTER ENGAGED" : "CRT RASTER FILTER DISABLED");
        repaint();
        requestFocusInWindow();
    }

    private void toggleSfx() {
        soundEnabled = !soundEnabled;
        btnSfxToggle.setText(soundEnabled ? "SFX: ON" : "SFX: OFF");
        btnSfxToggle.setForeground(soundEnabled ? RetroTheme.ACCENT_GREEN : RetroTheme.TEXT_DIM);
        if (soundEnabled) playTone(600, 30);
        showToast(soundEnabled ? "AUDIO SYNTHESIS ON" : "AUDIO SYNTHESIS MUTED");
        requestFocusInWindow();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. CENTER BATTLE ARENA (100% USEFUL & FUNCTIONAL DECK)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel createBattleArenaCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 4));
        center.setOpaque(false);

        // A. Top Mission Directive & Operator Cockpit
        center.add(createMissionDirectiveCockpit(), BorderLayout.NORTH);

        // B. Main Arena Split: Left Matrix + Contextual Helper (62%), Right Intel + Terminal (38%)
        JPanel split = new JPanel(new GridBagLayout());
        split.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Left Section: Worksheet + Contextual Diagnostic Helper Deck
        gbc.gridx = 0;
        gbc.weightx = 0.62;
        gbc.insets = new Insets(0, 0, 0, 6);
        JPanel leftPanel = createCipherMatrixAndHelperPanel();
        leftPanel.setMinimumSize(new Dimension(300, 200));
        split.add(leftPanel, gbc);

        // Right Section: Silicon Formula Chips & CRT Phosphor Terminal
        gbc.gridx = 1;
        gbc.weightx = 0.38;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel rightPanel = createIntelTerminalSidePanel();
        rightPanel.setMinimumSize(new Dimension(300, 200));
        split.add(rightPanel, gbc);

        center.add(split, BorderLayout.CENTER);
        return center;
    }

    /**
     * Compact, highly informative mission directive and operator cockpit header
     */
    private JPanel createMissionDirectiveCockpit() {
        JPanel cockpit = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color activeC = MOD_COLORS[currentModuleIdx];
                GradientPaint gp = new GradientPaint(0, 0, new Color(20, 14, 40, 240),
                        0, getHeight(), new Color(10, 6, 22, 250));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                // Corner Hazard Caution Stripes
                int hzW = 36;
                for (int x = 0; x < hzW; x += 8) {
                    g2.setColor(new Color(255, 200, 40, 140));
                    g2.fillPolygon(new int[]{x, x + 4, x + 8, x + 4}, new int[]{0, 0, 4, 4}, 4);
                    int rx = getWidth() - hzW + x;
                    g2.fillPolygon(new int[]{rx, rx + 4, rx + 8, rx + 4}, new int[]{0, 0, 4, 4}, 4);
                }

                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillRoundRect(1, 1, getWidth() - 2, (getHeight() / 2) - 1, 7, 7);

                g2.setColor(new Color(activeC.getRed(), activeC.getGreen(), activeC.getBlue(), (int) (140 * glowPulse)));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cockpit.setOpaque(false);
        cockpit.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        cockpit.setPreferredSize(new Dimension(800, 56));

        // Left/Center: Mission Directive & Scenario
        JPanel missionBox = new JPanel();
        missionBox.setOpaque(false);
        missionBox.setLayout(new BoxLayout(missionBox, BoxLayout.Y_AXIS));

        lblMissionTitle = new JLabel("MISSION 01 // SPACE INVADERS [AVERAGE]");
        lblMissionTitle.setFont(new Font("Consolas", Font.BOLD, 12));
        lblMissionTitle.setForeground(RetroTheme.ACCENT_YELLOW);
        missionBox.add(lblMissionTitle);

        missionBox.add(Box.createVerticalStrut(2));

        lblMissionDesc = new JLabel("🎯 OBJECTIVE: " + MOD_OBJECTIVES[0]);
        lblMissionDesc.setFont(new Font("Consolas", Font.BOLD, 11));
        lblMissionDesc.setForeground(new Color(230, 240, 255));
        missionBox.add(lblMissionDesc);

        cockpit.add(missionBox, BorderLayout.CENTER);

        // Right: Prominent Difficulty Badge + Operator Stats
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        rightBox.setOpaque(false);

        lblDifficultyBadge = new JLabel(" [ RECRUIT TIER // EASY ] ");
        lblDifficultyBadge.setFont(new Font("Consolas", Font.BOLD, 10));
        lblDifficultyBadge.setForeground(RetroTheme.BG_DARK);
        lblDifficultyBadge.setBackground(RetroTheme.ACCENT_GREEN);
        lblDifficultyBadge.setOpaque(true);
        lblDifficultyBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        rightBox.add(lblDifficultyBadge);

        lblPlayerShields = new JLabel("SHIELDS: 100%");
        lblPlayerShields.setFont(new Font("Consolas", Font.BOLD, 10));
        lblPlayerShields.setForeground(RetroTheme.ACCENT_GREEN);
        rightBox.add(lblPlayerShields);

        lblComboStreak = new JLabel("COMBO: 0X");
        lblComboStreak.setFont(new Font("Consolas", Font.BOLD, 10));
        lblComboStreak.setForeground(new Color(255, 160, 40));
        rightBox.add(lblComboStreak);

        lblAccuracyRate = new JLabel("ACCURACY: 100%");
        lblAccuracyRate.setFont(new Font("Consolas", Font.BOLD, 10));
        lblAccuracyRate.setForeground(RetroTheme.ACCENT_CYAN);
        rightBox.add(lblAccuracyRate);

        cockpit.add(rightBox, BorderLayout.EAST);
        return cockpit;
    }

    /**
     * Worksheet Data Matrix + The Hero Interactive Step Inspector & Tutor Workbench
     */
    private JPanel createCipherMatrixAndHelperPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
        leftPanel.setOpaque(false);

        // ── TOP: Interactive Cipher Table Card ──
        JPanel tableCard = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color activeC = MOD_COLORS[currentModuleIdx];

                g2.setColor(new Color(16, 12, 30, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                g2.setColor(new Color(activeC.getRed(), activeC.getGreen(), activeC.getBlue(), 90));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                g2.setColor(new Color(80, 60, 110));
                g2.fillOval(4, 4, 4, 4);
                g2.fillOval(getWidth() - 8, 4, 4, 4);
                g2.fillOval(4, getHeight() - 8, 4, 4);
                g2.fillOval(getWidth() - 8, getHeight() - 8, 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tableCard.setOpaque(false);
        tableCard.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        tableModel = new DefaultTableModel();
        tableModel.addTableModelListener(e -> {
            if (!updatingTable && e.getType() == TableModelEvent.UPDATE) {
                updateProgressMeter();
        updateStageProgressStrip();
                if (pnlCategoryVisualizer != null) pnlCategoryVisualizer.repaint();
            }
        });

        cipherTable = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int col) {
                if (activeQuestion == null || activeQuestion.worksheetData == null) return false;
                if (row < 0 || row >= activeQuestion.worksheetData.length) return false;
                if (col < 0 || col >= activeQuestion.worksheetData[row].length) return false;
                String origVal = activeQuestion.worksheetData[row][col];
                return origVal != null && origVal.trim().equals("?");
            }

            @Override
            public TableCellEditor getCellEditor(int row, int col) {
                String[] wordOptions = getWordOptionsForCell(row, col);
                if (wordOptions != null && wordOptions.length > 0) {
                    JComboBox<String> cmb = createStyledTableComboBox(wordOptions);
                    return new DefaultCellEditor(cmb);
                }
                return super.getCellEditor(row, col);
            }

            @Override
            public Component prepareEditor(TableCellEditor editor, int row, int col) {
                Component c = super.prepareEditor(editor, row, col);
                if (c instanceof JTextField) {
                    JTextField tf = (JTextField) c;
                    tf.setOpaque(true);
                    tf.setBackground(new Color(36, 24, 62)); // Dark retro synthwave slot
                    tf.setForeground(Color.WHITE);           // Crisp white font
                    tf.setCaretColor(RetroTheme.ACCENT_CYAN); // Neon cyan caret
                    tf.setFont(new Font("Consolas", Font.BOLD, 12));
                    tf.setHorizontalAlignment(JTextField.CENTER);
                    tf.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 2),
                            BorderFactory.createEmptyBorder(0, 4, 0, 4)
                    ));
                    tf.setSelectionColor(new Color(5, 217, 232, 180));
                    tf.setSelectedTextColor(Color.BLACK);

                    String text = tf.getText();
                    if (text != null && (text.trim().equals("?") || text.trim().equals("[ ? ]"))) {
                        tf.setText("");
                    }
                    tf.selectAll();
                } else if (c instanceof JComboBox) {
                    JComboBox<?> cb = (JComboBox<?>) c;
                    cb.setBackground(new Color(36, 24, 62));
                    cb.setForeground(Color.WHITE);
                    cb.setFont(new Font("Consolas", Font.BOLD, 12));
                    Object curVal = getValueAt(row, col);
                    if (curVal != null) {
                        String s = curVal.toString().trim();
                        if (!s.equals("?") && !s.equals("[ ? ]")) {
                            cb.setSelectedItem(s);
                        }
                    }
                    SwingUtilities.invokeLater(() -> cb.showPopup());
                }
                return c;
            }
        };

        cipherTable.setFont(new Font("Consolas", Font.BOLD, 12));
        cipherTable.setRowHeight(30);
        cipherTable.setBackground(new Color(18, 14, 34));
        cipherTable.setForeground(Color.WHITE);
        cipherTable.setGridColor(new Color(55, 38, 80));
        cipherTable.setShowGrid(true);
        cipherTable.getTableHeader().setBackground(new Color(28, 20, 52));
        cipherTable.getTableHeader().setForeground(RetroTheme.ACCENT_CYAN);
        cipherTable.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 11));
        cipherTable.getTableHeader().setPreferredSize(new Dimension(800, 28));

        // When user clicks or selects any cell, update Contextual Step Inspector immediately!
        cipherTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateContextualInspector();
        });
        cipherTable.getColumnModel().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateContextualInspector();
        });

        // Fast keyboard navigation: ENTER commits value and advances to next [ ? ] cell!
        cipherTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enterNextCell");
        cipherTable.getActionMap().put("enterNextCell", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (cipherTable.isEditing() && cipherTable.getCellEditor() != null) {
                    cipherTable.getCellEditor().stopCellEditing();
                }
                int r = cipherTable.getSelectedRow();
                int c = cipherTable.getSelectedColumn();
                selectNextBlankCell(r, c);
            }
        });

        cipherTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFocus, r, c);

                boolean isEditable = (activeQuestion != null && activeQuestion.worksheetData != null
                        && r < activeQuestion.worksheetData.length && c < activeQuestion.worksheetData[r].length
                        && activeQuestion.worksheetData[r][c].trim().equals("?"));
                int valState = (cellValidationState != null && r < cellValidationState.length && c < cellValidationState[r].length)
                        ? cellValidationState[r][c] : 0;
                boolean isAvgRow = (activeQuestion != null && activeQuestion.worksheetData != null
                        && r < activeQuestion.worksheetData.length && activeQuestion.worksheetData[r][0].contains("AVERAGE"));

                if (c == 0) {
                    String rowTitle = (val != null) ? val.toString() : "";
                    if (rowTitle.contains("AVERAGE")) {
                        l.setIcon(null);
                        l.setFont(new Font("Consolas", Font.BOLD, 11));
                        l.setForeground(RetroTheme.ACCENT_YELLOW);
                        l.setBackground(new Color(42, 28, 8));
                        l.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(220, 175, 40)),
                                BorderFactory.createEmptyBorder(0, 8, 0, 0)
                        ));
                        l.setHorizontalAlignment(SwingConstants.LEFT);
                    } else if (currentModuleIdx == 7) {
                        l.setIcon(null);
                        l.setFont(new Font("Consolas", Font.BOLD, 12));
                        l.setForeground(RetroTheme.ACCENT_YELLOW);
                        l.setBackground(r % 2 == 0 ? new Color(20, 14, 38) : new Color(26, 18, 48));
                        l.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
                        l.setHorizontalAlignment(SwingConstants.CENTER);
                    } else {
                        BufferedImage miniIcon = getMascotSprite(currentModuleIdx, 20);
                        if (miniIcon != null) l.setIcon(new ImageIcon(miniIcon));
                        l.setFont(new Font("Consolas", Font.BOLD, 12));
                        l.setForeground(Color.WHITE);
                        l.setBackground(r % 2 == 0 ? new Color(20, 14, 38) : new Color(26, 18, 48));
                        l.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
                        l.setHorizontalAlignment(SwingConstants.LEFT);
                    }
                } else {
                    l.setIcon(null);
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                    l.setBorder(BorderFactory.createEmptyBorder());

                    if (isAvgRow && val != null && val.toString().trim().equals("-")) {
                        l.setBackground(new Color(28, 20, 6));
                        l.setForeground(new Color(140, 115, 60));
                        l.setFont(new Font("Consolas", Font.BOLD, 12));
                        l.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(180, 140, 30)));
                        l.setText("—");
                    } else if (valState == 1) {
                        l.setBackground(new Color(14, 75, 36));
                        l.setForeground(new Color(57, 255, 60));
                        l.setFont(new Font("Consolas", Font.BOLD, 12));
                        l.setBorder(BorderFactory.createLineBorder(new Color(57, 255, 60), 1));
                        l.setText(val != null ? "✔ " + val.toString() : "");
                    } else if (valState == 2) {
                        l.setBackground(new Color(90, 14, 32));
                        l.setForeground(new Color(255, 75, 95));
                        l.setFont(new Font("Consolas", Font.BOLD, 12));
                        l.setBorder(BorderFactory.createLineBorder(new Color(255, 75, 95), 1));
                        l.setText(val != null ? "✘ " + val.toString() : "");
                    } else if (val != null && val.toString().trim().equals("-")) {
                        l.setBackground(new Color(14, 10, 26));
                        l.setForeground(new Color(120, 110, 140));
                        l.setFont(new Font("Consolas", Font.BOLD, 12));
                        l.setText("—");
                    } else if (isEditable) {
                        String sVal = (val == null) ? "" : val.toString().trim();
                        if (sVal.equals("?") || sVal.isEmpty() || sVal.equals("[ ? ]")) {
                            int pulseR = (int) (60 + 40 * glowPulse);
                            int pulseG = (int) (35 + 25 * glowPulse);
                            int pulseB = (int) (10 + 10 * glowPulse);
                            l.setBackground(new Color(pulseR, pulseG, pulseB));
                            l.setForeground(Color.WHITE);
                            l.setFont(new Font("Consolas", Font.BOLD, 12));
                            l.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(new Color(255, 215, 0, 220), 1),
                                    BorderFactory.createEmptyBorder(0, 4, 0, 4)
                            ));
                            l.setText("[ ? ]");
                        } else {
                            l.setBackground(new Color(55, 36, 92));
                            l.setForeground(RetroTheme.ACCENT_CYAN);
                            l.setFont(new Font("Consolas", Font.BOLD, 12));
                            l.setBorder(BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 1));
                            l.setText(sVal);
                        }
                    } else {
                        if (isAvgRow) {
                            l.setBackground(new Color(38, 26, 8));
                            l.setForeground(RetroTheme.ACCENT_YELLOW);
                            l.setFont(new Font("Consolas", Font.BOLD, 12));
                            l.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(200, 160, 40)));
                        } else {
                            l.setBackground(r % 2 == 0 ? new Color(20, 14, 38) : new Color(26, 18, 48));
                            l.setForeground(Color.WHITE);
                            l.setFont(new Font("Consolas", Font.BOLD, 12));
                        }
                    }
                }
                return l;
            }
        });

        JPanel pnlTableTopStrip = new JPanel(new BorderLayout(8, 0));
        pnlTableTopStrip.setOpaque(false);
        pnlTableTopStrip.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));

        lblTableDossierTag = new JLabel("● SYSTEM MATRIX // WORKSHEET DATA");
        lblTableDossierTag.setFont(new Font("Consolas", Font.BOLD, 10));
        lblTableDossierTag.setForeground(RetroTheme.ACCENT_CYAN);
        pnlTableTopStrip.add(lblTableDossierTag, BorderLayout.WEST);

        lblTableVaultAssets = new JLabel("");
        lblTableVaultAssets.setFont(new Font("Consolas", Font.BOLD, 10));
        lblTableVaultAssets.setForeground(RetroTheme.ACCENT_YELLOW);

        // Mario Policy Switch Strip (FIFO / LRU / OPTIMAL)
        pnlMarioPolicyStrip = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        pnlMarioPolicyStrip.setOpaque(false);
        JLabel lblPol = new JLabel("POLICY:");
        lblPol.setFont(new Font("Consolas", Font.BOLD, 9));
        lblPol.setForeground(RetroTheme.TEXT_DIM);
        pnlMarioPolicyStrip.add(lblPol);

        btnPolicyFifo = createPolicyBtn("FIFO", "🍄 FIFO");
        btnPolicyLru = createPolicyBtn("LRU", "🐢 LRU");
        btnPolicyOpt = createPolicyBtn("OPTIMAL", "🌟 OPTIMAL");
        pnlMarioPolicyStrip.add(btnPolicyFifo);
        pnlMarioPolicyStrip.add(btnPolicyLru);
        pnlMarioPolicyStrip.add(btnPolicyOpt);

        JLabel lblCapTag = new JLabel(" [3 PIPES] ");
        lblCapTag.setFont(new Font("Consolas", Font.BOLD, 9));
        lblCapTag.setForeground(RetroTheme.ACCENT_YELLOW);
        pnlMarioPolicyStrip.add(lblCapTag);

        // Cyber Crane Policy Switch Strip (FIRST FIT / BEST FIT / NEXT FIT)
        pnlCranePolicyStrip = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        pnlCranePolicyStrip.setOpaque(false);
        JLabel lblCranePol = new JLabel("POLICY:");
        lblCranePol.setFont(new Font("Consolas", Font.BOLD, 9));
        lblCranePol.setForeground(RetroTheme.TEXT_DIM);
        pnlCranePolicyStrip.add(lblCranePol);

        btnPolicyFirstFit = createCranePolicyBtn("FIRST FIT", "🚀 FIRST FIT");
        btnPolicyBestFit = createCranePolicyBtn("BEST FIT", "🎯 BEST FIT");
        btnPolicyNextFit = createCranePolicyBtn("NEXT FIT", "🔄 NEXT FIT");
        pnlCranePolicyStrip.add(btnPolicyFirstFit);
        pnlCranePolicyStrip.add(btnPolicyBestFit);
        pnlCranePolicyStrip.add(btnPolicyNextFit);

        JLabel lblCraneSockets = new JLabel(" [S1:100K|S2:500K|S3:200K|S4:300K|S5:600K] ");
        lblCraneSockets.setFont(new Font("Consolas", Font.BOLD, 9));
        lblCraneSockets.setForeground(RetroTheme.ACCENT_YELLOW);
        pnlCranePolicyStrip.add(lblCraneSockets);

        // Pac-Man Disk Seek Policy Strip (FCFS / SSTF / SCAN / C-SCAN / LOOK / C-LOOK)
        pnlPacmanPolicyStrip = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        pnlPacmanPolicyStrip.setOpaque(false);
        JLabel lblPacmanPol = new JLabel("POLICY:");
        lblPacmanPol.setFont(new Font("Consolas", Font.BOLD, 9));
        lblPacmanPol.setForeground(RetroTheme.TEXT_DIM);
        pnlPacmanPolicyStrip.add(lblPacmanPol);

        btnPolicyFcfs = createPacmanPolicyBtn("FCFS", "🟡 FCFS");
        btnPolicySstf = createPacmanPolicyBtn("SSTF", "🎯 SSTF");
        btnPolicyScan = createPacmanPolicyBtn("SCAN", "🛗 SCAN");
        btnPolicyCScan = createPacmanPolicyBtn("C-SCAN", "🔄 C-SCAN");
        btnPolicyLook = createPacmanPolicyBtn("LOOK", "👁️ LOOK");
        btnPolicyCLook = createPacmanPolicyBtn("C-LOOK", "🔁 C-LOOK");
        pnlPacmanPolicyStrip.add(btnPolicyFcfs);
        pnlPacmanPolicyStrip.add(btnPolicySstf);
        pnlPacmanPolicyStrip.add(btnPolicyScan);
        pnlPacmanPolicyStrip.add(btnPolicyCScan);
        pnlPacmanPolicyStrip.add(btnPolicyLook);
        pnlPacmanPolicyStrip.add(btnPolicyCLook);

        JLabel lblPacmanSpecs = new JLabel(" [HEAD: 53 | DIR: UP (->) | 0-199] ");
        lblPacmanSpecs.setFont(new Font("Consolas", Font.BOLD, 9));
        lblPacmanSpecs.setForeground(RetroTheme.ACCENT_YELLOW);
        pnlPacmanPolicyStrip.add(lblPacmanSpecs);

        // Overcooked Kitchen Concurrency PMU Strip
        pnlKitchenStrip = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        pnlKitchenStrip.setOpaque(false);
        JLabel lblKitchenTag = new JLabel(" [ 👨‍🍳 KITCHEN CPU PMU ] ");
        lblKitchenTag.setFont(new Font("Consolas", Font.BOLD, 9));
        lblKitchenTag.setForeground(RetroTheme.ACCENT_PINK);
        JLabel lblKitchenMode = new JLabel(" [ MULTIPROGRAMMING ] ");
        lblKitchenMode.setFont(new Font("Consolas", Font.BOLD, 9));
        lblKitchenMode.setForeground(RetroTheme.ACCENT_CYAN);
        JLabel lblKitchenSlicing = new JLabel(" [ 100% CPU // 1/N_j SLICE ] ");
        lblKitchenSlicing.setFont(new Font("Consolas", Font.BOLD, 9));
        lblKitchenSlicing.setForeground(RetroTheme.ACCENT_YELLOW);
        pnlKitchenStrip.add(lblKitchenTag);
        pnlKitchenStrip.add(lblKitchenMode);
        pnlKitchenStrip.add(lblKitchenSlicing);

        JPanel pnlEastTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pnlEastTop.setOpaque(false);
        pnlEastTop.add(lblTableVaultAssets);
        pnlEastTop.add(pnlMarioPolicyStrip);
        pnlEastTop.add(pnlCranePolicyStrip);
        pnlEastTop.add(pnlPacmanPolicyStrip);
        pnlEastTop.add(pnlKitchenStrip);
        pnlTableTopStrip.add(pnlEastTop, BorderLayout.EAST);

        tableCard.add(pnlTableTopStrip, BorderLayout.NORTH);

        scrollTable = RetroTheme.createRetroScroll(cipherTable);
        scrollTable.setBorder(BorderFactory.createLineBorder(new Color(55, 42, 85), 1));
        tableCard.add(scrollTable, BorderLayout.CENTER);

        leftPanel.add(tableCard, BorderLayout.NORTH);

        // ── CENTER: Hero Interactive Step Inspector & Live Formula Workbench ──
        leftPanel.add(createStepInspectorWorkbenchPanel(), BorderLayout.CENTER);
        return leftPanel;
    }

    private JPanel createStepInspectorWorkbenchPanel() {
        JPanel bench = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color activeTheme = MOD_COLORS[currentModuleIdx];

                GradientPaint gp = new GradientPaint(0, 0, new Color(18, 13, 34, 252),
                        0, getHeight(), new Color(10, 7, 22, 255));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                g2.setColor(new Color(activeTheme.getRed(), activeTheme.getGreen(), activeTheme.getBlue(), 90));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                g2.setColor(activeTheme);
                int bl = 8;
                g2.drawLine(2, 2, 2 + bl, 2); g2.drawLine(2, 2, 2, 2 + bl);
                g2.drawLine(getWidth() - 3, 2, getWidth() - 3 - bl, 2); g2.drawLine(getWidth() - 3, 2, getWidth() - 3, 2 + bl);
                g2.drawLine(2, getHeight() - 3, 2 + bl, getHeight() - 3); g2.drawLine(2, getHeight() - 3, 2, getHeight() - 3 - bl);
                g2.drawLine(getWidth() - 3, getHeight() - 3, getWidth() - 3 - bl, getHeight() - 3); g2.drawLine(getWidth() - 3, getHeight() - 3, getWidth() - 3, getHeight() - 3 - bl);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        bench.setOpaque(false);
        bench.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // 1. TOP HEADER: Title + Dynamic Selected Target Badge
        JPanel headerRow = new JPanel(new BorderLayout(10, 0));
        headerRow.setOpaque(false);

        JLabel lTitle = new JLabel("⚡ TACTICAL STEP TUTOR // FORMULA & TELEMETRY WORKBENCH");
        lTitle.setFont(new Font("Consolas", Font.BOLD, 11));
        lTitle.setForeground(RetroTheme.ACCENT_YELLOW);
        headerRow.add(lTitle, BorderLayout.WEST);

        lblStepTargetTag = new JLabel(" TARGET: SELECT ANY [ ? ] CELL TO INSPECT ");
        lblStepTargetTag.setFont(new Font("Consolas", Font.BOLD, 10));
        lblStepTargetTag.setForeground(RetroTheme.BG_DARK);
        lblStepTargetTag.setBackground(RetroTheme.ACCENT_CYAN);
        lblStepTargetTag.setOpaque(true);
        lblStepTargetTag.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        headerRow.add(lblStepTargetTag, BorderLayout.EAST);
        bench.add(headerRow, BorderLayout.NORTH);

        // 2. CENTER: 2-Column Telemetry Cockpit (Left: Formula + Scratchpad | Right: Sequence Challenge)
        JPanel bodySplit = new JPanel(new GridLayout(1, 2, 8, 0));
        bodySplit.setOpaque(false);

        // --- Column 1: Formula Specification & Quick Math Register Co-Processor ---
        JPanel leftCol = new JPanel(new BorderLayout(0, 4));
        leftCol.setOpaque(false);

        JPanel p1 = new JPanel(new BorderLayout(0, 2));
        p1.setOpaque(false);
        JLabel lHead1 = new JLabel("📐 FORMULA SPECIFICATION & LOGIC CO-PROCESSOR:");
        lHead1.setFont(new Font("Consolas", Font.BOLD, 10));
        lHead1.setForeground(RetroTheme.ACCENT_CYAN);
        p1.add(lHead1, BorderLayout.NORTH);

        lblStepFormulaBox = new JLabel("<html><b style='color:#05D9E8;'>SELECT A CELL:</b> Click any glowing <b style='color:#FFD700;'>[ ? ]</b> cell in the worksheet to inspect its calculation rule.</html>");
        lblStepFormulaBox.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblStepFormulaBox.setForeground(Color.WHITE);
        lblStepFormulaBox.setBackground(new Color(24, 18, 48));
        lblStepFormulaBox.setOpaque(true);
        lblStepFormulaBox.setVerticalAlignment(SwingConstants.TOP);
        lblStepFormulaBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 48, 100), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        p1.add(lblStepFormulaBox, BorderLayout.CENTER);
        leftCol.add(p1, BorderLayout.CENTER);

        // Quick Math Register / Scratchpad with Shortcut Bar
        JPanel p2 = new JPanel(new BorderLayout(0, 2));
        p2.setOpaque(false);
        JLabel lHead2 = new JLabel("🔢 QUICK MATH REGISTER / FAST SCRATCHPAD:");
        lHead2.setFont(new Font("Consolas", Font.BOLD, 10));
        lHead2.setForeground(RetroTheme.ACCENT_GREEN);
        p2.add(lHead2, BorderLayout.NORTH);

        JPanel scratchContainer = new JPanel(new BorderLayout(0, 2));
        scratchContainer.setOpaque(true);
        scratchContainer.setBackground(new Color(16, 22, 38));
        scratchContainer.setBorder(BorderFactory.createLineBorder(new Color(40, 75, 95), 1));

        cardsInputMode = new CardLayout();
        pnlInputSwitch = new JPanel(cardsInputMode);
        pnlInputSwitch.setOpaque(false);

        // Card 1: Calculator input row (for typing numeric answers)
        pnlMathCalcRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        pnlMathCalcRow.setOpaque(false);

        txtScratchA.setFont(new Font("Consolas", Font.BOLD, 11));
        txtScratchA.setBackground(new Color(10, 14, 26));
        txtScratchA.setForeground(Color.WHITE);
        txtScratchA.setCaretColor(Color.CYAN);

        cmbScratchOp.setFont(new Font("Consolas", Font.BOLD, 11));
        cmbScratchOp.setBackground(new Color(25, 30, 50));
        cmbScratchOp.setForeground(RetroTheme.ACCENT_YELLOW);

        txtScratchB.setFont(new Font("Consolas", Font.BOLD, 11));
        txtScratchB.setBackground(new Color(10, 14, 26));
        txtScratchB.setForeground(Color.WHITE);
        txtScratchB.setCaretColor(Color.CYAN);

        // Real-time synchronization listeners: recalculate dynamically as the user types!
        javax.swing.event.DocumentListener scratchDocListener = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { if (!updatingScratchpad) computeScratchpad(false); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { if (!updatingScratchpad) computeScratchpad(false); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { if (!updatingScratchpad) computeScratchpad(false); }
        };
        txtScratchA.getDocument().addDocumentListener(scratchDocListener);
        txtScratchB.getDocument().addDocumentListener(scratchDocListener);
        cmbScratchOp.addActionListener(e -> { if (!updatingScratchpad) computeScratchpad(false); });

        JButton btnCalc = createElevatedArcadeBtn("=", new Color(0, 150, 180), Color.WHITE, 20);
        btnCalc.addActionListener(e -> computeScratchpad(true));

        lblScratchResult.setFont(new Font("Consolas", Font.BOLD, 11));
        lblScratchResult.setForeground(RetroTheme.ACCENT_GREEN);
        lblScratchResult.setPreferredSize(new Dimension(55, 20));

        JButton btnInsert = createElevatedArcadeBtn("⚡ INSERT INTO CELL", new Color(0, 160, 80), Color.WHITE, 20);
        btnInsert.addActionListener(e -> insertScratchIntoCell());

        pnlMathCalcRow.add(txtScratchA);
        pnlMathCalcRow.add(cmbScratchOp);
        pnlMathCalcRow.add(txtScratchB);
        pnlMathCalcRow.add(btnCalc);
        pnlMathCalcRow.add(lblScratchResult);
        pnlMathCalcRow.add(btnInsert);
        pnlInputSwitch.add(pnlMathCalcRow, "MATH");

        // Card 2: Interactive Word Selection Row (for selecting words with 1 click)
        pnlWordSelectRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        pnlWordSelectRow.setOpaque(false);
        JScrollPane scrollWords = new JScrollPane(pnlWordSelectRow, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollWords.setOpaque(false);
        scrollWords.getViewport().setOpaque(false);
        scrollWords.setBorder(BorderFactory.createEmptyBorder());
        scrollWords.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 4));
        scrollWords.setPreferredSize(new Dimension(450, 32));
        pnlInputSwitch.add(scrollWords, "WORDS");

        scratchContainer.add(pnlInputSwitch, BorderLayout.NORTH);

        // Row 2: Fast calculation shortcut tools (contextual to active algorithm)
        JPanel scratchRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        scratchRow2.setOpaque(false);

        JLabel lShortcuts = new JLabel("CALC TOOLS:");
        lShortcuts.setFont(new Font("Consolas", Font.BOLD, 9));
        lShortcuts.setForeground(RetroTheme.TEXT_DIM);
        scratchRow2.add(lShortcuts);

        btnTool1 = createElevatedArcadeBtn("+ BT", new Color(40, 50, 80), Color.WHITE, 18);
        btnTool1.setFont(new Font("Consolas", Font.BOLD, 9));
        btnTool1.addActionListener(e -> onTool1Clicked());
        scratchRow2.add(btnTool1);

        btnTool2 = createElevatedArcadeBtn("- AT", new Color(50, 40, 80), Color.WHITE, 18);
        btnTool2.setFont(new Font("Consolas", Font.BOLD, 9));
        btnTool2.addActionListener(e -> onTool2Clicked());
        scratchRow2.add(btnTool2);

        btnTool3 = createElevatedArcadeBtn("÷ N (AVG)", new Color(60, 50, 30), Color.WHITE, 18);
        btnTool3.setFont(new Font("Consolas", Font.BOLD, 9));
        btnTool3.addActionListener(e -> onTool3Clicked());
        scratchRow2.add(btnTool3);

        btnNextBlank = createElevatedArcadeBtn("➔ NEXT [?]", new Color(30, 80, 70), Color.WHITE, 18);
        btnNextBlank.setToolTipText("Advance cursor to next blank cell in table");
        btnNextBlank.setFont(new Font("Consolas", Font.BOLD, 9));
        btnNextBlank.addActionListener(e -> selectNextBlankCell(cipherTable.getSelectedRow(), cipherTable.getSelectedColumn()));
        scratchRow2.add(btnNextBlank);

        scratchContainer.add(scratchRow2, BorderLayout.SOUTH);
        p2.add(scratchContainer, BorderLayout.CENTER);
        leftCol.add(p2, BorderLayout.SOUTH);
        bodySplit.add(leftCol);

        // --- Column 2: Interactive Sequence Order Challenge Deck ---
        JPanel pSeqCard = new JPanel(new BorderLayout(0, 3));
        pSeqCard.setOpaque(false);

        JPanel seqHeader = new JPanel(new GridLayout(2, 1, 0, 2));
        seqHeader.setOpaque(false);
        lblSequenceTitle.setFont(new Font("Consolas", Font.BOLD, 10));
        lblSequenceTitle.setForeground(RetroTheme.ACCENT_PINK);
        seqHeader.add(lblSequenceTitle);

        lblSequencePrompt.setFont(new Font("Consolas", Font.BOLD, 10));
        lblSequencePrompt.setForeground(Color.WHITE);
        seqHeader.add(lblSequencePrompt);
        pSeqCard.add(seqHeader, BorderLayout.NORTH);

        JPanel tapeBox = new JPanel(new BorderLayout(0, 3));
        tapeBox.setOpaque(true);
        tapeBox.setBackground(new Color(18, 14, 38));
        tapeBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 45, 110), 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        lblSequenceTape.setFont(new Font("Consolas", Font.BOLD, 10));
        lblSequenceTape.setForeground(RetroTheme.ACCENT_CYAN);
        lblSequenceTape.setPreferredSize(new Dimension(320, 22));
        tapeBox.add(lblSequenceTape, BorderLayout.NORTH);

        JPanel tapeCenter = new JPanel(new BorderLayout(0, 2));
        tapeCenter.setOpaque(false);
        tapeCenter.setBackground(new Color(18, 14, 38));
        pnlSequenceTokens.setOpaque(false);
        pnlSequenceTokens.setBackground(new Color(18, 14, 38));
        tapeCenter.add(pnlSequenceTokens, BorderLayout.NORTH);

        lblSequenceHint = new JLabel("<html><font color='#FFD700'>SEQUENCE ORDER:</font> <font color='#FFFFFF'>Click tokens in correct execution sequence. Use ⌫ POP to undo.</font></html>");
        lblSequenceHint.setFont(new Font("Consolas", Font.BOLD, 10));
        tapeCenter.add(lblSequenceHint, BorderLayout.CENTER);

        tapeBox.add(tapeCenter, BorderLayout.CENTER);
        pSeqCard.add(tapeBox, BorderLayout.CENTER);

        JPanel seqActionRow = new JPanel(new GridLayout(2, 1, 0, 2));
        seqActionRow.setOpaque(false);

        JPanel feedbackRow = new JPanel(new BorderLayout());
        feedbackRow.setOpaque(false);
        lblSequenceFeedback.setFont(new Font("Consolas", Font.BOLD, 10));
        lblSequenceFeedback.setForeground(new Color(200, 220, 255));
        feedbackRow.add(lblSequenceFeedback, BorderLayout.WEST);

        JLabel lBonusTag = new JLabel("[ +50 BONUS XP ]");
        lBonusTag.setFont(new Font("Consolas", Font.BOLD, 9));
        lBonusTag.setForeground(RetroTheme.ACCENT_YELLOW);
        feedbackRow.add(lBonusTag, BorderLayout.EAST);
        seqActionRow.add(feedbackRow);

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnBox.setOpaque(false);

        JButton btnAuditSeq = createElevatedArcadeBtn("⚡ AUDIT", new Color(0, 160, 90), Color.WHITE, 20);
        btnAuditSeq.setToolTipText("Verify sequence dispatch order (+50 XP)");
        btnAuditSeq.addActionListener(e -> auditSequence());
        btnBox.add(btnAuditSeq);

        JButton btnPopSeq = createElevatedArcadeBtn("⌫ POP", new Color(180, 110, 20), Color.WHITE, 20);
        btnPopSeq.setToolTipText("Remove last token from sequence tape");
        btnPopSeq.addActionListener(e -> popSequenceToken());
        btnBox.add(btnPopSeq);

        JButton btnClearSeq = createElevatedArcadeBtn("↺ RESET", new Color(170, 50, 60), Color.WHITE, 20);
        btnClearSeq.setToolTipText("Reset sequence tape and re-enable all tokens");
        btnClearSeq.addActionListener(e -> clearSequence());
        btnBox.add(btnClearSeq);

        seqActionRow.add(btnBox);
        pSeqCard.add(seqActionRow, BorderLayout.SOUTH);
        bodySplit.add(pSeqCard);

        bench.add(bodySplit, BorderLayout.CENTER);

        // 3. BOTTOM HUD: Segmented Progress Bar & XP Multiplier Strip
        JPanel hudRow = new JPanel(new BorderLayout(10, 0));
        hudRow.setOpaque(false);
        hudRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        lblProgressText = new JLabel("CALCULATION PROGRESS: 0/4 (0%)");
        lblProgressText.setFont(new Font("Consolas", Font.BOLD, 10));
        lblProgressText.setForeground(new Color(220, 230, 255));
        lblProgressText.setPreferredSize(new Dimension(220, 16));
        hudRow.add(lblProgressText, BorderLayout.WEST);

        cipherProgressBar = new JProgressBar(0, 100);
        cipherProgressBar.setValue(0);
        cipherProgressBar.setPreferredSize(new Dimension(140, 8));
        cipherProgressBar.setBackground(new Color(25, 18, 45));
        cipherProgressBar.setForeground(RetroTheme.ACCENT_CYAN);
        cipherProgressBar.setBorder(BorderFactory.createLineBorder(new Color(60, 45, 95), 1));
        hudRow.add(cipherProgressBar, BorderLayout.CENTER);

        lblStatusBadge = new JLabel(" [ 🔒 CIPHER ENCRYPTION LOCKED ] ");
        lblStatusBadge.setFont(new Font("Consolas", Font.BOLD, 10));
        lblStatusBadge.setForeground(RetroTheme.ACCENT_YELLOW);
        hudRow.add(lblStatusBadge, BorderLayout.EAST);

        bench.add(hudRow, BorderLayout.SOUTH);
        return bench;
    }

    private void addSequenceToken(String token) {
        boolean allowsDupes = token.equals("HIT") || token.equals("FAULT") || token.equals("VALID") || token.equals("TRAP");
        if (!allowsDupes && currentSequenceList.contains(token)) {
            showToast("TOKEN " + token + " ALREADY ON TAPE");
            return;
        }
        currentSequenceList.add(token);
        JButton btn = tokenButtonMap.get(token);
        if (btn != null && !allowsDupes) {
            btn.setEnabled(false);
            btn.setText("✓ " + token);
            btn.setForeground(new Color(130, 130, 160));
        }
        updateSequenceTape();
        playBlip();
    }

    private void popSequenceToken() {
        if (!currentSequenceList.isEmpty()) {
            String removed = currentSequenceList.remove(currentSequenceList.size() - 1);
            JButton btn = tokenButtonMap.get(removed);
            if (btn != null) {
                btn.setEnabled(true);
                btn.setText("+ " + removed);
                btn.setForeground(Color.WHITE);
            }
            updateSequenceTape();
            playBlip();
        }
    }

    private void updateSequenceTape() {
        if (currentSequenceList.isEmpty()) {
            lblSequenceTape.setText("<html><font color='#8888aa'>[ TAPE EMPTY — CLICK TOKENS BELOW IN SEQUENCE ]</font></html>");
        } else {
            StringBuilder sb = new StringBuilder("<html><font color='#05D9E8'><b>");
            for (int i = 0; i < currentSequenceList.size(); i++) {
                if (i > 0) sb.append(" <font color='#FFD700'>➔</font> ");
                sb.append("<span style='background:#221845; color:#57FF3C; border:1px solid #05D9E8; padding:1px 4px;'>[").append(currentSequenceList.get(i)).append("]</span>");
            }
            sb.append("</b></font></html>");
            lblSequenceTape.setText(sb.toString());
        }
    }

    private void clearSequence() {
        currentSequenceList.clear();
        sequenceVerified = false;
        for (Map.Entry<String, JButton> entry : tokenButtonMap.entrySet()) {
            entry.getValue().setEnabled(true);
            entry.getValue().setText("+ " + entry.getKey());
            entry.getValue().setForeground(Color.WHITE);
        }
        updateSequenceTape();
        lblSequenceFeedback.setText("STATUS: Sequence cleared. Select tokens in order.");
        lblSequenceFeedback.setForeground(RetroTheme.TEXT_DIM);
        playBlip();
    }

    private void auditSequence() {
        if (activeQuestion == null || activeQuestion.correctSequence == null || activeQuestion.correctSequence.isEmpty()) return;
        if (currentSequenceList.isEmpty()) {
            lblSequenceFeedback.setText("⚠ Build a sequence first by clicking the tokens above!");
            lblSequenceFeedback.setForeground(RetroTheme.ACCENT_YELLOW);
            return;
        }

        String userSeq = String.join(" -> ", currentSequenceList);
        if (userSeq.equalsIgnoreCase(activeQuestion.correctSequence)) {
            sequenceVerified = true;
            AppState.addScore(50);
            if (lblXpScore != null) lblXpScore.setText("XP: " + AppState.getScoreFormatted());
            playVictorySound();
            lblSequenceFeedback.setText("✔ SEQUENCE VALIDATED! +50 XP AWARDED [DISPATCH 100%]");
            lblSequenceFeedback.setForeground(RetroTheme.ACCENT_GREEN);
            showToast("★ SEQUENCE AUDIT PASSED! +50 XP");
        } else {
            playBuzzerSound();
            lblSequenceFeedback.setText("✘ SEQUENCE MISMATCH: Check arrival order or algorithm rules!");
            lblSequenceFeedback.setForeground(RetroTheme.ACCENT_PINK);
            showToast("⚠ SEQUENCE REJECTED: Incorrect order!");
        }
    }

    private void loadSequenceChallenge() {
        currentSequenceList.clear();
        sequenceVerified = false;
        tokenButtonMap.clear();
        if (lblSequenceTitle != null && activeQuestion != null) {
            lblSequenceTitle.setText("⚡ " + activeQuestion.sequenceTitle);
        }
        if (lblSequencePrompt != null && activeQuestion != null) {
            lblSequencePrompt.setText("<html>" + activeQuestion.sequencePrompt + "</html>");
        }
        updateSequenceTape();
        if (lblSequenceHint != null) {
            String hintText;
            switch (currentModuleIdx) {
                case 1:
                    hintText = "<font color='#FFD700'>SAFETY ORDER:</font> <font color='#FFFFFF'>Click clients in safe sequence (Need &lt;= Work). Use ⌫ POP to undo.</font>";
                    break;
                case 2:
                    hintText = "<font color='#57FF3C'>MMU ORDER:</font> <font color='#FFFFFF'>Click address references in DRAM translation order.</font>";
                    break;
                case 3:
                    String pol = (activeQuestion != null && activeQuestion.policy != null) ? activeQuestion.policy : "FIFO";
                    hintText = "<font color='#FF5757'>" + pol + " ORDER:</font> <font color='#FFFFFF'>Construct cache stream (HIT or FAULT). Use ⌫ POP to undo.</font>";
                    break;
                case 4:
                    hintText = "<font color='#FF5757'>SEGMENTATION:</font> <font color='#FFFFFF'>Click memory segments in translation order.</font>";
                    break;
                case 5:
                    String cPol = (activeQuestion != null && activeQuestion.policy != null) ? activeQuestion.policy : "FIRST FIT";
                    hintText = "<font color='#FFD700'>" + cPol + " ORDER:</font> <font color='#FFFFFF'>Click sockets in process arrival order. Use ⌫ POP to undo.</font>";
                    break;
                case 6:
                    String dPol = (activeQuestion != null && activeQuestion.policy != null) ? activeQuestion.policy : "FCFS";
                    hintText = "<font color='#05D9E8'>" + dPol + " ORDER:</font> <font color='#FFFFFF'>Click cylinders in " + dPol + " traversal order. Use ⌫ POP to undo.</font>";
                    break;
                case 7:
                    hintText = "<font color='#FF5757'>CONCURRENCY:</font> <font color='#FFFFFF'>Click jobs in order of completion on the CPU. Use ⌫ POP to undo.</font>";
                    break;
                default:
                    hintText = "<font color='#FFD700'>DISPATCH ORDER:</font> <font color='#FFFFFF'>Click tokens in arrival order (AT). Use ⌫ POP to undo.</font>";
                    break;
            }
            lblSequenceHint.setText("<html>" + hintText + "</html>");
        }
        if (lblSequenceFeedback != null) {
            lblSequenceFeedback.setText("STATUS: Click tokens below.");
            lblSequenceFeedback.setForeground(new Color(200, 220, 255));
        }
        if (pnlSequenceTokens != null && activeQuestion != null && activeQuestion.sequenceTokens != null) {
            pnlSequenceTokens.removeAll();
            for (String tok : activeQuestion.sequenceTokens) {
                JButton btnTok = createElevatedArcadeBtn("+ " + tok, new Color(40, 30, 75), Color.WHITE, 20);
                btnTok.setFont(new Font("Consolas", Font.BOLD, 9));
                btnTok.addActionListener(e -> addSequenceToken(tok));
                tokenButtonMap.put(tok, btnTok);
                pnlSequenceTokens.add(btnTok);
            }
            pnlSequenceTokens.revalidate();
            pnlSequenceTokens.repaint();
        }
    }

    private void computeScratchpad() {
        computeScratchpad(true);
    }

    private void computeScratchpad(boolean playAudio) {
        try {
            String aStr = txtScratchA.getText().trim();
            String bStr = txtScratchB.getText().trim();
            if (aStr.isEmpty() || bStr.isEmpty()) {
                lblScratchResult.setText("= ...");
                return;
            }
            double a = Double.parseDouble(aStr);
            double b = Double.parseDouble(bStr);
            String op = (String) cmbScratchOp.getSelectedItem();
            double res = 0.0;
            if ("+".equals(op)) res = a + b;
            else if ("-".equals(op)) res = a - b;
            else if ("×".equals(op)) res = a * b;
            else if ("÷".equals(op)) res = (b != 0) ? (a / b) : 0.0;
            else if ("%".equals(op)) res = (b != 0) ? (a % b) : 0.0;

            String resStr = (Math.abs(res - Math.round(res)) < 0.0001)
                    ? String.format(Locale.US, "%.0f", res)
                    : String.format(Locale.US, "%.2f", res);
            lblScratchResult.setText("= " + resStr);
            if (playAudio) playBlip();
        } catch (Exception ex) {
            lblScratchResult.setText("= ...");
        }
    }

    private void quickApplyBurstTime() {
        int r = cipherTable.getSelectedRow();
        if (r >= 0 && activeQuestion != null && activeQuestion.worksheetData != null && r < activeQuestion.worksheetData.length) {
            Double bt = getTableCellDouble(r, 2);
            if (bt != null) {
                updatingScratchpad = true;
                try {
                    cmbScratchOp.setSelectedItem("+");
                    txtScratchB.setText(formatDouble(bt));
                } finally {
                    updatingScratchpad = false;
                }
                computeScratchpad(true);
            }
        }
    }

    private void quickApplyArrivalTime() {
        int r = cipherTable.getSelectedRow();
        if (r >= 0 && activeQuestion != null && activeQuestion.worksheetData != null && r < activeQuestion.worksheetData.length) {
            Double at = getTableCellDouble(r, 1);
            if (at != null) {
                updatingScratchpad = true;
                try {
                    cmbScratchOp.setSelectedItem("-");
                    txtScratchB.setText(formatDouble(at));
                } finally {
                    updatingScratchpad = false;
                }
                computeScratchpad(true);
            }
        }
    }

    private void quickApplyAverageDiv() {
        if (activeQuestion != null && activeQuestion.worksheetData != null) {
            int n = Math.max(1, activeQuestion.worksheetData.length - 1);
            int c = cipherTable.getSelectedColumn();
            double sum = 0.0;
            int count = 0;
            if (c == 5 || c == 6) {
                for (int i = 0; i < n; i++) {
                    Double v = getTableCellDouble(i, c);
                    if (v != null) {
                        sum += v;
                        count++;
                    }
                }
            }
            updatingScratchpad = true;
            try {
                if (count > 0) {
                    txtScratchA.setText(formatDouble(sum));
                }
                cmbScratchOp.setSelectedItem("÷");
                txtScratchB.setText(String.valueOf(n));
            } finally {
                updatingScratchpad = false;
            }
            computeScratchpad(true);
        }
    }

    private boolean isCellUnsolvedBlank(int r, int c) {
        if (activeQuestion == null || r >= activeQuestion.worksheetData.length || c >= activeQuestion.worksheetData[r].length) return false;
        if (!activeQuestion.worksheetData[r][c].trim().equals("?")) return false;
        if (tableModel == null || r >= tableModel.getRowCount() || c >= tableModel.getColumnCount()) return false;
        Object val = tableModel.getValueAt(r, c);
        if (val == null) return true;
        String s = val.toString().trim();
        return s.isEmpty() || s.equals("?") || s.equals("[ ? ]") || s.equals("—");
    }

    private void selectNextBlankCell(int currentR, int currentC) {
        if (cipherTable == null || activeQuestion == null || activeQuestion.worksheetData == null) return;
        int rows = activeQuestion.worksheetData.length;
        int cols = activeQuestion.columnHeaders.length;

        // Pass 1: find next cell that is an editable blank AND still empty/unsolved in tableModel
        for (int r = currentR; r < rows; r++) {
            int startCol = (r == currentR) ? currentC + 1 : 1;
            for (int c = startCol; c < cols; c++) {
                if (isCellUnsolvedBlank(r, c)) {
                    cipherTable.changeSelection(r, c, false, false);
                    updateContextualInspector();
                    return;
                }
            }
        }
        for (int r = 0; r <= currentR; r++) {
            int endCol = (r == currentR) ? currentC : cols;
            for (int c = 1; c < endCol; c++) {
                if (isCellUnsolvedBlank(r, c)) {
                    cipherTable.changeSelection(r, c, false, false);
                    updateContextualInspector();
                    return;
                }
            }
        }
        // Pass 2: fallback to any editable blank if all are filled
        for (int r = 0; r < rows; r++) {
            for (int c = 1; c < cols; c++) {
                if (activeQuestion.worksheetData[r][c].trim().equals("?")) {
                    cipherTable.changeSelection(r, c, false, false);
                    updateContextualInspector();
                    return;
                }
            }
        }
    }

    private void insertScratchIntoCell() {
        String resText = lblScratchResult.getText().replace("=", "").trim();
        if (resText.isEmpty() || resText.equals("ERR") || resText.equals("...")) return;

        int r = cipherTable.getSelectedRow();
        int c = cipherTable.getSelectedColumn();
        if (r >= 0 && c >= 0 && cipherTable.isCellEditable(r, c)) {
            cipherTable.setValueAt(resText, r, c);
            playBlip();
            updateProgressMeter();
        updateStageProgressStrip();
            if (pnlCategoryVisualizer != null) pnlCategoryVisualizer.repaint();
            showToast("INSERTED " + resText + " INTO CELL [" + activeQuestion.worksheetData[r][0] + "]");
            selectNextBlankCell(r, c);
        } else {
            showToast("SELECT A BLANK CELL IN THE TABLE FIRST");
        }
    }

    private void setWorkbenchData(String formula, String derivation, String concept, String tip) {
        if (lblStepFormulaBox != null) {
            String cleanFormula = formula != null ? formula : "";
            // Strip any 3rd explanatory paragraph so only the title and exact formula remain
            String[] fLines = cleanFormula.split("(?i)<br>");
            if (fLines.length > 2) {
                cleanFormula = fLines[0] + "<br>" + fLines[1];
            }

            String cleanTip = (tip != null && !tip.isEmpty())
                    ? tip.replaceAll("(?i)<br>", " ").replaceAll("(?i)<b>tutor tip:</b>", "").replaceAll("(?i)tutor tip:", "").trim()
                    : (derivation != null ? derivation.replaceAll("(?i)<br>", " ").replaceAll("(?i)<b>derivation:</b>", "").replaceAll("(?i)derivation:", "").trim() : "");

            lblStepFormulaBox.setText("<html><body style='width: 250px; font-family: Consolas;'>"
                    + "<div style='color: #05D9E8; font-weight: bold; font-size: 11px;'>" + cleanFormula + "</div>"
                    + (cleanTip.isEmpty() ? "" : "<div style='color: #57FF3C; font-size: 10px; margin-top: 4px;'>💡 <b>HINT:</b> " + cleanTip + "</div>")
                    + "</body></html>");
        }
        if (lblInspectorFormula != null) {
            lblInspectorFormula.setText(formula != null ? formula.replaceAll("<[^>]*>", "") : "");
        }
        if (lblInspectorVars != null) {
            lblInspectorVars.setText((tip != null ? tip : concept != null ? concept : "").replaceAll("<[^>]*>", ""));
        }
    }

    /**
     * Dynamically updates the Contextual Step Inspector based on the user's selected cell!
     */
    private void updateContextualInspector() {
        int r = cipherTable.getSelectedRow();
        int c = cipherTable.getSelectedColumn();
        if (r < 0 || c < 0 || activeQuestion == null || activeQuestion.worksheetData == null) return;
        if (r >= activeQuestion.worksheetData.length || c >= activeQuestion.columnHeaders.length) return;

        String rowName = activeQuestion.worksheetData[r][0];
        String colName = activeQuestion.columnHeaders[c];
        lblInspectorTarget.setText("SELECTED: " + rowName + " [" + colName + "]");
        if (lblStepTargetTag != null) {
            lblStepTargetTag.setText(" TARGET: " + rowName + " ▸ " + colName + " ");
        }

        switch (currentModuleIdx) {
            case 0: // CPU Scheduling FCFS
                if (rowName.contains("AVERAGE") || rowName.contains("AVG")) {
                    int waveCount = Math.max(1, activeQuestion.worksheetData.length - 1);
                    if (colName.contains("WAIT")) {
                        setWorkbenchData(
                                "<b>AVERAGE WAITING TIME (AVG WT):</b><br><font color='#FFD700'>AVG WT = (Σ Waiting Times) / " + waveCount + "</font><br>Mean ready queue waiting latency across all " + waveCount + " waves.",
                                "<b>DERIVATION:</b><br><font color='#57FF3C'>Sum of WT column across W1..W" + waveCount + " divided by " + waveCount + " waves.</font>",
                                "<b>OS PRINCIPLE:</b><br>Average waiting time reflects CPU dispatch responsiveness. Minimizing Avg WT is critical for interactive scheduling workloads.",
                                "<b>TUTOR TIP:</b><br>Sum all the numbers in the WAIT (WT) column above and divide by " + waveCount + ". Enter with 2 decimals."
                        );
                    } else if (colName.contains("TURN")) {
                        setWorkbenchData(
                                "<b>AVERAGE TURNAROUND TIME (AVG TAT):</b><br><font color='#FFD700'>AVG TAT = (Σ Turnaround Times) / " + waveCount + "</font><br>Mean lifecycle duration from arrival to completion.",
                                "<b>DERIVATION:</b><br><font color='#57FF3C'>Sum of TAT column across W1..W" + waveCount + " divided by " + waveCount + " waves.</font>",
                                "<b>OS PRINCIPLE:</b><br>Average turnaround time measures overall system throughput and throughput satisfaction speed.",
                                "<b>TUTOR TIP:</b><br>Sum all numbers in the TURN (TAT) column above and divide by " + waveCount + ". Enter with 2 decimals."
                        );
                    } else {
                        setWorkbenchData(
                                "<b>SUMMARY ROW: OVERALL MEAN METRICS</b><br><font color='#FFD700'>Queue Aggregates</font>",
                                "<b>TARGETS:</b> Compute Average Waiting Time (AVG WT) and Average Turnaround Time (AVG TAT).",
                                "<b>OS PRINCIPLE:</b> The system evaluation engine summarizes batch performance over the entire scheduling workload.",
                                "<b>TUTOR TIP:</b> Click either [ ? ] in the AVERAGE row to inspect its formula."
                        );
                    }
                } else if (colName.contains("START")) {
                    setWorkbenchData(
                            "<b>START TIME (ST):</b><br><font color='#05D9E8'>ST = max(Arrival Time, prev Finish Time)</font><br>Timestamp when CPU is allocated to this wave.",
                            "<b>DERIVATION:</b><br><font color='#57FF3C'>For " + rowName + ": Check Arrival (AT). If first wave, ST = AT; else ST = max(AT, FT of prev wave).</font>",
                            "<b>OS PRINCIPLE:</b> In non-preemptive FCFS, if the core is idle upon arrival, execution begins immediately. If busy, the process waits in queue until the prior job completes.",
                            "<b>TUTOR TIP:</b> Look at the previous row's FINISH time and this row's ARRIVAL time. Choose whichever number is larger!"
                    );
                } else if (colName.contains("FINISH")) {
                    setWorkbenchData(
                            "<b>FINISH TIME (FT):</b><br><font color='#05D9E8'>FT = Start Time (ST) + Burst Time (BT)</font><br>Timestamp when process wave execution terminates.",
                            "<b>DERIVATION:</b><br><font color='#57FF3C'>For " + rowName + ": Add this wave's Burst Time (BT) to its computed Start Time (ST).</font>",
                            "<b>OS PRINCIPLE:</b> Non-preemptive CPU execution guarantees that once dispatched, the process runs uninterrupted until its full burst duration finishes.",
                            "<b>TUTOR TIP:</b> Take the START value and add the BURST value from this same row."
                    );
                } else if (colName.contains("WAIT")) {
                    setWorkbenchData(
                            "<b>WAITING TIME (WT):</b><br><font color='#05D9E8'>WT = Turnaround Time (TAT) - Burst Time (BT)</font><br>Also equivalent to: <font color='#FFD700'>WT = Start Time - Arrival Time</font>",
                            "<b>DERIVATION:</b><br><font color='#57FF3C'>For " + rowName + ": Total elapsed time spent in the queue before CPU execution.</font>",
                            "<b>OS PRINCIPLE:</b> Waiting time measures process queue latency caused by head-of-line convoy blocking and prior job execution.",
                            "<b>TUTOR TIP:</b> Either subtract BURST from TURNAROUND, or subtract ARRIVAL from START!"
                    );
                } else if (colName.contains("TURN")) {
                    setWorkbenchData(
                            "<b>TURNAROUND TIME (TAT):</b><br><font color='#05D9E8'>TAT = Finish Time (FT) - Arrival Time (AT)</font><br>Total lifetime in system from arrival to completion.",
                            "<b>DERIVATION:</b><br><font color='#57FF3C'>For " + rowName + ": Total duration elapsed between submission and termination.</font>",
                            "<b>OS PRINCIPLE:</b> Turnaround combines both waiting latency in the ready queue and active CPU execution time: <font color='#05D9E8'>TAT = WT + BT</font>.",
                            "<b>TUTOR TIP:</b> Subtract the ARRIVAL (AT) time from the FINISH (FT) time."
                    );
                } else {
                    setWorkbenchData(
                            "<b>GIVEN PARAMETER: " + colName + "</b><br><font color='#05D9E8'>Input Stream Specification</font>",
                            "<b>VALUE FOR " + rowName + ":</b> " + activeQuestion.worksheetData[r][c],
                            "<b>OS PRINCIPLE:</b> Pre-configured telemetry parameters dispatched by the operating system kernel.",
                            "<b>TUTOR TIP:</b> Use this given value to solve the missing [ ? ] cells in this row."
                    );
                }
                break;

            case 1: // Banker's Safety Algorithm
                if (colName.contains("NEED")) {
                    setWorkbenchData(
                            "<b>NEED MATRIX VECTOR (A B C):</b><br><font color='#FFD700'>Need[A B C] = Max Claim - Current Allocation</font><br>Resource units still required by " + rowName + " to complete.",
                            "<b>DERIVATION:</b><br><font color='#57FF3C'>Need[A]=Max[A]-Alloc[A],  Need[B]=Max[B]-Alloc[B],  Need[C]=Max[C]-Alloc[C]</font>",
                            "<b>OS PRINCIPLE:</b> The Banker's safety algorithm evaluates if remaining resource demands can be satisfied without entering an unsafe deadlock state.",
                            "<b>TUTOR TIP:</b> Subtract each resource number in Allocation from Max Claim. Enter space-separated (e.g. 7 4 3)."
                    );
                } else if (colName.contains("WORK") || colName.contains("VAULT")) {
                    setWorkbenchData(
                            "<b>WORK / AVAILABLE RESERVES (A B C):</b><br><font color='#05D9E8'>Work = Current Available Vault Reserves</font>",
                            "<b>DERIVATION:</b> Initial Work is [3, 3, 2]. When a client completes execution, it releases its allocation: <font color='#57FF3C'>Work = Work + Allocation</font>.",
                            "<b>OS PRINCIPLE:</b> The Work vector tracks system reserves available to allocate to waiting processes.",
                            "<b>TUTOR TIP:</b> Look at the current available pool. If previous client ran, add its allocation to the pool!"
                    );
                } else if (colName.contains("SATISFY")) {
                    setWorkbenchData(
                            "<b>SAFETY CONDITION CHECK:</b><br><font color='#FFD700'>Can Satisfy = (Need &lt;= Work for all resource types A, B, C)</font>",
                            "<b>DERIVATION:</b> Check if Need[A]&lt;=Work[A], Need[B]&lt;=Work[B], and Need[C]&lt;=Work[C].",
                            "<b>OS PRINCIPLE:</b> A process can only proceed if all its resource claims can be guaranteed by available reserves.",
                            "<b>TUTOR TIP:</b> Enter 'YES' if all 3 need components are less than or equal to work; otherwise enter 'NO'."
                    );
                } else if (colName.contains("STATUS") || colName.contains("CLEARANCE")) {
                    setWorkbenchData(
                            "<b>CLEARANCE STATUS:</b><br><font color='#57FF3C'>RUNS [EXEC] if Satisfied  |  WAIT [BLOCKED] if Unsatisfied</font>",
                            "<b>DERIVATION:</b> If Can Satisfy is YES, client runs and releases resources; otherwise it waits.",
                            "<b>OS PRINCIPLE:</b> Blocked processes wait safely in the queue without consuming unallocated resources.",
                            "<b>TUTOR TIP:</b> Enter 'RUNS [EXEC]' or 'WAIT [BLOCKED]'."
                    );
                } else {
                    setWorkbenchData(
                            "<b>BANKER'S VAULT PARAMETER: " + colName + "</b>",
                            "<b>CLIENT " + rowName + ":</b> " + activeQuestion.worksheetData[r][c],
                            "<b>OS PRINCIPLE:</b> Multi-instance resource matrix accounting for deadlock avoidance.",
                            "<b>TUTOR TIP:</b> Use Allocation and Max Claim to compute Need = Max - Alloc."
                    );
                }
                break;

            case 2: // Frame Hopper MMU Paging
                if (colName.contains("PAGE #")) {
                    setWorkbenchData(
                            "<b>PAGE NUMBER (p):</b><br><font color='#05D9E8'>Page # (p) = Logical Address / Page Size (1024 B)</font><br>Integer division quotient.",
                            "<b>DERIVATION:</b><br><font color='#57FF3C'>Divide the logical address by 1024 (discard remainder).</font>",
                            "<b>OS PRINCIPLE:</b> The MMU uses the high-order page number as an index into the process page table.",
                            "<b>TUTOR TIP:</b> Logical Address divided by 1024 (e.g. 2500 / 1024 = 2)."
                    );
                } else if (colName.contains("OFFSET")) {
                    setWorkbenchData(
                            "<b>PAGE OFFSET (d):</b><br><font color='#05D9E8'>Offset (d) = Logical Address % Page Size (1024 B)</font><br>Modulo remainder within page frame.",
                            "<b>DERIVATION:</b><br><font color='#57FF3C'>Compute remainder: Address modulo 1024.</font>",
                            "<b>OS PRINCIPLE:</b> Offset bits are copied directly from the logical address to the physical address without translation.",
                            "<b>TUTOR TIP:</b> Address % 1024 gives the byte offset (e.g. 2500 % 1024 = 452 B)."
                    );
                } else if (colName.contains("FRAME")) {
                    setWorkbenchData(
                            "<b>PAGE-TO-FRAME MAP LOOKUP:</b><br><font color='#FFD700'>Frame # (f) = PageTable[p]</font>",
                            "<b>DERIVATION:</b> Look up Page # (p) in the Page Table: [P0->F5, P1->F6, P2->F1, P3->F2, P4->F7, P5->F3, P6->F8, P7->F4].",
                            "<b>OS PRINCIPLE:</b> The page table translates virtual page numbers into physical DRAM frame numbers.",
                            "<b>TUTOR TIP:</b> Find the mapped frame for this row's Page # in the dossier table."
                    );
                } else if (colName.contains("PHYSICAL")) {
                    setWorkbenchData(
                            "<b>PHYSICAL RAM ADDRESS (PA):</b><br><font color='#57FF3C'>PA = (Frame # * Page Size) + Offset</font>",
                            "<b>DERIVATION:</b><br><font color='#05D9E8'>Multiply mapped Frame # by 1024 and add the byte offset (d).</font>",
                            "<b>OS PRINCIPLE:</b> Hardware address generation concatenates the frame base address with the offset.",
                            "<b>TUTOR TIP:</b> (Frame # * 1024) + Offset = Physical Address."
                    );
                }
                break;

            case 3: // Super Mario Cache Replacement
                if (rowName.contains("SUMMARY")) {
                    if (colName.contains("STATUS") || colName.contains("FAULT") || c == 4) {
                        setWorkbenchData(
                                "<b>TOTAL PAGE FAULTS:</b><br><font color='#FF5757'>Count of all cold and capacity misses across the stream.</font>",
                                "<b>DERIVATION:</b> Sum all rows with 'FAULT' status in the matrix above.",
                                "<b>OS PRINCIPLE:</b> Page faults require loading pages from backing store into physical memory frames.",
                                "<b>TUTOR TIP:</b> Count how many times 'FAULT' occurred in the STATUS column above."
                        );
                    } else if (colName.contains("HIT") || c == 5) {
                        setWorkbenchData(
                                "<b>CACHE HIT RATIO (%):</b><br><font color='#FFD700'>Hit Ratio = (Total Hits / Total References) * 100%</font>",
                                "<b>DERIVATION:</b> (Hits / Total Stream Steps) * 100%.",
                                "<b>OS PRINCIPLE:</b> Primary metric of cache efficiency. Higher hit ratios reduce memory stall cycles.",
                                "<b>TUTOR TIP:</b> Total Hits divided by reference stream length times 100."
                        );
                    }
                } else if (colName.contains("PIPE")) {
                    setWorkbenchData(
                            "<b>PIPE RESIDENT FRAME:</b><br><font color='#05D9E8'>Current page residing in this pipe slot.</font>",
                            "<b>DERIVATION:</b> Under FIFO policy, pages occupy Pipe #0, #1, #2. When full, the oldest arrived page is replaced.",
                            "<b>OS PRINCIPLE:</b> First-In First-Out (FIFO) replacement evicts the page that has been in memory the longest.",
                            "<b>TUTOR TIP:</b> Enter the page number currently in this pipe (or '—' if vacant)."
                    );
                } else if (colName.contains("STATUS")) {
                    setWorkbenchData(
                            "<b>CACHE STATUS (HIT / FAULT):</b><br><font color='#57FF3C'>HIT if page is already in pipes  |  FAULT if missing</font>",
                            "<b>DERIVATION:</b> Check if the incoming page is already present in Pipe #0, #1, or #2.",
                            "<b>OS PRINCIPLE:</b> Hits execute with zero memory stall. Faults require fetching and possible eviction.",
                            "<b>TUTOR TIP:</b> If the page is already in one of the pipes, enter 'HIT'; otherwise enter 'FAULT'!"
                    );
                }
                break;

            case 4: // Heap Snake Memory Segmentation
                if (colName.contains("STATUS")) {
                    setWorkbenchData(
                            "<b>SEGMENT BOUNDS PROTECTION:</b><br><font color='#FFD700'>If Offset &lt; Limit -> VALID [OK]  |  If Offset &gt;= Limit -> TRAP: SEGFAULT</font>",
                            "<b>DERIVATION:</b> Check if Offset is strictly within hardware Limit bound for " + rowName + ".",
                            "<b>OS PRINCIPLE:</b> Hardware limit registers prevent a process from accessing memory outside its assigned segment.",
                            "<b>TUTOR TIP:</b> If Offset is strictly less than Limit, enter 'VALID [OK]'; else enter 'TRAP: SEGFAULT'."
                    );
                } else if (colName.contains("PHYSICAL")) {
                    setWorkbenchData(
                            "<b>PHYSICAL RAM TRANSLATION:</b><br><font color='#57FF3C'>If VALID: PA = Base + Offset  |  If TRAP: 'TRAP: VIOLATION'</font>",
                            "<b>DERIVATION:</b> For valid accesses, add Base address to Offset.",
                            "<b>OS PRINCIPLE:</b> Base registers provide memory relocation; out-of-bounds accesses trigger an MMU trap.",
                            "<b>TUTOR TIP:</b> If status is TRAP, enter 'TRAP: VIOLATION'. If VALID, add Base and Offset."
                    );
                }
                break;

            case 5: // Cyber Crane Contiguous RAM Allocation
                if (colName.contains("BASE")) {
                    setWorkbenchData(
                            "<b>BASE ADDRESS:</b><br><font color='#05D9E8'>Base Address of assigned socket</font>",
                            "<b>DERIVATION:</b> Look up the allocated socket's physical Base address in Motherboard specs.",
                            "<b>OS PRINCIPLE:</b> Physical memory starting address for the allocated partition block.",
                            "<b>TUTOR TIP:</b> Base address of assigned socket (e.g. S2=100 KB, S3=600 KB; '—' if no fit)."
                    );
                } else if (colName.contains("LIMIT") || colName.contains("CEILING")) {
                    setWorkbenchData(
                            "<b>LIMIT / CEILING:</b><br><font color='#05D9E8'>Limit = Base Address + Socket Capacity</font>",
                            "<b>DERIVATION:</b> Add socket size to base address to get upper memory boundary.",
                            "<b>OS PRINCIPLE:</b> Memory ceiling register preventing partition overflow.",
                            "<b>TUTOR TIP:</b> Base Address plus Socket Capacity (e.g. 100 KB + 500 KB = 600 KB; '—' if no fit)."
                    );
                } else if (colName.contains("SOCKET")) {
                    setWorkbenchData(
                            "<b>FIRST-FIT SOCKET ASSIGNMENT:</b><br><font color='#05D9E8'>First available socket with Capacity &gt;= Demand</font>",
                            "<b>DERIVATION:</b> Search sockets in order: S1(100K), S2(500K), S3(200K), S4(300K), S5(600K). Assign first free socket that fits.",
                            "<b>OS PRINCIPLE:</b> First-Fit scans memory from the beginning and allocates the first hole that is large enough.",
                            "<b>TUTOR TIP:</b> Enter the socket name (e.g. 'Socket 2 [500K]' or 'Socket 5 [600K]'; if none fit, enter 'NONE')."
                    );
                } else if (colName.contains("FRAG")) {
                    setWorkbenchData(
                            "<b>INTERNAL FRAGMENTATION (HEAT):</b><br><font color='#FFD700'>Internal Frag = Socket Capacity - Chip Demand Size</font>",
                            "<b>DERIVATION:</b> Unused memory wasted inside the allocated partition block (0 KB if no fit).",
                            "<b>OS PRINCIPLE:</b> Occurs when memory allocated to a process is slightly larger than the requested size.",
                            "<b>TUTOR TIP:</b> Socket Limit minus Chip Demand (e.g. 500 KB - 212 KB = 288 KB)."
                    );
                } else if (colName.contains("STATUS")) {
                    setWorkbenchData(
                            "<b>ALLOCATION STATUS:</b><br><font color='#57FF3C'>ALLOCATED [OK] if fits  |  DENIED: NO FIT if no partition fits</font>",
                            "<b>DERIVATION:</b> Check if a free socket was found with sufficient capacity.",
                            "<b>OS PRINCIPLE:</b> The dynamic partition manager either allocates a hole or denies the request.",
                            "<b>TUTOR TIP:</b> Enter 'ALLOCATED [OK]' or 'DENIED: NO FIT'."
                    );
                } else {
                    setWorkbenchData(
                            "<b>PARTITION TELEMETRY: " + colName + "</b><br><font color='#05D9E8'>Chip specification</font>",
                            "<b>VALUE FOR " + rowName + ":</b> " + activeQuestion.worksheetData[r][c],
                            "<b>OS PRINCIPLE:</b> Memory demand profile.",
                            "<b>TUTOR TIP:</b> Use this value to calculate partition placement."
                    );
                }
                break;

            case 6: // Pac-Man Magnetic Disk Traversal
                if (rowName.contains("TOTAL") || rowName.contains("AVERAGE")) {
                    if (c == 3 || colName.contains("TRAVERSED") || colName.contains("TRACKS")) {
                        setWorkbenchData(
                                "<b>TOTAL CUMULATIVE SEEK:</b><br><font color='#FFD700'>Total = Sum of all individual track seeks</font>",
                                "<b>DERIVATION:</b> Sum the TRACKS TRAVERSED column across all steps.",
                                "<b>OS PRINCIPLE:</b> Measures total mechanical actuator arm traversal across magnetic platter cylinders.",
                                "<b>TUTOR TIP:</b> Add all values in the TRACKS TRAVERSED column above."
                        );
                    } else if (c == 5 || colName.contains("SEEK") || colName.contains("CUMULATIVE")) {
                        setWorkbenchData(
                                "<b>AVERAGE SEEK LENGTH:</b><br><font color='#57FF3C'>Average Seek = Total Tracks Traversed / Number of Steps</font>",
                                "<b>DERIVATION:</b> Divide Total Cumulative Seek by step count N.",
                                "<b>OS PRINCIPLE:</b> Key indicator of disk scheduler throughput efficiency.",
                                "<b>TUTOR TIP:</b> Total Tracks divided by number of steps. Enter with 2 decimals."
                        );
                    }
                } else if (colName.contains("TRAVERSED") || colName.contains("TRACKS")) {
                    setWorkbenchData(
                            "<b>TRACKS TRAVERSED:</b><br><font color='#05D9E8'>Tracks = |Next Track - Current Track|</font>",
                            "<b>DERIVATION:</b> Absolute difference between target cylinder and current head track.",
                            "<b>OS PRINCIPLE:</b> Cylinder seek distance is non-negative regardless of seek direction.",
                            "<b>TUTOR TIP:</b> |Next Track - Current Track| (e.g. |98 - 53| = 45 cyl)."
                    );
                } else if (colName.contains("DIRECTION")) {
                    setWorkbenchData(
                            "<b>SEEK DIRECTION:</b><br><font color='#FFD700'>UP (->) if Next &gt; Current  |  DOWN (&lt;-) if Next &lt; Current</font>",
                            "<b>DERIVATION:</b> Compare target track against current track position.",
                            "<b>OS PRINCIPLE:</b> Head actuator motor seeks radially inward (UP) or outward (DOWN).",
                            "<b>TUTOR TIP:</b> Enter 'UP (->)' if moving to a higher track, or 'DOWN (<-)' if lower."
                    );
                } else if (colName.contains("CUMULATIVE")) {
                    setWorkbenchData(
                            "<b>CUMULATIVE SEEK:</b><br><font color='#57FF3C'>Running Sum of all tracks traversed so far.</font>",
                            "<b>DERIVATION:</b> Add this step's tracks traversed to previous step's cumulative total.",
                            "<b>OS PRINCIPLE:</b> Cumulative seek tracks cumulative mechanical arm travel.",
                            "<b>TUTOR TIP:</b> Add this row's seek to previous row's cumulative total."
                    );
                }
                break;

            case 7: // Overcooked Multiprogramming Trace // Event-Driven Execution
                if (colName.contains("TIME LEFT") || c == 7) {
                    setWorkbenchData(
                            "<b>TIME LEFT (REMAINING BURST):</b><br><font color='#57FF3C'>TIME LEFT = previous TIME LEFT - hw/job</font>",
                            "<b>DERIVATION:</b> Subtract hardware work done (hw/job) from job's remaining burst time.",
                            "<b>OS PRINCIPLE:</b> Tracks remaining execution quantum until job finishes (0.000 [FINISHED]).",
                            "<b>TUTOR TIP:</b> Subtract hw/job from the previous time left for this job. Enter 0.000 [FINISHED] if completed."
                    );
                } else if (colName.contains("HW") || c == 5) {
                    setWorkbenchData(
                            "<b>HARDWARE WORK PER JOB (hw/job):</b><br><font color='#FFD700'>hw/job = et * (%cpu/job)</font>",
                            "<b>DERIVATION:</b> Multiply interval elapsed time (et) by equal CPU percentage (%cpu/job).",
                            "<b>OS PRINCIPLE:</b> Exact CPU execution cycles dedicated to each concurrent job during this interval.",
                            "<b>TUTOR TIP:</b> Multiply column 'et' by '%cpu/job'. Enter with 3 decimals."
                    );
                } else if (colName.contains("ET") || c == 4) {
                    setWorkbenchData(
                            "<b>ELAPSED TIME (et):</b><br><font color='#05D9E8'>et = present TIME - previous TIME</font>",
                            "<b>DERIVATION:</b> Duration of current time slice interval.",
                            "<b>OS PRINCIPLE:</b> Time quantum elapsed between consecutive arrival or termination events.",
                            "<b>TUTOR TIP:</b> Current row's TIME minus previous interval's TIME. Enter with 2 decimals."
                    );
                } else if (colName.contains("%CPU") || c == 3) {
                    setWorkbenchData(
                            "<b>%CPU PER JOB (%cpu/job):</b><br><font color='#FF5757'>%cpu/job = 1 / nj (Equal CPU sharing)</font>",
                            "<b>DERIVATION:</b> 1 divided by the number of active concurrent jobs (nj). If nj=0, 0.",
                            "<b>OS PRINCIPLE:</b> Multiprogramming processor dynamically partitions equal 1/N time slices.",
                            "<b>TUTOR TIP:</b> If nj=1 enter 1.000, if nj=2 enter 0.500, if nj=3 enter 0.333, etc."
                    );
                } else if (colName.contains("NJ") || c == 2) {
                    setWorkbenchData(
                            "<b>ACTIVE JOBS (nj):</b><br><font color='#FFD700'>Count of concurrently executing jobs on the CPU.</font>",
                            "<b>DERIVATION:</b> Number of jobs that have arrived and not yet finished at this interval.",
                            "<b>OS PRINCIPLE:</b> System concurrency degree determining time-slice division.",
                            "<b>TUTOR TIP:</b> Count how many jobs share the CPU during this interval."
                    );
                } else if (colName.contains("EVENT") || c == 1) {
                    setWorkbenchData(
                            "<b>DISPATCH EVENT:</b><br><font color='#05D9E8'>a/s = Arrive & Start  |  t [FINISHED] = Terminate</font>",
                            "<b>DERIVATION:</b> Event triggering interval transition.",
                            "<b>OS PRINCIPLE:</b> Event-driven preemptive concurrency triggers recalculation upon arrival or finish.",
                            "<b>TUTOR TIP:</b> Select the event from the word selector (e.g. 'J1 a/s' or 'J1 t [FINISHED]')."
                    );
                } else if (colName.contains("JOB") || c == 6) {
                    setWorkbenchData(
                            "<b>JOB IDENTIFIER (JOB#):</b><br><font color='#05D9E8'>Active process ID being updated in this row.</font>",
                            "<b>DERIVATION:</b> Process label (J1, J2, J3, ...) executing during this time slice.",
                            "<b>OS PRINCIPLE:</b> Each active job has its own dedicated remaining burst ledger entry.",
                            "<b>TUTOR TIP:</b> Select the process ID (J1, J2, J3, ...)."
                    );
                } else {
                    setWorkbenchData(
                            "<b>TIMESTAMP (TIME):</b><br><font color='#FF5757'>Event occurrence timestamp.</font>",
                            "<b>DERIVATION:</b> Clock time when arrival or termination event takes place.",
                            "<b>OS PRINCIPLE:</b> Discrete simulation timeline advancing with each event.",
                            "<b>TUTOR TIP:</b> Enter event timestamp (e.g. 7.40, 7.80, 8.10)."
                    );
                }
                break;
        }

        // ── Input Mode Selector: WORDS (one-click arcade pills) vs MATH (register) ──
        String[] wordOptions = getWordOptionsForCell(r, c);
        if (wordOptions != null && wordOptions.length > 0) {
            populateWordSelectionRow(r, c, wordOptions);
            cardsInputMode.show(pnlInputSwitch, "WORDS");
        } else {
            cardsInputMode.show(pnlInputSwitch, "MATH");
            autoPopulateQuickMathRegister(r, c, rowName, colName);
        }

        updateCalcToolButtons(r, c, rowName, colName);
    }

    private void autoPopulateQuickMathRegister(int r, int c, String rowName, String colName) {
        if (updatingScratchpad || activeQuestion == null || activeQuestion.worksheetData == null) return;
        updatingScratchpad = true;
        try {
            switch (currentModuleIdx) {
                case 0: // Space Invaders CPU Scheduling FCFS
                    if (rowName.contains("AVERAGE") || rowName.contains("AVG")) {
                        int n = Math.max(1, activeQuestion.worksheetData.length - 1);
                        int targetCol = (colName.contains("TURN") || c == 6) ? 6 : 5;
                        double sum = 0.0;
                        for (int i = 0; i < n; i++) {
                            Double val = getTableCellDouble(i, targetCol);
                            if (val != null) sum += val;
                        }
                        txtScratchA.setText(formatDouble(sum));
                        cmbScratchOp.setSelectedItem("÷");
                        txtScratchB.setText(String.valueOf(n));
                    } else if (colName.contains("START") || c == 3) {
                        if (r == 0) {
                            Double at = getTableCellDouble(r, 1);
                            txtScratchA.setText(at != null ? formatDouble(at) : "0.0");
                            cmbScratchOp.setSelectedItem("+");
                            txtScratchB.setText("0.0");
                        } else {
                            Double prevFt = getTableCellDouble(r - 1, 4);
                            Double at = getTableCellDouble(r, 1);
                            if (prevFt != null && at != null) {
                                double chosen = Math.max(at, prevFt);
                                txtScratchA.setText(formatDouble(chosen));
                                cmbScratchOp.setSelectedItem("+");
                                txtScratchB.setText("0.0");
                            } else if (prevFt != null) {
                                txtScratchA.setText(formatDouble(prevFt));
                                cmbScratchOp.setSelectedItem("+");
                                txtScratchB.setText("0.0");
                            } else if (at != null) {
                                txtScratchA.setText(formatDouble(at));
                                cmbScratchOp.setSelectedItem("+");
                                txtScratchB.setText("0.0");
                            } else {
                                txtScratchA.setText("0.0");
                                cmbScratchOp.setSelectedItem("+");
                                txtScratchB.setText("0.0");
                            }
                        }
                    } else if (colName.contains("FINISH") || c == 4) {
                        Double st = getTableCellDouble(r, 3);
                        Double bt = getTableCellDouble(r, 2);
                        if (st == null && r == 0) st = getTableCellDouble(r, 1);
                        if (st == null && r > 0) {
                            Double prevFt = getTableCellDouble(r - 1, 4);
                            Double at = getTableCellDouble(r, 1);
                            if (prevFt != null && at != null) st = Math.max(at, prevFt);
                        }
                        txtScratchA.setText(st != null ? formatDouble(st) : "0.0");
                        cmbScratchOp.setSelectedItem("+");
                        txtScratchB.setText(bt != null ? formatDouble(bt) : "0.0");
                    } else if (colName.contains("WAIT") || c == 5) {
                        Double tat = getTableCellDouble(r, 6);
                        Double bt = getTableCellDouble(r, 2);
                        Double st = getTableCellDouble(r, 3);
                        Double at = getTableCellDouble(r, 1);
                        if (tat != null && bt != null) {
                            txtScratchA.setText(formatDouble(tat));
                            cmbScratchOp.setSelectedItem("-");
                            txtScratchB.setText(formatDouble(bt));
                        } else if (st != null && at != null) {
                            txtScratchA.setText(formatDouble(st));
                            cmbScratchOp.setSelectedItem("-");
                            txtScratchB.setText(formatDouble(at));
                        } else {
                            txtScratchA.setText(st != null ? formatDouble(st) : (tat != null ? formatDouble(tat) : "0.0"));
                            cmbScratchOp.setSelectedItem("-");
                            txtScratchB.setText(at != null ? formatDouble(at) : (bt != null ? formatDouble(bt) : "0.0"));
                        }
                    } else if (colName.contains("TURN") || c == 6) {
                        Double ft = getTableCellDouble(r, 4);
                        Double at = getTableCellDouble(r, 1);
                        txtScratchA.setText(ft != null ? formatDouble(ft) : "0.0");
                        cmbScratchOp.setSelectedItem("-");
                        txtScratchB.setText(at != null ? formatDouble(at) : "0.0");
                    }
                    break;

                case 7: // Overcooked Multiprogramming Trace // Event-Driven Execution
                    if (colName.contains("%CPU") || c == 3) {
                        // 1 / nj
                        String njS = getTableCellString(r, 2);
                        int sc = r;
                        while ((njS == null || njS.trim().isEmpty() || njS.equals("-")) && sc > 0) {
                            sc--;
                            njS = getTableCellString(sc, 2);
                        }
                        txtScratchA.setText("1");
                        cmbScratchOp.setSelectedItem("÷");
                        txtScratchB.setText((njS != null && !njS.equals("?") && !njS.isEmpty() && !njS.equals("-")) ? njS : "1");
                    } else if (colName.contains("ET") || c == 4) {
                        // curT - prevT
                        String curTStr = getTableCellString(r, 0);
                        int curScan = r;
                        while ((curTStr == null || curTStr.trim().isEmpty() || curTStr.equals("-")) && curScan > 0) {
                            curScan--;
                            curTStr = getTableCellString(curScan, 0);
                        }
                        String prevTStr = "0.00";
                        for (int p = curScan - 1; p >= 0; p--) {
                            String pt = getTableCellString(p, 0);
                            if (pt != null && !pt.trim().isEmpty() && !pt.equals("-") && !pt.equals("?")) {
                                prevTStr = pt;
                                break;
                            }
                        }
                        txtScratchA.setText(curTStr != null && !curTStr.equals("?") ? curTStr : "0.00");
                        cmbScratchOp.setSelectedItem("-");
                        txtScratchB.setText(prevTStr != null && !prevTStr.equals("?") ? prevTStr : "0.00");
                    } else if (colName.contains("HW") || c == 5) {
                        // et * %cpu
                        Double etVal = getTableCellDouble(r, 4);
                        Double pctVal = getTableCellDouble(r, 3);
                        int hwSc = r;
                        while ((etVal == null || pctVal == null) && hwSc > 0) {
                            hwSc--;
                            if (etVal == null) etVal = getTableCellDouble(hwSc, 4);
                            if (pctVal == null) pctVal = getTableCellDouble(hwSc, 3);
                        }
                        txtScratchA.setText(etVal != null ? formatDouble(etVal) : "0.00");
                        cmbScratchOp.setSelectedItem("×");
                        txtScratchB.setText(pctVal != null ? formatDouble(pctVal) : "0.000");
                    } else if (colName.contains("TIME LEFT") || c == 7) {
                        // prevTimeLeft - hw/job
                        String curJob = getTableCellString(r, 6);
                        Double prevLeft = null;
                        if (curJob != null && !curJob.isEmpty() && !curJob.equals("?")) {
                            for (int p = r - 1; p >= 0; p--) {
                                if (curJob.equalsIgnoreCase(getTableCellString(p, 6))) {
                                    prevLeft = getTableCellDouble(p, 7);
                                    if (prevLeft != null) break;
                                }
                            }
                        }
                        Double hwVal = getTableCellDouble(r, 5);
                        int hwSc = r;
                        while (hwVal == null && hwSc > 0) {
                            hwSc--;
                            hwVal = getTableCellDouble(hwSc, 5);
                        }
                        txtScratchA.setText(prevLeft != null ? formatDouble(prevLeft) : "0.000");
                        cmbScratchOp.setSelectedItem("-");
                        txtScratchB.setText(hwVal != null ? formatDouble(hwVal) : "0.000");
                    }
                    break;

                case 1: // Banker's
                    if (colName.contains("NEED")) {
                        String allocStr = getTableCellString(r, 1);
                        String maxStr = getTableCellString(r, 2);
                        String[] allocTokens = allocStr.trim().split("\\s+");
                        String[] maxTokens = maxStr.trim().split("\\s+");
                        if (allocTokens.length > 0 && maxTokens.length > 0) {
                            txtScratchA.setText(maxTokens[0]);
                            cmbScratchOp.setSelectedItem("-");
                            txtScratchB.setText(allocTokens[0]);
                        }
                    }
                    break;

                case 2: // Paging
                    String addrStr = getTableCellString(r, 0).replaceAll("[^0-9]", "");
                    if (addrStr.isEmpty()) addrStr = "2500";
                    if (colName.contains("PAGE #")) {
                        txtScratchA.setText(addrStr);
                        cmbScratchOp.setSelectedItem("÷");
                        txtScratchB.setText("1024");
                    } else if (colName.contains("OFFSET")) {
                        txtScratchA.setText(addrStr);
                        cmbScratchOp.setSelectedItem("%");
                        txtScratchB.setText("1024");
                    }
                    break;

                case 5: // Crane RAM Partitions
                    if (colName.contains("FRAG")) {
                        Double cap = getTableCellDouble(r, 1);
                        Double dem = getTableCellDouble(r, 2);
                        txtScratchA.setText(cap != null ? formatDouble(cap) : "0.0");
                        cmbScratchOp.setSelectedItem("-");
                        txtScratchB.setText(dem != null ? formatDouble(dem) : "0.0");
                    }
                    break;

                case 6: // Pacman Disk Seek
                    if (rowName.contains("AVERAGE") || rowName.contains("TOTAL")) {
                        int steps = Math.max(1, activeQuestion.worksheetData.length - 1);
                        double tot = 0.0;
                        for (int i = 0; i < steps; i++) {
                            Double val = getTableCellDouble(i, 3);
                            if (val != null) tot += val;
                        }
                        txtScratchA.setText(formatDouble(tot));
                        cmbScratchOp.setSelectedItem("÷");
                        txtScratchB.setText(String.valueOf(steps));
                    } else if (colName.contains("TRAVERSED") || c == 3) {
                        Double targetTrack = getTableCellDouble(r, 1);
                        Double prevTrack = (r > 0) ? getTableCellDouble(r - 1, 1) : 53.0;
                        txtScratchA.setText(targetTrack != null ? formatDouble(targetTrack) : "0.0");
                        cmbScratchOp.setSelectedItem("-");
                        txtScratchB.setText(prevTrack != null ? formatDouble(prevTrack) : "0.0");
                    }
                    break;

                default:
                    break;
            }
        } catch (Exception ignored) {
        } finally {
            updatingScratchpad = false;
            computeScratchpad(false);
        }
    }

    private String getTableCellString(int r, int c) {
        if (tableModel == null || r < 0 || r >= tableModel.getRowCount() || c < 0 || c >= tableModel.getColumnCount()) {
            return "";
        }
        Object val = tableModel.getValueAt(r, c);
        if (val == null) return "";
        String s = val.toString().trim();
        if (s.startsWith("OK ") || s.startsWith("ERR ")) s = s.substring(3).trim();
        if (s.startsWith("✔ ") || s.startsWith("✘ ")) s = s.substring(2).trim();
        return s;
    }

    private Double getTableCellDouble(int r, int c) {
        String s = getTableCellString(r, c);
        if (s.isEmpty() || s.equals("?") || s.equals("[ ? ]") || s.equals("-") || s.equals("—")) return null;
        try {
            return Double.parseDouble(s);
        } catch (Exception ex) {
            return null;
        }
    }

    private String formatDouble(double d) {
        if (Math.abs(d - Math.round(d)) < 0.0001) {
            return String.format(Locale.US, "%.0f", d);
        } else {
            return String.format(Locale.US, "%.2f", d);
        }
    }

    public String[] getWordOptionsForCell(int row, int col) {
        if (activeQuestion == null || activeQuestion.columnHeaders == null || col >= activeQuestion.columnHeaders.length) {
            return null;
        }
        String header = activeQuestion.columnHeaders[col].toUpperCase(Locale.US);

        if (currentModuleIdx == 1) { // Banker's Algorithm
            if (header.contains("SATISFY")) {
                return new String[] { "YES", "NO" };
            }
            if (header.contains("CLEARANCE") || header.contains("STATUS")) {
                return new String[] { "RUNS [EXEC]", "WAIT [BLOCKED]" };
            }
        } else if (currentModuleIdx == 2) { // Frame Hopper
            if (header.contains("FRAME #") || (col == 4 && header.contains("FRAME"))) {
                return new String[] { "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8" };
            }
        } else if (currentModuleIdx == 3) { // Mario Cache
            int n = activeQuestion.worksheetData != null ? activeQuestion.worksheetData.length - 1 : 0;
            if (row < n && (header.contains("STATUS") || col == 5)) {
                return new String[] { "HIT", "FAULT" };
            }
        } else if (currentModuleIdx == 4) { // Snake Segmentation
            if (header.contains("STATUS")) {
                return new String[] { "VALID [OK]", "TRAP: SEGFAULT" };
            }
        } else if (currentModuleIdx == 5) { // Crane Dynamic Partitions
            if (header.contains("SOCKET")) {
                String[] sockNames = (activeQuestion != null && activeQuestion.telemetryData.get("sockNames") != null)
                        ? (String[]) activeQuestion.telemetryData.get("sockNames")
                        : new String[] { "Socket 1", "Socket 2", "Socket 3", "Socket 4", "Socket 5" };
                String[] opts = new String[sockNames.length + 1];
                System.arraycopy(sockNames, 0, opts, 0, sockNames.length);
                opts[sockNames.length] = "NONE";
                return opts;
            }
            if (header.contains("STATUS")) {
                return new String[] { "ALLOCATED [OK]", "DENIED: NO FIT" };
            }
        } else if (currentModuleIdx == 6) { // Pacman Disk Seek
            int n = activeQuestion.worksheetData != null ? activeQuestion.worksheetData.length - 1 : 0;
            if (row < n && (header.contains("DIRECTION") || col == 4)) {
                return new String[] { "UP (->)", "DOWN (<-)" };
            }
        } else if (currentModuleIdx == 7) { // Overcooked Multiprogramming Trace
            if (header.contains("EVENT") || col == 1) {
                List<String> events = new ArrayList<>();
                if (activeQuestion != null && activeQuestion.answerKey != null) {
                    for (int r = 0; r < activeQuestion.answerKey.length; r++) {
                        String ev = activeQuestion.answerKey[r][1];
                        if (ev != null && !ev.trim().isEmpty() && !ev.trim().equals("-") && !events.contains(ev.trim())) {
                            events.add(ev.trim());
                        }
                    }
                }
                if (events.isEmpty()) {
                    for (int j = 1; j <= 5; j++) {
                        events.add("J" + j + " a/s");
                        events.add("J" + j + " t [FINISHED]");
                    }
                }
                return events.toArray(new String[0]);
            }
            if (header.contains("JOB") || col == 6) {
                return new String[] { "J1", "J2", "J3", "J4", "J5", "J6", "J7" };
            }
        }

        return null;
    }

    private JButton createPolicyBtn(String pol, String label) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Consolas", Font.BOLD, 9));
        btn.setBackground(new Color(25, 20, 45));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 50, 95), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            QuizManager.activeCachePolicy = pol;
            updatePolicyButtons();
            loadQuestion(3, currentDifficulty, currentMode, true);
            showToast("SWITCHED TO " + pol + " PAGE REPLACEMENT POLICY");
        });
        return btn;
    }

    private void updatePolicyButtons() {
        if (btnPolicyFifo == null || btnPolicyLru == null || btnPolicyOpt == null) return;
        String cur = (activeQuestion != null && activeQuestion.policy != null)
                ? activeQuestion.policy : (QuizManager.activeCachePolicy != null ? QuizManager.activeCachePolicy : "FIFO");

        stylePolicyButton(btnPolicyFifo, cur.equals("FIFO"), RetroTheme.ACCENT_GREEN);
        stylePolicyButton(btnPolicyLru, cur.equals("LRU"), RetroTheme.ACCENT_CYAN);
        stylePolicyButton(btnPolicyOpt, cur.equals("OPTIMAL"), RetroTheme.ACCENT_YELLOW);
    }

    private void stylePolicyButton(JButton btn, boolean active, Color accent) {
        if (active) {
            btn.setBackground(new Color(accent.getRed() / 4, accent.getGreen() / 4, accent.getBlue() / 4));
            btn.setForeground(accent);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(accent, 1),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));
        } else {
            btn.setBackground(new Color(25, 20, 45));
            btn.setForeground(RetroTheme.TEXT_DIM);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(55, 45, 80), 1),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));
        }
    }

    private JButton createCranePolicyBtn(String pol, String label) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Consolas", Font.BOLD, 9));
        btn.setBackground(new Color(25, 20, 45));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 50, 95), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            QuizManager.activeAllocationPolicy = pol;
            updateCranePolicyButtons();
            loadQuestion(5, currentDifficulty, currentMode, true);
            showToast("SWITCHED TO " + pol + " ALLOCATION HEURISTIC");
        });
        return btn;
    }

    private void updateCranePolicyButtons() {
        if (btnPolicyFirstFit == null || btnPolicyBestFit == null || btnPolicyNextFit == null) return;
        String cur = (activeQuestion != null && activeQuestion.policy != null)
                ? activeQuestion.policy : (QuizManager.activeAllocationPolicy != null ? QuizManager.activeAllocationPolicy : "FIRST FIT");

        styleCranePolicyButton(btnPolicyFirstFit, cur.equals("FIRST FIT"), RetroTheme.ACCENT_CYAN);
        styleCranePolicyButton(btnPolicyBestFit, cur.equals("BEST FIT"), RetroTheme.ACCENT_GREEN);
        styleCranePolicyButton(btnPolicyNextFit, cur.equals("NEXT FIT"), RetroTheme.ACCENT_YELLOW);
    }

    private void styleCranePolicyButton(JButton btn, boolean active, Color accent) {
        if (active) {
            btn.setBackground(new Color(accent.getRed() / 4, accent.getGreen() / 4, accent.getBlue() / 4));
            btn.setForeground(accent);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(accent, 1),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));
        } else {
            btn.setBackground(new Color(25, 20, 45));
            btn.setForeground(RetroTheme.TEXT_DIM);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(55, 45, 80), 1),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));
        }
    }

    private JButton createPacmanPolicyBtn(String pol, String label) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Consolas", Font.BOLD, 9));
        btn.setBackground(new Color(25, 20, 45));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 50, 95), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            QuizManager.activeDiskPolicy = pol;
            updatePacmanPolicyButtons();
            loadQuestion(6, currentDifficulty, currentMode, true);
            showToast("SWITCHED TO " + pol + " DISK SEEK POLICY");
        });
        return btn;
    }

    private void updatePacmanPolicyButtons() {
        if (btnPolicyFcfs == null || btnPolicySstf == null || btnPolicyScan == null ||
                btnPolicyCScan == null || btnPolicyLook == null || btnPolicyCLook == null) return;
        String cur = (activeQuestion != null && activeQuestion.policy != null)
                ? activeQuestion.policy : (QuizManager.activeDiskPolicy != null ? QuizManager.activeDiskPolicy : "FCFS");

        stylePacmanPolicyButton(btnPolicyFcfs, cur.equals("FCFS"), RetroTheme.ACCENT_YELLOW);
        stylePacmanPolicyButton(btnPolicySstf, cur.equals("SSTF"), RetroTheme.ACCENT_CYAN);
        stylePacmanPolicyButton(btnPolicyScan, cur.equals("SCAN"), RetroTheme.ACCENT_GREEN);
        stylePacmanPolicyButton(btnPolicyCScan, cur.equals("C-SCAN"), RetroTheme.ACCENT_PINK);
        stylePacmanPolicyButton(btnPolicyLook, cur.equals("LOOK"), RetroTheme.ACCENT_PURPLE);
        stylePacmanPolicyButton(btnPolicyCLook, cur.equals("C-LOOK"), new Color(255, 160, 50));
    }

    private void stylePacmanPolicyButton(JButton btn, boolean active, Color accent) {
        if (active) {
            btn.setBackground(new Color(accent.getRed() / 4, accent.getGreen() / 4, accent.getBlue() / 4));
            btn.setForeground(accent);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(accent, 1),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));
        } else {
            btn.setBackground(new Color(25, 20, 45));
            btn.setForeground(RetroTheme.TEXT_DIM);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(55, 45, 80), 1),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));
        }
    }

    public JComboBox<String> createStyledTableComboBox(String[] options) {
        JComboBox<String> cb = new JComboBox<>(options);
        cb.setFont(new Font("Consolas", Font.BOLD, 11));
        cb.setBackground(new Color(24, 18, 45));
        cb.setForeground(RetroTheme.ACCENT_CYAN);
        cb.setBorder(BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 1));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setFont(new Font("Consolas", Font.BOLD, 11));
                lbl.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
                String text = (value != null) ? value.toString() : "";
                lbl.setText(text);
                if (isSelected) {
                    lbl.setBackground(new Color(45, 30, 75));
                } else {
                    lbl.setBackground(new Color(20, 14, 38));
                }
                if (text.contains("YES") || text.contains("RUNS") || text.contains("HIT") || text.contains("VALID") || text.contains("ALLOCATED") || text.contains("UP")) {
                    lbl.setForeground(RetroTheme.ACCENT_GREEN);
                } else if (text.contains("NO") || text.contains("WAIT") || text.contains("FAULT") || text.contains("TRAP") || text.contains("DENIED") || text.contains("DOWN")) {
                    lbl.setForeground(RetroTheme.ACCENT_PINK);
                } else {
                    lbl.setForeground(RetroTheme.ACCENT_YELLOW);
                }
                return lbl;
            }
        });
        return cb;
    }

    private void populateWordSelectionRow(int r, int c, String[] options) {
        pnlWordSelectRow.removeAll();

        JLabel lblTag = new JLabel("⚡ SELECT ANSWER:");
        lblTag.setFont(new Font("Consolas", Font.BOLD, 9));
        lblTag.setForeground(RetroTheme.ACCENT_YELLOW);
        pnlWordSelectRow.add(lblTag);

        for (String opt : options) {
            JButton btn = new JButton(opt);
            btn.setFont(new Font("Consolas", Font.BOLD, 9));
            btn.setBackground(new Color(25, 20, 48));
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            Color fg = RetroTheme.ACCENT_CYAN;
            Color borderC = new Color(55, 45, 80);
            if (opt.contains("YES") || opt.contains("RUNS") || opt.contains("HIT") || opt.contains("VALID") || opt.contains("ALLOCATED") || opt.contains("UP") || opt.contains("FINISHED")) {
                fg = RetroTheme.ACCENT_GREEN;
                borderC = new Color(30, 90, 50);
            } else if (opt.contains("NO") || opt.contains("WAIT") || opt.contains("FAULT") || opt.contains("TRAP") || opt.contains("DENIED") || opt.contains("DOWN") || opt.equalsIgnoreCase("NONE")) {
                fg = RetroTheme.ACCENT_PINK;
                borderC = new Color(90, 30, 60);
            } else if (opt.contains("Socket") || opt.contains("SOCK")) {
                fg = RetroTheme.ACCENT_YELLOW;
                borderC = new Color(90, 75, 30);
            } else if (opt.startsWith("J")) {
                fg = RetroTheme.ACCENT_YELLOW;
                borderC = new Color(60, 50, 90);
            }
            btn.setForeground(fg);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderC, 1),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));

            btn.addActionListener(e -> {
                if (cipherTable.isEditing()) {
                    cipherTable.getCellEditor().stopCellEditing();
                }
                tableModel.setValueAt(opt, r, c);
                cipherTable.repaint();
                playBlip();
                selectNextBlankCell(r, c);
            });
            pnlWordSelectRow.add(btn);
        }

        pnlWordSelectRow.revalidate();
        pnlWordSelectRow.repaint();
    }

    private void updateCalcToolButtons(int r, int c, String rowName, String colName) {
        if (btnTool1 == null || btnTool2 == null || btnTool3 == null) return;

        switch (currentModuleIdx) {
            case 0: // CPU Scheduling FCFS
                btnTool1.setText("+ BT");
                btnTool1.setToolTipText("Add Burst Time (BT) to Start Time (ST) = Finish Time (FT)");
                btnTool2.setText("- AT");
                btnTool2.setToolTipText("Subtract Arrival Time (AT) from Finish Time (FT) = Turnaround Time (TAT)");
                btnTool3.setText("÷ N (AVG)");
                btnTool3.setToolTipText("Compute Column Average divided by job count");
                break;

            case 1: // Banker's Safety Algorithm
                btnTool1.setText("Max - Alloc");
                btnTool1.setToolTipText("Calculate Need vector = Max Claim - Allocation for this client");
                btnTool2.setText("+ Alloc");
                btnTool2.setToolTipText("Simulate execution release: Work_new = Current Work + Allocation");
                btnTool3.setText("Need <= Work");
                btnTool3.setToolTipText("Audit if this client can be safely satisfied by available vault reserves");
                break;

            case 2: // Frame Hopper MMU Paging
                btnTool1.setText("÷ 1024");
                btnTool1.setToolTipText("Integer quotient: Logical Address / 1024 = Page # (p)");
                btnTool2.setText("% 1024");
                btnTool2.setToolTipText("Modulo remainder: Logical Address % 1024 = Offset (d)");
                btnTool3.setText("F*1024 + d");
                btnTool3.setToolTipText("Physical Address = Frame * 1024 + Offset");
                break;

            case 3: // Super Mario Cache Replacement
                String curPol = (activeQuestion != null && activeQuestion.policy != null) ? activeQuestion.policy : "FIFO";
                btnTool1.setText("Hit/Fault?");
                btnTool1.setToolTipText("Check if incoming page is resident in any of the 3 pipes");
                btnTool2.setText("Victim");
                btnTool2.setToolTipText("Find which pipe slot will be evicted under " + curPol + " policy");
                btnTool3.setText("Hit Ratio");
                btnTool3.setToolTipText("Compute Cache Hit Ratio = (Hits / Total References) * 100%");
                break;

            case 5: // Crane Dynamic Partitioning
                String cranePol = (activeQuestion != null && activeQuestion.policy != null) ? activeQuestion.policy : "FIRST FIT";
                btnTool1.setText("Cap - Dem");
                btnTool1.setToolTipText("Internal Fragmentation = Socket Capacity - Chip Demand");
                btnTool2.setText("Fit Socket");
                btnTool2.setToolTipText("Locate assigned socket and clearance under " + cranePol + " policy");
                btnTool3.setText("Total Frag");
                btnTool3.setToolTipText("Sum internal fragmentation across allocated sockets");
                break;

            case 6: // Pacman Disk Seek
                String pacmanPol = (activeQuestion != null && activeQuestion.policy != null) ? activeQuestion.policy : "FCFS";
                btnTool1.setText("|Δ Cyl|");
                btnTool1.setToolTipText("Traversed Tracks = |Next Track - Current Track|");
                btnTool2.setText("+ Cumul");
                btnTool2.setToolTipText("Cumulative Seek = Running sum of traversed tracks under " + pacmanPol);
                btnTool3.setText("÷ N (Avg)");
                btnTool3.setToolTipText("Average Seek Length = Total Cumulative Seek / Steps");
                break;

            case 7: // Overcooked Multiprogramming Trace // Event-Driven Execution
                btnTool1.setText("1 / nj");
                btnTool1.setToolTipText("%cpu/job = 1 / nj (Equal CPU slicing across active jobs)");
                btnTool2.setText("T - prevT");
                btnTool2.setToolTipText("Elapsed Time (et) = present TIME - previous TIME");
                btnTool3.setText("et * %cpu");
                btnTool3.setToolTipText("Hardware Work per Job (hw/job) = et * (%cpu/job)");
                btnNextBlank.setText("-> NEXT");
                break;

            default:
                btnTool1.setText("x 100%");
                btnTool1.setToolTipText("Multiply ratio by 100%");
                btnTool2.setText("- Base");
                btnTool2.setToolTipText("Subtract Base address");
                btnTool3.setText("÷ N (AVG)");
                btnTool3.setToolTipText("Divide by N");
                break;
        }
    }

    private void onTool1Clicked() {
        int r = (cipherTable != null) ? cipherTable.getSelectedRow() : -1;
        int c = (cipherTable != null) ? cipherTable.getSelectedColumn() : -1;
        if (r < 0 || activeQuestion == null) return;

        switch (currentModuleIdx) {
            case 0: // CPU FCFS: + BT
                Double st = getTableCellDouble(r, 3);
                Double bt = getTableCellDouble(r, 2);
                if (st != null && bt != null) {
                    txtScratchA.setText(formatDouble(st));
                    cmbScratchOp.setSelectedItem("+");
                    txtScratchB.setText(formatDouble(bt));
                    computeScratchpad(false);
                    if (c == 4) insertScratchIntoCell();
                }
                break;

            case 1: // Banker's: Max - Alloc (Need)
                int[] alloc = parseResourceVector(getTableCellString(r, 1), 0, 0, 0);
                int[] max = parseResourceVector(getTableCellString(r, 2), 0, 0, 0);
                int[] need = new int[]{ Math.max(0, max[0] - alloc[0]), Math.max(0, max[1] - alloc[1]), Math.max(0, max[2] - alloc[2]) };
                String needStr = need[0] + " " + need[1] + " " + need[2];
                lblScratchResult.setText(needStr);
                tableModel.setValueAt(needStr, r, 3);
                cipherTable.repaint();
                showToast("Need calculated for row " + r + ": [" + needStr + "]");
                selectNextBlankCell(r, 3);
                break;

            case 2: // Hopper: ÷ 1024
                Double la1 = getTableCellDouble(r, 1);
                if (la1 != null) {
                    txtScratchA.setText(formatDouble(la1));
                    cmbScratchOp.setSelectedItem("÷");
                    txtScratchB.setText("1024");
                    computeScratchpad(false);
                    if (c == 2) insertScratchIntoCell();
                }
                break;

            case 3: // Mario: Hit or Fault?
                int[] stream1 = (activeQuestion != null && activeQuestion.telemetryData.get("stream") != null) ? (int[]) activeQuestion.telemetryData.get("stream") : null;
                boolean[] hits1 = (activeQuestion != null && activeQuestion.telemetryData.get("hitHistory") != null) ? (boolean[]) activeQuestion.telemetryData.get("hitHistory") : null;
                if (stream1 != null && hits1 != null && r < hits1.length) {
                    boolean hit = hits1[r];
                    String statusStr = hit ? "HIT" : "FAULT";
                    tableModel.setValueAt(statusStr, r, 5);
                    cipherTable.repaint();
                    playBlip();
                    showToast(hit ? "🌟 COIN MATCH: P" + stream1[r] + " is resident in pipe -> HIT!" : "💥 PIPE SNAP: P" + stream1[r] + " not resident -> FAULT!");
                    selectNextBlankCell(r, 5);
                }
                break;

            case 5: // Crane: Cap - Size (Frag)
                int[] demands1 = (activeQuestion != null && activeQuestion.telemetryData.get("demands") != null) ? (int[]) activeQuestion.telemetryData.get("demands") : null;
                int[] frags1 = (activeQuestion != null && activeQuestion.telemetryData.get("frags") != null) ? (int[]) activeQuestion.telemetryData.get("frags") : null;
                int[] assignedSocks1 = (activeQuestion != null && activeQuestion.telemetryData.get("assignedSocks") != null) ? (int[]) activeQuestion.telemetryData.get("assignedSocks") : null;
                int[] sockSizes1 = (activeQuestion != null && activeQuestion.telemetryData.get("sockSizes") != null) ? (int[]) activeQuestion.telemetryData.get("sockSizes") : null;

                if (demands1 != null && frags1 != null && r < demands1.length) {
                    int demVal = demands1[r];
                    int fragVal = frags1[r];
                    int capVal = (assignedSocks1 != null && r < assignedSocks1.length && assignedSocks1[r] != -1 && sockSizes1 != null) ? sockSizes1[assignedSocks1[r]] : 0;

                    txtScratchA.setText(String.valueOf(capVal));
                    cmbScratchOp.setSelectedItem("-");
                    txtScratchB.setText(String.valueOf(demVal));
                    lblScratchResult.setText(fragVal + " KB");

                    tableModel.setValueAt(fragVal + " KB", r, 5);
                    cipherTable.repaint();
                    playBlip();
                    showToast(capVal > 0 ? String.format("Frag Computed: %dK Cap - %dK Demand = %d KB", capVal, demVal, fragVal) : "No socket allocated: Frag = 0 KB");
                    selectNextBlankCell(r, 5);
                }
                break;

            case 6: // Pacman: |Next - Cur| & Direction
                Double curT = getTableCellDouble(r, 1);
                Double nextT = getTableCellDouble(r, 2);
                if (curT != null && nextT != null) {
                    int diff = (int) Math.round(Math.abs(nextT - curT));
                    String dirStr = (nextT >= curT) ? "UP (->)" : "DOWN (<-)";
                    txtScratchA.setText(String.valueOf((int) Math.round(nextT)));
                    cmbScratchOp.setSelectedItem("-");
                    txtScratchB.setText(String.valueOf((int) Math.round(curT)));
                    lblScratchResult.setText(diff + " cyl");

                    if (c == 4) {
                        tableModel.setValueAt(dirStr, r, 4);
                        selectNextBlankCell(r, 4);
                    } else {
                        tableModel.setValueAt(diff + " cyl", r, 3);
                        selectNextBlankCell(r, 3);
                    }
                    cipherTable.repaint();
                    playBlip();
                    showToast(String.format("Step #%d: |%d - %d| = %d cyl (%s)", r + 1, (int)Math.round(nextT), (int)Math.round(curT), diff, dirStr));
                }
                break;

            case 7: // Overcooked: 1 / nj (%cpu/job)
                String njStr = getTableCellString(r, 2);
                int scanR = r;
                while ((njStr == null || njStr.trim().isEmpty() || njStr.equals("-")) && scanR > 0) {
                    scanR--;
                    njStr = getTableCellString(scanR, 2);
                }
                if (njStr != null && !njStr.trim().isEmpty() && !njStr.equals("-") && !njStr.equals("?")) {
                    try {
                        int njVal = Integer.parseInt(njStr.trim());
                        double pctCpu = (njVal > 0) ? (1.0 / njVal) : 0.0;
                        pctCpu = Math.round(pctCpu * 1000.0) / 1000.0;
                        txtScratchA.setText("1");
                        cmbScratchOp.setSelectedItem("÷");
                        txtScratchB.setText(String.valueOf(njVal));
                        lblScratchResult.setText(String.format(Locale.US, "%.3f", pctCpu));
                        tableModel.setValueAt(String.format(Locale.US, "%.3f", pctCpu), scanR, 3);
                        cipherTable.repaint();
                        playBlip();
                        showToast(String.format(Locale.US, "%%cpu/job = 1 / %d = %.3f", njVal, pctCpu));
                        selectNextBlankCell(scanR, 3);
                    } catch (Exception ignored) {}
                }
                break;

            default:
                break;
        }
    }

    private void onTool2Clicked() {
        int r = (cipherTable != null) ? cipherTable.getSelectedRow() : -1;
        int c = (cipherTable != null) ? cipherTable.getSelectedColumn() : -1;
        if (r < 0 || activeQuestion == null) return;

        switch (currentModuleIdx) {
            case 0: // CPU FCFS: - AT
                Double ft = getTableCellDouble(r, 4);
                Double at = getTableCellDouble(r, 1);
                if (ft != null && at != null) {
                    txtScratchA.setText(formatDouble(ft));
                    cmbScratchOp.setSelectedItem("-");
                    txtScratchB.setText(formatDouble(at));
                    computeScratchpad(false);
                    if (c == 6) insertScratchIntoCell();
                }
                break;

            case 1: // Banker's: + Alloc (Work)
                int[] curAvail = (activeQuestion.initialAvailable != null) ? activeQuestion.initialAvailable : new int[]{3, 3, 2};
                int[] rAlloc = parseResourceVector(getTableCellString(r, 1), 0, 0, 0);
                int[] work = parseResourceVector(getTableCellString(r, 4), curAvail[0], curAvail[1], curAvail[2]);
                int[] nextWork = new int[]{ work[0] + rAlloc[0], work[1] + rAlloc[1], work[2] + rAlloc[2] };
                String workStr = nextWork[0] + " " + nextWork[1] + " " + nextWork[2];
                lblScratchResult.setText(workStr);
                tableModel.setValueAt(workStr, r, 4);
                cipherTable.repaint();
                showToast("Simulated release: New Work reserves = [" + workStr + "]");
                selectNextBlankCell(r, 4);
                break;

            case 2: // Hopper: % 1024
                Double la2 = getTableCellDouble(r, 1);
                if (la2 != null) {
                    txtScratchA.setText(formatDouble(la2));
                    cmbScratchOp.setSelectedItem("%");
                    txtScratchB.setText("1024");
                    computeScratchpad(false);
                    if (c == 3) insertScratchIntoCell();
                }
                break;

            case 3: // Mario: Victim Eviction
                int[] vHist = (activeQuestion != null && activeQuestion.telemetryData.get("victimHistory") != null) ? (int[]) activeQuestion.telemetryData.get("victimHistory") : null;
                int[][] pHist = (activeQuestion != null && activeQuestion.telemetryData.get("pipeHistory") != null) ? (int[][]) activeQuestion.telemetryData.get("pipeHistory") : null;
                String polName = (activeQuestion != null && activeQuestion.policy != null) ? activeQuestion.policy : "FIFO";
                if (pHist != null && r < pHist.length) {
                    for (int p = 0; p < 3; p++) {
                        String s = getTableCellString(r, p + 2);
                        if (s.equals("?") || s.equals("[ ? ]")) {
                            int pv = pHist[r][p];
                            tableModel.setValueAt(pv == -1 ? "—" : String.valueOf(pv), r, p + 2);
                        }
                    }
                    cipherTable.repaint();
                    playBlip();
                    if (vHist != null && r < vHist.length && vHist[r] != -1) {
                        showToast("🐢 " + polName + " VICTIM: Evicted P" + vHist[r] + " from pipes!");
                    } else {
                        showToast("🐢 " + polName + ": No eviction needed (vacancy available or hit)!");
                    }
                    selectNextBlankCell(r, 2);
                }
                break;

            case 5: // Crane: Find Socket & Clearance
                int[] assignedSocks2 = (activeQuestion != null && activeQuestion.telemetryData.get("assignedSocks") != null) ? (int[]) activeQuestion.telemetryData.get("assignedSocks") : null;
                String[] sockNames2 = (activeQuestion != null && activeQuestion.telemetryData.get("sockNames") != null) ? (String[]) activeQuestion.telemetryData.get("sockNames") : null;
                int[] sockSizes2 = (activeQuestion != null && activeQuestion.telemetryData.get("sockSizes") != null) ? (int[]) activeQuestion.telemetryData.get("sockSizes") : null;
                int[] sockBases2 = (activeQuestion != null && activeQuestion.telemetryData.get("sockBases") != null) ? (int[]) activeQuestion.telemetryData.get("sockBases") : null;
                int[] frags2 = (activeQuestion != null && activeQuestion.telemetryData.get("frags") != null) ? (int[]) activeQuestion.telemetryData.get("frags") : null;
                String curPolicy2 = (activeQuestion != null && activeQuestion.policy != null) ? activeQuestion.policy : "FIRST FIT";

                if (assignedSocks2 != null && r < assignedSocks2.length) {
                    int sIdx = assignedSocks2[r];
                    if (sIdx != -1 && sockNames2 != null && sockSizes2 != null && sockBases2 != null) {
                        String sName = sockNames2[sIdx];
                        int sCap = sockSizes2[sIdx];
                        int sBase = sockBases2[sIdx];
                        int sFrag = (frags2 != null && r < frags2.length) ? frags2[r] : 0;

                        tableModel.setValueAt(sBase + " KB", r, 2);
                        tableModel.setValueAt(sCap + " KB", r, 3);
                        tableModel.setValueAt(sName + " [" + sCap + "K]", r, 4);
                        tableModel.setValueAt(sFrag + " KB", r, 5);
                        tableModel.setValueAt("ALLOCATED [OK]", r, 6);
                        showToast(String.format("✔ %s: P%d slotted into %s [%dK] (Base: %dK, Frag: %dK)", curPolicy2, r + 1, sName, sCap, sBase, sFrag));
                    } else {
                        tableModel.setValueAt("—", r, 2);
                        tableModel.setValueAt("—", r, 3);
                        tableModel.setValueAt("NONE", r, 4);
                        tableModel.setValueAt("0 KB", r, 5);
                        tableModel.setValueAt("DENIED: NO FIT", r, 6);
                        showToast(String.format("⚠ %s: P%d denied allocation: No available socket fits demand!", curPolicy2, r + 1));
                    }
                    cipherTable.repaint();
                    playBlip();
                    selectNextBlankCell(r, 4);
                }
                break;

            case 6: // Pacman: + Cumulative Seek
                int[] cumSteps = (activeQuestion != null && activeQuestion.telemetryData.get("cumulativeSteps") != null)
                        ? (int[]) activeQuestion.telemetryData.get("cumulativeSteps") : null;
                int cumVal = 0;
                if (cumSteps != null && r < cumSteps.length) {
                    cumVal = cumSteps[r];
                } else {
                    for (int i = 0; i <= r; i++) {
                        Double tr = getTableCellDouble(i, 3);
                        if (tr != null) cumVal += (int) Math.round(tr);
                    }
                }
                lblScratchResult.setText(cumVal + " cyl");
                tableModel.setValueAt(cumVal + " cyl", r, 5);
                cipherTable.repaint();
                playBlip();
                showToast(String.format("Cumulative Seek at Step #%d = %d cyl", r + 1, cumVal));
                selectNextBlankCell(r, 5);
                break;

            case 7: // Overcooked: T - prevT (Elapsed Time et)
                String curTStr = getTableCellString(r, 0);
                int curScan = r;
                while ((curTStr == null || curTStr.trim().isEmpty() || curTStr.equals("-")) && curScan > 0) {
                    curScan--;
                    curTStr = getTableCellString(curScan, 0);
                }
                String prevTStr = null;
                for (int p = curScan - 1; p >= 0; p--) {
                    String pt = getTableCellString(p, 0);
                    if (pt != null && !pt.trim().isEmpty() && !pt.equals("-") && !pt.equals("?")) {
                        prevTStr = pt;
                        break;
                    }
                }
                if (curTStr != null && prevTStr != null && !curTStr.equals("?") && !prevTStr.equals("?")) {
                    try {
                        double curT = Double.parseDouble(curTStr.trim());
                        double prevT = Double.parseDouble(prevTStr.trim());
                        double et = Math.max(0.0, Math.round((curT - prevT) * 100.0) / 100.0);
                        txtScratchA.setText(formatDouble(curT));
                        cmbScratchOp.setSelectedItem("-");
                        txtScratchB.setText(formatDouble(prevT));
                        lblScratchResult.setText(String.format(Locale.US, "%.2f", et));
                        tableModel.setValueAt(String.format(Locale.US, "%.2f", et), curScan, 4);
                        cipherTable.repaint();
                        playBlip();
                        showToast(String.format(Locale.US, "et = %.2f - %.2f = %.2f", curT, prevT, et));
                        selectNextBlankCell(curScan, 4);
                    } catch (Exception ignored) {}
                }
                break;

            default:
                break;
        }
    }

    private void onTool3Clicked() {
        int r = (cipherTable != null) ? cipherTable.getSelectedRow() : -1;
        int c = (cipherTable != null) ? cipherTable.getSelectedColumn() : -1;
        if (r < 0 || activeQuestion == null) return;

        switch (currentModuleIdx) {
            case 0: // CPU FCFS: ÷ N (AVG)
                int n0 = Math.max(1, activeQuestion.worksheetData.length - 1);
                int targetCol0 = (c == 6 || (cipherTable.getColumnName(c).contains("TURN"))) ? 6 : 5;
                double tot0 = 0.0;
                for (int i = 0; i < n0; i++) {
                    Double val = getTableCellDouble(i, targetCol0);
                    if (val != null) tot0 += val;
                }
                double avg0 = tot0 / n0;
                txtScratchA.setText(formatDouble(tot0));
                cmbScratchOp.setSelectedItem("÷");
                txtScratchB.setText(String.valueOf(n0));
                lblScratchResult.setText(formatDouble(avg0));
                if (r == n0) insertScratchIntoCell();
                break;

            case 7: // Overcooked: et * %cpu (hw/job)
                int hwScan = r;
                Double etVal = getTableCellDouble(hwScan, 4);
                Double pctVal = getTableCellDouble(hwScan, 3);
                while ((etVal == null || pctVal == null) && hwScan > 0) {
                    hwScan--;
                    if (etVal == null) etVal = getTableCellDouble(hwScan, 4);
                    if (pctVal == null) pctVal = getTableCellDouble(hwScan, 3);
                }
                if (etVal != null && pctVal != null) {
                    double hw = Math.round((etVal * pctVal) * 1000.0) / 1000.0;
                    txtScratchA.setText(formatDouble(etVal));
                    cmbScratchOp.setSelectedItem("×");
                    txtScratchB.setText(formatDouble(pctVal));
                    lblScratchResult.setText(String.format(Locale.US, "%.3f", hw));
                    tableModel.setValueAt(String.format(Locale.US, "%.3f", hw), hwScan, 5);
                    cipherTable.repaint();
                    playBlip();
                    showToast(String.format(Locale.US, "hw/job = %.2f * %.3f = %.3f", etVal, pctVal, hw));
                    selectNextBlankCell(hwScan, 5);
                }
                break;

            case 1: // Banker's: Need <= Work ?
                int[] avail = (activeQuestion.initialAvailable != null) ? activeQuestion.initialAvailable : new int[]{3, 3, 2};
                int[] nVec = parseResourceVector(getTableCellString(r, 3), 0, 0, 0);
                int[] wVec = parseResourceVector(getTableCellString(r, 4), avail[0], avail[1], avail[2]);
                boolean satisfied = (nVec[0] <= wVec[0]) && (nVec[1] <= wVec[1]) && (nVec[2] <= wVec[2]);

                tableModel.setValueAt(satisfied ? "YES" : "NO", r, 5);
                tableModel.setValueAt(satisfied ? "RUNS [EXEC]" : "WAIT [BLOCKED]", r, 6);
                cipherTable.repaint();
                showToast(satisfied ? "✔ CLIENT " + r + " CAN BE SAFELY SATISFIED!" : "⚠ CLIENT " + r + " EXCEEDS RESERVES (MUST WAIT)!");
                selectNextBlankCell(r, 6);
                break;

            case 2: // Hopper: F*1024 + d
                String frameStr = getTableCellString(r, 4);
                Double off = getTableCellDouble(r, 3);
                int frameNum = 0;
                if (frameStr.startsWith("F") || frameStr.startsWith("f")) {
                    try { frameNum = Integer.parseInt(frameStr.substring(1).trim()); } catch (Exception ignored) {}
                }
                if (frameNum > 0 && off != null) {
                    long pa = (long) frameNum * 1024 + Math.round(off);
                    lblScratchResult.setText(String.valueOf(pa));
                    tableModel.setValueAt(pa + " B", r, 5);
                    cipherTable.repaint();
                    selectNextBlankCell(r, 5);
                }
                break;

            case 3: // Mario: Hit Ratio %
                int steps3 = Math.max(1, activeQuestion.worksheetData.length - 1);
                int hCount = 0;
                int fCount = 0;
                for (int i = 0; i < steps3; i++) {
                    String st = getTableCellString(i, 5);
                    if (st.contains("HIT")) hCount++;
                    else if (st.contains("FAULT")) fCount++;
                }
                double hr = (hCount * 100.0) / steps3;
                txtScratchA.setText(String.valueOf(hCount * 100));
                cmbScratchOp.setSelectedItem("÷");
                txtScratchB.setText(String.valueOf(steps3));
                lblScratchResult.setText(String.format(Locale.US, "%.1f%%", hr));
                tableModel.setValueAt(fCount + " FAULTS", steps3, 4);
                tableModel.setValueAt(String.format(Locale.US, "%.1f%% HITS", hr), steps3, 5);
                cipherTable.repaint();
                playBlip();
                showToast(String.format("Summary Computed: %d Faults, %d Hits, Hit Ratio: %.1f%%", fCount, hCount, hr));
                break;

            case 5: // Crane: Total Frag & Utilization
                int[] allFrags = (activeQuestion != null && activeQuestion.telemetryData.get("frags") != null) ? (int[]) activeQuestion.telemetryData.get("frags") : null;
                int[] allDemands = (activeQuestion != null && activeQuestion.telemetryData.get("demands") != null) ? (int[]) activeQuestion.telemetryData.get("demands") : null;
                int[] allAssigned = (activeQuestion != null && activeQuestion.telemetryData.get("assignedSocks") != null) ? (int[]) activeQuestion.telemetryData.get("assignedSocks") : null;
                int[] allSizes = (activeQuestion != null && activeQuestion.telemetryData.get("sockSizes") != null) ? (int[]) activeQuestion.telemetryData.get("sockSizes") : null;

                int totalFrag = 0;
                int totalPayload = 0;
                if (allFrags != null) {
                    for (int f : allFrags) totalFrag += f;
                }
                if (allDemands != null && allAssigned != null) {
                    for (int i = 0; i < allDemands.length; i++) {
                        if (allAssigned[i] != -1) totalPayload += allDemands[i];
                    }
                }
                int totalRam = 0;
                if (allSizes != null) {
                    for (int s : allSizes) totalRam += s;
                } else {
                    totalRam = 1700;
                }
                int utilPct = totalRam > 0 ? (totalPayload * 100) / totalRam : 0;

                txtScratchA.setText(String.valueOf(totalFrag));
                cmbScratchOp.setSelectedItem("+");
                txtScratchB.setText(String.valueOf(totalPayload));
                lblScratchResult.setText(String.format("UTIL: %d%%", utilPct));
                showToast(String.format("RAM Motherboard Stats: Total Frag = %d KB | Active Payload = %d KB | Total RAM = %d KB | Utilization = %d%%",
                        totalFrag, totalPayload, totalRam, utilPct));
                playBlip();
                break;

            case 6: // Pacman: ÷ N (Avg Seek) & Total Seek
                int steps = Math.max(1, activeQuestion.worksheetData.length - 1);
                Integer totFromTelemetry = (activeQuestion != null && activeQuestion.telemetryData.get("totalSeek") != null)
                        ? (Integer) activeQuestion.telemetryData.get("totalSeek") : null;
                Double avgFromTelemetry = (activeQuestion != null && activeQuestion.telemetryData.get("avgSeek") != null)
                        ? (Double) activeQuestion.telemetryData.get("avgSeek") : null;

                double totSeek = (totFromTelemetry != null) ? totFromTelemetry : 0.0;
                double avgSeek = (avgFromTelemetry != null) ? avgFromTelemetry : (totSeek / steps);

                txtScratchA.setText(formatDouble(totSeek));
                cmbScratchOp.setSelectedItem("÷");
                txtScratchB.setText(String.valueOf(steps));
                lblScratchResult.setText(String.format(Locale.US, "%.2f cyl", avgSeek));

                tableModel.setValueAt(Math.round(totSeek) + " cyl", steps, 3);
                tableModel.setValueAt(String.format(Locale.US, "%.2f cyl", avgSeek), steps, 5);
                cipherTable.repaint();
                playBlip();
                showToast(String.format("Summary Computed: Total = %d cyl | Avg Seek Length = %.2f cyl", Math.round(totSeek), avgSeek));
                break;

            default:
                break;
        }
    }

    private void updateModeButtons() {
        boolean isFill = (currentMode == QuizManager.Mode.FILL_MISSING);
        btnModeFill.setForeground(isFill ? RetroTheme.ACCENT_CYAN : RetroTheme.TEXT_WHITE);
        btnModeSolve.setForeground(!isFill ? RetroTheme.ACCENT_CYAN : RetroTheme.TEXT_WHITE);
        repaint();
    }

    /**
     * Right Side Panel: System Archives Stage Dossier + 8 Category Visualizers + Silicon Formulas
     */
    private JPanel createIntelTerminalSidePanel() {
        JPanel side = new JPanel(new BorderLayout(0, 6));
        side.setOpaque(false);

        // 1. TOP: Stage Intel Dossier (Mascot Chamber + Directive + Category Badge)
        side.add(createStageDossierCard(), BorderLayout.NORTH);

        // 2. CENTER: Category-Specific Visualizer Engine (8 Distinct Category Interfaces!)
        side.add(createCategorySpecificVisualizer(), BorderLayout.CENTER);

        // 3. BOTTOM: Silicon Formula & Rule IC Chips
        side.add(createFormulaChipsCard(), BorderLayout.SOUTH);

        return side;
    }

    private JPanel createStageDossierCard() {
        JPanel card = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color activeTheme = MOD_COLORS[currentModuleIdx];

                g2.setColor(new Color(18, 13, 34, 250));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                g2.setColor(new Color(activeTheme.getRed(), activeTheme.getGreen(), activeTheme.getBlue(), 100));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                // Top highlight bar
                g2.setColor(activeTheme);
                g2.fillRect(14, 0, getWidth() - 28, 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        card.setPreferredSize(new Dimension(340, 66));

        // Mascot Chamber on Left
        JPanel mascotChamber = new JPanel() {
            @Override
            public Dimension getPreferredSize() { return new Dimension(54, 52); }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(new Color(10, 6, 20));
                g2.fillRoundRect(0, 0, w, h, 6, 6);
                g2.setColor(new Color(50, 36, 75));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);

                // Radar reticle in background
                int cx = w / 2;
                int cy = h / 2 - 2;
                g2.setColor(new Color(5, 217, 232, 40));
                g2.drawOval(cx - 15, cy - 15, 30, 30);
                g2.drawLine(cx - 18, cy, cx + 18, cy);
                g2.drawLine(cx, cy - 18, cx, cy + 18);

                // Mascot Sprite with subtle floating bob
                BufferedImage icon = getMascotSprite(currentModuleIdx, 30);
                if (icon != null) {
                    int bob = (int) (Math.sin(pulseTicks * 0.08) * 2.0);
                    g2.drawImage(icon, (w - 30) / 2, (h - 30) / 2 + bob, 30, 30, null);
                }
                g2.dispose();
            }
        };
        mascotChamber.setOpaque(false);
        card.add(mascotChamber, BorderLayout.WEST);

        // Dossier Info on Right
        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        lblDossierTitle = new JLabel("STAGE 01: SPACE INVADERS // CPU");
        lblDossierTitle.setFont(new Font("Consolas", Font.BOLD, 12));
        lblDossierTitle.setForeground(RetroTheme.ACCENT_YELLOW);
        infoPanel.add(lblDossierTitle);

        infoPanel.add(Box.createVerticalStrut(3));

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        badgeRow.setOpaque(false);

        lblDossierCategory = new JLabel(" [ CORE CPU SCHEDULING ] ");
        lblDossierCategory.setFont(new Font("Consolas", Font.BOLD, 10));
        lblDossierCategory.setForeground(RetroTheme.BG_DARK);
        lblDossierCategory.setOpaque(true);
        lblDossierCategory.setBackground(MOD_COLORS[currentModuleIdx]);
        lblDossierCategory.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
        badgeRow.add(lblDossierCategory);

        JLabel lblSync = new JLabel("● SIMULATOR ONLINE");
        lblSync.setFont(new Font("Consolas", Font.BOLD, 10));
        lblSync.setForeground(RetroTheme.ACCENT_GREEN);
        badgeRow.add(lblSync);
        infoPanel.add(badgeRow);

        lblDossierDesc = new JLabel(); // Dummy to avoid null pointer

        card.add(infoPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createCategorySpecificVisualizer() {
        JPanel vizPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Dark background
                g2.setColor(new Color(14, 10, 26, 250));
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                Color activeTheme = MOD_COLORS[currentModuleIdx];
                g2.setColor(new Color(activeTheme.getRed(), activeTheme.getGreen(), activeTheme.getBlue(), 90));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                switch (currentModuleIdx) {
                    case 0: drawCpuRunwayGantt(g2, w, h, activeTheme); break;
                    case 1: drawBankerResourceVault(g2, w, h, activeTheme); break;
                    case 2: drawMmuPagingBus(g2, w, h, activeTheme); break;
                    case 3: drawMarioCacheBuffer(g2, w, h, activeTheme); break;
                    case 4: drawSnakeSegmentTrapRadar(g2, w, h, activeTheme); break;
                    case 5: drawCyberCraneRamPartitions(g2, w, h, activeTheme); break;
                    case 6: drawPacmanDiskPlatter(g2, w, h, activeTheme); break;
                    case 7: drawOvercookedConcurrencyCurve(g2, w, h, activeTheme); break;
                }
                g2.dispose();
            }
        };
        vizPanel.setOpaque(false);
        vizPanel.setPreferredSize(new Dimension(340, 210));
        pnlCategoryVisualizer = vizPanel;
        return vizPanel;
    }

    // ── 8 DISTINCT CATEGORY INTERFACE VISUALIZERS ────────────────────────────

    // Category 01: CPU Runway & Execution Timeline (Option 1: Live Interactive Self-Check Gantt Timeline)
    private void drawCpuRunwayGantt(Graphics2D g2, int w, int h, Color theme) {
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        g2.setColor(RetroTheme.ACCENT_CYAN);
        g2.drawString("✈ CPU DISPATCH RUNWAY // CORE TIMELINE AUDITOR", 10, 16);

        g2.setFont(new Font("Consolas", Font.PLAIN, 9));
        g2.setColor(new Color(180, 190, 220));
        g2.drawString("Live Interactive Gantt Chart & Overlap Collision Detector", 10, 29);

        if (activeQuestion == null || tableModel == null || activeQuestion.worksheetData == null) return;
        int waveCount = Math.min(activeQuestion.worksheetData.length - 1, 7);
        if (waveCount <= 0) return;

        // 1. Read live user entries from tableModel
        String[] waveLabels = new String[waveCount];
        Double[] waveAT = new Double[waveCount];
        Double[] waveBT = new Double[waveCount];
        Double[] waveST = new Double[waveCount];
        Double[] waveFT = new Double[waveCount];
        boolean[] isSolved = new boolean[waveCount];
        boolean[] hasOverlap = new boolean[waveCount];
        boolean[] prematureStart = new boolean[waveCount];
        boolean[] invalidDuration = new boolean[waveCount];

        int solvedCount = 0;
        double maxEnteredFT = 0.0;

        for (int i = 0; i < waveCount; i++) {
            waveLabels[i] = "W" + (i + 1);
            waveAT[i] = getTableCellDouble(i, 1);
            waveBT[i] = getTableCellDouble(i, 2);
            waveST[i] = getTableCellDouble(i, 3);
            waveFT[i] = getTableCellDouble(i, 4);

            if (waveST[i] != null && waveFT[i] != null) {
                isSolved[i] = true;
                solvedCount++;
                if (waveFT[i] > maxEnteredFT) maxEnteredFT = waveFT[i];

                if (waveFT[i] <= waveST[i]) {
                    invalidDuration[i] = true;
                }
                if (waveAT[i] != null && waveST[i] < waveAT[i] - 0.001) {
                    prematureStart[i] = true;
                }
            }
        }

        // 2. Real-time Core Overlap Detection (Single CPU Core execution)
        boolean overlapConflict = false;
        String conflictPair = "";
        for (int j = 0; j < waveCount; j++) {
            if (!isSolved[j] || invalidDuration[j]) continue;
            for (int k = j + 1; k < waveCount; k++) {
                if (!isSolved[k] || invalidDuration[k]) continue;
                double overlapStart = Math.max(waveST[j], waveST[k]);
                double overlapEnd = Math.min(waveFT[j], waveFT[k]);
                if (overlapStart < overlapEnd - 0.001) {
                    overlapConflict = true;
                    hasOverlap[j] = true;
                    hasOverlap[k] = true;
                    conflictPair = waveLabels[j] + " & " + waveLabels[k];
                }
            }
        }

        // 3. Stable Time Horizon
        double theoreticalFT = 10.0;
        if (activeQuestion.answerKey != null && activeQuestion.answerKey.length > waveCount - 1) {
            try {
                String akFt = activeQuestion.answerKey[waveCount - 1][4];
                if (akFt != null) theoreticalFT = Double.parseDouble(akFt.trim());
            } catch (Exception ignored) {}
        }
        double timeHorizon = Math.max(theoreticalFT, Math.max(10.0, maxEnteredFT));

        // 4. Draw Timeline Chamber
        int rx0 = 12;
        int rw = w - 24;
        int ry = 36;
        int rh = 34;

        g2.setColor(new Color(8, 6, 18));
        g2.fillRoundRect(rx0, ry, rw, rh, 6, 6);
        g2.setColor(new Color(45, 35, 70));
        g2.drawRoundRect(rx0, ry, rw, rh, 6, 6);

        // Grid markings inside chamber
        g2.setColor(new Color(30, 24, 48));
        for (double t = 2.0; t < timeHorizon; t += 2.0) {
            int gx = rx0 + (int) Math.round((t / timeHorizon) * rw);
            g2.drawLine(gx, ry + 1, gx, ry + rh - 1);
        }

        // 5. Draw IDLE Gaps
        java.util.List<Integer> sortedSolved = new java.util.ArrayList<>();
        for (int i = 0; i < waveCount; i++) {
            if (isSolved[i] && !invalidDuration[i]) sortedSolved.add(i);
        }
        sortedSolved.sort((a, b) -> Double.compare(waveST[a], waveST[b]));

        // Idle before first wave
        if (!sortedSolved.isEmpty()) {
            int firstIdx = sortedSolved.get(0);
            if (waveST[firstIdx] > 0.1) {
                int ix2 = rx0 + (int) Math.round((waveST[firstIdx] / timeHorizon) * rw);
                drawHatchedIdleBlock(g2, rx0 + 1, ry + 1, Math.max(4, ix2 - rx0 - 1), rh - 2);
            }
        }
        // Idle between non-overlapping waves
        for (int idx = 0; idx < sortedSolved.size() - 1; idx++) {
            int curr = sortedSolved.get(idx);
            int next = sortedSolved.get(idx + 1);
            if (waveST[next] > waveFT[curr] + 0.1 && !hasOverlap[curr] && !hasOverlap[next]) {
                int ix1 = rx0 + (int) Math.round((waveFT[curr] / timeHorizon) * rw);
                int ix2 = rx0 + (int) Math.round((waveST[next] / timeHorizon) * rw);
                drawHatchedIdleBlock(g2, ix1, ry + 1, Math.max(4, ix2 - ix1), rh - 2);
            }
        }

        // Draw Live Solved Wave Blocks
        for (int i = 0; i < waveCount; i++) {
            if (!isSolved[i]) continue;
            int bx1 = rx0 + (int) Math.round((waveST[i] / timeHorizon) * rw);
            int bx2 = rx0 + (int) Math.round((waveFT[i] / timeHorizon) * rw);
            int bw = Math.max(16, bx2 - bx1);

            Color bc = STAGE_THEME_COLORS[i % STAGE_THEME_COLORS.length];

            if (hasOverlap[i]) {
                int glowAlpha = (int) (180 + 75 * Math.sin(pulseTicks * 0.15));
                g2.setColor(new Color(255, 30, 60, Math.min(255, Math.max(100, glowAlpha))));
                g2.fillRoundRect(bx1, ry + 2, bw, rh - 4, 4, 4);

                g2.setColor(new Color(255, 200, 200, 160));
                g2.drawLine(bx1 + 3, ry + rh - 4, bx1 + 10, ry + 3);
                g2.drawLine(bx1 + bw - 10, ry + rh - 4, bx1 + bw - 3, ry + 3);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.drawString("⚠ " + waveLabels[i], bx1 + 3, ry + 13);

                g2.setFont(new Font("Consolas", Font.BOLD, 8));
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawString(formatDouble(waveST[i]) + "-" + formatDouble(waveFT[i]), bx1 + 3, ry + rh - 6);
            } else if (invalidDuration[i] || prematureStart[i]) {
                g2.setColor(new Color(255, 140, 0, 210));
                g2.fillRoundRect(bx1, ry + 2, bw, rh - 4, 4, 4);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.drawString("⚠ " + waveLabels[i], bx1 + 3, ry + 13);
                g2.setFont(new Font("Consolas", Font.BOLD, 8));
                g2.drawString("ERR", bx1 + 3, ry + rh - 6);
            } else {
                g2.setColor(new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), 210));
                g2.fillRoundRect(bx1, ry + 2, bw, rh - 4, 4, 4);

                // Top edge shine
                g2.setColor(new Color(255, 255, 255, 70));
                g2.drawLine(bx1 + 2, ry + 3, bx1 + bw - 2, ry + 3);

                g2.setColor(bc.brighter());
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(bx1, ry + 2, bw, rh - 4, 4, 4);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.drawString(waveLabels[i], bx1 + 4, ry + 13);

                g2.setFont(new Font("Consolas", Font.PLAIN, 8));
                g2.setColor(new Color(230, 245, 255));
                g2.drawString(formatDouble(waveST[i]) + "-" + formatDouble(waveFT[i]), bx1 + 4, ry + rh - 6);
            }
        }

        // 6. Draw Timeline Axis Ticks & Time Numbers
        int axisY = ry + rh + 2;
        g2.setColor(new Color(70, 75, 110));
        g2.drawLine(rx0, axisY, rx0 + rw, axisY);

        g2.setFont(new Font("Consolas", Font.PLAIN, 8));
        g2.setColor(new Color(150, 160, 190));
        g2.drawLine(rx0, axisY, rx0, axisY + 3);
        g2.drawString("0", rx0, axisY + 11);

        for (int i = 0; i < waveCount; i++) {
            if (!isSolved[i]) continue;
            int sx = rx0 + (int) Math.round((waveST[i] / timeHorizon) * rw);
            int fx = rx0 + (int) Math.round((waveFT[i] / timeHorizon) * rw);
            g2.drawLine(sx, axisY, sx, axisY + 3);
            g2.drawString(formatDouble(waveST[i]), Math.max(rx0 + 1, sx - 4), axisY + 11);
            g2.drawLine(fx, axisY, fx, axisY + 3);
            g2.drawString(formatDouble(waveFT[i]), Math.min(rx0 + rw - 14, fx - 4), axisY + 11);
        }
        g2.drawLine(rx0 + rw, axisY, rx0 + rw, axisY + 3);
        g2.drawString(formatDouble(timeHorizon), rx0 + rw - 14, axisY + 11);

        // 7. Awaiting Waves Queue Dock (if any unsolved waves)
        int dockY = axisY + 16;
        if (solvedCount < waveCount) {
            g2.setFont(new Font("Consolas", Font.BOLD, 8));
            g2.setColor(new Color(130, 140, 175));
            g2.drawString("AWAITING SOLVED TIMESTAMPS:", rx0, dockY);

            int chipX = rx0 + 130;
            int chipY = dockY - 9;
            for (int i = 0; i < waveCount; i++) {
                if (!isSolved[i]) {
                    Color bc = STAGE_THEME_COLORS[i % STAGE_THEME_COLORS.length];
                    int chipW = 28;
                    g2.setColor(new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), 30));
                    g2.fillRoundRect(chipX, chipY, chipW, 13, 3, 3);
                    g2.setColor(new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), (int) (110 * glowPulse)));
                    g2.drawRoundRect(chipX, chipY, chipW, 13, 3, 3);
                    g2.setColor(new Color(210, 220, 245));
                    g2.drawString(waveLabels[i], chipX + 5, chipY + 10);
                    chipX += chipW + 4;
                    if (chipX > rx0 + rw - 30) break;
                }
            }
        }

        // 8. Schedule Telemetry & Diagnostic Status Card
        int cardY = dockY + (solvedCount < waveCount ? 14 : 6);
        int cardH = h - cardY - 6;
        if (cardH > 30) {
            if (overlapConflict) {
                // Hazard banner: Overlap
                g2.setColor(new Color(38, 10, 18, 250));
                g2.fillRoundRect(10, cardY, w - 20, cardH, 6, 6);
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(10, cardY, w - 20, cardH, 6, 6);

                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(new Color(255, 80, 100));
                g2.drawString("⚠ CORE HAZARD: OVERLAP DETECTED (" + conflictPair + ")", 16, cardY + 13);

                g2.setFont(new Font("Consolas", Font.PLAIN, 8));
                g2.setColor(new Color(255, 200, 210));
                g2.drawString("Waves execute simultaneously on single core!", 16, cardY + 25);
                if (cardH > 42) {
                    g2.drawString("In FCFS, Wave Start (ST) must be >= previous Wave Finish (FT).", 16, cardY + 37);
                }
            } else if (hasAny(prematureStart)) {
                g2.setColor(new Color(38, 25, 10, 250));
                g2.fillRoundRect(10, cardY, w - 20, cardH, 6, 6);
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawRoundRect(10, cardY, w - 20, cardH, 6, 6);

                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawString("⚠ TIMING ANOMALY: PREMATURE DISPATCH", 16, cardY + 13);
                g2.setFont(new Font("Consolas", Font.PLAIN, 8));
                g2.setColor(new Color(255, 230, 180));
                g2.drawString("A wave started before its Arrival Time (ST < AT)!", 16, cardY + 25);
            } else if (hasAny(invalidDuration)) {
                g2.setColor(new Color(38, 25, 10, 250));
                g2.fillRoundRect(10, cardY, w - 20, cardH, 6, 6);
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawRoundRect(10, cardY, w - 20, cardH, 6, 6);

                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawString("⚠ TIMING ERROR: FINISH <= START", 16, cardY + 13);
                g2.setFont(new Font("Consolas", Font.PLAIN, 8));
                g2.setColor(new Color(255, 230, 180));
                g2.drawString("Finish Time must be greater than Start Time (FT = ST + BT).", 16, cardY + 25);
            } else if (solvedCount == waveCount) {
                // 100% Solvent and synchronized
                g2.setColor(new Color(10, 38, 20, 250));
                g2.fillRoundRect(10, cardY, w - 20, cardH, 6, 6);
                g2.setColor(RetroTheme.ACCENT_GREEN);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(10, cardY, w - 20, cardH, 6, 6);

                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(RetroTheme.ACCENT_GREEN);
                g2.drawString("✔ SCHEDULE AUDIT: 100% SOLVENT // PIPELINE SYNCHRONIZED", 16, cardY + 13);
                g2.setFont(new Font("Consolas", Font.PLAIN, 8));
                g2.setColor(new Color(200, 255, 220));
                g2.drawString("All " + waveCount + " waves executed sequentially with zero core collisions!", 16, cardY + 25);
                if (cardH > 42) {
                    g2.drawString("Pipeline execution timeline verified conflict-free. Ready for submission!", 16, cardY + 37);
                }
            } else {
                // Working state
                g2.setColor(new Color(15, 12, 30, 240));
                g2.fillRoundRect(10, cardY, w - 20, cardH, 6, 6);
                g2.setColor(new Color(50, 40, 75));
                g2.drawRoundRect(10, cardY, w - 20, cardH, 6, 6);

                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.drawString("⚡ LIVE DISPATCH AUDITOR // PIPELINE READY", 16, cardY + 13);
                g2.setFont(new Font("Consolas", Font.PLAIN, 8));
                g2.setColor(new Color(190, 200, 230));
                g2.drawString("Milestones plot in real-time as you solve Start (ST) & Finish (FT).", 16, cardY + 25);
                if (cardH > 42) {
                    g2.drawString("Auto-audits for overlap conflicts and idle queue gaps.", 16, cardY + 37);
                }
            }
        }
    }

    private void drawHatchedIdleBlock(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(15, 12, 28));
        g2.fillRect(x, y, w, h);
        g2.setColor(new Color(50, 45, 75, 130));
        for (int d = -h; d < w; d += 6) {
            int x1 = Math.max(x, x + d);
            int y1 = Math.max(y, y + (d < 0 ? -d : 0));
            int x2 = Math.min(x + w, x + d + h);
            int y2 = Math.min(y + h, y + (d < 0 ? 0 : d) + h);
            g2.drawLine(x1, y1, x2, y2);
        }
        if (w >= 20) {
            g2.setFont(new Font("Consolas", Font.BOLD, 8));
            g2.setColor(new Color(140, 150, 180));
            g2.drawString("IDLE", x + (w - 18) / 2, y + (h + 8) / 2);
        }
    }

    private boolean hasAny(boolean[] arr) {
        for (boolean b : arr) if (b) return true;
        return false;
    }

    // Category 02: Banker's Grand Central Cyber Vault & Live Safety Audit Engine
    private void drawBankerResourceVault(Graphics2D g2, int w, int h, Color theme) {
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        g2.drawString("🛡 THE GRAND CENTRAL CYBER VAULT // AVAILABLE RESERVES", 10, 16);

        g2.setFont(new Font("Consolas", Font.PLAIN, 9));
        g2.setColor(new Color(180, 190, 220));
        g2.drawString("Vault Reserves [AVAIL] & Live Client Deadlock Audit", 10, 29);

        int[] avail = (activeQuestion != null && activeQuestion.initialAvailable != null)
                ? activeQuestion.initialAvailable
                : new int[]{ 3, 3, 2 };

        int rx0 = 12;
        int rw = w - 24;
        int ry = 35;
        int vaultW = (rw - 8) / 3;
        int vaultH = 46;

        String[] vaultNames = { "CORE A (GOLD)", "CORE B (DATA)", "CORE C (PLASMA)" };
        Color[] vaultColors = { RetroTheme.ACCENT_YELLOW, RetroTheme.ACCENT_CYAN, RetroTheme.ACCENT_PINK };
        Color[] vaultBgs = { new Color(28, 22, 10), new Color(10, 26, 30), new Color(30, 10, 26) };

        // 1. Draw The 3 Cyber Vaults (AVAIL Numbers clearly separated)
        for (int i = 0; i < 3; i++) {
            int vx = rx0 + i * (vaultW + 4);
            g2.setColor(vaultBgs[i]);
            g2.fillRoundRect(vx, ry, vaultW, vaultH, 6, 6);

            Color vc = vaultColors[i];
            g2.setColor(new Color(vc.getRed(), vc.getGreen(), vc.getBlue(), (int) (120 + 40 * Math.sin(pulseTicks * 0.08 + i))));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(vx, ry, vaultW, vaultH, 6, 6);

            // Small top tag
            g2.setFont(new Font("Consolas", Font.BOLD, 8));
            g2.setColor(new Color(vc.getRed(), vc.getGreen(), vc.getBlue(), 180));
            g2.drawString("AVAIL [" + (char)('A' + i) + "]", vx + 5, ry + 11);

            // Big numerical asset reserve number
            g2.setFont(new Font("Consolas", Font.BOLD, 17));
            g2.setColor(vc);
            g2.drawString(String.valueOf(avail[i]), vx + vaultW - 18, ry + 22);

            // Label
            g2.setFont(new Font("Consolas", Font.BOLD, 8));
            g2.setColor(new Color(230, 235, 255));
            g2.drawString(vaultNames[i], vx + 5, ry + vaultH - 6);
        }

        // 2. Client Safety Inspection & Vector Decomposition
        int auditY = ry + vaultH + 6;
        int auditH = h - auditY - 6;

        int selR = (cipherTable != null && cipherTable.getSelectedRow() >= 0) ? cipherTable.getSelectedRow() : 0;
        int rowCount = (activeQuestion != null && activeQuestion.worksheetData != null) ? activeQuestion.worksheetData.length : 1;
        if (selR >= rowCount) selR = 0;

        String clientName = (activeQuestion != null && activeQuestion.worksheetData != null && selR < activeQuestion.worksheetData.length)
                ? activeQuestion.worksheetData[selR][0] : "P0 (Client)";

        int[] allocVec = parseResourceVector(getTableCellString(selR, 1), 0, 1, 0);
        int[] maxVec = parseResourceVector(getTableCellString(selR, 2), 7, 5, 3);
        int[] needVec = parseResourceVector(getTableCellString(selR, 3), Math.max(0, maxVec[0] - allocVec[0]), Math.max(0, maxVec[1] - allocVec[1]), Math.max(0, maxVec[2] - allocVec[2]));
        int[] workVec = parseResourceVector(getTableCellString(selR, 4), avail[0], avail[1], avail[2]);

        boolean satA = needVec[0] <= workVec[0];
        boolean satB = needVec[1] <= workVec[1];
        boolean satC = needVec[2] <= workVec[2];
        boolean canSatisfy = satA && satB && satC;

        if (auditH > 40) {
            // Chamber container
            g2.setColor(new Color(14, 11, 26, 250));
            g2.fillRoundRect(10, auditY, w - 20, auditH, 6, 6);
            Color borderC = canSatisfy ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_YELLOW;
            g2.setColor(new Color(borderC.getRed(), borderC.getGreen(), borderC.getBlue(), 120));
            g2.drawRoundRect(10, auditY, w - 20, auditH, 6, 6);

            // Client Title
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("🔍 LIVE CLIENT AUDIT: " + clientName.toUpperCase(Locale.US), 16, auditY + 14);

            // Vector Decomposition Strip
            g2.setFont(new Font("Consolas", Font.PLAIN, 8));
            g2.setColor(new Color(190, 200, 230));
            String vecStr = String.format("Alloc: [%d %d %d]  Max: [%d %d %d]  Need: [%d %d %d]",
                    allocVec[0], allocVec[1], allocVec[2], maxVec[0], maxVec[1], maxVec[2], needVec[0], needVec[1], needVec[2]);
            g2.drawString(vecStr, 16, auditY + 26);

            // Resource Condition Checks side-by-side
            int cy = auditY + 38;
            drawConditionPill(g2, 16, cy, "A: " + needVec[0] + " <= " + workVec[0], satA);
            drawConditionPill(g2, 16 + 95, cy, "B: " + needVec[1] + " <= " + workVec[1], satB);
            drawConditionPill(g2, 16 + 190, cy, "C: " + needVec[2] + " <= " + workVec[2], satC);

            // Clearance Verdict Banner
            int vy = cy + 16;
            if (auditH > 75) {
                if (canSatisfy) {
                    g2.setColor(new Color(10, 42, 22, 240));
                    g2.fillRoundRect(14, vy, w - 28, auditH - (vy - auditY) - 4, 4, 4);
                    g2.setColor(RetroTheme.ACCENT_GREEN);
                    g2.drawRoundRect(14, vy, w - 28, auditH - (vy - auditY) - 4, 4, 4);

                    g2.setFont(new Font("Consolas", Font.BOLD, 9));
                    g2.drawString("✔ CAN SATISFY: YES  |  STATUS: RUNS [EXEC]", 20, vy + 12);
                    g2.setFont(new Font("Consolas", Font.PLAIN, 8));
                    g2.setColor(new Color(200, 255, 220));
                    String nextWork = String.format("[%d %d %d]", workVec[0] + allocVec[0], workVec[1] + allocVec[1], workVec[2] + allocVec[2]);
                    g2.drawString("Solvent! Executes safely. Upon release, Work becomes " + nextWork, 20, vy + 24);
                } else {
                    g2.setColor(new Color(42, 20, 10, 240));
                    g2.fillRoundRect(14, vy, w - 28, auditH - (vy - auditY) - 4, 4, 4);
                    g2.setColor(RetroTheme.ACCENT_YELLOW);
                    g2.drawRoundRect(14, vy, w - 28, auditH - (vy - auditY) - 4, 4, 4);

                    g2.setFont(new Font("Consolas", Font.BOLD, 9));
                    g2.drawString("⚠ CAN SATISFY: NO   |  STATUS: WAIT [BLOCKED]", 20, vy + 12);
                    g2.setFont(new Font("Consolas", Font.PLAIN, 8));
                    g2.setColor(new Color(255, 230, 180));
                    g2.drawString("Need exceeds available vault assets! Must wait to avoid deadlock.", 20, vy + 24);
                }
            }
        }
    }

    private void drawConditionPill(Graphics2D g2, int x, int y, String text, boolean satisfied) {
        Color c = satisfied ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_PINK;
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 35));
        g2.fillRoundRect(x, y, 90, 13, 3, 3);
        g2.setColor(c);
        g2.drawRoundRect(x, y, 90, 13, 3, 3);
        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        g2.drawString(text + (satisfied ? " ✔" : " ✘"), x + 4, y + 10);
    }

    private int[] parseResourceVector(String s, int d0, int d1, int d2) {
        if (s == null || s.trim().isEmpty() || s.contains("?")) return new int[]{ d0, d1, d2 };
        try {
            String[] parts = s.trim().split("\\s+");
            if (parts.length >= 3) {
                return new int[]{ Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]) };
            }
        } catch (Exception ignored) {}
        return new int[]{ d0, d1, d2 };
    }

    // Category 03: Frame Hopper MMU Address Translation Bus
    private void drawMmuPagingBus(Graphics2D g2, int w, int h, Color theme) {
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        g2.setColor(RetroTheme.ACCENT_GREEN);
        g2.drawString("🐸 MMU BUS ARCHITECTURE // ADDRESS TRANSLATOR", 10, 16);

        g2.setFont(new Font("Consolas", Font.PLAIN, 9));
        g2.setColor(new Color(180, 190, 220));
        g2.drawString("Virtual Addr -> [ Page #p | Offset d ] -> Frame #f", 10, 30);

        // Virtual Address Register Split
        int rw = w - 24;
        int rx = 12;
        int ry = 44;

        g2.setColor(new Color(20, 14, 38));
        g2.fillRoundRect(rx, ry, rw, 28, 4, 4);
        g2.setColor(RetroTheme.ACCENT_CYAN);
        g2.drawRoundRect(rx, ry, rw / 2, 28, 4, 4);
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        g2.drawRoundRect(rx + rw / 2, ry, rw / 2, 28, 4, 4);

        g2.setFont(new Font("Consolas", Font.BOLD, 9));
        g2.setColor(RetroTheme.ACCENT_CYAN);
        g2.drawString("PAGE # (p) = Addr / PS", rx + 8, ry + 18);
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        g2.drawString("OFFSET (d) = Addr % PS", rx + rw / 2 + 8, ry + 18);

        // Frame Dock mapping
        int my = 84;
        g2.setColor(new Color(10, 6, 20));
        g2.fillRoundRect(rx, my, rw, 50, 6, 6);
        g2.setColor(new Color(50, 40, 75));
        g2.drawRoundRect(rx, my, rw, 50, 6, 6);

        g2.setColor(RetroTheme.ACCENT_GREEN);
        g2.drawString("PAGE TABLE MAP: p -> Frame Dock #f", rx + 10, my + 18);
        g2.setColor(Color.WHITE);
        g2.drawString("PHYSICAL = (Frame #f * PageSize) + Offset (d)", rx + 10, my + 36);

        int ty = 146;
        g2.setColor(new Color(190, 200, 230));
        g2.setFont(new Font("Consolas", Font.PLAIN, 9));
        g2.drawString("• Non-contiguous logical pages map cleanly to physical frames.", 12, ty);
        g2.drawString("• Offset d remains identical across virtual & physical domains.", 12, ty + 16);
    }

    // Category 04: Mario Warp Pipe Cache Replacement (Full Archive Match)
    private void drawMarioCacheBuffer(Graphics2D g2, int w, int h, Color theme) {
        String policy = (activeQuestion != null && activeQuestion.policy != null) ? activeQuestion.policy : "FIFO";
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        g2.setColor(RetroTheme.ACCENT_PINK);
        g2.drawString("🍄 MARIO MMU // WARP PIPE CHAMBERS", 10, 16);

        g2.setFont(new Font("Consolas", Font.PLAIN, 9));
        g2.setColor(new Color(180, 190, 220));
        g2.drawString("POLICY: [" + policy + "]  •  3 WARP PIPE SLOTS", 10, 29);

        int[] stream = (activeQuestion != null && activeQuestion.telemetryData.get("stream") != null)
                ? (int[]) activeQuestion.telemetryData.get("stream")
                : new int[]{ 7, 0, 1, 2, 0, 3 };
        int[][] pipeHistory = (activeQuestion != null && activeQuestion.telemetryData.get("pipeHistory") != null)
                ? (int[][]) activeQuestion.telemetryData.get("pipeHistory")
                : null;
        int[] victimHistory = (activeQuestion != null && activeQuestion.telemetryData.get("victimHistory") != null)
                ? (int[]) activeQuestion.telemetryData.get("victimHistory")
                : null;
        boolean[] hitHistory = (activeQuestion != null && activeQuestion.telemetryData.get("hitHistory") != null)
                ? (boolean[]) activeQuestion.telemetryData.get("hitHistory")
                : null;

        int selR = (cipherTable != null && cipherTable.getSelectedRow() >= 0) ? cipherTable.getSelectedRow() : 0;
        if (selR >= stream.length) selR = stream.length - 1;

        // 1. Top Stats HUD (Hits, Faults, Capacity)
        int hudY = 34;
        int hudW = (w - 28) / 3;
        int hudH = 22;

        int totalHits = 0, totalFaults = 0;
        if (hitHistory != null) {
            for (boolean hit : hitHistory) {
                if (hit) totalHits++; else totalFaults++;
            }
        }
        double hitRate = stream.length > 0 ? (totalHits * 100.0) / stream.length : 0.0;

        // Box 1: Hits
        g2.setColor(new Color(10, 32, 18, 240));
        g2.fillRoundRect(10, hudY, hudW, hudH, 4, 4);
        g2.setColor(RetroTheme.ACCENT_GREEN);
        g2.drawRoundRect(10, hudY, hudW, hudH, 4, 4);
        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        g2.drawString("HITS: " + totalHits + " (" + (int)Math.round(hitRate) + "%)", 14, hudY + 14);

        // Box 2: Faults
        g2.setColor(new Color(38, 12, 22, 240));
        g2.fillRoundRect(10 + hudW + 4, hudY, hudW, hudH, 4, 4);
        g2.setColor(RetroTheme.ACCENT_PINK);
        g2.drawRoundRect(10 + hudW + 4, hudY, hudW, hudH, 4, 4);
        g2.drawString("FAULTS: " + totalFaults, 14 + hudW + 4, hudY + 14);

        // Box 3: Capacity
        g2.setColor(new Color(12, 24, 38, 240));
        g2.fillRoundRect(10 + (hudW + 4) * 2, hudY, hudW, hudH, 4, 4);
        g2.setColor(RetroTheme.ACCENT_CYAN);
        g2.drawRoundRect(10 + (hudW + 4) * 2, hudY, hudW, hudH, 4, 4);
        g2.drawString("PIPES: 3/3", 14 + (hudW + 4) * 2, hudY + 14);

        // 2. Reference Stream Conveyor Ribbon
        int ribbonY = 60;
        int ribbonH = 24;
        g2.setColor(new Color(18, 14, 32));
        g2.fillRoundRect(10, ribbonY, w - 20, ribbonH, 4, 4);
        g2.setColor(new Color(60, 48, 85));
        g2.drawRoundRect(10, ribbonY, w - 20, ribbonH, 4, 4);

        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        g2.setColor(new Color(180, 170, 210));
        g2.drawString("STREAM:", 14, ribbonY + 15);

        int tokenX = 58;
        int maxTokens = Math.min(stream.length, 8);
        for (int i = 0; i < maxTokens; i++) {
            boolean isCur = (i == selR);
            int tw = 28;
            int th = 18;
            int ty = ribbonY + 3;

            if (isCur) {
                g2.setColor(new Color(80, 60, 10));
                g2.fillRoundRect(tokenX, ty, tw, th, 3, 3);
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(tokenX, ty, tw, th, 3, 3);
                g2.setStroke(new BasicStroke(1f));
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(RetroTheme.ACCENT_YELLOW);
            } else {
                g2.setColor(new Color(28, 22, 45));
                g2.fillRoundRect(tokenX, ty, tw, th, 3, 3);
                g2.setColor(new Color(75, 60, 100));
                g2.drawRoundRect(tokenX, ty, tw, th, 3, 3);
                g2.setFont(new Font("Consolas", Font.PLAIN, 8));
                g2.setColor(new Color(200, 210, 230));
            }
            g2.drawString("P" + stream[i], tokenX + 7, ty + 12);
            tokenX += 32;
        }

        // 3. The 3 Super Mario Warp Pipes
        int pipeY = 88;
        int pipeW = (w - 44) / 3;
        int pipeH = 68;

        int curRef = (selR < stream.length) ? stream[selR] : -1;
        boolean isStepHit = (hitHistory != null && selR < hitHistory.length && hitHistory[selR]);
        int stepVictim = (victimHistory != null && selR < victimHistory.length) ? victimHistory[selR] : -1;

        for (int p = 0; p < 3; p++) {
            int px = 12 + p * (pipeW + 8);
            int resident = (pipeHistory != null && selR < pipeHistory.length) ? pipeHistory[selR][p] : -1;
            boolean isHitSlot = isStepHit && (resident == curRef);
            boolean isEvictSlot = (resident == curRef) && !isStepHit && (stepVictim != -1);

            // Pipe Top Collar / Rim
            int collarH = 16;
            g2.setColor(new Color(0, 190, 30));
            g2.fillRoundRect(px, pipeY, pipeW, collarH, 5, 5);

            // Rim highlight line
            g2.setColor(new Color(130, 255, 130));
            g2.drawLine(px + 4, pipeY + 2, px + pipeW - 5, pipeY + 2);

            // Collar shadow
            g2.setColor(new Color(0, 100, 15));
            g2.drawLine(px + 2, pipeY + collarH - 1, px + pipeW - 3, pipeY + collarH - 1);

            // Pipe Portal (Dark inner hole)
            g2.setColor(new Color(8, 20, 10));
            g2.fillOval(px + 8, pipeY + 3, pipeW - 16, collarH - 6);

            // Pipe Body
            int bodyX = px + 5;
            int bodyY = pipeY + collarH - 2;
            int bodyW = pipeW - 10;
            int bodyH = pipeH - collarH;

            GradientPaint bodyGp = new GradientPaint(bodyX, bodyY, new Color(0, 160, 25),
                    bodyX + bodyW, bodyY, new Color(0, 85, 12));
            g2.setPaint(bodyGp);
            g2.fillRect(bodyX, bodyY, bodyW, bodyH);

            // Body highlight stripe
            g2.setColor(new Color(110, 255, 110, 140));
            g2.fillRect(bodyX + 4, bodyY, 4, bodyH);

            // Pipe Outer Border
            g2.setColor(isHitSlot ? RetroTheme.ACCENT_YELLOW : (isEvictSlot ? RetroTheme.ACCENT_PINK : new Color(0, 70, 10)));
            g2.setStroke(new BasicStroke(isHitSlot || isEvictSlot ? 1.5f : 1f));
            g2.drawRoundRect(px, pipeY, pipeW, collarH, 5, 5);
            g2.drawRect(bodyX, bodyY, bodyW, bodyH);
            g2.setStroke(new BasicStroke(1f));

            // Pipe Tag (PIPE #0, PIPE #1, PIPE #2)
            g2.setFont(new Font("Consolas", Font.BOLD, 8));
            g2.setColor(new Color(220, 255, 220));
            g2.drawString("PIPE #" + p, px + 10, bodyY + 14);

            // Resident Page in bold glowing font
            if (resident != -1) {
                g2.setFont(new Font("Consolas", Font.BOLD, 14));
                g2.setColor(isHitSlot ? RetroTheme.ACCENT_YELLOW : RetroTheme.TEXT_WHITE);
                g2.drawString("[ P" + resident + " ]", px + 8, bodyY + 32);
            } else {
                g2.setFont(new Font("Consolas", Font.BOLD, 8));
                g2.setColor(new Color(120, 180, 130));
                g2.drawString("[ VACANT ]", px + 6, bodyY + 30);
            }

            // Hit / Evict Indicator Badges
            if (isHitSlot) {
                g2.setColor(new Color(255, 215, 0));
                g2.setFont(new Font("Consolas", Font.BOLD, 8));
                g2.drawString("★ COIN HIT", px + 6, bodyY + 46);
            } else if (isEvictSlot) {
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.setFont(new Font("Consolas", Font.BOLD, 8));
                g2.drawString("⚠ EVICT P" + stepVictim, px + 4, bodyY + 46);
            }
        }

        // 4. Bottom Warp Zone Eviction Verdict Banner
        int botY = pipeY + pipeH + 6;
        int botH = h - botY - 5;
        if (botH > 18) {
            g2.setColor(new Color(16, 12, 30, 240));
            g2.fillRoundRect(10, botY, w - 20, botH, 4, 4);
            g2.setColor(isStepHit ? RetroTheme.ACCENT_GREEN : new Color(80, 60, 110));
            g2.drawRoundRect(10, botY, w - 20, botH, 4, 4);

            g2.setFont(new Font("Consolas", Font.BOLD, 8));
            if (isStepHit) {
                g2.setColor(RetroTheme.ACCENT_GREEN);
                g2.drawString("★ STEP #" + (selR + 1) + " HIT: P" + curRef + " resident in pipe! Zero memory stall.", 16, botY + 13);
            } else if (stepVictim != -1) {
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.drawString("💥 STEP #" + (selR + 1) + " FAULT: Evicted P" + stepVictim + " -> Loaded P" + curRef + " (" + policy + ")", 16, botY + 13);
            } else {
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.drawString("⚡ STEP #" + (selR + 1) + " COMPULSORY MISS: Cold loaded P" + curRef + " into vacant pipe.", 16, botY + 13);
            }
        }
    }

    // Category 05: Snake Segmentation Limit Trap Radar
    private void drawSnakeSegmentTrapRadar(Graphics2D g2, int w, int h, Color theme) {
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        g2.setColor(RetroTheme.ACCENT_GREEN);
        g2.drawString("🐍 HEAP vs STACK // SEGMENT BOUNDARY RADAR", 10, 16);

        g2.setFont(new Font("Consolas", Font.PLAIN, 9));
        g2.setColor(new Color(180, 190, 220));
        g2.drawString("Hardware Limit Protection & SegFault Detection", 10, 30);

        int rw = w - 24;
        int ry = 48;

        g2.setColor(new Color(16, 12, 32));
        g2.fillRoundRect(12, ry, rw, 34, 4, 4);

        int limitW = (int) (rw * 0.65);
        g2.setColor(new Color(57, 255, 60, 60));
        g2.fillRect(12, ry, limitW, 34);
        g2.setColor(RetroTheme.ACCENT_GREEN);
        g2.drawRect(12, ry, limitW, 34);

        g2.setColor(new Color(255, 75, 95, 60));
        g2.fillRect(12 + limitW, ry, rw - limitW, 34);
        g2.setColor(RetroTheme.ACCENT_PINK);
        g2.drawRect(12 + limitW, ry, rw - limitW, 34);

        g2.setFont(new Font("Consolas", Font.BOLD, 9));
        g2.setColor(RetroTheme.ACCENT_GREEN);
        g2.drawString("BASE -> LIMIT (VALID)", 18, ry + 22);

        g2.setColor(RetroTheme.ACCENT_PINK);
        g2.drawString("TRAP ZONE", 18 + limitW + 6, ry + 22);

        int ty = 100;
        g2.setFont(new Font("Consolas", Font.BOLD, 9));
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        g2.drawString("• If Offset < Limit -> VALID: Physical = Base + Offset", 12, ty);
        g2.setColor(RetroTheme.ACCENT_PINK);
        g2.drawString("• If Offset >= Limit -> HARDWARE TRAP (SegFault Exception)", 12, ty + 18);

        g2.setColor(new Color(200, 210, 240));
        g2.setFont(new Font("Consolas", Font.PLAIN, 9));
        g2.drawString("Variable length segments protect separate Stack and Heap regions.", 12, ty + 36);
    }

    // Category 06: Cyber Crane Dynamic RAM Partition Map (Authentic Motherboard v2.6 Match)
    private void drawCyberCraneRamPartitions(Graphics2D g2, int w, int h, Color theme) {
        String policy = (activeQuestion != null && activeQuestion.policy != null) ? activeQuestion.policy : "FIRST FIT";

        // 1. Title & Policy Subtitle
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        g2.setColor(RetroTheme.ACCENT_PURPLE);
        g2.drawString("🏗️ CYBER RAM MOTHERBOARD v2.6", 10, 15);

        g2.setFont(new Font("Consolas", Font.PLAIN, 8));
        g2.setColor(new Color(180, 190, 220));
        g2.drawString("POLICY: [" + policy + "]  •  DUAL-CHANNEL CONTIGUOUS MATRIX", 10, 27);

        // Telemetry Data Extraction
        int[] sockSizes = (activeQuestion != null && activeQuestion.telemetryData.get("sockSizes") != null)
                ? (int[]) activeQuestion.telemetryData.get("sockSizes")
                : new int[]{ 100, 500, 200, 300, 600 };
        int[] sockBases = (activeQuestion != null && activeQuestion.telemetryData.get("sockBases") != null)
                ? (int[]) activeQuestion.telemetryData.get("sockBases")
                : new int[]{ 0, 100, 600, 800, 1100 };
        String[] sockNames = (activeQuestion != null && activeQuestion.telemetryData.get("sockNames") != null)
                ? (String[]) activeQuestion.telemetryData.get("sockNames")
                : new String[]{ "Socket 1", "Socket 2", "Socket 3", "Socket 4", "Socket 5" };
        int[] demands = (activeQuestion != null && activeQuestion.telemetryData.get("demands") != null)
                ? (int[]) activeQuestion.telemetryData.get("demands")
                : new int[]{ 212, 417, 112, 426 };
        int[] assignedSocks = (activeQuestion != null && activeQuestion.telemetryData.get("assignedSocks") != null)
                ? (int[]) activeQuestion.telemetryData.get("assignedSocks")
                : new int[]{ 3, 1, 2, 4 };
        int[] frags = (activeQuestion != null && activeQuestion.telemetryData.get("frags") != null)
                ? (int[]) activeQuestion.telemetryData.get("frags")
                : new int[]{ 88, 83, 88, 174 };

        int numSockets = sockSizes.length;
        int totalRam = 0;
        for (int s : sockSizes) totalRam += s;
        if (totalRam <= 0) totalRam = 1700;

        int totalPayload = 0;
        int totalFrag = 0;
        for (int i = 0; i < demands.length; i++) {
            if (i < assignedSocks.length && assignedSocks[i] != -1) {
                totalPayload += demands[i];
                if (i < frags.length) totalFrag += frags[i];
            }
        }
        int totalWaste = Math.max(0, totalRam - totalPayload);
        int utilPct = totalRam > 0 ? (totalPayload * 100) / totalRam : 0;

        int selR = (cipherTable != null && cipherTable.getSelectedRow() >= 0) ? cipherTable.getSelectedRow() : 0;
        if (selR >= demands.length) selR = demands.length - 1;

        // 2. Top Utilization HUD Bar
        int hudY = 32;
        int hudH = 14;
        g2.setColor(new Color(12, 8, 24));
        g2.fillRoundRect(10, hudY, w - 20, hudH, 3, 3);
        g2.setColor(new Color(50, 40, 75));
        g2.drawRoundRect(10, hudY, w - 20, hudH, 3, 3);

        int barW = 75;
        g2.setColor(new Color(25, 20, 45));
        g2.fillRect(12, hudY + 2, barW, hudH - 4);
        int fillW = Math.min(barW, (utilPct * barW) / 100);
        g2.setColor(RetroTheme.ACCENT_GREEN);
        g2.fillRect(12, hudY + 2, fillW, hudH - 4);

        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        g2.setColor(Color.WHITE);
        g2.drawString("UTIL: " + utilPct + "%", 92, hudY + 10);
        g2.setColor(RetroTheme.ACCENT_PINK);
        g2.drawString("WASTE: " + totalWaste + " KB", 148, hudY + 10);
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        g2.drawString("TOTAL: " + totalRam + " KB", w - 85, hudY + 10);

        // 3. Motherboard Physical Sockets
        int sockAreaY = 50;
        int sockAreaH = 46;
        int availW = w - 20;
        int gap = 3;
        int totalGaps = (numSockets - 1) * gap;
        int usableW = availW - totalGaps;

        // Chip colors
        Color[] CHIP_COLORS = {
                new Color(0, 210, 210),   // P1 Cyan
                new Color(40, 215, 96),   // P2 Green
                new Color(255, 180, 20),  // P3 Orange
                new Color(255, 55, 130),  // P4 Pink
                new Color(170, 70, 255),  // P5 Purple
                new Color(50, 150, 255),  // P6 Blue
                new Color(255, 110, 60)   // P7 Coral
        };

        // Determine which chip is in which socket
        int[] chipInSock = new int[numSockets];
        Arrays.fill(chipInSock, -1);
        for (int i = 0; i < demands.length; i++) {
            if (i < assignedSocks.length && assignedSocks[i] >= 0 && assignedSocks[i] < numSockets) {
                chipInSock[assignedSocks[i]] = i;
            }
        }

        int currX = 10;
        for (int s = 0; s < numSockets; s++) {
            int sw = Math.max(30, (sockSizes[s] * usableW) / totalRam);
            if (s == numSockets - 1) {
                sw = (10 + availW) - currX; // Close rounding delta
            }

            // Top Base Address Label & Pin
            g2.setFont(new Font("Consolas", Font.BOLD, 7));
            g2.setColor(new Color(170, 185, 220));
            g2.drawString(sockBases[s] + "K", currX + 1, sockAreaY - 2);

            // Socket Frame
            g2.setColor(new Color(14, 10, 24));
            g2.fillRect(currX, sockAreaY, sw, sockAreaH);

            // Solder Pins (Top & Bottom)
            g2.setColor(new Color(190, 200, 225));
            for (int px = currX + 4; px < currX + sw - 3; px += 8) {
                g2.fillRect(px, sockAreaY - 2, 3, 2);
                g2.fillRect(px, sockAreaY + sockAreaH, 3, 2);
            }

            int chipIdx = chipInSock[s];
            if (chipIdx != -1) {
                int dem = demands[chipIdx];
                int cap = sockSizes[s];
                int frag = (chipIdx < frags.length) ? frags[chipIdx] : (cap - dem);
                int cw = Math.max(14, Math.min(sw - 2, (dem * sw) / cap));

                // Chip Payload Fill
                Color chipCol = CHIP_COLORS[chipIdx % CHIP_COLORS.length];
                g2.setColor(chipCol);
                g2.fillRect(currX + 1, sockAreaY + 1, cw - 1, sockAreaH - 2);

                // Chip Label
                g2.setColor(new Color(10, 8, 20));
                g2.setFont(new Font("Consolas", Font.BOLD, 8));
                g2.drawString("P" + (chipIdx + 1), currX + 3, sockAreaY + 14);
                if (cw > 28) {
                    g2.setFont(new Font("Consolas", Font.PLAIN, 7));
                    g2.drawString(dem + "K", currX + 3, sockAreaY + 25);
                }

                // Internal Heat Frag (Red Hatching)
                int fragW = sw - cw;
                if (fragW > 1) {
                    int fx = currX + cw;
                    g2.setColor(new Color(65, 12, 25, 210));
                    g2.fillRect(fx, sockAreaY + 1, fragW - 1, sockAreaH - 2);

                    Shape oldClip = g2.getClip();
                    g2.clipRect(fx, sockAreaY + 1, fragW - 1, sockAreaH - 2);
                    g2.setColor(new Color(255, 60, 90, 160));
                    for (int hx = fx - sockAreaH; hx < fx + fragW + sockAreaH; hx += 5) {
                        g2.drawLine(hx, sockAreaY + sockAreaH, hx + sockAreaH, sockAreaY);
                    }
                    g2.setClip(oldClip);

                    if (fragW > 24) {
                        g2.setColor(new Color(255, 200, 210));
                        g2.setFont(new Font("Consolas", Font.BOLD, 7));
                        g2.drawString("HEAT", fx + 3, sockAreaY + 14);
                        g2.drawString(frag + "K", fx + 3, sockAreaY + 25);
                    }
                }
            } else {
                // Vacant Socket
                g2.setColor(new Color(22, 18, 36));
                g2.fillRect(currX + 1, sockAreaY + 1, sw - 2, sockAreaH - 2);
                g2.setColor(new Color(110, 105, 135));
                g2.setFont(new Font("Consolas", Font.BOLD, 7));
                g2.drawString("VACANT", currX + 2, sockAreaY + 14);
                g2.drawString(sockSizes[s] + "K", currX + 2, sockAreaY + 25);
            }

            // Socket Outer Border
            boolean isSelSocket = (selR < assignedSocks.length && assignedSocks[selR] == s);
            if (isSelSocket) {
                g2.setColor(new Color(255, 215, 0));
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRect(currX, sockAreaY, sw, sockAreaH);
                g2.setStroke(new BasicStroke(1f));
            } else {
                g2.setColor(new Color(55, 45, 80));
                g2.drawRect(currX, sockAreaY, sw, sockAreaH);
            }

            // Bottom Socket Name & Size
            g2.setFont(new Font("Consolas", Font.BOLD, 7));
            g2.setColor(isSelSocket ? RetroTheme.ACCENT_YELLOW : new Color(175, 170, 205));
            g2.drawString("S" + (s + 1) + "[" + sockSizes[s] + "K]", currX + 1, sockAreaY + sockAreaH + 9);

            currX += sw + gap;
        }

        // 4. Chip Cards Row (Mini Cards)
        int cardRowY = sockAreaY + sockAreaH + 13;
        int cardH = 26;
        int cardGap = 3;
        int cardW = (availW - (demands.length - 1) * cardGap) / demands.length;

        for (int i = 0; i < demands.length; i++) {
            int cx = 10 + i * (cardW + cardGap);
            boolean isCur = (i == selR);
            int sIdx = (i < assignedSocks.length) ? assignedSocks[i] : -1;

            g2.setColor(isCur ? new Color(35, 25, 60) : new Color(18, 14, 30));
            g2.fillRoundRect(cx, cardRowY, cardW, cardH, 3, 3);
            g2.setColor(isCur ? RetroTheme.ACCENT_YELLOW : new Color(50, 40, 70));
            g2.drawRoundRect(cx, cardRowY, cardW, cardH, 3, 3);

            Color chipCol = CHIP_COLORS[i % CHIP_COLORS.length];
            g2.setColor(chipCol);
            g2.fillRect(cx + 2, cardRowY + 3, 4, cardH - 6);

            g2.setFont(new Font("Consolas", Font.BOLD, 7));
            g2.setColor(Color.WHITE);
            g2.drawString("CHIP P" + (i + 1), cx + 8, cardRowY + 10);

            g2.setFont(new Font("Consolas", Font.PLAIN, 7));
            if (sIdx != -1) {
                g2.setColor(RetroTheme.ACCENT_GREEN);
                g2.drawString("IN SOCK " + (sIdx + 1), cx + 8, cardRowY + 18);
            } else {
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.drawString("DENIED", cx + 8, cardRowY + 18);
            }
        }

        // 5. Selected Process Telemetry & Allocation Rationale Banner
        int ratY = cardRowY + cardH + 4;
        int ratH = h - ratY - 4;
        if (ratH > 16) {
            g2.setColor(new Color(14, 10, 24, 245));
            g2.fillRoundRect(10, ratY, w - 20, ratH, 4, 4);
            g2.setColor(new Color(55, 42, 80));
            g2.drawRoundRect(10, ratY, w - 20, ratH, 4, 4);

            int targetSock = (selR < assignedSocks.length) ? assignedSocks[selR] : -1;
            int targetDem = (selR < demands.length) ? demands[selR] : 0;
            int targetFrag = (selR < frags.length) ? frags[selR] : 0;

            g2.setFont(new Font("Consolas", Font.BOLD, 8));
            if (targetSock != -1) {
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.drawString("CHIP P" + (selR + 1) + " (" + targetDem + " KB) -> " + sockNames[targetSock] + " [" + sockSizes[targetSock] + "K]", 14, ratY + 11);
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.drawString("HEAT: " + targetFrag + " KB", w - 75, ratY + 11);
            } else {
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.drawString("CHIP P" + (selR + 1) + " (" + targetDem + " KB) -> DENIED: NO FIT (Exceeds sockets)", 14, ratY + 11);
            }

            g2.setFont(new Font("Consolas", Font.PLAIN, 7));
            g2.setColor(new Color(190, 200, 225));
            String ratText;
            if (targetSock != -1) {
                if (policy.equals("BEST FIT")) {
                    ratText = "• BEST FIT: Picked smallest available socket (" + sockSizes[targetSock] + "K) to minimize internal heat frag.";
                } else if (policy.equals("NEXT FIT")) {
                    ratText = "• NEXT FIT: Continued scanning circular search from prior allocation position.";
                } else {
                    ratText = "• FIRST FIT: Assigned to first free socket (" + sockSizes[targetSock] + "K) with capacity >= " + targetDem + " KB.";
                }
            } else {
                ratText = "• " + policy + ": Process demand (" + targetDem + " KB) exceeds all free partition capacities.";
            }
            g2.drawString(ratText, 14, ratY + 22);

            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("TOTAL RAM: " + totalRam + "K | FRAG: " + totalFrag + "K | PAYLOAD: " + totalPayload + "K | UTIL: " + utilPct + "%", 14, ratY + 31);
        }
    }

    // Category 07: Pac-Man Disk Cylinder Seek Track Radar & Linear Rail (Authentic PMU v2.7)
    private void drawPacmanDiskPlatter(Graphics2D g2, int w, int h, Color theme) {
        String policy = (activeQuestion != null && activeQuestion.policy != null) ? activeQuestion.policy : "FCFS";

        // 1. Title & Policy Subtitle
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        g2.drawString("🟡 PAC-MAN DISK HEAD // CYLINDER PLATTER", 10, 15);

        g2.setFont(new Font("Consolas", Font.PLAIN, 8));
        g2.setColor(new Color(180, 190, 220));
        g2.drawString("POLICY: [" + policy + "]  •  0..199 CYLINDER TRACKS  •  DIR: UP (->)", 10, 27);

        // Telemetry Data Extraction
        int startHead = (activeQuestion != null && activeQuestion.telemetryData.get("startHead") != null)
                ? (int) activeQuestion.telemetryData.get("startHead") : 53;
        int maxCyl = (activeQuestion != null && activeQuestion.telemetryData.get("maxCyl") != null)
                ? (int) activeQuestion.telemetryData.get("maxCyl") : 199;
        int[] requests = (activeQuestion != null && activeQuestion.telemetryData.get("requests") != null)
                ? (int[]) activeQuestion.telemetryData.get("requests") : new int[]{ 98, 183, 37, 122, 14, 124, 65, 67 };
        int[] seekSeq = (activeQuestion != null && activeQuestion.telemetryData.get("seekSequence") != null)
                ? (int[]) activeQuestion.telemetryData.get("seekSequence") : new int[]{ 53, 98, 183, 37, 122 };
        int[] traversedSteps = (activeQuestion != null && activeQuestion.telemetryData.get("traversedSteps") != null)
                ? (int[]) activeQuestion.telemetryData.get("traversedSteps") : null;
        String[] directions = (activeQuestion != null && activeQuestion.telemetryData.get("directions") != null)
                ? (String[]) activeQuestion.telemetryData.get("directions") : null;
        int[] cumulativeSteps = (activeQuestion != null && activeQuestion.telemetryData.get("cumulativeSteps") != null)
                ? (int[]) activeQuestion.telemetryData.get("cumulativeSteps") : null;
        int totalSeek = (activeQuestion != null && activeQuestion.telemetryData.get("totalSeek") != null)
                ? (int) activeQuestion.telemetryData.get("totalSeek") : 0;
        double avgSeek = (activeQuestion != null && activeQuestion.telemetryData.get("avgSeek") != null)
                ? (double) activeQuestion.telemetryData.get("avgSeek") : 0.0;

        int numSteps = Math.max(1, seekSeq.length - 1);
        int selR = (cipherTable != null && cipherTable.getSelectedRow() >= 0) ? cipherTable.getSelectedRow() : 0;
        if (selR >= numSteps) selR = numSteps - 1;

        int fromCyl = (selR < seekSeq.length) ? seekSeq[selR] : startHead;
        int toCyl = (selR + 1 < seekSeq.length) ? seekSeq[selR + 1] : startHead;
        int seekDist = (traversedSteps != null && selR < traversedSteps.length) ? traversedSteps[selR] : Math.abs(toCyl - fromCyl);
        String dir = (directions != null && selR < directions.length) ? directions[selR] : (toCyl >= fromCyl ? "UP (->)" : "DOWN (<-)");
        int runningCumul = (cumulativeSteps != null && selR < cumulativeSteps.length) ? cumulativeSteps[selR] : seekDist;

        // 2. Top Stats HUD Pods
        int hudY = 32;
        int hudH = 15;
        int podW = (w - 28) / 3;

        // Pod 1: Total Seek
        g2.setColor(new Color(10, 30, 20, 240));
        g2.fillRoundRect(10, hudY, podW, hudH, 3, 3);
        g2.setColor(RetroTheme.ACCENT_GREEN);
        g2.drawRoundRect(10, hudY, podW, hudH, 3, 3);
        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        g2.drawString("TOTAL: " + totalSeek + " cyl", 14, hudY + 11);

        // Pod 2: Avg Seek
        g2.setColor(new Color(12, 25, 40, 240));
        g2.fillRoundRect(10 + podW + 4, hudY, podW, hudH, 3, 3);
        g2.setColor(RetroTheme.ACCENT_CYAN);
        g2.drawRoundRect(10 + podW + 4, hudY, podW, hudH, 3, 3);
        g2.drawString(String.format(Locale.US, "AVG: %.1f cyl", avgSeek), 14 + podW + 4, hudY + 11);

        // Pod 3: Active Head
        g2.setColor(new Color(36, 26, 10, 240));
        g2.fillRoundRect(10 + (podW + 4) * 2, hudY, podW, hudH, 3, 3);
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        g2.drawRoundRect(10 + (podW + 4) * 2, hudY, podW, hudH, 3, 3);
        g2.drawString("HEAD: Cyl " + fromCyl, 14 + (podW + 4) * 2, hudY + 11);

        // 3. Linear Cylinder Platter Chamber (Neon Blue Maze Rail)
        int railAreaY = 51;
        int railAreaH = 58;
        g2.setColor(new Color(10, 8, 22));
        g2.fillRoundRect(10, railAreaY, w - 20, railAreaH, 4, 4);
        g2.setColor(new Color(45, 35, 75));
        g2.drawRoundRect(10, railAreaY, w - 20, railAreaH, 4, 4);

        int startX = 32;
        int endX = w - 32;
        int trackW = endX - startX;
        int railY = railAreaY + 24;

        // Neon Blue Double Rail Tracks
        g2.setColor(new Color(33, 33, 222));
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawLine(startX, railY - 6, endX, railY - 6);
        g2.drawLine(startX, railY + 14, endX, railY + 14);

        // Left Boundary Warp Tunnel (Cyl 0)
        g2.setColor(new Color(0, 180, 255));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(startX - 18, railY - 10, 16, 28, 4, 4);
        g2.setFont(new Font("Consolas", Font.BOLD, 7));
        g2.drawString("0", startX - 12, railY + 7);

        // Right Boundary Warp Tunnel (Cyl 199)
        g2.drawRoundRect(endX + 2, railY - 10, 16, 28, 4, 4);
        g2.drawString("199", endX + 4, railY + 7);

        // Faint lane tick marks
        g2.setColor(new Color(60, 50, 95));
        g2.setStroke(new BasicStroke(1f));
        for (int c = 0; c <= 199; c += 25) {
            int cx = startX + (int) ((c * trackW) / 199.0);
            g2.drawLine(cx, railY - 9, cx, railY - 3);
            g2.drawLine(cx, railY + 11, cx, railY + 17);
        }

        // Draw Past Traversal Path (Cyan dashed lines)
        g2.setColor(new Color(0, 200, 255, 120));
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{3f, 3f}, 0f));
        for (int i = 0; i < selR && i + 1 < seekSeq.length; i++) {
            int p1 = startX + (int) ((seekSeq[i] * trackW) / 199.0);
            int p2 = startX + (int) ((seekSeq[i + 1] * trackW) / 199.0);
            g2.drawLine(p1, railY + 4, p2, railY + 4);
        }

        // Draw Active Seek Hop (Bright Golden Arrow)
        int curX = startX + (int) ((fromCyl * trackW) / 199.0);
        int nextX = startX + (int) ((toCyl * trackW) / 199.0);
        g2.setColor(new Color(255, 215, 0));
        g2.setStroke(new BasicStroke(2.2f));
        g2.drawLine(curX, railY + 4, nextX, railY + 4);
        // Arrow head on active hop
        int arrowDir = (nextX >= curX) ? 1 : -1;
        g2.drawLine(nextX, railY + 4, nextX - arrowDir * 6, railY);
        g2.drawLine(nextX, railY + 4, nextX - arrowDir * 6, railY + 8);
        g2.setStroke(new BasicStroke(1f));

        // Draw Ghost Targets on Request Cylinders
        Color[] GHOST_COLORS = {
                new Color(255, 0, 0),     // Blinky Red
                new Color(255, 184, 255), // Pinky Pink
                new Color(0, 255, 255),   // Inky Cyan
                new Color(255, 184, 82)   // Clyde Orange
        };
        for (int i = 0; i < requests.length; i++) {
            int rc = requests[i];
            int gx = startX + (int) ((rc * trackW) / 199.0);
            Color gCol = GHOST_COLORS[i % GHOST_COLORS.length];
            g2.setColor(gCol);
            // Cute 8-bit ghost head: dome with feet
            g2.fillArc(gx - 3, railY + 1, 7, 7, 0, 180);
            g2.fillRect(gx - 3, railY + 4, 7, 4);
            // Ghost eyes
            g2.setColor(Color.WHITE);
            g2.fillRect(gx - 2, railY + 3, 2, 2);
            g2.fillRect(gx + 1, railY + 3, 2, 2);
            // Label cylinder below
            g2.setFont(new Font("Consolas", Font.BOLD, 7));
            g2.setColor(new Color(190, 180, 220));
            g2.drawString(String.valueOf(rc), gx - 5, railY + 23);
        }

        // Draw Animated Chomping Pac-Man at current head position!
        int pacSize = 15;
        int chomp = (int) (Math.abs(Math.sin(pulseTicks * 0.28)) * 38);
        boolean facingRight = (toCyl >= fromCyl);
        g2.setColor(new Color(255, 230, 0));
        int startAngle = facingRight ? chomp : (180 + chomp);
        int arcAngle = 360 - chomp * 2;
        g2.fillArc(curX - pacSize / 2, railY + 4 - pacSize / 2, pacSize, pacSize, startAngle, arcAngle);
        // Pac-Man eye
        g2.setColor(new Color(10, 8, 20));
        int eyeX = facingRight ? curX : (curX - 2);
        g2.fillOval(eyeX, railY + 4 - 5, 2, 2);

        // 4. Active Step Telemetry Card
        int cardY = railAreaY + railAreaH + 4;
        int cardH = 38;
        g2.setColor(new Color(16, 12, 30, 245));
        g2.fillRoundRect(10, cardY, w - 20, cardH, 4, 4);
        g2.setColor(new Color(55, 42, 80));
        g2.drawRoundRect(10, cardY, w - 20, cardH, 4, 4);

        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        g2.drawString(String.format("STEP #%d: Cyl %d -> Cyl %d | DIST: %d cyl (%s)", selR + 1, fromCyl, toCyl, seekDist, dir), 14, cardY + 12);
        g2.setColor(RetroTheme.ACCENT_CYAN);
        g2.drawString("CUMUL: " + runningCumul + " cyl", w - 85, cardY + 12);

        g2.setFont(new Font("Consolas", Font.PLAIN, 7));
        g2.setColor(new Color(195, 205, 230));
        String rationale;
        if (policy.equals("SSTF")) {
            rationale = "• SSTF: Greedily selected Cyl " + toCyl + " (shortest distance = " + seekDist + " cyl from current head).";
        } else if (policy.equals("SCAN")) {
            rationale = "• SCAN: Elevator sweeping in path towards boundary 199, servicing all forward requests.";
        } else if (policy.equals("C-SCAN")) {
            rationale = "• C-SCAN: Circular sweep towards boundary 199, warps to 0 without servicing return requests.";
        } else if (policy.equals("LOOK")) {
            rationale = "• LOOK: Sweeping to highest pending cylinder (" + toCyl + ") without traveling to boundary 199.";
        } else if (policy.equals("C-LOOK")) {
            rationale = "• C-LOOK: Sweeping to highest pending request, warps directly to lowest pending request.";
        } else {
            rationale = "• FCFS: Services Cyl " + toCyl + " strictly in incoming queue arrival order.";
        }
        g2.drawString(rationale, 14, cardY + 23);
        g2.drawString("FORMULA: Traversed = |" + toCyl + " - " + fromCyl + "| = " + seekDist + " cyl | Cumulative = " + runningCumul + " cyl", 14, cardY + 32);

        // 5. Bottom Telemetry Banner
        int botY = cardY + cardH + 4;
        int botH = h - botY - 4;
        if (botH > 14) {
            g2.setColor(new Color(12, 8, 20, 240));
            g2.fillRoundRect(10, botY, w - 20, botH, 3, 3);
            g2.setColor(new Color(45, 35, 65));
            g2.drawRoundRect(10, botY, w - 20, botH, 3, 3);

            g2.setFont(new Font("Consolas", Font.BOLD, 7));
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("START: " + startHead + " | TOTAL TRAVERSED: " + totalSeek + " cyl | AVG SEEK: " + String.format(Locale.US, "%.2f cyl", avgSeek), 14, botY + 11);
        }
    }

    // Category 08: Overcooked Multiprogramming CPU Kitchen Concurrency
    private void drawOvercookedConcurrencyCurve(Graphics2D g2, int w, int h, Color theme) {
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        g2.setColor(RetroTheme.ACCENT_PINK);
        g2.drawString("🍳 MULTIPROGRAMMING TRACE // EVENT-DRIVEN EXECUTION", 10, 15);

        g2.setFont(new Font("Consolas", Font.PLAIN, 8));
        g2.setColor(new Color(180, 190, 220));
        g2.drawString("EXCEL TIME-STEP MODEL  •  EQUAL 1/N_J CPU SLICING", 10, 27);

        int n = 5;
        double avgTat = 0.72, avgWt = 0.40, totalElapsed = 1.70;
        String[] dishNames = null;
        int[] dishTypes = null;
        double[] at = null, bt = null, ft = null, tat = null, wt = null;

        if (activeQuestion != null && activeQuestion.telemetryData != null) {
            Object oN = activeQuestion.telemetryData.get("jobCount");
            if (oN instanceof Integer) n = (Integer) oN;
            Object oAt = activeQuestion.telemetryData.get("at");
            if (oAt instanceof double[]) at = (double[]) oAt;
            Object oBt = activeQuestion.telemetryData.get("bt");
            if (oBt instanceof double[]) bt = (double[]) oBt;
            Object oFt = activeQuestion.telemetryData.get("ft");
            if (oFt instanceof double[]) ft = (double[]) oFt;
            Object oTat = activeQuestion.telemetryData.get("tat");
            if (oTat instanceof double[]) tat = (double[]) oTat;
            Object oWt = activeQuestion.telemetryData.get("wt");
            if (oWt instanceof double[]) wt = (double[]) oWt;
            Object oAvgTat = activeQuestion.telemetryData.get("avgTat");
            if (oAvgTat instanceof Double) avgTat = (Double) oAvgTat;
            Object oAvgWt = activeQuestion.telemetryData.get("avgWt");
            if (oAvgWt instanceof Double) avgWt = (Double) oAvgWt;
            Object oDn = activeQuestion.telemetryData.get("dishNames");
            if (oDn instanceof String[]) dishNames = (String[]) oDn;
            Object oDt = activeQuestion.telemetryData.get("dishTypes");
            if (oDt instanceof int[]) dishTypes = (int[]) oDt;
            Object oTe = activeQuestion.telemetryData.get("totalElapsed");
            if (oTe instanceof Double) totalElapsed = (Double) oTe;
        }

        // 1. Top HUD Pods (3 Capsules)
        int podY = 32;
        int podH = 20;
        int podW = (w - 28) / 3;

        // Pod 1: AVG TAT
        g2.setColor(new Color(14, 28, 22));
        g2.fillRoundRect(10, podY, podW, podH, 5, 5);
        g2.setColor(new Color(0, 200, 110));
        g2.drawRoundRect(10, podY, podW, podH, 5, 5);
        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        g2.drawString(String.format(Locale.US, "AVG TAT: %.2f", avgTat), 14, podY + 13);

        // Pod 2: AVG WT
        g2.setColor(new Color(28, 24, 14));
        g2.fillRoundRect(10 + podW + 4, podY, podW, podH, 5, 5);
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        g2.drawRoundRect(10 + podW + 4, podY, podW, podH, 5, 5);
        g2.drawString(String.format(Locale.US, "AVG WT: %.2f", avgWt), 14 + podW + 4, podY + 13);

        // Pod 3: Total Elapsed Time
        g2.setColor(new Color(14, 20, 36));
        g2.fillRoundRect(10 + (podW + 4) * 2, podY, podW, podH, 5, 5);
        g2.setColor(RetroTheme.ACCENT_CYAN);
        g2.drawRoundRect(10 + (podW + 4) * 2, podY, podW, podH, 5, 5);
        g2.drawString(String.format(Locale.US, "TOTAL T: %.2f", totalElapsed), 14 + (podW + 4) * 2, podY + 13);

        // 2. Kitchen Order Rail
        int railY = 56;
        int railH = 58;
        int railW = w - 20;

        g2.setColor(new Color(18, 12, 32));
        g2.fillRoundRect(10, railY, railW, railH, 6, 6);
        g2.setColor(new Color(65, 45, 95));
        g2.drawRoundRect(10, railY, railW, railH, 6, 6);

        // Chef Base on Left
        int baseW = 44;
        g2.setColor(new Color(28, 18, 48));
        g2.fillRoundRect(12, railY + 2, baseW, railH - 4, 4, 4);
        g2.setColor(new Color(85, 60, 120));
        g2.drawRoundRect(12, railY + 2, baseW, railH - 4, 4, 4);

        // Bell on chef base
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        g2.fillArc(22, railY + 10, 16, 12, 0, 180);
        g2.fillRect(20, railY + 20, 20, 3);
        g2.fillRect(28, railY + 7, 4, 3);
        g2.setFont(new Font("Consolas", Font.BOLD, 7));
        g2.setColor(Color.WHITE);
        g2.drawString("CHEF", 20, railY + 34);
        g2.setColor(RetroTheme.ACCENT_CYAN);
        g2.drawString("BASE", 20, railY + 44);

        // Order Ticket Cards
        int selectedRow = (cipherTable != null) ? cipherTable.getSelectedRow() : -1;
        int cardAreaX = 10 + baseW + 6;
        int cardAreaW = railW - baseW - 10;
        int cardSlotW = Math.max(38, cardAreaW / Math.max(1, n));

        for (int i = 0; i < n; i++) {
            int cx = cardAreaX + i * cardSlotW;
            int cw = cardSlotW - 4;
            if (cx + cw > 10 + railW - 4) break;

            boolean isSel = false;
            if (selectedRow >= 0 && cipherTable != null && selectedRow < cipherTable.getRowCount()) {
                String jobInRow = getTableCellString(selectedRow, 6);
                if (jobInRow != null && jobInRow.equalsIgnoreCase("J" + (i + 1))) {
                    isSel = true;
                }
            }

            g2.setColor(isSel ? new Color(45, 30, 75) : new Color(25, 18, 40));
            g2.fillRoundRect(cx, railY + 4, cw, railH - 8, 4, 4);

            if (isSel) {
                g2.setColor(new Color(255, 215, 0));
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(new Color(70, 50, 100));
                g2.setStroke(new BasicStroke(1.0f));
            }
            g2.drawRoundRect(cx, railY + 4, cw, railH - 8, 4, 4);

            // Clip
            g2.setColor(new Color(190, 195, 215));
            g2.fillRect(cx + cw / 2 - 5, railY + 2, 10, 3);

            // Dish Icon
            int dType = (dishTypes != null && i < dishTypes.length) ? dishTypes[i] : (i % 6);
            BufferedImage dImg = PixelSprites.createDishSprite(dType, 16);
            if (dImg != null) {
                g2.drawImage(dImg, cx + cw / 2 - 8, railY + 7, null);
            }

            // Job label & dish name
            String dName = (dishNames != null && i < dishNames.length) ? dishNames[i] : ("Dish " + (i + 1));
            g2.setFont(new Font("Consolas", Font.BOLD, 7));
            g2.setColor(isSel ? RetroTheme.ACCENT_YELLOW : Color.WHITE);
            g2.drawString("J" + (i + 1), cx + 3, railY + 31);

            g2.setFont(new Font("Consolas", Font.PLAIN, 7));
            g2.setColor(new Color(200, 210, 230));
            String shortName = dName.length() > 5 ? dName.substring(0, 5) : dName;
            g2.drawString(shortName, cx + 3, railY + 40);

            // Finish Time
            if (ft != null && i < ft.length) {
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.drawString(String.format(Locale.US, "FT=%.2f", ft[i]), cx + 3, railY + 50);
            }
        }
        g2.setStroke(new BasicStroke(1.0f));

        // 3. Stove Counter Bar & Active Chef
        int stoveY = 118;
        int stoveH = 22;
        g2.setColor(new Color(30, 22, 50));
        g2.fillRoundRect(10, stoveY, railW, stoveH, 4, 4);
        g2.setColor(new Color(60, 42, 90));
        g2.drawRoundRect(10, stoveY, railW, stoveH, 4, 4);

        // Circular Burners
        for (int b = 0; b < 5; b++) {
            int bx = 20 + b * ((railW - 40) / 4);
            g2.setColor(new Color(15, 10, 25));
            g2.fillOval(bx - 6, stoveY + 4, 14, 14);
            g2.setColor(new Color(75, 55, 110));
            g2.drawOval(bx - 6, stoveY + 4, 14, 14);
            // Flame glow
            g2.setColor(new Color(0, 220, 255, 140));
            g2.fillOval(bx - 2, stoveY + 8, 6, 6);
        }

        // Animated / Dynamic Chef Sprite
        BufferedImage chefImg = PixelSprites.createChefSprite(18, true);
        if (chefImg != null) {
            g2.drawImage(chefImg, w - 38, stoveY + 2, null);
        }

        // 4. Selected Row Telemetry Inspector Card
        int inspectY = 144;
        int inspectH = 26;
        g2.setColor(new Color(12, 8, 24));
        g2.fillRoundRect(10, inspectY, railW, inspectH, 4, 4);
        g2.setColor(new Color(75, 55, 110));
        g2.drawRoundRect(10, inspectY, railW, inspectH, 4, 4);

        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        if (selectedRow >= 0 && cipherTable != null && selectedRow < cipherTable.getRowCount()) {
            String timeStr = getTableCellString(selectedRow, 0);
            String eventStr = getTableCellString(selectedRow, 1);
            String njStr = getTableCellString(selectedRow, 2);
            String cpuStr = getTableCellString(selectedRow, 3);
            String jobStr = getTableCellString(selectedRow, 6);
            String leftStr = getTableCellString(selectedRow, 7);

            // Backfill time if empty
            if (timeStr == null || timeStr.isEmpty() || timeStr.equals("-")) {
                for (int sc = selectedRow - 1; sc >= 0; sc--) {
                    String ts = getTableCellString(sc, 0);
                    if (ts != null && !ts.isEmpty() && !ts.equals("-")) {
                        timeStr = ts;
                        eventStr = getTableCellString(sc, 1);
                        break;
                    }
                }
            }

            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString(String.format("STEP #%d: T = %s  [%s]  |  nj = %s  |  %%cpu = %s",
                    selectedRow + 1,
                    (timeStr != null && !timeStr.isEmpty()) ? timeStr : "?",
                    (eventStr != null && !eventStr.isEmpty()) ? eventStr : "?",
                    (njStr != null && !njStr.isEmpty()) ? njStr : "?",
                    (cpuStr != null && !cpuStr.isEmpty()) ? cpuStr : "?"), 16, inspectY + 11);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Consolas", Font.PLAIN, 8));
            g2.drawString(String.format("ACTIVE DISH: %s  |  REMAINING BURST TIME LEFT: %s",
                    (jobStr != null && !jobStr.isEmpty()) ? jobStr : "—",
                    (leftStr != null && !leftStr.isEmpty()) ? leftStr : "?"), 16, inspectY + 21);
        } else {
            g2.setColor(new Color(180, 190, 220));
            g2.drawString("MULTIPROGRAMMING TRACE // EVENT-DRIVEN EXECUTION", 16, inspectY + 11);
            g2.setFont(new Font("Consolas", Font.PLAIN, 8));
            g2.drawString("Click any trace step row above to inspect time interval & remaining burst.", 16, inspectY + 21);
        }

        // 5. Formula & Multiprogramming Principle Footer
        int footerY = 182;
        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        g2.drawString("• et = TIME - prev TIME  |  %cpu = 1 / nj  |  hw = et * %cpu", 10, footerY);

        g2.setColor(new Color(180, 190, 220));
        g2.setFont(new Font("Consolas", Font.PLAIN, 8));
        g2.drawString("• TIME LEFT = prev TIME LEFT - hw  |  0.000 [FINISHED] on complete.", 10, footerY + 12);
        g2.drawString("• Multiprogramming allows shorter bursts to complete earlier on shared CPU.", 10, footerY + 24);
    }

    private JPanel createFormulaChipsCard() {
        JPanel chipsCard = new JPanel(new BorderLayout(0, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color activeTheme = MOD_COLORS[currentModuleIdx];

                g2.setColor(new Color(16, 12, 32, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                g2.setColor(new Color(activeTheme.getRed(), activeTheme.getGreen(), activeTheme.getBlue(), 90));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chipsCard.setOpaque(false);
        chipsCard.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        chipsCard.setPreferredSize(new Dimension(340, 140));

        JLabel lChips = new JLabel("⚡ KEY FORMULAS & LOGIC RULES");
        lChips.setFont(new Font("Consolas", Font.BOLD, 11));
        lChips.setForeground(RetroTheme.ACCENT_CYAN);
        chipsCard.add(lChips, BorderLayout.NORTH);

        formulaPanel = new JPanel();
        formulaPanel.setOpaque(false);
        formulaPanel.setLayout(new BoxLayout(formulaPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollFormulas = RetroTheme.createRetroScroll(formulaPanel);
        scrollFormulas.setBorder(BorderFactory.createEmptyBorder());
        chipsCard.add(scrollFormulas, BorderLayout.CENTER);
        return chipsCard;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. BOTTOM 3D ARCADE ACTION CONTROLS DECK
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel createBottomActionDeck() {
        JPanel deck = new JPanel(new BorderLayout(8, 0));
        deck.setOpaque(false);

        lblFeedback = new JLabel("STATUS: Double-click any glowing [ ? ] cell to calculate and type your answer.");
        lblFeedback.setFont(new Font("Consolas", Font.BOLD, 11));
        lblFeedback.setForeground(new Color(210, 230, 255));
        deck.add(lblFeedback, BorderLayout.WEST);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnRow.setOpaque(false);

        btnPrevStage = createElevatedArcadeBtn("◀ PREV [P]", new Color(42, 32, 68), Color.WHITE, 34);
        btnPrevStage.setToolTipText("Review previous completed mission [P]");
        btnPrevStage.addActionListener(e -> goToPreviousMission());
        btnPrevStage.setEnabled(false);
        btnRow.add(btnPrevStage);

        JButton btnCheck = createElevatedArcadeBtn("⚡ VERIFY / SUBMIT [ENTER]", new Color(0, 170, 75), Color.WHITE, 34);
        btnCheck.setToolTipText("Verify calculations and check accuracy [ENTER]");
        btnCheck.addActionListener(e -> checkAnswers());
        btnRow.add(btnCheck);

        btnNextStage = createElevatedArcadeBtn("NEXT MISSION ▶ [N]", new Color(0, 140, 210), Color.WHITE, 34);
        btnNextStage.setToolTipText("Advance to next operating system mission [N]");
        btnNextStage.addActionListener(e -> advanceToNextMission());
        btnRow.add(btnNextStage);

        JButton btnRandomize = createElevatedArcadeBtn("🎲 NEW SCENARIO [R]", new Color(150, 40, 150), Color.WHITE, 34);
        btnRandomize.setToolTipText("Generate fresh randomized math scenario [R]");
        btnRandomize.addActionListener(e -> randomizeCurrentQuestion());
        btnRow.add(btnRandomize);

        JButton btnReveal = createElevatedArcadeBtn("💡 SOLUTION [SPACE]", new Color(0, 130, 170), Color.WHITE, 34);
        btnReveal.setToolTipText("Show full step-by-step solution derivation [SPACE]");
        btnReveal.addActionListener(e -> revealAnswers());
        btnRow.add(btnReveal);

        JButton btnReset = createElevatedArcadeBtn("↺ RESET", new Color(175, 50, 60), Color.WHITE, 34);
        btnReset.setToolTipText("Reset user answers back to initial blanks");
        btnReset.addActionListener(e -> resetWorksheet());
        btnRow.add(btnReset);

        deck.add(btnRow, BorderLayout.EAST);
        return deck;
    }

    private JButton createElevatedArcadeBtn(String text, Color baseColor, Color textColor, int h) {
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
                if (!pressed) {
                    g2.setColor(new Color(8, 4, 16, 180));
                    g2.fillRoundRect(1, 3, w - 2, bh - 3, 6, 6);
                }

                Color topC = hover ? baseColor.brighter() : baseColor;
                Color botC = hover ? baseColor : baseColor.darker();
                GradientPaint gp = new GradientPaint(0, yOff, topC, 0, bh - 3 + yOff, botC);
                g2.setPaint(gp);
                g2.fillRoundRect(0, yOff, w, bh - 3, 6, 6);

                g2.setColor(new Color(255, 255, 255, hover ? 65 : 30));
                g2.fillRoundRect(1, yOff + 1, w - 2, (bh / 2) - 1, 5, 5);

                g2.setColor(new Color(255, 255, 255, hover ? 150 : 50));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, yOff, w - 1, bh - 4, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Consolas", Font.BOLD, 10));
        btn.setForeground(textColor);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        int prefW = btn.getFontMetrics(btn.getFont()).stringWidth(text) + 24;
        btn.setPreferredSize(new Dimension(Math.max(prefW, 55), h));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void updateFormulaChips(String formulaTip) {
        formulaPanel.removeAll();
        String clean = formulaTip;
        if (clean.startsWith("FORMULAS: ")) clean = clean.substring(10);
        else if (clean.startsWith("FORMULA: ")) clean = clean.substring(9);
        else if (clean.startsWith("RULE: ")) clean = clean.substring(6);

        String[] parts = clean.split("\\|");
        for (String part : parts) {
            String rule = part.trim();
            if (rule.isEmpty()) continue;

            JPanel chip = new JPanel(new BorderLayout(6, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth();
                    int h = getHeight();

                    // Silicon IC Resin Body
                    g2.setColor(new Color(24, 18, 42));
                    g2.fillRoundRect(5, 2, w - 10, h - 4, 4, 4);

                    // Pin-1 Notch on left edge
                    g2.setColor(new Color(12, 8, 22));
                    g2.fillArc(3, h / 2 - 3, 5, 6, -90, 180);

                    // Top and bottom metallic silver solder pins
                    g2.setColor(new Color(195, 205, 225));
                    for (int px = 14; px < w - 14; px += 14) {
                        g2.fillRect(px, 0, 4, 2);      // Top pin
                        g2.fillRect(px, h - 2, 4, 2);  // Bottom pin
                    }

                    // Gold laser-etched border
                    Color activeTheme = MOD_COLORS[currentModuleIdx];
                    g2.setColor(new Color(activeTheme.getRed(), activeTheme.getGreen(), activeTheme.getBlue(), 120));
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(5, 2, w - 11, h - 5, 4, 4);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            chip.setOpaque(false);
            chip.setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 10));
            chip.setAlignmentX(Component.LEFT_ALIGNMENT);
            chip.setMaximumSize(new Dimension(800, 26));

            JLabel icon = new JLabel("►");
            icon.setFont(new Font("Consolas", Font.BOLD, 11));
            icon.setForeground(RetroTheme.ACCENT_CYAN);
            chip.add(icon, BorderLayout.WEST);

            JLabel text = new JLabel(rule);
            text.setFont(new Font("Consolas", Font.BOLD, 11));
            text.setForeground(Color.WHITE);
            chip.add(text, BorderLayout.CENTER);

            formulaPanel.add(chip);
            formulaPanel.add(Box.createVerticalStrut(3));
        }
        formulaPanel.revalidate();
        formulaPanel.repaint();
    }

    private void updateProgressMeter() {
        if (activeQuestion == null || activeQuestion.worksheetData == null) return;
        int rows = tableModel.getRowCount();
        int cols = tableModel.getColumnCount();
        int totalBlanks = 0;
        int filled = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r < activeQuestion.worksheetData.length && c < activeQuestion.worksheetData[r].length
                        && activeQuestion.worksheetData[r][c].trim().equals("?")) {
                    totalBlanks++;
                    Object v = tableModel.getValueAt(r, c);
                    String s = (v == null) ? "" : v.toString().trim();
                    if (!s.isEmpty() && !s.equals("?") && !s.equals("[ ? ]")) {
                        filled++;
                    }
                }
            }
        }

        int pct = (totalBlanks > 0) ? (filled * 100 / totalBlanks) : 0;
        cipherProgressBar.setValue(pct);
        lblProgressText.setText("CALCULATION PROGRESS: " + filled + "/" + totalBlanks + " (" + pct + "%)");

        if (filled == totalBlanks && totalBlanks > 0) {
            lblStatusBadge.setText(" [ READY TO VERIFY ]");
            lblStatusBadge.setForeground(RetroTheme.ACCENT_GREEN);
        } else {
            lblStatusBadge.setText(" [ ENCRYPTION LOCKED ]");
            lblStatusBadge.setForeground(RetroTheme.ACCENT_YELLOW);
        }
    }

    private BufferedImage getMascotSprite(int modIdx, int size) {
        Color c = MOD_COLORS[modIdx];
        switch (modIdx) {
            case 0: return PixelSprites.createAlienSprite(size, c);
            case 1: return PixelSprites.createSpaceShootShip(size, c);
            case 2: return PixelSprites.createFrogSprite(size, c);
            case 3: return PixelSprites.createMarioSprite(size);
            case 4: return PixelSprites.createSnakeSprite(size, c);
            case 5: return PixelSprites.createCraneSprite(size, c);
            case 6: return PixelSprites.createPacman(size, 0.4f, true);
            case 7: return PixelSprites.createChefSprite(size, false);
            default: return PixelSprites.createAlienSprite(size, c);
        }
    }

    public void randomizeCurrentQuestion() {
        playRandomizeSfx();
        loadQuestion(currentModuleIdx, currentDifficulty, currentMode, true);
        showToast("🎲 SCENARIO RE-ROLLED! FRESH MATHEMATICAL PROBLEM GENERATED");
    }

    private void loadQuestion(int modIdx, QuizManager.Mode mode, boolean forceRandom) {
        loadQuestion(modIdx, currentDifficulty, mode, forceRandom);
    }

    private void loadQuestion(int modIdx, QuizManager.Difficulty diff, QuizManager.Mode mode, boolean forceRandom) {
        updatingTable = true;
        this.currentDifficulty = (diff != null) ? diff : QuizManager.Difficulty.AVERAGE;
        activeQuestion = QuizManager.getQuestionByModule(modIdx, currentDifficulty, mode);

        lblBreadcrumb.setText("OS CAMPAIGN // MISSION 0" + (modIdx + 1) + " OF 08");
        lblMissionTitle.setText(ARCHIVE_STAGE_TITLES[modIdx]);
        lblMissionDesc.setText("🎯 OBJECTIVE: " + MOD_OBJECTIVES[modIdx]);

        if (lblDossierTitle != null) {
            lblDossierTitle.setText(ARCHIVE_STAGE_TITLES[modIdx]);
        }
        if (lblDossierCategory != null) {
            lblDossierCategory.setText(" [ " + STAGE_CATEGORIES[modIdx] + " ] ");
            lblDossierCategory.setBackground(MOD_COLORS[modIdx]);
        }
        updateDifficultyUI();

        updateFormulaChips(activeQuestion.formulaTip);
        updateDiagnosticPods();

        if (lblTableDossierTag != null) {
            if (modIdx == 3) {
                lblTableDossierTag.setText(" STAGE 04 // WARP PIPE MMU ");
            } else if (modIdx == 5) {
                lblTableDossierTag.setText(" STAGE 06 // SILICON RAM PARTITIONS ");
            } else if (modIdx == 6) {
                lblTableDossierTag.setText(" STAGE 07 // PAC-MAN CYLINDER DISK ");
            } else if (modIdx == 7) {
                lblTableDossierTag.setText(" STAGE 08 // MULTIPROGRAMMING TRACE ");
            } else {
                lblTableDossierTag.setText(" " + ARCHIVE_STAGE_TITLES[modIdx] + " ");
            }
        }
        if (lblTableVaultAssets != null) {
            if (modIdx == 1 && activeQuestion.initialAvailable != null && activeQuestion.initialAvailable.length >= 3) {
                int[] v = activeQuestion.initialAvailable;
                lblTableVaultAssets.setText(String.format("  VAULT ASSETS // AVAILABLE: [ CORE A: %d | CORE B: %d | CORE C: %d ]  ", v[0], v[1], v[2]));
                lblTableVaultAssets.setVisible(true);
            } else {
                lblTableVaultAssets.setVisible(false);
            }
        }
        if (pnlMarioPolicyStrip != null) {
            if (modIdx == 3) {
                pnlMarioPolicyStrip.setVisible(true);
                updatePolicyButtons();
            } else {
                pnlMarioPolicyStrip.setVisible(false);
            }
        }
        if (pnlCranePolicyStrip != null) {
            if (modIdx == 5) {
                pnlCranePolicyStrip.setVisible(true);
                updateCranePolicyButtons();
            } else {
                pnlCranePolicyStrip.setVisible(false);
            }
        }
        if (pnlPacmanPolicyStrip != null) {
            if (modIdx == 6) {
                pnlPacmanPolicyStrip.setVisible(true);
                updatePacmanPolicyButtons();
            } else {
                pnlPacmanPolicyStrip.setVisible(false);
            }
        }
        if (pnlKitchenStrip != null) {
            pnlKitchenStrip.setVisible(modIdx == 7);
        }

        int rows = activeQuestion.worksheetData.length;
        int cols = activeQuestion.worksheetData[0].length;
        cellValidationState = new int[rows][cols];

        tableModel.setDataVector(cloneGrid(activeQuestion.worksheetData), activeQuestion.columnHeaders);
        updatingTable = false;

        // Auto-fit column widths so no headers or values are truncated across all 8 modules!
        if (cipherTable != null && cipherTable.getColumnModel().getColumnCount() == cols) {
            if (modIdx == 0 && cols == 7) {
                cipherTable.getColumnModel().getColumn(0).setPreferredWidth(145); // WAVE (e.g. W3 (Interceptor))
                cipherTable.getColumnModel().getColumn(1).setPreferredWidth(95);  // ARRIVAL (AT)
                cipherTable.getColumnModel().getColumn(2).setPreferredWidth(85);  // BURST (BT)
                cipherTable.getColumnModel().getColumn(3).setPreferredWidth(85);  // START (ST)
                cipherTable.getColumnModel().getColumn(4).setPreferredWidth(85);  // FINISH (FT)
                cipherTable.getColumnModel().getColumn(5).setPreferredWidth(85);  // WAIT (WT)
                cipherTable.getColumnModel().getColumn(6).setPreferredWidth(90);  // TURN (TAT)
            } else if (modIdx == 1 && cols == 7) {
                cipherTable.getColumnModel().getColumn(0).setPreferredWidth(155); // CLIENT / HEIST (e.g. P2 (Dreadnought))
                cipherTable.getColumnModel().getColumn(1).setPreferredWidth(95);  // ALLOCATED (A B C)
                cipherTable.getColumnModel().getColumn(2).setPreferredWidth(95);  // MAX DEMAND (A B C)
                cipherTable.getColumnModel().getColumn(3).setPreferredWidth(90);  // NEED (A B C)
                cipherTable.getColumnModel().getColumn(4).setPreferredWidth(90);  // AVAILABLE (A B C)
                cipherTable.getColumnModel().getColumn(5).setPreferredWidth(85);  // CAN EXECUTE?
                cipherTable.getColumnModel().getColumn(6).setPreferredWidth(90);  // WORK AFTER (A B C)
            } else if (modIdx == 2 && cols == 6) {
                cipherTable.getColumnModel().getColumn(0).setPreferredWidth(100); // LOGICAL ADDR
                cipherTable.getColumnModel().getColumn(1).setPreferredWidth(95);  // PAGE SIZE (S)
                cipherTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // PAGE # (p)
                cipherTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // OFFSET (d)
                cipherTable.getColumnModel().getColumn(4).setPreferredWidth(90);  // FRAME # (f)
                cipherTable.getColumnModel().getColumn(5).setPreferredWidth(135); // PHYSICAL ADDR (PA)
            } else if (modIdx == 3 && cols == 6) {
                cipherTable.getColumnModel().getColumn(0).setPreferredWidth(70);  // STEP #
                cipherTable.getColumnModel().getColumn(1).setPreferredWidth(85);  // PAGE REF
                cipherTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // PIPE #0
                cipherTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // PIPE #1
                cipherTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // PIPE #2
                cipherTable.getColumnModel().getColumn(5).setPreferredWidth(95);  // STATUS (H/F)
            } else if (modIdx == 4 && cols == 7) {
                cipherTable.getColumnModel().getColumn(0).setPreferredWidth(60);  // SEG #
                cipherTable.getColumnModel().getColumn(1).setPreferredWidth(110); // SEGMENT NAME
                cipherTable.getColumnModel().getColumn(2).setPreferredWidth(85);  // BASE ADDR
                cipherTable.getColumnModel().getColumn(3).setPreferredWidth(90);  // LIMIT BOUND
                cipherTable.getColumnModel().getColumn(4).setPreferredWidth(85);  // OFFSET (d)
                cipherTable.getColumnModel().getColumn(5).setPreferredWidth(75);  // STATUS
                cipherTable.getColumnModel().getColumn(6).setPreferredWidth(115); // PHYSICAL ADDR
            } else if (modIdx == 5 && cols == 7) {
                cipherTable.getColumnModel().getColumn(0).setPreferredWidth(70);   // IC / CHIP
                cipherTable.getColumnModel().getColumn(1).setPreferredWidth(90);   // DEMAND SIZE
                cipherTable.getColumnModel().getColumn(2).setPreferredWidth(88);   // BASE ADDRESS
                cipherTable.getColumnModel().getColumn(3).setPreferredWidth(92);   // LIMIT / CEILING
                cipherTable.getColumnModel().getColumn(4).setPreferredWidth(120);  // SOCKET ASSIGNED
                cipherTable.getColumnModel().getColumn(5).setPreferredWidth(85);   // INTERNAL FRAG
                cipherTable.getColumnModel().getColumn(6).setPreferredWidth(115);  // ALLOCATION STATUS
            } else if (modIdx == 6 && cols == 6) {
                cipherTable.getColumnModel().getColumn(0).setPreferredWidth(75);   // STEP #
                cipherTable.getColumnModel().getColumn(1).setPreferredWidth(100);  // CURRENT TRACK
                cipherTable.getColumnModel().getColumn(2).setPreferredWidth(125);  // NEXT TRACK ACCESSED
                cipherTable.getColumnModel().getColumn(3).setPreferredWidth(105);  // TRACKS TRAVERSED
                cipherTable.getColumnModel().getColumn(4).setPreferredWidth(105);  // SEEK DIRECTION
                cipherTable.getColumnModel().getColumn(5).setPreferredWidth(110);  // CUMULATIVE SEEK
            } else if (modIdx == 7 && cols == 8) {
                cipherTable.getColumnModel().getColumn(0).setPreferredWidth(65);  // TIME
                cipherTable.getColumnModel().getColumn(1).setPreferredWidth(125); // EVENT
                cipherTable.getColumnModel().getColumn(2).setPreferredWidth(45);  // nj
                cipherTable.getColumnModel().getColumn(3).setPreferredWidth(75);  // %cpu/job
                cipherTable.getColumnModel().getColumn(4).setPreferredWidth(55);  // et
                cipherTable.getColumnModel().getColumn(5).setPreferredWidth(70);  // hw/job
                cipherTable.getColumnModel().getColumn(6).setPreferredWidth(55);  // JOB#
                cipherTable.getColumnModel().getColumn(7).setPreferredWidth(110); // TIME LEFT
            } else if (modIdx == 7 && cols == 7) {
                cipherTable.getColumnModel().getColumn(0).setPreferredWidth(65);   // JOB#
                cipherTable.getColumnModel().getColumn(1).setPreferredWidth(95);   // DISH TYPE
                cipherTable.getColumnModel().getColumn(2).setPreferredWidth(95);   // ARRIVAL (AT)
                cipherTable.getColumnModel().getColumn(3).setPreferredWidth(95);   // BURST (BT)
                cipherTable.getColumnModel().getColumn(4).setPreferredWidth(95);   // FINISH (FT)
                cipherTable.getColumnModel().getColumn(5).setPreferredWidth(105);  // TURNAROUND (TAT)
                cipherTable.getColumnModel().getColumn(6).setPreferredWidth(105);  // WAITING (WT)
            } else {
                cipherTable.getColumnModel().getColumn(0).setPreferredWidth(120);
                for (int c = 1; c < cols; c++) {
                    cipherTable.getColumnModel().getColumn(c).setPreferredWidth(90);
                }
            }
        }

        // Dynamically size scrollTable so the tableCard hugs the rows tightly!
        int tableHeight = Math.min(290, Math.max(130, rows * 30 + 34));
        if (scrollTable != null) {
            scrollTable.setPreferredSize(new Dimension(680, tableHeight));
        }

        updateProgressMeter();
        updateStageProgressStrip();

        lblFeedback.setText("STATUS: Double-click any glowing [ ? ] cell to calculate and type your answer.");
        lblFeedback.setForeground(new Color(210, 230, 255));

        // Auto-select the first [ ? ] cell so the Step Inspector immediately illuminates!
        int firstBlankR = -1, firstBlankC = -1;
        for (int r = 0; r < rows; r++) {
            for (int c = 1; c < cols; c++) {
                if (activeQuestion.worksheetData[r][c].trim().equals("?")) {
                    firstBlankR = r;
                    firstBlankC = c;
                    break;
                }
            }
            if (firstBlankR >= 0) break;
        }
        if (firstBlankR >= 0 && firstBlankC >= 0) {
            cipherTable.changeSelection(firstBlankR, firstBlankC, false, false);
            updateContextualInspector();
        }

        loadSequenceChallenge();

        revalidate();
        repaint();
    }

    private void updateDiagnosticPods() {
        switch (currentModuleIdx) {
            case 0:
                lblMetricPod1Title.setText("METRIC: FCFS LATENCY & AGGREGATES");
                lblMetricPod1Val.setText("<html>Wait: <b>TAT - BT</b> | Avg WT: <b>Sum/N</b><br>Turn: <b>FT - AT</b> | Avg TAT: <b>Sum/N</b></html>");
                lblMetricPod2Title.setText("DISPATCH: NON-PREEMPTIVE");
                lblMetricPod2Val.setText("<html>Strict arrival order.<br>Convoy warning on high burst.</html>");
                lblMetricPod3Title.setText("REWARD: +150 XP");
                lblMetricPod3Val.setText("<html>Solve all blanks for<br><b>S-Rank Clear</b>.</html>");
                break;
            case 1:
                lblMetricPod1Title.setText("METRIC: DIJKSTRA SAFETY");
                lblMetricPod1Val.setText("<html>Condition: <b>Need <= Available</b><br>Safe sequence search.</html>");
                lblMetricPod2Title.setText("VECTOR: A=GOLD, B=DATA, C=PLASMA");
                lblMetricPod2Val.setText("<html>Need = Max Claim - Alloc.<br>Prevents system deadlock.</html>");
                lblMetricPod3Title.setText("REWARD: +150 XP");
                lblMetricPod3Val.setText("<html>Deadlock-Free State<br>Verification.</html>");
                break;
            case 2:
                lblMetricPod1Title.setText("METRIC: MMU TRANSLATION");
                lblMetricPod1Val.setText("<html>Page #: <b>Addr / PageSize</b><br>Offset: <b>Addr % PageSize</b></html>");
                lblMetricPod2Title.setText("RAM FRAME DOCK MAPPING");
                lblMetricPod2Val.setText("<html>Physical = (Frame * PS) + Offset.<br>Hardware table lookup.</html>");
                lblMetricPod3Title.setText("REWARD: +150 XP");
                lblMetricPod3Val.setText("<html>Memory bus routing<br>accuracy.</html>");
                break;
            case 3:
                lblMetricPod1Title.setText("METRIC: CACHE HIT RATIO");
                lblMetricPod1Val.setText("<html>Ratio = <b>(Hits / Refs) * 100%</b><br>Minimizes page faults.</html>");
                lblMetricPod2Title.setText("POLICIES: FIFO vs LRU");
                lblMetricPod2Val.setText("<html>FIFO: Evict oldest reference.<br>LRU: Evict least recently used.</html>");
                lblMetricPod3Title.setText("REWARD: +150 XP");
                lblMetricPod3Val.setText("<html>Cache efficiency<br>mastery.</html>");
                break;
            case 4:
                lblMetricPod1Title.setText("METRIC: SEGMENT LIMITS");
                lblMetricPod1Val.setText("<html>Rule: <b>Offset < Limit -> VALID</b><br>Offset >= Limit -> <b>TRAP</b></html>");
                lblMetricPod2Title.setText("BOUNDS CHECKER");
                lblMetricPod2Val.setText("<html>Code, Heap, Stack bounds.<br>Prevents SegFaults.</html>");
                lblMetricPod3Title.setText("REWARD: +150 XP");
                lblMetricPod3Val.setText("<html>Hardware boundary<br>validation.</html>");
                break;
            case 5:
                lblMetricPod1Title.setText("METRIC: RAM PARTITIONS");
                lblMetricPod1Val.setText("<html>Hole >= Process -> <b>YES</b><br>Frag = <b>Hole - Process</b></html>");
                lblMetricPod2Title.setText("FIRST-FIT ALLOCATOR");
                lblMetricPod2Val.setText("<html>Dynamic contiguous RAM.<br>Tracks internal fragmentation.</html>");
                lblMetricPod3Title.setText("REWARD: +150 XP");
                lblMetricPod3Val.setText("<html>Memory utilization<br>efficiency.</html>");
                break;
            case 6:
                lblMetricPod1Title.setText("METRIC: CYLINDER SEEK");
                lblMetricPod1Val.setText("<html>Seek = <b>|Target - Head|</b><br>Total = Running sum of seeks.</html>");
                lblMetricPod2Title.setText("PLATTER: 0 TO 199 TRACKS");
                lblMetricPod2Val.setText("<html>Head traversal latency.<br>Tracks seek distance.</html>");
                lblMetricPod3Title.setText("REWARD: +150 XP");
                lblMetricPod3Val.setText("<html>Disk actuator<br>optimization.</html>");
                break;
            case 7:
                lblMetricPod1Title.setText("METRIC: %CPU / JOB");
                lblMetricPod1Val.setText("<html>Share = <b>1 / nj</b><br>hw/job = <b>et * (%cpu/job)</b></html>");
                lblMetricPod2Title.setText("EVENT TRACE MODEL");
                lblMetricPod2Val.setText("<html>Event-driven intervals.<br>Discrete time-step simulation.</html>");
                lblMetricPod3Title.setText("REWARD: +" + currentDifficulty.xpReward + " XP");
                lblMetricPod3Val.setText("<html>Multiprogramming trace<br>verification.</html>");
                break;
        }

        lblMetricPod3Title.setText("REWARD: +" + currentDifficulty.xpReward + " XP");
        lblMetricPod3Val.setText("<html>Tier: <b>" + currentDifficulty.label + " (" + currentDifficulty.tier + ")</b><br>Score Multiplier: <b>" + currentDifficulty.multiplier + "x</b></html>");
    }

    private String[][] cloneGrid(String[][] orig) {
        String[][] copy = new String[orig.length][orig[0].length];
        for (int r = 0; r < orig.length; r++) {
            System.arraycopy(orig[r], 0, copy[r], 0, orig[r].length);
        }
        return copy;
    }

    private void checkAnswers() {
        if (cipherTable != null && cipherTable.isEditing() && cipherTable.getCellEditor() != null) {
            cipherTable.getCellEditor().stopCellEditing();
        }
        if (activeQuestion == null || activeQuestion.worksheetData == null) return;

        int rows = tableModel.getRowCount();
        int cols = tableModel.getColumnCount();
        if (rows != activeQuestion.worksheetData.length || cols != activeQuestion.worksheetData[0].length) return;

        boolean allCorrect = true;
        int emptyCount = 0;
        int wrongCount = 0;
        int correctCount = 0;
        StringBuilder log = new StringBuilder("=== WORKSHEET AUDIT & EVALUATION REPORT ===\n");
        log.append("Target Algorithm: ").append(activeQuestion.arcadeGameTitle).append("\n\n");

        for (int r = 0; r < rows; r++) {
            boolean rowOk = true;
            String rowName = activeQuestion.worksheetData[r][0];

            for (int c = 0; c < cols; c++) {
                Object val = tableModel.getValueAt(r, c);
                String strVal = (val == null) ? "" : val.toString().trim();
                if (strVal.startsWith("OK ") || strVal.startsWith("ERR ")) strVal = strVal.substring(3).trim();
                else if (strVal.startsWith("✔ ") || strVal.startsWith("✘ ")) strVal = strVal.substring(2).trim();

                boolean isEditable = activeQuestion.worksheetData[r][c].trim().equals("?");
                if (isEditable) {
                    if (strVal.isEmpty() || strVal.equals("?") || strVal.equals("[ ? ]")) {
                        emptyCount++;
                        cellValidationState[r][c] = 2;
                        allCorrect = false;
                        rowOk = false;
                    } else if (QuizManager.isCellCorrect(strVal, activeQuestion.answerKey[r][c])) {
                        cellValidationState[r][c] = 1;
                        correctCount++;
                    } else {
                        cellValidationState[r][c] = 2;
                        allCorrect = false;
                        wrongCount++;
                        rowOk = false;
                    }
                } else {
                    cellValidationState[r][c] = 0;
                }
            }

            if (rowOk) {
                log.append("[OK]  Row ").append(r + 1).append(" (").append(rowName).append("): Parameters decrypted!\n");
            } else {
                log.append("[ERR] Row ").append(r + 1).append(" (").append(rowName).append("): Calculation mismatch.\n");
            }
        }

        if (allCorrect) {
            streakCount++;
            lblComboStreak.setText("COMBO: " + streakCount + "X");
            lblRankBadge.setText("RANK: S-RANK CLEAR");
            lblRankBadge.setForeground(RetroTheme.ACCENT_GREEN);
            lblAccuracyRate.setText("ACCURACY: 100%");
            playVictorySound();
            triggerCelebrationConfetti();

            int earnedXP = (int) (currentDifficulty.xpReward + (streakCount * 25));
            AppState.addScore(earnedXP);
            lblXpScore.setText("XP: " + AppState.getScoreFormatted());
            stageCleared[currentModuleIdx] = true;
            updateStageProgressStrip();

            if (currentModuleIdx < 7) {
                lblFeedback.setText("★ S-RANK CLEAR! +" + earnedXP + " XP! Click [NEXT MISSION ▶] or press [N] to advance! ★");
                lblFeedback.setForeground(RetroTheme.ACCENT_GREEN);
                showToast("★ MISSION 0" + (currentModuleIdx + 1) + " CLEARED! +" + earnedXP + " XP ★");
                if (btnNextStage != null) {
                    btnNextStage.setText("▶ NEXT MISSION [N]");
                    btnNextStage.setBackground(new Color(0, 200, 100));
                }
            } else {
                lblFeedback.setText("🏆 CAMPAIGN COMPLETE! ALL 8 MISSIONS CONQUERED! +" + earnedXP + " XP 🏆");
                lblFeedback.setForeground(RetroTheme.ACCENT_YELLOW);
                showToast("🏆 ALL 8 OS MISSIONS CONQUERED! 🏆");
                if (btnNextStage != null) {
                    btnNextStage.setText("CLAIM VICTORY 🏆");
                    btnNextStage.setBackground(new Color(255, 180, 0));
                }
            }

            log.append("\n===================================\n");
            log.append("★ SCORE: 100% ACCURACY // S-RANK CLEAR ★\n");
            log.append("Difficulty Tier: ").append(currentDifficulty.label).append(" (").append(currentDifficulty.tier).append(")\n");
            log.append("Reward: +").append(earnedXP).append(" XP added to Profile.\n");
            log.append("Consecutive Winning Streak: ").append(streakCount).append(" ★\n");

            txtTerminalLog.setText(log.toString());
            txtTerminalLog.setCaretPosition(0);
        } else {
            streakCount = 0;
            lblComboStreak.setText("COMBO: 0X");
            lblRankBadge.setText("RANK: RETRY NEEDED");
            lblRankBadge.setForeground(RetroTheme.ACCENT_PINK);
            playerShieldPct = Math.max(20, playerShieldPct - 20);
            int pBars = Math.max(1, playerShieldPct / 10);
            StringBuilder sb = new StringBuilder();
            for (int b = 0; b < pBars; b++) sb.append("■");
            lblPlayerShields.setText("SHIELDS: " + playerShieldPct + "%");
            playBuzzerSound();

            int totalEditable = correctCount + wrongCount + emptyCount;
            int acc = totalEditable > 0 ? (correctCount * 100 / totalEditable) : 0;
            lblAccuracyRate.setText("ACCURACY: " + acc + "%");

            log.append("\n===================================\n");
            if (emptyCount > 0) {
                log.append("[!] STATUS: ").append(emptyCount).append(" cell(s) left blank.\n");
                lblFeedback.setText("INCOMPLETE: " + emptyCount + " cell(s) blank. Click any cell to view formula and calculate.");
                showToast("INCOMPLETE: " + emptyCount + " CELL(S) LEFT BLANK");
            } else {
                log.append("[X] STATUS: ").append(wrongCount).append(" calculation error(s) detected.\n");
                lblFeedback.setText("SOME VALUES ARE INCORRECT. Check red cells and Step Inspector below.");
                showToast("EVALUATION FAILED: " + wrongCount + " INCORRECT CELLS");
            }
            log.append("Review the formula breakdown on the right or click [SOLUTION].");
            lblFeedback.setForeground(RetroTheme.ACCENT_PINK);

            txtTerminalLog.setText(log.toString());
            txtTerminalLog.setCaretPosition(0);
        }
        repaint();
    }

    private void revealAnswers() {
        if (cipherTable != null && cipherTable.isEditing() && cipherTable.getCellEditor() != null) {
            cipherTable.getCellEditor().cancelCellEditing();
        }
        playTone(660, 40);
        updatingTable = true;
        int rows = activeQuestion.answerKey.length;
        int cols = activeQuestion.answerKey[0].length;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                tableModel.setValueAt(activeQuestion.answerKey[r][c], r, c);
                boolean isEditable = activeQuestion.worksheetData[r][c].trim().equals("?");
                cellValidationState[r][c] = isEditable ? 1 : 0;
            }
        }
        updatingTable = false;
        updateProgressMeter();
        updateStageProgressStrip();

        txtTerminalLog.setText("★ " + activeQuestion.arcadeGameTitle + " // STEP-BY-STEP MATHEMATICAL SOLUTION ★\n\n"
                + activeQuestion.explanation + "\n\n"
                + "> TELEMETRY NOTE: All correct answers have been synchronized into the matrix to your left.");
        txtTerminalLog.setCaretPosition(0);

        lblFeedback.setText("SOLUTION REVEALED. Study the step-by-step telemetry breakdown on the right.");
        lblFeedback.setForeground(RetroTheme.ACCENT_YELLOW);
        showToast("SOLUTION REVEALED // STUDY TELEMETRY BREAKDOWN");

        revalidate();
        repaint();
    }

    private void resetWorksheet() {
        if (cipherTable != null && cipherTable.isEditing() && cipherTable.getCellEditor() != null) {
            cipherTable.getCellEditor().cancelCellEditing();
        }
        playTone(480, 20);
        updatingTable = true;
        int rows = activeQuestion.worksheetData.length;
        int cols = activeQuestion.worksheetData[0].length;
        cellValidationState = new int[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                tableModel.setValueAt(activeQuestion.worksheetData[r][c], r, c);
            }
        }
        updatingTable = false;
        updateProgressMeter();
        updateStageProgressStrip();
        lblFeedback.setText("WORKSHEET RESET. Click any glowing [ ? ] cell to view its formula.");
        lblFeedback.setForeground(RetroTheme.TEXT_DIM);
        showToast("MATRIX RESET TO INITIAL STATE");
        repaint();
    }

    private void launchActiveMinigame() {
        playTone(700, 30);
        try {
            switch (currentModuleIdx) {
                case 0: new KernelTapperDialog(parentFrame, "FCFS", null).setVisible(true); break;
                case 1: new VaultPanicDialog(parentFrame, null, null, null).setVisible(true); break;
                case 2: new FrameHopperDialog(parentFrame, 4096, null, 8).setVisible(true); break;
                case 3: new MarioCacheRunnerDialog(parentFrame, null, 3, "FIFO").setVisible(true); break;
                case 4: game.snake.SnakeGameDialog.launchDefault(parentFrame, null); break;
                case 5: new DefragPanicDialog(parentFrame, null, null, null).setVisible(true); break;
                case 6: new PacmanDiskArcadeDialog(parentFrame, null, 53, 199, "FCFS").setVisible(true); break;
                case 7: new OvercookedKitchenDialog(parentFrame, null).setVisible(true); break;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not launch minigame: " + ex.getMessage(),
                    "Arcade Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void playTone(int freq, int durationMs) {
        if (!soundEnabled) return;
        new Thread(() -> {
            try {
                byte[] buf = new byte[durationMs * 16];
                for (int i = 0; i < buf.length; i++) {
                    double angle = i / (16000.0 / freq) * 2.0 * Math.PI;
                    buf[i] = (byte) (Math.sin(angle) * 80);
                }
                AudioFormat af = new AudioFormat(16000f, 8, 1, true, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();
                sdl.write(buf, 0, buf.length);
                sdl.drain();
                sdl.close();
            } catch (Exception ignored) {}
        }).start();
    }

    public static void playBlip() { playTone(920, 25); }

    private void playRandomizeSfx() {
        new Thread(() -> {
            playTone(380, 25);
            try { Thread.sleep(30); } catch (Exception ignored) {}
            playTone(520, 25);
            try { Thread.sleep(30); } catch (Exception ignored) {}
            playTone(680, 35);
        }).start();
    }

    private void playVictorySound() {
        new Thread(() -> {
            playTone(523, 60);
            try { Thread.sleep(70); } catch (Exception ignored) {}
            playTone(659, 60);
            try { Thread.sleep(70); } catch (Exception ignored) {}
            playTone(784, 60);
            try { Thread.sleep(70); } catch (Exception ignored) {}
            playTone(1046, 140);
        }).start();
    }

    private void playBuzzerSound() {
        new Thread(() -> {
            playTone(180, 90);
            try { Thread.sleep(50); } catch (Exception ignored) {}
            playTone(140, 130);
        }).start();
    }
}
