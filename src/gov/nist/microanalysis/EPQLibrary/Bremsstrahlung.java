package gov.nist.microanalysis.EPQLibrary;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * A class for computing the Bremsstrahlung from an energetic electron
 * interacting with an atom. The magnitude of the Bremsstrahlung is derived from
 * the tabulated values of Seltzer and Berger. The angular dependence is based
 * on the work of Acosta et al.
 * </p>
 * <ul>
 * <li>Acosta E, Llovet X, Salvat F, Appl. Phys. Lett., <b>80</b>, 17, 2002 pp
 * 3228
 * <li>Seltzer S, Berger M, Atom. Data and Nucl Data Tables, <b>35</b>, 1986 pp
 * 345-418
 * </ul>
 * <p>
 * The tabulated cross sections while derived from Seltzer &amp; Berger's 1986
 * article were extracted from Penelope by Salvat, Fernandex-Varea &amp; Sempau.
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

public class Bremsstrahlung {

   /**
    * beta - Calculate beta=v/c for an electron of the specified energy in
    * Joules.
    * 
    * @param energy double - in Joules
    * @return double - Unitless
    */
   public final static double beta(double energy) {
      final double g2 = Math2.sqr((REST_MASS_E + energy) / REST_MASS_E);
      return Math.sqrt((g2 - 1.0) / g2);
   }

   /**
    * parse - To account for the screwy + sign in the exponent.
    * 
    * @param s String
    * @return double
    */
   private static double parse(String s) {
      final int p = s.indexOf("+");
      return p == -1 ? Double.parseDouble(s) : Double.parseDouble(s.substring(0, p) + s.substring(p + 1));
   }

   // energyIndex(energy) is the index of the mEnergy at or below energy
   final private int energyIndex(double energy) {
      int ie = Arrays.binarySearch(mEnergy, energy);
      if(ie < 0)
         ie = Math.max(-(ie + 2), 0);
      assert ie >= 0 : Integer.toString(ie);
      assert ie < mEnergy.length : Integer.toString(ie);
      assert (ie == 0) || (energy >= mEnergy[ie]) : Double.toString(energy) + " not >= " + Double.toString(mEnergy[ie]);
      assert energy < mEnergy[ie + 1] : Double.toString(energy) + " not < " + Double.toString(mEnergy[ie]);
      return ie;
   }

   // kIndex(k) is the index of the mKoT at or below k
   final private int kIndex(double k) {
      int ik = Math.abs(Arrays.binarySearch(mKoT, k));
      if(ik < 0)
         ik = Math.max(-(ik + 2), 0);
      assert ik >= 0 : Integer.toString(ik);
      assert ik < mKoT.length : Integer.toString(ik);
      assert (ik == 0) || (k >= mKoT[ik]);
      assert k < mKoT[ik + 1];
      return ik;
   }

   private static final double REST_MASS_E = PhysicalConstants.ElectronMass * PhysicalConstants.SpeedOfLight
         * PhysicalConstants.SpeedOfLight;
   private static final double milliBARN = ToSI.barn(1.0e-3);
   private static final int NUM_ENERGIES = 25; // Up to 1.0 MeV
   private static final int NUM_DCS = 32;
   private static final int MIN_PHOTON_K = 0;
   private final Element mElement;
   private final double[] mEnergy = new double[NUM_ENERGIES];
   private final double[] mEnergyLoss = new double[NUM_ENERGIES];
   private final double[][] mIntXSec = new double[NUM_ENERGIES][NUM_DCS];

   // Note: the mKoT[0] was changed from 0.00000 to 0.001 to eliminate a the
   // integral divergence at 0.0
   private static final double[] mKoT = {
      0.001000, // 1.0e-6,
      0.05000,
      0.07500,
      0.10000,
      0.12500,
      0.15000,
      0.20000,
      0.25000,
      0.30000,
      0.35000,
      0.40000,
      0.45000,
      0.50000,
      0.55000,
      0.60000,
      0.65000,
      0.70000,
      0.75000,
      0.80000,
      0.85000,
      0.90000,
      0.92500,
      0.95000,
      0.97000,
      0.99000,
      0.99500,
      0.99900,
      0.99950,
      0.99990,
      0.99995,
      0.99999,
      1.00000
   };

