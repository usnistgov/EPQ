package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.NISTMonte.Electron;

/**
 * <p>
 * Implements a zero continuous energy loss formula. ZeroCSD always returns 0
 * energy loss. It is meant to be paired with inelastic scatter mechanisms that
 * themselves account for all energy losses, leaving none left over to be dealt
 * with via the continuous slowing down approximation.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
public class ZeroCSD implements SlowingDownAlg {

   /**
    * ZeroCSD - Creates a continuous slowing down object for the specified
    * material.
    */
   public ZeroCSD() {
   }

   @Override
   public double compute(double d, Electron pe) {
      return 0.;
   }

   @Override
   public void setMaterial(SEmaterial mat) {
   }

   /**
    * @return - the string "ZeroCSD()"
    */
   @Override
   public String toString() {
      return "ZeroCSD()";
   }

}
