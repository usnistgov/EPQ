/**
 * gov.nist.nanoscalemetrology.JMONSEL.NormalHeightMapShape Created by: Bin
 * Ming, John Villarrubia Date: June 01, 2011
 */

package gov.nist.nanoscalemetrology.JMONSEL;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Scanner;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.NISTMonte.TrajectoryVRML;
import gov.nist.microanalysis.NISTMonte.TrajectoryVRML.RenderContext;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * NormalHeightMapShape provides a convenient way to represent complicated
 * shapes. Shapes are represented by a 2-dimensional regular (equally spaced)
 * array of heights (along the z direction). Constructor arguments specify the
 * coordinates of the lower left corner of the array and the spacing between
 * points in the x and y directions. Unlike normal multiplane shapes, height
 * maps need not be convex. Height maps do, however, need to be single-valued.
 * That is, there is for each x,y lateral coordinate in the map, there is a
 * single corresponding height. Any surface or part of a surface that meets
 * these requirements can be represented by a height map.
 * </p>
 * <p>
 * A height map, like a plane, extends from -infinity to infinity in the x and y
 * directions. The heights on part of this infinite surface, the part specified
 * by the user's supplied 2-d array of heights, are specified explicitly. The
 * rest of the surface, the parts that lie to the left or right and/or above or
 * below this explicit part, are defined by extending the last specified edge to
 * infinity in all directions. (This shape can, of course, be modified in the
 * usual way by forming unions, intersections, or other combinations with other
 * shapes.)
 * </p>
 * <p>
 * The height map defined here is restricted to heights along the z direction
 * and corresponding lateral coordinates in the x and y directions. It cannot be
 * rotated or translated. (It does not implement ITransform.) These restrictions
 * can be removed by making this height map the "base shape" for an
 * AffinizedNormalShape object. In that case, the NormalHeightMapShape defined
 * in this class can be rotated (also scaled, translated, ...) to face along an
 * arbitrary direction. Shapes that are too complicated even for an ordinary
 * height map (undercut lines, particles with bottom and top surfaces that both
 * need to be specified in detail, etc.) can in this way be built up from height
 * map building blocks.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author Bin Ming, John Villarrubia
 * @version 1.0, 02/21/2012
 */

public class NormalHeightMapShape implements NormalShape, TrajectoryVRML.IRender {

   private final double nPosX[] = {1.0, 0.0, 0.0};
   private final double nNegX[] = {-1.0, 0.0, 0.0};
   private final double nPosY[] = {0.0, 1.0, 0.0};
   private final double nNegY[] = {0.0, -1.0, 0.0};
   private final double nPosZ[] = {0.0, 0.0, 1.0};
   private final double nNegZ[] = {0.0, 0.0, -1.0};

   private double[] result = null;

   private final double htOrigin[] = new double[2];
   private double htDeltaX, htDeltaY;
   private int htXlength, htYlength;
   private boolean bX1D, bY1D;
   private double htData[][];
   private boolean isShapeBelow;

   private NormalMultiPlaneShape[][][] lowerBlocks;
   private NormalMultiPlaneShape[][][] upperBlocks;

   /*
    * The following line defines a tunable constant, RETOLERANCE, which is a
    * round-off error setting. It is used in getIndex to decide whether a
    * computed index stands a chance of being incorrect due to round-off error.
    * When there IS a chance of round-off error, we have to do some extra
    * computations. For efficiency we want to avoid these extra computations--by
    * making RETOLERANCE small--when we can. However, we don't want to make
    * RETOLERANCE so small that we miss any errors. So how big should
    * RETOLERANCE be? The computed index calculation is in the form (pos0r[0] -
    * htOrigin[0]) / htDeltaX. The difference in the numerator should be largest
    * when po0r and htOrigin are close to 0.1 m, their largest possible values
    * for the default 0.1 m chamber size. In this case their difference could be
    * on the order of 1.e-17. Then for htDeltaX = 1.e-10 (that's 0.1 nm--it's
    * hard to imagine any necessity of making it even this small) the round off
    * error would be 1.e-7.
    */
   private static final double RETOLERANCE = 1.01e-7;

   private static final double push = 1.0e-15; // was 1.e-16

   public NormalHeightMapShape(double x0, double y0, double dX, double dY, double data[][]) {
      init(x0, y0, dX, dY, data, true);
   }

   /**
    * Constructs a NormalHeightMapShape. The final argument, data, is an n x m
    * array of heights that specifies the shape. The preceding arguments provide
    * context for the interpretation of the height map. x0 and y0 are the
    * lateral coordinates (in meters) of the first data point (the one in
    * data[0][0]). dX and dY are the distances between pixels in the x and y
    * directions. Thus, the i,j data point has x,y,z coordinates of
    * x0+i*dX,y0+j*dY,data[i][j]. The boolean indicates whether the actual shape
    * is below the height map surface (negative z), which is usually the default
    * case. There are two possible ways to construct a height map described by a
    * one dimensional array (i.e., a cross section): (1) by supplying a data
    * array that indicates one dimensional data, such as in Jython forms [[x1],
    * [x2], [x3] ...] or [[y1, y2, y3 ...]]. The superfluous dY or dX value will
    * be ignored. (2) by indicating either dX == 0. or dY == 0. In either case
    * only the first column (row) of data will be considered.
    *
    * @param x0
    * @param y0
    * @param dX
    * @param dY
    * @param data
    * @param isBelowHT
    */
   public NormalHeightMapShape(double x0, double y0, double dX, double dY, double data[][], boolean isBelowHT) {
      init(x0, y0, dX, dY, data, isBelowHT);
   }

   /**
    * Constructs a NormalHeightMapShape from a data file, typically generated by
    * scanning over a rectangular sample area by SEM or AFM. To be useful for
    * the NormalHeightMapShape shape, the file format requires: 1. The first
    * line is a string "$HeightMapFormat"; 2. Any nonzero value at the second
    * line describes a shape below (-Z) the height map surface. A value of zero
    * indicates a shape above (+Z) the surface. 3. The third and fourth lines
    * are the X and Y coordinates (in meters) of the origin of the height map;
    * 4. The next two lines are the dimension of the height data along X and the
    * step length (in meters) along that direction; 5. The next two lines are
    * the dimension of the height data along Y and the step length (in meters)
    * along that direction; 6. The next line is a unit scale factor that applies
    * to any following height map data (Z). So if the Z values are in
    * nanometers, this line will be "1.e-9". You may optionally use it as a
    * general scale factor that applies to all Z data. It may even assume a
    * negative value, but remember the sides (interior vs. exterior) of the
    * height map shape shall remain unaffected. 7. The data in the rest of file
    * follow the sequence of a SEM or AFM scan: starting from the top left, scan
    * through the first "row" to the right, then "raster" back to the beginning
    * of the the second row, and so on.
    *
    * @param HeightMapFileName
    * @throws FileNotFoundException
    */
   public NormalHeightMapShape(String HeightMapFileName) throws FileNotFoundException {
      final Scanner s = new Scanner(new BufferedReader(new FileReader(HeightMapFileName)));
      s.useLocale(Locale.US);
      try {
         final String str = s.next();
         if (!str.equals("$HeightMapFormat"))
            throw new EPQFatalException("1st token of  file was not $HeightMapFormat");
         boolean isBelowHT = true;
         if (s.nextDouble() == 0.)
            isBelowHT = false;
         final double x0 = s.nextDouble();
         final double y0 = s.nextDouble();
         final int htX = s.nextInt();
         final double dX = s.nextDouble();
         final int htY = s.nextInt();
         final double dY = s.nextDouble();
         final double zunit = s.nextDouble();
         final double data[][] = new double[htX][htY];

         for (int j = 0; j < htY; j++)
            for (int i = 0; i < htX; i++)
               /*
                * Next line, Bin's original, mirror's the Y axis. Not sure why
                * he did that. I removed it.
                */
               // data[i][htY - j - 1] = s.nextDouble() * zunit;
               data[i][j] = s.nextDouble() * zunit;
         init(x0, y0, dX, dY, data, isBelowHT);

         s.close();
      } finally {
         if (s != null)
            s.close();
      }
   }

