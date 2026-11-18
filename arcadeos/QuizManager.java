package arcadeos;

import java.util.*;

/**
 * Quiz Manager for ArcadeOS.
 * Provides interactive educational worksheets for all 8 Operating System algorithms.
 */
public class QuizManager {
    public enum Mode {
        FILL_MISSING, SOLVE_WHOLE
    }

    public static class QuizQuestion {
        public String moduleId;
        public String moduleName;
        public String instruction;
        public String formulaTip;
        public String[] columnHeaders;
        public String[][] worksheetData; // [row][col] with '?' for blanks
        public String[][] answerKey; // [row][col] correct answers
        public String explanation;

        public QuizQuestion(String modId, String name, String inst, String tip,
                String[] headers, String[][] worksheet, String[][] answers, String expl) {
            this.moduleId = modId;
            this.moduleName = name;
            this.instruction = inst;
            this.formulaTip = tip;
            this.columnHeaders = headers;
            this.worksheetData = worksheet;
            this.answerKey = answers;
            this.explanation = expl;
        }
    }

    /** Module 01: CPU Scheduling (FCFS) */
    public static QuizQuestion getCPUQuiz(Mode mode) {
        String[] headers = { "JOB", "AT", "BT", "START (ST)", "FINISH (FT)", "WAIT (WT)", "TURN (TAT)" };
        String tip = "FORMULAS: ST = max(AT, prev FT) | FT = ST + BT | TAT = FT - AT | WT = TAT - BT";
        String expl = "FCFS Answer Breakdown:\n" +
                "• J1: AT=0.0, BT=4.0 -> ST=0.0, FT=4.0, TAT=4.0, WT=0.0\n" +
                "• J2: AT=1.0, BT=3.0 -> ST=4.0, FT=7.0, TAT=6.0, WT=3.0\n" +
                "• J3: AT=2.0, BT=2.0 -> ST=7.0, FT=9.0, TAT=7.0, WT=5.0\n" +
                "Average Waiting Time = (0.0 + 3.0 + 5.0) / 3 = 2.67 ms";

        String[][] answers = {
                { "J1", "0.0", "4.0", "0.0", "4.0", "0.0", "4.0" },
                { "J2", "1.0", "3.0", "4.0", "7.0", "3.0", "6.0" },
                { "J3", "2.0", "2.0", "7.0", "9.0", "5.0", "7.0" }
        };

        String[][] worksheet;
        if (mode == Mode.FILL_MISSING) {
            worksheet = new String[][] {
                    { "J1", "0.0", "4.0", "0.0", "?", "0.0", "?" },
                    { "J2", "1.0", "3.0", "?", "7.0", "?", "6.0" },
                    { "J3", "2.0", "2.0", "?", "?", "5.0", "?" }
            };
        } else {
            worksheet = new String[][] {
                    { "J1", "0.0", "4.0", "?", "?", "?", "?" },
                    { "J2", "1.0", "3.0", "?", "?", "?", "?" },
                    { "J3", "2.0", "2.0", "?", "?", "?", "?" }
            };
        }

        return new QuizQuestion("MOD_01", "MISSION 01: CPU SCHEDULING (FCFS)",
                "Calculate Start Time, Finish Time, Waiting Time, and Turnaround Time for each job.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 02: Banker's Algorithm (Need Matrix Calculation) */
    public static QuizQuestion getBankerQuiz(Mode mode) {
        String[] headers = { "PROCESS", "ALLOCATION (A B C)", "MAX CLAIM (A B C)", "NEED (A B C)" };
        String tip = "FORMULA: Need Matrix = Max Claim - Allocation  (Element-by-element vector subtraction)";
        String expl = "Banker's Need Matrix Breakdown:\n" +
                "• P0: [7 5 3] - [0 1 0] = [7 4 3]\n" +
                "• P1: [3 2 2] - [2 0 0] = [1 2 2]\n" +
                "• P2: [9 0 2] - [3 0 2] = [6 0 0]\n" +
                "• P3: [2 2 2] - [2 1 1] = [0 1 1]";

        String[][] answers = {
                { "P0", "0 1 0", "7 5 3", "7 4 3" },
                { "P1", "2 0 0", "3 2 2", "1 2 2" },
                { "P2", "3 0 2", "9 0 2", "6 0 0" },
                { "P3", "2 1 1", "2 2 2", "0 1 1" }
        };

        String[][] worksheet;
        if (mode == Mode.FILL_MISSING) {
            worksheet = new String[][] {
                    { "P0", "0 1 0", "7 5 3", "?" },
                    { "P1", "2 0 0", "3 2 2", "1 2 2" },
                    { "P2", "3 0 2", "9 0 2", "?" },
                    { "P3", "2 1 1", "2 2 2", "?" }
            };
        } else {
            worksheet = new String[][] {
                    { "P0", "0 1 0", "7 5 3", "?" },
                    { "P1", "2 0 0", "3 2 2", "?" },
                    { "P2", "3 0 2", "9 0 2", "?" },
                    { "P3", "2 1 1", "2 2 2", "?" }
            };
        }

        return new QuizQuestion("MOD_02", "MISSION 02: BANKER'S ALGORITHM",
                "Compute the Need Matrix (A B C) for each process to ensure deadlock avoidance.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 03: Paging Address Translation */
    public static QuizQuestion getPagingQuiz(Mode mode) {
        String[] headers = { "LOGICAL ADDR", "PAGE SIZE", "PAGE # (p)", "OFFSET (d)", "FRAME # (f)", "PHYSICAL ADDR" };
        String tip = "FORMULAS: Page # (p) = Logical / PageSize | Offset (d) = Logical % PageSize | Physical = (Frame * PageSize) + Offset";
        String expl = "Paging Calculation:\n" +
                "• Logical 2500, PageSize 1024:\n" +
                "  p = 2500 / 1024 = 2\n" +
                "  d = 2500 % 1024 = 452\n" +
                "  Frame 5: Physical = (5 * 1024) + 452 = 5572 bytes\n" +
                "• Logical 1050, PageSize 512:\n" +
                "  p = 1050 / 512 = 2\n" +
                "  d = 1050 % 512 = 26\n" +
                "  Frame 3: Physical = (3 * 512) + 26 = 1562 bytes";

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
                "Translate logical addresses into physical memory addresses using page numbers and offsets.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 04: Page Replacement */
    public static QuizQuestion getCacheQuiz(Mode mode) {
        String[] headers = { "ALGORITHM", "REF STRING", "FRAMES", "TOTAL FAULTS", "TOTAL HITS", "HIT RATIO (%)" };
        String tip = "FORMULAS: Total Faults + Total Hits = Total References | Hit Ratio = (Hits / References) * 100%";
        String expl = "Page Replacement Evaluation:\n" +
                "• FIFO (7 0 1 2 0 3, 3 Frames): 5 Faults, 1 Hit -> Hit Ratio = 1 / 6 = 16.67%\n" +
                "• LRU  (1 2 3 2 1 4, 3 Frames): 4 Faults, 2 Hits -> Hit Ratio = 2 / 6 = 33.33%";

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
                "Determine page faults, hits, and overall hit ratios for FIFO and LRU algorithms.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 05: Segmentation & Limit Trap Checking */
    public static QuizQuestion getSegmentationQuiz(Mode mode) {
        String[] headers = { "SEGMENT", "BASE ADDR", "LIMIT", "LOGICAL OFFSET", "STATUS (VALID/TRAP)", "PHYSICAL ADDR" };
        String tip = "RULE: If Offset < Limit -> VALID, Physical = Base + Offset | If Offset >= Limit -> TRAP (SegFault!)";
        String expl = "Segmentation Address Validation Breakdown:\n" +
                "• S0 (CODE): Offset 150 < Limit 400 -> VALID! Physical = 0 + 150 = 150\n" +
                "• S1 (DATA): Offset 250 < Limit 600 -> VALID! Physical = 1400 + 250 = 1650\n" +
                "• S2 (STACK): Offset 950 >= Limit 800 -> TRAP! Hardware Limit Exceeded (No PA Generated)";

        String[][] answers = {
                { "S0 (CODE)", "0", "400", "150", "VALID", "150" },
                { "S1 (DATA)", "1400", "600", "250", "VALID", "1650" },
                { "S2 (STACK)", "2800", "800", "950", "TRAP", "TRAP" }
        };

        String[][] worksheet;
        if (mode == Mode.FILL_MISSING) {
            worksheet = new String[][] {
                    { "S0 (CODE)", "0", "400", "150", "VALID", "?" },
                    { "S1 (DATA)", "1400", "600", "250", "?", "?" },
                    { "S2 (STACK)", "2800", "800", "950", "?", "TRAP" }
            };
        } else {
            worksheet = new String[][] {
                    { "S0 (CODE)", "0", "400", "150", "?", "?" },
                    { "S1 (DATA)", "1400", "600", "250", "?", "?" },
                    { "S2 (STACK)", "2800", "800", "950", "?", "?" }
            };
        }

        return new QuizQuestion("MOD_05", "MISSION 05: MEMORY SEGMENTATION",
                "Validate logical offsets against segment limits and calculate physical addresses or hardware traps.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 06: Contiguous RAM Allocation & Internal Fragmentation */
    public static QuizQuestion getFragmentationQuiz(Mode mode) {
        String[] headers = { "PROCESS", "PROCESS SIZE", "PARTITION BLOCK", "ALLOCATED? (YES/NO)", "INTERNAL FRAG" };
        String tip = "RULE: If Block >= Process -> ALLOCATED: YES, Internal Frag = Block Size - Process Size | Else NO, Frag = 0";
        String expl = "Memory Allocation & Fragmentation Breakdown:\n" +
                "• P1 (212 KB) placed in Block 500 KB: Allocated = YES, Internal Frag = 500 - 212 = 288 KB\n" +
                "• P2 (417 KB) placed in Block 600 KB: Allocated = YES, Internal Frag = 600 - 417 = 183 KB\n" +
                "• P3 (112 KB) placed in Block 200 KB: Allocated = YES, Internal Frag = 200 - 112 = 88 KB\n" +
                "• P4 (426 KB) cannot fit in Block 300 KB: Allocated = NO, Internal Frag = 0 KB";

        String[][] answers = {
                { "P1", "212 KB", "500 KB", "YES", "288 KB" },
                { "P2", "417 KB", "600 KB", "YES", "183 KB" },
                { "P3", "112 KB", "200 KB", "YES", "88 KB" },
                { "P4", "426 KB", "300 KB", "NO", "0 KB" }
        };

        String[][] worksheet;
        if (mode == Mode.FILL_MISSING) {
            worksheet = new String[][] {
                    { "P1", "212 KB", "500 KB", "YES", "?" },
                    { "P2", "417 KB", "600 KB", "?", "183 KB" },
                    { "P3", "112 KB", "200 KB", "YES", "?" },
                    { "P4", "426 KB", "300 KB", "?", "0 KB" }
            };
        } else {
            worksheet = new String[][] {
                    { "P1", "212 KB", "500 KB", "?", "?" },
                    { "P2", "417 KB", "600 KB", "?", "?" },
                    { "P3", "112 KB", "200 KB", "?", "?" },
                    { "P4", "426 KB", "300 KB", "?", "?" }
            };
        }

        return new QuizQuestion("MOD_06", "MISSION 06: CONTIGUOUS ALLOCATION & FRAGMENTATION",
                "Determine partition allocation feasibility and calculate internal memory fragmentation.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 07: Disk Scheduling Track Traversal */
    public static QuizQuestion getDiskQuiz(Mode mode) {
        String[] headers = { "HOP #", "CURRENT HEAD", "NEXT CYLINDER", "SEEK DISTANCE", "CUMULATIVE HEAD MOVEMENT" };
        String tip = "FORMULAS: Seek Distance = |Next Cylinder - Current Head| | Cumulative Movement = Running Sum of Seeks";
        String expl = "Disk Seek Calculation Breakdown (Initial Head = 53):\n" +
                "• Hop 1: 53 -> 98  | Seek = |98 - 53| = 45 cyl | Total = 45 cyl\n" +
                "• Hop 2: 98 -> 183 | Seek = |183 - 98| = 85 cyl | Total = 45 + 85 = 130 cyl\n" +
                "• Hop 3: 183 -> 37 | Seek = |37 - 183| = 146 cyl | Total = 130 + 146 = 276 cyl";

        String[][] answers = {
                { "1", "53", "98", "45", "45" },
                { "2", "98", "183", "85", "130" },
                { "3", "183", "37", "146", "276" }
        };

        String[][] worksheet;
        if (mode == Mode.FILL_MISSING) {
            worksheet = new String[][] {
                    { "1", "53", "98", "45", "?" },
                    { "2", "98", "183", "?", "130" },
                    { "3", "183", "37", "146", "?" }
            };
        } else {
            worksheet = new String[][] {
                    { "1", "53", "98", "?", "?" },
                    { "2", "98", "183", "?", "?" },
                    { "3", "183", "37", "?", "?" }
            };
        }

        return new QuizQuestion("MOD_07", "MISSION 07: DISK SEEK TRAVERSAL",
                "Compute individual track seek distances and cumulative total head movement across cylinders.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 08: Multiprogramming CPU Utilization */
    public static QuizQuestion getMultiprogrammingQuiz(Mode mode) {
        String[] headers = { "DEGREE (N)", "I/O WAIT PROB (p)", "IDLE PROB (p^N)", "CPU UTILIZATION (%)" };
        String tip = "FORMULAS: Idle Probability = p^N | CPU Utilization = (1 - p^N) * 100%";
        String expl = "Multiprogramming Throughput Breakdown (p = 0.50):\n" +
                "• N = 1: Idle = 0.50^1 = 0.50 -> CPU Utilization = (1 - 0.50) * 100 = 50.0%\n" +
                "• N = 2: Idle = 0.50^2 = 0.25 -> CPU Utilization = (1 - 0.25) * 100 = 75.0%\n" +
                "• N = 3: Idle = 0.50^3 = 0.125 -> CPU Utilization = (1 - 0.125) * 100 = 87.5%\n" +
                "• N = 4: Idle = 0.50^4 = 0.0625 -> CPU Utilization = (1 - 0.0625) * 100 = 93.75%";

        String[][] answers = {
                { "1", "0.50", "0.50", "50.0" },
                { "2", "0.50", "0.25", "75.0" },
                { "3", "0.50", "0.125", "87.5" },
                { "4", "0.50", "0.0625", "93.75" }
        };

        String[][] worksheet;
        if (mode == Mode.FILL_MISSING) {
            worksheet = new String[][] {
                    { "1", "0.50", "0.50", "?" },
                    { "2", "0.50", "?", "75.0" },
                    { "3", "0.50", "0.125", "?" },
                    { "4", "0.50", "?", "93.75" }
            };
        } else {
            worksheet = new String[][] {
                    { "1", "0.50", "?", "?" },
                    { "2", "0.50", "?", "?" },
                    { "3", "0.50", "?", "?" },
                    { "4", "0.50", "?", "?" }
            };
        }

        return new QuizQuestion("MOD_08", "MISSION 08: MULTIPROGRAMMING UTILIZATION",
                "Calculate CPU idle probabilities and total active utilization curves for varying degrees of multiprogramming.",
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

    /** Normalize strings by stripping whitespace and common unit suffixes (KB, %, etc.) */
    public static String cleanStr(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("(?i)(kb|%|bytes|cyl)", "").trim();
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
