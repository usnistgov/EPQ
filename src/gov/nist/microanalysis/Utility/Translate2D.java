package gov.nist.microanalysis.Utility;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.Iterator;

/**
 * <p>
 * Translate from one planar (2D) coordinate system into another. The coordinate
 * systems may differ in offset, scale and rotation. (2 + 2 + 1 = 5
 * degrees-of-freedom). The two coordinate systems are calibrated relative to
 * each other using a set of points which are located in each coordinate system.
 * One point is sufficient to calibrate the offset; two points the offset, scale
 * and rotation in which the scale is assumed to be equal in both dimensions.
 * Three or more points overspecify the solution for the full 5
 * degree-of-freedom problem and the algorithm attempts to minimize the mean
 * square error using a Simplex algorithm.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nritchie
 * @version 1.0
 */
public class Translate2D {

   private static final double SCALE = 1.0e-3; // mm

   static final public class CalibrationPoint {
      final private double[] mOldPoint;
      final private double[] mNewPoint;

      CalibrationPoint(double[] orig, double[] newPt) {
         mOldPoint = orig.clone();
         mNewPoint = newPt.clone();
      }

      public double getX0() {
         return mOldPoint[0];
      }

      public double getY0() {
         return mOldPoint[1];
      }

      public double getX1() {
         return mNewPoint[0];
      }

      public double getY1() {
         return mNewPoint[1];
      }

      public boolean different(CalibrationPoint cp) {
         final double d = Math.max(Math2.distance(mOldPoint, cp.mOldPoint), Math2.distance(mNewPoint, cp.mNewPoint));
         return d > (1.0e-3 * Translate2D.SCALE);
      }

   }

   static public CalibrationPoint createCalibrationPoint(double[] oldCoord, double[] newCoord) {
      return new CalibrationPoint(oldCoord, newCoord);
   }

   static public CalibrationPoint createCalibrationPoint(double oldX, double oldY, double newX, double newY) {
      return new CalibrationPoint(new double[] { oldX, oldY }, new double[] { newX, newY });
   }

   // Transformation parameters
   private double[] mOffset        = new double[2];
   private double[] mScale         = new double[2];
   private double   mRotation;                     // radians
   private boolean  mXAxisInverted = false;

