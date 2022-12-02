/**
 * Provides methods for testing models that extend the ScatterMechanim abstract
 * class.
 *
 * @author jvillar
 */
package gov.nist.nanoscalemetrology.MONSELtests;

import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism;

/**
 *
 *
 */
public class ScatterMechanismTest {

   ScatterMechanism sm; // An instance of the scatter mechanism to be tested

   double srtime = -1.; // Average execution time of calls to scatterRate

   // method
   double scattertime = -1.; // Average execution time of a scatter() call

   /**
    *
    */
   public ScatterMechanismTest(ScatterMechanism sm) {
      super();
      this.sm = sm;
   }

   /**
    * Tests the ScatterMechanism's scatterRate method by returning an array of
    * scatter rates for various energies in the range specified by the input
    * arguments. This can be compared to independent calculation.
    *
    * @param minE
    *           -- double, the minimum energy at which to compute
    * @param maxE
    *           -- double, the maximum energy at which to compute
    * @param n
    *           -- int, the number of intervals into which the energy is to be
    *           divided
    * @return -- an array double[n] of scatter rates at the corresponding
    *         energies
    */
   public double[] testScatterRate(double minE, double maxE, int n) {
      return testScatterRate(minE, maxE, n, 1);
   }

   /**
    * Tests the ScatterMechanism's scatterRate method by returning an array of
    * scatter rates for various energies in the range specified by the input
    * arguments. This can be compared to independent calculation.
    *
    * @param minE
    *           -- double, the minimum energy at which to compute
    * @param maxE
    *           -- double, the maximum energy at which to compute
    * @param n
    *           -- int, the number of intervals into which the energy is to be
    *           divided
    * @param repeats
    *           -- int, the number of times to repeat the n intervals
    *           calculation for timing purposes. Default = 1.
    * @return -- an array double[n] of scatter rates at the corresponding
    *         energies
    */
   public double[] testScatterRate(double minE, double maxE, int n, int repeats) {

      long t0, tf, tint;

      final double[] sr = new double[n];
      final Electron el = new Electron(new double[]{0., 0., 0.}, 0.);

      final double inc = (maxE - minE) / (n - 1);

      t0 = System.currentTimeMillis(); // Start the "stopwatch"
      for (int j = 0; j < repeats; j++)
         for (int i = 0; i < n; i++) {
            el.setEnergy(minE + (i * inc)); // Set electron energy to test value
            sr[i] = sm.scatterRate(el); // Load output of scatterRate method
         }
      tint = System.currentTimeMillis();

      // Run a null loop to subtract loop overhead time
      for (int j = 0; j < repeats; j++)
         for (int i = 0; i < n; i++)

            el.setEnergy(minE + (i * inc)); // Set electron energy to test value
      tf = System.currentTimeMillis();

      srtime = (1000. * ((2. * tint) - tf - t0)) / n / repeats;

      return sr;
   }

   /**
    * Returns average microseconds per scatterRate call in most recent
    * testScatterRate result
    */
   public double getScatterRateTime() {
      return srtime;
   }

   /**
    * Constructs histograms of SE energies resulting from calls to the scatter
    * method.
    *
    * @param n
    *           -- long, the number of scatter events to simulate
    * @param kE
    *           -- double, the PE kinetic energy
    * @param minE
    *           -- double, the energy of the minimum bin in the histogram
    * @param maxE
    *           -- double, the energy of the maximum bin in the histogram
    * @param nbins
    *           -- int, the number of bins in the histogram
    * @return -- an array long[nbins] of counts at the correponding energies
    */
   public long[] SE_kE_Histogram(long n, double kE, double minE, double maxE, int nbins) {

      long t0, tf, tint;

      final long[] hist = new long[nbins];
      final Electron el = new Electron(new double[]{0., 0., 0.}, kE);
      Electron se;
      final Electron se2 = new Electron(new double[]{0., 0., 0.}, kE);
      final double binsize = (maxE - minE) / (nbins - 1);
      int binnum;
      @SuppressWarnings("unused")
      long total = 0;

      t0 = System.currentTimeMillis(); // Start the "stopwatch"
      for (long i = 0; i < (n - 1); i++) {
         se = sm.scatter(el); // Do the scatter & get a SE
         if (se != null) {
            // Determine bin number and increment it
            binnum = (int) ((se.getEnergy() - minE) / binsize);
            if ((binnum >= 0) && (binnum < nbins))
               hist[binnum]++;
         }
         el.setEnergy(kE); // Restore el energy
      }
      tint = System.currentTimeMillis();
      for (long i = 0; i < (n - 1); i++) {
         if (se2 != null) {
            // Determine bin number and increment it
            binnum = (int) ((se2.getEnergy() - minE) / binsize);
            if ((binnum >= 0) && (binnum < nbins))
               total++;
         }
         el.setEnergy(kE); // Restore el energy
      }
      tf = System.currentTimeMillis();

      /*
       * This calculation might not be appropriate if the scatter method only
       * sometimes generates an SE--but I don't know of any such models.
       */

      scattertime = (1000. * ((2. * tint) - tf - t0)) / n;

      return hist;
   }

   public double getScatterTime() {
      return scattertime;
   }

   /**
    * Constructs histograms of PE angles resulting from calls to the scatter
    * method.
    *
    * @param n
    *           -- long, the number of scatter events to simulate
    * @param kE
    *           -- double, the PE kinetic energy
    * @param minTheta
    *           -- double, the angle of the minimum bin in the histogram
    *           (radians)
    * @param maxTheta
    *           -- double, the angle of the maximum bin in the histogram
    *           (radians)
    * @param nbins
    *           -- int, the number of bins in the histogram
    * @return -- an array long[nbins] of counts at the correponding angles
    */
   public long[] PE_Theta_Histogram(long n, double kE, double minTheta, double maxTheta, int nbins) {

      long t0, tf, tint;

      final long[] hist = new long[nbins];
      final Electron el = new Electron(new double[]{0., 0., 0.}, kE);
      @SuppressWarnings("unused")
      Electron se;

      final double binsize = (maxTheta - minTheta) / (nbins - 1);
      int binnum;
      @SuppressWarnings("unused")
      long total = 0;

      t0 = System.currentTimeMillis(); // Start the "stopwatch"
      for (long i = 0; i < (n - 1); i++) {
         se = sm.scatter(el); // Do the scatter & get a SE
         binnum = (int) ((el.getTheta() - minTheta) / binsize);
         if ((binnum >= 0) && (binnum < nbins))
            hist[binnum]++;
         el.setDirection(0., 0.); // Reset direction before next sim
      }
      tint = System.currentTimeMillis();
      for (long i = 0; i < (n - 1); i++) {
         // Determine bin number and increment it
         binnum = (int) ((el.getTheta() - minTheta) / binsize);
         if ((binnum >= 0) && (binnum < nbins))
            total++;
         el.setDirection(0., 0.);
      }
      tf = System.currentTimeMillis();

      /*
       * This calculation might not be appropriate if the scatter method only
       * sometimes generates an SE--but I don't know of any such models.
       */

      scattertime = (1000. * ((2. * tint) - tf - t0)) / n;

      return hist;
   }

}
