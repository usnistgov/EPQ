package gov.nist.microanalysis.EPQLibrary;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * Given a KRatioSet for an unknown material and a set of Composition(s) and
 * KRatioSet(s) for reference materials, iterated to estimate the optimal
 * Composition of the unknown.
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
public class CompositionFromKRatios extends AlgorithmClass {

   abstract static public class UnmeasuredElementRule {

      private final Element mElement;

      protected UnmeasuredElementRule(final Element elm) {
         mElement = elm;
      }

      public Element getElement() {
         return mElement;
      }

      public Composition prepare(final Composition initial, final Composition current) {
         return current;
      }

      /**
       * Takes a Composition and adds the correct proportion of the unmeasured
       * element based on the rule implemented.
       *
       * @param comp
       * @return The original composition object adjusted for the unmeasured
       *         element
       */
      abstract public Composition compute(Composition comp);

      /**
       * Return an HTML description of this rule
       *
       * @return String
       */
      abstract public String toHTML();
   };

   static public class ElementByDifference extends UnmeasuredElementRule {

      private final Element mElement;

      public ElementByDifference(final Element elm) {
         super(elm);
         mElement = elm;
      }

      @Override
      public Composition compute(final Composition comp) {
         comp.removeElement(mElement);
         final UncertainValue2 uv2 = UncertainValue2.subtract(UncertainValue2.ONE, comp.sumWeightFractionU());
         if (uv2.doubleValue() > 0.0)
            comp.addElement(mElement, uv2);
         return comp;
      }

      @Override
      public String toHTML() {
         return "<b>Element-by-difference:</b> " + mElement.toAbbrev();
      }

      @Override
      public String toString() {
         return "Element-by-difference: " + mElement.toAbbrev();
      }

      @Override
      public boolean equals(final Object obj) {
         if (obj instanceof ElementByDifference) {
            final ElementByDifference e = (ElementByDifference) obj;
            return mElement.equals(e.mElement);
         } else
            return false;
      }
   }

   /**
    * <p>
    * Force the amount of a specific element to a specific amount. This crude
    * implementation does not do any sanity checking. Use with extreme caution.
    * If the element interferes with another there will be problems if the
    * element isn't fit.
    * </p>
    * <p>
    * Useful for adding hydrogen to hydrated materials
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
   static public class ElementByFiat extends UnmeasuredElementRule {

      final private UncertainValue2 mQuantity;

      public ElementByFiat(final Element elm, final UncertainValue2 uv) {
         super(elm);
         mQuantity = uv;
      }

      public ElementByFiat(final Element elm, final double v) {
         super(elm);
         mQuantity = new UncertainValue2(v);
      }

      @Override
      public Composition compute(final Composition comp) {
         comp.removeElement(getElement());
         comp.addElement(getElement(), mQuantity);
         return comp;
      }

      @Override
      public String toHTML() {
         return "<b>Element-by-fiat:</b> " + getElement().toAbbrev() + " at " + mQuantity.format(new DecimalFormat("0.000")) + " mass fraction.";
      }
   }

   /**
    * Implements the algorithm for computing an element by stoichiometry. The
    * user creates an instance of ElementByStoichiometry for a specific element
    * with a specified valence. Then the user specifies a series of other
    * elements which are presumably in the material and a valence (of opposite
    * sign than the constructor element) for each of these elements.
    *
    * @author nicholas
    */
   static public class OxygenByStoichiometry extends UnmeasuredElementRule {

      // private final Valence mElementToAdd;
      private Oxidizer mOxidizer;
      private final SortedSet<Element> mElements;

      public OxygenByStoichiometry(final Collection<Element> elms) {
         super(Element.O);
         mElements = new TreeSet<Element>(elms);
      }

      public void setOxidizer(final Oxidizer oxy) {
         mOxidizer = oxy;
      }

      public Oxidizer getOxidizer() {
         return mOxidizer;
      }

      @Override
      public Composition compute(final Composition comp) {
         return mOxidizer != null ? mOxidizer.compute(comp) : comp;
      }

      @Override
      public String toString() {
         return "O-by-stoichiometry";
      }

      @Override
      public boolean equals(final Object obj) {
         if (obj instanceof OxygenByStoichiometry) {
            final OxygenByStoichiometry e = (OxygenByStoichiometry) obj;
            return mOxidizer.equals(e.mOxidizer);
         } else
            return false;
      }

