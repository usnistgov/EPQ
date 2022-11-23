package gov.nist.microanalysis.EPQLibrary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.CompositionFromKRatios.OxygenByStoichiometry;
import gov.nist.microanalysis.EPQLibrary.CompositionFromKRatios.UnmeasuredElementRule;
import gov.nist.microanalysis.EPQLibrary.FilterFit.CullingStrategy;
import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.EPQTools.WriteSpectrumAsEMSA1_0;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Pair;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * Quantifies EDS spectra based on a set of spectra collected from standards and
 * an associated set of reference spectra. The difference between
 * <i>standards</i> and <i>references</i> can be subtle. Standards provide a
 * record of the intensity of characteristic emission for a specified element in
 * a known material. References provide reliable characteristic line shape
 * information. Standards can be references when the required characteristic
 * lines are not obscured by other elements.
 * </p>
 * <p>
 * The distinction between standards and references is designed to allow for
 * comparing unknowns to similar known materials. For example K412 can be
 * readily and accurately quantified against K411 and Al since K411 and K412 are
 * similar in O, Si, Mg, Ca and Fe content but K412 contains Al. Presumably the
 * ZAF corrections are similar in K411 and K412 up to but not including the Al
 * content.
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
public class QuantifyUsingStandards
   extends
   QuantificationOutline {

   /**
    * Maps elements into the spectra to be used as standards for this element.
    */
   private final Map<Element, ISpectrumData> mStdSpectra = new HashMap<Element, ISpectrumData>();

   /**
    * Maps ROIs into the spectra that are used as a reference for the ROI
    */
   private final Map<RegionOfInterest, ISpectrumData> mRefSpectra = new HashMap<RegionOfInterest, ISpectrumData>();

   /**
    * Copies of the standard spectra massaged to act as references.
    */
   private final Map<RegionOfInterest, ISpectrumData> mStdsAsRef = new HashMap<RegionOfInterest, ISpectrumData>();

   /**
    * The object used to convert k-ratios into composition.
    */
   private final CompositionFromKRatios mCfKR = new CompositionFromKRatios();

   /**
    * A mechanism for removing elements with zero or near zero presence.
    */
   private CullingStrategy mCullingStrategy = new FilterFit.CullByVariance(0.0);

   /**
    * The references are fit against both the standard and the unknown. The
    * result of this fit for the standard is cached in mReferenceScale.
    */
   transient private final TreeMap<RegionOfInterest, UncertainValue2> mReferenceScale = new TreeMap<RegionOfInterestSet.RegionOfInterest, UncertainValue2>();

   static private final boolean INCLUDE_REF_U = false;

   private final boolean mKRatiosOnly;

   /**
    * Sometimes it is nice to be able to add additional k-ratios derived from
    * another source (likely WDS) to the mix.
    */
   class ExtraKRatio {
      private final XRayTransitionSet mXRTS;
      private final Number mKRatio;
      private final Composition mComposition;
      private final SpectrumProperties mProperties;

      ExtraKRatio(final XRayTransitionSet xrts, final Number kratio, final Composition comp, final SpectrumProperties props) {
         mXRTS = xrts;
         mKRatio = kratio;
         mComposition = comp;
         mProperties = props;
      }

   }

   private final Map<Element, ExtraKRatio> mExtraKRatios = new TreeMap<Element, ExtraKRatio>();

   /**
    * A container for a quantification result. It represents both the
    * QuantifySpectrumUsingStandards object and the Result.
    *
    * @author nicholas
    */
   public class Result {
      private Composition mComposition;
      final private ISpectrumData mUnknown;
      private ISpectrumData mResidual;
      private String mWarning = null;

      protected Result(final ISpectrumData unk) {
         mComposition = null;
         mResidual = null;
         mUnknown = unk;
      }

      public Result(final Result res) {
         mComposition = res.mComposition;
         mUnknown = res.mUnknown;
         mResidual = res.mResidual;
         mWarning = res.mWarning;
      }

      /**
       * @return The Composition
       */
      public Composition getComposition() {
         return mComposition;
      }

      /**
       * @return The unknown spectrum (associated with the composition)
       */
      public ISpectrumData getUnknown() {
         return mUnknown;
      }

      /**
       * @return The unknown minus all peaks which have been accounted for
       */
      public ISpectrumData getResidual() {
         return mResidual;
      }

      public QuantifyUsingStandards getParent() {
         return QuantifyUsingStandards.this;
      }

      public String getWarningMessage() {
         return mWarning;
      }
   }

   /**
    * @param det The detector on which the unknown and standards were collected.
    * @param beamEnergy in Joules
    */
   public QuantifyUsingStandards(final EDSDetector det, final double beamEnergy) {
      this(det, beamEnergy, false);
   }

   /**
    * @param det The detector on which the unknown and standards were collected.
    * @param beamEnergy in Joules
    */
   public QuantifyUsingStandards(final EDSDetector det, final double beamEnergy, final boolean kRatiosOnly) {
      super(det, beamEnergy);
      mKRatiosOnly = kRatiosOnly;
   }

   /**
    * Updates the reference list by first discovering which standards can be
    * used as references then updating this list using the list of user supplied
    * references. Returns an immutable association of RegionOfInterest to
    * ISpectrumData
    *
    * @return Map&lt;RegionOfInterestSet.RegionOfInterest, ISpectrumData&gt;
    */
   public Map<RegionOfInterestSet.RegionOfInterest, ISpectrumData> getReferenceSpectra() {
      final Map<RegionOfInterestSet.RegionOfInterest, ReferenceMaterial> rc = super.getAssignedReferences();
      final Map<RegionOfInterestSet.RegionOfInterest, ISpectrumData> res = new HashMap<RegionOfInterestSet.RegionOfInterest, ISpectrumData>();
      for(final Map.Entry<RegionOfInterest, ReferenceMaterial> me : rc.entrySet()) {
         final ISpectrumData spec = getReferenceSpectrum(me.getKey());
         if(spec != null)
            res.put(me.getKey(), spec);
      }
      return res;
   }

   /**
    * Returns the best available spectrum to act as a reference for the
    * specified ROI. Defaults to the user specified reference, then checks
    * whether the standard can be used as a reference.
    *
    * @param roi
    * @return ISpectrumData
    */
   public ISpectrumData getReferenceSpectrum(final RegionOfInterest roi) {
      assert roi.getElementSet().size() == 1;
      ISpectrumData res = null;
      if(mRefSpectra.containsKey(roi))
         res = mRefSpectra.get(roi);
      else {
         if(!mStdsAsRef.containsKey(roi))
            for(final Element elm : getMeasuredElements()) {
               final ReferenceMaterial rm = super.getReference(roi);
               assert rm != null : roi.toString() + "\t" + elm.toAbbrev();
               final Composition std = getStandard(elm);
               assert std != null : roi.toString() + "\t" + elm.toAbbrev();
               assert rm.getComposition() != null : roi.toString() + "\t" + elm.toAbbrev();
               if(std.equals(rm.getComposition())) {
                  mReferenceScale.put(roi, UncertainValue2.ONE);
                  mStdsAsRef.put(roi, makeReference(mStdSpectra.get(elm)));
                  break;
               }
            }
         res = mStdsAsRef.get(roi);
      }
      return res;
   }

   public Set<RegionOfInterestSet.RegionOfInterest> getROISWithAssignedReferences() {
      final Map<RegionOfInterestSet.RegionOfInterest, ReferenceMaterial> rc = super.getAssignedReferences();
      final Set<RegionOfInterestSet.RegionOfInterest> res = new HashSet<RegionOfInterestSet.RegionOfInterest>();
      for(final Map.Entry<RegionOfInterest, ReferenceMaterial> me : rc.entrySet()) {
         final RegionOfInterest roi = me.getKey();
         if(mRefSpectra.containsKey(roi)) {
            final ISpectrumData ref = mRefSpectra.get(roi);
            final Element elm = roi.getElementSet().first();
            final ISpectrumData std = getStandardSpectrum(elm);
            if((std == null) || (!ref.equals(std)))
               res.add(roi);
         }
      }
      return res;
   }

   /**
    * Adds a standard spectrum associated with a material of the specified
    * composition for the specified element. A standard provides a information
    * about the intensity for all x-ray line intensities associated with the
    * specified element. The region-of-interests associated with this element
    * are determined and for each region-of-interest it is determined whether
    * the standard can also be used as a reference. This may mean that after
    * adding a standard some but not necessarily all associated references are
    * also assigned.
    *
    * @param elm The element
    * @param comp The composition of the standard material
    * @param stripElms A set of Element objects representing elements to strip
    *           from the standard.
    * @param std The spectrum
    * @throws EPQException
    */
   public void addStandard(final Element elm, final Composition comp, final Set<Element> stripElms, final ISpectrumData std)
         throws EPQException {
      assert std != null;
      // assert spec2.getProperties().getDetector() != null;
      super.addStandard(elm, comp, stripElms);
      final ISpectrumData copy = preProcessSpectrum(std);
      copy.getProperties().setCompositionProperty(SpectrumProperties.StandardComposition, comp);
      mStdSpectra.put(elm, copy);
   }

   /**
    * Added to keep this function available for scripting... Adds a standard
    * spectrum associated with a material of the specified composition for the
    * specified element. A standard provides a information about the intensity
    * for all x-ray line intensities associated with the specified element. The
    * region-of-interests associated with this element are determined and for
    * each region-of-interest it is determined whether the standard can also be
    * used as a reference. This may mean that after adding a standard some but
    * not necessarily all associated references are also assigned.
    *
    * @param elm The element
    * @param comp The composition of the standard material
    * @param std The spectrum
    * @throws EPQException
    */
   public void addStandard(final Element elm, final Composition comp, final ISpectrumData std)
         throws EPQException {
      addStandard(elm, comp, Collections.emptySet(), std);
   }

   public void addStandard(final StandardBundle bundle)
         throws EPQException {
      final ISpectrumData std = bundle.getStandard();
      final Composition comp = std.getProperties().getCompositionWithDefault(SpectrumProperties.StandardComposition, null);
      assert comp != null;
      addStandard(bundle.getElement(), comp, bundle.getStrippedElements(), std);
      for(final Map.Entry<RegionOfInterest, ISpectrumData> me : bundle.getReferences().entrySet())
         addReference(me.getKey(), me.getValue(), me.getValue().getProperties().getElements());
   }

   /**
    * addExtraKRatio is a mechanism to mix in additional WDS (or other) derived
    * k-ratios into the quantification process. In the case of redundant data,
    * these extra k-ratios replace those for the Element implicitly provided
    * through <code>xrts</code>.
    *
    * @param xrt
    * @param kratio
    * @param stdMat
    * @param props - Beam energy, take-off angle minimum
    */
   public void addExtraKRatio(final XRayTransition xrt, final Number kratio, final Composition stdMat, final SpectrumProperties props) {
      addExtraKRatio(new XRayTransitionSet(xrt), kratio, stdMat, props);
   }

   /**
    * addExtraKRatio is a mechanism to mix in additional WDS (or other) derived
    * k-ratios into the quantification process. In the case of redundant data,
    * these extra k-ratios replace those for the Element implicitly provided
    * through <code>xrts</code>.
    *
    * @param xrts
    * @param kratio
    * @param stdMat
    * @param props - Beam energy, take-off angle minimum
    */
   public void addExtraKRatio(final XRayTransitionSet xrts, final Number kratio, final Composition stdMat, final SpectrumProperties props) {
      mExtraKRatios.put(xrts.getElement(), new ExtraKRatio(xrts, kratio, stdMat, props));
   }

   public Set<Element> getElementsByExtraKRatio() {
      return Collections.unmodifiableSet(mExtraKRatios.keySet());
   }

   public void clearExtraKRatios() {
      mExtraKRatios.clear();
   }

   /**
    * Adjusts spectrum properties to make this spectrum suitable for use as a
    * reference. Basically this just ensures that the probe current and live
    * time are set to some values.
    *
    * @param spec
    * @return ISpectrum A modified copy of the original spectrum
    */
   private ISpectrumData makeReference(final ISpectrumData spec) {
      final ISpectrumData refSpec = preProcessSpectrum(spec);
      final SpectrumProperties refSp = refSpec.getProperties();
      final double faraday=SpectrumUtils.getAverageFaradayCurrent(refSp, 1.0);
      refSp.setNumericProperty(SpectrumProperties.ProbeCurrent, faraday);
      if(refSp.getNumericWithDefault(SpectrumProperties.LiveTime, 0.0) <= 0.0)
         refSp.setNumericProperty(SpectrumProperties.LiveTime, 60.0);
      return refSpec;
   }

   private ISpectrumData preProcessSpectrum(final ISpectrumData spec) {
      return SpectrumUtils.applyZeroPeakDiscriminator(SpectrumUtils.applyEDSDetector(getDetector(), spec));
   }

   /**
    * Assign a spectrum to serve as a reference for the specific ROI.
    *
    * @param roi These roi must be a member of the ROIS returned by
    *           <code>getRegionOfInterestSet(Element ...)</code>.
    * @param ref The spectrum
    * @param comp The composition
    */
   public void addReference(final RegionOfInterest roi, final ISpectrumData ref, final Composition comp)
         throws EPQException {
      assert roi.getElementSet().size() == 1;
      final Element elm = roi.getElementSet().first();
      assert getStandardROIS(elm).contains(roi);
      assert comp != null;
      assert comp.getElementCount() > 0;
      super.addReference(roi, comp);
      mRefSpectra.put(roi, makeReference(ref));
      mReferenceScale.remove(roi);
   }

   /**
    * Assign a spectrum to serve as a reference for the specific ROI.
    *
    * @param roi These roi must be a member of the ROIS returned by
    *           <code>getRegionOfInterestSet(Element ...)</code>.
    * @param ref The spectrum
    * @param elms A non-empty set of Element
    */
   public void addReference(final RegionOfInterest roi, final ISpectrumData ref, final Set<Element> elms)
         throws EPQException {
      assert roi.getElementSet().size() == 1;
      final Element elm = roi.getElementSet().first();
      assert getStandardROIS(elm).contains(roi);
      assert elms != null;
      assert elms.size() > 0;
      super.addReference(roi, elms);
      mRefSpectra.put(roi, makeReference(ref));
      mReferenceScale.remove(roi);
   }

   public ISpectrumData getStandardSpectrum(final Element elm) {
      return mStdSpectra.get(elm);
   }

   /**
    * Calibrates the reference relative to the standard by fitting the standard
    * spectrum with the appropriate reference spectrum (and any other required
    * references) to extract the k-ratio of references relative to standard.
    *
    * @param roi
    * @return UncertainValue2
    * @throws EPQException
    */
   public UncertainValue2 getReferenceCalibration(final RegionOfInterestSet.RegionOfInterest roi)
         throws EPQException {
      assert roi.getElementSet().size() == 1;
      if(roi.getElementSet().size() != 1)
         throw new EPQException("The ROI in calibrate reference must associated with one and only one element. " + roi);
      if(!mReferenceScale.containsKey(roi)) {
         final Element elm = roi.getElementSet().first();
         final Set<RegionOfInterest> refReq = new TreeSet<RegionOfInterest>(getReferenceRequirements(elm, roi));
         final ISpectrumData refSpec = mRefSpectra.get(roi);
         final ISpectrumData stdSpec = mStdSpectra.get(elm);
         if(!SpectrumUtils.equalData(refSpec, stdSpec))
            refReq.add(roi);
         if(refReq.size() == 0) {
            final double sToN = SpectrumUtils.computeSignalToNoise(roi, getStandardSpectrum(elm));
            mReferenceScale.put(roi, new UncertainValue2(1.0, "I[std," + elm.toAbbrev() + "]", sToN > 0.0 ? 1.0 / sToN : 0.0));
         } else {
            final FilterFit ff = new FilterFit(getDetector(), getBeamEnergy());
            ff.setStripUnlikely(false);
            for(final RegionOfInterest roi2 : refReq) {
               final ISpectrumData ref = getReferenceSpectrum(roi2);
               if(ref == null)
                  throw new EPQException("A reference for " + roi2.getElementSet().first().toAbbrev()
                        + " which is required to compute the scale for the " + elm + " standard is missing.");
               assert ref != null;
               ff.addReference(roi2, ref);
            }
            final KRatioSet krs = ff.getKRatios(stdSpec);
            final UncertainValue2 tmp = krs.getKRatioU(roi.getXRayTransitionSet(elm));
            if(INCLUDE_REF_U) {
               final double uRef = tmp.doubleValue() / SpectrumUtils.computeSignalToNoise(roi, getReferenceSpectrum(roi));
               tmp.assignComponent("I[ref," + elm.toAbbrev() + "]", uRef);
            }
            tmp.renameComponent("K", "I[std," + elm.toAbbrev() + "]");
            // assert tmp.doubleValue() != 0.0 : roi.toString() + ": std=" +
            // stdSpec.toString();
            mReferenceScale.put(roi, tmp);
         }
      }
      return mReferenceScale.get(roi);
   }

   /**
    * Compute the composition using the FilterFit algorithm and the current
    * CorrectionAlgorithm. The unk spectrum is modified on exit by the
    * application of the default properties and the addition of the KRatios and
    * MicroanalyticalComposition properties.
    *
    * @param unk
    * @return Composition
    * @throws EPQException
    */
   public Result compute(final ISpectrumData unk)
         throws EPQException {
      final ISpectrumData spec = preProcessSpectrum(unk);
      final Result res = new Result(unk);
      // Fit the unknown using the reference spectra
      final Map<RegionOfInterestSet.RegionOfInterest, ISpectrumData> refs = getReferenceSpectra();
      final FilterFit ff = new FilterFit(getDetector(), getBeamEnergy());
      FilterFit.setNaiveBackground(false);
      final Set<Element> measured = getMeasuredElements();
      for(final Map.Entry<RegionOfInterestSet.RegionOfInterest, ISpectrumData> me : refs.entrySet()) {
         final RegionOfInterest roi = me.getKey();
         final Element elm = roi.getElementSet().first();
         if(measured.contains(elm) || isUnmeasuredElementRule(elm) || isStripped(elm))
            ff.addReference(roi, me.getValue());
      }
      CullingStrategy strat = mCullingStrategy;
      // Don't cull elements for which there is an explicit perferred transition
      final Set<XRayTransitionSet> user = new HashSet<XRayTransitionSet>();
      for(final StandardPacket sp : getStandards().values()) {
         final RegionOfInterest prefRoi = sp.getPreferredROI();
         if(prefRoi != null)
            user.add(prefRoi.getAllTransitions());
      }
      if(!user.isEmpty())
         strat = new FilterFit.DontCull(strat, user);
      ff.setCullingStrategy(strat);
      ff.setStripUnlikely(false);
      final KRatioSet krsAgainstRefs = ff.getKRatios(spec);
      // Compute the k-ratios wrt the standards from the k-ratios wrt
      // references
      final KRatioSet krs = new KRatioSet(), fullKrs = new KRatioSet();
      for(final Element elm : measured) {
         assert !isUnmeasuredElementRule(elm);
         assert !isStripped(elm);
         final RegionOfInterest prefRoi = getPreferredROI(elm);
         for(final RegionOfInterestSet.RegionOfInterest roi : getStandardROIS(elm)) {
            assert roi.getElementSet().first().equals(elm);
            if(refs.containsKey(roi))
               try {
                  final UncertainValue2 sc = getReferenceCalibration(roi);
                  if((sc != null) && (sc.doubleValue() > 0.0)) {
                     final XRayTransitionSet xrts = roi.getXRayTransitionSet(elm);
                     final UncertainValue2 kr = krsAgainstRefs.getRawKRatio(xrts);
                     kr.renameComponent("K", "I[unk," + elm.toAbbrev() + "]");
                     if(INCLUDE_REF_U)
                        kr.assignComponent("I[ref," + elm.toAbbrev() + "]", kr.doubleValue()
                              / SpectrumUtils.computeSignalToNoise(roi, getReferenceSpectrum(roi)));
                     final UncertainValue2 kStd = UncertainValue2.divide(kr, sc);
                     fullKrs.addKRatio(xrts, kStd);
                     if((prefRoi == null) || roi.equals(prefRoi))
                        krs.addKRatio(xrts, kStd);
                  }
               }
               catch(final Exception e) {
                  System.out.println(e.getMessage());
                  e.printStackTrace();
               }
         }
      }
      final KRatioSet withStripped = new KRatioSet();
      for(final XRayTransitionSet xrts : krsAgainstRefs.getTransitions())
         if(fullKrs.isAvailable(xrts))
            withStripped.addKRatio(xrts, fullKrs.getKRatioU(xrts));
         else
            withStripped.addKRatio(xrts, krsAgainstRefs.getKRatioU(xrts));
      unk.getProperties().setKRatioProperty(SpectrumProperties.KRatios, withStripped);
      for(final Map.Entry<Element, ExtraKRatio> me : mExtraKRatios.entrySet())
         withStripped.addKRatio(me.getValue().mXRTS, me.getValue().mKRatio);
      if(!mKRatiosOnly) {
          final KRatioSet withExtra = new KRatioSet();
         for(final Element elm : krs.getElementSet())
            if(mExtraKRatios.containsKey(elm)) {
               final ExtraKRatio xtra = mExtraKRatios.get(elm);
               withExtra.addKRatio(xtra.mXRTS, xtra.mKRatio);
               mCfKR.addExtraStandard(xtra.mXRTS, xtra.mComposition, xtra.mProperties);
            } else
               for(final XRayTransitionSet xrts2 : krs.getTransitions(elm)) {
                  withExtra.addKRatio(xrts2, krs.getKRatioU(xrts2));
                  final SpectrumProperties rsProps = mStdSpectra.get(elm).getProperties();
                  mCfKR.addStandard(xrts2, rsProps.getCompositionProperty(SpectrumProperties.StandardComposition), rsProps);
               }
         final ConductiveCoating ccu = (ConductiveCoating) unk.getProperties().getObjectWithDefault(SpectrumProperties.ConductiveCoating, null);
         mCfKR.setUnknownCoating(ccu);
         // Specify unmeasured element rules
         mCfKR.clearUnmeasuredElementRules();
         for(final UnmeasuredElementRule uer : getUnmeasuredElementRules())
            mCfKR.addUnmeasuredElementRule(uer);
         // Specify user selected transitions
         for(final Map.Entry<Element, StandardPacket> me : getStandards().entrySet()) {
            final Element elm = me.getKey();
            final StandardPacket sp = me.getValue();
            final RegionOfInterest roi = sp.getPreferredROI();
            if((roi != null) && (!mExtraKRatios.containsKey(elm)))
               mCfKR.addUserSelectedTransition(elm, roi.getXRayTransitionSet(elm));
         }
         // Initialize element-by-difference and element-by-stoiciometry
         final SpectrumProperties unkProps = unk.getProperties();
         final Composition result = mCfKR.compute(withExtra, unkProps);
         final KRatioSet optKrs = mCfKR.getQuantifiedKRatios();
         unkProps.setKRatioProperty(SpectrumProperties.OptimalKRatios, optKrs);
         result.setName(unk.toString());
         res.mComposition = result;
         res.mWarning = mCfKR.getWarningMessage();
         unkProps.setCompositionProperty(SpectrumProperties.MicroanalyticalComposition, result);
      } else
         res.mComposition = Material.Null;
      final ISpectrumData residual = ff.getResidualSpectrum(spec, withStripped.getElementSet());
      res.mResidual = SpectrumUtils.applyEDSDetector(getDetector(), residual);
      return res;
   }

   /**
    * Determines a good but not necessarily optimal ROI based on overvoltage
    * constraints primarily. This is a good place to start the quant
    * optimization process, though not necessarily the ROI that will be selected
    * based on statistical criteria.
    *
    * @param elm
    * @return RegionOfInterest
    */
   public RegionOfInterest getDefaultROI(final Element elm) {
      final RegionOfInterestSet rois = getStandardROIS(elm);
      // Find the weightiest lines for each family
      final Map<Integer, RegionOfInterest> candidates = new TreeMap<Integer, RegionOfInterest>();
      for(final RegionOfInterest roi : rois) {
         final XRayTransitionSet xrts = roi.getXRayTransitionSet(elm);
         final int fam = xrts.getWeighiestTransition().getFamily();
         if(candidates.containsKey(fam)) {
            final RegionOfInterest cand = candidates.get(fam);
            if(cand.getXRayTransitionSet(elm).getSumWeight() < xrts.getSumWeight())
               candidates.put(fam, roi);
         } else
            candidates.put(fam, roi);
      }
      RegionOfInterest best = candidates.values().iterator().next();
      if(candidates.size() == 1)
         return best;
      // Choose the highest energy one with a U>MIN_U
      final double e0 = getBeamEnergy();
      final double MIN_U = 2.0;
      double bestU = e0 / best.getXRayTransitionSet(elm).getWeighiestTransition().getEdgeEnergy();
      for(final RegionOfInterest roi : candidates.values()) {
         final double u = e0 / roi.getXRayTransitionSet(elm).getWeighiestTransition().getEdgeEnergy();
         if(bestU < MIN_U ? u > bestU : u < bestU) {
            best = roi;
            bestU = u;
         }
      }
      return best;

   }

   /**
    * Returns the references associated with the specified element
    *
    * @param elm
    * @return Set&lt;ISpectrumData&gt;
    */
   public Set<ISpectrumData> findReferences(final Element elm) {
      final Set<ISpectrumData> res = new HashSet<ISpectrumData>();
      for(final Map.Entry<RegionOfInterestSet.RegionOfInterest, ISpectrumData> me : getReferenceSpectra().entrySet())
         if(me.getKey().getElementSet().contains(elm))
            res.add(me.getValue());
      return res;
   }

   /**
    * Returns an immutable association of element to the associated standard
    * spectrum
    *
    * @return Map&lt;Element,ISpectrumData&gt;
    */
   public Map<Element, ISpectrumData> getStandardSpectra() {
      return Collections.unmodifiableMap(mStdSpectra);
   }

   /**
    * Clear (delete/remove) all standard spectra, reference spectra, unmeasured
    * element rules and unknowns.
    */
   @Override
   public void clearStandards() {
      super.clearStandards();
      mReferenceScale.clear();
   }

   /**
    * Specify the culling strategy be applied after fitting to eliminate
    * elements with close-to-zero concentrations.
    */
   public void setCullingStrategy(final CullingStrategy cs) {
      mCullingStrategy = cs;
   }

   /**
    * Which culling strategies be applied after fitting to eliminate elements
    * with close-to-zero concentrations.
    */
   public CullingStrategy getCullingStrategy() {
      return mCullingStrategy;
   }

   @Override
   public QuantifyUsingStandards clone() {
      final QuantifyUsingStandards res = new QuantifyUsingStandards(getDetector(), getBeamEnergy());
      super.copyTo(res);
      // res.mCfKR = mCfKR;
      res.mReferenceScale.putAll(mReferenceScale);
      res.mRefSpectra.putAll(mRefSpectra);
      res.mStdsAsRef.putAll(mStdsAsRef);
      res.mStdSpectra.putAll(mStdSpectra);
      res.mCullingStrategy = mCullingStrategy;
      return res;
   }

   /**
    * Tabulate quantitative results.
    *
    * @param quantifiedSpectra
    * @param parentPath
    * @param extResults
    * @return String
    */
   public String tabulateResults(final List<ISpectrumData> quantifiedSpectra, final File parentPath, final Collection<ISpectrumData> extResults) {
      final StringWriter sw = new StringWriter(4096);
      final PrintWriter pw = new PrintWriter(sw);
      final NumberFormat nf1 = new HalfUpFormat("#0.0");
      final NumberFormat nf2 = new HalfUpFormat("0.0000");
      final NumberFormat nf3 = new HalfUpFormat("#0.000");
      final NumberFormat nf4 = new HalfUpFormat("#0.0000");
      pw.println("<TABLE>");
      // Header row
      final Set<Element> elms = getUnknownElements();
      pw.print("<tr><th>Spectrum</th><th>Quantity</th>");
      for(final Element el : elms) {
         pw.print("<TH COLSPAN=3 ALIGN=CENTER>");
         pw.print(el.toAbbrev());
         pw.print("</TH>");
      }
      pw.print("<th>Sum</th>");
      pw.print("</tr>");
      boolean first = true;
      for(final ISpectrumData spec : quantifiedSpectra) {
         // Separator line between spectra
         final SpectrumProperties specProps = spec.getProperties();
         final Composition comp = specProps.getCompositionWithDefault(SpectrumProperties.MicroanalyticalComposition, null);
         if(comp == null)
            continue;
         boolean boldNorm = false;
         if(!first) {
            pw.print("<tr><td colspan = ");
            pw.print(2 + (3 * elms.size()) + 1);
            pw.print("</td></tr>");
         }
         first = false;
         final KRatioSet optKrs = specProps.getKRatioWithDefault(SpectrumProperties.OptimalKRatios, null);
         final KRatioSet measKrs = specProps.getKRatioWithDefault(SpectrumProperties.KRatios, null);
         pw.print("<tr><th rowspan = 3>");
         pw.print(specProps.asURL(spec));
         {
            final SampleShape ss = specProps.getSampleShapeWithDefault(SpectrumProperties.SampleShape, null);
            if(ss != null) {
               pw.print("<br>");
               pw.print(ss.toString());
               boldNorm = !(ss instanceof SampleShape.Bulk);
            }
         }
         pw.print("</th>");
         // Characteristic line family
         pw.print("<td>Line</td>");
         for(final Element elm : elms) {
            final XRayTransitionSet xrts = optKrs != null ? optKrs.optimalDatum(elm) : null;
            if(xrts != null) {
               pw.print("<TD COLSPAN = 3 ALIGN=CENTER>");
               pw.print(xrts);
               pw.print("</TD>");
            } else {
               pw.print("<TD COLSPAN = 3 ALIGN=CENTER>");
               final UnmeasuredElementRule uer = getUnmeasuredElementRule(elm);
               if(uer != null)
                  pw.print(uer.toString());
               pw.print("</TD>");
            }
         }
         pw.println("<TD></TD></TR>");
         // ZAF correction
         pw.print("<tr><td>Z &#183; A &#183; F</td>");
         for(final Element elm : elms) {
            final XRayTransitionSet xrts = optKrs != null ? optKrs.optimalDatum(elm) : null;
            double[] zaf = null;
            if(xrts != null)
               try {
                  zaf = zaf(comp, xrts, specProps);
               }
               catch(final EPQException e) {
               }
            if(zaf != null) {
               pw.print("<TD ALIGN=CENTER>");
               pw.print(Double.isNaN(zaf[0]) ? "-" : nf3.format(zaf[0]));
               pw.print("</TD><TD ALIGN=CENTER>");
               pw.print(Double.isNaN(zaf[1]) ? "-" : nf3.format(zaf[1]));
               pw.print("</TD><TD ALIGN=CENTER>");
               pw.print(Double.isNaN(zaf[2]) ? "-" : nf3.format(zaf[2]));
               pw.print("</TD>");
            } else
               pw.print("<TD>-</TD><TD>-</TD><TD>-</TD>");
         }
         pw.println("<TD></TD></TR>");
         // k-ratios
         pw.print("<TR><TD>k-ratios</TD>");
         for(final Element elm : elms) {
            final XRayTransitionSet xrts = optKrs != null ? optKrs.optimalDatum(elm) : null;
            if(xrts != null) {
               pw.print("<TD ALIGN=RIGHT>");
               pw.print(nf4.format(measKrs.getRawKRatio(xrts)));
               pw.print("</TD><TD align=center>\u00B1</TD><TD ALIGN=LEFT>");
               pw.print(nf4.format(measKrs.getError(xrts)));
               pw.print("</TD>");
            } else
               pw.print("<TD>-</TD><TD>-</TD><TD>-</TD>");
         }
         pw.println("<TD></TD></TR>");
         // MACs
         if(System.getProperty("user.name").equalsIgnoreCase("nritchie")) {
            pw.print("<tr><th>&nbsp;</th><td>MAC</td>");
            CorrectionAlgorithm ca = (CorrectionAlgorithm) CorrectionAlgorithm.NullCorrection.getAlgorithm(CorrectionAlgorithm.class);
            if(ca == null)
               ca = AlgorithmUser.getDefaultCorrectionAlgorithm();
            assert ca != null;
            final MassAbsorptionCoefficient macAlg = (MassAbsorptionCoefficient) ca.getAlgorithm(MassAbsorptionCoefficient.class);
            for(final Element elm : elms) {
               final XRayTransitionSet xrts = optKrs != null ? optKrs.optimalDatum(elm) : null;
               if(xrts != null)
                  try {
                     UncertainValue2 netMac = UncertainValue2.ZERO;
                     {
                        double netW = 0.0;
                        for(final XRayTransition xrt : xrts.getTransitions()) {
                           final double w = xrt.getWeight(XRayTransition.NormalizeDefault);
                           if(w > 0.01) {
                              netMac = UncertainValue2.add(netMac, UncertainValue2.multiply(w, macAlg.computeWithUncertaintyEstimate(comp, xrt)));
                              netW += w;
                           }
                        }
                        netMac = UncertainValue2.divide(netMac, netW);
                     }
                     pw.print("<TD ALIGN=RIGHT>");
                     pw.print(nf1.format(netMac.doubleValue()));
                     pw.print("</TD><TD align=center>\u00B1</TD><TD ALIGN=LEFT>");
                     pw.print(nf4.format(netMac.uncertainty()));
                     pw.print("</TD>");
                  }
                  catch(final EPQException e) {
                     pw.print("<TD>-</TD><TD>-</TD><TD>-</TD>");
                  }
               else
                  pw.print("<TD>-</TD><TD>-</TD><TD>-</TD>");
            }
            pw.println("<TD></TD></TR>");
         }
         // wgt%
         pw.println("<TR>");
         final double dh = specProps.getNumericWithDefault(SpectrumProperties.DuaneHunt, Double.NaN);
         pw.print("<td>D-H = " + (Double.isNaN(dh) ? "?" : nf3.format(dh)) + " keV</td>");
         pw.print("<td>mass fraction</td>");
         for(final Element elm : elms) {
            pw.print(((!boldNorm) ? "<TH" : "<TD") + " ALIGN=RIGHT>");
            final UncertainValue2 wf = comp.weightFractionU(elm, false);
            pw.print(nf2.format(wf.doubleValue()));
            pw.print((!boldNorm) ? "</TH>" : "</TD>");
            pw.print("<TD align=center>\u00B1</TD><TD ALIGN=LEFT>");
            boolean first2 = true;
            for(final String name : wf.getComponentNames()) {
               if(!first2)
                  pw.print("<br/>");
               pw.print("<nobr>");
               pw.print(wf.formatComponent(name, 5));
               pw.print("</nobr>");
               first2 = false;
            }
            pw.print("<br/>[" + nf2.format(wf.uncertainty()) + "]");
            pw.print("</TD>");
         }
         pw.print("<TD>");
         pw.print(nf2.format(comp.sumWeightFraction()));
         pw.print("</TD>");
         pw.println("</TR>");

         // norm(wgt%)
         pw.print("<TR>");
         pw.print("<TD align=\"right\">I = " + nf3.format(SpectrumUtils.getAverageFaradayCurrent(specProps, Double.NaN))
               + " nA</TD>");
         pw.print("<td>norm(mass<br/>fraction)</td>");
         for(final Element elm : elms) {
            final UncertainValue2 nwf = comp.weightFractionU(elm, true);
            pw.print((boldNorm ? "<TH" : "<TD") + " ALIGN=RIGHT>");
            pw.print(nf2.format(nwf.doubleValue()));
            pw.print(boldNorm ? "</TH>" : "</TD>");
            pw.print("<TD align=center>\u00B1</TD><TD ALIGN=LEFT>");
            if(nwf.uncertainty() > 0.0)
               pw.print(nf2.format(nwf.uncertainty()));
            pw.print("</TD>");
         }
         pw.print("<TD> - </TD>");
         pw.println("</TR>");
         // atomic %
         pw.print("<TR>");
         pw.print("<TD align=\"right\">LT = "
               + nf1.format(specProps.getNumericWithDefault(SpectrumProperties.LiveTime, Double.NaN)) + " s</TD>");
         pw.print("<td>atomic<br/>fraction</td>");
         for(final Element elm : elms) {
            final UncertainValue2 ap = comp.atomicPercentU(elm);
            pw.print((boldNorm ? "<TH" : "<TD") + " ALIGN=RIGHT>");
            pw.print(nf2.format(ap.doubleValue()));
            pw.print(boldNorm ? "</TH>" : "</TD>");
            pw.print("<TD align=center>\u00B1</TD><TD ALIGN=LEFT>");
            if(ap.uncertainty() > 0.0)
               pw.print(nf2.format(ap.uncertainty()));
            pw.print("</TD>");
         }
         pw.println("<TD></TD></TR>");
         {
            final ConductiveCoating cc = (ConductiveCoating) specProps.getObjectWithDefault(SpectrumProperties.ConductiveCoating, null);
            pw.print("<TR>");
            pw.print("<TD align=\"right\">Conductive coating</TD>");
            pw.print("<TD COLSPAN=" + Integer.toString(2 + (3 * elms.size())) + " ALIGN=LEFT>");
            pw.print(cc != null ? cc.toString() : "None");
            pw.print("</TD>");
         }
         if(extResults != null) {
            ISpectrumData res = null;
            for(final ISpectrumData rs : extResults)
               if(rs instanceof DerivedSpectrum)
                  if(rs.toString().equals("Residual[" + spec.toString() + "]")) {
                     res = rs;
                     break;
                  }
            if(res != null) {
               pw.print("<TR>");
               pw.print("<TD align=\"right\">Residual</TD>");
               pw.print("<TD COLSPAN=" + Integer.toString(2 + (3 * elms.size())) + " ALIGN=LEFT>");
               try {
                  final File f = File.createTempFile("residual", ".msa", parentPath);
                  try (final FileOutputStream fos = new FileOutputStream(f)) {
                     WriteSpectrumAsEMSA1_0.write(res, fos, WriteSpectrumAsEMSA1_0.Mode.COMPATIBLE);
                  }
                  pw.print("<A HREF=\"");
                  pw.print(f.toURI().toURL().toExternalForm());
                  pw.print("\">");
                  pw.print(f.toString());
                  pw.print("</A>");
               }
               catch(final Exception e) {
                  pw.print("Error writing the residual");
               }
               pw.println("</TD>");
               pw.println("</TR>");
            }
         }
      }
      pw.print("<TR>");
      pw.print("<TD align=\"right\">Notes</TD>");
      pw.print("<TD COLSPAN=" + Integer.toString(2 + (3 * elms.size())) + " ALIGN=LEFT>");
      pw.print("Uncertainties are 1 &sigma; and labeled by source. (I[std|unk]: Count statistics[standard|unknown], [&mu;/&rho]: Absorption correction, &eta;: Backscatter correction, []: combined)");
      pw.println("</TD>");
      pw.println("</TR>");
      pw.println("</TABLE>");
      final UnmeasuredElementRule uer = getUnmeasuredElementRule(Element.O);
      if(uer instanceof OxygenByStoichiometry) {
         pw.print("<H3>Quantitative Results Expressed As Oxide Fractions</H3>");
         final List<Composition> comps = new ArrayList<Composition>();
         for(final ISpectrumData spec : quantifiedSpectra) {
            final Composition comp = spec.getProperties().getCompositionWithDefault(SpectrumProperties.MicroanalyticalComposition, null);
            if(comp != null)
               comps.add(comp);
         }
         final OxygenByStoichiometry obs = (OxygenByStoichiometry) uer;
         pw.println(obs.getOxidizer().toHTMLTable2(comps, nf4));
      }
      return sw.toString();
   }

   /**
    * Keeps track of which spectra have already been written into files and the
    * link associated with them.
    */
   private final List<Pair<ISpectrumData, String>> mWrittenSpectra = new ArrayList<>();

   private String writeSpectrum(final File path, final ISpectrumData spec, final String label) {
      // Check it the spectrum has already been written.
      for(final Pair<ISpectrumData, String> pr : mWrittenSpectra)
         if(pr.first == spec)
            return pr.second;
      final StringBuffer sb = new StringBuffer();
      boolean written = false;
      try {
         final File fn = File.createTempFile(label, ".msa", path);
         try (final FileOutputStream fos = new FileOutputStream(fn)) {
            WriteSpectrumAsEMSA1_0.write(spec, fos, WriteSpectrumAsEMSA1_0.Mode.COMPATIBLE);
            sb.append("<A HREF=\"");
            sb.append(fn.toURI().toURL().toExternalForm());
            sb.append("\">");
            sb.append(spec.toString());
            sb.append("</A>");
            written = true;
         }
      }
      catch(final Exception e) {
         // Ignore it...
      }
      if(!written)
         sb.append(spec.toString());
      mWrittenSpectra.add(new Pair<>(spec, sb.toString()));
      return sb.toString();
   }

   @Override
   public String toHTML(final File path) {
      final StringWriter sw = new StringWriter();
      final PrintWriter pw = new PrintWriter(sw);
      mWrittenSpectra.clear();
      final NumberFormat nf1 = new HalfUpFormat("0.0");
      {
         pw.println("<h3>Conditions</h3>");
         pw.println("<table>");
         pw.println("<tr><th>Item</th><th>Value</th></tr>");
         pw.print("<tr><td>Instrument</td><td>");
         pw.print(getDetector().getOwner().toString());
         pw.println("</td></tr>");
         pw.print("<tr><td>Detector</td><td>");
         pw.print(getDetector().getName());
         pw.println("</td></tr>");
         pw.print("<tr><td>Beam Energy</td><td>");
         pw.print(nf1.format(FromSI.keV(getBeamEnergy())));
         pw.println(" keV</td></tr>");
         pw.print("<tr><td>Correction Algorithm</td><td>");
         final AlgorithmClass ca = mCfKR.getCorrectionAlgorithm();
         pw.print(ca.getName());
         pw.println("</td></tr>");
         pw.print("<tr><td>Mass Absorption<br>Coefficient</td><td>");
         final AlgorithmClass mac = ca.getAlgorithm(MassAbsorptionCoefficient.class);
         pw.print(mac.getName());
         pw.println("</td></tr>");
         final Set<Element> stripped = getStrippedElements();
         if(stripped.size() > 0) {
            pw.print("<tr><td>Stripped elements</td><td>");
            boolean first = true;
            for(final Element elm : stripped) {
               if(!first)
                  pw.print(", ");
               pw.print(elm.toAbbrev());
               first = false;
            }
            pw.println("</td></tr>");
         }
         pw.println("</table></p>");
      }
      {
         pw.println("<h3>Standards</h3>");
         pw.println("<table>");
         pw.println("<tr><th>Element</th><th>Material</th><th>Req. References</th><th>Preferred ROI</th><th>Beam Energy</th><th>Spectrum</th></tr>");
         for(final Map.Entry<Element, StandardPacket> me : getStandards().entrySet()) {
            final StandardPacket sp = me.getValue();
            final Composition comp = sp.getComposition();
            pw.print("<tr><td>");
            final Element elm = me.getKey();
            pw.print(elm.toAbbrev());
            pw.print("</td><td>");
            pw.print(comp.toHTMLTable());
            pw.println("</td><td>");
            boolean firstRoi = true;
            for(final RegionOfInterest roi : sp.getElementROIS()) {
               final Set<RegionOfInterest> rr = sp.getRequiredReferences(roi);
               if(!firstRoi)
                  pw.print("<br/>");
               pw.print(roi);
               pw.print(":");
               if(rr.size() > 0)
                  for(final RegionOfInterest req : rr) {
                     pw.print("<br/>&nbsp;&nbsp;&nbsp;");
                     pw.print(req);
                  }
               else
                  pw.println("<br/>&nbsp;&nbsp;&nbsp;None");
               firstRoi = false;
            }
            pw.print("</td><td>");
            if(sp.getPreferredROI() != null)
               pw.print(sp.getPreferredROI());
            else if(sp.getElementROIS().size() == 1)
               pw.print("N/A");
            else
               pw.print("--None specified--");
            pw.println("</td><td>");
            pw.println(nf1.format(FromSI.keV(sp.getBeamEnergy())));
            pw.println(" keV</td><td>");
            try {
               final NumberFormat nf3 = new DecimalFormat("0.000");
               final ISpectrumData spec = mStdSpectra.get(me.getKey());
               final SpectrumProperties props = spec.getProperties();
               pw.print(writeSpectrum(path, spec, me.getKey().toAbbrev() + " std"));
               pw.println("<br/>I<sub>probe</sub> = ");
               pw.println(nf3.format(SpectrumUtils.getAverageFaradayCurrent(props, Double.NaN)));
               pw.println("<br/>Live time = ");
               pw.println(nf1.format(props.getNumericWithDefault(SpectrumProperties.LiveTime, Double.NaN)));
               pw.println("<br/>Duane-Hunt = ");
               pw.println(nf3.format(props.getNumericWithDefault(SpectrumProperties.DuaneHunt, Double.NaN)));
               pw.println("&nbsp;keV");
               final ConductiveCoating cc = (ConductiveCoating) props.getObjectWithDefault(SpectrumProperties.ConductiveCoating, null);
               pw.println("<br/>Conductive coating = ");
               pw.println(cc != null ? "<br/>" + cc.toString() : "None");
            }
            catch(final Throwable e) {
               pw.print("<font color=red>ERROR:</font> N/A");
            }
            pw.print("</td></tr>");

         }
         pw.print("</table>");
      }
      {
         final Set<RegionOfInterest> reqRefs = getROISWithAssignedReferences();
         if(reqRefs.size() > 0) {
            pw.println("<h3>References</h3>");
            pw.println("<table>");
            pw.print("<tr><th>Element/Lines</th><th>Material</th><th>Spectrum</th><th>Use</th></tr>");
            for(final RegionOfInterest reqRef : reqRefs) {
               pw.print("<tr><td>");
               pw.print(reqRef);
               pw.print("</td><td>");
               final ReferenceMaterial rm = getReference(reqRef);
               if(rm != null) {
                  if(rm.compositionIsAvailable())
                     pw.print(rm.getComposition().toHTMLTable());
                  else
                     pw.print(rm.toString());
               } else
                  pw.print("--Missing--");
               pw.print("<td>");
               pw.print(writeSpectrum(path, this.getReferenceSpectrum(reqRef), reqRef.shortName() + " ref"));
               pw.print("</td><td>");
               final Element elm = reqRef.getElementSet().first();
               if(getStrippedElements().contains(elm))
                  pw.print("Strip");
               else if(isUnmeasuredElementRule(elm))
                  pw.print("Fit but<br/>not measured.");
               else
                  pw.print("Reference");
               pw.println("</td></tr>");
            }
            pw.print("</table>");
         } else
            pw.print("<p>No reference spectra required.</p>");
      }
      if(mExtraKRatios.size() > 0) {
         final NumberFormat nf4 = new HalfUpFormat("0.00000");
         pw.println("<h3>Elements by User-Specified K-ratio</h>");
         pw.println("<p>The elements listed here are quantified using the specified k-ratios rather than fit k-ratios.</p>");
         pw.println("<table>");
         pw.println("<tr><th>Element</th><th>Material</th><th>X-Ray Transitions</th><th>Beam Energy</th><th>K-ratio</th></tr>");
         for(final Map.Entry<Element, ExtraKRatio> me : mExtraKRatios.entrySet()) {
            final Element elm = me.getKey();
            final ExtraKRatio extra = me.getValue();
            pw.print("<tr><th>");
            pw.print(elm.toString());
            pw.print("</th><th>");
            pw.print(extra.mComposition.toHTMLTable());
            pw.print("</th><th>");
            pw.print(extra.mXRTS.toString());
            pw.print("</th><th>");
            pw.print(nf1.format(extra.mProperties.getNumericWithDefault(SpectrumProperties.BeamEnergy, Double.NaN)));
            pw.print(" keV</th><th>");
            pw.print(UncertainValue2.format(nf4, extra.mKRatio));
            pw.println("</th></tr>");
         }
         pw.println("</table>");
      }
      final List<UnmeasuredElementRule> luer = getUnmeasuredElementRules();
      if(luer.size() > 0) {
         pw.append("<h3>Other elements</h3>\n");
         for(final UnmeasuredElementRule uer : luer) {
            pw.append("<li>");
            pw.append(uer.toHTML());
            pw.append("</li>");
         }
      }
      return sw.toString();
   }
}
