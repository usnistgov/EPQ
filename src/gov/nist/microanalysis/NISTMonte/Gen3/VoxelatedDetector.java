package gov.nist.microanalysis.NISTMonte.Gen3;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQLibrary.XRayTransitionSet;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.Gen3.BaseXRayGeneration3.BremsstrahlungXRay;
import gov.nist.microanalysis.NISTMonte.Gen3.BaseXRayGeneration3.CharacteristicXRay;
import gov.nist.microanalysis.NISTMonte.Gen3.BaseXRayGeneration3.XRay;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;

final public class VoxelatedDetector implements ActionListener {

   public interface XRayType {
      public boolean accept(XRay xr);
   }

   public static class CharacteristicXRayType implements XRayType {

      private final XRayTransition mXRay;

      public CharacteristicXRayType(XRayTransition xr) {
         mXRay = xr;
      }

      @Override
      public boolean accept(XRay xr) {
         if (xr instanceof CharacteristicXRay) {
            CharacteristicXRay cxr = (CharacteristicXRay) xr;
            return cxr.getTransition().equals(mXRay);
         }
         return false;
      }

      @Override
      public String toString() {
         return mXRay.toString();
      }

      /**
       * @return
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         return mXRay.hashCode();
      }

      /**
       * @param obj
       * @return
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (!(obj instanceof CharacteristicXRayType))
            return false;
         CharacteristicXRayType other = (CharacteristicXRayType) obj;
         return mXRay.equals(other.mXRay);
      }
   }

   public static class ContinuumXRayType implements XRayType {

      private final double mMinE;
      private final double mMaxE;

      public ContinuumXRayType(double minE, double maxE) {
         mMinE = Math.min(minE, maxE);
         mMaxE = Math.max(minE, maxE);
      }

      @Override
      public boolean accept(XRay xr) {
         if (xr instanceof BremsstrahlungXRay) {
            final double e = xr.getEnergy();
            return (e >= mMinE) && (e <= mMaxE);
         }
         return false;
      }

      @Override
      public String toString() {
         final DecimalFormat df = new HalfUpFormat("0.0");
         return "[" + df.format(FromSI.keV(mMinE)) + "," + df.format(FromSI.keV(mMinE)) + "]";
      }

      /**
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         long temp;
         temp = Double.doubleToLongBits(mMaxE);
         result = prime * result + (int) (temp ^ (temp >>> 32));
         temp = Double.doubleToLongBits(mMinE);
         result = prime * result + (int) (temp ^ (temp >>> 32));
         return result;
      }

      /**
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (!(obj instanceof ContinuumXRayType))
            return false;
         ContinuumXRayType other = (ContinuumXRayType) obj;
         if (Double.doubleToLongBits(mMaxE) != Double.doubleToLongBits(other.mMaxE))
            return false;
         if (Double.doubleToLongBits(mMinE) != Double.doubleToLongBits(other.mMinE))
            return false;
         return true;
      }
   }

   public static class AtomicShellType implements XRayType {

      private final AtomicShell mShell;

      public AtomicShellType(AtomicShell shell) {
         mShell = shell;
      }

      @Override
      public boolean accept(XRay xr) {
         if (xr instanceof CharacteristicXRay) {
            CharacteristicXRay cxr = (CharacteristicXRay) xr;
            return cxr.getTransition().getDestination().equals(mShell);
         }
         return false;
      }

      public double getEdgeEnergy() {
         return mShell.getEdgeEnergy();
      }

      @Override
      public String toString() {
         return mShell.toString();
      }

      /**
       * @return
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         return mShell.hashCode();
      }

      /**
       * @param obj
       * @return
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (!(obj instanceof AtomicShellType))
            return false;
         AtomicShellType other = (AtomicShellType) obj;
         return mShell.equals(other.mShell);
      }
   }

   private final static int DIMS = 3;
   private final double[] mPosition;
   private final double[] mDelta;
   private final Map<XRayType, double[][][]> mAccumulator = new HashMap<XRayType, double[][][]>();
   private final int[] mDims;
   private int mEventCount;
   private final boolean mGenerated;

   /**
    * Gets the current value assigned to eventCount
    * 
    * @return Returns the eventCount.
    */
   public int getEventCount() {
      return mEventCount;
   }

