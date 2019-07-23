package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.Utility.AdaptiveRungeKutta;
import gov.nist.microanalysis.Utility.UtilException;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the AdaptiveRungeKutta class.
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
public class AdaptiveRungeKuttaTest
   extends TestCase {
   public AdaptiveRungeKuttaTest(String test) {
      super(test);
   }

   public void testOne()
         throws UtilException {
      final AdaptiveRungeKutta trial = new AdaptiveRungeKutta(2) {
         @Override
         public void derivatives(double x, double[] y, double[] dydx) {
            dydx[0] = -Math.sin(x);
            dydx[1] = Math.cos(x);
         }
      };
      final double[] yst = {
         1.0,
         0.0
      };
      trial.setSaveInterval((Math.PI / 16.0) - 0.00001);
      trial.integrate(0.0, 2.0 * Math.PI, yst, 1.0e-6, 0.01);
      double sumErr = 0.0;
      for(int i = 0; i < trial.getNSaved(); ++i)
         sumErr += Math.abs(trial.getY(i)[0] - Math.cos(trial.getX(i))) + Math.abs(trial.getY(i)[1] - Math.sin(trial.getX(i)));
      assertTrue(sumErr < 1.0e-6);
   }

}
