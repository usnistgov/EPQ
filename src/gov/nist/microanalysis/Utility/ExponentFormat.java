package gov.nist.microanalysis.Utility;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 * <p>
 * Implements a number format that outputs in HTML as #.## x 10<sup>#</sup>.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nritchie
 * @version 1.0
 */
public class ExponentFormat extends NumberFormat {

   private static final long serialVersionUID = -500777501322251143L;

   private static final String FMT2 = "</sup>";
   private static final String FMT1 = "&middot;10<sup>";
   private final int mPlaces;
   private final NumberFormat mFormatter;

   public ExponentFormat(int places) {
      super();
      assert places > 0;
      mPlaces = Math.max(1, places);
      final StringBuffer sb = new StringBuffer();
      sb.append("0.");
      for (int i = 1; i < mPlaces; ++i)
         sb.append("0");
      mFormatter = new HalfUpFormat(sb.toString());
   }

   /**
    * @param arg0
    *           number
    * @param arg1
    *           output buffer
    * @param arg2
    *           current position
    * @return StringBuffer
    * @see java.text.NumberFormat#format(double, java.lang.StringBuffer,
    *      java.text.FieldPosition)
    */
   @Override
   public StringBuffer format(double arg0, StringBuffer arg1, FieldPosition arg2) {
      final String sign = Math.signum(arg0) >= 0 ? "" : "-";
      final double la = Math.log10(Math.abs(arg0));
      final int exp = la > 0.0 ? (int) la : (((int) la) - 1);
      final String tmp = "<nobr>" + (sign + (exp != 0
            ? mFormatter.format(Math.pow(10.0, la - exp)) + FMT1 + Integer.toString(exp) + FMT2
            : mFormatter.format(Math.pow(10.0, la - exp)))) + "</nobr>";
      arg2.setBeginIndex(arg1.length());
      arg1.append(tmp);
      arg2.setEndIndex(arg1.length());
      return arg1;
   }

   /**
    * @param arg0
    *           number
    * @param arg1
    *           output buffer
    * @param arg2
    *           current position
    * @return StringBuffer
    * @see java.text.NumberFormat#format(long, java.lang.StringBuffer,
    *      java.text.FieldPosition)
    */
   @Override
   public StringBuffer format(long arg0, StringBuffer arg1, FieldPosition arg2) {
      return format((double) arg0, arg1, arg2);
   }

   /**
    * @param arg0
    *           String
    * @param arg1
    *           position
    * @return Number
    * @see java.text.NumberFormat#parse(java.lang.String,
    *      java.text.ParsePosition)
    */
   @Override
   public Number parse(String arg0, ParsePosition arg1) {
      final Number tmp = mFormatter.parse(arg0, arg1);
      if (tmp != null) {
         final int sup = arg0.indexOf(FMT1, arg1.getIndex()) + FMT1.length();
         final int esup = arg0.indexOf(FMT2, sup);
         if ((sup >= arg1.getIndex()) && (esup > sup))
            try {
               final int exp = Integer.parseInt(arg0.substring(sup, esup));
               arg1.setIndex(esup + FMT2.length());
               return Double.valueOf(tmp.doubleValue() * Math.pow(10.0, exp));
            } catch (final NumberFormatException e) {
               return tmp;
            }
      }
      return tmp;
   }

}
