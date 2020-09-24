package gov.nist.microanalysis.EPQLibrary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.LitReference.Author;
import gov.nist.microanalysis.EPQLibrary.Detector.DetectorLineshapeModel;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.Utility.Interval;
import gov.nist.microanalysis.Utility.LinearLeastSquares;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * Analytical formulas for the Bremsstrahlung emission.
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
abstract public class BremsstrahlungAnalytic
   extends
   AlgorithmClass {

   protected MassAbsorptionCoefficient mMac;
   protected Composition mComposition;
   protected double mE0keV;
   protected double mTakeOffAngle;

   static private LitReference.JournalArticle small1987 = new LitReference.JournalArticle("Modeling of the bremsstrahlung radiation produced in pure-element targets by 10-40 keV electrons", LitReference.JApplPhys, "61 (2)", "p459-469", 1987, new Author[] {
      LitReference.JSmall,
      new LitReference.Author("Stefan", "Leigh", LitReference.NIST),
      LitReference.DNewbury,
      LitReference.RMyklebust
   });

   static private LitReference.JournalArticle reed1975 = new LitReference.JournalArticle("The Shape of the Continuous X-ray Spectrum and Background Corrections for Energy-Dispersive Electron Microprobe Analysis", LitReference.XRaySpec, "4", "p14-17", 1975, new Author[] {
      LitReference.SReed
   });

   static private LitReference.CrudeReference lifshin1974 = new LitReference.CrudeReference("E. Lifshin, Proc. Ninth Annual Conference on the Microbeam Analysis Society, Ottowa, Canada, paper 53 (1974)");

   /**
    * Initialize the algorithm.
    * 
    * @param comp Composition
    * @param e0 in Joules
    * @param toa in Radians
    */
   public void initialize(Composition comp, double e0, double toa) {
      mComposition = comp;
      mMac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
      mE0keV = FromSI.keV(e0);
      mTakeOffAngle = toa;
   }

   public static class NoBremsstrahlung
      extends
      BremsstrahlungAnalytic {

      /**
       * Constructs a NoBremsstrahlung object
       */
      protected NoBremsstrahlung() {
         super("No bremsstrahlung", LitReference.NullReference);
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.BremsstrahlungAnalytic#compute(double)
       */
      @Override
      public double compute(double eNu) {
         return 0.0;
      }
   };

   /**
    * Compute the emitted bremsstrahlung intensity at the specified photon
    * energy.
    * 
    * @param eNu In SI (Joules)
    * @return double The intensity in x-rays per nA*sec ?!?
    */
   abstract public double compute(double eNu);

   public static class Small1987
      extends
      BremsstrahlungAnalytic {

      private double mMeanZ;
      private double mM;
      private double mB;

      private final double mUnjustifiedFudgeFactor = 1.12;

      public Small1987() {
         super("Small - 1987", small1987);
      }

      @Override
      public void initialize(Composition comp, double e0, double toa) {
         super.initialize(comp, e0, toa);
         mM = (0.00599 * mE0keV) + 1.05;
         mB = (-0.0322 * mE0keV) + 5.80;
         mMeanZ = mComposition.meanAtomicNumber();
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.BremsstrahlungAnalytic#compute(double)
       */
      @Override
      public double compute(double eNu) {
         final double eNukeV = FromSI.keV(eNu);
         if((eNukeV > 0.0) && (eNukeV < mE0keV)) {
            final double mac = MassAbsorptionCoefficient.toCmSqrPerGram(mMac.compute(mComposition, eNu));
            final double f_p = Math2.sqr(Math.sin(mTakeOffAngle)
                  / (1.0 + (1.2e-6 * (Math.pow(mE0keV, 1.65) - Math.pow(eNukeV, 1.65)) * mac)));
            final double w = 1.15 - (0.150 * f_p);
            final double rc;
            {
               final double x = eNukeV / mE0keV;
               final double aa = 1.0e-4 * (1.0 - Math.exp((x * ((0.361 * x) + 0.288)) - 0.619));
               final double bb = 1.0e-2 * (1.0 - Math.exp((x * ((0.153 * x) + 2.04)) - 2.17));
               final double cc = x < 0.7 ? 1.003 + (0.0407 * x) : 1.017;
               rc = (mMeanZ * ((aa * mMeanZ) - bb)) + cc;
            }
            final double gen = Math.exp((mM * Math.log(mMeanZ * ((mE0keV / eNukeV) - 1.0))) + mB);
            return mUnjustifiedFudgeFactor * gen * f_p * rc * w;
         } else
            return 0.0;
      }
   }

   public static class Small1987alt
      extends
      BremsstrahlungAnalytic {

      private double mM;
      private double mB;

      public Small1987alt() {
         super("Small - 1987 (alt)", small1987);
      }

      @Override
      public void initialize(Composition comp, double e0, double toa) {
         super.initialize(comp, e0, toa);
         mM = (0.00599 * mE0keV) + 1.05;
         mB = (-0.0322 * mE0keV) + 5.80;
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.BremsstrahlungAnalytic#compute(double)
       */
      @Override
      public double compute(double eNu) {
         final double eNukeV = FromSI.keV(eNu);
         if((eNukeV > 0.0) && (eNukeV < mE0keV)) {
            final double mac = MassAbsorptionCoefficient.toCmSqrPerGram(mMac.compute(mComposition, eNu));
            final double f_p = Math2.sqr(Math.sin(mTakeOffAngle)
                  / (1.0 + (1.2e-6 * (Math.pow(mE0keV, 1.65) - Math.pow(eNukeV, 1.65)) * mac)));
            double gen = 0.0;
            final double x = eNukeV / mE0keV;
            final double aa = 1.0e-4 * (1.0 - Math.exp((x * ((0.361 * x) + 0.288)) - 0.619));
            final double bb = 1.0e-2 * (1.0 - Math.exp((x * ((0.153 * x) + 2.04)) - 2.17));
            final double cc = x < 0.7 ? 1.003 + (0.0407 * x) : 1.017;
            for(final Element elm : mComposition.getElementSet()) {
               final double z = elm.getAtomicNumber();
               final double rc = (z * ((aa * z) - bb)) + cc;
               final double w = 1.15 - (0.150 * f_p);
               gen += Math.exp((mM * Math.log(z * ((mE0keV / eNukeV) - 1.0))) + mB) * rc * w
                     * mComposition.weightFraction(elm, true);
            }

            return gen * f_p;
         } else
            return 0.0;
      }
   }

   /**
    * <p>
    * Implements the background correction from Reed 1975 as described in
    * Microsc. Microanal <b>12</b>, 406-415, 2006
    * </p>
    * <p>
    * Tested once - seems crudely ok but less good than DTSA.
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
   public static class Reed1975
      extends
      BremsstrahlungAnalytic {

      private double mH;

      public Reed1975() {
         super("Reed - 1975", reed1975);
      }

      /**
       * Computes chi - the take-off compensated absorption coefficient
       * 
       * @param e In SI
       * @param comp
       * @param takeOffAngle
       * @return double
       * @throws EPQException
       */
      protected double computeChi(double e, Composition comp, double takeOffAngle)
            throws EPQException {
         final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
         return mac.compute(comp, e) / Math.sin(takeOffAngle);
      }

      /**
       * Philibert's absorption correction algorithm
       * 
       * @param e In SI
       * @return double
       * @throws EPQException
       */
      private double philibert(double e)
            throws EPQException {
         final double eNukeV = FromSI.keV(e);
         final double chi = MassAbsorptionCoefficient.toCmSqrPerGram(computeChi(e, mComposition, mTakeOffAngle));
         final double sigma = 2.54e5 / (Math.pow(mE0keV, 1.5) - Math.pow(eNukeV, 1.5));
         return ((1 + mH) * (sigma * sigma)) / ((sigma + chi) * ((sigma * (1.0 + mH)) + (mH * chi)));
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.BremsstrahlungAnalytic#initialize(gov.nist.microanalysis.EPQLibrary.Composition,
       *      double, double)
       */
      @Override
      public void initialize(Composition comp, double e0, double toa) {
         super.initialize(comp, e0, toa);
         mH = 0;
         for(final Element el : mComposition.getElementSet()) {
            final double hEl = (4.5 * el.getAtomicWeight()) / Math2.sqr(el.getAtomicNumber());
            mH += mComposition.weightFraction(el, true) * hEl;
         }
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.BremsstrahlungAnalytic#compute(double)
       */
      @Override
      public double compute(double eNu) {
         final double eNukeV = FromSI.keV(eNu);
         if((eNukeV > 0.0) && (eNukeV < mE0keV))
            try {
               return ((mE0keV - eNukeV) / Math.pow(eNukeV, 1.21)) * philibert(eNu);
            }
            catch(final EPQException e) {
               return 0.0;
            }
         else
            return 0.0;
      }
   }

   /**
    * <p>
    * Implements what DTSA calls Lifshin's quadratic but which actually differs
    * from Lifshin's 1974 proceeding note in that the denominator of the (E0-E)
    * term is E^2 not E. The DTSA version seems to fit spectra better than
    * Lifshin's original expression.
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
   public static class DTSAQuadratic
      extends
      QuadraticBremsstrahlung {

      public DTSAQuadratic() {
         super("DTSA", LitReference.DTSA);
         mA = 110.0; // Magic numbers....
         mB = 3.3;
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.BremsstrahlungAnalytic#compute(double)
       */
      @Override
      public double compute(double eNu) {
         final double nukeV = FromSI.keV(eNu);
         if((nukeV > 0.0) && (nukeV < mE0keV)) {
            final double ee = Math.pow(mE0keV, 1.65) - Math.pow(nukeV, 1.65);
            final double chiC = MassAbsorptionCoefficient.toCmSqrPerGram(mMac.compute(mComposition, eNu))
                  / Math.sin(mTakeOffAngle);
            final double fE = 1.0 / (1.0 + (A1 * ee * chiC) + (A2 * Math2.sqr(ee * chiC)));
            final double x = (mE0keV - nukeV) / nukeV;
            return fE * mMeanZ * x * (mA + (mB * x));
         } else
            return 0.0;
      }
   }

   /**
    * <p>
    * This class implements exactly the expression from Lifshin's 1974
    * proceeding note. This is the expression referenced by Fiori, Myklebust,
    * Heinrich and Yakowitz's 1976 article, Small's 1987 article and Heinrich's
    * 1982 book. However it is not the expression implemented by DTSA.
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
   public static class Lifshin1974Model
      extends
      QuadraticBremsstrahlung {

      /**
       * Constructs an object representing Lifshin 1974 simple Bremsstralung
       * model
       */
      public Lifshin1974Model() {
         super("Lifshin 1974", lifshin1974);
         mA = 102.1;
         mB = 2.572;
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.BremsstrahlungAnalytic#compute(double)
       */
      @Override
      public double compute(double eNu) {
         final double nukeV = FromSI.keV(eNu);
         if((nukeV > 0.0) && (nukeV < mE0keV)) {
            final double ee = Math.pow(mE0keV, 1.65) - Math.pow(nukeV, 1.65);
            final double chiC = MassAbsorptionCoefficient.toCmSqrPerGram(mMac.compute(mComposition, eNu))
                  / Math.sin(mTakeOffAngle);
            final double fE = 1.0 / (1.0 + (A1 * ee * chiC) + (A2 * Math2.sqr(ee * chiC)));
            return (fE * mMeanZ * ((mA * (mE0keV - nukeV)) + (mB * Math2.sqr(mE0keV - nukeV)))) / nukeV;
         } else
            return 0.0;
      }
   }

   abstract protected static class QuadraticBremsstrahlung
      extends
      BremsstrahlungAnalytic {

      protected static final double A1 = 2.4e-6;
      protected static final double A2 = 1.44e-12;

      protected double mMeanZ = Double.NaN;
      protected double mA = 1.0;
      protected double mB = 0.0;
      private final boolean mWeightAverage = false;

      protected QuadraticBremsstrahlung(String name, LitReference ref) {
         super(name, ref);
      }

      public double getLinearTerm() {
         return mA;
      }

      public double getQuadraticTerm() {
         return mB;
      }

      public void setLinearTerm(double a) {
         mA = a;
      }

      public void setQuadraticTerm(double b) {
         mB = b;
      }

      /**
       * Set the model parameters.
       * 
       * @param a The linear parameter
       * @param b The quadratic parameter
       */
      public void setParameters(double a, double b) {
         mA = a;
         mB = b;
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.BremsstrahlungAnalytic#initialize(gov.nist.microanalysis.EPQLibrary.Composition,
       *      double, double)
       */
      @Override
      public void initialize(Composition comp, double e0, double toa) {
         super.initialize(comp, e0, toa);
         /**
          * There is a disagreement between Heinrich et al and Donovan about how
          * to perform the Z averaging. Heinrich says use weight fraction while
          * Donovan says use the mole fraction. I think of the issue as follows.
          * Consider you have A+B atoms of a material consisting of A atoms with
          * Z_A and B atoms with Z_B. The total number of protons in the
          * material is A*Z_A + B*Z_B for A+B atoms so the average Z per atom is
          * (A*Z_A + B*Z_B)/(A+B). That is to say I think Donovan is correct.
          */
         mMeanZ = mWeightAverage ? comp.weightAvgAtomicNumber() : comp.meanAtomicNumber();
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.BremsstrahlungAnalytic#fitBackground(gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector,
       *      gov.nist.microanalysis.EPQLibrary.ISpectrumData,
       *      java.util.Collection)
       */
      @Override
      public ISpectrumData fitBackground(final EDSDetector det, ISpectrumData spec, Collection<?> rois)
            throws EPQException {
         spec = SpectrumUtils.applyZeroPeakDiscriminator(spec);
         // Count the number of data points
         int roisLen = 0;
         for(final Object obj : rois)
            if(obj instanceof int[]) {
               final int[] roi = (int[]) obj;
               roisLen += roi[1] - roi[0];
            } else {
               final Interval iv = (Interval) obj;
               roisLen += iv.max() - iv.min();
            }
         // Allocate arrays for the data
         final double[] x = new double[roisLen];
         final double[] y = new double[roisLen];
         final double[] sig = new double[roisLen];
         // Fill in the arrays
         {
            int i = 0;
            for(final Object obj : rois) {
               int min, max;
               if(obj instanceof int[]) {
                  min = ((int[]) obj)[0];
                  max = ((int[]) obj)[1];
               } else {
                  min = ((Interval) obj).min();
                  max = ((Interval) obj).max();
               }
               for(int ch = min; ch < max; ++ch) {
                  x[i] = ch;
                  y[i] = spec.getCounts(ch);
                  sig[i] = y[i] > 0.0 ? Math.sqrt(y[i]) : Double.MAX_VALUE;
                  ++i;
               }
            }
         }
         // Create an object to perform the fit.
         final LinearLeastSquares llsq = new LinearLeastSquares(x, y, sig) {

            private final ISpectrumData mSpecA = getSpec(1.0, 0.0);
            private final ISpectrumData mSpecB = getSpec(0.0, 1.0);

            private ISpectrumData getSpec(double a, double b)
                  throws EPQException {
               mA = a;
               mB = b;
               det.reset();
               toDetector(det, 1.0);
               return det.getSpectrum(1.0);
            }

            @Override
            protected void fitFunction(double xi, double[] afunc) {
               final int ch = (int) Math.round(xi);
               afunc[0] = mSpecA.getCounts(ch);
               afunc[1] = mSpecB.getCounts(ch);
            }

            @Override
            protected int fitFunctionCount() {
               return 2;
            }
         };
         // Get the results
         final UncertainValue2[] res = llsq.getResults();
         mA = res[0].doubleValue();
         mB = res[1].doubleValue();
         det.reset();
         toDetector(det, 1.0);
         final ISpectrumData out = det.getSpectrum(1.0);
         SpectrumUtils.rename(out, "Background[" + spec.toString() + "," + this.getName() + ",A=" + Double.toString(mA) + ",B="
               + Double.toString(mB) + "]");
         System.out.println(spec.toString() + "\t\t" + Double.toString(mA) + "\t" + Double.toString(mB));
         return out;
      }
   }

   /**
    * Compute the bremsstrahlung spectrum as measured by the specified detector
    * at the specified beam flux (nA*sec). Use det.getSpectrum(..) to get the
    * resulting spectrum.
    * 
    * @param det
    * @param flux - probe flux in nA*sec / 10 eV window
    */
   public void toDetector(EDSDetector det, double flux) {
      final EditableSpectrum es = det.getSpectrum();
      final double f = es.getChannelWidth() / 10.0;
      for(int ch = es.getChannelCount() - 1; ch >= 0; --ch) {
         final double energy = ToSI.eV(SpectrumUtils.avgEnergyForChannel(es, ch));
         det.addEvent(energy, Math.max(0.0, compute(energy)) * flux * f);
      }
   }

   /**
    * Fit a Bremsstrahlung background to the specified spectrum using the
    * specified ranges of channels as the regions to fit.
    * 
    * @param det The detector on which the spectrum was collected
    * @param spec The spectrum to fit
    * @param rois A collection of int[2] objects defining the background region
    *           to fit (as channels)
    * @return An ISpectrumData containing the best fit background spectrum
    * @throws EPQException
    */
   public ISpectrumData fitBackground(EDSDetector det, ISpectrumData spec, Collection<?> rois)
         throws EPQException {
      spec = SpectrumUtils.applyZeroPeakDiscriminator(spec);
      det.reset();
      toDetector(det, 1.0);
      final ISpectrumData brem = det.getSpectrum(1.0);
      double sS = 0.0, sB = 0.0;
      for(final Object roi : rois) {
         final int minCh = roi instanceof int[] ? ((int[]) roi)[0] : ((Interval) roi).min();
         final int maxCh = roi instanceof int[] ? ((int[]) roi)[1] : ((Interval) roi).max();
         for(int ch = minCh; ch < maxCh; ++ch) {
            final double s = spec.getCounts(ch);
            final double b = brem.getCounts(ch);
            sS += b / s;
            sB += (b * b) / (s * s);
         }
      }
      final ISpectrumData out = SpectrumUtils.scale(sS / sB, brem);
      SpectrumUtils.rename(out, "Background[" + spec.toString() + "," + this.getName() + ", scale=" + Double.toString(sS / sB)
            + "]");
      return out;
   }

   /**
    * Returns a range of channels within the input <code>chRange</code> which is
    * closest to <code>ch</code> and of largest width possible up to and
    * including <code>desiredWidth</code>.
    * 
    * @param chRange
    * @param ch
    * @param desiredWidth
    * @return int[]
    */
   private int[] getOptimalRange(int[] chRange, int ch, int desiredWidth) {
      int minCh = Math.max(ch - (desiredWidth / 2), chRange[0]);
      final int maxCh = Math.min(minCh + desiredWidth, chRange[1]);
      minCh = Math.max(maxCh - desiredWidth, chRange[0]);
      return new int[] {
         minCh,
         maxCh
      };
   }

   private boolean overlaps(int[] i0, int[] i1) {
      return Math.max(i0[0], i1[0]) < Math.min(i0[1], i1[1]);
   }

   /**
    * Fits the best possible background to the specified set of x-ray
    * transitions <code>xrts</code> for the spectrum <code>spec</code> collected
    * on detector <code>det</code> on a material of composition
    * <code>comp</code>. This is useful for background removal around a specific
    * transition such as you might want to do when fitting a standard to an
    * unknown.
    * 
    * @param det The detector
    * @param spec The spectrum to fit
    * @param comp The composition of the material for the spectrum
    * @param xrts The set of x-ray transitions about which to fit the background
    * @return ISpectrumData
    * @throws EPQException
    */
   public ISpectrumData fitBackground(EDSDetector det, ISpectrumData spec, Composition comp, XRayTransitionSet xrts)
         throws EPQException {
      final DetectorLineshapeModel dlm = det.getDetectorLineshapeModel();
      final double xtra = ToSI.eV(dlm.getFWHMatMnKa());
      final RegionOfInterestSet rois = new RegionOfInterestSet(dlm, 0.001, 1.5 * xtra, xtra);
      rois.add(xrts);
      final ISpectrumData result = fitBackground(det, spec, comp, rois);
      SpectrumUtils.rename(result, "Background[" + spec.toString() + "," + this.getName() + "," + xrts.toString() + "]");
      return result;
   }

   /**
    * Fits the best possible background to the specified set region-of-interests
    * <code>rois</code> for the spectrum <code>spec</code> collected on detector
    * <code>det</code> on a material of composition <code>comp</code>. This is
    * useful for background removal around a specific transition such as you
    * might want to do when fitting a standard to an unknown.
    * 
    * @param det An EDSDectector on which the spectrum was collected
    * @param spec The spectrum
    * @param comp The composition of the material represented by the spectrum
    * @param rois Some region of interests
    * @return ISpectrumData A spectrum representing the Bremsstrahlung
    *         background
    * @throws EPQException
    */
   public ISpectrumData fitBackground(EDSDetector det, ISpectrumData spec, Composition comp, RegionOfInterestSet rois)
         throws EPQException {
      initialize(comp, ToSI.eV(SpectrumUtils.getBeamEnergy(spec)), SpectrumUtils.getTakeOffAngle(spec.getProperties()));
      final Collection<int[]> res = new ArrayList<int[]>();
      final DetectorLineshapeModel dlm = det.getDetectorLineshapeModel();
      final double xtra = ToSI.eV(dlm.getFWHMatMnKa());
      final double e0 = ToSI.eV(SpectrumUtils.getBeamEnergy(spec));
      final double eMin = ToSI.eV(SpectrumUtils.minEnergyForChannel(spec, SpectrumUtils.smallestNonZeroChannel(spec)));
      final RegionOfInterestSet roi = new RegionOfInterestSet(dlm, 0.001, 1.5 * xtra, xtra);
      for(final Element elm : comp.getElementSet())
         roi.add(new XRayTransitionSet(elm, eMin, e0));
      final ArrayList<int[]> tmp = new ArrayList<int[]>();
      {
         int min = SpectrumUtils.channelForEnergy(spec, FromSI.eV(eMin));
         for(final RegionOfInterestSet.RegionOfInterest r : roi) {
            final int max = SpectrumUtils.channelForEnergy(spec, FromSI.eV(r.lowEnergy()));
            if(max > min) {
               final int[] ii = new int[] {
                  min,
                  max
               };
               tmp.add(ii);
            }
            min = SpectrumUtils.channelForEnergy(spec, FromSI.eV(r.highEnergy()));
         }
         final int max = SpectrumUtils.channelForEnergy(spec, FromSI.eV(e0));
         final int[] ii = new int[] {
            min,
            max
         };
         tmp.add(ii);
      }
      final int width = 10;
      {
         double lowE = Double.MAX_VALUE, highE = 0.0;
         for(final RegionOfInterestSet.RegionOfInterest roi1 : rois) {
            lowE = Math.min(roi1.lowEnergy(), lowE);
            highE = Math.max(roi1.highEnergy(), highE);
         }
         final int lowCh = SpectrumUtils.channelForEnergy(spec, FromSI.eV(lowE));
         final int highCh = SpectrumUtils.channelForEnergy(spec, FromSI.eV(highE));
         int[] bestLow = null, bestHigh = null;
         int deltaLow = Integer.MAX_VALUE, deltaHigh = Integer.MAX_VALUE;
         for(final int[] ii : tmp) {
            {
               final int[] test = getOptimalRange(ii, lowCh, width);
               if((test[0] < lowCh) && ((lowCh - test[1]) < deltaLow)
                     && ((bestLow == null) || ((test[1] - test[0]) >= (bestLow[1] - bestLow[0])))) {
                  bestLow = test;
                  deltaLow = lowCh - test[1];
               }
            }
            {
               final int[] test = getOptimalRange(ii, highCh, width);
               if((test[1] > highCh) && ((test[0] - highCh) < deltaHigh)
                     && ((bestHigh == null) || ((test[1] - test[0]) >= (bestHigh[1] - bestHigh[0])))) {
                  bestHigh = test;
                  deltaHigh = test[0] - highCh;
               }
            }
         }
         if(bestLow != null)
            res.add(bestLow);
         if(bestHigh != null)
            res.add(bestHigh);
      }
      {
         // Add an extra set of channels aroung E0/3.0 to provide balance to
         // the quadratic equation.
         int[] best = null;
         final int extraCh = SpectrumUtils.channelForEnergy(spec, FromSI.eV(e0 / 3.0));
         int delta = Integer.MAX_VALUE;
         for(final int[] ii : tmp) {
            final int[] test = getOptimalRange(ii, extraCh, 10);
            if(Math.abs(((test[0] + test[1]) / 2) - extraCh) < delta) {
               best = test;
               delta = Math.abs(((test[0] + test[1]) / 2) - extraCh);
            }
         }
         boolean add = true;
         for(final int[] ii : res)
            if(overlaps(ii, best))
               add = false;
         if(add)
            res.add(best);
      }
      final ISpectrumData result = res.size() > 1 ? fitBackground(det, spec, res) : fitBackground(det, spec, comp);
      SpectrumUtils.rename(result, "Background[" + spec.toString() + "," + this.getName() + "," + rois.toString() + "]");
      return result;
   }

   /**
    * Automatically select regions-of-interest based on the specified
    * composition and then fit a background to the specified spectrum.
    * 
    * @param det The detector
    * @param spec A spectrum
    * @param comp The composition
    * @return An ISpectrumData object containing the best fit background
    * @throws EPQException
    */
   public ISpectrumData fitBackground(EDSDetector det, ISpectrumData spec, Composition comp)
         throws EPQException {
      spec = SpectrumUtils.applyZeroPeakDiscriminator(spec);
      initialize(comp, ToSI.eV(SpectrumUtils.getBeamEnergy(spec)), SpectrumUtils.getTakeOffAngle(spec.getProperties()));
      final Collection<int[]> res = new ArrayList<int[]>();
      final DetectorLineshapeModel dlm = det.getDetectorLineshapeModel();
      final double e0 = ToSI.keV(mE0keV);
      final double eMin = ToSI.eV(Math.min(100.0, SpectrumUtils.minEnergyForChannel(spec, SpectrumUtils.smallestNonZeroChannel(spec))));
      final RegionOfInterestSet roi = new RegionOfInterestSet(dlm, 0.001, 0.0, 0.0);
      for(final Element elm : comp.getElementSet())
         roi.add(new XRayTransitionSet(elm, eMin, e0));

      final ArrayList<int[]> tmp = new ArrayList<int[]>();
      {
         int min = SpectrumUtils.channelForEnergy(spec, FromSI.eV(eMin));
         for(final RegionOfInterestSet.RegionOfInterest r : roi) {
            final int max = SpectrumUtils.channelForEnergy(spec, FromSI.eV(r.lowEnergy()));
            if(max > min) {
               final int[] ii = new int[] {
                  min,
                  max
               };
               tmp.add(ii);
            }
            min = SpectrumUtils.channelForEnergy(spec, FromSI.eV(r.highEnergy()));
         }
         final int max = SpectrumUtils.channelForEnergy(spec, FromSI.eV(e0));
         final int[] ii = new int[] {
            min,
            max
         };
         tmp.add(ii);
      }
      final int first = SpectrumUtils.firstNonZeroChannel(spec);
      {
         if(first < (spec.getChannelCount() / 10))
            res.add(new int[] {
               first,
               first + 4
            });
      }
      final int width = 20;
      for(int i = 0; i < 8; ++i) {
         if(res.size() > 4)
            break;
         final int j = i < 4 ? 2 * i : (2 * i) + 1;
         final int min = SpectrumUtils.channelForEnergy(spec, j * 2000.0);
         final int max = SpectrumUtils.bound(spec, SpectrumUtils.channelForEnergy(spec, (j + 1) * 2000.0));
         if(ToSI.eV(SpectrumUtils.maxEnergyForChannel(spec, min)) > e0)
            break;
         if(min >= spec.getChannelCount())
            break;
         int[] best = null;
         for(final int[] ii : tmp)
            if((ii[1] > min) && (ii[0] < max)) {
               final int i0 = Math.max(ii[0], min);
               final int i1 = Math.min(ii[1], max);
               if((best == null) || ((i1 - i0) > (best[1] - best[0])))
                  best = new int[] {
                     i0,
                     i1
                  };
            }

         if(best != null) {
            if((best[1] - best[0]) > width) {
               final int a = (best[1] + best[0]) / 2;
               best[0] = a - (width / 2);
               best[1] = a + (width / 2);
            }
            res.add(best);
         }
      }
      ISpectrumData tmpSpec = fitBackground(det, spec, res);
      {
         int[] ii = null;
         boolean redo = false;
         for(int i = SpectrumUtils.channelForEnergy(spec, 2.0e3); i >= first; --i) {
            final double tol = -4.0 * Math.sqrt(spec.getCounts(i));
            if(ii == null) {
               if((spec.getCounts(i) - tmpSpec.getCounts(i)) <= tol)
                  ii = new int[] {
                     i,
                     i
                  };
            } else if((spec.getCounts(i) - tmpSpec.getCounts(i)) > tol)
               if((ii[1] - i) > 4) {
                  ii[0] = i + 1;
                  if((ii[1] - ii[0]) > width) {
                     ii[0] = ((ii[1] + ii[0]) - width) / 2;
                     ii[1] = ii[0] + width;
                  }
                  res.add(ii);
                  ii = null;
                  redo = true;
               } else
                  ii = null;
         }
         if(redo)
            tmpSpec = fitBackground(det, spec, res);
      }
      return tmpSpec;
   }

   /**
    * Automatically select regions-of-interest based on the specified
    * composition and then fit a background to the specified spectrum.
    * 
    * @param det The detector
    * @param spec A spectrum
    * @param comp The composition
    * @return An ISpectrumData object containing the best fit background
    * @throws EPQException
    */
   public ISpectrumData fitBackground2(EDSDetector det, ISpectrumData spec, Composition comp)
         throws EPQException {
      spec = SpectrumUtils.applyZeroPeakDiscriminator(spec);
      initialize(comp, ToSI.eV(SpectrumUtils.getBeamEnergy(spec)), SpectrumUtils.getTakeOffAngle(spec.getProperties()));
      final DetectorLineshapeModel dlm = det.getDetectorLineshapeModel();
      final double e0 = ToSI.keV(mE0keV);
      final double eMin = ToSI.eV(Math.min(100.0, SpectrumUtils.minEnergyForChannel(spec, SpectrumUtils.smallestNonZeroChannel(spec))));
      final RegionOfInterestSet roi = new RegionOfInterestSet(dlm, 0.001, 0.0, 0.0);
      for(final Element elm : comp.getElementSet())
         roi.add(new XRayTransitionSet(elm, eMin, e0));

      SortedSet<Interval> intervals = new TreeSet<Interval>();
      {
         int min0 = Math.max(SpectrumUtils.firstNonZeroChannel(spec), SpectrumUtils.channelForEnergy(spec, FromSI.eV(eMin)));
         for(final RegionOfInterestSet.RegionOfInterest r : roi) {
            final int max0 = SpectrumUtils.channelForEnergy(spec, FromSI.eV(r.lowEnergy()));
            if(max0 > min0)
               intervals = Interval.add(intervals, new Interval(min0, max0));
            min0 = SpectrumUtils.channelForEnergy(spec, FromSI.eV(r.highEnergy()));
         }
         min0 = SpectrumUtils.bound(spec, min0);
         final int max0 = SpectrumUtils.bound(spec, SpectrumUtils.channelForEnergy(spec, FromSI.eV(e0)));
         if(max0 > min0)
            intervals = Interval.add(intervals, new Interval(min0, max0));
      }
      final SortedSet<Interval> res = new TreeSet<Interval>();
      for(final Interval ii : intervals) {
         final double minE = SpectrumUtils.minEnergyForChannel(spec, ii.min());
         final double min = minE + dlm.rightWidth(minE, 0.01);
         final double maxE = SpectrumUtils.maxEnergyForChannel(spec, ii.max());
         final double max = maxE - dlm.leftWidth(minE, 0.01);
         final int minCh = SpectrumUtils.channelForEnergy(spec, min);
         final int maxCh = SpectrumUtils.channelForEnergy(spec, max);
         if((minCh + 4) < maxCh)
            res.add(new Interval(minCh, maxCh));
      }
      final ISpectrumData tmpSpec = fitBackground(det, spec, res);
      return tmpSpec;
   }

   @Override
   protected void initializeDefaultStrategy() {
      addDefaultAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Default);
   }

   public static final Small1987 Small87 = new Small1987();
   public static final Lifshin1974Model Lifshin74 = new Lifshin1974Model();
   public static final DTSAQuadratic DTSA = new DTSAQuadratic();
   public static final Reed1975 Reed75 = new Reed1975();
   public static final NoBremsstrahlung None = new NoBremsstrahlung();

   private static final AlgorithmClass AllImplementations[] = {
      Small87,
      Lifshin74,
      DTSA,
      Reed75,
      None
   };

   /**
    * Constructs an abstract BremsstrahlungAnalytic object.
    * 
    * @param name
    * @param ref
    */
   protected BremsstrahlungAnalytic(String name, LitReference ref) {
      super("Analytical Bremsstrahlung", name, ref);
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#getAllImplementations()
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(AllImplementations);
   }

}
