package gov.nist.microanalysis.EPQLibrary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import gov.nist.microanalysis.EPQLibrary.CompositionFromKRatios.UnmeasuredElementRule;
import gov.nist.microanalysis.Utility.Pair;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * @author NWMR
 */
public class STEMinSEMCorrection {

   private final HashMap<Element, Composition> mStandards;
   private final SpectrumProperties mProperties;

   public STEMinSEMCorrection(SpectrumProperties properties, Map<Element, Composition> standards) {
      assert properties.isDefined(SpectrumProperties.BeamEnergy);
      assert properties.isDefined(SpectrumProperties.TakeOffAngle);
      this.mProperties = properties;
      this.mStandards = new HashMap<>(standards);
   }

   public STEMinSEMCorrection(SpectrumProperties properties) {
      this(properties, new HashMap<Element, Composition>());
   }

   public Set<Element> getElements() {
      return mStandards.keySet();
   }

   /**
    * Beam energy in SI
    *
    * @return In SI
    */
   public final double getBeamEnergy() {
      return FromSI.keV(this.mProperties.getNumericWithDefault(SpectrumProperties.BeamEnergy, -1.0));
   }

   /**
    * Take off angle in radians
    *
    * @return In radians
    */
   public final double getTakeOffAngle() {
      return Math.toRadians(this.mProperties.getNumericWithDefault(SpectrumProperties.TakeOffAngle, -1.0));
   }

   public SpectrumProperties getProperties() {
      return this.mProperties;
   }

   public void addStandard(Element elm, Composition comp) {
      this.mStandards.put(elm, comp);
   }

   public Pair<Composition, Double> oneLayer(KRatioSet krs) throws EPQException {
      final double toa = Math.toRadians(mProperties.getNumericProperty(SpectrumProperties.TakeOffAngle));
      CorrectionAlgorithm.PhiRhoZAlgorithm xpp = new XPP1991();
      HashMap<Element, UncertainValue2> crhoz = new HashMap<>();
      HashMap<XRayTransition, Double> iphirhoz = new HashMap<>();
      double rhoz = -1000.0;
      Composition comp = new Composition();
      // Iterate a few times...
      for (int i = 0; i < 10; ++i) {
         double rhoz2 = 0.0;
         Composition comp2 = new Composition();
         for (XRayTransitionSet trs : krs.keySet()) {
            Element elm = trs.getElement();
            XRayTransition xrt = trs.getWeighiestTransition();
            Composition std = this.mStandards.getOrDefault(elm, new Composition(elm));
            double iprz = iphirhoz.getOrDefault(xrt, -1.0);
            if (iprz == -1.0) {
               xpp.initialize(std, xrt.getDestination(), this.mProperties);
               iprz = xpp.computeZAFCorrection(xrt);
               iphirhoz.put(xrt, iprz);
            }
            double cas = std.weightFraction(elm, true);
            // f is the absorption correction factor
            double f = 1.0;
            if (rhoz > 0.0) {
               double chi = MassAbsorptionCoefficient.Default.compute(comp, xrt) / Math.sin(toa);
               // Use the previous estimate of rhoz to estimate the absorption
               // correction
               f = (1.0 - Math.exp(-chi * rhoz)) / (chi * rhoz);
            }
            UncertainValue2 tmp = UncertainValue2.multiply(cas * iprz / f, krs.getKRatioU(trs));
            crhoz.put(elm, tmp);
            rhoz2 += tmp.doubleValue();
         }
         for (Map.Entry<Element, UncertainValue2> me : crhoz.entrySet())
            comp2.addElement(me.getKey(), UncertainValue2.divide(me.getValue(), rhoz2));
         // Break out early if rhoz converges
         boolean breakout = Math.abs(rhoz - rhoz2) / rhoz2 < 0.005;
         comp = comp2;
         rhoz = rhoz2;
         if (breakout)
            break;
      }
      return new Pair<>(comp, Double.valueOf(rhoz));
   }

