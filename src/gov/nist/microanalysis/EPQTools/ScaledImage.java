package gov.nist.microanalysis.EPQTools;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

import gov.nist.microanalysis.EPQLibrary.StageCoordinate;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate.Axis;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Histogram;

/**
 * @author nicholas
 */
public class ScaledImage extends BufferedImage implements Cloneable {

   /**
    * Horizontal field-of-view in meters
    */
   static public final String HOR_FIELD_OF_VIEW = "H_FOV";
   /**
    * Vertical field-of-view in meters
    */
   static public final String VERT_FIELD_OF_VIEW = "V_FOV";
   /**
    * Stage position as an array of doubles in stage relevant units. Since
    * stages come in a variety of ill-described (proprietary) geometries it is
    * not practical to translate stage coordinates into some universal format.
    * Instead stage coordinates are stored as received from the hardware.
    * (Standard ordering is X, Y, Z, R, T, B but this is considered optional)
    */
   static public final String STAGE_POINT = "STAGE_POS";

   static public final String IMAGE_ROTATION = "IMAGE_ROTATION";

   static public final String DETECTOR_INDEX = "DETECTOR_IDX";

   static private final Hashtable<String, Object> createProperties(final double hFov, final double vFov, final double rotation,
         final StageCoordinate stgPos, final String detIdx) {
      final Hashtable<String, Object> res = new Hashtable<String, Object>();
      res.put(HOR_FIELD_OF_VIEW, Double.valueOf(hFov));
      res.put(VERT_FIELD_OF_VIEW, Double.valueOf(vFov));
      res.put(IMAGE_ROTATION, Double.valueOf(rotation));
      if (stgPos != null)
         res.put(STAGE_POINT, stgPos);
      if (detIdx != null)
         res.put(DETECTOR_INDEX, detIdx);
      return res;
   }

   protected ScaledImage(final ColorModel cm, final WritableRaster raster, final boolean isAlphaPre, final Hashtable<?, ?> props) {
      super(cm, raster, isAlphaPre, props);
   }

   public ScaledImage(final BufferedImage bi, final double hFov, final double vFov, final StageCoordinate stgPt, final String detIdx) {
      this(bi.getColorModel(), bi.getRaster(), bi.isAlphaPremultiplied(), createProperties(hFov, vFov, 0.0, stgPt, detIdx));
   }

   public ScaledImage(final BufferedImage bi, final double hFov, final double vFov, final double rotation, final StageCoordinate stgPt,
         final String detIdx) {
      this(bi.getColorModel(), bi.getRaster(), bi.isAlphaPremultiplied(), createProperties(hFov, vFov, rotation, stgPt, detIdx));
   }

   @Override
   public String toString() {
      final NumberFormat nf = new DecimalFormat("#,##0.0");
      return "ScaledImage[fov=" + nf.format(getHorizontalFieldOfView()) + " µm, " + getStagePoint().toString() + "]";
   }

   @Override
   public ScaledImage clone() {
      final BufferedImage bi = new BufferedImage(getWidth(), getHeight(), getType());
      bi.getGraphics().drawImage(this, 0, 0, null);
      return new ScaledImage(bi, getHorizontalFieldOfView(), getVerticalFieldOfView(), getStagePoint(), getDetectorIndex());
   }

