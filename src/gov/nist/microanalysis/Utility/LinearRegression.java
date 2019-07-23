/**
 * <p>
 * A class implementing a basic LLSQ regression to a line.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas
 * @version 1.0
 */
package gov.nist.microanalysis.Utility;

import Jama.Matrix;

/**
 * Implements a class for computing the linear regression (the best fit line in
 * a least-squares sense.)
 */
public class LinearRegression {
   private double mS;
   private double mSxx;
   private double mSx;
   private double mSxy;
   private double mSy;
   private double mSyy;
   private int mCount;

   private double mSlope = Double.NaN;
   private double mIntercept = Double.NaN;
   private Matrix mCovariance = null;

   /**
    * Constructs an object implementing the linear regression for best fitting a
    * line to data points (in a least-squares sense.)
    */
   public LinearRegression() {
      super();
      clear();
   }

   /**
    * clear - Clear the current accumulation and start a new fit.
    */
   public void clear() {
      mS = 0.0;
      mSxx = 0.0;
      mSx = 0.0;
      mSxy = 0.0;
      mSy = 0.0;
      mSyy = 0.0;
      mCount = 0;
      mSlope = Double.NaN;
      mIntercept = Double.NaN;
   }

   /**
    * setData - Clear the current accumulation and add two equal lengthed arrays
    * of data points.
    * 
    * @param x double[]
    * @param y double[]
    */
   public void setData(double[] x, double[] y) {
      clear();
      addData(x, y);
   }

   /**
    * addData - Add two equal lengthed arrays of data points to the current
    * accumulation.
    * 
    * @param x double[]
    * @param y double[]
    */
   public void addData(double[] x, double[] y) {
      assert (x.length == y.length);
      for(int i = 0; i < x.length; ++i)
         addDatum(x[i], y[i]);
   }

   /**
    * addDatum - Add a single data point
    * 
    * @param x double
    * @param y double
    */
   public void addDatum(double x, double y) {
      addDatum(x, y, 1.0);
   }

   /**
    * addDatum - Add a single data point with error estimate
    * 
    * @param x double
    * @param y double
    * @param dy Error estimate on y
    */
   public void addDatum(double x, double y, double dy) {
      assert dy > 0 : "The error estimate must be larger than zero!!!";
      final double dy2 = dy * dy;
      mS += 1.0 / dy2;
      mSx += x / dy2;
      mSxx += (x * x) / dy2;
      mSxy += (x * y) / dy2;
      mSy += y / dy2;
      mSyy += (y * y) / dy2;
      ++mCount;
      mSlope = Double.NaN;
      mIntercept = Double.NaN;
   }

   /**
    * removeDatum - Removes a single data point from the set of data to be fit.
    * The values x &amp; y should have been previously added using addDatum(x,y)
    * or as part of an setData() or addData() command otherwise the meaning of
    * the operations performed by removeDatum(...) is not defined as expected.
    * removeDatum(...) does not check to see that the pair x,y were previously
    * added.
    * 
    * @param x double
    * @param y double
    * @param dy error estimate for y
    */
   public void removeDatum(double x, double y, double dy) {
      final double dy2 = dy * dy;
      mS -= 1.0 / dy2;
      mSx -= x / dy2;
      mSxx -= (x * x) / dy2;
      mSxy -= (x * y) / dy2;
      mSy -= y / dy2;
      mSyy -= (y * y) / dy2;
      --mCount;
      mSlope = Double.NaN;
      mIntercept = Double.NaN;
   }

   public void removeDatum(double x, double y) {
      removeDatum(x, y, 1.0);
   }

   private void compute() {
      mSlope = Double.POSITIVE_INFINITY;
      mIntercept = 0.0;
      final double den = (mS * mSxx) - (mSx * mSx);
      if(den != 0.0) {
         mSlope = ((mS * mSxy) - (mSx * mSy)) / den;
         mIntercept = ((mSxx * mSy) - (mSx * mSxy)) / den;
         mCovariance = new Matrix(2, 2);
         mCovariance.set(0, 0, mSxx / den);
         mCovariance.set(1, 1, mS / den);
         final double c = -mSx / den;
         mCovariance.set(1, 0, c);
         mCovariance.set(0, 1, c);
      }
   }

   /**
    * getSlope - Returns the current best estimate of the slope. y(x) =
    * x*getSlope() + getIntercept();
    * 
    * @return double
    */
   public double getSlope() {
      if(Double.isNaN(mSlope) && (mCount > 0))
         compute();
      return mSlope;
   }

   /**
    * getIntercept - Returns the current best estimate of the y-intercept. y(x)
    * = x*getSlope() + getIntercept();
    * 
    * @return double
    */
   public double getIntercept() {
      if(Double.isNaN(mSlope) && (mCount > 0))
         compute();
      return mIntercept;
   }

   /**
    * Returns the x such that y = 0.
    * 
    * @return double
    */
   public double getXIntercept() {
      if(Double.isNaN(mSlope) && (mCount > 0))
         compute();
      return -mIntercept / mSlope;
   }

   /**
    * computeY - Computes Y(x) = x*getSlope() + getIntercept();
    * 
    * @param x
    * @return double
    */
   public double computeY(double x) {
      return getIntercept() + (getSlope() * x);
   }

   public Matrix covariance() {
      if(Double.isNaN(mSlope) && (mCount > 0))
         compute();
      return mCovariance;
   }

   /**
    * Returns the correlation between the slope and intercept.
    * 
    * @return double
    */
   public double correlation() {
      return -mSxx / Math.sqrt(mS * mSxx);
   }

   /**
    * computeX - Computes the x such that y(x) = x*getSlope() + getIntercept();
    * 
    * @param y double
    * @return double
    */
   public double computeX(double y) {
      return (y - getIntercept()) / getSlope();
   }

   /**
    * Compute the chi-squared statistic for the best fit line through the
    * specified data points.
    * 
    * @return double
    */
   public double chiSquared() {
      final double a = getIntercept(), b = getSlope();
      final double res = (mSyy - (2.0 * (((a * mSy) - (a * b * mSx)) + (b * mSxy)))) + (b * b * mSxx) + (a * a * mS);
      // assert res >= 0.0 : Double.toString(res);
      return Math.max(0.0, res);
   }

   /**
    * Computes the goodness of fit metric Q = gammaq((N-2)/2,chi^2/2). See
    * section 15.2 of Press et al. C 2nd
    * 
    * @return double
    */
   public double goodnessOfFit() {
      return Math2.gammq(0.5 * (mCount - 2), chiSquared() / 2.0);
   }

   /**
    * Computes the r^2 metric
    * 
    * @return double
    */
   public double getRSquared() {
      return Math2.sqr((mCount * mSxy) - (mSx * mSy)) / (((mCount * mSxx) - (mSx * mSx)) * ((mCount * mSyy) - (mSy * mSy)));
   }

   /**
    * Returns the number of data points used in the linear regression.
    * 
    * @return int
    */
   public int getCount() {
      return mCount;
   }

   @Override
   public String toString() {
      return "Y=" + Double.toString(getSlope()) + " X + " + Double.toString(getIntercept());
   }
}
