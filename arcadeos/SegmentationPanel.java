package arcadeos;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class SegmentationPanel extends JPanel {
    private DefaultTableModel segModel;
    private JTextField txtSegNo, txtOffset;
    private JLabel lblResult;
    private JTextArea txtLog;

    public SegmentationPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        ctrl.setBackground(RetroTheme.PANEL_BG);
        ctrl.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        JButton btnAdd = RetroTheme.createRetroButton("+ Add Segment", new Color(40, 26, 74), RetroTheme.ACCENT_CYAN);
        JButton btnRemove = RetroTheme.createRetroButton("- Remove", new Color(40, 26, 74), RetroTheme.ACCENT_PINK);
        JButton btnSmall = RetroTheme.createRetroButton("Preset: Small (3)", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnLarge = RetroTheme.createRetroButton("Preset: Large (6)", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);

        ctrl.add(btnAdd);
        ctrl.add(btnRemove);
        ctrl.add(btnSmall);
        ctrl.add(btnLarge);
        add(ctrl, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 8, 8));
        center.setOpaque(false);

        segModel = new DefaultTableModel(new String[]{"Segment #", "Segment Name", "Base Address", "Limit (Size)"}, 0);
        JTable table = RetroTheme.createRetroTable(segModel);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR), " [ SEGMENT TABLE ] ",
                TitledBorder.LEFT, TitledBorder.TOP, RetroTheme.RETRO_FONT_BOLD, RetroTheme.ACCENT_CYAN
        ));
        center.add(scroll);

        JPanel transBox = new JPanel(new BorderLayout(8, 8));
        transBox.setBackground(RetroTheme.PANEL_BG);
        transBox.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        JPanel inputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        inputs.setOpaque(false);
        inputs.add(new JLabel("Seg #:"));
        txtSegNo = RetroTheme.createRetroTextField("1", 3);
        inputs.add(txtSegNo);

        inputs.add(new JLabel("Offset:"));
        txtOffset = RetroTheme.createRetroTextField("250", 5);
        inputs.add(txtOffset);

        JButton btnVal = RetroTheme.createRetroButton("TRANSLATE / VALIDATE", RetroTheme.ACCENT_YELLOW, Color.BLACK);
        inputs.add(btnVal);
        transBox.add(inputs, BorderLayout.NORTH);

        lblResult = new JLabel(" STATUS: Ready ");
        lblResult.setFont(new Font("Consolas", Font.BOLD, 13));
        lblResult.setForeground(RetroTheme.ACCENT_CYAN);
        lblResult.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
        transBox.add(lblResult, BorderLayout.CENTER);

        txtLog = new JTextArea();
        txtLog.setBackground(new Color(10, 6, 18));
        txtLog.setForeground(RetroTheme.TEXT_WHITE);
        txtLog.setFont(RetroTheme.RETRO_FONT);
        txtLog.setEditable(false);
        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setPreferredSize(new Dimension(300, 200));
        transBox.add(scrollLog, BorderLayout.SOUTH);

        center.add(transBox);
        add(center, BorderLayout.CENTER);

        btnAdd.addActionListener(e -> segModel.addRow(new Object[]{segModel.getRowCount(), "Seg_" + segModel.getRowCount(), "2000", "500"}));
        btnRemove.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) segModel.removeRow(r);
        });
        btnSmall.addActionListener(e -> loadSmallPreset());
        btnLarge.addActionListener(e -> loadLargePreset());
        btnVal.addActionListener(e -> validateAddress());

        loadSmallPreset();
    }

    private void loadSmallPreset() {
        segModel.setRowCount(0);
        segModel.addRow(new Object[]{"0", "Arcade_Sprites", "1400", "400"});
        segModel.addRow(new Object[]{"1", "Audio_Engine",   "6300", "300"});
        segModel.addRow(new Object[]{"2", "High_Scores",    "4300", "500"});
    }

    private void loadLargePreset() {
        segModel.setRowCount(0);
        segModel.addRow(new Object[]{"0", "Main_Kernel",    "1000", "1200"});
        segModel.addRow(new Object[]{"1", "Sprite_VRAM",    "2400", "800"});
        segModel.addRow(new Object[]{"2", "Sound_Chip",     "3600", "600"});
        segModel.addRow(new Object[]{"3", "Player_States",  "4500", "300"});
        segModel.addRow(new Object[]{"4", "Level_Map_Data", "5000", "1500"});
        segModel.addRow(new Object[]{"5", "Save_State_RAM", "7000", "400"});
    }

    private void validateAddress() {
        try {
            int targetSeg = Integer.parseInt(txtSegNo.getText().trim());
            int offset = Integer.parseInt(txtOffset.getText().trim());
            if (offset < 0) throw new Exception("Offset cannot be negative");

            boolean found = false;
            for (int i = 0; i < segModel.getRowCount(); i++) {
                int sNum = Integer.parseInt(segModel.getValueAt(i, 0).toString().trim());
                if (sNum == targetSeg) {
                    found = true;
                    String name = segModel.getValueAt(i, 1).toString().trim();
                    int base = Integer.parseInt(segModel.getValueAt(i, 2).toString().trim());
                    int limit = Integer.parseInt(segModel.getValueAt(i, 3).toString().trim());

                    if (offset >= limit) {
                        lblResult.setText(String.format("TRAP / SEGMENTATION FAULT! Offset (%d) >= Limit (%d)", offset, limit));
                        lblResult.setForeground(RetroTheme.ACCENT_PINK);
                        txtLog.setText(String.format(
                                "VALIDATION FAILED:\n" +
                                        "Segment %d [%s]\n" +
                                        "Base Address: %d\n" +
                                        "Segment Limit: %d\n" +
                                        "Target Offset: %d\n" +
                                        "Rule Check: Offset < Limit => %d < %d [VIOLATED!]",
                                sNum, name, base, limit, offset, offset, limit
                        ));
                    } else {
                        int phys = base + offset;
                        lblResult.setText(String.format("VALID ADDRESS! Physical Address = %d", phys));
                        lblResult.setForeground(RetroTheme.ACCENT_CYAN);
                        txtLog.setText(String.format(
                                "VALIDATION SUCCESS:\n" +
                                        "Segment %d [%s]\n" +
                                        "Base Address   : %d\n" +
                                        "Limit (Size)   : %d\n" +
                                        "Valid Offset   : %d\n" +
                                        "Calculation    : Base (%d) + Offset (%d) = Physical Address %d",
                                sNum, name, base, limit, offset, base, offset, phys
                        ));
                    }
                    break;
                }
            }
            if (!found) {
                lblResult.setText("ERROR: Segment #" + targetSeg + " does not exist in table!");
                lblResult.setForeground(RetroTheme.ACCENT_PINK);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: Valid integer segment and offset required.", "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
