package gov.nist.microanalysis.EPQLibrary;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.Utility.DescriptiveStatistics;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Interval;
import gov.nist.microanalysis.Utility.LinearRegression;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.PoissonDeviate;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * A set of utilities for handling common ISpectrumData related operations.
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

final public class SpectrumUtils {

   static private final double SQRT_2PI = Math.sqrt(2.0 * Math.PI);

   static private final double getMnKa() {
      try {
         return FromSI.eV((new XRayTransition(Element.Mn, XRayTransition.KA1))
               .getEnergy());
      } catch (final Exception ex) {
         System.err.println("Using default energy for Mn Ka.");
         return 5898.7;
      }
   }

   static final public XRayTransition MnKA1 = new XRayTransition(Element.Mn,
         XRayTransition.KA1);
   /**
    * E_MnKa - The energy of the Mn Ka line in eV.
    */
   static final public double E_MnKa = getMnKa();

   static final public double DEFAULT_FANO = 0.120;

   /**
    * areCompatible - Are the channel count, channel width and zero offset on
    * these two spectra equal?
    * 
    * @param sd1
    *           ISpectrumData
    * @param sd2
    *           ISpectrumData
    * @return boolean
    */
   public static boolean areCompatible(ISpectrumData sd1, ISpectrumData sd2) {
      return (sd1.getChannelCount() == sd2.getChannelCount())
            && (sd1.getChannelWidth() == sd2.getChannelWidth())
            && (sd1.getZeroOffset() == sd2.getZeroOffset());
   }

   /**
    * verifyCompatibility - Verifys that two spectra are compatible. If they are
    * compatible the function performs no action. If they are not compatible the
    * functions throws an EPQException specifying the reason.
    * 
    * @param sd1
    *           ISpectrumData
    * @param sd2
    *           ISpectrumData
    * @throws EPQException
    */
   public static void verifyCompatibility(ISpectrumData sd1, ISpectrumData sd2)
         throws EPQException {
      if (sd1.getChannelCount() != sd2.getChannelCount())
         throw new EPQException("The number of channels in " + sd1.toString()
               + " and " + sd2.toString() + " don't match.");
      if (sd1.getChannelWidth() != sd2.getChannelWidth())
         throw new EPQException("The channel widths for " + sd1.toString()
               + " and " + sd2.toString() + " don't match.");
      if (sd1.getZeroOffset() != sd2.getZeroOffset())
         throw new EPQException("The zero offsets for " + sd1.toString()
               + " and " + sd2.toString() + " don't match.");
   }

   /**
    * Tests whether a spectrum and a set of spectrum properties are calibrated
    * similar.
    * 
    * @param sp
    *           A set of spectrum properties
    * @param sd
    *           A spectrum
    * @param tol
    *           Nominally 0.01 for calibrated to within 1%
    * @return true if similarly calibrated, false otherwise
    */
   public static boolean areCalibratedSimilar(SpectrumProperties sp,
         ISpectrumData sd, double tol) {
      final double cw1 = sp
            .getNumericWithDefault(SpectrumProperties.EnergyScale, Double.NaN);
      final double cw2 = sd.getChannelWidth();
      final double zo1 = sp
            .getNumericWithDefault(SpectrumProperties.EnergyOffset, Double.NaN);
      final double zo2 = sd.getZeroOffset();
      return (Math.abs(cw1 - cw2) <= (tol * cw1)) && (Math
            .abs(zo1 - zo2) <= (0.1 * tol * cw1 * sd.getChannelCount()));
   }

   /**
    * channelForEnergy - Returns the index of the channel which contains the
    * specified channel. The channel index may be outside of the bounds of valid
    * channel indices for this spectrum.
    * 
    * @param sd
    *           ISpectrumData
    * @param e
    *           double - the energy in eV
    * @return int - the channel index
    */
   public static int channelForEnergy(ISpectrumData sd, double e) {
      return (int) ((e - sd.getZeroOffset()) / sd.getChannelWidth());
   }

   /**
    * energyForChannel - Compute the energy of the specified channel. The zeroth
    * channel runs from [zeroOffset,zeroOffset+getChannelWidth)]. The first from
    * [zeroOffset+getChannelWidth(),zeroOffset+2*getChannelWidth()) etc. This
    * function returns zeroOffset for channel zero,
    * zeroOffset+sd.getChannelWidth for channel 1, ....
    * 
    * @param sd
    *           ISpectrumData
    * @param ch
    *           int - the channel index
    * @return double - the energy in eV
    */
   public static double minEnergyForChannel(ISpectrumData sd, int ch) {
      return sd.getZeroOffset() + (ch * sd.getChannelWidth());
   }

   /**
    * Returns the average energy in this energy bin.
    * 
    * @param sd
    * @param ch
    * @return in eV
    */
   public static double avgEnergyForChannel(ISpectrumData sd, int ch) {
      return sd.getZeroOffset() + ((ch + 0.5) * sd.getChannelWidth());
   }

   /**
    * Returns the energy on the high side of the specified bin in eV.
    * 
    * @param sd
    * @param ch
    * @return in eV
    */
   public static double maxEnergyForChannel(ISpectrumData sd, int ch) {
      return sd.getZeroOffset() + ((ch + 1) * sd.getChannelWidth());
   }

   /**
    * bound - Returns ch bounded so that it exists on the interval 0 to
    * sd.getChannelCount()-1. If ch is less than zero, it returns 0. If ch is
    * greater than or equal to sd.getChannelCount() then it returns
    * sd.getChannelCount()-1.
    * 
    * @param sd
    *           ISpectrumData
    * @param ch
    *           int
    * @return int
    */
   public static int bound(ISpectrumData sd, int ch) {
      if (ch < 0)
         return 0;
      if (ch >= sd.getChannelCount())
         return sd.getChannelCount() - 1;
      return ch;
   }

   /**
    * The line width measured by a SiLi detector depends upon the energy of the
    * line. Given the width of a reference line at a known energy it is possible
    * to calculate the approximate width of the line at another energy. This
    * function does that using Fiori and Newbury's equation.
    * 
    * @param e
    *           double - In Joules
    * @param fwhmAtE0
    *           double - In Joules
    * @param e0
    *           double - In Joules
    * @return double - The full-width half-maximum in Joules
    */
   public static double linewidth(double e, double fwhmAtE0, double e0) {
      return ToSI
            .eV(linewidth_eV(FromSI.eV(e), FromSI.eV(fwhmAtE0), FromSI.eV(e0)));
   }

   /**
    * <p>
    * The line width measured by a SiLi detector depends upon the energy of the
    * line. Given the width of a reference line at a known energy it is possible
    * to calculate the approximate width of the line at another energy. This
    * function does that using Fiori and Newbury's equation.
    * </p>
    * <p>
    * The initial constant k is 8*ln(2)*Fano*w where w is the electron-hole pair
    * creation energy. Fiori used k=2.5. It is probably closer to 2.42 for SDD
    * and 2.47 for Si(Li) detectors. I've chosen to replace Fiori's 2.5 with
    * 2.45 as something approximately appropriate for either detector type.
    * </p>
    * 
    * @param e
    *           double - In eV
    * @param fwhmAtE0
    *           double - In eV
    * @param e0
    *           double - In eV
    * @return double - Full width half maximum in eV
    */
   public static double linewidth_eV(double e, double fwhmAtE0, double e0) {
      return Math.sqrt((2.45 * (e - e0)) + (fwhmAtE0 * fwhmAtE0));
   }

   /**
    * Converts between full width-half maximum and Gaussian width. (i.e. 2*x
    * such that exp(-0.5 (x/a)^2)=1/2))
    * 
    * @param fwhm
    *           double
    * @return double
    */
   public static double fwhmToGaussianWidth(double fwhm) {
      return fwhm / 2.354820045030949382023138652918; // fwhm/(2*sqrt(-2.0*ln(0.5
      // )))
   }

   /**
    * Converts between full width-half maximum and Gaussian width. (i.e. 2*x
    * such that exp(-0.5 (x/a)^2)=1/2))
    * 
    * @param fwhm
    *           UncertainValue
    * @return UncertainValue
    */
   public static UncertainValue2 fwhmToGaussianWidth(UncertainValue2 fwhm) {
      return UncertainValue2.multiply(1.0 / 2.354820045030949382023138652918,
            fwhm);
   }

   /**
    * Converts between Gaussina width and full width-half maximum (i.e. 2*x such
    * that exp(-0.5 (x/a)^2)=1/2))
    * 
    * @param gaussian
    * @return double
    */
   public static double gaussianWidthToFWHM(double gaussian) {
      return gaussian * 2.354820045030949382023138652918;
      // fwhm/(2*sqrt(-2.0*ln(0.5)))
   }

   /**
    * Converts between Gaussina width and full width-half maximum (i.e. 2*x such
    * that exp(-0.5 (x/a)^2)=1/2))
    * 
    * @param gaussian
    * @return double
    */
   public static UncertainValue2 gaussianWidthToFWHM(UncertainValue2 gaussian) {
      return UncertainValue2.multiply(2.354820045030949382023138652918,
            gaussian);
      // fwhm/(2*sqrt(-2.0*ln(0.5)))
   }

   /**
    * gaussian - Computes the Gaussian function (normalized to an integral of
    * 1.0)
    * 
    * @param dE
    *           double -
    * @param sigma
    *           double - Gaussian width
    * @return double
    */
   public static double gaussian(double dE, double sigma) {
      return Math.exp(-0.5 * Math2.sqr(dE / sigma)) / (sigma * SQRT_2PI);
   }

   /**
    * maxChannel - Returns the highest channel that has the maximum count value.
    * 
    * @param sd
    *           ISpectrumData
    * @return int
    */
   public static int maxChannel(ISpectrumData sd) {
      return maxChannel(sd, 0, sd.getChannelCount());
   }

   /**
    * maxChannel - Returns the highest channel that has the maximum count value.
    * 
    * @param sd
    *           ISpectrumData
    * @param minCh
    *           Low energy channel to start search
    * @param maxCh
    *           One past the high energy channel to stop search
    * @return int
    */
   public static int maxChannel(ISpectrumData sd, int minCh, int maxCh) {
      int res = minCh;
      double max = sd.getCounts(minCh);
      for (int i = minCh + 1; i < maxCh; ++i)
         if (sd.getCounts(i) > max) {
            max = sd.getCounts(i);
            res = i;
         }
      return res;
   }

   /**
    * minChannel - Returns the highest channel that has the minimum count value.
    * 
    * @param sd
    *           ISpectrumData
    * @return int
    */
   public static int minChannel(ISpectrumData sd) {
      int minCh = sd.getChannelCount() - 1;
      double min = sd.getCounts(minCh);
      for (int i = sd.getChannelCount() - 2; i >= 0; --i)
         if (sd.getCounts(i) < min) {
            min = sd.getCounts(i);
            minCh = i;
         }
      return minCh;
   }

   /**
    * Returns the first (smallest) channel in which there is a non-zero channel
    * count. Returns sd.getChannelCount() if all channels are zero.
    * 
    * @param sd
    * @return int Channel index
    */
   public static int firstNonZeroChannel(ISpectrumData sd) {
      int i;
      for (i = 0; i < sd.getChannelCount(); ++i)
         if (sd.getCounts(i) > 0)
            break;
      return i;
   }

   /**
    * Returns the last channel in which there is a non-zero channel count. 0 if
    * all channels are zero.
    * 
    * @param sd
    * @return int Channel index
    */
   public static int lastNonZeroChannel(ISpectrumData sd) {
      int i;
      for (i = sd.getChannelCount(); i > 0; --i)
         if (sd.getCounts(i) > 0)
            break;
      return i;
   }

