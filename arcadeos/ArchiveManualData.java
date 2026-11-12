package arcadeos;

/**
 * Enhanced Knowledge Base and Core Algorithm Logic for the 8 Operating System Manuals.
 * Features comprehensive theory, ASCII architecture diagrams, mathematical equations,
 * pseudocode, step-by-step trace tables, and exam cheat-sheets.
 */
public class ArchiveManualData {

    public static class ManualEntry {
        public String id;
        public String title;
        public String subtitle;
        public String complexity;
        public String tags;
        public String theory;
        public String formulas;
        public String pseudocode;
        public String workedExample;
        public String cheatSheet;
        public int simulatorTabIndex;
        public int quizModuleIndex;

        public ManualEntry(String id, String title, String subtitle, String complexity, String tags,
                           String theory, String formulas, String pseudocode, String workedExample,
                           String cheatSheet, int simTab, int quizMod) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.complexity = complexity;
            this.tags = tags;
            this.theory = theory;
            this.formulas = formulas;
            this.pseudocode = pseudocode;
            this.workedExample = workedExample;
            this.cheatSheet = cheatSheet;
            this.simulatorTabIndex = simTab;
            this.quizModuleIndex = quizMod;
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
            "The CPU Scheduler is the core kernel component responsible for allocating the CPU\n" +
            "execution core to one of the runnable processes currently residing in the READY QUEUE.\n\n" +
            "PROCESS STATE TRANSITION DIAGRAM:\n" +
            "┌──────────┐   admit   ┌───────────┐  dispatch ┌───────────┐  exit   ┌────────────┐\n" +
            "│   NEW    │ ────────> │   READY   │ ────────> │  RUNNING  │ ──────> │ TERMINATED │\n" +
            "└──────────┘           └───────────┘           └───────────┘         └────────────┘\n" +
            "                             ▲                       │\n" +
            "                             │       I/O Complete    ▼   I/O or Event Wait\n" +
            "                             └───────────────── ┌───────────┐\n" +
            "                                                │  WAITING  │\n" +
            "                                                └───────────┘\n\n" +
            "2. FOUR SCHEDULING CRITERIA:\n" +
            "  • FCFS (First-Come, First-Served) : Non-preemptive. Strict FIFO queue ordered by Arrival Time.\n" +
            "  • SJF (Shortest Job First)        : Non-preemptive. Selects process with smallest Burst Time.\n" +
            "  • SRTF (Shortest Remaining Time)  : Preemptive SJF. Preempts current job if new job has smaller remaining burst.\n" +
            "  • Round Robin (RR)                : Preemptive with Time Quantum (TQ). Enforces fair timesharing.",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. MATHEMATICAL FORMULAS & PERFORMANCE METRICS                         ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "┌─────────────────────────┬──────────────────────────────────────────────┐\n" +
            "│ METRIC NAME             │ FORMULA & COMPUTATION                        │\n" +
            "├─────────────────────────┼──────────────────────────────────────────────┤\n" +
            "│ Start Time (ST)         │ ST[i] = max(Arrival_Time[i], Finish_Time[i-1])│\n" +
            "│ Finish Time (FT)        │ FT[i] = ST[i] + Burst_Time[i]                │\n" +
            "│ Turnaround Time (TAT)   │ TAT[i] = FT[i] - Arrival_Time[i]             │\n" +
            "│ Waiting Time (WT)       │ WT[i] = TAT[i] - Burst_Time[i] = ST[i] - AT[i│\n" +
            "│ Average Waiting Time    │ AWT = (Σ WT[i]) / N                          │\n" +
            "│ Average Turnaround Time │ ATAT = (Σ TAT[i]) / N                        │\n" +
            "│ CPU Utilization         │ (Busy Time / Total Schedule Duration) * 100% │\n" +
            "│ Throughput              │ Completed Jobs / Unit Time                   │\n" +
            "└─────────────────────────┴──────────────────────────────────────────────┘",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. ALGORITHM PSEUDOCODE & COMPLEXITY ANALYSIS                          ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "// FCFS DISPATCHER ENGINE\n" +
            "function schedule_fcfs(Job[] jobs) {\n" +
            "    sort_by_arrival_time(jobs); // O(N log N)\n" +
            "    time = 0.0;\n" +
            "    for (Job j : jobs) {\n" +
            "        if (time < j.arrival) {\n" +
            "            record_idle(time, j.arrival); // CPU IDLE GAP\n" +
            "            time = j.arrival;\n" +
            "        }\n" +
            "        j.startTime = time;\n" +
            "        j.finishTime = time + j.burst;\n" +
            "        j.turnaround = j.finishTime - j.arrival;\n" +
            "        j.waiting = j.turnaround - j.burst;\n" +
            "        time = j.finishTime;\n" +
            "    }\n" +
            "}\n\n" +
            "COMPLEXITY: Time: O(N log N) | Space: O(N) for Gantt Timeline.",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED TRACE & GANTT RUNWAY                            ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "SAMPLE WORKLOAD: 3 PROCESSES\n" +
            "  • J1: Arrival = 0.0, Burst = 4.0\n" +
            "  • J2: Arrival = 1.0, Burst = 3.0\n" +
            "  • J3: Arrival = 2.0, Burst = 2.0\n\n" +
            "COMPUTATIONAL TRACE TABLE:\n" +
            "┌─────┬──────┬──────┬──────┬──────┬──────┬──────┬────────────────────────────┐\n" +
            "│ JOB │  AT  │  BT  │  ST  │  FT  │  WT  │  TAT │ STATUS / NOTES             │\n" +
            "├─────┼──────┼──────┼──────┼──────┼──────┼──────┼────────────────────────────┤\n" +
            "│ J1  │ 0.0  │ 4.0  │ 0.0  │ 4.0  │ 0.0  │ 4.0  │ Arrives at t=0, starts immed│\n" +
            "│ J2  │ 1.0  │ 3.0  │ 4.0  │ 7.0  │ 3.0  │ 6.0  │ Waits from 1.0 to 4.0      │\n" +
            "│ J3  │ 2.0  │ 2.0  │ 7.0  │ 9.0  │ 5.0  │ 7.0  │ Waits from 2.0 to 7.0      │\n" +
            "└─────┴──────┴──────┴──────┴──────┴──────┴──────┴────────────────────────────┘\n\n" +
            "ASCII GANTT TIMELINE:\n" +
            "0.0                 4.0                 7.0           9.0\n" +
            " ┌───────────────────┬───────────────────┬─────────────┐\n" +
            " │    JOB 1 (J1)     │    JOB 2 (J2)     │  JOB 3 (J3) │\n" +
            " └───────────────────┴───────────────────┴─────────────┘\n" +
            "METRICS: Total Time = 9.0 | Avg WT = 2.67 | Avg TAT = 5.67 | CPU Util = 100%",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET & COMMON PITFALLS                                  ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "★ THE CONVOY EFFECT:\n" +
            "  In FCFS, when a long CPU-bound process runs first, multiple short I/O-bound\n" +
            "  processes queue behind it, resulting in poor CPU and device utilization.\n\n" +
            "★ SJF IS OPTIMAL:\n" +
            "  Shortest Job First gives the absolute minimum average waiting time for any\n" +
            "  set of stationary processes. (Downside: Starvation for long processes).\n\n" +
            "★ ROUND ROBIN QUANTUM RULE:\n" +
            "  • If TQ is too large  -> RR degenerates into FCFS.\n" +
            "  • If TQ is too small  -> Context-switch overhead destroys throughput.\n" +
            "  • Golden Rule         -> 80% of CPU bursts should be shorter than TQ.",
            0, 0
        );

