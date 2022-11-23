package gov.nist.microanalysis.EPQLibrary;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.EPQLibrary.SpectrumSimulator.BasicSpectrumSimulator;
import gov.nist.microanalysis.EPQLibrary.StandardsDatabase2.StandardBlock2;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.TextUtilities;

/**
 * <p>
 * Description: A tool for optimizing EPMA measurements. There are a
 * </p>
 * <ol>
 * <li>Beam energy</li>
 * <li>Standards for each element</li>
 * <li>ROIs to use for each standard</li>
 * <li>References for ROI (can be standard)</li>
 * <li>Dose (probe current * live time)</li>
 * </ol>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * <p>
 * <ul>
 * <li>Optional references that are satisfied by standards (ie Fe Kb in K411)
 * </ul>
 * </p>
 * 
 * @author Nicholas
 * @version 1.0
 */
abstract public class EPMAOptimizer {

   /**
    * <p>
    * This class represents an standard and the conditions under which it is
    * measured. It is typically returned by the getOptimizedStandards(...)
    * method. This method produces an set of standards ordered by the score.
    * This highest scoring standards are the best.
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
   public class OptimizedStandard
      implements Comparable<OptimizedStandard> {
      private final Composition mStandard;
      protected double mScore;
      private final RegionOfInterestSet.RegionOfInterest mRoi;
      private final SpectrumProperties mProperties;

      /**
       * Constructs a OptimizedStandard associated with the specified
       * Composition
       * 
       * @param std The Composition of the standard
       * @param region The single element ROI with which the standard is
       *           associated.
       * @param score The quality metric associated with the standard (higher
       *           are better)
       * @param sp Spectrum properties defining the conditions under which the
       *           standard was collected.
       * @throws EPQException
       */
      public OptimizedStandard(Composition std, RegionOfInterestSet.RegionOfInterest region, double score, SpectrumProperties sp)
            throws EPQException {
         super();
         assert region.getElementSet().size() == 1;
         mStandard = std;
         mScore = score;
         mRoi = region;
         mProperties = sp;
      }

      /**
       * A score which is used to rank standards according to the suitability
       * for measurements.
       * 
       * @return double
       */
      public double getScore() {
         return mScore;
      }

      /**
       * The composition of the standard.
       * 
       * @return Composition
       */
      public Composition getComposition() {
         return mStandard;
      }

      public String toolTipText() {
         final StringBuffer sb = new StringBuffer(2048);
         sb.append("<HTML>");
         sb.append("<h2>" + mStandard.getName() + "</h2>");
         sb.append("<table><tr><th>Element</th><th>Mass<br>Fraction</th><th>Atomic<br>Fraction</th></tr>");
         final HalfUpFormat nf = new HalfUpFormat("0.0000");
         for(final Element elm : mStandard.getSortedElements()) {
            sb.append("<tr><td>" + elm.toAbbrev() + "</td>");
            sb.append("<td>" + nf.format(mStandard.weightFraction(elm, false)) + "</td>");
            sb.append("<td>" + nf.format(mStandard.atomicPercent(elm)) + "</td></tr>");
         }
         sb.append("</table>");
         return sb.toString();
      }

      public Element getElement() {
         assert mRoi.getElementSet().size() == 1;
         return mRoi.getElementSet().first();
      }

      /**
       * The single element region-of-interest with which this standard is
       * associated.
       * 
       * @return RegionOfInterest
       */
      public RegionOfInterestSet.RegionOfInterest getROI() {
         assert mRoi.getElementSet().size() == 1;
         return mRoi;
      }

      /**
       * Returns the full ROI associated with all elements which intersect with
       * the single element ROI (getROI()).
       * 
       * @return RegionOfInterest
       */
      public RegionOfInterest getAllElementROI() {
         for(final RegionOfInterest roi : getAllElementROIS())
            if(roi.fullyContains(mRoi))
               return roi;
         assert false;
         return null;
      }

      public List<RegionOfInterest> getElementROIS(Element elm) {
         final RegionOfInterestSet rois = LinearSpectrumFit.createElementROIS(elm, mDetector, getBeamEnergy());
         final List<RegionOfInterest> res = new ArrayList<RegionOfInterestSet.RegionOfInterest>();
         final RegionOfInterest allElmRoi = getAllElementROI();
         for(final RegionOfInterest roi : rois)
            if(roi.intersects(allElmRoi))
               res.add(roi);
         return res;
      }

      public RegionOfInterestSet getAllElementROIS() {
         return LinearSpectrumFit.createAllElementROIS(mStandard, mDetector, getBeamEnergy());
      }

      public SpectrumProperties getProperties() {
         return mProperties;
      }

