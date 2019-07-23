package gov.nist.microanalysis.EPQLibrary;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;

/**
 * <p>
 * A utility class for computing Bright's MaxPixel spectrum. This class computes
 * both a standard MaxPixel and a normalized MaxPixel spectrum as well as
 * MinPixel and normalized MinPixel spectra. Switch usage at any time by using
 * the setMode(...) method. All four spectra are always computed each time a new
 * spectrum is included.
 * </p>
 * <p>
 * The class also supports smoothing the spectrum before it is included through
 * the transformation algorithm.
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas
 * @version 1.0
 */
public class ExtremumSpectrum
   extends BaseSpectrum {

   static final private double NORMALIZATION = 1.0e4;

   static public final int MAX_PIXEL = 0;
   static public final int NORM_MAX_PIXEL = 1;
   static public final int MIN_PIXEL = 2;
   static public final int NORM_MIN_PIXEL = 3;

   private double[] mMaxPixel = null;
   private double[] mNormMaxPixel = null;
   private double[] mMinPixel = null;
   private double[] mNormMinPixel = null;
   private ISpectrumData[] mNormMaxPixelId = null;
   private SpectrumProperties mProperties = new SpectrumProperties();
   private int mNSpectra = 0;
   private int mMode = MAX_PIXEL;
   private ISpectrumTransformation mTransformation = null;

   private void updateSpectrumName() {
      switch(mMode) {
         case MAX_PIXEL:
            mProperties.setTextProperty(SpectrumProperties.SpecimenDesc, "Max-Pixel Spectrum");
            break;
         case NORM_MAX_PIXEL:
            mProperties.setTextProperty(SpectrumProperties.SpecimenDesc, "Normalized Max-Pixel Spectrum");
            break;
         case MIN_PIXEL:
            mProperties.setTextProperty(SpectrumProperties.SpecimenDesc, "Min-Pixel Spectrum");
            break;
         case NORM_MIN_PIXEL:
            mProperties.setTextProperty(SpectrumProperties.SpecimenDesc, "Normalized Min-Pixel Spectrum");
            break;
      }
   }

   /**
    * Constructs an empty MaxPixelSpectrum. Use include(..) to add spectra.
    */
   public ExtremumSpectrum() {
      super();
   }

   public ExtremumSpectrum(Iterator<ISpectrumData> specI)
         throws EPQException {
      super();
      include(specI);
   }

   /**
    * Constructs a MaxPixelSpectrum by applying the specified SpectrumSmoothing
    * algorithm to the specified list of spectra before adding them to the
    * maxPixel spectrum.
    * 
    * @param ss
    * @param specI
    * @throws EPQException
    */
   public ExtremumSpectrum(ISpectrumTransformation ss, Iterator<ISpectrumData> specI)
         throws EPQException {
      super();
      setTransformationAlgorithm(ss);
      include(specI);
   }

   /**
    * Specify a spectrum smoothing or other transformation algorithm to apply to
    * spectra before adding them to the maxPixel spectrum.
    * 
    * @param ss
    */
   public void setTransformationAlgorithm(ISpectrumTransformation ss) {
      mTransformation = ss;
   }

   /**
    * Revert to using no algorithm to smooth spectra before adding them to the
    * maxPixel spectrum.
    */
   public void clearTransformationAlgorithm() {
      mTransformation = null;
   }

   /**
    * Include the specified spectrum in the ExtremumSpectrum using the total
    * counts in <code>spec</code> as the integral. (see
    * <code>include(spec,integral)</code>)
    * 
    * @param spec
    * @throws EPQException
    */
   public void include(ISpectrumData spec)
         throws EPQException {
      include(spec, SpectrumUtils.totalCounts(spec, true));
   }

   /**
    * Include the specified spectrum in the ExtremumSpectrum using the specified
    * number as the integral for scaling purposes.
    * 
    * @param spec
    * @param integral
    * @throws EPQException
    */
   public void include(ISpectrumData spec, double integral)
         throws EPQException {
      boolean notFirst = true;
      if(mTransformation != null)
         spec = mTransformation.compute(spec);
      if(mMaxPixel == null) {
         mMaxPixel = new double[spec.getChannelCount()];
         mNormMaxPixel = new double[spec.getChannelCount()];
         mMinPixel = new double[spec.getChannelCount()];
         mNormMinPixel = new double[spec.getChannelCount()];
         mNormMaxPixelId = new ISpectrumData[spec.getChannelCount()];
         Arrays.fill(mMaxPixel, 0.0);
         Arrays.fill(mNormMaxPixel, 0.0);
         Arrays.fill(mMinPixel, Double.MAX_VALUE);
         Arrays.fill(mNormMinPixel, Double.MAX_VALUE);
         Arrays.fill(mNormMaxPixelId, null);
         setEnergyScale(spec.getZeroOffset(), spec.getChannelWidth());
         notFirst = false;
      }
      if(SpectrumUtils.areCompatible(this, spec)) {
         if(notFirst)
            mProperties = SpectrumProperties.merge(mProperties, spec.getProperties());
         else
            mProperties.addAll(spec.getProperties());
         updateSpectrumName();
         final double norm = integral != 0 ? NORMALIZATION / integral : 1.0;
         for(int ch = mMaxPixel.length - 1; ch >= 0; --ch) {
            final double counts = spec.getCounts(ch);
            if(counts > mMaxPixel[ch])
               mMaxPixel[ch] = counts;
            final double normCounts = counts * norm;
            if(normCounts > mNormMaxPixel[ch]) {
               mNormMaxPixel[ch] = normCounts;
               mNormMaxPixelId[ch] = spec;
            }
            if(counts < mMinPixel[ch])
               mMinPixel[ch] = counts;
            if(normCounts < mNormMinPixel[ch])
               mNormMinPixel[ch] = normCounts;
         }
         ++mNSpectra;
      } else
         throw new EPQException("This spectrum is not compatible with the previous spectra in this running average.");
   }

   public void include(Iterator<ISpectrumData> specs)
         throws EPQException {
      while(specs.hasNext())
         include(specs.next());
   }

   /**
    * Writes a mapping between a range of channels and the spectrum in the
    * normalized max pixel spectrum which produced these channels. Format
    * "###\t###\tspectrum"
    * 
    * @param wr Writer
    * @throws IOException
    */
   public void mapMaxPixelToSpectrum(Writer wr)
         throws IOException {
      ISpectrumData prevSpec = null;
      int prev = -1;
      for(int i = 0; i < mNormMaxPixelId.length; ++i)
         if(mNormMaxPixelId[i] != prevSpec) {
            if(prev != -1)
               wr.write(Integer.toString(prev) + "\t" + Integer.toString(i - 1) + "\t" + prevSpec.toString() + "\n");
            prev = (mNormMaxPixelId[i] != null ? i : -1);
            prevSpec = mNormMaxPixelId[i];
         }
      if((prev != (mNormMaxPixelId.length - 1)) && (prev != -1))
         wr.write(Integer.toString(prev) + "\t" + Integer.toString(mNormMaxPixelId.length - 1) + "\t" + prevSpec.toString()
               + "\n");
      wr.flush();
   }

   /**
    * getChannelCount
    * 
    * @return int
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getChannelCount()
    */
   @Override
   public int getChannelCount() {
      return mMaxPixel.length;
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
      switch(mMode) {
         case MAX_PIXEL:
            return mMaxPixel[i];
         case NORM_MAX_PIXEL:
            return mNormMaxPixel[i];
         case MIN_PIXEL:
            return mMinPixel[i];
         case NORM_MIN_PIXEL:
            return mNormMinPixel[i];
      }
      assert false;
      return 0.0;
   }

   /**
    * Allows the user to specify which spectrum to return. The alternatives are
    * MAX_PIXEL, NORM_MAX_PIXEL, MIN_PIXEL or NORM_MIN_PIXEL. This determines
    * which spectrum is returned by the getCounts method.
    * 
    * @param mode
    */
   public void setMode(int mode) {
      if((mode >= MAX_PIXEL) && (mode <= NORM_MIN_PIXEL) && (mMode != mode)) {
         mMode = mode;
         updateSpectrumName();
      }
   }

   /**
    * Returns the type of spectrum to return. The alternatives are MAX_PIXEL,
    * NORM_MAX_PIXEL, MIN_PIXEL or NORM_MIN_PIXEL. This determines which
    * spectrum is returned by the getCounts method.
    */
   public int getMode() {
      return mMode;
   }

   /**
    * getProperties
    * 
    * @return SpectrumProperties
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getProperties()
    */
   @Override
   public SpectrumProperties getProperties() {
      return mProperties != null ? mProperties : new SpectrumProperties();
   }

   /**
    * Returns the number of spectra included in the MaxPixelSpectrum
    * 
    * @return int
    */
   public int getSpectrumCount() {
      return mNSpectra;
   }
}
