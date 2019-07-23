package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.Utility.MCIntegrator;
import junit.framework.TestCase;

public class MCIntegratorTest
   extends TestCase {

   public void testIntegrate1() {
      final double[] pt1 = new double[] {
         1.0,
         1.0
      };
      final double[] pt2 = new double[] {
         -1.0,
         -1.0
      };
      final MCIntegrator integrator = new MCIntegrator(pt1, pt2) {

         @Override
         public double[] function(double[] args) {
            return new double[] {
               1.0
            };
         }

         @Override
         public boolean inside(double[] args) {
            return Math.sqrt((args[0] * args[0]) + (args[1] * args[1])) < 1.0;
         }
      };
      assertEquals(integrator.compute(1000)[0], Math.PI, 0.1);
   }

   public void testIntegrate2() {
      final double radius = 4.0;
      final double[] pt1 = new double[] {
         radius,
         radius
      };
      final double[] pt2 = new double[] {
         -radius,
         -radius
      };
      final MCIntegrator integrator = new MCIntegrator(pt1, pt2) {

         @Override
         public double[] function(double[] args) {
            return new double[] {
               2.0 * Math.sqrt((radius * radius) - (args[0] * args[0]) - (args[1] * args[1]))
            };
         }

         @Override
         public boolean inside(double[] args) {
            return ((args[0] * args[0]) + (args[1] * args[1])) < (radius * radius);
         }
      };
      final double res = ((4.0 * Math.PI) / 3.0) * Math.pow(radius, 3.0);
      assertEquals(integrator.compute(10000)[0], res, 0.02 * res);
   }
}