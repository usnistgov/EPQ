/**
 * <p>
 * An implementation of IRandomizedScatter that is designed to handle small
 * angle scattering using inelastic scattter in addition to the large angle
 * elastic scattering.
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

import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Scattering in a gas is qualitatively different from scattering in a solid.
 * Typically the path length between scattering events is much larger in a gas.
 * In a gas, small angular deviations can lead to large spatial deviations. In a
 * solid, many random small angular deviations eventually average to zero while
 * in a gas the number of small angular deviations is much fewer.
 * </p>
 * <p>
 * The treatment is based on electron scattering as detailed in R. F. Egerton's
 * book "Electron Energy-Loss Spectroscopy in the Electron Microscope", Second
 * Edition, Plenum Press, NY &amp; London, 1996.
 * </p>
 */
public class GasScatteringCrossSection extends RandomizedScatter {

   static LitReference.Book REFERENCE = new LitReference.Book("Electron Energy-Loss Spectroscopy in the Electron Microscope, Second Edition",
         "Plenum Press, NY & London", 1996, new LitReference.Author[]{LitReference.RFEdgerton});

   public static class GasScatteringRandomizedScatterFactory extends RandomizedScatterFactory {
      GasScatteringRandomizedScatterFactory() {
         super("Gas scattering algorithm", REFERENCE);
      }

      private final RandomizedScatter[] mScatter = new RandomizedScatter[Element.elmEndOfElements];

      /**
       * @see gov.nist.microanalysis.EPQLibrary.RandomizedScatterFactory#get(gov.nist.microanalysis.EPQLibrary.Element)
       */
      @Override
      public RandomizedScatter get(Element elm) {
         final int z = elm.getAtomicNumber();
         if (mScatter[z] == null)
            mScatter[z] = new GasScatteringCrossSection(elm);
         return mScatter[z];
      }

      @Override
      protected void initializeDefaultStrategy() {
         // Nothing to do...
      }
   }

   public static final RandomizedScatterFactory Factory = new GasScatteringRandomizedScatterFactory();

   private final Element mElement;
   private RandomizedScatter mElastic;

   private static final double E0 = PhysicalConstants.ElectronMass * PhysicalConstants.SpeedOfLight * PhysicalConstants.SpeedOfLight;

   /**
    * Constructs a GasScatteringCrossSection object
    */
   public GasScatteringCrossSection(Element elm) {
      super("Edgerton gas cross-section", REFERENCE);
      mElement = elm;
      mElastic = new ScreenedRutherfordScatteringAngle(mElement);
   }

   /**
    * setElasticModel - Specify which elastic cross section model to use.
    * Default is ScreenedRutherfordScatteringAngle.
    * 
    * @param elastic
    */
   public void setElasticModel(RandomizedScatter elastic) {
      if (!elastic.getElement().equals(mElement))
         throw new IllegalArgumentException("The element for the elastic model must match the element for the inelastic model.");
      mElastic = elastic;
   }

   /**
    * getElasticModel - Returns an instance of the model currrently being used
    * to model elastic events.
    * 
    * @return IRandomizedScatter
    */
   public RandomizedScatter getElasticModel() {
      return mElastic;
   }

   /**
    * ratioInelasticOverElastic - Edgerton gives the ratio sigmi_i / sigma_e ~
    * 20/Z based on experiment (pg 145).
    * 
    * @return double
    */
   public double ratioInelasticOverElastic() {
      return 20.0 / mElement.getAtomicNumber();
   }

   /**
    * getElement - The element with which this cross section is associated.
    * 
    * @return Element
    * @see gov.nist.microanalysis.EPQLibrary.RandomizedScatter#getElement()
    */
   @Override
   public Element getElement() {
      return mElement;
   }

   /**
    * totalCrossSection - Computes the total cross section from the sum of
    * elastic and inelastic contributions.
    * 
    * @param energy
    * @return double
    * @see gov.nist.microanalysis.EPQLibrary.RandomizedScatter#totalCrossSection(double)
    */
   @Override
   public double totalCrossSection(double energy) {
      return (1.0 + ratioInelasticOverElastic()) * mElastic.totalCrossSection(energy);
   }

   /**
    * randomScatteringAngle - Picks a random scattering angle based on the
    * distribution of scattering angles.
    * 
    * @param energy
    * @return double
    * @see gov.nist.microanalysis.EPQLibrary.RandomizedScatter#randomScatteringAngle(double)
    */
   @Override
   public double randomScatteringAngle(double energy) {
      if ((Math.random() * (1.0 + ratioInelasticOverElastic())) < 1.0)
         return mElastic.randomScatteringAngle(energy);
      else {
         // Electron velocity from energy
         final double v = PhysicalConstants.SpeedOfLight * Math.sqrt(1.0 - (1.0 / Math2.sqr((energy / E0) + 1.0)));
         // Compute gamma, wave vector magnitude and atomic radius
         final double g = Math.sqrt(1.0 - Math2.sqr(v / PhysicalConstants.SpeedOfLight));
         final double k0 = (g * PhysicalConstants.ElectronMass * v) / PhysicalConstants.PlanckReduced;
         final double rz = PhysicalConstants.BohrRadius * Math.pow(mElement.getAtomicNumber(), -0.3333333);
         // Two characteristic angles
         final double thE2 = Math2.sqr(mElement.getIonizationEnergy() / (g * PhysicalConstants.ElectronMass * v * v)); // unitless
         final double th02 = Math2.sqr(1.0 / (k0 * rz));
         // Compute the maximum integrated cross section
         final double siInt = Math.log((((Math.PI * Math.PI) + thE2) * (th02 + thE2)) / (thE2 * ((Math.PI * Math.PI) + th02 + thE2)));
         assert siInt > 0.0 : Double.toString(siInt);
         // Select a random integrated cross section
         final double exp_si = Math.exp(Math.random() * siInt);
         assert exp_si >= 1.0;
         // Solve for the angle that give us this (via Egerton 3.16)
         final double beta = Math.sqrt(((1 - exp_si) * thE2 * (th02 + thE2)) / (((exp_si - 1) * thE2) - th02));
         assert (beta >= 0) && (beta <= Math.PI) : Double.toString(beta);
         return beta;
      }
   }
}
