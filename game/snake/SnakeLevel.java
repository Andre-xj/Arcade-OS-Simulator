package game.snake;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * HEAP vs STACK SNAKE - one level description.
 *
 * A level owns nothing but numbers. The board, the snake, the stack and the
 * pickup spawner are all derived from these fields, so a level can be dropped in
 * without touching the model or the renderer. Addresses are plain byte addresses
 * into one flat address space: the board's bottom row is address 0 and the top
 * row is {@link #addressSpace}. One board row always spans {@link #rowUnits}
 * address units, so row = address / rowUnits.
 */
final class SnakeLevel {

    // ── Fixed neon palette, identical in the board, the HUD and the legend ──
    /** Code segment: gray/blue, drawn hatched because it is read-only. */
    static final Color C_CODE  = new Color(128, 152, 198);
    /** Data segment: yellow. */
    static final Color C_DATA  = new Color(255, 211, 25);
    /** Heap segment (the snake's arena): green. */
    static final Color C_HEAP  = new Color(57, 255, 20);
    /** Stack segment: magenta. */
    static final Color C_STACK = new Color(255, 42, 141);
    /** Free gap: dark, nearly the panel background. */
    static final Color C_FREE  = new Color(18, 15, 34);

    int number = 1;
    String name = "Heap Limits";

    int addressSpace = 1400;
    int rowUnits = 100;
    /** Board width in cells. 24 by default; a level may override it in its own data. */
    int cols = 24;

    /** Rows the fixed low segments take: CODE then DATA, one unit row each. */
    static final int CODE_ROWS = 2, DATA_ROWS = 2, ARENA_ROWS = 6, HEADROOM_ROWS = 4;

    int codeBase, codeLimit;
    int dataBase, dataLimit;
    int heapBase, heapLimit, brkStep;
    int stackTop;
    boolean stackEnabled;

    /**
     * PART 3: stack size in frames. The free gap above the heap top is sized to
     * hold exactly {@code gapFrames} frames, and one frame is {@link #frameRows}
     * rows of the arena's {@link #rowUnits}, so every level is winnable whatever
     * address space it is given.
     */
    int gapFrames = 0;
    int frameRows = 1;

    /**
     * When the arena was built from a segment in the Mission 05 segment table,
     * these hold that segment's real values so the gauge can show them. -1 means
     * "authored level, nothing to report".
     */
    int sourceBase = -1;
    int sourceLimit = -1;
    String sourceName = "";

    /** Milliseconds per snake step. */
    long moveMs = 150;

    // Stack pressure (stage 2+).
    long pushEveryMs = 8000;
    int pushRows = 1;
    int burstFrames = 0;
    int returnPops = 2;
    long returnCooldownMs = 3000;

    /** Read-only constants inside the arena: each entry is {base, limit}. */
    final List<int[]> readOnlyBlocks = new ArrayList<>();

    // ── TASK 3B: Segment Switch level configuration ──
    static final class SegmentDef {
        final int id;           // 1, 2, 3
        final String name;      // "Audio_Engine", "Arcade_Sprites", "High_Scores"
        final int base;         // 6300, 1400, 4300
        final int limit;        // 300, 400, 500
        final boolean readOnly; // true for segment 3
        final int startCol;     // (id - 1) * 8

        SegmentDef(int id, String name, int base, int limit, boolean readOnly) {
            this.id = id;
            this.name = name == null ? ("Seg " + id) : name;
            this.base = base;
            this.limit = limit;
            this.readOnly = readOnly;
            this.startCol = (id - 1) * 8;
        }
    }

    boolean segmentSwitchEnabled;
    final List<SegmentDef> segments = new ArrayList<>();
    boolean fromSelectedTable = false;
    static final List<SegmentDef> selectedTableRows = new ArrayList<>();

    /** How many pickups of each kind the board keeps topped up (stage 3). */
    int mallocSpawn = 3, brkSpawn = 0, freeSpawn = 0;
    /**
     * Milliseconds between MALLOC spawns. A value of 0 or less means "keep the
     * board topped up instantly", which is Level 1's original behaviour; a
     * positive value spawns one pellet at a time on that cadence (Level 2).
     */
    long mallocSpawnMs = 0;

    /** Exactly one of these is used; -1 means "not this goal". */
    int goalMallocs = -1;
    int goalSurviveSec = -1;

    /** Board rows, one per {@link #rowUnits} address units. */
    int rows() {
        return Math.max(2, addressSpace / rowUnits);
    }

    /** Highest address the heap may reach, exclusive. */
    int heapTop() {
        return heapBase + heapLimit;
    }

    /** Address units in one stack frame. */
    int frameUnits() {
        return Math.max(1, frameRows) * rowUnits;
    }

    /** Address units of free gap the level can hold - zero when there is no stack. */
    int gapUnits() {
        return stackEnabled ? Math.max(0, gapFrames) * frameUnits() : 0;
    }

    /**
     * Sizes the address space so the free gap above the heap top is exactly
     * {@code gapFrames} frames (stack levels) or {@link #HEADROOM_ROWS} rows
     * (levels without a stack), with CODE and DATA reserved at the bottom.
     */
    void deriveAddressSpace() {
        int arenaRows = Math.max(1, heapLimit / rowUnits);
        int aboveRows = stackEnabled ? Math.max(1, gapFrames) * Math.max(1, frameRows)
                : HEADROOM_ROWS;
        addressSpace = (CODE_ROWS + DATA_ROWS + arenaRows + aboveRows) * rowUnits;
        stackTop = addressSpace;
    }

    /** Writes the fixed low segments and a {@link #ARENA_ROWS}-row heap arena. */
    void layOutSegments() {
        layOutSegments(ARENA_ROWS);
    }

    /** Writes the fixed low segments and a heap arena of {@code arenaRows} rows. */
    void layOutSegments(int arenaRows) {
        codeBase = 0;                 codeLimit = CODE_ROWS * rowUnits;
        dataBase = codeLimit;         dataLimit = DATA_ROWS * rowUnits;
        heapBase = (CODE_ROWS + DATA_ROWS) * rowUnits;
        heapLimit = Math.max(1, arenaRows) * rowUnits;
        brkStep = 2 * rowUnits;
        deriveAddressSpace();
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /**
     * STEP 4: pick the row unit that turns {@code limit} into 6-10 playable arena
     * rows - the largest unit that fits. The enforced limit is always the chosen
     * unit times the row count; the caller keeps the real limit for the report.
     */
    static int chooseRowUnits(int limit) {
        int[] units = {100, 50, 25, 20, 10};
        for (int u : units) {
            int rows = limit / u;
            if (rows >= 6 && rows <= 10) return u;
        }
        // No unit lands in 6..10: fall back to the largest and let the caller clamp.
        return 100;
    }

    // =====================================================================
    // AUTHORED LEVELS
    // =====================================================================

    /**
     * Level 1 - Heap Limits. No stack, no pickups other than MALLOC: just the
     * base/limit rule against the heap arena. Built-in copy of levels/level1.json
     * so the game still has a level when the JSON cannot be read.
     */
    static SnakeLevel level1() {
        SnakeLevel l = new SnakeLevel();
        l.number = 1;
        l.name = "Heap Limits";
        l.rowUnits = 100;
        l.cols = 24;
        l.stackEnabled = false;
        l.layOutSegments();
        l.moveMs = 150;
        l.mallocSpawn = 3;
        l.mallocSpawnMs = 0;          // instant top-up: unchanged Level 1 behaviour
        l.goalMallocs = 10;
        return l;
    }

    /**
     * Level 2 - Growing Stack. Built-in copy of levels/level2.json: the stack
     * pushes a frame every 8 s and SPACE pops two, and the goal is to survive 60 s.
     */
    static SnakeLevel level2() {
        SnakeLevel l = new SnakeLevel();
        l.number = 2;
        l.name = "Growing Stack";
        l.rowUnits = 100;
        l.cols = 24;
        l.stackEnabled = true;
        l.gapFrames = 6;
        l.frameRows = 1;
        l.layOutSegments();
        l.moveMs = 140;
        l.pushEveryMs = 8000;
        l.pushRows = 1;
        l.returnPops = 2;
        l.returnCooldownMs = 4000;
        l.mallocSpawn = 3;
        l.mallocSpawnMs = 2500;
        l.goalSurviveSec = 60;
        return l;
    }

    /**
     * Level 3 - Segment Switch. Built-in copy of levels/level3.json:
     * Three segment panels side by side, keys 1-2-3 load segment register,
     * same offset checked against each segment's limit, R & W pellets with
     * read-only protection in segment 3. Goal is 12 pellets.
     */
    static SnakeLevel level3() {
        SnakeLevel l = new SnakeLevel();
        l.number = 3;
        l.name = "Segment Switch";
        l.segmentSwitchEnabled = true;
        l.stackEnabled = false;
        l.rowUnits = 50;
        l.cols = 24;
        l.fromSelectedTable = true;

        if (selectedTableRows.size() >= 3) {
            for (int i = 0; i < 3; i++) {
                SegmentDef src = selectedTableRows.get(i);
                l.segments.add(new SegmentDef(i + 1, src.name, src.base, src.limit, i == 2 ? true : src.readOnly));
            }
        } else {
            l.segments.add(new SegmentDef(1, "Audio_Engine", 6300, 300, false));
            l.segments.add(new SegmentDef(2, "Arcade_Sprites", 1400, 400, false));
            l.segments.add(new SegmentDef(3, "High_Scores", 4300, 500, true));
        }

        int maxLimit = 0;
        for (SegmentDef s : l.segments) {
            if (s.limit > maxLimit) maxLimit = s.limit;
        }
        int maxRows = Math.max(6, Math.min(10, maxLimit / l.rowUnits));
        l.addressSpace = maxRows * l.rowUnits;
        l.heapBase = 0;
        l.heapLimit = l.segments.get(0).limit;
        l.moveMs = 140;
        l.mallocSpawn = 4;
        l.mallocSpawnMs = 0;
        l.goalMallocs = 12;
        return l;
    }

    /**
     * Builds a compact level whose heap arena is sized from a segment picked in
     * the Mission 05 manual simulator.
     *
     * STEP 4: the arena height is chosen so it has 6-10 rows. The row unit is
     * picked from {100, 50, 25, 20, 10} - the largest that gives 6-10 rows - and
     * the enforced limit is {@code rows * unit}. The segment's real limit is kept
     * in {@link #sourceLimit}; when the two differ (no unit fit and the row count
     * had to be clamped) the gauge shows a "scaled from" note, so the effective
     * limit is never a silent change.
     *
     * The arena is deliberately NOT placed at the segment's real base: it sits in
     * a small, fixed board just above the Data segment so any base stays playable.
     *
     * @param name   segment name, shown in the gauge
     * @param base   the segment's real base address (informational only)
     * @param limit  the segment's real size in bytes
     */
    static SnakeLevel fromSegment(String name, int base, int limit) {
        SnakeLevel l = new SnakeLevel();
        l.number = 1;
        l.name = "Heap Limits";
        l.cols = 24;

        int unit = chooseRowUnits(limit);
        int arenaRows = clamp(limit / unit, 6, 10);
        l.rowUnits = unit;

        // Two code rows + two data rows + the arena + four rows of free headroom,
        // so stepping past the limit is a visible fault rather than an instant edge.
        l.stackEnabled = false;
        l.layOutSegments(arenaRows);

        l.moveMs = 150;
        l.mallocSpawn = 3;
        l.goalMallocs = 10;

        l.sourceBase = base;
        l.sourceLimit = limit;
        l.sourceName = name == null ? "" : name;
        return l;
    }

    /**
     * Every level the game knows about. PART 2: the campaign now comes from the
     * JSON level files via {@link SnakeLevels}; the built-in levels are only the
     * fallback when those files are missing or unusable.
     */
    static List<SnakeLevel> library() {
        return SnakeLevels.library();
    }

    /**
     * PART 1.1: the level after {@code cur} in the campaign order, or {@code null}
     * when the run just finished the last authored level. Compared by number, so
     * a level rebuilt from JSON matches the library entry of the same index.
     */
    static SnakeLevel next(SnakeLevel cur) {
        if (cur == null) return null;
        List<SnakeLevel> lib = library();
        for (int i = 0; i + 1 < lib.size(); i++) {
            if (lib.get(i).number == cur.number) return lib.get(i + 1);
        }
        return null;
    }
}
