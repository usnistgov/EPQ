package gov.nist.microanalysis.NISTMonte;

import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * This class handles the geometric considerations tracking an electron through
 * Monte Carlo steps. Storing the geometric considerations for each electron
 * independently may facilitate adding support for fast secondary electrons if
 * desired.
 * </p>
 * <p>
 * The direction of the electron is defined using spherical polar coordinates.
 * Theta is the polar angle and phi is the azimuthal angle. The direction theta
 * = 0, phi = 0 is the z-axis. Theta is a rotation about the y-axis measured
 * from the z-axis towards the x-axis. Phi is a subsequent rotation about the
 * z-axis from the x-axis towards the y-axis. All directions can be described
 * using theta in [0,Pi] and phi in [0, 2Pi).
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie, John Villarrubia
 * @version 1.0
 */
final public class Electron {

	// The x,y & z coordinates of the electron's position when it was generated
	private final transient double[] mPosition0;

	// The x,y & z coordinates of the electron
	private final transient double[] mPosition;

	// The location of the electron before the last call to updatePosition
	private final transient double[] mPrevPosition;

	// The direction of the current trajectory segment
	transient private double mPhi, mTheta;

	// The kinetic energy of the electron
	transient private double mEnergy;

	// Kinetic energy of the electron upon conclusion of the previous step
	transient private double mPreviousEnergy;

	transient private int mStepCount;

	transient private RegionBase mCurrentRegion;

	transient private RegionBase mPrevRegion;

	transient private Element mScatteringElement;

	transient private boolean mTrajectoryComplete;

	private static long lastID = 0; // ID of last generated electron
	private final long ident; // A unique identifying number to assist tracking
	private long parentID = 0; // 0 if from e-gun. Otherwise ID of parent.

	/**
	 * Construct an electron with the specified initial position and kinetic energy.
	 * The initial direction is along the z-axis (theta=0, phi=0)
	 * 
	 * @param initialPos double[]
	 * @param kE         double - Electron kinetic energy in Joules
	 */
	public Electron(double[] initialPos, double kE) {
		this(initialPos, 0., 0., kE);
	}

	/**
	 * Constructs a Electron at the initial position specified with motion in the
	 * direction specified by phi &amp; theta and a kinetic energy of kE. Phi is the
	 * angle measured from the z-axis and theta is the angle measured from x-axis.
	 * 
	 * @param initialPos
	 * @param theta
	 * @param phi
	 * @param kE
	 */
	public Electron(double[] initialPos, double theta, double phi, double kE) {
		super();
		mPosition0 = initialPos.clone();
		mPosition = initialPos.clone();
		mPrevPosition = initialPos.clone();
		mScatteringElement = null;
		mCurrentRegion = null;
		mPrevRegion = null;
		mEnergy = kE;
		mPreviousEnergy = kE;
		mTheta = theta;
		mPhi = phi;
		mStepCount = 0;
		mTrajectoryComplete = false;
		ident = ++lastID;
	}

	/**
	 * Constructs an Electron that starts its trajectory from the location of the
	 * specified parent electron with motion in the direction specified by phi &amp;
	 * theta and a kinetic energy of kE. Phi is the angle measured from the z-axis
	 * and theta is the angle measured from x-axis.
	 * 
	 * @param parent
	 * @param phi
	 * @param theta
	 * @param kE
	 */
	public Electron(Electron parent, double theta, double phi, double kE) {
		this(parent.getPosition(), theta, phi, kE);
		mCurrentRegion = parent.getCurrentRegion();
		mPrevRegion = mCurrentRegion;
		parentID = parent.getIdent();
	}

	/**
	 * Permits changing the current direction of the electron to any direction as
	 * defined by the provided spherical polar coordinates.
	 * 
	 * @param theta double - In radians
	 * @param phi   double - In radians
	 */
	public void setDirection(double theta, double phi) {
		mTheta = theta;
		mPhi = phi;
	}

	/**
	 * getPosition0 - Get the initial position of the electron (position when it was
	 * generated either in the electron gun or as a secondary electron) as a array
	 * of three doubles (x,y &amp; z)
	 * 
	 * @return double[]
	 */
	public double[] getPosition0() {
		return mPosition0;
	}

