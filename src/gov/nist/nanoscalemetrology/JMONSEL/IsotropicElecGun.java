/**
 * gov.nist.nanoscalemetrology.JMONSEL.IsotropicElecGun Created by: jvillar
 * Date: Nov 7, 2008
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.util.Random;

import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.ElectronGun;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * An electron gun that generates electrons of specified energy and location
 * with trajectories randomly and isotropically oriented. The anticipated use
 * for such a gun is to place a source of electrons inside a sample, for example
 * to test escape probabilities of randomly directed electrons.
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
public class IsotropicElecGun
   implements ElectronGun {

   private final transient Random mRandom = Math2.rgen;

   private final double[] center = new double[] {
      0.,
      0.,
      0.
   };

   private double beamEnergy;

   /**
    * Constructs an IsotropicElecGun
    *
    * @param beamEnergy - the energy of the electrons in Joules
    * @param center - an array of length 3 with x, y, and z coordinates where
    *           the electrons are to originate.
    */
   public IsotropicElecGun(double beamEnergy, double[] center) {
      this.beamEnergy = beamEnergy;
      this.center[0] = center[0];
      this.center[1] = center[1];
      this.center[2] = center[2];
   }

   @Override
   public Electron createElectron() {
      final double theta = Math.acos(1. - (2. * mRandom.nextDouble()));
      final double phi = mRandom.nextDouble() * 2. * Math.PI;
      return new Electron(center, theta, phi, beamEnergy);
   }

   @Override
   public double getBeamEnergy() {
      return beamEnergy;
   }

   @Override
   public double[] getCenter() {
      return center.clone();
   }

   @Override
   public void setBeamEnergy(double beamEnergy) {
      this.beamEnergy = beamEnergy;
   }

   @Override
   public void setCenter(double[] center) {
      this.center[0] = center[0];
      this.center[1] = center[1];
      this.center[2] = center[2];
   }

}
