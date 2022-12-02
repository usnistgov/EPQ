/**
 * <p>
 * This class defines and implements various different mechanisms for computing
 * the jump ratio, the ratio of the the pre-edge and post-edge mass absorption
 * coefficients. The absorption edge is the minimum photon energy necessary to
 * ionize the specified shell. Photons below this energy will not be absorbed
 * due to ionization this shell and thus are typically a few times less likely
 * to be absorbed that photons of this energy and above.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas
 * @version 1.0
 */
package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * JumpRatio class - Various different implementations of the methods to compute
 * the jump ratio - the ratio of the mass absorption coefficent above and below
 * an absorption edge. In each family, it seems that the highest available edge
 * (LIII nominally in the L family, MV nominally in the M family) has the
 * highest jump ratio. The jump ratio for the other members of each family are
 * typically much closer to 1.0. A jump ratio of 1.0 is equivalent to no
 * absorption edge and is thus the default value.
 * </p>
 * <p>
 * In fluorescence calculations, (if r is the jump ratio) the common figure of
 * merit is (r-1)/r, the fraction of ionizations that involve the specified
 * shell.
 * </p>
 */
public abstract class JumpRatio extends AlgorithmClass {

   /**
    * <p>
    * Many literature algorithms for the jump ratio compute only the jump ratio
    * for the full family of lines (K, [L1+L2+L3] or [M1+M2+M3+M4+M5]). To
    * compute the correct fluorescence for the individual lines in the L and M
    * families, we need to know the jump ratio on a per shell basis. This class
    * implements a mechanism for turning a all-shell jump ratio into a per shell
    * jump ratio.
    * </p>
    * <p>
    * The method <code>compute(Element elm,int family)</code> is implemented by
    * classes derived from FamilyJumpRatio. The method
    * </p>
    * <p>
    * <code>compute(AtomicShell shell)</code> is implemented by FamilyJumpRatio
    * using some crude estimates of the L1, L2, M1, M2, M3, M4.
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    * 
    * @author nritchie
    * @version 1.0
    */
   public abstract static class FamilyJumpRatio extends JumpRatio {

      /**
       * Approximate jump ratios for the lines L1 and L2
       */
      static private final double L_JUMPS[] = new double[]{1.16, 1.41};

      /**
       * Approximate jump ratios for the lines M1, M2, M3, M4
       */
      static private final double M_JUMPS[] = new double[]{1.02, 1.04, 1.13, 1.38};

      public FamilyJumpRatio(String name, String ref) {
         super(name, ref);
      }

      /**
       * Computes the total jump ratio for the specified element and line
       * family.
       * 
       * @param elm
       *           An element
       * @param family
       *           (One of AtomicShell.K_FAMILY, AtomicShell.L_FAMILY or
       *           AtomicShell.M_FAMILY)
       * @return The total jump ratio
       */
      abstract public double compute(Element elm, int family);

      /**
       * Determines whether the algorithm is capable of calculating the jump
       * ratio for this element and family.
       * 
       * @param elm
       *           An element
       * @param family
       *           (One of AtomicShell.K_FAMILY, AtomicShell.L_FAMILY or
       *           AtomicShell.M_FAMILY)
       * @return true or false
       */
      abstract public boolean isAvailable(Element elm, int family);

      /**
       * @see gov.nist.microanalysis.EPQLibrary.JumpRatio#isAvailable(gov.nist.microanalysis.EPQLibrary.AtomicShell)
       */
      @Override
      public boolean isAvailable(AtomicShell shell) {
         return isAvailable(shell.getElement(), shell.getFamily());
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.JumpRatio#compute(gov.nist.microanalysis.EPQLibrary.AtomicShell)
       */
      @Override
      public double compute(AtomicShell sh) {
         switch (sh.getShell()) {
            case AtomicShell.K :
               return compute(sh.getElement(), sh.getFamily());
            case AtomicShell.LI :
               return L_JUMPS[0];
            case AtomicShell.LII :
               return L_JUMPS[1];
            case AtomicShell.LIII :
               return compute(sh.getElement(), sh.getFamily()) / (L_JUMPS[0] * L_JUMPS[1]);
            case AtomicShell.MI :
               return M_JUMPS[0];
            case AtomicShell.MII :
               return M_JUMPS[1];
            case AtomicShell.MIII :
               return M_JUMPS[2];
            case AtomicShell.MIV :
               return M_JUMPS[3];
            case AtomicShell.MV :
               return compute(sh.getElement(), sh.getFamily()) / (M_JUMPS[0] * M_JUMPS[1] * M_JUMPS[2] * M_JUMPS[3]);
            default :
               return 1.0;
         }
      }

   }

