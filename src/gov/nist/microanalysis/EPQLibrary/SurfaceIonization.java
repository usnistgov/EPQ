package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * An algorithm class that defines the implementation for various different
 * style implementations of the surface ionization.
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

abstract public class SurfaceIonization extends AlgorithmClass {

   @Override
   protected void initializeDefaultStrategy() {
      // Don't do anything...
   }

   private SurfaceIonization(String name, String ref) {
      super("Surface Ionization", name, ref);
   }

   /**
    * getAllImplementations - Returns a full list of all available algorithms.
    * Each item is an implements the SurfaceIonization class.
    * 
    * @return List
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   /**
    * compute - Compute phi(0), the surface ionization potential,for an electron
    * of energy e0 (Joules) impinging on the Composition comp, ionizing the
    * AtomicShell shell.
    * 
    * @param comp
    *           Composition
    * @param shell
    *           AtomicShell
    * @param e0
    *           double - In Joules
    * @return double
    */
   abstract public double compute(Composition comp, AtomicShell shell, double e0);

   /**
    * caveat - The caveat method provides a mechanism for identifying the
    * limitations of a specific implementation of this algorithm based on the
    * arguments to the compute method. The result is a user friendly string
    * itemizing the limitations of the algorithm as known by the library author.
    * 
    * @param comp
    *           Composition
    * @param shell
    *           AtomicShell
    * @param e0
    *           double - In Joules
    * @return String
    */
   public String caveat(Composition comp, AtomicShell shell, double e0) {
      return CaveatBase.None;
   }

   /**
    * Pouchou1991 - Pouchou &amp; Pichoir in Electron Probe Quantitation,
    * Newbury &amp; Heinrich (eds), Plenum (1991)
    */
   public static class Pouchou1991SurfaceIonization extends SurfaceIonization {
      public Pouchou1991SurfaceIonization() {
         super("Pouchou & Pichoir 1991", "Pouchou & Pichoir in Electron Probe Quantitation, Newbury & Heinrich (eds), Plenum (1991)");
      }

      @Override
      protected void initializeDefaultStrategy() {
         addDefaultAlgorithm(BackscatterCoefficient.class, BackscatterCoefficient.PouchouAndPichoir91);
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         final BackscatterCoefficient bc = (BackscatterCoefficient) getAlgorithm(BackscatterCoefficient.class);
         final double u0 = e0 / shell.getEdgeEnergy();
         // no potential for unit ambiguities here...
         // (Only Z and u dependencies)
         final double eta = bc.compute(comp, e0);
         final double r = 2.0 - (2.3 * eta);
         return 1.0 + (3.3 * (1.0 - (1.0 / Math.pow(u0, r))) * Math.pow(eta, 1.2));
      }
   }

   static public final SurfaceIonization Pouchou1991 = new Pouchou1991SurfaceIonization();

   /**
    * Bastin1998 - Bastin GF, Dijkstra JM and Heijligers HJM, X-Ray
    * Spectrometry, Vol 27, 3-10 (1998)
    */
   static public class Bastin1998SurfaceIonization extends SurfaceIonization {
      public Bastin1998SurfaceIonization() {
         super("Bastin, Dijkstra & Heijligers 1998 (Proza96)", "Bastin GF, Dijkstra JM and Heijligers HJM, X-Ray Spectrometry, Vol 27, 3-10 (1998)");
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         final double u0 = e0 / shell.getEdgeEnergy();
         final double meanZ = comp.weightAvgAtomicNumber();
         final double a = 0.61747243 + (1.0991805e-3 * meanZ) + (1.224221 / Math.sqrt(meanZ));
         final double b = (-0.21964478) + ((0.11332964 - (2.0638629e-2 * Math.log(meanZ))) * meanZ);
         return 1.0 + (b * Math.pow(1.0 - (1.0 / Math.sqrt(u0)), a));
      }
   }

