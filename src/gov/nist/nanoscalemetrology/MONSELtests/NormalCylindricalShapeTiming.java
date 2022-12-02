package gov.nist.nanoscalemetrology.MONSELtests;

import gov.nist.nanoscalemetrology.JMONSEL.NormalCylindricalShape;

public class NormalCylindricalShapeTiming {

   public static double timetest(long repeats) {
      long t0; // Start time
      long tint, tf;
      double deltat = 0.;
      final double[] end0 = {0., 0., -1.};
      final double[] end1 = {0., 0., 1.};
      final double radius = 0.5;
      final NormalCylindricalShape s = new NormalCylindricalShape(end0, end1, radius);

      // Repeatedly execute the getFirstNormal routine

      t0 = System.currentTimeMillis(); // Start the "stopwatch"
      for (long i = 0; i < repeats; i++) {
         final double[] pos0 = {Math.random(), Math.random(), Math.random()};
         final double[] pos1 = {Math.random(), Math.random(), Math.random()};
         @SuppressWarnings("unused")
         final double[] normalv = s.getFirstNormal(pos0, pos1);
      }

      // Repeat the loop with a dummy assignment

      tint = System.currentTimeMillis(); // Get the intermediate time
      for (long i = 0; i < repeats; i++) {
         @SuppressWarnings("unused")
         final double[] pos0 = {Math.random(), Math.random(), Math.random()};
         @SuppressWarnings("unused")
         final double[] pos1 = {Math.random(), Math.random(), Math.random()};
         @SuppressWarnings("unused")
         final double[] normalv = {0., 0., 0.};
      }

      // Repeatedly execute the getFirstIntersection routine
      /*
       * t0 = System.currentTimeMillis(); //Start the "stopwatch" for(long
       * i=0;i<repeats;i++){ double[] pos0 =
       * {Math.random(),Math.random(),Math.random()}; double[] pos1 =
       * {Math.random(),Math.random(),Math.random()}; double u =
       * s.getFirstIntersection(pos0,pos1); }
       */

      // Repeat the loop with a dummy assignment
      /*
       * tint = System.currentTimeMillis(); // Get the intermediate time
       * for(long i=0;i<repeats;i++){ double[] pos0 =
       * {Math.random(),Math.random(),Math.random()}; double[] pos1 =
       * {Math.random(),Math.random(),Math.random()}; double u = 0.; }
       */

      // Compute the elapsed time
      tf = System.currentTimeMillis(); // Stop the "stopwatch"
      deltat = (1000. * ((2. * tint) - tf - t0)) / repeats; // Time per op in
      // microseconds
      return deltat;

   }
}

/*
 * In a test on 8/9/2006 the getFirstNormal routine was taking an average 0.310
 * +/- 0.01 us per call. getFirstIntersection takes 0.35 +/- 0.002 us.
 */
