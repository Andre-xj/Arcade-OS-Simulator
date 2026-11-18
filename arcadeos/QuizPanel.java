package arcadeos;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Screen: QUIZ MODE // INTERACTIVE RETRO ARCADE QUIZ ARENA
 * -----------------------------------------------------------------------------
 * 100% Authentic Retro Arcade Flow:
 * 1. Screen 1: STAGE SELECT CARTRIDGE HUB (8 Rock-Solid Static Arcade Cabinets, Lore, Difficulty, Timer Mode)
 * 2. Screen 2: ARCADE QUIZ COMBAT BATTLEFIELD:
 *    - Left: Mission Intel Dossier, Live Problem Matrix Table, Visual Schematic Pipeline & Minigame Launcher
 *    - Right: Interactive Arcade MCQ Combat Arena with 4 Tactile Option Buttons, Instant Sound/Glow Feedback,
 *             Heart Lives, Score Streak Counter, Step-by-Step Educational Explanations & S-Rank Victory Screen.
 */
public class QuizPanel extends JPanel {
    private final ArcadeOS parentFrame;

    // CardLayout switching between STAGE SELECT and BATTLE ARENA
    private final CardLayout rootCardLayout = new CardLayout();
    private final JPanel rootCardContainer = new JPanel(rootCardLayout);
    public static final String VIEW_STAGE_SELECT = "VIEW_STAGE_SELECT";
    public static final String VIEW_WORKSHEET    = "VIEW_WORKSHEET";
    private String activeView = VIEW_STAGE_SELECT;

    private int currentModuleIdx = 0; // 0 to 7
    private int currentQuestionIdx = 0; // 0 to 3
    private boolean questionAnswered = false;
    private int selectedOptionIdx = -1;
    private int totalCorrectInMission = 0;

    // Interactive Background Micro-Dots & Constellation System (identical to LoginPanel)
    private static class BgDotParticle {
        float x, y;
        float vx, vy;
        float baseRadius;
        float currentRadius;
        float alpha;
        float pulsePhase;
        Color color;

