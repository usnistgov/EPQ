package gov.nist.microanalysis.Utility;

import java.util.Arrays;

import gov.nist.microanalysis.EPQLibrary.EPQException;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * <p>
 * The object computes the linear least squares fit in the constructor and
 * provides methods to return the fit parameters, the covariances etc. The class
 * is abstract. Implement fitFunction to specify the m different functions that
 * are to be fit to the data. Implement error to assign an error measure to each
 * data point.
 * </p>
 * <p>
 * The implementation uses singular value decomposition (SVD) which is likely to
 * be use more memory and more CPU than the fastest algorithm but is extremely
 * robust. SVD is also very flexible and when many permutations of parameters
 * are considered it may actually turn out to be the most efficient.
 * </p>
 * <p>
 * This implementation is designed to be extended. It divides the computation of
 * the SVD from the computation of the fit parameters so that the weights on the
 * SVD can be modified without recomputing the whole SVD.
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

abstract public class LinearLeastSquares {
   static protected final double TOLERANCE = 1.0e-12;

   protected double[] mXCoordinate;
   protected double[] mData;
   protected double[] mSigma;
   protected UncertainValue2[] mFitCoefficients;
   protected Matrix mCovariance;
   private SingularValueDecomposition mSVD;
   private boolean[] mZeroThese;
   private static final double MAX_ERROR = Double.MAX_VALUE;

   /**
    * perform - Actually performs the LinearLeastSquares fit. Evaluated lazily.
    * You don't need to call this directly as fitParameters(), variances(),
    * covarianceMatrix() call it as necessary.
    * 
    * @throws EPQException
    */
   protected void perform()
         throws EPQException {
      if(mFitCoefficients == null) {
         if((mXCoordinate == null) || (mData == null) || (mSigma == null))
            throw new IllegalArgumentException("No data specified for the linear least squares fit.");
         final int nTot = fitFunctionCount();
         final int nFit = getNonZeroedCoefficientCount();
         // Allocate an array showing the location of non-zeroed coefficients
         final int[] nzIndex = new int[nFit];
         for(int j = 0, k = 0; j < nTot; ++j)
            if(!isZeroFitCoefficient(j)) {
               nzIndex[k] = j;
               ++k;
            }
         final int dataLen = mXCoordinate.length;
         if(mSVD == null) {
            // the "design matrix"
            final Matrix a = new Matrix(dataLen, nFit);
            final double[] afunc = new double[nTot];
            for(int i = 0; i < dataLen; ++i) {
               assert mSigma[i] >= 0.0 : Double.toString(mSigma[i]);
               fitFunction(mXCoordinate[i], afunc);
               for(int j = 0; j < nFit; ++j) {
                  final double val = afunc[nzIndex[j]] / Math.max(mSigma[i], 1.0e-20);
                  assert !(Double.isNaN(val) || Double.isInfinite(val)) : val;
                  a.set(i, j, val);
               }
            }
            mSVD = a.svd();
            // Check that mSVD really solves the linear equation defined by a
            assert (a.minus(mSVD.getU().times(mSVD.getS().times(mSVD.getV().transpose()))).norm1() / a.norm1()) < 1.0e-6;
         }
         final Matrix u = mSVD.getU(), s = mSVD.getS(), v = mSVD.getV();
         assert (u.getRowDimension() == dataLen) && (u.getColumnDimension() == nFit);
         assert (s.getRowDimension() == nFit) && (s.getColumnDimension() == nFit);
         assert (v.getRowDimension() == nFit) && (v.getColumnDimension() == nFit);
         // Edit singular values
         final double[] w = new double[nFit];
         {
            double[] wi = new double[nTot];
            for(int i = 0; i < nFit; ++i)
               wi[nzIndex[i]] = s.get(i, i);
            wi = editSingularValues(wi);
            for(int i = 0; i < nFit; ++i)
               w[i] = wi[nzIndex[i]];
         }
         // Compute the covariance matrix
         mCovariance = new Matrix(nTot, nTot);
         {
            final double[] wti = new double[nFit];
            for(int i = 0; i < nFit; ++i)
               wti[i] = (w[i] != 0.0 ? 1.0 / (w[i] * w[i]) : 0.0);
            for(int j = 0; j < nFit; ++j)
               for(int k = 0; k <= j; ++k) {
                  double sum = 0.0;
                  for(int i = 0; i < nFit; ++i)
                     sum += v.get(j, i) * v.get(k, i) * wti[i];
                  mCovariance.set(nzIndex[j], nzIndex[k], sum);
                  mCovariance.set(nzIndex[k], nzIndex[j], sum);
               }
         }
         final double[] fcs = new double[nFit];
         {
            final double[] b = new double[dataLen];
            for(int i = 0; i < dataLen; ++i)
               b[i] = mData[i] / mSigma[i];
            // Compute fit coefficients
            for(int k = 0; k < nFit; ++k) {
               double fc = 0.0;
               for(int i = 0; i < nFit; ++i)
                  if(w[i] != 0.0) {
                     double dot = 0.0;
                     for(int j = 0; j < dataLen; ++j)
                        dot += u.get(j, i) * b[j];
                     fc += (dot / w[i]) * v.get(k, i);
                  }
               fcs[k] = fc;
            }
         }
         mFitCoefficients = new UncertainValue2[nTot];
         Arrays.fill(mFitCoefficients, UncertainValue2.ZERO);
         final double[] expU = confidenceIntervals(INTERVAL_MODE.ONE_D_INTERVAL, 0.683, mCovariance);
         for(int j = 0; j < nFit; ++j)
        	 mFitCoefficients[nzIndex[j]] = new UncertainValue2(fcs[j], "LLS", expU[nzIndex[j]]);
      }
   }

