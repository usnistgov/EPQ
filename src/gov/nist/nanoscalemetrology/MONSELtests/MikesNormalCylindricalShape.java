/*
 * Mike Tsai wrote this for me as part of a project to improve the NormalShape
 * implementation. Here's a description of the idea, as I put it in an e-mail to
 * Nicholas Ritchie:
 */
/*
 * We could implement our primitive shapes (sphere, cylinder, multi-plane)
 * differently than we do now. Taking the cylinder class as an example, the
 * present code structure is [general cylinder implementation for any radius,
 * length, position, and orientation]. That is, we have a single module that
 * implements the cylinder in all its generality. Suppose instead we implemented
 * it as [prototypical cylinder: unit length and unit radius oriented along the
 * z axis and centered at the origin] followed by [shape transform that scales,
 * rotates, and translates]. Notice that to answer the kinds of questions we
 * need to answer for the Monte Carlo (containment, boundary intersection,
 * normal vector at intersection, etc.) we never need to actually apply the
 * transform to the object itself. Instead, the inverse transform can be applied
 * to a point or trajectory segment, thereby transforming it into the coordinate
 * system of our prototypical object. We answer the question with respect to
 * this transformed trajectory segment then transform the answer back to the
 * ordinary coordinate system.
 */
/*
 * This separation of labor has at least two advantages. 1) The primitive shape
 * class is now really simple. The methods that compute intersections,
 * containment, etc., do not need to do this computation in complete generality,
 * but only for a single standardized prototypical version of the object. This
 * should make it easier to add new primitive shapes and to keep old ones
 * debugged. The shape transform is the same for every primitive object--it only
 * needs to be written once. 2) The shape transform can easily include different
 * scale factors for different axes. This has the effect of preferentially
 * stretching or contracting the object in a particular direction. This means a
 * right circular cylinder, after transformation, can easily be made to have an
 * elliptical cross section. An implementation of "sphere" is also an
 * implementation of "pancake." This effectively enlarges the class of objects
 * that we can represent with any given primitive shape implementation.
 */
/*
 * I think Mike's version has some problems with it. For example, it doesn't
 * translate results back to the user's coordinates. I'm not sure whether he
 * checked carefully that the transforms are correct, and that the inverse
 * transforms really are inverses, etc. He ran out of time before he could check
 * the results. Nevertheless, I keep this around for now because I may be able
 * to make use of some of his work if I decide to complete the project.
 */
package gov.nist.nanoscalemetrology.MONSELtests;

import java.util.Random;

import gov.nist.microanalysis.NISTMonte.CylindricalShape;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.Transform3D;
import gov.nist.nanoscalemetrology.JMONSEL.NormalShape;

/**
 * Code under development. This is an unfinished code, probably with bugs. Use
 * is discouraged except for development or testing.
 * <p>
 * Extends the NISTMonte CylindricalShape class in order to implement the
 * NormalShape interface. This requires one additional method, to return the
 * vector normal to the sphere at the point of intersection.
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
 * @author Michael Tsai
 * @version 1.0
 */

