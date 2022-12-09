package gov.nist.microanalysis.EPQLibrary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import gov.nist.microanalysis.Utility.Pair;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * @author NWMR
 */
public class STEMinSEMQuant {

   private final HashMap<Element, Composition> standards;
   private final SpectrumProperties properties;

   public STEMinSEMQuant(SpectrumProperties properties, HashMap<Element, Composition> standards) {
      assert properties.isDefined(SpectrumProperties.BeamEnergy);
      assert properties.isDefined(SpectrumProperties.TakeOffAngle);
      this.properties = properties;
      this.standards = new HashMap<>(standards);
   }

   public STEMinSEMQuant(SpectrumProperties properties) {
      this(properties, new HashMap<Element, Composition>());
   }

   public void addStandard(Element elm, Composition comp) {
      this.standards.put(elm, comp);
   }

   public Pair<Composition, Double> oneLayer(KRatioSet krs) throws EPQException {
      final double toa = Math.toRadians(properties.getNumericProperty(SpectrumProperties.TakeOffAngle));
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
            Composition std = this.standards.getOrDefault(elm, new Composition(elm));
            double iprz = iphirhoz.getOrDefault(xrt, -1.0);
            if (iprz == -1.0) {
               xpp.initialize(std, xrt.getDestination(), this.properties);
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
      final double toa = Math.toRadians(properties.getNumericProperty(SpectrumProperties.TakeOffAngle));
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
            Composition std = this.standards.getOrDefault(elm, new Composition(elm));
            double iprz = iphirhoz.getOrDefault(xrt, -1.0);
            if (iprz == -1.0) {
               xpp.initialize(std, xrt.getDestination(), this.properties);
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
      return new Pair<>(new Pair<>(comp_l1, rhoz_l1),
            new Pair<>(comp_l2, rhoz_l2));
   }

   /**
    * Computes a the composition and mass-thickness of a N-layer unsupported thin film from a set of measured k-ratios.
    * The model is limited to samples in which each element is at most present in one layer.  The k-ratio set must contain 
    * one k-ratio per element.
    *  
    * @param krs The set of k-ratios (one per element)
    * @param layer Identifies which layer an element is present in (Layers are labeled 1, 2, 3 from front surface inward.)
    * @return An list of composition, mass-thickness pairs for each layer in from the front surface
    * @throws EPQException
    */
   public ArrayList<Pair<Composition, Double>> multiLayer(KRatioSet krs, Map<Element, Integer> layer) throws EPQException {
      final int nLayers = layer.values().stream().max((a,b)->Integer.compare(a,b)).get();
      { // Validate the inputs
         boolean[] filled = new boolean[nLayers];
         for (Map.Entry<Element, Integer> me : layer.entrySet()) {
            final int lyr = me.getValue()-1;
            assert lyr < nLayers
                  : "Element " + me.getKey().toAbbrev() + " is in layer " + me.getValue() + " which in more then nLayers = " + nLayers;
            filled[lyr] = true;
            assert krs.getElementSet().contains(me.getKey()) : me.getKey().toAbbrev() + " is not represented by a k-ratio.";
         }
         for (int i = 0; i < nLayers; ++i)
            assert filled[i] : "Layer " + i + " does not contain any elements.";
      }
      final double toa = Math.toRadians(properties.getNumericProperty(SpectrumProperties.TakeOffAngle));
      final MassAbsorptionCoefficient mac = MassAbsorptionCoefficient.Default;
      final HashMap<XRayTransition, Double> iphirhoz = new HashMap<>();
      final HashMap<Element, Double> stds = new HashMap<>();
      {  // Pre-compute the bulk phi-rho-z correction
         final CorrectionAlgorithm.PhiRhoZAlgorithm xpp = new XPP1991();
         for (XRayTransitionSet trs : krs.keySet()) {
            final Element elm = trs.getElement();
            XRayTransition xrt = trs.getWeighiestTransition();
            Composition std = this.standards.getOrDefault(elm, new Composition(elm));
            stds.put(elm, std.weightFraction(elm, true));
            xpp.initialize(std, xrt.getDestination(), this.properties);
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
      boolean breakout = false, firstIteration = true;
      for (int i = 0; (i < 10) && (!breakout); ++i) {
         // Next estimate of mass-thickness and composition
         final double[] rhoz2 = new double[nLayers];
         final Composition[] comp2 = new Composition[nLayers];
         for (int j = 0; j < nLayers; ++j)
            comp2[j] = new Composition();
         // Iterate the measured k-ratios
         for (final XRayTransitionSet xrts : krs.keySet()) {
            final Element elm = xrts.getElement();
            final int lyr = layer.get(elm) - 1; // 1, 2, 3,.... minus 1
            final XRayTransition xrt = xrts.getWeighiestTransition();
            // Compute the absorption correction
            double f = 1.0;
            if (!firstIteration) {
               double chi_lyr = mac.compute(comp[lyr], xrt) / Math.sin(toa);
               // This layer
               f = (1.0 - Math.exp(-chi_lyr * rhoz[lyr])) / (chi_lyr * rhoz[lyr]);
               // Layers between this layer and the top surface
               for (int l = lyr - 1; l >= 0; --l) {
                  double chi_l = mac.compute(comp[l], xrt) / Math.sin(toa);
                  f *= Math.exp(-chi_l * rhoz[l]);
               }
            }
            final UncertainValue2 tmp = UncertainValue2.multiply(stds.get(elm) * iphirhoz.get(xrt) / f, krs.getKRatioU(xrts));
            crhoz.get(lyr).put(elm, tmp);
            rhoz2[lyr] += tmp.doubleValue();
            for (Map.Entry<Element, UncertainValue2> me : crhoz.get(lyr).entrySet())
               comp2[lyr].addElement(me.getKey(), UncertainValue2.divide(me.getValue(), rhoz2[lyr]));
         }
         // Break out early if rhoz converges
         if (!firstIteration) {
            breakout = true;
            for (int l = nLayers - 1; l >= 0; --l)
               if (Math.abs(rhoz[l] - rhoz2[l]) > 0.005 * rhoz[l]) {
                  breakout = false;
                  break;
               }
         }
         for (int l = 0; l < nLayers; ++l) {
            comp[l] = comp2[l];
            rhoz[l] = rhoz2[l];
         }
         firstIteration = false;
      }
      final ArrayList<Pair<Composition, Double>> res = new ArrayList<>();
      for (int l = 0; l < nLayers; ++l)
         res.add(new Pair<>(comp[l], rhoz[l]));
      return res;
   }

}
