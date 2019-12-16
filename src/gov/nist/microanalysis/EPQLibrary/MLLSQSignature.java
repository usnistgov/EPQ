package gov.nist.microanalysis.EPQLibrary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
public class MLLSQSignature {

	private double mThreshold = 3.29;
	private final TreeMap<Element, ISpectrumData> mStandards;
	private TreeMap<XRayTransitionSet, Double> mOptimal;
	private final EDSDetector mDetector;
	private boolean mZafCorrectRefs = true;
	private boolean mStoreResiduals = true;
	private final TreeSet<Element> mExclude = new TreeSet<Element>();

	private final double mBeamEnergy; // in
										// Joules
	private Set<Element> mStrip = new TreeSet<Element>();
	public static Set<Element> DEFAULT_STRIP = defaultStrip();

	private double mFitQuality = 0.0;
	private double mChiSquared = 0.0;
	private double mCounts = Double.NaN;
	private ISpectrumData mResidual;
	private static final double K_TO_L = 1.66666;
	private static final double L_TO_M = 2.00000;

	private boolean mStripUnlikely;
	private FilterFit mFilterFit;

	private static Set<Element> defaultStrip() {
		final Set<Element> res = new TreeSet<Element>();
		res.add(Element.C);
		res.add(Element.O);
		return res;
	}
	
	/**
	 * Constructs a MLLSQSignature object to process spectra from the specified
	 * detector at the specified beam energy.
	 * 
	 * @param detector An ElectronProbe.EDSDetector object representing the
	 *                 detector/instrument on which these spectra were collected.
	 * @param e0       The beam energy in Joules
	 */
	public MLLSQSignature(EDSDetector detector, double e0) {
		mStandards = new TreeMap<Element, ISpectrumData>();
		mOptimal = null;
		mDetector = detector;
		mBeamEnergy = e0;
		mStrip = defaultStrip();
		mStripUnlikely = true;
	}