   /**
    * @param pos
    * @return
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#contains(double[])
    */
   @Override
   public boolean contains(double[] pos) {
      final int[] idx = getXYIndices(pos);
      if (getBlock(idx[0], idx[1], 0, isShapeBelow).contains(pos) || getBlock(idx[0], idx[1], 1, isShapeBelow).contains(pos))
         return true;
      return false;
   }

   /**
    * @param pos0
    * @param pos1
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#contains(double[],
    *      double[])
    */
   @Override
   public boolean contains(double[] pos0, double[] pos1) {
      final int[] idx = getXYIndices(pos0);
      if (getBlock(idx[0], idx[1], 0, isShapeBelow).contains(pos0, pos1) || getBlock(idx[0], idx[1], 1, isShapeBelow).contains(pos0, pos1))
         return true;
      return false;
   }

   /**
    * This is the method to create internal blocks "on demand", significantly
    * minimizing memory usage, especially for large height maps. iB and jB are
    * indices referring to the specific column and row. The boolean lo (low) is
    * true if the block to be created is below the height map surface, or vice
    * verse. The implementation is compatible with one dimensional height map
    * data (cross sections) either in X or Y direction.
    *
    * @param iB
    * @param jB
    * @param lo
    */
   private void createNodeBlocks(int iB, int jB, boolean lo) {
      int i = iB - 1;
      int j = jB - 1;
      if (iB == 0)
         i = 0;
      if (jB == 0)
         j = 0;
      if (iB == (htXlength - 1))
         i = htXlength - 3;
      if (jB == (htYlength - 1))
         j = htYlength - 3;

      if (!(bX1D || bY1D)) {
         // the two NormalMultiPlaneShape objects associated with each node
         // on the NormalHeightMapShape,
         // each with a triangular top plane
         final NormalMultiPlaneShape block1 = new NormalMultiPlaneShape();
         final NormalMultiPlaneShape block2 = new NormalMultiPlaneShape();

         // the two corresponding blocks that are above the NormalHeightShape
         // and opposite block1 and block2
         final NormalMultiPlaneShape block1o = new NormalMultiPlaneShape();
         final NormalMultiPlaneShape block2o = new NormalMultiPlaneShape();

         // calculating the four "nodes" in a square top cell on the
         // NormalHeightMapShape
         final double node1[] = {htOrigin[0] + (i * htDeltaX), htOrigin[1] + (j * htDeltaY), htData[i][j]};
         final double node2[] = {htOrigin[0] + ((i + 1) * htDeltaX), htOrigin[1] + (j * htDeltaY), htData[i + 1][j]};
         final double node3[] = {htOrigin[0] + (i * htDeltaX), htOrigin[1] + ((j + 1) * htDeltaY), htData[i][j + 1]};
         final double node4[] = {htOrigin[0] + ((i + 1) * htDeltaX), htOrigin[1] + ((j + 1) * htDeltaY), htData[i + 1][j + 1]};

         // calculating the four vectors between the four nodes
         final double vector12[] = Math2.minus(node2, node1);
         final double vector13[] = Math2.minus(node3, node1);
         final double vector23[] = Math2.minus(node3, node2);
         final double vector24[] = Math2.minus(node4, node2);
         final double vector34[] = Math2.minus(node4, node3);

         // the two normal vectors going out of the two top triangular planes
         // by calculating the cross products
         final double nPlane1[] = Math2.cross(vector12, vector13);
         final double nPlane2[] = Math2.cross(vector24, vector23);

         final double[] nPlane1o = Math2.negative(nPlane1);
         final double[] nPlane2o = Math2.negative(nPlane2);
         final double nPlane_in_between[] = {vector23[1], -vector23[0], 0.};
         final double[] nminusPlane_in_between = Math2.negative(nPlane_in_between);

         if (lo) {
            // adding four planes to each one of two blocks
            // (NormalMultiPlaneShape objects)

            // we add first the "top" plane, so that in future calls
            // to getFirstNormal(), the top plane takes precedence
            block1.addPlane(nPlane1, node1);
            block1.addPlane(nNegX, node1);
            block1.addPlane(nNegY, node1);
            block1.addPlane(nPlane_in_between, node2);

            block2.addPlane(nPlane2, node4);
            block2.addPlane(nPosX, node4);
            block2.addPlane(nPosY, node4);
            block2.addPlane(nminusPlane_in_between, node2);

            lowerBlocks[i + 1][j + 1][0] = block1;
            lowerBlocks[i + 1][j + 1][1] = block2;

         } else {
            // Now add four planes to each one of two upper "outblocks"
            // (NormalMultiPlaneShape objects)
            block1o.addPlane(nPlane1o, node1);
            block1o.addPlane(nNegX, node1);
            block1o.addPlane(nNegY, node1);
            block1o.addPlane(nPlane_in_between, node2);

            block2o.addPlane(nPlane2o, node4);
            block2o.addPlane(nPosX, node4);
            block2o.addPlane(nPosY, node4);
            block2o.addPlane(nminusPlane_in_between, node2);

            upperBlocks[i + 1][j + 1][0] = block1o;
            upperBlocks[i + 1][j + 1][1] = block2o;
         }

         // For a specific set of [i, j] located at the boundary, the
         // following will construct the appropriate "edge" and
         // possibly "corner" blocks. They are located at the (B)ottom,
         // (T)op, (L)eft, and (R)ight positions.
         if (i == 0) {
            if (j == 0)
               if (lo) {
                  // add the bottom left corner block
                  final NormalMultiPlaneShape blockDownBottomLeftCorner = new NormalMultiPlaneShape();
                  blockDownBottomLeftCorner.addPlane(nPosZ, node1);
                  blockDownBottomLeftCorner.addPlane(nPosX, node1);
                  blockDownBottomLeftCorner.addPlane(nPosY, node1);
                  lowerBlocks[0][0][0] = lowerBlocks[0][0][1] = blockDownBottomLeftCorner;
               } else {
                  final NormalMultiPlaneShape blockUpBottomLeftCorner = new NormalMultiPlaneShape();
                  blockUpBottomLeftCorner.addPlane(nNegZ, node1);
                  blockUpBottomLeftCorner.addPlane(nPosX, node1);
                  blockUpBottomLeftCorner.addPlane(nPosY, node1);
                  upperBlocks[0][0][0] = upperBlocks[0][0][1] = blockUpBottomLeftCorner;
               }
            if (j == (htYlength - 3))
               if (lo) {
                  // add the top left corner block
                  final NormalMultiPlaneShape blockDownTopLeftCorner = new NormalMultiPlaneShape();
                  blockDownTopLeftCorner.addPlane(nPosZ, node3);
                  blockDownTopLeftCorner.addPlane(nPosX, node3);
                  blockDownTopLeftCorner.addPlane(nNegY, node3);
                  lowerBlocks[0][htYlength - 1][0] = lowerBlocks[0][htYlength - 1][1] = blockDownTopLeftCorner;
               } else {
                  final NormalMultiPlaneShape blockUpTopLeftCorner = new NormalMultiPlaneShape();
                  blockUpTopLeftCorner.addPlane(nNegZ, node3);
                  blockUpTopLeftCorner.addPlane(nPosX, node3);
                  blockUpTopLeftCorner.addPlane(nNegY, node3);
                  upperBlocks[0][htYlength - 1][0] = upperBlocks[0][htYlength - 1][1] = blockUpTopLeftCorner;
               }
            // add the "edge" blocks at the leftmost column
            final double n13EdgeUp[] = {0., -vector13[2], vector13[1]};

            if (lo) {
               final NormalMultiPlaneShape blockDownLeftEdge = new NormalMultiPlaneShape();
               blockDownLeftEdge.addPlane(n13EdgeUp, node1);
               blockDownLeftEdge.addPlane(nPosX, node1);
               blockDownLeftEdge.addPlane(nPosY, node3);
               blockDownLeftEdge.addPlane(nNegY, node1);
               lowerBlocks[0][j + 1][0] = lowerBlocks[0][j + 1][1] = blockDownLeftEdge;
            } else {
               final NormalMultiPlaneShape blockUpLeftEdge = new NormalMultiPlaneShape();
               final double n13EdgeDown[] = Math2.negative(n13EdgeUp);
               blockUpLeftEdge.addPlane(n13EdgeDown, node1);
               blockUpLeftEdge.addPlane(nPosX, node1);
               blockUpLeftEdge.addPlane(nPosY, node3);
               blockUpLeftEdge.addPlane(nNegY, node1);
               upperBlocks[0][j + 1][0] = upperBlocks[0][j + 1][1] = blockUpLeftEdge;
            }
         }
         if (i == (htXlength - 3)) {
            if (j == 0)
               if (lo) {
                  // add the bottom right corner block
                  final NormalMultiPlaneShape blockDownBottomRightCorner = new NormalMultiPlaneShape();
                  blockDownBottomRightCorner.addPlane(nPosZ, node2);
                  blockDownBottomRightCorner.addPlane(nNegX, node2);
                  blockDownBottomRightCorner.addPlane(nPosY, node2);
                  lowerBlocks[htXlength - 1][0][0] = lowerBlocks[htXlength - 1][0][1] = blockDownBottomRightCorner;
               } else {
                  final NormalMultiPlaneShape blockUpBottomRightCorner = new NormalMultiPlaneShape();
                  blockUpBottomRightCorner.addPlane(nNegZ, node2);
                  blockUpBottomRightCorner.addPlane(nNegX, node2);
                  blockUpBottomRightCorner.addPlane(nPosY, node2);
                  upperBlocks[htXlength - 1][0][0] = upperBlocks[htXlength - 1][0][1] = blockUpBottomRightCorner;
               }
            if (j == (htYlength - 3))
               if (lo) {
                  // add the top right corner block
                  final NormalMultiPlaneShape blockDownTopRightCorner = new NormalMultiPlaneShape();
                  blockDownTopRightCorner.addPlane(nPosZ, node4);
                  blockDownTopRightCorner.addPlane(nNegX, node4);
                  blockDownTopRightCorner.addPlane(nNegY, node4);
                  lowerBlocks[htXlength - 1][htYlength - 1][0] = lowerBlocks[htXlength - 1][htYlength - 1][1] = blockDownTopRightCorner;
               } else {
                  final NormalMultiPlaneShape blockUpTopRightCorner = new NormalMultiPlaneShape();
                  blockUpTopRightCorner.addPlane(nNegZ, node4);
                  blockUpTopRightCorner.addPlane(nNegX, node4);
                  blockUpTopRightCorner.addPlane(nNegY, node4);
                  upperBlocks[htXlength - 1][htYlength - 1][0] = upperBlocks[htXlength - 1][htYlength - 1][1] = blockUpTopRightCorner;
               }
            // add the "edge" blocks at the rightmost column
            final double n24EdgeUp[] = {0., -vector24[2], vector24[1]};
            if (lo) {
               final NormalMultiPlaneShape blockDownRightEdge = new NormalMultiPlaneShape();
               blockDownRightEdge.addPlane(n24EdgeUp, node2);
               blockDownRightEdge.addPlane(nNegX, node2);
               blockDownRightEdge.addPlane(nPosY, node4);
               blockDownRightEdge.addPlane(nNegY, node2);
               lowerBlocks[htXlength - 1][j + 1][0] = lowerBlocks[htXlength - 1][j + 1][1] = blockDownRightEdge;
            } else {
               final NormalMultiPlaneShape blockUpRightEdge = new NormalMultiPlaneShape();
               final double n24EdgeDown[] = Math2.negative(n24EdgeUp);
               blockUpRightEdge.addPlane(n24EdgeDown, node2);
               blockUpRightEdge.addPlane(nNegX, node2);
               blockUpRightEdge.addPlane(nPosY, node4);
               blockUpRightEdge.addPlane(nNegY, node2);
               upperBlocks[htXlength - 1][j + 1][0] = upperBlocks[htXlength - 1][j + 1][1] = blockUpRightEdge;
            }
         }

         // now add the "edge" blocks of the bottom and top rows
         if (j == 0) {
            final double n12EdgeUp[] = {-vector12[2], 0., vector12[0]};
            if (lo) {
               final NormalMultiPlaneShape blockDownBottomEdge = new NormalMultiPlaneShape();
               blockDownBottomEdge.addPlane(n12EdgeUp, node1);
               blockDownBottomEdge.addPlane(nNegX, node1);
               blockDownBottomEdge.addPlane(nPosX, node2);
               blockDownBottomEdge.addPlane(nPosY, node1);
               lowerBlocks[i + 1][0][0] = lowerBlocks[i + 1][0][1] = blockDownBottomEdge;
            } else {
               final NormalMultiPlaneShape blockUpBottomEdge = new NormalMultiPlaneShape();
               final double n12EdgeDown[] = Math2.negative(n12EdgeUp);
               blockUpBottomEdge.addPlane(n12EdgeDown, node1);
               blockUpBottomEdge.addPlane(nNegX, node1);
               blockUpBottomEdge.addPlane(nPosX, node2);
               blockUpBottomEdge.addPlane(nPosY, node1);
               upperBlocks[i + 1][0][0] = upperBlocks[i + 1][0][1] = blockUpBottomEdge;
            }
         }
         if (j == (htYlength - 3)) {
            final double n34EdgeUp[] = {-vector34[2], 0., vector34[0]};
            if (lo) {
               final NormalMultiPlaneShape blockDownTopEdge = new NormalMultiPlaneShape();
               blockDownTopEdge.addPlane(n34EdgeUp, node3);
               blockDownTopEdge.addPlane(nNegX, node3);
               blockDownTopEdge.addPlane(nPosX, node4);
               blockDownTopEdge.addPlane(nNegY, node3);
               lowerBlocks[i + 1][htYlength - 1][0] = lowerBlocks[i + 1][htYlength - 1][1] = blockDownTopEdge;
            } else {
               final NormalMultiPlaneShape blockUpTopEdge = new NormalMultiPlaneShape();
               final double n34EdgeDown[] = Math2.negative(n34EdgeUp);
               blockUpTopEdge.addPlane(n34EdgeDown, node3);
               blockUpTopEdge.addPlane(nNegX, node3);
               blockUpTopEdge.addPlane(nPosX, node4);
               blockUpTopEdge.addPlane(nNegY, node3);
               upperBlocks[i + 1][htYlength - 1][0] = upperBlocks[i + 1][htYlength - 1][1] = blockUpTopEdge;
            }
         }
      } else if (bX1D && !bY1D) {
         // a one-dimensional cross section in the X direction
         final double node1[] = {htOrigin[0] + (i * htDeltaX), htOrigin[1], htData[i][0]};
         final double node2[] = {htOrigin[0] + ((i + 1) * htDeltaX), htOrigin[1], htData[i + 1][0]};
         final double vector12[] = Math2.minus(node2, node1);
         final double n12EdgeUp[] = {-vector12[2], 0., vector12[0]};
         if (lo) {
            final NormalMultiPlaneShape blockDownBottomEdge = new NormalMultiPlaneShape();
            blockDownBottomEdge.addPlane(n12EdgeUp, node1);
            blockDownBottomEdge.addPlane(nNegX, node1);
            blockDownBottomEdge.addPlane(nPosX, node2);
            blockDownBottomEdge.addPlane(nPosY, node1);
            lowerBlocks[i + 1][0][0] = lowerBlocks[i + 1][0][1] = blockDownBottomEdge;

            final NormalMultiPlaneShape blockDownTopEdge = new NormalMultiPlaneShape();
            blockDownTopEdge.addPlane(n12EdgeUp, node1);
            blockDownTopEdge.addPlane(nNegX, node1);
            blockDownTopEdge.addPlane(nPosX, node2);
            blockDownTopEdge.addPlane(nNegY, node1);
            lowerBlocks[i + 1][1][0] = lowerBlocks[i + 1][1][1] = blockDownTopEdge;
         } else {
            final NormalMultiPlaneShape blockUpBottomEdge = new NormalMultiPlaneShape();
            final double n12EdgeDown[] = Math2.negative(n12EdgeUp);
            blockUpBottomEdge.addPlane(n12EdgeDown, node1);
            blockUpBottomEdge.addPlane(nNegX, node1);
            blockUpBottomEdge.addPlane(nPosX, node2);
            blockUpBottomEdge.addPlane(nPosY, node1);
            upperBlocks[i + 1][0][0] = upperBlocks[i + 1][0][1] = blockUpBottomEdge;

            final NormalMultiPlaneShape blockUpTopEdge = new NormalMultiPlaneShape();
            blockUpTopEdge.addPlane(n12EdgeDown, node1);
            blockUpTopEdge.addPlane(nNegX, node1);
            blockUpTopEdge.addPlane(nPosX, node2);
            blockUpTopEdge.addPlane(nNegY, node1);
            upperBlocks[i + 1][1][0] = upperBlocks[i + 1][1][1] = blockUpTopEdge;
         }
         if (i == 0)
            if (lo) {
               // add the two left corner blocks
               final NormalMultiPlaneShape blockDownBottomLeftCorner = new NormalMultiPlaneShape();
               blockDownBottomLeftCorner.addPlane(nPosZ, node1);
               blockDownBottomLeftCorner.addPlane(nPosX, node1);
               blockDownBottomLeftCorner.addPlane(nPosY, node1);
               lowerBlocks[0][0][0] = lowerBlocks[0][0][1] = blockDownBottomLeftCorner;

               final NormalMultiPlaneShape blockDownTopLeftCorner = new NormalMultiPlaneShape();
               blockDownTopLeftCorner.addPlane(nPosZ, node1);
               blockDownTopLeftCorner.addPlane(nPosX, node1);
               blockDownTopLeftCorner.addPlane(nNegY, node1);
               lowerBlocks[0][1][0] = lowerBlocks[0][1][1] = blockDownTopLeftCorner;
            } else {
               final NormalMultiPlaneShape blockUpBottomLeftCorner = new NormalMultiPlaneShape();
               blockUpBottomLeftCorner.addPlane(nNegZ, node1);
               blockUpBottomLeftCorner.addPlane(nPosX, node1);
               blockUpBottomLeftCorner.addPlane(nPosY, node1);
               upperBlocks[0][0][0] = upperBlocks[0][0][1] = blockUpBottomLeftCorner;

               final NormalMultiPlaneShape blockUpTopLeftCorner = new NormalMultiPlaneShape();
               blockUpTopLeftCorner.addPlane(nNegZ, node1);
               blockUpTopLeftCorner.addPlane(nPosX, node1);
               blockUpTopLeftCorner.addPlane(nNegY, node1);
               upperBlocks[0][1][0] = upperBlocks[0][1][1] = blockUpTopLeftCorner;
            }
         if (i == (htXlength - 3))
            if (lo) {
               // add the two right corner blocks
               final NormalMultiPlaneShape blockDownBottomRightCorner = new NormalMultiPlaneShape();
               blockDownBottomRightCorner.addPlane(nPosZ, node2);
               blockDownBottomRightCorner.addPlane(nNegX, node2);
               blockDownBottomRightCorner.addPlane(nPosY, node2);
               lowerBlocks[htXlength - 1][0][0] = lowerBlocks[htXlength - 1][0][1] = blockDownBottomRightCorner;

               final NormalMultiPlaneShape blockDownTopRightCorner = new NormalMultiPlaneShape();
               blockDownTopRightCorner.addPlane(nPosZ, node2);
               blockDownTopRightCorner.addPlane(nNegX, node2);
               blockDownTopRightCorner.addPlane(nNegY, node2);
               lowerBlocks[htXlength - 1][1][0] = lowerBlocks[htXlength - 1][1][1] = blockDownTopRightCorner;
            } else {
               final NormalMultiPlaneShape blockUpBottomRightCorner = new NormalMultiPlaneShape();
               blockUpBottomRightCorner.addPlane(nNegZ, node2);
               blockUpBottomRightCorner.addPlane(nNegX, node2);
               blockUpBottomRightCorner.addPlane(nPosY, node2);
               upperBlocks[htXlength - 1][0][0] = upperBlocks[htXlength - 1][0][1] = blockUpBottomRightCorner;

               final NormalMultiPlaneShape blockUpTopRightCorner = new NormalMultiPlaneShape();
               blockUpTopRightCorner.addPlane(nNegZ, node2);
               blockUpTopRightCorner.addPlane(nNegX, node2);
               blockUpTopRightCorner.addPlane(nNegY, node2);
               upperBlocks[htXlength - 1][1][0] = upperBlocks[htXlength - 1][1][1] = blockUpTopRightCorner;
            }
      } else if (bY1D && !bX1D) {
         // a one-dimensional cross section in the Y direction
         final double node1[] = {htOrigin[0], htOrigin[1] + (j * htDeltaY), htData[0][j]};
         final double node3[] = {htOrigin[0], htOrigin[1] + ((j + 1) * htDeltaY), htData[0][j + 1]};
         final double vector13[] = Math2.minus(node3, node1);
         final double n13EdgeUp[] = {0., -vector13[2], vector13[1]};
         if (lo) {
            final NormalMultiPlaneShape blockDownLeftEdge = new NormalMultiPlaneShape();
            blockDownLeftEdge.addPlane(n13EdgeUp, node1);
            blockDownLeftEdge.addPlane(nPosX, node1);
            blockDownLeftEdge.addPlane(nPosY, node3);
            blockDownLeftEdge.addPlane(nNegY, node1);
            lowerBlocks[0][j + 1][0] = lowerBlocks[0][j + 1][1] = blockDownLeftEdge;

            final NormalMultiPlaneShape blockDownRightEdge = new NormalMultiPlaneShape();
            blockDownRightEdge.addPlane(n13EdgeUp, node1);
            blockDownRightEdge.addPlane(nNegX, node1);
            blockDownRightEdge.addPlane(nPosY, node3);
            blockDownRightEdge.addPlane(nNegY, node1);
            lowerBlocks[1][j + 1][0] = lowerBlocks[1][j + 1][1] = blockDownRightEdge;
         } else {
            final NormalMultiPlaneShape blockUpLeftEdge = new NormalMultiPlaneShape();
            final double n13EdgeDown[] = Math2.negative(n13EdgeUp);
            blockUpLeftEdge.addPlane(n13EdgeDown, node1);
            blockUpLeftEdge.addPlane(nPosX, node1);
            blockUpLeftEdge.addPlane(nPosY, node3);
            blockUpLeftEdge.addPlane(nNegY, node1);
            upperBlocks[0][j + 1][0] = upperBlocks[0][j + 1][1] = blockUpLeftEdge;

            final NormalMultiPlaneShape blockUpRightEdge = new NormalMultiPlaneShape();
            blockUpRightEdge.addPlane(n13EdgeDown, node1);
            blockUpRightEdge.addPlane(nNegX, node1);
            blockUpRightEdge.addPlane(nPosY, node3);
            blockUpRightEdge.addPlane(nNegY, node1);
            upperBlocks[1][j + 1][0] = upperBlocks[1][j + 1][1] = blockUpRightEdge;
         }
         if (j == 0)
            if (lo) {
               // add the two bottom corner blocks
               final NormalMultiPlaneShape blockDownBottomLeftCorner = new NormalMultiPlaneShape();
               blockDownBottomLeftCorner.addPlane(nPosZ, node1);
               blockDownBottomLeftCorner.addPlane(nPosX, node1);
               blockDownBottomLeftCorner.addPlane(nPosY, node1);
               lowerBlocks[0][0][0] = lowerBlocks[0][0][1] = blockDownBottomLeftCorner;

               final NormalMultiPlaneShape blockDownBottomRightCorner = new NormalMultiPlaneShape();
               blockDownBottomRightCorner.addPlane(nPosZ, node1);
               blockDownBottomRightCorner.addPlane(nNegX, node1);
               blockDownBottomRightCorner.addPlane(nPosY, node1);
               lowerBlocks[1][0][0] = lowerBlocks[1][0][1] = blockDownBottomRightCorner;
            } else {
               final NormalMultiPlaneShape blockUpBottomLeftCorner = new NormalMultiPlaneShape();
               blockUpBottomLeftCorner.addPlane(nNegZ, node1);
               blockUpBottomLeftCorner.addPlane(nPosX, node1);
               blockUpBottomLeftCorner.addPlane(nPosY, node1);
               upperBlocks[0][0][0] = upperBlocks[0][0][1] = blockUpBottomLeftCorner;

               final NormalMultiPlaneShape blockUpBottomRightCorner = new NormalMultiPlaneShape();
               blockUpBottomRightCorner.addPlane(nNegZ, node1);
               blockUpBottomRightCorner.addPlane(nNegX, node1);
               blockUpBottomRightCorner.addPlane(nPosY, node1);
               upperBlocks[1][0][0] = upperBlocks[1][0][1] = blockUpBottomRightCorner;
            }
         if (j == (htYlength - 3))
            if (lo) {
               // add the two top corner blocks
               final NormalMultiPlaneShape blockDownTopLeftCorner = new NormalMultiPlaneShape();
               blockDownTopLeftCorner.addPlane(nPosZ, node3);
               blockDownTopLeftCorner.addPlane(nPosX, node3);
               blockDownTopLeftCorner.addPlane(nNegY, node3);
               lowerBlocks[0][htYlength - 1][0] = lowerBlocks[0][htYlength - 1][1] = blockDownTopLeftCorner;

               final NormalMultiPlaneShape blockDownTopRightCorner = new NormalMultiPlaneShape();
               blockDownTopRightCorner.addPlane(nPosZ, node3);
               blockDownTopRightCorner.addPlane(nNegX, node3);
               blockDownTopRightCorner.addPlane(nNegY, node3);
               lowerBlocks[1][htYlength - 1][0] = lowerBlocks[1][htYlength - 1][1] = blockDownTopRightCorner;
            } else {
               final NormalMultiPlaneShape blockUpTopLeftCorner = new NormalMultiPlaneShape();
               blockUpTopLeftCorner.addPlane(nNegZ, node3);
               blockUpTopLeftCorner.addPlane(nPosX, node3);
               blockUpTopLeftCorner.addPlane(nNegY, node3);
               upperBlocks[0][htYlength - 1][0] = upperBlocks[0][htYlength - 1][1] = blockUpTopLeftCorner;

               final NormalMultiPlaneShape blockUpTopRightCorner = new NormalMultiPlaneShape();
               blockUpTopRightCorner.addPlane(nNegZ, node3);
               blockUpTopRightCorner.addPlane(nNegX, node3);
               blockUpTopRightCorner.addPlane(nNegY, node3);
               upperBlocks[1][htYlength - 1][0] = upperBlocks[1][htYlength - 1][1] = blockUpTopRightCorner;
            }
      } else {
         // one dimension in both X and Y, i.e., a single data point
         final double node1[] = {htOrigin[0], htOrigin[1], htData[0][0]};
         if (lo) {
            final NormalMultiPlaneShape blockDownBottomLeftCorner = new NormalMultiPlaneShape();
            blockDownBottomLeftCorner.addPlane(nPosZ, node1);
            blockDownBottomLeftCorner.addPlane(nPosX, node1);
            blockDownBottomLeftCorner.addPlane(nPosY, node1);
            lowerBlocks[0][0][0] = lowerBlocks[0][0][1] = blockDownBottomLeftCorner;

            final NormalMultiPlaneShape blockDownBottomRightCorner = new NormalMultiPlaneShape();
            blockDownBottomRightCorner.addPlane(nPosZ, node1);
            blockDownBottomRightCorner.addPlane(nNegX, node1);
            blockDownBottomRightCorner.addPlane(nPosY, node1);
            lowerBlocks[1][0][0] = lowerBlocks[1][0][1] = blockDownBottomRightCorner;

            final NormalMultiPlaneShape blockDownTopLeftCorner = new NormalMultiPlaneShape();
            blockDownTopLeftCorner.addPlane(nPosZ, node1);
            blockDownTopLeftCorner.addPlane(nPosX, node1);
            blockDownTopLeftCorner.addPlane(nNegY, node1);
            lowerBlocks[0][1][0] = lowerBlocks[0][1][1] = blockDownTopLeftCorner;

            final NormalMultiPlaneShape blockDownTopRightCorner = new NormalMultiPlaneShape();
            blockDownTopRightCorner.addPlane(nPosZ, node1);
            blockDownTopRightCorner.addPlane(nNegX, node1);
            blockDownTopRightCorner.addPlane(nNegY, node1);
            lowerBlocks[1][1][0] = lowerBlocks[1][1][1] = blockDownTopRightCorner;
         } else {
            final NormalMultiPlaneShape blockUpBottomLeftCorner = new NormalMultiPlaneShape();
            blockUpBottomLeftCorner.addPlane(nNegZ, node1);
            blockUpBottomLeftCorner.addPlane(nPosX, node1);
            blockUpBottomLeftCorner.addPlane(nPosY, node1);
            lowerBlocks[0][0][0] = lowerBlocks[0][0][1] = blockUpBottomLeftCorner;

            final NormalMultiPlaneShape blockUpBottomRightCorner = new NormalMultiPlaneShape();
            blockUpBottomRightCorner.addPlane(nNegZ, node1);
            blockUpBottomRightCorner.addPlane(nNegX, node1);
            blockUpBottomRightCorner.addPlane(nPosY, node1);
            upperBlocks[1][0][0] = upperBlocks[1][0][1] = blockUpBottomRightCorner;

            final NormalMultiPlaneShape blockUpTopLeftCorner = new NormalMultiPlaneShape();
            blockUpTopLeftCorner.addPlane(nNegZ, node1);
            blockUpTopLeftCorner.addPlane(nPosX, node1);
            blockUpTopLeftCorner.addPlane(nNegY, node1);
            upperBlocks[0][1][0] = upperBlocks[0][1][1] = blockUpTopLeftCorner;

            final NormalMultiPlaneShape blockUpTopRightCorner = new NormalMultiPlaneShape();
            blockUpTopRightCorner.addPlane(nNegZ, node1);
            blockUpTopRightCorner.addPlane(nNegX, node1);
            blockUpTopRightCorner.addPlane(nNegY, node1);
            upperBlocks[1][1][0] = upperBlocks[1][1][1] = blockUpTopRightCorner;
         }
      }
   }

