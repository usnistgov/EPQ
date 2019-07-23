/**
 *
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Scanner;
import java.util.TreeMap;

import gov.nist.nanoscalemetrology.JMONSELutils.ULagrangeInterpolation;

/*
 * TODO: I need to define the table format for users who want to write tables of
 * their own.
 */
/**
 * <p>
 * A class for interpolating regular tables using Lagrange (polynomial)
 * interpolation. A regular table is an array of 1 or more dimensions
 * representing the values of some function at equally spaced input values
 * (i.e., on a "regular grid"). For example, the table may contain values of
 * f(x,y) with Nx values of x: x[i] = xmin + i*xinc, i=0,1,...,Nx-1 and Ny
 * values of y: y[j] = ymin + j*yinc, j=0,1,...,Ny-1. The table holds the Nx x
 * Ny array of f values, corresponding to (x[i],y[j]). The table is not
 * restricted to the example's 2 dimensions, but currently may have 1, 2, 3, or
 * 4 dimensions.
 * </p>
 * <p>
 * The data for a given RegularTableInterpolation object are stored in a file,
 * the name of which is provided to the constructor when it is instantiated.
 * This class then provides a method for interpolating the table, permitting
 * estimation of function values at points intermediate between those on the
 * grid.
 * </p>
 * <p>
 * The implementation only permits a single RegularTableInterpolation instance
 * for each resource (file) that stores a table. This avoids storing duplicates
 * of the same table in memory. Use the static
 * RegularTableInterpolation.getInstance() method rather than the constructor to
 * obtain an interpolation table.
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
public class RegularTableInterpolation {

   /**
    * getInstance - Returns an instance of a RegularTableInterpolation object
    * for the table contained in the named resource.
    *
    * @param tableFileName - A String providing the name of the resource (data
    *           file) that stores the table to be interpolated.
    */
   public static RegularTableInterpolation getInstance(String tableFileName)
         throws FileNotFoundException {
      RegularTableInterpolation uniqueInstance = instanceMap.get(tableFileName);
      if(uniqueInstance == null) {
         uniqueInstance = new RegularTableInterpolation(tableFileName);
         instanceMap.put(tableFileName, uniqueInstance);
      }
      return uniqueInstance;
   }

   private double[] table1d;
   private double[][] table2d;
   private double[][][] table3d;
   private double[][][][] table4d;
   int dim; // dimension of this table
   int[] nPoints; // Array of length dim with number of points for each x
   double[] xinc; // Array of length dim with x increment size
   double[] xmin; // Array of minimum x values
   private final String tableFileName;

   private static TreeMap<String, RegularTableInterpolation> instanceMap = new TreeMap<String, RegularTableInterpolation>();

   /**
    * RegularTableInterpolation - Create an interpolation table from the named
    * resource. The table is assumed to be stored in the resource as numbers (in
    * character format) separated by white space. The numbers are in this order:
    * Number of input variables for this table (N), # of values taken by the 1st
    * input variable, minimum value of 1st input variable, increment of 1st
    * input variable, ... (repeated for 2nd, 3rd, up to Nth input variable),
    * then a list of the tabulated values in order with the Nth input variable
    * varying most rapidly, the N-1st next, and so on, with the 1st varying most
    * slowly.
    *
    * @param tableFileName - A String providing the name of the resource (data
    *           file) that stores the table to be interpolated.
    */
   private RegularTableInterpolation(String tableFileName)
         throws FileNotFoundException {
      this.tableFileName = tableFileName;
      ReadTable(tableFileName);
   }

   /**
    * interpolate - Interpolates this object's table to determine the value at
    * the supplied input coordinate.
    *
    * @param xval - double[] of length in principle equal to the dimension of
    *           the table. For convenience it is allowed to be greater, in which
    *           case the unnecessary values at the end of the array are ignored.
    * @param order - int The interpolation order, 1 for linear, 3 for cubic,
    *           etc.
    * @return double - The estimated value of the tabulated function at the
    *         supplied coordinate.
    */
   public double interpolate(double[] xval, int order) {
      if(xval.length < dim)
         throw new IllegalArgumentException("Attempt to interpolate " + tableFileName + " at x with " + String.valueOf(dim)
               + "dimensions");
      switch(dim) {
         case 1:
            return ULagrangeInterpolation.d1(table1d, xmin[0], xinc[0], order, xval[0])[0];
         case 2:
            return ULagrangeInterpolation.d2(table2d, xmin, xinc, order, xval)[0];
         case 3:
            return ULagrangeInterpolation.d3(table3d, xmin, xinc, order, xval)[0];
         case 4:
            return ULagrangeInterpolation.d4(table4d, xmin, xinc, order, xval)[0];
         default:
            throw new IllegalArgumentException("Table dimensions must be 1<=dim<=4");
      }
   }

   private void ReadTable(String tableFileName)
         throws FileNotFoundException {
      final InputStream is = RegularTableInterpolation.class.getResourceAsStream(tableFileName);
      if(is == null)
         throw new FileNotFoundException("Could not locate " + tableFileName);
      final Scanner s = new Scanner(is);
      s.useLocale(Locale.US);
      try {
         dim = s.nextInt();
         if((dim < 1) || (dim > 4))
            throw new IllegalArgumentException("Table dimensions must be 1<=dim<=4");
         /*
          * Note: I think I could write a general N-dimension interpolation
          * using techniques similar to Mick Flanagan's PolyCubicSpline
          * algorithm.
          */
         nPoints = new int[dim];
         xinc = new double[dim];
         final double[][] x = new double[dim][];
         xmin = new double[dim];

         for(int i = 0; i < dim; i++) {
            nPoints[i] = s.nextInt();
            xmin[i] = s.nextDouble();
            xinc[i] = s.nextDouble();
            x[i] = new double[nPoints[i]];
            for(int j = 0; j < nPoints[i]; j++)
               x[i][j] = xmin[i] + (j * xinc[i]);
         }
         switch(dim) {
            case 1:
               table1d = new double[nPoints[0]];
               for(int i = 0; i < nPoints[0]; i++)
                  table1d[i] = s.nextDouble();
               break;
            case 2:
               table2d = new double[nPoints[0]][nPoints[1]];
               for(int i = 0; i < nPoints[0]; i++)
                  for(int j = 0; j < nPoints[1]; j++)
                     table2d[i][j] = s.nextDouble();
               break;
            case 3:
               table3d = new double[nPoints[0]][nPoints[1]][nPoints[2]];
               for(int i = 0; i < nPoints[0]; i++)
                  for(int j = 0; j < nPoints[1]; j++)
                     for(int k = 0; k < nPoints[2]; k++)
                        table3d[i][j][k] = s.nextDouble();
               break;
            case 4:
               table4d = new double[nPoints[0]][nPoints[1]][nPoints[2]][nPoints[3]];
               for(int i = 0; i < nPoints[0]; i++)
                  for(int j = 0; j < nPoints[1]; j++)
                     for(int k = 0; k < nPoints[2]; k++)
                        for(int m = 0; m < nPoints[3]; m++)
                           table4d[i][j][k][m] = s.nextDouble();
               break;
         }
      }
      finally {
         s.close();
      }
   }
}
