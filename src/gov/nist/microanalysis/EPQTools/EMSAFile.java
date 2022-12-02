package gov.nist.microanalysis.EPQTools;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.ConductiveCoating;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.SampleShape;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate.Axis;
import gov.nist.microanalysis.EPQLibrary.Detector.DetectorProperties;
import gov.nist.microanalysis.EPQLibrary.Detector.XRayWindowFactory;

/**
 * <p>
 * A class for reading EMSA files
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie @version 1.0
 */

public class EMSAFile extends BaseSpectrum {
   private final SpectrumProperties mProperties = new SpectrumProperties();
   private double[] mChannels;
   // Transient bookkeeping
   private transient NumberFormat mDefaultFormat;
   private transient boolean mIsXY = false;
   private transient boolean mIsCPS = false;
   private transient double mBaseXUnit = 1.0; // eV
   private transient double mXTilt = 0.0, mYTilt = 0.0;
   private transient StageCoordinate mStagePosition;

   /**
    * Is the file specified by this Reader likely to be a EMSAFile. @param is
    * InputStream - Which will be closed by isInstanceOf @return boolean
    */
   public static boolean isInstanceOf(InputStream is) {
      boolean res = true;
      try {
         try (final BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("US-ASCII")))) {
            String line = br.readLine().trim();
            // It seems that DTSA expects a blank line up front!?! and LISPIX
            // obliges
            if (line.length() == 0)
               line = br.readLine().trim();
            res = line.startsWith("#FORMAT");
         }
      } catch (final Exception ex) {
         res = false;
      }
      return res;
   }

   @Override
   public int getChannelCount() {
      return mChannels.length;
   }

   @Override
   public double getCounts(int i) {
      return mChannels[i];
   }

   @Override
   public SpectrumProperties getProperties() {
      return mProperties;
   }

   private double parseDouble(String value) throws ParseException {
      if (value.indexOf('+') != -1)
         value = value.replaceAll("\\+", "");
      if (value.indexOf('e') != -1)
         value = value.replaceAll("e", "E");
      return mDefaultFormat.parse(value).doubleValue();
   }

   private void reset() {
      mProperties.clear();
      setEnergyScale(0.0, 10.0);
      mChannels = null;
      // Transient bookkeeping
      mDefaultFormat = null;
      mIsXY = false;
      mIsCPS = false;
      mBaseXUnit = 1.0; // eV
      mXTilt = 0.0;
      mYTilt = 0.0;
   }

   /**
    * read - reads the parsed file in EMSA format and converts it into the
    * intermediate format, ISpectrumData. @param is FileReader - the EMSA
    * file @throws IOException throws an IOException if there is an error
    * reading the file
    */
   public void read(InputStream is) throws IOException {
      reset();
      try (final Reader rd = new InputStreamReader(is, Charset.forName("US-ASCII"))) {
         try (final BufferedReader br = new BufferedReader(rd)) {
            // Number always use '.' as decimal separator
            mDefaultFormat = NumberFormat.getInstance(Locale.US);
            String Prefix, Data;
            String line = br.readLine().trim();
            // It seems that DTSA expects a blank line up front!?! and LISPIX
            // obliges
            if (line.length() == 0)
               line = br.readLine().trim();
            mStagePosition = new StageCoordinate();
            do {
               final int p = line.indexOf(':');
               Prefix = p != -1 ? line.substring(0, p) : "";
               Data = line.substring(p + 1);
               line = br.readLine().trim();
            } while (storeData(Prefix, Data));
            if (mStagePosition.size() > 0)
               mProperties.setObjectProperty(SpectrumProperties.StagePosition, mStagePosition);
            mStagePosition = null;
            final double scale = mIsCPS ? mProperties.getNumericWithDefault(SpectrumProperties.LiveTime, 1.0) : 1.0;
            // Cases: nextDatum then "," or nextDatum then EOL or EOL
            if (!line.startsWith("#ENDOFDATA")) {
               int start = 0, dataCounter = 0;
               int xyCounter = 0;
               while (br.ready() && (dataCounter < mChannels.length)) {
                  String item;
                  final int end = line.indexOf(',', start);
                  if (end == -1) { // no ","
                     item = line.substring(start).trim();
                     start = line.length();
                  } else {
                     item = line.substring(start, end).trim();
                     start = end + 1;
                  }
                  if (item.length() == 0) {
                     line = br.readLine();
                     start = 0;
                     if ((line == null) || line.startsWith("#ENDOFDATA"))
                        break;
                     continue;
                  }
                  if ((!mIsXY) || ((xyCounter % 2) == 1))
                     try {
                        mChannels[dataCounter] = scale * parseDouble(item);
                        dataCounter++;
                     } catch (final ParseException pe) {
                        // just ignore it...
                     }
                  ++xyCounter;
               }
               if (dataCounter != mChannels.length)
                  System.err.println("The number of data points was fewer than the reported number of channels.");
            }
            setEnergyScale(getZeroOffset() * mBaseXUnit, getChannelWidth() * mBaseXUnit);
            mDefaultFormat = null;
         }
      }
   }

   // str="hh:mm" or "hh:mm:ss"
   private Date parseTime(String str) {
      int day, month, year, hour = 9, min = 0, sec = 0;
      final Calendar c = Calendar.getInstance();
      try {
         if (mProperties.isDefined(SpectrumProperties.AcquisitionTime))
            c.setTime(mProperties.getTimestampProperty(SpectrumProperties.AcquisitionTime));
      } catch (final EPQException ex) {
      }
      day = c.get(Calendar.DAY_OF_MONTH);
      month = c.get(Calendar.MONTH);
      year = c.get(Calendar.YEAR);
      try {
         final String[] items = str.split(":");
         if (items.length >= 2) {
            hour = Integer.parseInt(items[0].trim());
            min = Integer.parseInt(items[1].trim());
            if (items.length > 2)
               sec = Integer.parseInt(items[2].trim()); // seconds
         }
      } catch (final Exception e) {
      }
      c.set(year, month, day, hour, min, sec);
      return c.getTime();
   }

   // value="dd-mmm-yyyy"
   private Date parseDate(String value) {
      final Calendar c = Calendar.getInstance();
      // Default to today
      int day = c.get(Calendar.DAY_OF_MONTH);
      int month = c.get(Calendar.MONTH);
      int year = c.get(Calendar.YEAR);
      int hour = 0, min = 0, sec = 0;
      try {
         if (mProperties.isDefined(SpectrumProperties.AcquisitionTime)) {
            // Get previously set time
            c.setTime(mProperties.getTimestampProperty(SpectrumProperties.AcquisitionTime));
            hour = c.get(Calendar.HOUR_OF_DAY);
            min = c.get(Calendar.MINUTE);
            sec = c.get(Calendar.SECOND);
         }
      } catch (final EPQException ex) {
      }
      try {
         // According to the EMSA standard (dd-mmm-yyyy)
         final String[] items = value.split("-");
         if (items.length < 3)
            throw new Exception("Misformatted date in EMSA file: " + value);
         day = Integer.parseInt(items[0].trim());
         month = findMonth(items[1].trim()) - 1;
         year = Integer.parseInt(items[2].trim());
         if (year < 100)
            year += 2000;
         if (year < 200)
            year += 1900;
      } catch (final Exception ex1) {
         // According to the locale
         try {
            final Date dt = DateFormat.getInstance().parse(value);
            c.setTimeInMillis(dt.getTime());
            day = c.get(Calendar.DAY_OF_MONTH);
            month = c.get(Calendar.MONTH);
            year = c.get(Calendar.YEAR);
         } catch (final Exception e) {
            System.err.println("Unable to parse date: " + value);
         }
      }
      c.set(year, month, day, hour, min, sec);
      return c.getTime();
   }

   private boolean storeData(String prefix, String value) {
      prefix = prefix.trim();
      prefix = prefix.toUpperCase();
      try {
         value = value.trim();
         if (prefix.startsWith("#FORMAT")) {
            final String du = value.toUpperCase();
            if (!(du.equals("EMSA/MAS SPECTRAL DATA FILE") || du.equals("EMSA/MAS SPECTRAL DATA STANDARD")))
               System.err.println("The format header in this EMSA file is spurious: " + value);
         } else if (prefix.startsWith("#VERSION")) {
            if ((parseDouble(value) != 1.0) && (!value.equalsIgnoreCase("TC202v1.0")))
               System.err.println("The EMSA file version number was not 1.0. It was " + value);
         } else if (prefix.startsWith("#SPECIMEN")) {
            if (value.length() > 0)
               mProperties.setTextProperty(SpectrumProperties.SpecimenDesc, value);
         } else if (prefix.startsWith("#TITLE")) {
            if ((value.length() > 0) && (!value.startsWith("EDS Spectral Data")))
               mProperties.setTextProperty(SpectrumProperties.SpectrumComment, value);
         } else if (prefix.startsWith("#DATE"))
            mProperties.setTimestampProperty(SpectrumProperties.AcquisitionTime, parseDate(value));
         else if (prefix.startsWith("#TIME"))
            mProperties.setTimestampProperty(SpectrumProperties.AcquisitionTime, parseTime(value));
         else if (prefix.startsWith("#OWNER")) {
            if (value.length() > 0)
               mProperties.setTextProperty(SpectrumProperties.InstrumentOperator, value);
         } else if (prefix.startsWith("#NPOINTS")) {
            if (value.startsWith("+"))
               value = value.substring(1);
            final int n = mDefaultFormat.parse(value).intValue();
            mChannels = new double[n];
         } else if (prefix.startsWith("#NCOLUMNS")) {
            // not necessary
         } else if (prefix.startsWith("#XUNITS")) {
            mBaseXUnit = 1.0;
            if (value.toUpperCase().contains("KEV")) {
               mBaseXUnit = 1000.0; // eV
               value = "eV";
            }
            if (value.length() > 0)
               mProperties.setTextProperty(SpectrumProperties.XUnits, value);
         } else if (prefix.startsWith("#YUNITS")) {
            if (value.length() > 0) {
               if (value.trim().compareToIgnoreCase("CPS") == 0) {
                  mIsCPS = true;
                  mProperties.setTextProperty(SpectrumProperties.YUnits, "Counts");
               } else
                  mProperties.setTextProperty(SpectrumProperties.YUnits, value);
            }
         } else if (prefix.startsWith("#XLABEL")) {

         } else if (prefix.startsWith("#YLABEL")) {

         } else if (prefix.startsWith("#DATATYPE"))
            mIsXY = value.equals("XY");
         // not necessary
         else if (prefix.startsWith("#XPERCHAN"))
            mProperties.setNumericProperty(SpectrumProperties.EnergyScale, parseDouble(value));
         else if (prefix.startsWith("#OFFSET"))
            mProperties.setNumericProperty(SpectrumProperties.EnergyOffset, parseDouble(value));
         else if (prefix.startsWith("#CHOFFSET")) {
            // The offset in channel units
         } else if (prefix.startsWith("#SIGNALTYPE")) {
            if (value.length() > 0)
               mProperties.setTextProperty(SpectrumProperties.SignalType, value);
         } else if (prefix.startsWith("#XLABEL") || prefix.startsWith("#YLABEL")) {
            // ignore
         } else if (prefix.startsWith("#COMMENT")) {
            if ((!mProperties.isDefined(SpectrumProperties.SpectrumComment)) && (!value.startsWith("Converted by SpecUtil32 of EDAX INC")))
               mProperties.setTextProperty(SpectrumProperties.SpectrumComment, value);
         } else if (prefix.startsWith("#BEAMKV"))
            mProperties.setNumericProperty(SpectrumProperties.BeamEnergy, parseDouble(value));
         else if (prefix.startsWith("#EMISSION"))
            mProperties.setNumericProperty(SpectrumProperties.EmissionCurrent, parseDouble(value));
         else if (prefix.startsWith("#PROBECUR")) {
            final double val = parseDouble(value);
            if (val > 0.0)
               mProperties.setNumericProperty(SpectrumProperties.ProbeCurrent, val);
         } else if (prefix.startsWith("#BEAMDIA")) {
            final double r = parseDouble(value) / 2.0;
            mProperties.setNumericProperty(SpectrumProperties.ProbeArea, Math.PI * r * r);
         } else if (prefix.matches("#MAGCAM"))
            mProperties.setNumericProperty(SpectrumProperties.Magnification, parseDouble(value));
         else if (prefix.startsWith("#CONVANGLE"))
            mProperties.setNumericProperty(SpectrumProperties.ConvergenceAngle, parseDouble(value));
         else if (prefix.startsWith("#OPERMODE")) {
            if (value.equals("IMAG"))
               value = value + "E";
            if (value.length() > 0)
               mProperties.setTextProperty(SpectrumProperties.OperatingMode, value);
         } else if (prefix.startsWith("#THICKNESS"))
            mProperties.setNumericProperty(SpectrumProperties.SpecimenThickness, parseDouble(value));
         else if (prefix.startsWith("#XTILTSTGE")) {
            mXTilt = parseDouble(value);
            updateSampleOrientation();
         } else if (prefix.startsWith("#YTILTSTGE")) {
            mYTilt = parseDouble(value);
            updateSampleOrientation();
         } else if (prefix.matches("#XPOSITION") || prefix.matches("#XPOSITION MM"))
            mStagePosition.set(Axis.X, parseDouble(value));
         else if (prefix.matches("#YPOSITION") || prefix.matches("#YPOSITION MM"))
            mStagePosition.set(Axis.Y, parseDouble(value));
         else if (prefix.matches("#ZPOSITION") || prefix.matches("#ZPOSITION MM"))
            mStagePosition.set(Axis.Z, parseDouble(value));
         else if (prefix.startsWith("#DWELLTIME"))
            mProperties.setNumericProperty(SpectrumProperties.DwellTime, parseDouble(value));
         else if (prefix.matches("#INTEGTIME"))
            mProperties.setNumericProperty(SpectrumProperties.IntegrationTime, parseDouble(value));
         else if (prefix.matches("#COLLANGLE"))
            mProperties.setNumericProperty(SpectrumProperties.CollectionAngle, parseDouble(value));
         else if (prefix.matches("#ELSDET")) {
            if ((value.matches("SERIAL")) || (value.matches("PARALL"))) {
               // ignore
            }
         } else if (prefix.startsWith("#ELEVANGLE"))
            mProperties.setNumericProperty(SpectrumProperties.Elevation, parseDouble(value));
         else if (prefix.startsWith("#AZIMANGLE"))
            mProperties.setNumericProperty(SpectrumProperties.Azimuth, parseDouble(value));
         else if (prefix.startsWith("#SOLIDANGL"))
            mProperties.setNumericProperty(SpectrumProperties.SolidAngle, parseDouble(value));
         else if (prefix.startsWith("#LIVETIME"))
            mProperties.setNumericProperty(SpectrumProperties.LiveTime, parseDouble(value));
         else if (prefix.startsWith("#REALTIME"))
            mProperties.setNumericProperty(SpectrumProperties.RealTime, parseDouble(value));
         else if (prefix.startsWith("#TBEWIND"))
            mProperties.setNumericProperty(SpectrumProperties.BerylliumWindow, parseDouble(value) * 1.0e4);
         else if (prefix.startsWith("#TAUWIND"))
            mProperties.setNumericProperty(SpectrumProperties.GoldLayer, parseDouble(value) * 1.0e7);
         else if (prefix.startsWith("#TDEADLYR"))
            mProperties.setNumericProperty(SpectrumProperties.DeadLayer, parseDouble(value) * 1.0e4);
         else if (prefix.startsWith("#TACTLYR"))
            mProperties.setNumericProperty(SpectrumProperties.ActiveLayer, parseDouble(value) * 1.0e4);
         else if (prefix.startsWith("#TALWIND"))
            mProperties.setNumericProperty(SpectrumProperties.AluminumLayer, parseDouble(value) * 1.0e4);
         else if (prefix.startsWith("#TPYWIND"))
            mProperties.setNumericProperty(SpectrumProperties.ParaleneWindow, parseDouble(value) * 1.0e4);
         else if (prefix.startsWith("#TBNWIND"))
            mProperties.setNumericProperty(SpectrumProperties.BoronNitrideWindow, parseDouble(value) * 1.0e4);
         else if (prefix.startsWith("#TDIWIND"))
            mProperties.setNumericProperty(SpectrumProperties.DiamondWindow, parseDouble(value) * 1.0e4);
         else if (prefix.startsWith("#THCWIND"))
            mProperties.setNumericProperty(SpectrumProperties.HydroCarbonWindow, parseDouble(value) * 1.0e4);
         else if (prefix.startsWith("#EDSDET")) {
            if (value.equalsIgnoreCase("SIBEW")) {
               mProperties.setTextProperty(SpectrumProperties.DetectorType, DetectorProperties.SILI);
               mProperties.setTextProperty(SpectrumProperties.WindowType, XRayWindowFactory.BE_WINDOW);
            } else if (value.equalsIgnoreCase("SIUTW")) {
               mProperties.setTextProperty(SpectrumProperties.DetectorType, DetectorProperties.SILI);
               mProperties.setTextProperty(SpectrumProperties.WindowType, XRayWindowFactory.UT_WINDOW);
            } else if (value.equalsIgnoreCase("SIWLS")) {
               mProperties.setTextProperty(SpectrumProperties.DetectorType, DetectorProperties.SILI);
               mProperties.setTextProperty(SpectrumProperties.WindowType, XRayWindowFactory.NO_WINDOW);
            } else if (value.equalsIgnoreCase("GEBEW")) {
               mProperties.setTextProperty(SpectrumProperties.DetectorType, DetectorProperties.GE);
               mProperties.setTextProperty(SpectrumProperties.WindowType, XRayWindowFactory.BE_WINDOW);
            } else if (value.equalsIgnoreCase("GEUTW")) {
               mProperties.setTextProperty(SpectrumProperties.DetectorType, DetectorProperties.GE);
               mProperties.setTextProperty(SpectrumProperties.WindowType, XRayWindowFactory.UT_WINDOW);
            } else if (value.equalsIgnoreCase("SDBEW")) {
               mProperties.setTextProperty(SpectrumProperties.DetectorType, DetectorProperties.SDD);
               mProperties.setTextProperty(SpectrumProperties.WindowType, XRayWindowFactory.BE_WINDOW);
            } else if (value.equalsIgnoreCase("SDUTW")) {
               mProperties.setTextProperty(SpectrumProperties.DetectorType, DetectorProperties.SDD);
               mProperties.setTextProperty(SpectrumProperties.WindowType, XRayWindowFactory.UT_WINDOW);
            } else if (value.equalsIgnoreCase("SDWLS")) {
               mProperties.setTextProperty(SpectrumProperties.DetectorType, DetectorProperties.SDD);
               mProperties.setTextProperty(SpectrumProperties.WindowType, XRayWindowFactory.NO_WINDOW);
            } else if (value.equalsIgnoreCase("UCALUTW")) {
               mProperties.setTextProperty(SpectrumProperties.DetectorType, DetectorProperties.MICROCAL);
               mProperties.setTextProperty(SpectrumProperties.WindowType, XRayWindowFactory.UT_WINDOW);
            }
         } else if (prefix.startsWith("##D2STDCMP")) { // DTSA-II custom tag
            final Composition comp = Material.fromParsableFormat(value);
            if (comp != null)
               mProperties.setCompositionProperty(SpectrumProperties.StandardComposition, comp);
         } else if (prefix.startsWith("##D2QUANT")) { // DTSA-II custom tag
            final Composition comp = Material.fromParsableFormat(value);
            if (comp != null)
               mProperties.setCompositionProperty(SpectrumProperties.MicroanalyticalComposition, comp);
         } else if (prefix.startsWith("##D2ELEMS")) {
            final Set<Element> elms = Element.parseElementString(value);
            if (elms != null)
               mProperties.setElements(elms);
         } else if (prefix.startsWith("##WORKING")) {
            if (value.length() > 0)
               mProperties.setNumericProperty(SpectrumProperties.WorkingDistance, parseDouble(value));
         } else if (prefix.startsWith("##WINDOW")) {
            if (value.length() > 0)
               mProperties.setTextProperty(SpectrumProperties.WindowType, value);
         } else if (prefix.startsWith("##MNFWHM  -eV")) {
            mProperties.setNumericProperty(SpectrumProperties.Resolution, parseDouble(value));
            mProperties.setNumericProperty(SpectrumProperties.ResolutionLine, SpectrumUtils.E_MnKa);
         } else if (prefix.startsWith("##SPECIMEN")) {
            // Custom property
            if (value.length() > 0)
               mProperties.setTextProperty(SpectrumProperties.SpecimenDesc, value);
         } else if (prefix.startsWith("##DEAD_TM -%")) { // JEOL EMSA
            // mProperties.setNumericProperty(SpectrumProperties.DeadPercent,
            // Double.parseDouble(value));
         } else if (prefix.startsWith("##SH_TIME -%")) { // JEOL EMSA
            final int sh = Integer.parseInt(value);
            switch (sh) {
               case 0 :
                  mProperties.setNumericProperty(SpectrumProperties.PulseProcessTime, 3.2);
                  break;
               case 1 :
                  mProperties.setNumericProperty(SpectrumProperties.PulseProcessTime, 6.4);
                  break;
               case 2 :
                  mProperties.setNumericProperty(SpectrumProperties.PulseProcessTime, 51.2);
                  break;
               case 3 :
                  mProperties.setNumericProperty(SpectrumProperties.PulseProcessTime, 102.4);
                  break;
               default :
                  // Don't do anything...
                  break;
            }
         } else if (prefix.startsWith("##DEAD_TM -%"))
            mProperties.setNumericProperty(SpectrumProperties.DeadPercent, Double.parseDouble(value));
         else if (prefix.startsWith("##SH_TIME -%")) { // JEOL EMSA
            final int sh = Integer.parseInt(value);
            switch (sh) {
               case 0 :
                  mProperties.setNumericProperty(SpectrumProperties.PulseProcessTime, 3.2);
                  break;
               case 1 :
                  mProperties.setNumericProperty(SpectrumProperties.PulseProcessTime, 6.4);
                  break;
               case 2 :
                  mProperties.setNumericProperty(SpectrumProperties.PulseProcessTime, 51.2);
                  break;
               case 3 :
                  mProperties.setNumericProperty(SpectrumProperties.PulseProcessTime, 102.4);
                  break;
               default :
                  // Don't do anything...
                  break;
            }
         } else if (prefix.startsWith("##SAMPLE")) {
            try {
               Object obj = EPQXStream.getInstance().fromXML(massage(value));
               if (obj instanceof SampleShape)
                  mProperties.setSampleShape(SpectrumProperties.SampleShape, (SampleShape) obj);
            } catch (Exception e) {
               // TESCAN also uses the ##SAMPLE tag but it just contains a
               // specimen description.
               mProperties.setTextProperty(SpectrumProperties.SpecimenDesc, value);

            }
            // ignored special custom tag (ignore)
         } else if (prefix.startsWith("##MASSTHICK")) {
            mProperties.setNumericProperty(SpectrumProperties.MassThickness, Double.parseDouble(value));
         } else if (prefix.startsWith("##CONDCOATING")) {
            mProperties.setObjectProperty(SpectrumProperties.ConductiveCoating, ConductiveCoating.parse(value));
         } else if (prefix.startsWith("##IMAGE_REF")) {
            mProperties.setTextProperty(SpectrumProperties.ImageRef, value.trim());
         } else if (prefix.startsWith("##")) {
            // ignored special custom tag (ignore)
         } else if (prefix.startsWith("#SPECTRUM"))
            return false;
         else if (prefix.startsWith("##MULTISPEC")) {
            mProperties.setNumericProperty(SpectrumProperties.MultiSpectrumMetric, Double.parseDouble(value));
         } else
            System.err.println("Unknown tag type in EMSA file - " + prefix);
      } catch (NumberFormatException | ParseException e) {
         e.printStackTrace();
      }
      return true;
   }

   private String massage(String input) {
      return input.replaceAll("\\n", "\n");
   }

   private void updateSampleOrientation() {
      final double[] normal = new double[]{Math.sin(mXTilt), Math.cos(mXTilt) * Math.sin(mYTilt), -Math.cos(mXTilt) * Math.cos(mYTilt)};
      mProperties.setSampleShape(SpectrumProperties.SampleShape, new SampleShape.Bulk(normal));
   }

   private int findMonth(String month) {
      final String[] months = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
      for (int i = 0; i < months.length; ++i)
         if (month.equalsIgnoreCase(months[i]))
            return i + 1;
      return 1;
   }

   public EMSAFile() {
      super();
      // Set the default acquisition time stamp to now...
      mProperties.setTimestampProperty(SpectrumProperties.AcquisitionTime, new Date());
   }

   public EMSAFile(File file, boolean withImgs) throws IOException {
      this();
      try (final InputStream is = new FileInputStream(file)) {
         read(is);
         setFilename(file.getCanonicalPath());
         if (withImgs) {
            try {
               final String imgRef = getProperties().getTextWithDefault(SpectrumProperties.ImageRef, null);
               if (imgRef != null) {
                  final File fn = new File(file.getParentFile(), imgRef);
                  final BufferedImage[] sis = ASPEXImage.read(fn);
                  if (sis.length > 0)
                     getProperties().setObjectProperty(SpectrumProperties.MicroImage, sis[0]);
                  if (sis.length > 1)
                     getProperties().setObjectProperty(SpectrumProperties.MicroImage2, sis[1]);
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
   }

   public EMSAFile(File file) throws IOException {
      this(file, false);
   }

   public EMSAFile(InputStream is) throws IOException {
      this();
      read(is);
   }

   public void setFilename(String filename) {
      final File f = new File(filename);
      {
         String path;
         try {
            path = f.getCanonicalPath();
         } catch (final IOException e) {
            path = f.getAbsolutePath();
         }
         mProperties.setTextProperty(SpectrumProperties.SourceFile, path);
      }
      String name = f.getName();
      if (name.toLowerCase().endsWith(".msa"))
         name = name.substring(0, name.length() - 4);
      else if (name.toLowerCase().endsWith(".emsa"))
         name = name.substring(0, name.length() - 5);
      mProperties.getTextWithDefault(SpectrumProperties.SpecimenDesc, null);

      if ((name.length() > 0) && (!name.matches("[1-9][0-9]*")))
         mProperties.setTextProperty(SpectrumProperties.SpectrumDisplayName, name);
      else if (!mProperties.isDefined(SpectrumProperties.SpectrumDisplayName)) {
         final File parent = f.getParentFile();
         mProperties.setTextProperty(SpectrumProperties.SpectrumDisplayName, parent.getName() + " - " + name);
      }
   }
}