   public static BufferedImage readHeader(final File baseFile, final BufferedImage baseImage) throws IOException {
      final File dir = baseFile.getParentFile();
      final String imgFilename = baseFile.getName();
      final String imgName = imgFilename.substring(0, imgFilename.length() - "[?].png".length());
      assert dir.isDirectory();
      final File imgsTxt = new File(dir, "images.txt");
      if (imgsTxt.exists() && imgsTxt.isFile())
         try (final Reader rdr = new FileReader(imgsTxt)) {
            try (final BufferedReader br = new BufferedReader(rdr)) {
               br.readLine(); // Read the header line
               String[] lastImgItems = null;
               for (String imgLine = br.readLine(); imgLine != null; imgLine = br.readLine()) {
                  final String[] imgItems = imgLine.split("\t");
                  if (imgName.equalsIgnoreCase(imgItems[0].trim()))
                     lastImgItems = imgItems;
               }
               if (lastImgItems != null)
                  try {
                     final double fov = Double.parseDouble(lastImgItems[2].trim()) / 1000.0;
                     final double xdim = Integer.parseInt(lastImgItems[3].trim());
                     final double ydim = Integer.parseInt(lastImgItems[4].trim());
                     final StageCoordinate stgPt = new StageCoordinate();
                     try {
                        final double x = Double.parseDouble(lastImgItems[7].trim());
                        stgPt.set(Axis.X, x);
                        final double y = Double.parseDouble(lastImgItems[8].trim());
                        stgPt.set(Axis.Y, y);
                        final double z = Double.parseDouble(lastImgItems[9].trim());
                        stgPt.set(Axis.Z, z);
                        final double r = Double.parseDouble(lastImgItems[10].trim());
                        stgPt.set(Axis.R, r);
                        final double t = Double.parseDouble(lastImgItems[11].trim());
                        stgPt.set(Axis.T, t);
                        final double b = Double.parseDouble(lastImgItems[12].trim());
                        stgPt.set(Axis.B, b);
                     } catch (final Exception ex) {
                        // Just ignore it..
                     }
                     final String idx = imgFilename.substring(imgFilename.length() - "?].png".length(), imgFilename.length() - "].png".length());
                     return new ScaledImage(baseImage, fov, (fov * ydim) / xdim, stgPt, idx);
                  } catch (final Exception ex) {
                     // Ignore it...
                  }
            }
         }
      return baseImage;
   }

   public static BufferedImage readPNG(final File file) throws IOException {
      final Iterator<ImageReader> irs = ImageIO.getImageReadersByFormatName("png");
      final ImageReader ir = irs.next();
      try (final FileImageInputStream fiis = new FileImageInputStream(file)) {
         ir.setInput(fiis);
         final BufferedImage bi = ir.read(0);
         return readHeader(file, bi);
      }
   }

   /**
    * Applys a micron bar to the image based on the horizontal field of view
    */
   public void applyMicronBar() {
      final Object fovProp = getProperty(HOR_FIELD_OF_VIEW);
      if (fovProp instanceof Double) {
         final double fov = ((Double) fovProp).doubleValue() * 1.0e6; // microns
         final double exp = Math.ceil(Math.log10(fov / 2.0)) - 1;
         final double sc = fov / (2.0 * Math.pow(10.0, exp));
         final double unit = (int) sc;
         final int pix = (int) Math.round((getWidth() * unit) / (2.0 * sc));
         final int right = (19 * getWidth()) / 20;
         final int left = right - pix;
         final Graphics2D g = createGraphics();
         final int h1 = getHeight() / 20;
         final int h0 = h1 - Math.max(2, getHeight() / 170);
         final NumberFormat nf = (unit * Math.pow(10.0, exp)) >= 10 ? new HalfUpFormat("#,###,##0") : new HalfUpFormat("0.0#");
         final String text = nf.format(unit * Math.pow(10.0, exp)) + " \u00B5m";
         final Font font = g.getFont();
         final float fontSize = getWidth() / 24;
         g.setFont(font.deriveFont(fontSize));
         final Rectangle2D r = g.getFontMetrics().getStringBounds(text, g);
         final int border = getWidth() / 64;
         final int base = (int) Math.round(h1 + r.getHeight());
         g.setColor(new Color(0, 0, 0, 64));
         g.fillRect(left - border, h0 - border, (right - left) + (2 * border), (base - h0) + (2 * border));
         g.setColor(new Color(255, 255, 255, 192));
         g.fillRect(left, h0, right - left, h1 - h0);
         g.drawString(text, (int) Math.round(((right + left) - r.getWidth()) / 2.0), base);
      }

   }

   public void applyCrossHair(final int x, final int y) {
      final Graphics2D g = createGraphics();
      g.setColor(new Color(0, 255, 255, 192));
      final int sc = Math.min(getHeight(), getWidth()) / 10;
      g.drawRect(x - 3, y - 3, 6, 6);
      g.drawLine(x - sc, y, x - 4, y);
      g.drawLine(x + 4, y, x + sc, y);
      g.drawLine(x, y - sc, x, y - 4);
      g.drawLine(x, y + 4, x, y + sc);
   }

