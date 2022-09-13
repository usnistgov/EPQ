/**
 * gov.nist.microanalysis.EPQLibrary.ConductiveCoatingCorrection Created by:
 * nicho Date: Feb 23, 2019
 */
package gov.nist.microanalysis.EPQLibrary;

import java.util.Objects;

import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * A simple model that addresses absorption by a ultra-thin surface coating as
 * the x-ray leaves the sample.
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
public class ConductiveCoating {

   private final double mThickness; // in meters
   private final Material mMaterial;

   private double massAbsorptionCoefficient(double energy) {
      return AlgorithmUser.getDefaultMAC().compute(mMaterial, energy) * mMaterial.getDensity();
   }

   /**
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      return Objects.hash(Double.valueOf(mThickness), mMaterial);
   }

   /**
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if(this == obj)
         return true;
      if(obj == null)
         return false;
      if(getClass() != obj.getClass())
         return false;
      final ConductiveCoating other = (ConductiveCoating) obj;
      return Objects.equals(mMaterial, other.mMaterial) && //
            (Double.doubleToLongBits(mThickness) == Double.doubleToLongBits(other.mThickness));
   }

   public ConductiveCoating(Material mat, double thickness) {
      mMaterial = mat;
      assert thickness < 1.0e-4;
      mThickness = thickness;
   }

   /**
    * Build a carbon layer of 2.0 g/cm<sup>3</sup>
    *
    * @param thickness Typically around 10 nm
    * @return ConductiveCoating
    */
   public static ConductiveCoating buildAmorphousCarbon(double thickness) {
      return build(Element.C, 2.0, thickness);
   }

   /**
    * <p>
    * A set of materials used for conductive coatings.
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
   public enum CCMaterial {
      Carbon(Element.C, 2.0), //
      Gold(Element.Au, 19.3), //
      Iridium(Element.Ir, 22.56), //
      Platinum(Element.Pt, 21.45);

      private final Material mMaterial;

      private CCMaterial(Element elm, double density) {
         this(new Material(new Composition(elm), ToSI.gPerCC(density)));
      }

      private CCMaterial(Material mat) {
         mMaterial = mat;
      }

      public Material getMaterial() {
         return mMaterial;
      }
   }

   /**
    * Build a single element coating
    *
    * @param elm An Element
    * @param density In g/cm<sup>3</sup>
    * @param thickness In meters
    * @return {@link ConductiveCoating}
    */
   public static ConductiveCoating build(Element elm, double density, double thickness) {
      Material mm = new Material(new Composition(Element.Ir), ToSI.gPerCC(density));
      return new ConductiveCoating(mm, thickness);
   }

   /**
    * Build a single element from the enumerated list in CCMaterial
    *
    * @param mat A CCMaterial
    * @param thickness In meters
    * @return {@link ConductiveCoating}
    */
   public static ConductiveCoating build(CCMaterial mat, double thickness) {
      return new ConductiveCoating(mat.getMaterial(), thickness);
   }

   /**
    * Compute the fraction of the incident x-rays of the specified energy that
    * will be transmitted at an take-off angle toa through this coating.
    *
    * @param toa
    * @param energy
    * @return double in range [0.0,1.0)
    */
   public double computeTransmission(double toa, double energy) {
      return Math.exp(-massAbsorptionCoefficient(energy) * (mThickness / Math.sin(toa)));
   }

   /**
    * Compute the fraction of the incident x-rays of the specified energy that
    * will be absorbed at an take-off angle toa through this coating.
    *
    * @param toa
    * @param energy
    * @return double in range [0.0,1.0)
    */
   public double computeAbsorption(double toa, double energy) {
      return 1.0 - computeTransmission(toa, energy);
   }

   /**
    * Compute the fraction of the incident x-rays of the specified energy that
    * will be transmitted at an take-off angle toa through this coating.
    *
    * @param toa
    * @param xr
    * @return double in range [0.0,1.0)
    */
   public double computeTransmission(double toa, XRayTransition xr) {
      try {
         return computeTransmission(toa, xr.getEnergy());
      }
      catch(EPQException e) {
         return 1.0;
      }
   }

   public Material getMaterial() {
      return mMaterial;
   }

   public double getThickness() {
      return mThickness;
   }

   /**
    * Computes the kinetic energy lost in traversing the conductive coating
    * layer
    *
    * @param e0 In Joules
    * @return In Joules
    */
   public double computeEnergyLoss(double e0) {
      // See Heinrich 1981 pp 226-227
      double res = 0.0;
      for(final Element el : mMaterial.getElementSet())
         res += AlgorithmUser.getDefaultBetheEnergyLoss().compute(el, e0) * mMaterial.weightFraction(el, true);
      return res * mMaterial.getDensity() * mThickness;
   }

   @Override
   public String toString() {
      HalfUpFormat nf = new HalfUpFormat("0.0");
      return nf.format(mThickness * 1.0e9) + " nm of " + mMaterial.toString() + " ("
            + nf.format(FromSI.gPerCC(mMaterial.getDensity())) + " g/cc)";
   }

   public String toParsableFormat() {
      HalfUpFormat nf = new HalfUpFormat("0.000");
      return nf.format(mThickness * 1.0e9) + " nm of " + mMaterial.toParsableFormat();
   }

   public static ConductiveCoating parse(String ss) {
      int i = ss.indexOf("nm of");
      if(i != -1) {
         String thick = ss.substring(0, i).trim();
         String matStr = ss.substring(i + 6).trim();
         try {
            final double thicknessNm = Double.parseDouble(thick);
            Composition mat = Material.fromParsableFormat(matStr);
            if((mat != null) && (mat instanceof Material) && (thicknessNm > 0.0))
               return new ConductiveCoating((Material) mat, 1.0e-9 * thicknessNm);
         }
         catch(NumberFormatException nex) {
            System.out.println(thick);
         }
      }
      return null;
   }

   public boolean equals(ConductiveCoating cc) {
      return mMaterial.equals(cc.mMaterial) && (mThickness == cc.mThickness);
   }

}
