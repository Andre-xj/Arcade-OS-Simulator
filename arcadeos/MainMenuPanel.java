package arcadeos;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Screen 2: MAIN MENU / AAA GAME-STYLE INTERFACE
 * Matches standard video game title menus (like Homefront / Cyberpunk):
 * - Full-bleed background wallpaper
 * - Left-side dark gradient vignette for crystal clear text readability
 * - Zero bulky boxes/cards, wide open right side to showcase the background art
 * - Vertical clean text menu with glowing active selection
 * - Bottom game prompts (PROFILE on left, CONTROLS on right)
 */
public class MainMenuPanel extends JPanel {
    private ArcadeOS parentFrame;
    private int selectedMenuIndex = 0; // 0=Archives, 1=Quiz, 2=About, 3=Exit
    private Timer pulseTimer;
    private float pulseAlpha = 1.0f;
    private boolean pulseUp = false;
    private Image bgImage;

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
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    selectedMenuIndex = (selectedMenuIndex - 1 + menuTitles.length) % menuTitles.length;
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    selectedMenuIndex = (selectedMenuIndex + 1) % menuTitles.length;
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    triggerMenuAction(selectedMenuIndex);
                }
            }
        });

        // Top mini toolbar
        add(createTopBar(), BorderLayout.NORTH);

        // Center: Left-aligned game menu list (No cards, no boxes!)
        add(createMenuArea(), BorderLayout.CENTER);

        // Bottom Bar: Profile on left, Controller prompts on right
        add(createBottomBar(), BorderLayout.SOUTH);

        // Subtle pulse timer for selected item / prompt
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
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
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
        btn.setForeground(new Color(220, 220, 240));
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(RetroTheme.ACCENT_CYAN);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 1),
                        BorderFactory.createEmptyBorder(5, 12, 5, 12)
                ));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(new Color(220, 220, 240));
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 255, 255, 60), 1),
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
        container.setBorder(BorderFactory.createEmptyBorder(30, 70, 20, 40));

        // 1. Small red/pink sub-tag (like in the reference photo)
        JLabel subTag = new JLabel("메인 메뉴 // ARCADE EDITION");
        subTag.setFont(new Font("Consolas", Font.BOLD, 11));
        subTag.setForeground(RetroTheme.ACCENT_PINK);
        subTag.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(subTag);

        container.add(Box.createVerticalStrut(4));

        // 2. Huge Game Title: MINI // OS
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblMini = new JLabel("MINI ");
        lblMini.setFont(new Font("Consolas", Font.BOLD, 58));
        lblMini.setForeground(Color.WHITE);

        JLabel lblSlash = new JLabel("// ");
        lblSlash.setFont(new Font("Consolas", Font.BOLD, 58));
        lblSlash.setForeground(RetroTheme.ACCENT_PINK);

        JLabel lblOs = new JLabel("OS");
        lblOs.setFont(new Font("Consolas", Font.BOLD, 58));
        lblOs.setForeground(Color.WHITE);

        titleRow.add(lblMini);
        titleRow.add(lblSlash);
        titleRow.add(lblOs);
        container.add(titleRow);

        container.add(Box.createVerticalStrut(4));

        // 3. Tagline
        JLabel lblTag = new JLabel("KAWAII 8-BIT OPERATING SYSTEM SIMULATOR");
        lblTag.setFont(new Font("Consolas", Font.BOLD, 11));
        lblTag.setForeground(RetroTheme.ACCENT_CYAN);
        lblTag.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(lblTag);

        container.add(Box.createVerticalStrut(36));

        // 4. Vertical Game Text Menu Items (clean, pure text without clunky boxes)
        JPanel menuList = new JPanel();
        menuList.setOpaque(false);
        menuList.setLayout(new BoxLayout(menuList, BoxLayout.Y_AXIS));
        menuList.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (int i = 0; i < menuTitles.length; i++) {
            final int idx = i;
            JPanel itemRow = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (selectedMenuIndex == idx) {
                        // Subtle glowing neon highlight indicator bar on left
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setColor(RetroTheme.ACCENT_PINK);
                        g2.fillRect(0, 4, 3, getHeight() - 8);
                        g2.dispose();
                    }
                }
            };
            itemRow.setOpaque(false);
            itemRow.setLayout(new BoxLayout(itemRow, BoxLayout.Y_AXIS));
            itemRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            itemRow.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            itemRow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Main Menu Item Title
            JLabel lblItem = new JLabel(menuTitles[i]);
            lblItem.setFont(new Font("Consolas", Font.BOLD, 22));
            lblItem.setForeground(selectedMenuIndex == idx ? RetroTheme.ACCENT_PINK : Color.WHITE);
            itemRow.add(lblItem);

            // Subtitle hint that shows for selected item
            JLabel lblSub = new JLabel(menuSubtitles[i]);
            lblSub.setFont(new Font("Consolas", Font.PLAIN, 10));
            lblSub.setForeground(selectedMenuIndex == idx ? RetroTheme.ACCENT_CYAN : new Color(150, 150, 170, 0));
            itemRow.add(lblSub);

            itemRow.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    selectedMenuIndex = idx;
                    updateMenuStyles(menuList);
                    repaint();
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    triggerMenuAction(idx);
                }
            });

            menuList.add(itemRow);
            menuList.add(Box.createVerticalStrut(14));
        }

        container.add(menuList);
        return container;
    }

    private void updateMenuStyles(JPanel menuList) {
        int compCount = menuList.getComponentCount();
        int itemIdx = 0;
        for (int i = 0; i < compCount; i++) {
            Component c = menuList.getComponent(i);
            if (c instanceof JPanel) {
                JPanel row = (JPanel) c;
                JLabel titleLbl = (JLabel) row.getComponent(0);
                JLabel subLbl = (JLabel) row.getComponent(1);
                boolean isSel = (itemIdx == selectedMenuIndex);
                titleLbl.setForeground(isSel ? RetroTheme.ACCENT_PINK : new Color(230, 230, 245));
                subLbl.setForeground(isSel ? RetroTheme.ACCENT_CYAN : new Color(150, 150, 170, 0));
                itemIdx++;
            }
        }
    }

    /** Bottom Game Bar: Profile on left, Controller Button Prompts on right */
    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(20, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 70, 18, 70));

        // Bottom Left: Player Profile & Session info (matching reference image)
        JPanel leftBox = new JPanel();
        leftBox.setOpaque(false);
        leftBox.setLayout(new BoxLayout(leftBox, BoxLayout.Y_AXIS));

        JPanel profileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        profileRow.setOpaque(false);
        JLabel icon = new JLabel("👤");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        profileRow.add(icon);

        JLabel lblProfile = new JLabel("PROFILE: " + AppState.playerName);
        lblProfile.setFont(new Font("Consolas", Font.BOLD, 12));
        lblProfile.setForeground(Color.WHITE);
        profileRow.add(lblProfile);
        leftBox.add(profileRow);

        JLabel lblTicker = new JLabel("// BUILD 01.09 // EDUCATION CORE // HI-SCORE: 000000 // STATUS: NOMINAL");
        lblTicker.setFont(new Font("Consolas", Font.PLAIN, 9));
        lblTicker.setForeground(RetroTheme.ACCENT_PINK);
        lblTicker.setBorder(BorderFactory.createEmptyBorder(3, 22, 0, 0));
        leftBox.add(lblTicker);

        bar.add(leftBox, BorderLayout.WEST);

        // Bottom Right: Game Controller Prompts (e.g., [ENTER] SELECT, [ESC] QUIT)
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
        l.setForeground(new Color(230, 230, 240));
        return l;
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        int w = getWidth();
        int h = getHeight();

        // 1. Draw Full-Bleed Background Image across the entire canvas
        if (bgImage != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(bgImage, 0, 0, w, h, this);
        } else {
            g2.setColor(RetroTheme.BG_DARK);
            g2.fillRect(0, 0, w, h);
        }

        // 2. Multi-Stop Ultra-Smooth LinearGradient Blend (Horizontal Left to Right)
        // Seamlessly blends from deep solid dark behind menu text into the clear arcade room
        // Absolutely NO hard cutoff, seam, or abrupt divider line!
        float[] fractions = { 0.0f, 0.28f, 0.40f, 0.54f, 0.68f, 0.82f, 1.0f };
        Color[] colors = {
            new Color(8, 7, 16, 255), // Solid dark at left edge
            new Color(8, 7, 16, 255), // Stays solid dark covering menu text
            new Color(8, 7, 16, 215), // Ultra-gentle start of roll-off
            new Color(8, 7, 16, 125), // Soft mid feathering
            new Color(8, 7, 16, 45),  // Near transparent
            new Color(8, 7, 16, 0),   // Fully transparent - reveals full image
            new Color(8, 7, 16, 0)
        };
        LinearGradientPaint smoothFade = new LinearGradientPaint(
            new Point2D.Float(0, 0),
            new Point2D.Float(w, 0),
            fractions,
            colors
        );
        g2.setPaint(smoothFade);
        g2.fillRect(0, 0, w, h);

        // 3. Subtle bottom vignette so bottom bar text and controller prompts stay crisp
        Paint gradBottom = new GradientPaint(
                0, h - 90, new Color(8, 7, 16, 0),
                0, h, new Color(8, 7, 16, 220)
        );
        g2.setPaint(gradBottom);
        g2.fillRect(0, h - 90, w, 90);

        // 4. Subtle top vignette for the top right toolbar
        Paint gradTop = new GradientPaint(
                0, 0, new Color(8, 7, 16, 180),
                0, 55, new Color(8, 7, 16, 0)
        );
        g2.setPaint(gradTop);
        g2.fillRect(0, 0, w, 55);

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