@Deprecated
public class MikesNormalCylindricalShape
   extends
   CylindricalShape
   implements
   NormalShape {

   private final double[] translate = new double[3];
   private final double[] axis = new double[3];// Vector position of the 1st
   // end cap
   // center
   private double[] nv = new double[3];
   private double[] end0 = new double[3];

   private final double axisScale, xRadiusScale, yRadiusScale, rotation1, rotation2; // Square
   private double rotation3;

   // of
   // radius

   public MikesNormalCylindricalShape(double[] end0, double[] end1, double radius) {
      super(new double[] {
         0,
         0,
         -.5
      }, new double[] {
         0,
         0,
         .5
      }, 1);// MT062408 create a unit cylinder centered at the origin
      // MT062508 set of transformation factors
      translate[0] = (end1[0] + end0[0]) / 2;
      translate[1] = (end1[1] + end0[1]) / 2;
      translate[2] = (end1[2] + end0[2]) / 2; // translation distance
      axis[0] = end1[0] - end0[0];
      axis[1] = end1[1] - end0[1];
      axis[2] = end1[2] - end0[2];
      axisScale = Math.sqrt((axis[0] * axis[0]) + (axis[1] * axis[1]) + (axis[2] * axis[2])); // scale
      // factor
      // in
      // z-direction
      xRadiusScale = radius; // scale factor in x-direction
      yRadiusScale = radius; // scale factor in y-direction
      final double[] a1 = {
         end1[0] - end0[0],
         end1[1] - end0[1],
         0
      }; // cylinder's axis xy plane
      final double[] a2 = {
         0,
         end1[1] - end0[1],
         end1[2] - end0[2]
      }; // cylinder's axis yz plane
      final double[] b1 = {
         -1,
         0,
         0
      };
      final double[] b2 = {
         0,
         0,
         1
      };
      this.end0 = end0.clone();
      rotation1 = Math2.angleBetween(a1, b1); // angle to rotate around y-axis
      rotation2 = Math2.angleBetween(a2, b2); // angle to rotate around x-axis
      end0 = Transform3D.translate(Transform3D.rotate(end0, translate, rotation1, rotation2, rotation3), translate, true);
      end1 = Transform3D.translate(Transform3D.rotate(end1, translate, rotation1, rotation2, rotation3), translate, true);
   }// constructor

   @Override
   public boolean contains(double[] pos0, double[] pos1) {
      double[] p0 = Transform3D.translate(pos0, translate, true);
      p0 = Transform3D.rotate(p0, new double[] {
         0,
         0,
         0
      }, rotation1, rotation2, 0);
      p0[0] /= xRadiusScale;
      p0[1] /= yRadiusScale;
      p0[2] /= axisScale;
      double[] p1 = Transform3D.translate(pos1, translate, true);
      p1 = Transform3D.rotate(p1, new double[] {
         0,
         0,
         0
      }, rotation1, rotation2, 0);
      p1[0] /= xRadiusScale;
      p1[1] /= yRadiusScale;
      p1[2] /= axisScale;
      final double[] p0c = difference(p0, end0);
      final double p0csquared = dotProduct(p0c, p0c);
      final double p0cDotN = p0c[2];
      final double[] delta = difference(p1, p0);
      final double r2 = p0csquared - Math.pow(p0cDotN, 2);
      final Random rand = new Random(5);
      if(((p0cDotN == 0) || (p0cDotN == 1)) && (r2 <= 1)) {// trajectory on one
         // endcap
         final int i = rand.nextInt();
         if((i % 2) == 0)
            return true;
         else
            return false;
      } // if
      else if((p0cDotN >= 0) && (p0cDotN <= 1) && (r2 == 1)) {// trajectory on
         // cylinder body
         final double[] nv = {
            p0c[0],
            p0c[1],
            p0c[2] - p0cDotN
         };// outward pointing normal
         return dotProduct(nv, delta) < 0;
      } // else if
      return (r2 < 1) && (p0cDotN > 0) && (p0cDotN < 1);// pos0 is not on the
      // boundary; this is the
      // usual case
   }// contains

   /* Overrides getFirstIntersection to compute and cache the normal vector */
   @Override
   public double getFirstIntersection(double[] pos0, double[] pos1) {
      pos0 = Transform3D.translate(Transform3D.rotate(pos0, translate, rotation1, rotation2, rotation3), translate, true);
      pos1 = Transform3D.translate(Transform3D.rotate(pos1, translate, rotation1, rotation2, rotation3), translate, true);
      nv = getFirstNormal(pos0, pos1);
      return nv[2];
   }// getfirstintersection

   /**
    * @param pos0
    * @param pos1
    * @return double[]
    * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#getFirstNormal(double[],
    *      double[])
    */
   @Override
   public double[] getFirstNormal(double[] pos0, double[] pos1) {
      final double[] delta = difference(pos1, pos0); // electron trajectory
      // vector
      final double[] p0c = difference(pos0, end0);
      // double[] n = {0,0,1};
      final double[][] uxy = new double[4][3]; // 2D array stores corresponding
      // u,x,y
      // values in each row
      double temp;
      int c;
      /*
       * END CAP SOLUTIONS
       */
      if(delta[2] != 0) {
         uxy[0][1] = -((delta[0] + (2 * pos0[2] * delta[0])) - (2 * pos0[0] * delta[2])) / (2 * delta[2]);
         uxy[0][2] = -((delta[1] + (2 * pos0[2] * delta[1])) - (2 * pos0[1] * delta[2])) / (2 * delta[2]);
         if((Math.pow(uxy[0][1], 2) + Math.pow(uxy[0][2], 2)) <= 1)
            uxy[0][0] = -(1 + (2 * pos0[2])) / (2 * delta[2]);
         else
            uxy[0][0] = -1;
         uxy[1][1] = ((delta[0] - (2 * pos0[2] * delta[0])) + (2 * pos0[0] * delta[2])) / (2 * delta[2]);
         uxy[1][2] = ((delta[1] - (2 * pos0[2] * delta[1])) + (2 * pos0[1] * delta[2])) / (2 * delta[2]);
         if((Math.pow(uxy[1][1], 2) + Math.pow(uxy[1][2], 2)) <= 1)
            uxy[1][0] = (1 - (2 * pos0[2])) / (2 * delta[2]);
         else
            uxy[1][0] = -1;
      } // if
      /*
       * CYLINDER BODY SOLUTIONS
       */
      if(((((-Math.pow(pos0[1] * delta[0], 2) + (2 * pos0[0] * pos0[1] * delta[0] * delta[1]))
            - Math.pow(pos0[0] * delta[1], 2)) + Math.pow(delta[0], 2) + Math.pow(delta[1], 2)) >= 0) && (p0c[2] >= -.5)
            && (p0c[2] <= .5)) {
         uxy[2][0] = -((pos0[0] * delta[0]) + (pos0[1] * delta[1])
               + Math.sqrt(((-Math.pow(pos0[1] * delta[0], 2) + (2 * pos0[0] * pos0[1] * delta[0] * delta[1]))
                     - Math.pow(pos0[0] * delta[1], 2)) + Math.pow(delta[0], 2) + Math.pow(delta[1], 2)))
               / (Math.pow(delta[0], 2) + Math.pow(delta[1], 2));
         uxy[3][0] = -(((pos0[0] * delta[0]) - (pos0[1] * delta[1]))
               + Math.sqrt(((-Math.pow(pos0[1] * delta[0], 2) + (2 * pos0[0] * pos0[1] * delta[0] * delta[1]))
                     - Math.pow(pos0[0] * delta[1], 2)) + Math.pow(delta[0], 2) + Math.pow(delta[1], 2)))
               / (Math.pow(delta[0], 2) + Math.pow(delta[1], 2));
      } // if
      else {
         uxy[2][0] = -1;
         uxy[3][0] = -1;
      } // else
      /*
       * Calculate smallest 'u' There's probably a neater or more elegant way to
       * do this
       */
      temp = uxy[0][0];
      c = 0;
      if((uxy[1][0] < temp) && (uxy[1][0] != -1)) {
         temp = uxy[1][0];
         c = 1;
      } // if
      else if((uxy[2][0] < temp) && (uxy[2][0] != -1)) {
         temp = uxy[2][0];
         c = 2;
      } // else if
      else if((uxy[3][0] < temp) && (uxy[3][0] != -1)) {
         temp = uxy[3][0];
         c = 3;
      } // else if
      if(temp != -1)
         switch(c) {
            case 0:// end cap 1
               return new double[] {
                  0,
                  0,
                  1
               };
            case 1:// end cap 2
               return new double[] {
                  0,
                  0,
                  -1
               };
            case 2:// body 1
               return new double[] {
                  uxy[2][1],
                  uxy[2][2],
                  0
               };
            case 3:// body 2
               return new double[] {
                  uxy[3][1],
                  uxy[3][2],
                  0
               };
         }// switch
      return new double[] {
         0,
         0,
         0
      };// no intersection
   }// getFirstNormal

   private double[] difference(double[] a, double[] b) {
      final double[] temp = {
         a[0] - b[0],
         a[1] - b[1],
         a[2] - b[2]
      };
      return temp;
   }// difference between two vectors

   private double dotProduct(double[] a, double[] b) {
      final double temp = (a[0] * b[0]) + (a[1] * b[1]) + (a[2] * b[2]);
      return temp;
   }// dot product of two vectors

   @Override
   public double[] getPreviousNormal() {
      return nv;
   }
}
