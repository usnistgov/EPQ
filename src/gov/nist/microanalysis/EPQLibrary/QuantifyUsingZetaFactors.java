package gov.nist.microanalysis.EPQLibrary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.EPQTools.EMSAFile;
import gov.nist.microanalysis.EPQTools.WriteSpectrumAsEMSA1_0;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Pair;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * Description
 * </p>
 *
 * @author Nicholas
 * @version 1.0
 */
public class QuantifyUsingZetaFactors {

   private final EDSDetector mDetector;
   private final double mBeamEnergy;
   private final Map<Element, ISpectrumData> mStandards = new TreeMap<Element, ISpectrumData>();
   private final Map<Element, ISpectrumData> mStrip = new TreeMap<Element, ISpectrumData>();
   private final ZetaFactor mAlg = new ZetaFactor();

   /**
    * Constructs a QuantifyUsingZetaFactors
    *
    * @param det EDSDetector
    * @param beamEnergy in Joules
    */
   public QuantifyUsingZetaFactors(final EDSDetector det, final double beamEnergy) {
      mDetector = det;
      mBeamEnergy = beamEnergy;
   }

   /**
    * Assigns a standard spectrum associated with the specified element. The
    * standard spectrum must have the composition property, the live time, the
    * sample current, the sample shape (as a thin film) and specimen density
    * properties defined. (StandardComposition, LiveTime, FaradayBegin,
    * SampleShape, SpecimenDensity)
    *
    * @param elm
    * @param spec
    * @throws EPQException
    */
   public void assignStandard(final Element elm, ISpectrumData spec)
         throws EPQException {
      spec = preProcessSpectrum(spec);
      final SpectrumProperties sp = spec.getProperties();
      final Composition comp = sp.getCompositionProperty(SpectrumProperties.StandardComposition);
      if(!comp.containsElement(elm))
         throw new EPQException("The standard does not contain " + elm.toString() + ".");
      sp.getNumericProperty(SpectrumProperties.LiveTime);
      sp.getNumericProperty(SpectrumProperties.FaradayBegin);
      if(Double.isNaN(SpectrumUtils.getMassThickness(sp)))
         throw new EPQException("The sample must define the mass-thickness.");
      final double e0 = ToSI.keV(sp.getNumericWithDefault(SpectrumProperties.BeamEnergy, FromSI.keV(mBeamEnergy)));
      if(Math.abs((e0 - mBeamEnergy) / mBeamEnergy) > 0.01)
         throw new EPQException("The standard was not collected at the correct beam energy.");
      if(mStrip.containsKey(elm))
         mStrip.remove(elm);
      mStandards.put(elm, spec);
   }

   /**
    * Assigns a reference spectrum to use to strip any characteristic peaks for
    * the specified element.
    *
    * @param elm Element
    * @param spec A reference containing unobstructed views of the
    *           characteristic peak associated with elm.
    */
   public void assignStrip(final Element elm, final ISpectrumData spec) {
      if(mStandards.containsKey(elm))
         mStandards.remove(elm);
      mStrip.put(elm, preProcessSpectrum(spec));
   }

   public class Result
      extends Pair<Number, Composition> {

      private final ISpectrumData mUnknown;
      private final ISpectrumData mResidual;

      private Result(Pair<Number, Composition> res, ISpectrumData unk, ISpectrumData residual) {
         super(res.first, res.second);
         mUnknown = unk;
         mResidual = residual;
      }

      public Number getMassThickness() {
         return first;
      }

      public Composition getComposition() {
         return second;
      }

      public ISpectrumData getUnknown() {
         return mUnknown;
      }

      public ISpectrumData getResidual() {
         return mResidual;
      }

   }

   private ISpectrumData preProcessSpectrum(final ISpectrumData spec) {
      return SpectrumUtils.applyZeroPeakDiscriminator(SpectrumUtils.applyEDSDetector(getDetector(), spec));
   }