   /**
    * sumCounts - Returns the number of counts in the channels in the range
    * [minCh, maxCh)
    * 
    * @param sd
    *           ISpectrumData
    * @param minCh
    *           int
    * @param maxCh
    *           int
    * @return double
    */
   public static double sumCounts(ISpectrumData sd, int minCh, int maxCh) {
      double sum = 0;
      for (int i = minCh; i < maxCh; ++i)
         sum += sd.getCounts(i);
      return sum;
   }

   /**
    * Integrate the counts between minE and maxE taking into account the partial
    * channels when minE and maxE don't exactly match channel boundaries.
    * 
    * @param sd
    * @param minE
    *           in eV
    * @param maxE
    *           in eV
    * @return double
    */
   public static double integrate(ISpectrumData sd, double minE, double maxE) {
      final int min = channelForEnergy(sd, minE);
      final int max = channelForEnergy(sd, maxE);
      double res = sumCounts(sd, bound(sd, min), bound(sd, max));
      if ((min >= 0) && (min < sd.getChannelCount()))
         res -= (sd.getCounts(min) * (minE - minEnergyForChannel(sd, min)))
               / sd.getChannelWidth();
      if ((max >= 0) && (max < sd.getChannelCount()))
         res += (sd.getCounts(max) * (maxE - minEnergyForChannel(sd, max)))
               / sd.getChannelWidth();
      return res;
   }

   /**
    * totalCounts - Returns the number of counts in all channels (excluding the
    * LLD)
    * 
    * @param sd
    *           ISpectrumData
    * @param applyLLD
    *           true-&gt; Does not include channels less than or equal to the
    *           LLD
    * @return double
    */
   public static double totalCounts(ISpectrumData sd, boolean applyLLD) {
      int lld = applyLLD ? getZeroStrobeDiscriminatorChannel(sd) : 0;
      double sum = 0;
      for (int i = sd.getChannelCount() - 1; i > lld; --i)
         sum += sd.getCounts(i);
      return sum;
   }

   /**
    * avgCounts - Returns the average number of counts over all channels
    * 
    * @param sd
    *           ISpectrumData
    * @return double
    */
   public static double avgCounts(ISpectrumData sd) {
      double sum = 0;
      for (int i = sd.getChannelCount() - 1; i >= 0; --i)
         sum += sd.getCounts(i);
      return sum / sd.getChannelCount();
   }

   /**
    * getBeamEnergy - Returns the nominal beam energy (in eV) for the specified
    * spectrum. Usually this is the number in the SpectrumProprties.BeamEnergy.
    * When this isn't available the Duane-Hunt limit is used next. When this
    * isn't available the beam energy is estimate from the spectrum data by
    * observing the highest channel with non-zero counts. Finally, all else
    * failing the beam energy is estimated from the number of channels in the
    * spectrum.
    * 
    * @param sd
    *           ISpectrumData
    * @return double - in eV
    */
   public static double getBeamEnergy(ISpectrumData sd) {
      final SpectrumProperties sp = sd.getProperties();
      double res = sp.getNumericWithDefault(SpectrumProperties.BeamEnergy, -1.0)
            * 1000.0;
      if (res < 0.0) {
         res = FromSI.eV(DuaneHuntLimit.DefaultDuaneHunt.compute(sd));
         if (Double.isNaN(res)) {
            int ch = largestNonZeroChannel(sd);
            if (ch < 0)
               ch = sd.getChannelCount();
            res = minEnergyForChannel(sd, ch);
         }
      }
      return res;
   }

   /**
    * getFWHMAtMnKA - Extracts the FWHM at MnKa from the SpectrumProperties
    * associated with the source ISpectrumData. If the ISpectrumData does not
    * define a resolution then the value in the argument def is returned.
    * 
    * @param sp
    *           SpectrumProperties
    * @param def
    *           double - The default value (in eV)
    * @return double - in eV
    */
   public static double getFWHMAtMnKA(SpectrumProperties sp, double def) {
      double res = sp.getNumericWithDefault(SpectrumProperties.Resolution, def);
      final double line = sp
            .getNumericWithDefault(SpectrumProperties.ResolutionLine, E_MnKa);
      if (line != SpectrumUtils.E_MnKa)
         res = linewidth_eV(SpectrumUtils.E_MnKa, res, line);
      return res;
   }

   /**
    * getFWHMAtMnKA - Extracts the FWHM at MnKa from the source ISpectrumData.
    * If the ISpectrumData does not define a resolution then the value in the
    * argument def is returned.
    * 
    * @param spec
    *           ISpectrumData
    * @param def
    *           double - The default value (in eV)
    * @return double - in eV
    */
   public static double getFWHMAtMnKA(ISpectrumData spec, double def) {
      return getFWHMAtMnKA(spec.getProperties(), def);
   }

   /**
    * dotProduct - Returns the dot product of the channels of each specified
    * spectrum.
    * 
    * @param spec1
    *           ISpectrumData
    * @param spec2
    *           ISpectrumData
    * @return double
    */
   public static double dotProduct(ISpectrumData spec1, ISpectrumData spec2) {
      assert (spec1.getChannelCount() == spec2.getChannelCount());
      double res = 0.0;
      for (int i = spec1.getChannelCount() - 1; i >= 0; --i)
         res += spec1.getCounts(i) * spec2.getCounts(i);
      return res;
   }

   /**
    * toDoubleArray - Returns the spectrum channel data as an array of doubles.
    * 
    * @param spec
    * @return double[]
    */
   final static public double[] toDoubleArray(ISpectrumData spec) {
      final double[] res = new double[spec.getChannelCount()];
      for (int i = 0; i < res.length; ++i)
         res[i] = spec.getCounts(i);
      return res;
   }

   /**
    * Extract the data from a range of channels from the specified spectrum.
    * 
    * @param spec
    *           The spectrum
    * @param lowCh
    *           The lowCh (can be &lt; 0)
    * @param count
    *           The number of channels to output (lowCh+count can be greater
    *           than spec.getChannelCount)
    * @return double[] An array of count items (0 if index out-of-range)
    */
   final static public double[] slice(ISpectrumData spec, int lowCh,
         int count) {
      final double[] res = new double[count];
      final int upper = Math.min(count, spec.getChannelCount() - lowCh);
      for (int i = Math.max(0, -lowCh); i < upper; ++i)
         res[i] = spec.getCounts(i + lowCh);
      return res;
   }

   /**
    * Convert a double[] into a SpectrumObject
    * 
    * @param chWidth
    *           In eV/channel
    * @param zeroOffset
    *           In eV
    * @param data
    *           In x-ray events or similar
    * @return ISpectrumData
    */
   static public ISpectrumData toSpectrum(double chWidth, double zeroOffset,
         double[] data) {
      return toSpectrum(chWidth, zeroOffset, data.length, data);
   }

   /**
    * Convert a double[] into a SpectrumObject
    * 
    * @param chWidth
    *           In eV/channel
    * @param zeroOffset
    *           In eV
    * @param nCh
    *           Number of channels in resulting spectrum
    * @param data
    *           In x-ray events or similar
    * @return ISpectrumData
    */
   static public ISpectrumData toSpectrum(double chWidth, double zeroOffset,
         int nCh, double[] data) {
      class NewSpectrum extends BaseSpectrum {
         private final SpectrumProperties mProperties;
         private final double[] mChannels;

         NewSpectrum(double chWidth, double zeroOffset, int nCh,
               double[] data) {
            super();
            mProperties = new SpectrumProperties();
            setEnergyScale(zeroOffset, chWidth);
            mChannels = new double[nCh];
            System.arraycopy(data, 0, mChannels, 0,
                  Math.min(data.length, mChannels.length));
            mProperties.setTextProperty(SpectrumProperties.SpectrumDisplayName,
                  "Converted");
         }

         /**
          * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getChannelCount()
          */
         @Override
         public int getChannelCount() {
            return mChannels.length;
         }

         /**
          * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getCounts(int)
          */
         @Override
         public double getCounts(int i) {
            return mChannels[i];
         }

         /**
          * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getProperties()
          */
         @Override
         public SpectrumProperties getProperties() {
            return mProperties;
         }

      }
      return new NewSpectrum(chWidth, zeroOffset, nCh, data);
   }

   /**
    * toIntArray - Returns the spectrum channel data as an array of int.
    * 
    * @param spec
    * @return int[]
    */
   static public int[] toIntArray(ISpectrumData spec) {
      final int[] res = new int[spec.getChannelCount()];
      for (int i = spec.getChannelCount() - 1; i >= 0; --i)
         res[i] = (int) spec.getCounts(i);
      return res;
   }

   /**
    * energyArray - Creates an array of doubles representing the energy axis.
    * 
    * @param sd
    *           A spectrum
    * @return double[] of energies in eV
    */
   public static double[] energyArray(ISpectrumData sd) {
      final int c = sd.getChannelCount();
      final double[] res = new double[c];
      for (int i = 0; i < c; ++i)
         res[i] = SpectrumUtils.avgEnergyForChannel(sd, i);
      return res;
   }

   /**
    * toColor - Converts a spectrum to a color in a manner that is inspired by
    * the way our eyes convert multiple wavelengths into three signals.
    * 
    * @param spec
    * @return Color
    */
   static public Color toColor(ISpectrumData spec) {
      final double pivot = 2.5;
      double r = 0.0, g = 0.0, b = 0.0, rn = 0.0, gn = 0.0, bn = 0.0;
      for (int i = spec.getChannelCount() - 1; i >= 0; --i) {
         final double e = avgEnergyForChannel(spec, i) / 1000.0;
         final double cx = spec.getCounts(i);
         final double c = Math.log(cx > 1.0 ? cx : 1.0);
         // red
         if ((e >= 0) && (e < pivot)) {
            r += (c * e) / pivot;
            rn += c;
         }
         // red & green
         if ((e >= pivot) && (e < (2.0 * pivot))) {
            r += (c * ((2.0 * pivot) - e)) / pivot;
            g += (c * (e - pivot)) / pivot;
            rn += c;
            gn += c;
         }
         // green & blue
         if ((e >= (2.0 * pivot)) && (e < (3.0 * pivot))) {
            g += (c * ((3.0 * pivot) - e)) / pivot;
            b += (c * (e - (2.0 * pivot))) / pivot;
            gn += c;
            bn += c;
         }
         // blue
         if ((e >= (3.0 * pivot)) && (e < (4.0 * pivot))) {
            b += (c * ((4.0 * pivot) - e)) / pivot;
            bn += c;
         }
      }
      final double n = Math.max(rn, Math.max(gn, bn));
      return new Color((float) (r / n), (float) (g / n), (float) (b / n));
   }

   /**
    * Displays the spectrum as a grey-scale bitmap. The width of the bitmap in
    * pixel is the number of channels. The height is as specified.
    * 
    * @param spec
    * @param height
    * @return BufferedImage
    */
   static public BufferedImage toStrip(ISpectrumData spec, int height) {
      final int cc = spec.getChannelCount();
      final BufferedImage img = new BufferedImage(cc, height,
            BufferedImage.TYPE_BYTE_GRAY);
      final WritableRaster wr = img.getRaster();
      final DataBufferByte dbb = (DataBufferByte) wr.getDataBuffer();
      final byte[] buffer = dbb.getData();
      final double max = spec.getCounts(SpectrumUtils.maxChannel(spec));
      for (int ch = cc - 1; ch >= 0; ch--)
         // buffer[ch]=(byte)Math2.bound((int) Math.round(255.0 * (1.0 +
         // Math.log10(spec.getCounts(ch)/max))),0,256);
         buffer[ch] = (byte) Math2.bound(
               (int) Math.round(255.0 * Math.sqrt(spec.getCounts(ch) / max)), 0,
               256);
      // buffer[ch]=(byte)Math2.bound((int) Math.round(255.0 *
      // spec.getCounts(ch)/max),0,256);
      for (int h = 1; h < height; ++h)
         System.arraycopy(buffer, 0, buffer, h * cc, cc);
      img.setData(wr);
      return img;
   }