   /**
    * Gets the current value assigned to electronCount
    * 
    * @return Returns the electronCount.
    */
   public int getElectronCount() {
      return mElectronCount;
   }

   private int mElectronCount;

   public VoxelatedDetector(double[] center, double[] size, int[] dims, boolean generated) {
      mDims = new int[DIMS];
      mDelta = new double[DIMS];
      mPosition = new double[DIMS];
      for (int i = 0; i < DIMS; ++i) {
         mDims[i] = 2 * ((dims[i] + 1) / 2);
         mDelta[i] = size[i] / mDims[i];
         mPosition[i] = center[i] - (i != 2 ? 0.5 * mDelta[i] * mDims[i] : 0);
      }
      mGenerated = generated;
      mEventCount = 0;
      mElectronCount = 0;
   }

   public void add(XRayTransition xrt) {
      XRayType obj = new CharacteristicXRayType(xrt);
      if (!mAccumulator.containsKey(obj))
         mAccumulator.put(obj, new double[mDims[0]][mDims[1]][mDims[2]]);
   }

   public void add(AtomicShell shell) {
      XRayType obj = new AtomicShellType(shell);
      if (!mAccumulator.containsKey(obj))
         mAccumulator.put(obj, new double[mDims[0]][mDims[1]][mDims[2]]);
   }

   public void add(double minE, double maxE) {
      XRayType obj = new ContinuumXRayType(minE, maxE);
      if (!mAccumulator.containsKey(obj))
         mAccumulator.put(obj, new double[mDims[0]][mDims[1]][mDims[2]]);
   }

   public void addAll(Collection<XRayTransitionSet> xrss) {
      for (XRayTransitionSet xrts : xrss)
         for (XRayTransition xr : xrts.getTransitions())
            add(xr);
   }

   public void addShells(Collection<XRayTransitionSet> xrss) {
      for (XRayTransitionSet xrts : xrss)
         for (XRayTransition xr : xrts.getTransitions())
            add(xr.getDestination());
   }

   /**
    * Coordinate of the center of the voxel at index x, y, z.
    * 
    * @param x
    * @param y
    * @param z
    * @return double[] In meters
    */
   public double[] coordinate(int x, int y, int z) {
      return new double[]{mPosition[0] + (x * mDelta[0]), mPosition[1] + (y * mDelta[1]), mPosition[2] + (z * mDelta[2])};
   }

   protected void increment(double[] pos, double inc, double[][][] acc) {
      final int xi = (int) Math.round((pos[0] - mPosition[0]) / mDelta[0]);
      final int yi = (int) Math.round((pos[1] - mPosition[1]) / mDelta[1]);
      final int zi = (int) Math.round((pos[2] - mPosition[2]) / mDelta[2]);
      if ((xi >= 0) && (xi < mDims[0]) && (yi >= 0) && (yi < mDims[1]) && (zi >= 0) && (zi < mDims[2]))
         acc[xi][yi][zi] += inc;
   }

   /**
    * @param ae
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   @Override
   public void actionPerformed(ActionEvent ae) {
      final Object src = ae.getSource();
      switch (ae.getID()) {
         case BaseXRayGeneration3.XRayGeneration : {
            assert src instanceof XRayTransport3;
            XRayTransport3 tran = (XRayTransport3) src;
            for (int i = tran.getEventCount() - 1; i >= 0; --i) {
               final XRay tr = tran.getXRay(i);
               for (Map.Entry<XRayType, double[][][]> me : mAccumulator.entrySet())
                  if (me.getKey().accept(tr))
                     increment(tr.getGenerationPos(), mGenerated ? tr.getGenerated() : tr.getIntensity(), me.getValue());
            }
            ++mEventCount;
         }
            break;
         case MonteCarloSS.TrajectoryStartEvent :
            ++mElectronCount;
            break;
      }
   }

   /**
    * Returns a list of voxel indices sorted by generated intensity. Smallest
    * intensities first.
    * 
    * @return List&lt;int[]&gt;
    */
   private List<int[]> getSortedVoxels(XRayType dest) {
      final double[][][] acc = mAccumulator.get(dest);
      if (acc != null) {
         final Comparator<int[]> c = new Comparator<int[]>() {
            @Override
            public int compare(int[] arg0, int[] arg1) {
               return Double.compare(acc[arg0[0]][arg0[1]][arg0[2]], acc[arg1[0]][arg1[1]][arg1[2]]);
            }
         };
         final SortedSet<int[]> res = new TreeSet<int[]>(c);
         for (int xi = 0; xi < mDims[0]; ++xi)
            for (int yi = 0; yi < mDims[1]; ++yi)
               for (int zi = 0; zi < mDims[2]; ++zi)
                  if (acc[xi][yi][zi] > 0.0)
                     res.add(new int[]{xi, yi, zi});
         return Collections.unmodifiableList(new ArrayList<int[]>(res));
      } else
         return Collections.emptyList();
   }

