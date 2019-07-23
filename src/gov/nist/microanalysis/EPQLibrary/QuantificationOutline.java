package gov.nist.microanalysis.EPQLibrary;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.CompositionFromKRatios.UnmeasuredElementRule;
import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.TextUtilities;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * QuantificationOutline allows a user to outline the plan for a measurement by
 * specifying standard materials, un-measured element rules (UER), elements to
 * strip and surface coatings. The use of a certain standard/EUR/strip/coatings
 * requires certain references which this class will identify.
 * </p>
 * <p>
 * The class also calculates generated intensities for the standards and
 * references at the NOMINAL_DOSE.
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
public class QuantificationOutline {

   public static final double NOMINAL_DOSE = 60.0;

   /**
    * <p>
    * It is not necessary to know the precise composition of a reference - only
    * the elements present in the reference. This class handles this by
    * retaining the Composition when available or defaulting to a
    * Set&lt;Element&gt; otherwise.
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
   public class ReferenceMaterial
      implements
      Comparable<ReferenceMaterial> {
      /**
       * A reference material is optimally defined by a composition
       */
      private final Composition mComposition;
      /**
       * But may simple be defined by a list of elements.
       */
      private final TreeSet<Element> mElements;
      /**
       * A map between an ROI and the associated measured intensity (60 nA.s
       * dose) in the relevant XRayTransition(s).
       */
      private final Map<RegionOfInterest, UncertainValue2> mMeasuredIntensity = new HashMap<RegionOfInterest, UncertainValue2>();

      private ReferenceMaterial(final Composition comp) {
         mComposition = comp;
         mElements = null;
      }

      private ReferenceMaterial(final Collection<Element> elms) {
         mComposition = null;
         mElements = new TreeSet<Element>(elms);
      }

      private ReferenceMaterial(final ReferenceMaterial rm) {
         mComposition = rm.mComposition;
         mElements = rm.mElements;
      }

      /**
       * Computes the measured x-ray intensity for all transitions in the ROI as
       * would be measured on the detector at 60 nA.s dose. If the reference is
       * an element list, the intensity is calculated for a pure element.
       *
       * @param roi
       * @return UncertainValue2 The number of x-ray counts at a dose of
       *         NOMINAL_DOSE and the associated count statistics limited
       *         uncertainty.
       * @throws EPQException
       */
      protected UncertainValue2 getMeasuredIntensity(final RegionOfInterest roi)
            throws EPQException {
         assert roi.getElementSet().size() == 1;
         Composition comp = mComposition;
         if(comp == null)
            comp = new Composition(roi.getElementSet().first());
         assert comp.containsElement(roi.getElementSet().first());
         if(!mMeasuredIntensity.containsKey(roi)) {
            final double sum = computeTotalIntensity(comp, roi);
            mMeasuredIntensity.put(roi, new UncertainValue2(sum, "k", Math.sqrt(sum)));
         }
         assert roi.getAllTransitions().isValid() : roi + ", " + roi.getAllTransitions();
         return mMeasuredIntensity.get(roi);
      }

      public Set<Element> getElementSet() {
         return mComposition != null ? mComposition.getElementSet() : mElements;
      }

      public Composition getComposition() {
         return mComposition;
      }

      public boolean compositionIsAvailable() {
         return mComposition != null;
      }

      @Override
      public String toString() {
         if(mComposition != null)
            return mComposition.toString();
         else
            return "Material containing " + QuantificationOutline.toString(mElements);
      }

      /**
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = (prime * result) + getOuterType().hashCode();
         result = (prime * result) + ((mComposition == null) ? 0 : mComposition.hashCode());
         result = (prime * result) + ((mElements == null) ? 0 : mElements.hashCode());
         return result;
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
         if(!(obj instanceof ReferenceMaterial))
            return false;
         final ReferenceMaterial other = (ReferenceMaterial) obj;
         if(!getOuterType().equals(other.getOuterType()))
            return false;
         if(mComposition == null) {
            if(other.mComposition != null)
               return false;
         } else if(!mComposition.equals(other.mComposition))
            return false;
         if(mElements == null) {
            if(other.mElements != null)
               return false;
         } else if(!mElements.equals(other.mElements))
            return false;
         return true;
      }

      /**
       * @see java.lang.Comparable#compareTo(java.lang.Object)
       */
      @Override
      public int compareTo(final ReferenceMaterial arg0) {
         int res = 0;
         // By composition first
         if(mComposition != null)
            res = arg0.mComposition != null ? mComposition.compareTo(arg0.mComposition) : 1;
         else if(arg0.mComposition != null)
            res = -1;
         // Then by element set
         if(res == 0)
            if(mElements != null) {
               if(arg0.mElements != null) {
                  final Set<Element> all = new TreeSet<Element>();
                  all.addAll(mElements);
                  all.addAll(arg0.mElements);
                  for(final Element elm : all)
                     if(!mElements.contains(elm)) {
                        res = -1;
                        break;
                     } else if(!arg0.mElements.contains(elm)) {
                        res = 1;
                        break;
                     }
               } else
                  res = 1;
            } else if(arg0.mElements != null)
               res = -1;
         return res;
      }

