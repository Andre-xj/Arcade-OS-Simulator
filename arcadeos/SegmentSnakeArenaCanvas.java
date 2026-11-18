package arcadeos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Live Animated Segment Snake Memory Arena Canvas for Mission 05 Segmentation.
 * Simulates a Cyber Snake navigating within allocated variable-length memory segments [0 .. Limit).
 * Demonstrates in-bounds token acquisition vs out-of-bounds MMU Limit Trap collisions.
 */
public class SegmentSnakeArenaCanvas extends JPanel {

    public static final int PHASE_IDLE = 0;
    public static final int PHASE_SLITHER = 1;
    public static final int PHASE_EAT_TOKEN = 2;
    public static final int PHASE_CRASH_TRAP = 3;

    private final SegmentationPanel parent;
    private javax.swing.Timer animTimer;

    // Current Segment Parameters
    private String segName = "Audio_Engine";
    private int segBase = 6300;
    private int segLimit = 500;
    private int probeOffset = 170;
    private int probePA = 6470;
    private boolean isValid = true;

    // Animation & State Machine
    private int phase = PHASE_IDLE;
    private int tickCount = 0;
    private int phaseTicks = 0;
    private float shockwaveRadius = 0f;
    private float shockwaveAlpha = 0f;

    // Cyber Snake Body Coordinates (Chain of joints)
    private static final int SNAKE_JOINTS = 7;
    private final float[] jointX = new float[SNAKE_JOINTS];
    private final float[] jointY = new float[SNAKE_JOINTS];
    private float targetHeadX = 140f;
    private float targetHeadY = 88f;
    private float headBob = 0f;
    private boolean tongueOut = false;

    // Sprites
    private BufferedImage snakeHeadSprite;
    private BufferedImage snakeHeadFault;

    // Particles & Floating Text
    private final List<ArenaParticle> particles = new ArrayList<>();
    private final List<ArenaFloatingText> floatingTexts = new ArrayList<>();

    // Banner message
    private String bannerStatus = "STANDBY: Base segment bound to hardware limits.";
    private Color bannerColor = RetroTheme.ACCENT_CYAN;

