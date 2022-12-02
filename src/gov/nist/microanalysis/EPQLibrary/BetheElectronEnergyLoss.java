package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * This class provides various different implementations for the electron energy
 * loss expression in the form first described by Bethe. The units of the result
 * of compute are energy/length or Joules/meter
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */
abstract public class BetheElectronEnergyLoss extends AlgorithmClass {

   private static final AlgorithmClass[] mAllImplementations = {BetheElectronEnergyLoss.Bethe1930, BetheElectronEnergyLoss.Bethe1930Strict,
         BetheElectronEnergyLoss.JoyLuo1989};

   /**
    * Constructs a BetheElectronEnergyLoss
    */
   protected BetheElectronEnergyLoss(String name, LitReference ref) {
      super("Stopping power", name, ref);
   }

   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   @Override
   protected void initializeDefaultStrategy() {
      addDefaultAlgorithm(MeanIonizationPotential.class, MeanIonizationPotential.Berger83);
   }

   /***
    * Converts from SI to (eV/A) per (g/cm^3)
    * 
    * @param val
    * @return val in (eV/A) per (g/cm^3)
    */
   public static double toNatural(double val) {
      // (J*m^2/kg)*(eV/J)*(A/m)/(g/kg) => eV/A
      return val * (FromSI.EV / FromSI.ANGSTROM) / (FromSI.GRAM / Math.pow(FromSI.CM, 3.0));
   }

   /**
    * Converts from (eV/A) per (g/cm^3) to SI units.
    * 
    * 
    * @param val
    * @return val in SI (K/M) per (kg/m^3)
    */
   public static double fromNatural(double val) {
      return val * (ToSI.EV / ToSI.ANGSTROM) / (ToSI.GRAM / Math.pow(ToSI.CM, 3.0));
   }
   /**
    * compute - Computes dE/ds in Joules/meter per kg/meter^3
    * 
    * @param elm
    *           An element
    * @param eB
    *           The electron energy (in Joules)
    * @return dE/ds in Joules per meter per kg/meter^3
    */
   abstract public double compute(Element elm, double eB);

   /**
    * JoyLuo1989 - Joy &amp; Luo's modification to the Bethe expression to
    * improve the applicability at low-beam energies. (As described in Scanning
    * Electron Microscopy and X-ray Microanalysis (3rd edition))
    */
   static public class JoyLuoBetheElectronEnergyLoss extends BetheElectronEnergyLoss {
      // Some minor caching optimizations
      transient private final double[] mK = new double[Element.elmEndOfElements];

      JoyLuoBetheElectronEnergyLoss() {
         super("Joy-Luo", LitReference.Goldstein);
         for (int z = Element.elmH; z < Element.elmEndOfElements; ++z)
            mK[z] = 0.731 + 0.0688 * Math.log10(z);
      }

      @Override
      public double compute(Element el, double eB) {
         final int z = el.getAtomicNumber();
         final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
         final double j = FromSI.eV(mip.compute(el)), e = FromSI.eV(eB);
         final double k = 0.731 + 0.0688 * Math.log10(z);
         final double j_star = j / (1.0 + k * j / e);
         return fromNatural(((-785.0 * z) / (el.getAtomicWeight() * e)) * Math.log(1.166 * e / j_star));
      }
   };

   static public BetheElectronEnergyLoss JoyLuo1989 = new JoyLuoBetheElectronEnergyLoss();

   /**
    * Bethe1930 - The original expression of Bethe for the stopping power
    * adjusted so that even when the electron energy falls below about J/1.166,
    * the electron continues to decelerate (albeit slowly).
    */
   static public class Bethe30ModElectronEnergyLoss extends BetheElectronEnergyLoss {
      Bethe30ModElectronEnergyLoss() {
         super("Bethe(Modified)", new LitReference.CrudeReference("Bethe H. Ann. Phys. (Leipzig) 1930; 5: 325"));
      }

      @Override
      public double compute(Element el, double eB) {
         final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
         final double e_eV = FromSI.eV(eB);
         final double j = FromSI.eV(mip.compute(el));
         double f = (1.166 * e_eV) / j;
         // The low energy modification...
         if (f < 1.1)
            f = 1.1;
         return fromNatural(((-785.0 * el.getAtomicNumber()) / (el.getAtomicWeight() * e_eV)) * Math.log(f));
      }
   }

   static public final BetheElectronEnergyLoss Bethe1930 = new Bethe30ModElectronEnergyLoss();

   /**
    * Bethe1930Strict - The original expression of Bethe for the stopping power.
    * Below eB = J/1.166 the energy loss goes positive (the electron shows
    * unphysical acceleration.)
    */
   static public class Bethe30ElectronEnergyLoss extends BetheElectronEnergyLoss {
      Bethe30ElectronEnergyLoss() {
         super("Bethe", new LitReference.CrudeReference("Bethe H. Ann. Phys. (Leipzig) 1930; 5: 325"));
      }

      @Override
      public double compute(Element el, double eB) {
         final MeanIonizationPotential mip = (MeanIonizationPotential) getAlgorithm(MeanIonizationPotential.class);
         final double e_eV = FromSI.eV(eB);
         final double j = FromSI.eV(mip.compute(el));
         final double f = (1.166 * e_eV) / j;
         return fromNatural(((-785.0 * el.getAtomicNumber()) / (el.getAtomicWeight() * e_eV)) * Math.log(f));
      }

   }

   static public BetheElectronEnergyLoss Bethe1930Strict = new Bethe30ElectronEnergyLoss();

}