   public double calibrate(Collection<CalibrationPoint> calPts) {
      final Iterator<CalibrationPoint> calIt = calPts.iterator();
      reset();
      if (calIt.hasNext()) {
         final CalibrationPoint pt1 = calIt.next();
         if (calIt.hasNext()) {
            final CalibrationPoint pt2 = calIt.next();
            final double x1 = pt1.mOldPoint[0];
            final double x1p = pt1.mNewPoint[0];
            final double y1 = pt1.mOldPoint[1];
            final double y1p = pt1.mNewPoint[1];
            final double x2 = pt2.mOldPoint[0];
            final double x2p = pt2.mNewPoint[0];
            final double y2 = pt2.mOldPoint[1];
            final double y2p = pt2.mNewPoint[1];
            final double dx = x1 - x2;
            final double dy = y1 - y2;
            final double dxp = x1p - x2p;
            final double dyp = y1p - y2p;
            final double dp2 = Math2.sqr(dxp) + Math2.sqr(dyp);
            final double d2 = Math2.sqr(dx) + Math2.sqr(dy);
            int bestI = -1;
            double bestErr = Double.MAX_VALUE;
            for (int i = 0; i < 5; ++i) {
               final int ii = (i == 4 ? bestI : i);
               final boolean axisInverted = ((ii % 2) == 0);
               final boolean invert = (ii < 2);
               mXAxisInverted = invert;
               if (!axisInverted) {
                  mOffset[0] = -(((((Math2.sqr(x1p) * x2) + (((dyp * x2) + (dy * x2p)) * y1p))
                        - (x1p * ((x1 * x2p) + (x2 * x2p) + (dy * y2p))))
                        + (x1 * ((Math2.sqr(x2p) - (y1p * y2p)) + Math2.sqr(y2p)))) / dp2);
                  mOffset[1] = (((-(Math2.sqr(x2p) * y1) + (dx * x2p * y1p)) - (Math2.sqr(x1p) * y2) - (dyp * y1p * y2))
                        + (dyp * y1 * y2p) + (x1p * ((x2p * (y1 + y2)) - (dx * y2p)))) / dp2;
                  final double ma = Math.abs((dx * dxp) - (dy * dyp));
                  mScale[0] = ma > 0.0 ? (dp2 * ma) / (Math.sqrt(d2 * dp2) * ((dx * dxp) - (dy * dyp))) : 1.0;
                  mRotation = Math.acos(Math.abs((dx * dxp) - (dy * dyp)) / Math.sqrt(d2 * dp2));
                  mScale[1] = mScale[0];
               } else {
                  mOffset[0] = (((-Math2.sqr(x1p) * x2) + (y1p * (((x2p * y1) - (x2 * y1p) - (x2p * y2)) + (x2 * y2p)))
                        + (x1p * (((x1 + x2) * x2p) + (-dy * y2p))))
                        - (x1 * ((Math2.sqr(x2p) - (y1p * y2p)) + Math2.sqr(y2p)))) / dp2;
                  mOffset[1] = -((((Math2.sqr(x2p) * y1) + (dx * x2p * y1p) + (Math2.sqr(x1p) * y2) + (dyp * y1p * y2))
                        - (dyp * y1 * y2p) - (x1p * ((x2p * (y1 + y2)) + (dx * y2p)))) / dp2);
                  final double ma = Math.abs((dx * dxp) + (dy * dyp));
                  mScale[0] = ma > 0.0 ? (dp2 * ((dx * dxp) + (dy * dyp))) / (Math.sqrt(d2 * dp2) * ma) : 1.0;
                  mRotation = Math.acos(Math.abs((dx * dxp) + (dy * dyp)) / Math.sqrt(d2 * dp2));
                  mScale[1] = mScale[0];
               }
               if (invert) {
                  mScale[0] = -mScale[0];
                  mScale[1] = mScale[1];
               }
               if (i != 4) {
                  final double err = error(calPts);
                  if (err < bestErr) {
                     bestI = i;
                     bestErr = err;
                  }
               }
            }
         } else {
            mOffset[0] = pt1.mNewPoint[0] - pt1.mOldPoint[0];
            mOffset[1] = pt1.mNewPoint[1] - pt1.mOldPoint[1];
         }
      }
      if (calIt.hasNext()) {
         class RefineCalibration extends Simplex {
            private final Collection<CalibrationPoint> calPts;

            RefineCalibration(Collection<CalibrationPoint> pts) {
               calPts = pts;
            }

            @Override
            public double function(double[] x) {
               double sumSqr = 0.0;
               final double scX = x[0], scY = x[1], offX = x[2], offY = x[3], rot = x[4];
               for (final CalibrationPoint cp : calPts) {
                  double resX, resY;
                  resX = (((cp.mOldPoint[0] + offX) * Math.cos(rot)) - ((cp.mOldPoint[1] + offY) * Math.sin(rot)))
                        * scX;
                  resY = (((cp.mOldPoint[1] + offY) * Math.cos(rot)) + ((cp.mOldPoint[0] + offX) * Math.sin(rot)))
                        * scY;
                  sumSqr += Math2.sqr(resX - cp.mNewPoint[0]) + Math2.sqr(resY - cp.mNewPoint[1]);
               }
               return sumSqr;
            }
         }
         final RefineCalibration rc = new RefineCalibration(calPts);
         double scOff = 0.01;
         for (final CalibrationPoint cp : calPts)
            scOff = Math.max(Math.max(scOff, Math.abs(cp.mOldPoint[0] - cp.mNewPoint[0]) / 100.0),
                  Math.abs(cp.mOldPoint[1] - cp.mNewPoint[1]) / 100.0);
         final double[] center = new double[] { mScale[0], mScale[1], mOffset[0], mOffset[1], mRotation };
         final double[] scale = new double[] { Math.abs(mScale[0]) / 10.0, Math.abs(mScale[1]) / 10.0, scOff, scOff,
               0.01 };
         double[] best;
         try {
            best = rc.perform(Simplex.randomizedStartingPoints(center, scale));
            mScale[0] = best[0];
            mScale[1] = best[1];
            mOffset[0] = best[2];
            mOffset[1] = best[3];
            mRotation = best[4] - Math.round((best[4] / (2.0 * Math.PI))) * (2.0 * Math.PI);
         } catch (final UtilException e) {
            // Can't refine it...
         }
      }
      return error(calPts);
   }

