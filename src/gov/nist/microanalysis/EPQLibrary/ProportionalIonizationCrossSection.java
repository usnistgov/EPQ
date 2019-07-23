package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

abstract public class ProportionalIonizationCrossSection
   extends IonizationCrossSection {
   private ProportionalIonizationCrossSection(String name, String ref) {
      super(name, ref);
   }

   private ProportionalIonizationCrossSection(String name, LitReference ref) {
      super(name, ref);
   }

   /**
    * The proportional ionization cross section descibed in Pochou &amp;
    * Pichoir's IXCOM 11 (1986) article. An additional factor is inserted to
    * account for the variation between different shells within the family.
    */
   static public class Pouchou86ICX
      extends ProportionalIonizationCrossSection {

      protected Pouchou86ICX() {
         super("Pouchou & Pichoir 1986", "Pochou & Pichoir in the proceedings from IXCOM 11 (1986)");
      }

      @Override
      public double computeShell(AtomicShell shell, double beamE) {
         return shellDependence(shell) * computeFamily(shell, beamE);
      }

      static public double computeExponent(AtomicShell shell) {
         // From PAP1991, agrees with Scott, Love &amp; Reed
         double m;
         final double za = shell.getElement().getAtomicNumber();
         switch(shell.getFamily()) {
            case AtomicShell.KFamily:
               m = 0.86 + (0.12 * Math.exp((-za * za) / 25.0));
               break;
            case AtomicShell.LFamily:
               m = 0.82;
               break;
            case AtomicShell.MFamily:
               m = 0.78;
               break;
            default:
               throw new EPQFatalException("Unsupported shell (" + shell.toString()
                     + ") in the the Pouchou & Pichoir IXCOM-11 ionization cross section.");
         }
         return m;
      }

      @Override
      public double computeFamily(AtomicShell shell, double beamE) {
         final double eCrit = FromSI.keV(shell.getEdgeEnergy());
         final double u = FromSI.keV(beamE) / eCrit;
         if(u <= 1.0)
            return 0.0;
         return Math.log(u) / ((eCrit * eCrit) * Math.pow(u, computeExponent(shell)));
      }
   };

   /**
    * The proportional ionization cross section detailed in Bastin, Dijkstra and
    * Heijliger's PROZA96 algorithm (X-Ray Spec. 27, pp 3-10 (1998)) An
    * additional factor is inserted to account for the variation between
    * different shells within the family.
    */
   static public class Proza96ICX
      extends ProportionalIonizationCrossSection {

      protected Proza96ICX() {
         super("Dijkstra and Heijliger 1998", LitReference.Proza96);
      }

      @Override
      public double computeShell(AtomicShell shell, double beamE) {
         return shellDependence(shell) * computeFamily(shell, beamE);
      }

      static public double computeExponent(AtomicShell shell) {
         double m;
         switch(shell.getFamily()) {
            case AtomicShell.KFamily: {
               // PROZA96 makes special cases out of the following elements
               switch(shell.getElement().getAtomicNumber()) {
                  case Element.elmC:
                     m = 0.888;
                     break;
                  case Element.elmN:
                     m = 0.86;
                     break;
                  case Element.elmO:
                     m = 0.89;
                     break;
                  default:
                     m = 0.90;
                     break;
               }
               break;
            }
            case AtomicShell.LFamily:
               m = 0.82;
               break;
            case AtomicShell.MFamily:
               m = 0.78;
               break;
            default:
               assert (false);
               throw new EPQFatalException("Unsupported line family in Proza96 constructor.");
         }
         return m;
      }

      @Override
      public double computeFamily(AtomicShell shell, double beamE) {
         final double eCrit = FromSI.keV(shell.getEdgeEnergy());
         final double u = FromSI.keV(beamE) / eCrit;
         if(u <= 1.0)
            return 0.0;
         return Math.log(u) / ((eCrit * eCrit) * Math.pow(u, computeExponent(shell)));
      }
   };

   /**
    * getAllImplementations - returns a list of all implementations of
    * IonizationCrossSection
    * 
    * @return List
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   private static final AlgorithmClass[] mAllImplementations = {
      ProportionalIonizationCrossSection.Pouchou86,
      ProportionalIonizationCrossSection.Proza96,
   };

   /**
    * Pouchou86 - The proportional ionization cross section descibed in Pochou
    * &amp; Pichoir's IXCOM 11 (1986) article. An additional factor is inserted
    * to account for the variation between different shells within the family.
    */
   public static final Pouchou86ICX Pouchou86 = new Pouchou86ICX();

   /**
    * Proza96 - The proportional ionization cross section detailed in Bastin,
    * Dijkstra and Heijliger's PROZA96 algorithm (X-Ray Spec. 27, pp 3-10
    * (1998)) An additional factor is inserted to account for the variation
    * between different shells within the family.
    */
   public static final Proza96ICX Proza96 = new Proza96ICX();

}