   /**
    * This function implements a simple default method for setting 1/w[i] to 0
    * depending upon the relative w[i] compared with the other w[*]. Override
    * this function to provide a more sophisticated method.
    * 
    * @param wi The initial values
    * @return double[] The edited values
    */
   protected double[] editSingularValues(double[] wi) {
      final double[] w = wi.clone();
      // Set the singular values to zero....
      double wMax = 0.0;
      for(final double element : w)
         if(element > wMax)
            wMax = element;
      final double thresh = wMax * TOLERANCE;
      for(int j = 0; j < w.length; ++j)
         if(w[j] < thresh)
            w[j] = 0.0;
      return w;
   }

   /**
    * LinearLeastSquares - Creates a blank LinearLeastSquares object to which
    * data will be later assigned using setData.
    */
   public LinearLeastSquares() {
      super();
   }

   /**
    * LinearLeastSquares - Construct an object that represents the linear least
    * squares fit of the abstract fitFunction to the data in x &amp; y.
    * 
    * @param x double[] - The abscissa
    * @param y double[] - The ordinate
    * @param sig double[] - The error for each ordinate value respectively.
    */
   public LinearLeastSquares(double[] x, double[] y, double[] sig) {
      super();
      setData(x, y, sig);
   }

   /**
    * LinearLeastSquares - Create a linear least squares object. Assume that the
    * errors in the measured parameter y are all due to counting statistics and
    * can be modeled as the square root of the signal size.
    * 
    * @param x double[]
    * @param y double[]
    */
   public LinearLeastSquares(double[] x, double[] y) {
      this(x, y, null);
   }

   /**
    * setData - Set the data to be fit. Setting new data will cause everything
    * to be recalculated.
    * 
    * @param x double[]
    * @param y double[]
    * @param sig double[] - The error associated with the data in the argument
    *           y.
    */
   public void setData(double[] x, double[] y, double[] sig) {
      assert (y.length == x.length);
      assert (sig == null) || (sig.length == x.length);
      int cx = 0;
      if(sig == null)
         cx = y.length;
      else
         for(final double element : sig)
            if(element < 1.0e300)
               ++cx;
      if(cx < y.length) {
         mXCoordinate = new double[cx];
         mData = new double[cx];
         mSigma = new double[cx];
         for(int i = 0, j = 0; i < sig.length; ++i)
            if(sig[i] < 1.0e300) {
               mXCoordinate[j] = x[i];
               mData[j] = y[i];
               mSigma[j] = sig[i];
               ++j;
            }
      } else {
         mXCoordinate = x;
         mData = y;
         mSigma = sig;
      }
      reevaluateAll();
   }

