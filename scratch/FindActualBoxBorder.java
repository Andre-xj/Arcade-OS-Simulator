package scratch;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class FindActualBoxBorder {
    public static void main(String[] args) throws Exception {
        BufferedImage full = ImageIO.read(new File("C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/.user_uploaded/media_1794485599706_ae523408.jpg"));
        
        // Let's find minX and maxX of the purple box lines across rows 230 to 270
        int minBoxX = 9999, maxBoxX = -1;
        for (int y = 230; y < 270; y++) {
            for (int x = 335; x < 990; x++) {
                int rgb = full.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                // Color is approx (115, 95, 175) or purple tint: b > r && r > 70 && b > 120
                if (b > r && r > 70 && b > 120 && g > 60 && g < 130) {
                    if (x < minBoxX) minBoxX = x;
                    if (x > maxBoxX) maxBoxX = x;
                }
            }
        }
        System.out.println("Box outline: minX=" + minBoxX + ", maxBoxX=" + maxBoxX + " -> Box width=" + (maxBoxX - minBoxX));
        System.out.println("White box: minX=333, maxX=993 -> White box width=660");
        int leftGap = minBoxX - 333;
        int rightGap = 993 - maxBoxX;
        System.out.println("Left gap=" + leftGap + ", Right gap=" + rightGap);
        System.out.println("Difference=" + (rightGap - leftGap));
    }
}

