package gov.nist.microanalysis.EPQLibrary;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.UncertainValue2;
import gov.nist.microanalysis.Utility.UtilException;

/**
 * <p>
 * Measured k-ratios can be associated with individual transitions but more
 * often they are associated with a set of transitions of similar energy. In
 * appreciation of this significant but often overlooked fact, the KRatioSet
 * class maps sets of transitions (XRayTransitionSet) into k-ratios with
 * associated errors (variances).
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

public class KRatioSet implements Cloneable {

   private final TreeMap<XRayTransitionSet, UncertainValue2> mData = new TreeMap<XRayTransitionSet, UncertainValue2>();

   public KRatioSet() {
      super();
   }

   /**
    * Add a new k-ratio data point.
    * 
    * @param trans
    *           XRayTransitionSet
    * @param kRatio
    *           double
    * @param variance
    *           double
    */
   public void addKRatio(XRayTransitionSet trans, double kRatio, double variance) {
      assert variance >= 0.0;
      addKRatio(trans, new UncertainValue2(kRatio, "K", variance));
   }

   /**
    * Remove the data associate with the specified XRayTransitionSet from this
    * KRatioSet
    * 
    * @param xrts
    */
   public void remove(XRayTransitionSet xrts) {
      mData.remove(xrts);
   }

   /**
    * Add a new k-ratio data point. Assume zero variance
    * 
    * @param trans
    *           XRayTransitionSet
    * @param kRatio
    *           double
    */
   public void addKRatio(XRayTransitionSet trans, double kRatio) {
      addKRatio(trans, kRatio, 0.0);
   }

   /**
    * Add a new k-ratio data point with the specified value and variance
    * 
    * @param xrts
    *           XRayTransitionSet
    * @param kRatio
    *           UncertainValue
    */
   public void addKRatio(XRayTransitionSet xrts, Number kRatio) {
      mData.put(xrts, UncertainValue2.asUncertainValue2(kRatio));
   }

   /**
    * Returns the XRayTransitionSet with the smallest variance for the specified
    * element
    * 
    * @param el
    * @return XRayTransitionSet
    */
   public XRayTransitionSet optimalDatum(Element el) {
      XRayTransitionSet optXrts = null;
      UncertainValue2 opt = null;
      for (final Map.Entry<XRayTransitionSet, UncertainValue2> me : mData.entrySet())
         if (me.getKey().getElement().equals(el)) {
            final UncertainValue2 k = me.getValue();
            if ((opt == null) || (k.uncertainty() < opt.uncertainty())) {
               optXrts = me.getKey();
               opt = k;
            }
         }
      return optXrts;
   }

   /**
    * Select from a KRatioSet, the one KRatioDatum per element which represents
    * the best quality data point for that element.
    * 
    * @return A KRatioSet containing one k-ratio per element
    */
   public KRatioSet optimalKRatioSet() {
      final KRatioSet res = new KRatioSet();
      for (final Element elm : getElementSet()) {
         final XRayTransitionSet xrts = optimalDatum(elm);
         res.addKRatio(xrts, getKRatioU(xrts));
      }
      return res;
   }

   /**
    * Returns a list of the elements contained within this KRatioSet
    * 
    * @return An unmodifiable Set of Element objects
    */
   public Set<Element> getElementSet() {
      final TreeSet<Element> res = new TreeSet<Element>();
      for (final XRayTransitionSet xrts : mData.keySet())
         res.add(xrts.getElement());
      return Collections.unmodifiableSet(res);
   }

   /**
    * Get the k-ratio for the specified element and line family.
    * 
    * @param xrts
    * @return double
    */
   public double getKRatio(XRayTransitionSet xrts) {
      final UncertainValue2 res = find(xrts);
      return res == null ? 0.0 : Math.max(0.0, res.doubleValue());
   }

   /**
    * Returns the k-ratio for the XRayTransitionSet in which the specified
    * XRayTranstion is found.
    * 
    * @param xrt
    * @return The k-ratio as a double
    */
   public double getKRatio(XRayTransition xrt) {
      for (final XRayTransitionSet xrts : mData.keySet())
         if (xrts.contains(xrt)) {
            final UncertainValue2 res = find(xrts);
            return res == null ? 0.0 : Math.max(0.0, res.doubleValue());
         }
      return 0.0;
   }

   /**
    * Returns the error associated with the k-ratio for the specified
    * transition.
    * 
    * @param xrts
    * @return double
    */
   public double getError(XRayTransitionSet xrts) {
      final UncertainValue2 res = find(xrts);
      return res == null ? 0.0 : res.uncertainty();
   }

   /**
    * Returns the non-negative k-ratio associated with the specified
    * XRayTransitionSet.
    * 
    * @param xrts
    * @return UncertainValue
    */
   public UncertainValue2 getKRatioU(XRayTransitionSet xrts) {
      final UncertainValue2 uv = find(xrts);
      return uv != null ? UncertainValue2.nonNegative(uv) : UncertainValue2.ZERO;
   }

   /**
    * Returns the k-ratio for the XRayTransitionSet in which the specified
    * XRayTranstion is found.
    * 
    * @param xrt
    * @return The k-ratio as a UncertainValue
    */
   public UncertainValue2 getKRatioU(XRayTransition xrt) {
      for (final XRayTransitionSet xrts : mData.keySet())
         if (xrts.contains(xrt)) {
            final UncertainValue2 uv = find(xrts);
            return uv != null ? UncertainValue2.nonNegative(uv) : UncertainValue2.ZERO;
         }
      return UncertainValue2.ZERO;
   }

   /**
    * Returns the raw k-ratio value. The raw k-ratio may be negative whereas the
    * value returned by getKRatioU(...) is forced to be non-negative.
    * 
    * @param xrts
    * @return UncertainValue
    */
   public UncertainValue2 getRawKRatio(XRayTransitionSet xrts) {
      final UncertainValue2 uv = find(xrts);
      return uv != null ? uv : UncertainValue2.ZERO;
   }

   public UncertainValue2 find(XRayTransitionSet xrts) {
      UncertainValue2 uv = mData.get(xrts);
      if (uv == null) {
         // Eventually I'd like to eliminate this section when I redo
         // QuantifySpectrumUsingStandards.
         double score = 0.0;
         final XRayTransition xrt = xrts.getWeighiestTransition();
         for (final Map.Entry<XRayTransitionSet, UncertainValue2> me : mData.entrySet())
            if (me.getKey().contains(xrt)) {
               final double sim = XRayTransitionSet.similarity(xrts, me.getKey());
               if ((sim > 0.8) && (sim > score)) {
                  score = sim;
                  uv = me.getValue();
               }
            }
      }
      return uv;
   }

   /**
    * Returns the sum of the non-negative k-ratios.
    * 
    * @return double
    */
   public double getKRatioSum() {
      double res = 0.0;
      for (final UncertainValue2 uv : mData.values())
         res += Math.max(0.0, uv.doubleValue());
      return res;
   }

   /**
    * A measure of the difference between this KRatioSet and the measuredKrs.
    * 
    * @param measuredKrs
    * @return double
    */
   public double difference(KRatioSet measuredKrs) {
      double res = 0.0;
      for (final Map.Entry<XRayTransitionSet, UncertainValue2> me : mData.entrySet())
         if (measuredKrs.isAvailable(me.getKey())) {
            final UncertainValue2 uv = measuredKrs.getKRatioU(me.getKey());
            res += Math2.sqr(me.getValue().doubleValue() - uv.doubleValue());
         }
      return Math.sqrt(res);
   }

   /**
    * A measure of the difference between this KRatioSet and the measuredKrs.
    * 
    * @param measuredKrs
    * @return UncertainValue2 (positive value)
    */
   public UncertainValue2 differenceU(KRatioSet measuredKrs) {
      UncertainValue2 res = UncertainValue2.ZERO;
      for (final Map.Entry<XRayTransitionSet, UncertainValue2> me : mData.entrySet())
         if (measuredKrs.isAvailable(me.getKey())) {
            final UncertainValue2 uv = measuredKrs.getKRatioU(me.getKey());
            res = UncertainValue2.add(res, UncertainValue2.sqr(UncertainValue2.subtract(me.getValue(), uv)));
         }
      return UncertainValue2.sqrt(res);
   }

   /**
    * Returns a set of XRayTransitionSet objects, one for each KRatioDatum
    * object in this KRatioSet.
    * 
    * @return A Set of XRayTransitionSet objects
    */
   public Set<XRayTransitionSet> keySet() {
      return mData.keySet();
   }

   /**
    * Returns the number of k-ratios in the set
    * 
    * @return int
    */
   public int size() {
      return mData.size();
   }

   /**
    * Is a k-ratio and error available for the specified XRayTransitionSet?
    * 
    * @param xrts
    * @return boolean
    */
   public boolean isAvailable(XRayTransitionSet xrts) {
      return find(xrts) != null;
   }

   /**
    * Is one or more k-ratio available for the specified element.
    * 
    * @param elm
    * @return boolean
    */
   public boolean isAvailable(Element elm) {
      for (final XRayTransitionSet xrts : mData.keySet())
         if (xrts.getElement().equals(elm))
            return true;
      return false;
   }

   @Override
   public String toString() {
      // (xrts,kRatio x var)
      final NumberFormat nf = NumberFormat.getInstance(Locale.US);
      nf.setGroupingUsed(false);
      nf.setMaximumFractionDigits(5);
      final StringBuffer sb = new StringBuffer(256);
      for (final Map.Entry<XRayTransitionSet, UncertainValue2> me : mData.entrySet()) {
         sb.append(sb.length() != 0 ? ", (" : "(");
         sb.append(me.getKey().toString());
         sb.append(",");
         sb.append(nf.format(me.getValue().doubleValue()));
         sb.append("\u00B1");
         sb.append(nf.format(me.getValue().uncertainty()));
         sb.append(")");
      }
      return sb.toString();
   }

   public static KRatioSet parseString(String str) throws EPQException {
      final String errStr = "Erroneous format in k-ratio string.";
      final NumberFormat nf = NumberFormat.getInstance(Locale.US);
      nf.setGroupingUsed(false);
      nf.setMaximumFractionDigits(5);
      final KRatioSet krs = new KRatioSet();
      int open = str.indexOf('(');
      while (open != -1) {
         final int close = str.indexOf(')', open + 1);
         final int comma1 = str.indexOf(',', open + 1);
         final int comma2 = str.indexOf('\u00B1', comma1 + 1);
         if ((close == -1) || (comma1 == -1) || (comma2 == -1))
            throw new EPQException(errStr);
         try {
            final XRayTransitionSet xrts = XRayTransitionSet.parseString(str.substring(open + 1, comma1));
            final int fam = AtomicShell.parseFamilyName(str.substring(comma1, comma2));
            final double kRatio = nf.parse(str.substring(comma1 + 1, comma2)).doubleValue();
            final double var = nf.parse(str.substring(comma2 + 1, close)).doubleValue();
            final Element elm = xrts.getElement();
            if ((!elm.isValid()) || (fam < AtomicShell.KFamily) || (fam > AtomicShell.MFamily))
               throw new EPQException(errStr);
            krs.addKRatio(xrts, kRatio, var);
         } catch (final ParseException ex) {
            throw new EPQException(errStr);
         }
         open = str.indexOf('(', close + 1);
      }
      return krs;
   }

   public String toTable() {
      final StringBuffer sb = new StringBuffer();
      sb.append("<TABLE>");
      sb.append("<TR><TH>[Element/line]</TH> <TD>kRatio</TD> <TD>Norm[k]</TD> <TD>Variance</TD></TR>");
      final NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(1);
      double norm = 0.0;
      final KRatioSet opt = optimalKRatioSet();
      for (final UncertainValue2 uv : opt.mData.values())
         norm += uv.doubleValue();
      for (final Map.Entry<XRayTransitionSet, UncertainValue2> me : mData.entrySet()) {
         final XRayTransitionSet xrts = me.getKey();
         final UncertainValue2 kr = me.getValue();
         sb.append("<TR><TH>");
         sb.append(xrts.toString());
         sb.append("</TR> <TD>");
         sb.append(nf.format(kr.doubleValue() * 100.0));
         sb.append("</TD> <TD>");
         final UncertainValue2 optV = opt.getKRatioU(xrts);
         if (optV != null) {
            sb.append(nf.format((optV.doubleValue() * 100.0) / norm));
            sb.append("%");
         } else
            sb.append("-");
         sb.append("</TD> <TD>");
         sb.append(nf.format(kr.uncertainty() * 100.0));
         sb.append("</TD></TR>");
      }
      sb.append("</TABLE>");
      return sb.toString();
   }

   @Override
   public Object clone() {
      final KRatioSet dup = new KRatioSet();
      for (final Map.Entry<XRayTransitionSet, UncertainValue2> me : mData.entrySet())
         dup.addKRatio(me.getKey(), me.getValue());
      return dup;
   }

   /**
    * Returns a set containing all the XRayTransitions for which there is data
    * within this KRatioSet.
    * 
    * @return Set&lt;XRayTransitionSet&gt;
    */
   public Set<XRayTransitionSet> getTransitions() {
      return Collections.unmodifiableSet(new TreeSet<XRayTransitionSet>(mData.keySet()));
   }

   /**
    * Returns a set containing all the XRayTransitions for which there is data
    * within this KRatioSet.
    * 
    * @param elm
    *           The element for which to return all XRayTransitions
    * @return Set&lt;XRayTransitionSet&gt;
    */
   public Set<XRayTransitionSet> getTransitions(Element elm) {
      final TreeSet<XRayTransitionSet> res = new TreeSet<XRayTransitionSet>();
      for (final XRayTransitionSet xrts : mData.keySet())
         if (xrts.getElement() == elm)
            res.add(xrts);
      return Collections.unmodifiableSet(res);
   }

   /**
    * <p>
    * Computes the maximum likelihood estimator of the mean k-ratio for the
    * specified XRayTransitionSet based on the evidence in the collection of
    * KRatioSet objects. This method is intended to make the best use of
    * multiple measurements of the same sample composition.
    * </p>
    * 
    * @param ckrs
    * @param xrts
    * @return UncertainValue
    */
   public UncertainValue2 weightedMean(Collection<KRatioSet> ckrs, XRayTransitionSet xrts) throws UtilException {
      final Collection<UncertainValue2> uvs = new HashSet<UncertainValue2>();
      for (final KRatioSet krs : ckrs)
         uvs.add(krs.getKRatioU(xrts));
      return UncertainValue2.weightedMean(uvs);
   }
}
