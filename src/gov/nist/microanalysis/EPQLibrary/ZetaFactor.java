/**
 * gov.nist.microanalysis.EPQLibrary.ZetaFactor Created by: nritchie Date: Aug
 * 26, 2015
 */
package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.Utility.Pair;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * Implements Watanabe's &zeta;-factor method for quantification of STEM
 * spectra.
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
public class ZetaFactor
   extends AlgorithmClass {

   public Map<XRayTransitionSet, Number> mStandards = new HashMap<XRayTransitionSet, Number>();

   public transient Set<XRayTransitionSet> mOptimal;

   /**
    * Constructs a ZetaFactor
    */
   public ZetaFactor() {
      super("STEM Quantification", "&zeta;-Factor Quantification", "Watanabe & Williams 2006");
   }

   public void addStandard(XRayTransitionSet xrts, Number zeta) {
      assert zeta.doubleValue() > 0.0;
      mStandards.put(xrts, zeta);
   }

   /**
    * Returns a list of the XRayTransitionSet objects which were measured but
    * for which there is not a standard &zeta;-factor defined.
    *
    * @param measuredLines
    * @return Set&lt;XRayTransitionSet&gt;
    */
   public Set<XRayTransitionSet> missingStandards(Set<XRayTransitionSet> measuredLines) {
      final Set<XRayTransitionSet> dup = new TreeSet<XRayTransitionSet>(measuredLines);
      dup.removeAll(mStandards.keySet());
      return dup;
   }

   /**
    * Returns a list of the elements which have been measured.
    *
    * @param measuredLines
    * @return Set&lt;Element&gt;
    */
   public Set<Element> measuredElements(Set<XRayTransitionSet> measuredLines) {
      Set<Element> elms = new TreeSet<Element>();
      for(XRayTransitionSet xrts : measuredLines)
         elms.add(xrts.getElement());
      return Collections.unmodifiableSet(elms);
   }

   public Map<XRayTransitionSet, Number> getStandards() {
      return Collections.unmodifiableMap(mStandards);
   }

   public Set<Element> standardizedElements() {
      Set<Element> elms = new TreeSet<Element>();
      for(XRayTransitionSet xrts : mStandards.keySet())
         elms.add(xrts.getElement());
      return Collections.unmodifiableSet(elms);
   }

   /**
    * Ensure that there is at least one standard for each Element represented in
    * measuredLines.
    *
    * @param measuredLines
    * @return Set&lt;Element&gt; A set of Element objects for which standard
    *         have not been defined.
    */
   public Set<Element> validateStandards(Set<XRayTransitionSet> measuredLines) {
      final Set<Element> measuredElms = new TreeSet<Element>(measuredElements(measuredLines));
      final Set<Element> standardizedElms = new TreeSet<Element>();
      for(XRayTransitionSet xrts : measuredLines)
         if(mStandards.containsKey(xrts))
            standardizedElms.add(xrts.getElement());
      measuredElms.removeAll(standardizedElms);
      return measuredElms;
   }

   /**
    * Compute the best estimate of the mass thickness and composition assuming
    * no absorption.
    *
    * @param measurements
    * @param dose
    * @return Pair&lt;Number, Composition&gt; -&gt; Pair&lt;&rho;t,
    *         Composition&gt;
    * @throws EPQException
    */
   public Pair<Number, Composition> compute(Map<XRayTransitionSet, Number> measurements, Number dose)
         throws EPQException {
      return computeHelper(measurements, dose, null, null, 0.0);
   }

   /**
    * Compute the best estimate of the mass thickness and composition assuming
    * an established mass-thickness and composition.
    *
    * @param measurements
    * @param dose In amps/second
    * @param assumedRhoT In kg/m<sup>2</sup>
    * @param assumedComp
    * @param toa In radians
    * @return Pair<Number, Composition> -> Pair<&rho;t, Composition>
    * @throws EPQException
    */
   private Pair<Number, Composition> computeHelper(Map<XRayTransitionSet, Number> measurements, Number dose, Number assumedRhoT, Composition assumedComposition, double toa)
         throws EPQException {
      final boolean withAbs = (assumedRhoT != null) && (assumedComposition != null);
      {
         // Check there is a standard for each measurement
         Set<Element> missingStd = validateStandards(measurements.keySet());
         if(missingStd.size() > 0)
            throw new EPQException("Standards are missing for " + missingStd.toString());
      }
      final Map<Element, Number> ci = new TreeMap<Element, Number>();
      final Set<Element> elms = standardizedElements();
      final Map<Element, XRayTransitionSet> bestXrts = new TreeMap<Element, XRayTransitionSet>();
      // Determine the best standard to use for each element and compute the
      // numerator.
      for(XRayTransitionSet xrts1 : measurements.keySet()) {
         final Element elm = xrts1.getElement();
         final Number a = withAbs ? computeAbsorption(assumedRhoT, assumedComposition, toa, xrts1) : UncertainValue2.ONE;
         final Number num = UncertainValue2.multiply(measurements.get(xrts1), UncertainValue2.multiply(mStandards.get(xrts1), a));
         final Number best = ci.get(elm);
         if((best == null) || (UncertainValue2.fractionalUncertainty(best) > UncertainValue2.fractionalUncertainty(num))) {
            ci.put(elm, num);
            bestXrts.put(elm, xrts1);
         }
      }
      assert ci.size() == elms.size();
      assert bestXrts.size() == elms.size();
      // Calculate the denominator from the best measurements
      final Number den = UncertainValue2.add(ci.values());
      // Compute the composition and mass thickness from the best measurements
      Composition comp = new Composition();
      for(Element elm : bestXrts.keySet())
         comp.addElement(elm, UncertainValue2.divide(ci.get(elm), den));
      final Number rhot = UncertainValue2.divide(den, dose);
      mOptimal = new TreeSet<XRayTransitionSet>(bestXrts.values());
      return new Pair<Number, Composition>(rhot, comp);
   }

   public Number computeAbsorption(Number assumedRhoT, Composition assumedComposition, double toa, XRayTransitionSet xrts1)
         throws EPQException {
      final MassAbsorptionCoefficient macAlg = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
      final XRayTransition weightiest = xrts1.getWeighiestTransition();
      final Number mac = macAlg.computeWithUncertaintyEstimate(assumedComposition, weightiest);
      final double csc = 1.0 / Math.sin(toa);
      final Number tmp = UncertainValue2.multiply(csc, UncertainValue2.multiply(mac, assumedRhoT));
      return UncertainValue2.divide(tmp, UncertainValue2.subtract(UncertainValue2.ONE, UncertainValue2.exp(UncertainValue2.negate(tmp))));
   }

   /**
    * Compute an estimate of the mass-thickness and Composition given a series
    * of intensity measurements, the probe dose and the take-off angle.
    *
    * @param measurements
    * @param dose
    * @param toa
    * @return Pair&lt;Number, Composition&gt;
    * @throws EPQException
    */
   public Pair<Number, Composition> compute(Map<XRayTransitionSet, Number> measurements, Number dose, double toa)
         throws EPQException {
      Pair<Number, Composition> prev = compute(measurements, dose);
      for(int i = 0; i < 100; ++i) {
         final Number rhoT = prev.first;
         final Composition comp = prev.second;
         Pair<Number, Composition> est = computeHelper(measurements, dose, rhoT, comp, toa);
         final boolean thP = (Math.abs(rhoT.doubleValue() - est.first.doubleValue()) < 0.01 * rhoT.doubleValue());
         if(thP && comp.almostEquals(est.second, 0.001))
            break;
         prev = est;
      }
      return prev;
   }

   /**
    * Compute &zeta; from a sufficently thin standard
    *
    * @param massThickness in kg/m^2
    * @param intensity in counts
    * @param massFraction in mass fraction
    * @param dose product of the (probe current)*(live time) or a number
    *           proportional to it.
    * @return zeta
    */
   public Number computeZeta(Number massThickness, Number intensity, Number massFraction, Number dose) {
      return UncertainValue2.divide(UncertainValue2.multiply(massThickness, UncertainValue2.multiply(massFraction, dose)), intensity);
   }

   /**
    * @param xrts Transitions to consider
    * @param comp Composition of the standard
    * @param intensity Measured intensity
    * @param dose Probe dose in A*sec
    * @param massThickness Mass thickness in kg/m<sup>2</sup>
    * @param toa take off angle.
    * @return The zeta-factor
    * @throws EPQException
    */
   public Number computeZeta(XRayTransitionSet xrts, Composition comp, Number intensity, Number dose, Number massThickness, double toa)
         throws EPQException {
      final Number massFraction = comp.weightFractionU(xrts.getElement(), false);
      final Number abs = computeAbsorption(massThickness, comp, toa, xrts);
      return UncertainValue2.divide(computeZeta(massThickness, intensity, massFraction, dose), abs);
   }

   public void calibrate(Map<XRayTransitionSet, Number> measurements, Composition comp, Number dose, Number massThickness, double toa)
         throws EPQException {
      for(Map.Entry<XRayTransitionSet, Number> me : measurements.entrySet()) {
         XRayTransitionSet xrts = me.getKey();
         mStandards.put(xrts, computeZeta(xrts, comp, me.getValue(), dose, massThickness, toa));
      }
   }

   /**
    * This implements W&amp;W2006's eqn 29 which is based on Kanaya &amp;
    * Okayama's 1972 range equation and the assumption of a maximum 3% loss in
    * the initial electron energy.
    * 
    * @param elm Element
    * @param e0 in Joules
    * @param density in kg/m^3
    * @return maxThickness in meters
    */
   public static double maxThickness(Element elm, double e0, double density) {
      final double e0eV = FromSI.eV(e0);
      final double a = elm.getAtomicWeight();
      final int z = elm.getAtomicNumber();
      return (1.0e-9 * 1.37e-5 * a * Math.pow(e0eV * (1.0 + 0.978e-6 * e0eV), 5.0 / 3.0))
            / (FromSI.gPerCC(density) * Math.pow(z, 8.0 / 9.0) * Math.pow(1.0 + 1.957e-6 * e0eV, 4.0 / 3.0));
   }

   public Set<XRayTransitionSet> getOptimal() {
      return mOptimal;
   }

   final private static AlgorithmClass[] mAllImplementations = {
      new ZetaFactor()
   };

   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   @Override
   protected void initializeDefaultStrategy() {
      addDefaultAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Chantler2005);
   }

}