   /**
    * Draw a semi-transparent yellow box of the specified width in the center of
    * the image
    * 
    * @param width
    *           in meters
    */
   public void applyCenterBox(final double width) {
      final Object fovProp = getProperty(HOR_FIELD_OF_VIEW);
      if (fovProp instanceof Double) {
         final double hfov = ((Double) fovProp).doubleValue(); // meters
         final double vfov = hfov * getHeight() / getWidth();
         if (width < hfov) {
            int left = (int) Math.round((0.5 * (hfov - width) / hfov) * getWidth());
            int top = (int) Math.round((0.5 * (vfov - width) / vfov) * getHeight());
            int w = (int) Math.round((width / hfov) * getWidth());
            int h = (int) Math.round((width / vfov) * getHeight());
            final Graphics2D g = createGraphics();
            g.setColor(new Color(0, 255, 255, 192));
            g.drawRect(left, top, w, h);
         }
      }

   }

   public void applyCenterCrossHair() {
      applyCrossHair(getWidth() / 2, getHeight() / 2);
   }

   /**
    * Horizontal field-of-view
    *
    * @return in meters
    */
   public double getHorizontalFieldOfView() {
      final Object fovProp = getProperty(HOR_FIELD_OF_VIEW);
      return fovProp instanceof Number ? ((Number) fovProp).doubleValue() : Double.NaN;
   }

   /**
    * Image rotation
    *
    * @return in degrees
    */
   public double getImageRotation() {
      final Object rot = getProperty(IMAGE_ROTATION);
      return rot instanceof Number ? ((Number) rot).doubleValue() : Double.NaN;
   }

   public ScaledImage extract(final Rectangle rect) {
      final double hFov = (getHorizontalFieldOfView() * rect.width) / getWidth();
      final double vFov = (getVerticalFieldOfView() * rect.height) / getHeight();
      final StageCoordinate oldSc = getStagePoint();
      final StageCoordinate sc = new StageCoordinate(oldSc);
      sc.set(Axis.X, oldSc.get(Axis.X) + (hFov * (0.5 - ((0.5 * (rect.x + rect.width)) / getWidth()))));
      sc.set(Axis.Y, oldSc.get(Axis.Y) + (vFov * (0.5 - ((0.5 * (rect.y + rect.height)) / getHeight()))));
      return new ScaledImage(getSubimage(rect.x, rect.y, rect.width, rect.height), hFov, vFov, sc, getDetectorIndex());
   }

   /**
    * Vertical field-of-view
    *
    * @return in meters
    */
   public double getVerticalFieldOfView() {
      final Object fovProp = getProperty(VERT_FIELD_OF_VIEW);
      return fovProp instanceof Number ? ((Number) fovProp).doubleValue() : Double.NaN;
   }

   public StageCoordinate getStagePoint() {
      final Object spProp = getProperty(STAGE_POINT);
      return spProp instanceof StageCoordinate ? (StageCoordinate) spProp : null;
   }

   public String getDetectorIndex() {
      final Object diProp = getProperty(DETECTOR_INDEX);
      return diProp instanceof String ? ((String) diProp) : null;
   }

   public BufferedImage applyThreshold(final int min, final int max, final Color color) {
      final BufferedImage res = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
      final Graphics2D g2 = res.createGraphics();
      g2.drawImage(this, 0, 0, getWidth(), getHeight(), null);
      final DataBuffer db = getRaster().getDataBuffer();
      g2.setColor(Color.yellow);
      if ((db instanceof DataBufferByte) || (db instanceof DataBufferInt)) {
         final int rgb = color.getRGB();
         assert db.getSize() == (getWidth() * getHeight());
         for (int i = db.getSize() - 1; i >= 0; --i) {
            final int elem = db.getElem(0, i);
            if ((elem > min) && (elem <= max))
               res.setRGB(i % getWidth(), i / getWidth(), rgb);
         }
      }
      return res;
   }

   /**
    * Draw a buffered image onto a graphics context. If the BufferedImage is an
    * instance of ScaledImage then draw a micron scale bar over the image.
    *
    * @param gr
    * @param x
    * @param y
    * @param width
    * @param height
    */
   static public void draw(final BufferedImage img, final Graphics gr, final int x, final int y, final int width, final int height) {
      gr.drawImage(img, x, y, width, height, null);
      if (img instanceof ScaledImage) {
         final Object fovProp = img.getProperty(HOR_FIELD_OF_VIEW);
         if (fovProp instanceof Double) {
            final double fov = ((Double) fovProp).doubleValue() * 1.0e6; // microns
            final double exp = Math.ceil(Math.log10(fov / 2.0)) - 1;
            final double sc = fov / (2.0 * Math.pow(10.0, exp));
            final double unit = (int) sc;
            final int pix = (int) Math.round((width * unit) / (2.0 * sc));
            final int right = x + ((19 * width) / 20);
            final int left = right - pix;
            gr.setColor(Color.yellow);
            final int h = y + (height / 20);
            gr.drawLine(left, h, right, h);
            final NumberFormat nf = new HalfUpFormat("#,###,##0");
            final String text = nf.format(unit * Math.pow(10.0, exp)) + " \u00B5m";
            final Rectangle2D r = gr.getFontMetrics().getStringBounds(text, gr);
            gr.drawString(text, (int) Math.round(((right + left) - r.getWidth()) / 2.0), (int) Math.round(h + r.getHeight()));
         }
      }
   }