   private final void readTable() {
      try {
         final int z = Math2.bound(mElement.getAtomicNumber(), 1, 93);
         final String name = "BergerSeltzerBrem/pdebr" + (z < 10 ? '0' + Integer.toString(z) : Integer.toString(z)) + ".tab";
         try (final BufferedReader br = new BufferedReader(new InputStreamReader(Bremsstrahlung.class.getResourceAsStream(name), "US-ASCII"))) {
            final String zz = br.readLine();
            if((zz == null) || (Integer.parseInt(zz.trim()) != z))
               throw new Exception();
            final double[] xSec = new double[NUM_DCS];
            final double[] intXSec = new double[NUM_DCS];
            for(int e = 0; e < NUM_ENERGIES; ++e) {
               for(int r = 0; r < 7; ++r) {
                  final String s = br.readLine();
                  if(s != null) {
                     if(r == 0)
                        mEnergy[e] = ToSI.eV(parse(s.substring(1, 9)));
                     final int off = 5 * r;
                     xSec[off] = parse(s.substring(10, 21));
                     xSec[off + 1] = parse(s.substring(22, 33));
                     if(r < 6) {
                        xSec[off + 2] = parse(s.substring(34, 45));
                        xSec[off + 3] = parse(s.substring(46, 57));
                        xSec[off + 4] = parse(s.substring(58, 69));
                     } else {
                        final double phiRad = parse(s.substring(70, 79)); // dimensionless
                        // (see Eq 5 of B&S 1986) Scaled to SI units
                        mEnergyLoss[e] = phiRad * PhysicalConstants.FineStructure
                              * Math2.sqr(PhysicalConstants.ClassicalElectronRadius * z)
                              * (mEnergy[e] + PhysicalConstants.ElectronRestMass);
                     }
                  }
               }
               final double x = milliBARN * Math2.sqr(z / beta(mEnergy[e]));
               // Integrate the xSec/k over each interval and tabulate
               // it...
               double total = 0.0;
               // Note: The integrals dependence on T (in k/T) cancels
               for(int j = 0; j < (NUM_DCS - 1); ++j) {
                  final double dk = mKoT[j + 1] - mKoT[j];
                  final double dx = xSec[j + 1] - xSec[j];
                  final double ss = dx / dk;
                  intXSec[j] = x * (((xSec[j] - (ss * mKoT[j])) * Math.log(mKoT[j + 1] / mKoT[j])) + (ss * dk));
                  assert intXSec[j] > 0.0 : intXSec[j];
                  total += intXSec[j];
               }
               // accumulate starting at the highest k
               mIntXSec[e][NUM_DCS - 1] = 0.0; // Highest energy
               for(int i = NUM_DCS - 2; i >= 0; --i)
                  mIntXSec[e][i] = mIntXSec[e][i + 1] + intXSec[i];
               assert Math.abs(mIntXSec[e][0] - total) < (1.0e-8 * total) : Double.toString(mIntXSec[e][0]) + "\t"
                     + Double.toString(total);
            }
         }
      }
      catch(final Exception ex) {
         ex.printStackTrace();
         throw new EPQFatalException("Bremsstrahlung data unavailable for " + mElement + ".");
      }
   }

   public Bremsstrahlung(Element el) {
      mElement = el;
      readTable();
   }

   /**
    * partialSigma - The partial cross section as a function of electron and
    * photon energies. The cross section for all photons from energy down to k.
    * Interpolate (or occasionally extrapolate) from the integrated Berger and
    * Seltzer scaled Differential Cross Section.
    * 
    * @param energy double - The kinetic energy of the electron energy
    * @param w double - The energy of the photon (e&lt;=energy)
    * @return double - in square meters
    */
   public double partialSigma(double energy, double w) {
      assert (energy > 0.0);
      assert (energy < ToSI.keV(1000.0));
      final double k = w / energy;
      assert k >= 0.0 : Double.toString(k);
      assert k <= 1.0 : Double.toString(k);
      // ie is the index of the energy at or above mEnergy
      final int ie = energyIndex(energy);
      assert (ie == 0) || (energy >= mEnergy[ie]);
      assert energy < mEnergy[ie + 1];
      final double de = (energy - mEnergy[ie]) / (mEnergy[ie + 1] - mEnergy[ie]);
      if(k <= mKoT[MIN_PHOTON_K])
         return mIntXSec[ie][MIN_PHOTON_K] + (de * (mIntXSec[ie + 1][MIN_PHOTON_K] - mIntXSec[ie][MIN_PHOTON_K]));
      else {
         // ik is the index of the mKoT at or above k
         final int ik = kIndex(k);
         assert (ik == 0) || (k >= mKoT[ik]);
         assert k < mKoT[ik + 1];
         final double dk = (k - mKoT[ik]) / (mKoT[ik + 1] - mKoT[ik]);
         // Thanks to Chris Walker to pointing out an error in earlier versions
         // - 5-Sept-2006
         final double se0 = mIntXSec[ie][ik] + (dk * (mIntXSec[ie][ik + 1] - mIntXSec[ie][ik]));
         final double se1 = mIntXSec[ie + 1][ik] + (dk * (mIntXSec[ie + 1][ik + 1] - mIntXSec[ie + 1][ik]));
         return se0 + (de * (se1 - se0));
      }
   }

