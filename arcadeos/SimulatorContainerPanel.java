package arcadeos;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Screen 4: SIMULATOR WORKBENCH CONTAINER
 * Hosts the top 8 Arcade Tabs, breadcrumb navigation, and renders the selected panel.
 * Exactly matches Screenshot 4 header and navigation structure.
 */
public class SimulatorContainerPanel extends JPanel {
    private ArcadeOS parentFrame;
    private JPanel contentCards;
    private CardLayout cardLayout;
    private JButton[] tabButtons;
    private JLabel lblBreadcrumb;
    private JLabel lblXpScore;

    private final String[] tabNames = {
        "01 CPU", "02 BANKER", "03 PAGING", "04 CACHE",
        "05 SEGMENT", "06 RAM", "07 DISK", "08 MULTIPROG"
    };

    private final String[] tabIcons = {
        "🕹️", "🪙", "💾", "🔄",
        "🗺️", "🧱", "💽", "⚡"
    };

    private final String[] missionNames = {
        "MISSION 01 CPU SCHEDULING",
        "MISSION 02 BANKER'S ALGORITHM",
        "MISSION 03 PAGING TRANSLATION",
        "MISSION 04 CACHE PAGE SWAP",
        "MISSION 05 MEMORY SEGMENTATION",
        "MISSION 06 RAM ALLOCATION & FRAGMENTATION",
        "MISSION 07 DISK HEAD SCHEDULING",
        "MISSION 08 MULTIPROGRAMMING DISPATCHER"
    };

    private int activeTab = 0;

    public SimulatorContainerPanel(ArcadeOS parent) {
        this.parentFrame = parent;
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout());

