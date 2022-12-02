/**
 * gov.nist.microanalysis.EPQLibrary.MicrocalSpectrumFitter Created by: nritchie
 * Date: May 29, 2009
 */
package gov.nist.microanalysis.EPQLibrary;

import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.Detector.DetectorLineshapeModel;
import gov.nist.microanalysis.EPQLibrary.Detector.MicrocalCalibration.BasicMicrocalLineshape;
import gov.nist.microanalysis.Utility.Constraint;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.LevenbergMarquardt2;
import gov.nist.microanalysis.Utility.LinearLeastSquares;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.ProgressEvent;
import gov.nist.microanalysis.Utility.UncertainValue2;

import Jama.Matrix;

/**
 * <p>
 * This is a specialized spectrum fitting routine for fitting individual peaks
 * within a microcalorimeter spectrum. It assumes that each peak is due to a
 * series of lines from a single element. The fit function is a Gaussian for
 * each transition with a global width and a constant background.
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
public class MicrocalSpectrumFitter {

   private static final double SQRT_2PI = Math.sqrt(2.0 * Math.PI);

   private static class PeakData implements Comparable<PeakData> {
      /**
       * Estimated position of the peak in channels
       */
      private final double mEstChannel;
      /**
       * A ROI describing the area and transitions represented by this peak.
       */
      private final RegionOfInterestSet.RegionOfInterest mROI;
      /**
       * Use this peak in the energy fit?
       */
      private final boolean mFitEnergy;
      /**
       * Width of peak (in channels)
       */
      private final double mWidth;

      private PeakData(double channel, RegionOfInterestSet.RegionOfInterest roi, boolean eFit, double width) {
         mEstChannel = channel;
         mROI = roi;
         mFitEnergy = eFit;
         mWidth = width;
      }

      @Override
      public int compareTo(PeakData o) {
         return mROI.compareTo(o.mROI);
      }

      @Override
      public int hashCode() {
         return mROI.hashCode();
      }
   }

   /**
    * The raw spectral data
    */
   private final ISpectrumData mSpectrum;
   private TreeMap<RegionOfInterestSet.RegionOfInterest, MicrocalLineFitter> mFitResults;
   private final TreeSet<PeakData> mPeaks = new TreeSet<PeakData>();
   private final ArrayList<Double> mPeakPositions = new ArrayList<Double>();
   private static TransitionEnergy mEnergy = TransitionEnergy.Default;
   private static final double PEAK_THRESHOLD = 20.0;

   /**
    * Result from the initial fit of the primary transition positions in
    * channels to the tabulated energy.
    */
   private UncertainValue2[] mEnergyFit0;
   /**
    * Result from the final fit of the weightiest transitions in channels to the
    * tabulated energies.
    */
   private UncertainValue2[] mEnergyFit1;

   public double[] performEnergyFit0() throws EPQException {
      if (mEnergyFit0 == null) {
         int count = 0;
         for (final PeakData pd : mPeaks)
            if (pd.mFitEnergy)
               ++count;
         if (count < 3)
            throw new EPQException("Too few peaks (" + Integer.toString(count) + "<3) identified for the energy fit.");
         final double[] x = new double[count];
         final double[] y = new double[count];
         final double[] sig = new double[count];
         {
            int i = 0;
            for (final PeakData pd : mPeaks)
               if (pd.mFitEnergy) {
                  x[i] = pd.mEstChannel;
                  y[i] = FromSI.eV(mEnergy.compute(pd.mROI.getXRayTransitionSet(pd.mROI.getElementSet().first()).getWeighiestTransition()));
                  sig[i] = 1.0;
                  ++i;
               }
         }
         final LinearLeastSquares llsq = new LinearLeastSquares(x, y, sig) {
            @Override
            protected int fitFunctionCount() {
               return 3;
            }

            @Override
            protected void fitFunction(double xi, double[] afunc) {
               double v = 1.0;
               for (int i = 0; i < afunc.length; ++i) {
                  afunc[i] = v;
                  v *= xi;
               }
            }
         };
         mEnergyFit0 = llsq.getResults();
      }
      final double[] res = new double[mEnergyFit0.length];
      for (int i = 0; i < mEnergyFit0.length; ++i)
         res[i] = mEnergyFit0[i].doubleValue();
      return res;
   }

   public void setInitialEneryCalibration(double[] poly) {
      mEnergyFit0 = new UncertainValue2[poly.length];
      for (int i = 0; i < poly.length; ++i)
         mEnergyFit0[i] = new UncertainValue2(poly[i], "E[" + i + "]", poly[i] * 0.01);
   }

   public double[] performEnergyFit1() throws EPQException {
      if (mEnergyFit1 == null) {
         int count = 0;
         for (final PeakData pd : mPeaks)
            if (pd.mFitEnergy)
               ++count;
         if (count < 3)
            throw new EPQException("Too few peaks (" + Integer.toString(count) + "<3) identified for the energy fit.");
         final double[] x = new double[count];
         final double[] y = new double[count];
         final double[] sig = new double[count];
         {
            int i = 0;
            for (final PeakData pd : mPeaks) {
               final RegionOfInterestSet.RegionOfInterest roi = pd.mROI;
               final MicrocalLineFitter mlf = mFitResults.get(roi);
               final XRayTransition weightiest = mlf.mTransitions.getWeighiestTransition();
               final UncertainValue2 channelPos = mlf.getChannelPosition(weightiest);
               if (pd.mFitEnergy) {
                  y[i] = FromSI.eV(mEnergy.compute(weightiest));
                  x[i] = channelPos.doubleValue();
                  sig[i] = Math.max(channelPos.uncertainty(), 1.0);
                  ++i;
               }
            }
         }
         final LinearLeastSquares llsq = new LinearLeastSquares(x, y, sig) {
            @Override
            protected int fitFunctionCount() {
               return 3;
            }

            @Override
            protected void fitFunction(double xi, double[] afunc) {
               double v = 1.0;
               for (int i = 0; i < afunc.length; ++i) {
                  afunc[i] = v;
                  v *= xi;
               }
            }
         };
         mEnergyFit1 = llsq.getResults();
      }
      final double[] res = new double[mEnergyFit1.length];
      for (int i = 0; i < mEnergyFit1.length; ++i)
         res[i] = mEnergyFit1[i].doubleValue();
      return res;
   }

   /**
    * Returns the channel index at which the specified energy can be found
    * according to the first, estimated energy fit.
    * 
    * @param e
    * @return Channel index as a double
    */
   private double channelForEnergy(double e) {
      final double[] res = Math2.quadraticSolver(mEnergyFit0[2].doubleValue(), mEnergyFit0[1].doubleValue(), mEnergyFit0[0].doubleValue() - e);
      return res != null ? (res[0] == Math2.bound(res[0], 0.0, 8096) ? res[0] : res[1]) : 0;
   }

   /**
    * Returns the energy at which the specified channel is located based on the
    * second more precise energy fit.
    * 
    * @param ch
    * @return The energy as an UncertainValue
    */
   private UncertainValue2 energyForChannel(UncertainValue2 ch) {
      UncertainValue2 res = mEnergyFit1[mEnergyFit1.length - 1];
      for (int i = mEnergyFit1.length - 2; i >= 0; --i)
         res = UncertainValue2.add(mEnergyFit1[i], UncertainValue2.multiply(ch, res));
      return res;
   }

   private class MicrocalLineFitter {

      private static final int BACKGROUND = 0;
      private static final int WIDTH = 1;
      private static final int OFFSET = 2;
      private static final int END_OF_FIXED = 3;

      private final RegionOfInterestSet.RegionOfInterest mROI;
      private final double mWidth;
      private final int mMinCh, mMaxCh;
      private final Constraint[] mConstraints;
      private final XRayTransitionSet mTransitions;
      private final double[] mOffsets;
      private transient LevenbergMarquardt2.FitResult mFitResult;

      private MicrocalLineFitter(RegionOfInterestSet.RegionOfInterest roi, double channel, double width) throws EPQException {
         mROI = roi;
         mWidth = width;
         mMinCh = (int) channelForEnergy(FromSI.eV(mROI.lowEnergy()));
         assert mMinCh >= 0 : mMinCh;
         assert mMinCh < mSpectrum.getChannelCount() : mMinCh;
         mMaxCh = (int) Math.ceil(channelForEnergy(FromSI.eV(mROI.highEnergy())));
         assert mMaxCh > mMinCh : mMaxCh;
         assert mMaxCh < mSpectrum.getChannelCount() : mMaxCh;
         final Element elm = mROI.getElementSet().first();
         mTransitions = new XRayTransitionSet();
         for (final XRayTransition xrt0 : mROI.getXRayTransitionSet(elm)) {
            boolean add = true;
            XRayTransition remove = null;
            for (final XRayTransition xrt1 : mTransitions)
               if (xrt0.getEnergy() == xrt1.getEnergy()) {
                  add = (xrt0.getWeight(XRayTransition.NormalizeDestination) > xrt1.getWeight(XRayTransition.NormalizeDestination));
                  if (add)
                     remove = xrt1;
               }
            if (remove != null) {
               mTransitions.remove(remove);
               assert add;
            }
            if (add)
               mTransitions.add(xrt0);
         }
         final double[] peakInt = SpectrumUtils.backgroundCorrectedIntegral(mSpectrum, FromSI.eV(mROI.lowEnergy()), FromSI.eV(mROI.highEnergy()));
         mOffsets = new double[mTransitions.size()];
         mConstraints = new Constraint[END_OF_FIXED + mTransitions.size()];
         mConstraints[BACKGROUND] = new Constraint.Positive(
               (0.05 * mSpectrum.getChannelWidth() * peakInt[2]) / (FromSI.eV(mROI.highEnergy() - mROI.lowEnergy())));
         mConstraints[WIDTH] = new Constraint.Bounded(mWidth, 0.99 * mWidth);
         mConstraints[OFFSET] = new Constraint.Bounded(0.0, 40.0);
         int i = 0;
         for (final XRayTransition xrt : mTransitions) {
            mOffsets[i] = channelForEnergy(FromSI.eV(mEnergy.compute(xrt)));
            mConstraints[i + END_OF_FIXED] = new Constraint.Positive(peakInt[2] * Math.min(xrt.getWeight(XRayTransition.NormalizeDestination), 0.01));
            ++i;
         }
      }

      public LevenbergMarquardt2.FitResult compute() throws EPQException {

         final LevenbergMarquardt2.FitFunction ff = new LevenbergMarquardt2.FitFunction() {

            /**
             * Computes the partial derivative matrix (the Jacobian) associated
             * with the fit function and the fit parameters.
             * 
             * @param params
             *           A m x 1 Matrix containing the fit function parameters
             * @return Matrix An n x m Matrix containing the Jacobian (partials)
             */
            @Override
            public Matrix partials(Matrix params) {
               assert mMaxCh > mMinCh;
               final Matrix res = new Matrix((mMaxCh - mMinCh) + 1, params.getRowDimension());
               assert params.getRowDimension() == res.getColumnDimension();
               for (int i = 0; i < res.getRowDimension(); ++i)
                  for (int j = 0; j < res.getColumnDimension(); ++j)
                     res.set(i, j, fitPartial(mMinCh + i, params, j));
               return res;
            }

            /**
             * Computes the fit function as a function of the fit parameters.
             * 
             * @param params
             *           A m x 1 Matrix containing the fit function parameters
             * @return Matrix
             */
            @Override
            public Matrix compute(Matrix params) {
               final Matrix res = new Matrix((mMaxCh - mMinCh) + 1, 1);
               for (int ch = mMinCh; ch < mMaxCh; ++ch)
                  res.set(ch - mMinCh, 0, fitFunction(ch, params));
               return res;
            }
         };
         if (mFitResult == null) {
            final LevenbergMarquardt2 fitter = new LevenbergMarquardt2();
            final int chDim = (1 + mMaxCh) - mMinCh;
            final Matrix yy = new Matrix(chDim, 1);
            final Matrix dy = new Matrix(chDim, 1);
            for (int i = 0; i < chDim; ++i) {
               final double cc = mSpectrum.getCounts(i + mMinCh);
               yy.set(i, 0, cc);
               dy.set(i, 0, Math.sqrt(Math.max(cc, 1.0)));
            }
            final Matrix p0 = new Matrix(mConstraints.length, 1);
            for (int i = 0; i < mConstraints.length; ++i)
               p0.set(i, 0, 0.0);
            mFitResult = fitter.compute(ff, yy, dy, p0);
         }
         return mFitResult;
      }

      public UncertainValue2 getBackground() {
         return mConstraints[BACKGROUND].getResult(mFitResult.getBestParametersU()[BACKGROUND]);
      }

      public UncertainValue2 getOffset() {
         return mConstraints[OFFSET].getResult(mFitResult.getBestParametersU()[OFFSET]);
      }

      public UncertainValue2 getWidth() {
         return mConstraints[WIDTH].getResult(mFitResult.getBestParametersU()[WIDTH]);
      }

      public UncertainValue2 getAmplitude(XRayTransition xrt) {
         int i = END_OF_FIXED;
         for (final XRayTransition xrt0 : mTransitions) {
            if (xrt.equals(xrt0))
               return mConstraints[i].getResult(mFitResult.getBestParametersU()[i]);
            ++i;
         }
         return UncertainValue2.ZERO;
      }

      /**
       * Returns the position of the specified transition in terms of the
       * channel at which the best fit location for this
       * 
       * @param xrt
       * @return UncertainValue
       */
      public UncertainValue2 getChannelPosition(XRayTransition xrt) {
         final UncertainValue2 offset = getOffset();
         int i = 0;
         assert (mTransitions.size() + END_OF_FIXED) == mConstraints.length;
         for (final XRayTransition xrt0 : mTransitions) {
            if (xrt.equals(xrt0))
               return UncertainValue2.add(mOffsets[i], offset);
            ++i;
         }
         assert false;
         return null;
      }

      public XRayTransitionSet getTransitions() {
         return mTransitions;
      }

      public double fitFunction(double channel, Matrix params) {
         final double bkgd = mConstraints[BACKGROUND].realToConstrained(params.get(BACKGROUND, 0));
         final double width = mConstraints[WIDTH].realToConstrained(params.get(WIDTH, 0));
         final double offset = mConstraints[OFFSET].realToConstrained(params.get(OFFSET, 0));
         double res = 0.0;
         final double k = 1.0 / (width * SQRT_2PI);
         for (int i = END_OF_FIXED; i < params.getRowDimension(); ++i) {
            final double height = mConstraints[i].realToConstrained(params.get(i, 0));
            final double x = (offset + mOffsets[i - END_OF_FIXED]) - channel;
            res += height * Math.exp(-0.5 * Math2.sqr(x / width));
         }
         return (k * res) + bkgd;
      }

      public double fitPartial(double channel, Matrix params, int idx) {
         assert Math2.bound(channel, mMinCh, mMaxCh) == channel : channel + " in [" + mMinCh + "," + mMaxCh + "]";
         final double width = mConstraints[WIDTH].realToConstrained(params.get(WIDTH, 0));
         final double offset = mConstraints[OFFSET].realToConstrained(params.get(OFFSET, 0));
         double res = Double.NaN;
         switch (idx) {
            case BACKGROUND :
               // Background
               res = 1.0;
               break;
            case WIDTH : {
               // Width
               res = 0.0;
               final double w2 = width * width;
               assert params.getRowDimension() == mConstraints.length;
               assert params.getColumnDimension() == 1;
               for (int i = END_OF_FIXED; i < params.getRowDimension(); ++i) {
                  final double height = mConstraints[i].realToConstrained(params.get(i, 0));
                  final double x2 = Math2.sqr((offset + mOffsets[i - END_OF_FIXED]) - channel);
                  res += height * Math.exp((-0.5 * x2) / w2) * (x2 - w2);
               }
               res /= (w2 * w2 * SQRT_2PI);
               break;
            }
            case OFFSET : {
               // offset
               res = 0.0;
               assert params.getRowDimension() == mConstraints.length;
               for (int i = END_OF_FIXED; i < params.getRowDimension(); ++i) {
                  final double height = mConstraints[i].realToConstrained(params.get(i, 0));
                  final double x = (offset + mOffsets[i - END_OF_FIXED]) - channel;
                  res -= height * x * Math.exp(-0.5 * Math2.sqr(x / width));
               }
               res /= (Math.pow(width, 3.0) * SQRT_2PI);
               break;
            }
            default : {
               final double x = (offset + mOffsets[idx - END_OF_FIXED]) - channel;
               res = Math.exp(-0.5 * Math2.sqr(x / width)) / (width * SQRT_2PI);
            }
         }
         assert !Double.isNaN(res);
         return res * mConstraints[idx].derivative(params.get(idx, 0));

      }
   }

   public MicrocalSpectrumFitter(ISpectrumData spec) {
      mSpectrum = spec;
      spec.getProperties().setNumericProperty(SpectrumProperties.Resolution, 8.0);
      spec.getProperties().setNumericProperty(SpectrumProperties.ResolutionLine, SpectrumUtils.E_MnKa);
      final double[] peaks = PeakROISearch.GaussianSearch.compute(spec);
      for (int i = 0; i < peaks.length; ++i)
         if (peaks[i] > PEAK_THRESHOLD) {
            int maxI = i;
            for (++i; (i < peaks.length) && (peaks[i] > (0.5 * PEAK_THRESHOLD)); ++i)
               if (mSpectrum.getCounts(i) > mSpectrum.getCounts(maxI))
                  maxI = i;
            mPeakPositions.add(Double.valueOf(maxI));
         }
   }

   public ISpectrumData getSpectrum() {
      return mSpectrum;
   }

   public List<Double> getPeakPositions() {
      return Collections.unmodifiableList(mPeakPositions);
   }

   /**
    * Used to identify specific lines the spectrum associated with the
    * MicrocalSpectrumFitter. The line at the specified <code>channel</code> is
    * associated with the specified
    * <code>RegionOfInterestSet.RegionOfInterest</code> which identifies a set
    * of <code>XRayTransition</code> objects. The <code>forEnergyFit</code>
    * argument determines whether the line should be used to perform the energy
    * calibration. The <code>width</code> argument is used to provide an initi
    * Identify the line at the specified channel
    * 
    * @param channel
    * @param roi
    * @param forEnergyFit
    * @param width
    */
   public void addLine(double channel, RegionOfInterestSet.RegionOfInterest roi, boolean forEnergyFit, double width) {
      mPeaks.add(new PeakData(channel, roi, forEnergyFit, width));
      mEnergyFit0 = null;
   }

   public void removeLine(RegionOfInterestSet.RegionOfInterest roi) {
      for (final Iterator<PeakData> i = mPeaks.iterator(); i.hasNext();) {
         final PeakData pd = i.next();
         if (pd.mROI.equals(roi)) {
            i.remove();
            mEnergyFit0 = null;
         }

      }
   }

   public void clearLines() {
      mPeaks.clear();
      mEnergyFit0 = null;
   }

   public void compute() throws EPQException {
      performEnergyFit0();
      mFitResults = new TreeMap<RegionOfInterestSet.RegionOfInterest, MicrocalLineFitter>();
      int i = 0;
      for (final PeakData peak : mPeaks) {
         final MicrocalLineFitter mlf = new MicrocalLineFitter(peak.mROI, peak.mEstChannel, peak.mWidth);
         mFitResults.put(peak.mROI, mlf);
         mlf.compute();
         mlf.getOffset();
         setProgress((int) Math.round((++i * 100.0) / mPeaks.size()));
      }
      performEnergyFit1();
   }

   public ISpectrumData toFitSpectrum() {
      final EditableSpectrum es = new EditableSpectrum(mSpectrum.getChannelCount(), mSpectrum.getChannelWidth(), mSpectrum.getZeroOffset());
      for (final MicrocalLineFitter mlf : mFitResults.values()) {
         final double w = mlf.getWidth().doubleValue();
         final double b = mlf.getBackground().doubleValue();
         final double k = 1.0 / (w * SQRT_2PI);
         for (int ch = mlf.mMinCh; ch < mlf.mMaxCh; ++ch) {
            double ampl = 0.0;
            for (final XRayTransition xrt : mlf.mTransitions) {
               final double a = mlf.getAmplitude(xrt).doubleValue();
               final double center = mlf.getChannelPosition(xrt).doubleValue();
               ampl += a * Math.exp(-0.5 * Math2.sqr((ch - center) / w));
            }
            es.setCounts(ch, es.getCounts(ch) + (k * ampl) + b);
         }
      }
      SpectrumUtils.rename(es, "Fit[" + mSpectrum.toString() + "]");
      return SpectrumUtils.copy(es);
   }

   public ISpectrumData toResidualSpectrum() {
      final EditableSpectrum es = new EditableSpectrum(mSpectrum);
      for (final MicrocalLineFitter mlf : mFitResults.values()) {
         final double w = mlf.getWidth().doubleValue();
         final double k = 1.0 / (w * SQRT_2PI);
         for (int ch = mlf.mMinCh; ch < mlf.mMaxCh; ++ch) {
            double ampl = 0.0;
            for (final XRayTransition xrt : mlf.mTransitions) {
               final double a = mlf.getAmplitude(xrt).doubleValue();
               final double center = mlf.getChannelPosition(xrt).doubleValue();
               ampl += a * Math.exp(-0.5 * Math2.sqr((ch - center) / w));
            }
            es.setCounts(ch, es.getCounts(ch) - (k * ampl));
         }
      }
      SpectrumUtils.rename(es, "Residual[" + mSpectrum.toString() + "]");
      return SpectrumUtils.copy(es);
   }

   public ISpectrumData linearizeSpectrum(ISpectrumData spec) throws EPQException {
      return SpectrumUtils.linearizeSpectrum2(spec, performEnergyFit1(), spec.getChannelWidth());
   }

   public ISpectrumData toEstLinearizedSpectrum() throws EPQException {
      final ISpectrumData res = SpectrumUtils.linearizeSpectrum2(mSpectrum, performEnergyFit0(), mSpectrum.getChannelWidth());
      SpectrumUtils.rename(res, "EstLinearized[" + mSpectrum.toString() + "]");
      return res;
   }

   public String toHTML() throws EPQException {
      final NumberFormat nf1 = new HalfUpFormat("0.0");
      final NumberFormat nf2 = new HalfUpFormat("0.00");
      final NumberFormat nf4 = new HalfUpFormat("0.00000");
      final StringBuffer sb = new StringBuffer(4096);
      sb.append("<H2>Microcalorimeter Spectrum Fit</H2>\n");
      sb.append("<H3>Initialization</H3>\n");
      sb.append("<table>");
      sb.append("<tr><th>Spectrum</th><th>" + mSpectrum.toString() + "</th></tr>");
      sb.append("</table>");
      if (mPeaks != null) {
         sb.append("<table>");
         sb.append("<tr><th>Channel</th><th>ROI</th><th>Fit</th><th>Width</th>");
         for (final PeakData peak : mPeaks) {
            sb.append("<tr><td>" + nf1.format(peak.mEstChannel) + "</td>");
            sb.append("<td>" + peak.mROI.toString() + "</td>");
            sb.append("<td>" + Boolean.toString(peak.mFitEnergy) + "</td>");
            sb.append("<td>" + nf1.format(peak.mWidth) + "</td></tr>");
         }
         sb.append("</table>");
      }
      if (mEnergyFit0 != null) {
         sb.append("<H3>Initial Energy Fit</H3>\n");
         sb.append("<table>");
         sb.append("<tr><th>offset</th><th>gain</th><th>quadratic</th></tr>");
         sb.append("<tr><td>eV</td><td>eV/ch</td><td>eV/ch<sup>2</sup></td></tr>");
         sb.append("<tr><td>" + mEnergyFit0[0].format(nf1) + "</td>");
         sb.append("<td>" + mEnergyFit0[1].format(nf4) + "</td>");
         sb.append("<td>" + UncertainValue2.multiply(1.0e6, mEnergyFit0[2]).format(nf1) + " &#215; 10<sup>-6</sup></td></tr>");
         sb.append("</table>");
      }
      if (mFitResults != null) {
         sb.append("<H3>Peak Fit Results</H3>");
         sb.append("<table>");
         sb.append("<tr><th>ROI</th>");
         sb.append("<th>Transition</th>");
         sb.append("<th>Amplitude</th>");
         sb.append("<th>Channel</th>");
         sb.append("<th>Width</th>");
         sb.append("<th>Background</th>");
         sb.append("<th>Energy(Fit)</th>");
         sb.append("<th>Energy(Tabulated)</th>");
         sb.append("</tr>");
         for (final Map.Entry<RegionOfInterestSet.RegionOfInterest, MicrocalLineFitter> me : mFitResults.entrySet()) {
            final RegionOfInterestSet.RegionOfInterest roi = me.getKey();
            final MicrocalLineFitter mlf = me.getValue();
            for (final XRayTransition xrt : mlf.getTransitions()) {
               final UncertainValue2 uv = mlf.getAmplitude(xrt);
               final UncertainValue2 pos = mlf.getChannelPosition(xrt);
               sb.append("<tr><td>" + roi.toString().substring(0, 20) + "</td>");
               sb.append("<td>" + xrt.toString() + "</td>");
               sb.append("<td>" + uv.format(nf1) + "</td>");
               sb.append("<td>" + pos.format(nf1) + "</td>");
               sb.append("<td>" + mlf.getWidth().format(nf2) + "</td>");
               sb.append("<td>" + mlf.getBackground().format(nf2) + "</td>");
               sb.append("<td>" + energyForChannel(pos).format(nf2) + "</td>");
               sb.append("<td>" + nf1.format(FromSI.eV(mEnergy.compute(xrt))) + "</td>");
               sb.append("</tr>");
            }
         }
         sb.append("</table>");
      }
      if (mEnergyFit1 != null) {
         sb.append("<H3>Secondary Energy Fit</H3>\n");
         sb.append("<table>");
         sb.append("<tr><th>offset</th><th>gain</th><th>quadratic</th></tr>");
         sb.append("<tr><td>eV</td><td>eV/ch</td><td>eV/ch<sup>2</sup></td></tr>");
         sb.append("<tr><td>" + mEnergyFit1[0].format(nf1) + "</td>");
         sb.append("<td>" + mEnergyFit1[1].format(nf4) + "</td>");
         sb.append("<td>" + UncertainValue2.multiply(1.0e6, mEnergyFit1[2]).format(nf2) + " &#215; 10<sup>-6</sup></td></tr>");
         sb.append("</table>");
      }
      return sb.toString();
   }

   public RegionOfInterestSet getElementROIS(Element elm, double width) {
      final double e0 = SpectrumUtils.getBeamEnergy(mSpectrum);
      final double w = width / mSpectrum.getChannelWidth();
      final DetectorLineshapeModel dlm = new BasicMicrocalLineshape(w);
      final RegionOfInterestSet res = new RegionOfInterestSet(dlm, 0.001, ToSI.eV(0.25 * w), ToSI.eV(0.5 * w));
      res.add(elm, e0, 0.0);
      return res;
   }

   private final ArrayList<ActionListener> mEvents = new ArrayList<ActionListener>();
   private int mEventIndex = 0;

   public void addProgressListener(ActionListener ae) {
      mEvents.add(ae);
   }

   public void removeProgressListener(ActionListener ae) {
      mEvents.remove(ae);
   }

   private void setProgress(int percent) {
      final ProgressEvent pe = new ProgressEvent(this, ++mEventIndex, percent);
      for (final ActionListener ae : mEvents)
         ae.actionPerformed(pe);
   }

}
