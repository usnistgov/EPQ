package gov.nist.microanalysis.EPQLibrary;

/**
 * <p>
 * An interface which all classes that contain spectral data should implement.
 * This interface defines a universal energy dispersive spectrum object which
 * bundles channel data and spectrum properties into a single consistent object.
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

public interface ISpectrumData
   extends Comparable<ISpectrumData> {

   /**
    * getChannelCount - Returns the number of channels in the spectrum
    * (nominally 2048)
    * 
    * @return int
    */
   public int getChannelCount();

   /**
    * getCounts - Return the counts in the i-th channel
    * 
    * @param i
    * @return double
    */
   public double getCounts(int i);

   /**
    * getChannelWidth - Returns the width of each channel (in eV)
    * 
    * @return double
    */
   public double getChannelWidth();

   /**
    * getZeroOffset - Returns the energy of the first channel.
    * 
    * @return double
    */
   public double getZeroOffset();

   /**
    * getProperties - Returns the SpectrumProperties object associated with this
    * spectrum. SpectrumProperties are the mechanism by which contextual data is
    * associated with spectra.
    * 
    * @return SpectrumProperties
    */
   public SpectrumProperties getProperties();
}
