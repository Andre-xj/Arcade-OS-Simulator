package game.snake;

import arcadeos.RetroTheme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;

/**
 * HEAP vs STACK SNAKE - rendering, layout and input.
 *
 * STEP 5 (DECLUTTER + LOOK): the screen reads as a game, not a dashboard.
 *   - a small fixed set of live read-outs only: hearts, score, the MALLOC bar,
 *     one OFFSET/LIMIT thermometer, one FREE GAP meter, a compact minimap and a
 *     one-line kernel ticker;
 *   - everything else is taught, not permanently labelled: the register chips /
 *     units / legend sit behind [TAB], the controls and the heap rule behind [H]
 *     (and appear on pause), and the board carries texture and colour instead of
 *     words - hovering a band shows what it is in a tooltip;
 *   - the footer is a row of key-caps that fades out after ten seconds of play;
 *   - the composition is centred at a fixed content width, so the left and right
 *     gutters are equal and the board is the largest element on screen.
 *
 * No fixed virtual buffer: everything is laid out from the real panel size and
 * the board cell is an integer pixel count, so the pixel font is never
 * resampled. Input uses Key Bindings (never KeyListener) and the model advances
 * on a fixed timestep. The model owns all rules; this class only draws and
 * routes input.
 */
final class SnakeGamePanel extends JPanel {

    /** Blocky 7x6 heart, drawn as pixels so it matches the rest of the cabinet. */
    private static final int[][] HEART = {
        {0, 1, 1, 0, 1, 1, 0},
        {1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 1, 1, 1, 1},
        {0, 1, 1, 1, 1, 1, 0},
        {0, 0, 1, 1, 1, 0, 0},
        {0, 0, 0, 1, 0, 0, 0},
    };

    /** Address rows the dim stack teaser covers when no stack is pushed. */
    private static final int STACK_TEASER_ROWS = 2;
    /** Kernel ticker: keep this many lines, show only the newest one. */
    private static final int LOG_CAP = 12, LOG_SHOWN = 1;

    /**
     * PART 1.8: footer key-caps as {glyph, label}. Every key that has a name -
     * P/R/SPACE/M/ESC - carries a 12 px label beside the cap; only the four
     * arrows stay bare. The whole row fades out after the first ten seconds
     * (see {@link #footerAlpha()}).
     */
    private static final String[][] FOOTER_CAPS = {
            {"\u2190", ""}, {"\u2191", ""}, {"\u2193", ""}, {"\u2192", ""},
            {"P", "PAUSE"}, {"R", "RESTART"}, {"SPACE", "RETURN"}, {"M", "MUTE"}, {"ESC", "EXIT"},
    };

    /**
     * STEP 5 B - one type scale for the whole cabinet. The numbers are the drawn
     * ink heights the spec asks for; Consolas renders a capital at about 0.63 em,
     * so 19/22/32/45 px nominal paint 12/14/20/29 px of ink. Measured by
     * {@link #fontMetricsLine(int)}.
     */
    static final int SZ_SCORE = 45, SZ_KEY = 32, SZ_TITLE = 22, SZ_HELPER = 19;

    /** FIX 11: every log line has a kind, and the kind picks the colour. */
    private enum LogKind { OK, WARN, FAULT, INFO }

    /** One kernel-log line: a "[  12.4]" stamp, the text, and its kind. */
    private static final class LogLine {
        final String ts, text;
        final LogKind kind;
        LogLine(String ts, String text, LogKind kind) {
            this.ts = ts;
            this.text = text;
            this.kind = kind;
        }
    }

    private final SnakeGameDialog owner;
    private final SnakeModel model;
    private final Timer timer;

    // ── Layout (recomputed whenever the panel size changes) ──────────────
    private int margin, gap, hudH, logH, footH, sideW, rulerW, mmW;
    private int cell, boardX, boardY, boardW, boardH;
    private int leftX, leftY, leftH, rulerX, rightX, rightY, rightH;
    private int hudX, hudY, hudW, logY, footY;
    private int contentW;
    private int lastW = -1, lastH = -1;
    private boolean metricsPrinted = false;

    // ── STEP 5 declutter state ───────────────────────────────────────────
    /** Tab: the advanced side panel (registers, units, legend). Hidden by default. */
    private boolean showAdvanced = false;
    /** H, or being paused: the controls list and the heap rule. Hidden by default. */
    private boolean showHelp = false;
    /** F2: faint CRT scanlines. On by default. */
    private boolean scanlines = true;
    /** Wall clock the current life started at - the footer key-caps fade from here. */
    private long lifeStartMs = System.currentTimeMillis();

    /* STEP 5 A: how long the footer key-caps stay up, and how long they take to go. */
    private static final long FOOTER_HOLD_MS = 10_000, FOOTER_FADE_MS = 700;

    // One type scale: score, key value, panel title, helper text.
    private Font fScore, fKey, fTitle, fHelper;

    /** Every string the gameplay renderer draws in the steady state (STEP 5 A). */
    private final List<String> frameText = new ArrayList<>();
    private boolean textReported = false;

    private int shakeX = 0, shakeY = 0;
    private long lastNano = System.nanoTime();

    // ── STEP 5 C: frame time. The acceptance number is the paint cost of one render()
    //    call, kept as an exponentially smoothed so a single slow frame never lies.
    private double frameMs = 0.0;
    private long frameReportAt = 0L;

    // ── STEP 5 C: motion. All of it is render-side and cheap: a handful of
    //    float particles, one floating "+10", a start banner and a death fade.
    /** Particles: {x, y, vx, vy, lifeMs, maxLife, r, g, b}. */
    private final List<double[]> fx = new ArrayList<>();
    /** Floating "+10" call-outs: {x, y, lifeMs}. */
    private final List<double[]> floats = new ArrayList<>();
    /** Wall clock the level-start banner is up until (1.5 s). */
    private long bannerUntilMs;
    /** Wall clock the death animation started, or 0 while alive. */
    private long deathMs;
    private int fxMallocs, fxHearts;
    private SnakeModel.Phase fxPhase;

    // ── P3: Analog needle smoothing for the OFFSET thermometer ───────────
    private double needleSmoothY = -1.0;

    // ── Kernel log (view-side only; fed by watching the model each frame) ─
    private final Deque<LogLine> log = new ArrayDeque<>();
    /** Wall clock the current life started at - the source of the "[  12.4]" stamps. */
    private long t0 = System.currentTimeMillis();
    private boolean booted = false;
    private int seenMallocs, seenHearts, seenFaults;
    private SnakeModel.Phase seenPhase;

    // ── C1: Briefing card state ─────────────────────────────────────────
    private boolean briefingActive = false;
    private long briefingStartMs = 0L;
    private int lastBriefLevel = -1;

    // ── C2: Review in simulator callback ──────────────────────────────────
    private SnakeReviewListener reviewListener = null;

    // ── PART 3: the stack, view side ─────────────────────────────────────
    /** Frames the log has already accounted for, so pushes / returns are logged once. */
    private int seenStackFrames;
    /** Set when a collision was just logged, so it is not also logged as a return. */
    private boolean stackLogSuppressed;
    /** The one-time, first-play stack callout: shown once, then dismissed by any move. */
    private boolean stackCalloutShown = false;
    private boolean calloutDismissed = false;
    /** Denied-pulse left after SPACE is refused during the return cooldown, in ms. */
    private double returnDeniedMs = 0;
    /** True latch so the alarm only replays when the gap first becomes critical. */
    private boolean alarmLatched = false;
    /** Countdown (ms) until the next alarm beep while the gap stays critical. */
    private double alarmBeepAcc = 0;

    // ── TASK 2: end screen (menu + persisted scores) ─────────────────────
    /** The end-screen action labels; NEXT LEVEL is conditional (PART 1.1). */
    private static final String NEXT_LABEL = "NEXT LEVEL",
            AGAIN_LABEL = "PLAY AGAIN", EXIT_LABEL = "RETURN TO OS",
            REVIEW_LABEL = "REVIEW IN SIM";
    /** Keyboard-selected button (Up/Down moves it, Enter activates it). */
    private int overIndex = 0;
    /** Button under the mouse, or -1. Drawn the same as the selected one. */
    private int hoverIndex = -1;
    /** True once this finished run has been written to the score table. */
    private boolean runRecorded = false;
    private List<SnakeScores.Entry> hiScores = new ArrayList<>();
    private String bestRank = "-";

