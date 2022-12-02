package gov.nist.microanalysis.NISTMonte;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import gov.nist.microanalysis.EPQLibrary.ITransform;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.Transform3D;

/**
 * <p>
 * MCSS_MultiPlane implements simple or more complex shapes as the region
 * bounded by a series of planes. The planes are defined by a normal pointing to
 * the outside of the body (imagine a porcupine) and a point on the plane. A
 * point is determined to be inside the MCSS_MultiPlane object if the point is
 * on the inside (the side away from the direction of the surface normal) of
 * each plane.
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

public class MultiPlaneShape extends Intersection implements MonteCarloSS.Shape, ITransform, TrajectoryVRML.IRender {

   // Helper class to handle individual planes
   final static public class Plane implements MonteCarloSS.Shape, ITransform {
      double[] mNormal = new double[3];
      double[] mPoint = new double[3];

      // construct a Plane object with the specified normal containing the
      // specified point
      public Plane(double[] normal, double[] point) {
         super();
         assert (normal.length == 3);
         assert (point.length == 3);
         System.arraycopy(normal, 0, mNormal, 0, 3);
         System.arraycopy(point, 0, mPoint, 0, 3);
      }

      // contains - Is the point p on the inside side of the plane? (Side
      // opposite to the direction of the normal)
      @Override
      public boolean contains(double[] p) {
         assert (p.length == 3);
         return ((((p[0] - mPoint[0]) * mNormal[0]) + ((p[1] - mPoint[1]) * mNormal[1]) + ((p[2] - mPoint[2]) * mNormal[2])) <= 0.0);
      }

      // Is the point close to being contained by this plane?
      public boolean almostContains(double[] p) {
         assert (p.length == 3);
         final double tmp = (((p[0] - mPoint[0]) * mNormal[0]) + ((p[1] - mPoint[1]) * mNormal[1]) + ((p[2] - mPoint[2]) * mNormal[2]));
         return tmp <= 0.0;
      }

      // intersection - Where does a line through p1 and p2 intersect the plane?
      // (Double.MAX_VALUE if the line and the plane
      // are parallel or the line intersection occurs before p1)
      @Override
      public double getFirstIntersection(double[] p1, double[] p2) {
         assert (p1.length == 3);
         assert (p2.length == 3);
         final double den = ((p2[0] - p1[0]) * mNormal[0]) + ((p2[1] - p1[1]) * mNormal[1]) + ((p2[2] - p1[2]) * mNormal[2]);
         if (den != 0.0) {
            final double res = (((mPoint[0] - p1[0]) * mNormal[0]) + ((mPoint[1] - p1[1]) * mNormal[1]) + ((mPoint[2] - p1[2]) * mNormal[2])) / den;
            return res < 0.0 ? Double.MAX_VALUE : res;
         } else
            return Double.MAX_VALUE;
      }

      // See ITransform for JavaDoc
      @Override
      public void rotate(double[] pivot, double phi, double theta, double psi) {
         mNormal = Transform3D.rotate(mNormal, phi, theta, psi);
         mPoint = Transform3D.rotate(mPoint, pivot, phi, theta, psi);
      }

      // See ITransform for JavaDoc
      @Override
      public void translate(double[] distance) {
         mPoint[0] += distance[0];
         mPoint[1] += distance[1];
         mPoint[2] += distance[2];
      }

      /**
       * Gets the current value assigned to normal to the plane
       * 
       * @return Returns the normal.
       */
      public double[] getNormal() {
         return mNormal.clone();
      }

      /**
       * Gets the current value assigned to the reference point located on the
       * plane.
       * 
       * @return Returns the point.
       */
      public double[] getPoint() {
         return mPoint.clone();
      }
   }

   /**
    * intersection - Returns the point at which three planes intersect. If the
    * planes don't intersect at a single point then the function returns null.
    * 
    * @param planes
    * @return A three-vector containing the point
    */
   static private double[] intersection(Plane[] planes) {
      assert (planes.length == 3);
      final double[] n0 = planes[0].mNormal, n1 = planes[1].mNormal, n2 = planes[2].mNormal;
      // The determinant of the matrix made from the vector normals
      final double det = ((n0[0] * ((n1[1] * n2[2]) - (n2[1] * n1[2])) //
      ) - (n1[0] * ((n0[1] * n2[2]) - (n2[1] * n0[2])) //
      )) + (n2[0] * ((n0[1] * n1[2]) - (n1[1] * n0[2])));
      if (Math.abs(det) > 1.0e-10)
         return Math2.divide(Math2.plus(
               Math2.plus(Math2.multiply(Math2.dot(planes[0].mPoint, n0), Math2.cross(n1, n2)),
                     Math2.multiply(Math2.dot(planes[1].mPoint, n1), Math2.cross(n2, n0))),
               Math2.multiply(Math2.dot(planes[2].mPoint, n2), Math2.cross(n0, n1))), det);
      else
         return null;
   }

   // Return a vector of length 1 parallel to vec
   private static double[] normalize(double[] vec) {
      final double[] res = new double[3];
      final double norm = Math.sqrt((vec[0] * vec[0]) + (vec[1] * vec[1]) + (vec[2] * vec[2]));
      res[0] = vec[0] / norm;
      res[1] = vec[1] / norm;
      res[2] = vec[2] / norm;
      return res;
   }

   // returns a vector pointing in the opposite direction of v
   private static double[] invert(double[] v) {
      final double[] res = new double[3];
      res[0] = -v[0];
      res[1] = -v[1];
      res[2] = -v[2];
      return res;
   }

   // Add a plane offset by dist*normal from pt
   private void addOffsetPlane(double[] normal, double[] pt, double dist) {
      normal = normalize(normal);
      addPlane(normal, new double[]{pt[0] + (normal[0] * dist), pt[1] + (normal[1] * dist), pt[2] + (normal[2] * dist)});
   }

   /**
    * MCSS_MultiPlane - Creates a MCSS_MultiPlane object. Use addPlane to define
    * the Shape.
    */
   public MultiPlaneShape() {
      super();
   }

   /**
    * createFilm - Construct a MCSS_MultiPlane object corresponding to a film.
    * Normal defines the orientation of the plane associated with pt1. A second
    * plane is constructed a distance thickness from the first plane.
    * 
    * @param normal
    *           double[]
    * @param pt1
    *           double[]
    * @param thickness
    *           double
    * @return MultiPlaneShape
    */
   public static MultiPlaneShape createFilm(double[] normal, double[] pt1, double thickness) {
      final MultiPlaneShape mp = new MultiPlaneShape();
      mp.addPlane(normal, pt1);
      mp.addOffsetPlane(invert(normal), pt1, thickness);
      return mp;
   }

   /**
    * createSubstrate - Construct a MCSS_MultiPlane object corresponding to an
    * infinitely thick layer.
    * 
    * @param normal
    *           double[]
    * @param pt
    *           double[]
    * @return MultiPlaneShape
    */
   public static MultiPlaneShape createSubstrate(double[] normal, double[] pt) {
      final MultiPlaneShape mp = new MultiPlaneShape();
      mp.addPlane(normal, pt);
      return mp;
   }

   /**
    * createBlock - Create a block of dimensions specified in dims, centered at
    * point then rotated by the euler angles phi, theta, psi. The rotation is a
    * rotation phi around the z-axis, followed by a rotation theta around the
    * y-axis and finally a rotation psi around the z-axis.
    * 
    * @param dims
    *           double[] - The unrotated dimensions (x,y,z axis)
    * @param point
    *           double[] - The location of the center of the block
    * @return MCSS_MultiPlaneShape
    */
   public static MultiPlaneShape createBlock(double[] dims, double[] point) {
      return createBlock(dims, point, 0.0, 0.0, 0.0);
   }

   /**
    * createBlock - Create a block of dimensions specified in dims, centered at
    * point then rotated by the euler angles phi, theta, psi. The rotation is a
    * rotation phi around the z-axis, followed by a rotation theta around the
    * y-axis and finally a rotation psi around the z-axis.
    * 
    * @param dims
    *           double[] - The unrotated dimensions (x,y,z axis)
    * @param point
    *           double[] - The location of the center of the block
    * @param phi
    *           double - rotation about the z-axis (radians)
    * @param theta
    *           double - rotation about the y-axis (radians)
    * @param psi
    *           double - rotation about the x-axis (radians)
    * @return MCSS_MultiPlaneShape
    */
   public static MultiPlaneShape createBlock(double[] dims, double[] point, double phi, double theta, double psi) {
      final double cphi = Math.cos(phi), sphi = Math.sin(phi);
      final double cpsi = Math.cos(psi), spsi = Math.sin(psi);
      final double cth = Math.cos(theta), sth = Math.sin(theta);
      // Rotated x, y and z-axis normals
      final double[][] normals = {{(cphi * cth * cpsi) - (sphi * spsi), (sphi * cpsi) + (cphi * cth * spsi), -cphi * sth},
            {(-sphi * cth * cpsi) - (cphi * spsi), (-sphi * cth * spsi) + (cphi * cpsi), sth * sphi}, {sth * cpsi, sth * spsi, cth}};

      final MultiPlaneShape mp = new MultiPlaneShape();

      for (int i = 0; i < 3; ++i) {
         final double[] normal = normals[i];
         mp.addPlane(normal, new double[]{point[0] + ((dims[i] * normal[0]) / 2.0), point[1] + ((dims[i] * normal[1]) / 2.0),
               point[2] + ((dims[i] * normal[2]) / 2.0)});
         mp.addPlane(invert(normal), new double[]{point[0] - ((dims[i] * normal[0]) / 2.0), point[1] - ((dims[i] * normal[1]) / 2.0),
               point[2] - ((dims[i] * normal[2]) / 2.0)});
      }
      return mp;
   }

   /**
    * Create a Tetrahedron (n=3), Pyramid (n=4), ..., etc with the specified
    * center, number of sides, height and base dimension. Use rotate(..) to
    * rotate the object into the desired orientation.
    * 
    * @param center
    *           Location of center double[3]
    * @param n
    *           Number of sides (excluding the base)
    * @param height
    *           Height of the object (&gt;0)
    * @param base
    *           Length of a side of the base
    * @return MultiPlaneShape
    */
   public static MultiPlaneShape createNamid(double[] center, int n, double height, double base) {
      assert height > 0 : "Height must be greater than zero.";
      assert base > 0 : "Base must be greater than zero.";
      final MultiPlaneShape mp = new MultiPlaneShape();
      mp.addPlane(Math2.Z_AXIS, Math2.ORIGIN_3D);
      final double theta = -Math.atan2(base / 2.0, height);
      for (int i = 0; i < n; ++i) {
         final double[] perp = Transform3D.rotate(Math2.X_AXIS, ((double) i / (double) n) * (2.0 * Math.PI), 0.0, 0.0);
         final double[] nn = Transform3D.rotate(Math2.X_AXIS, 0.0, -theta, ((double) i / (double) n) * (2.0 * Math.PI));
         mp.addPlane(nn, Math2.multiply(base / 2.0, perp));
      }
      mp.translate(Math2.multiply(-height, Math2.Z_AXIS));
      mp.translate(center);
      return mp;
   }

   public static MultiPlaneShape createSquarePyramid(double[] pinnacle, double base, double height) {
      assert height > 0 : "Height must be greater than zero.";
      assert base > 0 : "Base must be greater than zero.";
      final MultiPlaneShape mp = new MultiPlaneShape();
      mp.addPlane(Math2.v3(height, 0.0, -0.5 * base), pinnacle);
      mp.addPlane(Math2.v3(-height, 0.0, -0.5 * base), pinnacle);
      mp.addPlane(Math2.v3(0.0, height, -0.5 * base), pinnacle);
      mp.addPlane(Math2.v3(0.0, -height, -0.5 * base), pinnacle);
      mp.addPlane(Math2.Z_AXIS, Math2.plus(pinnacle, Math2.v3(0.0, 0.0, height)));
      return mp;
   }

   public static MultiPlaneShape createTriangularPrism(double[] pinnacle, double isosc, double length) {
      assert isosc > 0 : "Edge of isoscoles triangle must be greater than zero.";
      assert length > 0 : "Length must be greater than zero.";
      final MultiPlaneShape mp = new MultiPlaneShape();
      final double s3o2 = Math.sqrt(3) / 2;
      mp.addPlane(Math2.v3(s3o2 * isosc, 0.0, -0.5 * isosc), pinnacle);
      mp.addPlane(Math2.v3(-s3o2 * isosc, 0.0, -0.5 * isosc), pinnacle);
      mp.addPlane(Math2.Z_AXIS, Math2.plus(pinnacle, Math2.v3(0.0, 0.0, s3o2 * isosc)));
      mp.addPlane(Math2.Y_AXIS, Math2.multiply(0.5 * length, Math2.Y_AXIS));
      mp.addPlane(Math2.MINUS_Y_AXIS, Math2.multiply(-0.5 * length, Math2.Y_AXIS));
      return mp;
   }

   /**
    * addPlane - Add a new bounding plane to the Shape defined by this
    * MCSS_MultiPlane.
    * 
    * @param normal
    *           double[]
    * @param point
    *           double[]
    */
   public void addPlane(double[] normal, double[] point) {
      add(new Plane(normalize(normal), point));
   }

   /**
    * render - Renders this MultiPlaneShape as a series of triangular facets in
    * a VRML world. I've tested this against a handful of shapes and it seems to
    * work. The details are a little complex so I wouldn't be surprised if
    * someone found a problem or two.
    * 
    * @param rc
    *           The context into which to render this object
    * @param wr
    *           The Writer into which to write the results
    * @see gov.nist.microanalysis.NISTMonte.TrajectoryVRML.IRender#render(gov.nist.microanalysis.NISTMonte.TrajectoryVRML.RenderContext,
    *      java.io.Writer)
    */
   @Override
   public void render(TrajectoryVRML.RenderContext rc, Writer wr) throws IOException {
      // Configure the number format
      final NumberFormat nf = NumberFormat.getInstance(Locale.US);
      nf.setMaximumFractionDigits(3);
      nf.setGroupingUsed(false);
      // Configure the rendering color
      final Color color = rc.getCurrentColor();
      final String trStr = nf.format(rc.getTransparency());
      final String colorStr = nf.format(color.getRed() / 255.0) + " " + nf.format(color.getGreen() / 255.0) + " "
            + nf.format(color.getBlue() / 255.0);
      // Points are the intersection of three planes
      final Plane[] ps = new Plane[3];
      // For each plane find the points located on it...
      List<Shape> planes = getShapes();
      final int pSize = planes.size();
      for (int i = 0; i < pSize; ++i) {
         ps[0] = (Plane) planes.get(i);
         // An array of plane indexes int[2]
         final ArrayList<int[]> idxList = new ArrayList<int[]>();
         final HashMap<int[], double[]> ptMap = new HashMap<int[], double[]>();
         // For each plane find the other 2*n planes that define the
         for (int j = 0; j < pSize; ++j)
            if (i != j) {
               ps[1] = (Plane) planes.get(j);
               for (int k = j + 1; k < pSize; ++k)
                  if (i != k) {
                     ps[2] = (Plane) planes.get(k);
                     final double[] pt = intersection(ps);
                     if (pt != null) {
                        // Check to ensure that this point is inside
                        // all the other planes...
                        boolean inside = true;
                        for (int m = 0; inside && (m < pSize); m++)
                           if ((m != i) && (m != j) && (m != k)) {
                              final Plane p = (Plane) planes.get(m);
                              inside = p.almostContains(pt);
                           }
                        if (inside) {
                           boolean add = true;
                           for (final Iterator<double[]> m = ptMap.values().iterator(); add && m.hasNext();) {
                              final double[] inclPt = m.next();
                              final double d = Math2.distance(inclPt, pt);
                              add = (d > 1.0e-12);
                           }
                           if (add) {
                              final int[] idxItem = new int[]{j, k};
                              // This point forms one of the corners for this
                              // plane
                              idxList.add(idxItem);
                              ptMap.put(idxItem, pt);
                           }
                        }
                     }
                  }
            }
         if (ptMap.size() > 2) {
            // Put idxList in order such that we can step through them by
            // permuting only one index per step.
            for (int j = 0; j < idxList.size(); ++j) {
               final int[] iJ = idxList.get(j);
               for (int k = j + 1; k < idxList.size(); ++k) {
                  final int[] iK = idxList.get(k);
                  // Is the first or second index duplicated?
                  final boolean b0 = ((iJ[0] == iK[0]) || (iJ[0] == iK[1]));
                  final boolean b1 = ((iJ[1] == iK[0]) || (iJ[1] == iK[1]));
                  // Only permute one index
                  if (b0 ^ b1) {
                     // Swap iK into j+1
                     final int[] iJp1 = idxList.get(j + 1);
                     idxList.set(j + 1, iK);
                     idxList.set(k, iJp1);
                     break;
                  }
               }
            }
            wr.append("\n#Rendering side " + Integer.toString(i + 1) + " of " + Integer.toString(pSize));
            // Write each face separately (not solid)
            wr.append("\nShape {\n");
            wr.append(" geometry IndexedFaceSet {\n");
            wr.append("  coord Coordinate {\n");
            wr.append("   point [ ");
            for (int j = 0; j < idxList.size(); ++j) {
               if (j != 0)
                  wr.append(", ");
               final double[] pt = ptMap.get(idxList.get(j));
               wr.append(nf.format(pt[0] / TrajectoryVRML.SCALE));
               wr.append(" ");
               wr.append(nf.format(pt[1] / TrajectoryVRML.SCALE));
               wr.append(" ");
               wr.append(nf.format(pt[2] / TrajectoryVRML.SCALE));
            }
            wr.append(" ]\n");
            wr.append("  }\n");
            wr.append("  coordIndex [ ");
            for (int j = 0; j < idxList.size(); ++j) {
               if (j != 0)
                  wr.append(" ");
               wr.append(Integer.toString(j));
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
         }
      }
      wr.flush();
   }

   /**
    * Returns an immutable list of the Plane objects that define this
    * MultiPlaneShape
    * 
    * @return Returns the planes.
    */
   public List<Plane> getPlanes() {
      List<Plane> res = new ArrayList<Plane>();
      for (Shape sh : getShapes())
         res.add((Plane) sh);
      return Collections.unmodifiableList(res);
   }
}
