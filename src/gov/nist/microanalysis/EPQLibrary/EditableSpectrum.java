package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;

/**
 * <p>
 * A base implementation of the ISpectrumData interface for spectra in which the
 * channel data should be mutable.
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

public class EditableSpectrum
   extends BaseSpectrum {
   private final double[] mChannels;
   private double mChannelWidth = Double.NaN;
   private double mZeroOffset = Double.NaN;
   private SpectrumProperties mProperties;

   /**
    * EditableSpectrum - Create a blank editable spectrum.
    * 
    * @param nChannels int
    * @param chWidth double
    * @param zeroOffset double
    */
   public EditableSpectrum(int nChannels, double chWidth, double zeroOffset) {
      super();
      mChannels = new double[nChannels];
      setProperties(new SpectrumProperties());
      setEnergyScale(zeroOffset, chWidth);
   }

   /**
    * Constructs a EditableSpectrum from an array of channel data as doubles
    * 
    * @param chWidth
    * @param zeroOffset
    * @param chData
    */
   public EditableSpectrum(double chWidth, double zeroOffset, double[] chData) {
      super();
      mChannels = chData.clone();
      setProperties(new SpectrumProperties());
      setEnergyScale(zeroOffset, chWidth);
   }

   /**
    * Constructs a EditableSpectrum from an array of channel data as integers
    * 
    * @param chWidth
    * @param zeroOffset
    * @param chData
    */
   public EditableSpectrum(double chWidth, double zeroOffset, int[] chData) {
      this(chWidth, zeroOffset, chData.length, chData);
   }

   /**
    * Constructs a EditableSpectrum from an array of channel data as short
    * integers.
    * 
    * @param chWidth
    * @param zeroOffset
    * @param chData
    */
   public EditableSpectrum(double chWidth, double zeroOffset, short[] chData) {
      this(chWidth, zeroOffset, chData.length, chData);
   }

   /**
    * Constructs a EditableSpectrum from an array of channel data as integers
    * 
    * @param chWidth
    * @param zeroOffset
    * @param nCh
    * @param chData
    */
   public EditableSpectrum(double chWidth, double zeroOffset, int nCh, int[] chData) {
      super();
      mChannels = new double[nCh];
      final int dLen = Math.min(nCh, chData.length);
      for(int i = 0; i < dLen; ++i)
         mChannels[i] = chData[i];
      setProperties(new SpectrumProperties());
      setEnergyScale(zeroOffset, chWidth);
   }

   /**
    * Constructs a EditableSpectrum from an array of channel data as integers
    * 
    * @param chWidth
    * @param zeroOffset
    * @param nCh
    * @param chData
    */
   public EditableSpectrum(double chWidth, double zeroOffset, int nCh, short[] chData) {
      super();
      mChannels = new double[nCh];
      final int dLen = Math.min(nCh, chData.length);
      for(int i = 0; i < dLen; ++i)
         mChannels[i] = chData[i];
      setProperties(new SpectrumProperties());
      setEnergyScale(zeroOffset, chWidth);
   }

   /**
    * setProperties - Set the SpectrumProperty object assigned to this spectrum
    * to the specified SpectrumProperty set.
    * 
    * @param sp
    */
   public void setProperties(SpectrumProperties sp) {
      mProperties = new SpectrumProperties(sp);
      mProperties.setBooleanProperty(SpectrumProperties.IsEdited, true);
   }

   /**
    * EditableSpectrum - Create an editable spectrum that is a duplicate of the
    * specified spectrum.
    * 
    * @param sd ISpectrumData
    */
   public EditableSpectrum(ISpectrumData sd) {
      mChannels = new double[sd.getChannelCount()];
      for(int i = sd.getChannelCount() - 1; i >= 0; --i) {
         mChannels[i] = sd.getCounts(i);
         assert !Double.isNaN(mChannels[i]);
      }
      mProperties = new SpectrumProperties(sd.getProperties());
      setEnergyScale(sd.getZeroOffset(), sd.getChannelWidth());
   }

   @Override
   public void setEnergyScale(double zo, double chW) {
      super.setEnergyScale(zo, chW);
      mZeroOffset = zo;
      mChannelWidth = chW;
   }

   /**
    * getChannelCount - See ISpectrumData
    * 
    * @return int
    */
   @Override
   final public int getChannelCount() {
      return mChannels.length;
   }

   /**
    * getCounts - See ISpectrumData
    * 
    * @param i int
    * @return double
    */
   @Override
   public double getCounts(int i) {
      return (i >= 0) && (i < mChannels.length) ? mChannels[i] : 0.0;
   }

   /**
    * getCounts - Get the channel data as a raw double array. Can be edited
    * directly.
    * 
    * @return double[]
    */
   public double[] getCounts() {
      return mChannels;
   }

   /**
    * setCounts - Set the counts for the specified channel.
    * 
    * @param i int
    * @param counts double
    */
   public void setCounts(int i, double counts) {
      mChannels[i] = counts;
   }

   /**
    * getProperties - See ISpectrumData
    * 
    * @return SpectrumProperties
    */
   @Override
   final public SpectrumProperties getProperties() {
      return mProperties;
   }

   /**
    * clearChannels - Set all channel data to 0.0
    */
   final public void clearChannels() {
      Arrays.fill(mChannels, 0.0);
   }

   final public void add(double[] channels) {
      for(int i = 0; (i < channels.length) && (i < mChannels.length); ++i)
         mChannels[i] += channels[i];
   }

   final public void subtract(double[] channels) {
      for(int i = 0; (i < channels.length) && (i < mChannels.length); ++i)
         mChannels[i] -= channels[i];
   }

   final public void increment(int ch) {
      if((ch >= 0) && (ch < mChannels.length))
         ++mChannels[ch];
   }

   final public void increment(double energy) {
      final int ch = (int) ((energy - mZeroOffset) / mChannelWidth);
      if((ch >= 0) && (ch < mChannels.length))
         ++mChannels[ch];
   }

}