   /**
    * This is the method to return an internal block by a set of given indices.
    * If the specific block has never been created before, the method
    * creatNodeBlocks() will actually create one or several blocks.
    *
    * @param iB
    * @param jB
    * @param wB
    * @param lower
    * @return
    */
   public NormalMultiPlaneShape getBlock(int iB, int jB, int wB, boolean lower) {
      if (lower) {
         if (lowerBlocks[iB][jB][wB] == null)
            createNodeBlocks(iB, jB, true);
         return lowerBlocks[iB][jB][wB];
      } else {
         if (upperBlocks[iB][jB][wB] == null)
            createNodeBlocks(iB, jB, false);
         return upperBlocks[iB][jB][wB];
      }
   }

   private int[] getBlockIndex(double[] curr, double[] p1, boolean ifpush) {
      boolean xBoundary, yBoundary, xyDiagonalBoundary;
      xBoundary = yBoundary = xyDiagonalBoundary = false;
      final int[] idx = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0};

      // normalized x, y positions
      final double xpos = (curr[0] - htOrigin[0]) / htDeltaX;
      final double ypos = (curr[1] - htOrigin[1]) / htDeltaY;

      int xval = (int) Math.floor(xpos) + 1;
      int yval = (int) Math.floor(ypos) + 1;

      // if no push is intended, then we simply assign the indices by the
      // coordinates of curr[]
      if (!ifpush) {
         idx[0] = xval;
         idx[1] = yval;
         if (idx[0] < 0)
            idx[0] = 0;
         if (idx[0] > (htXlength - 1))
            idx[0] = htXlength - 1;
         if (idx[1] < 0)
            idx[1] = 0;
         if (idx[1] > (htYlength - 1))
            idx[1] = htYlength - 1;

         idx[2] = (((xpos + ypos) - xval - yval) + 1) > 0 ? 1 : 0;

         if ((xval - xpos - 1) == 0.)
            idx[3] += 1;
         if ((yval - ypos - 1) == 0.)
            idx[3] += 2;
         if ((((xpos + ypos) - xval - yval) + 1) == 0.)
            idx[3] += 4;
         return idx;
      }

