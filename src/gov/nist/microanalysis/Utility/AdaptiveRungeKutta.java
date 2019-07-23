package gov.nist.microanalysis.Utility;

/**
 * <p>
 * An adaptive step size Runge-Kutta algorithm for numerically evaluating
 * differential equations. This implementation can optionally save intermediate
 * points along the ODE trajectory at a user specified interval. Using this
 * option may limit the step size and thus
 * </p>
 * <p>
 * See Press, Teulolsky, Vetterling &amp; Flannery, Numerical Recipes in C,
 * Second Edition pp 714-722
 * </p>
 * <p>
 * Example:<br>
 * </p>
 * 
 * <pre>
 * AdaptiveRungeKutta trial = new AdaptiveRungeKutta(2) {
 *    void derivatives(double x, double[] y, double[] dydx) {
 *       dydx[0] = -Math.sin(x);
 *       dydx[1] = Math.cos(x);
 *    }
 * };
 * </pre>
 * 
 * <pre>
 * try {
 *    double[] yst = {
 *       1.0,
 *       0.0
 *    };
 *    trial.setSaveInterval(Math.PI / 16.0);
 *    trial.integrate(0.0, 2.0 * Math.PI, yst, 1.0e-6, 0.01);
 * }
 * catch(UtilException ex) {
 *    System.err.println(ex.toString());
 * }
 * for(int i = 0; i &lt; trial.getNSaved(); ++i)
 *    System.out.println(trial.getX(i) + &quot;\t&quot; + trial.getY(i)[0] + &quot;\t&quot; + trial.getY(i)[1]);
 * </pre>
 * <p>
 * NOTE: This algorithm is not thread-safe. Use each instance in one and only
 * one thread.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

abstract public class AdaptiveRungeKutta {
   private final int mNVariables; // The number of differential equations
   private double mHDid; // Actual step size accomplished in last call to
   // qcStep
   private double mHNext; // Next step size to try when calling qcStep
   private double mSaveInterval = Double.MAX_VALUE;
   private double mMinStepSize = 0.0;
   private double[] mXSave;
   private double[][] mYSave;
   private int mNSaved = 0;
   private int mMaxSteps = 10000;
   private int mNOk; // Number of ok steps
   private int mNBad; // Number of repeated steps
   // Temporary work space used by baseStep
   private double[] mWs2, mWs3, mWs4, mWs5, mWs6, mYTemp;
   // Temporary work space used by qcStep
   private double[] mYErr, mQcYTemp;

   private double sign(double magnitude, double sign) {
      return sign >= 0.0 ? Math.abs(magnitude) : -Math.abs(magnitude);
   }

   /**
    * baseStep - Take a single Cash-Karp Runge-Kutta step. Given the
    * n=mNDimensions values y[0..n-1] and their derivatives dydx[0..n-1] know at
    * x, use a fifth order Cash-Karp Runge-Kutta method to advance the solution
    * over an interval h. The resulting y value is returned in yout. An estimate
    * of the truncation error is returned in yerr.
    * 
    * @param x double
    * @param y double[]
    * @param dydx double[]
    * @param h double
    * @param yout double[]
    */
   private void baseStep(double x, double[] y, double[] dydx, double h, double[] yout, double[] yerr) {
      final double a2 = 0.2, a3 = 0.3, a4 = 0.6, a5 = 1.0, a6 = 0.875;
      final double b21 = 0.2;
      final double b31 = 3.0 / 40.0, b32 = 9.0 / 40.0;
      final double b41 = 0.3, b42 = -0.9, b43 = 1.2;
      final double b51 = -11.0 / 54.0, b52 = 2.5, b53 = -70.0 / 27.0, b54 = 35.0 / 27.0;
      final double b61 = 1631.0 / 55296.0, b62 = 175.0 / 512.0, b63 = 575.0 / 13824.0, b64 = 44275.0 / 110592.0, b65 = 253.0 / 4096.0;
      final double c1 = 37.0 / 378.0, c3 = 250.0 / 621.0, c4 = 125.0 / 594.0, c6 = 512.0 / 1771.0;
      final double dc1 = c1 - (2825.0 / 27648.0), dc3 = c3 - (18575.0 / 48384.0), dc4 = c4 - (13525.0 / 55296.0), dc5 = -277.0 / 14336.0, dc6 = c6 - 0.25;
      // Workspace
      if(mWs2 == null) {
         mWs2 = new double[mNVariables];
         mWs3 = new double[mNVariables];
         mWs4 = new double[mNVariables];
         mWs5 = new double[mNVariables];
         mWs6 = new double[mNVariables];
         mYTemp = new double[mNVariables];
      }
      // First step
      for(int i = 0; i < mNVariables; ++i)
         mYTemp[i] = y[i] + (b21 * h * dydx[i]);
      // Second step
      derivatives(x + (a2 * h), mYTemp, mWs2);
      for(int i = 0; i < mNVariables; ++i)
         mYTemp[i] = y[i] + (h * ((b31 * dydx[i]) + (b32 * mWs2[i])));
      // Third step
      derivatives(x + (a3 * h), mYTemp, mWs3);
      for(int i = 0; i < mNVariables; ++i)
         mYTemp[i] = y[i] + (h * ((b41 * dydx[i]) + (b42 * mWs2[i]) + (b43 * mWs3[i])));
      // Fourth step
      derivatives(x + (a4 * h), mYTemp, mWs4);
      for(int i = 0; i < mNVariables; ++i)
         mYTemp[i] = y[i] + (h * ((b51 * dydx[i]) + (b52 * mWs2[i]) + (b53 * mWs3[i]) + (b54 * mWs4[i])));
      // Fifth step
      derivatives(x + (a5 * h), mYTemp, mWs5);
      for(int i = 0; i < mNVariables; ++i)
         mYTemp[i] = y[i] + (h * ((b61 * dydx[i]) + (b62 * mWs2[i]) + (b63 * mWs3[i]) + (b64 * mWs4[i]) + (b65 * mWs5[i])));
      // Sixth step
      derivatives(x + (a6 * h), mYTemp, mWs6);
      for(int i = 0; i < mNVariables; ++i)
         yout[i] = y[i] + (h * ((c1 * dydx[i]) + (c3 * mWs3[i]) + (c4 * mWs4[i]) + (c6 * mWs6[i])));
      // Estimate the error
      for(int i = 0; i < mNVariables; ++i)
         yerr[i] = h * ((dc1 * dydx[i]) + (dc3 * mWs3[i]) + (dc4 * mWs4[i]) + (dc5 * mWs5[i]) + (dc6 * mWs6[i]));
   }

   /**
    * qcStep - Take a fifth order Runge-Kutta step with monitoring of local
    * truncation error. Input are the dependent variable y[0..mNDimensions-1]
    * and its derivatives dydx[0..mNDimensions-1] at the starting value of the
    * independent variable x. Also input is the attempted step size htry, the
    * required accuracy eps and the vector yscal against which the errors are
    * scaled. Upon return, y is replaced with the new values, x is returned and
    * mHDid and mHNext are set to the actual step size and the size of the next
    * step to try.
    * 
    * @param x double - (In) independent variable
    * @param y double[] - (In,Out) dependent variable
    * @param dydx double[] - (In) derivative at x
    * @param htry double - The step size to attempt
    * @param eps double - Desired accuracy
    * @param yscal double[] - (In) Error scaling vector
    * @throws UtilException - When the step size becomes too small
    * @return double - The new value of x
    */
   private double qcStep(double x, double[] y, double[] dydx, double htry, double eps, double[] yscal)
         throws UtilException {
      final double safety = 0.9;
      final double pgrow = -0.2;
      final double pshrnk = -0.25;
      final double errcon = 1.89e-4;
      if(mYErr == null) {
         mYErr = new double[mNVariables];
         mQcYTemp = new double[mNVariables];
      }
      double errmax, h = htry;
      do {
         baseStep(x, y, dydx, h, mQcYTemp, mYErr);
         errmax = 0.0;
         for(int i = 0; i < mNVariables; ++i)
            errmax = Math.max(errmax, Math.abs(mYErr[i] / yscal[i]));
         errmax /= eps;
         if(errmax > 1.0) {
            final double htemp = safety * h * Math.pow(errmax, pshrnk);
            h = (h >= 0 ? Math.max(htemp, 0.1 * h) : Math.min(htemp, 0.1 * h));
            // Check for step size underflow
            final double xnew = x + h;
            if(xnew == x)
               throw new UtilException("Step size underflow in AdaptiveRungeKutta.qcStep.");
         }
      } while(errmax > 1.0);
      mHNext = (errmax > errcon ? safety * h * Math.pow(errmax, pgrow) : 5.0 * h);
      mHDid = h;
      x += h;
      System.arraycopy(mQcYTemp, 0, y, 0, mNVariables);
      return x;
   }

   /**
    * clearWorkspace - null all temporary space to free memory
    */
   private void clearWorkspace() {
      mWs2 = null;
      mWs3 = null;
      mWs4 = null;
      mWs5 = null;
      mWs6 = null;
      mYTemp = null;
      mYErr = null;
      mQcYTemp = null;
   }

   /**
    * AdaptiveRungeKutta - Construct an AdaptiveRungeKutta object to solve a
    * differential equation of nVars variables. The implementation of
    * derivatives should return nVars derivative values for each x &amp; y.
    * 
    * @param nVars int
    */
   public AdaptiveRungeKutta(int nVars) {
      super();
      mNVariables = nVars;
   }

   /**
    * setSaveInterval - Set the interval on which to save intermediate points on
    * the integrated trajectory. (Use clearSaveInterval to not save any
    * intermediate points.) Note: The default is not to save any intermediate
    * points.
    * 
    * @param interval double
    */
   public void setSaveInterval(double interval) {
      mSaveInterval = Math.abs(interval);
   }

   /**
    * clearSaveInterval - Return to the default of not saving any intermediate
    * points.
    */
   public void clearSaveInterval() {
      mSaveInterval = Double.MAX_VALUE;
   }

   /**
    * getNSaved - Returns the number of saved values.
    * 
    * @return int
    */
   public int getNSaved() {
      return mNSaved;
   }

   /**
    * getX - Returns the x-coordinate of the i-th saved value
    * 
    * @param i int - Where i&lt;getNSaved()
    * @return double
    */
   public double getX(int i) {
      return mXSave[i];
   }

   /**
    * getY - returns the getNVariable x y-coordinates of the i-th saved values.
    * 
    * @param i int - Where i&lt;getNSaved()
    * @return double[] - Of dimension getNVariables
    */
   public double[] getY(int i) {
      return mYSave[i];
   }

   /**
    * setMaxSteps - Set the maximum number of ODE steps to allow. Default is
    * 10000.
    * 
    * @param maxSteps int
    */
   public void setMaxSteps(int maxSteps) {
      mMaxSteps = maxSteps;
   }

   /**
    * setMinStepSize - Sets the minimum permissible step size. Default is 0.0.
    * 
    * @param minStep double
    */
   public void setMinStepSize(double minStep) {
      mMinStepSize = Math.abs(minStep);
   }

   /**
    * getNVariables - Returns the number of variables as set in the constructor.
    * 
    * @return int
    */
   public int getNVariables() {
      return mNVariables;
   }

   /**
    * getStepCount - Get the total number of steps required to perform the
    * previous integrate operation.
    * 
    * @return int
    */
   public int getStepCount() {
      return mNOk + mNBad;
   }

   /**
    * getGoodStepCount - Get the number of steps leading to results of the
    * desired accuracy.
    * 
    * @return int
    */
   public int getGoodStepCount() {
      return mNOk;
   }

   /**
    * getBadStepCount - Get the number of steps that were needed to be
    * subdivided to attain results of the desired accuracy.
    * 
    * @return int
    */
   public int getBadStepCount() {
      return mNBad;
   }

   /**
    * integrate - Integrate the ODE specified by derivatives using the adaptive
    * step size Runge-Kutta algorithm over the independent variable interval x1
    * to x2. ystart contains the initial y values. eps is measure of the
    * permissible error. h is the initial step size.
    * 
    * @param x1 double - Start of the integration range
    * @param x2 double - End of the integration range
    * @param ystart double[] - (In &amp; out) The initial y value
    * @param eps double - The permissible relative error
    * @param h1 double - The initial step size
    * @return The final y values as an array of length getNVariables().
    * @throws UtilException - Upon too many steps or too small a step
    */
   public double[] integrate(double x1, double x2, double[] ystart, double eps, double h1)
         throws UtilException {
      final double tiny = 1.0e-10 * eps;
      final double[] yscal = new double[mNVariables];
      final double[] dydx = new double[mNVariables];
      final double[] y = new double[mNVariables];
      double x = x1;
      double h = sign(h1, x2 - x1);
      double xsav = 0.0;
      double saveInt = Double.MAX_VALUE;
      int kMax = 0;
      mNSaved = 0;
      mNOk = 0;
      mNBad = 0;
      System.arraycopy(ystart, 0, y, 0, mNVariables);
      if(mSaveInterval != Double.MAX_VALUE) {
         kMax = (int) Math.round((Math.abs(x2 - x1) + mSaveInterval) / mSaveInterval);
         saveInt = sign(mSaveInterval, x2 - x1);
         xsav = x - (2.0 * saveInt); // to ensure that the first step is
                                     // saved...
         mXSave = new double[kMax];
         mYSave = new double[kMax][mNVariables];
      }
      for(int step = 0; step < mMaxSteps; ++step) {
         // Save the necessary points
         if((kMax != 0) && (mNSaved < kMax) && (Math.abs(x - xsav) >= (0.9999 * mSaveInterval))) {
            mXSave[mNSaved] = x;
            System.arraycopy(y, 0, mYSave[mNSaved], 0, mNVariables);
            xsav = x;
            ++mNSaved;
         }
         derivatives(x, y, dydx);
         // Rescale h to ensure we hit desired points
         final double hMax = Math.abs((xsav + saveInt) - x);
         if(Math.abs(h) > hMax)
            h = sign(hMax, h);
         // Scaling to monitor accuracy...
         for(int i = 0; i < mNVariables; ++i)
            yscal[i] = Math.abs(y[i]) + Math.abs(dydx[i] * h) + tiny;
         if((((x + h) - x2) * ((x + h) - x1)) > 0.0)
            h = x2 - x;
         x = qcStep(x, y, dydx, h, eps, yscal);
         if(mHDid == h)
            ++mNOk;
         else
            ++mNBad;
         if(((x - x2) * (x2 - x1)) >= 0.0) {
            System.arraycopy(y, 0, ystart, 0, mNVariables);
            if(kMax != 0) {
               mNSaved = Math.min(mNSaved, kMax - 1);
               mXSave[mNSaved] = x;
               System.arraycopy(y, 0, mYSave[mNSaved], 0, mNVariables);
               ++mNSaved;
            }
            clearWorkspace();
            return y;
         }
         if(Math.abs(mHNext) <= mMinStepSize)
            throw new UtilException("Step size too small in AdaptiveRungeKutta.integrate");
         h = mHNext;
      }
      throw new UtilException("Too many steps in AdaptiveRungeKutta.integrate");
   }

   /**
    * derivatives - The derived class provides an implementation of the
    * derivatives function. x &amp; y[] are input and the user provided
    * implementation of derivatives is resposible for returning the derivatives
    * in the array dydx. The lengths of y and dydx are equal to mNDimensions.
    * 
    * @param x double - In
    * @param y double[] - In (of dimension mNDimensions)
    * @param dydx double[] - Out (of dimension mNDimensions)
    */
   abstract public void derivatives(double x, double[] y, double[] dydx);

}
