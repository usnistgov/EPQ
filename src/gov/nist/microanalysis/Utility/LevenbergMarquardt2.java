package gov.nist.microanalysis.Utility;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import gov.nist.microanalysis.EPQLibrary.EPQException;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * Fit m parameters to n data items using the non-linear Levenberg-Marquardt
 * iterative algorithm. Users must implement the FitFunction interface to
 * compute the partial derivatives (Jacobian) and the fit function.
 */
public class LevenbergMarquardt2 {

   public interface FitFunction {
      /**
       * Computes the partial derivative matrix (the Jacobian) associated with
       * the fit function and the fit parameters.
       * 
       * @param params
       *           A m x 1 Matrix containing the fit function parameters
       * @return Matrix An n x m Matrix containing the Jacobian (partials)
       */
      Matrix partials(Matrix params);

      /**
       * Computes the fit function as a function of the fit parameters.
       * 
       * @param params
       *           A m x 1 Matrix containing the fit function parameters
       * @return A n x 1 matrix containing the fit function values at each
       */
      Matrix compute(Matrix params);
   }

   /**
    * <p>
    * Implements the <code>partials(...)</code> method in the FitFunction
    * interface by calling <code>compute(...)</code> N_PARAMS+1 times to
    * estimate the partial derivatives.
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    * 
    * @author nicholas
    * @version 1.0
    */
   public static abstract class AutoPartialsFitFunction implements FitFunction {
      final private double DELTA;
      private double[] mDelta;

      protected AutoPartialsFitFunction(double delta) {
         super();
         DELTA = delta;
      }

      protected AutoPartialsFitFunction() {
         super();
         DELTA = 1.0e-8;
      }

      /**
       * Computes the partial derivative matrix (the Jacobian) associated with
       * the fit function and the fit parameters.
       * 
       * @param params
       *           A m x 1 Matrix containing the fit function parameters
       * @return Matrix An n x m Matrix containing the Jacobian (partials)
       */
      @Override
      public Matrix partials(Matrix params) {
         if (mDelta == null) {
            mDelta = new double[params.getRowDimension()];
            for (int i = 0; i < mDelta.length; ++i)
               mDelta[i] = DELTA;
         }
         final Matrix c = compute(params);
         final Matrix res = new Matrix(c.getRowDimension(), params.getRowDimension());
         // offset replicates params but is incremented by mDelta
         final Matrix offset = params.copy();
         for (int p = params.getRowDimension() - 1; p >= 0; --p) {
            // Update one parameter by mDelta
            final double v1 = params.get(p, 0);
            final double v2 = (v1 == 0.0 ? mDelta[p] : v1 * (1.0 + mDelta[p]));
            offset.set(p, 0, v2);
            // Recompute the fit function
            final Matrix c2 = compute(offset);
            // Compute the partial estimate
            for (int ch = c.getRowDimension() - 1; ch >= 0; --ch)
               res.set(ch, p, (c2.get(ch, 0) - c.get(ch, 0)) / (v2 - v1));
            // Restore the previous parameter
            offset.set(p, 0, v1);
         }
         return res;
      }

      public void setDelta(double[] delta) {
         mDelta = delta.clone();
      }
   }

   /**
    * <p>
    * An object containing a fit result and associated metrics.
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    * 
    * @author nicholas
    * @version 1.0
    */
   public class FitResult {
      protected double mChiSq;
      protected UncertainValue2[] mBestParams;
      protected double[] mBestY;
      protected Matrix mCovariance;
      protected int mIterCount;
      protected int mImproveCount;
      protected final FitFunction mFunction;

      FitResult(FitFunction ff) {
         mFunction = ff;
      }

      /**
       * Returns the parameters (with uncertainty measures) that produced the
       * lowest chi-square as UncertainValues
       * 
       * @return Returns the bestParams.
       */
      public UncertainValue2[] getBestParametersU() {
         return mBestParams;
      }

      /**
       * Returns the parameters that produced the lowest chi-square
       * 
       * @return Returns the bestParams.
       */
      public double[] getBestParameters() {
         final double[] res = new double[mBestParams.length];
         for (int i = 0; i < res.length; ++i)
            res[i] = mBestParams[i].doubleValue();
         return res;
      }

      /**
       * Returns the best fit values.
       * 
       * @return Returns the bestY.
       */
      public double[] getBestFitValues() {
         return mBestY;
      }

      /**
       * Returns the chi-squared statistic.
       * 
       * @return Returns the chiSq.
       */
      public double getChiSquared() {
         return mChiSq;
      }

      /**
       * Returns the covariance matrix.
       * 
       * @return Returns the covariance.
       */
      public Matrix getCovariance() {
         return mCovariance;
      }

      /**
       * Returns the number of iterations to a stable fit.
       * 
       * @return Returns the iterCount.
       */
      public int getIterationCount() {
         return mIterCount;
      }

      public int getImproveCount() {
         return mImproveCount;
      }

