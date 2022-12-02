package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm.ZAFCorrectionAlgorithm;
import gov.nist.microanalysis.EPQLibrary.LitReference.Author;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Implements John Armstrong's bulk correction model as described in the book
 * Electron Probe Quantification (1982) and implemented in CITZAF 3.06.
 * Armstrong mixes &phi;(&rho; z) and ZAF style corrections. The absorption
 * correction is done by &phi;(&rho; z)-type calculation but the Z correction is
 * performed using a classic atomic number (R/S)-style calculation.
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
public class Armstrong1982Base extends ZAFCorrectionAlgorithm {

   static private final LitReference RefBrownJTA = new LitReference.BookChapter(LitReference.MicrobeamAnalysis, "175-180",
         new Author[]{LitReference.JArmstrong});

   protected double mGamma0;
   protected double mAlpha;
   protected double mBeta;
   protected double mQ;
   protected final boolean GREEN_BOOK = true;

   /**
    * Constructs a BrownJTA1982
    */
   protected Armstrong1982Base(String name) {
      super(name, RefBrownJTA);
   }

   @Override
   protected void initializeDefaultStrategy() {
      super.initializeDefaultStrategy();
      addDefaultAlgorithm(SurfaceIonization.class, SurfaceIonization.Love1978Citzaf);
      addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.BergerAndSeltzerCITZAF);
      addDefaultAlgorithm(BackscatterFactor.class, BackscatterFactor.Love1978);
      addDefaultAlgorithm(StoppingPower.class, StoppingPower.LoveScottCITZAF);
   }

   final protected double computeZBar(Composition comp) {
      double top = 0.0;
      double bottom = 0.0;
      for (final Element elm : comp.getElementSet()) {
         final double c_i = comp.weightFraction(elm, true);
         top += (c_i * elm.getAtomicNumber()) / elm.getAtomicWeight();
         bottom += c_i / elm.getAtomicWeight();
      }
      return top / bottom;
   }

   final protected double computeABar(Composition comp) {
      double sumC_i = 0.0;
      double sumC_iOverA_i = 0.0;
      for (final Element elm : comp.getElementSet()) {
         final double c_i = comp.weightFraction(elm, true);
         sumC_i += c_i;
         sumC_iOverA_i += c_i / elm.getAtomicWeight();
      }
      return sumC_i / sumC_iOverA_i;
   }

   @Override
   public boolean initialize(Composition comp, AtomicShell shell, SpectrumProperties props) throws EPQException {
      comp = comp.normalize();
      final boolean res = super.initialize(comp, shell, props);
      if (res) {
         final SurfaceIonization si = (SurfaceIonization) getAlgorithm(SurfaceIonization.class);
         final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
         final double e0keV = FromSI.keV(mBeamEnergy);
         final double eCkeV = FromSI.keV(mShell.getEdgeEnergy());
         final double u0 = e0keV / eCkeV;
         final double a_bar = computeABar(comp);
         final double z_bar = computeZBar(comp);
         final double logU0 = Math.log(u0);
         mGamma0 = ((5.0 * Math.PI * u0) / (logU0 * (u0 - 1.0))) * ((logU0 - 5.0) + (5.0 * Math.pow(u0, -0.2)));
         final double w = FromSI.keV(mip.computeLn(comp));
         mAlpha = ((2.97e5 * Math.pow(z_bar, 1.05)) / (a_bar * Math.pow(e0keV, 1.25))) * Math.sqrt((Math.log((1.166 * e0keV) / w) / (e0keV - eCkeV)));
         mBeta = (8.5e5 * z_bar * z_bar) / (a_bar * e0keV * e0keV * (mGamma0 - 1));
         final double phiZero = si.compute(mComposition, mShell, mBeamEnergy);
         if (GREEN_BOOK)
            mQ = (mGamma0 - phiZero) / mGamma0;
         else
            mQ = (mGamma0 - phiZero);
         // X2, X3 & X4 from CITZAF 3.06
         // X2 = 5 * 3.14159 * U / (LOG(U) * (U - 1)) * (LOG(U) - 5 + 5 * U ^
         // (-.2))
         // X4 = 297000! * Z ^ 1.05 / (A * V ^ 1.25) * (LOG(1.166 * V / W) / (V
         // - V1)) ^ .5
         // X3 = 850000! * Z * Z / (A * V * V * (X2 - 1))
      }
      return res;
   }

   @Override
   public double generated(XRayTransition xrt) {
      assert (mComposition != null);
      assert Math.abs(mTakeOffAngle - mExitAngle) < Math.toRadians(1.0) : "The take-off and exit angles should agree to within 1\u00B0";
      assert mAlpha > 0.0;
      assert mBeta > 0.0;
      final double xx = (0.5 * mBeta) / mAlpha;
      final double generated = (Math.sqrt(Math.PI) * mGamma0 * 0.5 * (1.0 - (Math.exp(xx * xx) * mQ * Math2.erfc(xx)))) / mAlpha;
      return toSI(generated);
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm.PhiRhoZAlgorithm#computeCurve(double)
    */
   final public double computeCurve(double rhoZ) {
      assert rhoZ >= 0.0;
      final double rz = FromSI.gPerCC(rhoZ) * FromSI.CM;
      return mGamma0 * Math.exp(-Math2.sqr(mAlpha * rz)) * (1.0 - (mQ * Math.exp(-mBeta * rz)));
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm#caveat(gov.nist.microanalysis.EPQLibrary.Composition,
    *      gov.nist.microanalysis.EPQLibrary.AtomicShell,
    *      gov.nist.microanalysis.EPQLibrary.SpectrumProperties)
    */
   @Override
   public String caveat(Composition comp, AtomicShell shell, SpectrumProperties props) {
      return CaveatBase.None;
   }

   final public double computeZ(Composition comp, XRayTransition xrt, SpectrumProperties props) {
      final BackscatterFactor bsc = (BackscatterFactor) getAlgorithm(BackscatterFactor.class);
      final StoppingPower sp = (StoppingPower) getAlgorithm(StoppingPower.class);
      final AtomicShell shell = xrt.getDestination();
      return bsc.compute(comp, shell, mBeamEnergy) * sp.computeInv(comp, shell, mBeamEnergy);
   }

   @Override
   final public double relativeZ(Composition comp, XRayTransition xrt, SpectrumProperties props) {
      return computeZ(comp, xrt, props) / computeZ(new Material(xrt.getElement(), ToSI.gPerCC(5.0)), xrt, props);
   }
}
