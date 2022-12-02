package gov.nist.microanalysis.Utility;

/**
 * <p>
 * Implements a numerical integration routine based on an AdaptiveRungeKutta
 * algorithm from Press et al.
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
abstract public class Integrator extends AdaptiveRungeKutta {

   private final double mTolerance;

   public Integrator() {
      this(1.0e-6);
   }

   public Integrator(double tol) {
      super(1);
      mTolerance = tol;
   }

   public double integrate(double lowVal, double highVal) {
      final double[] y = new double[]{0.0};
      try {
         return highVal > lowVal ? integrate(lowVal, highVal, y, mTolerance, 0.05 * (highVal - lowVal))[0] : 0.0;
      } catch (final UtilException e) {
         e.printStackTrace();
         return Double.NaN;
      }
   }

   @Override
   public void derivatives(double x, double[] y, double[] dydx) {
      dydx[0] = getValue(x);
   }

   abstract public double getValue(double x);
}