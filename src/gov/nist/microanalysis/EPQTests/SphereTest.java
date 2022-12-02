package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.NISTMonte.Sphere;
import gov.nist.microanalysis.Utility.Math2;

import java.util.Random;

import junit.framework.TestCase;

/**
 * <p>
 * Test case for NISTMonte.Sphere.
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
public class SphereTest extends TestCase {

   private final double SCALE = 1.0e-5;
   private final int ITERATIONS = 1000;
   private final Random mRandom = new Random(0x1234);

   private double[] makeCenter() {
      return new double[]{(mRandom.nextDouble() - 0.5) * SCALE, (mRandom.nextDouble() - 0.5) * SCALE, (mRandom.nextDouble() - 0.5) * SCALE};
   }

   private double[] makeNormal() {
      return Math2.normalize(new double[]{mRandom.nextDouble(), mRandom.nextDouble(), mRandom.nextDouble()});
   }

   /**
    * Test method for
    * {@link gov.nist.microanalysis.NISTMonte.Sphere#contains(double[])}.
    */
   public void testContains() {
      for (int i = 0; i < ITERATIONS; ++i) {
         final double[] center = makeCenter();
         final double r = (mRandom.nextDouble() * SCALE) + (SCALE / 100.0);
         final Sphere sphere = new Sphere(center, r);
         {
            final double[] testPt = Math2.plus(center, Math2.multiply(0.99 * r * mRandom.nextDouble(), makeNormal()));
            assertTrue(sphere.contains(testPt));
         }
         {
            final double[] testPt = Math2.plus(center, Math2.multiply(r * (1.01 + mRandom.nextDouble()), makeNormal()));
            assertFalse(sphere.contains(testPt));
         }
      }
   }

   /**
    * Test method for
    * {@link gov.nist.microanalysis.NISTMonte.Sphere#getFirstIntersection(double[], double[])}
    * .
    */
   public void testGetFirstIntersection() {
      for (int i = 0; i < ITERATIONS; ++i) {
         final double[] center = makeCenter();
         final double r = (mRandom.nextDouble() * SCALE) + (SCALE / 100.0);
         final Sphere sphere = new Sphere(center, r);
         final double[] inside = Math2.plus(center, Math2.multiply(0.99 * r * mRandom.nextDouble(), makeNormal()));
         assertTrue(sphere.contains(inside));
         final double[] outside = Math2.plus(center, Math2.multiply(r * (1.01 + mRandom.nextDouble()), makeNormal()));
         assertFalse(sphere.contains(outside));
         final double t = sphere.getFirstIntersection(inside, outside);
         final double tp = sphere.getFirstIntersection(outside, inside);
         assertTrue(t < 1.0);
         assertTrue(tp < 1.0);
         assertEquals(1.0, tp + t, 1.0e-6);
      }
   }
}
