package gov.nist.microanalysis.EPQTools;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.ConductiveCoating;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SampleShape;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate.Axis;
import gov.nist.microanalysis.EPQLibrary.Detector.DetectorProperties;
import gov.nist.microanalysis.EPQLibrary.Detector.XRayWindowFactory;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Write a spectrum represented by an object implementing the ISpectrumData
 * interface to an ESMA 1.0 standard file.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author Nicholas W. M. Ritchie @version 0.1
 */

public class WriteSpectrumAsEMSA1_0 {

   public enum Mode {
      COMPATIBLE, FOR_DTSA, FOR_TIA
   }

   // Ensures that all characters fall in the restricted range permitted by the
   // standard.
   private static char restrict(char c) {
      return ((c >= 32) && (c <= 126)) ? c : '?';
   }

   static private void writeln(PrintWriter pw, String keyword, String value) {
      final StringBuffer sb = new StringBuffer("#           : ");
      sb.replace(1, keyword.length(), keyword);
      for (int p = 0; p < value.length(); ++p)
         sb.append(restrict(value.charAt(p)));
      if (keyword != "ENDOFDATA")
         pw.println(sb.toString());
      else
         pw.print(sb.toString());
   }

   public static void write(ISpectrumData spec, OutputStream os, Mode mode)
         throws EPQException {
      write(spec, os, mode, null);
   }