   public double sum(XRayType dest) {
      final double[][][] acc = mAccumulator.get(dest);
      double res = 0.0;
      for (int xi = 0; xi < mDims[0]; ++xi)
         for (int yi = 0; yi < mDims[1]; ++yi)
            for (int zi = 0; zi < mDims[2]; ++zi)
               res += acc[xi][yi][zi];
      return res;
   }

   public Set<XRayType> getAccumulatorObjects() {
      return new HashSet<XRayType>(mAccumulator.keySet());
   }

   /**
    * Creates an image projected onto the XZ plane that represents the sum of
    * all intensity in each Y column. The pixels are colored according to the
    * key on the bottom. 1.0 to 0.9 of max intensity is pinky-red, 0.9 to 0.8 is
    * pink, ..., to 0.1 to +0.0, red and 0.0 is black.
    * 
    * @param dim
    *           Image width in pixels
    * @param dest
    *           Associated with this object
    * @return BufferedImage
    */
   public BufferedImage createXZSum(int dim, XRayType dest) {
      final int dd = ((dim + mDims[0]) - 1) / mDims[0];
      final int width = dd * mDims[0];
      final int height = dd * mDims[2];
      final BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
      final double[][][] acc = mAccumulator.get(dest);
      if (acc != null) {
         final Color[] colors = new Color[10];
         for (int i = 0; i < colors.length; ++i)
            colors[i] = Color.getHSBColor((float) i / colors.length, 1.0f, 1.0f);
         final double[][] sum = new double[acc.length][acc[0][0].length];
         double maxSum = 0.0;
         for (int xi = 0; xi < mDims[0]; ++xi)
            for (int zi = 0; zi < mDims[2]; ++zi) {
               double tmp = 0.0;
               for (int yi = 0; yi < mDims[1]; ++yi)
                  tmp += acc[xi][yi][zi];
               sum[xi][zi] = tmp;
               maxSum = Math.max(tmp, maxSum);
            }
         maxSum *= 1.000001;
         final Graphics2D gr = res.createGraphics();
         for (int xi = 0; xi < mDims[0]; ++xi)
            for (int zi = 0; zi < mDims[2]; ++zi) {
               final Color color = sum[xi][zi] > 0.0 ? colors[(int) (colors.length * (sum[xi][zi] / maxSum))] : Color.black;
               gr.setColor(color);
               gr.fillRect(xi * dd, zi * dd, dd, dd);
            }
         for (int i = 0; i < colors.length; ++i) {
            gr.setColor(colors[i]);
            gr.fillRect(i * dd, height - dd, dd, dd);
         }
      }
      return res;
   }