      @Override
      public String toHTML() {
         final StringBuffer sb = new StringBuffer();
         sb.append("<h4>Oxygen-by-Stoichiometry</h4>");
         sb.append(baseHTML());
         return sb.toString();
      }

      protected String baseHTML() {
         final NumberFormat nf = new HalfUpFormat("0");
         final StringBuffer sb = new StringBuffer();
         sb.append("<table>\n");
         sb.append("<tr><th colspan=\"4\">" + toString() + "</th></tr>");
         sb.append("<tr><th>Element</th><th>Cation</th><th>Anion</th><th>Formula</th></tr>");
         for (final Element v : mElements)
            if (!v.equals(Element.O)) {
               sb.append("<tr><td>");
               sb.append(v.toAbbrev());
               sb.append("</td><td>");
               sb.append(nf.format(mOxidizer.getOxidationState(Element.O)));
               sb.append("</td><td>");
               sb.append(nf.format(mOxidizer.getOxidationState(v)));
               sb.append("</td><td>");
               sb.append(mOxidizer.toHTML(v));
               sb.append("</td></tr>\n");
            }
         sb.append("</table>");
         return sb.toString();
      }
   }

   /**
    * Compute O by stoichiometry. Compare this to measured O. If there is excess
    * O, add water to make up the difference.
    *
    * @author nicholas
    */
   public static class WatersOfCrystallization extends OxygenByStoichiometry {

      public WatersOfCrystallization(final Collection<Element> elms) {
         super(elms);
      }

      @Override
      public Composition compute(final Composition comp) {
         final Composition res = new Composition(comp);
         res.removeElement(Element.H);
         final Composition oByStoic = super.compute(res);
         // O_excess = O_measured - O_stoic
         final UncertainValue2 Oexcess = UncertainValue2.subtract(comp.weightFractionU(Element.O, false).reduced("M[O]"),
               oByStoic.weightFractionU(Element.O, false).reduced("S[O]"));
         if (Oexcess.doubleValue() > 0) {
            final UncertainValue2 massFracH = UncertainValue2.multiply((2.0 * Element.H.getAtomicWeight()) / Element.O.getAtomicWeight(), Oexcess);
            res.addElement(Element.H, massFracH);
         }
         return res;
      }

      @Override
      public Element getElement() {
         return Element.H;
      }

      @Override
      public String toHTML() {
         final StringBuffer sb = new StringBuffer();
         sb.append("<h4>Waters of Cystallization</h4>");
         sb.append(baseHTML());
         return sb.toString();
      }

      @Override
      public String toString() {
         return "Add H as water to account for excess O";
      }

      @Override
      public boolean equals(final Object obj) {
         return obj instanceof WatersOfCrystallization ? super.equals(obj) : false;
      }
   }

   // Helper to tie reference Composition and SpectrumProperties together
   private class TransitionData {
      private final Composition mComposition;
      private final SpectrumProperties mProperties;
      private final Map<XRayTransition, Double> mZAFFactors;
      private final boolean mValidate;

      private TransitionData(final Composition comp, final SpectrumProperties props) {
         this(comp, props, true);
      }

      private TransitionData(final Composition comp, final SpectrumProperties props, final boolean validate) {
         mComposition = comp;
         mProperties = props;
         mZAFFactors = new TreeMap<XRayTransition, Double>();
         mValidate = validate;
      }

      private double getKstd(final XRayTransition xrt) {
         if (!mZAFFactors.containsKey(xrt))
            try {
               final CorrectionAlgorithm ca = getCorrectionAlgorithm();
               ca.initialize(mComposition, xrt.getDestination(), mProperties);
               final ConductiveCoating cc = (ConductiveCoating) mProperties.getObjectWithDefault(SpectrumProperties.ConductiveCoating, null);
               final double toa = SpectrumUtils.getTakeOffAngle(mProperties);
               final double trStd = cc != null ? cc.computeTransmission(toa, xrt) : 1.0;
               mZAFFactors.put(xrt, //
                     ca.computeZAFCorrection(xrt) * mComposition.weightFraction(xrt.getElement(), false) * trStd);
            } catch (final EPQException e) {
               mZAFFactors.put(xrt, mComposition.weightFraction(xrt.getElement(), false));
            }
         return mZAFFactors.get(xrt);
      }
   }

