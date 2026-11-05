package arcadeos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class FragmentationPanel extends JPanel {
    private JTextField txtBlocks, txtProcesses;
    private JComboBox<String> algoCombo;
    private DefaultTableModel allocModel;
    private JLabel lblFragSummary;

    public FragmentationPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        ctrl.setBackground(RetroTheme.PANEL_BG);
        ctrl.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        JLabel lblA = new JLabel("Strategy:");
        lblA.setFont(RetroTheme.RETRO_FONT_BOLD);
        lblA.setForeground(RetroTheme.ACCENT_CYAN);
        ctrl.add(lblA);

        algoCombo = new JComboBox<>(new String[]{"First Fit", "Best Fit", "Worst Fit"});
        algoCombo.setFont(RetroTheme.RETRO_FONT);
        ctrl.add(algoCombo);

        JLabel lblB = new JLabel("RAM Blocks (KB):");
        lblB.setFont(RetroTheme.RETRO_FONT);
        lblB.setForeground(RetroTheme.TEXT_WHITE);
        ctrl.add(lblB);

        txtBlocks = RetroTheme.createRetroTextField("100 500 200 300 600", 15);
        ctrl.add(txtBlocks);

        JLabel lblP = new JLabel("Game Procs (KB):");
        lblP.setFont(RetroTheme.RETRO_FONT);
        lblP.setForeground(RetroTheme.TEXT_WHITE);
        ctrl.add(lblP);

        txtProcesses = RetroTheme.createRetroTextField("212 417 112 426", 15);
        ctrl.add(txtProcesses);

        JButton btnSmall = RetroTheme.createRetroButton("Preset: Small", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnLarge = RetroTheme.createRetroButton("Preset: Large", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnRun = RetroTheme.createRetroButton("▶ ALLOCATE RAM", RetroTheme.ACCENT_YELLOW, Color.BLACK);

        ctrl.add(btnSmall);
        ctrl.add(btnLarge);
        ctrl.add(btnRun);
        add(ctrl, BorderLayout.NORTH);

        JPanel outPanel = new JPanel(new BorderLayout(4, 4));
        outPanel.setBackground(RetroTheme.PANEL_BG);
        outPanel.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        lblFragSummary = new JLabel(" Internal Fragmentation: -- KB | External (Unallocated Free Space): -- KB ");
        lblFragSummary.setFont(new Font("Consolas", Font.BOLD, 13));
        lblFragSummary.setForeground(RetroTheme.ACCENT_YELLOW);
        lblFragSummary.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        outPanel.add(lblFragSummary, BorderLayout.NORTH);

        allocModel = new DefaultTableModel(new String[]{"Process #", "Process Size", "Allocated Block #", "Block Initial Size", "Internal Frag (Unused)"}, 0);
        JTable table = RetroTheme.createRetroTable(allocModel);
        JScrollPane scroll = new JScrollPane(table);
        outPanel.add(scroll, BorderLayout.CENTER);

        add(outPanel, BorderLayout.CENTER);

        btnSmall.addActionListener(e -> {
            txtBlocks.setText("100 500 200 300 600");
            txtProcesses.setText("212 417 112 426");
        });
        btnLarge.addActionListener(e -> {
            txtBlocks.setText("150 350 250 450 600 200 500 100");
            txtProcesses.setText("120 400 220 50 300 480 180");
        });
        btnRun.addActionListener(e -> runFragmentation());
    }

    private void runFragmentation() {
        try {
            String[] bTok = txtBlocks.getText().trim().split("\\s+");
            int[] bSizes = new int[bTok.length];
            int[] origSizes = new int[bTok.length];
            for (int i = 0; i < bTok.length; i++) {
                bSizes[i] = Integer.parseInt(bTok[i]);
                origSizes[i] = bSizes[i];
            }

            String[] pTok = txtProcesses.getText().trim().split("\\s+");
            int[] pSizes = new int[pTok.length];
            for (int i = 0; i < pTok.length; i++) pSizes[i] = Integer.parseInt(pTok[i]);

            String strat = algoCombo.getSelectedItem().toString();
            allocModel.setRowCount(0);

            int totalInternalFrag = 0;
            boolean[] blockUsed = new boolean[bSizes.length];
            int unallocatedProcMemory = 0;

            for (int pIdx = 0; pIdx < pSizes.length; pIdx++) {
                int pSize = pSizes[pIdx];
                int chosenBlock = -1;

                if (strat.equals("First Fit")) {
                    for (int b = 0; b < bSizes.length; b++) {
                        if (!blockUsed[b] && bSizes[b] >= pSize) {
                            chosenBlock = b;
                            break;
                        }
                    }
                } else if (strat.equals("Best Fit")) {
                    int minDiff = Integer.MAX_VALUE;
                    for (int b = 0; b < bSizes.length; b++) {
                        if (!blockUsed[b] && bSizes[b] >= pSize) {
                            int diff = bSizes[b] - pSize;
                            if (diff < minDiff) {
                                minDiff = diff;
                                chosenBlock = b;
                            }
                        }
                    }
                } else {
                    int maxDiff = -1;
                    for (int b = 0; b < bSizes.length; b++) {
                        if (!blockUsed[b] && bSizes[b] >= pSize) {
                            int diff = bSizes[b] - pSize;
                            if (diff > maxDiff) {
                                maxDiff = diff;
                                chosenBlock = b;
                            }
                        }
                    }
                }

                if (chosenBlock != -1) {
                    blockUsed[chosenBlock] = true;
                    int frag = origSizes[chosenBlock] - pSize;
                    totalInternalFrag += frag;
                    allocModel.addRow(new Object[]{
                            "P" + (pIdx + 1), pSize + " KB", "Block " + (chosenBlock + 1),
                            origSizes[chosenBlock] + " KB", frag + " KB"
                    });
                } else {
                    unallocatedProcMemory += pSize;
                    allocModel.addRow(new Object[]{
                            "P" + (pIdx + 1), pSize + " KB", "NOT ALLOCATED (Waiting)", "-", "-"
                    });
                }
            }

            int totalFreeSpace = 0;
            for (int b = 0; b < bSizes.length; b++) {
                if (!blockUsed[b]) totalFreeSpace += origSizes[b];
            }

            lblFragSummary.setText(String.format(
                    " Strategy: %s | Total Internal Frag: %d KB | Unused Free Blocks: %d KB (Blocked Reqs: %d KB) ",
                    strat, totalInternalFrag, totalFreeSpace, unallocatedProcMemory
            ));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error in blocks or process inputs: positive integers only.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
