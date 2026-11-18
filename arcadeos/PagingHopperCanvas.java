package arcadeos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Live Animated MMU Bus & Frame Hopper Arcade Arena for Mission 03 Paging.
 * Responsive Layout (100% width, ~160px height).
 */
public class PagingHopperCanvas extends JPanel {

    public static final int OUTCOME_IDLE = 0;
    public static final int OUTCOME_HIT = 1;
    public static final int OUTCOME_PAGE_FAULT = 2;
    public static final int OUTCOME_LIMIT_TRAP = 3;

    private static final int PHASE_IDLE = 0;
    private static final int PHASE_HOPPING_TO_GATE = 1;
    private static final int PHASE_SCANNING = 2;
    private static final int PHASE_TRANSIT_RAM = 3;
    private static final int PHASE_DISK_SWAP = 4;
    private static final int PHASE_TRAP_BOUNCE = 5;
    private static final int PHASE_LANDED = 6;

    private final PagingPanel parent;
    private javax.swing.Timer animTimer;

    // Simulation & Translation State
    private long logicalAddr = 2500;
    private int pageNumber = 2;
    private int offset = 452;
    private int targetFrame = 1;
    private long physicalAddr = 1476;
    private int currentOutcome = OUTCOME_HIT;

    // Animation Phase State Machine
    private int phase = PHASE_IDLE;
    private float progress = 0.0f;
    private int phaseTicks = 0;
    private int tickCount = 0;

    // Frog Entity Coordinates
    private float frogX = 42;
    private float frogY = 85;
    private float frogHopArc = 0;

    // Visual Sprites
    private BufferedImage frogSpriteNormal;
    private BufferedImage frogSpriteFault;
    private BufferedImage frogSpriteTrap;
    private BufferedImage chipHazard;
    private BufferedImage glitchHazard;

    // Floating text & particles
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();

    // Banner message
    private String bannerStatus = "STANDBY: Ready to dispatch MMU Hopper across memory buses.";
    private Color bannerColor = RetroTheme.ACCENT_CYAN;

    // Shockwave ring for RAM landing
    private float shockwaveRadius = 0f;
    private float shockwaveAlpha = 0f;

    // Disk Crane Animation (on Page Fault)
    private float craneY = -40f;
    private float diskProgress = 0f;

