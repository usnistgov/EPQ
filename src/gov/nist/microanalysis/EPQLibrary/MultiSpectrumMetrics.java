/**
 * gov.nist.microanalysis.EPQLibrary.MultiSpectrumMetrics Created by: nicho
 * Date: Mar 27, 2019
 */
package gov.nist.microanalysis.EPQLibrary;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collection;

import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * A class for comparing spectra collected on multiple detectors simultaneously.
 * When the spectra are similar, the score is low (displays as close to 100 in
 * plot). This suggests that the spectra don't suffer from shadowing or other
 * particle or rough surface related effects.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author nicho
 * @version 1.0
 */
public class MultiSpectrumMetrics {

   private static ISpectrumData[] asArray(final Collection<ISpectrumData> spectra) {
      final ISpectrumData[] res = new ISpectrumData[spectra.size()];
      int i = 0;
      for (final ISpectrumData spec : spectra)
         res[i++] = spec;
      return res;
   }

   private final ISpectrumData[] mSpectra;
   private transient int mMinChannel;
   private transient double[] mMean = null;

   private transient double[][] mRatio = null;

   public MultiSpectrumMetrics(final Collection<ISpectrumData> spectra) throws EPQException {
      this(asArray(spectra));
   }

   public MultiSpectrumMetrics(final ISpectrumData[] spectra) throws EPQException {
      mSpectra = spectra;
      int chCount = 0;
      double eVperCh = 0.0, offset = 0.0;
      for (final ISpectrumData spec : spectra) {
         if (chCount == 0)
            chCount = spec.getChannelCount();
         if (spec.getChannelCount() != chCount)
            throw new EPQException("All the spectra must have the same number of channels");
         if (eVperCh == 0.0)
            eVperCh = spec.getChannelWidth();
         if (offset == 0.0)
            offset = spec.getZeroOffset();
         if (Math.abs(spec.getChannelWidth() - eVperCh) > 0.01)
            throw new EPQException("All the spectra must have similar energy scale calibrations.");
         if (Math.abs(spec.getZeroOffset() - offset) > 5.0)
            throw new EPQException("All the spectra must have similar zero offsets.");
      }
   }

   private void computeMeanAndRatio() {
      if (mMean == null) {
         int minCh = 0, specLen = 0, maxCh = 100000;
         for (final ISpectrumData spec : mSpectra) {
            final SpectrumProperties sp = spec.getProperties();
            if (sp.isDefined(SpectrumProperties.LLD)) {
               final int lld = (int) Math.round(sp.getNumericWithDefault(SpectrumProperties.LLD, 0.0));
               if (lld > minCh)
                  minCh = lld;
            }
            assert (specLen == 0) || (specLen == spec.getChannelCount());
            specLen = spec.getChannelCount();
            final double e0 = sp.getNumericWithDefault(SpectrumProperties.BeamEnergy, 20.0);
            maxCh = Math.min(maxCh, SpectrumUtils.channelForEnergy(spec, 0.9 * 1000.0 * e0));

         }
         final double[] mean = new double[Math.min(specLen, maxCh) - minCh];
         final double[][] res = new double[mSpectra.length][mean.length];
         double thresh = 0.0;
         for (int i = 0; i < mSpectra.length; ++i) {
            final ISpectrumData spec = mSpectra[i];
            final double total = SpectrumUtils.sumCounts(spec, minCh, specLen);
            thresh = 10.0 / total;
            for (int ch = minCh; ch < mean.length; ++ch) {
               final double norm = spec.getCounts(ch) / total;
               res[i][ch - minCh] = norm;
               mean[ch - minCh] += norm / mSpectra.length;
            }
         }
         for (int i = 0; i < mSpectra.length; ++i)
            for (int ch = minCh; ch < mean.length; ++ch)
               res[i][ch - minCh] = (mean[ch - minCh] >= thresh ? res[i][ch - minCh] / mean[ch - minCh] : 0.0);
         mMinChannel = minCh;
         mMean = mean;
         mRatio = res;
      }
   }

   public double computeRatioScore(final double minE) {
      double sum = 0.0;
      for (int i = 0; i < mRatio.length; ++i)
         sum += computeSpectrumScore(i, minE);
      return 1.0 - (sum / mSpectra.length);
   }