   // Configuration-type data
   private double mEpsilon = 1.0e-4;
   private double mMinWeight = 0.01;
   private int mMaxIterations = 50;
   private final List<UnmeasuredElementRule> mUnmeasuredElementRule = new ArrayList<UnmeasuredElementRule>();
   private final Map<XRayTransitionSet, TransitionData> mStandardData = new TreeMap<XRayTransitionSet, TransitionData>();
   // Result-type data
   private int mIterationCount = 0;
   private Composition mResult = null;
   private KRatioSet mBestKRS = null;
   private final Map<Element, XRayTransitionSet> mUserSelectedTransitions = new HashMap<Element, XRayTransitionSet>();
   // Correction algorithm
   private CorrectionAlgorithm mCorrectionAlgorithm = null;
   // Optional coating on unknown sample
   private ConductiveCoating mCoating = null;
   private String mWarningMessage = null;

   private static final boolean LOG_ITERATION = false;

   public void addUserSelectedTransition(final Element elm, final XRayTransitionSet xrts) {
      mUserSelectedTransitions.put(elm, xrts);
   }

   /**
    * Specifies that the optimal XRayTransitionSet should be selected for each
    * element based on the counting statistics specified in the KRatioSet
    * argument to <code>compute(...)</code>.
    */
   public void clearUserSelectedTransitions() {
      mUserSelectedTransitions.clear();
   }

   private void throwConfigurationError(final KRatioSet krs) throws EPQException {
      StringBuilder res = null;
      for (final Element el : krs.getElementSet()) {
         boolean b = (forElement(el) != null);
         if (!b)
            for (final UnmeasuredElementRule uer : mUnmeasuredElementRule)
               b |= uer.getElement().equals(el);
         if (!b) {
            if (res == null)
               res = new StringBuilder("Missing references for ");
            else
               res.append(", ");
            res.append(el.toAbbrev());
         }

      }
      throw new EPQException(res != null ? res.toString() : "Indeterminate error condition.");
   }

   private XRayTransitionSet forElement(final Element el) {
      for (final XRayTransitionSet xrts : mStandardData.keySet())
         if (xrts.getElement().equals(el))
            return xrts;
      return null;
   }

   /**
    * Constructs a ComputeZAFCorrection for the specified beam energy and
    * take-off angle.
    */
   public CompositionFromKRatios() {
      super("K-ratios to composition", "Default", "Default");
   }

   /**
    * getAllImplementations
    *
    * @return List&lt;AlgorithmClass&gt;
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#getAllImplementations()
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      final AlgorithmClass[] res = {this};
      return Arrays.asList(res);
   }

   /**
    * initializeDefaultStrategy
    *
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#initializeDefaultStrategy()
    */
   @Override
   protected void initializeDefaultStrategy() {
      addDefaultAlgorithm(IterationAlgorithm.class, IterationAlgorithm.Default);
   }

   /**
    * isReady - Is this object ready to compute the Composition from the
    * specified KRatioSet?
    *
    * @param krs
    * @return true if ready, false otherwise.
    */
   public boolean isReady(final KRatioSet krs) {
      for (final Element el : krs.getElementSet()) {
         if (forElement(el) != null)
            continue;
         boolean b = false;
         for (final UnmeasuredElementRule uer : mUnmeasuredElementRule)
            b |= el.equals(uer.getElement());
         if (!b)
            return false;
      }
      return true;
   }

   /**
    * addStandard - Add a standard for the specified XRayTransitionSet.
    *
    * @param xrts
    *           - The XRayTransitionSet for which to define a reference
    * @param ref
    *           - The Composition of the reference
    * @param refProps
    *           SpectrumProperties for the reference containing at least
    *           SpectrumProperties.BeamEnergy and
    *           SpectrumProperties.TakeOffAngle
    */
   public void addStandard(final XRayTransitionSet xrts, final Composition ref, final SpectrumProperties refProps) {
      if (mStandardData.containsKey(xrts))
         mStandardData.remove(xrts);
      mStandardData.put(xrts, new TransitionData(ref, refProps));
   }

   public void addExtraStandard(final XRayTransitionSet xrts, final Composition ref, final SpectrumProperties refProps) {
      if (mStandardData.containsKey(xrts))
         mStandardData.remove(xrts);
      mStandardData.put(xrts, new TransitionData(ref, refProps, false));
   }

   /**
    * setConvergenceCriterion - Set the convergence criterion
    *
    * @param epsilon
    *           - Default is 1.0e-5
    */
   public void setConvergenceCriterion(final double epsilon) {
      assert (epsilon < 1.0);
      assert (epsilon >= 0.0);
      if ((epsilon >= 0) && (epsilon < 1.0))
         mEpsilon = epsilon;
   }

