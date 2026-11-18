package arcadeos;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Generates custom 8-bit / pixel art sprites using Java 2D Graphics.
 */
public class PixelSprites {

    /** Mascot Robot Icon (green winking box robot) */
    public static BufferedImage createRobotSprite(int size) {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        Color green = new Color(57, 255, 140);
        Color greenDark = new Color(20, 180, 90);
        Color eyeBlack = new Color(10, 8, 20);
        Color blush = new Color(255, 100, 160);

        // Antenna
        g.setColor(greenDark);
        g.fillRect(14, 2, 4, 4);

        // Body
        g.setColor(green);
        g.fillRect(6, 6, 20, 18);
        g.setColor(greenDark);
        g.drawRect(6, 6, 19, 17);

        // Eyes: left normal eye, right winking or smile
        g.setColor(eyeBlack);
        g.fillRect(9, 12, 3, 3); // Left eye
        // Right winking eye: > shape
        g.fillRect(19, 12, 3, 1);
        g.fillRect(18, 13, 2, 1);
        g.fillRect(19, 14, 3, 1);

        // Cheeks blush
        g.setColor(blush);
        g.fillRect(8, 16, 2, 2);
        g.fillRect(21, 16, 2, 2);

        // Mouth (cute smile)
        g.setColor(eyeBlack);
        g.fillRect(14, 17, 3, 1);

        // Feet
        g.setColor(greenDark);
        g.fillRect(9, 24, 4, 4);
        g.fillRect(18, 24, 4, 4);

        g.dispose();

        if (size != 32) {
            BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(img, 0, 0, size, size, null);
            g2.dispose();
            return scaled;
        }
        return img;
    }

