package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.Math2;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Implementations of various different stopping power algorithms. The SI unit
 * for stopping power is (Joule m^2)/kg = energy / (density * length). However
 * it should be noted that except for Pouchou1991 and Proza96 these expressions
 * are simply proportional to the stopping power.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */
abstract public class StoppingPower
   extends AlgorithmClass {

   /**
    * Converts from SI (J m^2 / kg) to the more standard (keV cm^2 / g)
    * 
    * @param x double
    * @return x converted from SI to keV-g-cm units
    */
   static public double tokeVcmSqrPerGram(double x) {
      // x in energy / (density * length)
      return (x * FromSI.KEV) / (FromSI.GRAM / (FromSI.CM * FromSI.CM));
   }

   /**
    * Converts from the more standard (keV cm^2 / g) to SI (J m^2 / kg)
    * 
    * @param x double
    * @return x converted to SI from keV-g-cm units
    */
   static public double fromkeVcmSqrPerGram(double x) {
      // x in energy / (density * length)
      return (x * ToSI.KEV) / (ToSI.GRAM / (ToSI.CM * ToSI.CM));
   }

   /**
    * Converts from SI (kg / (J m^2)) to the more standard g/(keV cm^2)
    * 
    * @param x double
    * @return x converted from SI to keV-g-cm units
    */
   static public double invToGramPerkeVcmSqr(double x) {
      // x in (density * length) / energy
      return (x * (FromSI.GRAM / (FromSI.CM * FromSI.CM))) / FromSI.KEV;
   }

   /**
    * Converts to SI (kg / (J m^2)) from the more standard g/(keV cm^2)
    * 
    * @param x double
    * @return x converted from SI to keV-g-cm units
    */
   static public double invFromGramPerkeVcmSqr(double x) {
      // J = A V
      // x in (density * length) / energy
      return (x * (ToSI.GRAM / (ToSI.CM * ToSI.CM))) / ToSI.KEV;
   }

   /**
    * To keV/(g/cm^2) from J/(kg/m^2)
    */
   protected StoppingPower(String name, String ref) {
      super("Stopping Power", name, ref);
   }

   protected StoppingPower(String name, LitReference ref) {
      super("Stopping Power", name, ref);
   }

   /**
    * initialize - Override this method in derived classes to set the default
    * MeanIonizationPotential algorithm to something other than Berger83. Call
    * this method to restore the default MIP algorithm.
    */
   @Override
   public void initializeDefaultStrategy() {
      addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.Berger83);
   }

   /**
    * getAllImplementations - An array of all classes implementing StoppingPower
    * 
    * @return List
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   /**
    * Compute the stopping power (S) in the specified material for the specified
    * element and shell given a beam of the specified energy.
    * 
    * @param comp Composition - The material
    * @param shell AtomicShell - The element and shell
    * @param e0 double - The beam energy
    * @return double - The stopping power (S) in Joule per (kg / meter^3) per
    *         meter
    */
   public double compute(Composition comp, AtomicShell shell, double e0) {
      return 1.0 / computeInv(comp, shell, e0);
   }

   /**
    * Compute the one over the stopping power (1/S) in the specified material
    * for the specified element and shell given a beam of the specified energy.
    * Often it is easier to calculate this and this is what is really required.
    * 
    * @param comp Composition - The material
    * @param shell AtomicShell - The element and shell
    * @param e0 double - The beam energy
    * @return double - The inverse stopping power (1/S) in kilogram / (Joule
    *         meter^2)
    */
   abstract public double computeInv(Composition comp, AtomicShell shell, double e0);

   /**
    * Computes the relative stopping power between the unknown sample and the
    * standard sample.
    * 
    * @param unknown The 'unknown' material
    * @param standard The standard material
    * @param shell The shell to be analyzed
    * @param e0 The beam energy
    * @return The ratio of unknown / standard stopping powers
    */
   public double computeRelative(Composition unknown, Composition standard, AtomicShell shell, double e0) {
      return computeInv(standard, shell, e0) / computeInv(unknown, shell, e0);
   }

   /**
    * Pouchou and Pichoir 1991
    */
   public static class Pouchou1991StoppingPower
      extends StoppingPower {
      public Pouchou1991StoppingPower() {
         super("Pouchou & Pichoir", LitReference.ElectronProbeQuant);
      }

      @Override
      public void initializeDefaultStrategy() {
         super.initializeDefaultStrategy();
         addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.Zeller75);
      }

      // Checked 12-Feb-2006
      @Override
      public double computeInv(Composition comp, AtomicShell shell, double e0) {
         final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
         // Seems to produce reasonable results (comparing TiO2 against TryZAF)
         double bigM = 0.0;
         for(final Element el : comp.getElementSet())
            bigM += (comp.weightFraction(el, true) * el.getAtomicNumber()) / el.getAtomicWeight();
         final double d[] = new double[3];
         final double p[] = new double[3];
         double v0;
         { // from PAP1991, agrees with Love, Scott & Reed
            final double j = FromSI.keV(mip.computeLn(comp));
            v0 = FromSI.keV(e0) / j;
            d[0] = 6.6e-6;
            d[1] = 1.12e-5 * (1.35 - (0.45 * j * j));
            d[2] = 2.2e-6 / j;
            p[0] = 0.78;
            p[1] = 0.1;
            p[2] = 0.25 * (j - 2.0); // -(0.5 - 0.25 * j);
         }
         final double m = ProportionalIonizationCrossSection.Pouchou86ICX.computeExponent(shell);
         final double u0 = e0 / shell.getEdgeEnergy(); // units cancel
         double tmp = 0.0;
         // From PAP1991, LSR is wrong!
         for(int k = 0; k < 3; ++k) {
            final double tk = (1.0 + p[k]) - m;
            tmp += (d[k] * Math.pow(v0 / u0, p[k]) * (((tk * Math.pow(u0, tk) * Math.log(u0)) - Math.pow(u0, tk)) + 1))
                  / (tk * tk);
         }
         return invFromGramPerkeVcmSqr((u0 / (v0 * bigM)) * tmp);
      }
   }

   public static final StoppingPower Pouchou1991 = new Pouchou1991StoppingPower();

   public static class Proza96StoppingPower
      extends StoppingPower {
      public Proza96StoppingPower() {
         super("Proza96", LitReference.Proza96);
      }

      @Override
      public void initializeDefaultStrategy() {
         super.initializeDefaultStrategy();
         addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.Zeller75);
      }

      @Override
      public double computeInv(Composition comp, AtomicShell shell, double e0) {
         final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
         // Seems to produce reasonable results (comparing TiO2 against TryZAF)
         double bigM = 0.0;
         for(final Element el : comp.getElementSet())
            bigM += (comp.weightFraction(el, true) * el.getAtomicNumber()) / el.getAtomicWeight();
         final double d[] = new double[3];
         final double p[] = new double[3];
         double v0;
         { // from PAP1991, agrees with Love, Scott & Reed
            final double j = FromSI.keV(mip.computeLn(comp));
            v0 = FromSI.keV(e0) / j;
            d[0] = 6.6e-6;
            d[1] = 1.12e-5 * (1.35 - (0.45 * j * j));
            d[2] = 2.2e-6 / j;
            p[0] = 0.78;
            p[1] = 0.1;
            p[2] = 0.25 * (j - 2.0); // -(0.5 - 0.25 * j);
         }
         final double m = ProportionalIonizationCrossSection.Proza96ICX.computeExponent(shell);
         final double u0 = e0 / shell.getEdgeEnergy(); // units cancel
         double tmp = 0.0;
         // From PAP1991, LSR is wrong!
         for(int k = 0; k < 3; ++k) {
            final double tk = (1.0 + p[k]) - m;
            tmp += (d[k] * Math.pow(v0 / u0, p[k]) * (((tk * Math.pow(u0, tk) * Math.log(u0)) - Math.pow(u0, tk)) + 1))
                  / (tk * tk);
         }
         return invFromGramPerkeVcmSqr((u0 / (v0 * bigM)) * tmp);
      }
   }

   public static final StoppingPower Proza96 = new Proza96StoppingPower();

   /**
    * Thomas1963
    */
   public static class Thomas1963StoppingPower
      extends StoppingPower {
      public Thomas1963StoppingPower() {
         super("Thomas 1963", LitReference.QuantitativeElectronProbeMicroanalysis);
      }

      @Override
      public void initializeDefaultStrategy() {
         super.initializeDefaultStrategy();
         addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.Duncumb69);
      }

      @Override
      public double computeInv(Composition comp, AtomicShell shell, double e0) {
         return 1.0 / compute(comp, shell, e0);
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
         double s = 0.0;
         final double ec = shell.getEdgeEnergy(); // All energys in SI
         for(final Element elm : comp.getElementSet()) {
            final double j = mip.compute(elm);
            s += comp.weightFraction(elm, true) * (elm.getAtomicNumber() / elm.getAtomicWeight())
                  * Math.log(((1.116 / 2.0) * (e0 + ec)) / j);
         }
         return StoppingPower.fromkeVcmSqrPerGram(s);
      }
   }

   public static final StoppingPower Thomas1963 = new Thomas1963StoppingPower();

   /**
    * Reed's modifications to Thomas' expression
    */
   public static class Reed1975StoppingPower
      extends StoppingPower {
      public Reed1975StoppingPower() {
         super("Reed's modification to Thomas 1963", LitReference.QuantitativeElectronProbeMicroanalysis);
      }

      @Override
      public void initializeDefaultStrategy() {
         super.initializeDefaultStrategy();
         addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.Duncumb69);
      }

      @Override
      public double computeInv(Composition comp, AtomicShell shell, double e0) {
         return compute(comp, shell, e0);
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
         double s = 0.0;
         final double ec = shell.getEdgeEnergy(); // All energys in SI
         for(final Element elm : comp.getElementSet())
            s += comp.weightFraction(elm, true) * (elm.getAtomicNumber() / elm.getAtomicWeight())
                  * Math.log(((1.116 / 3.0) * ((2.0 * e0) + ec)) / mip.compute(elm));
         return StoppingPower.fromkeVcmSqrPerGram(s);
      }
   }

   public static final StoppingPower Reed1975 = new Reed1975StoppingPower();

   public static class LoveScottCITZAFStoppingPower
      extends StoppingPower {
      public LoveScottCITZAFStoppingPower() {
         super("Love/Scott (CITZAF)", "Love/Scott as implemented in CITZAF");
      }

      @Override
      public void initializeDefaultStrategy() {
         super.initializeDefaultStrategy();
         addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.BergerAndSeltzerCITZAF);
      }

      @Override
      public double computeInv(Composition comp, AtomicShell shell, double e0) {
         final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
         final double ec = shell.getEdgeEnergy();
         final double u0A = e0 / ec;
         final double jBar = mip.computeLn(comp);
         double aa = 0.0;
         for(final Element elm : comp.getElementSet())
            aa += (elm.getAtomicNumber() * comp.weightFraction(elm, true)) / elm.getAtomicWeight();
         return (1.0 + (16.05 * Math.sqrt(jBar / ec) * Math.pow((Math.sqrt(u0A) - 1.0) / (u0A - 1.0), 1.07))) / aa;
      }
   }

   public static final StoppingPower LoveScottCITZAF = new LoveScottCITZAFStoppingPower();

   public static class LoveScottStoppingPower
      extends StoppingPower {
      public LoveScottStoppingPower() {
         super("Love/Scott (CITZAF)", "Love/Scott as described in JTA in the green book");
      }

      @Override
      public void initializeDefaultStrategy() {
         super.initializeDefaultStrategy();
         addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.BergerAndSeltzerCITZAF);
      }

      @Override
      public double computeInv(Composition comp, AtomicShell shell, double e0) {
         final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
         final double ec = shell.getEdgeEnergy();
         final double u0A = e0 / ec;
         final double jBar = mip.computeLn(comp);
         double aa = 0.0;
         for(final Element elm : comp.getElementSet())
            aa += (elm.getAtomicNumber() * comp.weightFraction(elm, true)) / elm.getAtomicWeight();
         return (1.0 + (16.05 * Math.sqrt(jBar / ec) * Math.pow(Math.sqrt(u0A) / (u0A - 1.0), 1.07))) / aa;
      }
   }

   public static final StoppingPower LoveScott = new LoveScottStoppingPower();

   /**
    * PhilibertTixier1968's simple expression for stopping power
    */
   static public class PhilibertTixier1968StoppingPower
      extends StoppingPower {
      public PhilibertTixier1968StoppingPower() {
         super("Philibert-Tixier 1968", LitReference.QuantitativeElectronProbeMicroanalysis);
      }

      @Override
      public void initializeDefaultStrategy() {
         super.initializeDefaultStrategy();
         addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.Wilson41);
      }

      @Override
      public double computeInv(Composition comp, AtomicShell shell, double e0) {
         final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
         double m = 0.0;
         for(final Element elm : comp.getElementSet())
            m += comp.weightFraction(elm, true) * (elm.getAtomicNumber() / elm.getAtomicWeight());
         final double jBar = mip.computeLn(comp);
         final double ee = shell.getEdgeEnergy();
         final double u0 = e0 / ee;
         assert u0 >= 1.0;
         final double w = (1.166 * ee) / jBar;
         assert w > 1.0;
         return StoppingPower.fromkeVcmSqrPerGram((u0 - 1.0 - ((Math.log(w) / w) * (Math2.li(w * u0) - Math2.li(w)))) / m);
      }
   }

   public static final StoppingPower PhilibertTixier1968 = new PhilibertTixier1968StoppingPower();

   private static final AlgorithmClass[] mAllImplementations = {
      StoppingPower.Thomas1963,
      StoppingPower.Reed1975,
      StoppingPower.PhilibertTixier1968,
      StoppingPower.Pouchou1991,
      StoppingPower.Proza96
   };

}
