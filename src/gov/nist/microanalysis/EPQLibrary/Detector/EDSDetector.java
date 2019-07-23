package gov.nist.microanalysis.EPQLibrary.Detector;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.EditableSpectrum;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQLibrary.XRayTransitionSet;
import gov.nist.microanalysis.EPQTools.EPQXStream;
import gov.nist.microanalysis.NISTMonte.Gen3.BaseXRayGeneration3;
import gov.nist.microanalysis.NISTMonte.Gen3.BaseXRayGeneration3.XRay;
import gov.nist.microanalysis.Utility.Math2;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * <p>
 * Implements an abstract base class for energy dispersive-type x-ray detectors.
 * EDSDetector objects implement the basic XRayDetector interface.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author Nicholas
 * @version 1.0
 */
public class EDSDetector
   implements
   IXRayDetector {

   static private Map<String, EDSDetector> mCache = new HashMap<String, EDSDetector>();

   private final DetectorProperties mDetProperties;
   private final EDSCalibration mCalibration;
   private transient Matrix mPseudoInverse = null;

   protected transient int mHash = Integer.MAX_VALUE;

   /**
    * Do the contents of mSpectrum represent the same information as in
    * mAccumulator?
    */
   transient protected boolean mDirty;
   transient private EditableSpectrum mSpectrum;
   transient private double[] mAccumulator;

   public EditableSpectrum getSpectrum() {
      if(mSpectrum == null) {
         final double scale = mCalibration.getChannelWidth();
         final double offset = mCalibration.getZeroOffset();
         mSpectrum = new EditableSpectrum(getDetectorProperties().getChannelCount(), scale, offset);
         final SpectrumProperties sp = mSpectrum.getProperties();
         sp.setDetector(this);
         sp.setBooleanProperty(SpectrumProperties.IsTheoreticallyGenerated, true);
         sp.setTimestampProperty(SpectrumProperties.AcquisitionTime, new Date());
      }
      return mSpectrum;
   }

   protected double[] getAccumulator() {
      if(mAccumulator == null)
         mAccumulator = new double[getDetectorProperties().getChannelCount()];
      return mAccumulator;
   }

   public ISpectrumData getRawXRayData(final double doseScale) {
      final double eVperBin = mCalibration.getChannelWidth();
      final double offset = mCalibration.getZeroOffset();
      final EditableSpectrum res = new EditableSpectrum(getDetectorProperties().getChannelCount(), eVperBin, offset);
      final SpectrumProperties sp = res.getProperties();
      sp.setDetector(this);
      sp.setBooleanProperty(SpectrumProperties.IsTheoreticallyGenerated, true);
      sp.setTimestampProperty(SpectrumProperties.AcquisitionTime, new Date());
      System.arraycopy(getAccumulator(), 0, res.getCounts(), 0, res.getChannelCount());
      final double calScale = mCalibration.getEfficiency(getDetectorProperties())[res.getChannelCount() / 6];
      return SpectrumUtils.scale(mCalibration.getFudgeFactor() * doseScale * calScale, res);
   }

   /**
    * Convolve takes the events in the accumulator and convolves them into the
    * existing spectrum. Convolve may be called many times as new events are
    * recorded by the detector.
    */
   protected void convolve() {
      final EditableSpectrum es = getSpectrum();
      final double eVperBin = es.getChannelWidth();
      final DetectorLineshapeModel dlm = getDetectorLineshapeModel();
      final double MIN_I = 0.0001;
      final double[] eff = getEfficiency();
      final double[] acc = getAccumulator();
      final double[] fs = Math2.multiply(eVperBin * mCalibration.getFudgeFactor(), Math2.multiply(acc, eff));
      final double[] spec = es.getCounts();
      Arrays.fill(spec, 0.0);
      for(int i = 0; i < fs.length; ++i)
         if(fs[i] > 0.0) {
            final double e = SpectrumUtils.minEnergyForChannel(es, i);
            final int highBin = Math2.bound(SpectrumUtils.channelForEnergy(es, e + dlm.rightWidth(e, MIN_I)), 0, spec.length);
            final int lowBin = Math2.bound(SpectrumUtils.channelForEnergy(es, e - dlm.leftWidth(e, MIN_I)), 0, spec.length);
            double ee = SpectrumUtils.minEnergyForChannel(es, lowBin);
            double prev = dlm.compute(ee, e);
            for(int ch = lowBin; ch < highBin; ++ch, ee += eVperBin) {
               final double curr = dlm.compute(ee + eVperBin, e);
               spec[ch + 1] += 0.5 * fs[i] * (prev + curr);
               prev = curr;
            }
         }
      final SpectrumProperties sp = es.getProperties();
      sp.setTimestampProperty(SpectrumProperties.AcquisitionTime, new Date());
      sp.setBooleanProperty(SpectrumProperties.IsTheoreticallyGenerated, true);
      mDirty = false;
   }

   /**
    * Use <code>createDetector(...)</code> instead!!!!
    */
   private EDSDetector(final DetectorProperties dp, final EDSCalibration calib) {
      mDetProperties = dp;
      mCalibration = calib;
      mAccumulator = null;
      mSpectrum = null;
      reset();
   }

   /**
    * createDetector hides EDSDetector caching behind a static function. Only
    * one instance of each EDSDetector will be created. If further instances are
    * required, the cached instance will be reused. Use this in place of
    * <code>new EDSDetector()</code>.
    *
    * @param dp
    * @param calib
    * @return EDSDetector
    */
   static public EDSDetector createDetector(final DetectorProperties dp, final EDSCalibration calib) {
      try {
         final String key = getCacheKey(dp, calib);
         if(!mCache.containsKey(key))
            mCache.put(key, new EDSDetector(dp, calib));
         return mCache.get(key);
      }
      catch(final EPQException e) {
         return new EDSDetector(dp, calib);
      }
   }

   private static String getCacheKey(final DetectorProperties dp, final EDSCalibration calib)
         throws EPQException {
      final SpectrumProperties calibProps = calib.getProperties();
      final SpectrumProperties detProps = dp.getProperties();
      if(!calibProps.isDefined(SpectrumProperties.CalibrationGUID))
         calibProps.setTextProperty(SpectrumProperties.CalibrationGUID, EPQXStream.generateGUID(calib));
      if(!detProps.isDefined(SpectrumProperties.DetectorGUID))
         detProps.setTextProperty(SpectrumProperties.DetectorGUID, EPQXStream.generateGUID(dp));
      final String key = calibProps.getTextProperty(SpectrumProperties.CalibrationGUID)
            + detProps.getTextProperty(SpectrumProperties.DetectorGUID);
      return key;
   }

   static public EDSDetector updateDetector(final DetectorProperties dp, final EDSCalibration calib)
         throws EPQException {
      final String key = getCacheKey(dp, calib);
      mCache.put(key, new EDSDetector(dp, calib));
      return mCache.get(key);
   }

   private Object readResolve() {
      mHash = Integer.MAX_VALUE;
      return this;
   }

   @Override
   public DetectorProperties getDetectorProperties() {
      return mDetProperties;
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.Detector.IXRayDetector#getProperties()
    */
   @Override
   public SpectrumProperties getProperties() {
      final SpectrumProperties sp = new SpectrumProperties();
      sp.addAll(mDetProperties.getProperties());
      sp.addAll(mCalibration.getProperties());
      return sp;
   }

   @Override
   public String toString() {
      return getDetectorProperties().toString() + " - " + mCalibration.toString();
   }

   @Override
   public String getName() {
      return getDetectorProperties().toString();
   }

   /**
    * Specify the window to use in front of the detector. You can change windows
    * and the resulting spectrum will be recalculated based on the new window's
    * properties.
    *
    * @param window XRayWindow
    */
   public void setWindow(final IXRayWindowProperties window) {
      if(getDetectorProperties().getWindow() != window) {
         getDetectorProperties().setWindow(window);
         mDirty = true;
      }
   }

   /**
    * Returns the window associated with this detector.
    *
    * @return A window properties object
    */
   public IXRayWindowProperties getWindow() {
      return getDetectorProperties().getWindow();
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.Detector.IXRayDetector#addEvent(double,
    *      double)
    */
   @Override
   public void addEvent(final double energy, final double intensity) {
      final int ch = SpectrumUtils.channelForEnergy(getSpectrum(), FromSI.eV(energy));
      final double[] acc = getAccumulator();
      if((ch >= 0) && (ch < acc.length)) {
         acc[ch] += intensity;
         mDirty = true;
      }
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.Detector.IXRayDetector#reset()
    */
   @Override
   public void reset() {
      mAccumulator = null;
      mSpectrum = null;
      mDirty = true;
   }

   /**
    * Returns the ElectronProbe with which this detector is associated.
    *
    * @return ElectronProbe
    */
   public ElectronProbe getOwner() {
      return getDetectorProperties().getOwner();
   }

   /**
    * Sets the ElectronProbe with which this detector is associated.
    *
    * @param ep
    */
   public void setOwner(final ElectronProbe ep) {
      getDetectorProperties().setOwner(ep);
   }

   /**
    * Implements the actionPerformed method such that if the source is an
    * instance of XRaySource then all the x-ray events in XRaySource are
    * accumulated.
    *
    * @param e
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   @Override
   public void actionPerformed(final ActionEvent e) {
      final Object src = e.getSource();
      if(src instanceof BaseXRayGeneration3) {
         final BaseXRayGeneration3 bxg = (BaseXRayGeneration3) src;
         for(int i = bxg.getEventCount() - 1; i >= 0; --i) {
            final XRay xr = bxg.getXRay(i);
            addEvent(xr.getEnergy(), xr.getIntensity());
         }
      }
   }

   /**
    * Returns the spectrum associated with this detector. Be careful as often
    * detectors are static objects, there is only one spectrum per detector and
    * this can't be shared between threads without consequences. Scale is a
    * multiplicative factor applied channel-by-channel to the spectrum channel
    * data. Nominally, scale is 1.0
    *
    * @param scale
    * @return ISpectrumData
    */
   public ISpectrumData getSpectrum(final double scale) {
      if(mDirty) {
         convolve();
         mDirty = false;
      }
      return SpectrumUtils.scale(scale, getSpectrum());
   }

   /**
    * Constructs the full set of XRayTransitions of edge energy less than
    * maxEnergy which can reasonably be expected to be visible with this
    * specified detector.
    *
    * @param elm
    * @param maxEnergy in Joules
    */
   public XRayTransitionSet getVisibleTransitions(final Element elm, final double maxEnergy) {
      final Set<XRayTransition> xrts = new TreeSet<XRayTransition>();
      for(int tr = XRayTransition.KA1; tr < XRayTransition.Last; ++tr)
         if(XRayTransition.exists(elm, tr) && (XRayTransition.getEdgeEnergy(elm, tr) < maxEnergy)) {
            final XRayTransition xrt = new XRayTransition(elm, tr);
            if(isVisible(xrt, maxEnergy))
               xrts.add(xrt);
         }
      return new XRayTransitionSet(xrts);
   }

   /**
    * Checks the specified spectrum and matches the energy scale and offset for
    * this detector to the specified tolerance.
    *
    * @param spec A ISpectrumData
    * @param tol - Tolerance (nominally 0.001)
    * @throws EPQException
    */
   public void checkSpectrumScale(final ISpectrumData spec, final double tol)
         throws EPQException {
      {
         final double scale = getProperties().getNumericProperty(SpectrumProperties.EnergyScale);
         if(Math.abs(spec.getChannelWidth() - scale) > (scale * tol))
            throw new EPQException("The channel widths for " + spec.toString() + " and " + toString() + " don't match.");
      }
      {
         final double off = getProperties().getNumericWithDefault(SpectrumProperties.EnergyOffset, 0.0);
         if(Math.abs(spec.getZeroOffset() - off) > (spec.getChannelCount() * spec.getChannelWidth() * tol))
            throw new EPQException("The zero offsets for " + spec.toString() + " and " + toString() + " don't match.");
      }
   }

   /**
    * Is the specified transition visible using this detector? This method
    * typically is used to filter out lines which are too low in energy.
    *
    * @param xrt
    * @param eBeam
    * @return boolean
    */
   public boolean isVisible(final XRayTransition xrt, final double eBeam) {
      return mCalibration.isVisible(xrt, eBeam);
   }

   public DetectorLineshapeModel getDetectorLineshapeModel() {
      return mCalibration.getLineshape();
   }

   /**
    * @see java.lang.Object#hashCode()
    */
   @Override
   public int hashCode() {
      if(mHash == Integer.MAX_VALUE) {
         final int PRIME = 31;
         int result = super.hashCode();
         result = (PRIME * result) + ((mCalibration == null) ? 0 : mCalibration.hashCode());
         result = (PRIME * result) + ((mDetProperties == null) ? 0 : mDetProperties.hashCode());
         if(result == Integer.MAX_VALUE)
            result = Integer.MIN_VALUE;
         mHash = result;
      }
      return mHash;
   }

   /**
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(final Object obj) {
      if(this == obj)
         return true;
      if(!super.equals(obj))
         return false;
      if(getClass() != obj.getClass())
         return false;
      final EDSDetector other = (EDSDetector) obj;
      if(mCalibration == null) {
         if(other.mCalibration != null)
            return false;
      } else if(!mCalibration.equals(other.mCalibration))
         return false;
      if(mDetProperties == null) {
         if(other.mDetProperties != null)
            return false;
      } else if(!mDetProperties.equals(other.mDetProperties))
         return false;
      return true;
   }

   public double[] getEfficiency() {
      return mCalibration.getEfficiency(getDetectorProperties());
   }

   /**
    * Set the position of the detector...
    *
    * @param pos The position in meters
    */
   public void setPosition(final double[] pos) {
      getDetectorProperties().setPosition(pos);
   }

   /**
    * Returns the number of channels collected by this detector.
    *
    * @return int
    */
   public int getChannelCount() {
      return getDetectorProperties().getChannelCount();
   }

   /**
    * The width of a single channel in the spectrum in eV
    *
    * @return in eV
    */
   public double getChannelWidth() {
      return mCalibration.getChannelWidth();
   }

   public double getZeroOffset() {
      return mCalibration.getZeroOffset();
   }

   public double getQuadratic() {
      return mCalibration.getQuadratic();
   }

   @Override
   public EDSCalibration getCalibration() {
      return mCalibration;
   }

   /**
    * The energies associated with the channel <i>ch</i> range from
    * minEnergyForChannel to maxEnergyForChannel.
    *
    * @param ch
    * @return The energy in eV
    */
   public double minEnergyForChannel(final int ch) {
      return getZeroOffset() + (ch * getChannelWidth());
   }

   /**
    * The energies associated with the channel <i>ch</i> range from
    * minEnergyForChannel to maxEnergyForChannel.
    *
    * @param ch
    * @return The energy in eV
    */
   public final double maxEnergyForChannel(final int ch) {
      return minEnergyForChannel(ch + 1);
   }

   /**
    * The energies associated with the channel <i>ch</i> range from
    * minEnergyForChannel to maxEnergyForChannel. The mid point is
    * avgEnergyForEnergy.
    *
    * @param ch
    * @return The energy in eV
    */
   public final double avgEnergyForChannel(final int ch) {
      return (minEnergyForChannel(ch) + maxEnergyForChannel(ch)) / 2.0;
   }

   /**
    * Returns the index of the channel which contains the specified channel. The
    * channel index may be outside of the bounds of valid channel indices for
    * this spectrum.
    *
    * @param e double - the energy in eV
    * @return int - the channel index
    */
   public int channelForEnergy(final double e) {
      return (int) ((e - getZeroOffset()) / getChannelWidth());
   }

   public int bound(final int ch) {
      return Math2.bound(ch, 0, getChannelCount());
   }

   /**
    * Create an EDSDetector with 100% efficiency at all x-ray energies
    *
    * @param nChannels Number of channels
    * @param chWidth Width in eV of each channel
    * @return EDSDetector
    * @throws EPQException
    */
   public static EDSDetector createPerfectDetector(final int nChannels, final double chWidth, final double[] pos)
         throws EPQException {
      class DeltaLineshapeModel
         extends
         DetectorLineshapeModel {
         private final double mChannelWidth;

         private DeltaLineshapeModel(final double chWidth) {
            mChannelWidth = chWidth;
         }

         @Override
         public double compute(final double ev, final double center) {
            return Math.abs(ev - center) < (mChannelWidth / 2.0) ? 1.0 : 0.0;
         }

         /*
          * (non-Javadoc)
          * @see java.lang.Object#hashCode()
          */
         @Override
         public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(mChannelWidth);
            result = (prime * result) + (int) (temp ^ (temp >>> 32));
            return result;
         }

         /*
          * (non-Javadoc)
          * @see java.lang.Object#equals(java.lang.Object)
          */
         @Override
         public boolean equals(final Object obj) {
            if(this == obj)
               return true;
            if(getClass() != obj.getClass())
               return false;
            final DeltaLineshapeModel other = (DeltaLineshapeModel) obj;
            if(Double.doubleToLongBits(mChannelWidth) != Double.doubleToLongBits(other.mChannelWidth))
               return false;
            return true;
         }

         @Override
         public double leftWidth(final double ev, final double fraction) {
            return 0.0;
         }

         @Override
         public double rightWidth(final double ev, final double fraction) {
            return 0.0;
         }

         @Override
         public DetectorLineshapeModel clone() {
            return new DeltaLineshapeModel(mChannelWidth);
         }
      }

      class IdealCalibration
         extends
         EDSCalibration {

         final private double mChannelWidth;

         public IdealCalibration(final double chWidth) {
            super("Ideal", chWidth, 0.0, new DeltaLineshapeModel(chWidth));
            mChannelWidth = chWidth;
         }

         /**
          * @see gov.nist.microanalysis.EPQLibrary.Detector.DetectorCalibration#getEfficiency(gov.nist.microanalysis.EPQLibrary.Detector.DetectorProperties)
          */
         @Override
         public double[] getEfficiency(final DetectorProperties dp) {
            final double[] res = new double[dp.getChannelCount()];
            Arrays.fill(res, 1.0);
            return res;
         }

         /**
          * @see gov.nist.microanalysis.EPQLibrary.Detector.DetectorCalibration#isVisible(gov.nist.microanalysis.EPQLibrary.XRayTransition,
          *      double,
          *      gov.nist.microanalysis.EPQLibrary.Detector.DetectorProperties)
          */
         @Override
         public boolean isVisible(final XRayTransition xrt, final double beam) {
            return true;
         }

         /**
          * @see gov.nist.microanalysis.EPQLibrary.Detector.EDSCalibration#clone()
          */
         @Override
         public EDSCalibration clone() {
            return new IdealCalibration(mChannelWidth);
         }
      }

      class IdealDetectorProperties
         extends
         DetectorProperties {
         IdealDetectorProperties(final int nChannels, final double[] pos) {
            super(new ElectronProbe("Perfect"), "Ideal Detector", nChannels, pos);
         }

      }

      class IdealDetector
         extends
         EDSDetector {

         IdealDetector(final int nChannels, final double chWidth, final double[] pos)
               throws EPQException {
            super(new IdealDetectorProperties(nChannels, pos), new IdealCalibration(chWidth));
         }

         @Override
         protected void convolve() {
            final EditableSpectrum es = getSpectrum();
            System.arraycopy(getAccumulator(), 0, es.getCounts(), 0, es.getChannelCount());
            mDirty = false;
         }
      }
      return new IdealDetector(nChannels, chWidth, pos);
   }

   /**
    * Create a new EDSDetector object to represent a basic Si(Li) detector.
    *
    * @param chCount
    * @param chWidth
    * @param fwhm
    * @return EDSDetector
    */
   public static EDSDetector createSiLiDetector(final int chCount, final double chWidth, final double fwhm) {
      final SiLiCalibration calib = new SiLiCalibration(chWidth, 0.0, fwhm);
      calib.makeBaseCalibration();
      final DetectorProperties dp = DetectorProperties.getDefaultSiLiProperties(new ElectronProbe("Probe"), "Si(Li)", chCount);
      return EDSDetector.createDetector(dp, calib);
   }

   /**
    * Create a new EDSDetector object to represent a basic Si(Li) detector.
    *
    * @param chCount
    * @param chWidth
    * @param fwhm
    * @return EDSDetector
    */
   public static EDSDetector createMicrocal(final int chCount, final double chWidth, final double fwhm) {
      final MicrocalCalibration calib = new MicrocalCalibration(chWidth, 0.0, fwhm);
      calib.makeBaseCalibration();
      final DetectorProperties dp = DetectorProperties.getDefaultSiLiProperties(new ElectronProbe("Probe"), "\u00B5Cal", chCount);
      return EDSDetector.createDetector(dp, calib);
   }

   /**
    * Create a new EDSDetector object to represent a basic SDD detector.
    *
    * @param chCount
    * @param chWidth
    * @param fwhm
    * @return EDSDetector
    */
   public static EDSDetector createSDDDetector(final int chCount, final double chWidth, final double fwhm) {
      return createSDDDetector(chCount, chWidth, 0.0, fwhm);
   }

   /**
    * Create a new EDSDetector object to represent a basic SDD detector.
    *
    * @param chCount
    * @param chWidth eV
    * @param zeroOffset eV
    * @param fwhm eV at Mn Ka
    * @return EDSDetector
    */
   public static EDSDetector createSDDDetector(final int chCount, final double chWidth, final double zeroOffset, final double fwhm) {
      final SiLiCalibration calib = new SDDCalibration(chWidth, zeroOffset, fwhm);
      calib.makeBaseCalibration();
      final DetectorProperties dp = DetectorProperties.getDefaultSDDProperties(new ElectronProbe("Default"), "SDD", chCount);
      return EDSDetector.createDetector(dp, calib);
   }

   public static EDSDetector readXML(final File file) {
      final EPQXStream xs = EPQXStream.getInstance();
      final Object tmp = xs.fromXML(file);
      return tmp instanceof EDSDetector ? (EDSDetector) tmp : null;
   }

   public void writeXML(final File file)
         throws IOException {
      try (final FileOutputStream fos = new FileOutputStream(file)) {
         final EPQXStream xs = EPQXStream.getInstance();
         xs.toXML(this, fos);
      }
   }

   public Matrix inverseDetectorFunction() {
      if(mPseudoInverse == null) {
         final int chCx = getChannelCount();
         final Matrix detFunc = new Matrix(chCx, chCx);
         final DetectorLineshapeModel dlm = getDetectorLineshapeModel();
         // final double[] eff =
         // getCalibration().getEfficiency(getDetectorProperties());
         for(int ch = 0; ch < chCx; ++ch) {
            final double center = avgEnergyForChannel(ch);
            if(center > 0.0) {
               final int min = Math.max(0, channelForEnergy(center - dlm.leftWidth(center, 0.0001)));
               final int max = Math.min(chCx - 1, channelForEnergy(center + dlm.rightWidth(center, 0.0001)));
               for(int outCh = min; outCh < max; ++outCh) {
                  final double eV = avgEnergyForChannel(outCh);
                  detFunc.set(ch, outCh, dlm.compute(eV, center));
               }
            } else
               detFunc.set(ch, ch, 1.0);
         }
         final SingularValueDecomposition svdDF = detFunc.svd();
         final double[] s = svdDF.getSingularValues();
         double maxS = 0.0;
         for(final double element : s)
            if(element > maxS)
               maxS = element;
         final double EPS = 1.0e-12;
         final Matrix ss = (new Matrix(svdDF.getS().getArrayCopy())).transpose();
         for(int i = 0; i < ss.getRowDimension(); ++i)
            ss.set(i, i, s[i] > EPS * maxS ? 1.0 / s[i] : 0.0);
         final Matrix u = svdDF.getU(), v = svdDF.getV();
         mPseudoInverse = v.times(ss).times(u.transpose());
      }
      return mPseudoInverse;
   }

   public ISpectrumData superResolve(final ISpectrumData spec) {
      final EditableSpectrum es = new EditableSpectrum(spec);
      final Matrix pInv = inverseDetectorFunction();
      final double[] data = Arrays.copyOf(es.getCounts(), pInv.getColumnDimension());
      final Matrix specData = new Matrix(data, data.length);
      final Matrix res = pInv.times(specData);
      final double[] chData = es.getCounts();
      Arrays.fill(chData, 0.0);
      for(int i = 0; i < Math.min(res.getRowDimension(), data.length); ++i)
         if(!Double.isNaN(res.get(i, 0)))
            chData[i] = res.get(i, 0);
      return SpectrumUtils.copy(es);
   }

}
