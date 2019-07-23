package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Integrator;
import gov.nist.microanalysis.Utility.Math2;

import java.text.NumberFormat;

/**
 * <p>
 * Implements John Armstrong's bulk Phi(rhoZ) model as described in the book
 * Electron Probe Quantification (1982).
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Mark Sailey
 * @author Nicholas Ritchie
 * @version 1.0
 */
public class Armstrong1982ParticleCorrection
   extends Armstrong1982Base {

   public interface Shape {

      /**
       * Returns vertical extend for integrating the phi(rho z) curve. In SI.
       * 
       * @return double in SI
       * @throws EPQException
       */
      double getThickness()
            throws EPQException;

      /**
       * The primary dimension of the shape. Defined a little different for each
       * shape.
       * 
       * @return in SI
       */
      double getDiameter();

      /**
       * Computes the ZA correction for this particle shape
       * 
       * @param rz in (g/cm^3)cm
       * @return double
       */
      double computeP(double rz);
   }

   /**
    * <p>
    * Implements a bulk normal to the electron beam.
    * </p>
    * <p>
    * Seems to work.
    * </p>
    * 
    * @author Nicholas W. M. Ritchie
    */
   public final class BulkShape
      implements Shape {
      public BulkShape() {
      }

      @Override
      public double getThickness()
            throws EPQException {
         return (1.5 * ElectronRange.KanayaAndOkayama1972.compute(mComposition, mBeamEnergy)) / ToSI.gPerCC(mRhoPC);
      }

      @Override
      public double getDiameter() {
         return 1.0;
      }

      @Override
      public String toString() {
         return "Bulk";
      }

      @Override
      public double computeP(double rhoZ) {
         assert rhoZ >= 0.0;
         return Math.exp(-mChiPC * rhoZ);
      }
   }

   // Armstrong's model #1 (Seems to work!)
   public final class RightRectangularPrism
      implements Shape {
      private boolean mSimpleH = true;
      final private double mThickness;
      final private double mDiameter;

      public RightRectangularPrism(double depth, double thickness) {
         mDiameter = depth;
         mThickness = thickness;
      }

      @Override
      public double getThickness() {
         return mThickness;
      }

      /*
       * The depth along the axis pointing towards the detector
       * @see
       * gov.nist.microanalysis.EPQLibrary.Armstrong1982ParticleCorrection.Shape
       * #getDiameter()
       */
      @Override
      public double getDiameter() {
         return mDiameter;
      }

      public void setSimple(boolean bool) {
         mSimpleH = bool;
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.00 \u00B5m");
         return "Right-rectangular-prism[Thickness=" + df.format(this.getThickness() * 1.0e6) + ", Diameter="
               + df.format(this.getDiameter() * 1.0e6) + "]";
      }

      @Override
      public double computeP(double rhoZ) {
         assert rhoZ >= 0.0;
         final double beta = beta(rhoZ); // ok
         final double chi = chi(rhoZ); // ok
         final double expNegChi = Math.exp(-chi);
         double p = 0.0;
         assert rhoZ <= mRhoD;
         if(mSimpleH) {
            // rectangle (h = 1)
            // Checked 4-Mar-2009
            if(rhoZ <= (mRhoD * Math.tan(mTakeOffAngle)))
               p = (1.0 / mRhoD) * (((mRhoD - beta) * expNegChi) + ((1.0 - expNegChi) / mAlphaPC));
            else
               p = (1.0 / mRhoD) * ((1.0 - Math.exp(-mAlphaPC * mRhoD)) / mAlphaPC);
         } else {
            // rectangle (h is complicated)
            final double invAArD = 1.0 / (mAlphaPC * mAlphaPC * mRhoD);
            // Checked 4-Mar-2009
            if(rhoZ <= (0.5 * mRhoD * Math.tan(mTakeOffAngle))) // ok
               p = (1.0 / mRhoD)
                     * ((((0.75 * mRhoD) - (0.5 * beta) - ((0.5 * beta * beta) / mRhoD) - (beta / (mAlphaPC * mRhoD))) * expNegChi)
                           + ((0.5 * (1.0 - expNegChi)) / mAlphaPC) + ((1.0 - expNegChi) * invAArD));
            else if(rhoZ <= (mRhoD * Math.tan(mTakeOffAngle)))
               p = (1.0 / mRhoD)
                     * (((((mRhoD - (1.5 * beta)) + ((0.5 * beta * beta) / mRhoD) + (beta / (mAlphaPC * mRhoD))) - (1.0 / mAlphaPC)) * expNegChi)
                           + (0.5 * ((1.0 - expNegChi) / mAlphaPC)) + (((1.0 - expNegChi) * invAArD) - (2.0 * Math.exp(-0.5
                           * mAlphaPC * mRhoD) * invAArD)));
            else
               p = (1.0 / mRhoD)
                     * (((0.5 * (1.0 - Math.exp(-mAlphaPC * mRhoD))) / mAlphaPC) + (invAArD * ((1.0 + Math.exp(-mAlphaPC
                           * mRhoD)) - (2.0 * Math.exp(-0.5 * mAlphaPC * mRhoD)))));
         }
         return p;
      }
   }

   // Armstrong's model #2 (Seems to work with Citzaf mods)
   public final class TetragonalPrism
      implements Shape {

      final private double mThickness;
      final private double mDiameter;

      public TetragonalPrism(double depth, double dep) {
         mDiameter = depth;
         mThickness = dep;
      }

      @Override
      public double getThickness() {
         return mThickness;
      }

      /*
       * The length of a diagonal
       * @see
       * gov.nist.microanalysis.EPQLibrary.Armstrong1982ParticleCorrection.Shape
       * #getDiameter()
       */
      @Override
      public double getDiameter() {
         return mDiameter;
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.00 \u00B5m");
         return "TetragonalPrism[Thickness=" + df.format(mThickness * 1.0e6) + ", Diameter=" + df.format(mDiameter * 1.0e6)
               + "]";
      }

      @Override
      public double computeP(double rhoZ) {
         assert rhoZ >= 0.0;
         final double beta = beta(rhoZ); // ok
         final double chi = chi(rhoZ); // ok
         final double expNegChi = Math.exp(-chi);
         final double lambda = lambda(rhoZ);
         // tetragonal prism
         double p = 0.0;
         assert rhoZ <= mRhoD;
         // Modified to be consistent with Citzaf (not book!)
         if(rhoZ <= (mRhoD * Math.tan(mTakeOffAngle)))
            p = (4.0 / (mRhoD * mRhoD))
                  * (((0.5 * (expNegChi - 1.0)) / (mAlphaPC * mAlphaPC)) + ((0.5 * beta) / mAlphaPC)
                        + (Math2.sqr(0.5 * lambda) * expNegChi) + (((0.5 * lambda) / mAlphaPC) * (1.0 - expNegChi)));
         else
            p = (4.0 / Math2.sqr(mRhoPC))
                  * (((Math.exp(-mAlphaPC * mRhoD) - 1.0) / Math2.sqr(mAlphaPC)) + ((0.5 * mRhoD) / mAlphaPC));
         return p;
      }
   }

   // Armstrong's model #3 (Does not compare well to Storm)
   public final class TriangularPrism
      implements Shape {

      final private double mDiameter;
      final private boolean GREEN_BOOK = false;

      public TriangularPrism(double depth) {
         mDiameter = depth;
      }

      @Override
      public double getThickness() {
         return 0.5 * mDiameter;
      }

      /*
       * The length of the base edge parallel to the beam axis.
       * @see
       * gov.nist.microanalysis.EPQLibrary.Armstrong1982ParticleCorrection.Shape
       * #getDiameter()
       */
      @Override
      public double getDiameter() {
         return mDiameter;
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.00 \u00B5m");
         return "TriangularPrism[Thickness=" + df.format(getThickness() * 1.0e6) + ", Diameter=" + df.format(mDiameter * 1.0e6)
               + "]";
      }

      @Override
      public double computeP(double rhoZ) {
         assert rhoZ >= 0.0;
         double p = 0.0;
         assert rhoZ <= mRhoD;
         if(GREEN_BOOK) {
            final double gamma = (mMuPC / Math.cos(mTakeOffAngle)) / (1.0 + Math.tan(mTakeOffAngle));
            final double zeta = (mRhoD - rhoZ) * gamma;
            final double grz = gamma * rhoZ;
            p = (0.5 * (((Math.exp(-grz) - Math.exp(-zeta)) / gamma) + (zeta * Math.exp(-grz)))) / mRhoD;
         } else {
            // triangular prism, h = 1, t = d/2
            // Taken from Citzaf
            final double egrz = Math.exp(-mGammaPC * rhoZ);
            p = (((0.5 * (egrz - Math.exp(-mGammaPC * (mRhoD - rhoZ)))) / mGammaPC) + (((0.5 * mRhoD) - rhoZ) * egrz)) / mRhoD;
         }
         return p;
      }
   }

   // Armstrong's model #4 (Seems to work!)
   public final class SquarePyramid
      implements Shape {

      final private double mDiameter;

      public SquarePyramid(double depth) {
         mDiameter = depth;
      }

      @Override
      public double getThickness() {
         return 0.5 * mDiameter;
      }

      /*
       * The length of a base edge
       * @see
       * gov.nist.microanalysis.EPQLibrary.Armstrong1982ParticleCorrection.Shape
       * #getDiameter()
       */
      @Override
      public double getDiameter() {
         return mDiameter;
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.00 \u00B5m");
         return "SquarePyramid[Thickness=" + df.format(getThickness() * 1.0e6) + ", Diameter=" + df.format(mDiameter * 1.0e6)
               + "]";
      }

      @Override
      public double computeP(double rhoZ) {
         assert rhoZ >= 0.0;
         final double zeta = zeta(rhoZ);
         final double xi = xi(rhoZ);
         // square pyramid, h=1 t = d/2
         double p = 0.0;
         assert rhoZ <= mRhoD : Double.toString(rhoZ) + ">" + Double.toString(mRhoD);
         // Checked 4-Mar-2009
         p = (1.0 / (mRhoD * mRhoD))
               * ((((zeta / mGammaPC) + (0.5 * zeta * zeta)) * Math.exp(-mGammaPC * rhoZ)) + ((Math.exp(-xi) - Math.exp(-mGammaPC
                     * rhoZ)) / (mGammaPC * mGammaPC)));
         return p;
      }
   }

   // Armstrong's model #5
   public final class VerticalCylinder
      implements Shape {

      final private double mThickness;
      final private double mDiameter;

      public VerticalCylinder(double diameter, double thickness) {
         mDiameter = diameter;
         mThickness = thickness;

      }

      @Override
      public double getThickness() {
         return mThickness;
      }

      /*
       * Diameter of the cylinder
       * @see
       * gov.nist.microanalysis.EPQLibrary.Armstrong1982ParticleCorrection.Shape
       * #getDiameter()
       */
      @Override
      public double getDiameter() {
         return mDiameter;
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.00 \u00B5m");
         return "VerticalCylinder[Thickness=" + df.format(mThickness * 1.0e6) + ", Diameter=" + df.format(mDiameter * 1.0e6)
               + "]";
      }

      @Override
      public double computeP(double rhoZ) {
         assert rhoZ >= 0.0;
         final double beta = beta(rhoZ); // ok
         final double lambda = lambda(rhoZ);
         final double chi = chi(rhoZ); // ok
         final double expNegChi = Math.exp(-chi);
         // cylinder resting on circular base, h=1 // 0<z<dtan(mTakeOffAngle)
         // double
         double p = 0.0;
         assert rhoZ <= mRhoD;
         final Integrator i = new Integrator(TOLERANCE) {
            @Override
            public double getValue(double ry) {
               assert ((mRhoD * mRhoD) - (4.0 * ry * ry)) >= 0.0 : Double.toString((mRhoD * mRhoD) - (4.0 * ry * ry));
               return (Math.exp(-mAlphaPC) * Math.sqrt((mRhoD * mRhoD) - (4.0 * ry * ry))) / mAlphaPC;
            }
         };
         if(rhoZ <= (mRhoD * Math.tan(mTakeOffAngle))) {
            // Checked 4-Mar-2009
            final double sbl = Math.sqrt(beta * lambda);
            assert Math.abs(((2.0 * sbl) / mRhoD) - (beta * sbl)) <= 1.0 : "beta = " + Double.toString(beta) + "\n"
                  + "lambda = " + Double.toString(lambda) + "\n" + "mRhoD = " + Double.toString(mRhoD);
            assert (beta * lambda) >= 0.0;
            assert !Double.isNaN(sbl);

            final double AA = (2.0 / mAlphaPC) * (((1.0 - expNegChi) * sbl) + ((0.5 * mRhoD) - sbl));
            final double BB = (sbl * Math.sqrt((0.25 * mRhoD * mRhoD) - (beta * lambda)))
                  + (0.25 * mRhoD * mRhoD * Math.asin(sbl * ((2.0 / mRhoD) - beta)));
            p = (4.0 / (Math.PI * mRhoD * mRhoD)) * ((AA + (2.0 * expNegChi * BB)) - (2.0 * i.integrate(sbl, 0.5 * mRhoD)));
         } else
            p = (8.0 / (Math.PI * mRhoD * mRhoD)) * (((0.5 * mRhoD) / mAlphaPC) - i.integrate(0.0, 0.5 * mRhoD));

         return p;
      }
   }

   // Armstrong's model #6
   public final class HorizontalCylinder
      implements Shape {

      final private double mDiameter;

      public HorizontalCylinder(double dia) {
         mDiameter = dia;
      }

      @Override
      public double getThickness() {
         return mDiameter;
      }

      /*
       * Diameter of the cylinder
       * @see
       * gov.nist.microanalysis.EPQLibrary.Armstrong1982ParticleCorrection.Shape
       * #getDiameter()
       */
      @Override
      public double getDiameter() {
         return mDiameter;
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.00 \u00B5m");
         return "HorizontalCylinder[Diameter=" + df.format(mDiameter * 1.0e6) + "]";
      }

      @Override
      public double computeP(final double rz) {
         assert rz >= 0.0;
         // cylinder resting on its side, h=1
         final double cosPsi = Math.cos(mTakeOffAngle);
         final double sinPsi = Math.sin(mTakeOffAngle);
         final Integrator i = new Integrator(TOLERANCE) {
            @Override
            public double getValue(double rx) {
               final double r = (((rz * (mRhoD - (sinPsi * ((2.0 * cosPsi * rx) + (mRhoD * sinPsi))))) - Math2.sqr(cosPsi * rz) - Math2.sqr(sinPsi
                     * rx)) + (mRhoD * cosPsi * sinPsi * rx))
                     + Math2.sqr(0.5 * mRhoD * sinPsi);
               assert r > -1.0e-20 : Double.toString(r);
               final double t = (Math.sqrt(Math.max(0.0, r)) + ((rz - (0.5 * mRhoD)) * sinPsi)) - (cosPsi * rx);
               assert t >= -1.0e-12;
               assert t < (1.000001 * mRhoD);
               return Math.exp(-mMuPC * t);
            }
         };
         final double limit = Math.sqrt(rz * (mRhoD - rz));
         return (1.0 / mRhoD) * i.integrate(-limit, limit);
      }
   }

   public class Sphere2
      implements Shape {

      private final double mDiameter;
      private final boolean mHemisphere;

      private Sphere2(double dia, boolean hemi) {
         mDiameter = dia;
         mHemisphere = hemi;
      }

      @Override
      public double getThickness()
            throws EPQException {
         return mHemisphere ? 0.5 * mDiameter : mDiameter;
      }

      @Override
      public double getDiameter() {
         return mDiameter;
      }

      @Override
      public double computeP(final double rhoz) {
         final Integrator outer = new Integrator(TOLERANCE) {
            @Override
            public double getValue(final double rhoy) {
               final Integrator inner = new Integrator(TOLERANCE) {
                  @Override
                  public double getValue(double rhox) {
                     final double tanGamma = Math.tan(mTakeOffAngle);
                     // TODO: Ask JTA how rhow should be defined!!!
                     final double rhow = Math.sqrt((0.25 * mRhoD * mRhoD) - (rhox * rhox) - (rhoy * rhoy)) - rhoz;
                     final double rhow2 = Math.sqrt((0.25 * mRhoD * mRhoD) - (rhox * rhox) - (rhoy * rhoy)) - rhoz
                           - (rhox * tanGamma);
                     final double a = 1.0 + (tanGamma * tanGamma);
                     final double b = 2.0 * rhow2 * tanGamma;
                     final double c = ((rhoy * rhoy) + (rhow2 * rhow2)) - (0.25 * mRhoD * mRhoD);
                     final double rhox2 = ((-b + Math.sqrt((b * b) - (4.0 * a * c))) / 2) * a;
                     final double rad = Math2.sqr(rhox2 - rhox) + Math2.sqr(rhow2 - rhow);
                     return (8.0 / (mRhoD * mRhoD)) * Math.exp(-mMuPC * Math.sqrt(rad));
                  }
               };
               if(mHemisphere) {
                  final double beta = Math.sqrt((0.25 * mRhoD * mRhoD) - (rhoy * rhoy) - (rhoz * rhoz));
                  assert beta >= 0.0;
                  return inner.integrate(-beta, beta);
               } else {
                  final double beta = Math.sqrt((0.25 * mRhoD * mRhoD) - (rhoy * rhoy) - (0.25 * rhoz * rhoz));
                  assert beta >= 0.0;
                  return inner.integrate(-beta, beta);
               }
            }
         };
         if(mHemisphere) {
            assert ((0.25 * mRhoD * mRhoD) - (rhoz * rhoz)) >= 0.0;
            return outer.integrate(0.0, Math.sqrt((0.25 * mRhoD * mRhoD) - (rhoz * rhoz)));
         } else {
            assert ((0.25 * mRhoD * mRhoD) - (0.25 * rhoz * rhoz)) >= 0.0;
            return outer.integrate(0, Math.sqrt((0.25 * mRhoD * mRhoD) - (0.25 * rhoz * rhoz)));
         }
      }
   }

   // Armstrong models #7 & #8
   public class Sphere
      implements Shape {

      final private double mDiameter;
      final private double mFraction;

      /**
       * Generic constructor to implement a sphere, hemisphere or any vertical
       * slice of a sphere. ie. The top <code>frac</code> of a sphere.
       * 
       * @param dia
       * @param frac
       */
      public Sphere(double dia, double frac) {
         mDiameter = dia;
         assert (frac > 0.0) && (frac <= 1.0);
         mFraction = frac;
      }

      public Sphere(double dia) {
         mDiameter = dia;
         mFraction = 1.0;
      }

      @Override
      public double getThickness() {
         return mFraction * mDiameter;
      }

      /*
       * Diameter of the sphere
       * @see
       * gov.nist.microanalysis.EPQLibrary.Armstrong1982ParticleCorrection.Shape
       * #getDiameter()
       */
      @Override
      public double getDiameter() {
         return mDiameter;
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.00");
         if(mFraction == 1.0)
            return "Sphere[Diameter=" + df.format(mDiameter * 1.0e6) + " \u00B5m]";
         else
            return "Sphere[Diameter=" + df.format(mDiameter * 1.0e6) + " \u00B5m, Upper " + df.format(mFraction) + "]";
      }

      @Override
      public double computeP(final double rz) {
         final double cosPsi = Math.cos(mTakeOffAngle);
         final double sinPsi = Math.sin(mTakeOffAngle);
         final double rR = 0.5 * mRhoD;
         assert Math.abs(rz - rR) <= rR : Double.toString(rz);
         final Integrator outer = new Integrator(TOLERANCE) {
            @Override
            public double getValue(final double ry) {
               assert Math.abs(ry) <= rR : Double.toString(ry);
               {
                  final Integrator inner = new Integrator(TOLERANCE) {
                     @Override
                     public double getValue(double rx) {
                        final double rad = (((0.25 * mRhoD * mRhoD) - (rx * rx)) * Math2.sqr(sinPsi))
                              + ((rz * (mRhoD - rz) * Math2.sqr(cosPsi)) + ((rx * ((2.0 * rz) - mRhoD) * sinPsi * cosPsi) - (ry * ry)));
                        // Due to rounding rad can go slightly negative
                        assert rad >= -1.0e-20 : Double.toString(rad);
                        final double tt = (sinPsi * ((0.5 * mRhoD) - rz)) - (cosPsi * rx);
                        // Pick the larger of the two roots
                        final double g = tt + Math.sqrt(Math.max(0.0, rad));
                        assert (g >= -1.0e-10) && (g < mRhoD) : Double.toString(g);
                        return Math.exp(-mMuPC * Math.max(0.0, g));
                     }
                  };
                  // inner.setMaxSplits(20);
                  final double rad = (rz * (mRhoD - rz)) - (ry * ry);
                  // Due to rounding rad can go slightly negative
                  assert rad > -1.0e-20 : Double.toString(rad);
                  final double beta2 = Math.sqrt(Math.max(0.0, rad));
                  final double res = beta2 > 1.0e-30 ? inner.integrate(-beta2, beta2) : 0.0;
                  assert !Double.isNaN(res);
                  return res;
                  // return inner.integrate(-beta2, beta2);
               }
            }
         };
         assert rz <= mRhoD;
         final double alpha2 = Math.sqrt(rz * (mRhoD - rz));
         // outer.setMaxSplits(50);
         final double res = outer.integrate(-alpha2, alpha2) / (0.25 * Math.PI * Math2.sqr(mRhoD));
         assert !Double.isNaN(res) : Double.toString(alpha2);
         return res;
      }
   }

   // Armstrong models #7 & #8
   public class Sphere3
      implements Shape {

      private final class SphereXIntegrate
         extends Integrator {

         final double mRy;
         final double mRz;
         final double mSinPsi;
         final double mCosPsi;

         private SphereXIntegrate(double ry, double rz) {
            super(TOLERANCE);
            mRy = ry;
            mRz = rz;
            mSinPsi = Math.sin(mTakeOffAngle);
            mCosPsi = Math.cos(mTakeOffAngle);
         }

         @Override
         public double getValue(double rx) {
            // rzp is rz coordinate of the point at rx, ry on the hemisphere a
            // distance rhoZ below the surface of the sphere.
            assert ((0.25 * mRhoD * mRhoD) - (rx * rx) - (mRy * mRy)) >= -1.0e-12;
            final double rzp = ((0.5 * mRhoD) + mRz)
                  - Math.sqrt(Math.max(0.0, (0.25 * mRhoD * mRhoD) - (rx * rx) - (mRy * mRy)));
            assert ((rx * rx) + (mRy * mRy) + Math2.sqr(rzp - (0.5 * mRhoD))) <= ((0.25 * mRhoD * mRhoD) + 1.0e-14) : Double.toString(rx)
                  + ", "
                  + Double.toString(mRy)
                  + ", "
                  + Double.toString(rzp)
                  + ", "
                  + Double.toString((rx * rx) + (mRy * mRy) + Math2.sqr(rzp - (0.5 * mRhoD)));
            assert (rx >= (-0.5 * mRhoD)) && (rx <= (0.5 * mRhoD));
            final double b = (2.0 * rx * mCosPsi) - (2.0 * (rzp - (0.5 * mRhoD)) * mSinPsi);
            final double c = (((rx * rx) + (mRy * mRy) + (rzp * rzp)) - (rzp * mRhoD));
            final double[] ts = Math2.quadraticSolver(1.0, b, c);
            final double t = ts != null ? Math.max(ts[0], ts[1]) : 0.0;
            // final double t = 0.5 * (-b + Math.sqrt(b * b - 4.0 * c));
            return t > 0.0 ? Math.exp(-mMuPC * t) : 1.0;
         }
      }

      private final class SphereYIntegral
         extends Integrator {
         private final double mRz;

         private SphereYIntegral(double rz) {
            super(TOLERANCE);
            mRz = rz;
         }

         @Override
         public double getValue(final double ry) {
            final double beta2 = (0.25 * mRhoD * mRhoD) - (0.25 * mRz * mRz) - (ry * ry);
            if(beta2 > 0.0) {
               final double beta = Math.min(Math.sqrt(beta2), 0.5 * mRhoD);
               final double res = (new SphereXIntegrate(ry, mRz)).integrate(-beta, beta);
               assert !Double.isNaN(res);
               return res;
            } else
               return 0.0;
         }
      }

      final private double mDiameter;
      final private double mFraction;

      /**
       * Generic constructor to implement a sphere, hemisphere or any vertical
       * slice of a sphere. ie. The top <code>frac</code> of a sphere.
       * 
       * @param dia
       * @param frac
       */
      public Sphere3(double dia, double frac) {
         mDiameter = dia;
         assert (frac > 0.0) && (frac <= 1.0);
         mFraction = frac;
      }

      public Sphere3(double dia) {
         mDiameter = dia;
         mFraction = 1.0;
      }

      @Override
      public double getThickness() {
         return mFraction * mDiameter;
      }

      /*
       * Diameter of the sphere
       * @see
       * gov.nist.microanalysis.EPQLibrary.Armstrong1982ParticleCorrection.Shape
       * #getDiameter()
       */
      @Override
      public double getDiameter() {
         return mDiameter;
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.00");
         if(mFraction == 1.0)
            return "Sphere[Diameter=" + df.format(mDiameter * 1.0e6) + " \u00B5m]";
         else
            return "Sphere[Diameter=" + df.format(mDiameter * 1.0e6) + " \u00B5m, Upper " + df.format(mFraction) + "]";
      }

      @Override
      public double computeP(final double rz) {
         assert (rz >= 0.0) && (rz <= mRhoD);
         final double alpha2 = (0.25 * mRhoD * mRhoD) - (0.25 * rz * rz);
         if(alpha2 > 0.0) {
            final double alpha = Math.min(Math.sqrt(alpha2), 0.5 * mRhoD);
            final double res = (new SphereYIntegral(rz)).integrate(-alpha, alpha) / (0.25 * Math.PI * mRhoD * mRhoD);
            if(Double.isNaN(res))
               assert !Double.isNaN(res) : Double.toString(alpha) + ", "
                     + Double.toString((new SphereYIntegral(rz)).integrate(-alpha, alpha));
            return res;
         } else
            return 0.0;
      }
   }

   /**
    * Demonstrates that it is possible to perform the direct numerical
    * integration and get the same result.
    * 
    * @author nritchie
    */
   public class IntegratedRRP
      implements Shape {

      private final double mDiameter;
      private final double mThickness;

      public IntegratedRRP(double dia, double thick) {
         mDiameter = dia;
         mThickness = thick;
      }

      @Override
      public double computeP(final double rz) {
         final double cosPsi = Math.cos(mTakeOffAngle);
         final double sinPsi = Math.sin(mTakeOffAngle);
         final Integrator inner = new Integrator(TOLERANCE) {
            @Override
            public double getValue(double rx) {
               double t;
               if(rx < ((0.5 * mRhoD) - ((rz * cosPsi) / sinPsi)))
                  t = rz / sinPsi;
               else
                  t = ((0.5 * mRhoD) - rx) / cosPsi;
               assert t >= 0.0 : Double.toString(t);
               assert t <= (mRhoD / cosPsi) : Double.toString(t);
               return Math.exp(-mMuPC * t);
            }
         };
         return inner.integrate(-0.5 * mRhoD, 0.5 * mRhoD) / mRhoD;
      }

      @Override
      public double getDiameter() {
         return mDiameter;
      }

      @Override
      public double getThickness()
            throws EPQException {
         return mThickness;
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.00 \u00B5m");
         return "IntegratedRRP[Thickness=" + df.format(mThickness * 1.0e6) + ", Diameter=" + df.format(mDiameter * 1.0e6) + "]";
      }
   };

   // Armstrong's model #5 (Using a numerical integral)
   public final class IntegratedCylinder
      implements Shape {

      final private double mThickness;
      final private double mDiameter;

      public IntegratedCylinder(double diameter, double thickness) {
         mDiameter = diameter;
         mThickness = thickness;

      }

      @Override
      public double getThickness() {
         return mThickness;
      }

      /*
       * Diameter of the cylinder
       * @see
       * gov.nist.microanalysis.EPQLibrary.Armstrong1982ParticleCorrection.Shape
       * #getDiameter()
       */
      @Override
      public double getDiameter() {
         return mDiameter;
      }

      @Override
      public String toString() {
         final NumberFormat df = new HalfUpFormat("0.00 \u00B5m");
         return "VerticalCylinder[Thickness=" + df.format(mThickness * 1.0e6) + ", Diameter=" + df.format(mDiameter * 1.0e6)
               + "]";
      }

      @Override
      public double computeP(final double rz) {
         final double cosPsi = Math.cos(mTakeOffAngle);
         final double sinPsi = Math.sin(mTakeOffAngle);
         final Integrator outer = new Integrator(TOLERANCE) {
            @Override
            public double getValue(final double ry) {
               final double limit = Math.sqrt((0.25 * mRhoD * mRhoD) - (ry * ry));
               final Integrator inner = new Integrator(TOLERANCE) {
                  @Override
                  public double getValue(final double rx) {
                     double t = (limit - rx) / cosPsi;
                     if(rz < (t * sinPsi))
                        t = rz / sinPsi;
                     return Math.exp(-mMuPC * t);
                  }
               };
               return inner.integrate(-limit, limit);
            }
         };
         return outer.integrate(-0.5 * mRhoD, 0.5 * mRhoD) / (0.25 * Math.PI * mRhoD * mRhoD);
      }
   }

   /**
    * Density in g/cm^3
    */
   private double mRhoPC;
   /**
    * Absorption coefficient in cm^2/g
    */
   private double mMuPC;
   /**
    * Angle compensated absorption coefficient in cm^2/g
    */
   private double mChiPC;
   /**
    * Rho D is density times thickness
    */
   private transient double mRhoD;
   /**
    * alpha from pg 273
    */
   private transient double mAlphaPC;
   /**
    * gamma from pg 273
    */
   private transient double mGammaPC;

   /**
    * The object defining the shape and associated particle correction
    */
   private Shape mShape;
   /**
    * The algorithm for computing bulk corrections if USE_ANALYTICAL_BULK is
    * true.
    */
   private final Armstrong1982Correction mBulkCorrection = new Armstrong1982Correction();;

   private static final boolean USE_ANALYTICAL_BULK = true;
   private static final double TOLERANCE = 1.0e-6;

   public Armstrong1982ParticleCorrection() {
      super("Armstrong CITZAF - Particle");
   }

   @Override
   public boolean initialize(Composition comp, AtomicShell shell, SpectrumProperties props)
         throws EPQException {
      final boolean res = super.initialize(comp, shell, props);
      mBulkCorrection.initialize(comp, shell, props);
      final SampleShape sh = props.getSampleShapeWithDefault(SpectrumProperties.SampleShape, null);
      if((sh == null) || (sh instanceof SampleShape.Bulk))
         mShape = USE_ANALYTICAL_BULK ? null : new BulkShape();
      else if(sh instanceof SampleShape.RightRectangularPrism) {
         final SampleShape.RightRectangularPrism s = (SampleShape.RightRectangularPrism) sh;
         mShape = new RightRectangularPrism(s.getDepth(), s.getHeight());
         // mShape = new IntegratedRRP(s.getDepth(),s.getHeight());
      } else if(sh instanceof SampleShape.TetragonalPrism) {
         final SampleShape.TetragonalPrism tp = (SampleShape.TetragonalPrism) sh;
         mShape = new TetragonalPrism(tp.getDiagonal(), tp.getHeight());
      } else if(sh instanceof SampleShape.TriangularPrism) {
         final SampleShape.TriangularPrism tp = (SampleShape.TriangularPrism) sh;
         mShape = new TriangularPrism(2.0 * tp.getHeight());
      } else if(sh instanceof SampleShape.Cylinder) {
         final SampleShape.Cylinder c = (SampleShape.Cylinder) sh;
         // mShape = new VerticalCylinder(2.0 * c.getRadius(), c.getHeight());
         mShape = new IntegratedCylinder(2.0 * c.getRadius(), c.getHeight());
      } else if(sh instanceof SampleShape.Hemisphere) {
         final SampleShape.Hemisphere h = (SampleShape.Hemisphere) sh;
         mShape = new Sphere2(2.0 * h.getRadius(), true);
      } else if(sh instanceof SampleShape.Sphere) {
         final SampleShape.Sphere s = (SampleShape.Sphere) sh;
         // mShape = new Sphere(2.0 * s.getRadius(), 1.0);
         mShape = new Sphere3(2.0 * s.getRadius());
      } else if(sh instanceof SampleShape.SquarePyramid) {
         final SampleShape.SquarePyramid sq = (SampleShape.SquarePyramid) sh;
         mShape = new SquarePyramid(sq.getBaseLength());
      } else if(sh instanceof SampleShape.Fiber) {
         final SampleShape.Fiber f = (SampleShape.Fiber) sh;
         mShape = new HorizontalCylinder(f.getRadius());
      } else
         throw new EPQException(getName() + " does not support the " + sh.getName() + " shape.");
      double rho = ToSI.gPerCC(mProperties.getNumericWithDefault(SpectrumProperties.SpecimenDensity, Double.NaN));
      if((mShape != null) && Double.isNaN(rho))
         if(mShape instanceof BulkShape)
            rho = ToSI.gPerCC(1.0);
         else
            throw new EPQException("Please specify a specimen density.  The particle correction algorithm requires a density.");
      mRhoPC = FromSI.gPerCC(rho);
      return res;
   }

   public class ParticleCorrection
      extends Integrator {

      private final Shape mShape;

      public ParticleCorrection(Shape s)
            throws EPQException {
         super(TOLERANCE);
         mShape = s;
      }

      /**
       * rhoZ is in (g/cm3)cm ~ (1.0 g/cm^3)(1e-4 cm)=1.0e-4 g/cm^2
       * 
       * @param rhoZ
       * @return The correction factor
       * @see gov.nist.microanalysis.Utility.Integrator#getValue(double)
       */
      @Override
      public double getValue(double rhoZ) {
         // See p273 (29) - This is the numerator
         return mShape.computeP(rhoZ) * computeCurve(ToSI.gPerCC(rhoZ) * ToSI.cm(1.0));
      }
   }

   /**
    * Returns true if the CorrectionAlgorithm supports the specified SampleShape
    * derived class.
    * 
    * @param ss
    * @return true if the specified SampleShape devived class is supported;
    *         false otherwise
    */
   @Override
   public boolean supports(Class<? extends SampleShape> ss) {
      return (ss == SampleShape.Bulk.class) || (ss == SampleShape.SquarePyramid.class) || (ss == SampleShape.Sphere.class)
            || (ss == SampleShape.Cylinder.class) || (ss == SampleShape.Fiber.class) || (ss == SampleShape.Hemisphere.class)
            || (ss == SampleShape.RightRectangularPrism.class) || (ss == SampleShape.TetragonalPrism.class)
            || (ss == SampleShape.TriangularPrism.class);
   }

   public double particleAbsorptionCorrection(XRayTransition xrt)
         throws EPQException {
      final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
      mMuPC = MassAbsorptionCoefficient.toCmSqrPerGram(mac.compute(mComposition, xrt));
      mChiPC = mMuPC / Math.sin(mTakeOffAngle);
      mRhoD = mRhoPC * FromSI.cm(mShape.getDiameter()); // ok
      mAlphaPC = mMuPC / Math.cos(mTakeOffAngle); // ok
      mGammaPC = mAlphaPC / (1.0 + Math.tan(mTakeOffAngle)); // ok
      final double maxThickness = FromSI.cm((1.5 * ElectronRange.KanayaAndOkayama1972.compute(mComposition, mBeamEnergy))
            / ToSI.gPerCC(mRhoPC));
      // See p 273 (29)
      final double emitted = (new ParticleCorrection(mShape)).integrate(0.0, Math.min(mRhoD, maxThickness));
      return toSI(emitted) / generated(xrt);
   }

   @Override
   public double computeZACorrection(XRayTransition xrt)
         throws EPQException {
      if(mShape != null) {
         final double pc = particleAbsorptionCorrection(xrt);
         return computeZ(mComposition, xrt, mProperties) * (pc != 0.0 ? pc : 1.0);
      } else
         return mBulkCorrection.computeZACorrection(xrt);
   }

   /**
    * beta on pg 273
    * 
    * @param rhoZ
    * @return double
    */
   private double beta(double rhoZ) {
      return rhoZ / Math.tan(mTakeOffAngle);
   }

   /**
    * chi on pg 273
    * 
    * @param rhoZ
    * @return double
    */
   private double chi(double rhoZ) {
      return (mMuPC * rhoZ) / Math.sin(mTakeOffAngle);
   }

   /**
    * lambda on pg 273
    * 
    * @param rhoZ
    * @return double
    */
   private double lambda(final double rhoZ) {
      return mRhoD - beta(rhoZ);
   }

   /**
    * zeta on pg 273
    * 
    * @param rhoZ
    * @return double
    */
   private double zeta(double rhoZ) {
      return mRhoD - (2.0 * rhoZ);
   }

   /**
    * xi on pg 273
    * 
    * @param rhoZ
    * @return double
    */
   private double xi(double rhoZ) {
      return (mRhoD - rhoZ) * mGammaPC;
   }
}
