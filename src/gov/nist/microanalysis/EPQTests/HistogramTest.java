/**
 * <p>
 * Title: gov.nist.microanalysis.EPQTests.HistogramTest.java
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
 * @author Nicholas
 * @version 1.0
 */
package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.Utility.Histogram;
import junit.framework.TestCase;

/**
 * Test cases for the Histogram class.
 */
public class HistogramTest extends TestCase {

   /**
    * Constructs a HistogramTest
    * 
    * @param arg0
    */
   public HistogramTest(String arg0) {
      super(arg0);
      // TODO Auto-generated constructor stub
   }

   public void testOne() throws EPQException {
      final Histogram h = new Histogram(0.0, 10.0, 10);
      assertEquals(h.minValue(-1), Double.NEGATIVE_INFINITY);
      assertEquals(h.maxValue(10), Double.POSITIVE_INFINITY);
      assertEquals(h.maxValue(-1), 0.0);
      assertEquals(h.maxValue(0), 1.0);
      assertEquals(h.maxValue(1), 2.0);
      assertEquals(h.maxValue(2), 3.0);
      final double[] data = new double[]{-1, -0.001, 0.5, 1.0, 1.0, 2.1, 2.3, 2.4, 3.1, 3.3, 3.4, 3.6, 9.9, 9.8, 10.4};
      h.add(data);
      assertEquals(h.counts(-1), 2);
      assertEquals(h.counts(0), 1);
      assertEquals(h.counts(1), 2);
      assertEquals(h.counts(2), 3);
      assertEquals(h.counts(3), 4);
      assertEquals(h.counts(10), 1);
      assertEquals(h.counts(9), 2);
   }

   public void testTwo() throws EPQException {
      final Histogram h = new Histogram(1.0, 10.0, 2.0);
      assertEquals(h.minValue(-1), Double.NEGATIVE_INFINITY);
      assertEquals(h.maxValue(-1), 1.0);
      assertEquals(h.maxValue(0), 2.0);
      assertEquals(h.maxValue(1), 4.0);
      assertEquals(h.maxValue(2), 8.0);
      assertEquals(h.maxValue(3), Double.POSITIVE_INFINITY);
      final double[] data = new double[]{-1, -0.001, 0.5, 1.0, 1.0, 2.1, 2.3, 2.4, 3.1, 3.3, 3.4, 3.6, 9.9, 9.8, 10.4};
      h.add(data);
      assertEquals(h.counts(-1), 3);
      assertEquals(h.counts(0), 2);
      assertEquals(h.counts(1), 7);
      assertEquals(h.counts(3), 3);
      assertEquals(h.binCount(), 3);
   }

   public void testThree() throws EPQException {
      final Histogram h = new Histogram(new double[]{0.0, 1.0, 2.0, 4.0, 8.0}, 10.0);
      assertEquals(h.minValue(-1), Double.NEGATIVE_INFINITY);
      assertEquals(h.maxValue(-1), 0.0);
      assertEquals(h.maxValue(0), 1.0);
      assertEquals(h.maxValue(1), 2.0);
      assertEquals(h.maxValue(2), 4.0);
      assertEquals(h.maxValue(4), 10.0);
      assertEquals(h.maxValue(5), Double.POSITIVE_INFINITY);
      final double[] data = new double[]{-1, -0.001, 0.5, 1.0, 1.0, 2.1, 2.3, 2.4, 3.1, 3.3, 3.4, 3.6, 9.9, 9.8, 10.4};
      h.add(data);
      assertEquals(h.counts(-1), 2);
      assertEquals(h.counts(0), 1);
      assertEquals(h.counts(1), 2);
      assertEquals(h.counts(4), 2);
      assertEquals(h.counts(5), 1);
      assertEquals(h.binCount(), 5);
   }

   public void testFour() throws EPQException {
      final Histogram h = new Histogram(1.0, 10.0, 2);
      assertEquals(h.minValue(0), 1.0);

   }

   public void testFive() throws EPQException {
      final Histogram h = new Histogram(0, 10.0, 10);
      assertEquals(h.isBinMin(1.0), true);
      assertEquals(h.isBinMin(5.0), true);
      assertEquals(h.isBinMin(9.0), true);
      assertEquals(h.isBinMin(10.0), true);
      assertEquals(h.isBinMin(0.0), true);
   }

   public void testRemoveBin() throws EPQException {
      final Histogram h = new Histogram(0, 10.0, 10);
      h.add(new double[]{5.1, 5.2, 6.1, 6.2, 6.3, 6.4, 7.1, 7.2, 7.3});
      assertEquals(h.totalCounts(), 9);
      assertEquals(h.counts(5), 2);
      assertEquals(h.counts(6), 4);
      assertEquals(h.counts(7), 3);
      h.removeBin(6);
      assertEquals(h.totalCounts(), 9);
      assertEquals(h.counts(5), 6);
      assertEquals(h.counts(6), 3);
      assertEquals(h.isBinMin(6), false);
      assertEquals(h.binCount(), 9);
   }
}
