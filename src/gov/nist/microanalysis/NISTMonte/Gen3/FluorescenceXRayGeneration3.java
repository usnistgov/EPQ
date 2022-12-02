package gov.nist.microanalysis.NISTMonte.Gen3;

import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import gov.nist.microanalysis.EPQLibrary.AlgorithmUser;
import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.BremsstrahlungAngularDistribution;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.JumpRatio;
import gov.nist.microanalysis.EPQLibrary.MassAbsorptionCoefficient;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.TransitionProbabilities;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.Utility.DescriptiveStatistics;
import gov.nist.microanalysis.Utility.Math2;

/**
 * FluorescenceXRayGeneration implements the physics for secondary x-ray
 * generation.
 *
 * @author nicholas
 */
final public class FluorescenceXRayGeneration3 extends BaseXRayGeneration3 {

   /**
    * The max distance a photon may travel before being discarded as
    * out-of-the-system (in meters).
    */
   private static final double MAX_TRAVEL = 0.01;
   /**
    * The minimum weight line to consider (0.01 of the maximum transition
    * probability in a shell)
    */
   private static final double MIN_WEIGHT = 0.01;
   /**
    * Model frac serves to optimize the calculation. Since the effect is small
    * and since many simulated partial photons are generated per absorption
    * event we can afford to simulate only a fraction of the primary emission
    * events and then scale the intensity appropriately.
    */
   private static double mModelFraction = 0.1;

   private BaseXRayGeneration3 mSource;

   transient private MassAbsorptionCoefficient mMac = null;
   transient private final Random mRandom = new Random();
   transient private final DescriptiveStatistics mScaleStats = new DescriptiveStatistics();

   private class ShellData {
      final AtomicShell mShell;
      final double mEdgeEnergy;
      final double mIonizationFrac;

      ShellData(final AtomicShell sh) {
         mShell = sh;
         mEdgeEnergy = mShell.getEdgeEnergy();
         final JumpRatio jra = (JumpRatio) getAlgorithm(JumpRatio.class);
         mIonizationFrac = jra.ionizationFraction(mShell);
      }
   }

   transient TreeMap<Element, ShellData[]> mShells = new TreeMap<Element, ShellData[]>();
   transient TreeMap<AtomicShell, TreeMap<XRayTransition, Double>> mTransitionMap = new TreeMap<AtomicShell, TreeMap<XRayTransition, Double>>();

   /**
    * Use this static method instead of the constructor to create instances of
    * this class and initialize it with an instance of the MonteCarloSS class. *
    *
    * @param mcss
    *           An instance of MonteCarloSS
    * @param src
    *           The source of the primary x-rays
    * @return FluorescenceXRayGeneration
    */
   public static FluorescenceXRayGeneration3 create(final MonteCarloSS mcss, final BaseXRayGeneration3 src) {
      final FluorescenceXRayGeneration3 res = new FluorescenceXRayGeneration3();
      res.initialize(mcss);
      res.mSource = src;
      src.addXRayListener(res);
      return res;
   }

   protected FluorescenceXRayGeneration3() {
      super("Secondary Fluorescence", "Default");
   }

   public DescriptiveStatistics getScaleStats() {
      return mScaleStats;
   }

