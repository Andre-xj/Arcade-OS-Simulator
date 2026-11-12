package arcadeos;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * Screen 3: OPERATING SYSTEM ARCHIVES & STAGE SELECT
 * Features a buttery-smooth, cinematic sliding Arcade Carousel with double-buffered
 * easing transitions, floating idle mascot sprites, and breathing neon auras.
 */
public class ArchivesPanel extends JPanel {
    private ArcadeOS parentFrame;
    private JLabel lblFilesReadCounter;
    private JProgressBar indexProgress;

    private int currentStageIndex = 0;
    private boolean isCarouselMode = true;

    // Smooth & Slower Animation Engine
    private Timer slideTimer;
    private Timer mascotPulseTimer;
    private boolean isSliding = false;
    private float slideProgress = 1.0f;
    private int slideDirection = 1; // +1 = next (slides left), -1 = prev (slides right)
    private BufferedImage snapshotOld = null;
    private BufferedImage snapshotNew = null;

    private int pulseTicks = 0;
    private float bobOffset = 0.0f;
    private float glowPulse = 0.7f;

    private JPanel centerContentHolder;
    private JPanel carouselView;
    private JPanel gridView;
    private JPanel cardsRow;
    private JPanel stageIndicatorsPanel;

    public static class ArchiveCardData {
        public String num;
        public String title;
        public String subtitle;
        public String desc;
        public String[] tags;
        public int complexity; // 1 to 5
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

        // 1. Top Header
        add(createHeader(), BorderLayout.NORTH);

        // 2. Center Content Holder (Switches between Carousel and Grid)
        centerContentHolder = new JPanel(new BorderLayout());
        centerContentHolder.setOpaque(false);

        carouselView = createCarouselView();
        gridView     = createGridView();

        centerContentHolder.add(isCarouselMode ? carouselView : gridView, BorderLayout.CENTER);
        add(centerContentHolder, BorderLayout.CENTER);

        // 3. Bottom Navigation Bar
        add(createBottomNav(), BorderLayout.SOUTH);

        // Slower, graceful idle mascot floating & breathing glow animation (30 FPS)
        mascotPulseTimer = new Timer(33, e -> {
            pulseTicks++;
            bobOffset = (float)(Math.sin(pulseTicks * 0.055) * 6.0); // Gentle 6px float
            glowPulse = (float)(0.55 + 0.40 * Math.sin(pulseTicks * 0.040)); // Slow breathing aura
            if (isCarouselMode && cardsRow != null && !isSliding) {
                cardsRow.repaint();
            }
        });
        mascotPulseTimer.start();

        // Keyboard navigation for Carousel
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isCarouselMode && !isSliding) {
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

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setBackground(RetroTheme.PANEL_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));

        // Left Section: Mascot + MINI//OS + 3 mini buttons
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(new JLabel(new ImageIcon(PixelSprites.createRobotSprite(32))));