   /**
    * Contrast enhancement by applying a linear scale which make 'pct' of these
    * 255. Only works on images with {@link ColorModel} derived from
    * {@link ComponentColorModel}.
    *
    * @param pct
    * @return ScaledImage returns 'this'
    */
   public boolean enhanceContrast(final double pct) {
      final Histogram hist = new Histogram(0.0, 255.0, 256);
      if ((getColorModel() instanceof ComponentColorModel) && (getColorModel().getNumComponents() == 1)) {
         int max = 255;
         {
            final int[] raster = getRaster().getPixels(0, 0, getWidth(), getHeight(), (int[]) null);
            for (final int element : raster)
               hist.add(element);
            int sum = hist.overrange();
            for (int i = 255; i >= 0; i--) {
               sum += hist.counts(i);
               if (sum >= (hist.totalCounts() * pct)) {
                  max = i;
                  break;
               }
            }
            for (int i = 0; i < raster.length; ++i)
               raster[i] = Math.min(255, (255 * raster[i]) / max);
            getRaster().setPixels(0, 0, getWidth(), getHeight(), raster);
         }
         return true;
      }
      return false;
   }

   /**
    * Center-weighed contrast enhancement by looking to the central one-quarter
    * pixels and applying a linear scale which make 'pct' of these 255. Only
    * works on images with {@link ColorModel} derived from
    * {@link ComponentColorModel}.
    *
    * @param pct
    * @return ScaledImage returns 'this'
    */
   public boolean enhanceContrastCW(final double pct) {
      final Histogram hist = new Histogram(0.0, 255.0, 256);
      if ((getColorModel() instanceof ComponentColorModel) && (getColorModel().getNumComponents() == 1)) {
         int max = 255;
         {
            final int[] raster = getRaster().getPixels(getWidth() / 4, getHeight() / 4, getWidth() / 2, getHeight() / 2, (int[]) null);
            for (final int element : raster)
               hist.add(element);
            int sum = hist.overrange();
            for (int i = 255; i >= 0; i--) {
               sum += hist.counts(i);
               if (sum >= (hist.totalCounts() * pct)) {
                  max = i;
                  break;
               }
            }
         }
         {
            final int[] raster2 = getRaster().getPixels(0, 0, getWidth(), getHeight(), (int[]) null);
            for (int i = 0; i < raster2.length; ++i)
               raster2[i] = Math.min(255, (255 * raster2[i]) / max);
            getRaster().setPixels(0, 0, getWidth(), getHeight(), raster2);
         }
         return true;
      }
      return false;
   }

   /**
    * Return the data in this image as a rectangular array of integer values.
    *
    * @return int[rows][cols]
    */
   public int[][] getRectRaster() {
      final int[] tmp = new int[getHeight() * getWidth()];
      getRaster().getPixels(0, 0, getWidth(), getHeight(), tmp);
      final int[][] res = new int[getHeight()][];
      for (int row = 0; row < getHeight(); ++row)
         res[0] = Arrays.copyOfRange(tmp, row * getWidth(), (row + 1) * getWidth());
      return res;
   }

   /**
    * Return the thresholded data in this image as a rectangular array of
    * boolean values.
    *
    * @return boolean[rows][cols] true if the pixel between minThresh
    *         (inclusive) and maxThresh (exclusive)
    */
   public boolean[][] threshold(final int minThresh, final int maxThresh) {
      final int w = getWidth();
      final int[] tmp = new int[getHeight() * w];
      getRaster().getPixels(0, 0, w, getHeight(), tmp);
      final boolean[][] res = new boolean[getHeight()][w];
      for (int row = 0; row < res.length; ++row)
         for (int col = 0; col < w; ++col) {
            final int v = tmp[(row * w) + col];
            res[row][col] = ((v >= minThresh) && (v < maxThresh));
         }
      return res;
   }
}