   /**
    * Displays the spectrum as a grey-scale bitmap. The width of the bitmap in
    * pixel is the number of channels. The height is as specified.
    * 
    * @param spec
    *           An ISpectrumData
    * @param eMin
    *           in eV
    * @param eMax
    *           in eV
    * @param width
    *           Width of the bitmap in pixels
    * @param height
    *           Height of the bitmap in pixels
    * @return BufferedImage
    */
   static public BufferedImage toStrip(ISpectrumData spec, double eMin,
         double eMax, int width, int height) {
      final int chMin = SpectrumUtils.channelForEnergy(spec, eMin);
      final int chMax = SpectrumUtils.channelForEnergy(spec, eMax) + 1;
      final int cc = chMax - chMin;
      final BufferedImage img = new BufferedImage(width, height,
            BufferedImage.TYPE_BYTE_GRAY);
      final WritableRaster wr = img.getRaster();
      final DataBufferByte dbb = (DataBufferByte) wr.getDataBuffer();
      final byte[] buffer = dbb.getData();
      final double max = spec
            .getCounts(SpectrumUtils.maxChannel(spec, chMin, chMax));
      if (cc > width) {
         // More channels than pixels
         int minCh = chMin, maxCh;
         for (int x = 0; x < width; ++x) {
            maxCh = (int) Math
                  .round((double) ((x + 1) * cc) / (double) (width - 1));
            double i = 0.0;
            for (int ch = minCh; ch < maxCh; ++ch)
               i += spec.getCounts(ch);
            i /= (maxCh - minCh);
            buffer[x] = (byte) Math2
                  .bound((int) Math.round(255.0 * Math.sqrt(i / max)), 0, 256);
            minCh = maxCh;
         }
      } else {
         // More pixels than channels
         int minPix = 0, maxPix;
         for (int ch = chMin; ch < chMax; ++ch) {
            final double i = spec.getCounts(ch);
            maxPix = (((ch - chMin) + 1) * width) / (chMax - chMin);
            for (int x = minPix; x < maxPix; ++x)
               buffer[x] = (byte) Math2.bound(
                     (int) Math.round(255.0 * Math.sqrt(i / max)), 0, 256);
            minPix = maxPix;
         }
      }
      for (int h = 1; h < height; ++h)
         System.arraycopy(buffer, 0, buffer, h * width, width);
      img.setData(wr);
      return img;
   }

   static public BufferedImage toStrips(Collection<ISpectrumData> specs,
         int height) {
      final int cc = specs.iterator().next().getChannelCount();
      final BufferedImage img = new BufferedImage(cc,
            (specs.size() * (height + 1)) - 1, BufferedImage.TYPE_BYTE_GRAY);
      int p = 0;
      final Graphics2D gr = img.createGraphics();
      for (final ISpectrumData spec : specs) {
         gr.drawImage(toStrip(spec, 1), 0, p, img.getWidth(), height, null);
         p += height + 1;
      }
      return img;
   }

   static public BufferedImage toThumbnail(ISpectrumData spec, int maxCh,
         int width, int height) {
      if (maxCh > spec.getChannelCount())
         maxCh = spec.getChannelCount();
      final double[] min = new double[width], max = new double[width];
      double allMax = -Double.MAX_VALUE;
      int j0 = 0;
      for (int i = 0; i < width; ++i) {
         final int j1 = ((i + 1) * maxCh) / width;
         min[i] = Double.MAX_VALUE;
         max[i] = -Double.MAX_VALUE;
         for (int j = j0; j < j1; ++j) {
            final double c = spec.getCounts(j);
            if (c < min[i])
               min[i] = c;
            if (c > max[i])
               max[i] = c;
            if (c > allMax)
               allMax = c;
         }
         j0 = j1;
      }
      final BufferedImage img = new BufferedImage(width, height,
            BufferedImage.TYPE_BYTE_GRAY);
      final Graphics2D g = img.createGraphics();
      g.setColor(Color.white);
      g.fillRect(0, 0, width, height);
      g.setColor(Color.lightGray);
      {
         final int div = 4;
         for (int i = 1; i < div; ++i)
            g.drawLine(0, (height * i) / div, width, (height * i) / div);
      }
      {
         final int div = 5;
         for (int i = 1; i < div; ++i)
            g.drawLine((width * i) / div, 0, (width * i) / div, height);
      }
      g.setColor(Color.black);

      for (int i = 0; i < width; ++i) {
         final int maxI = (height - 2)
               - (int) Math.round((max[i] / allMax) * (height - 2));
         g.drawLine(i, height - 1, i, maxI);
      }
      g.setColor(Color.gray);
      g.drawLine(0, height - 1, width, height - 1);
      g.drawLine(0, 0, 0, height - 1);
      int hh = 0;
      final Composition comp = SpectrumUtils.getComposition(spec);
      if (comp != null) {
         g.setFont(new Font("sans", Font.PLAIN, (32 * width) / 256));
         final Rectangle r = g.getFontMetrics()
               .getStringBounds(comp.toString(), g).getBounds();
         hh = Math.min(height, r.height);
         g.drawString(comp.toString(), Math.max(0, width - r.width), hh);
      }
      final double e0 = spec.getProperties()
            .getNumericWithDefault(SpectrumProperties.BeamEnergy, Double.NaN);
      if (!Double.isNaN(e0)) {
         final NumberFormat df = new HalfUpFormat("0.0 keV");
         final String tmp = df.format(e0);
         g.setFont(new Font("sans", Font.PLAIN, (24 * width) / 256));
         final Rectangle r = g.getFontMetrics().getStringBounds(tmp, g)
               .getBounds();
         g.drawString(tmp, Math.max(0, width - r.width),
               Math.min(height, hh + r.height));
      }
      return img;
   }

   /**
    * energyToWavelength - Converts energy to wavelength
    * 
    * @param energy
    *           Joules
    * @return In Meters
    */
   static public double energyToWavelength(double energy) {
      return (PhysicalConstants.PlanckConstant * PhysicalConstants.SpeedOfLight)
            / energy;
   }

   /**
    * wavelengthToEnergy - Converts wavelength to energy
    * 
    * @param wavelength
    *           - Meters
    * @return Joules
    */
   static public double wavelengthToEnergy(double wavelength) {
      return (PhysicalConstants.PlanckConstant * PhysicalConstants.SpeedOfLight)
            / wavelength;
   }

   /**
    * energyToWavenumber - Converts energy to wavelength
    * 
    * @param energy
    *           Joules
    * @return Meters<sup>-1</sup>
    */
   static public double energyToWavenumber(double energy) {
      return (2.0 * Math.PI * energy) / (PhysicalConstants.PlanckConstant
            * PhysicalConstants.SpeedOfLight);
   }

   /**
    * spectrumDataToText - Convert the spectral data in the specified energy
    * range into a String desscribing the raw channel data.
    * 
    * @param sd
    *           - The spectrum data
    * @param eLow
    *           - In eV
    * @param eHigh
    *           - In eV
    * @param withEnergies
    *           - Should the string contain the energy for each bin
    * @return String
    */
   static public String spectrumDataToText(ISpectrumData sd, double eLow,
         double eHigh, boolean withEnergies) {
      final StringBuffer sb = new StringBuffer();
      final int min = SpectrumUtils.bound(sd,
            SpectrumUtils.channelForEnergy(sd, eLow));
      // Finds the end (right side) of the highlighted region
      final int max = SpectrumUtils.bound(sd,
            SpectrumUtils.channelForEnergy(sd, eHigh));
      // for each data point in the region, write out the energy and
      // the count at that specific point
      if (withEnergies)
         for (int i = min; i < max; i++)
            sb.append(SpectrumUtils.minEnergyForChannel(sd, i) + '\t'
                  + SpectrumUtils.maxEnergyForChannel(sd, i) + '\t'
                  + sd.getCounts(i) + '\n');
      else
         for (int i = min; i < max; i++)
            sb.append(sd.getCounts(i) + '\n');
      return sb.toString();
   }

   /**
    * spectrumPropertiesToText - Converts a SpectrumProperties object into a
    * String containing a property per line. Each line consists of the 'property
    * name' = 'text description'
    * 
    * @param sp
    *           The SpectrumProperties object
    * @return String
    */
   static public String spectrumPropertiesToText(SpectrumProperties sp) {
      final StringBuffer sb = new StringBuffer();
      for (final Object element : sp.getPropertySet()) {
         final SpectrumProperties.PropertyId pid = (SpectrumProperties.PropertyId) element;
         sb.append(pid.toString());
         sb.append(" = ");
         try {
            sb.append(sp.getTextProperty(pid));
         } catch (final EPQException e) {
            sb.append("**Undefined**");
         }
         sb.append('\n');
      }
      return sb.toString();
   }

   /**
    * <p>
    * Create a new ISpectrumData object containing a spectrum derived from the
    * specified spectrum. The specified spectrum must have the
    * SpectrumProperties.LiveTime property defined and the argument liveTime
    * must be less than or equal to the numeric value in
    * SpectrumProperties.LiveTime
    * </p>
    * <p>
    * The strategy used by this method is not particularly CPU efficient but
    * seems to be strictly statistically rigorous. Randomly assign a time to
    * each x-ray event. If the time is less than liveTime then accept the event;
    * otherwise reject it. This mechanims scales perfectly from infinitesimal
    * acquisition times all the way to the full source spectrum live time.
    * </p>
    * 
    * @param sd
    * @param liveTime
    * @param seed
    *           Random number seed
    * @return EditableSpectrum
    * @throws EPQException
    *            If SpectrumProperties.LiveTime not defined or if liveTime too
    *            large.
    */
   public static EditableSpectrum subSampleSpectrum(ISpectrumData sd,
         double liveTime, long seed) throws EPQException {
      final int nCh = sd.getChannelCount();
      final double lt = sd.getProperties()
            .getNumericProperty(SpectrumProperties.LiveTime);
      final Random r = new Random(seed);
      if (liveTime > lt)
         throw new EPQException(
               "The sub-sampled live time must be less than the source spectrum live time.");
      final EditableSpectrum res = new EditableSpectrum(sd);
      final double k = liveTime / lt;
      for (int i = 0; i < nCh; ++i) {
         final int nTot = (int) Math.round(sd.getCounts(i));
         int n = 0;
         for (int j = 0; j < nTot; ++j)
            if (r.nextDouble() < k)
               ++n;
         res.setCounts(i, n);
      }
      {
         final NumberFormat nf = NumberFormat.getInstance();
         nf.setMaximumFractionDigits(2);
         res.getProperties().setTextProperty(
               SpectrumProperties.SpectrumDisplayName, "Subsampled["
                     + sd.toString() + "," + nf.format(liveTime) + " s]");
         res.getProperties().setNumericProperty(SpectrumProperties.LiveTime,
               liveTime);
      }
      return res;
   }

   /**
    * <p>
    * Create a new ISpectrumData object containing a spectrum derived from the
    * specified spectrum. The specified spectrum must have the
    * SpectrumProperties.LiveTime property defined and the argument liveTime
    * must be less than or equal to the numeric value in
    * SpectrumProperties.LiveTime
    * </p>
    * <p>
    * The strategy used by this method is not particularly CPU efficient but
    * seems to be strictly statistically rigorous. Randomly assign a time to
    * each x-ray event. If the time is less than liveTime then accept the event;
    * otherwise reject it. This mechanims scales perfectly from infinitesimal
    * acquisition times all the way to the full source spectrum live time.
    * </p>
    * 
    * @param sd
    * @param liveTime
    * @return EditableSpectrum
    * @throws EPQException
    *            If SpectrumProperties.LiveTime not defined or if liveTime too
    *            large.
    */
   public static EditableSpectrum subSampleSpectrum(ISpectrumData sd,
         double liveTime) throws EPQException {
      return subSampleSpectrum(sd, liveTime, System.currentTimeMillis());
   }

