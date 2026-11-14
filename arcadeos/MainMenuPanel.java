package arcadeos;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Screen 2: MAIN MENU / AAA GAME-STYLE INTERFACE
 * Modeled after Homefront: The Revolution main menu:
 * - Full-bleed background wallpaper (bg.jpg) covering 100% of the panel
 * - Heavy dark gradient vignette on the left ~45% for crystal clear text
 * - Clean vertical text-only menu items (no cards, no boxes)
 * - Subtle glowing pink indicator bar on the active selection
 * - Bottom bar: PROFILE on left, keyboard prompts on right
 * - Top bar: minimal ghost buttons in the upper-right
 */
public class MainMenuPanel extends JPanel {
    private ArcadeOS parentFrame;
    private int selectedMenuIndex = 0;
    private Timer pulseTimer;
    private float pulseAlpha = 1.0f;
    private boolean pulseUp = false;
    private Image bgImage;

    // Scrolling ticker offset
    private float tickerOffset = 0;
    private Timer tickerTimer;

    private final String[] menuTitles = {
        "SYSTEM ARCHIVES",
        "QUIZ MODE",
        "ABOUT SYSTEM",
        "EXIT"
    };

    private final String[] menuSubtitles = {
        "8 CORE SIMULATOR MANUALS // CPU, MEMORY, DISK",
        "CHALLENGE TASKS & KERNEL QUESTIONS",
        "DEVELOPER CREDITS & SYSTEM ARCHITECTURE",
        "TERMINATE CURRENT KERNEL SESSION"
    };

    private JPanel menuListPanel;

    public MainMenuPanel(ArcadeOS parent) {
        this.parentFrame = parent;
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout());

        // Load background image
        try {
            File f = new File("image/bg.jpg");
            if (!f.exists()) {
                f = new File(System.getProperty("user.dir"), "image/bg.jpg");
            }
            if (f.exists()) {
                bgImage = ImageIO.read(f);
            }
        } catch (Exception e) {
            System.err.println("Could not load image/bg.jpg: " + e.getMessage());
        }

