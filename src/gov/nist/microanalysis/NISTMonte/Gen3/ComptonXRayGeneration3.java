package gov.nist.microanalysis.NISTMonte.Gen3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import gov.nist.microanalysis.EPQLibrary.AlgorithmUser;
import gov.nist.microanalysis.EPQLibrary.BremsstrahlungAngularDistribution;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.JumpRatio;
import gov.nist.microanalysis.EPQLibrary.MassAbsorptionCoefficient;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MuCal;
import gov.nist.microanalysis.EPQLibrary.TransitionProbabilities;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.Utility.Math2;

/**
 * FluorescenceXRayGeneration implements the physics for secondary x-ray
 * generation.
 *
 * @author nicholas
 */
final public class ComptonXRayGeneration3 extends BaseXRayGeneration3 {

   /**
    * The max distance a photon may travel before being discarded as
    * out-of-the-system (in meters).
    */
   private static final double MAX_TRAVEL = 0.01;

   /**
    * Model frac serves to optimize the calculation. Since the effect is small
    * and since many simulated partial photons are generated per absorption
    * event we can afford to simulate only a fraction of the primary emission
    * events and then scale the intensity appropriately.
    */
   private static double mModelFraction = 0.1;

   private BaseXRayGeneration3 mSource;
   private final MuCal mMuCal = new MuCal();

   transient private final Random mRandom = new Random();

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
   public static ComptonXRayGeneration3 create(final MonteCarloSS mcss, final BaseXRayGeneration3 src) {
      final ComptonXRayGeneration3 res = new ComptonXRayGeneration3();
      res.initialize(mcss);
      res.mSource = src;
      src.addXRayListener(res);
      return res;
   }

   protected ComptonXRayGeneration3() {
      super("Secondary Fluorescence", "Default");
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
            // Randomly select only MODEL_FRAC of all x-rays to simulate Compton
            if (mRandom.nextDouble() >= mModelFraction)
               return;
            final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
            double[] pos = null;
            MonteCarloSS.RegionBase region = null;
            final BremsstrahlungAngularDistribution bremAngular = AlgorithmUser.getDefaultAngularDistribution();
            for (int i = mSource.getEventCount() - 1; i >= 0; --i) {
               // Only model a fraction of the primary photons
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
                     final double[] eps = Math2.multiply(1.0e-12, dir);
                     MonteCarloSS.RegionBase startR, endR = region;
                     double[] start = null, end = pos.clone();
                     final double xrE = xr.getEnergy();
                     // Find the region and point at which this x-ray is
                     // scattered.
                     boolean isNone = false;
                     double absorb = 0.0;
                     for (boolean step = true; step;) {
                        startR = endR;
                        start = end;
                        // Generate a random step of absLen in direction dir
                        final Material startMat = startR.getMaterial();
                        isNone = startMat.getDensity() < 1.0e-6;
                        final double len = isNone ? 2.0e6 : mMuCal.incoherentMeanFreePath(startMat, xrE) * Math2.expRand();
                        end = Math2.plus(start, Math2.multiply(len, dir));
                        endR = startR.findEndOfStep(start, end);
                        step = (endR != startR) && (endR != null);
                        absorb += mac.compute(startMat, xrE) * startMat.getDensity() * Math2.magnitude(Math2.minus(end, start));
                        if (Math2.magnitude(Math2.minus(pos, end)) > MAX_TRAVEL) {
                           // Terminate event due to traveling outside detection
                           // region
                           isNone = true;
                           step = false;
                        }
                        if (step)
                           // Pass through the interface and continue
                           end = Math2.plusEquals(end, eps);
                        assert startR != null;
                     }
                     // X-ray scatter event occurs at double[] 'end' in Region
                     // 'endR'
                     if (!((endR == null) || isNone)) {
                        performCompton(scale * xrI * Math.exp(-absorb), pos, end, xr);
                     }
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

   private void performCompton(final double xrI, final double[] start, final double[] end, final XRay xr) {
      for (final ActionListener al : mListener)
         if (al instanceof XRayTransport3) {
            final double[] ray = Math2.minus(end, start);
            if (Math2.magnitude(ray) > 0.0)
               addComptonXRay(end, ray, xrI, xr);
         }
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
   public static void setModelfraction(double modelfraction) {
      mModelFraction = Math2.bound(modelfraction, 0.01, 1.0);
   }
}
