/**
 * gov.nist.nanoscalemetrology.JMONSEL.AffinizedNormalShape Created by: Bin
 * Ming, John Villarrubia Date: June 01, 2011
 */
package gov.nist.microanalysis.NISTMonte;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape;
import gov.nist.microanalysis.Utility.Math2;
import Jama.Matrix;

/**
 * <p>
 * This class converts a base shape (the constructor input, which must be a
 * Shape) into a shape that conforms to the IAffineTransform interface. That is,
 * it provides affine transforms (rotation, translation, scaling, etc.) for any
 * Shape provided as input. Geometrically, an affine transformation preserves:
 * 1. The points which lie on a line continue to be collinear after the
 * transformation. 2. Ratios of distances along a line; i.e., for distinct
 * collinear points p1,p2,p3, the ratio | p2 - p1 | / | p3 - p2 | is preserved.
 * Transformations are cumulative.
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
 * @version 1.0
 */
public class AffinizedShape
   implements IAffineTransform, Shape {

   private final Shape baseShape;
   // a 4*4 affine transformation matrx
   private Matrix affine = Matrix.identity(4, 4);
   protected Matrix inverseAffine = Matrix.identity(4, 4);

   /**
    * Constructs an AffinizedShape. Initially, this shape is equivalent to the
    * supplied baseShape. (It is constructed with its affine transformation
    * matrix equal to the identity matrix.) It may be subsequently transformed
    * using one or more of the provided affine transform methods.
    * 
    * @param baseShape -
    */
   public AffinizedShape(Shape baseShape) {
      this.baseShape = baseShape;
   }

   /**
    * Private utility to transform a vector by the given matrix. The routine
    * computes matrix * vector with * understood as the linear algebra matrix
    * multiplication and vector understood to be a column vector. This is
    * essentially a matrix multiplication routine, but it takes the vector and
    * returns its results as double[] instead of Matrix, thereby sparing the
    * user from making the necessary transformations to and from matrix form.
    * 
    * @param matrix
    * @param vector
    * @return double[]
    */
   private double[] matrixTimesVector(Matrix matrix, double[] vector) {
      final Matrix pVector = new Matrix(new double[] {
         vector[0],
         vector[1],
         vector[2],
         1.
      }, 4);
      final Matrix t = matrix.times(pVector);
      return new double[] {
         t.get(0, 0),
         t.get(1, 0),
         t.get(2, 0)
      };
   }

   /**
    * Private utility to transform a vector from real coordinates to the
    * baseShape's coordinate system.
    * 
    * @param p
    * @return double[]
    */
   protected double[] toBaseCoordinate(double[] p) {
      return matrixTimesVector(inverseAffine, p);
   }

   @Override
   public boolean contains(double[] pos) {
      return baseShape.contains(toBaseCoordinate(pos));
   }

   @Override
   public double getFirstIntersection(double[] pos0, double[] pos1) {
      return baseShape.getFirstIntersection(toBaseCoordinate(pos0), toBaseCoordinate(pos1));
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ITransform#rotate(double[], double,
    *      double, double)
    */
   @Override
   public void rotate(double[] pivot, double phi, double theta, double psi) {
      assert pivot.length == 3;
      final Matrix newRotation = Matrix.identity(4, 4);

      double cos_phi, sin_phi, cos_theta, sin_theta, cos_psi, sin_psi;
      cos_phi = Math.cos(phi);
      sin_phi = Math.sin(phi);
      cos_theta = Math.cos(theta);
      sin_theta = Math.sin(theta);
      cos_psi = Math.cos(psi);
      sin_psi = Math.sin(psi);

      newRotation.set(0, 0, (cos_theta * cos_phi * cos_psi) - (sin_phi * sin_psi));
      newRotation.set(0, 1, (-cos_theta * cos_psi * sin_phi) - (cos_phi * sin_psi));
      newRotation.set(1, 0, (cos_theta * cos_phi * sin_psi) + (sin_phi * cos_psi));
      newRotation.set(1, 1, (-cos_theta * sin_psi * sin_phi) + (cos_phi * cos_psi));
      newRotation.set(0, 2, cos_psi * sin_theta);
      newRotation.set(1, 2, sin_psi * sin_theta);
      newRotation.set(2, 0, -cos_phi * sin_theta);
      newRotation.set(2, 1, sin_phi * sin_theta);
      newRotation.set(2, 2, cos_theta);

      if((pivot[0] == 0.) && (pivot[1] == 0.) && (pivot[2] == 0.)) {
         affine = newRotation.times(affine);
         inverseAffine = affine.inverse();
      } else {
         translate(Math2.negative(pivot));
         affine = newRotation.times(affine);
         translate(pivot);
      }
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ITransform#translate(double[])
    */
   @Override
   public void translate(double[] distance) {
      assert distance.length == 3;
      final Matrix tran = Matrix.identity(4, 4);
      tran.set(0, 3, distance[0]);
      tran.set(1, 3, distance[1]);
      tran.set(2, 3, distance[2]);
      affine = tran.times(affine);
      inverseAffine = affine.inverse();
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.IAffineTransform#customAffineTransform(Jama.Matrix)
    */
   @Override
   public void customAffineTransform(Matrix m) {
      // the user supplied matrix must be 3 * 4 in size
      final Matrix newTransform = Matrix.identity(4, 4);
      if((m.getRowDimension() == 3) && (m.getColumnDimension() == 4)) {
         newTransform.setMatrix(0, 2, 0, 3, m);
         affine = newTransform.times(affine);
         try {
            inverseAffine = affine.inverse();
         }
         catch(final Exception e) {
            throw new EPQFatalException("Error inverting matrix: " + e.getMessage());
         }
      } else
         throw new EPQFatalException("customAffineTransform(m) called with incorrectly dimensioned argument ("
               + m.getRowDimension() + "," + m.getColumnDimension() + ")");
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.IAffineTransform#scale(double,
    *      double, double)
    */
   @Override
   public void scale(double sx, double sy, double sz) {
      final Matrix scaleTransform = Matrix.identity(4, 4);

      scaleTransform.set(0, 0, sx);
      scaleTransform.set(1, 1, sy);
      scaleTransform.set(2, 2, sz);

      affine = scaleTransform.times(affine);
      inverseAffine = affine.inverse();
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.IAffineTransform#shear(int, int,
    *      double)
    */
   @Override
   public void shear(int shi, int shj, double shear) {
      if((shi > -1) && (shi < 3) && (shj > -1) && (shj < 3) && (shi != shj)) {
         final Matrix newShear = Matrix.identity(4, 4);
         newShear.set(shi, shj, shear);
         affine = newShear.times(affine);
         inverseAffine = affine.inverse();
      } else
         throw new EPQFatalException("shear() called with index out of bounds: Indices = " + shi + "," + shj);
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.IAffineTransform#reflect(int)
    */
   @Override
   public void reflect(int axis) {
      // axis may assume the values of 0 (reflection by X), 1 (reflection by Y)
      // or 2 (reflection by Z)
      switch(axis) {
         case 0:
            scale(-1., 1., 1.);
            break;
         case 1:
            scale(1., -1., 1.);
            break;
         case 2:
            scale(1., 1., -1.);
            break;
         default:
            throw new EPQFatalException("reflect() called with axis out of bounds: Axis = " + axis);
      }
   }

   @Override
   public void rotateZXZ(double pivot[], double phi, double theta, double psi) {
      assert pivot.length == 3;
      final Matrix newRotation = Matrix.identity(4, 4);

      double cos_phi, sin_phi, cos_theta, sin_theta, cos_psi, sin_psi;
      cos_phi = Math.cos(phi);
      sin_phi = Math.sin(phi);
      cos_theta = Math.cos(theta);
      sin_theta = Math.sin(theta);
      cos_psi = Math.cos(psi);
      sin_psi = Math.sin(psi);

      newRotation.set(0, 0, (cos_psi * cos_phi) - (cos_theta * sin_phi * sin_psi));
      newRotation.set(0, 1, (cos_psi * sin_phi) + (cos_theta * cos_phi * sin_psi));
      newRotation.set(0, 2, sin_psi * sin_theta);
      newRotation.set(1, 0, (-sin_psi * cos_phi) - (cos_theta * sin_phi * cos_psi));
      newRotation.set(1, 1, (-sin_psi * sin_phi) + (cos_theta * cos_phi * cos_psi));
      newRotation.set(1, 2, cos_psi * sin_theta);
      newRotation.set(2, 0, sin_theta * sin_phi);
      newRotation.set(2, 1, -sin_theta * cos_phi);
      newRotation.set(2, 2, cos_theta);

      if((pivot[0] == 0.) && (pivot[1] == 0.) && (pivot[2] == 0.)) {
         affine = newRotation.times(affine);
         inverseAffine = affine.inverse();
      } else {
         translate(Math2.negative(pivot));
         affine = newRotation.times(affine);
         translate(pivot);
      }
   }

   @Override
   public void rotateZYZ(double pivot[], double phi, double theta, double psi) {
      rotate(pivot, phi, theta, psi);
   }

   // Rotation around the Z-axis ("yaw")
   /**
    * @param pivot
    * @param phi
    * @see gov.nist.microanalysis.NISTMonte.IAffineTransform#rotateZ(double[],
    *      double)
    */
   @Override
   public void rotateZ(double[] pivot, double phi) {
      final Matrix rotZ = Matrix.identity(4, 4);
      final double cos = Math.cos(phi);
      final double sin = Math.sin(phi);
      rotZ.set(0, 0, cos);
      rotZ.set(0, 1, -sin);
      rotZ.set(1, 0, sin);
      rotZ.set(1, 1, cos);

      if((pivot[0] == 0.) && (pivot[1] == 0.) && (pivot[2] == 0.)) {
         affine = rotZ.times(affine);
         inverseAffine = affine.inverse();
      } else {
         translate(Math2.negative(pivot));
         affine = rotZ.times(affine);
         translate(pivot);
      }
   }

   // Rotation around the X-axis ("roll")
   /**
    * @param pivot
    * @param theta
    * @see gov.nist.microanalysis.NISTMonte.IAffineTransform#rotateX(double[],
    *      double)
    */
   @Override
   public void rotateX(double[] pivot, double theta) {
      final Matrix rotX = Matrix.identity(4, 4);
      final double cos = Math.cos(theta);
      final double sin = Math.sin(theta);
      rotX.set(1, 1, cos);
      rotX.set(1, 2, -sin);
      rotX.set(2, 1, sin);
      rotX.set(2, 2, cos);

      if((pivot[0] == 0.) && (pivot[1] == 0.) && (pivot[2] == 0.)) {
         affine = rotX.times(affine);
         inverseAffine = affine.inverse();
      } else {
         translate(Math2.negative(pivot));
         affine = rotX.times(affine);
         translate(pivot);
      }
   }

   // Rotation around the Y-axis ("pitch")
   /**
    * @param pivot
    * @param theta
    * @see gov.nist.microanalysis.NISTMonte.IAffineTransform#rotateY(double[],
    *      double)
    */
   @Override
   public void rotateY(double[] pivot, double theta) {
      final Matrix rotY = Matrix.identity(4, 4);
      final double cos = Math.cos(theta);
      final double sin = Math.sin(theta);
      rotY.set(0, 0, cos);
      rotY.set(2, 0, -sin);
      rotY.set(0, 2, sin);
      rotY.set(2, 2, cos);

      if((pivot[0] == 0.) && (pivot[1] == 0.) && (pivot[2] == 0.)) {
         affine = rotY.times(affine);
         inverseAffine = affine.inverse();
      } else {
         translate(Math2.negative(pivot));
         affine = rotY.times(affine);
         translate(pivot);
      }
   }

   @Override
   public String toString() {
      return "AffinizedShape[" + baseShape + "]";
   }

}
