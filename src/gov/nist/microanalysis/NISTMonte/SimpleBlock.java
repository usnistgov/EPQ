package gov.nist.microanalysis.NISTMonte;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;

/**
 * <p>
 * Implements the MonteCarloSS.Shape interface for a simple block shaped region.
 * The edges of the block are aligned with the coordinate axes. The extent of
 * the block is defined by two corners.
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

public class SimpleBlock implements MonteCarloSS.Shape, TrajectoryVRML.IRender {
   private final double[] mCorner0;
   private final double[] mCorner1;

   private boolean between(double x, double b0, double b1) {
      assert (b0 <= b1);
      return (x >= b0) && (x <= b1);
   }

   /**
    * SimpleBlock - Constructs a SimpleBlock, simple shape definition class that
    * implements the Shape interface.
    * 
    * @param corner0
    *           double[] - The x,y &amp; z coordinates of one corner
    * @param corner1
    *           double[] - The coordinates of the diagonal corner
    */
   public SimpleBlock(double[] corner0, double[] corner1) {
      assert (corner0.length == 3);
      assert (corner1.length == 3);
      mCorner0 = corner0.clone();
      mCorner1 = corner1.clone();
      // Normalize coordinates so that mCorner0[i]<=mCorner1[i]
      for (int i = 0; i < 3; ++i)
         if (mCorner0[i] > mCorner1[i]) {
            final double tmp = mCorner0[i];
            mCorner0[i] = mCorner1[i];
            mCorner1[i] = tmp;
         }
   }

   @Override
   public boolean contains(double[] pos) {
      assert (pos.length == 3);
      return between(pos[0], mCorner0[0], mCorner1[0]) && between(pos[1], mCorner0[1], mCorner1[1]) && between(pos[2], mCorner0[2], mCorner1[2]);
   }

   @Override
   public double getFirstIntersection(double[] pos0, double[] pos1) {
      assert (pos0.length == 3);
      assert (pos1.length == 3);
      double t = Double.MAX_VALUE;
      for (int i = 2; i >= 0; --i) {
         final int j = (i + 1) % 3, k = (i + 2) % 3;
         if (pos1[i] != pos0[i]) {
            double u = (mCorner0[i] - pos0[i]) / (pos1[i] - pos0[i]);
            if ((u >= 0.0) && (u <= t) && between(pos0[j] + (u * (pos1[j] - pos0[j])), mCorner0[j], mCorner1[j])
                  && between(pos0[k] + (u * (pos1[k] - pos0[k])), mCorner0[k], mCorner1[k]))
               t = u;
            // Bottom of block
            u = (mCorner1[i] - pos0[i]) / (pos1[i] - pos0[i]);
            if ((u >= 0.0) && (u <= t) && between(pos0[j] + (u * (pos1[j] - pos0[j])), mCorner0[j], mCorner1[j])
                  && between(pos0[k] + (u * (pos1[k] - pos0[k])), mCorner0[k], mCorner1[k]))
               t = u;
         }
      }
      return t >= 0 ? t : Double.MAX_VALUE;
   }

   /**
    * render - Renders a SimpleBlock as VRML
    * 
    * @param rc
    * @param wr
    * @throws IOException
    * @see gov.nist.microanalysis.NISTMonte.TrajectoryVRML.IRender#render(gov.nist.microanalysis.NISTMonte.TrajectoryVRML.RenderContext,
    *      java.io.Writer)
    */
   @Override
   public void render(TrajectoryVRML.RenderContext rc, Writer wr) throws IOException {
      final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
      nf.setMaximumFractionDigits(3);
      nf.setGroupingUsed(false);
      final Color color = rc.getCurrentColor();
      final String trStr = nf.format(rc.getTransparency());
      final String colorStr = nf.format(color.getRed() / 255.0) + " " + nf.format(color.getGreen() / 255.0) + " "
            + nf.format(color.getBlue() / 255.0);
      wr.append("\nTransform {\n");
      wr.append(" translation " + nf.format((mCorner0[0] + mCorner1[0]) / (2.0 * TrajectoryVRML.SCALE)) + " ");
      wr.append(nf.format((mCorner0[1] + mCorner1[1]) / (2.0 * TrajectoryVRML.SCALE)) + " ");
      wr.append(nf.format((mCorner0[2] + mCorner1[2]) / (2.0 * TrajectoryVRML.SCALE)) + "\n");
      wr.append(" children [\n");
      wr.append("  Shape {\n");
      wr.append("   geometry Box {\n");
      wr.append("    size " + nf.format(Math.abs(mCorner1[0] - mCorner0[0]) / TrajectoryVRML.SCALE) + " ");
      wr.append(nf.format(Math.abs(mCorner1[1] - mCorner0[1]) / TrajectoryVRML.SCALE) + " ");
      wr.append(nf.format(Math.abs(mCorner1[2] - mCorner0[2]) / TrajectoryVRML.SCALE) + "\n");
      wr.append("   }\n");
      wr.append("   appearance Appearance {\n");
      wr.append("    material Material {\n");
      wr.append("     emissiveColor " + colorStr + "\n");
      wr.append("     transparency " + trStr + "\n");
      wr.append("    }\n");
      wr.append("   }\n");
      wr.append("  }\n");
      wr.append(" ]\n");
      wr.append("}");
   }

   /**
    * Gets the current value assigned to one corner
    * 
    * @return Returns the coordinates of a corner.
    */
   public double[] getCorner0() {
      return mCorner0.clone();
   }

   /**
    * Gets the current value assigned to the other corner
    * 
    * @return Returns the coordinates of a corner.
    */
   public double[] getCorner1() {
      return mCorner1.clone();
   }

   @Override
   public String toString() {
      return "Block(" + Arrays.toString(mCorner0) + "," + Arrays.toString(mCorner1) + ")";
   }

}
