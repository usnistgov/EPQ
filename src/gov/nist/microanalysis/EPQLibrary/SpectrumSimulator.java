package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * An algorithm class for implementing spectrum simulators.
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
abstract public class SpectrumSimulator
   extends
   AlgorithmClass {
   private static final double DEFAULT_DET_DISTANCE = 0.05;

   protected SpectrumProperties mResultProperties;

   private static final AlgorithmClass[] mAllImplementations = {
      SpectrumSimulator.Basic
   };

   private boolean testSpectrum(ISpectrumData spec) {
      for(int i = 0; i < spec.getChannelCount(); ++i) {
         final double c = spec.getCounts(i);
         assert !Double.isNaN(c) : "getCounts(" + i + ") == NaN";
         assert !Double.isInfinite(c) : "getCounts(" + i + ") == Inf";
         assert c >= 0.0 : "getCounts(" + i + ") < 0";
         assert c <= 1.0e10 : "getCounts(" + i + ") > 1.0e10";
      }
      return true;
   }

   /**
    * A class that uses the ionization cross section and a transition
    * probability model to simulate x-ray emission.+
    */
   static public class BasicSpectrumSimulator
      extends
      SpectrumSimulator {

      public BasicSpectrumSimulator() {
         super("Basic spectrum simulator", LitReference.NullReference);
      }

      @Override
      protected void initializeDefaultStrategy() {
         addDefaultAlgorithm(CorrectionAlgorithm.PhiRhoZAlgorithm.class, CorrectionAlgorithm.XPPExtended);
         addDefaultAlgorithm(TransitionProbabilities.class, TransitionProbabilities.Default);
         addDefaultAlgorithm(EdgeEnergy.class, EdgeEnergy.SuperSet);
         addDefaultAlgorithm(AbsoluteIonizationCrossSection.class, AbsoluteIonizationCrossSection.BoteSalvat2008);
         addDefaultAlgorithm(BremsstrahlungAnalytic.class, BremsstrahlungAnalytic.Lifshin74);
      }

      /**
       * A general implementation of computeIntensities that permits computing
       * the intensity on an ionized shell-by-shell basis. The ionized shells
       * are specified via the <code>shells</code> argument.
       * 
       * @param comp
       * @param props
       * @param shells
       * @return TreeMap&lt;XRayTransition, Double&gt;
       * @throws EPQException
       */
      @Override
      public TreeMap<XRayTransition, Double> computeIntensities(final Composition comp, final SpectrumProperties props, final Collection<AtomicShell> shells)
            throws EPQException {
         mResultProperties = new SpectrumProperties();
         mResultProperties.addAll(props);
         final TreeMap<XRayTransition, Double> res = new TreeMap<XRayTransition, Double>();
         final TransitionProbabilities tp = (TransitionProbabilities) getAlgorithm(TransitionProbabilities.class);
         final double e0 = ToSI.keV(mResultProperties.getNumericWithDefault(SpectrumProperties.BeamEnergy, -1.0));
         if(e0 == ToSI.keV(-1.0))
            throw new EPQException("You must specify the beam energy.");
         if(e0 < ToSI.keV(0.1))
            throw new EPQException("The beam energy is too low to simulate a spectrum.");
         double dose;
         {
            // Probe current in amps (coulombs / second)
            final double pc = SpectrumUtils.getAverageFaradayCurrent(props, 1.0) * 1.0e-9;
            mResultProperties.setNumericProperty(SpectrumProperties.FaradayBegin, pc / 1.0e-9);
            // Live time in seconds
            final double lt = props.getNumericWithDefault(SpectrumProperties.LiveTime, 60.0);
            mResultProperties.setNumericProperty(SpectrumProperties.LiveTime, lt);
            // Working distance in mm
            {
               final double wd = props.getNumericWithDefault(SpectrumProperties.WorkingDistance, props.getNumericWithDefault(SpectrumProperties.DetectorOptWD, 0.0));
               mResultProperties.setNumericProperty(SpectrumProperties.WorkingDistance, wd);
            }
            // Dose in electrons ( 1 amp = (1/ElectronCharge) electrons /
            // second)
            dose = (lt * pc) / PhysicalConstants.ElectronCharge;
         }
         final CorrectionAlgorithm.PhiRhoZAlgorithm ca = (CorrectionAlgorithm.PhiRhoZAlgorithm) getAlgorithm(CorrectionAlgorithm.PhiRhoZAlgorithm.class);
         final AbsoluteIonizationCrossSection ic = (AbsoluteIonizationCrossSection) getAlgorithm(AbsoluteIonizationCrossSection.class);
         // For each shell to be ionized...
         final double MIN_WEIGHT = 1.0e-5;
         for(final AtomicShell shell : shells)
            if(shell.exists() && (shell.getEnergy() < e0)) {
               ca.initialize(comp, shell, mResultProperties);
               /*
                * (ionizations m^2 / (atomelectron)) electrons (atom/m^3) /
                * (kg/m^3) -> (ionizations (m^2 / kg))
                */
               final double icx = ic.computeShell(shell, e0) * dose * comp.atomsPerKg(shell.getElement(), true);
               assert icx >= 0.0 : "ICX[" + shell.toString() + "]=" + Double.toString(icx);
               // For each transition resulting from the ionization
               final Set<Map.Entry<XRayTransition, Double>> es = tp.getTransitions(shell, 0.0).entrySet();
               for(final Map.Entry<XRayTransition, Double> me : es) {
                  final XRayTransition xrt = me.getKey();
                  final double wgt = me.getValue().doubleValue();
                  if(xrt.energyIsAvailable() && (wgt >= MIN_WEIGHT) && xrt.isWellKnown()) {
                     final Double prev = res.get(xrt);
                     double s = (prev != null ? prev.doubleValue() : 0.0);
                     /*
                      * Apply the ZAF correction here as the phi-rho-z curve is
                      * determined by the shell but the absorption by the xrt.
                      * Subtle, n'est-ce pas?
                      * (x-rays/ionization)(ionizations(m^2/kg))(kg/m^2)
                      */
                     s += wgt * icx * ca.computeZAFCorrection(xrt);
                     if(!Double.isNaN(s)) {
                        assert (s >= 0.0) : "I[" + xrt.toString() + "]=" + Double.toString(s);
                        res.put(xrt, Double.valueOf(s));
                     }
                  }
               }
            }
         return res;
      }
   }

   /**
    * Constructs an object for that implements the logic for spectrum
    * simulation.
    * 
    * @param name
    * @param ref
    */
   protected SpectrumSimulator(final String name, final LitReference ref) {
      super("Spectrum simulator", name, ref);
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#getAllImplementations()
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   /**
    * Returns a full list of the shells excited in the specified material. If
    * the SpectrumProperties objects specifies a BeamEnergy then only those
    * shells with an edge energy below this energy will be returned.
    * 
    * @param comp
    * @param props
    * @return Set&lt;AtomicShell&gt;
    */
   public Set<AtomicShell> shellSet(final Composition comp, final SpectrumProperties props) {
      final EdgeEnergy ee = (EdgeEnergy) getAlgorithm(EdgeEnergy.class);
      final TreeSet<AtomicShell> shells = new TreeSet<AtomicShell>();
      final double e0 = ToSI.keV(props.getNumericWithDefault(SpectrumProperties.BeamEnergy, 1000.0));
      for(final Element elm : comp.getElementSet())
         if(comp.weightFraction(elm, false) > 0.0)
            for(int sh = AtomicShell.K; sh <= AtomicShell.MV; ++sh) {
               final AtomicShell as = new AtomicShell(elm, sh);
               if(ee.isSupported(as) && (ee.compute(as) < e0))
                  shells.add(as);
            }
      return shells;
   }

   /**
    * Generate a spectrum using the specified detector. Specify the beam energy,
    * working distance, take-off angle, probe current and live time in the
    * spectrum properties. Calculates the spectrum generated from the specified
    * set of atomic shells which may be a subset of the those excited by the
    * electron probe on the specified material.
    * 
    * @param comp
    * @param props
    * @param shells
    * @return An ISpectrumData
    * @throws EPQException
    */
   public ISpectrumData generateSpectrum(final Composition comp, final SpectrumProperties props, final Set<AtomicShell> shells, boolean withBremsstrahlung)
         throws EPQException {
      final EDSDetector detector = (EDSDetector) props.getDetector();
      detector.reset();
      final SpectrumProperties inProps = new SpectrumProperties();
      inProps.setDetector(detector);
      inProps.addAll(props);
      assert inProps.isDefined(SpectrumProperties.LiveTime);
      assert !Double.isNaN(SpectrumUtils.getAverageFaradayCurrent(inProps, Double.NaN));
      if((!inProps.isDefined(SpectrumProperties.WorkingDistance)) && inProps.isDefined(SpectrumProperties.DetectorOptWD))
         inProps.setNumericProperty(SpectrumProperties.WorkingDistance, inProps.getNumericWithDefault(SpectrumProperties.DetectorOptWD, 0.0));
      assert inProps.isDefined(SpectrumProperties.WorkingDistance);
      for(final Map.Entry<XRayTransition, Double> me : computeIntensities(comp, inProps, shells).entrySet())
         detector.addEvent(me.getKey().getEnergy(), me.getValue().doubleValue());
      if(withBremsstrahlung) {
         final BremsstrahlungAnalytic bs = (BremsstrahlungAnalytic) getAlgorithm(BremsstrahlungAnalytic.class);
         final double e0 = ToSI.keV(inProps.getNumericWithDefault(SpectrumProperties.BeamEnergy, Double.NaN));
         final double toa = SpectrumUtils.getTakeOffAngle(inProps);
         final double lt = inProps.getNumericWithDefault(SpectrumProperties.LiveTime, Double.NaN);
         final double pc = SpectrumUtils.getAverageFaradayCurrent(inProps, Double.NaN);
         bs.initialize(comp, e0, toa);
         bs.toDetector(detector, lt * pc);
      }
      final double sc = 1.0 / Math2.sqr(SpectrumUtils.sampleToDetectorDistance(inProps, DEFAULT_DET_DISTANCE));
      final ISpectrumData spec = detector.getSpectrum(sc);
      assert testSpectrum(spec);
      final SpectrumProperties sp = spec.getProperties();
      sp.addAll(props);
      sp.setCompositionProperty(SpectrumProperties.StandardComposition, comp);
      SpectrumUtils.rename(spec, "Simulation of " + comp.toString());
      assert testSpectrum(spec);
      return spec;
   }

   /**
    * Generate a spectrum using the specified detector. Specify the beam energy,
    * working distance, take-off angle, probe current and live time in the
    * spectrum properties.
    * 
    * @param comp
    * @param props
    * @param withBrem - Include the bremsstrahlung contribution.
    * @return ISpectrumData
    * @throws EPQException
    */
   public ISpectrumData generateSpectrum(final Composition comp, final SpectrumProperties props, boolean withBrem)
         throws EPQException {
      final Composition posComp = Composition.positiveDefinite(comp);
      return generateSpectrum(posComp, props, shellSet(posComp, props), withBrem);
   }

   /**
    * Compute the emitted x-ray flux from the specified material under the
    * specified conditions.
    * 
    * @param comp
    * @param props
    * @return TreeMap&lt;XRayTransition, Double&gt;
    * @throws EPQException
    */
   abstract public TreeMap<XRayTransition, Double> computeIntensities(Composition comp, SpectrumProperties props, Collection<AtomicShell> shells)
         throws EPQException;

   /**
    * Generates the characteristic intensities which would be measured on the
    * detector.
    * 
    * @param comp
    * @param props
    * @param shells
    * @return TreeMap&lt;XRayTransition, Double&gt;
    * @throws EPQException
    */
   public TreeMap<XRayTransition, Double> measuredIntensities(Composition comp, SpectrumProperties props, Collection<AtomicShell> shells)
         throws EPQException {
      final EDSDetector det = (EDSDetector) props.getDetector();
      final double sc = 1.0
            / (4.0 * Math.PI * Math2.sqr(SpectrumUtils.sampleToDetectorDistance(det.getProperties(), DEFAULT_DET_DISTANCE)));
      final TreeMap<XRayTransition, Double> tmp = computeIntensities(comp, props, shells);
      final TreeMap<XRayTransition, Double> res = new TreeMap<XRayTransition, Double>();
      final double[] eff = det.getEfficiency();
      for(final Map.Entry<XRayTransition, Double> me : tmp.entrySet()) {
         final XRayTransition xrt = me.getKey();
         final int ch = (int) ((xrt.getEnergy_eV() - det.getZeroOffset()) / det.getChannelWidth());
         if((ch >= 0) && (ch < eff.length))
            res.put(xrt, me.getValue().doubleValue() * sc * eff[ch]);
      }
      return res;
   }

   public static final SpectrumSimulator Basic = new SpectrumSimulator.BasicSpectrumSimulator();
}
