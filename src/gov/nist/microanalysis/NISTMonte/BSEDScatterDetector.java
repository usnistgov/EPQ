package gov.nist.microanalysis.NISTMonte;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.NISTMonte.MultiPlaneShape.Plane;
import gov.nist.microanalysis.Utility.Math2;

public class BSEDScatterDetector
   implements
   ActionListener {

   private final Plane mPlane;
   private final double[] mAxis0;
   private final double[] mAxis1;
   private final double mWidth;
   private final int mBinCount;
   private final double mMinEFrac;

   private int mTotal = 0;
   private double mMinE = Double.NaN;
   private final int[][] mAccumulator;

   /**
    * Constructs a BSEDScatterDetector centered at the point 'center' and in the
    * plane perpendicular to 'normal'. The direction 'axis' defines a preferred
    * orientation axis that defines the axes along which the data is binned.
    * Each bin is of width 'width' (in both directions). There are 'nBins' bins
    * on each axis.
    *
    * @param center The point around which the detector is centered.
    * @param normal The orientation of the detector
    * @param width Width of accumulator bins in meters (forced positive)
    * @param nBins The number of bins in each dimension along the plane (forced
    *           even)
    * @param eFrac Minimum fractional energy electrons to record (say 0.9
    *           records (0.9 * e0 to e0))
    */
   public BSEDScatterDetector(final double[] center, final double[] normal, final double width, final int nBins, final double eFrac) {
      mPlane = new MultiPlaneShape.Plane(normal, center);
      double[] ax2 = Math2.cross(Math2.normalize(normal), Math2.Z_AXIS);
      if(Math2.magnitude(ax2) < 1.0e-4)
         ax2 = Math2.cross(Math2.normalize(normal), Math2.X_AXIS);
      // mAxis0 and mAxis1 are in the plane of mPlane
      mAxis1 = Math2.normalize(ax2);
      mAxis0 = Math2.cross(Math2.normalize(normal), mAxis1);
      mWidth = Math.max(1.0e-9, Math.abs(width)); // force positive
      mBinCount = Math.max(2, ((nBins + 1) / 2) * 2); // force even
      mAccumulator = new int[mBinCount][mBinCount];
      mMinEFrac = eFrac;
   }

   public double[] getPrimaryAxis() {
      return mAxis0.clone();
   }

   public double[] getSecondaryAxis() {
      return mAxis1.clone();
   }

   @Override
   public void actionPerformed(final ActionEvent ae) {
      assert (ae.getSource() instanceof MonteCarloSS);
      switch(ae.getID()) {
         case MonteCarloSS.ScatterEvent:
         case MonteCarloSS.NonScatterEvent:
         case MonteCarloSS.BackscatterEvent: {
            final MonteCarloSS mcss = (MonteCarloSS) ae.getSource();
            final Electron el = mcss.getElectron();
            assert !Double.isNaN(mMinE);
            if(el.getEnergy() < mMinE)
               break;
            final double p = mPlane.getFirstIntersection(el.getPrevPosition(), el.getPosition());
            if((p >= 0) && (p < 1.0)) {
               final double[] delta = Math2.multiply(p, Math2.minus(el.getPosition(), el.getPrevPosition()));
               if(Math2.dot(delta, mPlane.getNormal()) > 0.0) {
                  final double[] pt = Math2.add(el.getPrevPosition(), delta);
                  final int[] idx = {
                     (int) (Math2.dot(pt, mAxis0) / mWidth) + (mBinCount / 2),
                     (int) (Math2.dot(pt, mAxis1) / mWidth) + (mBinCount / 2)
                  };
                  if((idx[0] >= 0) && (idx[0] < mBinCount) && (idx[1] >= 0) && (idx[1] < mBinCount))
                     mAccumulator[idx[0]][idx[1]]++;
               }
            }
            break;
         }
         case MonteCarloSS.TrajectoryStartEvent:
            if(Double.isNaN(mMinE)) {
               final MonteCarloSS mcss = (MonteCarloSS) ae.getSource();
               final Electron el = mcss.getElectron();
               mMinE = el.getEnergy() * mMinEFrac;
            }
            ++mTotal;
            break;
      }
   }

   /**
    * Gets the current value assigned to plane
    *
    * @return Returns the plane.
    */
   public Plane getPlane() {
      return mPlane;
   }

   /**
    * Gets the current value assigned to axis1
    *
    * @return Returns the axis1.
    */
   public double[] getAxis1() {
      return mAxis0.clone();
   }

   /**
    * Gets the current value assigned to axis2
    *
    * @return Returns the axis2.
    */
   public double[] getAxis2() {
      return mAxis1.clone();
   }

   /**
    * Gets the current value assigned to width
    *
    * @return Returns the width.
    */
   public double getWidth() {
      return mWidth;
   }

   /**
    * Gets the current value assigned to binCount
    *
    * @return Returns the binCount.
    */
   public int getBinCount() {
      return mBinCount;
   }

   /**
    * Gets the current value assigned to total
    *
    * @return Returns the total.
    */
   public int getTotal() {
      return mTotal;
   }

   /**
    * Gets the accumulated count for each bin within the 2-D extent of this
    * detector.
    *
    * @return int[][]
    */
   public int[][] getAccumulator() {
      return mAccumulator.clone();
   }

   /**
    * The distance along the plane of the detector from the central point for
    * the lesser edge of the bin. (Since the bins are square both directions are
    * equivalent.)
    *
    * @param bin
    * @return double
    */
   public double minVal(final int bin) {
      return (bin - (mBinCount / 2)) * mWidth;
   }

   /**
    * The distance along the plane of the detector from the central point for
    * the upper edge of the bin. (Since the bins are square both directions are
    * equivalent.)
    *
    * @param bin
    * @return double
    */
   public double maxVal(final int bin) {
      return minVal(bin + 1);
   }

   /**
    * The coordinate of the lower corner of the accumulator bin specified as an
    * argument.
    *
    * @param bin
    * @return double[3] - The 3-space coordinate of the lower corner of the
    *         specified accumulator bin.
    */
   public double[] minCoordinate(int[] bin) {
      assert bin.length == 2;
      return Math2.add(mPlane.mPoint, Math2.add(Math2.multiply((bin[0] - mBinCount / 2)
            * mWidth, mAxis0), Math2.multiply((bin[1] - mBinCount / 2) * mWidth, mAxis1)));
   }

   private boolean colIsZero(int c) {
      for(int r = 0; r < mBinCount; ++r)
         if(mAccumulator[r][c] != 0)
            return false;
      return true;
   }

   private boolean rowIsZero(int r) {
      for(int c = 0; c < mBinCount; ++c)
         if(mAccumulator[r][c] != 0)
            return false;
      return true;
   }

   /**
    * Writes out a tabulation of the non-zero grid squares from which scatter
    * events occur.
    *
    * @param wr
    * @throws IOException
    */
   public void dump(Writer wr)
         throws IOException {
      wr.write("BEGIN ======== BSED Scatter Map =========\n");
      wr.write("MIN_E       : " + FromSI.keV(mMinE) + " keV\n");
      wr.write("MIN_E_FRAC  : " + mMinEFrac + "\n");
      wr.write("AXIS0       :  " + Arrays.toString(mAxis0) + "\n");
      wr.write("AXIS1       :  " + Arrays.toString(mAxis1) + "\n");
      wr.write("NORMAL      :  " + Arrays.toString(mPlane.getNormal()) + "\n");
      wr.write("CENTER      :  " + Arrays.toString(mPlane.getPoint()) + "\n");
      wr.write("COUNT ELEC  :  " + Integer.toString(mTotal) + "\n");
      wr.write("WIDTH       :  " + Double.toString(1.0e6 * mWidth * mBinCount) + " micrometers\n");
      wr.write("================= RESULTS ================= (normalized to incident electron count)\n");
      int firstCol = mBinCount / 2, lastCol = mBinCount / 2 + 1;
      for(int c = 0; c < mBinCount / 2; ++c)
         if(!colIsZero(c)) {
            firstCol = Math.max(0, c - 1);
            break;
         }
      for(int c = mBinCount - 1; c >= mBinCount / 2; --c)
         if(!colIsZero(c)) {
            lastCol = Math.min(c + 1, mBinCount - 1);
            break;
         }
      assert firstCol < lastCol;
      wr.write("\t");
      for(int c = firstCol; c <= lastCol; ++c) {
         wr.write("\t");
         wr.write(Double.toString(minVal(c)));
      }
      wr.write("\n");
      wr.write("\t");
      for(int c = firstCol; c <= lastCol; ++c) {
         wr.write("\t");
         wr.write(Double.toString(maxVal(c)));
      }
      wr.write("\n");
      int firstRow = mBinCount / 2, lastRow = mBinCount / 2 + 1;
      for(int r = 0; r < mBinCount / 2; ++r)
         if(!rowIsZero(r)) {
            firstRow = Math.max(r - 1, 0);
            break;
         }
      for(int r = mBinCount - 1; r >= mBinCount / 2; --r)
         if(!rowIsZero(r)) {
            lastRow = Math.min(r + 1, mBinCount - 1);
            break;
         }
      assert firstRow < lastRow;
      for(int r = firstRow; r <= lastRow; ++r) {
         wr.write(Double.toString(minVal(r)));
         wr.write("\t");
         wr.write(Double.toString(maxVal(r)));
         for(int c = firstCol; c <= lastCol; ++c) {
            wr.write("\t");
            wr.write(Double.toString((double) mAccumulator[r][c] / (double) mTotal));
         }
         wr.write("\n");
      }
      wr.write("END ========== BSED Scatter Map =========\n");
   }
}