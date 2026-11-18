package arcadeos;

import java.util.*;

/**
 * Quiz Manager for ArcadeOS.
 * Educational worksheets themed around the 8 Arcade Minigames:
 * 01 CPU Invaders, 02 Space Shoot Banker, 03 Frame Hopper Paging,
 * 04 Super Mario Cache, 05 Cyber Snake Segmentation, 06 Cyber Crane RAM,
 * 07 Pac-Man Disk Seek, 08 Overcooked Concurrency.
 */
public class QuizManager {
    public enum Mode {
        FILL_MISSING, SOLVE_WHOLE
    }

    public static class QuizQuestion {
        public String moduleId;
        public String moduleName;
        public String arcadeGameTitle;
        public String instruction;
        public String formulaTip;
        public String[] columnHeaders;
        public String[][] worksheetData; // [row][col] with '?' for blanks
        public String[][] answerKey; // [row][col] correct answers
        public String explanation;

        public QuizQuestion(String modId, String name, String gameTitle, String inst, String tip,
                String[] headers, String[][] worksheet, String[][] answers, String expl) {
            this.moduleId = modId;
            this.moduleName = name;
            this.arcadeGameTitle = gameTitle;
            this.instruction = inst;
            this.formulaTip = tip;
            this.columnHeaders = headers;
            this.worksheetData = worksheet;
            this.answerKey = answers;
            this.explanation = expl;
        }
    }

