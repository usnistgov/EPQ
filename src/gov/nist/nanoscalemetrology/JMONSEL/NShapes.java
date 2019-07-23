/**
 *
 */
package gov.nist.nanoscalemetrology.JMONSEL;

/**
 * <p>
 * A class of static utility methods to simplify generation of some common
 * shapes. All shapes conform to the NormalShape interface.
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

/*
 * TODO There is probably a more elegant way to do this than to copy Nicholas's
 * routines and make NormalShape versions of them, but I don't have time to
 * figure it out right now.
 */
public class NShapes {

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
   private static void addOffsetPlane(NormalMultiPlaneShape shape, double[] normal, double[] pt, double dist) {
      normal = normalize(normal);
      shape.addPlane(normal, new double[] {
         pt[0] + (normal[0] * dist),
         pt[1] + (normal[1] * dist),
         pt[2] + (normal[2] * dist)
      });
   }

   /**
    * createNormalFilm - Construct a NormalMultiPlane object corresponding to a
    * film. Normal defines the orientation of the plane associated with pt1. A
    * second plane is constructed a distance thickness from the first plane.
    *
    * @param normal double[]
    * @param pt1 double[]
    * @param thickness double
    * @return MultiPlaneShape
    */
   public static NormalMultiPlaneShape createNormalFilm(double[] normal, double[] pt1, double thickness) {
      final NormalMultiPlaneShape mp = new NormalMultiPlaneShape();
      mp.addPlane(normal, pt1);
      addOffsetPlane(mp, invert(normal), pt1, thickness);
      return mp;
   }