   /**
    * @param name
    *           - The name of the implementation
    * @param ref
    *           - A reference detailing the source of the implemenation
    */
   public JumpRatio(String name, String ref) {
      super("Jump ratio", name, ref);
   }

   @Override
   protected void initializeDefaultStrategy() {
      // Don't do anything...
   }

   /**
    * getAllImplementations - Returns a full list of all available algorithms.
    * Each item is an implements the JumpRatio class.
    * 
    * @return List
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(new AlgorithmClass[]{JumpRatio.HeinrichDtsa, JumpRatio.Citzaf, JumpRatio.Springer1967, JumpRatio.Poehn, JumpRatio.Birks,});
   }

   /**
    * Does this implementation of the JumpRatio class handle this shell?
    * 
    * @param shell
    *           AtomicShell
    * @return true if this implementation can calculate this jump ratio
    */
   abstract public boolean isAvailable(AtomicShell shell);

   /**
    * Compute the jump ratio for the specified atomic shell. Compute should
    * return 1.0 if isAvailable returns false.
    * 
    * @param shell
    *           AtomicShell
    * @return The jump ratio (&gt;=1.0)
    */
   abstract public double compute(AtomicShell shell);

   /**
    * The fraction of ionizations involving the specified shell for x-rays of
    * energy slightly greater than the shell's edge energy.
    * 
    * @param shell
    * @return The ionization fraction (r-1)/r
    */
   public double ionizationFraction(AtomicShell shell) {
      final double r = compute(shell);
      final double res = r >= 1.0 ? (r - 1.0) / r : 0.0;
      assert (res >= 0.0) && (res <= 1.0) : res;
      return res;
   }

   /**
    * HeinrichDtsa implements a method for calculating the JumpRatios based on
    * the formulas described in Heinrich's IXCOM 11 paper and implemented in
    * DTSA. Note: The DTSA implementation deviates slightly from the
    * implementation described in the IXCOM 11 paper.
    */
   public static class HeinrichJumpRatio extends JumpRatio {
      public HeinrichJumpRatio() {
         super(MassAbsorptionCoefficient.HeinrichDtsa.getName(), MassAbsorptionCoefficient.HeinrichDtsa.getReference());
      }

      private final MassAbsorptionCoefficient mMAC = MassAbsorptionCoefficient.HeinrichDtsa;

      private final double DELTA = ToSI.eV(0.001);

      @Override
      public boolean isAvailable(AtomicShell shell) {
         switch (shell.getShell()) {
            case AtomicShell.K :
               if (shell.getElement().getAtomicNumber() > Element.elmIn)
                  return false;
               if (shell.getElement().getAtomicNumber() < Element.elmB)
                  return false;
               return true;
            case AtomicShell.LI :
               if (shell.getElement().getAtomicNumber() > Element.elmU)
                  return false;
               if (shell.getElement().getAtomicNumber() < Element.elmB)
                  return false;
               return true;
            case AtomicShell.LII :
               if (shell.getElement().getAtomicNumber() > Element.elmU)
                  return false;
               if (shell.getElement().getAtomicNumber() < Element.elmAl)
                  return false;
               return true;
            case AtomicShell.LIII :
               if (shell.getElement().getAtomicNumber() > Element.elmU)
                  return false;
               if (shell.getElement().getAtomicNumber() < Element.elmAl)
                  return false;
               return true;
            case AtomicShell.MI :
               if (shell.getElement().getAtomicNumber() > Element.elmU)
                  return false;
               if (shell.getElement().getAtomicNumber() < Element.elmCu)
                  return false;
               return true;
            case AtomicShell.MII :
               if (shell.getElement().getAtomicNumber() > Element.elmU)
                  return false;
               if (shell.getElement().getAtomicNumber() < Element.elmCu)
                  return false;
               return true;
            case AtomicShell.MIII :
               if (shell.getElement().getAtomicNumber() > Element.elmU)
                  return false;
               if (shell.getElement().getAtomicNumber() < Element.elmCu)
                  return false;
               return true;
            case AtomicShell.MIV :
               if (shell.getElement().getAtomicNumber() > Element.elmU)
                  return false;
               if (shell.getElement().getAtomicNumber() < Element.elmRb)
                  return false;
               return true;
            case AtomicShell.MV :
               if (shell.getElement().getAtomicNumber() > Element.elmU)
                  return false;
               if (shell.getElement().getAtomicNumber() < Element.elmTc)
                  return false;
               return true;
         }
         return false;
      }