   /**
    * Creates an image projected onto the XY plane that represents the sum of
    * all intensity in each Y column. The pixels are colored according to the
    * key on the bottom. 1.0 to 0.9 of max intensity is pinky-red, 0.9 to 0.8 is
    * pink, ..., to 0.1 to +0.0, red and 0.0 is black.
    * 
    * @param dim
    *           Image width in pixels
    * @param dest
    *           Associated with this object
    * @return BufferedImage
    */
   public BufferedImage createXYSum(int dim, XRayType dest) {
      final int dd = ((dim + mDims[0]) - 1) / mDims[0];
      final int width = dd * mDims[0];
      final int height = dd * mDims[1];
      final BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
      final double[][][] acc = mAccumulator.get(dest);
      if (acc != null) {
         final Color[] colors = new Color[10];
         for (int i = 0; i < colors.length; ++i)
            colors[i] = Color.getHSBColor((float) i / colors.length, 1.0f, 1.0f);
         final double[][] sum = new double[acc.length][acc[0][0].length];
         double maxSum = 0.0;
         for (int xi = 0; xi < mDims[0]; ++xi)
            for (int yi = 0; yi < mDims[1]; ++yi) {
               double tmp = 0.0;
               for (int zi = 0; zi < mDims[2]; ++zi)
                  tmp += acc[xi][yi][zi];
               sum[xi][yi] = tmp;
               maxSum = Math.max(tmp, maxSum);
            }
         maxSum *= 1.000001;
         final Graphics2D gr = res.createGraphics();
         for (int xi = 0; xi < mDims[0]; ++xi)
            for (int yi = 0; yi < mDims[1]; ++yi) {
               final Color color = sum[xi][yi] > 0.0 ? colors[(int) (colors.length * (sum[xi][yi] / maxSum))] : Color.black;
               gr.setColor(color);
               gr.fillRect(xi * dd, yi * dd, dd, dd);
            }
         for (int i = 0; i < colors.length; ++i) {
            gr.setColor(colors[i]);
            gr.fillRect(i * dd, height - dd, dd, dd);
         }
      }
      return res;
   }

   public boolean writeXYPlanar(int dim, XRayType dest, File outfile) {
      final int dd = ((dim + mDims[0]) - 1) / mDims[0];
      final int width = dd * mDims[0];
      final int height = dd * mDims[2];
      final double[][][] acc = mAccumulator.get(dest);
      if (acc != null) {
         final Color[] colors = new Color[100];
         for (int i = 0; i < colors.length; ++i)
            colors[i] = Color.getHSBColor(((float) i) / colors.length, 1.0f, 1.0f);
         double max = 0.0;
         for (int xi = 0; xi < mDims[0]; ++xi)
            for (int yi = 0; yi < mDims[1]; ++yi)
               for (int zi = 0; zi < mDims[2]; ++zi)
                  max = Math.max(1.000001 * acc[xi][yi][zi], max);
         final BufferedImage[] imgs = new BufferedImage[mDims[2]];
         for (int zi = 0; zi < mDims[2]; ++zi) {
            final BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            final Graphics2D gr = res.createGraphics();
            boolean nz = false;
            for (int xi = 0; xi < mDims[0]; ++xi)
               for (int yi = 0; yi < mDims[1]; ++yi) {
                  final double val = acc[xi][yi][zi];
                  nz |= (val > 0.0);
                  final Color color = val > 0.0 ? colors[(int) (colors.length * (val / max))] : Color.black;
                  gr.setColor(color);
                  gr.fillRect(xi * dd, yi * dd, dd, dd);
               }
            if (nz)
               imgs[zi] = res;
         }
         final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tif");
         if (writers.hasNext())
            try {
               // Don't write initial or final black images
               int begin = 0, end = 0;
               for (begin = 0; begin < imgs.length; ++begin)
                  if (imgs[begin] != null)
                     break;
               for (end = imgs.length - 1; end > begin; --end)
                  if (imgs[end] != null)
                     break;
               final ImageWriter writer = writers.next();
               final ImageOutputStream ios = ImageIO.createImageOutputStream(outfile);
               writer.setOutput(ios);
               try {
                  writer.write(null, new IIOImage(imgs[begin], null, null), null);
                  for (int yi = begin + 1; yi <= end; ++yi)
                     if (writer.canInsertImage(yi - begin))
                        writer.writeInsert(yi - begin, new IIOImage(imgs[yi], null, null), null);
               } finally {
                  ios.close();
               }
               return true;
            } catch (final IOException e) {
               return false;
            }
      }
      return false;
   }

