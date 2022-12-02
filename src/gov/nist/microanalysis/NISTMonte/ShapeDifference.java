package gov.nist.microanalysis.NISTMonte;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.ITransform;

/**
 * <p>
 * Implements the MonteCarloSS.Shape interface to define a Shape that represents
 * the volume in the primary Shape that is not within the volume of the delta
 * Shape. Creates a Shape by removing one Shape from another. ShapeDifference
 * does not implement TrajectoryVRML.IRender as it is hard to imagine how to do
 * this in a reasonably simple manner.
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

public class ShapeDifference implements MonteCarloSS.Shape, ITransform {

   static final boolean DEBUG = false;

   private final MonteCarloSS.Shape mPrimary;
   private final MonteCarloSS.Shape mDelta;

   /**
    * MCSS_ShapeDifference - Create a Shape that represents the the primary
    * Shape minus any overlapping regions in the delta Shape.
    * 
    * @param primary
    *           Shape
    * @param delta
    *           Shape
    */
   public ShapeDifference(MonteCarloSS.Shape primary, MonteCarloSS.Shape delta) {
      mPrimary = primary;
      mDelta = delta;
   }

   /**
    * contains - See MonteCarloSS.contains
    * 
    * @param pos
    *           double[]
    * @return boolean
    */
   @Override
   public boolean contains(double[] pos) {
      return mPrimary.contains(pos) && (!mDelta.contains(pos));
   }

   final double recurse(double s0, double[] pos0, double[] pos1) {
      assert s0 > 0.0;
      if (s0 > 1.0)
         return Double.MAX_VALUE;
      final double s0p = s0 + MonteCarloSS.SMALL_DISP;
      final double[] nextPt = new double[]{pos0[0] + (s0p * (pos1[0] - pos0[0])), pos0[1] + (s0p * (pos1[1] - pos0[1])),
            pos0[2] + (s0p * (pos1[2] - pos0[2]))};
      final double res = s0p + getFirstIntersection(nextPt, pos1);
      return res > 1.0 ? Double.MAX_VALUE : res;
   }

   /**
    * getFirstIntersection - See MonteCarloSS.getFirstIntersection.
    * 
    * @param pos0
    *           double[]
    * @param pos1
    *           double[]
    * @return double
    */
   @Override
   public double getFirstIntersection(double[] pos0, double[] pos1) {
      final boolean in1 = mPrimary.contains(pos0);
      final boolean in2 = mDelta.contains(pos0);
      final double u1 = mPrimary.getFirstIntersection(pos0, pos1);
      final double u2 = mDelta.getFirstIntersection(pos0, pos1) + 1.0e-10;
      assert u1 > 0.0 : "u1 = " + Double.toString(u1);
      assert u2 > 0.0 : "u2 = " + Double.toString(u2);
      if (in1) {
         if (!in2) {
            // Starting inside: (inside a, outside b)
            if (DEBUG) {
               if (u1 < u2)
                  return u1; // Leaving shape: (exiting a, outside b)
               else
                  return u2; // Leaving shape: (inside a, entering b)
            } else
               return u1 < u2 ? u1 : u2;
         } else // Starting outside: (inside a, inside b)
         if (DEBUG) {
            if (u2 < u1)
               return u2; // Entering inside (inside a, exiting b)
            else
               // Exiting a: (exiting a, inside b)
               return recurse(u1, pos0, pos1);
         } else
            return u1 < u2 ? recurse(u1, pos0, pos1) : u2;
      } else if (in2) {
         // Outside a, inside b
         if (DEBUG) {
            if (u1 < u2)
               // Entering a (entering a, inside b)
               return recurse(u1, pos0, pos1);
            else
               // Exiting b (outside a, exiting b)
               return recurse(u2, pos0, pos1);
         } else
            return recurse(u1 < u2 ? u1 : u2, pos0, pos1);
      } else if (DEBUG) {
         if (u1 < u2)
            return u1; // Entering shape (entering a, outside b)
         else
            // Not entering shape (outside a, entering b)
            return recurse(u2, pos0, pos1);
      } else
         return u1 < u2 ? u1 : recurse(u2, pos0, pos1);
   }

   private void checkTransform() throws EPQFatalException {
      if (!(mPrimary instanceof ITransform))
         throw new EPQFatalException(mPrimary.toString() + " does not support transformation.");
      if (!(mDelta instanceof ITransform))
         throw new EPQFatalException(mDelta.toString() + " does not support transformation.");
   }

   // JavaDoc in ITransform
   @Override
   public void rotate(double[] pivot, double phi, double theta, double psi) {
      checkTransform();
      ((ITransform) mPrimary).rotate(pivot, phi, theta, psi);
      ((ITransform) mDelta).rotate(pivot, phi, theta, psi);
   }

   // JavaDoc in ITransform
   @Override
   public void translate(double[] distance) {
      checkTransform();
      ((ITransform) mPrimary).translate(distance);
      ((ITransform) mDelta).translate(distance);
   }

   /**
    * Gets the current value assigned to the shape to be removed from the
    * primary shape.
    * 
    * @return Returns the delta.
    */
   public MonteCarloSS.Shape getDelta() {
      return mDelta;
   }

   /**
    * Gets the current value assigned to the primary shape.
    * 
    * @return Returns the primary.
    */
   public MonteCarloSS.Shape getPrimary() {
      return mPrimary;
   }

   @Override
   public String toString() {
      final StringBuffer res = new StringBuffer("Difference[");
      res.append(mPrimary.toString());
      res.append(", ");
      res.append(mDelta.toString());
      res.append("]");
      return res.toString();
   }

}
