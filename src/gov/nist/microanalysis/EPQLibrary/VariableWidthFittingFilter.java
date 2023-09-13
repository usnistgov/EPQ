/**
 * 
 */
package gov.nist.microanalysis.EPQLibrary;

import java.text.DecimalFormat;
import java.util.Arrays;

import gov.nist.microanalysis.EPQLibrary.Detector.EDSCalibration;
import gov.nist.microanalysis.Utility.Math2;

/**
 * @author nritchie
 *
 */
public class VariableWidthFittingFilter {

   public final double[][] mFilters;
   public final int[] mOffsets;
   public double mVarianceCorrection;

   public static double defaultVarianceCorrectionFactor(int lw, int uw) {
      // Verified with Fred Schamber on 2-Sep-2008
      return (2.0 * uw * lw) / (uw + (2.0 * lw));
   }

   protected VariableWidthFittingFilter(int nch) {
      mFilters = new double[nch][];
      mOffsets = new int[nch];
      mVarianceCorrection = 1.0;
   }

   /**
    * Apply the filter to count data.
    * 
    * @param counts
    *           The counts data from the full spectrum extent
    * @return double[2][counts.length] containing the filtered and variance
    *         spectra respectively
    */
   public double[][] compute(double[] counts) {
      assert counts.length <= mOffsets.length;
      double[][] result = new double[2][counts.length];
      for (int ch = 0; ch < counts.length; ++ch) {
         double[] filt = mFilters[ch];
         int offset = mOffsets[ch];
         double filtered = 0.0, variance = 0.0;
         for (int i = 0; i < filt.length; ++i) {
            double v = counts[i + offset] * filt[i];
            filtered += v;
            variance += v * filt[i];
         }
         result[0][ch] = filtered;
         result[1][ch] = variance;
      }
      return result;
   }

   public double[] filter(int ch) {
      return mFilters[ch];
   }

   public int offset(int ch) {
      return mOffsets[ch];
   }

   public double varianceCorrectionFactor() {
      return mVarianceCorrection;
   }

   /**
    * A variable width fitting filter based on the second derivative of a
    * Gaussian. This has proven to be a very effective filter in tests using
    * NeXLSpectrum.
    * 
    * @author nritchie
    */
   public static class G2VariableWidthFittingFilter extends VariableWidthFittingFilter {

      private final double a;  // Nominal 1
      private final double b;  // Nominal 4

      /**
       * Second derivative of a Gaussian (negative is just to make the filtered
       * peak go up...)
       * 
       * @param e
       * @param e0
       * @param w
       * @return double
       */
      private double func(double e, double e0, double w) {
         return -Math.exp(-0.5 * Math2.sqr((e - e0) / w)) * (Math2.sqr(e - e0) - w * w) / Math2.sqr(w * w);
      }

      public G2VariableWidthFittingFilter(EDSCalibration calib, int nch, double a, double b) {
         super(nch);
         this.a = a;
         this.b = b;
         for (int ch = 0; ch < nch; ++ch) {
            final double e = calib.getZeroOffset() + ch * calib.getChannelWidth();
            if (e > 0.0) {
               final double w = a * calib.getLineshape().leftWidth(e, Math.exp(-0.5));
               final int chw = (int) Math.ceil(b * w / calib.getChannelWidth());
               // Range of channels over which to compute the filter
               final int chmin = ch - chw, chmax = ch + chw;
               // Range of channels over which to save the filter
               final int smin = Math.max(0, chmin), smax = Math.min(chmax + 1, nch);
               final double[] filt = new double[smax - smin];
               double sum = 0.0;
               for (int i = chmin; i <= chmax; ++i) {
                  final double ei = calib.getZeroOffset() + i * calib.getChannelWidth();
                  final double f = func(ei, e, w);
                  filt[Math2.bound(i, smin, smax) - smin] += f;
                  sum += f;
               }
               double off = sum / (chmax - chmin + 1);
               for (int i = chmin; i <= chmax; ++i)
                  filt[Math2.bound(i, smin, smax) - smin] -= off;
               assert Math.abs(Arrays.stream(filt).sum()) <= 1.0e-8 : "At " + ch;
               mFilters[ch] = filt;
               mOffsets[ch] = smin;
            } else {
               mFilters[ch] = new double[0];
               mOffsets[ch] = 0;
            }
         }
         final double w_mn = a * calib.getLineshape().leftWidth(SpectrumUtils.E_MnKa, Math.exp(-0.5));
         final int chw_mn = (int) Math.ceil(w_mn / calib.getChannelWidth());
         mVarianceCorrection = defaultVarianceCorrectionFactor((int) (0.7 * (b-a) * chw_mn), (int) (1.5 * a * chw_mn));
      }

      public String toString() {
         DecimalFormat df = new DecimalFormat("0.00");
         return "G2[" + df.format(this.a) + "," + df.format(this.b) + "]";
      }

   };

}
