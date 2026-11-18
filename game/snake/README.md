# Snake OS: Memory Segmentation Game

## Overview
`game.snake` is an educational retro arcade game embedded in the Arcade OS Simulator. It teaches the fundamentals of **Memory Segmentation**, address translation, bounds checking, and protection faults using snake mechanics.

---

## How to Run

### Option 1: Direct CLI Command
Compile all sources and launch the Arcade OS Simulator:
```bash
javac -cp . arcadeos/*.java game/snake/*.java
java -cp . arcadeos.Main
```

### Option 2: Main Menu Navigation
1. Launch Arcade OS Simulator.
2. From the main launcher / mission select menu, select **Mission 05: Memory Segmentation**.
3. In the manual simulator, pick any segment from the table or dropdown, then click the **LAUNCH SNAKE GAME** button in the top control bar.

---

## Controls

| Key | Action | Context / Availability |
|---|---|---|
| **Arrow Keys / WASD** | Steer snake head (Up, Down, Left, Right) | All Levels |
| **Spacebar** | Return Pop (pops 2 stack frames, 4s cooldown) | Level 2 (Growing Stack) only |
| **1, 2, 3** | Load Segment Register (switches to Segment 1, 2, or 3) | Level 3 (Segment Switch) only |
| **Tab** (Hold) | Inspect Segment Table entry (Base, Limit, Perms) | All Levels |
| **P / Esc** | Pause / Resume game and display help overlay | All Levels |
| **R** | Restart current level | Game Over / Pause / In-game |
| **Enter / Space** | Advance / Proceed | Level Clear / Summary card |

---

## Architecture & Hardware Mapping

| Game Mechanic | Segmentation Hardware Equivalent |
|---|---|
| **Snake Head Position** | **Logical Offset** within the active segment ($0 \le \text{offset} < \text{limit}$). |
| **Arena Board / Panel** | **Segment Memory Region** in physical RAM ($[\text{base} .. \text{base} + \text{limit})$). |
| **Limit Barrier (Top Line)** | **Hardware Limit Register** bounds check. |
| **Crossing Arena Top** | **Limit Trap / SEGFAULT**: hardware trap triggered when $\text{offset} \ge \text{limit}$. |
| **Live PA Chip** | **Physical Address Generation**: computed dynamically as $\text{PA} = \text{Base} + \text{Offset}$. |
| **Keys 1, 2, 3** | **Segment Selector / Register**: switches the active segment base and limit descriptor. |
| **Read-Only Segment 3** | **Protection Bits**: W pellets act as write hazards triggering `PROTECTION FAULT`. |
| **Stack Frontier & Collision** | **External Fragmentation**: adjacent memory region growth preventing heap expansion. |

---

## Hardware Translation Details

### 1. Address Translation
In segmentation hardware, logical addresses consist of a tuple `(segment, offset)`. The MMU translates this using the segment table:
$$\text{Physical Address (PA)} = \text{Base} + \text{Offset}$$
A hardware limit register ensures that:
$$0 \le \text{Offset} < \text{Limit}$$
Any attempt to access an offset $\ge \text{Limit}$ triggers a hardware `SEGFAULT` (limit trap).

### 2. Live PA Chip
Located right next to the `OFFSET` gauge on the HUD:
- **Level 1 & 2**: Displays `-> PA <addr>` (e.g. `-> PA 1479`), computed from the active segment's real base + head offset.
- **Level 3**: Displays `(<seg>, <offset>) -> PA <addr>` (e.g. `(1, 250) -> PA 6550`).

### 3. Arena Ruler
- Shows decimal offsets directly inside the arena: `0` at the base line, `limit` at the top boundary line, with unlabeled tick marks in between.
- Removes all board-base vs real-base confusion: only the real base from the segment table is used, and decorative bands carry no addresses.

### 4. Kernel Log
- On each `MALLOC` pickup:
  `malloc +1 @ offset <off> -> PA <pa> (base <base> + <off>)`
- On fault:
  Logs the attempted offset, the limit, and the boundary crossed.

### 5. Tab Overlay
Pressing `Tab` reveals the hardware segment table entry:
- `SEG #`: Segment index
- `BASE`: Physical base address
- `LIMIT`: Segment size boundary
- `PERM`: Permissions (`R/W` or `R`)

