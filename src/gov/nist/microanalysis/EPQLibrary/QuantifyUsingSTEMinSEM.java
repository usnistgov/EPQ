package gov.nist.microanalysis.EPQLibrary;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nist.microanalysis.EPQLibrary.FilterFit.CullingStrategy;
import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.Utility.Pair;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * @author nritchie
 *
 */
public class QuantifyUsingSTEMinSEM {

   private final EDSDetector mDetector;
   private final double mBeamEnergy;
   /**
    * Maps elements into the spectra to be used as standards for this element.
    */
   private final Map<Element, ISpectrumData> mFitSpectra = new HashMap<>();

   // The correction algorithm...
   private STEMinSEMCorrection mCorrection;
   private final Map<Element, Integer> mLayers;
   private Map<Integer, Oxidizer> mOxidizers = new HashMap<>();

   /**
    * A mechanism for removing elements with zero or near zero presence.
    */
   private CullingStrategy mCullingStrategy = new FilterFit.CullByVariance(0.0);

   /**
    * Preferred RoI for quantification
    */
   private Map<Element, RegionOfInterest> mPreferred = new HashMap<>();

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

      public static String toHTML(List<Result> results) {
         StringBuilder sb = new StringBuilder(8192);
         DecimalFormat df1 = new DecimalFormat("0.0");
         DecimalFormat df2 = new DecimalFormat("0.000");
         Set<Element> elms = new HashSet<>();
         for (Result res : results)
            for (Pair<Composition, Double> lyr : res.mLayers)
               elms.addAll(lyr.first.getElementSet());
         List<Element> allElms = new ArrayList<>(elms);
         Collections.sort(allElms);
         sb.append("<p>");
         sb.append("<table>");
         // Header
         sb.append("<tr><th></th><th>Spectrum</th><th>Layer</th><th>Mass-Thickness</th>");
         for (Element elm : allElms)
            sb.append("<th>" + elm.toAbbrev() + "</th>");
         sb.append("</tr>");
         // Units
         sb.append("<tr><td></td><td></td><td></td><td>(&mu;g/cm<sup>2</sup>)</td>");
         for (Element elm : allElms)
            sb.append("<td>((g " + elm.toAbbrev() + ")/g)</td>");
         sb.append("</tr>");
         int spectrum = 1;
         for (Result res : results) {
            int layer = 1;
            for (Pair<Composition, Double> lyr : res.mLayers) {
               sb.append("<tr>");
               sb.append("<td>" + spectrum + "</td>");
               sb.append("<td>" + res.mSpectrum.toString() + "</td>");
               sb.append("<td>" + layer + "</td>");
               sb.append("<td>" + df1.format(lyr.second * 1.0e5) + "</td>");
               for (Element elm : allElms)
                  sb.append("<td>" + df2.format(lyr.first.weightFraction(elm, false)) + "</td>"); // mass-fraction
               sb.append("</tr>");
               ++layer;
            }
            ++spectrum;
         }
         sb.append("</table>");
         sb.append("<p>");
         return sb.toString();
      }
      
