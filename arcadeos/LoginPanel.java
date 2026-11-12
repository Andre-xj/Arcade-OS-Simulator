package arcadeos;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Screen 1: PLAYER LOGIN / ARCADE SECURITY GATE
 * Exactly matching the Figma prototype login modal (centered on dark grid).
 */
public class LoginPanel extends JPanel {
    private JTextField txtUser;
    private JPasswordField txtPass;
    private ArcadeOS parentFrame;

    public LoginPanel(ArcadeOS parent) {
        this.parentFrame = parent;
        setBackground(RetroTheme.BG_DARK);
        setLayout(new GridBagLayout());

        // Center card modal
        JPanel modal = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Card background
                g2.setColor(RetroTheme.PANEL_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // Glowing border
                g2.setColor(new Color(90, 45, 130));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        modal.setOpaque(false);
        modal.setPreferredSize(new Dimension(440, 480));
        modal.setLayout(new BoxLayout(modal, BoxLayout.Y_AXIS));
        modal.setBorder(BorderFactory.createEmptyBorder(20, 30, 25, 30));

        // 1. Top Header Marquee "MINI // OS"
        JPanel headerTag = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        headerTag.setOpaque(false);
        headerTag.setMaximumSize(new Dimension(380, 36));
        headerTag.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_PINK, 1),
                BorderFactory.createEmptyBorder(4, 20, 4, 20)
        ));
        JLabel lblMiniOs = new JLabel("MINI // OS");
        lblMiniOs.setFont(new Font("Consolas", Font.BOLD, 14));
        lblMiniOs.setForeground(RetroTheme.ACCENT_PINK);
        headerTag.add(lblMiniOs);
        modal.add(headerTag);

        modal.add(Box.createVerticalStrut(20));

        // 2. Green Mascot Robot Avatar
        JLabel lblRobot = new JLabel(new ImageIcon(PixelSprites.createRobotSprite(52)));
        lblRobot.setAlignmentX(Component.CENTER_ALIGNMENT);
        modal.add(lblRobot);

        modal.add(Box.createVerticalStrut(10));

        // 3. Security Gate Subtitle
        JLabel lblGate = new JLabel("ARCADE SECURITY GATE");
        lblGate.setFont(new Font("Consolas", Font.BOLD, 10));
        lblGate.setForeground(RetroTheme.ACCENT_CYAN);
        lblGate.setAlignmentX(Component.CENTER_ALIGNMENT);
        modal.add(lblGate);

        modal.add(Box.createVerticalStrut(4));

        // 4. Main Title "PLAYER LOGIN"
        JLabel lblTitle = new JLabel("PLAYER LOGIN");
        lblTitle.setFont(new Font("Consolas", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        modal.add(lblTitle);

        modal.add(Box.createVerticalStrut(22));

        // 5. Input Fields Panel
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        fieldsPanel.setMaximumSize(new Dimension(380, 80));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblU = new JLabel("USERNAME");
        lblU.setFont(new Font("Consolas", Font.BOLD, 10));
        lblU.setForeground(RetroTheme.TEXT_DIM);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        fieldsPanel.add(lblU, gbc);

        txtUser = RetroTheme.createRetroTextField("ENTER HANDLE", 14);
        txtUser.setForeground(RetroTheme.TEXT_DIM);
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
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.7;
        fieldsPanel.add(txtUser, gbc);

        JLabel lblP = new JLabel("PASSWORD");
        lblP.setFont(new Font("Consolas", Font.BOLD, 10));
        lblP.setForeground(RetroTheme.TEXT_DIM);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        fieldsPanel.add(lblP, gbc);

        txtPass = RetroTheme.createRetroPasswordField(14);
        txtPass.setText("password");
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.7;
        fieldsPanel.add(txtPass, gbc);

        modal.add(fieldsPanel);

        modal.add(Box.createVerticalStrut(18));

        // 6. Sign In Button
        JButton btnSignIn = new JButton("• SIGN IN") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnSignIn.setFont(new Font("Consolas", Font.BOLD, 12));
        btnSignIn.setForeground(Color.WHITE);
        btnSignIn.setFocusPainted(false);
        btnSignIn.setOpaque(false);
        btnSignIn.setContentAreaFilled(false);
        btnSignIn.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        btnSignIn.setMaximumSize(new Dimension(380, 36));
        btnSignIn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSignIn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSignIn.addActionListener(e -> doLogin(false));
        modal.add(btnSignIn);

        modal.add(Box.createVerticalStrut(8));

        // 7. Login as Guest Button
        JButton btnGuest = new JButton(": LOGIN AS GUEST // TEMPORARY SESSION") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(RetroTheme.BTN_DARK);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(RetroTheme.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnGuest.setFont(new Font("Consolas", Font.BOLD, 10));
        btnGuest.setForeground(new Color(160, 150, 200));
        btnGuest.setFocusPainted(false);
        btnGuest.setOpaque(false);
        btnGuest.setContentAreaFilled(false);
        btnGuest.setBorder(BorderFactory.createEmptyBorder(7, 0, 7, 0));
        btnGuest.setMaximumSize(new Dimension(380, 32));
        btnGuest.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnGuest.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnGuest.addActionListener(e -> doLogin(true));
        modal.add(btnGuest);

        modal.add(Box.createVerticalStrut(10));

        // 8. Warning text
        JLabel lblWarn = new JLabel("Some sessions are read-only and not saved.");
        lblWarn.setFont(new Font("Consolas", Font.PLAIN, 9));
        lblWarn.setForeground(new Color(255, 80, 100));
        lblWarn.setAlignmentX(Component.CENTER_ALIGNMENT);
        modal.add(lblWarn);

        // Add modal to center
        add(modal);

        // Allow pressing Enter to submit
        txtUser.addActionListener(e -> doLogin(false));
        txtPass.addActionListener(e -> doLogin(false));
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
        Graphics2D g2 = (Graphics2D) g;
        // Background grid pattern matching Figma screenshot
        g2.setColor(new Color(20, 16, 36));
        int gridSize = 32;
        for (int x = 0; x < getWidth(); x += gridSize) {
            g2.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += gridSize) {
            g2.drawLine(0, y, getWidth(), y);
        }

        // Bottom footer string
        g2.setFont(new Font("Consolas", Font.PLAIN, 9));
        g2.setColor(new Color(80, 70, 110));
        String footer = "BUILT ON // LOCAL ARCH CODE - v1.0.4";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(footer, (getWidth() - fm.stringWidth(footer)) / 2, getHeight() - 15);
    }
}
