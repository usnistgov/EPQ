package gov.nist.microanalysis.EPQLibrary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.ParticleSignature.StripMode;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * A class for computing MLLSQ Signatures - a quantification metric suitable for
 * particles. The class can be instantiated and configured once and applied to
 * multiple spectra.
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
public class MLLSQSignature implements Cloneable {

   private double mThreshold = 3.29;
   private final TreeMap<Element, ISpectrumData> mStandards;
   private TreeMap<XRayTransitionSet, Double> mOptimal;
   private final EDSDetector mDetector;
   private boolean mZafCorrectRefs = true;
   private boolean mStoreResiduals = true;
   private final double mBeamEnergy; // in
   // Joules
   private TreeMap<Element, StripMode> mStrip = new TreeMap<Element, StripMode>();

   private Set<Element> mDontFit = new TreeSet<Element>();

   private double mChiSquared = 0.0;
   private double mCounts = Double.NaN;
   private ISpectrumData mResidual;

   private boolean mStripUnlikely;
   private FilterFit mFilterFit;

   /**
    * Constructs a MLLSQSignature object to process spectra from the specified
    * detector at the specified beam energy.
    * 
    * @param detector
    *           An ElectronProbe.EDSDetector object representing the
    *           detector/instrument on which these spectra were collected.
    * @param e0
    *           The beam energy in Joules
    */
   public MLLSQSignature(EDSDetector detector, double e0) {
      mStandards = new TreeMap<Element, ISpectrumData>();
      mOptimal = null;
      mDetector = detector;
      mBeamEnergy = e0;
      mStrip = ParticleSignature.defaultStrip();
      mStripUnlikely = true;
      mDontFit = new TreeSet<Element>();
   }

   @Override
   public MLLSQSignature clone() {
      final MLLSQSignature res = new MLLSQSignature(mDetector, mBeamEnergy);
      res.mStandards.putAll(mStandards);
      if (mOptimal != null)
         res.mOptimal = new TreeMap<XRayTransitionSet, Double>(mOptimal);
      res.mZafCorrectRefs = mZafCorrectRefs;
      res.mStrip = new TreeMap<Element, StripMode>(mStrip);
      res.mChiSquared = mChiSquared;
      res.mCounts = mCounts;
      res.mResidual = mResidual;
      res.mStripUnlikely = mStripUnlikely;
      res.mThreshold = mThreshold;
      res.mDontFit = new TreeSet<Element>(mDontFit);
      return res;
   }

