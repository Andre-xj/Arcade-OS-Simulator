package arcadeos;

/**
 * Enhanced Knowledge Base and Core Algorithm Logic for the 8 Operating System Manuals.
 * Standardized to 74-character width across all modules and tabs for pixel-perfect
 * alignment, equal 22px side margins, cyber framing, and complete visual justification.
 */
public class ArchiveManualData {

    public static class ManualEntry {
        public String id, title, subtitle, complexity, tags;
        public String theory, formulas, pseudocode, workedExample, cheatSheet;
        public int simulatorTabIndex, quizModuleIndex;

        public ManualEntry(String id, String title, String subtitle, String complexity, String tags,
                           String theory, String formulas, String pseudocode, String workedExample,
                           String cheatSheet, int simTab, int quizMod) {
            this.id = id; this.title = title; this.subtitle = subtitle;
            this.complexity = complexity; this.tags = tags;
            this.theory = theory; this.formulas = formulas;
            this.pseudocode = pseudocode; this.workedExample = workedExample;
            this.cheatSheet = cheatSheet;
            this.simulatorTabIndex = simTab; this.quizModuleIndex = quizMod;
        }
    }

    private static final ManualEntry[] MANUALS = new ManualEntry[8];

    static {
        // ─────────────────────────────────────────────────────────────────────
        // 01. CPU PROCESS SCHEDULING
        // ─────────────────────────────────────────────────────────────────────
        MANUALS[0] = new ManualEntry(
            "01",
            "CPU PROCESS SCHEDULING",
            "MISSION 01: DISPATCH RUNWAY & QUEUE ANALYSIS",
            "★★★☆☆ (MODERATE)",
            "FCFS | SJF | SRTF | ROUND ROBIN | GANTT TIMELINE",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 1. EXECUTIVE OVERVIEW & ARCHITECTURE                                   ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "The CPU Scheduler is responsible for allocating the execution core to\n" +
            "one of the runnable processes currently residing in the READY QUEUE.\n" +
            "\n" +
            "PROCESS STATE TRANSITION DIAGRAM:\n" +
            "┌───────┐ admit ┌───────┐ dispatch ┌─────────┐ exit ┌────────────┐\n" +
            "│  NEW  │ ────> │ READY │ ───────> │ RUNNING │ ───> │ TERMINATED │\n" +
            "└───────┘       └───────┘          └─────────┘      └────────────┘\n" +
            "                    ▲                   │\n" +
            "                    │   I/O Complete    ▼  I/O or Event Wait\n" +
            "                    └────────────── ┌─────────┐\n" +
            "                                    │ WAITING │\n" +
            "                                    └─────────┘\n" +
            "\n" +
            "2. FOUR SCHEDULING CRITERIA:\n" +
            "  • FCFS (First-Come, First-Served) :\n" +
            "    Non-preemptive. Strict FIFO queue ordered by Arrival Time.\n" +
            "\n" +
            "  • SJF (Shortest Job First) :\n" +
            "    Non-preemptive. Selects process with smallest Burst Time.\n" +
            "\n" +
            "  • SRTF (Shortest Remaining Time) :\n" +
            "    Preemptive SJF. Preempts current job if new job has smaller burst.\n" +
            "\n" +
            "  • Round Robin (RR) :\n" +
            "    Preemptive with Time Quantum (TQ). Enforces fair timesharing.",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. MATHEMATICAL FORMULAS & PERFORMANCE METRICS                         ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌─────────────────────────┬──────────────────────────────────────────────┐\n" +
            "│ METRIC NAME             │ FORMULA & COMPUTATION                        │\n" +
            "├─────────────────────────┼──────────────────────────────────────────────┤\n" +
            "│ Start Time (ST)         │ ST[i] = max(Arrival_Time[i], FT[i-1])        │\n" +
            "│ Finish Time (FT)        │ FT[i] = ST[i] + Burst_Time[i]                │\n" +
            "│ Turnaround Time (TAT)   │ TAT[i] = FT[i] - Arrival_Time[i]             │\n" +
            "│ Waiting Time (WT)       │ WT[i] = TAT[i] - Burst[i] = ST[i] - AT[i]    │\n" +
            "│ Average Waiting Time    │ AWT = (Σ WT[i]) / N                          │\n" +
            "│ Average Turnaround Time │ ATAT = (Σ TAT[i]) / N                        │\n" +
            "│ CPU Utilization         │ (Busy Time / Total Schedule Duration) * 100% │\n" +
            "│ Throughput              │ Completed Jobs / Unit Time                   │\n" +
            "└─────────────────────────┴──────────────────────────────────────────────┘\n" +
            "\n" +
            "CALCULATION RUNWAY (SAMPLE JOB TRACE):\n" +
            "  • Given Job J1: Arrival (AT) = 2.0, Burst (BT) = 5.0, Start (ST) = 4.0\n" +
            "  • Finish Time (FT)     = ST + BT = 4.0 + 5.0 = 9.0\n" +
            "  • Turnaround Time(TAT) = FT - AT = 9.0 - 2.0 = 7.0\n" +
            "  • Waiting Time (WT)    = TAT - BT = 7.0 - 5.0 = 2.0  (or ST - AT = 2.0)\n" +
            "\n" +
            "CRITICAL EVALUATION RULES:\n" +
            "  ★ Arrival Gap Rule     : If CPU was idle prior, ST[i] = Arrival_Time[i].\n" +
            "  ★ Minimum Overhead     : Context-switch time adds latency between jobs.\n" +
            "  ★ Throughput vs Latency: Maximizing throughput may raise tail latency.",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. ALGORITHM PSEUDOCODE & DISPATCH ENGINES                             ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌────────────────────────────────────────────────────────────────────────┐\n" +
            "│ ENGINE 01: FCFS DISPATCHER (NON-PREEMPTIVE FIFO QUEUE)                 │\n" +
            "├────────────────────────────────────────────────────────────────────────┤\n" +
            "│ function schedule_fcfs(Job[] jobs) {                                   │\n" +
            "│     sort_by_arrival_time(jobs); // O(N log N)                          │\n" +
            "│     double time = 0.0;                                                 │\n" +
            "│     for (Job j : jobs) {                                               │\n" +
            "│         if (time < j.arrival) time = j.arrival; // CPU idle gap        │\n" +
            "│         j.startTime  = time;                                           │\n" +
            "│         j.finishTime = time + j.burst;                                 │\n" +
            "│         j.turnaround = j.finishTime - j.arrival;                       │\n" +
            "│         j.waiting    = j.turnaround - j.burst;                         │\n" +
            "│         time = j.finishTime;                                           │\n" +
            "│     }                                                                  │\n" +
            "│ }                                                                      │\n" +
            "└────────────────────────────────────────────────────────────────────────┘\n" +
            "\n" +
            "┌────────────────────────────────────────────────────────────────────────┐\n" +
            "│ ENGINE 02: ROUND ROBIN DISPATCHER (PREEMPTIVE TIME SLICE Q)            │\n" +
            "├────────────────────────────────────────────────────────────────────────┤\n" +
            "│ function schedule_round_robin(Queue readyQ, double timeQuantum) {      │\n" +
            "│     while (!readyQ.isEmpty()) {                                        │\n" +
            "│         Job current = readyQ.dequeue();                                │\n" +
            "│         double slice = min(current.remainingTime, timeQuantum);        │\n" +
            "│         time += slice;                                                 │\n" +
            "│         current.remainingTime -= slice;                                │\n" +
            "│         if (current.remainingTime > 0) {                               │\n" +
            "│             readyQ.enqueue(current); // Preempt back to tail           │\n" +
            "│         } else {                                                       │\n" +
            "│             current.finishTime = time; // Terminated                   │\n" +
            "│         }                                                              │\n" +
            "│     }                                                                  │\n" +
            "│ }                                                                      │\n" +
            "└────────────────────────────────────────────────────────────────────────┘\n" +
            "\n" +
            "COMPLEXITY: FCFS: O(N log N) | Round Robin: O(Σ Bursts / Q)",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED TRACE & GANTT RUNWAY                            ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "SAMPLE WORKLOAD: 3 PROCESSES\n" +
            "  • J1: Arrival = 0.0, Burst = 4.0\n" +
            "  • J2: Arrival = 1.0, Burst = 3.0\n" +
            "  • J3: Arrival = 2.0, Burst = 2.0\n" +
            "\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            "┌────┬─────┬─────┬─────┬─────┬─────┬─────┬──────────────────────────┐\n" +
            "│JOB │ AT  │ BT  │ ST  │ FT  │ WT  │ TAT │ STATUS / NOTES           │\n" +
            "├────┼─────┼─────┼─────┼─────┼─────┼─────┼──────────────────────────┤\n" +
            "│ J1 │ 0.0 │ 4.0 │ 0.0 │ 4.0 │ 0.0 │ 4.0 │ Starts immediately at t=0│\n" +
            "│ J2 │ 1.0 │ 3.0 │ 4.0 │ 7.0 │ 3.0 │ 6.0 │ Waits from 1.0 to 4.0    │\n" +
            "│ J3 │ 2.0 │ 2.0 │ 7.0 │ 9.0 │ 5.0 │ 7.0 │ Waits from 2.0 to 7.0    │\n" +
            "└────┴─────┴─────┴─────┴─────┴─────┴─────┴──────────────────────────┘\n" +
            "\n" +
            "ASCII GANTT TIMELINE:\n" +
            "0.0                 4.0                 7.0           9.0\n" +
            " ┌───────────────────┬───────────────────┬─────────────┐\n" +
            " │    JOB 1 (J1)     │    JOB 2 (J2)     │  JOB 3 (J3) │\n" +
            " └───────────────────┴───────────────────┴─────────────┘\n" +
            "METRICS: Total Time=9.0 | Avg WT=2.67 | Avg TAT=5.67 | CPU Util=100%",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET & SCHEDULING COMPARISON MATRIX                     ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌────────────────────┬──────────────────────┬────────────────────────────┐\n" +
            "│ ALGORITHM          │ DECISION TYPE        │ OPTIMALITY & TRADE-OFFS    │\n" +
            "├────────────────────┼──────────────────────┼────────────────────────────┤\n" +
            "│ FCFS               │ Non-preemptive FIFO  │ Simple; Convoy Effect      │\n" +
            "│ SJF                │ Non-preemptive Short │ Minimum average WT; Starv  │\n" +
            "│ SRTF               │ Preemptive SJF       │ Optimal; High Preemption   │\n" +
            "│ Round Robin (RR)   │ Preemptive Quantum Q │ Fair timeshare; CS Cost    │\n" +
            "└────────────────────┴──────────────────────┴────────────────────────────┘\n" +
            "\n" +
            "★ CONVOY EFFECT IN FCFS:\n" +
            "  Long CPU-bound job runs first -> short jobs wait -> low CPU/IO util.\n" +
            "\n" +
            "★ SJF IS OPTIMAL:\n" +
            "  Minimizes average waiting time. (Aging mechanism prevents starvation).\n" +
            "\n" +
            "★ ROUND ROBIN QUANTUM RULE:\n" +
            "  • If TQ is too large  -> RR degenerates into FCFS.\n" +
            "  • If TQ is too small  -> Context-switch overhead destroys throughput.\n" +
            "  • Golden Rule         -> 80% of CPU bursts should be shorter than TQ.",
            0, 0
        );

        // ─────────────────────────────────────────────────────────────────────
        // 02. BANKER'S DEADLOCK AVOIDANCE
        // ─────────────────────────────────────────────────────────────────────
        MANUALS[1] = new ManualEntry(
            "02",
            "BANKER'S DEADLOCK AVOIDANCE",
            "MISSION 02: SAFETY VERIFICATION & RESOURCE GRAPHS",
            "★★★★☆ (ADVANCED)",
            "AVAILABLE | ALLOCATION | MAX MATRIX | NEED MATRIX | SAFE STATE",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 1. EXECUTIVE OVERVIEW & DEADLOCK ARCHITECTURE                          ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "Developed by Edsger Dijkstra, the Banker's Algorithm ensures that\n" +
            "resource allocations never lead to DEADLOCK. A deadlock occurs when a\n" +
            "set of processes are permanently blocked holding resources while waiting\n" +
            "for ones held by others.\n" +
            "\n" +
            "RESOURCE ALLOCATION GRAPH (RAG) CYCLE SCHEMATIC:\n" +
            "┌──────────────┐      Holds Resource     ┌──────────────┐\n" +
            "│  PROCESS P1  │ ──────────────────────> │ RESOURCE R1  │\n" +
            "└──────────────┘                         └──────────────┘\n" +
            "       ▲                                        │\n" +
            "       │ Requests                               │ Assigned to\n" +
            "       │                                        ▼\n" +
            "┌──────────────┐      Holds Resource     ┌──────────────┐\n" +
            "│ RESOURCE R2  │ <────────────────────── │  PROCESS P2  │\n" +
            "└──────────────┘                         └──────────────┘\n" +
            "\n" +
            "FOUR COFFMAN CONDITIONS FOR DEADLOCK:\n" +
            "  1. Mutual Exclusion  : Only one process can use a resource at a time.\n" +
            "  2. Hold and Wait     : Processes holding resources can request more.\n" +
            "  3. No Preemption     : Resources cannot be forcibly seized.\n" +
            "  4. Circular Wait     : A closed chain of processes waiting for each.",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. MATHEMATICAL MATRIX EQUATIONS & FEASIBILITY RULES                   ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌─────────────────────────┬──────────────────────────────────────────────┐\n" +
            "│ METRIC / VECTOR         │ MATHEMATICAL FORMULA & MATRIX LOGIC          │\n" +
            "├─────────────────────────┼──────────────────────────────────────────────┤\n" +
            "│ Need Matrix             │ Need[i][j] = Max[i][j] - Allocation[i][j]    │\n" +
            "│ Request Feasibility     │ Request[i] <= Need[i] AND Request[i] <= Avail│\n" +
            "│ Speculative Alloc       │ Avail' = Avail - Req; Alloc' = Alloc + Req   │\n" +
            "│ Need Adjustment         │ Need'  = Need  - Req                         │\n" +
            "│ Safety Condition        │ Finish[i] == false AND Need[i] <= Work       │\n" +
            "│ Work Update on Exit     │ Work_new = Work_current + Allocation[i]      │\n" +
            "│ Safe System State       │ All Finish[i] == true (Valid Sequence Exists)│\n" +
            "└─────────────────────────┴──────────────────────────────────────────────┘\n" +
            "\n" +
            "CALCULATION RUNWAY (SAMPLE VECTOR EVALUATION):\n" +
            "  • Given State : Available = [3, 3, 2], Process P1 Need = [1, 2, 2]\n" +
            "  • Feasibility : Need[1] <= Work -> [1, 2, 2] <= [3, 3, 2] -> TRUE (RUNS)\n" +
            "  • P1 Returns  : Work_new = [3, 3, 2] + Alloc[P1: 2, 0, 0] = [5, 3, 2]\n" +
            "\n" +
            "CRITICAL EVALUATION RULES:\n" +
            "  ★ Allocation Rule      : Always return Allocation to Work, NEVER Need.\n" +
            "  ★ Safe State Guarantee : Safe implies NO deadlock; Unsafe != Deadlock.\n" +
            "  ★ Single-Resource RAG  : Cycle in single-instance graph = DEADLOCK.",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. SAFETY ALGORITHM & RESOURCE-REQUEST PSEUDOCODE                      ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌────────────────────────────────────────────────────────────────────────┐\n" +
            "│ SAFETY ALGORITHM (DIJKSTRA DEADLOCK AVOIDANCE VERIFIER)                │\n" +
            "├────────────────────────────────────────────────────────────────────────┤\n" +
            "│ function verify_safety(int[] Available, int[][] Alloc, int[][] Need) { │\n" +
            "│     int[] Work = copy(Available);                                      │\n" +
            "│     boolean[] Finish = new boolean[N]; // initialized to false         │\n" +
            "│     List<Integer> safeSeq = new ArrayList<>();                         │\n" +
            "│     while (safeSeq.size() < N) {                                       │\n" +
            "│         boolean found = false;                                         │\n" +
            "│         for (int i = 0; i < N; i++) {                                  │\n" +
            "│             if (!Finish[i] && vector_less_equal(Need[i], Work)) {      │\n" +
            "│                 vector_add(Work, Alloc[i]); // Pi returns resources!   │\n" +
            "│                 Finish[i] = true;                                      │\n" +
            "│                 safeSeq.add(i);                                        │\n" +
            "│                 found = true;                                          │\n" +
            "│             }                                                          │\n" +
            "│         }                                                              │\n" +
            "│         if (!found) return null; // UNSAFE STATE (DEADLOCK RISK)       │\n" +
            "│     }                                                                  │\n" +
            "│     return safeSeq; // SAFE STATE: Valid Execution Order Confirmed     │\n" +
            "│ }                                                                      │\n" +
            "└────────────────────────────────────────────────────────────────────────┘\n" +
            "\n" +
            "COMPLEXITY: Time: O(M * N^2) | Space: O(N + M) for Work & Finish vectors",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED EXAMPLE & SAFE SEQUENCE                         ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "INITIAL STATE: Available = [3, 3, 2] (Resource Types: A, B, C)\n" +
            "\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            "┌─────┬─────────────┬─────────────┬─────────────┬────────────────────────┐\n" +
            "│PROC │ ALLOCATION  │     MAX     │NEED=MAX-ALOC│ STATUS IN LOOP         │\n" +
            "├─────┼─────────────┼─────────────┼─────────────┼────────────────────────┤\n" +
            "│ P0  │  0   1   0  │  7   5   3  │  7   4   3  │ Needs > Work (WAIT)    │\n" +
            "│ P1  │  2   0   0  │  3   2   2  │  1   2   2  │ [1,2,2]<=Work (RUNS)   │\n" +
            "│ P2  │  3   0   2  │  9   0   2  │  6   0   0  │ Needs > Work (WAIT)    │\n" +
            "│ P3  │  2   1   1  │  2   2   2  │  0   1   1  │ [0,1,1]<=Work (RUNS)   │\n" +
            "│ P4  │  0   0   2  │  4   3   3  │  4   3   1  │ [4,3,1]<=Work (RUNS)   │\n" +
            "└─────┴─────────────┴─────────────┴─────────────┴────────────────────────┘\n" +
            "\n" +
            "EXECUTION STEP SEQUENCE:\n" +
            "  1. P1 runs -> Releases [2,0,0] -> Work becomes [5, 3, 2]\n" +
            "  2. P3 runs -> Releases [2,1,1] -> Work becomes [7, 4, 3]\n" +
            "  3. P4 runs -> Releases [0,0,2] -> Work becomes [7, 4, 5]\n" +
            "  4. P0 runs -> Releases [0,1,0] -> Work becomes [7, 5, 5]\n" +
            "  5. P2 runs -> Releases [3,0,2] -> Work becomes [10, 5, 7]\n" +
            "\n" +
            "RESULT: SAFE SEQUENCE = < P1 -> P3 -> P4 -> P0 -> P2 >",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET & DEADLOCK ESSENTIALS                              ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌────────────────────┬──────────────────────┬────────────────────────────┐\n" +
            "│ HANDLING METHOD    │ MECHANISM APPLIED    │ RESOURCE UTILIZATION       │\n" +
            "├────────────────────┼──────────────────────┼────────────────────────────┤\n" +
            "│ Deadlock Ignorance │ Ostrich Algorithm    │ Maximum; risk of hang      │\n" +
            "│ Prevention         │ Invalidate 1 Coffman │ Low; strict constraints    │\n" +
            "│ Avoidance (Banker) │ Dynamic Safe Checks  │ Moderate; needs max claim  │\n" +
            "│ Detect & Recover   │ Periodic RAG Checks  │ High until abort occurs    │\n" +
            "└────────────────────┴──────────────────────┴────────────────────────────┘\n" +
            "\n" +
            "★ SAFE != DEADLOCK-FREE ONLY:\n" +
            "  • Safe State   : Safe sequence exists; NO deadlock is possible.\n" +
            "  • Unsafe State : Deadlock possible if all jobs demand full Max claim.\n" +
            "  • Deadlock     : Closed cycle of blocked processes (subset of Unsafe).\n" +
            "\n" +
            "★ SAFETY ALGORITHM WORK UPDATE TRAP:\n" +
            "  When process Pi finishes, it returns its ALLOCATION to the Work pool:\n" +
            "  Work_new = Work_current + Allocation[Pi]  (NEVER add Need!).",
            1, 1
        );

        // ─────────────────────────────────────────────────────────────────────
        // 03. PAGING & ADDRESS TRANSLATION
        // ─────────────────────────────────────────────────────────────────────
        MANUALS[2] = new ManualEntry(
            "03",
            "PAGING & ADDRESS TRANSLATION",
            "MISSION 03: VIRTUAL MEMORY & MMU FRAME MAPPING",
            "★★☆☆☆ (FUNDAMENTAL)",
            "LOGICAL ADDRESS | PAGE TABLE | PHYSICAL ADDRESS | OFFSET | MMU",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 1. EXECUTIVE OVERVIEW & MMU HARDWARE                                   ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "Paging completely eliminates EXTERNAL FRAGMENTATION by dividing logical\n" +
            "memory into fixed-sized blocks called PAGES, and physical RAM into\n" +
            "identically-sized blocks called FRAMES.\n" +
            "\n" +
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
            "  PHYSICAL RAM ADDRESS = [ FRAME NUMBER (f)│  OFFSET (d) ]\n" +
            "\n" +
            "KEY HARDWARE CHARACTERISTICS:\n" +
            "  • Pure Paging suffers NO external fragmentation whatsoever.\n" +
            "  • Internal fragmentation on final page only (avg 0.5 * Page Size).\n" +
            "  • Translation is implemented entirely in MMU hardware for speed.",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. MATHEMATICAL TRANSLATION FORMULAS & TLB METRICS                     ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌─────────────────────────┬──────────────────────────────────────────────┐\n" +
            "│ PARAMETER / FORMULA     │ MATHEMATICAL DEFINITION                      │\n" +
            "├─────────────────────────┼──────────────────────────────────────────────┤\n" +
            "│ Page Number (p)         │ p = floor(Logical_Address / Page_Size)       │\n" +
            "│ Page Offset (d)         │ d = Logical_Address % Page_Size              │\n" +
            "│ Physical Address (PA)   │ PA = (Frame_Number * Page_Size) + d          │\n" +
            "│ Total Page Table Size   │ Entries = Logical_Address_Space / Page_Size  │\n" +
            "│ Total Physical Frames   │ Frames  = Physical_RAM / Frame_Size          │\n" +
            "│ Effective Access (EAT)  │ EAT = h*(t_tlb + t_ram) + (1-h)*(t_tlb+2*ram)│\n" +
            "└─────────────────────────┴──────────────────────────────────────────────┘\n" +
            "\n" +
            "CALCULATION RUNWAY (SAMPLE ADDRESS TRANSLATION):\n" +
            "  • Given : Page Size = 4096 bytes (2^12), Logical Address = 13000\n" +
            "  • Step 1: Page Number p = floor(13000 / 4096) = 3\n" +
            "  • Step 2: Offset d      = 13000 % 4096 = 712 bytes\n" +
            "  • Step 3: Page Table Lookup: If PageTable[3] = Frame 7\n" +
            "  • Step 4: PA = (7 * 4096) + 712 = 28672 + 712 = 29384 in RAM.",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. HARDWARE TRANSLATION PSEUDOCODE                                     ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌────────────────────────────────────────────────────────────────────────┐\n" +
            "│ MMU ADDRESS TRANSLATION & TLB LOOKUP ENGINE                            │\n" +
            "├────────────────────────────────────────────────────────────────────────┤\n" +
            "│ function translate(int logical_address, int page_size, PageTable pt) { │\n" +
            "│     int p = logical_address / page_size;                               │\n" +
            "│     int d = logical_address % page_size;                               │\n" +
            "│     if (p >= pt.entries.length || !pt[p].valid) {                      │\n" +
            "│         trigger_interrupt(PAGE_FAULT, p); // Demand Paging Fault       │\n" +
            "│     }                                                                  │\n" +
            "│     int f = pt[p].frameNumber;                                         │\n" +
            "│     return (f * page_size) + d; // Physical Memory Address             │\n" +
            "│ }                                                                      │\n" +
            "└────────────────────────────────────────────────────────────────────────┘\n" +
            "\n" +
            "COMPLEXITY: Hardware Lookups: O(1) | TLB Hit: < 1 ns | RAM Read: 100 ns",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED EXAMPLE & PAGE TABLE MAP                        ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "PARAMETERS: Page Size = 512 bytes | Logical Address Space = 2048 bytes\n" +
            "\n" +
            "PAGE TABLE MAPPING & TRANSLATION TRACE:\n" +
            "┌─────────┬─────────────┬─────────────┬───────────┬──────────────────────┐\n" +
            "│ LOG ADDR│ PAGE NO (p) │ OFFSET (d)  │ FRAME (f) │ PHYSICAL RAM ADDR    │\n" +
            "├─────────┼─────────────┼─────────────┼───────────┼──────────────────────┤\n" +
            "│ 1040    │ 1040/512 = 2│ 1040%512 =16│ Frame 5   │ (5*512) + 16 = 2576  │\n" +
            "│ 0250    │ 0250/512 = 0│ 0250%512=250│ Frame 3   │ (3*512) +250 = 1786  │\n" +
            "│ 0700    │ 0700/512 = 1│ 0700%512=188│ Frame 1   │ (1*512) +188 =  700  │\n" +
            "│ 1600    │ 1600/512 = 3│ 1600%512 =64│ Frame 2   │ (2*512) + 64 = 1088  │\n" +
            "└─────────┴─────────────┴─────────────┴───────────┴──────────────────────┘\n" +
            "\n" +
            "STEP-BY-STEP VERIFICATION FOR LOGICAL ADDRESS 1040:\n" +
            "  1. Page Number p = 1040 / 512 = 2 (points to Page Table Entry 2)\n" +
            "  2. Offset d      = 1040 % 512 = 16 bytes\n" +
            "  3. Lookup PT[2]  = Frame 5 in physical RAM\n" +
            "  4. Physical Addr = (5 * 512) + 16 = 2560 + 16 = 2576. (VERIFIED!)",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET & VIRTUAL MEMORY COMPARISON MATRIX                 ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌────────────────────┬──────────────────────┬────────────────────────────┐\n" +
            "│ MEMORY MODEL       │ ADVANTAGES           │ DISADVANTAGES & TRAPS      │\n" +
            "├────────────────────┼──────────────────────┼────────────────────────────┤\n" +
            "│ Contiguous Part    │ Simple hardware      │ Severe external frag       │\n" +
            "│ Pure Paging        │ No external frag     │ Internal frag on last pg   │\n" +
            "│ Inverted Page Table│ Compact table RAM    │ Slow hash chain search     │\n" +
            "│ Multi-Level Paging │ Supports 64-bit space│ 4+ memory access penalty   │\n" +
            "└────────────────────┴──────────────────────┴────────────────────────────┘\n" +
            "\n" +
            "★ INTERNAL FRAGMENTATION IN PAGING:\n" +
            "  Paging completely stops external fragmentation, but incurs internal\n" +
            "  fragmentation on the final page of a process (average 0.5 * Page Size).\n" +
            "\n" +
            "★ OFFSET NEVER CHANGES:\n" +
            "  Logical Address = [p | d] -> Physical Address = [f | d].\n" +
            "  Offset 'd' is copied directly into physical RAM; only 'p' is translated!",
            2, 2
        );

        // ─────────────────────────────────────────────────────────────────────
        // 04. PAGE REPLACEMENT POLICIES
        // ─────────────────────────────────────────────────────────────────────
        MANUALS[3] = new ManualEntry(
            "04",
            "PAGE REPLACEMENT POLICIES",
            "MISSION 04: DEMAND PAGING & CACHE FAULT REDUCTION",
            "★★★☆☆ (MODERATE)",
            "FIFO | LRU | OPTIMAL | BELADY'S ANOMALY | HIT RATIO",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 1. EXECUTIVE OVERVIEW & EVICTION PROTOCOLS                             ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "In demand-paged systems, when a referenced page is not present in\n" +
            "physical RAM, a PAGE FAULT occurs. If all memory frames are full, the OS\n" +
            "must evict a 'victim frame' to swap storage.\n" +
            "\n" +
            "PAGE FAULT TRAP & SWAP WORKFLOW:\n" +
            "  1. CPU Memory Reference ──────────> Page Table Entry (Valid Bit = 0)\n" +
            "                                             │\n" +
            "                                             ▼\n" +
            "  2. Hardware Trap to OS Kernel <──── Page Fault Interrupt\n" +
            "        │\n" +
            "        ├─> Find free physical frame (or evict victim frame via LRU)\n" +
            "        ├─> Issue Disk I/O Read to swap partition (Process Sleeps)\n" +
            "        └─> Update Page Table (Valid Bit = 1) ──> Restart Instruction!\n" +
            "\n" +
            "CORE EVICTION POLICIES:\n" +
            "  • FIFO : Evicts oldest loaded page. Vulnerable to Belady's Anomaly.\n" +
            "  • LRU  : Evicts page unreferenced longest in past. (Stack Algorithm).\n" +
            "  • OPT  : Evicts page unreferenced for longest future time. (Benchmark).",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. PERFORMANCE METRICS & ACCESS TIME FORMULAS                          ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌─────────────────────────┬──────────────────────────────────────────────┐\n" +
            "│ METRIC NAME             │ FORMULA & COMPUTATION                        │\n" +
            "├─────────────────────────┼──────────────────────────────────────────────┤\n" +
            "│ Total References (N)    │ N = Page_Hits (H) + Page_Faults (F)          │\n" +
            "│ Hit Ratio (HR)          │ HR = (H / N) * 100%                          │\n" +
            "│ Fault Ratio (FR)        │ FR = (F / N) * 100% = 100% - HR              │\n" +
            "│ Effective Access Time   │ EAT = (1 - p)*t_ram + p*(Fault_Service_Time) │\n" +
            "│ Fault Service Overhead  │ Disk Seek + Latency + Swap Read/Write        │\n" +
            "└─────────────────────────┴──────────────────────────────────────────────┘\n" +
            "\n" +
            "CALCULATION RUNWAY (PAGE FAULT IMPACT ON SPEED):\n" +
            "  • Given : RAM Access t_ram = 100 ns, Fault Service = 10 ms (10M ns)\n" +
            "  • With Fault Rate p = 0.001 (0.1% faults, 99.9% hits):\n" +
            "  • EAT   = (0.999 * 100 ns) + (0.001 * 10,000,000 ns)\n" +
            "  • EAT   = 99.9 ns + 10,000 ns = 10,099.9 ns (Over 100x slower!)",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. LRU & CLOCK PAGE REPLACEMENT PSEUDOCODE                             ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌────────────────────────────────────────────────────────────────────────┐\n" +
            "│ ENGINE 01: LRU CACHE ENGINE (LEAST RECENTLY USED EVICTION)             │\n" +
            "├────────────────────────────────────────────────────────────────────────┤\n" +
            "│ function access_page(int page, List<Integer> frames, int capacity) {   │\n" +
            "│     if (frames.contains(page)) {                                       │\n" +
            "│         record_hit();                                                  │\n" +
            "│         frames.remove((Integer) page); // Move to most recent          │\n" +
            "│         frames.add(page);                                              │\n" +
            "│     } else {                                                           │\n" +
            "│         record_fault();                                                │\n" +
            "│         if (frames.size() == capacity) {                               │\n" +
            "│             int victim = frames.remove(0); // Evict LRU at head        │\n" +
            "│         }                                                              │\n" +
            "│         frames.add(page);                                              │\n" +
            "│     }                                                                  │\n" +
            "│ }                                                                      │\n" +
            "└────────────────────────────────────────────────────────────────────────┘\n" +
            "\n" +
            "COMPLEXITY: LRU: O(1) with LinkedHashSet | Clock: O(1) amortized",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED EXAMPLE (LRU, 3 FRAMES)                         ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "REFERENCE STRING: 7, 0, 1, 2, 0, 3, 0, 4 (Physical Capacity = 3 Frames)\n" +
            "\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            "┌─────────┬─────────────┬─────────────┬─────────────┬────────────────────┐\n" +
            "│ REF PAG │ FRAME 1 (F1)│ FRAME 2 (F2)│ FRAME 3 (F3)│ CACHE STATUS / EVEN│\n" +
            "├─────────┼─────────────┼─────────────┼─────────────┼────────────────────┤\n" +
            "│    7    │      7      │      -      │      -      │ PAGE FAULT (Cold)  │\n" +
            "│    0    │      7      │      0      │      -      │ PAGE FAULT (Cold)  │\n" +
            "│    1    │      7      │      0      │      1      │ PAGE FAULT (Cold)  │\n" +
            "│    2    │      2      │      0      │      1      │ FAULT (7 evicted)  │\n" +
            "│    0    │      2      │      0      │      1      │ ★ CACHE HIT!       │\n" +
            "│    3    │      2      │      0      │      3      │ FAULT (1 evicted)  │\n" +
            "│    0    │      2      │      0      │      3      │ ★ CACHE HIT!       │\n" +
            "│    4    │      4      │      0      │      3      │ FAULT (2 evicted)  │\n" +
            "└─────────┴─────────────┴─────────────┴─────────────┴────────────────────┘\n" +
            "\n" +
            "METRICS SUMMARY: Total Refs = 8 | Hits = 2 (25.0%) | Faults = 6 (75.0%)",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET & REPLACEMENT TRAPS                                ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌──────────┬─────────────────────────────┬───────────────────────────────┐\n" +
            "│ POLICY   │ EVICTION CRITERIA           │ KEY CHARACTERISTICS / TRAP    │\n" +
            "├──────────┼─────────────────────────────┼───────────────────────────────┤\n" +
            "│ FIFO     │ Oldest arrived frame in RAM │ Belady's Anomaly risk         │\n" +
            "│ LRU      │ Unreferenced longest past   │ Stack algorithm; Optimal HW   │\n" +
            "│ OPT/MIN  │ Unreferenced longest future │ Theoretical benchmark         │\n" +
            "│ CLOCK    │ Circular queue + Ref bit    │ Second-chance LRU approx      │\n" +
            "└──────────┴─────────────────────────────┴───────────────────────────────┘\n" +
            "\n" +
            "★ BELADY'S ANOMALY:\n" +
            "  For FIFO, adding more physical frames can counter-intuitively INCREASE\n" +
            "  the number of page faults (e.g. stream 1,2,3,4,1,2,5,1,2,3,4,5).\n" +
            "  Stack Algorithms (LRU, Optimal) are mathematically immune to Belady's.",
            3, 3
        );

        // ─────────────────────────────────────────────────────────────────────
        // 05. SEGMENTATION & MEMORY PROTECTION
        // ─────────────────────────────────────────────────────────────────────
        MANUALS[4] = new ManualEntry(
            "05",
            "SEGMENTATION & MEMORY PROTECTION",
            "MISSION 05: VARIABLE BOUNDS & LIMIT TRAP DETECTION",
            "★★☆☆☆ (FUNDAMENTAL)",
            "BASE | LIMIT | BOUNDS CHECK | SEGMENTATION FAULT | TRAP",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 1. EXECUTIVE OVERVIEW & LOGICAL ADDRESS SPACE                          ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "Segmentation models memory from the programmer's view: variable-length\n" +
            "logical units such as Code, Stack, Heap, and Symbol Tables.\n" +
            "\n" +
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
            "                  Physical RAM Base ───────────(+)─────────> [ Base + d ]",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. BOUNDS CHECKING & ADDRESS TRANSLATION EQUATIONS                     ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌─────────────────────────┬──────────────────────────────────────────────┐\n" +
            "│ HARDWARE CHECK / PARAM  │ MATHEMATICAL VERIFICATION & LOGIC            │\n" +
            "├─────────────────────────┼──────────────────────────────────────────────┤\n" +
            "│ Segment Length Register │ Check: s < STLR (Segment Table Length Reg)   │\n" +
            "│ Segment Offset Limit    │ Check: 0 <= d < SegmentTable[s].Limit        │\n" +
            "│ Physical Address (PA)   │ PA = SegmentTable[s].Base + d                │\n" +
            "│ Out-of-Bounds Trap      │ IF d >= Limit THEN TRAP(SEGMENTATION_FAULT)  │\n" +
            "│ Protection Violation    │ IF operation not permitted THEN TRAP(PROT)   │\n" +
            "└─────────────────────────┴──────────────────────────────────────────────┘\n" +
            "\n" +
            "CALCULATION RUNWAY (SAMPLE BOUNDS CHECK):\n" +
            "  • Given : Segment 0 Base = 1400, Limit = 1000 bytes\n" +
            "  • Req <Seg 0, 420> : 420 < 1000? YES -> PA = 1400 + 420 = 1820 (OK)\n" +
            "  • Req <Seg 0, 1000>: 1000 < 1000? NO -> TRAP: SEGMENTATION FAULT!",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. TRANSLATION PSEUDOCODE                                              ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌────────────────────────────────────────────────────────────────────────┐\n" +
            "│ SEGMENTATION BOUNDS VALIDATOR & TRANSLATION ENGINE                     │\n" +
            "├────────────────────────────────────────────────────────────────────────┤\n" +
            "│ function translate_seg(int s, int d, SegmentEntry[] segTable) {        │\n" +
            "│     if (s < 0 || s >= segTable.length) {                               │\n" +
            "│         raise_trap('ILLEGAL SEGMENT SELECTOR');                        │\n" +
            "│     }                                                                  │\n" +
            "│     SegmentEntry e = segTable[s];                                      │\n" +
            "│     if (d < 0 || d >= e.limit) {                                       │\n" +
            "│         raise_trap('SEGMENTATION FAULT: Offset exceeds segment limit') │\n" +
            "│     }                                                                  │\n" +
            "│     return e.base + d; // Physical RAM Address                         │\n" +
            "│ }                                                                      │\n" +
            "└────────────────────────────────────────────────────────────────────────┘\n" +
            "\n" +
            "COMPLEXITY: Translation Time: O(1) | Space: O(Number of Segments)",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED EXAMPLE & SEGMENT TABLE MAP                     ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            "┌─────┬──────────────┬──────────┬──────────┬────────┬────────────────────┐\n" +
            "│ SEG │ SEGMENT NAME │   BASE   │  LIMIT   │ OFFSET │ RESULT / PHYSICAL  │\n" +
            "├─────┼──────────────┼──────────┼──────────┼────────┼────────────────────┤\n" +
            "│  0  │ Main Code    │   1400   │   1000   │  420   │ Valid -> PA = 1820 │\n" +
            "│  1  │ Call Stack   │   6300   │   0400   │  450   │ TRAP: SEG FAULT!   │\n" +
            "│  2  │ Symbol Table │   4300   │   0500   │  080   │ Valid -> PA = 4380 │\n" +
            "│  3  │ Subroutines  │   3200   │   1500   │ 1500   │ TRAP: d == Limit!  │\n" +
            "└─────┴──────────────┴──────────┴──────────┴────────┴────────────────────┘\n" +
            "\n" +
            "CASE EVALUATIONS:\n" +
            "  • Case 1: <Seg 0, 420>  : 420 < 1000 -> Valid -> PA = 1400 + 420 = 1820\n" +
            "  • Case 2: <Seg 1, 450>  : 450 exceeds Limit 400! -> SEGMENTATION FAULT\n" +
            "  • Case 3: <Seg 2, 80>   : 80 < 500   -> Valid -> PA = 4300 + 80 = 4380\n" +
            "  • Case 4: <Seg 3, 1500> : Offset == Limit is TRAP! (d must be < Limit).",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET: PAGING VS SEGMENTATION                            ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌────────────────────┬──────────────────────┬────────────────────────────┐\n" +
            "│ ARCHITECTURAL FACT │ PAGING               │ SEGMENTATION               │\n" +
            "├────────────────────┼──────────────────────┼────────────────────────────┤\n" +
            "│ Block Dimension    │ Fixed size (4 KB)    │ Variable length bytes      │\n" +
            "│ User Visibility    │ Invisible (Hardware) │ Visible (Code/Stack/Heap)  │\n" +
            "│ Fragmentation      │ Internal frag only   │ External frag only         │\n" +
            "│ Table Contents     │ Frame number (f)     │ Base address + Limit size  │\n" +
            "│ Sharing/Protection │ Page access flags    │ Natural logical units      │\n" +
            "└────────────────────┴──────────────────────┴────────────────────────────┘\n" +
            "\n" +
            "★ COMMON EXAM TRAP:\n" +
            "  In Paging, offset 'd' can be up to Page_Size - 1.\n" +
            "  In Segmentation, offset 'd' must be LESS THAN Limit (d < Limit)!\n" +
            "  If d == Limit, it immediately triggers a SEGMENTATION FAULT!",
            4, 2
        );

        // ─────────────────────────────────────────────────────────────────────
        // 06. CONTIGUOUS ALLOCATION & FRAGMENTATION
        // ─────────────────────────────────────────────────────────────────────
        MANUALS[5] = new ManualEntry(
            "06",
            "CONTIGUOUS ALLOCATION & FRAGMENTATION",
            "MISSION 06: PARTITION STRATEGIES & COMPACTION",
            "★★★☆☆ (MODERATE)",
            "FIRST FIT | BEST FIT | WORST FIT | INTERNAL FRAG | EXTERNAL FRAG",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 1. EXECUTIVE OVERVIEW: MEMORY PARTITIONING STRATEGIES                  ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "Contiguous allocation assigns each process a single contiguous section\n" +
            "of memory. This produces two distinct forms of fragmentation: Internal\n" +
            "(unused partition bytes) and External (scattered free holes).\n" +
            "\n" +
            "CONTIGUOUS MEMORY HOLE & PARTITION SCHEMATIC:\n" +
            "0K              100K        300K        500K        800K           1024K\n" +
            "┌───────────────┬───────────┬───────────┬───────────┬──────────────┐\n" +
            "│  OS RESERVED  │ PROCESS 1 │ FREE HOLE │ PROCESS 2 │ FREE HOLE    │\n" +
            "│   (100 KB)    │ (200 KB)  │ (200 KB)  │ (300 KB)  │ (224 KB)     │\n" +
            "└───────────────┴───────────┴───────────┴───────────┴──────────────┘\n" +
            "                                  ▲                       ▲\n" +
            "                                  └────── Free Holes ─────┘",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. FRAGMENTATION METRICS & HOLE EQUATIONS                              ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌─────────────────────────┬──────────────────────────────────────────────┐\n" +
            "│ METRIC NAME             │ FORMULA & COMPUTATION                        │\n" +
            "├─────────────────────────┼──────────────────────────────────────────────┤\n" +
            "│ Internal Fragmentation  │ Internal_Frag = Block_Size - Job_Size        │\n" +
            "│ External Fragmentation  │ Σ Free_Holes (when Σ Holes >= Job, max < Job)│\n" +
            "│ Knuth's 50% Rule        │ Number_of_Holes ≈ 0.5 * Allocated_Blocks     │\n" +
            "│ Memory Utilization      │ Util% = (Total_Allocated / Total_RAM) * 100% │\n" +
            "│ Compaction Free Yield   │ Continuous_Free = Σ All_Hole_Sizes           │\n" +
            "└─────────────────────────┴──────────────────────────────────────────────┘\n" +
            "\n" +
            "PLACEMENT POLICIES COMPARISON:\n" +
            "  • First Fit : Searches from start; takes first hole with size >= req.\n" +
            "  • Best Fit  : Searches list; takes smallest hole with size >= req.\n" +
            "  • Worst Fit : Searches entire list, allocates in largest available hole.",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. BEST-FIT & FIRST-FIT ALLOCATION PSEUDOCODE                          ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌────────────────────────────────────────────────────────────────────────┐\n" +
            "│ ENGINE 01: FIRST-FIT PARTITION ALLOCATOR                               │\n" +
            "├────────────────────────────────────────────────────────────────────────┤\n" +
            "│ function allocate_first_fit(int reqSize, List<Hole> holes) {           │\n" +
            "│     for (Hole h : holes) {                                             │\n" +
            "│         if (h.size >= reqSize) {                                       │\n" +
            "│             return split_and_allocate(h, reqSize);                     │\n" +
            "│         }                                                              │\n" +
            "│     }                                                                  │\n" +
            "│     return null; // No adequate hole found                             │\n" +
            "│ }                                                                      │\n" +
            "└────────────────────────────────────────────────────────────────────────┘\n" +
            "\n" +
            "COMPLEXITY: First Fit: O(N) average | Best Fit: O(N) exhaustive search",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED EXAMPLE & PLACEMENT TRACE                       ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "HOLES: [H1: 100 KB]  [H2: 500 KB]  [H3: 200 KB]  [H4: 300 KB]\n" +
            "Incoming Process Request: 212 KB\n" +
            "\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            "┌─────────┬─────────────┬─────────────┬─────────────┬────────────────────┐\n" +
            "│ POLICY  │ SEARCH ORDER│ HOLE CHOSEN │LEFTOVER HOLE│FRAGMENTATION EFFECT│\n" +
            "├─────────┼─────────────┼─────────────┼─────────────┼────────────────────┤\n" +
            "│First Fit│ H1->H2(500K)│ H2 (500 KB) │ 288 KB Free │ Large usable hole  │\n" +
            "│Best Fit │ Full Search │ H4 (300 KB) │  88 KB Free │ Tiny sliver created│\n" +
            "│Worst Fit│ Full Search │ H2 (500 KB) │ 288 KB Free │ Largest leftover   │\n" +
            "└─────────┴─────────────┴─────────────┴─────────────┴────────────────────┘\n" +
            "\n" +
            "OBSERVATION:\n" +
            "  • First Fit is fastest and leaves a large 288 KB usable hole.\n" +
            "  • Best Fit leaves only 88 KB, which is too small for incoming jobs!",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET: STRATEGIES & COMPACTION                           ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌──────────┬─────────────────────────────┬───────────────────────────────┐\n" +
            "│ POLICY   │ ALLOCATION HEURISTIC        │ PROS, CONS & PERFORMANCE      │\n" +
            "├──────────┼─────────────────────────────┼───────────────────────────────┤\n" +
            "│First Fit │ First hole with size >= req │ Fastest; low search overhead  │\n" +
            "│Best Fit  │ Smallest hole size >= req   │ Leaves unusable tiny slivers  │\n" +
            "│Worst Fit │ Largest available hole      │ Leaves largest leftover hole  │\n" +
            "│Next Fit  │ First Fit resuming from last│ Spreads allocations evenly    │\n" +
            "└──────────┴─────────────────────────────┴───────────────────────────────┘\n" +
            "\n" +
            "★ BEST FIT IS NOT ALWAYS 'BEST':\n" +
            "  Counter-intuitively, Best Fit produces tiny leftover hole slivers that\n" +
            "  are too small for any other job, creating severe external frag.\n" +
            "\n" +
            "★ KNUTH'S 50% RULE:\n" +
            "  For First Fit, for every N allocated blocks, about 0.5 N blocks are lost\n" +
            "  to external fragmentation (one-third of memory may be unusable!).",
            5, 1
        );

        // ─────────────────────────────────────────────────────────────────────
        // 07. DISK I/O HEAD SCHEDULING
        // ─────────────────────────────────────────────────────────────────────
        MANUALS[6] = new ManualEntry(
            "07",
            "DISK I/O HEAD SCHEDULING",
            "MISSION 07: CYLINDER TRAVERSAL & SEEK-TIME MINIMIZATION",
            "★★★☆☆ (MODERATE)",
            "FCFS | SSTF | SCAN | LOOK | C-SCAN | TOTAL HEAD MOVEMENT",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 1. EXECUTIVE OVERVIEW & PLATTER MECHANICS                              ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "Disk Seek Time (moving mechanical arm to target cylinder) dominates I/O\n" +
            "latency. Disk scheduling reorders pending I/O requests to minimize Total\n" +
            "Head Movement (THM).\n" +
            "\n" +
            "ROTATING PLATTER & READ/WRITE HEAD ACTUATOR SCHEMATIC:\n" +
            "     ┌───────────────────┐\n" +
            "     │ ( ( ( ʘ ) ) )     │  Spindle rotates at 7200 RPM / 10000 RPM\n" +
            "     └─────────┬─────────┘\n" +
            "               │  Actuator Arm Moves Bi-Directionally (Seek Time)\n" +
            "     ┌─────────▼─────────┐\n" +
            "     │ [ R/W HEAD ] <====│========[ ACTUATOR MOTOR ]\n" +
            "     └───────────────────┘\n" +
            "      Track 000 ───────────── Track 199 (Target Cylinders)",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. DISK SEEK & LATENCY METRIC FORMULAS                                 ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌─────────────────────────┬──────────────────────────────────────────────┐\n" +
            "│ METRIC NAME             │ FORMULA & HARDWARE COMPUTATION               │\n" +
            "├─────────────────────────┼──────────────────────────────────────────────┤\n" +
            "│ Step Seek Distance      │ Seek_Dist = |Target_Cylinder - Current_Cyl|  │\n" +
            "│ Total Head Movement     │ THM = Σ |C[i] - C[i-1]|                      │\n" +
            "│ Average Seek Distance   │ Avg_Seek = THM / Total_Requests              │\n" +
            "│ Avg Rotational Latency  │ Latency = 0.5 * (60 / RPM) seconds           │\n" +
            "│ Total Disk Access Time  │ Access_Time = Seek_Time + Latency + Transfer │\n" +
            "└─────────────────────────┴──────────────────────────────────────────────┘\n" +
            "\n" +
            "CALCULATION RUNWAY (7200 RPM HARD DRIVE COMPUTATION):\n" +
            "  • Rotational Period = 60 / 7200 = 0.00833 s = 8.33 ms\n" +
            "  • Average Rotational Latency = 8.33 ms / 2 = 4.17 ms\n" +
            "  (Mechanical seek time typically dominates at 4 ms to 9 ms).",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. SSTF & SCAN DISK SCHEDULING PSEUDOCODE                              ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌────────────────────────────────────────────────────────────────────────┐\n" +
            "│ ENGINE 01: SSTF (SHORTEST SEEK TIME FIRST)                             │\n" +
            "├────────────────────────────────────────────────────────────────────────┤\n" +
            "│ function schedule_sstf(List<Integer> queue, int head) {                │\n" +
            "│     int thm = 0;                                                       │\n" +
            "│     while (!queue.isEmpty()) {                                         │\n" +
            "│         int nextCyl = find_closest(queue, head);                       │\n" +
            "│         thm += abs(nextCyl - head);                                    │\n" +
            "│         head = nextCyl;                                                │\n" +
            "│         queue.remove((Integer) nextCyl);                               │\n" +
            "│     }                                                                  │\n" +
            "│     return thm;                                                        │\n" +
            "│ }                                                                      │\n" +
            "└────────────────────────────────────────────────────────────────────────┘\n" +
            "\n" +
            "COMPLEXITY: SSTF: O(N^2) | SCAN / LOOK: O(N log N) sorting phase",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED EXAMPLE & HEAD TRAVERSAL                        ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "QUEUE: 98, 183, 37, 122, 14, 124, 65, 67 | Initial Head Position = 53\n" +
            "\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            "┌─────┬──────────┬──────────────┬──────────────┬─────────────────────────┐\n" +
            "│STEP │ FROM CYL │ TO CYLINDER  │ SEEK (CYLS)  │ ACCUMULATED THM         │\n" +
            "├─────┼──────────┼──────────────┼──────────────┼─────────────────────────┤\n" +
            "│  1  │ 53 (Init)│ 65           │ |65 - 53|= 12│ Total =  12 cylinders   │\n" +
            "│  2  │ 65       │ 67           │ |67 - 65|=  2│ Total =  14 cylinders   │\n" +
            "│  3  │ 67       │ 37           │ |37 - 67|= 30│ Total =  44 cylinders   │\n" +
            "│  4  │ 37       │ 14           │ |14 - 37|= 23│ Total =  67 cylinders   │\n" +
            "│  5  │ 14       │ 98           │ |98 - 14|= 84│ Total = 151 cylinders   │\n" +
            "│  6  │ 98       │ 122          │ |122- 98|= 24│ Total = 175 cylinders   │\n" +
            "│  7  │ 122      │ 124          │ |124-122|=  2│ Total = 177 cylinders   │\n" +
            "│  8  │ 124      │ 183          │ |183-124|= 59│ Total = 236 cylinders   │\n" +
            "└─────┴──────────┴──────────────┴──────────────┴─────────────────────────┘\n" +
            "\n" +
            "TOTAL HEAD MOVEMENT (SSTF) = 236 cyls (vs FCFS = 640 cyls: 63% savings!).",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET: DISK ALGORITHMS COMPARISON                        ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌──────────┬─────────────────────────────┬───────────────────────────────┐\n" +
            "│ ALGORITHM│ PRIMARY ADVANTAGE           │ KEY TRAP / WEAKNESS           │\n" +
            "├──────────┼─────────────────────────────┼───────────────────────────────┤\n" +
            "│ FCFS     │ Fair, simple, no starvation │ Wild head swings, high THM    │\n" +
            "│ SSTF     │ Low seek time locally       │ Starvation for outer tracks   │\n" +
            "│ SCAN     │ Elevator sweep, no starv    │ Bounces at disk edge (0/max)  │\n" +
            "│ C-SCAN   │ Uniform wait across tracks  │ Resets head without service   │\n" +
            "│ LOOK     │ Reverses at last request    │ Does not travel to 0/max      │\n" +
            "│ C-LOOK   │ Uniform wait, optimal sweep │ Circular reset jump           │\n" +
            "└──────────┴─────────────────────────────┴───────────────────────────────┘\n" +
            "\n" +
            "★ EXAM TIP (SCAN VS LOOK):\n" +
            "  In SCAN, the head travels all the way to cylinder 0 (or max cylinder),\n" +
            "  even if there are no requests there! LOOK reverses direction immediately\n" +
            "  after serving the last request in that direction, saving head movement.",
            6, 0
        );

        // ─────────────────────────────────────────────────────────────────────
        // 08. MULTIPROGRAMMING & CONCURRENCY
        // ─────────────────────────────────────────────────────────────────────
        MANUALS[7] = new ManualEntry(
            "08",
            "MULTIPROGRAMMING & CONCURRENCY",
            "MISSION 08: DEGREE OF CONCURRENCY & CPU UTILIZATION",
            "★★★★☆ (ADVANCED)",
            "CPU UTILIZATION | DEGREE OF MULTIPROGRAMMING | THRASHING | I/O WAIT",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 1. EXECUTIVE OVERVIEW: THE NEED FOR CONCURRENCY                        ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "Processes spend much of their lifetime waiting for I/O. Multiprogramming\n" +
            "maintains multiple processes in memory so the CPU switches to another\n" +
            "process when one is blocked.\n" +
            "\n" +
            "UNIPROGRAMMING VS MULTIPROGRAMMING OVERLAP TIMELINE:\n" +
            "  UNIPROGRAMMING:\n" +
            "  [ CPU P1 ] [   I/O P1 (CPU IDLE!)   ] [ CPU P1 ] [ I/O P1 ]\n" +
            "\n" +
            "  MULTIPROGRAMMING (DEGREE N = 2):\n" +
            "  [ CPU P1 ] [ CPU P2 (WHILE P1 I/O)  ] [ CPU P1 ] [ CPU P2 ]\n" +
            "             [   I/O WAIT FOR P1      ]            [ I/O P2 ]",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. PROBABILISTIC CONCURRENCY & CPU UTILIZATION FORMULAS                ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "Let p = fraction of time a process spends waiting for I/O (0 <= p <= 1),\n" +
            "and N = Degree of Multiprogramming (processes in RAM):\n" +
            "\n" +
            "┌─────────────────────────┬──────────────────────────────────────────────┐\n" +
            "│ METRIC NAME             │ MATHEMATICAL FORMULA & CONCURRENCY LOGIC     │\n" +
            "├─────────────────────────┼──────────────────────────────────────────────┤\n" +
            "│ Probability All Idle    │ P(All_Idle) = p^N (where p = I/O wait frac)  │\n" +
            "│ Probabilistic CPU Util  │ CPU_Util = 1 - p^N                           │\n" +
            "│ Throughput Rate         │ Throughput = Completed_Processes / Total_Time│\n" +
            "│ Turnaround Time         │ TAT = Process_Finish_Time - Arrival_Time     │\n" +
            "│ Amdahl's Law Speedup    │ Speedup = 1 / ((1 - P) + P / N)              │\n" +
            "└─────────────────────────┴──────────────────────────────────────────────┘\n" +
            "\n" +
            "OBSERVATION:\n" +
            "  When p = 0.80, N=1 gives CPU Util = 20%. N=8 boosts CPU Util to 83.22%!\n" +
            "  However, if N exceeds physical RAM capacity, Thrashing occurs.",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. CONCURRENCY ADMISSION & WORKING SET PSEUDOCODE                      ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌────────────────────────────────────────────────────────────────────────┐\n" +
            "│ CONCURRENCY ADMISSION CONTROLLER & WORKING SET MONITOR                 │\n" +
            "├────────────────────────────────────────────────────────────────────────┤\n" +
            "│ function admit_process(Job job, int current_ram, int total_ram) {      │\n" +
            "│     if (current_ram + job.ram <= total_ram) {                          │\n" +
            "│         degree_N++;                                                    │\n" +
            "│         cpu_util = 1.0 - Math.pow(p_io_wait, degree_N);                │\n" +
            "│         return true; // Admitted to RAM                                │\n" +
            "│     } else {                                                           │\n" +
            "│         spool_to_disk(job); // Throttle admission to prevent Thrashing │\n" +
            "│         return false;                                                  │\n" +
            "│     }                                                                  │\n" +
            "│ }                                                                      │\n" +
            "└────────────────────────────────────────────────────────────────────────┘\n" +
            "\n" +
            "COMPLEXITY: Admission Check: O(1) | Working Set Evaluation: O(N * Δ)",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED EXAMPLE & UTILIZATION SCALING                   ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "WORKLOAD PARAMETERS: Process I/O wait fraction p = 0.80 (80% time\n" +
            "waiting for I/O)\n" +
            "\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            "┌─────────┬─────────────┬─────────────┬─────────────┬────────────────────┐\n" +
            "│ DEGREE N│ I/O WAIT (p)│ PROB IDLE p^│ CPU UTIL (%)│ SYSTEM EVALUATION  │\n" +
            "├─────────┼─────────────┼─────────────┼─────────────┼────────────────────┤\n" +
            "│    1    │ 0.80 (80%)  │ 0.8000 (80%)│ 20.00% Util │ Terrible uniprogram│\n" +
            "│    2    │ 0.80 (80%)  │ 0.6400 (64%)│ 36.00% Util │ 80% gain over N=1  │\n" +
            "│    4    │ 0.80 (80%)  │ 0.4096 (41%)│ 59.04% Util │ Solid concurrency  │\n" +
            "│    8    │ 0.80 (80%)  │ 0.1678 (17%)│ 83.22% Util │ Optimal throughput │\n" +
            "│   12    │ 0.80 (80%)  │ 0.0687 (07%)│ 93.13% Util │ Maximum sweet spot │\n" +
            "└─────────┴─────────────┴─────────────┴─────────────┴────────────────────┘\n" +
            "\n" +
            "RESULT: Concurrency scales CPU utilization from 20% to over 93% without\n" +
            "extra CPUs!",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET: THRASHING & SCHEDULER HIERARCHY                   ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌────────────────────┬──────────────────────┬────────────────────────────┐\n" +
            "│ SCHEDULER TIER     │ PRIMARY RESPONSIBILIT│ EXECUTION FREQUENCY        │\n" +
            "├────────────────────┼──────────────────────┼────────────────────────────┤\n" +
            "│ Long-Term (Job)    │ Controls Degree of MP│ Infrequent (secs/mins)     │\n" +
            "│ Medium-Term (Swap) │ Swaps in/out of RAM  │ Intermediate (alleviate)   │\n" +
            "│ Short-Term (CPU)   │ Allocates CPU core   │ Milliseconds (frequent)    │\n" +
            "└────────────────────┴──────────────────────┴────────────────────────────┘\n" +
            "\n" +
            "★ THE THRASHING PHENOMENON:\n" +
            "  When Degree of Multiprogramming (DOM) increases beyond available RAM:\n" +
            "  1. Processes page fault frequently as frames become scarce.\n" +
            "  2. CPU scheduler notices CPU is idle (waiting for disk page swaps).\n" +
            "  3. OS naively admits MORE processes to raise CPU load, worsening thrash!\n" +
            "  4. CPU Utilization plummets to near 0% as system spends all time paging.\n" +
            "\n" +
            "★ WORKING SET STRATEGY (Denning):\n" +
            "  The Working Set WSS(i) is pages process Pi accessed in time window Δ.\n" +
            "  • If Σ WSS(i) <= Total_Frames -> System is stable and productive.\n" +
            "  • If Σ WSS(i) > Total_Frames  -> Thrashing begins! OS suspends a job.",
            7, 0
        );

    }

    public static ManualEntry getManual(int index) {
        if (index >= 0 && index < MANUALS.length) {
            return MANUALS[index];
        }
        return MANUALS[0];
    }
}
