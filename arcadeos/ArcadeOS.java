package arcadeos;

import javax.swing.*;
import java.awt.*;

public class ArcadeOS extends JFrame {

    public ArcadeOS() {
        setTitle("ARCADE-OS // INSERT COIN [OPERATING SYSTEM SIMULATOR v1.0]");
        setSize(1180, 780);
        setMinimumSize(new Dimension(1000, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout());

        // Header Panel
        add(createHeader(), BorderLayout.NORTH);

        // Tabbed Pane for the 8 modular components
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(RetroTheme.PANEL_BG);
        tabbedPane.setForeground(RetroTheme.TEXT_WHITE);
        tabbedPane.setFont(RetroTheme.RETRO_FONT_BOLD);

        tabbedPane.addTab(" 🕹️ CPU SCHED ", new CPUSchedulingPanel());
        tabbedPane.addTab(" 🪙 BANKER'S ", new BankersPanel());
        tabbedPane.addTab(" 💾 PAGING ", new PagingPanel());
        tabbedPane.addTab(" 🔄 CACHE SWAP ", new PageReplacementPanel());
        tabbedPane.addTab(" 🗺️ SEGMENTS ", new SegmentationPanel());
        tabbedPane.addTab(" 🧱 RAM ALLOC ", new FragmentationPanel());
        tabbedPane.addTab(" 💽 DISK HEAD ", new DiskSchedulingPanel());
        tabbedPane.addTab(" ⚡ MULTIPROG ", new MultiprogrammingPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(RetroTheme.PANEL_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, RetroTheme.ACCENT_PINK),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));

        JLabel title = new JLabel("◆ ARCADE-OS: COIN-OP 8-BIT KERNEL ◆");
        title.setFont(new Font("Consolas", Font.BOLD, 16));
        title.setForeground(RetroTheme.ACCENT_YELLOW);

        JLabel status = new JLabel("CREDITS: 99 | SYS_STATUS: NOMINAL | SCAN: 60Hz");
        status.setFont(RetroTheme.RETRO_FONT_BOLD);
        status.setForeground(RetroTheme.ACCENT_CYAN);

        header.add(title, BorderLayout.WEST);
        header.add(status, BorderLayout.EAST);
        return header;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}
            new ArcadeOS().setVisible(true);
        });
    }
}
