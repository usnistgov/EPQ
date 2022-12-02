package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Implements a model for inelastic scattering of electrons from phonons as
 * described by Ganachaud and Mokrani in Surf. Sci. 334 (1995) p 329. The model
 * was attributed by them to earlier work of Llacer and Garwin and others.
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
public class GanachaudMokraniPhononInelasticSM extends ScatterMechanism {

   private final double ratemultiplier;
   private final double phononE; // Energy of the phonon mode
   private final double occupationFactor; // Typically ~ 1/2 at room temperature
   private final double epsRatio; // (eps0-epsinfinity)/esp0/epsinfinity
   private final double prefactor;
   private final double temperature;
   private final double eps0;
   private final double epsInfinity;

   /**
    * Constructs a GanachaudMokraniPhononInelasticSM
    *
    * @param ratemultiplier
    *           - 1 for a single phonon, the rate scales proportionally to this
    *           input.
    * @param phononE
    *           - The energy (in J) of phonons in this mode
    * @param temperature
    *           - Temperature of the medium (in K)
    * @param eps0
    *           - The DC dielectric constant
    * @param epsInfinity
    *           - The dielectric constant at high frequency
    */
   public GanachaudMokraniPhononInelasticSM(double ratemultiplier, double phononE, double temperature, double eps0, double epsInfinity) {
      super();
      this.ratemultiplier = ratemultiplier;
      this.phononE = phononE;
      this.temperature = temperature;
      this.eps0 = eps0;
      this.epsInfinity = epsInfinity;

      if (ratemultiplier <= 0.)
         throw new EPQFatalException("Nonpositive ratemultiplier in GanachaudMokraniPhononInelasticSM constructor.");
      if (phononE <= 0.)
         throw new EPQFatalException("Nonpositive phononE in GanachaudMokraniPhononInelasticSM constructor.");
      occupationFactor = 0.5 * (1. + (1. / (Math.exp(phononE / (PhysicalConstants.BoltzmannConstant * temperature)) - 1.)));
      epsRatio = (eps0 - epsInfinity) / eps0 / epsInfinity;
      if (epsRatio <= 0.)
         throw new EPQFatalException("(eps0-epsInfinity)/eps0/epsInfinity < 0 in GanachaudMokraniPhononInelasticSM constructor.");
      prefactor = (this.ratemultiplier * occupationFactor * epsRatio) / PhysicalConstants.BohrRadius;
   }

   /**
    * @param pe
    * @return Always returns null, because this process does not generate
    *         secondary electrons.
    * @see gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#scatter(gov.nist.microanalysis.NISTMonte.Electron)
    */
   @Override
   public Electron scatter(Electron pe) {
      final double kE0 = pe.getEnergy();
      if (kE0 < phononE)
         return null;

      final double x = phononE / kE0; // Energy ratio

      final double[] randoms = new double[]{Math2.rgen.nextDouble(), Math2.rgen.nextDouble()};

      double costheta; // scattering angle
      if (x < 0.1)
         costheta = 1 + (((x * x) - (Math.pow(16., randoms[0]) * Math.pow(x, 2. - (2. * randoms[0])))) / 8.);
      else { // Using general formula
         final double root = Math.sqrt(1. - x);
         final double temp = Math.pow(((-2. * (1 + root)) + x) / ((-2. * (1 - root)) + x), randoms[0]);
         costheta = temp + (((x - 2.) * (temp - 1)) / 2. / root);
      }
      final double phi = 2. * Math.PI * randoms[1];

      pe.updateDirection(Math.acos(costheta), phi);
      pe.setEnergy(kE0 - phononE);
      pe.setPreviousEnergy(kE0);

      return null;
   }

   /**
    * @param pe
    * @return - The average number of scattering events per meter
    * @see gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#scatterRate(gov.nist.microanalysis.NISTMonte.Electron)
    */
   @Override
   public double scatterRate(Electron pe) {
      final double kE = pe.getEnergy();
      if (kE < phononE)
         return 0.;
      final double x = phononE / kE; // Energy ratio
      /*
       * In the usual case the PE has energy of a few eV. Phonons typically have
       * energies ~0.1 eV or lower, so the above ratio is usually ~1/50 or so.
       * For such small x we calculate using a series expansion form. This is
       * simpler than the general expression, it is faster, and it avoids
       * numerical round-off problems. For larger x, which we expect to
       * encounter only rarely, we use the exact expression.
       */
      double result;
      if (x < 0.1) {
         result = prefactor * x * Math.log(4. / x);
         return result;
      } else if (x >= 1.) // phonon energy >= PE energy: no scattering possible
         return 0.;
      else {
         final double temp = Math.sqrt(1. - x);
         result = prefactor * x * Math.log((1. + temp) / (1. - temp));
         return result;
      }
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
    *         "GanachaudMokraniPhononInelasticSM(ratemultiplier,phononE,temp,eps0,epsinf)"
    *         , where the parameters are the rate multiplier, phonon energy,
    *         temperature, DC dielectric constant, and high frequency dielectric
    *         constant as supplied to the constuctor.
    */
   @Override
   public String toString() {
      return "GanachaudMokraniPhononInelasticSM(" + Double.valueOf(ratemultiplier).toString() + "," + Double.valueOf(phononE).toString() + ","
            + Double.valueOf(temperature).toString() + "," + Double.valueOf(eps0).toString() + "," + Double.valueOf(epsInfinity).toString() + ")";
   }

}
