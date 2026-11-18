package arcadeos;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Screen: QUIZ MODE // ALGORITHM MASTERY WORKSHEET ARENA
 * -----------------------------------------------------------------------------
 * Dual-Pane Tactical Arcade Battlefield:
 * - Left 63%:
 *     1. Mission Stage Header (48x48 pixel mascot, arcade theme, mission lore)
 *     2. Interactive Worksheet Table (glowing [ ? ] cells, neon green/red validation)
 *     3. Visual Telemetry Schematic (Dynamic Gantt Chart, Memory Map, or Disk Track)
 *     4. Real-time Completion Progress Meter
 *     5. Tactile 3D Arcade Control Deck ([CHECK ANSWERS], [REVEAL], [RESET], [PLAY MINIGAME])
 * - Right 37%:
 *     1. Algorithm Formula Chips (glowing neon chips, formatted equations)
 *     2. Telemetry & Step-by-Step Solution Log Terminal
 * - Top & Bottom:
 *     1. High-contrast Cartridge Ribbon (all 8 games, zero missing glyphs)
 *     2. Status bar with keyboard shortcut hints
 */
public class QuizPanel extends JPanel {
    private final ArcadeOS parentFrame;
    private QuizManager.Mode currentMode = QuizManager.Mode.FILL_MISSING;
    private int currentModuleIdx = 0; // 0 to 7
    private QuizManager.QuizQuestion activeQuestion;

    // Module Cartridge Titles (ASCII-clean, no broken emoji glyphs in Consolas)
    private final JButton[] modButtons = new JButton[8];
    private static final String[] MOD_TITLES = {
            "01 INVADERS", "02 SPACE", "03 HOPPER", "04 MARIO",
            "05 SNAKE", "06 CRANE", "07 PAC-MAN", "08 OVERCOOKED"
    };

    private static final Color[] MOD_COLORS = {
            new Color(5, 217, 232),   // 01 Invaders Cyan
            new Color(255, 211, 25),  // 02 Space Shoot Gold
            new Color(40, 255, 200),  // 03 Frame Hopper Mint
            new Color(255, 115, 115), // 04 Mario Coral
            new Color(90, 245, 140),  // 05 Cyber Snake Green
            new Color(215, 145, 255), // 06 Cyber Crane Violet
            new Color(255, 230, 50),  // 07 Pac-Man Yellow
            new Color(255, 150, 50)   // 08 Overcooked Orange
    };

    // Mode Buttons
    private JButton btnModeFill;
    private JButton btnModeSolve;

    // Stage Header UI Elements
    private JLabel lblMascotAvatar;
    private JLabel lblModuleTitle;
    private JLabel lblGameSubtitle;
    private JLabel lblInstruction;
    private JPanel missionCard;

    // Worksheet UI Elements
    private JTable quizTable;
    private DefaultTableModel tableModel;
    private JProgressBar progressBar;
    private JLabel lblProgressText;
    private JLabel lblStatusBadge;
    private MiniAlgorithmSchematicPanel schematicPanel;

    // Right Pane Elements (Formulas & Telemetry Terminal)
    private JPanel formulaPanel;
    private JTextArea txtTerminalLog;
    private JPanel rightPaneContainer;

    // Top & Bottom Status Elements
    private JLabel lblFeedback;
    private JLabel lblXpScore;
    private JLabel lblStreak;

    // Per-cell validation status: 0=normal, 1=correct, 2=incorrect
    private int[][] cellValidationState;
    private boolean soundEnabled = true;
    private int streakCount = 0;
    private boolean updatingTable = false;

