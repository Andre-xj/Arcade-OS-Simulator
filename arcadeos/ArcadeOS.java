package arcadeos;

import java.awt.*;
import java.awt.event.MouseEvent;
import javax.swing.*;

/**
 * Main Application Frame for ArcadeOS.
 * Coordinates transitions across:
 * 1. Screen 1: LoginPanel (Player Login / Arcade Security Gate)
 * 2. Screen 2: MainMenuPanel (8-Bit Retro Arcade Edition)
 * 3. Screen 3: ArchivesPanel (8 Archive Cards Manuals)
 * 4. Screen 4: SimulatorContainerPanel (8 Modular Simulators)
 */
public class ArcadeOS extends JFrame {
    public static final String SCREEN_SYSTEM_INIT = "SCREEN_SYSTEM_INIT";
    public static final String SCREEN_LOGIN       = "SCREEN_LOGIN";
    public static final String SCREEN_MAIN_MENU   = "SCREEN_MAIN_MENU";
    public static final String SCREEN_ARCHIVES    = "SCREEN_ARCHIVES";
    public static final String SCREEN_SIMULATOR   = "SCREEN_SIMULATOR";
    public static final String SCREEN_QUIZ        = "SCREEN_QUIZ";

    private JPanel rootCardPanel;
    private CardLayout rootCardLayout;

    private SystemInitPanel systemInitPanel;
    private LoginPanel loginPanel;
    private MainMenuPanel mainMenuPanel;
    private ArchivesPanel archivesPanel;
    private SimulatorContainerPanel simulatorPanel;
    private QuizPanel quizPanel;

    public ArcadeOS() {
        setTitle("MINI // OS - 8-Bit Retro Operating System Simulator [ARCADE EDITION]");
        setSize(1280, 860);
        setMinimumSize(new Dimension(1080, 720));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        getContentPane().setBackground(RetroTheme.BG_DARK);

        // Apply theme defaults
        RetroTheme.applyGlobalUIDefaults();

        rootCardLayout = new CardLayout();
        rootCardPanel = new JPanel(rootCardLayout);
        rootCardPanel.setBackground(RetroTheme.BG_DARK);

        // Initialize all Screens
        systemInitPanel = new SystemInitPanel(this);
        loginPanel      = new LoginPanel(this);
        mainMenuPanel   = new MainMenuPanel(this);
        archivesPanel   = new ArchivesPanel(this);
        simulatorPanel  = new SimulatorContainerPanel(this);
        quizPanel       = new QuizPanel(this);

        rootCardPanel.add(systemInitPanel, SCREEN_SYSTEM_INIT);
        rootCardPanel.add(loginPanel,      SCREEN_LOGIN);
        rootCardPanel.add(mainMenuPanel,   SCREEN_MAIN_MENU);
        rootCardPanel.add(archivesPanel,   SCREEN_ARCHIVES);
        rootCardPanel.add(simulatorPanel,  SCREEN_SIMULATOR);
        rootCardPanel.add(quizPanel,       SCREEN_QUIZ);

        add(rootCardPanel, BorderLayout.CENTER);

        // Start on SYSTEM INITIALIZATION (first boot) — Figma Screen 1
        showScreen(SCREEN_SYSTEM_INIT);
    }

    public void showScreen(String name) {
        if (name.equals(SCREEN_ARCHIVES) && archivesPanel != null) {
            archivesPanel.updateCounters();
        }
        rootCardLayout.show(rootCardPanel, name);
        rootCardPanel.revalidate();
        rootCardPanel.repaint();
    }

    public void loadManualExample(int manualIndex) {
        showScreen(SCREEN_SIMULATOR);
        if (simulatorPanel != null) {
            simulatorPanel.loadManualExample(manualIndex);
        }
    }

    public ArchivesPanel getArchivesPanel() {
        return archivesPanel;
    }

    public SimulatorContainerPanel getSimulatorPanel() {
        return simulatorPanel;
    }

    public void openSimulatorTab(int tabIndex) {
        showScreen(SCREEN_SIMULATOR);
        if (simulatorPanel != null) {
            simulatorPanel.selectTab(tabIndex);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}
            RetroTheme.applyGlobalUIDefaults();
            ArcadeOS os = new ArcadeOS();
            os.setExtendedState(JFrame.MAXIMIZED_BOTH);
            os.setVisible(true);
        });
    }
}
