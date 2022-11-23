package gov.nist.microanalysis.EPQLibrary;

import java.text.NumberFormat;

import gov.nist.microanalysis.Utility.PoissonDeviate;

/**
 * <p>
 * Adds Poisson noise to a spectrum. It is assumed that the initial spectrum is
 * "noise free." This function may either be used to simulate short acquisition
 * times given a spectrum with a much longer acquisition time or it may be used
 * to add noise to a theoretically generated spectrum.
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

public class NoisySpectrum
   extends DerivedSpectrum {

   private final double[] mChannels;
   private final double mScale;
   private final int mSeed;

   /**
    * NoisySpectrum - Create a new noisy spectrum from the data in src scaled by
    * the factor scale (nominally 0&lt;scale&lt;=1.0).
    * 
    * @param src ISpectrumData
    * @param scale double
    */
   public NoisySpectrum(ISpectrumData src, double scale, int seed) {
      super(src);
      mScale = scale;
      mSeed = seed;
      mChannels = new double[src.getChannelCount()];
      final PoissonDeviate pd = new PoissonDeviate(seed);
      for(int i = src.getChannelCount() - 1; i >= 0; --i)
         mChannels[i] = src.getCounts(i) > 0 ? pd.randomDeviate(scale * src.getCounts(i)) : 0.0;
      // Now take account for the rescaling in either the live time or the beam
      // current
      final SpectrumProperties sp = getProperties();
      if(sp.isDefined(SpectrumProperties.LiveTime))
         sp.setNumericProperty(SpectrumProperties.LiveTime, sp.getNumericWithDefault(SpectrumProperties.LiveTime, 0.0) * mScale);
      else {
         final double faraday = SpectrumUtils.getAverageFaradayCurrent(sp, 0.0);
         sp.setNumericProperty(SpectrumProperties.ProbeCurrent, faraday * mScale);
      }
   }

   /**
    * getCounts - see ISpectrumData
    * 
    * @param i int
    * @return double
    */
   @Override
   public double getCounts(int i) {
      return mChannels[i];
   }

   @Override
   public String toString() {
      final NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(2);
      return "Noisy[" + mSource.toString() + ", " + nf.format(mScale) + "," + Integer.toString(mSeed) + "]";
   }
}