   /**
    * sigma - The full cross section for bremsstrahlung emission from the
    * electron energy down to 0.05*energy
    * 
    * @param energy double
    * @return The total Bremsstrahlung cross section per atom for an electron of
    *         the specified energy traveling one meter.
    */
   public double sigma(double energy) {
      final int ie = energyIndex(energy);
      assert (ie == 0) || (energy >= mEnergy[ie]);
      assert energy < mEnergy[ie + 1];
      final double de = (energy - mEnergy[ie]) / (mEnergy[ie + 1] - mEnergy[ie]);
      return mIntXSec[ie][MIN_PHOTON_K] + ((mIntXSec[ie + 1][MIN_PHOTON_K] - mIntXSec[ie][MIN_PHOTON_K]) * de);
   }

   /**
    * getMinXRayEnergy - The integral over all energies diverges. However if we
    * select a minimum energy then the integral will converge. getMinK returns
    * the selected minimum photon energy for the specified electron energy.
    * 
    * @param energy double
    * @return double
    */
   public double getMinXRayEnergy(double energy) {
      return energy * mKoT[MIN_PHOTON_K];
   }

   // r is a random number on [0.0,1.0)
   private double findKoT(double r, int ie) {
      assert (r >= 0);
      assert (r < 1.0);
      final double[] xSec = mIntXSec[ie];
      final double intXSec = r * xSec[MIN_PHOTON_K]; // Goal
      for(int i = 1; i < NUM_DCS; ++i)
         if(intXSec > xSec[i]) {
            assert intXSec <= xSec[i - 1] : Double.toString(intXSec) + "\t" + Double.toString(xSec[i - 1]);
            // Interpolate in log(k) space
            final double k = Math.exp(Math.log(mKoT[i - 1])
                  + (((intXSec - xSec[i - 1]) / (xSec[i] - xSec[i - 1])) * Math.log(mKoT[i] / mKoT[i - 1])));
            assert k >= mKoT[i - 1] : Double.toString(k) + " not >= " + Double.toString(mKoT[i - 1]);
            assert k < mKoT[i] : Double.toString(k) + " not < " + Double.toString(mKoT[i]);
            return k;
         }
      return 0.0;
   }

   /**
    * getRandomizedEvent - Given a random number between 0 and 1, this function
    * generates a photon energy in a distribution such that the events will be
    * distributed like Bremsstrahlung.
    * 
    * @param energy double - In Joules
    * @param r double - A number on the interval [0,1)
    * @return double
    */
   public double getRandomizedEvent(double energy, double r) {
      final int ie = energyIndex(energy);
      assert (ie == 0) || (energy >= mEnergy[ie]);
      assert energy < mEnergy[ie + 1];
      final double de = (energy - mEnergy[ie]) / (mEnergy[ie + 1] - mEnergy[ie]);
      final double k0 = findKoT(r, ie);
      final double k1 = findKoT(r, ie + 1);
      return energy * (k0 + ((k1 - k0) * de));
   }

   /**
    * totalEnergyLossCrossSection - The total integrated radiative energy-loss
    * cross section
    * 
    * @param energy double - The electron energy in Joules
    * @return double - (Joule)(square meters)/(atom)
    */
   public double totalEnergyLossCrossSection(double energy) {
      assert (energy > 0.0);
      assert (energy < ToSI.keV(1000.0));
      final int ie = energyIndex(energy);
      assert (ie == 0) || (energy >= mEnergy[ie]);
      assert energy < mEnergy[ie + 1];
      final double de = (energy - mEnergy[ie]) / (mEnergy[ie + 1] - mEnergy[ie]);
      return mEnergyLoss[ie] + (de * (mEnergyLoss[ie + 1] - mEnergyLoss[ie]));
   }
}
