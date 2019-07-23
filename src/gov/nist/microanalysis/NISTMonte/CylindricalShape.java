package gov.nist.microanalysis.NISTMonte;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Locale;

import gov.nist.microanalysis.EPQLibrary.ITransform;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.Transform3D;

/**
 * <p>
 * A MonteCarloSS.Shape representing a cylindrical shape of arbitrary axis and
 * radius.
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

public class CylindricalShape
   implements MonteCarloSS.Shape, ITransform, TrajectoryVRML.IRender, Cloneable {
   private double[] mEnd0; // The position of the center of one end cap
   private double[] mDelta; // The length and direction of the axis
   final private double mRadius2; // The sqr(radius) of the cylinder
   final private double mLen2; // Cache the length squared...
   private final double mDelta2;

   static private final double EPSILON = 1.0e-40;

   /**
    * MCSS_CylindricalShape - Create a cylindrical shape from the end points
    * specified with the specified radius.
    * 
    * @param end0 double[]
    * @param end1 double[]
    * @param radius double
    */
   public CylindricalShape(double[] end0, double[] end1, double radius) {
      mEnd0 = end0.clone();
      mDelta = new double[] {
         end1[0] - end0[0],
         end1[1] - end0[1],
         end1[2] - end0[2]
      };
      mRadius2 = radius * radius;
      if(mRadius2 < 1.0e-30)
         throw new IllegalArgumentException("The cylinder radius is unrealistically small.");
      mLen2 = Math2.sqr(mDelta[0]) + Math2.sqr(mDelta[1]) + Math2.sqr(mDelta[2]);
      if(mLen2 < 1.0e-30)
         throw new IllegalArgumentException("The cylinder length is unrealistically small.");
      mDelta2 = Math2.dot(mDelta, mDelta);
   }

   public CylindricalShape clone(Object obj) {
      final CylindricalShape cs = (CylindricalShape) obj;
      return new CylindricalShape(cs.getEnd0(), cs.getEnd1(), cs.getRadius());
   }

   /**
    * closestPointOnAxis - Returns the parameterized coordinate of the closest
    * point on the cylinder axis to the specified point.
    * 
    * @param p double[]
    * @return double
    */
   final private double closestPointOnAxis(double[] p) {
      return ((mDelta[0] * (p[0] - mEnd0[0])) + (mDelta[1] * (p[1] - mEnd0[1])) + (mDelta[2] * (p[2] - mEnd0[2]))) / mLen2;
   }

   /**
    * distanceSqr - Distance (squared) from the parameterized point on the
    * cylinder axis to the specified point.
    * 
    * @param p double[] - The off axis point
    * @param u double - The parameterized point
    * @return double
    */
   final private double distanceSqr(double[] p, double u) {
      return Math2.sqr(p[0] - (mEnd0[0] + (u * mDelta[0]))) + Math2.sqr(p[1] - (mEnd0[1] + (u * mDelta[1])))
            + Math2.sqr(p[2] - (mEnd0[2] + (u * mDelta[2])));
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#contains(double[])
    */
   @Override
   public boolean contains(double[] pos) {
      // project pos onto the line defined by end0 and end1.
      final double u = closestPointOnAxis(pos);
      // Is this point between end0 and end1 and is pos^2 <= mRadius from the
      // line from end0 to end1?
      return (u >= 0) && (u <= 1.0) && (distanceSqr(pos, u) <= mRadius2);
   }

   /**
    * getEnd0 - Get one end point of the cylinder axis.
    * 
    * @return double[]
    */
   public double[] getEnd0() {
      return mEnd0.clone();
   }

   /**
    * getEnd1 - Get one the other end point of the cylinder axis.
    * 
    * @return double[]
    */
   public double[] getEnd1() {
      return new double[] {
         mEnd0[0] + mDelta[0],
         mEnd0[1] + mDelta[1],
         mEnd0[2] + mDelta[2]
      };
   }

   static final double checkT(double t) {
      return t >= 0.0 ? t : Double.MAX_VALUE;
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#getFirstIntersection(double[],
    *      double[])
    */
   @Override
   public double getFirstIntersection(double[] sa, double[] sb) {
      if(true) {
         double t0 = Double.MAX_VALUE, t1 = Double.MAX_VALUE, tc = Double.MAX_VALUE;
         final double[] n = Math2.minus(sb, sa);
         final double nd = Math2.dot(n, mDelta);
         if(nd != 0.0) {
            // Check end cap 0
            double t = Math2.dot(mDelta, Math2.minus(mEnd0, sa)) / nd;
            if(t > 0.0) {
               final double[] pt = Math2.minus(Math2.pointBetween(sa, sb, t), mEnd0);
               if(Math2.dot(pt, pt) < mRadius2)
                  t0 = t;
            }
            // Check end cap 1
            final double[] end1 = Math2.plus(mEnd0, mDelta);
            t = Math2.dot(mDelta, Math2.minus(end1, sa)) / nd;
            if(t > 0.0) {
               final double[] pt = Math2.minus(Math2.pointBetween(sa, sb, t), end1);
               if(Math2.dot(pt, pt) < mRadius2)
                  t1 = t;
            }
         }
         final double a = (mDelta2 * Math2.dot(n, n)) - (nd * nd);
         if(Math.abs(a) > EPSILON) {
            final double[] m = Math2.minus(sa, mEnd0);
            final double mn = Math2.dot(m, n);
            final double b = (mDelta2 * mn) - (nd * Math2.dot(m, mDelta));
            final double md = Math2.dot(m, mDelta);
            // Consider the side of the cylinder
            final double c = (mDelta2 * (Math2.dot(m, m) - mRadius2)) - (md * md);
            final double discr = (b * b) - (a * c);
            if(discr >= 0.0) {
               final double tm = (-b - Math.sqrt(discr)) / a;
               final double tp = (-b + Math.sqrt(discr)) / a;
               final double t = Math.min(tm > 0.0 ? tm : Double.MAX_VALUE, tp > 0.0 ? tp : Double.MAX_VALUE);
               if((t != Double.MAX_VALUE) && ((md + (t * nd)) >= 0.0) && ((md + (t * nd)) <= mDelta2))
                  tc = t;
            }
         }
         return Math.min(t0, Math.min(t1, tc));
      } else {
         final double[] d = mDelta, m = Math2.minus(sa, mEnd0), n = Math2.minus(sb, sa);
         final double md = Math2.dot(m, d), nd = Math2.dot(n, d), dd = Math2.dot(d, d);
         // Segment fully outside end caps...
         if((md < 0.0) && ((md + nd) < 0.0))
            return Double.MAX_VALUE;
         if((md > dd) && ((md + nd) > dd))
            return Double.MAX_VALUE;
         final double nn = Math2.dot(n, n), mn = Math2.dot(m, n);
         final double a = (dd * nn) - (nd * nd);
         final double k = Math2.dot(m, m) - mRadius2;
         final double c = (dd * k) - (md * md);
         if(Math.abs(a) < EPSILON)
            if(md < 0.0)
               return checkT(-mn / nn);
            else if(md > dd)
               return checkT((nd - mn) / nn);
            else
               return 0.0;
         final double b = (dd * mn) - (nd * md);
         final double disc = (b * b) - (a * c);
         if(disc < 0.0)
            return Double.MAX_VALUE;
         double t = (-b - Math.sqrt(disc)) / a; // Always a >= 0.0
         if(t < 0.0)
            t = (-b + Math.sqrt(disc)) / a;
         assert Math.abs(distanceSqr(Math2.plus(sa, Math2.multiply(t, n)), closestPointOnAxis(Math2.plus(sa, Math2.multiply(t, n))))
               - mRadius2) < (1.0e-10 * mRadius2);
         // Check end caps
         if((md + (t * nd)) < 0.0) {
            t = -md / nd;
            return (k + (2.0 * t * (mn + (t * nn)))) <= 0.0 ? checkT(t) : Double.MAX_VALUE;
         } else if((md + (t * nd)) > dd) {
            t = (dd - md) / nd;
            return (((k + dd) - (2.0 * md)) + (t * ((2.0 * (mn - nd)) + (t * nn)))) <= 0.0 ? checkT(t) : Double.MAX_VALUE;
         }
         return checkT(t);
      }
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ITransform#rotate(double[], double,
    *      double, double)
    */
   @Override
   public void rotate(double[] pivot, double phi, double theta, double psi) {
      mEnd0 = Transform3D.rotate(mEnd0, pivot, phi, theta, psi);
      mDelta = Transform3D.rotate(mDelta, phi, theta, psi);
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ITransform#translate(double[])
    */
   @Override
   public void translate(double[] distance) {
      mEnd0[0] += distance[0];
      mEnd0[1] += distance[1];
      mEnd0[2] += distance[2];
   }

   /**
    * getRadius - Returns the radius of the cylinder.
    * 
    * @return double
    */
   public double getRadius() {
      return Math.sqrt(mRadius2);
   }

   public double getLength() {
      return Math.sqrt((mDelta[0] * mDelta[0]) + (mDelta[1] * mDelta[1]) + (mDelta[2] * mDelta[2]));
   }

   /**
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      String res = "Cylinder([";
      final double[] end1 = getEnd1();
      res += Double.toString(getEnd0()[0]) + "," + Double.toString(getEnd0()[1]) + "," + Double.toString(getEnd0()[2]) + "],[";
      res += Double.toString(end1[0]) + "," + Double.toString(end1[1]) + "," + Double.toString(end1[2]) + "],";
      res += Double.toString(getRadius()) + ")";
      return res;
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.TrajectoryVRML.IRender#render(gov.nist.microanalysis.NISTMonte.TrajectoryVRML.RenderContext,
    *      java.io.Writer)
    */
   @Override
   public void render(TrajectoryVRML.RenderContext vra, Writer wr)
         throws IOException {
      final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
      nf.setMaximumFractionDigits(3);
      nf.setGroupingUsed(false);
      final Color color = vra.getCurrentColor();
      final String trStr = nf.format(vra.getTransparency());
      final String colorStr = nf.format(color.getRed() / 255.0) + " " + nf.format(color.getGreen() / 255.0) + " "
            + nf.format(color.getBlue() / 255.0);
      wr.append("\nTransform {\n");
      // r is the cross product (1,0,0) x norm(mDelta)
      {
         final double dm = Math2.magnitude(mDelta);
         assert (dm > 0.0);
         final double[] r = {
            mDelta[2] / dm,
            0.0,
            -mDelta[0] / dm,
         };
         final double rm = Math2.magnitude(r);
         if(rm > 0.0) { // if rotation required...
            nf.setMaximumFractionDigits(5);
            double th = Math.asin(rm);
            if(mDelta[1] < 0.0)
               th = Math.PI - th;
            wr.append(" rotation " + nf.format(r[0] / rm) + " " + nf.format(r[1] / rm) + " " + nf.format(r[2] / rm) + " "
                  + nf.format(th) + "\n");
            nf.setMaximumFractionDigits(3);
         }
      }
      wr.append(" translation " + nf.format((mEnd0[0] + (mDelta[0] / 2.0)) / TrajectoryVRML.SCALE) + " "
            + nf.format((mEnd0[1] + (mDelta[1] / 2.0)) / TrajectoryVRML.SCALE) + " "
            + nf.format((mEnd0[2] + (mDelta[2] / 2.0)) / TrajectoryVRML.SCALE) + "\n");
      wr.append(" children [\n");
      wr.append("  Shape {\n");
      wr.append("   geometry Cylinder {\n");
      wr.append("    radius " + nf.format(getRadius() / TrajectoryVRML.SCALE) + "\n");
      wr.append("    height " + nf.format(getLength() / TrajectoryVRML.SCALE) + "\n");
      wr.append("    bottom TRUE\n");
      wr.append("    side TRUE\n");
      wr.append("    top TRUE\n");
      wr.append("   }\n");
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
}
