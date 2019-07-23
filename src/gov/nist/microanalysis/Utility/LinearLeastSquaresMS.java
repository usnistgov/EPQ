package gov.nist.microanalysis.Utility;

import java.util.Arrays;

import gov.nist.microanalysis.EPQLibrary.EPQException;

/**
 * <p>
 * An implementation of the linear least squares fit that uses Bayesian model
 * selection to trim the number of fit paramters to an optimal number. MS stands
 * for 'model selection'.
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
abstract public class LinearLeastSquaresMS
   extends LinearLeastSquares {

   private int mNParams = Integer.MIN_VALUE;
   private boolean[] mZero = null;
   private double mAMax = Double.MAX_VALUE;
   private double[] mMetric;
   private boolean mOptimize = false;

   /**
    * Constructs a LinearLeastSquareMS. Call setData to specify the data to fit.
    */
   public LinearLeastSquaresMS() {
      super();
   }

   /**
    * Constructs a LinearLeastSquareMS with the specified data set.
    * 
    * @param x double[]
    * @param y double[]
    * @param sig double[]
    */
   public LinearLeastSquaresMS(double[] x, double[] y, double[] sig) {
      super(x, y, sig);
   }

   /**
    * Constructs a LinearLeastSquareMS with the specified data and an assumed
    * error model specified by the <code>computeError</code> function.
    * 
    * @param x double[]
    * @param y double[]
    */
   public LinearLeastSquaresMS(double[] x, double[] y) {
      super(x, y);
   }

   /**
    * This function implements a method for retaining the largest singular
    * values while setting the remainder to zero.
    * 
    * @param wi The initial weights
    * @return double[] The new weights
    */
   @Override
   protected double[] editSingularValues(double[] wi) {
      final double[] w = wi.clone();
      // Find the largest weight
      double wMax = -Double.MAX_VALUE;
      for(final double element : w)
         if(element > wMax)
            wMax = element;
      assert wMax >= 0.0;
      // mZero is used to force some fit parameters to zero
      if(mZero != null)
         for(int j = 0; j < w.length; ++j)
            if(mZero[j])
               w[j] = 0.0;
      final double[] dup = w.clone();
      Arrays.sort(dup);
      final int nDrop = fitFunctionCount() - mNParams;
      // Regardless set very small weighted items to zero
      final double thresh = wMax * TOLERANCE;
      // Set the smallest nDrop singular values to zero....
      for(int j = 0; j < w.length; ++j)
         if((Arrays.binarySearch(dup, w[j]) < nDrop) || (w[j] < thresh))
            w[j] = 0.0;
      return w;
   }

   /**
    * Enhances the perform in LinearLeastSquares to 1) eliminate negative fit
    * parameters 2) select the number of non-zero fit parameters that produces
    * the minimum value of the function computeMetric().
    * 
    * @see gov.nist.microanalysis.Utility.LinearLeastSquares#perform()
    */
   @Override
   protected void perform()
         throws EPQException {
      if(mNParams == Integer.MIN_VALUE) {
         mNParams = fitFunctionCount();
         if(mOptimize) {
            // Compute the assumed prior for the fitting parameters. This
            // implementation
            // is not likely to be optimal but serves as a starting point.
            final int[] maxCh = new int[mNParams];
            final double[] maxFF = new double[mNParams];
            fitFunction(mXCoordinate[0], maxFF);
            {
               final double[] ff = new double[mNParams];
               for(int ch = 1; ch < mXCoordinate.length; ++ch) {
                  fitFunction(mXCoordinate[ch], ff);
                  for(int i = 0; i < mNParams; ++i)
                     if(ff[i] > maxFF[i]) {
                        maxCh[i] = ch;
                        maxFF[i] = ff[i];
                     }
               }
            }
            mAMax = mData[maxCh[0]] / maxFF[0];
            for(int i = 1; i < mNParams; ++i)
               if((mData[maxCh[i]] / maxFF[i]) > mAMax)
                  mAMax = mData[maxCh[i]] / maxFF[i];
         }
         mZero = null;
         int zeroCx = 0;
         {
            super.perform();
            // Fit parameters that are less than zero are non-physical so
            // they should be zeroed.
            mZero = new boolean[mFitCoefficients.length];
            for(int j = 0; j < mFitCoefficients.length; ++j) {
               mZero[j] = (mFitCoefficients[j].doubleValue() < 0.0);
               if(mZero[j])
                  ++zeroCx;
            }
         }
         if(mOptimize) {
            // Recompute fit paramters...
            mMetric = new double[fitFunctionCount() - zeroCx];
            int minMetric = 0;
            mMetric[minMetric] = Double.MAX_VALUE;
            for(int i = 1; i < mMetric.length; ++i) {
               mNParams = i;
               reevaluate();
               super.perform();
               final double m = computeMetric();
               if(m < mMetric[minMetric])
                  minMetric = i;
               mMetric[i] = m;
            }
            // Revaluate using the best setting for mNParams
            mNParams = minMetric;
         }
      }
      reevaluate();
      super.perform();
   }

   @Override
   protected void reevaluateAll() {
      mNParams = Integer.MIN_VALUE;
      super.reevaluateAll();
   }

   /**
    * Computes the Bayesian model selection metric suggested by eqn 4.20 in
    * "Data Analysis: A Bayesian Tutorial" by D. S. Sivia (ISBN: 0-19-851762-9)
    * 
    * @return double
    * @throws EPQException
    */
   protected double computeMetric()
         throws EPQException {
      final double d = covariance().det();
      return (Math.pow((4.0 * Math.PI) / mAMax, mNParams) * Math.exp(-0.5 * chiSquared())) / Math.sqrt(d);
   }

   /**
    * Determines whether a Bayesian model selection optimization of the number
    * of fit parameters is performed.
    * 
    * @return Returns the optimize.
    */
   public boolean isOptimize() {
      return mOptimize;
   }

   /**
    * Determines whether a Bayesian model selection optimization of the number
    * of fit parameters is performed. Setting this parameter will force the
    * recomputation of all fit parameters.
    * 
    * @param optimize true to perform a Bayesian model selection optimization;
    *           false otherwise
    */
   public void setOptimize(boolean optimize) {
      if(mOptimize != optimize) {
         mOptimize = optimize;
         reevaluateAll();
      }
   }
}
