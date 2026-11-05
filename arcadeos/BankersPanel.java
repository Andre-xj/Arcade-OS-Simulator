package arcadeos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class BankersPanel extends JPanel {
    private JTextField txtAvail;
    private DefaultTableModel tableModel;
    private JLabel lblStatus;
    private JTextArea txtTrace;

    public BankersPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        ctrl.setBackground(RetroTheme.PANEL_BG);
        ctrl.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        JLabel lblA = new JLabel("Available Tokens (R1 R2 R3...):");
        lblA.setFont(RetroTheme.RETRO_FONT_BOLD);
        lblA.setForeground(RetroTheme.ACCENT_CYAN);
        ctrl.add(lblA);

        txtAvail = RetroTheme.createRetroTextField("3 3 2", 10);
        ctrl.add(txtAvail);

        JButton btnAdd = RetroTheme.createRetroButton("+ Add Player", new Color(40, 26, 74), RetroTheme.ACCENT_CYAN);
        JButton btnRemove = RetroTheme.createRetroButton("- Remove", new Color(40, 26, 74), RetroTheme.ACCENT_PINK);
        JButton btnSmall = RetroTheme.createRetroButton("Preset: Small (3P,3R)", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnLarge = RetroTheme.createRetroButton("Preset: Large (6P,4R)", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnRun = RetroTheme.createRetroButton("▶ CHECK SAFE SEQUENCE", RetroTheme.ACCENT_YELLOW, Color.BLACK);

        ctrl.add(btnAdd);
        ctrl.add(btnRemove);
        ctrl.add(btnSmall);
        ctrl.add(btnLarge);
        ctrl.add(btnRun);
        add(ctrl, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"Process / Player ID", "Allocated Resources", "Max Claim Resources"}, 0);
        JTable table = RetroTheme.createRetroTable(tableModel);
        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setPreferredSize(new Dimension(800, 180));

        JPanel logPanel = new JPanel(new BorderLayout(4, 4));
        logPanel.setBackground(RetroTheme.PANEL_BG);
        logPanel.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        lblStatus = new JLabel(" STATUS: Awaiting Algorithm Execution ");
        lblStatus.setFont(new Font("Consolas", Font.BOLD, 14));
        lblStatus.setForeground(RetroTheme.TEXT_WHITE);
        logPanel.add(lblStatus, BorderLayout.NORTH);

        txtTrace = new JTextArea();
        txtTrace.setBackground(new Color(10, 6, 18));
        txtTrace.setForeground(RetroTheme.TEXT_WHITE);
        txtTrace.setFont(RetroTheme.RETRO_FONT);
        txtTrace.setEditable(false);
        JScrollPane scrollTrace = new JScrollPane(txtTrace);
        logPanel.add(scrollTrace, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTable, logPanel);
        split.setDividerLocation(200);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);

        btnAdd.addActionListener(e -> tableModel.addRow(new Object[]{"P" + tableModel.getRowCount(), "0 1 0", "7 5 3"}));
        btnRemove.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) tableModel.removeRow(r);
        });
        btnSmall.addActionListener(e -> loadSmallPreset());
        btnLarge.addActionListener(e -> loadLargePreset());
        btnRun.addActionListener(e -> runBankerSafety());

        loadSmallPreset();
    }

    private void loadSmallPreset() {
        txtAvail.setText("3 3 2");
        tableModel.setRowCount(0);
        tableModel.addRow(new Object[]{"P0", "0 1 0", "7 5 3"});
        tableModel.addRow(new Object[]{"P1", "2 0 0", "3 2 2"});
        tableModel.addRow(new Object[]{"P2", "3 0 2", "9 0 2"});
        tableModel.addRow(new Object[]{"P3", "2 1 1", "2 2 2"});
        tableModel.addRow(new Object[]{"P4", "0 0 2", "4 3 3"});
    }

    private void loadLargePreset() {
        txtAvail.setText("2 3 2 1");
        tableModel.setRowCount(0);
        tableModel.addRow(new Object[]{"P0", "0 0 1 2", "2 1 3 4"});
        tableModel.addRow(new Object[]{"P1", "1 0 0 0", "2 1 1 2"});
        tableModel.addRow(new Object[]{"P2", "1 3 5 4", "1 4 6 5"});
        tableModel.addRow(new Object[]{"P3", "0 6 3 2", "0 6 5 2"});
        tableModel.addRow(new Object[]{"P4", "0 0 1 4", "0 1 2 5"});
        tableModel.addRow(new Object[]{"P5", "1 0 1 0", "3 2 1 1"});
    }

    private void runBankerSafety() {
        try {
            String[] avTokens = txtAvail.getText().trim().split("\\s+");
            int m = avTokens.length;
            int[] avail = new int[m];
            for (int j = 0; j < m; j++) avail[j] = Integer.parseInt(avTokens[j]);

            int n = tableModel.getRowCount();
            if (n == 0) throw new Exception("Walang processes sa table!");

            String[] pids = new String[n];
            int[][] alloc = new int[n][m];
            int[][] maxClaim = new int[n][m];
            int[][] need = new int[n][m];

            for (int i = 0; i < n; i++) {
                pids[i] = tableModel.getValueAt(i, 0).toString().trim();
                String[] aTok = tableModel.getValueAt(i, 1).toString().trim().split("\\s+");
                String[] mTok = tableModel.getValueAt(i, 2).toString().trim().split("\\s+");
                if (aTok.length != m || mTok.length != m) {
                    throw new Exception("Row " + pids[i] + " does not match resource count: " + m);
                }
                for (int j = 0; j < m; j++) {
                    alloc[i][j] = Integer.parseInt(aTok[j]);
                    maxClaim[i][j] = Integer.parseInt(mTok[j]);
                    need[i][j] = maxClaim[i][j] - alloc[i][j];
                    if (need[i][j] < 0) throw new Exception("Max < Alloc for " + pids[i]);
                }
            }

            int[] work = Arrays.copyOf(avail, m);
            boolean[] finish = new boolean[n];
            List<String> safeSeq = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            sb.append("=== NEED MATRIX (Need = Max - Alloc) ===\n");
            for (int i = 0; i < n; i++) {
                sb.append(String.format("%-6s : Need = %s | (Max %s - Alloc %s)\n",
                        pids[i], Arrays.toString(need[i]), Arrays.toString(maxClaim[i]), Arrays.toString(alloc[i])));
            }
            sb.append("\n=== SAFETY ALGORITHM STEP-BY-STEP TRACE ===\n");

            boolean found;
            do {
                found = false;
                for (int i = 0; i < n; i++) {
                    if (!finish[i]) {
                        boolean canExec = true;
                        for (int j = 0; j < m; j++) {
                            if (need[i][j] > work[j]) { canExec = false; break; }
                        }
                        if (canExec) {
                            for (int j = 0; j < m; j++) work[j] += alloc[i][j];
                            finish[i] = true;
                            safeSeq.add(pids[i]);
                            sb.append(String.format("-> %s executed safely! New Work/Available = %s\n", pids[i], Arrays.toString(work)));
                            found = true;
                        }
                    }
                }
            } while (found);

            txtTrace.setText(sb.toString());
            if (safeSeq.size() == n) {
                lblStatus.setText("STATE: SAFE STATE! [ SAFE SEQUENCE: < " + String.join(" -> ", safeSeq) + " > ]");
                lblStatus.setForeground(RetroTheme.ACCENT_CYAN);
            } else {
                lblStatus.setText("STATE: DEADLOCK DETECTED / UNSAFE STATE! (No safe sequence exists)");
                lblStatus.setForeground(RetroTheme.ACCENT_PINK);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Banker's error: " + ex.getMessage(), "Input Format Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
