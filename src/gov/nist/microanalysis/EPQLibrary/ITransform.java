package gov.nist.microanalysis.EPQLibrary;

/**
 * <p>
 * An interface for providing mechanisms for objects to rotate and translate in
 * 3-space.
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

public interface ITransform {

   /**
    * rotate - Rotate the object around the specified point by phi about the
    * z-axis followed by theta round the y-axis followed by psi around the
    * z-axis. These is the standard Euler angle rotation. The rotation matrix
    * is...
    * <table>
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
    * 
    * @param pivot double[] - a three-vector specifying the point around which
    *           the rotation is performed
    * @param phi double
    * @param theta double
    * @param psi double
    */
   void rotate(double[] pivot, double phi, double theta, double psi);

   /**
    * translate - Translate this object by the distance specified.
    * 
    * @param distance double[] - A three-vector specifying how much to translate
    */
   void translate(double[] distance);
}