    public QuizPanel(ArcadeOS parent) {
        this.parentFrame = parent;
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 16, 12, 16));

        // 1. Top Retro Header Bar
        add(createTopBar(), BorderLayout.NORTH);

        // 2. Center Content Split Battlefield
        add(createCenterBattlefield(), BorderLayout.CENTER);

        // 3. Bottom Retro Status Bar
        add(createBottomStatusBar(), BorderLayout.SOUTH);

        // Load default question (Module 01: CPU Invaders FCFS)
        loadQuestion(0, QuizManager.Mode.FILL_MISSING);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        // Left button: Return to Main Menu
        JButton btnBack = RetroTheme.createRetroButton("< MAIN MENU", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnBack.setFont(new Font("Consolas", Font.BOLD, 11));
        btnBack.setFocusable(false);
        btnBack.addActionListener(e -> {
            playTone(480, 20);
            if (parentFrame != null) {
                parentFrame.showScreen(ArcadeOS.SCREEN_MAIN_MENU);
            }
        });
        bar.add(btnBack, BorderLayout.WEST);

        // Center: Arcade Title Badge
        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        titleBox.setOpaque(false);

        JLabel star = new JLabel("*");
        star.setFont(new Font("Consolas", Font.BOLD, 16));
        star.setForeground(RetroTheme.ACCENT_PINK);

        JLabel title = new JLabel("ARCADE OS // ALGORITHM MASTERY WORKSHEET ARENA");
        title.setFont(new Font("Consolas", Font.BOLD, 14));
        title.setForeground(Color.WHITE);

        JLabel subBadge = new JLabel("[ 16-BIT TACTICAL BATTLEFIELD ]");
        subBadge.setFont(new Font("Consolas", Font.BOLD, 11));
        subBadge.setForeground(RetroTheme.ACCENT_YELLOW);

        titleBox.add(star);
        titleBox.add(title);
        titleBox.add(subBadge);
        bar.add(titleBox, BorderLayout.CENTER);

        // Right: Streak, XP Score & Sound Toggle
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        lblStreak = new JLabel("STREAK: 0 WINS");
        lblStreak.setFont(new Font("Consolas", Font.BOLD, 11));
        lblStreak.setForeground(new Color(255, 140, 50));
        right.add(lblStreak);

        lblXpScore = new JLabel("XP: " + AppState.getScoreFormatted() + "   ");
        lblXpScore.setFont(new Font("Consolas", Font.BOLD, 11));
        lblXpScore.setForeground(RetroTheme.ACCENT_YELLOW);
        right.add(lblXpScore);

        JButton btnSound = RetroTheme.createRetroButton("SOUND: ON", new Color(28, 18, 46), RetroTheme.ACCENT_CYAN);
        btnSound.setFont(new Font("Consolas", Font.BOLD, 10));
        btnSound.setFocusable(false);
        btnSound.addActionListener(e -> {
            soundEnabled = !soundEnabled;
            btnSound.setText(soundEnabled ? "SOUND: ON" : "SOUND: MUTED");
        });
        right.add(btnSound);

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createCenterBattlefield() {
        JPanel battlefield = new JPanel(new BorderLayout(0, 8));
        battlefield.setOpaque(false);

        // 1. Top Cartridges & Mode Switcher Deck
        battlefield.add(createCartridgeRibbon(), BorderLayout.NORTH);

        // 2. Dual-Pane Battlefield Split (Left: Worksheet + Controls, Right: Formulas + Telemetry)
        JPanel splitArena = new JPanel(new GridBagLayout());
        splitArena.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Left Pane (63% width)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.63;
        gbc.insets = new Insets(0, 0, 0, 8);
        splitArena.add(createLeftWorksheetPane(), gbc);

        // Right Pane (37% width)
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.37;
        gbc.insets = new Insets(0, 0, 0, 0);
        splitArena.add(createRightTelemetryPane(), gbc);

        battlefield.add(splitArena, BorderLayout.CENTER);
        return battlefield;
    }

    private JPanel createCartridgeRibbon() {
        JPanel ribbon = new JPanel(new BorderLayout(8, 0));
        ribbon.setOpaque(false);

        // 8 Cartridge Buttons
        JPanel modRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        modRow.setOpaque(false);

        JLabel lblSelect = new JLabel("CARTRIDGES:");
        lblSelect.setFont(new Font("Consolas", Font.BOLD, 10));
        lblSelect.setForeground(RetroTheme.TEXT_DIM);
        modRow.add(lblSelect);

        for (int i = 0; i < 8; i++) {
            final int idx = i;
            JButton btn = new JButton(MOD_TITLES[i]) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean active = (idx == currentModuleIdx);
                    Color theme = MOD_COLORS[idx];

                    // Fill background
                    if (active) {
                        g2.setColor(new Color(theme.getRed() / 4, theme.getGreen() / 4, theme.getBlue() / 4, 220));
                    } else {
                        g2.setColor(new Color(24, 16, 42));
                    }
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                    // Border
                    if (active) {
                        g2.setColor(theme);
                        g2.setStroke(new BasicStroke(2.0f));
                    } else {
                        g2.setColor(new Color(55, 42, 80));
                        g2.setStroke(new BasicStroke(1.0f));
                    }
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                    // Active dot indicator
                    if (active) {
                        g2.setColor(theme);
                        g2.fillOval(5, getHeight() / 2 - 3, 6, 6);
                    }

                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(new Font("Consolas", Font.BOLD, 10));
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            btn.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 8));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                currentModuleIdx = idx;
                updateModuleButtons();
                playTone(520, 20);
                loadQuestion(currentModuleIdx, currentMode);
            });
            modButtons[i] = btn;
            modRow.add(btn);
        }
        updateModuleButtons();
        ribbon.add(modRow, BorderLayout.WEST);

        // Mode Switcher Pills (Fill Missing vs Solve Whole)
        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        modeRow.setOpaque(false);

        JLabel lblModeBadge = new JLabel("MODE:");
        lblModeBadge.setFont(new Font("Consolas", Font.BOLD, 10));
        lblModeBadge.setForeground(RetroTheme.TEXT_DIM);
        modeRow.add(lblModeBadge);

        btnModeFill = RetroTheme.createRetroButton("FILL MISSING", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnModeFill.setFont(new Font("Consolas", Font.BOLD, 10));
        btnModeFill.setFocusable(false);

        btnModeSolve = RetroTheme.createRetroButton("SOLVE WHOLE", RetroTheme.BTN_DARK, RetroTheme.TEXT_WHITE);
        btnModeSolve.setFont(new Font("Consolas", Font.BOLD, 10));
        btnModeSolve.setFocusable(false);

        btnModeFill.addActionListener(e -> {
            currentMode = QuizManager.Mode.FILL_MISSING;
            updateModeButtons();
            playTone(560, 20);
            loadQuestion(currentModuleIdx, currentMode);
        });

        btnModeSolve.addActionListener(e -> {
            currentMode = QuizManager.Mode.SOLVE_WHOLE;
            updateModeButtons();
            playTone(600, 20);
            loadQuestion(currentModuleIdx, currentMode);
        });

        modeRow.add(btnModeFill);
        modeRow.add(btnModeSolve);
        ribbon.add(modeRow, BorderLayout.EAST);

        return ribbon;
    }

    private JPanel createLeftWorksheetPane() {
        JPanel leftPane = new JPanel(new BorderLayout(0, 8));
        leftPane.setOpaque(false);

        // 1. Mission Stage Header Card
        missionCard = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 12, 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                Color activeC = MOD_COLORS[currentModuleIdx];
                g2.setColor(new Color(activeC.getRed(), activeC.getGreen(), activeC.getBlue(), 100));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        missionCard.setOpaque(false);
        missionCard.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        lblMascotAvatar = new JLabel();
        lblMascotAvatar.setPreferredSize(new Dimension(48, 48));
        lblMascotAvatar.setHorizontalAlignment(SwingConstants.CENTER);
        missionCard.add(lblMascotAvatar, BorderLayout.WEST);

        JPanel infoBox = new JPanel();
        infoBox.setOpaque(false);
        infoBox.setLayout(new BoxLayout(infoBox, BoxLayout.Y_AXIS));

        lblModuleTitle = new JLabel("MISSION 01: CPU SCHEDULING (FCFS)");
        lblModuleTitle.setFont(new Font("Consolas", Font.BOLD, 13));
        lblModuleTitle.setForeground(RetroTheme.ACCENT_CYAN);
        infoBox.add(lblModuleTitle);

        infoBox.add(Box.createVerticalStrut(2));

        lblGameSubtitle = new JLabel("[ SPACE INVADERS // CPU DISPATCH ARENA ]");
        lblGameSubtitle.setFont(new Font("Consolas", Font.BOLD, 11));
        lblGameSubtitle.setForeground(RetroTheme.ACCENT_YELLOW);
        infoBox.add(lblGameSubtitle);

        infoBox.add(Box.createVerticalStrut(3));

        lblInstruction = new JLabel("Alien interceptors are entering the CPU pipeline! Calculate Start, Finish, Wait, and Turnaround times.");
        lblInstruction.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblInstruction.setForeground(new Color(225, 230, 245));
        infoBox.add(lblInstruction);

        missionCard.add(infoBox, BorderLayout.CENTER);
        leftPane.add(missionCard, BorderLayout.NORTH);

        // 2. Interactive Worksheet Table Card
        JPanel tableCard = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(14, 10, 26));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(50, 38, 75));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tableCard.setOpaque(false);
        tableCard.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        tableModel = new DefaultTableModel();
        tableModel.addTableModelListener(e -> {
            if (!updatingTable && e.getType() == TableModelEvent.UPDATE) {
                updateProgressMeter();
            }
        });

        quizTable = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int col) {
                if (activeQuestion == null) return false;
                String origVal = activeQuestion.worksheetData[row][col];
                return origVal.trim().equals("?");
            }
        };

        quizTable.setFont(new Font("Consolas", Font.PLAIN, 12));
        quizTable.setRowHeight(32);
        quizTable.setBackground(RetroTheme.INPUT_BG);
        quizTable.setForeground(Color.WHITE);
        quizTable.setGridColor(new Color(45, 34, 70));
        quizTable.setShowGrid(true);
        quizTable.getTableHeader().setBackground(new Color(26, 18, 48));
        quizTable.getTableHeader().setForeground(RetroTheme.ACCENT_CYAN);
        quizTable.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 11));
        quizTable.getTableHeader().setPreferredSize(new Dimension(800, 30));

        // Custom Cell Renderer with Per-Cell Validation Coloring
        quizTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFocus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);

                boolean isEditable = (activeQuestion != null && activeQuestion.worksheetData[r][c].trim().equals("?"));
                int valState = (cellValidationState != null && r < cellValidationState.length && c < cellValidationState[r].length)
                        ? cellValidationState[r][c] : 0;

                if (valState == 1) {
                    // Correct answer: Vivid Emerald Green
                    l.setBackground(new Color(16, 56, 30));
                    l.setForeground(new Color(57, 255, 60));
                    l.setFont(new Font("Consolas", Font.BOLD, 12));
                    l.setText(val != null ? "OK " + val.toString() : "");
                    l.setToolTipText("Correct mathematical value!");
                } else if (valState == 2) {
                    // Incorrect answer: Vivid Ruby Red
                    l.setBackground(new Color(68, 16, 28));
                    l.setForeground(new Color(255, 75, 95));
                    l.setFont(new Font("Consolas", Font.BOLD, 12));
                    l.setText(val != null ? "ERR " + val.toString() : "");
                    l.setToolTipText("Incorrect calculation. Refer to formulas on the right!");
                } else if (isEditable) {
                    // Editable cell waiting for answer
                    String sVal = (val == null) ? "" : val.toString().trim();
                    if (sVal.equals("?") || sVal.isEmpty()) {
                        l.setBackground(new Color(38, 22, 60));
                        l.setForeground(RetroTheme.ACCENT_YELLOW);
                        l.setFont(new Font("Consolas", Font.BOLD, 12));
                        l.setText("[ ? ]");
                        l.setToolTipText("Double-click to type your calculated answer!");
                    } else {
                        l.setBackground(new Color(42, 28, 68));
                        l.setForeground(RetroTheme.ACCENT_CYAN);
                        l.setFont(new Font("Consolas", Font.BOLD, 12));
                        l.setText(sVal);
                        l.setToolTipText("Answer entered. Click [CHECK MY ANSWERS] to verify.");
                    }
                } else {
                    // Standard given non-editable cell
                    l.setBackground(r % 2 == 0 ? RetroTheme.ROW_EVEN : RetroTheme.ROW_ODD);
                    l.setForeground(new Color(195, 200, 225));
                    l.setFont(new Font("Consolas", Font.PLAIN, 12));
                    l.setToolTipText("Given problem parameter.");
                }
                return l;
            }
        });

        JScrollPane scrollTable = RetroTheme.createRetroScroll(quizTable);
        scrollTable.setPreferredSize(new Dimension(800, 160));
        scrollTable.setBorder(BorderFactory.createLineBorder(new Color(55, 42, 85), 1));
        tableCard.add(scrollTable, BorderLayout.NORTH);

        // 3. Dynamic Visual Algorithm Telemetry Schematic Panel (Gantt, Memory Map, Disk Track)
        schematicPanel = new MiniAlgorithmSchematicPanel();
        schematicPanel.setPreferredSize(new Dimension(800, 95));
        tableCard.add(schematicPanel, BorderLayout.CENTER);

        // 4. Live Progress & Status Meter
        JPanel progressBox = new JPanel(new BorderLayout(8, 0));
        progressBox.setOpaque(false);
        progressBox.setBorder(BorderFactory.createEmptyBorder(4, 2, 2, 2));

        lblProgressText = new JLabel("PROGRESS: 0/7 SOLVED (0%)   ");
        lblProgressText.setFont(new Font("Consolas", Font.BOLD, 10));
        lblProgressText.setForeground(RetroTheme.TEXT_DIM);
        progressBox.add(lblProgressText, BorderLayout.WEST);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(180, 10));
        progressBar.setBackground(new Color(25, 18, 45));
        progressBar.setForeground(RetroTheme.ACCENT_CYAN);
        progressBar.setBorder(BorderFactory.createLineBorder(new Color(55, 40, 85), 1));
        progressBox.add(progressBar, BorderLayout.CENTER);

        lblStatusBadge = new JLabel("  [ IN PROGRESS ]");
        lblStatusBadge.setFont(new Font("Consolas", Font.BOLD, 10));
        lblStatusBadge.setForeground(RetroTheme.ACCENT_YELLOW);
        progressBox.add(lblStatusBadge, BorderLayout.EAST);

        tableCard.add(progressBox, BorderLayout.SOUTH);
        leftPane.add(tableCard, BorderLayout.CENTER);

        // 5. Tactile 3D Arcade Control Buttons Deck
        leftPane.add(createArcadeControlsDeck(), BorderLayout.SOUTH);

        return leftPane;
    }

    private JPanel createArcadeControlsDeck() {
        JPanel deck = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        deck.setOpaque(false);

        // Button 1: Check Answers (3D Arcade Green)
        JButton btnCheck = CPUSchedulingPanel.createArcadeButton("CHECK MY ANSWERS", new Color(0, 150, 65), Color.WHITE, 36);
        btnCheck.setFont(new Font("Consolas", Font.BOLD, 11));
        btnCheck.addActionListener(e -> checkAnswers());
        deck.add(btnCheck);

        // Button 2: Reveal Step-by-Step (3D Arcade Cyan/Blue)
        JButton btnReveal = CPUSchedulingPanel.createArcadeButton("REVEAL STEP-BY-STEP", new Color(0, 120, 160), Color.WHITE, 36);
        btnReveal.setFont(new Font("Consolas", Font.BOLD, 11));
        btnReveal.addActionListener(e -> revealAnswers());
        deck.add(btnReveal);

        // Button 3: Reset Worksheet (3D Arcade Ruby)
        JButton btnReset = CPUSchedulingPanel.createArcadeButton("RESET WORKSHEET", new Color(145, 25, 65), Color.WHITE, 36);
        btnReset.setFont(new Font("Consolas", Font.BOLD, 11));
        btnReset.addActionListener(e -> {
            playTone(480, 20);
            loadQuestion(currentModuleIdx, currentMode);
        });
        deck.add(btnReset);

        // Button 4: Play Minigame Directly! (3D Arcade Gold/Orange)
        JButton btnPlayGame = CPUSchedulingPanel.createArcadeButton("PLAY MINIGAME", new Color(180, 105, 10), Color.WHITE, 36);
        btnPlayGame.setFont(new Font("Consolas", Font.BOLD, 11));
        btnPlayGame.setToolTipText("Launch the playable arcade minigame corresponding to this algorithm!");
        btnPlayGame.addActionListener(e -> launchActiveMinigame());
        deck.add(btnPlayGame);

        return deck;
    }

    private JPanel createRightTelemetryPane() {
        rightPaneContainer = new JPanel(new BorderLayout(0, 8));
        rightPaneContainer.setOpaque(false);

        // 1. Formula & Cheat Sheet Card
        formulaPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 12, 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                Color activeC = MOD_COLORS[currentModuleIdx];
                g2.setColor(new Color(activeC.getRed(), activeC.getGreen(), activeC.getBlue(), 90));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        formulaPanel.setOpaque(false);
        formulaPanel.setLayout(new BoxLayout(formulaPanel, BoxLayout.Y_AXIS));
        formulaPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        rightPaneContainer.add(formulaPanel, BorderLayout.NORTH);

        // 2. Algorithm Telemetry & Solution Log Terminal
        JPanel terminalCard = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(10, 6, 20));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(45, 32, 70));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        terminalCard.setOpaque(false);
        terminalCard.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // Terminal Header
        JPanel termHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        termHeader.setOpaque(false);

        JLabel dots = new JLabel("---");
        dots.setFont(new Font("Consolas", Font.BOLD, 10));
        dots.setForeground(RetroTheme.ACCENT_PINK);

        JLabel termTitle = new JLabel("TELEMETRY & STEP LOG");
        termTitle.setFont(new Font("Consolas", Font.BOLD, 10));
        termTitle.setForeground(RetroTheme.ACCENT_CYAN);

        termHeader.add(dots);
        termHeader.add(termTitle);
        terminalCard.add(termHeader, BorderLayout.NORTH);

        txtTerminalLog = new JTextArea();
        txtTerminalLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtTerminalLog.setBackground(new Color(8, 5, 16));
        txtTerminalLog.setForeground(new Color(210, 220, 245));
        txtTerminalLog.setEditable(false);
        txtTerminalLog.setLineWrap(true);
        txtTerminalLog.setWrapStyleWord(true);
        txtTerminalLog.setMargin(new Insets(6, 8, 6, 8));

        JScrollPane scrollLog = RetroTheme.createRetroScroll(txtTerminalLog);
        scrollLog.setBorder(BorderFactory.createLineBorder(new Color(40, 28, 65), 1));
        terminalCard.add(scrollLog, BorderLayout.CENTER);

        rightPaneContainer.add(terminalCard, BorderLayout.CENTER);
        return rightPaneContainer;
    }

    private JPanel createBottomStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);

        lblFeedback = new JLabel("STATUS: Worksheet loaded. Double-click any glowing [ ? ] cell to enter your answer.");
        lblFeedback.setFont(new Font("Consolas", Font.BOLD, 11));
        lblFeedback.setForeground(RetroTheme.TEXT_DIM);
        bar.add(lblFeedback, BorderLayout.WEST);

        JLabel tipHint = new JLabel("HOTKEY: Use Tab / Shift+Tab to jump between cells | Press Enter to commit answer");
        tipHint.setFont(new Font("Consolas", Font.PLAIN, 10));
        tipHint.setForeground(new Color(130, 130, 160));
        bar.add(tipHint, BorderLayout.EAST);

        return bar;
    }

    private void updateFormulaChips(String formulaTip) {
        formulaPanel.removeAll();

        // Header label
        JLabel header = new JLabel("ALGORITHM FORMULA CHIPS");
        header.setFont(new Font("Consolas", Font.BOLD, 11));
        header.setForeground(MOD_COLORS[currentModuleIdx]);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        formulaPanel.add(header);
        formulaPanel.add(Box.createVerticalStrut(6));

        // Strip "FORMULAS: " or "RULE: " prefix if present
        String clean = formulaTip;
        if (clean.startsWith("FORMULAS: ")) clean = clean.substring(10);
        else if (clean.startsWith("FORMULA: ")) clean = clean.substring(9);
        else if (clean.startsWith("RULE: ")) clean = clean.substring(6);

        String[] parts = clean.split("\\|");
        for (String part : parts) {
            String rule = part.trim();
            if (rule.isEmpty()) continue;

            JPanel chip = new JPanel(new BorderLayout(6, 0));
            chip.setOpaque(true);
            chip.setBackground(new Color(24, 18, 44));
            chip.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(55, 42, 85), 1),
                    BorderFactory.createEmptyBorder(3, 8, 3, 8)
            ));
            chip.setAlignmentX(Component.LEFT_ALIGNMENT);
            chip.setMaximumSize(new Dimension(800, 26));

            JLabel icon = new JLabel(">");
            icon.setFont(new Font("Consolas", Font.BOLD, 11));
            icon.setForeground(RetroTheme.ACCENT_CYAN);
            chip.add(icon, BorderLayout.WEST);

            JLabel text = new JLabel(rule);
            text.setFont(new Font("Consolas", Font.PLAIN, 10));
            text.setForeground(new Color(230, 235, 255));
            chip.add(text, BorderLayout.CENTER);

            formulaPanel.add(chip);
            formulaPanel.add(Box.createVerticalStrut(4));
        }

        formulaPanel.revalidate();
        formulaPanel.repaint();
    }

    private void updateProgressMeter() {
        if (activeQuestion == null) return;
        int rows = tableModel.getRowCount();
        int cols = tableModel.getColumnCount();
        int totalBlanks = 0;
        int filled = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (activeQuestion.worksheetData[r][c].trim().equals("?")) {
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
        progressBar.setValue(pct);
        lblProgressText.setText("PROGRESS: " + filled + "/" + totalBlanks + " SOLVED (" + pct + "%)   ");

        if (filled == totalBlanks && totalBlanks > 0) {
            lblStatusBadge.setText("  [ READY TO SUBMIT ]");
            lblStatusBadge.setForeground(RetroTheme.ACCENT_GREEN);
        } else {
            lblStatusBadge.setText("  [ IN PROGRESS ]");
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

    private void updateModuleButtons() {
        for (int i = 0; i < 8; i++) {
            boolean active = (i == currentModuleIdx);
            Color themeColor = MOD_COLORS[i];
            modButtons[i].setForeground(active ? themeColor : RetroTheme.TEXT_WHITE);
        }
        repaint();
    }

    private void updateModeButtons() {
        boolean isFill = (currentMode == QuizManager.Mode.FILL_MISSING);
        btnModeFill.setForeground(isFill ? RetroTheme.ACCENT_CYAN : RetroTheme.TEXT_WHITE);
        btnModeFill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isFill ? RetroTheme.ACCENT_CYAN : RetroTheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        btnModeSolve.setForeground(!isFill ? RetroTheme.ACCENT_CYAN : RetroTheme.TEXT_WHITE);
        btnModeSolve.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(!isFill ? RetroTheme.ACCENT_CYAN : RetroTheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        repaint();
    }

    private void loadQuestion(int modIdx, QuizManager.Mode mode) {
        updatingTable = true;
        activeQuestion = QuizManager.getQuestionByModule(modIdx, mode);

        // Update Mascot Icon & Colors
        BufferedImage mascotImg = getMascotSprite(modIdx, 44);
        if (mascotImg != null) {
            lblMascotAvatar.setIcon(new ImageIcon(mascotImg));
        }

        Color themeColor = MOD_COLORS[modIdx];
        lblModuleTitle.setText(activeQuestion.moduleName);
        lblModuleTitle.setForeground(themeColor);

        lblGameSubtitle.setText("[ " + activeQuestion.arcadeGameTitle + " ]");
        lblGameSubtitle.setForeground(RetroTheme.ACCENT_YELLOW);

        lblInstruction.setText(activeQuestion.instruction);

        // Update Formula chips
        updateFormulaChips(activeQuestion.formulaTip);

        // Initial Telemetry Guidance Log
        txtTerminalLog.setText("=== MISSION TELEMETRY SYSTEM ===\n"
                + "> TARGET SYSTEM: " + activeQuestion.arcadeGameTitle + "\n"
                + "> DIRECTIVE: " + activeQuestion.instruction + "\n\n"
                + "=== HOW TO COMPLETE THIS WORKSHEET ===\n"
                + "1. Locate cells marked with glowing [ ? ].\n"
                + "2. Double-click the cell, enter your computed number, and press Enter.\n"
                + "3. Refer to the Formula Chips above for exact mathematical equations.\n"
                + "4. Click [ CHECK MY ANSWERS ] to submit for real-time grading.\n"
                + "5. If stuck, click [ REVEAL STEP-BY-STEP ] to view full telemetry calculations.");
        txtTerminalLog.setCaretPosition(0);

        // Reset per-cell validation state
        int rows = activeQuestion.worksheetData.length;
        int cols = activeQuestion.worksheetData[0].length;
        cellValidationState = new int[rows][cols];

        // Populate Table Model
        tableModel.setDataVector(cloneGrid(activeQuestion.worksheetData), activeQuestion.columnHeaders);
        updatingTable = false;

        updateProgressMeter();

        lblFeedback.setText("STATUS: Worksheet loaded. Double-click any glowing [ ? ] cell to enter your answer.");
        lblFeedback.setForeground(RetroTheme.TEXT_DIM);

        if (schematicPanel != null) {
            schematicPanel.repaint();
        }

        revalidate();
        repaint();
    }

    private String[][] cloneGrid(String[][] orig) {
        String[][] copy = new String[orig.length][orig[0].length];
        for (int r = 0; r < orig.length; r++) {
            System.arraycopy(orig[r], 0, copy[r], 0, orig[r].length);
        }
        return copy;
    }

    private void checkAnswers() {
        if (quizTable.isEditing()) {
            quizTable.getCellEditor().stopCellEditing();
        }

        int rows = tableModel.getRowCount();
        int cols = tableModel.getColumnCount();
        String[][] userGrid = new String[rows][cols];

        boolean allCorrect = true;
        int emptyCount = 0;
        int wrongCount = 0;
        StringBuilder log = new StringBuilder("=== WORKSHEET EVALUATION REPORT ===\n");
        log.append("Target: ").append(activeQuestion.arcadeGameTitle).append("\n\n");

        for (int r = 0; r < rows; r++) {
            boolean rowOk = true;
            String rowName = activeQuestion.worksheetData[r][0];

            for (int c = 0; c < cols; c++) {
                Object val = tableModel.getValueAt(r, c);
                String strVal = (val == null) ? "" : val.toString().trim();
                if (strVal.startsWith("OK ") || strVal.startsWith("ERR ")) {
                    strVal = strVal.substring(3).trim();
                } else if (strVal.startsWith("✔ ") || strVal.startsWith("✘ ")) {
                    strVal = strVal.substring(2).trim();
                }
                userGrid[r][c] = strVal;

                boolean isEditable = activeQuestion.worksheetData[r][c].trim().equals("?");
                if (isEditable) {
                    if (strVal.isEmpty() || strVal.equals("?") || strVal.equals("[ ? ]")) {
                        emptyCount++;
                        cellValidationState[r][c] = 2; // Error
                        allCorrect = false;
                        rowOk = false;
                    } else if (QuizManager.isCellCorrect(strVal, activeQuestion.answerKey[r][c])) {
                        cellValidationState[r][c] = 1; // Correct
                    } else {
                        cellValidationState[r][c] = 2; // Incorrect
                        allCorrect = false;
                        wrongCount++;
                        rowOk = false;
                    }
                } else {
                    cellValidationState[r][c] = 0;
                }
            }

            if (rowOk) {
                log.append("[OK]  Row ").append(r + 1).append(" (").append(rowName).append("): Parameters verified!\n");
            } else {
                log.append("[ERR] Row ").append(r + 1).append(" (").append(rowName).append("): Calculation mismatch.\n");
            }
        }

        if (allCorrect) {
            streakCount++;
            lblStreak.setText("STREAK: " + streakCount + " WINS");
            playVictorySound();
            AppState.addScore(150);
            lblXpScore.setText("XP: " + AppState.getScoreFormatted() + "   ");
            lblFeedback.setText("★ EXCELLENT WORK! All calculations verified mathematically! +150 XP AWARDED! (STREAK: " + streakCount + ") ★");
            lblFeedback.setForeground(RetroTheme.ACCENT_GREEN);

            log.append("\n===================================\n");
            log.append("★ SCORE: 100% ACCURACY // S-RANK CLEAR ★\n");
            log.append("Reward: +150 XP added to Profile.\n");
            log.append("Consecutive Winning Streak: ").append(streakCount).append(" ★\n");

            txtTerminalLog.setText(log.toString());
            txtTerminalLog.setCaretPosition(0);

            JOptionPane.showMessageDialog(this,
                    "★ EXCELLENT WORK! ★\n\n" +
                            "All worksheet calculations for [" + activeQuestion.arcadeGameTitle + "] match 100%!\n" +
                            "Reward: +150 XP awarded to your Profile.\n" +
                            "Current Winning Streak: " + streakCount + " ★",
                    "Mission Cleared", JOptionPane.INFORMATION_MESSAGE);
        } else {
            streakCount = 0;
            lblStreak.setText("STREAK: 0 WINS");
            playBuzzerSound();

            log.append("\n===================================\n");
            if (emptyCount > 0) {
                log.append("[!] STATUS: ").append(emptyCount).append(" cell(s) left blank.\n");
                lblFeedback.setText("INCOMPLETE: " + emptyCount + " cell(s) are blank. Fill in all glowing [ ? ] cells and re-check.");
            } else {
                log.append("[X] STATUS: ").append(wrongCount).append(" calculation error(s) detected.\n");
                lblFeedback.setText("SOME VALUES ARE INCORRECT. Check the highlighted red cells and formula chips on the right.");
            }
            log.append("Study the Formula Chips above and retry, or click [REVEAL STEP-BY-STEP].");
            lblFeedback.setForeground(RetroTheme.ACCENT_PINK);

            txtTerminalLog.setText(log.toString());
            txtTerminalLog.setCaretPosition(0);
        }

        repaint();
    }

    private void revealAnswers() {
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

        txtTerminalLog.setText("★ " + activeQuestion.arcadeGameTitle + " // STEP-BY-STEP SOLUTION BREAKDOWN ★\n\n"
                + activeQuestion.explanation + "\n\n"
                + "> TELEMETRY NOTE: All correct answers have been synchronized into the worksheet grid above.");
        txtTerminalLog.setCaretPosition(0);

        lblFeedback.setText("ANSWER KEY & BREAKDOWN REVEALED. Study the step-by-step working on the right.");
        lblFeedback.setForeground(RetroTheme.ACCENT_YELLOW);

        revalidate();
        repaint();
    }

    /**
     * Directly launches the corresponding playable arcade minigame for the current module!
     */
    private void launchActiveMinigame() {
        playTone(700, 30);
        try {
            switch (currentModuleIdx) {
                case 0: // CPU Invaders -> Kernel Tapper Arcade
                    new KernelTapperDialog(parentFrame, "FCFS", null).setVisible(true);
                    break;
                case 1: // Space Shoot -> Vault Panic Banker's Arcade
                    new VaultPanicDialog(parentFrame, null, null, null).setVisible(true);
                    break;
                case 2: // Frame Hopper -> MMU Paging Crossing Arcade
                    new FrameHopperDialog(parentFrame, 4096, null, 8).setVisible(true);
                    break;
                case 3: // Super Mario -> Mario Cache Runner Arcade
                    new MarioCacheRunnerDialog(parentFrame, null, 3, "FIFO").setVisible(true);
                    break;
                case 4: // Cyber Snake -> Heap vs Stack Segmentation Arcade
                    game.snake.SnakeGameDialog.launchDefault(parentFrame, null);
                    break;
                case 5: // Cyber Crane -> Memory Allocation Defrag Panic Arcade
                    new DefragPanicDialog(parentFrame, null, null, null).setVisible(true);
                    break;
                case 6: // Pac-Man Disk -> Magnetic Platter Seek Arcade
                    new PacmanDiskArcadeDialog(parentFrame, null, 53, 199, "FCFS").setVisible(true);
                    break;
                case 7: // Overcooked Kitchen -> Concurrent Multiprogramming Arcade
                    new OvercookedKitchenDialog(parentFrame, null).setVisible(true);
                    break;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not launch minigame: " + ex.getMessage(),
                    "Arcade Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Visual Algorithm Schematic Panel:
     * Dynamically renders custom Gantt Charts, Safe Sequences, Page Maps, or Disk Seek lines
     * based on the active module question!
     */
    private class MiniAlgorithmSchematicPanel extends JPanel {
        public MiniAlgorithmSchematicPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background card
            g2.setColor(new Color(11, 7, 22));
            g2.fillRoundRect(0, 0, w, h, 8, 8);
            g2.setColor(new Color(42, 30, 68));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

            // Title Tag
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(MOD_COLORS[currentModuleIdx]);
            g2.drawString("ALGORITHM EXECUTION PIPELINE // LIVE SCHEMATIC", 10, 15);

            int baseY = 25;
            int boxH = 45;
            int availW = w - 24;

            switch (currentModuleIdx) {
                case 0: // CPU FCFS Gantt Chart
                    drawGanttChart(g2, 12, baseY, availW, boxH);
                    break;
                case 1: // Banker's Safe Sequence
                    drawBankersPipeline(g2, 12, baseY, availW, boxH);
                    break;
                case 2: // Paging MMU Map
                    drawPagingPipeline(g2, 12, baseY, availW, boxH);
                    break;
                case 3: // Mario Cache Frame Slots
                    drawCachePipeline(g2, 12, baseY, availW, boxH);
                    break;
                case 4: // Segmentation Memory Map
                    drawSegmentationPipeline(g2, 12, baseY, availW, boxH);
                    break;
                case 5: // Memory Allocation Holes
                    drawAllocationPipeline(g2, 12, baseY, availW, boxH);
                    break;
                case 6: // Pac-Man Disk Seek Trajectory
                    drawDiskSeekPipeline(g2, 12, baseY, availW, boxH);
                    break;
                case 7: // Overcooked Concurrency Meter
                    drawConcurrencyPipeline(g2, 12, baseY, availW, boxH);
                    break;
            }

            g2.dispose();
        }

        private void drawGanttChart(Graphics2D g2, int x, int y, int w, int h) {
            // W1 (4ms), W2 (3ms), W3 (2ms) -> Total 9ms
            int w1 = (int) (w * (4.0 / 9.0));
            int w2 = (int) (w * (3.0 / 9.0));
            int w3 = w - w1 - w2;

            // Block W1
            g2.setColor(new Color(5, 180, 200));
            g2.fillRect(x, y, w1, h - 15);
            g2.setColor(new Color(10, 6, 20));
            g2.drawRect(x, y, w1, h - 15);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.drawString("W1: SCOUT (4ms)", x + 10, y + 20);

            // Block W2
            g2.setColor(new Color(255, 180, 25));
            g2.fillRect(x + w1, y, w2, h - 15);
            g2.setColor(new Color(10, 6, 20));
            g2.drawRect(x + w1, y, w2, h - 15);
            g2.setColor(new Color(15, 10, 25));
            g2.drawString("W2: FIGHTER (3ms)", x + w1 + 10, y + 20);

            // Block W3
            g2.setColor(new Color(255, 60, 140));
            g2.fillRect(x + w1 + w2, y, w3, h - 15);
            g2.setColor(new Color(10, 6, 20));
            g2.drawRect(x + w1 + w2, y, w3, h - 15);
            g2.setColor(Color.WHITE);
            g2.drawString("W3: MOTHER (2ms)", x + w1 + w2 + 8, y + 20);

            // Timeline markers
            g2.setColor(new Color(180, 180, 210));
            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            g2.drawString("0.0ms", x, y + h);
            g2.drawString("4.0ms", x + w1 - 10, y + h);
            g2.drawString("7.0ms", x + w1 + w2 - 10, y + h);
            g2.drawString("9.0ms", x + w - 24, y + h);
        }

        private void drawBankersPipeline(Graphics2D g2, int x, int y, int w, int h) {
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(new Color(25, 18, 48));
            g2.fillRoundRect(x, y, w, h - 10, 6, 6);
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawRoundRect(x, y, w, h - 10, 6, 6);

            g2.drawString("AVAILABLE: [ 3, 3, 2 ]", x + 12, y + 16);
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString("SAFE EXECUTION SEQUENCE:  [ S2 ] ──> [ S1 ] ──> [ S3 ]", x + 12, y + 30);
        }

        private void drawPagingPipeline(Graphics2D g2, int x, int y, int w, int h) {
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(new Color(20, 26, 45));
            g2.fillRoundRect(x, y, w, h - 10, 6, 6);
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawRoundRect(x, y, w, h - 10, 6, 6);

            g2.drawString("LOGICAL ADDR 5120  ──>  PAGE # 1 (Offset 1024)  ──>  FRAME # 2", x + 12, y + 16);
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString("PHYSICAL ADDRESS = (2 * 4096) + 1024 = 9216 BYTES", x + 12, y + 30);
        }

        private void drawCachePipeline(Graphics2D g2, int x, int y, int w, int h) {
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(new Color(36, 18, 30));
            g2.fillRoundRect(x, y, w, h - 10, 6, 6);
            g2.setColor(new Color(255, 115, 115));
            g2.drawRoundRect(x, y, w, h - 10, 6, 6);

            g2.drawString("PAGE FRAME SLOTS (FIFO): [ SLOT 0: P0 ]  [ SLOT 1: P3 ]  [ SLOT 2: P1 ]", x + 12, y + 16);
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("PAGE FAULTS: 15 / 20 REFERENCES  |  HIT RATIO: 25.0% (5 HITS)", x + 12, y + 30);
        }

        private void drawSegmentationPipeline(Graphics2D g2, int x, int y, int w, int h) {
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(new Color(18, 34, 25));
            g2.fillRoundRect(x, y, w, h - 10, 6, 6);
            g2.setColor(new Color(90, 245, 140));
            g2.drawRoundRect(x, y, w, h - 10, 6, 6);

            g2.drawString("SEGMENTS: [0: CODE Base 1000] [1: HEAP Base 2500] [2: STACK Base 4000]", x + 12, y + 16);
            g2.setColor(Color.WHITE);
            g2.drawString("OFFSET CHECK: Seg 0 (150 < 400) -> VALID (1150) | Seg 1 (600 >= 500) -> TRAP!", x + 12, y + 30);
        }

        private void drawAllocationPipeline(Graphics2D g2, int x, int y, int w, int h) {
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(new Color(30, 20, 48));
            g2.fillRoundRect(x, y, w, h - 10, 6, 6);
            g2.setColor(new Color(215, 145, 255));
            g2.drawRoundRect(x, y, w, h - 10, 6, 6);

            g2.drawString("MEMORY HOLES: [ Hole 0: 100K ]  [ Hole 1: 500K ]  [ Hole 2: 200K ]", x + 12, y + 16);
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("FIRST FIT: P1(212K) -> Hole 1 (Frag 288K) | P2(417K) -> NO FIT (0K Frag)", x + 12, y + 30);
        }

        private void drawDiskSeekPipeline(Graphics2D g2, int x, int y, int w, int h) {
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(new Color(34, 30, 16));
            g2.fillRoundRect(x, y, w, h - 10, 6, 6);
            g2.setColor(new Color(255, 230, 50));
            g2.drawRoundRect(x, y, w, h - 10, 6, 6);

            g2.drawString("CYLINDER SEEK TRAJECTORY: 53 -> 98 -> 183 -> 37 -> 122 -> 14", x + 12, y + 16);
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString("TOTAL HEAD TRAVEL: 45 + 85 + 146 + 85 + 108 = 469 CYLINDERS", x + 12, y + 30);
        }

        private void drawConcurrencyPipeline(Graphics2D g2, int x, int y, int w, int h) {
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(new Color(36, 22, 16));
            g2.fillRoundRect(x, y, w, h - 10, 6, 6);
            g2.setColor(new Color(255, 150, 50));
            g2.drawRoundRect(x, y, w, h - 10, 6, 6);

            g2.drawString("STATIONS CONCURRENCY: [ J1: Ramen ] [ J2: Steak ] [ J3: Pizza ] (p = 0.50)", x + 12, y + 16);
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("ACTIVE CPU UTILIZATION: (1 - 0.50^N) * 100% = 87.5% TO 96.9%", x + 12, y + 30);
        }
    }

    // ── Synthetic Audio FX ──────────────────────────────────────────────────
    private void playTone(int freq, int durationMs) {
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

    private void playVictorySound() {
        new Thread(() -> {
            playTone(523, 70); // C5
            try { Thread.sleep(80); } catch (Exception ignored) {}
            playTone(659, 70); // E5
            try { Thread.sleep(80); } catch (Exception ignored) {}
            playTone(784, 70); // G5
            try { Thread.sleep(80); } catch (Exception ignored) {}
            playTone(1046, 150); // C6
        }).start();
    }

    private void playBuzzerSound() {
        new Thread(() -> {
            playTone(180, 100);
            try { Thread.sleep(60); } catch (Exception ignored) {}
            playTone(140, 150);
        }).start();
    }
}
