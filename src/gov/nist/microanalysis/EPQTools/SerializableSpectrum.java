package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;

import java.io.IOException;
import java.io.Serializable;

/**
 * <p>
 * A class to make ISpectrumData instances Serializable.
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
public class SerializableSpectrum extends BaseSpectrum implements Serializable {

   private final static long serialVersionUID = 0x42;

   private double[] mChannels;
   private SpectrumProperties mProperties;

   public SerializableSpectrum() {
      super();
      mChannels = null;
      mProperties = null;
   }

   /**
    * Constructs a SerializableSpectrum based on the specified input spectrum
    */
   public SerializableSpectrum(ISpectrumData src) {
      super();
      mChannels = new double[src.getChannelCount()];
      for (int i = 0; i < mChannels.length; ++i)
         mChannels[i] = src.getCounts(i);
      mProperties = src.getProperties().clone();
      setEnergyScale(src.getZeroOffset(), src.getChannelWidth());
   }

   /**
    * getChannelCount
    * 
    * @return int
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getChannelCount()
    */
   @Override
   public int getChannelCount() {
      return mChannels.length;
   }

   /**
    * getCounts
    * 
    * @param i
    * @return double
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getCounts(int)
    */
   @Override
   public double getCounts(int i) {
      return mChannels[i];
   }

   /**
    * getProperties
    * 
    * @return SpectrumProperties
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getProperties()
    */
   @Override
   public SpectrumProperties getProperties() {
      return mProperties;
   }

   private void writeObject(java.io.ObjectOutputStream out) throws IOException {
      out.writeObject(mProperties);
      out.writeObject(mChannels);
   }

   private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
      initializeSpectrumIndex();
      mProperties = (SpectrumProperties) in.readObject();
      mChannels = (double[]) in.readObject();
   }

}
