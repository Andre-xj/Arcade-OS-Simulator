package arcadeos;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Screen: QUIZ MODE / ALGORITHM QUIZ ARENA
 * Interactive problem-solving engine based on in-suu/MiniOs QuizManager.
 */
public class QuizPanel extends JPanel {
    private ArcadeOS parentFrame;
    private QuizManager.Mode currentMode = QuizManager.Mode.FILL_MISSING;
    private int currentModuleIdx = 0; // 0=CPU, 1=Banker, 2=Paging, 3=Cache
    private QuizManager.QuizQuestion activeQuestion;

    private JButton btnModCpu, btnModBanker, btnModPaging, btnModCache;
    private JButton btnModeFill, btnModeSolve;
    private JLabel lblModuleTitle, lblInstruction, lblTip, lblFeedback;
    private JLabel lblXpScore;
    private JTable quizTable;
    private DefaultTableModel tableModel;
    private JTextArea txtExplanation;

    public QuizPanel(ArcadeOS parent) {
        this.parentFrame = parent;
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 20, 14, 20));

        // Top Navigation Bar
        add(createTopBar(), BorderLayout.NORTH);

        // Center Content Card
        add(createCenterWorksheetCard(), BorderLayout.CENTER);

        // Bottom Controls & Feedback Bar
        add(createBottomControls(), BorderLayout.SOUTH);

        // Load default question
        loadQuestion(0, QuizManager.Mode.FILL_MISSING);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout(15, 0));
        bar.setOpaque(false);

        // Left button: Return to Main Menu
        JButton btnBack = RetroTheme.createRetroButton("< MAIN MENU", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnBack.addActionListener(e -> parentFrame.showScreen(ArcadeOS.SCREEN_MAIN_MENU));
        bar.add(btnBack, BorderLayout.WEST);

        // Center: Title
        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        titleBox.setOpaque(false);
        JLabel star = new JLabel("✦");
        star.setFont(new Font("Consolas", Font.BOLD, 14));
        star.setForeground(RetroTheme.ACCENT_PINK);
        JLabel title = new JLabel("MINI // OS: ALGORITHM QUIZ ARENA");
        title.setFont(new Font("Consolas", Font.BOLD, 15));
        title.setForeground(Color.WHITE);
        titleBox.add(star);
        titleBox.add(title);
        bar.add(titleBox, BorderLayout.CENTER);

        // Right: Score
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        JLabel coin = new JLabel("🪙");
        coin.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        right.add(coin);
        lblXpScore = new JLabel("XP SCORE: " + AppState.getScoreFormatted());
        lblXpScore.setFont(new Font("Consolas", Font.BOLD, 12));
        lblXpScore.setForeground(RetroTheme.ACCENT_YELLOW);
        right.add(lblXpScore);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    private JPanel createCenterWorksheetCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 15, 38));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(60, 40, 90));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        // 1. Module Selector Row
        JPanel modRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        modRow.setOpaque(false);
        modRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSelect = new JLabel("SELECT MODULE:");
        lblSelect.setFont(new Font("Consolas", Font.BOLD, 10));
        lblSelect.setForeground(RetroTheme.TEXT_DIM);
        modRow.add(lblSelect);

        btnModCpu = createSelectBtn("🕹️ 01 CPU SCHED", 0);
        btnModBanker = createSelectBtn("🪙 02 BANKER", 1);
        btnModPaging = createSelectBtn("💾 03 PAGING", 2);
        btnModCache = createSelectBtn("🔄 04 CACHE SWAP", 3);

        modRow.add(btnModCpu);
        modRow.add(btnModBanker);
        modRow.add(btnModPaging);
        modRow.add(btnModCache);

        modRow.add(Box.createHorizontalStrut(20));

        // Mode switch: Fill Missing vs Solve Whole
        btnModeFill = RetroTheme.createRetroButton("✍ FILL MISSING", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnModeSolve = RetroTheme.createRetroButton("🧩 SOLVE WHOLE", RetroTheme.BTN_DARK, RetroTheme.TEXT_WHITE);

        btnModeFill.addActionListener(e -> {
            currentMode = QuizManager.Mode.FILL_MISSING;
            btnModeFill.setForeground(RetroTheme.ACCENT_CYAN);
            btnModeSolve.setForeground(RetroTheme.TEXT_WHITE);
            loadQuestion(currentModuleIdx, currentMode);
        });

        btnModeSolve.addActionListener(e -> {
            currentMode = QuizManager.Mode.SOLVE_WHOLE;
            btnModeSolve.setForeground(RetroTheme.ACCENT_CYAN);
            btnModeFill.setForeground(RetroTheme.TEXT_WHITE);
            loadQuestion(currentModuleIdx, currentMode);
        });

        modRow.add(btnModeFill);
        modRow.add(btnModeSolve);
        card.add(modRow);

        card.add(Box.createVerticalStrut(12));

        // 2. Question Header & Instructions
        lblModuleTitle = new JLabel("MODULE 01: CPU SCHEDULING (FCFS)");
        lblModuleTitle.setFont(new Font("Consolas", Font.BOLD, 14));
        lblModuleTitle.setForeground(RetroTheme.ACCENT_YELLOW);
        lblModuleTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblModuleTitle);

        card.add(Box.createVerticalStrut(4));

        lblInstruction = new JLabel(
                "Calculate Start Time, Finish Time, Waiting Time, and Turnaround Time for each job.");
        lblInstruction.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblInstruction.setForeground(RetroTheme.TEXT_WHITE);
        lblInstruction.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblInstruction);

        card.add(Box.createVerticalStrut(8));

        // 3. Formula Tip Box
        JPanel tipBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        tipBox.setOpaque(false);
        tipBox.setMaximumSize(new Dimension(1400, 32));
        tipBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        tipBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 40, 80), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        JLabel tipBadge = new JLabel("TIP & FORMULAS:");
        tipBadge.setFont(new Font("Consolas", Font.BOLD, 9));
        tipBadge.setForeground(RetroTheme.ACCENT_CYAN);
        lblTip = new JLabel("ST = max(AT, prev FT) | FT = ST + BT | TAT = FT - AT | WT = TAT - BT");
        lblTip.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblTip.setForeground(new Color(180, 180, 220));
        tipBox.add(tipBadge);
        tipBox.add(lblTip);
        card.add(tipBox);

        card.add(Box.createVerticalStrut(12));

        // 4. Interactive Quiz Table
        tableModel = new DefaultTableModel();
        quizTable = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int col) {
                if (activeQuestion == null)
                    return false;
                // Only cells that were originally '?' are editable!
                String origVal = activeQuestion.worksheetData[row][col];
                return origVal.trim().equals("?");
            }
        };
        quizTable.setFont(new Font("Consolas", Font.PLAIN, 12));
        quizTable.setRowHeight(30);
        quizTable.setBackground(RetroTheme.INPUT_BG);
        quizTable.setForeground(Color.WHITE);
        quizTable.setGridColor(RetroTheme.BORDER_COLOR);
        quizTable.getTableHeader().setBackground(RetroTheme.PANEL_BG);
        quizTable.getTableHeader().setForeground(RetroTheme.ACCENT_CYAN);
        quizTable.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 11));

        // Highlight editable cells in Yellow/Cyan
        quizTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int r,
                    int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFocus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                boolean isEditable = (activeQuestion != null && activeQuestion.worksheetData[r][c].trim().equals("?"));
                if (isEditable) {
                    l.setBackground(new Color(36, 24, 58));
                    l.setForeground(RetroTheme.ACCENT_YELLOW);
                    l.setFont(new Font("Consolas", Font.BOLD, 12));
                } else {
                    l.setBackground(RetroTheme.INPUT_BG);
                    l.setForeground(new Color(180, 180, 200));
                    l.setFont(new Font("Consolas", Font.PLAIN, 12));
                }
                return l;
            }
        });

        JScrollPane scrollTable = RetroTheme.createRetroScroll(quizTable);
        scrollTable.setPreferredSize(new Dimension(800, 150));
        scrollTable.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(scrollTable);

        card.add(Box.createVerticalStrut(10));

        // 5. Explanation Area (Revealed on demand)
        txtExplanation = new JTextArea();
        txtExplanation.setFont(new Font("Consolas", Font.PLAIN, 10));
        txtExplanation.setBackground(new Color(14, 10, 26));
        txtExplanation.setForeground(new Color(180, 180, 210));
        txtExplanation.setEditable(false);
        txtExplanation.setVisible(false);
        JScrollPane scrollExp = RetroTheme.createRetroScroll(txtExplanation);
        scrollExp.setPreferredSize(new Dimension(800, 75));
        scrollExp.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(scrollExp);

        return card;
    }

    private JButton createSelectBtn(String label, int modIdx) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Consolas", Font.BOLD, 10));
        btn.setForeground(currentModuleIdx == modIdx ? RetroTheme.ACCENT_PINK : RetroTheme.TEXT_WHITE);
        btn.setBackground(RetroTheme.BTN_DARK);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        currentModuleIdx == modIdx ? RetroTheme.ACCENT_PINK : RetroTheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            currentModuleIdx = modIdx;
            updateModuleButtons();
            loadQuestion(currentModuleIdx, currentMode);
        });
        return btn;
    }

    private void updateModuleButtons() {
        btnModCpu.setForeground(currentModuleIdx == 0 ? RetroTheme.ACCENT_PINK : RetroTheme.TEXT_WHITE);
        btnModBanker.setForeground(currentModuleIdx == 1 ? RetroTheme.ACCENT_PINK : RetroTheme.TEXT_WHITE);
        btnModPaging.setForeground(currentModuleIdx == 2 ? RetroTheme.ACCENT_PINK : RetroTheme.TEXT_WHITE);
        btnModCache.setForeground(currentModuleIdx == 3 ? RetroTheme.ACCENT_PINK : RetroTheme.TEXT_WHITE);
        repaint();
    }

    private void loadQuestion(int modIdx, QuizManager.Mode mode) {
        switch (modIdx) {
            case 0:
                activeQuestion = QuizManager.getCPUQuiz(mode);
                break;
            case 1:
                activeQuestion = QuizManager.getBankerQuiz(mode);
                break;
            case 2:
                activeQuestion = QuizManager.getPagingQuiz(mode);
                break;
            default:
                activeQuestion = QuizManager.getCacheQuiz(mode);
                break;
        }

        lblModuleTitle.setText(activeQuestion.moduleName);
        lblInstruction.setText(activeQuestion.instruction);
        lblTip.setText(activeQuestion.formulaTip);
        txtExplanation.setText(activeQuestion.explanation);
        txtExplanation.setVisible(false);

        // Populate Table Model
        tableModel.setDataVector(cloneGrid(activeQuestion.worksheetData), activeQuestion.columnHeaders);
        lblFeedback.setText("STATUS: Worksheet loaded. Double-click any yellow cell to enter your answer.");
        lblFeedback.setForeground(RetroTheme.TEXT_DIM);
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

        // Feedback label
        lblFeedback = new JLabel("STATUS: Worksheet loaded. Enter your answers and click CHECK.");
        lblFeedback.setFont(new Font("Consolas", Font.BOLD, 12));
        lblFeedback.setForeground(RetroTheme.TEXT_DIM);
        bar.add(lblFeedback, BorderLayout.NORTH);

        // Action Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setOpaque(false);

        JButton btnCheck = RetroTheme.createPrimaryButton("✔  CHECK MY ANSWERS");
        btnCheck.setFont(new Font("Consolas", Font.BOLD, 12));
        btnCheck.addActionListener(e -> checkAnswers());

        JButton btnReveal = RetroTheme.createRetroButton("👁 REVEAL ANSWERS", RetroTheme.BTN_DARK,
                RetroTheme.ACCENT_CYAN);
        btnReveal.setFont(new Font("Consolas", Font.BOLD, 11));
        btnReveal.addActionListener(e -> revealAnswers());

        JButton btnReset = RetroTheme.createRetroButton("🔄 RESET WORKSHEET", RetroTheme.BTN_DARK,
                RetroTheme.TEXT_WHITE);
        btnReset.setFont(new Font("Consolas", Font.BOLD, 11));
        btnReset.addActionListener(e -> loadQuestion(currentModuleIdx, currentMode));

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
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Object val = tableModel.getValueAt(r, c);
                userGrid[r][c] = (val == null) ? "" : val.toString();
            }
        }

        boolean pass = QuizManager.verify(userGrid, activeQuestion.answerKey);
        if (pass) {
            AppState.addScore(125);
            lblXpScore.setText("XP SCORE: " + AppState.getScoreFormatted());
            lblFeedback.setText("★ EXCELLENT WORK! All answers are 100% correct! +125 XP AWARDED! ★");
            lblFeedback.setForeground(RetroTheme.ACCENT_GREEN);

            JOptionPane.showMessageDialog(this,
                    "★ QUIZ MASTER! ★\n" +
                            "All worksheet values are mathematically verified!\n" +
                            "Reward: +125 XP added to your Player Profile.",
                    "Victory", JOptionPane.INFORMATION_MESSAGE);
        } else {
            lblFeedback.setText("✘ INCORRECT. Some values do not match. Check the formulas and try again.");
            lblFeedback.setForeground(RetroTheme.ACCENT_PINK);
        }
    }

    private void revealAnswers() {
        tableModel.setDataVector(cloneGrid(activeQuestion.answerKey), activeQuestion.columnHeaders);
        txtExplanation.setVisible(true);
        lblFeedback.setText("ANSWER KEY REVEALED. Review the calculation breakdown below.");
        lblFeedback.setForeground(RetroTheme.ACCENT_YELLOW);
        revalidate();
        repaint();
    }
}