      public LevenbergMarquardt2 getModel() {
         return LevenbergMarquardt2.this;
      }

      public LevenbergMarquardt2.FitFunction getFitFunction() {
         return mFunction;
      }
   }

   private final double mEps1 = 1.0e-15;
   private final double mEps2 = 1.0e-6;
   private final double mEps3 = 1.0e-15;
   private final double mTau = 1.0e-3;
   private int mKMax = 100;
   private int mIteration;
   private final ArrayList<ActionListener> mListeners = new ArrayList<ActionListener>();

   /**
    * Computes the a matrix = j.transpose().times(j). This matrix is symmetric
    * and can be computed more efficiently by taking advantage of this fact.
    */
   private Matrix jTj(Matrix j, Matrix sigma) {
      final int n = j.getRowDimension();
      final int m = j.getColumnDimension();
      final Matrix a = new Matrix(m, m);
      for (int im1 = 0; im1 < m; ++im1)
         for (int im2 = im1; im2 < m; ++im2) {
            double v = 0.0;
            for (int in = 0; in < n; ++in)
               v += (j.get(in, im1) * j.get(in, im2)) / Math2.sqr(sigma.get(in, 0));
            a.set(im1, im2, v);
            a.set(im2, im1, v);
         }
      return a;
   }

   /**
    * Compute g = j.transpose().times(eps)
    * 
    * @param j
    *           A n x m Jacobian matrix
    * @param eps
    *           A n x 1 vector of fit residuals
    * @param sigma
    *           A n x 1 matrix of error estimates
    * @returns j.transpose().times(eps)
    */
   private Matrix g(Matrix j, Matrix eps, Matrix sigma) {
      final int n = j.getRowDimension();
      final int m = j.getColumnDimension();
      assert eps.getRowDimension() == n;
      final Matrix g = new Matrix(m, 1);
      for (int i = 0; i < m; ++i) {
         double v = 0.0;
         for (int k = 0; k < n; ++k)
            v += (j.get(k, i) * eps.get(k, 0)) / sigma.get(k, 0);
         g.set(i, 0, v);
      }
      return g;
   }

   /**
    * Computes the weighted difference between the data and the fit.
    * 
    * @param yData
    *           The data (n x 1)
    * @param fp
    *           The fit (n x 1)
    * @param sigma
    *           The error scale (n x 1)
    * @return An n x 1 matrix
    */
   private Matrix eps(Matrix yData, Matrix fp, Matrix sigma) {
      final int n = yData.getRowDimension();
      final Matrix res = new Matrix(n, 1);
      for (int i = 0; i < n; ++i)
         res.set(i, 0, (yData.get(i, 0) - fp.get(i, 0)) / sigma.get(i, 0));
      return res;
   }

   /**
    * Solve the eigen equation (a + I*mu)*deltaP=g given <code>a</code>,
    * <code>mu</code> and <code>g</code>.
    * 
    * @param a
    * @param mu
    * @param g
    * @throws EPQException
    */
   private Matrix solve(Matrix a, double mu, Matrix g) throws EPQException {
      if (Double.isNaN(mu))
         throw new EPQException("mu is NaN in LevenbergMarquardt2.solve(a,mu,g)");
      for (int i = 0; i < a.getRowDimension(); ++i) {
         if (Double.isNaN(a.get(i, i)))
            throw new EPQException("a(" + i + "," + i + ") is NaN in LevenbergMarquardt2.solve(a,mu,g)");
         a.set(i, i, a.get(i, i) + mu);
      }
      final SingularValueDecomposition svd = a.svd();
      final Matrix w = updateSingularValues(svd.getS());
      return svd.getV().times(w).times(svd.getU().transpose().times(g));
   }

   private Matrix updateSingularValues(Matrix w) {
      double max = w.get(0, 0);
      for (int i = 1; i < w.getRowDimension(); ++i)
         if (w.get(i, i) > max)
            max = w.get(i, i);
      for (int i = 0; i < w.getRowDimension(); ++i)
         w.set(i, i, w.get(i, i) > (1.0e-10 * max) ? 1.0 / w.get(i, i) : 0.0);
      return w;
   }

   /**
    * Returns the maximum diagonal element in the matrix <code>a</code>.
    * 
    * @param a
    * @return double
    */
   private double maxDiagonal(Matrix a) {
      double max = a.get(0, 0);
      for (int i = 1; i < a.getRowDimension(); ++i)
         if (a.get(i, i) > max)
            max = a.get(i, i);
      return max;
   }

   /**
    * Compute chiSq from the difference of the data and the fit function divided
    * by a weighting factor (as eps)
    * 
    * @param eps
    * @return chiSq
    */
   private double chiSqr(Matrix eps) {
      double chiSq = 0.0;
      for (int r = 0; r < eps.getRowDimension(); ++r)
         chiSq += Math2.sqr(eps.get(r, 0));
      return chiSq;
   }

