package gov.nist.nanoscalemetrology.MONSELtests;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.TreeMap;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.EPQFatalException;

/**
 * <p>
 * This is a temporary copy of gov.nist.microanalysis.Utility.Histogram modified
 * for debugging purposes. In this modified version bins are (binMin,binMax]
 * instead of [binMin,binMax). Because of this, any elastically scattered
 * electrons are included in the uppermost bin instead of the over-range bin.
 * The only electrons that appear in the over-range bin should be those that
 * GAIN energy in the sample.
 * </p>
 * <p>
 * A class for creating histograms of series of data points. It is possible to
 * create histograms of equally spaced bins, ratio spaced bins or arbitrarily
 * spaced bins.
 * </p>
 * <p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class Histogram {
   private double[] mBinMin;
   private int[] mCounts;

   public class BinName
      implements Comparable<BinName> {
      private final int mBin;
      private final String mFormat;

      private BinName(int bin, String format) {
         assert (bin >= -1);
         assert (bin < (binCount() + 1));
         mBin = bin;
         mFormat = format;
      }

      @Override
      public String toString() {
         return "[" + (mBin < 0 ? "-inf"
               : MessageFormat.format(mFormat, new Object[] {
                  Double.valueOf(minValue(mBin))
               })) + "-" + (mBin >= binCount() ? "inf"
                     : MessageFormat.format(mFormat, new Object[] {
                        Double.valueOf(maxValue(mBin))
         })) + ")";
      }

      @Override
      public int compareTo(BinName o) {
         return mBin < o.mBin ? -1 : (mBin > o.mBin ? 1 : 0);
      }
   }

   /**
    * <p>
    * Histogram - Creates a histogram representing the specified range with bins
    * for over-range and under-range.
    * </p>
    * <p>
    * Bins are numbered such that 0..(nBins-1) correspond to the full range.
    * Bin[0] -&gt; [min,min+delta) where delta = (max-min)/nBins, Bin[nBins-1]
    * -&gt; [max-delta,max). There are two bins for under and overrange values.
    * Bin[-1]-&gt;[-inf,min), Bin[nBins] -&gt; [max,inf) where inf is infinity.
    * </p>
    *
    * @param min double
    * @param max double
    * @param nBins int - The number of bins (excluding over- and under-range)
    */
   public Histogram(double min, double max, int nBins) {
      if(max < min) {
         final double tmp = min;
         min = max;
         max = tmp;
      }
      if(min == max)
         throw new EPQFatalException("Histogram: min can not equal max");
      assert min < max;
      assert nBins > 0;
      mCounts = new int[nBins + 2]; // nBins + under + over
      mBinMin = new double[nBins + 1];
      final double delta = (max - min) / nBins;
      mBinMin[0] = min;
      for(int i = 1; i < mBinMin.length; ++i)
         mBinMin[i] = min + (i * delta);
      Arrays.sort(mBinMin);
   }

   /**
    * Constructs a Histogram object to represent bins between min (inclusive)
    * and max (excluded)
    *
    * @param min
    * @param max
    * @param ratio
    * @throws EPQException
    */
   public Histogram(double min, double max, double ratio)
         throws EPQException {
      if(ratio <= 1.0)
         throw new EPQException("Histogram: ration must be greater than 1.0");
      if(min >= max)
         throw new EPQException("Histogram: min must be less than max");
      if(min <= 0.0)
         throw new EPQException("Histogram: min must be larger than 0.0");
      final int nBins = (int) (Math.log(max / min) / Math.log(ratio));
      mCounts = new int[nBins + 2];
      mBinMin = new double[nBins + 1];
      for(int i = 0; i < mBinMin.length; ++i, min *= ratio)
         mBinMin[i] = min;
   }

   /**
    * Constructs a Histogram object to bin into the arbitrary bins defined by
    * lower ends defind by binMins. The max end of the top bin is defined by
    * max.
    *
    * @param binMins
    * @param max
    */
   public Histogram(double[] binMins, double max)
         throws EPQException {
      mBinMin = new double[binMins.length + 1];
      for(int i = 0; i < binMins.length; ++i)
         mBinMin[i] = binMins[i];
      mBinMin[binMins.length] = max;
      Arrays.sort(mBinMin);
      if(mBinMin[mBinMin.length - 1] != max)
         throw new EPQException("Histogram: Max is not larger than all binMins.");
      mCounts = new int[binMins.length + 2];
   }

   public Histogram(Histogram hist) {
      mBinMin = new double[hist.mBinMin.length];
      mCounts = new int[hist.mCounts.length];
      System.arraycopy(hist.mBinMin, 0, mBinMin, 0, mBinMin.length);
      System.arraycopy(hist.mCounts, 0, mCounts, 0, mCounts.length);
   }

   public void addBin(double binMin) {
      final double[] newBinMin = new double[mBinMin.length + 1];
      System.arraycopy(mBinMin, 0, newBinMin, 0, mBinMin.length);
      newBinMin[mBinMin.length] = binMin;
      Arrays.sort(newBinMin);
      mCounts = new int[newBinMin.length + 2];
      mBinMin = newBinMin;
   }

   /**
    * bin - Returns the bin into which the val fits.
    *
    * @param val double
    * @return int
    */
   public int bin(double val) {
      int i = Arrays.binarySearch(mBinMin, val);
      i = (i >= 0 ? i : -i - 2);
      assert i >= -1 : "index is " + Integer.toString(i) + " for " + Double.toString(val);
      assert i < mCounts.length;
      if((val == minValue(i)) && (i > -1))
         i--;
      return i;
   }

   /**
    * minValue - Returns the minimum value stored in the specified bin
    *
    * @param bin int
    * @return double
    */
   public double minValue(int bin) {
      return bin > -1 ? mBinMin[bin] : Double.NEGATIVE_INFINITY;
   }

   /**
    * maxValue - Returns the upper limit for values stored in this bin. Actually
    * this value is excluded from the bin and included in the next larger bin.
    *
    * @param bin int
    * @return double
    */
   public double maxValue(int bin) {
      return (bin + 1) < mBinMin.length ? mBinMin[bin + 1] : Double.POSITIVE_INFINITY;
   }

   /**
    * add - Add the specified value to the histogram.
    *
    * @param val double
    */
   public void add(double val) {
      ++mCounts[bin(val) + 1];
   }

   /**
    * add - Add the specified array of values to the histogram.
    *
    * @param vals double[]
    */
   public void add(double[] vals) {
      for(final double v : vals)
         add(v);
   }

   /**
    * binCount - Returns the number of bins (not counting over-range and
    * under-range bins)
    *
    * @return int
    */
   public int binCount() {
      return mBinMin.length - 1;
   }

   /**
    * binName - Returns a string that describes the specified bin. The String
    * format is used to format numbers using MessageFormat.format(...). An
    * common usage might be <code>binName(i,"{0,number,#.##}")</code>.
    *
    * @param bin int
    * @param format String
    * @return String
    */
   public String binName(int bin, String format) {
      return (new BinName(bin, format)).toString();
   }

   /**
    * counts - Returns the number of counts in the specified bin.
    *
    * @param bin int - -1 to binCount()
    * @return int
    */
   public int counts(int bin) {
      return mCounts[bin + 1];
   }

   /**
    * overrange - Returns the number of events that fell into the overrange bin
    * (larger than the max argument of the constructor.)
    *
    * @return int
    */
   public int overrange() {
      return mCounts[mCounts.length - 1];
   }

   /**
    * underrange -Returns the number of events that fell into the underrange bin
    * (less than the min argument of the constructor.)
    *
    * @return int
    */
   public int underrange() {
      return mCounts[0];
   }

   /**
    * Returns the total number of events recorded in the histogram.
    *
    * @return int
    */
   public int totalCounts() {
      int res = 0;
      for(final int c : mCounts)
         res += c;
      return res;
   }

   public boolean isBinMin(double binMin) {
      final int i = Arrays.binarySearch(mBinMin, binMin);
      return i >= 0;
   }

   public void removeBin(int binNum)
         throws EPQException {
      if((binNum < 0) || (binNum > (mCounts.length - 2)))
         throw new EPQException(binNum + " is not a bin in the histogram");
      final double[] newBinMin = new double[mBinMin.length - 1];
      for(int index = 0; index < mBinMin.length; index++)
         if(index < binNum)
            newBinMin[index] = mBinMin[index];
         else if(index > binNum)
            newBinMin[index - 1] = mBinMin[index];
      mBinMin = newBinMin;
      Arrays.sort(mBinMin);

      final int[] newCounts = new int[mCounts.length - 1];
      // Since we're deleting the bin, move the counts into
      // the bin before the one being removed
      mCounts[binNum] += mCounts[binNum + 1];
      for(int index = 0; index < mCounts.length; index++)
         if(index < (binNum + 1))
            newCounts[index] = mCounts[index];
         else if(index > (binNum + 1))
            newCounts[index - 1] = mCounts[index];
      mCounts = newCounts;
   }

   public TreeMap<BinName, Integer> getResultMap(String format) {
      final TreeMap<BinName, Integer> res = new TreeMap<BinName, Integer>();
      for(int i = -1; i < (binCount() + 1); ++i)
         res.put(new BinName(i, format), counts(i));
      return res;
   }
}