   public double getConvergenceCriterion() {
      return mEpsilon;
   }

   /**
    * Returns the number of iterations required to get covergence.
    *
    * @return int
    */
   public int getIterationCount() {
      return mIterationCount;
   }

   /**
    * <p>
    * Returns a list of the XRayTransitionSet objects which were actually used
    * to compute the Composition in the previous call to
    * <code>compute(...)</code>. This is a subset of the transitions in the
    * KRatioSet argument to <code>compute(...)</code> with one and only one
    * XRayTransitionSet per element.
    * </p>
    *
    * @return Set&lt;XRayTransitionSet&gt;
    */
   public Map<Element, XRayTransitionSet> getUserSelectedTransitions() {
      return Collections.unmodifiableMap(mUserSelectedTransitions);
   }

   private double score(final KRatioSet krs, final XRayTransitionSet xrts) {
      try {
         /*
          * Consider Fe Ka vs Fe Kb. I(Ka)~10 I(Kb), E(Ka)=6.4, E(Kb)=7.1,
          * S(Ka)/S(Kb) = (10*6.4^2)/(7.1^2) ~ 8
          */
         final double e = FromSI.keV(xrts.getWeighiestTransition().getEnergy());
         final UncertainValue2 k = krs.getKRatioU(xrts);
         final double sn = k.doubleValue() > 0.0 ? 1.0 / k.fractionalUncertainty() : 0.0;
         return sn * e * e;
      } catch (final EPQException e) {
         return 0.0;
      }
   }

   private KRatioSet selectInitialKRatios(final KRatioSet measured) {
      final KRatioSet krs = new KRatioSet();
      for (final Element elm : measured.getElementSet())
         if (!isUnmeasuredElement(elm)) {
            XRayTransitionSet best = mUserSelectedTransitions.get(elm);
            if (best == null) {
               double bestScore = 0.0;
               for (final XRayTransitionSet xrts : measured.getTransitions(elm)) {
                  final double thisScore = score(measured, xrts);
                  if ((best == null) || (thisScore > bestScore)) {
                     best = xrts;
                     bestScore = thisScore;
                  }
               }
            }
            if (best != null)
               krs.addKRatio(best, measured.getKRatioU(best));
         }
      return krs;
   }

   public Composition iterate(final KRatioSet krs, final SpectrumProperties unkProps, final Composition initialGuess) throws EPQException {
      Composition prev = initialGuess;
      IterationAlgorithm ia = (IterationAlgorithm) getAlgorithm(IterationAlgorithm.class);
      FileWriter lfs = null;
      mWarningMessage = null;
      if (LOG_ITERATION)
         try {
            final File tmpFile = File.createTempFile("Iteration", ".txt");
            lfs = new FileWriter(tmpFile);
            ia = new IterationAlgorithm.DiagnosticIterationAlgorithm(ia, lfs);
            System.out.println("Logging to " + tmpFile.getCanonicalPath());
         } catch (final IOException e) {
            System.err.println("Unable to create diagnostic log file.");
         }
      try {
         ia.initialize(krs, initialGuess);
         UncertainValue2 bestDelta = UncertainValue2.MAX_VALUE;
         Composition bestComp = null;
         boolean fin = false;
         final int maxIter = Math.max(4 * initialGuess.getElementCount(), mMaxIterations);
         for (int iteration = 0; iteration < maxIter; ++iteration) {
            mIterationCount++;
            // Compute k-ratios based on the estimated composition
            final Map<XRayTransitionSet, Double> zafMap = new TreeMap<XRayTransitionSet, Double>();
            final KRatioSet calcKrs = new KRatioSet();
            for (final XRayTransitionSet xrts : krs.getTransitions()) {
               // zafCorr = ZAF[unk]/(C[std]*ZAF[std])
               final double zafCorr = prev.weightFraction(xrts.getElement(), false) > 0.0 ? compute(xrts, prev, unkProps) : 1.0;
               calcKrs.addKRatio(xrts, new UncertainValue2( //
                     zafCorr * prev.weightFraction(xrts.getElement(), false), //
                     krs.getKRatioU(xrts).uncertainty()) //
               );
               zafMap.put(xrts, zafCorr);
            }
            final UncertainValue2 delta = calcKrs.differenceU(krs);
            if (delta.doubleValue() < bestDelta.doubleValue()) {
               bestComp = prev;
               bestDelta = delta;
            }
            if (fin)
               break;
            if ((bestDelta.doubleValue() < mEpsilon) || (bestDelta.doubleValue() < (0.1 * bestDelta.uncertainty())))
               fin = true;
            if (mIterationCount >= maxIter) {
               final StringBuffer msg = new StringBuffer();
               msg.append("The composition estimate failed to converge.\n");
               msg.append("Measured = " + krs.toString() + "\n");
               msg.append("Best Calculated = " + calcKrs.toString() + "\n");
               msg.append("Best = " + bestComp.descriptiveString(false) + "\n");
               msg.append("Best delta = " + Double.toString(bestDelta.doubleValue()) + " = " + Double.toString(bestDelta.doubleValue() / mEpsilon)
                     + " × tolerance\n");
               mWarningMessage = msg.toString();
               System.err.print(msg);
               break;
            }
            prev = ia.compute(zafMap);
            for (final UnmeasuredElementRule uer : mUnmeasuredElementRule)
               prev = uer.compute(prev);
         }
         return bestComp;
      } finally {
         if (lfs != null)
            try {
               lfs.flush();
               lfs.close();
               lfs = null;
            } catch (final IOException e) {
               System.err.println("Error closing iteration log file.");
            }
      }
   }

