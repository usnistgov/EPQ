package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.Utility.FindRoot;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the FindRoot class.
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
public class FindRootTest extends TestCase {
   public FindRootTest(String test) {
      super(test);
   }

   public void testOne() {
      final double eps = 1.0e-5;
      FindRoot fr = new FindRoot() {
         @Override
         public double function(double x0) {
            return Math.exp(x0) - 22.0; // zero at log(22.0)
         }
      };
      assertEquals(fr.perform(0.0, 10.0, eps, 20), Math.log(22.0), eps);

      fr = new FindRoot() {
         private double res;

         @Override
         public void initialize(double[] vars) {
            res = vars[0];
         }

         @Override
         public double function(double x0) {
            return Math.sin(x0) - res;
         }
      };
      final double[] vars = {0.5}; // root at 30 degrees
      fr.initialize(vars);
      assertEquals(fr.perform(0.0, Math.PI / 2.0, eps, 20), (30.0 * Math.PI) / 180.0, eps);
   }
};