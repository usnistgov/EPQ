package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * The Fluorescence class is responsible for calculating the F (fluorescence)
 * term in the ZAF correction procedure.
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

abstract public class Fluorescence extends AlgorithmClass {

   private static final AlgorithmClass[] mAllImplementations = {Fluorescence.Reed, Fluorescence.Null};

   protected Fluorescence(String name, String reference) {
      super("Fluorescence", name, reference);
   }

   @Override
   protected void initializeDefaultStrategy() {
      addDefaultAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Default);
      addDefaultAlgorithm(EdgeEnergy.class, EdgeEnergy.Default);
   }

   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   /**
    * Returns the mean free path for photons in a block of the specified
    * composition. This gives a sense for the volume producing secondary
    * fluorescence.
    * 
    * @param mat
    * @param primary
    * @return Length scale in meters
    * @throws EPQException
    */
   public double lengthScale(Material mat, XRayTransition primary) throws EPQException {
      final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
      return 1.0 / (mac.compute(mat, primary) * mat.getDensity());
   }

   /**
    * PrimaryExcitingLine - Which line of the specified element (el) is above
    * the excitation energy for the specified shell (sh) by the smallest amount.
    * Returns XRayTransition.None if no lines meet this criterion.
    * 
    * @param el
    *           Element - The element producing the fluorescence
    * @param sh
    *           AtomicShell - The destination shell for the x-ray transition
    *           being excited
    * @return int - One of None, KA1, ... , MZ2
    */
   public int primaryExcitingLine(Element el, AtomicShell sh) {
      final EdgeEnergy eea = (EdgeEnergy) getAlgorithm(EdgeEnergy.class);
      double wBest = 0.0;
      int best = XRayTransition.None;
      int bestFam = AtomicShell.NoFamily;
      final double ee = eea.compute(sh);
      for (int tr = XRayTransition.KA1; tr <= XRayTransition.MZ2; ++tr)
         try {
            if (XRayTransition.exists(el, tr) && (XRayTransition.getEnergy(el, tr) > ee) && (XRayTransition.getFamily(tr) >= bestFam)) {
               final double w = XRayTransition.getWeight(el, tr, XRayTransition.NormalizeDefault);
               // Favor L over K
               if ((XRayTransition.getFamily(tr) > bestFam) || (w > wBest)) {
                  wBest = w;
                  best = tr;
                  bestFam = XRayTransition.getFamily(best);
               }
            }
         } catch (final EPQException ex) {
            // Just ignore
         }
      return best;
   }

   /**
    * Compute the total additional ionization of the specified AtomicShell due
    * to fluorescence of other elements in the Composition due to a electron
    * beam at energy e0. This algorithm is slower but more thorough. It computes
    * the fluorescence for each potential contributing line independently and
    * sums the result.
    * 
    * @param comp
    *           Composition
    * @param xrt
    *           XRayTransition
    * @param e0
    *           double - The beam energy
    * @param takeOff
    *           double - The take-off angle in radians
    * @return double
    */
   public double computeThorough(Composition comp, XRayTransition xrt, double e0, double takeOff) {
      final EdgeEnergy eea = (EdgeEnergy) getAlgorithm(EdgeEnergy.class);
      double fSum = 0.0;
      for (final Element el : comp.getElementSet())
         try {
            for (int tr = XRayTransition.KA1; tr <= XRayTransition.MZ2; ++tr) {
               final XRayTransition xrt2 = new XRayTransition(el, tr);
               if ((xrt2.getEnergy() >= eea.compute(xrt)) && (eea.compute(xrt2) < e0)) {
                  final double w = xrt2.getWeight(XRayTransition.NormalizeFamily);
                  if (w > 0.0)
                     fSum += compute(comp, xrt2, xrt, e0, takeOff) * w;
               }
            }
         } catch (final EPQException ex) {
            // Just ignore it...
         }
      return 1.0 + fSum;
   }

   /**
    * Computes the fluorescence by considering only the lowest energy x-ray
    * transition for each element in the Composition capable of ionizing the
    * specified AtomicShell.
    * 
    * @param comp
    *           Composition - The bulk material
    * @param secondary
    *           XRayTransition - The transition under consideration.
    * @param e0
    *           double - The beam energy (in Joules)
    * @param takeOff
    *           double - Take off angle (in Radians)
    * @return double - The F factor &gt;= 1.0
    */
   public double compute(Composition comp, XRayTransition secondary, double e0, double takeOff) {
      final EdgeEnergy eea = (EdgeEnergy) getAlgorithm(EdgeEnergy.class);
      double fSum = 0.0;
      for (final Element el : comp.getElementSet()) {
         final int prim = primaryExcitingLine(el, secondary.getDestination());
         if (prim != XRayTransition.None) {
            final XRayTransition primXrt = new XRayTransition(el, prim);
            if (eea.compute(primXrt) < e0)
               // Ignore prim if it is more than 5.0 keV (or 3.5 keV for L&M
               // lines) above edge
               try {
                  final double shEE = eea.compute(secondary);
                  final double delta = (secondary.getFamily() == AtomicShell.KFamily ? ToSI.keV(5.0) : ToSI.keV(3.5));
                  if (primXrt.getEnergy() < (shEE + delta))
                     try {
                        double w = 0.0;
                        final int primFam = primXrt.getFamily();
                        for (int tr = XRayTransition.KA1; tr <= XRayTransition.MZ2; ++tr)
                           // same family and above ionization edge and less
                           // than the beam energy
                           if (XRayTransition.exists(el, tr) && (XRayTransition.getFamily(tr) == primFam)
                                 && (XRayTransition.getEnergy(el, tr) >= shEE) && (eea.compute(new XRayTransition(el, tr)) < e0))
                              w += XRayTransition.getWeight(el, tr, XRayTransition.NormalizeFamily);
                        if (w > 0.0)
                           // System.out.println(xrt.toString() + " is excited
                           // by "+ primXrt.toString() + ": w =
                           // "+Double.toString(w));
                           fSum += compute(comp, primXrt, secondary, e0, takeOff) * w;
                     } catch (final EPQException ex) {
                        // Just ignore it...
                     }
               } catch (final EPQException ex1) {
                  // Just ignore it...
               }
         }
      }
      return 1.0 + fSum;
   }

   /**
    * Implement this method to compute the intensity of the fluorescence due to
    * the specified XRayTransition on a specific AtomicShell (Element + shell)
    * 
    * @param comp
    *           Composition - The material in which the experiment is being
    *           performed
    * @param primary
    *           XRayTransition - The x-ray that ionizes the element producing
    *           the fluorescence radiation
    * @param secondary
    *           XRayTransition - The transition which is being fluoresced by
    *           xrtB
    * @param e0
    *           double - The beam energy (Joules)
    * @param takeOff
    *           double - The take of angule in radians
    * @return double
    */
   abstract public double compute(Composition comp, XRayTransition primary, XRayTransition secondary, double e0, double takeOff);

   /**
    * Null - Performs no fluorescence correction.
    */
   public static class NullFluorescence extends Fluorescence {
      public NullFluorescence() {
         super("Null", "None");
      }

      @Override
      public double compute(Composition comp, XRayTransition primary, XRayTransition secondary, double e0, double takeOff) {
         return 0.0;
      }
   }

   public static final Fluorescence Null = new NullFluorescence();

   public static class MCFluorescence extends Fluorescence {

      private final static int REPS = 1000;

      public MCFluorescence() {
         super("MC Secondary", "A simple Monte Carlo secondary modeling algorithm");
      }

      @Override
      protected void initializeDefaultStrategy() {
         super.initializeDefaultStrategy();
         this.addDefaultAlgorithm(JumpRatio.class, JumpRatio.HeinrichDtsa);
         this.addDefaultAlgorithm(TransitionProbabilities.class, TransitionProbabilities.Default);
         this.addDefaultAlgorithm(CorrectionAlgorithm.PhiRhoZAlgorithm.class, CorrectionAlgorithm.XPP);
      }

      @Override
      public double compute(Composition comp, XRayTransition primary, XRayTransition secondary, double e0, double takeOff) {
         double sum = 0.0;
         try {
            final double pE = primary.getEnergy();
            final double sE = secondary.getEnergy();
            if (pE > secondary.getEdgeEnergy()) {
               final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
               final JumpRatio jr = (JumpRatio) getAlgorithm(JumpRatio.class);
               final TransitionProbabilities tp = (TransitionProbabilities) getAlgorithm(TransitionProbabilities.class);
               final CorrectionAlgorithm.PhiRhoZAlgorithm prz = (CorrectionAlgorithm.PhiRhoZAlgorithm) getAlgorithm(
                     CorrectionAlgorithm.PhiRhoZAlgorithm.class);
               final TreeMap<XRayTransition, Double> trans = tp.getTransitions(secondary.getDestination(), 0.0);
               final double macP = MassAbsorptionCoefficient.toCmSqrPerGram(mac.compute(comp, pE));
               final double chi = MassAbsorptionCoefficient.toCmSqrPerGram(mac.compute(comp, sE)) / Math.sin(takeOff);
               final SpectrumProperties sp = new SpectrumProperties();
               sp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(e0));
               sp.setNumericProperty(SpectrumProperties.TakeOffAngle, Math.toDegrees(takeOff));
               prz.initialize(comp, secondary.getDestination(), sp);
               final double gS = prz.generated(secondary);
               prz.initialize(comp, primary.getDestination(), sp);
               final double gP = prz.generated(primary);
               final Element elmS = secondary.getElement();
               final double f = jr.ionizationFraction(secondary.getDestination()) * trans.get(secondary) * (gS / gP)
                     * ((comp.weightFraction(elmS, false) * mac.compute(elmS, pE)) / mac.compute(comp, pE));
               for (int i = 0; i < REPS; ++i) {
                  final double[] dir = Math2.randomDir();
                  if (dir[2] >= 0.0)
                     sum += f * Math.exp(((-dir[2] * Math2.expRand()) / macP) * chi);
               }
            }
         } catch (final EPQException e) {
            e.printStackTrace();
         }
         return sum / REPS;
      }

   }

   /**
    * Reed - Implements Reed's method of calculating the fluorescence due to
    * characteristic radiation as described in S.J.B. Reed, Electron Probe
    * Microanalysis, 2nd ed. 1993 Cambridge University Press (ISBN:
    * 0-521-41956-5)
    */

   public static class ReedFluorescence extends Fluorescence {

      ReedFluorescence() {
         super("Reed 1993", "SJB Reed, Electron Probe Microanalysis (2nd ed.), 1993 Cambridge University Press");
      }

      @Override
      protected void initializeDefaultStrategy() {
         super.initializeDefaultStrategy();
         addDefaultAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.HeinrichDtsa);
         addDefaultAlgorithm(JumpRatio.class, JumpRatio.HeinrichDtsa);
         addDefaultAlgorithm(LenardCoefficient.class, LenardCoefficient.Heinrich);
         addDefaultAlgorithm(IonizationDepthRatio.class, IonizationDepthRatio.Reed1990);
      }

      /**
       * Accounts for the differences in ionization cross section between K , L
       * & M shells
       */
      private double familyFactor(int fA, int fB) {
         final double res = 1.0; // fA==fB
         if (fA != fB) {
            if ((fA == AtomicShell.KFamily) && (fB == AtomicShell.LFamily))
               return 1.0 / 0.24;
            if ((fA == AtomicShell.LFamily) && (fB == AtomicShell.KFamily))
               return 0.24;
            if (fA == AtomicShell.MFamily) {
               assert ((fB == AtomicShell.KFamily) || (fB == AtomicShell.LFamily));
               return 0.02;
            }
            return 0.0;
         }
         return res;
      }

      /**
       * Compute the added intensity in the transitions associated with the
       * AtomicShell asA due to absorption of x-rays from XRayTransition xrtB in
       * the Composition comp.
       * 
       * @param comp
       *           Composition - The bulk material
       * @param primary
       *           XRayTransition - The radiation that ionizes the shell
       * @param secondary
       *           XRayTransition - The secondary fluorescence radiation
       * @param e0
       *           double - The beam energy
       * @param takeOff
       *           double - The takeOff angle (radians)
       * @return double - The amount of additional fluorescence
       */
      @Override
      public double compute(Composition comp, XRayTransition primary, XRayTransition secondary, double e0, double takeOff) {
         try {
            final double eeA = secondary.getEdgeEnergy();
            if (primary.getEnergy() >= eeA) {
               final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
               final JumpRatio jr = (JumpRatio) getAlgorithm(JumpRatio.class);
               final FluorescenceYieldMean fym = (FluorescenceYieldMean) getAlgorithm(FluorescenceYieldMean.class);
               final LenardCoefficient lc = (LenardCoefficient) getAlgorithm(LenardCoefficient.class);
               final IonizationDepthRatio idr = (IonizationDepthRatio) getAlgorithm(IonizationDepthRatio.class);
               assert (primary.getEnergy() >= eeA);
               assert (primary.getEnergy() < e0);
               final Element bElm = primary.getElement();
               final Element aElm = secondary.getElement();
               if (!(comp.containsElement(bElm) && comp.containsElement(aElm)))
                  return 0.0;
               final double cB = comp.weightFraction(bElm, true);
               // Mass absorption coefficient for primary in pure
               // secondary.getElement()
               final double muB_A = MassAbsorptionCoefficient.toCmSqrPerGram(mac.compute(aElm, primary));
               // Mass absorption coefficient for xrtB in comp
               final double muB = MassAbsorptionCoefficient.toCmSqrPerGram(mac.compute(comp, primary));
               // How much of the absorption is due to this edge???
               final double ionizeF = jr.ionizationFraction(secondary.getDestination());
               final double fluorB = fym.compute(primary.getDestination());
               final double Aa = aElm.getAtomicWeight();
               final double Ab = bElm.getAtomicWeight();
               final double u = MassAbsorptionCoefficient.toCmSqrPerGram(mac.compute(comp, secondary)) / (Math.sin(takeOff) * muB);
               final double v = lc.compute(e0, secondary) / muB; // keV
               double ss = idr.compute(primary, secondary, e0);
               // correct
               final double f = familyFactor(secondary.getFamily(), primary.getFamily());
               return f * 0.5 * cB * (muB_A / muB) * ionizeF * fluorB * (Aa / Ab) * ss * ((Math.log(1.0 + u) / u) + (Math.log(1.0 + v) / v));
            }
         } catch (final EPQException ex) {
            System.err.println("Error in " + toString() + ": " + ex.toString());
         }
         return 0.0;
      }
   }

   public static final Fluorescence Reed = new ReedFluorescence();

   public static class Reed1990Fluorescence extends Fluorescence {
      public Reed1990Fluorescence() {
         super("Reed 1990a", "Love, Scott & Reed");
      }

      @Override
      protected void initializeDefaultStrategy() {
         super.initializeDefaultStrategy();
         addDefaultAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.HeinrichDtsa);
      }

      /**
       * Compute the added intensity in the transitions associated with the
       * AtomicShell asA due to absorption of x-rays from XRayTransition xrtB in
       * the Composition comp.
       * 
       * @param comp
       *           Composition - The bulk material
       * @param primary
       *           XRayTransition - The radiation that ionizes the shell
       * @param secondary
       *           XRayTransition - The secondary fluorescence radiation
       * @param e0
       *           double - The beam energy
       * @param takeOff
       *           double - The takeOff angle (radians)
       * @return double - The amount of additional fluorescence
       */
      @Override
      public double compute(Composition comp, XRayTransition primary, XRayTransition secondary, double e0, double takeOff) {
         final EdgeEnergy eea = (EdgeEnergy) getAlgorithm(EdgeEnergy.class);
         try {
            final double eeA = eea.compute(secondary);
            if (primary.getEnergy() >= eeA) {
               final Element elmA = secondary.getElement();
               final Element elmB = primary.getElement();
               final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
               final double jumpRatio = JumpRatio.HeinrichDtsa.ionizationFraction(secondary.getDestination());
               final double uA = secondary.getEdgeEnergy() / e0;
               final double uB = primary.getEdgeEnergy() / e0;
               final double omega = AlgorithmUser.getDefaultFluorescenceYieldMean().compute(secondary.getDestination());
               final double mm = (comp.weightFraction(elmA, false) * mac.compute(elmA, primary)) / mac.compute(comp, primary);
               final double uu = ((elmA.getAtomicWeight() / elmB.getAtomicWeight()) * (((uB * Math.log(uB)) - uB) + 1.0))
                     / (((uA * Math.log(uA)) - uA) + 1.0);
               final double u = mac.compute(comp, secondary) / (Math.sin(takeOff) * mac.compute(comp, primary));
               final double sigma = 4.5e5 / (Math.pow(FromSI.keV(e0), 1.65) - Math.pow(FromSI.keV(secondary.getEdgeEnergy()), 1.65));
               final double v = sigma / MassAbsorptionCoefficient.toCmSqrPerGram(mac.compute(comp, primary));
               return 0.5 * mm * jumpRatio * omega * uu * ((Math.log(1.0 + u) / u) + (Math.log(1 + v) / v));
            }
         } catch (final EPQException ex) {
            System.err.println("Error in " + toString() + ": " + ex.toString());
         }
         return 0.0;
      }
   }

}