   public boolean writeXZPlanar(int dim, XRayType dest, File outfile) {
      final int dd = ((dim + mDims[0]) - 1) / mDims[0];
      final int width = dd * mDims[0];
      final int height = dd * mDims[2];
      final double[][][] acc = mAccumulator.get(dest);
      if (acc != null) {
         final Color[] colors = new Color[100];
         for (int i = 0; i < colors.length; ++i)
            colors[i] = Color.getHSBColor(((float) i) / colors.length, 1.0f, 1.0f);
         double max = 0.0;
         for (int xi = 0; xi < mDims[0]; ++xi)
            for (int yi = 0; yi < mDims[1]; ++yi)
               for (int zi = 0; zi < mDims[2]; ++zi)
                  max = Math.max(1.000001 * acc[xi][yi][zi], max);
         final BufferedImage[] imgs = new BufferedImage[mDims[1]];
         for (int yi = 0; yi < mDims[1]; ++yi) {
            final BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            final Graphics2D gr = res.createGraphics();
            for (int xi = 0; xi < mDims[0]; ++xi)
               for (int zi = 0; zi < mDims[2]; ++zi) {
                  final double val = acc[xi][yi][zi];
                  final Color color = val > 0.0 ? colors[(int) (colors.length * (val / max))] : Color.black;
                  gr.setColor(color);
                  gr.fillRect(xi * dd, zi * dd, dd, dd);
               }
            imgs[yi] = res;
         }
         final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tif");
         if (writers.hasNext())
            try {
               final ImageWriter writer = writers.next();
               final ImageOutputStream ios = ImageIO.createImageOutputStream(outfile);
               writer.setOutput(ios);
               writer.write(null, new IIOImage(imgs[0], null, null), null);
               for (int yi = 1; yi < imgs.length; ++yi)
                  if (writer.canInsertImage(yi))
                     writer.writeInsert(yi, new IIOImage(imgs[yi], null, null), null);
               ios.close();
               return true;
            } catch (final IOException e) {
               return false;
            }
      }
      return false;
   }

   /**
    * Creates an image projected onto the XY plane that represents the maximum
    * intensity in each Y column. Th pixels are plotted either black or white
    * where white represents those pixels which represent those pixels in the
    * most intense fraction of the image pixels. If frac=0.8 then those pixels
    * which represent the region generating 1.0-frac=0.2 of the total intensity
    * are colored white.
    * 
    * @param dim
    *           Image width in pixels
    * @param dest
    *           Associated with this object
    * @return BufferedImage
    */
   public BufferedImage createXZFraction(int dim, XRayType dest, double frac) {
      final int dd = ((dim + mDims[0]) - 1) / mDims[0];
      final int width = dd * mDims[0];
      final int height = dd * mDims[2];
      final BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
      final double[][][] acc = mAccumulator.get(dest);
      if (acc != null) {
         final Color[] colors = new Color[2];
         for (int i = 0; i < colors.length; ++i)
            colors[i] = new Color((i * 255) / (colors.length - 1), (i * 255) / (colors.length - 1), (i * 255) / (colors.length - 1));
         final double[] threshs = new double[colors.length];
         {
            final List<int[]> voxs = getSortedVoxels(dest);
            assert acc[voxs.get(0)[0]][voxs.get(0)[1]][voxs.get(0)[2]] <= acc[voxs.get(1)[0]][voxs.get(1)[1]][voxs.get(1)[2]];
            assert acc[voxs.get(0)[0]][voxs.get(0)[1]][voxs.get(0)[2]] < acc[voxs.get(voxs.size() - 1)[0]][voxs.get(voxs.size() - 1)[1]][voxs
                  .get(voxs.size() - 1)[2]];
            final double total = sum(dest);
            double sum = 0.0;
            frac *= total;
            for (int j = 0, i = 0; (j < voxs.size()) && (i < (colors.length - 1)); ++j) {
               final int[] idx = voxs.get(j);
               assert (idx[0] >= 0) && (idx[0] < mDims[0]);
               assert (idx[1] >= 0) && (idx[1] < mDims[1]);
               assert (idx[2] >= 0) && (idx[2] < mDims[2]);
               final double v = acc[idx[0]][idx[1]][idx[2]];
               sum += v;
               if (sum > frac) {
                  threshs[i + 1] = v;
                  break;
               }
            }
         }
         final Graphics2D gr = res.createGraphics();
         for (int xi = 0; xi < mDims[0]; ++xi)
            for (int zi = 0; zi < mDims[2]; ++zi) {
               double max = 0.0;
               for (int yi = 0; yi < mDims[1]; ++yi)
                  if (acc[xi][yi][zi] > max)
                     max = acc[xi][yi][zi];
               Color color = colors[colors.length - 1];
               for (int i = colors.length - 1; i >= 0; --i)
                  if (max >= threshs[i]) {
                     color = colors[i];
                     break;
                  }
               gr.setColor(color);
               gr.fillRect(xi * dd, zi * dd, dd, dd);
            }
      }
      return res;
   }

