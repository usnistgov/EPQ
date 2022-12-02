package gov.nist.microanalysis.NISTMonte.Gen3;

import java.awt.event.ActionEvent;
import java.util.Map;

import gov.nist.microanalysis.EPQLibrary.AlgorithmUser;
import gov.nist.microanalysis.EPQLibrary.BremsstrahlungAngularDistribution;
import gov.nist.microanalysis.EPQLibrary.MACCache;
import gov.nist.microanalysis.EPQLibrary.MassAbsorptionCoefficient;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.Detector.IXRayDetector;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.Utility.Math2;

/**
 * A class that accounts for intensity lost due transport from one point to the
 * next. Typically, this is used to transport the x-rays from the point of
 * generation to a detector. By breaking it down this way, it is possible to use
 * multiple detectors simultaneously.
 * 
 * @author nicholas
 */
final public class XRayTransport3 extends BaseXRayGeneration3 {

   private double[] mEndPoint;
   private BaseXRayGeneration3 mSource;

   /**
    * @return the mSource
    */
   public BaseXRayGeneration3 getSource() {
      return mSource;
   }

   transient private MACCache mCache;

   /**
    * Use this static method instead of the constructor to create instances of
    * this class and initialize it with an instance of the MonteCarloSS class.
    * 
    * @param mcss
    *           MonteCarloSS instance
    * @param endPoint
    *           The point at which the x-ray transport terminates
    * @return FluorescenceXRayGeneration
    */
   public static XRayTransport3 create(MonteCarloSS mcss, double[] endPoint, BaseXRayGeneration3 src) {
      final XRayTransport3 res = new XRayTransport3();
      res.initialize(mcss, endPoint, src);

      return res;
   }

   public void initialize(MonteCarloSS mcss, double[] endPt, BaseXRayGeneration3 src) {
      super.initialize(mcss);
      mEndPoint = endPt.clone();
      mSource = src;
      src.addXRayListener(this);
   }

   /**
    * Use this static method instead of the constructor to create instances of
    * this class and initialize it with an instance of the MonteCarloSS class.
    * 
    * @param mcss
    *           MonteCarloSS instance
    * @param det
    *           IXRayDetector like EDSDetector, SiLiDetector etc.
    * @return FluorescenceXRayGeneration
    */
   public static XRayTransport3 create(MonteCarloSS mcss, IXRayDetector det, BaseXRayGeneration3 src) {
      final XRayTransport3 res = create(mcss, det.getDetectorProperties().getPosition(), src);
      res.addXRayListener(det);
      return res;
   }

   protected XRayTransport3() {
      super("X-Ray Transport", "Default");
   }

   public double[] getEndPoint() {
      return mEndPoint.clone();
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * gov.nist.microanalysis.EPQLibrary.AlgorithmUser#initializeDefaultStrategy
    * ()
    */
   @Override
   protected void initializeDefaultStrategy() {
      addDefaultAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Default);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   @Override
   public void actionPerformed(ActionEvent e) {
      assert e.getSource() instanceof BaseXRayGeneration3;
      assert e.getSource() == mSource;
      reset();
      switch (e.getID()) {
         case BaseXRayGeneration3.XRayGeneration : {
            final BaseXRayGeneration3 bxre = (BaseXRayGeneration3) e.getSource();
            final BremsstrahlungAngularDistribution bremAngular = AlgorithmUser.getDefaultAngularDistribution();
            double[] startPt = null;
            double geo = Double.NaN;
            Map<Material, Double> path = null;
            for (int i = bxre.getEventCount() - 1; i >= 0; --i) {
               final XRay xr = bxre.getXRay(i);
               if (xr.getPosition() != startPt) {
                  startPt = xr.getPosition();
                  path = mMonte.getMaterialMap(startPt, mEndPoint);
                  geo = 1.0 / Math2.distanceSqr(startPt, mEndPoint);
               }
               final double energy = xr.getEnergy();
               final double[] outgoingDir = Math2.minus(mEndPoint, xr.getPosition());
               final double woAbs = geo * xr.getIntensity();
               if (xr instanceof BremsstrahlungXRay) {
                  BremsstrahlungXRay bxr = (BremsstrahlungXRay) xr;
                  final double generated = woAbs
                        * bremAngular.compute(bxr.getElement(), bxr.getAngle(outgoingDir), bxr.getElectronEnergy(), xr.getEnergy());
                  addXRay(xr, mEndPoint, generated * Math.exp(-calculateEffectiveMAC(path, energy)), generated);
               } else if (xr instanceof ComptonXRay) {
                  final ComptonXRay cxr = (ComptonXRay) xr;
                  final double th = Math2.angleBetween(cxr.getPrimaryDirection(), outgoingDir);
                  final double shifted = xr.getEnergy() * comptonShift(th, xr.getEnergy());
                  assert !Double.isNaN(shifted) : xr + " -> th=" + th;
                  final double generated = comptonAngular(xr.getEnergy(), th) * woAbs;
                  addXRay(cxr, mEndPoint, shifted, generated * Math.exp(-calculateEffectiveMAC(path, shifted)), generated);
               } else
                  addXRay(xr, mEndPoint, woAbs * Math.exp(-calculateEffectiveMAC(path, energy)), woAbs);
            }
            fireXRayListeners();
         }
            break;
         default :
            fireXRayListeners(e.getID());
            break;
      }
   }

   private double calculateEffectiveMAC(Map<Material, Double> path, final double energy) {
      if (mCache == null)
         mCache = new MACCache(mMonte.getBeamEnergy(), (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class));
      double mac = 0.0;
      for (final Map.Entry<Material, Double> matLen : path.entrySet()) {
         final Material mat = matLen.getKey();
         if (mat != Material.Null) {
            final double len = matLen.getValue().doubleValue();
            mac += mCache.getMAC(mat, energy) * mat.getDensity() * len;
         }
      }
      return mac;
   }

   /**
    * The fractional Compton shift.
    *
    * @param th
    * @param xrE
    *           In Joules
    * @return double The fractional change in x-ray energy
    */
   final private static double comptonShift(final double th, final double xrE) {
      return (1.0 / (1 + ((xrE / PhysicalConstants.ElectronRestMass) * (1 - Math.cos(th)))));
   }

   /**
    * This is based on the Klein-Nishina formula for Compton scattering. It has
    * been normalized so that the integral over dOmega = 2 pi sin(Th)dTh equals
    * 1.
    *
    * @param e
    *           In Joules
    * @param th
    * @return double
    */
   final public static double comptonAngular(final double e, final double th) {
      final double p = comptonShift(th, e);
      final double sinTh = Math.sin(th);
      final double me = ToSI.keV(511.0);
      final double den = (4 * Math.PI * me * ((e * (e * e * e + 9 * e * e * me + 8 * e * me * me + 2 * me * me * me)) / Math2.sqr(2 * e + me)
            + (e * e - 2 * e * me - 2 * me * me) * Math.atan(e / (e + me)))) / (e * e * e);
      return (p * p * (p + 1.0 / p - sinTh * sinTh)) / den;
   }
}
