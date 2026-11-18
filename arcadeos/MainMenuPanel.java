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

    // Background Interactive Dot Particles ("bilog bilog or dots nasa bg")
    private static class BgDotParticle {
        float x, y;
        float vx, vy;
        float baseRadius;
        float currentRadius;
        float alpha;
        float pulsePhase;
        Color color;

        public BgDotParticle(float x, float y, float vx, float vy, float r, float a, float phase, Color c) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.baseRadius = r;
            this.currentRadius = r;
            this.alpha = a;
            this.pulsePhase = phase;
            this.color = c;
        }
    }

    private final java.util.List<BgDotParticle> bgDots = new java.util.ArrayList<>();
    private float mouseX = -999;
    private float mouseY = -999;
    private float smoothMouseX = -999;
    private float smoothMouseY = -999;
    private Timer bgParticleTimer;

    private static final Color[] DOT_COLORS = {
        new Color(40, 240, 255),   // Electric Cyan
        new Color(255, 42, 109),   // Neon Pink
        new Color(255, 205, 50),   // Amber Gold
        new Color(175, 120, 255),  // Cyber Violet
        new Color(50, 255, 190),   // Mint Green
        new Color(245, 240, 255)   // Soft Starlight White
    };

    // Scrolling ticker offset
    private float tickerOffset = 0;
    private Timer tickerTimer;

    private final String[] menuTitles = {
        "01  SYSTEM ARCHIVES",
        "02  QUIZ ARENA",
        "03  SYSTEM INTEL & SPECS",
        "04  EXIT SESSION"
    };

    private final String[] menuSubtitles = {
        "8 CORE SIMULATOR LABS // CPU, MEMORY, DISK & CONCURRENCY",
        "KERNEL MASTERY CHALLENGES & ACADEMIC DRILLS",
        "SYSTEM ARCHITECTURE, TECH STACK & CREDITS",
        "TERMINATE SIMULATOR & RETURN TO DESKTOP"
    };

    private JPanel menuListPanel;

    public MainMenuPanel(ArcadeOS parent) {
        this.parentFrame = parent;
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout());

        // Load background image (Use bg.jpg with retro pixelation filter)
        try {
            File f = new File("image/bg.jpg");
            if (!f.exists()) f = new File(System.getProperty("user.dir"), "image/bg.jpg");
            if (f.exists()) {
                java.awt.image.BufferedImage raw = ImageIO.read(f);
                bgImage = applyPixelFilter(raw, 2);
            }
        } catch (Exception e) {
            System.err.println("Could not load background image: " + e.getMessage());
        }

        // Keyboard navigation
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
                    selectedMenuIndex = (selectedMenuIndex - 1 + menuTitles.length) % menuTitles.length;
                    ArchivesPanel.playTone(920, 25, 0.35f);
                    updateMenuStyles();
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
                    selectedMenuIndex = (selectedMenuIndex + 1) % menuTitles.length;
                    ArchivesPanel.playTone(920, 25, 0.35f);
                    updateMenuStyles();
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    ArchivesPanel.playTone(1200, 75, 0.5f);
                    triggerMenuAction(selectedMenuIndex);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    confirmExit();
                }
            }
        });

        // Top mini toolbar
        add(createTopBar(), BorderLayout.NORTH);

        // Center: 2-Column Balanced Dashboard
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

        // Initialize Background Interactive Circular Dots ("bilog bilog or dots nasa bg")
        initBackgroundParticles(110);

        // Background particles animation & smooth physics timer (~33 FPS)
        bgParticleTimer = new Timer(30, e -> {
            updateBackgroundParticles();
            repaint();
        });
        bgParticleTimer.start();

        // Interactive mouse tracking across the entire window for background particles
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
                if (event instanceof MouseEvent) {
                    MouseEvent me = (MouseEvent) event;
                    if (isShowing()) {
                        try {
                            Point p = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), MainMenuPanel.this);
                            mouseX = p.x;
                            mouseY = p.y;
                        } catch (Exception ignored) {}
                    }
                }
            }, AWTEvent.MOUSE_MOTION_EVENT_MASK);
        } catch (Exception ignored) {}

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 14));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 36));

        JButton btnSwitch = createTopArcadeButton("U", "SWITCH OPERATOR", RetroTheme.ACCENT_CYAN,
                e -> parentFrame.showScreen(ArcadeOS.SCREEN_LOGIN));

        JButton btnLeader = createTopArcadeButton("L", "LEADERBOARD", RetroTheme.ACCENT_YELLOW,
                e -> showLeaderboard());

        JButton btnLogout = createTopArcadeButton("X", "LOG OUT", RetroTheme.ACCENT_PINK,
                e -> parentFrame.showScreen(ArcadeOS.SCREEN_LOGIN));

        bar.add(btnSwitch);
        bar.add(btnLeader);
        bar.add(btnLogout);
        return bar;
    }

    private JButton createTopArcadeButton(String key, String label, Color accentColor, ActionListener action) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                boolean hover = getModel().isRollover();

                // 1. Solid frosted dark backdrop (100% contrast against trees, lamps, and bright pixels)
                Color bg = hover
                        ? new Color(26, 18, 48, 245)
                        : new Color(14, 10, 26, 230);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                // Subtle inner top gloss highlight
                g2.setColor(new Color(255, 255, 255, hover ? 35 : 18));
                g2.fillRoundRect(1, 1, w - 2, h / 2 - 1, 7, 7);

                // 2. High-contrast Neon Accent Border
                Color borderColor = hover ? accentColor : new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 160);
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(hover ? 1.8f : 1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                // If hovered, bottom accent glow bar
                if (hover) {
                    g2.setColor(accentColor);
                    g2.fillRect(6, h - 3, w - 12, 2);
                }

                // 3. 3D Keycap Box on Left for "[ U ]"
                int keyBoxW = 24;
                int keyBoxH = h - 10;
                int keyBoxX = 6;
                int keyBoxY = 5;

                g2.setColor(new Color(36, 26, 56));
                g2.fillRoundRect(keyBoxX, keyBoxY, keyBoxW, keyBoxH, 4, 4);
                g2.setColor(hover ? accentColor : new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 140));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(keyBoxX, keyBoxY, keyBoxW, keyBoxH, 4, 4);

                // Key letter centered
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int kw = fm.stringWidth(key);
                g2.setColor(hover ? Color.WHITE : accentColor);
                g2.drawString(key, keyBoxX + (keyBoxW - kw) / 2, keyBoxY + (keyBoxH + fm.getAscent() - fm.getDescent()) / 2);

                // 4. Action Text Label
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(hover ? Color.WHITE : new Color(235, 230, 250));
                g2.drawString(label, keyBoxX + keyBoxW + 8, (h + fm.getAscent() - fm.getDescent()) / 2);

                g2.dispose();
            }
        };

        // Measure preferred size to comfortably fit keycap + label + padding
        Font font = new Font("Consolas", Font.BOLD, 11);
        FontMetrics fm = btn.getFontMetrics(font);
        int textW = fm.stringWidth(label);
        int totalW = 6 + 24 + 8 + textW + 14;
        btn.setPreferredSize(new Dimension(totalW, 32));

        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder());
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                ArchivesPanel.playTone(1100, 20, 0.25f);
            }
        });

        return btn;
    }

    /** Clean, Vertically Balanced Left-Aligned Game Menu Area */
    private JPanel createMenuArea() {
        JPanel outer = new JPanel();
        outer.setOpaque(false);
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setBorder(BorderFactory.createEmptyBorder(10, 80, 20, 40));

        outer.add(Box.createVerticalGlue());

        // 1. Subtag
        JLabel subTag = new JLabel("SYSTEM ROOT // ARCADE EDITION");
        subTag.setFont(new Font("Consolas", Font.BOLD, 11));
        subTag.setForeground(RetroTheme.ACCENT_PINK);
        subTag.setAlignmentX(Component.LEFT_ALIGNMENT);
        outer.add(subTag);

        outer.add(Box.createVerticalStrut(4));

        // 2. Title Row: MINI // OS
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));

        JLabel lblMini = new JLabel("MINI ");
        lblMini.setFont(new Font("Consolas", Font.BOLD, 54));
        lblMini.setForeground(Color.WHITE);
        JLabel lblSlash = new JLabel("// ");
        lblSlash.setFont(new Font("Consolas", Font.BOLD, 54));
        lblSlash.setForeground(RetroTheme.ACCENT_PINK);
        JLabel lblOs = new JLabel("OS");
        lblOs.setFont(new Font("Consolas", Font.BOLD, 54));
        lblOs.setForeground(Color.WHITE);
        titleRow.add(lblMini);
        titleRow.add(lblSlash);
        titleRow.add(lblOs);
        outer.add(titleRow);

        outer.add(Box.createVerticalStrut(3));

        // 3. Tagline
        JLabel tagLine = new JLabel("8-BIT RETRO OPERATING SYSTEM SIMULATOR");
        tagLine.setFont(new Font("Consolas", Font.BOLD, 12));
        tagLine.setForeground(RetroTheme.ACCENT_CYAN);
        tagLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        tagLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        outer.add(tagLine);

        outer.add(Box.createVerticalStrut(14));

        // 4. Subtle accent line
        JPanel divider = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(
                    0, 0, RetroTheme.ACCENT_PINK,
                    420, 0, new Color(5, 217, 232, 0)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), 2);
                g2.dispose();
            }
        };
        divider.setMaximumSize(new Dimension(420, 2));
        divider.setPreferredSize(new Dimension(420, 2));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        divider.setOpaque(false);
        outer.add(divider);

        outer.add(Box.createVerticalStrut(32));

        // 5. Menu items list
        menuListPanel = new JPanel();
        menuListPanel.setOpaque(false);
        menuListPanel.setLayout(new BoxLayout(menuListPanel, BoxLayout.Y_AXIS));
        menuListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (int i = 0; i < menuTitles.length; i++) {
            final int idx = i;
            boolean isSel = (idx == selectedMenuIndex);

            JPanel itemRow = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (selectedMenuIndex == idx) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                        // Left neon accent bar
                        int barAlpha = (int)(pulseAlpha * 255);
                        g2.setColor(new Color(255, 42, 141, barAlpha));
                        g2.fillRoundRect(0, 2, 4, getHeight() - 4, 2, 2);

                        // Subtle horizontal gradient glow
                        GradientPaint glowH = new GradientPaint(
                            0, 0, new Color(255, 42, 141, 35),
                            460, 0, new Color(255, 42, 141, 0)
                        );
                        g2.setPaint(glowH);
                        g2.fillRect(0, 0, 460, getHeight());
                        g2.dispose();
                    }
                }
            };
            itemRow.setOpaque(false);
            itemRow.setLayout(new BoxLayout(itemRow, BoxLayout.Y_AXIS));
            itemRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            itemRow.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
            itemRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            itemRow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel lblItem = new JLabel((isSel ? ">> " : "   ") + menuTitles[i]);
            lblItem.setName("lblTitle");
            lblItem.setFont(new Font("Consolas", Font.BOLD, isSel ? 23 : 21));
            lblItem.setForeground(isSel ? Color.WHITE : new Color(210, 210, 230));
            lblItem.setAlignmentX(Component.LEFT_ALIGNMENT);
            lblItem.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            itemRow.add(lblItem);

            itemRow.add(Box.createVerticalStrut(2));

            JLabel lblSub = new JLabel("      " + menuSubtitles[i]);
            lblSub.setName("lblSub");
            lblSub.setFont(new Font("Consolas", Font.PLAIN, 11));
            lblSub.setForeground(isSel ? RetroTheme.ACCENT_CYAN : new Color(135, 135, 160));
            lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);
            lblSub.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            itemRow.add(lblSub);

            itemRow.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (selectedMenuIndex != idx) {
                        selectedMenuIndex = idx;
                        ArchivesPanel.playTone(920, 20, 0.3f);
                        updateMenuStyles();
                        repaint();
                    }
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    ArchivesPanel.playTone(1200, 70, 0.5f);
                    triggerMenuAction(idx);
                }
            });

            menuListPanel.add(itemRow);
            menuListPanel.add(Box.createVerticalStrut(18));
        }

        outer.add(menuListPanel);
        outer.add(Box.createVerticalGlue());

        return outer;
    }

    private void updateMenuStyles() {
        if (menuListPanel == null) return;
        int itemIdx = 0;
        for (int i = 0; i < menuListPanel.getComponentCount(); i++) {
            Component c = menuListPanel.getComponent(i);
            if (c instanceof JPanel) {
                JPanel row = (JPanel) c;
                boolean isSel = (itemIdx == selectedMenuIndex);

                for (Component child : row.getComponents()) {
                    if ("lblTitle".equals(child.getName()) && child instanceof JLabel) {
                        JLabel l = (JLabel) child;
                        l.setText((isSel ? ">> " : "   ") + menuTitles[itemIdx]);
                        l.setFont(new Font("Consolas", Font.BOLD, isSel ? 23 : 21));
                        l.setForeground(isSel ? Color.WHITE : new Color(210, 210, 230));
                    } else if ("lblSub".equals(child.getName()) && child instanceof JLabel) {
                        JLabel l = (JLabel) child;
                        l.setForeground(isSel ? RetroTheme.ACCENT_CYAN : new Color(135, 135, 160));
                    }
                }
                row.repaint();
                itemIdx++;
            }
        }
    }

    /** Bottom Game Bar: Profile on left, Controller Button Prompts on right */
    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(20, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(8, 80, 16, 60));

        // Bottom Left: Player Profile & Scrolling Ticker
        JPanel leftBox = new JPanel();
        leftBox.setOpaque(false);
        leftBox.setLayout(new BoxLayout(leftBox, BoxLayout.Y_AXIS));

        JLabel lblProfile = new JLabel("[*] OPERATOR: " + AppState.playerName + "   |   STATUS: NOMINAL");
        lblProfile.setFont(new Font("Consolas", Font.BOLD, 12));
        lblProfile.setForeground(Color.WHITE);
        lblProfile.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        lblProfile.setPreferredSize(new Dimension(460, 20));
        leftBox.add(lblProfile);

        // Scrolling ticker
        JPanel tickerRow = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setFont(new Font("Consolas", Font.PLAIN, 9));
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                String ticker = "// BUILD 01.09 // EDUCATION CORE // HI-SCORE: " +
                    AppState.getScoreFormatted() + " // MINI//OS ARCADE SYSTEM ONLINE // ";
                int x = (int) tickerOffset;
                g2.drawString(ticker + ticker, x, 11);
                g2.dispose();
            }
            @Override
            public Dimension getPreferredSize() { return new Dimension(540, 16); }
        };
        tickerRow.setOpaque(false);
        tickerRow.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        leftBox.add(tickerRow);

        bar.add(leftBox, BorderLayout.WEST);

        // Bottom Right: Game Controller Prompts
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 2));
        rightBox.setOpaque(false);

        JLabel prompt1 = createPromptPill("ENTER", "SELECT", RetroTheme.ACCENT_CYAN);
        JLabel prompt2 = createPromptPill("W/S", "NAVIGATE", RetroTheme.ACCENT_YELLOW);
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

    /** Pixelation filter: downscales image to retro pixel resolution and upscales with nearest-neighbor */
    private java.awt.image.BufferedImage applyPixelFilter(java.awt.image.BufferedImage src, int pixelBlockSize) {
        if (src == null || pixelBlockSize <= 1) return src;
        int w = src.getWidth();
        int h = src.getHeight();
        int smallW = Math.max(1, w / pixelBlockSize);
        int smallH = Math.max(1, h / pixelBlockSize);

        java.awt.image.BufferedImage small = new java.awt.image.BufferedImage(smallW, smallH, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D gSmall = small.createGraphics();
        gSmall.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gSmall.drawImage(src, 0, 0, smallW, smallH, null);
        gSmall.dispose();

        java.awt.image.BufferedImage pixelated = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D gPix = pixelated.createGraphics();
        gPix.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        gPix.drawImage(small, 0, 0, w, h, null);
        gPix.dispose();

        return pixelated;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();

        // 1. Draw Full-Bleed 16-Bit Background Image across the entire canvas
        if (bgImage != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            double imgW = bgImage.getWidth(null);
            double imgH = bgImage.getHeight(null);
            double scaleX = (double) w / imgW;
            double scaleY = (double) h / imgH;
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

        // 2. Cinematic Half-Dark Gradient:
        // Left ~40% is solid dark for perfect readability, feathers smoothly between 40% and 62%
        // Right side is transparent showing the 16-bit arcade room
        float[] fractions = { 0.0f, 0.38f, 0.48f, 0.62f, 1.0f };
        Color[] colors = {
            new Color(6, 4, 14, 250),
            new Color(6, 4, 14, 240),
            new Color(6, 4, 14, 160),
            new Color(6, 4, 14, 40),
            new Color(6, 4, 14, 0)
        };
        LinearGradientPaint leftFade = new LinearGradientPaint(
            new Point2D.Float(0, 0),
            new Point2D.Float(w, 0),
            fractions,
            colors
        );
        g2.setPaint(leftFade);
        g2.fillRect(0, 0, w, h);

        // 2.5 INTERACTIVE BACKGROUND PARTICLES ("bilog bilog or dots nasa bg sya")
        // Rendered strictly in background layer (behind menu text and buttons)
        paintBackgroundParticles(g2, w, h);

        // 3. Bottom vignette for bottom bar readability
        Paint gradBottom = new GradientPaint(
            0, h - 70, new Color(6, 4, 14, 0),
            0, h, new Color(6, 4, 14, 210)
        );
        g2.setPaint(gradBottom);
        g2.fillRect(0, h - 70, w, 70);

        // 4. Subtle CRT horizontal scanlines
        g2.setColor(new Color(0, 0, 0, 22));
        for (int y = 0; y < h; y += 4) {
            g2.drawLine(0, y, w, y);
        }

        g2.dispose();
    }

    private void initBackgroundParticles(int count) {
        bgDots.clear();
        java.util.Random rnd = new java.util.Random(1337);
        for (int i = 0; i < count; i++) {
            float px = rnd.nextFloat() * 1920f;
            float py = rnd.nextFloat() * 1080f;
            float vx = (rnd.nextFloat() - 0.5f) * 0.75f;
            float vy = (rnd.nextFloat() - 0.5f) * 0.75f;

            // Varied tiny sizes ("iba iba pero maliliit")
            float randTier = rnd.nextFloat();
            float r;
            if (randTier < 0.45f) {
                // Tier 1: Micro stardust dot (1.0px - 1.5px)
                r = 1.0f + rnd.nextFloat() * 0.55f;
            } else if (randTier < 0.82f) {
                // Tier 2: Small glowing ember (1.6px - 2.2px)
                r = 1.6f + rnd.nextFloat() * 0.65f;
            } else {
                // Tier 3: Medium accent micro-orb (2.3px - 2.8px)
                r = 2.3f + rnd.nextFloat() * 0.55f;
            }

            float a = 0.40f + rnd.nextFloat() * 0.55f; // alpha 0.40 to 0.95
            float phase = rnd.nextFloat() * 6.28f;
            Color c = DOT_COLORS[rnd.nextInt(DOT_COLORS.length)];
            bgDots.add(new BgDotParticle(px, py, vx, vy, r, a, phase, c));
        }
    }

    private void updateBackgroundParticles() {
        int w = getWidth() > 0 ? getWidth() : 1280;
        int h = getHeight() > 0 ? getHeight() : 860;

        // Smooth mouse tracking interpolation
        if (mouseX >= 0 && mouseY >= 0) {
            if (smoothMouseX < 0) {
                smoothMouseX = mouseX;
                smoothMouseY = mouseY;
            } else {
                smoothMouseX += (mouseX - smoothMouseX) * 0.14f;
                smoothMouseY += (mouseY - smoothMouseY) * 0.14f;
            }
        }

        for (BgDotParticle p : bgDots) {
            p.pulsePhase += 0.05f;
            // Subtle, delicate pulsing for tiny dots
            p.currentRadius = p.baseRadius + (float) Math.sin(p.pulsePhase) * 0.35f;

            // Gravitational attraction to mouse cursor in background
            if (smoothMouseX >= 0 && smoothMouseY >= 0) {
                float dx = smoothMouseX - p.x;
                float dy = smoothMouseY - p.y;
                float distSq = dx * dx + dy * dy;
                float dist = (float) Math.sqrt(distSq);

                if (dist < 300f && dist > 1f) {
                    float ndx = dx / dist;
                    float ndy = dy / dist;

                    float pullFactor = (1.0f - dist / 300f);
                    float pullForce = pullFactor * 0.36f;

                    // If dots get too close (< 40px), apply gentle repulsion/swirl so they orbit gracefully
                    if (dist < 40f) {
                        float pushFactor = (1.0f - dist / 40f) * 0.45f;
                        pullForce -= pushFactor;
                    }

                    p.vx += ndx * pullForce;
                    p.vy += ndy * pullForce;

                    // Subtle tangential swirl force
                    float perpX = -ndy;
                    float perpY = ndx;
                    p.vx += perpX * (pullFactor * 0.18f);
                    p.vy += perpY * (pullFactor * 0.18f);
                }
            }

            // Damping / friction
            p.vx *= 0.965f;
            p.vy *= 0.965f;

            // Ambient gentle drift minimum speed
            float speed = (float) Math.sqrt(p.vx * p.vx + p.vy * p.vy);
            if (speed < 0.18f) {
                p.vx += ((float) Math.random() - 0.5f) * 0.12f;
                p.vy += ((float) Math.random() - 0.5f) * 0.12f;
            } else if (speed > 3.2f) {
                p.vx = (p.vx / speed) * 3.2f;
                p.vy = (p.vy / speed) * 3.2f;
            }

            p.x += p.vx;
            p.y += p.vy;

            // Screen edge wrapping with margin
            if (p.x < -20) p.x = w + 20;
            else if (p.x > w + 20) p.x = -20;
            if (p.y < -20) p.y = h + 20;
            else if (p.y > h + 20) p.y = -20;
        }
    }

    private void paintBackgroundParticles(Graphics2D g, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Constellation / connecting lines between nearby tiny background dots (delicate, subtle web)
        int numDots = bgDots.size();
        for (int i = 0; i < numDots; i++) {
            BgDotParticle p1 = bgDots.get(i);
            for (int j = i + 1; j < numDots; j++) {
                BgDotParticle p2 = bgDots.get(j);
                float dx = p1.x - p2.x;
                float dy = p1.y - p2.y;
                float distSq = dx * dx + dy * dy;
                if (distSq < 3600f) { // 60px
                    float dist = (float) Math.sqrt(distSq);
                    int lineAlpha = (int) ((1.0f - dist / 60f) * 26f);
                    if (lineAlpha > 0) {
                        g2.setColor(new Color(p1.color.getRed(), p1.color.getGreen(), p1.color.getBlue(), lineAlpha));
                        g2.setStroke(new BasicStroke(0.65f));
                        g2.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
                    }
                }
            }
        }

        // 2. Cursor tether lines: soft connection lines drawn from cursor to nearest background dots
        if (smoothMouseX >= 0 && smoothMouseY >= 0) {
            for (BgDotParticle p : bgDots) {
                float dx = smoothMouseX - p.x;
                float dy = smoothMouseY - p.y;
                float distSq = dx * dx + dy * dy;
                if (distSq < 14400f) { // 120px
                    float dist = (float) Math.sqrt(distSq);
                    int tetherAlpha = (int) ((1.0f - dist / 120f) * 55f);
                    if (tetherAlpha > 0) {
                        g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), tetherAlpha));
                        g2.setStroke(new BasicStroke(0.85f));
                        g2.drawLine((int) smoothMouseX, (int) smoothMouseY, (int) p.x, (int) p.y);
                    }
                }
            }
        }

        // 3. Render circular dots ("bilog bilog or dots, iba iba pero maliliit")
        for (BgDotParticle p : bgDots) {
            // Soft glowing outer halo (compact 1.9x ratio)
            float haloR = p.currentRadius * 1.9f;
            int haloAlpha = Math.min(255, (int) (p.alpha * 42f));
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), haloAlpha));
            g2.fillOval((int) (p.x - haloR), (int) (p.y - haloR), (int) (haloR * 2), (int) (haloR * 2));

            // Vibrant solid inner circular core
            float coreR = p.currentRadius;
            int coreAlpha = Math.min(255, (int) (p.alpha * 230f));
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), coreAlpha));
            g2.fillOval((int) (p.x - coreR), (int) (p.y - coreR), (int) (coreR * 2), (int) (coreR * 2));

            // Bright starlet center pip only for larger micro-dots
            if (coreR >= 2.2f) {
                g2.setColor(new Color(255, 255, 255, Math.min(255, (int) (p.alpha * 220f))));
                g2.fillOval((int) (p.x - 0.75f), (int) (p.y - 0.75f), 1, 1);
            }
        }

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
            "  UI Engine    : Custom Java2D Synthwave & 8-Bit Retro Arcade Engine\n" +
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
