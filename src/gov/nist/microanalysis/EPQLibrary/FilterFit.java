package gov.nist.microanalysis.EPQLibrary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.Utility.Interval;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * Specializes the LinearLeastSquares algorithm for filter fitting EDS spectra.
 * Computes dose-corrected k-ratios for the unknown spectrum relative to the
 * reference spectra.
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
public class FilterFit extends LinearSpectrumFit {

   /**
    * Declares an interface for specifying a mechanism to determine which
    * elements should be remove from the fit based on insufficient evidence.
    */
   public interface CullingStrategy extends Cloneable {
      /**
       * Takes fit parameters determines whether based on these an element is
       * present. If the element is not likely to be present it is added to the
       * Set&lt;Element&gt; that is returned. Return an empty set for none.
       *
       * @param fitParams
       *           UncertainValue[]
       * @return Set&lt;Element&gt;
       */
      Set<Element> compute(FilterFit ff, UncertainValue2[] fitParams);

      public CullingStrategy clone();

   }

   // Don't consider any edges about maxU*beamEnergy
   private FilteredSpectrum mFilteredUnknown;
   private final FittingFilter mFilter;
   private CullingStrategy mCullingStrategy = null;

   // Determines the widths of ROIsets
   private boolean mStripUnlikely = true;
   private boolean mDirty = true;

   private void markDirty() {
      reevaluateAll();
      mDirty = true;
   }

   // Determines the background is modeled (eV)
   private double mResidualModelThreshold = 2.5e3;
   private static boolean mNaive = true;

   // The ArrayList ensures a consistent mapping between fit function and
   // associated data
   private final ArrayList<FilteredPacket> mFilteredPackets = new ArrayList<FilteredPacket>();
   // Temporary storage for the reference data while the fit is being
   // performed...
   private double[][] mTmpFitData;

   private final Set<Element> mExplicitlyZero = new TreeSet<Element>();

   public void forceZero(Collection<Element> ex) {
      mExplicitlyZero.clear();
      mExplicitlyZero.addAll(ex);
   }

   // A packet of data associated with each filtered spectrum.
   private static class FilteredPacket {
      final private FilteredSpectrum mFiltered;
      private DerivedSpectrum mRoiSpectrum;
      private double mIntegral = Double.NaN;
      private UncertainValue2 mKRatio;

      private FilteredPacket(final FilteredSpectrum filtered) {
         mFiltered = filtered;
         mKRatio = null;
      }

      @Override
      public String toString() {
         return "Filtered[" + mFiltered.getBaseSpectrum().toString() + "," + mFiltered.getRegionOfInterest() + "]";
      }

      public ISpectrumData getROISpectrum(final double modelThresh) {
         if (mRoiSpectrum == null) {
            if (mNaive)
               mRoiSpectrum = new ROISpectrumNaive(mFiltered.getBaseSpectrum(), mFiltered.getRegionOfInterest(), modelThresh);
            else
               mRoiSpectrum = new ROISpectrum(mFiltered.getBaseSpectrum(), mFiltered.getRegionOfInterest(), modelThresh);
         }
         return mRoiSpectrum;
      }

      /**
       * Gets the associated FilteredSpectrum (unique identifier)
       *
       * @return Returns the filtered.
       */
      private FilteredSpectrum getFiltered() {
         return mFiltered;
      }

      private Element getElement() {
         return getXRayTransitionSet().getElement();
      }

      private XRayTransitionSet getXRayTransitionSet() {
         return mFiltered.getXRayTransitionSet();
      }

      /**
       * Gets the current value assigned to kRatio
       *
       * @return Returns the kRatio.
       */
      private UncertainValue2 getKRatio() {
         return mKRatio;
      }

      /**
       * Returns the number of x-ray events in the peak corresponding to this
       * filtered spectrum
       *
       * @return double
       */
      private double getCounts() {
         if (Double.isNaN(mIntegral)) {
            mIntegral = 0.0;
            final RegionOfInterest roi = mFiltered.getRegionOfInterest();
            mIntegral = SpectrumUtils.backgroundCorrectedIntegral(mFiltered.getBaseSpectrum(), FromSI.eV(roi.lowEnergy()),
                  FromSI.eV(roi.highEnergy()))[0];
         }
         return mIntegral;
      }
   }

   /**
    * FilterFit - Create an object to perform a filter fit on the argument
    * spectrum.
    *
    * @param det
    *           An EDSDetector implementation
    * @param beamEnergy
    *           In Joules
    */
   public FilterFit(final EDSDetector det, final double beamEnergy) throws EPQException {
      super(det, beamEnergy);
      final double filterWidth = det.getDetectorLineshapeModel().getFWHMatMnKa();
      mFilter = new FittingFilter.TopHatFilter(filterWidth, det.getChannelWidth());
      assert (beamEnergy > ToSI.keV(1.0)) && (beamEnergy < ToSI.keV(450.0)) : Double.toString(FromSI.keV(beamEnergy));
   }

   /**
    * Returns the elements for which there are currently standards in the fit.
    *
    * @return Set&lt;Element&gt;
    */
   @Override
   public Set<Element> getElements() {
      final TreeSet<Element> res = new TreeSet<Element>();
      for (final FilteredPacket fp : mFilteredPackets)
         res.add(fp.mFiltered.getElement());
      return res;
   }

   private static double[] checkNotNaN(final double[] data) {
      for (int i = 0; i < data.length; ++i)
         if (Double.isNaN(data[i]))
            data[i] = i > 0 ? data[i - 1] : 0.0;
      return data;
   }

   private static double[] checkNotNaNZero(final double[] data) {
      for (int i = 0; i < data.length; ++i)
         if (Double.isNaN(data[i]) || (data[i] < 1.0e-100))
            data[i] = i > 0 ? data[i - 1] : Double.MAX_VALUE;
      return data;

   }