   public Pair<Pair<Composition, Double>, Pair<Composition, Double>> twoLayer(KRatioSet krs, Collection<Element> layer1, Collection<Element> layer2)
         throws EPQException {
      final double toa = Math.toRadians(mProperties.getNumericProperty(SpectrumProperties.TakeOffAngle));
      CorrectionAlgorithm.PhiRhoZAlgorithm xpp = new XPP1991();
      HashMap<Element, UncertainValue2> crhoz_l1 = new HashMap<>(), crhoz_l2 = new HashMap<>();
      HashMap<XRayTransition, Double> iphirhoz = new HashMap<>();
      double rhoz_l1 = -1000.0, rhoz_l2 = -1000.0;
      Composition comp_l1 = new Composition(), comp_l2 = new Composition();
      // Iterate a few times...
      for (int i = 0; i < 10; ++i) {
         double rhoz2_l1 = 0.0, rhoz2_l2 = 0.0;
         Composition comp2_l1 = new Composition(), comp2_l2 = new Composition();
         for (XRayTransitionSet trs : krs.keySet()) {
            Element elm = trs.getElement();
            XRayTransition xrt = trs.getWeighiestTransition();
            Composition std = this.mStandards.getOrDefault(elm, new Composition(elm));
            double iprz = iphirhoz.getOrDefault(xrt, -1.0);
            if (iprz == -1.0) {
               xpp.initialize(std, xrt.getDestination(), this.mProperties);
               iprz = xpp.computeZAFCorrection(xrt);
               iphirhoz.put(xrt, iprz);
            }
            double cas = std.weightFraction(elm, true);
            // f is the absorption correction factor
            if (layer1.contains(elm)) {
               if (layer2.contains(elm))
                  throw new EPQException("The element " + elm.toAbbrev() + " is present in both layers.");
               double f = 1.0; // Default no absorption
               if (rhoz_l1 > 0.0) {
                  double chi_l1 = MassAbsorptionCoefficient.Default.compute(comp_l1, xrt) / Math.sin(toa);
                  // Use the previous estimate of rhoz to estimate the
                  // absorption correction
                  f = (1.0 - Math.exp(-chi_l1 * rhoz_l1)) / (chi_l1 * rhoz_l1);
               }
               UncertainValue2 tmp = UncertainValue2.multiply(cas * iprz / f, krs.getKRatioU(trs));
               crhoz_l1.put(elm, tmp);
               rhoz2_l1 += tmp.doubleValue();
            } else if (layer2.contains(elm)) {
               double f = 1.0;
               if ((rhoz_l1 > 0.0) && (rhoz_l2 > 0)) {
                  double chi_l1 = MassAbsorptionCoefficient.Default.compute(comp_l1, xrt) / Math.sin(toa);
                  double chi_l2 = MassAbsorptionCoefficient.Default.compute(comp_l2, xrt) / Math.sin(toa);
                  // Use the previous estimate of rhoz to estimate the
                  // absorption correction
                  f = Math.exp(-chi_l1 * rhoz_l1) * (1.0 - Math.exp(-chi_l2 * rhoz_l2)) / (chi_l2 * rhoz_l2);
               }
               UncertainValue2 tmp = UncertainValue2.multiply(cas * iprz / f, krs.getKRatioU(trs));
               crhoz_l2.put(elm, tmp);
               rhoz2_l2 += tmp.doubleValue();
            } else
               throw new EPQException("The transition " + xrt + " is not specified as present in any layer.");
         }
         for (Map.Entry<Element, UncertainValue2> me : crhoz_l1.entrySet())
            comp2_l1.addElement(me.getKey(), UncertainValue2.divide(me.getValue(), rhoz2_l1));
         for (Map.Entry<Element, UncertainValue2> me : crhoz_l2.entrySet())
            comp2_l2.addElement(me.getKey(), UncertainValue2.divide(me.getValue(), rhoz2_l2));
         // Break out early if rhoz converges
         boolean breakout = (Math.abs(rhoz_l1 - rhoz2_l1) / rhoz2_l1 < 0.005) && (Math.abs(rhoz_l2 - rhoz2_l2) / rhoz2_l2 < 0.005);
         comp_l1 = comp2_l1;
         rhoz_l1 = rhoz2_l1;
         comp_l2 = comp2_l2;
         rhoz_l2 = rhoz2_l2;
         if (breakout)
            break;
      }
      return new Pair<>(new Pair<>(comp_l1, rhoz_l1), new Pair<>(comp_l2, rhoz_l2));
   }

