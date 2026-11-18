package game.snake;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

/**
 * HEAP vs STACK SNAKE - game-local chiptune cue generator.
 *
 * This is the game's own sound helper so the Snake package never reaches into
 * Fault Defender's package-private audio class. Every cue is a short mono blip
 * synthesised on the fly, so the game needs no audio assets. If the mixers are
 * unavailable the failure is swallowed and the game stays playable but silent.
 * A single mute flag backs the in-game [M] toggle and the SOUND button.
 */
final class GameSfx {

    private static final float RATE = 44100f;
    private static volatile boolean muted = false;
    private static volatile double volume = 0.55;

    private GameSfx() { }

    static boolean isMuted() { return muted; }

    static boolean toggleMute() {
        muted = !muted;
        return muted;
    }

    static void hit()     { square(520, 45, 0.24, 0.35); }
    static void fault()   { tone(150, 260, 0.40); }
    static void uiClick() { square(700, 30, 0.20, 0.25); }
    /** PART 3: a stack frame is pushed - a low, dull thud. */
    static void thud()    { tone(110, 90, 0.42); }
    /** PART 3: frames are popped - a quick descending whoosh. */
    static void whoosh()  { sweep(720, 260, 130, 0.30); }
    /** PART 3: the free gap is critically small - a short two-beep alarm. */
    static void alarm()   { square(880, 90, 0.22, 0.5); }

    /** Square wave: crunchy arcade lead. */
    private static void square(final double freq, final int ms, final double vol, final double duty) {
        play(freq, ms, vol, true, duty);
    }

    /** Sine/triangle-ish tone: softer cue. */
    private static void tone(final double freq, final int ms, final double vol) {
        play(freq, ms, vol, false, 0.5);
    }

    /** PART 3: a frequency glide from {@code f0} to {@code f1} - the return whoosh. */
    private static void sweep(final double f0, final double f1, final int ms, final double vol) {
        if (muted) return;
        final double v0 = vol * volume;
        Thread t = new Thread(() -> {
            try {
                int n = Math.max(1, (int) (RATE * ms / 1000.0));
                byte[] buf = new byte[n];
                double phase = 0;
                for (int i = 0; i < n; i++) {
                    double f = f0 + (f1 - f0) * (i / (double) n);
                    phase += 2 * Math.PI * f / RATE;
                    double attack = Math.min(1.0, i / (RATE * 0.004));
                    double release = Math.min(1.0, (n - i) / (RATE * 0.05));
                    double v = Math.sin(phase) * v0 * attack * release;
                    buf[i] = (byte) (Math.max(-1.0, Math.min(1.0, v)) * 127);
                }
                AudioFormat fmt = new AudioFormat(RATE, 8, 1, true, false);
                final Clip clip = AudioSystem.getClip();
                clip.addLineListener(ev -> { if (ev.getType() == LineEvent.Type.STOP) clip.close(); });
                clip.open(fmt, buf, 0, buf.length);
                clip.start();
            } catch (Exception ignored) {
                // No audio device / mixer busy: stay silent, keep playing.
            }
        }, "snake-sfx");
        t.setDaemon(true);
        t.start();
    }

    private static void play(final double freq, final int ms, final double vol,
                             final boolean square, final double duty) {
        if (muted) return;
        final double v0 = vol * volume;
        Thread t = new Thread(() -> {
            try {
                int n = Math.max(1, (int) (RATE * ms / 1000.0));
                byte[] buf = new byte[n];
                for (int i = 0; i < n; i++) {
                    double phase = (freq * i / RATE) % 1.0;
                    double wave = square ? (phase < duty ? 1.0 : -1.0) : Math.sin(2 * Math.PI * phase);
                    double attack = Math.min(1.0, i / (RATE * 0.004));
                    double release = Math.min(1.0, (n - i) / (RATE * 0.05));
                    double v = wave * v0 * attack * release;
                    buf[i] = (byte) (Math.max(-1.0, Math.min(1.0, v)) * 127);
                }
                AudioFormat fmt = new AudioFormat(RATE, 8, 1, true, false);
                final Clip clip = AudioSystem.getClip();
                clip.addLineListener(ev -> { if (ev.getType() == LineEvent.Type.STOP) clip.close(); });
                clip.open(fmt, buf, 0, buf.length);
                clip.start();
            } catch (Exception ignored) {
                // No audio device / mixer busy: stay silent, keep playing.
            }
        }, "snake-sfx");
        t.setDaemon(true);
        t.start();
    }
}
