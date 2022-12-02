package gov.nist.microanalysis.EPQLibrary.Detector;

import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * ElectronProbe is a simple abstraction of a electron probe, SEM or other
 * electron beam instrument. The geometry is defined relative to the electron
 * beam axis (beam centered). The native axis systems is defined such that the
 * beam travels from negative z to positive z. The zero of the z-axis is defined
 * using some convenient metric. Often this will be the point at which zero
 * working distance occurs. Facing the instrument, the x-axis goes to the right
 * and the y-axis comes towards the viewer (thus forming a right-handed
 * coordinate system.)
 * </p>
 * <p>
 * Each ElectronProbe may have zero or more x-ray detectors attached. Detectors
 * are defined by a location, window descriptions and detector details.
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
public class ElectronProbe implements Comparable<ElectronProbe> {
   // Properties that are shared by all detectors attached to the instrument
   private final SpectrumProperties mProbeProperties;
   private double mMinBeamEnergy = ToSI.keV(5.0);
   private double mMaxBeamEnergy = ToSI.keV(30.0);

   public ElectronProbe(SpectrumProperties props) {
      mProbeProperties = props.clone();
   }

   public ElectronProbe(String name) {
      super();
      mProbeProperties = new SpectrumProperties();
      mProbeProperties.setTextProperty(SpectrumProperties.Instrument, name);
   }

   public SpectrumProperties getProbeProperties() {
      return mProbeProperties;
   }

   /**
    * Min beam energy in Joule
    * 
    * @return double
    */
   public double getMinBeamEnergy() {
      return mMinBeamEnergy;
   }

   /**
    * Min beam energy in Joules
    */
   public void setMinBeamEnergy(double minBeamEnergy) {
      mMinBeamEnergy = minBeamEnergy;
   }

   /**
    * Max beam energy in Joule
    * 
    * @return double
    */
   public double getMaxBeamEnergy() {
      return mMaxBeamEnergy;
   }

   /**
    * Max beam energy in Joules
    */
   public void setMaxBeamEnergy(double maxBeamEnergy) {
      mMaxBeamEnergy = maxBeamEnergy;
   }

   /**
    * Computes the position of the detector assuming that an axis from the
    * optimal working distance (optWD) to the detector at an altitude angle
    * (altitudeAngle) above the x-y plane for the specified distance will find
    * the dispersive element of the detector. The detector is located at an
    * orientation specified by azimuthAngle relative to the nominal x-axis.
    * 
    * @param optWD
    * @param altitudeAngle
    * @param azimuthAngle
    * @param distance
    * @return double[3] with the detector position
    */
   public static double[] computePosition(double optWD, double altitudeAngle, double azimuthAngle, double distance) {
      final double[] res = new double[3];
      res[0] = distance * Math.cos(altitudeAngle) * Math.cos(azimuthAngle);
      res[1] = distance * Math.cos(altitudeAngle) * Math.sin(azimuthAngle);
      res[2] = optWD - (distance * Math.sin(altitudeAngle));
      return res;
   }

   /**
    * Computes the orientation of the detector assuming that the detector points
    * directly towards the point (0,0,optWD) at an altitude and azimuth
    * specified.
    * 
    * @param optWD
    * @param altitudeAngle
    * @param azimuthAngle
    * @param distance
    * @return a double[3] normalized to 1
    */
   public static double[] computeOrientation(double optWD, double altitudeAngle, double azimuthAngle, double distance) {
      return Math2.normalize(Math2.negative(computePosition(optWD, altitudeAngle, azimuthAngle, distance)));
   }

   @Override
   public String toString() {
      return mProbeProperties.getTextWithDefault(SpectrumProperties.Instrument, "Unknown");
   }

   /**
    * Change the name of the ElectronProbe
    * 
    * @param name
    */
   public void setName(String name) {
      mProbeProperties.setTextProperty(SpectrumProperties.Instrument, name);
   }

   @Override
   public int compareTo(ElectronProbe o) {
      return toString().compareTo(o.toString());
   }
}