    SnakeGamePanel(SnakeGameDialog owner, SnakeModel model) {
        this.owner = owner;
        this.model = model;
        setBackground(new Color(6, 4, 14));
        setFocusable(true);
        setLayout(null);
        // STEP 5 A: Tab opens the advanced panel, so it must not walk the focus.
        setFocusTraversalKeysEnabled(false);
        setupInput();

        // STEP 5 A: the board teaches by hover instead of by permanent labels.
        ToolTipManager.sharedInstance().registerComponent(this);
        ToolTipManager.sharedInstance().setInitialDelay(600);
        ToolTipManager.sharedInstance().setDismissDelay(6000);
        addMouseMotionListener(new MouseMotionAdapter() {
            private int lastX = -1, lastY = -1;
            @Override public void mouseMoved(MouseEvent e) {
                if (Math.abs(e.getX() - lastX) > 2 || Math.abs(e.getY() - lastY) > 2) {
                    lastX = e.getX();
                    lastY = e.getY();
                    dismissToolTip();
                }
            }
        });

        seenMallocs = model.mallocsEaten;
        seenHearts = model.hearts;
        seenFaults = model.mistakes.size();
        seenPhase = model.phase;
        fxMallocs = model.mallocsEaten;
        fxHearts = model.hearts;
        fxPhase = model.phase;
        bannerUntilMs = System.currentTimeMillis() + 1500;

        // TASK 2: the persisted high-score table and best rank (guest mode = empty).
        hiScores = SnakeScores.load();
        bestRank = SnakeScores.bestRank();

        // STEP 1: any click on the game takes keyboard focus back off the chrome
        // (e.g. after the user presses the SOUND button) so SPACE always plays.
        // TASK 2: a click on the end panel also activates the button under it.
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (model.phase == SnakeModel.Phase.PLAYING) return;
                int i = buttonAt(e.getX(), e.getY());
                if (i >= 0) { overIndex = i; activateOver(i); }
            }
        });
        // TASK 2: hovering an end button highlights it, like the keyboard cursor.
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int i = model.phase == SnakeModel.Phase.PLAYING
                        ? -1 : buttonAt(e.getX(), e.getY());
                if (i != hoverIndex) { hoverIndex = i; repaint(); }
            }
        });

        // (a) A resize must always repaint the whole panel: without this, Swing
        // only repaints the newly exposed strip and the previous layout - the
        // footer included - stays on screen as a faint leftover copy.
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                lastW = lastH = -1;
                repaint();
            }
        });

        timer = new Timer(16, e -> {
            long now = System.nanoTime();
            double dtMs = (now - lastNano) / 1e6;
            lastNano = now;
            if (dtMs > 100) dtMs = 100;                 // never simulate a huge catch-up
            int[] oldHead = model.body.peekFirst();
            int oldCol = oldHead != null ? oldHead[0] : -1;
            int oldRow = oldHead != null ? oldHead[1] : -1;
            // C1: Dismiss briefing on timer timeout (2.5s)
            if (briefingActive && System.currentTimeMillis() - briefingStartMs > 2500) {
                briefingActive = false;
            }
            model.advance(dtMs);
            int[] newHead = model.body.peekFirst();
            if (newHead != null && (newHead[0] != oldCol || newHead[1] != oldRow)) {
                dismissToolTip();
            }
            updateLog();
            updateEffects(dtMs);
            if (model.shake > 0) {
                shakeX = (int) (Math.random() * model.shake * 2 - model.shake);
                shakeY = (int) (Math.random() * model.shake * 2 - model.shake);
            } else {
                shakeX = shakeY = 0;
            }
            repaint();
        });
        timer.start();
    }

    void stop() {
        timer.stop();
    }

    void requestFocusNow() {
        requestFocusInWindow();
    }

    /** Restart the current level (R, or SPACE from a finished screen). */
    private void restart() {
        model.reset();
        resetView();
        GameSfx.uiClick();
        // STEP 1: after Play Again, keep the keys on the game panel.
        requestFocusInWindow();
    }

    /**
     * PART 3: SPACE - the RETURN key. It pops the level's frames while the stack
     * is live; a press that lands inside the cooldown only pulses (denied). On an
     * end card, or any non-playing state, it does nothing at all, so SPACE can
     * never activate a menu button.
     */
    private void spaceAction() {
        if (model.phase != SnakeModel.Phase.PLAYING || model.paused || showHelp) return;
        if (!model.level.stackEnabled) return;
        if (!model.canReturn()) {
            returnDeniedMs = 280;               // subtle denied pulse
            repaint();
            return;
        }
        model.returnFrames();
        repaint();
    }

    /**
     * Clears every piece of view state that belongs to one run. Shared by Restart
     * and by PART 1.1's Next Level, so both start from an identical clean slate.
     */
    private void resetView() {
        lastNano = System.nanoTime();
        log.clear();
        t0 = System.currentTimeMillis();
        lifeStartMs = t0;
        booted = false;
        seenMallocs = model.mallocsEaten;
        seenHearts = model.hearts;
        seenFaults = model.mistakes.size();
        seenPhase = model.phase;
        seenStackFrames = model.stackFrames;
        fxMallocs = model.mallocsEaten;
        fxHearts = model.hearts;
        fxPhase = model.phase;
        fx.clear();
        floats.clear();
        deathMs = 0;
        bannerUntilMs = System.currentTimeMillis() + 1500;
        overIndex = 0;
        hoverIndex = -1;
        runRecorded = false;
        stackCalloutShown = false;   // PART 3: the first-play callout returns
        calloutDismissed = false;
        returnDeniedMs = 0;
        alarmLatched = false;
        alarmBeepAcc = 0;
        stackLogSuppressed = false;

        // C1: Show briefing card on level start for 2.5s
        if (model.level != null && model.level.number != lastBriefLevel) {
            briefingActive = true;
            briefingStartMs = System.currentTimeMillis();
            lastBriefLevel = model.level.number;
        } else {
            briefingActive = false;
        }
    }

    void setReviewListener(SnakeReviewListener listener) {
        this.reviewListener = listener;
    }

    // =====================================================================
    // INPUT
    // =====================================================================
    private void setupInput() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        steer(im, am, KeyEvent.VK_LEFT, -1, 0);
        steer(im, am, KeyEvent.VK_A, -1, 0);
        steer(im, am, KeyEvent.VK_RIGHT, 1, 0);
        steer(im, am, KeyEvent.VK_D, 1, 0);
        steer(im, am, KeyEvent.VK_UP, 0, 1);
        steer(im, am, KeyEvent.VK_W, 0, 1);
        steer(im, am, KeyEvent.VK_DOWN, 0, -1);
        steer(im, am, KeyEvent.VK_S, 0, -1);

        // PART 3: SPACE is the RETURN key. It pops stack frames during play and
        // never activates an end-screen button; a refused press (cooldown) only
        // pulses. On the help / pause card it does nothing.
        press(im, am, KeyEvent.VK_SPACE, "SNAKE-RETURN", this::spaceAction);
        // TASK 2: on an end screen Enter activates the selected menu button.
        press(im, am, KeyEvent.VK_ENTER, "SNAKE-ENTER", () -> {
            if (model.phase != SnakeModel.Phase.PLAYING) activateOver(overIndex);
        });
        press(im, am, KeyEvent.VK_P, "SNAKE-PAUSE", () -> {
            model.paused = !model.paused;
            GameSfx.uiClick();
        });
        press(im, am, KeyEvent.VK_R, "SNAKE-RESTART", this::restart);
        press(im, am, KeyEvent.VK_M, "SNAKE-MUTE", () -> {
            GameSfx.toggleMute();
            owner.refreshMute();
        });
        press(im, am, KeyEvent.VK_ESCAPE, "SNAKE-EXIT", () -> owner.requestExit());

        // ── STEP 5 A: the declutter toggles ──
        // TAB: the advanced side panel (registers, units, legend).
        press(im, am, KeyEvent.VK_TAB, "SNAKE-ADV", () -> {
            showAdvanced = !showAdvanced;
            GameSfx.uiClick();
        });
        // H: the controls and the one rule this level is about.
        press(im, am, KeyEvent.VK_H, "SNAKE-HELP", () -> {
            showHelp = !showHelp;
            GameSfx.uiClick();
        });
        // F2: CRT scanlines.
        press(im, am, KeyEvent.VK_F2, "SNAKE-CRT", () -> scanlines = !scanlines);

        // TASK 3B: Segment register selection (keys 1, 2, 3)
        press(im, am, KeyEvent.VK_1, "SNAKE-SEG-1", () -> switchSegment(1));
        press(im, am, KeyEvent.VK_2, "SNAKE-SEG-2", () -> switchSegment(2));
        press(im, am, KeyEvent.VK_3, "SNAKE-SEG-3", () -> switchSegment(3));
    }

    private void switchSegment(int segNum) {
        if (model.phase == SnakeModel.Phase.PLAYING && !model.paused && model.isSegmentSwitchLevel()) {
            calloutDismissed = true;
            model.switchSegment(segNum);
            repaint();
        }
    }

    private void steer(InputMap im, ActionMap am, int key, final int dx, final int dy) {
        im.put(KeyStroke.getKeyStroke(key, 0), "steer-" + key);
        am.put("steer-" + key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                dismissToolTip();
                // P1: Briefing card consumes the dismissal key so player doesn't accidentally steer into a wall
                if (briefingActive) {
                    briefingActive = false;
                    repaint();
                    return;
                }
                if (model.phase == SnakeModel.Phase.PLAYING && !model.paused) {
                    calloutDismissed = true;    // PART 3: any move dismisses the callout
                    model.queueDir(dx, dy);
                } else if (model.phase != SnakeModel.Phase.PLAYING && dx == 0 && dy != 0) {
                    // TASK 2: on an end screen Up/Down walk the menu instead of steering.
                    moveOver(dy > 0 ? -1 : 1);
                }
            }
        });
    }

    private void press(InputMap im, ActionMap am, int key, String name, Runnable r) {
        im.put(KeyStroke.getKeyStroke(key, 0), name);
        am.put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                dismissToolTip();
                if (briefingActive) {
                    briefingActive = false;
                    repaint();
                }
                r.run();
            }
        });
    }

    // =====================================================================
    // TASK 2 - END-SCREEN MENU
    // =====================================================================
    /**
     * PART 1.1: the buttons this end card offers. "NEXT LEVEL" is present only when
     * the campaign has a level after the current one, so it is hidden - not merely
     * disabled - once the last authored level is cleared.
     */
    private String[] overButtons() {
        List<String> list = new ArrayList<>();
        // A2: NEXT LEVEL only on LEVEL_CLEAR, never after death.
        if (model.phase != SnakeModel.Phase.GAME_OVER && SnakeLevel.next(model.level) != null) {
            list.add(NEXT_LABEL);
        }
        // C2: REVIEW IN SIM button available when a reviewListener is registered
        if (reviewListener != null) {
            list.add(REVIEW_LABEL);
        }
        list.add(AGAIN_LABEL);
        list.add(EXIT_LABEL);
        return list.toArray(new String[0]);
    }

    /** Moves the keyboard cursor on the end menu, wrapping at both ends. */
    private void moveOver(int d) {
        int n = overButtons().length;
        overIndex = (overIndex + d + n) % n;
        GameSfx.uiClick();
        repaint();
    }

    /** Runs the end-screen button at index {@code i}. */
    private void activateOver(int i) {
        String[] b = overButtons();
        if (i < 0 || i >= b.length) return;
        if (NEXT_LABEL.equals(b[i])) {
            nextLevel();
        } else if (REVIEW_LABEL.equals(b[i])) {
            if (reviewListener != null) {
                int segNum = model.lastFaultSegmentId > 0 ? model.lastFaultSegmentId : 1;
                int base = model.lastFaultBase;
                int limit = model.lastFaultLimit;
                int off = model.lastFaultOffset;
                reviewListener.reviewInSimulator(segNum, base, limit, off);
            }
            owner.requestExit();
        } else if (AGAIN_LABEL.equals(b[i])) {
            restart();
        } else {
            owner.requestExit();
        }
    }

    /**
     * PART 1.1: advance to the next level in the library. The model is rebuilt for
     * the new level (board size and address space included) and the panel's view
     * state is cleared, so Level 2 starts exactly like a fresh run. When there is
     * no next level the button does not exist, and this is a no-op.
     */
    private void nextLevel() {
        SnakeLevel next = SnakeLevel.next(model.level);
        if (next == null) return;
        model.setLevel(next);
        resetView();
        owner.refreshHeader();   // A5: keep header in sync with current level
        GameSfx.uiClick();
        requestFocusInWindow();
    }

    /** Index of the end-screen button under a panel-space point, or -1. */
    private int buttonAt(int mx, int my) {
        Insets in = getInsets();
        int x = mx - in.left, y = my - in.top;
        int w = Math.max(0, getWidth() - in.left - in.right);
        int h = Math.max(0, getHeight() - in.top - in.bottom);
        EndLayout e = endLayoutFor(w, h);
        for (int i = 0; i < e.btnCount; i++) {
            if (x >= e.bx[i] && x < e.bx[i] + e.bw[i]
                    && y >= e.by[i] && y < e.by[i] + e.bh[i]) return i;
        }
        return -1;
    }

    /**
     * PART 1.1 / 1.4 / 1.5 / 1.6: the end card's geometry for this run. The
     * visible buttons, how many stat cells the report needs, how many high scores
     * exist and whether this is the Level Clear card all feed the content-sized
     * layout, so the card is never taller than what it actually draws.
     */
    private EndLayout endLayoutFor(int w, int h) {
        int stats = statCells().length;
        int hi = hiScores == null ? 0 : hiScores.size();
        boolean clear = model.phase == SnakeModel.Phase.LEVEL_CLEAR;
        return endLayout(w, h, model.cols, model.rows, overButtons().length, stats, hi, clear);
    }

    /**
     * PART 1.5: the two-column report grid, in full words. Only the faults that
     * actually happened are listed, and the smallest free gap is always the last
     * cell. Each cell is {label, value}.
     */
    private String[][] statCells() {
        List<String[]> out = new ArrayList<>();
        for (SnakeModel.Fault f : SnakeModel.Fault.values()) {
            int n = model.faultCount(f);
            if (n > 0) out.add(new String[]{statLabel(f), String.valueOf(n)});
        }
        if (model.level.stackEnabled) {
            out.add(new String[]{"SMALLEST GAP", String.valueOf(model.smallestFreeGap)});
        }
        return out.toArray(new String[0][]);
    }

    /** PART 1.5: the full-word name of a fault kind, for the report grid. */
    private static String statLabel(SnakeModel.Fault f) {
        switch (f) {
            case SEGFAULT:         return "SEGFAULTS";
            case HEAP_CORRUPTION:  return "CORRUPTION";
            case PROTECTION_FAULT: return "PROTECTION";
            case COLLISION:        return "COLLISIONS";
            default:               return "FAULT";
        }
    }

    void dismissToolTip() {
        try {
            ToolTipManager.sharedInstance().setEnabled(false);
            ToolTipManager.sharedInstance().setEnabled(true);
        } catch (Exception ignored) {}
    }

    @Override
    public Point getToolTipLocation(MouseEvent e) {
        if (e == null || model.phase != SnakeModel.Phase.PLAYING || model.paused || showHelp) return null;
        Insets in = getInsets();
        int mx = e.getX() - in.left, my = e.getY() - in.top;
        if (mx < boardX || mx >= boardX + boardW || my < boardY || my >= boardY + boardH) {
            if (mx >= rightX && mx < rightX + sideW && my >= rightY && my < rightY + rightH) {
                return new Point(e.getX() - 140, Math.max(rightY + in.top + 4, e.getY() - 32));
            }
            return null;
        }
        int c = (mx - boardX) / Math.max(1, cell);
        int r = (boardY + boardH - 1 - my) / Math.max(1, cell);
        int cellLeft = boardX + c * cell;
        int cellTop = boardY + boardH - (r + 1) * cell;
        int tipW = 280, tipH = 26;
        int tx = cellLeft + cell + 6;
        if (tx + tipW > boardX + boardW - 4) {
            tx = cellLeft - tipW - 6;
        }
        tx = clamp(tx, boardX + 4, Math.max(boardX + 4, boardX + boardW - tipW - 4));
        int ty = cellTop - tipH - 4;
        if (ty < boardY + 4) {
            ty = cellTop + cell + 4;
        }
        ty = clamp(ty, boardY + 4, Math.max(boardY + 4, boardY + boardH - tipH - 4));
        return new Point(tx + in.left, ty + in.top);
    }

    // =====================================================================
    // TOOLTIPS - the board explains itself on hover instead of by labels
    // =====================================================================
    @Override
    public String getToolTipText(MouseEvent e) {
        if (e == null) return null;
        // PART 1.2: while an end card is open it owns the whole board, so no board
        // or minimap tooltip may leak through it. The help / pause card is the same.
        if (model.phase != SnakeModel.Phase.PLAYING || model.paused || showHelp) return null;
        Insets in = getInsets();
        int mx = e.getX() - in.left, my = e.getY() - in.top;

        if (mx >= rightX && mx < rightX + sideW && my >= rightY && my < rightY + rightH) {
            int mmAddr = addrAt(my);
            int mmOff = Math.max(0, mmAddr - model.heapBase);
            int mmPA = model.currentSegmentBase() + mmOff;
            return "MINIMAP @ 0x" + hex4(mmPA) + " (" + mmPA + ") [offset " + mmOff + "]";
        }

        if (mx < boardX || mx >= boardX + boardW || my < boardY || my >= boardY + boardH) return null;
        SnakeLevel lv = model.level;
        boolean nearLimit = Math.abs(my - yOf(model.heapTop())) <= 4;
        boolean nearBase  = Math.abs(my - yOf(model.heapBase)) <= 4;
        // PART 1.2: on a boundary the tooltip prints the exact boundary address -
        // the same value the ruler labels - never a rounded 0x00C7 vs 0x00C8.
        int addr = nearLimit ? model.heapTop() : (nearBase ? model.heapBase : addrAt(my));
        String zone;
        if (addr >= lv.codeBase && addr < lv.codeBase + lv.codeLimit) {
            zone = "CODE \u2014 read-only segment";
        } else if (addr >= lv.dataBase && addr < lv.dataBase + lv.dataLimit) {
            zone = "DATA \u2014 read/write segment";
        } else if (addr >= model.heapBase && addr < model.heapTop()) {
            zone = "HEAP \u2014 your arena";
        } else if (lv.stackEnabled && addr >= model.stackFrontier()) {
            zone = "STACK \u2014 grows down";
        } else {
            zone = "FREE \u2014 unmapped address space";
        }
        String edge = "";
        if (nearLimit) edge = "  [LIMIT \u2014 base + limit]";
        else if (nearBase) edge = "  [BASE]";
        int off = addr - model.heapBase;
        int pa = model.currentSegmentBase() + off;
        return zone + edge + "   @ 0x" + hex4(pa) + " (" + pa + ") [offset " + off + "]";
    }

    private static String hex4(int v) {
        return String.format("%04X", v & 0xFFFF);
    }

    /**
     * TASK 1.2: the global ToolTip font is 9 px, below the 12 px floor, so every
     * board tooltip is forced to the helper tier and the cabinet's palette.
     */
    @Override
    public JToolTip createToolTip() {
        JToolTip tip = super.createToolTip();
        tip.setFont(fHelper != null ? fHelper : new Font("Consolas", Font.BOLD, SZ_HELPER));
        tip.setBackground(new Color(16, 12, 32));
        tip.setForeground(RetroTheme.TEXT_WHITE);
        tip.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_BRIGHT, 1));
        return tip;
    }

    /** Board address under a panel-space y, clamped to the address space. */
    private int addrAt(int my) {
        double frac = (boardY + boardH - my) / (double) Math.max(1, boardH);
        int a = (int) Math.round(frac * model.level.addressSpace);
        return clamp(a, 0, Math.max(0, model.level.addressSpace - 1));
    }

    // =====================================================================
    // LAYOUT
    // =====================================================================
    /** Measured layout numbers, kept so the probe and the live log share one formula. */
    static final class Metrics {
        int cell, boardX, boardY, boardW, boardH, hudY;
    }

    /**
     * Geometry of the TASK 2 end panel, so render and mouse hit-testing agree.
     *
     * PART 1.1 / 1.4 / 1.6: the card is sized from its content, so it also carries
     * how many buttons it shows, how many report rows the stats grid needs and
     * which optional blocks (Level Clear middle, mistake review) survived the
     * compact ladder.
     */
    static final class EndLayout {
        int px, py, pw, ph, pad;
        /** PART 1.1: NEXT LEVEL is hidden (not disabled) once the campaign ends. */
        int btnCount = 3;
        /** Rows in the two-column stats grid (faults that happened + smallest gap). */
        int statRows;
        /** Level Clear middle: high-score lines drawn under the time / rank row. */
        int hiRows;
        boolean showMiddle;
        /** Game Over: how many helper lines the mistake review band gets (0 = none). */
        int reviewLines;
        final int[] bx = new int[4], by = new int[4], bw = new int[4], bh = new int[4];

        /** Helper lines the Level Clear high-score block takes: header + rows. */
        int hiLines() { return hiRows > 0 ? hiRows + 1 : 0; }
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    // ── End-screen vertical rhythm: one source for render and the probe ──
    private static final int E_GAP_CAUSE = 10, E_GAP_SEP = 12, E_GAP_REPORT = 10,
            E_ROW_GAP = 2, E_GAP_REVIEW = 10, E_BTN_GAP = 12, E_GAP_BTN = 14,
            E_HINT_GAP = 14, E_REVIEW_LINES = 3, E_MAX_STAT_ROWS = 3;

    /**
     * The three end-card tiers, measured once off-screen so the layout maths can
     * ask how tall the card must be without owning a Graphics2D.
     */
    private static final FontMetrics END_SCORE_FM, END_KEY_FM, END_HELPER_FM;
    static {
        java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(
                1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        END_SCORE_FM = g.getFontMetrics(new Font("Consolas", Font.BOLD, SZ_SCORE));
        END_KEY_FM = g.getFontMetrics(new Font("Consolas", Font.BOLD, SZ_KEY));
        END_HELPER_FM = g.getFontMetrics(new Font("Consolas", Font.BOLD, SZ_HELPER));
        g.dispose();
    }

    /**
     * PART 1.4: the height the card needs for exactly the blocks {@code e} is
     * showing, from the title down to the hint line's padding. Nothing is
     * reserved for a block that the compact ladder dropped, so the card never
     * has a hole and never overlaps its own border.
     */
    private static int endMinHeight(EndLayout e, int pad, int btnH) {
        int helper = END_HELPER_FM.getHeight();
        int btnTotal = e.btnCount * btnH + Math.max(0, e.btnCount - 1) * E_BTN_GAP;
        int hgt = pad
                + END_SCORE_FM.getAscent()                        // title
                + E_GAP_CAUSE + END_KEY_FM.getAscent()            // cause
                + E_GAP_SEP + E_GAP_REPORT                        // separator + report gap
                + 2 * (END_KEY_FM.getHeight() + E_ROW_GAP)        // SCORE/RANK, LONGEST/MALLOC
                + e.statRows * helper                             // faults + smallest gap
                + E_GAP_BTN + btnTotal                            // gap + buttons
                + E_HINT_GAP + helper                             // hint line
                + pad;                                            // bottom padding
        if (e.showMiddle) hgt += END_KEY_FM.getHeight() + E_ROW_GAP + e.hiLines() * helper;
        if (e.reviewLines > 0) hgt += E_GAP_REVIEW + e.reviewLines * helper;
        return hgt;
    }

    /**
     * TASK 2 / PART 1.4 / 1.6: the end card, centred on the board rect and sized
     * from its own content. The richest arrangement is tried first; the compact
     * ladder then drops the high-score lines, then the Level Clear middle, then
     * the mistake review lines, until the card fits inside the board's height.
     */
    static EndLayout endLayout(int w, int h, int cols, int rows,
                               int btnCount, int stats, int hiCount, boolean clear) {
        Metrics m = compute(w, h, cols, rows);
        EndLayout e = new EndLayout();
        e.btnCount = clamp(btnCount, 1, 4);

        int pad = clamp(m.boardH / 22, 10, 24);
        int btnH = clamp(m.boardH / 14, 22, 42);
        int wantW = Math.min(820, Math.max(460, (int) (m.boardW * 0.74f)));
        e.pw = Math.min(wantW, Math.max(220, m.boardW - 20));

        int availH = Math.max(150, h - 2 * 12);
        int statRows = Math.max(1, Math.min(E_MAX_STAT_ROWS, (stats + 1) / 2));

        int need = 0;
        for (int detail = 3; detail >= 0; detail--) {
            e.statRows = statRows;
            e.showMiddle = clear && detail >= 1;
            e.hiRows = clear ? (detail >= 3 ? Math.min(3, Math.max(0, hiCount))
                    : (detail == 2 ? Math.min(1, Math.max(0, hiCount)) : 0)) : 0;
            e.reviewLines = clear ? 0 : (detail >= 3 ? E_REVIEW_LINES : detail);
            need = endMinHeight(e, pad, btnH);
            if (need <= m.boardH) break;      // it fits inside the board: keep it
        }
        int cap = Math.min(Math.max(150, m.boardH), availH);
        e.ph = clamp(need, 150, cap);
        e.px = clamp(m.boardX + (m.boardW - e.pw) / 2, 8, Math.max(8, w - 8 - e.pw));
        e.py = clamp(m.boardY + (m.boardH - e.ph) / 2, 8, Math.max(8, h - 8 - e.ph));
        e.pad = pad;

        int btnTotal = e.btnCount * btnH + (e.btnCount - 1) * E_BTN_GAP;
        int btnBottom = e.py + e.ph - pad - END_HELPER_FM.getHeight() - E_HINT_GAP;
        int btnTop = btnBottom - btnTotal;
        int btnWid = e.pw - 2 * pad;
        for (int i = 0; i < e.btnCount; i++) {
            e.bx[i] = e.px + pad;
            e.bw[i] = btnWid;
            e.bh[i] = btnH;
            e.by[i] = btnTop + i * (btnH + E_BTN_GAP);
        }
        return e;
    }

    /**
     * Y positions of the end card's header and report block, measured with the
     * real fonts: {titleBaseline, causeBaseline, separatorY, reportTop,
     * reportBottom}. The render and the offline probe both read this, so they can
     * never drift apart.
     */
    private static int[] endReportBottomY(int py, int pad, FontMetrics sfm,
                                          FontMetrics kfm, FontMetrics hfm, EndLayout e) {
        int titleB = py + pad + sfm.getAscent();
        int causeB = titleB + E_GAP_CAUSE + kfm.getAscent();
        int sepY = causeB + E_GAP_SEP;
        int ry = sepY + E_GAP_REPORT;
        ry += 2 * (kfm.getHeight() + E_ROW_GAP);     // SCORE/RANK, LONGEST/MALLOC
        ry += e.statRows * hfm.getHeight();          // faults + smallest gap
        int reportBottom = ry;
        if (e.showMiddle) ry += kfm.getHeight() + E_ROW_GAP + e.hiRows * hfm.getHeight();
        return new int[]{titleB, causeB, sepY, sepY + E_GAP_REPORT, reportBottom};
    }

    /** One-line report of the end panel at a size (used by the offline probe). */
    static String endPanelLine(int w, int h, SnakeLevel lv) {
        Metrics m = compute(w, h, lv.cols, lv.rows());
        // Worst case for the probe: the Level Clear card, three buttons, the full
        // stats grid, three high scores. If that fits, every card fits.
        EndLayout e = endLayout(w, h, lv.cols, lv.rows(), 3, 5, 3, true);
        boolean fits = e.px >= m.boardX && e.py >= m.boardY
                && e.px + e.pw <= m.boardX + m.boardW
                && e.py + e.ph <= m.boardY + m.boardH
                && e.px >= 0 && e.py >= 0 && e.px + e.pw <= w && e.py + e.ph <= h;

        int[] y = endReportBottomY(e.py, e.pad, END_SCORE_FM, END_KEY_FM, END_HELPER_FM, e);
        int reviewTop = y[4] + (e.reviewLines > 0 ? E_GAP_REVIEW : 0);
        int reviewBot = e.by[0] - E_GAP_BTN;
        int need = e.reviewLines * END_HELPER_FM.getHeight();
        int room = reviewBot - reviewTop;

        return String.format("SNAKE END panel=%d,%d %dx%d board=%d,%d %dx%d fits=%s "
                + "| stats=%d hi=%d middle=%s review=%d/%d | reviewTop=%d reviewBot=%d "
                + "room=%d need=%d",
                e.px, e.py, e.pw, e.ph, m.boardX, m.boardY, m.boardW, m.boardH, fits,
                e.statRows, e.hiRows, e.showMiddle, e.reviewLines, E_REVIEW_LINES,
                reviewTop, reviewBot, room, need);
    }

    /**
     * Pure layout maths. The composition is centred at a fixed content width: the
     * gauge column, the hex ruler, the board and the minimap column, with equal
     * gutters left and right. The board gets the largest integer cell that fits
     * the height it has left, so it is always the biggest thing on screen.
     *
     * TASK 1.3: the HUD now owns a real band ABOVE the board instead of being an
     * overlay, so stack frames pushed from the top can never be drawn underneath
     * the hearts, the score or the MALLOC counter. At the target resolutions the
     * board is width-limited, so reserving the band costs the board no height.
     */
    static Metrics compute(int w, int h, int cols, int rows) {
        Metrics m = new Metrics();
        int margin = 14, gap = 10;
        int hudH  = clamp(h / 11, 62, 92);     // HUD band above the board's top edge
        int logH  = clamp(h / 22, 30, 42);     // one-line kernel ticker
        int footH = clamp(h / 24, 30, 40);     // key-cap footer
        int sideW = clamp(w / 7, 190, 250);    // left gauge column
        int rulerW = clamp(w / 26, 54, 76);    // hex ruler
        int mmW   = clamp(w / 9, 140, 190);    // minimap column

        int availH = Math.max(1, h - 2 * margin - logH - footH - 2 * gap);
        int boardAvailH = Math.max(1, availH - hudH - gap);
        int maxBoardW = Math.max(1, w - 2 * margin - sideW - rulerW - mmW - 4 * gap);
        m.cell = Math.max(6, Math.min(maxBoardW / cols, boardAvailH / rows));
        m.boardW = m.cell * cols;
        m.boardH = m.cell * rows;

        int contentW = sideW + gap + rulerW + gap + m.boardW + gap + mmW;
        int left = Math.max(margin, (w - contentW) / 2);
        m.boardX = left + sideW + gap + rulerW + gap;
        int blockH = hudH + gap + m.boardH;
        int blockY = margin + Math.max(0, (availH - blockH) / 2);
        m.hudY = blockY;
        m.boardY = blockY + hudH + gap;
        return m;
    }

    /** One-line layout report for a hypothetical panel size (used by the test probe). */
    static String metricsLine(int w, int h, SnakeLevel lv) {
        int rows = lv.rows(), cols = lv.cols;
        Metrics m = compute(w, h, cols, rows);
        int gap = 10, margin = 14;
        int sideW = clamp(w / 7, 190, 250), rulerW = clamp(w / 26, 54, 76), mmW = clamp(w / 9, 140, 190);
        int contentW = sideW + gap + rulerW + gap + m.boardW + gap + mmW;
        int leftG = Math.max(margin, (w - contentW) / 2);
        // Absorb the odd leftover pixel into the right column so the report shows L == R.
        int rightX0 = m.boardX + m.boardW + gap;
        int mmW2 = (w - contentW >= 2 * margin)
                ? Math.max(120, w - leftG - rightX0) : mmW;
        int rightG = w - (rightX0 + mmW2);
        int arenaRows = lv.heapLimit / lv.rowUnits;
        int[] fs = fontSizes(h);
        return String.format(
                "content=%dx%d cell=%dpx board=%dx%d fill=%.0f%% rows=%d (arena %d) cols=%d "
                + "| gutters L=%d R=%d | fonts score=%d key=%d title=%d helper=%d",
                w, h, m.cell, m.boardW, m.boardH, 100.0 * m.boardH / h, rows, arenaRows, cols,
                leftG, rightG, fs[0], fs[1], fs[2], fs[3]);
    }

    private void ensureLayout() {
        Insets in = getInsets();
        int w = Math.max(0, getWidth() - in.left - in.right);
        int h = Math.max(0, getHeight() - in.top - in.bottom);
        if (w == lastW && h == lastH) return;
        lastW = w;
        lastH = h;
        recalcLayout(w, h);
        if (!metricsPrinted && w > 0 && h > 0) {
            metricsPrinted = true;
            System.out.println("SNAKE LAYOUT window=" + windowSize() + "  " + metricsLine(w, h, model.level));
            // (a) and (c): prove the layout height IS the real content height, and
            // report the rendered pixel size of every font tier.
            System.out.println(fitLine(w, h));
            System.out.println("SNAKE FONTS " + fontMetricsLine(h));
            System.out.println(textBudgetLine());
        }
    }

    /**
     * (a) Physical fit report. The panel fills the content pane's CENTER slot, so
     * its own content height is the height every strip is laid out against - this
     * prints both numbers plus the frame and the usable screen, so a clipped or
     * off-screen strip can be spotted immediately.
     */
    private String fitLine(int w, int h) {
        java.awt.Window win = SwingUtilities.getWindowAncestor(this);
        java.awt.Container cp = getParent();
        String cpSize = cp == null ? "?" : (cp.getWidth() + "x" + cp.getHeight());
        String winSize = win == null ? "?" : (win.getWidth() + "x" + win.getHeight());
        String insets = "?";
        String usable = "?";
        String geom = "?";
        String header = cp == null ? "?" : ((cp.getHeight() - getHeight()) + "px");
        if (win != null) {
            Insets wi = win.getInsets();
            insets = wi.top + "," + wi.bottom;
            java.awt.GraphicsConfiguration gc = win.getGraphicsConfiguration();
            if (gc != null) {
                java.awt.Rectangle b = gc.getBounds();
                Insets si = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(gc);
                int usableTop = b.y + si.top, usableBottom = b.y + b.height - si.bottom;
                usable = (b.width - si.left - si.right) + "x" + (b.height - si.top - si.bottom)
                        + "(taskbar " + si.bottom + "px)";
                if (isShowing()) {
                    java.awt.Point p = getLocationOnScreen();
                    int panelBottom = p.y + getHeight();
                    geom = "panelY=" + p.y + " panelBottom=" + panelBottom
                            + " usableTop=" + usableTop + " usableBottom=" + usableBottom
                            + " underTaskbar=" + Math.max(0, panelBottom - usableBottom) + "px";
                }
            }
        }
        Insets pi = getInsets();
        int bottomGap = h - (footY + footH);
        return "SNAKE FIT panel=" + getWidth() + "x" + getHeight()
                + " panelInsets=" + pi.top + "," + pi.left + "," + pi.bottom + "," + pi.right
                + " contentPane=" + cpSize + " header=" + header
                + " frame=" + winSize + " frameInsets(t,b)=" + insets
                + " content=" + w + "x" + h
                + " board=" + boardX + "," + boardY + " " + boardW + "x" + boardH
                + " leftCol=" + leftX + "," + leftY + " " + sideW + "x" + leftH
                + " rightCol=" + rightX + "," + rightY + " " + mmW + "x" + rightH
                + " hud=" + hudX + "," + hudY + " " + hudW + "x" + hudH
                + " bottomGap=" + bottomGap + "px screenUsable=" + usable + " " + geom;
    }

    private String windowSize() {
        java.awt.Window win = SwingUtilities.getWindowAncestor(this);
        return win == null ? "?" : (win.getWidth() + "x" + win.getHeight());
    }

    private void recalcLayout(int w, int h) {
        margin = 14;
        gap = 10;
        hudH = clamp(h / 11, 62, 92);
        logH = clamp(h / 22, 30, 42);
        footH = clamp(h / 24, 30, 40);
        sideW = clamp(w / 7, 190, 250);
        rulerW = clamp(w / 26, 54, 76);
        mmW = clamp(w / 9, 140, 190);

        Metrics m = compute(w, h, model.cols, model.rows);
        cell = m.cell;
        boardX = m.boardX;
        boardY = m.boardY;
        boardW = m.boardW;
        boardH = m.boardH;

        contentW = sideW + gap + rulerW + gap + boardW + gap + mmW;

        // Side panels are aligned to the board's top and bottom - not to the window.
        leftX = boardX - gap - rulerW - gap - sideW;
        leftY = boardY;
        leftH = boardH;
        rulerX = boardX - gap - rulerW;
        rightX = boardX + boardW + gap;
        rightY = boardY;
        rightH = boardH;

        // STEP 5 B: absorb the odd leftover pixel into the right column so the left
        // and right gutters are exactly equal (the layout report prints L == R).
        if (w - contentW >= 2 * margin) {
            mmW = Math.max(120, w - leftX - rightX);
            contentW = sideW + gap + rulerW + gap + boardW + gap + mmW;
        }

        // TASK 1.3: the HUD owns a reserved band directly above the board, so the
        // board's top rows - where the stack will grow - are never under the HUD.
        hudX = boardX;
        hudY = m.hudY;
        hudW = boardW;

        // FIX 2: reserve the bottom strip first - footer, then the kernel ticker -
        // so the board can never be drawn over them or past the bottom edge.
        footY = h - margin - footH;
        logY = footY - gap - logH;

        int[] fs = fontSizes(h);
        fScore  = new Font("Consolas", Font.BOLD, fs[0]);
        fKey    = new Font("Consolas", Font.BOLD, fs[1]);
        fTitle  = new Font("Consolas", Font.BOLD, fs[2]);
        fHelper = new Font("Consolas", Font.BOLD, fs[3]);
    }

    /**
     * STEP 5 B - the one type scale: score, key value, panel title, helper text.
     * Written as the ink heights the spec asks for (12/14/20/28 px) converted to
     * the nominal sizes that actually paint them in Consolas.
     */
    static int[] fontSizes(int h) {
        return new int[]{SZ_SCORE, SZ_KEY, SZ_TITLE, SZ_HELPER};
    }

    /**
     * Rendered pixel metrics of the type scale: for each tier the nominal size,
     * the font ascent, the actual ink height of a capital "H" and the full line
     * height. Capitals are measured with a GlyphVector, so these are painted
     * pixels, not the logical line box.
     */
    static String fontMetricsLine(int h) {
        int[] fs = fontSizes(h);
        String[] names = {"score", "key", "title", "helper"};
        StringBuilder sb = new StringBuilder();
        java.awt.image.BufferedImage bi =
                new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D gg = bi.createGraphics();
        for (int i = 0; i < fs.length; i++) {
            Font f = new Font("Consolas", Font.BOLD, fs[i]);
            FontMetrics fm = gg.getFontMetrics(f);
            java.awt.font.GlyphVector gv = f.createGlyphVector(gg.getFontRenderContext(), "H");
            int cap = gv.getPixelBounds(gg.getFontRenderContext(), 0f, 0f).height;
            if (i > 0) sb.append("  ");
            sb.append(names[i]).append("=").append(fs[i]).append("px(ink=").append(cap)
              .append(",asc=").append(fm.getAscent()).append(",line=").append(fm.getHeight()).append(")");
        }
        gg.dispose();
        sb.append("  | ink targets: helper>=12 title=14 key=20 score=28");
        return sb.toString();
    }

    // =====================================================================
    // PAINT
    // =====================================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        ensureLayout();
        Insets in = getInsets();
        int w = Math.max(0, getWidth() - in.left - in.right);
        int h = Math.max(0, getHeight() - in.top - in.bottom);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2.translate(in.left, in.top);
        long t0 = System.nanoTime();
        render(g2, w, h);
        double ms = (System.nanoTime() - t0) / 1e6;
        // Exponential smoothing: react fast, report steady.
        frameMs = frameMs == 0.0 ? ms : frameMs * 0.85 + ms * 0.15;
        if (System.currentTimeMillis() >= frameReportAt) {
            frameReportAt = System.currentTimeMillis() + 2000;
            System.out.printf("SNAKE FRAME render=%.2f ms (smoothed)  budget=16 ms%n", frameMs);
        }
        g2.dispose();
    }

    private void render(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(8, 6, 18));
        g2.fillRect(0, 0, w, h);

        // Board + everything on it shake together on a fault.
        Graphics2D bg = (Graphics2D) g2.create();
        bg.translate(shakeX, shakeY);
        renderBoard(bg);
        renderPickups(bg);
        renderSnake(bg);
        renderParticles(bg);
        renderFloats(bg);
        bg.dispose();

        renderRuler(g2);
        renderGauges(g2);
        renderMinimap(g2);
        renderHud(g2);
        renderTicker(g2);
        renderFooter(g2);
        if (showAdvanced) renderAdvanced(g2);

        // STEP 5 C: the CRT pass - vignette + scanlines - sits over the cabinet but
        // under the end/help cards so those stay perfectly readable.
        renderCrt(g2, w, h);
        renderBanner(g2, w, h);
        renderReturnMeter(g2);
        renderCallout(g2);

        // C1: Render briefing card before gameplay overlays
        if (briefingActive) renderBriefing(g2, w, h);

        // Overlays last: end-of-level first, then the help / pause card on top.
        renderOverlay(g2, w, h);
        if (showHelp || model.paused) renderHelp(g2, w, h);
    }

    // =====================================================================
    // STEP 5 C - LOOK: bloom, motion, CRT
    // =====================================================================
    /** Soft vignette plus optional CRT scanlines (F2). One pass, very cheap. */
    private void renderCrt(Graphics2D g2, int w, int h) {
        if (w <= 0 || h <= 0) return;
        g2.setPaint(new RadialGradientPaint(
                new Point2D.Float(w / 2f, h / 2f), Math.max(w, h) * 0.72f,
                new float[]{0.55f, 1f},
                new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 130)}));
        g2.fillRect(0, 0, w, h);
        g2.setPaint(null);

        if (scanlines) {
            g2.setColor(new Color(0, 0, 0, 26));
            for (int y = 0; y < h; y += 3) g2.drawLine(0, y, w, y);
        }
    }

    /** The 1.5 s level-start banner: a short callout that fades out on its own. */
    private void renderBanner(Graphics2D g2, int w, int h) {
        long left = bannerUntilMs - System.currentTimeMillis();
        if (left <= 0) return;
        float a = Math.min(1f, left / 450f);
        String s = "LEVEL " + model.level.number + " \u2014 "
                + model.level.name.toUpperCase();
        g2.setFont(fTitle);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(s);
        int cx = boardX + boardW / 2, cy = boardY + boardH / 2;
        int padX = 18, padY = 10;
        g2.setColor(new Color(10, 8, 24, (int) (215 * a)));
        g2.fillRoundRect(cx - tw / 2 - padX, cy - fm.getAscent() - padY,
                tw + 2 * padX, fm.getHeight() + 2 * padY, 10, 10);
        g2.setColor(new Color(RetroTheme.ACCENT_CYAN.getRed(), RetroTheme.ACCENT_CYAN.getGreen(),
                RetroTheme.ACCENT_CYAN.getBlue(), (int) (255 * a)));
        g2.drawRoundRect(cx - tw / 2 - padX, cy - fm.getAscent() - padY,
                tw + 2 * padX, fm.getHeight() + 2 * padY, 10, 10);
        g2.drawString(s, cx - tw / 2, cy);
    }

    /** PART 3: SPACE's cooldown, as a small bar just above the footer. */
    private void renderReturnMeter(Graphics2D g2) {
        SnakeLevel lv = model.level;
        if (!lv.stackEnabled || model.phase != SnakeModel.Phase.PLAYING) return;
        int bw = clamp(boardW / 5, 90, 200);
        int x = boardX + boardW - bw - 8;
        int y = footY - 15;
        int h = 7;
        g2.setColor(new Color(10, 8, 22, 210));
        g2.fillRoundRect(x, y, bw, h, 4, 4);
        g2.setColor(new Color(60, 50, 96));
        g2.drawRoundRect(x, y, bw, h, 4, 4);
        double frac = lv.returnCooldownMs <= 0 ? 0
                : Math.max(0, Math.min(1, model.returnCdMs / (double) lv.returnCooldownMs));
        int fw = (int) Math.round((1 - frac) * (bw - 2));
        Color c = model.returnCdMs <= 0 ? SnakeLevel.C_STACK : RetroTheme.ACCENT_YELLOW;
        if (returnDeniedMs > 0) c = pulseRed();      // the denied pulse
        g2.setColor(c);
        g2.fillRect(x + 1, y + 1, Math.max(1, fw), h - 2);
    }

    /**
     * PART 3: the one-time first-play callout. A magenta spotlight sits on the
     * stack frontier and a single line explains the RETURN key; the first move
     * dismisses it for the rest of the run (Restart brings it back).
     */
    private void renderCallout(Graphics2D g2) {
        SnakeLevel lv = model.level;
        if (calloutDismissed || showHelp || model.paused || model.phase != SnakeModel.Phase.PLAYING) return;

        if (model.isSegmentSwitchLevel()) {
            stackCalloutShown = true;
            // Spotlight: soft cyan glow band across the top of segment panels
            Shape old = g2.getClip();
            g2.clipRect(boardX, boardY, boardW, boardH);
            double t = (System.currentTimeMillis() % 1200) / 1200.0;
            int glow = (int) (45 + 45 * Math.abs(0.5 - t) * 2);
            g2.setColor(new Color(5, 217, 232, glow));
            g2.fillRect(boardX, boardY, boardW, 36);
            g2.setClip(old);

            String msg = "PRESS 1-2-3 TO LOAD A SEGMENT. SAME OFFSET, DIFFERENT LIMIT.";
            g2.setFont(fHelper);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(msg);
            int cx = boardX + boardW / 2;
            int cy = boardY + 54;
            int padX = 14, padY = 8;
            g2.setColor(new Color(10, 8, 24, 235));
            g2.fillRoundRect(cx - tw / 2 - padX, cy - fm.getAscent() - padY,
                    tw + 2 * padX, fm.getHeight() + 2 * padY, 8, 8);
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawRoundRect(cx - tw / 2 - padX, cy - fm.getAscent() - padY,
                    tw + 2 * padX, fm.getHeight() + 2 * padY, 8, 8);
            g2.drawString(msg, cx - tw / 2, cy);
            return;
        }

        if (!lv.stackEnabled) return;
        stackCalloutShown = true;

        int frontY = yOf(model.stackFrontier());
        double t = (System.currentTimeMillis() % 1200) / 1200.0;
        int glow = (int) (45 + 45 * Math.abs(0.5 - t) * 2);

        // Spotlight: a soft magenta band across the frontier, clipped to the board.
        Shape old = g2.getClip();
        g2.clipRect(boardX, boardY, boardW, boardH);
        g2.setColor(new Color(255, 42, 141, glow));
        int top = Math.max(boardY, frontY - 14);
        g2.fillRect(boardX, top, boardW, Math.min(boardY + boardH - top, 28));
        g2.setClip(old);

        String msg = "THE STACK GROWS DOWN. PRESS SPACE TO RETURN.";
        g2.setFont(fHelper);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(msg);
        int cx = boardX + boardW / 2;
        int cy = clamp(frontY + 26, boardY + 24, boardY + boardH - 12);
        int padX = 12, padY = 7;
        g2.setColor(new Color(10, 8, 24, 232));
        g2.fillRoundRect(cx - tw / 2 - padX, cy - fm.getAscent() - padY,
                tw + 2 * padX, fm.getHeight() + 2 * padY, 8, 8);
        g2.setColor(SnakeLevel.C_STACK);
        g2.drawRoundRect(cx - tw / 2 - padX, cy - fm.getAscent() - padY,
                tw + 2 * padX, fm.getHeight() + 2 * padY, 8, 8);
        g2.drawString(msg, cx - tw / 2, cy);
    }

    /** Watches the model each frame and turns state changes into motion. */
    private void updateEffects(double dtMs) {
        long now = System.currentTimeMillis();

        if (model.mallocsEaten > fxMallocs) {
            int[] h = model.body.peekFirst();
            if (h != null) {
                int cx = cellX(h[0]) + cell / 2, cy = cellY(h[1]) + cell / 2;
                burst(cx, cy, 12, 255, 240, 150, 0.045);
                // D1: Edge pickups award double points (+20 instead of +10)
                int pts = model.lastPickupWasEdge ? SnakeModel.MALLOC_POINTS * 2 : SnakeModel.MALLOC_POINTS;
                floats.add(new double[]{cx, cy - cell * 0.3, 700, pts, model.headPhysicalAddress()});
            }
        }
        fxMallocs = model.mallocsEaten;

        if (model.hearts < fxHearts) {
            int[] h = model.body.peekFirst();
            if (h != null) burst(cellX(h[0]) + cell / 2, cellY(h[1]) + cell / 2,
                    18, 255, 60, 90, 0.075);
        }
        fxHearts = model.hearts;

        if (model.phase == SnakeModel.Phase.GAME_OVER
                && fxPhase != SnakeModel.Phase.GAME_OVER) {
            deathMs = now;
        }
        fxPhase = model.phase;

        // ── PART 3: stack log lines, the critical-gap alarm and the denied pulse ──
        if (model.stackFrames != seenStackFrames) {
            int d = model.stackFrames - seenStackFrames;
            seenStackFrames = model.stackFrames;
            if (!stackLogSuppressed) {
                if (d > 0) pushLog("stack: push frame" + (d > 1 ? " x" + d : ""), LogKind.WARN);
                else pushLog("stack: return x" + (-d), LogKind.OK);
            }
        }
        stackLogSuppressed = false;

        boolean crit = model.phase == SnakeModel.Phase.PLAYING
                && model.level.stackEnabled && model.freeGapFrames() <= 2;
        if (crit) {
            if (!alarmLatched) {
                alarmLatched = true;               // one shot when the gap first goes critical
                alarmBeepAcc = 1100;
                GameSfx.alarm();
            } else {
                alarmBeepAcc -= dtMs;
                if (alarmBeepAcc <= 0) { alarmBeepAcc = 1100; GameSfx.alarm(); }
            }
        } else {
            alarmLatched = false;
        }
        if (returnDeniedMs > 0) returnDeniedMs = Math.max(0, returnDeniedMs - dtMs);

        // TASK 2: write the finished run to the score table once, then refresh the
        // table and best rank the end screen shows (guest mode writes nothing).
        if (model.phase != SnakeModel.Phase.PLAYING && !runRecorded) {
            runRecorded = true;
            SnakeScores.record(model.score, model.rank(), SnakeScores.today());
            hiScores = SnakeScores.load();
            bestRank = SnakeScores.bestRank();
        }

        for (int i = fx.size() - 1; i >= 0; i--) {
            double[] p = fx.get(i);
            p[4] -= dtMs;
            if (p[4] <= 0) { fx.remove(i); continue; }
            p[0] += p[2] * dtMs;
            p[1] += p[3] * dtMs;
            p[3] += 0.0003 * dtMs;                     // a touch of gravity
        }
        for (int i = floats.size() - 1; i >= 0; i--) {
            double[] f = floats.get(i);
            f[2] -= dtMs;
            if (f[2] <= 0) { floats.remove(i); continue; }
            f[1] -= 0.03 * dtMs;                       // drift up
        }
    }

    /** Spawns {@code n} spark particles at (x,y); {@code speed} is px per ms. */
    private void burst(int x, int y, int n, int r, int g, int b, double speed) {
        for (int i = 0; i < n; i++) {
            double ang = Math.random() * Math.PI * 2;
            double sp = speed * (0.35 + Math.random());
            fx.add(new double[]{x, y, Math.cos(ang) * sp, Math.sin(ang) * sp,
                    380 + Math.random() * 260, 640, r, g, b});
        }
        if (fx.size() > 240) fx.subList(0, fx.size() - 240).clear();
    }

    private void renderParticles(Graphics2D g2) {
        for (double[] p : fx) {
            int a = (int) (235 * Math.max(0, Math.min(1, p[4] / p[5])));
            g2.setColor(new Color((int) p[6], (int) p[7], (int) p[8], a));
            int s = Math.max(2, cell / 5);
            g2.fillRect((int) p[0] - s / 2, (int) p[1] - s / 2, s, s);
        }
    }

    private void renderFloats(Graphics2D g2) {
        g2.setFont(fHelper);
        FontMetrics fm = g2.getFontMetrics();
        for (double[] f : floats) {
            int a = (int) (255 * Math.max(0, Math.min(1, f[2] / 700)));
            int pts = f.length > 3 ? (int) f[3] : SnakeModel.MALLOC_POINTS;
            String s = "+" + pts;
            int fxPos = (int) f[0];
            int fyPos = (int) f[1];

            // Render score label
            g2.setColor(new Color(0, 0, 0, (int) (a * 0.6)));
            g2.drawString(s, fxPos - fm.stringWidth(s) / 2 + 1, fyPos + 1);
            g2.setColor(new Color(255, 240, 150, a));
            g2.drawString(s, fxPos - fm.stringWidth(s) / 2, fyPos);

            // D2 & P2: Floating "PA ..." label with distinct vertical gap and offset
            if (f.length > 4) {
                int paVal = (int) f[4];
                String paStr = "PA " + paVal;
                int paY = fyPos - fm.getHeight() - 4;
                g2.setColor(new Color(0, 0, 0, (int) (a * 0.6)));
                g2.drawString(paStr, fxPos - fm.stringWidth(paStr) / 2 + 1, paY + 1);
                g2.setColor(new Color(80, 220, 255, a));
                g2.drawString(paStr, fxPos - fm.stringWidth(paStr) / 2, paY);
            }
        }
    }

    // ── Board: the memory dump, taught by texture and colour, not by words ──
    private int yOf(int addr) {
        double frac = addr / (double) model.level.addressSpace;
        return boardY + boardH - (int) Math.round(frac * boardH);
    }

    private void band(Graphics2D g2, int addrLo, int addrHi, Color c) {
        int yTop = yOf(addrHi);
        int yBot = yOf(addrLo);
        g2.setColor(c);
        g2.fillRect(boardX, yTop, boardW, Math.max(1, yBot - yTop));
    }

    /** Diagonal hatch - the texture that means "unmapped" everywhere else too. */
    private void hatchBand(Graphics2D g2, int addrLo, int addrHi, Color c, int step) {
        int yTop = yOf(addrHi);
        int yBot = yOf(addrLo);
        int hh = Math.max(1, yBot - yTop);
        Shape old = g2.getClip();
        g2.clipRect(boardX, yTop, boardW, hh);
        g2.setColor(c);
        g2.setStroke(new BasicStroke(1f));
        for (int x = boardX - hh; x < boardX + boardW; x += step) {
            g2.drawLine(x, yBot, x + hh, yTop);
        }
        g2.setClip(old);
    }

    /** Fine vertical rules - the "read-only ROM" texture for CODE. */
    private void verticalRules(Graphics2D g2, int addrLo, int addrHi, Color c, int step) {
        int yTop = yOf(addrHi);
        int yBot = yOf(addrLo);
        Shape old = g2.getClip();
        g2.clipRect(boardX, yTop, boardW, Math.max(1, yBot - yTop));
        g2.setColor(c);
        for (int x = boardX; x < boardX + boardW; x += step) g2.drawLine(x, yTop, x, yBot);
        g2.setClip(old);
    }

    /** Fine horizontal rules - the "cell rows" texture for DATA. */
    private void horizontalRules(Graphics2D g2, int addrLo, int addrHi, Color c, int step) {
        int yTop = yOf(addrHi);
        int yBot = yOf(addrLo);
        Shape old = g2.getClip();
        g2.clipRect(boardX, yTop, boardW, Math.max(1, yBot - yTop));
        g2.setColor(c);
        for (int y = yTop; y < yBot; y += step) g2.drawLine(boardX, y, boardX + boardW, y);
        g2.setClip(old);
    }

    /** A faint memory-cell texture: the heap looks like a page of allocated cells. */
    private void memoryCells(Graphics2D g2, int addrLo, int addrHi) {
        int yTop = yOf(addrHi), yBot = yOf(addrLo);
        Shape old = g2.getClip();
        g2.clipRect(boardX, yTop, boardW, Math.max(1, yBot - yTop));
        int step = Math.max(6, cell / 2);
        g2.setColor(new Color(SnakeLevel.C_HEAP.getRed(), SnakeLevel.C_HEAP.getGreen(),
                SnakeLevel.C_HEAP.getBlue(), 14));
        for (int y = yTop; y < yBot; y += step) {
            int off = ((y / step) % 2 == 0) ? 0 : step / 2;
            for (int x = boardX + off; x < boardX + boardW; x += step) {
                g2.fillRect(x, y, step / 2, step / 2);
            }
        }
        g2.setClip(old);
    }

    /** A glowing laser barrier: three stacked translucent strokes make the bloom. */
    private void barrier(Graphics2D g2, int y, Color c, float strength) {
        int a = (int) (230 * strength);
        g2.setStroke(new BasicStroke(6f));
        g2.setColor(alpha(c, Math.max(8, a / 8)));
        g2.drawLine(boardX, y, boardX + boardW, y);
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(alpha(c, Math.max(16, a / 3)));
        g2.drawLine(boardX, y, boardX + boardW, y);
        g2.setStroke(new BasicStroke(1.6f));
        g2.setColor(alpha(c, a));
        g2.drawLine(boardX, y, boardX + boardW, y);
        g2.setStroke(new BasicStroke(1f));
    }

    /**
     * Barrier strength for a heap edge: the resting {@code base} value, plus a
     * pulsing flare when the head's row is within one cell of the edge. The
     * pulse is a slow sine so the line breathes rather than flickers.
     */
    private float flareStrength(int headRow, int edgeRow, float base) {
        if (headRow < 0) return base;
        int dist = Math.abs(headRow - edgeRow);
        if (dist > 1) return base;
        double t = (System.currentTimeMillis() % 700) / 700.0;
        float pulse = 0.35f + 0.25f * (float) Math.sin(t * Math.PI * 2);
        return Math.min(1f, base + pulse);
    }

    private void renderBoard(Graphics2D g2) {
        if (model.isSegmentSwitchLevel()) {
            renderSegmentSwitchBoard(g2);
            return;
        }

        SnakeLevel lv = model.level;
        int top = model.heapTop();
        int stackFront = model.stackFrontier();
        int teaserLo = Math.max(0, lv.addressSpace - STACK_TEASER_ROWS * lv.rowUnits);

        // Base fill = unmapped address space, hatched so it never reads as blank.
        g2.setColor(SnakeLevel.C_FREE);
        g2.fillRect(boardX, boardY, boardW, boardH);
        hatchBand(g2, 0, lv.addressSpace, new Color(34, 28, 62), 9);

        // CODE: read-only, so it gets a fine vertical rule texture.
        band(g2, lv.codeBase, lv.codeBase + lv.codeLimit, alpha(SnakeLevel.C_CODE, 34));
        verticalRules(g2, lv.codeBase, lv.codeBase + lv.codeLimit, alpha(SnakeLevel.C_CODE, 58), 6);

        // DATA: read/write, so it gets a cell-row texture.
        band(g2, lv.dataBase, lv.dataBase + lv.dataLimit, alpha(SnakeLevel.C_DATA, 26));
        horizontalRules(g2, lv.dataBase, lv.dataBase + lv.dataLimit, alpha(SnakeLevel.C_DATA, 44), 7);

        // HEAP: the live arena, with a subtle memory-cell texture. TASK 1.1:
        // the band and the cell texture are dimmed so the bright snake reads on top.
        band(g2, model.heapBase, top, alpha(SnakeLevel.C_HEAP, 16));
        memoryCells(g2, model.heapBase, top);

        // STACK: a dim teaser when inactive; the real pushed frames when active.
        if (!lv.stackEnabled) band(g2, teaserLo, lv.addressSpace, alpha(SnakeLevel.C_STACK, 22));
        else renderStackFrames(g2);

        // Grid: darker and subtler than before.
        g2.setColor(new Color(22, 18, 40));
        for (int c = 0; c <= model.cols; c++) {
            int x = boardX + c * cell;
            g2.drawLine(x, boardY, x, boardY + boardH);
        }
        for (int r = 0; r <= model.rows; r++) {
            int y = boardY + r * cell;
            g2.drawLine(boardX, y, boardX + boardW, y);
        }

        // Heap base and limit: glowing barriers. The limit flares when the head
        // is within one cell of it (the danger zone), and the base flares the
        // same way when the head drops near it. No words - hover explains them.
        int headRow = -1;
        int[] headCell = model.body.peekFirst();
        if (headCell != null) headRow = headCell[1];
        int baseRow = model.heapBase / lv.rowUnits;
        int topRow = top / lv.rowUnits;
        float baseFlare = flareStrength(headRow, baseRow, 0.55f);
        float topFlare  = flareStrength(headRow, topRow, 0.75f);
        barrier(g2, yOf(model.heapBase), SnakeLevel.C_HEAP, baseFlare);
        barrier(g2, yOf(top), SnakeLevel.C_HEAP, topFlare);
        if (!lv.stackEnabled) {
            barrier(g2, yOf(teaserLo), SnakeLevel.C_STACK, 0.30f);
        } else {
            // PART 3: the frontier glows, and pulses hard when the gap is critical.
            boolean crit = model.phase == SnakeModel.Phase.PLAYING && model.freeGapFrames() <= 2;
            float fs = crit
                    ? 0.8f + 0.2f * (float) Math.abs(Math.sin(System.currentTimeMillis() / 200.0))
                    : 0.8f;
            barrier(g2, yOf(stackFront), SnakeLevel.C_STACK, Math.min(1f, fs));
        }

        g2.setColor(RetroTheme.BORDER_BRIGHT);
        g2.drawRect(boardX, boardY, boardW, boardH);

        // PART 3: a thin red border pulses around the board when the gap is critical.
        if (lv.stackEnabled && model.phase == SnakeModel.Phase.PLAYING && model.freeGapFrames() <= 2) {
            int a = (int) (70 + 140 * Math.abs(Math.sin(System.currentTimeMillis() / 220.0)));
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255, 60, 90, Math.max(0, Math.min(220, a))));
            g2.drawRect(boardX + 1, boardY + 1, boardW - 2, boardH - 2);
            g2.setStroke(new BasicStroke(1f));
        }

        if (model.faultFlashMs > 0) {
            int a = (int) (120 * (model.faultFlashMs / 320.0));
            g2.setColor(new Color(255, 40, 80, Math.max(0, Math.min(160, a))));
            g2.fillRect(boardX, boardY, boardW, boardH);
        }
    }

    /**
     * PART 3: the pushed frames, drawn full-cell from the very top of the board
     * downward. Each frame is a magenta bar with a thick lower edge; no text - the
     * hover tooltip names the frame. Frames never reach the reserved HUD band
     * because every row is clamped to the board rectangle.
     */
    private void renderStackFrames(Graphics2D g2) {
        SnakeLevel lv = model.level;
        int n = model.stackFrames;
        if (n <= 0) return;
        for (int i = 0; i < n; i++) {
            int hiAddr = lv.stackTop - i * lv.frameUnits();
            int loAddr = hiAddr - lv.frameUnits();
            int yTop = Math.max(boardY, yOf(hiAddr));
            int yBot = Math.min(boardY + boardH, yOf(loAddr));
            if (yBot <= yTop) break;
            // Newest frame (i == 0) sits right under the frontier, so it is brightest.
            int a = i == 0 ? 78 : 46;
            g2.setColor(alpha(SnakeLevel.C_STACK, a));
            g2.fillRect(boardX, yTop, boardW, yBot - yTop);
            // A thick lower edge marks each frame boundary.
            g2.setColor(alpha(SnakeLevel.C_STACK, i == 0 ? 190 : 120));
            g2.fillRect(boardX, Math.max(boardY, yBot - 3), boardW, Math.min(3, yBot - boardY));
        }
    }

    /**
     * TASK 3B: Level 3 Segment Switch Board.
     * Three segment panels side by side, 8 columns each, bottom-aligned so offset 0
     * is at the bottom of every panel. Each panel's height is its own limit in rows.
     * Above a panel's limit is a hatched invalid offsets zone. The active segment glows;
     * each panel has a color chip with its number (1, 2, 3).
     */
    private void renderSegmentSwitchBoard(Graphics2D g2) {
        SnakeLevel lv = model.level;
        int segCount = lv.segments.size();

        g2.setColor(SnakeLevel.C_FREE);
        g2.fillRect(boardX, boardY, boardW, boardH);

        for (int i = 0; i < segCount; i++) {
            SnakeLevel.SegmentDef seg = lv.segments.get(i);
            int px = boardX + i * 8 * cell;
            int pw = 8 * cell;
            int segRows = seg.limit / lv.rowUnits;
            int validH = segRows * cell;
            int validY = boardY + boardH - validH;

            // Hatched invalid zone above limit
            if (validY > boardY) {
                hatchRect(g2, px, boardY, pw, validY - boardY, new Color(42, 32, 58, 160), 8);
                g2.setFont(fHelper);
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(new Color(110, 80, 100, 80));
                String invStr = "INVALID [> " + seg.limit + "]";
                if (pw >= fm.stringWidth(invStr) + 8 && validY - boardY >= 20) {
                    g2.drawString(invStr, px + (pw - fm.stringWidth(invStr)) / 2, boardY + (validY - boardY + fm.getAscent()) / 2);
                }
            }

            // Valid segment area
            Color segBg = (i == 0) ? new Color(16, 26, 40)
                        : (i == 1) ? new Color(32, 28, 18)
                        : new Color(34, 18, 36);
            g2.setColor(segBg);
            g2.fillRect(px, validY, pw, validH);

            // Subtle memory cell grid inside valid area
            g2.setColor(new Color(50, 42, 75, 75));
            for (int r = 0; r < segRows; r++) {
                int ry = boardY + boardH - (r + 1) * cell;
                for (int c = 0; c < 8; c++) {
                    int cx = px + c * cell;
                    g2.fillRect(cx + cell / 2 - 1, ry + cell / 2 - 1, 2, 2);
                }
            }

            // Limit barrier line at validY
            barrier(g2, validY, seg.readOnly ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_CYAN, 0.65f);

            // Active segment glow
            boolean isActive = (i == model.activeSegmentIndex);
            if (isActive) {
                g2.setColor(new Color(5, 217, 232, 45));
                g2.fillRect(px, validY, pw, validH);
                g2.setStroke(new BasicStroke(2.5f));
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.drawRect(px + 1, validY + 1, pw - 2, validH - 2);
                g2.setStroke(new BasicStroke(1f));
            } else {
                g2.setColor(new Color(55, 45, 85));
                g2.drawRect(px, validY, pw, validH);
            }

            // Number chip (1, 2, 3)
            int chipW = 22, chipH = 18;
            int chipX = px + 6;
            int chipY = validY + 6;
            g2.setColor(isActive ? RetroTheme.ACCENT_CYAN : new Color(24, 18, 44));
            g2.fillRoundRect(chipX, chipY, chipW, chipH, 5, 5);
            g2.setColor(isActive ? Color.BLACK : RetroTheme.ACCENT_CYAN);
            g2.setFont(fTitle);
            FontMetrics cfm = g2.getFontMetrics();
            String numStr = String.valueOf(seg.id);
            g2.drawString(numStr, chipX + (chipW - cfm.stringWidth(numStr)) / 2, chipY + cfm.getAscent() + 1);

            // Segment Name & Perm
            g2.setFont(fHelper);
            FontMetrics hfm = g2.getFontMetrics();
            g2.setColor(isActive ? RetroTheme.TEXT_WHITE : RetroTheme.TEXT_DIM);
            String nameStr = seg.name;
            g2.drawString(nameStr, chipX + chipW + 6, chipY + hfm.getAscent() + 1);

            if (seg.readOnly) {
                String roStr = "[R-ONLY]";
                g2.setColor(RetroTheme.ACCENT_PINK);
                g2.drawString(roStr, px + pw - hfm.stringWidth(roStr) - 6, chipY + hfm.getAscent() + 1);
            }
        }

        // Grid lines across columns
        g2.setColor(new Color(22, 18, 40));
        for (int c = 0; c <= model.cols; c++) {
            int x = boardX + c * cell;
            g2.drawLine(x, boardY, x, boardY + boardH);
        }
        for (int r = 0; r <= model.rows; r++) {
            int y = boardY + r * cell;
            g2.drawLine(boardX, y, boardX + boardW, y);
        }

        // Outer board border
        g2.setColor(RetroTheme.BORDER_BRIGHT);
        g2.drawRect(boardX, boardY, boardW, boardH);

        if (model.faultFlashMs > 0) {
            int a = (int) (120 * (model.faultFlashMs / 320.0));
            g2.setColor(new Color(255, 40, 80, Math.max(0, Math.min(160, a))));
            g2.fillRect(boardX, boardY, boardW, boardH);
        }
    }

    private static void hatchRect(Graphics2D g2, int x, int y, int w, int h, Color c, int step) {
        Shape old = g2.getClip();
        g2.clipRect(x, y, w, h);
        g2.setColor(c);
        for (int p = -h; p < w; p += step) {
            g2.drawLine(x + p, y + h, x + p + h, y);
        }
        g2.setClip(old);
    }

    // ── Snake ────────────────────────────────────────────────────────────
    private int cellX(int col) { return boardX + col * cell; }
    private int cellY(int row)  { return boardY + boardH - (row + 1) * cell; }

    private void renderSnake(Graphics2D g2) {
        List<int[]> cells = new ArrayList<>(model.body);
        int n = cells.size();
        boolean blink = model.invulnMs > 0 && ((int) (model.invulnMs / 120) % 2 == 0);

        // Direction of travel, for the eyes and the head bloom.
        int dx = 1, dy = 0;
        if (n >= 2) {
            dx = Integer.signum(cells.get(0)[0] - cells.get(1)[0]);
            dy = Integer.signum(cells.get(0)[1] - cells.get(1)[1]);
            if (dx == 0 && dy == 0) { dx = 1; }
        }

        // Death animation: the whole body fades out and shrinks away.
        float death = 0f;
        if (model.phase == SnakeModel.Phase.GAME_OVER && deathMs > 0) {
            death = Math.min(1f, (System.currentTimeMillis() - deathMs) / 900f);
        }

        int pad = Math.max(1, cell / 8);
        int arc = Math.max(4, cell / 3);

        // Tail first, so the head paints on top of the neck.
        for (int i = n - 1; i >= 0; i--) {
            int[] c = cells.get(i);
            int x = cellX(c[0]), y = cellY(c[1]);
            if (death > 0) {
                int shrink = (int) (cell * 0.5 * death);
                x += shrink / 2;
                y += shrink / 2;
            }
            double t = n <= 1 ? 0 : i / (double) (n - 1);
            // TASK 1.1: keep the tail bright - a high alpha floor so the last
            // segment still reads against the heap texture instead of fading out.
            int bodyA = (int) ((255 - 70 * t) * (1 - death));
            if (i == 0) {
                drawHead(g2, x, y, pad, arc, blink, dx, dy, (1 - death));
            } else {
                // Rounded segment: a soft outer glow, a dark outline for contrast
                // against the green heap texture, then a bright-to-deep gradient.
                g2.setColor(new Color(120, 255, 140, (int) (30 * (1 - death))));
                g2.fillRoundRect(x + pad - 1, y + pad - 1,
                        cell - 2 * pad + 2, cell - 2 * pad + 2, arc, arc);
                g2.setColor(new Color(6, 30, 12, Math.max(60, bodyA)));
                g2.fillRoundRect(x + pad, y + pad, cell - 2 * pad, cell - 2 * pad, arc, arc);
                g2.setPaint(new GradientPaint(x, y,
                        new Color(196, 255, 158, Math.max(70, bodyA)),
                        x, y + cell, new Color(42, 205, 62, Math.max(56, bodyA))));
                g2.fillRoundRect(x + pad + 1, y + pad + 1,
                        cell - 2 * pad - 2, cell - 2 * pad - 2, arc, arc);
                g2.setPaint(null);
            }
        }
    }

    /** The head: neon bloom, rounded gradient core and two eyes facing travel. */
    private void drawHead(Graphics2D g2, int x, int y, int pad, int arc,
                          boolean blink, int dx, int dy, float alive) {
        int gx = x + pad, gy = y + pad, gs = cell - 2 * pad;

        // Bloom: two translucent rounded strokes behind the head.
        g2.setStroke(new BasicStroke(6f));
        g2.setColor(new Color(120, 255, 140, (int) (40 * alive)));
        g2.drawRoundRect(gx - 2, gy - 2, gs + 4, gs + 4, arc, arc);
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(new Color(150, 255, 170, (int) (90 * alive)));
        g2.drawRoundRect(gx - 1, gy - 1, gs + 2, gs + 2, arc, arc);
        g2.setStroke(new BasicStroke(1f));

        Color core = blink ? new Color(150, 210, 150) : new Color(225, 255, 208);
        g2.setColor(new Color(18, 90, 30));
        g2.fillRoundRect(gx, gy, gs, gs, arc, arc);
        g2.setPaint(new GradientPaint(gx, gy, core, gx, gy + gs, new Color(70, 210, 90)));
        g2.fillRoundRect(gx + 1, gy + 1, gs - 2, gs - 2, arc, arc);
        g2.setPaint(null);

        // Eyes, offset toward the front and split across the heading.
        int e = Math.max(2, cell / 5);
        double hx = x + cell / 2.0, hy = y + cell / 2.0;
        double f = cell * 0.20, s = cell * 0.20;
        double ax = dx, ay = dy, px = -dy, py = dx;
        double cx1 = hx + ax * f + px * s, cy1 = hy + ay * f + py * s;
        double cx2 = hx + ax * f - px * s, cy2 = hy + ay * f - py * s;
        g2.setColor(new Color(12, 30, 12));
        g2.fillRect((int) (cx1 - e / 2.0), (int) (cy1 - e / 2.0), e, e);
        g2.fillRect((int) (cx2 - e / 2.0), (int) (cy2 - e / 2.0), e, e);
        g2.setColor(new Color(235, 255, 240, (int) (200 * alive)));
        int gl = Math.max(1, e / 3);
        g2.fillRect((int) (cx1 - e / 2.0), (int) (cy1 - e / 2.0), gl, gl);
        g2.fillRect((int) (cx2 - e / 2.0), (int) (cy2 - e / 2.0), gl, gl);
    }

    // ── Pickups ──────────────────────────────────────────────────────────
    private void renderPickups(Graphics2D g2) {
        double now = System.currentTimeMillis();
        int inset = Math.max(2, cell / 6);
        for (SnakePickup p : model.pickups) {
            int x = cellX(p.col), y = cellY(p.row);
            // Bob: a slow vertical float, phase-offset per pellet so they differ.
            y += (int) Math.round(Math.sin(now / 260.0 + p.col * 0.9 + p.row) * Math.max(1, cell / 14));

            boolean isReadOnlySeg = model.isSegmentSwitchLevel() && (p.col >= 16);
            boolean isHazard = isReadOnlySeg && (p.type == SnakePickup.Type.MALLOC);

            if (isHazard) {
                // TASK 3B: W pellet in read-only segment is a HAZARD
                int pulse = (int) (130 + 70 * Math.sin(now / 160.0));
                g2.setColor(new Color(255, 40, 80, pulse));
                g2.fillRoundRect(x - 2, y - 2, cell + 4, cell + 4, cell / 2, cell / 2);
                g2.setColor(new Color(255, 40, 80));
                g2.fillRoundRect(x + inset, y + inset, cell - 2 * inset, cell - 2 * inset, cell / 3, cell / 3);

                g2.setFont(fTitle);
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(RetroTheme.ACCENT_YELLOW);
                String mStr = "M";
                g2.drawString(mStr, x + (cell - fm.stringWidth(mStr)) / 2, y + (cell + fm.getAscent()) / 2 - 2);
            } else if (p.type == SnakePickup.Type.READ) {
                // TASK 3B: R pellet (cyan "R")
                g2.setColor(new Color(5, 217, 232, 65));
                g2.fillRoundRect(x - 1, y - 1, cell + 2, cell + 2, cell / 2, cell / 2);
                g2.setColor(RetroTheme.ACCENT_CYAN);
                g2.fillRoundRect(x + inset, y + inset, cell - 2 * inset, cell - 2 * inset, cell / 3, cell / 3);

                g2.setFont(fTitle);
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(new Color(10, 16, 32));
                String rStr = "R";
                g2.drawString(rStr, x + (cell - fm.stringWidth(rStr)) / 2, y + (cell + fm.getAscent()) / 2 - 2);
            } else {
                // TASK 3B: W pellet (yellow "M")
                g2.setColor(new Color(255, 240, 150, 55));
                g2.fillRoundRect(x - 1, y - 1, cell + 2, cell + 2, cell / 2, cell / 2);
                g2.setColor(SnakeLevel.C_DATA);
                g2.fillRoundRect(x + inset, y + inset, cell - 2 * inset, cell - 2 * inset, cell / 3, cell / 3);

                g2.setFont(fTitle);
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(new Color(85, 55, 0));
                String mStr = "M";
                g2.drawString(mStr, x + (cell - fm.stringWidth(mStr)) / 2, y + (cell + fm.getAscent()) / 2 - 2);
            }
        }
    }

    // ── TASK 3A: Ruler: shows decimal offsets inside arena, no board addresses ───
    private void renderRuler(Graphics2D g2) {
        int x = rulerX;
        g2.setColor(new Color(10, 8, 24));
        g2.fillRect(x, boardY, rulerW, boardH);
        g2.setColor(new Color(40, 34, 70));
        g2.drawRect(x, boardY, rulerW, boardH);

        int rows = model.rows;
        int lvUnits = model.level.rowUnits;
        g2.setColor(new Color(72, 64, 108));
        for (int r = 0; r <= rows; r++) {
            int y = boardY + boardH - (int) Math.round(r * (double) boardH / rows);
            int len = (r == 0 || r == rows) ? 12 : 6;
            g2.drawLine(x + rulerW - len - 1, y, x + rulerW - 1, y);
        }

        g2.setFont(fHelper);
        FontMetrics fm = g2.getFontMetrics();

        if (model.isSegmentSwitchLevel()) {
            int maxLimit = 0;
            for (SnakeLevel.SegmentDef s : model.level.segments) {
                if (s.limit > maxLimit) maxLimit = s.limit;
            }
            int topRow = maxLimit / lvUnits;
            rulerLabel(g2, fm, x, "0", 0, rows, alpha(SnakeLevel.C_HEAP, 230));
            if (topRow > 0 && topRow <= rows) {
                rulerLabel(g2, fm, x, String.valueOf(maxLimit), topRow, rows, alpha(SnakeLevel.C_HEAP, 230));
            }
            return;
        }

        int baseRow = model.heapBase / lvUnits;
        int topRow = model.heapTop() / lvUnits;
        int limit = model.heapLimit;

        // Decimal labels at base line (0) and top line (limit).
        // Decorative bands outside arena carry NO address labels.
        rulerLabel(g2, fm, x, "0", baseRow, rows, alpha(SnakeLevel.C_HEAP, 230));
        if (topRow > 0 && topRow <= rows) {
            rulerLabel(g2, fm, x, String.valueOf(limit), topRow, rows, alpha(SnakeLevel.C_HEAP, 230));
        }
    }

    private void rulerLabel(Graphics2D g2, FontMetrics fm, int x, String s, int row, int rows, Color c) {
        int y = boardY + boardH - (int) Math.round(row * (double) boardH / rows);
        int w = fm.stringWidth(s);
        int tx = x + rulerW - 3 - w;
        if (tx < x + 1) tx = x + 1;
        int baseline = y - 3;
        if (baseline - fm.getAscent() < boardY) baseline = y + fm.getAscent() + 3;
        if (baseline > boardY + boardH - 2) baseline = boardY + boardH - 2;
        Shape old = g2.getClip();
        g2.clipRect(x, boardY, rulerW + 30, boardH);
        g2.setColor(new Color(10, 8, 24));
        g2.fillRect(tx - 2, baseline - fm.getAscent(), w + 4, fm.getAscent() + 2);
        g2.setColor(c);
        g2.drawString(s, tx, baseline);
        g2.setClip(old);
    }

    // ── Left column: OFFSET / LIMIT thermometer, LIVE PA CHIP, and FREE GAP ──
    private void renderGauges(Graphics2D g2) {
        SnakeLevel lv = model.level;
        int x = leftX, y = leftY, w = sideW, h = leftH;
        card(g2, x, y, w, h);

        int pad = 12;
        int cy = y + pad;

        // B2: Segment identity chip at top of the left gauge panel
        String segIdentity;
        Color segColor;
        if (model.isSegmentSwitchLevel()) {
            SnakeLevel.SegmentDef active = model.activeSegment();
            int sNum = active != null ? active.id : (model.activeSegmentIndex + 1);
            String sName = active != null ? active.name : ("Seg_" + sNum);
            segIdentity = "#" + sNum + " " + sName;
            segColor = SegmentStyle.segmentColor(model.activeSegmentIndex);
        } else {
            String sName = lv.sourceName != null && !lv.sourceName.isEmpty() ? lv.sourceName : "HEAP";
            segIdentity = "#0 " + sName;
            segColor = SnakeLevel.C_HEAP;
        }

        g2.setFont(fHelper);
        FontMetrics idFm = g2.getFontMetrics();
        int idTxtW = idFm.stringWidth(segIdentity);
        int idW = idTxtW + 12;
        int idH = idFm.getAscent() + 6;
        g2.setColor(new Color(16, 12, 30));
        g2.fillRoundRect(x + pad, cy, idW, idH, 6, 6);
        g2.setColor(segColor);
        g2.drawRoundRect(x + pad, cy, idW, idH, 6, 6);
        g2.drawString(segIdentity, x + pad + 6, cy + idFm.getAscent() + 1);

        cy += idH + 6;

        g2.setFont(fTitle);
        FontMetrics tfm = g2.getFontMetrics();
        g2.setColor(RetroTheme.ACCENT_CYAN);
        g2.drawString("[ OFFSET GAUGE ]", x + pad, cy + tfm.getAscent());

        // TASK 3A #1 & 3B: Live PA chip next to the OFFSET gauge
        int realBase = model.currentSegmentBase();
        int curOff = model.headOffset();
        int pa = realBase + curOff;
        String paChip = model.isSegmentSwitchLevel()
                ? "(" + (model.activeSegmentIndex + 1) + ", " + curOff + ") -> PA " + pa
                : "-> PA " + pa;

        g2.setFont(fHelper);
        FontMetrics hfm = g2.getFontMetrics();
        int chipTxtW = hfm.stringWidth(paChip);
        int chipW = chipTxtW + 10;
        int chipH = hfm.getAscent() + 6;
        int chipX = x + w - pad - chipW;
        int chipY = cy + (tfm.getAscent() - chipH) / 2 + 1;

        g2.setColor(new Color(20, 16, 38));
        g2.fillRoundRect(chipX, chipY, chipW, chipH, 5, 5);
        g2.setColor(new Color(60, 180, 220, 180));
        g2.drawRoundRect(chipX, chipY, chipW, chipH, 5, 5);
        g2.setColor(RetroTheme.ACCENT_CYAN);
        g2.drawString(paChip, chipX + 5, chipY + hfm.getAscent() + 1);

        cy += tfm.getAscent() + 10;

        int limit = Math.max(1, model.currentSegmentLimit());
        int off = clamp(curOff, 0, limit);

        g2.setFont(fKey);
        FontMetrics kfm = g2.getFontMetrics();
        int numH = kfm.getAscent() + 6;
        int freeH = lv.stackEnabled ? 78 : 0;
        int gBot = Math.max(cy + 60, y + h - pad - freeH - numH - 14);
        int gw = Math.max(22, Math.min(48, w / 5));
        int gx = x + (w - gw) / 2;
        int gH = gBot - cy;

        g2.setColor(new Color(12, 9, 26));
        g2.fillRoundRect(gx, cy, gw, gH, 8, 8);
        g2.setColor(new Color(48, 40, 84));
        g2.drawRoundRect(gx, cy, gw, gH, 8, 8);

        // Danger zone at top of thermometer
        int redH = Math.max(4, (int) Math.round(gH * (double) lv.rowUnits / limit));
        g2.setColor(new Color(255, 60, 90, 80));
        g2.fillRect(gx + 2, cy + 1, gw - 4, redH);

        // Head position, filled from the bottom
        double frac = off / (double) limit;
        int fillH = (int) Math.round(frac * (gH - 2));
        g2.setColor(alpha(SnakeLevel.C_HEAP, 150));
        g2.fillRect(gx + 2, gBot - 1 - fillH, gw - 4, fillH);

        int targetHy = gBot - 1 - fillH;
        // P3: Smooth analog needle interpolation for physical needle feel
        if (needleSmoothY < 0) {
            needleSmoothY = targetHy;
        } else {
            needleSmoothY = needleSmoothY * 0.72 + targetHy * 0.28;
        }
        int hy = (int) Math.round(needleSmoothY);

        // D4 & P3: Pulse the pointer between red and white when near limit / danger boundary
        boolean nearLim = model.headNearLimit();
        boolean pulseFlash = (System.currentTimeMillis() / 125) % 2 == 0;
        Color pointerColor = (nearLim && pulseFlash) ? SegmentStyle.TRAP_COLOR : RetroTheme.TEXT_WHITE;
        g2.setColor(pointerColor);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(gx - 6, hy, gx + gw + 6, hy);
        g2.setStroke(new BasicStroke(1f));

        // Ticks per row
        int arenaRows = Math.max(1, limit / lv.rowUnits);
        g2.setColor(new Color(74, 66, 116));
        for (int r = 0; r <= arenaRows; r++) {
            int ty = gBot - (int) Math.round(r * (double) (gH - 2) / arenaRows) - 1;
            g2.drawLine(gx + gw, ty, gx + gw + 6, ty);
        }

        // P3: Colorblind-friendly danger symbol "[!]" beside offset reading when in danger zone
        g2.setFont(fKey);
        g2.setColor(nearLim ? (pulseFlash ? SegmentStyle.TRAP_COLOR : RetroTheme.ACCENT_YELLOW) : RetroTheme.TEXT_WHITE);
        String reading = (nearLim ? "[!] " : "") + off + " / " + limit;
        drawCentered(g2, reading, x + w / 2, gBot + kfm.getAscent() + 10);

        // ── FREE GAP meter (hidden when level has no stack) ──
        if (!lv.stackEnabled) return;

        int fy = y + h - pad - freeH;
        g2.setFont(fHelper);
        FontMetrics sfm = g2.getFontMetrics();
        g2.setColor(RetroTheme.TEXT_DIM);
        g2.drawString("FREE GAP", x + pad, fy + sfm.getAscent());

        int free = Math.max(0, lv.stackTop - model.heapTop());
        int frames = model.freeGapFrames();
        boolean crit = frames <= 2;
        boolean warn = !crit && frames <= 4;
        int barY2 = fy + sfm.getAscent() + 8;
        int barH2 = Math.max(10, freeH - sfm.getAscent() - 10 - kfm.getAscent() - 8);
        int bw = w - 2 * pad;
        g2.setColor(new Color(12, 9, 26));
        g2.fillRoundRect(x + pad, barY2, bw, barH2, 5, 5);
        g2.setColor(new Color(48, 40, 84));
        g2.drawRoundRect(x + pad, barY2, bw, barH2, 5, 5);
        double ff = lv.addressSpace <= 0 ? 0 : free / (double) lv.addressSpace;
        int fw2 = (int) Math.round(ff * (bw - 4));
        Color fill = crit ? pulseRed() : (warn ? RetroTheme.ACCENT_YELLOW : SnakeLevel.C_HEAP);
        g2.setColor(crit ? fill : alpha(fill, 200));
        g2.fillRect(x + pad + 2, barY2 + 2, Math.max(0, fw2), barH2 - 4);

        g2.setFont(fKey);
        g2.setColor(crit ? RetroTheme.ACCENT_PINK
                : (warn ? RetroTheme.ACCENT_YELLOW : RetroTheme.TEXT_WHITE));
        drawCentered(g2, String.valueOf(free), x + w / 2, barY2 + barH2 + kfm.getAscent() + 6);
    }

    // ── Advanced panel (Tab): registers, units, legend ───────────────────
    private void renderAdvanced(Graphics2D g2) {
        int x = leftX, y = leftY, w = sideW, h = leftH;
        card(g2, x, y, w, h);

        SnakeLevel lv = model.level;
        int limit = model.heapLimit;
        int off = model.headOffset();
        int top = model.heapTop();
        int free = Math.max(0, lv.stackTop - top);
        boolean freeSmall = lv.stackEnabled && model.freeGapFrames() <= 2;
        // STEP 5 A: one base only - the real segment base when there is one.
        int baseShown = lv.sourceBase >= 0 ? lv.sourceBase : model.heapBase;

        int pad = 12;
        int px = x + pad;
        int cw = w - 2 * pad;

        g2.setFont(fTitle);
        FontMetrics tfm = g2.getFontMetrics();
        g2.setColor(RetroTheme.ACCENT_CYAN);
        g2.drawString("[ ADVANCED REGISTERS ]", px, y + pad + tfm.getAscent());

        int py = y + pad + tfm.getAscent() + 10;
        int bottom = y + h - pad;

        // TASK 3A #4: Row mimicking the segment table entry: SEG #, BASE, LIMIT, PERM (R/W or R)
        int tblH = model.isSegmentSwitchLevel() ? 62 : 36;
        g2.setColor(new Color(22, 18, 44));
        g2.fillRoundRect(px, py, cw, tblH, 6, 6);
        g2.setColor(new Color(55, 45, 95));
        g2.drawRoundRect(px, py, cw, tblH, 6, 6);

        g2.setFont(fHelper);
        FontMetrics tfm2 = g2.getFontMetrics();
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        g2.drawString("SEG #", px + 6, py + tfm2.getAscent() + 2);
        g2.drawString("BASE", px + 44, py + tfm2.getAscent() + 2);
        g2.drawString("LIMIT", px + 88, py + tfm2.getAscent() + 2);
        g2.drawString("PERM", px + cw - 36, py + tfm2.getAscent() + 2);

        if (model.isSegmentSwitchLevel()) {
            for (int i = 0; i < model.level.segments.size(); i++) {
                SnakeLevel.SegmentDef s = model.level.segments.get(i);
                boolean active = (i == model.activeSegmentIndex);
                int ry = py + 24 + i * 13;
                g2.setColor(active ? RetroTheme.ACCENT_CYAN : RetroTheme.TEXT_WHITE);
                g2.drawString(String.valueOf(s.id), px + 10, ry);
                g2.drawString(String.valueOf(s.base), px + 44, ry);
                g2.drawString(String.valueOf(s.limit), px + 88, ry);
                g2.setColor(s.readOnly ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_GREEN);
                g2.drawString(s.readOnly ? "R" : "R/W", px + cw - 36, ry);
            }
            py += tblH + 6;
        } else {
            int baseVal = lv.sourceBase >= 0 ? lv.sourceBase : model.heapBase;
            int ry = py + 24;
            g2.setColor(RetroTheme.ACCENT_CYAN);
            g2.drawString("0", px + 10, ry);
            g2.drawString(String.valueOf(baseVal), px + 44, ry);
            g2.drawString(String.valueOf(limit), px + 88, ry);
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString("R/W", px + cw - 36, ry);
            py += tblH + 6;
        }

        int[] head = model.body.peekFirst();
        boolean nearTop = off >= limit - lv.rowUnits;
        boolean nearBottom = off < lv.rowUnits;
        Color offColor = (nearTop || nearBottom) ? RetroTheme.ACCENT_PINK : RetroTheme.TEXT_WHITE;

        int ch = 34, ch2 = 48;
        chip(g2, px, py, cw, ch, "BASE", String.valueOf(baseShown), SnakeLevel.C_HEAP);
        py += ch + 4;
        chip(g2, px, py, cw, ch, "LIMIT", String.valueOf(limit), SnakeLevel.C_HEAP);
        py += ch + 4;
        chip(g2, px, py, cw, ch, "TOP", String.valueOf(top), SnakeLevel.C_HEAP);
        py += ch + 4;
        chip2(g2, px, py, cw, ch2, "OFFSET", String.valueOf(off), offColor,
                "ROW " + (head == null ? 0 : head[1]) + " / COL " + (head == null ? 0 : head[0]), offColor);
        py += ch2 + 4;
        chip(g2, px, py, cw, ch, "STACK_PTR",
                lv.stackEnabled ? String.valueOf(model.stackFrontier()) : "----",
                lv.stackEnabled ? SnakeLevel.C_STACK : RetroTheme.TEXT_DIM);
        py += ch + 8;

        g2.setFont(fHelper);
        FontMetrics fm = g2.getFontMetrics();
        int lineH = fm.getAscent() + 5;
        String[] units = {
            "ROW UNIT  " + lv.rowUnits,
            "ARENA     " + (lv.heapLimit / lv.rowUnits) + " rows",
            "COLS      " + model.cols,
            "FRAMES    " + (lv.stackEnabled ? model.stackFrames + " / gap " + model.freeGapFrames() : "----"),
            "FREE GAP  " + free,
        };
        for (String s : units) {
            if (py + lineH > bottom) return;
            boolean warn = freeSmall && s.startsWith("FREE");
            g2.setColor(warn ? RetroTheme.ACCENT_PINK : RetroTheme.TEXT_DIM);
            g2.drawString(s, px, py + fm.getAscent());
            py += lineH;
        }

        py += 6;
        Object[][] legend = {
            {SnakeLevel.C_CODE, "CODE  read-only"},
            {SnakeLevel.C_DATA, "DATA  read/write"},
            {SnakeLevel.C_HEAP, "HEAP  your arena"},
            {SnakeLevel.C_FREE, "FREE  unmapped"},
        };
        for (Object[] e : legend) {
            if (py + lineH > bottom) return;
            Color c = (Color) e[0];
            g2.setColor(c == SnakeLevel.C_FREE ? new Color(96, 88, 130) : c);
            g2.fillRect(px, py + 3, 10, 10);
            g2.setColor(RetroTheme.TEXT_WHITE);
            g2.drawString((String) e[1], px + 16, py + fm.getAscent());
            py += lineH;
        }
    }

    private void chip(Graphics2D g2, int x, int y, int w, int h, String label, String value, Color valueColor) {
        chipBox(g2, x, y, w, h);
        g2.setFont(fHelper);
        FontMetrics fm = g2.getFontMetrics();
        int ly = y + h - 9;
        g2.setColor(RetroTheme.TEXT_DIM);
        g2.drawString(label, x + 8, ly);
        g2.setFont(fKey);
        fm = g2.getFontMetrics();
        g2.setColor(valueColor);
        String v = clip(fm, value, Math.max(20, w / 2));
        g2.drawString(v, x + w - 8 - fm.stringWidth(v), ly);
    }

    /** A chip with a second line under the value, e.g. "ROW 0 / COL 6". */
    private void chip2(Graphics2D g2, int x, int y, int w, int h,
                       String label, String value, Color valueColor, String sub, Color subColor) {
        chipBox(g2, x, y, w, h);
        g2.setFont(fHelper);
        FontMetrics fm = g2.getFontMetrics();
        int ly = y + h / 2 - 2;
        g2.setColor(RetroTheme.TEXT_DIM);
        g2.drawString(label, x + 8, ly);
        g2.setFont(fKey);
        fm = g2.getFontMetrics();
        g2.setColor(valueColor);
        String v = clip(fm, value, Math.max(20, w / 2));
        g2.drawString(v, x + w - 8 - fm.stringWidth(v), ly);
        g2.setFont(fHelper);
        fm = g2.getFontMetrics();
        g2.setColor(subColor);
        String s = clip(fm, sub, w - 16);
        g2.drawString(s, x + w - 8 - fm.stringWidth(s), ly + h / 2 - 2);
    }

    private static void chipBox(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(24, 18, 44));
        g2.fillRoundRect(x, y, w, h, 8, 8);
        g2.setColor(new Color(52, 44, 92));
        g2.drawRoundRect(x, y, w, h, 8, 8);
    }

    private static void card(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(16, 12, 32));
        g2.fillRoundRect(x, y, w, h, 10, 10);
        g2.setColor(RetroTheme.BORDER_BRIGHT);
        g2.drawRoundRect(x, y, w, h, 10, 10);
    }

    // ── HUD: a borderless overlay along the top edge of the board area ───
    private void renderHud(Graphics2D g2) {
        SnakeLevel lv = model.level;
        int x = hudX, y = hudY, h = hudH, w = hudW;

        // TASK 1.3: the HUD is a reserved band above the board, so it gets an
        // opaque backing - nothing from the board can show through behind the
        // hearts, the score or the MALLOC counter.
        g2.setColor(new Color(12, 10, 26, 246));
        g2.fillRect(x, y, w, h);
        g2.setColor(new Color(40, 60, 46));
        g2.drawLine(x, y + h - 1, x + w, y + h - 1);

        int midY = y + h / 2;

        // Lives on the left, as shapes.
        int hs = clamp(h / 8, 4, 8);
        int gw = HEART[0].length * hs;
        int hx = x + 10;
        int hy = midY - HEART.length * hs / 2;
        for (int i = 0; i < SnakeModel.START_HEARTS; i++) {
            drawHeart(g2, hx + i * (gw + 5), hy, hs, i < model.hearts);
        }

        // Score centred: TASK 1.5 zero-pads to six digits and adds a small SCORE
        // label on the same baseline, so the band height is unchanged.
        g2.setFont(fScore);
        FontMetrics sfm = g2.getFontMetrics();
        String sc = String.format("%06d", model.score);
        int sBaseline = midY + sfm.getAscent() / 2 - 2;
        g2.setFont(fHelper);
        FontMetrics lfm = g2.getFontMetrics();
        String sLab = "SCORE";
        int scoreLabW = lfm.stringWidth(sLab);
        int nw = sfm.stringWidth(sc);
        int labelGap = 10;
        int startX = x + w / 2 - (scoreLabW + labelGap + nw) / 2;
        g2.setColor(RetroTheme.TEXT_DIM);
        g2.drawString(sLab, startX, sBaseline);
        g2.setFont(fScore);
        g2.setColor(RetroTheme.TEXT_WHITE);
        g2.drawString(sc, startX + scoreLabW + labelGap, sBaseline);

        // Right side: the MALLOC progress bar, or - PART 3 - a clock and a time bar.
        g2.setFont(fKey);
        FontMetrics fm = g2.getFontMetrics();
        int right = x + w - 10;
        if (lv.goalMallocs > 0) {
            String lab = "M " + model.mallocsEaten + "/" + lv.goalMallocs;
            int lw = fm.stringWidth(lab);
            g2.setColor(SnakeLevel.C_DATA);
            g2.drawString(lab, right - lw, midY - 2);
            drawGoalBar(g2, right, midY + 4, Math.max(lw, 60), lv.goalMallocs, model.mallocsEaten);
        } else if (lv.goalSurviveSec > 0) {
            // PART 3: a small clock "0:23" and nothing more, over a time bar.
            String clock = timeLine();
            int lw = fm.stringWidth(clock);
            g2.setColor(SnakeLevel.C_DATA);
            g2.drawString(clock, right - lw, midY - 2);
            drawTimeBar(g2, right, midY + 4, Math.max(lw, 60),
                    lv.goalSurviveSec * 1000L, model.surviveMs);
        }
    }

    /** PART 3: a continuous bar that fills as the survival clock runs. */
    private void drawTimeBar(Graphics2D g2, int rightX, int y, int width, long goalMs, long doneMs) {
        int bw = Math.max(24, width);
        double frac = goalMs <= 0 ? 0 : Math.max(0, Math.min(1, doneMs / (double) goalMs));
        g2.setColor(new Color(74, 64, 36));
        g2.fillRect(rightX - bw, y, bw, 7);
        int fw = (int) Math.round(frac * bw);
        g2.setColor(new Color(255, 245, 190, 95));
        g2.fillRect(rightX - bw - 1, y - 1, fw + 2, 9);
        g2.setColor(new Color(255, 238, 96));
        g2.fillRect(rightX - bw, y, fw, 7);
    }

    private void drawGoalBar(Graphics2D g2, int rightX, int y, int width, int goal, int done) {
        int seg = Math.max(4, width / Math.max(1, goal));
        int gapPx = 2;
        for (int i = 0; i < goal; i++) {
            int sx = rightX - (goal - i) * seg - (goal - 1 - i) * gapPx;
            if (i < done) {
                // TASK 1.5: filled segments glow - a soft halo behind a bright core.
                g2.setColor(new Color(255, 245, 190, 95));
                g2.fillRect(sx - 1, y - 1, seg + 2, 9);
                g2.setColor(new Color(255, 238, 96));
            } else {
                g2.setColor(new Color(74, 64, 36));
            }
            g2.fillRect(sx, y, seg, 7);
        }
    }

    private void drawHeart(Graphics2D g2, int x, int y, int s, boolean filled) {
        Color on = filled ? RetroTheme.ACCENT_PINK : new Color(66, 34, 56);
        Color off = filled ? new Color(190, 30, 105) : new Color(40, 22, 36);
        g2.setColor(on);
        for (int r = 0; r < HEART.length; r++) {
            for (int c = 0; c < HEART[r].length; c++) {
                if (HEART[r][c] == 0) continue;
                if (r >= HEART.length - 2) g2.setColor(off);
                g2.fillRect(x + c * s, y + r * s, s, s);
            }
        }
    }

    // ── Minimap (right column): blocks, the head marker, the heap-top tick ──
    private void renderMinimap(Graphics2D g2) {
        SnakeLevel lv = model.level;
        int x = rightX, y = rightY, w = mmW, h = rightH;
        card(g2, x, y, w, h);

        int pad = 10;
        int barX = x + pad;
        int barW = Math.max(24, w - 2 * pad);
        int barY = y + pad;
        int barH = Math.max(30, y + h - pad - barY);

        g2.setColor(new Color(8, 6, 20));
        g2.fillRect(barX, barY, barW, barH);
        g2.setColor(new Color(46, 40, 78));
        g2.drawRect(barX, barY, barW, barH);

        // B5: In Level 3 (segment switch), render each segment from the table as a block
        if (model.isSegmentSwitchLevel()) {
            int maxPhys = 1000;
            for (SnakeLevel.SegmentDef s : lv.segments) {
                if (s.base + s.limit > maxPhys) maxPhys = s.base + s.limit;
            }
            maxPhys = Math.max(maxPhys, 8000);

            for (int i = 0; i < lv.segments.size(); i++) {
                SnakeLevel.SegmentDef s = lv.segments.get(i);
                boolean active = (i == model.activeSegmentIndex);
                Color c = SegmentStyle.segmentColor(i);
                int yTop = mmY(barY, barH, maxPhys, s.base + s.limit);
                int yBot = mmY(barY, barH, maxPhys, s.base);
                int hh = Math.max(2, yBot - yTop);

                g2.setColor(active ? c : alpha(c, 120));
                g2.fillRect(barX, yTop, barW, hh);
                if (active) {
                    g2.setColor(Color.WHITE);
                    g2.drawRect(barX, yTop, barW, hh);
                }

                g2.setFont(fHelper);
                FontMetrics fm = g2.getFontMetrics();
                if (hh >= fm.getHeight()) {
                    g2.setColor(Color.WHITE);
                    g2.drawString("#" + s.id, barX + 4, yTop + fm.getAscent() + (hh - fm.getHeight()) / 2);
                }
            }

            // Head marker at live physical address
            int pa = model.headPhysicalAddress();
            int hy = mmY(barY, barH, maxPhys, pa);
            g2.setColor(RetroTheme.TEXT_WHITE);
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawLine(barX - 5, hy, barX + barW + 5, hy);
            g2.setStroke(new BasicStroke(1f));
            g2.fillPolygon(new int[]{barX - 8, barX - 8, barX + 2}, new int[]{hy - 4, hy + 4, hy}, 3);
            return;
        }

        int front = model.stackFrontier();
        boolean freeSmall = lv.stackEnabled && model.freeGapFrames() <= 2;
        int top = model.heapTop();

        // One vertical bar, high addresses at the top. Only HEAP / FREE / STACK
        // get a word; the rest are read from their colour.
        mmSeg(g2, barX, barW, barY, barH, lv.codeBase, lv.codeBase + lv.codeLimit,
                alpha(SnakeLevel.C_CODE, 150), null);
        mmSeg(g2, barX, barW, barY, barH, lv.dataBase, lv.dataBase + lv.dataLimit,
                alpha(SnakeLevel.C_DATA, 150), null);
        mmSeg(g2, barX, barW, barY, barH, model.heapBase, top,
                alpha(SnakeLevel.C_HEAP, 170), "HEAP");
        mmSeg(g2, barX, barW, barY, barH, top, lv.stackTop,
                freeSmall ? pulseRed() : new Color(30, 26, 54), "FREE");
        if (lv.stackEnabled) {
            mmSeg(g2, barX, barW, barY, barH, front, lv.stackTop,
                    alpha(SnakeLevel.C_STACK, 170), "STACK");
        }

        // Heap top: a tick that crosses the bar (no address text).
        int topY = mmY(barY, barH, lv.addressSpace, top);
        g2.setColor(SnakeLevel.C_HEAP);
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawLine(barX - 5, topY, barX + barW + 5, topY);
        g2.setStroke(new BasicStroke(1f));

        if (lv.stackEnabled) {
            int fy = mmY(barY, barH, lv.addressSpace, front);
            g2.setColor(alpha(SnakeLevel.C_STACK, 190));
            g2.drawLine(barX - 3, fy, barX + barW + 3, fy);
        }

        // Head: the marker line and a pointer.
        int headAddr = model.headAddress();
        int hy = mmY(barY, barH, lv.addressSpace, headAddr);
        g2.setColor(RetroTheme.TEXT_WHITE);
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawLine(barX - 5, hy, barX + barW + 5, hy);
        g2.setStroke(new BasicStroke(1f));
        g2.fillPolygon(new int[]{barX - 8, barX - 8, barX + 2}, new int[]{hy - 4, hy + 4, hy}, 3);
    }

    /**
     * One coloured block on the minimap. The name is drawn right-aligned inside
     * the block, and only when there is room for it - a block that cannot hold its
     * own word stays clean rather than clipped.
     */
    private void mmSeg(Graphics2D g2, int barX, int barW, int barY, int barH,
                       int aLo, int aHi, Color c, String name) {
        int yTop = mmY(barY, barH, model.level.addressSpace, aHi);
        int yBot = mmY(barY, barH, model.level.addressSpace, aLo);
        int hh = Math.max(1, yBot - yTop);
        g2.setColor(c);
        g2.fillRect(barX, yTop, barW, hh);
        if (name == null) return;

        g2.setFont(fHelper);
        FontMetrics fm = g2.getFontMetrics();
        if (hh < fm.getAscent() + 4 || fm.stringWidth(name) + 8 > barW) return;
        g2.setColor(new Color(6, 4, 14, 210));
        g2.fillRect(barX + barW - fm.stringWidth(name) - 6, yTop,
                fm.stringWidth(name) + 6, hh);
        g2.setColor(RetroTheme.TEXT_WHITE);
        g2.drawString(name, barX + barW - fm.stringWidth(name) - 3,
                yTop + hh / 2 + fm.getAscent() / 2 - 1);
    }

    private static int mmY(int barY, int barH, int addressSpace, int addr) {
        double frac = addr / (double) addressSpace;
        return barY + barH - (int) Math.round(frac * barH);
    }

    private Color pulseRed() {
        double t = (System.currentTimeMillis() % 900) / 900.0;
        int a = (int) (90 + 120 * Math.abs(0.5 - t) * 2);
        return new Color(255, 60, 90, Math.max(60, Math.min(210, a)));
    }

    // ── Kernel ticker: one line, newest only ─────────────────────────────
    private void renderTicker(Graphics2D g2) {
        int x = boardX, y = logY, ww = boardW, hh = logH;
        g2.setColor(new Color(10, 16, 12));
        g2.fillRect(x, y, ww, hh);
        g2.setColor(new Color(44, 72, 50));
        g2.drawRect(x, y, ww, hh);

        // TASK 1.4: the kernel log is the 14 px tier, not the 12 px helper tier.
        g2.setFont(fTitle);
        FontMetrics fm = g2.getFontMetrics();
        int base = y + (hh + fm.getAscent()) / 2 - 2;

        String tag = "dmesg";
        int tagW = fm.stringWidth(tag);

        if (!log.isEmpty()) {
            LogLine ln = log.peekLast();
            g2.setColor(RetroTheme.TEXT_DIM);
            g2.drawString(ln.ts, x + 8, base);
            int tsW = fm.stringWidth(ln.ts) + 10;
            g2.setColor(logColor(ln.kind, 255));
            g2.drawString(clip(fm, ln.text, ww - 24 - tsW - tagW), x + 8 + tsW, base);
        }
        // The tag sits fully inside the strip, so it can never be clipped.
        g2.setColor(new Color(90, 140, 100));
        g2.drawString(tag, x + ww - 10 - tagW, base);
    }

    private static Color logColor(LogKind k, int a) {
        switch (k) {
            case OK:    return new Color(120, 230, 140, Math.max(40, Math.min(255, a)));
            case FAULT: return new Color(255, 90, 110, Math.max(40, Math.min(255, a)));
            case WARN:  return new Color(240, 200, 60, Math.max(40, Math.min(255, a)));
            default:    return new Color(130, 190, 235, Math.max(40, Math.min(255, a)));
        }
    }

    /** Watches the model and appends a line whenever something worth logging happens. */
    private void updateLog() {
        if (!booted) {
            booted = true;
            int base = model.currentSegmentBase();
            int limit = model.heapLimit;
            pushLog("heap [" + base + ".." + (base + limit) + ") limit=" + limit, LogKind.INFO);
        }
        if (model.mallocsEaten > seenMallocs) {
            int off = model.headOffset();
            int base = model.currentSegmentBase();
            // B4: use SegmentStyle shared sentence template
            String msg = SegmentStyle.mallocLogLine(off, base);
            pushLog(msg, LogKind.OK);
        }
        seenMallocs = model.mallocsEaten;

        if (model.hearts < seenHearts) {
            pushLog("kernel: page fault handled - " + model.hearts + "/"
                    + SnakeModel.START_HEARTS + " hearts left, respawn at base", LogKind.WARN);
        }
        seenHearts = model.hearts;

        if (model.mistakes.size() > seenFaults) {
            for (int i = seenFaults; i < model.mistakes.size(); i++) {
                SnakeModel.Fault f = model.mistakes.get(i);
                // PART 3: the collision reads as the memory manager's own line.
                String tag = f == SnakeModel.Fault.COLLISION
                        ? "mm: COLLISION" : SnakeModel.faultName(f);
                pushLog(tag + ": "
                        + SnakeModel.faultDetail(f, model.lastFaultOffset, model.lastFaultLimit, model.lastFaultSegmentId),
                        LogKind.FAULT);
                if (f == SnakeModel.Fault.COLLISION) stackLogSuppressed = true;
            }
            seenFaults = model.mistakes.size();
        } else if (model.mistakes.size() < seenFaults) {
            seenFaults = model.mistakes.size();
        }

        if (model.phase != seenPhase) {
            if (model.phase == SnakeModel.Phase.LEVEL_CLEAR) {
                pushLog("kernel: goal reached - LEVEL CLEAR", LogKind.OK);
            } else if (model.phase == SnakeModel.Phase.GAME_OVER) {
                pushLog("kernel: SIGSEGV - process terminated", LogKind.FAULT);
            } else {
                pushLog("kernel: new process forked at base " + model.currentSegmentBase(), LogKind.WARN);
            }
            seenPhase = model.phase;
        }
    }

    /** Stamps the line with the seconds since this life started. */
    private void pushLog(String s, LogKind kind) {
        double sec = (System.currentTimeMillis() - t0) / 1000.0;
        log.addLast(new LogLine(String.format("[%5.1f]", sec), s, kind));
        while (log.size() > LOG_CAP) log.pollFirst();
    }

    // ── Footer: key-caps that fade out after ten seconds ─────────────────
    /** 1 while the caps are up, sliding to 0 once the hold time is over. */
    private float footerAlpha() {
        if (model.paused || showHelp) return 1f;
        long el = System.currentTimeMillis() - lifeStartMs;
        if (el <= FOOTER_HOLD_MS) return 1f;
        if (el >= FOOTER_HOLD_MS + FOOTER_FADE_MS) return 0f;
        return 1f - (el - FOOTER_HOLD_MS) / (float) FOOTER_FADE_MS;
    }

    private void renderFooter(Graphics2D g2) {
        float a = footerAlpha();
        if (a <= 0.02f) return;

        int x = boardX, y = footY, ww = boardW, hh = footH;
        g2.setColor(new Color(16, 22, 18, (int) (190 * a)));
        g2.fillRect(x, y, ww, hh);
        g2.setColor(new Color(53, 82, 58, (int) (255 * a)));
        g2.drawRect(x, y, ww, hh);

        FontMetrics fm = g2.getFontMetrics(fHelper);
        int capH = hh - 10;
        int gapPx = 6;
        int labelGap = 5;

        List<String[]> list = new ArrayList<>();
        for (String[] c : FOOTER_CAPS) {
            if ("SPACE".equals(c[0]) && !model.level.stackEnabled) continue;
            list.add(c);
        }
        String[][] caps = list.toArray(new String[0][]);

        // PART 1.8: measure both passes; drop the labels if the row would not fit.
        int[] ws = new int[caps.length];
        int[] ls = new int[caps.length];
        boolean withLabels = true;
        for (int pass = 0; pass < 2; pass++) {
            int total = 0;
            for (int i = 0; i < caps.length; i++) {
                ws[i] = Math.max(capH + 4, fm.stringWidth(caps[i][0]) + 12);
                ls[i] = withLabels ? fm.stringWidth(caps[i][1]) : 0;
                total += ws[i] + (ls[i] > 0 ? labelGap + ls[i] : 0) + gapPx;
            }
            total -= gapPx;
            if (total <= ww - 12 || !withLabels) break;
            withLabels = false;
        }

        int totalW = 0;
        for (int i = 0; i < caps.length; i++) {
            totalW += ws[i] + (ls[i] > 0 ? labelGap + ls[i] : 0) + gapPx;
        }
        totalW -= gapPx;
        int cx = x + Math.max(6, (ww - totalW) / 2);
        for (int i = 0; i < caps.length; i++) {
            g2.setFont(fHelper);
            keyCap(g2, cx, y + 5, ws[i], capH, caps[i][0], fm, a);
            cx += ws[i];
            if (ls[i] > 0) {
                cx += labelGap;
                g2.setColor(new Color(150, 205, 160, (int) (235 * a)));
                g2.drawString(caps[i][1], cx, y + (hh + fm.getAscent()) / 2 - 1);
                cx += ls[i];
            }
            cx += gapPx;
        }
    }

    private void keyCap(Graphics2D g2, int x, int y, int w, int h, String glyph,
                        FontMetrics fm, float a) {
        g2.setColor(new Color(26, 34, 28, (int) (235 * a)));
        g2.fillRoundRect(x, y, w, h, 6, 6);
        g2.setColor(new Color(120, 170, 128, (int) (235 * a)));
        g2.drawRoundRect(x, y, w, h, 6, 6);
        g2.setColor(new Color(214, 240, 218, (int) (255 * a)));
        g2.drawString(glyph, x + (w - fm.stringWidth(glyph)) / 2,
                y + (h + fm.getAscent()) / 2 - 1);
    }

    // ── Overlays ─────────────────────────────────────────────────────────
    /** End-of-run screens only: pause and help share the [H] card instead. */
    private void renderOverlay(Graphics2D g2, int w, int h) {
        SnakeModel.Phase ph = model.phase;
        if (ph != SnakeModel.Phase.GAME_OVER && ph != SnakeModel.Phase.LEVEL_CLEAR) return;
        renderEndScreen(g2, w, h, ph == SnakeModel.Phase.GAME_OVER);
    }

    /**
     * TASK 2: the Game Over / Level Clear card. A dark neon panel sits over the
     * dimmed board and carries every line of text, so nothing is ever drawn over a
     * visible board cell. It holds a one-line cause, a compact end report, a mini
     * head-offset-vs-limit diagram, one tip for the most common fault, and the
     * three menu buttons (keyboard cursor plus mouse hover).
     */
    private void renderEndScreen(Graphics2D g2, int w, int h, boolean gameOver) {
        // Dim the cabinet but leave the board readable around the panel.
        g2.setColor(new Color(4, 3, 12, 212));
        g2.fillRect(0, 0, w, h);

        EndLayout e = endLayoutFor(w, h);
        int px = e.px, py = e.py, pw = e.pw, ph = e.ph;
        int pad = e.pad;
        int cx = px + pw / 2;
        Color edge = gameOver ? RetroTheme.ACCENT_PINK : SnakeLevel.C_HEAP;

        // Fonts and one measured vertical rhythm, taken from endReportBottomY so the
        // live card and the offline probe can never drift apart.
        g2.setFont(fScore);  FontMetrics sfm = g2.getFontMetrics();
        g2.setFont(fKey);    FontMetrics kfm = g2.getFontMetrics();
        g2.setFont(fHelper); FontMetrics hfm = g2.getFontMetrics();
        int[] yr = endReportBottomY(py, pad, sfm, kfm, hfm, e);
        int ty = yr[0], causeB = yr[1], sepY = yr[2];
        int col2 = px + pw / 2 + 6;

        // Neon panel: drop shadow, dark body, a bright border and an inner echo.
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(px + 4, py + 6, pw, ph, 16, 16);
        g2.setColor(new Color(8, 6, 20, 250));
        g2.fillRoundRect(px, py, pw, ph, 16, 16);
        g2.setColor(alpha(edge, 90));
        g2.drawRoundRect(px - 2, py - 2, pw + 4, ph + 4, 18, 18);
        g2.setColor(edge);
        g2.drawRoundRect(px, py, pw, ph, 16, 16);
        g2.setColor(alpha(edge, 55));
        g2.drawRoundRect(px + 3, py + 3, pw - 6, ph - 6, 13, 13);

        // Title, with the all-time best rank on the right.
        String title = gameOver ? "GAME OVER" : "LEVEL CLEAR";
        g2.setFont(fScore);
        g2.setColor(edge);
        g2.drawString(title, px + pad, ty);
        String best = "BEST " + bestRank;
        g2.setFont(fHelper);
        g2.setColor(RetroTheme.TEXT_DIM);
        g2.drawString(best, px + pw - pad - hfm.stringWidth(best),
                ty - sfm.getAscent() / 2 + hfm.getAscent() / 2 - 2);

        // One-line cause. PART 1.4: the clear line is short enough to never clip.
        // A3: GAME_OVER cause may be long — wrap to at most 2 lines instead of clipping.
        String cause = gameOver ? model.causeLine() : clearCauseLine();
        g2.setFont(fKey);
        g2.setColor(gameOver ? causeColor() : SnakeLevel.C_HEAP);
        int causeMaxW = pw - 2 * pad;
        if (gameOver && kfm.stringWidth(cause) > causeMaxW) {
            // Split at a word boundary so both parts fit causeMaxW.
            String[] parts = wrapLine(kfm, cause, causeMaxW);
            g2.drawString(parts[0], px + pad, causeB);
            if (parts[1] != null && !parts[1].isEmpty()) {
                g2.drawString(parts[1], px + pad, causeB + kfm.getHeight());
            }
        } else {
            g2.drawString(clip(kfm, cause, causeMaxW), px + pad, causeB);
        }

        g2.setColor(alpha(edge, 70));
        g2.drawLine(px + pad, sepY, px + pw - pad, sepY);

        // ── End report: two key rows, then the two-column stats grid ──
        int ry = yr[3];
        kv(g2, "SCORE", String.format("%06d", model.score), px + pad, ry + kfm.getAscent());
        kv(g2, "RANK", model.rank(), col2, ry + kfm.getAscent());
        ry += kfm.getHeight() + E_ROW_GAP;
        kv(g2, "LONGEST", String.valueOf(model.longest), px + pad, ry + kfm.getAscent());
        kv(g2, "MALLOC", String.valueOf(model.mallocsEaten), col2, ry + kfm.getAscent());
        ry += kfm.getHeight() + E_ROW_GAP;

        // PART 1.5: full words, two columns, and only the faults that happened.
        String[][] cells = statCells();
        int cw2 = (pw - 2 * pad) / 2;
        g2.setFont(fHelper);
        for (int i = 0; i < cells.length && i < e.statRows * 2; i++) {
            statCell(g2, hfm, cells[i][0], cells[i][1],
                    px + pad + (i % 2) * cw2, ry + (i / 2) * hfm.getHeight() + hfm.getAscent(),
                    cw2 - 12);
        }
        ry += e.statRows * hfm.getHeight();

        // ── PART 1.6: the Level Clear middle - time, next rank, top-three list ──
        if (e.showMiddle) {
            g2.setFont(fKey);
            kv(g2, "TIME", timeLine(), px + pad, ry + kfm.getAscent());
            // A6: only show NEXT row when there is a higher rank to reach
            String nrl = nextRankLine();
            if (!nrl.isEmpty()) kv(g2, "NEXT", nrl, col2, ry + kfm.getAscent());
            ry += kfm.getHeight() + E_ROW_GAP;
            renderHiList(g2, hfm, px + pad, ry, e.hiRows);
            ry += e.hiLines() * hfm.getHeight();
        }

        // ── Mistake review: mini diagram + one tip, only when it fits ──
        if (e.reviewLines > 0) {
            renderReview(g2, px + pad, ry + E_GAP_REVIEW, pw - 2 * pad,
                    e.reviewLines * hfm.getHeight(), edge);
        }

        // ── Buttons ──
        renderOverButtons(g2, e);

        // PART 1.4: the hint sits fully inside the border with >= 12 px of padding.
        g2.setFont(fHelper);
        FontMetrics hfm2 = g2.getFontMetrics();
        String hint = "\u2191\u2193 SELECT    ENTER OK    ESC EXIT";
        g2.setColor(new Color(120, 130, 168));
        g2.drawString(hint, cx - hfm2.stringWidth(hint) / 2,
                py + ph - Math.max(12, pad) - hfm2.getDescent());
    }

    /** PART 1.4: the Level Clear goal line, short enough that it never clips. */
    private String clearCauseLine() {
        if (model.level.goalSurviveSec > 0) {
            return "GOAL: SURVIVED " + (model.surviveMs / 1000) + " S";
        }
        return "GOAL: " + model.mallocsEaten + " MALLOCs";
    }

    /** PART 1.6: this run's time, as m:ss. */
    private String timeLine() {
        long sec = model.surviveMs / 1000;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    /** PART 1.6: what the next rank would take, or empty string when S is already reached. */
    private String nextRankLine() {
        // A6: rank is by fault count. Show what threshold the player must hit next.
        int faults = model.mistakes.size();
        boolean underTime = model.level.goalSurviveSec <= 0
                || model.surviveMs < model.level.goalSurviveSec * 1000L;
        if (faults == 0 && underTime) return "";          // already S, no NEXT row needed
        if (faults == 0) return "0 faults = S";           // S needs time only
        if (faults == 1) return "0 faults -> S";
        if (faults == 2) return "1 fault -> A";
        if (faults <= 4) return "2 faults -> B";
        return "3-4 faults -> C";
    }

    /** One cell of the report grid: a dim full-word label, then its value. */
    private void statCell(Graphics2D g2, FontMetrics fm, String label, String value,
                          int x, int baseline, int maxW) {
        g2.setFont(fHelper);
        g2.setColor(RetroTheme.TEXT_DIM);
        g2.drawString(clip(fm, label, maxW - 34), x, baseline);
        g2.setColor(RetroTheme.TEXT_WHITE);
        g2.drawString(value, x + fm.stringWidth(clip(fm, label, maxW - 34)) + 8, baseline);
    }

    /**
     * PART 1.6: the compact top-three table that fills the Level Clear middle. The
     * header line plus {@code entries} score lines, each "n  score  rank".
     */
    private void renderHiList(Graphics2D g2, FontMetrics fm, int x, int y, int entries) {
        g2.setFont(fHelper);
        g2.setColor(new Color(120, 130, 168));
        g2.drawString("HI-SCORES", x, y + fm.getAscent());
        g2.setColor(new Color(160, 172, 208));
        for (int i = 0; i < entries && hiScores != null && i < hiScores.size(); i++) {
            SnakeScores.Entry en = hiScores.get(i);
            String s = (i + 1) + "   " + String.format("%06d", en.score) + "   " + en.rank;
            g2.drawString(s, x, y + fm.getAscent() + (i + 1) * fm.getHeight());
        }
    }

    /** Bright colour for the cause line, keyed to the fault that ended the run. */
    private Color causeColor() {
        if (model.lastFault == null) return RetroTheme.ACCENT_PINK;
        switch (model.lastFault) {
            case SEGFAULT:         return new Color(255, 90, 110);
            case HEAP_CORRUPTION:  return new Color(255, 150, 60);
            case PROTECTION_FAULT: return RetroTheme.ACCENT_YELLOW;
            case COLLISION:        return RetroTheme.ACCENT_PINK;
            default:               return RetroTheme.ACCENT_PINK;
        }
    }

    /** A label in the dim tier followed by its value in the key-value tier. */
    private void kv(Graphics2D g2, String label, String value, int x, int baseline) {
        g2.setFont(fHelper);
        FontMetrics lfm = g2.getFontMetrics();
        g2.setColor(RetroTheme.TEXT_DIM);
        g2.drawString(label, x, baseline);
        g2.setFont(fKey);
        FontMetrics vfm = g2.getFontMetrics();
        g2.setColor(RetroTheme.TEXT_WHITE);
        g2.drawString(value, x + lfm.stringWidth(label) + 8, baseline);
    }

    /**
     * Mini diagram of the head offset against the limit (a short thermometer in
     * the same colours as the main gauge) beside one tip for the most common fault.
     */
    private void renderReview(Graphics2D g2, int x, int y, int w, int hgt, Color edge) {
        // PART 3: a COLLISION is not "head vs limit", it is "heap top vs stack
        // frontier", so it gets its own two-bar diagram.
        if (model.lastFault == SnakeModel.Fault.COLLISION) {
            renderCollisionReview(g2, x, y, w, hgt, edge);
            return;
        }
        int barW = clamp(w / 12, 14, 26);
        int barH = Math.max(28, hgt - 2);
        int barX = x, barY = y + 1;

        g2.setColor(new Color(12, 9, 26));
        g2.fillRect(barX, barY, barW, barH);
        g2.setColor(new Color(48, 40, 84));
        g2.drawRect(barX, barY, barW, barH);

        int limit = model.lastFaultSegmentId > 0 ? Math.max(1, model.lastFaultLimit) : Math.max(1, model.heapLimit);
        int rawOff = model.lastFaultSegmentId > 0 ? model.lastFaultOffset : model.lastFaultOffset;
        int off = clamp(rawOff, 0, limit);
        double frac = rawOff < 0 ? 0.0 : Math.min(1.0, rawOff / (double) limit);
        int fillH = (int) Math.round(frac * (barH - 2));
        g2.setColor(alpha(SnakeLevel.C_HEAP, 160));
        g2.fillRect(barX + 1, barY + barH - 1 - fillH, barW - 2, fillH);

        // Limit line (red, at the top) and the head marker (white).
        g2.setColor(new Color(255, 70, 100));
        g2.drawLine(barX - 3, barY + 1, barX + barW + 3, barY + 1);
        int hy = barY + barH - 1 - fillH;
        g2.setColor(RetroTheme.TEXT_WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(barX - 4, hy, barX + barW + 4, hy);
        g2.setStroke(new BasicStroke(1f));

        g2.setFont(fHelper);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(150, 160, 190));
        g2.drawString("LMT", barX + barW + 6, barY + fm.getAscent() - 2);
        g2.drawString("BASE", barX + barW + 6, barY + barH);

        int tx = barX + barW + 6 + fm.stringWidth("BASE") + 18;
        int tw = x + w - tx;
        if (tw < 80) return;

        g2.setColor(edge);
        g2.drawString("MISTAKE REVIEW", tx, y + fm.getAscent());
        int ly = y + fm.getAscent() + fm.getHeight() + 2;
        g2.setColor(RetroTheme.TEXT_WHITE);
        String l1;
        if (model.lastFaultSegmentId > 0) {
            l1 = "offset " + rawOff + " / limit " + limit + " (Seg " + model.lastFaultSegmentId + ")";
        } else {
            l1 = "offset " + model.lastFaultOffset + " / limit " + model.lastFaultLimit;
        }
        g2.drawString(clip(fm, l1, tw), tx, ly);
        ly += fm.getHeight();
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        for (String line : tipLines()) {
            if (line == null || ly > y + hgt) break;
            g2.drawString(clip(fm, line, tw), tx, ly);
            ly += fm.getHeight();
        }
    }

    /**
     * PART 3 collision review: the heap top and the stack frontier as two bars -
     * the heap rising from the base in green, the stack descending from the top in
     * magenta - with the free gap between them. The two addresses and the gap are
     * repeated as text beside the diagram.
     */
    private void renderCollisionReview(Graphics2D g2, int x, int y, int w, int hgt, Color edge) {
        int barW = clamp(w / 12, 14, 26);
        int barH = Math.max(28, hgt - 2);
        int barX = x, barY = y + 1;

        g2.setColor(new Color(12, 9, 26));
        g2.fillRect(barX, barY, barW, barH);
        g2.setColor(new Color(48, 40, 84));
        g2.drawRect(barX, barY, barW, barH);

        // Map [heapBase, stackTop] onto the bar: base at the bottom, stack top at the top.
        int lo = Math.max(0, model.lastFaultBase);
        int hi = Math.max(lo + 1, lo + Math.max(1, model.lastFaultLimit));
        int span = hi - lo;
        int heapTop = lo + Math.max(0, model.lastFaultOffset);
        int front = clamp(model.stackFrontier(), lo, hi);
        int yHeap = barY + barH - 1
                - (int) Math.round((heapTop - lo) / (double) span * (barH - 2));
        int yFront = barY + barH - 1
                - (int) Math.round((front - lo) / (double) span * (barH - 2));

        // Heap rises from the base (green); the stack descends from the top (magenta).
        g2.setColor(alpha(SnakeLevel.C_HEAP, 170));
        g2.fillRect(barX + 1, yHeap, barW - 2, Math.max(0, barY + barH - 1 - yHeap));
        g2.setColor(alpha(SnakeLevel.C_STACK, 170));
        g2.fillRect(barX + 1, barY + 1, barW - 2, Math.max(0, yFront - barY - 1));

        // Two markers: the heap top (white) and the stack frontier (magenta).
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(RetroTheme.TEXT_WHITE);
        g2.drawLine(barX - 4, yHeap, barX + barW + 4, yHeap);
        g2.setColor(SnakeLevel.C_STACK);
        g2.drawLine(barX - 4, yFront, barX + barW + 4, yFront);
        g2.setStroke(new BasicStroke(1f));

        g2.setFont(fHelper);
        FontMetrics fm = g2.getFontMetrics();
        int labelX = barX + barW + 6;
        int heapLy = clamp(yHeap + fm.getDescent(), barY + fm.getAscent(), barY + barH);
        int frontLy = clamp(yFront + fm.getDescent(), barY + fm.getAscent(), barY + barH);
        if (Math.abs(frontLy - heapLy) < fm.getHeight()) {
            heapLy = Math.min(barY + barH, heapLy + fm.getHeight() / 2);
            frontLy = Math.max(barY + fm.getAscent(), frontLy - fm.getHeight() / 2);
        }
        g2.setColor(new Color(150, 160, 190));
        g2.drawString("HEAP", labelX, heapLy);
        g2.drawString("STK", labelX, frontLy);

        int tx = labelX + fm.stringWidth("HEAP") + 18;
        int tw = x + w - tx;
        if (tw < 80) return;

        g2.setColor(edge);
        g2.drawString("MISTAKE REVIEW", tx, y + fm.getAscent());
        int ly = y + fm.getAscent() + fm.getHeight() + 2;
        g2.setColor(RetroTheme.TEXT_WHITE);
        int gap = Math.max(0, front - heapTop);
        g2.drawString(clip(fm, "heap top " + heapTop + " / stack " + front
                + " / gap " + gap, tw), tx, ly);
        ly += fm.getHeight();
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        for (String line : tipLines()) {
            if (line == null || ly > y + hgt) break;
            g2.drawString(clip(fm, line, tw), tx, ly);
            ly += fm.getHeight();
        }
    }

    /** One tip (a header plus one short line) for the most common fault. */
    private String[] tipLines() {
        if (model.lastFaultSegmentId > 0) {
            return new String[]{"TIP - SWITCH FAULT",
                    "move down before switching to a smaller segment."};
        }
        SnakeModel.Fault f = model.mostCommonFault();
        if (f == null) return new String[]{"TIP", "stay inside [base, base+limit)."};
        switch (f) {
            case SEGFAULT:
                return new String[]{"TIP - SEGFAULT",
                        "turn before the head reaches the limit."};
            case HEAP_CORRUPTION:
                return new String[]{"TIP - HEAP CORRUPTION",
                        "plan two moves ahead, don't bite the tail."};
            case PROTECTION_FAULT:
                return new String[]{"TIP - PROTECTION FAULT",
                        "go around read-only blocks, they reject writes."};
            case COLLISION:
                return new String[]{"TIP - COLLISION",
                        "press SPACE to free stack frames early."};
            default:
                return new String[]{"TIP", "stay inside the heap arena."};
        }
    }

    /** PART 1.1/1.4: the live menu buttons; the selected or hovered one is filled and lit. */
    private void renderOverButtons(Graphics2D g2, EndLayout e) {
        g2.setFont(fTitle);
        FontMetrics fm = g2.getFontMetrics();
        String[] btns = overButtons();
        for (int i = 0; i < btns.length && i < e.btnCount; i++) {
            boolean sel = i == overIndex || i == hoverIndex;
            int x = e.bx[i], y = e.by[i], bw = e.bw[i], bh = e.bh[i];
            Color fg = NEXT_LABEL.equals(btns[i]) ? SnakeLevel.C_HEAP
                    : (AGAIN_LABEL.equals(btns[i]) ? RetroTheme.ACCENT_CYAN : RetroTheme.ACCENT_PINK);
            if (sel) {
                g2.setColor(alpha(fg, 46));
                g2.fillRoundRect(x, y, bw, bh, 8, 8);
            }
            g2.setColor(sel ? fg : alpha(fg, 150));
            g2.drawRoundRect(x, y, bw, bh, 8, 8);
            g2.setColor(sel ? RetroTheme.TEXT_WHITE : alpha(RetroTheme.TEXT_WHITE, 190));
            g2.drawString(btns[i], x + 14, y + (bh + fm.getAscent()) / 2 - 2);
            if (sel) {
                g2.setColor(fg);
                int my = y + bh / 2;
                g2.fillPolygon(new int[]{x + bw - 20, x + bw - 20, x + bw - 10},
                        new int[]{my - 5, my + 5, my}, 3);
            }
        }
    }

    /** Compact "HI-SCORES" line: the top three scores, zero-padded. */
    private String hiScoreLine() {
        if (hiScores == null || hiScores.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("HI-SCORES");
        for (int i = 0; i < Math.min(3, hiScores.size()); i++) {
            sb.append("  ").append(String.format("%06d", hiScores.get(i).score));
        }
        return sb.toString();
    }

    /** The [H] card: the controls, and the one rule this level is about. */
    private void renderHelp(Graphics2D g2, int w, int h) {
        boolean paused = model.paused;
        g2.setColor(new Color(6, 4, 14, paused ? 205 : 150));
        g2.fillRect(0, 0, w, h);

        String line3 = model.level.stackEnabled
                ? "SPACE  return stack frames      ESC  return to OS"
                : "ESC  return to OS";
        String[] lines = {
            "\u2190\u2191\u2193\u2192 / WASD    steer the heap head",
            "P  pause / resume      R  restart      M  mute",
            line3,
            "TAB  advanced registers      F2  CRT scanlines",
        };

        g2.setFont(fHelper);
        FontMetrics fm = g2.getFontMetrics();
        int lineH = fm.getAscent() + 8;
        g2.setFont(fTitle);
        FontMetrics tfm = g2.getFontMetrics();

        int pad = 26;
        int cardW = Math.min(720, Math.max(360, w - 80));
        int cardH = pad + tfm.getAscent() + 16 + lines.length * lineH + 22 + fm.getAscent() + pad;
        int cx = boardX + (boardW - cardW) / 2;
        int cy = boardY + Math.min(Math.max(0, boardH - cardH) / 2 + 70,
                Math.max(0, boardH - cardH));

        g2.setColor(new Color(6, 4, 14, 240));
        g2.fillRoundRect(cx, cy, cardW, cardH, 12, 12);
        g2.setColor(RetroTheme.BORDER_BRIGHT);
        g2.drawRoundRect(cx, cy, cardW, cardH, 12, 12);

        int tx = cx + pad;
        int ty = cy + pad + tfm.getAscent();
        g2.setFont(fTitle);
        g2.setColor(paused ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_CYAN);
        g2.drawString(paused ? "PAUSED" : "HELP", tx, ty);

        ty += 16 + fm.getAscent();
        g2.setFont(fHelper);
        for (String s : lines) {
            g2.setColor(RetroTheme.TEXT_WHITE);
            g2.drawString(s, tx, ty);
            ty += lineH;
        }
        ty += 22;
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        g2.drawString("base <= offset < base + limit", tx, ty);
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    private static void drawRight(Graphics2D g2, String s, int right, int baseline) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, right - fm.stringWidth(s), baseline);
    }

    private static void drawCentered(Graphics2D g2, String s, int cx, int baseline) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, cx - fm.stringWidth(s) / 2, baseline);
    }

    /** Truncates with an ellipsis so a long string never runs past its box. */
    private static String clip(FontMetrics fm, String s, int maxW) {
        if (s == null || maxW <= 0 || fm.stringWidth(s) <= maxW) return s == null ? "" : s;
        String ell = "\u2026";
        int ew = fm.stringWidth(ell);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (fm.stringWidth(sb.toString() + s.charAt(i)) + ew > maxW) break;
            sb.append(s.charAt(i));
        }
        return sb.length() == 0 ? "" : sb + ell;
    }

    /**
     * A3: Splits {@code s} at a word boundary so the first part fits {@code maxW}.
     * Returns a two-element array: [line1, line2]. line2 is "" if the whole string fits.
     */
    private static String[] wrapLine(FontMetrics fm, String s, int maxW) {
        if (s == null) return new String[]{"", ""};
        if (fm.stringWidth(s) <= maxW) return new String[]{s, ""};
        // Find the last space whose prefix fits.
        int lastSpace = -1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') {
                if (fm.stringWidth(s.substring(0, i)) <= maxW) lastSpace = i;
            }
        }
        if (lastSpace < 0) {
            // No space found — just clip line1 and leave line2 empty.
            return new String[]{clip(fm, s, maxW), ""};
        }
        return new String[]{s.substring(0, lastSpace), s.substring(lastSpace + 1)};
    }

    // =====================================================================
    // STEP 5 A: the visible text budget
    // =====================================================================
    /**
     * Counts every text item the steady-state gameplay screen draws. The number
     * is the acceptance number for STEP 5 A: 25 or fewer, with the help card, the
     * advanced panel and the fading key-caps excluded because none of them is up
     * during normal play.
     */
    // ── C1: Briefing card rendering ──────────────────────────────────────
    private void renderBriefing(Graphics2D g2, int w, int h) {
        int cardW = Math.min(540, w - 40);
        int cardH = 220;
        int cardX = (w - cardW) / 2;
        int cardY = (h - cardH) / 2;

        g2.setColor(new Color(6, 4, 14, 230));
        g2.fillRoundRect(cardX, cardY, cardW, cardH, 12, 12);
        g2.setColor(RetroTheme.ACCENT_CYAN);
        g2.drawRoundRect(cardX, cardY, cardW, cardH, 12, 12);

        int pad = 16;
        int cy = cardY + pad;

        // Title
        g2.setFont(fTitle);
        FontMetrics tfm = g2.getFontMetrics();
        g2.setColor(RetroTheme.ACCENT_CYAN);
        String title = "[ LEVEL " + model.level.number + " : " + model.level.name.toUpperCase() + " ]";
        g2.drawString(title, cardX + pad, cy + tfm.getAscent());
        cy += tfm.getAscent() + 14;

        // Segment Table Row
        g2.setFont(fHelper);
        FontMetrics hfm = g2.getFontMetrics();
        g2.setColor(RetroTheme.TEXT_DIM);
        g2.drawString("SEG #", cardX + pad, cy + hfm.getAscent());
        g2.drawString("NAME", cardX + pad + 50, cy + hfm.getAscent());
        g2.drawString("BASE", cardX + pad + 180, cy + hfm.getAscent());
        g2.drawString("LIMIT", cardX + pad + 260, cy + hfm.getAscent());
        g2.drawString("PERM", cardX + pad + 340, cy + hfm.getAscent());
        cy += hfm.getAscent() + 6;

        g2.setColor(new Color(60, 50, 90));
        g2.drawLine(cardX + pad, cy, cardX + cardW - pad, cy);
        cy += 6;

        if (model.isSegmentSwitchLevel()) {
            for (int i = 0; i < Math.min(3, model.level.segments.size()); i++) {
                SnakeLevel.SegmentDef s = model.level.segments.get(i);
                g2.setColor(SegmentStyle.segmentColor(i));
                g2.fillRect(cardX + pad, cy + 2, 8, 8);
                g2.drawString(String.valueOf(s.id), cardX + pad + 14, cy + hfm.getAscent());
                g2.setColor(RetroTheme.TEXT_WHITE);
                g2.drawString(s.name, cardX + pad + 50, cy + hfm.getAscent());
                g2.drawString(String.valueOf(s.base), cardX + pad + 180, cy + hfm.getAscent());
                g2.drawString(String.valueOf(s.limit), cardX + pad + 260, cy + hfm.getAscent());
                g2.setColor(s.readOnly ? RetroTheme.ACCENT_PINK : RetroTheme.ACCENT_GREEN);
                g2.drawString(s.readOnly ? "R" : "R/W", cardX + pad + 340, cy + hfm.getAscent());
                cy += hfm.getHeight();
            }
        } else {
            g2.setColor(SnakeLevel.C_HEAP);
            g2.fillRect(cardX + pad, cy + 2, 8, 8);
            g2.drawString("0", cardX + pad + 14, cy + hfm.getAscent());
            g2.setColor(RetroTheme.TEXT_WHITE);
            String nm = model.level.sourceName != null ? model.level.sourceName : "HEAP";
            g2.drawString(nm, cardX + pad + 50, cy + hfm.getAscent());
            g2.drawString(String.valueOf(model.currentSegmentBase()), cardX + pad + 180, cy + hfm.getAscent());
            g2.drawString(String.valueOf(model.currentSegmentLimit()), cardX + pad + 260, cy + hfm.getAscent());
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString("R/W", cardX + pad + 340, cy + hfm.getAscent());
            cy += hfm.getHeight();
        }

        cy += 10;
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        String rule = "RULE: offset < limit -> PA = base + offset (>= limit is a SEGFAULT)";
        g2.drawString(rule, cardX + pad, cy + hfm.getAscent());

        cy += hfm.getHeight() + 14;
        g2.setColor(new Color(120, 130, 160));
        String skip = "Press any key to start";
        g2.drawString(skip, cardX + (cardW - hfm.stringWidth(skip)) / 2, cy + hfm.getAscent());
    }

    private String textBudgetLine() {
        List<String> items = new ArrayList<>();
        items.add("score");
        items.add("SCORE label");
        if (model.level.goalMallocs > 0) items.add("M n/goal");
        items.add("OFFSET title");
        items.add("offset/limit");
        items.add("FREE GAP title");
        items.add("free value");
        items.add("HEAP");
        items.add("FREE");
        if (model.level.stackEnabled) items.add("STACK");
        items.add("ticker ts");
        items.add("ticker line");
        items.add("dmesg tag");
        int base = items.size();
        int caps = FOOTER_CAPS.length;
        return "SNAKE TEXT gameplay items=" + base + " (key-caps add " + caps
                + " for the first " + (FOOTER_HOLD_MS / 1000) + "s) -> peak " + (base + caps)
                + "  list=" + items;
    }
}
