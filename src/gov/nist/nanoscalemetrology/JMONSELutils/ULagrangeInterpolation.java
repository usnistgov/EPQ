/**
 * 
 */
package gov.nist.nanoscalemetrology.JMONSELutils;

/**
 * <p>
 * Class of static methods for Lagrange (polynomial) interpolation of a function
 * f(x[]) given a table of function values on a uniform (equally spaced)
 * discrete grid of input values. The methods are based upon ideas from
 * Numerical Recipes in C, with adaptations for uniformity of the grid,
 * language, and object-oriented approach.
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
public class ULagrangeInterpolation {

	/**
	 * d1 - Performs interpolation of the 1-d function f(x). The values of the
	 * function on the grid are provided in a double[] (1-d array).
	 * 
	 * @param f     - double[] 1D array of function values at the grid points
	 * @param x0    - double The value of x corresponding to f[0]
	 * @param xinc  - double The grid spacing in x (difference between x[i] and
	 *              x[i-1])
	 * @param order - int The order of the interpolation (1 for linear, 3 for cubic,
	 *              etc.).
	 * @param x     - double The x value at which the function value is to be
	 *              estimated.
	 * @return double[] - An array of 2 values, the first of which is the estimate
	 *         for f[x] and the second is an error estimate.
	 */
	public static double[] d1(double[] f, double x0, double xinc, int order, double x) {
		/*
		 * The interpolation is performed by a call to neville, a separate static
		 * method.
		 */
		if (xinc == 0.)
			throw new IllegalArgumentException("Interval spacing must be nonzero.");
		if ((order < 1) || (f.length < (order + 1)))
			throw new IllegalArgumentException("0 < order <= table.length-1 is required.");
		/*
		 * The reduced coordinate is like an index into the array, but it can take on
		 * fractional values to indicate positions between the grid points.
		 */
		final double reducedx = (x - x0) / xinc;
		/*
		 * index0 is the index of the 1st grid point we'll include in the interpolation.
		 * Ideally, we'd like reducedx close to the middle of the set of grid points we
		 * choose. If reducedx is close to the beginning or end of the array, this might
		 * not be possible.
		 */
		int index0 = (int) reducedx - (order / 2);
		if (index0 < 0)
			index0 = 0;
		else if (index0 > (f.length - order - 1))
			index0 = f.length - order - 1;
		return uNeville(f, index0, order, reducedx - index0);
	}

	/**
	 * uNeville -- A private utility that performs the actual 1-d interpolation,
	 * using Neville's algorithm adapted to a uniform grid.
	 * 
	 * @param f      - double[] 1-D array of function values at the grid points
	 * @param offset - int The index of the first gridpoint to be included in the
	 *               interpolation
	 * @param order  - int The order of the interpolation (1 for linear, 3 for
	 *               cubic, etc.).
	 * @param x      - double The x value at which the function value is to be
	 *               estimated, in coordinates such that f[offset] corresponds to
	 *               x=0 f[offset+1] to x=1, etc.
	 * @return double[] - An array of 2 values, the first of which is the estimate
	 *         for f[x] and the second is an error estimate.
	 */
	private static double[] uNeville(double[] f, int offset, int order, double x) {

		int ns = (int) Math.round(x); // Nearest grid point
		if (ns < 0)
			ns = 0;
		else if (ns > order)
			ns = order;
		final double[] c = new double[order + 1];
		final double[] d = new double[order + 1];
		System.arraycopy(f, offset, c, 0, order + 1);
		System.arraycopy(f, offset, d, 0, order + 1);

		double y = c[ns--];
		double ho, hp, w, dy = 0;
		for (int m = 1; m <= order; m++) {
			for (int i = 0; i <= (order - m); i++) {
				ho = i - x;
				hp = (i + m) - x;
				w = c[i + 1] - d[i];
				d[i] = (-hp * w) / m;
				c[i] = (-ho * w) / m;
			}
			dy = ((2 * ns) < (order - 1 - m)) ? c[ns + 1] : d[ns--];
			y += dy;
		}

		return new double[] { y, dy };
	}

	/**
	 * d2 - Performs interpolation of the 2-d function f(x1,x2). The values of the
	 * function on the grid are provided in a double[][] (2-d array). The
	 * coordinates of the grid points are assumed to be x1[i] = x0[0]+i*xinc[0] and
	 * x2[j] = x0[1]+j*xinc[1] where i is an integer index ranging from 0 to
	 * f.length and j is another index ranging from 0 to f[0].length. The grid is
	 * specified by providing appropriate x0[] and xinc[] arrays.
	 * 
	 * @param f     - double[][] 2-D array of function values at the grid points
	 * @param x0    - double[] An array of two values, [x1,x2], corresponding to
	 *              f[0][0]
	 * @param xinc  - double[] The grid spacings.
	 * @param order - int The order of the interpolation.
	 * @param x     - double[] Array of two values, [x1,x2], providing the
	 *              coordinates of the point at which the function value is to be
	 *              estimated.
	 * @return double[] - An array of 2 values, the first of which is the estimate
	 *         for f[x] and the second is an error estimate. The error estimate is
	 *         based only upon the interpolation in the x1 direction, however.
	 */
	public static double[] d2(double[][] f, double[] x0, double[] xinc, int order, double[] x) {
		/*
		 * N-dimensional interpolation is performed by calling the N-1 dimensional
		 * interpolation routine to determine function values at nodes in the Nth
		 * dimension, which can then be interpolated via 1-d interpolation.
		 */
		if ((x0.length < 2) || (xinc.length < 2) || (x.length < 2))
			throw new IllegalArgumentException("Input array is too short.)");
		if (xinc[0] == 0.)
			throw new IllegalArgumentException("Interval spacing must be nonzero.");
		if (f.length < (order + 1))
			throw new IllegalArgumentException("0 < order <= table.length-1 is required.");
		/*
		 * The reduced coordinate is like an index into the array, but it can take on
		 * fractional values to indicate positions between the grid points. Obtain
		 * reduced 1st coordinate
		 */
		final double reducedx1 = (x[0] - x0[0]) / xinc[0];
		/*
		 * index0 is the index of the 1st grid point we'll include in the interpolation.
		 * Ideally, we'd like reducedx close to the middle of the set of grid points we
		 * choose. If reducedx is close to the beginning or end of the array, this might
		 * not be possible.
		 */
		int index0 = (int) reducedx1 - (order / 2);
		if (index0 < 0)
			index0 = 0;
		else if (index0 > (f.length - order - 1))
			index0 = f.length - order - 1;
		/* Generate and populate an array of function values at x2 */
		final double[] y = new double[order + 1];
		for (int i = 0; i <= order; i++) {
			final double[] temp = d1(f[index0 + i], x0[1], xinc[1], order, x[1]);
			y[i] = temp[0];
		}

		/* Interpolate these to find the value at x1 */
		return d1(y, x0[0] + (index0 * xinc[0]), xinc[0], order, x[0]);
	}

	/**
	 * d3 - Performs interpolation of the 3-d function f(x1,x2,x3). The values of
	 * the function on the grid are provided in a double[][][] (3-D array). The
	 * coordinates of the grid points are assumed to be x1[i] = x0[0]+i*xinc[0],
	 * x2[j] = x0[1]+j*xinc[1], and x3[k] = x0[2]+k*xinc[2] where i, j, and k are
	 * integer indices ranging from 0 to 1 less than the lengths of their respective
	 * dimensions. The grid is specified by providing appropriate x0[] and xinc[]
	 * arrays.
	 * 
	 * @param f     - double[][][] 3-D array of function values at the grid points
	 * @param x0    - double[] An array of two values, [x1,x2], corresponding to
	 *              f[0][0]
	 * @param xinc  - double[] The grid spacings.
	 * @param order - int The order of the interpolation.
	 * @param x     - double[] Array of two values, [x1,x2], providing the
	 *              coordinates of the point at which the function value is to be
	 *              estimated.
	 * @return double[] - An array of 2 values, the first of which is the estimate
	 *         for f[x] and the second is an error estimate. The error estimate is
	 *         based only upon the interpolation in the x1 direction, however.
	 */
	public static double[] d3(double[][][] f, double[] x0, double[] xinc, int order, double[] x) {
		/*
		 * N-dimensional interpolation is performed by calling the N-1 dimensional
		 * interpolation routine to determine function values at nodes in the Nth
		 * dimension, which can then be interpolated via 1-d interpolation.
		 */
		if ((x0.length < 3) || (xinc.length < 3) || (x.length < 3))
			throw new IllegalArgumentException("Input array is too short.)");
		if (xinc[0] == 0.)
			throw new IllegalArgumentException("Interval spacing must be nonzero.");
		if (f.length < (order + 1))
			throw new IllegalArgumentException("0 < order <= table.length-1 is required.");

		/*
		 * The reduced coordinate is like an index into the array, but it can take on
		 * fractional values to indicate positions between the grid points. Obtain
		 * reduced 1st coordinate
		 */
		final double reducedx1 = (x[0] - x0[0]) / xinc[0];
		/*
		 * index0 is the index of the 1st grid point we'll include in the interpolation.
		 * Ideally, we'd like reducedx close to the middle of the set of grid points we
		 * choose. If reducedx is close to the beginning or end of the array, this might
		 * not be possible.
		 */
		int index0 = (int) reducedx1 - (order / 2);
		if (index0 < 0)
			index0 = 0;
		else if (index0 > (f.length - order - 1))
			index0 = f.length - order - 1;
		/* Generate and populate an array of function values at x2,x3 */
		final double[] y = new double[order + 1];
		final double[] x0temp = new double[] { x0[1], x0[2] };
		final double[] xinctemp = new double[] { xinc[1], xinc[2] };
		final double[] xtemp = new double[] { x[1], x[2] };
		for (int i = 0; i <= order; i++) {
			final double[] temp = d2(f[index0 + i], x0temp, xinctemp, order, xtemp);
			y[i] = temp[0];
		}

		/* Interpolate these to find the value at x1 */
		return d1(y, x0[0] + (index0 * xinc[0]), xinc[0], order, x[0]);
	}

	/**
	 * d4 - Performs interpolation of the 4-d function f(x1,x2,x3,x4). The values of
	 * the function on the grid are provided in a double[][][][] (4-D array). The
	 * coordinates of the grid points are assumed to be x1[i] = x0[0]+i*xinc[0],
	 * x2[j] = x0[1]+j*xinc[1], x3[k] = x0[2]+k*xinc[2], x4[m] = x0[3]+m*xinc[3]
	 * where i, j, k, and m are integer indices ranging from 0 to 1 less than the
	 * lengths of their respective dimensions. The grid is specified by providing
	 * appropriate x0[] and xinc[] arrays.
	 * 
	 * @param f     - double[][][][] 4-D array of function values at the grid points
	 * @param x0    - double[] An array of two values, [x1,x2], corresponding to
	 *              f[0][0]
	 * @param xinc  - double[] The grid spacings.
	 * @param order - int The order of the interpolation.
	 * @param x     - double[] Array of two values, [x1,x2], providing the
	 *              coordinates of the point at which the function value is to be
	 *              estimated.
	 * @return double[] - An array of 2 values, the first of which is the estimate
	 *         for f[x] and the second is an error estimate. The error estimate is
	 *         based only upon the interpolation in the x1 direction, however.
	 */
	public static double[] d4(double[][][][] f, double[] x0, double[] xinc, int order, double[] x) {
		/*
		 * N-dimensional interpolation is performed by calling the N-1 dimensional
		 * interpolation routine to determine function values at nodes in the Nth
		 * dimension, which can then be interpolated via 1-d interpolation.
		 */
		/*
		 * I was concerned about the error checks on the following lines and in the
		 * similar areas of the lower dimensionality interpolation calls. When d4
		 * repeatedly calls d3, some of the error checking is unnecessary. I mostly use
		 * these routines (e.g., in RegularTableInterpolation) in repeated calls with
		 * only x differing. On the first call it makes sense to insure the array size
		 * is sufficient and interval spacing is nonzero, etc. but on the second call we
		 * already know the answer to this. However, by timing some interpolations I
		 * observe that removing the error checking completely speeds execution by less
		 * than 6%, so the more generally robust error check as currently written is
		 * probably worth it.
		 */

		if ((x0.length < 4) || (xinc.length < 4) || (x.length < 4))
			throw new IllegalArgumentException("Input array is too short.)");
		if (xinc[0] == 0.)
			throw new IllegalArgumentException("Interval spacing must be nonzero.");
		if (f.length < (order + 1))
			throw new IllegalArgumentException("0 < order <= table.length-1 is required.");

		/*
		 * The reduced coordinate is like an index into the array, but it can take on
		 * fractional values to indicate positions between the grid points. Obtain
		 * reduced 1st coordinate
		 */
		final double reducedx1 = (x[0] - x0[0]) / xinc[0];
		/*
		 * index0 is the index of the 1st grid point we'll include in the interpolation.
		 * Ideally, we'd like reducedx close to the middle of the set of grid points we
		 * choose. If reducedx is close to the beginning or end of the array, this might
		 * not be possible.
		 */
		int index0 = (int) reducedx1 - (order / 2);
		if (index0 < 0)
			index0 = 0;
		else if (index0 > (f.length - order - 1))
			index0 = f.length - order - 1;
		/* Generate and populate an array of function values at x2,x3 */
		final double[] y = new double[order + 1];
		final double[] x0temp = new double[] { x0[1], x0[2], x0[3] };
		final double[] xinctemp = new double[] { xinc[1], xinc[2], xinc[3] };
		final double[] xtemp = new double[] { x[1], x[2], x[3] };
		for (int i = 0; i <= order; i++) {
			final double[] temp = d3(f[index0 + i], x0temp, xinctemp, order, xtemp);
			y[i] = temp[0];
		}

		/* Interpolate these to find the value at x1 */
		return d1(y, x0[0] + (index0 * xinc[0]), xinc[0], order, x[0]);
	}
}
