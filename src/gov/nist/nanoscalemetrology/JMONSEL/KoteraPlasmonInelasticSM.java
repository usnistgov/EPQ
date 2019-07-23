package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Extends ScatterMechanism to create a plasmon mediated inelastic mechanism, as
 * described by Kotera et al, Jpn J Appl Phys 29 (1990) p 2277. This
 * implementation follows Lowney, Scanning 18 (1996) p. 301. In this mechanism
 * the primary electron excites a plasmon resonance mode in the target material.
 * The plasmon subsequently decays by production of a secondary electron (SE)
 * with energy equal to the plasmon energy and direction isotropically
 * distributed.
 * </p>
 * <p>
 * Results of Scheinfein et al., Phys Rev B 47, (1993) p. 4068 question whether
 * this is a significant pathway in Si, C, and perhaps other materials. Even
 * though plasmon excitation is readily observed in these materials, few SE of
 * corresponding energy were seen. In view of this, this implementation includes
 * an additional parameter, an efficiency factor between 0 and 1, that can be
 * set to model the possibility that some plasmon excitations decay without
 * producing SE.
 * </p>
 * <p>
 * This scattering mechanism uses the following material properties, which
 * therefore need to be properly defined: workfunction and eplasmon.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain.
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */

/*
 * This scatter mechanism was part of the original MONSEL series. I mean to keep
 * it around in this form for historical interest. It should only be edited to
 * correct differences between it and the original MONSEL algorithms. Ideas for
 * improvements should be implemented in a new scatter mechanism under a new
 * name.
 */
public class KoteraPlasmonInelasticSM
   extends ScatterMechanism {

   // private Material mat;

   private double plasmonE; // Plasmon energy
   private double efficiency = 1.; // Default = 1
   private double minEgenSE;

   private double sr_const; // Leading constant in scatterRate formula
   /*
    * crossoverE is the energy where the maximum allowable momentum transfer due
    * to Landau damping is equal to the minimum allowable transfer by
    * conservation of momentum and energy. For PE energies below this the
    * scatter rate is 0. See the derivation in PlasmonMechanism.nb
    */
   private double crossoverE;
   private double maxtransferLandau;

   /**
    *
    */
   public KoteraPlasmonInelasticSM(SEmaterial mat) {
      super();
      this.minEgenSE = mat.getWorkfunction();
      setMaterial(mat);
   }

   public KoteraPlasmonInelasticSM(SEmaterial mat, double efficiency) {
      super();
      this.efficiency = efficiency;
      this.minEgenSE = mat.getWorkfunction();
      setMaterial(mat);
   }

   public void setEfficiency(double efficiency) {
      this.efficiency = efficiency;
      sr_const = (efficiency * plasmonE) / 2. / PhysicalConstants.BohrRadius;
   }

   public double getEfficiency() {
      return efficiency;
   }

   /*
    * (non-Javadoc)
    * @see
    * gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#scatterRate(gov.nist
    * .microanalysis.NISTMonte.Electron)
    */
   @Override
   public double scatterRate(Electron pe) {
      final double kE = pe.getEnergy();
      if((plasmonE >= kE) || (minEgenSE > plasmonE) || (kE <= crossoverE))
         return 0.;
      final double k0 = Math.sqrt(kE); // magnitude PE intial momentum
      // (proportional)
      final double kf = Math.sqrt(kE - plasmonE); // magnitude PE final
      // momentum
      // Momentum change if PE continues in same direction
      final double mintransfer = k0 - kf;

      return (sr_const * Math.log(maxtransferLandau / mintransfer)) / kE;
   }

   /*
    * The plasmon is assumed to decay into a secondary electron with energy
    * equal to the plasmon energy and trajectory isotropically distributed. The
    * change in PE direction is treated as negligible. The change in PE energy
    * is already included in an average way in the continuous energy loss
    * formula, so to include it here would be double counting.
    * @see
    * gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#scatter(gov.nist.
    * microanalysis.NISTMonte.Electron)
    */
   @Override
   public Electron scatter(Electron pe) {
      return new Electron(pe, Math.acos(1. - (2. * Math2.rgen.nextDouble())), 2. * Math.PI * Math2.rgen.nextDouble(), plasmonE);
   }

   /*
    * (non-Javadoc)
    * @see
    * gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#setMaterial(gov.nist
    * .microanalysis.EPQLibrary.Material)
    */
   @Override
   public void setMaterial(Material mat) {
      if(!(mat instanceof SEmaterial))
         throw new EPQFatalException("Material " + mat.toString()
               + " is not an SEmaterial as required for KoteraPlasmonInelasticSM.");
      // this.mat = mat;

      plasmonE = ((SEmaterial) mat).getEplasmon();

      sr_const = (efficiency * plasmonE) / 2. / PhysicalConstants.BohrRadius;
      /*
       * eFermi is related to the plasmon energy because both depend upon the
       * electron density. Eliminating the density between the two equations
       * results in the following expression. (Jerry describes this in the
       * SCANNING paper. The derivation is carried out in SI units in my notes:
       * PlasmonMechanism.nb. The leading constant here is in units of
       * Joules^(-1/3) and is appropriate for plasmonE expressed in Joules.
       */
      final double eFermi = 541947. * Math.pow(plasmonE, 4. / 3.);
      crossoverE = eFermi + plasmonE;
      maxtransferLandau = Math.sqrt(crossoverE) - Math.sqrt(eFermi);
   }

   /**
    * @return Returns the minEgenSE.
    */
   public double getMinEgenSE() {
      return minEgenSE;
   }

   /**
    * The minimum energy for generated SE is equal to the material work function
    * by default. It can be set to a different positive value by using this
    * method. SE with energies below this value are "turned off".
    *
    * @param minEgenSE The minEgenSE to set.
    */
   public void setMinEgenSE(double minEgenSE) {
      if(minEgenSE > 0.)
         this.minEgenSE = minEgenSE;
      else
         throw new EPQFatalException("Illegal minEgenSE.");
   }
}
