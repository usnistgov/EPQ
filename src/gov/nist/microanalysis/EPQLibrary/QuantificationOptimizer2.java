package gov.nist.microanalysis.EPQLibrary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import gov.nist.microanalysis.EPQLibrary.CompositionFromKRatios.UnmeasuredElementRule;
import gov.nist.microanalysis.EPQLibrary.QuantificationOutline.ReferenceMaterial;
import gov.nist.microanalysis.EPQLibrary.QuantificationPlan.Acquisition;
import gov.nist.microanalysis.EPQLibrary.QuantifyUsingStandards.Result;
import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.EPQTools.WriteSpectrumAsEMSA1_0;
import gov.nist.microanalysis.Utility.UncertainValue2;

public class QuantificationOptimizer2 extends QuantificationOptimizer {

   private final SpectrumSimulator mSimulator;
   private final Map<Composition, ISpectrumData> mSpectra = new TreeMap<Composition, ISpectrumData>();
   private final Map<Composition, ISpectrumData> mNoisySpectra = new TreeMap<Composition, ISpectrumData>();
   private Result mResult;

   private final double mStandardFactor = 4;
   private final double mReferenceFactor = 2;
   private final double mUnknownFactor = 2;
   private double mMinDose = 10.0;

   static private final double PROBE_DOSE = 1.0e6;
   static private final double NOMINAL_DOSE = 60.0;
   static private final boolean VARIABLE_FF = true;


   public QuantificationOptimizer2(final QuantificationOutline qo, final SpectrumSimulator ss) {
      super(qo);
      mSimulator = ss;
   }

   public double roundUp(double dose) {
      if (dose > mMinDose) {
         final double exp = Math.pow(10.0, Math.ceil(Math.log10(dose)) - 2);
         final double scale = Math.ceil(dose / exp);
         return Math.max(mMinDose, scale * exp);
      } else
         return mMinDose;
   }

   /**
    * Gets a simulated spectrum for the specified material with no Poisson
    * statistical noise.
    * 
    * @param comp
    * @param props
    * @return ISpectrumData
    * @throws EPQException
    */
   private ISpectrumData getSpectrum(final Composition comp, final SpectrumProperties props) throws EPQException {
      if (!mSpectra.containsKey(comp)) {
         mSpectra.put(comp, mSimulator.generateSpectrum(comp, props, true));
         write(mSpectra.get(comp));
      }
      return mSpectra.get(comp);
   }

   /**
    * Gets a simulated spectrum for the specified material with the NOMINAL
    * probe dose and Poisson noise.
    * 
    * @param comp
    * @param props
    * @return ISpectrumData
    * @throws EPQException
    */
   private ISpectrumData getNoisySpectrum(final Composition comp, final SpectrumProperties props) throws EPQException {
      if (!mNoisySpectra.containsKey(comp)) {
         mNoisySpectra.put(comp, getNoisySpectrum(comp, props, NOMINAL_DOSE));
         write(mNoisySpectra.get(comp));
      }
      return mNoisySpectra.get(comp);
   }

   /**
    * Gets a noisy simulated spectrum with the specified dose. Does not cache
    * the result.
    * 
    * @param comp
    * @param props
    * @param dose
    * @return ISpectrumData
    * @throws EPQException
    */
   private ISpectrumData getNoisySpectrum(final Composition comp, final SpectrumProperties props, final double dose) throws EPQException {
      final ISpectrumData spec = getSpectrum(comp, props);
      return SpectrumUtils.addNoiseToSpectrum(spec, dose / SpectrumUtils.getDose(spec.getProperties()));
   }

