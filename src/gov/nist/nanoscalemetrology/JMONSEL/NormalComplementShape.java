/**
 *
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.ITransform;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape;

/**
 * <p>
 * Implements the NormalShape interface for the complement of a shape. If A
 * contains p then NormalComplementShape(A) does not and vice versa. In addition
 * to reversing the sense of containment, the complement has the same boundaries
 * as the original shape but the outward pointing normal vectors have their
 * directions reversed.
 * </p>
 * <p>
 * If A is finite in extent, its complement will be infinite. For this reason
 * care should be exercised in the use of this function in applications that
 * require bounded shapes (for example, if one is constructing a shape to define
 * a wholly contained subregion of a finite shape). Nevertheless, the complement
 * is useful because it can be combined with union or intersection in useful
 * ways. For example, Intersection(A,B) =
 * Complement(Union(Complement(A),Complement(B))). A-B =
 * Intersection(A,Complement(B))
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
public class NormalComplementShape implements Shape, ITransform, NormalShape {

   private final NormalShape shapeA;

   private double[] nv = null; // Most recent normal vector

   /**
    * Construct a NormalComplementShape that is the complement of the input
    * shape.
    *
    * @param shapeA
    *           - (NormalShape) The shape from which to form the complement.
    */
   public NormalComplementShape(NormalShape shapeA) {
      this.shapeA = shapeA;
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#contains(double[])
    */
   @Override
   public boolean contains(double[] pos) {
      return !shapeA.contains(pos);
   }

   /**
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#getFirstIntersection(double[],
    *      double[])
    */
   @Override
   public double getFirstIntersection(double[] pos0, double[] pos1) {
      nv = getFirstNormal(pos0, pos1);
      return nv[3];
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ITransform#rotate(double[], double,
    *      double, double)
    */
   @Override
   public void rotate(double[] pivot, double phi, double theta, double psi) {
      if (!(shapeA instanceof ITransform))
         throw new EPQFatalException(shapeA.toString() + " does not support transformation.");
      ((ITransform) shapeA).rotate(pivot, phi, theta, psi);

   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ITransform#translate(double[])
    */
   @Override
   public void translate(double[] distance) {
      if (!(shapeA instanceof ITransform))
         throw new EPQFatalException(shapeA.toString() + " does not support transformation.");
      ((ITransform) shapeA).translate(distance);

   }

   /**
    * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#getFirstNormal(double[],
    *      double[])
    */
   @Override
   public double[] getFirstNormal(double[] pos0, double[] pos1) {
      nv = shapeA.getFirstNormal(pos0, pos1); // Get normal vector of
      // shapeA
      nv[0] *= -1.; // Reverse direction of normal vector
      nv[1] *= -1.;
      nv[2] *= -1.;
      return nv;
   }

   /**
    * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#contains(double[],
    *      double[])
    */
   @Override
   public boolean contains(double[] pos0, double[] pos1) {
      return !shapeA.contains(pos0, pos1);
   }

   @Override
   public double[] getPreviousNormal() {
      return nv;
   }

}