      @Override
      public double compute(AtomicShell shell) {
         final double e = shell.getEdgeEnergy();
         final Element el = shell.getElement();
         return isAvailable(shell) ? mMAC.compute(el, e + DELTA) / mMAC.compute(el, e - DELTA) : 1.0;
      }
   }

   public static final JumpRatio HeinrichDtsa = new HeinrichJumpRatio();

   public static class CitzafJumpRatio extends FamilyJumpRatio {

      public CitzafJumpRatio() {
         super("CITZAF", "Taken from DTSA's implementation of CITZAF");
      }

      @Override
      public boolean isAvailable(Element elm, int family) {
         return (family >= AtomicShell.KFamily) && (family <= AtomicShell.MFamily);
      }

      @Override
      public double compute(Element elm, int family) {
         final int z = elm.getAtomicNumber();
         double tmp = 0.0;
         switch (family) {
            case AtomicShell.KFamily :
               tmp = 1.11728 - (0.07368 * Math.log(z)); // { K-LINE (0.88) }
               break;
            case AtomicShell.LFamily :
               tmp = 0.95478 - (0.00259 * z); // { L-LINE (0.75) }
               break;
            case AtomicShell.MFamily :
               return 3.56;
         }
         return 1.0 / (1.0 - tmp);
      }
   }

   public static JumpRatio Citzaf = new CitzafJumpRatio();

   public static class PoehnJumpRatio extends FamilyJumpRatio {

      public PoehnJumpRatio() {
         super("Poehn-1985", "As quoted in the Handbook of X-ray Spectrometry - pg 20");
      }

      @Override
      public boolean isAvailable(Element elm, int family) {
         final int z = elm.getAtomicNumber();
         switch (family) {
            case AtomicShell.KFamily :
               return (z >= 11) && (z <= 50);
            case AtomicShell.LFamily :
               return (z >= 30) && (z <= 83);
            default :
               return false;
         }
      }

      @Override
      public double compute(Element elm, int family) {
         final int z = elm.getAtomicNumber();
         switch (family) {
            case AtomicShell.KFamily :
               return 1.754e1 + (z * (-6.608e-1 + (z * (1.42e-2 - (z * 1.1e-4)))));
            case AtomicShell.LFamily :
               return 1.16 * 1.41 * (2.003e1 + (z * (-7.732e-1 + (z * (1.159e-2 + (z * -5.835e-5))))));
            case AtomicShell.MFamily :
               return 3.56;
            default :
               return 1.0;
         }
      }
   }

   public static JumpRatio Poehn = new PoehnJumpRatio();

   public static class BirksJumpRatio extends FamilyJumpRatio {

