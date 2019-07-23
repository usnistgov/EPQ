package gov.nist.nanoscalemetrology.JMONSEL;

import java.util.List;
import java.util.ListIterator;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.NISTMonte.MultiPlaneShape;
import gov.nist.microanalysis.Utility.Math2;

// import gov.nist.microanalysis.NISTMonte.MultiPlaneShape.Plane;

/**
 * <p>
 * Extends the NISTMonte MultiPlaneShape class in order to implement the
 * NormalShape interface. This requires one additional method, to return the
 * vector normal to the shape at the point of intersection.
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
public class NormalMultiPlaneShape
   extends
   MultiPlaneShape
   implements
   NormalShape {

   /*
    * The following local data are used to locally cache information about the
    * shape for speed optimization. During testing I found that I saved 87% of
    * execution time by caching the information locally like this. The integrity
    * of the cache is preserved by overriding MultiPlaneShape methods that
    * change the object's state. (This is not the best, because the addition of
    * methods to MultiPlaneShape could break this class.)
    */

   private double[] result = null;

   transient private List<Plane> mPlanes;

   transient private int numPlanes; // # of planes in this shape

   /*
    * narray[i] is the normal vector of the ith plane
    */
   transient private double[][] narray;

   /*
    * carray[i] is the point contained in the ith plane
    */
   transient private double[][] carray;

   /**
    * Constructs a NormalMultiPlaneShape with no planes. Use addPlane() to add
    * planes.
    */
   public NormalMultiPlaneShape() {
      super();
   }

   private void updateCach() {
      mPlanes = getPlanes();
      numPlanes = mPlanes.size();
      narray = new double[numPlanes][];
      carray = new double[numPlanes][];
      int i;
      ListIterator<Plane> it;
      for(i = 0, it = mPlanes.listIterator(); it.hasNext(); i++) {
         final Plane p = it.next(); // Get next plane
         narray[i] = p.getNormal(); // This plane's normal vector
         carray[i] = p.getPoint(); // A point in this plane
      }
   }

   /**
    * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#getFirstNormal(double[],
    *      double[])
    */
   @Override
   public double[] getFirstNormal(double[] pos0, double[] pos1) {
      /*
       * Explanation of the algorithm: Consider the line described by
       * pos0+u*(pos1-pos0) for u between -infinity and infinity. Suppose this
       * line intersects the ith plane at ui. Then the part of the line that is
       * inside the half space defined by that plane is an interval, either
       * (-infinity,ui] or [ui,infinity) depending upon whether it is an
       * insidethisplane->outside or outside->insidethisplane transition. The n
       * planes that make up this object define n such intervals. The
       * intersection (in the set theoretic sense) of all of these intervals,
       * call it [umin,umax], is the part of the line that is inside the shape.
       * If umin>0 then our starting position at u=0 is outside of the shape and
       * umin represents the closest boundary crossing (outside to in). If
       * umin<0 then we start inside and umax represents the outward bound
       * crossing. The result can be computed with a single loop through the
       * planes by using two variables, umin and umax, to keep track of the end
       * points of the intersection interval. umax starts at "infinity" and can
       * only decrease. umin starts at -"infinity" and can only increase.
       * Whenever we change an end point we store the index of the current
       * plane. At the end we choose a u value corresponding to the nearest
       * crossing with 0<u<=1 and the corresponding index is used to access the
       * normal vector.
       */

      /*
       * We can optimize by noticing that certain situations signal immediately
       * that no boundary crossing is possible. These situations are: (1) the
       * line is parallel to and outside of any plane. (2) umax < 0 This signals
       * that the entire "inside" is "behind" our line segment, at negative u.
       * (3) umax < umin This signals that we have at least two completely
       * disjoint intervals. There can be no position on this line that is
       * inside all of them. (4) umin > 1 This signals that the entire "inside"
       * is beyond the endpoint of our line segment. If any of these situations
       * arise as we loop through the planes, we immediately abort, returning
       * u>1 and undefined normal vector.
       */

      double umin = Double.NEGATIVE_INFINITY; // Starting interval is the
      // whole real line
      double umax = Double.POSITIVE_INFINITY;
      double u;
      int minindex = -1; // Stores index of plane responsible for umin
      int maxindex = -1; // Same for umax. Initial values are illegal
      // indices.
      result = new double[] {
         0.,
         0.,
         0.,
         Double.MAX_VALUE
      }; // Initial value
      // designates
      // no intersection

      final double[] delta = {
         pos1[0] - pos0[0],
         pos1[1] - pos0[1],
         pos1[2] - pos0[2]
      };
      for(int i = 0; i < numPlanes; i++) {
         final double[] pos0minusc = {
            pos0[0] - carray[i][0],
            pos0[1] - carray[i][1],
            pos0[2] - carray[i][2]
         };
         /*
          * Note significance of the sign of the next two variables numerator<0
          * means pos0 is inside the current plane; numerator>0 means it's
          * outside. denominator<0 means the line segment and the plane normal
          * point in opposite directions. I.e., this intersection is an
          * outside->inside transition. denominator>0 means the opposite.
          * denominator==0 means the trajectory is parallel to the plane.
          */
         final double numerator = (pos0minusc[0] * narray[i][0]) + (pos0minusc[1] * narray[i][1])
               + (pos0minusc[2] * narray[i][2]);
         final double denominator = (delta[0] * narray[i][0]) + (delta[1] * narray[i][1]) + (delta[2] * narray[i][2]);
         if(denominator == 0) {
            /*
             * If the trajectory is parallel to the plane there are no
             * intersections. If it starts inside it's always inside. If it
             * starts outside it's always outside. In or Out is determined by
             * the numerator. numerator<0, or =0 with tie break = true, means it
             * is inside. In this case we continue looping, searching for
             * intersections with other planes of this shape. Otherwise, we
             * return u>1.
             */
            if((numerator < 0) || ((numerator == 0) && containsTieBreak(narray[i])))
               continue;
            return result;
         }
         u = -numerator / denominator; // Compute intersection point
         if(denominator > 0) { // This is an insidethisplane->outside
            // transition. It changes umax.
            if(u < umax) {
               if((u < 0)
                     || (u <= umin)) /*
                                      * If the new umax is < 0 the "inside" is
                                      * behind our line segment If the new umax
                                      * is < umin, this plane's inside and an
                                      * earlier one are disjoint. If umax=umin,
                                      * the trajectory enters and leaves the
                                      * shape at the same point, i.e., it is
                                      * tangent to the surface. Since our shape
                                      * is convex, a line can only be tangent on
                                      * the OUTside, so this counts as a
                                      * non-intersection. In any of these cases,
                                      * abort and return no intersection.
                                      */
                  return result;
               umax = u;
               maxindex = i; // remember index of this plane
            }
         } else if(u > umin) { /*
                                * It changes umin. If the new umin is > 1 the
                                * "inside" is beyond the end of our line
                                * segment. If it is >umax this plane's inside
                                * and an earlier one are disjoint. Return
                                * "no intersection" in either case.
                                */
            if((u > 1) || (u >= umax))
               return result;
            umin = u;
            minindex = i; // Remember index of this plane
         } // end if
      } // end for

      // When we arrive here [umin,umax] defines the completed intersection
      // interval
      if(umin > 0) { // Our boundary crossing is outside -> inside at umin
         result[3] = umin;
         result[0] = narray[minindex][0];
         result[1] = narray[minindex][1];
         result[2] = narray[minindex][2];
         return result;
      } // Otherwise our starting position was already inside
      if((umax <= 1) && (umax > 0.)) { // Our boundary crossing is inside ->
         // outside at umax<1
         result[3] = umax;
         result[0] = narray[maxindex][0];
         result[1] = narray[maxindex][1];
         result[2] = narray[maxindex][2];
         return result;
      } // Otherwise the entire pos0, pos1 interval lies inside
      return result; // return "no intersection"
   }

   @Override
   public double getFirstIntersection(double[] pos0, double[] pos1) {
      return (getFirstNormal(pos0, pos1))[3];
   }

   /**
    * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#contains(double[],
    *      double[])
    */
   @Override
   public boolean contains(double[] pos0, double[] pos1) {

      boolean didDelta = false;
      double p0cdotn;
      double[] delta = null;
      // Loop over all planes in the shape
      for(int i = 0; i < numPlanes; i++) {
         final double p0c[] = {
            pos0[0] - carray[i][0],
            pos0[1] - carray[i][1],
            pos0[2] - carray[i][2]
         };
         p0cdotn = (p0c[0] * narray[i][0]) + (p0c[1] * narray[i][1]) + (p0c[2] * narray[i][2]);
         if(p0cdotn > 0.)
            return false;
         if(p0cdotn == 0.) {
            if(!didDelta) {
               delta = new double[] {
                  pos1[0] - pos0[0],
                  pos1[1] - pos0[1],
                  pos1[2] - pos0[2]
               };
               didDelta = true;
            }
            final double deltadotn = (delta[0] * narray[i][0]) + (delta[1] * narray[i][1]) + (delta[2] * narray[i][2]);
            if(deltadotn > 0.)
               return false;
            if((deltadotn == 0.) && !containsTieBreak(narray[i]))
               return false;
         }
      }
      return true;
   }

   /**
    * To be called if pos0 and pos1 both lie within a plane. This method
    * arbitrarily assigns containment based on a tie-break algorithm that is a
    * function of the plane normal, which is supplied as a parameter.
    *
    * @param normal - the plane's normal vector
    * @return - true if the plane contains the point, false otherwise.
    */
   private boolean containsTieBreak(double[] normal) {
      if(normal[0] < 0.)
         return false;
      if(normal[0] == 0.) {
         if(normal[1] < 0.)
            return false;
         if(normal[1] == 0.)
            if(normal[2] < 0.)
               return false;
      }
      return true;
   }

   @Override
   public double[] getPreviousNormal() {
      return result;
   }

   /**
    * addPlane - Add a new bounding plane defined by 3 non-colinear points.
    * Instead of a point and normal vector, a plane may be defined by 3 points.
    * The order of the points determines which side of the plane is inside and
    * which out. The points should be given in order clockwise when viewed from
    * inside the plane, counterclockwise when viewed from outside the plane.
    *
    * @param p1 double[]
    * @param p2 double[]
    * @param p3 double[]
    */
   public void addPlane(double[] p1, double[] p2, double[] p3)
         throws EPQFatalException {
      final double[] normal = Math2.cross(Math2.minus(p2, p1), Math2.minus(p3, p1));
      if(Math2.magnitude(normal) == 0.)
         throw new EPQFatalException("addPlane: 3 supplied points must be non-colinear.");
      addPlane(normal, p1);
   }

   /*
    * The following 3 methods override parent class methods to make sure we keep
    * our local cache up to date.
    */
   /**
    * @see gov.nist.microanalysis.NISTMonte.MultiPlaneShape#addPlane(double[],
    *      double[])
    */
   @Override
   public void addPlane(double[] normal, double[] point) {
      super.addPlane(normal, point);
      updateCach();
   }

   /**
    * Returns the number of planes in this shape
    *
    * @return - the number of planes in this shape
    */
   public int getNumPlanes() {
      return numPlanes;
   }

   /**
    * The equation of any plane can be written as n.x = b. This method returns n
    * for the plane of this shape specified by the given index. n is the
    * outward-pointing normal vector of the plane.
    *
    * @param index - The index of the plane
    * @return - the outward-pointing normal vector of the indexed plane
    */
   public double[] getNormal(int index) {
      return narray[index].clone();
   }

   /**
    * The equation of any plane can be written as n.x = b. This method returns b
    * for the plane of this shape specified by the given index. b is a constant
    * such that b*n is the point on the plane nearest the origin.
    *
    * @param index
    * @return - the value of b constant associated with the indexed plane
    */
   public double getB(int index) {
      return Math2.dot(narray[index], carray[index]);
   }

   // See ITransform for JavaDoc
   @Override
   public void rotate(double[] pivot, double phi, double theta, double psi) {
      super.rotate(pivot, phi, theta, psi);
      updateCach();
   }

   // See ITransform for JavaDoc
   @Override
   public void translate(double[] distance) {
      super.translate(distance);
      updateCach();
   }
}
