/**
 * Class for performing an interpolation on the tabulated function y =
 * f(x1,x2,x3,x4) using a natural quadricubic spline Assumes second derivatives
 * at end points = 0 (natural spine) </p>
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
 * @author Dr. Michael Thomas Flanagan, modified by Dr. John Villarrubia
 * @version 1.0
 */

// Modification history:
// 7/30/2007
package gov.nist.nanoscalemetrology.JMONSELutils;

public class QuadriCubicSpline {

	private int nPoints = 0; // no. of x1 tabulated points
	private int mPoints = 0; // no. of x2 tabulated points
	private int lPoints = 0; // no. of x3 tabulated points
	private int kPoints = 0; // no. of x4 tabulated points

	private double[][][][] y = null; // y=f(x1,x2,x3,x4) tabulated function
	private double[] x1 = null; // x1 in tabulated function f(x1,x2,x3,x4)
	private double[] x2 = null; // x2 in tabulated function f(x1,x2,x3,x4)
	private double[] x3 = null; // x3 in tabulated function f(x1,x2,x3,x4)
	private double[] x4 = null; // x4 in tabulated function f(x1,x2,x3,x4)
	private TriCubicSpline[] tcsn = null; // nPoints array of TriCubicSpline
	// instances
	private CubicSpline csm = null; // CubicSpline instance
	private double[][][][] d2ydx2 = null; // inner matrix of second derivatives
	private boolean derivCalculated = false; // = true when the called triicubic

	// spline derivatives have been
	// calculated

	// Constructor
	public QuadriCubicSpline(double[] x1, double[] x2, double[] x3, double[] x4, double[][][][] y) {
		this.nPoints = x1.length;
		this.mPoints = x2.length;
		this.lPoints = x3.length;
		this.kPoints = x4.length;
		if (this.nPoints != y.length)
			throw new IllegalArgumentException(
					"Arrays x1 and y-row are of different length " + this.nPoints + " " + y.length);
		if (this.mPoints != y[0].length)
			throw new IllegalArgumentException(
					"Arrays x2 and y-column are of different length " + this.mPoints + " " + y[0].length);
		if (this.lPoints != y[0][0].length)
			throw new IllegalArgumentException(
					"Arrays x3 and y-column are of different length " + this.mPoints + " " + y[0][0].length);
		if (this.kPoints != y[0][0][0].length)
			throw new IllegalArgumentException(
					"Arrays x4 and y-column are of different length " + this.kPoints + " " + y[0][0][0].length);
		if ((this.nPoints < 3) || (this.mPoints < 3) || (this.lPoints < 3) || (this.kPoints < 3))
			throw new IllegalArgumentException("The tabulated 4D array must have a minimum size of 3 X 3 X 3 X 3");

		this.csm = new CubicSpline(this.nPoints);
		this.tcsn = TriCubicSpline.oneDarray(this.nPoints, this.mPoints, this.lPoints, this.kPoints);
		this.x1 = new double[this.nPoints];
		this.x2 = new double[this.mPoints];
		this.x3 = new double[this.lPoints];
		this.x4 = new double[this.kPoints];

		this.y = new double[this.nPoints][this.mPoints][this.lPoints][this.kPoints];
		this.d2ydx2 = new double[this.nPoints][this.mPoints][this.lPoints][this.kPoints];
		for (int i = 0; i < this.nPoints; i++)
			this.x1[i] = x1[i];
		for (int j = 0; j < this.mPoints; j++)
			this.x2[j] = x2[j];
		for (int j = 0; j < this.lPoints; j++)
			this.x3[j] = x3[j];
		for (int j = 0; j < this.kPoints; j++)
			this.x4[j] = x4[j];
		for (int i = 0; i < this.nPoints; i++)
			for (int j = 0; j < this.mPoints; j++)
				for (int k = 0; k < this.lPoints; k++)
					for (int l = 0; l < this.kPoints; l++)
						this.y[i][j][k][l] = y[i][j][k][l];
	}

	// METHODS

	// Returns an interpolated value of y for values of x1, x2, x3 and x4
	// from a tabulated function y=f(x1,x2,x3,x4)
	public double interpolate(double xx1, double xx2, double xx3, double xx4) {

		final double[][][] yTempml = new double[this.mPoints][this.lPoints][this.kPoints];
		for (int i = 0; i < this.nPoints; i++) {
			for (int j = 0; j < this.mPoints; j++)
				for (int k = 0; k < this.lPoints; k++)
					for (int l = 0; l < this.kPoints; l++)
						yTempml[j][k][l] = y[i][j][k][l];
			this.tcsn[i].resetData(x2, x3, x4, yTempml);
		}
		final double[] yTempm = new double[nPoints];

		for (int i = 0; i < nPoints; i++) {
			if (this.derivCalculated)
				this.tcsn[i].setDeriv(d2ydx2[i]);
			yTempm[i] = this.tcsn[i].interpolate(xx2, xx3, xx4);
			if (!this.derivCalculated)
				d2ydx2[i] = this.tcsn[i].getDeriv();
		}
		derivCalculated = true;

		this.csm.resetData(x1, yTempm);
		return this.csm.interpolate(xx1);
	}

	// Return second derivatives
	public double[][][][] getDeriv() {
		return this.d2ydx2;
	}
}
