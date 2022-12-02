package gov.nist.microanalysis.NISTMonte;

import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.ElectronGun;
import gov.nist.microanalysis.Utility.Math2;

import java.util.Random;

/**
 * OverscanElectronGun implements the ElectronGun interface in a manner that
 * returns a electron within the specified rectangular region. Electrons are
 * selected at random from within the rectangular area. By running enough
 * electrons the result is similar (equivalent to?) rastering the beam over the
 * area.
 * 
 * @author nicholas
 */
public class OverscanElectronGun implements ElectronGun {
   private final double mXDim;
   private final double mYDim;
   private final double mRotation;
   private final double[] mCenter;
   private final Random mRandom = Math2.rgen;
   private double mBeamEnergy;

   /**
    * Create an OverscanElectronGun for a rectangle with <code>xDim</code>,
    * <code>yDim</code> and rotated to angle <code>rot</code>.
    * 
    * @param xDim
    *           double (in meters)
    * @param yDim
    *           double (in meters)
    * @param rot
    *           double (Angle in radians)
    */
   public OverscanElectronGun(double xDim, double yDim, double rot) {
      mXDim = xDim;
      mYDim = yDim;
      mRotation = rot;
      mCenter = new double[]{0.0, 0.0, -0.099};
      mBeamEnergy = ToSI.keV(20.0);
   }

   /**
    * Create an OverscanElectronGun for a rectangle with <code>xDim</code>,
    * <code>yDim</code>.
    * 
    * @param xDim
    *           double (in meters)
    * @param yDim
    *           double (in meters)
    */
   public OverscanElectronGun(double xDim, double yDim) {
      this(xDim, yDim, 0.0);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * gov.nist.microanalysis.NISTMonte.MonteCarloSS.ElectronGun#createElectron()
    */
   @Override
   public Electron createElectron() {
      final double x = mXDim * (0.5 - mRandom.nextDouble());
      final double y = mYDim * (0.5 - mRandom.nextDouble());
      final double[] initialPos = new double[]{((x * Math.cos(mRotation)) - (y * Math.sin(mRotation))) + mCenter[0],
            ((x * Math.sin(mRotation)) + (y * Math.cos(mRotation))) + mCenter[1], mCenter[2]};
      return new Electron(initialPos, mBeamEnergy);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * gov.nist.microanalysis.NISTMonte.MonteCarloSS.ElectronGun#getBeamEnergy()
    */
   @Override
   public double getBeamEnergy() {
      return mBeamEnergy;
   }

   /*
    * (non-Javadoc)
    * 
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.ElectronGun#getCenter()
    */
   @Override
   public double[] getCenter() {
      return mCenter.clone();
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * gov.nist.microanalysis.NISTMonte.MonteCarloSS.ElectronGun#setBeamEnergy
    * (double)
    */
   @Override
   public void setBeamEnergy(double beamEnergy) {
      mBeamEnergy = beamEnergy;
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * gov.nist.microanalysis.NISTMonte.MonteCarloSS.ElectronGun#setCenter(double
    * [])
    */
   @Override
   public void setCenter(double[] center) {
      for (int i = 0; i < mCenter.length; ++i)
         mCenter[i] = center[i];
   }
}
