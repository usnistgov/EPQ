package gov.nist.microanalysis.EPQLibrary;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import gov.nist.microanalysis.EPQTools.EPQXStream;

/**
 * <p>
 * Implements a set of utility functions that are of use across the full set of
 * ISpectrumData based classes.
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

abstract public class BaseSpectrum
   implements
   ISpectrumData {
   protected static final int NULL_HASH = 0;
   transient protected int mHashCode = NULL_HASH;
   transient private int mSpectrumIndex;
   private static int mNextIndex = 0;

   protected BaseSpectrum() {
      super();
      initializeSpectrumIndex();
   }

   protected void initializeSpectrumIndex() {
      mHashCode = NULL_HASH;
      mSpectrumIndex = mNextIndex++;
   }

   @Override
   public String toString() {
      final SpectrumProperties sp = getProperties();
      String res = sp.getTextWithDefault(SpectrumProperties.SpectrumDisplayName, null);
      if(res == null) {
         res = sp.getTextWithDefault(SpectrumProperties.SpecimenDesc, null);
         if(res == null)
            res = sp.getTextWithDefault(SpectrumProperties.SpectrumComment, null);
         if(res == null)
            res = sp.getTextWithDefault(SpectrumProperties.SpecimenName, null);
         if(res == null)
            res = sp.getTextWithDefault(SpectrumProperties.SourceFileId, null);
         if(res == null)
            res = sp.getTextWithDefault(SpectrumProperties.SourceFile, null);
         if(res != null)
            sp.setTextProperty(SpectrumProperties.SpectrumDisplayName, res);
         else
            res = "Spectrum " + Integer.toHexString(hashCode());
      }
      return res;
   }

   @Override
   public int hashCode() {
      /*
       * We want a hashCode that is unique to this particular instance of the
       * spectrum but will not change as the spectrum data or properties are
       * modified. I can compute hashes from the data and/or the properties but
       * they can change potentially causing havoc in HashSets or similar. Maybe
       * the best alternative is a punt (just use the Object reference converted
       * to an int.)
       */
      if(mHashCode == NULL_HASH) {
         mHashCode = Arrays.hashCode(SpectrumUtils.toDoubleArray(this)) ^ getProperties().hashCode();
         if(mHashCode == NULL_HASH)
            mHashCode = 137;
      }
      return mHashCode;
   }

   /**
    * compareTo - Keeping spectra in the order in which they were constructed.
    * 
    * @param obj
    * @return int
    */
   @Override
   public int compareTo(ISpectrumData obj) {
      assert obj instanceof BaseSpectrum;
      final BaseSpectrum bs = (BaseSpectrum) obj;
      if(mSpectrumIndex < bs.mSpectrumIndex)
         return -1;
      else if(mSpectrumIndex > bs.mSpectrumIndex)
         return +1;
      else
         return 0;
   }

   /**
    * equals - Returns true if two spectra contain identical counts in each
    * channel, the channel width and zero offset are equal and the
    * SpectrumProperties are the same. Note the two spectra may be instances of
    * different classes derived from BaseSpectrum and yet satisfy the criteria
    * for equality. (Equal != Identical)
    * 
    * @param obj Object
    * @return boolean
    */
   @Override
   public boolean equals(Object obj) {
      if(obj instanceof BaseSpectrum) {
         final BaseSpectrum bs = (BaseSpectrum) obj;
         if(hashCode() == bs.hashCode()) {
            if(getChannelWidth() != bs.getChannelWidth())
               return false;
            if(getZeroOffset() != bs.getZeroOffset())
               return false;
            if(bs.getChannelCount() == getChannelCount()) {
               final int cx = getChannelCount();
               for(int i = 0; i < cx; ++i)
                  if(getCounts(i) != bs.getCounts(i))
                     return false;
               return getProperties().equals(bs.getProperties());
            }
         }
      }
      return false;
   }

   /**
    * Write a spectrum object to XML using XStream
    * 
    * @param os
    */
   public void toXML(OutputStream os) {
      EPQXStream.getInstance().toXML(this, os);
   }

   /**
    * Read a spectrum object from XML using XStream
    * 
    * @param input
    * @return BaseSpectrum
    */
   public static BaseSpectrum fromXML(InputStream input) {
      return (BaseSpectrum) EPQXStream.getInstance().fromXML(input);
   }

   @Override
   public double getChannelWidth() {
      return getProperties().getNumericWithDefault(SpectrumProperties.EnergyScale, Double.NaN);
   }

   @Override
   public double getZeroOffset() {
      return getProperties().getNumericWithDefault(SpectrumProperties.EnergyOffset, Double.NaN);
   }

   public void setEnergyScale(double zero, double scale) {
      final SpectrumProperties sp = getProperties();
      sp.setNumericProperty(SpectrumProperties.EnergyOffset, zero);
      sp.setNumericProperty(SpectrumProperties.EnergyScale, scale);
   }

   /**
    * A null spectrum (no counts) for use as a benign error return in certain
    * methods
    */
   static final public BaseSpectrum NullSpectrum = new BaseSpectrum() {
      private SpectrumProperties mProperties;

      @Override
      public int getChannelCount() {
         return 2048;
      }

      @Override
      public double getCounts(int i) {
         return 0.0;
      }

      @Override
      public SpectrumProperties getProperties() {
         if(mProperties == null) {
            mProperties = new SpectrumProperties();
            mProperties.setTextProperty(SpectrumProperties.SpecimenDesc, "Null spectrum");
            setEnergyScale(0.0, 10.0);
         }
         return mProperties;
      }
   };
}
