package gov.nist.microanalysis.NISTMonte;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

import gov.nist.microanalysis.EPQLibrary.ITransform;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.Transform3D;

/**
 * <p>
 * Implements the MonteCarloSS.Shape interface for a sphere.
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

public class Sphere implements MonteCarloSS.Shape, ITransform, TrajectoryVRML.IRender {
   private final double mRadius; // meters
   private double[] mCenter; // = new double[3]; x,y & z in meters

   /**
    * Sphere - Constructs a Sphere object with the specified center and radius.
    * 
    * @param center
    *           double[] - The x,y &amp; z coordinates of the center of the
    *           sphere (meters)
    * @param radius
    *           double - The radius of the sphere in meters.
    */
   public Sphere(double[] center, double radius) {
      mCenter = center.clone();
      mRadius = radius;
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#contains(double[])
    */
   @Override
   public boolean contains(double[] pos) {
      return (Math2.sqr(pos[0] - mCenter[0]) + Math2.sqr(pos[1] - mCenter[1]) + Math2.sqr(pos[2] - mCenter[2])) <= (mRadius * mRadius);
   }

   /**
    * getRadius - Returns the sphere's radius
    * 
    * @return double
    */
   public double getRadius() {
      return mRadius;
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#getFirstIntersection(double[],
    *      double[])
    */
   @Override
   public double getFirstIntersection(double[] pos0, double[] pos1) {
      // Compute the intersection of the line between pos0 and pos1 and the
      // shell of the sphere.
      final double[] d = Math2.minus(pos1, pos0);
      final double[] m = Math2.minus(pos0, mCenter);
      final double ma2 = -2.0 * Math2.dot(d, d);
      final double b = 2.0 * Math2.dot(m, d);
      final double c2 = 2.0 * (Math2.dot(m, m) - (mRadius * mRadius));
      final double f = (b * b) + (ma2 * c2);
      if (f >= 0) {
         double up = (b + Math.sqrt(f)) / ma2;
         double un = (b - Math.sqrt(f)) / ma2;
         if (up < 0.0)
            up = Double.MAX_VALUE;
         if (un < 0.0)
            un = Double.MAX_VALUE;
         final double res = Math.min(up, un);
         assert (res == Double.MAX_VALUE)
               || ((Math2.magnitude(Math2.plus(m, Math2.multiply(res, d))) - mRadius) < Math.max(1.0e-12, Math2.distance(pos0, pos1) * 1.0e-9))
               : Double.toString(Math2.magnitude(Math2.plus(m, Math2.multiply(res, d))) - mRadius);
         return res;
      }
      return Double.MAX_VALUE;
   }

   /**
    * getInitialPoint - Used when Sphere represents the chamber region. The
    * initial point is the location of the electron gun.
    * 
    * @return double[]
    */
   public double[] getInitialPoint() {
      final double[] res = new double[3];
      res[0] = mCenter[0];
      res[1] = mCenter[1];
      res[2] = mCenter[2] - (0.999 * mRadius); // just inside...
      return res;
   }

   /**
    * getPointAt - Returns the coordinates of the boundary point at the
    * specified zenith (phi) and azimuthal (theta) angles.
    * 
    * @param phi
    *           double
    * @param theta
    *           double
    * @param frac
    *           double fraction of the total radius
    * @return double[] - A spatial 3 vector
    */
   public double[] getPointAt(double phi, double theta, double frac) {
      final double[] res = new double[3];
      res[2] = mCenter[2] + (mRadius * frac * Math.cos(phi));
      res[1] = mCenter[1] + (mRadius * frac * Math.sin(phi) * Math.sin(theta));
      res[0] = mCenter[0] + (mRadius * frac * Math.sin(phi) * Math.cos(theta));
      return res;
   }

   // JavaDoc in ITransform
   @Override
   public void rotate(double[] pivot, double phi, double theta, double psi) {
      mCenter = Transform3D.rotate(mCenter, pivot, phi, theta, psi);
   }

   // JavaDoc in ITransform
   @Override
   public void translate(double[] distance) {
      mCenter[0] += distance[0];
      mCenter[1] += distance[1];
      mCenter[2] += distance[2];
   }

   @Override
   public void render(TrajectoryVRML.RenderContext vra, Writer wr) throws IOException {
      final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
      nf.setMaximumFractionDigits(2);
      nf.setGroupingUsed(false);
      final Color color = vra.getCurrentColor();
      final String trStr = nf.format(vra.getTransparency());
      final String colorStr = nf.format(color.getRed() / 255.0) + " " + nf.format(color.getGreen() / 255.0) + " "
            + nf.format(color.getBlue() / 255.0);
      wr.append("Transform {\n");
      wr.append(" translation " + nf.format(mCenter[0] / TrajectoryVRML.SCALE) + " " + nf.format(mCenter[1] / TrajectoryVRML.SCALE) + " "
            + nf.format(mCenter[2] / TrajectoryVRML.SCALE) + "\n");
      wr.append(" children [\n");
      wr.append("  Shape {\n");
      wr.append("   geometry Sphere { radius " + nf.format(mRadius / TrajectoryVRML.SCALE) + "}\n");
      wr.append("   appearance Appearance {\n");
      wr.append("    material Material {\n");
      wr.append("     emissiveColor " + colorStr + "\n");
      wr.append("     transparency " + trStr + "\n");
      wr.append("    }\n");
      wr.append("   }\n");
      wr.append("  }\n");
      wr.append(" ]\n");
      wr.append("}");
      wr.flush();
   }

   /**
    * Gets the current value assigned to center
    * 
    * @return Returns the coordinates of the sphere's center.
    */
   public double[] getCenter() {
      return mCenter.clone();
   }

   @Override
   public String toString() {
      return "Sphere[" + Arrays.toString(mCenter) + ", r=" + Double.toString(mRadius) + "]";
   }

}
