package gov.nist.microanalysis.EPQLibrary.Detector;

import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;

/**
 * A concrete implementation of the DetectorLineshapeModel using Fiori's
 * resolution model for a Si(Li) detector.
 */

public class BasicSiLiLineshape
   extends DetectorLineshapeModel {

   private final double mFWHMatMnKa;

   // g(e) = exp(-0.5*(e/w)^2)/(sqrt(2 PI) w)

   /**
    * Constructs a simple Gaussina model of a Si(Li) style detector which is
    * characterized by the full-width half-maximum at Mn Kα
    * 
    * @param fwhmAtMnKa in eV
    */
   public BasicSiLiLineshape(double fwhmAtMnKa) {
      super();
      assert fwhmAtMnKa > 1.0;
      mFWHMatMnKa = fwhmAtMnKa;
      assert Math.abs(getFWHMatMnKa() - mFWHMatMnKa) < (mFWHMatMnKa / 1.0e6) : Double.toString(getFWHMatMnKa());
   }

   @Override
   public BasicSiLiLineshape clone() {
      return new BasicSiLiLineshape(mFWHMatMnKa);
   }

   private double gaussianLinewidth(double eV) {
      return SpectrumUtils.fwhmToGaussianWidth(SpectrumUtils.linewidth_eV(eV, mFWHMatMnKa, SpectrumUtils.E_MnKa));
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

   @Override
   public int hashCode() {
      final int PRIME = 31;
      int result = 1;
      long temp;
      temp = Double.doubleToLongBits(mFWHMatMnKa);
      result = (PRIME * result) + (int) (temp ^ (temp >>> 32));
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if(this == obj)
         return true;
      if(obj == null)
         return false;
      if(getClass() != obj.getClass())
         return false;
      final BasicSiLiLineshape other = (BasicSiLiLineshape) obj;
      if(Double.doubleToLongBits(mFWHMatMnKa) != Double.doubleToLongBits(other.mFWHMatMnKa))
         return false;
      return true;
   }
}