    /** 8-bit Space Invader / Alien sprite */
    public static BufferedImage createAlienSprite(int size, Color c) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);

        int[][] pixels = {
                { 3, 1 }, { 12, 1 },
                { 4, 2 }, { 11, 2 },
                { 3, 3 }, { 4, 3 }, { 5, 3 }, { 6, 3 }, { 7, 3 }, { 8, 3 }, { 9, 3 }, { 10, 3 }, { 11, 3 }, { 12, 3 },
                { 2, 4 }, { 3, 4 }, { 5, 4 }, { 6, 4 }, { 7, 4 }, { 8, 4 }, { 9, 4 }, { 10, 4 }, { 12, 4 }, { 13, 4 },
                { 1, 5 }, { 2, 5 }, { 3, 5 }, { 4, 5 }, { 5, 5 }, { 6, 5 }, { 7, 5 }, { 8, 5 }, { 9, 5 }, { 10, 5 },
                { 11, 5 }, { 12, 5 }, { 13, 5 }, { 14, 5 },
                { 1, 6 }, { 3, 6 }, { 4, 6 }, { 5, 6 }, { 6, 6 }, { 7, 6 }, { 8, 6 }, { 9, 6 }, { 10, 6 }, { 11, 6 },
                { 12, 6 }, { 14, 6 },
                { 1, 7 }, { 3, 7 }, { 12, 7 }, { 14, 7 },
                { 4, 8 }, { 5, 8 }, { 10, 8 }, { 11, 8 },
                { 3, 9 }, { 4, 9 }, { 11, 9 }, { 12, 9 }
        };
        for (int[] p : pixels) {
            g.fillRect(p[0], p[1], 1, 1);
        }
        g.dispose();
        return scale(img, size);
    }

    /** 8-bit Cyber Snake Head Sprite */
    public static BufferedImage createSnakeSprite(int size, Color c) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        // Head outline & body
        g.setColor(c);
        g.fillRect(3, 4, 10, 8);
        g.fillRect(2, 6, 12, 5);
        g.fillRect(5, 2, 6, 3);
        // Snout ridge
        g.fillRect(4, 12, 8, 2);
        // Big expressive eyes
        g.setColor(Color.WHITE);
        g.fillRect(3, 6, 4, 4);
        g.fillRect(9, 6, 4, 4);
        // Slit pupils (Pulsing neon pink/red)
        g.setColor(new Color(255, 42, 141));
        g.fillRect(5, 7, 2, 3);
        g.fillRect(9, 7, 2, 3);
        // Tongue (forked red)
        g.setColor(new Color(255, 60, 60));
        g.fillRect(7, 14, 2, 2);
        g.fillRect(6, 15, 1, 1);
        g.fillRect(9, 15, 1, 1);
        // Cyber crown glint
        g.setColor(new Color(255, 255, 180));
        g.fillRect(7, 3, 2, 2);

        g.dispose();
        return scale(img, size);
    }

    /** Cute Ghost Sprite */
    public static BufferedImage createGhostSprite(int size, Color c) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);

        // Body
        g.fillRect(4, 2, 8, 2);
        g.fillRect(2, 4, 12, 7);
        // Frills
        g.fillRect(2, 11, 2, 2);
        g.fillRect(6, 11, 4, 2);
        g.fillRect(12, 11, 2, 2);

        // Eyes
        g.setColor(Color.WHITE);
        g.fillRect(4, 5, 3, 3);
        g.fillRect(9, 5, 3, 3);
        g.setColor(Color.BLUE);
        g.fillRect(5, 6, 2, 2);
        g.fillRect(10, 6, 2, 2);

        g.dispose();
        return scale(img, size);
    }

    /** UFO / Spaceship */
    public static BufferedImage createUfoSprite(int size, Color c) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Dome
        g.setColor(new Color(5, 217, 232));
        g.fillRect(6, 3, 4, 3);

        // Saucer
        g.setColor(c);
        g.fillRect(3, 6, 10, 3);
        g.fillRect(1, 8, 14, 2);

        // Lights
        g.setColor(Color.YELLOW);
        g.fillRect(3, 9, 2, 1);
        g.fillRect(7, 9, 2, 1);
        g.fillRect(11, 9, 2, 1);

        g.dispose();
        return scale(img, size);
    }

    /** Banker Can / Safe */
    public static BufferedImage createCanSprite(int size, Color c) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setColor(c);
        g.fillRect(4, 3, 8, 10);
        g.setColor(Color.WHITE);
        g.fillRect(5, 4, 6, 2);
        g.setColor(new Color(255, 211, 25));
        g.fillRect(6, 7, 4, 4);

        g.dispose();
        return scale(img, size);
    }

    /** Shield Icon */
    public static BufferedImage createShieldSprite(int size, Color c) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setColor(c);
        g.fillRect(3, 2, 10, 6);
        g.fillRect(4, 8, 8, 3);
        g.fillRect(6, 11, 4, 2);
        g.fillRect(7, 13, 2, 1);

        g.setColor(new Color(150, 230, 255));
        g.fillRect(5, 4, 3, 4);

        g.dispose();
        return scale(img, size);
    }

    /** Brick Wall Sprite */
    public static BufferedImage createBrickSprite(int size) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setColor(new Color(210, 75, 40));
        g.fillRect(2, 2, 12, 12);

        g.setColor(new Color(255, 140, 90));
        g.fillRect(2, 2, 5, 3);
        g.fillRect(8, 2, 6, 3);
        g.fillRect(2, 6, 12, 1);
        g.fillRect(5, 7, 6, 3);
        g.fillRect(2, 11, 12, 1);

        g.dispose();
        return scale(img, size);
    }

    /** Space Shoot / Arcade Space Fighter Ship */
    public static BufferedImage createSpaceShootShip(int size, Color c) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // 1. Sleek Delta Wings & Fuselage
        g.setColor(c != null ? c : new Color(255, 180, 50));
        int[] wx = {8, 10, 14, 13, 10, 8, 5, 2, 1, 6};
        int[] wy = {1,  5, 11, 13, 12, 11, 12, 13, 11, 5};
        g.fillPolygon(wx, wy, 10);

        // 2. Wingtip Laser Cannons
        g.setColor(new Color(255, 60, 60));
        g.fillRect(1, 8, 2, 4);
        g.fillRect(13, 8, 2, 4);

        // 3. Glowing Neon Cyan Cockpit
        g.setColor(new Color(0, 240, 255));
        g.fillRect(7, 4, 2, 4);
        g.setColor(Color.WHITE);
        g.fillRect(7, 4, 1, 2); // Glint

        // 4. Plasma Thruster Engine Plume
        g.setColor(new Color(255, 220, 40));
        g.fillRect(6, 12, 4, 2);
        g.setColor(new Color(255, 90, 0));
        g.fillRect(7, 14, 2, 2);

        g.dispose();
        return scale(img, size);
    }

    /** Cyber Crane Claw / Overhead RAM Manipulator */
    public static BufferedImage createCraneSprite(int size, Color c) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        Color craneColor = c != null ? c : new Color(195, 115, 255);

        // 1. Top Gantry Trolley Mount
        g.setColor(new Color(80, 75, 105));
        g.fillRect(3, 1, 10, 3);
        g.setColor(new Color(255, 200, 30)); // Hazard Stripes
        g.fillRect(5, 1, 2, 2);
        g.fillRect(9, 1, 2, 2);

        // 2. Vertical Hoist Hydraulic Rod
        g.setColor(new Color(200, 205, 230));
        g.fillRect(7, 4, 2, 4);

        // 3. Articulated Claw Head Mount
        g.setColor(craneColor);
        g.fillRect(5, 7, 6, 3);

        // 4. Left & Right Mechanical Gripper Jaws
        g.setColor(craneColor.brighter());
        g.fillRect(3, 9, 2, 4);
        g.fillRect(4, 13, 2, 2); // Left finger hook
        g.fillRect(11, 9, 2, 4);
        g.fillRect(10, 13, 2, 2); // Right finger hook

        // 5. Glowing Microchip / RAM Block held in claw
        g.setColor(new Color(0, 240, 255));
        g.fillRect(6, 10, 4, 3);
        g.setColor(new Color(255, 255, 100));
        g.fillRect(7, 11, 2, 1); // Microchip core

        g.dispose();
        return scale(img, size);
    }

    /** Disk / Platter */
    public static BufferedImage createDiskSprite(int size) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setColor(new Color(220, 220, 240));
        g.fillOval(2, 2, 12, 12);
        g.setColor(new Color(50, 40, 80));
        g.fillOval(6, 6, 4, 4);

        g.dispose();
        return scale(img, size);
    }

    /** Lightning Bolt Sprite */
    public static BufferedImage createBoltSprite(int size) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setColor(new Color(255, 211, 25));
        int[] x = { 9, 5, 8, 4, 12, 8 };
        int[] y = { 2, 7, 7, 14, 6, 6 };
        g.fillPolygon(x, y, 6);

        g.dispose();
        return scale(img, size);
    }

    /** Process / Alien Avatar by Index (0..6) */
    public static BufferedImage getJobAvatar(int idx, int size) {
        Color[] colors = {
                new Color(180, 70, 255), // purple alien
                new Color(5, 217, 232), // cyan ship
                new Color(255, 140, 200), // pink ghost
                new Color(255, 70, 70), // red crab
                new Color(255, 40, 100), // cherry
                new Color(255, 180, 50), // orange
                new Color(100, 255, 150) // green demon
        };
        Color c = colors[idx % colors.length];
        switch (idx % 4) {
            case 0:
                return createAlienSprite(size, c);
            case 1:
                return createUfoSprite(size, c);
            case 2:
                return createGhostSprite(size, c);
            default:
                return createShieldSprite(size, c);
        }
    }

    /** High-Tech Cyber Agent / Dispatcher Sprite (Tactical Helmet, Neon Visor, Arm Cannon) */
    public static BufferedImage createPlayerSprite(int size) {
        BufferedImage img = new BufferedImage(18, 22, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // 1. Cyber Helmet (Dark graphite / titanium with antenna)
        g.setColor(new Color(25, 20, 42));
        g.fillRect(4, 2, 8, 6);
        g.setColor(new Color(45, 35, 75));
        g.drawRect(4, 2, 7, 5);

        // Antenna on helmet
        g.setColor(new Color(0, 240, 255));
        g.fillRect(3, 1, 2, 3);
        g.setColor(Color.WHITE);
        g.fillRect(3, 0, 1, 1);

        // 2. Glowing Neon Cyan Visor (Cyberpunk optical goggles)
        g.setColor(new Color(0, 240, 255));
        g.fillRect(6, 4, 6, 2);
        g.setColor(Color.WHITE); // Visor reflection glint
        g.fillRect(7, 4, 2, 1);

        // Tactical chin guard / respirator
        g.setColor(new Color(35, 25, 55));
        g.fillRect(5, 7, 6, 2);

        // 3. Cyber Armor Chestplate (Obsidian with Neon Magenta Reactor)
        g.setColor(new Color(28, 20, 50));
        g.fillRect(4, 9, 8, 6);
        g.setColor(new Color(60, 45, 95));
        g.drawRect(4, 9, 7, 5);

        // Core Power Reactor on chest (pulsing magenta diamond)
        g.setColor(new Color(255, 42, 141));
        g.fillRect(7, 11, 2, 2);
        g.setColor(Color.WHITE);
        g.fillRect(7, 11, 1, 1);

        // 4. Arm Blaster Cannon pointing forward (right)
        g.setColor(new Color(40, 30, 65));
        g.fillRect(10, 10, 5, 3);
        // Blaster muzzle tip (Glowing Neon Cyan)
        g.setColor(new Color(0, 240, 255));
        g.fillRect(15, 10, 2, 3);
        g.setColor(Color.WHITE);
        g.fillRect(16, 11, 1, 1);

        // 5. Tactical Utility Belt with Battery Cell
        g.setColor(new Color(255, 211, 25));
        g.fillRect(4, 15, 8, 2);
        g.setColor(new Color(57, 255, 140)); // Green power cell
        g.fillRect(6, 15, 2, 2);

        // 6. Armored Cyber Legs & Magnetic Boots
        g.setColor(new Color(32, 24, 56));
        g.fillRect(4, 17, 3, 4);
        g.fillRect(9, 17, 3, 4);

        // Magnetic Thruster Soles (Glowing Cyan bottom)
        g.setColor(new Color(0, 240, 255));
        g.fillRect(3, 20, 4, 2);
        g.fillRect(9, 20, 4, 2);

        g.dispose();
        return scale(img, size);
    }

    /** Classic 8-Bit Mario Sprite (Red Cap, Blue Overalls, Mustache) */
    public static BufferedImage createMarioSprite(int size) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        Color red = new Color(228, 0, 0);
        Color skin = new Color(252, 152, 56);
        Color brown = new Color(136, 112, 0);
        Color blue = new Color(0, 68, 228);
        Color yellow = new Color(252, 228, 0);

        // Cap
        g.setColor(red);
        g.fillRect(3, 1, 6, 2);
        g.fillRect(2, 2, 9, 1);

        // Hair / Sideburns
        g.setColor(brown);
        g.fillRect(2, 3, 3, 1);
        g.fillRect(1, 4, 2, 2);
        g.fillRect(3, 4, 1, 1);

        // Face / Skin
        g.setColor(skin);
        g.fillRect(4, 3, 5, 3);
        g.fillRect(3, 5, 2, 1);

        // Eye & Mustache
        g.setColor(Color.BLACK);
        g.fillRect(7, 3, 1, 2); // Eye
        g.setColor(brown);
        g.fillRect(5, 5, 5, 1); // Mustache
        g.fillRect(7, 6, 2, 1);

        // Red Shirt & Arms
        g.setColor(red);
        g.fillRect(2, 7, 8, 4);
        g.fillRect(1, 8, 2, 3);
        g.fillRect(9, 8, 2, 3);

        // Blue Overalls
        g.setColor(blue);
        g.fillRect(3, 8, 6, 5);
        g.fillRect(4, 7, 4, 2);
        g.fillRect(2, 12, 3, 2);
        g.fillRect(7, 12, 3, 2);

        // Yellow Buttons
        g.setColor(yellow);
        g.fillRect(4, 9, 1, 1);
        g.fillRect(7, 9, 1, 1);

        // Shoes
        g.setColor(brown);
        g.fillRect(1, 14, 4, 2);
        g.fillRect(7, 14, 4, 2);

        g.dispose();
        return scale(img, size);
    }

    /** Iconic Mario Power-Up Items for Memory Pages (0..7) */
    public static BufferedImage getMarioItem(int page, int size) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        int p = (page >= 0 ? page : 0) % 8;

        switch (p) {
            case 0: // Super Mushroom (Red)
                g.setColor(new Color(228, 0, 0)); // Red Cap
                g.fillRoundRect(2, 1, 12, 9, 4, 4);
                g.setColor(Color.WHITE); // Spots
                g.fillRect(6, 2, 4, 4);
                g.fillRect(2, 4, 2, 3);
                g.fillRect(12, 4, 2, 3);
                g.setColor(new Color(252, 216, 168)); // Stem Face
                g.fillRect(4, 9, 8, 6);
                g.setColor(Color.BLACK); // Eyes
                g.fillRect(5, 10, 1, 3);
                g.fillRect(10, 10, 1, 3);
                break;

            case 1: // Super Star (Yellow)
                g.setColor(new Color(252, 228, 0)); // Star Body
                int[] starX = {8, 10, 15, 11, 13, 8, 3, 5, 1, 6};
                int[] starY = {1, 5, 6, 9, 14, 11, 14, 9, 6, 5};
                g.fillPolygon(starX, starY, 10);
                g.setColor(new Color(200, 160, 0));
                g.drawPolygon(starX, starY, 10);
                g.setColor(Color.BLACK); // Eyes
                g.fillRect(6, 6, 1, 3);
                g.fillRect(9, 6, 1, 3);
                break;

            case 2: // Fire Flower
                g.setColor(new Color(228, 0, 0)); // Red Petals
                g.fillOval(3, 1, 10, 9);
                g.setColor(Color.WHITE); // White Ring
                g.fillOval(5, 2, 6, 7);
                g.setColor(new Color(252, 168, 0)); // Yellow Center
                g.fillOval(6, 3, 4, 5);
                g.setColor(new Color(0, 168, 0)); // Stem
                g.fillRect(7, 9, 2, 6);
                g.fillRect(4, 11, 8, 2); // Leaves
                break;

            case 3: // 1-Up Mushroom (Green)
                g.setColor(new Color(0, 180, 0)); // Green Cap
                g.fillRoundRect(2, 1, 12, 9, 4, 4);
                g.setColor(Color.WHITE); // Spots
                g.fillRect(6, 2, 4, 4);
                g.fillRect(2, 4, 2, 3);
                g.fillRect(12, 4, 2, 3);
                g.setColor(new Color(252, 216, 168)); // Stem Face
                g.fillRect(4, 9, 8, 6);
                g.setColor(Color.BLACK); // Eyes
                g.fillRect(5, 10, 1, 3);
                g.fillRect(10, 10, 1, 3);
                break;

            case 4: // Boo Ghost
                g.setColor(Color.WHITE); // Body
                g.fillOval(2, 2, 12, 11);
                g.fillOval(1, 5, 4, 5); // Left arm
                g.setColor(Color.BLACK); // Eyes & Mouth
                g.fillRect(8, 4, 2, 2);
                g.fillRect(8, 8, 4, 2);
                g.setColor(new Color(252, 100, 140)); // Tongue
                g.fillRect(10, 9, 3, 2);
                break;

            case 5: // Yoshi Egg
                g.setColor(Color.WHITE); // Egg
                g.fillOval(3, 1, 10, 14);
                g.setColor(new Color(0, 180, 0)); // Spots
                g.fillOval(5, 4, 4, 4);
                g.fillOval(4, 9, 3, 3);
                g.fillOval(9, 8, 3, 3);
                break;

            case 6: // Super Leaf
                g.setColor(new Color(180, 80, 0)); // Brown Leaf
                g.fillOval(2, 2, 12, 12);
                g.setColor(new Color(252, 180, 80));
                g.fillRect(7, 3, 2, 10);
                g.fillRect(4, 7, 8, 2);
                break;

            default: // Golden Coin
                g.setColor(new Color(252, 216, 0)); // Gold Rim
                g.fillOval(4, 1, 8, 14);
                g.setColor(new Color(252, 240, 120)); // Highlight
                g.fillRect(6, 3, 4, 10);
                g.setColor(new Color(180, 130, 0)); // Inner Groove
                g.drawRect(6, 4, 3, 8);
                break;
        }

        g.dispose();
        return scale(img, size);
    }

    /** Pac-Man Chomp Head Sprite with mouth open/close angle */
    public static BufferedImage createPacman(int size, float mouthAngle, boolean facingRight) {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color pacYellow = new Color(255, 238, 0);
        Color pacEye = new Color(20, 15, 30);

        int startAngle;
        int arcAngle;
        int m = (int)Math.max(10, Math.min(65, mouthAngle));

        if (facingRight) {
            startAngle = m / 2;
            arcAngle = 360 - m;
        } else {
            startAngle = 180 + m / 2;
            arcAngle = 360 - m;
        }

        // Body arc
        g.setColor(pacYellow);
        g.fillArc(2, 2, 28, 28, startAngle, arcAngle);

        // Eye
        int eyeX = facingRight ? 18 : 11;
        int eyeY = 8;
        g.setColor(pacEye);
        g.fillOval(eyeX, eyeY, 4, 4);

        g.dispose();
        return (size == 32) ? img : scale(img, size);
    }

    /** Iconic Pac-Man Ghost (Blinky, Pinky, Inky, Clyde) */
    public static BufferedImage createPacmanGhost(int size, int ghostType) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        Color bodyColor;
        switch (ghostType % 4) {
            case 0: bodyColor = new Color(255, 0, 0); break;      // Blinky (Red)
            case 1: bodyColor = new Color(255, 184, 255); break;  // Pinky (Pink)
            case 2: bodyColor = new Color(0, 255, 255); break;    // Inky (Cyan)
            default: bodyColor = new Color(255, 184, 82); break;  // Clyde (Orange)
        }

        // Dome Head
        g.setColor(bodyColor);
        g.fillRoundRect(2, 1, 12, 12, 6, 6);
        // Frill Skirt
        g.fillRect(2, 11, 3, 4);
        g.fillRect(6, 11, 4, 3);
        g.fillRect(11, 11, 3, 4);

        // Eyes (White Sclera)
        g.setColor(Color.WHITE);
        g.fillRect(4, 4, 3, 4);
        g.fillRect(9, 4, 3, 4);

        // Pupils (Deep Blue)
        g.setColor(new Color(0, 0, 180));
        g.fillRect(5, 5, 2, 2);
        g.fillRect(10, 5, 2, 2);

        g.dispose();
        return scale(img, size);
    }

    /** Pac-Man Arcade Cherry Fruit */
    public static BufferedImage createPacmanCherry(int size) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Stems (Green & Brown)
        g.setColor(new Color(130, 90, 40));
        g.drawLine(7, 2, 11, 6);
        g.drawLine(11, 6, 13, 10);
        g.drawLine(7, 2, 4, 8);
        g.setColor(new Color(50, 205, 50));
        g.fillRect(7, 1, 2, 2); // Leaf

        // Two Cherries (Vibrant Red)
        g.setColor(new Color(255, 0, 36));
        g.fillOval(2, 8, 6, 6);
        g.fillOval(8, 9, 6, 6);

        // Highlights
        g.setColor(Color.WHITE);
        g.fillRect(3, 9, 2, 1);
        g.fillRect(9, 10, 2, 1);

        g.dispose();
        return scale(img, size);
    }

    /** Retro Overcooked Head Chef Sprite (The CPU) */
    public static BufferedImage createChefSprite(int size, boolean isChopping) {
        BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Tall White Toque (Chef Hat)
        g.setColor(Color.WHITE);
        g.fillRoundRect(6, 1, 12, 8, 4, 4);
        g.fillRect(8, 7, 8, 3); // Hat brim

        // Face & Hair
        g.setColor(new Color(255, 206, 158));
        g.fillRoundRect(7, 9, 10, 7, 2, 2);
        // Eyes
        g.setColor(Color.BLACK);
        g.fillRect(9, 11, 2, 2);
        g.fillRect(13, 11, 2, 2);
        // Mustache
        g.setColor(new Color(110, 60, 20));
        g.fillRect(8, 14, 8, 2);

        // Chef White Uniform & Red Neckerchief
        g.setColor(new Color(228, 40, 40)); // Red Neckerchief
        g.fillRect(10, 15, 4, 2);
        g.setColor(Color.WHITE); // Double-breasted jacket
        g.fillRect(7, 16, 10, 6);
        g.setColor(new Color(30, 30, 40)); // Buttons
        g.fillRect(9, 17, 1, 1);
        g.fillRect(14, 17, 1, 1);
        g.fillRect(9, 19, 1, 1);
        g.fillRect(14, 19, 1, 1);

        // Arms & Spatula / Chopping Knife
        g.setColor(Color.WHITE);
        if (isChopping) {
            g.fillRect(4, 17, 4, 3); // Left arm raised
            g.fillRect(16, 15, 4, 3); // Right arm with knife down
            // Silver Kitchen Knife
            g.setColor(new Color(220, 225, 235));
            g.fillRect(18, 18, 5, 2);
            g.setColor(new Color(100, 50, 20));
            g.fillRect(17, 17, 2, 2);
        } else {
            g.fillRect(4, 17, 3, 4);
            g.fillRect(17, 17, 3, 4);
            // Spatula
            g.setColor(new Color(200, 205, 215));
            g.fillRect(19, 13, 3, 4);
            g.setColor(new Color(100, 50, 20));
            g.fillRect(19, 17, 2, 4);
        }

        // Trousers
        g.setColor(new Color(40, 50, 80));
        g.fillRect(8, 22, 3, 2);
        g.fillRect(13, 22, 3, 2);

        g.dispose();
        return scale(img, size);
    }

    /** Retro Kitchen Stove / Oven Sprite (The I/O Device) */
    public static BufferedImage createStoveSprite(int size, boolean flameOn) {
        BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Stainless Steel Stove Body
        g.setColor(new Color(90, 85, 110));
        g.fillRoundRect(2, 4, 20, 19, 3, 3);
        g.setColor(new Color(45, 40, 60));
        g.fillRect(4, 6, 16, 4); // Range top

        // Burner Coil
        g.setColor(new Color(25, 20, 35));
        g.fillOval(5, 7, 6, 3);
        g.fillOval(13, 7, 6, 3);

        if (flameOn) {
            // Animated Gas Burner Flames (Blue & Orange)
            g.setColor(new Color(0, 200, 255));
            g.fillOval(6, 6, 4, 2);
            g.fillOval(14, 6, 4, 2);
            g.setColor(new Color(255, 120, 0));
            g.fillOval(7, 5, 2, 2);
            g.fillOval(15, 5, 2, 2);
        }

        // Oven Window
        g.setColor(new Color(20, 15, 30));
        g.fillRect(5, 12, 14, 8);
        if (flameOn) {
            // Warm Orange Oven Interior Glow
            g.setColor(new Color(255, 150, 30, 180));
            g.fillRect(6, 13, 12, 6);
        }

        // Handle
        g.setColor(new Color(180, 180, 200));
        g.fillRect(7, 10, 10, 2);

        g.dispose();
        return scale(img, size);
    }

    /** Pixel Dish Sprites for Orders */
    public static BufferedImage createDishSprite(int type, int size) {
        BufferedImage img = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        int t = Math.abs(type) % 6;
        switch (t) {
            case 0: // Burger
                g.setColor(new Color(225, 150, 60)); // Top Bun
                g.fillRoundRect(3, 3, 14, 5, 4, 4);
                g.setColor(new Color(80, 180, 40)); // Lettuce
                g.fillRect(2, 8, 16, 2);
                g.setColor(new Color(255, 200, 20)); // Melted Cheese
                g.fillRect(3, 9, 14, 2);
                g.setColor(new Color(110, 45, 20)); // Beef Patty
                g.fillRect(3, 11, 14, 3);
                g.setColor(new Color(210, 140, 50)); // Bottom Bun
                g.fillRoundRect(3, 14, 14, 3, 2, 2);
                break;

            case 1: // Pizza Slice
                Polygon pz = new Polygon();
                pz.addPoint(10, 2);
                pz.addPoint(2, 16);
                pz.addPoint(18, 16);
                g.setColor(new Color(255, 200, 30)); // Melted Cheese
                g.fillPolygon(pz);
                g.setColor(new Color(180, 100, 40)); // Crust
                g.fillRect(2, 15, 16, 3);
                g.setColor(new Color(210, 30, 30)); // Pepperoni
                g.fillOval(8, 7, 4, 4);
                g.fillOval(5, 11, 3, 3);
                g.fillOval(12, 11, 3, 3);
                break;

            case 2: // Ramen Bowl
                g.setColor(new Color(210, 40, 40)); // Red Ceramic Bowl
                g.fillArc(2, 6, 16, 12, 180, 180);
                g.fillRect(7, 17, 6, 2); // Base
                g.setColor(new Color(250, 210, 80)); // Noodles
                g.fillOval(3, 6, 14, 4);
                g.setColor(Color.WHITE); // Boiled Egg
                g.fillOval(6, 7, 5, 4);
                g.setColor(new Color(255, 170, 0)); // Yolk
                g.fillOval(8, 8, 2, 2);
                g.setColor(new Color(40, 140, 40)); // Green Onions
                g.fillRect(12, 7, 2, 2);
                break;

            case 3: // Sushi Rolls
                g.setColor(new Color(25, 40, 30)); // Nori Seaweed
                g.fillRoundRect(3, 6, 14, 10, 4, 4);
                g.setColor(Color.WHITE); // Rice
                g.fillRoundRect(5, 7, 10, 8, 3, 3);
                g.setColor(new Color(255, 100, 60)); // Salmon center
                g.fillOval(8, 9, 4, 4);
                g.setColor(new Color(60, 180, 50)); // Cucumber
                g.fillRect(7, 10, 2, 2);
                break;

            case 4: // Steak Platter
                g.setColor(new Color(60, 60, 75)); // Sizzling Iron Plate
                g.fillOval(2, 7, 16, 10);
                g.setColor(new Color(120, 50, 30)); // Grilled Steak
                g.fillOval(4, 8, 12, 7);
                g.setColor(new Color(60, 20, 10)); // Grill Marks
                g.drawLine(6, 10, 9, 13);
                g.drawLine(10, 10, 13, 13);
                g.setColor(new Color(255, 220, 60)); // Butter Pat
                g.fillRect(9, 10, 3, 2);
                break;

            case 5: // Strawberry Cake
                g.setColor(new Color(255, 230, 210)); // Sponge Cake
                g.fillRect(4, 7, 12, 10);
                g.setColor(new Color(255, 120, 150)); // Strawberry Frosting
                g.fillRect(3, 5, 14, 4);
                g.setColor(new Color(255, 30, 60)); // Strawberry on top
                g.fillOval(8, 2, 4, 4);
                break;
        }

        g.dispose();
        return scale(img, size);
    }

    /** 8-bit Cyber Frog / Hopper Packet Sprite */
    public static BufferedImage createFrogSprite(int size, Color c) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // Eyes
        g.setColor(c);
        g.fillRect(2, 1, 3, 3);
        g.fillRect(11, 1, 3, 3);
        // Pupils
        g.setColor(Color.WHITE);
        g.fillRect(3, 2, 1, 1);
        g.fillRect(12, 2, 1, 1);
        // Head & Body
        g.setColor(c);
        g.fillRect(4, 3, 8, 3);
        g.fillRect(3, 6, 10, 5);
        g.fillRect(4, 11, 8, 2);
        // Front feet
        g.fillRect(1, 4, 2, 3);
        g.fillRect(13, 4, 2, 3);
        // Back legs & feet
        g.fillRect(1, 9, 2, 4);
        g.fillRect(13, 9, 2, 4);
        g.fillRect(0, 13, 3, 2);
        g.fillRect(13, 13, 3, 2);
        // Neon cyber chest core
        g.setColor(new Color(5, 217, 232));
        g.fillRect(7, 7, 2, 3);

        g.dispose();
        return scale(img, size);
    }

    /** 8-bit IC Microchip / Interrupt Hazard Sprite */
    public static BufferedImage createChipHazardSprite(int size, Color c) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Chip body
        g.setColor(c);
        g.fillRect(3, 3, 10, 10);
        // IC Pins on left & right
        g.setColor(new Color(220, 220, 240));
        g.fillRect(1, 4, 2, 1);
        g.fillRect(1, 7, 2, 1);
        g.fillRect(1, 10, 2, 1);
        g.fillRect(13, 4, 2, 1);
        g.fillRect(13, 7, 2, 1);
        g.fillRect(13, 10, 2, 1);
        // Pin 1 indicator dot
        g.setColor(Color.YELLOW);
        g.fillRect(4, 4, 2, 2);

        g.dispose();
        return scale(img, size);
    }

    /** 16-bit User Profile Icon (Pixel Art) */
    public static BufferedImage createUserIcon(int size, Color c) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        // Head
        g.fillRect(6, 2, 4, 4);
        // Neck
        g.fillRect(7, 6, 2, 2);
        // Shoulders / Torso
        g.fillRect(3, 8, 10, 6);
        g.dispose();
        return scale(img, size);
    }

    /** 16-bit Trophy / Leaderboard Icon (Pixel Art) */
    public static BufferedImage createTrophyIcon(int size, Color c) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        // Cup Rim & Body
        g.fillRect(4, 2, 8, 2);
        g.fillRect(5, 4, 6, 4);
        g.fillRect(6, 8, 4, 2);
        // Handles
        g.fillRect(2, 3, 2, 4);
        g.fillRect(12, 3, 2, 4);
        // Stem
        g.fillRect(7, 10, 2, 3);
        // Base
        g.fillRect(4, 13, 8, 2);
        g.dispose();
        return scale(img, size);
    }

    /** 16-bit Power / Logout Icon (Pixel Art) */
    public static BufferedImage createPowerIcon(int size, Color c) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        // Outer circular ring with top opening
        g.fillRect(4, 3, 2, 8);
        g.fillRect(10, 3, 2, 8);
        g.fillRect(4, 11, 8, 2);
        // Center vertical switch line
        g.setColor(Color.WHITE);
        g.fillRect(7, 2, 2, 6);
        g.dispose();
        return scale(img, size);
    }

    /** Voltage Glitch / Lightning Hazard Sprite */
    public static BufferedImage createGlitchHazardSprite(int width, int height, Color c) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        // Jagged lightning pulse
        int midY = height / 2;
        int[] xs = { 2, width / 3, width / 2 - 2, 2 * width / 3, width - 4, 2 * width / 3 - 2, width / 2 + 2, width / 3 - 2 };
        int[] ys = { midY, 2, midY + 2, 2, midY, height - 3, midY - 2, height - 3 };
        g.fillPolygon(xs, ys, 8);
        // White core highlight
        g.setColor(Color.WHITE);
        g.fillRect(width / 3, midY - 1, width / 3, 3);
        g.dispose();
        return img;
    }

    /** Heavy DMA / I-O Transport Truck Hazard Sprite */
    public static BufferedImage createTruckHazardSprite(int width, int height, Color c) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        // Main trailer body
        g.fillRect(2, 3, width - 14, height - 8);
        // Cab front
        g.setColor(new Color(255, 120, 60));
        g.fillRect(width - 12, 6, 10, height - 11);
        // Windshield
        g.setColor(new Color(180, 240, 255));
        g.fillRect(width - 8, 7, 5, 4);
        // Wheels
        g.setColor(new Color(220, 220, 230));
        g.fillRect(6, height - 4, 6, 4);
        g.fillRect(width - 12, height - 4, 6, 4);
        g.dispose();
        return img;
    }

    private static BufferedImage scale(BufferedImage src, int size) {
        BufferedImage dest = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, size, size, null);
        g.dispose();
        return dest;
    }
}