      static private final double[] K_JUMP_FACTORS = {12.3, 11.8, 11.1, 10.8, 10.5, 10.2, 9.8, 9.6, 9.3, 9.1, 8.9, 8.8, 8.6, 8.4, 8.3, 8.1, 8.0, 7.9,
            7.7, 7.6, 7.5, 7.4, 7.3, 7.2, 7.1, 7.0, 7.0, 6.9, 6.9, 6.8, 6.8, 6.7, 6.7, 6.6, 6.6, 6.6, 6.6, 6.5, 6.5, 6.5};
      static private final int[] L_JUMP_Z = {50, 51, 52, 53, 54, 55, 56, 57, 60, 65, 70, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 90,
            91, 92};
      static private final double[] L_JUMP_FACTORS = {5.0, 4.9, 4.8, 4.7, 4.6, 4.5, 4.5, 4.4, 4.3, 4.1, 4.0, 3.9, 3.9, 3.9, 3.8, 3.8, 3.8, 3.7, 3.7,
            3.7, 3.7, 3.6, 3.6, 3.6, 3.6, 3.5, 3.5, 3.5};

      public BirksJumpRatio() {
         super("Birks-1991", "Birks from the Practical Handbook of X-ray Spectrometry (1991)");
         assert K_JUMP_FACTORS.length == ((50 - 11) + 1);
         assert L_JUMP_FACTORS.length == L_JUMP_Z.length;
      }

      @Override
      public boolean isAvailable(Element elm, int family) {
         final int z = elm.getAtomicNumber();
         switch (family) {
            case AtomicShell.KFamily :
               return (z >= 11) && (z <= 50);
            case AtomicShell.LFamily :
               return Arrays.binarySearch(L_JUMP_Z, z) >= 0;
            default :
               return false;
         }
      }

      @Override
      public double compute(Element elm, int family) {
         final int z = elm.getAtomicNumber();
         if (isAvailable(elm, family))
            switch (family) {
               case AtomicShell.KFamily :
                  return K_JUMP_FACTORS[z - 11];
               case AtomicShell.LFamily :
                  return L_JUMP_FACTORS[Arrays.binarySearch(L_JUMP_Z, z)];
               case AtomicShell.MFamily :
                  return 3.56;
               default :
                  return 1.0;
            }
         else
            return 1.0;
      }
   }

   public static JumpRatio Birks = new BirksJumpRatio();

   /**
    * Springer1967 - This implementation of JumpRatio is based on the formulas
    * of Springer as quoted by Love, Scott and Reed in Quantitative Electron
    * Probe Microanalysis. It differs from the other implementations in that it
    * does not compute a jump ration for each shell independently but instead
    * provides a total jump ratio for a family of edges.
    */
   public static class SpringerJumpRatio extends FamilyJumpRatio {

      public SpringerJumpRatio() {
         super("Springer 1967", "Springer, Neues Jahrb. Mineral Abhanfl. 106, p241 (1967) as quoted by Love, Scott & Reed");
      }

      @Override
      public boolean isAvailable(Element elm, int family) {
         return (family >= AtomicShell.KFamily) && (family <= AtomicShell.MFamily);
      }

      @Override
      public double compute(Element elm, int family) {
         double tmp = 0.0, k = 1.0;
         final double z = elm.getAtomicNumber();
         switch (family) {
            case AtomicShell.KFamily :
               tmp = 0.924 - (0.00144 * z);
               break;
            case AtomicShell.LFamily : {
               k = 1.16 * 1.41;
               tmp = (0.548 - (0.00231 * z));
            }
            case AtomicShell.MFamily :
               return 3.56;
            default :
               return 1.0;
         }
         return k / (1.0 - (k * tmp));
      }
   }

   public static final JumpRatio Springer1967 = new SpringerJumpRatio();

   public static class McMaster extends JumpRatio {

      public McMaster() {
         super("McMaster 1969", "http://csrri.iit.edu/mucal-src/mucal_c-1.3.tar.gz");
      }

      /* L1-edge jump (Z>27) */
      static private final double l1_jump = 0.116000E+01;

      /* L2-edge jump (Z>27) */
      static private final double l2_jump = 0.141000E+01;

      /* L3-edge jump */

