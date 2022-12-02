package gov.nist.microanalysis.EPQLibrary;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQDatabase.Session.QCNormalizeMode;
import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties.PropertyId;
import gov.nist.microanalysis.EPQLibrary.Detector.DetectorLineshapeModel;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.Utility.Constraint;
import gov.nist.microanalysis.Utility.LevenbergMarquardtParameterized;
import gov.nist.microanalysis.Utility.LevenbergMarquardtParameterized.Function;
import gov.nist.microanalysis.Utility.LevenbergMarquardtParameterized.FunctionImpl;
import gov.nist.microanalysis.Utility.LevenbergMarquardtParameterized.InvertableFunction;
import gov.nist.microanalysis.Utility.LevenbergMarquardtParameterized.Parameter;
import gov.nist.microanalysis.Utility.LevenbergMarquardtParameterized.ParameterObject;
import gov.nist.microanalysis.Utility.LevenbergMarquardtParameterized.ParameterizedFitResult;
import gov.nist.microanalysis.Utility.LinearLeastSquares;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.UncertainValue2;
import gov.nist.microanalysis.Utility.UtilException;

/**
 * <p>
 * SpectrumFitter8 is the 8th generation mechanism for fitting spectra with a
 * linear superposition of Gaussian forms.
 * </p>
 * <p>
 * Spectra are fit in channel space. A parameterized function converts channels
 * to energy. This parameterization may be fit or may be held constant.
 * </p>
 * <p>
 * The resolution of the detector is modeled using a function parameterized by a
 * fano factor and a noise term.
 * </p>
 * <p>
 * The position of the Gaussians is fixed by the energy of the x-ray. The width
 * is parameterized by the resolution function so the only parameter explicitly
 * fit is the amplitude. Single Gaussian amplitudes can be fit individually or a
 * collection of Gaussians can be bound together into a bundle which is fit with
 * a single amplitude.
 * </p>
 * <p>
 * The background (Bremmstrahlung) is modeled and then the model adjusted using
 * a linear adjustment to fit both sides of the peak region. The difference
 * between the modeled background and the spectrum is the assumed to represent
 * the characteristic intensity. The characteristic intensity is fit.
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
public class SpectrumFitter8 {

   private static final String[] SQRT_UNITS = new String[]{"eV", "eV/ch", "eV/ch<sup>1/2</sup>"};
   private static final String[] SQRT_NAMES = new String[]{"Zero", "Width", "Root"};
   private static final String[] POLYNOMIAL_UNITS = new String[]{"eV", "eV/ch", "eV/ch<sup>2</sup>", "eV/ch<sup>3</sup>", "eV/ch<sup>4</sup>",
         "eV/ch<sup>5</sup>", "eV/ch<sup>6</sup>", "eV/ch<sup>7</sup>", "eV/ch<sup>8</sup>", "eV/ch<sup>9</sup>", "eV/ch<sup>10</sup>",};
   private static final String[] POLYNOMIAL_NAMES = new String[]{"Constant", "Linear", "Quadratic", "Cubic", "Quartic", "Quintic", "Sextic", "Septic",
         "Octic", "Nonic", "Decic"};

   /**
    * <p>
    * Used to model the resolution of the detector using a standard fano
    * function / electronic noise model. The function may be used to fit 0
    * (nothing), 1 (noise only) or 2 (noise/fano) parameters.
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
    * @author Nicholas
    * @version 1.0
    */
   static public class FanoNoiseWidth extends FunctionImpl {

      private static final double ENERGY_PER_EH_PAIR = 3.64; // Modified from
      // 3.76
      private static final double DEFAULT_FANO = 0.12;
      private final Parameter mFano;
      private final Parameter mNoise;

      public FanoNoiseWidth(int nDim, double fano, double noise) {
         super();
         mFano = add(new ParameterObject<String>("Fano-Noise[Fano]", new Constraint.Bounded(DEFAULT_FANO, 0.1), fano, nDim > 1, "Fano"));
         mNoise = add(new ParameterObject<String>("Fano-Noise[Noise]", new Constraint.Positive(6.0), noise, nDim > 0, "Noise"));
      }

      public FanoNoiseWidth(double noise) {
         this(1, DEFAULT_FANO, noise);
      }

      public FanoNoiseWidth duplicate(SpectrumFitter8 sf8) throws EPQException, UtilException {
         final Map<Parameter, UncertainValue2> param = sf8.getBestFitParameters();
         final double fano = mFano.getUncertainValue(param).doubleValue();
         final double noise = mNoise.getUncertainValue(param).doubleValue();
         final int nFit = (mFano.isFit() ? 1 : 0) + (mNoise.isFit() ? 1 : 0);
         return new FanoNoiseWidth(nFit, fano, noise);
      }

      @Override
      public double compute(double eV, Map<Parameter, Double> param) {
         final double noise = mNoise.getValue(param);
         final double fano = mFano.getValue(param);
         return ENERGY_PER_EH_PAIR * Math.sqrt((noise * noise) + ((eV * fano) / ENERGY_PER_EH_PAIR));
      }

      @Override
      public double derivative(double eV, Map<Parameter, Double> param, Parameter idx) {
         if (isFitParameter(idx)) {
            final double noise = mNoise.getValue(param);
            final double fano = mFano.getValue(param);
            if (idx.equals(mFano))
               return 0.5 * eV * Math.pow((noise * noise) + ((eV * fano) / ENERGY_PER_EH_PAIR), -0.5);
            else if (idx.equals(mNoise))
               return (noise * ENERGY_PER_EH_PAIR) * Math.pow((noise * noise) + ((eV * fano) / ENERGY_PER_EH_PAIR), -0.5);
         }
         return 0.0;
      }

      @Override
      public UncertainValue2 computeU(double eV, Map<Parameter, UncertainValue2> param) {
         final UncertainValue2 noise = mNoise.getUncertainValue(param);
         final UncertainValue2 fano = mFano.getUncertainValue(param);
         return UncertainValue2.multiply(ENERGY_PER_EH_PAIR, UncertainValue2.sqrt(
               UncertainValue2.add(UncertainValue2.sqr(noise), UncertainValue2.divide(UncertainValue2.multiply(eV, fano), ENERGY_PER_EH_PAIR))));
      }

      @Override
      public String toString() {
         return "Fano-Noise" + super.toString();
      }

      public Parameter getFano() {
         return mFano;
      }

      public Parameter getNoise() {
         return mNoise;
      }
   }

   static public class EnergyScaleFunction extends FunctionImpl implements InvertableFunction {

      private final Parameter[] mCoeff;

      public EnergyScaleFunction(double[] coeff, int nFit) {
         super();
         assert coeff.length >= 2;
         mCoeff = new Parameter[nFit];
         for (int i = 0; i < mCoeff.length; ++i) {
            final double def = i < coeff.length ? coeff[i] : 0.0;
            mCoeff[i] = add(
                  new Parameter("pow[ch," + Integer.toString(i) + "]", new Constraint.Bounded(def, 100.0 * Math.pow(10.0, -3.0 * i)), def, true));
         }

      }

      public EnergyScaleFunction(double[] coeff, boolean[] fitP) {
         super();
         assert coeff.length >= 2;
         mCoeff = new Parameter[Math.max(coeff.length, fitP.length)];
         for (int i = 0; i < mCoeff.length; ++i)
            mCoeff[i] = add(new Parameter("pow[ch," + Integer.toString(i) + "]", new Constraint.Bounded(coeff[i], 100.0 * Math.pow(10.0, -3.0 * i)),
                  i < coeff.length ? coeff[i] : 0.0, i < fitP.length ? fitP[i] : false));
      }

      public EnergyScaleFunction duplicate(SpectrumFitter8 sf8) throws EPQException, UtilException {
         final Map<Parameter, UncertainValue2> param = sf8.getBestFitParameters();
         final double[] coeffs = new double[mCoeff.length];
         final boolean[] fitP = new boolean[mCoeff.length];
         for (int i = 0; i < coeffs.length; ++i) {
            coeffs[i] = mCoeff[i].getUncertainValue(param).doubleValue();
            fitP[i] = mCoeff[i].isFit();
         }
         return new EnergyScaleFunction(coeffs, fitP);
      }

      @Override
      public double compute(double ch, Map<Parameter, Double> param) {
         double res = 0.0;
         for (int i = mCoeff.length - 1; i >= 0; --i)
            res = (res * ch) + mCoeff[i].getValue(param);
         return res;
      }

      @Override
      public double derivative(double ch, Map<Parameter, Double> param, Parameter idx) {
         if (isFitParameter(idx))
            for (int i = 0; i < mCoeff.length; ++i)
               if (mCoeff[i].equals(idx))
                  return Math.pow(ch, i);
         return 0.0;
      }

      @Override
      public UncertainValue2 computeU(double ch, Map<Parameter, UncertainValue2> param) {
         UncertainValue2 res = UncertainValue2.ZERO;
         for (int i = mCoeff.length - 1; i >= 0; --i)
            res = UncertainValue2.add(UncertainValue2.multiply(ch, res), mCoeff[i].getUncertainValue(param));
         return res;
      }

      @Override
      public UncertainValue2 inverse(double eV, Map<Parameter, UncertainValue2> param) {
         final UncertainValue2 zeroOff = mCoeff[0].getUncertainValue(param);
         final UncertainValue2 width = mCoeff[1].getUncertainValue(param);
         final UncertainValue2 de = UncertainValue2.subtract(zeroOff, new UncertainValue2(eV));
         final UncertainValue2 linear = UncertainValue2.negate(UncertainValue2.divide(de, width));
         if (mCoeff.length == 2)
            return linear;
         else if (mCoeff.length == 3) {
            final UncertainValue2[] res = UncertainValue2.quadratic(mCoeff[2].getUncertainValue(param), width, de);
            if (res == null)
               return UncertainValue2.NaN;
            else
               return Math.abs(res[0].doubleValue() - linear.doubleValue()) < Math.abs(res[1].doubleValue() - linear.doubleValue()) ? res[0] : res[1];
         } else {
            final double[] coeffs = new double[mCoeff.length];
            for (int i = 0; i < coeffs.length; ++i)
               coeffs[i] = mCoeff[i].getUncertainValue(param).doubleValue();
            coeffs[0] -= eV;
            try {
               // Estimate answer based on a quadratic fit then search nearby
               final double[] est = Math2.quadraticSolver(coeffs[2], coeffs[1], coeffs[0]);
               final double bestEst = Math.abs(est[0] - linear.doubleValue()) < Math.abs(est[1] - linear.doubleValue()) ? est[0] : est[1];
               final double res = Math2.findRoot(coeffs, bestEst - 100.0, bestEst + 100.0, 0.01);
               return new UncertainValue2(res);
            } catch (final EPQException e) {
               return UncertainValue2.NaN;
            }
         }
      }

      public Parameter getCoefficient(int i) {
         return i < mCoeff.length ? mCoeff[i] : new Parameter("x^" + Integer.toString(i), 0.0, false);
      }

      public Parameter[] getCoefficients() {
         final Parameter[] res = new Parameter[mCoeff.length];
         for (int i = 0; i < res.length; ++i)
            res[i] = getCoefficient(i);
         return res;
      }

      @Override
      public String toString() {
         return "Polynomial Energy" + super.toString();
      }
   };

   static public class AltEnergyScaleFunction extends FunctionImpl implements InvertableFunction {

      private final Parameter[] mCoeff;

      public AltEnergyScaleFunction(double[] coeff) {
         super();
         assert (coeff.length >= 2) && (coeff.length <= 3);
         mCoeff = new Parameter[3];
         mCoeff[0] = add(new Parameter("e0", new Constraint.Bounded(coeff[0], 100.0), coeff[0], true));
         mCoeff[1] = add(new Parameter("scale", new Constraint.Bounded(coeff[1], 0.1), coeff[1], true));
         mCoeff[2] = add(new Parameter("root", new Constraint.None(), coeff.length > 2 ? coeff[2] : 0.0, true));
      }

      public AltEnergyScaleFunction duplicate(SpectrumFitter8 sf8) throws EPQException, UtilException {
         final Map<Parameter, UncertainValue2> param = sf8.getBestFitParameters();
         final double[] coeffs = new double[mCoeff.length];
         for (int i = 0; i < coeffs.length; ++i)
            coeffs[i] = mCoeff[i].getUncertainValue(param).doubleValue();
         return new AltEnergyScaleFunction(coeffs);
      }

      @Override
      public double compute(double ch, Map<Parameter, Double> param) {
         return mCoeff[0].getValue(param) + (ch * mCoeff[1].getValue(param)) + (mCoeff[2].getValue(param) * Math.sqrt(ch));
      }

      @Override
      public double derivative(double ch, Map<Parameter, Double> param, Parameter idx) {
         if (isFitParameter(idx))
            for (int i = 0; i < mCoeff.length; ++i)
               if (mCoeff[i].equals(idx))
                  switch (i) {
                     case 0 :
                        return 1.0;
                     case 1 :
                        return ch;
                     case 2 :
                        return Math.sqrt(ch);
                     default :
                        assert false;
                        return 0.0;
                  }
         return 0.0;
      }

      @Override
      public UncertainValue2 computeU(double ch, Map<Parameter, UncertainValue2> param) {
         return UncertainValue2.add(
               UncertainValue2.add(mCoeff[0].getUncertainValue(param), UncertainValue2.multiply(ch, mCoeff[1].getUncertainValue(param))),
               UncertainValue2.multiply(mCoeff[2].getUncertainValue(param), new UncertainValue2(Math.sqrt(ch))));
      }

      @Override
      public UncertainValue2 inverse(double eV, Map<Parameter, UncertainValue2> param) {
         final UncertainValue2 e0 = mCoeff[0].getUncertainValue(param);
         final UncertainValue2 s = mCoeff[1].getUncertainValue(param);
         final UncertainValue2 a = mCoeff[2].getUncertainValue(param);
         final UncertainValue2 de = UncertainValue2.subtract(e0, new UncertainValue2(eV));
         final UncertainValue2[] res = UncertainValue2.quadratic(s, a, de);
         if (res == null)
            return UncertainValue2.NaN;
         else {
            res[0] = UncertainValue2.sqr(res[0]);
            res[1] = UncertainValue2.sqr(res[1]);
            return Math.abs(computeU(res[0].doubleValue(), param).doubleValue() - eV) < Math
                  .abs(computeU(res[1].doubleValue(), param).doubleValue() - eV) ? res[0] : res[1];
         }
      }

      public Parameter getCoefficient(int i) {
         return i < mCoeff.length ? mCoeff[i] : new Parameter("C[" + Integer.toString(i) + "]", 0.0, false);
      }

      public Parameter[] getCoefficients() {
         final Parameter[] res = new Parameter[mCoeff.length];
         for (int i = 0; i < res.length; ++i)
            res[i] = getCoefficient(i);
         return res;
      }

      @Override
      public String toString() {
         return "Sqrt Energy Scale Function" + super.toString();
      }
   };

   /**
    * <p>
    * A lineset represents one or more x-ray transitions. When there is more
    * than one line, each line can be scaled independently to allow forcing the
    * relative weights of lines.
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
    * @author Nicholas
    * @version 1.0
    */
   private class Lineset extends FunctionImpl {

      private static final double MAX_INCOMPLETE = 0.1;
      private final String mBaseName;
      private final Parameter mAmplitude;
      private final Parameter mIncomplete;
      private final Element mElement;

      private final class LineData {
         final XRayTransition mTransition;
         final double mEnergy;
         final double mWeight;

         LineData(XRayTransition xrt) throws EPQException {
            this(xrt, xrt.getWeight(XRayTransition.NormalizeFamily));
         }

         LineData(XRayTransition xrt, double weight) throws EPQException {
            mTransition = xrt;
            mEnergy = FromSI.eV(xrt.getEnergy());
            mWeight = weight;
         }

         @Override
         public String toString() {
            return mTransition.toString();
         }

         final double compute(double eV, Map<Parameter, Double> param) {
            final double x = (eV - mEnergy) / mResolution.compute(mEnergy, param);
            assert (mIncomplete.getValue(param) >= 0) && (mIncomplete.getValue(param) <= MAX_INCOMPLETE)
                  : Double.toString(mIncomplete.getValue(param));
            return (mFitIncompleteCharge && (x < 0.0) ? 1.0 - (mIncomplete.getValue(param) * x) : 1.0) * mAmplitude.getValue(param) * mWeight
                  * Math.exp(-0.5 * Math2.sqr(x));
         }

         final UncertainValue2 computeU(double eV, Map<Parameter, UncertainValue2> param) {
            final UncertainValue2 x = UncertainValue2.divide(eV - mEnergy, mResolution.computeU(mEnergy, param));
            assert (mIncomplete.getUncertainValue(param).doubleValue() >= 0)
                  && (mIncomplete.getUncertainValue(param).doubleValue() <= MAX_INCOMPLETE);
            final UncertainValue2 k = (x.doubleValue() < 0.0) && mFitIncompleteCharge
                  ? UncertainValue2.subtract(UncertainValue2.ONE, UncertainValue2.multiply(x, mIncomplete.getUncertainValue(param)))
                  : UncertainValue2.ONE;
            return UncertainValue2.multiply(k, UncertainValue2.multiply(mAmplitude.getUncertainValue(param),
                  UncertainValue2.multiply(mWeight, UncertainValue2.exp(UncertainValue2.multiply(-0.5, UncertainValue2.sqr(x))))));
         }

         final UncertainValue2 integrate(Map<Parameter, UncertainValue2> param) {
            final double K = 4.0;
            final UncertainValue2 w = mResolution.computeU(mEnergy, param);
            final int chMin = (int) Math.round(mScale.inverse(Math.max(0.0, mEnergy - (K * w.doubleValue())), param).doubleValue());
            final int chMax = (int) Math.round(mScale.inverse(mEnergy + (K * w.doubleValue()), param).doubleValue());
            UncertainValue2 res = UncertainValue2.ZERO;
            for (int ch = chMin; ch <= chMax; ++ch)
               res = UncertainValue2.add(res, computeU(mScale.computeU(ch, param).doubleValue(), param));
            return res;
         }
      }

      private final ArrayList<LineData> mLines = new ArrayList<LineData>();

      private Lineset(XRayTransitionSet xrts, double scale, Constraint con) throws EPQException {
         mBaseName = xrts.toString();
         mAmplitude = add(new ParameterObject<XRayTransitionSet>(xrts.toString(), con, scale, true, xrts));
         mIncomplete = add(new ParameterObject<XRayTransitionSet>("Incomplete[" + xrts.toString() + "]",
               new Constraint.Bounded(0.5 * MAX_INCOMPLETE, MAX_INCOMPLETE), 0.0 * MAX_INCOMPLETE, mFitIncompleteCharge, xrts));
         mElement = xrts.getElement();
         add(mResolution.getParameters(true));
         add(mScale.getParameters(true));
         for (final XRayTransition xrt : xrts.getTransitions())
            addLine(xrt);
      }

      private Lineset(XRayTransitionSet xrts, double scale) throws EPQException {
         this(xrts, scale, new Constraint.Positive(scale));
      }

      private Lineset(XRayTransition xrt, double scale) throws EPQException {
         this(new XRayTransitionSet(xrt), scale);
      }

      private Lineset(XRayTransition xrt, double scale, Constraint con) throws EPQException {
         this(new XRayTransitionSet(xrt), scale, con);
      }

      public void addLine(XRayTransition xrt) throws EPQException {
         assert mElement.equals(xrt.getElement());
         // assert xrt.getDestination().getGroundStateOccupancy() > 1 :
         // "Ground state of " + xrt + " is empty";
         // assert xrt.getSource().getGroundStateOccupancy() > 1 :
         // "Ground state of " + xrt + " is empty";
         if (mElement.equals(xrt.getElement()) && (xrt.getDestination().getGroundStateOccupancy() > 0)
               && (xrt.getSource().getGroundStateOccupancy() > 0))
            mLines.add(new LineData(xrt));
      }

      @Override
      public double compute(double ch, Map<Parameter, Double> param) {
         double res = 0.0;
         final double eV = mScale.compute(ch, param);
         for (final LineData ld : mLines)
            res += ld.compute(eV, param);
         return res;
      }

      @Override
      public double derivative(double ch, Map<Parameter, Double> param, Parameter idx) {
         if (isFitParameter(idx))
            if (idx.equals(mAmplitude))
               return compute(ch, param) / mAmplitude.getValue(param); // ok
            else {
               if (mScale.isFitParameter(idx)) {
                  final double eV = mScale.compute(ch, param);
                  double res = 0.0;
                  final double k = mIncomplete.getValue(param);
                  for (final LineData ld : mLines) {
                     final double r = mResolution.compute(ld.mEnergy, param);
                     final double c = ld.mEnergy;
                     final double x = (c - eV) / r;
                     final double f = ld.mWeight * Math.exp(-0.5 * Math2.sqr(x));
                     if (mFitIncompleteCharge && (eV < c))
                        res += ((x / r) - (k / ((k * (c - eV)) + r))) * f;
                     else
                        res += (x * f) / r;
                  }
                  return mAmplitude.getValue(param) * res * mScale.derivative(ch, param, idx);
               }
               if (mResolution.isFitParameter(idx)) {
                  double res = 0.0;
                  final double eV = mScale.compute(ch, param);
                  final double k = mIncomplete.getValue(param);
                  for (final LineData ld : mLines) {
                     final double r = mResolution.compute(ld.mEnergy, param);
                     final double c = ld.mEnergy;
                     final double xx2 = Math2.sqr((eV - c) / r);
                     final double f = ld.mWeight * Math.exp(-0.5 * xx2);
                     if (mFitIncompleteCharge && (eV < c))
                        res += (((xx2 / r) - (1.0 / r)) + (1.0 / ((k * (c - eV)) + r))) * f * mResolution.derivative(ld.mEnergy, param, idx);
                     else
                        res += ((f * xx2) / r) * mResolution.derivative(ld.mEnergy, param, idx);
                  }
                  return mAmplitude.getValue(param) * res;
               }
               if (mFitIncompleteCharge && (idx == mIncomplete)) {
                  double res = 0.0;
                  final double eV = mScale.compute(ch, param);
                  final double k = mIncomplete.getValue(param);
                  for (final LineData ld : mLines) {
                     final double r = mResolution.compute(ld.mEnergy, param);
                     final double c = ld.mEnergy;
                     final double xx2 = Math2.sqr((eV - c) / r);
                     final double f = ld.mWeight * Math.exp(-0.5 * xx2);
                     if (mFitIncompleteCharge && (eV < c))
                        res += ((c - eV) / ((k * (c - eV)) + r)) * f;
                  }
                  return mAmplitude.getValue(param) * res;
               }
               assert false;
            }
         return 0.0;
      }

      @Override
      public UncertainValue2 computeU(double ch, Map<Parameter, UncertainValue2> param) {
         UncertainValue2 res = UncertainValue2.ZERO;
         final UncertainValue2 eV = mScale.computeU(ch, param);
         for (final LineData ld : mLines) {
            final UncertainValue2 xx = UncertainValue2.divide(UncertainValue2.subtract(eV, new UncertainValue2(ld.mEnergy)),
                  mResolution.computeU(eV.doubleValue(), param));
            res = UncertainValue2.add(res,
                  UncertainValue2.multiply(ld.mWeight, UncertainValue2.exp(UncertainValue2.multiply(-0.5, UncertainValue2.multiply(xx, xx)))));
         }
         return UncertainValue2.multiply(mAmplitude.getUncertainValue(param), res);
      }

      public UncertainValue2 integrate(Map<Parameter, UncertainValue2> param) {
         UncertainValue2 res = UncertainValue2.ZERO;
         for (final LineData ld : mLines)
            res = UncertainValue2.add(res, ld.integrate(param));
         return res;
      }

      @Override
      public String toString() {
         return "Lineset[" + mBaseName + "]" + super.toString();
      }
   }

   public MultiLineset buildWeighted(RegionOfInterestSet rois) throws EPQException {
      final MultiLineset mls = new MultiLineset();
      for (final RegionOfInterest roi : rois)
         for (final Element elm : roi.getElementSet()) {
            final XRayTransitionSet xrts = roi.getXRayTransitionSet(elm);
            mls.add(new Lineset(xrts, 1.0));
         }
      return mls;
   }

   /**
    * Creates a MultiLineset build of (mostly) independent transitions. The
    * mostly comes from the limit that lines which are too close (as determined
    * by de) are fit as a unit.
    * 
    * @param rois
    *           RegionOfInterestSet
    * @param de
    *           Min separation between distinct lines in eV
    * @param fraction
    *           The fractional variability to permit in the NL fit
    * @return MultiLineset
    * @throws EPQException
    */
   public MultiLineset buildIndependent(RegionOfInterestSet rois, double de, double fraction) throws EPQException {
      final MultiLineset mls = new MultiLineset();
      for (final RegionOfInterest roi : rois)
         for (final Element elm : roi.getElementSet()) {
            final Set<XRayTransition> ssxrt = new TreeSet<XRayTransition>(new Comparator<XRayTransition>() {
               @Override
               public int compare(XRayTransition o1, XRayTransition o2) {
                  int res = 0;
                  try {
                     res = Double.compare(o1.getEnergy(), o2.getEnergy());
                  } catch (final EPQException e) {
                     e.printStackTrace();
                  }
                  return res != 0 ? res : o1.compareTo(o2);
               }
            });
            ssxrt.addAll(roi.getXRayTransitionSet(elm).getTransitions());
            XRayTransitionSet xrts = new XRayTransitionSet();
            XRayTransition prev = null;
            for (final XRayTransition xrt : ssxrt) {
               if ((prev == null) || (Math.abs(FromSI.eV(xrt.getEnergy() - prev.getEnergy())) < de))
                  xrts.add(xrt);
               else {
                  mls.add(new Lineset(xrts, 1.0, new Constraint.Fractional(xrts.toString(), 1.0, fraction)));
                  xrts = new XRayTransitionSet(xrt);
               }
               prev = xrt;
            }
            if (xrts.size() > 0)
               mls.add(new Lineset(xrts, 1.0, new Constraint.Fractional(xrts.toString(), 1.0, fraction)));
         }
      return mls;
   }

   /**
    * Break up the weighted MultiLineset into distinct Gaussian sub-units which
    * more closely represent individual transitions. If de == 0.0, then the
    * XRayTransitions are each added separately and fit separately. If
    * de&gt;0.0, then if two XRayTransition objects differ in energy by less
    * than de then the XRayTransition objects are placed in the same Lineset and
    * will be fit as a unit.
    * 
    * @param mls
    *           MultiLineset
    * @param param
    *           Map&lt;Parameter, UncertainValue&gt;
    * @param de
    *           The energy spacing in eV
    * @param fraction
    *           Change to permit in each fit parameter
    * @return MultiLineset
    * @throws EPQException
    */
   public MultiLineset makeIndependent(MultiLineset mls, Map<Parameter, UncertainValue2> param, double de, double fraction) throws EPQException {
      final MultiLineset res = new MultiLineset();
      // Sort the LineData in element, energy order...
      final TreeMap<Lineset.LineData, Lineset> ssld = new TreeMap<Lineset.LineData, Lineset>(new Comparator<Lineset.LineData>() {
         @Override
         public int compare(Lineset.LineData o1, Lineset.LineData o2) {
            int res = o1.mTransition.getElement().compareTo(o2.mTransition.getElement());
            if (res == 0.0)
               res = Double.compare(o1.mEnergy, o2.mEnergy);
            return res == 0 ? o1.mTransition.compareTo(o2.mTransition) : res;
         }
      });
      final Set<Element> elms = new TreeSet<Element>();
      for (final Lineset ls : mls)
         for (final Lineset.LineData ld : ls.mLines) {
            ssld.put(ld, ls);
            elms.add(ld.mTransition.getElement());
         }
      for (final Element elm : elms) {
         Lineset prev = null;
         double prevE = 3.0e300;
         for (final Map.Entry<Lineset.LineData, Lineset> me : ssld.entrySet()) {
            final Lineset.LineData ld = me.getKey();
            final XRayTransition xrt = ld.mTransition;
            if (xrt.getElement().equals(elm)) {
               if ((prev != null) && (Math.abs(prevE - ld.mEnergy) < de))
                  prev.addLine(xrt);
               else {
                  final double scale = me.getValue().mAmplitude.getUncertainValue(param).doubleValue();
                  final double fr = (xrt.getFamily() != AtomicShell.KFamily ? 1.0 : 0.2) * fraction;
                  prev = new Lineset(xrt, scale, new Constraint.Fractional(xrt.toString(), scale, fr));
                  res.add(prev);
               }
               prevE = ld.mEnergy;
            }
         }
      }
      return res;
   }

   public class MultiLineset extends FunctionImpl implements Iterable<Lineset> {

      final private ArrayList<Lineset> mLines = new ArrayList<Lineset>();

      private MultiLineset() {
         add(mResolution.getParameters(true));
         add(mScale.getParameters(true));
      }

      public void add(Lineset ls) {
         mLines.add(ls);
         add(ls.getParameters(true));
      }

      @Override
      public Iterator<Lineset> iterator() {
         return mLines.iterator();
      }

      public int size() {
         return mLines.size();
      }

      public Lineset get(int i) {
         return mLines.get(i);
      }

      @Override
      public double compute(double ch, Map<Parameter, Double> param) {
         double res = 0.0;
         for (final Lineset ls : mLines)
            res += ls.compute(ch, param);
         return res;
      }

      @Override
      public double derivative(double ch, Map<Parameter, Double> param, Parameter idx) {
         double res = 0.0;
         if (isFitParameter(idx))
            for (final Lineset ls : mLines)
               res += ls.derivative(ch, param, idx);
         return res;
      }

      @Override
      public UncertainValue2 computeU(double ch, Map<Parameter, UncertainValue2> param) {
         UncertainValue2 res = UncertainValue2.ZERO;
         for (final Lineset ls : mLines) {
            final UncertainValue2 uv = ls.computeU(ch, param);
            if (!uv.equals(UncertainValue2.ZERO))
               res = UncertainValue2.add(res, uv);
         }
         return res;
      }

      public UncertainValue2 integrate(Parameter p, Map<Parameter, UncertainValue2> param) {
         UncertainValue2 res = UncertainValue2.ZERO;
         for (final Lineset ls : mLines)
            if (ls.getParameters(true).contains(p))
               res = UncertainValue2.add(res, ls.integrate(param));
         return res;
      }

      @Override
      public String toString() {
         final StringBuffer sb = new StringBuffer();
         sb.append("MultiLineset[");
         boolean first = true;
         for (final Lineset ls : mLines) {
            if (!first)
               sb.append(", ");
            first = false;
            sb.append(ls.toString());
         }
         sb.append("]");
         return sb.toString();
      }

      public String dump(double[] chs, Set<Parameter> param) throws EPQException {
         final Map<Parameter, Double> pm = new HashMap<Parameter, Double>();
         for (final Parameter p : param)
            pm.put(p, p.getDefaultValue());
         final ISpectrumData back = getCharacteristicSpectrum();
         final StringBuffer sb = new StringBuffer(8192);
         for (final double ch : chs) {
            sb.append(ch);
            sb.append("\t");
            sb.append(mScale.compute(ch, pm));
            for (final Lineset ls : mLines) {
               sb.append("\t");
               sb.append(ls.compute(ch, pm));
            }
            sb.append("\t");
            final int chi = (int) Math.round(ch);
            sb.append(back.getCounts(chi));
            sb.append("\t");
            sb.append(Math.sqrt(Math2.positive(mSpectrum.getCounts(chi))));
            sb.append("\n");
         }
         return sb.toString();
      }
   }

   private final EDSDetector mDetector;
   private final Composition mComposition;
   private final ISpectrumData mSpectrum;

   private MultiLineset mMultiLines;
   private boolean mFitIncompleteCharge = false;
   private InvertableFunction mScale;
   private Function mResolution;
   // A listener
   private ActionListener mListener;

   private transient RegionOfInterestSet mROIS;
   private transient ISpectrumData mBackground;
   private transient ISpectrumData mRoughFit;
   private transient ISpectrumData mBestFit;
   private transient Map<Parameter, UncertainValue2> mBestFitParam = null;
   private transient ArrayList<SpectrumFitResult> mResults = new ArrayList<SpectrumFitResult>();

   private BremsstrahlungAnalytic.QuadraticBremsstrahlung mBremsstrahlung;

   /**
    * Constructs a SpectrumFitter8 to calibrated the specified detector using
    * the specified spectrum collected from the specified composition material.
    * 
    * @param det
    * @param comp
    * @param spec
    */
   public SpectrumFitter8(EDSDetector det, Composition comp, ISpectrumData spec) {
      mDetector = det;
      mComposition = comp;
      mSpectrum = spec;
   }

   /**
    * Constructs a SpectrumFitter8 to reuse the energy and resolution
    * calibration provided by detCal on the ISpectrumData spec which was
    * collected on a material of Composition comp.
    * 
    * @param detCal
    * @param comp
    * @param spec
    * @throws UtilException
    * @throws EPQException
    */
   public SpectrumFitter8(SpectrumFitter8 detCal, Composition comp, ISpectrumData spec) throws EPQException, UtilException {
      mDetector = detCal.mDetector;
      mComposition = comp;
      mSpectrum = spec;
      if (detCal.mResolution instanceof FanoNoiseWidth) {
         mResolution = ((FanoNoiseWidth) detCal.mResolution).duplicate(detCal);
         for (final Parameter p : mResolution.getParameters(true))
            p.setIsFit(false);
      }
      if (detCal.mScale instanceof EnergyScaleFunction)
         mScale = ((EnergyScaleFunction) detCal.mScale).duplicate(detCal);
      else if (detCal.mScale instanceof AltEnergyScaleFunction)
         mScale = ((AltEnergyScaleFunction) detCal.mScale).duplicate(detCal);
      if (mScale != null)
         for (final Parameter p : mScale.getParameters(true))
            p.setIsFit(false);
   }

   public RegionOfInterestSet getROIS() {
      if (mROIS == null) {
         final double e0 = mSpectrum.getProperties().getNumericWithDefault(SpectrumProperties.BeamEnergy, Double.NaN);
         assert !Double.isNaN(e0);
         final DetectorLineshapeModel dlm = mDetector.getDetectorLineshapeModel();
         final double fwhm = dlm.getFWHMatMnKa();
         final double ROIS_FACTOR = 0.2 * fwhm;
         final RegionOfInterestSet rois = new RegionOfInterestSet(dlm, 1.0e-6, ToSI.eV(ROIS_FACTOR), ToSI.eV(0.0));
         for (final Element elm : mComposition.getElementSet())
            rois.add(mDetector.getVisibleTransitions(elm, ToSI.keV(e0)));
         mROIS = rois;
      }
      return mROIS;
   }

   private static ISpectrumData extractCharacteristicSpectrum(ISpectrumData spec, EDSDetector det, Composition comp, RegionOfInterestSet rois,
         BremsstrahlungAnalytic bs) throws EPQException {
      final EditableSpectrum es = new EditableSpectrum(spec);
      final double[] data = es.getCounts();
      Arrays.fill(data, 0.0);
      final ISpectrumData brem = bs.fitBackground2(det, spec, comp);
      for (final RegionOfInterestSet.RegionOfInterest roi : rois) {
         final double eLow = FromSI.eV(roi.lowEnergy());
         final double eHigh = FromSI.eV(roi.highEnergy());
         final int lowCh = SpectrumUtils.bound(spec, SpectrumUtils.channelForEnergy(spec, eLow));
         final int highCh = SpectrumUtils.bound(spec, SpectrumUtils.channelForEnergy(spec, eHigh));
         final double minSrc = SpectrumUtils.estimateLowBackground(spec, lowCh)[0];
         final double maxSrc = SpectrumUtils.estimateHighBackground(spec, highCh)[0];
         final double minBack = SpectrumUtils.estimateLowBackground(brem, lowCh)[0];
         final double maxBack = SpectrumUtils.estimateHighBackground(brem, highCh)[0];
         final double minSc = (minBack > 0.0) && (minSrc > 0.0) ? (minSrc / minBack) : Double.MAX_VALUE;
         final double maxSc = (maxBack > 0.0) && (maxSrc > 0.0) ? (maxSrc / maxBack) : Double.MAX_VALUE;
         for (int i = lowCh; i < highCh; ++i) {
            final double k = ((double) (i - lowCh)) / ((double) (highCh - lowCh));
            data[i] = spec.getCounts(i) - ((minSc + (k * (maxSc - minSc))) * brem.getCounts(i));
         }
      }
      return SpectrumUtils.copy(es);
   }

   public ISpectrumData getCharacteristicSpectrum() throws EPQException {
      if (mBackground == null) {
         // mBremsstrahlung = new BremsstrahlungAnalytic.DTSAQuadratic();
         mBremsstrahlung = new BremsstrahlungAnalytic.Lifshin1974Model();
         mBackground = extractCharacteristicSpectrum(mSpectrum, mDetector, mComposition, getROIS(), mBremsstrahlung);
         SpectrumUtils.rename(mBackground, "Characteristic[" + mSpectrum + "]");
      }
      return mBackground;
   }

   public ISpectrumData getBremsstrahlungSpectrum() throws EPQException {
      final EditableSpectrum es = new EditableSpectrum(mSpectrum);
      final ISpectrumData bkgd = getCharacteristicSpectrum();
      for (int i = 0; i < es.getChannelCount(); ++i)
         es.setCounts(i, es.getCounts(i) - bkgd.getCounts(i));
      SpectrumUtils.rename(es, "Bremsstrahlung[" + mSpectrum + "]");
      return SpectrumUtils.copy(es);
   }

   static private double[] createChannelList(ISpectrumData spec, final RegionOfInterestSet rois) {
      // Create an array of the channels containing data to be fit.
      final ArrayList<Integer> tmp = new ArrayList<Integer>(1024);
      final int lld = SpectrumUtils.getZeroStrobeDiscriminatorChannel(spec);
      for (final RegionOfInterest roi : rois) {
         final int minCh = Math.max(lld, SpectrumUtils.bound(spec, SpectrumUtils.channelForEnergy(spec, FromSI.eV(roi.lowEnergy()))));
         final int maxCh = Math.max(lld, SpectrumUtils.bound(spec, SpectrumUtils.channelForEnergy(spec, FromSI.eV(roi.highEnergy()))));
         for (int ch = minCh; ch < maxCh; ++ch)
            tmp.add(ch);
      }
      final double[] chD = new double[tmp.size()];
      for (int i = 0; i < tmp.size(); ++i)
         chD[i] = tmp.get(i).intValue();
      return chD;
   }

   private double[] getPeakData(double[] chs) throws EPQException {
      final double[] res = new double[chs.length];
      final ISpectrumData back = getCharacteristicSpectrum();
      for (int i = 0; i < res.length; ++i)
         res[i] = back.getCounts((int) Math.round(chs[i]));
      return res;
   }

   private double[] getPeakSigma(double[] chs) throws EPQException {
      final double[] res = new double[chs.length];
      for (int i = 0; i < res.length; ++i) {
         final double tmp = mSpectrum.getCounts((int) Math.round(chs[i]));
         res[i] = tmp > 0.0 ? Math.sqrt(tmp) : Double.MAX_VALUE;
      }
      return res;
   }

   public Set<Parameter> getParameters() {
      return mMultiLines != null ? mMultiLines.getParameters(false) : new HashSet<Parameter>();
   }

   private class CrudeLinearFit extends LinearLeastSquares {

      private final Map<Parameter, Double> mParam;

      private CrudeLinearFit(double[] chs, Map<Parameter, Double> param) throws EPQException {
         super(chs, getPeakData(chs), getPeakSigma(chs));
         mParam = param;
      }

      @Override
      protected int fitFunctionCount() {
         return mMultiLines.size();
      }

      @Override
      protected void fitFunction(double xi, double[] afunc) {
         for (int i = 0; i < mMultiLines.size(); ++i)
            afunc[i] = mMultiLines.get(i).compute(xi, mParam);
      }

      protected ISpectrumData getFit(double[] chD) throws EPQException {
         final EditableSpectrum es = new EditableSpectrum(getCharacteristicSpectrum());
         Arrays.fill(es.getCounts(), 0.0);
         final UncertainValue2[] ffs = getResults();
         for (final double element : chD) {
            double tmp = 0.0;
            for (int j = 0; j < mMultiLines.size(); ++j)
               tmp += ffs[j].doubleValue() * mMultiLines.get(j).compute(element, mParam);
            es.setCounts((int) Math.round(element), tmp);
         }
         SpectrumUtils.rename(es, "RoughFit[" + es + "]");
         return es;
      }
   }

   /**
    * <p>
    * After using the full non-linear fitting apparatus to fit the energy scale,
    * the resolution and weights relative to one spectrum, it may be useful to
    * use the same energy scale and resolution functions to perform a linear
    * scale-only fit on other spectra. That is what this inner class does. It
    * takes the scale and resolution fit from the previously calibrated spectrum
    * in SpectrumFitter8 and holds this firm while adjusting the intensity
    * scales to best fit a new spectrum. The fit weights are then returned.
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
   public class HighQualityLinearFit extends LinearLeastSquares {

      private final MultiLineset mLineset;
      private final Map<Parameter, Double> mParameters;
      private final ISpectrumData mHQSpectrum;
      private final Composition mHQComposition;
      private final ISpectrumData mHQCharSpectrum;

      public HighQualityLinearFit(ISpectrumData spec, Composition comp, double de) throws EPQException, UtilException {
         final double e0 = spec.getProperties().getNumericWithDefault(SpectrumProperties.BeamEnergy, Double.NaN);
         assert !Double.isNaN(e0);
         final DetectorLineshapeModel dlm = mDetector.getDetectorLineshapeModel();
         final double fwhm = dlm.getFWHMatMnKa();
         final RegionOfInterestSet rois = new RegionOfInterestSet(dlm, 1.0e-6, ToSI.eV(0.3 * fwhm), ToSI.eV(0.3 * fwhm));
         for (final Element elm : comp.getElementSet())
            rois.add(mDetector.getVisibleTransitions(elm, ToSI.keV(e0)));
         mLineset = buildIndependent(rois, de, 1.0);
         mParameters = new HashMap<Parameter, Double>();
         for (final Parameter p : mLineset.getParameters(true)) {
            p.setIsFit(false);
            mParameters.put(p, p.getDefaultValue());
         }
         // Recover the energy scale and resolution calibration from the
         // SpectrumFitter8 object.
         final Map<Parameter, UncertainValue2> nlParams = SpectrumFitter8.this.getBestFitParameters();
         for (final Parameter p : mScale.getParameters(true)) {
            final UncertainValue2 uv = nlParams.get(p);
            if (uv != null)
               p.setDefaultValue(uv.doubleValue());
            mParameters.put(p, p.getDefaultValue());
         }
         for (final Parameter p : mResolution.getParameters(true)) {
            final UncertainValue2 uv = nlParams.get(p);
            if (uv != null)
               p.setDefaultValue(uv.doubleValue());
            mParameters.put(p, p.getDefaultValue());
         }
         mHQComposition = comp;
         mHQSpectrum = spec;
         mHQCharSpectrum = extractCharacteristicSpectrum(spec, mDetector, comp, rois, new BremsstrahlungAnalytic.DTSAQuadratic());
         final double[] x = createChannelList(mHQSpectrum, rois);
         final double[] y = new double[x.length], sigma = new double[x.length];
         for (int i = 0; i < x.length; ++i) {
            final int ch = (int) Math.round(x[i]);
            y[i] = mHQCharSpectrum.getCounts(ch);
            sigma[i] = Math.sqrt(Math.max(1.0, mHQSpectrum.getCounts(ch)));
         }
         setData(x, y, sigma);
      }

      @Override
      protected int fitFunctionCount() {
         return mLineset.size();
      }

      @Override
      protected void fitFunction(double xi, double[] afunc) {
         for (int i = 0; i < mLineset.size(); ++i)
            afunc[i] = mLineset.get(i).compute(xi, mParameters);
      }

      public Map<Parameter, UncertainValue2> getBestFitParameters() throws EPQException {
         final Map<Parameter, UncertainValue2> res = new HashMap<Parameter, UncertainValue2>();
         final UncertainValue2[] fitRes = getResults();
         for (int i = 0; i < mLineset.size(); ++i) {
            final Parameter p = mLineset.get(i).mAmplitude;
            p.setIsFit(true);
            res.put(p, UncertainValue2.multiply(p.getDefaultValue(), fitRes[i]));
         }
         return res;
      }

      public ISpectrumData getFit() throws EPQException {
         final EditableSpectrum es = new EditableSpectrum(mHQSpectrum);
         final UncertainValue2[] ffs = getResults();
         for (final double element : mXCoordinate) {
            final int ch = (int) Math.round(element);
            double tmp = es.getCounts(ch) - mHQCharSpectrum.getCounts(ch);
            for (int j = 0; j < mLineset.size(); ++j)
               tmp += ffs[j].doubleValue() * mLineset.get(j).compute(element, mParameters);
            es.setCounts((int) Math.round(element), tmp);
         }
         SpectrumUtils.rename(es, "HighQFit[" + es + "]");
         return es;
      }

      public String tabulate() throws EPQException {
         final Map<Parameter, UncertainValue2> bfp = getBestFitParameters();
         final StringBuffer sb1 = new StringBuffer(), sb2 = new StringBuffer(), sb3 = new StringBuffer();
         sb1.append("Parameter");
         sb2.append("Fit");
         for (final Map.Entry<Parameter, UncertainValue2> me : bfp.entrySet()) {
            sb1.append("\t");
            sb1.append(me.getKey().getName());
            sb1.append("\t");
            sb1.append("U(" + me.getKey().getName() + ")");

            sb3.append("\t");
            sb3.append(me.getValue().doubleValue());
            sb3.append("\t");
            sb3.append(me.getValue().uncertainty());

         }
         sb1.append("\n");
         sb1.append(sb2);
         sb1.append("\n");
         sb1.append(sb3);
         return sb1.toString();
      }

      public SpectrumFitResult getResult() throws EPQException {
         final SpectrumFitResult res = new SpectrumFitResult(mHQSpectrum, mHQComposition);
         final Map<Parameter, UncertainValue2> pm = getBestFitParameters();
         for (final Lineset ls : mLineset)
            for (final Lineset.LineData ld : ls.mLines) {
               final UncertainValue2 gaussianW = mResolution.computeU(ld.mEnergy, pm);
               res.addTransition(ld.mTransition, ld.computeU(ld.mEnergy, pm), ld.integrate(pm), gaussianW, mScale.inverse(ld.mEnergy, pm));
            }
         if (mResolution instanceof FanoNoiseWidth)
            res.setResolution(((FanoNoiseWidth) mResolution).getFano().getUncertainValue(pm),
                  ((FanoNoiseWidth) mResolution).getNoise().getUncertainValue(pm));
         if (mScale instanceof EnergyScaleFunction) {
            final EnergyScaleFunction esf = (EnergyScaleFunction) mScale;
            final Parameter[] ps = esf.getCoefficients();
            final UncertainValue2[] coeffs = new UncertainValue2[ps.length];
            for (int i = 0; i < ps.length; ++i)
               coeffs[i] = ps[i].getUncertainValue(pm);
            res.setEnergyCalibration(coeffs, POLYNOMIAL_NAMES, POLYNOMIAL_UNITS);
         } else if (mScale instanceof AltEnergyScaleFunction) {
            final AltEnergyScaleFunction psf = (AltEnergyScaleFunction) mScale;
            final Parameter[] ps = psf.getCoefficients();
            final UncertainValue2[] calib = new UncertainValue2[ps.length];
            for (int i = 0; i < ps.length; ++i)
               calib[i] = ps[i].getUncertainValue(pm);
            res.setEnergyCalibration(calib, SQRT_NAMES, SQRT_UNITS);
         }
         return res;
      }
   };

   /**
    * Performs a very crude fit as a first estimate of the amplitude of the
    * various linesets.
    * 
    * @param chD
    * @throws EPQException
    */
   private void roughFit(double[] chD) throws EPQException {
      final Map<Parameter, Double> param = new HashMap<Parameter, Double>();
      for (final Parameter p : getParameters())
         param.put(p, p.getDefaultValue());
      final CrudeLinearFit rlf = new CrudeLinearFit(chD, param);
      final UncertainValue2[] res = rlf.getResults();
      for (int i = 0; i < mMultiLines.size(); ++i) {
         final Parameter a = mMultiLines.get(i).mAmplitude;
         if (Double.isNaN(res[i].doubleValue()))
            throw new EPQException("The rough linear fit for the parameter " + a + " is NaN.");
         final double val = Math.max(0.001, a.getDefaultValue() * res[i].doubleValue());
         a.setDefaultValue(val);
         a.setConstraint(new Constraint.Positive(val));
      }
      mRoughFit = rlf.getFit(chD);
   }

   public SpectrumFitResult compute() throws EPQException, UtilException {
      final SpectrumFitResult res = new SpectrumFitResult(mSpectrum, mComposition);
      mResults.clear();
      assert mMultiLines != null;
      final double[] chD = createChannelList(mSpectrum, getROIS());
      roughFit(chD);
      // Fit the function mLines to the channels chD containing data
      final LevenbergMarquardtParameterized lmp = new LevenbergMarquardtParameterized();
      if (mListener != null)
         lmp.addActionListener(mListener);
      final ParameterizedFitResult fr = lmp.compute(mMultiLines, chD, getPeakData(chD), getPeakSigma(chD));
      { // Compute the best fit spectrum
         final Map<Parameter, Double> param = fr.getResults();
         final EditableSpectrum fit = new EditableSpectrum(getBremsstrahlungSpectrum());
         for (final double element : chD) {
            final int ch = (int) Math.round(element);
            fit.setCounts(ch, mMultiLines.compute(element, param) + fit.getCounts(ch));
         }
         SpectrumUtils.rename(fit, "BestFit[" + mSpectrum + "]");
         mBestFit = fit;
      }
      final Map<Parameter, UncertainValue2> pm = fr.getParameterMap();
      mBestFitParam = pm;
      if (mScale instanceof EnergyScaleFunction) {
         final EnergyScaleFunction esf = (EnergyScaleFunction) mScale;
         final Parameter[] ps = esf.getCoefficients();
         final UncertainValue2[] coeffs = new UncertainValue2[ps.length];
         for (int i = 0; i < ps.length; ++i)
            coeffs[i] = ps[i].getUncertainValue(pm);
         res.setEnergyCalibration(coeffs, POLYNOMIAL_NAMES, POLYNOMIAL_UNITS);
      } else if (mScale instanceof AltEnergyScaleFunction) {
         final AltEnergyScaleFunction psf = (AltEnergyScaleFunction) mScale;
         final Parameter[] ps = psf.getCoefficients();
         final UncertainValue2[] calib = new UncertainValue2[ps.length];
         for (int i = 0; i < ps.length; ++i)
            calib[i] = ps[i].getUncertainValue(pm);
         res.setEnergyCalibration(calib, SQRT_NAMES, SQRT_UNITS);
      }
      if (mResolution instanceof FanoNoiseWidth)
         res.setResolution(((FanoNoiseWidth) mResolution).getFano().getUncertainValue(pm),
               ((FanoNoiseWidth) mResolution).getNoise().getUncertainValue(pm));
      for (final Lineset ls : mMultiLines)
         for (final Lineset.LineData ld : ls.mLines) {
            final UncertainValue2 gaussianW = mResolution.computeU(ld.mEnergy, pm);
            res.addTransition(ld.mTransition, ld.computeU(ld.mEnergy, pm), ld.integrate(pm), gaussianW, mScale.inverse(ld.mEnergy, pm));
         }
      res.setBremsstrahlungModel(mBremsstrahlung, new UncertainValue2(mBremsstrahlung.mA), new UncertainValue2(mBremsstrahlung.mB));
      res.setBremsstrahlungSpectrum(getBremsstrahlungSpectrum());
      if (mListener != null)
         mListener.actionPerformed(new ActionEvent(this, 100, null));
      mResults.add(res);
      return res;
   }

   /**
    * Recomputes the fit but first splits weighted transitions apart so as to
    * fit the weights of lines.
    * 
    * @param de
    * @param fraction
    * @return SpectrumFitResult
    * @throws EPQException
    */
   public SpectrumFitResult recompute(double de, double fraction) throws EPQException {
      assert mMultiLines != null;
      mMultiLines = makeIndependent(mMultiLines, mBestFitParam, de, fraction);
      final double[] chD = createChannelList(mSpectrum, getROIS());
      // Fit the function mLines to the channels chD containing data
      final LevenbergMarquardtParameterized lmp = new LevenbergMarquardtParameterized();
      if (mListener != null)
         lmp.addActionListener(mListener);
      final ParameterizedFitResult fr = lmp.compute(mMultiLines, chD, getPeakData(chD), getPeakSigma(chD));
      { // Compute the best fit spectrum
         final Map<Parameter, Double> param = fr.getResults();
         final EditableSpectrum fit = new EditableSpectrum(getBremsstrahlungSpectrum());
         for (final double element : chD) {
            final int ch = (int) Math.round(element);
            fit.setCounts(ch, mMultiLines.compute(element, param) + fit.getCounts(ch));
         }
         SpectrumUtils.rename(fit, "BestFit[" + mSpectrum + "]");
         mBestFit = fit;
      }
      final Map<Parameter, UncertainValue2> pm = fr.getParameterMap();
      mBestFitParam = pm;
      final SpectrumFitResult res = new SpectrumFitResult(mSpectrum, mComposition);
      if (mResolution instanceof FanoNoiseWidth)
         res.setResolution(((FanoNoiseWidth) mResolution).getFano().getUncertainValue(pm),
               ((FanoNoiseWidth) mResolution).getNoise().getUncertainValue(pm));
      if (mScale instanceof EnergyScaleFunction) {
         final EnergyScaleFunction esf = (EnergyScaleFunction) mScale;
         final Parameter[] ps = esf.getCoefficients();
         final UncertainValue2[] coeffs = new UncertainValue2[ps.length];
         for (int i = 0; i < ps.length; ++i)
            coeffs[i] = ps[i].getUncertainValue(pm);
         res.setEnergyCalibration(coeffs, POLYNOMIAL_NAMES, POLYNOMIAL_UNITS);
      } else if (mScale instanceof AltEnergyScaleFunction) {
         final AltEnergyScaleFunction psf = (AltEnergyScaleFunction) mScale;
         final Parameter[] ps = psf.getCoefficients();
         final UncertainValue2[] calib = new UncertainValue2[ps.length];
         for (int i = 0; i < ps.length; ++i)
            calib[i] = ps[i].getUncertainValue(pm);
         res.setEnergyCalibration(calib, SQRT_NAMES, SQRT_UNITS);
      }
      for (final Lineset ls : mMultiLines)
         for (final Lineset.LineData ld : ls.mLines) {
            final UncertainValue2 gaussianW = mResolution.computeU(ld.mEnergy, pm);
            res.addTransition(ld.mTransition, ld.computeU(ld.mEnergy, pm), ld.integrate(pm), gaussianW, mScale.inverse(ld.mEnergy, pm));
         }
      res.setBremsstrahlungModel(mBremsstrahlung, new UncertainValue2(mBremsstrahlung.mA), new UncertainValue2(mBremsstrahlung.mB));
      res.setBremsstrahlungSpectrum(getBremsstrahlungSpectrum());
      if (mListener != null)
         mListener.actionPerformed(new ActionEvent(this, 100, null));
      mResults.add(res);
      return res;
   }

   public List<SpectrumFitResult> getResultList() {
      return Collections.unmodifiableList(mResults);
   }

   public SpectrumFitResult getLastResult() {
      return mResults.get(mResults.size() - 1);
   }

   public void setEnergyScale(InvertableFunction e2c) {
      mScale = e2c;
   }

   public InvertableFunction getEnergyScale() {
      return mScale;
   }

   public void setResolution(Function res) {
      mResolution = res;
   }

   public Function getResolution() {
      return mResolution;
   }

   public void setMultiLineset(MultiLineset mls) {
      mMultiLines = mls;
   }

   public MultiLineset getMultiLineset() {
      return mMultiLines;
   }

   public ISpectrumData getRoughFit() {
      return SpectrumUtils.copy(mRoughFit);
   }

   public ISpectrumData getBestFit() throws EPQException, UtilException {
      if (mBestFit == null)
         compute();
      return SpectrumUtils.copy(mBestFit);
   }

   public void fixResolution(Map<Parameter, UncertainValue2> param) {
      for (final Parameter p : mResolution.getParameters(true)) {
         p.setIsFit(false);
         if (param.containsKey(p))
            p.setDefaultValue(param.get(p).doubleValue());
      }
   }

   public void fixEnergyScale(Map<Parameter, UncertainValue2> param) {
      for (final Parameter p : mScale.getParameters(true)) {
         p.setIsFit(false);
         if (param.containsKey(p))
            p.setDefaultValue(param.get(p).doubleValue());
      }
   }

   public Map<Parameter, UncertainValue2> getBestFitParameters() throws EPQException, UtilException {
      if (mBestFitParam == null)
         compute();
      return mBestFitParam;
   }

   public UncertainValue2 getChannel(double ev) throws EPQException, UtilException {
      return mScale.inverse(ev, getBestFitParameters());
   }

   public void setActionListener(ActionListener al) {
      mListener = al;
   }

   public Set<Element> getElements() {
      return mComposition.getElementSet();
   }

   public ISpectrumData getElementSpectrum(Element elm) {
      // Compute the best fit spectrum
      final Map<Parameter, Double> param = new HashMap<Parameter, Double>();
      for (final Map.Entry<Parameter, UncertainValue2> me : mBestFitParam.entrySet())
         param.put(me.getKey(), me.getValue().doubleValue());
      final EditableSpectrum fit = new EditableSpectrum(mSpectrum);
      Arrays.fill(fit.getCounts(), 0.0);
      for (int ch = 0; ch < fit.getChannelCount(); ++ch)
         for (final Lineset ls : mMultiLines)
            if (ls.mLines.get(0).mTransition.getElement().equals(elm))
               fit.setCounts(ch, fit.getCounts(ch) + ls.compute(ch, param));
      fit.getProperties().clear(new PropertyId[]{SpectrumProperties.StandardComposition});
      fit.getProperties().setElements(Collections.singleton(elm));
      SpectrumUtils.rename(fit, "BestFit[" + elm + "," + fit + "]");
      return fit;
   }

   public ISpectrumData getSpectrum() {
      return mSpectrum;
   }

   /**
    * Perform a QC analysis on the specified spectrum and return the results as
    * a TreeMap&lt;String,UncertainValue&gt;. The ISpectrum
    * 
    * @param det
    *           EDSDetector The detector on which the spectrum was collected
    * @param std
    *           Composition The approximate composition of the material from
    *           which the spectrum was collected.
    * @param spec
    *           ISpectrumData The spectrum
    * @param mode
    *           QCNormalizeMode
    * @param al
    *           {@link ActionListener} (May be null)
    * @return TreeMap&lt;String,UncertainValue&gt;
    * @throws EPQException
    * @throws UtilException
    */
   public static TreeMap<String, UncertainValue2> performQC(EDSDetector det, Composition std, ISpectrumData spec, QCNormalizeMode mode,
         ActionListener al) throws EPQException, UtilException {
      final TreeMap<String, UncertainValue2> res = new TreeMap<String, UncertainValue2>();
      final SpectrumFitter8 sf8 = new SpectrumFitter8(det, std, spec);
      final double[] coeff = new double[]{det.getZeroOffset(), det.getChannelWidth()};
      final boolean[] fitP = new boolean[]{true, true};
      sf8.setEnergyScale(new EnergyScaleFunction(coeff, fitP));
      sf8.setResolution(new FanoNoiseWidth(2, 0.12, 6.0));
      sf8.setMultiLineset(sf8.buildWeighted(sf8.getROIS()));
      if (al != null)
         sf8.setActionListener(al);
      final double i = SpectrumUtils.getAverageFaradayCurrent(spec.getProperties(), Double.NaN);
      final double totalCounts = SpectrumUtils.totalCounts(spec, true);
      final double lt = spec.getProperties().getNumericProperty(SpectrumProperties.LiveTime);
      double dose = Double.NaN;
      if (Double.isNaN(i)) {
         if (mode == QCNormalizeMode.CURRENT)
            throw new EPQException("The QC spectrum does not define the required probe current property.");
      } else {
         dose = i * lt;
         res.put("Dose", new UncertainValue2(dose));
      }
      final SpectrumFitResult sfr = sf8.compute();
      res.put("Total counts", new UncertainValue2(totalCounts));
      res.put("FWHM @ Mn Ka", sfr.getFWHMatMnKa());
      res.put("Fano Factor", sfr.getFanoFactor());
      res.put("Noise", sfr.getNoise());
      res.put("Zero offset", sfr.getZeroOffset());
      res.put("Channel width", sfr.getChannelWidth());
      res.put("Duane-Hunt", new UncertainValue2(FromSI.keV(DuaneHuntLimit.DefaultDuaneHunt.compute(spec))));
      final double norm = (mode == QCNormalizeMode.CURRENT ? dose : totalCounts);
      assert !Double.isNaN(norm);
      {
         double bremCx = totalCounts;
         for (final Element elm : sf8.getElements())
            bremCx -= SpectrumUtils.totalCounts(sf8.getElementSpectrum(elm), false);;
         res.put("Brem Counts", new UncertainValue2(bremCx / norm));
      }
      for (final Element elm : std.getElementSet())
         for (final RegionOfInterestSet.RegionOfInterest roi : sf8.getRegionOfInterests(elm)) {
            final String name = roi.toString();
            res.put(name.substring(0, name.indexOf("[") - 1),
                  UncertainValue2.divide(sfr.getIntegratedIntensity(roi.getXRayTransitionSet(elm)), norm));
         }
      return res;
   }

   /**
    * Returns a {@link RegionOfInterestSet} containing all the RegionOfInterest
    * objects associate with the specified Element in the fit.
    * 
    * @param elm
    * @return RegionOfInterestSet
    */
   public RegionOfInterestSet getRegionOfInterests(Element elm) {
      final double MIN_I = 0.001;
      final DetectorLineshapeModel dlm = mDetector.getDetectorLineshapeModel();
      final RegionOfInterestSet rois = new RegionOfInterestSet(dlm, MIN_I, ToSI.eV(0.5 * dlm.getFWHMatMnKa()), ToSI.eV(0.25 * dlm.getFWHMatMnKa()));
      for (final RegionOfInterest roi : getROIS())
         for (final XRayTransition xrt : roi.getXRayTransitionSet(elm))
            rois.add(xrt);
      return rois;
   }

   /**
    * Returns true if the fitter is configured to fit incomplete charge
    * collection.
    * 
    * @return Returns the fitIncompleteCharge.
    */
   public boolean fitIncompleteCharge() {
      return mFitIncompleteCharge;
   }

   /**
    * Set to true to fit incomplete charge collection.
    * 
    * @param fitIncompleteCharge
    *           The value to which to set fitIncompleteCharge.
    */
   public void setFitIncompleteCharge(boolean fitIncompleteCharge) {
      if (mFitIncompleteCharge != fitIncompleteCharge)
         mFitIncompleteCharge = fitIncompleteCharge;
   }
}