   /**
    * perform - This overrides the base implementation in LinearLeastSquares to
    * ensure that the spectra are filtered before the fit is performed and to
    * perform culling after the fit to remove elements for which the spectrum
    * does not provide evidence.
    */
   /**
    * @throws EPQException
    * @see gov.nist.microanalysis.Utility.LinearLeastSquares#perform()
    */
   @Override
   public void perform() throws EPQException {
      if (mDirty) {
         // Update data
         assert mFilteredUnknown != null;
         // Update references
         clearZeroedCoefficients();
         final Set<Element> removeElms = new TreeSet<Element>(mExplicitlyZero), removedElms = new TreeSet<Element>();
         // if(removeElms.size()>0)
         // System.out.println(removeElms.toString());
         final ISpectrumData unk = mFilteredUnknown.getBaseSpectrum();
         if (mStripUnlikely) {
            final Set<Element> likely = PeakROISearch.PeakStrippingSearch.likelyElements(unk, mBeamEnergy, 3.0);
            for (final FilteredPacket raf : mFilteredPackets) {
               final Element elm = raf.getElement();
               if (!likely.contains(elm))
                  removeElms.add(elm);
            }
         }
         final UncertainValue2 zero = new UncertainValue2(0.0, "K", 0.0);
         for (final FilteredPacket raf : mFilteredPackets)
            raf.mKRatio = zero;
         boolean repeat = true;
         while (repeat) {
            repeat = false;
            // Determine intervals to fit...
            SortedSet<Interval> intervals = new TreeSet<Interval>();
            removedElms.addAll(removeElms);
            for (int i = 0; i < mFilteredPackets.size(); ++i) {
               final FilteredPacket raf = mFilteredPackets.get(i);
               if (removedElms.contains(raf.getElement()))
                  zeroFitCoefficient(i, true);
               if (!isZeroFitCoefficient(i))
                  intervals = Interval.add(intervals, raf.getFiltered().getNonZeroInterval());
            }
            removeElms.clear();
            Interval.validate(intervals);
            // Get the spectrum data on the fit intervals
            final double[] y = checkNotNaN(Interval.extract(mFilteredUnknown.getFilteredData(), intervals));
            final double[] errs = checkNotNaNZero(Interval.extract(mFilteredUnknown.getErrors(), intervals));
            final double[] x = new double[y.length];
            for (int i = 0; i < x.length; ++i)
               x[i] = i;
            setData(x, y, errs);
            // Set up references on fit intervals
            mTmpFitData = new double[mFilteredPackets.size()][];
            for (int i = 0; i < mTmpFitData.length; ++i) {
               final FilteredSpectrum fs = mFilteredPackets.get(i).getFiltered();
               mTmpFitData[i] = checkNotNaN(Interval.extract(fs.getFilteredData(), intervals));
               assert mTmpFitData[i].length == x.length;
            }
            // super.perform();
            final UncertainValue2[] fitParams = getResults();
            // See Schamber in
            // "X-Ray Fluorescence Analysis of Environmental Samples" edited
            // by Thomas Dzubay
            final double vcf = mFilter.varianceCorrectionFactor();
            for (int j = 0; j < mFilteredPackets.size(); ++j) {
               if (!isZeroFitCoefficient(j)) {
                  final UncertainValue2 uv = fitParams[j];
                  final FilteredPacket fpj = mFilteredPackets.get(j);
                  fpj.mKRatio = new UncertainValue2(uv.doubleValue(), "K", Math.sqrt(vcf * uv.variance()));
                  if (uv.doubleValue() < 0.0) {
                     zeroFitCoefficient(j, true);
                     repeat = true;
                  }
               }
            }
            if (mCullingStrategy != null) {
               final Set<Element> cullThese = mCullingStrategy.compute(this, fitParams);
               removeElms.addAll(cullThese);
            }
            for (final FilteredPacket fp : mFilteredPackets)
               if (removeElms.contains(fp.getElement()) || removedElms.contains(fp.getElement()))
                  fp.mKRatio = new UncertainValue2(0.0, fp.mKRatio.getComponents());
            // Ensure all k-ratios are non-negative.
            for (final FilteredPacket fp : mFilteredPackets)
               fp.mKRatio = UncertainValue2.nonNegative(fp.mKRatio);
            repeat |= !removedElms.containsAll(removeElms);
            if (getNonZeroedCoefficientCount() == 0) {
               // System.out.println("All elements have been removed from the
               // fit.");
               break;
            }
         }
         mDirty = false;
      }
   }

   private ArrayList<FilteredSpectrum> filterReference(final XRayTransitionSet xrts, final ISpectrumData ref) throws EPQException {
      final RegionOfInterestSet rois = LinearSpectrumFit.createEmptyROI(mDetector);
      rois.add(xrts);
      final Element elm = xrts.getElement();
      return filterReference(ref, rois, elm);

   }

   private ArrayList<FilteredSpectrum> filterReference(final ISpectrumData ref, final RegionOfInterestSet rois, final Element elm)
         throws EPQException {
      final ArrayList<FilteredSpectrum> res = new ArrayList<FilteredSpectrum>();
      for (final RegionOfInterestSet.RegionOfInterest roi : rois) {
         final double[] bci = SpectrumUtils.backgroundCorrectedIntegral(ref, FromSI.eV(roi.lowEnergy()), FromSI.eV(roi.highEnergy()));
         if ((bci[0] / bci[1]) > 3.0)
            res.add(new FilteredSpectrum(ref, elm, roi, mFilter));
      }
      return res;
   }

   /**
    * Add a reference spectrum associated with all visible lines associated with
    * the specified element.
    *
    * @param ref
    *           A reference spectrum
    * @throws EPQException
    *            - If the reference is not compatible with the unknown
    */
   public void addReference(final Element elm, final ISpectrumData ref) throws EPQException {
      mDetector.checkSpectrumScale(ref, SCALE_TOLERANCE);
      removeReference(elm);
      final Collection<FilteredSpectrum> fss = filterReference(createXRayTransitionSet(elm, ref), ref);
      for (final FilteredSpectrum fs : fss)
         mFilteredPackets.add(new FilteredPacket(fs));
      markDirty();
   }

   /**
    * Add a references spectrum associated with the specified RegionOfInterest.
    * The RegionOfInterest must be associated with one and only one Element ie.
    * roi.getElementSet().size()==1.
    *
    * @param roi
    * @param ref
    */
   public void addReference(final RegionOfInterest roi, final ISpectrumData ref) throws EPQException {
      assert roi.getElementSet().size() == 1;
      final Element elm = roi.getElementSet().first();
      final RegionOfInterestSet rois = new RegionOfInterestSet(roi);
      mDetector.checkSpectrumScale(ref, SCALE_TOLERANCE);
      final Collection<FilteredSpectrum> fss = filterReference(ref, rois, elm);
      for (final FilteredSpectrum fs : fss)
         mFilteredPackets.add(new FilteredPacket(fs));
      markDirty();
   }

