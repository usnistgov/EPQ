package gov.nist.microanalysis.Utility;

import gov.nist.microanalysis.EPQLibrary.EPQException;

import java.util.List;

/**
 * <p>
 * Implements a mechanism for performing relocation on coordinates in one
 * stage's coordinate system (native) in another stage's coordinate system
 * (relocated). The coordinate systems may differ in origin (deltaX, deltaY),
 * scale (xScale, yScale) and rotation. The may also have one or other of the
 * coordinate axes reversed in direction.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas
 * @version 1.0
 */
public class StageRelocation {
   private double mX0rigin, mYOrigin;
   private double mXScale, mYScale;
   private double mRotation;
   private double mError;
   private boolean mMirrored;

   /**
    * Constructs a StageRelocation corresponding to no translation, rotation or
    * scaling.
    */
   public StageRelocation() {
      mX0rigin = 0.0;
      mYOrigin = 0.0;
      mXScale = 1.0;
      mYScale = 1.0;
      mRotation = 0.0;
      mError = 0.0;
   }

   /**
    * Constructs a StageRelocation object corresponding to the optimal
    * translation, rotation and scaling to minimize the error between the
    * RelocatedPoints.
    * 
    * @param pts The original and relocated points
    * @param mirror Should one axis be mirrored?
    * @throws EPQException
    */
   public StageRelocation(List<RelocatedPoint> pts, boolean mirror)
         throws EPQException {
      this();
      optimize(pts, mirror);
   }

   /**
    * Apply the transformation from native to relocated coordinate systems.
    * Returns the argument <code>pt</code> transformed into the translated
    * coordinate system.
    * 
    * @param pt
    * @return double[]
    */
   public double[] apply(double[] pt) {
      return new double[] {
         (((pt[0] + mX0rigin) * Math.cos(mRotation)) - ((pt[1] + mYOrigin) * Math.sin(mRotation))) * mXScale,
         (((pt[1] + mYOrigin) * Math.cos(mRotation)) + ((pt[0] + mX0rigin) * Math.sin(mRotation))) * mYScale
      };
   }

   /**
    * Apply the inverse transform to go from relocated to native coordinate
    * systems. Returns the argument <code>pt</code> transformed back to the
    * native coordinate system.
    * 
    * @param pt
    * @return double[]
    */
   public double[] inverse(double[] pt) {
      return new double[] {
         -mX0rigin + ((Math.cos(mRotation) * pt[0]) / mXScale) + ((Math.sin(mRotation) * pt[1]) / mYScale),
         (-mYOrigin - ((Math.sin(mRotation) * pt[0]) / mXScale)) + ((Math.cos(mRotation) * pt[1]) / mYScale)
      };
   }

   private double distance(double x0, double y0, double x1, double y1) {
      return Math.sqrt(Math2.sqr(x1 - x0) + Math2.sqr(y1 - y0));
   }

   static public class RelocatedPoint {
      private final double[] mPoint1; // FX, FY
      private final double[] mPoint2; // FXp, FYp

      /**
       * Constructs a RelocatedPoint
       * 
       * @param nativePt The point in the original coordinate system.
       * @param relocatedPt The same point in the translated coordinate system.
       */
      public RelocatedPoint(double[] nativePt, double[] relocatedPt) {
         mPoint1 = nativePt.clone();
         mPoint2 = relocatedPt.clone();
      }

      /**
       * Get the point in the original coordinate system.
       * 
       * @return double[]
       */
      public double[] getNativePoint() {
         return mPoint1;
      }

      /**
       * Get the coordinates of the same point in the transformed coordinate
       * system.
       * 
       * @return double[]
       */
      public double[] getRelocatedPoint() {
         return mPoint2;
      }
   };