   /**
    * Creates a new spectrum from the specified source spectrum by adding
    * Poissonian simulated counting statistic noise. This method is appropriate
    * for adding noise to an analytically simulated spectrum such as computed by
    * SpectrumSimulator or NISTMonte. If you apply this method to a real
    * spectrum, the resulting spectrum will have too much noise. Use
    * subSampleSpectrum instead to rescale a 'real' spectrum.
    * 
    * @param src
    *           The source spectrum
    * @param k
    *           A multipler used to linearly rescale the source spectrum data
    * @return EditableSpectrum
    */
   public static EditableSpectrum addNoiseToSpectrum(ISpectrumData src,
         double k) {
      final EditableSpectrum res = new EditableSpectrum(src);
      final int nCh = res.getChannelCount();
      final PoissonDeviate pd = new PoissonDeviate(
            Math.round((Math.random() - 0.5) * Long.MAX_VALUE));
      for (int i = 0; i < nCh; ++i) {
         final double counts = Math.round(k * src.getCounts(i));
         res.setCounts(i, counts > 0.0 ? pd.randomDeviate(counts) : 0.0);
      }
      final SpectrumProperties sp = res.getProperties();
      if (sp.isDefined(SpectrumProperties.LiveTime))
         sp.setNumericProperty(SpectrumProperties.LiveTime,
               k * sp.getNumericWithDefault(SpectrumProperties.LiveTime, 0.0));
      else {
         if (sp.isDefined(SpectrumProperties.FaradayBegin))
            sp.setNumericProperty(SpectrumProperties.FaradayBegin, k * sp
                  .getNumericWithDefault(SpectrumProperties.FaradayBegin, 0.0));
         if (sp.isDefined(SpectrumProperties.FaradayEnd))
            sp.setNumericProperty(SpectrumProperties.FaradayEnd, k * sp
                  .getNumericWithDefault(SpectrumProperties.FaradayEnd, 0.0));
      }
      rename(res, "Noisy[" + src.toString() + "]");
      return res;
   }

   /**
    * <p>
    * Randomly subdivides the src spectrum into nParts different spectra. The
    * counts in src are randomly assigned to the resulting spectra. This method
    * is interesting because the resulting spectra are similar in character to
    * the src spectrum at 1.0/nParts of the acquisition live time.
    * </p>
    * <p>
    * Consider if you partition a spectrum into 4 parts. If each of these parts
    * remains in the same class as the source spectrum then the source spectrum
    * is likely to be firmly within the class. However if one or more of the
    * subdivided spectra are not in same class then it is likely the final
    * spectrum is close to the edge of the class.
    * </p>
    * 
    * @param src
    *           The source spectrum
    * @param nParts
    *           The number of spectra into which to subdivide the source
    *           spectrum
    * @return An array of spectra
    */
   public static ISpectrumData[] partition(ISpectrumData src, int nParts) {
      assert nParts > 0;
      final EditableSpectrum[] res = new EditableSpectrum[nParts];
      for (int i = 0; i < nParts; ++i) {
         res[i] = new EditableSpectrum(src);
         res[i].clearChannels();
      }
      final long[] ch = new long[nParts];
      final Random r = new Random();
      for (int c = src.getChannelCount() - 1; c >= 0; --c) {
         Arrays.fill(ch, 0);
         final long n = Math.round(src.getCounts(c));
         for (long i = n - 1; i >= 0; --i)
            ++ch[r.nextInt(nParts)];
         for (int i = 0; i < nParts; ++i)
            res[i].setCounts(c, ch[i]);
      }
      final SpectrumProperties sp = src.getProperties();
      final double lt = sp.getNumericWithDefault(SpectrumProperties.LiveTime,
            Double.NaN);
      final double rt = sp.getNumericWithDefault(SpectrumProperties.RealTime,
            Double.NaN);
      for (int i = 0; i < nParts; ++i) {
         rename(res[i],
               "Partition[" + src.toString() + "," + Integer.toString(i) + "]");
         if (!Double.isNaN(lt))
            res[i].getProperties()
                  .setNumericProperty(SpectrumProperties.LiveTime, lt / nParts);
         if (!Double.isNaN(rt))
            res[i].getProperties()
                  .setNumericProperty(SpectrumProperties.RealTime, rt / nParts);
      }
      return res;
   }

   public static void rename(ISpectrumData spec, String name) {
      final SpectrumProperties props = spec.getProperties();
      props.setTextProperty(SpectrumProperties.SpecimenDesc, name);
      props.setTextProperty(SpectrumProperties.SpectrumDisplayName, name);
   }

   /**
    * Returns the index of the smallest channel containing non-zero counts
    * 
    * @param spec
    * @return int
    */
   public static int smallestNonZeroChannel(ISpectrumData spec) {
      final SpectrumProperties sp = spec.getProperties();
      int lld = 0;
      if (sp.isDefined(SpectrumProperties.ZeroPeakDiscriminator)) {
         final double zpd = sp.getNumericWithDefault(
               SpectrumProperties.ZeroPeakDiscriminator, 0.0);
         lld = SpectrumUtils.channelForEnergy(spec, zpd);
      }
      int res = spec.getChannelCount();
      for (int i = lld; i < res; ++i)
         if (spec.getCounts(i) != 0.0) {
            res = i;
            break;
         }
      return res;
   }

   /**
    * Returns the index of the largest channel containing non-zero counts
    * 
    * @param spec
    * @return int
    */
   public static int largestNonZeroChannel(ISpectrumData spec) {
      int res = spec.getChannelCount();
      for (int i = spec.getChannelCount() - 1; i > 0; --i)
         if (spec.getCounts(i) != 0.0) {
            res = i;
            break;
         }
      return res;
   }

   /**
    * Estimate the background starting at channel <code>min</code> heading
    * towards channel 0 by averaging at least 3 but as many as 100 channels
    * together to minimize the error estimate in the background level.
    * 
    * @param spec
    * @param min
    * @return double[2] { average, error, nChannels }
    */
   static public double[] estimateLowBackground(ISpectrumData spec, int min) {
      int bestCh = 0;
      double bestRes = spec.getCounts(bestCh);
      double bestErr = bestRes > 1 ? Math.sqrt(bestRes) : 1.0;
      if (min > 0) {
         final int MAX_BINS = 50, MIN_BINS = 5;
         final LinearRegression lr = new LinearRegression();
         final int end = SpectrumUtils.bound(spec, min - MIN_BINS);
         for (int ch = min; ch >= end; --ch)
            lr.addDatum(ch, spec.getCounts(ch),
                  spec.getCounts(ch) > 1.0
                        ? Math.sqrt(spec.getCounts(ch))
                        : 1.0);
         bestCh = end;
         bestRes = lr.computeY(min);
         bestErr = Math.sqrt(lr.chiSquared()) / lr.getCount();
         if (bestErr < 2.0) {
            final int end2 = SpectrumUtils.bound(spec, min - MAX_BINS);
            for (int ch = min - (MIN_BINS + 1); ch >= end2; --ch) {
               final double x = spec.getCounts(ch);
               lr.addDatum(ch, x, x > 1.0 ? Math.sqrt(x) : 1.0);
               final double err = Math.sqrt(lr.chiSquared()) / lr.getCount();
               if (err < bestErr) {
                  bestRes = lr.computeY(min);
                  bestErr = err;
                  bestCh = ch;
               }
            }
         } else {
            // A linear model is not a good one. Try a short constant model.
            final DescriptiveStatistics ds = new DescriptiveStatistics();
            bestCh = min;
            ds.add(spec.getCounts(bestCh));
            bestRes = ds.average();
            bestErr = Math.sqrt(bestRes);
            for (int ch = min - 1; ch >= end; --ch) {
               ds.add(spec.getCounts(ch));
               final double err = Math.sqrt(ds.variance() / ds.count());
               if (err < bestErr) {
                  bestRes = ds.average();
                  bestErr = err;
               }
            }
            bestErr /= Math.max(1.0, Math.sqrt(bestRes));
         }
      }
      return new double[]{bestRes, Math.max(1.0, Math.sqrt(bestRes) * bestErr),
            (min - bestCh) + 1};
   }

   /**
    * Estimate the background starting at channel <code>max</code> heading
    * towards getChannelCount() by averaging at least 3 but as many as 100
    * channels together to minimize the error estimate in the background level.
    * 
    * @param spec
    * @param max
    * @return double[2] { average, error, nChannels }
    */
   static public double[] estimateHighBackground(ISpectrumData spec, int max) {

      int bestCh = spec.getChannelCount() - 1;
      double bestRes = spec.getCounts(bestCh);
      double bestErr = bestRes > 1 ? Math.sqrt(bestRes) : 1.0;
      if (max < spec.getChannelCount()) {
         final int MAX_BINS = 50, MIN_BINS = 5;
         final LinearRegression lr = new LinearRegression();
         final int end = SpectrumUtils.bound(spec, max + MIN_BINS);
         for (int ch = max; ch <= end; ++ch)
            lr.addDatum(ch, spec.getCounts(ch),
                  spec.getCounts(ch) > 1.0
                        ? Math.sqrt(spec.getCounts(ch))
                        : 1.0);
         bestCh = end;
         bestRes = lr.computeY(max);
         bestErr = Math.sqrt(lr.chiSquared()) / lr.getCount();
         if (bestErr < 2.0) {
            final int end2 = SpectrumUtils.bound(spec, max + MAX_BINS);
            for (int ch = max + (MIN_BINS + 1); ch <= end2; ++ch) {
               final double x = spec.getCounts(ch);
               lr.addDatum(ch, x, x > 1.0 ? Math.sqrt(x) : 1.0);
               final double err = Math.sqrt(lr.chiSquared()) / lr.getCount();
               if (err < bestErr) {
                  bestRes = lr.computeY(max);
                  bestErr = err;
                  bestCh = ch;
               }
            }
         } else {
            // A linear model is not a good one. Try a short constant model.
            final DescriptiveStatistics ds = new DescriptiveStatistics();
            bestCh = max;
            ds.add(spec.getCounts(bestCh));
            bestRes = ds.average();
            bestErr = Math.sqrt(bestRes);
            for (int ch = max + 1; ch <= end; ++ch) {
               ds.add(spec.getCounts(ch));
               final double err = Math.sqrt(ds.variance() / ds.count());
               if (err < bestErr) {
                  bestRes = ds.average();
                  bestErr = err;
               }
            }
            bestErr /= Math.max(1.0, Math.sqrt(bestRes));
         }
      }
      return new double[]{bestRes, Math.max(1.0, Math.sqrt(bestRes) * bestErr),
            (bestCh - max) + 1};
   }

