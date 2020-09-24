package gov.nist.microanalysis.EPQLibrary;

import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.BremsstrahlungAnalytic.QuadraticBremsstrahlung;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSCalibration;
import gov.nist.microanalysis.EPQLibrary.Detector.SDDCalibration;
import gov.nist.microanalysis.Utility.HTMLFormat;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * Description
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
public class SpectrumFitResult {

   private static class TransitionResult {
      private final UncertainValue2 mIntegratedIntensity;
      private final UncertainValue2 mPeakIntensity;
      private final UncertainValue2 mGaussianWidth;
      private final UncertainValue2 mPosition;

      private TransitionResult(UncertainValue2 intIntensity, UncertainValue2 peakIntensity, UncertainValue2 gWidth, UncertainValue2 ch) {
         mIntegratedIntensity = intIntensity;
         mPeakIntensity = peakIntensity;
         mGaussianWidth = gWidth;
         mPosition = ch;
      }

      @Override
      public String toString() {
         return "Iint=" + mIntegratedIntensity.toString() + ", Ip=" + mPeakIntensity.toString() + ", w="
               + mGaussianWidth.toString() + ", " + mPosition.toString();
      }
   }

   static private final double DEFAULT_FANO = 0.122;

   private final ISpectrumData mUnknown;
   private final Composition mComposition;

   private final TreeMap<XRayTransition, TransitionResult> mResults;
   private UncertainValue2[] mEnergyCalibration;
   private String[] mEnergyParams;
   private String[] mEnergyParamUnits;
   private UncertainValue2 mFano;
   private UncertainValue2 mNoise;
   private UncertainValue2 mFWHMatMnKa;
   private BremsstrahlungAnalytic mBremModel;
   private UncertainValue2 mBremA;
   private UncertainValue2 mBremB;
   private ISpectrumData mBremSpec;

   public EDSCalibration getCalibration() {
      return new SDDCalibration(mEnergyCalibration[1].doubleValue(), mEnergyCalibration[0].doubleValue(), (mEnergyParams.length > 2)
            && (mEnergyParams[2].equals("Quadratic")) ? mEnergyCalibration[2].doubleValue()
                  : 0.0, mFano.doubleValue(), mNoise.doubleValue());
   }

   public SpectrumFitResult(ISpectrumData unk, Composition comp) {
      mResults = new TreeMap<XRayTransition, TransitionResult>();
      mEnergyCalibration = new UncertainValue2[3];
      mEnergyCalibration[0] = new UncertainValue2(unk.getZeroOffset());
      mEnergyCalibration[1] = new UncertainValue2(unk.getChannelWidth());
      mEnergyCalibration[2] = UncertainValue2.ZERO;
      mFano = new UncertainValue2(DEFAULT_FANO);
      final double DEFAULT_FWHM = 130.0;
      mNoise = new UncertainValue2(SpectrumUtils.noiseFromResolution(DEFAULT_FANO, SpectrumUtils.fwhmToGaussianWidth(DEFAULT_FWHM), SpectrumUtils.E_MnKa));
      mFWHMatMnKa = new UncertainValue2(DEFAULT_FWHM);
      mUnknown = unk;
      mComposition = comp;
   }

   public void setResolution(UncertainValue2 fano, UncertainValue2 noise) {
      mFano = fano;
      mNoise = noise;
      mFWHMatMnKa = SpectrumUtils.gaussianWidthToFWHM(SpectrumUtils.resolutionU(fano, noise, SpectrumUtils.E_MnKa));
   }

   public void setResolution(UncertainValue2 fwhm, double energy_eV) {
      mFano = new UncertainValue2(0.122);
      mNoise = SpectrumUtils.noiseFromResolution(new UncertainValue2(DEFAULT_FANO), SpectrumUtils.fwhmToGaussianWidth(fwhm), energy_eV);
      mFWHMatMnKa = SpectrumUtils.resolutionU(mFano, mNoise, SpectrumUtils.E_MnKa);
   }