      /**
       * The beam energy in Joules
       * 
       * @return In Joules or NaN
       */
      public double getBeamEnergy() {
         return ToSI.keV(mProperties.getNumericWithDefault(SpectrumProperties.BeamEnergy, Double.NaN));
      }

      @Override
      public int compareTo(OptimizedStandard o) {
         int res = -Double.compare(mScore, o.mScore);
         if(res == 0)
            res = mRoi.compareTo(o.mRoi);
         if(res == 0)
            res = mStandard.compareTo(o.mStandard);
         if(res == 0)
            res = Double.compare(getBeamEnergy(), o.getBeamEnergy());
         return res;
      }

      @Override
      public String toString() {
         return mStandard.toString() + " [" + mRoi.getElementSet().first().toAbbrev() + ", ROI = " + mRoi + ", E0 = "
               + mProperties.getTextWithDefault(SpectrumProperties.BeamEnergy, "N/A") + ", score = " + mScore + "]";
      }
   }

   public interface MetricFunction {

   }

   /**
    * The estimated composition of the unknown.
    */
   protected final Composition mEstimatedUnknown;
   /**
    * The detector on which the unknown was/will be measured.
    */
   protected final EDSDetector mDetector;
   /**
    * The region of interest set associated with the unknown composition.
    */
   protected final RegionOfInterestSet mUnknownRois;
   /**
    * Associates OptmizedStandard objects with the Element.
    */
   protected final TreeMap<Element, OptimizedStandard> mStandards = new TreeMap<Element, OptimizedStandard>();
   /**
    * User specified unmeasured elements
    */
   protected final TreeSet<Element> mUnmeasuredElements = new TreeSet<Element>();
   /**
    * A map containing references assigned by the user to fulfill either
    * required or optional ROI.
    */
   protected final TreeMap<RegionOfInterest, Composition> mUserReferences = new TreeMap<RegionOfInterestSet.RegionOfInterest, Composition>();

   /**
    * Associates Composition which serve as required references for specific
    * RegionOfInterest objects. These ROIs are required for the measurement.
    */
   protected transient TreeMap<RegionOfInterest, Composition> mReqReferences = new TreeMap<RegionOfInterestSet.RegionOfInterest, Composition>();

   /**
    * Associates Composition which serve as optional references for specific
    * RegionOfInterest objects. These ROIs are not required for the measurement
    * but are useful for the residual.
    */
   protected transient TreeMap<RegionOfInterest, Composition> mOptReferences = new TreeMap<RegionOfInterestSet.RegionOfInterest, Composition>();

   /**
    * Constructs a EPMAOptimizer to optimize the measurement of a mateial with
    * the estimated composition on the specified detector.
    * 
    * @param det An EDSDetector
    * @param estComp The estimated composition of the unknown.
    */
   public EPMAOptimizer(EDSDetector det, Composition estComp) {
      mDetector = det;
      mEstimatedUnknown = estComp;
      mUnknownRois = LinearSpectrumFit.createAllElementROIS(estComp, det, det.getOwner().getMaxBeamEnergy());
      updateReferences();
   }

   public EDSDetector getDetector() {
      return mDetector;
   }

   /**
    * Returns a list of the elements for which a standard is required.
    * 
    * @return Set&lt;Element&gt;
    */
   public Set<Element> getRequiredElements() {
      return mEstimatedUnknown.getElementSet();
   }

   /**
    * <h3>Strategy</h3>
    * <ol>
    * <li>Figure out which lines/line families are available for each element in
    * the material
    * <li>For a range of beam energies nominally 5, 10, 15, 20, 25
    * <li>For each element determine a list of potential standards
    * <li>For each standard
    * <ol>
    * <li>Calculate the relative uncertainties for each line/line family
    * <li>For each element pick the best standard and line/line family
    * </ol>
    * <li>Calculate the best total uncertainty
    * <li>Find the best total uncertainty
    * <ol>
    * <li>This corresponds to a beam energy
    * <li>A standard for each element
    * <li>A line/line family for each element
    * </ol>
    * <li>Summarize and report this result
    * </ol>
    */
   /**
    * Returns a sorted List of OptimizedStandard objects for the specified
    * element.
    * 
    * @param elm The Element to get standards for
    * @param sp SpectrumProperties object containing at a minimum the BeamEnergy
    * @return List&lt;OptimizedStandard&gt;
    * @throws EPQException
    */
   abstract public List<OptimizedStandard> getOptimizedStandards(Element elm, SpectrumProperties sp)
         throws EPQException;

   /**
    * Suggests Composition objects which are suitable for use as references for
    * the specified single element RegionOfInterest
    * 
    * @param roi A single element ROI
    * @return A list of Composition objects
    * @throws EPQException
    */
   abstract public ArrayList<Composition> suggestReferences(RegionOfInterest roi)
         throws EPQException;

   /**
    * Returns a sorted List of OptimizedStandard objects for the specified
    * element.
    * 
    * @param elm The Element to get standards for
    * @param beamEnergy In Joules
    * @return List&lt;OptimizedStandard&gt;
    * @throws EPQException
    */
   public List<OptimizedStandard> getOptimizedStandards(Element elm, double beamEnergy)
         throws EPQException {
      final SpectrumProperties sp = new SpectrumProperties();
      sp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(beamEnergy));
      sp.setDetector(mDetector);
      return getOptimizedStandards(elm, sp);
   }

   public Map<XRayTransition, Double> getMeasuredIntensities(Composition comp, double beamEnergy, double probeDose)
         throws EPQException {
      final SpectrumProperties sp = new SpectrumProperties();
      sp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(beamEnergy));
      sp.setNumericProperty(SpectrumProperties.ProbeCurrent, 1.0);
      sp.setNumericProperty(SpectrumProperties.LiveTime, probeDose * 1.0e9);
      sp.setDetector(mDetector);
      final BasicSpectrumSimulator sim = new BasicSpectrumSimulator();
      return sim.measuredIntensities(comp, sp, sim.shellSet(comp, sp));
   }

   /**
    * Returns a sorted List of OptimizedStandard objects for the specified
    * element.
    * 
    * @param elm The Element to get standards for
    * @param beamEnergy In Joules
    * @param dose In amp*s (60.0 * 10^-9 = 6.0e-8 A*s)
    * @return List&lt;OptimizedStandard&gt;
    * @throws EPQException
    */
   public List<OptimizedStandard> getOptimizedStandards(Element elm, double beamEnergy, double dose)
         throws EPQException {
      final double NOMINAL_CURRENT = 1.0e-9;
      final SpectrumProperties sp = new SpectrumProperties();
      sp.setDetector(mDetector);
      sp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(beamEnergy));
      sp.setNumericProperty(SpectrumProperties.ProbeCurrent, 1.0);
      sp.setNumericProperty(SpectrumProperties.ProbeCurrent, 1.0);
      sp.setNumericProperty(SpectrumProperties.LiveTime, dose / NOMINAL_CURRENT);
      sp.setNumericProperty(SpectrumProperties.TakeOffAngle, SpectrumUtils.getTakeOffAngle(mDetector.getProperties()));
      return getOptimizedStandards(elm, sp);
   }

   /**
    * After running getOptimizedStandards(...) to suggest and rank
    * OptimizedStandard objects, the user can select an OptimizedStandard to use
    * to quantify the specified element. This OptimizedStandard is then used as
    * the basis for the reference selection methods.
    * 
    * @param elm
    * @param os
    */
   public void assignStandard(Element elm, OptimizedStandard os)
         throws EPQException {
      if(os != null) {
         if(!os.getComposition().containsElement(elm))
            throw new EPQException("This standard does not contain the required element.");
         final double e0 = ToSI.keV(os.mProperties.getNumericWithDefault(SpectrumProperties.BeamEnergy, 1000.0));
         if(e0 > mDetector.getOwner().getMaxBeamEnergy())
            throw new EPQException("The specified standard energy is higher than the instrument is capable of achieving.");
         mStandards.put(elm, os);
         mUnmeasuredElements.remove(elm);
      } else
         mStandards.remove(elm);
      // Can this standard also be used as a reference?
      updateReferences();
   }

   public boolean isSingleEnergy() {
      double e0 = Double.NaN;
      for(final OptimizedStandard os : mStandards.values()) {
         if(Double.isNaN(e0))
            e0 = os.getBeamEnergy();
         if(e0 != os.getBeamEnergy())
            return false;
      }
      return true;
   }

   public double getBeamEnergy() {
      if(isSingleEnergy())
         return mStandards.firstEntry().getValue().getBeamEnergy();
      else
         return Double.NaN;
   }

   /**
    * Add an element which will not be measured directly (through a standard.)
    * 
    * @param elm
    */
   public void addUnmeasuredElement(Element elm) {
      assert mEstimatedUnknown.containsElement(elm);
      mUnmeasuredElements.add(elm);
      updateReferences();
   }

   public double getMaxBeamEnergy() {
      double max = 0.0;
      for(final OptimizedStandard os : mStandards.values())
         if(os.getBeamEnergy() > max)
            max = os.getBeamEnergy();
      return max;
   }

   private void updateReferences() {
      mReqReferences.clear();
      mOptReferences.clear();
      // Add entries for all references required by the standards
      for(final OptimizedStandard os : mStandards.values()) {
         final RegionOfInterest allElmRoi = os.getAllElementROI();
         if(allElmRoi.equals(os.getROI()))
            mReqReferences.put(allElmRoi, mUserReferences.containsKey(allElmRoi) ? mUserReferences.get(allElmRoi)
                  : os.getComposition());
         else
            for(final Element allElm : allElmRoi.getElementSet())
               for(final RegionOfInterest lroi : os.getElementROIS(allElm))
                  mReqReferences.put(lroi, mUserReferences.containsKey(lroi) ? mUserReferences.get(lroi) : null);
      }
      // Add entries for all references required by the unmeasured elements in
      // the unknown
      final double e0 = getMaxBeamEnergy();
      for(final Element elm : mUnmeasuredElements)
         for(final RegionOfInterest roi : LinearSpectrumFit.createElementROIS(elm, mDetector, e0))
            for(final RegionOfInterest reqRoi : mReqReferences.keySet())
               if(roi.intersects(reqRoi))
                  mReqReferences.put(roi, mUserReferences.containsKey(roi) ? mUserReferences.get(roi) : null);
               else
                  mOptReferences.put(roi, mUserReferences.containsKey(roi) ? mUserReferences.get(roi) : null);
      // Add optional entries for all other lines in the unknown
      final Set<Element> unkElms = mEstimatedUnknown.getElementSet();
      for(final Element unkElm : unkElms)
         if(!mUnmeasuredElements.contains(unkElm))
            for(final RegionOfInterest unkElmRoi : LinearSpectrumFit.createElementROIS(unkElm, mDetector, e0))
               if(!mReqReferences.containsKey(unkElmRoi)) {
                  Composition ref = mUserReferences.containsKey(unkElmRoi) ? mUserReferences.get(unkElmRoi) : null;
                  if((ref == null) && mStandards.containsKey(unkElm)) {
                     assert mStandards.get(unkElm).getComposition().containsElement(unkElm);
                     final RegionOfInterestSet rois = mStandards.get(unkElm).getAllElementROIS();
                     if(rois.contains(unkElmRoi))
                        ref = mStandards.get(unkElm).getComposition();
                  }
                  mOptReferences.put(unkElmRoi, ref);
               }
   }

   public boolean isRequired(RegionOfInterest roi) {
      return mReqReferences.containsKey(roi);
   }

   public boolean isOptional(RegionOfInterest roi) {
      return mOptReferences.containsKey(roi);
   }

   public boolean isAssigned(RegionOfInterest roi) {
      return (mReqReferences.get(roi) != null) || (mOptReferences.get(roi) != null);
   }

   public boolean isStandard(Composition comp) {
      for(final OptimizedStandard os : mStandards.values())
         if(os.getComposition().equals(comp))
            return true;
      return false;
   }

   /**
    * Assign a reference to a RegionOfInterest
    * 
    * @param roi A single element roi associated with an element in comp
    * @param comp The material to use as a reference
    * @throws EPQException
    */
   public void assignReference(RegionOfInterest roi, Composition comp)
         throws EPQException {
      if(comp != Material.Null) {
         if(roi.getElementSet().size() > 1)
            throw new EPQException("The ROI " + roi + " contains more than one element.");
         final Element elm = roi.getElementSet().first();
         if(!comp.getElementSet().contains(elm))
            throw new EPQException(comp.toString() + " does not contain the element " + elm + ".");
         mUserReferences.put(roi, comp);
         updateReferences();
      }
   }

   /**
    * Returns the user assigned reference for the specified RegionOfInterest
    * 
    * @param roi
    * @return The material Composition
    */
   public Composition getAssignedReference(RegionOfInterest roi) {
      return mUserReferences.get(roi);
   }

   /**
    * Returns the RIOS associated with the specified element. For a full
    * residual, you will need a reference for each ROI.
    * 
    * @param elm
    * @return RegionOfInterestSet (getElementSet().size()==1 and
    *         getElementSet().first()==elm)
    */
   public RegionOfInterestSet getElementROIs(Element elm) {
      final RegionOfInterestSet res = LinearSpectrumFit.createElementROIS(elm, mDetector, mDetector.getOwner().getMaxBeamEnergy());
      assert res.getElementSet().size() == 1;
      assert res.getElementSet().first().equals(elm);
      return res;
   }

   public RegionOfInterest getRequiredElementROI(Element elm) {
      final OptimizedStandard std = mStandards.get(elm);
      return std != null ? std.mRoi : null;
   }

   public Map<Element, OptimizedStandard> getAssignedStandards() {
      return Collections.unmodifiableMap(mStandards);
   }

   public OptimizedStandard getStandard(Element elm) {
      return mStandards.get(elm);
   }

   /**
    * A list of material Composition objects which have been assigned as
    * standards.
    * 
    * @return A list containg material {@link Composition} objects.
    */
   public List<Composition> getStandards() {
      final TreeSet<Composition> res = new TreeSet<Composition>();
      for(final OptimizedStandard os : mStandards.values())
         res.add(os.getComposition());
      return Collections.unmodifiableList(new ArrayList<Composition>(res));
   }

   /**
    * A list of all required and optional standards. This list is different from
    * getAssignedReferences() in that it also includes automatically assigned
    * references for standards that can also serve as references.
    * 
    * @return A List of {@link Composition} objects
    */
   public List<Composition> getReferences() {
      final TreeSet<Composition> res = new TreeSet<Composition>();
      for(final Composition comp : mReqReferences.values())
         if(comp != null)
            res.add(comp);
      for(final Composition comp : mOptReferences.values())
         if(comp != null)
            res.add(comp);
      return Collections.unmodifiableList(new ArrayList<Composition>(res));
   }

   /**
    * Returns a list of the references associated with the specified
    * OptimizedStandard.
    * 
    * @param os
    * @return A List of Composition objects
    */
   public List<Composition> getReferences(OptimizedStandard os) {
      final TreeSet<Composition> res = new TreeSet<Composition>();
      for(final Element elm : os.getAllElementROI().getElementSet())
         for(final RegionOfInterest eroi : os.getElementROIS(elm))
            if(mReqReferences.containsKey(eroi))
               res.add(mReqReferences.get(eroi));
      return Collections.unmodifiableList(new ArrayList<Composition>(res));
   }

   /**
    * Returns a list of all ROI objects which should but don't need to have
    * references whether or not a reference has been assigned.
    * 
    * @return A List of RegionOfInterest objects
    */
   public List<RegionOfInterest> getOptionalReferences() {
      final ArrayList<RegionOfInterest> res = new ArrayList<RegionOfInterest>(mOptReferences.keySet());
      return Collections.unmodifiableList(res);
   }

   /**
    * Returns a list of all ROI objects which require references whether or not
    * a reference has been assigned.
    * 
    * @return A List of RegionOfInterest objects
    */
   public List<RegionOfInterest> getRequiredReferences() {
      final ArrayList<RegionOfInterest> res = new ArrayList<RegionOfInterest>(mReqReferences.keySet());
      return Collections.unmodifiableList(res);
   }

   /**
    * Returns a list of all ROI whether or not a reference has been assigned.
    * 
    * @return A List of RegionOfInterest objects
    */
   public List<RegionOfInterest> getAllReferences() {
      final ArrayList<RegionOfInterest> res = new ArrayList<RegionOfInterest>(mReqReferences.keySet());
      res.addAll(mOptReferences.keySet());
      return Collections.unmodifiableList(res);
   }

   /**
    * All Materials assinged as standards, optional or required references.
    * 
    * @return List&lt;Composition&gt;
    */
   public List<Composition> getAllMaterials() {
      final TreeSet<Composition> res = new TreeSet<Composition>(getStandards());
      res.addAll(getReferences());
      return Collections.unmodifiableList(new ArrayList<Composition>(res));
   }

   public Map<RegionOfInterest, Composition> getAssignedRequiredReferences() {
      final TreeMap<RegionOfInterest, Composition> res = new TreeMap<RegionOfInterest, Composition>();
      for(final Map.Entry<RegionOfInterest, Composition> me : mReqReferences.entrySet())
         if(me.getValue() != null)
            res.put(me.getKey(), me.getValue());
      return Collections.unmodifiableMap(res);
   }

   public Map<RegionOfInterest, Composition> getAssignedOptionalReferences() {
      final TreeMap<RegionOfInterest, Composition> res = new TreeMap<RegionOfInterest, Composition>();
      for(final Map.Entry<RegionOfInterest, Composition> me : mOptReferences.entrySet())
         if(me.getValue() != null)
            res.put(me.getKey(), me.getValue());
      return Collections.unmodifiableMap(res);
   }

   public Map<RegionOfInterest, Composition> getUserReferences() {
      return Collections.unmodifiableMap(mUserReferences);
   }

   public List<Element> getMissingStandards() {
      final Set<Element> elms = new TreeSet<Element>(mEstimatedUnknown.getElementSet());
      elms.removeAll(mUnmeasuredElements);
      final List<Element> missing = new ArrayList<Element>();
      for(final Element elm : elms)
         if(!mStandards.containsKey(elm))
            missing.add(elm);
      return Collections.unmodifiableList(missing);
   }

   public List<RegionOfInterest> getMissingOptionalReferences() {
      final ArrayList<RegionOfInterest> res = new ArrayList<RegionOfInterestSet.RegionOfInterest>();
      for(final RegionOfInterest roi : mOptReferences.keySet())
         if(mOptReferences.get(roi) == null)
            res.add(roi);
      return Collections.unmodifiableList(res);
   }

   public List<RegionOfInterest> getMissingRequiredReferences() {
      final ArrayList<RegionOfInterest> res = new ArrayList<RegionOfInterest>();
      for(final Map.Entry<RegionOfInterest, Composition> me : mReqReferences.entrySet())
         if(me.getValue() == null)
            res.add(me.getKey());
      return Collections.unmodifiableList(res);
   }

   /**
    * Is the specified composition suitable for use as a reference for the
    * specified ROI.
    * 
    * @param roi The ROI
    * @param refComp The reference Composition
    * @return true if suitable, false otherwise.
    */
   public boolean isSuitableAsReference(RegionOfInterest roi, Composition refComp) {
      assert roi.getElementSet().size() == 1;
      final Element elm = roi.getElementSet().first();
      if(refComp.containsElement(elm)) {
         final double e0 = ToSI.keV(mStandards.get(elm).mProperties.getNumericWithDefault(SpectrumProperties.BeamEnergy, Double.NaN));
         final RegionOfInterestSet refRois = LinearSpectrumFit.createAllElementROIS(refComp, mDetector, e0);
         for(final RegionOfInterest refRoi : refRois)
            if(refRoi.equals(roi) && (refRoi.getElementSet().size() == 1))
               return true;
      }
      // Missing the required element
      return false;

   }

   final double maxBeamEnergy() {
      double e0 = -1000.0;
      for(final OptimizedStandard os : mStandards.values())
         if(os.getBeamEnergy() > e0)
            e0 = os.getBeamEnergy();
      if(e0 < 0.0)
         e0 = mDetector.getOwner().getMaxBeamEnergy();
      return e0;
   }

   /**
    * Returns the energy of the highest edge among all the standards.
    * 
    * @return double In Joules
    */
   public double getMaxEdgeEnergy() {
      double eeMax = 0.0;
      for(final OptimizedStandard os : mStandards.values()) {
         final XRayTransitionSet xrts = os.mRoi.getXRayTransitionSet(os.mRoi.getElementSet().first());
         final double ee = xrts.getWeighiestTransition().getSource().getEdgeEnergy();
         if(ee > eeMax)
            eeMax = ee;
      }
      return eeMax;
   }

   /**
    * Have all necesary standards and references been assigned?
    * 
    * @return boolean
    */
   public boolean meetsMinimumRequirements() {
      for(final Element elm : mEstimatedUnknown.getElementSet())
         if(!(mStandards.containsKey(elm) || mUnmeasuredElements.contains(elm)))
            return false;
      for(final Map.Entry<RegionOfInterest, Composition> me : mReqReferences.entrySet())
         if(me.getValue() == null)
            return false;
      return true;
   }

   /**
    * Returns the reference associated with the specified ROI or null if no
    * reference is assigned
    * 
    * @param roi The RegionOfInterest
    * @return A {@link Composition} or null
    */
   public Composition getReference(RegionOfInterest roi) {
      Composition comp = null;
      if(mReqReferences.containsKey(roi))
         comp = mReqReferences.get(roi);
      else if(mOptReferences.containsKey(roi))
         comp = mOptReferences.get(roi);
      else if(mUserReferences.containsKey(roi))
         comp = mUserReferences.get(roi);
      return comp;
   }

   /**
    * @return String
    */
   public String toHTML() {
      final StringBuffer res = new StringBuffer();
      // Experimental paramater table
      res.append("<h1>EDS Measurement Strategy</h1>");
      res.append("<table>");
      res.append("<tr><th>Instrument</th><td>" + mDetector.getOwner().toString() + "</td></tr>");
      res.append("<tr><th>Detector</th><td>" + mDetector.getName() + "</td></tr>");
      res.append("<tr><th>Calibration</th><td>" + mDetector.getCalibration().toString() + "</td></tr>");
      res.append("<tr><th>Unknown<br/>Estimated<br/>Composition</th><td>");
      res.append("<table>");
      res.append("<th>Element</th><th>Mass Fraction</th><th>Atomic Fraction</th></tr>");
      final NumberFormat nf4 = new HalfUpFormat("0.0000");
      for(final Element elm : mEstimatedUnknown.getElementSet()) {
         res.append("<th>" + elm.toString() + "</th>");
         res.append("<td>" + nf4.format(mEstimatedUnknown.weightFraction(elm, false)) + "</td>");
         res.append("<td>" + nf4.format(mEstimatedUnknown.atomicPercent(elm)) + "</td></tr>");
      }
      res.append("</table>");
      res.append("</td></tr>");
      res.append("</table>");
      res.append("<h3>Standards</h3>");
      res.append("<table>");
      res.append("<tr><th>Element</th><th>Standard</th><th>Region-of-<br/>Interest</th><th>Full Region-of-<br/>Interest</th><th>Mass<br/>Fraction</th><th>Score</th><th>Beam<br/Energy</th></tr>");
      // Table of standards, line, ZAF corrections, k-ratios
      final NumberFormat nf1 = new HalfUpFormat("0.0");
      for(final Map.Entry<Element, OptimizedStandard> me : mStandards.entrySet()) {
         final Element elm = me.getKey();
         final OptimizedStandard os = me.getValue();
         final Composition comp = os.getComposition();
         res.append("<tr><td>");
         res.append(elm);
         res.append("</td><td>");
         res.append(comp);
         res.append("</td><td>");
         res.append(os.getROI());
         res.append("</td><td>");
         res.append(os.getAllElementROI());
         res.append("</td><td>");
         res.append(nf4.format(comp.weightFraction(elm, false)));
         res.append("</td><td>");
         res.append(nf1.format(os.getScore()));
         res.append("</td><td>");
         res.append(nf1.format(FromSI.keV(os.getBeamEnergy())));
         res.append(" keV</td></tr>");
      }
      res.append("</table>");
      if(mUnmeasuredElements.size() > 0) {
         res.append("<h3>Unmeasured elements</h3>");
         res.append("<table><tr><th>Element</th><th>ROI in unknown</th></tr>");
         for(final Element unmElm : mUnmeasuredElements) {
            res.append("<tr><td>" + unmElm + "</td><td>");
            for(final RegionOfInterest roi : mUnknownRois)
               if(roi.getElementSet().contains(unmElm))
                  res.append(roi.toString() + "<br/>");
            res.append("</td></tr>");
         }
      }
      res.append("</table>");
      // Table of refrence, available lines (optional/required)
      res.append("<h3>References</h3>");
      final TreeMap<RegionOfInterest, Composition> refs = new TreeMap<RegionOfInterestSet.RegionOfInterest, Composition>();
      refs.putAll(mReqReferences);
      refs.putAll(mOptReferences);
      if(refs.size() == 0)
         res.append("<p>No references required</p>");
      else {
         res.append("<table>");
         res.append("<tr><th>Region-of-<br>Interest</th><th>Material</th><th>Required?</th><th>Also standard</th></tr>");
         for(final Map.Entry<RegionOfInterest, Composition> me : refs.entrySet()) {
            final RegionOfInterest roi = me.getKey();
            final Composition comp = me.getValue();
            final boolean opt = !mReqReferences.containsKey(roi);
            boolean alsoStd = false;
            if(comp != null) {
               for(final OptimizedStandard os : mStandards.values())
                  if(os.mStandard.equals(comp)) {
                     alsoStd = true;
                     break;
                  }
               res.append("<tr><td>");
               res.append(roi.toString());
               res.append("</td><td>");
               res.append(comp.getName());
               res.append("</td><td>");
               res.append(opt ? "No" : "Yes");
               res.append("</td><td>");
               res.append(alsoStd ? "Yes" : "No");
               res.append("</td></tr>");
            } else {
               res.append("<tr><td>");
               res.append(roi.toString());
               res.append("</td><td>");
               res.append(opt ? "-" : "<font color=\"Red\">** Missing **</font>");
               res.append("</td><td>");
               res.append(opt ? "No" : "Yes");
               res.append("</td><td>");
               res.append(alsoStd ? "Yes" : "No");
               res.append("</td></tr>");
            }
         }
         res.append("</table>");
         // Table of suggested standard blocks and the associated materials
      }
      return res.toString();
   }

   /**
    * Have all required and optional standards and reference been assigned?
    * 
    * @return boolean
    */
   public boolean allReferencesAssigned() {
      return meetsMinimumRequirements() && (getMissingOptionalReferences().size() == 0);
   }

   /**
    * Based on the elements in the unknown suggest a list of beam energies from
    * low to high to
    * 
    * @return List&lt;Double&gt;
    */
   public List<Double> suggestBeamEnergies(boolean rationalized) {
      List<Double> res = new ArrayList<Double>();
      final double max = mDetector.getOwner().getMaxBeamEnergy();
      final double min = mDetector.getOwner().getMinBeamEnergy();
      final int[] lines = new int[] {
         XRayTransition.MA1,
         XRayTransition.LA1,
         XRayTransition.KA1
      };
      final TreeSet<XRayTransition> xrts = new TreeSet<XRayTransition>(new Comparator<XRayTransition>() {
         @Override
         public int compare(XRayTransition o1, XRayTransition o2) {
            int res = Double.compare(o1.getEdgeEnergy(), o2.getEdgeEnergy());
            if(res == 0)
               res = o1.compareTo(o2);
            return res;
         }
      });
      final List<Element> elms = new ArrayList<Element>(mEstimatedUnknown.getElementSet());
      for(int i = 0; i < elms.size(); ++i)
         for(final int line : lines) {
            final XRayTransition xrt = new XRayTransition(elms.get(i), line);
            if(mDetector.isVisible(xrt, max))
               xrts.add(xrt);
         }
      for(final XRayTransition xrt : xrts) {
         elms.remove(xrt.getElement());
         if(elms.size() == 0) {
            final double ee = xrt.getEdgeEnergy();
            if(((2.0 * ee) >= min) && ((2.0 * ee) <= max))
               res.add(2.0 * ee);
            else if((1.5 * ee) <= max)
               res.add(1.5 * ee);
         }
      }
      if(elms.size() == 0) {
         final double e0 = 2.0 * res.get(res.size() - 1);
         if((e0 >= min) && (e0 <= max))
            res.add(e0);
      }
      if(rationalized) {
         final List<Double> rat = new ArrayList<Double>();
         double last = -Double.MAX_VALUE;
         for(final Double e0 : res) {
            final double tmp = ToSI.keV(5.0 * Math.ceil(FromSI.keV(e0) / 5.0));
            if(tmp > last) {
               rat.add(Double.valueOf(tmp));
               last = tmp;
            }
         }
         res = rat;
      }
      return res;
   }

   public static class SimilarOptimizer
      extends EPMAOptimizer {

      private final StandardsDatabase2 mDatabase;
      private final TreeSet<StandardBlock2> mExclude = null;

      public SimilarOptimizer(EDSDetector det, Composition estComp, StandardsDatabase2 sdb) {
         super(det, estComp);
         mDatabase = sdb;
      }

      @Override
      public List<OptimizedStandard> getOptimizedStandards(Element elm, SpectrumProperties sp)
            throws EPQException {
         final ArrayList<OptimizedStandard> res = new ArrayList<EPMAOptimizer.OptimizedStandard>();
         final List<Composition> comps = mDatabase.findStandards(elm, 0.01, mExclude);
         final double e0 = sp.getNumericProperty(SpectrumProperties.BeamEnergy);
         final RegionOfInterestSet rois = LinearSpectrumFit.createElementROIS(elm, mDetector, ToSI.keV(e0));
         for(final Composition comp : comps)
            for(final RegionOfInterest roi : rois) {
               final double score = (1.0 / Math.max(1.0e-3, comp.difference(mEstimatedUnknown)))
                     * roi.getXRayTransitionSet(elm).getWeighiestTransition().getNormalizedWeight();
               res.add(new OptimizedStandard(comp, roi, score, sp));
            }
         Collections.sort(res);
         return res;
      }

      private class RefComparitor
         implements Comparator<Composition> {
         private final Element mElement;

         public RefComparitor(Element elm) {
            mElement = elm;
         }

         @Override
         public int compare(Composition o1, Composition o2) {
            return -Double.compare(o1.weightFraction(mElement, false), o2.weightFraction(mElement, false));
         }
      }

      @Override
      public ArrayList<Composition> suggestReferences(RegionOfInterest roi)
            throws EPQException {
         if(roi.getElementSet().size() > 1)
            throw new EPQException("suggestReferences(..) requires a single element ROI. Yours = "
                  + roi.getElementSet().toString());
         assert roi.getElementSet().size() == 1;
         final Element elm = roi.getElementSet().first();
         final List<Composition> comps = mDatabase.findStandards(elm, 0.1, mExclude);
         final double e0 = mDetector.getOwner().getMaxBeamEnergy();
         final ArrayList<Composition> res = new ArrayList<Composition>();
         for(final Composition comp : comps) {
            final RegionOfInterestSet allElmRois = LinearSpectrumFit.createAllElementROIS(comp, mDetector, e0);
            for(final RegionOfInterest allElmRoi : allElmRois)
               if(allElmRoi.equals(roi) && (allElmRoi.getElementSet().size() == 1))
                  res.add(comp);
         }
         Collections.sort(res, new RefComparitor(elm));
         return res;
      }

      @Override
      public String toHTML() {
         final StringBuffer sb = new StringBuffer(super.toHTML());
         final List<Composition> comps = getAllMaterials();
         final List<StandardBlock2> blocks = mDatabase.suggestBlocks(comps, mExclude);
         sb.append("<h3>Standard Blocks</h3>");
         sb.append("<table>");
         sb.append("<tr><th>Block</th><th>Contains materials</th></tr>");
         for(final StandardBlock2 block : blocks) {
            sb.append("<tr><td>");
            sb.append(block.toString());
            sb.append("</td><td>");
            sb.append(TextUtilities.toList(block.satisfies(comps)));
            sb.append("</td></tr>");
         }
         sb.append("</table>");
         return sb.toString();
      }
   };

};
