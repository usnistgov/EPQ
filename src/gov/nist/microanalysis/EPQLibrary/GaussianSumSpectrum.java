package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.EPQLibrary.Detector.DetectorLineshapeModel;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.Utility.Math2;

import java.util.Arrays;

/**
 * <p>
 * Description
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
public class GaussianSumSpectrum
   extends EditableSpectrum {

   private DetectorLineshapeModel mModel;

   public GaussianSumSpectrum(ISpectrumData sd) {
      super(sd);
      Arrays.fill(getCounts(), 0.0);
      final EDSDetector ed = (EDSDetector) getProperties().getDetector();
      mModel = ed.getDetectorLineshapeModel();
      SpectrumUtils.rename(this, "GaussianSumSpectrum");
   }

   public void setModel(DetectorLineshapeModel dlsm) {
      mModel = dlsm;
   }

   public void add(double center, double scale) {
      final double min = center - mModel.leftWidth(center, 0.0001), max = center + mModel.rightWidth(center, 0.0001);
      final int minCh = SpectrumUtils.bound(this, SpectrumUtils.channelForEnergy(this, min));
      final int maxCh = SpectrumUtils.bound(this, SpectrumUtils.channelForEnergy(this, max));
      for(int i = minCh; i < maxCh; ++i) {
         final double eV = SpectrumUtils.minEnergyForChannel(this, i);
         setCounts(i, getCounts(i) + (scale * mModel.compute(eV, center)));
      }
   }

   public void scale(double k) {
      Math2.timesEquals(k, getCounts());
   }
}
