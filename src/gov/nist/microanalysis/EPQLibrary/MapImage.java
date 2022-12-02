package gov.nist.microanalysis.EPQLibrary;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Set;

import javax.imageio.ImageIO;

import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.EPQTools.RippleFile;
import gov.nist.microanalysis.EPQTools.ScaledImage;
import gov.nist.microanalysis.Utility.DescriptiveStatistics;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * A class for translating x-ray map double matrices into a grey-scale images.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author nicholas
 * @version 1.0
 */
public class MapImage {

   private double mSumThresh = 0.1;

   public enum DataType {
      K_RATIOS, PEAK_INTEGRALS, COMPOSITION
   };

   private final double[][][] mData; // [ROI or ELM], X, Y
   private final String mDescription;
   private final RegionOfInterest[] mROIS;
   private final DataType mType;
   private double[][] mSum;
   private DescriptiveStatistics mSumStats;

   private static void validateROIS(RegionOfInterest[] rois) {
      for (final RegionOfInterest roi : rois) {
         assert roi != null;
         assert roi.getElementSet() != null;
         assert roi.getElementSet().size() >= 1;
      }
   }

   public MapImage(int width, int height, RegionOfInterest[] rois, String desc, DataType dt) {
      validateROIS(rois);
      mData = new double[rois.length][width][height];
      mROIS = rois.clone();
      mDescription = desc;
      mType = dt;
   }

   public MapImage(int width, int height, Set<RegionOfInterest> rois, String desc, DataType dt) {
      this(width, height, rois.toArray(new RegionOfInterestSet.RegionOfInterest[rois.size()]), desc, dt);
   }

   public int depth() {
      return mData.length;
   }

   public int width() {
      return mData[0].length;
   }

   public int height() {
      return mData[0][0].length;
   }

   public double maxValue(int index) {
      return Math2.max(mData[index]);
   }

   public RegionOfInterest getROI(int idx) {
      return mROIS[idx];
   }

   /**
    * Applies a quantiative correction to this MapImage data set and returns a
    * new MapImage object containing Compositions.
    *
    * @param ckr
    * @param props
    * @param normalize
    * @return MapImage
    */
   public MapImage quantify(CompositionFromKRatios ckr, SpectrumProperties props, boolean normalize) {
      if (mType == DataType.K_RATIOS) {
         final int width = width(), height = height();
         final MapImage res = new MapImage(width, height, mROIS, "Quantified[" + mDescription + "]", DataType.COMPOSITION);
         final XRayTransitionSet[] xrtss = new XRayTransitionSet[mROIS.length];
         for (int i = 0; i < xrtss.length; ++i)
            xrtss[i] = new XRayTransitionSet(mROIS[i].getXRayTransitionSet(mROIS[i].getElementSet().first()).getWeighiestTransition());
         for (int y = 0; y < height; ++y)
            for (int x = 0; x < width; ++x) {
               final KRatioSet krs = new KRatioSet();
               for (int i = 0; i < xrtss.length; ++i)
                  krs.addKRatio(xrtss[i], Math.max(0.0, mData[i][x][y]), 0.0);
               Composition comp;
               try {
                  comp = ckr.compute(krs, props);
                  for (int i = 0; i < xrtss.length; ++i)
                     res.mData[i][x][y] = Math.max(0.0, comp.weightFraction(xrtss[i].getElement(), normalize));
               } catch (final EPQException e) {
                  for (int i = 0; i < xrtss.length; ++i)
                     res.mData[i][x][y] = 0.0;
               }
            }
         return res;
      } else
         return null;
   }

   private void calculateSumMap() {
      if (mSum == null) {
         final int width = width(), height = height(), depth = depth();
         mSum = new double[width][height];
         mSumStats = new DescriptiveStatistics();
         for (int x = 0; x < width; ++x)
            for (int y = 0; y < height; ++y) {
               double sum = 0.0;
               for (int idx = 0; idx < depth; ++idx)
                  sum += Math.max(mData[idx][x][y], 0.0);
               mSum[x][y] = sum;
               if (sum > 0.0)
                  mSumStats.add(sum);
            }
      }
   }

   public DescriptiveStatistics getKRatioStats() {
      calculateSumMap();
      return mSumStats.clone();
   }

