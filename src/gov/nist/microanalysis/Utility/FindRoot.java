package gov.nist.microanalysis.Utility;

/**
 * <p>
 * A simple root finder based on Jack Crenshaw's "world's best root finder."
 * Derive a class from this that implements function (and optionally also
 * initialize). Anonymous classes work nicely for this.
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

abstract public class FindRoot {

   private double mBestX, mBestY;
   private int mNEvals = 0;

   /**
    * function - Implement this. This is the function for which the root will be
    * found.
    * 
    * @param x0 double
    * @return double
    */
   abstract public double function(double x0);

   /**
    * initialize - Override this method to perform any initialization required
    * by the implementing class. Declaring this dummy function makes it possible
    * to construct and use anonymous versions of this class that require
    * initialization. Otherwise it would not be possible to call the
    * initialization function through a reference to FindRoot.
    * 
    * @param vars double[]
    */
   public void initialize(double[] vars) {
   }

   /**
    * bestX - Returns the x value closest to the root.
    * 
    * @return double
    */
   public double bestX() {
      return mBestX;
   }

   /**
    * bestY - Returns the y value associated with the best x value.
    * 
    * @return double
    */
   public double bestY() {
      return mBestY;
   }

   public double EvaluationCount() {
      return mNEvals;
   }

   /**
    * perform - Implements the root find algorithm.
    * 
    * @param x0 double - The lower bound of the range
    * @param x2 double - The upper bound of the range
    * @param eps double - The absolute error goal
    * @param iMax double - The maximum number of iterations
    * @throws IllegalArgumentException - If function(x0)*function(x1)&gt;0.0
    * @throws ArithmeticException - If the root finder has not converged after
    *            iMax iterations.
    * @return double
    */
   public double perform(double x0, double x2, double eps, int iMax) {
      double y0, y2, xmlast = x0;
      mNEvals = 1;
      y0 = function(x0);
      if(y0 == 0.0) {
         mBestX = x0;
         mBestY = y0;
         return x0;
      }
      y2 = function(x2);
      ++mNEvals;
      if(y2 == 0.0) {
         mBestX = x2;
         mBestY = y2;
         return x2;
      }
      if((y2 * y0) > 0.0)
         throw new IllegalArgumentException("Input range does not straddle a zero in FindRoot.perform()");
      for(int i = 0; i < iMax; i++) {
         final double x1 = 0.5 * (x2 + x0);
         final double y1 = function(x1);
         ++mNEvals;
         if(y1 == 0.0)
            return x1;
         if(Math.abs(x1 - x0) < eps) {
            mBestX = x1;
            mBestY = y1;
            return x1;
         }
         if((y1 * y0) > 0.0) {
            double temp = x0;
            x0 = x2;
            x2 = temp;
            temp = y0;
            y0 = y2;
            y2 = temp;
         }
         final double y10 = y1 - y0;
         final double y21 = y2 - y1;
         final double y20 = y2 - y0;
         if((y2 * y20) < (2.0 * y1 * y10)) {
            x2 = x1;
            y2 = y1;
         } else {
            final double b = (x1 - x0) / y10;
            final double c = (y10 - y21) / (y21 * y20);
            final double xm = x0 - (b * y0 * (1.0 - (c * y1)));
            final double ym = function(xm);
            ++mNEvals;
            if(ym == 0.0)
               return xm;
            if(Math.abs(xm - xmlast) < eps) {
               mBestX = xm;
               mBestY = ym;
               return xm;
            }
            xmlast = xm;
            if((ym * y0) < 0.0) {
               x2 = xm;
               y2 = ym;
            } else {
               x0 = xm;
               y0 = ym;
               x2 = x1;
               y2 = y1;
            }
         }
         mBestX = x1;
         mBestY = y1;
      }
      throw new ArithmeticException("FindRoot.perform has not converged after " + Integer.toString(iMax) + " iterations.");
   }
}
