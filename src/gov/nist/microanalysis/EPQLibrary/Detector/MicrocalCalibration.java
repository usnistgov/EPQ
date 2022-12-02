package gov.nist.microanalysis.EPQLibrary.Detector;

import java.util.ArrayList;
import java.util.List;

import gov.nist.microanalysis.EPQLibrary.AlgorithmUser;
import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.MassAbsorptionCoefficient;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Description
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nritchie
 * @version 1.0
 */
public class MicrocalCalibration extends EDSCalibration {

   /**
    * A concrete implementation of the DetectorLineshapeModel using a constant
    * resolution over the range of energies.
    */
   // TODO Come up with a better model for microcalorimeter lineshapes
   static public class BasicMicrocalLineshape extends DetectorLineshapeModel {

      private final double mGaussianWidth;

      public BasicMicrocalLineshape(BasicMicrocalLineshape bml) {
         super();
         mGaussianWidth = bml.mGaussianWidth;
      }

      @Override
      public DetectorLineshapeModel clone() {
         return new BasicMicrocalLineshape(this);
      }

      /**
       * Constructs a simple Gaussina model of a Si(Li) style detector which is
       * characterized by the full-width half-maximum at Mn Kα
       * 
       * @param fwhm
       *           in eV
       */
      public BasicMicrocalLineshape(double fwhm) {
         super();
         assert fwhm > 1.0;
         mGaussianWidth = SpectrumUtils.fwhmToGaussianWidth(fwhm);
      }

      @Override
      public double leftWidth(double eV, double fraction) {
         return mGaussianWidth * Math.sqrt(-2.0 * Math.log(fraction));
      }

      @Override
      public double rightWidth(double eV, double fraction) {
         return mGaussianWidth * Math.sqrt(-2.0 * Math.log(fraction));
      }

      @Override
      public double compute(double eV, double center) {
         return SpectrumUtils.gaussian(eV - center, mGaussianWidth);
      }

      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         long temp;
         temp = Double.doubleToLongBits(mGaussianWidth);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         return result;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (getClass() != obj.getClass())
            return false;
         final BasicMicrocalLineshape other = (BasicMicrocalLineshape) obj;
         if (Double.doubleToLongBits(mGaussianWidth) != Double.doubleToLongBits(other.mGaussianWidth))
            return false;
         return true;
      }

      @Override
      public String toString() {
         return "FWHM = " + SpectrumUtils.gaussianWidthToFWHM(mGaussianWidth);
      }
   }

   private class Layer {
      Material mMaterial;
      double mThickness;

      private Layer(Material mat, double thickness) {
         mMaterial = mat;
         mThickness = thickness;
      }
   };

   private final List<Layer> mLayers = new ArrayList<Layer>();
   /**
    * Secondary windows (radiation exclusion etc.)
    */
   private final List<IXRayWindowProperties> mWindows = new ArrayList<IXRayWindowProperties>();
   private transient double[] mEfficiency;
   private final BasicMicrocalLineshape mLineshape;

   /**
    * Constructs an object representing the calibration of a standard Si(Li)
    * detector.
    */
   public MicrocalCalibration(double chWidth, double offset, double fwhm) {
      super("Microcal", chWidth, offset, new BasicMicrocalLineshape(fwhm));
      // Define the detector as a series of absorbing layers
      try {
         mLayers.add(new Layer(MaterialFactory.createPureElement(Element.Bi), 1.500e-6));
         mLayers.add(new Layer(MaterialFactory.createPureElement(Element.Cu), 0.758e-6));
         mLayers.add(new Layer(MaterialFactory.createPureElement(Element.Mo), 0.048e-6));
         final Material siN = MaterialFactory.createCompound("Si3N4", ToSI.gPerCC(3.29));
         siN.setName("Silicon nitride");
         mLayers.add(new Layer(siN, 0.400e-6));
         // Three 100 nm Al on 100 nm Paralene
         final XRayWindow xrw = new XRayWindow(1.0);
         xrw.addLayer(MaterialFactory.createPureElement(Element.Al), 3 * 100.0e-9);
         xrw.addLayer((Material) MaterialFactory.createMaterial(MaterialFactory.ParaleneC), 3.0 * 100.0e-9);
         mWindows.add(xrw);
         mLineshape = new BasicMicrocalLineshape(fwhm);
      } catch (final EPQException e) {
         throw new EPQFatalException(e);
      }
   }

   private MicrocalCalibration(MicrocalCalibration ucal) {
      this(ucal.getChannelWidth(), ucal.getZeroOffset(), SpectrumUtils.gaussianWidthToFWHM(ucal.mLineshape.mGaussianWidth));
   }

   @Override
   public EDSCalibration clone() {
      return new MicrocalCalibration(this);
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.Detector.DetectorCalibration#getEfficiency(gov.nist.microanalysis.EPQLibrary.Detector.DetectorProperties)
    */
   @Override
   public double[] getEfficiency(DetectorProperties dp) {
      if (mEfficiency == null) {
         final SpectrumProperties sp = dp.getProperties();
         final double[] res = new double[dp.getChannelCount()];
         final double scale = getChannelWidth();
         final double offset = getZeroOffset();
         final MassAbsorptionCoefficient mac = AlgorithmUser.getDefaultMAC();
         final IXRayWindowProperties window = dp.getWindow();
         for (int ch = res.length - 1; ch >= 0; --ch) {
            final double e = ToSI.eV((scale * (ch + 0.5)) + offset);
            double eff = 0.0;
            for (final Layer l : mLayers) {
               final Material mat = l.mMaterial;
               final double abs = 1.0 - Math.exp(-mac.compute(mat, e) * l.mThickness * mat.getDensity());
               eff += abs * (1.0 - eff);
            }
            eff *= window.transmission(e);
            for (final IXRayWindowProperties xrwp : mWindows)
               eff *= xrwp.transmission(e);
            res[ch] = eff;
         }
         // Secondary windows
         // Detector area
         final double area = 1.0e-6 * sp.getNumericWithDefault(SpectrumProperties.DetectorArea, Math2.sqr(0.01));
         mEfficiency = Math2.multiply(area / (4.0 * Math.PI), res);
      }
      return mEfficiency;
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.Detector.DetectorCalibration#isVisible(gov.nist.microanalysis.EPQLibrary.XRayTransition,
    *      double)
    */
   @Override
   public boolean isVisible(XRayTransition xrt, double eBeam) {
      switch (xrt.getFamily()) {
         case AtomicShell.KFamily :
            return (xrt.getElement().getAtomicNumber() >= Element.elmBe) && (xrt.getEdgeEnergy() < (eBeam / 1.3));
         case AtomicShell.LFamily :
            return (xrt.getElement().getAtomicNumber() >= Element.elmSc) && (xrt.getEdgeEnergy() < (eBeam / 1.3));
         case AtomicShell.MFamily :
            return (xrt.getElement().getAtomicNumber() >= Element.elmPm) && (xrt.getEdgeEnergy() < (eBeam / 1.3));
         default :
            return false;
      }
   }

   @Override
   public String toString() {
      return mLineshape.toString() + " - " + super.toString();
   }
}
