/**
 * gov.nist.nanoscalemetrology.JMONSEL.FittedInelSM
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Implements a simple parameterized inelastic scattering mechanism. The
 * parameters can be chosen to match measured yield vs. energy curves, similar
 * to the method used by Lin &amp; Joy, Surf. Interface Anal. 37, (2005), pp.
 * 895-900
 * </p>
 * <p>
 * The model takes an energy, Eav, and a SlowingDownAlg. The energy represents
 * the amount of energy transferred to a secondary electron at each generation
 * event. The inverse mean free path (events per unit path length) is determined
 * as the ratio of stopping power (from the SlowingDownAlg) to Eav. At each
 * scattering event, an SE is generated with kinetic energy equal to the Fermi
 * energy + Eav. The SE's direction is chosen at random from an isotropic
 * distribution.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain.
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
public class FittedInelSM
   extends
   ScatterMechanism {

   private final SlowingDownAlg sdAlg;
   private final double energySEgen; // Average energy for SE generation
   private double eFermi;

   /**
    * Constructs a FittedInelSM
    */
   public FittedInelSM(SEmaterial mat, double energySEgen, SlowingDownAlg sdAlg) {
      super();
      this.sdAlg = sdAlg;
      this.energySEgen = energySEgen;
      setMaterial(mat);
   }

   /**
    * Computes the result of a scattering event. The primary electron energy and
    * direction of travel are unmodified. I.e., it is necessary to separately
    * account for slowing down by specifying a slowing down algorithm. A
    * secondary electron with energy energySEgen (the parameter provided to the
    * constructor) + the Fermi energy is produced with a randomly oriented
    * (isotropic) initial velocity.
    *
    * @param pe
    * @return - Returns a secondary electron
    * @see gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#scatter(gov.nist.microanalysis.NISTMonte.Electron)
    */
   @Override
   public Electron scatter(Electron pe) {
      final double phi = 2 * Math.PI * Math2.rgen.nextDouble();
      final double theta = Math.acos(1. - (2. * Math2.rgen.nextDouble()));
      return new Electron(pe, theta, phi, energySEgen + eFermi);
   }

   /**
    * Computes scattering rate (inverse mean free path) for the primary
    * electron.
    *
    * @param pe
    * @return - Returns number of scattering events per meter
    * @see gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#scatterRate(gov.nist.microanalysis.NISTMonte.Electron)
    */
   @Override
   public double scatterRate(Electron pe) {
      if(pe.getEnergy() <= (energySEgen + eFermi))
         return 0.;
      return (-sdAlg.compute(1.e-10, pe) * 1.e10) / energySEgen;
   }

   /**
    * @param mat
    * @see gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#setMaterial(gov.nist.microanalysis.EPQLibrary.Material)
    */
   @Override
   public void setMaterial(Material mat) {
      eFermi = ((SEmaterial) mat).getEFermi();

   }

   /**
    * @return - a string in the form "FittedInelSM(eFermi,energySEgen,sdAlg)",
    *         where eFermi is the Fermi energy of the material, energySEgen is
    *         the average SE generation energy and sdAlg is the slowing down
    *         algorithm supplied in the constructor.
    */
   @Override
   public String toString() {
      return "FittedInelSM(" + Double.valueOf(eFermi).toString() + "," + Double.valueOf(energySEgen).toString() + "," + sdAlg.toString()
            + ")";
   }

}
