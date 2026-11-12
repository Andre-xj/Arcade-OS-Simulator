package arcadeos;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

/**
 * Enhanced Interactive OS Archive Manual Reader Dialog.
 * Provides 5 comprehensive tabs:
 * 1. Theory & Concepts
 * 2. Mathematical Formulas
 * 3. Algorithm & Pseudocode
 * 4. Step-by-Step Worked Trace & Gantt
 * 5. Exam Cheat-Sheet & Pitfalls
 *
 * Includes Interactive Font Size Controls [A-] [A+], One-Click Clipboard Export,
 * Real-Time Keyword Search Highlighter, and direct simulator/quiz launcher.
 */
public class ArchiveManualDialog extends JDialog {
    private ArcadeOS parentFrame;
    private ArchivesPanel parentPanel;
    private int manualIndex;
    private ArchiveManualData.ManualEntry manual;

    private int currentFontSize = 12;
    private JTextArea[] tabTextAreas;
    private JTabbedPane tabbedPane;
    private JTextField txtSearch;
    private JLabel lblStatusToast;

    public ArchiveManualDialog(ArcadeOS parentFrame, ArchivesPanel parentPanel, int index) {
        super(parentFrame, "OPERATING SYSTEM ARCHIVE SPECIFICATION MANUAL", true);
        this.parentFrame = parentFrame;
        this.parentPanel = parentPanel;
        this.manualIndex = index;
        this.manual = ArchiveManualData.getManual(index);

        setSize(940, 720);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(parentFrame);
        getContentPane().setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(0, 10));

        // 1. Header Banner
        add(createHeaderBanner(), BorderLayout.NORTH);

        // 2. Center 5-Tab Knowledge Base with Reader Controls
        add(createCenterContent(), BorderLayout.CENTER);

