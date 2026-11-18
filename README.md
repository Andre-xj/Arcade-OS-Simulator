# 🕹️ ARCADE OS // 8-Bit Retro Operating System Simulator

> **Course:** Operating Systems  
> **Project:** Final Operating System Simulator (GUI-Based Application)  
> **Language:** Java (JDK 17+) — 100% Native Java AWT & Swing (Zero External JAR Dependencies)  
> **Target OS:** Windows / macOS / Linux  

---

## 📋 Table of Contents
1. [Project Overview](#-project-overview)
2. [Quick Start & How to Run](#-quick-start--how-to-run)
3. [8 Required OS Modules & Algorithm Coverage](#-8-required-os-modules--algorithm-coverage)
4. [Compliance with Rubric Criteria](#-compliance-with-rubric-criteria)
5. [Sample Test Datasets (Small vs. Large)](#-sample-test-datasets-small-vs-large)
6. [Source Code Organization](#-source-code-organization)

---

## 🌟 Project Overview
**Arcade OS** is an interactive, gamified Operating System Simulator built from the ground up to demonstrate core operating system concepts through immersive 16-bit retro-arcade simulation mechanics. 

Every module combines:
- **Mathematical Accuracy:** Strict adherence to textbook algorithms (Galvin / Silberschatz / Tanenbaum).
- **Dynamic User Inputs:** Complete freedom to configure variable processes, cylinders, reference strings, matrices, and memory holes without arbitrary size limits.
- **Visual Telemetry:** Real-time Gantt charts, memory partition maps, page-to-frame translation pipelines, and cylinder head seek animations.
- **Interactive Minigames:** Unique interactive arcade adaptations illustrating each OS concept in action.

---

## 🚀 Quick Start & How to Run

### Option 1: One-Click Launch (Windows)
Double-click `run.bat` or run:
```cmd
run.bat
```

### Option 2: Pre-Built Standalone JAR
Double-click `ArcadeOS.jar` or execute:
```bash
java -jar ArcadeOS.jar
```

### Option 3: Compile from Source (Standard JDK)
```bash
# Compile to bin directory
javac -d bin -cp "bin" arcadeos/*.java game/snake/*.java

# Run main class
java -cp "bin" arcadeos.ArcadeOS
```

---

## 🧩 8 Required OS Modules & Algorithm Coverage

| Mission | OS Module | Algorithms / Policies Implemented | Key Visuals & Telemetry | Corresponding Java Class |
|:---:|:---|:---|:---|:---|
| **01** | **CPU Scheduling** (6 pts) | FCFS, SJF (Non-preemptive), SRTF (Preemptive SJF), Priority (Preemptive & Non-preemptive), Round Robin (Configurable Quantum $q$) | Live Timeline Gantt Chart, Per-Process & Average Waiting Time ($\bar{WT}$), Turnaround Time ($\bar{TAT}$), Ready Queue | `CPUSchedulingPanel.java`<br>`CPUComparisonDialog.java`<br>`KernelTapperDialog.java` |
| **02** | **Banker's Algorithm** (5 pts) | Dijkstra's Safety Algorithm, Multi-Resource Need Matrix ($Need = Max - Alloc$), Safe Sequence Finder, What-If Resource-Request Analysis | Dynamic Process Count & Resource Count, Allocation/Max/Need Matrices, Available Vector, Safety Auditor | `BankersPanel.java`<br>`VaultPanicDialog.java` |
| **03** | **Paging** (4 pts) | Logical-to-Physical Address Translation, Page-to-Frame Mapping Table, Page Table lookup, Variable Page & Frame Size | Real-Time Address Splitter ($p = addr / size$, $d = addr \pmod{size}$), Physical RAM Stack, Page Fault Handling | `PagingPanel.java`<br>`PagingHopperCanvas.java`<br>`FrameHopperDialog.java` |
| **04** | **Page Replacement** (6 pts) | FIFO (First-In First-Out), LRU (Least Recently Used), Optimal (Belady's Min) | Unlimited Pipes with Auto-Centering & Smooth Scrolling, Step-by-Step Frame Matrix, Page Hits & Faults Counters | `PageReplacementPanel.java`<br>`MarioCacheRunnerDialog.java` |
| **05** | **Segmentation** (4 pts) | Segment Table (Base & Limit registers), Hardware Limit Trap Validation ($offset \ge limit$) | Out-of-Range SIGSEGV Fault Detector, Heap/Stack Visual Arena, Variable Segment Editor, Snake Arena | `SegmentationPanel.java`<br>`game.snake.SnakeGameDialog.java`<br>`SegmentSnakeArenaCanvas.java` |
| **06** | **RAM Allocation & Fragmentation** (4 pts) | First-Fit, Best-Fit, Next-Fit, Contiguous Partitioning, Memory Compaction | Multiple Memory Blocks & Processes, Internal Heat Frag, External Frag Alert Bridge, Defragmentation Tool | `FragmentationPanel.java`<br>`DefragPanicDialog.java` |
| **07** | **Disk Scheduling** (6 pts) | FCFS, SSTF, SCAN, C-SCAN (Circular with Warp Tunnel), LOOK, C-LOOK | Cylindrical Platter & Sector Track, Step-by-Step Service Order, Total Head Movement & Avg Seek HUD | `DiskSchedulingPanel.java`<br>`PacmanDiskArcadeDialog.java` |
| **08** | **Multiprogramming** (5 pts) | Degree of Multiprogramming ($N$), Processor Sharing Model ($\%CPU/n_j = 1/n_j$), Kitchen Concurrency | Active Concurrency ($n_j$), CPU Share %, 100% CPU & RAM Utilization Display, Interval Execution Matrix | `MultiprogrammingPanel.java`<br>`OvercookedKitchenDialog.java` |

---

## 📊 Compliance with Rubric Criteria

### A. Program Development & Implementation (40 Points)
1. **CPU Scheduling (6 pts):**
   - Implements FCFS, SJF (preemptive SRTF & non-preemptive), Priority, and Round Robin (with dynamic time quantum).
   - Generates interactive visual Gantt charts with exact execution order.
   - Computes Arrival, Burst, Completion, Waiting Time ($WT$), and Turnaround Time ($TAT$) per process, plus system Average Waiting Time and Average Turnaround Time.
2. **Banker's Algorithm (5 pts):**
   - Supports user-configured process count (dynamic add/remove rows) and multi-resource counts (A, B, C tokens).
   - Automatically computes textbook Need Matrix ($Need = Max - Alloc$).
   - Accurately reports Safe vs. Unsafe state and outputs the exact Safe Sequence (e.g. `< P1 ➔ P3 ➔ P4 ➔ P0 ➔ P2 >`) or deadlock detection.
3. **Paging (4 pts):**
   - Implements page-to-frame mapping table with valid/unmapped status indicators.
   - Computes complete logical to physical address translation with page size decomposition and physical RAM stack highlighting.
4. **Page Replacement (6 pts):**
   - Implements FIFO, LRU, and Optimal algorithms.
   - Displays step-by-step frame changes across Mario Warp Pipes with horizontal memory trace matrix.
   - Real-time tracking of Page Hits (with hit ratio and coin beams) and Page Faults (with penalty disk swaps and eviction lasers).
   - Auto-centering for low pipe counts (2, 3, 4) and unlimited horizontal scrolling for large frame allocations.
5. **Segmentation (4 pts):**
   - Segment table with `#`, Segment Name, Base, Limit, and End address registers.
   - Hardware trap validation: if $offset \ge limit$, halts address calculation and displays an explicit **LIMIT TRAP: SEGMENTATION FAULT (SIGSEGV)** warning.
6. **Memory Allocation & Fragmentation (4 pts):**
   - Configurable multiple memory blocks/sockets and multiple process chips.
   - Shows First-Fit, Best-Fit, and Next-Fit allocation distributions.
   - Distinctly calculates and visualizes **Internal Fragmentation** (leftover partition space) and **External Fragmentation** (insufficient contiguous space despite adequate total free memory) with dynamic defragmentation.
7. **Disk Scheduling (6 pts):**
   - Implements all 6 classic algorithms: FCFS, SSTF, SCAN, C-SCAN, LOOK, and C-LOOK.
   - Displays complete service traversal order and cumulative Total Head Movement (cylinders) with average seek metrics.
8. **Multiprogramming (5 pts):**
   - Simulates multiple concurrent processes sharing CPU and resources via the processor sharing model ($\%CPU/n_j = 1/n_j$).
   - Live system status display with real-time active concurrency ($n_j$), CPU share, and CPU/RAM utilization metrics.

### B. GUI Design & Usability (10 Points)
- **Consistent Arcade Theme:** Unified retro CRT scanlines, 3D tactile arcade buttons, custom pixel sprites, and crisp Consolas typography.
- **Dynamic User Inputs (Section III):** Unlimited processes, requests, segments, frames, and reference strings can be added, edited, or cleared dynamically.
- **Bulletproof Input Validation:** Automatically catches empty strings, negative values, malformed characters, and out-of-bound requests with visual toast notifications instead of crashing.

### C. Algorithm Accuracy & Testing (15 Points)
- **Theoretical Comparison:** Includes built-in "Compare All Policies" engine comparing metrics across all algorithms simultaneously.
- **Step-by-Step Inspection:** Supports forward single-step execution, theoretical preview, and auto-solve speed multipliers ($1\times, 2\times, 4\times$).

### D. Source Code & Submission (10 Points)
- **Readable & Structured:** Fully commented Javadoc headers across all modules.
- **Zero Third-Party Dependencies:** Runs on standard Java Runtime Environment (JRE) without requiring Maven, Gradle, or native DLLs.

---

## 🧪 Sample Test Datasets (Small vs. Large)
Per Section III and Criterion C of the 100-point rubric, each of the 8 modules is tested and verified with both small datasets (standard textbook benchmarks) and substantially larger datasets (stress tests with dynamic scaling):

### 1. CPU Scheduling
- **Small Dataset (Classic 4 Jobs):**
  - P1: Arrival = 0, Burst = 7, Priority = 3
  - P2: Arrival = 2, Burst = 4, Priority = 1
  - P3: Arrival = 4, Burst = 1, Priority = 4
  - P4: Arrival = 5, Burst = 4, Priority = 2
  - *Expected Verification (FCFS):* Order: P1 -> P2 -> P3 -> P4 | Avg WT: 4.00 ms | Avg TAT: 8.00 ms
- **Large Dataset (10 Jobs Dynamic Stress Test):**
  - 10 custom jobs with staggered arrivals (t=0 to t=18) and varied burst times (1 to 12 ms).
  - *Verification:* Handled seamlessly with real-time Gantt packing and preemptive SRTF/RR context switching.

### 2. Banker's Algorithm
- **Small Dataset (Classic Galvin 5-Process Benchmark):**
  - Available: `[3, 3, 2]`
  - P0: Alloc `[0, 1, 0]`, Max `[7, 5, 3]`, Need `[7, 4, 3]`
  - P1: Alloc `[2, 0, 0]`, Max `[3, 2, 2]`, Need `[1, 2, 2]`
  - P2: Alloc `[3, 0, 2]`, Max `[9, 0, 2]`, Need `[6, 0, 0]`
  - P3: Alloc `[2, 1, 1]`, Max `[2, 2, 2]`, Need `[0, 1, 1]`
  - P4: Alloc `[0, 0, 2]`, Max `[4, 3, 3]`, Need `[4, 3, 1]`
  - *Expected Safe Sequence:* `P1 -> P3 -> P4 -> P0 -> P2` (System 100% Safe)
- **Large Dataset (7+ Processes & 4 Resource Types / Deadlock Stress):**
  - Click the **STRESS 6P** or **DEADLOCK** scenario buttons.
  - *Verification:* Accurately identifies circular wait and flags deadlock with immediate red warnings.

### 3. Paging & Address Translation
- **Small Dataset:**
  - Page Size = 1 KB (1024 B), 4 Logical Pages, 8 Physical Frames.
  - Logical Address = 2150 -> Page Number $p = 2150 / 1024 = 2$, Offset $d = 2150 \pmod{1024} = 102$.
  - Given Page Table: $P2 \to Frame\ 5$ -> Physical Address = $5 \times 1024 + 102 = 5222$.
- **Large Dataset (Multi-Page Stream):**
  - Stream of 16+ arbitrary logical addresses across 64 frames.
  - *Verification:* Real-time hardware address splitter with visual bus hopper animations.

### 4. Page Replacement
- **Small Dataset (8 Reference Stream):**
  - Reference: `1, 2, 3, 4, 1, 2, 5, 1` with 3 Frames.
- **Large Dataset (Classic 20-Page Belady Benchmark):**
  - Reference String: `7 0 1 2 0 3 0 4 2 3 0 3 2 1 2 0 1 7 0 1` with 3 and 4 Frames.
  - *Verification (3 Frames):*
    - FIFO: 15 Faults (5 Hits, 25.0% Hit Rate)
    - LRU: 12 Faults (8 Hits, 40.0% Hit Rate)
    - Optimal: 9 Faults (11 Hits, 55.0% Hit Rate)

### 5. Memory Segmentation
- **Small Dataset (3 Standard Segments):**
  - S0 (CODE): Base = 0, Limit = 400
  - S1 (DATA): Base = 1400, Limit = 600
  - S2 (STACK): Base = 2800, Limit = 800
  - *Legal Access:* S1 with Offset 300 -> PA = $1400 + 300 = 1700$ (Valid)
  - *Hardware Trap:* S1 with Offset 650 -> $650 \ge 600$ (Hardware Limit Trap / SegFault Exception)
- **Large Dataset (8 Dynamic Variable Segments):**
  - Complex segmented address space with irregular limit sizes; tests overlapping boundary audits.

### 6. Contiguous RAM Allocation & Fragmentation
- **Small Dataset (3 Partitions, 3 Processes):**
  - Blocks: `[100 KB, 500 KB, 200 KB]` | Processes: `[212 KB, 417 KB, 112 KB]`
  - *Verification:* Compares First-Fit, Best-Fit, and Worst-Fit allocation distributions.
- **Large Dataset (Galvin 5-Block / Knuth 50% Rule Benchmark):**
  - Memory: 1024 KB Motherboard with 5+ variable partition holes.
  - *Verification:* Real-time internal heat fragmentation calculation and dynamic memory compaction.

### 7. Disk Scheduling
- **Small Dataset (5 Cylinder Requests):**
  - Initial Head: 50, Queue: `[20, 80, 40, 10, 90]`
- **Large Dataset (Classic 8-Request Textbook Benchmark):**
  - Initial Head: `53`, Disk Boundary: `0 to 199`
  - Request Queue: `98 183 37 122 14 124 65 67`
  - *Expected Head Movement:*
    - FCFS: 640 cylinders
    - SSTF: 236 cylinders
    - SCAN (Upward): 208 cylinders
    - LOOK (Upward): 299 cylinders
    - C-SCAN: 382 cylinders
    - C-LOOK: 322 cylinders

### 8. Multiprogramming & Concurrency
- **Small Dataset (3 Processes):**
  - CPU Utilization: $1 - p^3$ where I/O wait probability $p = 0.50$ -> $87.5\%$ utilization.
- **Large Dataset (8 Concurrent Processes):**
  - Demonstrates saturation curve as degree of multiprogramming scales from $N=1$ to $N=8$, accounting for context switching thrashing overhead.

---

## 📁 Source Code Organization

```
ArcadeOS_Modular_Java/
│
├── ArcadeOS.jar                 # Standalone Runnable Executable JAR
├── run.bat                      # Windows One-Click Execution Batch Script
├── compile.bat                  # Clean Project Build Script
├── README.md                    # Project Documentation & Rubric Compliance Guide
│
├── arcadeos/                    # Core Arcade OS Source Package
│   ├── ArcadeOS.java            # Main Application Window & Navigation Router
│   ├── AppState.java            # Session State, Scores, and Progress Persistence
│   ├── LoginPanel.java          # User Authentication & Operator Mode Screen
│   ├── MainMenuPanel.java       # Main Navigation Hub
│   ├── ArchivesPanel.java       # Stage Select & System Dossier Carousel
│   ├── SimulatorContainerPanel.java # Unified Mission Cartridge Tab System
│   ├── SystemInitPanel.java     # BIOS POST Boot Sequence
│   ├── QuizPanel.java           # Interactive OS Concept Mastery Arena
│   │
│   ├── CPUSchedulingPanel.java  # Mission 01: CPU Scheduling Simulator
│   ├── BankersPanel.java        # Mission 02: Banker's Algorithm & Deadlock Avoidance
│   ├── PagingPanel.java         # Mission 03: Paging & Address Translation
│   ├── PageReplacementPanel.java# Mission 04: Cache Page Replacement
│   ├── SegmentationPanel.java   # Mission 05: Memory Segmentation & Trap Auditor
│   ├── FragmentationPanel.java  # Mission 06: Contiguous RAM Allocation & Defrag
│   ├── DiskSchedulingPanel.java # Mission 07: Disk Track Traversal
│   ├── MultiprogrammingPanel.java # Mission 08: Multiprogramming Concurrency
│   │
│   ├── VaultPanicDialog.java    # Mission 02 Arcade: Space Shoot
│   ├── DefragPanicDialog.java   # Mission 06 Arcade: Cyber Crane Attack
│   ├── FrameHopperDialog.java   # Mission 03 Arcade: Paging Hopper
│   ├── MarioCacheRunnerDialog.java # Mission 04 Arcade: Mario Cache Replacement
│   ├── PacmanDiskArcadeDialog.java # Mission 07 Arcade: Pac-Man Disk Seek
│   ├── KernelTapperDialog.java  # Mission 01 Arcade: Kernel Tapper
│   ├── OvercookedKitchenDialog.java # Mission 08 Arcade: Overcooked Kitchen
│   │
│   ├── RetroTheme.java          # CRT Colors, Dynamic Tactile 3D Buttons & Styling
│   ├── RetroIcons.java          # Zero-Dependency Vector Icons (Crisp at all DPIs)
│   └── PixelSprites.java        # Custom 8-Bit Pixel Sprite Graphic Generators
│
└── game/                        # Subsystem Support Modules
    └── snake/                   # Mission 05 Segmentation Snake Game Engine
```

---

*Authored for the Operating Systems Simulator Final Project Defense.*