   /**
    * Computes the background corrected integral over the specified range of
    * energies (eLow, eHigh). The width of the regions above and below the peak
    * that is used to estimate the background is adapted dynamically to reduce
    * the standard deviation in the background estimate.
    * 
    * @param spec
    *           The spectrum
    * @param eLow
    *           - Low energy in eV
    * @param eHigh
    *           - High energy in eV
    * @return double[4] - double[0] is the background corrected integral,
    *         double[1] is estimated error in [0], double[2] is total integral,
    *         double[3] is background integral
    */
   static public double[] backgroundCorrectedIntegral(ISpectrumData spec,
         double eLow, double eHigh) {
      assert eLow < eHigh;
      // Determine the min and max channels to include in the integral
      int min = SpectrumUtils.bound(spec,
            SpectrumUtils.channelForEnergy(spec, eLow));
      int max = SpectrumUtils.bound(spec,
            SpectrumUtils.channelForEnergy(spec, eHigh));
      if (min > max) {
         final int tmp = min;
         min = max;
         max = tmp;
      }
      double a, da;
      {
         final double[] low = estimateLowBackground(spec, min - 1);
         final double[] high = estimateHighBackground(spec, max + 1);
         a = 0.5 * (low[0] + high[0]) * ((max - min) + 1);
         da = (low[0] + high[0]) != 0.0
               ? (Math.sqrt((low[1] * low[1]) + (high[1] * high[1]))
                     / (low[0] + high[0])) * a
               : 1.0;
      }
      double i = 0.0;
      for (int ch = min; ch <= max; ++ch)
         i += spec.getCounts(ch);
      final double[] res = new double[]{i - a, Math.sqrt((da * da) + i), i, a};
      if (Double.isNaN(res[1]))
         res[1] = Math.sqrt(Math.max(1.0, i - a));
      return res;
   }

   /**
    * Trim a spectrum by removing any structure between e0 and e1. Useful for
    * peak removal. This method makes the best estimate of the background levels
    * above and below and then fills the interval with a line between these
    * points. <code>trimSpectrum</code> and
    * <code>backgroundCorrectedIntegral</code> form a matched pair. With the
    * same arguments, <code>trimSpectrum</code> will show exactly the background
    * used in <code>backgroundCorrectedIntegral</code>.
    * 
    * @param spec
    *           The spectrum
    * @param eLow
    *           - Low energy in eV
    * @param eHigh
    *           - High energy in eV
    * @return ISpectrumData
    */
   public static ISpectrumData trimSpectrum(ISpectrumData spec, double eLow,
         double eHigh) {
      int min = SpectrumUtils.bound(spec, channelForEnergy(spec, eLow));
      int max = SpectrumUtils.bound(spec, channelForEnergy(spec, eHigh));
      if (min > max) {
         final int tmp = min;
         min = max;
         max = tmp;
      }
      final double i0 = estimateLowBackground(spec, min - 1)[0];
      final double i1 = estimateHighBackground(spec, max + 1)[0];
      final EditableSpectrum res = new EditableSpectrum(spec);
      for (int ch = min; ch <= max; ++ch)
         res.setCounts(ch, i0 + (((ch - min) * (i1 - i0)) / (max - min)));
      final NumberFormat nf = new HalfUpFormat("0");
      rename(res, "Trimmed[" + spec.toString() + "," + nf.format(eLow) + " eV,"
            + nf.format(eHigh) + " eV]");
      return res;
   }

   /**
    * A more general algorithm for computing a background corrected integral
    * when it is not appropriate to use the area next to the peak to perform the
    * integration.
    * 
    * @param spec
    *           The spectrum
    * @param ePeakLow
    *           Low energy in peak
    * @param ePeakHigh
    *           High energy in peak
    * @param eLowBkgLow
    *           Low energy in low side background
    * @param eLowBkgHigh
    *           High energy in low side background
    * @param eHighBkgLow
    *           Low energy in high side background
    * @param eHighBkgHigh
    *           High energy in high side background
    * @return double[4] - double[0] -&gt; the background corrected integral,
    *         double[1] -&gt; estimated error in [0], double[2] -&gt; total
    *         integral, double[3] -&gt; background integral
    */
   static public double[] backgroundCorrectedIntegral(ISpectrumData spec,
         double ePeakLow, double ePeakHigh, double eLowBkgLow,
         double eLowBkgHigh, double eHighBkgLow, double eHighBkgHigh) {
      return backgroundCorrectedIntegral(spec,
            SpectrumUtils.channelForEnergy(spec, ePeakLow),
            SpectrumUtils.channelForEnergy(spec, ePeakHigh),
            SpectrumUtils.channelForEnergy(spec, eLowBkgLow),
            SpectrumUtils.channelForEnergy(spec, eLowBkgHigh),
            SpectrumUtils.channelForEnergy(spec, eHighBkgLow),
            SpectrumUtils.channelForEnergy(spec, eHighBkgHigh));
   }

   /**
    * A more general algorithm for computing a background corrected integral
    * when it is not appropriate to use the area next to the peak to perform the
    * integration.
    * 
    * @param spec
    *           The spectrum
    * @param chPeakLow
    *           Low channel in peak
    * @param chPeakHigh
    *           High channel in peak
    * @param chLowBkgLow
    *           Low channel in low side background
    * @param chLowBkgHigh
    *           High channel in low side background
    * @param chHighBkgLow
    *           Low channel in high side background
    * @param chHighBkgHigh
    *           High channel in high side background
    * @return double[4] - double[0] -&gt; the background corrected integral,
    *         double[1] -&gt; estimated error in [0], double[2] -&gt; total
    *         integral, double[3] -&gt; background integral
    */
   static public double[] backgroundCorrectedIntegral(ISpectrumData spec,
         int chPeakLow, int chPeakHigh, int chLowBkgLow, int chLowBkgHigh,
         int chHighBkgLow, int chHighBkgHigh) {
      UncertainValue2 i = null;
      { // Peak integral
        // Determine the min and max channels to include in the integral
         if (chPeakLow > chPeakHigh) {
            final int tmp = chPeakLow;
            chPeakLow = chPeakHigh;
            chPeakHigh = tmp;
         }
         chPeakLow = SpectrumUtils.bound(spec, chPeakLow);
         chPeakHigh = SpectrumUtils.bound(spec, chPeakHigh);
         double tmp = 0.0;
         for (int ch = chPeakLow; ch <= chPeakHigh; ++ch)
            tmp += spec.getCounts(ch);
         i = new UncertainValue2(tmp, "C", Math.sqrt(tmp));
      }
      UncertainValue2 bLow = null;
      { // Peak integral
        // Determine the min and max channels to include in the integral
         if (chLowBkgLow > chLowBkgHigh) {
            final int tmp = chLowBkgLow;
            chLowBkgLow = chLowBkgHigh;
            chLowBkgHigh = tmp;
         }
         chLowBkgLow = SpectrumUtils.bound(spec, chLowBkgLow);
         chLowBkgHigh = SpectrumUtils.bound(spec, chLowBkgHigh);
         double tmp = 0.0;
         for (int ch = chLowBkgLow; ch <= chLowBkgHigh; ++ch)
            tmp += spec.getCounts(ch);
         bLow = UncertainValue2.divide(
               new UncertainValue2(tmp, "Cl", Math.sqrt(tmp)),
               (chLowBkgHigh - chLowBkgLow) + 1);
      }
      UncertainValue2 bHigh = null;
      { // Peak integral
        // Determine the min and max channels to include in the integral
         if (chHighBkgLow > chHighBkgHigh) {
            final int tmp = chHighBkgLow;
            chHighBkgLow = chHighBkgHigh;
            chHighBkgHigh = tmp;
         }
         chHighBkgLow = SpectrumUtils.bound(spec, chHighBkgLow);
         chHighBkgHigh = SpectrumUtils.bound(spec, chHighBkgHigh);
         double tmp = 0.0;
         for (int ch = chHighBkgLow; ch <= chHighBkgHigh; ++ch)
            tmp += spec.getCounts(ch);
         bHigh = UncertainValue2.divide(
               new UncertainValue2(tmp, "Ch", Math.sqrt(tmp)),
               (chHighBkgHigh - chHighBkgLow) + 1);
      }
      final double cLow = 0.5 * (chLowBkgLow + chLowBkgHigh),
            cHigh = 0.5 * (chHighBkgLow + chHighBkgHigh),
            cPeak = 0.5 * (chPeakLow + chPeakHigh);
      final UncertainValue2 a = UncertainValue2
            .multiply(((chPeakHigh - chPeakLow) + 1),
                  UncertainValue2.add(UncertainValue2.multiply(
                        (cPeak - cLow) / (cHigh - cLow),
                        UncertainValue2.subtract(bHigh, bLow)), bLow));
      final UncertainValue2 ima = UncertainValue2.subtract(i, a);
      final double[] res = new double[]{ima.doubleValue(), ima.uncertainty(),
            i.doubleValue(), a.doubleValue()};
      return res;
   }

   /**
    * Returns a ISpectrumData object representing the argument spectrum
    * processed such that the absolute value of the counts in each channel is
    * returned. (<code>getCounts(i)&gt;=0</code>)
    * 
    * @param spec
    * @return ISpectrumData
    */
   public static ISpectrumData getAbsSpectrum(ISpectrumData spec) {
      for (int i = spec.getChannelCount() - 1; i >= 0; --i)
         if (spec.getCounts(i) < 0)
            return new DerivedSpectrum(spec) {
               @Override
               public double getCounts(int i) {
                  return Math.abs(mSource.getCounts(i));
               }
            };
      return spec;
   }

   /**
    * Returns a ISpectrumData object representing the argument spectrum
    * processed such that the negative counts in any channels are truncated to
    * zero. (<code>getCounts(i)&gt;=0</code>)
    * 
    * @param spec
    * @return ISpectrumData
    */
   public static ISpectrumData getPositiveSpectrum(ISpectrumData spec) {
      for (int i = spec.getChannelCount() - 1; i >= 0; --i)
         if (spec.getCounts(i) < 0)
            return new DerivedSpectrum(spec) {
               @Override
               public double getCounts(int i) {
                  final double res = mSource.getCounts(i);
                  return res > 0.0 ? res : 0.0;
               }
            };
      return spec;

   }

   /**
    * Computes the solid angle from the best available data in the
    * SpectrumProperties. If this information is not available the the value
    * <code>def</code> is used.
    * 
    * @param props
    * @param def
    * @return The solid angle in stetradians
    */
   public static double getSolidAngle(SpectrumProperties props, double def) {
      double res = def;
      final double area = props
            .getNumericWithDefault(SpectrumProperties.DetectorArea, 10.0);
      assert (area > 1.0e-6) && (area < 1000.0)
            : Double.toString(area) + " mm^2";
      final double[] pos = getDetectorPosition(props);
      if (pos != null) {
         final double[] sample = getSamplePosition(props);
         final double r = 1.0e3 * Math2.distance(sample, pos);
         assert (r > 1.0) && (r < 1000.0) : Double.toString(r) + " m";
         res = area / (r * r);
      }
      return res;
   }

   /**
    * Generate a deep copy of the specified spectrum. The spectrum data is
    * immutable but the SpectrumProperties can be modified.
    * 
    * @param spec
    * @return A copy of the specified spectrum
    */
   static public BaseSpectrum copy(ISpectrumData spec) {
      return scale(1.0, 0.0, spec);
   }

   /**
    * Returns a deep copy of the input spectrum with the intensity of the
    * spectrum data scale by the specified amount.
    * 
    * @param scale
    * @param spec
    * @return A scaled ISpectrumData object
    */
   static public BaseSpectrum scale(double scale, ISpectrumData spec) {
      return scale(scale, 0.0, spec);
   }

   /**
    * Returns a copy of the input spectrum with the intensity of the spectrum
    * data scale by the specified amount.
    * 
    * @param scale
    * @param offset
    * @param spec
    * @return A scaled ISpectrumData object
    */
   static public BaseSpectrum scale(double scale, double offset,
         ISpectrumData spec) {
      class ScaledSpectrum extends BaseSpectrum {
         private final double[] mChannels;
         private final SpectrumProperties mProperties;

         ScaledSpectrum(ISpectrumData spec, double offset, double scale) {
            super();
            mChannels = new double[spec.getChannelCount()];
            for (int i = 0; i < mChannels.length; ++i)
               mChannels[i] = (scale * spec.getCounts(i)) + offset;
            final String name = (scale != 1.0
                  ? Double.toString(scale) + '\u00D7'
                  : "") + spec.toString()
                  + (offset != 0.0 ? "+" + Double.toString(offset) : "");
            mProperties = spec.getProperties().clone();
            SpectrumUtils.rename(this, name);
         }

         /**
          * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getChannelCount()
          */
         @Override
         public int getChannelCount() {
            return mChannels.length;
         }

         /**
          * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getCounts(int)
          */
         @Override
         public double getCounts(int i) {
            return mChannels[i];
         }

         /**
          * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getProperties()
          */
         @Override
         public SpectrumProperties getProperties() {
            return mProperties;
         }
      }
      return new ScaledSpectrum(spec, offset, scale);
   }