   public void setEnergyCalibration(UncertainValue2[] calib, String[] names, String[] units) {
      mEnergyCalibration = new UncertainValue2[calib.length];
      mEnergyParams = new String[calib.length];
      mEnergyParamUnits = new String[calib.length];
      for(int i = 0; i < calib.length; ++i) {
         mEnergyCalibration[i] = calib[i].clone();
         mEnergyParams[i] = names[i];
         mEnergyParamUnits[i] = units[i];
      }
   }

   public void setBremstrahlungModel(BremsstrahlungAnalytic ba) {
      mBremModel = ba;
      if(mBremModel instanceof QuadraticBremsstrahlung) {
         final QuadraticBremsstrahlung qb = (QuadraticBremsstrahlung) mBremModel;
         mBremA = new UncertainValue2(qb.mA);
         mBremB = new UncertainValue2(qb.mB);
      }
   }

   public ISpectrumData getBremsstrahlungSpectrum() {
      return mBremSpec;
   }

   public void setBremsstrahlungSpectrum(ISpectrumData s) {
      mBremSpec = s;
   }

   public void setBremsstrahlungModel(BremsstrahlungAnalytic ba, UncertainValue2 bA, UncertainValue2 bB) {
      setBremstrahlungModel(ba);
      mBremA = bA;
      mBremB = bB;
   }

   void addTransition(XRayTransition xrt, UncertainValue2 peakI, UncertainValue2 intI, UncertainValue2 gaussianW, UncertainValue2 chPos) {
      mResults.put(xrt, new TransitionResult(intI, peakI, gaussianW, chPos));
   }

   public UncertainValue2 getIntegratedIntensity(XRayTransition xrt) {
      final TransitionResult tr = mResults.get(xrt);
      return tr != null ? tr.mIntegratedIntensity : UncertainValue2.ZERO;
   }

   public UncertainValue2 getIntegratedIntensity(XRayTransitionSet xrts) {
      double sum = 0.0;
      for(final XRayTransition xrt : xrts)
         sum += getIntegratedIntensity(xrt).doubleValue();
      return new UncertainValue2(sum, "S", Math.sqrt(Math.abs(sum)));
   }

   public UncertainValue2 getGaussianWidth(XRayTransition xrt) {
      final TransitionResult tr = mResults.get(xrt);
      return tr != null ? tr.mGaussianWidth : UncertainValue2.ZERO;
   }

   public UncertainValue2 getPosition(XRayTransition xrt) {
      final TransitionResult tr = mResults.get(xrt);
      return tr != null ? tr.mPosition : UncertainValue2.ZERO;
   }

   public XRayTransitionSet getFitTransitions(Element elm) {
      final XRayTransitionSet res = new XRayTransitionSet();
      for(final XRayTransition xrt : mResults.keySet())
         if(xrt.getElement().equals(elm))
            res.add(xrt);
      return res;
   }

   public Set<XRayTransition> getTransitions() {
      final Set<XRayTransition> res = new TreeSet<XRayTransition>();
      for(final XRayTransition xrt : mResults.keySet())
         res.add(xrt);
      return res;
   }

   /**
    * Tabulate the results without a correction for differences in materials.
    * 
    * @return String
    */
   public String tabulateResults() {
      return tabulateResults(null);
   }

