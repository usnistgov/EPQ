package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;

import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.Utility.Interval;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Creates a filtered spectrum from a unfiltered spectrum.
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

public class FilteredSpectrum extends DerivedSpectrum {

   // Local member data
   private transient double[] mFilteredData;
   private transient double[] mErrors;
   private transient Interval mNonZero;
   // When mROI and mElement are null the full spectrum is filtered
   private final RegionOfInterestSet.RegionOfInterest mROI;
   private final Element mElement;
   private final FittingFilter mFilter;
   private final VariableWidthFittingFilter mVarFilter;
   private final double mNormalization;

   private void compute(ISpectrumData spec, RegionOfInterest roi) {
      final int lld = SpectrumUtils.getZeroStrobeDiscriminatorChannel(spec);
      final int lowCh = SpectrumUtils.bound(spec, Math.max(lld, SpectrumUtils.channelForEnergy(spec, FromSI.eV(roi.lowEnergy()))));
      final int highCh = SpectrumUtils.bound(spec, SpectrumUtils.channelForEnergy(spec, FromSI.eV(roi.highEnergy())));
      compute(SpectrumUtils.slice(spec, lowCh, highCh - lowCh), lowCh, spec.getChannelCount());
   }

   private void compute(ISpectrumData spec) {
      final int lld = SpectrumUtils.getZeroStrobeDiscriminatorChannel(spec);
      final double[] data = SpectrumUtils.toDoubleArray(spec);
      Arrays.fill(data, 0, lld, data[lld]);
      compute(data, 0, spec.getChannelCount());
   }

   public Interval getNonZeroInterval() {
      if (mFilteredData == null)
         computeFilteredSpectrum();
      return mNonZero;
   }

   /**
    * Computes the filtered spectrum region.
    * 
    * @param roiData
    *           The raw spectrum data (determines the length of the data)
    * @param lowCh
    *           Specifies the low channel
    * @param chCount
    *           Total number of channels in the spectrum
    */
   private void compute(double[] roiData, int lowCh, int chCount) {
      final double[] tmp = new double[chCount];
      final double[] err = new double[chCount];
      if (mFilter != null) {
         assert mFilter.zeroSum();
         final double[] filter = mFilter.getFilter();
         // Perform filter
         final int hl = filter.length / 2, ol = filter.length - hl;
         for (int si = -hl; si < (roiData.length + ol); ++si) {
            final int ch = si + lowCh;
            if ((ch >= 0) && (ch < chCount)) {
               double sum = 0.0, errs = 0.0;
               for (int fi = 0; fi < filter.length; ++fi) {
                  final double fr = filter[fi] * roiData[Math2.bound((si - hl) + fi, 0, roiData.length)];
                  sum += fr;
                  errs += filter[fi] * fr;
               }
               tmp[ch] = mNormalization * sum;
               err[ch] = errs > 0.0 ? mNormalization * Math.sqrt(errs) : Double.MAX_VALUE;
            }
         }
         mFilteredData = tmp;
         mErrors = err;
      } else {
         assert mVarFilter != null;
         final double[] chdata = new double[chCount];
         System.arraycopy(roiData, 0, chdata, lowCh, roiData.length);
         double lowV=0.0, highV=0.0;
         for(int i=0;i<3;++i) {
            lowV+=roiData[i];
            highV+=roiData[roiData.length-(i+1)];
         }
         Arrays.fill(chdata,  0, lowCh, lowV/3.0);
         Arrays.fill(chdata,  lowCh+roiData.length, chdata.length, highV/3.0);
         final double[][] filtvar = mVarFilter.compute(chdata);
         for (int ch = 0; ch < chCount; ++ch) {
            filtvar[0][ch] *= mNormalization;
            filtvar[1][ch] = filtvar[1][ch] > 0.0 ? mNormalization * Math.sqrt(filtvar[1][ch]) : Double.MAX_VALUE;
         }
         mFilteredData = filtvar[0];
         mErrors = filtvar[1];
      }
      mNonZero = Interval.nonZeroInterval(mFilteredData);
   }

   private void computeFilteredSpectrum() {
      // assert mFilter.zeroSum();
      final ISpectrumData src = getBaseSpectrum();
      String specDesc;
      if (mROI != null) {
         compute(src, mROI);
         specDesc = "Filtered[" + getElement().toAbbrev() + "," + (mROI != null ? mROI.toString() : "No ROI") + "," + mSource.toString() + "]";
      } else {
         compute(src);
         specDesc = "Filtered[" + mSource.toString() + "]";
      }
      getProperties().setTextProperty(SpectrumProperties.SpecimenDesc, specDesc);
   }