    /** Module 01: CPU Invaders (FCFS Scheduling) */
    public static QuizQuestion getCPUQuiz(Mode mode) {
        String[] headers = { "ALIEN WAVE", "AT (ms)", "BT (ms)", "START (ST)", "FINISH (FT)", "WAIT (WT)", "TURNAROUND (TAT)" };
        String tip = "FORMULAS: Start = max(AT, prev FT) | Finish = Start + Burst | TAT = Finish - Arrival | Wait = TAT - Burst";
        String expl = "👾 CPU INVADERS // FCFS DISPATCH BREAKDOWN:\n" +
                "• W1 (Scout Alien): AT=0.0, BT=4.0 -> ST=0.0, FT=4.0, TAT=4.0, WT=0.0 ms\n" +
                "• W2 (Fighter Alien): AT=1.0, BT=3.0 -> ST=4.0, FT=7.0, TAT=6.0, WT=3.0 ms\n" +
                "• W3 (Mothership): AT=2.0, BT=2.0 -> ST=7.0, FT=9.0, TAT=7.0, WT=5.0 ms\n" +
                "★ Average Waiting Time: (0.0 + 3.0 + 5.0) / 3 = 2.67 ms\n" +
                "★ Average Turnaround Time: (4.0 + 6.0 + 7.0) / 3 = 5.67 ms";

        String[][] answers = {
                { "W1 (Scout)", "0.0", "4.0", "0.0", "4.0", "0.0", "4.0" },
                { "W2 (Fighter)", "1.0", "3.0", "4.0", "7.0", "3.0", "6.0" },
                { "W3 (Mothership)", "2.0", "2.0", "7.0", "9.0", "5.0", "7.0" }
        };

        String[][] worksheet;
        if (mode == Mode.FILL_MISSING) {
            worksheet = new String[][] {
                    { "W1 (Scout)", "0.0", "4.0", "0.0", "?", "0.0", "?" },
                    { "W2 (Fighter)", "1.0", "3.0", "?", "7.0", "?", "6.0" },
                    { "W3 (Mothership)", "2.0", "2.0", "?", "?", "5.0", "?" }
            };
        } else {
            worksheet = new String[][] {
                    { "W1 (Scout)", "0.0", "4.0", "?", "?", "?", "?" },
                    { "W2 (Fighter)", "1.0", "3.0", "?", "?", "?", "?" },
                    { "W3 (Mothership)", "2.0", "2.0", "?", "?", "?", "?" }
            };
        }

        return new QuizQuestion("MOD_01", "MISSION 01: CPU SCHEDULING (FCFS)",
                "SPACE INVADERS // CPU DISPATCH ARENA",
                "Alien interceptors are entering the CPU pipeline! Calculate Start, Finish, Wait, and Turnaround times.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 02: Space Shoot Banker (Need Matrix Calculation) */
    public static QuizQuestion getBankerQuiz(Mode mode) {
        String[] headers = { "STARFIGHTER", "ALLOCATION (A B C)", "MAX CLAIM (A B C)", "NEED MATRIX (A B C)" };
        String tip = "FORMULA: Need Vector = Max Claim - Current Allocation  (Resource vectors: A=Gold, B=Data, C=Plasma)";
        String expl = "🚀 SPACE SHOOT // BANKER'S VAULT BREAKDOWN:\n" +
                "• F0 (Vanguard): [7 5 3] - [0 1 0] = [7 4 3] Need\n" +
                "• F1 (Corsair):  [3 2 2] - [2 0 0] = [1 2 2] Need (First safe target: Need <= Available [3 3 2]!)\n" +
                "• F2 (Dreadnought): [9 0 2] - [3 0 2] = [6 0 0] Need\n" +
                "• F3 (Titan Flagship): [2 2 2] - [2 1 1] = [0 1 1] Need\n" +
                "★ Safe Sequence: F1 -> F3 -> F0 -> F2 (System is 100% Deadlock-Free!)";

        String[][] answers = {
                { "F0 (Vanguard)", "0 1 0", "7 5 3", "7 4 3" },
                { "F1 (Corsair)", "2 0 0", "3 2 2", "1 2 2" },
                { "F2 (Dreadnought)", "3 0 2", "9 0 2", "6 0 0" },
                { "F3 (Titan)", "2 1 1", "2 2 2", "0 1 1" }
        };

        String[][] worksheet;
        if (mode == Mode.FILL_MISSING) {
            worksheet = new String[][] {
                    { "F0 (Vanguard)", "0 1 0", "7 5 3", "?" },
                    { "F1 (Corsair)", "2 0 0", "3 2 2", "1 2 2" },
                    { "F2 (Dreadnought)", "3 0 2", "9 0 2", "?" },
                    { "F3 (Titan)", "2 1 1", "2 2 2", "?" }
            };
        } else {
            worksheet = new String[][] {
                    { "F0 (Vanguard)", "0 1 0", "7 5 3", "?" },
                    { "F1 (Corsair)", "2 0 0", "3 2 2", "?" },
                    { "F2 (Dreadnought)", "3 0 2", "9 0 2", "?" },
                    { "F3 (Titan)", "2 1 1", "2 2 2", "?" }
            };
        }

        return new QuizQuestion("MOD_02", "MISSION 02: BANKER'S ALGORITHM",
                "SPACE SHOOT // CYBER VAULT CLEARINGHOUSE",
                "Starfighters are requesting vault energy reserves! Compute the Need Matrix (A B C) to avoid deadlock.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 03: Frame Hopper Paging (Address Translation) */
    public static QuizQuestion getPagingQuiz(Mode mode) {
        String[] headers = { "HOPPER VIRTUAL ADDR", "PAGE SIZE", "PAGE # (p)", "OFFSET (d)", "FRAME DOCK #", "PHYSICAL ADDR" };
        String tip = "FORMULAS: Page # (p) = Addr / PageSize | Offset (d) = Addr % PageSize | Physical = (Frame * PageSize) + Offset";
        String expl = "🐸 FRAME HOPPER // MMU ADDRESS TRANSLATION BREAKDOWN:\n" +
                "• Virtual Address 2500 with Page Size 1024 B:\n" +
                "  p = 2500 / 1024 = 2 (Page 2)\n" +
                "  d = 2500 % 1024 = 452 (Offset 452 B)\n" +
                "  Page Table maps Page 2 -> Frame Dock 5\n" +
                "  Physical Address = (5 * 1024) + 452 = 5120 + 452 = 5572 bytes\n" +
                "• Virtual Address 1050 with Page Size 512 B:\n" +
                "  p = 1050 / 512 = 2 (Page 2)\n" +
                "  d = 1050 % 512 = 26 (Offset 26 B)\n" +
                "  Page Table maps Page 2 -> Frame Dock 3\n" +
                "  Physical Address = (3 * 512) + 26 = 1536 + 26 = 1562 bytes";

        String[][] answers = {
                { "2500", "1024", "2", "452", "5", "5572" },
                { "1050", "512", "2", "26", "3", "1562" }
        };

        String[][] worksheet;
        if (mode == Mode.FILL_MISSING) {
            worksheet = new String[][] {
                    { "2500", "1024", "2", "?", "5", "?" },
                    { "1050", "512", "?", "26", "3", "?" }
            };
        } else {
            worksheet = new String[][] {
                    { "2500", "1024", "?", "?", "5", "?" },
                    { "1050", "512", "?", "?", "3", "?" }
            };
        }

        return new QuizQuestion("MOD_03", "MISSION 03: PAGING ADDRESS TRANSLATION",
                "FRAME HOPPER // MEMORY BUS CROSSING",
                "The hopper packet must cross the data bus into physical RAM frame docks! Translate addresses accurately.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 04: Super Mario Pipe Cache (Page Replacement) */
    public static QuizQuestion getCacheQuiz(Mode mode) {
        String[] headers = { "CACHE POLICY", "WARP SEQUENCE", "PIPE SLOTS", "PAGE FAULTS", "PAGE HITS", "HIT RATIO (%)" };
        String tip = "FORMULAS: Faults + Hits = Total References | Hit Ratio = (Hits / Total References) * 100%";
        String expl = "🍄 SUPER MARIO // WARP PIPE CACHE EVALUATION:\n" +
                "• FIFO Policy on sequence '7 0 1 2 0 3' across 3 Pipes:\n" +
                "  7 (Fault) -> 0 (Fault) -> 1 (Fault) -> 2 evicts 7 (Fault) -> 0 (HIT!) -> 3 evicts 0 (Fault)\n" +
                "  Total Faults = 5 | Total Hits = 1 | Hit Ratio = 1 / 6 = 16.67%\n" +
                "• LRU Policy on sequence '1 2 3 2 1 4' across 3 Pipes:\n" +
                "  1 (Fault) -> 2 (Fault) -> 3 (Fault) -> 2 (HIT!) -> 1 (HIT!) -> 4 evicts 3 (Fault)\n" +
                "  Total Faults = 4 | Total Hits = 2 | Hit Ratio = 2 / 6 = 33.33%";

        String[][] answers = {
                { "FIFO", "7 0 1 2 0 3", "3", "5", "1", "16.67" },
                { "LRU", "1 2 3 2 1 4", "3", "4", "2", "33.33" }
        };

        String[][] worksheet;
        if (mode == Mode.FILL_MISSING) {
            worksheet = new String[][] {
                    { "FIFO", "7 0 1 2 0 3", "3", "?", "1", "?" },
                    { "LRU", "1 2 3 2 1 4", "3", "4", "?", "?" }
            };
        } else {
            worksheet = new String[][] {
                    { "FIFO", "7 0 1 2 0 3", "3", "?", "?", "?" },
                    { "LRU", "1 2 3 2 1 4", "3", "?", "?", "?" }
            };
        }

        return new QuizQuestion("MOD_04", "MISSION 04: PAGE REPLACEMENT POLICIES",
                "SUPER MARIO // WARP PIPE CACHE RUNNER",
                "Mario is navigating warp pipes! Calculate page faults, hits, and overall hit ratios for FIFO and LRU.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 05: Cyber Snake Segmentation (Limit Trap Check) */
    public static QuizQuestion getSegmentationQuiz(Mode mode) {
        String[] headers = { "MEMORY SEGMENT", "BASE ADDR", "LIMIT", "SNAKE OFFSET", "STATUS (VALID/TRAP)", "PHYSICAL ADDR" };
        String tip = "RULE: If Offset < Limit -> VALID, Physical = Base + Offset | If Offset >= Limit -> TRAP (SegFault!)";
        String expl = "🐍 HEAP vs STACK SNAKE // SEGMENTATION ARENA BREAKDOWN:\n" +
                "• S0 (CODE): Offset 150 < Limit 400 -> VALID! Physical = 0 + 150 = 150\n" +
                "• S1 (HEAP DATA): Offset 250 < Limit 600 -> VALID! Physical = 1400 + 250 = 1650\n" +
                "• S2 (STACK): Offset 950 >= Limit 800 -> TRAP! Hardware Limit Exceeded (Segmentation Fault Exception!)";

        String[][] answers = {
                { "S0 (CODE)", "0", "400", "150", "VALID", "150" },
                { "S1 (HEAP DATA)", "1400", "600", "250", "VALID", "1650" },
                { "S2 (STACK)", "2800", "800", "950", "TRAP", "TRAP" }
        };

        String[][] worksheet;
        if (mode == Mode.FILL_MISSING) {
            worksheet = new String[][] {
                    { "S0 (CODE)", "0", "400", "150", "VALID", "?" },
                    { "S1 (HEAP DATA)", "1400", "600", "250", "?", "?" },
                    { "S2 (STACK)", "2800", "800", "950", "?", "TRAP" }
            };
        } else {
            worksheet = new String[][] {
                    { "S0 (CODE)", "0", "400", "150", "?", "?" },
                    { "S1 (HEAP DATA)", "1400", "600", "250", "?", "?" },
                    { "S2 (STACK)", "2800", "800", "950", "?", "?" }
            };
        }

        return new QuizQuestion("MOD_05", "MISSION 05: MEMORY SEGMENTATION",
                "HEAP vs STACK SNAKE // SEGMENTATION ARENA",
                "The Cyber Snake is traversing memory segments! Validate pointer offsets against segment limits.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 06: Cyber Crane RAM Allocation & Internal Fragmentation */
    public static QuizQuestion getFragmentationQuiz(Mode mode) {
        String[] headers = { "PROCESS PAYLOAD", "REQ SIZE", "CRANE HOLE", "ALLOCATED? (YES/NO)", "INTERNAL FRAG" };
        String tip = "RULE: If Hole >= Process -> ALLOCATED: YES, Internal Frag = Hole Size - Process Size | Else NO, Frag = 0";
        String expl = "🏗️ CYBER CRANE ATTACK // RAM ALLOCATION BREAKDOWN:\n" +
                "• P1 (Audio Core, 212 KB) into Hole 500 KB: Allocated = YES, Internal Frag = 500 - 212 = 288 KB\n" +
                "• P2 (Render Engine, 417 KB) into Hole 600 KB: Allocated = YES, Internal Frag = 600 - 417 = 183 KB\n" +
                "• P3 (Physics Thread, 112 KB) into Hole 200 KB: Allocated = YES, Internal Frag = 200 - 112 = 88 KB\n" +
                "• P4 (Texture Cache, 426 KB) into Hole 300 KB: Cannot fit! Allocated = NO, Internal Frag = 0 KB";

        String[][] answers = {
                { "P1 (Audio Core)", "212 KB", "500 KB", "YES", "288 KB" },
                { "P2 (Render Engine)", "417 KB", "600 KB", "YES", "183 KB" },
                { "P3 (Physics)", "112 KB", "200 KB", "YES", "88 KB" },
                { "P4 (Texture Cache)", "426 KB", "300 KB", "NO", "0 KB" }
        };

        String[][] worksheet;
        if (mode == Mode.FILL_MISSING) {
            worksheet = new String[][] {
                    { "P1 (Audio Core)", "212 KB", "500 KB", "YES", "?" },
                    { "P2 (Render Engine)", "417 KB", "600 KB", "?", "183 KB" },
                    { "P3 (Physics)", "112 KB", "200 KB", "YES", "?" },
                    { "P4 (Texture Cache)", "426 KB", "300 KB", "?", "0 KB" }
            };
        } else {
            worksheet = new String[][] {
                    { "P1 (Audio Core)", "212 KB", "500 KB", "?", "?" },
                    { "P2 (Render Engine)", "417 KB", "600 KB", "?", "?" },
                    { "P3 (Physics)", "112 KB", "200 KB", "YES", "?" },
                    { "P4 (Texture Cache)", "426 KB", "300 KB", "?", "?" }
            };
        }

        return new QuizQuestion("MOD_06", "MISSION 06: RAM ALLOCATION & FRAGMENTATION",
                "CYBER CRANE ATTACK // MMU PARTITION MATRIX",
                "The industrial Cyber Crane is hoisting process blocks into RAM! Calculate fit allocation and internal fragmentation.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 07: Pac-Man Disk Seek (Head Movement) */
    public static QuizQuestion getDiskQuiz(Mode mode) {
        String[] headers = { "PAC-MAN SEEK HOP", "CURRENT CYLINDER", "NEXT TARGET", "SEEK DISTANCE", "CUMULATIVE HEAD MOVEMENT" };
        String tip = "FORMULAS: Seek Distance = |Next Cylinder - Current Head| | Cumulative Movement = Running Sum of Seeks";
        String expl = "🟡 PAC-MAN DISK SEEK // MAGNETIC PLATTER BREAKDOWN:\n" +
                "• Hop 1: Head at 53 seeks cylinder 98  | Seek = |98 - 53| = 45 cyl  | Total Movement = 45 cyl\n" +
                "• Hop 2: Head at 98 seeks cylinder 183 | Seek = |183 - 98| = 85 cyl | Total Movement = 45 + 85 = 130 cyl\n" +
                "• Hop 3: Head at 183 seeks cylinder 37 | Seek = |37 - 183| = 146 cyl | Total Movement = 130 + 146 = 276 cyl\n" +
                "★ Pac-Man successfully traversed 276 total cylinders across the platter!";

        String[][] answers = {
                { "Hop 1", "53", "98", "45", "45" },
                { "Hop 2", "98", "183", "85", "130" },
                { "Hop 3", "183", "37", "146", "276" }
        };

        String[][] worksheet;
        if (mode == Mode.FILL_MISSING) {
            worksheet = new String[][] {
                    { "Hop 1", "53", "98", "45", "?" },
                    { "Hop 2", "98", "183", "?", "130" },
                    { "Hop 3", "183", "37", "146", "?" }
            };
        } else {
            worksheet = new String[][] {
                    { "Hop 1", "53", "98", "?", "?" },
                    { "Hop 2", "98", "183", "?", "?" },
                    { "Hop 3", "183", "37", "?", "?" }
            };
        }

        return new QuizQuestion("MOD_07", "MISSION 07: DISK SEEK TRAVERSAL",
                "PAC-MAN DISK SEEK // MAGNETIC PLATTER ARENA",
                "Pac-Man is seeking pellets across magnetic cylinders! Calculate individual seek hops and cumulative head movement.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 08: Overcooked Multiprogramming (CPU Utilization) */
    public static QuizQuestion getMultiprogrammingQuiz(Mode mode) {
        String[] headers = { "ORDERS (N)", "I/O WAIT PROB (p)", "IDLE PROB (p^N)", "CHEF CPU UTILIZATION (%)" };
        String tip = "FORMULAS: Idle Probability = p^N | Active CPU Utilization = (1 - p^N) * 100%";
        String expl = "👨‍🍳 OVERCOOKED KITCHEN // MULTIPROGRAMMING BREAKDOWN (I/O Wait p = 0.50):\n" +
                "• 1 Order  (N=1): Idle = 0.50^1 = 0.50   -> CPU Utilization = (1 - 0.50) * 100 = 50.0%\n" +
                "• 2 Orders (N=2): Idle = 0.50^2 = 0.25   -> CPU Utilization = (1 - 0.25) * 100 = 75.0%\n" +
                "• 3 Orders (N=3): Idle = 0.50^3 = 0.125  -> CPU Utilization = (1 - 0.125) * 100 = 87.5%\n" +
                "• 4 Orders (N=4): Idle = 0.50^4 = 0.0625 -> CPU Utilization = (1 - 0.0625) * 100 = 93.75%\n" +
                "★ Multi-ticket concurrency keeps the kitchen CPU saturated near 94% throughput!";

        String[][] answers = {
                { "1 Order", "0.50", "0.50", "50.0" },
                { "2 Orders", "0.50", "0.25", "75.0" },
                { "3 Orders", "0.50", "0.125", "87.5" },
                { "4 Orders", "0.50", "0.0625", "93.75" }
        };

        String[][] worksheet;
        if (mode == Mode.FILL_MISSING) {
            worksheet = new String[][] {
                    { "1 Order", "0.50", "0.50", "?" },
                    { "2 Orders", "0.50", "?", "75.0" },
                    { "3 Orders", "0.50", "0.125", "?" },
                    { "4 Orders", "0.50", "?", "93.75" }
            };
        } else {
            worksheet = new String[][] {
                    { "1 Order", "0.50", "?", "?" },
                    { "2 Orders", "0.50", "?", "?" },
                    { "3 Orders", "0.50", "?", "?" },
                    { "4 Orders", "0.50", "?", "?" }
            };
        }

        return new QuizQuestion("MOD_08", "MISSION 08: MULTIPROGRAMMING CONCURRENCY",
                "OVERCOOKED // CONCURRENT KITCHEN DASHBOARD",
                "The kitchen is managing concurrent food tickets! Calculate idle probabilities and chef CPU utilization curves.",
                tip, headers, worksheet, answers, expl);
    }

    /** Helper to fetch question for any of the 8 modules */
    public static QuizQuestion getQuestionByModule(int modIdx, Mode mode) {
        switch (modIdx) {
            case 0: return getCPUQuiz(mode);
            case 1: return getBankerQuiz(mode);
            case 2: return getPagingQuiz(mode);
            case 3: return getCacheQuiz(mode);
            case 4: return getSegmentationQuiz(mode);
            case 5: return getFragmentationQuiz(mode);
            case 6: return getDiskQuiz(mode);
            case 7: return getMultiprogrammingQuiz(mode);
            default: return getCPUQuiz(mode);
        }
    }

    /** Normalize strings by stripping whitespace and common unit suffixes */
    public static String cleanStr(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("(?i)(kb|%|bytes|cyl|orders|order|ms)", "").trim();
    }

    /** Check if individual cell matches answer key */
    public static boolean isCellCorrect(String actual, String expected) {
        if (actual == null || expected == null) return false;
        String a = actual.trim();
        String e = expected.trim();
        if (a.equalsIgnoreCase(e)) return true;

        String aClean = cleanStr(a);
        String eClean = cleanStr(e);
        if (aClean.equalsIgnoreCase(eClean)) return true;

        try {
            double numA = Double.parseDouble(aClean);
            double numE = Double.parseDouble(eClean);
            return Math.abs(numA - numE) < 0.06;
        } catch (Exception ex) {
            return false;
        }
    }

    /** Verify user submitted answers against answer key */
    public static boolean verify(String[][] userGrid, String[][] correctGrid) {
        if (userGrid == null || correctGrid == null) return false;
        for (int r = 0; r < correctGrid.length; r++) {
            for (int c = 0; c < correctGrid[r].length; c++) {
                String expected = correctGrid[r][c];
                String actual = (r < userGrid.length && c < userGrid[r].length) ? userGrid[r][c] : "";
                if (!isCellCorrect(actual, expected)) {
                    return false;
                }
            }
        }
        return true;
    }
}
