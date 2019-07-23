package flanagan.interpolation;

import flanagan.math.Fmath;

/*******************************************************************************
 * <h3>TriCubicSpline.java</h3>
 * <p>
 * Class for performing an interpolation on the tabulated function y =
 * f(x1,x2,x3) using a natural tricubic spline Assumes second derivatives at end
 * points = 0 (natural spine)
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
public class TriCubicSpline {

   private int nPoints = 0; // no. of x1 tabulated points
   private int mPoints = 0; // no. of x2 tabulated points
   private int lPoints = 0; // no. of x3 tabulated points
   private double[][][] y = null; // y=f(x1,x2) tabulated function
   private double[] x1 = null; // x1 in tabulated function f(x1,x2,x3)
   private double[] x2 = null; // x2 in tabulated function f(x1,x2,x3)
   private double[] x3 = null; // x3 in tabulated function f(x1,x2,x3)
   private final double[] xMin = new double[3]; // minimum values of x1, x2 and
   // x3
   private final double[] xMax = new double[3]; // maximum values of x1, x2 and
   // x3
   private BiCubicSpline[] bcsn = null;// nPoints array of BiCubicSpline
   // instances
   private CubicSpline csm = null; // CubicSpline instance
   private double[][][] d2ydx2inner = null; // inner matrix of second
   // derivatives
   private boolean derivCalculated = false; // = true when the called bicubic
   // spline derivatives have been
   // calculated
   private String subMatrixIndices = " "; // String of indices of the
   // submatrices that have called
   // TriCubicSpline if called from
   // higher dimension interpolation
   @SuppressWarnings("unused")
   private boolean averageIdenticalAbscissae = false; // if true: the the

   // ordinate values for
   // identical abscissae are
   // averaged

   // If false: the abscissae values are separated by 0.001 of the total
   // abscissae range;

   // Constructor
   public TriCubicSpline(double[] x1, double[] x2, double[] x3, double[][][] y) {
      this.nPoints = x1.length;
      this.mPoints = x2.length;
      this.lPoints = x3.length;
      if(this.nPoints != y.length)
         throw new IllegalArgumentException("Arrays x1 and y-row are of different length " + this.nPoints + " " + y.length);
      if(this.mPoints != y[0].length)
         throw new IllegalArgumentException("Arrays x2 and y-column are of different length " + this.mPoints + " "
               + y[0].length);
      if(this.lPoints != y[0][0].length)
         throw new IllegalArgumentException("Arrays x3 and y-column are of different length " + this.mPoints + " "
               + y[0][0].length);
      if((this.nPoints < 3) || (this.mPoints < 3) || (this.lPoints < 3))
         throw new IllegalArgumentException("The tabulated 3D array must have a minimum size of 3 X 3 X 3");

      this.csm = new CubicSpline(this.nPoints);
      this.bcsn = BiCubicSpline.oneDarray(this.nPoints, this.mPoints, this.lPoints);
      this.x1 = new double[this.nPoints];
      this.x2 = new double[this.mPoints];
      this.x3 = new double[this.lPoints];
      this.y = new double[this.nPoints][this.mPoints][this.lPoints];
      this.d2ydx2inner = new double[this.nPoints][this.mPoints][this.lPoints];
      for(int i = 0; i < this.nPoints; i++)
         this.x1[i] = x1[i];
      this.xMin[0] = Fmath.minimum(this.x1);
      this.xMax[0] = Fmath.maximum(this.x1);
      for(int j = 0; j < this.mPoints; j++)
         this.x2[j] = x2[j];
      this.xMin[1] = Fmath.minimum(this.x2);
      this.xMax[1] = Fmath.maximum(this.x2);
      for(int j = 0; j < this.lPoints; j++)
         this.x3[j] = x3[j];
      this.xMin[2] = Fmath.minimum(this.x3);
      this.xMax[2] = Fmath.maximum(this.x3);
      for(int i = 0; i < this.nPoints; i++)
         for(int j = 0; j < this.mPoints; j++)
            for(int k = 0; k < this.lPoints; k++)
               this.y[i][j][k] = y[i][j][k];
   }

   // Constructor with data arrays initialised to zero
   // Primarily for use by QuadriCubicSpline
   public TriCubicSpline(int nP, int mP, int lP) {
      this.nPoints = nP;
      this.mPoints = mP;
      this.lPoints = lP;
      if((this.nPoints < 3) || (this.mPoints < 3) || (this.lPoints < 3))
         throw new IllegalArgumentException("The data matrix must have a minimum size of 3 X 3 X 3");

      this.csm = new CubicSpline(this.nPoints);
      this.bcsn = BiCubicSpline.oneDarray(this.nPoints, this.mPoints, this.lPoints);
      this.x1 = new double[this.nPoints];
      this.x2 = new double[this.mPoints];
      this.x3 = new double[this.lPoints];
      this.y = new double[this.nPoints][this.mPoints][this.lPoints];
      this.d2ydx2inner = new double[this.nPoints][this.mPoints][this.lPoints];
   }

   // METHODS

   // Reset the default handing of identical abscissae with different ordinates
   // from the default option of separating the two relevant abscissae by 0.001
   // of the range
   // to avraging the relevant ordinates
   public void averageIdenticalAbscissae() {
      this.averageIdenticalAbscissae = true;
      for(final BiCubicSpline element : this.bcsn)
         element.averageIdenticalAbscissae();
      this.csm.averageIdenticalAbscissae();
   }

   // Returns a new TriCubicSpline setting internal array size to nP x mP x lP
   // and all array values to zero with natural spline default
   // Primarily for use in this.oneDarray for QuadriCubicSpline
   public static TriCubicSpline zero(int nP, int mP, int lP) {
      if((nP < 3) || (mP < 3) || (lP < 3))
         throw new IllegalArgumentException("A minimum of three x three x three data points is needed");
      final TriCubicSpline aa = new TriCubicSpline(nP, mP, lP);
      return aa;
   }

   // Create a one dimensional array of TriCubicSpline objects of length nP each
   // of internal array size mP x lP xkP
   // Primarily for use in quadriCubicSpline
   public static TriCubicSpline[] oneDarray(int nP, int mP, int lP, int kP) {
      if((mP < 3) || (lP < 3) || (kP < 3))
         throw new IllegalArgumentException("A minimum of three x three x three data points is needed");
      final TriCubicSpline[] a = new TriCubicSpline[nP];
      for(int i = 0; i < nP; i++)
         a[i] = TriCubicSpline.zero(mP, lP, kP);
      return a;
   }

   // Resets the x1, x2, x3, y data arrays
   // Primarily for use in QuadriCubicSpline
   public void resetData(double[] x1, double[] x2, double[] x3, double[][][] y) {
      if(x1.length != y.length)
         throw new IllegalArgumentException("Arrays x1 and y row are of different length");
      if(x2.length != y[0].length)
         throw new IllegalArgumentException("Arrays x2 and y column are of different length");
      if(x3.length != y[0][0].length)
         throw new IllegalArgumentException("Arrays x3 and y column are of different length");
      if(this.nPoints != x1.length)
         throw new IllegalArgumentException("Original array length not matched by new array length");
      if(this.mPoints != x2.length)
         throw new IllegalArgumentException("Original array length not matched by new array length");
      if(this.lPoints != x3.length)
         throw new IllegalArgumentException("Original array length not matched by new array length");

      for(int i = 0; i < this.nPoints; i++)
         this.x1[i] = x1[i];
      this.xMin[0] = Fmath.minimum(this.x1);
      this.xMax[0] = Fmath.maximum(this.x1);

      for(int i = 0; i < this.mPoints; i++)
         this.x2[i] = x2[i];
      this.xMin[1] = Fmath.minimum(this.x2);
      this.xMax[1] = Fmath.maximum(this.x2);

      for(int i = 0; i < this.lPoints; i++)
         this.x3[i] = x3[i];
      this.xMin[2] = Fmath.minimum(this.x3);
      this.xMax[2] = Fmath.maximum(this.x3);

      for(int i = 0; i < this.nPoints; i++)
         for(int j = 0; j < this.mPoints; j++)
            for(int k = 0; k < this.lPoints; k++)
               this.y[i][j][k] = y[i][j][k];
   }

   // Set sub-matrix indices - for use with higher dimesion interpolations
   // calling TriCubicSpline
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
      final double[] limits = {
         xMin[0],
         xMax[0],
         xMin[1],
         xMax[1],
         xMin[2],
         xMax[2]
      };
      return limits;
   }

   // Display limits to x
   public void displayLimits() {
      System.out.println(" ");
      for(int i = 0; i < 3; i++)
         System.out.println("The limits to the x array " + i + " are " + xMin[i] + " and " + xMax[i]);
      System.out.println(" ");
   }

   // Returns an interpolated value of y for values of x1, x2 and x3
   // from a tabulated function y=f(x1,x2,x3)
   public double interpolate(double xx1, double xx2, double xx3) {

      final double[][] yTempml = new double[this.mPoints][this.lPoints];
      for(int i = 0; i < this.nPoints; i++) {
         String workingIndices = new String(subMatrixIndices);
         workingIndices += "TriCubicSpline rows  " + i;

         for(int j = 0; j < this.mPoints; j++) {
            workingIndices += ",  " + j + ": ";
            for(int k = 0; k < this.lPoints; k++)
               yTempml[j][k] = y[i][j][k];
         }
         this.bcsn[i].setSubMatrix(workingIndices);
         this.bcsn[i].resetData(x2, x3, yTempml);
      }
      final double[] yTempm = new double[nPoints];

      for(int i = 0; i < nPoints; i++) {
         if(this.derivCalculated)
            this.bcsn[i].setDeriv(this.d2ydx2inner[i]);
         yTempm[i] = this.bcsn[i].interpolate(xx2, xx3);
         if(!this.derivCalculated)
            this.d2ydx2inner[i] = this.bcsn[i].getDeriv();
      }
      derivCalculated = true;

      String workingIndices = new String(subMatrixIndices);
      workingIndices += "TriCubicSpline interpolated column:  ";
      this.csm.setSubMatrix(workingIndices);
      this.csm.resetData(x1, yTempm);

      return this.csm.interpolate(xx1);
   }

   // Get inner matrix of derivatives
   // Primarily used by QuadriCubicSpline
   public double[][][] getDeriv() {
      return this.d2ydx2inner;
   }

   // Set inner matrix of derivatives
   // Primarily used by QuadriCubicSpline
   public void setDeriv(double[][][] d2ydx2) {
      this.d2ydx2inner = d2ydx2;
      this.derivCalculated = true;
   }
}
