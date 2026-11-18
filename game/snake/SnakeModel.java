package game.snake;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * HEAP vs STACK SNAKE - model and rules. No Swing, no drawing.
 *
 * The board is the whole process address space: column / row map to a byte
 * address through {@link #address(int, int)}. The snake is the heap head; every
 * legality check reads the live base/limit fields, so moving the limit (BRK in a
 * later stage, stack pushes in a later stage) changes the verdict immediately.
 */
final class SnakeModel {

    /** Everything that can end badly, kept for the end-screen review. */
    enum Fault { SEGFAULT, HEAP_CORRUPTION, PROTECTION_FAULT, COLLISION }

    enum Phase { PLAYING, LEVEL_CLEAR, GAME_OVER }

    static final int START_HEARTS = 3;
    static final int MIN_LENGTH = 3;
    static final double INVULN_MS = 1500;
    static final int MALLOC_POINTS = 10;
    /** PART 3 scoring: +100 for every whole 10 s survived, +25 per remaining gap frame. */
    static final int TIME_BONUS_POINTS = 100;
    static final long TIME_BONUS_MS = 10_000;
    static final int GAP_BONUS_PER_FRAME = 25;
    /** PART 3: frames the stack backs off after a collision. */
    static final int COLLISION_RETREAT_FRAMES = 2;

    /** PART 1.1: the level can change at run time (Next Level), so it is not final. */
    SnakeLevel level;

    int cols;
    int rows;
    /** Live heap bounds. The limit is a size, so the heap covers [base, base+limit). */
    int heapBase;
    int heapLimit;

    Phase phase = Phase.PLAYING;
    boolean paused = false;
    int hearts = START_HEARTS;
    int score = 0;
    int mallocsEaten = 0;
    int longest = MIN_LENGTH;
    long surviveMs = 0;

    // ── PART 3: the stack (level 2). All frame counts are in units of
    //    level.frameUnits(); stackFrames 0 means "nothing pushed yet". ──
    /** Frames currently pushed. The frontier is stackTop minus this many frames. */
    int stackFrames = 0;
    /** Milliseconds left before SPACE is accepted again. */
    double returnCdMs = 0;
    /** Points banked for surviving whole 10 s marks (see {@link #advance}). */
    private int timeBonusMarks = 0;
    private double pushAccMs = 0;
    private double mallocAccMs = 0;
    /** How many frames the last stack event moved, for the log / render (signed). */
    int lastStackDelta = 0;

    /** Damage-free window after a respawn, in milliseconds. */
    double invulnMs = 0;
    /** Screen-shake magnitude the renderer reads and decays. */
    int shake = 0;
    /** Red flash left over from the last fault, in milliseconds. */
    double faultFlashMs = 0;

    /** Head is element 0. Each cell is {col, row}. */
    final Deque<int[]> body = new ArrayDeque<>();
    final List<SnakePickup> pickups = new ArrayList<>();
    final List<Fault> mistakes = new ArrayList<>();

    // ── TASK 2: end-screen data ──────────────────────────────────────────
    /** The run-ending fault and the numbers it happened at, for the cause line. */
    Fault lastFault;
    int lastFaultOffset;
    int lastFaultLimit;
    int lastFaultBase;
    int lastFaultSegmentId = 0;
    /** Active segment index (0, 1, 2) for Level 3 segment switching. */
    int activeSegmentIndex = 0;
    /** Smallest free gap (heap top to stack top) seen this run. */
    int smallestFreeGap;

    // ── TASK D: Game feel fields ─────────────────────────────────────────
    /** D1: True if the most recently collected pickup was in the top 20% limit danger zone. */
    boolean lastPickupWasEdge = false;
    /** D3: Tracks consecutive pellets eaten to accelerate move speed every 3 pellets. */
    int pelletsForSpeed = 0;

    private final Deque<int[]> pendingDirs = new ArrayDeque<>();
    private final Random rng;
    private int dirX = 1, dirY = 0;
    private int growPending = 0;
    private double accMs = 0;

    SnakeModel(SnakeLevel level) {
        this.level = level;
        this.cols = level.cols;
        this.rows = level.rows();
        this.rng = new Random();
        reset();
    }

    /**
     * PART 1.1: switches the run to another level and starts it fresh - used by the
     * "Next Level" button when the campaign moves on. Everything derived from the
     * level (board size, addresses) is rebuilt, so the panel only has to clear its
     * own view state afterwards.
     */
    void setLevel(SnakeLevel lv) {
        if (lv == null) return;
        this.level = lv;
        this.cols = lv.cols;
        this.rows = lv.rows();
        reset();
    }

    // =====================================================================
    // LIFECYCLE
    // =====================================================================
    void reset() {
        heapBase = level.heapBase;
        heapLimit = level.heapLimit;

        phase = Phase.PLAYING;
        paused = false;
        hearts = START_HEARTS;
        score = 0;
        mallocsEaten = 0;
        longest = MIN_LENGTH;
        surviveMs = 0;
        invulnMs = 0;
        shake = 0;
        faultFlashMs = 0;
        accMs = 0;
        growPending = 0;
        pendingDirs.clear();
        mistakes.clear();
        pickups.clear();
        activeSegmentIndex = 0;
        lastFaultSegmentId = 0;
        lastFault = null;
        lastFaultOffset = 0;
        lastFaultLimit = currentSegmentLimit();
        lastFaultBase = currentSegmentBase();
        smallestFreeGap = freeGap();
        lastPickupWasEdge = false;
        pelletsForSpeed = 0;

        // PART 3: the stack starts empty, the return key is ready, and the timers
        // are zeroed. The spawner starts empty on levels that spawn over time.
        stackFrames = 0;
        returnCdMs = 0;
        timeBonusMarks = 0;
        pushAccMs = 0;
        mallocAccMs = 0;
        lastStackDelta = 0;

        respawn();
        if (isSegmentSwitchLevel()) {
            spawnInitialPickupsLevel3();
        } else if (level.mallocSpawnMs > 0) {
            // A timed level builds its pellets up over time (Level 2).
            mallocAccMs = level.mallocSpawnMs;
        } else {
            for (int i = 0; i < level.mallocSpawn; i++) spawnPickup();
        }
    }

    /** Advances the fixed-timestep loop by {@code dtMs}; the caller clamps it. */
    void advance(double dtMs) {
        if (faultFlashMs > 0) faultFlashMs = Math.max(0, faultFlashMs - dtMs);
        if (shake > 0) {
            shake -= Math.max(1, (int) Math.round(dtMs / 16.0));
            if (shake < 0) shake = 0;
        }
        if (phase != Phase.PLAYING || paused) return;

        if (invulnMs > 0) invulnMs = Math.max(0, invulnMs - dtMs);
        if (returnCdMs > 0) returnCdMs = Math.max(0, returnCdMs - dtMs);
        surviveMs += (long) dtMs;

        // PART 3: +100 for every whole 10 s survived.
        int marks = (int) (surviveMs / TIME_BONUS_MS);
        while (timeBonusMarks < marks) {
            timeBonusMarks++;
            score += TIME_BONUS_POINTS;
        }

        // PART 3: the timed MALLOC spawner and the stack push / collision cycle.
        updateMallocSpawn(dtMs);
        updateStack(dtMs);

        int gap = freeGap();
        if (gap < smallestFreeGap) smallestFreeGap = gap;

        double step = Math.max(40, level.moveMs);
        accMs += dtMs;
        int guard = 0;
        while (accMs >= step && phase == Phase.PLAYING && guard++ < 8) {
            accMs -= step;
            stepOnce();
        }
        if (phase == Phase.PLAYING) checkGoal();
    }

    /** PART 3: spawns one MALLOC every {@code level.mallocSpawnMs} up to the cap. */
    private void updateMallocSpawn(double dtMs) {
        if (level.mallocSpawnMs <= 0) return;
        mallocAccMs += dtMs;
        while (mallocAccMs >= level.mallocSpawnMs && phase == Phase.PLAYING) {
            mallocAccMs -= level.mallocSpawnMs;
            if (pickups.size() < level.mallocSpawn) spawnPickup();
        }
    }

    /**
     * PART 3: pushes frames on the level's cadence and turns a full gap into a
     * COLLISION. Each push removes one frame of real gap; a collision costs a
     * heart, retreats the stack by two frames, and grants the usual invulnerable
     * window. Levels without a stack do nothing here.
     */
    private void updateStack(double dtMs) {
        if (!level.stackEnabled) return;
        pushAccMs += dtMs;
        while (pushAccMs >= level.pushEveryMs && phase == Phase.PLAYING) {
            pushAccMs -= level.pushEveryMs;
            pushFrames(level.pushRows, false);
        }
    }

    // =====================================================================
    // INPUT
    // =====================================================================
    /** Buffers a heading change. Rejects 180-degree turns and presses past two ahead. */
    boolean queueDir(int dx, int dy) {
        if (dx == 0 && dy == 0) return false;
        int lx = dirX, ly = dirY;
        if (!pendingDirs.isEmpty()) {
            int[] last = pendingDirs.peekLast();
            lx = last[0];
            ly = last[1];
        }
        if (dx == -lx && dy == -ly) return false;   // no reversing into ourselves
        if (dx == lx && dy == ly) return false;     // already going that way
        if (pendingDirs.size() >= 2) return false;
        pendingDirs.addLast(new int[]{dx, dy});
        return true;
    }

    // =====================================================================
    // BOARD MATHS
    // =====================================================================
    /** Byte address of the top-left corner of a cell. */
    int address(int col, int row) {
        double cellUnits = level.rowUnits / (double) cols;
        return (int) Math.round(row * (double) level.rowUnits + col * cellUnits);
    }

    /** First board row that lies fully inside the heap. */
    int arenaRowLo() {
        int lo = (int) Math.ceil(heapBase / (double) level.rowUnits);
        return clamp(lo, 0, rows - 1);
    }

    /** Last board row that lies fully inside the heap. */
    int arenaRowHi() {
        int hi = (int) Math.ceil((heapBase + heapLimit) / (double) level.rowUnits) - 1;
        return clamp(hi, 0, rows - 1);
    }

    int heapTop() {
        return heapBase + heapLimit;
    }

    boolean isSegmentSwitchLevel() {
        return level != null && level.segmentSwitchEnabled;
    }

    SnakeLevel.SegmentDef activeSegment() {
        if (!isSegmentSwitchLevel() || level.segments.isEmpty()) return null;
        int idx = clamp(activeSegmentIndex, 0, level.segments.size() - 1);
        return level.segments.get(idx);
    }

    int currentSegmentBase() {
        if (isSegmentSwitchLevel()) {
            SnakeLevel.SegmentDef seg = activeSegment();
            return seg != null ? seg.base : 0;
        }
        return level.sourceBase >= 0 ? level.sourceBase : heapBase;
    }

    int currentSegmentLimit() {
        if (isSegmentSwitchLevel()) {
            SnakeLevel.SegmentDef seg = activeSegment();
            return seg != null ? seg.limit : heapLimit;
        }
        return heapLimit;
    }

    int headAddress() {
        int[] h = body.peekFirst();
        return h == null ? heapBase : address(h[0], h[1]);
    }

    /** Head offset inside the heap / active segment - what the HUD gauge shows. */
    int headOffset() {
        int[] h = body.peekFirst();
        if (h == null) return 0;
        if (isSegmentSwitchLevel()) {
            return h[1] * level.rowUnits;
        }
        return headAddress() - heapBase;
    }

    /** Real Physical Address = Segment Base + Offset. */
    int headPhysicalAddress() {
        return currentSegmentBase() + headOffset();
    }

    /** D4: True when the head is near the segment limit or bottom boundary (danger zone). */
    boolean headNearLimit() {
        int off = headOffset();
        int lim = currentSegmentLimit();
        return off >= lim - level.rowUnits || off < level.rowUnits;
    }

    /**
     * TASK 3B: Segment register switch (keys 1, 2, 3).
     * The head keeps its offset (row) in the target segment; the body re-lays out.
     * If that offset >= target segment limit, it causes a SEGFAULT.
     */
    boolean switchSegment(int targetNum) {
        if (!isSegmentSwitchLevel() || phase != Phase.PLAYING || paused) return false;
        int targetIdx = targetNum - 1;
        if (targetIdx < 0 || targetIdx >= level.segments.size()) return false;
        if (targetIdx == activeSegmentIndex) return false;

        SnakeLevel.SegmentDef targetSeg = level.segments.get(targetIdx);
        int currentOffset = headOffset();

        if (currentOffset >= targetSeg.limit) {
            mistakes.add(Fault.SEGFAULT);
            lastFault = Fault.SEGFAULT;
            lastFaultOffset = currentOffset;
            lastFaultLimit = targetSeg.limit;
            lastFaultBase = targetSeg.base;
            lastFaultSegmentId = targetSeg.id;
            hearts--;
            shake = 9;
            faultFlashMs = 320;
            GameSfx.fault();
            if (hearts <= 0) {
                hearts = 0;
                phase = Phase.GAME_OVER;
                return true;
            }
            respawn();
            invulnMs = INVULN_MS;
            return true;
        }

        activeSegmentIndex = targetIdx;
        int startCol = targetSeg.startCol;
        int[] oldHead = body.peekFirst();
        int oldCol = oldHead != null ? oldHead[0] : 0;
        int oldRow = oldHead != null ? oldHead[1] : 0;
        int newHeadCol = startCol + (oldCol % 8);

        body.clear();
        int segMaxRow = (targetSeg.limit / level.rowUnits) - 1;
        for (int i = 0; i < longest; i++) {
            int c = startCol + ((newHeadCol - startCol - i * dirX) % 8 + 8) % 8;
            int r = clamp(oldRow - i * dirY, 0, Math.max(0, segMaxRow));
            body.addLast(new int[]{c, r});
        }
        GameSfx.whoosh();
        return true;
    }

    /** True when the head is outside [base, base+limit) right now. */
    boolean headOutOfBounds() {
        int a = headAddress();
        return a < heapBase || a >= heapTop();
    }

    /** Bytes between the heap top and the live stack frontier - the room before a collision. */
    int freeGap() {
        return Math.max(0, stackFrontier() - heapTop());
    }

    // =====================================================================
    // PART 3 - THE STACK
    // =====================================================================
    /** Address of the live stack frontier: the top of the space minus the pushed frames. */
    int stackFrontier() {
        if (!level.stackEnabled) return level.stackTop;
        return Math.max(heapTop(), level.stackTop - stackFrames * level.frameUnits());
    }

    /** Real gap measured in whole stack frames - what the FREE GAP meter shows. */
    int freeGapFrames() {
        if (!level.stackEnabled || level.frameUnits() <= 0) return 0;
        return freeGap() / level.frameUnits();
    }

    /** True while SPACE is still cooling down after a return. */
    boolean canReturn() {
        return level.stackEnabled && returnCdMs <= 0 && phase == Phase.PLAYING;
    }

    /**
     * PART 3: SPACE. Pops {@code level.returnPops} frames (a whoosh) and starts the
     * cooldown. Returns the number of frames actually popped - zero when the key is
     * refused because the cooldown is still running or the stack is already empty.
     */
    int returnFrames() {
        if (!canReturn()) return 0;
        int pop = Math.min(level.returnPops, stackFrames);
        if (pop <= 0) {
            // Nothing to pop, but still respect the cooldown so the key cannot spam.
            returnCdMs = level.returnCooldownMs;
            return 0;
        }
        stackFrames -= pop;
        lastStackDelta = -pop;
        returnCdMs = level.returnCooldownMs;
        GameSfx.whoosh();
        return pop;
    }

    /**
     * PART 3: pushes {@code n} frames. Unless {@code quiet}, a push thuds and shakes
     * the screen. When the gap reaches zero the heap has met the stack: a COLLISION
     * costs a heart, retreats the stack by two frames and grants invulnerability.
     */
    void pushFrames(int n, boolean quiet) {
        if (!level.stackEnabled || n <= 0) return;
        stackFrames += n;
        lastStackDelta = n;
        if (!quiet) {
            GameSfx.thud();
            shake = Math.max(shake, 4);
        }
        if (freeGapFrames() <= 0) collide();
    }

    /** PART 3: the heap met the stack. */
    private void collide() {
        mistakes.add(Fault.COLLISION);
        lastFault = Fault.COLLISION;
        lastFaultOffset = heapTop() - heapBase;      // heap top, for the review bars
        lastFaultLimit = level.stackTop - heapBase;  // stack top, measured from base
        lastFaultBase = heapBase;
        hearts--;
        shake = Math.max(shake, 9);
        faultFlashMs = 320;
        GameSfx.fault();
        // Retreat two frames so the player has room to react.
        stackFrames = Math.max(0, stackFrames - COLLISION_RETREAT_FRAMES);
        lastStackDelta = -COLLISION_RETREAT_FRAMES;
        if (hearts <= 0) {
            hearts = 0;
            phase = Phase.GAME_OVER;
            return;
        }
        respawn();
        invulnMs = INVULN_MS;
    }

    // =====================================================================
    // TASK 2 - END REPORT
    // =====================================================================
    /** How many times one fault kind was suffered this run. */
    int faultCount(Fault f) {
        int n = 0;
        for (Fault m : mistakes) if (m == f) n++;
        return n;
    }

    /** The fault the player made most often, or {@code null} when none was made. */
    Fault mostCommonFault() {
        Fault best = null;
        int bestN = 0;
        for (Fault f : Fault.values()) {
            int n = faultCount(f);
            if (n > bestN) { bestN = n; best = f; }
        }
        return best;
    }

    /**
     * S..D for the end report.
     * A6: rank by fault count so it is easy to understand and not inflated by time.
     * S: no faults + under time target (or no time target); A: ≤1 fault;
     * B: ≤2 faults; C: ≤4 faults; D: 5+ faults.
     */
    String rank() {
        int faults = mistakes.size();
        boolean underTime = level.goalSurviveSec <= 0 || surviveMs < level.goalSurviveSec * 1000L;
        if (faults == 0 && underTime) return "S";
        if (faults <= 1) return "A";
        if (faults <= 2) return "B";
        if (faults <= 4) return "C";
        return "D";
    }

    /** Uppercase name of a fault, shared by the kernel log and the cause line. */
    static String faultName(Fault f) {
        switch (f) {
            case SEGFAULT:         return "SEGFAULT";
            case HEAP_CORRUPTION:  return "HEAP CORRUPTION";
            case PROTECTION_FAULT: return "PROTECTION FAULT";
            case COLLISION:        return "COLLISION";
            default:               return "FAULT";
        }
    }

    /**
     * PART 1.3 & TASK 3B: why a fault happened, naming the boundary that was crossed and the
     * attempted offset. A segfault says TOP or BASE and the offset that fell
     * outside, so it can never read as a valid "offset=6 limit=400".
     * In Level 3 segment switches, names the target segment limit.
     */
    static String faultDetail(Fault f, int offset, int limit, int segmentId) {
        if (f == null) return "";
        switch (f) {
            case SEGFAULT:
                if (segmentId > 0) {
                    return "offset " + offset + " >= limit " + limit + " of segment " + segmentId;
                }
                if (offset >= limit) {
                    return "head left through the TOP (offset " + offset
                            + " >= limit " + limit + ")";
                }
                return "head left through the BASE (offset " + offset + " < 0)";
            case HEAP_CORRUPTION:
                return "head hit its own body";
            case PROTECTION_FAULT:
                if (segmentId > 0) {
                    return "write to read-only segment " + segmentId;
                }
                return "touched a read-only block";
            case COLLISION:
                return "heap met stack (no free space left to grow)";
            default:
                return "";
        }
    }

    static String faultDetail(Fault f, int offset, int limit) {
        return faultDetail(f, offset, limit, 0);
    }

    /** The detail for the last fault, using its recorded offset, limit, and segment. */
    String faultDetail() {
        return lastFault == null ? "" : faultDetail(lastFault, lastFaultOffset, lastFaultLimit, lastFaultSegmentId);
    }

    /** One short line naming how the run ended, for the top of the end screen. */
    String causeLine() {
        if (lastFault == null) return "GAME OVER";
        return faultName(lastFault) + ": " + faultDetail();
    }

    // =====================================================================
    // STEP
    // =====================================================================
    private void stepOnce() {
        if (!pendingDirs.isEmpty()) {
            int[] d = pendingDirs.poll();
            dirX = d[0];
            dirY = d[1];
        }

        int[] head = body.peekFirst();
        int nc = head[0] + dirX;
        int nr = head[1] + dirY;

        if (isSegmentSwitchLevel()) {
            SnakeLevel.SegmentDef seg = activeSegment();
            int segStart = seg != null ? seg.startCol : 0;
            int segEnd = segStart + 8;

            // Wrap left/right inside active 8-column panel
            if (nc < segStart) nc = segEnd - 1;
            else if (nc >= segEnd) nc = segStart;

            int segLimitRows = seg != null ? (seg.limit / level.rowUnits) : rows;
            boolean offBottom = nr < 0;
            boolean offTop = nr >= segLimitRows;

            if (invulnMs <= 0) {
                if (offBottom) {
                    lastFaultSegmentId = 0;
                    fault(Fault.SEGFAULT, -1);
                    return;
                }
                if (offTop) {
                    lastFaultSegmentId = 0;
                    fault(Fault.SEGFAULT, nr * level.rowUnits);
                    return;
                }
                if (hitsBody(nc, nr)) {
                    lastFaultSegmentId = 0;
                    fault(Fault.HEAP_CORRUPTION);
                    return;
                }
            }

            body.addFirst(new int[]{nc, nr});
            if (growPending > 0) growPending--;
            else body.pollLast();
            if (body.size() > longest) longest = body.size();

            SnakePickup p = pickupAt(nc, nr);
            if (p != null) eat(p);
            return;
        }

        // Left and right edges wrap; the address space has a hard top and bottom.
        if (nc < 0) nc = cols - 1;
        else if (nc >= cols) nc = 0;

        boolean offBoard = nr < 0 || nr >= rows;
        int addr = address(nc, nr);              // the address the head tried to enter
        boolean outside = offBoard || addr < heapBase || addr >= heapTop();

        if (invulnMs <= 0) {
            if (outside) { lastFaultSegmentId = 0; fault(Fault.SEGFAULT, addr - heapBase); return; }
            if (hitsBody(nc, nr)) { lastFaultSegmentId = 0; fault(Fault.HEAP_CORRUPTION); return; }
        }

        body.addFirst(new int[]{nc, nr});
        if (growPending > 0) growPending--;
        else body.pollLast();
        if (body.size() > longest) longest = body.size();

        if (!outside) {
            SnakePickup p = pickupAt(nc, nr);
            if (p != null) eat(p);
        }
    }

    /** True when the given cell is occupied, ignoring the tail that is about to vacate. */
    private boolean hitsBody(int c, int r) {
        int n = body.size();
        int i = 0;
        for (int[] cell : body) {
            if (cell[0] == c && cell[1] == r) {
                if (i == n - 1 && growPending == 0) return false;
                return true;
            }
            i++;
        }
        return false;
    }

    private void fault(Fault f) { fault(f, headOffset()); }

    private void fault(Fault f, int atOffset) {
        mistakes.add(f);
        lastFault = f;
        lastFaultOffset = atOffset;
        lastFaultLimit = currentSegmentLimit();
        lastFaultBase = currentSegmentBase();
        hearts--;
        shake = 9;
        faultFlashMs = 320;
        GameSfx.fault();
        if (hearts <= 0) {
            hearts = 0;
            phase = Phase.GAME_OVER;
            return;
        }
        respawn();
        invulnMs = INVULN_MS;
    }

    /** Puts a length-3 snake back at the heap base, heading right. */
    private void respawn() {
        body.clear();
        if (isSegmentSwitchLevel()) {
            SnakeLevel.SegmentDef seg = activeSegment();
            int startCol = seg != null ? seg.startCol : 0;
            int head = startCol + 3;
            int row = 1;
            for (int i = 0; i < MIN_LENGTH; i++) body.addLast(new int[]{head - i, row});
            dirX = 1;
            dirY = 0;
            pendingDirs.clear();
            growPending = 0;
            return;
        }
        int row = arenaRowLo();
        int head = Math.min(cols - 1, MIN_LENGTH);
        for (int i = 0; i < MIN_LENGTH; i++) body.addLast(new int[]{head - i, row});
        dirX = 1;
        dirY = 0;
        pendingDirs.clear();
        growPending = 0;
    }

    // =====================================================================
    // PICKUPS
    // =====================================================================
    private SnakePickup pickupAt(int c, int r) {
        for (SnakePickup p : pickups) {
            if (p.col == c && p.row == r) return p;
        }
        return null;
    }

    private void eat(SnakePickup p) {
        if (isSegmentSwitchLevel()) {
            SnakeLevel.SegmentDef seg = activeSegment();
            if (p.type == SnakePickup.Type.MALLOC && seg != null && seg.readOnly) {
                // Touching W pellet in read-only segment is a PROTECTION FAULT!
                if (invulnMs <= 0) {
                    lastFaultSegmentId = seg.id;
                    fault(Fault.PROTECTION_FAULT, headOffset());
                }
                return;
            }
        }
        pickups.remove(p);
        mallocsEaten++;

        // D1: Edge bonus — collecting near the limit (>80% limit) awards double points
        int curLimit = currentSegmentLimit();
        if (curLimit > 0 && headOffset() >= (int) (0.8 * curLimit)) {
            lastPickupWasEdge = true;
            score += MALLOC_POINTS * 2;
        } else {
            lastPickupWasEdge = false;
            score += MALLOC_POINTS;
        }

        // D3: Speed ramp — every 3 pellets slightly accelerate the game (up to floor 60ms)
        pelletsForSpeed++;
        if (pelletsForSpeed % 3 == 0) {
            level.moveMs = Math.max(60, (long) (level.moveMs * 0.95));
        }

        growPending++;
        GameSfx.hit();
        spawnPickup();
    }

    /** Tops the board back up with pickups inside the arena. */
    private void spawnPickup() {
        if (isSegmentSwitchLevel()) {
            if (level.segments.isEmpty()) return;
            for (int attempt = 0; attempt < 100; attempt++) {
                int segIdx = rng.nextInt(level.segments.size());
                SnakeLevel.SegmentDef seg = level.segments.get(segIdx);
                int segRows = seg.limit / level.rowUnits;
                int c = seg.startCol + rng.nextInt(8);
                int r = rng.nextInt(segRows);
                if (occupied(c, r)) continue;

                SnakePickup.Type type = SnakePickup.Type.READ;
                if (!seg.readOnly) {
                    type = rng.nextBoolean() ? SnakePickup.Type.MALLOC : SnakePickup.Type.READ;
                } else {
                    boolean hasHazard = false;
                    for (SnakePickup ex : pickups) {
                        if (ex.type == SnakePickup.Type.MALLOC && ex.col >= 16) hasHazard = true;
                    }
                    type = (!hasHazard && rng.nextInt(3) == 0) ? SnakePickup.Type.MALLOC : SnakePickup.Type.READ;
                }
                pickups.add(new SnakePickup(type, c, r));
                return;
            }
            return;
        }
        int lo = arenaRowLo();
        int hi = arenaRowHi();
        int span = Math.max(1, hi - lo + 1);
        for (int attempt = 0; attempt < 80; attempt++) {
            int c = rng.nextInt(cols);
            int r = lo + rng.nextInt(span);
            if (occupied(c, r)) continue;
            pickups.add(new SnakePickup(SnakePickup.Type.MALLOC, c, r));
            return;
        }
    }

    /** Places balanced initial pellets for Level 3. */
    private void spawnInitialPickupsLevel3() {
        pickups.clear();
        // Pellet 1: R pellet in Seg 2 at (11, 2) [offset 100: valid for both Seg 1 & 2]
        pickups.add(new SnakePickup(SnakePickup.Type.READ, 11, 2));
        // Pellet 2: R pellet in Seg 3 at (19, 3) [offset 150: valid for both Seg 2 & 3]
        pickups.add(new SnakePickup(SnakePickup.Type.READ, 19, 3));
        // Pellet 3: R pellet in Seg 3 at (20, 8) [offset 400: high up, must move down before switching to Seg 1]
        pickups.add(new SnakePickup(SnakePickup.Type.READ, 20, 8));
        // Pellet 4: W pellet in Seg 1 at (4, 2) [offset 100]
        pickups.add(new SnakePickup(SnakePickup.Type.MALLOC, 4, 2));
        // Hazard W pellet in Seg 3 at (18, 5)
        pickups.add(new SnakePickup(SnakePickup.Type.MALLOC, 18, 5));
    }

    private boolean occupied(int c, int r) {
        for (int[] cell : body) if (cell[0] == c && cell[1] == r) return true;
        for (SnakePickup p : pickups) if (p.col == c && p.row == r) return true;
        return false;
    }

    // =====================================================================
    // GOAL
    // =====================================================================
    private void checkGoal() {
        if (level.goalMallocs > 0 && mallocsEaten >= level.goalMallocs) {
            complete();
        } else if (level.goalSurviveSec > 0 && surviveMs >= level.goalSurviveSec * 1000L) {
            complete();
        }
    }

    /**
     * PART 3: the run is won. A stacked level pays a final bonus for the gap left
     * at the end, so keeping the stack low is worth playing for.
     */
    private void complete() {
        if (level.stackEnabled) score += freeGapFrames() * GAP_BONUS_PER_FRAME;
        phase = Phase.LEVEL_CLEAR;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
