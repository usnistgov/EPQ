package gov.nist.microanalysis.EPQLibrary.Detector;

import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;

/**
 * A concrete implementation of the DetectorLineshapeModel using Fiori's
 * resolution model for a Si(Li) detector.
 */

public class FanoSiLiLineshape
   extends DetectorLineshapeModel {

   /**
    * mFanoFactor and mNoise define the detector resolution as a function of
    * energy
    */
   private final double mFanoFactor;
   private final double mNoise;
   /**
    * mScale accounts for incomplete knowledge of the geometric efficiency.
    */
   private final double mScale;

   private double getFWHM() {
      return SpectrumUtils.gaussianWidthToFWHM(SpectrumUtils.resolution(mFanoFactor, mNoise, SpectrumUtils.E_MnKa));
   }

   // g(e) = exp(-0.5*(e/w)^2)/(sqrt(2 PI) w)

   /**
    * Constructs a simple Gaussina model of a Si(Li) style detector which is
    * characterized by the full-width half-maximum at Mn Kα
    * 
    * @param fwhmAtMnKa in eV
    */
   public FanoSiLiLineshape(double fwhmAtMnKa) {
      super();
      assert fwhmAtMnKa > 1.0;
      mFanoFactor = 0.122;
      mNoise = SpectrumUtils.noiseFromResolution(mFanoFactor, SpectrumUtils.fwhmToGaussianWidth(fwhmAtMnKa), SpectrumUtils.E_MnKa);
      mScale = 1.0;
      assert Math.abs(fwhmAtMnKa - getFWHM()) < (1.0e-6 * fwhmAtMnKa);
   }

   public FanoSiLiLineshape(double fano, double noise) {
      super();
      mFanoFactor = fano;
      mNoise = noise;
      mScale = 1.0;
   }

   public FanoSiLiLineshape() {
      super();
      mFanoFactor = 0.122;
      mNoise = 8.5;
      mScale = 1.0;
   }

   @Override
   public FanoSiLiLineshape clone() {
      return new FanoSiLiLineshape(mFanoFactor, mNoise);
   }

   private double gaussianLinewidth(double eV) {
      return SpectrumUtils.resolution(mFanoFactor, mNoise, eV);
   }

   @Override
   public double leftWidth(double eV, double fraction) {
      return gaussianLinewidth(eV) * Math.sqrt(-2.0 * Math.log(fraction));
   }

   @Override
   public double rightWidth(double eV, double fraction) {
      return gaussianLinewidth(eV) * Math.sqrt(-2.0 * Math.log(fraction));
   }

   @Override
   public double compute(double eV, double center) {
      return SpectrumUtils.gaussian(eV - center, gaussianLinewidth(center));
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.Detector.DetectorLineshapeModel#getScale()
    */
   @Override
   public double getScale() {
      return mScale;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      long temp;
      temp = Double.doubleToLongBits(mFanoFactor);
      result = (prime * result) + (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(mNoise);
      result = (prime * result) + (int) (temp ^ (temp >>> 32));
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if(this == obj)
         return true;
      if(getClass() != obj.getClass())
         return false;
      final FanoSiLiLineshape other = (FanoSiLiLineshape) obj;
      if(Double.doubleToLongBits(mFanoFactor) != Double.doubleToLongBits(other.mFanoFactor))
         return false;
      if(Double.doubleToLongBits(mNoise) != Double.doubleToLongBits(other.mNoise))
         return false;
      return true;
   }

   public double getFanoFactor() {
      return mFanoFactor;
   }

   public double getNoise() {
      return mNoise;
   }
}
