/**
 *
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.util.List;

import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.SumShape;

/**
 * <p>
 * Extends the NISTMonte SumShape class to implement the NormalShape interface.
 * This uses a different algorithm than SumShape for determining the
 * intersections. Currently, it is limited to only two shapes, unlike SumShape.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
public class NormalUnionShape extends SumShape implements NormalShape {

   private double[] result = null;

   /**
    * Constructs the union, NormalUnionShape, of its two input shapes.
    *
    * @param a
    *           - (NormalShape) One of the two shapes.
    * @param b
    *           - (NormalShape) The other shape.
    */
   public NormalUnionShape(NormalShape a, NormalShape b) {
      super(a, b);
   }

   /**
    * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#contains(double[],
    *      double[])
    */
   @Override
   public boolean contains(double[] pos0, double[] pos1) {
      final List<MonteCarloSS.Shape> shapes = getShapes();
      for (final MonteCarloSS.Shape shape : shapes)
         if (((NormalShape) shape).contains(pos0, pos1))
            return true;
      return false;
   }

   @Override
   public double getFirstIntersection(double[] pos0, double[] pos1) {
      return (getFirstNormal(pos0, pos1))[3];
   }

   /**
    * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#getFirstNormal(double[],
    *      double[])
    */
   @Override
   public double[] getFirstNormal(double[] pos0, double[] pos1) {

      final double[] nointersection = {0., 0., 0., Double.MAX_VALUE};
      final List<MonteCarloSS.Shape> shapes = getShapes();
      final NormalShape shapeA = (NormalShape) shapes.get(0);
      final NormalShape shapeB = (NormalShape) shapes.get(1);
      int adepth, bdepth;
      double u;

      /*
       * Logic of the algorithm: The algorithm below is a modified form of the
       * "union of dexels" algorithm I designed for a mathematical morphology
       * project. Think of the line parameterized by p = pos0+u*(pos1-pos0) for
       * u from -infinity to infinity. We can characterize the "depth" of parts
       * of this line as 0 if the part is outside both A and B, 1 if it is
       * inside A or B but not both, or 2 if it is inside both. Depth changes
       * only at boundary crossings with shapes A and B, either increasing by 1
       * (for outside-inside) transitions or decreasing by 1 (for inside-outside
       * transitions). (The depth can change by 2 if our line hits A and B
       * boundaries simultaneously.) We are only interested in boundaries for u
       * in (0,1], so we start at u=0 and process boundary crossings in order,
       * updating the depth at each one. If we hit a boundary at which the depth
       * changes from anything to 0 or from 0 to anything, we stop and return
       * this boundary crossing. Depth changes from 1 to 2 or vice versa are
       * "internal boundaries" of the combination, and do not represent
       * boundaries of the union shape, so we keep looking. Alternatively, if we
       * reach u>1 without having found a boundary, we can stop because there
       * are none in (0,1]. The only tricky part is that we don't know when we
       * start at u=0 whether we are inside or outside our object. We could use
       * the contains() methods of the individual shapes, but it is more
       * efficient to wait. Instead we get the first boundary crossing from each
       * shape. (We need this anyway.) If the crossing occurs for u<1, the
       * normal vector is valid, and we can use it to ascertain whether the
       * boundary crossing is inside-out or vice versa. If u>1 the normal vector
       * is not valid, and we have to revert to our fallback plan of using the
       * contains() method.
       */

      // Get 1st A and B intersections and whether we are inside or outside
      final double[] delta = {pos1[0] - pos0[0], pos1[1] - pos0[1], pos1[2] - pos0[2]};
      double nva[] = shapeA.getFirstNormal(pos0, pos1);
      if (nva[3] <= 1.)
         adepth = ((delta[0] * nva[0]) + (delta[1] * nva[1]) + (delta[2] * nva[2])) > 0 ? 1 : 0;
      else { // If the crossing is inside-out, then at p0 we are inside.
         // To come back to: What about delta.nva==0?
         if (shapeA.contains(pos0, pos1))
            return nointersection; // If we were inside at p0 and u>1 there
         // can be no inside-out crossing
         adepth = 0;
      }

      double nvb[] = shapeB.getFirstNormal(pos0, pos1);
      if (nvb[3] <= 1.)
         bdepth = ((delta[0] * nvb[0]) + (delta[1] * nvb[1]) + (delta[2] * nvb[2])) > 0 ? 1 : 0;
      else { // If the crossing is inside-out, then at p0 we are inside.
         // To come back to: What about ?.nvb==0?
         if (shapeB.contains(pos0, pos1))
            return nointersection; // If we were inside at p0 and u>1 there
         // can be no inside-out crossing
         result = nva;
         return nva; // Otherwise there can, and it is at the first A
         // crossing
      }
      int cdepth = adepth + bdepth;

      // See explantion below at first use
      final double EXTRAU = 1.e-10; // This amounts to an Angstrom for a 1
      // meter
      // step.

      for (;;)
         if (nva[3] < nvb[3]) { // shape A provides the first intersection
            if (adepth == cdepth) {
               result = nva;
               return nva; // c toggles from 0 to 1 or vice versa, like A,
               // so this is a boundary
            }

            cdepth = (cdepth == 1 ? 2 : 1); // It wasn't a boundary so we
            // update cdepth

            // Get the next intersection in A
            /*
             * Round off error can cause problems when we try to calculate a u
             * value for the next intersection after our current one. Our new
             * start position at pos0 + u*delta can, because of roundoff error,
             * be just shy of the boundary. We therefore rediscover the same
             * boundary, at a distance of u = 1.e-16 or so! Unfortunately, since
             * we assume each new boundary crossing toggles inside/outside, this
             * messes us up. To avoid this, we advance the position by a tiny
             * extra amount before looking for the next crossing. This amount
             * must be so small that we don't accidentally skip over any
             * boundaries. Step sizes (pos1-pos0) are likely to be on the order
             * of nanometers, so EXTRAU = 1.e-10 means we're safe as long as our
             * shape boundaries are at least a few times 1.e-19 m apart. Since
             * this is smaller than an atomic nucleus, it seems a safe bet! Even
             * for step sizes up to a meter we're safe for boundaries more than
             * 0.1 nm apart.
             */
            u = nva[3] + EXTRAU; // Save the distance to our new start
            // point
            nva = shapeA.getFirstNormal(new double[]{pos0[0] + (u * delta[0]), // This
                                                                               // is
                                                                               // pos0+u*delta
                  pos0[1] + (u * delta[1]), pos0[2] + (u * delta[2])}, pos1); // Find
            // the
            // next
            // one
            // after
            // that

            if (nva[3] < Double.MAX_VALUE)
               nva[3] = (nva[3] * (1. - u)) + u;
            if (nva[3] > 1)
               if (cdepth == bdepth) {
                  result = nvb;
                  return nvb;
               } else
                  return nointersection;
            adepth = adepth ^ 1; // Toggle depth in A
         } else if (nva[3] > nvb[3]) { // Same as above, with A and B roles
            // reversed
            if (bdepth == cdepth) {
               result = nvb;
               return nvb; // c toggles from 0 to 1
               // or vice versa, like
               // B, so this is a
               // boundary
            }
            cdepth = (cdepth == 1 ? 2 : 1); // It wasn't a boundary so we
            // update cdepth
            // Get the next intersection in B
            u = nvb[3] + EXTRAU; // Save the distance to our new start
            // point
            nvb = shapeB.getFirstNormal(new double[]{pos0[0] + (u * delta[0]), // This
                                                                               // is
                                                                               // pos0+u*delta
                  pos0[1] + (u * delta[1]), pos0[2] + (u * delta[2])}, pos1); // Find
            // the
            // next
            // one
            // after
            // that

            if (nvb[3] < Double.MAX_VALUE)
               nvb[3] = (nvb[3] * (1. - u)) + u;
            if (nvb[3] > 1)
               if (cdepth == adepth) {
                  result = nva;
                  return nva;
               } else
                  return nointersection;
            bdepth = bdepth ^ 1; // Toggle depth in B
         } else { // Arrive here only in the unlikely event that we
            // simultaneously hit A and B boundaries. Depth changes
            // by 0 or 2
            final int depthchange = (((adepth ^ 1) - adepth) + (bdepth ^ 1)) - bdepth;
            if (depthchange == 0) { // We simultaneously went into one as we
               // went out of the other
               // Update information for both A and B
               // Get the next intersection in both, A first
               u = nva[3] + EXTRAU; // Save the distance to our new
               // start point
               nva = shapeA.getFirstNormal(new double[]{pos0[0] + (u * delta[0]), // This
                                                                                  // is
                                                                                  // pos0+u*delta
                     pos0[1] + (u * delta[1]), pos0[2] + (u * delta[2])}, pos1); // Find
                                                                                 // the
                                                                                 // next
                                                                                 // one
                                                                                 // after
                                                                                 // that
               if (nva[3] < Double.MAX_VALUE)
                  nva[3] = (nva[3] * (1. - u)) + u;

               // Get the next intersection in B
               u = nvb[3] + EXTRAU; // Save the distance to our new
               // start point
               nvb = shapeB.getFirstNormal(new double[]{pos0[0] + (u * delta[0]), // This
                                                                                  // is
                                                                                  // pos0+u*delta
                     pos0[1] + (u * delta[1]), pos0[2] + (u * delta[2])}, pos1); // Find
                                                                                 // the
                                                                                 // next
                                                                                 // one
                                                                                 // after
                                                                                 // that
               if (nvb[3] < Double.MAX_VALUE)
                  nvb[3] = (nvb[3] * (1. - u)) + u;

               if (nva[3] > 1)
                  // in A
                  if (cdepth != bdepth) { // remember bdepth changed but
                     // we've not
                     // yet updated it. This really means cdepth ==
                     // bdepth
                     result = nvb;
                     return nvb;
                  } else
                     return nointersection;
               if (nvb[3] > 1)
                  // in A
                  if (cdepth != adepth) {// remember adepth changed but
                     // we've not
                     // yet updated it. This really means cdepth ==
                     // adepth
                     result = nva;
                     return nva;
                  } else
                     return nointersection;
               adepth = adepth ^ 1; // Toggle depth in A
               bdepth = bdepth ^ 1; // Toggle depth in B
            } else {
               /*
                * Depth went from 0 to 2 or 2 to 0. Either way, this is a
                * boundary. Return average of the two normal vectors. (nva[3]
                * and nvb[3] are the same, so either will do.)
                */
               result = new double[]{(nva[0] + nvb[0]) / 2., (nva[1] + nvb[1]) / 2., (nva[2] + nvb[2]) / 2., nva[3]};
               return result;
            }
         } // End simultaneous boundaries block
   } // End getFirstNormal()

   @Override
   public double[] getPreviousNormal() {
      return result;
   }

}