   static public final SurfaceIonization Bastin1998 = new Bastin1998SurfaceIonization();

   /**
    * Reuter1972 - Reuter, W., Proc. 6th Int. Conf. on X-Ray Optics and
    * Microanalysis 1971, University of Tokyo Press 121 (1972)
    */
   public static class Reuter1972SurfaceIonization extends SurfaceIonization {
      public Reuter1972SurfaceIonization() {
         super("Reuter 1972", "Reuter, W., Proc. 6th Int. Conf. on X-Ray Optics and Microanalysis 1971, University of Tokyo Press 121 (1972)");
      }

      @Override
      public String caveat(Composition comp, AtomicShell shell, double e0) {
         return CaveatBase.None;
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         final double u0 = e0 / shell.getEdgeEnergy();
         final double eta = BackscatterCoefficient.PouchouAndPichoir91.compute(comp, e0);
         return 1.0 + (2.8 * (1.0 - (0.9 / u0)) * eta);
      }
   }

   static public final SurfaceIonization Reuter1972 = new Reuter1972SurfaceIonization();

   /**
    * Love1978 - Love, G., The surface ionisation function fi(0) derived derived
    * using a Monte Carlo method, J. Phys. D. (1978)
    */
   public static class Love1978SurfaceIonization extends SurfaceIonization {
      public Love1978SurfaceIonization() {
         super("Love 1978", "Love, G., The surface ionisation function fi(0) derived using a Monte Carlo method, J. Phys. D. (1978)");
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         final double u0_inv = shell.getEdgeEnergy() / e0;
         final double eta = BackscatterCoefficient.PouchouAndPichoir91.compute(comp, e0);
         final double a = 3.43378 + ((-10.7872 + ((10.97628 - (3.62286 * u0_inv)) * u0_inv)) * u0_inv);
         final double b = -0.59299 + ((21.55329 + ((-30.55428 + (9.59218 * u0_inv)) * u0_inv)) * u0_inv);
         return 1.0 + ((eta / (1.0 + eta)) * (a + (b * Math.log(1.0 + eta))));
      }
   }

   /**
    * Love1978 - Love, G., The surface ionisation function fi(0) derived derived
    * using a Monte Carlo method, J. Phys. D. (1978)
    */
   public static class Love1978CitzafSurfaceIonization extends SurfaceIonization {
      public Love1978CitzafSurfaceIonization() {
         super("Love 1978", "Love, G., J. Phys. D. (1978) as in CITZAF 3.06");
      }

      @Override
      protected void initializeDefaultStrategy() {
         addDefaultAlgorithm(BackscatterCoefficient.class, BackscatterCoefficient.Love1978);
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         final BackscatterCoefficient bsc = (BackscatterCoefficient) getAlgorithm(BackscatterCoefficient.class);
         final double u0_inv = shell.getEdgeEnergy() / e0;
         final double eta = bsc.compute(comp, e0);
         final double jpu = 3.43378 + (u0_inv * (-10.7872 + (u0_inv * (10.97628 + (u0_inv * -3.62286)))));
         final double gpu = -0.59299 + (u0_inv * (21.55329 + (u0_inv * (-30.55428 + (u0_inv * 9.59218)))));
         return 1.0 + ((eta / (1.0 + eta)) * (jpu + (gpu * Math.log(1.0 + eta))));
      }
   }

   static public final SurfaceIonization Love1978 = new Love1978SurfaceIonization();
   static public final SurfaceIonization Love1978Citzaf = new Love1978CitzafSurfaceIonization();

   // TODO: Implement Rehbach &amp; Karduck - Rehbach W &amp; Karduck P,
   // Microbeam
   // Anal.
   // 285 (1988)

   static private final AlgorithmClass[] mAllImplementations = {SurfaceIonization.Bastin1998, SurfaceIonization.Pouchou1991,
         SurfaceIonization.Reuter1972};

}
