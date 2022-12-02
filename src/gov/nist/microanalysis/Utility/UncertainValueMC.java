/**
 * gov.nist.microanalysis.Utility.UncertainValue3 Created by: nritchie Date: Mar
 * 25, 2014
 */
package gov.nist.microanalysis.Utility;

import java.util.Map;
import java.util.Random;

/**
 * <p>
 * A class to assist in implementing a MonteCarlo method of calculating the
 * uncertainty distribution associated with propagation of uncertainties. All
 * uncertainties are assumed to be normally distributed.
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
public class UncertainValueMC extends Number {

   private static final long serialVersionUID = 8528835028857799252L;

   private static final Random sRandom = new Random(System.currentTimeMillis());

   private final double mValue;
   private final double mRandVal;

   static private final double calculateDeviate(UncertainValue2 uv2, Map<String, Double> deviate) {
      double randVal = 0.0;
      for (String comp : uv2.getComponentNames()) {
         if (!deviate.containsKey(comp))
            deviate.put(comp, UncertainValueMC.normalDeviate());
         randVal += deviate.get(comp).doubleValue() * uv2.getComponent(comp);
      }
      return randVal;
   }

   static private double normalDeviate() {
      return sRandom.nextGaussian();
   }

   /**
    * FOR INTERNAL USE ONLY!!!!
    * 
    * @param v
    * @param randVal
    * @param obj
    */
   private UncertainValueMC(double v, double randVal, Object obj) {
      mValue = v;
      mRandVal = randVal;
      assert obj == null;
   }

   /**
    * Constructs a UncertainValueMC from a value and an uncertainty.
    * 
    * @param v
    *           The value
    * @param dv
    *           One standard deviation uncertainty on v
    */
   public UncertainValueMC(double v, double dv) {
      this(v, dv * normalDeviate(), null);
   }

   /**
    * Constructs an UncertainValueMC from the UncertainValue2 using the
    * dictionary deviates to map uncertainty components into normalDeviate()
    * values. If the component has a deviate in deviates, this is used.
    * Otherwise a new deviate is generated using normalDeviate() and is stored
    * in deviates for later use.
    * 
    * @param uv
    * @param deviates
    */
   public UncertainValueMC(UncertainValue2 uv, Map<String, Double> deviates) {
      this(uv.doubleValue(), uv.doubleValue() + calculateDeviate(uv, deviates), null);
   }

   /**
    * Constructs a UncertainValueMC
    */

   static public UncertainValueMC add(final double a, final UncertainValueMC uva, final double b, final UncertainValueMC uvb) {
      return new UncertainValueMC(a * uva.mValue + b * uvb.mValue, a * uva.mRandVal + b * uvb.mRandVal, null);
   }

   static public UncertainValueMC sum(UncertainValueMC[] vals) {
      double v = 0.0, rv = 0.0;
      for (int i = 0; i < vals.length; ++i) {
         v += vals[i].mValue;
         rv += vals[i].mRandVal;
      }
      return new UncertainValueMC(v, rv, null);
   }

   static public UncertainValueMC mean(UncertainValueMC[] vals) {
      return UncertainValueMC.multiply(1.0 / vals.length, sum(vals));
   }

   public UncertainValueMC abs() {
      return new UncertainValueMC(Math.abs(mValue), Math.abs(mRandVal), null);
   }

   public UncertainValueMC nonNegative() {
      return new UncertainValueMC(Math.abs(mValue), Math.abs(mRandVal), null);
   }

   public static UncertainValueMC add(UncertainValueMC v1, UncertainValueMC v2) {
      return new UncertainValueMC(v1.mValue + v2.mValue, v1.mRandVal + v2.mRandVal, null);
   }

   public static UncertainValueMC subtract(UncertainValueMC v1, UncertainValueMC v2) {
      return new UncertainValueMC(v1.mValue - v2.mValue, v1.mRandVal - v2.mRandVal, null);
   }

   public static UncertainValueMC multiply(UncertainValueMC v1, UncertainValueMC v2) {
      return new UncertainValueMC(v1.mValue * v2.mValue, v1.mRandVal * v2.mRandVal, null);
   }

   public static UncertainValueMC multiply(double v1, UncertainValueMC v2) {
      return new UncertainValueMC(v1 * v2.mValue, v1 * v2.mRandVal, null);
   }

   public static UncertainValueMC divide(UncertainValueMC v1, UncertainValueMC v2) {
      return new UncertainValueMC(v1.mValue / v2.mValue, v1.mRandVal / v2.mRandVal, null);
   }

   public static UncertainValueMC divide(UncertainValueMC v1, double v2) {
      return new UncertainValueMC(v1.mValue / v2, v1.mRandVal / v2, null);
   }

   public static UncertainValueMC pow(UncertainValueMC n, UncertainValueMC exp) {
      return new UncertainValueMC(Math.pow(n.mValue, exp.mRandVal), Math.pow(n.mRandVal, exp.mRandVal), null);
   }

   public static UncertainValueMC log(UncertainValueMC n) {
      return new UncertainValueMC(Math.log(n.mValue), Math.log(n.mRandVal), null);
   }

   public static UncertainValueMC exp(UncertainValueMC n) {
      return new UncertainValueMC(Math.log(n.mValue), Math.log(n.mRandVal), null);
   }

   public static UncertainValueMC sqrt(UncertainValueMC n) {
      return new UncertainValueMC(Math.sqrt(n.mValue), Math.sqrt(Math.max(0.0, n.mRandVal)), null);
   }

   public double nominalValue() {
      return mValue;
   }

   @Override
   public double doubleValue() {
      return mRandVal;
   }

   @Override
   public float floatValue() {
      return Double.valueOf(mRandVal).floatValue();
   }

   @Override
   public int intValue() {
      return Double.valueOf(mRandVal).intValue();
   }

   @Override
   public long longValue() {
      return Double.valueOf(mRandVal).longValue();
   }

   @Override
   public String toString() {
      return Double.toString(mRandVal);
   }
}