   /**
    * Remove the standard spectra (if any) assigned to the specified element.
    *
    * @param elm
    */
   public void removeReference(final Element elm) {
      for (final Iterator<FilteredPacket> i = mFilteredPackets.iterator(); i.hasNext();)
         if (i.next().getElement().equals(elm))
            i.remove();
      markDirty();
   }

   /**
    * fitFunction - Implements the abstract method in LinearLeastSquares for
    * fitting filtered references to a filtered spectrum.
    *
    * @param x
    *           double
    * @param y
    *           double[]
    * @see gov.nist.microanalysis.Utility.LinearLeastSquares#fitFunction(double,
    *      double[])
    */
   @Override
   protected void fitFunction(final double x, final double[] y) {
      assert mTmpFitData != null;
      assert (mFilteredPackets != null);
      assert (y.length == mFilteredPackets.size());
      assert (y.length == mTmpFitData.length);
      final int ch = (int) x;
      assert ch >= 0;
      for (int j = 0; j < mTmpFitData.length; ++j)
         y[j] = mTmpFitData[j] != null ? mTmpFitData[j][ch] : 0.0;
   }

   /**
    * @see gov.nist.microanalysis.Utility.LinearLeastSquares#fitFunctionCount()
    */
   @Override
   public int fitFunctionCount() {
      return mFilteredPackets.size();
   }

   /**
    * Get the results of the FilterFit process as a KRatioSet. This assumes that
    * the StandardSpectrum objects were associated with elements. Use
    * fitParameter(StandardSpectrum) if the StandardSpectrum objects were not
    * associated with elements.
    *
    * @return The results as a KRatioSet
    */
   @Override
   public KRatioSet getKRatios(final ISpectrumData unk) throws EPQException {
      updateUnknown(unk);
      final KRatioSet res = new KRatioSet();
      for (final FilteredPacket raf : mFilteredPackets)
         if (raf.mFiltered.getElement() != Element.None) {
            // Filter fit seems to consistently overestimate the k-ratio for O K
            if (raf.mFiltered.getElement() == Element.O)
               // Fenigilty's Fudge Factor
               res.addKRatio(raf.mFiltered.getXRayTransitionSet(), UncertainValue2.multiply(0.95, raf.getKRatio()));
            else
               res.addKRatio(raf.mFiltered.getXRayTransitionSet(), raf.getKRatio());
         }
      return res;
   }

   /**
    * Returns 0.0 for perfect fit, 1.0 for terrible fit. Based on the fraction
    * of peak counts that are actually fit by the element set.
    *
    * @return double
    * @throws EPQException
    */
   public double getFitMetric(final ISpectrumData unk) throws EPQException {
      updateUnknown(unk);
      final Set<int[]> rois = PeakROISearch.GaussianSearch.peakROIs(unk, 3.0);
      double sum = 0.0;
      for (final int[] roi : rois) {
         final double eLow = SpectrumUtils.minEnergyForChannel(unk, roi[0]);
         final double eHigh = SpectrumUtils.maxEnergyForChannel(unk, roi[1]);
         sum += SpectrumUtils.backgroundCorrectedIntegral(unk, eLow, eHigh)[0];
      }
      return Math.min(1.0, Math.abs(1.0 - (getFitEventCount(unk, null) / sum)));
   }

   /**
    * Returns the reference spectrum associated with the specified element.
    *
    * @param xrts
    *           XRayTransitionSet
    * @return ISpectrumData
    */
   public ISpectrumData getReference(final XRayTransitionSet xrts) {
      for (final FilteredPacket raf : mFilteredPackets)
         if (raf.getXRayTransitionSet().equals(xrts))
            return raf.mFiltered.getBaseSpectrum();
      return null;
   }

   /**
    * Returns a set containing the filtered spectra associated with the
    * specified element.
    *
    * @param elm
    * @return HashSet&lt;FilteredSpectrum&gt;
    */
   public HashSet<FilteredSpectrum> getFilteredSpectra(final Element elm) {
      final HashSet<FilteredSpectrum> fss = new HashSet<FilteredSpectrum>();
      for (final FilteredPacket raf : mFilteredPackets)
         if (raf.mFiltered.getElement().equals(elm))
            fss.add(raf.mFiltered);
      return fss;
   }

   /**
    * getResidualSpectrum - Returns a ISpectrumData object containing the
    * difference between the unknown and the sum of the reference spectra.
    *
    * @return ISpectrumData
    */
   public ISpectrumData getResidualSpectrum(final ISpectrumData unk) throws EPQException {
      updateUnknown(unk);
      final SpectrumMath res = new SpectrumMath(mFilteredUnknown.getBaseSpectrum());
      for (final FilteredPacket raf : mFilteredPackets) {
         final FilteredSpectrum fs = raf.mFiltered;
         final double norm = Math.max(0.0, raf.mKRatio.doubleValue() * (fs.getNormalization() / mFilteredUnknown.getNormalization()));
         if (norm > 0.0) {
            assert !fs.getElement().equals(Element.None);
            res.subtract(raf.getROISpectrum(mResidualModelThreshold), norm);
         }
      }
      final SpectrumProperties props = res.getProperties(), unkProps = unk.getProperties();
      if (unkProps.isDefined(SpectrumProperties.LiveTime))
         props.setNumericProperty(SpectrumProperties.LiveTime, unkProps.getNumericWithDefault(SpectrumProperties.LiveTime, Double.NaN));
      if (unkProps.isDefined(SpectrumProperties.RealTime))
         props.setNumericProperty(SpectrumProperties.RealTime, unkProps.getNumericWithDefault(SpectrumProperties.RealTime, Double.NaN));
      if (unkProps.isDefined(SpectrumProperties.ProbeCurrent))
         props.setNumericProperty(SpectrumProperties.ProbeCurrent, unkProps.getNumericWithDefault(SpectrumProperties.ProbeCurrent, Double.NaN));
      props.setTextProperty(SpectrumProperties.SpectrumComment, "Filter = " + mFilter.toString());
      SpectrumUtils.rename(res, "Residual[" + mFilteredUnknown.getBaseSpectrum().toString() + "]");
      return res;
   }

