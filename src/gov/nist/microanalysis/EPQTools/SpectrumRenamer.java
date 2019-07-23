package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;

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
public class SpectrumRenamer {

   public enum Rule {

      Acquired("$ACQ$", SpectrumProperties.AcquisitionTime, "Acquisition time"), //
      BeamEnergy("$E0$", SpectrumProperties.BeamEnergy, "Beam energy (keV)"), //
      ProbeCurrent("$I0$", SpectrumProperties.FaradayBegin, "Probe current (nA)"), //
      Client("$CLIENT$", SpectrumProperties.ClientName, "Client name"), //
      Spot("$SPOT$", SpectrumProperties.SpotSize, "Spot size"), //
      DeadTime("$DT$", SpectrumProperties.DeadPercent, "Dead time (%)"), //
      Detector("$DET", SpectrumProperties.Detector, "Detector"), //
      Instrument("$INST$", SpectrumProperties.Instrument, "Instrument"), //
      Operator("$OPER$", SpectrumProperties.InstrumentOperator, "Operator"), //
      LiveTime("$LT$", SpectrumProperties.LiveTime, "Live time (s)"), //
      StdComp("$COMP$", SpectrumProperties.StandardComposition, "Standard composition"), //
      OCR("$OCR$", SpectrumProperties.OutputCountRate, "Output count rate (cps)"), //
      ProbeArea("$PA$", SpectrumProperties.ProbeArea, "Probe area (nm\u00B2)"), //
      Project("$PROJ$", SpectrumProperties.ProjectName, "Project name"), //
      PPS("$PPS$", SpectrumProperties.PulseProcessorSetting, "Pulse processor setting"), //
      PPT("$PPT$", SpectrumProperties.PulseProcessTime, "Pulse processor time (µs)"), //
      RealTime("$RT$", SpectrumProperties.RealTime, "Real time (s)"), //
      Resolution("$RES$", SpectrumProperties.Resolution, "Resolution at Mn K\u03B1 (eV)"), //
      SampleShape("$SHAPE$", SpectrumProperties.SampleShape, "Sample shape"), //
      MassThickness("$MT$", SpectrumProperties.MassThickness, "Mass Thickness"), //
      SpecimenName("$SPEC$", SpectrumProperties.SpecimenName, "Sample name"), //
      Thickness("$THICK$", SpectrumProperties.SpecimenThickness, "Sample thickness (nm)"), //
      StagePos("$POS$", SpectrumProperties.StagePosition, "Stage position (mm)"), //
      Previous("$PREV$", SpectrumProperties.SpectrumDisplayName, "Previous name");

      private final String mToken;
      private SpectrumProperties.PropertyId mReplacement;
      private final String mDescription;

      Rule(String token, SpectrumProperties.PropertyId replacement, String desc) {
         mToken = token;
         mReplacement = replacement;
         mDescription = desc;
      }

      public String getDescription() {
         return mDescription;
      }

      public String getToken() {
         return mToken;
      }

      private String replace(SpectrumProperties sp, String str) {
         final int pos = str.indexOf(mToken, 0);
         if(pos >= 0)
            return str.replace(mToken, sp.getTextWithDefault(mReplacement, "Unknown"));
         else
            return str;
      }

   }

   private final String mRule;
   private int mCount;

   /**
    * Constructs a SpectrumRenamer
    */
   public SpectrumRenamer(String rule) {
      mRule = rule;
      mCount = 0;
   }

   public String computerizer(SpectrumProperties sp) {
      String res = mRule;
      for(Rule rule : Rule.values())
         res = rule.replace(sp, res);
      if(res.indexOf("$I$") >= 0) {
         res = res.replace("$I$", Integer.toString(mCount));
         ++mCount;
      }
      return res;
   }
}
