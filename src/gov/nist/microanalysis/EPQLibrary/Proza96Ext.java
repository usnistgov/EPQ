package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Implements the extension to the Proza96 phi(rho-z) correction scheme for
 * non-normal beam incidence.
 * </p>
 * <p>
 * Ref: X-Ray Spectrom. 2001; 30: 382-387
 * </p>
 * <p>
 * Note: Energies in keV and lengths in cm are used extensively in internals and
 * the private methods associated with this class. The public methods all follow
 * the library standard of using SI units.
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
public class Proza96Ext
   extends Proza96Base {

   private static final double SQRT_PI = Math.sqrt(Math.PI);
   private static final double MIN_TILT = Math.toRadians(1.0);

   private double mTilt;
   protected double mPhi0Tilted; // The height of the phi(rho z) curve
   private double mRhoZMaxTilted; // The rho-z position of the peak of the
   private double mAlphaTilted;
   private double mBetaTilted;
   private double mFLeft;
   private double mFRight;
   private double mG;
   private double mD;
   private double mDelta;
   private double mGamma;
   private double mFe;
   private double mPhiMaxTilted;

   static private double posVal(double x) {
      return x < 0.0 ? 0.0 : x;
   }

   public Proza96Ext() {
      super("Proza96 - Bastin et al. (Non-normal)", LitReference.Proza96Extended);
   }

   /**
    * caveat - Determine any caveats that have been identified for this
    * algorithm.
    * 
    * @param comp Composition
    * @param shell AtomicShell
    * @param props
    * @return String
    */
   @Override
   public String caveat(Composition comp, AtomicShell shell, SpectrumProperties props) {
      return super.caveat(comp, shell, props);
   }

   @Override
   public boolean initialize(Composition comp, AtomicShell shell, SpectrumProperties props)
         throws EPQException {
      final boolean res = super.initialize(comp, shell, props);
      mTilt = getTilt(props);
      if(Math.abs(mTilt) > MIN_TILT) {
         assert Math.abs(mTilt) < Math.PI;
         final double cosTilt = Math.cos(mTilt);
         double fTot;
         { // Taken from PAP
            final StoppingPower sp = (StoppingPower) getAlgorithm(StoppingPower.class);
            final ProportionalIonizationCrossSection icx = (ProportionalIonizationCrossSection) getAlgorithm(ProportionalIonizationCrossSection.class);
            // Use the XPP implementation of the tilted backscatter factor...
            XPP1989Ext.TiltedBackscatterFactor.setTilt(mTilt);
            // mF = (R/S)*(1/Qj(E0))
            fTot = (cosTilt * XPP1989Ext.TiltedBackscatterFactor.compute(mComposition, mShell, mBeamEnergy) * StoppingPower.invToGramPerkeVcmSqr(sp.computeInv(mComposition, mShell, mBeamEnergy)))
                  / icx.computeFamily(mShell, mBeamEnergy);
         }

         final double meanZ = mComposition.weightAvgAtomicNumber();
         final double u0 = mBeamEnergy / shell.getEdgeEnergy();

         mBetaTilted = mBeta / Math.pow(cosTilt, 1.35);
         {
            // From mPhiZero computed in Proza96Base using [8] in PAP90 or pg383
            // in Bastin
            final double q = 1.0 + ((0.2 + (2.3 / Math.sqrt(meanZ))) * (1.0 - Math.exp(-Math.pow(u0 - 1.0, 0.3))));
            mPhi0Tilted = q * Math.pow(mPhiZero / q, Math.pow(cosTilt, 0.7));
         }
         mRhoZMaxTilted = mRhoZMax * Math2.sqr(cosTilt);
         mPhiMaxTilted = mPhi0Tilted * Math.exp(Math2.sqr(mBetaTilted * mRhoZMaxTilted));
         mFLeft = (mPhiMaxTilted * SQRT_PI * erf(mBetaTilted * mRhoZMaxTilted)) / (2.0 * mBetaTilted);
         mFRight = fTot - mFLeft;
         assert mFLeft > 0;
         assert mFRight > 0;
         mAlphaTilted = (mPhiMaxTilted * SQRT_PI) / (2.0 * mFRight);
         final double tiltDeg = Math.toDegrees(mTilt);
         mG = mPhiMaxTilted * posVal((tiltDeg - 30.0) / 60.0);
         mD = mPhiMaxTilted - mG;
         final double rt = 1.0 + (0.33 * posVal(tiltDeg - 57.0));
         mDelta = ((mG / rt) + mD) / mFRight;
         mGamma = mDelta * rt;
         mFe = tiltDeg < 62.0 ? Math.exp((-20.091702 - (0.27907911 * tiltDeg)) + (4.749195 * Math.sqrt(tiltDeg))) : 1.0;
      }
      return res;
   }

   @Override
   public double computeZACorrection(XRayTransition xrt)
         throws EPQException {
      if(Math.abs(mTilt) > MIN_TILT) {
         final double chi = MassAbsorptionCoefficient.toCmSqrPerGram(chi(xrt));
         final double chi_2betaT = chi / (2.0 * mBetaTilted);
         final double e1 = ((mPhiMaxTilted * SQRT_PI) / (2.0 * mBetaTilted))
               * Math.exp((chi_2betaT * chi_2betaT) - (chi * mRhoZMaxTilted))
               * (erf((mBetaTilted * mRhoZMaxTilted) - chi_2betaT) + erf(chi_2betaT));
         final double chi_2at = chi / (2.0 * mAlphaTilted);
         final double e2 = ((mPhiMaxTilted * SQRT_PI) / (2.0 * mAlphaTilted))
               * Math.exp(Math2.sqr(chi_2at) - (chi * mRhoZMaxTilted)) * (1.0 - erf(chi_2at));
         final double e3 = ((mG / (mGamma + chi)) + (mD / (mDelta + chi))) * Math.exp(-chi * mRhoZMaxTilted);
         return toSI((e1 + ((1.0 - mFe) * e2) + (mFe * e3)) / (Math.cos(mTilt)));
      } else
         return super.computeZACorrection(xrt);
   }

   @Override
   public double generated(XRayTransition xrt) {
      if(Math.abs(mTilt) > MIN_TILT) {
         double fi = (0.5 * mPhiMaxTilted * SQRT_PI)
               * ((erf(mBetaTilted * mRhoZMaxTilted) / mBetaTilted) + ((1.0 - mFe) / mAlphaTilted));
         fi += mFe * ((mG / mGamma) + (mD / mDelta));
         return toSI(fi / Math.cos(mTilt));
      } else
         return super.generated(xrt);
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm.PhiRhoZAlgorithm#computeCurve(double)
    */
   @Override
   public double computeCurve(double rhoZ) {
      if(rhoZ > 0.0) {
         rhoZ /= 10.0; // convert from kg/m^2 to g/cm^2
         return rhoZ < mRhoZMaxTilted ? mPhiMaxTilted * Math.exp(-Math2.sqr(mBetaTilted * (rhoZ - mRhoZMaxTilted)))
               : mPhiMaxTilted * Math.exp(-Math2.sqr(mAlphaTilted * (rhoZ - mRhoZMaxTilted)));
      } else
         return 0.0;

   }
}
