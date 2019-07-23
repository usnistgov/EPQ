package gov.nist.microanalysis.Utility;

import java.util.Arrays;
import java.util.Random;

/**
 * <p>
 * The Simplex algorithm is a method for minimizing non-linear functions (Use
 * LinearLeastSquares for linear optimization). In an N dimensional space, the
 * Simplex starts with N+1 user specified points and wanders these points around
 * the space looking to minimize 'function'. The Simplex algorithm stops when
 * all points are within the specified tolerance of a local minimum value.
 * </p>
 * <p>
 * Derive a class from Simplex and implement the method function(double[] args).
 * Simplex.perform will adjust the input parameters function to produce the
 * smallest possible return value.
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

abstract public class Simplex {
   /**
    * DEFAULT_TOLERANCE - The default value for the tolerance.
    */
   public static final double DEFAULT_TOLERANCE = 1.0e-8;
   /**
    * DEFAULT_EVALUATIONS - The default maximum number of function evaluations
    * before giving up.
    */
   public static final int DEFAULT_EVALUATIONS = 5000;
   private static final double LARGEST_TOLERANCE = 0.01;
   private static final int MIN_EVALUATIONS = 100;
   private double[] mPTry = null; // Allocated by attempt(...)
   private double mTolerance = DEFAULT_TOLERANCE;
   private int mMaxEvaluations = DEFAULT_EVALUATIONS;
   private double[] mBest = null; // best result after failure or success
   private double mResult = Double.NaN;
   private Object[] mParameters = null; // An optional set of parameters
   private int mEvaluationCount = 0;

   // handed in through the constructor

   // ooze - Trys a single Simplex step by projecting one vertex of the Simplex
   // by a fractional amount (frac) and
   // evaluating function at the new point.
   private double ooze(double[][] p, double[] y, double[] psum, int ihi, double fac)
         throws UtilException {
      final int nDim = p.length - 1;
      assert (y.length == (nDim + 1));
      assert (p[0].length == nDim);
      assert (psum.length == nDim);
      if((mPTry == null) || (mPTry.length != nDim))
         mPTry = new double[nDim];
      final double fac1 = (1.0 - fac) / nDim;
      final double fac2 = fac1 - fac;
      for(int j = 0; j < nDim; ++j)
         mPTry[j] = (psum[j] * fac1) - (p[ihi][j] * fac2);
      final double ytry = evaluateFunction(mPTry);
      if(ytry < y[ihi]) {
         y[ihi] = ytry;
         for(int j = 0; j < nDim; ++j) {
            psum[j] += mPTry[j] - p[ihi][j];
            p[ihi][j] = mPTry[j];
         }
      }
      return ytry;
   }

   // Tests the result from function for basic reasonableness
   private double evaluateFunction(double[] x)
         throws UtilException {
      final double res = function(x);
      if(Double.isNaN(res))
         throw new UtilException("The function in the Simplex routine returned NaN at " + Arrays.toString(x));
      if(Double.isInfinite(res))
         throw new UtilException("The function in the Simplex routine is not finite at " + Arrays.toString(x));
      return res;
   }

   /**
    * Simplex - Default constructor for no custom parameters.
    */
   public Simplex() {
   }

   /**
    * Simplex - Constructor for handing in custom parameters that have meaning
    * only to the derived class. This facilitates using Simplex to derive
    * anonymous classes.
    * 
    * @param params double[]
    */
   public Simplex(Object[] params) {
      mParameters = params.clone();
   }

   /**
    * getParameters - Gives access to the custom parameters to a derived class.
    * 
    * @return double[]
    */
   public Object[] getParameters() {
      return mParameters;
   }

   /**
    * function - The user defined function that Simplex is to minimize. It is
    * not uncommon that the input parameters may not always remain within a
    * meaningful bound (ie they may step outside of the range of physically
    * realizable parameters). The simplest way to handle this is to simply
    * return Double.MAX_VALUE. The Simplex will see this as very poorly
    * optimized step and avoid taking it.
    * 
    * @param x double[] - The nDim function arguments
    * @return double - The result
    */
   public abstract double function(double[] x);

   /**
    * Create a randomized starting point based on the single point given in
    * center and an associated scale for each dimension given in scale.
    * 
    * @param center double[]
    * @param scale double[]
    * @return double[][]
    */
   public static double[][] randomizedStartingPoints(double[] center, double[] scale) {
      final Random r = new Random(System.currentTimeMillis()); // ensure
      // randomized
      assert (center.length == scale.length);
      final double[][] res = new double[center.length + 1][center.length];
      for(int j = 0; j < res[0].length; ++j)
         res[0][j] = center[j];
      for(int i = 1; i < res.length; ++i)
         for(int j = 0; j < res[i].length; ++j)
            res[i][j] = center[j] + (scale[j] * (1.0 - (2.0 * r.nextDouble())));
      return res;
   }

   /**
    * Create a regularized set of starting points based on the single point
    * given in center and an associated scale for each dimension given in scale.
    * The starting points are based on the center point. The first point is the
    * center point, the second point is offset by scale[0], the third by
    * scale[1], etc.
    * 
    * @param center double[]
    * @param scale double[]
    * @return double[][]
    */
   public static double[][] regularizedStartingPoints(double[] center, double[] scale) {
      assert (center.length == scale.length);
      final double[][] res = new double[center.length + 1][center.length];
      for(final double[] re : res)
         System.arraycopy(center, 0, re, 0, center.length);
      for(int i = 1; i < res.length; ++i)
         res[i][i - 1] += scale[i - 1];
      return res;
   }

   /**
    * perform - Perform the Simplex algorithm starting with a simplex of N+1
    * dimensions as specified in p. The result is returned in p and the returned
    * array of doubles. All are within the tolerance of the minimum. (note:
    * Simplex deduces the number of dimensions in the problem from p[0].length.
    * 
    * @param p double[][] - a double[n+1][n] dimension array
    * @return double[] - The best result
    * @throws UtilException
    */
   public double[] perform(double[][] p)
         throws UtilException {
      final int nDim = p[0].length, mpts = nDim + 1;
      mEvaluationCount = nDim;
      assert (p.length == mpts);
      assert (p[0].length == nDim);
      assert (p[mpts - 1].length == nDim);
      final double[] psum = new double[nDim];
      final double[] y = new double[mpts];
      // Fill y with the function evaluated at the points in p
      for(int i = 0; i < mpts; ++i)
         y[i] = evaluateFunction(p[i]);
      // GET_PSUM
      for(int j = 0; j < nDim; ++j) {
         double sum = 0.0;
         for(int i = 0; i < mpts; ++i)
            sum += p[i][j];
         psum[j] = sum;
      }
      while(true) {
         int ilo = 0;
         int ihi = (y[0] > y[1] ? 0 : 1);
         int inhi = 1 - ihi;
         for(int i = 0; i < mpts; ++i) {
            if(y[i] <= y[ilo])
               ilo = i;
            if(y[i] > y[ihi]) {
               inhi = ihi;
               ihi = i;
            } else if((y[i] > y[inhi]) && (i != ihi))
               inhi = i;
         }
         final double tol = Math.abs(y[ihi] - y[ilo]);
         final double rtol = (2.0 * tol) / (Math.abs(y[ihi]) + Math.abs(y[ilo]));
         if((rtol < mTolerance) || (tol < mTolerance)) {
            mBest = p[ilo];
            mResult = y[ilo];
            break; // out of while(true)
         }
         if(mEvaluationCount > mMaxEvaluations) {
            mBest = p[ilo];
            mResult = y[ilo];
            throw new UtilException("Exceeded the maximum number of iterations in Simplex algorithm.");
         }
         mEvaluationCount += 2;
         double yTry = ooze(p, y, psum, ihi, -1.0);
         if(yTry <= y[ilo])
            yTry = ooze(p, y, psum, ihi, 2.0);
         else if(yTry >= y[inhi]) {
            final double ySave = y[ihi];
            yTry = ooze(p, y, psum, ihi, 0.5);
            if(yTry >= ySave) {
               for(int i = 0; i < mpts; ++i)
                  if(i != ilo) {
                     for(int j = 0; j < nDim; ++j)
                        p[i][j] = (psum[j] = 0.5 * (p[i][j] + p[ilo][j]));
                     y[i] = evaluateFunction(psum);
                  }
               mEvaluationCount += nDim;
               // GET_PSUM
               for(int j = 0; j < nDim; ++j) {
                  double sum = 0.0;
                  for(int i = 0; i < mpts; ++i)
                     sum += p[i][j];
                  psum[j] = sum;
               }
            }
         } else
            --mEvaluationCount;
      }
      return mBest;
   }

   /**
    * Returns the number of evaluations of the fit function performed during the
    * last optimization.
    * 
    * @return int
    */
   public int getEvaluationCount() {
      return mEvaluationCount;
   }

   /**
    * getBestResult - Returns the value at the parameters that produced the
    * smallest value of function in the last iteration of Simplex.perform(..)
    * 
    * @return double
    */
   public double getBestResult() {
      return mResult;
   }

   /**
    * setTolerance - Set the fractional tolerance used to determine when to halt
    * the optimization. (Default 1.0e-8, Min 0.01)
    * 
    * @param t double
    */
   public void setTolerance(double t) {
      t = Math.abs(t);
      assert (t < LARGEST_TOLERANCE);
      mTolerance = (t < LARGEST_TOLERANCE ? t : LARGEST_TOLERANCE);
   }

   /**
    * getTolerance - Gets the fractional tolerance used to determine when to
    * halt the optimization. (Default 1.0e-8)
    * 
    * @return double - The current tolerance
    */
   public double getTolerance() {
      return mTolerance;
   }

   /**
    * setMaxEvaluations - Sets the maximum number of evaluations of function
    * that Simplex.perform will take before aborting the optimization. (default
    * 5000, min 100)
    * 
    * @param n int
    */
   public void setMaxEvaluations(int n) {
      assert (n > MIN_EVALUATIONS);
      mMaxEvaluations = n < MIN_EVALUATIONS ? MIN_EVALUATIONS : n;
   }

   /**
    * getMaxEvaluations - Gets the maximum number of evaluations of function
    * that Simplex.perform will take before aborting the optimization.
    * 
    * @return - The maximum
    */
   public int getMaxEvaluations() {
      return mMaxEvaluations;
   }
}
