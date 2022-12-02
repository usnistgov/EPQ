package gov.nist.microanalysis.NISTMonte;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;

import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * A software implementation of an annular detector such as might be found on an
 * idealized AEM. The detector is constructed from a series of annular rings.
 * The total radius of the detector, a center point, a surface normal and the
 * ring count define the detector. The detector must be placed within the
 * chamber region but outside of the sample.
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
final public class AnnularDetector implements ActionListener {
   // Define the detector
   private final double mRadius;
   private final int mNRings;
   private final double[] mCenter;
   private final double[] mNormal;
   private double mMinEnergy;
   private double mMaxEnergy;
   // Record the results
   private final int[] mDetected;
   private int mTotal;
   // Only accept electrons coming in the opposite direction to mNormal?
   private boolean mTopOnly = true;

   // intersectingRing - Returns the index of the ring through which the
   // electron passes. Returns -1 if the electron does
   // not intersect a ring.
   private int intersectingRing(double[] p1, double[] p2) {
      assert (p1.length == 3);
      assert (p2.length == 3);
      // double den = Math2.dot(Math2.minus(p2,p1),mNormal);
      final double den = ((p2[0] - p1[0]) * mNormal[0]) + ((p2[1] - p1[1]) * mNormal[1]) + ((p2[2] - p1[2]) * mNormal[2]);
      if (mTopOnly && (den > 0))
         return -1;
      if (den != 0.0) {
         // double res = Math2.dot(Math2.minus(mCenter,p1),mNormal)/den;
         final double res = (((mCenter[0] - p1[0]) * mNormal[0]) + ((mCenter[1] - p1[1]) * mNormal[1]) + ((mCenter[2] - p1[2]) * mNormal[2])) / den;
         if ((res >= 0.0) && (res <= 1.0)) {
            // double[] pt =
            // Math2.plus(p1,Math2.multiply(res,Math2.minus(p2,p1)));
            // double dist = Math2.magnitude(Math2.minus(pt,mCenter));
            final double dist = Math.sqrt(Math2.sqr((p1[0] + (res * (p2[0] - p1[0]))) - mCenter[0])
                  + Math2.sqr((p1[1] + (res * (p2[1] - p1[1]))) - mCenter[1]) + Math2.sqr((p1[2] + (res * (p2[2] - p1[2]))) - mCenter[2]));
            return dist <= mRadius ? (int) (mNRings * (dist / mRadius)) : -1;
         }
      }
      return -1;
   }

   /**
    * AnnularDetector - Construct an annular detector of the specified total
    * radius which is subdivided into nRing equal width rings detectors.
    * 
    * @param radius
    *           double - Outer radius (in meters)
    * @param nRings
    *           int - The number of equal width rings into which the detector is
    *           subdivided.
    * @param center
    *           double[] - A point defining the center of the detector
    * @param normal
    *           double[] - A vector defining the orientation of the detector
    *           (oriented towards the incoming electrons)
    */
   public AnnularDetector(double radius, int nRings, double[] center, double[] normal) {
      assert (center.length == 3);
      mRadius = radius;
      mNRings = nRings;
      mDetected = new int[mNRings];
      mCenter = center.clone();
      mNormal = Math2.normalize(normal);
      mMinEnergy = ToSI.eV(0.0);
      mMaxEnergy = Double.MAX_VALUE;
   }

   @Override
   public void actionPerformed(ActionEvent ae) {
      assert (ae.getSource() instanceof MonteCarloSS);
      switch (ae.getID()) {
         case MonteCarloSS.ScatterEvent :
         case MonteCarloSS.NonScatterEvent :
         case MonteCarloSS.BackscatterEvent : {
            final MonteCarloSS mcss = (MonteCarloSS) ae.getSource();
            final Electron el = mcss.getElectron();
            final double[] pp = el.getPrevPosition();
            final double[] p = el.getPosition();
            final int ring = intersectingRing(pp, p);
            final double ee = el.getEnergy();
            if ((ring >= 0) && (ee >= mMinEnergy) && (ee < mMaxEnergy))
               synchronized (mDetected) {
                  ++mDetected[ring];
               }
            break;
         }
         case MonteCarloSS.TrajectoryStartEvent :
            synchronized (mDetected) {
               ++mTotal;
            }
            break;
      }
   }

   /**
    * numberOfRings - The number of rings into which the detector is divided.
    * 
    * @return int
    */
   public int numberOfRings() {
      return mNRings;
   }

