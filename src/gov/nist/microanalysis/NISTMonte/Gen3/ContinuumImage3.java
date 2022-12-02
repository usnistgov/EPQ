package gov.nist.microanalysis.NISTMonte.Gen3;

import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.Gen3.BaseXRayGeneration3.XRay;

/**
 * <p>
 * The ContinuumImage3 class can be used to compare the positional dependence of
 * the continuum generation with the position dependence of characteristic
 * radiation. (Actually, it only checks the x-ray energy so it could be used to
 * map characteristic radiation also. It really depends upon which generation
 * mechanism the listener is attached.)
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
final public class ContinuumImage3 extends EmissionImageBase {

   private final double mMinE;
   private final double mMaxE;

   public ContinuumImage3(int width, int height, double eMin, double eMax) {
      super(width, height);
      mMinE = Math.min(eMin, eMax);
      mMaxE = Math.max(eMin, eMax);
   }

   @Override
   protected String getTitle() {
      final NumberFormat nf = new DecimalFormat("0.000 keV");
      return "[" + nf.format(FromSI.keV(mMinE)) + "," + nf.format(FromSI.keV(mMaxE)) + "]";
   }

   @Override
   public void actionPerformed(ActionEvent e) {
      final XRayTransport3 xrg = (XRayTransport3) e.getSource();
      switch (e.getID()) {
         case MonteCarloSS.TrajectoryEndEvent :
            ++mTrajectoryCount;
            break;
         case BaseXRayGeneration3.XRayGeneration : {
            if (mTrajectoryCount < mMaxTrajectories) {
               for (int i = xrg.getEventCount() - 1; i >= 0; --i) {
                  final XRay xr = xrg.getXRay(i);
                  final double exr = xr.getEnergy();
                  if ((exr >= mMinE) && (exr <= mMaxE)) {
                     final double ii = mEmission ? xr.getIntensity() : xr.getGenerated();
                     final double[] pos = xr.getGenerationPos();
                     setPixel(pos[0], pos[2], ii);
                  }
               }
               resetImage();
            }
            break;
         }
      }
   }

}
