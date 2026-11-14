package scratch;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class FindBoxCoords {
    public static void main(String[] args) throws Exception {
        BufferedImage img1 = ImageIO.read(new File("C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/.user_uploaded/media_1794485619442_f5105ac5.png"));
        BufferedImage img2 = ImageIO.read(new File("C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/.user_uploaded/media_1794485627311_9c739ba9.png"));
        
        // Find white vertical line in img1:
        int whiteX1 = -1;
        for (int x = 0; x < img1.getWidth(); x++) {
            int rgb = img1.getRGB(x, 100);
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            if (r > 200 && g > 200 && b > 200) { whiteX1 = x; break; }
        }
        
        // Find leftmost pixel of the inner boxes (look for purple box border or orange text)
        int innerLeftX = 999;
        for (int y = 40; y < 140; y++) {
            for (int x = whiteX1 + 5; x < img1.getWidth(); x++) {
                int rgb = img1.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                // Look for purple border (approx 115, 95, 175) or text colors
                if ((r > 80 && b > 140) || (r > 150 && g > 100)) {
                    if (x < innerLeftX) innerLeftX = x;
                }
            }
        }
        System.out.println("img1: whiteX=" + whiteX1 + ", innerLeftX=" + innerLeftX + " -> LEFT GAP = " + (innerLeftX - whiteX1));
        
        // In img2 (right crop):
        int whiteX2 = -1;
        for (int x = 0; x < img2.getWidth(); x++) {
            int rgb = img2.getRGB(x, 100);
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            if (r > 200 && g > 200 && b > 200) { whiteX2 = x; break; }
        }
        
        int innerRightX = -1;
        for (int y = 40; y < 140; y++) {
            for (int x = 0; x < whiteX2 - 5; x++) {
                int rgb = img2.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                if ((r > 80 && b > 140) || (r > 150 && g > 100)) {
                    if (x > innerRightX) innerRightX = x;
                }
            }
        }
        System.out.println("img2: innerRightX=" + innerRightX + ", whiteX=" + whiteX2 + " -> RIGHT GAP = " + (whiteX2 - innerRightX));
    }
}