      static private final double[] l3_jump = {/* 1 */0.000000E+00, /* 2 */
            0.000000E+00, /* 3 */
            0.000000E+00, /* 4 */0.000000E+00, /* 5 */
            0.000000E+00, /* 6 */
            0.000000E+00, /* 7 */0.000000E+00, /* 8 */
            0.000000E+00, /* 9 */
            0.000000E+00, /* 10 */0.000000E+00, /* 11 */
            0.000000E+00, /* 12 */
            0.000000E+00, /* 13 */0.000000E+00, /* 14 */
            0.000000E+00, /* 15 */
            0.000000E+00, /* 16 */0.000000E+00, /* 17 */
            0.000000E+00, /* 18 */
            0.000000E+00, /* 19 */0.000000E+00, /* 20 */
            0.000000E+00, /* 21 */
            0.000000E+00, /* 22 */0.000000E+00, /* 23 */
            0.000000E+00, /* 24 */
            0.000000E+00, /* 25 */0.000000E+00, /* 26 */
            0.000000E+00, /* 27 */
            0.000000E+00, /* 28 */0.277200E+01, /* 29 */
            0.287400E+01, /* 30 */
            0.568400E+01, /* 31 */0.567100E+01, /* 32 */
            0.570400E+01, /* 33 */
            0.487500E+01, /* 34 */0.458700E+01, /* 35 */
            0.455700E+01, /* 36 */
            0.417000E+01, /* 37 */0.422300E+01, /* 38 */
            0.390600E+01, /* 39 */
            0.403600E+01, /* 40 */0.397600E+01, /* 41 */
            0.377400E+01, /* 42 */
            0.367500E+01, /* 43 */0.359100E+01, /* 44 */
            0.343100E+01, /* 45 */
            0.372100E+01, /* 46 */0.340200E+01, /* 47 */
            0.322300E+01, /* 48 */
            0.324900E+01, /* 49 */0.325500E+01, /* 50 */
            0.306000E+01, /* 51 */
            0.293900E+01, /* 52 */0.297900E+01, /* 53 */
            0.285600E+01, /* 54 */
            0.287900E+01, /* 55 */0.284700E+01, /* 56 */
            0.283900E+01, /* 57 */
            0.271700E+01, /* 58 */0.273700E+01, /* 59 */
            0.269500E+01, /* 60 */
            0.266200E+01, /* 61 */0.270200E+01, /* 62 */
            0.268000E+01, /* 63 */
            0.272300E+01, /* 64 */0.270100E+01, /* 65 */
            0.271300E+01, /* 66 */
            0.904700E+01, /* 67 */0.286300E+01, /* 68 */
            0.293300E+01, /* 69 */
            0.275800E+01, /* 70 */0.257300E+01, /* 71 */
            0.262000E+01, /* 72 */
            0.241500E+01, /* 73 */0.260000E+01, /* 74 */
            0.261700E+01, /* 75 */
            0.267500E+01, /* 76 */0.252900E+01, /* 77 */
            0.238700E+01, /* 78 */
            0.263200E+01, /* 79 */0.243900E+01, /* 80 */
            0.240000E+01, /* 81 */
            0.249800E+01, /* 82 */0.246600E+01, /* 83 */
            0.233800E+01, /* 84 */
            0.000000E+00, /* 85 */0.000000E+00, /* 86 */
            0.234400E+01, /* 87 */
            0.000000E+00, /* 88 */0.000000E+00, /* 89 */
            0.000000E+00, /* 90 */
            0.238800E+01, /* 91 */0.000000E+00, /* 92 */
            0.229200E+01, /* 93 */
            0.000000E+00, /* 94 */0.225100E+01};

      @Override
      public double compute(AtomicShell shell) {
         assert isAvailable(shell) : "Not supported";
         if (isAvailable(shell))
            switch (shell.getShell()) {
               case AtomicShell.LI :
                  return l1_jump;
               case AtomicShell.LII :
                  return l2_jump;
               case AtomicShell.LIII :
                  return l3_jump[shell.getElement().getAtomicNumber() - 1];
               default :
                  assert false : "Not supported";
            }
         return 1.0;
      }

      @Override
      public boolean isAvailable(AtomicShell shell) {
         final int sh = shell.getShell();
         return ((shell.getElement().getAtomicNumber() > 27) && (sh == AtomicShell.LI)) || (sh == AtomicShell.LII) || (sh == AtomicShell.LIII);
      }

   }

}
