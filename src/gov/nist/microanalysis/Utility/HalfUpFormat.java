package gov.nist.microanalysis.Utility;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

/**
 * Deals with the annoying default rounding scheme used by DecimalForamt and
 * implements the NIST suggested space grouping scheme.
 * 
 * @author nicholas
 */
public class HalfUpFormat extends DecimalFormat {

   private static final long serialVersionUID = -2503012670987467760L;
   private final static char MEDIUM_MATHEMATICAL_SPACE = '\u2006';

   /**
    * Like DecimalFormat but rounds up
    */
   public HalfUpFormat() {
      this(false);
   }

   /**
    * Rounds up and uses a small space for grouping (if spaceGrouping is true)
    * 
    * @param spaceGrouping
    *           If true uses small space for grouping in units of 3
    */
   public HalfUpFormat(boolean spaceGrouping) {
      super();
      if (spaceGrouping) {
         final DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
         dfs.setGroupingSeparator(MEDIUM_MATHEMATICAL_SPACE);
         setDecimalFormatSymbols(dfs);
         this.setGroupingSize(3);
         this.setGroupingUsed(true);
      }
      setRoundingMode(RoundingMode.HALF_UP);

   }

   /**
    * Like DecimalFormat but rounds up
    * 
    * @param pattern
    *           String
    */
   public HalfUpFormat(String pattern) {
      this(pattern, false);
   }

   /**
    * Like DecimalFormat but rounds up
    * 
    * @param pattern
    *           String
    * @param spaceGrouping
    *           boolean
    */
   public HalfUpFormat(String pattern, boolean spaceGrouping) {
      super(pattern);
      if (spaceGrouping) {
         final DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
         dfs.setGroupingSeparator(MEDIUM_MATHEMATICAL_SPACE);
         setDecimalFormatSymbols(dfs);
         this.setGroupingSize(3);
         this.setGroupingUsed(true);
      }
      setRoundingMode(RoundingMode.HALF_UP);
   }

   /**
    * Like DecimalFormat but rounds up
    * 
    * @param pattern
    *           {@link String}
    * @param symbols
    *           {@link DecimalFormatSymbols}
    */
   public HalfUpFormat(String pattern, DecimalFormatSymbols symbols) {
      super(pattern, symbols);
      setRoundingMode(RoundingMode.HALF_UP);
   }

   /**
    * Adaptively format the number such that the specified number is output
    * using HalfUpFormating with the specified precision. The format is
    * adaptively built to ensure that only precision significant digits are
    * displayed and that the output mode switches to scientific when the
    * absolute value is larger than sciRange or the absolute value is less than
    * 1.0/sciRange.
    * 
    * @param number
    *           The number to format
    * @param precision
    *           &gt;0 A number of digits of precision to use
    * @param sciRange
    *           Nominally 1.0e6 or so
    * @return The number formated appropriately in a String.
    */
   static public String adaptiveFormat(double number, int precision, double sciRange) {
      precision = Math.max(precision, 1);
      sciRange = Math.abs(sciRange);
      final StringBuffer fmt = new StringBuffer();
      if ((Math.abs(number) > sciRange) || (Math.abs(number) < (1.0 / sciRange))) {
         fmt.append("0.");
         for (int i = 1; i < precision; ++i)
            fmt.append("0");
         fmt.append("E0");
         return (new HalfUpFormat(fmt.toString(), true)).format(number);
      } else {
         final int nn = (int) Math.floor(Math.log10(Math.abs(number))) + 1;
         if (nn >= precision) {
            for (int i = 0; i < nn; ++i)
               fmt.append("0");
            final NumberFormat huf = new HalfUpFormat(fmt.toString(), true);
            final double div = Math.pow(10.0, nn - precision);
            final double rounded = Math.round(number / div) * div;
            return huf.format(rounded);
         } else {
            for (int i = 0; i < Math.max(nn, 1); ++i)
               fmt.append("0");
            fmt.append(".");
            for (int i = nn; i < precision; ++i)
               fmt.append("0");
            return (new HalfUpFormat(fmt.toString(), true)).format(number);
         }
      }
   }
}
