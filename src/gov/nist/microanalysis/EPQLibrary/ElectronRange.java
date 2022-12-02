package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * A class that implements various different algorithms to calculate the
 * ElectronRange in a bulk material. The SI units for electron range are meters
 * * kg/meter^3 = kg / meter^2.
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

abstract public class ElectronRange extends AlgorithmClass {

   static private double averageAtomicWeight(Composition comp) {
      double res = 0.0;
      for (final Element elm : comp.getElementSet())
         res += comp.weightFraction(elm, true) * elm.getAtomicWeight();
      return res;
   }

   /**
    * Converts to the traditional electron range in cm per (g/cm^3) from meters
    * per (kg/meter^3)
    * 
    * @param x
    * @return double
    */
   static public double toGramPerCmSqr(double x) {
      return (x * FromSI.GRAM) / (FromSI.CM * FromSI.CM);
   }

   /**
    * Converts from the traditional electron range in cm (g/cm^3) to meters
    * (kg/meter^3)
    * 
    * @param x
    * @return double
    */
   static public double toKilogramPerMeterSqr(double x) {
      return (x * ToSI.GRAM) / (ToSI.CM * ToSI.CM);
   }

   /**
    * ElectronRange - Create an instance of the abstract ElectronRange class
    * with the specified name and literature reference.
    * 
    * @param name
    *           String
    * @param reference
    *           String
    */
   private ElectronRange(String name, String reference) {
      super("Electron Range", name, reference);
   }

   @Override
   protected void initializeDefaultStrategy() {
      addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.Berger83);
   }

   /**
    * getAllImplementations - Returns a full list of all available algorithms.
    * Each item is an implements the ElectronRange abstract class.
    * 
    * @return List
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   public String caveat(Composition comp, double e0) {
      return CaveatBase.None;
   }

   /**
    * Computes the electron range using the current algorithm. To compute the
    * range of an electron in a Material divide the results by
    * Material.getDensity().
    * 
    * @param comp
    *           The composition
    * @param e0
    *           The beam energy in Joules
    * @return The result is in meters * (kg/meter^3)
    */
   abstract public double compute(Composition comp, double e0);

   /**
    * Computes the electron range for an electron of incident energy e0 until it
    * drops below the edge energy for the specified shell in the specified
    * material.
    * 
    * @param comp
    *           The material's composition
    * @param shell
    *           The shell defining an edge energy
    * @param e0
    *           The incident beam energy
    * @return The range in meters * (kg/meter^3)
    */
   public double compute(Composition comp, AtomicShell shell, double e0) {
      return compute(comp, e0) - compute(comp, shell.getEdgeEnergy());
   }

   /**
    * Computes the electron range using the current algorithm. To compute the
    * range of an electron in a Material divide the results by
    * Material.getDensity().
    * 
    * @param mat
    *           The composition
    * @param e0
    *           The beam energy in Joules
    * @return The result is in meters
    */
   public double computeMeters(Material mat, double e0) {
      return compute((Composition) mat, e0) / mat.getDensity();
   }

   public static class PouchouElectronRange extends ElectronRange {
      PouchouElectronRange() {
         super("Pouchou & Pichoir 1991", "Electron Probe Microanalysis, Heinrich & Newbury (eds)");
      }

      @Override
      public String caveat(Composition comp, double e0) {
         if (e0 > ToSI.keV(50.0))
            return CaveatBase.append(CaveatBase.None, "Not designed for use above 50.0 keV");
         return CaveatBase.None;
      }

      @Override
      protected void initializeDefaultStrategy() {
         super.initializeDefaultStrategy();
         addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.Zeller75);
      }

      @Override
      public double compute(Composition comp, double e0) {
         final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
         double bigM = 0.0;
         for (final Element el : comp.getElementSet())
            bigM += (comp.weightFraction(el, true) * el.getAtomicNumber()) / el.getAtomicWeight();
         final double j = FromSI.keV(mip.computeLn(comp));
         final double d[] = {6.6e-6, 1.12e-5 * (1.35 - (0.45 * j * j)), 2.2e-6 / j};
         final double p[] = {0.78, 0.1, -(0.5 - (0.25 * j))};
         double tmp = 0.0;
         final double e0keV = FromSI.keV(e0);
         for (int k = 0; k < 3; ++k)
            tmp += (Math.pow(j, 1.0 - p[k]) * d[k] * Math.pow(e0keV, 1.0 + p[k])) / (1.0 + p[k]);
         // in length * density
         return toKilogramPerMeterSqr((1.0 / bigM) * tmp);
      }
   }

   public static final ElectronRange Pouchou1991 = new PouchouElectronRange();

   public static class LoveElectronRange extends ElectronRange {
      LoveElectronRange() {
         super("Love et al 1979", "Love et al as quoted in Reed in Electron Probe Analysis 2nd ed.");
      }

      @Override
      protected void initializeDefaultStrategy() {
         super.initializeDefaultStrategy();
         addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.Zeller75);
      }

      @Override
      public double compute(Composition comp, double e0) {
         final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
         final double a = averageAtomicWeight(comp);
         final double z = comp.meanAtomicNumber();
         final double j = FromSI.keV(mip.computeLn(comp));
         e0 = FromSI.keV(e0);
         return toKilogramPerMeterSqr((a / z) * ((7.73e-6 * Math.sqrt(j) * Math.pow(e0, 1.5)) + (7.35e-7 * e0 * e0)));
      }
   }

   public static final ElectronRange LoveEtAl1978 = new LoveElectronRange();

   public static class KanayaAndOkayamaElectronRange extends ElectronRange {
      KanayaAndOkayamaElectronRange() {
         super("Kanaya & Okayama 1972", "Kanaya & Okayama 1972 as quoted in Reed in Electron Probe Analysis 2nd ed.");
      }

      private double compute(Element elm, double e0) {
         return toKilogramPerMeterSqr(
               (1.0e-4 * 0.0276 * elm.getAtomicWeight() * Math.pow(FromSI.keV(e0), 1.67)) / (Math.pow(elm.getAtomicNumber(), 0.89)));
      }

      @Override
      public double compute(Composition comp, double e0) {
         double sum = 0.0;
         for (final Element elm : comp.getElementSet())
            sum += comp.weightFraction(elm, true) / compute(elm, e0);
         return 1.0 / sum;
      }

   }

   public static final ElectronRange KanayaAndOkayama1972 = new KanayaAndOkayamaElectronRange();

   public static final ElectronRange Default = Pouchou1991;

   static private AlgorithmClass[] mAllImplementations = {ElectronRange.KanayaAndOkayama1972, ElectronRange.LoveEtAl1978, ElectronRange.Pouchou1991};

}
