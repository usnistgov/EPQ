package gov.nist.microanalysis.Utility;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.EPQException;

/**
 * <p>
 * The UncertainValue2 class implements a class for handling values with zero or
 * more component normally distributed uncertainties. The class implements a
 * number of static methods for performing basic mathematical operations on
 * numbers while propagating the component uncertainties.
 * </p>
 * <p>
 * Each uncertain value is represented by a value and a series of named
 * component uncertainties. The component names identify the source of the
 * uncertainty. Uncertainty components associated with the same name are assumed
 * to be 100% correlated (r=1.0) and are accumulated each time an operation is
 * performed. Uncertainties associated with different names are accumulated
 * separately. The named uncertainties can be reduced to a single uncertainty as
 * a final step. This step may either assume the components are independent or
 * correlated using the Correlation imbedded class.
 * </p>
 * <p>
 * Each component of uncertainty propagates through the mathematical operations
 * as though it was the only source of uncertainty.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nicholas
 * @version 1.0
 */
final public class UncertainValue2
   extends
   Number
   implements
   Comparable<UncertainValue2> {

   public static final String DEFAULT = "Default";
   private static transient long sDefIndex = 0;
   private static final long serialVersionUID = 119495064970078787L;
   /**
    * The value
    */
   private final double mValue;
   /**
    * A map of one-sigma width uncertainty components.
    */
   private final TreeMap<String, Double> mSigmas = new TreeMap<String, Double>();

   public static final UncertainValue2 ONE = new UncertainValue2(1.0);
   public static final UncertainValue2 ZERO = new UncertainValue2(0.0);
   public static final UncertainValue2 NaN = new UncertainValue2(Double.NaN);
   public static final UncertainValue2 POSITIVE_INFINITY = new UncertainValue2(Double.POSITIVE_INFINITY);
   public static final UncertainValue2 NEGATIVE_INFINITY = new UncertainValue2(Double.NEGATIVE_INFINITY);
   public static final UncertainValue2 MAX_VALUE = new UncertainValue2(Double.MAX_VALUE);

   @Override
   public UncertainValue2 clone() {
      return new UncertainValue2(mValue, mSigmas);
   }

   /**
    * Constructs a UncertainValue2 with value <code>v</code> and uncertainty
    * <code>dv</code>.
    * 
    * @param v The value
    * @param dv The associated uncertainty
    */
   public UncertainValue2(final double v, final double dv) {
      this(v, DEFAULT + Long.toString(++sDefIndex), dv);
   }

   /**
    * Constructs a UncertainValue2 with value <code>v</code> and no uncertainty.
    * 
    * @param v The value
    */
   public UncertainValue2(final double v) {
      this(v, 0.0);
   }

   public static UncertainValue2 asUncertainValue2(final Number n) {
      return n instanceof UncertainValue2 ? (UncertainValue2) n : new UncertainValue2(n.doubleValue());
   }

   /**
    * Constructs a UncertainValue2
    * 
    * @param v double
    * @param source String The name of the source of the uncertainty
    * @param dv The associate uncertainty
    */
   public UncertainValue2(final double v, final String source, final double dv) {
      mValue = v;
      assignComponent(source, dv);
   }

   /**
    * Constructs a UncertainValue2 with value <code>v</code> and uncertainties
    * <code>sigmas</code>.
    * 
    * @param v The value
    * @param sigmas The associated uncertainties
    */
   public UncertainValue2(final double v, final Map<String, Double> sigmas) {
      super();
      mValue = v;
      if(sigmas != null)
         for(final Map.Entry<String, Double> me : sigmas.entrySet())
            assignComponent(me.getKey(), me.getValue());
   }

   /**
    * Create an UncertainValue2 where the source of uncertainty is Gaussian
    * (sigma~sqrt(v)).
    * 
    * @param v
    * @param source
    * @return UncertainValue2
    */
   static public UncertainValue2 createGaussian(final double v, final String source) {
      return new UncertainValue2(v, source, Math.sqrt(v));
   }

   /**
    * Assigns the magnitude (abs(sigma)) of the specified source of uncertainty.
    * Any previous value assigned to this source is replaced. If sigma is zero
    * then any earlier value is erased.
    * 
    * @param name The name of the source of the uncertainty
    * @param sigma The magnitude of the uncertainty
    */
   public void assignComponent(final String name, final double sigma) {
      if(sigma != 0.0)
         mSigmas.put(name, Math.abs(sigma));
      else
         mSigmas.remove(name);
   }

   public void assignComponents(final Map<String, Double> comps) {
      for(final Map.Entry<String, Double> me : comps.entrySet())
         assignComponent(me.getKey(), me.getValue());
   }

   @Override
   public String toString() {
      if(mSigmas.size() > 0)
         return Double.toString(mValue) + " \u00B1 " + Double.toString(uncertainty());
      else
         return Double.toString(mValue);
   }

   public String toLongString() {
      if(mSigmas.size() > 0) {
         final StringBuffer sb = new StringBuffer();
         sb.append(mValue);
         for(final Map.Entry<String, Double> me : mSigmas.entrySet()) {
            sb.append("\u00B1");
            sb.append(me.getValue());
            sb.append("(");
            sb.append(me.getKey());
            sb.append(")");
         }
         return sb.toString();
      } else
         return Double.toString(mValue);
   }

   static public String format(final NumberFormat nf, Number n) {
      if(n instanceof UncertainValue2)
         return ((UncertainValue2) n).format(nf);
      else
         return nf.format(n);
   }

   /**
    * Formats the {@link UncertainValue2} as a val +- uncertainty using the
    * specified NumberFormat for both.
    * 
    * @param nf
    * @return String
    */
   public String format(final NumberFormat nf) {
      if(mSigmas.size() > 0)
         return nf.format(mValue) + "\u00B1" + nf.format(uncertainty());
      else
         return nf.format(mValue);
   }

   /**
    * Formats the {@link UncertainValue2} as a val +- dv1(src1) +- dv2(src2)...
    * using the specified NumberFormat.
    * 
    * @param nf
    * @return String
    */
   public String formatLong(final NumberFormat nf) {
      final StringBuffer sb = new StringBuffer();
      sb.append(nf.format(mValue));
      for(final Map.Entry<String, Double> me : mSigmas.entrySet()) {
         sb.append("\u00B1");
         sb.append(nf.format(me.getValue()));
         sb.append("(");
         sb.append(me.getKey());
         sb.append(")");
      }
      return sb.toString();
   }

   /**
    * Format the specified uncertainty source in the format "U(src)=df".
    * 
    * @param src
    * @param nf
    * @return String
    */
   public String format(final String src, final NumberFormat nf) {
      final Double dv = mSigmas.get(src);
      return "U(" + src + ")=" + nf.format(dv != null ? dv.doubleValue() : 0.0);
   }

   /**
    * Returns the specified source of uncertainty.
    * 
    * @param src
    * @return The specified uncertainty or 0.0 if src not defined.
    */
   public double getComponent(final String src) {
      final Double v = mSigmas.get(src);
      return v != null ? v.doubleValue() : 0.0;
   }

   /**
    * Get the fractional uncertainty associated with the specified component.
    * 
    * @param src
    * @return double
    */
   public double getFractional(final String src) {
      final Double v = mSigmas.get(src);
      return v != null ? v.doubleValue() / mValue : 0.0;

   }

   public String formatComponent(final String comp, final NumberFormat nf) {
      final Double v = mSigmas.get(comp);
      return nf.format(v != null ? v.doubleValue() : 0.0) + "(" + comp + ")";
   }

   public String formatComponent(final String comp, final int dec) {
      final Double v = mSigmas.get(comp);
      NumberFormat nf;
      if((v != null) && (Math.abs(v.doubleValue()) < (10.0 * Math.pow(10, -dec))))
         nf = new ExponentFormat(2);
      else {
         final StringBuffer sf = new StringBuffer("0.");
         for(int i = 1; i < dec; ++i)
            sf.append("0");
         nf = new HalfUpFormat(sf.toString());
      }
      return nf.format(v != null ? v.doubleValue() : 0.0) + "(" + comp + ")";
   }

   /**
    * Is this component defined?
    * 
    * @param src
    * @return true or false
    */
   public boolean hasComponent(final String src) {
      return mSigmas.containsKey(src);
   }

   public Map<String, Double> getComponents() {
      return Collections.unmodifiableMap(mSigmas);
   }

   public Set<String> getComponentNames() {
      return Collections.unmodifiableSet(mSigmas.keySet());
   }

   /**
    * Rename an uncertainty component. Fails with an EPQException if a component
    * with the new name already exists.
    * 
    * @param oldName
    * @param newName
    */
   public void renameComponent(final String oldName, final String newName)
         throws EPQException {
      if(mSigmas.containsKey(newName))
         throw new EPQException("A component named " + newName + " already exists.");
      final Double val = mSigmas.remove(oldName);
      if(val != null)
         mSigmas.put(newName, val);
   }

   static private UncertainValue2 toUV2(Number num) {
      if(num instanceof UncertainValue2)
         return (UncertainValue2) num;
      else
         return new UncertainValue2(num.doubleValue());
   }

   /**
    * Add a list of {@link UncertainValue2}.
    * 
    * @param uvs
    * @return An UncertainValue2 equal to the sum of the uvs
    */
   static public UncertainValue2 add(final Collection<? extends Number> uvs) {
      final HashSet<String> srcs = new HashSet<String>();
      double sum = 0.0;
      for(final Number n2 : uvs) {
         final UncertainValue2 uv2 = toUV2(n2);
         srcs.addAll(uv2.mSigmas.keySet());
         sum += uv2.mValue;
      }
      final UncertainValue2 res = new UncertainValue2(sum);
      for(final String src : srcs) {
         double unc = 0.0;
         for(final Number n2 : uvs) {
            unc += toUV2(n2).getComponent(src);
         }
         res.assignComponent(src, unc);
      }
      return res;
   }

   static public UncertainValue2 add(final Number[] uvs) {
      return add(Arrays.asList(uvs));
   }

   static public UncertainValue2 add(final double a, final Number na, final double b, final Number nb) {
      final UncertainValue2 uva = toUV2(na), uvb = toUV2(nb);
      final UncertainValue2 res = new UncertainValue2((a * uva.mValue) + (b * uvb.mValue));
      final Set<String> srcs = new HashSet<String>();
      srcs.addAll(uva.mSigmas.keySet());
      srcs.addAll(uvb.mSigmas.keySet());
      for(final String src : srcs)
         res.assignComponent(src, Math.abs((a * uva.getComponent(src)) + (b * uvb.getComponent(src))));
      return res;
   }

   static public UncertainValue2 subtract(final Number na, final Number nb) {
      return add(1.0, toUV2(na), -1.0, toUV2(nb));
   }

   public static UncertainValue2 mean(final Collection<Number> uvs) {
      return divide(add(uvs), uvs.size());
   }

   public static UncertainValue2 mean(final Number[] uvs) {
      return divide(add(uvs), uvs.length);
   }

   public UncertainValue2 abs() {
      return mValue >= 0.0 ? this : new UncertainValue2(-mValue, this.mSigmas);
   }

   public static UncertainValue2 abs(Number n) {
      return toUV2(n).abs();
   }

   /**
    * <p>
    * Computes the variance weighted mean - the maximum likelyhood estimator of
    * the mean under the assumption that the samples are independent and
    * normally distributed.
    * </p>
    * 
    * @param cuv
    * @return UncertainValue2
    */
   static public UncertainValue2 weightedMean(final Collection<? extends Number> cuv)
         throws UtilException {
      double varSum = 0.0;
      UncertainValue2 sum = ZERO;
      for(final Number nuv : cuv) {
         final UncertainValue2 uv = toUV2(nuv);
         final double ivar = 1.0 / uv.variance();
         if(Double.isNaN(ivar))
            throw new UtilException("Unable to compute the weighted mean when one or more datapoints have zero uncertainty.");
         varSum += ivar;
         sum = UncertainValue2.add(sum, UncertainValue2.multiply(ivar, uv));
      }
      final double iVarSum = 1.0 / varSum;
      return Double.isNaN(iVarSum) ? UncertainValue2.NaN : UncertainValue2.multiply(iVarSum, sum);
   }

   /**
    * <p>
    * Computes the variance weighted mean - the maximum likelyhood estimator of
    * the mean under the assumption that the samples are independent and
    * normally distributed. Discards points for which the variance() is zero.
    * </p>
    * 
    * @param cuv
    * @return UncertainValue2
    */
   static public UncertainValue2 safeWeightedMean(final Collection<? extends Number> cuv) {
      double varSum = 0.0;
      UncertainValue2 sum = ZERO;
      for(final Number nuv : cuv) {
         if(!(nuv instanceof UncertainValue2))
            continue;
         final UncertainValue2 uv = (UncertainValue2) nuv;
         final double ivar = 1.0 / uv.variance();
         if(Double.isNaN(ivar))
            continue;
         varSum += ivar;
         sum = UncertainValue2.add(sum, UncertainValue2.multiply(ivar, uv));
      }
      final double iVarSum = 1.0 / varSum;
      return Double.isNaN(iVarSum) ? UncertainValue2.NaN : UncertainValue2.multiply(iVarSum, sum);
   }

   /**
    * Returns the uncertain value with the smallest value. When two uncertain
    * values are equal by value, the one with the larger uncertainty is
    * returned.
    * 
    * @param uvs
    * @return UncertainValue2
    */
   public static UncertainValue2 min(final Collection<Number> uvs) {
      UncertainValue2 res = null;
      for(final Number nuv : uvs) {
         final UncertainValue2 uv = toUV2(nuv);
         if(res == null)
            res = uv;
         else if(uv.doubleValue() < res.doubleValue())
            res = uv;
         else if(uv.doubleValue() == res.doubleValue())
            if(uv.uncertainty() > res.uncertainty())
               res = uv;
      }
      return res;
   }

   /**
    * Returns the uncertain value with the largest value. When two uncertain
    * values are equal by value, the one with the larger uncertainty is
    * returned.
    * 
    * @param uvs
    * @return UncertainValue2
    */
   public static UncertainValue2 max(final Collection<UncertainValue2> uvs) {
      UncertainValue2 res = null;
      for(final Number nuv : uvs) {
         final UncertainValue2 uv = toUV2(nuv);
         if(res == null)
            res = uv;
         else if(uv.doubleValue() > res.doubleValue())
            res = uv;
         else if(uv.doubleValue() == res.doubleValue())
            if(uv.uncertainty() > res.uncertainty())
               res = uv;
      }
      return res;
   }

   /**
    * Add a quantity known without error to an UncertainValue2.
    * 
    * @param v1
    * @param v2
    * @return An UncertainValue2
    */
   static public Number add(final Number v1, final double v2) {
      if(v1 instanceof UncertainValue2)
         return new UncertainValue2(((UncertainValue2) v1).mValue + v2, ((UncertainValue2) v1).mSigmas);
      else
         return Double.valueOf(v1.doubleValue() + v2);
   }

   /**
    * Add a quantity known without error to an UncertainValue2.
    * 
    * @param v1
    * @param v2
    * @return An UncertainValue2
    */
   static public UncertainValue2 add(final double v1, final Number v2) {
      if(v2 instanceof UncertainValue2)
         return new UncertainValue2(((UncertainValue2) v2).mValue + v1, ((UncertainValue2) v2).mSigmas);
      else
         return UncertainValue2.valueOf(v1 + v2.doubleValue());
   }

   /**
    * Add a quantity known without error to an UncertainValue2.
    * 
    * @param v1
    * @param v2
    * @return An UncertainValue2
    */
   static public UncertainValue2 add(final Number v1, final Number v2) {
      return add(1.0, v1, 1.0, v2);
   }

   /**
    * Multiply a constant times an UncertainValue2
    * 
    * @param v1
    * @param n2
    * @return An UncertainValue2
    */
   static public UncertainValue2 multiply(final double v1, final Number n2) {
      UncertainValue2 v2 = toUV2(n2);
      assert v2.uncertainty() >= 0.0 : v2.toLongString();
      final UncertainValue2 res = new UncertainValue2(v1 * v2.mValue);
      for(final Map.Entry<String, Double> me : v2.mSigmas.entrySet())
         res.assignComponent(me.getKey(), v1 * me.getValue());
      return res;
   }

   /**
    * Multiply two uncertain values
    * 
    * @param na
    * @param nb
    * @return An UncertainValue2
    */
   static public UncertainValue2 multiply(final Number na, final Number nb) {
      final UncertainValue2 a = toUV2(na), b = toUV2(nb);
      final UncertainValue2 res = new UncertainValue2(a.mValue * b.mValue);
      final Set<String> srcs = new TreeSet<String>();
      srcs.addAll(a.mSigmas.keySet());
      srcs.addAll(b.mSigmas.keySet());
      final double ca = b.mValue;
      final double cb = a.mValue;
      for(final String src : srcs) {
         final double ua = a.getComponent(src), ub = b.getComponent(src);
         res.assignComponent(src, Math.abs((ca * ua) + (cb * ub)));
      }
      return res;
   }

   static public UncertainValue2 invert(final Number nv) {
      final UncertainValue2 v = toUV2(nv);
      final UncertainValue2 res = new UncertainValue2(1.0 / v.mValue);
      if(!Double.isNaN(res.doubleValue())) {
         final double cb = 1.0 / (v.mValue * v.mValue);
         if(Double.isNaN(cb))
            return UncertainValue2.NaN;
         for(final String src : v.mSigmas.keySet())
            res.assignComponent(src, Math.abs(cb * v.getComponent(src)));
      }
      return res;
   }

   /**
    * Divide two uncertain values.
    * 
    * @param na
    * @param nb
    * @return An UncertainValue2
    */
   static public UncertainValue2 divide(final Number na, final Number nb) {
      final UncertainValue2 a = toUV2(na), b = toUV2(nb);
      final UncertainValue2 res = new UncertainValue2(a.mValue / b.mValue);
      if(!Double.isNaN(res.doubleValue())) {
         final Set<String> srcs = new TreeSet<String>();
         srcs.addAll(a.mSigmas.keySet());
         srcs.addAll(b.mSigmas.keySet());
         final double ca = 1.0 / b.mValue;
         final double cb = -a.mValue / (b.mValue * b.mValue);
         if(Double.isNaN(ca) || Double.isNaN(cb))
            return UncertainValue2.NaN;
         for(final String src : srcs) {
            final double ua = a.getComponent(src), ub = b.getComponent(src);
            res.assignComponent(src, Math.abs((ca * ua) + (cb * ub)));
         }
      }
      return res;
   }

   static public UncertainValue2 divide(final double a, final Number nb) {
      final UncertainValue2 b = toUV2(nb);
      final UncertainValue2 res = new UncertainValue2(a / b.mValue);
      if(!Double.isNaN(res.doubleValue())) {
         final double ub = Math.abs(a / (b.mValue * b.mValue));
         for(final Map.Entry<String, Double> me : b.mSigmas.entrySet())
            res.assignComponent(me.getKey(), ub * me.getValue().doubleValue());
      }
      return res;
   }

   static public UncertainValue2 divide(final Number na, final double b) {
      final UncertainValue2 a = toUV2(na);
      final double den = 1.0 / b;
      if(!Double.isNaN(den)) {
         final UncertainValue2 res = new UncertainValue2(den * a.doubleValue());
         final double ua = Math.abs(den);
         for(final Map.Entry<String, Double> me : a.mSigmas.entrySet())
            res.assignComponent(me.getKey(), ua * me.getValue().doubleValue());
         return res;
      } else
         return UncertainValue2.NaN;
   }

   /**
    * Compute the exponental function of an UncertainValue2.
    * 
    * @param nx
    * @return An UncertainValue2
    */

   static public UncertainValue2 exp(final Number nx) {
      final UncertainValue2 x = toUV2(nx);
      assert !Double.isNaN(x.mValue) : x.toString();
      final double ex = Math.exp(x.mValue);
      final UncertainValue2 res = new UncertainValue2(ex);
      for(final Map.Entry<String, Double> me : x.mSigmas.entrySet())
         res.assignComponent(me.getKey(), ex * me.getValue().doubleValue());
      return res;
   }

   /**
    * Compute the natural logarithm of an UncertainValue2.
    * 
    * @param nx
    * @return An UncertainValue2
    */
   static public UncertainValue2 log(final Number nx) {
      final UncertainValue2 v2 = toUV2(nx);
      final double tmp = 1.0 / v2.mValue;
      final double lv = Math.log(v2.mValue);
      if(!(Double.isNaN(tmp) || Double.isNaN(lv))) {
         final UncertainValue2 res = new UncertainValue2(lv);
         for(final Map.Entry<String, Double> me : v2.mSigmas.entrySet())
            res.assignComponent(me.getKey(), tmp * me.getValue());
         return res;
      } else
         return UncertainValue2.NaN;
   }

   /**
    * Compute an UncertainValue2 raised to the specified power.
    * 
    * @param n1 The value
    * @param n The exponent
    * @return An UncertainValue2
    */
   static public UncertainValue2 pow(final Number n1, final double n) {
      UncertainValue2 v1 = toUV2(n1);
      if(v1.mValue != 0.0) {
         final double f = Math.pow(v1.mValue, n);
         final double df = n * Math.pow(v1.mValue, n - 1.0);
         final UncertainValue2 res = new UncertainValue2(f);
         for(final Map.Entry<String, Double> me : v1.mSigmas.entrySet())
            res.assignComponent(me.getKey(), me.getValue() * df);
         return res;
      } else
         return UncertainValue2.ZERO;
   }

   public UncertainValue2 sqrt() {
      return pow(this, 0.5);
   }

   public static UncertainValue2 sqrt(final Number uv) {
      return pow(uv, 0.5);
   }

   /**
    * Solves the quadratice equation a x^2 + b x + c == 0
    * 
    * @param na
    * @param nb
    * @param nc
    * @return zero or two values for x depending upon whether x is imaginary (0)
    *         or real (2)
    */
   public static UncertainValue2[] quadratic(final Number na, final Number nb, final Number nc) {
      final UncertainValue2 a = toUV2(na), b = toUV2(nb), c = toUV2(nc);
      // q=-0.5*(b+signum(b)*sqrt(pow(b,2.0)-4*a*c))
      // return [ q/a, c/q ]
      final UncertainValue2 r = UncertainValue2.add(1.0, UncertainValue2.pow(b, 2.0), -4.0, UncertainValue2.multiply(a, c));
      if(r.doubleValue() > 0.0) {
         final UncertainValue2 q = UncertainValue2.multiply(-0.5, UncertainValue2.add(b, UncertainValue2.multiply(Math.signum(b.mValue), r.sqrt())));
         return new UncertainValue2[] {
            UncertainValue2.divide(q, a),
            UncertainValue2.divide(c, q)
         };
      } else
         return null;

   }

   /**
    * The value portion of the UncertainValue2.
    * 
    * @return double
    */
   @Override
   public double doubleValue() {
      return mValue;
   }

   /**
    * True if the uncertainty associated with this item is non-zero.
    * 
    * @return boolean
    */
   public boolean isUncertain() {
      return mSigmas.size() > 0;
   }

   /**
    * The uncertainty in the UncertainValue2.
    * 
    * @return A double
    */
   public double uncertainty() {
      return Math.sqrt(variance());
   }

   public static double uncertainty(Number n) {
      return n instanceof UncertainValue2 ? ((UncertainValue2) n).doubleValue() : 0.0;
   }

   /**
    * The variance associated with this UncertainValue2
    * 
    * @return double
    */
   public double variance() {
      double sigma2 = 0.0;
      for(final Double s : mSigmas.values())
         sigma2 += s * s;
      return sigma2;
   }

   /**
    * The fractional uncertainty = Math.abs(uncertainty()/doubleValue()) If
    * doubleValue()==0, returns a large number (Double.MAX_VALUE).
    * 
    * @return A double
    */
   public double fractionalUncertainty() {
      return Double.isNaN(1.0 / mValue) ? Double.MAX_VALUE : Math.abs(uncertainty() / mValue);
   }

   /**
    * The fractional uncertainty = Math.abs(uncertainty()/doubleValue())
    * 
    * @return A double
    */
   public static double fractionalUncertainty(Number n) {
      if(n instanceof UncertainValue2)
         return ((UncertainValue2) n).fractionalUncertainty();
      else
         return 0.0;
   }

   public UncertainValue2 fractionalUncertaintyU() {
      return UncertainValue2.divide(this, mValue);
   }

   public boolean isNaN() {
      return Double.isNaN(mValue);
   }

   /**
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      return Objects.hash(mValue, mSigmas);
   }

   /**
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(final Object obj) {
      if(this == obj)
         return true;
      if(obj == null)
         return false;
      if(getClass() != obj.getClass())
         return false;
      final UncertainValue2 other = (UncertainValue2) obj;
      return mSigmas.equals(other.mSigmas) && (mValue == other.mValue);
   }

   /**
    * Tests for equality of both the value and the uncertainty to less than
    * tolerance in each component and the total uncertainty.
    * 
    * @param other
    * @param tolerance
    * @return true if equal to within tolerance, false otherwise.
    */
   public boolean equals(final UncertainValue2 other, double tolerance) {
      if(this == other)
         return true;
      Set<String> keys = new TreeSet<String>();
      keys.addAll(other.mSigmas.keySet());
      keys.addAll(mSigmas.keySet());
      for(String key : keys)
         if(Math.abs(mSigmas.get(key) - other.mSigmas.get(key)) >= tolerance)
            return false;
      return (Math.abs(uncertainty() - other.uncertainty()) < tolerance) && (Math.abs(mValue - other.mValue) < tolerance);
   }

   /**
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   @Override
   public int compareTo(final UncertainValue2 o) {
      final int res = Double.compare(mValue, o.mValue);
      return res != 0 ? res : Double.compare(uncertainty(), o.uncertainty());
   }

   public boolean lessThan(final UncertainValue2 uv2) {
      return mValue < uv2.mValue;
   }

   public boolean greaterThan(final UncertainValue2 uv2) {
      return mValue > uv2.mValue;
   }

   public boolean lessThanOrEqual(final UncertainValue2 uv2) {
      return mValue <= uv2.mValue;
   }

   public boolean greaterThanOrEqual(final UncertainValue2 uv2) {
      return mValue >= uv2.mValue;
   }

   static public UncertainValue2 sqr(final UncertainValue2 uv) {
      return pow(uv, 2.0);
   }

   static public UncertainValue2 negate(final Number n) {
      final UncertainValue2 uv = toUV2(n);
      return new UncertainValue2(-uv.mValue, uv.mSigmas);
   }

   static public UncertainValue2 atan(final UncertainValue2 uv) {
      final double f = Math.atan(uv.doubleValue());
      final double df = 1.0 / (1.0 + (uv.doubleValue() * uv.doubleValue()));
      if(!(Double.isNaN(f) || Double.isNaN(df))) {
         final UncertainValue2 res = new UncertainValue2(f);
         for(final Map.Entry<String, Double> me : uv.mSigmas.entrySet())
            res.assignComponent(me.getKey(), df * me.getValue());
         return res;
      } else
         return UncertainValue2.NaN;
   }

   static public UncertainValue2 atan2(final UncertainValue2 y, final UncertainValue2 x) {
      final double f = Math.atan2(y.doubleValue(), x.doubleValue());
      final double df = 1.0 / (1.0 + Math2.sqr(y.doubleValue() / x.doubleValue()));
      if(!(Double.isNaN(f) || Double.isNaN(df))) {
         final UncertainValue2 res = new UncertainValue2(f);
         for(final Map.Entry<String, Double> me : UncertainValue2.divide(y, x).mSigmas.entrySet())
            res.assignComponent(me.getKey(), df * me.getValue());
         return res;
      } else
         return UncertainValue2.NaN;
   }

   static public UncertainValue2 nonNegative(final UncertainValue2 uv) {
      return uv.doubleValue() >= 0.0 ? uv : new UncertainValue2(0.0, uv.mSigmas);
   }

   /**
    * <p>
    * This class is used to hold the symmetric Correlation matrix. The matrix
    * associates a correlation with pair of sources. The ordering of the sources
    * is not important.
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
    * @author Nicholas
    * @version 1.0
    */
   static public class Correlations {

      static private class Key {
         private final String mSource1;
         private final String mSource2;

         Key(final String src1, final String src2) {
            mSource1 = src1;
            mSource2 = src2;
         }

         @Override
         public int hashCode() {
            return mSource1.hashCode() + mSource2.hashCode();
         }

         @Override
         public boolean equals(final Object obj) {
            if(obj instanceof Key) {
               final Key k2 = (Key) obj;
               return (mSource1.equals(k2.mSource1) && mSource2.equals(k2.mSource2))
                     || (mSource1.equals(k2.mSource2) && mSource2.equals(k2.mSource1));
            } else
               return false;
         }
      }

      private final HashMap<Key, Double> mCorrelations;

      public Correlations() {
         mCorrelations = new HashMap<Key, Double>();
      }

      /**
       * Adds a correlation between src1 and src2 (order does not matter.)
       * 
       * @param src1
       * @param src2
       * @param corr The correlation on the range [-1.0,1.0]
       */
      public void add(final String src1, final String src2, final double corr) {
         assert (corr >= -1.0) && (corr <= 1.0);
         mCorrelations.put(new Key(src1, src2), Math2.bound(corr, -1.0, 1.0));
      }

      /**
       * Returns the correlation associated with src1 and src2 (order does not
       * matter) or zero if one has not been specified.
       * 
       * @param src1
       * @param src2
       * @return [-1.0,1.0] with 0.0 as default
       */
      public double get(final String src1, final String src2) {
         final Double r = mCorrelations.get(new Correlations.Key(src1, src2));
         return r == null ? 0.0 : r.doubleValue();
      }
   }

   public double uncertainty(final Collection<String> comps) {
      double sum2 = 0.0;
      for(final String comp : comps)
         sum2 += getComponent(comp);
      return Math.sqrt(sum2);
   }

   /**
    * Compute the variance when the component uncertainties are correlated. The
    * correlation between components is determined by the Correlations matrix
    * which associates pairs of components with a correlation between [1.0,-1.0]
    * where 0.0 is the default correlation.
    * 
    * @param corr
    * @return The correlated variance
    */
   public double variance(final Correlations corr) {
      final ArrayList<String> keys = new ArrayList<String>(mSigmas.keySet());
      double res = 0.0;
      for(int i = 0; i < keys.size(); ++i)
         res += Math2.sqr(mSigmas.get(keys.get(i)));
      for(int i = 0; i < (keys.size() - 1); ++i)
         for(int j = i + 1; j < keys.size(); ++j)
            res += 2.0 * mSigmas.get(keys.get(i)) * mSigmas.get(keys.get(j)) * corr.get(keys.get(i), keys.get(j));
      return res;
   }

   /**
    * Compute the uncertainty when the component uncertainties are correlated.
    * The correlation between components is determined by the Correlations
    * matrix which associates pairs of components with a correlation between
    * [1.0,-1.0] where 0.0 is the default correlation.
    * 
    * @param corr
    * @return The correlated variance
    */
   public double uncertainty(final Correlations corr) {
      return Math.sqrt(variance(corr));
   }

   @Override
   public float floatValue() {
      return Double.valueOf(mValue).floatValue();
   }

   @Override
   public int intValue() {
      return Double.valueOf(mValue).intValue();
   }

   @Override
   public long longValue() {
      return Double.valueOf(mValue).longValue();
   }

   @Override
   public byte byteValue() {
      return Double.valueOf(mValue).byteValue();
   }

   @Override
   public short shortValue() {
      return Double.valueOf(mValue).shortValue();
   }

   static public UncertainValue2 valueOf(Number n) {
      return n instanceof UncertainValue2 ? (UncertainValue2) n : new UncertainValue2(n.doubleValue());
   }

   public UncertainValue2 reduced(String name) {
      return new UncertainValue2(doubleValue(), name, uncertainty());
   }

   public UncertainValue2[] normalize(UncertainValue2[] vals) {
      UncertainValue2[] res = new UncertainValue2[vals.length];
      double norm = 0.0;
      for(UncertainValue2 val : vals)
         norm += val.doubleValue();
      for(int i = 0; i < vals.length; ++i)
         res[i] = UncertainValue2.multiply(1.0 / norm, vals[i]);
      return res;
   }
}
