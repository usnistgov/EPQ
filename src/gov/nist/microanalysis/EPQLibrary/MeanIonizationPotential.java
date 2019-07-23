package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

import gov.nist.microanalysis.Utility.CSVReader;

/**
 * <p>
 * An abstract class that serves as the basis for various different algorithms
 * for or tabulations of the mean ionization potential.
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

abstract public class MeanIonizationPotential
   extends AlgorithmClass {

   /**
    * MeanIonizationPotential - Create an instance of the abstract
    * MeanIonizationPotential class with the specified name and literature
    * reference.
    * 
    * @param name String
    * @param reference String
    */
   private MeanIonizationPotential(String name, String reference) {
      super("Mean Ionization Potential", name, reference);
   }

   @Override
   protected void initializeDefaultStrategy() {
      // Don't do anything...
   }

   /**
    * getAllImplementations - Returns a full list of all available algorithms.
    * Each item is an implements the MeanIonizationClass.
    * 
    * @return List
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   public String caveat(Element el) {
      return CaveatBase.None;
   }

   public String caveat(Composition comp) {
      String res = CaveatBase.None;
      for(final Element el : comp.getElementSet())
         res = CaveatBase.append(res, caveat(el));
      return res;
   }

   /**
    * computeLn - Computes the mean ionization potential for a Composition using
    * the rule based on the ln(J_i) where J_i is the elemental
    * MeanIonizationPotential.
    * 
    * @param comp Composition
    * @return double
    */
   public double computeLn(Composition comp) {
      double m = 0.0;
      double lnJ = 0.0;
      for(final Element el : comp.getElementSet()) {
         final double cz_a = (comp.weightFraction(el, true) * el.getAtomicNumber()) / el.getAtomicWeight();
         m += cz_a;
         lnJ += cz_a * Math.log(FromSI.keV(compute(el)));
      }
      return ToSI.keV(Math.exp(lnJ / m));
   }

   /**
    * eval - Each different version of the algorithm should implement this
    * method.
    * 
    * @param el Element - The element
    * @return double - in Joules
    */
   abstract public double compute(Element el);

   /**
    * Sternheimer64 - An empirical expression for J attributed to Sternheimer by
    * Berger and Seltzer
    */
   public static class Sternheimer64MeanIonizationPotential
      extends MeanIonizationPotential {
      public Sternheimer64MeanIonizationPotential() {
         super("Sternheimer 1964", "Sternheimer quoted in Berger MJ, Seltzer S. NASA Technical Publication SP-4012 (1964)");
      }

      @Override
      final public double compute(Element el) {
         final double z = el.getAtomicNumber();
         return ToSI.eV((9.76 * z) + (58.8 * Math.pow(z, -0.19)));
      }
   }

   public static final MeanIonizationPotential Sternheimer64 = new Sternheimer64MeanIonizationPotential();

   /**
    * Berger and Seltzer as implemented by JT Armstrong in CITZAF 3.06
    */
   public static class BergerAndSeltzerCITZAFMeanIonizationPotential
      extends MeanIonizationPotential {
      public BergerAndSeltzerCITZAFMeanIonizationPotential() {
         super("Berger & Seltzer as per JTA", "Berger and Seltzer as implemented by CITZAF 3.06");
      }

      @Override
      public double compute(Element el) {
         final double z = el.getAtomicNumber();
         return ToSI.eV((9.76 * z) + (58.5 * Math.pow(z, -0.19)));
      }
   }

   public static final MeanIonizationPotential BergerAndSeltzerCITZAF = new BergerAndSeltzerCITZAFMeanIonizationPotential();

   /**
    * Bloch33 - An empirical expression for J attributed to F Bloch
    */
   public static class Bloch33MeanIonizationPotential
      extends MeanIonizationPotential {
      public Bloch33MeanIonizationPotential() {
         super("Bloch 1933", "Bloch F, F. Z. Phys. 81, 363 (1933)");
      }

      @Override
      final public double compute(Element el) {
         final double z = el.getAtomicNumber();
         return ToSI.eV(13.5 * z);
      }
   }

   public static final MeanIonizationPotential Bloch33 = new Bloch33MeanIonizationPotential();

   /**
    * Wilson41 - An empirical expression for J attributed to R R Wilson
    */
   public static class Wilson41MeanIonizationPotential
      extends MeanIonizationPotential {
      public Wilson41MeanIonizationPotential() {
         super("Wilson 1941", "Wilson RR. Phys Rev. 60. 749 (1941)");
      }

      @Override
      final public double compute(Element el) {
         final double z = el.getAtomicNumber();
         return ToSI.eV(11.5 * z);
      }
   }

   public static final MeanIonizationPotential Wilson41 = new Wilson41MeanIonizationPotential();

   /**
    * Springer67 - An empirical expression for J attributed to G Springer
    */
   public static class Springer67MeanIonizationPotential
      extends MeanIonizationPotential {
      public Springer67MeanIonizationPotential() {
         super("Springer 1967", "Springer G. Meues Jahrbuch Fuer Mineralogie, Monatshefte (1967) 9/10, p. 304");
      }

      @Override
      final public double compute(Element el) {
         final double z = el.getAtomicNumber();
         return ToSI.eV(z * ((9.0 * (1.0 + Math.pow(z, -0.67))) + (0.03 * z)));
      }
   }

   public static final MeanIonizationPotential Springer67 = new Springer67MeanIonizationPotential();

   /**
    * Heinrich70 - An empirical expression for J attributed to Heinrich &amp;
    * Yakowitz. (There
    */

   public static class Heinrich70MeanIonizationPotential
      extends MeanIonizationPotential {
      public Heinrich70MeanIonizationPotential() {
         super("Heinrich & Yakowitz 1970", "Heinrich KFJ, Yakowitz H. Mikrochim Acta (1970) p 123");
      }

      @Override
      final public double compute(Element el) {
         final double z = el.getAtomicNumber();
         return ToSI.eV(z * (12.4 + (0.027 * z)));
      }
   }

   public static final MeanIonizationPotential Heinrich70 = new Heinrich70MeanIonizationPotential();

   /**
    * Duncumb69 - An empirical expression for J attributed to Duncumb &amp;
    * DeCasa
    */
   public static class Duncumb69MeanIonizationPotential
      extends MeanIonizationPotential {
      public Duncumb69MeanIonizationPotential() {
         super("Duncumb & DeCasa 1969", "Duncumb P, Shields-Mason PK, DeCasa C. Proc. 5th Int. Congr. on X-ray Optics and Microanalysis, Springer, Berlin, 1969 p. 146");
      }

      @Override
      final public double compute(Element el) {
         final double z = el.getAtomicNumber();
         return ToSI.eV((((14.0 * (1.0 - Math.exp(-0.1 * z))) + (75.5 / Math.pow(z, z / 7.5))) - (z / (100 + z))) * z);
      }
   }

   public static final MeanIonizationPotential Duncumb69 = new Duncumb69MeanIonizationPotential();

   /**
    * Zeller75 - An empirical expression for J attributed to C Zeller
    */
   public static class Zeller75MeanIonizationPotential
      extends MeanIonizationPotential {
      public Zeller75MeanIonizationPotential() {
         super("Zeller 1975", "Zeller C in Ruste J, Gantois M, J. Phys. D. Appl. Phys 8, 872 (1975)");
      }

      @Override
      final public double compute(Element el) {
         final double z = el.getAtomicNumber();
         return ToSI.eV((10.04 + (8.25 * Math.exp(-z / 11.22))) * z);
      }
   }

   public static final MeanIonizationPotential Zeller75 = new Zeller75MeanIonizationPotential();

   /**
    * Berger64 - Measured values of the mean ionization potential attributed to
    * Berger &amp; Seltzer 1964
    */
   public static class Berger64MeanIonizationPotential
      extends MeanIonizationPotential {
      Berger64MeanIonizationPotential() {
         super("Berger & Seltzer 1964", "Berger MJ, Seltzer S. NASA Technical Publication SP-4012 (1964)");
      }

      private double[] mMeasured; // nominal, in Joules

      private void readTabulatedValues() {
         try {
            synchronized(this) {
               if(mMeasured == null) {
                  final double[][] res = (new CSVReader.ResourceReader("BergerSeltzer64.csv", true)).getResource(MeanIonizationPotential.class);
                  mMeasured = new double[res.length];
                  for(int i = 0; i < res.length; ++i)
                     mMeasured[i] = (res[i].length > 0 ? ToSI.eV(res[i][0]) : 0.0);
               }
            }
         }
         catch(final Exception ex) {
            throw new EPQFatalException("Fatal error while attempting to load the mean ionization potential data file.");
         }
      }

      @Override
      final public double compute(Element el) {
         if(mMeasured == null)
            readTabulatedValues();
         return mMeasured[el.getAtomicNumber() - 1];
      }
   }

   public static final MeanIonizationPotential Berger64 = new Berger64MeanIonizationPotential();

   /**
    * Measured values of the mean ionization potential attributed to Berger
    * &amp; Seltzer 1983
    */
   public static class Berger83MeanIonizationPotential
      extends MeanIonizationPotential {
      public Berger83MeanIonizationPotential() {
         super("Berger & Seltzer 1983", "Berger MJ, Seltzer S. NBSIR 82-2550-A - US Dept of Commerce, Washington DC (1983)");
      }

      private double[] mMeasured; // nominal, in Joules

      private void readTabulatedValues() {
         try {
            final double[][] res = (new CSVReader.ResourceReader("BergerSeltzer83.csv", true)).getResource(MeanIonizationPotential.class);
            mMeasured = new double[res.length];
            for(int i = 0; i < res.length; ++i)
               mMeasured[i] = (res[i].length > 0 ? ToSI.eV(res[i][0]) : 0.0);
         }
         catch(final Exception ex) {
            throw new EPQFatalException("Fatal error while attempting to load the mean ionization potential data file.");
         }
      }

      @Override
      final public double compute(Element el) {
         if(mMeasured == null)
            readTabulatedValues();
         return mMeasured[el.getAtomicNumber() - 1];
      }
   }

   public static final MeanIonizationPotential Berger83 = new Berger83MeanIonizationPotential();

   static private final AlgorithmClass[] mAllImplementations = {
      MeanIonizationPotential.Berger64,
      MeanIonizationPotential.Berger83,
      MeanIonizationPotential.Bloch33,
      MeanIonizationPotential.Duncumb69,
      MeanIonizationPotential.BergerAndSeltzerCITZAF,
      MeanIonizationPotential.Heinrich70,
      MeanIonizationPotential.Springer67,
      MeanIonizationPotential.Sternheimer64,
      MeanIonizationPotential.Wilson41,
      MeanIonizationPotential.Zeller75
   };

}
