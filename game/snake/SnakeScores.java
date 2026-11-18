package game.snake;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * HEAP vs STACK SNAKE - local high-score table plus the persistent best rank.
 *
 * A small text file beside the working directory lets the cabinet remember the
 * player between runs. The format is:
 *
 * <pre>
 *   # HEAP vs STACK SNAKE HIGH SCORES
 *   best|A
 *   120|A|2026-11-16
 *   90|B|2026-11-16
 *   ...
 * </pre>
 *
 * Guest mode is the same path with nothing recorded yet: an empty or missing
 * file simply reports no best rank and an empty table. Every I/O failure is
 * swallowed, so the game runs without persistence exactly like the silent-audio
 * fallback. This mirrors the Fault Defender table (its own file, its own class).
 */
final class SnakeScores {

    /** One finished run. */
    static final class Entry implements Comparable<Entry> {
        final int score;
        final String rank;
        final String date;
        Entry(int score, String rank, String date) {
            this.score = score;
            this.rank = rank == null ? "-" : rank;
            this.date = date == null ? "" : date;
        }
        @Override public int compareTo(Entry o) { return Integer.compare(o.score, score); }  // best first
    }

    private static final String FILE = "snake_scores.txt";
    private static final int MAX = 8;
    private static List<Entry> cachedEntries = null;
    private static String cachedBest = null;

    private SnakeScores() { }

    /** S &gt; A &gt; B &gt; C &gt; D, anything else lowest. */
    static int rankTier(String rank) {
        if (rank == null || rank.isEmpty()) return -1;
        switch (Character.toUpperCase(rank.charAt(0))) {
            case 'S': return 5;
            case 'A': return 4;
            case 'B': return 3;
            case 'C': return 2;
            case 'D': return 1;
            default:  return 0;
        }
    }

    private static String betterRank(String a, String b) {
        return rankTier(b) > rankTier(a) ? b : a;
    }

    /** Loads the table, best score first (at most {@link #MAX} entries). */
    static synchronized List<Entry> load() {
        if (cachedEntries != null) return new ArrayList<>(cachedEntries);
        List<Entry> out = new ArrayList<>();
        File f = new File(FILE);
        if (f.isFile()) {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("best|")) continue;
                    String[] p = line.split("\\|");
                    if (p.length < 2) continue;
                    try {
                        out.add(new Entry(Integer.parseInt(p[0].trim()), p[1].trim(),
                                p.length > 2 ? p[2].trim() : ""));
                    } catch (NumberFormatException ignored) { }
                }
            } catch (IOException ignored) { }
        }
        Collections.sort(out);
        if (out.size() > MAX) out = new ArrayList<>(out.subList(0, MAX));
        cachedEntries = out;
        return new ArrayList<>(cachedEntries);
    }

    /** The best rank ever achieved, or {@code "-"} when nothing is recorded yet. */
    static synchronized String bestRank() {
        if (cachedBest != null) return cachedBest;
        File f = new File(FILE);
        if (!f.isFile()) {
            cachedBest = "-";
            return cachedBest;
        }
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("best|")) {
                    String[] p = line.split("\\|");
                    if (p.length > 1 && !p[1].trim().isEmpty()) {
                        cachedBest = p[1].trim();
                        return cachedBest;
                    }
                }
            }
        } catch (IOException ignored) { }
        cachedBest = "-";
        return cachedBest;
    }

    /** Records a finished run and returns the (possibly updated) best rank. */
    static synchronized String record(int score, String rank, String date) {
        List<Entry> entries = load();
        String best = betterRank(bestRank(), rank);
        cachedBest = best;
        if (score > 0) entries.add(new Entry(score, rank, date));
        Collections.sort(entries);
        if (entries.size() > MAX) entries = new ArrayList<>(entries.subList(0, MAX));
        cachedEntries = entries;
        write(best, entries);
        return best;
    }

    private static void write(String best, List<Entry> entries) {
        try (BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8))) {
            out.write("# HEAP vs STACK SNAKE HIGH SCORES");
            out.newLine();
            out.write("best|" + best);
            out.newLine();
            for (Entry e : entries) {
                out.write(e.score + "|" + e.rank + "|" + e.date);
                out.newLine();
            }
        } catch (IOException ignored) { }
    }

    static String today() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }
}
