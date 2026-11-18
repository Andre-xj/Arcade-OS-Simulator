package game.snake;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HEAP vs STACK SNAKE - the JSON level library.
 *
 * PART 2: the campaign is data, not code. Level {@code n} is read from
 * {@code level<n>.json}, looked for in this order:
 *
 * <ol>
 *   <li>the packaged resource {@code /game/snake/levels/level<n>.json};</li>
 *   <li>the file {@code game/snake/levels/level<n>.json} beside the working
 *       directory (the game's own resource folder when the build does not
 *       package resources);</li>
 *   <li>the relative {@code levels/level<n>.json} fallback directory.</li>
 * </ol>
 *
 * A file that cannot be read, parsed or validated never crashes the game: the
 * reason is collected in {@link #lastErrors()} (and printed once) and the
 * built-in level for that index is used instead. When even the built-in table
 * runs out, the campaign stops. The library is cached so a run reads each file
 * once, and the same instances are handed back every time so a level can be
 * identified by object or by number.
 */
final class SnakeLevels {

    /** Highest level index the loader will look for. */
    private static final int MAX_LEVEL = 9;

    private static List<SnakeLevel> cache;
    private static final List<String> errors = new ArrayList<>();
    private static boolean announced;

    private SnakeLevels() { }

    // =====================================================================
    // LIBRARY
    // =====================================================================
    /** The whole campaign, loaded once from JSON with built-in fallbacks. */
    static synchronized List<SnakeLevel> library() {
        if (cache == null) {
            cache = load();
            if (!announced) {
                announced = true;
                for (String e : errors) System.out.println("SNAKE LEVELS: " + e);
                StringBuilder sb = new StringBuilder();
                for (SnakeLevel l : cache) {
                    sb.append(' ').append(l.number).append('=').append(l.name)
                      .append("(rows=").append(l.rows())
                      .append(",stack=").append(l.stackEnabled).append(')');
                }
                System.out.println("SNAKE LEVELS loaded" + sb);
            }
        }
        return cache;
    }

    /** Why the last load fell back, one line per problem; empty when all was well. */
    static synchronized List<String> lastErrors() {
        library();
        return new ArrayList<>(errors);
    }

    /** Only for tests: forget the cache so the files are read again. */
    static synchronized void reload() {
        cache = null;
        errors.clear();
        announced = false;
    }

    private static List<SnakeLevel> load() {
        List<SnakeLevel> out = new ArrayList<>();
        for (int n = 1; n <= MAX_LEVEL; n++) {
            String text = readLevelFile(n);
            SnakeLevel builtin = builtin(n);
            if (text == null && builtin == null) break;      // nothing more authored

            SnakeLevel lv = null;
            if (text != null) {
                try {
                    lv = fromJson(n, text);
                } catch (RuntimeException ex) {
                    errors.add("level " + n + " (" + sourceOf(n) + "): " + ex.getMessage()
                            + " - falling back to the built-in Level " + n);
                }
            } else {
                errors.add("level " + n + ": no level" + n + ".json found - using the built-in Level " + n);
            }
            if (lv == null) lv = builtin;
            if (lv == null) break;
            out.add(lv);
        }
        if (out.isEmpty()) {
            out.add(SnakeLevel.level1());
            errors.add("no level files found - using the built-in Level 1");
        }
        return out;
    }

    /** The built-in definition for an index, or {@code null} when there is none. */
    private static SnakeLevel builtin(int n) {
        if (n == 1) return SnakeLevel.level1();
        if (n == 2) return SnakeLevel.level2();
        if (n == 3) return SnakeLevel.level3();
        return null;
    }

    // =====================================================================
    // FILE LOOKUP
    // =====================================================================
    private static String readLevelFile(int n) {
        String name = "level" + n + ".json";
        // 1. packaged resource
        try (InputStream in = SnakeLevels.class.getResourceAsStream("/game/snake/levels/" + name)) {
            if (in != null) return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
        // 2. / 3. files beside the working directory
        for (String dir : new String[]{"game/snake/levels", "levels"}) {
            File f = new File(dir, name);
            if (f.isFile()) {
                try {
                    return Files.readString(f.toPath(), StandardCharsets.UTF_8);
                } catch (Exception ignored) { }
            }
        }
        return null;
    }

    private static String sourceOf(int n) {
        String name = "level" + n + ".json";
        for (String dir : new String[]{"game/snake/levels", "levels"}) {
            File f = new File(dir, name);
            if (f.isFile()) return f.getPath();
        }
        return "classpath:" + name;
    }

    // =====================================================================
    // JSON -> LEVEL
    // =====================================================================
    /**
     * Parses and validates one level file. Throws {@link IllegalArgumentException}
     * with a short reason whenever a field is missing, out of range or the file
     * does not describe a playable level.
     */
    static SnakeLevel fromJson(int expectedNumber, String text) {
        Map<String, Object> root = SnakeJson.obj(SnakeJson.parse(text));

        int level = SnakeJson.intg(root, "level", expectedNumber);
        if (level != expectedNumber) {
            throw new IllegalArgumentException("\"level\" is " + level + ", expected " + expectedNumber);
        }
        String name = SnakeJson.str(root, "name", null);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("\"name\" is missing");
        }
        int cols = SnakeJson.intg(root, "cols", 0);
        if (cols < 8 || cols > 48) {
            throw new IllegalArgumentException("\"cols\" is " + cols + " (expected 8..48)");
        }
        long moveMs = SnakeJson.intg(root, "moveMs", 0);
        if (moveMs < 40) {
            throw new IllegalArgumentException("\"moveMs\" is " + moveMs + " (expected >= 40)");
        }
        int gapFrames = SnakeJson.intg(root, "gapFrames", 0);
        int frameRows = SnakeJson.intg(root, "frameRows", 1);

        Map<String, Object> stack = SnakeJson.child(root, "stack");
        boolean stackEnabled = stack != null && SnakeJson.bool(stack, "enabled", false);

        Map<String, Object> goal = SnakeJson.child(root, "goal");
        int goalMallocs = SnakeJson.intg(goal, "mallocs", -1);
        int goalSurviveSec = SnakeJson.intg(goal, "surviveSec", -1);
        if ((goalMallocs > 0) == (goalSurviveSec > 0)) {
            throw new IllegalArgumentException(
                    "\"goal\" must set exactly one of \"mallocs\" or \"surviveSec\"");
        }

        Map<String, Object> pickups = SnakeJson.child(root, "pickups");
        Map<String, Object> malloc = pickups == null ? null : SnakeJson.child(pickups, "malloc");
        int mallocSpawn = Math.max(0, SnakeJson.intg(malloc, "max", 3));
        long mallocSpawnMs = Math.max(0, SnakeJson.intg(malloc, "spawnEveryMs", 0));
        int pushRows = 1, returnPops = 2;
        long pushEveryMs = 8000, returnCooldownMs = 4000;

        if (stackEnabled) {
            if (gapFrames < 1 || gapFrames > 40) {
                throw new IllegalArgumentException("\"gapFrames\" is " + gapFrames
                        + " (expected 1..40 when the stack is enabled)");
            }
            if (frameRows < 1 || frameRows > 4) {
                throw new IllegalArgumentException("\"frameRows\" is " + frameRows + " (expected 1..4)");
            }
            pushEveryMs = SnakeJson.intg(stack, "pushEveryMs", 0);
            pushRows = SnakeJson.intg(stack, "pushRows", 1);
            returnPops = SnakeJson.intg(stack, "returnPops", 2);
            returnCooldownMs = SnakeJson.intg(stack, "returnCooldownMs", 4000);
            if (pushEveryMs < 500) {
                throw new IllegalArgumentException("\"stack.pushEveryMs\" is " + pushEveryMs
                        + " (expected >= 500)");
            }
            if (pushRows < 1) {
                throw new IllegalArgumentException("\"stack.pushRows\" is " + pushRows + " (expected >= 1)");
            }
            if (returnPops < 1) {
                throw new IllegalArgumentException("\"stack.returnPops\" is " + returnPops + " (expected >= 1)");
            }
            if (returnCooldownMs < 0) {
                throw new IllegalArgumentException("\"stack.returnCooldownMs\" is out of range");
            }
        }

        boolean fromSelectedTable = SnakeJson.bool(root, "fromSelectedTable", false);
        List<Object> segList = SnakeJson.list(root, "segments");
        boolean segmentSwitchEnabled = (level == 3) || (segList != null && !segList.isEmpty());

        SnakeLevel l = new SnakeLevel();
        l.number = level;
        l.name = name.trim();
        l.cols = cols;
        l.moveMs = moveMs;
        l.fromSelectedTable = fromSelectedTable;
        l.segmentSwitchEnabled = segmentSwitchEnabled;

        if (segmentSwitchEnabled) {
            l.rowUnits = 50;
            l.stackEnabled = false;
            l.mallocSpawn = mallocSpawn > 0 ? mallocSpawn : 4;
            l.mallocSpawnMs = mallocSpawnMs;
            l.goalMallocs = goalMallocs > 0 ? goalMallocs : 12;

            if (fromSelectedTable && SnakeLevel.selectedTableRows.size() >= 3) {
                for (int i = 0; i < 3; i++) {
                    SnakeLevel.SegmentDef src = SnakeLevel.selectedTableRows.get(i);
                    l.segments.add(new SnakeLevel.SegmentDef(i + 1, src.name, src.base, src.limit, i == 2 ? true : src.readOnly));
                }
            } else if (segList != null && !segList.isEmpty()) {
                int idCounter = 1;
                for (Object o : segList) {
                    Map<String, Object> so = SnakeJson.obj(o);
                    int id = SnakeJson.intg(so, "id", idCounter++);
                    String sName = SnakeJson.str(so, "name", "Seg " + id);
                    int base = SnakeJson.intg(so, "base", 0);
                    int limit = SnakeJson.intg(so, "limit", 300);
                    boolean ro = SnakeJson.bool(so, "readOnly", id == 3);
                    l.segments.add(new SnakeLevel.SegmentDef(id, sName, base, limit, ro));
                }
            } else {
                l.segments.add(new SnakeLevel.SegmentDef(1, "Audio_Engine", 6300, 300, false));
                l.segments.add(new SnakeLevel.SegmentDef(2, "Arcade_Sprites", 1400, 400, false));
                l.segments.add(new SnakeLevel.SegmentDef(3, "High_Scores", 4300, 500, true));
            }

            int maxLimit = 0;
            for (SnakeLevel.SegmentDef s : l.segments) {
                if (s.limit > maxLimit) maxLimit = s.limit;
            }
            int maxRows = Math.max(6, Math.min(10, maxLimit / l.rowUnits));
            l.addressSpace = maxRows * l.rowUnits;
            l.heapBase = 0;
            l.heapLimit = l.segments.get(0).limit;
            return l;
        }

        l.rowUnits = 100;
        l.gapFrames = gapFrames;
        l.frameRows = frameRows;
        l.stackEnabled = stackEnabled;
        l.pushEveryMs = pushEveryMs;
        l.pushRows = pushRows;
        l.returnPops = returnPops;
        l.returnCooldownMs = returnCooldownMs;
        l.mallocSpawn = mallocSpawn;
        l.mallocSpawnMs = mallocSpawnMs;
        l.goalMallocs = goalMallocs;
        l.goalSurviveSec = goalSurviveSec;

        // The arena is always six rows and the free gap above it is exactly
        // gapFrames frames, so the level is winnable whatever the file asked for.
        l.layOutSegments();
        return l;
    }
}