    public PagingHopperCanvas(PagingPanel parent) {
        this.parent = parent;
        setBackground(new Color(11, 9, 24));
        setOpaque(true);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(65, 48, 100), 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));

        // Create pixel sprites
        frogSpriteNormal = PixelSprites.createFrogSprite(24, RetroTheme.ACCENT_GREEN);
        frogSpriteFault = PixelSprites.createFrogSprite(24, RetroTheme.ACCENT_PINK);
        frogSpriteTrap = PixelSprites.createFrogSprite(24, RetroTheme.ACCENT_YELLOW);
        chipHazard = PixelSprites.createChipHazardSprite(18, RetroTheme.ACCENT_PINK);
        glitchHazard = PixelSprites.createGlitchHazardSprite(22, 12, RetroTheme.ACCENT_YELLOW);

        // Click interaction
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (phase == PHASE_IDLE || phase == PHASE_LANDED) {
                    addFloatingText(frogX, frogY - 12, "HOP! 🐸", RetroTheme.ACCENT_GREEN);
                    addBurstParticles(frogX + 12, frogY + 16, RetroTheme.ACCENT_GREEN, 8);
                    frogHopArc = 14;
                    repaint();
                }
            }
        });

        // 30ms Animation Loop (~33 FPS)
        animTimer = new javax.swing.Timer(30, e -> updateTick());
        animTimer.start();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(480, 140);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(280, 120);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 150);
    }

    public void stopAnimation() {
        if (animTimer != null && animTimer.isRunning()) animTimer.stop();
    }

    public void resumeAnimation() {
        if (animTimer != null && !animTimer.isRunning()) animTimer.start();
    }

    public void triggerTranslation(long la, int p, int d, int f, long pa, int outcome) {
        this.logicalAddr = la;
        this.pageNumber = p;
        this.offset = d;
        this.targetFrame = f;
        this.physicalAddr = pa;
        this.currentOutcome = outcome;

        this.phase = PHASE_HOPPING_TO_GATE;
        this.progress = 0.0f;
        this.phaseTicks = 0;
        this.shockwaveRadius = 0f;
        this.shockwaveAlpha = 0f;
        this.craneY = -50f;
        this.diskProgress = 0f;

        if (outcome == OUTCOME_HIT) {
            bannerStatus = String.format("DISPATCH: Routing LA %d (Page %d) -> Frame F%02d", la, p, f);
            bannerColor = RetroTheme.ACCENT_CYAN;
        } else if (outcome == OUTCOME_PAGE_FAULT) {
            bannerStatus = String.format("DISPATCH: Routing LA %d (Page %d) -> [PAGE FAULT]", la, p);
            bannerColor = RetroTheme.ACCENT_PINK;
        } else {
            bannerStatus = String.format("DISPATCH: Routing LA %d (Page %d) -> [LIMIT TRAP]", la, p);
            bannerColor = RetroTheme.ACCENT_YELLOW;
        }

        addBurstParticles(50, 85, RetroTheme.ACCENT_CYAN, 8);
        repaint();
    }

    private void updateTick() {
        if (!isShowing()) return;

        tickCount++;
        phaseTicks++;

        int w = Math.max(300, getWidth());
        int h = Math.max(140, getHeight());

        float startX = 45f;
        float gateX = w * 0.48f;
        float endX = w - 55f;
        float baseY = 80f;

        switch (phase) {
            case PHASE_IDLE:
                frogX = startX;
                frogY = baseY + (float) Math.sin(tickCount * 0.12) * 2.0f;
                frogHopArc = 0;
                break;

            case PHASE_HOPPING_TO_GATE:
                progress += 0.055f;
                if (progress > 1.0f) progress = 1.0f;

                frogX = startX + (gateX - startX) * progress;
                float hopPhase = (progress * 2) % 1.0f;
                frogHopArc = (float) Math.sin(hopPhase * Math.PI) * 18f;
                frogY = baseY - frogHopArc;

                if (progress >= 1.0f) {
                    phase = PHASE_SCANNING;
                    progress = 0f;
                    phaseTicks = 0;
                    bannerStatus = String.format("MMU SCAN: Scanning Page Table for Page #%d...", pageNumber);
                    bannerColor = RetroTheme.ACCENT_YELLOW;
                    addBurstParticles(gateX, baseY + 8, RetroTheme.ACCENT_YELLOW, 8);
                }
                break;

            case PHASE_SCANNING:
                frogX = gateX - 12;
                frogY = baseY;
                frogHopArc = 0;
                progress += 0.06f;

                if (progress >= 1.0f) {
                    progress = 0f;
                    phaseTicks = 0;
                    if (currentOutcome == OUTCOME_HIT) {
                        phase = PHASE_TRANSIT_RAM;
                        bannerStatus = String.format("MMU HIT! Page %d -> Frame F%02d [PA = %d B]", pageNumber, targetFrame, physicalAddr);
                        bannerColor = RetroTheme.ACCENT_GREEN;
                        addBurstParticles(gateX, baseY, RetroTheme.ACCENT_GREEN, 12);
                    } else if (currentOutcome == OUTCOME_PAGE_FAULT) {
                        phase = PHASE_DISK_SWAP;
                        bannerStatus = String.format("PAGE FAULT! Page %d Valid=0 -> Trap to OS Disk Swap!", pageNumber);
                        bannerColor = RetroTheme.ACCENT_PINK;
                        addBurstParticles(gateX, baseY, RetroTheme.ACCENT_PINK, 14);
                        addFloatingText(gateX - 25, baseY - 16, "PAGE FAULT!", RetroTheme.ACCENT_PINK);
                    } else {
                        phase = PHASE_TRAP_BOUNCE;
                        bannerStatus = String.format("LIMIT TRAP! Page %d > Page Table Limit!", pageNumber);
                        bannerColor = RetroTheme.ACCENT_YELLOW;
                        addBurstParticles(gateX, baseY, RetroTheme.ACCENT_YELLOW, 14);
                        addFloatingText(gateX - 25, baseY - 16, "TRAP! > LIMIT", RetroTheme.ACCENT_YELLOW);
                    }
                }
                break;

            case PHASE_TRANSIT_RAM:
                progress += 0.055f;
                if (progress > 1.0f) progress = 1.0f;

                frogX = (gateX - 12) + (endX - (gateX - 12)) * progress;
                float ramHopPhase = (progress * 2) % 1.0f;
                frogHopArc = (float) Math.sin(ramHopPhase * Math.PI) * 18f;
                frogY = baseY - frogHopArc;

                if (tickCount % 2 == 0) {
                    particles.add(new Particle(frogX, frogY + 10, (float)(Math.random() - 0.5) * 2, (float)Math.random() * 2, RetroTheme.ACCENT_GREEN, 10));
                }

                if (progress >= 1.0f) {
                    phase = PHASE_LANDED;
                    progress = 0f;
                    phaseTicks = 0;
                    shockwaveRadius = 2f;
                    shockwaveAlpha = 1.0f;
                    bannerStatus = String.format("TRANSLATED: Physical Address = %d Bytes (Frame F%02d + Offset %d B)", physicalAddr, targetFrame, offset);
                    bannerColor = RetroTheme.ACCENT_GREEN;

                    addBurstParticles(endX, baseY + 8, RetroTheme.ACCENT_GREEN, 16);
                    addFloatingText(endX - 30, baseY - 18, "+100 XP [HIT]", RetroTheme.ACCENT_GREEN);
                }
                break;

            case PHASE_DISK_SWAP:
                progress += 0.035f;
                if (craneY < 32f) craneY += 3.5f;
                diskProgress = Math.min(1.0f, progress * 1.2f);
                frogX = gateX - 12;
                frogY = baseY + 10f;

                if (tickCount % 4 == 0) {
                    particles.add(new Particle(gateX, craneY + 20, (float)(Math.random() - 0.5) * 3, 2f, RetroTheme.ACCENT_PINK, 12));
                }

                if (progress >= 1.0f) {
                    phase = PHASE_LANDED;
                    bannerStatus = String.format("PAGE FAULT RESOLVED: Frame loaded from disk backing store.");
                    bannerColor = RetroTheme.ACCENT_PINK;
                    addBurstParticles(gateX, baseY + 8, RetroTheme.ACCENT_PINK, 12);
                }
                break;

            case PHASE_TRAP_BOUNCE:
                progress += 0.05f;
                frogX = (gateX - 12) - (float) Math.sin(progress * Math.PI) * 28f;
                frogY = baseY + (float) Math.cos(progress * Math.PI * 2) * 6f;

                if (tickCount % 3 == 0) {
                    particles.add(new Particle(gateX - 12, baseY + (float)(Math.random() * 16 - 8), -2f, (float)(Math.random() - 0.5) * 2, RetroTheme.ACCENT_YELLOW, 10));
                }

                if (progress >= 1.0f) {
                    phase = PHASE_LANDED;
                    bannerStatus = "TRAP COMPLETED: CPU trapped to OS boundary exception.";
                    bannerColor = RetroTheme.ACCENT_YELLOW;
                }
                break;

            case PHASE_LANDED:
                if (currentOutcome == OUTCOME_HIT) {
                    frogX = endX;
                    frogY = baseY + (float) Math.sin(tickCount * 0.15) * 1.5f;
                } else {
                    frogX = gateX - 12;
                    frogY = baseY;
                }
                frogHopArc = 0;

                if (shockwaveAlpha > 0f) {
                    shockwaveRadius += 2.0f;
                    shockwaveAlpha -= 0.045f;
                    if (shockwaveAlpha < 0) shockwaveAlpha = 0;
                }
                break;
        }

        Iterator<Particle> pIt = particles.iterator();
        while (pIt.hasNext()) {
            Particle pt = pIt.next();
            pt.x += pt.vx;
            pt.y += pt.vy;
            pt.life--;
            if (pt.life <= 0) pIt.remove();
        }

        Iterator<FloatingText> tIt = floatingTexts.iterator();
        while (tIt.hasNext()) {
            FloatingText ft = tIt.next();
            ft.y -= 0.7f;
            ft.life--;
            if (ft.life <= 0) tIt.remove();
        }

        repaint();
    }

    private void addBurstParticles(float cx, float cy, Color c, int count) {
        for (int i = 0; i < count; i++) {
            double ang = Math.random() * Math.PI * 2;
            float spd = (float)(1.0 + Math.random() * 3.0);
            particles.add(new Particle(cx, cy, (float) Math.cos(ang) * spd, (float) Math.sin(ang) * spd, c, 16 + (int)(Math.random() * 8)));
        }
    }

    private void addFloatingText(float x, float y, String text, Color c) {
        floatingTexts.add(new FloatingText(x, y, text, c, 34));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Grid & Circuit Background
        paintCyberCircuitBg(g2, w, h);

        // 2. Highway Bus Lanes
        paintBusLanes(g2, w, h);

        // 3. Station Pods: Logical Register Station (Left) & Physical RAM Frame (Right)
        paintRegisterStations(g2, w, h);

        // 4. MMU Page Table Gate & Scanner (Center)
        paintMmuGate(g2, w, h);

        // 5. Disk Crane on Page Fault
        if (phase == PHASE_DISK_SWAP) {
            paintDiskCrane(g2, w, h);
        }

        // 6. Shockwave on RAM Landing
        if (shockwaveAlpha > 0f) {
            g2.setColor(new Color(0, 240, 160, (int)(shockwaveAlpha * 220)));
            g2.setStroke(new BasicStroke(1.8f));
            int r = (int) shockwaveRadius;
            g2.drawOval((int)(w - 55f + 12 - r), (int)(80f + 12 - r), r * 2, r * 2);
        }

        // 7. Render Animated Cyber Frog
        paintCyberFrog(g2);

        // 8. Particles & Floating Text
        for (Particle pt : particles) {
            float alpha = (float) pt.life / pt.maxLife;
            g2.setColor(new Color(pt.c.getRed(), pt.c.getGreen(), pt.c.getBlue(), Math.max(0, Math.min(255, (int)(alpha * 255)))));
            g2.fillRect((int) pt.x, (int) pt.y, 2, 2);
        }

        for (FloatingText ft : floatingTexts) {
            float alpha = (float) ft.life / ft.maxLife;
            g2.setColor(new Color(ft.c.getRed(), ft.c.getGreen(), ft.c.getBlue(), Math.max(0, Math.min(255, (int)(alpha * 255)))));
            g2.setFont(new Font("Consolas", Font.BOLD, 10));
            g2.drawString(ft.text, (int) ft.x, (int) ft.y);
        }

        // 9. Top Ribbon Status Bar
        paintTopRibbon(g2, w);

        g2.dispose();
    }

    private void paintCyberCircuitBg(Graphics2D g2, int w, int h) {
        g2.setColor(new Color(25, 20, 48, 65));
        for (int x = 0; x < w; x += 20) g2.drawLine(x, 22, x, h - 2);
        for (int y = 22; y < h; y += 20) g2.drawLine(0, y, w, y);

        // Circuit Traces
        g2.setColor(new Color(0, 220, 255, 30));
        g2.drawLine(10, 38, w - 10, 38);
        g2.drawLine(10, h - 16, w - 10, h - 16);

        // Moving data pulses
        int pulseX1 = (tickCount * 3) % w;
        int pulseX2 = (w - (tickCount * 2) % w);
        g2.setColor(new Color(0, 240, 255, 140));
        g2.fillRect(pulseX1, 37, 10, 2);
        g2.setColor(new Color(57, 255, 140, 140));
        g2.fillRect(pulseX2, h - 17, 10, 2);
    }

    private void paintBusLanes(Graphics2D g2, int w, int h) {
        int laneLeft = 95;
        int laneRight = w - 95;
        int laneW = Math.max(20, laneRight - laneLeft);

        // Lane 1: Upper TLB Fast-Path Bus
        g2.setColor(new Color(18, 30, 56, 120));
        g2.fillRect(laneLeft, 48, laneW, 20);
        g2.setColor(new Color(0, 220, 255, 60));
        for (int x = laneLeft + 4; x < laneRight - 4; x += 18) {
            g2.fillRect(x, 57, 8, 2);
        }

        // Lane 2: Main MMU Data Bus
        g2.setColor(new Color(16, 26, 44, 150));
        g2.fillRect(laneLeft - 8, 76, laneW + 16, 28);
        g2.setColor(new Color(40, 240, 160, 50));
        for (int x = laneLeft - 4; x < laneRight + 4; x += 20) {
            g2.fillRect(x, 89, 10, 2);
        }

        // Lane 3: Lower I/O Disk Swap Runway
        g2.setColor(new Color(36, 18, 36, 120));
        g2.fillRect(laneLeft, 114, laneW, 20);
        g2.setColor(new Color(255, 80, 140, 50));
        for (int x = laneLeft + 4; x < laneRight - 4; x += 18) {
            g2.fillRect(x, 123, 8, 2);
        }

        // Ambient moving sprites
        if (laneW > 60) {
            int chipX = laneLeft + ((tickCount * 2 + 40) % (laneW - 20));
            g2.drawImage(chipHazard, chipX, 115, null);

            int glitchX = laneRight - 20 - ((tickCount * 3 + 60) % (laneW - 20));
            g2.drawImage(glitchHazard, glitchX, 52, null);
        }
    }

    private void paintRegisterStations(Graphics2D g2, int w, int h) {
        // Station Left: CPU Logical Register
        int regW = 84;
        int regH = 58;
        int regX = 6;
        int regY = 56;

        g2.setColor(new Color(18, 14, 38));
        g2.fillRoundRect(regX, regY, regW, regH, 6, 6);
        g2.setColor(new Color(0, 220, 255));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(regX, regY, regW, regH, 6, 6);

        g2.setColor(new Color(0, 220, 255, 40));
        g2.fillRect(regX + 1, regY + 1, regW - 2, 14);
        g2.setColor(new Color(0, 240, 255));
        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        g2.drawString("LOGICAL REG", regX + 5, regY + 11);

        g2.setFont(new Font("Consolas", Font.BOLD, 9));
        g2.setColor(Color.WHITE);
        g2.drawString("LA: " + logicalAddr + "B", regX + 5, regY + 25);

        g2.setColor(new Color(0, 240, 200));
        g2.setFont(new Font("Consolas", Font.PLAIN, 8));
        g2.drawString(String.format("P: %d", pageNumber), regX + 5, regY + 39);
        g2.setColor(RetroTheme.ACCENT_YELLOW);
        g2.drawString(String.format("D: %d", offset), regX + 5, regY + 51);

        // Station Right: Target RAM Frame
        int ramW = 88;
        int ramH = 58;
        int ramX = w - ramW - 6;
        int ramY = 56;

        boolean isHit = (currentOutcome == OUTCOME_HIT);
        Color borderC = isHit ? RetroTheme.ACCENT_GREEN : new Color(60, 48, 85);
        g2.setColor(new Color(16, 26, 28));
        g2.fillRoundRect(ramX, ramY, ramW, ramH, 6, 6);
        g2.setColor(borderC);
        g2.setStroke(new BasicStroke(isHit ? 1.6f : 1.0f));
        g2.drawRoundRect(ramX, ramY, ramW, ramH, 6, 6);

        g2.setColor(new Color(borderC.getRed(), borderC.getGreen(), borderC.getBlue(), 40));
        g2.fillRect(ramX + 1, ramY + 1, ramW - 2, 14);
        g2.setColor(borderC);
        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        g2.drawString("TARGET RAM", ramX + 5, ramY + 11);

        g2.setFont(new Font("Consolas", Font.BOLD, 9));
        if (targetFrame >= 0) {
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString(String.format("FRAME: F%02d", targetFrame), ramX + 5, ramY + 25);
            g2.setColor(new Color(180, 240, 200));
            g2.setFont(new Font("Consolas", Font.PLAIN, 8));
            g2.drawString(String.format("BASE: %dB", (long) targetFrame * parent.getPageSize()), ramX + 5, ramY + 39);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Consolas", Font.BOLD, 8));
            g2.drawString(String.format("PA: %d B", physicalAddr), ramX + 5, ramY + 51);
        } else {
            g2.setColor(RetroTheme.TEXT_DIM);
            g2.drawString("FRAME: --", ramX + 5, ramY + 25);
            g2.setColor(new Color(255, 90, 130));
            g2.setFont(new Font("Consolas", Font.PLAIN, 8));
            g2.drawString(currentOutcome == OUTCOME_PAGE_FAULT ? "[PAGE FAULT]" : "[LIMIT TRAP]", ramX + 5, ramY + 43);
        }
    }

    private void paintMmuGate(Graphics2D g2, int w, int h) {
        float gateX = w * 0.48f;
        int gateW = 54;
        int gateH = 68;
        int gx = (int)(gateX - gateW / 2);
        int gy = 52;

        g2.setColor(new Color(22, 16, 44));
        g2.fillRoundRect(gx, gy, gateW, gateH, 8, 8);
        g2.setColor(new Color(130, 80, 220));
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawRoundRect(gx, gy, gateW, gateH, 8, 8);

        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        g2.setColor(new Color(200, 170, 255));
        g2.drawString("MMU GATE", gx + 8, gy + 11);

        float scanY = gy + 14 + (float)(Math.sin(tickCount * 0.18) * 0.5 + 0.5) * (gateH - 20);
        g2.setColor(new Color(0, 240, 255, 180));
        g2.drawLine(gx + 3, (int) scanY, gx + gateW - 3, (int) scanY);
        g2.setColor(new Color(0, 240, 255, 35));
        g2.fillRect(gx + 3, (int) scanY - 2, gateW - 6, 4);

        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        if (phase == PHASE_SCANNING) {
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("SCANNING", gx + 6, gy + 36);
            g2.drawString(String.format("P%d -> ?", pageNumber), gx + 10, gy + 48);
        } else if (currentOutcome == OUTCOME_HIT && phase >= PHASE_TRANSIT_RAM) {
            g2.setColor(RetroTheme.ACCENT_GREEN);
            g2.drawString("VALID [V]", gx + 6, gy + 36);
            g2.drawString(String.format("P%d->F%02d", pageNumber, targetFrame), gx + 7, gy + 48);
        } else if (currentOutcome == OUTCOME_PAGE_FAULT && phase >= PHASE_DISK_SWAP) {
            g2.setColor(RetroTheme.ACCENT_PINK);
            g2.drawString("FAULT [X]", gx + 6, gy + 36);
            g2.drawString("DISK I/O", gx + 8, gy + 48);
        } else if (currentOutcome == OUTCOME_LIMIT_TRAP && phase >= PHASE_TRAP_BOUNCE) {
            g2.setColor(RetroTheme.ACCENT_YELLOW);
            g2.drawString("TRAP [!]", gx + 8, gy + 36);
            g2.drawString("LIMIT", gx + 14, gy + 48);
        } else {
            g2.setColor(RetroTheme.TEXT_DIM);
            g2.drawString("READY", gx + 14, gy + 36);
            g2.drawString("PAGE MAP", gx + 6, gy + 48);
        }
    }

    private void paintDiskCrane(Graphics2D g2, int w, int h) {
        float gateX = w * 0.48f;
        int cx = (int) gateX;
        int cy = (int) craneY;

        g2.setColor(new Color(255, 100, 160));
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawLine(cx, 0, cx, cy);

        g2.setColor(new Color(60, 20, 45));
        g2.fillRoundRect(cx - 16, cy, 32, 14, 4, 4);
        g2.setColor(RetroTheme.ACCENT_PINK);
        g2.drawRoundRect(cx - 16, cy, 32, 14, 4, 4);

        g2.setColor(new Color(255, 60, 140, 50));
        Polygon p = new Polygon();
        p.addPoint(cx - 12, cy + 14);
        p.addPoint(cx + 12, cy + 14);
        p.addPoint(cx + 20, (int) frogY + 16);
        p.addPoint(cx - 20, (int) frogY + 16);
        g2.fillPolygon(p);

        g2.setColor(new Color(20, 10, 30));
        g2.fillRect(cx - 20, cy + 18, 40, 10);
        g2.setColor(RetroTheme.ACCENT_PINK);
        g2.drawRect(cx - 20, cy + 18, 40, 10);
        g2.fillRect(cx - 18, cy + 20, (int)(36 * diskProgress), 6);
    }

    private void paintCyberFrog(Graphics2D g2) {
        BufferedImage img;
        if (currentOutcome == OUTCOME_PAGE_FAULT && phase >= PHASE_DISK_SWAP) {
            img = frogSpriteFault;
        } else if (currentOutcome == OUTCOME_LIMIT_TRAP && phase >= PHASE_TRAP_BOUNCE) {
            img = frogSpriteTrap;
        } else {
            img = frogSpriteNormal;
        }

        int fx = (int) frogX;
        int fy = (int) frogY;

        int shadowW = Math.max(6, 20 - (int)(frogHopArc * 0.5f));
        int shadowAlpha = Math.max(20, 120 - (int)(frogHopArc * 4.0f));
        g2.setColor(new Color(0, 0, 0, shadowAlpha));
        g2.fillOval(fx + 2, 80 + 20, shadowW, 5);

        g2.drawImage(img, fx, fy, null);

        // Address packet tag
        g2.setColor(new Color(15, 10, 30));
        g2.fillRoundRect(fx - 2, fy - 10, 30, 10, 3, 3);
        Color tagC = (currentOutcome == OUTCOME_HIT) ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_YELLOW;
        g2.setColor(tagC);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(fx - 2, fy - 10, 30, 10, 3, 3);

        g2.setFont(new Font("Consolas", Font.BOLD, 7));
        g2.setColor(Color.WHITE);
        if (phase >= PHASE_TRANSIT_RAM && currentOutcome == OUTCOME_HIT) {
            g2.drawString(String.format("F%02d|%d", targetFrame, offset), fx + 1, fy - 2);
        } else {
            g2.drawString(String.format("P%d|%d", pageNumber, offset), fx + 1, fy - 2);
        }
    }

    private void paintTopRibbon(Graphics2D g2, int w) {
        g2.setColor(new Color(18, 12, 36));
        g2.fillRect(0, 0, w, 20);
        g2.setColor(new Color(60, 44, 95));
        g2.drawLine(0, 20, w, 20);

        g2.setFont(new Font("Consolas", Font.BOLD, 9));
        g2.setColor(bannerColor);
        g2.drawString("👾 " + bannerStatus, 6, 14);

        int rx = w - 110;
        g2.setFont(new Font("Consolas", Font.BOLD, 8));
        g2.setColor(RetroTheme.ACCENT_CYAN);
        g2.drawString("MMU: ONLINE", rx, 14);

        int alpha = (int)(Math.sin(tickCount * 0.15) * 50 + 205);
        g2.setColor(new Color(57, 255, 140, Math.min(255, alpha)));
        g2.fillOval(w - 14, 6, 6, 6);
    }

    private static class Particle {
        float x, y, vx, vy;
        Color c;
        int life, maxLife;
        Particle(float x, float y, float vx, float vy, Color c, int life) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.c = c; this.life = life; this.maxLife = life;
        }
    }

    private static class FloatingText {
        float x, y;
        String text;
        Color c;
        int life, maxLife;
        FloatingText(float x, float y, String text, Color c, int life) {
            this.x = x; this.y = y; this.text = text; this.c = c; this.life = life; this.maxLife = life;
        }
    }
}
