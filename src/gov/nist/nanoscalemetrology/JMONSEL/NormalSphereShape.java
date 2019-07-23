package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.NISTMonte.Sphere;
import gov.nist.microanalysis.Utility.Transform3D;

/**
 * <p>
 * Extends the Sphere class in order to implement the NormalShape interface.
 * This requires one additional method, to return the vector normal to the
 * sphere at the point of intersection.
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
public class NormalSphereShape
   extends Sphere
   implements NormalShape {

   /*
    * NISTMonte's Sphere class does not, as of this writing, provide a get
    * function for the center coordinates of the sphere. This creates a problem,
    * since we must know the center to compute the interesection and normal
    * vector. The only solution that does not involve changing the parent class
    * is to keep our own copy. This is OK as long as the user has no way of
    * changing shape parameters in the parent class without us knowing about it.
    * Currently, the only way to change the parent class shape parameters is
    * through the rotate and translate functions. We therefore must override
    * those below.
    */
   private final double mRadius, mRadiusSquared; // meters

   private double[] mCenter; // = new double[3]; x,y & z in meters

   private double[] result = null;

   /**
    * Constructs a NormalSphereShape with the specified center coordinates and
    * radius.
    *
    * @param center - 3 coordinates specifying the center position
    * @param radius - radius in meters
    */
   public NormalSphereShape(double[] center, double radius) {
      super(center, radius);
      mCenter = center.clone(); // Make our own copy of these.
      mRadius = radius;
      mRadiusSquared = mRadius * mRadius;
   }

   @Override
   public boolean contains(double[] pos0, double[] pos1) {
      final double[] posminuscenter = {
         pos0[0] - mCenter[0],
         pos0[1] - mCenter[1],
         pos0[2] - mCenter[2]
      };
      final double distSquared = (posminuscenter[0] * posminuscenter[0]) + (posminuscenter[1] * posminuscenter[1])
            + (posminuscenter[2] * posminuscenter[2]);
      if(distSquared != mRadiusSquared)
         return distSquared < mRadiusSquared;
      /*
       * Arrive here if pos is on the boundary. Is this inside or outside? We
       * break the tie by saying it is inside if the trajectory is inbound (has
       * a component opposite the normal vector at the surface). Otherwise it is
       * outside.
       */
      final double[] delta = {
         pos1[0] - pos0[0],
         pos1[1] - pos0[1],
         pos1[2] - pos0[2]
      };
      return ((delta[0] * posminuscenter[0]) + (delta[1] * posminuscenter[1]) + (delta[2] * posminuscenter[2])) < 0;
   }

   /**
    * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#getFirstNormal(double[],
    *      double[])
    */
   @Override
   public double[] getFirstNormal(double[] pos0, double[] pos1) {
      result = new double[] {
         0.,
         0.,
         0.,
         Double.MAX_VALUE
      };
      boolean posintersection = false;
      // Compute the intersection of the line between pos0 and pos1 and the
      // shell of the sphere.
      final double[] deltap = {
         pos1[0] - pos0[0],
         pos1[1] - pos0[1],
         pos1[2] - pos0[2]
      };
      final double[] p0wrtcenter = {
         pos0[0] - mCenter[0],
         pos0[1] - mCenter[1],
         pos0[2] - mCenter[2]
      };

      // u is from solution of quadratic eqn. (-b^2 +/- Sqrt(b^2-4 a c))/(2a).
      // Following computes the constants
      final double a = (deltap[0] * deltap[0]) + (deltap[1] * deltap[1]) + (deltap[2] * deltap[2]);
      // Following is actually b/2
      final double b = (p0wrtcenter[0] * deltap[0]) + (p0wrtcenter[1] * deltap[1]) + (p0wrtcenter[2] * deltap[2]);
      final double c = ((p0wrtcenter[0] * p0wrtcenter[0]) + (p0wrtcenter[1] * p0wrtcenter[1])
            + (p0wrtcenter[2] * p0wrtcenter[2])) - (mRadius * mRadius);
      double term = (b * b) - (a * c); // This is actually (b^2-4ac)/4 Remember
      // the
      // factor of 2 in b.

      /*
       * term is the part under the square root in the quadratic eqn. If it is
       * negative, neither solution is real. This corresponds to a ray that
       * begins outside the sphere and does not intersect it. term = 0
       * corresponds to pos0 outside the sphere and the ray tangent to the
       * sphere, intersecting at one point. This ray is outward bound from the
       * sphere at the point of intersection so we count this point as inside
       * the containing shape rather than this sphere. That is, this is not a
       * true intersection, in the sense that the ray never transitions from
       * outside to inside or vice versa. Only for term > 0 do we consider there
       * to be a true intersection. In this case there are two, and we must
       * choose the nearest positive one or, in the case where both are
       * negative, neither.
       */

      if(term > 0.) { // There are 2 intersections
         /*
          * Determine the nearer one. Exclude u=0 on the grounds that u=0 means
          * the trajectory STARTS on the boundary. Everything having to do with
          * such an intersection should have been addressed on the last leg of
          * the trajectory, when it first occurred.
          */
         term = Math.sqrt(term) / a;
         final double minusbovera = -b / a;
         final double u1 = minusbovera + term;
         final double u2 = minusbovera - term;
         if(u1 > 0) {
            result[3] = u1;
            posintersection = true;
         }
         if((u2 > 0) && (u2 < u1)) {
            result[3] = u2;
            posintersection = true;
         }

         // Compute normal vector, but only if one of the intersections was
         // for
         // u>0
         if(posintersection) {
            result[0] = (p0wrtcenter[0] + (result[3] * deltap[0])) / mRadius;
            result[1] = (p0wrtcenter[1] + (result[3] * deltap[1])) / mRadius;
            result[2] = (p0wrtcenter[2] + (result[3] * deltap[2])) / mRadius;
         }
      }

      return result;
   }

   @Override
   public double getFirstIntersection(double[] pos0, double[] pos1) {
      result = getFirstNormal(pos0, pos1);
      return result[3];
   }

   // JavaDoc in ITransform
   // We must override parent rotate and translate functions in order to
   // maintain synchronization between our local shape parameters and those
   // in the parent class.
   @Override
   public void rotate(double[] pivot, double phi, double theta, double psi) {
      mCenter = Transform3D.rotate(mCenter, pivot, phi, theta, psi);
      super.rotate(pivot, phi, theta, psi);
   }

   // JavaDoc in ITransform
   @Override
   public void translate(double[] distance) {
      mCenter[0] += distance[0];
      mCenter[1] += distance[1];
      mCenter[2] += distance[2];
      super.translate(distance);
   }

   @Override
   public double[] getPreviousNormal() {
      return result;
   }
}
