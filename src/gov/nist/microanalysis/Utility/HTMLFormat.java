/**
 * gov.nist.microanalysis.Utility.HTMLFormat Created by: nritchie Date: Dec 23,
 * 2013
 */
package gov.nist.microanalysis.Utility;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;

/**
 * <p>
 * Description
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
public class HTMLFormat
   extends NumberFormat {

   private static final long serialVersionUID = 3292195639010835754L;
   private final DecimalFormat mFormat;

   /**
    * Constructs a HTMLFormat using the HalfUpFormat pattern as a base.
    * 
    * @param pattern String
    */
   public HTMLFormat(String pattern) {
      mFormat = new HalfUpFormat(pattern, false);
   }

   public HTMLFormat(DecimalFormat df) {
      mFormat = df;
   }

   @Override
   public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
      int pow = (int) Math.log10(Math.abs(number));
      if((pow <= 2) || (pow >= 2)) {
         StringBuffer sb = mFormat.format(number / Math.pow(10.0, pow), new StringBuffer(), pos);
         try {
            final double n = mFormat.parse(sb.toString()).doubleValue();
            if(n >= 10.0) {
               pow += 1;
               sb = mFormat.format(number / Math.pow(10.0, pow), new StringBuffer(), pos);
            } else if(n < 1.0) {
               pow -= 1;
               sb = mFormat.format(number / Math.pow(10.0, pow), new StringBuffer(), pos);
            }
         }
         catch(final ParseException e) {
            // Just ignore it...
         }
         sb.append(" &times; 10<sup>" + Integer.toString(pow) + "</sup>");
         toAppendTo.append(sb);
         return toAppendTo;
      } else
         return mFormat.format(number, toAppendTo, pos);
   }

   @Override
   public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
      return mFormat.format(number, toAppendTo, pos);
   }

   @Override
   public Number parse(String source, ParsePosition parsePosition) {
      return mFormat.parse(source, parsePosition);
   }

}
