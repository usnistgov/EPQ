package gov.nist.microanalysis.EPQLibrary.Detector;

import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.Utility.Math2;

import java.util.Arrays;

/**
 * <p>
 * An object to contain the basic properties of a detector. The objects are
 * joined to DetectorCalibration objects to form detectors.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nicholas
 * @version 1.0
 */
public class DetectorProperties implements Cloneable {

   public static final String SILI = "Si(Li)";
   public static final String SDD = "Silicon Drift Detector";
   public static final String MICROCAL = "Microcalorimeter";
   public static final String GE = "Germanium";

   private String mName;
   private SpectrumProperties mProperties;
   private int mChannelCount;
   transient protected ElectronProbe mOwner;
   transient protected int mHash = Integer.MAX_VALUE;
   // Not actually used but kept for legacy purposes
   @SuppressWarnings("unused")
   transient private double[] mPosition = null;

   @Override
   public DetectorProperties clone() {
      return new DetectorProperties(this);
   }

   public DetectorProperties(DetectorProperties dp) {
      this(dp.mOwner, dp.mName, dp.mChannelCount, dp.getPosition());
      mProperties = dp.mProperties.clone();
   }

   public DetectorProperties(ElectronProbe owner, String name, int channelCount, double[] position) {
      mOwner = owner;
      mName = name;
      mChannelCount = channelCount;
      mProperties = new SpectrumProperties();
      setPosition(position);
   }

   private Object readResolve() {
      mHash = Integer.MAX_VALUE;
      return this;
   }

   /**
    * The spectrum properties that are inherited by the spectrum.
    * 
    * @return SpectrumProperties
    */
   public SpectrumProperties getProperties() {
      return mProperties;
   }

   /**
    * Get the window in front of this detector
    * 
    * @return IXRayWindowProperties
    */
   public IXRayWindowProperties getWindow() {
      return mProperties.getWindowWithDefault(SpectrumProperties.DetectorWindow, XRayWindowFactory.createWindow(XRayWindowFactory.NoWindow));
   }

   /**
    * Set the window in front of this detector
    * 
    * @param window
    */
   public void setWindow(IXRayWindowProperties window) {
      mProperties.setWindow(window);
   }

   /**
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return mName;
   }

   /**
    * Provide a new user-friendly name for the detector
    * 
    * @param name
    */
   public void setName(String name) {
      mName = name;
   }

   /**
    * The number of channels of x-ray data in the spectrum
    * 
    * @return int
    */
   public int getChannelCount() {
      return mChannelCount;
   }

   /**
    * Set the number of channels of x-ray data in the spectrum
    */
   public void setChannelCount(int nCh) {
      mChannelCount = nCh;
   }

   /**
    * Set the ElectronProbe which owns this detector.
    * 
    * @return ElectronProbe
    */
   public ElectronProbe getOwner() {
      return mOwner;
   }

   /**
    * Set the ElectronProbe which owns this detector.
    * 
    * @param owner
    */
   public void setOwner(ElectronProbe owner) {
      mOwner = owner;
   }

   /**
    * Set the position of the detector...
    * 
    * @param pos
    *           The position in meters
    */
   public void setPosition(double[] pos) {
      mProperties.setDetectorPosition(pos, 1.0e-3 * mProperties.getNumericWithDefault(SpectrumProperties.DetectorOptWD, Double.NaN));
   }

   /**
    * Returns the position of the detector
    * 
    * @return double[3] in meters!
    */
   public double[] getPosition() {
      return SpectrumUtils.getDetectorPosition(mProperties);
   }

   public static DetectorProperties getDefaultSiLiProperties(ElectronProbe owner, String name, int chCount) {
      final double OPT_WD = 20.0e-3;
      final double TAKE_OFF = Math.toRadians(35.0);
      final double DISTANCE = 50.0e-3;
      final DetectorProperties res = new DetectorProperties(owner, name, chCount, Math2.multiply(0.1, Math2.X_AXIS));
      // Position
      final SpectrumProperties sp = res.mProperties;
      sp.setDetectorPosition(TAKE_OFF, 0.0, DISTANCE, OPT_WD);
      // Detector crystal parameters
      sp.setNumericProperty(SpectrumProperties.DetectorArea, 10.0);
      sp.setTextProperty(SpectrumProperties.DetectorType, SILI);
      sp.setNumericProperty(SpectrumProperties.GoldLayer, 8.0);
      sp.setNumericProperty(SpectrumProperties.DeadLayer, 0.085);
      sp.setNumericProperty(SpectrumProperties.DetectorThickness, 5.0);
      // Window parameters
      sp.setWindow(XRayWindowFactory.createWindow(XRayWindowFactory.Moxtek_AP3_3_Model));
      return res;
   }

   public static DetectorProperties getDefaultSDDProperties(ElectronProbe owner, String name, int chCount) {
      final double OPT_WD = 20.0e-3;
      final double TAKE_OFF = Math.toRadians(35.0);
      final double DISTANCE = 50.0e-3;
      final DetectorProperties res = new DetectorProperties(owner, name, chCount, Math2.X_AXIS);
      // Position
      final SpectrumProperties sp = res.getProperties();
      sp.setDetectorPosition(TAKE_OFF, 0.0, DISTANCE, OPT_WD);
      // Crystal parameters
      sp.setNumericProperty(SpectrumProperties.DetectorArea, 10.0);
      sp.setTextProperty(SpectrumProperties.DetectorType, SDD);
      sp.setNumericProperty(SpectrumProperties.AluminumLayer, 10.0);
      sp.setNumericProperty(SpectrumProperties.GoldLayer, 0.0);
      sp.setNumericProperty(SpectrumProperties.DeadLayer, 0.0);
      sp.setNumericProperty(SpectrumProperties.DetectorThickness, 0.45);
      return res;
   }

   /**
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      if (mHash == Integer.MAX_VALUE) {
         final int PRIME = 31;
         int result = 1;
         result = (PRIME * result) + mChannelCount;
         result = (PRIME * result) + ((mName == null) ? 0 : mName.hashCode());
         result = (PRIME * result) + ((mProperties == null) ? 0 : mProperties.hashCode());
         if (result == Integer.MAX_VALUE)
            result = Integer.MIN_VALUE;
         mHash = result;
      }
      return mHash;
   }

   /**
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      final DetectorProperties other = (DetectorProperties) obj;
      if (mChannelCount != other.mChannelCount)
         return false;
      if (mName == null) {
         if (other.mName != null)
            return false;
      } else if (!mName.equals(other.mName))
         return false;
      if (!Arrays.equals(getPosition(), other.getPosition()))
         return false;
      if (mProperties == null) {
         if (other.mProperties != null)
            return false;
      } else if (!mProperties.equals(other.mProperties))
         return false;
      return true;
   }
}
