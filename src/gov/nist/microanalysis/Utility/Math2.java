package gov.nist.microanalysis.Utility;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Random;

import gov.nist.microanalysis.EPQLibrary.EPQException;

import Jama.Matrix;

/**
 * <p>
 * Useful math functions not provided in the standard libraries.
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

public class Math2 {

   public static final double[] ORIGIN_3D = Math2.v3(0.0, 0.0, 0.0);
   public static final double[] ONE = Math2.v3(1.0, 1.0, 1.0);
   public static final double[] X_AXIS = Math2.v3(1.0, 0.0, 0.0);
   public static final double[] Y_AXIS = Math2.v3(0.0, 1.0, 0.0);
   public static final double[] Z_AXIS = Math2.v3(0.0, 0.0, 1.0);
   public static final double[] MINUS_X_AXIS = Math2.v3(-1.0, 0.0, 0.0);
   public static final double[] MINUS_Y_AXIS = Math2.v3(0.0, -1.0, 0.0);
   public static final double[] MINUS_Z_AXIS = Math2.v3(0.0, 0.0, -1.0);

   /**
    * SQRT_PI - The square root of Pi ~ 1.772
    */
   public static final double SQRT_PI = Math.sqrt(Math.PI);

   /**
    * A random number generator. It is invoked by, e.g., rgen.nextDouble(). By
    * default it functions similarly to Math.double(), with an initial seed
    * produced by a procedure unlikely to give the same value under repeated
    * invocations.
    */
   public static Random rgen = new Random();

   /**
    * initializeRandom - When called with an argument of type long, it
    * initializes the Math2.rgen random number generator with that seed value.
    * When called without an argument it sets the seed using a procedure
    * unlikely to give the same value under repeated invocations.
    * 
    * @param seed Seed for {@link Random} constructor
    */
   public static void initializeRandom(long seed) {
      rgen = new Random(seed);
   }

   public static void initializeRandom() {
      rgen = new Random();
   }

   /**
    * sqr - returns x*x
    * 
    * @param x double
    * @return double
    */
   public static final double sqr(double x) {
      return x * x;
   }

   /**
    * erf - The error function (2/sqrt(pi))*Integrate[Exp[-t^2],{t,0,x}] <br>
    * Source: Numerical Recipes in C - 2nd Edition
    * 
    * @param x double
    * @return double
    */
   public static final double erf(double x) {
      return x < 0.0 ? -gammap(0.5, x * x) : gammap(0.5, x * x);
   }

   /**
    * erfc - The complementary error function (1-erf(x)) <br>
    * Source: Numerical Recipes in C - 2nd Edition
    * 
    * @param x double
    * @return double
    */
   public static final double erfc(double x) {
      return x < 0.0 ? 1.0 + gammap(0.5, x * x) : gammq(0.5, x * x);
   }

   /**
    * gammaq - The incomplete gamma function Q(a,x) = 1 - P(a,x) <br>
    * Source: Numerical Recipes in C - 2nd Edition
    * 
    * @param a double
    * @param x double
    * @return double
    */
   public static final double gammq(double a, double x) {
      assert (x >= 0.0);
      assert (a > 0.0);
      if(x < (a + 1.0))
         return 1.0 - gser(a, x);
      else
         return gcf(a, x);
   }

   /**
    * gammap - Computes the incomplete gamma function P(a,x) by selecting
    * between series and continued fraction representations based on the size of
    * the arguments. <br>
    * Source: Numerical Recipes in C - 2nd Edition
    * 
    * @param a double
    * @param x double
    * @return double
    */
   public static final double gammap(double a, double x) {
      assert (x >= 0.0);
      assert (a > 0.0);
      if(x < (a + 1.0))
         return gser(a, x);
      else
         return 1.0 - gcf(a, x);
   }

   /**
    * chiSquaredConfidenceLevel - Computes the table in Press et al 15.6 which
    * displays dChiSq as a function of confidence level and degrees of freedom.
    * 
    * @param confidence double on (0,1)
    * @param degreesOfFreedom int &gt; 0
    * @return double
    */
   public static double chiSquaredConfidenceLevel(double confidence, int degreesOfFreedom) {
      assert (confidence > 0.0) && (confidence < 1.0) : "Confidence must be in the range (0, 1).";
      assert degreesOfFreedom > 0 : "Degrees of freedom must be 1 or larger.";
      if((confidence < 0.0) || (confidence >= 1.0) || (degreesOfFreedom <= 0))
         return Double.NaN;
      final FindRoot fr = new FindRoot() {
         double mDegreesOfFreedom;
         double mConfidenceLimit;

         @Override
         public void initialize(double[] dd) {
            mDegreesOfFreedom = dd[0];
            mConfidenceLimit = dd[1];
         }

         @Override
         public double function(double x0) {
            return gammap(0.5 * mDegreesOfFreedom, 0.5 * x0) - mConfidenceLimit;
         }
      };
      fr.initialize(new double[] {
         degreesOfFreedom,
         confidence
      });
      return fr.perform(1.0, (2.0 * degreesOfFreedom) + 50.0, 1.0e-3, 100);
   }

   /**
    * gser - Calculates the incomplete gamma function P(a,x) by evaluating the
    * series representation. <br>
    * Source: Numerical Recipes in C - 2nd Edition
    * 
    * @param a double
    * @param x double
    * @return double
    */
   private static final double gser(double a, double x) {
      assert (x >= 0.0);
      final double ITMAX = 100;
      final double EPS = 3.0e-7;
      if(x == 0.0)
         return 0.0;
      else {
         double ap = a;
         double sum = 1.0 / a;
         double del = sum;
         for(int n = 1; n <= ITMAX; ++n) {
            ++ap;
            del *= x / ap;
            sum += del;
            if(Math.abs(del) < (Math.abs(sum) * EPS))
               return sum * Math.exp((-x + (a * Math.log(x))) - gammaln(a));
         }
         assert false : "a too large, ITMAX too small in routine gser";
         return sum * Math.exp((-x + (a * Math.log(x))) - gammaln(a));
      }
   }

   /**
    * gcf - Implements the incomplete gamma function Q(a,x) evaluated as a
    * continued fraction. <br>
    * Source: Numerical Recipes in C - 2nd Edition
    * 
    * @param a double
    * @param x double
    * @return double
    */
   private static final double gcf(double a, double x) {
      final double ITMAX = 100;
      final double EPS = 3.0e-7;
      final double FPMIN = 1.0e-30;
      double b = (x + 1.0) - a;
      double c = 1.0 / FPMIN;
      double d = 1.0 / b;
      double h = d;
      for(int i = 1; i <= ITMAX; ++i) {
         final double an = -i * (i - a);
         b += 2.0;
         d = (an * d) + b;
         if(Math.abs(d) < FPMIN)
            d = FPMIN;
         c = b + (an / c);
         if(Math.abs(c) < FPMIN)
            c = FPMIN;
         d = 1.0 / d;
         final double del = d * c;
         h *= del;
         if(Math.abs(del - 1.0) < EPS)
            break;
         assert i != ITMAX : "a too large, ITMAX too small in gcf";
      }
      return Math.exp((-x + (a * Math.log(x))) - gammaln(a)) * h;
   }

   /**
    * gammaln - The natural log of the gamma function. <br>
    * Source: Numerical Recipes in C - 2nd Edition
    * 
    * @param xx double
    * @return double
    */
   public static final double gammaln(double xx) {
      // Coefficients used by gammaln
      final double[] coeff = {
         76.18009172947146,
         -86.50532032941677,
         24.01409824083091,
         -1.231739572450155,
         0.1208650973866179e-2,
         -0.5395239384953e-5
      };
      double y = xx;
      double tmp = xx + 5.5;
      tmp -= (xx + 0.5) * Math.log(tmp);
      double ser = 1.000000000190015;
      for(int j = 0; j <= 5; ++j) {
         y += 1.0;
         ser += coeff[j] / y;
      }
      return -tmp + Math.log((2.5066282746310005 * ser) / xx);
   }

   /**
    * expRand - Selects a random value from an exponential distibution. The mean
    * value returned is 1.0.
    * 
    * @return double - Returns a random variable in the range [0,infinity)
    */
   static final public double expRand() {
      return -Math.log(rgen.nextDouble());
   }

   /**
    * expRand - Selects a random value from an exponential distibution. The mean
    * value returned is 1.0.
    *
    * @param lambda rate (mean = 1.0/lambda)
    * @return double - Returns a random variable in the range [0,infinity)
    */
   static final public double expRand(final double lambda) {
      return -Math.log(rgen.nextDouble()) / lambda;
   }

   /**
    * Computes a random 3-vector uniform in solid angle using the algorithm of
    * Robert Knop in Commun. ACM, ACM, 1970, 13, 326
    * 
    * @return double[3]
    */
   static final public double[] randomDir() {
      double x, y, s;
      do {
         x = 2.0 * (Math.random() - 0.5);
         y = 2.0 * (Math.random() - 0.5);
         s = (x * x) + (y * y);
      } while(s > 1.0);
      final double z = (2.0 * s) - 1.0;
      s = Math.sqrt((1 - (z * z)) / s);
      x *= s;
      y *= s;
      return new double[] {
         x,
         y,
         z
      };
   }

   /**
    * distance - What is the Euclidean distance between p1 and p2.
    * 
    * @param p1
    * @param p2
    * @return double
    */
   static final public double distance(double[] p1, double[] p2) {
      assert (p1.length == p2.length);
      double sum2 = 0.0;
      for(int i = 0; i < p1.length; ++i)
         sum2 += Math2.sqr(p2[i] - p1[i]);
      return Math.sqrt(sum2);
   }

   /**
    * distanceSqr - What is the square of the Euclidean distance between p1 and
    * p2.
    * 
    * @param p1
    * @param p2
    * @return double
    */
   static final public double distanceSqr(double[] p1, double[] p2) {
      assert (p1.length == p2.length);
      double sum2 = 0.0;
      for(int i = 0; i < p1.length; ++i)
         sum2 += Math2.sqr(p2[i] - p1[i]);
      return sum2;
   }

   /**
    * magnitude - What is the length of the specified vector?
    * 
    * @param p
    * @return double
    */
   static final public double magnitude(double[] p) {
      double sum2 = 0.0;
      for(final double element : p)
         sum2 += element * element;
      return Math.sqrt(sum2);
   }

   /**
    * normalize - Returns a copy of the input vector normalized to 1 length of
    * 1.0.
    * 
    * @param p The vector to normalize
    * @return A new vector containing the normalized result.
    */
   static final public double[] normalize(double[] p) {
      return divide(p, Math2.magnitude(p));
   }

   /**
    * sum - Returns the sum of the specified array
    * 
    * @param da
    * @return The sum of the specified array
    */
   static final public double sum(double[] da) {
      double res = 0.0;
      for(final double element : da)
         res += element;
      return res;
   }

   /**
    * Returns the array representing the element-by-element sum of the specified
    * arrays.
    * 
    * @param da
    * @param db
    * @return The array representing the sum of the arrays
    */
   static final public double[] add(double[] da, double[] db) {
      assert da.length == db.length;
      final double[] res = new double[Math.min(da.length, db.length)];
      for(int i = 0; i < res.length; ++i)
         res[i] = da[i] + db[i];
      return res;
   }

   /**
    * Returns the array representing the element-by-element sum of the specified
    * arrays in the original array da.
    * 
    * @param da
    * @param db
    * @return da+db in da
    */
   static final public double[] addInPlace(double[] da, double[] db) {
      for(int i = 0; (i < da.length) && (i < db.length); ++i)
         da[i] += db[i];
      return da;
   }

   /**
    * sum - Returns the sum of the specified array
    * 
    * @param da
    * @return The sum of the specified array
    */
   static final public int sum(int[] da) {
      int res = 0;
      for(final int element : da)
         res += element;
      return res;
   }

   /**
    * plus - Returns the vector sum a + b
    * 
    * @param a
    * @param b
    * @return The vector a+b
    */
   static final public double[] plus(double[] a, double[] b) {
      if(a.length != b.length)
         throw new IllegalArgumentException("Both arguments to the plus operator must be the same length.");
      final double[] res = new double[a.length];
      for(int i = 0; i < a.length; ++i)
         res[i] = a[i] + b[i];
      return res;
   }

   /**
    * plus - Returns the vector sum a + b
    * 
    * @param a
    * @param b
    * @return The vector a[i]+b
    */
   static final public double[] plus(double[] a, double b) {
      final double[] res = new double[a.length];
      for(int i = 0; i < a.length; ++i)
         res[i] = a[i] + b;
      return res;
   }

   /**
    * Replaces a with the sum a+b and returns a+b in a.
    * 
    * @param a
    * @param b
    * @return The vector a+b
    */
   static final public double[] plusEquals(double[] a, double[] b) {
      if(a.length != b.length)
         throw new IllegalArgumentException("Both arguments to the plus operator must be the same length.");
      for(int i = 0; i < a.length; ++i)
         a[i] += b[i];
      return a;
   }

   /**
    * minus - Returns the vector difference a - b
    * 
    * @param a
    * @param b
    * @return The vector a+b
    */
   static final public double[] minus(double[] a, double[] b) {
      if(a.length != b.length)
         throw new IllegalArgumentException("Both arguments to the plus operator must be the same length.");
      final double[] res = new double[a.length];
      for(int i = 0; i < a.length; ++i)
         res[i] = a[i] - b[i];
      return res;
   }

   /**
    * minus - Returns the vector sum a + b
    * 
    * @param a
    * @param b
    * @return The vector a[i]+b
    */
   static final public double[] minus(double[] a, double b) {
      final double[] res = new double[a.length];
      for(int i = 0; i < a.length; ++i)
         res[i] = a[i] - b;
      return res;
   }

   /**
    * dot - Compute the dot product of two equal lengthed vectors
    * 
    * @param a
    * @param b
    * @return The dot product of a and b
    */
   static final public double dot(double[] a, double[] b) {
      if(a.length != b.length)
         throw new IllegalArgumentException("Both arguments to the dot product must be the same length.");
      double res = 0.0;
      for(int i = 0; i < a.length; ++i)
         res += a[i] * b[i];
      return res;
   }

   /**
    * Returns a vector in which each of the coordinates equals -1 times the
    * coordinates in a.
    * 
    * @param a
    * @return double [] = -a
    */
   static final public double[] negative(double[] a) {
      final double[] na = new double[a.length];
      for(int i = 0; i < a.length; ++i)
         na[i] = -a[i];
      return na;
   }

   /**
    * cross - Computes the cross product of two three-vectors a and b
    * 
    * @param a
    * @param b
    * @return A three-vector perpendicular to both a and b and of length |a||b|
    *         sin(th) where th is the angle between a and b
    */
   static final public double[] cross(double[] a, double[] b) {
      if((a.length != 3) | (b.length != 3))
         throw new IllegalArgumentException("Both arguments to the cross product must be the three-vectors.");
      return new double[] {
         (a[1] * b[2]) - (a[2] * b[1]),
         (a[2] * b[0]) - (a[0] * b[2]),
         (a[0] * b[1]) - (a[1] * b[0])
      };
   }

   /**
    * multiply - Returns a vector containing the product of a, a scalar, times
    * b, a vector.
    * 
    * @param a
    * @param b
    * @return A vector containing a*b
    */
   static final public double[] multiply(double a, double[] b) {
      final double[] res = new double[b.length];
      for(int i = 0; i < b.length; ++i)
         res[i] = a * b[i];
      return res;
   }

   /**
    * multiply - Returns a vector containing the product of a, a vector, times
    * b, a vector where the multiplication is done piecewise.
    * 
    * @param a
    * @param b
    * @return A vector containing a[i]*b[i] for all i
    */
   static final public double[] multiply(double[] a, double[] b) {
      final double[] res = new double[Math.min(a.length, b.length)];
      for(int i = 0; i < res.length; ++i)
         res[i] = a[i] * b[i];
      return res;
   }

   static final public double[] timesEquals(double a, double[] b) {
      for(int i = 0; i < b.length; ++i)
         b[i] = a * b[i];
      return b;
   }

   static final public double[] abs(double[] data) {
      final double[] res = new double[data.length];
      for(int i = 0; i < res.length; ++i)
         res[i] = (data[i] > 0.0 ? data[i] : 0.0);
      return res;
   }

   /**
    * Returns a point that is a fraction <code>f</code> of the distance between
    * <code>a</code> and <code>b</code>. If f=0, then the result is
    * <code>a</code>. If f=1, then the result is <code>b</code>. This function
    * works in an arbitrary number of dimensions but <code>a</code> and
    * <code>b</code> must have the same number of dimensions.
    * 
    * @param a
    * @param b
    * @param f
    * @return double[] - a point
    */
   static final public double[] pointBetween(double[] a, double[] b, double f) {
      final double[] res = new double[a.length];
      for(int i = 0; i < res.length; ++i)
         res[i] = a[i] + ((b[i] - a[i]) * f);
      return res;
   }

   /**
    * Returns true if the argument vector has magnitude of 1.0.
    * 
    * @param a
    * @return boolean
    */
   static final boolean isUnitVector(double[] a) {
      return Math.abs(Math2.magnitude(a) - 1.0) < (a.length * Double.MIN_VALUE);
   }

   /**
    * angleBetween - Returns the angle between the vector a and vector b in
    * radians.
    * 
    * @param a
    * @param b
    * @return The angle 0.0 (parallel) to Pi (antiparallel)
    */
   static final public double angleBetween(double[] a, double[] b) {
      assert magnitude(a) > 0.0 : Arrays.toString(a);
      assert magnitude(b) > 0.0 : Arrays.toString(b);
      return Math.acos(dot(a, b) / (magnitude(a) * magnitude(b)));
   }

   /**
    * divide - Returns a vector containing a, a vector, divided by b, a scalar.
    * 
    * @param a
    * @param b
    * @return A vector containing a/b
    */
   static final public double[] divide(double[] a, double b) {
      final double[] res = new double[a.length];
      for(int i = 0; i < a.length; ++i)
         res[i] = a[i] / b;
      return res;
   }

   static final public double[] divideEquals(double[] a, double b) {
      for(int i = 0; i < a.length; ++i)
         a[i] = a[i] / b;
      return a;
   }

   /**
    * Divide a by b on an element-by-element basis.
    *
    * @param a
    * @param b
    * @return c[i] = a[i]/b[i]
    */
   static final public double[] ebeDivide(double[] a, double[] b) {
      assert a.length == b.length;
      double[] res = new double[a.length];
      for(int i = 0; i < a.length; ++i)
         res[i] = a[i] / b[i % b.length];
      return res;
   }

   /**
    * A numerically stable method for solving the quadratic equation a*x^2 + b*x
    * + c = 0.
    * 
    * @param a
    * @param b
    * @param c
    * @return double[2] containing the solutions or null if there is no real
    *         solution.
    */
   static final public double[] quadraticSolver(double a, double b, double c) {
      final double r = (b * b) - (4.0 * a * c);
      if(r < 0.0)
         return null;
      final double q = -0.5 * (b + (Math.signum(b) * Math.sqrt(r)));
      return new double[] {
         q / a,
         c / q
      };
   }

   public static final double cubeRoot(double x) {
      return x < 0.0 ? -Math.pow(-x, 1.0 / 3.0) : Math.pow(x, 1.0 / 3.0);
   }

   public static double[] cubicSolver(double a, double b, double c, double d) {
      // find the discriminant
      final double f = (((3.0 * c) / a) - ((b * b) / (a * a))) / 3.0;
      final double g = (((2.0 * Math.pow(b / a, 3.0)) - ((9 * b * c) / (a * a))) + ((27.0 * d) / a)) / 27.0;
      final double h = ((g * g) / 4.0) + (Math.pow(f, 3.0) / 27);
      // evaluate discriminant
      if((f == 0.0) && (g == 0.0) && (h == 0.0)) {
         // 3 equal roots
         final double x = -cubeRoot(d / a);
         return new double[] {
            x,
            x,
            x
         };
      } else if(h <= 0) {
         // 3 real roots
         final double i = Math.sqrt(((g * g) / 4.0) - h);
         final double j = cubeRoot(i);
         final double k = Math.acos(-(g / (2.0 * i)));
         final double m = Math.cos(k / 3.0);
         final double n = Math.sqrt(3.0) * Math.sin(k / 3.0);
         final double p = -(b / (3.0 * a));
         return new double[] {
            (2 * j * m) + p,
            (-j * (m + n)) + p,
            (-j * (m - n)) + p
         };
      } else {
         // 1 real root and 2 complex roots
         final double r = (-0.5 * g) + Math.sqrt(h);
         final double s = cubeRoot(r);
         final double t = (-0.5 * g) - Math.sqrt(h);
         final double u = cubeRoot(t);
         final double p = -(b / (3.0 * a));
         return new double[] {
            (s + u) + p
         };
      }
   }

   /**
    * Compute the polynomial <code>coeff[0]+coeff[1]*x+coeff[2]*x^2+...</code>
    * 
    * @param coeff
    * @param x
    * @return The result as a double
    */
   static final public double polynomial(double[] coeff, double x) {
      double res = coeff[coeff.length - 1];
      for(int i = coeff.length - 2; i >= 0; --i)
         res = (res * x) + coeff[i];
      return res;
   }

   static public double closestTo(double[] vals, double val) {
      double res = vals[0];
      for(int i = 1; i < vals.length; ++i)
         if(Math.abs(vals[i] - val) < Math.abs(res - val))
            res = vals[i];
      return res;
   }

   /**
    * Solve the polynomial equation
    * <code>coeff[0]+coeff[1]*x+coeff[2]*x^2+..=0</code> for x. Polynomials up
    * to order cubic are supported. Only real roots will be returned.
    * 
    * @param coeff
    * @return An array containing all the real roots
    * @throws EPQException
    */
   static final public double[] solvePoly(double[] coeff)
         throws EPQException {
      switch(coeff.length) {
         case 2:
            return new double[] {
               -coeff[0] / coeff[1]
            };
         case 3:
            return quadraticSolver(coeff[2], coeff[1], coeff[0]);
         case 4:
            return cubicSolver(coeff[3], coeff[2], coeff[1], coeff[0]);
         default:
            throw new EPQException("Analytical solution not available");
      }
   }

   static final public double[] solvePoly(double[] coeff, double y)
         throws EPQException {
      final double[] tmp = coeff.clone();
      tmp[0] -= y;
      return solvePoly(tmp);
   }

   /**
    * li - A naive implementation of the Logarithmic Integral
    * 
    * @param x
    * @return double
    */
   static final public double li(double x) {
      if(x <= 1.0)
         throw new IllegalArgumentException("x>1.0 :" + Double.toString(x));
      final double lx = Math.log(x);
      double res = Math.log(lx) + 0.577215664901532860;
      double ff = 1.0;
      double lxp = 1.0;
      for(double f = 1.0; f < 20.0; ++f) {
         ff *= f;
         lxp *= lx;
         res += lxp / (ff * f);
      }
      return res;
   }

   /**
    * Returns x trimmed such that if x is less than x0 then x0 is returned and
    * if x is greater than x1 then x1 is returned, otherwise x is returned.
    * 
    * @param x
    * @param x0
    * @param x1
    * @return return x &lt; x0 ? x0 : (x &gt; x1 ? x1 : x);
    */
   static final public double bound(double x, double x0, double x1) {
      if(x0 > x1) {
         final double t = x0;
         x0 = x1;
         x1 = t;
      }
      return Double.isNaN(x) ? x : (x < x0 ? x0 : (x > x1 ? x1 : x));
   }

   /**
    * Returns x trimmed such that if x is less than lowerInc then lowerInc is
    * returned and if x is greater or equal to upperExc then upperExc-1 is
    * returned, otherwise x is returned.
    * 
    * @param x
    * @param lowerInc where lowerInc &lt; upperExc
    * @param upperExc where upperExc &gt; lowerInc
    * @return return x &lt; lowerInc ? lowerInc : (x &gt;= upperExc ? upperExc-1
    *         : x);
    */
   static final public int bound(int x, int lowerInc, int upperExc) {
      assert lowerInc < upperExc;
      return x < lowerInc ? lowerInc : (x >= upperExc ? upperExc - 1 : x);
   }

   /**
    * Returns x trimmed such that if x is less than lowerInc then lowerInc is
    * returned and if x is greater or equal to upperExc then upperExc-1 is
    * returned, otherwise x is returned.
    * 
    * @param x
    * @param lowerInc where lowerInc &lt; upperExc
    * @param upperExc where upperExc &gt; lowerInc
    * @return return x &lt; lowerInc ? lowerInc : (x &gt;= upperExc ? upperExc-1
    *         : x);
    */
   static final public long bound(long x, long lowerInc, long upperExc) {
      assert lowerInc < upperExc;
      return x < lowerInc ? lowerInc : (x >= upperExc ? upperExc - 1 : x);
   }

   /**
    * Returns x if x&gt;0, 0 if x&lt;=0
    * 
    * @param x
    * @return x if x&gt;0, 0 if x&lt;=0
    */
   static public final double positive(double x) {
      return x > 0.0 ? x : 0.0;
   }

   /**
    * Returns x if x&lt;0, 0 otherwise
    * 
    * @param x
    * @return x if x&lt;0, 0 otherwise
    */
   static public final double negative(double x) {
      return x < 0.0 ? x : 0.0;
   }

   /**
    * Compute the number of permutions of N items chosen M at a time.
    * 
    * @param n
    * @param m
    * @return double
    */
   static final public int binomialCoefficient(int n, int m) {
      if((n >= m) && (m > 0)) {
         double res = 1.0;
         for(int i = m + 1; i <= n; ++i)
            res *= i;
         for(int i = n - m; i > 0; --i)
            res /= i;
         assert (int) res == Math.round(res) : Double.toString(res);
         return (int) Math.round(res);
      } else
         return 0;
   }

   static final public double max(double[] da) {
      double res = -Double.MAX_VALUE;
      for(final double d : da)
         if(d > res)
            res = d;
      return res;
   }

   static final public double max(double[][] m) {
      double max = Math2.max(m[m.length - 1]);
      for(int h = m.length - 2; h >= 0; --h) {
         final double tmp = Math2.max(m[h]);
         if(tmp > max)
            max = tmp;
      }
      return max;
   }

   static final public int max(int[] da) {
      int res = da[0];
      for(final int d : da)
         if(d > res)
            res = d;
      return res;
   }

   static final public double min(double[] da) {
      double res = Double.MAX_VALUE;
      for(final double d : da)
         if(d < res)
            res = d;
      return res;
   }

   static final public int min(int[] da) {
      int res = da[0];
      for(final int d : da)
         if(d < res)
            res = d;
      return res;
   }

   static final public double min(double[][] m) {
      double min = Math2.min(m[m.length - 1]);
      for(int h = m.length - 2; h >= 0; --h) {
         final double tmp = Math2.min(m[h]);
         if(tmp < min)
            min = tmp;
      }
      return min;
   }

   /**
    * Extract a slice of data from the array <code>data</code> starting with the
    * element indexed by <code>start</code> and of length <code>len</code>. This
    * function will fail with an IndexOutOfBoundsException exception if
    * <code>st+len&gt;data.length</code>.
    * 
    * @param data
    * @param st
    * @param len
    * @return double[] of length len
    */
   static public double[] slice(double[] data, int st, int len) {
      final double[] res = new double[len];
      System.arraycopy(data, st, res, 0, len);
      return res;
   }

   static public double pNorm(double[] data, double p) {
      double res = 0.0;
      for(final double element : data)
         res += Math.pow(Math.abs(element), p);
      return Math.pow(res, 1.0 / p);
   }

   static public double infinityNorm(double[] data) {
      double res = 0.0;
      for(final double element : data)
         if(res < Math.abs(element))
            res = Math.abs(element);
      return res;
   }

   /**
    * Computes the Legendre polynomials up to n=10.
    * 
    * @param x
    * @param n In range 0 to 10
    * @return double
    */
   static public double Legendre(double x, int n) {
      switch(n) {
         case 0:
            return 1.0;
         case 1:
            return x;
         case 2:
            return 0.5 * (-1.0 + (3.0 * x * x));
         case 3:
            return 0.5 * x * (-3.0 + (5.0 * x * x));
         case 4: {
            final double xx = x * x;
            return 0.125 * (3.0 + (xx * (-30.0 + (xx * 35.0))));
         }
         case 5: {
            final double xx = x * x;
            return 0.125 * x * (15.0 + (xx * (-70.0 + (xx * 63.0))));
         }
         case 6: {
            final double xx = x * x;
            return 0.0625 * (-5.0 + (xx * (105.0 + (xx * (-315.0 + (xx * 231.0))))));
         }
         case 7: {
            final double xx = x * x;
            return 0.0625 * x * (-35.0 + (xx * (315.0 + (xx * (-693.0 + (429.0 * xx))))));
         }
         case 8: {
            final double xx = x * x;
            return 0.0078125 * (35.0 + (xx * (-1260.0 + (xx * (6930.0 + (xx * (-12012.0 + (xx * 6435.0))))))));
         }
         case 9: {
            final double xx = x * x;
            return 0.0078125 * x * (315.0 + (xx * (-4620.0 + (xx * (18018.0 + (xx * (-25740.0 + (xx * 12155.0))))))));
         }
         case 10: {
            final double xx = x * x;
            return 0.00390625
                  * (-63.0 + (xx * (3465.0 + (xx * (-30030.0 + (xx * (90090.0 + (xx * (-109395.0 + (xx * 46189.0))))))))));
         }
         default:
            throw new IllegalArgumentException("Legendre order out of range [0,10].");
      }
   }

   /**
    * Are <code>a</code> and <code>b</code> approximately equal to within
    * <code>frac</code> of the average of a+b. Use with care on numbers
    * <code>a</code> and <code>b</code> which are both strictly positive or
    * strictly negative.
    * 
    * @param a
    * @param b
    * @param frac
    * @return approxEquals
    */
   public static boolean approxEquals(double a, double b, double frac) {
      assert frac > 0.0;
      assert frac < 1.0;
      assert Math.abs(a + b) > Math.abs(a);
      return Math.abs(a - b) < (0.5 * Math.abs(a + b) * frac);
   }

   /**
    * Convolves the specified kernel with the specified vector. The end members
    * of <code>v</code> are reused when the kernel extends past the ends.
    * 
    * @param v
    * @param kernel
    * @return double[]
    */
   static public double[] convolve(double[] v, double[] kernel) {
      assert (kernel.length % 2) == 1;
      final double[] res = new double[v.length];
      final int mid = kernel.length / 2;
      for(int i = 0; i < res.length; ++i)
         for(int j = 0; j < kernel.length; ++j)
            res[i] += kernel[j] * v[bound((i + j) - mid, 0, v.length)];
      return res;
   }

   static public String toString(double[] vec) {
      return toString(vec, NumberFormat.getInstance());
   }

   static public String toString(double[] vec, NumberFormat nf) {
      final StringBuffer sb = new StringBuffer();
      if(vec.length > 0) {
         sb.append(nf.format(vec[0]));
         for(int i = 1; i < vec.length; ++i) {
            sb.append(",");
            sb.append(nf.format(vec[i]));
         }
      }
      return sb.toString();
   }

   static public boolean isNaN(double[] arr) {
      for(final double d : arr)
         if(Double.isNaN(d))
            return true;
      return false;
   }

   /**
    * Converts a double into a continued fraction to within the specified
    * tolerance.
    * 
    * @param val
    * @param tol
    * @return long[]
    */
   public static long[] toContinuedFraction(double val, double tol) {
      final long[] res = new long[10];
      final double[] num = new double[res.length + 2];
      final double[] den = new double[res.length + 2];
      num[1] = 1.0;
      den[0] = 1.0;
      final double sign = Math.signum(val);
      double rem = Math.abs(val);
      for(int i = 0; i < res.length; ++i) {
         res[i] = (long) Math.floor(rem);
         num[i + 2] = (res[i] * num[i + 1]) + num[i];
         den[i + 2] = (res[i] * den[i + 1]) + den[i];
         System.out.println(num[i + 2] / den[i + 2]);
         rem -= res[i];
         if(Math.abs((num[i + 2] / den[i + 2]) - Math.abs(val)) < tol) {
            res[0] = (long) (sign * res[0]);
            return Arrays.copyOfRange(res, 0, i + 1);
         }
         rem = 1.0 / rem;
      }
      res[0] = (long) (sign * res[0]);
      return res;
   }

   /**
    * Convert a continued fraction into a double
    * 
    * @param cf
    * @return double
    */
   public static double toDecimal(long[] cf) {
      double x = cf[cf.length - 1], y = 1.0;
      for(int i = cf.length - 2; i >= 1; --i) {
         final double oldX = x;
         x = (cf[i] * x) + y;
         y = oldX;
      }
      return cf[0] > 0 ? cf[0] + (y / x) : cf[0] - (y / x);
   }

   /**
    * Convert a continued fraction into a a long[2] containing the numerator [0]
    * and denominator [1] of a fraction.
    * 
    * @param cf
    * @return long[2]
    */
   public static long[] toFraction(long[] cf) {
      long x = cf[cf.length - 1], y = 1;
      for(int i = cf.length - 2; i >= 1; --i) {
         final long oldX = x;
         x = (cf[i] * x) + y;
         y = oldX;
      }
      if(cf[0] > 0)
         return new long[] {
            (cf[0] * x) + y,
            x
         };
      else
         return new long[] {
            (cf[0] * x) - y,
            x
         };
   }

   public static Matrix createRowMatrix(double[] vals) {
      return new Matrix(vals, vals.length);
   }

   /**
    * Computes the greatest common divisor of a and b. The result is always
    * positive.
    * 
    * @param a
    * @param b
    * @return gcd(a,b) -&gt; a % gcd(a,b)==0 and b % gcd(a,b)==0
    */
   public static long gcd(long a, long b) {
      if(b == 0)
         return Math.abs(a);
      return gcd(b, a - (b * (a / b)));
   }

   /**
    * Computes the quadratic equation (ax^2+bx+c=0) in a numerically stable
    * manner.
    * 
    * @param a Quadratic coefficient
    * @param b Linear coefficient
    * @param c Offset
    * @return The two roots or null if there are no real roots.
    */
   public static double[] solveQuadratic(double a, double b, double c)
         throws EPQException {
      return solvePoly(new double[] {
         c,
         b,
         a
      });
   }

   /**
    * Solves the cubic equation (x^3+ax^2+bx+c=0) in a numerically stable
    * manner.
    * 
    * @param a Quadratic coefficient
    * @param b Linear coefficient
    * @param c Offset
    * @return The two roots or null if there are no real roots.
    */
   public static double[] solveCubic(double a, double b, double c) {
      final double q = ((a * a) - (3.0 * b)) / 9.0;
      final double r = (((2.0 * a * a * a) - (9.0 * a * b)) + (27.0 * c)) / 54.0;
      if((r * r) < (q * q * q)) {
         // Three real roots
         final double th = Math.acos(r / Math.pow(q, 1.5));
         return new double[] {
            (-2.0 * q * Math.cos(th / 3.0)) - (a / 3.0),
            (-2.0 * q * Math.cos((th + (2.0 * Math.PI)) / 3.0)) - (a / 3.0),
            (-2.0 * q * Math.cos((th - (2.0 * Math.PI)) / 3.0)) - (a / 3.0)
         };
      } else {
         // One real root
         final double A = -Math.signum(r) * Math.pow((Math.abs(r) + Math.sqrt((r * r) - (q * q * q))), 1.0 / 3.0);
         final double B = (a == 0.0 ? 0.0 : q / a);
         return new double[] {
            (A + B) - (a / 3.0)
         };
      }
   }

   static public double findRoot(double[] coeffs, double x1, double x2, double xacc)
         throws EPQException {
      final int MAXIT = 100;
      final double[] deriv = new double[coeffs.length - 1];
      for(int i = 0; i < deriv.length; ++i)
         deriv[i] = coeffs[i + 1] * (i + 1);
      final double fl = polynomial(coeffs, x1);
      final double fh = polynomial(coeffs, x2);
      if(Math.signum(fl) == Math.signum(fh))
         throw new EPQException("End points must bracket the root in Math2.findRoot.");
      double xl = (fl < 0.0 ? x1 : x2);
      double xh = (fl < 0.0 ? x2 : x1);
      double rts = 0.5 * (x1 + x2);
      double dxold = Math.abs(x2 - x1);
      double dx = dxold;
      double f = polynomial(coeffs, rts);
      double df = polynomial(deriv, rts);
      for(int j = 0; j < MAXIT; ++j) {
         if((((((rts - xh) * df) - f) * (((rts - xl) * df) - f)) >= 0.0) || (Math.abs(2.0 * f) > Math.abs(dxold * df))) {
            dxold = dx;
            dx = 0.5 * (xh - xl);
            rts = xl + dx;
            if(xl == rts)
               return rts;
         } else {
            dxold = dx;
            dx = f / df;
            final double temp = rts;
            rts -= dx;
            if(temp == rts)
               return rts;
         }
         if(Math.abs(dx) < xacc)
            return rts;
         f = polynomial(coeffs, rts);
         df = polynomial(deriv, rts);
         if(f < 0.0)
            xl = rts;
         else
            xh = rts;
      }
      throw new EPQException("Maximum iteration count exceeded in Math2.rootFind");
   }

   static public double[] v3(double x, double y, double z) {
      return new double[] {
         x,
         y,
         z
      };
   }

   static public double[] x3(double x) {
      return new double[] {
         x,
         0.0,
         0.0
      };
   }

   static public double[] y3(double y) {
      return new double[] {
         0.0,
         y,
         0.0
      };
   }

   static public double[] z3(double z) {
      return new double[] {
         0.0,
         0.0,
         z
      };
   }

   public static double[][] transpose(double[][] mat) {
      double[][] res = new double[mat[0].length][mat.length];
      for(int i = 0; i < mat.length; ++i)
         for(int j = 0; j < mat[i].length; ++j)
            res[j][i] = mat[i][j];
      return res;
   }

}