      public String toString() {
         StringBuilder sb = new StringBuilder();
         DecimalFormat df1 = new DecimalFormat("0.0");
         int layer=1;
         for (Pair<Composition, Double> lyr : mLayers) {
            if(layer>1)
               sb.append(", ");
            sb.append("Layer("+layer+", ");
            sb.append(lyr.first.descriptiveString(false));
            sb.append(", ρt="+df1.format(lyr.second * 1.0e5)+" µg/cm²)");
            ++layer;
         }
         return sb.toString();
      }
      
      
   }
   
   

   public QuantifyUsingSTEMinSEM(EDSDetector det, double beamEnergy) throws EPQException {
      mDetector = det;
      mBeamEnergy = beamEnergy;
      SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(beamEnergy));
      double takeOff = det.getDetectorProperties().getProperties().getNumericWithDefault(SpectrumProperties.Elevation, -1.0);
      assert takeOff != -1.0;
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, takeOff);
      props.setObjectProperty(SpectrumProperties.Detector, det);
      mCorrection = new STEMinSEMCorrection(props);
      mLayers = new HashMap<>();
   }

   private ISpectrumData preProcessSpectrum(final ISpectrumData spec) {
      return SpectrumUtils.applyZeroPeakDiscriminator(SpectrumUtils.applyEDSDetector(mDetector, spec));
   }

   public Set<Element> getMeasuredElements() {
      return mCorrection.getElements();
   }

   public Map<Element, ISpectrumData> getFitSpectra() {
      return new HashMap<>(mFitSpectra);
   }

   public Map<Element, ISpectrumData> getStandardSpectra() {
      Map<Element, ISpectrumData> res = new HashMap<>();
      for (Map.Entry<Element, ISpectrumData> me : mFitSpectra.entrySet())
         if (!isStripped(me.getKey()))
            res.put(me.getKey(), me.getValue());
      return res;
   }

   public void addStandard(Element elm, Composition comp, ISpectrumData spec) throws EPQException {
      spec.getProperties().setCompositionProperty(SpectrumProperties.StandardComposition, comp);
      mFitSpectra.put(elm, spec);
      assert comp.containsElement(elm);
      mCorrection.addStandard(elm, comp);
      mLayers.put(elm, Integer.valueOf(1));
   }

   protected FilterFit buildFF() throws EPQException {
      final FilterFit res = new FilterFit(mDetector, mBeamEnergy);
      for (Map.Entry<Element, ISpectrumData> me : mFitSpectra.entrySet())
         res.addReference(me.getKey(), me.getValue());
      return res;
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
      mLayers.clear();
      mLayers.putAll(layers);
   }

   public void setLayer(Element elm, int layer) {
      mLayers.put(elm, Integer.valueOf(layer));
   }

   public boolean isStripped(Element elm) {
      return mLayers.get(elm) <= 0;
   }

   public int getLayer(Element elm) {
      return mLayers.get(elm);
   }

   public Map<Element, Integer> getLayers() {
      Map<Element, Integer> res = new HashMap<>();
      for (Map.Entry<Element, Integer> me : mLayers.entrySet())
         if (me.getValue() > 0)
            res.put(me.getKey(), me.getValue());
      return res;
   }

   public Set<Element> getStripped() {
      Set<Element> res = new HashSet<>();
      for (Map.Entry<Element, Integer> me : mLayers.entrySet())
         if (me.getValue() <= 0)
            res.add(me.getKey());
      return res;
   }

   public Set<Element> getMeasured() {
      Set<Element> res = new HashSet<>();
      for (Map.Entry<Element, Integer> me : mLayers.entrySet())
         if (me.getValue() > 0)
            res.add(me.getKey());
      return res;
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
               if (((best == null) || (sc > bestSc)) && (ee < 0.8 * mBeamEnergy)) {
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
      final Set<XRayTransitionSet> user = new HashSet<>();
      for (RegionOfInterest pref : mPreferred.values())
         user.add(pref.getAllTransitions());
      if (!user.isEmpty())
         strat = new FilterFit.DontCull(strat, user);
      final FilterFit ff = buildFF();
      ff.setCullingStrategy(strat);
      ff.setStripUnlikely(false);
      Set<Element> stripped = getStripped();
      final KRatioSet best = pickBest(ff.getKRatios(spec).strip(stripped));
      final ArrayList<Pair<Composition, Double>> layers = mCorrection.multiLayer(best, getLayers(), this.mOxidizers);
      final ISpectrumData residual = SpectrumUtils.applyEDSDetector(mDetector, ff.getResidualSpectrum(spec, ff.getElements()));
      return new Result(unk, residual, layers, best);
   }
   
   public String toHTML() {
      StringBuilder sb = new StringBuilder();
      sb.append("<h2>Quantify STEM-in-SEM Thin Film Spectra</h2>\n");
      sb.append("<p>Quantify unsupported thin-film spectra using bulk standards and a &phi;(&rho;z)-based correction algorithm.<p>\n");
      sb.append("<p>\n");
      sb.append("<h3>Instrument and Detector</h3>\n");
      // Instrument parameters
      sb.append("<table>\n");
      sb.append("  <td>Instrument</td><td>"+mDetector.getOwner()+"</td></tr>\n");
      sb.append("  <td>Instrument</td><td>"+mDetector+"</td></tr>\n");
      sb.append("  <td>Beam Energy</td><td>"+FromSI.keV(mBeamEnergy)+" keV</td></tr>\n");
      sb.append("</table>\n");
      
      sb.append("<h3>Standards</h3>\n");
      sb.append("<table>");
      sb.append("  <tr><th>Element</th><th>Spectrum</th><th>Layer</th><th>Quantified Line</th></tr>\n");
      for(Element elm : mFitSpectra.keySet()) {
         sb.append("  <tr>");
         sb.append("<td>"+elm.toAbbrev()+"</td>");
         sb.append("<td>"+mFitSpectra.get(elm)+"</td>");
         sb.append("<td>"+(mLayers.get(elm)==0?"Strip": Integer.toString(mLayers.get(elm)))+ "</td>");
         if(mPreferred.containsKey(elm))
            sb.append("<td>"+mPreferred.get(elm)+" (User)</td>");
         else
            sb.append("<td>Default</td>");
         sb.append("</tr>\n");
      }
      sb.append("</table>\n");
      sb.append("</p>\n");
      return sb.toString();
   }

}
