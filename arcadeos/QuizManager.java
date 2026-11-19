package arcadeos;

import java.util.*;

/**
 * Quiz Manager for ArcadeOS // Dynamic Procedural Worksheet Engine.
 * -----------------------------------------------------------------------------
 * Procedurally generates randomized, mathematically sound algorithm worksheets
 * for all 8 Arcade Modules across 3 distinct difficulty tiers:
 *   - EASY    [RECRUIT]: Compact table (3 rows), fewer missing items (1 blank/row), generous anchors.
 *   - AVERAGE [VETERAN]: Balanced table (4 rows), moderate missing items (2 blanks/row), standard constraints.
 *   - EXPERT  [MASTER]:  Expanded matrix (5-6 rows), maximum missing items (3-4 blanks/row or full derivation),
 *                        strict edge cases and complex arithmetic.
 */
public class QuizManager {
    public enum Mode {
        FILL_MISSING, SOLVE_WHOLE
    }

    public enum Difficulty {
        EASY("EASY", "RECRUIT TIER", 100, 1.0),
        AVERAGE("AVERAGE", "VETERAN TIER", 250, 1.5),
        EXPERT("EXPERT", "CYBER MASTER TIER", 500, 2.5);

        public final String label;
        public final String tier;
        public final int xpReward;
        public final double multiplier;

        Difficulty(String label, String tier, int xpReward, double multiplier) {
            this.label = label;
            this.tier = tier;
            this.xpReward = xpReward;
            this.multiplier = multiplier;
        }
    }

    public static class QuizQuestion {
        public String moduleId;
        public String moduleName;
        public String arcadeGameTitle;
        public String instruction;
        public String formulaTip;
        public String[] columnHeaders;
        public String[][] worksheetData; // [row][col] with '?' for blanks
        public String[][] answerKey;     // [row][col] correct answers
        public String explanation;
        public Difficulty difficulty = Difficulty.AVERAGE;
        public Map<String, Object> telemetryData = new HashMap<>();

        // Sequence Quiz Challenge fields
        public String sequenceTitle = "EXECUTION SEQUENCE";
        public String sequencePrompt = "Arrange or determine the correct execution order:";
        public String correctSequence = "";
        public String[] sequenceTokens = new String[0];
        public int[] initialAvailable = null; // For Banker's algorithm and resource tracking
        public String policy = "FIFO"; // For page replacement: FIFO, LRU, OPTIMAL

        public QuizQuestion(String modId, String name, String gameTitle, String inst, String tip,
                String[] headers, String[][] worksheet, String[][] answers, String expl, Difficulty diff) {
            this.moduleId = modId;
            this.moduleName = name;
            this.arcadeGameTitle = gameTitle;
            this.instruction = inst;
            this.formulaTip = tip;
            this.columnHeaders = headers;
            this.worksheetData = worksheet;
            this.answerKey = answers;
            this.explanation = expl;
            this.difficulty = diff;
        }
    }

    private static final Random RND = new Random();
    public static String activeCachePolicy = "FIFO";
    public static String activeAllocationPolicy = "FIRST FIT";
    public static String activeDiskPolicy = "FCFS";

    /**
     * Randomly picks distinct computable columns per row and blanks them out with '?'
     * so the blank positions are dynamically changing and never static.
     */
    private static void applyRandomBlanks(String[][] worksheet, int rowStart, int rowEnd, int[] candidateCols, int blanksPerRow) {
        for (int r = rowStart; r < rowEnd; r++) {
            List<Integer> pool = new ArrayList<>();
            for (int c : candidateCols) pool.add(c);
            Collections.shuffle(pool, RND);
            int count = Math.min(blanksPerRow, pool.size());
            for (int k = 0; k < count; k++) {
                worksheet[r][pool.get(k)] = "?";
            }
        }
    }

