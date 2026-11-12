package arcadeos;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;

public class PagingPanel extends JPanel {
    private JTextField txtPageSize, txtQueryAddr;
    private DefaultTableModel pageTableModel;
    private JLabel lblTranslateResult;
    private JTextArea txtPageInfo;

    public PagingPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel ctrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        ctrl.setBackground(RetroTheme.PANEL_BG);
        ctrl.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        JLabel lblPS = new JLabel("Page Size (Bytes):");
        lblPS.setFont(RetroTheme.RETRO_FONT_BOLD);
        lblPS.setForeground(RetroTheme.ACCENT_CYAN);
        ctrl.add(lblPS);

        txtPageSize = RetroTheme.createRetroTextField("1024", 5);
        ctrl.add(txtPageSize);

        JButton btnAdd = RetroTheme.createRetroButton("+ Add Page", new Color(40, 26, 74), RetroTheme.ACCENT_CYAN);
        JButton btnRemove = RetroTheme.createRetroButton("- Remove", new Color(40, 26, 74), RetroTheme.ACCENT_PINK);
        JButton btnSmall = RetroTheme.createRetroButton("Preset: Small (4)", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnLarge = RetroTheme.createRetroButton("Preset: Large (10)", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);

        ctrl.add(btnAdd);
        ctrl.add(btnRemove);
        ctrl.add(btnSmall);
        ctrl.add(btnLarge);
        add(ctrl, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 8, 8));
        center.setOpaque(false);

        pageTableModel = new DefaultTableModel(new String[]{"Logical Page #", "Physical Frame #"}, 0);
        JTable table = RetroTheme.createRetroTable(pageTableModel);
        JScrollPane scroll = RetroTheme.createRetroScroll(table);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR), " [ PAGE-TO-FRAME MAP TABLE ] ",
                TitledBorder.LEFT, TitledBorder.TOP, RetroTheme.RETRO_FONT_BOLD, RetroTheme.ACCENT_CYAN
        ));
        center.add(scroll);

        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setBackground(RetroTheme.PANEL_BG);
        rightPanel.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        JPanel transQuery = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        transQuery.setOpaque(false);
        JLabel lblQ = new JLabel("Query Logical Address:");
        lblQ.setFont(RetroTheme.RETRO_FONT_BOLD);
        lblQ.setForeground(RetroTheme.ACCENT_YELLOW);
        transQuery.add(lblQ);

        txtQueryAddr = RetroTheme.createRetroTextField("2500", 7);
        transQuery.add(txtQueryAddr);

        JButton btnTrans = RetroTheme.createRetroButton("TRANSLATE", RetroTheme.ACCENT_YELLOW, Color.BLACK);
        transQuery.add(btnTrans);
        rightPanel.add(transQuery, BorderLayout.NORTH);

        lblTranslateResult = new JLabel(" PHYSICAL ADDRESS: -- ");
        lblTranslateResult.setFont(new Font("Consolas", Font.BOLD, 13));
        lblTranslateResult.setForeground(RetroTheme.ACCENT_CYAN);
        lblTranslateResult.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
        rightPanel.add(lblTranslateResult, BorderLayout.CENTER);

        txtPageInfo = new JTextArea();
        txtPageInfo.setBackground(RetroTheme.INPUT_BG);
        txtPageInfo.setForeground(RetroTheme.TEXT_WHITE);
        txtPageInfo.setFont(RetroTheme.RETRO_FONT);
        txtPageInfo.setEditable(false);
        JScrollPane scrollInfo = RetroTheme.createRetroScroll(txtPageInfo);
        scrollInfo.setPreferredSize(new Dimension(300, 200));
        rightPanel.add(scrollInfo, BorderLayout.SOUTH);

        center.add(rightPanel);
        add(center, BorderLayout.CENTER);

        btnAdd.addActionListener(e -> pageTableModel.addRow(new Object[]{pageTableModel.getRowCount(), pageTableModel.getRowCount() + 2}));
        btnRemove.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0) pageTableModel.removeRow(r);
        });
        btnSmall.addActionListener(e -> loadSmallPreset());
        btnLarge.addActionListener(e -> loadLargePreset());
        btnTrans.addActionListener(e -> translateAddress());

        loadSmallPreset();
    }

    private void loadSmallPreset() {
        txtPageSize.setText("1024");
        pageTableModel.setRowCount(0);
        pageTableModel.addRow(new Object[]{"0", "5"});
        pageTableModel.addRow(new Object[]{"1", "6"});
        pageTableModel.addRow(new Object[]{"2", "1"});
        pageTableModel.addRow(new Object[]{"3", "2"});
    }

    private void loadLargePreset() {
        txtPageSize.setText("512");
        pageTableModel.setRowCount(0);
        for (int i = 0; i < 10; i++) {
            pageTableModel.addRow(new Object[]{String.valueOf(i), String.valueOf((i * 3 + 1) % 16)});
        }
    }

    private void translateAddress() {
        try {
            int pageSize = Integer.parseInt(txtPageSize.getText().trim());
            int logicalAddr = Integer.parseInt(txtQueryAddr.getText().trim());
            if (pageSize <= 0 || logicalAddr < 0) throw new Exception("Invalid size or address");

            int pageNumber = logicalAddr / pageSize;
            int offset = logicalAddr % pageSize;

            Map<Integer, Integer> map = new HashMap<>();
            for (int i = 0; i < pageTableModel.getRowCount(); i++) {
                int p = Integer.parseInt(pageTableModel.getValueAt(i, 0).toString().trim());
                int f = Integer.parseInt(pageTableModel.getValueAt(i, 1).toString().trim());
                map.put(p, f);
            }

            if (!map.containsKey(pageNumber)) {
                lblTranslateResult.setText("PAGE FAULT / TRAP: Page " + pageNumber + " not in Page Table!");
                lblTranslateResult.setForeground(RetroTheme.ACCENT_PINK);
                txtPageInfo.setText("Logical Addr: " + logicalAddr + "\nCalculated Page #: " + pageNumber + "\nOffset: " + offset + "\nResult: Page is outside mapped limits.");
                return;
            }

            int frame = map.get(pageNumber);
            int physicalAddr = (frame * pageSize) + offset;

            lblTranslateResult.setText(String.format("PHYSICAL ADDR: %d (Frame %d, Offset %d)", physicalAddr, frame, offset));
            lblTranslateResult.setForeground(RetroTheme.ACCENT_CYAN);

            txtPageInfo.setText(String.format(
                    "--- TRANSLATION FORMULA BREAKDOWN ---\n" +
                            "1. Page Size         = %d Bytes\n" +
                            "2. Logical Address   = %d\n" +
                            "3. Page Number (p)   = %d / %d = %d\n" +
                            "4. Page Offset (d)   = %d %% %d = %d\n" +
                            "5. Frame Number (f)  = %d (from Page Table)\n" +
                            "6. Physical Address  = (%d * %d) + %d = %d Bytes",
                    pageSize, logicalAddr, logicalAddr, pageSize, pageNumber,
                    logicalAddr, pageSize, offset, frame, frame, pageSize, offset, physicalAddr
            ));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Check numbers: Page size > 0 at Address >= 0.", "Translation Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
