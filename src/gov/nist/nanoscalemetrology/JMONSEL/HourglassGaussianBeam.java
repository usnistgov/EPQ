/**
 * gov.nist.nanoscalemetrology.JMONSEL.HourglassGaussianBeam Created by: John
 * Villarrubia, Date: March 26, 2012
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.util.Random;

import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.ElectronGun;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Implements the ElectronGun interface for an electron gun that produces a
 * hourglass-shaped beam in a user-specified direction with a Gaussian
 * distribution of electrons at best focus. "Gaussian" here means that at best
 * focus the probability that an electron's position will be a distance between
 * r and r + dr from the center of the spot is r * Exp[-r^2/(2 sigma^2)] * dr.
 * Hourglass-shaped means the electrons converge from above best-focus and
 * diverge below it.
 * </p>
 * <p>
 * The axis of symmetry of the beam (average direction of electrons) can be
 * specified by the setBeamDirection() method. The default is [0.,0.,1.]. (Note
 * that, like NISTMonte's GaussianBeam upon which this one is modeled, the beam
 * travels by default up the z axis. This facilitates use of the ElectronGun
 * with the same detectors used for GaussianBeam.) The beam center, set in the
 * constructor or by setCenter(), is the position of best focus. Electrons pass
 * through a plane perpendicular to the beam direction at this center point in a
 * 2-d Gaussian distribution. The standard deviation is set by the width
 * parameter in the constructor or setWidth(). The electron directions are
 * uniformly distributed in solid angle with a maximum deviation of
 * angularAperture (default 0.) from the mean beam direction. The initial
 * electron position is offset a distance (default 1 cm) from the best focus
 * position. From there, the electrons converge to the focus plane and then
 * diverge beyond it. (This allows parts of the sample above the best focus
 * position to realistically intercept incoming electrons.)
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */

public class HourglassGaussianBeam
   implements
   ElectronGun {
   // transient private Random mRandom = new Random();
   private final transient Random mRandom = Math2.rgen;

   private double[] mCenter; // best focus position
   private double mBeamEnergy; // beam energy
   private double mWidth; // standard deviation at best focus
   private double[] beamDirection = {
      0.,
      0.,
      1.
   };
   /* beamDirection in polar coordinates */
   private double meanTheta = 0.;
   private double meanPhi = 0.;
   /*
    * Other normal vectors to complete our orthonormal coordinate system. There
    * are infinitely many choices we could make. The strange ones below are for
    * consistency with the simple algorithm implemented at setBeamDirection().
    */
   double[] x = {
      0.,
      1.,
      0.
   };
   double[] y = {
      -1.,
      0.,
      0.
   };

   private double angularAperture = 0.;
   private double cosAperture = 1.;
   private double offset = 0.01;

   /**
    * GaussianBeam - Create a instance of the ElectronGun interface modeling a
    * GaussianBeam with the specified Gaussian width parameter. The beam's
    * minimum size occurs at the coordinates given by the center parameter.
    *
    * @param width double - The Gaussian width parameter in meters.
    * @param center double[] - The coordinates of best focus.
    */
   public HourglassGaussianBeam(double width, double[] center) {
      mWidth = width;
      mCenter = center.clone();
   }

   /**
    * setWidth - Set the width of the Gaussian beam. Definition of width: this
    * width is the same as the sigma parameter in the above expression for the
    * probability. Among other things, this means that if we were to imagine a
    * scatter plot of landing positions in the plane of best focus, the x and y
    * coordinates (axes in this plane) of those landing positions would each
    * have standard deviation equal to the supplied width. The root mean square
    * radius, sqrt(x^2+y^2), then necessarily is sqrt(2)*width. There is another
    * convention that defines "beam diameter" as that diameter which contains
    * 56% of the beam current. Let's call this d56. Then width = 0.3902*d56.
    *
    * @param width double
    */
   public void setWidth(double width) {
      mWidth = width;
   }

   /**
    * getWidth - Returns the width of the Gaussian beam.
    *
    * @return double
    */
   public double getWidth() {
      return mWidth;
   }

   @Override
   public void setBeamEnergy(double beamEnergy) {
      mBeamEnergy = beamEnergy;
   }

   @Override
   public double getBeamEnergy() {
      return mBeamEnergy;
   }

   @Override
   public void setCenter(double[] center) {
      mCenter = center.clone();
   }

   @Override
   public double[] getCenter() {
      return mCenter.clone();
   }

   /**
    * Gets the current value assigned to beamDirection
    *
    * @return Returns the beamDirection.
    */
   public double[] getBeamDirection() {
      return beamDirection;
   }

   /**
    * Sets the value assigned to beamDirection.
    *
    * @param bD -- The value to which to set beamDirection.
    */
   public void setBeamDirection(double[] bD) {
      beamDirection = Math2.normalize(bD);
      meanTheta = Math.acos(beamDirection[2]);
      meanPhi = Math.atan2(beamDirection[1], beamDirection[0]);
      /* Find two more orthogonal directions */
      int minindex = 0;
      double min = Math.abs(beamDirection[0]);
      for(int i = 1; i < 3; i++) {
         final double newval = Math.abs(beamDirection[i]);
         if(newval < min) {
            min = newval;
            minindex = i;
         }
      }
      final double[] temp = {
         0.,
         0.,
         0.
      };
      temp[minindex] = 1.;
      x = Math2.normalize(Math2.cross(beamDirection, temp));
      y = Math2.cross(beamDirection, x);
   }

   /**
    * Gets the current value (in radians) assigned to angularAperture
    *
    * @return Returns the angularAperture.
    */
   public double getAngularAperture() {
      return angularAperture;
   }

   /**
    * Sets the value assigned to angularAperture. This angle is the maximum
    * amount by which an electron's direction can differ from the average beam
    * direction. I.e., it is the half angle of the cones that comprise the
    * hourglass that describes the beam shape.
    *
    * @param angularAperture The value, in radians, to which to set
    *           angularAperture.
    */
   public void setAngularAperture(double angularAperture) {
      this.angularAperture = angularAperture;
      cosAperture = Math.cos(angularAperture);
   }

   /**
    * Gets the current value assigned to offset
    *
    * @return Returns the offset.
    */
   public double getOffset() {
      return offset;
   }

   /**
    * Sets the value assigned to offset. This is the distance, in meters, that
    * electrons are offset from the best focus position (the beam center).
    * Ordinarily one makes this large enough so that electrons start well clear
    * of the sample, even in cases where the best focus position lies beyond the
    * sample position nearest the electron gun. Positive values of offset cause
    * the electrons to converge for this distance before reaching best focus,
    * then diverge from there. Setting offset = 0 causes the electrons to start
    * in the best focus plane. Negative values cause the electrons to start at a
    * position already beyond best focus. The default value is 0.01 (1 cm).
    *
    * @param offset The value to which to set offset.
    */
   public void setOffset(double offset) {
      if(this.offset != offset)
         this.offset = offset;
   }

   @Override
   public Electron createElectron() {
      // Get direction of this electron relative to mean direction.
      final double r = mRandom.nextDouble();
      final double costheta = (1. - r) + (r * cosAperture);
      final double theta = Math.acos(costheta);
      final double phi = 2. * Math.PI * mRandom.nextDouble();

      // Position of this electron at best focus
      double[] pos = mCenter.clone();
      // double rad = mRandom.nextGaussian() * mWidth;
      final double rad = Math.sqrt(-2. * Math.log(mRandom.nextDouble())) * mWidth;
      final double th = 2.0 * Math.PI * mRandom.nextDouble();
      pos = Math2.plus(Math2.plus(pos, Math2.multiply(rad * Math.cos(th), x)), Math2.multiply(rad * Math.sin(th), y));
      /*
       * Create the electron at best focus position and direction = mean beam
       * direction
       */
      final Electron electron = new Electron(pos, meanTheta, meanPhi, mBeamEnergy);
      /* Deflect it to account for this electron's deviation from the mean */
      electron.updateDirection(theta, phi);
      /* Offset its position with no energy change */
      electron.setPosition(electron.candidatePoint(-offset));
      return electron;
   }
}