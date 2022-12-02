package gov.nist.microanalysis.EPQLibrary;

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

   public Pair<Composition, Double> compute(KRatioSet krs) throws EPQException {
      final double toa = Math.toRadians(properties.getNumericProperty(SpectrumProperties.TakeOffAngle));
      CorrectionAlgorithm.PhiRhoZAlgorithm xpp = new XPP1991();
      HashMap<Element, UncertainValue2> crhoz = new HashMap<>();
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
            xpp.initialize(std, xrt.getDestination(), this.properties);
            double iprz = xpp.computeZAFCorrection(xrt);
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

}
