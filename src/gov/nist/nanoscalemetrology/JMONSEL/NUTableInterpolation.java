/**
 *
 */

package gov.nist.nanoscalemetrology.JMONSEL;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Locale;
import java.util.Scanner;
import java.util.TreeMap;

import gov.nist.nanoscalemetrology.JMONSELutils.NULagrangeInterpolation;

/**
 * <p>
 * A class for interpolating nonuniform tables using Lagrange (polynomial)
 * interpolation. A nonuniform table is an array of 1 or more dimensions
 * representing the values of some function at input values that are not
 * necessarily equally spaced. For example, the table may contain values of
 * f(x,y) defined at all combinations of x[i],y[j], i=0,1,...Nx-1 and
 * j=0,1,...Ny-1. The table holds the Nx x[i] values, the Ny y[j] values, and a
 * Nx x Ny array of f values, corresponding to (x[i],y[j]). The table is not
 * restricted to the example's 2 dimensions, but currently may have 1, 2, 3, or
 * 4 dimensions.
 * </p>
 * <p>
 * The data for a given NUTableInterpolation object are stored in a file, the
 * name of which is provided to the constructor when it is instantiated. This
 * class then provides a method for interpolating the table, permitting
 * estimation of function values at points intermediate between those on the
 * grid.
 * </p>
 * <p>
 * The implementation only permits a single NUTableInterpolation instance for
 * each file that stores a table. This avoids storing duplicates of the same
 * table in memory. Use the static RegularTableInterpolation.getInstance()
 * method rather than the constructor to obtain an interpolation table.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain.
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 */
public class NUTableInterpolation {

	/**
	 * getInstance - Returns an instance of a RegularTableInterpolation object for
	 * the table contained in the named resource.
	 *
	 * @param tableFileName - A String providing the full path name of the data file
	 *                      that stores the table to be interpolated.
	 */
	public static NUTableInterpolation getInstance(String tableFileName) throws FileNotFoundException {
		NUTableInterpolation uniqueInstance = instanceMap.get(tableFileName);
		if (uniqueInstance == null) {
			uniqueInstance = new NUTableInterpolation(tableFileName);
			instanceMap.put(tableFileName, uniqueInstance);
		}
		return uniqueInstance;
	}