   /**
    * Compute the distance between the original and translated new points
    * 
    * @param calPts
    * @return double
    */
   public double error(Collection<CalibrationPoint> calPts) {
      // Compute the residual error
      double sumSqr = 0.0;
      for (final CalibrationPoint calib : calPts) {
         final double[] newPt = compute(calib.mOldPoint);
         sumSqr += Math2.sqr(newPt[0] - calib.mNewPoint[0]) + Math2.sqr(newPt[1] - calib.mNewPoint[1]);
      }
      return Math.sqrt(sumSqr / calPts.size());

   }

   // Reset to the null-translation state
   private void reset() {
      mOffset[0] = 0.0;
      mOffset[1] = 0.0;
      mScale[0] = 1.0;
      mScale[1] = 1.0;
      mRotation = 0.0;
      mXAxisInverted = false;
   }

   public Translate2D(Translate2D t2d) {
      mOffset[0] = t2d.mOffset[0];
      mOffset[1] = t2d.mOffset[1];
      mScale[0] = t2d.mScale[0];
      mScale[1] = t2d.mScale[1];
      mRotation = t2d.mRotation;
      mXAxisInverted = t2d.mXAxisInverted;
   }

   /**
    * Constructs a Translate2D object representing the null-translation.
    */
   public Translate2D() {
      reset();
   }

   /**
    * Constructs a Translate2D object representing the null-translation.
    */
   public Translate2D(double[] offset, double rotation) {
      reset();
      mOffset = offset.clone();
      mRotation = rotation;
   }

   /**
    * Constructs a Translate2D object representing the specified translation
    * 
    * @param offset
    * @param scale
    * @param rotation
    * @param invertXAxis
    */
   public Translate2D(double[] offset, double[] scale, double rotation, boolean invertXAxis) {
      mOffset = offset.clone();
      mScale = scale.clone();
      mRotation = rotation;
      mXAxisInverted = invertXAxis;
   }

   /**
    * Given a coordinate in the original coordinate system compute the
    * equivalent coordinate in the new coordinate system.
    * 
    * @param oldCoord
    * @return double[2]
    */
   public double[] compute(double[] oldCoord) {
      final double[] res = new double[2];
      res[0] = (((oldCoord[0] + mOffset[0]) * Math.cos(mRotation)) - ((oldCoord[1] + mOffset[1]) * Math.sin(mRotation)))
            * mScale[0];
      res[1] = (((oldCoord[1] + mOffset[1]) * Math.cos(mRotation)) + ((oldCoord[0] + mOffset[0]) * Math.sin(mRotation)))
            * mScale[1];
      return res;
   }

   /**
    * Given a coordinate in the new coordinate system compute the equivalent
    * coordinate in the old coordinate system.
    * 
    * @param newCoord
    * @return double[2]
    */
   public double[] inverse(double[] newCoord) {
      final double[] res = new double[2];
      res[0] = -mOffset[0] + ((Math.cos(mRotation) * newCoord[0]) / mScale[0])
            + ((Math.sin(mRotation) * newCoord[1]) / mScale[1]);
      res[1] = (-mOffset[1] - ((Math.sin(mRotation) * newCoord[0]) / mScale[0]))
            + ((Math.cos(mRotation) * newCoord[1]) / mScale[1]);
      return res;
   }

   /**
    * Gets the current value assigned to xAxisInverted
    * 
    * @return Returns the xAxisInverted.
    */
   public boolean isXAxisInverted() {
      return mXAxisInverted;
   }

   @Override
   public String toString() {
      final NumberFormat nf = new HalfUpFormat("0.00");
      final StringBuffer sb = new StringBuffer();
      sb.append("Translation[Scale=[");
      sb.append(nf.format(mScale[0]));
      sb.append(",");
      sb.append(nf.format(mScale[1]));
      sb.append("],Offset=[");
      sb.append(nf.format(mOffset[0]));
      sb.append(",");
      sb.append(nf.format(mOffset[1]));
      sb.append("],Rotation=");
      sb.append(nf.format(Math.toDegrees(mRotation)));
      sb.append("]]");
      return sb.toString();
   }

   public double getXScale() {
      return mScale[0];
   }

   public double getYScale() {
      return mScale[1];
   }

   public double getXOffset() {
      return mOffset[0];
   }

   public double getYOffset() {
      return mOffset[1];
   }

   public double getRotation() {
      return mRotation;
   }
}