   /**
    * setData - Set the data to be fit. Errors are assumed to be those
    * associated with the implementation in <code>computeErrors</code> which is
    * by default counting statistics (sigma[i]=sqrt(y[i])). Setting new data
    * will cause everything to be recalculated.
    * 
    * @param x double[]
    * @param y double[]
    */
   public void setData(double[] x, double[] y) {
      setData(x, y, null);
   }

   /**
    * fitParameters - Returns m fit parameters. Automatically performs the LLSQ
    * fit if it has not previous been performed.
    * 
    * @return double[]
    * @throws EPQException
    */
   public double[] fitParameters()
         throws EPQException {
      if(mFitCoefficients == null)
         perform();
      final double[] res = new double[mFitCoefficients.length];
      for(int i = 0; i < res.length; ++i)
         res[i] = mFitCoefficients[i].doubleValue();
      return res;
   }

   /**
    * Returns the fit parameters and the associated errors as UncertainValue
    * objects. Automatically performs the LLSQ fit if it has not previously been
    * performed.
    * 
    * @return UncertainValue[]
    * @throws EPQException
    */
   public UncertainValue2[] getResults()
         throws EPQException {
      if(mFitCoefficients == null)
         perform();
      return mFitCoefficients;
   }

   /**
    * Returns the i-th fit parameter.
    * 
    * @param i parameter index
    * @return double value
    * @throws EPQException
    */
   public double fitParamter(int i)
         throws EPQException {
      if(mFitCoefficients == null)
         perform();
      return mFitCoefficients[i].doubleValue();
   }

   /**
    * Returns an array of m error metrics for the m fit parameters.
    * Automatically performs the LLSQ fit if it has not previous been performed.
    * 
    * @return double[]
    * @throws EPQException
    */
   public double[] errors()
         throws EPQException {
      if(mFitCoefficients == null)
         perform();
      final double[] res = new double[mCovariance.getRowDimension()];
      for(int i = 0; i < res.length; ++i)
         res[i] = Math.sqrt(mCovariance.get(i, i));
      return res;
   }

   /**
    * Returns the covariance matrix. Automatically performs the LLSQ fit if it
    * has not previous been performed. The size of the matrix is
    * fitFunctionCount() x fitFunctionCount().
    * 
    * @return Matrix
    * @throws EPQException
    */
   public Matrix covariance()
         throws EPQException {
      if(mFitCoefficients == null)
         perform();
      return mCovariance;
   }

   public static double chiSqr(int degsOfFree, double prob) {
      double min = 0.1, max = 100.0;
      double minV = 1.0 - Math2.gammq(0.5 * degsOfFree, 0.5 * min);
      double maxV = 1.0 - Math2.gammq(0.5 * degsOfFree, 0.5 * max);
      assert minV < prob;
      assert maxV > prob;
      while(Math.abs(max - min) > 0.01) {
         final double test = 0.5 * (max + min);
         final double testV = 1.0 - Math2.gammq(0.5 * degsOfFree, 0.5 * test);
         if(testV > prob) {
            max = test;
            maxV = testV;
         } else if(testV < prob) {
            min = test;
            minV = testV;
         }
      }
      return Math.abs(minV - prob) < Math.abs(maxV - prob) ? min : max;
   }

   public enum INTERVAL_MODE {
      /**
       * The standard confidence interval when taking each dimension
       * independently (sqrt(k*cov[i,i]))
       */
      ONE_D_INTERVAL,
      /**
       * The confidence interval from considering the outer extent along the
       * specified axis of the error ellipse with the specified probability
       * bounds.
       */
      JOINT_INTERVAL
   };