---

## Multi-Segment Switching (Level 3)

### 1. Multi-Segment Layout
Level 3 presents three segment panels side by side (each 8 columns wide, 24 columns total):
1. **Segment 1 (Audio_Engine)**: Base 6300, Limit 300 (6 rows @ 50 units/row), Permissions `R/W`.
2. **Segment 2 (Arcade_Sprites)**: Base 1400, Limit 400 (8 rows @ 50 units/row), Permissions `R/W`.
3. **Segment 3 (High_Scores)**: Base 4300, Limit 500 (10 rows @ 50 units/row), Permissions `Read-Only`.

### 2. Segment Register Switching
- Keys `1`, `2`, and `3` load the respective segment register.
- The head maintains its logical offset (row) in the target segment, while the snake body transitions along.
- If switching while $\text{offset} \ge \text{target\_segment.limit}$, a hardware fault is raised:
  `SEGFAULT: offset <off> >= limit <limit> of segment <id>`

### 3. Read-Only Protection
- **R Pellets (Cyan)**: Read requests. Collectible in all segments.
- **W Pellets (Yellow 'M')**: Write / Malloc requests. In read-only segment 3, they act as hazards. Touching a W pellet triggers:
  `PROTECTION FAULT: write to read-only segment 3`

### 4. Level Progression
- Level 1 advances to Level 2.
- Level 2 advances to Level 3.
- Level 3 is the terminal level: the "Next Level" button is hidden on the Level Clear card.

---

## JSON Level Guide

Levels are defined as external JSON files inside the `levels/` directory:
- `levels/level1.json`: Level 1 "Heap Limits"
- `levels/level2.json`: Level 2 "Growing Stack"
- `levels/level3.json`: Level 3 "Segment Switch"

### Level JSON Schema
```json
{
  "levelNumber": 2,
  "title": "GROWING STACK",
  "subtitle": "STACK EXPANDS DOWNWARDS",
  "gridCols": 12,
  "gridRows": 14,
  "rowUnits": 50,
  "baseAddress": 1400,
  "limit": 700,
  "speedMs": 110,
  "targetScore": 5,
  "timeLimitSec": 60,
  "stackEnabled": true,
  "stackGrowEverySec": 8.0,
  "returnCooldownSec": 4.0,
  "gapFrames": 6
}
```

### Adding or Modifying Levels
1. Place a valid `level<N>.json` file in `levels/`.
2. If any JSON file is absent, damaged, or fails schema validation, `SnakeLevels.java` automatically falls back to robust built-in defaults without crashing.

---

## How this maps to real segmentation

In hardware memory segmentation, programs do not see a flat physical memory space. Instead, memory is split into logical segments representing code, data, heap, and stack:
- Each segment has an independent **Base** and **Limit** stored in the Segment Table / Segment Descriptor registers.
- Moving between segments changes the base register, pointing the CPU to a different region in physical memory without changing the program's relative internal offsets.
- Hardware protection bits enforce read/write/execute permissions per segment, preventing unintended modification of read-only segments (such as constant tables or shared code).

### Caveat on Stack/Heap Growth
> In pure segmentation the heap and stack are separate segments that can sit anywhere in physical memory; the stack/heap "collision" in Level 2 models a segment that cannot grow because there is no free space next to it (external fragmentation), as in the classic process address-space layout.

---

## Known Environment Behavior (Not a Bug)
- **Windows Screenshot Overlay**: When the Windows screenshot overlay opens (via `PrtScn`, `Win+Shift+S`, or similar screen capture tools), a visual strip may appear over the taskbar area. This is a known Windows compositor behavior that occurs across multiple applications (including Google Chrome) and is not an issue with the simulator.

---

## Out of Scope (Future Work)
The following features are intentionally out of scope for this release and are planned for future versions:
- `BRK` and `FREE` dynamic heap expansion / deallocation pickups
- Dynamic segment growth and relocation (handling segments unable to grow due to external fragmentation)
- Interactive onboarding tutorial
- In-game custom setup menu
- Predict mode (address translation pre-calculation challenge)
