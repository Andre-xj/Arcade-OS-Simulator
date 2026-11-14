package scratch;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MeasurePixelGaps {
    public static void main(String[] args) throws Exception {
        BufferedImage img1 = ImageIO.read(new File("C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/.user_uploaded/media_1794485619442_f5105ac5.png"));
        BufferedImage img2 = ImageIO.read(new File("C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/.user_uploaded/media_1794485627311_9c739ba9.png"));
        
        // In img1 (left crop), find the white vertical line (approx x=20-30) and the orange box border
        // Let's sample a horizontal scanline around y = 40 (where the orange box is)
        System.out.println("Scanline y=40 in img1:");
        int whiteX1 = -1;
        int orangeX1 = -1;
        for (int x = 0; x < img1.getWidth(); x++) {
            int rgb = img1.getRGB(x, 40);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            if (r > 200 && g > 200 && b > 200) {
                if (whiteX1 == -1) whiteX1 = x;
            }
            if (r > 200 && g > 100 && b < 50) { // Orange
                if (orangeX1 == -1) orangeX1 = x;
            }
        }
        System.out.println("img1: whiteX=" + whiteX1 + ", orangeX=" + orangeX1 + " -> GAP = " + (orangeX1 - whiteX1));

        System.out.println("Scanline y=40 in img2:");
        int orangeX2 = -1;
        int whiteX2 = -1;
        for (int x = 0; x < img2.getWidth(); x++) {
            int rgb = img2.getRGB(x, 40);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            if (r > 200 && g > 100 && b < 50) { // Orange
                orangeX2 = x; // right edge of orange
            }
            if (r > 200 && g > 200 && b > 200) { // White
                if (whiteX2 == -1) whiteX2 = x;
            }
        }
        System.out.println("img2: orangeX=" + orangeX2 + ", whiteX=" + whiteX2 + " -> GAP = " + (whiteX2 - orangeX2));
    }
}

