/**
 * Class for performing an interpolation on the tabulated function y = f(x1,x2)
 * using a natural bicubic spline Assumes second derivatives at end points = 0
 * (natural spine) </p>
 * <p>
 * This version is based heavily upon the CubicSpline class of Dr. Michael
 * Thomas Flanagan, May 2002, updated 29 April 2005, 17 February 2006, 21
 * September 2006, with minor modifications by John Villarrubia.
 * </p>
 * <p>
 * Copyright: Flanagan's original code was accompanied by this copyright notice:
 * "Permission to use, copy and modify this software and its documentation for
 * NON-COMMERCIAL purposes is granted, without fee, provided that an
 * acknowledgement to the author, Michael Thomas Flanagan at
 * www.ee.ucl.ac.uk/~mflanaga, appears in all copies. Dr Michael Thomas Flanagan
 * makes no representations about the suitability or fitness of the software for
 * any or for a particular purpose. Michael Thomas Flanagan shall not be liable
 * for any damages suffered as a result of using, modifying or distributing this
 * software or its derivatives." Modifications by John Villarrubia, are,
 * pursuant to title 17 Section 105 of the United States Code, not subject to
 * copyright protection and are in the public domain.
 * </p>
 * 
 * @author Dr Michael Thomas Flanagan, modified by Dr John Villarrubia
 * @version 1.0
 */

// Modification history:
// 7/30/2007
package gov.nist.nanoscalemetrology.JMONSELutils;

public class BiCubicSpline {

	private int nPoints = 0; // no. of x1 tabulated points
	private int mPoints = 0; // no. of x2 tabulated points
	private double[][] y = null; // y=f(x1,x2) tabulated function
	private double[] x1 = null; // x1 in tabulated function f(x1,x2)
	private double[] x2 = null; // x2 in tabulated function f(x1,x2)
	private double[][] d2ydx2inner = null; // second derivatives of first called
	// array of cubic splines
	private CubicSpline csn[] = null; // nPoints array of CubicSpline instances
	private CubicSpline csm = null; // CubicSpline instance
	private boolean derivCalculated = false; // = true when the first called

	// cubic spline derivatives have
	// been calculated

	// Constructor
	// Constructor with data arrays initialised to arrays x and y
	public BiCubicSpline(double[] x1, double[] x2, double[][] y) {
		this.nPoints = x1.length;
		this.mPoints = x2.length;
		if (this.nPoints != y.length)
			throw new IllegalArgumentException(
					"Arrays x1 and y-row are of different length " + this.nPoints + " " + y.length);
		if (this.mPoints != y[0].length)
			throw new IllegalArgumentException(
					"Arrays x2 and y-column are of different length " + this.mPoints + " " + y[0].length);
		if ((this.nPoints < 3) || (this.mPoints < 3))
			throw new IllegalArgumentException("The data matrix must have a minimum size of 3 X 3");

		this.csm = new CubicSpline(this.nPoints);
		this.csn = CubicSpline.oneDarray(this.nPoints, this.mPoints);
		this.x1 = new double[this.nPoints];
		this.x2 = new double[this.mPoints];
		this.y = new double[this.nPoints][this.mPoints];
		this.d2ydx2inner = new double[this.nPoints][this.mPoints];
		for (int i = 0; i < this.nPoints; i++)
			this.x1[i] = x1[i];
		for (int j = 0; j < this.mPoints; j++)
			this.x2[j] = x2[j];
		for (int i = 0; i < this.nPoints; i++)
			for (int j = 0; j < this.mPoints; j++)
				this.y[i][j] = y[i][j];
	}

	// Constructor with data arrays initialised to zero
	// Primarily for use by TriCubicSpline
	public BiCubicSpline(int nP, int mP) {
		this.nPoints = nP;
		this.mPoints = mP;
		if ((this.nPoints < 3) || (this.mPoints < 3))
			throw new IllegalArgumentException("The data matrix must have a minimum size of 3 X 3");

		this.csm = new CubicSpline(this.nPoints);
		this.csn = CubicSpline.oneDarray(this.nPoints, this.mPoints);
		this.x1 = new double[this.nPoints];
		this.x2 = new double[this.mPoints];
		this.y = new double[this.nPoints][this.mPoints];
		this.d2ydx2inner = new double[this.nPoints][this.mPoints];
	}

	// METHODS

	// Resets the x1, x2, y data arrays
	// Primarily for use in TiCubicSpline
	public void resetData(double[] x1, double[] x2, double[][] y) {
		if (x1.length != y.length)
			throw new IllegalArgumentException("Arrays x1 and y row are of different length");
		if (x2.length != y[0].length)
			throw new IllegalArgumentException("Arrays x2 and y column are of different length");
		if (this.nPoints != x1.length)
			throw new IllegalArgumentException("Original array length not matched by new array length");
		if (this.mPoints != x2.length)
			throw new IllegalArgumentException("Original array length not matched by new array length");

		for (int i = 0; i < this.nPoints; i++)
			this.x1[i] = x1[i];

		for (int i = 0; i < this.mPoints; i++)
			this.x2[i] = x2[i];

		for (int i = 0; i < this.nPoints; i++)
			for (int j = 0; j < this.mPoints; j++)
				this.y[i][j] = y[i][j];
	}

	// Returns a new BiCubicSpline setting internal array size to nP x mP and all
	// array values to zero with natural spline default
	// Primarily for use in this.oneDarray for TiCubicSpline
	public static BiCubicSpline zero(int nP, int mP) {
		if ((nP < 3) || (mP < 3))
			throw new IllegalArgumentException("A minimum of three x three data points is needed");
		final BiCubicSpline aa = new BiCubicSpline(nP, mP);
		return aa;
	}

	// Create a one dimensional array of BiCubicSpline objects of length nP each
	// of internal array size mP x lP
	// Primarily for use in TriCubicSpline
	public static BiCubicSpline[] oneDarray(int nP, int mP, int lP) {
		if ((mP < 3) || (lP < 3))
			throw new IllegalArgumentException("A minimum of three x three data points is needed");
		final BiCubicSpline[] a = new BiCubicSpline[nP];
		for (int i = 0; i < nP; i++)
			a[i] = BiCubicSpline.zero(mP, lP);
		return a;
	}

	// Get inner matrix of derivatives
	// Primarily used by TriCubicSpline
	public double[][] getDeriv() {
		return this.d2ydx2inner;
	}

	// Set inner matrix of derivatives
	// Primarily used by TriCubicSpline
	public void setDeriv(double[][] d2ydx2) {
		this.d2ydx2inner = d2ydx2;
		this.derivCalculated = true;
	}

	// Returns an interpolated value of y for a value of x
	// from a tabulated function y=f(x1,x2)
	public double interpolate(double xx1, double xx2) {

		final double[] yTempn = new double[mPoints];

		for (int i = 0; i < this.nPoints; i++) {

			for (int j = 0; j < mPoints; j++)
				yTempn[j] = y[i][j];
			this.csn[i].resetData(x2, yTempn);
			if (!this.derivCalculated)
				this.csn[i].calcDeriv();
			this.d2ydx2inner[i] = this.csn[i].getDeriv();
		}
		this.derivCalculated = true;

		final double[] yTempm = new double[this.nPoints];

		for (int i = 0; i < this.nPoints; i++) {
			this.csn[i].setDeriv(this.d2ydx2inner[i]);
			yTempm[i] = this.csn[i].interpolate(xx2);
		}

		this.csm.resetData(x1, yTempm);
		return this.csm.interpolate(xx1);
	}
}
