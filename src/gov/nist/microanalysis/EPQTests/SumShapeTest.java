package gov.nist.microanalysis.EPQTests;

import java.util.Random;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.NISTMonte.CylindricalShape;
import gov.nist.microanalysis.NISTMonte.GaussianBeam;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.MultiPlaneShape;
import gov.nist.microanalysis.NISTMonte.Sphere;
import gov.nist.microanalysis.NISTMonte.SumShape;
import gov.nist.microanalysis.Utility.Math2;

import junit.framework.TestCase;

/**
 * <p>
 * A test class for the SumShape class.
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
public class SumShapeTest
   extends TestCase {

   static final double beamEnergy = ToSI.keV(20.0);
   static final double beamDia = 1.0e-8;
   static final double length = 1.0e-6;
   static final double radius = 0.5e-6;
   private MonteCarloSS mMonte;
   private MonteCarloSS.Shape mPillOuter;

   private final Random mRandom = new Random(0x12345678L);
   private Sphere mCap0Outer;
   private Sphere mCap1Outer;
   private CylindricalShape mCylOuter;

   /**
    * @see junit.framework.TestCase#setUp()
    */
   @Override
   protected void setUp()
         throws Exception {
      super.setUp();
      final Material mat1 = new Material(new Composition(new Element[] {
         Element.C,
         Element.O,
         Element.Cl
      }, new double[] {
         0.7,
         0.28,
         0.02
      }, "Resin"), ToSI.gPerCC(1.14));
      final Material mat2 = new Material(new Composition(new Element[] {
         Element.C,
         Element.O,
         Element.N,
         Element.H,
         Element.S,
         Element.P,
         Element.Os
      }, new double[] {
         0.4962,
         0.2988,
         0.0784,
         0.0773,
         0.006,
         0.0232,
         0.02
      }, "Inner"), ToSI.gPerCC(1.11));

      mMonte = new MonteCarloSS();
      mMonte.setBeamEnergy(beamEnergy);
      final GaussianBeam beam = new GaussianBeam(beamDia);
      mMonte.setElectronGun(beam);
      beam.setCenter(new double[] {
         0,
         0,
         -0.05
      });

      final MultiPlaneShape blk = MultiPlaneShape.createSubstrate(Math2.MINUS_Z_AXIS, Math2.ORIGIN_3D);
      final MonteCarloSS.Region r1 = mMonte.addSubRegion(mMonte.getChamber(), mat1, blk);

      final double[] end0 = new double[] {
         -length,
         0.0,
         2.0 * radius
      };
      final double[] end1 = new double[] {
         length,
         0.0,
         2.0 * radius
      };

      mCylOuter = new CylindricalShape(end0, end1, radius);
      mCap0Outer = new Sphere(end0, radius);
      mCap1Outer = new Sphere(end1, radius);
      mPillOuter = new SumShape(new MonteCarloSS.Shape[] {
         mCylOuter,
         mCap0Outer,
         mCap1Outer
      });
      mMonte.addSubRegion(r1, mat2, mPillOuter);
   }

   private double[] pointInside() {
      final double[] res = new double[3];
      switch(mRandom.nextInt(3)) {
         case 0: {
            final double r = radius * mRandom.nextDouble();
            final double th = mRandom.nextDouble() * Math.PI;
            final double phi = 2.0 * mRandom.nextDouble() * Math.PI;
            res[0] = -length + (r * Math.cos(phi) * Math.sin(th));
            res[1] = r * Math.sin(phi) * Math.sin(th);
            res[2] = (2.0 * radius) + (r * Math.cos(th));
            assertTrue(mCap0Outer.contains(res));
         }
            break;
         case 1: {
            final double phi = 2.0 * mRandom.nextDouble() * Math.PI;
            final double r = radius * mRandom.nextDouble();
            res[0] = 2.0 * length * (mRandom.nextDouble() - 0.5);
            res[1] = r * Math.cos(phi);
            res[2] = (2.0 * radius) + (Math.sin(phi) * r);
            assertTrue(mCylOuter.contains(res));
         }
            break;
         case 2: {
            final double r = radius * mRandom.nextDouble();
            final double th = mRandom.nextDouble() * Math.PI;
            final double phi = 2.0 * mRandom.nextDouble() * Math.PI;
            res[0] = length + (r * Math.cos(phi) * Math.sin(th));
            res[1] = r * Math.sin(phi) * Math.sin(th);
            res[2] = (2.0 * radius) + (r * Math.cos(th));
            assertTrue(mCap1Outer.contains(res));
         }
            break;
      }
      return res;
   }

   private double[] pointOutside() {
      final double[] res = new double[3];
      final double phi = 2.0 * mRandom.nextDouble() * Math.PI;
      final double r = radius * (1.00001 + (0.9 * mRandom.nextDouble()));
      res[0] = 3.0 * length * (mRandom.nextDouble() - 0.5);
      res[1] = r * Math.cos(phi);
      res[2] = (2.0 * radius) + (Math.sin(phi) * r);
      return res;
   }

   /**
    * Test method for
    * {@link gov.nist.microanalysis.NISTMonte.SumShape#getFirstIntersection(double[], double[])}
    * .
    */
   public void testGetFirstIntersection() {
      for(int i = 0; i < 1000; ++i) {
         final double[] inside1 = pointInside();
         assertTrue(mPillOuter.contains(inside1));
         final double[] inside2 = pointInside();
         assertTrue(mPillOuter.contains(inside2));
         final double[] outside = pointOutside();
         assertFalse(mPillOuter.contains(outside));
         {
            final double t = mPillOuter.getFirstIntersection(inside1, inside2);
            assertTrue(t != Double.MAX_VALUE);
            assertTrue(t > 1.0);
         }
         {
            assertTrue(mPillOuter.contains(inside1));
            assertFalse(mPillOuter.contains(outside));
            final double t = mPillOuter.getFirstIntersection(inside1, outside);

            assertTrue(mPillOuter.contains(Math2.pointBetween(inside1, outside, 0.99 * t)));
            assertFalse(mPillOuter.contains(Math2.pointBetween(inside1, outside, 1.01 * t)));
            final double tp = mPillOuter.getFirstIntersection(outside, inside1);
            assertFalse(mPillOuter.contains(Math2.pointBetween(outside, inside1, 0.99 * tp)));
            assertTrue(mPillOuter.contains(Math2.pointBetween(outside, inside1, 1.01 * tp)));
            assertTrue(t < 1.0);
            assertTrue(tp < 1.0);
            assertEquals(1.0, t + tp, 1.0e-6);
         }
         {
            final double t = mPillOuter.getFirstIntersection(inside2, outside);
            final double tp = mPillOuter.getFirstIntersection(outside, inside2);
            assertTrue(t < 1.0);
            assertTrue(tp < 1.0);
            assertEquals(1.0, t + tp, 1.0e-6);
         }
      }
   }

   public void testAll() {
      mMonte.runMultipleTrajectories(1000);
   }
}