   private void optimize(List<RelocatedPoint> pts, boolean mirror)
         throws EPQException {
      mMirrored = mirror;
      switch(pts.size()) {
         case 0:
            break;
         case 1: {
            final RelocatedPoint pt1 = pts.get(0);
            mX0rigin = pt1.mPoint2[0] - pt1.mPoint1[0];
            mYOrigin = pt1.mPoint2[1] - pt1.mPoint1[1];
         }
            break;
         default: {
            final RelocatedPoint pt1 = pts.get(0);
            final RelocatedPoint pt2 = pts.get(1);
            final double x1 = pt1.mPoint1[0];
            final double x1p = pt1.mPoint2[0];
            final double y1 = pt1.mPoint1[1];
            final double y1p = pt1.mPoint2[1];
            final double x2 = pt2.mPoint1[0];
            final double x2p = pt2.mPoint2[0];
            final double y2 = pt2.mPoint1[1];
            final double y2p = pt2.mPoint2[1];
            final double dx = x1 - x2;
            final double dy = y1 - y2;
            final double dxp = x1p - x2p;
            final double dyp = y1p - y2p;
            final double dp2 = Math2.sqr(dxp) + Math2.sqr(dyp);
            final double d2 = Math2.sqr(dx) + Math2.sqr(dy);
            if(!mirror) {
               mX0rigin = (((-Math2.sqr(x1p) * x2) + (y1p * (((x2p * y1) - (x2 * y1p) - (x2p * y2)) + (x2 * y2p))) + (x1p * (((x1 + x2) * x2p) + (-dy * y2p)))) - (x1 * ((Math2.sqr(x2p) - (y1p * y2p)) + Math2.sqr(y2p))))
                     / dp2;
               mYOrigin = -((((Math2.sqr(x2p) * y1) + (dx * x2p * y1p) + (Math2.sqr(x1p) * y2) + (dyp * y1p * y2))
                     - (dyp * y1 * y2p) - (x1p * ((x2p * (y1 + y2)) + (dx * y2p)))) / dp2);
               mXScale = (dp2 * ((dx * dxp) + (dy * dyp))) / (Math.sqrt(d2 * dp2) * Math.abs((dx * dxp) + (dy * dyp)));
               mRotation = Math.acos(Math.abs((dx * dxp) + (dy * dyp)) / Math.sqrt(d2 * dp2));
               mYScale = mXScale;
            } else {
               mX0rigin = -(((((Math2.sqr(x1p) * x2) + (((dyp * x2) + (dy * x2p)) * y1p)) - (x1p * ((x1 * x2p) + (x2 * x2p) + (dy * y2p)))) + (x1 * ((Math2.sqr(x2p) - (y1p * y2p)) + Math2.sqr(y2p)))) / dp2);
               mYOrigin = (((-(Math2.sqr(x2p) * y1) + (dx * x2p * y1p)) - (Math2.sqr(x1p) * y2) - (dyp * y1p * y2))
                     + (dyp * y1 * y2p) + (x1p * ((x2p * (y1 + y2)) - (dx * y2p))))
                     / dp2;
               mXScale = (dp2 * Math.abs((dx * dxp) - (dy * dyp))) / (Math.sqrt(d2 * dp2) * ((dx * dxp) - (dy * dyp)));
               mRotation = Math.acos(Math.abs((dx * dxp) - (dy * dyp)) / Math.sqrt(d2 * dp2));
               mYScale = -mXScale;
            }
            if(distance(x1, y1, x1p, y1p) > (0.001 * Math.abs(x2p - x1p))) {
               mXScale = -mXScale;
               mYScale = -mYScale;
               mRotation = Math.PI - mRotation;
            }
            if((distance(x1, y1, x1p, y1p) > (0.001 * Math.abs(x2p - x1p)))
                  || (distance(x2, y2, x2p, y2p) > (0.001 * Math.abs(x2p - x1p))))
               throw new EPQException("Erroneous fit in optimize procedure - Translation invalid.");

            try {
               if(pts.size() > 2) {
                  final int kX0 = 0;
                  final int kY0 = 1;
                  final int kXScale = 2;
                  final int kYScale = 3;
                  final int kRotation = 4;

                  class OptimizeFit
                     extends Simplex {

                     private final List<RelocatedPoint> mPoints;

                     OptimizeFit(List<RelocatedPoint> pts) {
                        mPoints = pts;
                     }

                     @Override
                     public double function(double[] v) {
                        final double x0 = v[kX0];
                        final double y0 = v[kY0];
                        final double xSc = v[kXScale];
                        final double ySc = v[kYScale];
                        final double rot = v[kRotation];
                        double res = 0.0;
                        for(int i = 0; i < mPoints.size(); ++i) {
                           final RelocatedPoint pt = mPoints.get(i);
                           final double xp = (((pt.mPoint1[0] + x0) * Math.cos(rot)) - ((pt.mPoint1[1] + y0) * Math.sin(rot)))
                                 * xSc;
                           final double yp = (((pt.mPoint1[1] + y0) * Math.cos(rot)) + ((pt.mPoint1[0] + x0) * Math.sin(rot)))
                                 * ySc;
                           res += Math.sqrt(Math2.sqr(xp - pt.mPoint2[0]) + Math2.sqr(yp - pt.mPoint2[1]));
                        }
                        return res;
                     }
                  }
                  ;

                  final OptimizeFit of = new OptimizeFit(pts);
                  final double[][] p = new double[kRotation + 2][kRotation + 1];
                  for(int i = kX0; i < (kRotation + 2); ++i) {
                     p[i][kX0] = mX0rigin;
                     p[i][kY0] = mYOrigin;
                     p[i][kXScale] = mXScale;
                     p[i][kYScale] = mYScale;
                     p[i][kRotation] = mRotation;
                  }
                  p[kX0 + 1][kX0] = mX0rigin + 0.1;
                  p[kY0 + 1][kY0] = mYOrigin + 0.1;
                  p[kXScale + 1][kXScale] = mXScale + 0.01;
                  p[kYScale + 1][kYScale] = mYScale + 0.01;
                  p[kRotation + 1][kRotation] = mRotation + 0.01;

                  of.setTolerance(1.0e-6);
                  of.setMaxEvaluations(100);
                  final double[] y = of.perform(p);
                  mError = of.getBestResult() / pts.size();

                  mX0rigin = y[kX0];
                  mYOrigin = y[kY0];
                  mXScale = y[kXScale];
                  mYScale = y[kYScale];
                  mRotation = y[kRotation];
               }
            }
            catch(final Exception ex) {
               throw new EPQException("Error during Simplex optimization - Using two point fit.");
            }
         }
      }
   }

   /**
    * Returns the average residual error per fit point
    * 
    * @return double
    */
   public double getResidualError() {
      return mError;
   }

   /**
    * Does this transformation involve mirroring the coordinate axes?
    * 
    * @return boolean
    */
   public boolean isMirrored() {
      return mMirrored;
   }
}