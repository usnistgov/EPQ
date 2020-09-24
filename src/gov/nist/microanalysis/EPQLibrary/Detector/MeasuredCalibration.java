/**
 * 
 */
package gov.nist.microanalysis.EPQLibrary.Detector;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.MassAbsorptionCoefficient;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.SpectrumFitResult;
import gov.nist.microanalysis.EPQLibrary.SpectrumFitter8;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQLibrary.XRayTransitionSet;
import gov.nist.microanalysis.Utility.Constraint;
import gov.nist.microanalysis.Utility.LevenbergMarquardt2;
import gov.nist.microanalysis.Utility.LevenbergMarquardt2.FitResult;
import gov.nist.microanalysis.Utility.LevenbergMarquardtConstrained;
import gov.nist.microanalysis.Utility.LevenbergMarquardtConstrained.ConstrainedFitFunction;
import gov.nist.microanalysis.Utility.UncertainValue2;
import gov.nist.microanalysis.Utility.UtilException;

import Jama.Matrix;

/**
 * @author nicholas
 */
public class MeasuredCalibration
   extends
   EDSCalibration {

   /**
    * Mass thickness of any absorbers between sample and detector active area
    * (except a partially transparent grid)
    */
   private final Map<Composition, UncertainValue2> mMassThickness = new TreeMap<Composition, UncertainValue2>();
   /**
    * Open fraction of the window support grid.
    */
   private double mOpenFraction = 0.77;
   /**
    * Thickness of the window support grid
    */
   private double mGridThickness = 0.38e-3;
   /**
    * The material from the window support grid
    */
   private Material mGridMaterial = createSilicon();
   /**
    * The thickness of the detector active region (nominally 3 mm for Si(Li) and
    * 450 microns)
    */
   private UncertainValue2 mDetectorThickness = new UncertainValue2(0.450e-3, "DT", 0.10e-3);
   /**
    * The material from which the detector active area is made
    */
   private Material mActiveMaterial = createSilicon();
   transient private double[] mEfficiency = null;

   private Material createSilicon() {
      return new Material(new Composition(new Element[] {
         Element.Si
      }, new double[] {
         1.0
      }, "Silicon"), ToSI.gPerCC(2.329));
   }

   final transient MassAbsorptionCoefficient mMAC = MassAbsorptionCoefficient.Default;

   public void removeLayer(Composition comp) {
      mMassThickness.remove(comp);
   }

   public double layerThickness(Composition comp) {
      return mMassThickness.containsKey(comp) ? mMassThickness.get(comp).doubleValue() : 0.0;
   }

   public UncertainValue2 layerThicknessU(Composition comp) {
      return mMassThickness.containsKey(comp) ? mMassThickness.get(comp) : UncertainValue2.ZERO;
   }

   public Map<Composition, UncertainValue2> layers() {
      return Collections.unmodifiableMap(mMassThickness);
   }

   public void addLayer(Composition comp, double estThickness) {
      mMassThickness.put(comp, new UncertainValue2(estThickness, "LT", 0.1 * estThickness));
   }

   /**
    * Takes a map of XRayTransitions to measured efficiencies
    * 
    * @param efficiency
    * @return boolean
    * @throws EPQException
    */
   public boolean calibrate(Map<XRayTransition, Double> efficiency)
         throws EPQException {
      // Number of data points to fit
      final int dataLen = efficiency.size();
      final double[] energies = new double[dataLen];
      final double[] efficiencies = new double[dataLen];
      // Set up the fit parameters
      final Composition[] comps = mMassThickness.keySet().toArray(new Composition[0]);
      final int paramLen = comps.length + 1;
      assert paramLen <= dataLen : "You must have more data items than fit parameters.";
      if(paramLen > dataLen)
         throw new EPQException("Too few data points.  There must be more data items than fit parameters.");
      {
         int idx = 0;
         for(final Map.Entry<XRayTransition, Double> xrtMe : efficiency.entrySet()) {
            energies[idx] = xrtMe.getKey().getEnergy();
            efficiencies[idx++] = xrtMe.getValue();
         }
         assert idx == dataLen;
      }

      final LevenbergMarquardt2.FitFunction ff = new LevenbergMarquardt2.FitFunction() {
         /**
          * params are N * [rho*z]_i + detThick
          */
         @Override
         public Matrix partials(Matrix params) {
            final Matrix res = new Matrix(dataLen, params.getColumnDimension());
            final Matrix vals = compute(params);
            for(int n = 0; n < dataLen; ++n) {
               final double val = vals.get(n, 0);
               int idx;
               for(idx = 0; idx < comps.length; ++idx)
                  res.set(n, idx, -val * params.get(idx, 0));
               {
                  final double detThick = params.get(idx, 0);
                  res.set(n, idx++, (val * dDetector(detThick, energies[n])) / detector(detThick, energies[n]));
               }
               assert idx == paramLen;
            }
            return res;
         }

         @Override
         public Matrix compute(Matrix params) {
            // Compute window etc. thickness
            final Matrix res = new Matrix(energies.length, 1);
            for(int n = 0; n < dataLen; ++n) {
               final TreeMap<Composition, UncertainValue2> layers = new TreeMap<Composition, UncertainValue2>();
               int i = 0;
               for(final Composition comp : comps)
                  layers.put(comp, new UncertainValue2(params.get(i++, 0)));
               final UncertainValue2 detThick = new UncertainValue2(params.get(i++, 0));
               assert i == paramLen;
               res.set(n++, 0, MeasuredCalibration.this.compute(layers, detThick, energies[n]) - efficiencies[n]);
            }
            return res;
         }
      };

      final LevenbergMarquardtConstrained.ConstrainedFitFunction cff = new ConstrainedFitFunction(ff, paramLen);
      {
         int i = 0;
         for(@SuppressWarnings("unused")
         final Composition comp : comps)
            cff.setConstraint(i++, new Constraint.Positive(1.0e-8));
         cff.setConstraint(i++, new Constraint.Bounded(2.5e-3, 2.5e-3));
      }
      final LevenbergMarquardtConstrained lmc = new LevenbergMarquardtConstrained();
      final Matrix yData = new Matrix(dataLen, 1);
      final Matrix sigma = new Matrix(dataLen, 1);
      {
         int idx = 0;
         for(final Map.Entry<XRayTransition, Double> me : efficiency.entrySet()) {
            final double eff = me.getValue();
            yData.set(idx, 0, eff);
            sigma.set(idx++, 0, 0.10 * eff);
         }
         assert idx == dataLen;
      }
      final Matrix p0 = new Matrix(paramLen, 1);
      for(int i = 0; i < (paramLen - 1); ++i)
         p0.set(i, 0, Math.min(Math.max(1.0e-9, mMassThickness.get(comps[i]).doubleValue()), 1.0e-6));
      p0.set(paramLen - 1, 0, Math.min(Math.max(1.0e-9, mDetectorThickness.doubleValue()), 4.9e-3));
      final FitResult fr = lmc.compute(cff, yData, sigma, p0);
      {
         final UncertainValue2[] bestFit = fr.getBestParametersU();
         int i = 0;
         mMassThickness.clear();
         for(final Composition comp : comps)
            mMassThickness.put(comp, bestFit[i++]);
         mDetectorThickness = bestFit[i++];
      }
      return true;
   }

   final private double layer(Composition mat, double massThickness, double energy) {
      return Math.exp(-mMAC.compute(mat, energy) * massThickness);
   }

   final private double grid(double energy) {
      return mGridMaterial != null
            ? mOpenFraction + ((1.0 - mOpenFraction) * Math.exp(-mMAC.compute(mGridMaterial, energy) * mGridThickness))
            : 1.0;
   }

   final private double detector(double detThick, double energy) {
      return 1.0 - Math.exp(-mMAC.compute(mActiveMaterial, energy) * detThick);
   }

   final private double dDetector(double detThick, double energy) {
      final double mac = mMAC.compute(mActiveMaterial, energy);
      return Math.exp(-mac * detThick) * mac;
   }

   final private double compute(Map<Composition, UncertainValue2> layers, UncertainValue2 detThick, double energy) {
      double res = 1.0;
      for(final Map.Entry<Composition, UncertainValue2> me : layers.entrySet())
         res *= layer(me.getKey(), me.getValue().doubleValue(), energy);
      return res * detector(detThick.doubleValue(), energy) * grid(energy);
   }

   /**
    * @param scale eV/channel energy scale
    * @param offset Zero offset in eV
    * @param quadratic Quadratic term in energy calibration (eV/ch^2)
    * @param dlm The detector lineshape model
    */
   public MeasuredCalibration(double scale, double offset, double quadratic, DetectorLineshapeModel dlm) {
      super("Fit Calibration", scale, offset, quadratic, dlm);
   }

   /*
    * (non-Javadoc)
    * @see gov.nist.microanalysis.EPQLibrary.Detector.DetectorCalibration#
    * getEfficiency
    * (gov.nist.microanalysis.EPQLibrary.Detector.DetectorProperties)
    */
   @Override
   public double[] getEfficiency(DetectorProperties dp) {
      if(mEfficiency == null) {
         final double[] res = new double[dp.getChannelCount()];
         final double eVpCh = this.getChannelWidth();
         final double zeroO = this.getZeroOffset();
         for(int i = 0; i < res.length; ++i)
            res[i] = compute(mMassThickness, mDetectorThickness, ToSI.eV(zeroO + (i * eVpCh)));
         mEfficiency = res;
      }
      return mEfficiency;
   }

   /**
    * Returns two arrays representing crude estimates of the bounds of the
    * detector efficiency. The bounds are calculated by offseting all best fit
    * parameters by one sigma (plus and minus) and recomputing the efficiency.
    * 
    * @param dp
    * @return double[][]
    */
   public double[][] getCrudeBounds(DetectorProperties dp) {
      final double[][] res = new double[2][dp.getChannelCount()];
      final double eVpCh = this.getChannelWidth();
      final double zeroO = this.getZeroOffset();
      final TreeMap<Composition, UncertainValue2> p = new TreeMap<Composition, UncertainValue2>();
      final TreeMap<Composition, UncertainValue2> n = new TreeMap<Composition, UncertainValue2>();
      for(final Map.Entry<Composition, UncertainValue2> me : mMassThickness.entrySet()) {
         final UncertainValue2 val = me.getValue();
         p.put(me.getKey(), new UncertainValue2(val.doubleValue() + val.uncertainty()));
         n.put(me.getKey(), new UncertainValue2(val.doubleValue() - val.uncertainty()));
      }
      for(int i = 0; i < res.length; ++i) {
         final UncertainValue2 pThick = new UncertainValue2(mDetectorThickness.doubleValue()
               + (10.0 * mDetectorThickness.uncertainty()));
         final UncertainValue2 nThick = new UncertainValue2(mDetectorThickness.doubleValue()
               - (10.0 * mDetectorThickness.uncertainty()));
         res[0][i] = compute(p, pThick, ToSI.eV(zeroO + (i * eVpCh)));
         res[1][i] = compute(n, nThick, ToSI.eV(zeroO + (i * eVpCh)));
      }
      return res;
   }

   @Override
   public EDSCalibration clone() {
      final MeasuredCalibration res = new MeasuredCalibration(getChannelWidth(), getZeroOffset(), getQuadratic(), getLineshape());
      res.mMassThickness.clear();
      res.mMassThickness.putAll(mMassThickness);
      res.mOpenFraction = mOpenFraction;
      res.mGridThickness = mGridThickness;
      res.mGridMaterial = mGridMaterial.clone();
      res.mDetectorThickness = mDetectorThickness;
      res.mActiveMaterial = mActiveMaterial.clone();
      return res;
   }

   @Override
   public boolean isVisible(XRayTransition xrt, double eBeam) {
      try {
         return compute(mMassThickness, mDetectorThickness, xrt.getEnergy()) > 0.001;
      }
      catch(final EPQException e) {
         e.printStackTrace();
         return false;
      }
   }

   public MeasuredCalibration BAMCRMCalibration(EDSDetector det, ISpectrumData spec10keV, ISpectrumData spec30keV)
         throws EPQException, UtilException {
      final Composition comp = new Composition(new Element[] {
         Element.C,
         Element.O,
         Element.Al,
         Element.Ar,
         Element.Mn,
         Element.Cu,
         Element.Zr
      }, new double[] {
         0.2010,
         0.0020,
         0.0590,
         0.0110,
         0.4380,
         0.1180,
         0.1710
      });
      SpectrumFitResult sfr10, sfr30;
      EDSCalibration cal;
      {
         SpectrumFitter8 sf8 = new SpectrumFitter8(det, comp, spec10keV);
         sfr10 = sf8.compute();
         sf8 = new SpectrumFitter8(det, comp, spec30keV);
         sfr30 = sf8.compute();
         cal = sfr30.getCalibration();
      }
      final XRayTransitionSet[] xrts10 = new XRayTransitionSet[] {
         new XRayTransitionSet(Element.C, XRayTransitionSet.K_FAMILY),
         new XRayTransitionSet(Element.Mn, XRayTransitionSet.L_FAMILY),
         new XRayTransitionSet(Element.Cu, XRayTransitionSet.L_FAMILY),
         new XRayTransitionSet(Element.Al, XRayTransitionSet.K_FAMILY),
         new XRayTransitionSet(Element.Zr, XRayTransitionSet.L_FAMILY)
      };
      final XRayTransitionSet[] xrts30 = new XRayTransitionSet[] {
         new XRayTransitionSet(Element.Cu, XRayTransitionSet.K_ALPHA),
         new XRayTransitionSet(Element.Zr, XRayTransitionSet.K_ALPHA),
         new XRayTransitionSet(Element.Zr, XRayTransitionSet.K_BETA)
      };
      final XRayTransitionSet mnKa = new XRayTransitionSet(Element.Mn, XRayTransitionSet.K_ALPHA);
      final double mn10 = sfr10.getIntegratedIntensity(mnKa).doubleValue();
      final double mn30 = sfr30.getIntegratedIntensity(mnKa).doubleValue();
      final Map<XRayTransition, Double> eff = new TreeMap<XRayTransition, Double>();
      for(final XRayTransitionSet xrts : xrts10)
         eff.put(xrts.getWeighiestTransition(), sfr10.getIntegratedIntensity(xrts).doubleValue() / mn10);
      for(final XRayTransitionSet xrts : xrts30)
         eff.put(xrts.getWeighiestTransition(), sfr30.getIntegratedIntensity(xrts).doubleValue() / mn30);
      final MeasuredCalibration res = new MeasuredCalibration(cal.getChannelWidth(), cal.getZeroOffset(), cal.getQuadratic(), cal.getLineshape());
      res.addLayer(MaterialFactory.createCompound("C"), 1.0e-6);
      res.addLayer(MaterialFactory.createCompound("O"), 1.0e-6);
      res.addLayer(MaterialFactory.createCompound("Si"), 1.0e-8);
      res.addLayer(MaterialFactory.createCompound("Al"), 1.0e-9);
      res.addLayer(MaterialFactory.createCompound("Ni"), 1.0e-9);
      res.calibrate(eff);
      return res;
   }
}