   private SpectrumProperties makeSpectrumProperties() {
      final SpectrumProperties props = new SpectrumProperties();
      props.setDetector(mOutline.getDetector());
      props.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(mOutline.getBeamEnergy()));
      props.setNumericProperty(SpectrumProperties.ProbeCurrent, 1.0);
      props.setNumericProperty(SpectrumProperties.LiveTime, PROBE_DOSE);
      return props;
   }

   private ISpectrumData write(final ISpectrumData spec) {
      final File f = new File("C:\\Users\\nritchie.NIST\\Desktop\\dump", spec.toString() + ".msa");
      if (f.getParentFile().isDirectory())
         try {
            try (final FileOutputStream fos = new FileOutputStream(f)) {
               try {
                  WriteSpectrumAsEMSA1_0.write(spec, fos, WriteSpectrumAsEMSA1_0.Mode.COMPATIBLE);
               } catch (final EPQException e) {
                  e.printStackTrace();
               }
            }
         } catch (final IOException e) {
            e.printStackTrace();
         }
      return spec;
   }

   private double computeDose(final UncertainValue2 kr, final double prec) {
      return NOMINAL_DOSE * Math.pow(kr.fractionalUncertainty() / prec, 2.0);
   }

   @Override
   public QuantificationPlan compute(final Composition unk) throws EPQException {
      final QuantificationPlan res = new QuantificationPlan(mOutline);
      final Set<Element> unkElms = unk.getElementSet();
      // Verify that this outline is capable of measuring all the elements in
      // the unknown.
      mOutline.validate(unkElms);
      // Add in the measurements of the standard materials
      final SpectrumProperties props = makeSpectrumProperties();
      final FilterFit ffUnk = new FilterFit(mOutline.getDetector(), mOutline.getBeamEnergy(), VARIABLE_FF);
      // Map<Element, Set<RegionOfInterest>> refRoiStds = new TreeMap<Element,
      // Set<RegionOfInterest>>();
      // 1a. Fit the unknown with the requisite references...
      final Map<Element, RegionOfInterest> prefRois = new TreeMap<Element, RegionOfInterest>();
      for (final Element elm : unkElms) {
         if (mOutline.isUnmeasuredElementRule(elm)) {
            for (RegionOfInterest refRoi : mOutline.getAllRequiredReferences(false))
               if (refRoi.getXRayTransitionSet(elm).size() > 0)
                  ffUnk.addReference(refRoi, getSpectrum(mOutline.getReference(refRoi).getComposition(), props));
         } else {
            final Composition std = mOutline.getStandard(elm);
            if (std == null)
               throw new EPQException("No standard has been assigned for the element " + elm + ".");
            assert mOutline.getPreferredROI(elm) != null;
            final RegionOfInterest prefRoi = mOutline.getPreferredROI(elm);
            prefRois.put(elm, prefRoi);
            if (mOutline.standardCanBeUsedAsReference(prefRoi))
               ffUnk.addReference(prefRoi, getSpectrum(std, props));
            final Map<RegionOfInterest, Set<RegionOfInterest>> rr = mOutline.getRequiredReferences(elm, std, Collections.emptySet());
            for (final Map.Entry<RegionOfInterest, Set<RegionOfInterest>> me : rr.entrySet())
               for (final RegionOfInterest roiRef : me.getValue()) {
                  assert roiRef.getElementSet().size() == 1;
                  final Element roiElm = roiRef.getElementSet().first();
                  if (unk.containsElement(roiElm))
                     ffUnk.addReference(roiRef, getSpectrum(mOutline.getReference(roiRef).getComposition(), props));
               }
         }
      }
      final KRatioSet krsUnk = ffUnk.getKRatios(getNoisySpectrum(unk, props));
      // 1b. Determine the doses necessary to get the desired precisions
      // relative to the unknown
      for (final Element elm : unkElms)
         if (!mOutline.isUnmeasuredElementRule(elm)) {
            final RegionOfInterest elmRoi = prefRois.get(elm);
            assert elmRoi != null;
            final UncertainValue2 krUnk = krsUnk.getKRatioU(elmRoi.getXRayTransitionSet(elm));
            final double dose = computeDose(krUnk, mOutline.getDesiredPrecision(elm));
            res.add(unk, roundUp(mUnknownFactor * dose), mOutline.getBeamEnergy());
            System.out.println(unk + " at " + FromSI.keV(mOutline.getBeamEnergy()) + " for " + (mUnknownFactor * dose) + " n�s at " + elmRoi);
            // Check if a reference was required.
            if (!mOutline.standardCanBeUsedAsReference(elmRoi)) {
               // Calculate reference dose
               final Composition refComp = mOutline.getReference(elmRoi).getComposition();
               res.add(refComp, roundUp(mReferenceFactor * dose * krUnk.doubleValue()), mOutline.getBeamEnergy(), elmRoi);
               System.out.println(refComp + " at " + FromSI.keV(mOutline.getBeamEnergy()) + " for " + (mReferenceFactor * dose * krUnk.doubleValue())
                     + " n�s at " + elmRoi + " for Unknown");
            }
         }
      // 1c. Check for references associated with unmeasured elements.
      for (final RegionOfInterest refRoi : mOutline.getAllRequiredReferences(false)) {
         final ReferenceMaterial refMat = mOutline.getReference(refRoi);
         final Composition refComp = refMat.getComposition();
         final Acquisition acq = res.find(refComp);
         if (acq == null) {
            Acquisition unkAcq = res.find(unk);
            final double dose = krsUnk.getKRatio(refRoi.getAllTransitions()) * unkAcq.getDose() / mUnknownFactor;
            res.add(refComp, roundUp(mReferenceFactor * dose), mOutline.getBeamEnergy(), refRoi);
            System.out.println(refComp + " at " + FromSI.keV(mOutline.getBeamEnergy()) + " for " + dose + " n�s at " + refRoi);
         }
      }

      // 2a. Fit the standards with the requisite references
      final Map<Element, KRatioSet> krsStds = new TreeMap<Element, KRatioSet>();
      for (final Element elm : unkElms)
         if (!mOutline.isUnmeasuredElementRule(elm)) {
            final FilterFit ffStd = new FilterFit(mOutline.getDetector(), mOutline.getBeamEnergy(), VARIABLE_FF);
            final Composition std = mOutline.getStandard(elm);
            if (std == null)
               throw new EPQException("No standard has been assigned for the element " + elm + ".");
            final Map<RegionOfInterest, Set<RegionOfInterest>> rr = mOutline.getRequiredReferences(elm, std, Collections.emptySet());
            final RegionOfInterest prefRoi = prefRois.get(elm);
            if (mOutline.standardCanBeUsedAsReference(prefRoi))
               ffStd.addReference(prefRoi, getSpectrum(std, props));
            for (final Map.Entry<RegionOfInterest, Set<RegionOfInterest>> me : rr.entrySet())
               for (final RegionOfInterest refRoi : me.getValue())
                  ffStd.addReference(refRoi, getSpectrum(mOutline.getReference(refRoi).getComposition(), props));
            krsStds.put(elm, ffStd.getKRatios(getNoisySpectrum(std, props)));
         }
      // 2b. Compute the dose for each standard
      for (final Element elm : unkElms)
         if (!mOutline.isUnmeasuredElementRule(elm)) {
            final KRatioSet krs = krsStds.get(elm);
            final RegionOfInterest elmRoi = prefRois.get(elm);
            final double dose = computeDose(krs.getKRatioU(elmRoi.getXRayTransitionSet(elm)), mOutline.getDesiredPrecision(elm));
            res.add(mOutline.getStandard(elm), roundUp(mStandardFactor * dose), mOutline.getBeamEnergy(), elm);
            System.out.println(mOutline.getStandard(elm) + " at " + FromSI.keV(mOutline.getBeamEnergy()) + " for " + (mStandardFactor * dose)
                  + " nA·s at " + elmRoi);
            if (!mOutline.standardCanBeUsedAsReference(elmRoi)) {
               final Set<RegionOfInterest> refReq = mOutline.getReferenceRequirements(elm, elmRoi);
               // For each reference required by this standard determine the
               // dose.
               for (final RegionOfInterest refRoi : refReq) {
                  assert refRoi.getElementSet().size() == 1;
                  final UncertainValue2 krRef = krs.getKRatioU(refRoi.getAllTransitions());
                  final Composition refComp = mOutline.getReference(refRoi).getComposition();
                  res.add(refComp, roundUp(mReferenceFactor * dose * krRef.doubleValue()), mOutline.getBeamEnergy(), refRoi);
                  System.out.println(refComp + " at " + FromSI.keV(mOutline.getBeamEnergy()) + " for "
                        + (mReferenceFactor * dose * krRef.doubleValue()) + " nA·s at " + refRoi + " for " + elmRoi);
               }
            }
         }
      final QuantifyUsingStandards qus = new QuantifyUsingStandards(mOutline.getDetector(), mOutline.getBeamEnergy(), false, true);
      for (final Element elm : mOutline.getMeasuredElements()) {
         final Composition std = mOutline.getStandard(elm);
         assert std != null;
         final Acquisition acq = res.find(std);
         assert acq != null;
         qus.addStandard(elm, std, Collections.emptySet(), getNoisySpectrum(std, props, acq.getDose()));
      }
      for (final RegionOfInterest refRoi : mOutline.getAllRequiredReferences(false)) {
         final ReferenceMaterial refMat = mOutline.getReference(refRoi);
         final Composition refComp = refMat.getComposition();
         final Acquisition acq = res.find(refComp);
         qus.addReference(refRoi, getNoisySpectrum(refComp, props, acq.getDose()), refComp);
      }
      for (final UnmeasuredElementRule uer : mOutline.getUnmeasuredElementRules())
         qus.addUnmeasuredElementRule(uer);
      mResult = qus.compute(getNoisySpectrum(unk, props));
      return res;
   }

   public Result getQuantResult() {
      return mResult;
   }

   /**
    * Gets the current value assigned to minDose
    * 
    * @return Returns the minDose.
    */
   public double getMinDose() {
      return mMinDose;
   }

   /**
    * Sets the value assigned to minDose.
    * 
    * @param minDose
    *           The value to which to set minDose.
    */
   public void setMinDose(final double minDose) {
      mMinDose = Math.max(1.0e-6, minDose);
   }

}
