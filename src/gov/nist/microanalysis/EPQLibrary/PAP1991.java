package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Implements the PAP microanalytical correction scheme of Pouchou and Pichoir
 * as described in their chapter in the book Electron Probe Quantification.
 * </p>
 * <p>
 * Description:
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
public class PAP1991
   extends CorrectionAlgorithm.PhiRhoZAlgorithm {

   /**
    * Amplitude of the first parabola
    */
   private double mA1;
   /**
    * Amplitude of the second parabola
    */
   private double mA2;
   /**
    * Integral of the area under the parabolas
    */
   private double mF;
   /**
    * Joint point between the two parabolas
    */
   private double mRc;
   /**
    * Deepest point on the inner parabola
    */
   private double mRx;

   public double getRx() {
      return mRx;
   }

   /**
    * First parabola maximum
    */
   private double mRm;
   /**
    * Surface ionization
    */
   private double mPhi0;

   @Override
   protected void initializeDefaultStrategy() {
      super.initializeDefaultStrategy();
      addDefaultAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Pouchou1991);
      addDefaultAlgorithm(BackscatterFactor.class, BackscatterFactor.Pouchou1991);
      addDefaultAlgorithm(ProportionalIonizationCrossSection.class, ProportionalIonizationCrossSection.Pouchou86);
      addDefaultAlgorithm(SurfaceIonization.class, SurfaceIonization.Pouchou1991);
      addDefaultAlgorithm(StoppingPower.class, StoppingPower.Pouchou1991);
      addDefaultAlgorithm(ElectronRange.class, ElectronRange.Pouchou1991);
      addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.Zeller75);
   }

   public PAP1991() {
      super("PAP - Pouchou & Pichoir's Full \u03C6(\u03C1z)", LitReference.PAPinEPQ);
   }

   /**
    * The mean atomic number as defined by Pouchou and Pichoir in the PAP &amp;
    * XPP correction procedures.
    * 
    * @return double Zbar as defined by P &amp; P
    */
   static public double papMeanAtomicNumber(Composition comp) {
      double sum = 0.0;
      for(final Element elm : comp.getElementSet())
         sum += comp.weightFraction(elm, true) * Math.sqrt(elm.getAtomicNumber());
      return Math2.sqr(sum);
   }

   static public double logMeanAtomicNumber(Composition comp) {
      double sum = 0.0;
      for(final Element elm : comp.getElementSet())
         sum += comp.weightFraction(elm, true) * Math.log(elm.getAtomicNumber());
      return Math.exp(sum);
   }

   /**
    * The exponent (m) in the expression for the ionization cross section used
    * in PAP.
    * 
    * @param shell The AtomicShell for which to return the exponent
    * @return double
    */
   @Override
   public String caveat(Composition comp, AtomicShell shell, SpectrumProperties props) {
      String res = CaveatBase.None;
      double e0;
      try {
         e0 = ToSI.keV(props.getNumericProperty(SpectrumProperties.BeamEnergy));
         final BackscatterFactor bf = (BackscatterFactor) getAlgorithm(BackscatterFactor.class);
         res = CaveatBase.append(res, bf.caveat(comp, shell, e0));
         final BackscatterCoefficient bc = (BackscatterCoefficient) getAlgorithm(BackscatterCoefficient.class);
         res = CaveatBase.append(res, bc.caveat(comp, e0));
         final SurfaceIonization si = (SurfaceIonization) getAlgorithm(SurfaceIonization.class);
         res = CaveatBase.append(res, si.caveat(comp, shell, e0));
         // res=CaveatBase.append(res,mStoppingPower.
         final ElectronRange er = (ElectronRange) getAlgorithm(ElectronRange.class);
         res = CaveatBase.append(res, er.caveat(comp, e0));
         final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
         for(final XRayTransition xrt : XRayTransition.createByDestinationShell(shell))
            try {
               res = CaveatBase.append(res, mac.caveat(comp, xrt.getEnergy()));
            }
            catch(final EPQException ex) {
               res = CaveatBase.append(res, ex.toString());
            }
      }
      catch(final EPQException epq) {
         res = CaveatBase.append(res, epq.getMessage());
      }
      return CaveatBase.format(this, res);
   }

   @Override
   public boolean initialize(Composition comp, AtomicShell shell, SpectrumProperties props)
         throws EPQException {
      final boolean res = super.initialize(comp, shell, props);
      if(res) {
         final ElectronRange er = (ElectronRange) getAlgorithm(ElectronRange.class);
         final BackscatterFactor bf = (BackscatterFactor) getAlgorithm(BackscatterFactor.class);
         final SurfaceIonization si = (SurfaceIonization) getAlgorithm(SurfaceIonization.class);
         final StoppingPower sp = (StoppingPower) getAlgorithm(StoppingPower.class);
         final ProportionalIonizationCrossSection icx = (ProportionalIonizationCrossSection) getAlgorithm(ProportionalIonizationCrossSection.class);
         final double u0 = mBeamEnergy / mShell.getEdgeEnergy(); // units ok
         {
            // weight average atomic number is correct here... (see pg 60)
            final double zBar = mComposition.weightAvgAtomicNumber();
            {
               final double zBarN = logMeanAtomicNumber(mComposition);
               final double beta = 40.0 / zBar;
               final double q0 = 1.0 - (0.535 * Math.exp(-Math.pow(21.0 / zBarN, 1.2)))
                     - (2.5e-4 * Math.pow(zBarN / 20.0, 3.5));
               final double q = q0 + ((1.0 - q0) * Math.exp((1.0 - u0) / beta));
               final double d = 1.0 + (1.0 / Math.pow(u0, Math.pow(zBar, 0.45)));
               // mElectronRange is in m*(kg/m^3) -> kg/m^2
               // mRx is the deepest point on the inner parabola
               mRx = q * d * ElectronRange.toGramPerCmSqr((er.compute(mComposition, shell, mBeamEnergy)));
               assert mRx > 0 : "Deepest point must be larger than zero";
               assert mRx < 0.1 : "Deepest point is likely to be less than 1 mm";
            }
            {
               final double g1 = 0.11 + (0.41 * Math.exp(-Math.pow(zBar / 12.75, 0.75)));
               final double g2 = 1.0 - Math.exp(-Math.pow(u0 - 1.0, 0.35) / 1.19);
               final double g3 = 1.0 - Math.exp(((0.5 - u0) * Math.pow(zBar, 0.4)) / 4.0);
               // Location of the peak of the first parabola
               mRm = g1 * g2 * g3 * mRx;
               assert mRm < mRx : "The peak must be inside the deepest point!!!";
               assert mRm > 0 : "The peak should be inside the material.";
            }
         }
         /*
          * After puzzling about this for !many! hour I've come to the
          * conclusion that P&P eqn 13 is wrong. It should read F = (R/S) /
          * QlA(E0). This agrees with the XMAQNT C library and better yet seems
          * to work.
          */
         mF = (bf.compute(mComposition, mShell, mBeamEnergy)
               * StoppingPower.invToGramPerkeVcmSqr(sp.computeInv(mComposition, mShell, mBeamEnergy)))
               / icx.computeFamily(mShell, mBeamEnergy);
         assert mF > 0.0 : "The integral must be larger than zero.";
         {
            mPhi0 = si.compute(mComposition, mShell, mBeamEnergy);
            assert mPhi0 >= 1.0 : "Phi0 should be larger than 1.0";
            assert mPhi0 < 5.0 : "Phi0 should be less than 5.0";
            {
               final double tt = mF - ((mPhi0 * mRx) / 3.0);
               final double dr = mRx - mRm;
               final double d = dr * tt * ((dr * mF) - (mPhi0 * mRx * (mRm + (mRx / 3.0))));
               if(d > 0.0)
                  // Location of the joint between parabola one and two
                  mRc = 1.5 * ((tt / mPhi0) - (Math.sqrt(d) / (mPhi0 * dr)));
               else {
                  mRm = (mRx * (mF - ((mPhi0 * mRx) / 3.0))) / (mF + (mPhi0 * mRx));
                  mRc = (3.0 * mRm * (mF + (mPhi0 * mRx))) / (2.0 * mPhi0 * mRx);
                  if((mRm < 0.0) || (mRm > mRx))
                     throw new EPQException("Too small an overvoltage for this algorithm.");
               }
            
            }
            mA1 = mPhi0 / ((mRm * (mRc + mRx)) - (mRx * mRc));
         }
         mA2 = (mA1 * (mRc - mRm)) / (mRc - mRx);
      }
      return res;
   }

   @Override
   public double generated(XRayTransition xrt) {
      return toSI(mF);
   }

   @Override
   public double computeZACorrection(XRayTransition xrt)
         throws EPQException {
      final double chi = MassAbsorptionCoefficient.toCmSqrPerGram(chi(xrt));
      final double chi2 = chi * chi;
     // PAP1991's expressions (corrected!!)
     final double f1 = (mA1 / chi)
           * ((((((mRc - mRm) * (mRx - mRc - (2.0 / chi))) - (2.0 / chi2)) * Math.exp(-chi * mRc)) - ((mRc - mRm) * mRx))
                 + (mRm * (mRc - (2.0 / chi))) + (2.0 / chi2));
     final double f2 = (mA2 / chi) * (((((mRx - mRc) * (mRx - mRc - (2.0 / chi))) + (2.0 / chi2)) * Math.exp(-chi * mRc))
           - ((2.0 / chi2) * Math.exp(-chi * mRx)));
     return toSI(f1 + f2);
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm.PhiRhoZAlgorithm#computeCurve(double)
    */
   @Override
   public double computeCurve(double rhoZ) {
      if(rhoZ > 0.0) {
         rhoZ *= 0.1; // convert from kg/m^2 to g/cm^2
         if(rhoZ < mRc) {
            final double b1 = mPhi0 - (mA1 * mRm * mRm);
            return (mA1 * Math2.sqr(rhoZ - mRm)) + b1;
         } else if(rhoZ < mRx)
            return mA2 * Math2.sqr(rhoZ - mRx);
      }
      return 0.0;

   }
};
