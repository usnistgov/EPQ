package gov.nist.microanalysis.NISTMonte.Gen3;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.Utility.CSVReader.ResourceReader;
import gov.nist.microanalysis.Utility.LazyEvaluate;

/**
 * <p>
 * Creates a bitmap image showing the generation of detected x-rays as a
 * function of position. The thermal color scale shows white where the most
 * x-rays are generated and black where almost no or no x-rays are generated.
 * The left and bottom edge show accumulated transmitted phi-rho-z and phi-rho-x
 * (projected) curves.
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

public abstract class EmissionImageBase implements ActionListener {
   private double mXMin, mXScale;
   private double mYMin, mYScale;
   private final float[][] mBuffer;
   protected double mIntensityScale = 1.0;
   private boolean mLogScale = false;
   protected boolean mEmission = true;
   protected int mMaxTrajectories = Integer.MAX_VALUE;
   protected int mTrajectoryCount = 0;
   protected boolean mLabel = true;
   private final LazyEvaluate<Double> mMaxIntensity = new LazyEvaluate<Double>() {

      @Override
      protected Double compute() {
         double max = 0.0;
         for (int h = 0; h < getHeight(); h++)
            for (int w = 0; w < getWidth(); w++)
               if (mBuffer[h][w] > max)
                  max = mBuffer[h][w];
         return Double.valueOf(max <= 0.0 ? 1.0 : max);
      }

   };

   private final LazyEvaluate<BufferedImage> mImage = new LazyEvaluate<BufferedImage>() {

      @Override
      protected BufferedImage compute() {
         final int width = getWidth(), height = getHeight();
         BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, sPalette);
         final Graphics gr = res.getGraphics();
         gr.setColor(Color.black);
         gr.fillRect(0, 0, width, height);

         final double[] phiRhoZ = new double[height];
         final double[] phiRhoX = new double[width];
         double maxPhiZ = 0.0, maxPhiX = 0.0;
         for (int yy = 0; yy < height; ++yy) {
            final float[] line = mBuffer[yy];
            double sum = 0.0;
            for (int xx = 0; xx < width; ++xx) {
               sum += line[xx];
               phiRhoX[xx] += line[xx];
            }
            phiRhoZ[yy] = sum;
            if (sum > maxPhiZ)
               maxPhiZ = sum;
         }
         for (int xx = 0; xx < width; ++xx)
            if (maxPhiX < phiRhoX[xx])
               maxPhiX = phiRhoX[xx];
         // Clear image
         final Graphics dup = res.getGraphics().create();
         dup.setColor(Color.BLACK);
         dup.fillRect(0, 0, width, height);
         // Draw main image
         final double max = getMaxIntensity();
         if (mLogScale)
            // float scale = (float) (255.0 / Math.log(max));
            for (int yy = 0; yy < height; ++yy) {
               final float[] line = mBuffer[yy];
               for (int xx = 0; xx < width; ++xx)
                  if (line[xx] > 0.0) {
                     final int index = (int) Math.round((255.0 * line[xx]) / max);
                     // Math.round(scale*Math.log(line[xx]));
                     if (index > 0) {
                        assert (index <= 255);
                        res.setRGB(xx, yy, res.getColorModel().getRGB(index));
                     }
                  }
            }
         else {
            final float scale = (float) (255.0 / max);
            for (int yy = 0; yy < height; ++yy) {
               final float[] line = mBuffer[yy];
               for (int xx = 0; xx < width; ++xx) {
                  final int index = Math.round(scale * line[xx]);
                  assert index <= 255;
                  res.setRGB(xx, yy, res.getColorModel().getRGB(index));
               }
            }
         }
         // Draw phi-rho-z
         for (int yy = 0; yy < height; ++yy) {
            final int rgb = res.getColorModel().getRGB((int) Math.round((255.0 * phiRhoZ[yy]) / maxPhiZ));
            for (int xx = 0; xx < (width / 100); ++xx)
               res.setRGB(xx + 1, yy, rgb);
         }
         // Draw phi-rho-x
         for (int xx = 0; xx < width; ++xx) {
            final int rgb = res.getColorModel().getRGB((int) Math.round((255.0 * phiRhoX[xx]) / maxPhiX));
            for (int yy = 0; yy < (height / 100); ++yy)
               res.setRGB(xx, height - (yy + 2), rgb);
         }
         if (mLabel) {
            dup.setColor(Color.white);
            dup.setFont(new Font("sanserif", Font.PLAIN, (10 * width) / 256));
            dup.drawString(getTitle(), 10, dup.getFontMetrics().getHeight());
            final NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits(2);
            {
               final double[] fov = getFOV();
               final String tmp = nf.format(1.0e6 * fov[0]) + " \u00B5m \u00D7 " + //
                     nf.format(1.0e6 * fov[1]) + " \u00B5m";
               dup.drawString(tmp, width - (dup.getFontMetrics().stringWidth(tmp) + 10), dup.getFontMetrics().getHeight());
            }
            {
               final double scale = getIntensityScaleFactor();
               if (nf instanceof DecimalFormat)
                  ((DecimalFormat) nf).applyPattern(scale < 0.01 ? "0.##E0;-0.##E0" : "0.000");
               dup.drawString(nf.format(scale), 10, height - 10);
            }
            {
               final String desc = getType();
               dup.drawString(desc, width - (dup.getFontMetrics().stringWidth(desc) + 10), height - 10);
            }
         }
         return res;
      }

   };

   public String getType() {
      return mEmission ? "Emission" : "Generation";
   }

   public double[] getFOV() {
      return new double[]{getWidth() / mXScale, getHeight() / mYScale};
   }

   public double getIntensityScaleFactor() {
      return getMaxIntensity() / mIntensityScale;
   }

   protected static IndexColorModel sPalette = createColorModel("palette.csv");

   /**
    * Set the color model used to color the output image to some arbitrary
    * model.
    *
    * @param ixm
    */
   public static void updateIndexColorModel(IndexColorModel ixm) {
      sPalette = ixm;
   }

   /**
    * Use a gray-scale palette to colorize the image.
    */
   public static void useGrayScalePalette() {
      updateIndexColorModel(createColorModel("palette_bw.csv"));
   }

   /**
    * Use a heat map palette to colorize the image.
    */
   public static void useHeatMapPalette() {
      updateIndexColorModel(createColorModel("palette.csv"));
   }

   /**
    * createThermalColorModel - Creates a thermal color scheme palette to apply
    * to this image. The palette is mapped such that palette index 0 corresponds
    * to black and palette index 255 corresponds to white.
    * 
    * @param palResName
    *           Palette resource name
    * @return IndexColorModel
    */
   public static IndexColorModel createColorModel(String palResName) {
      final ResourceReader rr = new ResourceReader(palResName, false);
      final double[][] pal = rr.getResource(EmissionImageBase.class);
      assert pal.length == 256;
      final short[][] res = new short[256][3];
      for (int i = 0; i < 256; ++i)
         for (int j = 0; j < 3; ++j)
            res[i][j] = (short) Math.round(pal[i][j]);
      final byte[] r = new byte[256];
      final byte[] g = new byte[256];
      final byte[] b = new byte[256];
      for (int i = 0; i < 256; ++i) {
         r[i] = (byte) pal[i][0];
         g[i] = (byte) pal[i][1];
         b[i] = (byte) pal[i][2];
      }
      return new IndexColorModel(8, 256, r, g, b);
   }

   /**
    * dumpToFile - Dump this MCSS_TrajectoryImage to a file in the specified
    * directory.
    * 
    * @param dest
    *           String - The destination directory
    * @throws FileNotFoundException
    * @throws IOException
    */
   public void dumpToFile(String dest) throws FileNotFoundException, IOException, EPQException {
      if (dest.endsWith("\\") || dest.endsWith("/"))
         dest = dest.substring(0, dest.length() - 1);
      new File(dest).mkdirs();
      final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
      if (writers.hasNext()) {
         final ImageWriter writer = writers.next();
         final File f = new File(dest, getTitle() + ".png");
         final ImageOutputStream ios = ImageIO.createImageOutputStream(f);
         writer.setOutput(ios);
         writer.write(getImage());
         ios.close();
      } else
         throw new EPQException("No suitable image writer available.");

   }

   @Override
   public String toString() {
      return "Image[" + this.getWidth() + "," + this.getHeight() + "," + getTitle() + "]";
   }

   /**
    * dumpToFiles - Dump a list of MCSS_Image into files within the specified
    * destination directory.
    * 
    * @param imgs
    *           MCSS_Image[]
    * @param dest
    *           String
    * @throws FileNotFoundException
    * @throws IOException
    */
   public static void dumpToFiles(Collection<EmissionImageBase> imgs, String dest) throws FileNotFoundException, IOException, EPQException {
      for (final EmissionImageBase img : imgs)
         img.dumpToFile(dest);
   }

   /**
    * monteCarloImage - Creates a monteCarloImage instance with the standard
    * thermal color palette of the specified size (width and height).
    * 
    * @param width
    *           int
    * @param height
    *           int
    */
   public EmissionImageBase(int width, int height) {
      this.mBuffer = new float[height][width];
      mXMin = -1.0e-5;
      mXScale = 1.0 / 2.0e-5;
      mYMin = -1.0e-5;
      mYScale = 1.0 / 2.0e-5;
   }

   public int getHeight() {
      return mBuffer.length;
   }

   public int getWidth() {
      return mBuffer[0].length;
   }

   /**
    * setXRange - Sets the range of x values that will display on the image. min
    * is left and max is right.
    * 
    * @param min
    *           double
    * @param max
    *           double
    */
   public void setXRange(double min, double max) {
      mXMin = min;
      mXScale = getWidth() / (max - min);
      resetImage();
   }

   /**
    * setYRange - Sets the range of y-values that will display on the image. min
    * is top and max is bottom. This method is very poorly named because what it
    * really refers to is the Z coordinate in the simulation.
    * 
    * @param min
    *           double
    * @param max
    *           double
    */
   public void setYRange(double min, double max) {
      mYMin = min;
      mYScale = getHeight() / (max - min);
      resetImage();
   }

   /**
    * getMaxIntensity - Get the intensity of the pixel the generates the most
    * detected x-rays. Valid only after a trajectory set ends.
    * 
    * @return double
    */
   public double getMaxIntensity() {
      return mMaxIntensity.get().doubleValue();
   }

   protected void resetImage() {
      mImage.reset();
      mMaxIntensity.reset();
      mIntensityScale = 1.0;
   }

   public BufferedImage getImage() {
      return mImage.get();
   }

   /**
    * Scales the emission images such that the pixel with the most emission in
    * any image is assigned a value of 1. All the other images are scaled
    * relative to this image and this is the number written on the image
    * lower-left corner.
    *
    * @param imgs
    */
   public static void scaleEmissionImages(Collection<? extends EmissionImageBase> imgs) {
      double maxI = 0.0;
      for (EmissionImageBase eib : imgs)
         maxI = Math.max(maxI, eib.getMaxIntensity());
      for (EmissionImageBase eib : imgs)
         eib.mIntensityScale = maxI;
   }

   /**
    * setPixel - Sets the pixel at position (x, y) with the color corresponding
    * to val. val is [0.0,1.0]
    * 
    * @param x
    *           double
    * @param y
    *           double
    * @param val
    *           double - between 0.0 and 1.0 inclusive
    */
   public void setPixel(double x, double y, double val) {
      assert (val >= 0.0);
      final int xx = (int) (mXScale * (x - mXMin));
      final int yy = (int) (mYScale * (y - mYMin));
      if ((xx >= 0) && (xx < getWidth()) && (yy >= 0) && (yy < getWidth()))
         mBuffer[yy][xx] += (float) val;
   }

   /**
    * setMaxTrajectories - Sets the maximum number of trajectories that will be
    * added to the image.
    * 
    * @param max
    *           int
    */
   public void setMaxTrajectories(int max) {
      mMaxTrajectories = max;
   }

   /**
    * Gets the current value assigned to logScale
    * 
    * @return Returns the logScale.
    */
   public boolean isLogScale() {
      return mLogScale;
   }

   /**
    * Sets the value assigned to logScale.
    * 
    * @param logScale
    *           The value to which to set logScale.
    */
   public void setLogScale(boolean logScale) {
      if (mLogScale != logScale) {
         mLogScale = logScale;
         mImage.reset();
      }
   }

   /**
    * Display an emission image vs display a generation image
    * 
    * @return boolean
    */
   public boolean isEmission() {
      return mEmission;
   }

   /**
    * Sets whether to display an emission image (default) or a generation image
    * (false).
    * 
    * @param emission
    */
   public void setEmission(boolean emission) {
      mEmission = emission;
   }

   public boolean getLabel() {
      return mLabel;
   }

   public void setLabel(boolean label) {
      mLabel = label;
   }

   public void dumpPalette(File file, int height) throws IOException {
      final int width = 256;
      BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, sPalette);
      for (int xx = 0; xx < 256; ++xx) {
         for (int yy = 0; yy < height; ++yy)
            res.setRGB(xx, yy, res.getColorModel().getRGB(xx));
      }
      final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
      try (final ImageOutputStream ios = ImageIO.createImageOutputStream(file)) {
         final ImageWriter writer = writers.next();
         writer.setOutput(ios);
         writer.write(res);
      }
   }

   abstract protected String getTitle();

   /**
    * actionPerformed - Implements actionPerformed for the ActionListener
    * interface.
    * 
    * @param e
    *           ActionEvent
    */
   @Override
   abstract public void actionPerformed(ActionEvent e);

}
