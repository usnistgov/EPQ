package flanagan.interpolation;

import flanagan.math.Fmath;

/*******************************************************************************
 * <h3>BiCubicSpline.java</h3>
 * <p>
 * Class for performing an interpolation on the tabulated function y = f(x1,x2)
 * using a natural bicubic spline Assumes second derivatives at end points = 0
 * (natural spine)
 * </p>
 * <p>
 * WRITTEN BY: Dr Michael Thomas Flanagan<br>
 * DATE: May 2002<br>
 * UPDATE: 20 May 2003, 17 February 2006, 27 July 2007, 4 December 2007
 * </p>
 * <p>
 * DOCUMENTATION:<br>
 * See Michael Thomas Flanagan's Java library on-line web page:<br>
 * http://www.ee.ucl.ac.uk/~mflanaga/java/BiCubicSpline.html<br>
 * http://www.ee.ucl.ac.uk/~mflanaga/java/
 * </p>
 * <p>
 * Copyright (c) May 2003, February 2006 Michael Thomas Flanagan
 * </p>
 * <h3>PERMISSION TO COPY:</h3>
 * <p>
 * Permission to use, copy and modify this software and its documentation for
 * NON-COMMERCIAL purposes is granted, without fee, provided that an
 * acknowledgement to the author, Michael Thomas Flanagan at
 * www.ee.ucl.ac.uk/~mflanaga, appears in all copies.
 * </p>
 * <p>
 * Dr Michael Thomas Flanagan makes no representations about the suitability or
 * fitness of the software for any or for a particular purpose. Michael Thomas
 * Flanagan shall not be liable for any damages suffered as a result of using,
 * modifying or distributing this software or its derivatives.
 * </p>
 ******************************************************************************/
public class BiCubicSpline {

   private int nPoints = 0; // no. of x1 tabulated points
   private int mPoints = 0; // no. of x2 tabulated points
   private double[][] y = null; // y=f(x1,x2) tabulated function
   private double[] x1 = null; // x1 in tabulated function f(x1,x2)
   private double[] x2 = null; // x2 in tabulated function f(x1,x2)
   private final double[] xMin = new double[2]; // minimum values of x1 and x2
   private final double[] xMax = new double[2]; // maximum values of x1 and x2
   private double[][] d2ydx2inner = null; // second derivatives of first called
   // array of cubic splines
   private CubicSpline csn[] = null; // nPoints array of CubicSpline instances
   private CubicSpline csm = null; // CubicSpline instance
   private boolean derivCalculated = false; // = true when the first called
   // cubic spline derivatives have
   // been calculated
   private String subMatrixIndices = " "; // String of indices of the
   // submatrices that have called
   // BiCubicSpline if called from higher
   // dimension interpolation
   @SuppressWarnings("unused")
   private boolean averageIdenticalAbscissae = false; // if true: the the

   // ordinate values for
   // identical abscissae are
   // averaged

   // If false: the abscissae values are separated by 0.001 of the total
   // abscissae range;

   // Constructor
   // Constructor with data arrays initialised to arrays x and y
   public BiCubicSpline(double[] x1, double[] x2, double[][] y) {
      this.nPoints = x1.length;
      this.mPoints = x2.length;
      if (this.nPoints != y.length)
         throw new IllegalArgumentException("Arrays x1 and y-row are of different length " + this.nPoints + " " + y.length);
      if (this.mPoints != y[0].length)
         throw new IllegalArgumentException("Arrays x2 and y-column are of different length " + this.mPoints + " " + y[0].length);
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
      this.xMin[0] = Fmath.minimum(this.x1);
      this.xMax[0] = Fmath.maximum(this.x1);
      for (int j = 0; j < this.mPoints; j++)
         this.x2[j] = x2[j];
      this.xMin[1] = Fmath.minimum(this.x2);
      this.xMax[1] = Fmath.maximum(this.x2);
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

   // Reset the default handing of identical abscissae with different ordinates
   // from the default option of separating the two relevant abscissae by 0.001
   // of the range
   // to avraging the relevant ordinates
   public void averageIdenticalAbscissae() {
      this.averageIdenticalAbscissae = true;
      for (final CubicSpline element : this.csn)
         element.averageIdenticalAbscissae();
      this.csm.averageIdenticalAbscissae();
   }

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

   // Set sub-matrix indices - for use with higher dimesion interpolations
   // calling BiCubicSpline
   public void setSubMatrix(String subMatrixVector) {
      this.subMatrixIndices = subMatrixVector;
   }

   // Get minimum limits
   public double[] getXmin() {
      return this.xMin;
   }

   // Get maximum limits
   public double[] getXmax() {
      return this.xMax;
   }

   // Get limits to x
   public double[] getLimits() {
      final double[] limits = {xMin[0], xMax[0], xMin[1], xMax[1]};
      return limits;
   }

   // Display limits to x
   public void displayLimits() {
      System.out.println(" ");
      for (int i = 0; i < 2; i++)
         System.out.println("The limits to the x array " + i + " are " + xMin[i] + " and " + xMax[i]);
      System.out.println(" ");
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
         String workingIndices = new String(subMatrixIndices);
         workingIndices += "BiCubicSpline row  " + i + ": ";
         this.csn[i].setSubMatrix(workingIndices);
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

      String workingIndices = new String(subMatrixIndices);
      workingIndices += "BiCubicSpline interpolated column:  ";
      this.csm.setSubMatrix(workingIndices);
      this.csm.resetData(x1, yTempm);
      return this.csm.interpolate(xx1);
   }
}
