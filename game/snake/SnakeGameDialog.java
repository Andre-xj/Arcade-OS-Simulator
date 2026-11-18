package game.snake;

import arcadeos.PixelSprites;
import arcadeos.RetroTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * HEAP vs STACK SNAKE - the Mission 05 arcade cabinet window.
 *
 * STEP 2: the game is a full-size top-level window, not a dialog over Mission 05.
 * It opens maximised on the screen that holds the main window, over an opaque
 * dark background, with nothing from Mission 05 or the IDE behind it. The main
 * frame is only disabled while the game is open and is re-enabled and refocused
 * when the game closes, so Mission 05 is returned exactly as it was left.
 *
 * The class keeps its historical name so the two shared call sites in
 * {@code arcadeos} need no edit - only the launch call is shared.
 */
public final class SnakeGameDialog extends JFrame {

    private static final Font F_TITLE = new Font("Consolas", Font.BOLD, 13);
    private static final Font F_SMALL = new Font("Consolas", Font.BOLD, 11);

    private final JFrame owner;
    private final SnakeModel model;
    private final SnakeGamePanel gamePanel;
    private final JButton muteButton;
    /** A5: level label that refreshHeader() updates when the level changes. */
    private JLabel headerLevelLabel;

    /** True when the PAUSED state was forced by focus loss, so focus regain clears it. */
    private boolean autoPaused = false;

    SnakeGameDialog(JFrame owner, SnakeLevel level) {
        super("HEAP vs STACK SNAKE // MISSION 05");
        this.owner = owner;
        this.model = new SnakeModel(level == null ? SnakeLevel.level1() : level);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout());

