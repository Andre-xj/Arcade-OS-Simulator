package scratch;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MeasureInnerBoxInFull {
    public static void main(String[] args) throws Exception {
        BufferedImage full = ImageIO.read(new File("C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/.user_uploaded/media_1794485599706_ae523408.jpg"));
        // The orange box is around y = 240
        int y = 250;
        int innerMinX = 9999, innerMaxX = -1;
        for (int x = 335; x < 990; x++) {
            int rgb = full.getRGB(x, y);
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            // Orange: r is high, g is medium, b is low
            if (r > 180 && g > 90 && b < 70) {
                if (x < innerMinX) innerMinX = x;
                if (x > innerMaxX) innerMaxX = x;
            }
        }
        System.out.println("Inner orange box: minX=" + innerMinX + ", maxX=" + innerMaxX);
        System.out.println("White box: minX=333, maxX=993");
        int leftGap = innerMinX - 333;
        int rightGap = 993 - innerMaxX;
        System.out.println("Left gap = " + leftGap + ", Right gap = " + rightGap);
        System.out.println("Difference = " + (rightGap - leftGap));
    }
}

