package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.NISTMonte.Electron;

/**
 * <p>
 * Implements a model for trapping of electrons by polarons as described by
 * Ganachaud and Mokrani in Surf. Sci. 334 (1995) p 329.
 * </p>
 * <p>
 * In this model the rate of electron trapping follows an exponential law: rate
 * = prefactor*exp(-extinction*energy), where energy is the electron's kinetic
 * energy.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
public class GanachaudMokraniPolaronTrapSM extends ScatterMechanism {

   private final double prefactor;
   private final double extinction;
   private final double CUTOFF;

   /**
    * Constructs a GanachaudMokraniPolaronTrapSM. The scatter rate for an
    * electron obeys an exponential law: rate =
    * prefactor*exp(-extinction*energy), where energy is the electron's kinetic
    * energy and the other variables are parameters.
    *
    * @param prefactor
    *           - parameter (in inverse meters) in the exponential expression
    * @param extinction
    *           - parameter (in inverse joules) in the exponential expression
    */
   public GanachaudMokraniPolaronTrapSM(double prefactor, double extinction) {
      super();
      this.prefactor = prefactor;
      this.extinction = extinction;
      if (prefactor < 0.)
         throw new EPQFatalException("Nonpositive prefactor in GanachaudMokraniPolaronTrapSM constructor.");
      if (extinction < 0.)
         throw new EPQFatalException("Nonpositive extinction in GanachaudMokraniPolaronTrapSM constructor.");
      CUTOFF = -Math.log(10. / prefactor) / extinction;
   }

   /**
    * @param pe
    * @return Always returns null because this process does not produce
    *         secondary electrons.
    * @see gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#scatter(gov.nist.microanalysis.NISTMonte.Electron)
    */
   @Override
   public Electron scatter(Electron pe) {
      pe.setEnergy(0.); // So listeners, if any, will record the energy change.
      pe.setTrajectoryComplete(true); // It's trapped
      return null;
   }

   /**
    * @param pe
    * @return The average number of scattering events per meter of travel.
    * @see gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#scatterRate(gov.nist.microanalysis.NISTMonte.Electron)
    */
   @Override
   public double scatterRate(Electron pe) {
      final double kE0 = pe.getEnergy();
      if (kE0 > CUTOFF)
         return 0.;
      final double result = prefactor * Math.exp(-extinction * kE0);
      return result;
   }

   /**
    * @param mat
    * @see gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#setMaterial(gov.nist.microanalysis.EPQLibrary.Material)
    */
   @Override
   public void setMaterial(Material mat) {
      /*
       * There's nothing to do here. This is a required method, but the phonon
       * model doesn't require any parameters not already passed in the
       * constructor.
       */
   }

   /**
    * @return - a string in the form
    *         "GanachaudMokraniPolaronTrapSM(prefactor,extinction)", where
    *         prefactor and extinction are the parameters supplied to the
    *         constructor.
    */
   @Override
   public String toString() {
      return "GanachaudMokraniPolaronTrapSM(" + Double.valueOf(prefactor).toString() + "," + Double.valueOf(extinction).toString() + ")";
   }
}
