package arcadeos;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * Screen: MISSION 03 PAGING TRANSLATION WORKBENCH (V3)
 * Full Overhaul addressing all Priority 1 Bugs, Priority 2 Clarity, and Priority 3 Polish:
 * 1. Fixed Layout Weights (30% / 38% / 32%) with setMinimumSize on all panels (no pushing or horizontal scrollbars).
 * 2. Distinct Fault Types: PAGE FAULT (unmapped page in table, valid=0) vs INVALID ADDRESS TRAP (exceeds table limit).
 * 3. Robust Inline Input Validation: empty, negative, non-numeric, zero page size handled gracefully with zero exceptions.
 * 4. Hero Values (36px bold) in Decomposition boxes and Result Card with small labels above.
 * 5. Full-width line for Step 6 formula so resolved equations never wrap or split.
 * 6. Obvious table selection: full-row cyan tint, '>' indicator, and no false row-0 selection on faults.
 * 7. Colored status: [V] VALID in bright green, [X] UNMAPPED in bright red.
 * 8. Column-Major Physical Memory Stack (F00-F07 left, F08-F15 right) with raised 10-11px base addresses and separate '>' target arrow.
 * 9. Translation History List and frame stats share the Physical RAM column.
 */
public class PagingPanel extends JPanel {

    private int pageSize = 1024;
    private JTextField txtPageSize, txtQueryAddr;
    private DefaultTableModel pageTableModel;
    private JTable pageTable;
    private int activeHighlightRow = -1;

    // Visual Result & Hero Value Components
    private JPanel resultCard;
    private JLabel lblResultHero;
    private JTextArea lblResultSub;
    private JTextArea lblResultRecovery;

    private JLabel lblHeroPage;
    private JLabel lblHeroOffset;
    private JLabel lblDecompTag;
    private JLabel lblDecompDetails;
    private JEditorPane formulaPane;

    // Legend & Hint Components
    private CardLayout legendCardLayout;
    private JPanel legendCard;
    private JLabel lblTableHint;
    private javax.swing.Timer hintTimer;

    // Physical Memory View (Column-Major 2-Column Frames Stack)
    private PhysicalMemoryView physicalMemView;
    private JLabel lblRamStats;

    // Live MMU Frame Hopper Animated Canvas
    private PagingHopperCanvas hopperCanvas;
    private JButton btnSimStream;
    private javax.swing.Timer streamTimer;
    private boolean isStreamRunning = false;
    private int streamIndex = 0;
    private static final long[] STREAM_ADDRESSES = { 2500, 1024, 4500, 7168, 6144, 15000, 8192, 3072, 9999 };

    public int getPageSize() { return pageSize; }
    public JTextField getTxtQueryAddr() { return txtQueryAddr; }
    public void translateAddressDirect() { translateAddress(); }

    // Translation History List (deduplicated by address, newest first)
    private DefaultListModel<String> historyListModel;
    private JList<String> historyList;
    private JScrollPane historyScroll;
    private JLabel historyEmptyLabel;
    private JPanel historyPanel;
    private JPanel historyContent;
    private CardLayout historyCardLayout;
    private final List<HistoryEntry> historyEntries = new ArrayList<>();
    private static final int HISTORY_LIMIT = 5;

    // Result card text: kept as raw HTML and re-wrapped to the card's real width on resize
    private JPanel resultSubStack;
    private String resultSubHtml = "Enter address above & click TRANSLATE";
    private String resultRecoveryHtml = "";

    // Shake animation timer for errors
    private javax.swing.Timer shakeTimer;
    private int shakeTicks = 0;

    /** One translation-history row. Repeats of the same address are merged with a count. */
    private static class HistoryEntry {
        final long la; long p, d; int f; long pa; String outcome; int count = 1;
        HistoryEntry(long la, long p, long d, int f, long pa, String outcome) {
            this.la = la; this.p = p; this.d = d; this.f = f; this.pa = pa; this.outcome = outcome;
        }
    }

    public PagingPanel() {
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // 1. Top Configuration Toolbar
        add(createTopToolbar(), BorderLayout.NORTH);

        // 2. 3-Column Center Workbench: table/legend | analytics | RAM/history
        JPanel centerPanel = new JPanel(new LayoutManager() {
            @Override public void addLayoutComponent(String name, Component comp) {}
            @Override public void removeLayoutComponent(Component comp) {}
            @Override public Dimension preferredLayoutSize(Container parent) { return new Dimension(860, 480); }
            @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(0, 0); }
            @Override public void layoutContainer(Container parent) {
                Insets in = parent.getInsets();
                int w = parent.getWidth() - in.left - in.right;
                int h = parent.getHeight() - in.top - in.bottom;
                if (parent.getComponentCount() < 3) return;
                int gap = 10;
                int availW = Math.max(0, w - gap * 2);
                int w0 = (int) Math.round(availW * 0.30);
                int w1 = (int) Math.round(availW * 0.38);
                int w2 = availW - w0 - w1;

                Component c0 = parent.getComponent(0);
                Component c1 = parent.getComponent(1);
                Component c2 = parent.getComponent(2);

                c0.setBounds(in.left, in.top, w0, h);
                c1.setBounds(in.left + w0 + gap, in.top, w1, h);
                c2.setBounds(in.left + w0 + gap + w1 + gap, in.top, w2, h);
            }
        });
        centerPanel.setOpaque(false);

        // Column 1: Page Table & Legend (30%)
        JPanel leftPanel = createLeftTablePanel();
        leftPanel.setPreferredSize(new Dimension(0, 0));
        leftPanel.setMinimumSize(new Dimension(0, 0));
        centerPanel.add(leftPanel);

        // Column 2: Middle Analytics (38%)
        JPanel middlePanel = createMiddleAnalyticsPanel();
        middlePanel.setPreferredSize(new Dimension(0, 0));
        middlePanel.setMinimumSize(new Dimension(0, 0));
        centerPanel.add(middlePanel);

        // Column 3: Physical Memory & Recent Translations (32%)
        JPanel rightPanel = createRightMemoryPanel();
        rightPanel.setPreferredSize(new Dimension(0, 0));
        rightPanel.setMinimumSize(new Dimension(0, 0));
        centerPanel.add(rightPanel);

        add(centerPanel, BorderLayout.CENTER);

