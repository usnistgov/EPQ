package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Various implementations of the A portion of the Z*A*F correction scheme.
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
abstract public class AbsorptionCorrection
   extends AlgorithmClass {

   private static final AlgorithmClass[] mAllImplementations = {
      AbsorptionCorrection.Philibert,
      AbsorptionCorrection.PhilibertHeinrich,
      AbsorptionCorrection.PhilibertHeinrichFull
   };

   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   @Override
   protected void initializeDefaultStrategy() {
      addDefaultAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Chantler2005);
   }

   protected AbsorptionCorrection(String name, LitReference ref) {
      super("Absorption Correction", name, ref);
   }

   protected double computeChi(XRayTransition xrt, Composition comp, double takeOffAngle)
         throws EPQException {
      final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
      return mac.compute(comp, xrt) / Math.sin(takeOffAngle);
   }

   abstract public double compute(Composition comp, XRayTransition xrt, double e0, double takeOffAngle)
         throws EPQException;

   public static class PhilibertAbsorption
      extends AbsorptionCorrection {
      PhilibertAbsorption() {
         super("Philibert", LitReference.QuantitativeElectronProbeMicroanalysis);
      }

      @Override
      public double compute(Composition comp, XRayTransition xrt, double e0, double takeOffAngle)
            throws EPQException {
         final double chi = MassAbsorptionCoefficient.toCmSqrPerGram(computeChi(xrt, comp, takeOffAngle));
         final double sigma = 2.54e5 / (Math.pow(FromSI.keV(e0), 1.5) - Math.pow(FromSI.keV(xrt.getEdgeEnergy()), 1.5));
         double h = 0;
         for(final Element el : comp.getElementSet()) {
            final double hEl = (4.5 * el.getAtomicWeight()) / Math2.sqr(el.getAtomicNumber());
            h += comp.weightFraction(el, true) * hEl;
         }
         if(true)
            return ((1 + h) * (sigma * sigma)) / ((sigma + chi) * ((sigma * (1.0 + h)) + (h * chi)));
         else
            return 1.0 / ((1.0 + (chi / sigma)) * (1.0 + ((h * (chi / sigma)) / (1.0 + h))));
      }
   }

   static public AbsorptionCorrection Philibert = new PhilibertAbsorption();

   /**
    * Philibert's formula as modified by Heinrich. (This is the model that JEOL
    * claims to implement (as per the XM17330/27330 documentation dated
    * Dec-2001)
    */
   static public PhilibertHeinrichAbsorption PhilibertHeinrich = new PhilibertHeinrichAbsorption();

   static public class PhilibertHeinrichAbsorption
      extends AbsorptionCorrection {

      public PhilibertHeinrichAbsorption() {
         super("Philibert/Heinrich (Simplified)", LitReference.QuantitativeElectronProbeMicroanalysis);
      }

      @Override
      public double compute(Composition comp, XRayTransition xrt, double e0, double takeOffAngle)
            throws EPQException {
         final double chi = MassAbsorptionCoefficient.toCmSqrPerGram(computeChi(xrt, comp, takeOffAngle));
         final double sigma = 4.5e5 / (Math.pow(FromSI.keV(e0), 1.65) - Math.pow(FromSI.keV(xrt.getEdgeEnergy()), 1.65));
         double h = 0;
         for(final Element el : comp.getElementSet()) {
            final double hEl = (1.2 * el.getAtomicWeight()) / Math2.sqr(el.getAtomicNumber());
            h += comp.weightFraction(el, true) * hEl;
         }
         return ((1 + h) * (sigma * sigma)) / ((sigma + chi) * (sigma + (h * (sigma + chi))));
      }

      /**
       * Estimates the uncertainty in the absorption correction due to
       * uncertainty in chi.
       * 
       * @param comp
       * @param xrt
       * @param e0
       * @param takeOffAngle
       * @return dF where F=compute(comp,xrt,e0,takeOffAngle)
       * @throws EPQException
       */
      public double uncertainty(Composition comp, XRayTransition xrt, double e0, double takeOffAngle)
            throws EPQException {
         final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
         final double chi = MassAbsorptionCoefficient.toCmSqrPerGram(computeChi(xrt, comp, takeOffAngle));
         final double dChi = chi * mac.fractionalUncertainty(comp, xrt);
         final double sigma = 4.5e5 / (Math.pow(FromSI.keV(e0), 1.65) - Math.pow(FromSI.keV(xrt.getEdgeEnergy()), 1.65));
         double h = 0;
         for(final Element el : comp.getElementSet()) {
            final double hEl = (1.2 * el.getAtomicWeight()) / Math2.sqr(el.getAtomicNumber());
            h += comp.weightFraction(el, true) * hEl;
         }
         final double spc = sigma + chi;
         final double df_dchi = -((1.0 + h) * sigma * sigma * (sigma + (2.0 * h * spc))) / Math2.sqr(spc * (sigma + (h * spc)));
         return Math.abs(df_dchi * dChi);
      }
   };

   public static class PhilibertHeinrichFullAbsorption
      extends AbsorptionCorrection {
      PhilibertHeinrichFullAbsorption() {
         super("Philibert/Heinrich (Full)", LitReference.QuantitativeElectronProbeMicroanalysis);
      }

      @Override
      protected void initializeDefaultStrategy() {
         super.initializeDefaultStrategy();
         addDefaultAlgorithm(SurfaceIonization.class, SurfaceIonization.Reuter1972);
      }

      @Override
      public double compute(Composition comp, XRayTransition xrt, double e0, double takeOffAngle)
            throws EPQException {
         final double chi = MassAbsorptionCoefficient.toCmSqrPerGram(computeChi(xrt, comp, takeOffAngle));
         final double sigma = 4.5e5 / (Math.pow(FromSI.keV(e0), 1.65) - Math.pow(FromSI.keV(xrt.getEdgeEnergy()), 1.65));
         double h = 0;
         for(final Element el : comp.getElementSet()) {
            final double hEl = (1.2 * el.getAtomicWeight()) / Math2.sqr(el.getAtomicNumber());
            h += comp.weightFraction(el, true) * hEl;
         }
         final SurfaceIonization si = (SurfaceIonization) getAlgorithm(SurfaceIonization.class);
         final double phi0 = si.compute(comp, xrt.getDestination(), e0);
         return (1.0 + ((phi0 * h * chi) / ((4.0 + (phi0 * h)) * sigma)))
               / ((1.0 + (chi / sigma)) * (1.0 + ((h * (chi / sigma)) / (1.0 + h))));
      }

   }

   static public AbsorptionCorrection PhilibertHeinrichFull = new PhilibertHeinrichFullAbsorption();
}
