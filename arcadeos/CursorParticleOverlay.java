package arcadeos;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

/**
 * AAA 8-Bit Retro Arcade Cursor Particle Overlay:
 * Generates sparkling neon stardust, pixel sparks, and starlight trails
 * that smoothly glide behind the user's mouse cursor across the UI.
 *
 * Designed as a non-blocking Swing GlassPane:
 * - {@code contains(x, y)} returns false so ALL mouse clicks pass through 100%.
 * - Uses global AWTEventListener to passively track cursor movement and clicks.
 * - Auto-pauses animation when no particles are active (zero idle CPU).
 * - Uses dirty-region repainting so underlying cards & tables stay 100% rock-solid.
 */
public class CursorParticleOverlay extends JComponent {

    public static class SparkParticle {
        float x, y;
        float vx, vy;
        float life;     // 1.0 down to 0.0
        float decay;    // fading rate
        float size;     // pixel size (2 to 5px)
        Color color;
        int shape;      // 0=pixel square, 1=diamond star, 2=cross sparkle

        public SparkParticle(float x, float y, float vx, float vy, float size, float decay, Color color, int shape) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.decay = decay;
            this.color = color;
            this.shape = shape;
            this.life = 1.0f;
        }
    }

    private final List<SparkParticle> particles = new ArrayList<>();
    private Timer animTimer;
    private final java.util.Random rng = new java.util.Random();

    // Vibrant Synthwave / 8-Bit Arcade Stardust Palette
    private static final Color[] PALETTE = {
        new Color(40, 240, 255),   // Electric Cyan
        new Color(255, 42, 109),   // Neon Pink
        new Color(255, 215, 0),    // Starlight Gold
        new Color(50, 255, 180),   // Neon Mint
        new Color(255, 255, 255),  // Pure White
        new Color(190, 110, 255)   // Cyber Violet
    };

    private int lastX = -999;
    private int lastY = -999;
    private Rectangle lastDirtyRect = null;

    public CursorParticleOverlay() {
        setOpaque(false);

        // 60 FPS animation loop with dirty-region repainting
        animTimer = new Timer(16, e -> {
            Rectangle dirty = null;
            synchronized (particles) {
                if (particles.isEmpty()) {
                    animTimer.stop();
                    if (lastDirtyRect != null) {
                        repaint(lastDirtyRect);
                        lastDirtyRect = null;
                    }
                    return;
                }

                int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

                for (int i = particles.size() - 1; i >= 0; i--) {
                    SparkParticle p = particles.get(i);
                    p.x += p.vx;
                    p.y += p.vy;
                    // Gentle upward stardust float + slight air resistance
                    p.vy -= 0.035f;
                    p.vx *= 0.95f;
                    p.life -= p.decay;

                    if (p.life <= 0.0f) {
                        particles.remove(i);
                    } else {
                        int px = (int) p.x;
                        int py = (int) p.y;
                        int sz = (int) p.size + 6;
                        if (px - sz < minX) minX = px - sz;
                        if (py - sz < minY) minY = py - sz;
                        if (px + sz > maxX) maxX = px + sz;
                        if (py + sz > maxY) maxY = py + sz;
                    }
                }

                if (minX <= maxX && minY <= maxY) {
                    dirty = new Rectangle(minX - 4, minY - 4, (maxX - minX) + 8, (maxY - minY) + 8);
                }
            }

            // Union with last dirty rect to cleanly erase disappearing particles
            Rectangle toRepaint = dirty;
            if (lastDirtyRect != null) {
                if (toRepaint != null) {
                    toRepaint = toRepaint.union(lastDirtyRect);
                } else {
                    toRepaint = lastDirtyRect;
                }
            }
            lastDirtyRect = dirty;

            if (toRepaint != null) {
                repaint(toRepaint);
            }
        });

        // Global AWT Event Listener to passively capture mouse movement across any card or component
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
                if (event instanceof MouseEvent me) {
                    if (!isShowing()) return;
                    Point p = me.getLocationOnScreen();
                    SwingUtilities.convertPointFromScreen(p, CursorParticleOverlay.this);
                    int id = me.getID();
                    if (id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_DRAGGED) {
                        onMouseMove(p.x, p.y);
                    } else if (id == MouseEvent.MOUSE_PRESSED) {
                        onMouseClick(p.x, p.y);
                    }
                }
            }, AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
        } catch (SecurityException ignored) {}
    }

    /**
     * CRITICAL: Returning false allows every click, hover, and drag
     * to seamlessly pass through to the buttons, sliders, and panels underneath!
     */
    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    /** Spawns trail particles on mouse move */
    public void onMouseMove(int x, int y) {
        if (lastX == -999) {
            lastX = x;
            lastY = y;
            return;
        }

        double dist = Math.hypot(x - lastX, y - lastY);
        // Only spawn when mouse is actually moving
        if (dist >= 3.0) {
            int count = Math.min(4, Math.max(1, (int)(dist / 6.0)));
            synchronized (particles) {
                if (particles.size() < 100) {
                    for (int i = 0; i < count; i++) {
                        float interp = (float)i / count;
                        float px = lastX + (x - lastX) * interp;
                        float py = lastY + (y - lastY) * interp;

                        float angle = (float)(rng.nextDouble() * Math.PI * 2);
                        float speed = (float)(0.4 + rng.nextDouble() * 1.5);
                        float vx = (float)(Math.cos(angle) * speed);
                        float vy = (float)(Math.sin(angle) * speed - 0.4); // upward drift

                        float size = 2.5f + rng.nextFloat() * 2.5f;
                        float decay = 0.030f + rng.nextFloat() * 0.035f; // ~400ms lifespan
                        Color col = PALETTE[rng.nextInt(PALETTE.length)];
                        int shape = rng.nextInt(3);

                        particles.add(new SparkParticle(px, py, vx, vy, size, decay, col, shape));
                    }
                }
            }
            lastX = x;
            lastY = y;

            if (!animTimer.isRunning()) {
                animTimer.start();
            }
        }
    }

    /** Spawns a celebratory radial spark burst on mouse click */
    public void onMouseClick(int x, int y) {
        synchronized (particles) {
            int burstCount = 10;
            for (int i = 0; i < burstCount; i++) {
                double angle = (Math.PI * 2 * i / burstCount) + (rng.nextDouble() * 0.4 - 0.2);
                float speed = (float)(1.5 + rng.nextDouble() * 2.2);
                float vx = (float)(Math.cos(angle) * speed);
                float vy = (float)(Math.sin(angle) * speed);

                float size = 3.0f + rng.nextFloat() * 2.5f;
                float decay = 0.035f + rng.nextFloat() * 0.025f;
                Color col = PALETTE[rng.nextInt(PALETTE.length)];
                int shape = rng.nextInt(3);

                particles.add(new SparkParticle(x, y, vx, vy, size, decay, col, shape));
            }
        }
        if (!animTimer.isRunning()) {
            animTimer.start();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (particles.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        synchronized (particles) {
            for (SparkParticle p : particles) {
                if (p.life <= 0.01f) continue;

                int alpha = Math.max(0, Math.min(255, (int)(p.life * 235)));
                Color baseCol = p.color;
                Color drawCol = new Color(baseCol.getRed(), baseCol.getGreen(), baseCol.getBlue(), alpha);

                int px = (int) p.x;
                int py = (int) p.y;
                int sz = Math.max(2, (int) p.size);

                // 1. Soft ambient glow aura around each particle
                if (sz >= 3 && alpha > 40) {
                    Color glowCol = new Color(baseCol.getRed(), baseCol.getGreen(), baseCol.getBlue(), alpha / 4);
                    g2.setColor(glowCol);
                    g2.fillOval(px - sz, py - sz, sz * 2, sz * 2);
                }

                g2.setColor(drawCol);
                if (p.shape == 0) {
                    // Crisp 8-Bit Pixel Square
                    g2.fillRect(px - sz / 2, py - sz / 2, sz, sz);
                } else if (p.shape == 1) {
                    // Starlight Diamond (rotated square)
                    int[] dx = {px, px + sz, px, px - sz};
                    int[] dy = {py - sz, py, py + sz, py};
                    g2.fillPolygon(dx, dy, 4);
                } else {
                    // 4-Pointed Sparkle Star
                    int arm = sz + 1;
                    g2.fillRect(px - arm, py, arm * 2 + 1, 1);
                    g2.fillRect(px, py - arm, 1, arm * 2 + 1);
                    // Center core in white for radiant shimmer
                    g2.setColor(new Color(255, 255, 255, alpha));
                    g2.fillRect(px, py, 1, 1);
                }
            }
        }

        g2.dispose();
    }
}