   /**
    * Returns a grey scale image in which all pixels below the threshold are set
    * to black. The threshold is scaled by getKRatioStats().average().
    *
    * @param index
    * @param thresh
    * @return BufferedImage
    */
   public BufferedImage getMask(int index, double thresh, boolean label) {
      final DescriptiveStatistics ds = getKRatioStats();
      final double scaledThresh = thresh * ds.average();
      final double scale = 255.0 / ds.average();
      final BufferedImage bi = new BufferedImage(width(), height(), BufferedImage.TYPE_BYTE_GRAY);
      final DataBufferByte db = (DataBufferByte) bi.getRaster().getDataBuffer();
      final byte[] data = db.getData();
      final int width = width(), height = height();
      final double sumThresh = mSumThresh * mSumStats.average();
      for (int y = 0, off = 0; y < height; ++y)
         for (int x = 0; x < width; ++x, ++off)
            if (mSum[x][y] > sumThresh)
               data[off] = (byte) (mData[index][x][y] >= scaledThresh ? Math2.bound((int) Math.round(mData[index][x][y] * scale), 0, 256) : 0x0);
      if (label)
         return labelImage(bi, mROIS[index].getElementSet().first().toAbbrev(), Color.YELLOW);
      else
         return bi;
   }

   public BufferedImage getImage(int index, double maxI) {
      final BufferedImage bi = new BufferedImage(width(), height(), BufferedImage.TYPE_BYTE_GRAY);
      final DataBufferByte db = (DataBufferByte) bi.getRaster().getDataBuffer();
      final byte[] data = db.getData();
      final double scale = 255.0 / maxI;
      final int width = width(), height = height();
      final double sumThresh = mSumThresh * mSumStats.average();
      for (int y = 0, off = 0; y < height; ++y)
         for (int x = 0; x < width; ++x, ++off)
            if (mSum[x][y] > sumThresh)
               data[off] = (byte) Math2.bound((int) Math.round(mData[index][x][y] * scale), 0, 256);
      return bi;
   }

   private IndexColorModel getLog3Band() {
      final byte[] red = new byte[256];
      final byte[] green = new byte[256];
      final byte[] blue = new byte[256];
      for (int i = 1; i <= 86; ++i) {
         red[i] = green[i] = (byte) ((200 * i) / 86);
         blue[i] = (byte) (60 + ((195 * i) / 86));
      }
      for (int i = 0; i <= (170 - 87); ++i) {
         red[i + 87] = blue[i + 87] = (byte) ((200 * i) / (170 - 87));
         green[i + 87] = (byte) (60 + ((195 * i) / (170 - 87)));
      }
      for (int i = 0; i <= (254 - 171); ++i) {
         red[i + 171] = (byte) (60 + ((195 * i) / (254 - 171)));
         green[i + 171] = blue[i + 171] = (byte) ((200 * i) / (254 - 171));
      }
      red[255] = green[255] = (byte) 255;
      blue[255] = 0;
      return new IndexColorModel(8, 256, red, green, blue);
   }

   private BufferedImage labelImage(BufferedImage src, String label, Color color) {
      final BufferedImage res = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
      final Graphics2D gr = res.createGraphics();
      gr.drawImage(src, 0, 0, null);
      gr.setColor(color);
      gr.setFont(new Font(Font.SANS_SERIF, Font.BOLD, src.getWidth() / 12));
      final FontMetrics fm = gr.getFontMetrics();
      gr.drawString(label, res.getWidth() - (fm.stringWidth(label) + 4), res.getHeight() - (fm.getHeight() / 2));
      return res;
   }

   public BufferedImage getLog3BandImage(int index, boolean label) {
      calculateSumMap();
      final BufferedImage bi = new BufferedImage(width(), height(), BufferedImage.TYPE_BYTE_INDEXED, getLog3Band());
      final DataBufferByte db = (DataBufferByte) bi.getRaster().getDataBuffer();
      final byte[] data = db.getData();
      final int width = width(), height = height();
      final double sumThresh = mSumThresh * mSumStats.average();
      for (int y = 0, off = 0; y < height; ++y)
         for (int x = 0; x < width; ++x, ++off)
            if (mSum[x][y] > sumThresh)
               data[off] = (byte) (254
                     + Math2.bound((int) ((254.0 / 3.0) * Math.log10(Math2.bound(mData[index][x][y] / mSum[x][y], 1.0e-6, 1.0))), -254, 0));
            else
               data[off] = (byte) 0xFF;
      if (label)
         return labelImage(bi, mROIS[index].getElementSet().first().toAbbrev(), Color.YELLOW);
      else
         return bi;
   }

   public BufferedImage getLogImage(int index, boolean label) {
      calculateSumMap();
      final BufferedImage bi = new BufferedImage(width(), height(), BufferedImage.TYPE_BYTE_GRAY);
      final DataBufferByte db = (DataBufferByte) bi.getRaster().getDataBuffer();
      final byte[] data = db.getData();
      final int width = width(), height = height();
      final double sumThresh = mSumThresh * mSumStats.average();
      for (int y = 0, off = 0; y < height; ++y)
         for (int x = 0; x < width; ++x, ++off)
            if (mSum[x][y] > sumThresh)
               data[off] = (byte) (254
                     + Math2.bound((int) ((254.0 / 3.0) * Math.log10(Math2.bound(mData[index][x][y] / mSum[x][y], 1.0e-6, 1.0))), -254, 0));
            else
               data[off] = (byte) 0;
      if (label)
         return labelImage(bi, mROIS[index].getElementSet().first().toAbbrev(), Color.YELLOW);
      else
         return bi;
   }