   /**
    * createLine - Construct a line oriented along the y axis with the center of
    * its "bottom" face at the origin. The line is trapezoidal in cross section,
    * with its top face at a specified z value. Its bottom width, length,
    * sidewall angles, and top corner radii are all independently specified via
    * the input parameters. The line can be subsequently rotated and translated
    * if desired by using the rotate and translate functions. This routine uses
    * an algorithm that forms the line as the intersection of three pieces, a
    * right side (one or two planes + possible corner rounding), a similar left
    * side, and an enclosure (top, bottom, and end caps on the line).
    *
    * @param topz double -- z coordinate of "top" face
    * @param width double -- bottom width of the line.
    * @param length double -- length of the line
    * @param thetal double -- angle of the left sidewall in radians, expressed
    *           as a deviation from the vertical. Negative angles refer to
    *           undercut lines.
    * @param thetar double -- angle of right sidewall in radians
    * @param radl double -- radius of top left corner
    * @param radr double -- radius of top right corner *
    * @return MultiPlaneShape
    */
   public static NormalShape createLine(double topz, // z of the top face
         double width, // line width
         double length, // length of line
         double thetal, // angle of right sidewall
         double thetar, // angle of left sidewall
         double radl, // radius of top right corner
         double radr // radius of top left corner
   ) {
      // Parameter checks
      if(radr < 0.)
         radr = 0.;
      if(radl < 0.)
         radl = 0.;

      /*
       * The line will be the intersection of 3 pieces, a right side, a left
       * side, and an "enclosure". The right side is a multiplane shape with 2
       * planes, one representing the sidewall and the other a plane that joins
       * the top of the line to the place where the cylinder touches the
       * sidewall. The left side does the same for the other side. The enclosure
       * is a NormalMultiPlaneShape shape consisting of the top, bottom, and
       * endcaps. I replaced the previous createLine algorithm with this one on
       * 2/14/2013. The previous one formed the union of two cylinders,
       * representing the corner rounding, with one multiplane shape (8 planes)
       * to form the line. This worked fine for lines that were wide enough, but
       * when the corner rounding becomes large enough (e.g., in the extreme
       * when the cylinder diameter is greater than the linewidth) the cylinder
       * that rounds the right corner can protrude through the left sidewall.
       * This new algorithm eliminates that issue at the cost of being slightly
       * (less than 5%) slower. This is a price worth paying now that narrow
       * lines are much more frequently done.
       */

      /* First, construct the enclosure */
      final NormalMultiPlaneShape enclosure = new NormalMultiPlaneShape();
      // Add top plane
      double signz = Math.signum(topz);
      if(signz == 0.)
         signz = 1.; // For rare case of 0-height specification
      enclosure.addPlane(new double[] {
         0.,
         0.,
         signz
      }, new double[] {
         0.,
         0.,
         topz
      });
      // Add bottom plane
      enclosure.addPlane(new double[] {
         0.,
         0.,
         -signz
      }, new double[] {
         0.,
         0.,
         0.
      });
      // Add end caps
      enclosure.addPlane(new double[] { // Right end
         0.,
         1.,
         0.
      }, new double[] {
         0.,
         length / 2.,
         0.
      });
      enclosure.addPlane(new double[] { // Left end
         0.,
         -1.,
         0.
      }, new double[] {
         0.,
         -length / 2.,
         0.
      });

      /* Now do the right side */

      final NormalMultiPlaneShape rightNMPS = new NormalMultiPlaneShape();
      NormalShape rightSide;

      // Add right sidewall
      final double costhetar = Math.cos(thetar);
      final double sinthetar = Math.sin(thetar);
      rightNMPS.addPlane(new double[] {
         costhetar,
         0.,
         signz * sinthetar
      }, new double[] {
         width / 2,
         0.,
         0.
      });
      // If radr>0 add a clipping plane and the cylinder
      final double root2 = Math.sqrt(2.);
      final double absz = signz * topz;
      if(radr > 0) {
         final double rad = Math.sqrt(1 - sinthetar);
         rightNMPS.addPlane(new double[] {
            rad / root2,
            0.,
            (signz * costhetar) / root2 / rad
         }, new double[] {
            ((width / 2.) - (radr / costhetar)) + (((radr - absz) * sinthetar) / costhetar),
            0.,
            topz
         });
         // Construct cylinder for right corner
         final double xc = ((width / 2.) - (radr / Math.cos(thetar))) + ((radr - absz) * Math.tan(thetar));
         final double zc = topz - (signz * radr);
         final NormalCylindricalShape rcylinder = new NormalCylindricalShape(new double[] {
            xc,
            -length / 2.,
            zc
         }, new double[] {
            xc,
            length / 2.,
            zc
         }, radr);
         rightSide = new NormalUnionShape(rightNMPS, rcylinder);
      } else
         rightSide = rightNMPS;

      /* Now do likewise for the left side */
      final NormalMultiPlaneShape leftNMPS = new NormalMultiPlaneShape();
      NormalShape leftSide;

      // Add left sidewall
      final double costhetal = Math.cos(thetal);
      final double sinthetal = Math.sin(thetal);
      leftNMPS.addPlane(new double[] {
         -costhetal,
         0.,
         signz * sinthetal
      }, new double[] {
         -width / 2,
         0.,
         0.
      });
      // If radl>0 add a clipping plane and the cylinder
      if(radl > 0.) {
         final double rad = Math.sqrt(1 - sinthetal);
         leftNMPS.addPlane(new double[] {
            -rad / root2,
            0.,
            (signz * costhetal) / root2 / rad
         }, new double[] {
            ((-width / 2.) + (radl / costhetal)) - (((radl - absz) * sinthetal) / costhetal),
            0.,
            topz
         });
         final double xc = ((width / 2.) - (radl / Math.cos(thetal))) + ((radl - absz) * Math.tan(thetal));
         final double zc = topz - (signz * radl);
         // Construct cylinder for left corner
         final NormalCylindricalShape lcylinder = new NormalCylindricalShape(new double[] {
            -xc,
            -length / 2.,
            zc
         }, new double[] {
            -xc,
            length / 2.,
            zc
         }, radl);
         leftSide = new NormalUnionShape(leftNMPS, lcylinder);
      } else
         leftSide = leftNMPS;

      /* The shape is the intersection of the 3 shapes just constructed */

      return new NormalIntersectionShape(new NormalIntersectionShape(leftSide, rightSide), enclosure);
   }
}
