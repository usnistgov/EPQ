package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.EPQLibrary.SpectrumProperties.PropertyId;

/**
 * <p>
 * SpectrumMath makes it easy to add or subtract a multiple of the counts in one
 * spectrum from another spectrum. Good for creating sum spectra or residual
 * spectra. Use AverageSpectrum to compute the average spectra or
 * MaxPixelSpectrum to compute Bright's max-pixel spectrum.
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
public class SpectrumMath extends DerivedSpectrum {
   private final double[] mData;
   private int mNSpectra;

   /**
    * Constructs a SpectrumMath DerivedSpectrum from the specified src file
    * 
    * @param src
    */
   public SpectrumMath(ISpectrumData src) {
      super(src);
      mData = new double[src.getChannelCount()];
      for (int i = src.getChannelCount() - 1; i >= 0; --i)
         mData[i] = src.getCounts(i);
      getProperties().clear(new PropertyId[]{SpectrumProperties.SourceFile});
      getProperties().setTextProperty(SpectrumProperties.SpecimenDesc, src.toString());
      mNSpectra = 1;
   }

   /**
    * Add k times counts in the specified spectrum into the sum spectrum.
    * 
    * @param src
    *           ISpectrumData
    * @param k
    *           double
    */
   public void add(ISpectrumData src, double k) {
      if ((Math.abs(getChannelWidth() - src.getChannelWidth()) * 100.0) > getChannelWidth())
         throw new EPQFatalException("The spectrum " + src.toString() + " does not have similar channel width as " + toString() + ".");
      // Account for differences in zero offset
      final int offset = (int) Math.round((getZeroOffset() - src.getZeroOffset()) / getChannelWidth());
      final int min = Math.max(0, -offset);
      final int max = Math.min(getChannelCount(), src.getChannelCount() + offset);
      assert (max - min) <= getChannelCount();
      for (int i = min; i < max; ++i)
         mData[i] = mData[i] + (k * src.getCounts(i + offset));
      SpectrumProperties sp = getProperties();
      final SpectrumProperties ssp = src.getProperties();
      String name = sp.getTextWithDefault(SpectrumProperties.SpecimenDesc, "Base");
      final double realTime = sp.getNumericWithDefault(SpectrumProperties.RealTime, -Double.MAX_VALUE / 3.0)
            + (k != 0.0 ? k * ssp.getNumericWithDefault(SpectrumProperties.RealTime, -Double.MAX_VALUE / (3.0 * k)) : 0.0);
      final double lt0 = sp.getNumericWithDefault(SpectrumProperties.LiveTime, -Double.MAX_VALUE / 3.0);
      final double lt1 = Math.abs(k) * ssp.getNumericWithDefault(SpectrumProperties.LiveTime, -Double.MAX_VALUE / (3.0 * Math.abs(k)));
      if (mNSpectra < 20) {
         if (k < 0.0) {
            if (k == -1.0)
               name = name + "-" + src.toString();
            else
               name = name + "-" + Double.toString(-k) + "\u00D7" + src.toString();
         } else if (k == 1.0)
            name = name + "+" + src.toString();
         else
            name = name + "+" + Double.toString(k) + "\u00D7" + src.toString();
      } else
         name = "Sum[" + Integer.toString(mNSpectra + 1) + " spectra]";
      double pcb = Double.NaN;
      if ((lt0 + lt1) > 0) {
         if (sp.isDefined(SpectrumProperties.ProbeCurrent) && ssp.isDefined(SpectrumProperties.ProbeCurrent)) {
            final double pc0 = sp.getNumericWithDefault(SpectrumProperties.ProbeCurrent, -Double.MAX_VALUE);
            final double pc1 = ssp.getNumericWithDefault(SpectrumProperties.ProbeCurrent, -Double.MAX_VALUE);
            pcb = ((pc0 * lt0) + (pc1 * lt1)) / (lt0 + lt1);
         }
      }
      final double zo = getZeroOffset(), gain = getChannelWidth();
      mergeProperties(ssp);
      setEnergyScale(zo, gain);
      sp = getProperties();
      if (realTime > 0)
         sp.setNumericProperty(SpectrumProperties.RealTime, realTime);
      if ((lt0 + lt1) > 0)
         sp.setNumericProperty(SpectrumProperties.LiveTime, lt0 + lt1);
      if (!Double.isNaN(pcb))
         sp.setNumericProperty(SpectrumProperties.ProbeCurrent, pcb);
      SpectrumUtils.rename(this, name);
      ++mNSpectra;
   }

   /**
    * add - Subtract k times the counts from the specified spectrum from the sum
    * spectrum.
    * 
    * @param src
    *           ISpectrumData
    * @param k
    */
   public void subtract(ISpectrumData src, double k) {
      add(src, -k);
   }

   /**
    * getCounts - Returns the count in the SpectrumMath spectrum
    * 
    * @param i
    * @return double
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getCounts(int)
    */
   @Override
   public double getCounts(int i) {
      return mData[i];
   }

   public int getSpectrumCount() {
      return mNSpectra;
   }
}
