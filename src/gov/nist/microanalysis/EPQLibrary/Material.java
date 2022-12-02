package gov.nist.microanalysis.EPQLibrary;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * A simple class for managing a materials properties based on a Composition +
 * density.
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

public class Material extends Composition {
   private static final long serialVersionUID = 0x42;
   private static final double DEFAULT_DENSITY = ToSI.gPerCC(5.0);
   private double mDensity; // kilograms per cubic meter

   @Override
   protected void renormalize() {
      super.renormalize();
   }

   public Material(double density) {
      super();
      mDensity = density;
   }

   /**
    * Material - Constructs a material with the specified mass fractions. If the
    * length of weighFracs is one less than the mass of elms then the last mass
    * fraction is calculated from the others assuming that the sum is 1.0.
    * 
    * @param elms
    *           Element[] - The elements
    * @param massFracs
    *           double[] - The associated mass fractions
    * @param density
    *           double - In SI
    * @param name
    *           String - User friendly
    */
   protected Material(Element[] elms, double[] massFracs, double density, String name) {
      super(elms, massFracs, name);
      mDensity = density;
      renormalize();
   }

   public Material(Composition comp, double density) {
      super();
      replicate(comp);
      mDensity = density;
   }

   public Material(Element elm, double density) {
      this(new Element[]{elm}, new double[]{1.0}, density, "Pure " + elm.toString());
   }

   /**
    * setDensity - Sets the density of the material in kg pre cubic meter.
    * 
    * @param den
    *           double
    */
   public void setDensity(double den) {
      if (mDensity != den)
         mDensity = den;
   }

   /**
    * getDensity - returns the density of the material in kg pre cubic meter.
    * 
    * @return double
    */
   public double getDensity() {
      return mDensity;
   }

   /**
    * defineByWeightFraction - Define the composition of this material by weight
    * fraction. The map argument contains a map where the key may be an Integer
    * containing the atomic number, a String containing the abbreviation or full
    * name of the element or an Element object. The value is the mass fraction
    * as a Double.
    * 
    * @param map
    *           Map - keys are either Integer, String or Element types, values
    *           are Double
    * @param den
    *           double - in kg/m^3
    */
   public void defineByWeightFraction(Map<Object, Double> map, double den) {
      mDensity = den;
      super.defineByWeightFraction(map);
   }

   /**
    * clear - Clear all consistuent elements. Material -&gt; pure vacuum
    */
   @Override
   public void clear() {
      super.clear();
      mDensity = DEFAULT_DENSITY;
   }

   /**
    * atomsPerCubicMeter - Computes the numbers of atoms per cubic centimeter
    * for the specified element based on the composition and density of this
    * Material.
    * 
    * @param elm
    *           Element
    * @return double
    */
   public double atomsPerCubicMeter(Element elm) {
      assert ((getElementCount() == 0) || (mDensity > 0.0));
      return (weightFraction(elm, true) * mDensity) / elm.getMass();
   }

   /**
    * defineByMaterialFraction - Extends defineByMaterialFraction to also
    * compute the density based on the amount of each base material and the
    * density of the base material.
    * 
    * @param mats
    *           Material[] - The base materials (ie SiO2, MgO,...)
    * @param matFracs
    *           double[] - The proportion of each
    */
   public void defineByMaterialFraction(Material[] mats, double[] matFracs) {
      super.defineByMaterialFraction(mats, matFracs);
      double den = 0.0;
      for (int i = 0; i < mats.length; ++i)
         den += matFracs[i] * mats[i].getDensity();
      setDensity(den);
   }

   /**
    * defineByMaterialFraction - Extends defineByMaterialFraction to also
    * compute the density based on the amount of each base material and the
    * density of the base material.
    * 
    * @param compositions
    *           Compositions[] - The base materials (ie SiO2, MgO,...)
    * @param matFracs
    *           double[] - The proportion of each
    * @param density
    *           - Density in kg/m^3
    */
   public void defineByMaterialFraction(Composition[] compositions, double[] matFracs, double density) {
      super.defineByMaterialFraction(compositions, matFracs);
      setDensity(density);
   }