	@Override
	public MLLSQSignature clone() {
		final MLLSQSignature res = new MLLSQSignature(mDetector, mBeamEnergy);
		res.mStandards.putAll(mStandards);
		if (mOptimal != null)
			res.mOptimal = new TreeMap<XRayTransitionSet, Double>(mOptimal);
		res.mZafCorrectRefs = mZafCorrectRefs;
		res.mExclude.addAll(mExclude);
		res.mStrip = new TreeSet<Element>(mStrip);
		res.mFitQuality = mFitQuality;
		res.mChiSquared = mChiSquared;
		res.mCounts = mCounts;
		res.mResidual = mResidual;
		res.mStripUnlikely = mStripUnlikely;
		res.mThreshold = mThreshold;
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
		if (!sp.isDefined(SpectrumProperties.FaradayBegin))
			sp.setNumericProperty(SpectrumProperties.FaradayBegin, 1.0);
		if (!sp.isDefined(SpectrumProperties.LiveTime))
			sp.setNumericProperty(SpectrumProperties.LiveTime, 60.0);
		assert Math.abs(FromSI.eV(mBeamEnergy) - SpectrumUtils.getBeamEnergy(ref))
				/ FromSI.eV(mBeamEnergy) < 0.01 : "Beam energies do not match...";
		mStandards.put(elm, ref);
		mOptimal = null;
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
			// Special rules for one element masquerading as another
			final FilterFit.CompoundCullingStrategy cs = new FilterFit.CompoundCullingStrategy();
			// Special rules for one element masquerading as another
			final FilterFit.SpecialCulling sc = new FilterFit.SpecialCulling();
			if (!mStrip.contains(Element.Fe))
				sc.add(Element.F, Element.Fe);
			if (!mStrip.contains(Element.Th))
				sc.add(Element.Ag, Element.Th);
			if (!mStrip.contains(Element.Sr))
				sc.add(Element.Si, Element.Sr);
			if (!mStrip.contains(Element.Si)) {
				sc.add(Element.Sr, Element.Si);
				sc.add(Element.W, Element.Si);
			}
			if (!mStrip.contains(Element.Ca))
				sc.add(Element.Sb, Element.Ca);
			cs.append(sc);
			cs.append(new FilterFit.CullByAverageUncertainty(mThreshold, 0.6 * mThreshold));
			// cs.append(new FilterFit.CullByBrightest(mThreshold));
			mFilterFit.setCullingStrategy(cs);
			for (Map.Entry<Element, ISpectrumData> me : mStandards.entrySet())
				mFilterFit.addReference(me.getKey(), me.getValue());
		}
		mFilterFit.forceZero(mExclude);
		final KRatioSet res = mFilterFit.getKRatios(spec);
		mFitQuality = mFilterFit.getFitMetric(spec);
		mChiSquared = mFilterFit.chiSquared();
		mResidual = mFilterFit.getResidualSpectrum(spec);
		mCounts = mFilterFit.getFitEventCount(spec, mStrip);
		if (mOptimal == null)
			synchronized (this) {
				if (mOptimal == null) {
					mOptimal = new TreeMap<XRayTransitionSet, Double>();
					// Figure out which lines to use
					for (final Map.Entry<Element, ISpectrumData> me : mStandards.entrySet()) {
						final ISpectrumData ref = me.getValue();
						SpectrumUtils.applyZeroPeakDiscriminator(ref, SpectrumUtils.channelForEnergy(ref, ToSI.eV(100.0)));
						final Element elm = me.getKey();
						RegionOfInterestSet.RegionOfInterest bestRoi = null;
						int best = XRayTransition.None;
						for (final FilteredSpectrum fs : mFilterFit.getFilteredSpectra(elm)) {
							final RegionOfInterestSet.RegionOfInterest roi = fs.getRegionOfInterest();
							final XRayTransitionSet xrts = fs.getXRayTransitionSet();
							if (xrts.contains(XRayTransition.KA1)) {
								// Always take K if it is below the overvoltage
								// threshold
								if (K_TO_L * XRayTransition.getEnergy(elm, XRayTransition.KA1) < mBeamEnergy) {
									bestRoi = roi;
									best = XRayTransition.KA1;
								}
							} else if (xrts.contains(XRayTransition.LA1)) {
								// Take L if K not available and L below
								// overvoltage
								// threshold
								if (best != XRayTransition.KA1)
									if (L_TO_M * XRayTransition.getEnergy(elm, XRayTransition.LA1) < mBeamEnergy) {
										bestRoi = roi;
										best = XRayTransition.LA1;
									}
							} else if (xrts.contains(XRayTransition.MA1))
								// Take M if K or L not available
								if ((best != XRayTransition.KA1) && (best != XRayTransition.LA1)) {
									bestRoi = roi;
									best = XRayTransition.MA1;
								}
						}
						if (bestRoi != null) {
							final XRayTransitionSet xrts = bestRoi.getXRayTransitionSet(elm);
							final XRayTransition xrt = xrts.find(best);
							assert xrt != null;
							final CorrectionAlgorithm ca = new XPP1991();
							/**
							 * Iref -> Measured intensity of reference Ipure = Iref/(wRef*ZAFref) wUnk ~
							 * Iunk / Ipure = Iunk/(Iref/(wRef*ZAFref)) = (Iunk/Iref)*wRef*ZAFref
							 */
							final SpectrumProperties refProps = ref.getProperties();
							final Composition comp = refProps
									.getCompositionProperty(SpectrumProperties.StandardComposition);
							final double zaf = mZafCorrectRefs ? ca.relativeZAF(comp, xrt, refProps)[3] : 1.0;
							System.out.println(comp+"("+xrt+")="+zaf);
							mOptimal.put(xrts, Double.valueOf(comp.weightFraction(elm, true) * zaf));
						}
					}
				}
			}
		return res;
	}

	/**
	 * Trims the k-ratio set from <code>compute</code> to a sub-set containing one
	 * line per element. The line is selected such that higher energy lines are
	 * favored over lower energy lines since presumably the absorption will be less
	 * of an issue.
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
	 * normalized to 1.0. If oxygen is included in the stripping set then oxygen is
	 * handled slightly differently. Oxygen is added back in after all the other
	 * element values have been computed and the result renormalized. Then the
	 * resulting value for oxygen is included in the signature. Thus all elements in
	 * the signature except O will normalize to 1.0 but there will be a value for O
	 * which suggests its abundance.
	 * 
	 * @param krs
	 * @return KRatioSet
	 */
	public ParticleSignature signature(KRatioSet krs) {
		final KRatioSet optimal = optimalKRatioSet(krs);
		final HashSet<Element> strip = new HashSet<>(mStrip);
		strip.remove(Element.C);
		final ParticleSignature res = new ParticleSignature(mStrip.contains(Element.C) ? Collections.singleton(Element.C) : Collections.emptySet(), strip);
		for (final XRayTransitionSet xrts : optimal.getTransitions())
			res.add(xrts.getElement(),
					UncertainValue2.multiply(mOptimal.get(xrts).doubleValue(), optimal.getKRatioU(xrts)));
		return res;
	}

	/**
	 * Returns the value of fit quality calculated as a result of the last call to
	 * <code>compute</code>
	 * 
	 * @return 1.0 for perfect fit, 0.0 for perfectly lousy
	 */
	public double getFitQuality() {
		return mFitQuality;
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
		mStrip.addAll(col);
	}
	
	/**
	 * Is an element in the list of elements which will be stripped (ie. ignored in
	 * the signature)
	 * 
	 * @param elm
	 * @return boolean
	 */
	public boolean isStripped(Element elm) {
		return mStrip.contains(elm);
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
	 * Determines whether the references ZAF corrected in addition to corrected for
	 * composition?
	 * 
	 * @param zafCorrectRefs true to ZAF correct, false otherwise
	 */
	public void setZafCorrectRefs(boolean zafCorrectRefs) {
		if (mZafCorrectRefs != zafCorrectRefs) {
			mZafCorrectRefs = zafCorrectRefs;
			mOptimal = null;
		}
	}

	/**
	 * Returns the residual spectrum computed in the last iteration of compute(...)
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
	 * Specify a set of elements to exclude from the signature fit. Even if a
	 * reference exists for this element it will not be fit.
	 * 
	 * @param exclude
	 */
	public void setExclusionSet(Collection<Element> exclude) {
		mExclude.clear();
		if (exclude != null)
			mExclude.addAll(exclude);
	}

	/**
	 * Clear the exclusion set so that all elements for which there are references
	 * is fit.
	 */
	public void clearExclusionSet() {
		mExclude.clear();
	}

	/**
	 * Returns a set of elements which are excluded from the signature fit. Even if
	 * a reference exists for this element it will not be fit.
	 * 
	 * @return TreeSet&gt;Element&lt;
	 */
	public Set<Element> getExclusionSet() {
		return Collections.unmodifiableSet(mExclude);
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
	 * @param stripUnlikely The value to which to set stripUnlikely.
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
