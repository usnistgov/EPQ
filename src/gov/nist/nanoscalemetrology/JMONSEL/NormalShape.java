/**
 *
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape;

/**
 * <p>
 * An extension of the NISTMonte Shape interface. The Shape interface specifies
 * methods that allow one to determine whether a point is contained within the
 * shape and whether and where a specified line segment intersects a boundary. A
 * NormalShape must additionally provide a method that returns the outward
 * pointing normal vector at the point of intersection. This normal vector is
 * required by barrier crossing models, since the work function change at an
 * interface can cause an electron to be totally internally reflected or
 * transmitted with refraction depending upon the angle of incidence.
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
public interface NormalShape extends Shape {
	/**
	 * <p>
	 * contains - This is an overloaded version of the Shape class contains()
	 * method. Its definition of containment is somewhat more elaborate, with the
	 * intention of removing ambiguity for pos0 on the boundary of the shape. The
	 * idea is that when pos0 is on a boundary shared by two shapes (imagine two
	 * spheres touching at a point or a pair of cubes with a shared side) the
	 * contains() method should assign pos0 uniquely to one shape or the other. We
	 * want to avoid the situation where pos0 is assigned to both or to neither.
	 * When pos0 is not on the boundary, i.e., it and a neighborhood around it are
	 * inside the shape or outside, this function operates exactly like
	 * Shape.contains(). When pos0 is on the boundary, it uses the following
	 * criteria to break the tie:
	 * </p>
	 * <p>
	 * If an infinitesimal displacement from pos0 in the direction of pos1 moves off
	 * of the boundary towards the inside, contains() returns true. If it moves to
	 * the outside it returns false. This is ordinarily checked by evaluating the
	 * component of pos1-pos0 in the direction of the shape's outward pointing
	 * surface normal at pos0. If it is negative pos0-&gt;pos1 represents an inbound
	 * segment and contains returns true. If it is positive contains() returns
	 * false. If it is 0 (pos1-pos0 is tangent to the surface at pos0) this vector
	 * component test is not distinguishing, but the idea of using an infinitesimal
	 * displacement may still be decisive. For example, if the surface is concave at
	 * pos0, the tangent vector is outbound and contains() returns false. The
	 * reverse is true if the surface is convex.
	 * </p>
	 * <p>
	 * Finally, if pos1-pos0 lies within a flat boundary (e.g., a plane) we are left
	 * only with purely arbitrary criteria to distinguish inside from out. This is
	 * done on the basis of the surface normal vector at pos0. The shape either
	 * contains or does not contain pos0 as the x component of its surface normal is
	 * respectively positive or negative. If the x component is 0, the y component
	 * is used. If that is also 0, then the z component is used. (These components
	 * may not all be 0 because the normal vector has unit length.)
	 * </p>
	 *
	 * @param pos0 double[] - 3 element array, initial position
	 * @param pos1 double[] - 3 element array, final position
	 * @return boolean - Returns true if pos0 is inside the shape, false if not.
	 */
	public boolean contains(double[] pos0, double[] pos1);

	/**
	 * <p>
	 * getFirstNormal - Returns the outward pointing normal vector at the point
	 * where a ray drawn from pos0 in the direction of pos1 first intersects a
	 * boundary of the shape. This routine returns a double[] of length 4, the first
	 * 3 elements of which are the components of this normal vector. Since
	 * determining this vector necessarily entails determining u, the fraction of
	 * the distance from pos0 to pos1 at which the first intersection point occurs,
	 * this method returns u in the last element of the array. This saves duplicate
	 * computation because routines that need the getFirstNormal() method do not
	 * also need to call getFirstIntersection(). As with the parent Shape interface,
	 * NormalShape may be optimized when u is greater than 1 by returning a number
	 * greater than 1 but not equal to the real u when determining the real u would
	 * involve significant additional effort. Likewise, the normal vector need not
	 * be calculated when u&gt;1.
	 * </p>
	 *
	 * @param pos0 double[] - 3 element array
	 * @param pos1 double[] - 3 element array
	 * @return double[] - A 4-element array. If a ray from pos0 in the direction of
	 *         pos1 intersects a boundary of the shape, the first 3 elements of this
	 *         array are the components of the outward pointing normal of the shape
	 *         boundary first intersected. The final value is the fraction of the
	 *         length from pos0 to pos1 at which the first intersection occurs. If
	 *         the ray does not intersect a boundary, the first 3 elements are
	 *         unspecified and the last one is Double.MAX_VALUE.
	 */
	double[] getFirstNormal(double[] pos0, double[] pos1);

	/**
	 * <p>
	 * getPreviousNormal - Returns a double[], the first 3 elements of which are the
	 * components of the normal vector that corresponds to the most recent call to
	 * either getFirstIntersection or getFirstNormal. Notice that this means the
	 * Shape class getFirstIntersection routine will ordinarily need to be
	 * overridden in order that this vector be calculated and cached. The return
	 * value when the previous call to getFirstNormal and getFirstIntersection found
	 * no intersection is unspecified.
	 * </p>
	 *
	 * @return double[] - A 3-element array giving the components of the normal
	 *         vector.
	 */
	double[] getPreviousNormal();
}
