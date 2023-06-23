package gov.nist.microanalysis.Utility;

import java.util.Collection;

/**
 * <p>
 * A simple class to calculate four basic descriptive statistics - average,
 * variance, skewness and kurtosis. Create an instance of the class, then use
 * addPoint() to record data points. At any time you may call one of the
 * statistic functions to return the current value of the statistic.
 * </p>
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

public class DescriptiveStatistics implements Comparable<DescriptiveStatistics> {
   private double mLast;
   private double mSum;
   private double mSumOfSqrs;
   private double mSumOfCubes;
   private double mSumOfQuarts;
   private double mMin, mMax;
   private int mNPoints;

   @Override
   public DescriptiveStatistics clone() {
      final DescriptiveStatistics res = new DescriptiveStatistics();
      res.mLast = mLast;
      res.mSum = mSum;
      res.mSumOfSqrs = mSumOfSqrs;
      res.mSumOfCubes = mSumOfCubes;
      res.mSumOfQuarts = mSumOfQuarts;
      res.mMin = mMin;
      res.mMax = mMax;
      res.mNPoints = mNPoints;
      return res;
   }

   /**
    * Form a DescriptiveStatistics object which combines the statistics from two
    * source DescriptiveStatistics objects into one. For example, the average of
    * the resulting object is the average of the source objects weighted by the
    * number of items in each.
    * 
    * @param ds1
    *           DescriptiveStatistics
    * @param ds2
    *           DescriptiveStatistics
    */
   public DescriptiveStatistics(DescriptiveStatistics ds1, DescriptiveStatistics ds2) {
      mLast = Double.NaN;
      mSum = ds1.mSum + ds2.mSum;
      mSumOfSqrs = ds1.mSumOfSqrs + ds2.mSumOfSqrs;
      mSumOfCubes = ds1.mSumOfCubes + ds2.mSumOfCubes;
      mSumOfQuarts = ds1.mSumOfQuarts + ds2.mSumOfQuarts;
      mNPoints = ds1.mNPoints + ds2.mNPoints;
      mMin = Math.min(ds1.mMin, ds2.mMin);
      mMax = Math.max(ds1.mMax, ds2.mMax);
      mNPoints = ds1.mNPoints + ds2.mNPoints;
   }

   /**
    * Constructs an empty DescriptiveStatistics object
    */
   public DescriptiveStatistics() {
      mLast = Double.NaN;
      mSum = 0.0;
      mSumOfSqrs = 0.0;
      mSumOfCubes = 0.0;
      mSumOfQuarts = 0.0;
      mNPoints = 0;
      mMin = Double.MAX_VALUE;
      mMax = -Double.MAX_VALUE;
   }

   /**
    * Merge the argument DescriptiveStatistics object with this one.
    * 
    * @param ds
    *           DescriptiveStatistics
    */
   public void merge(DescriptiveStatistics ds) {
      mLast = Double.NaN;
      mSum += ds.mSum;
      mSumOfSqrs += ds.mSumOfSqrs;
      mSumOfCubes += ds.mSumOfCubes;
      mSumOfQuarts += ds.mSumOfQuarts;
      mNPoints += ds.mNPoints;
      mMin = Math.min(mMin, ds.mMin);
      mMax = Math.max(mMax, ds.mMax);
      mNPoints += ds.mNPoints;
   }

   /**
    * Add a number to the accumulated statistics.
    * 
    * @param x
    *           double
    */
   public void add(double x) {
      final double x2 = x * x;
      mSum += x;
      mSumOfSqrs += x2;
      mSumOfCubes += x2 * x;
      mSumOfQuarts += x2 * x2;
      if (Double.isNaN(mMin) || (x < mMin))
         mMin = x;
      if (Double.isNaN(mMax) || (x > mMax))
         mMax = x;
      mLast = x;
      ++mNPoints;
   }

   /**
    * The average value of the values added to this object using the add method.
    * 
    * @return double
    */
   public double average() {
      return mSum / mNPoints;
   }

   /**
    * variance - sigma^2=E[(x-mu)^2]
    * 
    * @return double
    */
   public double variance() {
      final double avg = average();
      return mNPoints > 1 ? Math.max(0.0, (mSumOfSqrs - (mSum * avg)) / mNPoints) : Double.NaN;
   }

   /**
    * standardDeviation - The square root of the variance.
    * 
    * @return double
    */
   public double standardDeviation() {
      return Math.sqrt(variance());
   }

   /**
    * Returns the average and the standard deviation as an uncertain value.
    * 
    * @param name
    *           Name given to the uncertainty component (Source)
    * @return UncertainValue2
    */
   public UncertainValue2 getValue(String name) {
      final double sd = standardDeviation();
      return new UncertainValue2(average(), name, Double.isNaN(sd) ? 0.0 : sd);
   }

   /**
    * skewness - E[(x-mu)^3]/sigma^3
    * 
    * @return double
    */
   public double skewness() {
      final double mu = average();
      return ((mSumOfCubes - (3.0 * mu * mSumOfSqrs)) + (2 * mSum * mu * mu)) / (mNPoints * Math.pow(variance(), 1.5));
   }

   /**
    * standardErrorOfSkewness - A number against which to compare the skewness
    * to determine the statistical significance. A skewness of in excess of
    * twice this number indicates a distribution that is highly likely to be
    * skewed.
    * 
    * @return double
    */
   public double standardErrorOfSkewness() {
      return Math.sqrt(6.0 / mNPoints);
   }

   /**
    * kurtosis - (E[(x-mu)^4]/sigma^4) - 3
    * 
    * @return double
    */
   public double kurtosis() {
      final double mu = average();
      final double v = variance();
      return ((((mSumOfQuarts - (4.0 * mu * mSumOfCubes)) + (6.0 * mu * mu * mSumOfSqrs)) - (3.0 * mSum * mu * mu * mu)) / (mNPoints * v * v)) - 3.0;
   }

   /**
    * standardErrorOfKurtosis - A number against which the kurtosis can be
    * compared to determine whether the distribution has a significant kurtosis.
    * A kurtosis of twice this number indicates a statistically significant
    * kurtosis.
    * 
    * @return double
    */
   public double standardErrorOfKurtosis() {
      return Math.sqrt(24.0 / mNPoints);
   }

   /**
    * minimum - The minimum value in the list
    * 
    * @return double
    */
   public double minimum() {
      return mMin;
   }

   /**
    * maximum - The maximum value in the list
    * 
    * @return double
    */
   public double maximum() {
      return mMax;
   }

   @Override
   public String toString() {
      return Integer.toString(mNPoints) + "\t" + Double.toString(average()) + "\t" + Double.toString(standardDeviation()) + "\t"
            + Double.toString(minimum()) + "\t" + Double.toString(maximum());
   }

   /**
    * Returns the number of data items added to this DescriptiveStatistics
    * object.
    * 
    * @return int
    */
   public int count() {
      return mNPoints;
   }

   /**
    * Returns the sum of the points
    * 
    * @return double
    */
   public double sum() {
      return mSum;
   }

   public double getLastAdded() {
      return mLast;
   }

   /**
    * <p>
    * Remove a previously added value and update the average, stdDev, skewness
    * and kurtosis statistics. Minimum and maximum statistics may not be correct
    * after a call to remove.
    * </p>
    * <p>
    * <b>Use this method with caution.</b> It is not possible to remove values
    * without the potential for errors. First, there is no attempt to verify
    * that the value was previously added. Second, since there is no history,
    * minimum and maximum values may become corrupt (NaN).
    * </p>
    * 
    * @param val
    *           double
    */
   public void remove(double val) {
      if (mMax == val)
         mMax = Double.NaN;
      if (mMin == val)
         mMin = Double.NaN;
      --mNPoints;
      mSum -= val;
      mSumOfSqrs -= val * val;
      mSumOfCubes -= val * val * val;
      mSumOfQuarts -= val * val * val * val;
   }

   /**
    * remove the last number added to this descriptive statistics object. Undoes
    * the actions of add(...).
    * 
    * @return true if it is possible to remove the last
    */
   public boolean removeLast() {
      final boolean res = !Double.isNaN(mLast);
      if (res)
         remove(mLast);
      mLast = Double.NaN;
      return res;
   }

   /**
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   @Override
   public int compareTo(DescriptiveStatistics o) {
      final double ta = average();
      final double oa = o.average();
      if (ta == oa) {
         final double tv = variance();
         final double ov = o.variance();
         return tv == ov ? 0 : (tv < ov ? -1 : 1);
      } else
         return ta < oa ? -1 : 1;
   }

   public static DescriptiveStatistics compute(Number[] ns) {
      DescriptiveStatistics ds = new DescriptiveStatistics();
      for (Number n : ns)
         ds.add(n.doubleValue());
      return ds;
   }

   public static DescriptiveStatistics compute(Collection<? extends Number> ns) {
      DescriptiveStatistics ds = new DescriptiveStatistics();
      for (Number n : ns)
         ds.add(n.doubleValue());
      return ds;
   }

}