	/**
	 * getPosition - Get the current position of the electron as a array of three
	 * doubles (x,y &amp; z)
	 * 
	 * @return double[]
	 */
	public double[] getPosition() {
		return mPosition;
	}

	/**
	 * A unit vector in the direction of propagation.
	 *
	 * @return double[]
	 */
	public double[] getDirection() {
		return Math2.normalize(Math2.minus(mPosition, mPrevPosition));
	}

	/**
	 * setPosition - Set the current position of the electron as a array of three
	 * doubles (x,y &amp; z). The setter simply resets the position, unlike move(),
	 * which keeps a record of the electron's current position accessible through
	 * getPrevPosition(). The previous position is meant to maintain a record of the
	 * electron's position at the beginning of each trajectory leg. move() should
	 * therefore be used when such a record is needed (as for example for a normal
	 * electron trajectory step) and setPosition() should be used when it is
	 * explicitly desired to NOT overwrite the stored previous position (as for
	 * example when adjusting an electron position a tiny distance off of a boundary
	 * to avoid round-off ambiguities).
	 * 
	 * @param newpos double[] - An array of 3 values specifying new x, y, z
	 */
	public void setPosition(double[] newpos) {
		for (int i = 0; i < 3; i++)
			mPosition[i] = newpos[i];
	}

	/**
	 * getPrevPosition - Get the previous position of the electron as a array of
	 * three doubles (x,y &amp; z)
	 * 
	 * @return double[]
	 */
	public double[] getPrevPosition() {
		return mPrevPosition;
	}

	/**
	 * Returns the RegionBase in which the electron is currently located. null if
	 * the current region is unknown.
	 */
	public RegionBase getCurrentRegion() {
		return mCurrentRegion;
	}

	/**
	 * Returns the RegionBase in which the electron was located at the previous
	 * position (getPreviousPosition). null if the previous region is unknown.
	 */
	public RegionBase getPreviousRegion() {
		return mPrevRegion;
	}

	/**
	 * Returns the current kinetic energy of the electon in Joules.
	 * 
	 * @return double
	 */
	public double getEnergy() {
		return mEnergy;
	}

	/**
	 * Returns the previous kinetic energy of the electon.
	 * 
	 * @return double
	 */
	public double getPreviousEnergy() {
		return mPreviousEnergy;
	}

	/**
	 * Returns the number of steps since the trajectory began.
	 * 
	 * @return int
	 */
	public int getStepCount() {
		return mStepCount;
	}

	/**
	 * Gets the length of the step between the start of the step and the end of the
	 * step. This varies as the electron energy varies and as the material changes.
	 * 
	 * @return double
	 */
	public double stepLength() {
		return MonteCarloSS.distance(mPrevPosition, mPosition);
	}

	/**
	 * Computes the location of the next point assuming that the electron's current
	 * trajectory is deviated by angles alpha and beta and the step length is dS. It
	 * is called a candidate point because the point is not necessarily in the same
	 * material as the current point. If the candidate point is not in the same
	 * material then the actual step will be foreshortened at the interface between
	 * the two materials.. (The foreshortening is handled by the RegionBase class.)
	 * 
	 * @param dS double
	 * @return double []
	 */
	public double[] candidatePoint(double dS) {
		final double st = Math.sin(mTheta);
		// Calculate the new point as dS distance from mPosition
		return new double[] { mPosition[0] + (dS * Math.cos(mPhi) * st), mPosition[1] + (dS * Math.sin(mPhi) * st),
				mPosition[2] + (dS * Math.cos(mTheta)) };
	}