      // a 3-digit binary value to indicate direction from curr[] to p1[]
      int v = 0;
      final double deltax = p1[0] - curr[0];
      final double deltay = p1[1] - curr[1];
      if (deltax > 0)
         v |= 1;
      if (deltay > 0)
         v |= 2;
      if ((deltax + deltay) > 0)
         v |= 4;

      // flags are set when xpos or ypos is evaluated to be within rounding
      // errors of a node or a boundary plane
      if ((xpos - Math.floor(xpos)) < RETOLERANCE)
         xBoundary = true;
      else if ((Math.ceil(xpos) - xpos) < RETOLERANCE) {
         xBoundary = true;
         xval += 1;
      }

      if ((ypos - Math.floor(ypos)) < RETOLERANCE)
         yBoundary = true;
      else if ((Math.ceil(ypos) - ypos) < RETOLERANCE) {
         yBoundary = true;
         yval += 1;
      }

      final double xnode = ((xval - 1) * htDeltaX) + htOrigin[0];
      final double ynode = ((yval - 1) * htDeltaY) + htOrigin[1];

      if (xBoundary && yBoundary)
         // mapping sections around a node, clockwise from the top right
         // quadrant
         switch (v) {
            case 7 :
               idx[0] = xval;
               idx[1] = yval;
               idx[2] = 0;
               curr[0] = xnode + push;
               curr[1] = ynode + push;
               break;
            case 5 :
               idx[0] = xval;
               idx[1] = yval - 1;
               idx[2] = 1;
               curr[0] = xnode + (2 * push);
               curr[1] = ynode - push;
               break;
            case 1 :
               idx[0] = xval;
               idx[1] = yval - 1;
               idx[2] = 0;
               curr[0] = xnode + push;
               curr[1] = ynode - (2 * push);
               break;
            case 0 :
               idx[0] = xval - 1;
               idx[1] = yval - 1;
               idx[2] = 1;
               curr[0] = xnode - push;
               curr[1] = ynode - push;
               break;
            case 2 :
               idx[0] = xval - 1;
               idx[1] = yval;
               idx[2] = 0;
               curr[0] = xnode - (2 * push);
               curr[1] = ynode + push;
               break;
            case 6 :
               idx[0] = xval - 1;
               idx[1] = yval;
               idx[2] = 1;
               curr[0] = xnode - push;
               curr[1] = ynode + (2 * push);
               break;
            // default should not happen
            default :
               idx[0] = -1;
               idx[1] = -1;
               idx[2] = -1;
               break;
         }
      else if (xBoundary) {
         v &= 1;
         if (v == 1) {
            idx[0] = xval;
            idx[1] = yval;
            idx[2] = 0;
            curr[0] = xnode + push;

         } else if (v == 0) {
            idx[0] = xval - 1;
            idx[1] = yval;
            idx[2] = 1;
            curr[0] = xnode - push;
         }
      } else if (yBoundary) {
         v &= 2;
         if (v == 2) {
            idx[0] = xval;
            idx[1] = yval;
            idx[2] = 0;
            curr[1] = ynode + push;
         } else if (v == 0) {
            idx[0] = xval;
            idx[1] = yval - 1;
            idx[2] = 1;
            curr[1] = ynode - push;
         }
      }
      // not at a node, but along a diagonal block boundary
      else if ((Math.abs(((xpos + ypos) - xval - yval) + 1) < RETOLERANCE) && (xpos > 0) && (xpos < (htXlength - 2)) && (ypos > 0)
            && (ypos < (htYlength - 2))) {
         xyDiagonalBoundary = true;
         v &= 4;
         if (v == 4) {
            idx[0] = xval;
            idx[1] = yval;
            idx[2] = 1;
            curr[1] = (((xval + yval) - 1 - xpos) * htDeltaY) + htOrigin[1] + push;
            curr[0] += push;
         } else if (v == 0) {
            idx[0] = xval;
            idx[1] = yval;
            idx[2] = 0;
            curr[1] = ((((xval + yval) - 1 - xpos) * htDeltaY) + htOrigin[1]) - push;
            curr[0] -= push;
         }
      }
      // the last two cases don't involve any boundary scenarios
      else if ((((xpos + ypos) - xval - yval) + 1) > 0) {
         idx[0] = xval;
         idx[1] = yval;
         idx[2] = 1;
      } else if ((((xpos + ypos) - xval - yval) + 1) < 0) {
         idx[0] = xval;
         idx[1] = yval;
         idx[2] = 0;
      }
      if (idx[0] < 0)
         idx[0] = 0;
      if (idx[0] > (htXlength - 1))
         idx[0] = htXlength - 1;
      if (idx[1] < 0)
         idx[1] = 0;
      if (idx[1] > (htYlength - 1))
         idx[1] = htYlength - 1;
      if (xBoundary || yBoundary || xyDiagonalBoundary) {
         if (xBoundary)
            idx[3] += 1;
         if (yBoundary)
            idx[3] += 2;
         if (xyDiagonalBoundary)
            idx[3] += 4;
      }
      return idx;
   }

   /**
    * @param pos0
    * @param pos1
    * @return
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#getFirstIntersection(double[],
    *      double[])
    */
   @Override
   public double getFirstIntersection(double[] pos0, double[] pos1) {
      return (getFirstNormal(pos0, pos1))[3];
   }

   /**
    * @param pos0
    * @param pos1
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#getFirstNormal(double[],
    *      double[])
    */
   @Override
   public double[] getFirstNormal(double[] pos0, double[] pos1) {
      result = new double[]{.0, .0, .0, Double.MAX_VALUE};

      final double currIntersect[] = pos0.clone();
      final double intersectAtBoundary[] = pos0.clone();
      NormalMultiPlaneShape currBlock, currOppositeBlock;
      int[] currIndex = getBlockIndex(currIntersect, pos1, true);
      currBlock = getBlock(currIndex[0], currIndex[1], currIndex[2], true);
      // flag to indicate whether the electron originally starts from
      // below or above the height map shape
      final boolean ifStartBelow = currBlock.contains(currIntersect, pos1);
      if (!ifStartBelow)
         currBlock = getBlock(currIndex[0], currIndex[1], currIndex[2], false);

      // In case pos0 and pos1 belong to the same internal block, and the
      // electron gets pushed out (later) into a neighboring block, an infinite
      // loop may ensue. Here we find out the original block where pos0 belongs
      // (without the push), and return before a possible infinite loop.
      final int[] pos0Index = getBlockIndex(pos0, pos1, false);
      if ((currIndex[0] != pos0Index[0]) || (currIndex[1] != pos0Index[1]) || (currIndex[2] != pos0Index[2])) {
         // we have to be more careful if pos0 sits exactly on any
         // internal boundaries
         if (pos0Index[3] != 0)
            // pos0 is on a node on the x-y plane
            if ((pos0Index[3] & 3) == 3) {
               if (getBlock(pos0Index[0], pos0Index[1] - 1, 1, ifStartBelow).contains(pos0, pos1)) {
                  pos0Index[1] -= 1;
                  pos0Index[2] = 1;
               } else if (getBlock(pos0Index[0], pos0Index[1] - 1, 0, ifStartBelow).contains(pos0, pos1)) {
                  pos0Index[1] -= 1;
                  pos0Index[2] = 0;
               } else if (getBlock(pos0Index[0] - 1, pos0Index[1] - 1, 1, ifStartBelow).contains(pos0, pos1)) {
                  pos0Index[0] -= 1;
                  pos0Index[1] -= 1;
                  pos0Index[2] = 1;
               } else if (getBlock(pos0Index[0] - 1, pos0Index[1], 0, ifStartBelow).contains(pos0, pos1)) {
                  pos0Index[0] -= 1;
                  pos0Index[2] = 0;
               } else if (getBlock(pos0Index[0] - 1, pos0Index[1], 1, ifStartBelow).contains(pos0, pos1)) {
                  pos0Index[0] -= 1;
                  pos0Index[2] = 1;
               }
               // pos0 is on an internal boundary where x is fixed
            } else if ((pos0Index[3] & 1) == 1) {
               if (getBlock(pos0Index[0] - 1, pos0Index[1], 1, ifStartBelow).contains(pos0, pos1)) {
                  pos0Index[0] -= 1;
                  pos0Index[2] = 1;
               }
               // pos0 is on an internal boundary where y is fixed
            } else if ((pos0Index[3] & 2) == 2) {
               if (getBlock(pos0Index[0], pos0Index[1] - 1, 1, ifStartBelow).contains(pos0, pos1)) {
                  pos0Index[1] -= 1;
                  pos0Index[2] = 1;
               }
               // pos0 is on a diagonal internal boundary on the x-y plane
            } else if ((pos0Index[3] & 4) == 4)
               if (getBlock(pos0Index[0], pos0Index[1], 0, ifStartBelow).contains(pos0, pos1))
                  pos0Index[2] = 0;
               else if (getBlock(pos0Index[0], pos0Index[1], 1, ifStartBelow).contains(pos0, pos1))
                  pos0Index[2] = 1;
         final NormalMultiPlaneShape pos0Block = getBlock(pos0Index[0], pos0Index[1], pos0Index[2], ifStartBelow);
         final double[] result0 = pos0Block.getFirstNormal(pos0, pos1);
         if ((result0[3] < 0.) || (result0[3] >= 1.0)) {
            result = result0;
            return result;
         }
      }

      double deltaIntersect = 0.;
      double delta = 0.;
      for (int nCount = 0;; nCount++) {
         result = currBlock.getFirstNormal(currIntersect, pos1);

         if ((result[3] < 0.) || (result[3] > 1.0)) {
            // return if there is no intersection
            if (currBlock.contains(currIntersect, pos1))
               return result;
            // in the event the electron is found outside the current
            // block, we "trace back" toward the intersect point before
            // the push, and calculate the current u
            currOppositeBlock = getBlock(currIndex[0], currIndex[1], currIndex[2], !ifStartBelow);
            final double[] posPushedOut = currIntersect.clone();
            result = currOppositeBlock.getFirstNormal(currIntersect, intersectAtBoundary);
            // if the electron is found outside our height map shape for the
            // first time, there is an ever slight possibility that it will
            // continue its trajectory back inside the shape, so we "nudge"
            // it inside the shape for only one time
            if ((result[3] > 0.) && (result[3] < 1.0)) {
               for (int i = 0; i < 3; i++)
                  currIntersect[i] = currIntersect[i] + (((result[3] + 1) / 2) * (intersectAtBoundary[i] - currIntersect[i]));
               result = currBlock.getFirstNormal(currIntersect, pos1);
            }
            if ((result[3] < 0.) || (result[3] > 1.0)) {
               for (int i = 0; i < 3; i++)
                  result[i] = (ifStartBelow ^ isShapeBelow) ? -currBlock.getNormal(0)[i] : currBlock.getNormal(0)[i];
               final double uPushedOut = currOppositeBlock.getFirstNormal(pos0, posPushedOut)[3];
               // if((uPushedOut < 0.) || (uPushedOut > 1.0))
               // uPushedOut = 1.;
               for (int i = 0; i < 3; i++) {
                  deltaIntersect += (posPushedOut[i] - pos0[i]) * uPushedOut * result[i];
                  delta += (pos1[i] - pos0[i]) * result[i];
               }
               // if(deltaIntersect / delta < 0 || deltaIntersect / delta >
               // 1.)
               // nCount = nCount + 0;
               result[3] = deltaIntersect / delta;
               return result;
            }

         }
         // check if the vector going from pos0 to pos1 intersects immediately
         // from the top plane of currBlock by checking the Z component of the
         // plane normal. This applies to the outside to inside transition, and
         // inside to outside as well
         if (result[2] != 0.) {
            for (int i = 0; i < 3; i++) {
               deltaIntersect += ((currIntersect[i] - pos0[i]) + (result[3] * (pos1[i] - currIntersect[i]))) * result[i];
               delta += (pos1[i] - pos0[i]) * result[i];
            }
            if ((deltaIntersect / delta) > 1.)
               nCount = nCount + 0;
            else if ((deltaIntersect / delta) < 0)
               result[3] = 0.;
            else
               result[3] = deltaIntersect / delta;
            // if it's an outside to inside transition, then the sign of
            // the
            // normal needs to be inverted
            if (ifStartBelow ^ isShapeBelow)
               for (int i = 0; i < 3; i++)
                  result[i] = -result[i];
            return result;
         }

         // the intersecting point becomes the current starting point
         for (int i = 0; i < 3; i++) {
            // currIntersect[i] = currIntersect[i] + (result[3] * (pos1[i] -
            // currIntersect[i]));
            currIntersect[i] = currIntersect[i] + (result[3] * (pos1[i] - currIntersect[i])) + result[i] * push;
            intersectAtBoundary[i] = currIntersect[i];
         }
         currIndex = getBlockIndex(currIntersect, pos1, true);
         currBlock = getBlock(currIndex[0], currIndex[1], currIndex[2], ifStartBelow);
      }
   }

   /**
    * Get the step length in the X direction
    *
    * @return
    */
   public double gethtDeltaX() {
      return htDeltaX;
   }

   /**
    * Get the step length in the Y direction
    *
    * @return
    */
   public double gethtDeltaY() {
      return htDeltaY;
   }

   /**
    * Get the coordinates of the origin
    *
    * @return
    */
   public double[] gethtOrigin() {
      return htOrigin;
   }

   /**
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#getPreviousNormal()
    */
   @Override
   public double[] getPreviousNormal() {
      return result;
   }

   /**
    * An utility method to calculate the X, Y indices BEFORE any transforms.
    * Therefore the input coordinates were already (inversely) converted.
    *
    * @param posr
    * @return
    */
   private int[] getXYIndices(double[] posr) {
      final int idx[] = new int[2];
      double xypos = (posr[0] - htOrigin[0]) / htDeltaX;
      if (xypos < 0)
         idx[0] = 0;
      else if (xypos > (htXlength - 2))
         idx[0] = htXlength - 1;
      else
         idx[0] = (int) Math.floor(xypos) + 1;

      xypos = (posr[1] - htOrigin[1]) / htDeltaY;
      if (xypos < 0)
         idx[1] = 0;
      else if (xypos > (htYlength - 2))
         idx[1] = htYlength - 1;
      else
         idx[1] = (int) Math.floor(xypos) + 1;

      return idx;
   }

   /**
    * initialization for class members.
    *
    * @param x0
    * @param y0
    * @param dX
    * @param dY
    * @param data
    * @param isBelowHT
    */
   private void init(double x0, double y0, double dX, double dY, double data[][], boolean isBelowHT) {
      htOrigin[0] = x0;
      htOrigin[1] = y0;
      htDeltaX = dX;
      htDeltaY = dY;

      htXlength = data.length + 1;
      htYlength = data[0].length + 1;
      bX1D = bY1D = false;
      // We consider the height map to be one dimensional in X or Y
      // if (1) the supplied data is one dimensional
      // or (2) dX or dY is zero in value
      if ((htYlength == 2) || (dY == 0.)) {
         // a dummy non-zero value
         htDeltaY = 1.;
         // one dimension along X
         htYlength = 2;
         bX1D = true;
      }
      if ((htXlength == 2) || (dX == 0.)) {
         // a dummy non-zero value
         htDeltaX = 1.;
         htXlength = 2;
         bY1D = true;
      }

      htData = data;
      isShapeBelow = isBelowHT;
      lowerBlocks = new NormalMultiPlaneShape[htXlength][htYlength][2];
      upperBlocks = new NormalMultiPlaneShape[htXlength][htYlength][2];

   }

   /**
    * @param rc
    * @param wr
    * @throws IOException
    * @see gov.nist.microanalysis.NISTMonte.TrajectoryVRML.IRender#render(gov.nist.microanalysis.NISTMonte.TrajectoryVRML.RenderContext,
    *      java.io.Writer)
    */
   @Override
   public void render(RenderContext rc, Writer wr) throws IOException {
      // Configure the number format
      final NumberFormat nf = NumberFormat.getInstance(Locale.US);
      nf.setMaximumFractionDigits(3);
      nf.setGroupingUsed(false);
      // Configure the rendering color
      final Color color = rc.getCurrentColor();
      final String trStr = nf.format(rc.getTransparency());
      final String colorStr = nf.format(color.getRed() / 255.0) + " " + nf.format(color.getGreen() / 255.0) + " "
            + nf.format(color.getBlue() / 255.0);
      // Write each face separately (not solid)
      wr.append("\nShape {\n");
      wr.append(" geometry IndexedFaceSet {\n");
      wr.append("  coord Coordinate {\n");
      wr.append("   point [ ");

      for (int i = 0; i < (htXlength - 1); i++)
         for (int j = 0; j < (htYlength - 1); j++) {
            if ((i != 0) || (j != 0))
               wr.append(", \n");
            // three points on a plane, CCW
            wr.append(nf.format((htOrigin[0] + (i * htDeltaX)) / TrajectoryVRML.SCALE));
            wr.append(" ");
            wr.append(nf.format((htOrigin[1] + (j * htDeltaY)) / TrajectoryVRML.SCALE));
            wr.append(" ");
            wr.append(nf.format(htData[i][j] / TrajectoryVRML.SCALE));
         }
      wr.append(" ]\n");
      wr.append("  }\n");
      wr.append("  coordIndex [ ");
      for (int i = 0; i < (htXlength - 2); i++)
         for (int j = 0; j < (htYlength - 2); j++) {
            if ((i != 0) || (j != 0))
               wr.append(" -1 \n");
            wr.append(Integer.toString(((htYlength - 1) * i) + j) + " " + Integer.toString(((htYlength - 1) * (i + 1)) + j) + " "
                  + Integer.toString(((htYlength - 1) * i) + j + 1) + " -1 ");
            wr.append(Integer.toString(((htYlength - 1) * (i + 1)) + j) + " " + Integer.toString(((htYlength - 1) * (i + 1)) + j + 1) + " "
                  + Integer.toString(((htYlength - 1) * i) + (j + 1)));
         }
      wr.append(" ]\n");
      wr.append("  solid FALSE\n");
      wr.append(" }\n");
      wr.append(" appearance Appearance {\n");
      wr.append("  material Material {\n");
      wr.append("   emissiveColor " + colorStr + "\n");
      wr.append("   transparency " + trStr + "\n");
      wr.append("  }\n");
      wr.append(" }\n");
      wr.append("}");
      wr.flush();
   }

}
