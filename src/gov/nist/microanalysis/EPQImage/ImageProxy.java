package gov.nist.microanalysis.EPQImage;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.Raster;

import gov.nist.microanalysis.Utility.DescriptiveStatistics;

/**
 * <p>
 * A class that takes in an 8-bit BufferedImage object and translate it into an
 * alternative form that facilitates basic image processing.
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
public class ImageProxy {

   /**
    * <p>
    * Simple interface to test a single pixel against a threshold.
    * </p>
    *
    * @author nritchie
    * @version 1.0
    */
   public interface Threshold {

      public boolean meets(int i);

   }

   final private int[][] mBuffer; // [HEIGHT][WIDTH]

   final public static int toGray(int argb) {
      return ((argb & 0xFF) + ((argb & 0xFF00) >> 8) + ((argb & 0xFF0000) >> 16)) / 3;
   }

   final public static int toRGB(int i) {
      return (i << 16) + (i << 8) + i;
   }

   public ImageProxy(BufferedImage ib) {
      final int w = ib.getWidth();
      final int h = ib.getHeight();
      mBuffer = new int[h][w];
      final ColorModel colorModel = ib.getColorModel();
      if((colorModel instanceof ComponentColorModel) && (colorModel.getNumComponents() == 1)) {
         Raster r = ib.getData();
         for(int y = 0; y < h; ++y)
            r.getPixels(0, y, w, 1, mBuffer[y]);
      } else {
         for(int y = 0; y < h; ++y)
            for(int x = 0; x < w; ++x)
               set(x, y, toGray(ib.getRGB(x, y)));
      }
   }

   public ImageProxy(int w, int h) {
      mBuffer = new int[h][w];
   }

   public ImageProxy(ImageProxy ip) {
      mBuffer = ip.mBuffer.clone();
   }

   /**
    * <p>
    * An interface to facilitate remaping one color to another.
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    *
    * @author nritchie
    * @version 1.0
    */
   public interface ColorRemap {
      public int value(int i);
   }

   public void remap(ColorRemap cm) {
      final int w = getWidth(), h = getHeight();
      for(int x = 0; x < w; ++x)
         for(int y = 0; y < h; ++y)
            set(x, y, cm.value(get(x, y)));
   }

   public void invert() {
      remap(new ColorRemap() {
         @Override
         public int value(int i) {
            return 255 - i;
         }
      });
   }

   public void stretch(final int min, final int max) {
      remap((i) -> (255 * (i - min)) / (max - min));
   }

   public void threshold(final int min, final int max, final int iOut) {
      remap((i) -> (i >= min) && (i < max) ? iOut : 0);
   }

   public Rectangle validate(Rectangle rect) {
      final int x = Math.max(0, rect.x);
      final int y = Math.max(0, rect.y);
      final int xm = Math.min(getWidth(), rect.x + rect.width);
      final int ym = Math.min(getHeight(), rect.y + rect.height);
      return (x != rect.x) || (y != rect.y) || (xm - x != rect.width) || (ym - y != rect.height)
            ? new Rectangle(x, y, xm - x, ym - y)
            : rect;

   }

   /**
    * Get all the data matching the specified value in the specified rectangle
    * and put it in an ImageProxy object.
    *
    * @param rect
    * @param val
    * @return ImageProxy
    */
   public ImageProxy get(Rectangle rect, int val, int outVal) {
      assert rect.x >= 0;
      assert rect.x + rect.width < getWidth();
      assert rect.y >= 0;
      assert rect.y + rect.height < getHeight();
      rect = validate(rect);
      final ImageProxy res = new ImageProxy(rect.width, rect.height);
      for(int x = rect.x; x < rect.x + rect.width; ++x)
         for(int y = rect.y; y < rect.y + rect.height; ++y)
            if(get(x, y) == val)
               res.set(x - rect.x, y - rect.y, outVal);
      return res;
   }

   /**
    * Get all the data in the specified rectangle and put it in an ImageProxy
    * object.
    *
    * @param rect
    * @return ImageProxy
    */
   public ImageProxy get(Rectangle rect) {
      assert rect.x >= 0;
      assert rect.x + rect.width < getWidth();
      assert rect.y >= 0;
      assert rect.y + rect.height < getHeight();
      rect = validate(rect);
      final ImageProxy res = new ImageProxy(rect.width, rect.height);
      for(int x = rect.x; x < rect.x + rect.width; ++x)
         for(int y = rect.y; y < rect.y + rect.height; ++y)
            res.set(x - rect.x, y - rect.y, get(x, y));
      return res;
   }

   public ImageProxy invertMask() {
      ImageProxy res = new ImageProxy(getWidth(), getHeight());
      for(int x = 0; x < getWidth(); ++x)
         for(int y = 0; y < getHeight(); ++y)
            res.set(x, y, get(x, y) != 0 ? 0 : 1);
      return res;

   }

   public ImageProxy mask(Rectangle rect, ImageProxy mask) {
      assert rect.width == mask.getWidth();
      assert rect.height == mask.getHeight();
      ImageProxy res = new ImageProxy(mask.getWidth(), mask.getHeight());
      for(int x = 0; x < rect.width; ++x)
         for(int y = 0; y < rect.height; ++y)
            if(mask.get(x, y) != 0)
               res.set(x, y, get(x + rect.x, y + rect.y));
      return res;
   }

   public DescriptiveStatistics getImageStatistics(Rectangle rect, ImageProxy mask) {
      assert rect.width == mask.getWidth();
      assert rect.height == mask.getHeight();
      DescriptiveStatistics res = new DescriptiveStatistics();
      for(int x = 0; x < rect.width; ++x)
         for(int y = 0; y < rect.height; ++y)
            if(mask.get(x, y) != 0)
               res.add(get(x + rect.x, y + rect.y));
      return res;
   }

   public int getWidth() {
      return mBuffer[0].length;
   }

   public int getHeight() {
      return mBuffer.length;
   }

   public int get(int x, int y) {
      return mBuffer[y][x];
   }

   public void set(int x, int y, int i) {
      mBuffer[y][x] = i;
   }

   public BufferedImage asGrayImage() {
      final int width = getWidth();
      final int height = getHeight();
      BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
      for(int x = 0; x < width; ++x)
         for(int y = 0; y < height; ++y)
            bi.setRGB(x, y, toRGB(get(x, y) % 0x100));
      return bi;
   }

   /**
    * Count the pixels meeting the threshold
    *
    * @param thresh
    * @return int
    */
   public int count(Threshold thresh) {
      final int width = getWidth();
      final int height = getHeight();
      int cx = 0;
      for(int x = 0; x < width; ++x)
         for(int y = 0; y < height; ++y)
            if(thresh.meets(mBuffer[y][x]))
               ++cx;
      return cx;
   }

   public int getPerimeter(Threshold thresh) {
      final int width = getWidth();
      final int height = getHeight();
      int cx = 0;
      for(int x = 0; x < width; ++x)
         for(int y = 0; y < height; ++y) {
            if(thresh.meets(mBuffer[y][x])) {
               int neighbors = 0;
               if((x > 0) && (thresh.meets(mBuffer[y][x - 1])))
                  ++neighbors;
               if((x < width - 1) && (thresh.meets(mBuffer[y][x + 1])))
                  ++neighbors;
               if((y > 0) && (thresh.meets(mBuffer[y - 1][x])))
                  ++neighbors;
               if((y < height - 1) && (thresh.meets(mBuffer[y + 1][x])))
                  ++neighbors;
               if(neighbors != 4)
                  cx++;
            }
         }
      return cx;
   }

   /**
    * Computes the mean intensity for all pixels with intensities larger than 0.
    *
    * @return double
    */
   public double getMeanIntensity() {
      double sum = 0.0;
      int cx = 0;
      for(int x = getWidth() - 1; x >= 0; --x)
         for(int y = getHeight() - 1; y >= 0; --y) {
            if(mBuffer[y][x] > 0) {
               sum += mBuffer[y][x];
               ++cx;
            }
         }
      return sum / cx;
   }

   /**
    * Computes the intensity statistics for all pixels with intensities larger
    * than 0.
    *
    * @return {@link DescriptiveStatistics}
    */
   public DescriptiveStatistics getImageStatistics() {
      DescriptiveStatistics res = new DescriptiveStatistics();
      for(int x = getWidth() - 1; x >= 0; --x)
         for(int y = getHeight() - 1; y >= 0; --y)
            if(mBuffer[y][x] > 0)
               res.add(mBuffer[y][x]);
      return res;
   }

   public double getEquivalentCircularDiameter(Threshold thresh) {
      return 2.0 * Math.sqrt(count(thresh) / Math.PI);
   }

   public Point getCenterOfMass(Threshold thresh) {
      long nx = 0, ny = 0, den = 0;
      for(int x = getWidth() - 1; x >= 0; --x)
         for(int y = getHeight() - 1; y >= 0; --y)
            if(thresh.meets(mBuffer[y][x])) {
               nx += x;
               ny += y;
               den += 1;
            }
      return new Point((int) (nx / den), (int) (ny / den));
   }
}