        JPanel leftText = new JPanel();
        leftText.setOpaque(false);
        leftText.setLayout(new BoxLayout(leftText, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        JLabel lm = new JLabel("M ");
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

        JLabel tag = new JLabel("[ CINEMATIC STAGE CAROUSEL ]");
        tag.setFont(new Font("Consolas", Font.PLAIN, 8));
        tag.setForeground(RetroTheme.ACCENT_CYAN);
        leftText.add(tag);

        left.add(leftText);
        header.add(left, BorderLayout.WEST);

        // Center Section: Title & Subtitles
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel subTag = new JLabel("OPERATING SYSTEM SIMULATOR // STAGE SELECT");
        subTag.setFont(new Font("Consolas", Font.BOLD, 9));
        subTag.setForeground(RetroTheme.ACCENT_PINK);
        subTag.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(subTag);

        JPanel midRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        midRow.setOpaque(false);
        JLabel mainTitle = new JLabel("OPERATING SYSTEM ARCHIVES & MANUALS");
        mainTitle.setFont(new Font("Consolas", Font.BOLD, 14));
        mainTitle.setForeground(Color.WHITE);
        midRow.add(mainTitle);

        JLabel pill = new JLabel(" [ ARCHIVIST SESSION ACTIVE ] ");
        pill.setFont(new Font("Consolas", Font.BOLD, 9));
        pill.setForeground(RetroTheme.ACCENT_GREEN);
        pill.setBorder(BorderFactory.createLineBorder(RetroTheme.ACCENT_GREEN, 1));
        midRow.add(pill);
        center.add(midRow);

        JLabel desc = new JLabel("Smoothly browse through 8 OS stages, inspect interactive manuals, and launch simulation missions.");
        desc.setFont(new Font("Consolas", Font.PLAIN, 10));
        desc.setForeground(RetroTheme.TEXT_DIM);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(desc);

        header.add(center, BorderLayout.CENTER);

        // Right Section: View Switcher (Carousel / Grid) + Files Read Counter
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        right.setOpaque(false);

        JButton btnToggleView = RetroTheme.createRetroButton("🎠 CAROUSEL", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnToggleView.setFont(new Font("Consolas", Font.BOLD, 9));
        btnToggleView.addActionListener(e -> {
            if (isSliding) return;
            isCarouselMode = !isCarouselMode;
            btnToggleView.setText(isCarouselMode ? "🎠 CAROUSEL" : "⊞ GRID VIEW");
            btnToggleView.setForeground(isCarouselMode ? RetroTheme.ACCENT_CYAN : RetroTheme.ACCENT_YELLOW);
            centerContentHolder.removeAll();
            centerContentHolder.add(isCarouselMode ? carouselView : gridView, BorderLayout.CENTER);
            centerContentHolder.revalidate();
            centerContentHolder.repaint();
            requestFocusInWindow();
        });
        right.add(btnToggleView);

        // Files Read Counter
        JPanel readBox = new JPanel();
        readBox.setOpaque(false);
        readBox.setLayout(new BoxLayout(readBox, BoxLayout.Y_AXIS));

        JLabel lblRead = new JLabel("FILES READ");
        lblRead.setFont(new Font("Consolas", Font.BOLD, 9));
        lblRead.setForeground(RetroTheme.TEXT_DIM);
        readBox.add(lblRead);

        lblFilesReadCounter = new JLabel(String.format("%03d / 008", AppState.countFilesRead()));
        lblFilesReadCounter.setFont(new Font("Consolas", Font.BOLD, 16));
        lblFilesReadCounter.setForeground(RetroTheme.ACCENT_YELLOW);
        readBox.add(lblFilesReadCounter);

        right.add(readBox);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DUAL-BUFFERED CINEMATIC SLIDING CAROUSEL
    // ─────────────────────────────────────────────────────────────────────────

    private JPanel createCarouselView() {
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);
        container.setBorder(BorderFactory.createEmptyBorder(6, 4, 4, 4));

        // 1. Center Stage Slider Track
        JPanel sliderTrack = new JPanel(new BorderLayout(14, 0));
        sliderTrack.setOpaque(false);

        // Big Left Carousel Button
        JButton btnPrev = createCarouselNavButton("◀", "PREV STAGE [A]");
        btnPrev.addActionListener(e -> { prevStage(); requestFocusInWindow(); });
        sliderTrack.add(btnPrev, BorderLayout.WEST);

        // Smooth Dual-Snapshot Carousel Sliding Canvas
        cardsRow = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (isSliding && snapshotOld != null && snapshotNew != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                    int w = getWidth();
                    int h = getHeight();

                    // Slower, graceful sinusoidal ease-in-out curve
                    float t = slideProgress;
                    float ease = (float)(0.5 - 0.5 * Math.cos(t * Math.PI));

                    // Direction: +1 means moving to next (current shifts left, next comes from right)
                    int shiftOld = (int)(-slideDirection * ease * w);
                    int shiftNew = (int)(shiftOld + slideDirection * w);

                    // Draw outgoing snapshot with subtle fading
                    float alphaOld = Math.max(0.0f, Math.min(1.0f, 1.0f - ease * 0.35f));
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaOld));
                    g2.drawImage(snapshotOld, shiftOld, 0, w, h, null);

                    // Draw incoming snapshot with smooth fading in
                    float alphaNew = Math.max(0.0f, Math.min(1.0f, 0.40f + ease * 0.60f));
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaNew));
                    g2.drawImage(snapshotNew, shiftNew, 0, w, h, null);

