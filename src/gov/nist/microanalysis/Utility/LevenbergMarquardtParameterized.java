package gov.nist.microanalysis.Utility;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.Utility.LevenbergMarquardt2.FitFunction;
import gov.nist.microanalysis.Utility.LevenbergMarquardt2.FitResult;
import gov.nist.microanalysis.Utility.LevenbergMarquardtConstrained.ConstrainedFitFunction;

import Jama.Matrix;

/**
 * <p>
 * This class is similar to LevenbergMarquardtConstrained but adds the ability
 * to tag the parameters with tokens (represented by Parameter objects)
 * containing a Constraint, a default value and a name. The bookkeeping of
 * parameters is then handled transparently and the resulting fit values can be
 * accessed via the Parameter handle. Further specializations of the Parameter
 * class can be created to associate Parameters with specific objects like
 * XRayTransition objects, XRayTransitionSet objects or other.
 * </p>
 * <p>
 * The other critical component is the Function interface. The Function
 * interface provides a mechanism for computing the fit function and its partial
 * derivatives. The <code>compute</code>, <code>derivative</code> and
 * <code>getResult</code> functions are handed a map connecting Parameter to
 * value which they they use to perform the computation.
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
public class LevenbergMarquardtParameterized {

   private ActionListener mListener;

   /**
    * <p>
    * The class representing a fit parameter. The Parameter object carries a
    * Constraint, a default value and a name.
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
   static public class Parameter {

      private Constraint mConstraint;
      private double mDefaultValue;
      private final String mName;
      private boolean mIsFit;

      public Parameter(String name, double defValue, boolean isFit) {
         this(name, new Constraint.None(), defValue, isFit);
      }

      public Parameter(String name, Constraint constraint, double defValue, boolean isFit) {
         mName = name;
         mConstraint = constraint;
         mDefaultValue = defValue;
         mIsFit = isFit;
      }

      /**
       * Gets the current value assigned to constraint
       * 
       * @return Returns the constraint.
       */
      public Constraint getConstraint() {
         return mConstraint;
      }

      /**
       * Sets the value assigned to constraint.
       * 
       * @param constraint
       *           The value to which to set constraint.
       */
      public void setConstraint(Constraint constraint) {
         if (mConstraint != constraint)
            mConstraint = constraint;
      }

      public double getDefaultValue() {
         return mDefaultValue;
      }

      public void setDefaultValue(double iv) {
         mDefaultValue = iv;
      }

      public double getValue(Map<Parameter, Double> param) {
         assert (!mIsFit) || param.containsKey(this) : toString() + " - " + (isFit() ? "Fit" : "Not fit");
         return mIsFit ? param.get(this) : mDefaultValue;
      }

      public UncertainValue2 getUncertainValue(Map<Parameter, UncertainValue2> param) {
         assert (!mIsFit) || param.containsKey(this);
         final UncertainValue2 res = mIsFit ? param.get(this) : new UncertainValue2(mDefaultValue);
         return new UncertainValue2(res.doubleValue(), mName, res.uncertainty());
      }

      public boolean isFit() {
         return mIsFit;
      }

      public void setIsFit(boolean b) {
         mIsFit = b;
      }

      /**
       * @see java.lang.Object#hashCode()
       */
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = (prime * result) + mName.hashCode();
         return result;
      }

      /**
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         return mName.equals(((Parameter) obj).mName);
      }

      @Override
      public String toString() {
         return mName + "[" + mConstraint + "," + mDefaultValue + "]";
      }

      public String getName() {
         return mName;
      }
   }

   static public class ParameterObject<T> extends Parameter {
      private final T mObject;

      public ParameterObject(String name, Constraint constraint, double defValue, boolean isFit, T obj) {
         super(name, constraint, defValue, isFit);
         mObject = obj;
      }

      protected ParameterObject(String name, double defValue, boolean isFit, T obj) {
         super(name, defValue, isFit);
         mObject = obj;
      }

      public T getObject() {
         return mObject;
      }

   }

   /**
    * <p>
    * Description
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
   public interface Function {

      /**
       * Does this Function depend upon the argument Parameter and is this
       * Parameter fit?
       * 
       * @param idx
       *           {@link Parameter}
       * @return true if dependent and fit; false otherwise
       */
      boolean isFitParameter(Parameter idx);

      Set<Parameter> getParameters(boolean all);

      /**
       * A function with argument arg and parameters param.
       * 
       * @param arg
       *           double
       * @param param
       *           Map&lt;Parameter, Double&gt;
       * @return The constrained result
       */
      double compute(double arg, Map<Parameter, Double> param);

      /**
       * The derivative of the function in <code>compute(..)</code> with respect
       * to the i-th member of the arguments <code>param</code>.
       * 
       * @param arg
       *           double
       * @param param
       *           Map&lt;Parameter, Double&gt;
       * @param idx
       *           Parameter
       * @return The derivative
       */
      double derivative(double arg, Map<Parameter, Double> param, Parameter idx);

      /**
       * Returns the same as <code>compute(..)</code> except also propogates the
       * error in <code>param</code> into error in the result.
       * 
       * @param arg
       *           double
       * @param param
       *           Map&lt;Parameter, UncertainValue2&gt;
       * @return An UncertainValue containing the error propogated result of the
       *         constraint.
       */
      UncertainValue2 computeU(double arg, Map<Parameter, UncertainValue2> param);
   }

   public interface InvertableFunction extends Function {

      /**
       * Computes the inverse of <code>compute(double arg, Map&lt;Parameter,
       * UncertainValue&gt; param)</code>
       * 
       * @param arg
       *           double
       * @param param
       *           Map&lt;Parameter, UncertainValue2&gt;
       * @return UncertainValue2
       */
      public UncertainValue2 inverse(double arg, Map<Parameter, UncertainValue2> param);
   }

   public static abstract class FunctionImpl implements Function {

      private final HashSet<Parameter> mParameters = new HashSet<Parameter>();

      final public Parameter add(Parameter p) {
         mParameters.add(p);
         return p;
      }

      final public void add(Collection<Parameter> cp) {
         mParameters.addAll(cp);
      }

      @Override
      final public Set<Parameter> getParameters(boolean all) {
         final HashSet<Parameter> res = new HashSet<Parameter>();
         for (final Parameter p : mParameters)
            if (all || p.isFit())
               res.add(p);
         return res;
      }

      @Override
      final public boolean isFitParameter(Parameter p) {
         return mParameters.contains(p) && p.isFit();
      }

      /**
       * Extracts the fit Parameter values associated with this function from a
       * Map containing all fit parameter values.
       * 
       * @param fitResult
       *           Map&lt;Parameter, UncertainValue2&gt;
       * @return Map&lt;Parameter, Double&gt;
       */
      final public Map<Parameter, UncertainValue2> extract(Map<Parameter, UncertainValue2> fitResult) {
         final HashMap<Parameter, UncertainValue2> res = new HashMap<Parameter, UncertainValue2>();
         for (final Parameter p : getParameters(false))
            res.put(p, fitResult.get(p));
         return res;
      }

      @Override
      public String toString() {
         return "[all=" + this.getParameters(true).size() + ",fit=" + getParameters(false).size() + "]";
      }
   }

   /**
    * <p>
    * Internal class used to map {@link Function} objects into
    * {@link FitFunction} objects.
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
   private class ParameterizedFitFunction implements FitFunction {

      private final ArrayList<Parameter> mParameters;
      private final Function mFunction;
      private final double[] mOrdinate;

      private ParameterizedFitFunction(double[] x, Function f) {
         mFunction = f;
         mParameters = new ArrayList<Parameter>(f.getParameters(false));
         mOrdinate = x.clone();
      }

      private Map<Parameter, Double> getUpdatedParam(Matrix params) {
         final Map<Parameter, Double> param = new HashMap<Parameter, Double>();
         int i = 0;
         for (final Parameter p : mParameters) {
            param.put(p, params.get(i, 0));
            ++i;
         }
         return param;
      }

      @Override
      public Matrix partials(Matrix params) {
         final Matrix res = new Matrix(mOrdinate.length, params.getRowDimension());
         final Map<Parameter, Double> param = getUpdatedParam(params);
         for (int j = 0; j < mParameters.size(); ++j) {
            final Parameter p = mParameters.get(j);
            for (int ch = 0; ch < mOrdinate.length; ++ch)
               res.set(ch, j, mFunction.derivative(mOrdinate[ch], param, p));
         }
         return res;
      }

      /**
       * Computes the fit function as a function of the fit parameters.
       * 
       * @param params
       *           A m x 1 Matrix containing the fit function parameters
       * @return A n x 1 matrix containing the fit function values at each
       */
      @Override
      public Matrix compute(Matrix params) {
         final Matrix res = new Matrix(mOrdinate.length, 1);
         final Map<Parameter, Double> param = getUpdatedParam(params);
         for (int j = 0; j < mOrdinate.length; ++j)
            res.set(j, 0, mFunction.compute(mOrdinate[j], param));
         return res;
      }

      public int paramSize() {
         return mParameters.size();
      }
   }

   /**
    * <p>
    * Extends MarquardtLevenberg2.{@link FitResult} by adding the ability to map
    * Parameter objects into the best fit values.
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
   public static class ParameterizedFitResult extends FitResult {

      private final List<Parameter> mParameters;

      private ParameterizedFitResult(LevenbergMarquardt2 lm2, FitResult fr, List<Parameter> params) {
         lm2.super(fr.mFunction);
         this.mBestParams = fr.mBestParams;
         this.mBestY = fr.mBestY;
         this.mChiSq = fr.mChiSq;
         this.mCovariance = fr.mCovariance;
         this.mImproveCount = fr.mImproveCount;
         this.mIterCount = fr.mIterCount;
         mParameters = Collections.unmodifiableList(new ArrayList<Parameter>(params));
      }

      public int indexOf(Parameter p) {
         return mParameters.indexOf(p);
      }

      public UncertainValue2 getBestFitValue(Parameter p) {
         return getBestParametersU()[indexOf(p)];
      }

      public double getBestFit(Parameter p) {
         return getBestParametersU()[indexOf(p)].doubleValue();
      }

      public List<Parameter> getParameters() {
         return mParameters;
      }

      public List<Parameter> getParametersByClass(Class<? extends Parameter> pc) {
         final List<Parameter> res = new ArrayList<LevenbergMarquardtParameterized.Parameter>();
         for (final Parameter p : mParameters)
            if (p.getClass().equals(pc))
               res.add(p);
         return Collections.unmodifiableList(res);
      }

      public Parameter getParameterByClass(Class<? extends Parameter> pc) {
         for (final Parameter p : mParameters)
            if (p.getClass().equals(pc))
               return p;
         return null;
      }

      public Map<Parameter, UncertainValue2> getParameterMap() {
         final Map<Parameter, UncertainValue2> res = new HashMap<LevenbergMarquardtParameterized.Parameter, UncertainValue2>();
         final UncertainValue2[] bpu = getBestParametersU();
         for (int i = 0; i < mParameters.size(); ++i)
            res.put(mParameters.get(i), bpu[i]);
         return Collections.unmodifiableMap(res);
      }

      public Map<Parameter, Double> getResults() {
         final Map<Parameter, Double> res = new HashMap<LevenbergMarquardtParameterized.Parameter, Double>();
         final UncertainValue2[] bpu = getBestParametersU();
         for (int i = 0; i < mParameters.size(); ++i)
            res.put(mParameters.get(i), bpu[i].doubleValue());
         return Collections.unmodifiableMap(res);
      }

      public String tabulate() {
         final StringBuffer sb = new StringBuffer();
         sb.append("Name\tDefault\tFit\tu(Fit)\n");
         for (final Parameter p : mParameters) {
            sb.append(p.mName);
            sb.append("\t");
            sb.append(p.mDefaultValue);
            sb.append("\t");
            sb.append(getBestFitValue(p).doubleValue());
            sb.append("\t");
            sb.append(getBestFitValue(p).uncertainty());
            sb.append("\n");
         }
         return sb.toString();
      }
   }

   /**
    * Applies the {@link LevenbergMarquardtConstrained} algorithm to optimize
    * the Function <code>f</code> relative at the ordinate values
    * <code>xVals</code> and data values <code>yData</code> with uncertainty
    * estimates <code>sigma</code>. The results are returned as a
    * {@link ParameterizedFitResult} object.
    * 
    * @param f
    *           {@link Function}
    * @param xVals
    *           double[n]
    * @param yData
    *           double[n]
    * @param sigma
    *           double[n]
    * @return {@link ParameterizedFitResult}
    * @throws EPQException
    */
   public ParameterizedFitResult compute(Function f, double[] xVals, double[] yData, double[] sigma) throws EPQException {
      final ParameterizedFitFunction pff = new ParameterizedFitFunction(xVals, f);
      // Initialize constraints
      final ConstrainedFitFunction cff = new ConstrainedFitFunction(pff, pff.paramSize());
      for (int i = 0; i < pff.paramSize(); ++i)
         cff.setConstraint(i, pff.mParameters.get(i).mConstraint);
      final LevenbergMarquardtConstrained lmq = new LevenbergMarquardtConstrained();
      if (mListener != null)
         lmq.addActionListener(mListener);
      // Initialize fit data.
      final Matrix yM = new Matrix(yData.length, 1), sM = new Matrix(sigma.length, 1);
      for (int i = 0; i < yData.length; ++i) {
         yM.set(i, 0, yData[i]);
         sM.set(i, 0, sigma[i]);
      }
      final Matrix p0 = new Matrix(pff.mParameters.size(), 1);
      for (int i = 0; i < pff.paramSize(); ++i)
         p0.set(i, 0, pff.mParameters.get(i).mDefaultValue);
      return new ParameterizedFitResult(lmq, lmq.compute(cff, yM, sM, p0), pff.mParameters);
   }

   public void addActionListener(ActionListener al) {
      mListener = al;
   }
}