        // Load default preset (Large 10)
        loadLargePreset();
    }

    private JPanel createTopToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(RetroTheme.PANEL_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        left.setOpaque(false);

        JLabel lblPS = new JLabel("Page Size (Bytes):");
        lblPS.setFont(RetroTheme.RETRO_FONT_BOLD);
        lblPS.setForeground(RetroTheme.ACCENT_CYAN);
        left.add(lblPS);

        txtPageSize = RetroTheme.createRetroTextField("1024", 6);
        txtPageSize.addActionListener(e -> translateAddress());
        left.add(txtPageSize);

        JButton btnAdd = RetroTheme.createRetroButton("+ Add Page", new Color(40, 26, 74), RetroTheme.ACCENT_CYAN);
        JButton btnRemove = RetroTheme.createRetroButton("- Remove", new Color(40, 26, 74), RetroTheme.ACCENT_PINK);
        btnAdd.setMnemonic('A');
        btnRemove.setMnemonic('R');
        JButton btnSmall = RetroTheme.createRetroButton("Preset: Small (4)", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);
        JButton btnLarge = RetroTheme.createRetroButton("Preset: Large (10)", new Color(35, 35, 60), RetroTheme.TEXT_WHITE);

        btnAdd.addActionListener(e -> addPageRow());
        btnRemove.addActionListener(e -> removePageRow());
        btnSmall.addActionListener(e -> loadSmallPreset());
        btnLarge.addActionListener(e -> loadLargePreset());

        left.add(btnAdd);
        left.add(btnRemove);
        left.add(btnSmall);
        left.add(btnLarge);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        right.setOpaque(false);

        JButton btnArcade = RetroTheme.createRetroButton("PLAY FRAME HOPPER", new Color(20, 28, 25), RetroTheme.ACCENT_GREEN);
        btnArcade.setIcon(new ImageIcon(PixelSprites.createFrogSprite(14, RetroTheme.ACCENT_GREEN)));
        btnArcade.setIconTextGap(6);
        btnArcade.setToolTipText("Launch 16-Bit Arcade MMU Memory Bus Crossing Game");
        btnArcade.addActionListener(e -> { ensurePageSizeTextValid(); launchFrameHopper(); });
        right.add(btnArcade);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createLeftTablePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);

        pageTableModel = new DefaultTableModel(new String[]{"Logical Page #", "Physical Frame #", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        // Plain JTable (NOT RetroTheme.createRetroTable) so this custom renderer owns the colors.
        // The shared table helper overrides cell background/foreground in prepareRenderer, which
        // used to wipe out the [V] green / [X] red status coloring.
        pageTable = new JTable(pageTableModel);
        pageTable.setBackground(RetroTheme.INPUT_BG);
        pageTable.setForeground(RetroTheme.TEXT_WHITE);
        pageTable.setFont(RetroTheme.FONT_BODY);
        pageTable.setGridColor(RetroTheme.BORDER_COLOR);
        pageTable.setRowHeight(24);
        pageTable.setSelectionBackground(new Color(80, 30, 100));
        pageTable.setSelectionForeground(RetroTheme.ACCENT_CYAN);
        pageTable.getTableHeader().setBackground(RetroTheme.PANEL_BG);
        pageTable.getTableHeader().setForeground(RetroTheme.ACCENT_CYAN);
        pageTable.getTableHeader().setFont(RetroTheme.FONT_SM);
        pageTable.getTableHeader().setReorderingAllowed(false);
        pageTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, RetroTheme.BORDER_COLOR));

        // Highlight active row with full cyan tint, '>' indicator, and high-contrast status
        pageTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, val, isSel, hasFoc, row, col);
                setHorizontalAlignment(CENTER);

                boolean isActive = (row == activeHighlightRow);
                String status = (String) pageTableModel.getValueAt(row, 2);
                boolean isValid = (status != null && status.contains("VALID"));

                if (isActive) {
                    if (isValid) {
                        c.setBackground(new Color(15, 65, 80)); // Full-row cyan tint
                        c.setForeground(RetroTheme.ACCENT_CYAN);
                    } else {
                        c.setBackground(new Color(75, 20, 35)); // Red tint for unmapped/fault
                        c.setForeground(new Color(255, 85, 120));
                    }
                    setFont(new Font("Consolas", Font.BOLD, 12));
                } else {
                    c.setBackground(row % 2 == 0 ? RetroTheme.ROW_EVEN : RetroTheme.ROW_ODD);
                    if (col == 2) {
                        // [V] VALID green / [X] UNMAPPED red, matching the legend
                        c.setForeground(isValid ? RetroTheme.ACCENT_GREEN : new Color(255, 75, 110));
                        setFont(new Font("Consolas", Font.BOLD, 11));
                    } else if (col == 0) {
                        c.setForeground(RetroTheme.ACCENT_CYAN);
                        setFont(new Font("Consolas", Font.BOLD, 11));
                    } else {
                        c.setForeground(isValid ? RetroTheme.TEXT_WHITE : new Color(205, 130, 152));
                        setFont(new Font("Consolas", Font.PLAIN, 11));
                    }
                }
                if (c instanceof JComponent) {
                    ((JComponent) c).setBorder(isActive
                            ? BorderFactory.createMatteBorder(1, 0, 1, 0, isValid ? RetroTheme.ACCENT_CYAN : new Color(255, 85, 120)) : null);
                }
                return c;
            }
        });

        JScrollPane scroll = RetroTheme.createRetroScroll(pageTable);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR), " [ PAGE-TO-FRAME MAP TABLE ] ",
                TitledBorder.LEFT, TitledBorder.TOP, RetroTheme.RETRO_FONT_BOLD, RetroTheme.ACCENT_CYAN
        ));
        p.add(scroll, BorderLayout.CENTER);

        // Bottom-left mapping legend; translation history sits below RAM on the right.
        JPanel bottomStack = new JPanel();
        bottomStack.setLayout(new BoxLayout(bottomStack, BoxLayout.Y_AXIS));
        bottomStack.setOpaque(false);

        // Legend
        legendCardLayout = new CardLayout();
        legendCard = new JPanel(legendCardLayout);
        legendCard.setBackground(RetroTheme.PANEL_BG);
        legendCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JPanel legendItems = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        legendItems.setOpaque(false);

        JLabel l1 = new JLabel("[V] VALID: Present in RAM");
        l1.setFont(new Font("Consolas", Font.BOLD, 10));
        l1.setForeground(RetroTheme.ACCENT_GREEN);

        JLabel l2 = new JLabel("[X] UNMAPPED: Page Fault");
        l2.setFont(new Font("Consolas", Font.BOLD, 10));
        l2.setForeground(new Color(255, 80, 110));

        legendItems.add(l1);
        legendItems.add(l2);

        lblTableHint = new JLabel("Page table needs at least 1 page.");
        lblTableHint.setFont(new Font("Consolas", Font.BOLD, 10));
        lblTableHint.setForeground(RetroTheme.ACCENT_PINK);

        legendCard.add(legendItems, "legend");
        legendCard.add(lblTableHint, "hint");
        bottomStack.add(legendCard);
        bottomStack.add(Box.createVerticalStrut(4));

        // Translation History
        historyPanel = new JPanel(new BorderLayout(0, 2));
        historyPanel.setBackground(RetroTheme.PANEL_BG);
        historyPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 6, 8)
        ));

        JLabel histTitle = new JLabel("RECENT TRANSLATIONS (CLICK TO RE-RUN):");
        histTitle.setFont(new Font("Consolas", Font.BOLD, 9));
        histTitle.setForeground(RetroTheme.TEXT_DIM);
        historyPanel.add(histTitle, BorderLayout.NORTH);

        historyListModel = new DefaultListModel<>();
        historyList = new JList<>(historyListModel);
        historyList.setFont(new Font("Consolas", Font.PLAIN, 10));
        historyList.setBackground(new Color(14, 10, 28));
        historyList.setForeground(RetroTheme.TEXT_WHITE);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && historyList.getSelectedValue() != null) {
                String val = historyList.getSelectedValue();
                if (val.contains(" -> ")) {
                    String addrStr = val.substring(0, val.indexOf(" ->")).trim();
                    txtQueryAddr.setText(addrStr);
                    translateAddress();
                }
            }
        });

        JScrollPane histScroll = new JScrollPane(historyList);
        // Tall enough for HISTORY_LIMIT rows so the last entry never runs past the bottom edge
        histScroll.setPreferredSize(new Dimension(232, 94));
        histScroll.setMinimumSize(new Dimension(120, 94));
        histScroll.setBorder(BorderFactory.createLineBorder(new Color(40, 30, 65)));
        histScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        histScroll.getViewport().setBackground(new Color(14, 10, 28));
        historyScroll = histScroll;

        // Empty state (shown when there are no translations yet)
        historyEmptyLabel = new JLabel("No translations yet", SwingConstants.CENTER);
        historyEmptyLabel.setFont(new Font("Consolas", Font.ITALIC, 10));
        historyEmptyLabel.setForeground(RetroTheme.TEXT_DIM);
        historyEmptyLabel.setBorder(BorderFactory.createLineBorder(new Color(40, 30, 65)));

        historyCardLayout = new CardLayout();
        historyContent = new JPanel(historyCardLayout);
        historyContent.add(histScroll, "history");
        historyContent.add(historyEmptyLabel, "empty");
        historyPanel.add(historyContent, BorderLayout.CENTER);

        p.add(bottomStack, BorderLayout.SOUTH);

        rebuildHistoryModel();

        return p;
    }

    private JPanel createMiddleAnalyticsPanel() {
        JPanel middle = new JPanel(new BorderLayout(0, 6));
        middle.setOpaque(false);

        // 1. Widened Address Query Bar with clean, balanced edge-to-edge alignment
        JPanel transQuery = new JPanel();
        transQuery.setLayout(new BoxLayout(transQuery, BoxLayout.Y_AXIS));
        transQuery.setBackground(RetroTheme.PANEL_BG);
        transQuery.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        // Row 1: Logical Address Input + Primary Translate Action (Edge-to-Edge)
        JPanel row1 = new JPanel(new BorderLayout(8, 0));
        row1.setOpaque(false);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JPanel addrGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        addrGroup.setOpaque(false);

        JLabel lblQ = new JLabel("Logical Addr:");
        lblQ.setFont(RetroTheme.RETRO_FONT_BOLD);
        lblQ.setForeground(RetroTheme.ACCENT_YELLOW);
        addrGroup.add(lblQ);

        txtQueryAddr = RetroTheme.createRetroTextField("2500", 8);
        txtQueryAddr.setPreferredSize(new Dimension(95, 28));
        txtQueryAddr.addActionListener(e -> translateAddress());
        addrGroup.add(txtQueryAddr);
        row1.add(addrGroup, BorderLayout.WEST);

        JButton btnTrans = RetroTheme.createRetroButton(">> TRANSLATE", RetroTheme.ACCENT_YELLOW, Color.BLACK);
        btnTrans.setMnemonic('T');
        btnTrans.setFont(new Font("Consolas", Font.BOLD, 11));
        btnTrans.setPreferredSize(new Dimension(145, 28));
        btnTrans.addActionListener(e -> translateAddress());
        row1.add(btnTrans, BorderLayout.EAST);

        // Row 2: Live Stream Playback & Step Controls (Uniform 3-Button Grid)
        JPanel row2 = new JPanel(new GridLayout(1, 3, 6, 0));
        row2.setOpaque(false);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row2.setPreferredSize(new Dimension(0, 28));

        JButton btnStep = RetroTheme.createRetroButton("STEP >>", new Color(34, 26, 56), RetroTheme.ACCENT_CYAN);
        btnStep.setFont(new Font("Consolas", Font.BOLD, 11));
        btnStep.setToolTipText("Advance step-by-step through sample memory addresses");
        btnStep.addActionListener(e -> stepStream());
        row2.add(btnStep);

        btnSimStream = RetroTheme.createRetroButton("> SIM STREAM", new Color(34, 26, 56), RetroTheme.ACCENT_GREEN);
        btnSimStream.setFont(new Font("Consolas", Font.BOLD, 11));
        btnSimStream.setToolTipText("Autoplay live memory translation stream across valid and trap addresses");
        btnSimStream.addActionListener(e -> toggleSimStream());
        row2.add(btnSimStream);

        JButton btnReset = RetroTheme.createRetroButton("RESET", new Color(34, 26, 56), RetroTheme.ACCENT_PINK);
        btnReset.setFont(new Font("Consolas", Font.BOLD, 11));
        btnReset.setToolTipText("Reset translation sequence to start");
        btnReset.addActionListener(e -> resetStream());
        row2.add(btnReset);

        transQuery.add(row1);
        transQuery.add(Box.createVerticalStrut(6));
        transQuery.add(row2);

        middle.add(transQuery, BorderLayout.NORTH);

        // 2. Center Column: Live MMU Frame Hopper Canvas + Decomposition + Result Card + Formula Box
        JPanel centerStack = new JPanel();
        centerStack.setLayout(new BoxLayout(centerStack, BoxLayout.Y_AXIS));
        centerStack.setOpaque(false);

        hopperCanvas = new PagingHopperCanvas(this);
        hopperCanvas.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerStack.add(hopperCanvas);
        centerStack.add(Box.createVerticalStrut(4));

        JPanel decompBar = createAddressDecompositionBar();
        decompBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerStack.add(decompBar);
        centerStack.add(Box.createVerticalStrut(4));

        JPanel resCard = createResultCard();
        resCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerStack.add(resCard);
        centerStack.add(Box.createVerticalStrut(4));

        JPanel formulaBox = createFormulaBreakdownBox();
        formulaBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerStack.add(formulaBox);

        middle.add(centerStack, BorderLayout.CENTER);
        return middle;
    }

    private JPanel createRightMemoryPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);

        // Top Stats Strip (filling space and showing memory pressure!)
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        statsPanel.setBackground(RetroTheme.PANEL_BG);
        statsPanel.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR));

        lblRamStats = new JLabel("FRAMES: 4/16 USED | FREE: 12 | RAM: 16384 B");
        lblRamStats.setFont(new Font("Consolas", Font.BOLD, 10));
        lblRamStats.setForeground(RetroTheme.ACCENT_GREEN);
        statsPanel.add(lblRamStats);
        p.add(statsPanel, BorderLayout.NORTH);

        physicalMemView = new PhysicalMemoryView(this);
        JScrollPane scrollMem = RetroTheme.createRetroScroll(physicalMemView);
        scrollMem.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR), " [ PHYSICAL RAM HARDWARE MODULE (DIMM) ] ",
                TitledBorder.LEFT, TitledBorder.TOP, RetroTheme.RETRO_FONT_BOLD, RetroTheme.ACCENT_GREEN
        ));
        scrollMem.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        p.add(scrollMem, BorderLayout.CENTER);

        // Keep history beside RAM so long page tables cannot squeeze it away.
        JPanel lowerStack = new JPanel();
        lowerStack.setLayout(new BoxLayout(lowerStack, BoxLayout.Y_AXIS));
        lowerStack.setOpaque(false);

        JPanel memLegend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        memLegend.setBackground(RetroTheme.PANEL_BG);
        memLegend.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        memLegend.add(ramLegendChip("MAPPED", RetroTheme.ACCENT_CYAN));
        memLegend.add(ramLegendChip("SELECTED", RetroTheme.ACCENT_GREEN));
        memLegend.add(ramLegendChip("FREE", RetroTheme.TEXT_DIM));
        memLegend.setAlignmentX(Component.LEFT_ALIGNMENT);
        historyPanel.setPreferredSize(new Dimension(260, 126));
        historyPanel.setMinimumSize(new Dimension(120, 112));
        historyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        lowerStack.add(memLegend);
        lowerStack.add(Box.createVerticalStrut(4));
        lowerStack.add(historyPanel);
        p.add(lowerStack, BorderLayout.SOUTH);
        return p;
    }

    private static JLabel ramLegendChip(String label, Color color) {
        JLabel l = new JLabel("\u25A0 " + label);
        l.setFont(new Font("Consolas", Font.BOLD, 10));
        l.setForeground(color);
        return l;
    }

    private JPanel createAddressDecompositionBar() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setBackground(RetroTheme.PANEL_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JLabel title = new JLabel("ADDRESS DECOMPOSITION");
        title.setFont(new Font("Consolas", Font.BOLD, 10));
        title.setForeground(RetroTheme.TEXT_DIM);
        topHeader.add(title, BorderLayout.WEST);

        lblDecompTag = new JLabel("");
        lblDecompTag.setFont(new Font("Consolas", Font.BOLD, 10));
        topHeader.add(lblDecompTag, BorderLayout.EAST);
        p.add(topHeader, BorderLayout.NORTH);

        // Hero Values Boxes (36px font, value is the HERO!)
        JPanel heroBoxes = new JPanel(new GridLayout(1, 2, 8, 0));
        heroBoxes.setOpaque(false);

        // Box 1: Page (p)
        JPanel boxP = new JPanel(new BorderLayout(2, 2));
        boxP.setBackground(new Color(14, 32, 46));
        boxP.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        JLabel lblTagP = new JLabel("PAGE NUMBER (p)", SwingConstants.CENTER);
        lblTagP.setFont(new Font("Consolas", Font.BOLD, 9));
        lblTagP.setForeground(new Color(130, 210, 240));
        boxP.add(lblTagP, BorderLayout.NORTH);

        lblHeroPage = new JLabel("--", SwingConstants.CENTER);
        lblHeroPage.setFont(new Font("Consolas", Font.BOLD, 24));
        lblHeroPage.setForeground(RetroTheme.ACCENT_CYAN);
        boxP.add(lblHeroPage, BorderLayout.CENTER);

        // Box 2: Offset (d)
        JPanel boxD = new JPanel(new BorderLayout(2, 2));
        boxD.setBackground(new Color(42, 36, 16));
        boxD.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_YELLOW, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        JLabel lblTagD = new JLabel("PAGE OFFSET (d)", SwingConstants.CENTER);
        lblTagD.setFont(new Font("Consolas", Font.BOLD, 9));
        lblTagD.setForeground(new Color(240, 220, 140));
        boxD.add(lblTagD, BorderLayout.NORTH);

        lblHeroOffset = new JLabel("--", SwingConstants.CENTER);
        lblHeroOffset.setFont(new Font("Consolas", Font.BOLD, 24));
        lblHeroOffset.setForeground(RetroTheme.ACCENT_YELLOW);
        boxD.add(lblHeroOffset, BorderLayout.CENTER);

        heroBoxes.add(boxP);
        heroBoxes.add(boxD);
        p.add(heroBoxes, BorderLayout.CENTER);

        lblDecompDetails = new JLabel("Enter logical address above and click [>> TRANSLATE]");
        lblDecompDetails.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblDecompDetails.setForeground(RetroTheme.TEXT_WHITE);
        lblDecompDetails.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 0));
        p.add(lblDecompDetails, BorderLayout.SOUTH);

        return p;
    }

    private JPanel createResultCard() {
        resultCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(18, 22, 38));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        resultCard.setLayout(new BoxLayout(resultCard, BoxLayout.Y_AXIS));
        resultCard.setOpaque(false);
        resultCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JLabel tag = new JLabel("RESOLVED PHYSICAL ADDRESS");
        tag.setFont(new Font("Consolas", Font.BOLD, 10));
        tag.setForeground(RetroTheme.TEXT_DIM);
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultCard.add(tag);
        resultCard.add(Box.createVerticalStrut(3));

        // Hero value (Always given full line height, never squashed!)
        lblResultHero = new JLabel("-- BYTES");
        lblResultHero.setFont(new Font("Consolas", Font.BOLD, 20));
        lblResultHero.setForeground(RetroTheme.ACCENT_CYAN);
        lblResultHero.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblResultHero.setMinimumSize(new Dimension(150, 26));
        lblResultHero.setPreferredSize(new Dimension(340, 26));
        resultCard.add(lblResultHero);
        resultCard.add(Box.createVerticalStrut(4));

        // Subtitle + What happens next recovery sequence
        resultSubStack = new JPanel();
        resultSubStack.setLayout(new BoxLayout(resultSubStack, BoxLayout.Y_AXIS));
        resultSubStack.setOpaque(false);
        resultSubStack.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblResultSub = new JTextArea(2, 32);
        lblResultSub.setEditable(false);
        lblResultSub.setFocusable(false);
        lblResultSub.setOpaque(false);
        lblResultSub.setLineWrap(true);
        lblResultSub.setWrapStyleWord(true);
        lblResultSub.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblResultSub.setForeground(RetroTheme.TEXT_WHITE);
        lblResultSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultSubStack.add(lblResultSub);

        lblResultRecovery = new JTextArea(2, 32);
        lblResultRecovery.setEditable(false);
        lblResultRecovery.setFocusable(false);
        lblResultRecovery.setOpaque(false);
        lblResultRecovery.setLineWrap(true);
        lblResultRecovery.setWrapStyleWord(true);
        lblResultRecovery.setFont(new Font("Consolas", Font.ITALIC, 10));
        lblResultRecovery.setForeground(new Color(255, 170, 200));
        lblResultRecovery.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultSubStack.add(Box.createVerticalStrut(2));
        resultSubStack.add(lblResultRecovery);

        resultCard.add(resultSubStack);

        resultCard.setPreferredSize(new Dimension(480, 125));
        resultCard.setMinimumSize(new Dimension(200, 115));

        // Re-wrap the message to the card's true width so it uses the full card, not a fixed 280px
        resultCard.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                applyResultTextWidth();
            }
        });
        applyResultTextWidth();

        return resultCard;
    }

    /** Refreshes the wrapping text areas after content or card-size changes. */
    private void applyResultTextWidth() {
        if (lblResultSub == null) return;
        String sub = toPlainResultText(resultSubHtml);
        lblResultSub.setText(sub);
        int columns = Math.max(18, (resultCard.getWidth() - 32) / Math.max(1, lblResultSub.getFontMetrics(lblResultSub.getFont()).charWidth('m')));
        lblResultSub.setColumns(columns);
        lblResultSub.setRows(wrappedLineCount(sub, columns));
        boolean hasRecovery = resultRecoveryHtml != null && !resultRecoveryHtml.trim().isEmpty();
        lblResultRecovery.setVisible(hasRecovery);
        if (hasRecovery) {
            String recovery = toPlainResultText(resultRecoveryHtml);
            lblResultRecovery.setText(recovery);
            lblResultRecovery.setColumns(columns);
            lblResultRecovery.setRows(wrappedLineCount(recovery, columns));
        }
        resultSubStack.revalidate();
        resultCard.revalidate();
    }

    private int wrappedLineCount(String text, int columns) {
        int lines = 1;
        int current = 0;
        for (String word : text.split("\\s+")) {
            if (current == 0) {
                current = word.length();
            } else if (current + 1 + word.length() <= columns) {
                current += 1 + word.length();
            } else {
                lines++;
                current = word.length();
            }
        }
        return Math.max(2, lines);
    }

    private String toPlainResultText(String value) {
        return value.replace("<html><body>", "")
                .replace("</body></html>", "")
                .replace("<b>", "").replace("</b>", "")
                .replace("&rarr;", "->");
    }

    private JPanel createFormulaBreakdownBox() {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);

        formulaPane = new JEditorPane();
        formulaPane.setContentType("text/html");
        formulaPane.setEditable(false);
        formulaPane.setBackground(RetroTheme.INPUT_BG);
        formulaPane.setFont(RetroTheme.RETRO_FONT);
        formulaPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        renderFormulaHtml(1024, -1, -1, -1, -1, -1, 0, "");

        p.add(formulaPane, BorderLayout.CENTER);
        return p;
    }

    /**
     * faultType: 0 = Normal Hit, 1 = Page Fault (in table, valid=0), 2 = Invalid Address (limit trap), 3 = Input Error
     */
    private void renderFormulaHtml(long ps, long la, long p, long d, int f, long pa, int faultType, String errorMsg) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='font-family:Consolas; font-size:11px; color:#eef0f2;'>");
        sb.append("<div style='color:#05d9e8; font-weight:bold; margin-bottom:4px;'>STEP-BY-STEP TRANSLATION BREAKDOWN</div>");

        sb.append("<table width='100%' cellpadding='1' cellspacing='0'>");

        sb.append("<tr><td style='color:#a0a0b8; width:50%;'>1. Page Size (S):</td>");
        sb.append("<td align='right' style='color:#ffffff; font-weight:bold;'>").append(ps > 0 ? ps : "--").append(" Bytes</td></tr>");

        sb.append("<tr><td style='color:#a0a0b8;'>2. Logical Addr (A):</td>");
        sb.append("<td align='right' style='color:#ffffff; font-weight:bold;'>").append(la >= 0 ? la : "--").append(" Bytes</td></tr>");

        sb.append("<tr><td style='color:#a0a0b8;'>3. Page Number (p):</td>");
        if (p >= 0) sb.append("<td align='right' style='color:#05d9e8; font-weight:bold;'>A / S = ").append(p).append("</td></tr>");
        else sb.append("<td align='right' style='color:#777;'>--</td></tr>");

        sb.append("<tr><td style='color:#a0a0b8;'>4. Page Offset (d):</td>");
        if (d >= 0) sb.append("<td align='right' style='color:#ffd319; font-weight:bold;'>A % S = ").append(d).append(" Bytes</td></tr>");
        else sb.append("<td align='right' style='color:#777;'>--</td></tr>");

        sb.append("<tr><td style='color:#a0a0b8;'>5. Target Frame (f):</td>");
        if (faultType == 1) {
            sb.append("<td align='right' style='color:#ff2a8d; font-weight:bold;'>valid bit = 0 -&gt; PAGE FAULT</td></tr>");
        } else if (faultType == 2) {
            // Short, single-token form so it never wraps: LIMIT TRAP (585 &gt; 15)
            sb.append("<td align='right' style='color:#ff2a8d; font-weight:bold;'>LIMIT TRAP (")
              .append(p).append(" &gt; ").append(getTableLimitPage()).append(")</td></tr>");
        } else if (f >= 0) {
            sb.append("<td align='right' style='color:#39ff14; font-weight:bold;'>PageTable[").append(p).append("] = ").append(f).append("</td></tr>");
        } else {
            sb.append("<td align='right' style='color:#777;'>--</td></tr>");
        }

        // Dedicated full-width line for Step 6 formula so it NEVER wraps or breaks across lines!
        sb.append("<tr><td colspan='2' style='color:#a0a0b8; padding-top:4px;'>6. Physical Address (P) = (f * S) + d:</td></tr>");
        if (faultType == 1) {
            sb.append("<tr><td colspan='2' style='color:#ff2a8d; font-weight:bold; padding-left:12px;'>PAGE FAULT TRAP: OS loads page from disk</td></tr>");
        } else if (faultType == 2) {
            sb.append("<tr><td colspan='2' style='color:#ff2a8d; font-weight:bold; padding-left:12px;'>INVALID ADDRESS TRAP: outside process memory</td></tr>");
        } else if (faultType == 3) {
            sb.append("<tr><td colspan='2' style='color:#ff2a8d; font-weight:bold; padding-left:12px;'>INPUT ERROR: ").append(errorMsg).append("</td></tr>");
        } else if (pa >= 0) {
            sb.append("<tr><td colspan='2' style='color:#05d9e8; font-weight:bold; font-size:12px; padding-left:12px;'>(").append(f).append(" * ").append(ps).append(" B) + ").append(d).append(" B = ").append(pa).append(" Bytes</td></tr>");
        } else {
            sb.append("<tr><td colspan='2' style='color:#777; padding-left:12px;'>--</td></tr>");
        }

        sb.append("</table>");
        sb.append("</div>");

        formulaPane.setText(sb.toString());
        formulaPane.setCaretPosition(0);
    }

    private boolean applyPageSizeText() {
        String t = txtPageSize.getText().trim();
        long v;
        try { v = Long.parseLong(t); } catch (NumberFormatException ex) { v = -1; }
        if (v <= 0 || v > Integer.MAX_VALUE) {
            handleInlineInputError("Invalid Page Size! Must be a positive integer (e.g. 512, 1024). Still using " + pageSize + " B.");
            return false; // do NOT touch pageSize, frames strip, or history
        }
        if (v != pageSize) {
            pageSize = (int) v;
            syncAllViewsAfterModelChange();
        }
        return true;
    }

    private void ensurePageSizeTextValid() {
        String t = txtPageSize.getText().trim();
        long v;
        try { v = Long.parseLong(t); } catch (NumberFormatException ex) { v = -1; }
        if (v <= 0 || v > Integer.MAX_VALUE) {
            txtPageSize.setText(String.valueOf(pageSize));
        } else if (v != pageSize) {
            pageSize = (int) v;
            syncAllViewsAfterModelChange();
        }
    }

    public void translateAddress() {
        if (!applyPageSizeText()) {
            return;
        }

        String addrText = txtQueryAddr.getText().trim();

        if (addrText.isEmpty()) {
            handleInlineInputError("Enter a logical address (non-negative integer).");
            return;
        }

        if (addrText.startsWith("+")) {
            handleInlineInputError("Logical address must be a whole number.");
            return;
        }

        if (addrText.startsWith("-")) {
            handleInlineInputError("Logical address cannot be negative.");
            return;
        }

        boolean allDigits = true;
        for (int i = 0; i < addrText.length(); i++) {
            if (!Character.isDigit(addrText.charAt(i))) {
                allDigits = false;
                break;
            }
        }

        if (!allDigits) {
            handleInlineInputError("Logical address must be a whole number.");
            return;
        }

        long logicalAddr;
        try {
            logicalAddr = Long.parseLong(addrText);
        } catch (NumberFormatException ex) {
            handleInlineInputError("Address is too large.");
            return;
        }

        long pageNumber = logicalAddr / pageSize;
        long offset = logicalAddr % pageSize;

        // Hero Values
        lblHeroPage.setText(String.valueOf(pageNumber));
        lblHeroOffset.setText(String.valueOf(offset));
        lblDecompDetails.setText(String.format("Logical Addr: %d Bytes = Page %d * %d B + Offset %d B", logicalAddr, pageNumber, pageSize, offset));

        // Scan Page Table
        Map<Integer, Integer> map = getTableMappings();
        int highestPageInTable = -1;
        int unmappedRow = -1;

        for (int r = 0; r < pageTableModel.getRowCount(); r++) {
            Object pVal = pageTableModel.getValueAt(r, 0);
            if (pVal != null && !pVal.toString().trim().isEmpty()) {
                try {
                    String cleanP = pVal.toString().replace(">", "").trim();
                    int p = Integer.parseInt(cleanP);
                    if (p > highestPageInTable) highestPageInTable = p;

                    if (p == pageNumber) {
                        String st = (String) pageTableModel.getValueAt(r, 2);
                        if (st != null && st.contains("UNMAPPED")) {
                            unmappedRow = r;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        // 2. CASE A: TRUE PAGE FAULT (Page exists in table capacity, but valid bit is 0)
        if (unmappedRow >= 0) {
            activeHighlightRow = unmappedRow;
            refreshTableHighlightMarkers();
            pageTable.scrollRectToVisible(pageTable.getCellRect(unmappedRow, 0, true));
            physicalMemView.setSelectedFrame(-1);

            lblDecompTag.setText("[PAGE FAULT]");
            lblDecompTag.setForeground(RetroTheme.ACCENT_PINK);

            resultCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(RetroTheme.ACCENT_PINK, 2),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            lblResultHero.setText("PAGE FAULT");
            lblResultHero.setForeground(RetroTheme.ACCENT_PINK);
            resultSubHtml = String.format("Page %d is in range but its valid bit = 0. The OS traps, loads the page from disk into a free frame, updates the page table, and retries.", pageNumber);
            resultRecoveryHtml = "";
            applyResultTextWidth();

            renderFormulaHtml(pageSize, logicalAddr, pageNumber, offset, -1, -1, 1, "");
            triggerCardShake();
            recordHistory(logicalAddr, pageNumber, offset, -1, -1, "PAGE FAULT");

            if (hopperCanvas != null) {
                hopperCanvas.triggerTranslation(logicalAddr, (int) pageNumber, (int) offset, -1, -1, PagingHopperCanvas.OUTCOME_PAGE_FAULT);
            }
            return;
        }

        // 3. CASE B: INVALID ADDRESS / LIMIT TRAP (Page number exceeds table limits)
        if (pageNumber > highestPageInTable || !map.containsKey((int) pageNumber)) {
            // DO NOT HIGHLIGHT ROW 0! Clear selection completely.
            activeHighlightRow = -1;
            refreshTableHighlightMarkers();
            pageTable.clearSelection();
            physicalMemView.setSelectedFrame(-1);

            lblDecompTag.setText("[INVALID ADDR]");
            lblDecompTag.setForeground(RetroTheme.ACCENT_PINK);

            resultCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(RetroTheme.ACCENT_PINK, 2),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            lblResultHero.setText("LIMIT TRAP");
            lblResultHero.setForeground(RetroTheme.ACCENT_PINK);
            resultSubHtml = String.format("INVALID ADDRESS: Requested page %d exceeds the page table limit (last page %d).", pageNumber, highestPageInTable);
            resultRecoveryHtml = "[KERNEL TRAP]: Memory limit violation (SIGSEGV). Process memory boundary exceeded.";
            applyResultTextWidth();

            renderFormulaHtml(pageSize, logicalAddr, pageNumber, offset, -1, -1, 2, "");
            triggerCardShake();
            recordHistory(logicalAddr, pageNumber, offset, -1, -1, "LIMIT TRAP");

            if (hopperCanvas != null) {
                hopperCanvas.triggerTranslation(logicalAddr, (int) pageNumber, (int) offset, -1, -1, PagingHopperCanvas.OUTCOME_LIMIT_TRAP);
            }
            return;
        }

        // 4. CASE C: NORMAL HIT
        int frame = map.get((int) pageNumber);
        long physicalAddr = ((long) frame * pageSize) + offset;

        // Highlight matching table row
        activeHighlightRow = -1;
        for (int r = 0; r < pageTableModel.getRowCount(); r++) {
            Object pVal = pageTableModel.getValueAt(r, 0);
            if (pVal != null && !pVal.toString().trim().isEmpty()) {
                try {
                    String cleanP = pVal.toString().replace(">", "").trim();
                    if (Integer.parseInt(cleanP) == pageNumber) {
                        activeHighlightRow = r;
                        pageTable.scrollRectToVisible(pageTable.getCellRect(r, 0, true));
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }
        refreshTableHighlightMarkers();

        // Highlight in Physical Memory View
        physicalMemView.setMemoryState(pageSize, map, frame);
        updateRamStats();

        lblDecompTag.setText("[VALID IN RAM]");
        lblDecompTag.setForeground(RetroTheme.ACCENT_GREEN);

        // Update Result Card (Hero value: 32px bold)
        resultCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_GREEN, 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        lblResultHero.setText(String.format("%d BYTES", physicalAddr));
        lblResultHero.setForeground(RetroTheme.ACCENT_GREEN);
        resultSubHtml = String.format("Frame %d (Base: %d B) + Offset %d B = <b>%d Bytes</b>", frame, (long) frame * pageSize, offset, physicalAddr);
        resultRecoveryHtml = "";
        applyResultTextWidth();

        renderFormulaHtml(pageSize, logicalAddr, pageNumber, offset, frame, physicalAddr, 0, "");
        recordHistory(logicalAddr, pageNumber, offset, frame, physicalAddr, "HIT");

        if (hopperCanvas != null) {
            hopperCanvas.triggerTranslation(logicalAddr, (int) pageNumber, (int) offset, frame, physicalAddr, PagingHopperCanvas.OUTCOME_HIT);
        }
    }

    private void stepStream() {
        streamIndex = (streamIndex + 1) % STREAM_ADDRESSES.length;
        txtQueryAddr.setText(String.valueOf(STREAM_ADDRESSES[streamIndex]));
        translateAddress();
    }

    private void resetStream() {
        pauseSimulation();
        streamIndex = 0;
        txtQueryAddr.setText(String.valueOf(STREAM_ADDRESSES[0]));
        translateAddress();
    }

    private void toggleSimStream() {
        if (isStreamRunning) {
            pauseSimulation();
        } else {
            isStreamRunning = true;
            if (btnSimStream != null) {
                btnSimStream.setText("PAUSE STREAM");
                btnSimStream.setForeground(RetroTheme.ACCENT_YELLOW);
            }
            if (streamTimer == null) {
                streamTimer = new javax.swing.Timer(2500, e -> {
                    if (!isShowing()) {
                        pauseSimulation();
                        return;
                    }
                    stepStream();
                });
            }
            streamTimer.start();
            stepStream();
        }
    }

    public void pauseSimulation() {
        if (streamTimer != null && streamTimer.isRunning()) {
            streamTimer.stop();
        }
        isStreamRunning = false;
        if (btnSimStream != null) {
            btnSimStream.setText("> SIM STREAM");
            btnSimStream.setForeground(RetroTheme.ACCENT_GREEN);
        }
    }

    private void handleInlineInputError(String message) {
        activeHighlightRow = -1;
        refreshTableHighlightMarkers();
        pageTable.clearSelection();
        physicalMemView.setSelectedFrame(-1);

        lblHeroPage.setText("--");
        lblHeroOffset.setText("--");
        lblDecompTag.setText("[INPUT ERROR]");
        lblDecompTag.setForeground(RetroTheme.ACCENT_PINK);
        lblDecompDetails.setText(message);

        resultCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_PINK, 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        lblResultHero.setText("INPUT ERROR");
        lblResultHero.setForeground(RetroTheme.ACCENT_PINK);
        resultSubHtml = message;
        resultRecoveryHtml = "";
        applyResultTextWidth();

        renderFormulaHtml(pageSize, -1, -1, -1, -1, -1, 3, message);
        triggerCardShake();
    }

    private void refreshTableHighlightMarkers() {
        for (int r = 0; r < pageTableModel.getRowCount(); r++) {
            Object pVal = pageTableModel.getValueAt(r, 0);
            if (pVal != null) {
                String raw = pVal.toString().replace(">", "").trim();
                if (r == activeHighlightRow) {
                    pageTableModel.setValueAt("> " + raw, r, 0);
                } else {
                    pageTableModel.setValueAt(raw, r, 0);
                }
            }
        }
        pageTable.repaint();
    }

    private void recordHistory(long la, long p, long d, int f, long pa, String outcome) {
        // Merge repeats of the same address (keeps the newest result) instead of stacking duplicates.
        int existing = -1;
        for (int i = 0; i < historyEntries.size(); i++) {
            if (historyEntries.get(i).la == la) { existing = i; break; }
        }
        HistoryEntry he;
        if (existing >= 0) {
            he = historyEntries.remove(existing);
            he.count++;
            he.p = p; he.d = d; he.f = f; he.pa = pa; he.outcome = outcome;
        } else {
            he = new HistoryEntry(la, p, d, f, pa, outcome);
        }
        historyEntries.add(0, he);
        while (historyEntries.size() > HISTORY_LIMIT) {
            historyEntries.remove(historyEntries.size() - 1);
        }
        rebuildHistoryModel();
    }

    private void rebuildHistoryModel() {
        if (historyListModel == null) return;
        historyListModel.clear();
        for (HistoryEntry he : historyEntries) {
            String item;
            if (he.outcome.equals("HIT")) {
                item = String.format("%d -> P%d+%d -> F%d = %d [HIT]", he.la, he.p, he.d, he.f, he.pa);
            } else if (he.outcome.equals("PAGE FAULT")) {
                item = String.format("%d -> P%d+%d -> PAGE FAULT", he.la, he.p, he.d);
            } else {
                item = String.format("%d -> P%d -> %s", he.la, he.p, he.outcome);
            }
            if (he.count > 1) item += String.format("  x%d", he.count);
            historyListModel.addElement(item);
        }
        boolean empty = historyEntries.isEmpty();
        if (historyCardLayout != null && historyContent != null) {
            historyCardLayout.show(historyContent, empty ? "empty" : "history");
        }
        if (historyScroll != null) { historyScroll.revalidate(); historyScroll.repaint(); }
    }

    /** Highest logical page currently present in the table = the addressable page limit. */
    private int getTableLimitPage() {
        if (pageTableModel == null) return -1;
        int max = -1;
        for (int r = 0; r < pageTableModel.getRowCount(); r++) {
            Object v = pageTableModel.getValueAt(r, 0);
            if (v != null) {
                String s = v.toString().replace(">", "").trim();
                if (!s.isEmpty()) {
                    try { max = Math.max(max, Integer.parseInt(s)); } catch (Exception ignored) {}
                }
            }
        }
        return max;
    }

    private void triggerCardShake() {
        if (shakeTimer != null && shakeTimer.isRunning()) shakeTimer.stop();
        shakeTicks = 0;
        final Point orig = resultCard.getLocation();
        shakeTimer = new javax.swing.Timer(25, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shakeTicks++;
                int dx = (int) (Math.sin(shakeTicks * 1.5) * (8 - shakeTicks));
                resultCard.setLocation(orig.x + dx, orig.y);
                if (shakeTicks >= 8) {
                    shakeTimer.stop();
                    resultCard.setLocation(orig);
                }
            }
        });
        shakeTimer.start();
    }

    private Map<Integer, Integer> getTableMappings() {
        Map<Integer, Integer> map = new LinkedHashMap<>();
        Set<Integer> ownedFrames = new HashSet<>();
        for (int i = 0; i < pageTableModel.getRowCount(); i++) {
            Object pObj = pageTableModel.getValueAt(i, 0);
            Object fObj = pageTableModel.getValueAt(i, 1);
            Object sObj = pageTableModel.getValueAt(i, 2);
            if (pObj != null && fObj != null && sObj != null) {
                String pStr = pObj.toString().replace(">", "").trim();
                String fStr = fObj.toString().trim();
                String sStr = sObj.toString().trim();
                if (sStr.contains("VALID") && !pStr.isEmpty() && !fStr.isEmpty() && !fStr.equals("--")) {
                    try {
                        int page = Integer.parseInt(pStr);
                        int frame = Integer.parseInt(fStr);
                        if (frame >= 0 && frame < 16 && ownedFrames.add(frame)) {
                            map.put(page, frame);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return map;
    }

    private void showTableHint(String msg) {
        if (lblTableHint != null && legendCardLayout != null && legendCard != null) {
            lblTableHint.setText(msg);
            legendCardLayout.show(legendCard, "hint");
            if (hintTimer != null && hintTimer.isRunning()) hintTimer.stop();
            hintTimer = new javax.swing.Timer(2000, e -> {
                legendCardLayout.show(legendCard, "legend");
            });
            hintTimer.setRepeats(false);
            hintTimer.start();
        }
    }

    private void syncAllViewsAfterModelChange() {
        activeHighlightRow = -1;
        refreshTableHighlightMarkers();
        pageTable.clearSelection();
        clearTranslationHistory();
        refreshPhysicalMemoryView();
        int last = pageTableModel.getRowCount() - 1;
        if (last >= 0) {
            pageTable.scrollRectToVisible(pageTable.getCellRect(last, 0, true));
        }
    }

    private void addPageRow() {
        ensurePageSizeTextValid();
        int nextP = 0;
        for (int i = 0; i < pageTableModel.getRowCount(); i++) {
            try {
                String raw = pageTableModel.getValueAt(i, 0).toString().replace(">", "").trim();
                int p = Integer.parseInt(raw);
                if (p >= nextP) nextP = p + 1;
            } catch (Exception ignored) {}
        }
        Set<Integer> usedFrames = new HashSet<>(getTableMappings().values());
        int freeFrame = -1;
        for (int f = 0; f < 16; f++) {
            if (!usedFrames.contains(f)) {
                freeFrame = f;
                break;
            }
        }
        if (freeFrame >= 0) {
            pageTableModel.addRow(new Object[]{String.valueOf(nextP), String.valueOf(freeFrame), "[V] VALID"});
        } else {
            pageTableModel.addRow(new Object[]{String.valueOf(nextP), "--", "[X] UNMAPPED"});
        }
        syncAllViewsAfterModelChange();
    }

    private void removePageRow() {
        ensurePageSizeTextValid();
        if (pageTableModel.getRowCount() <= 1) {
            showTableHint("Page table needs at least 1 page.");
            return;
        }
        int lastRow = pageTableModel.getRowCount() - 1;
        pageTableModel.removeRow(lastRow);
        syncAllViewsAfterModelChange();
    }

    private void clearTranslationHistory() {
        historyEntries.clear();
        rebuildHistoryModel();
        showIdleState();
    }

    private void loadSmallPreset() {
        pageSize = 1024;
        txtPageSize.setText("1024");
        pageTableModel.setRowCount(0);
        pageTableModel.addRow(new Object[]{"0", "5", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"1", "6", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"2", "1", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"3", "2", "[V] VALID"});
        padEmptyCapacityRows(8);
        syncAllViewsAfterModelChange();
    }

    public void loadManualExample() {
        pageSize = 512;
        txtPageSize.setText("512");
        pageTableModel.setRowCount(0);
        pageTableModel.addRow(new Object[]{"0", "3", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"1", "1", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"2", "5", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"3", "2", "[V] VALID"});
        syncAllViewsAfterModelChange();
        txtQueryAddr.setText("1040");
        translateAddress();
    }

    private void loadLargePreset() {
        pageSize = 1024;
        txtPageSize.setText("1024");
        pageTableModel.setRowCount(0);
        pageTableModel.addRow(new Object[]{"0", "5", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"1", "6", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"2", "1", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"3", "2", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"4", "--", "[X] UNMAPPED"});
        pageTableModel.addRow(new Object[]{"5", "--", "[X] UNMAPPED"});
        pageTableModel.addRow(new Object[]{"6", "--", "[X] UNMAPPED"});
        pageTableModel.addRow(new Object[]{"7", "--", "[X] UNMAPPED"});
        pageTableModel.addRow(new Object[]{"8", "0", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"9", "3", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"10", "4", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"11", "7", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"12", "8", "[V] VALID"});
        pageTableModel.addRow(new Object[]{"13", "9", "[V] VALID"});
        syncAllViewsAfterModelChange();
    }

    private void padEmptyCapacityRows(int targetRows) {
        int cur = pageTableModel.getRowCount();
        for (int i = cur; i < targetRows; i++) {
            pageTableModel.addRow(new Object[]{String.valueOf(i), "--", "[X] UNMAPPED"});
        }
    }

    private void showIdleState() {
        String pageSizeLabel = pageSize + " B";
        activeHighlightRow = -1;
        refreshTableHighlightMarkers();
        pageTable.clearSelection();
        physicalMemView.setSelectedFrame(-1);
        lblHeroPage.setText("--");
        lblHeroOffset.setText("--");
        lblDecompTag.setText("");
        lblDecompDetails.setText(String.format("Logical Address: -- Bytes (Page Size: %s)", pageSizeLabel));

        resultCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        lblResultHero.setText("-- BYTES");
        lblResultHero.setForeground(RetroTheme.ACCENT_CYAN);
        resultSubHtml = "Enter address above & click TRANSLATE";
        resultRecoveryHtml = "";
        applyResultTextWidth();

        renderFormulaHtml(pageSize, -1, -1, -1, -1, -1, 0, "");
    }

    private void refreshPhysicalMemoryView() {
        normalizeFrameMappings();
        Map<Integer, Integer> map = getTableMappings();
        physicalMemView.setMemoryState(pageSize, map, -1);
        updateRamStats();
    }

    private void normalizeFrameMappings() {
        Set<Integer> owners = new HashSet<>();
        for (int row = 0; row < pageTableModel.getRowCount(); row++) {
            String status = String.valueOf(pageTableModel.getValueAt(row, 2));
            if (!status.contains("VALID")) continue;
            try {
                int frame = Integer.parseInt(String.valueOf(pageTableModel.getValueAt(row, 1)).trim());
                if (frame < 0 || frame >= 16 || !owners.add(frame)) {
                    pageTableModel.setValueAt("--", row, 1);
                    pageTableModel.setValueAt("[X] UNMAPPED", row, 2);
                }
            } catch (NumberFormatException ex) {
                pageTableModel.setValueAt("--", row, 1);
                pageTableModel.setValueAt("[X] UNMAPPED", row, 2);
            }
        }
    }

    private void updateRamStats() {
        // Count the same unique frame-owner map that the RAM view paints.
        int used = physicalMemView.getUsedFrameCount();
        int total = 16;
        int free = Math.max(0, total - used);
        long totalRam = (long) total * pageSize;
        lblRamStats.setText(String.format("FRAMES: %d/%d USED | FREE: %d | TOTAL RAM: %d B", used, total, free, totalRam));
    }

    private void launchFrameHopper() {
        ensurePageSizeTextValid();
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        FrameHopperDialog dlg = new FrameHopperDialog(topFrame, pageSize, getTableMappings(), getPageTableCapacity());
        dlg.setVisible(true);
    }

    private int getPageTableCapacity() {
        int maxPage = -1;
        for (int row = 0; row < pageTableModel.getRowCount(); row++) {
            Object value = pageTableModel.getValueAt(row, 0);
            if (value == null) continue;
            try {
                String page = value.toString().replace(">", "").trim();
                if (!page.isEmpty()) maxPage = Math.max(maxPage, Integer.parseInt(page));
            } catch (NumberFormatException ignored) {}
        }
        return maxPage + 1;
    }

    // =========================================================================
    // =========================================================================
    // PHYSICAL MEMORY VIEW: REALISTIC HARDWARE RAM STICK (DIMM MODULE)
    // =========================================================================
    private static class PhysicalMemoryView extends JPanel {
        private final PagingPanel owner;
        private long pageSize = 1024;
        private Map<Integer, Integer> pageToFrameMap = new LinkedHashMap<>();
        private Map<Integer, Integer> frameToPageMap = new LinkedHashMap<>();
        private int selectedFrame = -1;
        private static final int TOTAL_FRAMES = 16;
        private static final int FRAMES_PER_COL = 8; // Column-major: Bank A (0..7), Bank B (8..15)

        public PhysicalMemoryView(PagingPanel owner) {
            this.owner = owner;
            setBackground(new Color(10, 8, 18));
            setPreferredSize(new Dimension(280, 360));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleChipClick(e.getX(), e.getY());
                }
            });
        }

        public void setMemoryState(long ps, Map<Integer, Integer> p2f, int selFrame) {
            this.pageSize = (ps > 0) ? ps : 1024;
            this.pageToFrameMap = (p2f != null) ? new LinkedHashMap<>(p2f) : new LinkedHashMap<>();
            this.frameToPageMap.clear();

            for (Map.Entry<Integer, Integer> e : this.pageToFrameMap.entrySet()) {
                frameToPageMap.put(e.getValue(), e.getKey());
            }
            this.selectedFrame = selFrame;
            repaint();
        }

        public int getUsedFrameCount() {
            return frameToPageMap.size();
        }

        public void setSelectedFrame(int f) {
            this.selectedFrame = f;
            repaint();
        }

        private void handleChipClick(int mx, int my) {
            int w = getWidth();
            int ramMarginX = 6;
            int ramW = w - (ramMarginX * 2);
            int ramTop = 6;
            int chipStartY = ramTop + 30;
            int colGap = 8;
            int availChipW = ramW - 16 - colGap;
            int chipW = availChipW / 2;
            int chipH = 31;
            int chipRowGap = 5;

            for (int f = 0; f < TOTAL_FRAMES; f++) {
                int col = f / FRAMES_PER_COL;
                int row = f % FRAMES_PER_COL;
                int cx = ramMarginX + 8 + col * (chipW + colGap);
                int cy = chipStartY + row * (chipH + chipRowGap);

                if (mx >= cx && mx <= cx + chipW && my >= cy && my <= cy + chipH) {
                    if (frameToPageMap.containsKey(f) && owner != null) {
                        int page = frameToPageMap.get(f);
                        long targetAddr = (long) page * pageSize;
                        owner.txtQueryAddr.setText(String.valueOf(targetAddr));
                        owner.translateAddress();
                    } else if (owner != null) {
                        long base = (long) f * pageSize;
                        owner.txtQueryAddr.setText(String.valueOf(base));
                        owner.translateAddress();
                    }
                    break;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Motherboard Tray Background
            g2.setColor(new Color(10, 8, 18));
            g2.fillRect(0, 0, w, h);

            // Subtle PCB Background Circuit Traces
            g2.setColor(new Color(18, 22, 34));
            g2.setStroke(new BasicStroke(1.0f));
            for (int ty = 20; ty < h - 30; ty += 36) {
                g2.drawLine(2, ty, w - 2, ty);
            }

            // 2. RAM Stick (DIMM PCB Substrate)
            int ramMarginX = 6;
            int ramW = w - (ramMarginX * 2);
            int ramTop = 6;
            int goldPinH = 20;
            int ramH = h - ramTop - 8;

            // PCB Body: Deep Matte Cyber Emerald Green Finish
            GradientPaint pcbPaint = new GradientPaint(
                    ramMarginX, ramTop, new Color(13, 34, 26),
                    ramMarginX + ramW, ramTop + ramH, new Color(7, 20, 15)
            );
            g2.setPaint(pcbPaint);
            g2.fillRoundRect(ramMarginX, ramTop, ramW, ramH, 8, 8);

            // PCB Border
            g2.setColor(new Color(28, 75, 52));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(ramMarginX, ramTop, ramW, ramH, 8, 8);

            // Side Mechanical Retention Notches (DIMM Socket Clip Notches)
            int notchY = ramTop + (ramH / 2) - 10;
            g2.setColor(new Color(10, 8, 18));
            g2.fillRoundRect(ramMarginX - 4, notchY, 8, 20, 4, 4);
            g2.setColor(new Color(35, 90, 65));
            g2.drawRoundRect(ramMarginX - 4, notchY, 8, 20, 4, 4);

            g2.setColor(new Color(10, 8, 18));
            g2.fillRoundRect(ramMarginX + ramW - 4, notchY, 8, 20, 4, 4);
            g2.setColor(new Color(35, 90, 65));
            g2.drawRoundRect(ramMarginX + ramW - 4, notchY, 8, 20, 4, 4);

            // DIMM Silk-Screen Title & Specs
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            g2.setColor(new Color(90, 200, 140));
            g2.drawString("RAM-MODULE // DDR-MMU 16KB", ramMarginX + 10, ramTop + 13);

            g2.setFont(new Font("Consolas", Font.PLAIN, 8));
            g2.setColor(new Color(80, 140, 110));
            g2.drawString("BANK A (F00-F07)", ramMarginX + 10, ramTop + 25);
            g2.drawString("BANK B (F08-F15)", ramMarginX + (ramW / 2) + 6, ramTop + 25);

            // Divider trace separating Bank A and Bank B
            g2.setColor(new Color(22, 58, 44));
            g2.drawLine(ramMarginX + (ramW / 2), ramTop + 16, ramMarginX + (ramW / 2), ramTop + ramH - goldPinH - 6);

            // 3. 16 DRAM IC CHIPS (Surface-Mount Memory Chips with Pin Legs)
            int chipStartY = ramTop + 29;
            int colGap = 8;
            int availChipW = ramW - 16 - colGap;
            int chipW = availChipW / 2;
            int chipH = 31;
            int chipRowGap = 5;

            for (int f = 0; f < TOTAL_FRAMES; f++) {
                int col = f / FRAMES_PER_COL; // 0 for Bank A, 1 for Bank B
                int row = f % FRAMES_PER_COL; // 0..7
                int cx = ramMarginX + 8 + col * (chipW + colGap);
                int cy = chipStartY + row * (chipH + chipRowGap);

                boolean isSelected = (f == selectedFrame);
                boolean isMapped = frameToPageMap.containsKey(f);
                long baseAddr = (long) f * pageSize;

                // Metallic Silver Solder Legs on left and right sides of each DRAM chip
                g2.setColor(isSelected ? new Color(120, 255, 140) : (isMapped ? new Color(180, 190, 215) : new Color(90, 100, 115)));
                for (int p = 0; p < 3; p++) {
                    int legY = cy + 6 + (p * 8);
                    g2.drawLine(cx - 3, legY, cx, legY);
                    g2.drawLine(cx + chipW, legY, cx + chipW + 3, legY);
                }

                // DRAM Chip Body (Charcoal Epoxy Mold Package)
                if (isSelected) {
                    // Active Target Chip Glow
                    g2.setColor(new Color(18, 60, 38));
                    g2.fillRoundRect(cx, cy, chipW, chipH, 4, 4);

                    g2.setColor(RetroTheme.ACCENT_GREEN);
                    g2.setStroke(new BasicStroke(2.0f));
                    g2.drawRoundRect(cx, cy, chipW, chipH, 4, 4);

                    // Glowing Active LED
                    g2.setColor(new Color(57, 255, 20));
                    g2.fillOval(cx + chipW - 8, cy + 4, 5, 5);
                } else if (isMapped) {
                    // Mapped Page Chip
                    g2.setColor(new Color(22, 25, 36));
                    g2.fillRoundRect(cx, cy, chipW, chipH, 4, 4);

                    g2.setColor(new Color(65, 55, 105));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(cx, cy, chipW, chipH, 4, 4);

                    // Mapped LED (Cyan)
                    g2.setColor(RetroTheme.ACCENT_CYAN);
                    g2.fillOval(cx + chipW - 8, cy + 4, 4, 4);
                } else {
                    // Empty / Free Chip
                    g2.setColor(new Color(15, 18, 24));
                    g2.fillRoundRect(cx, cy, chipW, chipH, 4, 4);

                    g2.setColor(new Color(32, 38, 48));
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(cx, cy, chipW, chipH, 4, 4);

                    // Dormant LED (Dim Gray)
                    g2.setColor(new Color(45, 55, 65));
                    g2.fillOval(cx + chipW - 8, cy + 4, 3, 3);
                }

                // Orientation notch dot (Pin 1 marker)
                g2.setColor(new Color(70, 75, 85));
                g2.fillOval(cx + 3, cy + 3, 2, 2);

                // Chip Label (e.g. F01)
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(isSelected ? RetroTheme.ACCENT_GREEN : (isMapped ? RetroTheme.ACCENT_CYAN : new Color(110, 120, 135)));
                String frameStr = String.format("F%02d", f);
                g2.drawString(frameStr, cx + 5, cy + 12);

                // Base Address Bytes
                g2.setFont(new Font("Consolas", Font.PLAIN, 8));
                g2.setColor(isSelected ? Color.WHITE : (isMapped ? new Color(195, 200, 220) : new Color(75, 85, 95)));
                g2.drawString(String.format("[%dB]", baseAddr), cx + 5, cy + 24);

                // Mapped Page Badge
                if (isMapped) {
                    int p = frameToPageMap.get(f);
                    int badgeW = 32;
                    int bx = cx + chipW - badgeW - 11;
                    g2.setColor(isSelected ? new Color(35, 80, 52) : new Color(36, 26, 64));
                    g2.fillRoundRect(bx, cy + 8, badgeW, 15, 3, 3);
                    g2.setColor(isSelected ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_YELLOW);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(bx, cy + 8, badgeW, 15, 3, 3);

                    g2.setFont(new Font("Consolas", Font.BOLD, 9));
                    g2.drawString(String.format("P%d", p), bx + 5, cy + 19);
                } else {
                    g2.setFont(new Font("Consolas", Font.PLAIN, 8));
                    g2.setColor(new Color(65, 75, 85));
                    g2.drawString("FREE", cx + chipW - 34, cy + 20);
                }
            }

            // 4. GOLD CONNECTOR PINS (Motherboard Edge Connector / Gold Fingers)
            int pinY = ramTop + ramH - goldPinH;
            int pinW = ramW - 14;
            int pinStartX = ramMarginX + 7;

            // Gold Bus Strip Background with rich metallic gradient
            GradientPaint goldBg = new GradientPaint(
                    pinStartX, pinY, new Color(215, 170, 40),
                    pinStartX, pinY + goldPinH, new Color(140, 100, 18)
            );
            g2.setPaint(goldBg);
            g2.fillRoundRect(pinStartX, pinY, pinW, goldPinH, 3, 3);

            // Individual Gold Fingers (32 Realistic Gold Pins with Center Key Notch)
            int numPins = 32;
            int pinStep = Math.max(3, pinW / numPins);
            int keyHoleIndex = numPins / 2;

            for (int i = 0; i < numPins; i++) {
                int px = pinStartX + (i * pinStep);
                // Center Key Notch
                if (i >= keyHoleIndex - 1 && i <= keyHoleIndex + 1) {
                    g2.setColor(new Color(10, 8, 18));
                    g2.fillRect(px, pinY - 1, pinStep, goldPinH + 3);
                    continue;
                }
                // Gold Pin Highlight
                g2.setColor(new Color(255, 240, 130));
                g2.drawLine(px, pinY + 2, px, pinY + goldPinH - 2);
                // Gold Pin Shadow
                g2.setColor(new Color(115, 85, 12));
                g2.drawLine(px + 1, pinY + 2, px + 1, pinY + goldPinH - 2);
            }

            // Silk-screen Gold Bus Label
            g2.setFont(new Font("Consolas", Font.BOLD, 8));
            g2.setColor(new Color(40, 28, 0));
            g2.drawString("GOLD BUS // 64-BIT CHANNEL", pinStartX + 6, pinY + 13);

            g2.dispose();
        }
    }
}