        BgDotParticle(float x, float y, float vx, float vy, float r, float a, float phase, Color c) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.baseRadius = r;
            this.currentRadius = r;
            this.alpha = a;
            this.pulsePhase = phase;
            this.color = c;
        }
    }

    private final List<BgDotParticle> bgDots = new ArrayList<>();
    private static final Color[] DOT_COLORS = {
            new Color(255, 42, 141),   // Hot Pink
            new Color(5, 217, 232),    // Electric Cyan
            new Color(255, 211, 25),   // Radiant Yellow
            new Color(180, 0, 255),    // Neon Purple
            new Color(57, 255, 20),    // Cyber Green
    };

    int mouseX = -999;
    int mouseY = -999;
    float smoothMouseX = -999f;
    float smoothMouseY = -999f;
    private javax.swing.Timer bgParticleTimer;
    private float animTick = 0f;
    private AWTEventListener globalMouseListener;

    // Thrill & Game Rules State
    private boolean timeAttackEnabled = true;
    private int timeRemainingSeconds = 90; // 90 seconds survival clock
    private javax.swing.Timer countdownTimer;
    private int playerHearts = 3;
    private int playerScore = 0;
    private int streakCount = 0;
    private boolean soundEnabled = true;

    // Cleared Missions Tracker (0 to 7)
    private final boolean[] clearedMissions = new boolean[8];

    // Module Cartridge Data
    private static final String[] MOD_TITLES = {
            "01 INVADERS", "02 SPACE", "03 HOPPER", "04 MARIO",
            "05 SNAKE", "06 CRANE", "07 PAC-MAN", "08 OVERCOOKED"
    };

    private static final String[] MOD_FULL_NAMES = {
            "CPU SCHEDULING (FCFS)",
            "BANKER DEADLOCK AVOIDANCE",
            "MMU PAGING TRANSLATION",
            "CACHE REPLACEMENT (FIFO)",
            "SEGMENTATION & TRAPS",
            "MEMORY ALLOCATION (FIRST FIT)",
            "DISK HEAD SEEK (FCFS)",
            "MULTIPROGRAMMING CONCURRENCY"
    };

    private static final String[] MOD_LORES = {
            "Alien Dispatch Runway",
            "Deadlock Verification",
            "Virtual MMU Paging",
            "Cache Fault Eviction",
            "Segment Limit Traps",
            "Contiguous RAM Alloc",
            "Magnetic Head Traversal",
            "Kitchen Workload Concurrency"
    };

    private static final String[] MOD_GAMES = {
            "SPACE INVADERS",
            "SPACE SHOOT VAULT",
            "FRAME HOPPER BUS",
            "SUPER MARIO CACHE",
            "CYBER SNAKE HEAP",
            "CYBER CRANE DEFRAG",
            "PAC-MAN DISK HEAD",
            "OVERCOOKED KITCHEN"
    };

    private static final Color[] MOD_COLORS = {
            new Color(5, 217, 232),   // 01 Invaders Cyan
            new Color(255, 211, 25),  // 02 Space Shoot Gold
            new Color(40, 255, 200),  // 03 Frame Hopper Mint
            new Color(255, 115, 115), // 04 Mario Coral
            new Color(90, 245, 140),  // 05 Cyber Snake Green
            new Color(215, 145, 255), // 06 Cyber Crane Violet
            new Color(255, 230, 50),  // 07 Pac-Man Yellow
            new Color(255, 150, 50)   // 08 Overcooked Orange
    };

    private static final String[] MOD_DIFFICULTIES = {
            "[EASY] Lv. 2",
            "[HARD] Lv. 4",
            "[NORMAL] Lv. 3",
            "[NORMAL] Lv. 3",
            "[HARD] Lv. 4",
            "[EASY] Lv. 2",
            "[NORMAL] Lv. 3",
            "[NORMAL] Lv. 3"
    };

    // =========================================================================
    // ARCADE MULTIPLE-CHOICE QUESTION (MCQ) DATA STRUCTURE
    // =========================================================================
    public static class ArcadeMCQ {
        public final String question;
        public final String[] options;
        public final int correctIndex; // 0..3
        public final String explanation;
        public final String formulaHint;
        public final String category;

        public ArcadeMCQ(String question, String[] options, int correctIndex, String explanation, String formulaHint, String category) {
            this.question = question;
            this.options = options;
            this.correctIndex = correctIndex;
            this.explanation = explanation;
            this.formulaHint = formulaHint;
            this.category = category;
        }
    }

    private static final List<List<ArcadeMCQ>> MODULE_MCQS = new ArrayList<>();

    static {
        // Module 0: CPU Invaders (FCFS)
        List<ArcadeMCQ> m0 = new ArrayList<>();
        m0.add(new ArcadeMCQ(
                "Alien Wave W1 (Scout) arrives at 0.0ms with CPU burst of 4.0ms. What is its FINISH TIME (FT)?",
                new String[]{"2.0 ms", "4.0 ms", "5.0 ms", "7.0 ms"},
                1,
                "W1 arrives at 0.0ms and executes immediately for 4.0ms uninterrupted. Finish Time = Start (0.0) + Burst (4.0) = 4.0ms.",
                "Finish Time (FT) = Start Time (ST) + Burst Time (BT)",
                "FINISH TIME CALCULATION"
        ));
        m0.add(new ArcadeMCQ(
                "Alien Wave W2 (Fighter) arrives at 1.0ms with burst 3.0ms. Under non-preemptive FCFS, what is its START TIME (ST)?",
                new String[]{"1.0 ms", "3.0 ms", "4.0 ms", "7.0 ms"},
                2,
                "Under non-preemptive FCFS, W2 cannot preempt W1 and must wait until W1 completes at 4.0ms. ST = max(Arrival, prev Finish) = 4.0ms.",
                "Start Time (ST) = max(Arrival Time, Previous Job Finish Time)",
                "START TIME EVALUATION"
        ));
        m0.add(new ArcadeMCQ(
                "What is the TURNAROUND TIME (TAT) of Alien Wave W2 (Fighter) given Finish=7.0ms and Arrival=1.0ms?",
                new String[]{"3.0 ms", "4.0 ms", "6.0 ms", "7.0 ms"},
                2,
                "Turnaround Time measures total time spent from arrival to completion: TAT = Finish Time (7.0ms) - Arrival Time (1.0ms) = 6.0ms.",
                "Turnaround Time (TAT) = Finish Time (FT) - Arrival Time (AT)",
                "TURNAROUND TIME METRIC"
        ));
        m0.add(new ArcadeMCQ(
                "What is the WAITING TIME (WT) of Alien Wave W3 (Mothership) with Start=7.0ms, Arrival=2.0ms, and Burst=2.0ms?",
                new String[]{"2.0 ms", "5.0 ms", "7.0 ms", "9.0 ms"},
                1,
                "Waiting Time = Start Time (7.0ms) - Arrival Time (2.0ms) = 5.0ms (or TAT 7.0 - BT 2.0 = 5.0ms).",
                "Waiting Time (WT) = Start Time - Arrival Time = TAT - Burst Time",
                "WAITING TIME DELAY"
        ));
        MODULE_MCQS.add(m0);

        // Module 1: Space Shoot Vault (Banker's Deadlock)
        List<ArcadeMCQ> m1 = new ArrayList<>();
        m1.add(new ArcadeMCQ(
                "In Banker's Algorithm, how is the NEED matrix calculated for each process?",
                new String[]{"Need = Max - Allocation", "Need = Allocation + Available", "Need = Max + Available", "Need = Available - Allocation"},
                0,
                "The Need matrix defines remaining resource demands: Need[i][j] = Max[i][j] - Allocation[i][j].",
                "Need = Max Demand - Currently Held Allocation",
                "NEED MATRIX FORMULA"
        ));
        m1.add(new ArcadeMCQ(
                "Process P1 has Max=[3, 2, 2] and Allocation=[2, 0, 0]. What is P1's remaining NEED vector?",
                new String[]{"[1, 2, 2]", "[5, 2, 2]", "[0, 2, 2]", "[1, 0, 2]"},
                0,
                "Need = [3-2, 2-0, 2-0] = [1, 2, 2]. P1 still requires 1 unit of A, 2 units of B, and 2 units of C.",
                "Need[j] = Max[j] - Allocation[j] for each resource type",
                "VECTOR ARITHMETIC"
        ));
        m1.add(new ArcadeMCQ(
                "With Available=[3, 3, 2], when Process P1 (Allocation=[2, 0, 0]) completes, what is the NEW AVAILABLE pool?",
                new String[]{"[1, 1, 0]", "[5, 3, 2]", "[3, 3, 2]", "[2, 0, 0]"},
                1,
                "When a process finishes, it releases all held resources: New Available = Current Available [3, 3, 2] + Allocation [2, 0, 0] = [5, 3, 2].",
                "New Available = Current Available + Process Allocation",
                "RESOURCE RECLAMATION"
        ));
        m1.add(new ArcadeMCQ(
                "In Banker's Deadlock Avoidance, a system state is deemed SAFE if and only if:",
                new String[]{"No processes are currently executing", "At least one safe sequence exists without deadlock", "CPU utilization is exactly 100%", "All memory partitions are defragmented"},
                1,
                "A state is safe if the OS can allocate resources to every process in some sequence such that all run to completion without deadlocking.",
                "Safe State Condition: Need[i] <= Available Work Vector",
                "DEADLOCK SAFETY CONDITION"
        ));
        MODULE_MCQS.add(m1);

        // Module 2: Frame Hopper Bus (MMU Paging)
        List<ArcadeMCQ> m2 = new ArrayList<>();
        m2.add(new ArcadeMCQ(
                "For a virtual memory architecture with 4 KB (4096 bytes) page size, how many offset bits (d) are required?",
                new String[]{"8 bits", "10 bits", "12 bits", "16 bits"},
                2,
                "Since 2^12 = 4096, exactly 12 bits are required in the logical address to index every byte inside a 4 KB page frame.",
                "Offset Bits (d) = log2(Page Size in bytes)",
                "OFFSET BIT CALCULATION"
        ));
        m2.add(new ArcadeMCQ(
                "Given logical address 5120 and page size 4096, what is the logical Page Number (p) and Offset (d)?",
                new String[]{"p = 1, d = 1024", "p = 0, d = 5120", "p = 2, d = 1024", "p = 1, d = 512"},
                0,
                "Page Number = 5120 / 4096 = 1. Offset = 5120 % 4096 = 1024. Therefore, p = 1 and d = 1024.",
                "p = LogicalAddr / PageSize | d = LogicalAddr % PageSize",
                "PAGE & OFFSET EXTRACTION"
        ));
        m2.add(new ArcadeMCQ(
                "If Page 1 maps to Physical Frame 2 (4096 bytes/frame) with offset 1024, what is the PHYSICAL ADDRESS?",
                new String[]{"5120", "8192", "9216", "10240"},
                2,
                "Physical Address = (Frame Number * PageSize) + Offset = (2 * 4096) + 1024 = 8192 + 1024 = 9216.",
                "Physical Address = (Frame Number * PageSize) + Offset",
                "PHYSICAL TRANSLATION"
        ));
        m2.add(new ArcadeMCQ(
                "What hardware interrupt trap occurs when a process references a page marked INVALID in the page table?",
                new String[]{"System Reboot Trap", "Page Fault Trap", "TLB Purge Signal", "Memory Boundary Fault"},
                1,
                "When the valid/invalid bit is 0, the MMU cannot resolve the address in RAM and triggers a Page Fault Trap to fetch it from disk.",
                "Invalid Page Table Entry -> OS Page Fault Trap Handler",
                "HARDWARE INTERRUPT"
        ));
        MODULE_MCQS.add(m2);

        // Module 3: Super Mario Cache (FIFO Cache)
        List<ArcadeMCQ> m3 = new ArrayList<>();
        m3.add(new ArcadeMCQ(
                "When a 3-slot CPU cache is initially empty and page 1 is accessed, what event occurs?",
                new String[]{"Cache Hit", "Cache Miss (Cold Fault)", "Cache Flush", "Write-Through"},
                1,
                "Because page 1 is not resident in cache, a Cold Cache Miss occurs and the CPU loads page 1 from main memory into slot 0.",
                "Accessing non-resident memory = Cache Miss / Cold Fault",
                "COLD MISS DETECTION"
        ));
        m3.add(new ArcadeMCQ(
                "When the cache holds [1, 2, 3] in arrival order, and page 4 arrives, which page is EVICTED under FIFO?",
                new String[]{"Page 1", "Page 2", "Page 3", "Page 4"},
                0,
                "FIFO (First-In, First-Out) always evicts the page that has been in the cache the longest: Page 1 entered first, so it is evicted first.",
                "FIFO Policy: Evict the oldest resident cache entry",
                "FIFO EVICTION TARGET"
        ));
        m3.add(new ArcadeMCQ(
                "If the cache currently contains [4, 2, 3] and page 2 is accessed, what is the result?",
                new String[]{"Cache Miss (evicts 4)", "Cache Hit (zero eviction)", "Cache flushes all slots", "Page 2 is duplicated in slot 0"},
                1,
                "Page 2 is already resident in the cache, resulting in an instant Cache Hit with zero eviction and minimal latency.",
                "Cache Hit occurs when requested block is already in cache",
                "CACHE HIT EVALUATION"
        ));
        m3.add(new ArcadeMCQ(
                "What is Belady's Anomaly in FIFO page replacement?",
                new String[]{"Increasing cache frames can cause more page faults", "Cache hits drop to zero permanently", "FIFO always outperforms LRU", "Memory partition leaks occur"},
                0,
                "Belady's Anomaly proves that for specific reference strings, allocating MORE frames to a FIFO cache can unexpectedly INCREASE total faults.",
                "Belady's Anomaly: Frames ↑ -> Page Faults ↑ (FIFO only)",
                "BELADY'S ANOMALY"
        ));
        MODULE_MCQS.add(m3);

        // Module 4: Cyber Snake Heap (Segmentation)
        List<ArcadeMCQ> m4 = new ArrayList<>();
        m4.add(new ArcadeMCQ(
                "How does the MMU hardware evaluate the physical address for a valid segment reference?",
                new String[]{"Base Register - Offset", "Base Register + Offset", "Limit Register * Offset", "Base Register + Limit Register"},
                1,
                "The MMU adds the offset to the segment's starting physical base register: Physical Address = Base + Offset (provided Offset < Limit).",
                "Physical Address = Base Address + Valid Offset",
                "SEGMENT ADDRESS MAPPING"
        ));
        m4.add(new ArcadeMCQ(
                "For Segment 0 (Base=1000, Limit=400), if the logical offset is 150, what is the physical address?",
                new String[]{"1150", "1400", "850", "Trap SegFault"},
                0,
                "Offset 150 < Limit 400 is strictly legal and valid. Physical Address = Base (1000) + Offset (150) = 1150.",
                "Valid Condition: Offset < Limit | PhysAddr = Base + Offset",
                "VALID BOUNDS VERIFICATION"
        ));
        m4.add(new ArcadeMCQ(
                "For Segment 1 (Base=2000, Limit=500), if a program requests logical offset 600, what happens?",
                new String[]{"Physical Address 2600", "Hardware Trap (Segmentation Fault)", "Offset wrapped to 100", "Request ignored silently"},
                1,
                "Because offset 600 >= Limit 500, a memory protection violation occurs and the MMU triggers a Segmentation Fault Hardware Trap!",
                "If Offset >= Limit -> Immediate Hardware TRAP (SegFault)",
                "PROTECTION TRAP TRIGGER"
        ));
        m4.add(new ArcadeMCQ(
                "Unlike Paging which divides memory into fixed-size physical blocks, Segmentation divides memory by:",
                new String[]{"Uniform 4KB sectors", "Logical programmer units (Functions, Arrays, Stack, Heap)", "Hardware track cylinders", "Fixed 64-byte cache lines"},
                1,
                "Segmentation supports a user-oriented view of memory, splitting program space into variable-length logical segments.",
                "Segmentation reflects logical program structures",
                "SEGMENTATION ARCHITECTURE"
        ));
        MODULE_MCQS.add(m4);

        // Module 5: Cyber Crane Defrag (Memory Allocation)
        List<ArcadeMCQ> m5 = new ArrayList<>();
        m5.add(new ArcadeMCQ(
                "Under FIRST-FIT allocation, in which hole is an incoming process of 212 KB placed given holes [100, 500, 200, 300, 600] KB?",
                new String[]{"100 KB hole", "500 KB hole", "300 KB hole", "600 KB hole"},
                1,
                "First-Fit scans memory holes from the start and chooses the very first hole large enough to satisfy the request: 500 KB > 212 KB.",
                "First-Fit: Allocate the first hole with size >= requested size",
                "FIRST-FIT ALGORITHM"
        ));
        m5.add(new ArcadeMCQ(
                "Under BEST-FIT allocation, in which hole is the 212 KB process placed given holes [100, 500, 200, 300, 600] KB?",
                new String[]{"500 KB hole", "300 KB hole", "200 KB hole", "600 KB hole"},
                1,
                "Best-Fit searches all available holes for the SMALLEST hole >= 212 KB. The 300 KB hole leaves only 88 KB leftover fragment.",
                "Best-Fit: Allocate the smallest hole with size >= requested size",
                "BEST-FIT ALGORITHM"
        ));
        m5.add(new ArcadeMCQ(
                "Under WORST-FIT allocation, in which hole is the 212 KB process placed given holes [100, 500, 200, 300, 600] KB?",
                new String[]{"100 KB hole", "300 KB hole", "500 KB hole", "600 KB hole"},
                3,
                "Worst-Fit allocates the LARGEST available hole in memory (600 KB), leaving a large contiguous leftover hole of 388 KB.",
                "Worst-Fit: Allocate the largest available hole in memory",
                "WORST-FIT ALGORITHM"
        ));
        m5.add(new ArcadeMCQ(
                "What is EXTERNAL FRAGMENTATION in dynamic contiguous memory allocation?",
                new String[]{"Unused space inside an allocated partition", "Total free memory is sufficient, but broken into non-contiguous slices", "Disk platter magnetic track damage", "Call stack buffer overflow"},
                1,
                "External fragmentation occurs when enough total memory exists to satisfy a request, but storage is fragmented into unusable small chunks.",
                "Compaction / Defragmentation merges external fragments",
                "EXTERNAL FRAGMENTATION"
        ));
        MODULE_MCQS.add(m5);

        // Module 6: Pac-Man Disk Head (Disk Head Scheduling)
        List<ArcadeMCQ> m6 = new ArrayList<>();
        m6.add(new ArcadeMCQ(
                "The disk head starts at cylinder 53. If the next request is cylinder 98, what is the seek distance?",
                new String[]{"35 cylinders", "45 cylinders", "53 cylinders", "98 cylinders"},
                1,
                "Seek distance measures the absolute cylinder hops traversed: |Target Cylinder - Current Cylinder| = |98 - 53| = 45 cylinders.",
                "Seek Hop Distance = |Target Cylinder - Current Cylinder|",
                "CYLINDER SEEK HOP"
        ));
        m6.add(new ArcadeMCQ(
                "After servicing cylinder 98, the head moves to cylinder 183. What is this second hop distance?",
                new String[]{"85 cylinders", "98 cylinders", "130 cylinders", "183 cylinders"},
                0,
                "Seek hop distance = |183 - 98| = 85 cylinders.",
                "Hop Distance = |183 - 98| = 85 cylinders",
                "SUBSEQUENT HOP DISTANCE"
        ));
        m6.add(new ArcadeMCQ(
                "What is the CUMULATIVE head movement after servicing cylinder 98 (hop 45) and cylinder 183 (hop 85)?",
                new String[]{"45 cylinders", "85 cylinders", "130 cylinders", "281 cylinders"},
                2,
                "Cumulative head movement is the running sum of all individual seek hops: 45 + 85 = 130 cylinders.",
                "Cumulative Seek = Sum of individual seek hops = 45 + 85 = 130",
                "CUMULATIVE SEEK DISTANCE"
        ));
        m6.add(new ArcadeMCQ(
                "Which disk scheduling algorithm always selects the pending request closest to current head position?",
                new String[]{"FCFS", "SSTF (Shortest Seek Time First)", "SCAN (Elevator)", "C-LOOK"},
                1,
                "SSTF (Shortest Seek Time First) selects the request that requires the least head arm movement from current track.",
                "SSTF selects min(|Request Cylinder - Current Cylinder|)",
                "SSTF POLICY"
        ));
        MODULE_MCQS.add(m6);

        // Module 7: Overcooked Kitchen (Multiprogramming & Concurrency)
        List<ArcadeMCQ> m7 = new ArrayList<>();
        m7.add(new ArcadeMCQ(
                "What is a CRITICAL SECTION in concurrent multi-threaded execution?",
                new String[]{"A section of code accessing shared resources that cannot be executed by multiple threads simultaneously", "An infinite loop causing CPU lockup", "A motherboard electrical failure", "The boot sector of the storage disk"},
                0,
                "A critical section modifies shared data (variables, memory, hardware). Mutual exclusion is mandatory to prevent race conditions.",
                "Critical Section: Shared resource access requiring Mutual Exclusion",
                "CRITICAL SECTION DEFINITION"
        ));
        m7.add(new ArcadeMCQ(
                "What is the primary role of a MUTEX (Mutual Exclusion) Lock?",
                new String[]{"Allow unlimited simultaneous read/writes", "Guarantee that only ONE thread enters the critical section at a time", "Increase CPU clock multiplier", "Terminate background processes"},
                1,
                "A Mutex ensures mutual exclusion: only the thread holding the lock may enter the critical section; others must block until release.",
                "Mutex Lock: acquire() -> Enter Critical Section -> release()",
                "MUTEX LOCK ROLE"
        ));
        m7.add(new ArcadeMCQ(
                "If Chef Mario holds Lock A and waits for Lock B, while Chef Luigi holds Lock B and waits for Lock A, what occurs?",
                new String[]{"Cache Hit", "Deadlock (Circular Wait)", "Context Switch", "Priority Inversion"},
                1,
                "A Circular Wait condition where two or more threads mutually wait on locks held by each other produces unrecoverable Deadlock.",
                "Coffman Condition: Circular Wait between mutually held locks = Deadlock",
                "DEADLOCK CONDITIONS"
        ));
        m7.add(new ArcadeMCQ(
                "Which synchronization primitive maintains an integer counter with atomic wait() (P) and signal() (V) operations?",
                new String[]{"Semaphore", "Spinlock", "Pipe Pointer", "Virtual Address Bus"},
                0,
                "A Semaphore uses an atomic counter with P/wait() (decrement and block if <= 0) and V/signal() (increment and wake) primitives.",
                "Semaphore: wait(S) decrements; signal(S) increments",
                "SEMAPHORE SYNCHRONIZATION"
        ));
        MODULE_MCQS.add(m7);
    }

    // =========================================================================
    // UI COMPONENTS
    // =========================================================================
    // Header capsules
    private JLabel lblModuleTitle;
    private JLabel lblGameSubtitle;
    private JLabel lblDifficultyBadge;
    private JLabel lblInstruction;
    private JLabel lblMascotAvatar;
    private JLabel lblHearts;
    private JLabel lblScore;
    private JLabel lblQuestionTracker;
    private JLabel lblStreak;
    private JLabel lblTimer;

    // Cartridge Switcher Buttons
    private final JButton[] modButtons = new JButton[8];

    // Left Pane Components (Reference Table & Schematic)
    private DefaultTableModel scenarioTableModel;
    private JTable scenarioTable;
    private MiniAlgorithmSchematicPanel schematicPanel;

    // Right Pane Components (Quiz Combat HUD)
    private JLabel lblQuizHeaderTag;
    private JPanel pnlDotsTracker;
    private JLabel lblRewardTag;
    private JTextArea txtQuestionPrompt;
    private final JButton[] btnOptions = new JButton[4];
    private JPanel pnlExplanationCard;
    private JLabel lblExplanationBanner;
    private JTextArea txtExplanationBody;
    private JLabel lblFormulaPill;
    private JButton btnFormulaHint;
    private JButton btnNextQuestion;

    // Victory Screen Overlay
    private JPanel pnlVictoryCard;
    private JLabel lblVictoryRank;
    private JLabel lblVictoryScore;
    private JLabel lblVictoryAccuracy;

    // Card containers
    private JPanel pnlQuizCombatCard;
    private JPanel rightPaneContainer;

    public QuizPanel(ArcadeOS parent) {
        this.parentFrame = parent;
        setLayout(new BorderLayout());
        setBackground(new Color(10, 7, 18));

        // Ensure root card container is completely transparent so QuizPanel's space & grid background shines through
        rootCardContainer.setOpaque(false);

        // Setup Countdown Timer
        initCountdownTimer();

        // Screen 1: Stage Select Hub
        rootCardContainer.add(createStageSelectScreen(), VIEW_STAGE_SELECT);

        // Screen 2: Interactive Arcade Quiz Arena
        rootCardContainer.add(createWorksheetScreen(), VIEW_WORKSHEET);

        add(rootCardContainer, BorderLayout.CENTER);

        // Pre-initialize Mission 01 so the arena is immediately armed and ready
        loadMission(0);

        // Show Stage Select Hub by default
        switchView(VIEW_STAGE_SELECT);

        // Initialize Background Interactive Circular Micro-Dots (identical to LoginPanel)
        initBackgroundParticles(95);

        // Background particles animation & physics timer (~33 FPS, exactly like LoginPanel)
        bgParticleTimer = new javax.swing.Timer(30, e -> {
            if (isShowing()) {
                animTick += 1.0f;
                updateBackgroundParticles();
                repaint();
            }
        });
        bgParticleTimer.start();

        // Mouse tracking across window for background particles
        try {
            globalMouseListener = event -> {
                if (event instanceof MouseEvent me) {
                    if (isShowing()) {
                        try {
                            Point p = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), QuizPanel.this);
                            mouseX = p.x;
                            mouseY = p.y;
                        } catch (Exception ignored) {}
                    }
                }
            };
            Toolkit.getDefaultToolkit().addAWTEventListener(globalMouseListener, AWTEvent.MOUSE_MOTION_EVENT_MASK);
        } catch (Exception ignored) {}

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                mouseX = -999;
                mouseY = -999;
            }
        });
    }

    public void switchView(String view) {
        this.activeView = view;
        rootCardLayout.show(rootCardContainer, view);
        if (VIEW_STAGE_SELECT.equals(view)) {
            if (countdownTimer != null) {
                countdownTimer.stop();
            }
        }
        revalidate();
        repaint();
    }

    public String getActiveView() {
        return activeView;
    }

    public boolean isCountdownTimerRunning() {
        return countdownTimer != null && countdownTimer.isRunning();
    }

    public void selectAndEngageMissionForTest(int modIdx) {
        selectAndEngageMission(modIdx);
    }

    private void initCountdownTimer() {
        countdownTimer = new javax.swing.Timer(1000, e -> {
            // Guard: Only count down if player is actively inside the Quiz Battlefield Arena and panel is showing
            if (!VIEW_WORKSHEET.equals(activeView) || !isShowing()) {
                countdownTimer.stop();
                return;
            }
            if (timeRemainingSeconds > 0) {
                timeRemainingSeconds--;
                updateTimerDisplay();
                if (timeRemainingSeconds == 15) {
                    playBuzzerSound();
                }
            } else {
                countdownTimer.stop();
                handleTimeout();
            }
        });
    }

    private void updateTimerDisplay() {
        if (lblTimer == null) return;
        int m = timeRemainingSeconds / 60;
        int s = timeRemainingSeconds % 60;
        String timeStr = String.format("⏱ %02d:%02d", m, s);
        lblTimer.setText(timeStr);
        if (timeRemainingSeconds <= 15) {
            lblTimer.setForeground(RetroTheme.ACCENT_PINK);
        } else {
            lblTimer.setForeground(RetroTheme.ACCENT_CYAN);
        }
    }

    private void updateHeartsDisplay() {
        if (lblHearts == null) return;
        StringBuilder sb = new StringBuilder("LIVES: ");
        for (int i = 0; i < 3; i++) {
            if (i < playerHearts) {
                sb.append("♥ ");
            } else {
                sb.append("♡ ");
            }
        }
        lblHearts.setText(sb.toString().trim());
        lblHearts.setForeground(playerHearts > 1 ? RetroTheme.ACCENT_PINK : Color.RED);
    }

    private void updateScoreDisplay() {
        if (lblScore == null) return;
        lblScore.setText(String.format("XP: %06d", playerScore));
        if (lblStreak != null) {
            lblStreak.setText(String.format("🔥 %dx STREAK", streakCount));
            lblStreak.setForeground(streakCount >= 2 ? RetroTheme.ACCENT_YELLOW : RetroTheme.TEXT_DIM);
        }
    }

    // =========================================================================
    // STATIC DEEP SPACE & OUTRUN GRID BACKGROUND (100% ROCK-SOLID STATIC)
    // =========================================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            g2.dispose();
            return;
        }

        // 1. Deep Space Gradient Background
        GradientPaint spaceGrad = new GradientPaint(
                0, 0, new Color(10, 7, 18),
                0, h, new Color(18, 12, 32)
        );
        g2.setPaint(spaceGrad);
        g2.fillRect(0, 0, w, h);

        // 2. Ambient Corner Glows
        Point2D centerTL = new Point2D.Float(100, 100);
        float radiusTL = 450;
        float[] distTL = {0.0f, 1.0f};
        Color[] colorsTL = {new Color(5, 217, 232, 25), new Color(10, 7, 18, 0)};
        RadialGradientPaint glowTL = new RadialGradientPaint(centerTL, radiusTL, distTL, colorsTL);
        g2.setPaint(glowTL);
        g2.fillRect(0, 0, w, h);

        Point2D centerBR = new Point2D.Float(w - 100, h - 100);
        Color[] colorsBR = {new Color(255, 42, 109, 22), new Color(18, 12, 32, 0)};
        RadialGradientPaint glowBR = new RadialGradientPaint(centerBR, radiusTL, distTL, colorsBR);
        g2.setPaint(glowBR);
        g2.fillRect(0, 0, w, h);

        // 3. Cyber Dot-Matrix Grid across background
        g2.setColor(new Color(90, 65, 140, 48));
        int dotSpacing = 28;
        for (int x = dotSpacing / 2; x < w; x += dotSpacing) {
            for (int y = dotSpacing / 2; y < h; y += dotSpacing) {
                g2.fillRect(x, y, 2, 2);
            }
        }

        // 4. Interactive Floating Background Micro-Dots & Constellation Lines (identical to LoginPanel)
        paintBackgroundParticles(g2, w, h);

        // 5. Static Retro Grid Lines at Bottom
        int horizonY = h - 65;
        g2.setColor(new Color(255, 42, 109, 32));
        g2.drawLine(0, horizonY, w, horizonY);
        for (int x = -100; x < w + 200; x += 65) {
            g2.drawLine(w / 2, horizonY, x, h);
        }
        for (int y = horizonY + 8; y < h; y += (y - horizonY + 4)) {
            g2.drawLine(0, y, w, y);
        }

        g2.dispose();
    }

    // =========================================================================
    // BACKGROUND MICRO-DOTS PHYSICS & RENDERING ENGINE (FROM LOGINPANEL)
    // =========================================================================
    private void initBackgroundParticles(int count) {
        bgDots.clear();
        Random rnd = new Random(2026);
        for (int i = 0; i < count; i++) {
            float px = rnd.nextFloat() * 1920f;
            float py = rnd.nextFloat() * 1080f;
            float vx = (rnd.nextFloat() - 0.5f) * 0.7f;
            float vy = (rnd.nextFloat() - 0.5f) * 0.7f;

            float randTier = rnd.nextFloat();
            float r;
            if (randTier < 0.50f) {
                r = 0.9f + rnd.nextFloat() * 0.5f;
            } else if (randTier < 0.85f) {
                r = 1.5f + rnd.nextFloat() * 0.6f;
            } else {
                r = 2.2f + rnd.nextFloat() * 0.4f;
            }

            float a = 0.40f + rnd.nextFloat() * 0.55f;
            float phase = rnd.nextFloat() * 6.28f;
            Color c = DOT_COLORS[rnd.nextInt(DOT_COLORS.length)];
            bgDots.add(new BgDotParticle(px, py, vx, vy, r, a, phase, c));
        }
    }

    void updateBackgroundParticles() {
        int w = getWidth() > 0 ? getWidth() : 1280;
        int h = getHeight() > 0 ? getHeight() : 860;

        if (mouseX >= 0 && mouseY >= 0) {
            if (smoothMouseX < 0) {
                smoothMouseX = mouseX;
                smoothMouseY = mouseY;
            } else {
                smoothMouseX += (mouseX - smoothMouseX) * 0.15f;
                smoothMouseY += (mouseY - smoothMouseY) * 0.15f;
            }
        }

        for (BgDotParticle p : bgDots) {
            p.pulsePhase += 0.05f;
            p.currentRadius = p.baseRadius + (float) Math.sin(p.pulsePhase) * 0.3f;

            if (smoothMouseX >= 0 && smoothMouseY >= 0) {
                float dx = smoothMouseX - p.x;
                float dy = smoothMouseY - p.y;
                float distSq = dx * dx + dy * dy;
                float dist = (float) Math.sqrt(distSq);

                if (dist < 320f && dist > 1f) {
                    float ndx = dx / dist;
                    float ndy = dy / dist;

                    float pullFactor = (1.0f - dist / 320f);
                    float pullForce = pullFactor * 0.38f;

                    if (dist < 45f) {
                        float pushFactor = (1.0f - dist / 45f) * 0.50f;
                        pullForce -= pushFactor;
                    }

                    p.vx += ndx * pullForce;
                    p.vy += ndy * pullForce;

                    float perpX = -ndy;
                    float perpY = ndx;
                    p.vx += perpX * (pullFactor * 0.20f);
                    p.vy += perpY * (pullFactor * 0.20f);
                }
            }

            p.vx *= 0.965f;
            p.vy *= 0.965f;

            float speed = (float) Math.sqrt(p.vx * p.vx + p.vy * p.vy);
            if (speed < 0.18f) {
                p.vx += ((float) Math.random() - 0.5f) * 0.12f;
                p.vy += ((float) Math.random() - 0.5f) * 0.12f;
            } else if (speed > 3.0f) {
                p.vx = (p.vx / speed) * 3.0f;
                p.vy = (p.vy / speed) * 3.0f;
            }

            p.x += p.vx;
            p.y += p.vy;

            if (p.x < -20) p.x = w + 20;
            else if (p.x > w + 20) p.x = -20;
            if (p.y < -20) p.y = h + 20;
            else if (p.y > h + 20) p.y = -20;
        }
    }

    private void paintBackgroundParticles(Graphics2D g, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int numDots = bgDots.size();
        for (int i = 0; i < numDots; i++) {
            BgDotParticle p1 = bgDots.get(i);
            for (int j = i + 1; j < numDots; j++) {
                BgDotParticle p2 = bgDots.get(j);
                float dx = p1.x - p2.x;
                float dy = p1.y - p2.y;
                float distSq = dx * dx + dy * dy;
                if (distSq < 3600f) {
                    float dist = (float) Math.sqrt(distSq);
                    int lineAlpha = (int) ((1.0f - dist / 60f) * 26f);
                    if (lineAlpha > 0) {
                        g2.setColor(new Color(p1.color.getRed(), p1.color.getGreen(), p1.color.getBlue(), lineAlpha));
                        g2.setStroke(new BasicStroke(0.65f));
                        g2.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
                    }
                }
            }
        }

        if (smoothMouseX >= 0 && smoothMouseY >= 0) {
            for (BgDotParticle p : bgDots) {
                float dx = smoothMouseX - p.x;
                float dy = smoothMouseY - p.y;
                float distSq = dx * dx + dy * dy;
                if (distSq < 14400f) {
                    float dist = (float) Math.sqrt(distSq);
                    int tetherAlpha = (int) ((1.0f - dist / 120f) * 55f);
                    if (tetherAlpha > 0) {
                        g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), tetherAlpha));
                        g2.setStroke(new BasicStroke(0.85f));
                        g2.drawLine((int) smoothMouseX, (int) smoothMouseY, (int) p.x, (int) p.y);
                    }
                }
            }
        }

        for (BgDotParticle p : bgDots) {
            float r = Math.max(0.6f, p.currentRadius);
            int baseAlpha = (int) (p.alpha * 255f);
            baseAlpha = Math.max(15, Math.min(255, baseAlpha));

            int haloAlpha = (int) (baseAlpha * 0.32f);
            float haloR = r * 2.6f;
            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), haloAlpha));
            g2.fillOval((int) (p.x - haloR), (int) (p.y - haloR), (int) (haloR * 2), (int) (haloR * 2));

            g2.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), baseAlpha));
            g2.fillOval((int) (p.x - r), (int) (p.y - r), (int) (r * 2), (int) (r * 2));

            if (r > 1.8f) {
                g2.setColor(new Color(255, 255, 255, (int) (baseAlpha * 0.65f)));
                g2.fillOval((int) (p.x - 0.7f), (int) (p.y - 0.7f), 2, 2);
            }
        }

        g2.dispose();
    }

    // =========================================================================
    // SCREEN 1: STAGE SELECT HUB (8 ROCK-SOLID STATIC CABINET CARTRIDGES)
    // =========================================================================
    private JPanel createStageSelectScreen() {
        JPanel hub = new JPanel(new BorderLayout(0, 8));
        hub.setOpaque(false);
        hub.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // Top Marquee Header
        JPanel topMarquee = new JPanel(new BorderLayout());
        topMarquee.setOpaque(false);

        JLabel titleLbl = new JLabel("★ SELECT ALGORITHM MISSION CARTRIDGE | INSERT COIN // ENGAGE MISSION ★", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Consolas", Font.BOLD, 13));
        titleLbl.setForeground(RetroTheme.ACCENT_PINK);
        topMarquee.add(titleLbl, BorderLayout.CENTER);

        JLabel xpMarquee = new JLabel(String.format("XP: %06d", playerScore));
        xpMarquee.setFont(new Font("Consolas", Font.BOLD, 11));
        xpMarquee.setForeground(RetroTheme.ACCENT_YELLOW);
        topMarquee.add(xpMarquee, BorderLayout.EAST);

        hub.add(topMarquee, BorderLayout.NORTH);

        // 8 Cartridge Boxes Grid (2 rows x 4 columns) centered in GridBagLayout (Keeps boxes at balanced 'sakto lang' size, preventing full-screen stretch)
        JPanel gridWrapper = new JPanel(new GridBagLayout());
        gridWrapper.setOpaque(false);

        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 10));
        grid.setOpaque(false);

        for (int i = 0; i < 8; i++) {
            grid.add(createCabinetCard(i));
        }
        gridWrapper.add(grid);
        hub.add(gridWrapper, BorderLayout.CENTER);

        // Bottom Navigation Bar
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        JButton btnBack = RetroTheme.createRetroButton("< MENU [ESC]", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnBack.setFont(new Font("Consolas", Font.BOLD, 11));
        btnBack.addActionListener(e -> {
            playTone(480, 20);
            if (parentFrame != null) {
                parentFrame.showScreen(ArcadeOS.SCREEN_MAIN_MENU);
            }
        });
        bottom.add(btnBack, BorderLayout.WEST);

        // ESC Keybinding for entire hub screen
        hub.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESC_TO_MAIN_MENU");
        hub.getActionMap().put("ESC_TO_MAIN_MENU", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playTone(480, 20);
                if (parentFrame != null) {
                    parentFrame.showScreen(ArcadeOS.SCREEN_MAIN_MENU);
                }
            }
        });

        JPanel modeToggleBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        modeToggleBox.setOpaque(false);

        JButton btnTimeToggle = RetroTheme.createRetroButton("SURVIVAL MODE (90s): ON", new Color(18, 55, 35), RetroTheme.ACCENT_GREEN);
        btnTimeToggle.setFont(new Font("Consolas", Font.BOLD, 11));
        btnTimeToggle.setToolTipText("Toggle 90-second survival countdown timer with 3 lives for extra thrill!");
        btnTimeToggle.addActionListener(e -> {
            timeAttackEnabled = !timeAttackEnabled;
            btnTimeToggle.setText(timeAttackEnabled ? "SURVIVAL MODE (90s): ON" : "PRACTICE MODE (NO TIMER): ON");
            btnTimeToggle.setBackground(timeAttackEnabled ? new Color(18, 55, 35) : new Color(38, 28, 55));
            btnTimeToggle.setForeground(timeAttackEnabled ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_CYAN);
            playTone(560, 20);
        });
        modeToggleBox.add(btnTimeToggle);

        JLabel hint = new JLabel("CHOOSE ANY OF THE 8 MODULES TO ENTER THE BATTLEFIELD ARENA!");
        hint.setFont(new Font("Consolas", Font.BOLD, 11));
        hint.setForeground(RetroTheme.TEXT_DIM);
        modeToggleBox.add(hint);

        bottom.add(modeToggleBox, BorderLayout.CENTER);

        JLabel soundToggle = new JLabel("CLICK ANY CARTRIDGE TO LAUNCH >>");
        soundToggle.setFont(new Font("Consolas", Font.BOLD, 11));
        soundToggle.setForeground(RetroTheme.ACCENT_YELLOW);
        bottom.add(soundToggle, BorderLayout.EAST);

        hub.add(bottom, BorderLayout.SOUTH);
        return hub;
    }

    private JPanel createCabinetCard(int modIdx) {
        final Color c = MOD_COLORS[modIdx];
        final BufferedImage mascot = getMascotSprite(modIdx, 48);

        JPanel card = new JPanel(new BorderLayout()) {
            private boolean isHovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                    @Override public void mousePressed(MouseEvent e) { selectAndEngageMission(modIdx); }
                });
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // 1. Draw Aligned Game-Themed Background
                drawCabinetGameThemedBackground(g2, modIdx, w, h, c, isHovered);

                // 2. Glowing Oval Pedestal beneath Mascot Sprite
                int cx = w / 2;
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), isHovered ? 90 : 60));
                g2.fillOval(cx - 38, 96, 76, 16);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), isHovered ? 200 : 120));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(cx - 38, 96, 76, 16);

                // 3. Top Arcade Header Strip
                g2.setColor(new Color(c.getRed() / 4, c.getGreen() / 4, c.getBlue() / 4, 230));
                g2.fillRoundRect(2, 2, w - 4, 26, 12, 12);
                g2.fillRect(2, 15, w - 4, 11);

                // Top Header Text: MISSION 01 (Left) & Difficulty (Right)
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                g2.setColor(c);
                g2.drawString(String.format("MISSION %02d", modIdx + 1), 12, 18);

                g2.setColor(RetroTheme.ACCENT_YELLOW);
                String diff = MOD_DIFFICULTIES[modIdx];
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(diff, w - 12 - fm.stringWidth(diff), 18);

                // 4. Mascot Sprite (Centered at y=40..90)
                if (mascot != null) {
                    int mx = (w - mascot.getWidth()) / 2;
                    g2.drawImage(mascot, mx, 40, null);
                }

                // 5. Game Name (Bold White at y=128)
                g2.setFont(new Font("Consolas", Font.BOLD, 13));
                g2.setColor(Color.WHITE);
                String gameName = MOD_GAMES[modIdx];
                fm = g2.getFontMetrics();
                g2.drawString(gameName, (w - fm.stringWidth(gameName)) / 2, 128);

                // 6. Algorithm Pill Capsule at y=144
                String fullAlgo = MOD_FULL_NAMES[modIdx];
                g2.setFont(new Font("Consolas", Font.BOLD, 10));
                fm = g2.getFontMetrics();
                int pillW = fm.stringWidth(fullAlgo) + 16;
                int pillX = (w - pillW) / 2;
                g2.setColor(new Color(c.getRed() / 4, c.getGreen() / 4, c.getBlue() / 4, 210));
                g2.fillRoundRect(pillX, 138, pillW, 18, 6, 6);
                g2.setColor(c);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(pillX, 138, pillW, 18, 6, 6);
                g2.drawString(fullAlgo, pillX + 8, 151);

                // 7. Lore Line at y=172
                g2.setFont(new Font("Consolas", Font.PLAIN, 10));
                g2.setColor(new Color(190, 195, 215));
                String lore = MOD_LORES[modIdx];
                fm = g2.getFontMetrics();
                g2.drawString(lore, (w - fm.stringWidth(lore)) / 2, 172);

                // 8. Reward Badge at y=188
                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(RetroTheme.ACCENT_GREEN);
                String rwd = "REWARD: +150 XP";
                fm = g2.getFontMetrics();
                g2.drawString(rwd, (w - fm.stringWidth(rwd)) / 2, 188);

                // 9. Outer Border (Glows brighter on hover)
                g2.setColor(isHovered ? c : new Color(c.getRed() / 2, c.getGreen() / 2, c.getBlue() / 2, 130));
                g2.setStroke(new BasicStroke(isHovered ? 2.0f : 1.2f));
                g2.drawRoundRect(1, 1, w - 2, h - 2, 12, 12);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(285, 245));
        card.setMinimumSize(new Dimension(285, 245));
        card.setMaximumSize(new Dimension(285, 245));

        JButton btnEngage = new JButton("ENGAGE MISSION >>") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(c.getRed() / 3, c.getGreen() / 3, c.getBlue() / 3));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(c);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnEngage.setFont(new Font("Consolas", Font.BOLD, 11));
        btnEngage.setForeground(c);
        btnEngage.setContentAreaFilled(false);
        btnEngage.setOpaque(false);
        btnEngage.setFocusPainted(false);
        btnEngage.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        btnEngage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEngage.addActionListener(e -> selectAndEngageMission(modIdx));
        card.add(btnEngage, BorderLayout.SOUTH);

        return card;
    }

    private void drawCabinetGameThemedBackground(Graphics2D g2, int modIdx, int w, int h, Color c, boolean hovered) {
        GradientPaint bgGrad = new GradientPaint(
                0, 0, new Color(16, 12, 30),
                0, h, new Color(c.getRed() / 6, c.getGreen() / 6, c.getBlue() / 6)
        );
        g2.setPaint(bgGrad);
        g2.fillRoundRect(1, 1, w - 2, h - 2, 12, 12);

        switch (modIdx) {
            case 0: drawCabinetInvadersBg(g2, w, h, c); break;
            case 1: drawCabinetSpaceShootBg(g2, w, h, c); break;
            case 2: drawCabinetFrameHopperBg(g2, w, h, c); break;
            case 3: drawCabinetMarioCacheBg(g2, w, h, c); break;
            case 4: drawCabinetSnakeBg(g2, w, h, c); break;
            case 5: drawCabinetCraneBg(g2, w, h, c); break;
            case 6: drawCabinetPacmanBg(g2, w, h, c); break;
            case 7: drawCabinetOvercookedBg(g2, w, h, c); break;
        }
    }

    private void drawCabinetInvadersBg(Graphics2D g2, int w, int h, Color c) {
        g2.setColor(new Color(255, 255, 255, 45));
        int[] starX = {22, 58, 85, 130, 185, 220, 260, 40, 150, 240};
        int[] starY = {35, 62, 40, 55, 38, 70, 48, 105, 110, 95};
        for (int i = 0; i < starX.length; i++) {
            g2.fillRect(starX[i] % w, starY[i], (i % 2 == 0) ? 2 : 1, (i % 2 == 0) ? 2 : 1);
        }
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 35));
        for (int x = 20; x < w - 20; x += 40) {
            g2.fillRect(x, 48, 8, 5);
        }
    }

    private void drawCabinetSpaceShootBg(Graphics2D g2, int w, int h, Color c) {
        int cx = w / 2;
        int cy = 68;
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 28));
        g2.drawOval(cx - 45, cy - 45, 90, 90);
        g2.drawOval(cx - 28, cy - 28, 56, 56);
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
        g2.drawLine(cx - 50, cy, cx + 50, cy);
        g2.drawLine(cx, cy - 50, cx, cy + 50);
    }

    private void drawCabinetFrameHopperBg(Graphics2D g2, int w, int h, Color c) {
        g2.setColor(new Color(30, 60, 130, 45));
        g2.fillRect(16, 32, w - 32, 28);
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
        g2.fillRoundRect(24, 36, 42, 10, 4, 4);
        g2.fillRoundRect(86, 36, 55, 10, 4, 4);
    }

    private void drawCabinetMarioCacheBg(Graphics2D g2, int w, int h, Color c) {
        g2.setColor(new Color(140, 30, 20, 35));
        int brickW = 28;
        int brickH = 10;
        for (int row = 0; row < 5; row++) {
            int y = 32 + row * brickH;
            int offset = (row % 2 == 0) ? 0 : brickW / 2;
            for (int col = 0; col < w / brickW + 2; col++) {
                int x = col * brickW + offset;
                g2.drawRect(x, y, brickW, brickH);
            }
        }
        g2.setColor(new Color(255, 200, 30, 60));
        g2.fillRoundRect(w - 44, 38, 16, 16, 4, 4);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        g2.drawString("?", w - 39, 50);
    }

    private void drawCabinetSnakeBg(Graphics2D g2, int w, int h, Color c) {
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 25));
        int gridSz = 16;
        for (int x = 16; x < w - 16; x += gridSz) {
            g2.drawLine(x, 32, x, 120);
        }
        for (int y = 32; y < 120; y += gridSz) {
            g2.drawLine(16, y, w - 16, y);
        }
    }

    private void drawCabinetCraneBg(Graphics2D g2, int w, int h, Color c) {
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
        g2.drawLine(16, 35, w - 16, 35);
        g2.drawLine(16, 43, w - 16, 43);
        for (int x = 16; x < w - 16; x += 18) {
            g2.drawLine(x, 35, x + 9, 43);
            g2.drawLine(x + 9, 43, x + 18, 35);
        }
    }

    private void drawCabinetPacmanBg(Graphics2D g2, int w, int h, Color c) {
        int cx = w / 2;
        int cy = 68;
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 35));
        g2.drawOval(cx - 48, cy - 48, 96, 96);
        g2.drawOval(cx - 30, cy - 30, 60, 60);
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            int px = cx + (int) (Math.cos(angle) * 38);
            int py = cy + (int) (Math.sin(angle) * 38);
            g2.fillOval(px - 2, py - 2, 4, 4);
        }
    }

    private void drawCabinetOvercookedBg(Graphics2D g2, int w, int h, Color c) {
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 40));
        int panW = 34;
        for (int i = 0; i < 4; i++) {
            int px = 20 + i * (panW + 12);
            g2.fillRoundRect(px, 34, panW, 16, 4, 4);
        }
    }

    private void selectAndEngageMission(int modIdx) {
        playTone(600, 30);
        switchView(VIEW_WORKSHEET);
        loadMission(modIdx);
    }

    // =========================================================================
    // SCREEN 2: INTERACTIVE ARCADE QUIZ ARENA (DUAL-PANE BATTLEFIELD)
    // =========================================================================
    private JPanel createWorksheetScreen() {
        JPanel arena = new JPanel(new BorderLayout(0, 10));
        arena.setOpaque(false);
        arena.setBorder(BorderFactory.createEmptyBorder(10, 16, 12, 16));

        // 1. Top Bar with < MISSION SELECT, Hearts & Timer
        arena.add(createWorksheetTopBar(), BorderLayout.NORTH);

        // 2. Center Battlefield
        arena.add(createCenterBattlefield(), BorderLayout.CENTER);

        // 3. Bottom Status Bar
        arena.add(createBottomStatusBar(), BorderLayout.SOUTH);

        return arena;
    }

    private JLabel createStatusCapsule(String text, Color fg, Color bg) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Consolas", Font.BOLD, 11));
        l.setForeground(fg);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 120), 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        return l;
    }

    private JPanel createWorksheetTopBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(15, 11, 26, 235));
                g2.fillRoundRect(0, 0, w, h, 10, 10);
                g2.setColor(new Color(45, 32, 68));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                Color activeC = MOD_COLORS[currentModuleIdx];
                g2.setColor(new Color(activeC.getRed(), activeC.getGreen(), activeC.getBlue(), 120));
                g2.drawLine(10, h - 1, w - 10, h - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        // Left button: Return to Stage Select
        JButton btnStageSelect = RetroTheme.createRetroButton("< STAGE SELECT", new Color(24, 18, 44), RetroTheme.ACCENT_CYAN);
        btnStageSelect.setFont(new Font("Consolas", Font.BOLD, 11));
        btnStageSelect.addActionListener(e -> {
            playTone(450, 20);
            countdownTimer.stop();
            switchView(VIEW_STAGE_SELECT);
        });
        bar.add(btnStageSelect, BorderLayout.WEST);

        // Center Title Badge
        JLabel titleTag = new JLabel("❖ ARCADE QUIZ ARENA // INTERACTIVE COMBAT HUD ❖", SwingConstants.CENTER);
        titleTag.setFont(new Font("Consolas", Font.BOLD, 12));
        titleTag.setForeground(RetroTheme.ACCENT_PINK);
        bar.add(titleTag, BorderLayout.CENTER);

        // Right: Hearts, Timer, Streak, XP Score & Audio
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        lblHearts = createStatusCapsule("LIVES: ♥ ♥ ♥", RetroTheme.ACCENT_PINK, new Color(45, 16, 28));
        right.add(lblHearts);

        lblTimer = createStatusCapsule("⏱ 01:30", RetroTheme.ACCENT_CYAN, new Color(16, 32, 45));
        right.add(lblTimer);

        lblStreak = createStatusCapsule("🔥 0x STREAK", RetroTheme.TEXT_DIM, new Color(38, 28, 20));
        right.add(lblStreak);

        lblScore = createStatusCapsule("XP: 000,000", RetroTheme.ACCENT_YELLOW, new Color(42, 35, 16));
        right.add(lblScore);

        JButton btnSound = RetroTheme.createRetroButton("SOUND: ON", new Color(24, 18, 44), RetroTheme.ACCENT_CYAN);
        btnSound.setFont(new Font("Consolas", Font.BOLD, 10));
        btnSound.addActionListener(e -> {
            soundEnabled = !soundEnabled;
            btnSound.setText(soundEnabled ? "SOUND: ON" : "SOUND: OFF");
            btnSound.setForeground(soundEnabled ? RetroTheme.ACCENT_CYAN : RetroTheme.TEXT_DIM);
            if (soundEnabled) playTone(700, 20);
        });
        right.add(btnSound);

        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createCenterBattlefield() {
        JPanel battlefield = new JPanel(new BorderLayout(0, 8));
        battlefield.setOpaque(false);

        // 1. Top Cartridge Switcher Ribbon (Direct 1-click stage switching)
        battlefield.add(createCartridgeRibbon(), BorderLayout.NORTH);

        // 2. Dual-Pane Combat Battlefield (Left: Intel Matrix + Schematic | Right: Interactive Arcade Quiz HUD)
        JPanel dualPane = new JPanel(new GridLayout(1, 2, 14, 0));
        dualPane.setOpaque(false);

        dualPane.add(createLeftWorksheetPane());
        dualPane.add(createRightQuizCombatPane());

        battlefield.add(dualPane, BorderLayout.CENTER);
        return battlefield;
    }

    private JPanel createCartridgeRibbon() {
        JPanel ribbon = new JPanel(new BorderLayout(10, 0));
        ribbon.setOpaque(false);

        JPanel modRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        modRow.setOpaque(false);

        JLabel lblTag = new JLabel("MISSION: ");
        lblTag.setFont(new Font("Consolas", Font.BOLD, 10));
        lblTag.setForeground(RetroTheme.TEXT_DIM);
        modRow.add(lblTag);

        for (int i = 0; i < 8; i++) {
            final int idx = i;
            JButton btn = new JButton(MOD_TITLES[i]) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    boolean isCur = (idx == currentModuleIdx);
                    Color themeC = MOD_COLORS[idx];

                    if (isCur) {
                        g2.setColor(new Color(themeC.getRed() / 3, themeC.getGreen() / 3, themeC.getBlue() / 3));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                        g2.setColor(themeC);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                    } else {
                        g2.setColor(new Color(20, 15, 34));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                        g2.setColor(new Color(50, 40, 75));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                    }

                    if (clearedMissions[idx]) {
                        g2.setColor(RetroTheme.ACCENT_GREEN);
                        g2.fillOval(5, getHeight() / 2 - 3, 6, 6);
                    }

                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(new Font("Consolas", Font.BOLD, 10));
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            btn.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 8));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                playTone(520, 20);
                loadMission(idx);
            });
            modButtons[i] = btn;
            modRow.add(btn);
        }
        updateModuleButtons();
        ribbon.add(modRow, BorderLayout.WEST);

        JLabel hint = new JLabel("CLICK ANY MISSION TAB TO JUMP DIRECTLY >>");
        hint.setFont(new Font("Consolas", Font.BOLD, 10));
        hint.setForeground(RetroTheme.TEXT_DIM);
        ribbon.add(hint, BorderLayout.EAST);

        return ribbon;
    }

    // =========================================================================
    // LEFT COLUMN: MISSION INTEL & REFERENCE SCHEMATIC
    // =========================================================================
    private JPanel createLeftWorksheetPane() {
        JPanel leftPane = new JPanel(new BorderLayout(0, 8));
        leftPane.setOpaque(false);

        // 1. Mission Stage Header Card (Tactical Cyber Dossier)
        JPanel missionCard = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(new Color(16, 12, 30, 240));
                g2.fillRoundRect(0, 0, w, h, 12, 12);

                Color activeC = MOD_COLORS[currentModuleIdx];
                g2.setColor(new Color(activeC.getRed(), activeC.getGreen(), activeC.getBlue(), 120));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

                // Corner Cyber HUD Brackets
                g2.setColor(activeC);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawLine(2, 8, 2, 2);
                g2.drawLine(2, 2, 8, 2);
                g2.drawLine(w - 3, 8, w - 3, 2);
                g2.drawLine(w - 3, 2, w - 9, 2);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        missionCard.setOpaque(false);
        missionCard.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // Mascot Pedestal Container
        JPanel avatarBox = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color activeC = MOD_COLORS[currentModuleIdx];
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(activeC.getRed(), activeC.getGreen(), activeC.getBlue(), 35));
                g2.fillOval(4, 4, w - 8, h - 8);
                g2.setColor(new Color(activeC.getRed(), activeC.getGreen(), activeC.getBlue(), 140));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(4, 4, w - 9, h - 9);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatarBox.setOpaque(false);
        avatarBox.setPreferredSize(new Dimension(54, 54));

        lblMascotAvatar = new JLabel("", SwingConstants.CENTER);
        avatarBox.add(lblMascotAvatar, BorderLayout.CENTER);
        missionCard.add(avatarBox, BorderLayout.WEST);

        // Center Info Stack
        JPanel infoBox = new JPanel(new GridLayout(4, 1, 0, 2));
        infoBox.setOpaque(false);

        lblModuleTitle = new JLabel("MISSION 01: CPU SCHEDULING (FCFS)");
        lblModuleTitle.setFont(new Font("Consolas", Font.BOLD, 13));
        lblModuleTitle.setForeground(MOD_COLORS[0]);
        infoBox.add(lblModuleTitle);

        lblGameSubtitle = new JLabel("[ SPACE INVADERS // CPU DISPATCH ARENA ]");
        lblGameSubtitle.setFont(new Font("Consolas", Font.BOLD, 10));
        lblGameSubtitle.setForeground(RetroTheme.ACCENT_YELLOW);
        infoBox.add(lblGameSubtitle);

        lblDifficultyBadge = new JLabel("DIFFICULTY: [EASY] (Lv. 2/5)  *  REWARD: +150 XP");
        lblDifficultyBadge.setFont(new Font("Consolas", Font.BOLD, 10));
        lblDifficultyBadge.setForeground(RetroTheme.ACCENT_GREEN);
        infoBox.add(lblDifficultyBadge);

        lblInstruction = new JLabel("Alien interceptors are entering the CPU pipeline! Calculate Start, Finish, Wait, and Turnaround times.");
        lblInstruction.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblInstruction.setForeground(new Color(225, 230, 245));
        infoBox.add(lblInstruction);

        missionCard.add(infoBox, BorderLayout.CENTER);
        leftPane.add(missionCard, BorderLayout.NORTH);

        // Center stack: Reference Scenario Table Card + Live Schematic Canvas
        JPanel centerStack = new JPanel(new BorderLayout(0, 8));
        centerStack.setOpaque(false);

        // 2. Reference Scenario Table Card (Clean read-only problem matrix)
        JPanel tableCard = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(14, 10, 26, 240));
                g2.fillRoundRect(0, 0, w, h, 10, 10);
                g2.setColor(new Color(50, 38, 75));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tableCard.setOpaque(false);
        tableCard.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JLabel tblHeader = new JLabel("❖ SCENARIO PROBLEM MATRIX // TELEMETRY PARAMETERS");
        tblHeader.setFont(new Font("Consolas", Font.BOLD, 10));
        tblHeader.setForeground(RetroTheme.ACCENT_CYAN);
        tableCard.add(tblHeader, BorderLayout.NORTH);

        scenarioTableModel = new DefaultTableModel();
        scenarioTable = new JTable(scenarioTableModel) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // Clean read-only reference
            }
        };
        scenarioTable.setFont(new Font("Consolas", Font.PLAIN, 11));
        scenarioTable.setRowHeight(26);
        scenarioTable.setBackground(new Color(16, 12, 28));
        scenarioTable.setForeground(Color.WHITE);
        scenarioTable.setGridColor(new Color(45, 34, 70));
        scenarioTable.setShowGrid(true);

        scenarioTable.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFocus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBackground(new Color(24, 17, 44));
                l.setForeground(MOD_COLORS[currentModuleIdx]);
                l.setFont(new Font("Consolas", Font.BOLD, 10));
                l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(60, 44, 90)));
                return l;
            }
        });

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        scenarioTable.setDefaultRenderer(Object.class, centerRenderer);

        JScrollPane scrollTbl = new JScrollPane(scenarioTable);
        scrollTbl.getViewport().setBackground(new Color(16, 12, 28));
        scrollTbl.setBorder(BorderFactory.createLineBorder(new Color(45, 34, 70), 1));
        scrollTbl.setPreferredSize(new Dimension(500, 130));
        tableCard.add(scrollTbl, BorderLayout.CENTER);

        centerStack.add(tableCard, BorderLayout.CENTER);

        // 3. Mini Schematic Pipeline Panel
        schematicPanel = new MiniAlgorithmSchematicPanel();
        centerStack.add(schematicPanel, BorderLayout.SOUTH);

        leftPane.add(centerStack, BorderLayout.CENTER);

        // 4. Quick Minigame Launcher Button
        JPanel btmBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btmBar.setOpaque(false);

        JButton btnPlayGame = RetroTheme.createRetroButton("🕹 LAUNCH RETRO MINIGAME (HANDS-ON PRACTICE)", new Color(60, 30, 20), RetroTheme.ACCENT_YELLOW);
        btnPlayGame.setFont(new Font("Consolas", Font.BOLD, 11));
        btnPlayGame.setPreferredSize(new Dimension(460, 32));
        btnPlayGame.setToolTipText("Launch the playable arcade minigame corresponding to this algorithm!");
        btnPlayGame.addActionListener(e -> launchActiveMinigame());
        btmBar.add(btnPlayGame);

        leftPane.add(btmBar, BorderLayout.SOUTH);
        return leftPane;
    }

    // =========================================================================
    // RIGHT COLUMN: INTERACTIVE ARCADE QUIZ COMBAT HUD
    // =========================================================================
    private JPanel createRightQuizCombatPane() {
        rightPaneContainer = new JPanel(new CardLayout());
        rightPaneContainer.setOpaque(false);

        // Card 1: Active Quiz Combat Card
        pnlQuizCombatCard = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(new Color(15, 11, 28, 245));
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                Color activeC = MOD_COLORS[currentModuleIdx];
                g2.setColor(new Color(activeC.getRed(), activeC.getGreen(), activeC.getBlue(), 120));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

                // Corner Cyber HUD Brackets
                g2.setColor(activeC);
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawLine(3, 10, 3, 3);
                g2.drawLine(3, 3, 10, 3);
                g2.drawLine(w - 4, 10, w - 4, 3);
                g2.drawLine(w - 4, 3, w - 11, 3);
                g2.drawLine(3, h - 11, 3, h - 4);
                g2.drawLine(3, h - 4, 10, h - 4);
                g2.drawLine(w - 4, h - 11, w - 4, h - 4);
                g2.drawLine(w - 4, h - 4, w - 11, h - 4);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlQuizCombatCard.setOpaque(false);
        pnlQuizCombatCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // Top Question Tracker Bar
        JPanel topTracker = new JPanel(new BorderLayout());
        topTracker.setOpaque(false);

        lblQuizHeaderTag = new JLabel("❖ QUESTION 1 OF 4 ❖");
        lblQuizHeaderTag.setFont(new Font("Consolas", Font.BOLD, 12));
        lblQuizHeaderTag.setForeground(RetroTheme.ACCENT_CYAN);
        topTracker.add(lblQuizHeaderTag, BorderLayout.WEST);

        // Progress Dots Tracker
        pnlDotsTracker = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 0; i < 4; i++) {
                    int x = 10 + i * 18;
                    int y = 4;
                    if (i < currentQuestionIdx) {
                        g2.setColor(RetroTheme.ACCENT_GREEN); // Answered
                    } else if (i == currentQuestionIdx) {
                        g2.setColor(RetroTheme.ACCENT_CYAN); // Current
                    } else {
                        g2.setColor(new Color(50, 40, 75)); // Upcoming
                    }
                    g2.fillOval(x, y, 10, 10);
                    g2.setColor(Color.WHITE);
                    g2.drawOval(x, y, 10, 10);
                }
                g2.dispose();
            }
        };
        pnlDotsTracker.setOpaque(false);
        pnlDotsTracker.setPreferredSize(new Dimension(100, 18));
        topTracker.add(pnlDotsTracker, BorderLayout.CENTER);

        lblRewardTag = new JLabel("REWARD: +100 XP");
        lblRewardTag.setFont(new Font("Consolas", Font.BOLD, 11));
        lblRewardTag.setForeground(RetroTheme.ACCENT_YELLOW);
        lblRewardTag.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        topTracker.add(lblRewardTag, BorderLayout.EAST);

        pnlQuizCombatCard.add(topTracker, BorderLayout.NORTH);

        // Keyboard Shortcuts A, B, C, D, 1, 2, 3, 4, Space, Enter for tactile arcade controls
        InputMap im = pnlQuizCombatCard.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = pnlQuizCombatCard.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "OPT_A");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_1, 0), "OPT_A");
        am.put("OPT_A", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { onOptionClicked(0); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, 0), "OPT_B");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_2, 0), "OPT_B");
        am.put("OPT_B", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { onOptionClicked(1); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "OPT_C");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_3, 0), "OPT_C");
        am.put("OPT_C", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { onOptionClicked(2); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "OPT_D");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_4, 0), "OPT_D");
        am.put("OPT_D", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { onOptionClicked(3); } });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "NEXT_Q");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "NEXT_Q");
        am.put("NEXT_Q", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnNextQuestion != null && btnNextQuestion.isEnabled()) {
                    nextQuestion();
                }
            }
        });

        // Question Prompt & 4 Arcade Buttons Stack
        JPanel qCenter = new JPanel(new BorderLayout(0, 10));
        qCenter.setOpaque(false);

        // Question Prompt Card
        JPanel promptBox = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(24, 18, 44));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(65, 50, 95));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        promptBox.setOpaque(false);
        promptBox.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        promptBox.setPreferredSize(new Dimension(500, 64));

        txtQuestionPrompt = new JTextArea("Question prompt loading...");
        txtQuestionPrompt.setFont(new Font("Consolas", Font.BOLD, 12));
        txtQuestionPrompt.setRows(3);
        txtQuestionPrompt.setForeground(Color.WHITE);
        txtQuestionPrompt.setOpaque(false);
        txtQuestionPrompt.setEditable(false);
        txtQuestionPrompt.setLineWrap(true);
        txtQuestionPrompt.setWrapStyleWord(true);
        promptBox.add(txtQuestionPrompt, BorderLayout.CENTER);

        qCenter.add(promptBox, BorderLayout.NORTH);

        // 4 Tactile Arcade Choice Buttons Stack
        JPanel optionsStack = new JPanel(new GridLayout(4, 1, 0, 8));
        optionsStack.setOpaque(false);

        String[] keyTags = {"[ A ]", "[ B ]", "[ C ]", "[ D ]"};
        for (int i = 0; i < 4; i++) {
            final int optIdx = i;
            JButton btn = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth();
                    int h = getHeight();

                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, w, h, 8, 8);

                    // Button Border
                    if (questionAnswered) {
                        ArcadeMCQ q = getCurrentMCQ();
                        if (optIdx == q.correctIndex) {
                            g2.setColor(RetroTheme.ACCENT_GREEN);
                            g2.setStroke(new BasicStroke(2.0f));
                        } else if (optIdx == selectedOptionIdx) {
                            g2.setColor(Color.RED);
                            g2.setStroke(new BasicStroke(2.0f));
                        } else {
                            g2.setColor(new Color(55, 42, 80));
                        }
                    } else {
                        g2.setColor(new Color(65, 50, 95));
                    }
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(new Font("Consolas", Font.BOLD, 12));
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(24, 18, 44));
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> onOptionClicked(optIdx));

            btnOptions[i] = btn;
            optionsStack.add(btn);
        }

        qCenter.add(optionsStack, BorderLayout.CENTER);

        // Explanation & Intel Card (Hidden initially, expands on answer)
        pnlExplanationCard = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(18, 14, 34));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(60, 45, 90));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlExplanationCard.setOpaque(false);
        pnlExplanationCard.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        pnlExplanationCard.setPreferredSize(new Dimension(500, 85));
        pnlExplanationCard.setVisible(false);

        lblExplanationBanner = new JLabel("✔ CORRECT! +100 XP");
        lblExplanationBanner.setFont(new Font("Consolas", Font.BOLD, 12));
        lblExplanationBanner.setForeground(RetroTheme.ACCENT_GREEN);
        pnlExplanationCard.add(lblExplanationBanner, BorderLayout.NORTH);

        txtExplanationBody = new JTextArea();
        txtExplanationBody.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtExplanationBody.setForeground(new Color(225, 230, 245));
        txtExplanationBody.setOpaque(false);
        txtExplanationBody.setEditable(false);
        txtExplanationBody.setLineWrap(true);
        txtExplanationBody.setWrapStyleWord(true);
        pnlExplanationCard.add(txtExplanationBody, BorderLayout.CENTER);

        lblFormulaPill = new JLabel("💡 FORMULA: --");
        lblFormulaPill.setFont(new Font("Consolas", Font.BOLD, 10));
        lblFormulaPill.setForeground(RetroTheme.ACCENT_YELLOW);
        pnlExplanationCard.add(lblFormulaPill, BorderLayout.SOUTH);

        qCenter.add(pnlExplanationCard, BorderLayout.SOUTH);

        pnlQuizCombatCard.add(qCenter, BorderLayout.CENTER);

        // Bottom Controls: Formula Hint & Next Question
        JPanel btmControls = new JPanel(new BorderLayout(10, 0));
        btmControls.setOpaque(false);

        btnFormulaHint = RetroTheme.createRetroButton("💡 TOGGLE FORMULA HINT", new Color(38, 28, 55), RetroTheme.ACCENT_YELLOW);
        btnFormulaHint.setFont(new Font("Consolas", Font.BOLD, 11));
        btnFormulaHint.addActionListener(e -> {
            playTone(520, 20);
            ArcadeMCQ q = getCurrentMCQ();
            if (q != null) {
                JOptionPane.showMessageDialog(this,
                        q.formulaHint,
                        "ALGORITHM INTEL // " + q.category,
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
        btmControls.add(btnFormulaHint, BorderLayout.WEST);

        btnNextQuestion = RetroTheme.createRetroButton("NEXT QUESTION >>", new Color(15, 60, 40), RetroTheme.ACCENT_GREEN);
        btnNextQuestion.setFont(new Font("Consolas", Font.BOLD, 12));
        btnNextQuestion.setPreferredSize(new Dimension(200, 34));
        btnNextQuestion.setEnabled(false);
        btnNextQuestion.addActionListener(e -> nextQuestion());
        btmControls.add(btnNextQuestion, BorderLayout.EAST);

        pnlQuizCombatCard.add(btmControls, BorderLayout.SOUTH);

        rightPaneContainer.add(pnlQuizCombatCard, "CARD_COMBAT");

        // Card 2: S-Rank Victory Screen Card
        pnlVictoryCard = createVictoryCard();
        rightPaneContainer.add(pnlVictoryCard, "CARD_VICTORY");

        return rightPaneContainer;
    }

    private JPanel createVictoryCard() {
        JPanel p = new JPanel(new BorderLayout(0, 14)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(new Color(15, 11, 28, 250));
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                Color activeC = MOD_COLORS[currentModuleIdx];
                g2.setColor(new Color(activeC.getRed(), activeC.getGreen(), activeC.getBlue(), 160));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel banner = new JLabel("★ MISSION CLEARED! S-RANK PILOT ★", SwingConstants.CENTER);
        banner.setFont(new Font("Consolas", Font.BOLD, 18));
        banner.setForeground(RetroTheme.ACCENT_YELLOW);
        p.add(banner, BorderLayout.NORTH);

        JPanel centerInfo = new JPanel(new GridLayout(3, 1, 0, 10));
        centerInfo.setOpaque(false);

        lblVictoryRank = new JLabel("FINAL RANK: S-RANK (PERFECT CLEAR)", SwingConstants.CENTER);
        lblVictoryRank.setFont(new Font("Consolas", Font.BOLD, 15));
        lblVictoryRank.setForeground(RetroTheme.ACCENT_GREEN);
        centerInfo.add(lblVictoryRank);

        lblVictoryScore = new JLabel("SCORE REWARD: +400 XP", SwingConstants.CENTER);
        lblVictoryScore.setFont(new Font("Consolas", Font.BOLD, 14));
        lblVictoryScore.setForeground(RetroTheme.ACCENT_CYAN);
        centerInfo.add(lblVictoryScore);

        lblVictoryAccuracy = new JLabel("ACCURACY: 4 / 4 (100%)", SwingConstants.CENTER);
        lblVictoryAccuracy.setFont(new Font("Consolas", Font.BOLD, 13));
        lblVictoryAccuracy.setForeground(Color.WHITE);
        centerInfo.add(lblVictoryAccuracy);

        p.add(centerInfo, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        btnRow.setOpaque(false);

        JButton btnRetry = RetroTheme.createRetroButton("↺ RETRY MISSION", new Color(38, 28, 55), RetroTheme.ACCENT_CYAN);
        btnRetry.setFont(new Font("Consolas", Font.BOLD, 11));
        btnRetry.addActionListener(e -> {
            playTone(520, 20);
            loadMission(currentModuleIdx);
        });
        btnRow.add(btnRetry);

        JButton btnNextMod = RetroTheme.createRetroButton("NEXT MISSION >>", new Color(18, 55, 35), RetroTheme.ACCENT_GREEN);
        btnNextMod.setFont(new Font("Consolas", Font.BOLD, 11));
        btnNextMod.addActionListener(e -> {
            playTone(600, 20);
            int nextIdx = (currentModuleIdx + 1) % 8;
            loadMission(nextIdx);
        });
        btnRow.add(btnNextMod);

        JButton btnBackHub = RetroTheme.createRetroButton("< STAGE SELECT", RetroTheme.BTN_DARK, RetroTheme.ACCENT_PINK);
        btnBackHub.setFont(new Font("Consolas", Font.BOLD, 11));
        btnBackHub.addActionListener(e -> {
            playTone(450, 20);
            countdownTimer.stop();
            switchView(VIEW_STAGE_SELECT);
        });
        btnRow.add(btnBackHub);

        p.add(btnRow, BorderLayout.SOUTH);
        return p;
    }

    private JPanel createBottomStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(4, 6, 2, 6));

        JLabel lblFeedback = new JLabel("STATUS: Real-time Interactive Arcade Quiz active. Choose [A], [B], [C], or [D]!");
        lblFeedback.setFont(new Font("Consolas", Font.BOLD, 11));
        lblFeedback.setForeground(RetroTheme.TEXT_DIM);
        lblFeedback.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));
        bar.add(lblFeedback, BorderLayout.WEST);

        JLabel tipHint = new JLabel("HOTKEYS: A / B / C / D to answer | Space / Enter for Next");
        tipHint.setFont(new Font("Consolas", Font.PLAIN, 10));
        tipHint.setForeground(new Color(130, 130, 160));
        bar.add(tipHint, BorderLayout.EAST);

        return bar;
    }

    // =========================================================================
    // QUIZ GAMEPLAY ENGINE & MCQ STATE MACHINE
    // =========================================================================
    private ArcadeMCQ getCurrentMCQ() {
        List<ArcadeMCQ> list = MODULE_MCQS.get(currentModuleIdx);
        if (currentQuestionIdx >= 0 && currentQuestionIdx < list.size()) {
            return list.get(currentQuestionIdx);
        }
        return null;
    }

    public void loadMission(int modIdx) {
        this.currentModuleIdx = modIdx;
        this.currentQuestionIdx = 0;
        this.totalCorrectInMission = 0;
        this.playerHearts = 3;
        this.streakCount = 0;
        this.timeRemainingSeconds = 90;

        updateHeartsDisplay();
        updateScoreDisplay();
        updateTimerDisplay();
        updateModuleButtons();

        if (timeAttackEnabled && VIEW_WORKSHEET.equals(activeView)) {
            countdownTimer.restart();
        } else {
            countdownTimer.stop();
            if (!timeAttackEnabled) {
                lblTimer.setText("PRACTICE MODE");
                lblTimer.setForeground(RetroTheme.ACCENT_CYAN);
            }
        }

        // Update Dossier Header
        Color themeColor = MOD_COLORS[modIdx];
        lblModuleTitle.setText(String.format("MISSION %02d: %s", modIdx + 1, MOD_FULL_NAMES[modIdx]));
        lblModuleTitle.setForeground(themeColor);

        lblGameSubtitle.setText("[ " + MOD_GAMES[modIdx] + " // ARENA ]");
        lblGameSubtitle.setForeground(RetroTheme.ACCENT_YELLOW);

        lblDifficultyBadge.setText("DIFFICULTY: " + MOD_DIFFICULTIES[modIdx] + "  *  REWARD: +150 XP");
        lblInstruction.setText("Engage arcade questions! Study the problem matrix and algorithm pipeline on the left.");

        BufferedImage mascot = getMascotSprite(modIdx, 48);
        if (mascot != null) {
            lblMascotAvatar.setIcon(new ImageIcon(mascot));
        }

        // Load Scenario Table Data
        loadScenarioTable(modIdx);

        // Display First Question
        displayCurrentQuestion();

        CardLayout cl = (CardLayout) rightPaneContainer.getLayout();
        cl.show(rightPaneContainer, "CARD_COMBAT");

        revalidate();
        repaint();
    }

    private void loadScenarioTable(int modIdx) {
        String[] headers;
        String[][] data;

        switch (modIdx) {
            case 0:
                headers = new String[]{"ALIEN WAVE", "ARRIVAL (AT)", "BURST (BT)", "TARGET PRIO"};
                data = new String[][]{
                        {"W1 (Scout Alien)", "0.0 ms", "4.0 ms", "High (P1)"},
                        {"W2 (Fighter Alien)", "1.0 ms", "3.0 ms", "Medium (P2)"},
                        {"W3 (Mothership)", "2.0 ms", "2.0 ms", "Critical (P3)"}
                };
                break;
            case 1:
                headers = new String[]{"PROCESS", "ALLOCATION [A B C]", "MAX NEED [A B C]", "AVAILABLE"};
                data = new String[][]{
                        {"P0 (Interceptor)", "[0, 1, 0]", "[7, 5, 3]", "[3, 3, 2]"},
                        {"P1 (Bomber)", "[2, 0, 0]", "[3, 2, 2]", "Available Pool"},
                        {"P2 (Flagship)", "[3, 0, 2]", "[9, 0, 2]", "Safe Check"}
                };
                break;
            case 2:
                headers = new String[]{"PAGE #", "PHYSICAL FRAME #", "STATUS", "BASE ADDR (4KB)"};
                data = new String[][]{
                        {"Page 0", "Frame 5", "VALID", "0x5000 (20,480)"},
                        {"Page 1", "Frame 2", "VALID", "0x2000 (8,192)"},
                        {"Page 2", "Frame 7", "VALID", "0x7000 (28,672)"}
                };
                break;
            case 3:
                headers = new String[]{"CACHE SLOT", "RESIDENT PAGE", "ENTRY TIME", "FIFO STATUS"};
                data = new String[][]{
                        {"Slot 0 (Head)", "Page 1", "T = 0 (Oldest)", "Next Eviction Target"},
                        {"Slot 1", "Page 2", "T = 1", "Resident"},
                        {"Slot 2 (Tail)", "Page 3", "T = 2 (Newest)", "Resident"}
                };
                break;
            case 4:
                headers = new String[]{"SEGMENT #", "BASE ADDRESS", "LIMIT REGISTER", "ACCESS TYPE"};
                data = new String[][]{
                        {"Seg 0 (Code)", "1000", "400 bytes", "Read-Only (Valid < 400)"},
                        {"Seg 1 (Stack)", "2000", "500 bytes", "Read-Write (Valid < 500)"}
                };
                break;
            case 5:
                headers = new String[]{"HOLE #", "START ADDRESS", "HOLE SIZE", "FIRST-FIT EVAL (212 KB)"};
                data = new String[][]{
                        {"Hole 0", "0x0000", "100 KB", "Too small (100 < 212)"},
                        {"Hole 1", "0x0400", "500 KB", "ALLOCATED (500 >= 212)"},
                        {"Hole 2", "0x1200", "200 KB", "Skipped"},
                        {"Hole 3", "0x1A00", "300 KB", "Best-Fit candidate"},
                        {"Hole 4", "0x2400", "600 KB", "Worst-Fit candidate"}
                };
                break;
            case 6:
                headers = new String[]{"HOP #", "FROM CYLINDER", "TO CYLINDER", "SEEK DISTANCE"};
                data = new String[][]{
                        {"Hop 1", "53 (Start Head)", "98", "45 cylinders"},
                        {"Hop 2", "98", "183", "85 cylinders"},
                        {"Hop 3", "183", "37", "146 cylinders"},
                        {"Hop 4", "37", "122", "85 cylinders"}
                };
                break;
            case 7:
            default:
                headers = new String[]{"CHEF WORKLOAD", "CRITICAL SECTION", "MUTEX LOCK", "STATE"};
                data = new String[][]{
                        {"Chef Mario", "Burger Grill Station", "ACQUIRED", "Executing in Critical Section"},
                        {"Chef Luigi", "Burger Grill Station", "BLOCKED", "Waiting on kitchen_lock"}
                };
                break;
        }

        scenarioTableModel.setDataVector(data, headers);
    }

    private void displayCurrentQuestion() {
        ArcadeMCQ q = getCurrentMCQ();
        if (q == null) return;

        questionAnswered = false;
        selectedOptionIdx = -1;

        lblQuizHeaderTag.setText(String.format("❖ QUESTION %d OF 4 // %s ❖", currentQuestionIdx + 1, q.category));
        pnlDotsTracker.repaint();

        txtQuestionPrompt.setText(q.question);

        String[] keyTags = {"[ A ]  ", "[ B ]  ", "[ C ]  ", "[ D ]  "};
        for (int i = 0; i < 4; i++) {
            btnOptions[i].setText(keyTags[i] + q.options[i]);
            btnOptions[i].setEnabled(true);
            btnOptions[i].setBackground(new Color(24, 18, 44));
            btnOptions[i].setForeground(Color.WHITE);
        }

        pnlExplanationCard.setVisible(false);
        btnNextQuestion.setEnabled(false);
        btnNextQuestion.setText(currentQuestionIdx == 3 ? "FINISH MISSION >>" : "NEXT QUESTION >>");

        schematicPanel.repaint();
    }

    private void onOptionClicked(int optIdx) {
        if (questionAnswered) return;
        questionAnswered = true;
        selectedOptionIdx = optIdx;

        ArcadeMCQ q = getCurrentMCQ();
        if (q == null) return;

        // Disable all options from further clicks
        for (JButton b : btnOptions) {
            b.setEnabled(false);
        }

        boolean isCorrect = (optIdx == q.correctIndex);

        if (isCorrect) {
            playTone(880, 70); // Laser chime
            totalCorrectInMission++;
            streakCount++;
            int xpEarned = 100 + (streakCount * 25);
            playerScore += xpEarned;

            btnOptions[optIdx].setBackground(new Color(0, 180, 80)); // Bright Neon Green
            btnOptions[optIdx].setForeground(Color.BLACK);
            btnOptions[optIdx].setText("✔ " + btnOptions[optIdx].getText() + String.format("  [ +%d XP ]", xpEarned));

            lblExplanationBanner.setText(String.format("✔ CORRECT! +%d XP (STREAK: %dx!)", xpEarned, streakCount));
            lblExplanationBanner.setForeground(RetroTheme.ACCENT_GREEN);
        } else {
            playBuzzerSound(); // Buzzer
            streakCount = 0;
            playerHearts--;
            updateHeartsDisplay();

            btnOptions[optIdx].setBackground(new Color(200, 20, 50)); // Crimson Red
            btnOptions[optIdx].setForeground(Color.WHITE);
            btnOptions[optIdx].setText("✘ " + btnOptions[optIdx].getText());

            // Highlight the correct answer in gold
            btnOptions[q.correctIndex].setBackground(new Color(45, 38, 15));
            btnOptions[q.correctIndex].setForeground(RetroTheme.ACCENT_YELLOW);
            btnOptions[q.correctIndex].setText(btnOptions[q.correctIndex].getText() + " ★ (CORRECT ANSWER)");

            lblExplanationBanner.setText("✘ INCORRECT! -1 LIFE ♥");
            lblExplanationBanner.setForeground(Color.RED);
        }

        updateScoreDisplay();

        // Reveal Explanation
        txtExplanationBody.setText(q.explanation);
        lblFormulaPill.setText("💡 FORMULA: " + q.formulaHint);
        pnlExplanationCard.setVisible(true);

        btnNextQuestion.setEnabled(true);

        // Check if player died
        if (playerHearts <= 0) {
            countdownTimer.stop();
            handleGameOver();
        }
    }

    private void nextQuestion() {
        playTone(560, 25);
        if (currentQuestionIdx < 3) {
            currentQuestionIdx++;
            displayCurrentQuestion();
        } else {
            // Mission Completed!
            showMissionCleared();
        }
    }

    private void showMissionCleared() {
        countdownTimer.stop();
        playVictorySound();
        clearedMissions[currentModuleIdx] = true;
        updateModuleButtons();

        int bonus = (timeAttackEnabled && timeRemainingSeconds > 0) ? timeRemainingSeconds * 5 : 50;
        playerScore += bonus;
        updateScoreDisplay();

        String rank;
        if (totalCorrectInMission == 4 && playerHearts == 3) {
            rank = "FINAL RANK: S-RANK (PERFECT CLEAR)";
            lblVictoryRank.setForeground(RetroTheme.ACCENT_YELLOW);
        } else if (totalCorrectInMission >= 3) {
            rank = "FINAL RANK: A-RANK (EXCELLENT)";
            lblVictoryRank.setForeground(RetroTheme.ACCENT_GREEN);
        } else {
            rank = "FINAL RANK: B-RANK (CLEARED)";
            lblVictoryRank.setForeground(RetroTheme.ACCENT_CYAN);
        }

        lblVictoryRank.setText(rank);
        lblVictoryScore.setText(String.format("SCORE REWARD: +%d XP  (TIME BONUS: +%d XP)", totalCorrectInMission * 100, bonus));
        lblVictoryAccuracy.setText(String.format("ACCURACY: %d / 4 (%d%%)", totalCorrectInMission, (totalCorrectInMission * 100) / 4));

        CardLayout cl = (CardLayout) rightPaneContainer.getLayout();
        cl.show(rightPaneContainer, "CARD_VICTORY");
    }

    private void handleGameOver() {
        playBuzzerSound();
        int opt = JOptionPane.showConfirmDialog(this,
                "CRITICAL FAILURE: All 3 Lives Lost!\nWould you like to retry this Mission?",
                "MISSION FAILED",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            loadMission(currentModuleIdx);
        } else {
            switchView(VIEW_STAGE_SELECT);
        }
    }

    private void handleTimeout() {
        playBuzzerSound();
        int opt = JOptionPane.showConfirmDialog(this,
                "TIME EXPIRED: The 90-second survival clock reached zero!\nWould you like to retry this Mission?",
                "SURVIVAL TIMEOUT",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            loadMission(currentModuleIdx);
        } else {
            switchView(VIEW_STAGE_SELECT);
        }
    }

    private void updateModuleButtons() {
        for (int i = 0; i < 8; i++) {
            if (modButtons[i] != null) {
                modButtons[i].repaint();
            }
        }
    }

    private BufferedImage getMascotSprite(int modIdx, int size) {
        Color c = MOD_COLORS[modIdx];
        switch (modIdx) {
            case 0: return PixelSprites.createAlienSprite(size, c);
            case 1: return PixelSprites.createSpaceShootShip(size, c);
            case 2: return PixelSprites.createFrogSprite(size, c);
            case 3: return PixelSprites.createMarioSprite(size);
            case 4: return PixelSprites.createSnakeSprite(size, c);
            case 5: return PixelSprites.createCraneSprite(size, c);
            case 6: return PixelSprites.createPacman(size, 0.4f, true);
            case 7: return PixelSprites.createChefSprite(size, false);
            default: return null;
        }
    }

    private void launchActiveMinigame() {
        playTone(700, 30);
        try {
            switch (currentModuleIdx) {
                case 0:
                    new KernelTapperDialog(parentFrame, "FCFS", null).setVisible(true);
                    break;
                case 1:
                    new VaultPanicDialog(parentFrame, null, null, null).setVisible(true);
                    break;
                case 2:
                    new FrameHopperDialog(parentFrame, 4096, null, 8).setVisible(true);
                    break;
                case 3:
                    new MarioCacheRunnerDialog(parentFrame, null, 3, "FIFO").setVisible(true);
                    break;
                case 4:
                    game.snake.SnakeGameDialog.launchDefault(parentFrame, null);
                    break;
                case 5:
                    new DefragPanicDialog(parentFrame, null, null, null).setVisible(true);
                    break;
                case 6:
                    new PacmanDiskArcadeDialog(parentFrame, null, 53, 199, "FCFS").setVisible(true);
                    break;
                case 7:
                    new OvercookedKitchenDialog(parentFrame, null).setVisible(true);
                    break;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not launch minigame: " + ex.getMessage(),
                    "Arcade Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // MINI ALGORITHM SCHEMATIC PIPELINE CANVAS
    // =========================================================================
    private class MiniAlgorithmSchematicPanel extends JPanel {
        public MiniAlgorithmSchematicPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(500, 140));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(new Color(11, 7, 22, 240));
            g2.fillRoundRect(0, 0, w, h, 8, 8);
            Color activeC = MOD_COLORS[currentModuleIdx];
            g2.setColor(new Color(activeC.getRed(), activeC.getGreen(), activeC.getBlue(), 90));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

            // Title Tag
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(activeC);
            g2.drawString("● REAL-TIME ALGORITHM EXECUTION PIPELINE SCHEMATIC", 10, 15);

            int baseY = 24;
            int boxH = h - 34;
            int availW = w - 24;

            switch (currentModuleIdx) {
                case 0: drawGanttChart(g2, 12, baseY, availW, boxH); break;
                case 1: drawBankersPipeline(g2, 12, baseY, availW, boxH); break;
                case 2: drawPagingPipeline(g2, 12, baseY, availW, boxH); break;
                case 3: drawCachePipeline(g2, 12, baseY, availW, boxH); break;
                case 4: drawSegmentationPipeline(g2, 12, baseY, availW, boxH); break;
                case 5: drawAllocationPipeline(g2, 12, baseY, availW, boxH); break;
                case 6: drawDiskSeekPipeline(g2, 12, baseY, availW, boxH); break;
                case 7: drawConcurrencyPipeline(g2, 12, baseY, availW, boxH); break;
            }

            g2.dispose();
        }

        private void drawGanttChart(Graphics2D g2, int x, int y, int w, int h) {
            int w1 = (int) (w * (4.0 / 9.0));
            int w2 = (int) (w * (3.0 / 9.0));
            int w3 = w - w1 - w2;
            int blockH = 36;
            int blockY = y + 8;

            g2.setColor(new Color(15, 11, 26));
            g2.fillRoundRect(x, blockY - 2, w, blockH + 4, 8, 8);
            g2.setColor(new Color(45, 32, 68));
            g2.drawRoundRect(x, blockY - 2, w, blockH + 4, 8, 8);

            drawCyberGanttBlock(g2, x + 2, blockY, w1 - 4, blockH,
                    new Color(5, 217, 232), new Color(0, 105, 135), "W1: SCOUT (4ms)", 0);
            drawCyberGanttBlock(g2, x + w1 + 2, blockY, w2 - 4, blockH,
                    new Color(255, 185, 30), new Color(155, 100, 10), "W2: FIGHTER (3ms)", 1);
            drawCyberGanttBlock(g2, x + w1 + w2 + 2, blockY, w3 - 4, blockH,
                    new Color(255, 42, 109), new Color(145, 15, 55), "W3: MOTHER (2ms)", 2);

            int tickY = blockY + blockH + 16;
            drawTimelineTick(g2, x + 2, tickY, "● 0.0ms");
            drawTimelineTick(g2, x + w1, tickY, "● 4.0ms");
            drawTimelineTick(g2, x + w1 + w2, tickY, "● 7.0ms");
            drawTimelineTick(g2, x + w - 4, tickY, "● 9.0ms");
        }

        private void drawCyberGanttBlock(Graphics2D g2, int bx, int by, int bw, int bh, Color cTop, Color cBot, String label, int alienType) {
            if (bw <= 0) return;
            GradientPaint gp = new GradientPaint(bx, by, cTop, bx, by + bh, cBot);
            g2.setPaint(gp);
            g2.fillRoundRect(bx, by, bw, bh, 6, 6);

            g2.setColor(new Color(255, 255, 255, 18));
            for (int scan = by; scan < by + bh; scan += 3) {
                g2.drawLine(bx, scan, bx + bw, scan);
            }

            g2.setColor(new Color(255, 255, 255, 140));
            g2.drawLine(bx + 2, by + 1, bx + bw - 3, by + 1);

            g2.setColor(new Color(cTop.getRed(), cTop.getGreen(), cTop.getBlue(), 220));
            g2.drawRoundRect(bx, by, bw - 1, bh - 1, 6, 6);

            BufferedImage sprite = (alienType == 0) ? PixelSprites.createAlienSprite(14, Color.WHITE) :
                    (alienType == 1) ? PixelSprites.createAlienSprite(14, Color.BLACK) :
                            PixelSprites.createAlienSprite(14, Color.WHITE);
            int textX = bx + 8;
            if (sprite != null && bw > 70) {
                g2.drawImage(sprite, bx + 6, by + (bh - 14) / 2, null);
                textX += 18;
            }

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            if (fm.stringWidth(label) < bw - (textX - bx)) {
                g2.drawString(label, textX, by + bh / 2 + 4);
            }
        }

        private void drawTimelineTick(Graphics2D g2, int tx, int ty, String text) {
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            FontMetrics fm = g2.getFontMetrics();
            int sw = fm.stringWidth(text);
            int px = Math.max(4, tx - sw / 2);

            g2.setColor(new Color(5, 217, 232));
            g2.drawString(text, px, ty);
        }

        private void drawBankersPipeline(Graphics2D g2, int x, int y, int w, int h) {
            int boxW = (w - 60) / 4;
            int by = y + 10;
            drawStepBox(g2, x + 6, by, boxW, 36, "ALLOCATION", "P1: [2, 0, 0]", RetroTheme.ACCENT_CYAN);
            drawArrow(g2, x + 6 + boxW + 4, by + 18, 14);

            drawStepBox(g2, x + 24 + boxW, by, boxW, 36, "NEED VECTOR", "P1: [1, 2, 2]", RetroTheme.ACCENT_PINK);
            drawArrow(g2, x + 24 + boxW * 2 + 4, by + 18, 14);

            drawStepBox(g2, x + 42 + boxW * 2, by, boxW, 36, "AVAILABLE", "[3, 3, 2]", RetroTheme.ACCENT_YELLOW);
            drawArrow(g2, x + 42 + boxW * 3 + 4, by + 18, 14);

            drawStepBox(g2, x + 60 + boxW * 3, by, boxW, 36, "SAFE SEQ", "P1 -> P0 -> P2", RetroTheme.ACCENT_GREEN);

            g2.setColor(new Color(255, 211, 25));
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.drawString("★ SAFETY CHECK: Need [1,2,2] <= Available [3,3,2] -> P1 FINISHES -> New Work = [5,3,2]!", x + 10, by + 58);
        }

        private void drawPagingPipeline(Graphics2D g2, int x, int y, int w, int h) {
            int boxW = (w - 70) / 3;
            int by = y + 10;
            drawStepBox(g2, x + 10, by, boxW, 36, "LOGICAL ADDR", "5120 (4KB Page)", RetroTheme.ACCENT_CYAN);
            drawArrow(g2, x + 10 + boxW + 4, by + 18, 16);

            drawStepBox(g2, x + 32 + boxW, by, boxW + 10, 36, "PAGE # / OFFSET", "p = 1, d = 1024", RetroTheme.ACCENT_YELLOW);
            drawArrow(g2, x + 42 + boxW * 2 + 14, by + 18, 16);

            drawStepBox(g2, x + 64 + boxW * 2, by, boxW, 36, "PHYSICAL ADDR", "Frame 2 -> 9216", RetroTheme.ACCENT_GREEN);

            g2.setColor(new Color(40, 255, 200));
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.drawString("★ MMU TRANSLATION: Page 1 -> Frame 2 | Physical = (2 * 4096) + 1024 = 9216 bytes", x + 12, by + 58);
        }

        private void drawCachePipeline(Graphics2D g2, int x, int y, int w, int h) {
            int slotW = 76;
            int slotH = 34;
            int sx = x + 18;
            int sy = y + 10;

            for (int i = 0; i < 3; i++) {
                g2.setColor(new Color(25, 18, 38));
                g2.fillRoundRect(sx + i * (slotW + 14), sy, slotW, slotH, 6, 6);
                g2.setColor(new Color(255, 115, 115));
                g2.drawRoundRect(sx + i * (slotW + 14), sy, slotW, slotH, 6, 6);

                g2.setFont(new Font("Consolas", Font.BOLD, 9));
                g2.setColor(Color.WHITE);
                g2.drawString("SLOT " + i, sx + i * (slotW + 14) + 8, sy + 14);
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                String val = (i == 0) ? "[ P4 ] EVICT" : (i == 1) ? "[ P2 ] HIT" : "[ P3 ] OK";
                g2.drawString(val, sx + i * (slotW + 14) + 8, sy + 28);
            }

            g2.setColor(new Color(255, 115, 115));
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.drawString("★ FIFO CACHE EVICTION: Page 4 arrives -> Evicts oldest resident Page 1 from Slot 0!", sx, sy + 58);
        }

        private void drawSegmentationPipeline(Graphics2D g2, int x, int y, int w, int h) {
            g2.setColor(new Color(90, 245, 140));
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.drawString("SEGMENTATION MEMORY MAP (Base Register + Offset Bound Checks)", x + 12, y + 18);

            int by = y + 26;
            drawStepBox(g2, x + 12, by, 175, 34, "SEG 0: CODE", "Base: 1000 | Limit: 400", RetroTheme.ACCENT_GREEN);
            drawStepBox(g2, x + 200, by, 175, 34, "SEG 1: STACK", "Base: 2000 | Limit: 500", RetroTheme.ACCENT_CYAN);

            g2.setColor(Color.WHITE);
            g2.drawString("OFFSET EVAL: Seg 0 (150 < 400) -> VALID (1150) | Seg 1 (600 >= 500) -> TRAP! (SEGFAULT)", x + 12, y + 78);
        }

        private void drawAllocationPipeline(Graphics2D g2, int x, int y, int w, int h) {
            g2.setColor(new Color(215, 145, 255));
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.drawString("DYNAMIC PARTITIONING HOLES: [ 100KB | 500KB (ALLOC) | 200KB | 300KB | 600KB ]", x + 12, y + 18);

            int by = y + 28;
            int pw = (w - 30) / 5;
            for (int i = 0; i < 5; i++) {
                boolean target = (i == 1);
                g2.setColor(target ? new Color(60, 25, 80) : new Color(20, 15, 32));
                g2.fillRoundRect(x + 12 + i * pw, by, pw - 4, 32, 6, 6);
                g2.setColor(target ? RetroTheme.ACCENT_PINK : new Color(60, 45, 85));
                g2.drawRoundRect(x + 12 + i * pw, by, pw - 4, 32, 6, 6);
            }
            g2.setColor(Color.WHITE);
            g2.drawString("★ FIRST-FIT: 212 KB job fits into Hole 1 (500 KB) leaving a 288 KB remainder fragment.", x + 12, y + 78);
        }

        private void drawDiskSeekPipeline(Graphics2D g2, int x, int y, int w, int h) {
            g2.setColor(new Color(255, 230, 50));
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.drawString("PAC-MAN DISK HEAD TRAVERSAL (FCFS Seek: 53 -> 98 -> 183 -> 37 -> 122 -> 14)", x + 12, y + 18);

            int lineY = y + 42;
            g2.setColor(new Color(70, 55, 100));
            g2.drawLine(x + 20, lineY, x + w - 20, lineY);

            int[] cylinders = {14, 37, 53, 98, 122, 183};
            for (int c : cylinders) {
                int cx = x + 20 + (int) ((c / 199.0) * (w - 40));
                g2.setColor(c == 53 ? RetroTheme.ACCENT_YELLOW : RetroTheme.ACCENT_CYAN);
                g2.fillOval(cx - 4, lineY - 4, 8, 8);
                g2.setFont(new Font("Consolas", Font.BOLD, 8));
                g2.drawString(String.valueOf(c), cx - 6, lineY + 14);
            }
            g2.setColor(Color.WHITE);
            g2.drawString("★ TOTAL CUMULATIVE SEEK: |98-53| + |183-98| + |37-183| + ... = 469 CYLINDERS", x + 12, y + 78);
        }

        private void drawConcurrencyPipeline(Graphics2D g2, int x, int y, int w, int h) {
            g2.setColor(new Color(255, 150, 50));
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.drawString("OVERCOOKED MUTEX PIPELINE // CRITICAL SECTION PROTOCOL", x + 12, y + 18);

            int by = y + 26;
            drawStepBox(g2, x + 12, by, 130, 34, "CHEF MARIO", "Acquire(lock) -> RUN", RetroTheme.ACCENT_GREEN);
            drawArrow(g2, x + 146, by + 18, 14);

            drawStepBox(g2, x + 164, by, 140, 34, "CRITICAL SECTION", "Burger Grill In-Use", RetroTheme.ACCENT_YELLOW);
            drawArrow(g2, x + 308, by + 18, 14);

            drawStepBox(g2, x + 326, by, 130, 34, "CHEF LUIGI", "BLOCKED (Wait)", RetroTheme.ACCENT_PINK);

            g2.setColor(Color.WHITE);
            g2.drawString("★ MUTEX GUARANTEE: Exactly 1 chef in Critical Section. Circular wait causes DEADLOCK!", x + 12, y + 78);
        }

        private void drawStepBox(Graphics2D g2, int x, int y, int w, int h, String line1, String line2, Color accent) {
            g2.setColor(new Color(20, 15, 34));
            g2.fillRoundRect(x, y, w, h, 6, 6);
            g2.setColor(accent);
            g2.drawRoundRect(x, y, w, h, 6, 6);
            g2.drawString(line1, x + 6, y + 14);
            g2.setColor(Color.WHITE);
            g2.drawString(line2, x + 6, y + 28);
        }

        private void drawArrow(Graphics2D g2, int x, int y, int len) {
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawLine(x, y, x + len, y);
            g2.drawLine(x + len - 4, y - 3, x + len, y);
            g2.drawLine(x + len - 4, y + 3, x + len, y);
        }
    }

    // ── Synthetic Audio FX ──────────────────────────────────────────────────
    private void playTone(int freq, int durationMs) {
        if (!soundEnabled) return;
        new Thread(() -> {
            try {
                byte[] buf = new byte[durationMs * 16];
                for (int i = 0; i < buf.length; i++) {
                    double angle = i / (16000.0 / freq) * 2.0 * Math.PI;
                    buf[i] = (byte) (Math.sin(angle) * 80);
                }
                AudioFormat af = new AudioFormat(16000f, 8, 1, true, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();
                sdl.write(buf, 0, buf.length);
                sdl.drain();
                sdl.close();
            } catch (Exception ignored) {}
        }).start();
    }

    private void playVictorySound() {
        new Thread(() -> {
            playTone(523, 70); // C5
            try { Thread.sleep(80); } catch (Exception ignored) {}
            playTone(659, 70); // E5
            try { Thread.sleep(80); } catch (Exception ignored) {}
            playTone(784, 70); // G5
            try { Thread.sleep(80); } catch (Exception ignored) {}
            playTone(1046, 150); // C6
        }).start();
    }

    private void playBuzzerSound() {
        new Thread(() -> {
            playTone(180, 100);
            try { Thread.sleep(60); } catch (Exception ignored) {}
            playTone(140, 150);
        }).start();
    }
}
