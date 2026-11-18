package arcadeos;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;

/**
 * Screen 1: PLAYER LOGIN / ARCADE SECURITY GATE
 * Upgraded with interactive floating background micro-particles,
 * radial atmospheric glow, frosted acrylic glass card, holographic mascot pedestal,
 * and high-contrast glowing cyber buttons.
 */
public class LoginPanel extends JPanel {
    private JTextField txtUser;
    private JPasswordField txtPass;
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
            new Color(255, 42, 141),   // Hot Pink
            new Color(5, 217, 232),    // Electric Cyan
            new Color(255, 211, 25),   // Radiant Yellow
            new Color(180, 0, 255),    // Neon Purple
            new Color(57, 255, 20),    // Cyber Green
            new Color(255, 110, 180),  // Pastel Pink
            new Color(100, 240, 255)   // Ice Cyan
    };

    private float mouseX = -999f;
    private float mouseY = -999f;
    private float smoothMouseX = -999f;
    private float smoothMouseY = -999f;
    private Timer bgParticleTimer;
    private float animTick = 0f;
    private AWTEventListener globalMouseListener;

    public LoginPanel(ArcadeOS parent) {
        this.parentFrame = parent;
        setBackground(RetroTheme.BG_DEEPER);
        setLayout(new GridBagLayout());

        // Initialize Background Interactive Circular Micro-Dots
        initBackgroundParticles(95);

        // Center card modal with Frosted Acrylic Glassmorphism & Cyber Accents
        JPanel modal = new JPanel() {
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

                // Subtle Outer Neon Rim Glow (Pink to Cyan)
                g2.setColor(new Color(255, 42, 141, 30));
                g2.setStroke(new BasicStroke(5f));
                g2.drawRoundRect(2, 2, w - 5, h - 5, 20, 20);

                // 2. Frosted Dark Glass Gradient Body
                GradientPaint bgGrad = new GradientPaint(
                        0, 0, new Color(24, 18, 44, 248),
                        0, h, new Color(11, 8, 22, 252)
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

                // 3. Cyber Dual-Tone Neon Border (Hot Pink top to Electric Cyan bottom)
                GradientPaint borderGrad = new GradientPaint(
                        0, 0, new Color(255, 42, 141, 235),
                        0, h, new Color(5, 217, 232, 205)
                );
                g2.setPaint(borderGrad);
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, 20, 20);

                // 4. Cyber Corner HUD Bracket Accents
                g2.setStroke(new BasicStroke(2.5f));
                g2.setColor(RetroTheme.ACCENT_PINK);
                // Top-Left corner
                g2.drawLine(4, 16, 4, 4);
                g2.drawLine(4, 4, 16, 4);
                // Top-Right corner
                g2.drawLine(w - 17, 4, w - 5, 4);
                g2.drawLine(w - 5, 4, w - 5, 16);

                g2.setColor(RetroTheme.ACCENT_CYAN);
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
        modal.setOpaque(false);
        modal.setPreferredSize(new Dimension(480, 490));
        modal.setLayout(new BoxLayout(modal, BoxLayout.Y_AXIS));
        modal.setBorder(BorderFactory.createEmptyBorder(20, 34, 20, 34));

        // 1. Top Header Marquee "MINI // OS" pill badge
        JPanel headerTag = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(255, 42, 141, 28));
                g2.fillRoundRect(0, 0, w, h, 6, 6);
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        headerTag.setOpaque(false);
        headerTag.setLayout(new FlowLayout(FlowLayout.CENTER, 8, 4));
        headerTag.setMaximumSize(new Dimension(360, 30));

        JLabel lblTagDotL = new JLabel("+");
        lblTagDotL.setFont(new Font("Consolas", Font.BOLD, 11));
        lblTagDotL.setForeground(RetroTheme.ACCENT_CYAN);

        JLabel lblMiniOs = new JLabel("MINI // OS");
        lblMiniOs.setFont(new Font("Consolas", Font.BOLD, 13));
        lblMiniOs.setForeground(Color.WHITE);

        JLabel lblTagDotR = new JLabel("+");
        lblTagDotR.setFont(new Font("Consolas", Font.BOLD, 11));
        lblTagDotR.setForeground(RetroTheme.ACCENT_PINK);

        headerTag.add(lblTagDotL);
        headerTag.add(lblMiniOs);
        headerTag.add(lblTagDotR);
        headerTag.setAlignmentX(Component.CENTER_ALIGNMENT);
        modal.add(headerTag);

        modal.add(Box.createVerticalStrut(14));

        // 2. Animated Mascot Robot with Holographic Base Pedestal
        JPanel mascotBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = getWidth() / 2;
                int cy = getHeight() - 8;

                // Glowing Holographic Pedestal Disk
                float pulse = (float) Math.sin(animTick * 0.08f) * 0.2f + 0.8f;
                int diskW = (int) (80 * pulse);
                int diskH = (int) (14 * pulse);

                // Outer aura ring
                g2.setColor(new Color(5, 217, 232, (int) (40 * pulse)));
                g2.fillOval(cx - diskW / 2 - 4, cy - diskH / 2 - 2, diskW + 8, diskH + 4);

                // Inner bright neon disk
                g2.setColor(new Color(5, 217, 232, (int) (125 * pulse)));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx - diskW / 2, cy - diskH / 2, diskW, diskH);

                // Subtle projector light beam lines
                g2.setColor(new Color(5, 217, 232, 28));
                g2.drawLine(cx - diskW / 3, cy, cx - 18, 12);
                g2.drawLine(cx + diskW / 3, cy, cx + 18, 12);

                g2.dispose();
            }
        };
        mascotBox.setOpaque(false);
        mascotBox.setLayout(new BorderLayout());
        mascotBox.setMaximumSize(new Dimension(110, 68));
        mascotBox.setPreferredSize(new Dimension(110, 68));
        mascotBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblRobot = new JLabel(new ImageIcon(PixelSprites.createRobotSprite(50))) {
            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int bobOffset = (int) (Math.sin(animTick * 0.07f) * 3f);
                g2.translate(0, bobOffset);
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        lblRobot.setHorizontalAlignment(SwingConstants.CENTER);
        mascotBox.add(lblRobot, BorderLayout.CENTER);
        modal.add(mascotBox);

        modal.add(Box.createVerticalStrut(8));

        // 3. Security Gate Subtitle with Live Status Indicator
        JPanel subRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        subRow.setOpaque(false);
        subRow.setMaximumSize(new Dimension(420, 20));
        subRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblStatusDot = new JLabel("*");
        lblStatusDot.setFont(new Font("Consolas", Font.BOLD, 12));
        lblStatusDot.setForeground(RetroTheme.ACCENT_GREEN);

        JLabel lblGate = new JLabel("ARCADE SECURITY GATE // SYSTEM ACCESS");
        lblGate.setFont(new Font("Consolas", Font.BOLD, 11));
        lblGate.setForeground(RetroTheme.ACCENT_CYAN);

        subRow.add(lblStatusDot);
        subRow.add(lblGate);
        modal.add(subRow);

        modal.add(Box.createVerticalStrut(4));

        // 4. Main Title "PLAYER LOGIN"
        JLabel lblTitle = new JLabel("PLAYER LOGIN");
        lblTitle.setFont(new Font("Consolas", Font.BOLD, 22));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setMaximumSize(new Dimension(420, 30));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        modal.add(lblTitle);

        modal.add(Box.createVerticalStrut(14));

        // 5. Input Fields Panel with Frosted Cyber Fields
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        fieldsPanel.setMaximumSize(new Dimension(420, 88));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblU = new JLabel("[ USERNAME ]");
        lblU.setFont(new Font("Consolas", Font.BOLD, 10));
        lblU.setForeground(new Color(165, 155, 195));
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.28;
        fieldsPanel.add(lblU, gbc);

        txtUser = createCyberTextField("ENTER HANDLE", 14);
        txtUser.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtUser.getText().equals("ENTER HANDLE")) {
                    txtUser.setText("");
                    txtUser.setForeground(RetroTheme.TEXT_WHITE);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (txtUser.getText().trim().isEmpty()) {
                    txtUser.setText("ENTER HANDLE");
                    txtUser.setForeground(RetroTheme.TEXT_DIM);
                }
            }
        });
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.72;
        fieldsPanel.add(txtUser, gbc);

        JLabel lblP = new JLabel("[ PASSWORD ]");
        lblP.setFont(new Font("Consolas", Font.BOLD, 10));
        lblP.setForeground(new Color(165, 155, 195));
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.28;
        fieldsPanel.add(lblP, gbc);

        txtPass = createCyberPasswordField(14);
        txtPass.setText("password");
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.72;
        fieldsPanel.add(txtPass, gbc);

        modal.add(fieldsPanel);

        modal.add(Box.createVerticalStrut(16));

        // 6. Sign In Button (3D Neon Pink Gradient CTA with Gloss Highlight)
        JButton btnSignIn = new JButton(">>  SIGN IN  <<") {
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
                g2.setColor(new Color(255, 42, 141, hover ? 110 : 45));
                g2.setStroke(new BasicStroke(hover ? 4f : 2f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 9, 9);

                // Gradient background
                GradientPaint gp = hover
                        ? new GradientPaint(0, 0, new Color(255, 75, 160), 0, h, new Color(225, 20, 115))
                        : new GradientPaint(0, 0, new Color(255, 42, 141), 0, h, new Color(195, 15, 95));
                if (pressed) {
                    gp = new GradientPaint(0, 0, new Color(195, 15, 95), 0, h, new Color(160, 10, 80));
                }
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                // Top glossy glass highlight reflection
                g2.setColor(new Color(255, 255, 255, hover ? 65 : 40));
                g2.fillRoundRect(1, 1, w - 2, h / 2 - 1, 7, 7);

                // Border edge
                g2.setColor(hover ? Color.WHITE : new Color(255, 190, 225));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnSignIn.setFont(new Font("Consolas", Font.BOLD, 13));
        btnSignIn.setForeground(Color.WHITE);
        btnSignIn.setFocusPainted(false);
        btnSignIn.setOpaque(false);
        btnSignIn.setContentAreaFilled(false);
        btnSignIn.setBorder(BorderFactory.createEmptyBorder(9, 0, 9, 0));
        btnSignIn.setMaximumSize(new Dimension(420, 40));
        btnSignIn.setPreferredSize(new Dimension(420, 40));
        btnSignIn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSignIn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSignIn.addActionListener(e -> {
            ArchivesPanel.playTone(1050, 60, 0.45f);
            doLogin(false);
        });
        modal.add(btnSignIn);

        modal.add(Box.createVerticalStrut(8));

        // 7. Login as Guest Button (Frosted Cyber Pill)
        JButton btnGuest = new JButton("[*]  LOGIN AS GUEST // TEMPORARY SESSION") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                boolean hover = getModel().isRollover();

                // Frosted background
                g2.setColor(hover ? new Color(36, 26, 65, 240) : new Color(20, 14, 38, 220));
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                // Top subtle highlight
                g2.setColor(new Color(255, 255, 255, hover ? 25 : 12));
                g2.fillRoundRect(1, 1, w - 2, h / 2 - 1, 7, 7);

                // Glowing Cyan/Purple Border
                Color bc = hover ? RetroTheme.ACCENT_CYAN : new Color(5, 217, 232, 125);
                g2.setColor(bc);
                g2.setStroke(new BasicStroke(hover ? 1.6f : 1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnGuest.setFont(new Font("Consolas", Font.BOLD, 10));
        btnGuest.setForeground(new Color(195, 228, 255));
        btnGuest.setFocusPainted(false);
        btnGuest.setOpaque(false);
        btnGuest.setContentAreaFilled(false);
        btnGuest.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        btnGuest.setMaximumSize(new Dimension(420, 34));
        btnGuest.setPreferredSize(new Dimension(420, 34));
        btnGuest.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnGuest.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnGuest.addActionListener(e -> {
            ArchivesPanel.playTone(850, 60, 0.4f);
            doLogin(true);
        });
        modal.add(btnGuest);

        modal.add(Box.createVerticalStrut(12));

        // 8. Warning text badge
        JPanel warnPill = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 70, 90, 22));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(255, 70, 90, 80));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        warnPill.setOpaque(false);
        warnPill.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 3));
        warnPill.setMaximumSize(new Dimension(380, 26));
        warnPill.setPreferredSize(new Dimension(380, 26));

        JLabel lblWarn = new JLabel("! GUEST MODE (READ-ONLY SESSION)");
        lblWarn.setFont(new Font("Consolas", Font.PLAIN, 9));
        lblWarn.setForeground(new Color(255, 125, 145));
        warnPill.add(lblWarn);
        warnPill.setAlignmentX(Component.CENTER_ALIGNMENT);
        modal.add(warnPill);

        // Add modal to center
        add(modal);

        // Allow pressing Enter to submit
        txtUser.addActionListener(e -> doLogin(false));
        txtPass.addActionListener(e -> doLogin(false));

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
                            Point p = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), LoginPanel.this);
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
        tf.setForeground(RetroTheme.TEXT_DIM);
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
                g2.setColor(focus ? RetroTheme.ACCENT_PINK : new Color(65, 48, 95));
                g2.setStroke(new BasicStroke(focus ? 1.5f : 1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        pf.setFont(new Font("Consolas", Font.PLAIN, 12));
        pf.setForeground(RetroTheme.TEXT_WHITE);
        pf.setCaretColor(RetroTheme.ACCENT_PINK);
        pf.setOpaque(false);
        pf.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        return pf;
    }

    private void initBackgroundParticles(int count) {
        bgDots.clear();
        Random rnd = new Random(2026);
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

    private void doLogin(boolean asGuest) {
        if (asGuest) {
            AppState.playerName = "GUEST_USER";
            AppState.isGuest = true;
        } else {
            String u = txtUser.getText().trim();
            if (u.isEmpty() || u.equals("ENTER HANDLE")) {
                AppState.playerName = "PLAYER_01";
            } else {
                AppState.playerName = u;
            }
            AppState.isGuest = false;
        }
        parentFrame.showScreen(ArcadeOS.SCREEN_MAIN_MENU);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();

        // 1. Solid deep background
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
                    new Color(55, 18, 75, 110), // Deep Neon Violet aura
                    new Color(20, 14, 40, 50),
                    new Color(8, 6, 18, 0)
            };
            RadialGradientPaint rgp = new RadialGradientPaint(center, radius, dist, colors);
            g2.setPaint(rgp);
            g2.fillRect(0, 0, w, h);
        } catch (Exception ignored) {}

        // 3. Cyber Grid Lines with subtle perspective & falloff
        g2.setColor(new Color(28, 20, 52, 140));
        g2.setStroke(new BasicStroke(1.0f));
        int gridSize = 36;
        for (int x = 0; x < w; x += gridSize) {
            g2.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += gridSize) {
            g2.drawLine(0, y, w, y);
        }

        // Grid Intersection Crosshairs
        g2.setColor(new Color(5, 217, 232, 40));
        for (int x = 0; x < w; x += gridSize * 2) {
            for (int y = 0; y < h; y += gridSize * 2) {
                g2.drawLine(x - 2, y, x + 2, y);
                g2.drawLine(x, y - 2, x, y + 2);
            }
        }

        // 4. Interactive Floating Background Dots & Constellation Lines
        paintBackgroundParticles(g2, w, h);

        // 5. Bottom System Footer
        g2.setFont(new Font("Consolas", Font.PLAIN, 10));
        g2.setColor(new Color(110, 95, 150));
        String footer = "MINI // OS KERNEL v2.4.0 — ARCH SECURE AUTH GATEWAY";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(footer, (w - fm.stringWidth(footer)) / 2, h - 16);

        g2.dispose();
    }
}