   /**
    * Fits the unknown spectrum using the standards and strip spectra.
    * Quantifies the k-ratios associated with the standard spectra using the
    * ZetaFactor algorithm. Returns the estimated mass thickness and estimated
    * composition of the thin film.
    *
    * @param unk The unknown spectrum
    * @param withAbsorption Whether to apply the absorption correction.
    * @return Pair&lt;Number, Composition&gt;
    * @throws EPQException
    */
   public Result compute(final ISpectrumData unk, boolean withAbsorption)
         throws EPQException {
      final ISpectrumData dup = preProcessSpectrum(unk);
      final FilterFit ff = new FilterFit(mDetector, mBeamEnergy);
      for(final Map.Entry<Element, ISpectrumData> me : mStandards.entrySet())
         ff.addReference(me.getKey(), me.getValue());
      for(final Map.Entry<Element, ISpectrumData> me : mStrip.entrySet())
         ff.addReference(me.getKey(), me.getValue());
      final KRatioSet kr = new KRatioSet();
      final SpectrumProperties unkProps = unk.getProperties();
      {
         final KRatioSet all = ff.getKRatios(dup);
         unkProps.setKRatioProperty(SpectrumProperties.KRatios, all);
         for(final XRayTransitionSet xrts : all.getTransitions())
            if(mStandards.containsKey(xrts.getElement()))
               kr.addKRatio(xrts, all.getKRatioU(xrts));
      }
      final Map<XRayTransitionSet, Number> measurements = new HashMap<XRayTransitionSet, Number>();
      for(final XRayTransitionSet xrts : kr.getTransitions()) {
         final Element elm = xrts.getElement();
         final ISpectrumData std = mStandards.get(elm);
         final Material stdMat = (Material) std.getProperties().getCompositionProperty(SpectrumProperties.StandardComposition);
         final double dose = 1.0; // Dose already handled in k-ratios
         final double massThickness = ToSI.ugPcm2(SpectrumUtils.getMassThickness(std.getProperties())).doubleValue();
         final double toa = SpectrumUtils.getTakeOffAngle(std.getProperties());
         if(withAbsorption)
            mAlg.addStandard(xrts, mAlg.computeZeta(xrts, stdMat, UncertainValue2.ONE, dose, massThickness, toa));
         else
            mAlg.addStandard(xrts, mAlg.computeZeta(massThickness, UncertainValue2.ONE, stdMat.weightFraction(elm, false), dose));
         measurements.put(xrts, kr.getKRatioU(xrts));
      }
      final double unkToa = SpectrumUtils.getTakeOffAngle(unkProps);
      final double unkDose = 1.0; // Dose already handled in k-ratios
      final Pair<Number, Composition> tmp = withAbsorption ? mAlg.compute(measurements, unkDose, unkToa)
            : mAlg.compute(measurements, unkDose);
      final Result res = new Result(tmp, unk, ff.getResidualSpectrum(unk));
      unkProps.setCompositionProperty(SpectrumProperties.MicroanalyticalComposition, tmp.second);
      KRatioSet optimal = new KRatioSet();
      for(XRayTransitionSet xrts : mAlg.getOptimal())
         optimal.addKRatio(xrts, kr.getKRatio(xrts));
      unkProps.setKRatioProperty(SpectrumProperties.OptimalKRatios, optimal);
      unkProps.setNumericProperty(SpectrumProperties.MassThickness, FromSI.ugPcm2(res.getMassThickness()));
      return res;
   }

   public EDSDetector getDetector() {
      return mDetector;
   }

   public double getBeamEnergy() {
      return mBeamEnergy;
   }

