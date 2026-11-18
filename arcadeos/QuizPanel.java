package arcadeos;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Screen: QUIZ MODE // ALGORITHM MASTERY ARENA
 * -----------------------------------------------------------------------------
 * Fully gamified, 16-bit arcade-themed interactive worksheet arena.
 * Themed around all 8 Arcade OS Minigames:
 * 01 CPU Invaders, 02 Space Shoot, 03 Frame Hopper, 04 Super Mario,
 * 05 Cyber Snake, 06 Cyber Crane, 07 Pac-Man Disk, 08 Overcooked Kitchen.
 */
public class QuizPanel extends JPanel {
    private final ArcadeOS parentFrame;
    private QuizManager.Mode currentMode = QuizManager.Mode.FILL_MISSING;
    private int currentModuleIdx = 0; // 0 to 7
    private QuizManager.QuizQuestion activeQuestion;

    // Module Cartridge Buttons (8 Arcade Games)
    private final JButton[] modButtons = new JButton[8];
    private static final String[] MOD_TITLES = {
            "👾 01 INVADERS", "🚀 02 SPACE", "🐸 03 HOPPER", "🍄 04 MARIO",
            "🐍 05 SNAKE", "🏗️ 06 CRANE", "🟡 07 PAC-MAN", "👨‍🍳 08 OVERCOOKED"
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

    // Worksheet UI Elements
    private JLabel lblMascotAvatar;
    private JLabel lblModuleTitle;
    private JLabel lblGameSubtitle;
    private JLabel lblInstruction;
    private JLabel lblTipFormula;
    private JLabel lblFeedback;
    private JLabel lblXpScore;
    private JLabel lblStreak;
    private JTable quizTable;
    private DefaultTableModel tableModel;
    private JTextArea txtExplanation;
    private JScrollPane scrollExplanation;
    private JPanel briefingCard;

    // Per-cell validation status: 0=normal, 1=correct, 2=incorrect
    private int[][] cellValidationState;
    private boolean soundEnabled = true;
    private int streakCount = 0;

    public QuizPanel(ArcadeOS parent) {
        this.parentFrame = parent;
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 16, 12, 16));

        // 1. Top Retro Header Bar
        add(createTopBar(), BorderLayout.NORTH);

        // 2. Center Content Card
        add(createCenterWorksheetCard(), BorderLayout.CENTER);

        // 3. Bottom Controls & Feedback Bar
        add(createBottomControls(), BorderLayout.SOUTH);

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

        JLabel star = new JLabel("✦");
        star.setFont(new Font("Consolas", Font.BOLD, 14));
        star.setForeground(RetroTheme.ACCENT_PINK);

        JLabel title = new JLabel("ARCADE OS // ALGORITHM MASTERY WORKSHEET ARENA");
        title.setFont(new Font("Consolas", Font.BOLD, 13));
        title.setForeground(Color.WHITE);

        titleBox.add(star);
        titleBox.add(title);
        bar.add(titleBox, BorderLayout.CENTER);

        // Right: Streak, XP Score & Sound Toggle
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        lblStreak = new JLabel("STREAK: 0 ★");
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

    private JPanel createCenterWorksheetCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Card background
                g2.setColor(new Color(18, 13, 34));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // Dynamic border glow matching selected arcade game
                Color activeC = MOD_COLORS[currentModuleIdx];
                g2.setColor(new Color(activeC.getRed(), activeC.getGreen(), activeC.getBlue(), 120));
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        // Top Section: 8 Cartridges + Mode Selectors
        JPanel topSection = new JPanel(new BorderLayout(0, 6));
        topSection.setOpaque(false);

        // 1. Module Selector Row (8 Themed Cartridges)
        JPanel modRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        modRow.setOpaque(false);

        JLabel lblSelect = new JLabel("MISSION:");
        lblSelect.setFont(new Font("Consolas", Font.BOLD, 11));
        lblSelect.setForeground(RetroTheme.TEXT_DIM);
        modRow.add(lblSelect);

