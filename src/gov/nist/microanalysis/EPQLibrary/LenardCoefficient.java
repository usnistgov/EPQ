package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Compute Lenard coefficient.
 * </p>
 * <p>
 * From Reed, Electron microprobe analysis, 2nd edition, 1993, Cambridge
 * University Press, p. 199:
 * </p>
 * <p>
 * The transmission of electrons through thin films may be represented
 * approximately by an exponential function:
 * </p>
 * <p>
 * i = i<sub>0</sub> exp (- &sigma;&rho;x)
 * </p>
 * <p>
 * where i and i_0 are the transmitted and incident currents respectively, x is
 * the film thickness and sigma is the Lenard cofficient, which is analogous to
 * the mass attenuation coefficient for X-rays.
 * </p>
 * The Lenard coefficient [...] represents the depth distribution of X-ray
 * production.
 * 
 * @author ppinard
 */
public abstract class LenardCoefficient extends AlgorithmClass {

   private static final AlgorithmClass[] mAllImplementations = {LenardCoefficient.DuncumbShields, LenardCoefficient.Heinrich,
         LenardCoefficient.Citzaf,};

   protected LenardCoefficient(String name, LitReference reference) {
      super("Lenard coefficient", name, reference);
   }

   protected LenardCoefficient(String name, String reference) {
      super("Lenard coefficient", name, reference);
   }

   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   @Override
   protected void initializeDefaultStrategy() {
      // Don't do anything...
   }

   /**
    * Returns the Lenard coefficient.
    * 
    * @param e0
    *           beam energy in Joules
    * @param xray
    *           excited x-ray
    * @return Lenard coefficient
    */
   public abstract double compute(double e0, XRayTransition xray);

   public static class DuncumbShieldsLenardCoefficient extends LenardCoefficient {

      private static final LitReference reference = new LitReference.BookChapter(LitReference.ElectronMicroprobe, "284",
            new LitReference.Author[]{LitReference.PDuncumb, new LitReference.Author("P.K.", "Shields)")});

      protected DuncumbShieldsLenardCoefficient() {
         super("Duncumb & Shields 1966", reference);
      }

      @Override
      public double compute(double e0, XRayTransition xray) {
         double ee = xray.getEdgeEnergy();
         return 2.39e5 / (Math.pow(FromSI.keV(e0), 1.5) - Math.pow(FromSI.keV(ee), 1.5));
      }
   }

   public static final LenardCoefficient DuncumbShields = new DuncumbShieldsLenardCoefficient();

   public static class HeinrichLenardCoefficient extends LenardCoefficient {

      private static final LitReference reference = new LitReference.CrudeReference("Heinrich K. F. J. (1967) EPASA 2, paper no. 7");

      protected HeinrichLenardCoefficient() {
         super("Heinrich 1967", reference);
      }

      @Override
      public double compute(double e0, XRayTransition xray) {
         double ee = xray.getEdgeEnergy();
         return 4.5e5 / (Math.pow(FromSI.keV(e0), 1.65) - Math.pow(FromSI.keV(ee), 1.65));
      }
   }

   public static final LenardCoefficient Heinrich = new HeinrichLenardCoefficient();

   public static class CitzafLenardCoefficient extends LenardCoefficient {

      protected CitzafLenardCoefficient() {
         super("CITZAF", "Taken from Probe for EPMA's implementation of CITZAF");
      }

      @Override
      public double compute(double e0, XRayTransition xray) {
         double ee = xray.getEdgeEnergy();
         return 333000 / (Math.pow(FromSI.keV(e0), 1.65) - Math.pow(FromSI.keV(ee), 1.65));
      }
   }

   public static final LenardCoefficient Citzaf = new CitzafLenardCoefficient();
}
