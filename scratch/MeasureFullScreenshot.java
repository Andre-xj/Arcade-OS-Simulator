package scratch;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MeasureFullScreenshot {
    public static void main(String[] args) throws Exception {
        BufferedImage full = ImageIO.read(new File("C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/.user_uploaded/media_1794485599706_ae523408.jpg"));
        System.out.println("Full screenshot size: " + full.getWidth() + "x" + full.getHeight());
        
        // Find the white box left and right edges at row y = 200
        int whiteLeft = -1;
        int whiteRight = -1;
        for (int x = 0; x < full.getWidth(); x++) {
            int rgb = full.getRGB(x, 200);
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            if (r > 200 && g > 200 && b > 200) {
                if (whiteLeft == -1) whiteLeft = x;
                whiteRight = x;
            }
        }
        System.out.println("White box in full image: left=" + whiteLeft + ", right=" + whiteRight + " -> width=" + (whiteRight - whiteLeft));
        
        // Find inner box left and right edges
        int innerLeft = -1;
        int innerRight = -1;
        for (int x = whiteLeft + 2; x < whiteRight - 2; x++) {
            int rgb = full.getRGB(x, 200);
            int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
            if ((r > 80 && b > 140) || (r > 150 && g > 100)) {
                if (innerLeft == -1) innerLeft = x;
                innerRight = x;
            }
        }
        System.out.println("Inner box in full image: left=" + innerLeft + ", right=" + innerRight + " -> width=" + (innerRight - innerLeft));
        System.out.println("Left gap: " + (innerLeft - whiteLeft) + ", Right gap: " + (whiteRight - innerRight));
    }
}

