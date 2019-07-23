/**
 * <p>
 * XRayDetector is a basic interface which objects which act as an x-ray
 * detector should implement to work with NISTMonte and SpectrumSimulator.
 * Hypothetically these could be energy dispersive or other types of x-ray
 * detector.
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
package gov.nist.microanalysis.EPQLibrary.Detector;

import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;

import java.awt.event.ActionListener;

/**
 * Defines a very basic interface which x-ray detectors should implement.
 */
public interface IXRayDetector
   extends ActionListener {

   /**
    * Clear the internal ISpectrumData object to prepare for collecting a new
    * spectrum.
    */
   void reset();

   /**
    * Add an x-ray of the specified energy and intensity (1.0 for a full, single
    * x-ray) to the internal ISpectrumData object. The intensity is scaled to
    * account for distance from the point of generation to the detector.
    * 
    * @param energy Joules
    * @param intensity Count of x-rays (fractional ok!)
    */
   void addEvent(double energy, double intensity);

   /**
    * Returns the basic set of properties associated with this detector and the
    * parent instrument.
    * 
    * @return SpectrumProperties
    */
   SpectrumProperties getProperties();

   /**
    * A user friendly name for this detector. Typically does not include the
    * instrument name. (Use toString() to get instrument name + detector name).
    * 
    * @return String
    */
   String getName();

   /**
    * Returns the DetectorProperties object associated with this detector
    */
   DetectorProperties getDetectorProperties();

   /**
    * Returns the DetectorCalibration object associated with this detector
    */
   DetectorCalibration getCalibration();
}