                    g2.dispose();
                }
            }

            @Override
            protected void paintChildren(Graphics g) {
                // When actively sliding, hide child components and paint smooth snapshots instead
                if (!isSliding) {
                    super.paintChildren(g);
                }
            }
        };
        cardsRow.setOpaque(false);
        rebuildCarouselCards();

        sliderTrack.add(cardsRow, BorderLayout.CENTER);

        // Big Right Carousel Button
        JButton btnNext = createCarouselNavButton("▶", "NEXT STAGE [D]");
        btnNext.addActionListener(e -> { nextStage(); requestFocusInWindow(); });
        sliderTrack.add(btnNext, BorderLayout.EAST);

        container.add(sliderTrack, BorderLayout.CENTER);

        // 2. Bottom Stage Selector Strip & Dots
        stageIndicatorsPanel = createStageIndicatorsStrip();
        container.add(stageIndicatorsPanel, BorderLayout.SOUTH);

        return container;
    }

    private void rebuildCarouselCards() {
        if (cardsRow == null) return;
        cardsRow.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 8, 0, 8);

        int prevIdx = (currentStageIndex - 1 + cards.length) % cards.length;
        int nextIdx = (currentStageIndex + 1) % cards.length;

        // 1. Left Peeker Card
        gbc.gridx = 0;
        gbc.weightx = 0.22;
        gbc.weighty = 1.0;
        JPanel leftPeeker = createPeekerCard(cards[prevIdx], prevIdx, true);
        cardsRow.add(leftPeeker, gbc);

        // 2. Center Hero Featured Stage Card
        gbc.gridx = 1;
        gbc.weightx = 0.56;
        gbc.weighty = 1.0;
        JPanel centerHero = createHeroStageCard(cards[currentStageIndex], currentStageIndex);
        cardsRow.add(centerHero, gbc);

        // 3. Right Peeker Card
        gbc.gridx = 2;
        gbc.weightx = 0.22;
        gbc.weighty = 1.0;
        JPanel rightPeeker = createPeekerCard(cards[nextIdx], nextIdx, false);
        cardsRow.add(rightPeeker, gbc);

        cardsRow.revalidate();
        cardsRow.repaint();
    }

    /** Center Hero Card with breathing animation and floating sprite */
    private JPanel createHeroStageCard(ArchiveCardData d, int idx) {
        JPanel hero = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Card background
                g2.setColor(new Color(24, 16, 46));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                // Breathing dynamic neon border with sinusoidal glow
                int alpha = (int)(glowPulse * 255);
                Color dynamicBorder = new Color(5, 217, 232, Math.max(80, Math.min(255, alpha)));
                g2.setColor(dynamicBorder);
                g2.setStroke(new BasicStroke(2.2f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);

                // Inner top glow
                GradientPaint glow = new GradientPaint(
                        0, 0, new Color(5, 217, 232, (int)(glowPulse * 55)),
                        0, 90, new Color(5, 217, 232, 0)
                );
                g2.setPaint(glow);
                g2.fillRoundRect(2, 2, getWidth() - 4, 90, 14, 14);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        hero.setOpaque(false);
        hero.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));

        // Top Row: Stage Badge + Read Status
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel stageBadge = new JLabel("✦ STAGE " + d.num + " / 08 ✦  " + d.subtitle);
        stageBadge.setFont(new Font("Consolas", Font.BOLD, 11));
        stageBadge.setForeground(RetroTheme.ACCENT_PINK);
        topRow.add(stageBadge, BorderLayout.WEST);

        boolean isRead = (AppState.filesRead[idx] == 1);
        JLabel statusLbl = new JLabel(isRead ? "STATUS: READ ✔ (+50 XP)" : "STATUS: UNREAD >");
        statusLbl.setFont(new Font("Consolas", Font.BOLD, 10));
        statusLbl.setForeground(isRead ? RetroTheme.ACCENT_GREEN : RetroTheme.ACCENT_YELLOW);
        topRow.add(statusLbl, BorderLayout.EAST);
        hero.add(topRow, BorderLayout.NORTH);

        // Center: Floating Sprite + Title + Description + Tags + Complexity
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        // Floating Animated Mascot Sprite (Gentle slow sine wave)
        JPanel spriteHolder = new JPanel() {
            @Override
            public Dimension getPreferredSize() { return new Dimension(72, 72); }
            @Override
            public Dimension getMaximumSize() { return new Dimension(72, 72); }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                Image spr = getCardSprite(idx, 64);
                int drawY = 4 + (int) bobOffset;
                g2.drawImage(spr, 4, drawY, this);
                g2.dispose();
            }
        };
        spriteHolder.setOpaque(false);
        spriteHolder.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(spriteHolder);
        body.add(Box.createVerticalStrut(6));

        // Mission Title
        JLabel titleLbl = new JLabel(d.title);
        titleLbl.setFont(new Font("Consolas", Font.BOLD, 18));
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(titleLbl);
        body.add(Box.createVerticalStrut(4));

        // Description
        JLabel descLbl = new JLabel("<html><center><font color='#B0B0D0' size='3'>" + d.desc + "</font></center></html>");
        descLbl.setFont(new Font("Consolas", Font.PLAIN, 11));
        descLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(descLbl);
        body.add(Box.createVerticalStrut(8));

        // Tags
        JPanel tagsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        tagsPanel.setOpaque(false);
        for (String t : d.tags) {
            JLabel pill = new JLabel(t);
            pill.setFont(new Font("Consolas", Font.PLAIN, 9));
            pill.setForeground(new Color(190, 180, 240));
            pill.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(75, 55, 120), 1),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)
            ));
            tagsPanel.add(pill);
        }
        body.add(tagsPanel);
        body.add(Box.createVerticalStrut(6));

        // Complexity Stars
        JPanel compRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        compRow.setOpaque(false);
        JLabel compLbl = new JLabel("ALGORITHM COMPLEXITY: ");
        compLbl.setFont(new Font("Consolas", Font.PLAIN, 10));
        compLbl.setForeground(RetroTheme.TEXT_DIM);
        compRow.add(compLbl);

        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < 5; c++) {
            sb.append(c < d.complexity ? "★ " : "☆ ");
        }
        JLabel starsLbl = new JLabel(sb.toString().trim());
        starsLbl.setFont(new Font("Consolas", Font.BOLD, 11));
        starsLbl.setForeground(RetroTheme.ACCENT_YELLOW);
        compRow.add(starsLbl);
        body.add(compRow);

        hero.add(body, BorderLayout.CENTER);

        // Bottom Action Bar: Three Buttons!
        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        actionsRow.setOpaque(false);

        JButton btnManual = RetroTheme.createRetroButton("📖 VIEW MANUAL", RetroTheme.BTN_DARK, RetroTheme.ACCENT_CYAN);
        btnManual.addActionListener(e -> openManual(d.tabIndex, idx));

        JButton btnLaunch = RetroTheme.createRetroButton("🚀 LAUNCH SIMULATOR", new Color(45, 20, 70), RetroTheme.ACCENT_PINK);
        btnLaunch.addActionListener(e -> {
            AppState.markRead(idx);
            updateCounters();
            parentFrame.openSimulatorTab(d.tabIndex);
        });

        JButton btnQuiz = RetroTheme.createRetroButton("❓ QUIZ ARENA", RetroTheme.BTN_DARK, RetroTheme.ACCENT_YELLOW);
        btnQuiz.addActionListener(e -> parentFrame.showScreen(ArcadeOS.SCREEN_QUIZ));

        actionsRow.add(btnManual);
        actionsRow.add(btnLaunch);
        actionsRow.add(btnQuiz);

        hero.add(actionsRow, BorderLayout.SOUTH);
        return hero;
    }

    /** Side Peeker Cards with hover animation */
    private JPanel createPeekerCard(ArchiveCardData d, int idx, boolean isLeft) {
        JPanel peeker = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 12, 30, 210));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(60, 45, 90));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        peeker.setOpaque(false);
        peeker.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
        peeker.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        peeker.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isSliding) {
                    animateTransition(idx, isLeft ? -1 : 1);
                }
            }
        });

        JLabel tagLbl = new JLabel(isLeft ? "◄ PREV: STAGE " + d.num : "STAGE " + d.num + " :NEXT ►");
        tagLbl.setFont(new Font("Consolas", Font.BOLD, 10));
        tagLbl.setForeground(RetroTheme.TEXT_DIM);
        tagLbl.setHorizontalAlignment(SwingConstants.CENTER);
        peeker.add(tagLbl, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel spriteLbl = new JLabel(new ImageIcon(getCardSprite(idx, 40)));
        spriteLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(spriteLbl);
        center.add(Box.createVerticalStrut(8));

        JLabel titleLbl = new JLabel("<html><center><font color='#A0A0C0'><b>" + d.title + "</b></font></center></html>");
        titleLbl.setFont(new Font("Consolas", Font.BOLD, 11));
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(titleLbl);

        peeker.add(center, BorderLayout.CENTER);

        JLabel clickLbl = new JLabel("[ CLICK TO SELECT ]");
        clickLbl.setFont(new Font("Consolas", Font.PLAIN, 8));
        clickLbl.setForeground(RetroTheme.ACCENT_CYAN);
        clickLbl.setHorizontalAlignment(SwingConstants.CENTER);
        peeker.add(clickLbl, BorderLayout.SOUTH);

        return peeker;
    }

    private JButton createCarouselNavButton(String arrow, String tooltip) {
        JButton btn = new JButton(arrow) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(55, 32, 90) : new Color(25, 18, 45));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(getModel().isRollover() ? RetroTheme.ACCENT_CYAN : RetroTheme.BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Consolas", Font.BOLD, 26));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        btn.setToolTipText(tooltip);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { btn.setForeground(RetroTheme.ACCENT_CYAN); }
            @Override
            public void mouseExited(MouseEvent e) { btn.setForeground(Color.WHITE); }
        });
        return btn;
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
            pill.setFont(new Font("Consolas", Font.BOLD, 9));
            pill.setForeground(isActive ? Color.WHITE : new Color(160, 150, 190));
            pill.setFocusPainted(false);
            pill.setOpaque(false);
            pill.setContentAreaFilled(false);
            pill.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            pill.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pill.addActionListener(e -> {
                if (!isSliding) {
                    int dir = (stageIdx > currentStageIndex) ? 1 : -1;
                    animateTransition(stageIdx, dir);
                    requestFocusInWindow();
                }
            });
            pillsRow.add(pill);
        }
        strip.add(pillsRow, BorderLayout.CENTER);

        JLabel hints = new JLabel("[◄ / ►] OR [A / D] CYCLE STAGES   •   [ENTER] LAUNCH SIMULATOR   •   [M] OPEN MANUAL");
        hints.setFont(new Font("Consolas", Font.PLAIN, 9));
        hints.setForeground(RetroTheme.TEXT_DIM);
        hints.setHorizontalAlignment(SwingConstants.CENTER);
        strip.add(hints, BorderLayout.SOUTH);

        return strip;
    }

    private void prevStage() {
        if (isSliding) return;
        int next = (currentStageIndex - 1 + cards.length) % cards.length;
        animateTransition(next, -1);
    }

    private void nextStage() {
        if (isSliding) return;
        int next = (currentStageIndex + 1) % cards.length;
        animateTransition(next, 1);
    }

    /** Take snapshot of component into an offscreen image */
    private BufferedImage captureSnapshot(JComponent comp) {
        int w = Math.max(1, comp.getWidth());
        int h = Math.max(1, comp.getHeight());
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        comp.paint(g2);
        g2.dispose();
        return img;
    }

    /**
     * Silky-smooth, slower cinematic sliding transition.
     * Takes ~450ms across ~28 frames with sinusoidal ease-in-out curve.
     */
    private void animateTransition(int targetIdx, int direction) {
        if (targetIdx == currentStageIndex || isSliding) return;

        int w = cardsRow.getWidth();
        int h = cardsRow.getHeight();
        if (w <= 0 || h <= 0) {
            // If not yet laid out, change instantly
            currentStageIndex = targetIdx;
            rebuildCarouselCards();
            updateStageIndicators();
            return;
        }

        // 1. Capture snapshot of current stage view
        snapshotOld = captureSnapshot(cardsRow);

        // 2. Temporarily switch to target stage and capture incoming snapshot
        currentStageIndex = targetIdx;
        rebuildCarouselCards();
        snapshotNew = captureSnapshot(cardsRow);

        // 3. Setup smooth animation parameters
        this.slideDirection = direction;
        this.slideProgress = 0.0f;
        this.isSliding = true;

        if (slideTimer != null && slideTimer.isRunning()) {
            slideTimer.stop();
        }

        // Slower, graceful timer: step 0.035f at 16ms = ~28 frames = ~460 milliseconds of buttery gliding
        slideTimer = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                slideProgress += 0.036f;
                if (slideProgress >= 1.0f) {
                    slideProgress = 1.0f;
                    isSliding = false;
                    slideTimer.stop();
                    snapshotOld = null;
                    snapshotNew = null;
                    rebuildCarouselCards();
                    updateStageIndicators();
                }
                if (cardsRow != null) {
                    cardsRow.repaint();
                }
            }
        });
        slideTimer.start();
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
    // OPTIONAL GRID VIEW IMPLEMENTATION
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
        JLabel plus = new JLabel("✦");
        plus.setFont(new Font("Consolas", Font.PLAIN, 10));
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

        JLabel tagLbl = new JLabel("ARCHIVE FILE " + d.num);
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

        JLabel descLbl = new JLabel("<html><center><font color='#8C8CAA' size='2'>" + d.desc + "</font></center></html>");
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

        JLabel statusLbl = new JLabel(AppState.filesRead[idx] == 1 ? "STATUS: READ ✔" : "STATUS: UNREAD >");
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
            lblFilesReadCounter.setText(String.format("%03d / 008", readCount));
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
        JLabel lblIdx = new JLabel("ARCHIVE INDEX");
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

        JLabel lblCount = new JLabel("8 STAGES");
        lblCount.setFont(new Font("Consolas", Font.BOLD, 10));
        lblCount.setForeground(Color.WHITE);
        center.add(lblCount);

        JLabel lblOnline = new JLabel("🪐 ARCADE CAROUSEL ACTIVE");
        lblOnline.setFont(new Font("Consolas", Font.BOLD, 9));
        lblOnline.setForeground(RetroTheme.ACCENT_YELLOW);
        center.add(lblOnline);

        bar.add(center, BorderLayout.CENTER);

        String userTag = String.format("> CURRENT USER: %s [SCORE: %s] [GUEST MODE]",
                AppState.playerName, AppState.getScoreFormatted());
        JLabel lblRight = new JLabel(userTag);
        lblRight.setFont(new Font("Consolas", Font.BOLD, 9));
        lblRight.setForeground(RetroTheme.ACCENT_PINK);
        lblRight.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(RetroTheme.ACCENT_PINK, 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        bar.add(lblRight, BorderLayout.EAST);

        return bar;
    }
}
