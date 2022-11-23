package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * The backscatter coefficient is often called 'eta'. It represents the fraction
 * of incident electrons which backscatter out of the sample.
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
abstract public class BackscatterCoefficient
   extends
   AlgorithmClass {

   protected BackscatterCoefficient(String name, LitReference ref) {
      super("Backscatter Coefficient", name, ref);
   }

   /**
    * Returns a List of all implementations of this algorithm
    * 
    * @return List
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#getAllImplementations()
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   /**
    * Compute eta, the backscatter coefficient, which represents the fraction of
    * incident electrons which will backscatter from a sample of pure Element
    * el.
    * 
    * @param el The element
    * @param e0 Beam energy (Joules)
    * @return double on [0.0 to 1.0]
    */
   abstract public double compute(Element el, double e0);

   public String caveat(Element el, double e0) {
      return CaveatBase.None;
   }

   public String caveat(Composition comp, double e0) {
      String res = CaveatBase.None;
      for(final Element el : comp.getElementSet())
         res = CaveatBase.append(res, caveat(el, e0));
      return res;
   }

   /**
    * Compute eta, the backscatter coefficient, which represents the fraction of
    * incident electrons which will backscatter from a sample of the specified
    * composition. (See Heinrich81 EBXM eqn 9.3.3)
    * 
    * @param comp The composition of the material
    * @param e0 Beam energy (Joules)
    * @return double on [0.0,1.0]
    */
   public double compute(Composition comp, double e0) {
      double eta = 0.0;
      for(final Element el : comp.getElementSet())
         eta += comp.weightFraction(el, true) * compute(el, e0);
      assert (eta >= 0.0);
      assert (eta <= 1.0);
      return eta;
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#initializeDefaultStrategy()
    */
   @Override
   protected void initializeDefaultStrategy() {
      // Don't do anything..
   }

   public static class HeinrichBackscatterCoefficient
      extends
      BackscatterCoefficient {
      HeinrichBackscatterCoefficient() {
         super("Heinrich81", LitReference.ElectronBeamXRayMicroanalysis);
      }

      @Override
      public double compute(Element el, double e0) {
         final double z = el.getAtomicNumber();
         return 0.500 - (0.228e-4 * (80.0 - z) * Math.pow(Math.abs(80.0 - z), 1.3));
      }
   }

   static public BackscatterCoefficient Heinrich81 = new HeinrichBackscatterCoefficient();

   public static class PouchoAndPichoirBackscatterCoefficient
      extends
      BackscatterCoefficient {

      PouchoAndPichoirBackscatterCoefficient() {
         super("Pouchou & Pichoir", LitReference.PAPinEPQ);
      }

      private double computeZp(double zp) {
         return (1.75e-3 * zp) + (0.37 * (1.0 - Math.exp(-0.015 * Math.pow(zp, 1.3))));
      }

      @Override
      public double compute(Composition comp, double e0) {
         double zp = 0.0;
         // If we don't normalize the weight fraction here then the zp
         // blows up when the analytical total is substantially
         // above unity.
         final double total = Math.min(1.1, comp.sumWeightFraction());
         for(final Element el : comp.getElementSet())
            zp += (comp.weightFraction(el, false) / total) * Math.sqrt(el.getAtomicNumber());
         return computeZp(zp * zp);
      }

      @Override
      public double compute(Element el, double e0) {
         return computeZp(el.getAtomicNumber());
      }
   }

   static public BackscatterCoefficient PouchouAndPichoir91 = new PouchoAndPichoirBackscatterCoefficient();

   public static class LoveBackscatterCoefficient
      extends
      BackscatterCoefficient {
      LoveBackscatterCoefficient() {
         super("Love & Scott 1978", LitReference.LoveScott1978);
      }

      @Override
      public double compute(Element el, double e0) {
         // From CITZAF 3.06
         // H1 = (-52.3791 + 150.48371# * Y - 1.67373 * Y ^ 2 + .00716 * Y ^ 3)
         // / 10000
         // H2 = (-1112.8 + 30.289 * Y - .15498 * Y ^ 2) / 10000
         // H1 = H1 * (1 + H2 * LOG(E0(I) / 20))
         final double z = el.getAtomicNumber();
         final double e0kV = FromSI.keV(e0);
         final double eta20 = -52.3791e-4 + (z * (150.48371e-4 + (z * (-1.67373e-4 + (z * 0.00716e-4)))));
         final double hZoEta20 = -1112.8e-4 + (z * (30.289e-4 + (z * -0.15498e-4)));
         return eta20 * (1.0 + (hZoEta20 * Math.log(e0kV / 20.0)));
      }
   }

   static public BackscatterCoefficient Love1978 = new LoveBackscatterCoefficient();

   static private AlgorithmClass[] mAllImplementations = {
      Heinrich81,
      PouchouAndPichoir91,
      Love1978
   };
}