   /**
    * Computes an individual spectrum score
    *
    * @param specIndex
    * @param minE
    *           in eV
    * @return
    */
   public double computeSpectrumScore(final int specIndex, final double minE) {
      // Use the same channel for all spectra
      computeMeanAndRatio();
      final int lowCh = Math.max(mMinChannel, SpectrumUtils.channelForEnergy(mSpectra[0], minE));
      double sum = 0.0;
      for (int ch = lowCh; ch < mMean.length; ++ch)
         sum += Math.abs(mRatio[specIndex][ch] - 1.0);
      return sum / (mMean.length - lowCh);
   }

   public int getSpectrumCount() {
      return mSpectra.length;
   }

   public double[] getSpectrumRatio(final int specIdx) {
      return Arrays.copyOf(mRatio[specIdx], mRatio[specIdx].length);
   }

   public BufferedImage plotRatios(final int width, final int height, final Color[] specColor) {
      computeMeanAndRatio();
      final BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
      final Graphics2D g2 = (Graphics2D) bi.getGraphics();
      // Draw grids
      g2.setColor(new Color(0xFC, 0xFC, 0xF2));
      g2.fillRect(0, 0, width, height);
      g2.setColor(new Color(0xFF, 0xE0, 0xE0));
      g2.drawRect(0, 0, width - 1, height - 1);
      g2.drawLine(0, height / 2, width - 1, height / 2);
      // Display the score in a colored circle.
      final HalfUpFormat huf = new HalfUpFormat("0.0");
      final double score = computeRatioScore(100.0);
      final String scStr = huf.format(100.0 * score);
      final FontMetrics fm = g2.getFontMetrics();
      final int sw = fm.stringWidth(scStr);
      final int TRANS = 192;
      Color scColor = new Color(255, 0, 0, TRANS);
      if (score > 0.998)
         scColor = new Color(0, 255, 0, TRANS);
      else if (score < 0.99)
         scColor = new Color(Color.yellow.getRed(), Color.yellow.getGreen(), Color.yellow.getBlue(), TRANS);
      g2.setColor(scColor);
      final int ow = (5 * sw) / 4;
      final int l = ((8 * width) / 10) - (ow / 2), t = (height / 5) - (ow / 2);
      g2.fillOval(l, t, ow + 1, ow + 1);
      g2.setColor(new Color(0xC0, 0xC0, 0xC0));
      g2.drawOval(l, t, ow, ow);
      g2.drawString(scStr, ((8 * width) / 10) - (sw / 2), ((height / 5) + (fm.getHeight() / 2)) - fm.getDescent());
      // Plot the ratio
      for (int i = 0; i < mRatio.length; ++i) {
         final double[] ratio = mRatio[i];
         final double rlow = ratio.length / (width - 2);
         final Color color = new Color(specColor[i].getRed(), specColor[i].getGreen(), specColor[i].getBlue(), TRANS);
         final Color badColor = new Color(255, 0, 0, TRANS);
         g2.setColor(color);
         for (int x = 0; x < (width - 2); ++x) {
            final int minCh = Math.max((int) (rlow * x), mMinChannel), maxCh = (int) (rlow * (x + 1)) - 1;
            double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
            for (int ch = minCh; ch < maxCh; ++ch) {
               min = Math.min(min, ratio[ch]);
               max = Math.max(max, ratio[ch]);
            }
            if (max < 0.0) {
               g2.setColor(badColor);
               g2.drawLine(x + 1, height - 2, x + 1, height - 3);
               g2.setColor(color);
            } else if (min > 2.0) {
               g2.setColor(badColor);
               g2.drawLine(x + 1, 2, x + 1, 1);
               g2.setColor(color);
            } else if (max != 0.0)
               g2.drawLine(x + 1, (int) (0.5 * min * (height - 2)) + 1, //
                     x + 1, (int) ((0.5 * max * (height - 2)) + 1));
         }
      }
      return bi;
   }

   public BufferedImage plotRatios(final int width, final int height) {
      final Color[] specColor = new Color[mSpectra.length];
      for (int i = 0; i < specColor.length; ++i)
         specColor[i] = Color.darkGray;
      return plotRatios(width, height, specColor);
   }

}