   public static void write(ISpectrumData spec, OutputStream os, Mode mode,
         File path) throws EPQException {
      try {
         final String oldLs = System.getProperty("line.separator");
         // Force <CR><LF> as required by the standard
         System.setProperty("line.separator", "\r\n");
         final PrintWriter pw = new PrintWriter(
               new OutputStreamWriter(os, "US-ASCII"));
         // Ensure that numbers are written in the standard US format without
         // grouping...
         final NumberFormat nf = new HalfUpFormat("#.#####;-#.#####",
               new DecimalFormatSymbols(Locale.US));
         nf.setGroupingUsed(false);
         try {
            final SpectrumProperties sp = spec.getProperties();
            // Round to next largest power of 2
            int nCh = (int) (Math.pow(2.0,
                  Math.ceil(Math.log(spec.getChannelCount()) / Math.log(2.0)))
                  + 0.5);
            if (mode == Mode.FOR_DTSA) {
               if (nCh > 8192)
                  nCh = 8192; // DTSA (often) balks at spectra larger than 8192
               // channels
            } else if (nCh > 16384)
               nCh = 16384; // Can't be larger than 2^14
            // One column makes it easier to read back into spreadsheets etc.
            final int nCols = 1;
            // string identifies this format as "EMSA/MAS Spectral Data File"
            writeln(pw, "FORMAT", "EMSA/MAS Spectral Data File"); // Character
            // File Format Version Number (1.0 for this implementation)
            writeln(pw, "VERSION", "1.0");
            // Write the #TITLE
            if (sp.isDefined(SpectrumProperties.SpectrumComment)) {
               final String title = sp.getTextWithDefault(
                     SpectrumProperties.SpectrumComment, "");
               for (int i = 0; i < title.length(); i += 64) {
                  int e = i + 64;
                  if (e > title.length())
                     e = title.length();
                  // Gives a short description of the spectra
                  writeln(pw, "TITLE", title.substring(i, e));
               }
            } else
               writeln(pw, "TITLE", spec.toString());
            {
               final String[] months = {"JAN", "FEB", "MAR", "APR", "MAY",
                     "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
               long date = 0; // Default to conversion date
               if (sp.isDefined(SpectrumProperties.AcquisitionTime))
                  date = sp.getTimestampProperty(
                        SpectrumProperties.AcquisitionTime).getTime();
               final Calendar cal = Calendar.getInstance();
               cal.setTimeInMillis(date);
               // The calendar day-month-year in which the spectra was recorded,
               // in
               // the form: DD-MMM-YYYY.
               writeln(pw, "DATE",
                     (cal.get(Calendar.DAY_OF_MONTH) < 10 ? "0" : "")
                           + Integer.toString(cal.get(Calendar.DAY_OF_MONTH))
                           + "-" + months[cal.get(Calendar.MONTH)] + "-"
                           + Integer.toString(cal.get(Calendar.YEAR)));
               // The time of day at which the spectrum was recorded,in 24-hour
               // format: HH:MM.
               writeln(pw, "TIME",
                     (cal.get(Calendar.HOUR) < 10 ? "0" : "")
                           + Integer.toString(cal.get(Calendar.HOUR)) + ":"
                           + (cal.get(Calendar.MINUTE) < 10 ? "0" : "")
                           + Integer.toString(cal.get(Calendar.MINUTE)));
            }
            // The name of the person who recorded the spectrum.
            writeln(pw, "OWNER", sp.getTextWithDefault(
                  SpectrumProperties.InstrumentOperator, "Unknown"));
            // NPOINTS =Total Number of Data Points in X & Y Data Arrays
            // 1. < NPOINTS < 4096.
            // Note: Limits not enforced in this implementation
            writeln(pw, "NPOINTS", Integer.toString(nCh));
            // Number of columns of data
            writeln(pw, "NCOLUMNS", Integer.toString(nCols));
            // Units for x-axis data, for example: eV.
            writeln(pw, "XUNITS", "eV");
            // Units for y-axis data, for example: counts.
            writeln(pw, "YUNITS", "counts");
            // Method in which the data values are stored as Y Axis only values
            // or
            // X,Y data pairs. The
            // current options are the characters 'Y' and 'XY'.
            writeln(pw, "DATATYPE", "Y");
            // The number of x-axis units per channel.
            writeln(pw, "XPERCHAN", nf.format(spec.getChannelWidth()));
            // A real (but possibly negative) number representing value of
            // channel
            // one in xunits
            writeln(pw, "OFFSET", nf.format(spec.getZeroOffset()));
            // Accelerating Voltage of Instrument in kilovolts [RN]
            if (sp.isDefined(SpectrumProperties.BeamEnergy))
               writeln(pw, "BEAMKV", nf.format(
                     sp.getNumericProperty(SpectrumProperties.BeamEnergy)));
            // SIGNALTYPE = Type of Spectroscopy, allowed values are [3CS]:
            // EDS = Energy Dispersive Spectroscopy
            // WDS = Wavelength Dispersive Spectroscopy
            // ELS = Energy Loss Spectroscopy
            // AES = Auger Electron Spectroscopy
            // PES = Photo Electron Spectroscopy
            // XRF = X-ray Fluorescence Spectroscopy
            // CLS = Cathodoluminescence Spectroscopy
            // GAM = Gamma Ray Spectroscopy
            writeln(pw, "SIGNALTYPE", "EDS");
            // Elevation angle of EDS,WDS detector in degrees [RN]
            if (sp.isDefined(SpectrumProperties.Elevation))
               writeln(pw, "ELEVANGLE", nf.format(
                     sp.getNumericProperty(SpectrumProperties.Elevation)));
            // Azimuthal angle of EDS,WDS detector in degrees [RN]
            if (sp.isDefined(SpectrumProperties.Azimuth))
               writeln(pw, "AZIMANGLE", nf.format(
                     sp.getNumericProperty(SpectrumProperties.Azimuth)));
            // Signal Processor Active (Live) time in seconds [RN]
            if (sp.isDefined(SpectrumProperties.LiveTime))
               writeln(pw, "LIVETIME", nf.format(
                     sp.getNumericProperty(SpectrumProperties.LiveTime)));
            // Total clock time used to record the spectrum in seconds [RN]
            if (sp.isDefined(SpectrumProperties.RealTime))
               writeln(pw, "REALTIME", nf.format(
                     sp.getNumericProperty(SpectrumProperties.RealTime)));
            // Magnification or Camera Length [RN] Mag in x or times, Cl in mm
            if (sp.isDefined(SpectrumProperties.FaradayBegin)) {
               double pc = sp
                     .getNumericProperty(SpectrumProperties.FaradayBegin);
               pc += sp.getNumericWithDefault(SpectrumProperties.FaradayEnd,
                     pc);
               // Probe current in nanoAmps [RN]
               writeln(pw, "PROBECUR", nf.format(pc / 2.0));
            }
            // Thickness of Au Window/Electrical Contact in cm [RN]
            if (sp.isDefined(SpectrumProperties.GoldLayer))
               writeln(pw, "TAUWIND",
                     nf.format(
                           sp.getNumericProperty(SpectrumProperties.GoldLayer)
                                 * 1.0e-7));
            // Thickness of Dead Layer in cm [RN]
            if (sp.isDefined(SpectrumProperties.DeadLayer))
               writeln(pw, "TDEADLYR",
                     nf.format(
                           sp.getNumericProperty(SpectrumProperties.DeadLayer)
                                 * 1.0e-4));
            // Thickness of Active Layer in cm [RN]
            if (sp.isDefined(SpectrumProperties.ActiveLayer))
               writeln(pw, "TACTLYR",
                     nf.format(
                           sp.getNumericProperty(SpectrumProperties.ActiveLayer)
                                 / 10000.0));
            else if (sp.isDefined(SpectrumProperties.DetectorThickness))
               writeln(pw, "TACTLYR", nf.format(
                     sp.getNumericProperty(SpectrumProperties.DetectorThickness)
                           / 10.0));
            // Thickness of Be Window on detector in cm [RN]
            if (sp.isDefined(SpectrumProperties.BerylliumWindow))
               writeln(pw, "TBEWIND", nf.format(
                     sp.getNumericProperty(SpectrumProperties.BerylliumWindow)
                           / 10000.0));
            // Thickness of Aluminium Window in cm [RN]
            if (sp.isDefined(SpectrumProperties.AluminumWindow))
               writeln(pw, "TALWIND", nf.format(
                     sp.getNumericProperty(SpectrumProperties.AluminumWindow)
                           / 10000.0));
            // Thickness of Pyrolene Window in cm [RN]
            if (sp.isDefined(SpectrumProperties.PyroleneWindow))
               writeln(pw, "TPYWIND", nf.format(
                     sp.getNumericProperty(SpectrumProperties.PyroleneWindow)
                           / 10000.0));
            // Thickness of Boron-Nitride Window in cm [RN]
            if (sp.isDefined(SpectrumProperties.BoronNitrideWindow))
               writeln(pw, "TBNWIND", nf.format(sp.getNumericProperty(
                     SpectrumProperties.BoronNitrideWindow) / 10000.0));
            // Thickness of Diamond Window in cm [RN]
            if (sp.isDefined(SpectrumProperties.DiamondWindow))
               writeln(pw, "TDIWIND", nf.format(
                     sp.getNumericProperty(SpectrumProperties.DiamondWindow)
                           / 10000.0));
            // Thickness of HydroCarbon Window in cm [RN]
            if (sp.isDefined(SpectrumProperties.HydroCarbonWindow))
               writeln(pw, "THCWIND", nf.format(
                     sp.getNumericProperty(SpectrumProperties.HydroCarbonWindow)
                           / 10000.0));
            writeln(pw, "CHOFFSET", "0.0");
            // X-Axis Data label
            writeln(pw, "XLABEL", "Energy (eV)");
            // Y-Axis Data label
            writeln(pw, "YLABEL", "Counts");
            // Gun Emission current in microAmps [RN]
            if (sp.isDefined(SpectrumProperties.EmissionCurrent))
               writeln(pw, "EMISSION", nf.format(sp
                     .getNumericProperty(SpectrumProperties.EmissionCurrent)));
            // Diameter of incident probe in nanometers [RN]
            if (sp.isDefined(SpectrumProperties.ProbeArea)) {
               final double area = sp
                     .getNumericProperty(SpectrumProperties.ProbeArea);
               writeln(pw, "BEAMDIA",
                     nf.format(2.0 * Math.sqrt(area / Math.PI)));
            }
            // Specimen/Beam position along the X axis [RN]
            if (sp.isDefined(SpectrumProperties.StagePosition)) {
               final StageCoordinate pos = (StageCoordinate) sp
                     .getObjectWithDefault(SpectrumProperties.StagePosition,
                           null);
               if (pos.isPresent(Axis.X))
                  writeln(pw, "XPOSITION", nf.format(pos.get(Axis.X)));
               if (pos.isPresent(Axis.Y))
                  writeln(pw, "YPOSITION", nf.format(pos.get(Axis.Y)));
               if (pos.isPresent(Axis.Z))
                  writeln(pw, "ZPOSITION", nf.format(pos.get(Axis.Z)));
            }
            if (sp.isDefined(SpectrumProperties.DetectorType)) {
               final String dt = sp
                     .getTextProperty(SpectrumProperties.DetectorType);
               final StringBuffer res = new StringBuffer();
               if (dt.equals(DetectorProperties.SILI))
                  res.append("SI");
               else if (dt.equals(DetectorProperties.SDD))
                  res.append("SD");
               else if (dt.equals(DetectorProperties.MICROCAL))
                  res.append("UCAL");
               else
                  res.append("UNK");
               if (sp.isDefined(SpectrumProperties.WindowType)) {
                  final String wt = sp
                        .getTextProperty(SpectrumProperties.WindowType);
                  if (wt.equals(XRayWindowFactory.NO_WINDOW))
                     res.append("WLS");
                  else if (wt.equals(XRayWindowFactory.UT_WINDOW))
                     res.append("UTW");
                  else if (wt.equals(XRayWindowFactory.BE_WINDOW))
                     res.append("BEW");
                  else if (wt.equals(XRayWindowFactory.DIAMOND_WINDOW))
                     res.append("DIA");
                  else if (wt.equals(XRayWindowFactory.BN_WINDOW))
                     res.append("BNW");
               }
               writeln(pw, "EDSDET", res.toString());
            }
            if (mode != Mode.FOR_DTSA) {
               // Specimen stage tilt X-axis in degrees [RN]
               {
                  final SampleShape ss = sp.getSampleShapeWithDefault(
                        SpectrumProperties.SampleShape, null);
                  if (ss != null) {
                     double[] normal = ss.getOrientation();
                     normal = Math2.normalize(normal);
                     final double xTilt = Math.asin(normal[0]);
                     final double yTilt = Math
                           .asin(normal[1] / Math.cos(xTilt));
                     if (Math.abs(xTilt) > Math.toRadians(1.0e-2))
                        writeln(pw, "XTILTSTGE",
                              nf.format(Math.toDegrees(xTilt)));
                     if (Math.abs(yTilt) > Math.toRadians(1.0e-2))
                        writeln(pw, "YTILTSTGE",
                              nf.format(Math.toDegrees(yTilt)));
                  }
               }
               if (sp.isDefined(SpectrumProperties.Magnification))
                  writeln(pw, "MAGCAM", nf.format(sp
                        .getNumericProperty(SpectrumProperties.Magnification)));
               // Convergence semi-angle of incident beam in milliRadians [RN]
               if (sp.isDefined(SpectrumProperties.ConvergenceAngle))
                  writeln(pw, "CONVANGLE", nf.format(sp.getNumericProperty(
                        SpectrumProperties.ConvergenceAngle)));
               // Integration time per spectrum for parallel data collection in
               // milliseconds [RN]
               if (sp.isDefined(SpectrumProperties.IntegrationTime))
                  writeln(pw, "INTEGTIME", nf.format(sp.getNumericProperty(
                        SpectrumProperties.IntegrationTime)));
            }
            if (mode == Mode.COMPATIBLE) {
               // Custom tags...
               if (sp.isDefined(SpectrumProperties.SpecimenDesc))
                  writeln(pw, "#SPECIMEN", sp.getTextWithDefault(
                        SpectrumProperties.SpecimenDesc, ""));
               boolean elms = false;
               {
                  final Composition comp = sp.getCompositionWithDefault(
                        SpectrumProperties.StandardComposition, null);
                  if (comp != null) {
                     writeln(pw, "#D2STDCMP", comp.toParsableFormat());
                     elms = true;
                  }
               }
               {
                  final Composition comp = sp.getCompositionWithDefault(
                        SpectrumProperties.MicroanalyticalComposition, null);
                  if (comp != null) {
                     writeln(pw, "#D2QUANT", comp.toParsableFormat());
                     elms = true;
                  }
               }
               if (!elms) {
                  final Set<Element> elmset = sp.getElements();
                  if (elmset != null)
                     writeln(pw, "#D2ELEMS", elmset.toString());
               }
               // These tags associate provide a unique ID to associate
               // detectors and calibrations across disparate applications.
               // The value is not important except that it should uniquely
               // identify a detector and a calibration throughout time and
               // space.
               if (sp.isDefined(SpectrumProperties.DetectorGUID))
                  writeln(pw, "#DET_HASH", sp.getTextWithDefault(
                        SpectrumProperties.DetectorGUID, ""));
               if (sp.isDefined(SpectrumProperties.CalibrationGUID))
                  writeln(pw, "#CALIB_HASH", sp.getTextWithDefault(
                        SpectrumProperties.CalibrationGUID, ""));
               if (sp.isDefined(SpectrumProperties.WorkingDistance))
                  writeln(pw, "#WORKING", sp.getTextWithDefault(
                        SpectrumProperties.WorkingDistance, "-"));
               if (sp.isDefined(SpectrumProperties.SampleShape))
                  writeln(pw, "#SAMPLE",
                        massage(EPQXStream.getInstance()
                              .toXML(sp.getSampleShapeWithDefault(
                                    SpectrumProperties.SampleShape, null))));
               if (sp.isDefined(SpectrumProperties.MassThickness))
                  writeln(pw, "#MASSTHICK",
                        Double.toString(sp.getNumericWithDefault(
                              SpectrumProperties.MassThickness, 0.0)));
               if (sp.isDefined(SpectrumProperties.MultiSpectruMetric))
                  writeln(pw, "#MULTISPEC",
                        Double.toString(sp.getNumericWithDefault(
                              SpectrumProperties.MultiSpectruMetric, -1.0)));
               if (sp.isDefined(SpectrumProperties.ConductiveCoating)) {
                  final ConductiveCoating cc = (ConductiveCoating) sp
                        .getObjectWithDefault(
                              SpectrumProperties.ConductiveCoating, null);
                  if (cc != null)
                     writeln(pw, "#CONDCOATING", cc.toParsableFormat());
               }
               if (sp.isDefined(SpectrumProperties.Detector)) {
                  writeln(pw, "#DET_NAME", sp.getDetector().getName());
               }
               if (sp.isDefined(SpectrumProperties.DetectorMode)) {
                  writeln(pw, "#DET_MODE", sp.getTextWithDefault(
                        SpectrumProperties.DetectorMode, ""));
               }
               if ((path != null)
                     && (path.getName().toLowerCase().endsWith(".msa"))
                     && (sp.isDefined(SpectrumProperties.MicroImage)
                           || sp.isDefined(SpectrumProperties.MicroImage2))) {
                  try {
                     final File imgPath = new File(path.getPath()
                           .replace("\\.[Mm][Ss][Aa]$", "_msa.tif"));
                     WriteSpectrumAsTIFF.writeMicroImages(imgPath, spec);
                     writeln(pw, "#IMAGE_REF", imgPath.getName());
                  } catch (IOException e) {
                     // Just ignore it...
                  }
               }

            }
            writeln(pw, "SPECTRUM", "");
            for (int n = 0; n < nCh; ++n)
               pw.println((n < spec.getChannelCount()
                     ? nf.format(spec.getCounts(n))
                     : "0") + ",");
            writeln(pw, "ENDOFDATA", "");
            pw.write('\n');
            pw.flush();
         } finally {
            System.setProperty("line.separator", oldLs);
         }
      } catch (final UnsupportedEncodingException e) {
         throw new EPQException(e);
      }
   }

   private static String massage(String input) {
      return input.replaceAll("\\n\\h*", "\\\\n");

   }

}
