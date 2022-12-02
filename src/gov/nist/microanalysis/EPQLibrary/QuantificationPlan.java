package gov.nist.microanalysis.EPQLibrary;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nist.microanalysis.EPQLibrary.CompositionFromKRatios.UnmeasuredElementRule;
import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.TextUtilities;

/**
 * <p>
 * A QuantificationPlan details a series of spectrum acquisitions as necessary
 * to accomplish the measurement in a QuantificationOutline.
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
public class QuantificationPlan {

   /**
    * <p>
    * An Acquisition object is a description of an measurement of an EDS
    * spectrum. It includes information about the material, shape, preparation,
    * dose, beam energy. It also contains information about the purpose of the
    * acquisition - what it is a standard for (element) and what it is a
    * reference for (ROI).
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
   static public class Acquisition {
      private final Composition mComposition;
      private double mDose;
      private final double mBeamEnergy; // In Joules
      private final SampleShape mSampleShape;
      private final SamplePreparation mSamplePreparation;
      private final Set<RegionOfInterest> mReferenceFor = new HashSet<RegionOfInterest>();
      private final Set<Element> mStandardFor = new HashSet<Element>();
      private boolean mIsUnknown;

      private Acquisition(final Composition comp, final double dose, final double beamEnergy, final SampleShape ss, final SamplePreparation prep,
            final RegionOfInterest roi) {
         mComposition = comp;
         mDose = dose;
         mBeamEnergy = beamEnergy;
         mSampleShape = ss;
         mSamplePreparation = prep;
         mReferenceFor.add(roi);
         mIsUnknown = false;
      }

      private Acquisition(final Composition comp, final double dose, final double beamEnergy, final SampleShape ss, final SamplePreparation prep) {
         mComposition = comp;
         mDose = dose;
         mBeamEnergy = beamEnergy;
         mSampleShape = ss;
         mSamplePreparation = prep;
         mIsUnknown = true;
      }

      private Acquisition(final Composition comp, final double dose, final double beamEnergy, final SampleShape ss, final SamplePreparation prep,
            final Element elm) {
         mComposition = comp;
         mDose = dose;
         mBeamEnergy = beamEnergy;
         mSampleShape = ss;
         mSamplePreparation = prep;
         mStandardFor.add(elm);
         mIsUnknown = false;
      }

      /**
       * Constructs a Acquisition with a bulk sample and flat polished prep
       * (50.0e-9) as a reference for roi.
       * 
       * @param comp
       * @param dose
       * @param beamEnergy
       * @param roi
       */
      private Acquisition(final Composition comp, final double dose, final double beamEnergy, RegionOfInterest roi) {
         this(comp, dose, beamEnergy, new SampleShape.Bulk(), new SamplePreparation.FlatPolishPreparation(50.0e-9), roi);
      }

      /**
       * Constructs a Acquisition with a bulk sample and flat polished prep
       * (50.0e-9) as a standard for elm
       * 
       * @param comp
       * @param dose
       * @param beamEnergy
       * @param elm
       */
      private Acquisition(final Composition comp, final double dose, final double beamEnergy, Element elm) {
         this(comp, dose, beamEnergy, new SampleShape.Bulk(), new SamplePreparation.FlatPolishPreparation(50.0e-9), elm);
      }

      /**
       * Constructs a Acquisition with a bulk sample and flat polished prep
       * (50.0e-9) as an unknown with estimated composition comp.
       * 
       * @param comp
       * @param dose
       * @param beamEnergy
       */
      private Acquisition(final Composition comp, final double dose, final double beamEnergy) {
         this(comp, dose, beamEnergy, new SampleShape.Bulk(), new SamplePreparation.FlatPolishPreparation(50.0e-9));
      }

      public String getTypeString() {
         final List<String> res = new ArrayList<String>();
         if (isReference())
            res.add("Reference");
         if (isStandard())
            res.add("Standard");
         if (isUnknown())
            res.add("Unknown");
         return TextUtilities.toList(res);
      }

      @Override
      public String toString() {
         return mComposition + " at " + FromSI.keV(mBeamEnergy) + " keV for " + mDose + " nA.s on " + mSampleShape + " " + mSamplePreparation
               + getTypeString();
      }

      public boolean isStandard() {
         return mStandardFor.size() > 0;
      }

      public boolean isReference() {
         return mReferenceFor.size() > 0;
      }

      public boolean isUnknown() {
         return mIsUnknown;
      }

      public Composition getComposition() {
         return mComposition;
      }

      /**
       * The probe dose.
       * 
       * @return in nA.sec
       */
      public double getDose() {
         return mDose;
      }

      public SampleShape getShape() {
         return mSampleShape;
      }

      public SamplePreparation getPreparation() {
         return mSamplePreparation;
      }

      /**
       * @return In Joules
       */
      public double getBeamEnergy() {
         return mBeamEnergy;
      }

      private ISpectrumData simulate(SpectrumSimulator ss, final SpectrumProperties sp) throws EPQException {
         final NumberFormat nf = new HalfUpFormat("0.0");
         sp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(mBeamEnergy));
         sp.setNumericProperty(SpectrumProperties.LiveTime, mDose);
         sp.setNumericProperty(SpectrumProperties.ProbeCurrent, 1.0);
         final ISpectrumData res = SpectrumUtils.addNoiseToSpectrum(ss.generateSpectrum(mComposition, sp, true), 1.0);
         SpectrumUtils.rename(res, "Simulated(" + mComposition.toString() + ", " + nf.format(FromSI.keV(mBeamEnergy)) + " keV, " + nf.format(mDose)
               + " nA\u00B7s, " + getTypeString() + ")");
         return res;
      }

      /**
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         long temp;
         temp = Double.doubleToLongBits(mBeamEnergy);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         result = (prime * result) + ((mComposition == null) ? 0 : mComposition.hashCode());
         temp = Double.doubleToLongBits(mDose);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         result = (prime * result) + Boolean.valueOf(mIsUnknown).hashCode();
         result = (prime * result) + ((mReferenceFor == null) ? 0 : mReferenceFor.hashCode());
         result = (prime * result) + ((mSamplePreparation == null) ? 0 : mSamplePreparation.hashCode());
         result = (prime * result) + ((mSampleShape == null) ? 0 : mSampleShape.hashCode());
         result = (prime * result) + ((mStandardFor == null) ? 0 : mStandardFor.hashCode());
         return result;
      }

      /**
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (!(obj instanceof QuantificationPlan.Acquisition))
            return false;
         final QuantificationPlan.Acquisition other = (QuantificationPlan.Acquisition) obj;
         if (Double.doubleToLongBits(mBeamEnergy) != Double.doubleToLongBits(other.mBeamEnergy))
            return false;
         if (mComposition == null) {
            if (other.mComposition != null)
               return false;
         } else if (!mComposition.equals(other.mComposition))
            return false;
         if (Double.doubleToLongBits(mDose) != Double.doubleToLongBits(other.mDose))
            return false;
         if (mIsUnknown != other.mIsUnknown)
            return false;
         if (mReferenceFor == null) {
            if (other.mReferenceFor != null)
               return false;
         } else if (!mReferenceFor.equals(other.mReferenceFor))
            return false;
         if (mSamplePreparation == null) {
            if (other.mSamplePreparation != null)
               return false;
         } else if (!mSamplePreparation.equals(other.mSamplePreparation))
            return false;
         if (mSampleShape == null) {
            if (other.mSampleShape != null)
               return false;
         } else if (!mSampleShape.equals(other.mSampleShape))
            return false;
         if (mStandardFor == null) {
            if (other.mStandardFor != null)
               return false;
         } else if (!mStandardFor.equals(other.mStandardFor))
            return false;
         return true;
      }
   }

   private final QuantificationOutline mOutline;
   private final ArrayList<QuantificationPlan.Acquisition> mAcquisitions = new ArrayList<QuantificationPlan.Acquisition>();

   protected QuantificationPlan(final QuantificationOutline qo) {
      mOutline = qo;
   }

   /**
    * Merge a series of AcquisitonPlan objects based on the same
    * QuantificationPlan2 into a single all encompassing AcquisitionPlan.
    * 
    * @param plans
    * @return {@link QuantificationPlan}
    * @throws EPQException
    */
   public static QuantificationPlan merge(Collection<QuantificationPlan> plans) throws EPQException {
      QuantificationPlan res = null;
      for (final QuantificationPlan plan : plans)
         res = (res == null ? plan : merge(res, plan));
      return res;
   }

   /**
    * Merge two AcquisitonPlan objects based on the same QuantificationPlan2
    * into a single all encompassing AcquisitionPlan.
    * 
    * @param plan1
    * @param plan2
    * @return {@link QuantificationPlan}
    * @throws EPQException
    */
   public static QuantificationPlan merge(QuantificationPlan plan1, QuantificationPlan plan2) throws EPQException {
      if (plan1.mOutline != plan2.mOutline)
         throw new EPQException("In order to merge acquisition plans, they must be based on the same quantification plan.");
      final QuantificationPlan res = new QuantificationPlan(plan1.mOutline);
      for (final QuantificationPlan.Acquisition acq : plan1.mAcquisitions)
         res.add(acq);
      for (final QuantificationPlan.Acquisition acq : plan2.mAcquisitions)
         res.add(acq);
      return res;
   }

   /**
    * Returns a map of Acquisition objects to ISpectrumData objects for the
    * Acquisitions described in this QuantificationPlan.
    * 
    * @return Map&lt;QuantificationPlan.Acquisition, ISpectrumData&gt;
    * @throws EPQException
    */
   public Map<QuantificationPlan.Acquisition, ISpectrumData> simulate(SpectrumSimulator ss) throws EPQException {
      final SpectrumProperties sp = new SpectrumProperties();
      sp.setDetector(mOutline.getDetector());
      final HashMap<QuantificationPlan.Acquisition, ISpectrumData> res = new HashMap<QuantificationPlan.Acquisition, ISpectrumData>();
      for (final QuantificationPlan.Acquisition acq : mAcquisitions)
         res.put(acq, acq.simulate(ss, sp));
      return res;
   }

   public List<ISpectrumData> simulateUnknown(SpectrumSimulator ss, int duplicates) throws EPQException {
      final SpectrumProperties sp = new SpectrumProperties();
      sp.setDetector(mOutline.getDetector());
      final ArrayList<ISpectrumData> res = new ArrayList<ISpectrumData>();
      for (final QuantificationPlan.Acquisition acq : mAcquisitions)
         if (acq.isUnknown())
            for (int i = 0; i < duplicates; ++i) {
               final ISpectrumData spec = acq.simulate(ss, sp);
               SpectrumUtils.rename(spec, spec.toString() + "[" + Integer.toString(i + 1) + "]");
               res.add(spec);
            }
      return res;
   }

   /**
    * @param specs
    * @return QuantifyUsingStandards.Result
    * @throws EPQException
    */
   public QuantifyUsingStandards buildQuantifyUsingStandards(Map<QuantificationPlan.Acquisition, ISpectrumData> specs) throws EPQException {
      final QuantifyUsingStandards qus = new QuantifyUsingStandards(mOutline.getDetector(), mOutline.getBeamEnergy());
      for (final Map.Entry<QuantificationPlan.Acquisition, ISpectrumData> me : specs.entrySet()) {
         final QuantificationPlan.Acquisition acq = me.getKey();
         for (final Element elm : acq.mStandardFor)
            qus.addStandard(elm, acq.getComposition(), Collections.emptySet(), me.getValue());
         for (final RegionOfInterest roi : acq.mReferenceFor)
            qus.addReference(roi, me.getValue(), acq.getComposition());
      }
      for (final UnmeasuredElementRule uer : mOutline.getUnmeasuredElementRules())
         qus.addUnmeasuredElementRule(uer);
      return qus;
   }

   /**
    * Finds the first Acquisition associated with the specified Composition.
    * 
    * @param comp
    * @return QuantificationPlan.Acquisition
    */
   public QuantificationPlan.Acquisition find(final Composition comp) {
      for (final QuantificationPlan.Acquisition acq : mAcquisitions)
         if (acq.mComposition.equals(comp))
            return acq;
      return null;
   }

   /**
    * Creates a user-friendly HTML description of the QuantificationPlan. This
    * is suitable for use as a recipe detailing the plan.
    * 
    * @param withOutline
    * @return String in HTML
    */
   public String toHTML(File path, boolean withOutline) {
      final NumberFormat nf = new HalfUpFormat("0.0");
      final StringBuffer res = new StringBuffer();
      if (withOutline) {
         res.append("<h2>Quantification Outline</h2>\n");
         res.append("<p>" + mOutline.toHTML(path) + "</p>");
      }
      res.append("<h3>Measurement plan</h3>\n");
      res.append("<p><table>");
      res.append(
            "\t<tr><th>Composition</th><th>Beam Energy<br/>(keV)</th><th>Form</th><th>Preparation</th><th>Dose<br/>(nA&middot;s)</th><th>Type</th></tr>\n");
      for (final QuantificationPlan.Acquisition acq : mAcquisitions) {
         res.append("\t<tr><td>");
         res.append(acq.mComposition.toHTMLTable());
         res.append("</td><td>");
         res.append(FromSI.keV(acq.mBeamEnergy));
         res.append("</td><td>");
         res.append(acq.mSampleShape.toString());
         res.append("</td><td>");
         res.append(acq.mSamplePreparation.toHTML());
         res.append("</td><td>");
         res.append(nf.format(acq.mDose));
         res.append("</td><td>");
         res.append(acq.getTypeString());
         res.append("</td></tr>\n");
      }
      res.append("</table></p>");
      return res.toString();
   }

   /**
    * Adds the specified Acquisition representing a reference to this
    * AcquisitionPlan. If an equivalent or better Acquisition already exists in
    * the AcquisitionPlan, then the Acquisition is not added. Aims to improve
    * the sample preparation or increase the dose of pre-existing Acquisition
    * objects when a better/higher dose one comes along.
    * 
    * @param comp
    *           Composition
    * @param dose
    *           nA.s = nC
    * @param beamEnergy
    *           in Joules
    * @param roi
    *           The ROI for which this acquisition is a reference
    */
   public void add(final Composition comp, final double dose, final double beamEnergy, RegionOfInterest roi) {
      final QuantificationPlan.Acquisition newAcq = new Acquisition(comp, dose, beamEnergy, roi);
      add(newAcq);
   }

   /**
    * Adds the specified Acquisition representing a standard to this
    * AcquisitionPlan. If an equivalent or better Acquisition already exists in
    * the AcquisitionPlan, then the Acquisition is not added. Aims to improve
    * the sample preparation or increase the dose of pre-existing Acquisition
    * objects when a better/higher dose one comes along.
    * 
    * @param comp
    *           Composition
    * @param dose
    *           nA.s = nC
    * @param beamEnergy
    *           in Joules
    * @param elm
    *           The element for which this Acquisition is a standard
    */
   public void add(final Composition comp, final double dose, final double beamEnergy, Element elm) {
      final QuantificationPlan.Acquisition newAcq = new Acquisition(comp, dose, beamEnergy, elm);
      add(newAcq);
   }

   /**
    * Adds the specified Acquisition representing the unknown material to this
    * AcquisitionPlan. If an equivalent or better Acquisition already exists in
    * the AcquisitionPlan, then the Acquisition is not added. Aims to improve
    * the sample preparation or increase the dose of pre-existing Acquisition
    * objects when a better/higher dose one comes along.
    * 
    * @param comp
    *           Composition
    * @param dose
    *           nA.s = nC
    * @param beamEnergy
    *           in Joules
    */
   public void add(final Composition comp, final double dose, final double beamEnergy) {
      final QuantificationPlan.Acquisition newAcq = new Acquisition(comp, dose, beamEnergy);
      add(newAcq);
   }

   /**
    * Adds a new Acquisition to the plan.
    * 
    * @param newAcq
    */
   private void add(QuantificationPlan.Acquisition newAcq) {
      boolean added = false;
      for (final QuantificationPlan.Acquisition acq : mAcquisitions) {
         final boolean sameMat = acq.getComposition().equals(newAcq.mComposition);
         final boolean sameE0 = Math.abs(acq.getBeamEnergy() - newAcq.mBeamEnergy) < ToSI.eV(0.1);
         // boolean sameShape = acq.getShape().equals(newAcq.getShape());
         if (sameMat && sameE0) { // && sameShape) {
            if (newAcq.mDose > acq.mDose)
               acq.mDose = newAcq.mDose;
            acq.mReferenceFor.addAll(newAcq.mReferenceFor);
            acq.mStandardFor.addAll(newAcq.mStandardFor);
            acq.mIsUnknown = newAcq.mIsUnknown;
            added = true;
            break;
         }
      }
      if (!added)
         mAcquisitions.add(newAcq);
   }

   /**
    * Returns a list of the Acquisition objects which implement the
    * QuantificationPlan.
    * 
    * @return List&lt;QuantificationPlan.Acquisition&gt;
    */
   public List<QuantificationPlan.Acquisition> getAcquisitions() {
      return Collections.unmodifiableList(mAcquisitions);
   }

   /**
    * Returns the QuantificationOutline object from which this
    * QuantificationPlan was derived.
    * 
    * @return QuantificationOutline
    */
   public QuantificationOutline getQuantificationOutline() {
      return mOutline;
   }
}