      private QuantificationOutline getOuterType() {
         return QuantificationOutline.this;
      }
   }

   /**
    * <p>
    * The data in common for StandardPacket, UERPacket and StripPacket. This
    * data is the Element and the RegionOfInterestSet associated with this
    * element.
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
   private class ElementPacket
      implements
      Comparable<ElementPacket> {

      private final Element mElement;
      private final RegionOfInterestSet mElementROIS;

      protected ElementPacket(final Element elm) {
         mElement = elm;
         mElementROIS = LinearSpectrumFit.createElementROIS(elm, mDetector, mBeamEnergy);
      }

      public RegionOfInterestSet getElementROIS() {
         return mElementROIS;
      }

      public Element getElement() {
         return mElement;
      }

      /**
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         return mElement.hashCode();
      }

      /**
       * @param obj
       * @return
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(final Object obj) {
         if(this == obj)
            return true;
         if(obj == null)
            return false;
         if(!(obj instanceof ElementPacket))
            return false;
         final ElementPacket other = (ElementPacket) obj;
         if(!getOuterType().equals(other.getOuterType()))
            return false;
         return mElement.equals(other.mElement);
      }

      private QuantificationOutline getOuterType() {
         return QuantificationOutline.this;
      }

      @Override
      public int compareTo(final ElementPacket o) {
         return mElement.compareTo(o.mElement);
      }

   }

   private class StripPacket
      extends
      ElementPacket {

      private StripPacket(final Element elm) {
         super(elm);
      }

      @Override
      public String toString() {
         return "Strip[" + getElement() + "]";
      }

   }

   /**
    * <p>
    * Implements a mechanism to store UnmeasuredElementRule objects for elements
    * that are present but unmeasured along with any associated data.
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
   public class UERPacket
      extends
      ElementPacket {
      private final UnmeasuredElementRule mUnmeasuredElementRule;

      protected UERPacket(final UnmeasuredElementRule uer) {
         super(uer.getElement());
         mUnmeasuredElementRule = uer;
      }

      @Override
      public String toString() {
         return mUnmeasuredElementRule.toString();
      }

      public UnmeasuredElementRule getRule() {
         return mUnmeasuredElementRule;
      }

      @Override
      public boolean equals(final Object obj) {
         if(obj instanceof UERPacket) {
            final UERPacket o2 = (UERPacket) obj;
            return super.equals(obj) && mUnmeasuredElementRule.equals(o2.mUnmeasuredElementRule)
                  && getElementROIS().equals(o2.getElementROIS());
         } else
            return false;
      }
   }

   static private String toString(final Collection<Element> elms) {
      final StringBuffer sb = new StringBuffer();
      boolean first = true;
      for(final Iterator<Element> i = elms.iterator(); i.hasNext();) {
         final Element elm = i.next();
         if(!first)
            sb.append(i.hasNext() ? ", " : " & ");
         sb.append(elm.toAbbrev());
         first = false;
      }
      return sb.toString();
   }

   /**
    * <p>
    * StandardPacket encapsulates the data associated with a standard material
    * associated with a specific element.
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
   public class StandardPacket
      extends
      ElementPacket {
      /**
       * The composition of the standard
       */
      private final Composition mComposition;
      /**
       * The combined ROIs associated with all elements in mComposition
       */
      private final RegionOfInterestSet mAllROIs;
      /**
       * A map between an mElementROIs ROI and any interfering other element
       * ROIs (includes the element ROI in the set)
       */
      private final Map<RegionOfInterest, Set<RegionOfInterest>> mRequiredRefs = new TreeMap<RegionOfInterest, Set<RegionOfInterest>>();
      /**
       * One of mElementROIs ROI which is preferred for quantification
       */
      private RegionOfInterest mPreferredROI;
      /**
       * A map between an ROI and the associated measured intensity (60 nA.s
       * dose) in the relevant XRayTransition(s).
       */
      private final Map<RegionOfInterest, Map<XRayTransition, Double>> mMeasuredIntensity = new HashMap<RegionOfInterest, Map<XRayTransition, Double>>();

      private final Set<StripPacket> mStripElements;

      /**
       * The user specified desired fractional precision for this element.
       */
      private double mDesiredPrecision = 0.01;

      @Override
      public String toString() {
         return "Standard[" + getElement().toAbbrev() + ", " + mComposition.toString() + "]";
      }

      protected boolean isAnROI(final RegionOfInterest roi) {
         return mRequiredRefs.containsKey(roi);
      }

      /**
       * Constructs a StandardPacket for the specified element using the
       * specified material as a standard to be measured at the specified beam
       * energy.
       *
       * @param elm
       * @param comp Contains elm
       * @param Set&lt;Element&gt; Elements to strip
       */
      private StandardPacket(final Element elm, final Composition comp, final Set<Element> stripElms) {
         super(elm);
         assert comp.containsElement(elm);
         mComposition = comp;
         mStripElements = new TreeSet<StripPacket>();
         for(final Element stripElm : stripElms)
            mStripElements.add(new StripPacket(stripElm));
         final Set<Element> allElms = new TreeSet<Element>(stripElms);
         allElms.addAll(comp.getElementSet());
         mAllROIs = LinearSpectrumFit.createAllElementROIS(allElms, mDetector, getBeamEnergy());
         final Map<Element, RegionOfInterestSet> elmROIS = new TreeMap<Element, RegionOfInterestSet>();
         for(final Element celm : allElms)
            if(!celm.equals(getElement()))
               elmROIS.put(celm, LinearSpectrumFit.createElementROIS(celm, mDetector, getBeamEnergy()));
         for(final RegionOfInterest elmROI : getElementROIS()) {
            final Set<RegionOfInterest> reqRefs = new TreeSet<RegionOfInterest>();
            for(final Element celm : allElms) {
               final RegionOfInterestSet celmROIs = elmROIS.get(celm);
               if(celmROIs == null)
                  continue;
               // Build a list of required references for this elm ROI
               for(final RegionOfInterest allROI : mAllROIs)
                  // Check each all-element ROI to see if it intersects this
                  // compElement ROI
                  if(elmROI.intersects(allROI))
                     for(final RegionOfInterest celmROI : celmROIs)
                        if(celmROI.intersects(allROI)) {
                           reqRefs.add(celmROI);
                           reqRefs.add(elmROI);
                        }
            }
            if(reqRefs.size() > 0)
               mRequiredRefs.put(elmROI, reqRefs);
         }
      }

      /**
       * Estimate a suitable dose
       *
       * @param roi
       * @return double
       */
      private double estimateDose(final RegionOfInterest roi) {
         try {
            return NOMINAL_DOSE / Math2.sqr(mDesiredPrecision * signalToNoiseNominal(roi));
         }
         catch(final EPQException e) {
            return -1.0;
         }
      }

      /**
       * Computes the measured x-ray intensity as would be measured on the
       * detector at 60 nA.s dose on this standard.
       *
       * @param roi
       * @return double x-ray counts at a dose of 60 nA.s
       * @throws EPQException
       */
      private Map<XRayTransition, Double> getMeasuredIntensity(final RegionOfInterest roi)
            throws EPQException {
         assert roi.getElementSet().size() == 1;
         assert mComposition.containsAll(roi.getElementSet());
         if(!mMeasuredIntensity.containsKey(roi))
            mMeasuredIntensity.put(roi, computeIntensity(mComposition, roi));
         assert roi.getAllTransitions().isValid() : roi + ", " + roi.getAllTransitions();
         return mMeasuredIntensity.get(roi);
      }

      public Set<Element> getStripElements() {
         final Set<Element> res = new HashSet<Element>();
         for(final StripPacket sp : mStripElements)
            res.add(sp.getElement());
         return res;
      }

      /**
       * Returns the total integrated characteristic x-ray intensity generated
       * by this standard on the specified ROI.
       *
       * @param roi
       * @return double
       * @throws EPQException
       */
      private double getTotalIntensity(final RegionOfInterest roi)
            throws EPQException {
         double sum = 0.0;
         for(final double i : getMeasuredIntensity(roi).values())
            sum += i;
         return sum;
      }

      /**
       * Computes the approximate uncertainties associated with measuring the
       * specified unknown using this standard, the specified ROI and dose on
       * the unknown. This calculates uncertainties due to MAC, back scatter
       * coefficient, standard dose and unknown dose.
       *
       * @param unknown The approximate composition to be measured
       * @param roi A single element region of interest to be measured
       * @return The mass fraction of the element associated with ROI with
       *         associated uncertainties
       * @throws EPQException
       */
      public UncertainValue2 massFraction(final Composition unknown, final RegionOfInterest roi)
            throws EPQException {
         assert roi.getElementSet().size() == 1;
         final Element elm = roi.getElementSet().first();
         final SpectrumProperties sp = new SpectrumProperties();
         sp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(getBeamEnergy()));
         sp.setDetector(mDetector);
         final Map<XRayTransition, Double> mxd = getMeasuredIntensity(roi);
         final XPP1991 unkXpp = new XPP1991(), stdXpp = new XPP1991();
         final double cStd = mComposition.weightFraction(elm, false);
         final double cUnk = unknown.weightFraction(elm, false);
         final ArrayList<UncertainValue2> mfs = new ArrayList<UncertainValue2>();
         double iStdTot = 0.0, iUnkTot = 0.0;
         for(final Map.Entry<XRayTransition, Double> me : mxd.entrySet()) {
            final double iXrt = me.getValue();
            if(iXrt > 0.0) {
               final XRayTransition xrt = me.getKey();
               unkXpp.initialize(unknown, xrt.getDestination(), sp);
               stdXpp.initialize(mComposition, xrt.getDestination(), sp);
               // Compute uncertainty due to correction...
               final UncertainValue2 mf = XPP1991.massFraction(mComposition, unknown, xrt, sp);
               assert Math.abs(cUnk - mf.doubleValue()) < 1.0e-6;
               final double zaf = unkXpp.computeZAFCorrection(xrt) / stdXpp.computeZAFCorrection(xrt);
               // Compute uncertainty due to intensity
               final double kr = (zaf / cStd) * mf.doubleValue();
               assert Math.abs(kr - new XPP1991().kratio(mComposition, unknown, xrt, sp).doubleValue()) < 0.0001;
               // Compute the count statistics contribution to the k-ratio
               iStdTot += iXrt;
               iUnkTot += iXrt * kr;
               mfs.add(mf);
            }
         }
         final UncertainValue2 res = UncertainValue2.safeWeightedMean(mfs);
         res.assignComponent("I[std," + elm.toAbbrev() + "]", res.doubleValue() / Math.sqrt(iStdTot));
         res.assignComponent("I[unk," + elm.toAbbrev() + "]", res.doubleValue() / Math.sqrt(iUnkTot));
         assert Math.abs(res.doubleValue() - cUnk) < 0.00001;
         return res;
      }

      protected UncertainValue2 kRatio(final Composition unknown, final RegionOfInterest roi, final double stdDose, final double unkDose)
            throws EPQException {
         UncertainValue2 res = UncertainValue2.ZERO;
         final SpectrumProperties sp = new SpectrumProperties();
         sp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(getBeamEnergy()));
         sp.setDetector(mDetector);
         final XPP1991 xpp = new XPP1991();
         final Map<XRayTransition, Double> mxd = getMeasuredIntensity(roi);
         for(final Map.Entry<XRayTransition, Double> me : mxd.entrySet()) {
            final XRayTransition xrt = me.getKey();
            final UncertainValue2 kr = xpp.kratio(mComposition, unknown, xrt, sp);
            final double mi = me.getValue();
            final String es = xrt.getElement().toAbbrev();
            final UncertainValue2 stdI = UncertainValue2.createGaussian((mi * stdDose) / NOMINAL_DOSE, "I[std," + es + "]");
            final UncertainValue2 unkI = UncertainValue2.createGaussian((mi * kr.doubleValue() * unkDose)
                  / NOMINAL_DOSE, "I[unk," + es + "]");
            final UncertainValue2 kr2 = UncertainValue2.divide(unkI, stdI);
            for(final String comp : kr2.getComponentNames())
               kr.assignComponent(comp, kr2.getComponent(comp));
            res = UncertainValue2.add(res, kr);
         }
         return res;
      }

      /**
       * Computes the signal-to-noise at the NOMINAL_DOSE.
       *
       * @param roi
       * @return The signal-to-noise ratio
       * @throws EPQException
       */
      public double signalToNoiseNominal(final RegionOfInterest roi)
            throws EPQException {
         return mComposition.containsElement(roi.getElementSet().first()) ? Math.sqrt(getTotalIntensity(roi)) : 0.0;
      }

      /**
       * Returns a set of other element ROIs which are required to use this
       *
       * @param roi
       * @return Set&lt;RegionOfInterest&gt;
       */
      public Set<RegionOfInterest> getRequiredReferences(final RegionOfInterest roi) {
         assert getElementROIS().contains(roi);
         final Set<RegionOfInterest> res = mRequiredRefs.get(roi);
         return res != null ? res : new TreeSet<RegionOfInterest>();
      }

      protected Composition getComposition() {
         return mComposition;
      }

      public boolean isSuitableAsReference(final RegionOfInterest roi) {
         if(roi.getElementSet().first().equals(getElement()))
            return mRequiredRefs.get(roi) == null;
         else
            return mAllROIs.contains(roi);
      }

      protected void setPreferredROI(final RegionOfInterest roi) {
         assert (roi == null) || getElementROIS().contains(roi);
         if((roi == null) || getElementROIS().contains(roi))
            mPreferredROI = roi;
      }

      protected void clearPreferredROI() {
         mPreferredROI = null;
      }

      public RegionOfInterest getPreferredROI() {
         return mPreferredROI;
      }

      /**
       * @return The beam energy in Joules
       */
      public double getBeamEnergy() {
         return mBeamEnergy;
      }
   }

   private final EDSDetector mDetector;
   private final double mBeamEnergy; // in
                                     // Joules

   private final Set<Element> mElements = new TreeSet<Element>();
   private final Map<Element, StandardPacket> mStandards = new TreeMap<Element, StandardPacket>();
   private final Map<RegionOfInterest, ReferenceMaterial> mUserReferences = new TreeMap<RegionOfInterest, ReferenceMaterial>();
   private final Map<Element, StripPacket> mStrip = new TreeMap<Element, StripPacket>();
   private final Map<Element, UERPacket> mUnmeasuredElementRule = new TreeMap<Element, UERPacket>();
   private final SampleShape mShape = new SampleShape.Bulk();
   private ConductiveCoating mConductiveCoating = null;

   /**
    * Constructs a QuantificationPlanner2 to plan the quantification of a
    * spectrum collected at the specified beam energy on the specified detector.
    *
    * @param det
    * @param defBeamEnergy in Joules
    */
   public QuantificationOutline(final EDSDetector det, final double defBeamEnergy) {
      mDetector = det;
      mBeamEnergy = defBeamEnergy;
   }

   public void add(final Element elm) {
      mElements.add(elm);
   }

   public void addAll(final Collection<Element> elms) {
      mElements.addAll(elms);
   }

   public Set<Element> getElements() {
      return Collections.unmodifiableSet(mElements);
   }

   public Set<Element> getMissingElements() {
      final Set<Element> res = new TreeSet<Element>(mElements);
      res.removeAll(mStandards.keySet());
      res.removeAll(mUnmeasuredElementRule.keySet());
      return res;
   }

   public Map<Element, StandardPacket> getStandards() {
      return Collections.unmodifiableMap(mStandards);
   }

   /**
    * Assign a standard of the specified composition to quantify the specified
    * element.
    *
    * @param elm
    * @param comp
    */
   public void addStandard(final Element elm, final Composition comp, final Set<Element> stripElms) {
      assert comp != null;
      assert elm != null;
      assert comp.containsElement(elm) : comp.getElementSet() + " does not contain " + elm;
      final StandardPacket sp = new StandardPacket(elm, comp, stripElms);
      mStandards.put(elm, sp);
   }

   public Set<Element> getStripElements(final Element elm) {
      return mStandards.get(elm).getStripElements();

   }

   public void addStandard(final Element elm, final Composition comp, final Set<Element> stripElms, final double desiredPrecision) {
      assert comp.containsElement(elm) : comp.getElementSet() + " does not contain " + elm;
      final StandardPacket sp = new StandardPacket(elm, comp, stripElms);
      sp.mDesiredPrecision = desiredPrecision;
      mStandards.put(elm, sp);
   }

   public Composition getStandard(final Element elm) {
      final StandardPacket sp = mStandards.get(elm);
      return sp != null ? sp.getComposition() : null;
   }

   /**
    * Removes the specified element from the quantification.
    *
    * @param elm
    */
   public void removeStandard(final Element elm) {
      mStandards.remove(elm);
   }

   public void clearStandards() {
      mStandards.clear();
   }

   /**
    * Evaluates whether a spectrum of the specified Composition would be a good
    * reference for the specified roi.
    *
    * @param roi A RegionOfInterest for which we want to spec as a reference
    * @param elms A set of elements
    * @return A string detailing reasons why these elements are not suitable or
    *         null if the elements are..
    */
   final String evaluateReference(final RegionOfInterest roi, final Set<Element> elms)
         throws EPQException {
      final StringBuffer sb = new StringBuffer();
      for(final Element otherElm : elms)
         if(!roi.getElementSet().contains(otherElm))
            for(final RegionOfInterest otherRoi : LinearSpectrumFit.createElementROIS(otherElm, mDetector, mBeamEnergy))
               if(otherRoi.intersects(roi))
                  sb.append(roi.toString() + " intersects with " + otherRoi + "\n");
      return sb.length() > 0 ? sb.toString() : null;
   }

   /**
    * Returns a list of ROIs for which the specified composition is suitable as
    * a reference.
    *
    * @param comp
    * @return Set&lt;RegionOfInterest&gt;
    */
   public Set<RegionOfInterest> suitableAsReference(final Composition comp) {
      return suitableAsReference(comp.getElementSet());
   }

   /**
    * Returns a list of ROIs for which the specified collection of elements is
    * suitable as a reference.
    *
    * @param elms
    * @return Set&lt;RegionOfInterest&gt;
    */
   public Set<RegionOfInterest> suitableAsReference(final Collection<Element> elms) {
      final RegionOfInterestSet allRois = LinearSpectrumFit.createAllElementROIS(elms, mDetector, mBeamEnergy);
      final Set<RegionOfInterest> res = new TreeSet<RegionOfInterest>();
      for(final RegionOfInterest roi : allRois)
         res.add(roi);
      return res;
   }

   /**
    * Assigns the specified composition as the reference for any ROIs for which
    * it is suitable.
    *
    * @param comp
    * @return Set&lt;RegionOfInterest&gt; The ROIs for which the reference was
    *         set to comp
    */
   public Set<RegionOfInterest> addReference(final Composition comp) {
      final Set<RegionOfInterest> res = suitableAsReference(comp);
      for(final RegionOfInterest roi : res)
         try {
            addReference(roi, comp);
         }
         catch(final EPQException e) {
            // Should never happen...
            e.printStackTrace();
         }
      return res;
   }

   /**
    * Assign a reference for the specified ROI.
    *
    * @param roi
    * @param elms A collection of Element objects.
    * @throws EPQException
    */
   public void addReference(final RegionOfInterest roi, final Collection<Element> elms)
         throws EPQException {
      final ReferenceMaterial rm = new ReferenceMaterial(elms);
      final RegionOfInterestSet rois = LinearSpectrumFit.createAllElementROIS(elms, mDetector, mBeamEnergy);
      if(!rois.containsThisROI(roi))
         throw new EPQException("The set of elements (" + elms.toString() + ") is not suitable as a reference for this ROI ("
               + roi.toString() + ").");
      mUserReferences.put(roi, rm);
   }

   /**
    * Assign a reference for the specified ROI.
    *
    * @param roi
    * @param comp A Composition suitable as a reference.
    * @throws EPQException
    */
   public void addReference(final RegionOfInterest roi, final Composition comp)
         throws EPQException {
      final ReferenceMaterial rm = new ReferenceMaterial(comp);
      final RegionOfInterestSet rois = LinearSpectrumFit.createAllElementROIS(comp, mDetector, mBeamEnergy);
      if(!rois.containsThisROI(roi))
         throw new EPQException(comp.toString() + " is not suitable as a reference for this ROI (" + roi.toString() + ").");
      mUserReferences.put(roi, rm);
   }

   /**
    * Clear the reference associated with the specified ROI.
    *
    * @param roi
    */
   public void clearReference(final RegionOfInterest roi) {
      mUserReferences.remove(roi);
   }

   /**
    * Specify a rule for estimating the quantity of unmeasured elements. Just
    * because an element is unmeasured doesn't necessarily mean that a reference
    * won't be required. This function clears any standards associated with this
    * element.
    *
    * @param uer
    */
   public void addUnmeasuredElementRule(final UnmeasuredElementRule uer) {
      mStandards.remove(uer.getElement());
      mUnmeasuredElementRule.put(uer.getElement(), new UERPacket(uer));
   }

   /**
    * Clear any unmeasured element rules associated with this element.
    *
    * @param elm
    */
   public void clearUnmeasuredElementRule(final Element elm) {
      mUnmeasuredElementRule.remove(elm);
   }

   /**
    * Returns true if this element is going to be quantified using an unmeasured
    * element rule.
    *
    * @param elm
    * @return boolean
    */
   public boolean isUnmeasuredElementRule(final Element elm) {
      return mUnmeasuredElementRule.containsKey(elm);
   }

   /**
    * Returns a set containing all references required to perform the
    * measurement based on the specified standards and strip elements.
    *
    * @return Set&lt;RegionOfInterest&gt;
    */
   public Set<RegionOfInterest> getAllRequiredReferences(final boolean includeStd) {
      final Set<RegionOfInterest> res = new TreeSet<RegionOfInterest>();
      // Add references required by the standards
      for(final StandardPacket sp : mStandards.values()) {
         for(final RegionOfInterest roi : sp.mRequiredRefs.keySet())
            res.addAll(sp.mRequiredRefs.get(roi));
         if(includeStd)
            for(final RegionOfInterest roi : sp.getElementROIS())
               res.add(roi);
      }
      // Add references as required by the UnmeasuredElementRules and stripped
      // elements
      final Set<Element> otherElms = new TreeSet<Element>();
      otherElms.addAll(mUnmeasuredElementRule.keySet());
      otherElms.addAll(mStrip.keySet());
      if(mConductiveCoating != null)
         otherElms.addAll(mConductiveCoating.getMaterial().getElementSet());
      final Set<Element> allElms = new TreeSet<Element>(getMeasuredElements());
      allElms.addAll(otherElms);
      final Map<Element, RegionOfInterestSet> melmRois = new TreeMap<>();
      for(final Element elm : allElms)
         melmRois.put(elm, LinearSpectrumFit.createElementROIS(elm, mDetector, mBeamEnergy));
      for(final Element otherElm : otherElms)
         for(final RegionOfInterest otherRoi : melmRois.get(otherElm))
            for(final RegionOfInterestSet elmRois : melmRois.values())
               for(final RegionOfInterest elmRoi : elmRois)
                  if((!elmRoi.equals(otherRoi)) && elmRoi.intersects(otherRoi)) {
                     res.add(elmRoi);
                     res.add(otherRoi);
                  }
      return res;
   }

   /**
    * Returns a map of ROI to the ReferenceMaterial which is available to act as
    * a reference for this ROI. Does not return ROIs for which a
    * ReferenceMaterial has not been defined.
    *
    * @return Map&lt;RegionOfInterest, ReferenceMaterial&gt;
    */
   public Map<RegionOfInterest, ReferenceMaterial> getAssignedReferences() {
      final Map<RegionOfInterest, ReferenceMaterial> res = new TreeMap<RegionOfInterest, ReferenceMaterial>();
      for(final StandardPacket sp : mStandards.values())
         for(final RegionOfInterest roi : sp.getElementROIS()) {
            final ReferenceMaterial rm = getReference(roi);
            if(rm != null)
               res.put(roi, rm);
         }
      for(final StripPacket sp : mStrip.values())
         for(final RegionOfInterest roi : sp.getElementROIS()) {
            final ReferenceMaterial rm = getReference(roi);
            if(rm != null)
               res.put(roi, rm);
         }
      for(final UERPacket uer : mUnmeasuredElementRule.values())
         for(final RegionOfInterest roi : uer.getElementROIS()) {
            final ReferenceMaterial rm = getReference(roi);
            if(rm != null)
               res.put(roi, rm);
         }
      return res;
   }

   /**
    * Gets the reference associated with this ROI. If there is a user specified
    * reference then this will be used. If a standard can act as a reference
    * then this standard is suggested so long as the estimated signal-to-noise
    * is adequate.
    *
    * @param roi
    * @return ReferenceMaterial
    */
   public ReferenceMaterial getReference(final RegionOfInterest roi) {
      assert roi.getElementSet().size() == 1 : roi.toString();
      ReferenceMaterial rm = mUserReferences.get(roi);
      if(rm == null) {
         final Element elm = roi.getElementSet().first();
         final StandardPacket std = mStandards.get(elm);
         if((std != null) && std.isSuitableAsReference(roi))
            return new ReferenceMaterial(std.mComposition);
         double bestSN = 100.0;
         for(final Map.Entry<Element, StandardPacket> me : mStandards.entrySet()) {
            final StandardPacket sp = me.getValue();
            if(sp.mComposition.containsElement(elm))
               try {
                  if(sp.isSuitableAsReference(roi)) {
                     final double sn = sp.signalToNoiseNominal(roi);
                     if(sn > bestSN) {
                        rm = new ReferenceMaterial(sp.mComposition);
                        bestSN = sn;
                     }
                  }
               }
               catch(final EPQException e) {
                  // Shouldn't happen but we'll ignore it if it does...
                  e.printStackTrace();
               }
         }
      }
      return rm;
   }

   /**
    * Returns a list of RegionOfInterest objects for which there are references.
    *
    * @return Set&lt;RegionOfInterest&gt;
    */
   public Set<RegionOfInterest> getSatisfiedReferences() {
      final Set<RegionOfInterest> req = getAllRequiredReferences(true);
      final Set<RegionOfInterest> res = new TreeSet<RegionOfInterest>();
      for(final RegionOfInterest roi : req)
         if(getReference(roi) != null)
            res.add(roi);
      return res;
   }

   /**
    * Returns a list of RegionOfInterest objects for which no reference has been
    * specified.
    *
    * @return Set&lt;RegionOfInterest&gt;
    */
   public Set<RegionOfInterest> getUnsatisfiedReferences() {
      final Set<RegionOfInterest> res = getAllRequiredReferences(true);
      res.removeAll(getSatisfiedReferences());
      return res;
   }

   /**
    * Returns a set of all elements to be measured directly (not via an
    * UnmeasuredElementRule or stripped).
    *
    * @return Set&lt;Elemetn&gt;
    */
   public Set<Element> getMeasuredElements() {
      return Collections.unmodifiableSet(mStandards.keySet());
   }

   /**
    * Returns a list of all directly measured and indirectly deduced elements.
    * (The elements which this outline is suitable for characterizing in an
    * unknown.)
    *
    * @return Set&lt;Element&gt;
    */
   public Set<Element> getUnknownElements() {
      final TreeSet<Element> res = new TreeSet<Element>(mStandards.keySet());
      res.addAll(mUnmeasuredElementRule.keySet());
      return res;
   }

   /**
    * Returns a list of elements for which adequate references have been
    * provided. The reference may be the standard in cases in which the standard
    * can act as a clean reference.
    *
    * @param partial Determines whether all ROIs need to be satisfied or whether
    *           just one per element is required.
    * @return Set&lt;Element&gt;
    */
   public Set<Element> getSatisfiedElements(final boolean partial) {
      final Set<Element> res = new TreeSet<Element>();
      // Check standards
      for(final Map.Entry<Element, StandardPacket> me : mStandards.entrySet()) {
         boolean fullySatisfied = true;
         for(final RegionOfInterest roi : me.getValue().getElementROIS()) {
            boolean satisfied = true;
            final Set<RegionOfInterest> req = new TreeSet<RegionOfInterest>(me.getValue().getRequiredReferences(roi));
            for(final RegionOfInterest reqRoi : req)
               if(!mUserReferences.containsKey(reqRoi)) {
                  satisfied = false;
                  break;
               }
            if(partial && satisfied)
               res.add(me.getKey());
            fullySatisfied &= satisfied;
         }
         if(fullySatisfied)
            res.add(me.getKey());
      }
      // Get the references required to strip the elements in mStrip
      for(final StripPacket sp : mStrip.values()) {
         boolean satisfied = true;
         for(final RegionOfInterest roi : sp.getElementROIS())
            if(!mUserReferences.containsKey(roi))
               satisfied = false;
         if(!satisfied)
            res.add(sp.getElement());
      }
      return res;
   }

   /**
    * Returns a list of elements for which adequate references have *not* been
    * provided. The reference may be the standard in cases in which the standard
    * can act as a clean reference.
    *
    * @param partial Determines whether all ROIs need to be satisfied or whether
    *           just one per element is required.
    * @return Set&lt;Element&gt;
    */
   public Set<Element> getUnsatisfiedElements(final boolean partial) {
      final Set<Element> res = new TreeSet<Element>(getMeasuredElements());
      res.removeAll(getSatisfiedElements(partial));
      return res;
   }

   /**
    * Get a set of RegionOfInterest objects listing all the references which are
    * required to quantify the specified element using the material currently
    * assigned as a standard using the specified RegionOfInterest.
    *
    * @param elm
    * @param roi
    * @return Set&lt;RegionOfInterest&gt;
    */
   public Set<RegionOfInterest> getReferenceRequirements(final Element elm, final RegionOfInterest roi) {
      assert roi.getElementSet().size() == 1;
      assert roi.getElementSet().first().equals(elm);
      final StandardPacket sp = mStandards.get(elm);
      assert sp != null : "No standard is defined for this element.";
      return sp != null ? sp.getRequiredReferences(roi) : new TreeSet<RegionOfInterest>();
   }

   public boolean standardCanBeUsedAsReference(final RegionOfInterest roi) {
      assert roi.getElementSet().size() == 1;
      final Element elm = roi.getElementSet().first();
      final StandardPacket sp = mStandards.get(elm);
      return (sp != null) && sp.getElementROIS().contains(roi) && (sp.getRequiredReferences(roi).size() == 0);
   }

   /**
    * Get a RegionOfInterestSet for the specified Element.
    *
    * @param elm An Element (need not have a current standard specified)
    * @return RegionOfInterestSet
    */
   public RegionOfInterestSet getStandardROIS(final Element elm) {
      final StandardPacket sp = mStandards.get(elm);
      return sp != null ? sp.getElementROIS() : LinearSpectrumFit.createElementROIS(elm, mDetector, mBeamEnergy);
   }

   /**
    * Computes the intensity generated on the standard for the specified ROI.
    *
    * @param elm The element for which to find the standard.
    * @param roi Need not be 'elm's ROI but should intersect with one of 'elm's
    *           ROIs
    * @return double x-ray counts
    * @throws EPQException
    */
   public double getStandardIntensity(final Element elm, final RegionOfInterest roi)
         throws EPQException {
      final StandardPacket sp = mStandards.get(elm);
      return sp != null ? sp.getTotalIntensity(roi) : 0.0;
   }

   /**
    * Assign a reference material for the specified RegionOfInterest.
    *
    * @param roi
    * @param rm
    */
   public void assignReference(final RegionOfInterest roi, final ReferenceMaterial rm) {
      assert roi.getElementSet().size() == 1;
      assert rm.getElementSet().contains(roi.getElementSet().first());
   }

   /**
    * Returns a list of stripped elements.
    *
    * @return Set&lt;Element&gt;
    */
   public Set<Element> getStrippedElements() {
      return Collections.unmodifiableSet(mStrip.keySet());
   }

   /**
    * Adds an element to strip. Specify the material to use for the stripping
    * using assignReference(...)
    *
    * @param elm
    */
   public void addElementToStrip(final Element elm) {
      mStandards.remove(elm);
      mStrip.put(elm, new StripPacket(elm));
   }

   public void clearElementToStrip(final Element elm) {
      mStrip.remove(elm);
   }

   /**
    * Clears all stripped elements.
    */
   public void clearElementsToStrip() {
      mStrip.clear();
   }

   /**
    * Has the user specified this element to be stripped (fit but not
    * quantified)?
    *
    * @param elm
    * @return true if the element will be stripped
    */
   public boolean isStripped(final Element elm) {
      return mStrip.containsKey(elm);
   }

   /**
    * Determines whether this QuantificationOutline is sufficiently defined
    * (suitable standards and unmeasured element rules) to quantify all the
    * elements in unkElms
    *
    * @param unkElms
    * @return true if the outline is sufficient; false otherwise.
    */
   public boolean isFullyDefined(final Set<Element> unkElms) {
      final Set<Element> measured = getMeasuredElements();
      for(final Element elm : measured)
         unkElms.remove(elm);
      for(final UnmeasuredElementRule uem : getUnmeasuredElementRules())
         unkElms.remove(uem.getElement());
      return unkElms.size() == 0;
   }

   public void validate(final Collection<Element> unkElmc)
         throws EPQException {
      final Set<Element> unkElms = new TreeSet<Element>(unkElmc);
      final Set<Element> measured = getMeasuredElements();
      for(final Element elm : measured)
         unkElms.remove(elm);
      for(final UnmeasuredElementRule uem : getUnmeasuredElementRules())
         unkElms.remove(uem.getElement());
      if(unkElms.size() != 0)
         throw new EPQException("This quantification plan is missing the elements (" + unkElms.toString()
               + ") necessary to quantify the elements " + unkElmc.toString() + ". ");
   }

   /**
    * Specifies which RegionOfInteest is to be used to perform the
    * quantification.
    *
    * @param roi RegionOfInterest from getStandardROIS(elm)
    */
   public void setPreferredROI(final RegionOfInterest roi) {
      assert roi.getElementSet().size() == 1;
      final StandardPacket sp = mStandards.get(roi.getElementSet().first());
      assert sp != null;
      if(sp != null)
         sp.setPreferredROI(roi);
   }

   /**
    * Clears the preferred ROI associated with the specified measured Element.
    *
    * @param elm
    */
   public void clearPreferredROI(final Element elm) {
      final StandardPacket sp = mStandards.get(elm);
      if(sp != null)
         sp.clearPreferredROI();
   }

   /**
    * Returns the user specified preferredROI for the
    *
    * @param elm Element
    * @return The preferred ROI if one has been specified; null otherwise
    */
   public RegionOfInterest getPreferredROI(final Element elm) {
      final StandardPacket sp = mStandards.get(elm);
      return sp != null ? sp.getPreferredROI() : null;
   }

   public RegionOfInterest getMeasurementROI(final Element elm) {
      final StandardPacket sp = mStandards.get(elm);
      RegionOfInterest res = sp.getPreferredROI();
      if(res == null) {
         final RegionOfInterestSet rois = sp.getElementROIS();
         if(rois.size() == 1)
            res = rois.iterator().next();
      }
      return res;
   }

   /**
    * Remove all preferred ROI settings.
    */
   public void clearPreferredROIs() {
      for(final StandardPacket sp : mStandards.values())
         sp.setPreferredROI(null);
   }

   public String toHTML(File path) {
      final StringWriter sw = new StringWriter();
      final PrintWriter pw = new PrintWriter(sw);
      final NumberFormat nf1 = new HalfUpFormat("0.0");
      // final NumberFormat nf3 = new HalfUpFormat("0.000");
      {
         pw.println("<h3>Conditions</h3>");
         pw.println("<table>");
         pw.println("<tr><th>Item</th><th>Value</th></tr>");
         pw.print("<tr><td>Instrument</td><td>");
         pw.print(TextUtilities.normalizeHTML(mDetector.getOwner().toString()));
         pw.println("</td></tr>");
         pw.print("<tr><td>Detector</td><td>");
         pw.print(TextUtilities.normalizeHTML(mDetector.getName()));
         pw.println("</td></tr>");
         pw.print("<tr><td>Default Beam Energy</td><td>");
         pw.print(TextUtilities.normalizeHTML(nf1.format(FromSI.keV(mBeamEnergy))));
         pw.println(" keV</td></tr>");
         if(mStrip.size() > 0) {
            pw.print("<tr><td>Stripped elements</td><td>");
            boolean first = true;
            for(final Element elm : mStrip.keySet()) {
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
         pw.print("<tr><th>Element</th><th>Material</th><th>Req. References</th><th>Preferred ROI</th><th>Parameters</th></tr>");
         for(final Map.Entry<Element, StandardPacket> me : mStandards.entrySet()) {
            final StandardPacket sp = me.getValue();
            final Composition comp = sp.mComposition;
            pw.print("<tr><td>");
            final Element elm = me.getKey();
            pw.print(elm.toAbbrev());
            pw.print("</td><td>");
            pw.print(comp.toString());
            pw.println("</td><td>");
            boolean firstRoi = true;
            for(final RegionOfInterest roi : sp.getElementROIS()) {
               final Set<RegionOfInterest> rr = sp.getRequiredReferences(roi);
               if(!firstRoi)
                  pw.print("<br/>");
               pw.print(TextUtilities.normalizeHTML(roi.toString()));
               pw.print(":");
               if(rr.size() > 0)
                  for(final RegionOfInterest req : rr) {
                     pw.print("<br/>&nbsp;&nbsp;&nbsp;<i>");
                     pw.print(TextUtilities.normalizeHTML(req.toString()));
                     pw.print("</i>");
                  }
               else
                  pw.println("<br/>&nbsp;&nbsp;&nbsp;<i>None</i>");
               firstRoi = false;
            }
            pw.print("</td><td>");
            if(sp.getPreferredROI() != null)
               pw.print(TextUtilities.normalizeHTML(sp.getPreferredROI().toString()));
            else if(sp.getElementROIS().size() == 1)
               pw.print("N/A");
            else
               pw.print("--None specified--");
            pw.println("</td><td>");
            pw.println(nf1.format(FromSI.keV(sp.getBeamEnergy())));
            pw.println(" keV<br/>");
            pw.println("Precision&nbsp;=&nbsp;" + nf1.format(getDesiredPrecision(elm) * 100.0) + "%");
            pw.println("</td></tr>");

         }
         pw.print("</table>");
      }
      {
         pw.println("<h3>References</h3>");
         pw.println("<table>");
         pw.print("<tr><th>Element/Lines</th><th>Material</th></tr>");
         for(final RegionOfInterest reqRef : getAllRequiredReferences(true)) {
            pw.print("<tr><td>");
            pw.print(TextUtilities.normalizeHTML(reqRef.toString()));
            pw.print("</td><td>");
            final ReferenceMaterial rm = getReference(reqRef);
            if(rm != null)
               pw.print(TextUtilities.normalizeHTML(rm.toString()));
            else
               pw.print("--Missing--");
            pw.println("</td></tr>");
         }
         pw.print("</table>");
      }
      if(mUnmeasuredElementRule.size() > 0) {
         pw.append("<h3>Other elements</h3>\n");
         for(final UERPacket uer : mUnmeasuredElementRule.values()) {
            pw.append("<li>");
            pw.append(uer.mUnmeasuredElementRule.toHTML());
            pw.append("</li>");
         }
      }
      return sw.toString();
   }

   /**
    * Returns a map of ROIs of elm to the references required to use std as a
    * standard for this element.
    *
    * @param elm
    * @param std
    * @return Map&lt;RegionOfInterest, Set&lt;RegionOfInterest&gt;&gt;
    */
   public Map<RegionOfInterest, Set<RegionOfInterest>> getRequiredReferences(final Element elm, final Composition std, final Set<Element> stripElms) {
      final StandardPacket sp = new StandardPacket(elm, std, stripElms);
      final Map<RegionOfInterest, Set<RegionOfInterest>> res = new TreeMap<RegionOfInterest, Set<RegionOfInterest>>();
      for(final RegionOfInterest elmRoi : sp.getElementROIS()) {
         final Set<RegionOfInterest> refs = sp.getRequiredReferences(elmRoi);
         if(refs.size() > 0)
            res.put(elmRoi, refs);
      }
      return res;
   }

   /**
    * Computes the k-ratio
    *
    * @param unk
    * @param stdDose
    * @param unkDose
    * @return Map&lt;RegionOfInterest, UncertainValue2&gt;
    * @throws EPQException
    */
   public Map<RegionOfInterest, UncertainValue2> kRatios(final Composition unk, final double stdDose, final double unkDose)
         throws EPQException {
      final Map<RegionOfInterest, UncertainValue2> res = new TreeMap<RegionOfInterest, UncertainValue2>();
      for(final StandardPacket stdPk : mStandards.values())
         for(final RegionOfInterest roi : stdPk.getElementROIS())
            res.put(roi, stdPk.kRatio(unk, roi, stdDose, unkDose));
      return res;
   }

   /**
    * Calculates the uncertainties associated with a measurement of the
    * specified unknown for all RegionOfInterest associated with all elements in
    * unk. This function assumes a dose of NOMINAL_DOSE for both the standard
    * and the unknown.
    *
    * @param unk The unknown material
    * @return Map&lt;RegionOfInterest, UncertainValue2&gt;
    * @throws EPQException
    */
   public Map<RegionOfInterest, UncertainValue2> massFractions(final Composition unk)
         throws EPQException {
      final Map<RegionOfInterest, UncertainValue2> res = new TreeMap<RegionOfInterest, UncertainValue2>();
      for(final StandardPacket stdPk : mStandards.values())
         for(final RegionOfInterest roi : stdPk.getElementROIS())
            res.put(roi, stdPk.massFraction(unk, roi));
      return res;
   }

   /**
    * Calculates the uncertainties associated with a measurement of the
    * specified unknown using the specified single element RegionOfInterest
    * (which also defines the element being measured). This function assumes a
    * dose of NOMINAL_DOSE for both the standard and the unknown.
    *
    * @param unk The unknown material
    * @param roi The ROI to use to perform the measurement
    * @return UncertainValue2
    * @throws EPQException
    */
   public UncertainValue2 massFraction(final Composition unk, final RegionOfInterest roi)
         throws EPQException {
      assert roi.getElementSet().size() == 1;
      for(final StandardPacket stdPk : mStandards.values())
         if(stdPk.getElementROIS().contains(roi))
            return stdPk.massFraction(unk, roi);
      assert false;
      return null;
   }

   /**
    * Based on the suggested map of potential reference materials by element,
    * create and apply a list of suggested references to those ROIs which are
    * currently unsatisfied.
    *
    * @param suggestions
    */
   public void applySuggestedReferences(final Map<Element, List<Composition>> suggestions) {
      final Map<RegionOfInterest, Composition> tmp = suggestReferences(getUnsatisfiedReferences(), suggestions);
      for(final Map.Entry<RegionOfInterest, Composition> me : tmp.entrySet())
         try {
            addReference(me.getKey(), me.getValue());
         }
         catch(final EPQException e) {
            // Ignore it...
            e.printStackTrace();
         }
   }

   /**
    * Using the suggested references in defRefs, suggest references for all
    * unsatisfied ROIs. For each element, this function takes a list of
    * potential materials. Not all materials are suitable as references for all
    * an element's ROIs. For each ROI in rois, the function searches for the
    * first material that is usable as a reference for the ROI.
    *
    * @param rois A set of RegionOfInterest objects for which suitable materials
    *           need to be identified as references
    * @param defRefs A Map&lt;Element,List&lt;Composition&gt;&gt; of elements
    *           and the suggested materials to use as references for them.
    * @return Map&lt;RegionOfInterest, Composition&gt; A map of RegionOfInterest
    *         objects and the materials which are suitable as references for
    *         them.
    */
   public Map<RegionOfInterest, Composition> suggestReferences(final Set<RegionOfInterest> rois, final Map<Element, List<Composition>> defRefs) {
      final Map<RegionOfInterest, Composition> res = new TreeMap<RegionOfInterest, Composition>();
      for(final RegionOfInterest roi : rois) {
         final Element elm = roi.getElementSet().first();
         final List<Composition> sugg = defRefs.get(elm);
         if(sugg != null)
            for(final Composition comp : sugg)
               if(suitableAsReference(comp).contains(roi)) {
                  res.put(roi, comp);
                  break;
               }
      }
      return res;
   }

   public EDSDetector getDetector() {
      return mDetector;
   }

   /**
    * Beam energy in Joules
    *
    * @return in Joules
    */
   public double getBeamEnergy() {
      return mBeamEnergy;
   }

   public void clearUnmeasuredElementRules() {
      mUnmeasuredElementRule.clear();
   }

   public List<UnmeasuredElementRule> getUnmeasuredElementRules() {
      final List<UnmeasuredElementRule> res = new ArrayList<UnmeasuredElementRule>();
      for(final UERPacket up : mUnmeasuredElementRule.values())
         res.add(up.mUnmeasuredElementRule);
      return res;
   }

   public UnmeasuredElementRule getUnmeasuredElementRule(final Element elm) {
      for(final UERPacket up : mUnmeasuredElementRule.values())
         if(up.mUnmeasuredElementRule.getElement().equals(elm))
            return up.mUnmeasuredElementRule;
      return null;
   }

   /**
    * Computes the measured x-ray intensity that would be measured for this
    * standard on the detector at 60 nA.s dose.
    *
    * @param roi
    * @return The measured x-ray intensity in counts for the specified ROI.
    * @throws EPQException
    */
   protected Map<XRayTransition, Double> computeIntensity(final Composition comp, final RegionOfInterest roi)
         throws EPQException {
      assert roi.getElementSet().size() == 1;
      assert comp.containsAll(roi.getElementSet());
      final Set<AtomicShell> shells = new TreeSet<AtomicShell>();
      final SpectrumProperties sp = new SpectrumProperties();
      sp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(getBeamEnergy()));
      sp.setNumericProperty(SpectrumProperties.FaradayBegin, 1.0);
      sp.setNumericProperty(SpectrumProperties.LiveTime, NOMINAL_DOSE);
      sp.setDetector(mDetector);
      for(final XRayTransition xrt : roi.getAllTransitions())
         shells.add(xrt.getDestination());
      final SpectrumSimulator ss = new SpectrumSimulator.BasicSpectrumSimulator();
      final Map<XRayTransition, Double> res = new TreeMap<XRayTransition, Double>();
      final Map<XRayTransition, Double> tmp = ss.measuredIntensities(comp, sp, shells);
      // tmp contains all transitions to shells, trim out those we requested via
      // roi
      final XRayTransitionSet xrts = roi.getXRayTransitionSet(roi.getElementSet().first());
      for(final Map.Entry<XRayTransition, Double> me : tmp.entrySet())
         if(xrts.contains(me.getKey()))
            res.put(me.getKey(), me.getValue());
      return res;
   }

   public double computeTotalIntensity(final Composition comp, final RegionOfInterest roi)
         throws EPQException {
      double res = 0.0;
      if(comp.containsAll(roi.getElementSet()))
         for(final Double i : computeIntensity(comp, roi).values())
            res += i;
      return res;
   }

   /**
    * Computes the relative ZAF correction between the specified composition and
    * the standard for the specified x-ray transition. Includes the uncertainty
    * terms due to the backscatter and mass absorption corrections.
    *
    * @param comp
    * @param xrts
    * @return UncertainValue2
    * @throws EPQException
    */

   final public double[] zaf(final Composition comp, final XRayTransitionSet xrts, final SpectrumProperties sp)
         throws EPQException {
      CorrectionAlgorithm ca = (CorrectionAlgorithm) CorrectionAlgorithm.NullCorrection.getAlgorithm(CorrectionAlgorithm.class);
      if(ca == null)
         ca = AlgorithmUser.getDefaultCorrectionAlgorithm();
      assert ca != null;
      final Composition standard = mStandards.get(xrts.getElement()).getComposition();
      double[] res = new double[4];
      if(standard != null) {
         sp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(mBeamEnergy));
         sp.setDetector(mDetector);
         double sum = 0.0;
         for(final XRayTransition xrt : xrts) {
            final double w = xrt.getWeight(XRayTransition.NormalizeFamily);
            sum += w;
            final double[] tmp = ca.relativeZAF(comp, xrt, sp, standard);
            assert tmp.length == res.length;
            for(int i = 0; i < res.length; ++i)
               res[i] += w * tmp[i];
         }
         res = Math2.divide(res, sum);
      } else
         Arrays.fill(res, Double.NaN);
      return res;
   }

   public final static String ETA_COMPONENT = XPP1991.UNCERTAINTY_ETA;
   public final static String MU_COMPONENT = XPP1991.UNCERTAINTY_MAC;

   /**
    * Computes the relative ZAF correction between the specified composition and
    * the standard for the specified x-ray transition. Includes the uncertainty
    * terms due to the backscatter and mass absorption corrections.
    *
    * @param comp
    * @param xrt
    * @param sp
    * @return UncertainValue2
    * @throws EPQException
    */
   final private UncertainValue2 uZaf(final Composition comp, final XRayTransition xrt, final SpectrumProperties sp)
         throws EPQException {
      final CorrectionAlgorithm ca = (CorrectionAlgorithm) CorrectionAlgorithm.NullCorrection.getAlgorithm(CorrectionAlgorithm.class);
      assert ca != null;
      final Composition standard = mStandards.get(xrt.getElement()).getComposition();
      final UncertainValue2 res = new UncertainValue2(ca.relativeZAF(comp, xrt, sp, standard)[3]);
      if(ca instanceof XPP1991) {
         final XPP1991 stdXpp = new XPP1991(), unkXpp = new XPP1991();
         stdXpp.initialize(standard, xrt.getDestination(), sp);
         unkXpp.initialize(comp, xrt.getDestination(), sp);
         res.assignComponent(ETA_COMPONENT, XPP1991.uEta(stdXpp, unkXpp, xrt));
         res.assignComponent(MU_COMPONENT, XPP1991.uMAC(stdXpp, unkXpp, xrt));
      }
      return res;
   }

   /**
    * Computes the relative ZAF correction between the specified composition and
    * the standard for the specified x-ray transition. Includes the uncertainty
    * terms due to the backscatter and mass absorption corrections.
    *
    * @param comp
    * @param xrts
    * @return UncertainValue2
    * @throws EPQException
    */

   final public UncertainValue2 uZaf(final Composition comp, final XRayTransitionSet xrts)
         throws EPQException {
      final SpectrumProperties sp = new SpectrumProperties();
      sp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(mBeamEnergy));
      sp.setDetector(mDetector);
      UncertainValue2 res = UncertainValue2.ZERO;
      double sum = 0.0;
      for(final XRayTransition xrt : xrts) {
         final double w = xrt.getWeight(XRayTransition.NormalizeFamily);
         sum += w;
         res = UncertainValue2.add(res, UncertainValue2.multiply(w, uZaf(comp, xrt, sp)));
      }
      return UncertainValue2.divide(res, sum);
   }

   /**
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      return Objects.hash(Double.valueOf(mBeamEnergy), mDetector, mStandards, mStrip, mUnmeasuredElementRule, mUserReferences, mConductiveCoating);
   }

   @Override
   public QuantificationOutline clone() {
      final QuantificationOutline res = new QuantificationOutline(mDetector, mBeamEnergy);
      copyTo(res);
      return res;
   }

   /**
    * Replicates everything in this to res (except detector and beam energy
    * which are final)
    *
    * @param res
    */
   protected void copyTo(final QuantificationOutline res) {
      res.mStandards.clear();
      for(final Map.Entry<Element, StandardPacket> me : mStandards.entrySet()) {
         final Element elm = me.getKey();
         final StandardPacket sp = me.getValue();
         res.addStandard(elm, sp.mComposition, sp.getStripElements(), sp.mDesiredPrecision);
         final StandardPacket resSp = res.mStandards.get(elm);
         assert resSp.mAllROIs.equals(sp.mAllROIs);
         assert resSp.mComposition.equals(sp.mComposition);
         resSp.mDesiredPrecision = sp.mDesiredPrecision;
         assert resSp.getElement().equals(sp.getElement());
         assert resSp.getElementROIS().equals(sp.getElementROIS());
         for(final Map.Entry<RegionOfInterest, Map<XRayTransition, Double>> me2 : sp.mMeasuredIntensity.entrySet())
            resSp.mMeasuredIntensity.put(me2.getKey(), me2.getValue());
         resSp.setPreferredROI(sp.getPreferredROI());
         assert resSp.mRequiredRefs.equals(sp.mRequiredRefs);
      }
      res.mStrip.clear();
      for(final StripPacket sp : mStrip.values())
         res.mStrip.put(sp.getElement(), new StripPacket(sp.getElement()));
      res.mUnmeasuredElementRule.clear();
      for(final Map.Entry<Element, UERPacket> me : mUnmeasuredElementRule.entrySet()) {
         final UERPacket resU = res.new UERPacket(me.getValue().mUnmeasuredElementRule);
         res.mUnmeasuredElementRule.put(me.getKey(), resU);
      }
      res.mUserReferences.clear();
      for(final Map.Entry<RegionOfInterest, ReferenceMaterial> me : mUserReferences.entrySet()) {
         final ReferenceMaterial resRm = new ReferenceMaterial(me.getValue());
         res.mUserReferences.put(me.getKey(), resRm);
      }
      assert res.hashCode() == this.hashCode();
      assert res.equals(this);
   }

   /**
    * Specify the desired precision for the measurement of the element 'elm'
    *
    * @param elm
    * @param prec The fractional precision
    */
   public void setDesiredPrecision(final Element elm, final double prec) {
      final StandardPacket sp = mStandards.get(elm);
      if(sp != null)
         sp.mDesiredPrecision = prec;
   }

   /**
    * Get the desired precision for the measurement of the element 'elm'
    *
    * @param elm
    * @return The fractional precision
    */
   public double getDesiredPrecision(final Element elm) {
      final StandardPacket sp = mStandards.get(elm);
      return sp != null ? sp.mDesiredPrecision : Double.NaN;
   }

   /**
    * Estimate a suitable dose for the standard for the specified element. These
    * are likely to be underestimates because they don't include other
    * interfering elements and don't account for uncertainty in the reference
    * (when there is one).
    *
    * @param elm
    * @param roi
    * @return Dose in nA.s
    */
   public double estimateStandardDose(final Element elm, final RegionOfInterest roi) {
      final StandardPacket sp = mStandards.get(elm);
      return sp != null ? sp.estimateDose(roi) : -1;
   }

   /**
    * Set the material for a surface coating such as a conductive carbon
    * coating.
    *
    * @param material
    * @param thickness
    */
   public void setCoating(ConductiveCoating cc) {
      mConductiveCoating = cc;
   }

   /**
    * Specify an amorphous carbon coating.
    *
    * @param thickness
    */
   public void setCarbonCoat(final double thickness) {
      mConductiveCoating = ConductiveCoating.buildAmorphousCarbon(thickness);
   }

   /**
    * Remove the surface coating.
    */
   public void clearCoating() {
      mConductiveCoating = null;
   }

   /**
    * Returns the material which coats the surface (such as a carbon conductive
    * coating.)
    *
    * @return Material
    */
   public ConductiveCoating getCoating() {
      return mConductiveCoating;
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
      if(!(obj instanceof QuantificationOutline))
         return false;
      final QuantificationOutline other = (QuantificationOutline) obj;
      if(Double.doubleToLongBits(mBeamEnergy) != Double.doubleToLongBits(other.mBeamEnergy))
         return false;
      if(!mDetector.equals(other.mDetector))
         return false;
      if(!mStandards.equals(other.mStandards))
         return false;
      if(!mStrip.equals(other.mStrip))
         return false;
      if(!mUnmeasuredElementRule.equals(other.mUnmeasuredElementRule))
         return false;
      if(!mUserReferences.equals(other.mUserReferences))
         return false;
      return true;
   }

   public boolean isStandard(final ReferenceMaterial rm) {
      if(rm.compositionIsAvailable())
         for(final StandardPacket sp : mStandards.values())
            if(sp.mComposition.equals(rm.mComposition))
               return true;
      return false;
   }

   /**
    * Returns the sample shape.
    *
    * @return SampleShape
    */
   public SampleShape getShape() {
      return mShape;
   }
}
