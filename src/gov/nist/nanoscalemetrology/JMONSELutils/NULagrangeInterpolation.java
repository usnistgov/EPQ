/**
 * 
 */

package gov.nist.nanoscalemetrology.JMONSELutils;

/**
 * <p>
 * Class of static methods for Lagrange (polynomial) interpolation of a function
 * f(x[]) given a table of function values on a nonuniform discrete grid of
 * input values. The methods are based upon ideas from Numerical Recipes in C,
 * with adaptations for language and object-oriented approach.
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
public class NULagrangeInterpolation {

   /**
    * d1 - Performs interpolation of the function f(x). The caller supplies a
    * sampling of this function at points xsamp, fsamp. These are arrays (type
    * double[]) of the same length. The values in xsamp must be montonic (not
    * necessarily at equal intervals) without duplicates and the values in fsamp
    * must be the corresponding function values.
    * 
    * @param fsamp
    *           - double[] 1D array of function values at the grid points
    * @param xsamp
    *           - double[] The values of the independent variable at the grid
    *           points
    * @param order
    *           - int The order of the interpolation (1 for linear, 3 for cubic,
    *           etc.).
    * @param x
    *           - double The x value at which the function value is to be
    *           estimated.
    * @return double[] - An array of 2 values, the first of which is the
    *         estimate for f[x] and the second is an error estimate.
    */
   public static double[] d1(double[] fsamp, double[] xsamp, int order, double x) {
      /*
       * The interpolation is performed by a call to neville, a separate static
       * method.
       */
      if ((order < 1) || (fsamp.length < (order + 1)))
         throw new IllegalArgumentException("0 < order <= table.length-1 is required.");

      /*
       * index0 is the index of the 1st grid point we'll include in the
       * interpolation. Ideally, we'd like reducedx close to the middle of the
       * set of grid points we choose. If reducedx is close to the beginning or
       * end of the array, this might not be possible.
       */
      final int[] location = locate(x, xsamp, order);
      return neville(fsamp, xsamp, location, order, x);
   }

   /**
    * locate -- A private utility that locates the position in the table of the
    * values we need for a given interpolation. It returns an array of two
    * integer values {firstIndex,nearestIndex}. The required values for
    * interpolation range from the firstIndex to firstIndex+order.
    * xsamp[nearestIndex] is closer to x than any of the other values in xsamp.
    * 
    * @param x
    *           - double The x value at which the function value is to be
    *           estimated.
    * @param xsamp
    *           - double[] The 1-D array of tabulated x values
    * @param order
    *           - int The order of the interpolation (1 for linear, 3 for cubic,
    *           etc.).
    * @return int[] - An array containing the index of the first x coordinate
    *         and the index of the nearest x coordinate
    */
   private static int[] locate(double x, double[] xsamp, int order) {
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
         return new int[]{0, 0};
      else if (uplim > maxindex)
         return new int[]{maxindex - order, maxindex};
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
       * At this point lowlim and uplim differ by 1. x is between xsamp[lowlim]
       * and xsamp[uplim]. If order>1 these 2 points are not enough. We need
       * order+1 points for our interpolation. We widen the range by adding
       * points either above or below, adding them in order of proximity to the
       * original interval.
       */
      int firstInd = lowlim; // Initialize
      int lastInd = uplim;

      while (((lastInd - firstInd) < order) && (firstInd > 0) && (lastInd < maxindex))
         if (((((xsamp[lowlim] - xsamp[firstInd - 1]) + xsamp[uplim]) - xsamp[lastInd + 1]) >= 0) == ascending)
            lastInd += 1;
         else
            firstInd -= 1;
      if (lastInd >= maxindex)
         return new int[]{maxindex - order, nindex}; // Extrapolating off high
                                                     // index end of table
      else if (firstInd <= 0)
         return new int[]{0, nindex}; // Extrapolating off the low end

      return new int[]{firstInd, nindex};

   }

   /**
    * neville -- A private utility that performs the actual 1-d interpolation,
    * using Neville's algorithm.
    * 
    * @param fsamp
    *           - double[] 1D array of function values at the grid points
    * @param xsamp
    *           - double[] The values of the independent variable at the grid
    *           points
    * @param location
    *           - int[] Location in the table to use, as returned by locate.
    * @param order
    *           - int The order of the interpolation (1 for linear, 3 for cubic,
    *           etc.).
    * @param x
    *           - double The x value at which the function is to be estimated.
    * @return double[] - An array of 2 values, the first of which is the
    *         estimate for f[x] and the second is an error estimate.
    */
   private static double[] neville(double[] f, double[] xsamp, int[] location, int order, double x) {

      final int offset = location[0];
      int ns = location[1] - offset; // Nearest grid point

      final double[] c = new double[order + 1];
      final double[] d = new double[order + 1];
      System.arraycopy(f, offset, c, 0, order + 1);
      System.arraycopy(f, offset, d, 0, order + 1);

      double y = c[ns--];
      double ho, hp, w, den, dy = 0;
      for (int m = 1; m <= order; m++) {
         for (int i = 0; i <= (order - m); i++) {
            ho = xsamp[i + offset] - x;
            hp = xsamp[i + m + offset] - x;
            w = c[i + 1] - d[i];
            den = ho - hp;
            if (den == 0.)
               throw new IllegalArgumentException("neville: Identical x values (x = " + Double.toString(xsamp[i + offset])
                     + " in interpolation table at indices " + Integer.toString(i + offset) + " and " + Integer.toString(i + offset + m) + ".");
            den = w / den;
            d[i] = hp * den;
            c[i] = ho * den;
         }
         dy = ((2 * ns) < (order - 1 - m)) ? c[ns + 1] : d[ns--];
         y += dy;
      }

      return new double[]{y, dy};
   }

   /**
    * d2 - Performs interpolation of the 2-d function f(x1,x2). The values of
    * the function on the grid are provided in a double[][] (2-d array). The
    * coordinates of the grid points are assumed to be x1[i] = x0[0]+i*xinc[0]
    * and x2[j] = x0[1]+j*xinc[1] where i is an integer index ranging from 0 to
    * f.length and j is another index ranging from 0 to f[0].length. The grid is
    * specified by providing appropriate x0[] and xinc[] arrays.
    * 
    * @param f
    *           - double[][] 2-D array of function values at the grid points
    * @param xsamp
    *           - double[][] 2 x ? (ragged) array of x values at the grid points
    * @param order
    *           - int The order of the interpolation.
    * @param x
    *           - double[] Array of two values, [x1,x2], providing the
    *           coordinates of the point at which the function value is to be
    *           estimated.
    * @return double[] - An array of 2 values, the first of which is the
    *         estimate for f[x] and the second is an error estimate. The error
    *         estimate is based only upon the interpolation in the x1 direction,
    *         however.
    */
   public static double[] d2(double[][] f, double[][] xsamp, int order, double[] x) {
      /*
       * N-dimensional interpolation is performed by calling the N-1 dimensional
       * interpolation routine to determine function values at nodes in the Nth
       * dimension, which can then be interpolated via 1-d interpolation.
       */
      if (x.length < 2)
         throw new IllegalArgumentException("Input array is too short.)");
      if (f.length < (order + 1))
         throw new IllegalArgumentException("0 < order <= table.length-1 is required.");

      final int index0 = locate(x[0], xsamp[0], order)[0];

      /* Generate and populate an array of function values at x2 */
      final double[] y = new double[order + 1];
      for (int i = 0; i <= order; i++)
         y[i] = d1(f[index0 + i], xsamp[1], order, x[1])[0];
      /* Make corresponding x array */
      final double[] xtemp = new double[order + 1];
      System.arraycopy(xsamp[0], index0, xtemp, 0, order + 1);

      /* Interpolate these to find the value at x1 */
      return d1(y, xtemp, order, x[0]);
   }

   /**
    * d3 - Performs interpolation of the 3-d function f(x1,x2,x3). The values of
    * the function on the grid are provided in a double[][][] (3-D array). The
    * coordinates of the grid points are assumed to be x1[i] = x0[0]+i*xinc[0],
    * x2[j] = x0[1]+j*xinc[1], and x3[k] = x0[2]+k*xinc[2] where i, j, and k are
    * integer indices ranging from 0 to 1 less than the lengths of their
    * respective dimensions. The grid is specified by providing appropriate x0[]
    * and xinc[] arrays.
    * 
    * @param f
    *           - double[][][] 3-D array of function values at the grid points
    * @param xsamp
    *           - double[][] 3 x ? (ragged) array of x values at the grid points
    * @param order
    *           - int The order of the interpolation.
    * @param x
    *           - double[] Array of 3 values, [x1,x2,x3], providing the
    *           coordinates of the point at which the function value is to be
    *           estimated.
    * @return double[] - An array of 2 values, the first of which is the
    *         estimate for f[x] and the second is an error estimate. The error
    *         estimate is based only upon the interpolation in the x1 direction,
    *         however.
    */
   public static double[] d3(double[][][] f, double[][] xsamp, int order, double[] x) {
      /*
       * N-dimensional interpolation is performed by calling the N-1 dimensional
       * interpolation routine to determine function values at nodes in the Nth
       * dimension, which can then be interpolated via 1-d interpolation.
       */
      if (x.length < 3)
         throw new IllegalArgumentException("Input array is too short.)");
      if (f.length < (order + 1))
         throw new IllegalArgumentException("0 < order <= table.length-1 is required.");

      final int index0 = locate(x[0], xsamp[0], order)[0];

      /* Generate and populate an array of function values at x2,x3 */
      final double[] y = new double[order + 1];
      final double reducedx[] = {x[1], x[2]};
      final double[][] reducedxsamp = {xsamp[1], xsamp[2]};
      for (int i = 0; i <= order; i++)
         y[i] = d2(f[index0 + i], reducedxsamp, order, reducedx)[0];
      /* Make corresponding x array */
      final double[] xtemp = new double[order + 1];
      System.arraycopy(xsamp[0], index0, xtemp, 0, order + 1);

      /* Interpolate these to find the value at x1 */
      return d1(y, xtemp, order, x[0]);
   }

   /**
    * d4 - Performs interpolation of the 4-d function f(x1,x2,x3,x4). The values
    * of the function on the grid are provided in a double[][][][] (4-D array).
    * The coordinates of the grid points are assumed to be x1[i] =
    * x0[0]+i*xinc[0], x2[j] = x0[1]+j*xinc[1], x3[k] = x0[2]+k*xinc[2], x4[m] =
    * x0[3]+m*xinc[3] where i, j, k, and m are integer indices ranging from 0 to
    * 1 less than the lengths of their respective dimensions. The grid is
    * specified by providing appropriate x0[] and xinc[] arrays.
    * 
    * @param f
    *           - double[][][][] 4-D array of function values at the grid points
    * @param xsamp
    *           - double[][] 4 x ? (ragged) array of x values at the grid points
    * @param order
    *           - int The order of the interpolation.
    * @param x
    *           - double[] Array of 4 values, [x1,x2,x3,x4], providing the
    *           coordinates of the point at which the function value is to be
    *           estimated.
    * @return double[] - An array of 2 values, the first of which is the
    *         estimate for f[x] and the second is an error estimate. The error
    *         estimate is based only upon the interpolation in the x1 direction,
    *         however.
    */
   public static double[] d4(double[][][][] f, double[][] xsamp, int order, double[] x) {
      /*
       * N-dimensional interpolation is performed by calling the N-1 dimensional
       * interpolation routine to determine function values at nodes in the Nth
       * dimension, which can then be interpolated via 1-d interpolation.
       */
      /*
       * I was concerned about the error checks on the following lines and in
       * the similar areas of the lower dimensionality interpolation calls. When
       * d4 repeatedly calls d3, some of the error checking is unnecessary. I
       * mostly use these routines (e.g., in RegularTableInterpolation) in
       * repeated calls with only x differing. On the first call it makes sense
       * to insure the array size is sufficient and interval spacing is nonzero,
       * etc. but on the second call we already know the answer to this.
       * However, by timing some interpolations I observe that removing the
       * error checking completely speeds execution by less than 6%, so the more
       * generally robust error check as currently written is probably worth it.
       */

      if (x.length < 4)
         throw new IllegalArgumentException("Input array is too short.)");
      if (f.length < (order + 1))
         throw new IllegalArgumentException("0 < order <= table.length-1 is required.");

      /*
       * The reduced coordinate is like an index into the array, but it can take
       * on fractional values to indicate positions between the grid points.
       * Obtain reduced 1st coordinate
       */
      final int index0 = locate(x[0], xsamp[0], order)[0];

      /* Generate and populate an array of function values at x2,x3,x4 */
      final double[] y = new double[order + 1];
      final double reducedx[] = {x[1], x[2], x[3]};
      final double[][] reducedxsamp = {xsamp[1], xsamp[2], xsamp[3]};
      for (int i = 0; i <= order; i++)
         y[i] = d3(f[index0 + i], reducedxsamp, order, reducedx)[0];
      /* Make corresponding x array */
      final double[] xtemp = new double[order + 1];
      System.arraycopy(xsamp[0], index0, xtemp, 0, order + 1);

      /* Interpolate these to find the value at x1 */
      return d1(y, xtemp, order, x[0]);
   }
}
