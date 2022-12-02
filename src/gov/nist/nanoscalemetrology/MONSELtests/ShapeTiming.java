/**
 * gov.nist.nanoscalemetrology.MONSELtests.ShapeTiming Created by: jvillar Date:
 * Jul 1, 2011
 */
package gov.nist.nanoscalemetrology.MONSELtests;

import gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Description
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author jvillar
 * @version 1.0
 */
public class ShapeTiming {

   private final Shape myShape;

   /**
    * Constructs a ShapeTiming
    *
    * @param myShape
    */
   public ShapeTiming(Shape myShape) {
      super();
      this.myShape = myShape;
   }

   /**
    * Calls the Shape's contains(pos0) method repeats times, each time with a
    * randomly assigned value of pos0, with pos0[i] = center[i] +
    * (Math.random()*2.-1.)*distances[i]. That is, the coordinates of the
    * argument lie within a rectangular box within +/- distances[] of the
    * supplied center[].
    *
    * @param center
    *           - double[3] giving the center coordinates of the test box
    * @param distances
    *           - double[3] giving the size of the test box
    * @param repeats
    *           - number of times to call the contains
    * @return - average time per call in microseconds
    */
   public double testContains(double[] center, double[] distances, long repeats) {
      long t0; // Start time
      long tint, tf; // intermediate and final times
      double deltat = 0.;

      // Repeatedly execute the routine
      @SuppressWarnings("unused")
      boolean result;

      t0 = System.currentTimeMillis();
      double[] pos0;

      for (long i = 0; i < repeats; i++) {
         pos0 = Math2.plus(center, new double[]{((Math.random() * 2.) - 1.) * distances[0], ((Math.random() * 2.) - 1.) * distances[1],
               ((Math.random() * 2.) - 1.) * distances[2]});
         result = myShape.contains(pos0);
      }

      tint = System.currentTimeMillis();
      for (long i = 0; i < repeats; i++)
         pos0 = Math2.plus(center, new double[]{((Math.random() * 2.) - 1.) * distances[0], ((Math.random() * 2.) - 1.) * distances[1],
               ((Math.random() * 2.) - 1.) * distances[2]});

      tf = System.currentTimeMillis();
      deltat = (1000. * ((2. * tint) - tf - t0)) / repeats;
      return deltat;
   }

   /**
    * Calls the Shape's getFirstIntersection(pos0,pos1) method repeats times,
    * each time with a randomly assigned values of pos0 and pos1, each according
    * to pos[i] = center[i] + (Math.random()*2.-1.)*distances[i]. That is, the
    * coordinates of the argument lie within a rectangular box within +/-
    * distances[] of the supplied center[].
    *
    * @param center
    *           - double[3] giving the center coordinates of the test box
    * @param distances
    *           - double[3] giving the size of the test box
    * @param repeats
    *           - number of times to call the contains
    * @return - average time per call in microseconds
    */
   public double testGetFirstIntersection(double[] center, double[] distances, long repeats) {
      long t0; // Start time
      long tint, tf; // intermediate and final times
      double deltat = 0.;

      // Repeatedly execute the routine
      @SuppressWarnings("unused")
      double result;

      t0 = System.currentTimeMillis();
      double[] pos0;
      double[] pos1;

      for (long i = 0; i < repeats; i++) {
         pos0 = Math2.plus(center, new double[]{((Math.random() * 2.) - 1.) * distances[0], ((Math.random() * 2.) - 1.) * distances[1],
               ((Math.random() * 2.) - 1.) * distances[2]});
         pos1 = Math2.plus(center, new double[]{((Math.random() * 2.) - 1.) * distances[0], ((Math.random() * 2.) - 1.) * distances[1],
               ((Math.random() * 2.) - 1.) * distances[2]});
         result = myShape.getFirstIntersection(pos0, pos1);
      }

      tint = System.currentTimeMillis();
      for (long i = 0; i < repeats; i++) {
         pos0 = Math2.plus(center, new double[]{((Math.random() * 2.) - 1.) * distances[0], ((Math.random() * 2.) - 1.) * distances[1],
               ((Math.random() * 2.) - 1.) * distances[2]});
         pos1 = Math2.plus(center, new double[]{((Math.random() * 2.) - 1.) * distances[0], ((Math.random() * 2.) - 1.) * distances[1],
               ((Math.random() * 2.) - 1.) * distances[2]});
      }

      tf = System.currentTimeMillis();
      deltat = (1000. * ((2. * tint) - tf - t0)) / repeats;
      return deltat;
   }

}

/*
 * Some examples, run on Galileo2.26 GHz E5520 7/26/2011 With 300 x 300 x 300
 * (nm) test box: NormalHeightMapShape contains: {0.48,0.78,0.46,0.62} us =
 * (0.59 +/- 0.15) us NormalHeightMapShape getFirstIntersection:
 * {379.07,314.69,314.07,379.08} us = (347 +/- 37) us With 1 x 1 x 1 (nm) test
 * box: NormalHeightMapShape contains: {0.093,0.078,0.079,0.079,0.079} us =
 * (0.082 +/- 0.006) us NormalHeightMapShape getFirstIntersection:
 * {0.47,1.484,10.235,0.657,0.641,0.61} us = (0.8 +/- 0.4) us (if I omit the
 * outlier 10)
 */
