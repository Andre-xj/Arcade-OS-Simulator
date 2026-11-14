package scratch;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.text.*;

public class TestCustomInsetsPane {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            frame.setSize(600, 400);
            
            JTextPane pane = new JTextPane() {
                @Override
                public boolean getScrollableTracksViewportWidth() {
                    return true;
                }
                
                @Override
                public Insets getInsets() {
                    Font f = new Font("Consolas", Font.PLAIN, 12);
                    FontMetrics fm = getFontMetrics(f);
                    int textW = fm.stringWidth("╔════════════════════════════════════════════════════════════════════════╗");
                    int w = getWidth();
                    int hMargin = Math.max(10, (w - textW) / 2);
                    return new Insets(16, hMargin, 16, hMargin);
                }

                @Override
                public Insets getInsets(Insets in) {
                    Insets dyn = getInsets();
                    in.top = dyn.top;
                    in.left = dyn.left;
                    in.bottom = dyn.bottom;
                    in.right = dyn.right;
                    return in;
                }
            };
            pane.setFont(new Font("Consolas", Font.PLAIN, 12));
            pane.setText("╔════════════════════════════════════════════════════════════════════════╗\n" +
                          "║ TEST CENTERED BOX                                                      ║\n" +
                          "╚════════════════════════════════════════════════════════════════════════╝");

            JScrollPane sp = new JScrollPane(pane);
            frame.add(sp);
            frame.setVisible(true);

            // Let's check measurements once displayed
            SwingUtilities.invokeLater(() -> {
                int w = pane.getWidth();
                Insets in = pane.getInsets();
                FontMetrics fm = pane.getFontMetrics(new Font("Consolas", Font.PLAIN, 12));
                int textW = fm.stringWidth("╔════════════════════════════════════════════════════════════════════════╗");
                int leftGap = in.left;
                int rightGap = w - (in.left + textW);
                System.out.println("Width=" + w + ", TextW=" + textW + ", LeftGap=" + leftGap + ", RightGap=" + rightGap);
                System.out.println("Are gaps equal? " + (leftGap == rightGap || Math.abs(leftGap - rightGap) <= 1));
                frame.dispose();
            });
        });
    }
}