   /**
    * Returns an ISpectrumData object representing this spectrum but with all
    * channels less than the value in ZeroPeakDiscriminator set to zero. If the
    * property ZeroPeakDiscriminator is not available then the input spectrum is
    * returned.
    * 
    * @param spec
    * @return ISpectrumData
    */
   static public ISpectrumData applyZeroPeakDiscriminator(ISpectrumData spec) {
      final int lld = getZeroStrobeDiscriminatorChannel(spec);
      return lld > 0 ? applyZeroPeakDiscriminator(spec, lld) : spec;
   }

   static public int getZeroStrobeDiscriminatorChannel(ISpectrumData spec) {
      final SpectrumProperties sp = spec.getProperties();
      if (sp.isDefined(SpectrumProperties.ZeroPeakDiscriminator)) {
         final double zpd = sp.getNumericWithDefault(
               SpectrumProperties.ZeroPeakDiscriminator, 0.0);
         return SpectrumUtils.channelForEnergy(spec, zpd);
      }
      return 0;
   }

   /**
    * Returns the same spectrum in which the channels below lldCh have been set
    * to zero.
    * 
    * @param spec
    * @param lldCh
    * @return ISpectrumData
    */
   static public ISpectrumData applyZeroPeakDiscriminator(ISpectrumData spec,
         int lldCh) {
      for (int i = 0; i < lldCh; ++i)
         if (spec.getCounts(i) != 0.0) {
            final EditableSpectrum es = new EditableSpectrum(spec);
            for (int j = i; j < lldCh; ++j)
               es.setCounts(j, 0.0);
            return es;
         }
      return spec;
   }

   /**
    * Rescale the spectrum data and rescale the LiveTime property to reflect the
    * change in scale.
    * 
    * @param scale
    * @param spec
    * @return ISpectrumData
    */
   static public ISpectrumData scaleWithLiveTime(double scale,
         ISpectrumData spec) {
      final ISpectrumData res = scale(scale, spec);
      final SpectrumProperties sp = res.getProperties();
      sp.setNumericProperty(SpectrumProperties.LiveTime, scale
            * sp.getNumericWithDefault(SpectrumProperties.LiveTime, 60.0));
      return res;
   }

   /**
    * Determines the surface normal from the data in the SpectrumProperties
    * object. If there is no explicit information in SpectrumProperties, the
    * surface normal is assumed to be the minus Z-axis.
    * 
    * @param props
    * @return double[]
    */
   static public double[] getSurfaceNormal(SpectrumProperties props) {
      final SampleShape ss = props.getSampleShapeWithDefault(
            SpectrumProperties.SampleShape, new SampleShape.Bulk());
      return ss.getOrientation();
   }

   /**
    * Computes the take-off angle from the SpectrumProperties.DetectorPosition
    * and getSamplePosition when available or otherwise from the
    * SpectrumProperties.TakeOffAngle property. This library assumes the
    * convention that the take-off angle is a function of the detector position
    * but not the sample orientation. As the sample orientation changes, the
    * exit angle (<code>getExitAngle(props)</code>) varies but not the take-off
    * angle. The take-off angle can vary slightly with changes in working
    * distance. The only way it can vary by large amounts is if the detector is
    * physically moved.
    * 
    * @param props
    * @return double (in Radians)
    */
   static public double getTakeOffAngle(SpectrumProperties props) {
      if (props.isDefined(SpectrumProperties.DetectorPosition)
            && (props.isDefined(SpectrumProperties.WorkingDistance)
                  || props.isDefined(SpectrumProperties.DetectorOptWD))) {
         final double[] vec = Math2.minus(getDetectorPosition(props),
               getSamplePosition(props));
         return (Math.PI / 2.0) - Math2.angleBetween(vec, Math2.MINUS_Z_AXIS);
      } else if (props.isDefined(SpectrumProperties.TakeOffAngle))
         return Math.toRadians(props.getNumericWithDefault(
               SpectrumProperties.TakeOffAngle, Double.NaN));
      else
         return Math.toRadians(props.getNumericWithDefault(
               SpectrumProperties.Elevation, Double.NaN));
   }

   /**
    * Returns the position of the sample as specified by information in the
    * SpectrumProperties.
    * 
    * @param props
    * @return double[] in meters!!!!
    */
   static public double[] getSamplePosition(SpectrumProperties props) {
      final double wd = 1.0e-3 * props.getNumericWithDefault(
            SpectrumProperties.WorkingDistance,
            props.getNumericWithDefault(SpectrumProperties.DetectorOptWD, 0.0));
      return Math2.multiply(wd, Math2.Z_AXIS);
   }

   /**
    * Returns the position of the detector in real space relative to the origin
    * at WD=0.0 on the beam axis.
    * 
    * @param props
    * @return double[3] in meters!
    */
   static public double[] getDetectorPosition(SpectrumProperties props) {
      double[] pos = props
            .getArrayWithDefault(SpectrumProperties.DetectorPosition, null);
      if (pos == null) {
         final double el = props
               .getNumericWithDefault(SpectrumProperties.Elevation, 40.0);
         final double az = props
               .getNumericWithDefault(SpectrumProperties.Azimuth, 0.0);
         final double dist = props.getNumericWithDefault(
               SpectrumProperties.DetectorDistance, 60.0);
         final double optWd = props
               .getNumericWithDefault(SpectrumProperties.DetectorOptWD, 20.0);
         props.setDetectorPosition(Math.toRadians(el), Math.toRadians(az),
               1.0e-3 * dist, 1.0e-3 * optWd);
         pos = props.getArrayWithDefault(SpectrumProperties.DetectorPosition,
               null);
         assert pos != null;
      }
      return Math2.multiply(1.0e-3, pos);
   }

   /**
    * Returns a normalized 3-vector pointing from the sample to the detector.
    * 
    * @param props
    * @return double[3]
    */
   static public double[] getDetectorAxis(SpectrumProperties props) {
      return Math2.normalize(
            Math2.minus(getDetectorPosition(props), getSamplePosition(props)));
   }

   /**
    * <p>
    * Computes the x-ray exit angle from the XRayDetectorPosition,
    * WorkingDistance and (optionally) the SampleSurfaceNormal properties. The
    * exit angle is the angle between the surface of the sample (assumed flat)
    * and the x-ray detector. When the sample is normal to the beam, the exit
    * angle is the same as the take-off angle.
    * </p>
    * <p>
    * <b>Note:</b> The exit angle is measured up from the plane perpendicular to
    * the beam axis.
    * </p>
    * 
    * @param props
    * @return double
    * @throws EPQException
    */
   static public double getExitAngle(SpectrumProperties props)
         throws EPQException {
      if (props.isDefined(SpectrumProperties.DetectorPosition)
            && (props.isDefined(SpectrumProperties.WorkingDistance)
                  || props.isDefined(SpectrumProperties.DetectorOptWD))) {
         // Vector from sample to the detector
         final double[] vec = Math2.minus(getDetectorPosition(props),
               getSamplePosition(props));
         final double res = (Math.PI / 2.0)
               - Math2.angleBetween(vec, getSurfaceNormal(props));
         assert (Math2.distance(getSurfaceNormal(props),
               Math2.MINUS_Z_AXIS) > 0.01)
               || (Math.abs(res - Math.toRadians(props.getNumericWithDefault(
                     SpectrumProperties.Elevation, Math.toDegrees(res)))) < Math
                           .toRadians(5.0))
               : "The computed TOA and default TOA differ by "
                     + Double.toString(Math.toDegrees(res) - props
                           .getNumericWithDefault(SpectrumProperties.Elevation,
                                 Math.toDegrees(res)))
                     + "\u00B0";
         return res;
      } else
         return getTakeOffAngle(props);
   }

   /**
    * Get the distance from the sample to the detector.
    * 
    * @param props
    * @return double The distance in meters
    * @throws EPQException
    */
   static public double getDetectorDistance(SpectrumProperties props)
         throws EPQException {
      return Math2.magnitude(
            Math2.minus(getDetectorPosition(props), getSamplePosition(props)));
   }

   /**
    * Makes the best estimate of the faraday current from the data available in
    * the specified set of SpectrumProperties. If no data is available this
    * function returns Double.NaN.
    * 
    * @param props
    * @param def
    *           The default current to return in nA
    * @return The current in nA
    */
   static public double getAverageFaradayCurrent(SpectrumProperties props,
         double def) {
      final double liveTime0 = props
            .getNumericWithDefault(SpectrumProperties.FaradayBegin, Double.NaN);
      final double liveTime1 = props
            .getNumericWithDefault(SpectrumProperties.FaradayEnd, Double.NaN);
      double res = 0.0;
      double n = 0;
      if (!Double.isNaN(liveTime0)) {
         res += liveTime0;
         ++n;
      }
      if (!Double.isNaN(liveTime1)) {
         res += liveTime1;
         ++n;
      }
      return n > 0 ? res / n : def;
   }

   /**
    * @param props
    * @return In nA.s
    * @throws EPQException
    */
   static public double getDose(SpectrumProperties props) throws EPQException {
      final double fc = getAverageFaradayCurrent(props, 0.0);
      if (fc <= 0.0)
         throw new EPQException("The probe current is unavailable.");
      final double lt = props.getNumericWithDefault(SpectrumProperties.LiveTime,
            0.0);
      if (lt <= 0.0)
         throw new EPQException("The live-time is unavailable.");
      return lt * fc;
   }

   /**
    * Returns the mass thickness in &mu;g/cm<sup>2</sup>
    * 
    * @param props
    * @return the mass-thickness in &mu;g/cm<sup>2</sup>
    */
   static public double getMassThickness(SpectrumProperties props) {
      final SampleShape ss = props
            .getSampleShapeWithDefault(SpectrumProperties.SampleShape, null);
      final Composition comp = props.getCompositionWithDefault(
            SpectrumProperties.StandardComposition, null);
      if ((ss instanceof SampleShape.ThinFilm) && (comp instanceof Material))
         return FromSI.ugPcm2(((SampleShape.ThinFilm) ss).getThickness()
               * ((Material) comp).getDensity()).doubleValue();
      return props.getNumericWithDefault(SpectrumProperties.MassThickness,
            Double.NaN);
   }

   /**
    * Returns the sum over all channels of spec1.getCounts()-spec2.getCounts()
    * 
    * @param spec1
    * @param spec2
    * @return double
    */
   static public double deltaCounts(ISpectrumData spec1, ISpectrumData spec2) {
      final int max = Math.min(spec1.getChannelCount(),
            spec2.getChannelCount());
      double res = 0.0;
      for (int i = 0; i < max; ++i)
         res += spec1.getCounts(i) - spec2.getCounts(i);
      return res;
   }

   static public DerivedSpectrum getSlice(ISpectrumData spec, int lowCh,
         int highCh) {
      class Slice extends DerivedSpectrum {
         private final int mLowCh;
         private final int mHighCh;

         Slice(ISpectrumData spec, int lowCh, int highCh) {
            super(spec);
            mLowCh = lowCh;
            mHighCh = highCh;
         }

         @Override
         public double getCounts(int i) {
            return (i >= mLowCh) && (i < mHighCh)
                  ? getBaseSpectrum().getCounts(i)
                  : 0.0;
         }
      }
      return new Slice(spec, lowCh, highCh);
   }