        for (int i = 0; i < 8; i++) {
            final int idx = i;
            JButton btn = new JButton(MOD_TITLES[i]);
            btn.setFont(new Font("Consolas", Font.BOLD, 10));
            btn.setFocusPainted(false);
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

        // 2. Mode Toggle Row (Fill Missing vs Solve Whole)
        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        modeRow.setOpaque(false);

        JLabel lblModeBadge = new JLabel("MODE:");
        lblModeBadge.setFont(new Font("Consolas", Font.BOLD, 11));
        lblModeBadge.setForeground(RetroTheme.TEXT_DIM);
        modeRow.add(lblModeBadge);

        btnModeFill = RetroTheme.createRetroButton("✍ FILL MISSING", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnModeFill.setFont(new Font("Consolas", Font.BOLD, 10));
        btnModeFill.setFocusable(false);

        btnModeSolve = RetroTheme.createRetroButton("🧩 SOLVE WHOLE", RetroTheme.BTN_DARK, RetroTheme.TEXT_WHITE);
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

        JPanel headerControls = new JPanel(new BorderLayout());
        headerControls.setOpaque(false);
        headerControls.add(modRow, BorderLayout.WEST);
        headerControls.add(modeRow, BorderLayout.EAST);
        topSection.add(headerControls, BorderLayout.NORTH);

        // 3. Immersive Game Briefing & Formula Card
        briefingCard = new JPanel(new BorderLayout(12, 0));
        briefingCard.setOpaque(true);
        briefingCard.setBackground(new Color(14, 10, 26));
        briefingCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(55, 40, 85)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        // Mascot Icon (Left)
        lblMascotAvatar = new JLabel();
        lblMascotAvatar.setPreferredSize(new Dimension(36, 36));
        lblMascotAvatar.setHorizontalAlignment(SwingConstants.CENTER);
        briefingCard.add(lblMascotAvatar, BorderLayout.WEST);

        // Center: Title + Game Subtitle + Mission Instructions
        JPanel centerInfo = new JPanel();
        centerInfo.setOpaque(false);
        centerInfo.setLayout(new BoxLayout(centerInfo, BoxLayout.Y_AXIS));

        lblModuleTitle = new JLabel("MISSION 01: CPU SCHEDULING (FCFS)");
        lblModuleTitle.setFont(new Font("Consolas", Font.BOLD, 12));
        lblModuleTitle.setForeground(RetroTheme.ACCENT_CYAN);
        centerInfo.add(lblModuleTitle);

        centerInfo.add(Box.createVerticalStrut(1));

        lblGameSubtitle = new JLabel("ARCADE THEME: SPACE INVADERS // CPU DISPATCH ARENA");
        lblGameSubtitle.setFont(new Font("Consolas", Font.BOLD, 10));
        lblGameSubtitle.setForeground(RetroTheme.ACCENT_YELLOW);
        centerInfo.add(lblGameSubtitle);

        centerInfo.add(Box.createVerticalStrut(2));

        lblInstruction = new JLabel("Alien interceptors are entering the CPU pipeline! Calculate Start, Finish, Wait, and Turnaround times.");
        lblInstruction.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblInstruction.setForeground(RetroTheme.TEXT_WHITE);
        centerInfo.add(lblInstruction);

        centerInfo.add(Box.createVerticalStrut(4));

        // Formula Bar
        JPanel tipBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        tipBox.setOpaque(true);
        tipBox.setBackground(new Color(22, 16, 40));
        tipBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 42, 85), 1),
                BorderFactory.createEmptyBorder(1, 6, 1, 6)
        ));

        JLabel tipBadge = new JLabel("💡 FORMULAS & RULES:");
        tipBadge.setFont(new Font("Consolas", Font.BOLD, 9));
        tipBadge.setForeground(RetroTheme.ACCENT_CYAN);
        tipBox.add(tipBadge);

        lblTipFormula = new JLabel("ST = max(AT, prev FT) | FT = ST + BT | TAT = FT - AT | WT = TAT - BT");
        lblTipFormula.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblTipFormula.setForeground(new Color(215, 215, 240));
        tipBox.add(lblTipFormula);

        centerInfo.add(tipBox);
        briefingCard.add(centerInfo, BorderLayout.CENTER);

        topSection.add(briefingCard, BorderLayout.SOUTH);
        card.add(topSection, BorderLayout.NORTH);

        // Center: Interactive Table + Workbench Notes
        JPanel centerWorksheet = new JPanel(new BorderLayout(0, 8));
        centerWorksheet.setOpaque(false);

        tableModel = new DefaultTableModel();
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
        quizTable.getTableHeader().setBackground(new Color(24, 16, 44));
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
                    // Correct answer: Neon Green
                    l.setBackground(new Color(16, 52, 28));
                    l.setForeground(new Color(57, 255, 20));
                    l.setFont(new Font("Consolas", Font.BOLD, 12));
                    l.setText(val != null ? "✔ " + val.toString() : "");
                } else if (valState == 2) {
                    // Incorrect answer: Neon Red
                    l.setBackground(new Color(60, 16, 28));
                    l.setForeground(new Color(255, 74, 92));
                    l.setFont(new Font("Consolas", Font.BOLD, 12));
                    l.setText(val != null ? "✘ " + val.toString() : "");
                } else if (isEditable) {
                    // Editable cell waiting for answer
                    l.setBackground(new Color(36, 24, 58));
                    l.setForeground(RetroTheme.ACCENT_YELLOW);
                    l.setFont(new Font("Consolas", Font.BOLD, 12));
                } else {
                    // Standard given non-editable cell
                    l.setBackground(r % 2 == 0 ? RetroTheme.ROW_EVEN : RetroTheme.ROW_ODD);
                    l.setForeground(new Color(190, 190, 215));
                    l.setFont(new Font("Consolas", Font.PLAIN, 12));
                }
                return l;
            }
        });

        JScrollPane scrollTable = RetroTheme.createRetroScroll(quizTable);
        scrollTable.setPreferredSize(new Dimension(800, 150));
        scrollTable.setBorder(BorderFactory.createLineBorder(new Color(60, 45, 95), 1));
        centerWorksheet.add(scrollTable, BorderLayout.NORTH);

        // Lower Card: Dynamic Notes or Step-by-Step Solution Breakdown
        txtExplanation = new JTextArea();
        txtExplanation.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtExplanation.setBackground(new Color(12, 8, 22));
        txtExplanation.setForeground(new Color(200, 205, 230));
        txtExplanation.setEditable(false);
        txtExplanation.setLineWrap(true);
        txtExplanation.setWrapStyleWord(true);
        txtExplanation.setMargin(new Insets(8, 12, 8, 12));

        scrollExplanation = RetroTheme.createRetroScroll(txtExplanation);
        scrollExplanation.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 1),
                " 📖 ALGORITHM WORKBENCH // SOLUTION BREAKDOWN & NOTES ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Consolas", Font.BOLD, 10),
                RetroTheme.ACCENT_CYAN
        ));
        centerWorksheet.add(scrollExplanation, BorderLayout.CENTER);

        card.add(centerWorksheet, BorderLayout.CENTER);
        return card;
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
            modButtons[i].setBackground(active ? new Color(themeColor.getRed() / 4, themeColor.getGreen() / 4, themeColor.getBlue() / 4) : RetroTheme.BTN_DARK);
            modButtons[i].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(active ? themeColor : RetroTheme.BORDER_COLOR, active ? 2 : 1),
                    BorderFactory.createEmptyBorder(4, 7, 4, 7)
            ));
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
        activeQuestion = QuizManager.getQuestionByModule(modIdx, mode);

        // Update Mascot Icon & Colors
        BufferedImage mascotImg = getMascotSprite(modIdx, 32);
        if (mascotImg != null) {
            lblMascotAvatar.setIcon(new ImageIcon(mascotImg));
        }

        Color themeColor = MOD_COLORS[modIdx];
        lblModuleTitle.setText(activeQuestion.moduleName);
        lblModuleTitle.setForeground(themeColor);

        lblGameSubtitle.setText("[ " + activeQuestion.arcadeGameTitle + " ]");
        lblGameSubtitle.setForeground(RetroTheme.ACCENT_YELLOW);

        lblInstruction.setText(activeQuestion.instruction);
        lblTipFormula.setText(activeQuestion.formulaTip);

        // Helpful guidance in lower card
        txtExplanation.setText("• MISSION CONTEXT: " + activeQuestion.arcadeGameTitle + "\n"
                + "• OBJECTIVE: " + activeQuestion.instruction + "\n\n"
                + "• CALCULATION HINTS:\n  " + activeQuestion.formulaTip.replace(" | ", "\n  ") + "\n\n"
                + "• HOW TO PLAY & SOLVE:\n"
                + "  1. Double-click any cell marked with [ ? ] (highlighted in gold/yellow).\n"
                + "  2. Type your calculated value and press Enter.\n"
                + "  3. Click [ ✔ CHECK MY ANSWERS ] below to verify each cell.\n"
                + "     (Correct cells turn GREEN ✔; incorrect cells turn RED ✘).\n"
                + "  4. Need assistance? Click [ 👁 REVEAL STEP-BY-STEP SOLUTION ] anytime.");
        txtExplanation.setCaretPosition(0);

        // Reset per-cell validation state
        int rows = activeQuestion.worksheetData.length;
        int cols = activeQuestion.worksheetData[0].length;
        cellValidationState = new int[rows][cols];

        // Populate Table Model
        tableModel.setDataVector(cloneGrid(activeQuestion.worksheetData), activeQuestion.columnHeaders);

        lblFeedback.setText("STATUS: Worksheet loaded. Double-click any yellow cell to enter your answer.");
        lblFeedback.setForeground(RetroTheme.TEXT_DIM);

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

    private JPanel createBottomControls() {
        JPanel bar = new JPanel(new BorderLayout(15, 6));
        bar.setOpaque(false);

        // Feedback label banner
        lblFeedback = new JLabel("STATUS: Worksheet loaded. Double-click any yellow cell to enter your answer.");
        lblFeedback.setFont(new Font("Consolas", Font.BOLD, 12));
        lblFeedback.setForeground(RetroTheme.TEXT_DIM);
        bar.add(lblFeedback, BorderLayout.NORTH);

        // Action Buttons Row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);

        JButton btnCheck = RetroTheme.createRetroButton("✔  CHECK MY ANSWERS", new Color(18, 70, 34), Color.WHITE);
        btnCheck.setFont(new Font("Consolas", Font.BOLD, 12));
        btnCheck.setFocusable(false);
        btnCheck.addActionListener(e -> checkAnswers());

        JButton btnReveal = RetroTheme.createRetroButton("👁 REVEAL STEP-BY-STEP SOLUTION", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnReveal.setFont(new Font("Consolas", Font.BOLD, 11));
        btnReveal.setFocusable(false);
        btnReveal.addActionListener(e -> revealAnswers());

        JButton btnReset = RetroTheme.createRetroButton("🔄 RESET WORKSHEET", RetroTheme.BTN_DARK, RetroTheme.TEXT_WHITE);
        btnReset.setFont(new Font("Consolas", Font.BOLD, 11));
        btnReset.setFocusable(false);
        btnReset.addActionListener(e -> {
            playTone(480, 20);
            loadQuestion(currentModuleIdx, currentMode);
        });

        btnRow.add(btnCheck);
        btnRow.add(btnReveal);
        btnRow.add(btnReset);

        bar.add(btnRow, BorderLayout.CENTER);
        return bar;
    }

    private void checkAnswers() {
        // Stop any active cell edit
        if (quizTable.isEditing()) {
            quizTable.getCellEditor().stopCellEditing();
        }

        int rows = tableModel.getRowCount();
        int cols = tableModel.getColumnCount();
        String[][] userGrid = new String[rows][cols];

        boolean allCorrect = true;
        int emptyCount = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Object val = tableModel.getValueAt(r, c);
                String strVal = (val == null) ? "" : val.toString().trim();
                // Strip existing check/cross markers if re-checking
                if (strVal.startsWith("✔ ") || strVal.startsWith("✘ ")) {
                    strVal = strVal.substring(2).trim();
                }
                userGrid[r][c] = strVal;

                boolean isEditable = activeQuestion.worksheetData[r][c].trim().equals("?");
                if (isEditable) {
                    if (strVal.isEmpty() || strVal.equals("?")) {
                        emptyCount++;
                        cellValidationState[r][c] = 2; // Flag as error
                        allCorrect = false;
                    } else if (QuizManager.isCellCorrect(strVal, activeQuestion.answerKey[r][c])) {
                        cellValidationState[r][c] = 1; // Correct!
                    } else {
                        cellValidationState[r][c] = 2; // Incorrect!
                        allCorrect = false;
                    }
                } else {
                    cellValidationState[r][c] = 0; // Normal
                }
            }
        }

        if (allCorrect) {
            streakCount++;
            lblStreak.setText("STREAK: " + streakCount + " ★");
            playVictorySound();
            AppState.addScore(150);
            lblXpScore.setText("XP: " + AppState.getScoreFormatted() + "   ");
            lblFeedback.setText("★ EXCELLENT WORK! All calculations verified mathematically! +150 XP AWARDED! (STREAK: " + streakCount + ") ★");
            lblFeedback.setForeground(RetroTheme.ACCENT_GREEN);

            JOptionPane.showMessageDialog(this,
                    "★ EXCELLENT WORK! ★\n\n" +
                            "All worksheet calculations for [" + activeQuestion.arcadeGameTitle + "] match 100%!\n" +
                            "Reward: +150 XP awarded to your Profile.\n" +
                            "Current Winning Streak: " + streakCount + " ★",
                    "Mission Cleared", JOptionPane.INFORMATION_MESSAGE);
        } else {
            streakCount = 0;
            lblStreak.setText("STREAK: 0 ★");
            playBuzzerSound();
            if (emptyCount > 0) {
                lblFeedback.setText("✘ INCOMPLETE: " + emptyCount + " cell(s) are blank. Fill in all '?' cells and click Check.");
            } else {
                lblFeedback.setText("✘ SOME VALUES ARE INCORRECT. Check the highlighted red cells and formula tips.");
            }
            lblFeedback.setForeground(RetroTheme.ACCENT_PINK);
        }

        repaint();
    }

    private void revealAnswers() {
        playTone(660, 40);
        int rows = activeQuestion.answerKey.length;
        int cols = activeQuestion.answerKey[0].length;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                tableModel.setValueAt(activeQuestion.answerKey[r][c], r, c);
                boolean isEditable = activeQuestion.worksheetData[r][c].trim().equals("?");
                cellValidationState[r][c] = isEditable ? 1 : 0;
            }
        }

        txtExplanation.setText("★ " + activeQuestion.arcadeGameTitle + " // STEP-BY-STEP SOLUTION BREAKDOWN ★\n\n"
                + activeQuestion.explanation + "\n\n"
                + "• NOTE: All answers have been automatically loaded into the worksheet grid above for reference.");
        txtExplanation.setCaretPosition(0);

        lblFeedback.setText("ANSWER KEY & BREAKDOWN REVEALED. Study the step-by-step working below.");
        lblFeedback.setForeground(RetroTheme.ACCENT_YELLOW);

        revalidate();
        repaint();
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
