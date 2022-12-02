package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;

import java.text.NumberFormat;

/**
 * A abstract base class defining the structure for fitting filters for filter
 * fitting spectra. Also contained within are a handful of concrete
 * implementations of this abstract base class.
 * 
 * @author nicholas
 */
abstract public class FittingFilter {

   public static double defaultVarianceCorrectionFactor(int lw, int uw) {
      // Verified with Fred Schamber on 2-Sep-2008
      return (2.0 * uw * lw) / (uw + (2.0 * lw));
   }

   protected double[] mFilter;
   protected double mVarCorrection;
   protected String mName;
   protected final double mWidth;
   protected final double mChannelWidth;

   protected FittingFilter(double width, double chWidth) {
      mWidth = width;
      mChannelWidth = chWidth;
   }

   public boolean zeroSum() {
      return Math.abs(Math2.sum(mFilter)) < 1.0e-6;
   }

   public double[] getFilter() {
      return mFilter.clone();
   }

   /**
    * Returns the width of the filter in electron volts.
    * 
    * @return full width of filter in electron volts
    */
   public double getFilterWidth() {
      return mChannelWidth * mFilter.length;
   }

   @Override
   public String toString() {
      return mName;
   }

   public double varianceCorrectionFactor() {
      return mVarCorrection;
   }

   /**
    * Standard top-hat style filter (as per Schamber)
    * 
    * @author nicholas
    */
   static public class TopHatFilter extends FittingFilter {

      public TopHatFilter(double width, double chWidth) {
         super(width, chWidth);
         // (2*m+1)*channelWidth ~ mFilterWidth
         final int m = Math.max(((int) Math.round(width / chWidth) - 1) / 2, 2);
         final int n = m;
         final int filterLen = (2 * n) + (2 * m) + 1;
         mFilter = new double[filterLen];
         for (int i = 0; i < n; ++i) {
            mFilter[i] = -1.0;
            mFilter[i + n + (2 * m) + 1] = -1.0;
         }
         final double k = (2.0 * n) / ((2.0 * m) + 1.0);
         for (int i = 0; i < ((2 * m) + 1); ++i)
            mFilter[i + n] = k;
         mVarCorrection = defaultVarianceCorrectionFactor(m, 2 * m + 1);
         mName = "TopHat[m=" + m + ", n=" + n + "]";
      }
   }

   /**
    * Filter based on a Gaussian shape offset to give zero sum.
    * 
    * @author nicholas
    */
   static public class GaussianFilter extends FittingFilter {

      public GaussianFilter(double width, double chWidth) {
         super(width, chWidth);
         final int tmp = (int) Math.round((2.0 * width) / chWidth);
         final int filterLen = (2 * (tmp / 2)) + 1;
         double sum = 0.0;
         final double w = filterLen / 6.0;
         mFilter = new double[filterLen];
         for (int i = 0; i < filterLen; ++i) {
            mFilter[i] = Math.exp(-((i - (filterLen / 2)) / w) * ((i - (filterLen / 2)) / w));
            sum += mFilter[i];
         }
         sum /= filterLen;
         for (int i = 0; i < filterLen; ++i)
            mFilter[i] -= sum;
         final NumberFormat nf = new HalfUpFormat("0.0");
         mVarCorrection = defaultVarianceCorrectionFactor(tmp, 2 * tmp + 1);
         mName = "Gaussian[w=" + nf.format(w) + "]";
      }
   }

   /**
    * A filter based on a SavitskyGolay function.
    * 
    * @author nicholas
    */
   static public class SavitskyGolayFilter extends FittingFilter {

      public SavitskyGolayFilter(double width, double chWidth) {
         super(width, chWidth);
         final int tmp = (int) Math.round((2.0 * width) / chWidth);
         final int filterLen = (2 * (tmp / 2)) + 1;
         mFilter = new double[filterLen];
         final double m = (filterLen / 2);
         final double sum = ((2.0 * m) - 1.0) * ((2.0 * m) + 1.0) * ((2.0 * m) + 3.0);
         for (int jp = 0; jp < filterLen; ++jp) {
            final double j = jp - m;
            mFilter[jp] = ((3.0 * (((3.0 * m * m) + (3.0 * m)) - 1.0 - (5.0 * j * j))) / sum) - (1.0 / filterLen);
         }
         final NumberFormat nf = new HalfUpFormat("0.0");
         mVarCorrection = defaultVarianceCorrectionFactor(tmp, 2 * tmp + 1);
         mName = "Savitsky-Golay[m=" + nf.format(m) + "]";
      }
   }

   /**
    * Filter based on the second derivative of a Gaussian
    * 
    * @author nicholas
    */
   static public class D2Gaussian extends FittingFilter {

      public D2Gaussian(double width, double chWidth) {
         super(width, chWidth);
         final int tmp = (int) Math.round((2.0 * width) / chWidth);
         final int filterLen = (2 * (tmp / 2)) + 1;
         mFilter = new double[filterLen];
         final double w = filterLen / 6.0;
         double sum = 0.0;
         for (int i = 0; i < filterLen; ++i) {
            final double x = ((double) filterLen * (double) (i - (filterLen / 2))) / (filterLen - 1);
            final double y = -Math2.sqr(x / w);
            mFilter[i] = ((2.0 * Math.exp(y)) / w) * (1 + (2.0 * y));
            sum += mFilter[i];
         }
         sum /= filterLen;
         for (int i = 0; i < filterLen; ++i)
            mFilter[i] -= sum;
         final NumberFormat nf = new HalfUpFormat("0.0");
         mVarCorrection = defaultVarianceCorrectionFactor(tmp, 2 * tmp + 1);
         mName = "d2G/dE2[m=" + nf.format(w) + "]";
      }
   }

}