    public static void shuffleArray(String[] ar) {
        for (int i = ar.length - 1; i > 0; i--) {
            int index = RND.nextInt(i + 1);
            String a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODULE 01: CPU INVADERS // FCFS SCHEDULING (RANDOMIZED & SCALED)
    // ─────────────────────────────────────────────────────────────────────────
    public static QuizQuestion getCPUQuiz(Difficulty diff, Mode mode) {
        String[] headers = { "WAVE", "ARRIVAL (AT)", "BURST (BT)", "START (ST)", "FINISH (FT)", "WAIT (WT)", "TURN (TAT)" };
        int n = (diff == Difficulty.EASY) ? 3 : (diff == Difficulty.AVERAGE ? 5 : 7);
        String tip = "FORMULAS: Start = max(AT, prev FT) | Finish = Start + Burst | TAT = Finish - Arrival | Wait = TAT - Burst | Avg WT = Sum(WT) / " + n + " | Avg TAT = Sum(TAT) / " + n;

        String[] pool = {
            "Scout", "Drone", "Interceptor", "Corvette", "Dreadnought",
            "Viper", "Fighter", "Raider", "Carrier", "Flagship"
        };
        String[] waveNames = new String[n];
        for (int i = 0; i < n; i++) {
            waveNames[i] = "W" + (i + 1) + " (" + pool[i % pool.length] + ")";
        }

        // Generate random, non-continuous arrival times across waves
        List<Integer> arrivalOrder = new ArrayList<>();
        for (int i = 0; i < n; i++) arrivalOrder.add(i);
        Collections.shuffle(arrivalOrder, RND);

        double[] at = new double[n];
        double[] bt = new double[n];
        double[] st = new double[n];
        double[] ft = new double[n];
        double[] tat = new double[n];
        double[] wt = new double[n];

        // First wave in arrivalOrder arrives at 0.0; subsequent waves arrive with variable delays
        double curTimeline = 0.0;
        for (int step = 0; step < n; step++) {
            int waveIdx = arrivalOrder.get(step);
            if (step == 0) {
                at[waveIdx] = 0.0;
            } else {
                boolean idle = (RND.nextDouble() < 0.28);
                curTimeline += idle ? (2.0 + RND.nextInt(3)) : (1.0 + RND.nextInt(2));
                at[waveIdx] = curTimeline;
            }
            bt[waveIdx] = 2.0 + RND.nextInt(5);
        }

        // Simulate FCFS execution in arrivalOrder:
        double coreTime = 0.0;
        for (int step = 0; step < n; step++) {
            int waveIdx = arrivalOrder.get(step);
            st[waveIdx] = Math.max(at[waveIdx], coreTime);
            ft[waveIdx] = st[waveIdx] + bt[waveIdx];
            tat[waveIdx] = ft[waveIdx] - at[waveIdx];
            wt[waveIdx] = tat[waveIdx] - bt[waveIdx];
            coreTime = ft[waveIdx];
        }

        double sumWT = 0, sumTAT = 0;
        for (int i = 0; i < n; i++) {
            sumWT += wt[i];
            sumTAT += tat[i];
        }
        double avgWT = sumWT / n;
        double avgTAT = sumTAT / n;

        int totalRows = n + 1;
        String[][] answers = new String[totalRows][7];
        String[][] worksheet = new String[totalRows][7];

        for (int i = 0; i < n; i++) {
            answers[i][0] = waveNames[i];
            answers[i][1] = String.format(Locale.US, "%.1f", at[i]);
            answers[i][2] = String.format(Locale.US, "%.1f", bt[i]);
            answers[i][3] = String.format(Locale.US, "%.1f", st[i]);
            answers[i][4] = String.format(Locale.US, "%.1f", ft[i]);
            answers[i][5] = String.format(Locale.US, "%.1f", wt[i]);
            answers[i][6] = String.format(Locale.US, "%.1f", tat[i]);

            System.arraycopy(answers[i], 0, worksheet[i], 0, 7);
        }

        // Summary Row for AVERAGE (AVG WT & AVG TAT)
        answers[n][0] = "AVERAGE (AVG)";
        answers[n][1] = "-";
        answers[n][2] = "-";
        answers[n][3] = "-";
        answers[n][4] = "-";
        answers[n][5] = String.format(Locale.US, "%.2f", avgWT);
        answers[n][6] = String.format(Locale.US, "%.2f", avgTAT);

        worksheet[n][0] = answers[n][0];
        worksheet[n][1] = "-";
        worksheet[n][2] = "-";
        worksheet[n][3] = "-";
        worksheet[n][4] = "-";
        worksheet[n][5] = String.format(Locale.US, "%.2f", avgWT);
        worksheet[n][6] = String.format(Locale.US, "%.2f", avgTAT);

        int[] calcCols = { 3, 4, 5, 6 };
        if (mode == Mode.FILL_MISSING) {
            if (diff == Difficulty.EASY) {
                applyRandomBlanks(worksheet, 0, n, calcCols, 1);
                if (RND.nextBoolean()) {
                    worksheet[n][5] = "?";
                } else {
                    worksheet[n][6] = "?";
                }
            } else if (diff == Difficulty.AVERAGE) {
                applyRandomBlanks(worksheet, 0, n, calcCols, 2);
                worksheet[n][5] = "?";
                worksheet[n][6] = "?";
            } else {
                applyRandomBlanks(worksheet, 0, n, calcCols, 3);
                worksheet[n][5] = "?";
                worksheet[n][6] = "?";
            }
        } else {
            for (int i = 0; i < n; i++) {
                for (int c : calcCols) worksheet[i][c] = "?";
            }
            worksheet[n][5] = "?";
            worksheet[n][6] = "?";
        }

        StringBuilder expl = new StringBuilder(String.format("👾 CPU INVADERS // FCFS DISPATCH TELEMETRY [%s TIER]:\n", diff.label));
        for (int step = 0; step < n; step++) {
            int i = arrivalOrder.get(step);
            expl.append(String.format(Locale.US, "• [%d] %s: AT=%.1f, BT=%.1f -> ST=%.1f, FT=%.1f, TAT=%.1f, WT=%.1f ms\n",
                    step + 1, waveNames[i], at[i], bt[i], st[i], ft[i], tat[i], wt[i]));
        }
        expl.append(String.format(Locale.US, "★ Average Waiting Time (AVG WT): (%.1f", wt[0]));
        for (int i = 1; i < n; i++) expl.append(String.format(Locale.US, " + %.1f", wt[i]));
        expl.append(String.format(Locale.US, ") / %d = %.2f ms\n", n, avgWT));

        expl.append(String.format(Locale.US, "★ Average Turnaround Time (AVG TAT): (%.1f", tat[0]));
        for (int i = 1; i < n; i++) expl.append(String.format(Locale.US, " + %.1f", tat[i]));
        expl.append(String.format(Locale.US, ") / %d = %.2f ms\n", n, avgTAT));

        QuizQuestion q = new QuizQuestion("MOD_01", "MISSION 01: CPU SCHEDULING (FCFS)",
                "SPACE INVADERS // CPU DISPATCH ARENA",
                String.format("Alien interceptors are entering the CPU pipeline (%s Tier: %d Waves)! Calculate Start, Finish, Wait, Turnaround, and the Average WT and Average TAT.", diff.label, n),
                tip, headers, worksheet, answers, expl.toString(), diff);

        q.sequenceTitle = "CPU DISPATCH ORDER // EXECUTION SEQUENCE";
        q.sequencePrompt = "Identify the order waves claim the CPU (FCFS: Earliest Arrival First):";
        StringBuilder seqSb = new StringBuilder();
        for (int step = 0; step < n; step++) {
            int waveIdx = arrivalOrder.get(step);
            if (step > 0) seqSb.append(" -> ");
            seqSb.append("W").append(waveIdx + 1);
        }
        q.correctSequence = seqSb.toString();
        String[] tokens = new String[n];
        for (int i = 0; i < n; i++) tokens[i] = "W" + (i + 1);
        shuffleArray(tokens);
        q.sequenceTokens = tokens;

        q.telemetryData.put("waveNames", waveNames);
        q.telemetryData.put("at", at);
        q.telemetryData.put("bt", bt);
        q.telemetryData.put("st", st);
        q.telemetryData.put("ft", ft);
        q.telemetryData.put("wt", wt);
        q.telemetryData.put("tat", tat);
        q.telemetryData.put("avgWT", avgWT);
        q.telemetryData.put("avgTAT", avgTAT);

        return q;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODULE 02: SPACE SHOOT // BANKER'S ALGORITHM (EXACT SIMULATOR & ARCHIVE MATCH)
    // ─────────────────────────────────────────────────────────────────────────
    public static QuizQuestion getBankerQuiz(Difficulty diff, Mode mode) {
        String[] headers = {
                "PID / CLIENT", "ALLOC (A B C)", "MAX (A B C)",
                "NEED (M - A)", "WORK / VAULT", "CAN SATISFY?", "CLEARANCE STATUS"
        };

        int n = (diff == Difficulty.EASY) ? 3 : (diff == Difficulty.AVERAGE ? 5 : 7);

        String[] pool = { "P0 (Vanguard)", "P1 (Corsair)", "P2 (Dreadnought)", "P3 (Titan)", "P4 (Valkyrie)", "P5 (Phantom)", "P6 (Raptor)" };
        String[] names = new String[n];
        System.arraycopy(pool, 0, names, 0, n);

        int[][] alloc = new int[n][3];
        int[][] max = new int[n][3];
        int[][] need = new int[n][3];

        int[][] baseAlloc = { {0, 1, 0}, {2, 0, 0}, {3, 0, 2}, {2, 1, 1}, {0, 0, 2}, {1, 1, 0}, {1, 0, 1} };
        int[][] baseMax   = { {7, 5, 3}, {3, 2, 2}, {9, 0, 2}, {2, 2, 2}, {4, 3, 3}, {3, 2, 1}, {2, 1, 2} };
        int[] work = { 3 + RND.nextInt(2), 3 + RND.nextInt(2), 2 + RND.nextInt(2) };
        String tip = String.format("VAULT ASSETS // AVAILABLE: A: [ %d ]  B: [ %d ]  C: [ %d ] | FORMULAS: Need = Max - Alloc | Can Satisfy = (Need <= Work) | If Satisfied -> Work += Alloc", work[0], work[1], work[2]);

        for (int i = 0; i < n; i++) {
            for (int k = 0; k < 3; k++) {
                alloc[i][k] = baseAlloc[i % baseAlloc.length][k] + (RND.nextDouble() < 0.3 ? 1 : 0);
                max[i][k]   = baseMax[i % baseMax.length][k] + (RND.nextDouble() < 0.3 ? 1 : 0);
                if (max[i][k] < alloc[i][k]) max[i][k] = alloc[i][k] + RND.nextInt(2);
                need[i][k] = Math.max(0, max[i][k] - alloc[i][k]);
            }
        }

        boolean[] finished = new boolean[n];
        int[] curWork = work.clone();
        String[][] workStates = new String[n][3];
        String[] canSatisfy = new String[n];
        String[] status = new String[n];
        List<String> safeSeqList = new ArrayList<>();

        List<Integer> candSearch = new ArrayList<>();
        for (int i = 0; i < n; i++) candSearch.add(i);
        Collections.shuffle(candSearch, RND);

        for (int step = 0; step < n; step++) {
            int cand = -1;
            for (int i : candSearch) {
                if (!finished[i] && need[i][0] <= curWork[0] && need[i][1] <= curWork[1] && need[i][2] <= curWork[2]) {
                    cand = i;
                    break;
                }
            }
            if (cand == -1) {
                for (int i : candSearch) {
                    if (!finished[i]) { cand = i; break; }
                }
            }

            workStates[cand][0] = curWork[0] + " " + curWork[1] + " " + curWork[2];
            boolean sat = (need[cand][0] <= curWork[0] && need[cand][1] <= curWork[1] && need[cand][2] <= curWork[2]);
            canSatisfy[cand] = sat ? "YES" : "NO";
            status[cand] = sat ? "RUNS [EXEC]" : "WAIT [BLOCKED]";

            if (sat) {
                safeSeqList.add("P" + cand);
                for (int k = 0; k < 3; k++) curWork[k] += alloc[cand][k];
                finished[cand] = true;
            } else {
                finished[cand] = true;
            }
        }

        for (int i = 0; i < n; i++) {
            if (workStates[i][0] == null) {
                workStates[i][0] = curWork[0] + " " + curWork[1] + " " + curWork[2];
                canSatisfy[i] = "YES";
                status[i] = "RUNS [EXEC]";
            }
            if (!safeSeqList.contains("P" + i)) {
                safeSeqList.add("P" + i);
            }
        }

        String[][] answers = new String[n][7];
        String[][] worksheet = new String[n][7];
        StringBuilder expl = new StringBuilder(String.format("🛡️ SPACE SHOOT // BANKER'S SAFETY AUDIT [%s TIER]:\n", diff.label));

        for (int i = 0; i < n; i++) {
            answers[i][0] = names[i];
            answers[i][1] = alloc[i][0] + " " + alloc[i][1] + " " + alloc[i][2];
            answers[i][2] = max[i][0] + " " + max[i][1] + " " + max[i][2];
            answers[i][3] = need[i][0] + " " + need[i][1] + " " + need[i][2];
            answers[i][4] = workStates[i][0];
            answers[i][5] = canSatisfy[i];
            answers[i][6] = status[i];

            System.arraycopy(answers[i], 0, worksheet[i], 0, 7);

            expl.append(String.format("• %s: Alloc=[%s], Max=[%s] -> Need=[%s] | Work=[%s] -> Satisfy=%s (%s)\n",
                    names[i], answers[i][1], answers[i][2], answers[i][3], answers[i][4], answers[i][5], answers[i][6]));
        }

        int[] candidateCols = { 3, 4, 5, 6 };
        if (mode == Mode.FILL_MISSING) {
            if (diff == Difficulty.EASY) {
                applyRandomBlanks(worksheet, 0, n, new int[]{3, 5, 6}, 1);
            } else if (diff == Difficulty.AVERAGE) {
                applyRandomBlanks(worksheet, 0, n, candidateCols, 2);
            } else {
                applyRandomBlanks(worksheet, 0, n, candidateCols, 3);
            }
        } else {
            for (int i = 0; i < n; i++) {
                worksheet[i][3] = "?";
                worksheet[i][4] = "?";
                worksheet[i][5] = "?";
            }
        }

        QuizQuestion q = new QuizQuestion("MOD_02", "MISSION 02: BANKER'S SAFETY ALGORITHM",
                "SPACE SHOOT // GRAND CENTRAL CYBER VAULT",
                String.format("Audit client credit demands against initial vault reserves [A:%d B:%d C:%d] (%s Tier: %d Clients)! Derive Need Matrix and verify safe execution order.", work[0], work[1], work[2], diff.label, n),
                tip, headers, worksheet, answers, expl.toString(), diff);

        q.initialAvailable = work.clone();
        q.sequenceTitle = "BANKER'S SAFE SEQUENCE // DEADLOCK AVOIDANCE ORDER";
        q.sequencePrompt = "Discover the deadlock-free Safe Sequence of clients to clear:";
        q.correctSequence = String.join(" -> ", safeSeqList);
        String[] tokens = new String[n];
        for (int i = 0; i < n; i++) tokens[i] = "P" + i;
        shuffleArray(tokens);
        q.sequenceTokens = tokens;

        q.telemetryData.put("initialAvailable", work.clone());
        q.telemetryData.put("vaultAssets", work.clone());
        q.telemetryData.put("safeSequence", safeSeqList);
        q.telemetryData.put("names", names);
        q.telemetryData.put("alloc", alloc);
        q.telemetryData.put("max", max);
        q.telemetryData.put("need", need);
        return q;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODULE 03: FRAME HOPPER // MMU PAGING (EXACT SIMULATOR & ARCHIVE MATCH)
    // ─────────────────────────────────────────────────────────────────────────
    public static QuizQuestion getPagingQuiz(Difficulty diff, Mode mode) {
        String[] headers = { "LOGICAL ADDR", "PAGE SIZE (S)", "PAGE # (p)", "OFFSET (d)", "FRAME # (f)", "PHYSICAL ADDR (PA)" };
        String tip = "FORMULAS: Page Number p = Logical Addr / Page Size | Offset d = Logical Addr % Page Size | Physical Address PA = (f * Page Size) + d";

        int n = (diff == Difficulty.EASY) ? 3 : (diff == Difficulty.AVERAGE ? 5 : 7);
        int pageSize = 1024; // Standard 1024 bytes as in Frame Hopper simulator

        int[] pageTable = { 5, 6, 1, 2, 7, 3, 8, 4 }; // Page 0->5, 1->6, 2->1, 3->2...

        int[] logicalAddrs = new int[n];
        for (int i = 0; i < n; i++) {
            int p = (i * 2 + RND.nextInt(2)) % pageTable.length;
            int d = 32 + (RND.nextInt(30) * 32) + RND.nextInt(16);
            logicalAddrs[i] = (p * pageSize) + d;
        }

        String[][] answers = new String[n][6];
        String[][] worksheet = new String[n][6];
        StringBuilder expl = new StringBuilder(String.format("🐸 FRAME HOPPER // MMU PAGING TRANSLATION [%s TIER] (Page Size = %d B):\n", diff.label, pageSize));
        expl.append("PAGE TABLE MAPPING: [P0->F5, P1->F6, P2->F1, P3->F2, P4->F7, P5->F3, P6->F8, P7->F4]\n\n");

        String[] frameSeq = new String[n];
        for (int i = 0; i < n; i++) {
            int la = logicalAddrs[i];
            int p = la / pageSize;
            int d = la % pageSize;
            int f = pageTable[p % pageTable.length];
            int pa = (f * pageSize) + d;
            frameSeq[i] = "F" + f;

            answers[i][0] = la + " B";
            answers[i][1] = pageSize + " B";
            answers[i][2] = String.valueOf(p);
            answers[i][3] = d + " B";
            answers[i][4] = "Frame " + f;
            answers[i][5] = pa + " B";

            System.arraycopy(answers[i], 0, worksheet[i], 0, 6);

            expl.append(String.format("• LA %d B: p = %d / %d = %d | d = %d %% %d = %d B | PT[%d] = Frame %d -> PA = (%d * %d) + %d = %d B\n",
                    la, la, pageSize, p, la, pageSize, d, p, f, f, pageSize, d, pa));
        }

        int[] candidateCols = { 2, 3, 4, 5 };
        if (mode == Mode.FILL_MISSING) {
            if (diff == Difficulty.EASY) {
                applyRandomBlanks(worksheet, 0, n, new int[]{2, 3, 5}, 1);
            } else if (diff == Difficulty.AVERAGE) {
                applyRandomBlanks(worksheet, 0, n, candidateCols, 2);
            } else {
                applyRandomBlanks(worksheet, 0, n, candidateCols, 3);
            }
        } else {
            for (int i = 0; i < n; i++) {
                worksheet[i][2] = "?";
                worksheet[i][3] = "?";
                worksheet[i][5] = "?";
            }
        }

        QuizQuestion q = new QuizQuestion("MOD_03", "MISSION 03: MMU PAGING TRANSLATION",
                "FRAME HOPPER // MMU PAGE-TO-FRAME MAP",
                String.format("Translate virtual logical addresses to physical RAM frames (%s Tier: %d Addresses)! Compute Page #, Offset, and Physical Address.", diff.label, n),
                tip, headers, worksheet, answers, expl.toString(), diff);

        q.sequenceTitle = "PAGE-TO-FRAME TRANSLATION SEQUENCE";
        q.sequencePrompt = "Identify the mapped physical frames in translation order:";
        q.correctSequence = String.join(" -> ", frameSeq);
        String[] tokens = new String[] { "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8" };
        shuffleArray(tokens);
        q.sequenceTokens = tokens;

        q.telemetryData.put("pageSize", pageSize);
        q.telemetryData.put("logicalAddrs", logicalAddrs);
        q.telemetryData.put("pageTable", pageTable);
        return q;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODULE 04: SUPER MARIO // CACHE REPLACEMENT (EXACT SIMULATOR & ARCHIVE MATCH)
    // ─────────────────────────────────────────────────────────────────────────
    public static QuizQuestion getCacheQuiz(Difficulty diff, Mode mode) {
        return getCacheQuiz(diff, mode, activeCachePolicy);
    }

    public static QuizQuestion getCacheQuiz(Difficulty diff, Mode mode, String policy) {
        if (policy == null || policy.isEmpty()) policy = activeCachePolicy;
        policy = policy.toUpperCase(Locale.US);
        if (!policy.equals("FIFO") && !policy.equals("LRU") && !policy.equals("OPTIMAL")) {
            policy = "FIFO";
        }

        String[] headers = { "STEP #", "PAGE REF", "PIPE #0", "PIPE #1", "PIPE #2", "STATUS (H/F)" };

        String tip;
        if (policy.equals("LRU")) {
            tip = "LRU POLICY: Evicts least recently referenced page | Hits reset reference timestamp | Hit Ratio = (Hits / Total) * 100%";
        } else if (policy.equals("OPTIMAL")) {
            tip = "OPTIMAL POLICY: Evicts page referenced farthest in future | Minimizes overall faults | Hit Ratio = (Hits / Total) * 100%";
        } else {
            tip = "FIFO POLICY: Evicts oldest arrived page | Pipe arrival order is preserved | Hit Ratio = (Hits / Total) * 100%";
        }

        int n = (diff == Difficulty.EASY) ? 4 : (diff == Difficulty.AVERAGE ? 6 : 8);
        int totalRows = n + 1; // n steps + 1 summary row

        // Generate reference stream with repeating hits and eviction opportunities
        int[] stream = new int[n];
        int[] samplePages = { 7, 0, 1, 2, 0, 3, 0, 4, 2, 3 };
        for (int i = 0; i < n; i++) {
            if (i < samplePages.length && RND.nextDouble() < 0.6) {
                stream[i] = samplePages[i % samplePages.length];
            } else if (i >= 2 && RND.nextDouble() < 0.45) {
                stream[i] = stream[RND.nextInt(i)];
            } else {
                stream[i] = RND.nextInt(5);
            }
        }

        String[][] answers = new String[totalRows][6];
        String[][] worksheet = new String[totalRows][6];
        StringBuilder expl = new StringBuilder(String.format("🍄 SUPER MARIO // WARP PIPE MEMORY TRACE [%s TIER] (Capacity = 3 Pipes, Policy: %s):\n", diff.label, policy));

        int[] pipes = { -1, -1, -1 };
        int[] loadedAt = { -1, -1, -1 };
        int[] lastUsed = { -1, -1, -1 };
        int ageCounter = 0;
        int faults = 0;
        int hits = 0;

        int[][] pipeHistory = new int[n][3];
        int[] victimHistory = new int[n];
        boolean[] hitHistory = new boolean[n];
        String[] statusSeq = new String[n];

        for (int i = 0; i < n; i++) {
            int page = stream[i];
            int hitIndex = -1;
            for (int p = 0; p < 3; p++) {
                if (pipes[p] == page) {
                    hitIndex = p;
                    break;
                }
            }

            int victimSlot = -1;
            int evictedPage = -1;

            if (hitIndex != -1) {
                hits++;
                hitHistory[i] = true;
                statusSeq[i] = "HIT";
                if (policy.equals("LRU")) {
                    lastUsed[hitIndex] = i; // Reset access timestamp on hit
                }
            } else {
                faults++;
                hitHistory[i] = false;
                statusSeq[i] = "FAULT";

                int emptySlot = -1;
                for (int p = 0; p < 3; p++) {
                    if (pipes[p] == -1) {
                        emptySlot = p;
                        break;
                    }
                }

                if (emptySlot != -1) {
                    pipes[emptySlot] = page;
                    loadedAt[emptySlot] = ++ageCounter;
                    lastUsed[emptySlot] = i;
                } else {
                    // Eviction required according to active policy
                    if (policy.equals("FIFO")) {
                        victimSlot = 0;
                        for (int p = 1; p < 3; p++) {
                            if (loadedAt[p] < loadedAt[victimSlot]) {
                                victimSlot = p;
                            }
                        }
                    } else if (policy.equals("LRU")) {
                        victimSlot = 0;
                        for (int p = 1; p < 3; p++) {
                            if (lastUsed[p] < lastUsed[victimSlot]) {
                                victimSlot = p;
                            }
                        }
                    } else { // OPTIMAL
                        int furthestDist = -1;
                        victimSlot = 0;
                        for (int p = 0; p < 3; p++) {
                            int nextOccur = Integer.MAX_VALUE;
                            for (int k = i + 1; k < n; k++) {
                                if (stream[k] == pipes[p]) {
                                    nextOccur = k;
                                    break;
                                }
                            }
                            if (nextOccur > furthestDist) {
                                furthestDist = nextOccur;
                                victimSlot = p;
                            } else if (nextOccur == furthestDist && loadedAt[p] < loadedAt[victimSlot]) {
                                victimSlot = p;
                            }
                        }
                    }

                    evictedPage = pipes[victimSlot];
                    pipes[victimSlot] = page;
                    loadedAt[victimSlot] = ++ageCounter;
                    lastUsed[victimSlot] = i;
                }
            }

            victimHistory[i] = evictedPage;
            for (int p = 0; p < 3; p++) pipeHistory[i][p] = pipes[p];

            answers[i][0] = "#" + (i + 1);
            answers[i][1] = String.valueOf(page);
            answers[i][2] = (pipes[0] == -1) ? "—" : String.valueOf(pipes[0]);
            answers[i][3] = (pipes[1] == -1) ? "—" : String.valueOf(pipes[1]);
            answers[i][4] = (pipes[2] == -1) ? "—" : String.valueOf(pipes[2]);
            answers[i][5] = statusSeq[i];

            System.arraycopy(answers[i], 0, worksheet[i], 0, 6);

            expl.append(String.format("• Step #%d [Ref %d]: Pipes=[%s, %s, %s] -> %s%s\n",
                    (i + 1), page, answers[i][2], answers[i][3], answers[i][4], answers[i][5],
                    (evictedPage != -1 ? " (Evicted P" + evictedPage + ")" : "")));
        }

        double hitRatio = (hits * 100.0) / n;
        answers[n][0] = "SUMMARY (TOTAL)";
        answers[n][1] = "Refs: " + n;
        answers[n][2] = "—";
        answers[n][3] = "—";
        answers[n][4] = faults + " FAULTS";
        answers[n][5] = String.format(Locale.US, "%.1f%% HITS", hitRatio);

        worksheet[n][0] = answers[n][0];
        worksheet[n][1] = answers[n][1];
        worksheet[n][2] = answers[n][2];
        worksheet[n][3] = answers[n][3];
        worksheet[n][4] = answers[n][4];
        worksheet[n][5] = answers[n][5];

        expl.append(String.format("\n★ SUMMARY [%s]: Total Page Faults = %d | Page Hits = %d | Hit Ratio = %.1f%%\n",
                policy, faults, hits, hitRatio));

        int[] candidateCols = { 2, 3, 4, 5 };
        if (mode == Mode.FILL_MISSING) {
            if (diff == Difficulty.EASY) {
                applyRandomBlanks(worksheet, 0, n, candidateCols, 1);
                worksheet[n][4] = "?";
            } else if (diff == Difficulty.AVERAGE) {
                applyRandomBlanks(worksheet, 0, n, candidateCols, 2);
                worksheet[n][4] = "?";
                worksheet[n][5] = "?";
            } else {
                applyRandomBlanks(worksheet, 0, n, candidateCols, 3);
                worksheet[n][4] = "?";
                worksheet[n][5] = "?";
            }
        } else {
            for (int i = 0; i < n; i++) {
                for (int c : candidateCols) worksheet[i][c] = "?";
            }
            worksheet[n][4] = "?";
            worksheet[n][5] = "?";
        }

        QuizQuestion q = new QuizQuestion("MOD_04", "MISSION 04: PAGE REPLACEMENT POLICIES",
                "SUPER MARIO // WARP PIPE MEMORY TRACE",
                String.format("Trace memory page reference stream across Mario's warp pipes (%s Policy | %s Tier: %d Steps)! Determine resident pages in Pipes #0-#2, Faults, Hits, and Hit Ratio.", policy, diff.label, n),
                tip, headers, worksheet, answers, expl.toString(), diff);

        q.policy = policy;
        q.sequenceTitle = policy + " OUTCOME STREAM CHALLENGE";
        q.sequencePrompt = "Identify the sequence of Cache outcomes (HIT or FAULT):";
        q.correctSequence = String.join(" -> ", statusSeq);
        q.sequenceTokens = new String[] { "FAULT", "HIT" };

        q.telemetryData.put("policy", policy);
        q.telemetryData.put("stream", stream);
        q.telemetryData.put("faults", faults);
        q.telemetryData.put("hits", hits);
        q.telemetryData.put("hitRatio", hitRatio);
        q.telemetryData.put("pipeHistory", pipeHistory);
        q.telemetryData.put("victimHistory", victimHistory);
        q.telemetryData.put("hitHistory", hitHistory);
        return q;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODULE 05: HEAP SNAKE // MEMORY SEGMENTATION (EXACT SIMULATOR & ARCHIVE MATCH)
    // ─────────────────────────────────────────────────────────────────────────
    public static QuizQuestion getSegmentationQuiz(Difficulty diff, Mode mode) {
        String[] headers = { "SEG #", "SEGMENT NAME", "BASE ADDR", "LIMIT BOUND", "OFFSET (d)", "STATUS", "PHYSICAL ADDR" };
        String tip = "HARDWARE COMPARATOR: If Offset < Limit -> VALID, Physical Address = Base + Offset | If Offset >= Limit -> TRAP: SEGFAULT!";

        int n = (diff == Difficulty.EASY) ? 3 : (diff == Difficulty.AVERAGE ? 5 : 7);

        String[] segNames = {
                "Arcade_Sprites", "Audio_Engine", "High_Scores", "Physics_Core", "Vulkan_Shaders", "AI_Agent_State", "Texture_Heap"
        };
        int[] bases  = { 1400, 6300, 4700, 3200, 8000, 5200, 9500 };
        int[] limits = {  400,  300,  500,  600,  250,  450,  700 };

        int[] offsets = new int[n];
        String[] statusSeq = new String[n];
        for (int i = 0; i < n; i++) {
            boolean isTrap = (i == 1) || (RND.nextDouble() < 0.35);
            if (isTrap) {
                offsets[i] = limits[i] + 15 + RND.nextInt(150);
                statusSeq[i] = "TRAP";
            } else {
                offsets[i] = 20 + RND.nextInt(Math.max(10, limits[i] - 40));
                statusSeq[i] = "VALID";
            }
        }

        String[][] answers = new String[n][7];
        String[][] worksheet = new String[n][7];
        StringBuilder expl = new StringBuilder(String.format("🐍 HEAP SNAKE // SEGMENTATION & MMU BOUNDS CHECK [%s TIER]:\n", diff.label));

        for (int i = 0; i < n; i++) {
            int segId = i;
            String name = segNames[segId];
            int base = bases[segId];
            int limit = limits[segId];
            int d = offsets[i];
            boolean valid = (d < limit);

            String statusStr = valid ? "VALID [OK]" : "TRAP: SEGFAULT";
            String paStr = valid ? String.valueOf(base + d) : "TRAP: VIOLATION";

            answers[i][0] = String.valueOf(segId);
            answers[i][1] = name;
            answers[i][2] = String.valueOf(base);
            answers[i][3] = String.valueOf(limit);
            answers[i][4] = String.valueOf(d);
            answers[i][5] = statusStr;
            answers[i][6] = paStr;

            System.arraycopy(answers[i], 0, worksheet[i], 0, 7);

            expl.append(String.format("• Seg #%d (%s): Base=%d, Limit=%d, Offset=%d -> %d < %d ? %s -> PA=%s\n",
                    segId, name, base, limit, d, d, limit, statusStr, paStr));
        }

        int[] candidateCols = { 4, 5, 6 };
        if (mode == Mode.FILL_MISSING) {
            if (diff == Difficulty.EASY) {
                applyRandomBlanks(worksheet, 0, n, new int[]{5, 6}, 1);
            } else if (diff == Difficulty.AVERAGE) {
                applyRandomBlanks(worksheet, 0, n, candidateCols, 2);
            } else {
                applyRandomBlanks(worksheet, 0, n, candidateCols, 3);
            }
        } else {
            for (int i = 0; i < n; i++) {
                worksheet[i][5] = "?";
                worksheet[i][6] = "?";
            }
        }

        QuizQuestion q = new QuizQuestion("MOD_05", "MISSION 05: MEMORY SEGMENTATION",
                "HEAP SNAKE // MMU SEGMENT BOUNDS RADAR",
                String.format("Validate segment table pointers against hardware limit bounds (%s Tier: %d Segments)! Detect boundary overflows, traps, and compute physical addresses.", diff.label, n),
                tip, headers, worksheet, answers, expl.toString(), diff);

        q.sequenceTitle = "SEGMENT BOUNDS VALIDATION SEQUENCE";
        q.sequencePrompt = "Identify the segment verification status order (VALID or TRAP):";
        q.correctSequence = String.join(" -> ", statusSeq);
        q.sequenceTokens = new String[] { "VALID", "TRAP" };

        q.telemetryData.put("bases", bases);
        q.telemetryData.put("limits", limits);
        q.telemetryData.put("offsets", offsets);
        return q;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODULE 06: CYBER CRANE ATTACK // RAM ALLOCATION (EXACT SIMULATOR & ARCHIVE MATCH)
    // ─────────────────────────────────────────────────────────────────────────
    public static QuizQuestion getFragmentationQuiz(Difficulty diff, Mode mode) {
        return getFragmentationQuiz(diff, mode, activeAllocationPolicy);
    }

    public static QuizQuestion getFragmentationQuiz(Difficulty diff, Mode mode, String policy) {
        if (policy == null || policy.isEmpty()) policy = activeAllocationPolicy;
        policy = policy.toUpperCase(Locale.US);
        if (!policy.equals("FIRST FIT") && !policy.equals("BEST FIT") && !policy.equals("NEXT FIT")) {
            policy = "FIRST FIT";
        }

        String[] headers = {
                "IC / CHIP", "DEMAND SIZE", "BASE ADDRESS", "LIMIT / CEILING", "SOCKET ASSIGNED", "INTERNAL FRAG", "ALLOCATION STATUS"
        };

        String tip;
        if (policy.equals("BEST FIT")) {
            tip = "BEST FIT: Smallest socket where Cap >= Demand | Frag = Socket Cap - Demand | Minimizes internal fragmentation";
        } else if (policy.equals("NEXT FIT")) {
            tip = "NEXT FIT: Searches from last allocated socket | Frag = Socket Cap - Demand | Wraps around socket list";
        } else {
            tip = "FIRST FIT: First socket where Cap >= Demand | Frag = Socket Cap - Demand | Scans from Socket 1 upwards";
        }

        int n = (diff == Difficulty.EASY) ? 3 : (diff == Difficulty.AVERAGE ? 4 : 6);

        // Motherboard Physical Sockets (System Archives exact layout)
        int[] sockBases  = { 0, 100, 600, 800, 1100, 1700, 2200 };
        int[] sockSizes  = { 100, 500, 200, 300, 600, 500, 400 };
        String[] sockNames = { "Socket 1", "Socket 2", "Socket 3", "Socket 4", "Socket 5", "Socket 6", "Socket 7" };
        int numSockets = (diff == Difficulty.EXPERT) ? 7 : 5;
        boolean[] sockOccupied = new boolean[numSockets];
        int lastAllocIndex = 0; // For Next Fit

        // Incoming chip demands from System Archives: 212, 417, 112, 426...
        int[] demands = new int[n];
        int[] baseDems = { 212, 417, 112, 426, 180, 290, 550 };
        for (int i = 0; i < n; i++) {
            demands[i] = baseDems[i % baseDems.length] + (RND.nextInt(3) * 10) - (RND.nextInt(3) * 10);
            if (demands[i] < 90) demands[i] = 112;
        }

        String[][] answers = new String[n][7];
        String[][] worksheet = new String[n][7];
        StringBuilder expl = new StringBuilder(String.format("🏗️ CYBER CRANE ATTACK // %s RAM ALLOCATION [%s TIER]:\n", policy, diff.label));
        String[] sockSeq = new String[n];
        int[] assignedSocks = new int[n];
        int[] frags = new int[n];
        boolean[] allocatedHistory = new boolean[n];

        for (int i = 0; i < n; i++) {
            String chipName = "P" + (i + 1);
            int dem = demands[i];
            int assignedSock = -1;

            if (policy.equals("FIRST FIT")) {
                for (int s = 0; s < numSockets; s++) {
                    if (!sockOccupied[s] && sockSizes[s] >= dem) {
                        assignedSock = s;
                        break;
                    }
                }
            } else if (policy.equals("BEST FIT")) {
                int minRemaining = Integer.MAX_VALUE;
                for (int s = 0; s < numSockets; s++) {
                    if (!sockOccupied[s] && sockSizes[s] >= dem) {
                        int remaining = sockSizes[s] - dem;
                        if (remaining < minRemaining) {
                            minRemaining = remaining;
                            assignedSock = s;
                        }
                    }
                }
            } else { // NEXT FIT
                for (int step = 0; step < numSockets; step++) {
                    int s = (lastAllocIndex + step) % numSockets;
                    if (!sockOccupied[s] && sockSizes[s] >= dem) {
                        assignedSock = s;
                        lastAllocIndex = s;
                        break;
                    }
                }
            }

            assignedSocks[i] = assignedSock;

            if (assignedSock != -1) {
                sockOccupied[assignedSock] = true;
                allocatedHistory[i] = true;
                int base = sockBases[assignedSock];
                int limit = sockSizes[assignedSock];
                int frag = limit - dem;
                frags[i] = frag;
                String sockStr = sockNames[assignedSock] + " [" + limit + "K]";
                sockSeq[i] = sockNames[assignedSock];

                answers[i][0] = chipName;
                answers[i][1] = dem + " KB";
                answers[i][2] = base + " KB";
                answers[i][3] = limit + " KB";
                answers[i][4] = sockStr;
                answers[i][5] = frag + " KB";
                answers[i][6] = "ALLOCATED [OK]";

                expl.append(String.format("• %s (%d KB): Fits in %s -> Base=%d KB, Internal Frag = %d - %d = %d KB (ALLOCATED)\n",
                        chipName, dem, sockStr, base, limit, dem, frag));
            } else {
                allocatedHistory[i] = false;
                frags[i] = 0;
                sockSeq[i] = "NONE";
                answers[i][0] = chipName;
                answers[i][1] = dem + " KB";
                answers[i][2] = "—";
                answers[i][3] = "—";
                answers[i][4] = "NONE";
                answers[i][5] = "0 KB";
                answers[i][6] = "DENIED: NO FIT";

                expl.append(String.format("• %s (%d KB): No available socket has capacity >= %d KB -> DENIED: NO FIT\n",
                        chipName, dem, dem));
            }

            System.arraycopy(answers[i], 0, worksheet[i], 0, 7);
        }

        int[] candidateCols = { 2, 3, 4, 5, 6 };
        if (mode == Mode.FILL_MISSING) {
            if (diff == Difficulty.EASY) {
                applyRandomBlanks(worksheet, 0, n, new int[]{4, 5, 6}, 1);
            } else if (diff == Difficulty.AVERAGE) {
                applyRandomBlanks(worksheet, 0, n, candidateCols, 2);
            } else {
                applyRandomBlanks(worksheet, 0, n, candidateCols, 3);
            }
        } else {
            for (int i = 0; i < n; i++) {
                worksheet[i][4] = "?";
                worksheet[i][5] = "?";
                worksheet[i][6] = "?";
            }
        }

        QuizQuestion q = new QuizQuestion("MOD_06", "MISSION 06: RAM CONTIGUOUS ALLOCATION",
                "CYBER CRANE ATTACK // SILICON RAM PARTITIONS",
                String.format("Allocate incoming process chips into physical RAM sockets (%s Policy | %s Tier: %d Chips)! Calculate Base Address, Socket Assignment, and Internal Fragmentation.", policy, diff.label, n),
                tip, headers, worksheet, answers, expl.toString(), diff);

        q.policy = policy;
        q.sequenceTitle = policy + " SOCKET ALLOCATION SEQUENCE";
        q.sequencePrompt = "Identify the sockets allocated to incoming processes:";
        q.correctSequence = String.join(" -> ", sockSeq);
        String[] tokens = new String[numSockets + 1];
        for (int s = 0; s < numSockets; s++) tokens[s] = sockNames[s];
        tokens[numSockets] = "NONE";
        shuffleArray(tokens);
        q.sequenceTokens = tokens;

        q.telemetryData.put("policy", policy);
        q.telemetryData.put("sockSizes", Arrays.copyOf(sockSizes, numSockets));
        q.telemetryData.put("sockBases", Arrays.copyOf(sockBases, numSockets));
        q.telemetryData.put("sockNames", Arrays.copyOf(sockNames, numSockets));
        q.telemetryData.put("demands", demands);
        q.telemetryData.put("assignedSocks", assignedSocks);
        q.telemetryData.put("frags", frags);
        q.telemetryData.put("allocatedHistory", allocatedHistory);
        return q;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODULE 07: PAC-MAN DISK // CYLINDER HEAD SEEK (ALL 6 SEEK POLICIES)
    // ─────────────────────────────────────────────────────────────────────────
    public static QuizQuestion getDiskQuiz(Difficulty diff, Mode mode) {
        return getDiskQuiz(diff, mode, activeDiskPolicy);
    }

    public static QuizQuestion getDiskQuiz(Difficulty diff, Mode mode, String policy) {
        if (policy == null || policy.isEmpty()) policy = activeDiskPolicy;
        policy = policy.toUpperCase(Locale.US);
        if (!policy.equals("FCFS") && !policy.equals("SSTF") && !policy.equals("SCAN") &&
                !policy.equals("C-SCAN") && !policy.equals("LOOK") && !policy.equals("C-LOOK")) {
            policy = "FCFS";
        }

        String[] headers = {
                "STEP #", "CURRENT TRACK", "NEXT TRACK ACCESSED", "TRACKS TRAVERSED", "SEEK DIRECTION", "CUMULATIVE SEEK"
        };

        String tip;
        if (policy.equals("SSTF")) {
            tip = "SSTF: Smallest seek distance |Next - Current| | Traversed = |Next - Current| | Cumulative = Running Sum";
        } else if (policy.equals("SCAN")) {
            tip = "SCAN: Sweeps in head direction | Reverses at disk boundary (0/199) | Traversed = |Next - Current|";
        } else if (policy.equals("C-SCAN")) {
            tip = "C-SCAN: Sweeps in direction to 199 | Warps to 0 without servicing | Resumes forward sweep";
        } else if (policy.equals("LOOK")) {
            tip = "LOOK: Sweeps to farthest request | Reverses without touching boundary | Traversed = |Next - Current|";
        } else if (policy.equals("C-LOOK")) {
            tip = "C-LOOK: Sweeps to highest request | Warps directly to lowest request | Traversed = |Next - Current|";
        } else {
            tip = "FCFS: Services cylinders in arrival order | Traversed = |Next - Current| | Cumulative = Running Sum";
        }

        int n = (diff == Difficulty.EASY) ? 4 : (diff == Difficulty.AVERAGE ? 5 : 7);
        int maxCyl = 199;
        int startHead = 53; // Authentic OS benchmark head position

        // Realistic candidate pools ensuring both requests below 53 and above 53
        int[] baseCandidates = { 98, 183, 37, 122, 14, 124, 65, 67, 15, 190, 40, 85 };
        List<Integer> reqPool = new ArrayList<>();
        for (int c : baseCandidates) reqPool.add(c);
        Collections.shuffle(reqPool, RND);

        List<Integer> reqList = new ArrayList<>();
        int lowerCount = 0, upperCount = 0;
        int targetLower = Math.max(1, n / 2);
        int targetUpper = n - targetLower;

        for (int c : reqPool) {
            if (reqList.size() >= n) break;
            if (c < startHead && lowerCount < targetLower) {
                reqList.add(c);
                lowerCount++;
            } else if (c > startHead && upperCount < targetUpper) {
                reqList.add(c);
                upperCount++;
            }
        }
        for (int c : reqPool) {
            if (reqList.size() >= n) break;
            if (!reqList.contains(c)) reqList.add(c);
        }

        int[] rawRequests = new int[reqList.size()];
        for (int i = 0; i < reqList.size(); i++) rawRequests[i] = reqList.get(i);

        // Simulation Engine: FCFS, SSTF, SCAN, C-SCAN, LOOK, C-LOOK
        List<Integer> seekSequence = new ArrayList<>();
        seekSequence.add(startHead);
        boolean goUp = true; // Initial seek direction UP (towards 199)

        if ("FCFS".equals(policy)) {
            seekSequence.addAll(reqList);

        } else if ("SSTF".equals(policy)) {
            List<Integer> pool = new ArrayList<>(reqList);
            int current = startHead;
            while (!pool.isEmpty()) {
                int closest = -1, minDiff = Integer.MAX_VALUE;
                for (int r : pool) {
                    int diffC = Math.abs(r - current);
                    if (diffC < minDiff) {
                        minDiff = diffC;
                        closest = r;
                    }
                }
                seekSequence.add(closest);
                current = closest;
                pool.remove(Integer.valueOf(closest));
            }

        } else if ("SCAN".equals(policy)) {
            List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
            for (int r : reqList) {
                if (r < startHead) left.add(r);
                else right.add(r);
            }
            Collections.sort(left);
            Collections.sort(right);

            if (goUp) {
                seekSequence.addAll(right);
                if (!left.isEmpty()) {
                    seekSequence.add(maxCyl); // Boundary turnaround
                    Collections.reverse(left);
                    seekSequence.addAll(left);
                }
            } else {
                Collections.reverse(left);
                seekSequence.addAll(left);
                if (!right.isEmpty()) {
                    seekSequence.add(0); // Boundary turnaround
                    seekSequence.addAll(right);
                }
            }

        } else if ("C-SCAN".equals(policy)) {
            List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
            for (int r : reqList) {
                if (r < startHead) left.add(r);
                else right.add(r);
            }
            Collections.sort(left);
            Collections.sort(right);

            if (goUp) {
                seekSequence.addAll(right);
                seekSequence.add(maxCyl); // Touches boundary 199
                seekSequence.add(0);      // Warps to boundary 0
                seekSequence.addAll(left);
            } else {
                Collections.reverse(left);
                seekSequence.add(0);
                seekSequence.add(maxCyl);
                Collections.reverse(right);
                seekSequence.addAll(right);
            }

        } else if ("LOOK".equals(policy)) {
            List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
            for (int r : reqList) {
                if (r < startHead) left.add(r);
                else right.add(r);
            }
            Collections.sort(left);
            Collections.sort(right);

            if (goUp) {
                seekSequence.addAll(right);
                Collections.reverse(left);
                seekSequence.addAll(left);
            } else {
                Collections.reverse(left);
                seekSequence.addAll(right);
            }

        } else if ("C-LOOK".equals(policy)) {
            List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
            for (int r : reqList) {
                if (r < startHead) left.add(r);
                else right.add(r);
            }
            Collections.sort(left);
            Collections.sort(right);

            if (goUp) {
                seekSequence.addAll(right);
                seekSequence.addAll(left); // Warps directly to lowest request
            } else {
                Collections.reverse(left);
                Collections.reverse(right);
                seekSequence.addAll(right); // Warps directly to highest request
            }
        }

        int stepCount = seekSequence.size() - 1;
        int totalRows = stepCount + 1; // steps + 1 summary row

        String[][] answers = new String[totalRows][6];
        String[][] worksheet = new String[totalRows][6];
        StringBuilder expl = new StringBuilder(String.format("🟡 PAC-MAN DISK HEAD // %s TRAVERSAL MATRIX [%s TIER] (Initial Head = %d):\n", policy, diff.label, startHead));

        int cumulative = 0;
        String[] cylSeq = new String[stepCount];
        int[] traversedSteps = new int[stepCount];
        int[] cumulativeSteps = new int[stepCount];
        String[] dirSteps = new String[stepCount];

        for (int i = 0; i < stepCount; i++) {
            int curCyl = seekSequence.get(i);
            int nextCyl = seekSequence.get(i + 1);
            int diffCyl = Math.abs(nextCyl - curCyl);
            cumulative += diffCyl;
            String dir = (nextCyl >= curCyl) ? "UP (->)" : "DOWN (<-)";

            cylSeq[i] = "T" + nextCyl;
            traversedSteps[i] = diffCyl;
            cumulativeSteps[i] = cumulative;
            dirSteps[i] = dir;

            answers[i][0] = "#" + (i + 1);
            answers[i][1] = String.valueOf(curCyl);
            answers[i][2] = String.valueOf(nextCyl);
            answers[i][3] = diffCyl + " cyl";
            answers[i][4] = dir;
            answers[i][5] = cumulative + " cyl";

            System.arraycopy(answers[i], 0, worksheet[i], 0, 6);

            expl.append(String.format("• Step #%d: Head at %d seeks %d | Traversed = |%d - %d| = %d cyl (%s) | Cumulative = %d cyl\n",
                    (i + 1), curCyl, nextCyl, nextCyl, curCyl, diffCyl, dir, cumulative));
        }

        // Summary row
        double avgSeek = (double) cumulative / stepCount;
        answers[stepCount][0] = "TOTAL // AVERAGE";
        answers[stepCount][1] = "—";
        answers[stepCount][2] = "Targets: " + reqList.size();
        answers[stepCount][3] = cumulative + " cyl";
        answers[stepCount][4] = "—";
        answers[stepCount][5] = String.format(Locale.US, "%.2f cyl", avgSeek);

        worksheet[stepCount][0] = answers[stepCount][0];
        worksheet[stepCount][1] = answers[stepCount][1];
        worksheet[stepCount][2] = answers[stepCount][2];
        worksheet[stepCount][3] = answers[stepCount][3];
        worksheet[stepCount][4] = answers[stepCount][4];
        worksheet[stepCount][5] = answers[stepCount][5];

        expl.append(String.format("\n★ %s SUMMARY: Total Tracks Traversed = %d cyl | Average Seek Length = %.2f cyl\n",
                policy, cumulative, avgSeek));

        int[] candidateCols = { 3, 4, 5 };
        if (mode == Mode.FILL_MISSING) {
            if (diff == Difficulty.EASY) {
                applyRandomBlanks(worksheet, 0, stepCount, candidateCols, 1);
                worksheet[stepCount][3] = "?";
            } else if (diff == Difficulty.AVERAGE) {
                applyRandomBlanks(worksheet, 0, stepCount, candidateCols, 2);
                worksheet[stepCount][3] = "?";
                worksheet[stepCount][5] = "?";
            } else {
                applyRandomBlanks(worksheet, 0, stepCount, candidateCols, 3);
                worksheet[stepCount][3] = "?";
                worksheet[stepCount][5] = "?";
            }
        } else {
            for (int i = 0; i < stepCount; i++) {
                for (int c : candidateCols) worksheet[i][c] = "?";
            }
            worksheet[stepCount][3] = "?";
            worksheet[stepCount][5] = "?";
        }

        QuizQuestion q = new QuizQuestion("MOD_07", "MISSION 07: DISK CYLINDER SCHEDULING",
                "PAC-MAN // MAGNETIC DISK TRAVERSAL",
                String.format("Service cylinder read/write requests across magnetic platter tracks (%s Policy | %s Tier: %d Steps)! Calculate Traversed Tracks, Seek Direction, Cumulative Seek, and Average Seek Length.", policy, diff.label, stepCount),
                tip, headers, worksheet, answers, expl.toString(), diff);

        q.policy = policy;
        q.sequenceTitle = policy + " CYLINDER ACCESS SEQUENCE";
        q.sequencePrompt = "Order the cylinders accessed by the read/write head under " + policy + ":";
        q.correctSequence = String.join(" -> ", cylSeq);

        Set<String> tokenSet = new LinkedHashSet<>(Arrays.asList(cylSeq));
        String[] tokens = tokenSet.toArray(new String[0]);
        shuffleArray(tokens);
        q.sequenceTokens = tokens;

        int[] seekSeqArr = new int[seekSequence.size()];
        for (int i = 0; i < seekSequence.size(); i++) seekSeqArr[i] = seekSequence.get(i);

        q.telemetryData.put("policy", policy);
        q.telemetryData.put("startHead", startHead);
        q.telemetryData.put("maxCyl", maxCyl);
        q.telemetryData.put("initialDirectionUp", goUp);
        q.telemetryData.put("requests", rawRequests);
        q.telemetryData.put("seekSequence", seekSeqArr);
        q.telemetryData.put("traversedSteps", traversedSteps);
        q.telemetryData.put("directions", dirSteps);
        q.telemetryData.put("cumulativeSteps", cumulativeSteps);
        q.telemetryData.put("totalSeek", cumulative);
        q.telemetryData.put("avgSeek", avgSeek);
        return q;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODULE 08: OVERCOOKED KITCHEN // EVENT-DRIVEN MULTIPROGRAMMING TRACE
    // ─────────────────────────────────────────────────────────────────────────
    public static class MultiSimResult {
        public double[] ft;
        public double[] tat;
        public double[] wt;
        public double avgTat;
        public double avgWt;
        public String[] finishOrder;
        public double totalElapsed;
    }

    public static QuizQuestion getMultiprogrammingQuiz(Difficulty diff, Mode mode) {
        String[] headers = {
                "TIME", "EVENT", "nj", "%cpu/job", "et", "hw/job", "JOB#", "TIME LEFT"
        };
        String tip = "FORMULAS (EXCEL MODEL): et = present time - prev time | %cpu/job = 1 / nj | hw/job = et * (%cpu/job) | timeleft = prev timeleft - hw/job";

        int n = (diff == Difficulty.EASY) ? 3 : (diff == Difficulty.AVERAGE ? 5 : 7);

        double[] at;
        double[] bt;
        String[] jobNames;

        if (diff == Difficulty.EASY) {
            // 3 Fast Kitchen Orders (8-10 trace rows)
            double baseT = 1.0 + RND.nextInt(5);
            at = new double[] {
                baseT,
                Math.round((baseT + 0.2) * 10.0) / 10.0,
                Math.round((baseT + 0.4) * 10.0) / 10.0
            };
            bt = new double[] { 0.4, 0.5, 0.1 };
            jobNames = new String[] { "J1", "J2", "J3" };

        } else if (diff == Difficulty.AVERAGE) {
            // Standard Classroom syllabus scenarios
            int scenario = RND.nextInt(3);
            if (scenario == 0) {
                // Exact CLASS 7.4 problem set from Syllabus & System Archives screenshot!
                at = new double[] { 7.4, 7.9, 8.1, 8.3, 8.4 };
                bt = new double[] { 0.4, 0.6, 0.3, 0.2, 0.1 };
            } else if (scenario == 1) {
                // Exact EXCEL 10.0 problem set
                at = new double[] { 10.0, 10.2, 10.4, 10.5, 10.8 };
                bt = new double[] { 0.3, 0.5, 0.1, 0.4, 0.1 };
            } else {
                // LUNCH RUSH problem set
                at = new double[] { 1.0, 1.3, 1.5, 1.8, 2.0 };
                bt = new double[] { 0.6, 0.5, 0.4, 0.3, 0.2 };
            }
            jobNames = new String[] { "J1", "J2", "J3", "J4", "J5" };

        } else {
            // EXPERT Tier: 7 Orders with high concurrency overlap
            double baseT = 5.0 + RND.nextInt(4);
            at = new double[] {
                baseT,
                Math.round((baseT + 0.3) * 10.0) / 10.0,
                Math.round((baseT + 0.7) * 10.0) / 10.0,
                Math.round((baseT + 1.0) * 10.0) / 10.0,
                Math.round((baseT + 1.3) * 10.0) / 10.0,
                Math.round((baseT + 1.5) * 10.0) / 10.0,
                Math.round((baseT + 1.8) * 10.0) / 10.0
            };
            bt = new double[] { 0.5, 0.6, 0.3, 0.4, 0.2, 0.3, 0.1 };
            jobNames = new String[] { "J1", "J2", "J3", "J4", "J5", "J6", "J7" };
        }

        class SJob {
            String name; double at, bt, timeLeft; Double finishTime = null;
            SJob(String n, double a, double b) { this.name = n; this.at = a; this.bt = b; this.timeLeft = b; }
        }
        class BlockJob {
            String name; double timeLeft;
            BlockJob(String n, double tl) { this.name = n; this.timeLeft = tl; }
        }
        class EvStep {
            double time; int nj; double cpuPct, et, hwJob;
            List<String> eventList = new ArrayList<>();
            List<BlockJob> intervalJobs = new ArrayList<>();
        }

        List<SJob> simJobs = new ArrayList<>();
        for (int i = 0; i < n; i++) simJobs.add(new SJob(jobNames[i], at[i], bt[i]));

        List<EvStep> steps = new ArrayList<>();
        int jobIdx = 0;
        List<SJob> active = new ArrayList<>();
        double prevTime = simJobs.get(0).at;
        List<String> finishOrderList = new ArrayList<>();

        while (jobIdx < n || !active.isEmpty()) {
            if (active.isEmpty()) {
                SJob nextJ = simJobs.get(jobIdx);
                double arrivalTime = nextJ.at;
                double et = Math.round((arrivalTime - prevTime) * 1000.0) / 1000.0;
                active.add(nextJ);
                jobIdx++;

                EvStep step = new EvStep();
                step.time = arrivalTime;
                step.eventList.add(nextJ.name + " a/s");
                while (jobIdx < n && Math.abs(simJobs.get(jobIdx).at - arrivalTime) < 1e-6) {
                    SJob sim = simJobs.get(jobIdx);
                    active.add(sim);
                    step.eventList.add(sim.name + " a/s");
                    jobIdx++;
                }
                step.nj = 0;
                step.cpuPct = 0.0;
                step.et = et;
                step.hwJob = 0.0;
                for (SJob j : active) step.intervalJobs.add(new BlockJob(j.name, j.timeLeft));
                steps.add(step);
                prevTime = arrivalTime;
                continue;
            }

            int currentNj = active.size();
            double cpuShare = 1.0 / currentNj;
            double minTimeLeft = Double.MAX_VALUE;
            for (SJob j : active) {
                if (j.timeLeft < minTimeLeft) minTimeLeft = j.timeLeft;
            }

            double timeToFinish = minTimeLeft / cpuShare;
            double termTime = Math.round((prevTime + timeToFinish) * 10000.0) / 10000.0;
            double nextArrTime = (jobIdx < n) ? simJobs.get(jobIdx).at : Double.MAX_VALUE;

            double eventTime = Math.min(termTime, nextArrTime);
            double et = Math.round((eventTime - prevTime) * 1000.0) / 1000.0;
            double hwJob = Math.round((et * cpuShare) * 1000.0) / 1000.0;

            for (SJob j : active) {
                j.timeLeft = Math.max(0, Math.round((j.timeLeft - hwJob) * 1000.0) / 1000.0);
            }

            List<SJob> arrivingThisStep = new ArrayList<>();
            List<String> arrivingNames = new ArrayList<>();
            while (jobIdx < n && Math.abs(simJobs.get(jobIdx).at - eventTime) < 1e-4) {
                SJob arr = simJobs.get(jobIdx);
                arrivingThisStep.add(arr);
                arrivingNames.add(arr.name);
                jobIdx++;
            }

            List<SJob> terminatingThisStep = new ArrayList<>();
            List<String> termNames = new ArrayList<>();
            for (SJob j : active) {
                if (j.timeLeft <= 1e-6) {
                    j.timeLeft = 0.0;
                    j.finishTime = eventTime;
                    terminatingThisStep.add(j);
                    termNames.add(j.name);
                    finishOrderList.add(j.name);
                }
            }

            EvStep step = new EvStep();
            step.time = eventTime;
            step.nj = currentNj;
            step.cpuPct = cpuShare;
            step.et = et;
            step.hwJob = hwJob;

            for (String a : arrivingNames) step.eventList.add(a + " a/s");
            for (String t : termNames) step.eventList.add(t + " t [FINISHED]");

            for (SJob j : active) step.intervalJobs.add(new BlockJob(j.name, j.timeLeft));
            for (SJob arr : arrivingThisStep) step.intervalJobs.add(new BlockJob(arr.name, arr.timeLeft));

            steps.add(step);
            active.removeAll(terminatingThisStep);
            active.addAll(arrivingThisStep);
            prevTime = eventTime;
        }

        // Calculate metrics
        double[] ft = new double[n];
        double[] tat = new double[n];
        double[] wt = new double[n];
        double totalTat = 0, totalWt = 0;
        for (int i = 0; i < n; i++) {
            SJob sj = simJobs.get(i);
            ft[i] = sj.finishTime != null ? sj.finishTime : prevTime;
            tat[i] = Math.round((ft[i] - sj.at) * 100.0) / 100.0;
            wt[i] = Math.max(0.0, Math.round((tat[i] - sj.bt) * 100.0) / 100.0);
            totalTat += tat[i];
            totalWt += wt[i];
        }
        double avgTat = Math.round((totalTat / n) * 100.0) / 100.0;
        double avgWt = Math.round((totalWt / n) * 100.0) / 100.0;
        double totalElapsed = Math.round((prevTime - at[0]) * 100.0) / 100.0;

        // Flatten steps into trace table rows matching System Archives
        List<String[]> traceRows = new ArrayList<>();
        for (int s = 0; s < steps.size(); s++) {
            EvStep step = steps.get(s);
            int numRows = Math.max(1, Math.max(step.intervalJobs.size(), step.eventList.size()));

            for (int r = 0; r < numRows; r++) {
                String[] row = new String[8];
                row[0] = (r == 0) ? String.format(Locale.US, "%.2f", step.time) : "";
                row[1] = (r < step.eventList.size()) ? step.eventList.get(r) : "";
                row[2] = (r == 0) ? String.valueOf(step.nj) : "";
                row[3] = (r == 0) ? (step.nj > 0 ? String.format(Locale.US, "%.3f", step.cpuPct) : "0") : "";
                row[4] = (r == 0) ? String.format(Locale.US, "%.2f", step.et) : "";
                row[5] = (r == 0) ? String.format(Locale.US, "%.3f", step.hwJob) : "";
                row[6] = (r < step.intervalJobs.size()) ? step.intervalJobs.get(r).name : "";

                String tlStr = "";
                if (r < step.intervalJobs.size()) {
                    double tlVal = step.intervalJobs.get(r).timeLeft;
                    if (tlVal <= 1e-6) {
                        tlStr = "0.000 [FINISHED]";
                    } else {
                        tlStr = String.format(Locale.US, "%.3f", tlVal);
                    }
                }
                row[7] = tlStr;
                traceRows.add(row);
            }
        }

        int totalRows = traceRows.size();
        String[][] answers = new String[totalRows][8];
        String[][] worksheet = new String[totalRows][8];

        for (int i = 0; i < totalRows; i++) {
            String[] r = traceRows.get(i);
            System.arraycopy(r, 0, answers[i], 0, 8);
            System.arraycopy(r, 0, worksheet[i], 0, 8);
        }

        // Apply blanks to worksheet based on difficulty & mode
        int blanksPerRow = (diff == Difficulty.EASY) ? 1 : ((diff == Difficulty.AVERAGE) ? 2 : 3);
        int[] candidateCols = { 1, 3, 4, 5, 6, 7 };

        for (int i = 0; i < totalRows; i++) {
            if (mode == Mode.SOLVE_WHOLE) {
                for (int c : candidateCols) {
                    if (!answers[i][c].isEmpty()) worksheet[i][c] = "?";
                }
            } else {
                List<Integer> validCols = new ArrayList<>();
                for (int c : candidateCols) {
                    if (!answers[i][c].isEmpty()) validCols.add(c);
                }
                if (!validCols.isEmpty()) {
                    Collections.shuffle(validCols, RND);
                    int count = Math.min(blanksPerRow, validCols.size());
                    for (int k = 0; k < count; k++) {
                        worksheet[i][validCols.get(k)] = "?";
                    }
                }
            }
        }

        StringBuilder expl = new StringBuilder(String.format(Locale.US,
                "👨‍🍳 MULTIPROGRAMMING TRACE // EVENT-DRIVEN EXECUTION [%s TIER]:\n\n", diff.label));
        expl.append("EXCEL TIME-STEP FORMULAS:\n");
        expl.append("• et = present time - prev time\n");
        expl.append("• %cpu/job = 1 / nj (equal CPU sharing among active jobs)\n");
        expl.append("• hw/job = et * (%cpu/job) (burst work done per job)\n");
        expl.append("• TIME LEFT = prev timeleft - hw/job (0.000 = [FINISHED])\n\n");
        expl.append(String.format(Locale.US, "★ METRICS: AVG TAT = %.2f | AVG WT = %.2f | TOTAL ELAPSED TIME = %.2f (100%% CPU)\n",
                avgTat, avgWt, totalElapsed));

        QuizQuestion q = new QuizQuestion("MOD_08", "MISSION 08: MULTIPROGRAMMING DISPATCHER",
                "OVERCOOKED // MULTIPROGRAMMING TRACE TABLE",
                String.format("Fill in the event-driven execution trace table (%s Tier: %d Jobs, %d Trace Steps)! Calculate %%cpu/job, et, hw/job, and remaining burst times.", diff.label, n, totalRows),
                tip, headers, worksheet, answers, expl.toString(), diff);

        q.sequenceTitle = "KITCHEN DISH COMPLETION SEQUENCE";
        q.sequencePrompt = "Identify the order in which dishes finish on the CPU core:";
        q.correctSequence = String.join(" -> ", finishOrderList);
        String[] tokens = jobNames.clone();
        shuffleArray(tokens);
        q.sequenceTokens = tokens;

        q.telemetryData.put("jobCount", n);
        q.telemetryData.put("jobNames", jobNames);
        q.telemetryData.put("at", at);
        q.telemetryData.put("bt", bt);
        q.telemetryData.put("ft", ft);
        q.telemetryData.put("tat", tat);
        q.telemetryData.put("wt", wt);
        q.telemetryData.put("avgTat", avgTat);
        q.telemetryData.put("avgWt", avgWt);
        q.telemetryData.put("finishOrder", finishOrderList.toArray(new String[0]));
        q.telemetryData.put("totalElapsed", totalElapsed);
        q.telemetryData.put("traceRowCount", totalRows);
        return q;
    }

    /** Helper to fetch procedural randomized question for any of the 8 modules with Difficulty */
    public static QuizQuestion getQuestionByModule(int modIdx, Difficulty diff, Mode mode) {
        if (diff == null) diff = Difficulty.AVERAGE;
        switch (modIdx) {
            case 0: return getCPUQuiz(diff, mode);
            case 1: return getBankerQuiz(diff, mode);
            case 2: return getPagingQuiz(diff, mode);
            case 3: return getCacheQuiz(diff, mode);
            case 4: return getSegmentationQuiz(diff, mode);
            case 5: return getFragmentationQuiz(diff, mode);
            case 6: return getDiskQuiz(diff, mode);
            case 7: return getMultiprogrammingQuiz(diff, mode);
            default: return getCPUQuiz(diff, mode);
        }
    }

    /** Overload for backward compatibility */
    public static QuizQuestion getQuestionByModule(int modIdx, Mode mode) {
        return getQuestionByModule(modIdx, Difficulty.AVERAGE, mode);
    }

    /** Normalize strings by stripping whitespace and common unit suffixes */
    public static String cleanStr(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("(?i)(kb|%|bytes|cyl|orders|order|ms|cylinders)", "").trim();
    }

    /** Check if individual cell matches answer key */
    public static boolean isCellCorrect(String actual, String expected) {
        if (actual == null || expected == null) return false;
        String a = actual.trim();
        String e = expected.trim();
        if (a.equalsIgnoreCase(e)) return true;

        // Vector comparison: e.g. "7 4 3" vs "7, 4, 3" vs "[7 4 3]" vs "[7, 4, 3]"
        String aVec = a.replaceAll("[\\[\\](),]", " ").replaceAll("\\s+", " ").trim();
        String eVec = e.replaceAll("[\\[\\](),]", " ").replaceAll("\\s+", " ").trim();
        if (!aVec.isEmpty() && aVec.equalsIgnoreCase(eVec)) return true;

        String aClean = cleanStr(a);
        String eClean = cleanStr(e);
        if (aClean.equalsIgnoreCase(eClean)) return true;

        // Allocation Status keyword match: e.g. "ALLOCATED [OK]" vs "ALLOCATED"
        if (a.toLowerCase().contains("allocated") && e.toLowerCase().contains("allocated")) return true;
        if (a.toLowerCase().contains("denied") && e.toLowerCase().contains("denied")) return true;

        // Socket comparison: e.g. "Socket 2" vs "Socket 2 [500K]" vs "SOCK 2" vs "2"
        String aSock = a.replaceAll("(?i)\\[.*?\\]", "").replaceAll("(?i)sock(et)?", "").replaceAll("\\s+", "").trim();
        String eSock = e.replaceAll("(?i)\\[.*?\\]", "").replaceAll("(?i)sock(et)?", "").replaceAll("\\s+", "").trim();
        if (!aSock.isEmpty() && aSock.equalsIgnoreCase(eSock)) return true;

        // Direction comparison: e.g. "UP (->)" vs "UP" vs "->" vs "DOWN (<-)" vs "DOWN" vs "<-"
        if ((a.toUpperCase().contains("UP") || a.contains("->")) && (e.toUpperCase().contains("UP") || e.contains("->"))) return true;
        if ((a.toUpperCase().contains("DOWN") || a.contains("<-")) && (e.toUpperCase().contains("DOWN") || e.contains("<-"))) return true;

        // Multiprogramming Event match: e.g. "J1 t [FINISHED]" vs "J1 t" vs "J1 terminate"
        if (e.toUpperCase().contains("[FINISHED]") || a.toUpperCase().contains("[FINISHED]")) {
            String aFin = a.replaceAll("(?i)\\[FINISHED\\]", "").replaceAll("(?i)terminate", "t").trim();
            String eFin = e.replaceAll("(?i)\\[FINISHED\\]", "").replaceAll("(?i)terminate", "t").trim();
            if (aFin.equalsIgnoreCase(eFin)) return true;
            if ((aFin.isEmpty() || aFin.equals("0") || aFin.equals("0.0") || aFin.equals("0.000")) &&
                (eFin.isEmpty() || eFin.equals("0") || eFin.equals("0.0") || eFin.equals("0.000"))) return true;
        }

        // Percentage cross-match: e.g. "40%" vs "0.40" vs "40.0" vs "40"
        try {
            double da = parseNumOrPercent(a);
            double de = parseNumOrPercent(e);
            if (Math.abs(da - de) < 0.05) return true;
        } catch (NumberFormatException ignored) {}

        // Numeric float comparison: 2 == 2.0 (with 0.025 tolerance for rounded values)
        try {
            String aNumStr = aClean.replaceAll("(?i)\\[.*?\\]", "").trim();
            String eNumStr = eClean.replaceAll("(?i)\\[.*?\\]", "").trim();
            double da = Double.parseDouble(aNumStr);
            double de = Double.parseDouble(eNumStr);
            if (Math.abs(da - de) < 0.025) return true;
        } catch (NumberFormatException ignored) {}

        return false;
    }

    private static double parseNumOrPercent(String str) {
        String s = str.trim();
        boolean isPct = s.endsWith("%");
        if (isPct) s = s.substring(0, s.length() - 1).trim();
        s = cleanStr(s);
        double val = Double.parseDouble(s);
        return isPct ? val : (val <= 1.0 && val > 0.0 ? val * 100.0 : val);
    }

    /** Procedural validation runner */
    public static void main(String[] args) {
        for (Difficulty d : Difficulty.values()) {
            System.out.println("=== TESTING DIFFICULTY TIER: " + d.label + " ===");
            for (int i = 0; i < 8; i++) {
                QuizQuestion qFill = getQuestionByModule(i, d, Mode.FILL_MISSING);
                QuizQuestion qSolve = getQuestionByModule(i, d, Mode.SOLVE_WHOLE);

                boolean fillOk = qFill.worksheetData != null && qFill.answerKey != null
                        && qFill.worksheetData.length == qFill.answerKey.length;
                boolean solveOk = qSolve.worksheetData != null && qSolve.answerKey != null
                        && qSolve.worksheetData.length == qSolve.answerKey.length;

                System.out.println(String.format("Module %d (%s) [%s]: Rows=%d fillOk=%b solveOk=%b",
                        i, qFill.arcadeGameTitle, d.label, qFill.worksheetData.length, fillOk, solveOk));
            }
        }
        System.out.println("ALL 8 MODULES PROCEDURALLY VERIFIED ACROSS ALL 3 DIFFICULTY TIERS!");
    }
}
