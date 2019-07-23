package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.Iterator;

import gov.nist.microanalysis.Utility.Math2;

public class AverageSpectrum
   extends BaseSpectrum {

   private double[] mSum = null;
   private double[] mSumOfSqrs = null;
   private SpectrumProperties mProperties;
   private int mNSpectra = 0;

   public AverageSpectrum() {
      super();
      mProperties = new SpectrumProperties();
      setEnergyScale(0.0, 10.0);
   }

   /**
    * Constructs a AverageSpectrum object by adding all spectra returned by the
    * argument Iterator&lt;ISpectrumData&gt;.
    * 
    * @param specs
    * @throws EPQException
    */
   public AverageSpectrum(Iterator<ISpectrumData> specs)
         throws EPQException {
      super();
      while(specs.hasNext())
         include(specs.next());
   }

   /**
    * Include the specified spectrum in the running average. The spectra must be
    * compatible as defined by SpectrumUtils.areCompatible(...)
    * 
    * @param spec
    */
   public void include(ISpectrumData spec)
         throws EPQException {
      boolean notFirst = true;
      if(mSum == null) {
         mSum = new double[spec.getChannelCount()];
         mSumOfSqrs = new double[spec.getChannelCount()];
         Arrays.fill(mSum, 0.0);
         Arrays.fill(mSumOfSqrs, 0.0);
         mProperties = new SpectrumProperties(spec.getProperties());
         setEnergyScale(spec.getZeroOffset(), spec.getChannelWidth());
         notFirst = false;
      }
      if(SpectrumUtils.areCompatible(this, spec)) {
         if(notFirst)
            mProperties = SpectrumProperties.merge(mProperties, spec.getProperties());
         for(int ch = mSum.length - 1; ch >= 0; --ch) {
            mSum[ch] += spec.getCounts(ch);
            mSumOfSqrs[ch] += Math2.sqr(spec.getCounts(ch));
         }
         ++mNSpectra;
      } else
         throw new EPQException("This spectrum is not compatible with the previous spectra in this running average.");
   }

   /**
    * getChannelCount
    * 
    * @return int
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getChannelCount()
    */
   @Override
   public int getChannelCount() {
      return mSum != null ? mSum.length : 0;
   }

   /**
    * Returns the average number of counts in the specified channel over all
    * included spectra.
    * 
    * @param i
    * @return double
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getCounts(int)
    */
   @Override
   public double getCounts(int i) {
      return mSum != null ? mSum[i] / mNSpectra : 0.0;
   }

   /**
    * Returns the standard deviation of the number of counts in the specified
    * channel over all included spectra.
    * 
    * @param i
    * @return double
    */
   public double getStandardDeviation(int i) {
      final double avg = getCounts(i);
      return Math.sqrt((mSumOfSqrs[i] - (mSum[i] * avg)) / mNSpectra);
   }

   /**
    * getProperties
    * 
    * @return double
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getProperties()
    */
   @Override
   public SpectrumProperties getProperties() {
      return mProperties;
   }
}