   /**
    * tabulateResults with the reported relative intensities corrected for
    * differences in ZAF correction.
    * 
    * @param props SpectrumProperties as required for a ZAF correction
    * @return String
    */
   public String tabulateResults(SpectrumProperties props) {
      final StringBuffer res = new StringBuffer();
      final Set<Element> elms = new TreeSet<Element>();
      for(final XRayTransition xrt : mResults.keySet())
         elms.add(xrt.getElement());
      final NumberFormat nf = new DecimalFormat("0.0000E0");
      nf.setRoundingMode(RoundingMode.HALF_UP);
      final boolean zafCorrect = (props != null);
      for(final Element elm : elms) {
         if(res.length() > 0)
            res.append("\n");
         final Composition pureElm = new Composition(elm);
         final double[] maxI = new double[(AtomicShell.NFamily - AtomicShell.KFamily) + 1];
         final double[] intI = new double[XRayTransition.Last];
         for(final Map.Entry<XRayTransition, TransitionResult> me : mResults.entrySet()) {
            final XRayTransition xrt = me.getKey();
            final TransitionResult tres = me.getValue();
            if(xrt.getElement().equals(elm)) {
               double kr = 1.0;
               if(zafCorrect)
                  try {
                     kr = CorrectionAlgorithm.XPP.kratio(pureElm, mComposition, xrt, props).doubleValue();
                  }
                  catch(final EPQException e) {
                     e.printStackTrace();
                  }
               final int idx = xrt.getFamily() - AtomicShell.KFamily;
               intI[xrt.getTransitionIndex()] = tres.mIntegratedIntensity.doubleValue() / kr;
               maxI[idx] = Math.max(intI[xrt.getTransitionIndex()], maxI[idx]);
            }
         }
         res.append("\"");
         res.append(mUnknown.toString());
         res.append("\", \"");
         res.append(DateFormat.getDateTimeInstance().format(new Date()));
         res.append("\", \"");
         res.append(mComposition.toString());
         res.append("\" , \"");
         res.append(elm.toAbbrev());
         res.append("\" , ");
         res.append(elm.getAtomicNumber());
         res.append(", ");
         res.append("\"I\"");
         for(final double max : maxI) {
            res.append(", ");
            res.append(nf.format(max));
         }
         res.append(zafCorrect ? ", \"KW\"" : ", \"W\"");
         for(final int tr : XRayTransition.ALL_TRANSITIONS) {
            res.append(", ");
            final int idx = XRayTransition.getFamily(tr) - AtomicShell.KFamily;
            res.append(maxI[idx] > 0.0 ? nf.format(intI[tr] / maxI[idx]) : "0");
         }

      }
      return res.toString();
   }

   public String format3Col(UncertainValue2 uv, NumberFormat nf) {
      final StringBuffer sb = new StringBuffer();
      sb.append("<TD>");
      sb.append(nf.format(uv.doubleValue()));
      final double u = uv.uncertainty();
      sb.append(u > 0.0 ? "</TD><TD>&plusmn;</TD><TD>" : "</TD><TD></TD><TD>");
      sb.append(u > 0.0 ? nf.format(u) : "--");
      sb.append("</TD>");
      return sb.toString();
   }

   /**
    * Summarize the results from this fit as HTML.
    * 
    * @return A String containing HTML
    */
   public String toHTML() {
      final StringBuffer sb = new StringBuffer(4096);
      sb.append("<p>");
      if((mUnknown != null) && (mComposition != null)) {
         sb.append("<H2>");
         sb.append(mUnknown.toString() + " fit as " + mComposition.toString());
         sb.append("</H2>\n");
      }
      sb.append(fitToHTML());
      sb.append(integratedIntensityToHTML());
      sb.append(energyCalibrationToHTML());
      sb.append(bremToHTML());
      sb.append(resolutionToHTML());
      sb.append("</p>");
      return sb.toString();
   }

