package scratch;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class InspectCrop {
    public static void main(String[] args) throws Exception {
        File f1 = new File("C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/.user_uploaded/media_1794485619442_f5105ac5.png");
        File f2 = new File("C:/Users/john andre/.gemini/antigravity/brain/84cbe9aa-6ef5-44fe-8435-41999c82e1a2/.user_uploaded/media_1794485627311_9c739ba9.png");
        
        BufferedImage img1 = ImageIO.read(f1);
        BufferedImage img2 = ImageIO.read(f2);
        
        System.out.println("Image 1 (Left crop) size: " + img1.getWidth() + "x" + img1.getHeight());
        System.out.println("Image 2 (Right crop) size: " + img2.getWidth() + "x" + img2.getHeight());
    }
}