   /**
    * Creates an image projected onto the XY plane that represents the maximum
    * intensity in each Y column. The pixels that generated the first 10% of
    * intensity are colored pinky-red, the second 10% (10% to 20%) are purple,
    * the third 10% (blue) etc out to the last 10% which is red. The pixels are
    * colored according to the key on the bottom. 1.0 to 0.9 of max intensity is
    * red, 0.9 to 0.8 is pink, ..., to 0.1 to +0.0, red and 0.0 is black.
    * 
    * @param dim
    *           Image width in pixels
    * @param dest
    *           Associated with this object
    * @return BufferedImage
    */
   public BufferedImage createXZView(int dim, XRayType dest) {
      final int dd = ((dim + mDims[0]) - 1) / mDims[0];
      final int width = dd * mDims[0];
      final int height = dd * mDims[2];
      final BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
      final double[][][] acc = mAccumulator.get(dest);
      if (acc != null) {
         final Color[] colors = new Color[10];
         for (int i = 0; i < colors.length; ++i)
            colors[i] = Color.getHSBColor((float) i / colors.length, 1.0f, 1.0f);
         final double[] threshs = new double[colors.length];
         {
            final List<int[]> voxs = getSortedVoxels(dest);
            assert acc[voxs.get(0)[0]][voxs.get(0)[1]][voxs.get(0)[2]] <= acc[voxs.get(1)[0]][voxs.get(1)[1]][voxs.get(1)[2]];
            assert acc[voxs.get(0)[0]][voxs.get(0)[1]][voxs.get(0)[2]] < acc[voxs.get(voxs.size() - 1)[0]][voxs.get(voxs.size() - 1)[1]][voxs
                  .get(voxs.size() - 1)[2]];
            final double total = sum(dest);
            double sum = 0.0;
            double thresh = total / colors.length;
            for (int j = 0, i = 0; (j < voxs.size()) && (i < (colors.length - 1)); ++j) {
               final int[] idx = voxs.get(j);
               assert (idx[0] >= 0) && (idx[0] < mDims[0]);
               assert (idx[1] >= 0) && (idx[1] < mDims[1]);
               assert (idx[2] >= 0) && (idx[2] < mDims[2]);
               final double v = acc[idx[0]][idx[1]][idx[2]];
               sum += v;
               if (sum > thresh) {
                  i++;
                  threshs[i] = v;
                  thresh = (total * (i + 1.0)) / colors.length;
               }
            }
         }
         final Graphics2D gr = res.createGraphics();
         for (int xi = 0; xi < mDims[0]; ++xi)
            for (int zi = 0; zi < mDims[2]; ++zi) {
               double max = 0.0;
               for (int yi = 0; yi < mDims[1]; ++yi)
                  if (acc[xi][yi][zi] > max)
                     max = acc[xi][yi][zi];
               Color color = max > 0.0 ? colors[colors.length - 1] : Color.black;
               if (max > 0.0)
                  for (int i = colors.length - 1; i >= 0; --i)
                     if (max > threshs[i]) {
                        color = colors[i];
                        break;
                     }
               gr.setColor(color);
               gr.fillRect(xi * dd, zi * dd, dd, dd);
            }
         for (int i = 0; i < colors.length; ++i) {
            gr.setColor(colors[i]);
            gr.fillRect(i * dd, height - dd, dd, dd);
         }
      }
      return res;
   }

