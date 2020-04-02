/**
 *
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Scanner;

import gov.nist.nanoscalemetrology.JMONSELutils.BiCubicSpline;
import gov.nist.nanoscalemetrology.JMONSELutils.CubicSpline;
import gov.nist.nanoscalemetrology.JMONSELutils.QuadriCubicSpline;
import gov.nist.nanoscalemetrology.JMONSELutils.TriCubicSpline;

/**
 * <p>
 * A class for interpolating regular tables using Splines. A regular table is an
 * array of 1 or more dimensions representing the values of some function at
 * equally spaced input values (i.e., on a "regular grid"). For example, the
 * table may contain values of f(x,y) with Nx values of x: x[i] = xmin + i*xinc,
 * i=0,1,...,Nx-1 and Ny values of y: y[j] = ymin + j*yinc, j=0,1,...,Ny-1. The
 * table holds the Nx x Ny array of f values, corresponding to (x[i],y[j]). The
 * table is not restricted to the example's 2 dimensions, but currently may have
 * 1, 2, 3, or 4 dimensions.
 * </p>
 * <p>
 * The data for a given RegularTableSplineInterpolation object are stored in a
 * file, the name of which is provided to the constructor when it is
 * instantiated. This class then provides a method for interpolating the table,
 * permitting estimation of function values at points intermediate between those
 * on the grid.
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
 */
public class RegularTableSplineInterpolation {

	private CubicSpline spline1d = null;
	private BiCubicSpline spline2d = null;
	private TriCubicSpline spline3d = null;
	private QuadriCubicSpline spline4d = null;
	int dim; // dimension of this table
	int[] nPoints; // Array of length dim with number of points for each x
	double[] xinc; // Array of length dim with x increment size
	double[] xmin; // Array of minimum x values
	private final String tableFileName;

	/**
	 * RegularTableSplineInterpolation - Create an interpolation table from the
	 * named resource. The table is assumed to be stored in the resource as numbers
	 * (in character format) separated by white space. The numbers are in this
	 * order: Number of input variables for this table (N), # of values taken by the
	 * 1st input variable, minimum value of 1st input variable, increment of 1st
	 * input variable, ... (repeated for 2nd, 3rd, up to Nth input variable), then a
	 * list of the tabulated values in order with the Nth input variable varying
	 * most rapidly, the N-1st next, and so on, with the 1st varying most slowly.
	 *
	 * @param tableFileName - A String providing the name of the resource (data
	 *                      file) that stores the table to be interpolated.
	 */
	public RegularTableSplineInterpolation(String tableFileName) throws FileNotFoundException {
		this.tableFileName = tableFileName;
		ReadTable(tableFileName);
	}

	public double interpolate(double[] xval) {
		if (xval.length < dim)
			throw new IllegalArgumentException(
					"Attempt to interpolate " + tableFileName + " at x with " + String.valueOf(dim) + "dimensions");
		switch (dim) {
		case 1:
			return spline1d.interpolate(xval[0]);
		case 2:
			return spline2d.interpolate(xval[0], xval[1]);
		case 3:
			return spline3d.interpolate(xval[0], xval[1], xval[2]);
		case 4:
			return spline4d.interpolate(xval[0], xval[1], xval[2], xval[3]);
		default:
			throw new IllegalArgumentException("Table dimensions must be 1<=dim<=4");
		}
	}

	private void ReadTable(String tableFileName) throws FileNotFoundException {
		final InputStream is = RegularTableSplineInterpolation.class.getResourceAsStream(tableFileName);
		if (is == null)
			throw new FileNotFoundException("Could not locate " + tableFileName);
		final Scanner s = new Scanner(is);
		s.useLocale(Locale.US);
		try {
			dim = s.nextInt();
			if ((dim < 1) || (dim > 4))
				throw new IllegalArgumentException("Table dimensions must be 1<=dim<=4");
			nPoints = new int[dim];
			xinc = new double[dim];
			final double[][] x = new double[dim][];
			xmin = new double[dim];

			for (int i = 0; i < dim; i++) {
				nPoints[i] = s.nextInt();
				xmin[i] = s.nextDouble();
				xinc[i] = s.nextDouble();
				x[i] = new double[nPoints[i]];
				for (int j = 0; j < nPoints[i]; j++)
					x[i][j] = xmin[i] + (j * xinc[i]);
			}
			switch (dim) {
			case 1:
				final double[] table1d = new double[nPoints[0]];
				for (int i = 0; i < nPoints[0]; i++)
					table1d[i] = s.nextDouble();
				spline1d = new CubicSpline(x[0], table1d);
				break;
			case 2:
				final double[][] table2d = new double[nPoints[0]][nPoints[1]];
				for (int i = 0; i < nPoints[0]; i++)
					for (int j = 0; j < nPoints[1]; j++)
						table2d[i][j] = s.nextDouble();
				spline2d = new BiCubicSpline(x[0], x[1], table2d);
				break;
			case 3:
				final double[][][] table3d = new double[nPoints[0]][nPoints[1]][nPoints[2]];
				for (int i = 0; i < nPoints[0]; i++)
					for (int j = 0; j < nPoints[1]; j++)
						for (int k = 0; k < nPoints[2]; k++)
							table3d[i][j][k] = s.nextDouble();
				spline3d = new TriCubicSpline(x[0], x[1], x[2], table3d);
				break;
			case 4:
				final double[][][][] table4d = new double[nPoints[0]][nPoints[1]][nPoints[2]][nPoints[3]];
				for (int i = 0; i < nPoints[0]; i++)
					for (int j = 0; j < nPoints[1]; j++)
						for (int k = 0; k < nPoints[2]; k++)
							for (int m = 0; m < nPoints[3]; m++)
								table4d[i][j][k][m] = s.nextDouble();
				spline4d = new QuadriCubicSpline(x[0], x[1], x[2], x[3], table4d);
				break;
			}
		} finally {
			s.close();
		}
	}
}
