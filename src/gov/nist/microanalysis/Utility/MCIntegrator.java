/**
 * gov.nist.microanalysis.Utility.Integrator Created by: nritchie Date: Jun 24,
 * 2008
 */
package gov.nist.microanalysis.Utility;

import java.util.Random;

/**
 * <p>
 * A simple class for performing a one dimensional integration.
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
abstract public class MCIntegrator {

   private final double[] mPoint1;
   private final double[] mPoint2;
   private final Random mRand;

   /**
    * A user supplied function to integrate.
    * 
    * @param args
    * @return The function value at <code>args</code>
    */
   abstract public double[] function(double[] args);

   /**
    * A user supplied function that returns true if <code>arg</code> is within
    * the integration volume and false otherwise.
    * 
    * @param args
    * @return boolean - true within integration volume
    */
   abstract public boolean inside(double[] args);

   protected MCIntegrator(double[] point1, double[] point2) {
      assert (point1.length == point2.length);
      mPoint1 = point1;
      mPoint2 = point2;
      mRand = new Random();

   }

   public double[] compute(int nTests) {
      double volume = mPoint2[0] - mPoint1[0];
      for (int i = 1; i < mPoint1.length; i++)
         volume *= mPoint2[i] - mPoint1[i];

      double[] inner = null;
      final double[] tempPoint = new double[mPoint1.length];
      for (int i = 0; i < nTests; i++) {
         for (int index = 0; index < mPoint1.length; index++)
            tempPoint[index] = mPoint1[index] + (mRand.nextDouble() * (mPoint2[index] - mPoint1[index]));

         if (inside(tempPoint)) {
            final double[] f = function(tempPoint);
            inner = (inner == null ? f : Math2.plusEquals(inner, f));
         }
      }
      return Math2.timesEquals(volume / nTests, inner);
   }

}
