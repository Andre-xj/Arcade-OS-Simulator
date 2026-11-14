package scratch;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class CheckImageProps {
    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/.user_uploaded/media_1794485599706_ae523408.jpg"));
        System.out.println("Image size: " + img.getWidth() + "x" + img.getHeight());
    }
}

