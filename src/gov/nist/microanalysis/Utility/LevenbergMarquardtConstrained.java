package gov.nist.microanalysis.Utility;

import gov.nist.microanalysis.EPQLibrary.EPQException;

import Jama.Matrix;

/**
 * <p>
 * This class makes it realatively simple to perform constrained least squares
 * fitting. You implement the ConstrainedFitFunction class and specify the
 * constraints. The
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
public class LevenbergMarquardtConstrained
   extends LevenbergMarquardt2 {

   /**
    * <p>
    * You want to work within your domain parameter space in which the
    * parameters may be constrained to fall within ranges (positive, negative,
    * bounded), etc.. However {@link LevenbergMarquardt2} works over the full
    * real number space (-&infin;,&infin;). {@link ConstrainedFitFunction} is a
    * mechanism for mapping (-&infin;,&infin;) onto a bounded region through the
    * helper class Constraint. You provide input parameters in your domain
    * parameter space. {@link ConstrainedFitFunction} takes a FitFunction and
    * transforms it as necessary to perform the fit over the (-&infin;,&infin;).
    * </p>
    * <p>
    * The one caveat is that the results returned by <code>compute(...)</code>
    * are in the transformed parameter space. You will need to use
    * <code>transform</code> to translate the best fit parameters from
    * (-&infin;,&infin;) to the domain restricted space.
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
    * @author nritchie
    * @version 1.0
    */
   public static class ConstrainedFitFunction
      implements FitFunction {

      private final Constraint[] mConstraints;
      private final FitFunction mFitFunction;

      /**
       * Constructs a ConstrainedFitFunction in which the <code>paramDim</code>
       * parameters are unconstrained (<code>Constraint.None</code>).
       * 
       * @param ff The fit function assuming domain space parameters.
       * @param paramDim The number of parameters in the fit
       */
      public ConstrainedFitFunction(FitFunction ff, int paramDim) {
         mConstraints = new Constraint[paramDim];
         for(int i = 0; i < paramDim; ++i)
            mConstraints[i] = new Constraint.None();
         mFitFunction = ff;
      }

      /**
       * Specify the constraint associated with the <code>paramIdx</code>
       * <sup>th</sup> parameter.
       * 
       * @param paramIdx int
       * @param c {@link Constraint}
       */
      public void setConstraint(int paramIdx, Constraint c) {
         mConstraints[paramIdx] = c;
      }

      public Matrix realToConstrained(Matrix rParams) {
         assert rParams.getColumnDimension() == 1;
         final Matrix params = new Matrix(rParams.getRowDimension(), 1);
         for(int i = rParams.getRowDimension() - 1; i >= 0; --i)
            params.set(i, 0, mConstraints[i].realToConstrained(rParams.get(i, 0)));
         return params;
      }

      public Matrix constrainedToReal(Matrix rParams) {
         assert rParams.getColumnDimension() == 1;
         final Matrix params = new Matrix(rParams.getRowDimension(), 1);
         for(int i = rParams.getRowDimension() - 1; i >= 0; --i)
            params.set(i, 0, mConstraints[i].constrainedToReal(rParams.get(i, 0)));
         return params;
      }

      @Override
      public Matrix partials(Matrix rParams) {
         final Matrix tmp = mFitFunction.partials(realToConstrained(rParams));
         assert tmp.getColumnDimension() == rParams.getRowDimension();
         for(int c = 0; c < tmp.getColumnDimension(); ++c) {
            final double dp = mConstraints[c].derivative(rParams.get(c, 0));
            for(int r = 0; r < tmp.getRowDimension(); ++r)
               tmp.set(r, c, tmp.get(r, c) * dp);
         }
         return tmp;
      }

      @Override
      public Matrix compute(Matrix rParams) {
         return mFitFunction.compute(realToConstrained(rParams));
      }

      /**
       * Transform the FitResults from (-&infin;,&infin;) to the constrained
       * problem space domain.
       * 
       * @param fr {@link FitResult}
       * @return FitResult
       */
      public FitResult realToConstrained(FitResult fr) {
         final FitResult res = fr.getModel().new FitResult(mFitFunction);
         res.mBestParams = new UncertainValue2[mConstraints.length];
         for(int i = 0; i < mConstraints.length; ++i)
            res.mBestParams[i] = mConstraints[i].getResult(fr.mBestParams[i]);
         res.mBestY = fr.mBestY.clone();
         res.mChiSq = fr.mChiSq;
         {
            final Matrix covar = fr.mCovariance.copy();
            final double[] dp = new double[mConstraints.length];
            for(int i = 0; i < dp.length; ++i)
               dp[i] = mConstraints[i].derivative(fr.mBestParams[i].doubleValue());
            for(int c = 0; c < covar.getColumnDimension(); ++c)
               for(int r = 0; r < covar.getRowDimension(); ++r)
                  covar.set(r, c, fr.mCovariance.get(r, c) * dp[r] * dp[c]);
            res.mCovariance = covar;
         }
         res.mImproveCount = fr.mImproveCount;
         res.mIterCount = fr.mIterCount;
         return res;
      }
   }

   /**
    * Constructs a LevenbergMarquardtConstrained
    */
   public LevenbergMarquardtConstrained() {
      super();
   }

   @Override
   public FitResult compute(FitFunction ff, Matrix yData, Matrix sigma, Matrix p0)
         throws EPQException {
      if(ff instanceof ConstrainedFitFunction) {
         final ConstrainedFitFunction cff = (ConstrainedFitFunction) ff;
         final FitResult tmp = super.compute(cff, yData, sigma, cff.constrainedToReal(p0));
         return cff.realToConstrained(tmp);
      } else
         return super.compute(ff, yData, sigma, p0);
   }
}
