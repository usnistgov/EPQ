package gov.nist.microanalysis.EPQTools;

import java.awt.Color;
import java.awt.SystemColor;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 * <p>
 * Some functions to make for a more consistent user interface.
 * </p>
 *
 * @author Nicholas
 * @version 1.0
 */
public class SwingUtils {

   private final static Color sLineColor = createLineColor();

   private static final int balance(int i1, int i2) {
      return (i1 + 3 * i2) / 4;
   }

   private static final Color createLineColor() {
      final Color c1 = SystemColor.controlShadow;
      final Color c2 = SystemColor.control;
      return new Color(balance(c1.getRed(), c2.getRed()), balance(c1.getGreen(), c2.getGreen()), balance(c1.getBlue(), c2.getBlue()), balance(c1.getAlpha(), c2.getAlpha()));
   }

   public static TitledBorder createTitledBorder(String name) {
      return BorderFactory.createTitledBorder(createEmptyBorder(), name);
   }

   public static Border createDefaultBorder() {
      return BorderFactory.createMatteBorder(1, 1, 1, 1, sLineColor);
   }

   public static Border createEmptyBorder() {
      return BorderFactory.createEmptyBorder(5, 5, 5, 5);
   }
}
