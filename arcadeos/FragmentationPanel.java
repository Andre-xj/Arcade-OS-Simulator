package arcadeos;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * MISSION 06: RAM ALLOCATION & FRAGMENTATION // SILICON MMU LAB
 *
 * 16-Bit Cyber Motherboard Architecture:
 * - Authentic Cyber RAM DIMM sticks with glowing gold pins and blinking status LEDs.
 * - Dynamic process chip avatars (Robot, Alien, Ghost, UFO) slotted into physical memory modules.
 * - Animated Thermal Heat-Sink internal fragmentation chambers with glowing hazard stripes and rising heat particles.
 * - Holographic External Fragmentation alert bridge linking scattered disjoint holes.
 * - Process Microchip Cartridge Conveyor with DIP pinouts and live injection laser guidance.
 * - Hardware Policy Selector Bar (FIRST FIT, BEST FIT, NEXT FIT) with glowing indicator LEDs.
 * - Full Physical Memory Address Translation Ledger (Base Address, Limit, Socket, Frag, MMU Reason).
 * - Direct launcher for RAM Defragger: Cyber Stacker Arcade.
 */
public class FragmentationPanel extends JPanel {

    public enum Strategy {
        FIRST_FIT("FIRST FIT", "Allocates the first memory partition with capacity >= demand."),
        BEST_FIT("BEST FIT", "Allocates partition leaving the minimum leftover internal frag."),
        NEXT_FIT("NEXT FIT", "First Fit resuming search from the last allocated socket.");

        public final String title;
        public final String description;
        Strategy(String t, String d) { this.title = t; this.description = d; }
    }

    public static class RamPartition {
        public int id;
        public int capacityKb;
        public int startAddr;
        public int endAddr;
        public ProcessChip slottedChip = null;
        public int internalFragKb = 0;

        public RamPartition(int id, int capacityKb, int startAddr) {
            this.id = id;
            this.capacityKb = capacityKb;
            this.startAddr = startAddr;
            this.endAddr = startAddr + capacityKb;
        }

        public boolean isFree() {
            return slottedChip == null;
        }
    }

    public static class ProcessChip {
        public String pid;
        public int demandKb;
        public int assignedSocketId = -1;
        public int internalFragKb = 0;
        public String status = "READY";
        public String searchReason = "Awaiting bus allocation...";
        public Color chipColor;

        public ProcessChip(String pid, int demandKb, Color chipColor) {
            this.pid = pid;
            this.demandKb = demandKb;
            this.chipColor = chipColor;
        }
    }

    public static class AllocationEvent {
        public int chipIndex;
        public int socketIndex; // -1 if unallocated
        public String pid;
        public int demandKb;
        public int socketCapacityKb;
        public int internalFragKb;
        public String rationale;

        public AllocationEvent(int chipIndex, int socketIndex, String pid,
                               int demandKb, int socketCapacityKb, int internalFragKb, String rationale) {
            this.chipIndex = chipIndex;
            this.socketIndex = socketIndex;
            this.pid = pid;
            this.demandKb = demandKb;
            this.socketCapacityKb = socketCapacityKb;
            this.internalFragKb = internalFragKb;
            this.rationale = rationale;
        }
    }

    // State Variables
    private Strategy activeStrategy = Strategy.BEST_FIT;
    private final List<Integer> initialSocketSizes = new ArrayList<>();
    private final List<Integer> initialChipDemands = new ArrayList<>();

    private final List<RamPartition> partitions = new ArrayList<>();
    private final List<ProcessChip> chips = new ArrayList<>();
    private final List<AllocationEvent> eventLog = new ArrayList<>();

    private int currentStepIndex = 0;
    private boolean isAutoRunning = false;
    private int speedFactor = 1;
    private javax.swing.Timer mmuClockTimer;
    private boolean isManualSocketingMode = false;

    // Metrics
    private int totalInternalFragKb = 0;
    private int totalExternalFragKb = 0;
    private int totalRamCapacityKb = 0;
    private int totalAllocatedKb = 0;
    private boolean isExternalFragHazard = false;

    // UI Elements
    private JLabel lblBiosTitle, lblMmuStatus;
    private JButton btnRun, btnStep, btnFlush, btnDefrag, btnClockSpeed, btnManualMode, btnArcadeCabinet;
    private JButton[] policyButtons;
    private JTextField txtSocketsConfig, txtChipsConfig;
    private SiliconMotherboardCanvas motherboardCanvas;
    private DefaultTableModel translationTableModel;
    private JTable translationTable;
    private JLabel lblStatTotalRam, lblStatInternalFrag, lblStatExternalFrag, lblStatUtilization;

    private final Color[] CHIP_NEON_COLORS = {
            RetroTheme.ACCENT_CYAN, RetroTheme.ACCENT_GREEN, RetroTheme.ACCENT_YELLOW,
            RetroTheme.ACCENT_PINK, new Color(185, 95, 255), new Color(255, 145, 50),
            new Color(90, 230, 255), new Color(255, 110, 180)
    };

    public FragmentationPanel() {
        setBackground(new Color(10, 14, 20)); // Deep silicon PCB black-green
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        // MMU Clock Timer (60 FPS animation & stepped auto-run)
        mmuClockTimer = new javax.swing.Timer(850, e -> {
            if (isAutoRunning) {
                if (currentStepIndex < chips.size()) {
                    executeStepForward();
                } else {
                    haltAutoRun();
                }
            }
        });

        // Initialize Defaults directly into lists
        initialSocketSizes.addAll(Arrays.asList(100, 500, 200, 300, 600));
        initialChipDemands.addAll(Arrays.asList(212, 417, 112, 426));

        // 1. Top Section: MMU BIOS Header + Unified Retro Control Cockpit
        add(createBiosAndControlsCockpit(), BorderLayout.NORTH);

        // 2. Center Stage: Silicon Motherboard Canvas (Full Height Guaranteed)
        motherboardCanvas = new SiliconMotherboardCanvas(this);
        motherboardCanvas.setPreferredSize(new Dimension(1200, 330));
        motherboardCanvas.setMinimumSize(new Dimension(880, 280));
        add(motherboardCanvas, BorderLayout.CENTER);

        // 3. Bottom Section: Translation Ledger & Telemetry Inputs Deck
        JPanel bottomDeck = createMemoryTranslationDeck();
        bottomDeck.setPreferredSize(new Dimension(1200, 220));
        add(bottomDeck, BorderLayout.SOUTH);

        // Initial Silicon State Computation
        recomputeSiliconState();
    }