        muteButton = RetroTheme.createRetroButton(muteLabel(), RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        // STEP 1: header buttons must never take keyboard focus, or SPACE would
        // activate the focused button instead of acting as the game key.
        muteButton.setFocusable(false);

        gamePanel = new SnakeGamePanel(this, model);
        muteButton.addActionListener(e -> {
            GameSfx.toggleMute();
            refreshMute();
            gamePanel.requestFocusNow();
        });

        add(createHeader(), BorderLayout.NORTH);
        add(gamePanel, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { stopGame(); }
            @Override public void windowClosed(WindowEvent e) { closeCleanup(); }
        });
        // STEP 2: pause when the window loses focus or is minimised. STEP 1: hand
        // focus back to the game panel whenever the window regains it.
        addWindowFocusListener(new WindowAdapter() {
            @Override public void windowLostFocus(WindowEvent e) { pauseForFocus(); }
            @Override public void windowGainedFocus(WindowEvent e) { resumeAfterFocus(); }
        });
        addWindowStateListener(e -> {
            if ((e.getNewState() & Frame.ICONIFIED) != 0) pauseForFocus();
        });
    }

    // =====================================================================
    // LAUNCH
    // =====================================================================
    /** Convenience launcher that mirrors the other cabinets. */
    static void launch(JFrame parent) {
        launch(parent, SnakeLevel.level1());
    }

    /** Launcher that plays a specific level, e.g. an arena built from a picked segment. */
    static void launch(JFrame parent, SnakeLevel level) {
        new SnakeGameDialog(parent, level).open();
    }

    /**
     * Mission 05 entry point: play the picked segment as the snake's heap arena.
     * Everything about building the arena lives here, so the Mission 05 panel only
     * has to make one launch call. Returns a one-line report of the values the game
     * actually received.
     */
    public static String launchFromSegment(JFrame parent, String name, int base, int limit) {
        return launchFromSegment(parent, name, base, limit, null);
    }

    /** C2: Overload that accepts a SnakeReviewListener to review faults in the simulator. */
    public static String launchFromSegment(JFrame parent, String name, int base, int limit, SnakeReviewListener reviewListener) {
        extractTableRowsFromParent(parent);
        SnakeLevel level = SnakeLevel.fromSegment(name, base, limit);
        String report = "ARCADE INPUT: segment [" + name + "] base=" + base + " limit=" + limit
                + "  ->  rowUnits=" + level.rowUnits + " arena rows=" + (level.heapLimit / level.rowUnits)
                + " cols=" + level.cols
                + " effective limit=" + level.heapLimit
                + (level.sourceLimit != level.heapLimit ? " (scaled from " + level.sourceLimit + ")" : "");
        System.out.println(report);
        SnakeGameDialog dlg = new SnakeGameDialog(parent, level);
        if (reviewListener != null) {
            dlg.gamePanel.setReviewListener(reviewListener);
        }
        dlg.open();
        return report;
    }

    /** Mission 05 entry point when no segment is picked: play the default level. */
    public static String launchDefault(JFrame parent) {
        return launchDefault(parent, null);
    }

    /** C2: Overload with SnakeReviewListener. */
    public static String launchDefault(JFrame parent, SnakeReviewListener reviewListener) {
        extractTableRowsFromParent(parent);
        SnakeLevel level = SnakeLevel.level1();
        String report = "ARCADE INPUT: no segment picked  ->  default Level 1 (heap base=" + level.heapBase
                + " limit=" + level.heapLimit + " cols=" + level.cols + ")";
        System.out.println(report);
        SnakeGameDialog dlg = new SnakeGameDialog(parent, level);
        if (reviewListener != null) {
            dlg.gamePanel.setReviewListener(reviewListener);
        }
        dlg.open();
        return report;
    }

    private static void extractTableRowsFromParent(JFrame parent) {
        if (parent == null) return;
        try {
            findAndStoreTable(parent);
        } catch (Throwable ignored) { }
    }

    private static void findAndStoreTable(java.awt.Container container) {
        if (container == null) return;
        for (java.awt.Component comp : container.getComponents()) {
            if (comp instanceof javax.swing.JTable) {
                javax.swing.JTable table = (javax.swing.JTable) comp;
                if (table.getColumnCount() >= 4 && table.getRowCount() >= 1) {
                    boolean hasSwatch = table.getColumnCount() >= 6;
                    int colId = hasSwatch ? 1 : 0;
                    int colName = hasSwatch ? 2 : 1;
                    int colBase = hasSwatch ? 3 : 2;
                    int colLimit = hasSwatch ? 4 : 3;
                    java.util.List<SnakeLevel.SegmentDef> rows = new java.util.ArrayList<>();
                    for (int r = 0; r < Math.min(3, table.getRowCount()); r++) {
                        String idStr = String.valueOf(table.getValueAt(r, colId));
                        int id = r + 1;
                        try { id = Integer.parseInt(idStr.trim()) + 1; } catch (Exception ignored) {}
                        String name = String.valueOf(table.getValueAt(r, colName));
                        int base = Integer.parseInt(String.valueOf(table.getValueAt(r, colBase)).trim());
                        int limit = Integer.parseInt(String.valueOf(table.getValueAt(r, colLimit)).trim());
                        boolean ro = (r == 2);
                        rows.add(new SnakeLevel.SegmentDef(id, name, base, limit, ro));
                    }
                    if (rows.size() >= 3) {
                        SnakeLevel.selectedTableRows.clear();
                        SnakeLevel.selectedTableRows.addAll(rows);
                        return;
                    }
                }
            } else if (comp instanceof java.awt.Container) {
                findAndStoreTable((java.awt.Container) comp);
            }
        }
    }

    /**
     * Opens the game maximised on the main window's screen, disabling the main
     * frame while open.
     *
     * (a) The maximised bounds are the screen's *usable* area, not its full
     * bounds: a plain MAXIMIZED_BOTH window runs under the taskbar, so the last
     * strip of the layout is drawn below the visible screen and shows through the
     * translucent taskbar as a clipped, faint copy. Clamping to the work area
     * keeps every strip on screen and makes the content height the real height.
     */
    void open() {
        GraphicsConfiguration gc = owner != null ? owner.getGraphicsConfiguration()
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle b = gc.getBounds();
        Insets si = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        Rectangle usable = new Rectangle(b.x + si.left, b.y + si.top,
                Math.max(200, b.width - si.left - si.right),
                Math.max(200, b.height - si.top - si.bottom));
        setMaximizedBounds(usable);
        setLocation(usable.x, usable.y);
        setSize(usable.width, usable.height);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        if (owner != null) owner.setEnabled(false);
        setVisible(true);
        SwingUtilities.invokeLater(gamePanel::requestFocusNow);
    }

    // =====================================================================
    // STATE / CHROME HOOKS USED BY THE PANEL
    // =====================================================================
    void refreshMute() {
        muteButton.setText(muteLabel());
        muteButton.setForeground(GameSfx.isMuted() ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_CYAN);
    }

    void requestExit() {
        stopGame();
        dispose();
    }

    void stopGame() {
        if (gamePanel != null) gamePanel.stop();
    }

    /** Re-enables and refocuses the main frame so Mission 05 carries on untouched. */
    private void closeCleanup() {
        stopGame();
        if (owner != null) {
            owner.setEnabled(true);
            owner.toFront();
            owner.requestFocus();
            SwingUtilities.invokeLater(owner::requestFocus);
        }
    }

    private void pauseForFocus() {
        if (model.phase == SnakeModel.Phase.PLAYING && !model.paused) {
            model.paused = true;
            autoPaused = true;
            gamePanel.repaint();
        }
    }

    private void resumeAfterFocus() {
        if (autoPaused) {
            model.paused = false;
            autoPaused = false;
            gamePanel.repaint();
        }
        if (gamePanel != null) gamePanel.requestFocusNow();
    }

    private static String muteLabel() {
        return GameSfx.isMuted() ? "SOUND OFF" : "SOUND ON";
    }

    // =====================================================================
    // A5: update the header whenever the level changes.
    // =====================================================================
    void refreshHeader() {
        if (headerLevelLabel != null) {
            headerLevelLabel.setText("LEVEL " + model.level.number + " - " + model.level.name.toUpperCase());
        }
    }

    // =====================================================================
    // SLIM HEADER - title, level, SOUND, EXIT (all buttons non-focusable)
    // =====================================================================
    private JPanel createHeader() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(new Color(14, 20, 16));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 120, 70)),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(new JLabel(new ImageIcon(PixelSprites.createRobotSprite(20))));
        // FIX 3: the window title bar already carries the game name, so the
        // header shows only the level - no second copy of the title, no mission text.
        // A5: store in field so refreshHeader() can update it on level advance.
        headerLevelLabel = new JLabel("LEVEL " + model.level.number + " - " + model.level.name.toUpperCase());
        headerLevelLabel.setFont(F_TITLE);
        headerLevelLabel.setForeground(SnakeLevel.C_HEAP);
        left.add(headerLevelLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(muteButton);
        JButton exit = RetroTheme.createRetroButton("EXIT", RetroTheme.BTN_DARK, RetroTheme.ACCENT_PINK);
        exit.setFont(F_SMALL);
        exit.setFocusable(false); // STEP 1: SPACE must never trigger EXIT
        exit.setToolTipText("Return to Mission 05 [ESC]");
        exit.addActionListener(e -> requestExit());
        right.add(exit);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }
}