   public int getIteration() {
      return mIteration;
   }

   public void addActionListener(ActionListener al) {
      mListeners.add(al);
   }

   /**
    * Computes the best set of parameters to fit the data <code>yData</code>
    * with associated errors <code>yErr</code> with the fit function provided by
    * <code>ff</code>. <code>p0</code> is the starting parameters which should
    * provide a good estimate of the best parameters to ensure convergence.
    * 
    * @param ff
    *           FitFunction interface
    * @param yData
    *           Array of dependent data items (Length n)
    * @param sigma
    *           Array of errors associated with yData (Length n)
    * @param p0
    *           Array of initial parameters (Length m)
    * @return {@link FitResult}
    * @throws EPQException
    */
   public FitResult compute(FitFunction ff, Matrix yData, Matrix sigma, Matrix p0) throws EPQException {
      final int m = p0.getRowDimension();
      final int n = yData.getRowDimension();
      assert n > m : "There must be more data points than fit parameters.";
      assert yData.getColumnDimension() == 1;
      assert sigma.getColumnDimension() == 1;
      assert sigma.getRowDimension() == n;
      assert Math2.min(sigma.getColumnPackedCopy()) > 0.0;
      double nu = 2.0;
      Matrix p = p0;
      Matrix j = ff.partials(p); // n x m -> Jacobian
      Matrix a = jTj(j, sigma); // m x m = jTj/(yErr^2)
      Matrix fp = ff.compute(p); // n x 1
      Matrix epsP = eps(yData, fp, sigma); // n x 1
      double chiSq = chiSqr(epsP);
      Matrix g = g(j, epsP, sigma); // m x 1
      double mu = mTau * maxDiagonal(a);
      mIteration = 0;
      int improveCx = 0;
      boolean stop1 = g.normInf() < mEps1, stop2 = false, stop3 = false;
      while ((!(stop1 || stop2 || stop3)) && (mIteration < mKMax)) {
         ++mIteration;
         double rho = -1.0;
         do {
            // Solve (A+mu*Matrix.identity(m))*deltaP=g for deltaP using SVD
            final Matrix deltaP = solve(a, mu, g);
            // Test stop conditions
            stop2 = (deltaP.normF() <= (mEps2 * p.normF())); // norm ok
            if (!(stop1 || stop2 || stop3)) {
               final Matrix pNew = p.plus(deltaP);
               fp = ff.compute(pNew);
               final Matrix epsPnew = eps(yData, fp, sigma);
               final Matrix den = deltaP.transpose().times(deltaP.times(mu).plus(g));
               assert den.getRowDimension() == 1;
               assert den.getColumnDimension() == 1;
               assert den.get(0, 0) > 0.0;
               final double newChiSq = chiSqr(epsPnew);
               rho = (chiSq - newChiSq) / den.get(0, 0);
               if (rho > 0.0) {
                  ++improveCx;
                  p = pNew;
                  j = ff.partials(p);
                  a = jTj(j, sigma);
                  epsP = epsPnew;
                  chiSq = newChiSq;
                  g = g(j, epsP, sigma);
                  stop1 = (g.normInf() <= mEps1);
                  stop3 = (chiSqr(epsP) <= mEps3); // ok
                  mu = mTau * maxDiagonal(a);
                  mu *= Math.max(1.0 / 3.0, 1.0 - Math.pow((2.0 * rho) - 1.0, 3.0));
                  nu = 2.0;
               } else if (rho == 0.0)
                  stop1 = true;
               else {
                  mu *= nu;
                  nu *= 2.0;
               }
            }
            for (int i = mListeners.size() - 1; i >= 0; --i)
               mListeners.get(i).actionPerformed(new ActionEvent(this, (100 * mIteration) / mKMax, null));
         } while ((rho < 0.0) && (!(stop1 || stop2 || stop3)));
      }
      final FitResult res = new FitResult(ff);
      {
         j = ff.partials(p);
         a = jTj(j, sigma);
         // Solve A*deltaP=g for deltaP using SVD
         SingularValueDecomposition svd;
         svd = a.svd();
         final Matrix w = updateSingularValues(svd.getS());
         final Matrix covar = svd.getV().times(w).times(svd.getU().transpose());
         res.mBestY = ff.compute(p).getColumnPackedCopy();
         res.mChiSq = chiSq;
         res.mIterCount = mIteration;
         res.mImproveCount = improveCx;
         res.mCovariance = covar;
         res.mBestParams = new UncertainValue2[p.getRowDimension()];
         for (int i = 0; i < res.mBestParams.length; ++i) {
            final double c = covar.get(i, i);
            res.mBestParams[i] = new UncertainValue2(p.get(i, 0), "LM", Math.sqrt(Math.abs(c)));
         }

      }
      return res;
   }

   public int getMaxIterations() {
      return mKMax;
   }

   public void setMaxIterations(int max) {
      mKMax = max;
   }
}