   private Composition estimateInitialComposition(final KRatioSet krs) {
      Composition prev = new Composition();
      for (final XRayTransitionSet xrts : krs.getTransitions()) {
         TransitionData sd = mStandardData.get(xrts);
         /*
          * try { XRayTransition xrt = xrts.getWeighiestTransition(); final
          * CorrectionAlgorithm ca = getCorrectionAlgorithm(); ca.initialize(new
          * Material(xrts.getElement(), 1.0), xrt.getDestination(),
          * sd.mProperties); final double zafp = ca.computeZAFCorrection(xrt);
          * ca.initialize(sd.mComposition, xrt.getDestination(),
          * sd.mProperties); final double zafs = ca.computeZAFCorrection(xrt);
          * wf *= zafs / zafp; } catch (EPQException e) { e.printStackTrace(); }
          */
         final UncertainValue2 c = UncertainValue2.multiply(sd.mComposition.weightFraction(xrts.getElement(), true), krs.getKRatioU(xrts));
         prev.addElement(xrts.getElement(), c);
      }
      for (final UnmeasuredElementRule uer : mUnmeasuredElementRule)
         prev = uer.compute(prev);
      return prev.normalize();
   }

   private KRatioSet pickOptimized(final Composition firstC, final KRatioSet measured, final SpectrumProperties unkProps) {
      final KRatioSet res = new KRatioSet();
      // For each element select the best XRayTransitionSet
      for (final Element elm : measured.getElementSet()) {
         XRayTransitionSet best = mUserSelectedTransitions.get(elm);
         if (best == null) {
            // Get all measured XRayTransitionSet(s) for elm
            final Set<XRayTransitionSet> sxrts = measured.getTransitions(elm);
            best = sxrts.iterator().next();
            if (sxrts.size() > 1) {
               // Determine the XRTS for each line family that has the highest
               // intensity
               final Map<Integer, XRayTransitionSet> bestInFam = new TreeMap<Integer, XRayTransitionSet>();
               for (final XRayTransitionSet xrts : sxrts) {
                  final int fam = xrts.getWeighiestTransition().getFamily();
                  final XRayTransitionSet bestX = bestInFam.get(Integer.valueOf(fam));
                  if (bestX == null)
                     bestInFam.put(Integer.valueOf(fam), xrts);
                  else if (xrts.getSumWeight() > bestX.getSumWeight())
                     bestInFam.put(Integer.valueOf(fam), xrts);
               }
               // Determine which k-ratio has the best signal-to-noise
               final List<XRayTransitionSet> sorted = new ArrayList<>(bestInFam.values());
               sorted.sort(new Comparator<XRayTransitionSet>() {
                  @Override
                  public int compare(final XRayTransitionSet o1, final XRayTransitionSet o2) {
                     // largest (best) to smallest (worst)
                     return Double.compare(1.0 / measured.getKRatioU(o1).fractionalUncertainty(),
                           1.0 / measured.getKRatioU(o2).fractionalUncertainty());
                  }
               });
               best = null;
               double bestStoN = Double.NaN;
               UncertainValue2 bestC = null;
               final double GOOD_ENOUGH = 10.0; // Based on Curry's criterion
               // for measurability
               final double FRACTION_OF_BEST = 0.1;
               for (final XRayTransitionSet xrts : sorted) {
                  final UncertainValue2 k = measured.getKRatioU(xrts);
                  final double ston = 1.0 / k.fractionalUncertainty();
                  if (k.doubleValue() > 0.0) {
                     final UncertainValue2 c = UncertainValue2.multiply(massFractionU(xrts, firstC, unkProps), k.fractionalUncertaintyU());
                     if (Double.isNaN(bestStoN)) {
                        bestC = c;
                        best = xrts;
                        bestStoN = ston;
                     } else if ((ston > (bestStoN * FRACTION_OF_BEST)) && (ston > GOOD_ENOUGH)
                           && (c.fractionalUncertainty() < bestC.fractionalUncertainty())) {
                        bestC = c;
                        best = xrts;
                     }
                  }
               }
            }
         }
         res.addKRatio(best, measured.getKRatioU(best));
      }
      return res;
   }