   public void addReference(Element elm, Composition comp, ISpectrumData spec) throws EPQException {
      mFilterFit = null;
      assert comp.containsElement(elm);
      final ISpectrumData ref = SpectrumUtils.copy(spec);
      final SpectrumProperties sp = ref.getProperties();
      sp.setCompositionProperty(SpectrumProperties.StandardComposition, comp);
      sp.apply(mDetector.getProperties());
      if (!sp.isDefined(SpectrumProperties.BeamEnergy))
         sp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(mBeamEnergy));
      if (!sp.isDefined(SpectrumProperties.ProbeCurrent))
         sp.setNumericProperty(SpectrumProperties.ProbeCurrent, 1.0);
      if (!sp.isDefined(SpectrumProperties.LiveTime))
         sp.setNumericProperty(SpectrumProperties.LiveTime, 60.0);
      assert Math.abs(FromSI.eV(mBeamEnergy) - SpectrumUtils.getBeamEnergy(ref)) / FromSI.eV(mBeamEnergy) < 0.01 : "Beam energies do not match...";
      mStandards.put(elm, ref);
      mOptimal = null;
   }

   public TreeMap<XRayTransitionSet, Double> getOptimalTransitions() {
      return mOptimal;
   }

   public Set<Element> stripped() {
      Set<Element> res = new TreeSet<Element>();
      for (Element elm : mStrip.keySet())
         if (mStrip.get(elm) == StripMode.Strip)
            res.add(elm);
      return res;
   }

   public Set<Element> excluded() {
      Set<Element> res = new TreeSet<Element>();
      for (Element elm : mStrip.keySet())
         if (mStrip.get(elm) == StripMode.Exclude)
            res.add(elm);
      return res;
   }

   public Set<Element> dontFit() {
      return mDontFit;
   }

   public void setDontFit(Collection<Element> elms) {
      mDontFit.clear();
      if (elms != null)
         mDontFit.addAll(elms);
   }

   public StripMode stripMode(Element elm) {
      return mStrip.get(elm) == null ? StripMode.Normal : mStrip.get(elm);
   }

   /**
    * Compute the full k-ratio set for all fitted transitions.
    * 
    * @param spec
    * @return KRatioSet
    * @throws EPQException
    */
   public KRatioSet compute(ISpectrumData spec) throws EPQException {
      final SpectrumProperties specProps = spec.getProperties();
      specProps.setDetector(mDetector);
      if (!specProps.isDefined(SpectrumProperties.LiveTime)) {
         final double rt = specProps.getNumericWithDefault(SpectrumProperties.RealTime, 1.0);
         specProps.setNumericProperty(SpectrumProperties.LiveTime, 0.8 * rt);
      }
      // assert mBeamEnergy == ToSI.eV(SpectrumUtils.getBeamEnergy(spec));
      if (mFilterFit == null) {
         mFilterFit = new FilterFit(mDetector, mBeamEnergy);
         mFilterFit.setStripUnlikely(mStripUnlikely);
         mFilterFit.setResidualModelThreshold(0.0);
         for (Map.Entry<Element, ISpectrumData> me : mStandards.entrySet())
            mFilterFit.addReference(me.getKey(), me.getValue());
         // Special rules for one element masquerading as another
         final FilterFit.CompoundCullingStrategy cs = new FilterFit.CompoundCullingStrategy();
         // Special rules for one element masquerading as another
         final FilterFit.SpecialCulling sc = new FilterFit.SpecialCulling();
         if (mStrip.get(Element.Fe) != StripMode.Strip)
            sc.add(Element.F, Element.Fe);
         if (mStrip.get(Element.Th) != StripMode.Strip)
            sc.add(Element.Ag, Element.Th);
         if (mStrip.get(Element.Sr) != StripMode.Strip)
            sc.add(Element.Si, Element.Sr);
         if (mStrip.get(Element.Si) != StripMode.Strip) {
            sc.add(Element.Sr, Element.Si);
            sc.add(Element.W, Element.Si);
         }
         if (mStrip.get(Element.Ca) != StripMode.Strip)
            sc.add(Element.Sb, Element.Ca);
         if (mStrip.get(Element.S) != StripMode.Strip) {
            sc.add(Element.S, Element.Pb);
            sc.add(Element.S, Element.Mo);
         }
         if (mStrip.get(Element.Pb) != StripMode.Strip) {
            sc.add(Element.Pb, Element.S);
            sc.add(Element.Pb, Element.Mo);
         }
         cs.append(sc);
         assert mOptimal == null;
         mOptimal = computeOptimal(mFilterFit, mBeamEnergy);
         cs.append(new FilterFit.CullByOptimal(mThreshold, mOptimal.keySet()));
         mFilterFit.setCullingStrategy(cs);
      }
      mFilterFit.forceZero(mDontFit);
      final KRatioSet res = mFilterFit.getKRatios(spec);
      mChiSquared = mFilterFit.chiSquared();
      mResidual = mFilterFit.getResidualSpectrum(spec);
      mCounts = mFilterFit.getFitEventCount(spec, stripped());
      return res;
   }

   public TreeMap<XRayTransitionSet, Double> computeOptimal(FilterFit ff, double e0) throws EPQException {
      TreeMap<XRayTransitionSet, Double> res = new TreeMap<XRayTransitionSet, Double>();
      // Figure out which lines to use and the matrix correction factors
      final double MIN_E = ToSI.eV(2.0e3);
      for (final Map.Entry<Element, ISpectrumData> me : mStandards.entrySet()) {
         final Element elm = me.getKey();
         RegionOfInterestSet.RegionOfInterest bestRoi = null;
         int bestFam = -1;
         XRayTransition bestXrt = null;
         TreeSet<FilteredSpectrum> sfs = new TreeSet<>(ff.getFilteredSpectra(elm));
         for (final FilteredSpectrum fs : sfs.descendingSet()) {
            final RegionOfInterestSet.RegionOfInterest roi = fs.getRegionOfInterest();
            final XRayTransition weightiest = fs.getXRayTransitionSet().getWeighiestTransition();
            final int fam = weightiest.getFamily();
            if ((bestFam == -1) && (weightiest.getEdgeEnergy() < 0.8 * e0)) {
               bestRoi = roi;
               bestFam = fam;
               bestXrt = weightiest;
            } else if ((fam == bestFam) && (weightiest.getWeight(XRayTransition.NormalizeDefault) > //
                  bestXrt.getWeight(XRayTransition.NormalizeDefault))) {
               bestRoi = roi;
               bestFam = fam;
               bestXrt = weightiest;
            } else if ((fam > bestFam) && (weightiest.getEnergy() > MIN_E)) {
               bestRoi = roi;
               bestFam = fam;
               bestXrt = weightiest;
            }
         }
         // Compute the matrix correction factor
         assert bestXrt != null;
         final XRayTransitionSet xrts = bestRoi.getXRayTransitionSet(elm);
         final XRayTransition xrt = xrts.getWeighiestTransition();
         assert xrt != null;
         final CorrectionAlgorithm ca = new XPP1991();
         /**
          * Iref -> Measured intensity of reference Ipure = Iref/(wRef*ZAFref)
          * wUnk ~ Iunk / Ipure = Iunk/(Iref/(wRef*ZAFref)) =
          * (Iunk/Iref)*wRef*ZAFref
          */
         final SpectrumProperties refProps = me.getValue().getProperties();
         final Composition comp = refProps.getCompositionProperty(SpectrumProperties.StandardComposition);
         final double zaf = mZafCorrectRefs ? ca.relativeZAF(comp, xrt, refProps)[3] : 1.0;
         res.put(xrts, Double.valueOf(comp.weightFraction(elm, true) * zaf));
      }
      // System.out.println(res);
      return res;
   }

   /**
    * Trims the k-ratio set from <code>compute</code> to a sub-set containing
    * one line per element. The line is selected such that higher energy lines
    * are favored over lower energy lines since presumably the absorption will
    * be less of an issue.
    * 
    * @param krs
    * @return KRatioSet
    */
   public KRatioSet optimalKRatioSet(KRatioSet krs) {
      final KRatioSet bestKrs = new KRatioSet();
      for (final XRayTransitionSet xrts : mOptimal.keySet())
         bestKrs.addKRatio(xrts, krs.getKRatio(xrts), krs.getError(xrts));
      return bestKrs;
   }

   /**
    * Computes the signature of the KRatioSet as returned from
    * <code>compute</code>. The signature is the optimal KRatioSet with the
    * elements in the the stripping set removed and the remaining elements
    * normalized to 1.0. If oxygen is included in the stripping set then oxygen
    * is handled slightly differently. Oxygen is added back in after all the
    * other element values have been computed and the result renormalized. Then
    * the resulting value for oxygen is included in the signature. Thus all
    * elements in the signature except O will normalize to 1.0 but there will be
    * a value for O which suggests its abundance.
    * 
    * @param krs
    * @return KRatioSet
    */
   public ParticleSignature signature(KRatioSet krs) {
      final KRatioSet optimal = optimalKRatioSet(krs);
      final ParticleSignature res = new ParticleSignature(stripped(), excluded());
      for (final XRayTransitionSet xrts : optimal.getTransitions())
         res.add(xrts.getElement(), UncertainValue2.multiply(mOptimal.get(xrts).doubleValue(), optimal.getKRatioU(xrts)));
      return res;
   }

   public double getChiSquared() {
      return mChiSquared;
   }

   /**
    * Return a list of elements represented by a reference within this object.
    * 
    * @return TreeSet&lt;Element&gt;
    */
   public TreeSet<Element> getElements() {
      return new TreeSet<Element>(mStandards.keySet());
   }

   /**
    * All all the Element objects in the specified collection.
    * 
    * @param col
    */
   public void addStripped(Collection<Element> col) {
      for (Element el : col)
         mStrip.put(el, StripMode.Strip);
   }

   public void addExluded(Collection<Element> col) {
      for (Element el : col)
         mStrip.put(el, StripMode.Exclude);
   }

   /**
    * Is an element in the list of elements which will be stripped (ie. ignored
    * in the signature)
    * 
    * @param elm
    * @return boolean
    */
   public boolean isStripped(Element elm) {
      return (mStrip.get(elm) != null) && (mStrip.get(elm) == StripMode.Strip);
   }

   /**
    * Clear the list of elements which will be stripped (ie. ignored in the
    * signature)
    */
   public void clearStripped() {
      mStrip.clear();
   }

   /**
    * Are the references ZAF corrected in addition to corrected for composition?
    * 
    * @return boolean true to ZAF correct, false otherwise
    */
   public boolean zafCorrectRefs() {
      return mZafCorrectRefs;
   }

   /**
    * Determines whether the references ZAF corrected in addition to corrected
    * for composition?
    * 
    * @param zafCorrectRefs
    *           true to ZAF correct, false otherwise
    */
   public void setZafCorrectRefs(boolean zafCorrectRefs) {
      if (mZafCorrectRefs != zafCorrectRefs) {
         mZafCorrectRefs = zafCorrectRefs;
         mOptimal = null;
      }
   }

   /**
    * Returns the residual spectrum computed in the last iteration of
    * compute(...)
    * 
    * @return Returns the residual as an ISpectrumData
    */
   public ISpectrumData getResidual() {
      return mResidual;
   }

   /**
    * Returns a list of the reference spectra.
    * 
    * @return Returns the references.
    */
   public ArrayList<ISpectrumData> getStandards() {
      return new ArrayList<ISpectrumData>(mStandards.values());
   }

   /**
    * Following a fit returns the number of x-ray events explained by the fit
    * excluding stripped elements.
    * 
    * @return Returns the counts.
    */
   public double getCounts() {
      assert mResidual != null;
      return mCounts;
   }

   /**
    * Determines whether the list of elements are initially culled to remove
    * element for which there are not likely peaks.
    * 
    * @return Returns the stripUnlikely.
    */
   public boolean getStripUnlikely() {
      return mStripUnlikely;

   }

   /**
    * Determines whether the list of elements are initially culled to remove
    * element for which there are not likely peaks.
    * 
    * @param stripUnlikely
    *           The value to which to set stripUnlikely.
    */
   public void setStripUnlikely(boolean stripUnlikely) {
      mFilterFit = null;
      mStripUnlikely = stripUnlikely;
   }

   public boolean storeResiduals() {
      return mStoreResiduals;
   }

   public void setStoreResiduals(boolean storeResiduals) {
      mStoreResiduals = storeResiduals;
   }

   public void setThreshold(double thresh) {
      mThreshold = Math.min(Math.max(0.0, thresh), 10.0);
   }

   public double getThreshold(double thresh) {
      return mThreshold;
   }

   public void resetThreshold() {
      mThreshold = 3.29;
   }

}