   /**
    * getResidualSpectrum - Returns a ISpectrumData object containing the
    * difference between the unknown and the sum of the reference spectra for
    * the elements specified.
    *
    * @param unk
    * @param elms
    * @return ISpectrumData
    * @throws EPQException
    */
   public ISpectrumData getResidualSpectrum(final ISpectrumData unk, Set<Element> elms) throws EPQException {
      updateUnknown(unk);
      final SpectrumMath res = new SpectrumMath(mFilteredUnknown.getBaseSpectrum());
      for (final FilteredPacket raf : mFilteredPackets) {
         final FilteredSpectrum fs = raf.mFiltered;
         final double norm = Math.max(0.0, raf.mKRatio.doubleValue() * (fs.getNormalization() / mFilteredUnknown.getNormalization()));
         if ((norm > 0.0) && elms.contains(fs.getElement())) {
            assert !fs.getElement().equals(Element.None);
            res.subtract(raf.getROISpectrum(mResidualModelThreshold), norm);
         }
      }
      final SpectrumProperties props = res.getProperties(), unkProps = unk.getProperties();
      if (unkProps.isDefined(SpectrumProperties.AcquisitionTime))
         props.setTimestampProperty(SpectrumProperties.AcquisitionTime,
               unkProps.getTimestampWithDefault(SpectrumProperties.AcquisitionTime, new Date()));
      if (unkProps.isDefined(SpectrumProperties.LiveTime))
         props.setNumericProperty(SpectrumProperties.LiveTime, unkProps.getNumericWithDefault(SpectrumProperties.LiveTime, Double.NaN));
      if (unkProps.isDefined(SpectrumProperties.RealTime))
         props.setNumericProperty(SpectrumProperties.RealTime, unkProps.getNumericWithDefault(SpectrumProperties.RealTime, Double.NaN));
      if (unkProps.isDefined(SpectrumProperties.ProbeCurrent))
         props.setNumericProperty(SpectrumProperties.ProbeCurrent, unkProps.getNumericWithDefault(SpectrumProperties.ProbeCurrent, Double.NaN));
      props.setTextProperty(SpectrumProperties.SpectrumComment, "Filter = " + mFilter.toString());
      SpectrumUtils.rename(res, "Residual[" + mFilteredUnknown.getBaseSpectrum().toString() + "]");
      return res;
   }

   /**
    * Returns the total number of x-ray events explained by the elements in the
    * fit excluding the ones listed in <code>exclude</code>.
    *
    * @param strip
    *           (may be null)
    * @return double
    * @throws EPQException
    */
   public double getFitEventCount(final ISpectrumData unk, final Set<Element> strip) throws EPQException {
      updateUnknown(unk);
      double res = 0.0;
      for (final FilteredPacket raf : mFilteredPackets) {
         final FilteredSpectrum fs = raf.mFiltered;
         if ((strip == null) || (!strip.contains(fs.getElement()))) {
            final double norm = Math.max(0.0, raf.mKRatio.doubleValue()) * (fs.getNormalization() / mFilteredUnknown.getNormalization());
            if (norm > 0.0)
               res += norm * raf.getCounts();
         }
      }
      return res;
   }

   private void updateUnknown(final ISpectrumData unk) throws EPQException {
      if ((mFilteredUnknown == null) || (unk != mFilteredUnknown.getBaseSpectrum())) {
         mFilteredUnknown = new FilteredSpectrum(unk, mFilter);
         markDirty();
      }
      perform();
   }

   /**
    * Returns the residual computed from the filtered unknown minus the filtered
    * references times the associated k-ratio. It should be relatively easy to
    * tell from this spectrum whether any major elements have been omitted from
    * the fit.
    *
    * @return ISpectrumData
    * @throws EPQException
    */
   public ISpectrumData getFilteredResidual(final ISpectrumData unk) throws EPQException {
      updateUnknown(unk);
      final SpectrumMath res = new SpectrumMath(mFilteredUnknown);
      for (final FilteredPacket raf : mFilteredPackets)
         res.subtract(raf.mFiltered, Math.max(0.0, raf.mKRatio.doubleValue()));
      return res;
   }

   /**
    * Returns the FilteredSpectrum associated with the n-th fit parameter.
    *
    * @param n
    * @return FilteredSpectrum
    */
   public FilteredSpectrum getFilteredSpectrum(final int n) {
      return mFilteredPackets.get(n).mFiltered;
   }

   /**
    * parameterDescription - Returns a string containing a human-friendly
    * description of the n-th parameter in the fit.
    *
    * @param n
    *           int
    * @return String
    */
   public String parameterDescription(final int n) {
      final FilteredSpectrum fs = getFilteredSpectrum(n);
      return fs != null ? fs.toString() : "Null";
   }

   /**
    * Gets the current value assigned to filteredPackets
    *
    * @return Returns the filteredPackets.
    */
   public List<FilteredPacket> getFilteredPackets() {
      return Collections.unmodifiableList(mFilteredPackets);
   };

   /**
    * Returns the peak integral associated with the specified ROI.
    *
    * @param roi
    * @return The number of counts in the ROI
    */
   public double getPeakIntegral(final RegionOfInterest roi) {
      assert roi.getElementSet().size() == 1;
      for (final FilteredPacket fp : mFilteredPackets)
         if (fp.mFiltered.sameRegionOfInterest(roi.getElementSet().first(), roi))
            return fp.getCounts();
      return 0.0;
   }

   /**
    * If strip unlikely is true then the filter fit process first examines the
    * spectrum for peaks and identifies which elements those peaks could
    * correspond to. Any elements which are not in this list are removed from
    * the fit. This optimization can speed the fit process by a factor of 10 or
    * more. On the down side, it can remove elements which are actually present
    * in very small quantities. Use with care....
    *
    * @return Returns the true if the pretest is performed.
    */
   public boolean isStripUnlikely() {
      return mStripUnlikely;
   }