   /*
    * @see
    * gov.nist.microanalysis.EPQLibrary.AlgorithmUser#initializeDefaultStrategy
    * ()
    */
   @Override
   protected void initializeDefaultStrategy() {
      addDefaultAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Default);
      addDefaultAlgorithm(JumpRatio.class, JumpRatio.Springer1967);
      addDefaultAlgorithm(TransitionProbabilities.class, TransitionProbabilities.Default);
   }

   /**
    * Pick an element and an atomic shell within the element to which to assign
    * the ionization due to photoabsorption of a photon of the specified energy.
    *
    * @param mat
    * @param energy
    * @return AtomicShell
    */
   public AtomicShell pickShell(final Material mat, final double energy) {
      // Pick the element that absorbed the x-ray from the MACs
      assert mMac != null;
      final Element absorber = mMac.randomizedAbsorbingElement(mat, energy);
      // Cache the AtomicShell objects
      final ShellData[] shells = getShells(absorber);
      // absorbed by 'absorber' now figure out which shell
      int highE = AtomicShell.NoShell;
      {
         for (int sh = AtomicShell.K; sh <= AtomicShell.MV; ++sh)
            if ((shells[sh] != null) && (energy > shells[sh].mEdgeEnergy)) {
               // Only consider the highest energy family below the specified
               // x-ray energy
               highE = sh;
               break;
            }
      }
      if (highE != AtomicShell.NoShell) {
         final int lowE = AtomicShell.getLastInFamily(AtomicShell.getFamily(highE));
         assert AtomicShell.getFamily(lowE) == AtomicShell.getFamily(highE);
         assert lowE >= highE;
         assert AtomicShell.getEdgeEnergy(absorber, highE) >= AtomicShell.getEdgeEnergy(absorber, lowE);
         double r = mRandom.nextDouble();
         double sc = 1.0;
         for (int sh = highE; sh <= lowE; ++sh) {
            final ShellData shellData = shells[sh];
            if (shellData != null) {
               final double f = sc * shellData.mIonizationFrac;
               r -= f;
               if (r < 0.0)
                  return shellData.mShell;
               sc *= (1.0 - f);
            }
         }
      }
      return null;
   }

   /**
    * Implements a cacheing scheme for atomic shell data associated with the
    * specified element.
    *
    * @param absorber
    * @return An array of ShellData objects
    */
   private ShellData[] getShells(final Element absorber) {
      ShellData[] shells = mShells.get(absorber);
      if (shells == null) {
         shells = new ShellData[AtomicShell.NI];
         for (int sh = AtomicShell.K; sh <= AtomicShell.MV; ++sh)
            if (AtomicShell.exists(absorber, sh)) {
               final ShellData shellData = new ShellData(new AtomicShell(absorber, sh));
               if (shellData.mIonizationFrac > 0.0)
                  shells[sh] = shellData;
            }
         mShells.put(absorber, shells);
      }
      return shells;
   }

   /**
    * @param e
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   @Override
   public void actionPerformed(final ActionEvent e) {
      assert e.getSource() instanceof BaseXRayGeneration3;
      assert e.getSource() == mSource;
      reset(); // Reset the result accumulator...
      switch (e.getID()) {
         case BaseXRayGeneration3.XRayGeneration : {
            // Randomly select only MODEL_FRAC of all x-rays to simulate SF
            if (mRandom.nextDouble() >= mModelFraction)
               return;
            mMac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
            double[] pos = null;
            MonteCarloSS.RegionBase region = null;
            final BremsstrahlungAngularDistribution bremAngular = AlgorithmUser.getDefaultAngularDistribution();
            for (int i = mSource.getEventCount() - 1; i >= 0; --i) {
               // Only model a fraction of the primary photons for secondary
               final BaseXRayGeneration3.XRay xr = mSource.getXRay(i);
               // Scale the intensity to account for the neglected events
               final double xrI = xr.getIntensity() / mModelFraction;
               assert xrI > 0.0;
               if (xrI > 0.0)
                  try {
                     if (xr.getPosition() != pos) {
                        pos = xr.getPosition();
                        region = mMonte.findRegionContaining(pos);
                     }
                     // The primary x-ray intensity is assumed to be emitted
                     // isotropically starting at pos
                     final double[] dir = Math2.randomDir();
                     final BremsstrahlungXRay bxr = (xr instanceof BremsstrahlungXRay ? (BremsstrahlungXRay) xr : null);
                     // Account for Bremsstrahlung shape function if necessary.
                     final double scale = (bxr == null
                           ? 1.0
                           : bremAngular.compute(bxr.getElement(), bxr.getAngle(dir), bxr.getElectronEnergy(), xr.getEnergy()));
                     mScaleStats.add(scale);
                     // final double scale = 1.0;
                     final double[] eps = Math2.multiply(1.0e-12, dir);
                     MonteCarloSS.RegionBase startR, endR = region;
                     double[] start = null, end = pos.clone();
                     final double xrE = xr.getEnergy();
                     // Find the region and point at which this x-ray is
                     // absorbed...
                     boolean generateFluor = true;
                     for (boolean takeAnotherStep = true; takeAnotherStep;) {
                        startR = endR;
                        start = end;
                        // Generate a random step of absLen in direction dir
                        assert startR != null;
                        final Material startMat = startR.getMaterial();
                        generateFluor = startMat.getDensity() >= 1.0e-6;
                        final double len = generateFluor ? mMac.meanFreePath(startMat, xrE) * Math2.expRand() : 1.0e6;
                        end = Math2.plus(start, Math2.multiply(len, dir));
                        endR = startR.findEndOfStep(start, end);
                        takeAnotherStep = (endR != startR) && (endR != null);
                        if (takeAnotherStep) {
                           // Pass through the interface and continue
                           end = Math2.plusEquals(end, eps);
                           if (Math2.magnitude(Math2.minus(pos, end)) > MAX_TRAVEL) {
                              // Terminate event due to traveling outside
                              // detection region
                              generateFluor = false;
                              takeAnotherStep = false;
                           }
                        }
                     }

                     // X-ray scatter event occurs at double[] 'end' in Region
                     // 'endR'
                     if ((endR != null) && generateFluor)
                        performFluorescence(scale * xrI, endR, end, xrE);
                  } catch (final EPQException e1) {
                     e1.printStackTrace();
                  }
            }
            fireXRayListeners(BaseXRayGeneration3.XRayGeneration);
         }
            break;
         default :
            fireXRayListeners(e.getID());
            break;
      }
   }

   private void performFluorescence(final double xrI, final MonteCarloSS.RegionBase endR, final double[] end, final double xrE) throws EPQException {
      assert endR.getMaterial().getDensity() > 0.0 : endR.getMaterial().toString();
      final AtomicShell ionized = pickShell(endR.getMaterial(), xrE);
      if (ionized != null) {
         final TreeMap<XRayTransition, Double> tm = getTransitions(ionized);
         for (final Map.Entry<XRayTransition, Double> me2 : tm.entrySet()) {
            final XRayTransition sfXrt = me2.getKey();
            final double energy = sfXrt.getEnergy();
            if (energy > 0.0) {
               final Double trProb = me2.getValue();
               addCharXRay(end, energy, trProb * xrI, trProb * xrI, sfXrt);
            }
         }
      }
   }

   /**
    * Returns a map containing x-ray transition and the associated probabilities
    * for ionizations in the specified AtomicShell.
    *
    * @param ionized
    * @return TreeMap&lt;XRayTransition, Double&gt;
    */
   private TreeMap<XRayTransition, Double> getTransitions(final AtomicShell ionized) {
      TreeMap<XRayTransition, Double> tm = mTransitionMap.get(ionized);
      if (tm == null) {
         final TransitionProbabilities tp = (TransitionProbabilities) getAlgorithm(TransitionProbabilities.class);
         assert tp != null;
         tm = tp.getTransitions(ionized, MIN_WEIGHT);
         mTransitionMap.put(ionized, tm);
      }
      return tm;
   }

   /**
    * The model fraction is the fraction of characteristic or Bremsstrahlung
    * x-rays which are used to estimate the secondary fluorescence.
    *
    * @return Returns the mmodelfraction.
    */
   public static double getModelfraction() {
      return mModelFraction;
   }

   /**
    * The model fraction is the fraction of characteristic or Bremsstrahlung
    * x-rays which are used to estimate the secondary fluorescence. Default is
    * 0.1.
    *
    * @param modelfraction
    *           The value to which to set modelfraction [0.01, 1.0]
    */
   public static void setModelfraction(final double modelfraction) {
      mModelFraction = Math2.bound(modelfraction, 0.01, 1.0);
   }
}
