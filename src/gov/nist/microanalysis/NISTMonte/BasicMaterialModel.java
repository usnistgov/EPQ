/**
 * <p>
 * Implements the IMaterialScatteringModel as a simple elastic scattering with
 * continuous slowing down energy loss.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nritchie
 * @version 1.0
 */
package gov.nist.microanalysis.NISTMonte;

import gov.nist.microanalysis.EPQLibrary.AlgorithmUser;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Gas;
import gov.nist.microanalysis.EPQLibrary.GasScatteringCrossSection;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.NISTMottScatteringAngle;
import gov.nist.microanalysis.EPQLibrary.RandomizedScatterFactory;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase;
import gov.nist.microanalysis.Utility.Math2;

public class BasicMaterialModel
   extends AlgorithmUser
   implements IMaterialScatterModel {
   private final Material mMaterial;
   private double minEforTracking = ToSI.eV(50.0);

   /**
    * Constructs a BasicMaterialModel for the specified Material using the
    * NISTMottScatteringAngle scattering algorithm.
    * 
    * @param mat
    */
   public BasicMaterialModel(Material mat)
         throws EPQException {
      super();
      mMaterial = mat;
      if(mMaterial instanceof Gas)
         addDefaultAlgorithm(RandomizedScatterFactory.class, GasScatteringCrossSection.Factory);
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.IMaterialScatterModel#getMaterial()
    */
   @Override
   public Material getMaterial() {
      return mMaterial;
   }

   /*
    * @seegov.nist.microanalysis.NISTMonte.MonteCarloSS.IMaterialScatterModel#
    * randomMeanPathLength
    * (gov.nist.microanalysis.NISTMonte.MonteCarloSS.Electron)
    */
   @Override
   public double randomMeanPathLength(Electron pe) {
      // Ref: Heinrich 1981 p 458
      final double kE = pe.getEnergy();
      double minMfp = 1.0;
      Element bestEl = Element.None;
      final double den = mMaterial.getDensity();
      final RandomizedScatterFactory rsf = (RandomizedScatterFactory) getAlgorithm(RandomizedScatterFactory.class);
      assert rsf != null;
      for(final Element el : mMaterial.getElementSet()) {
         final double mfp = (el.getMass() * Math2.expRand())
               / (den * mMaterial.weightFraction(el, true) * rsf.get(el).totalCrossSection(kE));
         if(mfp < minMfp) {
            minMfp = mfp;
            bestEl = el;
         }
      }
      pe.setScatteringElement(bestEl);
      return minMfp;
   }

   /*
    * @see
    * gov.nist.microanalysis.NISTMonte.MonteCarloSS.IMaterialScatterModel#scatter
    * (gov.nist.microanalysis.NISTMonte.MonteCarloSS.Electron)
    */
   @Override
   public Electron scatter(Electron pe) {
      final Element se = pe.getScatteringElement();
      if((se != null) && (se != Element.None)) {
         final RandomizedScatterFactory rsf = (RandomizedScatterFactory) getAlgorithm(RandomizedScatterFactory.class);
         assert rsf != null;
         final double alpha = rsf.get(se).randomScatteringAngle(pe.getEnergy());
         final double beta = 2.0 * Math.PI * Math2.rgen.nextDouble();
         // Update the primary electron's direction angles
         // We could pe.setEnergy() here, but this is an elastic model
         // so it is not necessary.
         pe.updateDirection(alpha, beta);
      }
      return null; // --no SE generation in this model
   }

   /*
    * @seegov.nist.microanalysis.NISTMonte.MonteCarloSS.IMaterialScatterModel#
    * barrierScatter(gov.nist.microanalysis.NISTMonte.MonteCarloSS.Electron,
    * gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase)
    */
   @Override
   public Electron barrierScatter(Electron pe, RegionBase nextRegion) {
      pe.setCurrentRegion(nextRegion);
      pe.setScatteringElement(null);
      return null;
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.IMaterialScatterModel#calculateEnergyLoss(double,
    *      gov.nist.microanalysis.NISTMonte.Electron)
    */
   @Override
   public double calculateEnergyLoss(double len, Electron pe) {
      // See Heinrich 1981 pp 226-227
      final double kE = pe.getEnergy();
      double res = 0.0;
      for(final Element el : mMaterial.getElementSet())
         res += AlgorithmUser.getDefaultBetheEnergyLoss().compute(el, kE) * mMaterial.weightFraction(el, true);
      return res * mMaterial.getDensity() * len;
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.IMaterialScatterModel#getMinEforTracking()
    */
   @Override
   public double getMinEforTracking() {
      return minEforTracking;
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.IMaterialScatterModel#setMinEforTracking(double)
    */
   @Override
   public void setMinEforTracking(double minEforTracking) {
      this.minEforTracking = minEforTracking;
   }

   @Override
   protected void initializeDefaultStrategy() {
      // addDefaultAlgorithm(RandomizedScatterFactory.class,
      // ScreenedRutherfordScatteringAngle.Factory);
      addDefaultAlgorithm(RandomizedScatterFactory.class, NISTMottScatteringAngle.Factory);
   }
}