   private String fitToHTML() {
      final Strategy strat = new Strategy();
      strat.addAlgorithm(TransitionEnergy.class, TransitionEnergy.Default);
      AlgorithmUser.applyGlobalOverride(strat);
      try {
         final StringBuffer sb = new StringBuffer();
         sb.append("<TABLE>\n");
         sb.append("<TR><TH>Transition</TH><TH colspan=3>Intensity</TH><TH colspan=4>Relative</TH><TH colspan=3>Gaussian<br>Width<br>(eV)</TH><TH colspan=3>FWHM<br>(eV)</TH><TH colspan=3>Channel</TH><TH>Energy<br>(eV)</TH></TR>\n");
         final NumberFormat iFmt = new HalfUpFormat("#,###,###,##0");
         final NumberFormat fFmt = new HalfUpFormat("#,###,###,##0.0");
         final NumberFormat nFmt = new HalfUpFormat("0.000000");
         for(final Map.Entry<XRayTransition, TransitionResult> me : mResults.entrySet()) {
            final TransitionResult tr = me.getValue();
            final UncertainValue2 gw = tr.mGaussianWidth;
            final XRayTransition xrt = me.getKey();
            final XRayTransition xrt2 = XRayTransition.getBrightestTransition(xrt.getElement(), xrt.getFamily());
            final TransitionResult tr2 = xrt2 != null ? mResults.get(xrt2) : null;
            sb.append("<TR><TH ALIGN=\"LEFT\">");
            sb.append(xrt.toString());
            sb.append("</TH>");
            sb.append(format3Col(tr.mIntegratedIntensity, iFmt));
            if(tr2 != null) {
               sb.append(format3Col(UncertainValue2.divide(tr.mIntegratedIntensity, tr2.mIntegratedIntensity), nFmt));
               sb.append("<TD>" + xrt2.toString() + " - " + nFmt.format(xrt.getWeight(XRayTransition.NormalizeKLM)) + "</TD>");
            } else
               sb.append("<TD>--</TD><TD>\u0177</TD><TD>--</TD><TD>--</TD>");
            sb.append(format3Col(tr.mGaussianWidth, fFmt));
            sb.append(format3Col(SpectrumUtils.gaussianWidthToFWHM(gw), fFmt));
            sb.append(format3Col(tr.mPosition, fFmt));
            sb.append("<TD>");
            try {
               sb.append(iFmt.format(FromSI.eV(xrt.getEnergy())));
            }
            catch(final EPQException e) {
               sb.append("?");
            }
            sb.append("</TD></TR>\n");
         }
         sb.append("</TABLE><BR>");
         return sb.toString();
      }
      finally {
         AlgorithmUser.clearGlobalOverride();
      }
   }

   private String integratedIntensityToHTML() {
      final StringBuffer sb = new StringBuffer();
      if(mComposition != null) {
         final NumberFormat iFmt = new HalfUpFormat("#,###,###,##0");
         final String[] xtrsNames = new String[] {
            XRayTransitionSet.K_FAMILY,
            XRayTransitionSet.K_ALPHA,
            XRayTransitionSet.K_BETA,
            XRayTransitionSet.L_FAMILY,
            XRayTransitionSet.L_ALPHA,
            XRayTransitionSet.L_BETA,
            XRayTransitionSet.L_GAMMA,
            XRayTransitionSet.M_FAMILY,
            XRayTransitionSet.M_ALPHA,
            XRayTransitionSet.M_BETA,
            XRayTransitionSet.M_GAMMA
         };
         sb.append("<TR><TH ALIGN=\"LEFT\">Element</TH><TH>Family</TH><TH colspan=3>Intensity</TH></TR>\n");
         for(final Element elm : mComposition.getElementSet())
            for(final String name : xtrsNames) {
               final XRayTransitionSet xrts = new XRayTransitionSet(elm, name);
               final UncertainValue2 uv = this.getIntegratedIntensity(xrts);
               if(uv.doubleValue() > 0.0) {
                  sb.append("<TR><TH ALIGN=\"LEFT\">");
                  sb.append(elm);
                  sb.append("</TH><TH>");
                  sb.append(name);
                  sb.append("</TH>");
                  sb.append(format3Col(uv, iFmt));
                  sb.append("</TR>\n");
               }
            }
         sb.append("</TABLE><BR>");
      }
      return sb.toString();
   }

