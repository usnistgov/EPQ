package gov.nist.microanalysis.Utility;

import Jama.Matrix;

/**
 * <p>
 * A set of static methods for rotating and translating 3D vectors and points by
 * Euler angles.
 * </p>
 * <p>
 * The rotation matrix is...
 * </p>
 * <table cellspacing=10 summary="">
 * <tr>
 * <td>[</td>
 * <td>cos(phi)*cos(th)*cos(psi)-sin(phi)*sin(psi)</td>
 * <td>-sin(phi)*cos(th)*cos(psi)-cos(phi)*sin(psi)</td>
 * <td>sin(th)*cos(psi)</td>
 * <td>]</td>
 * <tr>
 * <tr>
 * <td>[</td>
 * <td>sin(phi)*cos(psi)+cos(phi)*cos(th)*sin(psi)</td>
 * <td>-sin(phi)*cos(th)*sin(psi)+cos(phi)*cos(psi)</td>
 * <td>sin(th)*sin(psi)</td>
 * <td>]</td>
 * </tr>
 * <tr>
 * <td>[</td>
 * <td>-cos(phi)*sin(th)</td>
 * <td>sin(th)*sin(phi)</td>
 * <td>cos(th)</td>
 * <td>]</td>
 * </tr>
 * </table>
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

public class Transform3D {

   private static Matrix rotation(double phi, double th, double psi) {
      final Matrix r = new Matrix(3, 3);
      if(phi != 0.0) {
         final double cTh = Math.cos(th);
         final double sTh = Math.sin(th);
         final double cPhi = Math.cos(phi);
         final double sPhi = Math.sin(phi);
         final double cPsi = Math.cos(psi);
         final double sPsi = Math.sin(psi);
         r.set(0, 0, (cPhi * cTh * cPsi) - (sPhi * sPsi));
         r.set(0, 1, (-sPhi * cTh * cPsi) - (cPhi * sPsi));
         r.set(0, 2, sTh * cPsi);
         r.set(1, 0, (sPhi * cPsi) + (cPhi * cTh * sPsi));
         r.set(1, 1, (-sPhi * cTh * sPsi) + (cPhi * cPsi));
         r.set(1, 2, sTh * sPsi);
         r.set(2, 0, -cPhi * sTh);
         r.set(2, 1, sTh * sPhi);
         r.set(2, 2, cTh);
      } else {
         // Optimize the common special case phi=0.0
         final double cTh = Math.cos(th);
         final double sTh = Math.sin(th);
         final double cPsi = Math.cos(psi);
         final double sPsi = Math.sin(psi);
         r.set(0, 0, cTh * cPsi);
         r.set(0, 1, -sPsi);
         r.set(0, 2, sTh * cPsi);
         r.set(1, 0, cTh * sPsi);
         r.set(1, 1, cPsi);
         r.set(1, 2, sTh * sPsi);
         r.set(2, 0, -sTh);
         r.set(2, 1, 0.0);
         r.set(2, 2, cTh);
      }
      return r;
   }

   /**
    * rotate - Rotate the specified directional vector by the Euler angles phi,
    * th, psi. Rotate the object by phi about the z-axis followed by theta round
    * the y-axis followed by psi around the z-axis.
    * 
    * @param vector double[]
    * @param phi double
    * @param th double
    * @param psi double
    * @return double[]
    */
   public static double[] rotate(double[] vector, double phi, double th, double psi) {
      final Matrix m = new Matrix(vector, 3);
      final Matrix res = rotation(phi, th, psi).times(m);
      return new double[] {
         res.get(0, 0),
         res.get(1, 0),
         res.get(2, 0)
      };
   }

   /**
    * rotate - Rotate the specified point about the center point by the Euler
    * angles specified. Rotate the object around the specified point by phi
    * about the z-axis followed by theta round the y-axis followed by psi around
    * the z-axis.
    * 
    * @param point double[] - The point to rotate
    * @param pivot double[] - The pivot point
    * @param phi double - Initial rotation about z
    * @param theta double - Second rotation about y
    * @param psi double - Final rotation about z
    * @return double[] - The result
    */
   public static double[] rotate(double[] point, double[] pivot, double phi, double theta, double psi) {
      return translate(rotate(translate(point, pivot, true), phi, theta, psi), pivot, false);
   }

   /**
    * translate - Translate the specified point by the specified distance.
    * 
    * @param point double[] - The original point
    * @param distance double[] - The offset
    * @param negate - true to translate -distance or false to translate
    *           +distance
    * @return double[] - A new point translated by the specified amount.
    */
   public static double[] translate(double[] point, double[] distance, boolean negate) {
      final double[] res = new double[3];
      if(negate) {
         res[0] = point[0] - distance[0];
         res[1] = point[1] - distance[1];
         res[2] = point[2] - distance[2];
      } else {
         res[0] = point[0] + distance[0];
         res[1] = point[1] + distance[1];
         res[2] = point[2] + distance[2];
      }
      return res;
   }
}
