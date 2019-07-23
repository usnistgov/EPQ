package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Extends ScatterMechanism to create a Moller Inelastic mechanism. The Moller
 * mechanism applies to scattering of a primary electron from an unbound
 * stationary target electron in the material. The result is two electrons with
 * nonzero kinetic energy, one of which (the less energetic of the pair by
 * convention) we consider to be a secondary electron.
 * </p>
 * <p>
 * In a real material electrons typically come with a distribution of binding
 * energies. This is a problem because the Moller model is not strictly
 * applicable to electrons with binding energies &gt; 0. In practice, tightly
 * bound core electrons have low cross sections for secondary electron
 * production, so this implementation simply ignores them. Those with small
 * binding energies (defined here as those within 10 eV of vacuum level) are
 * treated in approximate fashion as though they are free.
 * </p>
 * <p>
 * This scattering mechanism uses the following material properties, which
 * therefore need to be properly defined: workfunction, bindingEnergy array, and
 * electronDensity array.
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
public class MollerInelasticSM
   extends
   ScatterMechanism {

   /*
    * The following is the "free electron" density in electrons/m^3. This
    * material constant corresponds to (Avagadro's #) * (mass density) * (#
    * valence electrons) divided by (atomic weight) in the original MONSEL
    * series. The Moller mechanism applies to free electrons--the original
    * MONSEL series loosely equated the free electrons with those in the atomic
    * valence shells. These were supplied by the user for each element when he
    * specified the material properties. For a material comprised of several
    * constituent elements the original MONSEL series formed a
    * stoichiometrically weighted average of the # valence electrons of each of
    * the constituents. If desired, the number of free electrons could deviate
    * from the above loose definition. This might be desirable, for example, if
    * some of the valence electrons are so tightly bound in reality that the
    * user thinks they should be ignored in a Moller free-electron
    * approximation.
    */
   private double feDensity;

   /*
    * In an SEmaterial the material data are specified differently than in the
    * original MONSEL. Instead of a number of valence electrons we have an array
    * of densities and corresponding binding energies. We therefore adopt a rule
    * that counts as free all those electrons with binding energies below a
    * threshold and ignores the rest. The default threshold is 10 eV. Although
    * any such sharp cutoff between "free" and "bound" is necessarily a crude
    * approximation, such is what we are forced to do by using a model that
    * applies only to free electrons, as this Moller model does. 10 eV seems a
    * reasonable place to put the dividing line. The original MONSEL assigned
    * nve = 4 to Si. Since silicon's valence band (which contains those 4
    * electrons) extends to almost 20 eV below the vacuum level, that original
    * choice corresponded a 20 eV threshold, a choice that on reflection we
    * think too high. Choosing a lower threshold allows the residual energy loss
    * (in the original MONSEL stopping power formula) to be smaller, and this is
    * on the whole a good thing. The default choice of cutoff can be altered by
    * specifying a different one in the second form of the constructor.
    */
   private final double FECUTOFF = ToSI.eV(10.);
   private final double SIGMA0 = Math.pow(PhysicalConstants.ElectronCharge, 4)
         / Math.pow(4 * PhysicalConstants.PermittivityOfFreeSpace, 2) / Math.PI;

   /*
    * The scatter routine must solve for the energy of the produced SE by
    * solving an equation that involves a logarithmic term. The logarithmic term
    * prevents an analytical solution, thereby slowing evaluation if it is used.
    * Instead this model approximates the expression by using a rational
    * function (A+B*eps+C*esp^2+D*eps^3)/(1+F*eps+G*eps^2). The coefficients are
    * chosen to be those that give the best least squares fit to the exact
    * expression subject to constraints. The constraints require that the
    * approximate expression have 0 error at the endpoints (epsilon = 0 and
    * epsilon = 1/2) and 0 error in its derivative at epsilon = 1/2. With these
    * constraints the best fit (See my notes in
    * R:\proj\linewidth\jvillar\develop\NewMONSEL\Physics\MollerMechanism.nb)
    * produces the following values for the constants.
    */
   final private double a = 1.0;
   final private double b = 1.43511;
   final private double c = -12.8184;
   final private double d = 11.8963;
   final private double f = 6.64533;
   final private double g = -12.3686;

   /* Material constants */
   private double sr_const; // Leading material constant in scatter
   // rate formula
   private double minEgenSE;

   /* Cached values */
   private double last_kE = -1.; // Stores the last kE for which
   // feps_over_eps was computed. Initialized to unphysical value.
   private double last_feps_over_eps;
   private double fecutoff = FECUTOFF; // sets to default

   /**
    * Constructs a MollerInelasticSM with the cutoff energy for determining what
    * constitutes a free electron set to its default value.
    */
   public MollerInelasticSM(SEmaterial mat) {
      super();
      minEgenSE = mat.getWorkfunction();
      setMaterial(mat);
   }

   /**
    * Constructs a MollerInelasticSM with the cutoff energy for determining what
    * constitutes a free electron set by the second argument.
    */
   public MollerInelasticSM(SEmaterial mat, double fecutoff) {
      super();
      this.fecutoff = fecutoff;
      minEgenSE = mat.getWorkfunction();
      setMaterial(mat);
   }

   /*
    * A utility routine to calculate the f(eps_min)/eps_min function in the
    * exact form (with the logarithm in it). eps_min is minEgenSE/kE. Since the
    * numerator is a material constant, it need not be passed, and the function
    * argument is simply the kinetic Energy, kE. The result is cached so the
    * calculation need not be repeated if the next call is for the same
    * argument. This routine fails if kE <= minEgenSE. The caller is responsible
    * to not supply such inputs! (That's why this is a private method. It can
    * only be called by members of this class, and those members can be held
    * accountable if they break this rule!)
    */
   private double fepsm_over_epsm(double kE) {
      if(kE != last_kE) {
         /*
          * Divisions in the following lines should be safe because the calling
          * routines within this class guarantee 0 < eps <= 1/2
          */
         final double eps = minEgenSE / kE;
         final double recip_eps = 1. / eps;
         last_kE = kE; // Save for next time
         assert recip_eps > 1.;
         last_feps_over_eps = (recip_eps - (1. / (1. - eps)) - Math.log(recip_eps - 1.));
      }
      return last_feps_over_eps;
   }

   /**
    * @return Returns the minEgenSE.
    */
   public double getMinEgenSE() {
      return minEgenSE;
   }

   /**
    * Simulates a Moller scatter mechanism, in which the target electron is
    * assumed to be unbound and at rest before scattering. This target becomes a
    * secondary electron with energy governed by a constrained cubic rational
    * function approximation of the Moller probability distribution. The
    * constraints require the approximation to match the true function for
    * energy transfers of 0 and 1/2 of the PE's kinetic energy. The derivative
    * of the approximation is also constrained to match the exact form at the
    * higher energy. The SE and PE will leave the scatter site on vectors 90
    * degrees apart, as dictated by conservation of energy and momentum. The PE
    * energy is not decremented by the amount of energy transferred to the
    * electron, since this energy loss is assumed to already be included in the
    * slowing down algorithm that is in use. This scatter mechanism should
    * therefore be paired with a slowing down algorithm for which that is true.
    *
    * @see gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#scatter(Electron)
    */
   @Override
   public Electron scatter(Electron pe) {
      /*
       * Use the electron's energy before the slowing down loss. This avoids the
       * following situation: An electron with energy just above minEgenSE has a
       * scatterRate > 0 and is determined to have scattered by this mechanism.
       * It moves to the scatter site, and its energy is decremented. It then
       * has energy less than minEgenSE, leading to a negative argument in the
       * logarithm when fepsm_over_epsm is computed.
       */
      final double kE = pe.getPreviousEnergy();
      // double kE = pe.getEnergy();

      final double rk = Math2.rgen.nextDouble() * fepsm_over_epsm(kE);
      double eps;

      if(rk < 1.E3) {
         /*
          * This block implements the solution of a cubic equation for eps. This
          * is a simple form of the solution, valid only for the special case
          * where q>0 and 4q^3-p^2 > 0. These conditions can be shown to always
          * apply (i.e., for any value of 0<=rk<infinity) when the coefficients
          * a-g are as given above. (See
          * R:\proj\linewidth\jvillar\develop\NewMONSEL\NISTMonte) Changing the
          * coefficients could render this solution invalid.
          */
         final double cc = c - (f * rk);
         final double dd = d - (g * rk);
         final double ccSquared = cc * cc;
         final double bbdd = (b - rk) * dd;
         final double q = ccSquared - (3 * bbdd);
         final double p = ((-2 * ccSquared * cc) + (9 * bbdd * cc)) - (27 * a * dd * dd);
         final double atanOver3 = Math.atan2(Math.sqrt((4 * q * q * q) - (p * p)), p) / 3.;
         eps = ((((Math.sqrt(3.) * Math.sin(atanOver3)) - Math.cos(atanOver3)) * Math.sqrt(q)) - cc) / 3. / dd;
      } else
         eps = 1. / rk;

      final double kE_SE = eps * kE; // Energy of the SE
      /*
       * Polar scattering angles. These are given in the coordinate system where
       * z axis is PE initial direction. That is, these are angular deflections.
       */
      final double polarAngleSE = Math.acos(Math.sqrt(eps));
      final double polarAnglePE = (Math.PI / 2.) - polarAngleSE;
      /*
       * Azimuthal angle of SE deflection is uniformly distributed.
       */
      final double azimuthalAngleSE = 2. * Math.PI * Math2.rgen.nextDouble();

      /*
       * Create a SE with appropriate energy but trajectory initially the same
       * as the PE initial trajectory. Then deflect the trajectory the required
       * amount using the Electron class's updateDirection() method.
       */
      final Electron se = new Electron(pe, pe.getTheta(), pe.getPhi(), kE_SE);
      se.updateDirection(polarAngleSE, azimuthalAngleSE);
      /*
       * Update PE trajectory. Azimuthal angle of PE deflection is opposite that
       * of SE deflection. Note that the energy of the PE is not decremented by
       * kE_SE. This is because this scatter mechanism is intended to be paired
       * with a continuous energy loss formula that includes all loss
       * mechanisms, including this one. To decrement the energy here would
       * double count. This means that this model conserves energy on average,
       * but not in individual collisions. I could write another continuous
       * energy loss formula, one that excludes the average energy loss from
       * each of my explicitly included inelastic mechanisms. In that case I
       * could write a similar Moller mechanism to be paired with it, one that
       * would decrement PE energy here, thereby conserving energy in each
       * collision instead of merely on average. It's on my to-do list to try.
       */
      pe.updateDirection(polarAnglePE, azimuthalAngleSE - Math.PI);

      return se;
   }

   /*
    * (non-Javadoc)
    * @see
    * gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#scatterRate(double)
    */
   @Override
   public double scatterRate(Electron pe) {
      final double kE = pe.getEnergy(); // The PE kinetic energy
      /*
       * The SE must end up with <= 1/2 kE. If this is less than the minimum
       * energy for generating an SE, there can be no SE, so return scatter rate
       * = 0.
       */
      if(kE < (2. * minEgenSE))
         return 0.;
      /*
       * Otherwise, compute the scatter rate according to Moller formula. 0 <
       * minEgenSE / kE <= 1/2 in this call. The lower limit is guaranteed
       * because minEgenSE > 0 is checked by setMaterial() and kE>=2*minEgenSE
       * (as guaranteed by the above line) is therefore also > 0. The ratio <=
       * 1/2 is guaranteed by the above line.
       */

      return (sr_const * fepsm_over_epsm(kE)) / (kE * kE);
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
         throw new EPQFatalException("Material " + mat.toString() + " is not an SEmaterial as required for MollerInelasticSM.");
      final Double[] binding = ((SEmaterial) mat).getBindingEnergyArray();
      final Double[] density = ((SEmaterial) mat).getElectronDensityArray();
      feDensity = 0.;
      for(int i = 0; i < binding.length; i++)
         if(binding[i] <= fecutoff)
            feDensity += density[i];
      sr_const = feDensity * SIGMA0;
      return;
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
