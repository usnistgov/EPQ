package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Provides details as to how the sample has been prepared for analysis. These
 * preparations can include polishing, coating, etching, ....
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
public abstract class SamplePreparation {

   private static Material CARBON = createCarbon();

   private static final Material createCarbon() {
      final Material res = new Material(Element.C, ToSI.gPerCC(2.0));
      res.setName("carbon");
      return res;
   }

   private final double mScale;

   protected SamplePreparation(double scale) {
      mScale = scale;
   }

   /**
    * Returns the scale of the texture on the surface
    * 
    * @return in meters
    */
   public double getTextureScale() {
      return mScale;
   }

   /**
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      long temp;
      temp = Double.doubleToLongBits(mScale);
      result = (prime * result) + (int) (temp ^ (temp >>> 32));
      return result;
   }

   /**
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (!(obj instanceof SamplePreparation))
         return false;
      final SamplePreparation other = (SamplePreparation) obj;
      if (Double.doubleToLongBits(mScale) != Double.doubleToLongBits(other.mScale))
         return false;
      return true;
   }

   abstract public double score();

   /**
    * An user-friendly description of the preparation as an HTML .
    * 
    * @return String in HTML
    */
   abstract public String toHTML();

   /**
    * Coat the sample with the specified thickness of 2.0 g/cc carbon.
    * 
    * @param thickness
    *           In meters
    * @param parent
    * @return CoatedPreparation
    */
   public static CoatedPreparation carbonCoatSample(double thickness, SamplePreparation parent) {
      return new CoatedPreparation(CARBON, thickness, parent);
   }

   /**
    * Coat the sample with the specified thickness of 19.3 g/cc gold.
    * 
    * @param thickness
    *           In meters
    * @param parent
    * @return CoatedPreparation
    */
   public static CoatedPreparation goldCoatSample(double thickness, SamplePreparation parent) {
      final Material c = new Material(Element.Au, ToSI.gPerCC(19.3));
      c.setName("gold");
      return new CoatedPreparation(c, thickness, parent);
   }

   /**
    * Coat the sample with the specified thickness of 22.56 g/cc iridium.
    * 
    * @param thickness
    *           In meters
    * @param parent
    * @return CoatedPreparation
    */
   public static CoatedPreparation iridiumCoatSample(double thickness, SamplePreparation parent) {
      final Material c = new Material(Element.Ir, ToSI.gPerCC(22.56));
      c.setName("iridium");
      return new CoatedPreparation(c, thickness, parent);
   }

   /**
    * Flat polish then carbon coat the sample with the specified thickness of
    * 2.0 g/cc carbon.
    * 
    * @param roughness
    *           Polish roughness in meters
    * @param thickness
    *           Coating thickness in meters
    * @return CoatedPreparation
    */
   public static CoatedPreparation flatPolishAndCarbonCoatSample(double roughness, double thickness) {
      return carbonCoatSample(thickness, new FlatPolishPreparation(roughness));
   }

   /**
    * <p>
    * Represents a flat-polished sample preparation.
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    * 
    * @author nritchie
    * @version 1.0
    */
   static public class FlatPolishPreparation extends SamplePreparation {

      public FlatPolishPreparation(double roughness) {
         super(roughness);
      }

      @Override
      public String toHTML() {
         final HalfUpFormat nf = new HalfUpFormat("#,##0.0");
         return "Polished to a " + nf.format(getTextureScale() * 1.0e9) + " nm finish";
      }

      @Override
      public double score() {
         return Math2.bound(10.0 - Math.log10(getTextureScale() / 1.0e-9), 0.0, 10.0);
      }
   }

   /**
    * <p>
    * Represents a coating over another SamplePreparation.
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    * 
    * @author nritchie
    * @version 1.0
    */
   static public class CoatedPreparation extends SamplePreparation {

      private final SamplePreparation mParent;
      private final double mThickness;
      private final Material mMaterial;

      /**
       * Constructs an object representing a coating layer over another
       * SamplePreparation.
       */
      public CoatedPreparation(Material mat, double thickness, SamplePreparation parent) {
         super(parent.mScale);
         mMaterial = mat;
         mThickness = thickness;
         mParent = parent;
      }

      @Override
      public double score() {
         final XRayTransition xrt = new XRayTransition(Element.O, XRayTransition.KA1);
         double k = 10.0;
         try {
            k = (mThickness * MassAbsorptionCoefficient.HeinrichDtsa.compute(mMaterial, xrt) * mMaterial.getDensity())
                  / (10.0e-9 * MassAbsorptionCoefficient.HeinrichDtsa.compute(CARBON, xrt) * CARBON.getDensity());
         } catch (final EPQException e) {
            e.printStackTrace();
         }
         return Math2.bound(mParent.score() - Math.abs(k - 1.0), 0.0, 10.0);
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.SamplePreparation#toHTML()
       */
      @Override
      public String toHTML() {
         final HalfUpFormat nf = new HalfUpFormat("#,##0.0");
         return mParent.toHTML() + " coated with " + nf.format(mThickness * 1.0e9) + " nm of " + mMaterial.toString();
      }

      /**
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = super.hashCode();
         result = (prime * result) + ((mMaterial == null) ? 0 : mMaterial.hashCode());
         result = (prime * result) + ((mParent == null) ? 0 : mParent.hashCode());
         long temp;
         temp = Double.doubleToLongBits(mThickness);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         return result;
      }

      /**
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (!super.equals(obj))
            return false;
         if (!(obj instanceof CoatedPreparation))
            return false;
         final CoatedPreparation other = (CoatedPreparation) obj;
         if (mMaterial == null) {
            if (other.mMaterial != null)
               return false;
         } else if (!mMaterial.equals(other.mMaterial))
            return false;
         if (mParent == null) {
            if (other.mParent != null)
               return false;
         } else if (!mParent.equals(other.mParent))
            return false;
         if (Double.doubleToLongBits(mThickness) != Double.doubleToLongBits(other.mThickness))
            return false;
         return true;
      }
   }

   /**
    * <p>
    * Constructs an object representing no sample preparation.
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    * 
    * @author nritchie
    * @version 1.0
    */
   static public class NoSamplePreparation extends SamplePreparation {

      /**
       * Constructs a NoSamplePreparation
       */
      public NoSamplePreparation(double scale) {
         super(scale);
      }

      @Override
      public double score() {
         return 5.0;
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.SamplePreparation#toHTML()
       */
      @Override
      public String toHTML() {
         return "As received";
      }

   }
}