   /**
    * Tabulate quantitative results.
    * 
    * @param quantifiedSpectra
    * @param parentPath
    * @param extResults
    * @return String
    */
   public String tabulateResults(List<ISpectrumData> quantifiedSpectra, File parentPath, Collection<ISpectrumData> extResults) {
      final StringWriter sw = new StringWriter(4096);
      final PrintWriter pw = new PrintWriter(sw);
      final NumberFormat nf1 = new HalfUpFormat("#0.0");
      final NumberFormat nf2 = new HalfUpFormat("0.0000");
      final NumberFormat nf3 = new HalfUpFormat("#0.000");
      final NumberFormat nf4 = new HalfUpFormat("#0.0000");
      pw.println("<TABLE>");
      // Header row
      Set<Element> elms = mStandards.keySet();
      pw.print("<tr><th>Spectrum</th><th>Quantity</th>");
      for(final Element el : elms) {
         pw.print("<TH COLSPAN=3 ALIGN=CENTER>");
         pw.print(el.toAbbrev());
         pw.print("</TH>");
      }
      pw.print("<th>Sum</th>");
      pw.print("</tr>");
      boolean first = true;
      for(ISpectrumData spec : quantifiedSpectra) {
         // Separator line between spectra
         final Composition comp = spec.getProperties().getCompositionWithDefault(SpectrumProperties.MicroanalyticalComposition, null);
         if(comp == null)
            continue;
         boolean boldNorm = false;
         if(!first) {
            pw.print("<tr><td colspan = ");
            pw.print(3 + (3 * elms.size()) + 1);
            pw.print("</td></tr>");
            first = false;
         }
         final SpectrumProperties specProps = spec.getProperties();
         final KRatioSet optKrs = specProps.getKRatioWithDefault(SpectrumProperties.OptimalKRatios, null);
         final KRatioSet measKrs = specProps.getKRatioWithDefault(SpectrumProperties.KRatios, null);
         pw.print("<tr><th rowspan = 2>");
         pw.print(specProps.asURL(spec));
         pw.print("</th>");
         // Characteristic line family
         pw.print("<td>Line</td>");
         for(final Element elm : elms) {
            final XRayTransitionSet xrts = optKrs != null ? optKrs.optimalDatum(elm) : null;
            if(xrts != null) {
               pw.print("<TD COLSPAN = 3 ALIGN=CENTER>");
               pw.print(xrts);
               pw.print("</TD>");
            } else
               pw.print("<TD COLSPAN = 3 ALIGN=CENTER>WFT</TD>");
         }
         pw.println("<TD></TD></TR>");
         // k-ratios
         pw.print("<tr><td>k-ratios</td>");
         for(final Element elm : elms) {
            final XRayTransitionSet xrts = optKrs != null ? optKrs.optimalDatum(elm) : null;
            if(xrts != null) {
               pw.print("<TD ALIGN=RIGHT>");
               pw.print(nf4.format(measKrs.getRawKRatio(xrts)));
               pw.print("</TD><TD align=center>\u00B1</TD><TD ALIGN=LEFT>");
               pw.print(nf4.format(measKrs.getError(xrts)));
               pw.print("</TD>");
            } else
               pw.print("<TD>-</TD><TD>-</TD><TD>-</TD>");
         }
         pw.println("<TD></TD></TR>");
         // wgt%
         pw.println("<TR><th>");
         {
            final Number mth = specProps.getNumericWithDefault(SpectrumProperties.MassThickness, null);
            if(mth != null)
               pw.print("<br>Mass-thickness<br>" + UncertainValue2.format(nf1, mth) + " &mu;g/cm<sup>2</sup></TD>");
            else
               pw.print("<br>?WFT?");
         }
         pw.print("</th><td>mass fraction</td>");
         for(final Element elm : elms) {
            pw.print(((!boldNorm) ? "<TH" : "<TD") + " ALIGN=RIGHT>");
            final UncertainValue2 wf = comp.weightFractionU(elm, false);
            pw.print(nf2.format(wf.doubleValue()));
            pw.print((!boldNorm) ? "</TH>" : "</TD>");
            pw.print("<TD align=center>\u00B1</TD><TD ALIGN=LEFT>");
            boolean first2 = true;
            for(final String name : wf.getComponentNames()) {
               if(!first2)
                  pw.print("<br/>");
               pw.print("<nobr>");
               pw.print(wf.formatComponent(name, 5));
               pw.print("</nobr>");
               first2 = false;
            }
            pw.print("<br/>[" + nf2.format(wf.uncertainty()) + "]");
            pw.print("</TD>");
         }
         pw.print("<TD>");
         pw.print(nf2.format(comp.sumWeightFraction()));
         pw.print("</TD>");
         pw.println("</TR>");
         // norm(wgt%)
         pw.print("<TR>");
         pw.print("<TD align=\"right\">I = " + nf3.format(SpectrumUtils.getAverageFaradayCurrent(specProps, Double.NaN))
               + " nA</TD>");
         pw.print("<td>norm(mass<br/>fraction)</td>");
         for(final Element elm : elms) {
            final UncertainValue2 nwf = comp.weightFractionU(elm, true);
            pw.print((boldNorm ? "<TH" : "<TD") + " ALIGN=RIGHT>");
            pw.print(nf2.format(nwf.doubleValue()));
            pw.print(boldNorm ? "</TH>" : "</TD>");
            pw.print("<TD align=center>\u00B1</TD><TD ALIGN=LEFT>");
            if(nwf.uncertainty() > 0.0)
               pw.print(nf2.format(nwf.uncertainty()));
            pw.print("</TD>");
         }
         pw.print("<TD> - </TD>");
         pw.println("</TR>");
         // atomic %
         pw.print("<TR>");
         pw.print("<TD align=\"right\">LT = "
               + nf1.format(specProps.getNumericWithDefault(SpectrumProperties.LiveTime, Double.NaN)) + " s</TD>");
         pw.print("<td>atomic<br/>fraction</td>");
         for(final Element elm : elms) {
            final UncertainValue2 ap = comp.atomicPercentU(elm);
            pw.print((boldNorm ? "<TH" : "<TD") + " ALIGN=RIGHT>");
            pw.print(nf2.format(ap.doubleValue()));
            pw.print(boldNorm ? "</TH>" : "</TD>");
            pw.print("<TD align=center>\u00B1</TD><TD ALIGN=LEFT>");
            if(ap.uncertainty() > 0.0)
               pw.print(nf2.format(ap.uncertainty()));
            pw.print("</TD>");
         }
         pw.println("<TD></TD></TR>");
         if(extResults != null) {
            ISpectrumData res = null;
            for(final ISpectrumData rs : extResults)
               if(rs instanceof DerivedSpectrum)
                  if((((DerivedSpectrum) rs).getBaseSpectrum() == spec) && rs.toString().startsWith("Residual")) {
                     res = rs;
                     break;
                  }
            if(res != null) {
               pw.print("<TR>");
               pw.print("<TD align=\"right\">Residual</TD>");
               pw.print("<TD COLSPAN=" + Integer.toString(1 + (3 * elms.size())) + " ALIGN=LEFT>");
               try {
                  final File f = File.createTempFile("residual", ".msa", parentPath);
                  try (final FileOutputStream fos = new FileOutputStream(f)) {
                     WriteSpectrumAsEMSA1_0.write(res, fos, WriteSpectrumAsEMSA1_0.Mode.COMPATIBLE);
                  }
                  pw.print("<A HREF=\"");
                  pw.print(f.toURI().toURL().toExternalForm());
                  pw.print("\">");
                  pw.print(f.toString());
                  pw.print("</A>");
               }
               catch(final Exception e) {
                  pw.print("Error writing the residual");
               }
               pw.println("</TD>");
               pw.println("</TR>");
            }
         }
      }
      pw.print("<TR>");
      pw.print("<TD align=\"right\">Notes</TD>");
      pw.print("<TD COLSPAN=" + Integer.toString(1 + (3 * elms.size())) + " ALIGN=LEFT>");
      pw.print("Uncertainties are 1 &sigma; and labeled by source. (K: k-ratio, MAC: mass absorption coefficient, []: combined)");
      pw.println("</TD>");
      pw.println("</TR>");
      pw.println("</TABLE>");
      return sw.toString();
   }

