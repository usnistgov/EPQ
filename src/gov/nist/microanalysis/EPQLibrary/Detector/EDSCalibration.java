package gov.nist.microanalysis.EPQLibrary.Detector;

import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;

/**
 * <p>
 * The base class for encapsulating EDS calibration parameters. Presumably the
 * description of an EDS detector can be divided into parameters that are
 * constant and those that change (albeit infrequently). By splitting out these
 * two different types of data it is possible to demonstrate continuity
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
abstract public class EDSCalibration extends DetectorCalibration {

   /**
    * Contamination on the face of the detector or one the window acts like an
    * additional window and is thus modeled herein like a window. The window may
    * have ice layers, hydrocarbon oil layers or other forms of contamination
    * that modify the response of the detector.
    */
   protected IXRayWindowProperties mContamination;
   /**
    * Calibrated detector line shape model - includes FWHM and line shape
    * information
    */
   private DetectorLineshapeModel mLineshape;

   /**
    * A user friendly name for this calibration.
    */
   private final String mName;

   /**
    * A global fudge factor used to scale the intensity from simulated spectra
    * to best match measured spectra.
    */
   private double mFudgeFactor;

   /**
    * Constructs an EDSCalibration object suitable for a Si(Li) detector with
    * the specified calibration parameters.
    * 
    * @param scale
    *           Channel width in eV per channel
    * @param offset
    *           Channel offset in eV
    * @param fwhm
    *           FWHM at Mn Ka in eV
    */
   public EDSCalibration(String name, double scale, double offset, double fwhm) {
      super();
      mName = name;
      mContamination = new XRayWindow(1.0);
      mFudgeFactor = 1.0;
      setChannelWidth(scale);
      setZeroOffset(offset);
      setFWHM(fwhm);
   }

   /**
    * Constructs an EDSCalibration object suitable for a Si(Li) detector with
    * the specified calibration parameters.
    * 
    * @param scale
    *           Channel width in eV per channel
    * @param offset
    *           Channel offset in eV
    * @param fanoFactor
    *           Fano factor (nominally 0.122)
    * @param noise
    *           Noise factor
    */
   public EDSCalibration(String name, double scale, double offset, double fanoFactor, double noise) {
      super();
      mName = name;
      mContamination = new XRayWindow(1.0);
      mFudgeFactor = 1.0;
      setChannelWidth(scale);
      setZeroOffset(offset);
      setResolution(fanoFactor, noise);
   }

   /**
    * Constructs an EDSCalibration object suitable for a Si(Li) detector with
    * the specified calibration parameters.
    * 
    * @param scale
    *           Channel width in eV per channel
    * @param offset
    *           Channel offset in eV
    * @param quadratic
    *           Quadratic factor (eV/ch^2)
    * @param fanoFactor
    *           Fano factor (nominally 0.122)
    * @param noise
    *           Noise factor
    */
   public EDSCalibration(String name, double scale, double offset, double quadratic, double fanoFactor, double noise) {
      super();
      mName = name;
      mContamination = new XRayWindow(1.0);
      mFudgeFactor = 1.0;
      setChannelWidth(scale);
      setZeroOffset(offset);
      setQuadratic(quadratic);
      setResolution(fanoFactor, noise);
   }

   private void setLineshape(DetectorLineshapeModel dlm) {
      mLineshape = dlm;
      final double fwhm = mLineshape.getFWHMatMnKa();
      mProperties.setNumericProperty(SpectrumProperties.Resolution, fwhm);
      mProperties.setNumericProperty(SpectrumProperties.ResolutionLine, SpectrumUtils.E_MnKa);
   }

   /**
    * Constructs an EDSCalibration object suitable for a Si(Li) detector with
    * the specified calibration parameters.
    * 
    * @param scale
    *           Channel width in eV per channel
    * @param offset
    *           Channel offset in eV
    * @param dlm
    *           DetectorLineshapeModel
    */
   public EDSCalibration(String name, double scale, double offset, DetectorLineshapeModel dlm) {
      super();
      mName = name;
      mContamination = new XRayWindow(1.0);
      mFudgeFactor = 1.0;
      setChannelWidth(scale);
      setZeroOffset(offset);
      setLineshape(dlm);
   }

   /**
    * Constructs an EDSCalibration object suitable for a Si(Li) detector with
    * the specified calibration parameters.
    * 
    * @param name
    * @param scale
    *           Channel width in eV per channel
    * @param offset
    *           Channel offset in eV
    * @param quadratic
    *           Channel quadratic scale (eV/ch^2)
    * @param dlm
    *           DetectorLineshapeModel
    */
   public EDSCalibration(String name, double scale, double offset, double quadratic, DetectorLineshapeModel dlm) {
      super();
      mName = name;
      mContamination = new XRayWindow(1.0);
      mFudgeFactor = 1.0;
      setChannelWidth(scale);
      setZeroOffset(offset);
      setQuadratic(quadratic);
      setLineshape(dlm);
   }

   public EDSCalibration(EDSCalibration model) {
      super(model);
      mName = model.mName;
      mContamination = model.mContamination.clone();
      mFudgeFactor = 1.0;
      setChannelWidth(model.getChannelWidth());
      setZeroOffset(model.getZeroOffset());
      setLineshape(model.mLineshape.clone());
   }

   private Object readResolve() {
      mHash = Integer.MAX_VALUE;
      mFudgeFactor = (mFudgeFactor == 0.0 ? 1.0 : mFudgeFactor);
      return this;
   }

   @Override
   abstract public EDSCalibration clone();

   public DetectorLineshapeModel getLineshape() {
      return mLineshape;
   }

   public double getChannelWidth() {
      return mProperties.getNumericWithDefault(SpectrumProperties.EnergyScale, Double.NaN);
   }

   public double getZeroOffset() {
      return mProperties.getNumericWithDefault(SpectrumProperties.EnergyOffset, Double.NaN);
   }

   public double getQuadratic() {
      return mProperties.getNumericWithDefault(SpectrumProperties.EnergyQuadratic, 0.0);
   }

   private void setChannelWidth(double cw) {
      mProperties.setNumericProperty(SpectrumProperties.EnergyScale, cw);
   }

   private void setZeroOffset(double off) {
      mProperties.setNumericProperty(SpectrumProperties.EnergyOffset, off);
   }

   private void setQuadratic(double quad) {
      mProperties.setNumericProperty(SpectrumProperties.EnergyQuadratic, quad);
   }

   private void setFWHM(double fwhm) {
      setLineshape(new BasicSiLiLineshape(fwhm));
   }

   private void setResolution(double fano, double noise) {
      setLineshape(new FanoSiLiLineshape(fano, noise));
      mProperties.setNumericProperty(SpectrumProperties.FanoFactor, fano);
   }

   public IXRayWindowProperties getContaminationModel() {
      return mContamination;
   }

   public void setContaminationModel(SpectrumProperties sp, IXRayWindowProperties contamination) {
      mContamination = contamination;
      mProperties.clear(new SpectrumProperties.PropertyId[]{SpectrumProperties.IceThickness, SpectrumProperties.OilThickness});
      mProperties.addAll(sp);
   }

   /**
    * An ad-hoc mechanism for dealing with global intensity (scale) errors
    * 
    * @return double
    */
   public double getFudgeFactor() {
      return mFudgeFactor > 0 ? mFudgeFactor : 1.0;
   }

   /**
    * An ad-hoc mechanism for dealing with global intensity (scale) errors
    */
   public void setFudgeFactor(double ff) {
      mFudgeFactor = ff;
   }

   @Override
   public int hashCode() {
      if (mHash == Integer.MAX_VALUE) {
         final int prime = 31;
         int result = super.hashCode();
         result = (prime * result) + ((mContamination == null) ? 0 : mContamination.hashCode());
         result = (prime * result) + ((mLineshape == null) ? 0 : mLineshape.hashCode());
         result = (prime * result) + ((mName == null) ? 0 : mName.hashCode());
         result = (prime * result) + Double.toString(mFudgeFactor).hashCode();
         if (result == Integer.MAX_VALUE)
            result = Integer.MIN_VALUE;
         mHash = result;
      }
      // System.out.println("EDSC.hashCode()="+Integer.toString(mHash));
      return mHash;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      final EDSCalibration other = (EDSCalibration) obj;
      if (!super.equals(obj))
         return false;
      if (mContamination == null) {
         if (other.mContamination != null)
            return false;
      } else if (!mContamination.equals(other.mContamination))
         return false;
      if (mLineshape == null) {
         if (other.mLineshape != null)
            return false;
      } else if (!mLineshape.equals(other.mLineshape))
         return false;
      if (mName == null) {
         if (other.mName != null)
            return false;
      } else if (!mName.equals(other.mName))
         return false;
      return mFudgeFactor == other.mFudgeFactor;
   }
}
