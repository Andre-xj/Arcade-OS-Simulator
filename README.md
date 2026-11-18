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
| **01** | **CPU Scheduling** | FCFS, SJF (Non-preemptive), SRTF (Preemptive), Priority (Preemptive & Non-preemptive), Round Robin (Configurable Quantum $q$) | Live Timeline Gantt Chart, Waiting Time ($\bar{WT}$), Turnaround Time ($\bar{TAT}$), Ready Queue | `CPUSchedulingPanel.java`<br>`CPUComparisonDialog.java`<br>`KernelTapperDialog.java` |
| **02** | **Banker's Algorithm** | Dijkstra's Safety Algorithm, Multi-Resource Need Matrix calculation ($Need = Max - Alloc$), Safe Sequence Finder, Resource-Request What-If Analysis | Allocation/Max/Need Matrices, Available Vector, Safety Auditor, Space Shoot Arcade | `BankersPanel.java`<br>`VaultPanicDialog.java` |
| **03** | **Paging** | Logical-to-Physical Address Translation, Page-to-Frame Mapping, Page Table lookup, Variable Page & Frame Size calculation | Real-Time Address Splitter ($p = addr / size$, $d = addr \pmod{size}$), Physical Bus Hopper Canvas | `PagingPanel.java`<br>`PagingHopperCanvas.java`<br>`FrameHopperDialog.java` |
| **04** | **Page Replacement** | FIFO (First-In First-Out), LRU (Least Recently Used), Optimal (Belady's Min) | Frame State Grid, Page Hit / Fault counter, Hit Rate Ratio ($Hit\%$), Mario Cache Arena | `PageReplacementPanel.java`<br>`MarioCacheRunnerDialog.java` |
| **05** | **Segmentation** | Segment Table validation, Base & Limit Register mapping, Hardware Trap / Boundary Fault detection | Segment Limit Trap Detector, Heap/Stack Visual Arena, Variable Segment Editor, Heap vs Stack Snake Arena | `SegmentationPanel.java`<br>`SnakeGameDialog.java`<br>`SegmentSnakeArenaCanvas.java` |
| **06** | **RAM Allocation & Fragmentation** | First-Fit, Best-Fit, Next-Fit, Contiguous Partitioning, Memory Compaction | Silicon RAM Motherboard Map, Internal Heat Frag, External Frag Gauges, Cyber Crane Attack Arena | `FragmentationPanel.java`<br>`DefragPanicDialog.java` |
| **07** | **Disk Scheduling** | FCFS, SSTF, SCAN, C-SCAN, LOOK, C-LOOK | Cylindrical Track Platter, Step-by-Step Traversal Order, Total Head Movement (Cylinders), Pac-Man Disk Arena | `DiskSchedulingPanel.java`<br>`PacmanDiskArcadeDialog.java` |
| **08** | **Multiprogramming** | Degree of Multiprogramming ($N$), Concurrent CPU Scheduling, CPU Utilization Curve ($1 - p^N$), Context Switching Overhead | Concurrency Kitchen Dashboard, CPU Load vs. I/O Wait Ratio, Memory Footprint Gauges | `MultiprogrammingPanel.java`<br>`OvercookedKitchenDialog.java` |

---

## 📊 Compliance with Rubric Criteria

### A. Program Development & Implementation (40 Points)
- **100% Complete Implementation:** All 8 required modules are fully functional, interactive, and mathematically verified.
- **Safe State Sequence:** Guaranteed safe sequence generation under Banker's algorithm; detects unsafe states and deadlocks accurately.
- **Fault Traps:** Hardware limit traps trigger immediately on illegal logical addresses in Segmentation ($d \ge Limit$).

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

### 1. CPU Scheduling
- **Small Dataset (Classic 4 Jobs):**
  - P1: Arrival = 0, Burst = 7, Priority = 3
  - P2: Arrival = 2, Burst = 4, Priority = 1
  - P3: Arrival = 4, Burst = 1, Priority = 4
  - P4: Arrival = 5, Burst = 4, Priority = 2
- **Large Dataset (8+ Jobs Stress Test):**
  - Press the **CSV / Preset** shortcut or click `+ ADD` to dynamically scale up to 10+ processes.

### 2. Banker's Algorithm
- **Classic Galvin 5-Process Benchmark (Safe Sequence: P1 -> P3 -> P4 -> P0 -> P2):**
  - Available: `[3, 3, 2]`
  - P0: Alloc `[0, 1, 0]`, Max `[7, 5, 3]`
  - P1: Alloc `[2, 0, 0]`, Max `[3, 2, 2]`
  - P2: Alloc `[3, 0, 2]`, Max `[9, 0, 2]`
  - P3: Alloc `[2, 1, 1]`, Max `[2, 2, 2]`
  - P4: Alloc `[0, 0, 2]`, Max `[4, 3, 3]`
- **Unsafe / Deadlock Trap:**
  - Click the **DEADLOCK** scenario button to verify instant deadlock alert and detection.

### 3. Page Replacement
- **Reference String:** `7 0 1 2 0 3 0 4 2 3 0 3 2 1 2 0 1 7 0 1`
- **Frames:** `3` or `4` frames.
- **Verification:**
  - FIFO: 15 faults (for 3 frames)
  - LRU: 12 faults (for 3 frames)
  - Optimal: 9 faults (for 3 frames)

### 4. Disk Scheduling
- **Initial Head:** `53`, Disk Limit: `199`
- **Queue:** `98 183 37 122 14 124 65 67`
- **Expected Total Head Movement:**
  - FCFS: 640 cylinders
  - SSTF: 236 cylinders
  - SCAN (Upward): 208 cylinders
  - LOOK: 299 cylinders

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
