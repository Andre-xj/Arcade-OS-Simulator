package scratch;

import java.awt.*;
import javax.swing.*;

public class ViewportWidth {
    public static void main(String[] args) {
        // Window is 840px wide.
        // Dialog insets on Windows: left ~8, right ~8 (or 0 if undecorated/custom).
        // BorderLayout with left column 230px, right column fills the rest.
        // Let's create the exact panel structure from ArchiveManualDialog:
        JPanel content = new JPanel(new BorderLayout(14, 0));
        content.setBorder(BorderFactory.createEmptyBorder(0, 14, 10, 14));
        
        JPanel left = new JPanel();
        left.setPreferredSize(new Dimension(230, 0));
        
        JPanel right = new JPanel(new BorderLayout());
        JPanel centerCard = new JPanel(new BorderLayout(0, 6));
        JPanel cardContainer = new JPanel(new CardLayout());
        
        JTextPane pane = new JTextPane();
        JScrollPane sp = new JScrollPane(pane);
        sp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 40, 85), 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        cardContainer.add(sp, "TAB_0");
        centerCard.add(cardContainer, BorderLayout.CENTER);
        right.add(centerCard, BorderLayout.CENTER);
        
        content.add(left, BorderLayout.WEST);
        content.add(right, BorderLayout.CENTER);
        
        JFrame frame = new JFrame();
        frame.setSize(840, 580);
        frame.getContentPane().add(content);
        frame.doLayout();
        frame.validate();
        
        int spWidth = sp.getWidth();
        int viewportWidth = sp.getViewport().getWidth();
        System.out.println("sp width: " + spWidth);
        System.out.println("viewport width: " + viewportWidth);
        
        // Font text width:
        Font f = new Font("Consolas", Font.PLAIN, 12);
        FontMetrics fm = pane.getFontMetrics(f);
        int textWidth = fm.stringWidth("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("text width: " + textWidth);
        
        int totalRemaining = viewportWidth - textWidth;
        int equalSideMargin = totalRemaining / 2;
        System.out.println("Total remaining space: " + totalRemaining);
        System.out.println("Equal side margin should be: " + equalSideMargin);
        System.exit(0);
    }
}