   /**
    * Calculates the confidence intervals for each parameter in the fit.
    * 
    * @param mode Either ONE_D_INTERVAL or JOINT_INTERVAL
    * @param prob 0.683 for "1 sigma" etc.
    * @return An array of double containing the confidence intervals
    * @throws EPQException
    */
   public double[] confidenceIntervals(INTERVAL_MODE mode, double prob, Matrix cov)
         throws EPQException {
      final double[] res = new double[cov.getRowDimension()];
      switch(mode) {
         case ONE_D_INTERVAL: {
            // The standard 1D recipe
            final double k = prob==0.683 ? 1.0 : chiSqr(1, prob);
            for(int i = 0; i < res.length; ++i)
               res[i] = Math.sqrt(k * cov.get(i, i));
            break;
         }
         case JOINT_INTERVAL: {
            final Matrix ci = cov.inverse();
            final double k = chiSqr(res.length, prob);
            final double d = ci.det();
            final int subDim = res.length - 1;
            final Matrix sub = new Matrix(subDim, subDim);
            for(int i = 0; i < res.length; ++i) {
               for(int r = 0; r < subDim; ++r)
                  for(int c = 0; c < subDim; ++c)
                     sub.set(r, c, ci.get(r < i ? r : r + 1, c < i ? c : c + 1));
               res[i] = Math.sqrt(Math.abs((k * sub.det()) / d));
            }
            break;
         }
      }
      return res;
   }

   /**
    * Returns the corrolation matrix between fit parameters. The diagonal is
    * always 1.0. The off diagonal terms are 0 for no corrolation, -1 for
    * perfect anti-corrolation and +1 for perfect corrolation.
    * 
    * @return Matrix
    * @throws EPQException
    */
   public Matrix correlation()
         throws EPQException {
      if(mFitCoefficients == null)
         perform();
      final Matrix res = mCovariance.copy();
      for(int r = 0; r < res.getRowDimension(); ++r) {
         res.set(r, r, 1.0);
         for(int c = r + 1; c < res.getColumnDimension(); ++c) {
            final double cv = mCovariance.get(r, c);
            final double corr = cv != 0.0 ? cv / Math.sqrt(mCovariance.get(r, r) * mCovariance.get(c, c)) : 0.0;
            assert corr >= -1.0 : "Correlation is " + Double.toString(corr);
            assert corr <= 1.0 : "Correlation is " + Double.toString(corr);
            res.set(r, c, corr);
            res.set(c, r, corr);
         }
      }
      return res;
   }

   /**
    * Returns chi-squared metric for the best fit parameters.
    * 
    * @return double
    * @throws EPQException
    */
   public double chiSquared()
         throws EPQException {
      return chiSquared(fitParameters());
   }

   /**
    * Computes the reduced chi-square metric based on the desired confidence
    * level and the number of degrees of freedom.
    * 
    * @param confidenceLevel - Use 0.683 for 1 sigma, 0.954 for 2 sigma etc. on
    *           range (0,1).
    * @return double
    * @throws EPQException
    */
   public double reducedChiSquared(double confidenceLevel)
         throws EPQException {
      final double[] fp = fitParameters();
      // Count the number of degrees of freedom
      int dof = 0;
      for(final double element : mSigma)
         if(element != MAX_ERROR)
            ++dof;
      for(final double element : fp)
         if(element != 0.0)
            --dof;
      assert dof > 0;
      return chiSquared(fp) / Math2.chiSquaredConfidenceLevel(confidenceLevel, dof);
   }

   /**
    * Computes the chiSquared metric for the specified fitCoefficient array.
    * 
    * @param fitCoeff - double[fitFunctionCount()]
    * @return double
    */
   protected double chiSquared(double[] fitCoeff) {
      final int n = fitFunctionCount();
      assert fitCoeff.length == n;
      final double[] ff = new double[n];
      double chi2 = 0.0;
      for(int ch = 0; ch < mXCoordinate.length; ++ch) {
         fitFunction(mXCoordinate[ch], ff);
         double y = 0.0;
         for(int j = 0; j < n; ++j)
            y += fitCoeff[j] * ff[j];
         chi2 += Math2.sqr((y - mData[ch]) / mSigma[ch]);
      }
      return chi2;
   }

