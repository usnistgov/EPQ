/**
 *
 */
package gov.nist.nanoscalemetrology.MONSELtests;

// import java.util.*;

import gov.nist.microanalysis.NISTMonte.MultiPlaneShape;
import gov.nist.nanoscalemetrology.JMONSEL.NormalMultiPlaneShape;

/**
 * @author jvillar
 */
public class NormalMultiPlaneShapeTiming
   extends MultiPlaneShape {

   public static double timetest(long repeats) {
      long t0; // Start time
      long tint, tf;
      double deltat = 0.;

      // Create a 6-plane figure for testing.
      final NormalMultiPlaneShape s = new NormalMultiPlaneShape();
      s.addPlane(new double[] {
         0.,
         0.,
         1.
      }, new double[] {
         0.,
         0.,
         1.
      });
      s.addPlane(new double[] {
         0.,
         0.,
         -1.
      }, new double[] {
         0.,
         0.,
         -1.
      });
      s.addPlane(new double[] {
         0.,
         1.,
         0.
      }, new double[] {
         0.,
         1.,
         0.
      });
      s.addPlane(new double[] {
         0.,
         -1.,
         0.
      }, new double[] {
         0.,
         -1.,
         0.
      });
      s.addPlane(new double[] {
         1.,
         0.,
         0.
      }, new double[] {
         1.,
         0.,
         0.
      });
      s.addPlane(new double[] {
         -1.,
         0.,
         0.
      }, new double[] {
         -1.,
         0.,
         0.
      });

      // Repeatedly execute the routine
      @SuppressWarnings("unused")
      double[] result;
      @SuppressWarnings("unused")
      final double result1;

      double[] pos0;
      double[] pos1 = {
         (Math.random() * 4.) - 2.,
         (Math.random() * 4.) - 2.,
         (Math.random() * 4.) - 2
      };
      t0 = System.currentTimeMillis();

      for(long i = 0; i < repeats; i++) {
         pos0 = pos1;
         pos1 = new double[] {
            (Math.random() * 4.) - 2.,
            (Math.random() * 4.) - 2.,
            (Math.random() * 4.) - 2
         };
         result = s.getFirstNormal(pos0, pos1);
      }

      tint = System.currentTimeMillis();
      for(long i = 0; i < repeats; i++) {
         pos0 = pos1;
         pos1 = new double[] {
            (Math.random() * 4.) - 2.,
            (Math.random() * 4.) - 2.,
            (Math.random() * 4.) - 2
         };
      }

      /*
       * t0 = System.currentTimeMillis(); double[] result; double result1;
       * List<Plane> mPlanes; Plane p; double[] n; for(long i = 0; i < repeats;
       * i++) { mPlanes = s.getPlanes(); for(ListIterator<Plane> it =
       * mPlanes.listIterator(); it.hasNext();) { p = it.next(); // Get next
       * plane n = p.getNormal(); // This plane's normal vector double[] c =
       * p.getPoint(); // A point in this plane } } tint =
       * System.currentTimeMillis(); for(long i = 0; i < repeats; i++) { }
       */

      /*
       * double[] pos0; double[] pos1 = { Math.random() * 4. - 2., Math.random()
       * * 4. - 2., Math.random() * 4. - 2 }; t0 = System.currentTimeMillis();
       * for(long i = 0; i < repeats; i++) { pos0 = pos1; pos1 = new double[] {
       * Math.random() * 4. - 2., Math.random() * 4. - 2., Math.random() * 4. -
       * 2 }; result1 = s.getFirstIntersection(pos0, pos1); } tint =
       * System.currentTimeMillis(); for(long i = 0; i < repeats; i++) { pos0 =
       * pos1; pos1 = new double[] { Math.random() * 4. - 2., Math.random() * 4.
       * - 2., Math.random() * 4. - 2 }; }
       */

      // Compute the elapsed time
      tf = System.currentTimeMillis(); // Stop the "stopwatch"
      deltat = (1000. * ((2. * tint) - tf - t0)) / repeats; // Time per op in
      // microseconds
      return deltat;

   }
}

/*
 * In a test on 8/11/2006 the getFirstNormal routine was taking an average 5.6
 * +/- 0.03 us per call. getFirstIntersection was taking 1.583 +/- 0.008 us.
 * This version of getFirstNormal was using the getPlanes, getNormal, and
 * getPoint methods to obtain shape information from the parent class each time
 * it was called.
 */
/*
 * getPlanes takes 30 ns per call. However, doing a getPlanes and then iterating
 * through the planes, extracting the normal and point from each one, takes 5.40
 * +/-.03 us. That is, almost all of our time is spent accessing the shape
 * information from the parent class!
 */
/*
 * A new implementation that cache's the parent class shape data takes 0.71
 * +/-.03 us per call.
 */