   /**
    * <p>
    * Compute the Composition given the specified measured KRatioSet. Before
    * compute is called it is necessary to specify a reference for every Element
    * in the KRatioSet using addReference(...). If a reference is missing this
    * algorithm will throw an exception.
    * </p>
    * <p>
    * If more than one k-ratio is specified for a specific element then the
    * algorithm will select the k-ratio which minimizes the composition estimate
    * error. This optimization requires that the redundant k-ratios all have
    * associated error estimates (use
    * <code>KRatioSet.add(XRayTransitionSet, value, error)</code>). The method
    * will throw an exception if the error estimate is missing. If no redundant
    * k-ratios are provided then error estimates are not required.
    * </p>
    *
    * @param measured
    * @param unkProps
    *           SpectrumProperties for the unknown containing at least
    *           SpectrumProperties.BeamEnergy and
    *           SpectrumProperties.TakeOffAngle
    * @return The best estimate Composition for the measured k-ratio set
    * @throws EPQException
    */
   public Composition compute(final KRatioSet measured, final SpectrumProperties unkProps) throws EPQException {
      if (!isReady(measured))
         throwConfigurationError(measured);
      final KRatioSet measuredNz = new KRatioSet(), measuredZ = new KRatioSet();
      for (final XRayTransitionSet xrts : measured.getTransitions()) {
         final UncertainValue2 uv = measured.getRawKRatio(xrts);
         if (uv.doubleValue() > 0.0)
            measuredNz.addKRatio(xrts, uv);
         else
            measuredZ.addKRatio(xrts, uv);
      }
      // Stores the best estimate of the Composition
      mResult = null;
      mBestKRS = null;
      mIterationCount = 0;
      // Try up to mMaxIterations iterations until convergence achieved
      final KRatioSet firstKrs = selectInitialKRatios(measuredNz);
      final Composition firstC = iterate(firstKrs, unkProps, estimateInitialComposition(firstKrs));
      // Use the total uncertainty budget to pick the best sub-set of k-ratios
      mBestKRS = pickOptimized(firstC, measuredNz, unkProps);
      // Reiterate if necessary based on the optimized k-ratio set.
      final Composition best = mBestKRS.equals(firstKrs) ? firstC : iterate(mBestKRS, unkProps, firstC);
      // Compute the result with the full uncertainty budget
      Composition result = new Composition();
      for (final XRayTransitionSet xrts : mBestKRS.getTransitions())
         result.addElement(xrts.getElement(),
               UncertainValue2.multiply(massFractionU(xrts, best, unkProps), mBestKRS.getKRatioU(xrts).fractionalUncertaintyU()));
      // Computer other element rules
      for (final UnmeasuredElementRule uer : mUnmeasuredElementRule)
         result = uer.compute(result);
      for (final XRayTransitionSet xrts : measuredZ.getTransitions()) {
         final Element elm = xrts.getElement();
         if (!mBestKRS.getElementSet().contains(elm)) {
            final UncertainValue2 uv2 = measuredZ.getKRatioU(xrts);
            mBestKRS.addKRatio(xrts, uv2);
            result.addElement(elm, uv2);
         }
      }
      mResult = result;
      return mResult;
   }

