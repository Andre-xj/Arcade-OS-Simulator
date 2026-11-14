package arcadeos;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * Screen: OPERATING SYSTEM ARCHIVE SPECIFICATION MANUAL
 * Template: Capcom / Mega Man Boss Intel & Hunter Base Telemetry Database
 *
 * Enhanced with Dynamic Horizontal Auto-Centering:
 * - Content in all 5 tabs (Theory, Formulas, Pseudocode, Worked Trace, Cheat Sheet)
 *   is dynamically centered horizontally in the terminal viewport, eliminating awkward
 *   left-hugging or empty right space.
 * - Mascot title, subtitle, and badges in the left telemetry column are centered.
 * - 2-Column Arcade HUD Layout with 100% bug-free vector rendering.
 */
public class ArchiveManualDialog extends JDialog {
    private ArcadeOS parentFrame;
    private ArchivesPanel parentPanel;
    private int manualIndex;
    private ArchiveManualData.ManualEntry manual;

    private JTextPane[] tabTextPanes;
    private CardLayout cardLayout;
    private JPanel cardContainer;
    private JButton[] tabButtons;
    private int selectedTabIndex = 0;

    private JTextField txtSearch;
    private JLabel lblSearchMatches;
    private JLabel lblStatusToast;

    // Mascot floating animation
    private Timer mascotBobTimer;
    private int bobTicks = 0;
    private float bobOffset = 0.0f;
    private JPanel telemetryCard;

    // Tab accent colors
    private final Color[] tabAccents = {
        RetroTheme.ACCENT_CYAN,                  // 0. Theory
        RetroTheme.ACCENT_YELLOW,                // 1. Formulas
        new Color(195, 110, 255),                // 2. Pseudocode (Electric Violet)
        RetroTheme.ACCENT_GREEN,                 // 3. Worked Trace
        new Color(255, 145, 50)                  // 4. Cheat Sheet
    };

    private final String[] tabTitles = {
        "01 THEORY",
        "02 FORMULAS",
        "03 PSEUDOCODE",
        "04 WORKED TRACE",
        "05 CHEAT SHEET"
    };

    private final String[] rawTabContents;

    public ArchiveManualDialog(ArcadeOS parentFrame, ArchivesPanel parentPanel, int index) {
        super(parentFrame, "CAPCOM / KERNEL INTEL DATABASE // ARCHIVE #" + (index + 1), true);
        this.parentFrame = parentFrame;
        this.parentPanel = parentPanel;
        this.manualIndex = index;
        this.manual = ArchiveManualData.getManual(index);

        this.rawTabContents = new String[] {
            manual.theory,
            manual.formulas,
            manual.pseudocode,
            manual.workedExample,
            manual.cheatSheet
        };

        setSize(840, 580);
        setResizable(false);
        setLocationRelativeTo(parentFrame);
        getContentPane().setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(0, 10));

        // 1. Top HUD Header Bar
        add(createTopHeaderBar(), BorderLayout.NORTH);

        // 2. Center 2-Column Split (Left: Telemetry Card, Right: Centered Blueprint Terminal)
        add(createMain2ColumnContent(), BorderLayout.CENTER);

        // 3. Bottom System Footer
        add(createBottomSystemFooter(), BorderLayout.SOUTH);

        // Mascot idle floating bob animation timer (60 FPS)
        mascotBobTimer = new Timer(16, e -> {
            bobTicks++;
            bobOffset = (float)(Math.sin(bobTicks * 0.065) * 5.0);
            if (telemetryCard != null) {
                telemetryCard.repaint();
            }
        });
        mascotBobTimer.start();

        // Keyboard navigation: [ESC] to close, [1]-[5] to switch tabs (when not typing in search)
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        for (int i = 0; i < 5; i++) {
            final int tIdx = i;
            getRootPane().registerKeyboardAction(e -> {
                if (txtSearch == null || !txtSearch.hasFocus()) {
                    selectTab(tIdx);
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_1 + i, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (mascotBobTimer != null) mascotBobTimer.stop();
            }
        });
    }

