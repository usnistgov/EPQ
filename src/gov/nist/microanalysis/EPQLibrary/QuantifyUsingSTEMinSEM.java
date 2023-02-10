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

   private final Set<Element> mStripped = new HashSet<>();

   public class Result {
      final private ISpectrumData mSpectrum;
      final private ISpectrumData mResidual;
      final private List<Pair<Composition, Double>> mLayers;
      final private KRatioSet mKRatios;

      private Result(ISpectrumData spec, ISpectrumData resid, List<Pair<Composition, Double>> layers, KRatioSet krs) {
         mSpectrum = spec;
         mResidual = resid;
         mLayers = Collections.unmodifiableList(layers);
         mKRatios = krs;
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

      public KRatioSet getKRatios() {
         return mKRatios;
      }

   };

   public QuantifyUsingSTEMinSEM(EDSDetector det, double beamEnergy) throws EPQException {
      mDetector = det;
      mBeamEnergy = beamEnergy;
      mFit = new FilterFit(mDetector, mBeamEnergy);
      SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(beamEnergy));
      double takeOff = det.getDetectorProperties().getProperties().getNumericWithDefault(SpectrumProperties.Elevation, -1.0);
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

   public void setLayers(Map<Element, Integer> layers) {
      mLayers = Collections.unmodifiableMap(layers);
   }

   public void setPreferredROI(RegionOfInterest roi) {
      assert roi.getElementSet().size() == 1;
      mPreferred.put(roi.getElementSet().first(), roi);
   }

   public void setOxidizer(int layer, Oxidizer oxid) {
      mOxidizers.put(layer, oxid);
   }

   public void setOxidizers(Map<Integer, Oxidizer> oxids) {
      mOxidizers.clear();
      mOxidizers.putAll(oxids);
   }

   public KRatioSet pickBest(KRatioSet full, double overvoltage) {
      KRatioSet result = new KRatioSet();
      for (Element elm : mCorrection.getElements()) {
         XRayTransitionSet best = null;
         if (mPreferred.containsKey(elm)) {
            final XRayTransitionSet xrts = mPreferred.getOrDefault(elm, null).getAllTransitions();
            if (full.getKRatioU(xrts) != UncertainValue2.ZERO)
               best = xrts;
         }
         if (best == null) {
            double bestSc = -1.0;
            for (XRayTransitionSet xrts : full.getTransitions(elm)) {
               final XRayTransition xrt = xrts.getWeighiestTransition();
               final double ee = xrt.getEdgeEnergy(), w = xrt.getWeight(XRayTransition.NormalizeFamily);
               // Favor K over L, intense over less intense and overvoltages
               // less than overvoltage
               final double sc = w * ((AtomicShell.MFamily - xrt.getFamily()) + (ee <= overvoltage * mBeamEnergy ? 1.0 : -2.0));
               if (((best == null) || (sc > bestSc)) && (ee < 0.8*mBeamEnergy)) {
                  best = xrts;
                  bestSc = sc;
               }
            }
         }
         if (best != null)
            result.addKRatio(best, full.getKRatioU(best));
      }
      return result;
   }

   public KRatioSet pickBest(KRatioSet full) {
      return pickBest(full, 0.5);
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
      final KRatioSet best = pickBest(mFit.getKRatios(spec).strip(mStripped));
      final ArrayList<Pair<Composition, Double>> layers = mCorrection.multiLayer(best, this.mLayers, this.mOxidizers);
      final ISpectrumData residual = SpectrumUtils.applyEDSDetector(mDetector, mFit.getResidualSpectrum(spec, mStripped));
      return new Result(unk, residual, layers, best);
   }

}
