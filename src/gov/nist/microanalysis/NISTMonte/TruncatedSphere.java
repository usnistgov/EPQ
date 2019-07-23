
package gov.nist.microanalysis.NISTMonte;

import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Implements the MonteCarloSS.Shape interface for a sphere truncated at the top
 * and bottom.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class TruncatedSphere
   extends Intersection {
   private final Sphere mSphere;

   /**
    * Constructs a Sphere with the specified center and radius truncated at the
    * upper and lower bounds in the z-dimension.
    *
    * @param center double[] - The x,y &amp; z coordinates of the center of the
    *           sphere (meters)
    * @param radius double - The radius of the sphere in meters.
    * @param top in range (-radius, radius) or Double.NaN - Cut off top of
    *           sphere
    * @param bottom in range (top, radius) or Double.NaN - Cut off bottom of
    *           sphere
    */
   public TruncatedSphere(double[] center, double radius, double top, double bottom) {
      super();
      mSphere = new Sphere(center, radius);
      add(mSphere);
      if(top > bottom) {
         final double tmp = top;
         top = bottom;
         bottom = tmp;
      }
      if((!Double.isNaN(top)) && (top > -radius) && (top < radius))
         add(new MultiPlaneShape.Plane(Math2.MINUS_Z_AXIS, Math2.add(center, Math2.v3(0.0, 0.0, top))));
      if((!Double.isNaN(bottom)) && (bottom > top) && (bottom < radius))
         add(new MultiPlaneShape.Plane(Math2.Z_AXIS, Math2.add(center, Math2.v3(0.0, 0.0, bottom))));
   }

   public TruncatedSphere(double[] center, double radius, double[] normal, double distance) {
      super();
      mSphere = new Sphere(center, radius);
      add(mSphere);
      add(normal, distance);
   }

   public void add(double[] normal, double distance) {
      normal = Math2.normalize(normal);
      if(Math.abs(distance) < getRadius())
         add(new MultiPlaneShape.Plane(Math2.normalize(normal), Math2.add(getCenter(), Math2.multiply(distance, normal))));
   }

   public static TruncatedSphere buildFlatTop(double[] center, double radius, double top) {
      return new TruncatedSphere(center, radius, top, Double.NaN);
   }

   public static TruncatedSphere buildFlatBottom(double[] center, double radius, double bottom) {
      return new TruncatedSphere(center, radius, Double.NaN, bottom);
   }

   /**
    * getRadius - Returns the sphere's radius
    * 
    * @return double
    */
   public double getRadius() {
      return mSphere.getRadius();
   }

   /**
    * Gets the current value assigned to center
    * 
    * @return Returns the coordinates of the sphere's center.
    */
   public double[] getCenter() {
      return mSphere.getCenter();
   }
}
