package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.Utility.Translate2D;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

/**
 * 
 */
public class Translate2DTest extends TestCase {

   /**
    * Test method for
    * {@link gov.nist.microanalysis.Utility.Translate2D#Translate2D()}.
    */
   public void testTranslate2D() {
      final double[] oldPt = new double[]{3.4, 4.4};
      final Translate2D t2d = new Translate2D();
      final double[] newPt = t2d.compute(oldPt);
      assertEquals(oldPt[0], newPt[0], 1.0e-6);
      assertEquals(oldPt[1], newPt[1], 1.0e-6);
   }

   /**
    * Test method for
    * {@link gov.nist.microanalysis.Utility.Translate2D#Translate2D()}.
    */
   public void test1() {
      final Translate2D t2d = new Translate2D();
      {
         final Collection<Translate2D.CalibrationPoint> pts = new ArrayList<Translate2D.CalibrationPoint>();
         pts.add(Translate2D.createCalibrationPoint(new double[]{0.0, 0.0}, new double[]{1.395, 2.981}));
         pts.add(Translate2D.createCalibrationPoint(new double[]{1.0, 0.0}, new double[]{2.032, 3.112}));
         pts.add(Translate2D.createCalibrationPoint(new double[]{0.0, 1.0}, new double[]{1.265, 3.628}));
         pts.add(Translate2D.createCalibrationPoint(new double[]{1.0, 1.0}, new double[]{1.902, 3.756}));
         t2d.calibrate(pts);
      }
      {
         final double[] r = t2d.inverse(t2d.compute(new double[]{0.0, 0.0}));
         assertEquals(r[0], 0.0, 1.0e-6);
         assertEquals(r[1], 0.0, 1.0e-6);
      }
      {
         final double[] r = t2d.inverse(t2d.compute(new double[]{1.0, 0.0}));
         assertEquals(r[0], 1.0, 1.0e-6);
         assertEquals(r[1], 0.0, 1.0e-6);
      }
      {
         final double[] r = t2d.inverse(t2d.compute(new double[]{0.0, 1.0}));
         assertEquals(r[0], 0.0, 1.0e-6);
         assertEquals(r[1], 1.0, 1.0e-6);
      }
      {
         final double[] r = t2d.inverse(t2d.compute(new double[]{1.0, 1.0}));
         assertEquals(r[0], 1.0, 1.0e-6);
         assertEquals(r[1], 1.0, 1.0e-6);
      }
   }

   public void test2() {
      // Test single point calibrations
      final Translate2D t2d = new Translate2D(new double[]{2.0, -3.0}, new double[]{1.0, 1.0}, 0.0, false);
      final double[] r = t2d.compute(new double[]{1.0, 4.0});
      assertEquals(r[0], 3.0, 1.0e-9);
      assertEquals(r[1], 1.0, 1.0e-9);
   }

   public void test3() {
      // Test two point calibrations
   }

}