   /**
    * Computes a the composition and mass-thickness of a N-layer unsupported
    * thin film from a set of measured k-ratios. The model is limited to samples
    * in which each element is at most present in one layer. The k-ratio set
    * must contain one k-ratio per element.
    *
    * @param krs
    *           The set of k-ratios (one per element)
    * @param layer
    *           Identifies which layer an element is present in (Layers are
    *           labeled 1, 2, 3 from front surface inward.)
    * @param oxidizers
    *           An optional Map<Integer, Oxidizer> to compute O-by-stoichiometry
    *           for the i-th layer
    * @return An list of composition, mass-thickness pairs for each layer in
    *         from the front surface
    * @throws EPQException
    */
   public ArrayList<Pair<Composition, Double>> multiLayer(KRatioSet krs, Map<Element, Integer> layer, Map<Integer, Oxidizer> oxidizers)
         throws EPQException {
      final int nLayers = layer.values().stream().max((a, b) -> Integer.compare(a, b)).get();
      { // Validate the inputs
         boolean[] filled = new boolean[nLayers];
         for (Map.Entry<Element, Integer> me : layer.entrySet()) {
            final int lyr = me.getValue() - 1;
            assert lyr >= 0 : "The layer index must be one or larger.";
            assert lyr < nLayers : "Element " + me.getKey().toAbbrev() + " is in layer " + me.getValue() + " which in more then nLayers = " + nLayers;
            filled[lyr] = true;
            assert krs.getElementSet().contains(me.getKey()) : me.getKey().toAbbrev() + " is not represented by a k-ratio.";
         }
         for (int i = 0; i < nLayers; ++i)
            assert filled[i] : "Layer " + i + " does not contain any elements.";
      }
      final double toa = SpectrumUtils.getTakeOffAngle(mProperties);
      assert (toa >= 0.0) && (toa < Math.PI);
      final MassAbsorptionCoefficient mac = MassAbsorptionCoefficient.Default;
      final HashMap<XRayTransition, Double> iphirhoz = new HashMap<>();
      final HashMap<Element, Double> stds = new HashMap<>();
      { // Pre-compute the bulk phi-rho-z correction
         final CorrectionAlgorithm.PhiRhoZAlgorithm xpp = new XPP1991();
         for (XRayTransitionSet trs : krs.keySet()) {
            final Element elm = trs.getElement();
            final XRayTransition xrt = trs.getWeighiestTransition();
            final Composition std = this.mStandards.getOrDefault(elm, new Composition(elm));
            stds.put(elm, std.weightFraction(elm, true));
            xpp.initialize(std, xrt.getDestination(), this.mProperties);
            iphirhoz.put(xrt, xpp.computeZAFCorrection(xrt));
         }
      }
      // Best estimate mass-thickness and composition for each layer
      final double[] rhoz = new double[nLayers];
      final Composition[] comp = new Composition[nLayers];
      final ArrayList<HashMap<Element, UncertainValue2>> crhoz = new ArrayList<>();
      for (int i = 0; i < nLayers; ++i) {
         comp[i] = new Composition();
         crhoz.add(new HashMap<>());
      }
      // Iterate a few times until the rhoz converge...
      boolean done = false;
      for (int i = 0; (i < 10) && (!done); ++i) {
         // Next estimate of mass-thickness and composition
         final double[] rhoz2 = new double[nLayers];
         // Iterate the measured k-ratios
         for (final XRayTransitionSet xrts : krs.keySet()) {
            final Element elm = xrts.getElement();
            final int lyr = layer.get(elm) - 1; // 1, 2, 3,.... minus 1
            final XRayTransition xrt = xrts.getWeighiestTransition();
            // Compute the absorption correction
            double f = 1.0;
            if (i > 0) {
               double chi_lyr = mac.compute(comp[lyr], xrt) / Math.sin(toa);
               // The layer containing the element
               f = (1.0 - Math.exp(-chi_lyr * rhoz[lyr])) / (chi_lyr * rhoz[lyr]);
               // Layers above the element layer to the top surface
               for (int l = lyr - 1; l >= 0; --l) {
                  final double chi_l = mac.compute(comp[l], xrt) / Math.sin(toa);
                  f *= Math.exp(-chi_l * rhoz[l]);
               }
               assert f <= 1.0;
               assert f >= 0.0;
            }
            final UncertainValue2 tmp = UncertainValue2.multiply(stds.get(elm) * iphirhoz.get(xrt) / f, krs.getKRatioU(xrts));
            assert !crhoz.get(lyr).containsKey(elm);
            crhoz.get(lyr).put(elm, tmp);
            rhoz2[lyr] += tmp.doubleValue();
         }
         // Accumulate the estimated compositions for each layer
         final Composition[] comp2 = new Composition[nLayers];
         for (int j = 0; j < nLayers; ++j)
            comp2[j] = new Composition();
         for (int l = 0; l < nLayers; ++l) {
            final HashMap<Element, UncertainValue2> lyr = crhoz.get(l);
            // Optionally compute oxygen-by-stoichiometry
            if (oxidizers.containsKey(l + 1)) {
               final Oxidizer oxidizer = oxidizers.get(l + 1);
               UncertainValue2 oxy = UncertainValue2.ZERO;
               for (Map.Entry<Element, UncertainValue2> me : lyr.entrySet()) {
                  final Element elm = me.getKey();
                  if (!elm.equals(Element.O)) {
                     final UncertainValue2 val = me.getValue();
                     final Composition oxide = oxidizer.getComposition(elm);
                     final UncertainValue2 qty = UncertainValue2.multiply(oxide.weightFraction(Element.O, false) / oxide.weightFraction(elm, false),
                           val.reduced(elm.toAbbrev()));
                     oxy = UncertainValue2.add(oxy, qty);
                  }
               }
               lyr.put(Element.O, oxy);
               rhoz2[l] += oxy.doubleValue();
            }
            for (Map.Entry<Element, UncertainValue2> me : lyr.entrySet())
               comp2[l].addElement(me.getKey(), UncertainValue2.divide(me.getValue(), rhoz2[l]));
         }
         // Break out early if rhoz converges
         done = (i >= 1);
         for (int l = nLayers - 1; (l >= 0) && done; --l)
            if (Math.abs(rhoz[l] - rhoz2[l]) > 0.0001 * rhoz2[l])
               done = false;
         for (int l = 0; l < nLayers; ++l) {
            comp[l] = comp2[l];
            rhoz[l] = rhoz2[l];
         }
      }
      final ArrayList<Pair<Composition, Double>> res = new ArrayList<>();
      for (int l = 0; l < nLayers; ++l)
         res.add(new Pair<>(comp[l], rhoz[l]));
      return res;
   }

   public ArrayList<Pair<Composition, Double>> multiLayer(KRatioSet krs, Map<Element, Integer> layer) throws EPQException {
      return multiLayer(krs, layer, Collections.emptyMap());
   }
}