	/**
	 * Updates the electron trajectory angles, given the deflection angles dTheta,
	 * dPhi from its current direction.
	 * 
	 * @param dTheta double - The deflection polar angle (0 = no deflection)
	 * @param dPhi   double - The deflection azimuthal angle
	 */
	public void updateDirection(double dTheta, double dPhi) {
		// The candidate point is computed by rotating the current trajectory back
		// to the z-axis, deflecting the z-axis by dTheta down from the z-axis and
		// dPhi around the z-axis, then finally rotating back to the original
		// trajectory.
		final double ct = Math.cos(mTheta), st = Math.sin(mTheta);
		final double cp = Math.cos(mPhi), sp = Math.sin(mPhi);
		final double ca = Math.cos(dTheta), sa = Math.sin(dTheta);
		final double cb = Math.cos(dPhi);

		final double xx = (cb * ct * sa) + (ca * st);
		final double yy = sa * Math.sin(dPhi);
		final double dx = (cp * xx) - (sp * yy);
		final double dy = (cp * yy) + (sp * xx);
		final double dz = (ca * ct) - (cb * sa * st);

		mTheta = Math.atan2(Math.sqrt((dx * dx) + (dy * dy)), dz);
		mPhi = Math.atan2(dy, dx);
	}

	/**
	 * Update the position and energy of this electron. The new position should be
	 * the point returned by <code>candidatePoint(...)</code> unless the trajectory
	 * takes the electron between materials. In this case, the new point is the
	 * location of the interface between the two materials. The dE depends upon the
	 * material and the step length. The electron's initial kinetic energy is copied
	 * to previousEnergy, and then its kinetic energy is incremented by the amount
	 * dE. (dE&lt;0 is the typical energy loss situation.)
	 * 
	 * @param newPoint double[]
	 * @param dE       double - in Joules
	 */
	public void move(double[] newPoint, double dE) {
		// Update mPrevPosition and then mPosition
		System.arraycopy(mPosition, 0, mPrevPosition, 0, 3);
		System.arraycopy(newPoint, 0, mPosition, 0, 3);

		// Update the energy
		mPreviousEnergy = mEnergy;
		mEnergy += dE;
		++mStepCount;
	}

	/**
	 * Sets the current kinetic energy of the electron in Joules. This call does not
	 * set the previous Energy before making the change.
	 */
	public void setEnergy(double newEnergy) {
		mEnergy = newEnergy;
	}

	/**
	 * Sets the previous kinetic energy of the electon in Joules.
	 */
	public void setPreviousEnergy(double newPreviousEnergy) {
		mPreviousEnergy = newPreviousEnergy;
	}

	/**
	 * Records the current RegionBase in which the electron is located. Also
	 * remembers the overwritten RegionBase as the previous region.
	 */
	public void setCurrentRegion(RegionBase reg) {
		mPrevRegion = mCurrentRegion;
		mCurrentRegion = reg;
	}

	/**
	 * Returns the last element off of which this electron scattered.
	 * 
	 * @return Returns the scatteringElement.
	 */
	public Element getScatteringElement() {
		return mScatteringElement;
	}

	/**
	 * Records the last element off of which this electron scattered.
	 * 
	 * @param scatteringElement The value to which to set scatteringElement.
	 */
	public void setScatteringElement(Element scatteringElement) {
		mScatteringElement = scatteringElement;
	}

	/**
	 * Returns the angle phi as defined in the class documentation.
	 * 
	 * @return double
	 */
	public double getPhi() {
		return mPhi;
	}

	/**
	 * Returns the angle theta as defined in the class documentation.
	 * 
	 * @return double
	 */
	public double getTheta() {
		return mTheta;
	}

	/**
	 * Gets the current value assigned to trajectoryComplete
	 * 
	 * @return Returns the trajectoryComplete.
	 */
	public boolean isTrajectoryComplete() {
		return mTrajectoryComplete;
	}

	/**
	 * Sets the value assigned to trajectoryComplete.
	 * 
	 * @param trajectoryComplete The value to which to set trajectoryComplete.
	 */
	public void setTrajectoryComplete(boolean trajectoryComplete) {
		mTrajectoryComplete = trajectoryComplete;
	}

	/**
	 * @return Returns a number unique to this electron, used as an identifier.
	 */
	public long getIdent() {
		return ident;
	}

	/**
	 * @return Returns the identifier of the most recently created electron.
	 */
	static public long getlastIdent() {
		return lastID;
	}

	/**
	 * The parentID is 0 if this electron was created by an electron gun. If it is a
	 * secondary electron, the parentID is the identifier of the parent electron.
	 * 
	 * @return Returns the parentID.
	 */
	public long getParentID() {
		return parentID;
	}
}