   private String energyCalibrationToHTML() {
      final StringBuffer sb = new StringBuffer();
      if((mEnergyCalibration[1] != null) || (mEnergyCalibration[0] != null)) {
         sb.append("<H3>Energy Calibration</H3>");
         final NumberFormat nf = new HalfUpFormat("#,###,###,##0.0000");
         sb.append("<TABLE><TR><TH>Item</TH><TH colspan=3>Quantity</TH><TH>Unit</TH></TR>");
         for(int i = 0; i < mEnergyCalibration.length; ++i) {
            sb.append("<TR><TD>");
            sb.append(mEnergyParams[i]);
            sb.append("</TD>");
            sb.append(i < 2 ? format3Col(mEnergyCalibration[i], nf)
                  : format3Col(mEnergyCalibration[i], new HTMLFormat("0.000")));
            sb.append("<TD>");
            sb.append(mEnergyParamUnits[i]);
            sb.append("</TD></TR>");
         }
         sb.append("</TABLE><BR>\n");
      }
      return sb.toString();
   }

   private String bremToHTML() {
      final StringBuffer sb = new StringBuffer();
      if((mBremA != null) && (mBremB != null)) {
         final NumberFormat fFmt = new HalfUpFormat("#,###,###,##0.0");
         sb.append("<H3>Bremsstrahlung</H3>");
         sb.append("<TABLE><TR><TH>Linear</TH><TH>Quadratic</TH></TR>\n");
         sb.append("<TR><TD>");
         sb.append(mBremA.format(fFmt));
         sb.append("</TD><TD>");
         sb.append(mBremB.format(fFmt));
         sb.append("</TD></TR></TABLE></P>\n");
      }
      return sb.toString();
   }

   public String resolutionToHTML() {
      final StringBuffer sb = new StringBuffer();
      if((mFano != null) || (mNoise != null) || (mFWHMatMnKa != null)) {
         final NumberFormat fFmt = new HalfUpFormat("#,###,###,##0.0");
         final NumberFormat fFmt4 = new HalfUpFormat("#,###,###,##0.0000");
         sb.append("<H3>Detector Resolution</H3>");
         sb.append("<TABLE><TR><TH>Fano<br>Factor</TH><TH colspan=3>Noise<br>(eV)</TH><TH colspan=3>FWHM at<br>Mn K\u03B1<br>(eV)</TH></TR>\n");
         sb.append("<TR>");
         sb.append(mFano != null ? mFano.format(fFmt4) : "<TD>-</TD>");
         sb.append(mNoise != null ? format3Col(mNoise, fFmt4) : "<TD>-</TD>");
         sb.append(mFWHMatMnKa != null ? format3Col(mFWHMatMnKa, fFmt) : "<TD>-</TD>");
         sb.append("</TR></TABLE>");
      }
      return sb.toString();

   }

   public String toTransitionString(boolean withHeader) {
      final StringBuffer sb = new StringBuffer();
      final Set<Element> elms = new TreeSet<Element>();
      for(final XRayTransition xrt : mResults.keySet())
         elms.add(xrt.getElement());
      if(withHeader) {
         sb.append("Z");
         for(int tr = XRayTransition.KA1; tr < XRayTransition.N4N6; ++tr) {
            sb.append("\t");
            sb.append("I[" + XRayTransition.getIUPACName(tr) + "]");
            sb.append("\t");
            sb.append("dI[" + XRayTransition.getIUPACName(tr) + "]");
         }
         sb.append("\n");
      }
      final NumberFormat nf = new HalfUpFormat("#,###,###,##0.0");
      for(final Element elm : elms) {
         sb.append(elm.getAtomicNumber());
         for(int tr = XRayTransition.KA1; tr < XRayTransition.N4N6; ++tr) {
            final XRayTransition xrt = new XRayTransition(elm, tr);
            final TransitionResult res = mResults.get(xrt);
            if(res != null) {
               final UncertainValue2 uv = res.mIntegratedIntensity;
               sb.append("\t");
               sb.append(nf.format(uv.doubleValue()));
               sb.append("\t");
               sb.append(nf.format(uv.uncertainty()));
            } else
               sb.append("\t-\t-");
         }
         sb.append("\n");
      }
      return sb.toString();
   }