   /**
    * Performs a crude energy calibration based on the specified set of x-ray
    * lines which are presumed to be visible and the set of extracted peak
    * positions
    * 
    * @param xrts
    *           A set of XRayTransition objects
    * @param peakPositions
    *           Positions Joules
    * @param tol
    *           Peak position tolerance in Joules
    * @return Map&lt;XRayTransition, Double&gt;
    * @throws EPQException
    */
   static public Map<XRayTransition, Double> roughEnergyCalibration(
         Collection<XRayTransition> xrts, Collection<Double> peakPositions,
         double tol) throws EPQException {
      if (xrts.size() < 3)
         throw new EPQException(
               "Too few transitions in the rough energy calibration.");
      Map<XRayTransition, Double> res = null;
      // Sort the peaks low to high...
      final ArrayList<Double> peaks = new ArrayList<Double>(peakPositions);
      Collections.sort(peaks);
      final ArrayList<XRayTransition> trans = new ArrayList<XRayTransition>(
            xrts);
      Collections.sort(trans, new Comparator<XRayTransition>() {
         @Override
         public int compare(XRayTransition o1, XRayTransition o2) {
            try {
               return (int) Math
                     .round(Math.signum(o1.getEnergy() - o2.getEnergy()));
            } catch (final EPQException e) {
               e.printStackTrace();
               return 0;
            }
         }
      });
      double bestMetric = Double.MAX_VALUE;
      for (int ti = 0; ti < trans.size(); ++ti)
         for (int tj = ti + 1; tj < trans.size(); ++tj) {
            final XRayTransition low = trans.get(ti);
            final XRayTransition high = trans.get(tj);
            final double deltaE = high.getEnergy() - low.getEnergy();
            assert deltaE > 0.0;
            for (int i = 0; i < peaks.size(); ++i)
               for (int j = i + 1; j < peaks.size(); ++j) {
                  // Assume that low is peak i, and high is peak j
                  final Double lowPeak = peaks.get(i);
                  final Double highPeak = peaks.get(j);
                  assert (highPeak - lowPeak) > 0;
                  final double scale = deltaE / (highPeak - lowPeak);
                  if ((scale < 0.5) || (scale > 2.0))
                     continue;
                  final double offset = low.getEnergy() - (scale * lowPeak);
                  assert Math.abs(((scale * lowPeak) + offset)
                        - low.getEnergy()) < ToSI.eV(1.0);
                  assert Math.abs(((scale * highPeak) + offset)
                        - high.getEnergy()) < ToSI.eV(1.0);
                  final Map<XRayTransition, Double> map = new TreeMap<XRayTransition, Double>();
                  map.put(low, lowPeak);
                  map.put(high, highPeak);
                  double chiSq = 0.0;
                  for (int k = 0; k < peaks.size(); ++k) {
                     if ((k == i) || (k == j))
                        continue;
                     final Double peak = peaks.get(k);
                     final double posK = (scale * peak) + offset;
                     XRayTransition best = null;
                     double bestErr = Double.MAX_VALUE;
                     for (final XRayTransition xrt : xrts) {
                        if (map.containsKey(xrt))
                           continue;
                        final double err = Math.abs(posK - xrt.getEnergy());
                        if (err < bestErr) {
                           best = xrt;
                           bestErr = err;
                        }
                     }
                     if (bestErr < tol) {
                        map.put(best, peak);
                        chiSq += (bestErr * bestErr) / (tol * tol);
                     }
                  }
                  // Use a metric that favors more and better matches
                  if ((res == null) || (map.size() > res.size())
                        || ((map.size() == res.size())
                              && (chiSq < bestMetric))) {
                     bestMetric = chiSq;
                     res = map;
                  }
               }
         }
      return res;
   }

   /**
    * Perform a rough energy calibration based on a list of elements present.
    * 
    * @param elms
    *           A list of elements present in the spectrum
    * @param peakPositions
    *           A Collection of peak positions in Joules
    * @param e0
    *           The beam energy in Joules
    * @param eLow
    *           The minimum energy line to consider in Joules
    * @param tol
    *           The peak match tolerance in Joules
    * @return The best map between XRayTransition objects and peaks
    * @throws EPQException
    */
   static public Map<XRayTransition, Double> roughEnergyCalibration(
         Collection<Element> elms, Collection<Double> peakPositions, double e0,
         double eLow, double tol) throws EPQException {
      final int[] lines = new int[]{XRayTransition.KA1, XRayTransition.LA1,
            XRayTransition.MA1};
      final Set<XRayTransition> xrts = new TreeSet<XRayTransition>();
      for (final Element elm : elms)
         for (final int line : lines) {
            final double e = XRayTransition.getEnergy(elm, line);
            if ((e >= eLow) && (e <= (0.75 * e0)))
               xrts.add(new XRayTransition(elm, line));
         }
      return roughEnergyCalibration(xrts, peakPositions, tol);
   }

   /**
    * Perform a rough linear energy calibration based on a list of elements
    * present.
    * 
    * @param elms
    *           The elements
    * @param peakPositions
    *           The positions of the peaks in the spectrum in Joules
    * @param e0
    *           The beam energy in Joules
    * @param eLow
    *           The lowest energy to consider in Joules
    * @param tol
    *           The peak match tolerance in Joules
    * @return A LinearRegression object containing the best fit in Joules
    * @throws EPQException
    */
   static public LinearRegression linearEnergyCalibration(
         Collection<Element> elms, Collection<Double> peakPositions, double e0,
         double eLow, double tol) throws EPQException {
      final Map<XRayTransition, Double> res = roughEnergyCalibration(elms,
            peakPositions, e0, eLow, tol);
      final LinearRegression lr = new LinearRegression();
      for (final Map.Entry<XRayTransition, Double> me : res.entrySet())
         lr.addDatum(me.getKey().getEnergy(), me.getValue());
      return lr;
   }

   static public Set<Double> peakPositions(ISpectrumData spec,
         int typicalPeakWidth, double thresh) {
      final double[] filtered = filterSpectrum(spec, typicalPeakWidth);
      final TreeSet<Double> pp = new TreeSet<Double>();
      double max = 0.0;
      for (final double element : filtered)
         max = Math.max(element, max);
      for (int i = 0; i < filtered.length; ++i)
         if (filtered[i] > (thresh * max))
            for (++i; i < filtered.length; ++i)
               if (filtered[i] < 0) {
                  pp.add(ToSI.eV(SpectrumUtils.avgEnergyForChannel(spec,
                        Integer.valueOf(i))));
                  break;
               }
      return pp;
   }

   /**
    * Apply a generic filter to the specified spectra. The filter should be of
    * odd length and the filter is offset so that the central channel of the
    * filter is applied to the channel <code>ch</code>.
    * 
    * @param spec
    * @param filter
    * @param ch
    * @return double
    */
   static public double applyFilter(ISpectrumData spec, double[] filter,
         int ch) {
      assert (filter.length % 2) == 1;
      double res = 0.0;
      final int maxCh = spec.getChannelCount() - 1;
      final int offset = ch - (filter.length / 2);
      for (int i = 0; i < filter.length; ++i)
         res += filter[i]
               * spec.getCounts(Math.min(Math.max(0, offset + i), maxCh));
      return res;
   }

   /**
    * Apply a simple smoothing first-derivative type filter to the argument
    * spectrum.
    * 
    * @param spec
    * @param typicalPeakWidth
    * @return double[]
    */
   static public double[] filterSpectrum(ISpectrumData spec,
         int typicalPeakWidth) {
      final double[] filter = new double[(2 * typicalPeakWidth) + 1];
      {
         final double sc = 2.0 / ((1.0 + typicalPeakWidth) * typicalPeakWidth);
         filter[typicalPeakWidth] = 0.0;
         for (int i = 0; i < typicalPeakWidth; ++i) {
            final double v = sc * (i + 1.0);
            filter[i] = -v;
            filter[filter.length - i] = v;
         }
         assert Math.abs(Math2.sum(filter)) < 1.0e-6;
         assert Math.abs(Math2.sum(Math2.slice(filter, 0, typicalPeakWidth))
               + 1.0) < 1.0e-6;
         assert Math.abs(Math2
               .sum(Math2.slice(filter, typicalPeakWidth + 1, typicalPeakWidth))
               - 1.0) < 1.0e-6;
      }
      final double[] filtered = new double[spec.getChannelCount()];
      for (int i = filtered.length - 1; i >= 0; --i)
         filtered[i] = applyFilter(spec, filter, i);
      return filtered;
   }

   /**
    * Returns the best estimate of the composition from the data within the
    * spectrum properties.
    * 
    * @param spec
    * @return Composition
    */
   static public Composition getComposition(ISpectrumData spec) {
      Composition res = null;
      final SpectrumProperties sp = spec.getProperties();
      res = sp.getCompositionWithDefault(SpectrumProperties.StandardComposition,
            null);
      if (res == null)
         res = sp.getCompositionWithDefault(
               SpectrumProperties.MicroanalyticalComposition, null);
      return res;
   }

   /**
    * Does this channel represent a channel for which this spectrum contains
    * data?
    * 
    * @param spec
    * @param ch
    * @return True if the spectrum has data available for this channel
    */
   static public boolean isValidChannel(ISpectrumData spec, int ch) {
      return (ch >= 0) && (ch < spec.getChannelCount());
   }

   /**
    * The nominal amount of energy required to generate a single electron hole
    * pair in a Si(Li) detector. The actual value depends on the device
    * temperture and is about 3.71 at LN temperatures and 3.64 eV at SDD
    * temperatures.
    */
   // static public final double ENERGY_PER_EH_PAIR = 3.76;
   static public final double ENERGY_PER_EH_PAIR = 3.64; // Modified from 3.76
                                                         // on 18-Mar-2011

   /**
    * Computes the detector resolution at the specified energy (<code>eV</code>)
    * for the specified fano factor and noise.
    * 
    * @param fano
    *           Fano factor (nominally ~0.122)
    * @param noise
    *           Noise in eV
    * @param eV
    *           The x-ray detection energy in eV
    * @return The Gaussian width in eV
    */
   static public double resolution(double fano, double noise, double eV) {
      return ENERGY_PER_EH_PAIR
            * Math.sqrt((noise * noise) + ((eV * fano) / ENERGY_PER_EH_PAIR));
   }

   /**
    * Computes the detector resolution at the specified energy (<code>eV</code>)
    * for the specified fano factor and noise.
    * 
    * @param fano
    *           Fano factor (nominally ~0.122)
    * @param noise
    *           Noise in eV
    * @param eV
    *           The x-ray detection energy in eV
    * @return The Gaussian width in eV
    */
   static public UncertainValue2 resolutionU(UncertainValue2 fano,
         UncertainValue2 noise, double eV) {
      return UncertainValue2.multiply(ENERGY_PER_EH_PAIR,
            UncertainValue2
                  .add(UncertainValue2.multiply(noise, noise),
                        UncertainValue2.multiply(eV / ENERGY_PER_EH_PAIR, fano))
                  .sqrt());
   }

   /**
    * Computes the noise term given the fano factor, resolution at a specified
    * energy and the energy.
    * 
    * @param fano
    *           Fano factor (nominally ~0.122)
    * @param gRes
    *           Gaussian resolution at eV
    * @param eV
    *           The energy at which gRes was measured in ev
    * @return The noise term in eV
    */
   static public double noiseFromResolution(double fano, double gRes,
         double eV) {
      return Math.sqrt(Math2.sqr(gRes / ENERGY_PER_EH_PAIR)
            - ((eV * fano) / ENERGY_PER_EH_PAIR));
   }