   /**
    * ringWidth - The width of each ring.
    * 
    * @return double
    */
   public double ringWidth() {
      return mRadius / mNRings;
   }

   /**
    * totalElectronCount - The total number of electron trajectories.
    * 
    * @return int
    */
   public int totalElectronCount() {
      return mTotal;
   }

   /**
    * detectedElectronCount - The number of electron trajectories that were
    * detected by the specified ring.
    * 
    * @return int
    */
   public int detectedElectronCount(int ring) {
      return mDetected[ring];
   }

   /**
    * sumElectronCount - The sum of the number of electrons detected over the
    * range of rings [ring0, ring1) (inclusive of ring0, excluding ring1). This
    * method is a little redundant.
    * 
    * @param lowerRing
    *           - index of lower ring
    * @param upperRing
    *           - index of upper most ring
    * @return The sum of detectedElectronCount(i) for i from lowerRing to
    *         upperRing-1
    */
   public int sumElectronCount(int lowerRing, int upperRing) {
      assert (lowerRing < upperRing);
      int res = 0;
      for (int ring = lowerRing; ring < upperRing; ++ring)
         res += mDetected[ring];
      return res;
   }

   /**
    * innerRadius - The inner radius of the specified ring.
    * 
    * @param ring
    *           int
    * @return double
    */
   public double innerRadius(int ring) {
      return (ring * mRadius) / mNRings;
   }

   /**
    * outerRadius - The outer radius of the specified ring.
    * 
    * @param ring
    *           int
    * @return double
    */
   public double outerRadius(int ring) {
      return innerRadius(ring + 1);
   }

   /**
    * ringArea - The area of the specified ring.
    * 
    * @param ring
    *           int
    * @return double
    */
   public double ringArea(int ring) {
      return Math.PI * (Math2.sqr(outerRadius(ring)) - Math2.sqr(innerRadius(ring)));
   }

   /**
    * dump - Writes a summary of the results collected by this detector.
    * 
    * @param wr
    * @throws IOException
    */
   public void dump(Writer wr) throws IOException {
      final NumberFormat df = new HalfUpFormat("0.000E0");
      wr.append("Annular Detector - " + Integer.toString(totalElectronCount()) + "\n");
      wr.append("Index\tinner\touter\tarea\tdetected\tfraction\tN[area]\n");
      for (int i = 0; i < mNRings; ++i) {
         wr.append(Integer.toString(i) + "\t");
         wr.append(df.format(innerRadius(i)) + "\t");
         wr.append(df.format(outerRadius(i)) + "\t");
         wr.append(df.format(ringArea(i)) + "\t");
         wr.append(Integer.toString(detectedElectronCount(i)) + "\t");
         wr.append(df.format((double) detectedElectronCount(i) / (double) totalElectronCount()) + "\t");
         wr.append(df.format(detectedElectronCount(i) / ringArea(i)) + "\n");
      }
   }

   /**
    * Determines whether the detector accepts electrons only anti-parallel to
    * the normal (front of the detector) or also parallel (back of the detector)
    * 
    * @return Returns true for top only, false for both sides
    */
   public boolean isTopOnly() {
      return mTopOnly;
   }

   /**
    * Determines whether the detector accepts electrons only anti-parallel to
    * the normal (front of the detector) or also parallel (back of the detector)
    * 
    * @param topOnly
    *           true for top only (default is true)
    */
   public void setTopOnly(boolean topOnly) {
      mTopOnly = topOnly;
   }

   /**
    * Set the minimum electron energy which will be detected in Joules
    * 
    * @param ee
    *           Electron kinetic energy in eV
    */
   public void setMinEnergy(double ee) {
      mMinEnergy = Math.max(0.0, ee);
   }

   /**
    * Set the minimum electron energy which will be detected in Joules
    */
   public double getMinEnergy() {
      return mMinEnergy;
   }

   /**
    * Set the maximum electron energy which will be detected in Joules
    * 
    * @param ee
    *           Electron kinetic energy in eV
    */
   public void setMaxEnergy(double ee) {
      mMaxEnergy = Math.max(mMinEnergy + ToSI.eV(1.0), ee);
   }

   /**
    * Set the maximum electron energy which will be detected in Joules
    */
   public double getMaxEnergy() {
      return mMaxEnergy;
   }
}
