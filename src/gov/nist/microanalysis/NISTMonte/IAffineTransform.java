/**
 * gov.nist.nanoscalemetrology.JMONSEL.IAffineTransform Created by: Bin Ming,
 * John Villarrubia Date: June 01, 2011
 */
package gov.nist.microanalysis.NISTMonte;

import gov.nist.microanalysis.EPQLibrary.ITransform;
import Jama.Matrix;

/**
 * <p>
 * Implementations of IAffineTransform are shapes that allow Affine
 * transformations. The interface provides one method (setCustomAffineMatrix)
 * whereby users can specify a general affine transformation and several
 * convenience methods that allow specification of simpler frequently used
 * transformations.
 * </p>
 * <p>
 * An affine transform is a linear transformation followed by a translation. For
 * a point x in 3 dimensions (a 3-vector) this can be written in matrix notation
 * as x' = A.x + b where x' is the transformed x, A is a 3x3 matrix, and b a
 * 3-vector representing the translation.
 * </p>
 * <p>
 * In addition to rotations and translations (required by the ITransform
 * interface that this interface extends) shapes that conform to this interface
 * allow other affine transformations, e.g., scaling and shearing.
 * Geometrically, an affine transformation preserves: 1. The points which lie on
 * a line continue to be collinear after the transformation. 2. Ratios of
 * distances along a line, i.e., for distinct collinear points p1,p2,p3, the
 * ratio | p2 - p1 | / | p3 - p2 | is preserved.
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
public interface IAffineTransform
   extends ITransform {

   /**
    * The user specifies a general affine transformation by supplying a 3 x 4
    * matrix that represents the transform. An affine transform is a linear
    * transformation followed by a translation. For a point x in 3 dimensions (a
    * 3-vector) this can be written in matrix notation as x' = A.x + b where x'
    * is the transformed x, A is a 3x3 matrix, and b a 3-vector representing the
    * translation. If we append a 1 to each 3-vector, we can equivalently
    * represent this transformation as multiplication by a single 4 x 4 matrix
    * in which the upper left 3 x 3 submatrix is A, the upper right 3 x 1 column
    * is b, and the bottom row is [0, 0, 0, 1]. Such a matrix represents the the
    * most general form of an affine transform. With this method, the affine
    * matrix is supplied by the user, who is responsible for insuring that the
    * linear part (the 3x3 A matrix) is invertible.
    * 
    * @param m - a 3x4 matrix representing the upper 3 rows of the affine
    *           transformation matrix.
    */
   public void customAffineTransform(Matrix m);

   /**
    * Scales (stretches, contracts, and/or inverts) the shape along the 3
    * coordinate axes. This is, the point (x,y,z) transforms to
    * (alpha*x,beta*y,gamma*z). Scale factors with magnitude greater than 1
    * stretch the shape, less than 1 contract it. Negative scale factors invert
    * the shape.
    * 
    * @param alpha - scale factor for x axis
    * @param beta - scale factor for y axis
    * @param gamma - scale factor for z axis
    */
   public void scale(double alpha, double beta, double gamma);

   /**
    * Specifies a shear transformation of the base shape. The row index of the
    * shear value specifies the axis of the shear transformation, x (for shi =
    * 0), y (shi = 1), or z (shi = 2). The shear transformation adds some amount
    * of a second coordinate, specified by the shj (again 0, 1, or 2 for x, y,
    * or z). E.g., For shi = 0 and shj = 2 the transformation is x' = x +
    * shear*z where x is transformed because shi = 0, it is transformed by
    * adding an amount proportional to z (because shj = 2) and the
    * proportionality is given by the value of shear.
    * 
    * @param shi - row index for the shear value
    * @param shj - column index for the shear value
    * @param shear - the amount of shear
    */
   public void shear(int shi, int shj, double shear);

   /**
    * Reflects the shape along the specified axis.
    * 
    * @param axis - index of the axis along which to reflect, 0 for x, 1 for y,
    *           2 for z
    */
   public void reflect(int axis);

   /**
    * Rotates the shape through the Euler angles (phi,theta,psi), i.e. rotation
    * by phi around the Z axis (yaw), then rotation by theta around X axis
    * (roll), and then rotation by psi around Z axis (yaw). This is referred to
    * as the Euler 313 (or Z-X-Z) sequence, not to be confused with the Euler
    * 321 sequence. We assume that the positive rotation angle is
    * counterclockwise.
    * 
    * @param p0 - Coordinates of the center of rotation (the "pivot")
    * @param phi - rotation angle (radians) of first rotation about z
    * @param theta - rotation angle (radians) about x
    * @param psi - rotation angle (radians) of second rotation about z
    */
   public void rotateZXZ(double p0[], double phi, double theta, double psi);

   /**
    * Rotates the shape through the Euler angles (phi,theta,psi), i.e. rotation
    * by phi around the Z axis (yaw), then rotation by theta around Y axis
    * (pitch), and then rotation by psi around Z axis (yaw). This is the
    * rotation sequence in comformity with the ITranform interface, and will be
    * the default method called by various shape classes, unless specified
    * otherwise.
    * 
    * @param p0 - Coordinates of the center of rotation (the "pivot")
    * @param phi - rotation angle (radians) of first rotation about z
    * @param theta - rotation angle (radians) about y
    * @param psi - rotation angle (radians) of second rotation about z
    */
   public void rotateZYZ(double p0[], double phi, double theta, double psi);

   /**
    * Z-axis ("yaw") rotation about a pivot.
    * 
    * @param p0 - Coordinates of the center of rotation (the "pivot")
    * @param phi - angle (radians) of the rotation
    */
   public void rotateZ(double p0[], double phi);

   /**
    * X-axis ("roll") rotation about a pivot.
    * 
    * @param p0 - Coordinates of the center of rotation (the "pivot")
    * @param theta - angle (radians) of the rotation
    */
   public void rotateX(double p0[], double theta);

   /**
    * Y-axis ("pitch") rotation about a pivot.
    * 
    * @param p0 - Coordinates of the center of rotation (the "pivot")
    * @param theta - angle (radians) of the rotation
    */
   public void rotateY(double p0[], double theta);

}
