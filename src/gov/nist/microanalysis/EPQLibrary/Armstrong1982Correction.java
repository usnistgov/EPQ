package gov.nist.microanalysis.EPQLibrary;

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
public final class Armstrong1982Correction
   extends Armstrong1982Base {

   /**
    * Constructs a BrownJTA1982
    */
   public Armstrong1982Correction() {
      super("Armstrong CITZAF");
   }

   private double errorFunction(double erfx) {
      final double[] erf = new double[] {
         0.254829592,
         -0.284496736,
         1.421413741,
         -1.453152027,
         1.061405429
      };
      final double ERFP = 0.3275911;
      double erfs = 1.0;
      final double erft = 1 / (1 + (ERFP * erfx));
      double res = 0.0;
      for(int i = 0; i < 5; ++i) {
         erfs *= erft;
         res += erf[i] * erfs;
      }
      return res;
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm#computeZACorrection(gov.nist.microanalysis.EPQLibrary.XRayTransition)
    */
   @Override
   public double computeZACorrection(XRayTransition xrt)
         throws EPQException {
      assert (mComposition != null);
      assert Math.abs(mTakeOffAngle - mExitAngle) < Math.toRadians(1.0) : "The take-off and exit angles should agree to within 1\u00B0";
      final double chi = MassAbsorptionCoefficient.toCmSqrPerGram(chi(xrt));
      assert mAlpha > 0.0;
      assert mBeta > 0.0;
      double emitted, generated;
      final double xx = (0.5 * chi) / mAlpha;
      final double yy = (0.5 * (mBeta + chi)) / mAlpha;
      final double zz = (0.5 * mBeta) / mAlpha;
      if(GREEN_BOOK) {
         emitted = (Math.sqrt(Math.PI) * mGamma0 * 0.5 * ((Math.exp(xx * xx) * Math2.erfc(xx)) - (Math.exp(yy * yy) * mQ * Math2.erfc(yy))))
               / mAlpha;
         generated = (-Math.sqrt(Math.PI) * mGamma0 * 0.5 * (-1.0 + (Math.exp(zz * zz) * mQ * Math2.erfc(zz)))) / mAlpha;
      } else {
         emitted = (Math.sqrt(Math.PI) * ((mGamma0 * errorFunction(xx)) - (mQ * errorFunction(yy)))) / mAlpha;
         generated = (Math.sqrt(Math.PI) * (mGamma0 - (mQ * errorFunction(zz)))) / mAlpha;
      }
      // Usually I'd associate emitted with ZA however CITZAF doesn't and we
      // must adjust...
      return computeZ(mComposition, xrt, mProperties) * (emitted / generated);
   }
}