   /**
    * Computes the noise term given the fano factor, resolution at a specified
    * energy and the energy.
    * 
    * @param fano
    *           Fano factor (nominally ~0.122)
    * @param gRes
    *           Gaussian resolution at eV
    * @param eV
    *           The energy at which gRes was measured in ev
    * @return The noise term
    */
   static public UncertainValue2 noiseFromResolution(UncertainValue2 fano,
         UncertainValue2 gRes, double eV) {
      return UncertainValue2.sqrt(UncertainValue2.subtract(
            UncertainValue2.sqr(
                  UncertainValue2.multiply(1.0 / ENERGY_PER_EH_PAIR, gRes)),
            UncertainValue2.multiply(eV / ENERGY_PER_EH_PAIR, fano)));
   }

   /**
    * Sample to detector distance in meters.
    * 
    * @param props
    * @param def
    *           The default returned if the detector position not available
    * @return double Meters
    */
   static public double sampleToDetectorDistance(SpectrumProperties props,
         double def) {
      double res = def;
      final double[] pos = getDetectorPosition(props);
      if (pos != null) {
         final double[] sample = getSamplePosition(props);
         res = Math2.distance(sample, pos);
         assert (res > 1.0e-3) && (res < 1.0) : Double.toString(res) + " m";
      }
      return res;
   }

   /**
    * Remaps a spectrum to a specified number of bins which may be more or less
    * than the current number. Returns a new spectrum object unless the spectrum
    * already has the correct number of channels.
    * 
    * @param spec
    * @param nChannels
    * @return ISpectrumData
    */
   static public ISpectrumData remap(ISpectrumData spec, int nChannels) {
      class RemapSpectrum extends DerivedSpectrum {
         final double[] mData;

         RemapSpectrum(ISpectrumData spec, int chCount) {
            super(spec);
            mData = new double[chCount];
            final double[] tmp = SpectrumUtils.toDoubleArray(spec);
            for (int i = 0; i < mData.length; ++i)
               mData[i] = (i < tmp.length ? tmp[i] : 0.0);
         }

         @Override
         public int getChannelCount() {
            return mData.length;
         }

         @Override
         public double getCounts(int i) {
            return mData[i];
         }
      }
      return spec.getChannelCount() == nChannels
            ? spec
            : new RemapSpectrum(spec, nChannels);
   }

   static public ISpectrumData remap(ISpectrumData spec, double zero,
         double chWidth) {
      final double dE = SpectrumUtils.maxEnergyForChannel(spec,
            spec.getChannelCount() - 1) - zero;
      final int nCh = (int) (dE / chWidth);
      return remap(spec, zero, chWidth, nCh);
   }

   static public ISpectrumData remap(ISpectrumData spec, double zero,
         double chWidth, int nChannels) {
      class RemapSpectrum extends BaseSpectrum {

         final SpectrumProperties mProperties;
         final double[] mData;

         RemapSpectrum(double zeroOff, double chWidth, int nCh,
               ISpectrumData spec) {
            mProperties = spec.getProperties().clone();
            setEnergyScale(zeroOff, chWidth);
            mData = new double[nCh];
            double eMax = getZeroOffset();
            for (int ch = 0; ch < nCh; ++ch) {
               final double eMin = eMax;
               eMax = zeroOff + ((ch + 1) * chWidth);
               final double chMin = (eMin - spec.getZeroOffset())
                     / spec.getChannelWidth();
               final double chMax = (eMax - spec.getZeroOffset())
                     / spec.getChannelWidth();
               final int iMin = (int) Math.ceil(chMin);
               final int iMax = (int) Math.floor(chMax);
               double sum = 0.0;
               if (((iMin - 1) > 0) && (iMin < spec.getChannelCount()))
                  sum += (iMin - chMin) * spec.getCounts(iMin - 1);
               if ((iMax > 0) && (iMax < spec.getChannelCount()))
                  sum += (chMax - iMax) * spec.getCounts(iMax);
               for (int i = SpectrumUtils.bound(spec, iMin); i < SpectrumUtils
                     .bound(spec, iMax); ++i)
                  sum += spec.getCounts(i);
               mData[ch] = sum;
            }
            final NumberFormat df4 = new HalfUpFormat("0.0000");
            final NumberFormat df1 = new HalfUpFormat("0.0");
            SpectrumUtils.rename(this,
                  "Remap[" + spec.toString() + "," + df4.format(chWidth)
                        + " eV/ch," + df1.format(zeroOff) + "eV,"
                        + Integer.toString(nCh) + "]");
         }

         @Override
         public int getChannelCount() {
            return mData.length;
         }

         @Override
         public double getCounts(int i) {
            return mData[i];
         }

         @Override
         public SpectrumProperties getProperties() {
            return mProperties;
         }
      }
      return new RemapSpectrum(zero, chWidth, nChannels, spec);
   }

   /**
    * Constructs a LinearizeSpectrum by assuming the energy scale for the 'spec'
    * is specified by the polynomial coefficients in 'poly'. The channel width
    * in the new spectrum is specified by chWidth.
    * 
    * @param spec
    *           ISpectrumData
    * @param eScale
    *           double[]
    * @param chWidth
    *           double
    */
   public final static ISpectrumData linearizeSpectrum(ISpectrumData spec,
         double[] eScale, double chWidth) {
      final double minE = Math2.polynomial(eScale,
            SpectrumUtils.minEnergyForChannel(spec, 0));
      final double maxE = Math2.polynomial(eScale,
            SpectrumUtils.minEnergyForChannel(spec, spec.getChannelCount()));
      final int nCh = (int) Math.ceil((maxE - minE) / chWidth);
      final EditableSpectrum res = new EditableSpectrum(nCh, chWidth, minE);
      double lowE = SpectrumUtils.minEnergyForChannel(spec, 0);
      for (int ch = 0; ch < nCh; ++ch) {
         final double highE = SpectrumUtils.maxEnergyForChannel(res, ch);
         res.setCounts(ch, SpectrumUtils.integrate(spec, lowE, highE));
         lowE = highE;
      }
      SpectrumUtils.rename(res, "Linearized[" + spec.toString() + ","
            + Arrays.toString(eScale) + "]");
      return SpectrumUtils.copy(res);
   }

   /**
    * Constructs a LinearizeSpectrum by assuming the energy scale for the 'spec'
    * is specified by the polynomial coefficients in 'poly'. The channel width
    * in the new spectrum is specified by chWidth.
    * 
    * @param spec
    *           ISpectrumData
    * @param scale
    *           double[]
    * @param chWidth
    *           double
    * @throws EPQException
    */
   public final static ISpectrumData linearizeSpectrum2(ISpectrumData spec,
         double[] scale, double chWidth) throws EPQException {
      final double minE = Math2.polynomial(scale, 0.0);
      final double maxE = Math2.polynomial(scale, spec.getChannelCount());
      final int nCh = (int) Math.ceil((maxE - minE) / chWidth);
      final EditableSpectrum res = new EditableSpectrum(nCh, chWidth, minE);
      final double[] xx = Math2.solvePoly(scale,
            SpectrumUtils.minEnergyForChannel(res, 0));
      double oldLowCh = xx != null ? Math2.closestTo(xx, 0.0) : 0.0;
      for (int ch = 0; ch < nCh; ++ch) {
         final double oldHighCh = Math2.closestTo(Math2.solvePoly(scale,
               SpectrumUtils.maxEnergyForChannel(res, ch)), oldLowCh);
         final double oldLowE = spec.getZeroOffset()
               + (spec.getChannelWidth() * oldLowCh);
         final double oldHighE = spec.getZeroOffset()
               + (spec.getChannelWidth() * oldHighCh);
         res.setCounts(ch, SpectrumUtils.integrate(spec, oldLowE, oldHighE));
         oldLowCh = oldHighCh;
      }
      SpectrumUtils.rename(res, "Linearized2[" + spec.toString() + ","
            + Arrays.toString(scale) + "]");
      return SpectrumUtils.copy(res);
   }

   public final static ISpectrumData applyEDSDetector(EDSDetector det,
         ISpectrumData spec) {
      ISpectrumData res;
      if ((det != null) && ((spec.getProperties().getDetector() != det)
            || (spec.getChannelCount() != det.getChannelCount()))) {
         res = remap(spec, det.getChannelCount());
         res.getProperties().setDetector(det);
      } else
         res = spec;
      return res;
   }

   /**
    * Using a background corrected integration estimate the signal-to-noise for
    * the specified spectrum over the specified ROI.
    * 
    * @param roi
    * @param spec
    * @return double
    */
   public static double computeSignalToNoise(
         RegionOfInterestSet.RegionOfInterest roi, ISpectrumData spec) {
      final double[] specInt = backgroundCorrectedIntegral(spec,
            FromSI.eV(roi.lowEnergy()), FromSI.eV(roi.highEnergy()));
      return specInt[0] / specInt[1];
   }

   /**
    * Returns true in the channel count, zero offset, channel width and channel
    * data are equal between spec1 and spec2.
    *
    * @param spec1
    * @param spec2
    * @return boolean
    */
   public static boolean equalData(ISpectrumData spec1, ISpectrumData spec2) {
      if (spec1.getChannelCount() != spec2.getChannelCount())
         return false;
      if (spec1.getChannelWidth() != spec2.getChannelWidth())
         return false;
      if (spec1.getZeroOffset() != spec2.getZeroOffset())
         return false;
      for (int i = 0; i < spec1.getChannelCount(); ++i)
         if (spec1.getCounts(i) != spec2.getCounts(i))
            return false;
      return true;
   }

   public static ISpectrumData sum(Collection<ISpectrumData> specs) {
      SpectrumMath res = null;
      for (ISpectrumData s : specs)
         if (res == null)
            res = new SpectrumMath(s);
         else
            res.add(s, 1.0);
      return SpectrumUtils.copy(res);
   }

   public static double chiSqr(ISpectrumData spec1, ISpectrumData spec2,
         Collection<Interval> intervals) throws EPQException {
      double k1 = 1.0 / SpectrumUtils.getDose(spec1.getProperties());
      double k2 = 1.0 / SpectrumUtils.getDose(spec2.getProperties());
      double d = 0.0, su = 0.0;
      for (Interval ii : intervals)
         for (int i = ii.min(); i < ii.max(); ++i) {
            d += Math2.sqr(k1 * spec1.getCounts(i) - k2 * spec2.getCounts(i));
            su += k1 * k1 * Math.max(1.0, spec1.getCounts(i))
                  + k2 * k2 * Math.max(1.0, spec2.getCounts(i));
         }
      return (d / su);
   }

   public static SortedSet<Interval> asIntervals(ISpectrumData spec,
         RegionOfInterestSet rois) {
      final double e0 = SpectrumUtils.getBeamEnergy(spec);
      SortedSet<Interval> intervals = new TreeSet<Interval>();
      for (RegionOfInterestSet.RegionOfInterest roi : rois) {
         if (roi.lowEnergy() < e0) {
            final Interval i = new Interval(
                  SpectrumUtils.channelForEnergy(spec,
                        FromSI.eV(roi.lowEnergy())),
                  SpectrumUtils.channelForEnergy(spec,
                        Math.min(FromSI.eV(roi.highEnergy()), e0)));
            intervals = Interval.add(intervals, i);
         }
      }
      return intervals;
   }

   public static double measureDissimilarity(ISpectrumData spec,
         Collection<ISpectrumData> specs, RegionOfInterestSet rois)
         throws EPQException {
      assert !specs.contains(spec);
      return specs.size() == 0
            ? 1.0
            : chiSqr(spec, sum(specs), asIntervals(spec, rois));
   }
}