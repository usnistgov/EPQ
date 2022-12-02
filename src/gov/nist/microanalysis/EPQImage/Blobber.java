package gov.nist.microanalysis.EPQImage;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import gov.nist.microanalysis.Utility.DescriptiveStatistics;
import gov.nist.microanalysis.Utility.Pair;

/**
 * <p>
 * Indexes contiguous regions of pixels meeting a threshold.
 * </p>
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
public class Blobber {

   public static class Blob {
      final private Rectangle mBounds;
      final private ImageProxy mMask;

      private Blob(final Rectangle rect, final ImageProxy shape) {
         mBounds = rect;
         mMask = shape;
      }

      /**
       * Gets the current value assigned to bounds
       *
       * @return Returns the bounds.
       */
      public Rectangle getBounds() {
         return mBounds;
      }

      /**
       * Gets the current value assigned to shape
       *
       * @return Returns the shape.
       */
      public ImageProxy getShape() {
         return mMask;
      }

      public ImageProxy mask(final ImageProxy src) {
         return src.mask(mBounds, mMask);
      }

      public DescriptiveStatistics getBlobStatistics(final ImageProxy src) {
         return src.getImageStatistics(mBounds, mMask);
      }

      public DescriptiveStatistics getBackgroundStatistics(final ImageProxy src) {
         return src.getImageStatistics(mBounds, mMask.invertMask());
      }

      public BufferedImage getMask() {
         return mMask.asGrayImage();
      }

      public Point getCenterOfMass() {
         Point pt = mMask.getCenterOfMass((v) -> v > 0);
         return new Point(pt.x + mBounds.x, pt.y + mBounds.y);
      }

      public int getPerimeter() {
         return mMask.getPerimeter((v) -> v > 0);
      }

      public double getEquivalentCircularDiameter() {
         return mMask.getEquivalentCircularDiameter((v) -> v > 0);
      }

      public int getCount() {
         return mMask.count((v) -> v > 0);
      }

      @Override
      public String toString() {
         return "Blob[(" + mBounds.x + "," + mBounds.y + "),(" + //
               Integer.toString(mBounds.x + mBounds.width) + "," + Integer.toString(mBounds.y + mBounds.height) + ")]";

      }

   }

   public static void loadMe() {
   }

   private final ImageProxy mImage;
   private final ArrayList<Blob> mBlobs = new ArrayList<>();

   public Blobber(final ImageProxy img, final int min, final int max) {
      this(img, (v) -> (v >= min) && (v < max));
   }

   public Blobber(final ImageProxy img, final ImageProxy.Threshold thresh) {
      mImage = img;
      final ImageProxy ip2 = new ImageProxy(img.getWidth(), img.getHeight());
      // Implement a two-pass blobber
      final int imgW = mImage.getWidth(), imgH = mImage.getHeight();
      // List of joined blob indices
      final ArrayList<Set<Integer>> alias = new ArrayList<>();
      int prev = 0; // Index of blob index of last pixel
      int next = 1; // Value for the next blob index
      for (int y = 0; y < imgH; ++y)
         for (int x = 0; x < imgW; ++x)
            if (thresh.meets(mImage.get(x, y))) {
               // prev is what it was at the end of the previous pixel
               if (y > 0) {
                  // Check above...
                  if (prev == 0)
                     prev = ip2.get(x, y - 1);
                  else {
                     // Check if we should renumber...
                     final int above = ip2.get(x, y - 1);
                     if ((above != 0) && (above != prev)) {
                        assert prev != 0;
                        // These are the same regions with different indexes
                        int ia = -1, ip = -1;
                        final Integer iia = Integer.valueOf(above);
                        final Integer iip = Integer.valueOf(prev);
                        for (int i = 0; i < alias.size(); ++i) {
                           final Set<Integer> si = alias.get(i);
                           if (si.contains(iia)) {
                              assert ia == -1;
                              ia = i;
                           }
                           if (si.contains(iip)) {
                              assert ip == -1;
                              ip = i;
                           }
                        }
                        assert ia != -1;
                        assert ip != -1;
                        if (ia != ip) {
                           // Merge them
                           alias.get(ia).addAll(alias.get(ip));
                           alias.remove(ip);
                        }
                        prev = above;
                     }
                  }
               }
               if (prev == 0) {
                  // Get the next available blob index
                  prev = next;
                  ++next;
                  final TreeSet<Integer> tsi = new TreeSet<Integer>();
                  tsi.add(prev);
                  alias.add(tsi);
               }
               assert prev != 0;
               ip2.set(x, y, prev);
            } else
               prev = 0;
      // Second pass combine the adjacent indices
      final int[] idx = new int[next];
      for (int i = 0; i < alias.size(); ++i)
         for (final int id : alias.get(i))
            idx[id] = i + 1;
      final Rectangle[] rects = new Rectangle[alias.size() + 1];
      for (int y = 0; y < imgH; ++y)
         for (int x = 0; x < imgW; ++x) {
            final int vip2 = ip2.get(x, y);
            if (vip2 > 0) {
               final int id = idx[vip2];
               if (rects[id] != null) {
                  if (!rects[id].contains(x, y))
                     rects[id].add(x + 1, y + 1);
               } else
                  rects[id] = new Rectangle(x, y, 1, 1);
               ip2.set(x, y, id);
            }
         }
      for (int id = 0; id < rects.length; ++id) {
         final Rectangle rect = rects[id];
         if (rect != null)
            mBlobs.add(new Blob(rect, ip2.get(rect, id, 255)));
      }
   }

   public List<Blob> getBlobs() {
      return Collections.unmodifiableList(mBlobs);
   }

   public Blob getLargest() {
      Blob largest = mBlobs.get(0);
      int lCount = largest.getCount();
      for (Blob bl : mBlobs)
         if (bl.getCount() > lCount) {
            largest = bl;
            lCount = largest.getCount();
         }
      return largest;
   }

   public int getBlobCount() {
      return mBlobs.size();
   }

   public interface Scorer {

      public double score(Blob blob, ImageProxy ip);

   }

   public List<Blob> getRankedBlobs(Scorer scorer) {
      List<Pair<Double, Blob>> scores = new ArrayList<>();
      for (Blob bl : mBlobs)
         scores.add(Pair.create(scorer.score(bl, mImage), bl));
      scores.sort(new Comparator<Pair<Double, Blob>>() {

         @Override
         public int compare(Pair<Double, Blob> o1, Pair<Double, Blob> o2) {
            return Double.compare(o1.first.doubleValue(), o2.first.doubleValue());
         }
      });
      List<Blob> res = new ArrayList<>();
      for (Pair<Double, Blob> pr : scores)
         res.add(pr.second);
      return res;
   }

   public static void main(String[] args) {
      try {
         BufferedImage bi = ImageIO.read(Blobber.class.getResourceAsStream("blob_test1.png"));
         ImageProxy ip = new ImageProxy(bi);
         Blobber bl = new Blobber(ip, (v) -> v > 10);
         String userHome = System.getProperty("user.home");
         File path = new File(userHome, "Desktop\\Blob_Test1");
         path.mkdirs();
         int i = 1;
         for (Blob b : bl.getBlobs()) {
            ImageIO.write(b.getMask(), "png", new File(path, "blob" + i + ".png"));
            System.out.println(b.toString() + "\t" + b.getCount() + "\t" + b.getCenterOfMass() //
                  + "\t" + b.getPerimeter() + "\t" + (Math.PI * b.getEquivalentCircularDiameter()));
            ++i;
         }
      } catch (IOException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

   }

}