        // Keyboard navigation
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
                    selectedMenuIndex = (selectedMenuIndex - 1 + menuTitles.length) % menuTitles.length;
                    updateMenuStyles();
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
                    selectedMenuIndex = (selectedMenuIndex + 1) % menuTitles.length;
                    updateMenuStyles();
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    triggerMenuAction(selectedMenuIndex);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    confirmExit();
                }
            }
        });

        // Top mini toolbar (upper-right ghost buttons)
        add(createTopBar(), BorderLayout.NORTH);

        // Center: Left-aligned game menu list
        add(createMenuArea(), BorderLayout.CENTER);

        // Bottom Bar: Profile on left, Controller prompts on right
        add(createBottomBar(), BorderLayout.SOUTH);

        // Subtle pulse timer for selected item glow
        pulseTimer = new Timer(50, e -> {
            if (pulseUp) {
                pulseAlpha += 0.05f;
                if (pulseAlpha >= 1.0f) { pulseAlpha = 1.0f; pulseUp = false; }
            } else {
                pulseAlpha -= 0.05f;
                if (pulseAlpha <= 0.4f) { pulseAlpha = 0.4f; pulseUp = true; }
            }
            repaint();
        });
        pulseTimer.start();

        // Scrolling RSS-style ticker
        tickerTimer = new Timer(40, e -> {
            tickerOffset -= 1.2f;
            if (tickerOffset < -900) tickerOffset = 0;
            repaint();
        });
        tickerTimer.start();
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        bar.setOpaque(false);

        JButton btnSwitch = createMinimalTopButton("👤 SWITCH PLAYER");
        btnSwitch.addActionListener(e -> parentFrame.showScreen(ArcadeOS.SCREEN_LOGIN));

        JButton btnLeader = createMinimalTopButton("🏆 LEADERBOARD");
        btnLeader.addActionListener(e -> showLeaderboard());

        JButton btnLogout = createMinimalTopButton("⏻ LOG OUT");
        btnLogout.addActionListener(e -> parentFrame.showScreen(ArcadeOS.SCREEN_LOGIN));

        bar.add(btnSwitch);
        bar.add(btnLeader);
        bar.add(btnLogout);
        return bar;
    }

    private JButton createMinimalTopButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Consolas", Font.BOLD, 10));
        btn.setForeground(new Color(180, 180, 200, 180));
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 35), 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(RetroTheme.ACCENT_CYAN);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(5, 217, 232, 120), 1),
                        BorderFactory.createEmptyBorder(5, 12, 5, 12)
                ));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(new Color(180, 180, 200, 180));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 255, 255, 35), 1),
                        BorderFactory.createEmptyBorder(5, 12, 5, 12)
                ));
            }
        });
        return btn;
    }

    /** Center Game Menu Area: Big Title + Left-Aligned Direct Text Menu */
    private JPanel createMenuArea() {
        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBorder(BorderFactory.createEmptyBorder(20, 70, 20, 40));

        // Push content down a bit from top
        container.add(Box.createVerticalGlue());

        // 1. Small red/pink sub-tag (like Homefront's Korean text)
        JLabel subTag = new JLabel("메인 메뉴 // ARCADE EDITION");
        subTag.setFont(new Font("Consolas", Font.BOLD, 11));
        subTag.setForeground(RetroTheme.ACCENT_PINK);
        subTag.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(subTag);

        container.add(Box.createVerticalStrut(2));

        // 2. Huge Game Title: MINI // OS
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel lblMini = new JLabel("MINI ");
        lblMini.setFont(new Font("Consolas", Font.BOLD, 60));
        lblMini.setForeground(Color.WHITE);

        JLabel lblSlash = new JLabel("// ");
        lblSlash.setFont(new Font("Consolas", Font.BOLD, 60));
        lblSlash.setForeground(RetroTheme.ACCENT_PINK);

        JLabel lblOs = new JLabel("OS");
        lblOs.setFont(new Font("Consolas", Font.BOLD, 60));
        lblOs.setForeground(Color.WHITE);

        titleRow.add(lblMini);
        titleRow.add(lblSlash);
        titleRow.add(lblOs);
        container.add(titleRow);

        container.add(Box.createVerticalStrut(2));

        // 3. Tagline
        JLabel lblTag = new JLabel("KAWAII 8-BIT OPERATING SYSTEM SIMULATOR");
        lblTag.setFont(new Font("Consolas", Font.BOLD, 11));
        lblTag.setForeground(RetroTheme.ACCENT_CYAN);
        lblTag.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(lblTag);

        container.add(Box.createVerticalStrut(40));

        // 4. Vertical Game Text Menu Items (clean, pure text like Homefront)
        menuListPanel = new JPanel();
        menuListPanel.setOpaque(false);
        menuListPanel.setLayout(new BoxLayout(menuListPanel, BoxLayout.Y_AXIS));
        menuListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (int i = 0; i < menuTitles.length; i++) {
            final int idx = i;
            JPanel itemRow = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (selectedMenuIndex == idx) {
                        // Pink neon indicator bar on left (like Homefront's yellow highlight)
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int barAlpha = (int)(pulseAlpha * 255);
                        g2.setColor(new Color(255, 42, 141, barAlpha));
                        g2.fillRect(0, 4, 3, getHeight() - 8);

                        // Subtle horizontal glow behind selected item
                        GradientPaint glowH = new GradientPaint(
                            0, 0, new Color(255, 42, 141, 25),
                            280, 0, new Color(255, 42, 141, 0)
                        );
                        g2.setPaint(glowH);
                        g2.fillRect(0, 0, 280, getHeight());
                        g2.dispose();
                    }
                }
            };
            itemRow.setOpaque(false);
            itemRow.setLayout(new BoxLayout(itemRow, BoxLayout.Y_AXIS));
            itemRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            itemRow.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            itemRow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            itemRow.setMaximumSize(new Dimension(500, 60));

            // Main Menu Item Title
            boolean isSel = (selectedMenuIndex == idx);
            JLabel lblItem = new JLabel(menuTitles[i]);
            lblItem.setFont(new Font("Consolas", Font.BOLD, isSel ? 24 : 22));
            lblItem.setForeground(isSel ? RetroTheme.ACCENT_PINK : new Color(220, 220, 235));
            lblItem.setName("menu-title-" + i);
            itemRow.add(lblItem);

            // Subtitle hint (only visible on selected)
            JLabel lblSub = new JLabel(menuSubtitles[i]);
            lblSub.setFont(new Font("Consolas", Font.PLAIN, 10));
            lblSub.setForeground(isSel ? RetroTheme.ACCENT_CYAN : new Color(150, 150, 170, 0));
            lblSub.setName("menu-sub-" + i);
            itemRow.add(lblSub);

            itemRow.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    selectedMenuIndex = idx;
                    updateMenuStyles();
                    repaint();
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    triggerMenuAction(idx);
                }
            });

            menuListPanel.add(itemRow);
        }

        container.add(menuListPanel);
        container.add(Box.createVerticalGlue());

        return container;
    }

    private void updateMenuStyles() {
        if (menuListPanel == null) return;
        int compCount = menuListPanel.getComponentCount();
        int itemIdx = 0;
        for (int i = 0; i < compCount; i++) {
            Component c = menuListPanel.getComponent(i);
            if (c instanceof JPanel) {
                JPanel row = (JPanel) c;
                for (Component child : row.getComponents()) {
                    if (child instanceof JLabel) {
                        JLabel lbl = (JLabel) child;
                        String name = lbl.getName();
                        if (name != null && name.startsWith("menu-title-")) {
                            boolean isSel = (itemIdx == selectedMenuIndex);
                            lbl.setFont(new Font("Consolas", Font.BOLD, isSel ? 24 : 22));
                            lbl.setForeground(isSel ? RetroTheme.ACCENT_PINK : new Color(220, 220, 235));
                        } else if (name != null && name.startsWith("menu-sub-")) {
                            boolean isSel = (itemIdx == selectedMenuIndex);
                            lbl.setForeground(isSel ? RetroTheme.ACCENT_CYAN : new Color(150, 150, 170, 0));
                        }
                    }
                }
                itemIdx++;
            }
        }
    }

    /** Bottom Game Bar: Profile on left, Controller Button Prompts on right */
    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(20, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 70, 16, 70));

        // Bottom Left: Player Profile & Session info
        JPanel leftBox = new JPanel();
        leftBox.setOpaque(false);
        leftBox.setLayout(new BoxLayout(leftBox, BoxLayout.Y_AXIS));

        JPanel profileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        profileRow.setOpaque(false);
        JLabel icon = new JLabel("👤");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        profileRow.add(icon);

        JLabel lblProfile = new JLabel("PROFILE: " + AppState.playerName);
        lblProfile.setFont(new Font("Consolas", Font.BOLD, 12));
        lblProfile.setForeground(Color.WHITE);
        profileRow.add(lblProfile);
        leftBox.add(profileRow);

        // Scrolling ticker (like Homefront's RSS feed)
        JPanel tickerRow = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setFont(new Font("Consolas", Font.PLAIN, 9));
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                String ticker = "// BUILD 01.09 // EDUCATION CORE // HI-SCORE: " +
                    AppState.getScoreFormatted() + " // STATUS: NOMINAL // MINI//OS ARCADE SYSTEM ONLINE // ";
                // Draw ticker twice for seamless looping
                int x = (int) tickerOffset;
                g2.drawString(ticker + ticker, x, 11);
                g2.dispose();
            }
            @Override
            public Dimension getPreferredSize() { return new Dimension(400, 16); }
        };
        tickerRow.setOpaque(false);
        tickerRow.setBorder(BorderFactory.createEmptyBorder(2, 22, 0, 0));
        leftBox.add(tickerRow);

        bar.add(leftBox, BorderLayout.WEST);

        // Bottom Right: Game Controller Prompts
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 4));
        rightBox.setOpaque(false);

        JLabel prompt1 = createPromptPill("ENTER", "SELECT", RetroTheme.ACCENT_CYAN);
        JLabel prompt2 = createPromptPill("▲/▼", "NAVIGATE", RetroTheme.ACCENT_YELLOW);
        JLabel prompt3 = createPromptPill("ESC", "QUIT", RetroTheme.ACCENT_PINK);

        rightBox.add(prompt1);
        rightBox.add(prompt2);
        rightBox.add(prompt3);

        bar.add(rightBox, BorderLayout.EAST);
        return bar;
    }

    private JLabel createPromptPill(String key, String action, Color c) {
        JLabel l = new JLabel("<html><b><font color='" + toHex(c) + "'>[" + key + "]</font></b> " + action + "</html>");
        l.setFont(new Font("Consolas", Font.BOLD, 11));
        l.setForeground(new Color(200, 200, 220));
        return l;
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();

        // 1. Draw Full-Bleed Background Image across the entire canvas
        if (bgImage != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            // Cover-fit: fill entire panel, crop if aspect mismatch
            double imgW = bgImage.getWidth(null);
            double imgH = bgImage.getHeight(null);
            double scaleX = w / imgW;
            double scaleY = h / imgH;
            double scale = Math.max(scaleX, scaleY);
            int drawW = (int)(imgW * scale);
            int drawH = (int)(imgH * scale);
            int drawX = (w - drawW) / 2;
            int drawY = (h - drawH) / 2;
            g2.drawImage(bgImage, drawX, drawY, drawW, drawH, this);
        } else {
            g2.setColor(RetroTheme.BG_DARK);
            g2.fillRect(0, 0, w, h);
        }

        // 2. Cinematic LEFT-HEAVY Dark Gradient Overlay (Homefront style)
        // The left ~45% is nearly opaque dark for perfect text readability
        // Then it feathers out to transparent showing the full background art on the right
        float[] fractions = { 0.0f, 0.30f, 0.42f, 0.52f, 0.62f, 0.75f, 1.0f };
        Color[] colors = {
            new Color(6, 4, 14, 250),   // Nearly solid at left edge
            new Color(6, 4, 14, 245),   // Still solid behind menu text area
            new Color(6, 4, 14, 200),   // Start of gentle fade
            new Color(6, 4, 14, 120),   // Midway transparency
            new Color(6, 4, 14, 50),    // Nearly transparent
            new Color(6, 4, 14, 10),    // Barely there
            new Color(6, 4, 14, 0)      // Fully transparent — shows background art
        };
        LinearGradientPaint leftFade = new LinearGradientPaint(
            new Point2D.Float(0, 0),
            new Point2D.Float(w, 0),
            fractions,
            colors
        );
        g2.setPaint(leftFade);
        g2.fillRect(0, 0, w, h);

        // 3. Bottom vignette for bottom bar text readability
        Paint gradBottom = new GradientPaint(
                0, h - 80, new Color(6, 4, 14, 0),
                0, h, new Color(6, 4, 14, 210)
        );
        g2.setPaint(gradBottom);
        g2.fillRect(0, h - 80, w, 80);

        // 4. Subtle top vignette for the top right toolbar area
        Paint gradTop = new GradientPaint(
                0, 0, new Color(6, 4, 14, 140),
                0, 50, new Color(6, 4, 14, 0)
        );
        g2.setPaint(gradTop);
        g2.fillRect(0, 0, w, 50);

        // 5. Very subtle overall film grain / noise tint for cinematic feel
        g2.setColor(new Color(0, 0, 0, 15));
        g2.fillRect(0, 0, w, h);

        g2.dispose();
    }

    private void triggerMenuAction(int index) {
        if (index == 0) {
            parentFrame.showScreen(ArcadeOS.SCREEN_ARCHIVES);
        } else if (index == 1) {
            parentFrame.showScreen(ArcadeOS.SCREEN_QUIZ);
        } else if (index == 2) {
            showAboutSystemDialog();
        } else if (index == 3) {
            confirmExit();
        }
    }

    private void confirmExit() {
        int opt = JOptionPane.showConfirmDialog(this,
                "POWER OFF: Do you wish to shut down MINI // OS?",
                "System Power Sequence",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private void showAboutSystemDialog() {
        JDialog dialog = new JDialog(parentFrame, "MINI // OS - SYSTEM ARCHITECTURE & CREDITS", true);
        dialog.setLayout(new BorderLayout(0, 15));
        dialog.getContentPane().setBackground(RetroTheme.BG_DARK);
        dialog.setSize(620, 540);
        dialog.setLocationRelativeTo(this);

        JPanel pnl = new JPanel();
        pnl.setOpaque(false);
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel titleLbl = new JLabel("◆ MINI // OS SYSTEM ARCHITECTURE & CREDITS ◆");
        titleLbl.setFont(new Font("Consolas", Font.BOLD, 15));
        titleLbl.setForeground(RetroTheme.ACCENT_PINK);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnl.add(titleLbl);
        pnl.add(Box.createVerticalStrut(16));

        JTextArea infoText = new JTextArea();
        infoText.setEditable(false);
        infoText.setFont(new Font("Consolas", Font.PLAIN, 12));
        infoText.setBackground(RetroTheme.CARD_BG);
        infoText.setForeground(new Color(230, 230, 245));
        infoText.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 25), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        infoText.setText(
            "[ INSTITUTION & COURSE ]\n" +
            "  University : University of Makati (UMak) - CCIS\n" +
            "  Course     : Operating Systems (BSCS I-BCSAD)\n" +
            "  Academic Yr: 2026\n\n" +
            "[ TECH STACK & ARCHITECTURE ]\n" +
            "  Architecture : Modular Object-Oriented Programming (Java / OOP)\n" +
            "  UI Engine    : Custom Java2D Synthwave & Kawaii 8-Bit Retro Engine\n" +
            "  Key Features : 8 OS Subsystem Simulators, Live Gantt Runways,\n" +
            "                 Multi-Module Interactive Quiz Arena, Dynamic Leaderboards\n\n" +
            "[ MODULE ENGINEERING CREDITS ]\n" +
            "  Lead Architect  : John Andre (BSCS I-BCSAD)\n" +
            "  Subsystems 01-04: CPU Scheduling, Banker's, Paging, Page Replacement\n" +
            "  Subsystems 05-08: Segmentation, Dynamic Partitioning, Disk, Multiprogramming\n" +
            "  Special Thanks  : Insuu & UMak CCIS OS Faculty"
        );
        infoText.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnl.add(infoText);
        pnl.add(Box.createVerticalStrut(18));

        JButton btnClose = RetroTheme.createRetroButton("RETURN TO MAIN MENU", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnClose.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnClose.addActionListener(e -> dialog.dispose());
        pnl.add(btnClose);

        dialog.add(pnl, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void showLeaderboard() {
        JDialog dialog = new JDialog(parentFrame, "HIGH SCORE HALL OF FAME", true);
        dialog.setLayout(new BorderLayout(0, 12));
        dialog.getContentPane().setBackground(RetroTheme.BG_DARK);
        dialog.setSize(640, 460);
        dialog.setLocationRelativeTo(this);

        JPanel pnl = new JPanel(new BorderLayout(0, 12));
        pnl.setOpaque(false);
        pnl.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        // Header
        JPanel headPnl = new JPanel(new BorderLayout());
        headPnl.setOpaque(false);
        JLabel titleLbl = new JLabel("★ RETRO ARCADE LEADERBOARD ★");
        titleLbl.setFont(new Font("Consolas", Font.BOLD, 15));
        titleLbl.setForeground(RetroTheme.ACCENT_YELLOW);
        headPnl.add(titleLbl, BorderLayout.WEST);

        JLabel subLbl = new JLabel("ACTIVE SESSIONS & TOP OPERATORS");
        subLbl.setFont(new Font("Consolas", Font.PLAIN, 11));
        subLbl.setForeground(RetroTheme.TEXT_DIM);
        headPnl.add(subLbl, BorderLayout.SOUTH);
        pnl.add(headPnl, BorderLayout.NORTH);

        // Leaderboard Table
        String[] cols = {"RANK", "PLAYER HANDLE", "ROLE", "LEVEL", "SCORE"};
        java.util.List<AppState.UserProfile> list = AppState.getLeaderboard();
        Object[][] data = new Object[list.size()][5];
        for (int i = 0; i < list.size(); i++) {
            AppState.UserProfile u = list.get(i);
            data[i][0] = String.format("#%02d", i + 1);
            data[i][1] = u.username;
            data[i][2] = u.role;
            data[i][3] = "LVL " + u.level;
            data[i][4] = String.format("%,d PTS", u.score);
        }

        JTable table = new JTable(data, cols);
        table.setFont(new Font("Consolas", Font.BOLD, 12));
        table.setRowHeight(32);
        table.setBackground(RetroTheme.CARD_BG);
        table.setForeground(new Color(230, 230, 245));
        table.setGridColor(new Color(255, 255, 255, 20));
        table.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(15, 12, 30));
        table.getTableHeader().setForeground(RetroTheme.ACCENT_CYAN);
        table.setEnabled(false);

        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(RetroTheme.CARD_BG);
        sp.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 30), 1));
        pnl.add(sp, BorderLayout.CENTER);

        // Close button
        JButton btnClose = RetroTheme.createRetroButton("CLOSE", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnClose.addActionListener(e -> dialog.dispose());
        JPanel btnPnl = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPnl.setOpaque(false);
        btnPnl.add(btnClose);
        pnl.add(btnPnl, BorderLayout.SOUTH);

        dialog.add(pnl, BorderLayout.CENTER);
        dialog.setVisible(true);
    }
}
