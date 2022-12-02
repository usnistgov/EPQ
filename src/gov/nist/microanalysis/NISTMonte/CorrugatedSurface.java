package gov.nist.microanalysis.NISTMonte;

import java.util.Arrays;

import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Implements the MonteCarloSS.Shape interface for a simple block shaped region.
 * The edges of the block are aligned with the coordinate axes. The extent of
 * the block is defined by two corners. The surface of the block is modeled as a
 * corrugated surface consisting of alternating surfaces of a cylinder aligned
 * along the Y axis.
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

public class CorrugatedSurface implements MonteCarloSS.Shape {
   private final double[] mCornerMin;
   private final double[] mCornerMid;
   private final double[] mCornerMax;
   private final double mDepth;

   private boolean between(double x, double b0, double b1) {
      assert (b0 <= b1);
      return (x >= b0) && (x <= b1);
   }

   private boolean between(double[] x, double[] p0, double[] p1) {
      for (int i = 0; i < x.length; ++i)
         if (!between(x[i], p0[i], p1[i]))
            return false;
      return true;
   }

   /**
    * SimpleBlock - Constructs a SimpleBlock, simple shape definition class that
    * implements the Shape interface.
    * 
    * @param corner0
    *           double[] - The x,y &amp; z coordinates of one corner
    * @param corner1
    *           double[] - The coordinates of the diagonal corner
    */
   public CorrugatedSurface(double[] corner0, double[] corner1, double depth) {
      assert (corner0.length == 3);
      assert (corner1.length == 3);
      mCornerMin = corner0.clone();
      mCornerMid = corner0.clone();
      mCornerMax = corner1.clone();
      mDepth = depth;
      // Normalize coordinates so that mCorner0[i]<=mCorner1[i]
      for (int i = 0; i < 3; ++i)
         if (mCornerMin[i] > mCornerMax[i]) {
            final double tmp = mCornerMin[i];
            mCornerMin[i] = mCornerMax[i];
            mCornerMid[i] = mCornerMax[i];
            mCornerMax[i] = tmp;
         }
      mCornerMid[2] -= mDepth;
   }

   @Override
   public boolean contains(double[] pos) {
      assert (pos.length == 3);
      final boolean b = between(pos, mCornerMid, mCornerMax);
      if ((!b) && between(pos, mCornerMin, mCornerMid)) {
         final int ii = (int) (pos[0] / mDepth);
         final double dx = pos[0] - (ii + 0.5) * mDepth;
         assert between(dx, -0.5 * mDepth, 0.5 * mDepth);
         final double dy2 = Math2.sqr(0.5 * mDepth) - dx * dx;
         assert !Double.isNaN(dy2);
         if (ii % 2 == 0) // Down
            return (dy2 >= 0.0) && between(pos[1] - mCornerMid[1], 0.0, 0.5 * mDepth - Math.sqrt(dy2));
         else // Up
            return (dy2 >= 0.0) && between(pos[1] - (mCornerMin[1] - 0.5 * mDepth), 0.0, 0.5 * mDepth - Math.sqrt(dy2));
      }
      return b;

   }

   @Override
   public double getFirstIntersection(double[] pos0, double[] pos1) {
      assert (pos0.length == 3);
      assert (pos1.length == 3);
      double t = Double.MAX_VALUE;
      for (int i = 2; i >= 0; --i) {
         final int j = (i + 1) % 3, k = (i + 2) % 3;
         if (pos1[i] != pos0[i]) {
            double u = (mCornerMin[i] - pos0[i]) / (pos1[i] - pos0[i]);
            if ((u >= 0.0) && (u <= t) && between(pos0[j] + (u * (pos1[j] - pos0[j])), mCornerMin[j], mCornerMax[j])
                  && between(pos0[k] + (u * (pos1[k] - pos0[k])), mCornerMin[k], mCornerMax[k]))
               t = u;
            // Mid of block
            u = (mCornerMid[i] - pos0[i]) / (pos1[i] - pos0[i]);
            if ((u >= 0.0) && (u <= t) && between(pos0[j] + (u * (pos1[j] - pos0[j])), mCornerMin[j], mCornerMid[j])
                  && between(pos0[k] + (u * (pos1[k] - pos0[k])), mCornerMin[k], mCornerMid[k]))
               t = u;
            // Top of block
            u = (mCornerMin[i] - pos0[i]) / (pos1[i] - pos0[i]);
            if ((u >= 0.0) && (u <= t) && between(pos0[j] + (u * (pos1[j] - pos0[j])), mCornerMin[j], mCornerMid[j])
                  && between(pos0[k] + (u * (pos1[k] - pos0[k])), mCornerMin[k], mCornerMid[k]))
               t = u;
         }
      }
      return t >= 0 ? t : Double.MAX_VALUE;
   }

   @SuppressWarnings("unused")
   private double intersectCylinder(double[] sa, double[] sb, double px) {
      final double[] p = mCornerMin.clone(), q = mCornerMin.clone();
      p[2] += 0.5 * mDepth;
      q[1] = mCornerMax[1];
      q[2] += 0.5 * mDepth;
      final double[] d = Math2.minus(q, p), m = Math2.minus(sa, p), n = Math2.minus(sb, sa);
      final double md = Math2.dot(m, d), nd = Math2.dot(n, d), dd = Math2.dot(d, d);
      if ((md < 0.0) && (md + nd < 0.0))
         return Double.NaN;
      if ((md > dd) && (md + nd > dd))
         return Double.NaN;
      final double nn = Math2.dot(n, n), mn = Math2.dot(m, n), r = 0.5 * mDepth;
      final double a = dd * nn - nd * nd, k = Math2.dot(m, m) - r * r, c = dd * k - md * md;
      if (Math.abs(a) < 1.0e-12) {
         if (c > 0)
            return Double.NaN;
         if (md < 0.0)
            return -mn / nn;
         else if (md > dd)
            return (nd - mn) / nn;
         else
            return 0.0;
      }

      return Double.NaN;
   }

   /**
    * Gets the current value assigned to one corner
    * 
    * @return Returns the coordinates of a corner.
    */
   public double[] getCorner0() {
      return mCornerMin.clone();
   }

   /**
    * Gets the current value assigned to the other corner
    * 
    * @return Returns the coordinates of a corner.
    */
   public double[] getCorner1() {
      return mCornerMax.clone();
   }

   @Override
   public String toString() {
      return "Corrugated(" + Arrays.toString(mCornerMin) + "," + Arrays.toString(mCornerMax) + "," + Double.toString(mDepth) + ")";
   }

}