   public BufferedImage getNormKRatioImage(int index, boolean label) {
      calculateSumMap();
      final BufferedImage bi = new BufferedImage(width(), height(), BufferedImage.TYPE_BYTE_GRAY);
      final DataBufferByte db = (DataBufferByte) bi.getRaster().getDataBuffer();
      final byte[] data = db.getData();
      final int width = width(), height = height();
      for (int y = 0, off = 0; y < height; ++y)
         for (int x = 0; x < width; ++x, ++off) {
            final double d = mData[index][x][y] / mSum[x][y];
            if ((!Double.isNaN(d)) && (d >= (1.0 / 255.0)))
               data[off] = (byte) Math2.bound((int) Math.round((255.0 * mData[index][x][y]) / mSum[x][y]), 0, 256);
            else
               data[off] = (byte) 0;
         }
      if (label)
         return labelImage(bi, mROIS[index].getElementSet().first().toAbbrev(), Color.YELLOW);
      else
         return bi;
   }

   public BufferedImage getKRatioSumImage() {
      calculateSumMap();
      final BufferedImage bi = new BufferedImage(width(), height(), BufferedImage.TYPE_BYTE_GRAY);
      final DataBufferByte db = (DataBufferByte) bi.getRaster().getDataBuffer();
      final byte[] data = db.getData();
      final int width = width(), height = height();
      final double scale = 255.0 / Math2.max(mSum);
      for (int y = 0, off = 0; y < height; ++y)
         for (int x = 0; x < width; ++x, ++off)
            data[off] = (byte) Math2.bound((int) Math.round(mSum[x][y] * scale), 0, 256);
      return bi;
   }

   public double get(int x, int y, int index) {
      return mData[index][x][y];
   }

   public void set(int x, int y, int index, double val) {
      mData[index][x][y] = val;
      mSum = null;
   }

   public void inc(int x, int y, int index, double inc) {
      mData[index][x][y] += inc;
      mSum = null;
   }

   public void inc(int x, int y, double[] inc) {
      assert inc.length == depth();
      for (int index = 0; index < inc.length; ++index)
         mData[index][x][y] += inc[index];
      mSum = null;
   }

   public ScaledImage getScaledImage(int index, double maxI, double hFov, StageCoordinate stgPt) {
      return new ScaledImage(getImage(index, maxI), hFov, (hFov * height()) / width(), stgPt, Integer.toString(index));
   }

   public ScaledImage getScaledImage(int index, double maxI, double hFov, double rotation, StageCoordinate stgPt) {
      return new ScaledImage(getImage(index, maxI), hFov, (hFov * height()) / width(), rotation, stgPt, Integer.toString(index));
   }

   /**
    * Creates a map representing the sum of the k-ratios in which the k-ratios
    * are plotted on a scale such that a k-ratio equal to the average k-ratio is
    * plotted as black, a k-ratio less than the average is plotted in
    * increasingly red tones and a k-ratio more than the average is plotted in
    * increasingly blue tones. Very low sums are in white and very sums are in
    * green.
    *
    * @return A BufferedImage
    */
   public BufferedImage getKRatioSummaryImage() {
      final byte[] r = new byte[256], g = new byte[256], b = new byte[256];
      for (int i = 0; i < 128; ++i) {
         r[i] = (byte) ((2 * (128 - i)) - 1);
         b[255 - i] = (byte) (2 * (127 - i));
      }
      r[0] = (byte) 0xFF;
      g[0] = (byte) 0xFF;
      b[0] = (byte) 0xFF;
      r[255] = (byte) 0x0;
      g[255] = (byte) 0xFF;
      b[255] = (byte) 0x0;
      final double k = 32 / Math.log(2.0);
      final IndexColorModel icm = new IndexColorModel(8, 256, r, g, b);
      final BufferedImage bi = new BufferedImage(width(), height(), BufferedImage.TYPE_BYTE_INDEXED, icm);
      final DataBufferByte db = (DataBufferByte) bi.getRaster().getDataBuffer();
      final byte[] data = db.getData();
      final DescriptiveStatistics ds = getKRatioStats();
      final double avg = ds.average();
      final int width = width(), height = height();
      for (int y = 0, off = 0; y < height; ++y)
         for (int x = 0; x < width; ++x, ++off) {
            final double v = mSum[x][y] / avg;
            data[off] = (byte) Math2.bound((int) Math.round(128.0 + (k * (v <= 0.0 ? -4 : Math.log(v)))), 0, 256);
         }
      return bi;
   }