    /** Top HUD Header: Minimal, high-tech, uncluttered */
    private JPanel createTopHeaderBar() {
        JPanel header = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 15, 38));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(60, 45, 90));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        // Left Tag
        JPanel leftTag = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftTag.setOpaque(false);

        JLabel tagSystem = new JLabel("MINI // OS  [INTEL ARCHIVE PROTOCOL]");
        tagSystem.setFont(new Font("Consolas", Font.BOLD, 13));
        tagSystem.setForeground(RetroTheme.ACCENT_PINK);
        leftTag.add(tagSystem);

        JLabel subTag = new JLabel("// STAGE " + manual.id + " SPECIFICATION");
        subTag.setFont(new Font("Consolas", Font.PLAIN, 11));
        subTag.setForeground(RetroTheme.ACCENT_CYAN);
        leftTag.add(subTag);

        header.add(leftTag, BorderLayout.WEST);

        // Right Tools: [COPY NOTES], [CLOSE [ESC]]
        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTools.setOpaque(false);

        JButton btnCopy = createGhostButton("COPY NOTES", "Copy current notes to clipboard");
        btnCopy.addActionListener(e -> copyCurrentNotes());

        JButton btnClose = RetroTheme.createRetroButton("CLOSE [ESC]", RetroTheme.BTN_DARK, RetroTheme.TEXT_DIM);
        btnClose.setFont(new Font("Consolas", Font.BOLD, 10));
        btnClose.addActionListener(e -> dispose());

        rightTools.add(btnCopy);
        rightTools.add(btnClose);

        header.add(rightTools, BorderLayout.EAST);
        return header;
    }

    /**
     * 2-Column Split:
     * - Left: Capcom / Mega Man Module Telemetry Card (~300px)
     * - Right: Tactical Blueprint & Data Terminal (~740px)
     */
    private JPanel createMain2ColumnContent() {
        JPanel container = new JPanel(new BorderLayout(14, 0));
        container.setOpaque(false);
        container.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        // LEFT COLUMN: Telemetry Card
        container.add(createLeftTelemetryColumn(), BorderLayout.WEST);

        // RIGHT COLUMN: Blueprint Terminal
        container.add(createRightTerminalColumn(), BorderLayout.CENTER);

        return container;
    }

    /**
     * Left Column: Mega Man Boss Intel Dossier
     * Features Mascot Hologram, Subsystem Data, Difficulty, and Launch Actions
     */
    private JPanel createLeftTelemetryColumn() {
        telemetryCard = new JPanel(new BorderLayout(0, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Cyberpunk panel background
                g2.setColor(new Color(20, 14, 38));
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                // Border
                g2.setColor(new Color(65, 45, 100));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

                // Top highlight bar
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.fillRect(16, 0, w - 32, 2);

                g2.dispose();
            }
        };
        telemetryCard.setPreferredSize(new Dimension(230, 0));
        telemetryCard.setOpaque(false);
        telemetryCard.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));

        // 1. Top Section: Holographic Mascot Box
        JPanel mascotBox = new JPanel() {
            @Override
            public Dimension getPreferredSize() { return new Dimension(206, 105); }
            @Override
            public Dimension getMaximumSize() { return new Dimension(206, 105); }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Dark hologram chamber
                g2.setColor(new Color(12, 8, 24));
                g2.fillRoundRect(0, 0, w, h, 10, 10);
                g2.setColor(new Color(45, 32, 70));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Radar reticle in background
                int cx = w / 2;
                int cy = h / 2 - 4;
                g2.setColor(new Color(5, 217, 232, 35));
                g2.drawOval(cx - 36, cy - 36, 72, 72);
                g2.drawOval(cx - 20, cy - 20, 40, 40);
                g2.drawLine(cx - 42, cy, cx + 42, cy);
                g2.drawLine(cx, cy - 42, cx, cy + 42);

                // Corner HUD brackets
                g2.setColor(RetroTheme.ACCENT_PINK);
                int bLen = 8;
                g2.drawLine(5, 5, 5 + bLen, 5);
                g2.drawLine(5, 5, 5, 5 + bLen);
                g2.drawLine(w - 6, 5, w - 6 - bLen, 5);
                g2.drawLine(w - 6, 5, w - 6, 5 + bLen);
                g2.drawLine(5, h - 6, 5 + bLen, h - 6);
                g2.drawLine(5, h - 6, 5, h - 6 - bLen);
                g2.drawLine(w - 6, h - 6, w - 6 - bLen, h - 6);
                g2.drawLine(w - 6, h - 6, w - 6, h - 6 - bLen);

                // Draw floating mascot sprite
                Image spr = getSpriteForManual(manualIndex, 54);
                int sprX = cx - 27;
                int sprY = cy - 27 + (int)bobOffset;
                g2.drawImage(spr, sprX, sprY, null);

                // Bottom chamber status text
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                boolean isRead = (AppState.filesRead[manualIndex] == 1);
                g2.setColor(isRead ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_YELLOW);
                String chamberTag = isRead ? "[ INTEL ANALYZED // READY ]" : "[ INTEL PENDING STUDY ]";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(chamberTag, cx - fm.stringWidth(chamberTag) / 2, h - 7);

                g2.dispose();
            }
        };
        mascotBox.setOpaque(false);
        telemetryCard.add(mascotBox, BorderLayout.NORTH);

        // 2. Middle Section: Telemetry Data Fields
        JPanel statsPanel = new JPanel();
        statsPanel.setOpaque(false);
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));

        // Centered Titles in Left Card
        JLabel titleLbl = new JLabel(manual.title);
        titleLbl.setFont(new Font("Consolas", Font.BOLD, 14));
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
        statsPanel.add(titleLbl);

        JLabel subLbl = new JLabel(manual.subtitle);
        subLbl.setFont(new Font("Consolas", Font.PLAIN, 9));
        subLbl.setForeground(RetroTheme.ACCENT_CYAN);
        subLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        subLbl.setHorizontalAlignment(SwingConstants.CENTER);
        statsPanel.add(subLbl);
        statsPanel.add(Box.createVerticalStrut(8));

        // Data Rows
        statsPanel.add(createStatRow("STAGE ID:", "MODULE #" + manual.id, Color.WHITE));
        statsPanel.add(Box.createVerticalStrut(4));

        int compVal = parseComplexityLevel(manual.complexity);
        statsPanel.add(createDifficultyRow(compVal));
        statsPanel.add(Box.createVerticalStrut(4));

        statsPanel.add(createStatRow("SUBSYSTEM:", "OS KERNEL CORE", new Color(190, 180, 240)));
        statsPanel.add(Box.createVerticalStrut(4));

        statsPanel.add(createStatRow("SIMULATOR:", "TAB #" + (manual.simulatorTabIndex + 1), RetroTheme.ACCENT_PINK));
        statsPanel.add(Box.createVerticalStrut(4));

        statsPanel.add(createStatRow("EXP REWARD:", "+50 XP EARNED", RetroTheme.ACCENT_GREEN));
        statsPanel.add(Box.createVerticalStrut(8));

        // Tags Pill Area (Centered)
        JLabel lblTags = new JLabel("ALGORITHM TAGS:");
        lblTags.setFont(new Font("Consolas", Font.BOLD, 9));
        lblTags.setForeground(RetroTheme.TEXT_DIM);
        lblTags.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsPanel.add(lblTags);
        statsPanel.add(Box.createVerticalStrut(3));

        JPanel tagsWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 3));
        tagsWrap.setOpaque(false);
        tagsWrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        for (String t : manual.tags.split("\\|")) {
            String clean = t.trim();
            if (!clean.isEmpty()) {
                JLabel pill = new JLabel(clean);
                pill.setFont(new Font("Consolas", Font.PLAIN, 9));
                pill.setForeground(new Color(210, 200, 255));
                pill.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(75, 55, 115), 1),
                        BorderFactory.createEmptyBorder(2, 5, 2, 5)
                ));
                tagsWrap.add(pill);
            }
        }
        statsPanel.add(tagsWrap);

        telemetryCard.add(statsPanel, BorderLayout.CENTER);

        // 3. Bottom Section: Action Launch Buttons
        JPanel actionsBox = new JPanel(new GridLayout(3, 1, 0, 6));
        actionsBox.setOpaque(false);

        JButton btnSim = RetroTheme.createRetroButton("LAUNCH SIMULATOR", new Color(45, 18, 55), RetroTheme.ACCENT_PINK);
        btnSim.setFont(new Font("Consolas", Font.BOLD, 10));
        btnSim.addActionListener(e -> {
            markManualRead();
            dispose();
            parentFrame.openSimulatorTab(manual.simulatorTabIndex);
        });
        actionsBox.add(btnSim);

        JButton btnQuiz = RetroTheme.createRetroButton("TEST IN QUIZ ARENA", RetroTheme.BTN_DARK, RetroTheme.ACCENT_YELLOW);
        btnQuiz.setFont(new Font("Consolas", Font.BOLD, 10));
        btnQuiz.addActionListener(e -> {
            markManualRead();
            dispose();
            parentFrame.showScreen(ArcadeOS.SCREEN_QUIZ);
        });
        actionsBox.add(btnQuiz);

        JButton btnMarkRead = RetroTheme.createRetroButton("MARK AS STUDIED", new Color(18, 42, 28), RetroTheme.ACCENT_GREEN);
        btnMarkRead.setFont(new Font("Consolas", Font.BOLD, 10));
        btnMarkRead.addActionListener(e -> {
            markManualRead();
            dispose();
        });
        actionsBox.add(btnMarkRead);

        telemetryCard.add(actionsBox, BorderLayout.SOUTH);
        return telemetryCard;
    }

    private JPanel createStatRow(String label, String value, Color valColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Consolas", Font.BOLD, 10));
        lbl.setForeground(RetroTheme.TEXT_DIM);
        row.add(lbl, BorderLayout.WEST);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Consolas", Font.BOLD, 10));
        val.setForeground(valColor);
        row.add(val, BorderLayout.EAST);

        return row;
    }

    private JPanel createDifficultyRow(int level) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel("DIFFICULTY:");
        lbl.setFont(new Font("Consolas", Font.BOLD, 10));
        lbl.setForeground(RetroTheme.TEXT_DIM);
        row.add(lbl, BorderLayout.WEST);

        // Vector glowing pips
        JPanel pipsPnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 2)) {
            @Override
            public Dimension getPreferredSize() { return new Dimension(110, 14); }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int pw = 14;
                int ph = 8;
                int gap = 3;
                int startX = getWidth() - (5 * pw + 4 * gap);
                int py = 2;

                for (int i = 0; i < 5; i++) {
                    int px = startX + i * (pw + gap);
                    if (i < level) {
                        g2.setColor(RetroTheme.ACCENT_YELLOW);
                        g2.fillRoundRect(px, py, pw, ph, 2, 2);
                    } else {
                        g2.setColor(new Color(36, 26, 52));
                        g2.fillRoundRect(px, py, pw, ph, 2, 2);
                        g2.setColor(new Color(65, 48, 90));
                        g2.drawRoundRect(px, py, pw, ph, 2, 2);
                    }
                }
                g2.dispose();
            }
        };
        pipsPnl.setOpaque(false);
        row.add(pipsPnl, BorderLayout.EAST);
        return row;
    }

    /**
     * Right Column: Tactical Blueprint & Data Terminal
     * Features Search Bar, Segmented Cyber Tabs, and Dynamically Centered Document Viewer
     */
    private JPanel createRightTerminalColumn() {
        JPanel terminal = new JPanel(new BorderLayout(0, 8));
        terminal.setOpaque(false);

        // 1. Top Sub-Bar: Search input + Matches + Toast
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setOpaque(false);

        JPanel leftSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftSearch.setOpaque(false);

        JLabel lblSearch = new JLabel("SEARCH INTEL:");
        lblSearch.setFont(new Font("Consolas", Font.BOLD, 10));
        lblSearch.setForeground(RetroTheme.ACCENT_CYAN);
        leftSearch.add(lblSearch);

        txtSearch = new JTextField(18);
        txtSearch.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtSearch.setBackground(RetroTheme.INPUT_BG);
        txtSearch.setForeground(Color.WHITE);
        txtSearch.setCaretColor(RetroTheme.ACCENT_CYAN);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 50, 110), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                highlightKeyword(txtSearch.getText().trim());
            }
        });
        leftSearch.add(txtSearch);

        lblSearchMatches = new JLabel("");
        lblSearchMatches.setFont(new Font("Consolas", Font.BOLD, 10));
        lblSearchMatches.setForeground(RetroTheme.ACCENT_YELLOW);
        leftSearch.add(lblSearchMatches);

        lblStatusToast = new JLabel("");
        lblStatusToast.setFont(new Font("Consolas", Font.BOLD, 10));
        lblStatusToast.setForeground(RetroTheme.ACCENT_GREEN);
        leftSearch.add(lblStatusToast);

        searchBar.add(leftSearch, BorderLayout.WEST);

        // Right side of search bar: Protocol Tag
        JLabel rightTag = new JLabel("[ PROTOCOL: LEVEL_4 KERNEL ACCESS ]");
        rightTag.setFont(new Font("Consolas", Font.PLAIN, 9));
        rightTag.setForeground(RetroTheme.TEXT_DIM);
        searchBar.add(rightTag, BorderLayout.EAST);

        terminal.add(searchBar, BorderLayout.NORTH);

        // 2. Segmented Cyber HUD Tabs (Single Row, Never Wraps!)
        JPanel tabSwitcher = new JPanel(new GridLayout(1, 5, 5, 0));
        tabSwitcher.setOpaque(false);
        tabSwitcher.setPreferredSize(new Dimension(0, 36));

        tabButtons = new JButton[5];
        for (int i = 0; i < 5; i++) {
            final int tIdx = i;
            JButton tabBtn = new JButton(tabTitles[i]) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    boolean isSelected = (selectedTabIndex == tIdx);
                    boolean isHover = getModel().isRollover();

                    if (isSelected) {
                        g2.setColor(new Color(32, 22, 58));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        // Glowing accent line on bottom
                        g2.setColor(tabAccents[tIdx]);
                        g2.fillRect(0, getHeight() - 3, getWidth(), 3);
                        g2.setColor(new Color(tabAccents[tIdx].getRed(), tabAccents[tIdx].getGreen(), tabAccents[tIdx].getBlue(), 80));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    } else if (isHover) {
                        g2.setColor(new Color(24, 18, 44));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        g2.setColor(new Color(75, 55, 115));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    } else {
                        g2.setColor(new Color(16, 12, 30));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        g2.setColor(new Color(45, 35, 68));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            tabBtn.setFont(new Font("Consolas", Font.BOLD, 10));
            tabBtn.setForeground(i == selectedTabIndex ? tabAccents[i] : new Color(160, 160, 190));
            tabBtn.setFocusPainted(false);
            tabBtn.setOpaque(false);
            tabBtn.setContentAreaFilled(false);
            tabBtn.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));
            tabBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            tabBtn.addActionListener(e -> selectTab(tIdx));

            tabButtons[i] = tabBtn;
            tabSwitcher.add(tabBtn);
        }

        // 3. Document View Area
        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setOpaque(false);

        tabTextPanes = new JTextPane[5];
        for (int i = 0; i < 5; i++) {
            cardContainer.add(createStyledReaderView(i, rawTabContents[i], tabAccents[i]), "TAB_" + i);
        }

        JPanel centerTerminalCard = new JPanel(new BorderLayout(0, 6));
        centerTerminalCard.setOpaque(false);
        centerTerminalCard.add(tabSwitcher, BorderLayout.NORTH);
        centerTerminalCard.add(cardContainer, BorderLayout.CENTER);

        terminal.add(centerTerminalCard, BorderLayout.CENTER);
        return terminal;
    }

    private void selectTab(int tabIndex) {
        selectedTabIndex = tabIndex;
        // Switch tab card view
        cardLayout.show(cardContainer, "TAB_" + tabIndex);

        if (tabTextPanes != null && tabIndex >= 0 && tabIndex < tabTextPanes.length && tabTextPanes[tabIndex] != null) {
            JTextPane p = tabTextPanes[tabIndex];
            p.revalidate();
            p.repaint();
            p.setCaretPosition(0);
        }

        for (int i = 0; i < tabButtons.length; i++) {
            tabButtons[i].setForeground(i == selectedTabIndex ? tabAccents[i] : new Color(160, 160, 190));
            tabButtons[i].repaint();
        }

        if (txtSearch != null && !txtSearch.getText().trim().isEmpty()) {
            highlightKeyword(txtSearch.getText().trim());
        }
    }

    /**
     * Creates a styled reader pane with locked horizontal width.
     * Implements intelligent text auto-fitting to the fixed window size:
     * - Perfectly equal margins on both sides across all tabs and all manuals.
     * - Dynamic auto-centering insets ensure left space == right space to the exact pixel.
     * - Monospace Consolas 12 font ensures consistent 74-character cyber box alignment.
     */
    private JScrollPane createStyledReaderView(int tabIdx, String rawText, Color accent) {
        // Intelligent Auto-Fit: Calculate optimal vertical centering
        String[] lines = rawText.split("\n");
        int lineCount = lines.length;
        int maxLineLen = 0;
        for (String l : lines) {
            if (l.length() > maxLineLen) maxLineLen = l.length();
        }

        int fontSize = 12;
        float lineSpacing = 0.08f;
        int topMargin = Math.max(12, Math.min(26, (380 - (int)(lineCount * 17.0f)) / 2));

        JTextPane pane = new JTextPane() {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                // Lock horizontal width to viewport: NEVER scroll horizontally!
                return true;
            }

            @Override
            public Insets getInsets() {
                // Precision horizontal auto-centering: guarantees left space == right space to the pixel
                Font f = new Font("Consolas", Font.PLAIN, 12);
                FontMetrics fm = getFontMetrics(f);
                int textW = fm.stringWidth("╔════════════════════════════════════════════════════════════════════════╗");
                int w = getWidth();
                int hMargin = Math.max(12, (w - textW) / 2);
                return new Insets(topMargin, hMargin, topMargin, hMargin);
            }

            @Override
            public Insets getInsets(Insets in) {
                Insets dyn = getInsets();
                in.top = dyn.top;
                in.left = dyn.left;
                in.bottom = dyn.bottom;
                in.right = dyn.right;
                return in;
            }
        };
        pane.setFont(new Font("Consolas", Font.PLAIN, 12));
        pane.setEditable(false);
        pane.setBackground(new Color(15, 11, 28));
        pane.setCaretColor(RetroTheme.ACCENT_CYAN);

        populateStyledText(pane, rawText, accent, fontSize, lineSpacing);
        tabTextPanes[tabIdx] = pane;

        JScrollPane sp = new JScrollPane(pane);
        sp.getViewport().setBackground(new Color(15, 11, 28));
        sp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 40, 85), 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        // Completely disable horizontal scrolling - content is fixed in center!
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(18);

        return sp;
    }

    private void populateStyledText(JTextPane pane, String text, Color accent, int fontSize, float lineSpacing) {
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());

            Style def = pane.addStyle("def", null);
            StyleConstants.setFontFamily(def, "Consolas");
            StyleConstants.setFontSize(def, fontSize);
            StyleConstants.setLineSpacing(def, lineSpacing);
            StyleConstants.setForeground(def, new Color(225, 225, 245));

            Style box = pane.addStyle("box", def);
            StyleConstants.setForeground(box, new Color(115, 95, 175)); // Crisp cyber outline

            Style head = pane.addStyle("head", def);
            StyleConstants.setBold(head, true);
            StyleConstants.setForeground(head, accent);

            Style comment = pane.addStyle("comment", def);
            StyleConstants.setItalic(comment, true);
            StyleConstants.setForeground(comment, new Color(130, 145, 175));

            Style kw = pane.addStyle("kw", def);
            StyleConstants.setBold(kw, true);
            StyleConstants.setForeground(kw, new Color(255, 85, 145)); // Electric Coral

            Style typeStyle = pane.addStyle("type", def);
            StyleConstants.setBold(typeStyle, true);
            StyleConstants.setForeground(typeStyle, RetroTheme.ACCENT_CYAN);

            Style fnStyle = pane.addStyle("fn", def);
            StyleConstants.setBold(fnStyle, true);
            StyleConstants.setForeground(fnStyle, RetroTheme.ACCENT_YELLOW);

            Style metric = pane.addStyle("metric", def);
            StyleConstants.setBold(metric, true);
            StyleConstants.setForeground(metric, RetroTheme.ACCENT_CYAN);

            Style highlight = pane.addStyle("highlight", def);
            StyleConstants.setBold(highlight, true);
            StyleConstants.setForeground(highlight, RetroTheme.ACCENT_YELLOW);

            String[] lines = text.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();

                if (trimmed.startsWith("╔") || trimmed.startsWith("╚") || trimmed.startsWith("┌") ||
                    trimmed.startsWith("└") || trimmed.startsWith("├")) {
                    doc.insertString(doc.getLength(), line + "\n", box);
                } else if (trimmed.startsWith("║")) {
                    doc.insertString(doc.getLength(), line + "\n", head);
                } else if (trimmed.startsWith("│")) {
                    // Check if table header
                    if (trimmed.contains("METRIC NAME") || trimmed.contains("CRITERIA") ||
                        trimmed.contains("ALLOCATION") || trimmed.contains("REF PAG") ||
                        trimmed.contains("ALGORITHM") || trimmed.contains("ARCHITECTURAL FACT") ||
                        trimmed.contains("JOB │") || trimmed.contains("PROC│") ||
                        trimmed.contains("PARAMETER") || trimmed.contains("STRATEGY") ||
                        trimmed.contains("POLICY") || trimmed.contains("HANDLING METHOD") ||
                        trimmed.contains("MEMORY ARCHITECTURE") || trimmed.contains("HARDWARE CHECK") ||
                        trimmed.contains("MEMORY MODEL") || trimmed.contains("DEGREE N") ||
                        trimmed.contains("SCHEDULER TIER") || trimmed.contains("STEP │")) {
                        doc.insertString(doc.getLength(), line + "\n", metric);
                    } else {
                        // Data row or Cyber Code Box
                        String[] cols = line.split("│", -1);
                        if (cols.length == 3) {
                            // Single-column Cyber Box (e.g. Pseudocode or Cyber Title)
                            doc.insertString(doc.getLength(), "│", box);
                            String inner = cols[1];
                            String innerTrim = inner.trim();
                            if (innerTrim.startsWith("ENGINE") || innerTrim.startsWith("SAFETY") ||
                                innerTrim.startsWith("MMU") || innerTrim.startsWith("FIRST-FIT") ||
                                innerTrim.startsWith("BEST-FIT") || innerTrim.startsWith("SSTF") ||
                                innerTrim.startsWith("SCAN") || innerTrim.startsWith("CONCURRENCY") ||
                                innerTrim.startsWith("LRU") || innerTrim.startsWith("CLOCK") ||
                                innerTrim.startsWith("SEGMENTATION")) {
                                doc.insertString(doc.getLength(), inner, head);
                            } else if (inner.contains("function ") || inner.contains("for ") ||
                                       inner.contains("if ") || inner.contains("while ") ||
                                       inner.contains("return ") || inner.contains("int ") ||
                                       inner.contains("double ") || inner.contains("boolean ") ||
                                       inner.contains("//") || inner.contains("new ") ||
                                       inner.contains("Work =") || inner.contains("SafeSequence")) {
                                // Tokenize code line inside cyber box
                                int commentIdx = inner.indexOf("//");
                                String codePart = (commentIdx >= 0) ? inner.substring(0, commentIdx) : inner;
                                String commentPart = (commentIdx >= 0) ? inner.substring(commentIdx) : "";

                                String[] tokens = codePart.split("(?<=[\\s(),;{}<>=!+\\-*/\\[\\]])|(?=[\\s(),;{}<>=!+\\-*/\\[\\]])");
                                for (String tok : tokens) {
                                    String cleanTok = tok.trim();
                                    if (cleanTok.equals("function") || cleanTok.equals("for") || cleanTok.equals("if") ||
                                        cleanTok.equals("else") || cleanTok.equals("while") || cleanTok.equals("return") ||
                                        cleanTok.equals("true") || cleanTok.equals("false") || cleanTok.equals("new")) {
                                        doc.insertString(doc.getLength(), tok, kw);
                                    } else if (cleanTok.equals("Job") || cleanTok.equals("Queue") || cleanTok.equals("PageTable") ||
                                               cleanTok.equals("SegmentEntry") || cleanTok.equals("double") || cleanTok.equals("int") ||
                                               cleanTok.equals("boolean") || cleanTok.equals("void") || cleanTok.equals("List") ||
                                               cleanTok.equals("ArrayList")) {
                                        doc.insertString(doc.getLength(), tok, typeStyle);
                                    } else if (cleanTok.startsWith("schedule_") || cleanTok.startsWith("sort_") ||
                                               cleanTok.startsWith("record_") || cleanTok.startsWith("translate") ||
                                               cleanTok.startsWith("find_") || cleanTok.startsWith("min") ||
                                               cleanTok.startsWith("max") || cleanTok.startsWith("dequeue") ||
                                               cleanTok.startsWith("enqueue") || cleanTok.startsWith("verify_") ||
                                               cleanTok.startsWith("vector_") || cleanTok.startsWith("touch_") ||
                                               cleanTok.startsWith("trigger_") || cleanTok.startsWith("raise_") ||
                                               cleanTok.startsWith("split_") || cleanTok.startsWith("spool_")) {
                                        doc.insertString(doc.getLength(), tok, fnStyle);
                                    } else {
                                        doc.insertString(doc.getLength(), tok, def);
                                    }
                                }
                                if (!commentPart.isEmpty()) {
                                    doc.insertString(doc.getLength(), commentPart, comment);
                                }
                            } else {
                                doc.insertString(doc.getLength(), inner, highlight);
                            }
                            doc.insertString(doc.getLength(), "│\n", box);
                        } else if (cols.length >= 4) {
                            // Multi-column table: col 1 highlighted, others styled cleanly
                            doc.insertString(doc.getLength(), "│", box);
                            doc.insertString(doc.getLength(), cols[1], highlight);
                            doc.insertString(doc.getLength(), "│", box);
                            for (int c = 2; c < cols.length - 1; c++) {
                                doc.insertString(doc.getLength(), cols[c], def);
                                doc.insertString(doc.getLength(), "│", box);
                            }
                            doc.insertString(doc.getLength(), "\n", def);
                        } else {
                            doc.insertString(doc.getLength(), line + "\n", def);
                        }
                    }
                } else if (trimmed.startsWith("//")) {
                    doc.insertString(doc.getLength(), line + "\n", comment);
                } else if (trimmed.contains("function ") || trimmed.contains("schedule_") ||
                           trimmed.contains("while ") || trimmed.contains("for ") ||
                           trimmed.contains("if ") || trimmed.contains("return ") ||
                           trimmed.contains("sort_") || trimmed.contains("Work =") ||
                           trimmed.contains("SafeSequence")) {
                    // Tokenized syntax highlighting for code lines
                    int commentIdx = line.indexOf("//");
                    String codePart = (commentIdx >= 0) ? line.substring(0, commentIdx) : line;
                    String commentPart = (commentIdx >= 0) ? line.substring(commentIdx) : "";

                    String[] tokens = codePart.split("(?<=[\\s(),;{}<>=!+\\-*/\\[\\]])|(?=[\\s(),;{}<>=!+\\-*/\\[\\]])");
                    for (String tok : tokens) {
                        String cleanTok = tok.trim();
                        if (cleanTok.equals("function") || cleanTok.equals("for") || cleanTok.equals("if") ||
                            cleanTok.equals("else") || cleanTok.equals("while") || cleanTok.equals("return") ||
                            cleanTok.equals("true") || cleanTok.equals("false")) {
                            doc.insertString(doc.getLength(), tok, kw);
                        } else if (cleanTok.equals("Job") || cleanTok.equals("Queue") || cleanTok.equals("PageTable") ||
                                   cleanTok.equals("SegmentEntry") || cleanTok.equals("double") || cleanTok.equals("int") ||
                                   cleanTok.equals("boolean") || cleanTok.equals("void")) {
                            doc.insertString(doc.getLength(), tok, typeStyle);
                        } else if (cleanTok.startsWith("schedule_") || cleanTok.startsWith("sort_") ||
                                   cleanTok.startsWith("record_") || cleanTok.startsWith("translate") ||
                                   cleanTok.startsWith("find_") || cleanTok.startsWith("min") ||
                                   cleanTok.startsWith("max") || cleanTok.startsWith("dequeue") || cleanTok.startsWith("enqueue")) {
                            doc.insertString(doc.getLength(), tok, fnStyle);
                        } else {
                            doc.insertString(doc.getLength(), tok, def);
                        }
                    }
                    if (!commentPart.isEmpty()) {
                        doc.insertString(doc.getLength(), commentPart, comment);
                    }
                    doc.insertString(doc.getLength(), "\n", def);
                } else if (trimmed.startsWith("★")) {
                    doc.insertString(doc.getLength(), line + "\n", head);
                } else if (line.contains("COMPLEXITY:") || line.contains("CRITERIA:") ||
                           line.contains("CALCULATION") || line.contains("STEP-BY-STEP") ||
                           line.contains("RULES:") || line.contains("NUMERICAL") ||
                           line.contains("RESULT:") || line.contains("SUMMARY:")) {
                    doc.insertString(doc.getLength(), line + "\n", metric);
                } else {
                    doc.insertString(doc.getLength(), line + "\n", def);
                }
            }

            pane.setCaretPosition(0);
        } catch (Exception e) {
            pane.setText(text);
        }
    }

    private void copyCurrentNotes() {
        if (selectedTabIndex >= 0 && selectedTabIndex < tabTextPanes.length) {
            String text = tabTextPanes[selectedTabIndex].getText();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            showToast("NOTES COPIED TO CLIPBOARD!");
        }
    }

    private void highlightKeyword(String kw) {
        if (selectedTabIndex < 0 || tabTextPanes[selectedTabIndex] == null) return;

        JTextPane pane = tabTextPanes[selectedTabIndex];
        Highlighter hl = pane.getHighlighter();
        hl.removeAllHighlights();

        if (kw.isEmpty()) {
            lblSearchMatches.setText("");
            return;
        }

        String content = pane.getText().toLowerCase();
        String query = kw.toLowerCase();
        int idx = 0;
        int count = 0;
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 42, 141, 140));

        while ((idx = content.indexOf(query, idx)) >= 0) {
            try {
                hl.addHighlight(idx, idx + query.length(), painter);
                count++;
                idx += query.length();
            } catch (BadLocationException ignored) {
                break;
            }
        }

        lblSearchMatches.setText("[" + count + " MATCHES]");
    }

    private void showToast(String msg) {
        if (lblStatusToast != null) {
            lblStatusToast.setText("✔ " + msg);
            Timer t = new Timer(3000, e -> lblStatusToast.setText(""));
            t.setRepeats(false);
            t.start();
        }
    }

    private JPanel createBottomSystemFooter() {
        JPanel footer = new JPanel(new BorderLayout(15, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 18, 10, 18));

        JLabel lblLeft = new JLabel("KERNEL REVISION 2.4  //  CCIS OPERATING SYSTEMS SPECIFICATION MANUAL");
        lblLeft.setFont(new Font("Consolas", Font.PLAIN, 9));
        lblLeft.setForeground(RetroTheme.TEXT_DIM);
        footer.add(lblLeft, BorderLayout.WEST);

        String userTag = String.format("OPERATOR: %s  |  HI-SCORE: %s PTS",
                AppState.playerName, AppState.getScoreFormatted());
        JLabel lblRight = new JLabel(userTag);
        lblRight.setFont(new Font("Consolas", Font.BOLD, 10));
        lblRight.setForeground(RetroTheme.ACCENT_PINK);
        footer.add(lblRight, BorderLayout.EAST);

        return footer;
    }

    private JButton createGhostButton(String text, String tip) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Consolas", Font.BOLD, 10));
        btn.setForeground(new Color(220, 220, 245));
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 60, 120), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        btn.setToolTipText(tip);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(RetroTheme.ACCENT_CYAN);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 1),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8)
                ));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(new Color(220, 220, 245));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(80, 60, 120), 1),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8)
                ));
            }
        });
        return btn;
    }

    private void markManualRead() {
        if (AppState.filesRead[manualIndex] == 0) {
            AppState.markRead(manualIndex);
            AppState.addScore(50);
            if (parentPanel != null) {
                parentPanel.updateCounters();
            }
        }
    }

    private int parseComplexityLevel(String compStr) {
        if (compStr == null) return 3;
        int count = 0;
        for (char c : compStr.toCharArray()) {
            if (c == '★' || c == '*') count++;
        }
        if (count > 0) return Math.min(5, count);
        if (compStr.contains("EASY")) return 2;
        if (compStr.contains("MODERATE")) return 3;
        if (compStr.contains("HARD")) return 4;
        if (compStr.contains("EXPERT")) return 5;
        return 3;
    }

    private Image getSpriteForManual(int idx, int sz) {
        switch (idx) {
            case 0: return PixelSprites.createAlienSprite(sz, new Color(170, 90, 255));
            case 1: return PixelSprites.createCanSprite(sz, new Color(255, 100, 130));
            case 2: return PixelSprites.createUfoSprite(sz, new Color(5, 217, 232));
            case 3: return PixelSprites.createGhostSprite(sz, new Color(255, 220, 240));
            case 4: return PixelSprites.createShieldSprite(sz, new Color(70, 160, 255));
            case 5: return PixelSprites.createBrickSprite(sz);
            case 6: return PixelSprites.createDiskSprite(sz);
            default: return PixelSprites.createBoltSprite(sz);
        }
    }
}
