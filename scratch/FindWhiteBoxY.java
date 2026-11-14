package scratch;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class FindWhiteBoxY {
    public static void main(String[] args) throws Exception {
        BufferedImage full = ImageIO.read(new File(
                "C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/.user_uploaded/media_1794485599706_ae523408.jpg"));
        for (int y = 150; y < 650; y += 10) {
            int whites = 0;
            int minX = 9999, maxX = -1;
            for (int x = 200; x < full.getWidth() - 20; x++) {
                int rgb = full.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                if (r > 210 && g > 210 && b > 210) {
                    whites++;
                    if (x < minX)
                        minX = x;
                    if (x > maxX)
                        maxX = x;
                }
            }
            if (whites > 1) {
                System.out.printf("y=%d whites=%d minX=%d maxX=%d width=%d%n", y, whites, minX, maxX, (maxX - minX));
            }
        }
    }
}
