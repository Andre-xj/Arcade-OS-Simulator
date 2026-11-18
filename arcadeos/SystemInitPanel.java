package arcadeos;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;

/**
 * Screen 0: SYSTEM INITIALIZATION — First Boot / Create Root Admin Account
 * Upgraded with interactive background micro-particles, cyber radial ambiance,
 * frosted acrylic card, and high-contrast glowing cyber buttons.
 */
public class SystemInitPanel extends JPanel {
    private JTextField txtAdminUser;
    private JPasswordField txtPass, txtConfirmPass;
    private ArcadeOS parentFrame;

    // Interactive Background Particle System
    private static class BgDotParticle {
        float x, y;
        float vx, vy;
        float baseRadius;
        float currentRadius;
        float alpha;
        float pulsePhase;
        Color color;

        BgDotParticle(float x, float y, float vx, float vy, float r, float a, float phase, Color c) {
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

    private final List<BgDotParticle> bgDots = new ArrayList<>();
    private static final Color[] DOT_COLORS = {
            new Color(255, 211, 25),   // Radiant Yellow
            new Color(5, 217, 232),    // Electric Cyan
            new Color(255, 42, 141),   // Hot Pink
            new Color(57, 255, 20),    // Cyber Green
            new Color(180, 0, 255),    // Neon Purple
            new Color(100, 240, 255)   // Ice Cyan
    };

    private float mouseX = -999f;
    private float mouseY = -999f;
    private float smoothMouseX = -999f;
    private float smoothMouseY = -999f;
    private Timer bgParticleTimer;
    private float animTick = 0f;
    private AWTEventListener globalMouseListener;

    public SystemInitPanel(ArcadeOS parent) {
        this.parentFrame = parent;
        setBackground(RetroTheme.BG_DEEPER);
        setLayout(new GridBagLayout());

        // Initialize Background Interactive Circular Micro-Dots
        initBackgroundParticles(95);

        // ── Center Card with Frosted Glassmorphism ───────────────────────────
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // 1. Multi-layered Drop Shadow for 3D elevation
                g2.setColor(new Color(0, 0, 0, 110));
                g2.fillRoundRect(8, 12, w - 16, h - 8, 22, 22);
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillRoundRect(4, 6, w - 8, h - 4, 20, 20);

                // Cyan/Yellow ambient rim glow
                g2.setColor(new Color(5, 217, 232, 30));
                g2.setStroke(new BasicStroke(5f));
                g2.drawRoundRect(2, 2, w - 5, h - 5, 20, 20);

                // 2. Frosted Dark Glass Gradient Body
                GradientPaint bgGrad = new GradientPaint(
                        0, 0, new Color(24, 18, 46, 248),
                        0, h, new Color(11, 8, 24, 252)
                );
                g2.setPaint(bgGrad);
                g2.fillRoundRect(0, 0, w, h, 20, 20);

                // Top glossy glass highlight reflection
                GradientPaint topGloss = new GradientPaint(
                        0, 0, new Color(255, 255, 255, 32),
                        0, h / 3, new Color(255, 255, 255, 0)
                );
                g2.setPaint(topGloss);
                g2.fillRoundRect(1, 1, w - 2, h / 3, 19, 19);

                // 3. Cyber Dual-Tone Neon Border (Electric Cyan top to Radiant Yellow bottom)
                GradientPaint borderGrad = new GradientPaint(
                        0, 0, new Color(5, 217, 232, 240),
                        0, h, new Color(255, 211, 25, 220)
                );
                g2.setPaint(borderGrad);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, 20, 20);

                // 4. Cyber Corner HUD Bracket Accents
                g2.setStroke(new BasicStroke(2.5f));
                g2.setColor(RetroTheme.ACCENT_CYAN);
                // Top-Left corner
                g2.drawLine(4, 16, 4, 4);
                g2.drawLine(4, 4, 16, 4);
                // Top-Right corner
                g2.drawLine(w - 17, 4, w - 5, 4);
                g2.drawLine(w - 5, 4, w - 5, 16);

                g2.setColor(RetroTheme.ACCENT_YELLOW);
                // Bottom-Left corner
                g2.drawLine(4, h - 17, 4, h - 5);
                g2.drawLine(4, h - 5, 16, h - 5);
                // Bottom-Right corner
                g2.drawLine(w - 17, h - 5, w - 5, h - 5);
                g2.drawLine(w - 5, h - 5, w - 5, h - 17);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(500, 460));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(20, 36, 20, 36));

