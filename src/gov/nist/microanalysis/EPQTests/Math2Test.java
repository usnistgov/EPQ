package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.Utility.Math2;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the Math2 class.
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
public class Math2Test extends TestCase {
   public Math2Test(String test) {
      super(test);
   }

   public void testErf() {
      final double[] x = {0.113, 0.293, 0.452, 0.728, 1.13, 2.93, 4.52, 7.28};
      final double[] r = {0.8730337930959786, 0.6786071294114382, 0.522676876403969, 0.3032224835233542, 0.11002932963703772, 0.00003418276664265989,
            1.6346737338997198e-10, 7.3854368862444885e-25};
      final double[] lg = {2.12511, 1.12062, 0.672632, 0.227795, -0.0619483, 0.629529, 2.48156, 7.10957};
      final double[] g = {0.9999872475688222, 0.9989217699823233, 0.9868480871575643, 0.784464140190217, 0.4565498366611956, 0.01272580112957962,
            0.000056703383632205426, 1.2987280359516387e-11};
      for (int i = 0; i < x.length; ++i) {
         assertEquals(Math2.erf(x[i]), (1.0 - r[i]), 0.000001);
         assertEquals(Math2.erfc(x[i]), r[i], 0.000001);
         assertEquals(Math2.gammaln(x[i]), lg[i], 0.00001);
         assertEquals(Math2.gammap(x[i], x[x.length - (i + 1)]), g[i], g[i] * 0.00001);
      }
      assertEquals(Math2.li(1.1), -1.67577, 0.00001);
      assertEquals(Math2.li(1.2), -0.933787, 0.00001);
      assertEquals(Math2.li(1.3), -0.480178, 0.00001);
      assertEquals(Math2.li(1.5), 0.125065, 0.00001);
      assertEquals(Math2.li(1.7), 0.553744, 0.00001);
      assertEquals(Math2.li(2.0), 1.04516, 0.00001);

      assertEquals(Math2.li(10.0), 6.1656, 0.0001);
      assertEquals(Math2.li(50.0), 18.4687, 0.0001);
      assertEquals(Math2.li(100.0), 30.1261, 0.0001);
   }

   public void testLegendre() {
      final double[] results = new double[]{1.0, 1.0 / 4.0, -(13.0 / 32.0), -(43.0 / 128.0), 323.0 / 2048.0, 2783.0 / 8192.0, 1591.0 / 65536.0,
            -(73379.0 / 262144.0), -(1278877.0 / 8388608.0), 5933243.0 / 33554432.0, 59377981.0 / 268435456.0};
      for (int n = 0; n <= 10; ++n)
         assertEquals(Math2.Legendre(0.25, n), results[n], 1.0e-10);
   }

   public void testQuadratic() {
      {
         final double[] q1 = Math2.quadraticSolver(2.0, 4.0, 1.0);
         assertEquals(Math2.max(q1), -0.2928932188, 1.0e-8);
         assertEquals(Math2.min(q1), -1.707106781, 1.0e-8);
      }
      {
         final double[] q1 = Math2.quadraticSolver(2.0, 4.0, 4.0);
         assert (Double.isNaN(q1[0]));
         assert (Double.isNaN(q1[1]));
      }
      {
         final double[] q1 = Math2.quadraticSolver(3.0 / 2.0, 7.0 / 3.0, 1.0 / 8.0);
         assertEquals(Math2.min(q1), -1.5, 1.0e-8);
         assertEquals(Math2.max(q1), -1.0 / 18.0, 1.0e-8);
      }
   }

}
