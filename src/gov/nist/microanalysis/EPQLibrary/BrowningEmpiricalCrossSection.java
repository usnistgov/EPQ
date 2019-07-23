package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Computes the elastic scattering cross section for electrons of energy between
 * 0.1 and 30 keV for the specified element target. The algorithm comes from<br>
 * Browning R, Li TZ, Chui B, Ye J, Pease FW, Czyzewski Z &amp; Joy D; J Appl
 * Phys 76 (4) 15-Aug-1994 2016-2022
 * </p>
 * <p>
 * The implementation is designed to be similar to the implementation found in
 * MONSEL.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class BrowningEmpiricalCrossSection {
   private final Element mElement;
   // Cached values
   private final transient double mZp17;
   private final transient double mZp2;
   private final transient double mZp3;

   public BrowningEmpiricalCrossSection(Element elm) {
      mElement = elm;
      // Precompute these...
      mZp17 = Math.pow(elm.getAtomicNumber(), 1.7);
      mZp2 = Math.pow(elm.getAtomicNumber(), 2.0);
      mZp3 = Math.pow(elm.getAtomicNumber(), 3.0);
   }

   public Element getElement() {
      return mElement;
   }

   /**
    * totalCrossSection - Computes the total cross section for an electron of
    * the specified energy.
    * 
    * @param energy double - In Joules
    * @return double - in square meters
    */
   final public double totalCrossSection(double energy) {
      final double e = FromSI.keV(energy);
      final double re = Math.sqrt(e);
      return (3.0e-22 * mZp17) / (e + (0.005 * mZp17 * re) + ((0.0007 * mZp2) / re));
   }

   /**
    * randomScatteringAngle - Returns a randomized scattering angle in the range
    * [0,PI] that comes from the distribution of scattering angles for an
    * electron of specified energy on an atom of the element represented by the
    * instance of this class.
    * 
    * @param energy double - In Joules
    * @return double - an angle in radians
    */
   final public double randomScatteringAngle(double energy) {
      final double r1 = Math2.rgen.nextDouble(), r2 = Math2.rgen.nextDouble();
      final double z = mElement.getAtomicNumber();
      final double e = FromSI.keV(energy);
      final double r = ((300.0 * e) / z) + (mZp3 / (3.0e5 * e));
      if(r1 <= (r / (r + 1))) {
         // Screened Rutherford scattering
         final double alpha = 7.0e-3 / e;
         return Math.acos(1.0 - ((2.0 * alpha * r2) / ((alpha - r2) + 1)));
      } else
         // Isotropic scattering
         return Math.acos(1 - (2.0 * r2));
   }
}
