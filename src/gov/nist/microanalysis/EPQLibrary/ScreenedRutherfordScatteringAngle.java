/**
 * <p>
 * Implements the IElasticCrossSection interface for the screened Rutherford
 * model of elastic scattered from a screened nucleus.
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
package gov.nist.microanalysis.EPQLibrary;

/**
 * Implements the IElasticCrossSection interface for the screened Rutherford
 * model of elastic scattered from a screened nucleus.
 */
public class ScreenedRutherfordScatteringAngle
   extends RandomizedScatter {

   static private final LitReference REFERENCE = new LitReference.CrudeReference("NBSMONTE.FOR");

   public static class ScreenedRutherfordRandomizedScatterFactory
      extends RandomizedScatterFactory {
      public ScreenedRutherfordRandomizedScatterFactory() {
         super("Screened Rutherford elastic cross-section", REFERENCE);
      }

      private final RandomizedScatter[] mScatter = new RandomizedScatter[Element.elmEndOfElements];

      /**
       * @see gov.nist.microanalysis.EPQLibrary.RandomizedScatterFactory#get(gov.nist.microanalysis.EPQLibrary.Element)
       */
      @Override
      public RandomizedScatter get(Element elm) {
         final int z = elm.getAtomicNumber();
         if(mScatter[z] == null)
            mScatter[z] = new ScreenedRutherfordScatteringAngle(elm);
         return mScatter[z];
      }

      @Override
      protected void initializeDefaultStrategy() {
         // Nothing to do...
      }
   }

   public static final RandomizedScatterFactory Factory = new ScreenedRutherfordRandomizedScatterFactory();

   private final Element mElement;

   /**
    * Constructs a ScreenedRutherfordScatteringAngle
    */
   public ScreenedRutherfordScatteringAngle(Element elm) {
      super("Screened Rutherford", REFERENCE);
      mElement = elm;
   }

   /**
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "CrossSection[Screened-Rutherford," + mElement.toAbbrev() + "]";
   }

   /**
    * Returns the Element associated with this instance of the Rutherford
    * scattering cross section.
    * 
    * @return Element
    */
   @Override
   public Element getElement() {
      return mElement;
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.RandomizedScatter#totalCrossSection(double)
    */
   @Override
   public double totalCrossSection(double energy) {
      // Ref: Heinrich 1981 p 459 convert to SI units
      final double z = mElement.getAtomicNumber();
      final double zp = Math.pow(z, 1.0 / 3.0);
      return (7.670843088080456e-38 * zp * (1.0 + z)) / ((energy + ((5.44967975966321e-19 * zp * zp))));
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.RandomizedScatter#randomScatteringAngle(double)
    */
   @Override
   public double randomScatteringAngle(double energy) {
      // This method for calculating the scattering angle is taken from
      // NBSMONTE.FOR
      final double alpha = (5.44968e-19 * Math.pow(mElement.getAtomicNumber(), 2.0 / 3.0)) / energy;
      final double r = Math.random();
      return Math.acos(1 - ((2.0 * alpha * r) / ((1 + alpha) - r)));
   }
}