   /**
    * descriptiveId - A string describing this material terms of the constituent
    * element's weight percent and the material density.
    * 
    * @param normalize
    *           Normalize weight percents to 100%?
    * @return String
    */
   @Override
   public String descriptiveString(boolean normalize) {
      if (this.getElementCount() > 0) {
         final NumberFormat nf = NumberFormat.getInstance();
         nf.setMaximumFractionDigits(1);
         final StringBuffer sb = new StringBuffer(super.descriptiveString(normalize));
         final int p = sb.lastIndexOf("]");
         sb.insert(p, "," + nf.format(FromSI.gPerCC(mDensity)) + " g/cc");
         return sb.toString();
      } else
         return "None";
   }

   @Override
   public int compareTo(Composition obj) {
      int res = super.compareTo(obj);
      if ((res == 0) && (obj instanceof Material))
         res = Double.compare(mDensity, ((Material) obj).mDensity);
      return res;
   }

   protected void replicate(Material mat) {
      super.replicate(mat);
      mDensity = mat.mDensity;
   }

   @Override
   public Material clone() {
      super.clone();
      final Material res = new Material(mDensity);
      res.replicate(this);
      return res;
   }

   public static final Material Null = createNull();

   private static Material createNull() {
      final Material res = new Material(0.0);
      res.setName("None");
      return res;
   }

   public static final Material Other = createOther();

   private static Material createOther() {
      final Material res = new Material(0.0);
      res.setName("Other...");
      return res;
   }

   /**
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      if (mHashCode == Integer.MAX_VALUE) {
         final int PRIME = 31;
         int result = super.hashCode();
         long temp;
         temp = Double.doubleToLongBits(mDensity);
         result = (PRIME * result) + (int) (temp ^ (temp >>> 32));
         if (result == Integer.MAX_VALUE)
            result = Integer.MIN_VALUE;
         mHashCode = result;
      }
      return mHashCode;
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
      if (getClass() != obj.getClass())
         return false;
      final Material other = (Material) obj;
      if (Double.doubleToLongBits(mDensity) != Double.doubleToLongBits(other.mDensity))
         return false;
      return true;
   }

   @Override
   public boolean almostEquals(Composition other, double tol) {
      boolean res = super.almostEquals(other, tol);
      if (other instanceof Material) {
         final Material otherMat = (Material) other;
         res = res && ((Math.abs(getDensity() - otherMat.getDensity()) / Math.max(getDensity(), otherMat.getDensity())) < tol);
      }
      return res;
   }

   @Override
   public String toParsableFormat() {
      final StringBuffer sb = new StringBuffer(super.toParsableFormat());
      final NumberFormat dfm = new HalfUpFormat("0.0#", new DecimalFormatSymbols(Locale.US));
      sb.append(",");
      sb.append(dfm.format(FromSI.gPerCC(getDensity())));
      return sb.toString();
   }

   public static Composition fromParsableFormat(String str) {
      final NumberFormat nf = NumberFormat.getInstance(Locale.US);
      Composition comp = new Composition();
      final String[] items = str.split(",");
      for (int i = 1; i < items.length; ++i)
         try {
            final String tmp = items[i].trim();
            final int p = tmp.indexOf(":");
            if (tmp.startsWith("(") && tmp.endsWith(")") && (p > 0)) {
               final Element elm = Element.byName(tmp.substring(1, p).trim());
               final double qty = nf.parse(tmp.substring(p + 1, tmp.length() - 1).trim()).doubleValue();
               comp.addElement(elm, 0.01 * qty);
            } else {
               // Density
               final double qty = nf.parse(tmp.substring(0, tmp.length()).trim()).doubleValue();
               comp = new Material(comp, ToSI.gPerCC(qty));
            }
         } catch (final ParseException e) {
            // Just ignore it...
         }
      comp.setName(items[0]);
      return comp.getElementCount() > 0 ? comp : null;
   }
}