   @Override
   public String toString() {
      return "MapImage[" + mDescription + "]";
   }

   /**
    * Writes the raw MapImage data to a Ripple/Raw file in 8-byte double format.
    *
    * @param rplFilename
    *           The name and path of file into which to save the RPL file.
    * @throws IOException
    */
   public void writeToRpl(String rplFilename) throws IOException {
      if (!rplFilename.toLowerCase().endsWith(".rpl"))
         rplFilename = rplFilename + ".rpl";
      final String rawFilename = rplFilename.replaceAll(".[rR][pP][lL]$", ".raw");
      try (final RippleFile rf = new RippleFile(width(), height(), depth(), RippleFile.FLOAT, 8, RippleFile.DONT_CARE_ENDIAN, rplFilename,
            rawFilename)) {
         final double[] strip = new double[depth()];
         for (int y = 0; y < height(); ++y)
            for (int x = 0; x < width(); ++x) {
               for (int l = 0; l < depth(); ++l)
                  strip[l] = mData[l][x][y];
               rf.seek(y, x);
               rf.write(strip);
            }
      }
   }

   public void toCSV(Writer fw, NumberFormat nf, int plane) throws IOException {
      fw.write("Plane, ROI, width, height\n");
      fw.write(Integer.toString(plane) + ", " + mROIS[plane].shortName() + "," + //
            Integer.toString(width()) + ", " + Integer.toString(height()) + "\n\n");
      final double sumThresh = mSumThresh * mSumStats.average();
      for (int y = 0; y < height(); ++y) {
         for (int x = 0; x < width(); ++x) {
            if (x > 0)
               fw.write(", ");
            double val = mSum[x][y] > sumThresh ? mData[plane][x][y] / mSum[x][y] : 0.0;
            fw.write(nf.format(Math.max(0.0, val)));
         }
         fw.write("\n");
      }
   }

   public void writeToCSVs(String filename, NumberFormat nf) throws IOException {
      for (int l = 0; l < depth(); ++l) {
         final String name = mROIS[l].shortName();
         final String fn = filename + "[" + name + "].csv";
         try (BufferedWriter fw = new BufferedWriter(new FileWriter(fn))) {
            toCSV(fw, nf, l);
            fw.flush();
         }
      }
   }

   public void writeToCSV(String filename, NumberFormat nf) throws IOException {
      if (!filename.toLowerCase().endsWith(".csv"))
         filename = filename + ".csv";
      try (BufferedWriter fw = new BufferedWriter(new FileWriter(filename))) {
         for (int l = 0; l < depth(); ++l) {
            toCSV(fw, nf, l);
            fw.write("\n");
            fw.flush();
         }
      }
   }

   /**
    * The threshold is useful for eliminating pixels for which the analytical
    * total is much lower than the average analytical total.
    *
    * @return Returns the sumThresh.
    */
   public double getSumThresh() {
      return mSumThresh;
   }

   /**
    * The threshold is useful for eliminating pixels for which the analytical
    * total is much lower than the average analytical total.
    *
    * @param sumThresh
    *           The value to which to set sumThresh (range 0.0 to 1.0)
    */
   public void setSumThresh(double sumThresh) {
      mSumThresh = Math2.bound(sumThresh, 0.0, 1.0);
   }

   /**
    * Write the MapImage data to a set of PNG files in various different
    * formats.
    *
    * @param path
    *           Directory into which to save images
    * @param asLog3
    *           As a Bright-style Log-3 band colorized log-scale image
    * @param asLog
    *           As a grey-scale log image
    * @param asKRatio
    *           As a normalized k-ratio linear greyscale image
    * @throws IOException
    */
   public void save(String path, String prefix, boolean asLog3, boolean asLog, boolean asKRatio, boolean label) throws IOException {
      for (int i = 0; i < depth(); ++i) {
         final String roiName = getROI(i).toString();
         if (asLog3)
            ImageIO.write(getLog3BandImage(i, label), "png", new File(path, "VecsLog3[" + prefix + "][" + roiName + "].png"));
         if (asLog)
            ImageIO.write(getLogImage(i, label), "png", new File(path, "VecsLog[" + prefix + "][" + roiName + "].png"));
         if (asKRatio)
            ImageIO.write(getNormKRatioImage(i, label), "png", new File(path, "VecsNormK[" + prefix + "][" + roiName + "].png"));
      }
      ImageIO.write(getKRatioSumImage(), "png", new File(path, "Analytic Total[" + prefix + "].png"));
      ImageIO.write(getKRatioSummaryImage(), "png", new File(path, "K-ratio map[" + prefix + "].png"));
   }

}
