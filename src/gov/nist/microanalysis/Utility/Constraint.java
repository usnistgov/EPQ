package gov.nist.microanalysis.Utility;

/**
 * <p>
 * Provides a generic mechanism for constraining the fit parameters. The
 * parameter the optimizer sees can range over the full extent of the real
 * numbers but the fit parameter can be constrained within a sub-set of the
 * reals. Typically, the constraints are either within a bounded range of
 * values, strictly positive or strictly negative.
 * </p>
 * <p>
 * Constraints are implemented by mapping the [-&#8734;,&#8734;] onto the
 * desired sub-range through some invertible, differentiable function.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nritchie
 * @version 1.0
 */
public interface Constraint {
   /**
    * A function that maps <code>param</code> onto a constrained range.
    * 
    * @param param the value to constrain
    * @return The constrained result
    */
   double realToConstrained(double param);

   /**
    * The inversion function of compute(...).
    * <code>inverse(compute(param))==param</code> evaluates true.
    * 
    * @param param the value to constrain
    * @return The <code>param</code> such that <code>compute(param)=res</code>
    */
   double constrainedToReal(double param);

   /**
    * The derivative of the function in <code>compute(..)</code> with respect to
    * the argument <code>param</code>.
    * 
    * @param param the value
    * @return The derivative
    */
   double derivative(double param);

   /**
    * Returns the same as <code>compute(..)</code> except also propogates the
    * error in <code>param</code> into error in the result.
    * 
    * @param param the value
    * @return An UncertainValue containing the error propogated result of the
    *         constraint.
    */
   UncertainValue2 getResult(UncertainValue2 param);

   /**
    * <p>
    * Constrains the resulting fit parameter to be strictly positive.
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    * 
    * @author nritchie
    * @version 1.0
    */
   public class Positive
      implements Constraint {
      private final double mScale;

      public Positive(double scale) {
         mScale = scale;
      }

      @Override
      public double realToConstrained(double param) {
         return mScale * Math.exp(param);
      }

      @Override
      public double constrainedToReal(double res) {
         return Math.log(res / mScale);
      }

      @Override
      public double derivative(double param) {
         return mScale * Math.exp(param);
      }

      @Override
      public UncertainValue2 getResult(UncertainValue2 param) {
         return UncertainValue2.multiply(mScale, UncertainValue2.exp(param));
      }

      @Override
      public String toString() {
         return "Positive[scale=" + Double.toString(mScale) + "]";
      }
   }

   public class Fractional
      implements Constraint {
      private final String mName;
      private final double mScale;
      private final double mFraction;

      public Fractional(String name, double scale, double fraction) {
         mName = name;
         mScale = scale;
         mFraction = fraction;
      }

      static private final double TWO_OVER_PI = 2.0 / Math.PI;

      @Override
      public double realToConstrained(double param) {
         return mScale * (1.0 + (mFraction * TWO_OVER_PI * Math.atan(param)));
      }

      @Override
      public double constrainedToReal(double res) {
         return Math.tan((res - mScale) / (mScale * mFraction * TWO_OVER_PI));
      }

      @Override
      public double derivative(double param) {
         return (mScale * mFraction * TWO_OVER_PI) / (1.0 + (param * param));
      }

      @Override
      public UncertainValue2 getResult(UncertainValue2 param) {
         return UncertainValue2.add(mScale, UncertainValue2.multiply(mScale * mFraction
               * TWO_OVER_PI, UncertainValue2.atan(param)));
      }

      @Override
      public String toString() {
         return "Fraction[" + mName + "," + Double.toString(mScale) + " +- " + Double.toString(mScale * mFraction) + "]";
      }
   }

   /**
    * <p>
    * Constrains the resulting fit parameter to be within <code>width</code> of
    * the value <code>center</code>.
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    * 
    * @author nritchie
    * @version 1.0
    */
   public class Bounded
      implements Constraint {
      private final double mCenter;
      private final double mWidth;

      public Bounded(double center, double width) {
         mCenter = center;
         mWidth = 0.5 * width;
      }

      static private final double TWO_OVER_PI = 2.0 / Math.PI;

      @Override
      public double realToConstrained(double param) {
         return mCenter + (mWidth * TWO_OVER_PI * Math.atan(param));
      }

      @Override
      public double constrainedToReal(double res) {
         return Math.tan((res - mCenter) / (mWidth * TWO_OVER_PI));
      }

      @Override
      public double derivative(double param) {
         return (mWidth * TWO_OVER_PI) / (1.0 + (param * param));
      }

      @Override
      public UncertainValue2 getResult(UncertainValue2 param) {
         return UncertainValue2.add(mCenter, UncertainValue2.multiply(mWidth * TWO_OVER_PI, UncertainValue2.atan(param)));
      }

      @Override
      public String toString() {
         return "Bounded[min=" + Double.toString(mCenter - mWidth) + ",max=" + Double.toString(mCenter + mWidth) + "]";
      }
   }

   /**
    * <p>
    * Does not constrain the fit parameter in any way.
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    * 
    * @author nritchie
    * @version 1.0
    */
   public class None
      implements Constraint {

      public None() {
         super();
      }

      @Override
      public double realToConstrained(double param) {
         return param;
      }

      @Override
      public double constrainedToReal(double res) {
         return res;
      }

      @Override
      public double derivative(double param) {
         return 1.0;
      }

      @Override
      public UncertainValue2 getResult(UncertainValue2 param) {
         return param;
      }

      @Override
      public String toString() {
         return "Unconstrained[]";
      }

   }
}
