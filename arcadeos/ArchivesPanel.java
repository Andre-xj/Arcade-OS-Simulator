package arcadeos;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * Screen 3: OPERATING SYSTEM ARCHIVES & STAGE SELECT
 * Features a clean, high-contrast, uncluttered AAA Game Stage Selection Carousel:
 * - Removed redundant text and broken unicode glyphs ([?])
 * - Replaced broken star glyphs with custom vector difficulty rating bars
 * - High-contrast readable typography (Consolas bold, bright colors)
 * - True continuous rail coordinate system (no gaps, no seams)
 * - Vector chevron buttons (< and >)
 */
public class ArchivesPanel extends JPanel {
    private ArcadeOS parentFrame;
    private JLabel lblFilesReadCounter;
    private JProgressBar indexProgress;

    private int currentStageIndex = 0;
    private boolean isCarouselMode = true;

    // Smooth continuous rail animation variables
    private float currentScroll = 0.0f;
    private float targetScroll = 0.0f;
    private Timer railTimer;
    private Timer idleAnimTimer;

    private int pulseTicks = 0;
    private float bobOffset = 0.0f;
    private float glowPulse = 0.7f;

    private JPanel centerContentHolder;
    private JPanel carouselView;
    private JPanel gridView;
    private CarouselCanvas carouselCanvas;
    private JPanel stageIndicatorsPanel;

    public static class ArchiveCardData {
        public String num;
        public String title;
        public String subtitle;
        public String desc;
        public String[] tags;
        public int complexity;
        public int tabIndex;

        public ArchiveCardData(String num, String title, String subtitle, String desc, String[] tags, int comp, int tab) {
            this.num = num; this.title = title; this.subtitle = subtitle; this.desc = desc; this.tags = tags;
            this.complexity = comp; this.tabIndex = tab;
        }
    }

    private final ArchiveCardData[] cards = {
        new ArchiveCardData("01", "CPU SCHEDULING", "MISSION 01: DISPATCH RUNWAY",
                "Process dispatch and execution-order analysis engine (FCFS, SJF, SRTF, Round Robin).",
                new String[]{"FCFS", "SJF", "SRT", "ROUND ROBIN"}, 3, 0),
        new ArchiveCardData("02", "BANKER'S ALGORITHM", "MISSION 02: DEADLOCK AVOIDANCE",
                "Dijkstra's safety-state verification protocol and multi-resource allocation matrices.",
                new String[]{"SAFETY", "NEED MATRIX", "SAFE SEQUENCE"}, 4, 1),
        new ArchiveCardData("03", "PAGING", "MISSION 03: VIRTUAL MEMORY",
                "Non-contiguous logical-to-physical address translation engine using page tables.",
                new String[]{"PAGE MAP", "ADDRESS TRANSLATION", "OFFSET"}, 2, 2),
        new ArchiveCardData("04", "PAGE REPLACEMENT", "MISSION 04: CACHE EVICTION",
                "Demand paging frame eviction policy and cache-fault minimization protocol.",
                new String[]{"FIFO", "LRU", "OPTIMAL", "FAULT TRACE"}, 3, 3),
        new ArchiveCardData("05", "SEGMENTATION", "MISSION 05: VARIABLE BOUNDS",
                "Variable-length segment protection checking and hardware limit trap detection.",
                new String[]{"BOUND CHECK", "LIMIT TRAP", "BASE REG"}, 2, 4),
        new ArchiveCardData("06", "FRAGMENTATION", "MISSION 06: RAM PARTITIONING",
                "Contiguous storage allocation strategies (First-Fit, Best-Fit, Worst-Fit) and compaction.",
                new String[]{"FIRST FIT", "BEST FIT", "WORST FIT", "COMPACTION"}, 3, 5),
        new ArchiveCardData("07", "DISK SCHEDULING", "MISSION 07: I/O TRAVERSAL",
                "Magnetic cylinder track traversal and seek-time minimization algorithms.",
                new String[]{"FCFS", "SSTF", "SCAN", "LOOK", "SEEK MIN"}, 3, 6),
        new ArchiveCardData("08", "MULTIPROG", "MISSION 08: DEGREE OF CONCURRENCY",
                "Concurrent workload sharing, probabilistic CPU utilization, and thrashing analysis.",
                new String[]{"CPU UTIL%", "RAM%", "ADMISSION", "THRASHING"}, 4, 7),
    };

