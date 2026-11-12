package arcadeos;

import java.util.*;

/**
 * Quiz Manager for ArcadeOS.
 * Adapted and enhanced from in-suu/MiniOs to provide interactive
 * worksheets for OS algorithm problem solving.
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
                "J1 arrives at 0.0, BT=4.0 -> ST=0.0, FT=4.0, TAT=4.0, WT=0.0\n" +
                "J2 arrives at 1.0, BT=3.0 -> ST=4.0, FT=7.0, TAT=6.0, WT=3.0\n" +
                "J3 arrives at 2.0, BT=2.0 -> ST=7.0, FT=9.0, TAT=7.0, WT=5.0";

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

        return new QuizQuestion("MOD_01", "MODULE 01: CPU SCHEDULING (FCFS)",
                "Calculate Start Time, Finish Time, Waiting Time, and Turnaround Time for each job.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 02: Banker's Algorithm (Need Matrix Calculation) */
    public static QuizQuestion getBankerQuiz(Mode mode) {
        String[] headers = { "PROCESS", "ALLOCATION (A B C)", "MAX CLAIM (A B C)", "NEED (A B C)" };
        String tip = "FORMULA: Need Matrix = Max Claim - Allocation  (Element-by-element subtraction)";
        String expl = "Banker's Need Matrix Breakdown:\n" +
                "P0: [7 5 3] - [0 1 0] = [7 4 3]\n" +
                "P1: [3 2 2] - [2 0 0] = [1 2 2]\n" +
                "P2: [9 0 2] - [3 0 2] = [6 0 0]\n" +
                "P3: [2 2 2] - [2 1 1] = [0 1 1]";

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

        return new QuizQuestion("MOD_02", "MODULE 02: BANKER'S ALGORITHM",
                "Compute the Need Matrix (A B C) for each process to ensure deadlock avoidance.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 03: Paging Address Translation */
    public static QuizQuestion getPagingQuiz(Mode mode) {
        String[] headers = { "LOGICAL ADDR", "PAGE SIZE", "PAGE # (p)", "OFFSET (d)", "FRAME # (f)", "PHYSICAL ADDR" };
        String tip = "FORMULAS: Page # (p) = Logical / PageSize | Offset (d) = Logical % PageSize | Physical = (Frame * PageSize) + Offset";
        String expl = "Paging Calculation:\n" +
                "Logical 2500, PageSize 1024:\n" +
                "p = 2500 / 1024 = 2\n" +
                "d = 2500 % 1024 = 452\n" +
                "With Frame 5: Physical = (5 * 1024) + 452 = 5572 bytes\n" +
                "Logical 1050, PageSize 512:\n" +
                "p = 1050 / 512 = 2\n" +
                "d = 1050 % 512 = 26\n" +
                "With Frame 3: Physical = (3 * 512) + 26 = 1562 bytes";

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

        return new QuizQuestion("MOD_03", "MODULE 03: PAGING ADDRESS TRANSLATION",
                "Translate logical addresses into physical memory addresses using page numbers and offsets.",
                tip, headers, worksheet, answers, expl);
    }

    /** Module 04: Page Replacement */
    public static QuizQuestion getCacheQuiz(Mode mode) {
        String[] headers = { "ALGORITHM", "REF STRING", "FRAMES", "TOTAL FAULTS", "TOTAL HITS", "HIT RATIO (%)" };
        String tip = "FORMULAS: Total Faults + Total Hits = Total References | Hit Ratio = (Hits / References) * 100%";
        String expl = "Page Replacement Evaluation:\n" +
                "FIFO (7 0 1 2 0 3, 3 Frames): 5 Faults, 1 Hit -> 16.67%\n" +
                "LRU  (1 2 3 2 1 4, 3 Frames): 4 Faults, 2 Hits -> 33.33%";

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

        return new QuizQuestion("MOD_04", "MODULE 04: PAGE REPLACEMENT POLICIES",
                "Determine page faults, hits, and overall hit ratios for FIFO and LRU algorithms.",
                tip, headers, worksheet, answers, expl);
    }

    /** Verify user submitted answers against answer key */
    public static boolean verify(String[][] userGrid, String[][] correctGrid) {
        if (userGrid == null || correctGrid == null)
            return false;
        for (int r = 0; r < correctGrid.length; r++) {
            for (int c = 0; c < correctGrid[r].length; c++) {
                String expected = correctGrid[r][c].trim();
                String actual = (r < userGrid.length && c < userGrid[r].length) ? userGrid[r][c].trim() : "";
                // Format check: normalize numbers if float
                if (!isMatch(actual, expected)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isMatch(String actual, String expected) {
        if (actual.equalsIgnoreCase(expected))
            return true;
        try {
            double a = Double.parseDouble(actual);
            double e = Double.parseDouble(expected);
            return Math.abs(a - e) < 0.05;
        } catch (Exception ex) {
            return false;
        }
    }
}