   /**
    * If strip unlikely is true then the filter fit process first examines the
    * spectrum for peaks and identifies which elements those peaks could
    * correspond to. Any elements which are not in this list are removed from
    * the fit. This optimization can speed the fit process by a factor of 10 or
    * more. On the down side, it can remove elements which are actually present
    * in very small quantities. Use with care....
    *
    * @param stripUnlikely
    *           true to preform this quick prefit test
    */
   public void setStripUnlikely(final boolean stripUnlikely) {
      if (mStripUnlikely != stripUnlikely) {
         mStripUnlikely = stripUnlikely;
         reevaluateAll();
      }
   }

   /**
    * Returns a list of transitions for which there are standards. These are the
    * same transitions which will be fitted.
    *
    * @return Set&lt;XRayTransitionSet&gt;
    */
   public Set<XRayTransitionSet> getTransitions() {
      final TreeSet<XRayTransitionSet> res = new TreeSet<XRayTransitionSet>();
      for (final FilteredPacket fp : mFilteredPackets)
         res.add(fp.mFiltered.getXRayTransitionSet());
      return res;
   }

   /**
    * Gets the current value assigned to residualModelThreshold
    *
    * @return Returns the residualModelThreshold.
    */
   public double getResidualModelThreshold() {
      return mResidualModelThreshold;
   }

   /**
    * Sets the value assigned to residualModelThreshold.
    *
    * @param residualModelThreshold
    *           The value to which to set residualModelThreshold.
    */
   public void setResidualModelThreshold(final double residualModelThreshold) {
      if (mResidualModelThreshold != residualModelThreshold) {
         mResidualModelThreshold = residualModelThreshold;
         for (FilteredPacket fp : mFilteredPackets)
            fp.mRoiSpectrum = null;
      }
   }

   /**
    * Gets the current value assigned to cullingStrategy
    *
    * @return Returns the cullingStrategy.
    */
   public CullingStrategy getCullingStrategy() {
      return mCullingStrategy;
   }

   /**
    * Allows the user to specify a class derived from CullingStragety to use to
    * remove elements from the fit. Set to <code>null</code> for no culling.
    *
    * @param cullingStrategy
    *           The value to which to set cullingStrategy.
    */
   public void setCullingStrategy(CullingStrategy cullingStrategy) {
      if (mCullingStrategy != cullingStrategy) {
         mCullingStrategy = cullingStrategy;
         clearZeroedCoefficients();
         reevaluateAll();
      }
   }

   /**
    * <p>
    * CullByVariance considers all the evidence (k-ratios) associated with an
    * element and keeps only those that exceed a user-specified level of
    * statistical significance.
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
   public static class CullByVariance implements CullingStrategy, Cloneable {

      private final double mSignificance;

      /**
       * Constructs a CullByVariance
       *
       * @param significance
       */
      public CullByVariance(double significance) {
         mSignificance = significance;
      }

      @Override
      public Set<Element> compute(FilterFit ff, UncertainValue2[] fitParams) {
         final Set<Element> removeElm = new TreeSet<Element>();
         final List<FilteredPacket> fps = ff.getFilteredPackets();
         for (final Element elm : ff.getElements()) {
            final List<UncertainValue2> vals = new ArrayList<UncertainValue2>();
            for (int j = 0; j < fps.size(); ++j) {
               final FilteredPacket fp = fps.get(j);
               if (fp.getElement().equals(elm))
                  vals.add(fitParams[j]);
            }
            final UncertainValue2 mean = UncertainValue2.safeWeightedMean(vals);
            if ((!mean.isNaN()) && (mean.doubleValue() < (mSignificance * mean.uncertainty())))
               removeElm.add(elm);
         }
         return removeElm;
      }