    public SegmentSnakeArenaCanvas(SegmentationPanel parent) {
        this.parent = parent;
        setBackground(new Color(11, 9, 24));
        setOpaque(true);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 48, 100), 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));

        snakeHeadSprite = PixelSprites.createSnakeSprite(22, RetroTheme.ACCENT_GREEN);
        snakeHeadFault = PixelSprites.createSnakeSprite(22, RetroTheme.ACCENT_PINK);

        // Initialize snake joints
        for (int i = 0; i < SNAKE_JOINTS; i++) {
            jointX[i] = 70f - i * 8f;
            jointY[i] = 88f;
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (phase == PHASE_IDLE) {
                    addFloatingText(jointX[0], jointY[0] - 20, ">> PROBE DISPATCHED", RetroTheme.ACCENT_CYAN);
                    addBurstParticles(jointX[0] + 11, jointY[0] + 11, RetroTheme.ACCENT_GREEN, 10);
                    headBob = 10f;
                    triggerAction(probeOffset);
                }
            }
        });

        startAnimationLoop();
    }

    private void startAnimationLoop() {
        animTimer = new javax.swing.Timer(16, e -> {
            if (!isShowing()) return;
            tickCount++;
            phaseTicks++;
            headBob = (float) (Math.sin(tickCount * 0.1) * 3.0);
            tongueOut = ((tickCount / 14) % 2 == 1);

            updateSnakeMotion();
            updateParticles();
            repaint();
        });
        animTimer.start();
    }

    public void stopAnimationLoop() {
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(450, 215);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(320, 160);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 260);
    }

    /**
     * Resets the arena to standby state without running/slithering the snake.
     */
    public void setStandby(String name, int base, int limit, int offset) {
        this.segName = (name == null || name.trim().isEmpty()) ? "Segment" : name;
        this.segBase = Math.max(0, base);
        this.segLimit = Math.max(1, limit);
        this.probeOffset = Math.max(0, offset);
        this.probePA = base + offset;
        this.isValid = (offset < limit);
        this.phase = PHASE_IDLE;
        this.phaseTicks = 0;
        this.shockwaveAlpha = 0f;
        this.shockwaveRadius = 0f;
        this.floatingTexts.clear();
        this.particles.clear();

        for (int i = 0; i < SNAKE_JOINTS; i++) {
            jointX[i] = 70f - i * 8f;
            jointY[i] = 88f;
        }
        targetHeadX = 70f;
        targetHeadY = 88f;

        this.bannerStatus = "STANDBY: MMU Armed. Click [>> VALIDATE] or [STEP >>] to translate.";
        this.bannerColor = RetroTheme.ACCENT_CYAN;
        repaint();
    }

    /**
     * Updates the memory probe parameters and triggers the corresponding arena animation.
     */
    public void updateProbe(String name, int base, int limit, int offset, int pa, boolean valid) {
        this.segName = (name == null || name.trim().isEmpty()) ? "Segment" : name;
        this.segBase = Math.max(0, base);
        this.segLimit = Math.max(1, limit);
        this.probeOffset = Math.max(0, offset);
        this.probePA = pa;
        this.isValid = valid;

        triggerAction(offset);
    }

    private void triggerAction(int offset) {
        phaseTicks = 0;
        int runwayW = getWidth() - 90;
        if (runwayW < 120) runwayW = 320;
        float runwayStartX = 45f;

        // Visual position mapping:
        // Offset 0 = runwayStartX, Offset segLimit = runwayStartX + runwayW * 0.72f
        float limitPixelX = runwayStartX + runwayW * 0.72f;
        float offsetPixelX;

        if (offset <= segLimit) {
            offsetPixelX = runwayStartX + ((float) offset / segLimit) * (limitPixelX - runwayStartX);
        } else {
            float overflowRatio = Math.min(1.0f, (float) (offset - segLimit) / Math.max(1, segLimit * 0.4f));
            offsetPixelX = limitPixelX + overflowRatio * (runwayW - (limitPixelX - runwayStartX));
        }

        targetHeadX = Math.max(runwayStartX, Math.min(runwayStartX + runwayW - 24, offsetPixelX));
        targetHeadY = 88f;

        if (isValid) {
            phase = PHASE_SLITHER;
            bannerStatus = String.format("VALID ACCESS: Offset %d < Limit %d -> PA = %d [IN-BOUNDS]", probeOffset, segLimit, probePA);
            bannerColor = RetroTheme.ACCENT_GREEN;
        } else {
            phase = PHASE_SLITHER;
            bannerStatus = String.format("LIMIT TRAP: Offset %d >= Limit %d -> SEGFAULT [OUT-OF-BOUNDS]", probeOffset, segLimit);
            bannerColor = RetroTheme.ACCENT_PINK;
        }
    }

    private void updateSnakeMotion() {
        // Move head towards target
        float dx = targetHeadX - jointX[0];
        float dy = targetHeadY - jointY[0];
        float dist = (float) Math.hypot(dx, dy);

        if (phase == PHASE_SLITHER) {
            if (dist > 4f) {
                float speed = Math.max(3.5f, dist * 0.14f);
                jointX[0] += (dx / dist) * speed;
                jointY[0] += (dy / dist) * speed;
                // Add footstep/slither particles occasionally
                if (tickCount % 6 == 0) {
                    particles.add(new ArenaParticle(jointX[0] + 6, jointY[0] + 16, (float)(Math.random() * 2 - 1), 1f, new Color(57, 255, 140, 160), 16));
                }
            } else {
                // Head arrived at target
                if (isValid) {
                    phase = PHASE_EAT_TOKEN;
                    phaseTicks = 0;
                    shockwaveRadius = 2f;
                    shockwaveAlpha = 1.0f;
                    addBurstParticles(targetHeadX + 11, targetHeadY + 11, RetroTheme.ACCENT_GREEN, 18);
                    addBurstParticles(targetHeadX + 11, targetHeadY + 11, RetroTheme.ACCENT_YELLOW, 12);
                    addFloatingText(targetHeadX, targetHeadY - 20, "[PASS] PA = " + probePA + " BYTES", RetroTheme.ACCENT_GREEN);
                } else {
                    phase = PHASE_CRASH_TRAP;
                    phaseTicks = 0;
                    shockwaveRadius = 2f;
                    shockwaveAlpha = 1.0f;
                    addBurstParticles(targetHeadX + 11, targetHeadY + 11, RetroTheme.ACCENT_PINK, 24);
                    addBurstParticles(targetHeadX + 11, targetHeadY + 11, RetroTheme.ACCENT_YELLOW, 16);
                    addFloatingText(targetHeadX, targetHeadY - 20, "[TRAP] LIMIT EXCEEDED!", RetroTheme.ACCENT_PINK);
                }
            }
        } else if (phase == PHASE_EAT_TOKEN) {
            if (phaseTicks > 30) {
                phase = PHASE_IDLE;
            }
        } else if (phase == PHASE_CRASH_TRAP) {
            // Recoil shake
            if (phaseTicks < 18) {
                jointX[0] += (float) (Math.sin(phaseTicks * 1.5) * 4.0);
            }
            if (phaseTicks > 35) {
                phase = PHASE_IDLE;
            }
        }

        // Snake joints follow head like IK string
        for (int i = 1; i < SNAKE_JOINTS; i++) {
            float jdx = jointX[i - 1] - jointX[i];
            float jdy = jointY[i - 1] - jointY[i];
            float jdist = (float) Math.hypot(jdx, jdy);
            float targetDist = 9.0f;
            if (jdist > targetDist) {
                jointX[i] = jointX[i - 1] - (jdx / jdist) * targetDist;
                jointY[i] = jointY[i - 1] - (jdy / jdist) * targetDist;
            }
            // Small idle wave
            jointY[i] += (float) (Math.sin(tickCount * 0.15 + i * 0.8) * 0.4);
        }

        // Shockwave expansion
        if (shockwaveAlpha > 0.05f) {
            shockwaveRadius += 3.2f;
            shockwaveAlpha *= 0.88f;
        } else {
            shockwaveAlpha = 0f;
        }
    }

    private void updateParticles() {
        Iterator<ArenaParticle> it = particles.iterator();
        while (it.hasNext()) {
            ArenaParticle p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            p.life--;
            if (p.life <= 0) it.remove();
        }

        Iterator<ArenaFloatingText> txtIt = floatingTexts.iterator();
        while (txtIt.hasNext()) {
            ArenaFloatingText t = txtIt.next();
            t.y -= 0.8f;
            t.life--;
            if (t.life <= 0) txtIt.remove();
        }
    }

    private void addBurstParticles(float x, float y, Color c, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            float speed = (float) (1.5 + Math.random() * 3.5);
            particles.add(new ArenaParticle(x, y, (float) (Math.cos(angle) * speed), (float) (Math.sin(angle) * speed), c, 20 + (int) (Math.random() * 15)));
        }
    }

    private void addFloatingText(float x, float y, String txt, Color c) {
        floatingTexts.clear(); // Prevents multiple floating texts from stacking/colliding!
        floatingTexts.add(new ArenaFloatingText(x, y, txt, c, 50));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Dark Tech Grid Background
        g2.setColor(new Color(11, 9, 24));
        g2.fillRect(0, 0, w, h);

        g2.setColor(new Color(36, 26, 58, 120));
        for (int x = 0; x < w; x += 22) g2.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += 22) g2.drawLine(0, y, w, y);

        // 2. Top Header HUD Bar
        paintTopHUD(g2, w);

        // 3. Segment Runway & Memory Rails
        paintSegmentRunway(g2, w, h);

        // 4. Memory Token / Food Pellet [M]
        paintMemoryPellet(g2, w, h);

        // 5. Shockwave Ring
        if (shockwaveAlpha > 0.05f) {
            Color sc = isValid ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_PINK;
            g2.setColor(new Color(sc.getRed(), sc.getGreen(), sc.getBlue(), (int) (shockwaveAlpha * 220)));
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawOval((int) (targetHeadX + 11 - shockwaveRadius), (int) (targetHeadY + 11 - shockwaveRadius), (int) (shockwaveRadius * 2), (int) (shockwaveRadius * 2));
        }

        // 6. Cyber Snake Entity
        paintCyberSnake(g2);

        // 7. Particles & Floating Text
        paintParticles(g2);

        // 8. Bottom Telemetry Banner
        paintBottomBanner(g2, w, h);

        g2.dispose();
    }

    private void paintTopHUD(Graphics2D g2, int w) {
        g2.setColor(new Color(22, 16, 42, 220));
        g2.fillRect(0, 0, w, 28);
        g2.setColor(new Color(65, 48, 100));
        g2.drawLine(0, 28, w, 28);

        // Title & Segment Tag
        g2.setFont(new Font("Consolas", Font.BOLD, 11));
        g2.setColor(RetroTheme.ACCENT_CYAN);
        g2.drawString(">> HEAP SNAKE // " + segName.toUpperCase(), 12, 18);

        // Right bound specs
        String boundsStr = String.format("BASE:%d  LIMIT:%d  d:%d  PA:%d", segBase, segLimit, probeOffset, probePA);
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        int bw = fm.stringWidth(boundsStr);

        g2.setColor(new Color(210, 205, 240));
        g2.drawString(boundsStr, w - bw - 10, 18);
    }

    private void paintSegmentRunway(Graphics2D g2, int w, int h) {
        float runwayStartX = 45f;
        int runwayW = w - 90;
        if (runwayW < 120) runwayW = 320;
        int runwayY = 70;
        int runwayH = 46;

        float limitPixelX = runwayStartX + runwayW * 0.72f;

        // [A] Valid Segment Zone: [0 .. Limit)
        int validW = (int) (limitPixelX - runwayStartX);
        GradientPaint validGp = new GradientPaint(
                runwayStartX, runwayY, new Color(18, 55, 40, 180),
                runwayStartX, runwayY + runwayH, new Color(8, 30, 22, 200)
        );
        g2.setPaint(validGp);
        g2.fill(new RoundRectangle2D.Float(runwayStartX, runwayY, validW, runwayH, 8, 8));

        // Green boundary border
        g2.setColor(new Color(57, 255, 140, 180));
        g2.setStroke(new BasicStroke(1.6f));
        g2.draw(new RoundRectangle2D.Float(runwayStartX, runwayY, validW, runwayH, 8, 8));

        // Ticks inside valid zone
        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        g2.setColor(new Color(57, 255, 140, 220));
        g2.drawString("0 [BASE: " + segBase + "]", runwayStartX + 4, runwayY + runwayH - 8);

        // Midpoint tick
        float midX = runwayStartX + validW / 2f;
        g2.setColor(new Color(57, 255, 140, 100));
        g2.drawLine((int) midX, runwayY + 4, (int) midX, runwayY + runwayH - 4);
        g2.drawString(String.valueOf(segLimit / 2), midX - 8, runwayY + 16);

        // [B] Limit Barrier & Electric Fence at Limit
        int fenceX = (int) limitPixelX;
        // Electric fence pulse
        float zapPulse = (float) (0.6 + 0.4 * Math.sin(tickCount * 0.25));
        g2.setColor(new Color(255, 211, 25, (int) (zapPulse * 220)));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawLine(fenceX, runwayY - 8, fenceX, runwayY + runwayH + 8);

        // Barrier Insulator Posts
        g2.setColor(new Color(255, 211, 25));
        g2.fillRect(fenceX - 3, runwayY - 10, 6, 6);
        g2.fillRect(fenceX - 3, runwayY + runwayH + 4, 6, 6);
        g2.drawString("LIMIT: " + segLimit, fenceX - 25, runwayY - 14);

        // [C] Overflow Forbidden Trap Zone: [Limit .. Overflow]
        int overflowW = (int) (runwayStartX + runwayW - limitPixelX);
        if (overflowW > 10) {
            GradientPaint trapGp = new GradientPaint(
                    limitPixelX, runwayY, new Color(75, 18, 28, 190),
                    limitPixelX, runwayY + runwayH, new Color(45, 10, 18, 220)
            );
            g2.setPaint(trapGp);
            g2.fill(new RoundRectangle2D.Float(limitPixelX, runwayY, overflowW, runwayH, 8, 8));

            // Hazard diagonal stripes
            g2.setColor(new Color(255, 42, 141, 75));
            g2.setStroke(new BasicStroke(1.5f));
            for (int sx = (int) limitPixelX - 20; sx < limitPixelX + overflowW + 30; sx += 14) {
                g2.drawLine(sx, runwayY + runwayH, sx + 14, runwayY);
            }

            g2.setColor(new Color(255, 74, 92, 220));
            g2.draw(new RoundRectangle2D.Float(limitPixelX, runwayY, overflowW, runwayH, 8, 8));

            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.setColor(new Color(255, 74, 92));
            g2.drawString("SEGFAULT TRAP ZONE", limitPixelX + 8, runwayY + runwayH - 8);
        }
    }

    private void paintMemoryPellet(Graphics2D g2, int w, int h) {
        float runwayStartX = 45f;
        int runwayW = w - 90;
        if (runwayW < 120) runwayW = 320;
        float limitPixelX = runwayStartX + runwayW * 0.72f;

        float pelletX;
        if (probeOffset <= segLimit) {
            pelletX = runwayStartX + ((float) probeOffset / segLimit) * (limitPixelX - runwayStartX);
        } else {
            float overflowRatio = Math.min(1.0f, (float) (probeOffset - segLimit) / Math.max(1, segLimit * 0.4f));
            pelletX = limitPixelX + overflowRatio * (runwayW - (limitPixelX - runwayStartX));
        }

        float pelletY = 88f + (float) (Math.sin(tickCount * 0.18) * 3.5);

        // Outer glow
        Color pc = isValid ? RetroTheme.ACCENT_YELLOW : RetroTheme.ACCENT_PINK;
        g2.setColor(new Color(pc.getRed(), pc.getGreen(), pc.getBlue(), 80));
        g2.fillOval((int) pelletX - 10, (int) pelletY - 10, 20, 20);

        // Core Pellet Diamond [M]
        g2.setColor(pc);
        int[] dx = { (int) pelletX, (int) pelletX + 6, (int) pelletX, (int) pelletX - 6 };
        int[] dy = { (int) pelletY - 6, (int) pelletY, (int) pelletY + 6, (int) pelletY };
        g2.fillPolygon(dx, dy, 4);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 9));
        g2.drawString("M", pelletX - 3, pelletY + 3);

        // Target offset readout
        g2.setFont(new Font("Consolas", Font.BOLD, 9));
        g2.setColor(pc);
        g2.drawString("d:" + probeOffset, pelletX - 12, pelletY + 16);
    }

    private void paintCyberSnake(Graphics2D g2) {
        Color primaryAccent = isValid ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_PINK;

        // 1. Cyber Spine / Bus Data Trail connecting joints
        g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(primaryAccent.getRed(), primaryAccent.getGreen(), primaryAccent.getBlue(), 120));
        for (int i = SNAKE_JOINTS - 1; i >= 1; i--) {
            g2.drawLine((int) jointX[i], (int) jointY[i], (int) jointX[i - 1], (int) jointY[i - 1]);
        }
        g2.setStroke(new BasicStroke(1f));

        // 2. Draw body joints back-to-front
        for (int i = SNAKE_JOINTS - 1; i >= 1; i--) {
            float jx = jointX[i];
            float jy = jointY[i];
            float size = 15f - i * 1.1f;

            Color bodyColor;
            if (isValid) {
                int greenVal = Math.max(130, 255 - i * 16);
                bodyColor = new Color(25, greenVal, 95);
            } else {
                bodyColor = new Color(255 - i * 14, 30, 85);
            }

            // Segment core
            g2.setColor(bodyColor);
            g2.fillOval((int) (jx - size / 2), (int) (jy - size / 2), (int) size, (int) size);

            // Cyber neon glowing outline
            g2.setColor(primaryAccent);
            g2.drawOval((int) (jx - size / 2), (int) (jy - size / 2), (int) size, (int) size);

            // Inner LED highlight
            g2.setColor(new Color(255, 255, 255, 180));
            g2.fillOval((int) (jx - 1.5f), (int) (jy - 1.5f), 3, 3);
        }

        // 3. Draw Snake Head
        float headX = jointX[0];
        float headY = jointY[0];
        BufferedImage headImg = (isValid || phase != PHASE_CRASH_TRAP) ? snakeHeadSprite : snakeHeadFault;

        g2.drawImage(headImg, (int) (headX - 11), (int) (headY - 11 + headBob), null);

        // Animated Tongue Flick
        if (tongueOut && phase != PHASE_CRASH_TRAP) {
            g2.setColor(new Color(255, 60, 60));
            g2.fillRect((int) headX + 11, (int) headY - 1, 6, 2);
            g2.fillRect((int) headX + 17, (int) headY - 2, 2, 1);
            g2.fillRect((int) headX + 17, (int) headY + 1, 2, 1);
        }
    }

    private void paintParticles(Graphics2D g2) {
        for (ArenaParticle p : particles) {
            g2.setColor(p.color);
            g2.fillRect((int) p.x, (int) p.y, 3, 3);
        }

        // Draw sleek floating notification pill
        for (ArenaFloatingText t : floatingTexts) {
            g2.setFont(new Font("Consolas", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int strW = fm.stringWidth(t.text);
            int pillX = (int) (t.x - strW / 2);
            int pillY = (int) t.y - 12;
            pillX = Math.max(10, Math.min(getWidth() - strW - 20, pillX));

            // Alpha fade as life decreases
            float alpha = Math.min(1.0f, t.life / 20.0f);
            int aVal = (int) (alpha * 225);

            // Dark pill plate
            g2.setColor(new Color(12, 8, 24, aVal));
            g2.fillRoundRect(pillX - 6, pillY - 4, strW + 12, 20, 6, 6);

            // Neon glowing pill border
            Color c = t.color;
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha * 240)));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(pillX - 6, pillY - 4, strW + 12, 20, 6, 6);
            g2.setStroke(new BasicStroke(1f));

            // Text
            g2.setColor(new Color(255, 255, 255, (int) (alpha * 255)));
            g2.drawString(t.text, pillX, pillY + 11);
        }
    }

    private void paintBottomBanner(Graphics2D g2, int w, int h) {
        int bannerY = h - 28;
        g2.setColor(new Color(18, 14, 32, 230));
        g2.fillRect(0, bannerY, w, 28);
        g2.setColor(new Color(65, 48, 100));
        g2.drawLine(0, bannerY, w, bannerY);

        g2.setFont(new Font("Consolas", Font.BOLD, 10));
        g2.setColor(bannerColor);
        FontMetrics fm = g2.getFontMetrics();
        int maxW = w - 24;
        String bText = bannerStatus;
        if (fm.stringWidth(bText) > maxW) {
            while (fm.stringWidth(bText + "...") > maxW && bText.length() > 5) {
                bText = bText.substring(0, bText.length() - 1);
            }
            bText += "...";
        }
        g2.drawString(bText, 12, bannerY + 18);
    }

    // Helper classes
    private static class ArenaParticle {
        float x, y, vx, vy;
        Color color;
        int life;
        ArenaParticle(float x, float y, float vx, float vy, Color color, int life) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.color = color; this.life = life;
        }
    }

    private static class ArenaFloatingText {
        float x, y;
        String text;
        Color color;
        int life;
        ArenaFloatingText(float x, float y, String text, Color color, int life) {
            this.x = x; this.y = y; this.text = text; this.color = color; this.life = life;
        }
    }
}