   /**
    * Creates an image projected onto the XY plane that represents the maximum
    * intensity in each Y column. The pixels that generated the first 10% of
    * intensity are colored pinky-red, the second 10% (10% to 20%) are purple,
    * the third 10% (blue) etc out to the last 10% which is red. The pixels are
    * colored according to the key on the bottom. 1.0 to 0.9 of max intensity is
    * red, 0.9 to 0.8 is pink, ..., to 0.1 to +0.0, red and 0.0 is black.
    * 
    * @param dim
    *           Image width in pixels
    * @param dest
    *           Associated with this object
    * @return BufferedImage
    */
   public BufferedImage createXYView(int dim, XRayType dest) {
      final int dd = ((dim + mDims[0]) - 1) / mDims[0];
      final int width = dd * mDims[0];
      final int height = dd * mDims[2];
      final BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
      final double[][][] acc = mAccumulator.get(dest);
      if (acc != null) {
         final Color[] colors = new Color[10];
         for (int i = 0; i < colors.length; ++i)
            colors[i] = Color.getHSBColor((float) i / colors.length, 1.0f, 1.0f);
         final double[] threshs = new double[colors.length];
         {
            final List<int[]> voxs = getSortedVoxels(dest);
            assert acc[voxs.get(0)[0]][voxs.get(0)[1]][voxs.get(0)[2]] <= acc[voxs.get(1)[0]][voxs.get(1)[1]][voxs.get(1)[2]];
            assert acc[voxs.get(0)[0]][voxs.get(0)[1]][voxs.get(0)[2]] < acc[voxs.get(voxs.size() - 1)[0]][voxs.get(voxs.size() - 1)[1]][voxs
                  .get(voxs.size() - 1)[2]];
            final double total = sum(dest);
            double sum = 0.0;
            double thresh = total / colors.length;
            for (int j = 0, i = 0; (j < voxs.size()) && (i < (colors.length - 1)); ++j) {
               final int[] idx = voxs.get(j);
               assert (idx[0] >= 0) && (idx[0] < mDims[0]);
               assert (idx[1] >= 0) && (idx[1] < mDims[1]);
               assert (idx[2] >= 0) && (idx[2] < mDims[2]);
               final double v = acc[idx[0]][idx[1]][idx[2]];
               sum += v;
               if (sum > thresh) {
                  i++;
                  threshs[i] = v;
                  thresh = (total * (i + 1.0)) / colors.length;
               }
            }
         }
         final Graphics2D gr = res.createGraphics();
         for (int xi = 0; xi < mDims[0]; ++xi)
            for (int yi = 0; yi < mDims[1]; ++yi) {
               double max = 0.0;
               for (int zi = 0; zi < mDims[2]; ++zi)
                  if (acc[xi][yi][zi] > max)
                     max = acc[xi][yi][zi];
               Color color = max > 0.0 ? colors[colors.length - 1] : Color.black;
               if (max > 0.0)
                  for (int i = colors.length - 1; i >= 0; --i)
                     if (max > threshs[i]) {
                        color = colors[i];
                        break;
                     }
               gr.setColor(color);
               gr.fillRect(xi * dd, yi * dd, dd, dd);
            }
         for (int i = 0; i < colors.length; ++i) {
            gr.setColor(colors[i]);
            gr.fillRect(i * dd, height - dd, dd, dd);
         }
      }
      return res;
   }