        // ── 1. Robot icon + "MINI // OS" header row ──────────────────────────
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel iconLbl = new JLabel(new ImageIcon(PixelSprites.createRobotSprite(38))) {
            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int bobOffset = (int) (Math.sin(animTick * 0.08f) * 2.5f);
                g2.translate(0, bobOffset);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        titleRow.add(iconLbl);

        JPanel textStack = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        textStack.setOpaque(false);
        JLabel lMini = new JLabel("MINI ");
        lMini.setFont(new Font("Consolas", Font.BOLD, 18));
        lMini.setForeground(Color.WHITE);
        JLabel lSlash = new JLabel("// ");
        lSlash.setFont(new Font("Consolas", Font.BOLD, 18));
        lSlash.setForeground(RetroTheme.ACCENT_PINK);
        JLabel lOs = new JLabel("OS");
        lOs.setFont(new Font("Consolas", Font.BOLD, 18));
        lOs.setForeground(Color.WHITE);
        textStack.add(lMini); textStack.add(lSlash); textStack.add(lOs);
        titleRow.add(textStack);
        card.add(titleRow);

        card.add(Box.createVerticalStrut(6));

        // ── 2. "WELCOME TO MINI//OS" subtitle ────────────────────────────────
        JLabel lblWelcome = new JLabel("✦  WELCOME TO MINI // OS  ✦");
        lblWelcome.setFont(new Font("Consolas", Font.BOLD, 10));
        lblWelcome.setForeground(RetroTheme.ACCENT_PINK);
        lblWelcome.setHorizontalAlignment(SwingConstants.CENTER);
        lblWelcome.setMaximumSize(new Dimension(440, 18));
        lblWelcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblWelcome);

        card.add(Box.createVerticalStrut(8));

