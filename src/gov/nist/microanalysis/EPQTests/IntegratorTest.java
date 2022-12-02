package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.Utility.Integrator;
import junit.framework.TestCase;

public class IntegratorTest extends TestCase {

   public void testCompare1() {
      final Integrator i2 = new Integrator(1.0e-6) {

         @Override
         public double getValue(double x) {
            return Math.exp(x * x) * Math.cos(x);
         }
      };
      assertEquals(-1.16761e42, i2.integrate(0.0, 10.0), 0.0001e42);
   }

   public void testCompare2() {
      final Integrator i2 = new Integrator(1.0e-6) {

         @Override
         public double getValue(double x) {
            return Math.cos(x);
         }
      };
      assertEquals(-0.506366, i2.integrate(0.0, 100.0), 0.00001);
   }

   public void testCompare3() {
      final Integrator i2 = new Integrator(1.0e-6) {

         @Override
         public double getValue(double x) {
            return Math.sin(x);
         }
      };
      assertEquals(0.0, i2.integrate(-1.0, 1.0), 0.00001);
   }

   public void testCompare4() {
      final Integrator i2 = new Integrator(1.0e-6) {

         @Override
         public double getValue(double x) {
            return Math.log(0.5 * x);
         }
      };
      assertEquals(7.78753, i2.integrate(1.0, 10.0), 0.00001);
   }

   public void testIntegrate() {
      {
         final Integrator i = new Integrator() {
            @Override
            public double getValue(double x) {
               return 1.0;
            }
         };
         assertEquals(i.integrate(0.0, 1.0), 1.0, 1.0e-15);

      }
      {
         final Integrator i = new Integrator() {
            @Override
            public double getValue(double x) {
               return Math.sin(x);
            }
         };
         assertEquals(i.integrate(0.0, 2.0 * Math.PI), 0.0, 1.0e-6);
      }
      {
         final Integrator i = new Integrator() {
            @Override
            public double getValue(double x) {
               return 3.0 * x * x;
            }
         };

         assertEquals(i.integrate(0.0, 10.0), 1000.0, 1.0e-6);
      }
      {
         final Integrator i = new Integrator() {
            @Override
            public double getValue(double x) {
               return Math.cos(x);
            }
         };
         assertEquals(i.integrate(0.0, 1.5 * Math.PI), -1.0, 1.0e-6);
      }
      {
         final Integrator outer = new Integrator() {
            @Override
            public double getValue(double outer) {
               {
                  final double x = outer;
                  final Integrator inner = new Integrator() {
                     @Override
                     public double getValue(double y) {
                        return x + y;
                     }
                  };
                  return inner.integrate(0, 2);
               }
            }
         };
         assertEquals(outer.integrate(0.0, 2.0), 8.0, 1.0e-6);
      }
   }

}
