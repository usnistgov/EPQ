package gov.nist.nanoscalemetrology.JMONSEL;

// import gov.nist.microanalysis.EPQLibrary.*;
import gov.nist.microanalysis.NISTMonte.Electron;

/**
 * <p>
 * An interface for defining slowing down algorithms. These algorithms compute
 * energy change for an electron as a function of initial energy and distance
 * moved. The loss depends upon properties of the medium through which the
 * electron travels, provided via setMaterial.
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
public interface SlowingDownAlg {
   /**
    * Sets the material for which the energy loss is to be computed
    *
    * @param mat
    */
   void setMaterial(SEmaterial mat);

   /**
    * compute - Computes the energy change for an electron with initial energy
    * eK traversing distance d. The return value is negative if the electron
    * loses energy.
    *
    * @param d
    *           double -- the distance moved by the electron
    * @param pe
    *           Electron, the primary electron
    * @return double -- the energy change
    */
   double compute(double d, Electron pe);

}