        // 3. Action Footer Controls
        add(createFooterControls(), BorderLayout.SOUTH);
    }

    private JPanel createHeaderBanner() {
        JPanel header = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(24, 16, 46));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(RetroTheme.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // Left Icon
        JLabel iconLbl = new JLabel(new ImageIcon(getSpriteForManual(manualIndex, 48)));
        header.add(iconLbl, BorderLayout.WEST);

        // Center Details
        JPanel centerDetails = new JPanel();
        centerDetails.setOpaque(false);
        centerDetails.setLayout(new BoxLayout(centerDetails, BoxLayout.Y_AXIS));

        JPanel topTagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topTagRow.setOpaque(false);

        JLabel tagFile = new JLabel("ARCHIVE FILE " + manual.id + " // SPECIFICATION");
        tagFile.setFont(new Font("Consolas", Font.BOLD, 10));
        tagFile.setForeground(RetroTheme.ACCENT_PINK);
        topTagRow.add(tagFile);

        JLabel compLbl = new JLabel("COMPLEXITY: " + manual.complexity);
        compLbl.setFont(new Font("Consolas", Font.PLAIN, 10));
        compLbl.setForeground(RetroTheme.ACCENT_YELLOW);
        topTagRow.add(compLbl);

        centerDetails.add(topTagRow);
        centerDetails.add(Box.createVerticalStrut(4));

        JLabel titleLbl = new JLabel(manual.title);
        titleLbl.setFont(new Font("Consolas", Font.BOLD, 18));
        titleLbl.setForeground(Color.WHITE);
        centerDetails.add(titleLbl);

        JLabel subLbl = new JLabel(manual.subtitle);
        subLbl.setFont(new Font("Consolas", Font.PLAIN, 11));
        subLbl.setForeground(RetroTheme.ACCENT_CYAN);
        centerDetails.add(subLbl);

        header.add(centerDetails, BorderLayout.CENTER);

        // Right Reader Tools: Font Sizing & Quick Copy Notes
        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        rightTools.setOpaque(false);

        JButton btnFontMinus = createMiniToolButton("A-", "Decrease font size");
        btnFontMinus.addActionListener(e -> adjustFontSize(-1));

        JButton btnFontPlus = createMiniToolButton("A+", "Increase font size");
        btnFontPlus.addActionListener(e -> adjustFontSize(1));

        JButton btnCopy = createMiniToolButton("📋 COPY NOTES", "Copy current tab text to clipboard");
        btnCopy.addActionListener(e -> copyCurrentNotes());

        rightTools.add(btnFontMinus);
        rightTools.add(btnFontPlus);
        rightTools.add(btnCopy);

        header.add(rightTools, BorderLayout.EAST);
        return header;
    }

    private JButton createMiniToolButton(String text, String tip) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Consolas", Font.BOLD, 10));
        btn.setForeground(new Color(220, 220, 240));
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(75, 55, 120), 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        btn.setToolTipText(tip);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btn.setForeground(RetroTheme.ACCENT_CYAN); }
            @Override
            public void mouseExited(MouseEvent e) { btn.setForeground(new Color(220, 220, 240)); }
        });
        return btn;
    }

    private JComponent createCenterContent() {
        JPanel centerPanel = new JPanel(new BorderLayout(0, 6));
        centerPanel.setOpaque(false);

        // 1. Top Sub-Bar: Search / Keyword Filter
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setOpaque(false);
        searchBar.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));

        JPanel leftSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftSearch.setOpaque(false);
        JLabel lblFind = new JLabel("🔍 FIND KEYWORD:");
        lblFind.setFont(new Font("Consolas", Font.BOLD, 10));
        lblFind.setForeground(RetroTheme.ACCENT_CYAN);
        leftSearch.add(lblFind);

        txtSearch = new JTextField(16);
        txtSearch.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtSearch.setBackground(RetroTheme.INPUT_BG);
        txtSearch.setForeground(Color.WHITE);
        txtSearch.setCaretColor(RetroTheme.ACCENT_CYAN);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)
        ));
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                highlightKeyword(txtSearch.getText().trim());
            }
        });
        leftSearch.add(txtSearch);

        lblStatusToast = new JLabel("");
        lblStatusToast.setFont(new Font("Consolas", Font.BOLD, 10));
        lblStatusToast.setForeground(RetroTheme.ACCENT_GREEN);
        leftSearch.add(lblStatusToast);
        searchBar.add(leftSearch, BorderLayout.WEST);

        centerPanel.add(searchBar, BorderLayout.NORTH);

        // 2. 5 Comprehensive Tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Consolas", Font.BOLD, 11));
        tabbedPane.setBackground(RetroTheme.BG_DARK);
        tabbedPane.setForeground(Color.WHITE);

        tabTextAreas = new JTextArea[5];

        tabbedPane.addTab("📘 1. THEORY & ARCHITECTURE",
                createScrollableText(0, manual.theory, RetroTheme.ACCENT_CYAN));

        tabbedPane.addTab("📐 2. FORMULAS & METRICS",
                createScrollableText(1, manual.formulas, RetroTheme.ACCENT_YELLOW));

        tabbedPane.addTab("💻 3. ALGORITHM & PSEUDOCODE",
                createScrollableText(2, manual.pseudocode, RetroTheme.ACCENT_PINK));

        tabbedPane.addTab("🔍 4. WORKED TRACE & GANTT",
                createScrollableText(3, manual.workedExample, RetroTheme.ACCENT_GREEN));

        tabbedPane.addTab("💡 5. CHEAT SHEET & PITFALLS",
                createScrollableText(4, manual.cheatSheet, new Color(255, 140, 40)));

        centerPanel.add(tabbedPane, BorderLayout.CENTER);
        return centerPanel;
    }

    private JScrollPane createScrollableText(int tabIdx, String text, Color accent) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, currentFontSize));
        area.setBackground(new Color(20, 15, 38));
        area.setForeground(new Color(230, 230, 245));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        tabTextAreas[tabIdx] = area;

        JScrollPane sp = new JScrollPane(area);
        sp.getViewport().setBackground(new Color(20, 15, 38));
        sp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 2, 4, 2),
                BorderFactory.createLineBorder(new Color(60, 45, 90), 1)
        ));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private void adjustFontSize(int delta) {
        currentFontSize = Math.max(9, Math.min(20, currentFontSize + delta));
        for (JTextArea a : tabTextAreas) {
            if (a != null) {
                a.setFont(new Font("Consolas", Font.PLAIN, currentFontSize));
            }
        }
        showToast("FONT SIZE: " + currentFontSize + " PT");
    }

    private void copyCurrentNotes() {
        int sel = tabbedPane.getSelectedIndex();
        if (sel >= 0 && sel < tabTextAreas.length && tabTextAreas[sel] != null) {
            String text = tabTextAreas[sel].getText();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            showToast("✔ NOTES COPIED TO CLIPBOARD!");
        }
    }

    private void highlightKeyword(String kw) {
        int sel = tabbedPane.getSelectedIndex();
        if (sel < 0 || tabTextAreas[sel] == null) return;

        JTextArea area = tabTextAreas[sel];
        Highlighter hl = area.getHighlighter();
        hl.removeAllHighlights();

        if (kw.isEmpty()) return;

        String content = area.getText().toLowerCase();
        String query = kw.toLowerCase();
        int idx = 0;
        int count = 0;
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 42, 141, 120));

        while ((idx = content.indexOf(query, idx)) >= 0) {
            try {
                hl.addHighlight(idx, idx + query.length(), painter);
                count++;
                idx += query.length();
            } catch (BadLocationException ignored) {
                break;
            }
        }
        if (count > 0) {
            showToast("FOUND " + count + " MATCHES FOR: '" + kw + "'");
        } else {
            showToast("NO MATCHES FOR: '" + kw + "'");
        }
    }

    private void showToast(String msg) {
        if (lblStatusToast != null) {
            lblStatusToast.setText(msg);
            Timer t = new Timer(3000, e -> lblStatusToast.setText(""));
            t.setRepeats(false);
            t.start();
        }
    }

    private JPanel createFooterControls() {
        JPanel footer = new JPanel(new BorderLayout(15, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        // Left Status
        boolean isRead = (AppState.filesRead[manualIndex] == 1);
        JLabel statusLbl = new JLabel(isRead ? "STATUS: ARCHIVE READ ✔ (+50 XP EARNED)" : "STATUS: UNREAD (STUDY TO EARN +50 XP)");
        statusLbl.setFont(new Font("Consolas", Font.BOLD, 11));
        statusLbl.setForeground(isRead ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_PINK);
        footer.add(statusLbl, BorderLayout.WEST);

        // Right Actions
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.setOpaque(false);

        // 1. Launch Simulator Button
        JButton btnSim = RetroTheme.createRetroButton("🚀 TEST IN SIMULATOR", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnSim.addActionListener(e -> {
            markManualRead();
            dispose();
            parentFrame.openSimulatorTab(manual.simulatorTabIndex);
        });
        rightActions.add(btnSim);

        // 2. Open Quiz Arena Button
        JButton btnQuiz = RetroTheme.createRetroButton("❓ QUIZ ARENA", RetroTheme.BTN_DARK, RetroTheme.ACCENT_YELLOW);
        btnQuiz.addActionListener(e -> {
            markManualRead();
            dispose();
            parentFrame.showScreen(ArcadeOS.SCREEN_QUIZ);
        });
        rightActions.add(btnQuiz);

        // 3. Mark Read & Close
        JButton btnMarkRead = RetroTheme.createRetroButton("MARK AS COMPLETED", RetroTheme.BTN_DARK, RetroTheme.ACCENT_GREEN);
        btnMarkRead.addActionListener(e -> {
            markManualRead();
            dispose();
        });
        rightActions.add(btnMarkRead);

        // 4. Simple Close Button
        JButton btnClose = RetroTheme.createRetroButton("CLOSE", RetroTheme.BTN_DARK, RetroTheme.TEXT_DIM);
        btnClose.addActionListener(e -> dispose());
        rightActions.add(btnClose);

        footer.add(rightActions, BorderLayout.EAST);
        return footer;
    }

    private void markManualRead() {
        if (AppState.filesRead[manualIndex] == 0) {
            AppState.markRead(manualIndex);
            AppState.addScore(50); // +50 XP for studying manual
            if (parentPanel != null) {
                parentPanel.updateCounters();
            }
        }
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