    // =========================================================================
    // 1. UNIFIED MMU BIOS HEADER & RETRO CONTROL COCKPIT
    // =========================================================================
    private JPanel createBiosAndControlsCockpit() {
        JPanel cockpit = new JPanel(new GridLayout(2, 1, 0, 4));
        cockpit.setOpaque(false);

        // --- ROW 1: BIOS Header & Policy Selector & Arcade Launcher ---
        JPanel row1 = new JPanel(new BorderLayout(10, 0));
        row1.setOpaque(true);
        row1.setBackground(new Color(18, 14, 32));
        row1.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 45, 80), 1),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        // Left BIOS Title
        JPanel leftBios = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftBios.setOpaque(false);
        JLabel icIcon = new JLabel("⚡ MMU//06");
        icIcon.setFont(new Font("Consolas", Font.BOLD, 12));
        icIcon.setForeground(RetroTheme.ACCENT_GREEN);
        leftBios.add(icIcon);

        lblBiosTitle = new JLabel("SILICON RAM ALLOCATION // CYBER CRANE ATTACK MATRIX");
        lblBiosTitle.setFont(new Font("Consolas", Font.BOLD, 11));
        lblBiosTitle.setForeground(RetroTheme.TEXT_WHITE);
        leftBios.add(lblBiosTitle);
        row1.add(leftBios, BorderLayout.WEST);

        // Center: Hardware Policy Radio Bar
        JPanel policyBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        policyBar.setOpaque(false);
        JLabel lPol = new JLabel("POLICY:");
        lPol.setFont(new Font("Consolas", Font.BOLD, 10));
        lPol.setForeground(RetroTheme.ACCENT_YELLOW);
        policyBar.add(lPol);

        Strategy[] allStrats = Strategy.values();
        policyButtons = new JButton[allStrats.length];
        for (int i = 0; i < allStrats.length; i++) {
            final Strategy st = allStrats[i];
            JButton b = RetroTheme.createRetroButton(st.title, new Color(28, 20, 48), RetroTheme.TEXT_DIM);
            b.setFont(new Font("Consolas", Font.BOLD, 10));
            b.addActionListener(e -> {
                this.activeStrategy = st;
                updatePolicyButtonsUI();
                resetSimulation();
            });
            policyButtons[i] = b;
            policyBar.add(b);
        }
        updatePolicyButtonsUI();
        row1.add(policyBar, BorderLayout.CENTER);

        // East Header: Clean Status Text + Play Cyber Crane Attack Arcade Button
        JPanel eastHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        eastHeader.setOpaque(false);

        lblMmuStatus = new JLabel(" [ MMU SOLVENT // READY TO INJECT ] ");
        lblMmuStatus.setFont(new Font("Consolas", Font.BOLD, 10));
        lblMmuStatus.setForeground(RetroTheme.ACCENT_GREEN);
        lblMmuStatus.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        eastHeader.add(lblMmuStatus);

        btnArcadeCabinet = RetroTheme.createRetroButton("PLAY CYBER CRANE ATTACK ARCADE", new Color(42, 16, 24), RetroTheme.ACCENT_YELLOW);
        btnArcadeCabinet.setFont(new Font("Consolas", Font.BOLD, 10));
        btnArcadeCabinet.setIcon(new ImageIcon(PixelSprites.createCraneSprite(14, RetroTheme.ACCENT_YELLOW)));
        btnArcadeCabinet.setIconTextGap(6);
        btnArcadeCabinet.setToolTipText("Launch playable Cyber Crane Attack arcade minigame!");
        btnArcadeCabinet.addActionListener(e -> {
            haltAutoRun();
            List<Integer> gameSockets = new ArrayList<>();
            if (initialSocketSizes.size() >= 3) {
                gameSockets.addAll(initialSocketSizes);
            } else {
                gameSockets.addAll(Arrays.asList(100, 500, 200, 300, 600));
            }
            List<Integer> gameDemands = new ArrayList<>();
            if (initialChipDemands.size() >= 3) {
                gameDemands.addAll(initialChipDemands);
            } else {
                gameDemands.addAll(Arrays.asList(212, 417, 112, 426, 95, 180, 250));
            }
            DefragPanicDialog dlg = new DefragPanicDialog(
                    (JFrame) SwingUtilities.getWindowAncestor(this),
                    this,
                    gameSockets,
                    gameDemands
            );
            dlg.setVisible(true);
        });
        eastHeader.add(btnArcadeCabinet);
        row1.add(eastHeader, BorderLayout.EAST);

        // --- ROW 2: Transport Console & Scenario Presets ---
        JPanel row2 = new JPanel(new BorderLayout(8, 0));
        row2.setOpaque(true);
        row2.setBackground(new Color(22, 16, 42));
        row2.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // Transport Actions (Left) - Tactile 3D Arcade Buttons with Icons
        JPanel leftTransport = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftTransport.setOpaque(false);

        btnRun = CPUSchedulingPanel.createArcadeButton("AUTO ALLOC", new Color(0, 150, 60), Color.WHITE, 28);
        btnRun.setIcon(RetroIcons.createPlayIcon(11, Color.WHITE));
        btnRun.setFont(new Font("Consolas", Font.BOLD, 10));

        btnStep = CPUSchedulingPanel.createArcadeButton("STEP INJECT >>", new Color(0, 110, 170), Color.WHITE, 28);
        btnStep.setIcon(RetroIcons.createStepIcon(11, Color.WHITE));
        btnStep.setFont(new Font("Consolas", Font.BOLD, 10));

        btnFlush = CPUSchedulingPanel.createArcadeButton("FLUSH MMU", new Color(145, 28, 56), Color.WHITE, 28);
        btnFlush.setIcon(RetroIcons.createResetIcon(11, Color.WHITE));
        btnFlush.setFont(new Font("Consolas", Font.BOLD, 10));

        btnDefrag = CPUSchedulingPanel.createArcadeButton("⚡ DEFRAG COMPACT", new Color(140, 100, 10), Color.WHITE, 28);
        btnDefrag.setFont(new Font("Consolas", Font.BOLD, 10));

        btnClockSpeed = CPUSchedulingPanel.createArcadeButton("1X CLOCK", new Color(48, 34, 78), RetroTheme.ACCENT_YELLOW, 28);
        btnClockSpeed.setFont(new Font("Consolas", Font.BOLD, 10));

        btnManualMode = CPUSchedulingPanel.createArcadeButton("AUTO SLOT", new Color(38, 26, 68), RetroTheme.ACCENT_CYAN, 28);
        btnManualMode.setFont(new Font("Consolas", Font.BOLD, 10));

        btnRun.addActionListener(e -> toggleAutoRun());
        btnStep.addActionListener(e -> executeStepForward());
        btnFlush.addActionListener(e -> resetSimulation());
        btnDefrag.addActionListener(e -> triggerSiliconDefragmentation());
        btnClockSpeed.addActionListener(e -> {
            if (speedFactor == 1) speedFactor = 2;
            else if (speedFactor == 2) speedFactor = 4;
            else speedFactor = 1;
            btnClockSpeed.setText(speedFactor + "X CLOCK");
            mmuClockTimer.setDelay(850 / speedFactor);
        });
        btnManualMode.addActionListener(e -> {
            isManualSocketingMode = !isManualSocketingMode;
            btnManualMode.setText(isManualSocketingMode ? "AUTO SLOT" : "MANUAL SLOT");
            btnManualMode.setForeground(isManualSocketingMode ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_CYAN);
            haltAutoRun();
            motherboardCanvas.setManualMode(isManualSocketingMode);
        });

        leftTransport.add(btnRun);
        leftTransport.add(btnStep);
        leftTransport.add(btnFlush);
        leftTransport.add(btnDefrag);
        leftTransport.add(btnClockSpeed);
        leftTransport.add(btnManualMode);
        row2.add(leftTransport, BorderLayout.WEST);

        // Right Actions (Scenario Presets)
        JPanel rightPresets = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightPresets.setOpaque(false);

        JLabel lBench = new JLabel("SCENARIOS:");
        lBench.setFont(new Font("Consolas", Font.BOLD, 9));
        lBench.setForeground(RetroTheme.ACCENT_CYAN);
        rightPresets.add(lBench);

        rightPresets.add(createMiniPresetBtn("GALVIN 5B", () -> loadBenchmarkGalvin()));
        rightPresets.add(createMiniPresetBtn("KNUTH 50%", () -> loadBenchmarkKnuthTrap()));
        rightPresets.add(createMiniPresetBtn("COALESCE", () -> loadBenchmarkCoalesce()));

        row2.add(rightPresets, BorderLayout.EAST);

        cockpit.add(row1);
        cockpit.add(row2);
        return cockpit;
    }

    private void updatePolicyButtonsUI() {
        if (policyButtons == null) return;
        Strategy[] allStrats = Strategy.values();
        for (int i = 0; i < allStrats.length; i++) {
            Strategy st = allStrats[i];
            boolean isSel = (st == activeStrategy);
            if (isSel) {
                policyButtons[i].setBackground(new Color(0, 165, 195));
                policyButtons[i].setForeground(Color.WHITE);
                policyButtons[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0, 240, 255), 2),
                        BorderFactory.createEmptyBorder(2, 9, 2, 9)
                ));
            } else {
                policyButtons[i].setBackground(new Color(28, 20, 48));
                policyButtons[i].setForeground(RetroTheme.TEXT_DIM);
                policyButtons[i].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(60, 45, 88), 1),
                        BorderFactory.createEmptyBorder(3, 9, 3, 9)
                ));
            }
        }
    }

    private JButton createMiniPresetBtn(String label, Runnable action) {
        JButton b = CPUSchedulingPanel.createArcadeButton(label, new Color(36, 26, 64), Color.WHITE, 26);
        b.setFont(new Font("Consolas", Font.BOLD, 10));
        b.addActionListener(e -> {
            haltAutoRun();
            action.run();
        });
        return b;
    }

    // =========================================================================
    // 2. PHYSICAL MEMORY TRANSLATION TABLE & CONFIG INPUTS
    // =========================================================================
    private JPanel createMemoryTranslationDeck() {
        JPanel deck = new JPanel(new BorderLayout(0, 4));
        deck.setOpaque(false);

        // Config Header Strip (Inputs on Left, Live Telemetry on Right)
        JPanel configStrip = new JPanel(new BorderLayout(8, 0));
        configStrip.setOpaque(true);
        configStrip.setBackground(new Color(22, 16, 42));
        configStrip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 45, 95), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        // Left Inputs
        JPanel leftInputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftInputs.setOpaque(false);

        JLabel lSock = new JLabel("SOCKET SIZES (KB):");
        lSock.setFont(new Font("Consolas", Font.BOLD, 10));
        lSock.setForeground(RetroTheme.ACCENT_CYAN);
        txtSocketsConfig = RetroTheme.createRetroTextField("100 500 200 300 600", 25);
        txtSocketsConfig.setPreferredSize(new Dimension(250, 24));
        txtSocketsConfig.setFont(new Font("Consolas", Font.BOLD, 10));
        txtSocketsConfig.setCaretPosition(0);

        JLabel lChips = new JLabel("CHIP DEMANDS (KB):");
        lChips.setFont(new Font("Consolas", Font.BOLD, 10));
        lChips.setForeground(RetroTheme.ACCENT_YELLOW);
        txtChipsConfig = RetroTheme.createRetroTextField("212 417 112 426", 20);
        txtChipsConfig.setPreferredSize(new Dimension(200, 24));
        txtChipsConfig.setFont(new Font("Consolas", Font.BOLD, 10));
        txtChipsConfig.setCaretPosition(0);

        JButton btnApply = CPUSchedulingPanel.createArcadeButton("FLASH MMU", new Color(0, 140, 60), Color.WHITE, 26);
        btnApply.setFont(new Font("Consolas", Font.BOLD, 10));
        btnApply.addActionListener(e -> parseConfigAndFlash());

        leftInputs.add(lSock);
        leftInputs.add(txtSocketsConfig);
        leftInputs.add(lChips);
        leftInputs.add(txtChipsConfig);
        leftInputs.add(btnApply);
        configStrip.add(leftInputs, BorderLayout.WEST);

        // Right Telemetry Gauges
        JPanel telemetry = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        telemetry.setOpaque(false);

        lblStatTotalRam = new JLabel("TOTAL RAM: 0 KB");
        lblStatTotalRam.setFont(new Font("Consolas", Font.BOLD, 10));
        lblStatTotalRam.setForeground(RetroTheme.TEXT_WHITE);

        lblStatInternalFrag = new JLabel("INTERNAL FRAG: 0 KB");
        lblStatInternalFrag.setFont(new Font("Consolas", Font.BOLD, 10));
        lblStatInternalFrag.setForeground(RetroTheme.ACCENT_PINK);

        lblStatExternalFrag = new JLabel("EXTERNAL FRAG: 0 KB");
        lblStatExternalFrag.setFont(new Font("Consolas", Font.BOLD, 10));
        lblStatExternalFrag.setForeground(RetroTheme.ACCENT_YELLOW);

        lblStatUtilization = new JLabel("UTILIZATION: 0%");
        lblStatUtilization.setFont(new Font("Consolas", Font.BOLD, 10));
        lblStatUtilization.setForeground(RetroTheme.ACCENT_GREEN);

        telemetry.add(lblStatTotalRam);
        telemetry.add(lblStatInternalFrag);
        telemetry.add(lblStatExternalFrag);
        telemetry.add(lblStatUtilization);
        configStrip.add(telemetry, BorderLayout.EAST);

        deck.add(configStrip, BorderLayout.NORTH);

        // Translation Matrix Table
        String[] columns = {
                "IC / CHIP", "DEMAND SIZE", "BASE ADDRESS", "LIMIT / CEILING", "SOCKET ASSIGNED", "INTERNAL FRAG (HEAT)", "MMU ALLOCATION RATIONALE"
        };
        translationTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        translationTable = new JTable(translationTableModel);
        translationTable.setFont(new Font("Consolas", Font.BOLD, 11));
        translationTable.setRowHeight(24);
        translationTable.setBackground(new Color(12, 18, 24));
        translationTable.setForeground(Color.WHITE);
        translationTable.setSelectionBackground(new Color(30, 55, 45));
        translationTable.setSelectionForeground(RetroTheme.ACCENT_CYAN);
        translationTable.setGridColor(new Color(30, 48, 40));
        translationTable.getTableHeader().setFont(new Font("Consolas", Font.BOLD, 10));
        translationTable.getTableHeader().setBackground(new Color(18, 30, 38));
        translationTable.getTableHeader().setForeground(RetroTheme.ACCENT_CYAN);
        translationTable.getTableHeader().setPreferredSize(new Dimension(0, 26));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return c;
            }
        };
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < translationTable.getColumnCount(); i++) {
            translationTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        translationTable.getColumnModel().getColumn(0).setPreferredWidth(85);
        translationTable.getColumnModel().getColumn(1).setPreferredWidth(105);
        translationTable.getColumnModel().getColumn(2).setPreferredWidth(105);
        translationTable.getColumnModel().getColumn(3).setPreferredWidth(105);
        translationTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        translationTable.getColumnModel().getColumn(5).setPreferredWidth(140);
        translationTable.getColumnModel().getColumn(6).setPreferredWidth(340);

        JScrollPane scroll = RetroTheme.createRetroScroll(translationTable);
        scroll.getViewport().setBackground(new Color(12, 18, 24));
        scroll.setCorner(JScrollPane.LOWER_RIGHT_CORNER, new JPanel() {{ setBackground(new Color(12, 18, 24)); }});
        deck.add(scroll, BorderLayout.CENTER);

        return deck;
    }

    // =========================================================================
    // 3. COMPUTATIONAL ALLOCATION ENGINE & MATHEMATICAL MATRIX
    // =========================================================================
    private void recomputeSiliconState() {
        partitions.clear();
        int baseAddr = 0;
        totalRamCapacityKb = 0;
        for (int i = 0; i < initialSocketSizes.size(); i++) {
            int cap = initialSocketSizes.get(i);
            partitions.add(new RamPartition(i + 1, cap, baseAddr));
            baseAddr += cap;
            totalRamCapacityKb += cap;
        }

        chips.clear();
        for (int i = 0; i < initialChipDemands.size(); i++) {
            chips.add(new ProcessChip(
                    "P" + (i + 1),
                    initialChipDemands.get(i),
                    CHIP_NEON_COLORS[i % CHIP_NEON_COLORS.length]
            ));
        }

        eventLog.clear();
        int lastAllocatedIdx = -1;
        boolean[] socketOccupied = new boolean[partitions.size()];

        for (int cIdx = 0; cIdx < chips.size(); cIdx++) {
            ProcessChip chip = chips.get(cIdx);
            int chosenSocket = -1;
            String rationale = "";

            if (activeStrategy == Strategy.FIRST_FIT) {
                for (int s = 0; s < partitions.size(); s++) {
                    if (!socketOccupied[s] && partitions.get(s).capacityKb >= chip.demandKb) {
                        chosenSocket = s;
                        rationale = String.format("First partition with capacity (%dK >= %dK)", partitions.get(s).capacityKb, chip.demandKb);
                        break;
                    }
                }
            } else if (activeStrategy == Strategy.BEST_FIT) {
                int minFrag = Integer.MAX_VALUE;
                for (int s = 0; s < partitions.size(); s++) {
                    if (!socketOccupied[s] && partitions.get(s).capacityKb >= chip.demandKb) {
                        int diff = partitions.get(s).capacityKb - chip.demandKb;
                        if (diff < minFrag) {
                            minFrag = diff;
                            chosenSocket = s;
                        }
                    }
                }
                if (chosenSocket >= 0) {
                    rationale = String.format("Optimal socket (%dK) leaving minimum heat frag (%dK)",
                            partitions.get(chosenSocket).capacityKb, minFrag);
                }
            } else { // NEXT FIT
                int start = (lastAllocatedIdx + 1) % partitions.size();
                for (int offset = 0; offset < partitions.size(); offset++) {
                    int s = (start + offset) % partitions.size();
                    if (!socketOccupied[s] && partitions.get(s).capacityKb >= chip.demandKb) {
                        chosenSocket = s;
                        rationale = String.format("Next fit search resuming from Socket %d", (start + 1));
                        break;
                    }
                }
            }

            if (chosenSocket >= 0) {
                socketOccupied[chosenSocket] = true;
                lastAllocatedIdx = chosenSocket;
                int frag = partitions.get(chosenSocket).capacityKb - chip.demandKb;
                eventLog.add(new AllocationEvent(
                        cIdx, chosenSocket, chip.pid, chip.demandKb,
                        partitions.get(chosenSocket).capacityKb, frag, rationale
                ));
            } else {
                eventLog.add(new AllocationEvent(
                        cIdx, -1, chip.pid, chip.demandKb, 0, 0,
                        "REJECTED: No vacant socket holds adequate contiguous memory!"
                ));
            }
        }

        updateTelemetryAndViews();
    }

    private void updateTelemetryAndViews() {
        for (RamPartition p : partitions) {
            p.slottedChip = null;
            p.internalFragKb = 0;
        }
        for (ProcessChip c : chips) {
            c.assignedSocketId = -1;
            c.internalFragKb = 0;
            c.status = "READY";
            c.searchReason = "-";
        }

        totalInternalFragKb = 0;
        totalAllocatedKb = 0;

        for (int i = 0; i < currentStepIndex && i < eventLog.size(); i++) {
            AllocationEvent ev = eventLog.get(i);
            ProcessChip chip = chips.get(ev.chipIndex);

            if (ev.socketIndex >= 0) {
                RamPartition part = partitions.get(ev.socketIndex);
                part.slottedChip = chip;
                part.internalFragKb = ev.internalFragKb;
                chip.assignedSocketId = part.id;
                chip.internalFragKb = ev.internalFragKb;
                chip.status = "SLOTTED (SOCKET " + part.id + ")";
                chip.searchReason = ev.rationale;

                totalInternalFragKb += ev.internalFragKb;
                totalAllocatedKb += chip.demandKb;
            } else {
                chip.status = "REJECTED / UNALLOCATED";
                chip.searchReason = ev.rationale;
            }
        }

        // External Fragmentation Analysis
        totalExternalFragKb = 0;
        isExternalFragHazard = false;
        int totalVacantMemory = 0;
        for (RamPartition p : partitions) {
            if (p.isFree()) totalVacantMemory += p.capacityKb;
        }

        if (currentStepIndex < chips.size()) {
            ProcessChip waitingChip = chips.get(currentStepIndex);
            if (waitingChip.assignedSocketId == -1 && totalVacantMemory >= waitingChip.demandKb) {
                boolean canFitAny = false;
                for (RamPartition p : partitions) {
                    if (p.isFree() && p.capacityKb >= waitingChip.demandKb) { canFitAny = true; break; }
                }
                if (!canFitAny) {
                    isExternalFragHazard = true;
                    totalExternalFragKb = totalVacantMemory;
                }
            }
        }

        // Translation Table Refresh
        translationTableModel.setRowCount(0);
        for (int cIdx = 0; cIdx < chips.size(); cIdx++) {
            ProcessChip c = chips.get(cIdx);
            String baseStr, limitStr, sockStr, fragStr, reasonStr;

            if (cIdx < currentStepIndex) {
                if (c.assignedSocketId >= 0) {
                    RamPartition p = partitions.get(c.assignedSocketId - 1);
                    baseStr = String.format("0x%05X (%dK)", p.startAddr, p.startAddr);
                    limitStr = String.format("%d KB", c.demandKb);
                    sockStr = "Socket " + p.id + " (" + p.capacityKb + "K)";
                    fragStr = c.internalFragKb + " KB";
                    reasonStr = "✔ " + c.searchReason;
                } else {
                    baseStr = "UNMAPPED";
                    limitStr = c.demandKb + " KB";
                    sockStr = "NONE (FAULT)";
                    fragStr = "-";
                    reasonStr = "❌ " + c.searchReason;
                }
            } else if (cIdx == currentStepIndex) {
                baseStr = "PROBING...";
                limitStr = c.demandKb + " KB";
                sockStr = "SCANNING MMU...";
                fragStr = "-";
                reasonStr = "⏳ NEXT IC CHIP SCHEDULED FOR INJECTION";
            } else {
                baseStr = "-";
                limitStr = c.demandKb + " KB";
                sockStr = "-";
                fragStr = "-";
                reasonStr = "QUEUED ON CONVEYOR";
            }

            translationTableModel.addRow(new Object[]{
                    c.pid, c.demandKb + " KB", baseStr, limitStr, sockStr, fragStr, reasonStr
            });
        }

        // Live Telemetry Label Updates
        lblStatTotalRam.setText(String.format("TOTAL RAM: %d KB", totalRamCapacityKb));
        lblStatInternalFrag.setText(String.format("INTERNAL FRAG: %d KB", totalInternalFragKb));
        lblStatExternalFrag.setText(String.format("EXTERNAL FRAG: %d KB", isExternalFragHazard ? totalExternalFragKb : totalVacantMemory));

        int utilPct = totalRamCapacityKb == 0 ? 0 : (totalAllocatedKb * 100 / totalRamCapacityKb);
        lblStatUtilization.setText(String.format("UTILIZATION: %d%% (%d/%d KB)", utilPct, totalAllocatedKb, totalRamCapacityKb));

        // BIOS Status Pill (Clean glowing retro text, no box border)
        if (currentStepIndex == 0) {
            lblMmuStatus.setText(" [ MMU SOLVENT // READY TO INJECT ] ");
            lblMmuStatus.setForeground(RetroTheme.ACCENT_CYAN);
        } else if (isExternalFragHazard) {
            lblMmuStatus.setText(" [ ⚠️ EXTERNAL FRAG HAZARD // RUN DEFRAG ] ");
            lblMmuStatus.setForeground(RetroTheme.ACCENT_PINK);
        } else if (currentStepIndex >= chips.size()) {
            lblMmuStatus.setText(" [ ALLOCATION SEQUENCE COMPLETE ✔ ] ");
            lblMmuStatus.setForeground(RetroTheme.ACCENT_GREEN);
        } else {
            lblMmuStatus.setText(String.format(" [ INJECTING STEP %d/%d // %s ] ",
                    currentStepIndex, chips.size(), chips.get(currentStepIndex).pid));
            lblMmuStatus.setForeground(RetroTheme.ACCENT_YELLOW);
        }

        motherboardCanvas.repaint();
    }

    // =========================================================================
    // 4. SIMULATION ACTIONS & DEFRAGMENTATION COMPACTION
    // =========================================================================
    private void toggleAutoRun() {
        if (isAutoRunning) {
            haltAutoRun();
        } else {
            if (currentStepIndex >= chips.size()) {
                currentStepIndex = 0;
            }
            isAutoRunning = true;
            btnRun.setText("PAUSE");
            btnRun.setIcon(RetroIcons.createPauseIcon(11, RetroTheme.ACCENT_YELLOW));
            btnRun.setForeground(RetroTheme.ACCENT_YELLOW);
            mmuClockTimer.start();
        }
    }

    private void haltAutoRun() {
        isAutoRunning = false;
        btnRun.setText("AUTO ALLOC");
        btnRun.setIcon(RetroIcons.createPlayIcon(11, Color.WHITE));
        btnRun.setForeground(Color.WHITE);
        mmuClockTimer.stop();
    }

    public void executeStepForward() {
        if (currentStepIndex < chips.size()) {
            currentStepIndex++;
            updateTelemetryAndViews();
            if (currentStepIndex >= chips.size()) {
                haltAutoRun();
            }
        }
    }

    private void resetSimulation() {
        haltAutoRun();
        currentStepIndex = 0;
        recomputeSiliconState();
    }

    public void triggerSiliconDefragmentation() {
        haltAutoRun();

        List<Integer> activeChips = new ArrayList<>();
        int reclaimedFrag = 0;
        for (RamPartition p : partitions) {
            if (!p.isFree()) {
                activeChips.add(p.slottedChip.demandKb);
                reclaimedFrag += p.internalFragKb;
            }
        }

        if (activeChips.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "⚡ MMU BUS REPORT: No active process memory allocations to compact!\n" +
                    "Allocate processes first using [▶ AUTO ALLOC] or [⏩ STEP INJECT].",
                    "MMU Compaction", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int totalUsed = 0;
        for (int sz : activeChips) totalUsed += sz;
        int contiguousFree = totalRamCapacityKb - totalUsed;

        initialSocketSizes.clear();
        for (int sz : activeChips) initialSocketSizes.add(sz);
        if (contiguousFree > 0) initialSocketSizes.add(contiguousFree);

        if (txtSocketsConfig != null) {
            StringBuilder sb = new StringBuilder();
            for (int s : initialSocketSizes) sb.append(s).append(" ");
            txtSocketsConfig.setText(sb.toString().trim());
            txtSocketsConfig.setCaretPosition(0);
        }

        motherboardCanvas.triggerLaserDefragAnimation();
        recomputeSiliconState();
        currentStepIndex = activeChips.size();
        updateTelemetryAndViews();

        JOptionPane.showMessageDialog(this,
                String.format("⚡ SILICON MMU COMPACTION COMPLETED!\n" +
                                "• All socketed IC chips slid to low memory (Address 0x00000).\n" +
                                "• Reclaimed %d KB of thermal internal fragmentation.\n" +
                                "• Consolidated %d KB contiguous free hole into a master socket!",
                        reclaimedFrag, contiguousFree),
                "Hardware Defragmentation", JOptionPane.INFORMATION_MESSAGE);
    }

    private void parseConfigAndFlash() {
        if (txtSocketsConfig == null || txtChipsConfig == null) return;
        try {
            String[] sTok = txtSocketsConfig.getText().trim().split("\\s+");
            List<Integer> sList = new ArrayList<>();
            for (String s : sTok) {
                int v = Integer.parseInt(s.trim());
                if (v > 0) sList.add(v);
            }

            String[] cTok = txtChipsConfig.getText().trim().split("\\s+");
            List<Integer> cList = new ArrayList<>();
            for (String s : cTok) {
                int v = Integer.parseInt(s.trim());
                if (v > 0) cList.add(v);
            }

            if (sList.isEmpty() || cList.isEmpty()) throw new Exception();

            initialSocketSizes.clear();
            initialSocketSizes.addAll(sList);

            initialChipDemands.clear();
            initialChipDemands.addAll(cList);

            resetSimulation();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid MMU parameters! Enter positive integer values separated by spaces.",
                    "Bus Configuration Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Benchmark Presets
    private void loadBenchmarkGalvin() {
        if (txtSocketsConfig != null) {
            txtSocketsConfig.setText("100 500 200 300 600");
            txtSocketsConfig.setCaretPosition(0);
        }
        if (txtChipsConfig != null) {
            txtChipsConfig.setText("212 417 112 426");
            txtChipsConfig.setCaretPosition(0);
        }
        activeStrategy = Strategy.BEST_FIT;
        updatePolicyButtonsUI();
        parseConfigAndFlash();
    }

    private void loadBenchmarkKnuthTrap() {
        if (txtSocketsConfig != null) {
            txtSocketsConfig.setText("120 480 180 260 550 140");
            txtSocketsConfig.setCaretPosition(0);
        }
        if (txtChipsConfig != null) {
            txtChipsConfig.setText("110 420 170 250 500 130 90");
            txtChipsConfig.setCaretPosition(0);
        }
        activeStrategy = Strategy.FIRST_FIT;
        updatePolicyButtonsUI();
        parseConfigAndFlash();
    }

    private void loadBenchmarkCoalesce() {
        if (txtSocketsConfig != null) {
            txtSocketsConfig.setText("100 300 150 250 400");
            txtSocketsConfig.setCaretPosition(0);
        }
        if (txtChipsConfig != null) {
            txtChipsConfig.setText("280 220 140 380");
            txtChipsConfig.setCaretPosition(0);
        }
        activeStrategy = Strategy.BEST_FIT;
        updatePolicyButtonsUI();
        parseConfigAndFlash();
    }

    // =========================================================================
    // 5. SILICON MOTHERBOARD & RAM SOCKET RACK CANVAS
    // =========================================================================
    public static class SiliconMotherboardCanvas extends JPanel {
        private final FragmentationPanel parentPanel;
        private boolean isManualMode = false;
        private int pulseTick = 0;
        private boolean defragLaserFired = false;
        private float laserX = 0;

        // Visual Heat Particles
        public static class HeatParticle {
            float x, y, vy;
            Color color;
            int life, maxLife;
            public HeatParticle(float x, float y, float vy, Color c, int l) {
                this.x = x; this.y = y; this.vy = vy; this.color = c; this.life = l; this.maxLife = l;
            }
        }
        private final List<HeatParticle> heatParticles = new ArrayList<>();

        public SiliconMotherboardCanvas(FragmentationPanel parentPanel) {
            this.parentPanel = parentPanel;
            setBackground(new Color(10, 16, 22));
            setOpaque(true);
            setBorder(BorderFactory.createLineBorder(new Color(35, 60, 50), 1));

            new javax.swing.Timer(40, e -> {
                pulseTick++;

                // Defrag Laser
                if (defragLaserFired) {
                    laserX += 36;
                    if (laserX > getWidth()) defragLaserFired = false;
                }

                // Heat Particle Physics
                for (int i = heatParticles.size() - 1; i >= 0; i--) {
                    HeatParticle p = heatParticles.get(i);
                    p.y += p.vy;
                    p.x += (float)(Math.sin(pulseTick * 0.4 + i) * 0.8);
                    p.life--;
                    if (p.life <= 0) heatParticles.remove(i);
                }

                repaint();
            }).start();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (isManualMode && parentPanel.currentStepIndex < parentPanel.chips.size()) {
                        parentPanel.executeStepForward();
                    }
                }
            });
        }

        public void setManualMode(boolean active) {
            this.isManualMode = active;
            repaint();
        }

        public void triggerLaserDefragAnimation() {
            defragLaserFired = true;
            laserX = 0;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Silicon Motherboard PCB & Golden Traces
            drawPcbMotherboardGrid(g2, w, h);

            // 2. Top Telemetry Banner & Marquee
            drawTelemetryMarquee(g2, w);

            // 3. Physical Memory Partition Sockets (DIMM Modules)
            drawSiliconSocketsRack(g2, w);

            // 4. Process Chip Conveyor Bay (DIP Microchip Cartridges)
            drawChipCartridgeBay(g2, w);

            // 5. Heat Particles
            drawHeatParticles(g2);

            // 6. Laser Defrag Sweeper
            if (defragLaserFired) {
                g2.setColor(new Color(5, 217, 232, 230));
                g2.setStroke(new BasicStroke(4.0f));
                g2.drawLine((int) laserX, 10, (int) laserX, h - 25);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine((int) laserX, 10, (int) laserX, h - 25);
                g2.setStroke(new BasicStroke(1.0f));
            }

            // 7. Silicon MMU Bus Log (Bottom Ticker Tape with Guarantee Margin)
            drawSiliconBusTicker(g2, w, h);
        }

        private void drawPcbMotherboardGrid(Graphics2D g2, int w, int h) {
            g2.setColor(new Color(10, 16, 22));
            g2.fillRect(0, 0, w, h);

            // Motherboard Traces & Circuit Vias
            g2.setColor(new Color(22, 38, 32));
            for (int gx = 0; gx < w; gx += 36) {
                g2.drawLine(gx, 0, gx, h);
                for (int gy = 18; gy < h; gy += 36) {
                    g2.fillOval(gx - 2, gy - 2, 4, 4);
                }
            }
            for (int gy = 0; gy < h; gy += 36) {
                g2.drawLine(0, gy, w, gy);
            }

            // Gold Bus Power Rails
            g2.setColor(new Color(190, 145, 45, 90));
            g2.drawLine(16, 12, w - 16, 12);
            g2.drawLine(16, 14, w - 16, 14);
            g2.drawLine(16, h - 30, w - 16, h - 30);
        }

        private void drawTelemetryMarquee(Graphics2D g2, int w) {
            // Marquee Bar at top
            g2.setColor(new Color(14, 24, 32, 220));
            g2.fillRoundRect(16, 6, w - 32, 26, 6, 6);
            g2.setColor(new Color(40, 75, 65));
            g2.drawRoundRect(16, 6, w - 32, 26, 6, 6);

            // Blinking Green Power LED
            boolean ledOn = (pulseTick % 20 < 14);
            g2.setColor(ledOn ? RetroTheme.ACCENT_GREEN : new Color(20, 60, 40));
            g2.fillOval(26, 15, 8, 8);
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawOval(26, 15, 8, 8);

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.drawString("CYBER RAM MOTHERBOARD v2.6 // DUAL-CHANNEL CONTIGUOUS MATRIX", 40, 21);

            // Segmented Battery / Utilization Meter
            int utilPct = parentPanel.totalRamCapacityKb == 0 ? 0 :
                    (parentPanel.totalAllocatedKb * 100 / parentPanel.totalRamCapacityKb);
            int barW = 100;
            int barX = w - 240;
            int barY = 13;

            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString(String.format("UTIL: %d%%", utilPct), barX - 60, 21);

            g2.setColor(new Color(25, 40, 48));
            g2.fillRoundRect(barX, barY, barW, 11, 3, 3);
            int fillW = (int)(barW * (utilPct / 100.0f));
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.fillRoundRect(barX, barY, fillW, 11, 3, 3);
            g2.setColor(new Color(50, 85, 75));
            g2.drawRoundRect(barX, barY, barW, 11, 3, 3);

            // Waste Telemetry Tag
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.drawString(String.format("WASTE: %d KB", parentPanel.totalInternalFragKb), w - 125, 21);
        }

        private void drawSiliconSocketsRack(Graphics2D g2, int w) {
            int rackX = 16;
            int rackY = 38;
            int rackW = w - 32;
            int rackH = 136;

            // Motherboard Socket Bank Outer Frame
            g2.setColor(new Color(16, 25, 34, 240));
            g2.fillRoundRect(rackX, rackY, rackW, rackH, 8, 8);
            g2.setColor(new Color(45, 80, 70));
            g2.drawRoundRect(rackX, rackY, rackW, rackH, 8, 8);

            // External Frag Circuit Warning Hologram Banner
            if (parentPanel.isExternalFragHazard) {
                boolean flash = (pulseTick % 24 < 16);
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(flash ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_YELLOW);
                String alert = "⚠️ EXTERNAL FRAG LOCKOUT: " + parentPanel.totalExternalFragKb + "K FREE SCATTERED // RUN [⚡ DEFRAG COMPACT]";
                int aw = g2.getFontMetrics().stringWidth(alert);
                g2.drawString(alert, rackX + (rackW - aw) / 2, rackY + 16);
            }

            int innerX = rackX + 10;
            int innerY = rackY + 22;
            int innerW = rackW - 20;
            int innerH = rackH - 30;

            int totalRam = Math.max(1, parentPanel.totalRamCapacityKb);
            int curX = innerX;

            for (int i = 0; i < parentPanel.partitions.size(); i++) {
                RamPartition part = parentPanel.partitions.get(i);
                int partW = (int)((long) part.capacityKb * innerW / totalRam);
                partW = Math.max(75, partW); // Readable minimum width

                // Memory Stick Body
                boolean isOccupied = !part.isFree();
                Color stickBg = isOccupied ? new Color(18, 30, 38) : new Color(14, 22, 28);
                Color borderC = isOccupied ? new Color(60, 110, 90) : new Color(35, 60, 50);

                g2.setColor(stickBg);
                g2.fillRoundRect(curX, innerY, partW, innerH, 6, 6);
                g2.setColor(borderC);
                g2.drawRoundRect(curX, innerY, partW, innerH, 6, 6);

                // LED Activity Indicator Dots on Top of DIMM Stick
                Color[] ledCols = {RetroTheme.ACCENT_GREEN, RetroTheme.ACCENT_YELLOW, RetroTheme.ACCENT_CYAN, RetroTheme.ACCENT_PINK};
                for (int l = 0; l < 4; l++) {
                    boolean lit = isOccupied ? ((pulseTick + l * 5) % 20 < 12) : (l == 2);
                    g2.setColor(lit ? ledCols[l] : new Color(25, 45, 38));
                    g2.fillOval(curX + 6 + l * 8, innerY + 4, 5, 5);
                }

                // Golden Connector Pins at base of DIMM stick
                g2.setColor(new Color(235, 185, 50));
                for (int pinX = curX + 4; pinX < curX + partW - 4; pinX += 5) {
                    g2.drawLine(pinX, innerY + innerH - 3, pinX, innerY + innerH + 1);
                }

                // Address Marker Tag
                g2.setFont(new Font("Consolas", Font.PLAIN, 9));
                g2.setColor(new Color(140, 175, 160));
                g2.drawString(part.startAddr + "K", curX + 4, innerY + 19);

                // Slot contents
                int contentY = innerY + 23;
                int contentH = innerH - 36;

                if (part.isFree()) {
                    // Vacant Slot Hologram
                    g2.setColor(new Color(16, 26, 34));
                    g2.fillRect(curX + 4, contentY, partW - 8, contentH);
                    g2.setColor(new Color(28, 48, 58));
                    g2.drawRect(curX + 4, contentY, partW - 8, contentH);

                    g2.setFont(new Font("Consolas", Font.BOLD, 10));
                    g2.setColor(new Color(90, 140, 125));
                    g2.drawString("VACANT", curX + (partW - 38) / 2, contentY + contentH / 2 - 2);
                    g2.drawString(part.capacityKb + " KB", curX + (partW - 44) / 2, contentY + contentH / 2 + 12);
                } else {
                    // Socketed Process Chip + Heat-Sink Frag Chamber
                    ProcessChip chip = part.slottedChip;
                    float allocFraction = (float) chip.demandKb / part.capacityKb;
                    int chipW = (int)((partW - 8) * allocFraction);
                    int heatFragW = (partW - 8) - chipW;

                    // 1. Silicon IC Chip
                    g2.setColor(chip.chipColor.darker().darker());
                    g2.fillRect(curX + 4, contentY, chipW, contentH);
                    g2.setColor(chip.chipColor);
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRect(curX + 4, contentY, chipW, contentH);
                    g2.setStroke(new BasicStroke(1.0f));

                    // Cute Retro Process Avatar bobbing
                    float bob = (float)(Math.sin(pulseTick * 0.3 + i) * 2);
                    BufferedImage avatar = PixelSprites.getJobAvatar(i, 24);
                    if (avatar != null && chipW > 28) {
                        g2.drawImage(avatar, curX + 8, (int)(contentY + 4 + bob), null);
                    }

                    // Chip Label
                    g2.setFont(new Font("Consolas", Font.BOLD, 11));
                    g2.setColor(Color.WHITE);
                    g2.drawString(chip.pid, curX + 36, contentY + 16);

                    g2.setFont(new Font("Consolas", Font.BOLD, 9));
                    g2.setColor(RetroTheme.ACCENT_CYAN);
                    g2.drawString(chip.demandKb + " KB", curX + 36, contentY + 28);

                    // 2. Animated Thermal Heat-Sink Frag Chamber
                    if (heatFragW > 6) {
                        int fragX = curX + 4 + chipW;
                        g2.setColor(new Color(48, 18, 26));
                        g2.fillRect(fragX, contentY, heatFragW, contentH);

                        // Hazard Stripes (Clipped cleanly to fragment chamber)
                        Shape oldClip = g2.getClip();
                        g2.clipRect(fragX, contentY, heatFragW, contentH);
                        g2.setColor(new Color(255, 60, 130, 120));
                        for (int sx = fragX - contentH - 8; sx < fragX + heatFragW + 8; sx += 8) {
                            g2.drawLine(sx, contentY + contentH, sx + 8, contentY);
                        }
                        g2.setClip(oldClip);

                        if (heatFragW > 30) {
                            g2.setFont(new Font("Consolas", Font.BOLD, 8));
                            g2.setColor(RetroTheme.ACCENT_PINK);
                            g2.drawString("⚠️ HEAT", fragX + 4, contentY + 14);
                            g2.drawString(part.internalFragKb + "K", fragX + 4, contentY + 26);
                        }

                        // Spawn Rising Heat Particle
                        if (pulseTick % 6 == 0) {
                            heatParticles.add(new HeatParticle(
                                    fragX + (float)(Math.random() * heatFragW), contentY + contentH,
                                    -(float)(Math.random() * 1.5 + 0.8), RetroTheme.ACCENT_PINK, 18
                            ));
                        }
                    }
                }

                // Socket ID & Capacity Banner at base of stick
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(new Color(180, 220, 205));
                g2.drawString("SOCK " + part.id + " [" + part.capacityKb + "K]", curX + 4, innerY + innerH - 5);

                curX += partW;
            }
        }

        private void drawChipCartridgeBay(Graphics2D g2, int w) {
            int bayY = 186;
            int cardH = 80;

            int count = parentPanel.chips.size();
            if (count == 0) return;

            int cardW = 145;
            int gap = 12;
            int totalW = count * cardW + (count - 1) * gap;
            int startX = Math.max(20, (w - totalW) / 2);

            for (int i = 0; i < count; i++) {
                ProcessChip chip = parentPanel.chips.get(i);
                int cx = startX + i * (cardW + gap);

                boolean isCurrent = (i == parentPanel.currentStepIndex);
                boolean isSlotted = (i < parentPanel.currentStepIndex && chip.assignedSocketId >= 0);
                boolean isFault = (i < parentPanel.currentStepIndex && chip.assignedSocketId == -1);

                // Cartridge Body
                Color bodyBg = isSlotted ? new Color(16, 34, 26) : (isFault ? new Color(38, 16, 24) :
                        (isCurrent ? new Color(42, 28, 55) : new Color(18, 26, 34)));
                Color pinBorder = isSlotted ? RetroTheme.ACCENT_GREEN : (isFault ? RetroTheme.ACCENT_PINK :
                        (isCurrent ? RetroTheme.ACCENT_YELLOW : new Color(50, 85, 75)));

                g2.setColor(bodyBg);
                g2.fillRoundRect(cx, bayY, cardW, cardH, 6, 6);
                g2.setColor(pinBorder);
                g2.setStroke(new BasicStroke(isCurrent ? 2.0f : 1.2f));
                g2.drawRoundRect(cx, bayY, cardW, cardH, 6, 6);
                g2.setStroke(new BasicStroke(1.0f));

                // DIP Metallic Pins along left and right
                g2.setColor(new Color(220, 220, 240));
                for (int py = bayY + 10; py < bayY + cardH - 10; py += 12) {
                    g2.fillRect(cx - 3, py, 4, 5);
                    g2.fillRect(cx + cardW - 1, py, 4, 5);
                }

                // Process Avatar
                BufferedImage avatar = PixelSprites.getJobAvatar(i, 24);
                if (avatar != null) {
                    g2.drawImage(avatar, cx + 8, bayY + 8, null);
                }

                // Chip ID & Demand
                g2.setFont(new Font("Consolas", Font.BOLD, 11));
                g2.setColor(Color.WHITE);
                g2.drawString("CHIP " + chip.pid, cx + 38, bayY + 19);

                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.drawString(chip.demandKb + " KB DEMAND", cx + 38, bayY + 31);

                // Divider
                g2.setColor(new Color(45, 70, 65));
                g2.drawLine(cx + 6, bayY + 39, cx + cardW - 6, bayY + 39);

                // Status details
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                if (isSlotted) {
                    g2.setColor(RetroTheme.ACCENT_GREEN);
                    g2.drawString("✔ IN SOCKET " + chip.assignedSocketId, cx + 8, bayY + 54);
                    g2.drawString("HEAT: " + chip.internalFragKb + " KB", cx + 8, bayY + 68);
                } else if (isFault) {
                    g2.setColor(RetroTheme.ACCENT_PINK);
                    g2.drawString("❌ BLOCKED: EXT FRAG", cx + 8, bayY + 54);
                    g2.drawString("PARKED IN WAIT QUEUE", cx + 8, bayY + 68);
                } else if (isCurrent) {
                    g2.setColor(RetroTheme.ACCENT_YELLOW);
                    g2.drawString("⚡ INJECTION PENDING", cx + 8, bayY + 54);
                    g2.drawString("CLICK [⏩ STEP INJECT]", cx + 8, bayY + 68);
                } else {
                    g2.setColor(new Color(130, 165, 155));
                    g2.drawString("QUEUED ON BUS", cx + 8, bayY + 60);
                }
            }
        }

        private void drawHeatParticles(Graphics2D g2) {
            for (HeatParticle p : heatParticles) {
                float alpha = (float) p.life / p.maxLife;
                g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), (int)(alpha * 240)));
                g2.fillRect((int) p.x, (int) p.y, 2, 3);
            }
        }

        private void drawSiliconBusTicker(Graphics2D g2, int w, int h) {
            // Clean Bottom Banner Panel (Guaranteed zero collision)
            g2.setColor(new Color(12, 18, 26, 240));
            g2.fillRect(16, h - 26, w - 32, 22);
            g2.setColor(new Color(40, 70, 60));
            g2.drawRect(16, h - 26, w - 32, 22);

            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString("MMU BUS LOG:", 24, h - 11);

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(Color.WHITE);

            String msg;
            if (parentPanel.currentStepIndex == 0) {
                msg = "MMU bus idle. Click [▶ AUTO ALLOC] or [⏩ STEP INJECT] to execute contiguous silicon partition trace.";
            } else if (parentPanel.currentStepIndex <= parentPanel.eventLog.size()) {
                AllocationEvent last = parentPanel.eventLog.get(parentPanel.currentStepIndex - 1);
                if (last.socketIndex >= 0) {
                    msg = String.format("Cycle %d: Chip %s (%d KB) slotted into Socket %d (%d KB). Internal heat frag: %d KB (%s)",
                            parentPanel.currentStepIndex, last.pid, last.demandKb, (last.socketIndex + 1),
                            last.socketCapacityKb, last.internalFragKb, last.rationale);
                } else {
                    msg = String.format("Cycle %d: Chip %s (%d KB) REJECTED: Insufficient contiguous capacity in any vacant socket!",
                            parentPanel.currentStepIndex, last.pid, last.demandKb);
                }
            } else {
                msg = "All process memory allocations completed. Bus clear.";
            }

            int maxW = w - 190;
            FontMetrics fm = g2.getFontMetrics();
            if (fm.stringWidth(msg) > maxW) {
                while (msg.length() > 3 && fm.stringWidth(msg + "...") > maxW) {
                    msg = msg.substring(0, msg.length() - 1);
                }
                msg += "...";
            }
            g2.drawString(msg, 135, h - 11);
        }
    }
}