   /**
    * Returns the maximum number of iterations to refine the compositional
    * estimate
    *
    * @return Returns the maxIterations.
    */
   public double getMaxIterations() {
      return mMaxIterations;
   }

   /**
    * Sets the maximum number of iterations to refine the compositional
    * estimate.
    *
    * @param maxIterations
    *           The value to which to set maxIterations.
    */
   public void setMaxIterations(final int maxIterations) {
      mMaxIterations = maxIterations;
   }

   /**
    * Returns the best estimate of the composition as computed in the last call
    * to compute. (null if compute hasn't been called or failed before getting
    * to the iteration loop.) This method returns the best estimate Composition
    * regardless of whether compute converged.
    *
    * @return The best estimate Composition
    */
   public Composition getResult() {
      return mResult;
   }

   public KRatioSet getQuantifiedKRatios() {
      return mBestKRS;
   }

   /**
    * Clear all UnmeasuredElementRule objects from the list.
    */
   public void clearUnmeasuredElementRules() {
      mUnmeasuredElementRule.clear();
   }

   /**
    * Add an UnmeasuredElementRule to the list. This method ensures that there
    * is only one UnmeasuredElementRule per element. The rules evaluate in the
    * order in which they are added so it is possible to add one element by
    * stoichiometry and another by difference from 100.0%.
    * UnmeasuredElementRules with getElement().equals(Element.None) are special
    * it that more than one can exist at a time.
    *
    * @param uer
    */
   public void addUnmeasuredElementRule(final UnmeasuredElementRule uer) {
      if (uer.getElement().equals(Element.None))
         for (final Iterator<UnmeasuredElementRule> i = mUnmeasuredElementRule.iterator(); i.hasNext();)
            if (i.next().getElement().equals(uer.getElement()))
               i.remove();
      mUnmeasuredElementRule.add(uer);
   }

   /**
    * @return The number of UnmeasuredElementRule objects in the list
    */
   public int getUnmeasuredElementRuleCount() {
      return mUnmeasuredElementRule.size();
   }

   /**
    * Returns the i-th UnmeasuredElementRule in the list.
    *
    * @return UnmeasuredElementRule
    */
   public List<UnmeasuredElementRule> getUnmeasuredElementRules() {
      return Collections.unmodifiableList(mUnmeasuredElementRule);
   }

   public boolean isUnmeasuredElement(final Element elm) {
      for (final UnmeasuredElementRule uer : mUnmeasuredElementRule)
         if (uer.getElement().equals(elm))
            return true;
      return false;
   }

   public CorrectionAlgorithm getCorrectionAlgorithm() {
      if (mCorrectionAlgorithm == null)
         mCorrectionAlgorithm = AlgorithmUser.getDefaultCorrectionAlgorithm();
      assert mCorrectionAlgorithm != null : "No correction algorithm defined in CompositionFromKRatios.";
      return mCorrectionAlgorithm;
   }

   /**
    * @param xrts
    *           The set of x-ray transitions for which to compute the ZAF
    *           correction.
    * @param unkComp
    *           The composition of the material
    * @param unkProps
    *           The SpectrumProperties for the material containing at least
    *           SpectrumProperties.BeamEnergy and
    *           SpectrumProperties.TakeOffAngle. These properties must match
    *           those in addStandad(...)
    * @return Returns ZAF[unk]/(C[std]*ZAF[std])
    */
   public double compute(final XRayTransitionSet xrts, final Composition unkComp, final SpectrumProperties unkProps) {
      assert (unkProps.isDefined(SpectrumProperties.BeamEnergy));
      final SampleShape ss = unkProps.getSampleShapeWithDefault(SpectrumProperties.SampleShape, null);
      assert (ss == null) || getCorrectionAlgorithm().supports(ss.getClass());
      double sum = 0.0, sumW = 0.0;
      final TransitionData stdData = mStandardData.get(xrts);
      final double toa = SpectrumUtils.getTakeOffAngle(unkProps);
      if (stdData.mValidate) {
         final SpectrumProperties refProps = stdData.mProperties;
         if (Math.abs(refProps.getNumericWithDefault(SpectrumProperties.BeamEnergy, -1.0)
               - unkProps.getNumericWithDefault(SpectrumProperties.BeamEnergy, -2.0)) > 0.01)
            throw new EPQFatalException("The beam energy for the standard and unknown must match.");
         if (Math.abs(SpectrumUtils.getTakeOffAngle(refProps) - toa) > Math.toRadians(1.0))
            throw new EPQFatalException("The take-off angle for the standard and unknown must match to within a degree.");
      }
      final double norm = xrts.getWeighiestTransition().getWeight(XRayTransition.NormalizeFamily);
      for (final XRayTransition xrt : xrts) {
         final double trUnk = (mCoating != null ? mCoating.computeTransmission(toa, xrt) : 1.0);
         final double kStd = stdData.getKstd(xrt);
         final double w = xrt.getWeight(XRayTransition.NormalizeFamily);
         if ((w / norm) >= mMinWeight) {
            final AtomicShell shell = xrt.getDestination();
            if (unkComp.containsElement(shell.getElement()))
               try {
                  final CorrectionAlgorithm ca = getCorrectionAlgorithm();
                  ca.initialize(unkComp, shell, unkProps);
                  // kstd = ZAFstd Cstd trStd
                  sum += (w * ca.computeZAFCorrection(xrt) * trUnk) / kStd;
                  // sum += (w ZAFunk trUnk) / (ZAFstd Cstd trStd)
                  sumW += w;
               } catch (final EPQException e) {
                  e.printStackTrace();
               }
         }
      }
      return sumW > 0.0 ? sum / sumW : 1.0;
   }

