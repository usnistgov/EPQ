package gov.nist.microanalysis.EPQLibrary;

/**
 * <p>
 * A spectrum derived from another spectrum. The number of channels, channel
 * width, zero offset and base properties are all tied together.
 * SpectrumProperties is designed so that that modification of the properties of
 * a DerivedSpectrum will not modify the properties of the base ISpectrumData.
 * </p>
 * <p>
 * DerivedSpectrum is abstract (getCounts()). Usually getCounts() is implemented
 * as some function of mSource.getCounts().
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

abstract public class DerivedSpectrum
   extends BaseSpectrum {

   static class BasicDerivedSpectrum
      extends DerivedSpectrum {

      private final double[] mData;

      public BasicDerivedSpectrum(ISpectrumData src, double[] data, String name) {
         super(src);
         assert data.length == src.getChannelCount();
         mData = data;
         SpectrumUtils.rename(this, name);
      }

      @Override
      public double getCounts(int i) {
         return mData[i];
      }

   }

   final protected ISpectrumData mSource;
   protected SpectrumProperties mProperties = new SpectrumProperties();

   @Override
   public int hashCode() {
      return (mSource.hashCode() ^ mProperties.hashCode()) ^ 0xAAAAAAAA;
   }

   /**
    * DerivedSpectrum - Create a new DerivedSpectrum based on the specified
    * source spectrum.
    * 
    * @param src ISpectrumData
    */
   public DerivedSpectrum(ISpectrumData src) {
      super();
      mSource = src;
      mProperties.addAll(src.getProperties());
      mProperties.setBooleanProperty(SpectrumProperties.IsDerived, true);
   }

   /**
    * getChannelCount - see ISpectrumData
    * 
    * @return int
    */
   @Override
   public int getChannelCount() {
      return mSource.getChannelCount();
   }

   /**
    * getProperties - see ISpectrumData
    * 
    * @return SpectrumProperties
    */
   @Override
   final public SpectrumProperties getProperties() {
      return mProperties;
   }

   /**
    * getBaseSpectrum - Return the spectrum on which this one was based.
    * 
    * @return ISpectrumData
    */
   final public ISpectrumData getBaseSpectrum() {
      return mSource;
   }

   /**
    * Return the counts in specified channel in the source spectrum.
    * 
    * @param ch
    * @return The counts in the <code>ch</code> channel in the source spectrum
    */
   public double getSourceCounts(int ch) {
      return mSource.getCounts(ch);
   }

   /**
    * isDerivedFrom - Is this spectrum derived from the argument spectrum?
    * (Recursively checks the whole chain of derivation if this derived spectrum
    * is derived from yet another.)
    * 
    * @param spec ISpectrumData
    * @return boolean
    */
   public boolean isDerivedFrom(ISpectrumData spec) {
      if(mSource == spec)
         return true;
      if(mSource instanceof DerivedSpectrum)
         return ((DerivedSpectrum) mSource).isDerivedFrom(spec);
      return false;
   }

   public static ISpectrumData ultimateSource(ISpectrumData spec) {
      if(spec instanceof DerivedSpectrum)
         return ultimateSource(((DerivedSpectrum) spec).mSource);
      else
         return spec;
   }

   /**
    * equals - Overrides equals to handle the special case in which the channel
    * data and the SpecimenDesc are the same but the derived spectrum represents
    * some modification of the original. Derived spectra are not considered
    * equal to there base regardless.
    * 
    * @param obj Object
    * @return boolean
    */
   @Override
   public boolean equals(Object obj) {
      if(obj instanceof DerivedSpectrum) {
         final DerivedSpectrum ds = (DerivedSpectrum) obj;
         if((ds.mSource == mSource) && (mProperties.equals(ds.mProperties))) {
            for(int ch = getChannelCount() - 1; ch >= 0; --ch)
               if(ds.getCounts(ch) != getCounts(ch))
                  return false;
            return true;
         } else
            return false;
      } else
         return false;
   }

   /**
    * isAlreadyDerivedFrom - Sometimes it is a bad idea to derive a spectrum
    * from a class from which it is already derived. An example would be the
    * NormalizedSpectrum - Normalizing a spectrum twice is probably an error.
    * 
    * @param cls Class
    * @return boolean
    */
   public boolean isAlreadyDerivedFrom(Class<?> cls) {
      assert (DerivedSpectrum.class.isAssignableFrom(cls));
      if(mSource.getClass().isAssignableFrom(cls))
         return true;
      if(mSource instanceof DerivedSpectrum)
         return ((DerivedSpectrum) mSource).isAlreadyDerivedFrom(cls);
      return false;
   }

   /**
    * Merge the specified SpectrumProperties with the current SpectrumProperties
    * of this spectrum to get a set that is consistent across both.
    * 
    * @param props
    */
   protected void mergeProperties(SpectrumProperties props) {
      mProperties = SpectrumProperties.merge(mProperties, props);
   }

}
