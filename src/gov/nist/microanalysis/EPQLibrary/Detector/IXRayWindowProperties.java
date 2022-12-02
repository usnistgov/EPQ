package gov.nist.microanalysis.EPQLibrary.Detector;

import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;

/**
 * <p>
 * An interface exposing the fundamental properties of an x-ray window.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public interface IXRayWindowProperties extends Cloneable {

   /**
    * transmission - Returns the fractional transmission as a function of the
    * x-ray energy.
    * 
    * @param energy
    *           double - In Joules
    * @return double - [0.0,1.0]
    */
   double transmission(double energy);

   /**
    * Set a user friendly name for the window
    * 
    * @param name
    */
   void setName(String name);

   /**
    * Get a user friendly name for the window
    * 
    * @return String
    */
   String getName();

   @Override
   int hashCode();

   @Override
   boolean equals(Object obj);

   /**
    * Returns the properties associated with this Window
    * 
    * @return SpectrumProperties
    */
   SpectrumProperties getProperties();

   IXRayWindowProperties clone();
}