   /**
    * Creates a filtered spectrum that corresponds to the filtered version of
    * the source spectrum. The ISpectrumData is filtered about the specified
    * RegionOfInterest
    * 
    * @param src
    *           ISpectrumData
    * @param elm
    *           Element
    * @param roi
    *           RegionOfInterestSet.RegionOfInterest
    * @param ff
    *           FittingFilter
    */
   public FilteredSpectrum(ISpectrumData src, Element elm, RegionOfInterestSet.RegionOfInterest roi, FittingFilter ff) throws EPQException {
      super(src);
      mNormalization = 1.0 / SpectrumUtils.getDose(mSource.getProperties());
      mFilter = ff;
      mVarFilter = null;
      mROI = roi;
      mElement = elm;
      assert (roi == null) || roi.getElementSet().contains(elm);
   }

   public FilteredSpectrum(ISpectrumData src, Element elm, RegionOfInterestSet.RegionOfInterest roi, VariableWidthFittingFilter varff)
         throws EPQException {
      super(src);
      mNormalization = 1.0 / SpectrumUtils.getDose(mSource.getProperties());
      mFilter = null;
      mVarFilter = varff;
      mROI = roi;
      mElement = elm;
      assert (roi == null) || roi.getElementSet().contains(elm);
   }

   /**
    * Constructs a FilteredSpectrum from a source ISpectrumData.
    * 
    * @param src
    *           ISpectrumData
    * @param ff
    *           FittingFilter
    */
   public FilteredSpectrum(ISpectrumData src, FittingFilter ff) throws EPQException {
      this(src, null, null, ff);
   }

   public FilteredSpectrum(ISpectrumData src, VariableWidthFittingFilter varff) throws EPQException {
      this(src, null, null, varff);
   }

   /**
    * Does this FilteredSpectrum represent the same region of interest as the
    * arguments.
    * 
    * @param el
    * @param roi
    * @return boolean
    */
   public boolean sameRegionOfInterest(Element el, RegionOfInterestSet.RegionOfInterest roi) {
      return el.equals(getElement()) && roi.equals(mROI);
   }

   /**
    * getElement - If this spectrum is a reference then getElement returns the
    * element associated with this FilteredSpectrum. Otherwise getElement
    * returns null.
    * 
    * @return Element
    */
   public Element getElement() {
      return mElement;
   }

   /**
    * getXRayTransitionSet - Returns the XRayTransitionSet which was filtered
    * for fitting.
    * 
    * @return XRayTransitionSet
    */
   public XRayTransitionSet getXRayTransitionSet() {
      return mROI != null ? mROI.getXRayTransitionSet(mElement) : new XRayTransitionSet();
   }

   /**
    * isReference - Does this FilteredSpectrum represent a reference for a
    * single element and line?
    * 
    * @return boolean
    */
   public boolean isReference() {
      assert ((mROI == null) && (mElement != null)) || ((mElement != null) && (mROI != null));
      return mROI != null;
   }

   /**
    * Returns an array of sqrt(n) estimated measurement errors.
    * 
    * @return double[]
    */
   public double[] getErrors() {
      if (mFilteredData == null)
         computeFilteredSpectrum();
      return mErrors;
   }

   /**
    * Returns the filtered data as a double array
    * 
    * @return double[]
    */
   public double[] getFilteredData() {
      if (mFilteredData == null)
         computeFilteredSpectrum();
      return mFilteredData;
   }

   public FittingFilter getFilter() {
      return mFilter;
   }

   @Override
   public double getCounts(int i) {
      if (mFilteredData == null)
         computeFilteredSpectrum();
      return i < mFilteredData.length ? mFilteredData[i] : 0.0;
   }

   @Override
   public int compareTo(ISpectrumData obj) {
      if (obj instanceof FilteredSpectrum) {
         final FilteredSpectrum fs = (FilteredSpectrum) obj;
         final Element el = getElement();
         if ((el == null) && (fs.getElement() == null))
            return 0;
         if (el != null) {
            int res = el.compareTo(fs.getElement());
            if (res == 0)
               res = mROI.compareTo(fs.mROI);
            return res;
         }
      }
      return -1;
   }

   public double getNormalization() {
      return mNormalization;
   }

   /**
    * Returns the RegionOfInterest associated with this FilteredSpectrum
    * 
    * @return RegionOfInterestSet.RegionOfInterest or null
    */
   public RegionOfInterestSet.RegionOfInterest getRegionOfInterest() {
      return mROI;
   }
}