   public UncertainValue2 getChannelWidth() {
      return mEnergyCalibration[1];
   }

   public UncertainValue2 getZeroOffset() {
      return mEnergyCalibration[0];
   }

   public UncertainValue2 getFanoFactor() {
      return mFano;
   }

   public UncertainValue2 getNoise() {
      return mNoise;
   }

   public UncertainValue2 getFWHMatMnKa() {
      return mFWHMatMnKa;
   }

   public ISpectrumData getSubSpectrum(Collection<XRayTransition> xrts, boolean withTail) {
      final EditableSpectrum res = new EditableSpectrum(mUnknown);
      SpectrumUtils.rename(res, "Fit[" + mUnknown.toString() + "," + mComposition.toString() + "]");
      final double[] data = res.getCounts();
      Arrays.fill(data, 0.0);
      for(final XRayTransition xrt : xrts) {
         final TransitionResult tr = mResults.get(xrt);
         if(tr != null)
            try {
               final double e0 = FromSI.eV(xrt.getEnergy());
               final double sigma = tr.mGaussianWidth.doubleValue(); // in eV
               final double p = tr.mPosition.doubleValue(); // in channels
               final double g = tr.mPeakIntensity.doubleValue(); // in counts
               final int lowCh = Math2.bound((int) (p
                     - ((5.0 * sigma) / mEnergyCalibration[1].doubleValue())), 0, res.getChannelCount());
               final int highCh = Math2.bound((int) (p + ((5.0 * sigma) / mEnergyCalibration[1].doubleValue()))
                     + 1, 0, res.getChannelCount());
               for(int ch = lowCh; ch < highCh; ++ch) {
                  final double e = (ch * mEnergyCalibration[1].doubleValue()) + mEnergyCalibration[0].doubleValue();
                  data[ch] += g * Math.exp(-0.5 * Math2.sqr((e - e0) / sigma));
               }
            }
            catch(final EPQException e) {
               e.printStackTrace();
            }
      }
      return res;
   }

   public ISpectrumData getSubSpectrum(Element elm, boolean withTail) {
      final ArrayList<XRayTransition> xrts = new ArrayList<XRayTransition>();
      for(final XRayTransition xrt : mResults.keySet())
         if(xrt.getElement().equals(elm))
            xrts.add(xrt);
      return getSubSpectrum(xrts, withTail);
   }

   public ISpectrumData getSubSpectrum(Set<Element> elms, boolean withTail) {
      final ArrayList<XRayTransition> xrts = new ArrayList<XRayTransition>();
      for(final XRayTransition xrt : mResults.keySet())
         for(Element elm : elms)
            if(xrt.getElement().equals(elm))
               xrts.add(xrt);
      return getSubSpectrum(xrts, withTail);
   }

   /**
    * Returns the characteristic portion of the fit spectrum.
    * 
    * @return ISpectrumData
    */

   public ISpectrumData getFitSpectrum() {
      return getSubSpectrum(mResults.keySet(), true);
   }

   /**
    * Returns the characteristic portion of the fit spectrum.
    * 
    * @param withBrem - With bremsstrahlung added in (if available)
    * @return ISpectrumData
    */

   public ISpectrumData getFitSpectrum(boolean withBrem) {
      final SpectrumMath res = new SpectrumMath(getSubSpectrum(mResults.keySet(), true));
      if(withBrem && (mBremSpec != null))
         res.add(mBremSpec, 1.0);
      return res;
   }

   /**
    * Returns the difference between the original spectrum and the fit.
    * 
    * @return ISpectrumData
    */
   public ISpectrumData getResidual() {
      final SpectrumMath sm = new SpectrumMath(mUnknown);
      sm.subtract(getFitSpectrum(), 1.0);
      return SpectrumUtils.copy(sm);
   }

   /**
    * Returns the original fit spectrum.
    * 
    * @return ISpectrumData
    */
   public ISpectrumData getUnknown() {
      return mUnknown;
   }
}