   /**
    * @param xrts
    *           The set of x-ray transitions for which to compute the ZAF
    *           correction.
    * @param unkComp
    *           The composition of the material
    * @param unkProps
    *           The SpectrumProperties for the material containing at least
    *           SpectrumProperties.BeamEnergy and
    *           SpectrumProperties.TakeOffAngle. These properties must match
    *           those in addStandad(...)
    * @return Returns the composition of the unknown with accuracy related
    *         uncertainties
    */
   public UncertainValue2 massFractionU(final XRayTransitionSet xrts, final Composition unkComp, final SpectrumProperties unkProps) {
      assert (unkProps.isDefined(SpectrumProperties.BeamEnergy));
      final SampleShape ss = unkProps.getSampleShapeWithDefault(SpectrumProperties.SampleShape, null);
      assert (ss == null) || getCorrectionAlgorithm().supports(ss.getClass());
      double sumW = 0.0;
      UncertainValue2 sum = UncertainValue2.ZERO;
      final TransitionData stdData = mStandardData.get(xrts);
      if (stdData.mValidate) {
         final SpectrumProperties stdProps = stdData.mProperties;
         if (Math.abs(stdProps.getNumericWithDefault(SpectrumProperties.BeamEnergy, -1.0)
               - unkProps.getNumericWithDefault(SpectrumProperties.BeamEnergy, -2.0)) > 0.01)
            throw new EPQFatalException("The beam energy for the standard and unknown must match.");
         if (Math.abs(SpectrumUtils.getTakeOffAngle(stdProps) - SpectrumUtils.getTakeOffAngle(unkProps)) > Math.toRadians(1.0))
            throw new EPQFatalException("The take-off angle for the standard and unknown must match to within a degree.");
      }
      final Element elm = xrts.getElement();
      final UncertainValue2 cUnk = unkComp.weightFractionU(elm, false);
      if (cUnk.doubleValue() > 0.0) {
         final double norm = xrts.getWeighiestTransition().getWeight(XRayTransition.NormalizeFamily);
         for (final XRayTransition xrt : xrts) {
            final double w = xrt.getWeight(XRayTransition.NormalizeFamily);
            if ((w / norm) >= mMinWeight)
               try {
                  sum = UncertainValue2.add(sum,
                        UncertainValue2.multiply(w, CorrectionAlgorithm.XPP.kratio(stdData.mComposition, unkComp, xrt, unkProps)));
                  sumW += w;
               } catch (final EPQException e) {
                  e.printStackTrace();
               }
         }
         return UncertainValue2.multiply(cUnk, UncertainValue2.divide(sum, sumW).fractionalUncertaintyU());
      } else
         return UncertainValue2.invert(stdData.mComposition.weightFractionU(elm, false));
   }

   public void setMinWeight(final double mw) {
      mMinWeight = mw;
   }

   public double getMinWeight() {
      return mMinWeight;
   }

   public boolean isWarning() {
      return mWarningMessage != null;
   }

   public String getWarningMessage() {
      return mWarningMessage;
   }

   public ConductiveCoating getCoating() {
      return mCoating;
   }

   public void setUnknownCoating(final ConductiveCoating coating) {
      mCoating = coating;
   }

}