   /**
    * Generates an array of double pairs. The first double in each pair is the
    * distance from 'center' and the second is the fraction of x-ray generated
    * at or less than this distance.
    * 
    * @param center
    * @param dest
    * @return double[..][2]
    */
   public double[][] createRadialPDF(double[] center, XRayType dest, double eps) {
      double maxDist = 0.0;
      {
         for (int xi = 0; xi < mDims[0]; ++xi)
            for (int yi = 0; yi < mDims[1]; ++yi) {
               final double[] pp = coordinate(xi, yi, 0);
               final double d = Math.sqrt(Math2.sqr(pp[0] - center[0]) + Math2.sqr(pp[1] - center[1]));
               maxDist = Math.max(d, maxDist);
            }
      }
      int maxIdx = (int) Math.round(Math.ceil(maxDist / eps)) + 1;
      final double zacc[][] = new double[maxIdx][2]; // outer distance, events
                                                     // inside
      assert zacc.length == maxIdx;
      assert zacc[1].length == 1;
      {
         for (int i = 0; i < maxIdx; ++i)
            zacc[i][0] = i * eps;
         final double[][][] acc = mAccumulator.get(dest);
         double total = 0.0;
         for (int xi = 0; xi < mDims[0]; ++xi)
            for (int yi = 0; yi < mDims[1]; ++yi) {
               final double[] pp = coordinate(xi, yi, 0);
               final double d = Math.sqrt(Math2.sqr(pp[0] - center[0]) + Math2.sqr(pp[1] - center[1]));;
               final int idx = (int) Math.round(Math.floor(d / eps));
               assert idx >= 0 && idx < maxIdx;
               assert d < zacc[idx][0];
               assert (idx == 0) || (d >= zacc[idx - 1][0]);
               final double zsum = Math2.sum(acc[xi][yi]); // Sum over all
                                                           // depths
               total += zsum;
               zacc[idx][1] += zsum;
            }
         for (int i = 0; i < maxIdx; ++i)
            zacc[i][1] /= total;
      }
      return zacc;
   }

   public double[][] createRadialCDF(double[] center, XRayType dest, double eps) {
      double[][] res = createRadialPDF(center, dest, eps);
      for (int i = 1; i < res.length; ++i)
         res[i][1] += res[i - 1][1];
      return res;
   }

   /**
    * Returns the fractional generation volume. The volume within which the
    * fraction f of the total x-rays associated with tran are generated. The
    * voxels are sorted in order of generate intensity and the smallest number
    * of voxels required to generate a fraction f is determined.
    * 
    * @param tran
    * @param f
    * @return double
    */
   public double getFractionalGenerationVolume(final XRayType tran, final double f) {
      assert f >= 0;
      assert f <= 1.0;
      final List<int[]> sorted = getSortedVoxels(tran);
      final double[][][] acc = mAccumulator.get(tran);
      final double total = sum(tran);
      double partial = 0.0;
      int count = 0;
      for (int i = sorted.size() - 1; i >= 0; --i) {
         final int[] ii = sorted.get(i);
         partial += acc[ii[0]][ii[1]][ii[2]];
         ++count;
         if (partial >= (f * total))
            return count * mDelta[0] * mDelta[1] * mDelta[2];
      }
      return Double.NaN;
   }

   /**
    * Returns the fractional generation depth. The depth above which a fraction
    * f of the total x-rays associated with tran are generated.
    * 
    * @param tran
    * @param f
    * @return double
    */
   public double[] getFractionalGenerationDepth(final XRayType tran, final double f) {
      assert f >= 0;
      assert f <= 1.0;
      final double[][][] acc = mAccumulator.get(tran);
      final double total = sum(tran);
      double partial = 0.0;
      for (int zi = 0; zi < mDims[2]; ++zi)
         for (int xi = 0; xi < mDims[0]; ++xi)
            for (int yi = 0; yi < mDims[1]; ++yi) {
               partial += acc[xi][yi][zi];
               if (partial > (f * total))
                  return new double[]{zi * mDelta[2], (zi + 1) * mDelta[2]};
            }
      return new double[]{mDims[2] * mDelta[2], Double.POSITIVE_INFINITY};
   }

   /**
    * Returns the fractional generation radius. The radius from the beam axis
    * within which a fraction f of the total x-rays associated with tran are
    * generated.
    * 
    * @param tran
    * @param f
    * @return double
    */
   public double[] getFractionalGenerationRadius(double[] origin, final XRayType tran, final double f) {
      final double d = Math.sqrt(Math2.sqr(mDelta[0]) + Math2.sqr(mDelta[1]));
      double[][] cdf = createRadialCDF(origin, tran, d);
      for (int i = 1; i < cdf.length; ++i)
         if (cdf[i][1] > f)
            return new double[]{cdf[i - 1][0], cdf[i][0]};
      return new double[]{cdf[cdf.length][0], Double.POSITIVE_INFINITY};
   }
}
