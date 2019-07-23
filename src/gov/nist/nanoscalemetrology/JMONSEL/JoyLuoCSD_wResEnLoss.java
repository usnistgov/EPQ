/**
 *
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MeanIonizationPotential;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.NISTMonte.Electron;

/**
 * <p>
 * Implements a form of the Bethe continuous energy loss formula as modified
 * first by Joy and Luo and subsequently by J. Lowney. The Joy/Luo modification
 * improves the accuracy at low energies. Lowney's modifications involve a
 * method for computing a parameter used by Joy and Luo (thereby extending the
 * Joy/Luo formula to elements that they did not tabulate) and the addition of a
 * residual energy loss. The parameter calculation requires the material
 * workfunction in Joules.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * This scattering mechanism uses the following material properties, which
 * therefore need to be properly defined: workfunction (unless supplied in the
 * constructor), the elemental composition, density, and weight fractions.
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
public class JoyLuoCSD_wResEnLoss
   implements SlowingDownAlg {

   private Material mat;

   private double resel; // residual energy loss rate

   private double wf; // work function

   private int nce; // # constituent elements

   /* Combinations of variables that we need */
   private double[] recipJ; // 1.166/J where J=ionization energy of const.

   // elem.

   private double[] coef; // 78500 rho c[i]Z[i]/A[i] = leading coefficient in

   private double[] beta; // 1=1.166(wf+1eV)/J[i]

   private double wfplus1eV; // WF + 1eV

   private double minEforTracking;

   /**
    * JoyLuoCSD_wResEnLoss - Creates a continuous slowing down object for the
    * specified material. This form of the constructor obtains the residual
    * energy loss rate and work function from the SEmaterial argument.
    *
    * @param mat
    */
   public JoyLuoCSD_wResEnLoss(SEmaterial mat, double resel) {
      this.resel = resel;
      setMaterial(mat);
   }

   /**
    * JoyLuoCSD_wResEnLoss - Creates a continuous slowing down object for the
    * specified material. This form of the constructor obtains the residual
    * energy loss rate and work function from the second and third arguments.
    *
    * @param mat - the material
    * @param resel - the residual energy loss rate in Joules/m.
    * @param wf - the work function in Joules.
    */
   public JoyLuoCSD_wResEnLoss(Material mat, double resel, double wf) {
      setMaterial(mat, resel, wf);
   }

   /**
    * setMaterial - Sets the material, residual energy loss rate, work function
    * for which losses are to be computed.
    *
    * @param mat - the material
    * @param resel - the residual energy loss rate in Joules/m.
    * @param wf - the work function in Joules.
    */
   public void setMaterial(Material mat, double resel, double wf) {
      this.mat = mat.clone();
      this.resel = resel;
      this.wf = wf;
      init();
   }

   /**
    * setMaterial - Sets the material, residual energy loss rate, work function
    * for which losses are to be computed. The latter two are taken from the
    * properties of the supplied (SEmaterial) argument.
    *
    * @param mat - the material
    */
   @Override
   public void setMaterial(SEmaterial mat) {
      this.mat = mat.clone();
      wf = mat.getWorkfunction();
      init();
   }

   /*
    * A utility routine to precalculate combinations of material-dependent
    * constants we will need for the computation.
    */
   private void init() {
      nce = mat.getElementCount();
      if(nce == 0)
         return;
      recipJ = new double[nce];
      coef = new double[nce];
      beta = new double[nce];
      wfplus1eV = wf + ToSI.eV(1.);
      final Object[] el = mat.getElementSet().toArray();

      for(int i = 0; i < nce; i++) {
         recipJ[i] = 1.166
               / (((Element) el[i]).getAtomicNumber() < 13 ? MeanIonizationPotential.Wilson41.compute((Element) el[i])
                     : MeanIonizationPotential.Sternheimer64.compute((Element) el[i]));
         beta[i] = 1. - (recipJ[i] * wfplus1eV);
         /*
          * The constant in the following expression is in units of J^2 m^2/kg.
          * It is appropriate for density in kg/m^3, energy in Joules, and
          * resulting continuous energy loss in J/m. 2.0096E-28
          */
         coef[i] = (2.01507E-28 * mat.getDensity() * mat.weightFraction((Element) el[i], true)
               * ((Element) el[i]).getAtomicNumber()) / ((Element) el[i]).getAtomicWeight();
      }
      /*
       * In the original MONSEL, the CSD routine knows the minEforTracking and
       * can use it to optimize--quitting early if the electron energy falls
       * below this during a step (since the electron will be dropped anyway).
       * In this version, minEforTracking is just a synonym for the work
       * function.
       */
      minEforTracking = wf;
   }

   private final double maxlossfraction = 0.1;

   /**
    * compute - Implements a MONSEL-style continuous slowing down approximation.
    * This routine computes the energy change for an electron that starts with
    * energy kE and moves a distance len.
    *
    * @param len - the distance of travel for which to compute the loss.
    * @param pe - the Electron object
    * @return - returns the energy lost by the electron in this step.
    */
   @Override
   public double compute(double len, Electron pe) {

      final double kE = pe.getEnergy();
      /*
       * No energy loss in vacuum, and don't waste time on any with energy
       * already low enough to be dropped.
       */
      if((nce == 0) || (kE < minEforTracking) || (kE <= 0.))
         return 0.;
      /*
       * Joy/Luo energy loss formula reaches 0 at WF + 1 eV. Below that we
       * return the residual energy loss
       */
      double loss;
      if(kE <= wfplus1eV) {
         loss = -resel * len;
         return loss > -kE ? loss : -kE;
      }
      /*
       * Lose energy in steps of at most maxlossfraction, until distance len has
       * been traversed
       */

      // Evaluate Joy/Luo loss rate based upon incident energy
      double JLlossrate = 0.;
      for(int i = 0; i < nce; i++)
         JLlossrate += coef[i] * Math.log((recipJ[i] * kE) + beta[i]);
      JLlossrate /= kE;

      // Compute loss based on this rate, or 0 if rate is negative

      loss = (JLlossrate + resel) * len;

      /*
       * Decide how big of a "bite" to take out of the remaining length. If the
       * Joy/Luo part of loss does not exceed maxlossfraction of kEremaining or
       * it is less than maxlossfraction of the residual energy loss (so resel
       * dominates) we can take the whole thing. Otherwise, we limit the step
       * size such that the Joy/Luo portion is equal to this maximum allowed
       * amount.
       */
      double maxloss = maxlossfraction * kE;

      if(loss < maxloss) // We can take the whole step
         return loss > -kE ? -loss : -kE;

      /* Otherwise, take steps that results in loss = maxloss */
      double kEremaining = kE;
      double remaininglen = len;
      double steplen;

      while(kEremaining >= minEforTracking) {
         final double centerE = kEremaining * (1. - (maxlossfraction / 2.));
         if(centerE <= wfplus1eV) {
            kEremaining -= resel * remaininglen;
            if(kEremaining < 0.)
               return -kE; // It lost it all
            else
               return kEremaining - kE;
         }

         JLlossrate = 0.;
         for(int i = 0; i < nce; i++)
            JLlossrate += coef[i] * Math.log((recipJ[i] * centerE) + beta[i]);
         JLlossrate /= centerE;
         final double totalLossRate = JLlossrate + resel;

         maxloss = maxlossfraction * kEremaining;
         // Compute loss based on this rate
         steplen = maxloss / totalLossRate;

         if((remaininglen <= steplen) || ((((1. / maxlossfraction) - 1.) * JLlossrate) <= resel)) {
            kEremaining -= remaininglen * totalLossRate;
            return (kEremaining > 0. ? kEremaining : 0.) - kE;
         }
         remaininglen -= steplen;
         kEremaining *= 1. - maxlossfraction;
      }

      /*
       * Ordinarily we remain in the above loop until all of the step is used
       * up, at which point we return. However, we can end up here if the energy
       * falls below the minimum energy for tracking (usually equal to the work
       * function). In that case this electron is to be dropped, so there is no
       * point spending time continuing.
       */
      return (kEremaining > 0. ? kEremaining : 0.) - kE;
   }

   /**
    * @return the resel
    */
   public double getResel() {
      return resel;
   }

   /**
    * @return - a string in the form
    *         "JoyLuoCSD_wResEnLoss(material,resel,workfunction)", where
    *         material, resel, and workfuction, are the parameters either
    *         supplied to the constructor or, in the case of workfunction,
    *         possibly ascertained from the material property.
    */
   @Override
   public String toString() {
      return "JoyLuoCSD_wResEnLoss(" + mat.toString() + "," + Double.valueOf(resel).toString() + "," + Double.valueOf(wf).toString()
            + ")";
   }
}
