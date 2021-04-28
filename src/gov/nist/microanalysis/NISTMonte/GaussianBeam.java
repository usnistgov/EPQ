package gov.nist.microanalysis.NISTMonte;

import gov.nist.microanalysis.NISTMonte.MonteCarloSS.ElectronGun;
import gov.nist.microanalysis.Utility.Math2;

import java.util.Random;

/**
 * <p>
 * Implements the ElectronBeam interface for a Gaussian electron beam.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nritchie
 * @version 1.0
 */
public class GaussianBeam implements ElectronGun {
	// transient private Random mRandom = new Random();
	private final transient Random mRandom = Math2.rgen;

	private double[] mCenter = Math2.multiply(0.99 * MonteCarloSS.ChamberRadius, Math2.MINUS_Z_AXIS);

	private double mBeamEnergy;

	private double mWidth = 1.0e-9;
	private final double minBeamWidth = 5.e-12;

	/**
	 * GaussianBeam - Create a instance of the ElectronGun interface modeling a
	 * GaussianBeam with the specified Gaussian width parameter.
	 * 
	 * @param width double - The Gaussian width parameter in meters.
	 */
	public GaussianBeam(double width) {
		if (width < minBeamWidth)
			mWidth = minBeamWidth;
		else
			mWidth = width;
	}

	/**
	 * setWidth - Set the width of the Gaussian beam.
	 * </p>
	 * <p>
	 * To avoid numerical issues at sharp corners, there is a minimum beam width of
	 * 5.e-12 m. Widths specified smaller than this are replaced by this minimum
	 * value.
	 * 
	 * @param width double
	 */
	public void setWidth(double width) {
		if (width < minBeamWidth)
			mWidth = minBeamWidth;
		else
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

	@Override
	public Electron createElectron() {
		final double[] initialPos = mCenter.clone();
		final double r = Math.sqrt(-2. * Math.log(mRandom.nextDouble())) * mWidth;
		final double th = 2.0 * Math.PI * mRandom.nextDouble();
		initialPos[0] += r * Math.cos(th);
		initialPos[1] += r * Math.sin(th);
		return new Electron(initialPos, mBeamEnergy);
	}
}