      @Override
      public CullingStrategy clone() {
         return new CullByVariance(mSignificance);
      }
   };

   /**
    * <p>
    * DontCull - A mechanism to force certain elements to exist so long as a
    * specific line is positive.
    * </p>
    */
   public static class DontCull implements CullingStrategy, Cloneable {

      private final CullingStrategy mBase;
      private final Set<XRayTransitionSet> mDont;

      /**
       * Constructs a DontCull
       *
       * @param base
       *           base {@link CullingStrategy} to build on
       * @param dont
       *           The set of transitions not to cull if positive
       */
      public DontCull(CullingStrategy base, Collection<XRayTransitionSet> dont) {
         mBase = base;
         mDont = new HashSet<XRayTransitionSet>(dont);
      }

      @Override
      public Set<Element> compute(FilterFit ff, UncertainValue2[] fitParams) {
         final TreeSet<Element> removeElm = new TreeSet<Element>(mBase.compute(ff, fitParams));
         final List<FilteredPacket> fps = ff.getFilteredPackets();
         assert fps.size() == fitParams.length;
         for (int i = 0; i < fps.size(); ++i) {
            final FilteredPacket fp = fps.get(i);
            if (mDont.contains(fp.getXRayTransitionSet()) && (fitParams[i].doubleValue() > 0.0))
               removeElm.remove(fp.getElement());
         }
         return removeElm;
      }

      @Override
      public CullingStrategy clone() {
         return new DontCull(mBase, mDont);
      }
   };

   /**
    * <p>
    * A simple strategy based on the how removal of an element effects the
    * chi-squared statistic. The optimal threshold depends upon count
    * statistics. For spectra with good count statistics, a 1% increase (1.01)
    * represents a good threshold. For spectra with less good statistics, 10% or
    * larger might be appropriate. This necessity to tune the strategy makes it
    * in general a poor choice.
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
   public class CullByChiSquared implements CullingStrategy, Cloneable {

      private double mThreshold = 1.01;

      public CullByChiSquared(final double threshold) {
         mThreshold = threshold;
      }

      @Override
      public Set<Element> compute(final FilterFit ff, final UncertainValue2[] fitParams) {
         final Set<Element> checked = new TreeSet<Element>();
         final Set<Element> removeElm = new TreeSet<Element>();
         try {
            final double none = chiSquared();
            for (int j = 0; j < mFilteredPackets.size(); ++j) {
               final FilteredSpectrum fs = mFilteredPackets.get(j).mFiltered;
               assert fs != null;
               final Element elm = fs.getElement();
               if ((fitParams[j].doubleValue() != 0.0) && (!checked.contains(elm))) {
                  final UncertainValue2[] fpDup = fitParams.clone();
                  for (int i = 0; i < mFilteredPackets.size(); ++i) {
                     final FilteredSpectrum fs2 = mFilteredPackets.get(i).mFiltered;
                     assert fs2 != null;
                     if (fs2.getElement() == elm)
                        fpDup[i] = UncertainValue2.ZERO;
                  }
                  if (chiSquared(fpDup) < (mThreshold * none))
                     removeElm.add(elm);
               }
            }
         } catch (final EPQException ex) {
            removeElm.clear();
         }
         return removeElm;
      }

      @Override
      public CullingStrategy clone() {
         return new CullByChiSquared(mThreshold);
      }
   };

   /**
    * <p>
    * This class makes certain that if a minor line is present that the
    * associated major line is also present. For example, if MG is present then
    * MA1 must also be present.
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
   public static class CullWithinFamily implements CullingStrategy, Cloneable {

      static private final int[] mLines = {XRayTransition.KA1, // 0
            XRayTransition.KB1, // 1
            XRayTransition.LA1, // 2
            XRayTransition.LB1, // 3
            XRayTransition.LB2, // 4
            XRayTransition.LG1, // 5
            XRayTransition.LG3, // 6
            XRayTransition.Ll, // 7
            XRayTransition.MA1, // 8
            XRayTransition.MB, // 9
            XRayTransition.MZ1, // 10
            XRayTransition.MG, // 11
            XRayTransition.M2N4, // 12
      };

      static private final int FIRST_L = 2;
      static private final int LAST_L = 7;
      static private final int FIRST_M = 8;
      static private final int LAST_M = 12;

      private final double mThreshold = 3.0;

      @Override
      public CullingStrategy clone() {
         final CullWithinFamily res = new CullWithinFamily();
         return res;
      }

      /**
       * @param ff
       * @param fitParams
       * @return Set&lt;Element&gt;
       * @see gov.nist.microanalysis.EPQLibrary.FilterFit.CullingStrategy#compute(gov.nist.microanalysis.EPQLibrary.FilterFit,
       *      gov.nist.microanalysis.Utility.UncertainValue2[])
       */
      @Override
      public Set<Element> compute(final FilterFit ff, final UncertainValue2[] fitParams) {
         // present: the set of all XRayTransitions for which there is solid
         // evidence
         final TreeSet<XRayTransition> present = new TreeSet<XRayTransition>();
         for (final FilteredPacket fp : ff.getFilteredPackets()) {
            final XRayTransitionSet xrts = fp.getFiltered().getXRayTransitionSet();
            final UncertainValue2 kr = fp.getKRatio();
            final boolean b = ((kr.doubleValue() / kr.uncertainty()) > mThreshold);
            for (final int mLine : mLines) {
               final XRayTransition xrt = xrts.find(mLine);
               if ((xrt != null) && b)
                  present.add(xrt);
            }
         }
         final TreeSet<Element> removeThese = new TreeSet<Element>();
         final Set<Element> elms = ff.getElements();
         // Checks to see that if a minor line is present then the primary
         // line
         // is also present
         for (final Element elm : elms) {
            // If KB1 present then KA1 must also be
            if (present.contains(new XRayTransition(elm, XRayTransition.KB1)) && (!present.contains(new XRayTransition(elm, XRayTransition.KA1))))
               removeThese.add(elm);
            // If any of LB1,..,Ll present then LA1 must also be
            {
               boolean b = false;
               for (int tr = FIRST_L + 1; tr <= LAST_L; ++tr)
                  b |= present.contains(new XRayTransition(elm, mLines[tr]));
               if (b && (!present.contains(new XRayTransition(elm, XRayTransition.LA1))))
                  removeThese.add(elm);
            }
            // If any of MB,..,M2N4 present then MA1 must also be
            {
               boolean b = false;
               for (int tr = FIRST_M + 1; tr <= LAST_M; ++tr)
                  b |= present.contains(new XRayTransition(elm, mLines[tr]));
               if (b && (!present.contains(new XRayTransition(elm, XRayTransition.MA1))))
                  removeThese.add(elm);
            }
         }
         return removeThese;
      }
   }

   /**
    * <p>
    * Makes certain that all families that should be visible are visible. For
    * example if both K and L should be visible but only one of K or L is
    * visible the element should probably not be included in the fit.
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
   public static class CullByFamilies implements CullingStrategy, Cloneable {

      private double mThreshold = 3.0;

      public CullByFamilies() {
         mThreshold = 3.0;
      }

      public CullByFamilies(final double thresh) {
         mThreshold = thresh;
      }

      @Override
      public CullingStrategy clone() {
         final CullByFamilies res = new CullByFamilies(mThreshold);
         return res;
      }

      @Override
      public Set<Element> compute(final FilterFit ff, final UncertainValue2[] fitParams) {
         final TreeSet<Element> removeThese = new TreeSet<Element>();
         /*
          * Do inter-family checks... If the lower energy family is visible and
          * the higher energy family is visible in the reference then the higher
          * energy family should be visible in the unknown.
          */
         for (final Element elm : ff.getElements()) {
            // Find the fits for KA1, LA1 and MA1
            final XRayTransition xrtKa1 = new XRayTransition(elm, XRayTransition.KA1);
            final XRayTransition xrtLa1 = new XRayTransition(elm, XRayTransition.LA1);
            final XRayTransition xrtMa1 = new XRayTransition(elm, XRayTransition.MA1);
            double kS = 0.0, lS = 0.0, mS = 0.0;
            FilteredPacket fpKa1 = null, fpLa1 = null, fpMa1 = null;
            for (final FilteredPacket fp : ff.getFilteredPackets())
               if (fp.getFiltered().getElement().equals(elm)) {
                  final XRayTransitionSet xrts = fp.getFiltered().getXRayTransitionSet();
                  final UncertainValue2 kr = fp.getKRatio();
                  if (xrts.contains(xrtKa1)) {
                     fpKa1 = fp;
                     kS = kr.doubleValue() / kr.uncertainty();
                  }
                  if (xrts.contains(xrtLa1)) {
                     fpLa1 = fp;
                     lS = kr.doubleValue() / kr.uncertainty();
                  }
                  if (xrts.contains(xrtMa1)) {
                     fpMa1 = fp;
                     mS = kr.doubleValue() / kr.uncertainty();
                  }
               }
            // No evidence...
            if ((kS < mThreshold) && (lS < mThreshold) && (mS < mThreshold))
               removeThese.add(elm);
            // If l visible but intense k not visible
            if ((fpLa1 != null) && (lS > mThreshold) && (fpKa1 != null) && (fpKa1.getCounts() > fpLa1.getCounts()) && (kS < mThreshold))
               removeThese.add(elm);
            // If m visible but intense l not visible
            if ((fpLa1 != null) && (mS > mThreshold) && (fpMa1 != null) && (fpLa1.getCounts() > fpMa1.getCounts()) && (lS < mThreshold))
               removeThese.add(elm);
         }
         return removeThese.size() > 0 ? removeThese : null;
      }
   }

   public static class CullByOptimal implements CullingStrategy, Cloneable {
      final double mSigma;
      final Map<Element, XRayTransition> mMapOfXRTS;

      public CullByOptimal(double sigma, Collection<XRayTransitionSet> sxrts) {
         mSigma = sigma;
         mMapOfXRTS = new TreeMap<Element, XRayTransition>();
         for (XRayTransitionSet xrts : sxrts) {
            Element el = xrts.getElement();
            final XRayTransition wt = xrts.getWeighiestTransition();
            if (el.getAtomicNumber() >= Element.Ge.getAtomicNumber()) {
               // Addresses issue when Sr K is not seen (this also addresses low
               // over-voltage due to charging)
               if (wt.getFamily() < AtomicShell.LFamily) {
                  XRayTransitionSet xrts2 = new XRayTransitionSet(new AtomicShell(el, AtomicShell.LIII));
                  mMapOfXRTS.put(el, xrts2.getWeighiestTransition());
               }
            } else if (el.getAtomicNumber() == Element.Yb.getAtomicNumber()) {
               // Addresses issue when W L3-M1 is mistaken for Yb L3-M5
               if (wt.getFamily() < AtomicShell.MFamily)
                  mMapOfXRTS.put(el, new XRayTransition(el, 7, 14));
            }
            if (!mMapOfXRTS.containsKey(el))
               mMapOfXRTS.put(el, wt);
         }
      }

      private CullByOptimal(double sigma, Map<Element, XRayTransition> mxrt) {
         mSigma = sigma;
         mMapOfXRTS = new TreeMap<Element, XRayTransition>(mxrt);
      }

      @Override
      public CullingStrategy clone() {
         return new CullByOptimal(mSigma, mMapOfXRTS);
      }

      @Override
      public Set<Element> compute(final FilterFit ff, final UncertainValue2[] fitParams) {
         final Set<Element> elms = ff.getElements();
         final TreeSet<Element> removeThese = new TreeSet<Element>();
         final List<FilteredPacket> fps = ff.getFilteredPackets();
         for (final Element elm : elms) {
            final XRayTransition opt = mMapOfXRTS.get(elm);
            assert opt != null : "Optimal transition is null for " + elm;
            for (int j = 0; j < fps.size(); ++j) {
               final FilteredPacket fp = fps.get(j);
               if (fp.getElement().equals(elm) && fp.getXRayTransitionSet().contains(opt)) {
                  final UncertainValue2 kr = fp.mKRatio;
                  final boolean keep = ((kr.doubleValue() > 0.0) && (kr.uncertainty() <= 0.0))
                        || (Math.max(0.0, kr.doubleValue()) / kr.uncertainty() > mSigma);
                  if (!keep)
                     removeThese.add(elm);
                  break;
               }
            }
         }
         return removeThese;
      }
   }

   public static class CullByAverageUncertainty implements CullingStrategy, Cloneable {

      final double mOneAbove;
      final double mAvgAbove;

      public CullByAverageUncertainty(final double oneAbove, final double avgAbove) {
         mAvgAbove = avgAbove;
         mOneAbove = oneAbove;
      }

      @Override
      public CullingStrategy clone() {
         final CullByAverageUncertainty res = new CullByAverageUncertainty(mOneAbove, mAvgAbove);
         return res;
      }

      @Override
      public Set<Element> compute(final FilterFit ff, final UncertainValue2[] fitParams) {
         final Set<Element> elms = ff.getElements();
         final TreeSet<Element> removeThese = new TreeSet<Element>();
         final List<FilteredPacket> fps = ff.getFilteredPackets();
         for (final Element elm : elms) {
            boolean keep = false;
            double sum = 0.0;
            int cx = 0;
            for (int j = 0; j < fps.size(); ++j) {
               final FilteredPacket fp = fps.get(j);
               if (fp.getElement().equals(elm)) {
                  final UncertainValue2 kr = fp.mKRatio;
                  if ((kr.doubleValue() != 0.0) && (kr.uncertainty() > 0.0)) {
                     final double uu = Math.max(0.0, kr.doubleValue()) / kr.uncertainty();
                     sum += uu;
                     ++cx;
                     if (uu > mOneAbove) {
                        keep = true;
                        break;
                     }
                  }
               }
            }
            keep = keep || (cx == 0) || (sum > (mAvgAbove * cx));
            if (!keep)
               removeThese.add(elm);
         }
         return removeThese;
      }
   }

   /**
    * <p>
    * Check the most intense line (according to the standard) for each element
    * and remove the element if this line is not clearly present.
    * </p>
    * <p>
    * This strategy often works well. It fails when the brightest line is at low
    * energy and for some reason the line is strongly absorbed.
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
   public static class CullByBrightest implements CullingStrategy, Cloneable {

      private final double mThreshold;

      public CullByBrightest(final double thresh) {
         super();
         mThreshold = thresh;
      }

      @Override
      public CullingStrategy clone() {
         final CullByBrightest res = new CullByBrightest(mThreshold);
         return res;
      }

      /**
       * @param ff
       * @param fitParams
       * @return Set&lt;Element&gt;
       * @see gov.nist.microanalysis.EPQLibrary.FilterFit.CullingStrategy#compute(gov.nist.microanalysis.EPQLibrary.FilterFit,
       *      gov.nist.microanalysis.Utility.UncertainValue2[])
       */
      @Override
      public Set<Element> compute(final FilterFit ff, final UncertainValue2[] fitParams) {
         final TreeSet<Element> removeThese = new TreeSet<Element>();
         try {
            /*
             * Find the most intense line family in the reference and make
             * certain that it is visible
             */
            for (final Element elm : ff.getElements()) {
               FilteredPacket bestFp = null;
               for (final FilteredPacket fp : ff.getFilteredPackets())
                  if (fp.getElement().equals(elm))
                     if ((bestFp == null) || ((fp.mKRatio.uncertainty() != 0.0) && (fp.getCounts() > bestFp.getCounts())))
                        bestFp = fp;
               if ((bestFp != null) && ((bestFp.mKRatio.doubleValue() / bestFp.mKRatio.uncertainty()) < mThreshold))
                  removeThese.add(elm);
            }
         } catch (final Throwable th) {
            th.printStackTrace();
         }
         return removeThese;
      }
   }

   /**
    * <p>
    * Remove one element based on the presence of another. ie. F in the presence
    * of Fe.
    * </p>
    * <p>
    * The algorithm determines which element has the better statistical support.
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
   public static class SpecialCulling implements CullingStrategy, Cloneable {

      private final TreeMap<Element, Element> mMap = new TreeMap<Element, Element>();

      @Override
      public CullingStrategy clone() {
         final SpecialCulling res = new SpecialCulling();
         res.mMap.putAll(mMap);
         return res;
      }

      /**
       * Remove <code>removeMe</code> when the element <code>whenPresent</code>
       * is present with better statistics.
       *
       * @param removeMe
       * @param whenPresent
       */
      public void add(final Element removeMe, final Element whenPresent) {
         mMap.put(removeMe, whenPresent);
      }

      /**
       * Returns the element to remove when the specified element is present.
       *
       * @param whenPresent
       * @return Element
       */
      public Element removed(final Element whenPresent) {
         for (final Map.Entry<Element, Element> me : mMap.entrySet())
            if (me.getValue().equals(whenPresent))
               return me.getKey();
         return null;
      }

      /**
       * Returns the element which triggers the removal of the specified
       * element.
       *
       * @param removeMe
       * @return Element
       */
      public Element trigger(final Element removeMe) {
         return mMap.get(removeMe);
      }

      /**
       * @param ff
       * @param fitParams
       * @return Set&lt;Element&gt;
       * @see gov.nist.microanalysis.EPQLibrary.FilterFit.CullingStrategy#compute(gov.nist.microanalysis.EPQLibrary.FilterFit,
       *      gov.nist.microanalysis.Utility.UncertainValue2[])
       */
      @Override
      public Set<Element> compute(final FilterFit ff, final UncertainValue2[] fitParams) {
         final TreeSet<Element> removeThese = new TreeSet<Element>();
         try {

            for (final Map.Entry<Element, Element> me : mMap.entrySet()) {
               final Element removeMe = me.getKey();
               final Element whenPresent = me.getValue();
               final ArrayList<UncertainValue2> removeUvs = new ArrayList<UncertainValue2>();
               final ArrayList<UncertainValue2> presentUvs = new ArrayList<UncertainValue2>();
               for (final FilteredPacket fp : ff.getFilteredPackets())
                  if (!Double.isNaN(fp.mKRatio.fractionalUncertainty())) {
                     if (fp.getElement().equals(removeMe))
                        removeUvs.add(fp.mKRatio);
                     if (fp.getElement().equals(whenPresent))
                        presentUvs.add(fp.mKRatio);
                  }
               if ((removeUvs.size() == 0) || (presentUvs.size() == 0))
                  continue;
               final UncertainValue2 removeUv = UncertainValue2.safeWeightedMean(removeUvs);
               final UncertainValue2 presentUv = UncertainValue2.safeWeightedMean(presentUvs);
               if (removeUv.doubleValue() > 0.0) {
                  final double presentSN = presentUv.doubleValue() / presentUv.uncertainty();
                  final double removeSN = removeUv.doubleValue() / removeUv.uncertainty();
                  if ((!Double.isNaN(presentSN)) && (!Double.isNaN(removeSN)))
                     if ((presentSN > 5.0 * removeSN) && (removeSN < 6.0))
                        removeThese.add(removeMe);
               }
            }
         } catch (final Throwable th) {
            th.printStackTrace();
         }
         return removeThese;
      }
   }

   /**
    * <p>
    * Allows multiple CullingStrategy objects to be strung together
    * sequentially. The resulting list of Elements consists of elements removed
    * by any of the constituent CullingStrategy objects.
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
   public static class CompoundCullingStrategy implements CullingStrategy, Cloneable {

      private final List<CullingStrategy> mStrategies;

      public CompoundCullingStrategy() {
         mStrategies = new ArrayList<CullingStrategy>();
      }

      /**
       * Applys all the CullingStrategy objects in the order in which they are
       * entered into the list and returns the intersection of the resulting
       * element sets.
       *
       * @param ff
       * @param fitParams
       * @return Set&lt;Element&gt;
       * @see gov.nist.microanalysis.EPQLibrary.FilterFit.CullingStrategy#compute(gov.nist.microanalysis.EPQLibrary.FilterFit,
       *      gov.nist.microanalysis.Utility.UncertainValue2[])
       */
      @Override
      public Set<Element> compute(final FilterFit ff, final UncertainValue2[] fitParams) {
         final Set<Element> removeThese = new TreeSet<Element>();
         for (final CullingStrategy cs : mStrategies) {
            final Set<Element> res = cs.compute(ff, fitParams);
            if ((res != null) && (res.size() > 0))
               removeThese.addAll(res);
         }
         return removeThese;
      }

      /**
       * Add a new CullingStrategy to the front of the list (called first)
       *
       * @param cs
       */
      public void prepend(final CullingStrategy cs) {
         mStrategies.add(0, cs);
      }

      /**
       * Add a new CullingStrategy to the end of the list (called last)
       *
       * @param cs
       */
      public void append(final CullingStrategy cs) {
         mStrategies.add(cs);
      }

      /**
       * Remove a culling strategy from the list.
       *
       * @param cs
       */
      public void remove(final CullingStrategy cs) {
         mStrategies.remove(cs);
      }

      /**
       * Remove all strategies from the list.
       */
      public void clear() {
         mStrategies.clear();
      }

      @Override
      public CullingStrategy clone() {
         final CompoundCullingStrategy res = new CompoundCullingStrategy();
         for (final CullingStrategy cs : mStrategies)
            res.mStrategies.add(cs.clone());
         return res;
      }

   }

   static public void setNaiveBackground(boolean useNaive) {
      mNaive = useNaive;
   }
}