        // 1. Top Section: Arcade Nav Bar + Breadcrumbs
        JPanel topContainer = new JPanel();
        topContainer.setOpaque(false);
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));

        topContainer.add(createArcadeHeader());
        topContainer.add(createBreadcrumbBar());

        add(topContainer, BorderLayout.NORTH);

        // 2. Center: CardLayout with all 8 Panels
        cardLayout = new CardLayout();
        contentCards = new JPanel(cardLayout);
        contentCards.setOpaque(false);

        contentCards.add(new CPUSchedulingPanel(), tabNames[0]);
        contentCards.add(new BankersPanel(), tabNames[1]);
        contentCards.add(new PagingPanel(), tabNames[2]);
        contentCards.add(new PageReplacementPanel(), tabNames[3]);
        contentCards.add(new SegmentationPanel(), tabNames[4]);
        contentCards.add(new FragmentationPanel(), tabNames[5]);
        contentCards.add(new DiskSchedulingPanel(), tabNames[6]);
        contentCards.add(new MultiprogrammingPanel(), tabNames[7]);

        add(contentCards, BorderLayout.CENTER);

        // 3. Bottom Status Bar
        add(createBottomBar(), BorderLayout.SOUTH);

        // ESC key returns to Archives
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESCAPE_BACK");
        getActionMap().put("ESCAPE_BACK", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parentFrame.showScreen(ArcadeOS.SCREEN_ARCHIVES);
            }
        });
    }

    private JPanel createArcadeHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(RetroTheme.PANEL_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, RetroTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        // Left Section: Mascot + M // OS + 3 mini buttons
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        left.setOpaque(false);
        left.add(new JLabel(new ImageIcon(PixelSprites.createRobotSprite(28))));

        JLabel lblLogo = new JLabel("M // OS");
        lblLogo.setFont(new Font("Consolas", Font.BOLD, 13));
        lblLogo.setForeground(Color.WHITE);
        left.add(lblLogo);

        JButton btnSw = RetroTheme.createNavButton("👤", "", RetroTheme.TEXT_WHITE);
        btnSw.setToolTipText("Switch Player");
        btnSw.addActionListener(e -> parentFrame.showScreen(ArcadeOS.SCREEN_LOGIN));

        JButton btnLb = RetroTheme.createNavButton("🏆", "", RetroTheme.ACCENT_YELLOW);
        btnLb.setToolTipText("Leaderboard");
        btnLb.addActionListener(e -> JOptionPane.showMessageDialog(this, "Leaderboard Active", "Scores", JOptionPane.INFORMATION_MESSAGE));

        JButton btnLo = RetroTheme.createNavButton("⏻", "", RetroTheme.ACCENT_PINK);
        btnLo.setToolTipText("Log Out");
        btnLo.addActionListener(e -> parentFrame.showScreen(ArcadeOS.SCREEN_LOGIN));

        left.add(btnSw); left.add(btnLb); left.add(btnLo);
        header.add(left, BorderLayout.WEST);

        // Center: 8 Arcade Module Tabs
        JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        tabsPanel.setOpaque(false);

        tabButtons = new JButton[tabNames.length];
        for (int i = 0; i < tabNames.length; i++) {
            final int idx = i;
            JButton btn = new JButton(tabIcons[i] + " " + tabNames[i]) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (activeTab == idx) {
                        g2.setColor(new Color(50, 24, 75));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                        // Bottom pink underline
                        g2.setColor(RetroTheme.ACCENT_PINK);
                        g2.fillRect(4, getHeight() - 3, getWidth() - 8, 3);
                    } else {
                        g2.setColor(RetroTheme.BTN_DARK);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(new Font("Consolas", Font.BOLD, 10));
            btn.setForeground(activeTab == i ? RetroTheme.ACCENT_CYAN : RetroTheme.TEXT_DIM);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> selectTab(idx));

            tabButtons[i] = btn;
            tabsPanel.add(btn);
        }
        header.add(tabsPanel, BorderLayout.CENTER);

        // Right Section: XP Score Counter
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        right.setOpaque(false);

        JLabel coinIcon = new JLabel("🪙");
        coinIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        right.add(coinIcon);

        lblXpScore = new JLabel("XP SCORE " + String.format("%06d", AppState.score + 125));
        lblXpScore.setFont(new Font("Consolas", Font.BOLD, 12));
        lblXpScore.setForeground(RetroTheme.ACCENT_YELLOW);
        right.add(lblXpScore);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createBreadcrumbBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

        // Left breadcrumb
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JButton btnStageSelect = new JButton("< STAGE SELECT");
        btnStageSelect.setFont(new Font("Consolas", Font.BOLD, 10));
        btnStageSelect.setForeground(RetroTheme.ACCENT_CYAN);
        btnStageSelect.setOpaque(false);
        btnStageSelect.setContentAreaFilled(false);
        btnStageSelect.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        btnStageSelect.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnStageSelect.addActionListener(e -> parentFrame.showScreen(ArcadeOS.SCREEN_ARCHIVES));
        left.add(btnStageSelect);

        JLabel sep = new JLabel("|");
        sep.setFont(new Font("Consolas", Font.PLAIN, 11));
        sep.setForeground(RetroTheme.BORDER_COLOR);
        left.add(sep);

        lblBreadcrumb = new JLabel(missionNames[0]);
        lblBreadcrumb.setFont(new Font("Consolas", Font.BOLD, 10));
        lblBreadcrumb.setForeground(Color.WHITE);
        left.add(lblBreadcrumb);

        JButton btnOpenManual = new JButton("📖 MANUAL");
        btnOpenManual.setFont(new Font("Consolas", Font.BOLD, 10));
        btnOpenManual.setForeground(RetroTheme.ACCENT_YELLOW);
        btnOpenManual.setOpaque(false);
        btnOpenManual.setContentAreaFilled(false);
        btnOpenManual.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 211, 25, 120), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        btnOpenManual.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnOpenManual.addActionListener(e -> {
            ArchiveManualDialog dialog = new ArchiveManualDialog(parentFrame, null, activeTab);
            dialog.setVisible(true);
        });
        left.add(btnOpenManual);

        bar.add(left, BorderLayout.WEST);

        // Right pill: [ GUEST MODE // READ ONLY ]
        JLabel pill = new JLabel(AppState.isGuest ? " [ GUEST MODE // READ ONLY ] " : " [ ARCHIVIST SESSION // ACTIVE ] ");
        pill.setFont(new Font("Consolas", Font.BOLD, 9));
        pill.setForeground(AppState.isGuest ? new Color(200, 100, 150) : RetroTheme.ACCENT_GREEN);
        pill.setBorder(BorderFactory.createLineBorder(AppState.isGuest ? RetroTheme.BORDER_COLOR : RetroTheme.ACCENT_GREEN, 1));
        bar.add(pill, BorderLayout.EAST);

        return bar;
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, RetroTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 16, 4, 16)
        ));

        JLabel lblVer = new JLabel("VERSION v1.0.4");
        lblVer.setFont(new Font("Consolas", Font.PLAIN, 9));
        lblVer.setForeground(RetroTheme.TEXT_DIM);
        bar.add(lblVer, BorderLayout.WEST);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        center.setOpaque(false);
        JLabel lblStable = new JLabel("● SYSTEM STABLE");
        lblStable.setFont(new Font("Consolas", Font.BOLD, 9));
        lblStable.setForeground(RetroTheme.ACCENT_GREEN);
        center.add(lblStable);

        JLabel lblEsc = new JLabel("PRESS ESC TO RETURN");
        lblEsc.setFont(new Font("Consolas", Font.PLAIN, 9));
        lblEsc.setForeground(RetroTheme.TEXT_DIM);
        center.add(lblEsc);
        bar.add(center, BorderLayout.CENTER);

        String userTag = String.format("> CURRENT USER: %s [SCORE: %s] [GUEST MODE]",
                AppState.playerName, AppState.getScoreFormatted());
        JLabel lblRight = new JLabel(userTag);
        lblRight.setFont(new Font("Consolas", Font.BOLD, 9));
        lblRight.setForeground(RetroTheme.ACCENT_PINK);
        lblRight.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_PINK, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        bar.add(lblRight, BorderLayout.EAST);

        return bar;
    }

    public void selectTab(int index) {
        if (index < 0 || index >= tabNames.length) return;
        activeTab = index;

        // Update button styles
        for (int i = 0; i < tabButtons.length; i++) {
            tabButtons[i].setForeground(i == activeTab ? RetroTheme.ACCENT_CYAN : RetroTheme.TEXT_DIM);
        }

        // Update breadcrumb
        lblBreadcrumb.setText(missionNames[index]);

        // Flip CardLayout
        cardLayout.show(contentCards, tabNames[index]);
        repaint();
    }
}

