/**
 * <p>
 * Title: gov.nist.microanalysis.EPQTests.StageRelocationTest.java
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nritchie
 * @version 1.0
 */
package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.StageRelocation;

import java.util.ArrayList;
import java.util.Random;

import junit.framework.TestCase;

/**
 * 
 */
public class StageRelocationTest extends TestCase {

   private final Random mRandom = new Random(0x1234);

   /**
    * Test method for
    * {@link gov.nist.microanalysis.Utility.StageRelocation#StageRelocation()}.
    */
   public void testStageRelocation() {
      final StageRelocation sr = new StageRelocation();
      for (int i = 0; i < 1000; ++i) {
         final double[] pt = new double[]{2.0 * (0.5 - mRandom.nextDouble()), 2.0 * (0.5 - mRandom.nextDouble())};
         final double[] res = sr.apply(pt);
         assertTrue(Math2.distance(pt, res) < 1.0e-9);
      }
   }

   /**
    * Test method for
    * {@link gov.nist.microanalysis.Utility.StageRelocation#StageRelocation(java.util.List, boolean)}
    * .
    */
   public void testStageRelocation2() throws EPQException {
      { // Test one point relocation
         final ArrayList<StageRelocation.RelocatedPoint> al = new ArrayList<StageRelocation.RelocatedPoint>();
         al.add(new StageRelocation.RelocatedPoint(new double[]{-1.2, -3.4}, new double[]{-2.3, 4.5}));
         final StageRelocation sr = new StageRelocation(al, false);
         {
            final double[] pt1 = new double[]{0.0, 0.0};
            final double[] pt2 = new double[]{-1.1, 7.8};
            assertTrue(Math2.distance(sr.apply(pt1), pt2) < 1.0e-6);
         }
         {
            final double[] pt1 = new double[]{-3.0, 4.0};
            final double[] pt2 = new double[]{-4.1, 11.8};
            assertTrue(Math2.distance(pt1, pt2) < 1.0e-6);
         }
      }
      { // Test two point relocation
         final ArrayList<StageRelocation.RelocatedPoint> al = new ArrayList<StageRelocation.RelocatedPoint>();
         al.add(new StageRelocation.RelocatedPoint(new double[]{1.0, 10.0}, new double[]{5.23958, -1.33052}));
         al.add(new StageRelocation.RelocatedPoint(new double[]{7.0, -4.0}, new double[]{-7.24686, -10.0534}));
         {
            final StageRelocation sr = new StageRelocation(al, false);
            assertTrue(Math2.distance(new double[]{-8.9127, 2.87814}, sr.apply(new double[]{-6.0, -3.0})) < 0.01);
            assertTrue(Math2.distance(new double[]{-8.9127, 2.87814}, sr.apply(new double[]{1.0, 1.0})) < 0.01);
         }
         // Test > 3 pt relocation
         al.add(new StageRelocation.RelocatedPoint(new double[]{-6.0, -3.0}, new double[]{-8.9127, 2.87814}));
         al.add(new StageRelocation.RelocatedPoint(new double[]{1.0, 1.0}, new double[]{-8.9127, 2.87814}));
         {
            final StageRelocation sr = new StageRelocation(al, false);

            assertTrue(Math2.distance(new double[]{-0.228008, -4.50942}, sr.apply(new double[]{3.0, 4.0})) < 0.01);

            assertTrue(Math2.distance(new double[]{3.0, 4.0}, sr.inverse(sr.apply(new double[]{3.0, 4.0}))) < 0.01);

         }
      }
   }

   /**
    * Test method for
    * {@link gov.nist.microanalysis.Utility.StageRelocation#StageRelocation(java.util.List, boolean)}
    * .
    */
   public void testStageRelocation3() throws EPQException {
      { // Test one point relocation
         final ArrayList<StageRelocation.RelocatedPoint> al = new ArrayList<StageRelocation.RelocatedPoint>();
         al.add(new StageRelocation.RelocatedPoint(new double[]{-1.2, -3.4}, new double[]{-2.3, 4.5}));
         final StageRelocation sr = new StageRelocation(al, false);
         {
            final double[] pt1 = new double[]{0.0, 0.0};
            final double[] pt2 = new double[]{-1.1, 7.8};
            assertTrue(Math2.distance(sr.apply(pt1), pt2) < 1.0e-6);
         }
         {
            final double[] pt1 = new double[]{-3.0, 4.0};
            final double[] pt2 = new double[]{-4.1, 11.8};
            assertTrue(Math2.distance(pt1, pt2) < 1.0e-6);
         }
      }
      { // Test two point relocation
         final ArrayList<StageRelocation.RelocatedPoint> al = new ArrayList<StageRelocation.RelocatedPoint>();
         al.add(new StageRelocation.RelocatedPoint(new double[]{1.0, 10.0}, new double[]{5.23958, -1.33052}));
         al.add(new StageRelocation.RelocatedPoint(new double[]{7.0, -4.0}, new double[]{-7.24686, -10.0534}));
         {
            final StageRelocation sr = new StageRelocation(al, false);
            assertTrue(Math2.distance(new double[]{-8.9127, 2.87814}, sr.apply(new double[]{-6.0, -3.0})) < 0.01);
            assertTrue(Math2.distance(new double[]{-8.9127, 2.87814}, sr.apply(new double[]{1.0, 1.0})) < 0.01);
         }
         // Test > 3 pt relocation
         al.add(new StageRelocation.RelocatedPoint(new double[]{-6.0, -3.0}, new double[]{-8.9127, 2.87814}));
         al.add(new StageRelocation.RelocatedPoint(new double[]{1.0, 1.0}, new double[]{-8.9127, 2.87814}));
         {
            final StageRelocation sr = new StageRelocation(al, false);

            assertTrue(Math2.distance(new double[]{-0.228008, -4.50942}, sr.apply(new double[]{3.0, 4.0})) < 0.01);

            assertTrue(Math2.distance(new double[]{3.0, 4.0}, sr.inverse(sr.apply(new double[]{3.0, 4.0}))) < 0.01);

         }
      }
   }
}
