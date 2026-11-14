package scratch;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ScanColors {
    public static void main(String[] args) throws Exception {
        BufferedImage img1 = ImageIO.read(new File("C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/.user_uploaded/media_1794485619442_f5105ac5.png"));
        BufferedImage img2 = ImageIO.read(new File("C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/.user_uploaded/media_1794485627311_9c739ba9.png"));
        
        System.out.println("img1 (left) row 40:");
        for (int x = 0; x < img1.getWidth(); x++) {
            int rgb = img1.getRGB(x, 40);
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            if (r > 60 || g > 60 || b > 60) {
                System.out.printf("x=%d rgb=(%d,%d,%d) ", x, r, g, b);
            }
        }
        System.out.println("\nimg2 (right) row 40:");
        for (int x = 0; x < img2.getWidth(); x++) {
            int rgb = img2.getRGB(x, 40);
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            if (r > 60 || g > 60 || b > 60) {
                System.out.printf("x=%d rgb=(%d,%d,%d) ", x, r, g, b);
            }
        }
        System.out.println();
    }
}

