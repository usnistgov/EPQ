package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

/**
 * The ionization depth distribution ratio is used in the calculation of the
 * fluorescence.
 * 
 * @author ppinard
 */
public abstract class IonizationDepthRatio extends AlgorithmClass {

   private static final AlgorithmClass[] mAllImplementations = {IonizationDepthRatio.Castaing1951, IonizationDepthRatio.ReedLong1963,
         IonizationDepthRatio.Reed1990, IonizationDepthRatio.Armstrong1988};

   protected IonizationDepthRatio(String name, LitReference ref) {
      super("Ionization depth ratio", name, ref);
   }

   protected IonizationDepthRatio(String name, String ref) {
      super("Ionization depth ratio", name, ref);
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
    * Returns the ionization depth distribution ratio between two x-rays lines.
    * 
    * @param primary
    *           primary x-ray B
    * @param secondary
    *           secondary x-ray A
    * @param e0
    *           beam energy in Joules
    * @return ionization depth distribution ratio
    */
   public abstract double compute(XRayTransition primary, XRayTransition secondary, double e0);

   public static final class Castaing1951IonizationDepthRatio extends IonizationDepthRatio {

      private static final LitReference reference = new LitReference.Book("Ph.D. thesis", "Univ. Paris", 1951,
            new LitReference.Author[]{LitReference.RCastaing});

      protected Castaing1951IonizationDepthRatio() {
         super("Castaing 1951", reference);
      }

      @Override
      public double compute(XRayTransition primary, XRayTransition secondary, double e0) {
         return secondary.getEdgeEnergy() / primary.getEdgeEnergy();
      }

   }

   public static final IonizationDepthRatio Castaing1951 = new Castaing1951IonizationDepthRatio();

   public static final class ReedLong1963IonizationDepthRatio extends IonizationDepthRatio {

      private static final LitReference reference = new LitReference.CrudeReference("Reed S.J.B. and Long J.V.P. (1963) ICXOM 3, p.317");

      protected ReedLong1963IonizationDepthRatio() {
         super("Reed & Long 1963", reference);
      }

      @Override
      public double compute(XRayTransition primary, XRayTransition secondary, double e0) {
         final double uA = e0 / secondary.getEdgeEnergy();
         final double uB = e0 / primary.getEdgeEnergy();
         return Math.pow((uB - 1.0) / (uA - 1.0), 1.67);
      }

   }

   public static final IonizationDepthRatio ReedLong1963 = new ReedLong1963IonizationDepthRatio();

   public static final class Reed1990IonizationDepthRatio extends IonizationDepthRatio {

      private static final LitReference reference = new LitReference.CrudeReference("Reed S.J.B. (1990) Microbeam Analysis, p.109");

      protected Reed1990IonizationDepthRatio() {
         super("Reed 1990", reference);
      }

      @Override
      public double compute(XRayTransition primary, XRayTransition secondary, double e0) {
         final double uA = e0 / secondary.getEdgeEnergy();
         final double uB = e0 / primary.getEdgeEnergy();
         return ((((uB * Math.log(uB)) - uB) + 1.0) / (((uA * Math.log(uA)) - uA) + 1.0));
      }

   }

   public static final IonizationDepthRatio Reed1990 = new Reed1990IonizationDepthRatio();

   public static final class Armstrong1988IonizationDepthRatio extends IonizationDepthRatio {

      private static final LitReference reference = new LitReference.CrudeReference("Armstrong J.T. (1988) Microbeam Analysis, p.239-246");

      protected Armstrong1988IonizationDepthRatio() {
         super("Armstrong 1988", reference);
      }

      @Override
      public double compute(XRayTransition primary, XRayTransition secondary, double e0) {
         final double uA = e0 / secondary.getEdgeEnergy();
         final double uB = e0 / primary.getEdgeEnergy();
         final double temp = (uB - 1) / (uA - 1);
         if (temp < 2 / 3)
            return Math.pow(temp, 1.59);
         else
            return 1.87 * Math.pow(temp, 3.19);
      }

   }

   public static final IonizationDepthRatio Armstrong1988 = new Armstrong1988IonizationDepthRatio();
}