	private double[] table1d;
	private double[][] table2d;
	private double[][][] table3d;
	private double[][][][] table4d;
	private double[][] x;
	private double[][] domain;
	private final double[] range = { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
	private int dim; // dimension of this table
	// int[] nPoints; // Array of length dim with number of points for each x
	// double[] xinc; // Array of length dim with x increment size
	// double[] xmin; // Array of minimum x values
	private final String tableFileName;

	private static TreeMap<String, NUTableInterpolation> instanceMap = new TreeMap<String, NUTableInterpolation>();

	/**
	 * RegularTableInterpolation - Create an interpolation table from the named
	 * resource. The table is assumed to be stored in the resource as numbers (in
	 * character format) separated by white space. The numbers are in this order:
	 * Number of input variables for this table (N), # of values taken by the 1st
	 * input variable, the monotonic list of these values, ... (repeated for 2nd,
	 * 3rd, up to Nth input variable), then a list of the tabulated values in order
	 * with the Nth input variable varying most rapidly, the N-1st next, and so on,
	 * with the 1st varying most slowly.
	 *
	 * @param tableFileName - A String providing the name of the resource (data
	 *                      file) that stores the table to be interpolated.
	 */
	private NUTableInterpolation(String tableFileName) throws FileNotFoundException {
		this.tableFileName = tableFileName;
		ReadTable(tableFileName);
	}

	/**
	 * Returns the domain (i.e., the interval of valid input values) of the
	 * interpolation table. This is an array of double[dim][2] where dim is the
	 * number of input variables. domain[i][0] and domain[i][1] are the smallest and
	 * largest values of the ith input variable.
	 *
	 * @return the domain
	 */
	public double[][] getDomain() {
		// Return a deep copy
		final double[][] domainCopy = new double[dim][2];
		for (int i = 0; i < dim; i++)
			for (int j = 0; j < 2; j++)
				domainCopy[i][j] = domain[i][j];
		return domainCopy;
	}

	/**
	 * Returns a two element array {min,max}, respectively the minimum and maximum
	 * output values contained in the table. Note that the interpolated value can
	 * fall outside of this range due to overshoot, but usually not by much.
	 *
	 * @return the range
	 */
	public double[] getRange() {
		// Return a copy
		final double[] rangeCopy = { range[0], range[1] };
		return rangeCopy;
	}

	/**
	 * interpolate - Interpolates this object's table to determine the value at the
	 * supplied input coordinate. If the supplied coordinate lies outside the domain
	 * of the table, this method extrapolates. This can very quickly lead to very
	 * poor estimates. The calling routine is responsible for checking the input
	 * against the domain if extrapolation is to be avoided.
	 *
	 * @param xval  - double[] of length in principle equal to the dimension of the
	 *              table. For convenience it is allowed to be greater, in which
	 *              case the unnecessary values at the end of the array are ignored.
	 * @param order - int The interpolation order, 1 for linear, 3 for cubic, etc.
	 * @return double - The estimated value of the tabulated function at the
	 *         supplied coordinate.
	 */
	public double interpolate(double[] xval, int order) {
		if (xval.length < dim)
			throw new IllegalArgumentException(
					"Attempt to interpolate " + tableFileName + " at x with " + String.valueOf(dim) + "dimensions");

		switch (dim) {
		case 1:
			return NULagrangeInterpolation.d1(table1d, x[0], order, xval[0])[0];
		case 2:
			return NULagrangeInterpolation.d2(table2d, x, order, xval)[0];
		case 3:
			return NULagrangeInterpolation.d3(table3d, x, order, xval)[0];
		case 4:
			return NULagrangeInterpolation.d4(table4d, x, order, xval)[0];
		default:
			throw new IllegalArgumentException("Table dimensions must be 1<=dim<=4");
		}
	}

	private void ReadTable(String tableFileName) throws FileNotFoundException {
		final FileReader fr = new FileReader(tableFileName);

		final BufferedReader br = new BufferedReader(fr);
		final Scanner s = new Scanner(br);
		s.useLocale(Locale.US);
		try {
			dim = s.nextInt();
			if ((dim < 1) || (dim > 4))
				throw new IllegalArgumentException("Table dimensions must be 1<=dim<=4");
			/*
			 * Note: I think I could write a general N-dimension interpolation using
			 * techniques similar to Mick Flanagan's PolyCubicSpline algorithm.
			 */
			final int[] nPoints = new int[dim];
			x = new double[dim][];

			domain = new double[dim][2];

			for (int i = 0; i < dim; i++) {
				nPoints[i] = s.nextInt();
				x[i] = new double[nPoints[i]];

				for (int j = 0; j < nPoints[i]; j++)
					/*
					 * TODO The try/catch below was added to debug a problem that seems unique to
					 * Georg Frase's computer. After the problem is solved, I can remove it.
					 */
					x[i][j] = s.nextDouble();

				if (x[i][0] < x[i][nPoints[i] - 1]) {
					domain[i][0] = x[i][0];
					domain[i][1] = x[i][nPoints[i] - 1];
				} else {
					domain[i][1] = x[i][0];
					domain[i][0] = x[i][nPoints[i] - 1];
				}
			}

			switch (dim) {
			case 1:
				table1d = new double[nPoints[0]];
				for (int i = 0; i < nPoints[0]; i++) {
					table1d[i] = s.nextDouble();
					if (table1d[i] < range[0])
						range[0] = table1d[i];
					else if (table1d[i] > range[1])
						range[1] = table1d[i];
				}
				break;
			case 2:
				table2d = new double[nPoints[0]][nPoints[1]];
				for (int i = 0; i < nPoints[0]; i++)
					for (int j = 0; j < nPoints[1]; j++) {
						table2d[i][j] = s.nextDouble();
						if (table2d[i][j] < range[0])
							range[0] = table2d[i][j];
						else if (table2d[i][j] > range[1])
							range[1] = table2d[i][j];
					}
				break;
			case 3:
				table3d = new double[nPoints[0]][nPoints[1]][nPoints[2]];
				for (int i = 0; i < nPoints[0]; i++)
					for (int j = 0; j < nPoints[1]; j++)
						for (int k = 0; k < nPoints[2]; k++) {
							table3d[i][j][k] = s.nextDouble();
							if (table3d[i][j][k] < range[0])
								range[0] = table3d[i][j][k];
							else if (table3d[i][j][k] > range[1])
								range[1] = table3d[i][j][k];
						}
				break;
			case 4:
				table4d = new double[nPoints[0]][nPoints[1]][nPoints[2]][nPoints[3]];
				for (int i = 0; i < nPoints[0]; i++)
					for (int j = 0; j < nPoints[1]; j++)
						for (int k = 0; k < nPoints[2]; k++)
							for (int m = 0; m < nPoints[3]; m++) {
								table4d[i][j][k][m] = s.nextDouble();
								if (table4d[i][j][k][m] < range[0])
									range[0] = table4d[i][j][k][m];
								else if (table4d[i][j][k][m] > range[1])
									range[1] = table4d[i][j][k][m];
							}
				break;
			}
		} finally {
			if (s != null)
				s.close();
		}
	}
}
