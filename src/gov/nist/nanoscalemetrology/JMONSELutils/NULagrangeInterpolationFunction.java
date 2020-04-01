/**
 * 
 */

package gov.nist.nanoscalemetrology.JMONSELutils;

import java.lang.Integer;
import java.lang.Math;
import java.util.Arrays;

/**
 * <p>
 * Class to recursively build a polynomial (Lagrange) interpolation of a
 * function of arbitrarily many variables sampled on possibly nonuniform
 * intervals.
 * </p>
 * <p>
 * For interpolation on a single variable, the function f(x) is sampled at an
 * array of independent values, double[] xsamp, producing an array of dependent
 * values, double[] fsamp. The constructor builds an interpolation function that
 * can subsequently be evaluated for arbitrary x, supplied to evaluateAt as an
 * array of length 1: double[1] {x}.
 * </p>
 * <p>
 * For interpolation on 2 variables, instead of an array of double[], fsamp is
 * an array of IListFunction[]. fsamp[i] is a function that implements f(x_i,y).
 * Since x_i is a constant, these f(x_i,y) are univariate functions, for example
 * interpolations of the kind described in the previous paragraph.
 * </p>
 * <p>
 * Likewise, to interpolate on 3 variables, fsamp is an array of 2-variable
 * IListFunctions which can be constructed according to the preceding paragraph.
 * At each stage the N-variable interpolation is done as a single variable
 * interpolation on functions of N-1 variables. In this way, one can recursively
 * construct interpolation on any number of variables.
 * </p>
 * <p>
 * This interpolation differs from the more usual interpolation on a
 * multidimensional array of values of the independent variables inasmuch as the
 * functions need not be sampled on the same number of points. That is, the
 * array may be ragged. This lends itself, for example, to sampling the
 * reference function, let us say f(x,y), densely where needed for accuracy and
 * more coarsely elsewhere, without the constraint that dense sampling of x for
 * some y values must imply dense sampling for all y values.
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
public class NULagrangeInterpolationFunction implements IListFunction {

	private IListFunction[] fsamp;
	private double[] f;
	private double[] xsamp;
	private int order;
	private int nvars;

	/**
	 * This constructor is used for interpolation on a single variable. fsamp is a
	 * double[] array of function values at the sampled grid points.
	 * 
	 * @param fsamp - An array of function values
	 * @param xsamp - A strictly increasing array of corresponding independent
	 *              variable values
	 * @param order - The polynomial order of the interpolation
	 */
	public NULagrangeInterpolationFunction(double[] fsamp, double[] xsamp, int order) {
		super();
		this.xsamp = xsamp;
		this.order = order;
		this.nvars = 1;
		this.f = fsamp;

		if ((order < 1) || (fsamp.length < (order + 1)))
			throw new IllegalArgumentException("0 < order <= table.length-1 is required.");
		if (xsamp.length != xsamp.length)
			throw new IllegalArgumentException(
					"Dependent (fsamp) and independent (x) arrays must be of the same length.");
		/* xsamp values must be strictly monotonic */
		double sign = Math.signum(xsamp[1] - xsamp[0]);
		if (sign == 0.)
			throw new IllegalArgumentException("Illegal identical values in xsamp");
		for (int i = 2; i < xsamp.length; i++) {
			double newsign = Math.signum(xsamp[i] - xsamp[i - 1]);
			if (newsign != sign) {
				if (newsign == 0)
					throw new IllegalArgumentException("Illegal identical values in xsamp");
				else
					new IllegalArgumentException("Illegal non-monotonic xsamp");
			}
		}

	}

	/**
	 * This constructor is used for interpolation of nvars variables when nvars > 1.
	 * fsamp is an array of IListFunction[], each of which takes nvars-1 variables
	 * for input.
	 * 
	 * @param fsamp - An array of ListFunctions
	 * @param xsamp - A strictly increasing array of corresponding independent
	 *              variable values
	 * @param order - The polynomial order of the interpolation
	 * @param nvars - The number of variables accepted as input by this
	 *              interpolation function
	 */
	public NULagrangeInterpolationFunction(IListFunction[] fsamp, double[] xsamp, int order, int nvars) {
		super();
		this.fsamp = fsamp;
		this.xsamp = xsamp;
		this.order = order;
		this.nvars = nvars;

		if (nvars < 1)
			throw new IllegalArgumentException("nvars must be an integer > 0.");
		if (nvars == 1)
			throw new IllegalArgumentException("Wrong constructor! fsamp should be double[] when nvars = 1");
		if ((order < 1) || (fsamp.length < (order + 1)))
			throw new IllegalArgumentException("0 < order <= table.length-1 is required.");
		if (xsamp.length != xsamp.length)
			throw new IllegalArgumentException(
					"Dependent (fsamp) and independent (x) arrays must be of the same length.");

		/* The ListFunctions in fsamp should all be functions of nvars - 1 variable */
		int nRest = nvars - 1;
		for (IListFunction f : fsamp) {
			if (f.nVariables() != nRest)
				throw new IllegalArgumentException(
						"Elements of fsamp must all be IListFunctions of " + nRest + " variables.");
		}

		/* xsamp values must be strictly monotonic */
		double sign = Math.signum(xsamp[1] - xsamp[0]);
		if (sign == 0.)
			throw new IllegalArgumentException("Illegal identical values in xsamp");
		for (int i = 2; i < xsamp.length; i++) {
			double newsign = Math.signum(xsamp[i] - xsamp[i - 1]);
			if (newsign != sign) {
				if (newsign == 0)
					throw new IllegalArgumentException("Illegal identical values in xsamp");
				else
					new IllegalArgumentException("Illegal non-monotonic xsamp");
			}
		}

	}

	/**
	 * evaluateAt - Performs interpolation of the function f(x). The caller supplies
	 * a sampling of this function at points xsamp, fsamp. These are arrays (type
	 * double[]) of the same length. The values in xsamp must be montonic (not
	 * necessarily at equal intervals) without duplicates and the values in fsamp
	 * must be the corresponding function values.
	 * 
	 * @param fsamp - double[] 1D array of function values at the grid points
	 * @param xsamp - double[] The values of the independent variable at the grid
	 *              points
	 * @param order - int The order of the interpolation (1 for linear, 3 for cubic,
	 *              etc.).
	 * @param x     - double The x value at which the function value is to be
	 *              estimated.
	 * @return double[] - An array of 2 values, the first of which is the estimate
	 *         for f[x] and the second is an error estimate.
	 */
	@Override
	public double evaluateAt(double[] x) {
		/*
		 * The interpolation is performed by a call to neville, a separate static
		 * method.
		 */

		/*
		 * index0 is the index of the 1st grid point we'll include in the interpolation.
		 * Ideally, we'd like reducedx close to the middle of the set of grid points we
		 * choose. If reducedx is close to the beginning or end of the array, this might
		 * not be possible.
		 */
		final int[] location = locate(x[0]);
		return neville(location, x)[0];
	}

	/**
	 * locate -- A private utility that locates the position in the table of the
	 * values we need for a given interpolation. It returns an array of two integer
	 * values {firstIndex,nearestIndex}. The required values for interpolation range
	 * from the firstIndex to firstIndex+order. xsamp[nearestIndex] is closer to x
	 * than any of the other values in xsamp.
	 * 
	 * @param x - double The x value at which the function value is to be estimated.
	 * @return int[] - An array containing the index of the first x coordinate and
	 *         the index of the nearest x coordinate
	 */
	private int[] locate(double x) {
		/* Use bisection to find the xsamp value nearest x. */

		/* Initialize limits between which the desired index must lie */
		int lowlim = -1;
		int uplim = xsamp.length;
		final int maxindex = uplim - 1;
		int midpoint;
		int nindex; // nearest index
		final boolean ascending = xsamp[maxindex] > xsamp[0];

		while ((uplim - lowlim) > 1) {
			midpoint = (uplim + lowlim) >> 1;
			if ((x > xsamp[midpoint]) == ascending)
				lowlim = midpoint;
			else
				uplim = midpoint;
		}

		if (lowlim < 0)
			return new int[] { 0, 0 };
		else if (uplim > maxindex)
			return new int[] { maxindex - order, maxindex };
		if (ascending) {
			if ((x - xsamp[lowlim]) <= (xsamp[uplim] - x))
				nindex = lowlim;
			else
				nindex = uplim;
		} else if ((xsamp[lowlim] - x) <= (x - xsamp[uplim]))
			nindex = lowlim;
		else
			nindex = uplim;
		/*
		 * At this point lowlim and uplim differ by 1. x is between xsamp[lowlim] and
		 * xsamp[uplim]. If order>1 these 2 points are not enough. We need order+1
		 * points for our interpolation. We widen the range by adding points either
		 * above or below, adding them in order of proximity to the original interval.
		 */
		int firstInd = lowlim; // Initialize
		int lastInd = uplim;

		while (((lastInd - firstInd) < order) && (firstInd > 0) && (lastInd < maxindex))
			if (((((xsamp[lowlim] - xsamp[firstInd - 1]) + xsamp[uplim]) - xsamp[lastInd + 1]) > 0) == ascending)
				lastInd += 1;
			else
				firstInd -= 1;
		if (lastInd >= maxindex)
			return new int[] { maxindex - order, nindex }; // Extrapolating off high index end of table
		else if (firstInd <= 0)
			return new int[] { 0, nindex }; // Extrapolating off the low end

		return new int[] { firstInd, nindex };

	}

	/**
	 * neville -- A private utility that performs the interpolation using Neville's
	 * algorithm.
	 * 
	 * @param location - int[] Location in the table to use, as returned by locate.
	 * @param x        - double The x value at which the function is to be
	 *                 estimated.
	 * @return double[] - An array of 2 values, the first of which is the estimate
	 *         for f[x] and the second is an error estimate.
	 */
	private double[] neville(int[] location, double[] x) {
		final int offset = location[0];
		int ns = location[1] - offset; // Nearest grid point

		double xhead = x[0];

		double[] c = new double[order + 1];
		double[] d = new double[order + 1];

		if (nvars == 1) {
			System.arraycopy(f, offset, c, 0, order + 1);
			System.arraycopy(f, offset, d, 0, order + 1);
		} else {
			double[] xrest = Arrays.copyOfRange(x, 1, x.length);
			for (int i = 0; i <= order; i++)
				c[i] = d[i] = fsamp[offset + i].evaluateAt(xrest);
		}

		double y = c[ns--];
		double ho, hp, w, den, dy = 0;
		for (int m = 1; m <= order; m++) {
			for (int i = 0; i <= (order - m); i++) {
				ho = xsamp[i + offset] - xhead;
				hp = xsamp[i + m + offset] - xhead;
				w = c[i + 1] - d[i];
				den = ho - hp;
				if (den == 0.)
					throw new IllegalArgumentException("neville: Identical x values (x = "
							+ Double.toString(xsamp[i + offset]) + " in interpolation table at indices "
							+ Integer.toString(i + offset) + " and " + Integer.toString(i + offset + m) + ".");
				den = w / den;
				d[i] = hp * den;
				c[i] = ho * den;
			}
			dy = ((2 * ns) < (order - 1 - m)) ? c[ns + 1] : d[ns--];
			y += dy;
		}

		return new double[] { y, dy };
	}

	@Override
	public int nVariables() {
		return nvars;
	}
}
