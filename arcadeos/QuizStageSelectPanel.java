package arcadeos;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen: STAGE SELECT // ALGORITHM QUIZ ARENA
 * -----------------------------------------------------------------------------
 * 8 Modular Mission Cartridge Cabinets:
 * 1. Space Invaders (CPU Scheduling FCFS)
 * 2. Space Shoot Vault (Banker's Deadlock Avoidance)
 * 3. Frame Hopper Bus (MMU Paging Translation)
 * 4. Super Mario Cache (Cache Replacement FIFO)
 * 5. Cyber Snake Heap (Segmentation & Traps)
 * 6. Cyber Crane Defrag (Memory Allocation First-Fit)
 * 7. Pac-Man Disk Head (Disk Head Seek FCFS)
 * 8. Overcooked Kitchen (Multiprogramming Concurrency)
 *
 * Flow: Clicking any cartridge -> Navigates to Difficulty Selection Screen (QuizDifficultyPanel)
 */
public class QuizStageSelectPanel extends JPanel {
    private final ArcadeOS parentFrame;
    private boolean crtEnabled = false;
    private boolean soundEnabled = true;
    private boolean timeAttackEnabled = true;

    // Ambient Cyber Animation
    private final List<CyberStar> stars = new ArrayList<>();
    private javax.swing.Timer animTimer;

    // Header Components
    private JLabel lblHeaderScore;
    private JButton btnCrtToggle;
    private JButton btnSfxToggle;

    public static final String[] MOD_TITLES = {
            "01 INVADERS", "02 SPACE", "03 HOPPER", "04 MARIO",
            "05 SNAKE", "06 CRANE", "07 PAC-MAN", "08 OVERCOOKED"
    };

    public static final String[] MOD_FULL_NAMES = {
            "CPU SCHEDULING (FCFS)",
            "BANKER DEADLOCK AVOIDANCE",
            "MMU PAGING TRANSLATION",
            "CACHE REPLACEMENT (FIFO)",
            "SEGMENTATION & TRAPS",
            "MEMORY ALLOCATION (FIRST FIT)",
            "DISK HEAD SEEK (FCFS)",
            "MULTIPROGRAMMING CONCURRENCY"
    };

    public static final String[] MOD_LORES = {
            "Alien Dispatch Runway",
            "Deadlock Verification",
            "Virtual MMU Paging",
            "Cache Fault Eviction",
            "Segment Limit Traps",
            "Contiguous RAM Alloc",
            "Magnetic Head Traversal",
            "Kitchen Workload Concurrency"
    };

    public static final String[] MOD_GAMES = {
            "SPACE INVADERS",
            "SPACE SHOOT VAULT",
            "FRAME HOPPER BUS",
            "SUPER MARIO CACHE",
            "CYBER SNAKE HEAP",
            "CYBER CRANE DEFRAG",
            "PAC-MAN DISK HEAD",
            "OVERCOOKED KITCHEN"
    };

    public static final Color[] MOD_COLORS = {
            new Color(5, 217, 232),   // 01 Invaders Cyan
            new Color(255, 211, 25),  // 02 Space Shoot Gold
            new Color(40, 255, 200),  // 03 Frame Hopper Mint
            new Color(255, 115, 115), // 04 Mario Coral
            new Color(90, 245, 140),  // 05 Cyber Snake Green
            new Color(215, 145, 255), // 06 Cyber Crane Violet
            new Color(255, 230, 50),  // 07 Pac-Man Yellow
            new Color(255, 150, 50)   // 08 Overcooked Orange
    };

    public static final String[] MOD_DIFFICULTIES = {
            "[EASY] Lv. 2",
            "[HARD] Lv. 4",
            "[NORMAL] Lv. 3",
            "[NORMAL] Lv. 3",
            "[HARD] Lv. 4",
            "[EASY] Lv. 2",
            "[NORMAL] Lv. 3",
            "[NORMAL] Lv. 3"
    };

    private static class CyberStar {
        float x, y, vy, size, alpha;
        Color color;
    }

    public QuizStageSelectPanel(ArcadeOS parentFrame) {
        this.parentFrame = parentFrame;
        setLayout(new BorderLayout(0, 8));
        setBackground(RetroTheme.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        // Spawn ambient arcade stars
        Color[] starColors = {
                new Color(5, 217, 232), new Color(255, 42, 141),
                new Color(57, 255, 60),  new Color(255, 211, 25), Color.WHITE
        };
        for (int i = 0; i < 60; i++) {
            CyberStar s = new CyberStar();
            s.x = (float) (Math.random() * 1400);
            s.y = (float) (Math.random() * 900);
            s.vy = (float) (-0.25 - Math.random() * 0.45);
            s.size = (float) (1.2 + Math.random() * 2.2);
            s.alpha = (float) (0.25 + Math.random() * 0.65);
            s.color = starColors[(int) (Math.random() * starColors.length)];
            stars.add(s);
        }

        animTimer = new javax.swing.Timer(33, e -> {
            for (CyberStar s : stars) {
                s.y += s.vy;
                if (s.y < 0) {
                    s.y = getHeight() > 0 ? getHeight() : 860;
                    s.x = (float) (Math.random() * (getWidth() > 0 ? getWidth() : 1280));
                }
            }
            repaint();
        });
        animTimer.start();

        // 1. Top Header with Background Box (Matching System Archives header style)
        add(createHeader(), BorderLayout.NORTH);

        // 2. Centered 2x4 Cartridge Grid (Sakto lang size, centered)
        add(createCenterGridDeck(), BorderLayout.CENTER);

        // 3. Bottom Navigation Bar
        add(createBottomConsole(), BorderLayout.SOUTH);

        // Keyboard navigation
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_ESCAPE) {
                    returnToMainMenu();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_8) {
                    selectAndEngageMission(code - KeyEvent.VK_1);
                } else if (code >= KeyEvent.VK_NUMPAD1 && code <= KeyEvent.VK_NUMPAD8) {
                    selectAndEngageMission(code - KeyEvent.VK_NUMPAD1);
                } else if (code == KeyEvent.VK_C) {
                    toggleCrt();
                } else if (code == KeyEvent.VK_S) {
                    toggleSfx();
                }
            }
        });

        addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override public void ancestorAdded(javax.swing.event.AncestorEvent e) { onShow(); }
            @Override public void ancestorRemoved(javax.swing.event.AncestorEvent e) {}
            @Override public void ancestorMoved(javax.swing.event.AncestorEvent e) {}
        });
    }

    public void onShow() {
        if (lblHeaderScore != null) {
            lblHeaderScore.setText(String.format("OPERATOR: %s | %s XP", AppState.playerName, AppState.getScoreFormatted()));
        }
        playTone(520, 20);
        requestFocusInWindow();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. TOP HEADER (Rounded container with subtle border & dark background)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                // Dark rounded metallic container
                g2.setColor(new Color(20, 15, 36, 240));
                g2.fillRoundRect(0, 0, w, h, 10, 10);
                g2.setColor(RetroTheme.BORDER_COLOR);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));

        // Left: Mascot + Arcade OS Title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(240, 42));
        left.add(new JLabel(new ImageIcon(PixelSprites.createRobotSprite(32))));

        JPanel leftText = new JPanel();
        leftText.setOpaque(false);
        leftText.setLayout(new BoxLayout(leftText, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        JLabel lm = new JLabel("MINI ");
        lm.setFont(new Font("Consolas", Font.BOLD, 16));
        lm.setForeground(Color.WHITE);
        JLabel ls = new JLabel("// ");
        ls.setFont(new Font("Consolas", Font.BOLD, 16));
        ls.setForeground(RetroTheme.ACCENT_PINK);
        JLabel lo = new JLabel("OS");
        lo.setFont(new Font("Consolas", Font.BOLD, 16));
        lo.setForeground(Color.WHITE);
        titleRow.add(lm); titleRow.add(ls); titleRow.add(lo);
        leftText.add(titleRow);

        JLabel tag = new JLabel("OPERATING SYSTEM SIMULATOR");
        tag.setFont(new Font("Consolas", Font.BOLD, 9));
        tag.setForeground(new Color(185, 175, 215));
        leftText.add(tag);

        left.add(leftText);
        header.add(left, BorderLayout.WEST);

        // Center: Stage Select Header
        JPanel center = new JPanel(new GridLayout(2, 1, 0, 2));
        center.setOpaque(false);

        JLabel mainTitle = new JLabel("STAGE SELECT  //  ALGORITHM QUIZ ARENA", SwingConstants.CENTER);
        mainTitle.setFont(new Font("Consolas", Font.BOLD, 15));
        mainTitle.setForeground(Color.WHITE);
        center.add(mainTitle);

        JLabel desc = new JLabel("CHOOSE A MISSION TO TEST YOUR OS MASTERY", SwingConstants.CENTER);
        desc.setFont(new Font("Consolas", Font.BOLD, 11));
        desc.setForeground(new Color(95, 245, 255)); // Bright Cyan
        center.add(desc);

        header.add(center, BorderLayout.CENTER);

        // Right: Gamification Badges & Toggles
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        right.setOpaque(false);

        String userTag = String.format("OPERATOR: %s | %s XP", AppState.playerName, AppState.getScoreFormatted());
        lblHeaderScore = new JLabel(userTag);
        lblHeaderScore.setFont(new Font("Consolas", Font.BOLD, 11));
        lblHeaderScore.setForeground(RetroTheme.ACCENT_YELLOW);
        lblHeaderScore.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        right.add(lblHeaderScore);

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

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private void toggleCrt() {
        crtEnabled = !crtEnabled;
        btnCrtToggle.setText(crtEnabled ? "CRT: ON" : "CRT: OFF");
        btnCrtToggle.setForeground(crtEnabled ? RetroTheme.ACCENT_GREEN : RetroTheme.TEXT_DIM);
        repaint();
        requestFocusInWindow();
    }

    private void toggleSfx() {
        soundEnabled = !soundEnabled;
        btnSfxToggle.setText(soundEnabled ? "SFX: ON" : "SFX: OFF");
        btnSfxToggle.setForeground(soundEnabled ? RetroTheme.ACCENT_GREEN : RetroTheme.TEXT_DIM);
        if (soundEnabled) playTone(600, 30);
        requestFocusInWindow();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. CENTERED 2x4 CARTRIDGE GRID DECK
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel createCenterGridDeck() {
        JPanel gridWrapper = new JPanel(new GridBagLayout());
        gridWrapper.setOpaque(false);

        JPanel grid = new JPanel(new GridLayout(2, 4, 14, 12));
        grid.setOpaque(false);

        for (int i = 0; i < 8; i++) {
            grid.add(createCabinetCard(i));
        }

        gridWrapper.add(grid);
        return gridWrapper;
    }

    private JPanel createCabinetCard(int modIdx) {
        final Color c = MOD_COLORS[modIdx];
        final BufferedImage mascot = getMascotSprite(modIdx, 48);

        JPanel card = new JPanel(new BorderLayout()) {
            private boolean isHovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                    @Override public void mousePressed(MouseEvent e) { selectAndEngageMission(modIdx); }
                });
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // 1. Draw Aligned Game-Themed Background
                drawCabinetGameThemedBackground(g2, modIdx, w, h, c, isHovered);

                // 2. Glowing Oval Pedestal beneath Mascot Sprite
                int cx = w / 2;
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), isHovered ? 90 : 60));
                g2.fillOval(cx - 38, 96, 76, 16);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), isHovered ? 200 : 120));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(cx - 38, 96, 76, 16);

                // 3. Top Arcade Header Strip
                g2.setColor(new Color(c.getRed() / 4, c.getGreen() / 4, c.getBlue() / 4, 230));
                g2.fillRoundRect(2, 2, w - 4, 26, 12, 12);
                g2.fillRect(2, 15, w - 4, 11);

                // Top Header Text: MISSION 01 (Left) & Difficulty (Right)
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(c);
                g2.drawString(String.format("MISSION %02d", modIdx + 1), 12, 18);

                g2.setColor(RetroTheme.ACCENT_YELLOW);
                String diff = MOD_DIFFICULTIES[modIdx];
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(diff, w - 12 - fm.stringWidth(diff), 18);

                // 4. Mascot Sprite (Centered at y=40..90)
                if (mascot != null) {
                    int mx = (w - mascot.getWidth()) / 2;
                    g2.drawImage(mascot, mx, 40, null);
                }

                // 5. Game Name (Bold White at y=128)
                g2.setFont(new Font("Consolas", Font.BOLD, 13));
                g2.setColor(Color.WHITE);
                String gameName = MOD_GAMES[modIdx];
                fm = g2.getFontMetrics();
                g2.drawString(gameName, (w - fm.stringWidth(gameName)) / 2, 128);

                // 6. Algorithm Pill Capsule at y=144
                String fullAlgo = MOD_FULL_NAMES[modIdx];
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                fm = g2.getFontMetrics();
                int pillW = fm.stringWidth(fullAlgo) + 16;
                int pillX = (w - pillW) / 2;
                g2.setColor(new Color(c.getRed() / 4, c.getGreen() / 4, c.getBlue() / 4, 210));
                g2.fillRoundRect(pillX, 138, pillW, 18, 6, 6);
                g2.setColor(c);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(pillX, 138, pillW, 18, 6, 6);
                g2.drawString(fullAlgo, pillX + 8, 151);

                // 7. Lore Line at y=172
                g2.setFont(new Font("Consolas", Font.PLAIN, 10));
                g2.setColor(new Color(190, 195, 215));
                String lore = MOD_LORES[modIdx];
                fm = g2.getFontMetrics();
                g2.drawString(lore, (w - fm.stringWidth(lore)) / 2, 172);

                // 8. Reward Badge at y=188
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(RetroTheme.ACCENT_GREEN);
                String rwd = "REWARD: +150 XP";
                fm = g2.getFontMetrics();
                g2.drawString(rwd, (w - fm.stringWidth(rwd)) / 2, 188);

                // 9. Outer Border (Glows brighter on hover)
                g2.setColor(isHovered ? c : new Color(c.getRed() / 2, c.getGreen() / 2, c.getBlue() / 2, 130));
                g2.setStroke(new BasicStroke(isHovered ? 2.0f : 1.2f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, 12, 12);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(280, 245));
        card.setMinimumSize(new Dimension(280, 245));
        card.setMaximumSize(new Dimension(280, 245));

        JButton btnEngage = new JButton("ENGAGE MISSION >>") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(c.getRed() / 3, c.getGreen() / 3, c.getBlue() / 3));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(c);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnEngage.setFont(new Font("Consolas", Font.BOLD, 11));
        btnEngage.setForeground(c);
        btnEngage.setContentAreaFilled(false);
        btnEngage.setOpaque(false);
        btnEngage.setFocusPainted(false);
        btnEngage.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        btnEngage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEngage.addActionListener(e -> selectAndEngageMission(modIdx));
        card.add(btnEngage, BorderLayout.SOUTH);

        return card;
    }

    private void drawCabinetGameThemedBackground(Graphics2D g2, int modIdx, int w, int h, Color c, boolean hovered) {
        GradientPaint bgGrad = new GradientPaint(
                0, 0, new Color(16, 12, 30),
                0, h, new Color(c.getRed() / 6, c.getGreen() / 6, c.getBlue() / 6)
        );
        g2.setPaint(bgGrad);
        g2.fillRoundRect(1, 1, w - 2, h - 2, 12, 12);

        switch (modIdx) {
            case 0: drawCabinetInvadersBg(g2, w, h, c); break;
            case 1: drawCabinetSpaceShootBg(g2, w, h, c); break;
            case 2: drawCabinetFrameHopperBg(g2, w, h, c); break;
            case 3: drawCabinetMarioCacheBg(g2, w, h, c); break;
            case 4: drawCabinetSnakeBg(g2, w, h, c); break;
            case 5: drawCabinetCraneBg(g2, w, h, c); break;
            case 6: drawCabinetPacmanBg(g2, w, h, c); break;
            case 7: drawCabinetOvercookedBg(g2, w, h, c); break;
        }
    }

    private void drawCabinetInvadersBg(Graphics2D g2, int w, int h, Color c) {
        g2.setColor(new Color(255, 255, 255, 45));
        int[] starX = {22, 58, 85, 130, 185, 220, 260, 40, 150, 240};
        int[] starY = {35, 62, 40, 55, 38, 70, 48, 105, 110, 95};
        for (int i = 0; i < starX.length; i++) {
            g2.fillRect(starX[i] % w, starY[i], (i % 2 == 0) ? 2 : 1, (i % 2 == 0) ? 2 : 1);
        }
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 35));
        for (int x = 20; x < w - 20; x += 40) {
            g2.fillRect(x, 48, 8, 5);
        }
    }

    private void drawCabinetSpaceShootBg(Graphics2D g2, int w, int h, Color c) {
        int cx = w / 2;
        int cy = 68;
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 28));
        g2.drawOval(cx - 45, cy - 45, 90, 90);
        g2.drawOval(cx - 28, cy - 28, 56, 56);
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
        g2.drawLine(cx - 50, cy, cx + 50, cy);
        g2.drawLine(cx, cy - 50, cx, cy + 50);
    }

    private void drawCabinetFrameHopperBg(Graphics2D g2, int w, int h, Color c) {
        g2.setColor(new Color(30, 60, 130, 45));
        g2.fillRect(16, 32, w - 32, 28);
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
        g2.fillRoundRect(24, 36, 42, 10, 4, 4);
        g2.fillRoundRect(86, 36, 55, 10, 4, 4);
    }

    private void drawCabinetMarioCacheBg(Graphics2D g2, int w, int h, Color c) {
        g2.setColor(new Color(140, 30, 20, 35));
        int brickW = 28;
        int brickH = 10;
        for (int row = 0; row < 5; row++) {
            int y = 32 + row * brickH;
            int offset = (row % 2 == 0) ? 0 : brickW / 2;
            for (int col = 0; col < w / brickW + 2; col++) {
                int x = col * brickW + offset;
                g2.drawRect(x, y, brickW, brickH);
            }
        }
        g2.setColor(new Color(255, 200, 30, 60));
        g2.fillRoundRect(w - 44, 38, 16, 16, 4, 4);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        g2.drawString("?", w - 39, 50);
    }

    private void drawCabinetSnakeBg(Graphics2D g2, int w, int h, Color c) {
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 25));
        int gridSz = 16;
        for (int x = 16; x < w - 16; x += gridSz) {
            g2.drawLine(x, 32, x, 120);
        }
        for (int y = 32; y < 120; y += gridSz) {
            g2.drawLine(16, y, w - 16, y);
        }
    }

    private void drawCabinetCraneBg(Graphics2D g2, int w, int h, Color c) {
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
        g2.drawLine(16, 35, w - 16, 35);
        g2.drawLine(16, 43, w - 16, 43);
        for (int x = 16; x < w - 16; x += 18) {
            g2.drawLine(x, 35, x + 9, 43);
            g2.drawLine(x + 9, 43, x + 18, 35);
        }
    }

    private void drawCabinetPacmanBg(Graphics2D g2, int w, int h, Color c) {
        int cx = w / 2;
        int cy = 68;
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 35));
        g2.drawOval(cx - 48, cy - 48, 96, 96);
        g2.drawOval(cx - 30, cy - 30, 60, 60);
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            int px = cx + (int) (Math.cos(angle) * 38);
            int py = cy + (int) (Math.sin(angle) * 38);
            g2.fillOval(px - 2, py - 2, 4, 4);
        }
    }

    private void drawCabinetOvercookedBg(Graphics2D g2, int w, int h, Color c) {
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
        int panW = 34;
        for (int i = 0; i < 4; i++) {
            int px = 20 + i * (panW + 12);
            g2.fillRoundRect(px, 34, panW, 16, 4, 4);
        }
    }

    public static BufferedImage getMascotSprite(int modIdx, int size) {
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

    /**
     * User clicks any cartridge -> Navigates to Difficulty Selection Screen
     */
    private void selectAndEngageMission(int modIdx) {
        playTone(600, 30);
        if (parentFrame != null) {
            parentFrame.selectQuizMission(modIdx);
        }
    }

    private void returnToMainMenu() {
        playTone(380, 25);
        if (parentFrame != null) {
            parentFrame.showScreen(ArcadeOS.SCREEN_MAIN_MENU);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. BOTTOM CONTROL STRIP
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel createBottomConsole() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        JButton btnBack = RetroTheme.createRetroButton("< MENU [ESC]", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnBack.setFont(new Font("Consolas", Font.BOLD, 11));
        btnBack.setFocusable(false);
        btnBack.addActionListener(e -> returnToMainMenu());
        bottom.add(btnBack, BorderLayout.WEST);

        JPanel modeToggleBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        modeToggleBox.setOpaque(false);

        JButton btnTimeToggle = RetroTheme.createRetroButton("SURVIVAL MODE (90s): ON", new Color(18, 55, 35), RetroTheme.ACCENT_GREEN);
        btnTimeToggle.setFont(new Font("Consolas", Font.BOLD, 11));
        btnTimeToggle.setFocusable(false);
        btnTimeToggle.setToolTipText("Toggle 90-second survival countdown timer with 3 lives for extra thrill!");
        btnTimeToggle.addActionListener(e -> {
            timeAttackEnabled = !timeAttackEnabled;
            btnTimeToggle.setText(timeAttackEnabled ? "SURVIVAL MODE (90s): ON" : "PRACTICE MODE (NO TIMER): ON");
            btnTimeToggle.setBackground(timeAttackEnabled ? new Color(18, 55, 35) : new Color(38, 28, 55));
            btnTimeToggle.setForeground(timeAttackEnabled ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_CYAN);
            playTone(560, 20);
        });
        modeToggleBox.add(btnTimeToggle);

        JLabel hint = new JLabel("CHOOSE ANY OF THE 8 MODULES TO ENTER THE BATTLEFIELD ARENA!");
        hint.setFont(new Font("Consolas", Font.BOLD, 11));
        hint.setForeground(RetroTheme.TEXT_DIM);
        modeToggleBox.add(hint);

        bottom.add(modeToggleBox, BorderLayout.CENTER);

        JLabel soundToggle = new JLabel("CLICK ANY CARTRIDGE TO LAUNCH >>");
        soundToggle.setFont(new Font("Consolas", Font.BOLD, 11));
        soundToggle.setForeground(RetroTheme.ACCENT_YELLOW);
        bottom.add(soundToggle, BorderLayout.EAST);

        return bottom;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();

        // 1. Deep Space Cyber Gradient
        GradientPaint bgGrad = new GradientPaint(
                0, 0, new Color(10, 8, 20),
                0, h, new Color(18, 12, 34)
        );
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, w, h);

        // 2. Ambient Floating Cyber Stars
        for (CyberStar s : stars) {
            g2.setColor(new Color(
                    s.color.getRed(), s.color.getGreen(), s.color.getBlue(),
                    (int) (Math.min(1.0f, Math.max(0.0f, s.alpha)) * 255)
            ));
            g2.fill(new java.awt.geom.Ellipse2D.Float(s.x % w, s.y % h, s.size, s.size));
        }

        // 3. Optional CRT scanlines
        if (crtEnabled) {
            g2.setColor(new Color(0, 0, 0, 38));
            for (int y = 0; y < h; y += 3) {
                g2.drawLine(0, y, w, y);
            }
        }

        g2.dispose();
    }

    private void playTone(int hz, int msecs) {
        if (!soundEnabled) return;
        new Thread(() -> {
            try {
                byte[] buf = new byte[msecs * 8];
                for (int i = 0; i < buf.length; i++) {
                    double angle = i / (8000.0 / hz) * 2.0 * Math.PI;
                    buf[i] = (byte) (Math.sin(angle) * 80.0);
                }
                AudioFormat af = new AudioFormat(8000f, 8, 1, true, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();
                sdl.write(buf, 0, buf.length);
                sdl.drain();
                sdl.close();
            } catch (Exception ignored) {}
        }).start();
    }
}
