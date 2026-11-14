package scratch;

import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class GenerateManualData {

    public static String padRight(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }

    public static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    public static String banner(String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔").append(repeat('═', 72)).append("╗\n");
        sb.append("║ ").append(padRight(title, 70)).append(" ║\n");
        sb.append("╚").append(repeat('═', 72)).append("╝");
        return sb.toString();
    }

    public static String codeBox(String header, String[] codeLines) {
        StringBuilder sb = new StringBuilder();
        sb.append("┌").append(repeat('─', 72)).append("┐\n");
        if (header != null && !header.isEmpty()) {
            sb.append("│ ").append(padRight(header, 70)).append(" │\n");
            sb.append("├").append(repeat('─', 72)).append("┤\n");
        }
        for (String cl : codeLines) {
            sb.append("│ ").append(padRight(cl, 70)).append(" │\n");
        }
        sb.append("└").append(repeat('─', 72)).append("┘");
        return sb.toString();
    }

    public static String table2(String h1, String h2, int w1, int w2, String[][] rows) {
        // w1 + w2 = 71
        StringBuilder sb = new StringBuilder();
        sb.append("┌").append(repeat('─', w1)).append("┬").append(repeat('─', w2)).append("┐\n");
        sb.append("│").append(padRight(h1, w1)).append("│").append(padRight(h2, w2)).append("│\n");
        sb.append("├").append(repeat('─', w1)).append("┼").append(repeat('─', w2)).append("┤\n");
        for (String[] r : rows) {
            sb.append("│").append(padRight(r[0], w1)).append("│").append(padRight(r[1], w2)).append("│\n");
        }
        sb.append("└").append(repeat('─', w1)).append("┴").append(repeat('─', w2)).append("┘");
        return sb.toString();
    }

    public static String table3(String h1, String h2, String h3, int w1, int w2, int w3, String[][] rows) {
        // w1 + w2 + w3 = 70
        StringBuilder sb = new StringBuilder();
        sb.append("┌").append(repeat('─', w1)).append("┬").append(repeat('─', w2)).append("┬").append(repeat('─', w3)).append("┐\n");
        sb.append("│").append(padRight(h1, w1)).append("│").append(padRight(h2, w2)).append("│").append(padRight(h3, w3)).append("│\n");
        sb.append("├").append(repeat('─', w1)).append("┼").append(repeat('─', w2)).append("┼").append(repeat('─', w3)).append("┤\n");
        for (String[] r : rows) {
            sb.append("│").append(padRight(r[0], w1)).append("│").append(padRight(r[1], w2)).append("│").append(padRight(r[2], w3)).append("│\n");
        }
        sb.append("└").append(repeat('─', w1)).append("┴").append(repeat('─', w2)).append("┴").append(repeat('─', w3)).append("┘");
        return sb.toString();
    }

    public static String table5(String h1, String h2, String h3, String h4, String h5,
                                int w1, int w2, int w3, int w4, int w5, String[][] rows) {
        // w1 + w2 + w3 + w4 + w5 = 68
        StringBuilder sb = new StringBuilder();
        sb.append("┌").append(repeat('─', w1)).append("┬").append(repeat('─', w2)).append("┬")
          .append(repeat('─', w3)).append("┬").append(repeat('─', w4)).append("┬")
          .append(repeat('─', w5)).append("┐\n");
        sb.append("│").append(padRight(h1, w1)).append("│").append(padRight(h2, w2)).append("│")
          .append(padRight(h3, w3)).append("│").append(padRight(h4, w4)).append("│")
          .append(padRight(h5, w5)).append("│\n");
        sb.append("├").append(repeat('─', w1)).append("┼").append(repeat('─', w2)).append("┼")
          .append(repeat('─', w3)).append("┼").append(repeat('─', w4)).append("┼")
          .append(repeat('─', w5)).append("┤\n");
        for (String[] r : rows) {
            sb.append("│").append(padRight(r[0], w1)).append("│").append(padRight(r[1], w2)).append("│")
              .append(padRight(r[2], w3)).append("│").append(padRight(r[3], w4)).append("│")
              .append(padRight(r[4], w5)).append("│\n");
        }
        sb.append("└").append(repeat('─', w1)).append("┴").append(repeat('─', w2)).append("┴")
          .append(repeat('─', w3)).append("┴").append(repeat('─', w4)).append("┴")
          .append(repeat('─', w5)).append("┘");
        return sb.toString();
    }

    public static String table6(String h1, String h2, String h3, String h4, String h5, String h6,
                                int w1, int w2, int w3, int w4, int w5, int w6, String[][] rows) {
        // w1 + w2 + w3 + w4 + w5 + w6 = 67
        StringBuilder sb = new StringBuilder();
        sb.append("┌").append(repeat('─', w1)).append("┬").append(repeat('─', w2)).append("┬")
          .append(repeat('─', w3)).append("┬").append(repeat('─', w4)).append("┬")
          .append(repeat('─', w5)).append("┬").append(repeat('─', w6)).append("┐\n");
        sb.append("│").append(padRight(h1, w1)).append("│").append(padRight(h2, w2)).append("│")
          .append(padRight(h3, w3)).append("│").append(padRight(h4, w4)).append("│")
          .append(padRight(h5, w5)).append("│").append(padRight(h6, w6)).append("│\n");
        sb.append("├").append(repeat('─', w1)).append("┼").append(repeat('─', w2)).append("┼")
          .append(repeat('─', w3)).append("┼").append(repeat('─', w4)).append("┼")
          .append(repeat('─', w5)).append("┼").append(repeat('─', w6)).append("┤\n");
        for (String[] r : rows) {
            sb.append("│").append(padRight(r[0], w1)).append("│").append(padRight(r[1], w2)).append("│")
              .append(padRight(r[2], w3)).append("│").append(padRight(r[3], w4)).append("│")
              .append(padRight(r[4], w5)).append("│").append(padRight(r[5], w6)).append("│\n");
        }
        sb.append("└").append(repeat('─', w1)).append("┴").append(repeat('─', w2)).append("┴")
          .append(repeat('─', w3)).append("┴").append(repeat('─', w4)).append("┴")
          .append(repeat('─', w5)).append("┴").append(repeat('─', w6)).append("┘");
        return sb.toString();
    }

    public static String table8(String[] headers, int[] widths, String[][] rows) {
        // sum(widths) = 74 - 9 = 65
        StringBuilder sb = new StringBuilder();
        sb.append("┌");
        for (int i = 0; i < widths.length; i++) {
            sb.append(repeat('─', widths[i]));
            if (i < widths.length - 1) sb.append("┬");
        }
        sb.append("┐\n│");
        for (int i = 0; i < widths.length; i++) {
            sb.append(padRight(headers[i], widths[i])).append("│");
        }
        sb.append("\n├");
        for (int i = 0; i < widths.length; i++) {
            sb.append(repeat('─', widths[i]));
            if (i < widths.length - 1) sb.append("┼");
        }
        sb.append("┤\n");
        for (String[] r : rows) {
            sb.append("│");
            for (int i = 0; i < widths.length; i++) {
                sb.append(padRight(r[i], widths[i])).append("│");
            }
            sb.append("\n");
        }
        sb.append("└");
        for (int i = 0; i < widths.length; i++) {
            sb.append(repeat('─', widths[i]));
            if (i < widths.length - 1) sb.append("┴");
        }
        sb.append("┘");
        return sb.toString();
    }

    public static String wrapParagraph(String text, int maxLen, String indent) {
        StringBuilder sb = new StringBuilder();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            if (cur.length() == 0) {
                cur.append(w);
            } else if (cur.length() + 1 + w.length() <= maxLen) {
                cur.append(" ").append(w);
            } else {
                sb.append(cur.toString()).append("\n");
                cur = new StringBuilder(indent).append(w);
            }
        }
        if (cur.length() > 0) sb.append(cur.toString());
        return sb.toString();
    }

    public static String toJavaLiteral(String s) {
        String[] lines = s.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            l = l.replace("\\", "\\\\").replace("\"", "\\\"");
            sb.append("            \"").append(l);
            if (i < lines.length - 1) {
                sb.append("\\n\" +\n");
            } else {
                sb.append("\"");
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        PrintWriter out = new PrintWriter(new FileWriter("arcadeos/ArchiveManualData.java"));
        out.println("package arcadeos;\n");
        out.println("/**");
        out.println(" * Enhanced Knowledge Base and Core Algorithm Logic for the 8 Operating System Manuals.");
        out.println(" * Standardized to 74-character width across all modules and tabs for pixel-perfect");
        out.println(" * alignment, equal 22px side margins, cyber framing, and complete visual justification.");
        out.println(" */");
        out.println("public class ArchiveManualData {\n");
        out.println("    public static class ManualEntry {");
        out.println("        public String id, title, subtitle, complexity, tags;");
        out.println("        public String theory, formulas, pseudocode, workedExample, cheatSheet;");
        out.println("        public int simulatorTabIndex, quizModuleIndex;\n");
        out.println("        public ManualEntry(String id, String title, String subtitle, String complexity, String tags,");
        out.println("                           String theory, String formulas, String pseudocode, String workedExample,");
        out.println("                           String cheatSheet, int simTab, int quizMod) {");
        out.println("            this.id = id; this.title = title; this.subtitle = subtitle;");
        out.println("            this.complexity = complexity; this.tags = tags;");
        out.println("            this.theory = theory; this.formulas = formulas;");
        out.println("            this.pseudocode = pseudocode; this.workedExample = workedExample;");
        out.println("            this.cheatSheet = cheatSheet;");
        out.println("            this.simulatorTabIndex = simTab; this.quizModuleIndex = quizMod;");
        out.println("        }");
        out.println("    }\n");
        out.println("    private static final ManualEntry[] MANUALS = new ManualEntry[8];\n");
        out.println("    static {");

        // ─────────────────────────────────────────────────────────────────────
        // MODULE 01
        // ─────────────────────────────────────────────────────────────────────
        String m1_t1 = banner("1. EXECUTIVE OVERVIEW & ARCHITECTURE") + "\n" +
            wrapParagraph("The CPU Scheduler is responsible for allocating the execution core to one of the runnable processes currently residing in the READY QUEUE.", 72, "") + "\n\n" +
            "PROCESS STATE TRANSITION DIAGRAM:\n" +
            "┌───────┐ admit ┌───────┐ dispatch ┌─────────┐ exit ┌────────────┐\n" +
            "│  NEW  │ ────> │ READY │ ───────> │ RUNNING │ ───> │ TERMINATED │\n" +
            "└───────┘       └───────┘          └─────────┘      └────────────┘\n" +
            "                    ▲                   │\n" +
            "                    │   I/O Complete    ▼  I/O or Event Wait\n" +
            "                    └────────────── ┌─────────┐\n" +
            "                                    │ WAITING │\n" +
            "                                    └─────────┘\n\n" +
            "2. FOUR SCHEDULING CRITERIA:\n" +
            "  • FCFS (First-Come, First-Served) :\n" +
            "    Non-preemptive. Strict FIFO queue ordered by Arrival Time.\n\n" +
            "  • SJF (Shortest Job First) :\n" +
            "    Non-preemptive. Selects process with smallest Burst Time.\n\n" +
            "  • SRTF (Shortest Remaining Time) :\n" +
            "    Preemptive SJF. Preempts current job if new job has smaller burst.\n\n" +
            "  • Round Robin (RR) :\n" +
            "    Preemptive with Time Quantum (TQ). Enforces fair timesharing.";

        String m1_t2 = banner("2. MATHEMATICAL FORMULAS & PERFORMANCE METRICS") + "\n" +
            table2(" METRIC NAME", " FORMULA & COMPUTATION", 25, 46, new String[][]{
                {" Start Time (ST)", " ST[i] = max(Arrival_Time[i], FT[i-1])"},
                {" Finish Time (FT)", " FT[i] = ST[i] + Burst_Time[i]"},
                {" Turnaround Time (TAT)", " TAT[i] = FT[i] - Arrival_Time[i]"},
                {" Waiting Time (WT)", " WT[i] = TAT[i] - Burst[i] = ST[i] - AT[i]"},
                {" Average Waiting Time", " AWT = (Σ WT[i]) / N"},
                {" Average Turnaround Time", " ATAT = (Σ TAT[i]) / N"},
                {" CPU Utilization", " (Busy Time / Total Schedule Duration) * 100%"},
                {" Throughput", " Completed Jobs / Unit Time"}
            }) + "\n\n" +
            "CALCULATION RUNWAY (SAMPLE JOB TRACE):\n" +
            "  • Given Job J1: Arrival (AT) = 2.0, Burst (BT) = 5.0, Start (ST) = 4.0\n" +
            "  • Finish Time (FT)     = ST + BT = 4.0 + 5.0 = 9.0\n" +
            "  • Turnaround Time(TAT) = FT - AT = 9.0 - 2.0 = 7.0\n" +
            "  • Waiting Time (WT)    = TAT - BT = 7.0 - 5.0 = 2.0  (or ST - AT = 2.0)\n\n" +
            "CRITICAL EVALUATION RULES:\n" +
            "  ★ Arrival Gap Rule     : If CPU was idle prior, ST[i] = Arrival_Time[i].\n" +
            "  ★ Minimum Overhead     : Context-switch time adds latency between jobs.\n" +
            "  ★ Throughput vs Latency: Maximizing throughput may raise tail latency.";

        String m1_t3 = banner("3. ALGORITHM PSEUDOCODE & DISPATCH ENGINES") + "\n" +
            codeBox("ENGINE 01: FCFS DISPATCHER (NON-PREEMPTIVE FIFO QUEUE)", new String[]{
                "function schedule_fcfs(Job[] jobs) {",
                "    sort_by_arrival_time(jobs); // O(N log N)",
                "    double time = 0.0;",
                "    for (Job j : jobs) {",
                "        if (time < j.arrival) time = j.arrival; // CPU idle gap",
                "        j.startTime  = time;",
                "        j.finishTime = time + j.burst;",
                "        j.turnaround = j.finishTime - j.arrival;",
                "        j.waiting    = j.turnaround - j.burst;",
                "        time = j.finishTime;",
                "    }",
                "}"
            }) + "\n\n" +
            codeBox("ENGINE 02: ROUND ROBIN DISPATCHER (PREEMPTIVE TIME SLICE Q)", new String[]{
                "function schedule_round_robin(Queue readyQ, double timeQuantum) {",
                "    while (!readyQ.isEmpty()) {",
                "        Job current = readyQ.dequeue();",
                "        double slice = min(current.remainingTime, timeQuantum);",
                "        time += slice;",
                "        current.remainingTime -= slice;",
                "        if (current.remainingTime > 0) {",
                "            readyQ.enqueue(current); // Preempt back to tail",
                "        } else {",
                "            current.finishTime = time; // Terminated",
                "        }",
                "    }",
                "}"
            }) + "\n\n" +
            "COMPLEXITY: FCFS: O(N log N) | Round Robin: O(Σ Bursts / Q)";

        // 8-column trace table: widths = 4, 5, 5, 5, 5, 5, 5, 26 -> sum = 60 + 5 = 65. 65 + 9 borders = 74!
        String m1_t4 = banner("4. STEP-BY-STEP WORKED TRACE & GANTT RUNWAY") + "\n" +
            "SAMPLE WORKLOAD: 3 PROCESSES\n" +
            "  • J1: Arrival = 0.0, Burst = 4.0\n" +
            "  • J2: Arrival = 1.0, Burst = 3.0\n" +
            "  • J3: Arrival = 2.0, Burst = 2.0\n\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            table8(new String[]{"JOB", " AT ", " BT ", " ST ", " FT ", " WT ", " TAT", " STATUS / NOTES"},
                   new int[]{4, 5, 5, 5, 5, 5, 5, 26},
                   new String[][]{
                       {" J1", " 0.0", " 4.0", " 0.0", " 4.0", " 0.0", " 4.0", " Starts immediately at t=0"},
                       {" J2", " 1.0", " 3.0", " 4.0", " 7.0", " 3.0", " 6.0", " Waits from 1.0 to 4.0"},
                       {" J3", " 2.0", " 2.0", " 7.0", " 9.0", " 5.0", " 7.0", " Waits from 2.0 to 7.0"}
                   }) + "\n\n" +
            "ASCII GANTT TIMELINE:\n" +
            "0.0                 4.0                 7.0           9.0\n" +
            " ┌───────────────────┬───────────────────┬─────────────┐\n" +
            " │    JOB 1 (J1)     │    JOB 2 (J2)     │  JOB 3 (J3) │\n" +
            " └───────────────────┴───────────────────┴─────────────┘\n" +
            "METRICS: Total Time=9.0 | Avg WT=2.67 | Avg TAT=5.67 | CPU Util=100%";

        String m1_t5 = banner("5. EXAM CHEAT-SHEET & SCHEDULING COMPARISON MATRIX") + "\n" +
            table3(" ALGORITHM", " DECISION TYPE", " OPTIMALITY & TRADE-OFFS", 20, 22, 28, new String[][]{
                {" FCFS", " Non-preemptive FIFO", " Simple; Convoy Effect"},
                {" SJF", " Non-preemptive Short", " Minimum average WT; Starv"},
                {" SRTF", " Preemptive SJF", " Optimal; High Preemption"},
                {" Round Robin (RR)", " Preemptive Quantum Q", " Fair timeshare; CS Cost"}
            }) + "\n\n" +
            "★ CONVOY EFFECT IN FCFS:\n" +
            "  Long CPU-bound job runs first -> short jobs wait -> low CPU/IO util.\n\n" +
            "★ SJF IS OPTIMAL:\n" +
            "  Minimizes average waiting time. (Aging mechanism prevents starvation).\n\n" +
            "★ ROUND ROBIN QUANTUM RULE:\n" +
            "  • If TQ is too large  -> RR degenerates into FCFS.\n" +
            "  • If TQ is too small  -> Context-switch overhead destroys throughput.\n" +
            "  • Golden Rule         -> 80% of CPU bursts should be shorter than TQ.";

        emitManual(out, 0, "01", "CPU PROCESS SCHEDULING", "MISSION 01: DISPATCH RUNWAY & QUEUE ANALYSIS",
                   "★★★☆☆ (MODERATE)", "FCFS | SJF | SRTF | ROUND ROBIN | GANTT TIMELINE",
                   m1_t1, m1_t2, m1_t3, m1_t4, m1_t5, 0, 0);

        // ─────────────────────────────────────────────────────────────────────
        // MODULE 02: BANKER'S DEADLOCK AVOIDANCE
        // ─────────────────────────────────────────────────────────────────────
        String m2_t1 = banner("1. EXECUTIVE OVERVIEW & DEADLOCK ARCHITECTURE") + "\n" +
            wrapParagraph("Developed by Edsger Dijkstra, the Banker's Algorithm ensures that resource allocations never lead to DEADLOCK. A deadlock occurs when a set of processes are permanently blocked holding resources while waiting for ones held by others.", 72, "") + "\n\n" +
            "RESOURCE ALLOCATION GRAPH (RAG) CYCLE SCHEMATIC:\n" +
            "┌──────────────┐      Holds Resource     ┌──────────────┐\n" +
            "│  PROCESS P1  │ ──────────────────────> │ RESOURCE R1  │\n" +
            "└──────────────┘                         └──────────────┘\n" +
            "       ▲                                        │\n" +
            "       │ Requests                               │ Assigned to\n" +
            "       │                                        ▼\n" +
            "┌──────────────┐      Holds Resource     ┌──────────────┐\n" +
            "│ RESOURCE R2  │ <────────────────────── │  PROCESS P2  │\n" +
            "└──────────────┘                         └──────────────┘\n\n" +
            "FOUR COFFMAN CONDITIONS FOR DEADLOCK:\n" +
            "  1. Mutual Exclusion  : Only one process can use a resource at a time.\n" +
            "  2. Hold and Wait     : Processes holding resources can request more.\n" +
            "  3. No Preemption     : Resources cannot be forcibly seized.\n" +
            "  4. Circular Wait     : A closed chain of processes waiting for each.";

        String m2_t2 = banner("2. MATHEMATICAL MATRIX EQUATIONS & FEASIBILITY RULES") + "\n" +
            table2(" METRIC / VECTOR", " MATHEMATICAL FORMULA & MATRIX LOGIC", 25, 46, new String[][]{
                {" Need Matrix", " Need[i][j] = Max[i][j] - Allocation[i][j]"},
                {" Request Feasibility", " Request[i] <= Need[i] AND Request[i] <= Avail"},
                {" Speculative Alloc", " Avail' = Avail - Req; Alloc' = Alloc + Req"},
                {" Need Adjustment", " Need'  = Need  - Req"},
                {" Safety Condition", " Finish[i] == false AND Need[i] <= Work"},
                {" Work Update on Exit", " Work_new = Work_current + Allocation[i]"},
                {" Safe System State", " All Finish[i] == true (Valid Sequence Exists)"}
            }) + "\n\n" +
            "CALCULATION RUNWAY (SAMPLE VECTOR EVALUATION):\n" +
            "  • Given State : Available = [3, 3, 2], Process P1 Need = [1, 2, 2]\n" +
            "  • Feasibility : Need[1] <= Work -> [1, 2, 2] <= [3, 3, 2] -> TRUE (RUNS)\n" +
            "  • P1 Returns  : Work_new = [3, 3, 2] + Alloc[P1: 2, 0, 0] = [5, 3, 2]\n\n" +
            "CRITICAL EVALUATION RULES:\n" +
            "  ★ Allocation Rule      : Always return Allocation to Work, NEVER Need.\n" +
            "  ★ Safe State Guarantee : Safe implies NO deadlock; Unsafe != Deadlock.\n" +
            "  ★ Single-Resource RAG  : Cycle in single-instance graph = DEADLOCK.";

        String m2_t3 = banner("3. SAFETY ALGORITHM & RESOURCE-REQUEST PSEUDOCODE") + "\n" +
            codeBox("SAFETY ALGORITHM (DIJKSTRA DEADLOCK AVOIDANCE VERIFIER)", new String[]{
                "function verify_safety(int[] Available, int[][] Alloc, int[][] Need) {",
                "    int[] Work = copy(Available);",
                "    boolean[] Finish = new boolean[N]; // initialized to false",
                "    List<Integer> safeSeq = new ArrayList<>();",
                "    while (safeSeq.size() < N) {",
                "        boolean found = false;",
                "        for (int i = 0; i < N; i++) {",
                "            if (!Finish[i] && vector_less_equal(Need[i], Work)) {",
                "                vector_add(Work, Alloc[i]); // Pi returns resources!",
                "                Finish[i] = true;",
                "                safeSeq.add(i);",
                "                found = true;",
                "            }",
                "        }",
                "        if (!found) return null; // UNSAFE STATE (DEADLOCK RISK)",
                "    }",
                "    return safeSeq; // SAFE STATE: Valid Execution Order Confirmed",
                "}"
            }) + "\n\n" +
            "COMPLEXITY: Time: O(M * N^2) | Space: O(N + M) for Work & Finish vectors";

        // 5-column trace table: widths = 5, 13, 13, 13, 24 -> 68 + 6 = 74
        String m2_t4 = banner("4. STEP-BY-STEP WORKED EXAMPLE & SAFE SEQUENCE") + "\n" +
            "INITIAL STATE: Available = [3, 3, 2] (Resource Types: A, B, C)\n\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            table5("PROC", " ALLOCATION  ", "     MAX     ", "NEED=MAX-ALOC", " STATUS IN LOOP         ",
                   5, 13, 13, 13, 24, new String[][]{
                       {" P0", "  0   1   0  ", "  7   5   3  ", "  7   4   3  ", " Needs > Work (WAIT)   "},
                       {" P1", "  2   0   0  ", "  3   2   2  ", "  1   2   2  ", " [1,2,2]<=Work (RUNS)  "},
                       {" P2", "  3   0   2  ", "  9   0   2  ", "  6   0   0  ", " Needs > Work (WAIT)   "},
                       {" P3", "  2   1   1  ", "  2   2   2  ", "  0   1   1  ", " [0,1,1]<=Work (RUNS)  "},
                       {" P4", "  0   0   2  ", "  4   3   3  ", "  4   3   1  ", " [4,3,1]<=Work (RUNS)  "}
                   }) + "\n\n" +
            "EXECUTION STEP SEQUENCE:\n" +
            "  1. P1 runs -> Releases [2,0,0] -> Work becomes [5, 3, 2]\n" +
            "  2. P3 runs -> Releases [2,1,1] -> Work becomes [7, 4, 3]\n" +
            "  3. P4 runs -> Releases [0,0,2] -> Work becomes [7, 4, 5]\n" +
            "  4. P0 runs -> Releases [0,1,0] -> Work becomes [7, 5, 5]\n" +
            "  5. P2 runs -> Releases [3,0,2] -> Work becomes [10, 5, 7]\n\n" +
            "RESULT: SAFE SEQUENCE = < P1 -> P3 -> P4 -> P0 -> P2 >";

        String m2_t5 = banner("5. EXAM CHEAT-SHEET & DEADLOCK ESSENTIALS") + "\n" +
            table3(" HANDLING METHOD", " MECHANISM APPLIED", " RESOURCE UTILIZATION", 20, 22, 28, new String[][]{
                {" Deadlock Ignorance", " Ostrich Algorithm", " Maximum; risk of hang"},
                {" Prevention", " Invalidate 1 Coffman", " Low; strict constraints"},
                {" Avoidance (Banker)", " Dynamic Safe Checks", " Moderate; needs max claim"},
                {" Detect & Recover", " Periodic RAG Checks", " High until abort occurs"}
            }) + "\n\n" +
            "★ SAFE != DEADLOCK-FREE ONLY:\n" +
            "  • Safe State   : Safe sequence exists; NO deadlock is possible.\n" +
            "  • Unsafe State : Deadlock possible if all jobs demand full Max claim.\n" +
            "  • Deadlock     : Closed cycle of blocked processes (subset of Unsafe).\n\n" +
            "★ SAFETY ALGORITHM WORK UPDATE TRAP:\n" +
            "  When process Pi finishes, it returns its ALLOCATION to the Work pool:\n" +
            "  Work_new = Work_current + Allocation[Pi]  (NEVER add Need!).";

        emitManual(out, 1, "02", "BANKER'S DEADLOCK AVOIDANCE", "MISSION 02: SAFETY VERIFICATION & RESOURCE GRAPHS",
                   "★★★★☆ (ADVANCED)", "AVAILABLE | ALLOCATION | MAX MATRIX | NEED MATRIX | SAFE STATE",
                   m2_t1, m2_t2, m2_t3, m2_t4, m2_t5, 1, 1);

        // ─────────────────────────────────────────────────────────────────────
        // MODULE 03: PAGING & ADDRESS TRANSLATION
        // ─────────────────────────────────────────────────────────────────────
        String m3_t1 = banner("1. EXECUTIVE OVERVIEW & MMU HARDWARE") + "\n" +
            wrapParagraph("Paging completely eliminates EXTERNAL FRAGMENTATION by dividing logical memory into fixed-sized blocks called PAGES, and physical RAM into identically-sized blocks called FRAMES.", 72, "") + "\n\n" +
            "HARDWARE MMU TRANSLATION SCHEMATIC:\n" +
            "  CPU LOGICAL ADDRESS  = [ PAGE NUMBER (p) │  OFFSET (d) ]\n" +
            "                                │                 │\n" +
            "                                ▼                 │ (Direct Copy)\n" +
            "                        ┌────────────────┐        │\n" +
            "                        │   PAGE TABLE   │        │\n" +
            "                        │ p -> Frame (f) │        │\n" +
            "                        └────────────────┘        │\n" +
            "                                │                 │\n" +
            "                                ▼                 ▼\n" +
            "  PHYSICAL RAM ADDRESS = [ FRAME NUMBER (f)│  OFFSET (d) ]\n\n" +
            "KEY HARDWARE CHARACTERISTICS:\n" +
            "  • Pure Paging suffers NO external fragmentation whatsoever.\n" +
            "  • Internal fragmentation on final page only (avg 0.5 * Page Size).\n" +
            "  • Translation is implemented entirely in MMU hardware for speed.";

        String m3_t2 = banner("2. MATHEMATICAL TRANSLATION FORMULAS & TLB METRICS") + "\n" +
            table2(" PARAMETER / FORMULA", " MATHEMATICAL DEFINITION", 25, 46, new String[][]{
                {" Page Number (p)", " p = floor(Logical_Address / Page_Size)"},
                {" Page Offset (d)", " d = Logical_Address % Page_Size"},
                {" Physical Address (PA)", " PA = (Frame_Number * Page_Size) + d"},
                {" Total Page Table Size", " Entries = Logical_Address_Space / Page_Size"},
                {" Total Physical Frames", " Frames  = Physical_RAM / Frame_Size"},
                {" Effective Access (EAT)", " EAT = h*(t_tlb + t_ram) + (1-h)*(t_tlb+2*ram)"}
            }) + "\n\n" +
            "CALCULATION RUNWAY (SAMPLE ADDRESS TRANSLATION):\n" +
            "  • Given : Page Size = 4096 bytes (2^12), Logical Address = 13000\n" +
            "  • Step 1: Page Number p = floor(13000 / 4096) = 3\n" +
            "  • Step 2: Offset d      = 13000 % 4096 = 712 bytes\n" +
            "  • Step 3: Page Table Lookup: If PageTable[3] = Frame 7\n" +
            "  • Step 4: PA = (7 * 4096) + 712 = 28672 + 712 = 29384 in RAM.";

        String m3_t3 = banner("3. HARDWARE TRANSLATION PSEUDOCODE") + "\n" +
            codeBox("MMU ADDRESS TRANSLATION & TLB LOOKUP ENGINE", new String[]{
                "function translate(int logical_address, int page_size, PageTable pt) {",
                "    int p = logical_address / page_size;",
                "    int d = logical_address % page_size;",
                "    if (p >= pt.entries.length || !pt[p].valid) {",
                "        trigger_interrupt(PAGE_FAULT, p); // Demand Paging Fault",
                "    }",
                "    int f = pt[p].frameNumber;",
                "    return (f * page_size) + d; // Physical Memory Address",
                "}"
            }) + "\n\n" +
            "COMPLEXITY: Hardware Lookups: O(1) | TLB Hit: < 1 ns | RAM Read: 100 ns";

        // 5-column table: widths = 9, 13, 13, 11, 22 -> 68 + 6 = 74
        String m3_t4 = banner("4. STEP-BY-STEP WORKED EXAMPLE & PAGE TABLE MAP") + "\n" +
            "PARAMETERS: Page Size = 512 bytes | Logical Address Space = 2048 bytes\n\n" +
            "PAGE TABLE MAPPING & TRANSLATION TRACE:\n" +
            table5(" LOG ADDR", " PAGE NO (p) ", " OFFSET (d)  ", " FRAME (f) ", " PHYSICAL RAM ADDR    ",
                   9, 13, 13, 11, 22, new String[][]{
                       {" 1040", " 1040/512 = 2", " 1040%512 =16", " Frame 5", " (5*512) + 16 = 2576"},
                       {" 0250", " 0250/512 = 0", " 0250%512=250", " Frame 3", " (3*512) +250 = 1786"},
                       {" 0700", " 0700/512 = 1", " 0700%512=188", " Frame 1", " (1*512) +188 =  700"},
                       {" 1600", " 1600/512 = 3", " 1600%512 =64", " Frame 2", " (2*512) + 64 = 1088"}
                   }) + "\n\n" +
            "STEP-BY-STEP VERIFICATION FOR LOGICAL ADDRESS 1040:\n" +
            "  1. Page Number p = 1040 / 512 = 2 (points to Page Table Entry 2)\n" +
            "  2. Offset d      = 1040 % 512 = 16 bytes\n" +
            "  3. Lookup PT[2]  = Frame 5 in physical RAM\n" +
            "  4. Physical Addr = (5 * 512) + 16 = 2560 + 16 = 2576. (VERIFIED!)";

        String m3_t5 = banner("5. EXAM CHEAT-SHEET & VIRTUAL MEMORY COMPARISON MATRIX") + "\n" +
            table3(" MEMORY MODEL", " ADVANTAGES", " DISADVANTAGES & TRAPS", 20, 22, 28, new String[][]{
                {" Contiguous Part", " Simple hardware", " Severe external frag"},
                {" Pure Paging", " No external frag", " Internal frag on last pg"},
                {" Inverted Page Table", " Compact table RAM", " Slow hash chain search"},
                {" Multi-Level Paging", " Supports 64-bit space", " 4+ memory access penalty"}
            }) + "\n\n" +
            "★ INTERNAL FRAGMENTATION IN PAGING:\n" +
            "  Paging completely stops external fragmentation, but incurs internal\n" +
            "  fragmentation on the final page of a process (average 0.5 * Page Size).\n\n" +
            "★ OFFSET NEVER CHANGES:\n" +
            "  Logical Address = [p | d] -> Physical Address = [f | d].\n" +
            "  Offset 'd' is copied directly into physical RAM; only 'p' is translated!";

        emitManual(out, 2, "03", "PAGING & ADDRESS TRANSLATION", "MISSION 03: VIRTUAL MEMORY & MMU FRAME MAPPING",
                   "★★☆☆☆ (FUNDAMENTAL)", "LOGICAL ADDRESS | PAGE TABLE | PHYSICAL ADDRESS | OFFSET | MMU",
                   m3_t1, m3_t2, m3_t3, m3_t4, m3_t5, 2, 2);

        // ─────────────────────────────────────────────────────────────────────
        // MODULE 04: PAGE REPLACEMENT
        // ─────────────────────────────────────────────────────────────────────
        String m4_t1 = banner("1. EXECUTIVE OVERVIEW & EVICTION PROTOCOLS") + "\n" +
            wrapParagraph("In demand-paged systems, when a referenced page is not present in physical RAM, a PAGE FAULT occurs. If all memory frames are full, the OS must evict a 'victim frame' to swap storage.", 72, "") + "\n\n" +
            "PAGE FAULT TRAP & SWAP WORKFLOW:\n" +
            "  1. CPU Memory Reference ──────────> Page Table Entry (Valid Bit = 0)\n" +
            "                                             │\n" +
            "                                             ▼\n" +
            "  2. Hardware Trap to OS Kernel <──── Page Fault Interrupt\n" +
            "        │\n" +
            "        ├─> Find free physical frame (or evict victim frame via LRU)\n" +
            "        ├─> Issue Disk I/O Read to swap partition (Process Sleeps)\n" +
            "        └─> Update Page Table (Valid Bit = 1) ──> Restart Instruction!\n\n" +
            "CORE EVICTION POLICIES:\n" +
            "  • FIFO : Evicts oldest loaded page. Vulnerable to Belady's Anomaly.\n" +
            "  • LRU  : Evicts page unreferenced longest in past. (Stack Algorithm).\n" +
            "  • OPT  : Evicts page unreferenced for longest future time. (Benchmark).";

        String m4_t2 = banner("2. PERFORMANCE METRICS & ACCESS TIME FORMULAS") + "\n" +
            table2(" METRIC NAME", " FORMULA & COMPUTATION", 25, 46, new String[][]{
                {" Total References (N)", " N = Page_Hits (H) + Page_Faults (F)"},
                {" Hit Ratio (HR)", " HR = (H / N) * 100%"},
                {" Fault Ratio (FR)", " FR = (F / N) * 100% = 100% - HR"},
                {" Effective Access Time", " EAT = (1 - p)*t_ram + p*(Fault_Service_Time)"},
                {" Fault Service Overhead", " Disk Seek + Latency + Swap Read/Write"}
            }) + "\n\n" +
            "CALCULATION RUNWAY (PAGE FAULT IMPACT ON SPEED):\n" +
            "  • Given : RAM Access t_ram = 100 ns, Fault Service = 10 ms (10M ns)\n" +
            "  • With Fault Rate p = 0.001 (0.1% faults, 99.9% hits):\n" +
            "  • EAT   = (0.999 * 100 ns) + (0.001 * 10,000,000 ns)\n" +
            "  • EAT   = 99.9 ns + 10,000 ns = 10,099.9 ns (Over 100x slower!)";

        String m4_t3 = banner("3. LRU & CLOCK PAGE REPLACEMENT PSEUDOCODE") + "\n" +
            codeBox("ENGINE 01: LRU CACHE ENGINE (LEAST RECENTLY USED EVICTION)", new String[]{
                "function access_page(int page, List<Integer> frames, int capacity) {",
                "    if (frames.contains(page)) {",
                "        record_hit();",
                "        frames.remove((Integer) page); // Move to most recent",
                "        frames.add(page);",
                "    } else {",
                "        record_fault();",
                "        if (frames.size() == capacity) {",
                "            int victim = frames.remove(0); // Evict LRU at head",
                "        }",
                "        frames.add(page);",
                "    }",
                "}"
            }) + "\n\n" +
            "COMPLEXITY: LRU: O(1) with LinkedHashSet | Clock: O(1) amortized";

        // 5-column table: widths = 9, 13, 13, 13, 20 -> 68 + 6 = 74
        String m4_t4 = banner("4. STEP-BY-STEP WORKED EXAMPLE (LRU, 3 FRAMES)") + "\n" +
            "REFERENCE STRING: 7, 0, 1, 2, 0, 3, 0, 4 (Physical Capacity = 3 Frames)\n\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            table5(" REF PAG", " FRAME 1 (F1)", " FRAME 2 (F2)", " FRAME 3 (F3)", " CACHE STATUS / EVENT",
                   9, 13, 13, 13, 20, new String[][]{
                       {"    7   ", "      7      ", "      -      ", "      -      ", " PAGE FAULT (Cold)  "},
                       {"    0   ", "      7      ", "      0      ", "      -      ", " PAGE FAULT (Cold)  "},
                       {"    1   ", "      7      ", "      0      ", "      1      ", " PAGE FAULT (Cold)  "},
                       {"    2   ", "      2      ", "      0      ", "      1      ", " FAULT (7 evicted)  "},
                       {"    0   ", "      2      ", "      0      ", "      1      ", " ★ CACHE HIT!       "},
                       {"    3   ", "      2      ", "      0      ", "      3      ", " FAULT (1 evicted)  "},
                       {"    0   ", "      2      ", "      0      ", "      3      ", " ★ CACHE HIT!       "},
                       {"    4   ", "      4      ", "      0      ", "      3      ", " FAULT (2 evicted)  "}
                   }) + "\n\n" +
            "METRICS SUMMARY: Total Refs = 8 | Hits = 2 (25.0%) | Faults = 6 (75.0%)";

        String m4_t5 = banner("5. EXAM CHEAT-SHEET & REPLACEMENT TRAPS") + "\n" +
            table3(" POLICY", " EVICTION CRITERIA", " KEY CHARACTERISTICS / TRAP", 10, 29, 31, new String[][]{
                {" FIFO", " Oldest arrived frame in RAM", " Belady's Anomaly risk"},
                {" LRU", " Unreferenced longest past", " Stack algorithm; Optimal HW"},
                {" OPT/MIN", " Unreferenced longest future", " Theoretical benchmark"},
                {" CLOCK", " Circular queue + Ref bit", " Second-chance LRU approx"}
            }) + "\n\n" +
            "★ BELADY'S ANOMALY:\n" +
            "  For FIFO, adding more physical frames can counter-intuitively INCREASE\n" +
            "  the number of page faults (e.g. stream 1,2,3,4,1,2,5,1,2,3,4,5).\n" +
            "  Stack Algorithms (LRU, Optimal) are mathematically immune to Belady's.";

        emitManual(out, 3, "04", "PAGE REPLACEMENT POLICIES", "MISSION 04: DEMAND PAGING & CACHE FAULT REDUCTION",
                   "★★★☆☆ (MODERATE)", "FIFO | LRU | OPTIMAL | BELADY'S ANOMALY | HIT RATIO",
                   m4_t1, m4_t2, m4_t3, m4_t4, m4_t5, 3, 3);

        // ─────────────────────────────────────────────────────────────────────
        // MODULE 05: SEGMENTATION
        // ─────────────────────────────────────────────────────────────────────
        String m5_t1 = banner("1. EXECUTIVE OVERVIEW & LOGICAL ADDRESS SPACE") + "\n" +
            wrapParagraph("Segmentation models memory from the programmer's view: variable-length logical units such as Code, Stack, Heap, and Symbol Tables.", 72, "") + "\n\n" +
            "SEGMENTATION MMU HARDWARE TRANSLATION SCHEMATIC:\n" +
            "  CPU Logical Address = [ Segment (s) │ Offset (d) ]\n" +
            "                               │            │\n" +
            "                               ▼            ├──────────────────────┐\n" +
            "                       ┌──────────────┐     │                      │\n" +
            "                       │SEGMENT TABLE │     │                      │\n" +
            "                       │ Base │ Limit │     ▼                      ▼\n" +
            "                       └──────────────┘  d < Limit?          Physical Addr\n" +
            "                          │       │     /          \\               │\n" +
            "                          │       └────> NO: TRAP   YES            │\n" +
            "                          │                  (FAULT) │             │\n" +
            "                          ▼                          ▼             ▼\n" +
            "                  Physical RAM Base ───────────(+)─────────> [ Base + d ]";

        String m5_t2 = banner("2. BOUNDS CHECKING & ADDRESS TRANSLATION EQUATIONS") + "\n" +
            table2(" HARDWARE CHECK / PARAM", " MATHEMATICAL VERIFICATION & LOGIC", 25, 46, new String[][]{
                {" Segment Length Register", " Check: s < STLR (Segment Table Length Reg)"},
                {" Segment Offset Limit", " Check: 0 <= d < SegmentTable[s].Limit"},
                {" Physical Address (PA)", " PA = SegmentTable[s].Base + d"},
                {" Out-of-Bounds Trap", " IF d >= Limit THEN TRAP(SEGMENTATION_FAULT)"},
                {" Protection Violation", " IF operation not permitted THEN TRAP(PROT)"}
            }) + "\n\n" +
            "CALCULATION RUNWAY (SAMPLE BOUNDS CHECK):\n" +
            "  • Given : Segment 0 Base = 1400, Limit = 1000 bytes\n" +
            "  • Req <Seg 0, 420> : 420 < 1000? YES -> PA = 1400 + 420 = 1820 (OK)\n" +
            "  • Req <Seg 0, 1000>: 1000 < 1000? NO -> TRAP: SEGMENTATION FAULT!";

        String m5_t3 = banner("3. TRANSLATION PSEUDOCODE") + "\n" +
            codeBox("SEGMENTATION BOUNDS VALIDATOR & TRANSLATION ENGINE", new String[]{
                "function translate_seg(int s, int d, SegmentEntry[] segTable) {",
                "    if (s < 0 || s >= segTable.length) {",
                "        raise_trap('ILLEGAL SEGMENT SELECTOR');",
                "    }",
                "    SegmentEntry e = segTable[s];",
                "    if (d < 0 || d >= e.limit) {",
                "        raise_trap('SEGMENTATION FAULT: Offset exceeds segment limit');",
                "    }",
                "    return e.base + d; // Physical RAM Address",
                "}"
            }) + "\n\n" +
            "COMPLEXITY: Translation Time: O(1) | Space: O(Number of Segments)";

        // 6-column table: widths = 5, 14, 10, 10, 8, 20 -> 67 + 7 = 74
        String m5_t4 = banner("4. STEP-BY-STEP WORKED EXAMPLE & SEGMENT TABLE MAP") + "\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            table6(" SEG ", " SEGMENT NAME ", "   BASE   ", "  LIMIT   ", " OFFSET ", " RESULT / PHYSICAL  ",
                   5, 14, 10, 10, 8, 20, new String[][]{
                       {"  0  ", " Main Code    ", "   1400   ", "   1000   ", "  420   ", " Valid -> PA = 1820 "},
                       {"  1  ", " Call Stack   ", "   6300   ", "   0400   ", "  450   ", " TRAP: SEG FAULT!   "},
                       {"  2  ", " Symbol Table ", "   4300   ", "   0500   ", "  080   ", " Valid -> PA = 4380 "},
                       {"  3  ", " Subroutines  ", "   3200   ", "   1500   ", " 1500   ", " TRAP: d == Limit!  "}
                   }) + "\n\n" +
            "CASE EVALUATIONS:\n" +
            "  • Case 1: <Seg 0, 420>  : 420 < 1000 -> Valid -> PA = 1400 + 420 = 1820\n" +
            "  • Case 2: <Seg 1, 450>  : 450 exceeds Limit 400! -> SEGMENTATION FAULT\n" +
            "  • Case 3: <Seg 2, 80>   : 80 < 500   -> Valid -> PA = 4300 + 80 = 4380\n" +
            "  • Case 4: <Seg 3, 1500> : Offset == Limit is TRAP! (d must be < Limit).";

        String m5_t5 = banner("5. EXAM CHEAT-SHEET: PAGING VS SEGMENTATION") + "\n" +
            table3(" ARCHITECTURAL FACT", " PAGING", " SEGMENTATION", 20, 22, 28, new String[][]{
                {" Block Dimension", " Fixed size (4 KB)", " Variable length bytes"},
                {" User Visibility", " Invisible (Hardware)", " Visible (Code/Stack/Heap)"},
                {" Fragmentation", " Internal frag only", " External frag only"},
                {" Table Contents", " Frame number (f)", " Base address + Limit size"},
                {" Sharing/Protection", " Page access flags", " Natural logical units"}
            }) + "\n\n" +
            "★ COMMON EXAM TRAP:\n" +
            "  In Paging, offset 'd' can be up to Page_Size - 1.\n" +
            "  In Segmentation, offset 'd' must be LESS THAN Limit (d < Limit)!\n" +
            "  If d == Limit, it immediately triggers a SEGMENTATION FAULT!";

        emitManual(out, 4, "05", "SEGMENTATION & MEMORY PROTECTION", "MISSION 05: VARIABLE BOUNDS & LIMIT TRAP DETECTION",
                   "★★☆☆☆ (FUNDAMENTAL)", "BASE | LIMIT | BOUNDS CHECK | SEGMENTATION FAULT | TRAP",
                   m5_t1, m5_t2, m5_t3, m5_t4, m5_t5, 4, 2);

        // ─────────────────────────────────────────────────────────────────────
        // MODULE 06: FRAGMENTATION
        // ─────────────────────────────────────────────────────────────────────
        String m6_t1 = banner("1. EXECUTIVE OVERVIEW: MEMORY PARTITIONING STRATEGIES") + "\n" +
            wrapParagraph("Contiguous allocation assigns each process a single contiguous section of memory. This produces two distinct forms of fragmentation: Internal (unused partition bytes) and External (scattered free holes).", 72, "") + "\n\n" +
            "CONTIGUOUS MEMORY HOLE & PARTITION SCHEMATIC:\n" +
            "0K              100K        300K        500K        800K           1024K\n" +
            "┌───────────────┬───────────┬───────────┬───────────┬──────────────┐\n" +
            "│  OS RESERVED  │ PROCESS 1 │ FREE HOLE │ PROCESS 2 │ FREE HOLE    │\n" +
            "│   (100 KB)    │ (200 KB)  │ (200 KB)  │ (300 KB)  │ (224 KB)     │\n" +
            "└───────────────┴───────────┴───────────┴───────────┴──────────────┘\n" +
            "                                  ▲                       ▲\n" +
            "                                  └────── Free Holes ─────┘";

        String m6_t2 = banner("2. FRAGMENTATION METRICS & HOLE EQUATIONS") + "\n" +
            table2(" METRIC NAME", " FORMULA & COMPUTATION", 25, 46, new String[][]{
                {" Internal Fragmentation", " Internal_Frag = Block_Size - Job_Size"},
                {" External Fragmentation", " Σ Free_Holes (when Σ Holes >= Job, max < Job)"},
                {" Knuth's 50% Rule", " Number_of_Holes ≈ 0.5 * Allocated_Blocks"},
                {" Memory Utilization", " Util% = (Total_Allocated / Total_RAM) * 100%"},
                {" Compaction Free Yield", " Continuous_Free = Σ All_Hole_Sizes"}
            }) + "\n\n" +
            "PLACEMENT POLICIES COMPARISON:\n" +
            "  • First Fit : Searches from start; takes first hole with size >= req.\n" +
            "  • Best Fit  : Searches list; takes smallest hole with size >= req.\n" +
            "  • Worst Fit : Searches entire list, allocates in largest available hole.";

        String m6_t3 = banner("3. BEST-FIT & FIRST-FIT ALLOCATION PSEUDOCODE") + "\n" +
            codeBox("ENGINE 01: FIRST-FIT PARTITION ALLOCATOR", new String[]{
                "function allocate_first_fit(int reqSize, List<Hole> holes) {",
                "    for (Hole h : holes) {",
                "        if (h.size >= reqSize) {",
                "            return split_and_allocate(h, reqSize);",
                "        }",
                "    }",
                "    return null; // No adequate hole found",
                "}"
            }) + "\n\n" +
            "COMPLEXITY: First Fit: O(N) average | Best Fit: O(N) exhaustive search";

        // 5-column table: widths = 9, 13, 13, 13, 20 -> 68 + 6 = 74
        String m6_t4 = banner("4. STEP-BY-STEP WORKED EXAMPLE & PLACEMENT TRACE") + "\n" +
            "HOLES: [H1: 100 KB]  [H2: 500 KB]  [H3: 200 KB]  [H4: 300 KB]\n" +
            "Incoming Process Request: 212 KB\n\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            table5(" POLICY  ", " SEARCH ORDER", " HOLE CHOSEN ", "LEFTOVER HOLE", "FRAGMENTATION EFFECT",
                   9, 13, 13, 13, 20, new String[][]{
                       {"First Fit", " H1->H2(500K)", " H2 (500 KB) ", " 288 KB Free ", " Large usable hole  "},
                       {"Best Fit ", " Full Search ", " H4 (300 KB) ", "  88 KB Free ", " Tiny sliver created"},
                       {"Worst Fit", " Full Search ", " H2 (500 KB) ", " 288 KB Free ", " Largest leftover   "}
                   }) + "\n\n" +
            "OBSERVATION:\n" +
            "  • First Fit is fastest and leaves a large 288 KB usable hole.\n" +
            "  • Best Fit leaves only 88 KB, which is too small for incoming jobs!";

        String m6_t5 = banner("5. EXAM CHEAT-SHEET: STRATEGIES & COMPACTION") + "\n" +
            table3(" POLICY", " ALLOCATION HEURISTIC", " PROS, CONS & PERFORMANCE", 10, 29, 31, new String[][]{
                {"First Fit", " First hole with size >= req", " Fastest; low search overhead"},
                {"Best Fit", " Smallest hole size >= req", " Leaves unusable tiny slivers"},
                {"Worst Fit", " Largest available hole", " Leaves largest leftover hole"},
                {"Next Fit", " First Fit resuming from last", " Spreads allocations evenly"}
            }) + "\n\n" +
            "★ BEST FIT IS NOT ALWAYS 'BEST':\n" +
            "  Counter-intuitively, Best Fit produces tiny leftover hole slivers that\n" +
            "  are too small for any other job, creating severe external frag.\n\n" +
            "★ KNUTH'S 50% RULE:\n" +
            "  For First Fit, for every N allocated blocks, about 0.5 N blocks are lost\n" +
            "  to external fragmentation (one-third of memory may be unusable!).";

        emitManual(out, 5, "06", "CONTIGUOUS ALLOCATION & FRAGMENTATION", "MISSION 06: PARTITION STRATEGIES & COMPACTION",
                   "★★★☆☆ (MODERATE)", "FIRST FIT | BEST FIT | WORST FIT | INTERNAL FRAG | EXTERNAL FRAG",
                   m6_t1, m6_t2, m6_t3, m6_t4, m6_t5, 5, 1);

        // ─────────────────────────────────────────────────────────────────────
        // MODULE 07: DISK SCHEDULING
        // ─────────────────────────────────────────────────────────────────────
        String m7_t1 = banner("1. EXECUTIVE OVERVIEW & PLATTER MECHANICS") + "\n" +
            wrapParagraph("Disk Seek Time (moving mechanical arm to target cylinder) dominates I/O latency. Disk scheduling reorders pending I/O requests to minimize Total Head Movement (THM).", 72, "") + "\n\n" +
            "ROTATING PLATTER & READ/WRITE HEAD ACTUATOR SCHEMATIC:\n" +
            "     ┌───────────────────┐\n" +
            "     │ ( ( ( ʘ ) ) )     │  Spindle rotates at 7200 RPM / 10000 RPM\n" +
            "     └─────────┬─────────┘\n" +
            "               │  Actuator Arm Moves Bi-Directionally (Seek Time)\n" +
            "     ┌─────────▼─────────┐\n" +
            "     │ [ R/W HEAD ] <====│========[ ACTUATOR MOTOR ]\n" +
            "     └───────────────────┘\n" +
            "      Track 000 ───────────── Track 199 (Target Cylinders)";

        String m7_t2 = banner("2. DISK SEEK & LATENCY METRIC FORMULAS") + "\n" +
            table2(" METRIC NAME", " FORMULA & HARDWARE COMPUTATION", 25, 46, new String[][]{
                {" Step Seek Distance", " Seek_Dist = |Target_Cylinder - Current_Cyl|"},
                {" Total Head Movement", " THM = Σ |C[i] - C[i-1]|"},
                {" Average Seek Distance", " Avg_Seek = THM / Total_Requests"},
                {" Avg Rotational Latency", " Latency = 0.5 * (60 / RPM) seconds"},
                {" Total Disk Access Time", " Access_Time = Seek_Time + Latency + Transfer"}
            }) + "\n\n" +
            "CALCULATION RUNWAY (7200 RPM HARD DRIVE COMPUTATION):\n" +
            "  • Rotational Period = 60 / 7200 = 0.00833 s = 8.33 ms\n" +
            "  • Average Rotational Latency = 8.33 ms / 2 = 4.17 ms\n" +
            "  (Mechanical seek time typically dominates at 4 ms to 9 ms).";

        String m7_t3 = banner("3. SSTF & SCAN DISK SCHEDULING PSEUDOCODE") + "\n" +
            codeBox("ENGINE 01: SSTF (SHORTEST SEEK TIME FIRST)", new String[]{
                "function schedule_sstf(List<Integer> queue, int head) {",
                "    int thm = 0;",
                "    while (!queue.isEmpty()) {",
                "        int nextCyl = find_closest(queue, head);",
                "        thm += abs(nextCyl - head);",
                "        head = nextCyl;",
                "        queue.remove((Integer) nextCyl);",
                "    }",
                "    return thm;",
                "}"
            }) + "\n\n" +
            "COMPLEXITY: SSTF: O(N^2) | SCAN / LOOK: O(N log N) sorting phase";

        // 5-column table: widths = 5, 10, 14, 14, 25 -> 68 + 6 = 74
        String m7_t4 = banner("4. STEP-BY-STEP WORKED EXAMPLE & HEAD TRAVERSAL") + "\n" +
            "QUEUE: 98, 183, 37, 122, 14, 124, 65, 67 | Initial Head Position = 53\n\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            table5("STEP ", " FROM CYL ", " TO CYLINDER  ", " SEEK (CYLS)  ", " ACCUMULATED THM         ",
                   5, 10, 14, 14, 25, new String[][]{
                       {"  1  ", " 53 (Init)", " 65           ", " |65 - 53|= 12", " Total =  12 cylinders   "},
                       {"  2  ", " 65       ", " 67           ", " |67 - 65|=  2", " Total =  14 cylinders   "},
                       {"  3  ", " 67       ", " 37           ", " |37 - 67|= 30", " Total =  44 cylinders   "},
                       {"  4  ", " 37       ", " 14           ", " |14 - 37|= 23", " Total =  67 cylinders   "},
                       {"  5  ", " 14       ", " 98           ", " |98 - 14|= 84", " Total = 151 cylinders   "},
                       {"  6  ", " 98       ", " 122          ", " |122- 98|= 24", " Total = 175 cylinders   "},
                       {"  7  ", " 122      ", " 124          ", " |124-122|=  2", " Total = 177 cylinders   "},
                       {"  8  ", " 124      ", " 183          ", " |183-124|= 59", " Total = 236 cylinders   "}
                   }) + "\n\n" +
            "TOTAL HEAD MOVEMENT (SSTF) = 236 cyls (vs FCFS = 640 cyls: 63% savings!).";

        String m7_t5 = banner("5. EXAM CHEAT-SHEET: DISK ALGORITHMS COMPARISON") + "\n" +
            table3(" ALGORITHM", " PRIMARY ADVANTAGE", " KEY TRAP / WEAKNESS", 10, 29, 31, new String[][]{
                {" FCFS", " Fair, simple, no starvation", " Wild head swings, high THM"},
                {" SSTF", " Low seek time locally", " Starvation for outer tracks"},
                {" SCAN", " Elevator sweep, no starv", " Bounces at disk edge (0/max)"},
                {" C-SCAN", " Uniform wait across tracks", " Resets head without service"},
                {" LOOK", " Reverses at last request", " Does not travel to 0/max"},
                {" C-LOOK", " Uniform wait, optimal sweep", " Circular reset jump"}
            }) + "\n\n" +
            "★ EXAM TIP (SCAN VS LOOK):\n" +
            "  In SCAN, the head travels all the way to cylinder 0 (or max cylinder),\n" +
            "  even if there are no requests there! LOOK reverses direction immediately\n" +
            "  after serving the last request in that direction, saving head movement.";

        emitManual(out, 6, "07", "DISK I/O HEAD SCHEDULING", "MISSION 07: CYLINDER TRAVERSAL & SEEK-TIME MINIMIZATION",
                   "★★★☆☆ (MODERATE)", "FCFS | SSTF | SCAN | LOOK | C-SCAN | TOTAL HEAD MOVEMENT",
                   m7_t1, m7_t2, m7_t3, m7_t4, m7_t5, 6, 0);

        // ─────────────────────────────────────────────────────────────────────
        // MODULE 08: MULTIPROGRAMMING
        // ─────────────────────────────────────────────────────────────────────
        String m8_t1 = banner("1. EXECUTIVE OVERVIEW: THE NEED FOR CONCURRENCY") + "\n" +
            wrapParagraph("Processes spend much of their lifetime waiting for I/O. Multiprogramming maintains multiple processes in memory so the CPU switches to another process when one is blocked.", 72, "") + "\n\n" +
            "UNIPROGRAMMING VS MULTIPROGRAMMING OVERLAP TIMELINE:\n" +
            "  UNIPROGRAMMING:\n" +
            "  [ CPU P1 ] [   I/O P1 (CPU IDLE!)   ] [ CPU P1 ] [ I/O P1 ]\n\n" +
            "  MULTIPROGRAMMING (DEGREE N = 2):\n" +
            "  [ CPU P1 ] [ CPU P2 (WHILE P1 I/O)  ] [ CPU P1 ] [ CPU P2 ]\n" +
            "             [   I/O WAIT FOR P1      ]            [ I/O P2 ]";

        String m8_t2 = banner("2. PROBABILISTIC CONCURRENCY & CPU UTILIZATION FORMULAS") + "\n" +
            wrapParagraph("Let p = fraction of time a process spends waiting for I/O (0 <= p <= 1), and N = Degree of Multiprogramming (processes in RAM):", 72, "") + "\n\n" +
            table2(" METRIC NAME", " MATHEMATICAL FORMULA & CONCURRENCY LOGIC", 25, 46, new String[][]{
                {" Probability All Idle", " P(All_Idle) = p^N (where p = I/O wait frac)"},
                {" Probabilistic CPU Util", " CPU_Util = 1 - p^N"},
                {" Throughput Rate", " Throughput = Completed_Processes / Total_Time"},
                {" Turnaround Time", " TAT = Process_Finish_Time - Arrival_Time"},
                {" Amdahl's Law Speedup", " Speedup = 1 / ((1 - P) + P / N)"}
            }) + "\n\n" +
            "OBSERVATION:\n" +
            "  When p = 0.80, N=1 gives CPU Util = 20%. N=8 boosts CPU Util to 83.22%!\n" +
            "  However, if N exceeds physical RAM capacity, Thrashing occurs.";

        String m8_t3 = banner("3. CONCURRENCY ADMISSION & WORKING SET PSEUDOCODE") + "\n" +
            codeBox("CONCURRENCY ADMISSION CONTROLLER & WORKING SET MONITOR", new String[]{
                "function admit_process(Job job, int current_ram, int total_ram) {",
                "    if (current_ram + job.ram <= total_ram) {",
                "        degree_N++;",
                "        cpu_util = 1.0 - Math.pow(p_io_wait, degree_N);",
                "        return true; // Admitted to RAM",
                "    } else {",
                "        spool_to_disk(job); // Throttle admission to prevent Thrashing",
                "        return false;",
                "    }",
                "}"
            }) + "\n\n" +
            "COMPLEXITY: Admission Check: O(1) | Working Set Evaluation: O(N * Δ)";

        // 5-column table: widths = 9, 13, 13, 13, 20 -> 68 + 6 = 74
        String m8_t4 = banner("4. STEP-BY-STEP WORKED EXAMPLE & UTILIZATION SCALING") + "\n" +
            wrapParagraph("WORKLOAD PARAMETERS: Process I/O wait fraction p = 0.80 (80% time waiting for I/O)", 72, "") + "\n\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            table5(" DEGREE N", " I/O WAIT (p)", " PROB IDLE p^N", " CPU UTIL (%)", " SYSTEM EVALUATION  ",
                   9, 13, 13, 13, 20, new String[][]{
                       {"    1    ", " 0.80 (80%)  ", " 0.8000 (80%) ", " 20.00% Util ", " Terrible uniprogram"},
                       {"    2    ", " 0.80 (80%)  ", " 0.6400 (64%) ", " 36.00% Util ", " 80% gain over N=1  "},
                       {"    4    ", " 0.80 (80%)  ", " 0.4096 (41%) ", " 59.04% Util ", " Solid concurrency  "},
                       {"    8    ", " 0.80 (80%)  ", " 0.1678 (17%) ", " 83.22% Util ", " Optimal throughput "},
                       {"   12    ", " 0.80 (80%)  ", " 0.0687 (07%) ", " 93.13% Util ", " Maximum sweet spot "}
                   }) + "\n\n" +
            wrapParagraph("RESULT: Concurrency scales CPU utilization from 20% to over 93% without extra CPUs!", 72, "");

        String m8_t5 = banner("5. EXAM CHEAT-SHEET: THRASHING & SCHEDULER HIERARCHY") + "\n" +
            table3(" SCHEDULER TIER", " PRIMARY RESPONSIBILITY", " EXECUTION FREQUENCY", 20, 22, 28, new String[][]{
                {" Long-Term (Job)", " Controls Degree of MP", " Infrequent (secs/mins)"},
                {" Medium-Term (Swap)", " Swaps in/out of RAM", " Intermediate (alleviate)"},
                {" Short-Term (CPU)", " Allocates CPU core", " Milliseconds (frequent)"}
            }) + "\n\n" +
            "★ THE THRASHING PHENOMENON:\n" +
            "  When Degree of Multiprogramming (DOM) increases beyond available RAM:\n" +
            "  1. Processes page fault frequently as frames become scarce.\n" +
            "  2. CPU scheduler notices CPU is idle (waiting for disk page swaps).\n" +
            "  3. OS naively admits MORE processes to raise CPU load, worsening thrash!\n" +
            "  4. CPU Utilization plummets to near 0% as system spends all time paging.\n\n" +
            "★ WORKING SET STRATEGY (Denning):\n" +
            "  The Working Set WSS(i) is pages process Pi accessed in time window Δ.\n" +
            "  • If Σ WSS(i) <= Total_Frames -> System is stable and productive.\n" +
            "  • If Σ WSS(i) > Total_Frames  -> Thrashing begins! OS suspends a job.";

        emitManual(out, 7, "08", "MULTIPROGRAMMING & CONCURRENCY", "MISSION 08: DEGREE OF CONCURRENCY & CPU UTILIZATION",
                   "★★★★☆ (ADVANCED)", "CPU UTILIZATION | DEGREE OF MULTIPROGRAMMING | THRASHING | I/O WAIT",
                   m8_t1, m8_t2, m8_t3, m8_t4, m8_t5, 7, 0);

        out.println("    }\n");
        out.println("    public static ManualEntry getManual(int index) {");
        out.println("        if (index >= 0 && index < MANUALS.length) {");
        out.println("            return MANUALS[index];");
        out.println("        }");
        out.println("        return MANUALS[0];");
        out.println("    }");
        out.println("}");
        out.close();
        System.out.println("Generation completed successfully!");
    }

    private static void emitManual(PrintWriter out, int idx, String id, String title, String subtitle,
                                   String complexity, String tags,
                                   String t1, String t2, String t3, String t4, String t5, int simTab, int quizMod) {
        out.println("        // " + repeat('─', 69));
        out.println("        // " + id + ". " + title);
        out.println("        // " + repeat('─', 69));
        out.println("        MANUALS[" + idx + "] = new ManualEntry(");
        out.println("            \"" + id + "\",");
        out.println("            \"" + title + "\",");
        out.println("            \"" + subtitle + "\",");
        out.println("            \"" + complexity + "\",");
        out.println("            \"" + tags + "\",");
        out.println(toJavaLiteral(t1) + ",");
        out.println(toJavaLiteral(t2) + ",");
        out.println(toJavaLiteral(t3) + ",");
        out.println(toJavaLiteral(t4) + ",");
        out.println(toJavaLiteral(t5) + ",");
        out.println("            " + simTab + ", " + quizMod);
        out.println("        );\n");
    }
}
