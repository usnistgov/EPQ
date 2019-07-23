package gov.nist.microanalysis.EPQLibrary.Detector;

import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;

/**
 * <p>
 * An abstraction of the linewidths to facilitate playing with various different
 * models for the shape of lines for various different detectors. We can use
 * this to add selfs or even sum and escape peaks presumably.
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
abstract public class DetectorLineshapeModel
   implements
   Cloneable {

   /**
    * The inverse of compute on the left (lower energy side of the peak.) At
    * what energy is the peak intensity diminished to fraction of its peak
    * value.
    * 
    * @param eV
    * @param fraction
    * @return double - distance from the center <code>eV</code> in eV
    */
   abstract public double leftWidth(double eV, double fraction);

   /**
    * The inverse of compute on the right (higher energy side of the peak.) At
    * what energy is the peak intensity diminished to fraction of its peak
    * value.
    * 
    * @param eV
    * @param fraction
    * @return double - distance from the center <code>eV</code> in eV
    */
   abstract public double rightWidth(double eV, double fraction);

   /**
    * Computes the intensity at the specified energy <code>eV</code> for a peak
    * at <code>center</code> The integral over all energies is normalized to
    * 1.0.
    * 
    * @param eV - The energy of interest
    * @param center - The peak center in eV
    * @return double - width in eV
    */
   abstract public double compute(double eV, double center);

   @Override
   abstract public boolean equals(Object obj);

   @Override
   abstract public int hashCode();

   /**
    * Returns the nominal full-width half max at Mn Ka
    * 
    * @return in eV
    */
   public double getFWHMatMnKa() {
      return leftWidth(SpectrumUtils.E_MnKa, 0.5) + rightWidth(SpectrumUtils.E_MnKa, 0.5);
   }

   @Override
   abstract public DetectorLineshapeModel clone();

   /**
    * A scale factor to account for differences in geometric efficiencies not
    * correctly accounted for by geometric factors.
    * 
    * @return double
    */
   public double getScale() {
      return 1.0;
   }

}