   public String toHTML() {
      final StringWriter sw = new StringWriter();
      final PrintWriter pw = new PrintWriter(sw);

      final NumberFormat nf1 = new HalfUpFormat("0.0");
      {
         pw.println("<h3>Conditions</h3>");
         pw.println("<table>");
         pw.println("<tr><th>Item</th><th>Value</th></tr>");
         pw.print("<tr><td>Instrument</td><td>");
         pw.print(getDetector().getOwner().toString());
         pw.println("</td></tr>");
         pw.print("<tr><td>Detector</td><td>");
         pw.print(getDetector().getName());
         pw.println("</td></tr>");
         pw.print("<tr><td>Beam Energy</td><td>");
         pw.print(nf1.format(FromSI.keV(getBeamEnergy())));
         pw.println(" keV</td></tr>");
         pw.print("<tr><td>Correction Algorithm</td><td>Watanabe's 2006 &zeta;-factors</td></tr>");
         pw.print("<tr><td>Mass Absorption<br>Coefficient</td><td>");
         final AlgorithmClass mac = mAlg.getAlgorithm(MassAbsorptionCoefficient.class);
         pw.print(mac.getName());
         pw.println("</td></tr>");
      }
      {
         final Set<Element> stripped = mStrip.keySet();
         if(stripped.size() > 0) {
            pw.print("<tr><td>Stripped elements</td><td>");
            boolean first = true;
            for(final Element elm : stripped) {
               if(!first)
                  pw.print(", ");
               pw.print(elm.toAbbrev());
               first = false;
            }
            pw.println("</td></tr>");
         }
         pw.println("</table></p>");
      }

      {
         pw.println("<h3>Standards</h3>");
         pw.println("<table>");
         pw.print("<tr><th>Element</th><th>Material</th><th>Dose</th><th>Mass-Thickness<br/>&mu;g/cm<sup>2</sup></th><th>Spectrum</th></tr>");
         for(final Map.Entry<Element, ISpectrumData> me : mStandards.entrySet()) {
            final NumberFormat nf3 = new DecimalFormat("0.000");
            final ISpectrumData spec = me.getValue();
            final SpectrumProperties props = spec.getProperties();
            final Composition comp = props.getCompositionWithDefault(SpectrumProperties.StandardComposition, null);
            pw.print("<tr><td>");
            final Element elm = me.getKey();
            pw.print(elm.toAbbrev());
            pw.print("</td><td>");
            pw.print(comp.toHTMLTable());
            pw.print("</td><td>");
            try {
               pw.print(nf3.format(SpectrumUtils.getDose(props)));
            }
            catch(EPQException e) {
               pw.print("N/A");
               e.printStackTrace();
            }
            pw.println("</td><td>");
            pw.print(nf3.format(SpectrumUtils.getMassThickness(props)));
            pw.println("</td><td>");
            pw.print(props.asURL(spec));
            pw.print("</td><td>");
            pw.println("</td>");
            pw.println("</tr>");

         }
         pw.print("</table>");
      }
      return sw.toString();
   }

