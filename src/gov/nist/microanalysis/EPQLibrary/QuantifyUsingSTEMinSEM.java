package gov.nist.microanalysis.EPQLibrary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nist.microanalysis.EPQLibrary.FilterFit.CullingStrategy;
import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.Utility.Pair;
import gov.nist.microanalysis.Utility.UncertainValue2;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;

/**
 * @author nritchie
 *
 */
public class QuantifyUsingSTEMinSEM {

   private final EDSDetector mDetector;
   private final double mBeamEnergy;
   private final FilterFit mFit;
   // The correction algorithm...
   private STEMinSEMCorrection mCorrection;
   private Map<Element, Integer> mLayers = null;
   private Map<Integer, Oxidizer> mOxidizers = new HashMap<Integer, Oxidizer>();
   
   /**
    * A mechanism for removing elements with zero or near zero presence.
    */
   private CullingStrategy mCullingStrategy = new FilterFit.CullByVariance(0.0);

   /**
    * Preferred RoI for quantification
    */
   private Map<Element, RegionOfInterest> mPreferred = new HashMap<>();
   /**
    * Maps ROIs into the spectra that are used as a reference for the ROI
    */
   private final Map<RegionOfInterest, ISpectrumData> mStdSpectra = new HashMap<RegionOfInterest, ISpectrumData>();

   private final Set<Element> mStripped = new HashSet<>();
   
   
   public class Result {
      final private ISpectrumData mSpectrum;
      final private ISpectrumData mResidual;
      final private List<Pair<Composition, Double>> mLayers;

      private Result(ISpectrumData spec, ISpectrumData resid, List<Pair<Composition, Double>> layers) {
         mSpectrum = spec;
         mResidual = resid;
         mLayers = Collections.unmodifiableList(layers);
      }

      public ISpectrumData getSpectrum() {
         return mSpectrum;
      }

      public ISpectrumData getResidual() {
         return mResidual;
      }

      public List<Pair<Composition, Double>> getLayers() {
         return mLayers;
      }

   };

   public QuantifyUsingSTEMinSEM(EDSDetector det, double beamEnergy) throws EPQException {
      mDetector = det;
      mBeamEnergy = beamEnergy;
      mFit = new FilterFit(mDetector, mBeamEnergy);
      SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(beamEnergy));
      double takeOff = det.getDetectorProperties().getProperties().getNumericWithDefault(SpectrumProperties.TakeOffAngle, -1.0);
      assert takeOff != -1.0;
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, takeOff);
      props.setObjectProperty(SpectrumProperties.Detector, det);
      mCorrection = new STEMinSEMCorrection(props);
   }

   private ISpectrumData preProcessSpectrum(final ISpectrumData spec) {
      return SpectrumUtils.applyZeroPeakDiscriminator(SpectrumUtils.applyEDSDetector(mDetector, spec));
   }

   public Set<Element> getMeasuredElements() {
      return mCorrection.getElements();
   }

   public void addStandard(Element elm, Composition comp, ISpectrumData spec) throws EPQException {
      mFit.addReference(elm, spec);
      assert comp.containsElement(elm);
      mCorrection.addStandard(elm, comp);

   }
   
   public void addStripped(Element elm, ISpectrumData spec) throws EPQException {
      mFit.addReference(elm, spec);
      mStripped.add(elm);
   }

   public boolean isStripped(Element elm) {
      return mStripped.contains(elm);
   }

   public RegionOfInterest getPreferredROI(Element elm) {
      return mPreferred.get(elm);
   }
   
   public double getBeamEnergy() {
      return mBeamEnergy;
   }
   
   public EDSDetector getDetector() {
      return mDetector;
   }
   
   public void setLayers(Map<Element,Integer> layers) {
      mLayers = Collections.unmodifiableMap(layers);
   }

   public void setPreferredROI(RegionOfInterest roi) {
      assert roi.getElementSet().size() == 1;
      mPreferred.put(roi.getElementSet().first(), roi);
   }
   
   public void setOxidizer(int layer, Oxidizer oxid) {
      mOxidizers.put(layer, oxid);
   }

   public KRatioSet pickBest(KRatioSet full) {
      KRatioSet result = new KRatioSet();
      for (Element elm : mCorrection.getElements()) {
         XRayTransitionSet best = null;
         RegionOfInterest roi = mPreferred.getOrDefault(elm, null);
         if (roi != null) {
            XRayTransitionSet xrts = roi.getAllTransitions();
            if (full.getKRatioU(xrts) != UncertainValue2.ZERO)
               best = xrts;
         }
         if (best == null) {
            double bestE = -1.0;
            for (XRayTransitionSet xrts : full.getTransitions(elm)) {
               final XRayTransition w = xrts.getWeighiestTransition();
               try {
                  final double ew = w.getEnergy();
                  if ((best == null) || //
                        ((ew > bestE) && (ew < 0.5 * mBeamEnergy)) || //
                        ((bestE > 0.5 * mBeamEnergy) && (ew < bestE))) {
                     best = xrts;
                     bestE = ew;
                  }
               } catch (EPQException e) {
                  e.printStackTrace();
               }
            }
         }
         if(best!=null)
            result.addKRatio(best, full.getKRatioU(best));
      }
      return result;
   }

   /**
    * Compute the composition using the FilterFit algorithm and the
    * STEMinSEMCorrection algorithm. The unk spectrum is modified on exit by the
    * application of the default properties and the addition of the KRatios and
    * MicroanalyticalComposition properties.
    *
    * @param unk
    * @return Composition
    * @throws EPQException
    */
   public Result compute(final ISpectrumData unk) throws EPQException {
      final ISpectrumData spec = preProcessSpectrum(unk);
      // Fit the unknown using the reference spectra
      FilterFit.setNaiveBackground(false);
      CullingStrategy strat = mCullingStrategy;
      // Don't cull elements for which there is an explicit preferred transition
      final Set<XRayTransitionSet> user = new HashSet<XRayTransitionSet>();
      for (RegionOfInterest pref : mPreferred.values())
         user.add(pref.getAllTransitions());
      if (!user.isEmpty())
         strat = new FilterFit.DontCull(strat, user);
      mFit.setCullingStrategy(strat);
      mFit.setStripUnlikely(false);
      final KRatioSet krsAgainstRefs = mFit.getKRatios(spec);
      for(XRayTransitionSet xrts : krsAgainstRefs.getTransitions())
         if(mStripped.contains(xrts.getElement()))
            krsAgainstRefs.remove(xrts);
      // Pick best krs
      KRatioSet best = pickBest(krsAgainstRefs);
      //
      Map<Element, Composition> stdComps = new HashMap<Element, Composition>();
      for (XRayTransitionSet xrts : best.keySet()) {
         Element elm = xrts.getElement();
         for (Map.Entry<RegionOfInterest, ISpectrumData> me : mStdSpectra.entrySet())
            if (me.getKey().getXRayTransitionSet(elm).equals(xrts)) {
               ISpectrumData isp = me.getValue();
               Composition stdComp = isp.getProperties().getCompositionWithDefault(SpectrumProperties.StandardComposition, null);
               if (stdComp != null) {
                  stdComps.put(elm, stdComp);
                  continue;
               }
            }
         assert stdComps.getOrDefault(elm, null) == null : "No standard found for " + elm.toAbbrev();
      }
      ArrayList<Pair<Composition, Double>> layers = mCorrection.multiLayer(best, this.mLayers, this.mOxidizers);
      final ISpectrumData residual = SpectrumUtils.applyEDSDetector(mDetector, mFit.getResidualSpectrum(spec, mStripped));
      return new Result(unk, residual, layers);
   }

}