    public ArchivesPanel(ArcadeOS parent) {
        this.parentFrame = parent;
        setBackground(RetroTheme.BG_DARK);
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        // 1. Clean, uncluttered Top Header
        add(createHeader(), BorderLayout.NORTH);

        // 2. Center Content Holder (Carousel / Grid)
        centerContentHolder = new JPanel(new BorderLayout());
        centerContentHolder.setOpaque(false);

        carouselView = createCarouselView();
        gridView     = createGridView();

        centerContentHolder.add(isCarouselMode ? carouselView : gridView, BorderLayout.CENTER);
        add(centerContentHolder, BorderLayout.CENTER);

        // 3. Clean Bottom Navigation Bar
        add(createBottomNav(), BorderLayout.SOUTH);

        // Idle sprite float & neon pulse timer (60 FPS)
        idleAnimTimer = new Timer(16, e -> {
            pulseTicks++;
            bobOffset = (float)(Math.sin(pulseTicks * 0.055) * 6.0);
            glowPulse = (float)(0.60 + 0.38 * Math.sin(pulseTicks * 0.045));
            if (isCarouselMode && carouselCanvas != null) {
                carouselCanvas.repaint();
            }
        });
        idleAnimTimer.start();

        // Keyboard navigation for Carousel
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isCarouselMode) {
                    if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_A) {
                        prevStage();
                    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_D) {
                        nextStage();
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        parentFrame.openSimulatorTab(cards[currentStageIndex].tabIndex);
                    } else if (e.getKeyCode() == KeyEvent.VK_M) {
                        openManual(cards[currentStageIndex].tabIndex, currentStageIndex);
                    }
                }
            }
        });
    }

    /** Clean, focused header with no verbose text clutter */
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setBackground(RetroTheme.PANEL_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));

        // Left: Mascot + Title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(new JLabel(new ImageIcon(PixelSprites.createRobotSprite(32))));

        JPanel leftText = new JPanel();
        leftText.setOpaque(false);
        leftText.setLayout(new BoxLayout(leftText, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        JLabel lm = new JLabel("MINI ");
        lm.setFont(new Font("Consolas", Font.BOLD, 15));
        lm.setForeground(Color.WHITE);
        JLabel ls = new JLabel("// ");
        ls.setFont(new Font("Consolas", Font.BOLD, 15));
        ls.setForeground(RetroTheme.ACCENT_PINK);
        JLabel lo = new JLabel("OS");
        lo.setFont(new Font("Consolas", Font.BOLD, 15));
        lo.setForeground(Color.WHITE);
        titleRow.add(lm); titleRow.add(ls); titleRow.add(lo);
        leftText.add(titleRow);

        JLabel tag = new JLabel("OPERATING SYSTEM SIMULATOR");
        tag.setFont(new Font("Consolas", Font.PLAIN, 9));
        tag.setForeground(RetroTheme.TEXT_DIM);
        leftText.add(tag);

        left.add(leftText);
        header.add(left, BorderLayout.WEST);

        // Center: Single clean title and concise hint
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel mainTitle = new JLabel("STAGE SELECT  //  SYSTEM ARCHIVES");
        mainTitle.setFont(new Font("Consolas", Font.BOLD, 15));
        mainTitle.setForeground(Color.WHITE);
        mainTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(mainTitle);

        JLabel desc = new JLabel("SELECT A MODULE TO VIEW ARCHIVE MANUAL OR LAUNCH SIMULATOR");
        desc.setFont(new Font("Consolas", Font.PLAIN, 10));
        desc.setForeground(RetroTheme.ACCENT_CYAN);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(desc);

        header.add(center, BorderLayout.CENTER);

        // Right: View Switcher + Clean Counter
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 4));
        right.setOpaque(false);

        JButton btnToggleView = RetroTheme.createRetroButton("CAROUSEL VIEW", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnToggleView.setFont(new Font("Consolas", Font.BOLD, 10));
        btnToggleView.addActionListener(e -> {
            isCarouselMode = !isCarouselMode;
            btnToggleView.setText(isCarouselMode ? "CAROUSEL VIEW" : "GRID VIEW");
            btnToggleView.setForeground(isCarouselMode ? RetroTheme.ACCENT_CYAN : RetroTheme.ACCENT_YELLOW);
            centerContentHolder.removeAll();
            centerContentHolder.add(isCarouselMode ? carouselView : gridView, BorderLayout.CENTER);
            centerContentHolder.revalidate();
            centerContentHolder.repaint();
            requestFocusInWindow();
        });
        right.add(btnToggleView);

        JPanel readBox = new JPanel();
        readBox.setOpaque(false);
        readBox.setLayout(new BoxLayout(readBox, BoxLayout.Y_AXIS));

        JLabel lblRead = new JLabel("FILES READ");
        lblRead.setFont(new Font("Consolas", Font.BOLD, 9));
        lblRead.setForeground(RetroTheme.TEXT_DIM);
        readBox.add(lblRead);

        lblFilesReadCounter = new JLabel(String.format("%02d / 08", AppState.countFilesRead()));
        lblFilesReadCounter.setFont(new Font("Consolas", Font.BOLD, 16));
        lblFilesReadCounter.setForeground(RetroTheme.ACCENT_YELLOW);
        readBox.add(lblFilesReadCounter);

        right.add(readBox);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TRUE MAP/LEVEL SELECT CONTINUOUS CAROUSEL
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel createCarouselView() {
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);
        container.setBorder(BorderFactory.createEmptyBorder(6, 4, 4, 4));

        JPanel sliderTrack = new JPanel(new BorderLayout(8, 0));
        sliderTrack.setOpaque(false);

        // Vector Left Nav Button (<)
        VectorArrowButton btnPrev = new VectorArrowButton(true, "PREV STAGE [A]");
        btnPrev.addActionListener(e -> { prevStage(); requestFocusInWindow(); });
        sliderTrack.add(btnPrev, BorderLayout.WEST);

        // Continuous-rail stage canvas
        carouselCanvas = new CarouselCanvas();
        sliderTrack.add(carouselCanvas, BorderLayout.CENTER);

        // Vector Right Nav Button (>)
        VectorArrowButton btnNext = new VectorArrowButton(false, "NEXT STAGE [D]");
        btnNext.addActionListener(e -> { nextStage(); requestFocusInWindow(); });
        sliderTrack.add(btnNext, BorderLayout.EAST);

        container.add(sliderTrack, BorderLayout.CENTER);

        // Bottom Stage Selector Strip
        stageIndicatorsPanel = createStageIndicatorsStrip();
        container.add(stageIndicatorsPanel, BorderLayout.SOUTH);

        return container;
    }

    /**
     * Vector Arrow Button: Renders custom antialiased chevron arrows.
     * Guaranteed never to fail font character rendering or show [?] glyphs!
     */
    private class VectorArrowButton extends JButton {
        private boolean isLeft;

        public VectorArrowButton(boolean isLeft, String tooltip) {
            this.isLeft = isLeft;
            setPreferredSize(new Dimension(46, 0));
            setFocusPainted(false);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder());
            setToolTipText(tooltip);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            boolean hover = getModel().isRollover();

            // Background pill
            g2.setColor(hover ? new Color(55, 30, 95, 230) : new Color(22, 16, 42, 200));
            g2.fillRoundRect(2, 10, w - 4, h - 20, 14, 14);

            // Border
            g2.setColor(hover ? RetroTheme.ACCENT_CYAN : new Color(70, 50, 105));
            g2.setStroke(new BasicStroke(hover ? 1.8f : 1.2f));
            g2.drawRoundRect(2, 10, w - 4, h - 20, 14, 14);

            // Vector Chevron Arrow
            int cx = w / 2;
            int cy = h / 2;
            g2.setColor(hover ? RetroTheme.ACCENT_CYAN : Color.WHITE);
            g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            if (isLeft) {
                g2.drawLine(cx + 4, cy - 14, cx - 6, cy);
                g2.drawLine(cx - 6, cy, cx + 4, cy + 14);
            } else {
                g2.drawLine(cx - 4, cy - 14, cx + 6, cy);
                g2.drawLine(cx + 6, cy, cx - 4, cy + 14);
            }

            g2.dispose();
        }
    }

    /**
     * Continuous Stage Rail Canvas:
     * High-contrast readable typography, clean spacing, and zero broken characters.
     */
    private class CarouselCanvas extends JPanel {
        private Rectangle manualBtnBounds = new Rectangle();
        private Rectangle launchBtnBounds = new Rectangle();
        private Rectangle quizBtnBounds = new Rectangle();
        private Rectangle[] cardBounds = new Rectangle[cards.length];

        public CarouselCanvas() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            for (int i = 0; i < cards.length; i++) {
                cardBounds[i] = new Rectangle();
            }

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Point p = e.getPoint();

                    // Check hero buttons if near center
                    if (Math.abs(currentScroll - targetScroll) < 0.20f) {
                        if (manualBtnBounds.contains(p)) {
                            openManual(cards[currentStageIndex].tabIndex, currentStageIndex);
                            return;
                        } else if (launchBtnBounds.contains(p)) {
                            AppState.markRead(currentStageIndex);
                            updateCounters();
                            parentFrame.openSimulatorTab(cards[currentStageIndex].tabIndex);
                            return;
                        } else if (quizBtnBounds.contains(p)) {
                            parentFrame.showScreen(ArcadeOS.SCREEN_QUIZ);
                            return;
                        }
                    }

                    // Check if clicked on any visible peeker card to glide to it
                    for (int i = 0; i < cards.length; i++) {
                        if (i != currentStageIndex && cardBounds[i].contains(p)) {
                            glideToStage(i);
                            requestFocusInWindow();
                            return;
                        }
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    Point p = e.getPoint();
                    boolean overAction = manualBtnBounds.contains(p) ||
                                         launchBtnBounds.contains(p) ||
                                         quizBtnBounds.contains(p);
                    boolean overCard = false;
                    for (int i = 0; i < cards.length; i++) {
                        if (i != currentStageIndex && cardBounds[i].contains(p)) {
                            overCard = true;
                            break;
                        }
                    }
                    if (overAction || overCard) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    } else {
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) {
                g2.dispose();
                return;
            }

            int centerX = w / 2;

            // Geometry proportions
            int heroW = Math.min(620, (int)(w * 0.54));
            int heroH = h - 16;
            int peekerW = (int)(w * 0.22);
            int peekerH = (int)(heroH * 0.88);

            int stepDistance = (heroW / 2) + 16 + (peekerW / 2);

            manualBtnBounds.setBounds(0, 0, 0, 0);
            launchBtnBounds.setBounds(0, 0, 0, 0);
            quizBtnBounds.setBounds(0, 0, 0, 0);

            // Depth-sorted drawing
            Integer[] order = new Integer[cards.length];
            float[] diffs = new float[cards.length];
            for (int i = 0; i < cards.length; i++) {
                order[i] = i;
                float diff = i - currentScroll;
                while (diff < -4.0f) diff += cards.length;
                while (diff > 4.0f)  diff -= cards.length;
                diffs[i] = diff;
            }

            java.util.Arrays.sort(order, (a, b) -> Float.compare(Math.abs(diffs[b]), Math.abs(diffs[a])));

            for (int cardIdx : order) {
                float diff = diffs[cardIdx];
                float dist = Math.abs(diff);

                if (dist > 2.2f) {
                    cardBounds[cardIdx].setBounds(0, 0, 0, 0);
                    continue;
                }

                float focus = Math.max(0.0f, Math.min(1.0f, 1.0f - dist));

                int cardW = (int)(peekerW + (heroW - peekerW) * focus);
                int cardH = (int)(peekerH + (heroH - peekerH) * focus);

                int cardCenterX = (int)(centerX + diff * stepDistance);
                int cardX = cardCenterX - cardW / 2;
                int cardY = (h - cardH) / 2;

                cardBounds[cardIdx].setBounds(cardX, cardY, cardW, cardH);
                paintStageCard(g2, cardIdx, cardX, cardY, cardW, cardH, focus);
            }

            g2.dispose();
        }

        private void paintStageCard(Graphics2D g2, int idx, int x, int y, int w, int h, float focus) {
            Graphics2D g = (Graphics2D) g2.create();
            g.setClip(x, y, w, h);

            ArchiveCardData d = cards[idx];
            boolean isHero = (focus > 0.65f);

            int bgR = (int)(16 + (24 - 16) * focus);
            int bgG = (int)(12 + (16 - 12) * focus);
            int bgB = (int)(32 + (46 - 32) * focus);
            int bgA = (int)(190 + 65 * focus);
            g.setColor(new Color(bgR, bgG, bgB, bgA));
            g.fillRoundRect(x, y, w, h, 14, 14);

            if (isHero) {
                // === HERO CARD PRESENTATION (Clean, High-Contrast) ===

                // Neon breathing border
                int alpha = (int)(glowPulse * 255);
                Color dynamicBorder = new Color(5, 217, 232, Math.max(90, Math.min(255, alpha)));
                g.setColor(dynamicBorder);
                g.setStroke(new BasicStroke(2.2f));
                g.drawRoundRect(x + 1, y + 1, w - 3, h - 3, 14, 14);

                // Top soft ambient glow
                GradientPaint glow = new GradientPaint(
                        x, y, new Color(5, 217, 232, (int)(glowPulse * 50 * focus)),
                        x, y + 80, new Color(5, 217, 232, 0)
                );
                g.setPaint(glow);
                g.fillRoundRect(x + 2, y + 2, w - 4, 80, 12, 12);

                int pad = 24;

                // 1. Stage tag & Mission subtitle (NO BROKEN UNICODE GLYPHS!)
                g.setFont(new Font("Consolas", Font.BOLD, 12));
                g.setColor(RetroTheme.ACCENT_PINK);
                String tagLine = "STAGE " + d.num + " / 08  //  " + d.subtitle;
                g.drawString(tagLine, x + pad, y + 30);

                // Clean Status Badge on Top Right
                boolean isRead = (AppState.filesRead[idx] == 1);
                String statusStr = isRead ? "READ [DONE]" : "UNREAD";
                FontMetrics fm = g.getFontMetrics();
                int stW = fm.stringWidth(statusStr) + 14;
                int stX = x + w - pad - stW;
                int stY = y + 16;
                g.setColor(isRead ? new Color(20, 60, 35) : new Color(60, 45, 25));
                g.fillRoundRect(stX, stY, stW, 20, 6, 6);
                g.setColor(isRead ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_YELLOW);
                g.drawRoundRect(stX, stY, stW, 20, 6, 6);
                g.drawString(statusStr, stX + 7, stY + 14);

                // 2. Animated floating mascot sprite (centered)
                Image spr = getCardSprite(idx, 64);
                int sprX = x + (w - 64) / 2;
                int sprY = y + 46 + (int)bobOffset;
                g.drawImage(spr, sprX, sprY, null);

                // 3. Stage Title - BIG, BOLD, CRISP WHITE
                g.setFont(new Font("Consolas", Font.BOLD, 22));
                g.setColor(Color.WHITE);
                fm = g.getFontMetrics();
                g.drawString(d.title, x + (w - fm.stringWidth(d.title)) / 2, y + 138);

                // 4. Description - Clear, readable text with comfortable contrast
                g.setFont(new Font("Consolas", Font.PLAIN, 12));
                g.setColor(new Color(225, 225, 245));
                drawCenteredWrappedText(g, d.desc, x + pad, y + 162, w - pad * 2);

                // 5. Algorithm Tags - High contrast pills
                g.setFont(new Font("Consolas", Font.BOLD, 10));
                fm = g.getFontMetrics();
                int totalTagW = 0;
                for (String t : d.tags) totalTagW += fm.stringWidth(t) + 18 + 8;
                int tagX = x + (w - totalTagW) / 2;
                int tagY = y + h - 116;
                for (String t : d.tags) {
                    int tw = fm.stringWidth(t) + 18;
                    g.setColor(new Color(40, 30, 65));
                    g.fillRoundRect(tagX, tagY, tw, 22, 6, 6);
                    g.setColor(new Color(100, 80, 160));
                    g.drawRoundRect(tagX, tagY, tw, 22, 6, 6);
                    g.setColor(new Color(220, 215, 255));
                    g.drawString(t, tagX + 9, tagY + 15);
                    tagX += tw + 8;
                }

                // 6. Vector Difficulty Rating Bars (REPLACES BROKEN UNICODE [?] STARS!)
                int compY = y + h - 76;
                g.setFont(new Font("Consolas", Font.BOLD, 11));
                g.setColor(RetroTheme.ACCENT_CYAN);
                fm = g.getFontMetrics();
                String compLabel = "DIFFICULTY:";
                int labelW = fm.stringWidth(compLabel);

                int pipW = 18;
                int pipH = 8;
                int pipGap = 5;
                int totalPipsW = 5 * pipW + 4 * pipGap;
                int totalCompW = labelW + 12 + totalPipsW;
                int startCompX = x + (w - totalCompW) / 2;

                g.drawString(compLabel, startCompX, compY + 8);

                int pipX = startCompX + labelW + 12;
                for (int c = 0; c < 5; c++) {
                    if (c < d.complexity) {
                        // Active pip: glowing neon yellow
                        g.setColor(RetroTheme.ACCENT_YELLOW);
                        g.fillRoundRect(pipX, compY, pipW, pipH, 4, 4);
                    } else {
                        // Inactive pip: dark subtle box
                        g.setColor(new Color(38, 28, 58));
                        g.fillRoundRect(pipX, compY, pipW, pipH, 4, 4);
                        g.setColor(new Color(75, 55, 105));
                        g.drawRoundRect(pipX, compY, pipW, pipH, 4, 4);
                    }
                    pipX += pipW + pipGap;
                }

                // 7. Clean Action Buttons (VIEW MANUAL, LAUNCH SIMULATOR, QUIZ ARENA)
                int btnY = y + h - 50;
                int btnH = 34;
                int btnGap = 14;
                String[] btnLabels = {"VIEW MANUAL", "LAUNCH SIMULATOR", "QUIZ ARENA"};
                Color[] btnColors = {RetroTheme.ACCENT_CYAN, RetroTheme.ACCENT_PINK, RetroTheme.ACCENT_YELLOW};

                g.setFont(new Font("Consolas", Font.BOLD, 11));
                fm = g.getFontMetrics();
                int[] bWidths = new int[3];
                int totalBtnsW = 0;
                for (int b = 0; b < 3; b++) {
                    bWidths[b] = fm.stringWidth(btnLabels[b]) + 26;
                    totalBtnsW += bWidths[b];
                }
                totalBtnsW += btnGap * 2;

                int curBx = x + (w - totalBtnsW) / 2;

                manualBtnBounds.setBounds(curBx, btnY, bWidths[0], btnH);
                drawStyledButton(g, manualBtnBounds, btnLabels[0], btnColors[0], fm);
                curBx += bWidths[0] + btnGap;

                launchBtnBounds.setBounds(curBx, btnY, bWidths[1], btnH);
                drawStyledButton(g, launchBtnBounds, btnLabels[1], btnColors[1], fm);
                curBx += bWidths[1] + btnGap;

                quizBtnBounds.setBounds(curBx, btnY, bWidths[2], btnH);
                drawStyledButton(g, quizBtnBounds, btnLabels[2], btnColors[2], fm);

            } else {
                // === PEEKER CARD PRESENTATION (Clean, Compact) ===

                g.setColor(new Color(65, 45, 95));
                g.drawRoundRect(x, y, w - 1, h - 1, 14, 14);

                int cx = x + w / 2;

                // Stage Number tag
                g.setFont(new Font("Consolas", Font.BOLD, 10));
                g.setColor(RetroTheme.TEXT_DIM);
                FontMetrics fm = g.getFontMetrics();
                String numStr = "STAGE " + d.num;
                g.drawString(numStr, cx - fm.stringWidth(numStr) / 2, y + 26);

                // Compact mascot sprite
                Image spr = getCardSprite(idx, 40);
                g.drawImage(spr, cx - 20, y + 44, null);

                // Stage Title
                g.setFont(new Font("Consolas", Font.BOLD, 12));
                g.setColor(new Color(200, 200, 235));
                fm = g.getFontMetrics();
                String title = d.title;
                if (fm.stringWidth(title) > w - 16) {
                    while (fm.stringWidth(title + "...") > w - 16 && title.length() > 3) {
                        title = title.substring(0, title.length() - 1);
                    }
                    title += "...";
                }
                g.drawString(title, cx - fm.stringWidth(title) / 2, y + 104);

                // Click to select hint
                g.setFont(new Font("Consolas", Font.BOLD, 9));
                g.setColor(RetroTheme.ACCENT_CYAN);
                fm = g.getFontMetrics();
                String hint = "[ CLICK TO SELECT ]";
                g.drawString(hint, cx - fm.stringWidth(hint) / 2, y + h - 18);
            }

            g.dispose();
        }

        private void drawStyledButton(Graphics2D g, Rectangle b, String label, Color c, FontMetrics fm) {
            g.setColor(new Color(26, 16, 52));
            g.fillRoundRect(b.x, b.y, b.width, b.height, 8, 8);
            g.setColor(c);
            g.setStroke(new BasicStroke(1.4f));
            g.drawRoundRect(b.x, b.y, b.width, b.height, 8, 8);
            int textX = b.x + (b.width - fm.stringWidth(label)) / 2;
            int textY = b.y + (b.height + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString(label, textX, textY);
        }

        private void drawCenteredWrappedText(Graphics2D g, String text, int x, int y, int maxW) {
            FontMetrics fm = g.getFontMetrics();
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            int curY = y;

            for (String w : words) {
                if (fm.stringWidth(line + w + " ") > maxW && line.length() > 0) {
                    String s = line.toString().trim();
                    g.drawString(s, x + (maxW - fm.stringWidth(s)) / 2, curY);
                    line = new StringBuilder();
                    curY += fm.getHeight() + 3;
                }
                line.append(w).append(" ");
            }
            if (line.length() > 0) {
                String s = line.toString().trim();
                g.drawString(s, x + (maxW - fm.stringWidth(s)) / 2, curY);
            }
        }
    }

    private void glideToStage(int targetIdx) {
        if (targetIdx < 0 || targetIdx >= cards.length) return;

        currentStageIndex = targetIdx;

        float diff = targetIdx - currentScroll;
        while (diff < -4.0f) diff += cards.length;
        while (diff > 4.0f)  diff -= cards.length;

        targetScroll = currentScroll + diff;

        if (railTimer != null && railTimer.isRunning()) {
            railTimer.stop();
        }

        railTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                float dist = targetScroll - currentScroll;
                if (Math.abs(dist) < 0.01f) {
                    currentScroll = targetScroll;
                    currentScroll = ((currentScroll % cards.length) + cards.length) % cards.length;
                    targetScroll = currentScroll;
                    railTimer.stop();
                    updateStageIndicators();
                } else {
                    currentScroll += dist * 0.18f;
                }
                if (carouselCanvas != null) {
                    carouselCanvas.repaint();
                }
            }
        });
        railTimer.start();
        updateStageIndicators();
    }

    private void prevStage() {
        int next = (currentStageIndex - 1 + cards.length) % cards.length;
        glideToStage(next);
    }

    private void nextStage() {
        int next = (currentStageIndex + 1) % cards.length;
        glideToStage(next);
    }

    private JPanel createStageIndicatorsStrip() {
        JPanel strip = new JPanel(new BorderLayout(10, 4));
        strip.setOpaque(false);
        strip.setBorder(BorderFactory.createEmptyBorder(6, 10, 4, 10));

        JPanel pillsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        pillsRow.setOpaque(false);

        for (int i = 0; i < cards.length; i++) {
            final int stageIdx = i;
            boolean isActive = (i == currentStageIndex);
            ArchiveCardData c = cards[i];

            JButton pill = new JButton(c.num + " " + c.tags[0]) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (stageIdx == currentStageIndex) {
                        g2.setColor(new Color(65, 30, 95));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        g2.setColor(RetroTheme.ACCENT_CYAN);
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    } else {
                        g2.setColor(new Color(20, 15, 36));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        g2.setColor(new Color(50, 40, 70));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            pill.setFont(new Font("Consolas", Font.BOLD, 10));
            pill.setForeground(isActive ? Color.WHITE : new Color(160, 150, 190));
            pill.setFocusPainted(false);
            pill.setOpaque(false);
            pill.setContentAreaFilled(false);
            pill.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pill.addActionListener(e -> {
                glideToStage(stageIdx);
                requestFocusInWindow();
            });
            pillsRow.add(pill);
        }
        strip.add(pillsRow, BorderLayout.CENTER);

        // CLEAN, ERROR-FREE KEYBOARD HINTS
        JLabel hints = new JLabel("[A / D] CYCLE STAGES   •   [ENTER] LAUNCH SIMULATOR   •   [M] VIEW MANUAL");
        hints.setFont(new Font("Consolas", Font.PLAIN, 10));
        hints.setForeground(RetroTheme.TEXT_DIM);
        hints.setHorizontalAlignment(SwingConstants.CENTER);
        strip.add(hints, BorderLayout.SOUTH);

        return strip;
    }

    private void updateStageIndicators() {
        if (carouselView != null && stageIndicatorsPanel != null) {
            carouselView.remove(stageIndicatorsPanel);
            stageIndicatorsPanel = createStageIndicatorsStrip();
            carouselView.add(stageIndicatorsPanel, BorderLayout.SOUTH);
            carouselView.revalidate();
            carouselView.repaint();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GRID VIEW IMPLEMENTATION
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel createGridView() {
        JPanel grid = new JPanel(new GridLayout(2, 4, 12, 12));
        grid.setOpaque(false);
        grid.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        for (int i = 0; i < cards.length; i++) {
            grid.add(createArchiveCard(cards[i], i));
        }
        return grid;
    }

    private JPanel createArchiveCard(ArchiveCardData d, int idx) {
        JPanel card = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(RetroTheme.CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(RetroTheme.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel plus = new JLabel("+");
        plus.setFont(new Font("Consolas", Font.BOLD, 12));
        plus.setForeground(RetroTheme.ACCENT_CYAN);
        topRow.add(plus, BorderLayout.WEST);

        JLabel num = new JLabel(d.num);
        num.setFont(new Font("Consolas", Font.BOLD, 13));
        num.setForeground(new Color(110, 90, 150));
        topRow.add(num, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel iconLbl = new JLabel(new ImageIcon(getCardSprite(idx, 36)));
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(iconLbl);
        body.add(Box.createVerticalStrut(6));

        JLabel tagLbl = new JLabel("ARCHIVE " + d.num);
        tagLbl.setFont(new Font("Consolas", Font.BOLD, 9));
        tagLbl.setForeground(RetroTheme.ACCENT_PINK);
        tagLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(tagLbl);
        body.add(Box.createVerticalStrut(2));

        JLabel titleLbl = new JLabel(d.title);
        titleLbl.setFont(new Font("Consolas", Font.BOLD, 12));
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(titleLbl);
        body.add(Box.createVerticalStrut(4));

        JLabel descLbl = new JLabel("<html><center><font color='#B0B0D0' size='2'>" + d.desc + "</font></center></html>");
        descLbl.setFont(new Font("Consolas", Font.PLAIN, 9));
        descLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(descLbl);

        card.add(body, BorderLayout.CENTER);

        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);

        JButton btnView = new JButton("VIEW MANUAL");
        btnView.setFont(new Font("Consolas", Font.BOLD, 9));
        btnView.setForeground(RetroTheme.ACCENT_CYAN);
        btnView.setOpaque(false);
        btnView.setContentAreaFilled(false);
        btnView.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_CYAN, 1),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        btnView.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnView.addActionListener(e -> openManual(d.tabIndex, idx));
        bottomRow.add(btnView, BorderLayout.WEST);

        JLabel statusLbl = new JLabel(AppState.filesRead[idx] == 1 ? "STATUS: READ" : "STATUS: UNREAD");
        statusLbl.setFont(new Font("Consolas", Font.BOLD, 9));
        statusLbl.setForeground(AppState.filesRead[idx] == 1 ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_PINK);
        bottomRow.add(statusLbl, BorderLayout.EAST);

        card.add(bottomRow, BorderLayout.SOUTH);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openManual(d.tabIndex, idx);
            }
        });
        return card;
    }

    private Image getCardSprite(int idx, int sz) {
        switch (idx) {
            case 0: return PixelSprites.createAlienSprite(sz, new Color(170, 90, 255));
            case 1: return PixelSprites.createCanSprite(sz, new Color(255, 100, 130));
            case 2: return PixelSprites.createUfoSprite(sz, new Color(5, 217, 232));
            case 3: return PixelSprites.createGhostSprite(sz, new Color(255, 220, 240));
            case 4: return PixelSprites.createShieldSprite(sz, new Color(70, 160, 255));
            case 5: return PixelSprites.createBrickSprite(sz);
            case 6: return PixelSprites.createDiskSprite(sz);
            default: return PixelSprites.createBoltSprite(sz);
        }
    }

    private void openManual(int tabIndex, int cardIdx) {
        ArchiveManualDialog dialog = new ArchiveManualDialog(parentFrame, this, cardIdx);
        dialog.setVisible(true);
        updateCounters();
    }

    public void updateCounters() {
        int readCount = AppState.countFilesRead();
        if (lblFilesReadCounter != null) {
            lblFilesReadCounter.setText(String.format("%02d / 08", readCount));
        }
        if (indexProgress != null) {
            indexProgress.setValue(readCount);
        }
        repaint();
    }

    private JPanel createBottomNav() {
        JPanel bar = new JPanel(new BorderLayout(15, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));

        JButton btnBack = RetroTheme.createRetroButton("< MAIN MENU", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnBack.addActionListener(e -> parentFrame.showScreen(ArcadeOS.SCREEN_MAIN_MENU));
        bar.add(btnBack, BorderLayout.WEST);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        center.setOpaque(false);
        JLabel lblIdx = new JLabel("ARCHIVE PROGRESS");
        lblIdx.setFont(new Font("Consolas", Font.BOLD, 10));
        lblIdx.setForeground(RetroTheme.TEXT_DIM);
        center.add(lblIdx);

        indexProgress = new JProgressBar(0, 8);
        indexProgress.setValue(AppState.countFilesRead());
        indexProgress.setPreferredSize(new Dimension(160, 10));
        indexProgress.setForeground(RetroTheme.ACCENT_CYAN);
        indexProgress.setBackground(RetroTheme.INPUT_BG);
        indexProgress.setBorder(BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR, 1));
        center.add(indexProgress);

        JLabel lblCount = new JLabel("8 MODULES");
        lblCount.setFont(new Font("Consolas", Font.BOLD, 10));
        lblCount.setForeground(Color.WHITE);
        center.add(lblCount);

        bar.add(center, BorderLayout.CENTER);

        String userTag = String.format("OPERATOR: %s  |  SCORE: %s",
                AppState.playerName, AppState.getScoreFormatted());
        JLabel lblRight = new JLabel(userTag);
        lblRight.setFont(new Font("Consolas", Font.BOLD, 9));
        lblRight.setForeground(RetroTheme.ACCENT_PINK);
        lblRight.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_PINK, 1),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)
        ));
        bar.add(lblRight, BorderLayout.EAST);

        return bar;
    }
}
