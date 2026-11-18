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
        "01 INVADERS CPU", "02 SPACE BANKER", "03 HOPPER PAGING", "04 MARIO CACHE",
        "05 SNAKE SEGMENT", "06 CRANE RAM", "07 PAC-MAN DISK", "08 OVERCOOKED MULTI"
    };

    private final String[] tabIcons = {
        "👾", "🚀", "🐸", "🍄",
        "🐍", "🏗️", "🟡", "👨‍🍳"
    };

    private final String[] missionNames = {
        "MISSION 01 SPACE INVADERS // CPU SCHEDULING",
        "MISSION 02 SPACE SHOOT // BANKER'S ALGORITHM",
        "MISSION 03 FRAME HOPPER // PAGING TRANSLATION",
        "MISSION 04 SUPER MARIO // CACHE REPLACEMENT",
        "MISSION 05 HEAP SNAKE // MEMORY SEGMENTATION",
        "MISSION 06 CYBER CRANE ATTACK // DYNAMIC RAM ALLOCATION",
        "MISSION 07 PAC-MAN // DISK HEAD SCHEDULING",
        "MISSION 08 OVERCOOKED // MULTIPROGRAMMING DISPATCHER"
    };

    private int activeTab = 0;
    private final Component[] simPanels = new Component[8];

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

        simPanels[0] = new CPUSchedulingPanel();
        simPanels[1] = new BankersPanel();
        simPanels[2] = new PagingPanel();
        simPanels[3] = new PageReplacementPanel();
        simPanels[4] = new SegmentationPanel();
        simPanels[5] = new FragmentationPanel();
        simPanels[6] = new DiskSchedulingPanel();
        simPanels[7] = new MultiprogrammingPanel();

        for (int i = 0; i < 8; i++) {
            contentCards.add(simPanels[i], tabNames[i]);
        }

        add(contentCards, BorderLayout.CENTER);

        // 3. Bottom Status Bar
        add(createBottomBar(), BorderLayout.SOUTH);

        // ESC key returns to Archives
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESCAPE_BACK");
        getActionMap().put("ESCAPE_BACK", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isShowing()) return;
                parentFrame.showScreen(ArcadeOS.SCREEN_ARCHIVES);
            }
        });
    }

    private JPanel createArcadeHeader() {
        JPanel header = new JPanel(new BorderLayout(4, 0));
        header.setBackground(new Color(15, 10, 26));
        header.setPreferredSize(new Dimension(0, 44));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(65, 45, 95)),
                BorderFactory.createEmptyBorder(4, 4, 4, 6)
        ));

        // Left Section: Mascot + M // OS + 3 mini buttons
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        left.setOpaque(false);
        left.add(new JLabel(new ImageIcon(PixelSprites.createRobotSprite(22))));

        JLabel lblLogo = new JLabel("M // OS");
        lblLogo.setFont(new Font("Consolas", Font.BOLD, 11));
        lblLogo.setForeground(Color.WHITE);
        left.add(lblLogo);

        JButton btnSw = RetroTheme.createNavButton("[U]", "USER", RetroTheme.TEXT_WHITE);
        btnSw.setToolTipText("Switch Player");
        btnSw.addActionListener(e -> parentFrame.showScreen(ArcadeOS.SCREEN_LOGIN));

        JButton btnLb = RetroTheme.createNavButton("[#]", "RANKS", RetroTheme.ACCENT_YELLOW);
        btnLb.setToolTipText("Leaderboard");
        btnLb.addActionListener(e -> JOptionPane.showMessageDialog(this, "Leaderboard Active", "Scores", JOptionPane.INFORMATION_MESSAGE));

        JButton btnLo = RetroTheme.createNavButton("[X]", "EXIT", RetroTheme.ACCENT_PINK);
        btnLo.setToolTipText("Log Out");
        btnLo.addActionListener(e -> parentFrame.showScreen(ArcadeOS.SCREEN_LOGIN));

        left.add(btnSw); left.add(btnLb); left.add(btnLo);
        header.add(left, BorderLayout.WEST);

        // Center: 8 Arcade Module Cards (Game-Themed Cyber Cartridges)
        JPanel tabsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        tabsPanel.setOpaque(false);

        final String[][] tabMeta = {
            {"01", "CPU", "INVADERS"},
            {"02", "BANK", "SPACE"},
            {"03", "PAGE", "HOPPER"},
            {"04", "CACHE", "MARIO"},
            {"05", "SEGM", "SNAKE"},
            {"06", "RAM", "CRANE"},
            {"07", "DISK", "PAC-MAN"},
            {"08", "MULTI", "OVERCOOKED"}
        };

        final Color[] tabAccents = {
            new Color(0, 225, 255),   // 01 Cyan
            new Color(255, 160, 50),  // 02 Amber Gold
            new Color(0, 240, 200),   // 03 Neon Mint
            new Color(255, 80, 80),   // 04 Mario Red
            new Color(70, 230, 120),  // 05 Snake Green
            new Color(195, 115, 255), // 06 Cyber Violet
            new Color(255, 220, 0),   // 07 Pacman Yellow
            new Color(255, 65, 150)   // 08 Chef Hot Pink
        };

        final Image[] tabSprites = {
            PixelSprites.createAlienSprite(15, tabAccents[0]),
            PixelSprites.createSpaceShootShip(15, tabAccents[1]),
            PixelSprites.createFrogSprite(15, tabAccents[2]),
            PixelSprites.createMarioSprite(15),
            PixelSprites.createShieldSprite(15, tabAccents[4]),
            PixelSprites.createCraneSprite(15, tabAccents[5]),
            PixelSprites.createPacman(15, 0.4f, true),
            PixelSprites.createChefSprite(15, false)
        };

        tabButtons = new JButton[tabNames.length];
        for (int i = 0; i < tabNames.length; i++) {
            final int idx = i;
            ArcadeTabButton btn = new ArcadeTabButton(
                idx,
                tabMeta[i][0],
                tabMeta[i][1],
                tabMeta[i][2],
                tabAccents[i],
                tabSprites[i],
                this
            );
            tabButtons[i] = btn;
            tabsPanel.add(btn);
        }
        header.add(tabsPanel, BorderLayout.CENTER);

        // Right Section: XP Score Counter (Robust font, no missing coin emoji)
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 1));
        right.setOpaque(false);

        lblXpScore = new JLabel("● XP " + String.format("%06d", AppState.score + 125));
        lblXpScore.setFont(new Font("Consolas", Font.BOLD, 11));
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
        btnStageSelect.setFont(new Font("Consolas", Font.BOLD, 11));
        btnStageSelect.setForeground(RetroTheme.ACCENT_CYAN);
        btnStageSelect.setOpaque(false);
        btnStageSelect.setContentAreaFilled(false);
        btnStageSelect.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        btnStageSelect.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnStageSelect.addActionListener(e -> parentFrame.showScreen(ArcadeOS.SCREEN_ARCHIVES));
        left.add(btnStageSelect);

        JLabel sep = new JLabel("|");
        sep.setFont(new Font("Consolas", Font.PLAIN, 12));
        sep.setForeground(RetroTheme.BORDER_COLOR);
        left.add(sep);

        lblBreadcrumb = new JLabel(missionNames[0]);
        lblBreadcrumb.setFont(new Font("Consolas", Font.BOLD, 11));
        lblBreadcrumb.setForeground(Color.WHITE);
        left.add(lblBreadcrumb);

        JButton btnOpenManual = new JButton("DOCS // MANUAL");
        btnOpenManual.setFont(new Font("Consolas", Font.BOLD, 11));
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

    public int getActiveTab() {
        return activeTab;
    }

    public Component getPanel(int index) {
        if (index >= 0 && index < simPanels.length) return simPanels[index];
        return null;
    }

    public void loadManualExample(int manualIndex) {
        selectTab(manualIndex);
        if (manualIndex >= 0 && manualIndex < simPanels.length && simPanels[manualIndex] != null) {
            Component p = simPanels[manualIndex];
            if (p instanceof PagingPanel) ((PagingPanel) p).loadManualExample();
            else if (p instanceof SegmentationPanel) ((SegmentationPanel) p).loadManualExample();
        }
    }

    public void selectTab(int index) {
        if (index < 0 || index >= tabNames.length) return;

        // Pause other running panels if any
        for (int i = 0; i < 8; i++) {
            if (i != index && simPanels[i] != null) {
                if (simPanels[i] instanceof CPUSchedulingPanel) ((CPUSchedulingPanel) simPanels[i]).pauseSimulation();
                else if (simPanels[i] instanceof PageReplacementPanel) ((PageReplacementPanel) simPanels[i]).pauseSimulation();
                else if (simPanels[i] instanceof DiskSchedulingPanel) ((DiskSchedulingPanel) simPanels[i]).pauseSimulation();
                else if (simPanels[i] instanceof MultiprogrammingPanel) ((MultiprogrammingPanel) simPanels[i]).pauseSimulation();
                else if (simPanels[i] instanceof PagingPanel) ((PagingPanel) simPanels[i]).pauseSimulation();
                else if (simPanels[i] instanceof SegmentationPanel) ((SegmentationPanel) simPanels[i]).pauseSimulation();
            }
        }

        activeTab = index;

        // Repaint all arcade tab cards
        for (JButton b : tabButtons) {
            if (b != null) b.repaint();
        }

        // Update breadcrumb
        lblBreadcrumb.setText(missionNames[index]);

        // Flip CardLayout
        cardLayout.show(contentCards, tabNames[index]);
        repaint();
    }

    // =========================================================================
    // ARCADE TAB CARD BUTTON (Tactile Cyber Cartridge Micro-Card)
    // =========================================================================
    private static class ArcadeTabButton extends JButton {
        private final int tabIndex;
        private final String numStr;
        private final String topicStr;
        private final String gameStr;
        private final Color accentColor;
        private final Image iconSprite;
        private boolean isHovered = false;
        private final SimulatorContainerPanel container;

        public ArcadeTabButton(int index, String num, String topic, String game, Color accent, Image sprite, SimulatorContainerPanel container) {
            super();
            this.tabIndex = index;
            this.numStr = num;
            this.topicStr = topic;
            this.gameStr = game;
            this.accentColor = accent;
            this.iconSprite = sprite;
            this.container = container;

            setPreferredSize(new Dimension(93, 33));
            setMinimumSize(new Dimension(88, 33));
            setMaximumSize(new Dimension(96, 33));
            setFocusPainted(false);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder());
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Mission " + num + ": " + game + " (" + topic + ")");

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });

            addActionListener(e -> container.selectTab(tabIndex));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            boolean isActive = (container.getActiveTab() == tabIndex);

            // 1. Background fill
            if (isActive) {
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(52, 22, 74),
                        0, h, new Color(26, 12, 42)
                );
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 7, 7);

                // Top ambient glow
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 40));
                g2.fillRoundRect(2, 2, w - 4, (h / 2) - 1, 5, 5);
            } else if (isHovered) {
                g2.setColor(new Color(40, 28, 62));
                g2.fillRoundRect(0, 0, w, h, 7, 7);
            } else {
                g2.setColor(new Color(20, 14, 34));
                g2.fillRoundRect(0, 0, w, h, 7, 7);
            }

            // 2. Bevel / Border
            if (isActive) {
                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 7, 7);

                // Hot bottom neon bar with rounded ends
                g2.setColor(accentColor);
                g2.fillRect(3, h - 3, w - 6, 3);

                // Tiny glowing active LED dot at top right
                g2.setColor(accentColor);
                g2.fillOval(w - 7, 4, 3, 3);
            } else if (isHovered) {
                g2.setColor(new Color(100, 75, 145));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 7, 7);
            } else {
                g2.setColor(new Color(48, 35, 72));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 7, 7);
            }

            // 3. Pixel Sprite Icon
            if (iconSprite != null) {
                int iconSz = 14;
                int iconY = (h - iconSz) / 2;
                g2.drawImage(iconSprite, 5, iconY, iconSz, iconSz, null);
            }

            // 4. Two-Tier Typography
            // Line 1: Tag [num + topic]
            g2.setFont(new Font("Consolas", Font.BOLD, 8));
            g2.setColor(isActive ? accentColor : (isHovered ? accentColor : new Color(135, 120, 165)));
            g2.drawString(numStr, 22, 11);

            g2.setFont(new Font("Consolas", Font.PLAIN, 7));
            g2.setColor(isActive ? new Color(220, 220, 240) : (isHovered ? new Color(200, 200, 220) : new Color(115, 110, 145)));
            g2.drawString(topicStr, 33, 11);

            // Line 2: Game Name
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            if (isActive) {
                g2.setColor(Color.WHITE);
            } else if (isHovered) {
                g2.setColor(new Color(245, 245, 255));
            } else {
                g2.setColor(new Color(165, 160, 190));
            }
            g2.drawString(gameStr, 22, 24);

            g2.dispose();
        }
    }
}