   private static ISpectrumData loadResSpectrum(String name)
         throws IOException {
      ClassLoader cl = ClassLoader.getSystemClassLoader();
      try (InputStream is = cl.getResourceAsStream("gov/nist/microanalysis/EPQTests/TestData/STEM/" + name)) {
         EMSAFile res = new EMSAFile(is);
         return res;
      }
   }

   public static ISpectrumData test()
         throws IOException, EPQException {
      ISpectrumData unk = loadResSpectrum("SRM-2063a std with scatter.msa");
      EDSDetector det = EDSDetector.createSiLiDetector(2048, 10.0, 135.0);
      QuantifyUsingZetaFactors qzf = new QuantifyUsingZetaFactors(det, ToSI.keV(30.0));
      qzf.assignStandard(Element.Ar, loadResSpectrum("Ar std.msa"));
      qzf.assignStandard(Element.Ca, loadResSpectrum("CaF2 std.msa"));
      qzf.assignStandard(Element.Fe, loadResSpectrum("Fe std.msa"));
      qzf.assignStandard(Element.Mg, loadResSpectrum("MgO std.msa"));
      qzf.assignStandard(Element.O, loadResSpectrum("MgO std.msa"));
      qzf.assignStandard(Element.Si, loadResSpectrum("Si std.msa"));
      qzf.assignStrip(Element.Cu, loadResSpectrum("Cu std.msa"));
      qzf.assignStrip(Element.Ti, loadResSpectrum("Ti std.msa"));
      final Result res = qzf.compute(unk, false);
      final HalfUpFormat nf = new HalfUpFormat("0.0");
      System.out.println("MT = " + UncertainValue2.format(nf, FromSI.ugPcm2(res.first)));
      final Composition comp = res.second,
            unkComp = unk.getProperties().getCompositionProperty(SpectrumProperties.StandardComposition);
      System.out.println(comp.descriptiveString(false));
      for(Element elm : comp.getElementSet()) {
         System.out.print(elm.toAbbrev());
         System.out.print("\t");
         System.out.print(comp.weightFraction(elm, false));
         System.out.print("\t");
         System.out.print(unkComp.weightFraction(elm, false));
         System.out.print("\t");
         System.out.println(unkComp.weightFraction(elm, false) - comp.weightFraction(elm, false));
      }
      return unk;
   }
}
