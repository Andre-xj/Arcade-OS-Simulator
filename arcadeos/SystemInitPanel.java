package arcadeos;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Screen 0: SYSTEM INITIALIZATION — First Boot / Create Root Admin Account
 * Exactly matches the Figma first-page screenshot (centered modal on dark grid).
 */
public class SystemInitPanel extends JPanel {
    private JTextField txtAdminUser;
    private JPasswordField txtPass, txtConfirmPass;
    private ArcadeOS parentFrame;

    public SystemInitPanel(ArcadeOS parent) {
        this.parentFrame = parent;
        setBackground(RetroTheme.BG_DARK);
        setLayout(new GridBagLayout());

        // ── Center Card ──────────────────────────────────────────────────────
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Card background
                g2.setColor(new Color(22, 16, 46));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                // Cyan glow border
                g2.setColor(new Color(5, 200, 220, 220));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(480, 420));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        // ── 1. Robot icon + "MINI // OS" on same row ─────────────────────────
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel iconLbl = new JLabel(new ImageIcon(PixelSprites.createRobotSprite(38)));
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
        JLabel lblWelcome = new JLabel("WELCOME TO MINI//OS");
        lblWelcome.setFont(new Font("Consolas", Font.BOLD, 10));
        lblWelcome.setForeground(RetroTheme.ACCENT_PINK);
        lblWelcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblWelcome);

        card.add(Box.createVerticalStrut(10));

        // ── 3. "SYSTEM" large bold white ──────────────────────────────────────
        JLabel lblSystem = new JLabel("SYSTEM");
        lblSystem.setFont(new Font("Consolas", Font.BOLD, 32));
        lblSystem.setForeground(Color.WHITE);
        lblSystem.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblSystem);

        // ── 4. "INITIALIZATION" large bold cyan ───────────────────────────────
        JLabel lblInit = new JLabel("INITIALIZATION");
        lblInit.setFont(new Font("Consolas", Font.BOLD, 28));
        lblInit.setForeground(RetroTheme.ACCENT_CYAN);
        lblInit.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblInit);

        card.add(Box.createVerticalStrut(4));

        // ── 5. "Create Administrator (Root) Account" ──────────────────────────
        JLabel lblSub = new JLabel("Create Administrator (Root) Account");
        lblSub.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblSub.setForeground(new Color(80, 220, 240));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(lblSub);

        card.add(Box.createVerticalStrut(20));

        // ── 6. Input fields ───────────────────────────────────────────────────
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        fields.setAlignmentX(Component.CENTER_ALIGNMENT);
        fields.setMaximumSize(new Dimension(410, 110));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 4, 5, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ADMIN USERNAME
        JLabel lblU = new JLabel("ADMIN USERNAME");
        lblU.setFont(new Font("Consolas", Font.BOLD, 9));
        lblU.setForeground(new Color(140, 130, 180));
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.28;
        fields.add(lblU, gbc);

        txtAdminUser = RetroTheme.createRetroTextField("ROOT_PLAYER", 14);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.72;
        fields.add(txtAdminUser, gbc);

        // PASSWORD
        JLabel lblP = new JLabel("PASSWORD");
        lblP.setFont(new Font("Consolas", Font.BOLD, 9));
        lblP.setForeground(new Color(140, 130, 180));
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.28;
        fields.add(lblP, gbc);

        txtPass = RetroTheme.createRetroPasswordField(14);
        txtPass.setText("password");
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.72;
        fields.add(txtPass, gbc);

        // CONFIRM PASSWORD
        JLabel lblC = new JLabel("CONFIRM PASSWORD");
        lblC.setFont(new Font("Consolas", Font.BOLD, 9));
        lblC.setForeground(new Color(140, 130, 180));
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.28;
        fields.add(lblC, gbc);

        txtConfirmPass = RetroTheme.createRetroPasswordField(14);
        txtConfirmPass.setText("password");
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.72;
        fields.add(txtConfirmPass, gbc);

        card.add(fields);

        card.add(Box.createVerticalStrut(18));

        // ── 7. Yellow "INITIALIZE SYSTEM KERNEL" button ───────────────────────
        JButton btnInit = new JButton("▶  INITIALIZE SYSTEM KERNEL") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnInit.setFont(new Font("Consolas", Font.BOLD, 13));
        btnInit.setForeground(Color.BLACK);
        btnInit.setFocusPainted(false);
        btnInit.setOpaque(false);
        btnInit.setContentAreaFilled(false);
        btnInit.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        btnInit.setMaximumSize(new Dimension(410, 40));
        btnInit.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnInit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnInit.addActionListener(e -> doInitialize());
        card.add(btnInit);

        card.add(Box.createVerticalStrut(12));

        // ── 8. Bottom row: warning left + "FIRST BOOT" right ─────────────────
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);
        bottomRow.setMaximumSize(new Dimension(410, 20));
        bottomRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblKernel = new JLabel("▶ KERNEL WAITING FOR ROOT CREDENTIALS");
        lblKernel.setFont(new Font("Consolas", Font.BOLD, 8));
        lblKernel.setForeground(RetroTheme.ACCENT_YELLOW);
        bottomRow.add(lblKernel, BorderLayout.WEST);

        JLabel lblBoot = new JLabel("FIRST BOOT");
        lblBoot.setFont(new Font("Consolas", Font.BOLD, 8));
        lblBoot.setForeground(RetroTheme.ACCENT_PINK);
        bottomRow.add(lblBoot, BorderLayout.EAST);

        card.add(bottomRow);

        // Center card on panel
        add(card);

        // Allow Enter key to submit
        txtAdminUser.addActionListener(e -> doInitialize());
        txtPass.addActionListener(e -> doInitialize());
        txtConfirmPass.addActionListener(e -> doInitialize());
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

    /** Quick horizontal shake animation on a component */
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
        Graphics2D g2 = (Graphics2D) g;
        // Subtle dark grid background matching Figma screenshot
        g2.setColor(new Color(18, 14, 34));
        int gs = 32;
        for (int x = 0; x < getWidth(); x += gs)
            g2.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += gs)
            g2.drawLine(0, y, getWidth(), y);
    }
}
