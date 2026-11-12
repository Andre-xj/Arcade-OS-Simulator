package arcadeos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.Queue;

public class PageReplacementPanel extends JPanel {
    private JTextField txtRefString, txtFrames;
    private JComboBox<String> algoCombo;
    private DefaultTableModel traceModel;
    private JLabel lblSummary;

    public PageReplacementPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        ctrl.setBackground(RetroTheme.PANEL_BG);
        ctrl.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        JLabel lblAlgo = new JLabel("Algorithm:");
        lblAlgo.setFont(RetroTheme.RETRO_FONT_BOLD);
        lblAlgo.setForeground(RetroTheme.ACCENT_CYAN);
        ctrl.add(lblAlgo);

        algoCombo = new JComboBox<>(new String[]{"FIFO", "LRU", "Optimal"});
        algoCombo.setFont(RetroTheme.RETRO_FONT);
        algoCombo.setBackground(new Color(32, 24, 56));
        algoCombo.setForeground(RetroTheme.TEXT_WHITE);
        ctrl.add(algoCombo);

        JLabel lblF = new JLabel("Frames:");
        lblF.setFont(RetroTheme.RETRO_FONT);
        lblF.setForeground(RetroTheme.TEXT_WHITE);
        ctrl.add(lblF);

        txtFrames = RetroTheme.createRetroTextField("3", 3);
        ctrl.add(txtFrames);

        JLabel lblR = new JLabel("Reference String:");
        lblR.setFont(RetroTheme.RETRO_FONT);
        lblR.setForeground(RetroTheme.TEXT_WHITE);
        ctrl.add(lblR);

        txtRefString = RetroTheme.createRetroTextField("7 0 1 2 0 3 0 4 2 3 0 3 2 1 2 0 1 7 0 1", 22);
        ctrl.add(txtRefString);

        JButton btnSmall = RetroTheme.createRetroButton("Preset: Small (8)", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnLarge = RetroTheme.createRetroButton("Preset: Large (20)", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnRun = RetroTheme.createRetroButton("▶ SIMULATE SWAP", RetroTheme.ACCENT_YELLOW, Color.BLACK);

        ctrl.add(btnSmall);
        ctrl.add(btnLarge);
        ctrl.add(btnRun);
        add(ctrl, BorderLayout.NORTH);

        JPanel display = new JPanel(new BorderLayout(4, 4));
        display.setBackground(RetroTheme.PANEL_BG);
        display.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        lblSummary = new JLabel(" Total Pages: -- | Hits: -- | Faults: -- | Fault Ratio: --% ");
        lblSummary.setFont(new Font("Consolas", Font.BOLD, 13));
        lblSummary.setForeground(RetroTheme.ACCENT_YELLOW);
        lblSummary.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        display.add(lblSummary, BorderLayout.NORTH);

        traceModel = new DefaultTableModel();
        JTable table = RetroTheme.createRetroTable(traceModel);
        JScrollPane scroll = RetroTheme.createRetroScroll(table);
        display.add(scroll, BorderLayout.CENTER);

        add(display, BorderLayout.CENTER);

        btnSmall.addActionListener(e -> {
            txtFrames.setText("3");
            txtRefString.setText("1 3 0 3 5 6 3 1");
        });
        btnLarge.addActionListener(e -> {
            txtFrames.setText("4");
            txtRefString.setText("7 0 1 2 0 3 0 4 2 3 0 3 2 1 2 0 1 7 0 1");
        });
        btnRun.addActionListener(e -> runPageReplacement());
    }

    private void runPageReplacement() {
        try {
            int numFrames = Integer.parseInt(txtFrames.getText().trim());
            if (numFrames <= 0) throw new Exception("Frames > 0");

            String[] tokens = txtRefString.getText().trim().split("\\s+");
            int[] pages = new int[tokens.length];
            for (int i = 0; i < tokens.length; i++) pages[i] = Integer.parseInt(tokens[i]);

            String algo = algoCombo.getSelectedItem().toString();
            int faults = 0, hits = 0;

            String[] cols = new String[numFrames + 3];
            cols[0] = "Step";
            cols[1] = "Ref Page";
            for (int f = 0; f < numFrames; f++) cols[2 + f] = "Frame " + f;
            cols[numFrames + 2] = "Status";

            traceModel.setDataVector(new Object[][]{}, cols);

            int[] frameSlots = new int[numFrames];
            Arrays.fill(frameSlots, -1);

            Queue<Integer> fifoQueue = new LinkedList<>();
            Map<Integer, Integer> lruLastUsed = new HashMap<>();

            for (int step = 0; step < pages.length; step++) {
                int p = pages[step];
                boolean isHit = false;

                for (int f = 0; f < numFrames; f++) {
                    if (frameSlots[f] == p) {
                        isHit = true;
                        break;
                    }
                }

                String status;
                if (isHit) {
                    hits++;
                    status = "HIT ★";
                    if (algo.equals("LRU")) lruLastUsed.put(p, step);
                } else {
                    faults++;
                    status = "FAULT ✗";

                    int emptyIdx = -1;
                    for (int f = 0; f < numFrames; f++) {
                        if (frameSlots[f] == -1) { emptyIdx = f; break; }
                    }

                    if (emptyIdx != -1) {
                        frameSlots[emptyIdx] = p;
                        if (algo.equals("FIFO")) fifoQueue.add(p);
                        else if (algo.equals("LRU")) lruLastUsed.put(p, step);
                    } else {
                        if (algo.equals("FIFO")) {
                            int victim = fifoQueue.poll();
                            for (int f = 0; f < numFrames; f++) {
                                if (frameSlots[f] == victim) {
                                    frameSlots[f] = p;
                                    break;
                                }
                            }
                            fifoQueue.add(p);
                        } else if (algo.equals("LRU")) {
                            int lruPage = -1, oldest = Integer.MAX_VALUE;
                            for (int f = 0; f < numFrames; f++) {
                                int cand = frameSlots[f];
                                int last = lruLastUsed.getOrDefault(cand, -1);
                                if (last < oldest) { oldest = last; lruPage = cand; }
                            }
                            for (int f = 0; f < numFrames; f++) {
                                if (frameSlots[f] == lruPage) { frameSlots[f] = p; break; }
                            }
                            lruLastUsed.put(p, step);
                        } else {
                            int victimIdx = -1, farthest = -1;
                            for (int f = 0; f < numFrames; f++) {
                                int cand = frameSlots[f];
                                int nextUse = Integer.MAX_VALUE;
                                for (int k = step + 1; k < pages.length; k++) {
                                    if (pages[k] == cand) { nextUse = k; break; }
                                }
                                if (nextUse > farthest) {
                                    farthest = nextUse;
                                    victimIdx = f;
                                }
                            }
                            frameSlots[victimIdx] = p;
                        }
                    }
                }

                Object[] row = new Object[numFrames + 3];
                row[0] = step + 1;
                row[1] = p;
                for (int f = 0; f < numFrames; f++) {
                    row[2 + f] = frameSlots[f] == -1 ? "-" : frameSlots[f];
                }
                row[numFrames + 2] = status;
                traceModel.addRow(row);
            }

            double faultRatio = (double) faults / pages.length * 100.0;
            lblSummary.setText(String.format(" Total Refs: %d | Hits: %d | Faults: %d | Fault Rate: %.2f%% | Hit Rate: %.2f%% ",
                    pages.length, hits, faults, faultRatio, 100.0 - faultRatio));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: check reference string (spaces between ints) & frames > 0.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