        // ─────────────────────────────────────────────────────────────────────
        // 02. BANKER'S ALGORITHM
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
            "Developed by Edsger Dijkstra, the Banker's Algorithm ensures that resource allocations\n" +
            "never lead to DEADLOCK. A deadlock occurs when a set of processes are permanently blocked\n" +
            "because each process holds resources while waiting for ones held by others.\n\n" +
            "FOUR COFFMAN CONDITIONS FOR DEADLOCK:\n" +
            "  1. Mutual Exclusion  : Only one process can use a resource at a time.\n" +
            "  2. Hold and Wait     : Processes holding resources can request additional ones.\n" +
            "  3. No Preemption     : Resources cannot be forcibly seized.\n" +
            "  4. Circular Wait     : A closed chain of processes each waiting for the next.\n\n" +
            "CORE STATE MATRICES (N processes, M resource types):\n" +
            "  • Available[M]   : Vector of currently unallocated free resource units.\n" +
            "  • Max[N][M]      : Maximum resource claim declared by each process.\n" +
            "  • Allocation[N][M]: Resources currently loaned to each process.\n" +
            "  • Need[N][M]     : Additional resources each process will still request.",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. MATHEMATICAL MATRIX EQUATIONS                                       ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "1. NEED MATRIX RELATIONSHIP:\n" +
            "   Need[i][j] = Max[i][j] - Allocation[i][j]\n\n" +
            "2. RESOURCE REQUEST FEASIBILITY TEST:\n" +
            "   Condition 1 : Request[i] <= Need[i]     (Must not exceed declared max claim)\n" +
            "   Condition 2 : Request[i] <= Available   (Must not exceed current free inventory)\n\n" +
            "3. SPECULATIVE ALLOCATION EQUATIONS:\n" +
            "   Available'  = Available - Request[i]\n" +
            "   Allocation' = Allocation + Request[i]\n" +
            "   Need'       = Need - Request[i]",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. SAFETY ALGORITHM PSEUDOCODE                                         ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "Work = copy(Available);\n" +
            "Finish[0..N-1] = {false};\n" +
            "SafeSequence = [];\n\n" +
            "while (true) {\n" +
            "    boolean found = false;\n" +
            "    for (int i = 0; i < N; i++) {\n" +
            "        if (!Finish[i] && Need[i] <= Work) {\n" +
            "            Work = Work + Allocation[i]; // Pi finishes and returns resources!\n" +
            "            Finish[i] = true;\n" +
            "            SafeSequence.add(i);\n" +
            "            found = true;\n" +
            "        }\n" +
            "    }\n" +
            "    if (!found) break;\n" +
            "}\n" +
            "if (SafeSequence.size() == N) -> SYSTEM IS SAFE\n" +
            "else                          -> SYSTEM IS UNSAFE (DEADLOCK RISK)",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED EXAMPLE & SAFE SEQUENCE                         ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "SYSTEM STATE: Available = [3, 3, 2] (Resources: A, B, C)\n\n" +
            "┌────┬──────────────┬──────────────┬──────────────┬──────────────────────┐\n" +
            "│PROC│ ALLOCATION   │     MAX      │ NEED=MAX-ALOC│ STATUS IN SAFETY LOOP│\n" +
            "│    │  A   B   C   │  A   B   C   │  A   B   C   │                      │\n" +
            "├────┼──────────────┼──────────────┼──────────────┼──────────────────────┤\n" +
            "│ P0 │  0   1   0   │  7   5   3   │  7   4   3   │ Needs [7,4,3] > [3,3,2] WAIT│\n" +
            "│ P1 │  2   0   0   │  3   2   2   │  1   2   2   │ [1,2,2] <= [3,3,2] -> RUNS! │\n" +
            "│ P2 │  3   0   2   │  9   0   2   │  6   0   0   │ Needs [6,0,0] > Work WAIT │\n" +
            "│ P3 │  2   1   1   │  2   2   2   │  0   1   1   │ [0,1,1] <= [5,3,2] -> RUNS! │\n" +
            "│ P4 │  0   0   2   │  4   3   3   │  4   3   1   │ [4,3,1] <= [7,4,3] -> RUNS! │\n" +
            "└────┴──────────────┴──────────────┴──────────────┴──────────────────────┘\n\n" +
            "EXECUTION STEP SEQUENCE:\n" +
            "  1. P1 runs -> Releases [2,0,0] -> Work becomes [5, 3, 2]\n" +
            "  2. P3 runs -> Releases [2,1,1] -> Work becomes [7, 4, 3]\n" +
            "  3. P4 runs -> Releases [0,0,2] -> Work becomes [7, 4, 5]\n" +
            "  4. P0 runs -> Releases [0,1,0] -> Work becomes [7, 5, 5]\n" +
            "  5. P2 runs -> Releases [3,0,2] -> Work becomes [10, 5, 7]\n\n" +
            "RESULT: SAFE SEQUENCE = < P1 -> P3 -> P4 -> P0 -> P2 >",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET & COMMON TRAPS                                     ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "★ SAFE != DEADLOCK-FREE ONLY:\n" +
            "  An UNSAFE state is not yet deadlocked, but it means the OS CANNOT guarantee\n" +
            "  that deadlock won't happen if processes demand their full maximum claims.\n\n" +
            "★ RESOURCE WORK UPDATE:\n" +
            "  Remember that when a process completes, it yields ALLOCATION back to Work:\n" +
            "  Work_new = Work_current + Allocation[Pi]  (NOT Need!).",
            1, 1
        );

        // ─────────────────────────────────────────────────────────────────────
        // 03. PAGING
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
            "Paging completely eliminates EXTERNAL FRAGMENTATION by dividing logical memory into\n" +
            "fixed-sized blocks called PAGES, and physical RAM into identically-sized blocks called FRAMES.\n\n" +
            "HARDWARE TRANSLATION SCHEMATIC:\n" +
            "  CPU Logical Address = [ Page Number (p) | Offset (d) ]\n" +
            "                             │                 │\n" +
            "                             ▼                 │\n" +
            "                    ┌────────────────┐         │\n" +
            "                    │   PAGE TABLE   │         │\n" +
            "                    │ p -> Frame (f) │         │\n" +
            "                    └────────────────┘         │\n" +
            "                             │                 │\n" +
            "                             ▼                 ▼\n" +
            "  Physical Memory Address = [ Frame Number (f) | Offset (d) ]",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. MATHEMATICAL TRANSLATION FORMULAS                                   ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "Given Page Size = S bytes (typically 2^M bytes):\n" +
            "  • Page Number (p)       : p = floor(Logical_Address / S)\n" +
            "  • Offset (d)            : d = Logical_Address % S\n" +
            "  • Physical Address (PA) : PA = (Frame_Number * S) + d\n" +
            "  • Number of Pages       : Logical_Memory_Size / Page_Size\n" +
            "  • Number of Frames      : Physical_Memory_Size / Frame_Size",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. HARDWARE TRANSLATION PSEUDOCODE                                     ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "function translate(int logical_address, int page_size, PageTable pt) {\n" +
            "    int p = logical_address / page_size;\n" +
            "    int d = logical_address % page_size;\n" +
            "    if (p >= pt.entries.length || !pt[p].valid) {\n" +
            "        trigger_interrupt(PAGE_FAULT, p);\n" +
            "    }\n" +
            "    int f = pt[p].frameNumber;\n" +
            "    return (f * page_size) + d;\n" +
            "}",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED EXAMPLE                                         ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "PARAMETERS: Page Size = 512 bytes | Logical Address = 1040\n" +
            "PAGE TABLE:\n" +
            "  • Page 0 -> Frame 3\n" +
            "  • Page 1 -> Frame 1\n" +
            "  • Page 2 -> Frame 5\n" +
            "  • Page 3 -> Frame 2\n\n" +
            "STEP-BY-STEP COMPUTATION:\n" +
            "  Step 1: Calculate Page Number:\n" +
            "          p = 1040 / 512 = 2\n" +
            "  Step 2: Calculate Offset within page:\n" +
            "          d = 1040 % 512 = 16 bytes\n" +
            "  Step 3: Lookup Page Table entry for p = 2:\n" +
            "          PageTable[2] = Frame 5\n" +
            "  Step 4: Calculate Physical Address in RAM:\n" +
            "          Physical Address = (5 * 512) + 16 = 2560 + 16 = 2576\n\n" +
            "VERIFIED: Logical Address 1040 maps to Physical RAM Address 2576.",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET & INTERNAL FRAGMENTATION                           ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "★ INTERNAL FRAGMENTATION IN PAGING:\n" +
            "  Paging completely stops external fragmentation, but suffers from internal\n" +
            "  fragmentation on the final page of a process (average 0.5 * Page Size).\n\n" +
            "★ OFFSET NEVER CHANGES:\n" +
            "  Notice that offset 'd' in the logical address is identical to offset 'd'\n" +
            "  in the physical address. Only page number 'p' gets mapped to frame 'f'!",
            2, 2
        );

        // ─────────────────────────────────────────────────────────────────────
        // 04. PAGE REPLACEMENT
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
            "In demand-paged systems, when a referenced page is not present in physical RAM,\n" +
            "a PAGE FAULT occurs. If all memory frames are full, the OS must evict a 'victim frame'\n" +
            "to swap storage.\n\n" +
            "THREE CORE POLICIES:\n" +
            "  1. FIFO (First-In, First-Out)  : Evicts oldest loaded page. (Vulnerable to Belady's Anomaly).\n" +
            "  2. LRU (Least Recently Used)   : Evicts page that hasn't been accessed for the longest time.\n" +
            "  3. OPTIMAL (OPT / MIN)         : Evicts page that will not be accessed for longest future duration.",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. PERFORMANCE FORMULAS                                                ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "  • Total References (N) = Page_Hits (H) + Page_Faults (F)\n" +
            "  • Hit Ratio (%)        = (H / N) * 100%\n" +
            "  • Fault Ratio (%)      = (F / N) * 100%\n" +
            "  • Effective Access Time (EAT) = (1 - p)*Memory_Access_Time + p*Page_Fault_Overhead",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. LRU PAGE REPLACEMENT PSEUDOCODE                                     ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "for (int page : reference_stream) {\n" +
            "    if (frames.contains(page)) {\n" +
            "        hits++;\n" +
            "        touch_timestamp(page);\n" +
            "    } else {\n" +
            "        faults++;\n" +
            "        if (frames.size() == max_frames) {\n" +
            "            int victim = get_least_recently_used(frames);\n" +
            "            frames.remove(victim);\n" +
            "        }\n" +
            "        frames.add(page);\n" +
            "    }\n" +
            "}",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED EXAMPLE (LRU, 3 FRAMES)                         ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "REFERENCE STRING: 7, 0, 1, 2, 0, 3, 0, 4\n\n" +
            "┌─────────┬─────────┬─────────┬─────────┬──────────────┐\n" +
            "│ REF PAG │ FRAME 1 │ FRAME 2 │ FRAME 3 │ RESULT / HIT │\n" +
            "├─────────┼─────────┼─────────┼─────────┼──────────────┤\n" +
            "│    7    │    7    │    -    │    -    │ PAGE FAULT   │\n" +
            "│    0    │    7    │    0    │    -    │ PAGE FAULT   │\n" +
            "│    1    │    7    │    0    │    1    │ PAGE FAULT   │\n" +
            "│    2    │    2    │    0    │    1    │ FAULT (7 out)│\n" +
            "│    0    │    2    │    0    │    1    │ ★ CACHE HIT! │\n" +
            "│    3    │    2    │    0    │    3    │ FAULT (1 out)│\n" +
            "│    0    │    2    │    0    │    3    │ ★ CACHE HIT! │\n" +
            "│    4    │    4    │    0    │    3    │ FAULT (2 out)│\n" +
            "└─────────┴─────────┴─────────┴─────────┴──────────────┘\n" +
            "SUMMARY: Total Refs = 8 | Hits = 2 (25.0%) | Faults = 6 (75.0%)",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET & BELADY'S ANOMALY                                 ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "★ BELADY'S ANOMALY:\n" +
            "  For FIFO, adding more physical frames can counter-intuitively INCREASE\n" +
            "  the number of page faults (e.g. string 1,2,3,4,1,2,5,1,2,3,4,5).\n" +
            "  LRU and Optimal never suffer from Belady's Anomaly because they are Stack Algorithms.",
            3, 3
        );

        // ─────────────────────────────────────────────────────────────────────
        // 05. SEGMENTATION
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
            "Segmentation models memory from the programmer's view: variable-length logical units\n" +
            "such as Code, Stack, Heap, and Symbol Tables.\n\n" +
            "SEGMENT TABLE STRUCTURE:\n" +
            "  • BASE  : Starting physical RAM address of the segment.\n" +
            "  • LIMIT : The exact byte length (size) of the segment.",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. BOUNDS CHECKING & ADDRESS EQUATION                                  ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "Given Logical Address = <Segment s, Offset d>:\n" +
            "  1. Bounds Condition : d < SegmentTable[s].Limit\n" +
            "  2. Valid Access     : Physical Address = SegmentTable[s].Base + d\n" +
            "  3. Trap Condition   : If d >= Limit -> Hardware TRAP (SEGMENTATION FAULT)",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. TRANSLATION PSEUDOCODE                                              ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "if (s >= segment_table.length) trap('Invalid Segment Number');\n" +
            "SegmentEntry e = segment_table[s];\n" +
            "if (d < 0 || d >= e.limit) {\n" +
            "    raise_trap('SEGMENTATION FAULT: Offset exceeds segment boundary');\n" +
            "} else {\n" +
            "    return e.base + d;\n" +
            "}",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED EXAMPLE                                         ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "SEGMENT TABLE:\n" +
            "  • Seg 0 (Code)  : Base = 1400, Limit = 1000\n" +
            "  • Seg 1 (Stack) : Base = 6300, Limit = 400\n\n" +
            "CASE 1: Request <Seg 0, Offset 420>\n" +
            "  Check: 420 < 1000? YES -> Physical Address = 1400 + 420 = 1820 (OK)\n\n" +
            "CASE 2: Request <Seg 1, Offset 450>\n" +
            "  Check: 450 < 400? NO -> 450 exceeds limit! TRAP: SEGMENTATION FAULT!",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET: PAGING VS SEGMENTATION                            ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "  • Paging       : Fixed block size, invisible to programmer, NO external frag.\n" +
            "  • Segmentation : Variable block size, visible to programmer, HAS external frag.",
            4, 2
        );

        // ─────────────────────────────────────────────────────────────────────
        // 06. FRAGMENTATION
        // ─────────────────────────────────────────────────────────────────────
        MANUALS[5] = new ManualEntry(
            "06",
            "CONTIGUOUS ALLOCATION & FRAGMENTATION",
            "MISSION 06: PARTITION STRATEGIES & COMPACTION",
            "★★★☆☆ (MODERATE)",
            "FIRST FIT | BEST FIT | WORST FIT | INTERNAL FRAG | EXTERNAL FRAG",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 1. EXECUTIVE OVERVIEW: FORMS OF FRAGMENTATION                          ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "• Internal Fragmentation: Unused space inside an allocated partition block.\n" +
            "• External Fragmentation: Total free memory is adequate, but fragmented into non-contiguous slices.",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. PLACEMENT STRATEGIES FORMULAS                                       ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "• FIRST FIT : Allocates first hole with size >= process size. Fast O(N).\n" +
            "• BEST FIT  : Finds smallest hole with size >= process size (minimizes leftover hole).\n" +
            "• WORST FIT : Allocates largest hole available (leaves largest remaining hole).",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. BEST-FIT ALLOCATION PSEUDOCODE                                      ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "int bestIdx = -1;\n" +
            "for (int i = 0; i < holes.length; i++) {\n" +
            "    if (holes[i].size >= reqSize) {\n" +
            "        if (bestIdx == -1 || holes[i].size < holes[bestIdx].size) {\n" +
            "            bestIdx = i;\n" +
            "        }\n" +
            "    }\n" +
            "}\n" +
            "if (bestIdx != -1) split_and_allocate(holes[bestIdx], reqSize);",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED EXAMPLE                                         ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "HOLES: [H1: 100 KB]  [H2: 500 KB]  [H3: 200 KB]  [H4: 300 KB]\n" +
            "Incoming Process: 212 KB\n\n" +
            "  • First Fit : Skips H1 -> Fits in H2 (500 KB). Leftover = 288 KB.\n" +
            "  • Best Fit  : Fits in H4 (300 KB). Leftover = 88 KB.\n" +
            "  • Worst Fit : Allocates in largest H2 (500 KB). Leftover = 288 KB.",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET: COMPACTION & 50% RULE                             ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "★ COMPACTION:\n" +
            "  External fragmentation can be eliminated by Compaction (moving all active\n" +
            "  processes to one side of RAM to form one large free memory hole).\n" +
            "  Compaction is only possible if relocation is dynamic and done at runtime.",
            5, 1
        );

        // ─────────────────────────────────────────────────────────────────────
        // 07. DISK SCHEDULING
        // ─────────────────────────────────────────────────────────────────────
        MANUALS[6] = new ManualEntry(
            "07",
            "DISK I/O HEAD SCHEDULING",
            "MISSION 07: CYLINDER TRAVERSAL & SEEK-TIME MINIMIZATION",
            "★★★☆☆ (MODERATE)",
            "FCFS | SSTF | SCAN | LOOK | C-SCAN | TOTAL HEAD MOVEMENT",
            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 1. EXECUTIVE OVERVIEW: DISK MECHANICS                                  ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "Disk Seek Time (moving the mechanical head to the target cylinder) dominates disk I/O.\n" +
            "Disk scheduling reorders pending I/O requests to minimize Total Head Movement (THM).",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. SEEK DISTANCE FORMULAS                                              ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "  • Step Seek Distance     : |Current_Cylinder - Target_Cylinder|\n" +
            "  • Total Head Movement    : THM = Σ |C[i] - C[i-1]|\n" +
            "  • Average Seek Distance  : THM / Total_Requests",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. SSTF PSEUDOCODE                                                     ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "while (!queue.isEmpty()) {\n" +
            "    int nextCyl = find_closest(queue, currentHead);\n" +
            "    thm += abs(nextCyl - currentHead);\n" +
            "    currentHead = nextCyl;\n" +
            "    queue.remove(nextCyl);\n" +
            "}",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED EXAMPLE                                         ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "QUEUE: 98, 183, 37, 122, 14, 124, 65, 67 | Head = 53\n" +
            "SSTF Path: 53 -> 65(12) -> 67(2) -> 37(30) -> 14(23) -> 98(84) -> 122(24) -> 124(2) -> 183(59)\n" +
            "TOTAL HEAD MOVEMENT (SSTF) = 236 cylinders! (FCFS is 640 cylinders - 63% savings!).",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET: SSTF VS SCAN                                      ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "★ SSTF STARVATION:\n" +
            "  SSTF can cause starvation for cylinders far away from the head.\n" +
            "  SCAN (Elevator) algorithm avoids starvation by sweeping in one continuous direction.",
            6, 0
        );

        // ─────────────────────────────────────────────────────────────────────
        // 08. MULTIPROGRAMMING
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
            "Processes spend much of their lifetime waiting for I/O. Multiprogramming maintains\n" +
            "multiple processes in memory so the CPU switches to another process when one is blocked.",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 2. PROBABILISTIC CPU UTILIZATION FORMULA                               ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "Let p = fraction of time a process waits for I/O, N = Degree of Multiprogramming:\n" +
            "  • Probability all N processes are idle = p^N\n" +
            "  • Overall CPU Utilization             = 1 - p^N",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 3. CONCURRENCY ADMISSION PSEUDOCODE                                    ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "if (current_ram + job.ram <= total_ram) {\n" +
            "    degree_N++;\n" +
            "    cpu_util = 1.0 - Math.pow(p_io_wait, degree_N);\n" +
            "} else {\n" +
            "    spool_to_disk(); // Prevent Thrashing\n" +
            "}",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 4. STEP-BY-STEP WORKED EXAMPLE                                         ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "Given p = 0.80 (80% I/O wait):\n" +
            "  • N = 1 (Uniprogramming) : CPU Util = 1 - 0.80^1 = 20%\n" +
            "  • N = 2                  : CPU Util = 1 - 0.80^2 = 36%\n" +
            "  • N = 4                  : CPU Util = 1 - 0.80^4 = 59.04%\n" +
            "  • N = 8                  : CPU Util = 1 - 0.80^8 = 83.22%\n" +
            "  • N = 12                 : CPU Util = 1 - 0.80^12 = 93.13%!",

            "╔════════════════════════════════════════════════════════════════════════╗\n" +
            "║ 5. EXAM CHEAT-SHEET: THRASHING                                         ║\n" +
            "╚════════════════════════════════════════════════════════════════════════╝\n" +
            "★ THRASHING:\n" +
            "  When Degree of Multiprogramming is raised too high beyond available RAM,\n" +
            "  processes spend 99% of time swapping pages rather than executing instructions.\n" +
            "  CPU Utilization suddenly crashes to near zero!",
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
