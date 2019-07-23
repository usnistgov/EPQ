package gov.nist.microanalysis.EPQLibrary;

/**
 * <p>
 * An interface describing any class that takes an ISpectrumData as an argument
 * and returns another. It is designed to be used to smooth, filter, linearize a
 * spectrum.
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
public interface ISpectrumTransformation {
   /**
    * This method may defined in any reasonable way that transforms one spectrum
    * into another.
    * 
    * @param spec A ISpectrumData object
    * @return A new ISpectrumData object
    */
   ISpectrumData compute(ISpectrumData spec);
}
