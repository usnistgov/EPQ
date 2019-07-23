package gov.nist.microanalysis.NISTMonte;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * The EnergyLossListener is designed to listen for electron scattering events
 * and to tabulate the energy loss as a function of position in the sample. The
 * listener voxelates a region of space.
 * </p>
 * <p>
 * The results which represent the energy deposited per voxel per current dose
 * can be reported in slice-by-slice or the sum projected onto a plane.
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
final public class EnergyLossListener
   implements ActionListener {

   private final int[] mDims;
   private final double[][][] mVoxel;
   private final double[] mPoint;
   private final double[] mSize;

   private transient int mElectronCount = 0;
   private transient double mTotalEnergyLoss = 0.0;

   /**
    * Creates an EnergyLossListener. The point argument represent the center of
    * the top face of the volume so it is the center of the XY plane
    * representing the smallest Z point in the volume. This is typically the
    * same point at which the electron enter the material.
    * 
    * @param point A point which specifies the center of the top face of the
    *           volume.
    * @param size The width of the volume in the x, y and z dimensions
    * @param n The number of bins in each dimension
    */
   public EnergyLossListener(double[] point, double size, int n) {
      mDims = new int[] {
         n,
         n,
         n
      };
      mVoxel = new double[n][n][n];
      final double[] pt = point.clone();
      pt[0] -= (0.5 * size);
      pt[1] -= (0.5 * size);
      mSize = new double[] {
         size,
         size,
         size
      };
      mPoint = pt;
      reset();
   }

   public EnergyLossListener(double[] point, double[] size, int[] n) {
      mDims = n.clone();
      mVoxel = new double[n[0]][n[1]][n[2]];
      final double[] pt = point.clone();
      pt[0] -= (0.5 * size[0]);
      pt[1] -= (0.5 * size[1]);
      mSize = size.clone();
      mPoint = pt;
      reset();
   }

   /**
    * Reset the accumulators.
    */
   public void reset() {
      for(int i = 0; i < mDims[0]; ++i)
         for(int j = 0; j < mDims[1]; ++j)
            Arrays.fill(mVoxel[i][j], 0.0);
      mElectronCount = 0;
      mTotalEnergyLoss = 0.0;
   }

   /**
    * Returns the voxel index associated with the specified position.
    * 
    * @param pos A double[3]
    * @return An int[3]
    */
   public int[] indexOf(double[] pos) {
      final double[] ii = Math2.ebeDivide(Math2.minus(pos, mPoint), mSize);
      return new int[] {
         (int) ii[0],
         (int) ii[1],
         (int) ii[2]
      };
   }

   /**
    * Do these two voxel coordinates represent the same voxel?
    * 
    * @param ii0 An int[3]
    * @param ii1 An int[3]
    * @return true->same voxel, false otherwise.
    */
   private static boolean sameVoxel(int[] ii0, int[] ii1) {
      return Arrays.equals(ii0, ii1);
   }

   /**
    * Returns the number of bins along the specified axis
    * 
    * @param axis int in [0,3)
    * @return int
    */
   public int getDimension(int axis) {
      return mDims[axis];
   }

   /**
    * Coordinate of the low side of the bin-th bin for the axis-th axis.
    * 
    * @param axis int in [0,3) for x,y,z respectively.
    * @param bin int in [0, getDimension(axis))
    * @return The coordinate
    */
   public double lowSidePosition(int axis, int bin) {
      return mPoint[axis] + (mSize[axis] * bin);
   }

   /**
    * Coordinate of the high side of the bin-th bin for the axis-th axis.
    * 
    * @param axis int in [0,3) for x,y,z respectively.
    * @param bin int in [0, getDimension(axis))
    * @return The coordinate
    */
   public double highSidePosition(int axis, int bin) {
      return mPoint[axis] + (mSize[axis] * (bin + 1));

   }

   /**
    * Returns the energy deposited in eV/electron in this voxel
    * 
    * @param x Bin index
    * @param y Bin index
    * @param z Bin index
    * @return double eV per electron
    */
   public double get(int x, int y, int z) {
      return FromSI.eV(mVoxel[x][y][z]) / mElectronCount;
   }

   /**
    * Dumps the sliced defined by x=n. Energy deposition is reported as
    * eV/electron.
    * 
    * @param wr A Writer determining where to write the results
    * @param n The slice index (x-axis)
    * @throws IOException
    */
   public void dumpXSlice(Writer wr, int n)
         throws IOException {
      final double norm = FromSI.eV(1.0) / mElectronCount;
      for(int y = 0; y < mDims[1]; ++y) {
         wr.append(", ");
         wr.append(Double.toString(mPoint[1] + (mSize[1] * y)));
      }
      wr.append("\n");
      for(int z = 0; z < mDims[2]; ++z) {
         wr.append(Double.toString(mPoint[2] + (mSize[2] * z)));
         for(int y = 0; y < mDims[1]; ++y) {
            wr.append(", ");
            wr.append(Double.toString(mVoxel[n][y][z] * norm));
         }
         wr.append("\n");
      }
   }

   /**
    * Dumps the sliced defined by y=n. Energy deposition is reported as
    * eV/electron.
    * 
    * @param wr A Writer determining where to write the results
    * @param n The slice index (y-axis)
    * @throws IOException
    */
   public void dumpYSlice(Writer wr, int n)
         throws IOException {
      final double norm = FromSI.eV(1.0) / mElectronCount;
      for(int x = 0; x < mDims[0]; ++x) {
         wr.append(", ");
         wr.append(Double.toString(mPoint[0] + (mSize[0] * x)));
      }
      wr.append("\n");
      for(int z = 0; z < mDims[2]; ++z) {
         wr.append(Double.toString(mPoint[2] + (mSize[2] * z)));
         for(int x = 0; x < mDims[0]; ++x) {
            wr.append(", ");
            wr.append(Double.toString(mVoxel[x][n][z] * norm));
         }
         wr.append("\n");
      }
   }

   /**
    * Dumps the sliced defined by z=n. Energy deposition is reported as
    * eV/electron.
    * 
    * @param wr A Writer determining where to write the results
    * @param n The slice index (z-axis)
    * @throws IOException
    */
   public void dumpZSlice(Writer wr, int n)
         throws IOException {
      final double norm = FromSI.eV(1.0) / mElectronCount;
      for(int x = 0; x < mDims[0]; ++x) {
         wr.append(", ");
         wr.append(Double.toString(mPoint[0] + (mSize[0] * x)));
      }
      wr.append("\n");
      for(int y = 0; y < mDims[1]; ++y) {
         wr.append(Double.toString(mPoint[1] + (mSize[1] * y)));
         for(int x = 0; x < mDims[1]; ++x) {
            wr.append(", ");
            wr.append(Double.toString(mVoxel[x][y][n] * norm));
         }
         wr.append("\n");
      }
   }

   /**
    * Dumps the sum over the Z axis projected onto the XY plane of the energy
    * dumped into the volume. Energy deposition is reported as eV/electron.
    * 
    * @param wr A Writer defining the output device
    * @throws IOException
    */
   public void dumpXYProjection(Writer wr)
         throws IOException {
      final double norm = FromSI.eV(1.0) / mElectronCount;
      for(int x = 0; x < mDims[0]; ++x) {
         wr.append(", ");
         wr.append(Double.toString(mPoint[0] + (mSize[0] * x)));
      }
      wr.append("\n");
      for(int y = 0; y < mDims[1]; ++y) {
         wr.append(Double.toString(mPoint[1] + (mSize[1] * y)));
         for(int x = 0; x < mDims[0]; ++x) {
            double sum = 0.0;
            for(int z = 0; z < mDims[2]; ++z)
               sum += mVoxel[x][y][z];
            wr.append(", ");
            wr.append(Double.toString(sum * norm));
         }
         wr.append("\n");
      }
   }

   /**
    * Dumps the sum over the Y axis projected onto the XZ plane of the energy
    * dumped into the volume. Energy deposition is reported as eV/electron.
    * 
    * @param wr A Writer defining the output device
    * @throws IOException
    */
   public void dumpXZProjection(Writer wr)
         throws IOException {
      final double norm = FromSI.eV(1.0) / mElectronCount;
      for(int x = 0; x < mDims[0]; ++x) {
         wr.append(", ");
         wr.append(Double.toString(mPoint[0] + (mSize[0] * x)));
      }
      wr.append("\n");
      for(int z = 0; z < mDims[2]; ++z) {
         wr.append(Double.toString(mPoint[2] + (mSize[2] * z)));
         for(int x = 0; x < mDims[0]; ++x) {
            double sum = 0.0;
            for(int y = 0; y < mDims[1]; ++y)
               sum += mVoxel[x][y][z];
            wr.append(", ");
            wr.append(Double.toString(sum * norm));
         }
         wr.append("\n");
      }
   }

   /**
    * <p>
    * Dumps the raw voxel by voxel energy deposition data. Energy deposition is
    * reported as J m<sup>-3</sup> C<sup>-1</sup> and eV per electron
    * </p>
    * <p>
    * x, y, z, energy dep in J m<sup>-3</sup> C<sup>-1</sup>, energy dep in eV
    * /e<sup>-</sup>
    * </p>
    * 
    * @param wr Writer
    * @param withZeros If false zero values are not written.
    * @throws IOException
    */
   public void dumpVoxels(Writer wr, boolean withZeros)
         throws IOException {
      wr.append("\"Corner[0]\", ");
      wr.append(Double.toString(mPoint[0]));
      wr.append(", ");
      wr.append(Double.toString(mPoint[1]));
      wr.append(", ");
      wr.append(Double.toString(mPoint[2]));
      wr.append("\n");
      wr.append("\"Corner[1]\", ");
      wr.append(Double.toString(mPoint[0] + (mSize[0] * mDims[0])));
      wr.append(", ");
      wr.append(Double.toString(mPoint[1] + (mSize[1] * mDims[1])));
      wr.append(", ");
      wr.append(Double.toString(mPoint[2] + (mSize[2] * mDims[2])));
      wr.append("\n");
      wr.append("\n");
      wr.append("x,y,z,energy\n");
      final double norm = normalization();
      for(int x = 0; x < mDims[0]; ++x)
         for(int y = 0; y < mDims[0]; ++y)
            for(int z = 0; z < mDims[2]; ++z)
               if(withZeros || (mVoxel[x][y][z] != 0.0)) {
                  wr.append(Integer.toString(x));
                  wr.append(", ");
                  wr.append(Integer.toString(y));
                  wr.append(", ");
                  wr.append(Integer.toString(z));
                  wr.append(", ");
                  wr.append(Double.toString(mVoxel[x][y][z] * norm));
                  wr.append(", ");
                  wr.append(Double.toString(FromSI.eV(mVoxel[x][y][z]) / mElectronCount));
                  wr.append("\n");
               }
      wr.append(Double.toString(FromSI.eV(mTotalEnergyLoss) / mElectronCount));
   }

   /**
    * Dumps the sum over the X axis projected onto the YZ plane of the energy
    * dumped into the volume.
    * 
    * @param wr A Writer defining the output device
    * @throws IOException
    */
   public void dumpYZProjection(Writer wr)
         throws IOException {
      final double norm = FromSI.eV(1.0) / mElectronCount;
      for(int x = 0; x < mDims[0]; ++x) {
         wr.append(", ");
         wr.append(Double.toString(mPoint[0] + (mSize[0] * x)));
      }
      wr.append("\n");
      for(int z = 0; z < mDims[2]; ++z) {
         wr.append(Double.toString(mPoint[2] + (mSize[2] * z)));
         for(int y = 0; y < mDims[0]; ++y) {
            double sum = 0.0;
            for(int x = 0; x < mDims[1]; ++x)
               sum += mVoxel[x][y][z];
            wr.append(", ");
            wr.append(Double.toString(sum * norm));
         }
         wr.append("\n");
      }
   }

   private boolean isInside(int[] vox) {
      return (vox[0] >= 0) && (vox[1] >= 0) && (vox[2] >= 0) && (vox[0] < mDims[0]) && (vox[1] < mDims[1])
            && (vox[2] < mDims[2]);

   }

   private boolean isInsideOrEdge(int[] vox) {
      return (vox[0] >= 0) && (vox[1] >= 0) && (vox[2] >= 0) && (vox[0] <= mDims[0]) && (vox[1] <= mDims[1])
            && (vox[2] <= mDims[2]);

   }

   private double[] endBounded(double[] start, double[] end) {
      double[] res = end;
      for(int ax = 0; ax < 3; ax++) {
         final double low = lowSidePosition(ax, 0);
         final double high = lowSidePosition(ax, mDims[ax]);
         if(res[ax] < low) {
            final double t = (low - start[ax]) / (res[ax] - start[ax]);
            if((t >= 0) && (t <= 1.0)) {
               res = Math2.plus(start, Math2.multiply(t, Math2.minus(res, start)));
               assert Math.abs(res[ax] - low) < 1.0e-15;
            } else
               return null;
         } else if(res[ax] > high) {
            final double t = (high - start[ax]) / (res[ax] - start[ax]);
            if((t >= 0) && (t <= 1.0)) {
               res = Math2.plus(start, Math2.multiply(t, Math2.minus(res, start)));
               assert Math.abs(res[ax] - high) < 1.0e-15;
            } else
               return null;
         }
      }
      return res;
   }

   private double[] startBounded(double[] start, double[] end) {
      return endBounded(end, start);
   }

   /**
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   @Override
   public void actionPerformed(ActionEvent arg0) {
      assert arg0.getSource() instanceof MonteCarloSS;
      final MonteCarloSS mcss = (MonteCarloSS) arg0.getSource();
      switch(arg0.getID()) {
         case MonteCarloSS.ScatterEvent:
         case MonteCarloSS.NonScatterEvent: {
            final Electron ee = mcss.getElectron();
            final double[] prevPos = ee.getPrevPosition();
            final double[] thisPos = ee.getPosition();
            // end constrained to instrumented region
            final double[] endPos = endBounded(prevPos, thisPos);
            if(endPos == null)
               return;
            final int[] endVoxel = indexOf(endPos);
            assert isInsideOrEdge(endVoxel) : Arrays.toString(endVoxel);
            // start constrained to instrumented region
            final double[] startPos = startBounded(prevPos, thisPos);
            if(startPos == null)
               return;
            final int[] startVoxel = indexOf(startPos);
            assert isInsideOrEdge(startVoxel) : Arrays.toString(startVoxel);
            double dE = ee.getPreviousEnergy() - ee.getEnergy();
            if(dE == 0.0)
               return;
            final double dEperM = dE / Math2.distance(prevPos, thisPos);
            final double fullLength = Math2.distance(startPos, endPos);
            assert fullLength >= 0.0;
            int[] currVoxel = startVoxel;
            double[] currPos = startPos;
            double remLength = fullLength;
            while(isInside(currVoxel)) {
               // default to the endPos
               double[] nextPos = endPos;
               int[] nextVoxel = endVoxel;
               double currDist = remLength;
               assert Math.abs(Math2.distance(currPos, endPos) - currDist) < 1.0e-15;
               // Unless we leave the current voxel
               for(int i = 0; i < 3; ++i) {
                  if(currVoxel[i] != endVoxel[i]) {
                     final int[] ppVox = Arrays.copyOf(currVoxel, 3);
                     double pp_i;
                     if(currVoxel[i] < endVoxel[i]) {
                        ppVox[i] = currVoxel[i] + 1;
                        pp_i = lowSidePosition(i, ppVox[i]);
                        assert pp_i == highSidePosition(i, currVoxel[i]);
                     } else {
                        ppVox[i] = currVoxel[i] - 1;
                        pp_i = highSidePosition(i, ppVox[i]);
                        assert pp_i == lowSidePosition(i, currVoxel[i]);
                     }
                     final double t = (pp_i - currPos[i]) / (endPos[i] - currPos[i]);
                     if((t >= 0.0) && (t < 1.0)) {
                        // Calculate the start of the next segment
                        final double[] pp = Math2.plus(currPos, Math2.multiply(t, Math2.minus(endPos, currPos)));
                        assert Math.abs(pp[i] - pp_i) < 1.0e-15;
                        final double dd = Math2.distance(currPos, pp);
                        // Select the smallest possible step
                        assert dd < remLength + 1.0e-15;
                        if(dd < currDist) {
                           nextPos = pp;
                           nextVoxel = ppVox;
                           currDist = dd;
                        }
                     }
                  }
               }
               assert currDist <= remLength + 1.0e-15 : currDist + "<=" + remLength;
               assert isInside(currVoxel) : Arrays.toString(currVoxel);
               final double currDe = dEperM * currDist;
               mVoxel[currVoxel[0]][currVoxel[1]][currVoxel[2]] += currDe;
               mTotalEnergyLoss += currDe;
               dE -= currDe;
               assert dE >= -ToSI.eV(1.0e-3) : dE;
               remLength -= currDist;
               assert remLength >= -1.0e-15 : remLength;
               if(sameVoxel(currVoxel, endVoxel))
                  break;
               currVoxel = nextVoxel;
               currPos = nextPos;
            }
         }
            break;
         case MonteCarloSS.TrajectoryStartEvent:
            ++mElectronCount;
            break;
         case MonteCarloSS.BackscatterEvent:
         case MonteCarloSS.TrajectoryEndEvent:
         case MonteCarloSS.LastTrajectoryEvent:
         case MonteCarloSS.FirstTrajectoryEvent:
         case MonteCarloSS.StartSecondaryEvent:
         case MonteCarloSS.EndSecondaryEvent:
         case MonteCarloSS.BeamEnergyChanged:
            break;
      }
   }

   /**
    * Add red coordinate axes of the specified length to the VRML file. These
    * axes serve as both orientation and length scale markers.
    * 
    * @param wr A Writer containing VRML
    * @param length The length of the axes in meters
    * @throws IOException
    */
   public void addAxesToVRML(Writer wr, double length)
         throws IOException {
      final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
      nf.setMaximumFractionDigits(3);
      nf.setGroupingUsed(false);
      wr.append("\n#Prototype for a coordinate axis\n");
      wr.append("PROTO CoordAxis [\n");
      wr.append(" field SFRotation rota 0 0 1 0\n");
      wr.append(" field SFVec3f tran 0 0 0\n");
      wr.append("]\n");
      wr.append("{\n");
      wr.append(" Transform {\n");
      wr.append("  rotation IS rota");
      wr.append("  translation IS tran");
      wr.append("  children [\n");
      wr.append("   Shape {\n");
      wr.append("    geometry Cylinder {\n");
      wr.append("     radius 0.1\n");
      wr.append("     height " + nf.format(length / mSize[2]) + "\n");
      wr.append("     bottom TRUE\n");
      wr.append("     side TRUE\n");
      wr.append("     top TRUE\n");
      wr.append("    }\n");
      wr.append("    appearance Appearance {\n");
      wr.append("     material Material {\n");
      wr.append("      emissiveColor 1 0 0\n");
      wr.append("      transparency 0.7\n");
      wr.append("     }\n");
      wr.append("    }\n");
      wr.append("   }\n");
      wr.append("  ]\n");
      wr.append(" }\n");
      wr.append("}\n");
      {
         final String tmp = nf.format((-0.5 * length) / mSize[2]);
         wr.append("CoordAxis { rota 0 0 1 0 tran 0 " + tmp + " -0.5 }\n");
         wr.append("CoordAxis { rota 0 0 1 1.5708 tran " + tmp + " 0 -0.5 }\n");
      }
      {
         final String tmp = nf.format(((0.5 * length) / mSize[2]) - 0.5);
         wr.append("CoordAxis { rota 1 0 0 1.5708 tran 0 0 " + tmp + " }\n");
      }
   }

   /**
    * Write the voxels to a VRML file. The VRML uses transparency to represent
    * energy deposition so white pixels show high energy deposition and black
    * are none.
    * 
    * @param wr
    * @throws IOException
    */
   public void writeAsVRML(Writer wr)
         throws IOException {
      final double max = maxVoxelValue();
      final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
      nf.setMaximumFractionDigits(3);
      nf.setGroupingUsed(false);
      wr.append("#VRML V2.0 utf8\n");
      wr.append("#Generated by NISTMonte in DTSA-II\n");
      wr.append("WorldInfo {\n");
      wr.append(" title \"NISTMonte Energy Deposition Visualizer\"\n");
      wr.append(" info [\"See http://www.cstl.nist.gov/div837/Division/home.html for more information\"]\n");
      wr.append("}\n");

      wr.append("Viewpoint {\n");
      wr.append(" fieldOfView 0.785398\n");
      wr.append(" position " + Integer.toString((3 * mDims[0]) / 2) + " 0 0\n");
      wr.append(" jump TRUE\n");
      wr.append(" orientation -0.57735 0.57735 -0.57735 2.094395\n");
      wr.append(" description \"X-Axis\"\n");
      wr.append("}\n");

      wr.append("Viewpoint {\n");
      wr.append(" fieldOfView 0.785398\n");
      wr.append(" position 0 " + Integer.toString((3 * mDims[0]) / 2) + " 0\n");
      wr.append(" jump TRUE\n");
      wr.append(" orientation -1 0 0 1.570796\n");
      wr.append(" description \"Y-Axis\"\n");
      wr.append("}\n");

      wr.append("Viewpoint {\n");
      wr.append(" fieldOfView 0.785398\n");
      wr.append(" position 0 0 " + Integer.toString((3 * mDims[0]) / 2) + "\n");
      wr.append(" jump TRUE\n");
      wr.append(" orientation 0 0 -1 1.570796\n");
      wr.append(" description \"Z-Axis\"\n");
      wr.append("}\n");

      wr.append("Viewpoint {\n");
      wr.append(" fieldOfView 0.785398\n");
      final String tmp = Integer.toString(mDims[0]);
      wr.append(" position " + tmp + " " + tmp + " " + tmp + "\n");
      wr.append(" jump TRUE\n");
      wr.append(" orientation -0.742906 0.307721 -0.594472 1.217116\n");
      wr.append(" description \"Oblique - above\"\n");
      wr.append("}\n");

      wr.append("Background { skyColor 0.34 0.2 0.06 }\n");
      wr.append("NavigationInfo { headlight FALSE speed 10.0 }\n");

      wr.append("\n#Prototype for a voxel\n");
      wr.append("PROTO Voxel [\n");
      wr.append(" field SFVec3f offset 0 0 0\n");
      wr.append(" field SFFloat trans 1.0\n");
      wr.append("]\n");
      wr.append("{\n");
      wr.append(" Transform {\n");
      wr.append("  translation IS offset\n");
      wr.append("  children [\n");
      wr.append("   Shape {\n");
      wr.append("    geometry Box {\n");
      wr.append("     size 1,1,1\n");
      wr.append("    }\n");
      wr.append("    appearance Appearance {\n");
      wr.append("     material Material {\n");
      wr.append("      emissiveColor 1 1 1\n");
      wr.append("      transparency IS trans\n");
      wr.append("     }\n");
      wr.append("    }\n");
      wr.append("   }\n");
      wr.append("  ]\n");
      wr.append(" }\n");
      wr.append("}\n");

      for(int x = 0; x < mDims[0]; ++x)
         for(int y = 0; y < mDims[0]; ++y)
            for(int z = 0; z < mDims[2]; ++z)
               if(mVoxel[x][y][z] != 0.0) {
                  final double[] c0 = Math2.ebeDivide(new double[] {
                     lowSidePosition(0, x),
                     lowSidePosition(1, y),
                     lowSidePosition(2, z),
                  }, mSize);
                  // tr -> 0.75 to 0
                  final String trStr = nf.format(0.99 * (1.00 - (mVoxel[x][y][z] / max)));
                  wr.append("\nVoxel {");
                  wr.append(" offset " + nf.format(c0[0] + 0.5) + " ");
                  wr.append(nf.format(c0[1] + 0.5) + " ");
                  wr.append(nf.format(c0[2]));
                  wr.append(" trans " + trStr);
                  wr.append(" }");
               }
   }

   /**
    * Write y-z planes as a series of images x=0,1,..N where N is the number of
    * X-axis bins. The images are labeled 'planeX.png' and stored in the
    * directory specified.
    * 
    * @param dir A directory as a String
    * @param scale The size of the pixels to represent each voxel (scale x
    *           scale)
    * @throws IOException
    */
   public void writeAsImageSet(String dir, int scale)
         throws IOException {
      writeAsImageSet(new File(dir), scale);
   }

   /**
    * Write y-z planes as a series of images x=0,1,..N where N is the number of
    * X-axis bins. The images are labeled 'planeX.png' and stored in the
    * directory specified. Only writes non-zero planes.
    * 
    * @param dir A directory as a File
    * @param scale The size of the pixels to represent each voxel (scale x
    *           scale)
    * @throws IOException
    */
   public void writeAsImageSet(File dir, int scale)
         throws IOException {
      final Color[] cc = buildPalette(0.04F);
      final double max = maxVoxelValue();
      for(int x = 0; x < mDims[0]; ++x) {
         final BufferedImage img = new BufferedImage(mDims[1] * scale, mDims[2] * scale, BufferedImage.TYPE_3BYTE_BGR);
         final Graphics2D gr = (Graphics2D) img.getGraphics();
         boolean nonZero = false;
         for(int y = 0; y < mDims[1]; ++y)
            for(int z = 0; z < mDims[2]; ++z) {
               nonZero |= (mVoxel[x][y][z] > 0.0);
               gr.setColor(cc[(int) (0.999999 + ((255.0 * mVoxel[x][y][z]) / max))]);
               gr.fillRect(y * scale, z * scale, scale, scale);
            }
         final File ff = new File(dir, "plane" + Integer.toString(x) + ".png");
         if(nonZero) {
            drawMicronMarker(scale, gr, 0, 1);
            ImageIO.write(img, "png", ff);
         } else if(ff.exists())
            ff.delete();

      }
   }

   /**
    * Integrates along the y-axis to project the energy deposited onto the X-Y
    * plane and constructs an image.
    * 
    * @param file A path as a File
    * @param scale The size of the pixels to represent each voxel (scale x
    *           scale)
    * @throws IOException
    */
   public void imageXZProjection(File file, int scale)
         throws IOException {
      final Color[] cc = buildPalette(0.04F);
      final double[][] proj = new double[mDims[0]][mDims[2]];
      double max = -Double.MAX_VALUE;
      for(int x = 0; x < mDims[0]; ++x)
         for(int z = 0; z < mDims[2]; ++z) {
            double tmp = 0.0;
            for(int y = 0; y < mDims[1]; ++y)
               tmp += mVoxel[x][y][z];
            if(tmp > max)
               max = tmp;
            proj[x][z] = tmp;
         }
      final BufferedImage img = new BufferedImage(mDims[0] * scale, mDims[2] * scale, BufferedImage.TYPE_3BYTE_BGR);
      final Graphics2D gr = (Graphics2D) img.getGraphics();
      for(int x = 0; x < mDims[0]; ++x)
         for(int z = 0; z < mDims[2]; ++z) {
            gr.setColor(cc[(int) (0.999999 + ((255.0 * proj[x][z]) / max))]);
            gr.fillRect(x * scale, z * scale, scale, scale);
         }
      drawMicronMarker(scale, gr, 0, 2);
      ImageIO.write(img, "png", file);
   }

   /**
    * Integrates along the z-axis to project the energy deposited onto the X-Z
    * plane and constructs an image.
    * 
    * @param file A path as a File
    * @param scale The size of the pixels to represent each voxel (scale x
    *           scale)
    * @throws IOException
    */
   public void imageXYProjection(File file, int scale)
         throws IOException {
      final Color[] cc = buildPalette(0.04F);
      final double[][] proj = new double[mDims[0]][mDims[1]];
      double max = -Double.MAX_VALUE;
      for(int x = 0; x < mDims[0]; ++x)
         for(int y = 0; y < mDims[1]; ++y) {
            double tmp = 0.0;
            for(int z = 0; z < mDims[2]; ++z)
               tmp += mVoxel[x][y][z];
            if(tmp > max)
               max = tmp;
            proj[x][y] = tmp;
         }
      final BufferedImage img = new BufferedImage(mDims[0] * scale, mDims[1] * scale, BufferedImage.TYPE_3BYTE_BGR);
      final Graphics2D gr = (Graphics2D) img.getGraphics();
      for(int x = 0; x < mDims[0]; ++x)
         for(int y = 0; y < mDims[1]; ++y) {
            gr.setColor(cc[(int) (0.999999 + ((255.0 * proj[x][y]) / max))]);
            gr.fillRect(x * scale, y * scale, scale, scale);
         }
      drawMicronMarker(scale, gr, 0, 1);
      ImageIO.write(img, "png", file);
   }

   /**
    * Integrates along the z-axis to project the energy deposited onto the X-Y
    * plane and constructs an image consisting of nRings at 1/nRings,
    * 2/nRings,... fractions of the total energy deposited.
    * 
    * @param file A path as a File
    * @param scale The size of the pixels to represent each voxel (scale x
    *           scale)
    * @param nRings The number of fractional intervals into which to divide the
    *           total energy deposition.
    * @throws IOException
    */
   public void imageXYRings(File file, int scale, int nRings)
         throws IOException {
      final Color[] cc = buildPalette(0.0F);
      final double[][] proj = new double[mDims[0]][mDims[1]];
      double max = -Double.MAX_VALUE, sum = 0.0;
      for(int x = 0; x < mDims[0]; ++x)
         for(int y = 0; y < mDims[1]; ++y) {
            double tmp = 0.0;
            for(int z = 0; z < mDims[2]; ++z)
               tmp += mVoxel[x][y][z];
            if(tmp > max)
               max = tmp;
            proj[x][y] = tmp;
            sum += tmp;
         }
      final BufferedImage img = new BufferedImage(mDims[0] * scale, mDims[1] * scale, BufferedImage.TYPE_3BYTE_BGR);
      final Graphics2D gr = (Graphics2D) img.getGraphics();
      double remains = sum;
      for(int i = nRings - 1; i >= 0; --i) {
         gr.setColor(cc[(int) ((255.0 * (nRings - i)) / nRings)]);
         final double thresh = (double) i / (double) nRings;
         boolean done = false;
         while((remains > (thresh * sum)) && (!done)) {
            done = true;
            double min = Double.MAX_VALUE;
            int xMin = 0, yMin = 0;
            for(int x = 0; x < mDims[0]; ++x)
               for(int y = 0; y < mDims[1]; ++y)
                  if((proj[x][y] > 0.0) && (proj[x][y] < min)) {
                     min = proj[x][y];
                     xMin = x;
                     yMin = y;
                     done = false;
                  }
            remains -= proj[xMin][yMin];
            gr.fillRect(xMin * scale, yMin * scale, scale, scale);
            proj[xMin][yMin] = 0.0;
         }
      }
      drawMicronMarker(scale, gr, 0, 1);
      ImageIO.write(img, "png", file);
   }

   /**
    * Integrates along the z-axis to project the energy deposited onto the X-Y
    * plane and constructs an image consisting of nRings at 1/nRings,
    * 2/nRings,... fractions of the total energy deposited.
    * 
    * @param file A path as a File
    * @param scale The size of the pixels to represent each voxel (scale x
    *           scale)
    * @param nRings The number of fractional intervals into which to divide the
    *           total energy deposition.
    * @throws IOException
    */
   public void imageXZRings(File file, int scale, int nRings)
         throws IOException {
      final Color[] cc = buildPalette(0.0F);
      final double[][] proj = new double[mDims[0]][mDims[2]];
      double max = -Double.MAX_VALUE, sum = 0.0;
      for(int x = 0; x < mDims[0]; ++x)
         for(int z = 0; z < mDims[2]; ++z) {
            double tmp = 0.0;
            for(int y = 0; y < mDims[1]; ++y)
               tmp += mVoxel[x][y][z];
            if(tmp > max)
               max = tmp;
            proj[x][z] = tmp;
            sum += tmp;
         }
      final BufferedImage img = new BufferedImage(mDims[0] * scale, mDims[2] * scale, BufferedImage.TYPE_3BYTE_BGR);
      final Graphics2D gr = (Graphics2D) img.getGraphics();
      double remains = sum;
      for(int i = nRings - 1; i >= 0; --i) {
         gr.setColor(cc[(int) ((255.0 * (nRings - i)) / nRings)]);
         final double thresh = (double) i / (double) nRings;
         boolean done = false;
         while((remains > (thresh * sum)) && (!done)) {
            done = true;
            double min = Double.MAX_VALUE;
            int xMin = 0, zMin = 0;
            for(int x = 0; x < mDims[0]; ++x)
               for(int z = 0; z < mDims[2]; ++z)
                  if((proj[x][z] > 0.0) && (proj[x][z] < min)) {
                     min = proj[x][z];
                     xMin = x;
                     zMin = z;
                     done = false;
                  }
            remains -= proj[xMin][zMin];
            gr.fillRect(xMin * scale, zMin * scale, scale, scale);
            proj[xMin][zMin] = 0.0;
         }
      }
      drawMicronMarker(scale, gr, 0, 2);
      ImageIO.write(img, "png", file);
   }

   private void drawMicronMarker(int scale, final Graphics2D gr, int dim1, int dim2) {
      double markerLen = 100.0e-6;
      int width = (int) Math.round((markerLen * scale) / mSize[0]);
      while(width > ((7 * mDims[dim1] * scale) / 10)) {
         markerLen /= 10.0;
         width = (int) Math.round((markerLen * scale) / mSize[0]);
      }
      final Font oldFont = gr.getFont();
      try {
         gr.setFont(new Font("Sans", Font.PLAIN, (7 * scale * mDims[dim1]) / 256));
         gr.setColor(new Color(255, 253, 123, 255));
         final int left = ((9 * mDims[dim1] * scale) / 10) - width;
         final int top = (19 * mDims[dim2] * scale) / 20;
         gr.fillRect(left, top, width, scale / 2);
         final NumberFormat df = new HalfUpFormat("0.0## \u03BCm");
         final String tmp = df.format(markerLen / 1.0e-6);
         gr.drawString(tmp, left + ((width - gr.getFontMetrics().stringWidth(tmp)) / 2), top - (2 * scale));
      }
      finally {
         gr.setFont(oldFont);
      }
   }

   private static Color[] buildPalette(float offset) {
      final Color[] cc = new Color[256];
      cc[0] = new Color(0, 0, 0);
      final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
      assert cs.getNumComponents() == 1;
      final float minV = cs.getMinValue(0);
      final float maxV = cs.getMaxValue(0);
      for(int i = 1; i < 255; i++) {
         float h = minV + ((offset + (((1.0F - offset) * i) / 255.0F)) * (maxV - minV));
         if(h > 1.0F)
            h = 1.0F;
         cc[i] = new Color(cs, new float[] {
            h
         }, 1.0F);
      }
      return cc;
   }

   /**
    * Returns the energy deposited in the voxel into which the most energy was
    * deposited.
    * 
    * @return double Joules
    */
   public double maxVoxelValue() {
      double max = -Double.MAX_VALUE;
      for(int x = 0; x < mDims[0]; ++x)
         for(int y = 0; y < mDims[1]; ++y)
            for(int z = 0; z < mDims[2]; ++z)
               if(mVoxel[x][y][z] > max)
                  max = mVoxel[x][y][z];
      return max;
   }

   /**
    * Returns the normalization constant to convert eV per
    * (electron&middot;voxel) into J m <sup>-3</sup> C<sup>-1</sup> electron
    * <sup>-1</sup>.
    * 
    * @return double per m<sup>3</sup> per Coulomb
    */
   private double normalization() {
      return FromSI.eV(1.0) / ((mSize[0] * mSize[1] * mSize[2]) * PhysicalConstants.ElectronCharge);
   }

   /**
    * Computes the total energy dumped into the volume. This is the energy of
    * the beam minus any energy lost to backscattered electrons. (X-rays are
    * rare and are neglected.)
    * 
    * @return Energy deposited in Joules
    */
   public double getTotalEnergyDeposition() {
      return mTotalEnergyLoss / mElectronCount;
   }

   /**
    * Divides the total energy deposited into nBins linear bins. Then it
    * determines the number of voxels (the area) into which this fraction of the
    * energy is dumped. This is written to a text file. This implementation uses
    * a lot of memory when the bin count is large.
    * 
    * @param wr Writer
    * @param nBins 100 for 1% bins
    * @throws IOException
    */
   public void writeDepositionVolume(Writer wr, int nBins)
         throws IOException {
      final TreeMap<Double, Integer> bins = new TreeMap<Double, Integer>(new Comparator<Double>() {
         @Override
         public int compare(Double o1, Double o2) {
            return -o1.compareTo(o2);
         }
      });
      double sum = 0.0;
      final double voxArea = mSize[0] * mSize[1] * mSize[2] / 1.0e-18;
      for(int x = 0; x < mDims[0]; ++x)
         for(int y = 0; y < mDims[1]; ++y)
            for(int z = 0; z < mDims[0]; ++z) {
               final double tmp = mVoxel[x][y][z] / mElectronCount;
               if(tmp > 0.0) {
                  sum += tmp;
                  final Double v = Double.valueOf(tmp);
                  final Integer i = bins.get(v);
                  bins.put(v, Integer.valueOf(i != null ? i.intValue() + 1 : 1));
               }
            }
      wr.append("\"Bin\", \"Pct\", \"Min (keV)\", \"Max (keV)\", \"Volume (\u03BCm\u00B3)\"\n");
      for(int i = 0; i < nBins; ++i) {
         final double min = (i * sum) / nBins;
         final double max = ((i + 1) * sum) / nBins;
         wr.append(Integer.toString(i) + ", ");
         wr.append(Double.toString((100.0 * max) / sum) + ", " + Double.toString(FromSI.keV(min)));
         wr.append(", " + Double.toString(FromSI.keV(max)));
         double acc = 0.0, count = 0.0;
         for(final Map.Entry<Double, Integer> me : bins.entrySet()) {
            final double all = me.getKey().doubleValue() * me.getValue().intValue();
            if((acc + all) >= max) {
               count += (max - acc) / me.getKey().doubleValue();
               acc = max;
               break;
            } else {
               acc += all;
               count += me.getValue().intValue();
            }
         }
         wr.append(", ");
         wr.append(Double.toString(count * voxArea));
         wr.append("\n");
      }
   }
}