   /**
    * Computes the chiSquared metric for the specified fitCoefficient array.
    * 
    * @param fitCoeff - UncertainValue[fitFunctionCount()]
    * @return double
    */
   protected double chiSquared(UncertainValue2[] fitCoeff) {
      final int n = fitFunctionCount();
      assert fitCoeff.length == n;
      final double[] ff = new double[n];
      double chi2 = 0.0;
      for(int ch = 0; ch < mXCoordinate.length; ++ch) {
         fitFunction(mXCoordinate[ch], ff);
         double y = 0.0;
         for(int j = 0; j < n; ++j)
            y += fitCoeff[j].doubleValue() * ff[j];
         chi2 += Math2.sqr((y - mData[ch]) / mSigma[ch]);
      }
      return chi2;
   }

   /**
    * reevaluate - Force the fit parameters and variances to be recomputed
    * (lazily). Does not recompute the singular value decomposition so use this
    * method after modifying how editSingularValues works to ensure that fit
    * coefficients and the covariance is computed efficiently.
    */
   protected void reevaluate() {
      mFitCoefficients = null;
      mCovariance = null;
   }

   /**
    * Forces the fit parameters, covariances and singular value decomposition to
    * be recomputed.
    */
   protected void reevaluateAll() {
      mSVD = null;
      reevaluate();
   }

   public void clearZeroedCoefficients() {
      if(mZeroThese != null)
         Arrays.fill(mZeroThese, false);
   }

   /**
    * Specify whether to force the coefficient for the specified fit function to
    * zero.
    * 
    * @param i Index of fit function
    * @param b true to force to zero; false otherwise
    */
   public void zeroFitCoefficient(int i, boolean b) {
      if((mZeroThese == null) && b)
         mZeroThese = new boolean[fitFunctionCount()];
      if((mZeroThese != null) && (mZeroThese[i] != b)) {
         mZeroThese[i] = b;
         reevaluateAll();
      }
   }

   public int getNonZeroedCoefficientCount() {
      if(mZeroThese == null)
         return fitFunctionCount();
      else {
         int cx = 0;
         for(final boolean b : mZeroThese)
            if(!b)
               ++cx;
         return cx;
      }
   }

   /**
    * Determine whether the coefficient for the specified fit function has been
    * forced to zero.
    * 
    * @param i Index of fit function
    * @return boolean true to forced to zero
    */
   public boolean isZeroFitCoefficient(int i) {
      return (mZeroThese != null) && mZeroThese[i];
   }

   public double fitQuality()
         throws EPQException {
      return fitQuality(fitParameters());
   }

   /**
    * Computes a metric of fit quality [GAMMAQ(nu/2,chiSq/2)]. If the fit is
    * good and the model likely fits the data then the metric is close to zero.
    * If the fit is poor and the model does not fit the data then the fit
    * quality is near 1.0.
    * 
    * @param fp double[]
    * @return double
    */
   public double fitQuality(double[] fp) {
      final int dataPtCx = mXCoordinate.length;
      int paramCx = 0;
      for(final double element : fp)
         if(element != 0.0)
            ++paramCx;
      return Math2.gammq(0.5 * (dataPtCx - paramCx), 0.5 * chiSquared(fp));
   }

   /**
    * fitFunctionCount - The number of functions to fit. This will be the same
    * number as the length of the array argument afunc to the abstract method
    * fitFunction.
    * 
    * @return int
    */
   abstract protected int fitFunctionCount();

   /**
    * fitFunction - For the specified abscissa coordinate, computes the m
    * different values of the fit function at the abscissa value xi.
    * 
    * @param xi double - The abscissa coordinate
    * @param afunc double[] - Takes a blank m element double array and returns
    *           the m ordinate values of the fit function.
    */
   abstract protected void fitFunction(double xi, double[] afunc);
};