        // ── 3. "SYSTEM" large bold white ──────────────────────────────────────
        JLabel lblSystem = new JLabel("SYSTEM");
        lblSystem.setFont(new Font("Consolas", Font.BOLD, 30));
        lblSystem.setForeground(Color.WHITE);
        lblSystem.setHorizontalAlignment(SwingConstants.CENTER);
        lblSystem.setMaximumSize(new Dimension(440, 34));
        lblSystem.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblSystem);

        // ── 4. "INITIALIZATION" large bold cyan ───────────────────────────────
        JLabel lblInit = new JLabel("INITIALIZATION");
        lblInit.setFont(new Font("Consolas", Font.BOLD, 26));
        lblInit.setForeground(RetroTheme.ACCENT_CYAN);
        lblInit.setHorizontalAlignment(SwingConstants.CENTER);
        lblInit.setMaximumSize(new Dimension(440, 30));
        lblInit.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblInit);

        card.add(Box.createVerticalStrut(4));

        // ── 5. "Create Administrator (Root) Account" ──────────────────────────
        JLabel lblSub = new JLabel("Create Administrator (Root) Account // Priority 00");
        lblSub.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblSub.setForeground(new Color(110, 235, 255));
        lblSub.setHorizontalAlignment(SwingConstants.CENTER);
        lblSub.setMaximumSize(new Dimension(440, 18));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblSub);

        card.add(Box.createVerticalStrut(16));

        // ── 6. Input fields ───────────────────────────────────────────────────
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        fields.setAlignmentX(Component.CENTER_ALIGNMENT);
        fields.setMaximumSize(new Dimension(440, 115));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ADMIN USERNAME
        JLabel lblU = new JLabel("[ ROOT USER ]");
        lblU.setFont(new Font("Consolas", Font.BOLD, 10));
        lblU.setForeground(new Color(170, 160, 200));
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.28;
        fields.add(lblU, gbc);

        txtAdminUser = createCyberTextField("ROOT_PLAYER", 14);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.72;
        fields.add(txtAdminUser, gbc);

        // PASSWORD
        JLabel lblP = new JLabel("[ PASSWORD ]");
        lblP.setFont(new Font("Consolas", Font.BOLD, 10));
        lblP.setForeground(new Color(170, 160, 200));
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.28;
        fields.add(lblP, gbc);

        txtPass = createCyberPasswordField(14);
        txtPass.setText("password");
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.72;
        fields.add(txtPass, gbc);

        // CONFIRM PASSWORD
        JLabel lblC = new JLabel("[ CONFIRM ]");
        lblC.setFont(new Font("Consolas", Font.BOLD, 10));
        lblC.setForeground(new Color(170, 160, 200));
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.28;
        fields.add(lblC, gbc);

        txtConfirmPass = createCyberPasswordField(14);
        txtConfirmPass.setText("password");
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.72;
        fields.add(txtConfirmPass, gbc);

        card.add(fields);

        card.add(Box.createVerticalStrut(16));

        // ── 7. Yellow "INITIALIZE SYSTEM KERNEL" button ───────────────────────
        JButton btnInit = new JButton(">>  INITIALIZE SYSTEM KERNEL  <<") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                boolean hover = getModel().isRollover();
                boolean pressed = getModel().isPressed();

                // Drop shadow
                g2.setColor(new Color(0, 0, 0, 75));
                g2.fillRoundRect(2, 4, w - 4, h - 3, 8, 8);

                // Neon Outer Glow
                g2.setColor(new Color(255, 211, 25, hover ? 110 : 50));
                g2.setStroke(new BasicStroke(hover ? 4f : 2f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 9, 9);

                // Gradient background
                GradientPaint gp = hover
                        ? new GradientPaint(0, 0, new Color(255, 230, 80), 0, h, new Color(230, 175, 10))
                        : new GradientPaint(0, 0, new Color(255, 211, 25), 0, h, new Color(205, 150, 5));
                if (pressed) {
                    gp = new GradientPaint(0, 0, new Color(205, 150, 5), 0, h, new Color(170, 120, 0));
                }
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                // Top glossy glass highlight reflection
                g2.setColor(new Color(255, 255, 255, hover ? 80 : 50));
                g2.fillRoundRect(1, 1, w - 2, h / 2 - 1, 7, 7);

                // Border edge
                g2.setColor(hover ? Color.WHITE : new Color(255, 240, 160));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnInit.setFont(new Font("Consolas", Font.BOLD, 13));
        btnInit.setForeground(new Color(15, 10, 5));
        btnInit.setFocusPainted(false);
        btnInit.setOpaque(false);
        btnInit.setContentAreaFilled(false);
        btnInit.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        btnInit.setMaximumSize(new Dimension(440, 42));
        btnInit.setPreferredSize(new Dimension(440, 42));
        btnInit.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnInit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnInit.addActionListener(e -> {
            ArchivesPanel.playTone(1150, 60, 0.45f);
            doInitialize();
        });
        card.add(btnInit);

        card.add(Box.createVerticalStrut(14));

        // ── 8. Bottom row: status pill tags ──────────────────────────────────
        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        bottomRow.setOpaque(false);
        bottomRow.setMaximumSize(new Dimension(440, 30));
        bottomRow.setPreferredSize(new Dimension(440, 30));
        bottomRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblKernel = new JLabel("● KERNEL READY");
        lblKernel.setFont(new Font("Consolas", Font.BOLD, 10));
        lblKernel.setForeground(RetroTheme.ACCENT_YELLOW);
        bottomRow.add(lblKernel);

        JLabel lblDiv = new JLabel("//");
        lblDiv.setFont(new Font("Consolas", Font.BOLD, 10));
        lblDiv.setForeground(new Color(140, 120, 180));
        bottomRow.add(lblDiv);

        JLabel lblBoot = new JLabel("FIRST BOOT");
        lblBoot.setFont(new Font("Consolas", Font.BOLD, 10));
        lblBoot.setForeground(RetroTheme.ACCENT_PINK);
        bottomRow.add(lblBoot);

        card.add(bottomRow);

        // Center card on panel
        add(card);

        // Allow Enter key to submit
        txtAdminUser.addActionListener(e -> doInitialize());
        txtPass.addActionListener(e -> doInitialize());
        txtConfirmPass.addActionListener(e -> doInitialize());

        // Background particles animation & physics timer (~33 FPS)
        bgParticleTimer = new Timer(30, e -> {
            animTick += 1.0f;
            updateBackgroundParticles();
            repaint();
        });
        bgParticleTimer.start();

        // Mouse tracking across window for background particles
        try {
            globalMouseListener = event -> {
                if (event instanceof MouseEvent) {
                    MouseEvent me = (MouseEvent) event;
                    if (isShowing()) {
                        try {
                            Point p = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), SystemInitPanel.this);
                            mouseX = p.x;
                            mouseY = p.y;
                        } catch (Exception ignored) {}
                    }
                }
            };
            Toolkit.getDefaultToolkit().addAWTEventListener(globalMouseListener, AWTEvent.MOUSE_MOTION_EVENT_MASK);
        } catch (Exception ignored) {}
    }

    private JTextField createCyberTextField(String placeholder, int columns) {
        JTextField tf = new JTextField(placeholder, columns) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(new Color(15, 11, 28, 240));
                g2.fillRoundRect(0, 0, w, h, 6, 6);

                boolean focus = hasFocus();
                g2.setColor(focus ? RetroTheme.ACCENT_CYAN : new Color(65, 48, 95));
                g2.setStroke(new BasicStroke(focus ? 1.5f : 1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        tf.setFont(new Font("Consolas", Font.PLAIN, 12));
        tf.setForeground(RetroTheme.TEXT_WHITE);
        tf.setCaretColor(RetroTheme.ACCENT_CYAN);
        tf.setOpaque(false);
        tf.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        return tf;
    }

    private JPasswordField createCyberPasswordField(int columns) {
        JPasswordField pf = new JPasswordField(columns) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(new Color(15, 11, 28, 240));
                g2.fillRoundRect(0, 0, w, h, 6, 6);

                boolean focus = hasFocus();
                g2.setColor(focus ? RetroTheme.ACCENT_YELLOW : new Color(65, 48, 95));
                g2.setStroke(new BasicStroke(focus ? 1.5f : 1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        pf.setFont(new Font("Consolas", Font.PLAIN, 12));
        pf.setForeground(RetroTheme.TEXT_WHITE);
        pf.setCaretColor(RetroTheme.ACCENT_YELLOW);
        pf.setOpaque(false);
        pf.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        return pf;
    }

    private void initBackgroundParticles(int count) {
        bgDots.clear();
        Random rnd = new Random(31415);
        for (int i = 0; i < count; i++) {
            float px = rnd.nextFloat() * 1920f;
            float py = rnd.nextFloat() * 1080f;
            float vx = (rnd.nextFloat() - 0.5f) * 0.7f;
            float vy = (rnd.nextFloat() - 0.5f) * 0.7f;

            float randTier = rnd.nextFloat();
            float r;
            if (randTier < 0.50f) {
                r = 0.9f + rnd.nextFloat() * 0.5f;
            } else if (randTier < 0.85f) {
                r = 1.5f + rnd.nextFloat() * 0.6f;
            } else {
                r = 2.2f + rnd.nextFloat() * 0.4f;
            }

            float a = 0.40f + rnd.nextFloat() * 0.55f;
            float phase = rnd.nextFloat() * 6.28f;
            Color c = DOT_COLORS[rnd.nextInt(DOT_COLORS.length)];
            bgDots.add(new BgDotParticle(px, py, vx, vy, r, a, phase, c));
        }
    }

    private void updateBackgroundParticles() {
        int w = getWidth() > 0 ? getWidth() : 1280;
        int h = getHeight() > 0 ? getHeight() : 860;

        if (mouseX >= 0 && mouseY >= 0) {
            if (smoothMouseX < 0) {
                smoothMouseX = mouseX;
                smoothMouseY = mouseY;
            } else {
                smoothMouseX += (mouseX - smoothMouseX) * 0.15f;
                smoothMouseY += (mouseY - smoothMouseY) * 0.15f;
            }
        }

        for (BgDotParticle p : bgDots) {
            p.pulsePhase += 0.05f;
            p.currentRadius = p.baseRadius + (float) Math.sin(p.pulsePhase) * 0.3f;

            if (smoothMouseX >= 0 && smoothMouseY >= 0) {
                float dx = smoothMouseX - p.x;
                float dy = smoothMouseY - p.y;
                float distSq = dx * dx + dy * dy;
                float dist = (float) Math.sqrt(distSq);

                if (dist < 320f && dist > 1f) {
                    float ndx = dx / dist;
                    float ndy = dy / dist;

                    float pullFactor = (1.0f - dist / 320f);
                    float pullForce = pullFactor * 0.38f;

                    if (dist < 45f) {
                        float pushFactor = (1.0f - dist / 45f) * 0.50f;
                        pullForce -= pushFactor;
                    }

                    p.vx += ndx * pullForce;
                    p.vy += ndy * pullForce;

                    float perpX = -ndy;
                    float perpY = ndx;
                    p.vx += perpX * (pullFactor * 0.20f);
                    p.vy += perpY * (pullFactor * 0.20f);
                }
            }

            p.vx *= 0.965f;
            p.vy *= 0.965f;

            float speed = (float) Math.sqrt(p.vx * p.vx + p.vy * p.vy);
            if (speed < 0.18f) {
                p.vx += ((float) Math.random() - 0.5f) * 0.12f;
                p.vy += ((float) Math.random() - 0.5f) * 0.12f;
            } else if (speed > 3.0f) {
                p.vx = (p.vx / speed) * 3.0f;
                p.vy = (p.vy / speed) * 3.0f;
            }

            p.x += p.vx;
            p.y += p.vy;

            if (p.x < -20) p.x = w + 20;
            else if (p.x > w + 20) p.x = -20;
            if (p.y < -20) p.y = h + 20;
            else if (p.y > h + 20) p.y = -20;
        }
    }

    private void paintBackgroundParticles(Graphics2D g, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int numDots = bgDots.size();
        for (int i = 0; i < numDots; i++) {
            BgDotParticle p1 = bgDots.get(i);
            for (int j = i + 1; j < numDots; j++) {
                BgDotParticle p2 = bgDots.get(j);
                float dx = p1.x - p2.x;
                float dy = p1.y - p2.y;
                float distSq = dx * dx + dy * dy;
                if (distSq < 3600f) {
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

        if (smoothMouseX >= 0 && smoothMouseY >= 0) {
            for (BgDotParticle p : bgDots) {
                float dx = smoothMouseX - p.x;
                float dy = smoothMouseY - p.y;
                float distSq = dx * dx + dy * dy;
                if (distSq < 14400f) {
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

        for (BgDotParticle p : bgDots) {
            float r = Math.max(0.6f, p.currentRadius);
            int baseAlpha = (int) (p.alpha * 255f);
            baseAlpha = Math.max(15, Math.min(255, baseAlpha));

            int haloAlpha = (int) (baseAlpha * 0.32f);
            float haloR = r * 2.6f;
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), haloAlpha));
            g2.fillOval((int) (p.x - haloR), (int) (p.y - haloR), (int) (haloR * 2), (int) (haloR * 2));

            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), baseAlpha));
            g2.fillOval((int) (p.x - r), (int) (p.y - r), (int) (r * 2), (int) (r * 2));

            if (r > 1.8f) {
                g2.setColor(new Color(255, 255, 255, (int) (baseAlpha * 0.65f)));
                g2.fillOval((int) (p.x - 0.7f), (int) (p.y - 0.7f), 2, 2);
            }
        }

        g2.dispose();
    }

    private void doInitialize() {
        String username = txtAdminUser.getText().trim();
        String pass = new String(txtPass.getPassword());
        String confirm = new String(txtConfirmPass.getPassword());

        if (username.isEmpty()) {
            shake(txtAdminUser);
            return;
        }
        if (!pass.equals(confirm)) {
            JOptionPane.showMessageDialog(this,
                    "SYSTEM ERROR: Passwords do not match!\nRe-enter root credentials.",
                    "Password Mismatch", JOptionPane.ERROR_MESSAGE);
            txtConfirmPass.setText("");
            txtConfirmPass.requestFocus();
            return;
        }

        // Store root admin & proceed to Main Menu
        AppState.playerName = username;
        AppState.isGuest = false;
        parentFrame.showScreen(ArcadeOS.SCREEN_MAIN_MENU);
    }

    private void shake(Component c) {
        final int[] dx = { -6, 6, -4, 4, -2, 2, 0 };
        final Point orig = c.getLocation();
        Timer t = new Timer(40, null);
        final int[] step = { 0 };
        t.addActionListener(e -> {
            if (step[0] < dx.length) {
                c.setLocation(orig.x + dx[step[0]], orig.y);
                step[0]++;
            } else {
                c.setLocation(orig);
                t.stop();
            }
        });
        t.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();

        // 1. Deep solid background
        g2.setColor(new Color(8, 6, 18));
        g2.fillRect(0, 0, w, h);

        // 2. Ambient Cyber Horizon Glow centered behind the modal card
        try {
            float cx = w / 2f;
            float cy = h / 2f;
            float radius = Math.max(w, h) * 0.55f;
            Point2D center = new Point2D.Float(cx, cy);
            float[] dist = {0.0f, 0.45f, 1.0f};
            Color[] colors = {
                    new Color(15, 60, 75, 110), // Cyan/Teal aura
                    new Color(15, 14, 40, 50),
                    new Color(8, 6, 18, 0)
            };
            RadialGradientPaint rgp = new RadialGradientPaint(center, radius, dist, colors);
            g2.setPaint(rgp);
            g2.fillRect(0, 0, w, h);
        } catch (Exception ignored) {}

        // 3. Cyber Grid Lines
        g2.setColor(new Color(25, 20, 50, 140));
        g2.setStroke(new BasicStroke(1.0f));
        int gs = 36;
        for (int x = 0; x < w; x += gs) g2.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += gs) g2.drawLine(0, y, w, y);

        // Grid Crosshairs
        g2.setColor(new Color(255, 211, 25, 35));
        for (int x = 0; x < w; x += gs * 2) {
            for (int y = 0; y < h; y += gs * 2) {
                g2.drawLine(x - 2, y, x + 2, y);
                g2.drawLine(x, y - 2, x, y + 2);
            }
        }

        // 4. Background Interactive Dots
        paintBackgroundParticles(g2, w, h);

        // 5. System Footer
        g2.setFont(new Font("Consolas", Font.PLAIN, 10));
        g2.setColor(new Color(110, 95, 150));
        String footer = "MINI // OS FIRST-BOOT KERNEL INITIALIZATION PROTOCOL";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(footer, (w - fm.stringWidth(footer)) / 2, h - 16);

        g2.dispose();
    }
}
