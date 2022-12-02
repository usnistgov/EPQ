package gov.nist.microanalysis.EPQLibrary;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.Detector.IXRayDetector;
import gov.nist.microanalysis.EPQLibrary.Detector.IXRayWindowProperties;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.TextUtilities;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * A generic class for capturing numerical and textual properties of spectra in
 * a file independent format.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class SpectrumProperties implements Cloneable, Serializable {

   private static final long serialVersionUID = 0x42;

   /**
    * Represents a tag by which individual properties can be identified.
    */
   static public class PropertyId implements Comparable<PropertyId> {
      private String mName;
      private final Class<?> mType;
      private final String mUnit;
      private String mFormatStr;

      public PropertyId(String name, String unit, Class<?> type) {
         mName = name;
         mUnit = unit;
         mType = type;
      }

      public PropertyId(String name, String unit, String fmtStr) {
         mName = name;
         mUnit = unit;
         mType = Number.class;
         mFormatStr = fmtStr;
      }

      public PropertyId(String name, Class<?> type) {
         mName = name;
         mUnit = "";
         mType = type;
      }

      public PropertyId(PropertyId base, int index) {
         mName = MessageFormat.format(base.mName, new Object[]{Integer.valueOf(index)});
         mUnit = base.mUnit;
         mType = base.mType;
      }

      public void writeObject(java.io.ObjectOutputStream out) throws IOException {
         out.writeObject(mName);
      }

      /**
       * readObject - Implements serializability
       * 
       * @param in
       * @throws IOException
       * @throws ClassNotFoundException
       */
      public void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
         mName = (String) in.readObject();
      }

      @Override
      public int compareTo(PropertyId obj) {
         return mName.compareTo(obj.mName);
      }

      @Override
      public String toString() {
         return mName;
      }

      @Override
      public int hashCode() {
         return mName.hashCode();
      }

      @Override
      public boolean equals(Object obj) {
         if ((obj != null) && obj.getClass().equals(getClass()))
            return mName.equals(((PropertyId) obj).mName);
         else
            return false;
      }

      /**
       * getName - Gets the name of this property.
       * 
       * @return String
       */
      public String getName() {
         return mName;
      }

      /**
       * getUnits - Get the type of units associated with an item of this
       * PropertyId.
       * 
       * @return String
       */
      public String getUnits() {
         return mUnit;
      }

      /**
       * getPropertyType - Returns an integer index (one of Numeric, Timestamp,
       * Boolean, ImageType, Textual or Unknown) to represent the type of item
       * stored in a property identified with this PropertyId.
       * 
       * @return int
       */
      public PropertyType getPropertyType() {
         if (mType.equals(Number.class))
            return PropertyType.Numeric;
         else if (mType.equals(Date.class))
            return PropertyType.Timestamp;
         else if (mType.equals(Boolean.class))
            return PropertyType.Boolean;
         else if (mType.equals(Image.class))
            return PropertyType.ImageType;
         else if (mType.equals(String.class))
            return PropertyType.Textual;
         else if (mType.equals(Array.class))
            return PropertyType.ArrayType;
         else if (mType.equals(Composition.class))
            return PropertyType.CompositionType;
         else if (mType.equals(ParticleSignature.class))
            return PropertyType.ParticleSignatureType;
         else if (mType.equals(SampleShape.class))
            return PropertyType.SampleShapeType;
         else
            return PropertyType.Unknown;
      }

      public String getFormatString() {
         return mFormatStr;
      }
   }

   // Context data

   /**
    * The display name of the spectrum. This is the string that toString
    * returns.
    */
   public static final PropertyId SpectrumDisplayName = new PropertyId("Display name", String.class);

   /**
    * A short name for the specimen (sample name)
    */
   public static final PropertyId SpecimenName = new PropertyId("Sample name", String.class);

   /**
    * A user friendly description of the specimen
    */
   public static final PropertyId SpecimenDesc = new PropertyId("Specimen description", String.class);

   /**
    * The index of the spectrum within the source file.
    */
   public static final PropertyId SpectrumIndex = new PropertyId("Spectrum index", "", "#");

   /**
    * A field (sometimes an index number) identifying the sample within a
    * project or within a sample block.
    */
   public static final PropertyId SampleId = new PropertyId("Sample number", String.class);

   /**
    * ClientNumber - A field (sometimes a number) provided by a client that the
    * client uses to identify the sample.
    */
   public static final PropertyId ClientsSampleID = new PropertyId("Client's ID", String.class);

   /**
    * The name of the client for whom the analysis was performed.
    */
   public static final PropertyId ClientName = new PropertyId("Client name", String.class);

   /**
    * A field (sometimes a number) identifying the project with which this
    * spectrum is associated.
    */
   public static final PropertyId ProjectName = new PropertyId("Project", String.class);

   /**
    * A field (sometimes a number) identifying the subproject with which this
    * spectrum is associated.
    */
   public static final PropertyId SubProjectName = new PropertyId("Subproject", String.class);

   /**
    * The name of the file from which the spectrum was extracted.
    */
   public static final PropertyId SourceFile = new PropertyId("Source file", String.class);

   /**
    * The file path (minus filename) from which the spectrum was extracted.
    */
   public static final PropertyId SourcePath = new PropertyId("Source path", String.class);

   /**
    * The file name and integer index detailing where the spectrum was found.
    */
   public static final PropertyId SourceFileId = new PropertyId("Identifier", String.class);

   /**
    * The instrument operator who collected the spectrum.
    */
   public static final PropertyId InstrumentOperator = new PropertyId("Operator", String.class);

   /**
    * The person who processed the spectrum
    */
   public static final PropertyId Analyst = new PropertyId("Analyst", String.class);

   /**
    * The instrument on which the spectum was collected. Should identify one
    * instrument instance uniquely.
    */
   public static final PropertyId Instrument = new PropertyId("Instrument", String.class);

   /**
    * The detector object on which this spectrum was collected.
    */
   public static final PropertyId Detector = new PropertyId("Detector", IXRayDetector.class);

   /**
    * A MD5 hash code that uniquely defines a single instance of a detector
    * throughout time and across computers.
    */
   public static final PropertyId DetectorGUID = new PropertyId("Detector GUID", String.class);

   /**
    * A MD5 hash code that uniquely defines a single instance of a detector
    * calibration throughout time and across computers.
    */
   public static final PropertyId CalibrationGUID = new PropertyId("Calibration GUID", String.class);

   /**
    * What software package was used to acquire the spectrum.
    */
   public static final PropertyId Software = new PropertyId("Software", String.class);

   /**
    * The time at which the spectrum was aquired. This is reported as the number
    * of milliseconds since January 1, 1970, 00:00:00 GMT.
    */
   public static final PropertyId AcquisitionTime = new PropertyId("Acquisition time", Date.class); //

   // Detector geometry
   /**
    * The azimuthal angle of the detector (from x-axis counterclockwise).
    * Preferentially set this property via the setDetectorPosition method.
    */
   public static final PropertyId Azimuth = new PropertyId("Azimuthal angle", "\u00B0", "0.##");

   /**
    * Elevation - The elevation of the detector above the plane defined by the
    * normal to the beam. This property differs from TakeOffAngle in as much as
    * this property refers to the detector position independent of sample
    * orientation. Preferentially set this property via the setDetectorPosition
    * method.
    */
   public static final PropertyId Elevation = new PropertyId("Elevation", "\u00B0", "0.##");

   /**
    * The active surface area of the detector in square millimeters.
    */
   public static final PropertyId DetectorArea = new PropertyId("Detector area", " mm\u00B2", "0.#");

   /**
    * The thickness of the active detector in millimeters.
    */
   public static final PropertyId DetectorThickness = new PropertyId("Detector thickness", " mm", "0.###");

   /**
    * The angle between the detector normal and the a vector between the sample
    * and the detector.
    */
   public static final PropertyId DetectorTilt = new PropertyId("Detector tilt", "\u00B0", "0.#");

   /**
    * The angle at which the beam comes off of the sample. This angle is defined
    * by the point at which the x-ray is emitted and the location of the
    * detector. In degrees measured from the x-y plane towards the z-axis. This
    * differs from Elevation in that it includes both the Elevation of the
    * detector and the orientation of the sample.
    */
   public static final PropertyId TakeOffAngle = new PropertyId("Take off angle", "\u00B0", "0.#");

   /**
    * The distance from the sample-beam point to the detector in millimeters.
    * Preferentially set this property via the setDetectorPosition method.
    */
   public static final PropertyId DetectorDistance = new PropertyId("Specimen-to-detector distance", " mm", "0.#");

   /**
    * Collection solid angle of detector in sR
    */
   public static final PropertyId SolidAngle = new PropertyId("Solid angle", " sR", "#.######");

   // Detector windows

   /**
    * A descriptive name for the window type.
    */
   public static final PropertyId WindowType = new PropertyId("Window type", String.class);

   /**
    * The thickness of the detector window in micrometers (\u00B5).
    */
   public static final PropertyId DiamondWindow = new PropertyId("Diamond window thickness", " \u00B1m", "0.#");

   /**
    * The thickness of the detector window in micrometers (\u00B5).
    */
   public static final PropertyId MylarWindow = new PropertyId("Mylar window thickness", " \u00B5m", "0.#");

   /**
    * The thickness of the detector window in micrometers (\u00B5).
    */
   public static final PropertyId BoronNitrideWindow = new PropertyId("Boron nitride window thickness", " \u00B5m", "0.#");

   /**
    * The thickness of the detector window in micrometers (\u00B5).
    */
   public static final PropertyId SiliconNitrideWindow = new PropertyId("Silicon nitride window thickness", " \u00B5m", "0.#");

   /**
    * The thickness of the detector window in micrometers (\u00B5).
    */
   public static final PropertyId ParaleneWindow = new PropertyId("Paralene window thickness", " \u00B5m", "0.#");

   /**
    * The thickness of the detector window in micrometers (\u00B5).
    */
   public static final PropertyId PyroleneWindow = new PropertyId("Pyrolene window thickness", " \u00B5m", "0.#");

   /**
    * The thickness of the detector window in micrometers (\u00B5).
    */
   public static final PropertyId MoxtekWindow = new PropertyId("Moxtek window thickness", " \u00B5m", "0.#");

   /**
    * The thickness of the detector window in micrometers (\u00B5).
    */
   public static final PropertyId HydroCarbonWindow = new PropertyId("Hydrocarbon window thickness", " \u00B5m", "0.#");

   /**
    * The thickness of the detector window in micrometers (\u00B5).
    */

   public static final PropertyId BerylliumWindow = new PropertyId("Beryllium window thickness", " \u00B5m", "0.#");

   /**
    * Thickness of the aluminum window in nanometers.
    */
   public static final PropertyId AluminumWindow = new PropertyId("Aluminum window thickness", " nm", "0.#");

   /**
    * Open area of the support grid used on polymer windows.
    */
   public static final PropertyId WindowOpenArea = new PropertyId("Support grid open area", "%", "0.0");

   /**
    * Thickness of the Si support grid used on polymer windows.
    */
   public static final PropertyId SupportGridThickness = new PropertyId("Support grid thickness", " mm", "0.000");

   // Detector coatings
   /**
    * The thickness (in nanometers) of the gold layer coating the surface of the
    * detector.
    */
   public static final PropertyId GoldLayer = new PropertyId("Gold layer thickness", " nm", "0.#");

   /**
    * The thickness (in nanometers) of the nickel layer coating the surface of
    * the detector.
    */
   public static final PropertyId NickelLayer = new PropertyId("Nickel layer thickness", " nm", "0.#");

   /**
    * The thickness (in nanometers) of the aluminum layer coating the surface of
    * the detector.
    */
   public static final PropertyId AluminumLayer = new PropertyId("Aluminum layer thickness", " nm", "0.#");

   /**
    * The thickness (in nanometers) of the boron layer coating the surface of
    * the detector.
    */
   public static final PropertyId BoronLayer = new PropertyId("Boron layer thickness", " nm", "0.#");

   /**
    * The thickness (in nanometers) of the carbon layer coating the surface of
    * the detector.
    */
   public static final PropertyId CarbonLayer = new PropertyId("Carbon layer thickness", " nm", "0.0");

   /**
    * The thickness (in nanometers) of the nitrogen layer coating the surface of
    * the detector.
    */
   public static final PropertyId NitrogenLayer = new PropertyId("Nitrogen layer thickness", " nm", "0.0");

   /**
    * The thickness (in nanometers) of the oxygen layer coating the surface of
    * the detector.
    */
   public static final PropertyId OxygenLayer = new PropertyId("Oxygen layer thickness", " nm", "0.0");

   // Contamination
   /**
    * Estimated thickness (in micrometers) of the ice layer covering the window
    * of the detector.
    */
   public static final PropertyId IceThickness = new PropertyId("Ice layer thickness", " \u00B5m", "0.#");
   /**
    * Estimated thickness (in micrometers) of the oil layer covering the window
    * of the detector.
    */
   public static final PropertyId OilThickness = new PropertyId("Oil layer thickness", " \u00B5m", "0.#");

   /**
    * Estimated thickness (in nanometers) of the carbon layer coating the
    * surface of the sample.
    */
   public static final PropertyId CarbonCoating = new PropertyId("Carbon layer", " nm", "0.#");

   /**
    * The thickness of the dead layer at the surface of the detector in
    * micrometers.
    */
   public static final PropertyId DeadLayer = new PropertyId("Dead layer", " \u00B5m", "0.##");

   /**
    * The dead time in percent.
    */
   public static final PropertyId DeadPercent = new PropertyId("Dead-time", "%", "0.#");

   /**
    * Thickness of the detector active layer in micrometers.
    */
   public static final PropertyId ActiveLayer = new PropertyId("Active layer", " \u00B5m", "0.#");

   /**
    * Type of X-ray Detector, allowed values are EDSDetector.SILI,
    * EDSDetector.SDD, EDSDetector.MICROCAL
    */
   public static final PropertyId DetectorType = new PropertyId("Detector type", String.class);

   /**
    * A property modeling the window between the sample and the detector.
    * (IXRayWindowProperties)
    */
   public static final PropertyId DetectorWindow = new PropertyId("Detector window", IXRayWindowProperties.class);

   // Detector calibration
   /**
    * The linear scale factor for converting channels into energies (in eV). [eV
    * per channel]
    */
   public static final PropertyId EnergyScale = new PropertyId("Energy scale", " eV/channel", "0.##");

   /**
    * The zero offset for converting channels to energies. The
    * energy=ValueOf(EnergyScale)*channel+ValueOf(EnergyOffset).
    */
   public static final PropertyId EnergyOffset = new PropertyId("Energy offset", " eV", "0.#");

   /**
    * The quadratic term in the energy calibration (rarely used and often
    * ignored.)
    */
   public static final PropertyId EnergyQuadratic = new PropertyId("Energy Quadratic", " eV/ch^2", "0.00000000");

   /**
    * The measured detector resolution at the energy specified by
    * ResolutionLine.
    */
   public static final PropertyId Resolution = new PropertyId("Resolution", " eV", "0.#");

   /**
    * The energy of the line at which the resolution of the detector was
    * measured.
    */
   public static final PropertyId ResolutionLine = new PropertyId("Resolution measurement energy", " eV", "0");

   /**
    * The fano-factor associated with this detector (detector that collected
    * this spectrum)
    */
   public static final PropertyId FanoFactor = new PropertyId("Fano factor", "", "0.##");

   /**
    * The efficiency of the detector for measuring x-rays. Most x-ray detectors
    * measure all x-rays that strike the detector thus the quantum efficiency is
    * 1.0. This concept can be a little ill-defined as some thin detectors may
    * allow higher energy x-rays to pass unabsorbed. The quantum efficiency then
    * is not a constant but depends on energy.
    */
   public static final PropertyId QuantumEfficiency = new PropertyId("Quantum efficiency", "", "0.###");

   /**
    * Integration time per spectrum for parallel data collection in milliseconds
    * [RN] (DTSA)
    */
   public static final PropertyId IntegrationTime = new PropertyId("Integration time", " ms", "0.#");

   /**
    * DwellTime - Dwell time/channel for serial data collection in msec
    */
   public static final PropertyId DwellTime = new PropertyId("Dwell time", " ms", "0.#");

   // Spectrum contextual data
   /**
    * A user-friendly comment describing the data contained within this spectrum
    * object.
    */
   public static final PropertyId SpectrumComment = new PropertyId("Spectrum description", String.class);
   /**
    * SpectrumClass - A user defined descriptor identifying a class of similar
    * spectra or sample types.
    */
   public static final PropertyId SpectrumClass = new PropertyId("Spectrum class", String.class);
   /**
    * SpectrumType - As defined by DTSA
    */
   public static final PropertyId SpectrumType = new PropertyId("Spectrum type", String.class);
   /**
    * IsTheoreticallyGenerated - Was this spectrum generated rather than
    * measured?
    */
   public static final PropertyId IsTheoreticallyGenerated = new PropertyId("Theoretical", Boolean.class);
   /**
    * IsResidualSpectrum - Is this the a residual spectrum?
    */
   public static final PropertyId IsResidualSpectrum = new PropertyId("Residual", Boolean.class);

   /**
    * IsROISpectrum - Is this a ROI spectrum?
    */
   public static final PropertyId IsROISpectrum = new PropertyId("ROI", Boolean.class);

   /**
    * IsStandard - Is this spectrum a standard?
    */
   public static final PropertyId IsStandard = new PropertyId("Is a standard", Boolean.class);

   /**
    * IsEdited - Has the spectrum data been edited.
    */
   public static final PropertyId IsEdited = new PropertyId("Is edited", Boolean.class);

   /**
    * IsTemporary - Is the spectrum of ephemeral use? Temporary spectra are
    * never saved to the database.
    */
   public static final PropertyId IsTemporary = new PropertyId("Is temporary", Boolean.class);

   /**
    * IsDerived - Was this spectrum derived from another spectrum's data?
    */
   public static final PropertyId IsDerived = new PropertyId("Is temporary", Boolean.class);

   /**
    * BackgroundCorrected - Has this spectral data been background corrected?
    */
   public static final PropertyId BackgroundCorrected = new PropertyId("Is background corrected", Boolean.class);

   /**
    * LiveTime - The amount of time during which the detector was available to
    * process x-ray events. (LiveTime&lt;=RealTime)
    */
   public static final PropertyId LiveTime = new PropertyId("Live time", " s", "0.###");
   /**
    * RealTime - The acquisition time (as per clock on wall or equivalent.)
    */
   public static final PropertyId RealTime = new PropertyId("Real time", " s", "0.###");
   public static final PropertyId XUnits = new PropertyId("X Units", String.class);
   public static final PropertyId YUnits = new PropertyId("Y Units", String.class);
   /**
    * SignalType - As specified in the EMSA file format. The detector type from
    * which this data was collected.
    */
   public static final PropertyId SignalType = new PropertyId("Signal type", String.class); // DTSA:
   // "EDS", "WDS", "ELS", "AES", "PES", "XRF", "CLS", "GAM"
   // Instrument properties

   public static final PropertyId StagePosition = new PropertyId("Stage position", StageCoordinate.class);

   /**
    * MassThickness - The mass thickness of a thin-film sample in &mu;g/cm
    * <sup>2</sup>
    */
   public static final PropertyId MassThickness = new PropertyId("Mass-thickness", " µg/cm²", "#,##0.0");

   /**
    * XPosition - The x stage coordinate in millimeters.
    */
   // public static final PropertyId XPosition = new
   // PropertyId("Stage x-position", " mm", "0.###");
   /**
    * YPosition - The y stage coordinate in millimeters.
    */
   // public static final PropertyId YPosition = new
   // PropertyId("Stage y-position", " mm", "0.###");
   /**
    * ZPosition - The z stage coordinate in millimeters.
    */
   // public static final PropertyId ZPosition = new
   // PropertyId("Stage z-position", " mm", "0.##");
   /**
    * StageRotation - The rotation stage coordinate in degrees
    */
   // public static final PropertyId StageRotation = new
   // PropertyId("Stage rotation", "\u00B0", "0.#");
   /**
    * StageTilt - The tilt stage coordinate in degrees
    */
   // public static final PropertyId StageTilt = new PropertyId("Stage tilt",
   // "\u00B0", "0.#");
   /**
    * StageBank - The bank stage coordinate in degrees
    */
   // public static final PropertyId StageBank = new PropertyId("Stage bank",
   // "\u00B0", "0.#");
   /**
    * A measurement of the probe current . (nA)
    */
   public static final PropertyId ProbeCurrent = new PropertyId("Probe current", " nA", "0.000###");
   /**
    * Until Nov-2022, the probe current data could be recorded as measured
    * before the spectrum (FaradayBegin) or after the spectrum (FaradayEnd).
    * This lead to some unfortunate bugs and really was overkill given that
    * todays instruments are so much more stable. Now both FaradayBegin and
    * FaradayEnd are aliases for ProbeCurrent, the one-and-only one property for
    * recording the number of electrons incident on the sample per second. They
    * are retained as aliases to ensure that old Python code continues to work.
    */
   @Deprecated
   public static final PropertyId FaradayBegin = ProbeCurrent;
   @Deprecated
   public static final PropertyId FaradayEnd = ProbeCurrent;

   /**
    * SpotSize - The spot size as defined by the ASPEX software.
    */
   public static final PropertyId SpotSize = new PropertyId("Spot size", "%", "0.#");
   /**
    * ProbeArea - As define by DTSA
    */
   public static final PropertyId ProbeArea = new PropertyId("Probe area", " nm\u00B2", "0.#");
   /**
    * SlowChannelCounts
    */
   public static final PropertyId SlowChannelCounts = new PropertyId("Slow channel counts", " cps", "0");
   /**
    * MediumChannelCounts
    */
   public static final PropertyId MediumChannelCounts = new PropertyId("Medium channel counts", " cps", "0");
   /**
    * FastChannelCounts
    */
   public static final PropertyId FastChannelCounts = new PropertyId("Fast channel counts", " cps", "0");
   /**
    * LLD
    */
   public static final PropertyId LLD = new PropertyId("LLD", "channels", "0");
   /**
    * The ZeroPeakDiscriminator is used to zero out those channels which result
    * from the a zero stablization peak. In eV since the position in channels
    * may shift.
    */
   public static final PropertyId ZeroPeakDiscriminator = new PropertyId("Zero peak discriminator", " eV", "0");
   /**
    * PulseProcessorType
    */
   public static final PropertyId PulseProcessorType = new PropertyId("Pulse processor type", String.class);
   /**
    * PulseProcessTime
    */
   public static final PropertyId PulseProcessTime = new PropertyId("Pulse process time", " \u00B5s", "0.##");
   /**
    * PulseProcessorSetting
    */
   public static final PropertyId PulseProcessorSetting = new PropertyId("Pulse processor setting", String.class);
   /**
    * BeamEnergy - The energy of the electron probe in kiloelectron volts.
    */
   public static final PropertyId BeamEnergy = new PropertyId("Beam energy", " keV", "0.0##");
   /**
    * WorkingDistance - The distance between the pole piece and the sample as
    * implied by the objective current necessary to bring the beam into focus
    * (in mm).
    */
   public static final PropertyId WorkingDistance = new PropertyId("Working distance", " mm", "0.0#");
   /**
    * EmissionCurrent - The emission current in microamps. Not this is the
    * amount of emission from the gun not the amount that strikes the sample.
    */
   public static final PropertyId EmissionCurrent = new PropertyId("Emission current", " \u00B5A", "0.000");
   /**
    * Magnification - The magnification at which the image was acquired. This is
    * ambiguous as the size at which the image is displayed changes the
    * magnification. Nominally I use a 3.5" display size to calculate mag. I
    * can't say what other people do.
    */
   public static final PropertyId Magnification = new PropertyId("Magnification", "\u00D7", "0");

   /**
    * MagnificationZoom - The zoom factor for the micro-image.
    */
   public static final PropertyId MagnificationZoom = new PropertyId("Magnification zoom", "\u00D7", "0.#");

   /**
    * ConvergenceAngle - Convergence semi-angle of incident beam in milliRadians
    * [RN] (EMSA)
    */
   public static final PropertyId ConvergenceAngle = new PropertyId("Convergence angle", " mR", "0.##");

   /**
    * CollectionAngle - Collection semi-angle of scattered beam in mR
    */
   public static final PropertyId CollectionAngle = new PropertyId("Collection angle", " mR", "0.##");
   /**
    * OperatingMode - Operating Mode, allowed values are [5CS]: (DTSA) IMAGE =
    * Imaging Mode DIFFR = Diffraction Mode SCIMG = Scanning Imaging Mode SCDIF
    * = Scanning Diffraction Mode "IMAGE", "DIFFR", "SCIMG", "SCDIF"
    */
   public static final PropertyId OperatingMode = new PropertyId("Operating mode", String.class);
   // Specimen properties
   /**
    * SpecimenThickness - Specimen thickness in nanometers [RN] (DTSA)
    */
   public static final PropertyId SpecimenThickness = new PropertyId("Specimen thickness", " nm", "0.#");
   /**
    * SpecimenDensity - Nominal or estimated specimen density in g/cc. Required
    * to compute film and other non-bulk quantitative corrections.
    */
   public static final PropertyId SpecimenDensity = new PropertyId("Specimen density", " g/cm\u00B2", "0.#");

   /**
    * The StandardComposition should reflect the composition of the sample as
    * determined by an reliable independent method such as wet chemistry.
    */
   public static final PropertyId StandardComposition = new PropertyId("Standard Composition", Composition.class);

   /**
    * This property should describe the best microanalytically derived
    * understanding of composition of the sample.
    */
   public static final PropertyId MicroanalyticalComposition = new PropertyId("Microanalytical Composition", Composition.class);

   /**
    * ParticleSignature - A ParticleSignature object containing the result of a
    * particle signature measurement. This is associated with Graf but defined
    * in EPQLibrary.
    */
   public static final PropertyId ParticleSignature = new PropertyId("Particle Signature", ParticleSignature.class);
   public static final PropertyId ParticleSignature2 = new PropertyId("Particle Signature2", TreeMap.class);

   /**
    * SampleShape - A SampleShape object containing a description of the
    * particle shape.
    */
   public static final PropertyId SampleShape = new PropertyId("Sample Shape", SampleShape.class);

   /**
    * ElementList - A list of the elements contained material represented by the
    * spectrum. Essentially this is the equivalent to the qual list. The
    * ElementList items is stored as a comma separated list of element
    * abbreviations. Benitoite is "Ba, Ti, Si, O"
    */
   public static final PropertyId ElementList = new PropertyId("Element List", String.class);

   /**
    * For what elements is this spectrum a standard?
    */
   public static final PropertyId StandardizedElements = new PropertyId("Standard For", String.class);
   /**
    * KRatios - Describes the results of a fit of reference spectra as a set of
    * k-ratios. (As implemented by KRatioSet.toParseableString and
    * KRatioSet.parseString)
    */
   public static final PropertyId KRatios = new PropertyId("K-Ratios", KRatioSet.class);

   /**
    * OptimalKRatios - The full K-ratio set trimmed down to one k-ratio per
    * element as was deemed optimal by the filter fit and quantitative
    * correction algorithm.
    */
   public static final PropertyId OptimalKRatios = new PropertyId("Optimal K-Ratios", KRatioSet.class);

   /**
    * ChiSquare statistic associated with the most recent fit - the one
    * producing the OptimalKRatios propery.
    */
   public static final PropertyId ChiSquare = new PropertyId("ChiSquare", "", "0.##");

   /**
    * The ReducedChiSquare statistic associated with the most recent fit - the
    * one producing the OptimalKRatios propery.
    */
   public static final PropertyId ReducedChiSquare = new PropertyId("Reduced ChiSquare", "", "0.##");

   /**
    * MacroImage - Associates a macro-view Image with a spectrum.
    */
   public static final PropertyId MacroImage = new PropertyId("Macro image", Image.class);
   /**
    * MicroImage - Associates a microscopic-view Image with a spectrum.
    */
   public static final PropertyId MicroImage = new PropertyId("Micro image", Image.class);

   /**
    * MicroImage2 - Associates a microscopic-view Image with a spectrum. Usually
    * BSED and MicroImage is SED
    */
   public static final PropertyId MicroImage2 = new PropertyId("Micro image2", Image.class);

   /**
    * Contains the filename to which MicroImage and MicroImage2 properties were
    * saved as a TIFF.
    */
   public static final PropertyId ImageRef = new PropertyId("Image Reference", String.class);

   /**
    * AFA_DAvg - The average particle diameter as measured by ASPEX AFA.
    */
   public static final PropertyId AFA_DAvg = new PropertyId("Average diameter", " \u00B5m", "0.##");

   /**
    * ASPEX_BeamX - The location of the beam on the image during the EDS
    * acquisition
    */
   public static final PropertyId BeamSpotX = new PropertyId("Beam position[X]", "", "0");

   /**
    * ASPEX_BeamY - The location of the beam on the image during the EDS
    * acquisition
    */
   public static final PropertyId BeamSpotY = new PropertyId("Beam position[Y]", "", "0");

   /**
    * DuaneHunt - The location of the Duane-Hunt limit (in keV)
    */
   public static final PropertyId DuaneHunt = new PropertyId("Duane-Hunt", " keV", "#.###");

   /**
    * Orientation of a normal to the surface of the EDS detector
    * (out-of-the-face of the detector)
    */
   public static final PropertyId DetectorOrientation = new PropertyId("Detector orientation", Array.class);

   /**
    * The physical location of the x-ray detector relative to the beam axis. See
    * SampleSurfaceNormal for a description of the coordinate system.
    * Preferentially set this property via one of the setDetectorPosition
    * methods.
    */
   public static final PropertyId DetectorPosition = new PropertyId("Detector position", " mm", Array.class);

   /**
    * Some kind of descriptive information about the detector on which a
    * spectrum was acquired. Not the detector name.
    */
   public static final PropertyId DetectorDescription = new PropertyId("Detector description", String.class);

   /**
    * What is the optimal working distance for this detector? (in mm)
    * Preferentially set this property via the setDetectorPosition method.
    */
   public static final PropertyId DetectorOptWD = new PropertyId("Optimal working distance", " mm", Number.class);

   /**
    * Number of stores per second.
    */
   public static final PropertyId OutputCountRate = new PropertyId("Output count rate", " cps", Number.class);

   // Database related properties

   /***
    * The ID column in the SPECTRUM table for this spectrum.
    */
   public static final PropertyId SpectrumDB = new PropertyId("Spectrum DB Index", "", "#");

   /***
    * The MultiSpectrumMetric value associated with this spectrum. 1.0 is
    * perfect agreement. Need to be very close to unity for a good spectrum
    */
   public static final PropertyId MultiSpectrumMetric = new PropertyId("Multi-Spectrum Metric", "", Number.class);

   /***
    * An optional conductive coating on the surface of the sample
    */
   public static final PropertyId ConductiveCoating = new PropertyId("Conductive coating", "", ConductiveCoating.class);

   public static final PropertyId XRFAtmosphere = new PropertyId("XRF Atmosphere", String.class);
   public static final PropertyId XRFFilter = new PropertyId("XRF Source Filter", String.class);
   public static final PropertyId XRFSourceVoltage = new PropertyId("XRF Source Voltage", " keV", Number.class);
   public static final PropertyId XRFTubeCurrent = new PropertyId("XRF Tube Current", " \u00B5A", Number.class);

   /**
    * A human friendly name for the resolution mode of EDS detector
    */
   public static final PropertyId DetectorMode = new PropertyId("Detector Mode", String.class);

   public enum PropertyType {
      // Nominal Property types
      /**
       * Unknown - This property type is undefined or the type unrecognized.
       */
      Unknown,

      /**
       * Numeric - This property type is most naturally representated as a
       * double precision floating point number.
       */
      Numeric,

      /**
       * Textual - This property type is most naturally represented as text.
       */
      Textual,

      /**
       * Boolean - This property type is most naturally represented as a boolean
       * value ('true' or 'false')
       */
      Boolean,

      /**
       * Timestamp - This property type is most naturally represented as a date
       * and time.
       */
      Timestamp,

      /**
       * ImageType - This property type is most naturally represented as an
       * Image.
       */
      ImageType,

      /**
       * CompositionType - This property type is most naturally represented as
       * an Composition object.
       */
      CompositionType,

      /**
       * ArrayType - This property is most naturally represented as an double[]
       */
      ArrayType,

      /**
       * KRatioType - This property is most naturally expressed as a KRatioSet
       */
      KRatioType,

      /**
       * WindowPropertyType - This property is most naturally expressed as an
       * IXRayWindowProperties type
       */
      WindowPropertyType,

      /**
       * A ParticleSignature
       */
      ParticleSignatureType,

      /**
       * A SampleShape object
       */
      SampleShapeType,
   };

   private static final Map<String, PropertyId> preDefinedPropertiesMap = createPredefinedPropertyMap();

   private static Map<String, PropertyId> createPredefinedPropertyMap() {
      final TreeMap<String, PropertyId> res = new TreeMap<String, PropertyId>();
      final PropertyId[] contents = new PropertyId[]{SpectrumIndex, SpecimenName, SpectrumDisplayName, SpecimenDesc, SampleId, ClientsSampleID,
            ClientName, ProjectName, SourceFile, InstrumentOperator, Analyst, Software, AcquisitionTime, Azimuth, Elevation, DetectorArea,
            DetectorThickness, DetectorTilt, DetectorOptWD, TakeOffAngle, DetectorDistance, SolidAngle, WindowType, DiamondWindow, MylarWindow,
            BoronNitrideWindow, SiliconNitrideWindow, SiliconNitrideWindow, ParaleneWindow, PyroleneWindow, MoxtekWindow, HydroCarbonWindow,
            BerylliumWindow, AluminumWindow, BoronLayer, CarbonLayer, NitrogenLayer, OxygenLayer, GoldLayer, NickelLayer, AluminumLayer, IceThickness,
            CarbonCoating, DeadLayer, ActiveLayer, DetectorType, DetectorWindow, EnergyScale, EnergyOffset, EnergyQuadratic, Resolution,
            ResolutionLine, QuantumEfficiency, MassThickness, IntegrationTime, DwellTime, SpectrumComment, SpectrumClass, SpectrumType,
            IsTheoreticallyGenerated, IsResidualSpectrum, IsROISpectrum, IsStandard, IsEdited, IsTemporary, IsDerived, BackgroundCorrected, LiveTime,
            RealTime, DeadPercent, XUnits, YUnits, SignalType, DetectorOrientation, DetectorPosition, StagePosition, ProbeCurrent, SpotSize,
            ProbeArea, SlowChannelCounts, MediumChannelCounts, FastChannelCounts, LLD, ZeroPeakDiscriminator, PulseProcessorType, PulseProcessTime,
            PulseProcessorSetting, BeamEnergy, WorkingDistance, EmissionCurrent, Magnification, MagnificationZoom, ConvergenceAngle, CollectionAngle,
            OperatingMode, SpecimenThickness, SpecimenDensity, StandardComposition, ElementList, StandardizedElements, KRatios, OptimalKRatios,
            ChiSquare, ReducedChiSquare, MacroImage, MicroImage, MicroImage2, AFA_DAvg, BeamSpotX, BeamSpotY, DuaneHunt, SourceFileId, Instrument,
            Detector, FanoFactor, DetectorDescription, ParticleSignature, SampleShape, SpectrumDB, WindowOpenArea, SupportGridThickness,
            CalibrationGUID, DetectorGUID, OutputCountRate, XRFAtmosphere, XRFFilter, XRFSourceVoltage, XRFTubeCurrent, DetectorMode

      };
      for (final PropertyId pid : contents)
         res.put(pid.mName, pid);
      return Collections.unmodifiableMap(res);
   }

   public static List<PropertyId> WINDOW_PROPERTIES = Arrays.asList(new PropertyId[]{WindowType, DiamondWindow, MylarWindow, BoronNitrideWindow,
         SiliconNitrideWindow, SiliconNitrideWindow, ParaleneWindow, PyroleneWindow, MoxtekWindow, HydroCarbonWindow, BerylliumWindow, AluminumWindow,
         // IceThickness,
         // BoronLayer,
         // CarbonLayer,
         // NitrogenLayer,
         // OxygenLayer,
         // NickelLayer,
         // AluminumLayer,
         // GoldLayer

   });

   private static List<PropertyId> DETECTOR_PROPERTIES = Arrays.asList(new PropertyId[]{Azimuth, Elevation, DetectorArea, DetectorThickness,
         DetectorTilt, DetectorOptWD, TakeOffAngle, DetectorDistance, SolidAngle,
         // WindowType,
         // DiamondWindow,
         // MylarWindow,
         // BoronNitrideWindow,
         // SiliconNitrideWindow,
         // SiliconNitrideWindow,
         // ParaleneWindow,
         // PyroleneWindow,
         // MoxtekWindow,
         // HydroCarbonWindow,
         // BerylliumWindow,
         // AluminumWindow,
         GoldLayer, NickelLayer, AluminumLayer, IceThickness, DeadLayer, ActiveLayer, DetectorType, DetectorWindow, EnergyScale, EnergyOffset,
         EnergyQuadratic, Resolution, ResolutionLine, QuantumEfficiency, SignalType, DetectorOrientation, DetectorPosition, LLD, PulseProcessorType,
         PulseProcessTime, PulseProcessorSetting, CollectionAngle, Instrument, Detector, FanoFactor, DetectorDescription, DetectorGUID,
         CalibrationGUID});
   /**
    * mProperty - Contains the list of propertry strings that have been assigned
    * to this SpectrumProperty object. pid = null indicates that this property
    * has been explicitly cleared.
    */
   private SortedMap<PropertyId, Object> mProperty;

   private transient int mHashCode = Integer.MAX_VALUE;

   private final Object findProperty(PropertyId pid) {
      // Check locally first then if not available check the parent
      return mProperty.containsKey(pid) ? mProperty.get(pid) : null;
   }

   public Object getObjectWithDefault(PropertyId pid, Object def) {
      final Object res = findProperty(pid);
      return res != null ? res : def;
   }

   public void setObjectProperty(PropertyId pid, Object val) {
      mProperty.put(pid, val);
   }

   public Object getObjectProperty(PropertyId pid) throws EPQException {
      final Object res = findProperty(pid);
      if (res == null)
         throw new EPQException("The property " + pid + " is not defined.");
      return res;
   }

   public void removeAll(Collection<PropertyId> propArray) {
      for (final PropertyId pid : propArray)
         mProperty.remove(pid);
   }

   public void remove(PropertyId pid) {
      mProperty.remove(pid);
   }

   public void setDetector(IXRayDetector det) {
      removeAll(DETECTOR_PROPERTIES);
      if (det != null) {
         addAll(det.getProperties());
         addAll(det.getCalibration().getProperties());
         setObjectProperty(SpectrumProperties.Detector, det);
         setObjectProperty(SpectrumProperties.Instrument, det.getDetectorProperties().getOwner());
      } else {
         mProperty.remove(SpectrumProperties.Detector);
         mProperty.remove(SpectrumProperties.Instrument);
      }
   }

   public IXRayDetector getDetector() {
      return (IXRayDetector) getObjectWithDefault(SpectrumProperties.Detector, null);
   }

   /**
    * SpectrumProperties - Construct an empty SpectrumProperties object.
    */
   public SpectrumProperties() {
      mProperty = new TreeMap<PropertyId, Object>();
   }

   /**
    * SpectrumProperties - Construct a SpectrumProperties object that is a
    * duplicate of the parentProps object.
    * 
    * @param parentProps
    *           SpectrumProperties
    */
   public SpectrumProperties(SpectrumProperties parentProps) {
      mProperty = new TreeMap<PropertyId, Object>();
      addAll(parentProps);
   }

   public Object readResolve() {
      mHashCode = Integer.MAX_VALUE;
      return this;
   }

   /**
    * Returns true if the specified property (pid) is defined (non-null) in both
    * this and sp1 and the associated values are equal(..)
    * 
    * @param sp2
    * @param pid
    * @return boolean
    */
   public boolean equalProperty(SpectrumProperties sp2, PropertyId pid) {
      final Object o1 = findProperty(pid);
      if (o1 == null)
         return false;
      final Object o2 = sp2.findProperty(pid);
      if (o2 == null)
         return false;
      return (o1 == o2) || (o1.getClass().equals(o2.getClass()) && o1.equals(o2));
   }

   /**
    * Create a new SpectrumProperty object containing all the properties which
    * are defined equivalently in both sp1 and sp2.
    * 
    * @param sp1
    *           (may be null)
    * @param sp2
    * @return SpectrumProperties
    */
   static public SpectrumProperties merge(SpectrumProperties sp1, SpectrumProperties sp2) {
      SpectrumProperties res;
      if (sp1 != null) {
         res = new SpectrumProperties();
         for (final PropertyId pd1 : sp1.getPropertySet())
            if (sp1.equalProperty(sp2, pd1))
               res.mProperty.put(pd1, sp1.findProperty(pd1));
      } else
         res = sp2;
      return res;
   }

   /**
    * Returns those properties in sp1 that are not in sp2.
    * 
    * @param sp1
    *           SpectrumProperties
    * @param sp2
    *           SpectrumProperties
    * @return SpectrumProperties
    */
   static public SpectrumProperties difference(SpectrumProperties sp1, SpectrumProperties sp2) {
      final SpectrumProperties res = new SpectrumProperties();
      for (final Entry<PropertyId, Object> me : sp1.mProperty.entrySet()) {
         final Object o1 = me.getValue();
         final Object o2 = sp2.getObjectWithDefault(me.getKey(), null);
         if ((o2 == null) || (!o1.equals(o2)))
            res.mProperty.put(me.getKey(), o1);
      }
      return res;
   }

   /**
    * clone - Creates an exact duplicate of this SpectrumProperties object.
    * 
    * @return SpectrumProperties
    */
   @Override
   public SpectrumProperties clone() {
      final SpectrumProperties sp = new SpectrumProperties();
      for (final Map.Entry<PropertyId, Object> me : mProperty.entrySet())
         sp.mProperty.put(me.getKey(), me.getValue());
      sp.mHashCode = mHashCode;
      return sp;
   }

   /**
    * getTextProperty - Get the textual property identified by the specified
    * pid. This method throws an exception if the named property is not found.
    * 
    * @param pid
    *           String
    * @throws EPQException
    * @return String
    */
   public String getTextProperty(PropertyId pid) throws EPQException {
      final Object res = getObjectProperty(pid);
      switch (pid.getPropertyType()) {
         case Textual :
            return res.toString();
         case Timestamp : {
            final Date dt = new Date(Math.round(((Double) res).doubleValue()));
            final DateFormat df = DateFormat.getInstance();
            return df.format(dt);
         }
         case Numeric : {
            String tmp;
            if ((pid.mFormatStr != null) && (res instanceof Number)) {
               final NumberFormat df = new HalfUpFormat(pid.getFormatString());
               tmp = UncertainValue2.format(df, (Number) res);
            } else
               tmp = res.toString();
            final String units = pid.getUnits();
            if (pid.mName.length() > 0)
               return tmp + units;
            else
               return tmp;
         }
         case CompositionType :
            return ((Composition) res).toString();
         case ArrayType : {
            final NumberFormat df = new HalfUpFormat("0.000");
            final StringBuffer sb = new StringBuffer();
            for (final double d : (double[]) res) {
               sb.append(sb.length() == 0 ? "[" : ",");
               sb.append(df.format(d));
            }
            sb.append("]");
            return sb.toString();
         }
         case ImageType :
            return res instanceof BufferedImage
                  ? Integer.toString(((BufferedImage) res).getWidth()) + " \u00D7 " + Integer.toString(((BufferedImage) res).getHeight()) + " image"
                  : "Image";
         case KRatioType :
            return res.toString();
         case WindowPropertyType :
            return "Window: " + res.toString();
         default :
            return res.toString();
      }
   }

   /**
    * Get the textual property identified by the specified pid without the unit
    * string. This method throws an exception if the named property is not
    * found.
    * 
    * @param pid
    *           String
    * @throws EPQException
    * @return String
    */
   public String getTextProperty_NoUnit(PropertyId pid) throws EPQException {
      final Object res = getObjectProperty(pid);
      switch (pid.getPropertyType()) {
         case Textual :
            return res.toString();
         case Timestamp :
            final Date dt = new Date(Math.round(((Double) res).doubleValue()));
            return dt.toString();
         case Numeric : {
            String tmp;
            if (pid.mFormatStr != null) {
               final NumberFormat df = new HalfUpFormat(pid.getFormatString());
               tmp = UncertainValue2.format(df, (Number) res);
            } else
               tmp = res.toString();
            return tmp;
         }
         case CompositionType :
            return ((Composition) res).descriptiveString(false);
         case ArrayType :
            return Arrays.toString((double[]) res);
         case KRatioType :
            return res.toString();
         case WindowPropertyType :
            return "Window: " + res.toString();
         default :
            return res.toString();
      }
   }

   /**
    * Similar to getTextProperty except instead of throwing an exception, this
    * version returns the default value (def) if the property is not defined.
    * 
    * @param pid
    *           String
    * @param def
    *           String
    * @return String
    */
   public String getTextWithDefault(PropertyId pid, String def) {
      try {
         return getTextProperty(pid);
      } catch (final EPQException e) {
         return def;
      }
   }

   /**
    * Similar to getTextProperty_NoUnit except instead of throwing an exception,
    * this version returns the default value (def) if the property is not
    * defined.
    * 
    * @param pid
    *           String
    * @param def
    *           String
    * @return String
    */
   public String getTextWithDefault_NoUnit(PropertyId pid, String def) {
      try {
         return getTextProperty_NoUnit(pid);
      } catch (final EPQException e) {
         return def;
      }
   }

   /**
    * setTextProperty - Sets the value (val) of the specified (pid) property.
    * Any previous definition is overwritten.
    * 
    * @param pid
    *           String
    * @param val
    *           String
    */
   public void setTextProperty(PropertyId pid, String val) {
      assert (SpectrumProperties.isPreDefinedProperty(pid.mName));
      assert pid.mType == String.class : pid.mName;
      mProperty.put(pid, val.trim());
   }

   /**
    * getNumericProperty - Gets the numerical value of the specified property
    * (pid). If the property is not defined or if it is not representable as a
    * double then the method raises an EPQException.
    * 
    * @param pid
    *           String
    * @throws EPQException
    * @return double
    */
   public double getNumericProperty(PropertyId pid) throws EPQException {
      final Object res = getObjectProperty(pid);
      if (res instanceof Double)
         return ((Double) res).doubleValue();
      else if (res instanceof String)
         return Double.parseDouble((String) res);
      else
         throw new EPQException("The property " + pid + " is of unexpected type.");
   }

   /**
    * getNumericWithDefault - Similar to getNumericProperty except that instead
    * of throwing an Exception, the default value (def) is returned if
    * getNumericProperty would throw an exception.
    * 
    * @param pid
    *           String
    * @param def
    *           double
    * @return double
    */
   public double getNumericWithDefault(PropertyId pid, double def) {
      final Object res = findProperty(pid);
      if (res == null)
         return def;
      else if (res instanceof Number)
         return checkNotNaN(((Number) res).doubleValue(), def);
      else if (res instanceof String)
         return checkNotNaN(Double.parseDouble((String) res), def);
      else
         return def;
   }

   private static double checkNotNaN(double d, double def) {
      return Double.isNaN(d) ? def : d;
   }

   /**
    * setNumericProperty - Sets the named property's value to the specified
    * number.
    * 
    * @param pid
    *           String
    * @param val
    *           double
    */
   public void setNumericProperty(PropertyId pid, double val) {
      assert (SpectrumProperties.isPreDefinedProperty(pid.mName));
      mProperty.put(pid, Double.valueOf(val));
   }

   /**
    * setNumericProperty - Sets the named property's value to the specified
    * number.
    * 
    * @param pid
    *           String
    * @param val
    *           double
    */
   public void setNumericProperty(PropertyId pid, Number val) {
      assert (SpectrumProperties.isPreDefinedProperty(pid.mName));
      mProperty.put(pid, val);
   }

   public Number getNumericWithDefault(PropertyId pid, Number defVal) {
      return (Number) getObjectWithDefault(pid, defVal);
   }

   /**
    * setArrayProperty - Sets the specified array property to a double[]
    * 
    * @param pid
    * @param val
    *           double[]
    */
   public void setArrayProperty(PropertyId pid, double[] val) {
      assert (SpectrumProperties.isPreDefinedProperty(pid.mName));
      assert (pid.mType == Array.class);
      mProperty.put(pid, val.clone());
   }

   /**
    * getArrayProperty - Gets the values contained within the specified array
    * property
    * 
    * @param pid
    * @return double[]
    * @throws EPQException
    */
   public double[] getArrayProperty(PropertyId pid) throws EPQException {
      return (double[]) getObjectProperty(pid);
   }

   /**
    * getArrayProperty - Gets the values contained within the specified array
    * property if it exists or otherwise returns the values in def.
    * 
    * @param pid
    * @param def
    * @return double[]
    */
   public double[] getArrayWithDefault(PropertyId pid, double[] def) {
      final Object res = findProperty(pid);
      return res == null ? def : (double[]) res;
   }

   public void setKRatioProperty(PropertyId pid, KRatioSet krs) {
      assert (SpectrumProperties.isPreDefinedProperty(pid.mName));
      mProperty.put(pid, krs);
   }

   public KRatioSet getKRatioProperty(PropertyId pid) throws EPQException {
      return (KRatioSet) getObjectProperty(pid);
   }

   public KRatioSet getKRatioWithDefault(PropertyId pid, KRatioSet def) {
      final Object res = findProperty(pid);
      return res == null ? def : (KRatioSet) res;
   }

   /**
    * setImageProperty - Associates an image with a spectrum.
    * 
    * @param pid
    *           String
    * @param img
    *           Image
    */
   public void setImageProperty(PropertyId pid, Image img) {
      assert (SpectrumProperties.isPreDefinedProperty(pid.mName));
      assert (pid.mType == Image.class);
      mProperty.put(pid, img);
   }

   /**
    * getImageProperty - Get an image associated with a spectrum.
    * 
    * @param pid
    *           String
    * @return Image
    * @throws EPQException
    */
   public Image getImageProperty(PropertyId pid) throws EPQException {
      return (Image) getObjectProperty(pid);
   }

   /**
    * getCompositionProperty - Get the Composition object associated with the
    * specified PropertyId
    * 
    * @param pid
    * @return The Composition
    * @throws EPQException
    */
   public Composition getCompositionProperty(PropertyId pid) throws EPQException {
      return (Composition) getObjectProperty(pid);
   }

   /**
    * getCompositionWithDefault - Get the Composition associated with the
    * specified PropertyId. If none is defined return comp.
    * 
    * @param pid
    * @param comp
    * @return The Composition
    */
   public Composition getCompositionWithDefault(PropertyId pid, Composition comp) {
      final Object res = findProperty(pid);
      return (res != null) && (res instanceof Composition) ? (Composition) res : comp;
   }

   /**
    * setCompositionProperty - Sets the Composition associated with the
    * specified PropertyId
    * 
    * @param pid
    * @param comp
    */
   public void setCompositionProperty(PropertyId pid, Composition comp) {
      assert ((pid.mType == Composition.class) || (pid.mType == Material.class));
      mProperty.put(pid, comp);
   }

   /**
    * getParticleSignatureProperty - Get the ParticleSignature object associated
    * with the specified PropertyId
    * 
    * @param pid
    * @return The ParticleSignature
    * @throws EPQException
    */
   public ParticleSignature getParticleSignatureProperty(PropertyId pid) throws EPQException {
      return (ParticleSignature) getObjectProperty(pid);
   }

   /**
    * getParticleShapeProperty - Get the BrownJTA1982p.Shape object associated
    * with the specified PropertyId
    * 
    * @param pid
    * @return The ParticleSignature
    * @throws EPQException
    */
   public SampleShape getSampleShapeProperty(PropertyId pid) throws EPQException {
      return (SampleShape) getObjectProperty(pid);
   }

   /**
    * getParticleSignatureWithDefault - Get the ParticleSignature associated
    * with the specified PropertyId. If none is defined return sig.
    * 
    * @param pid
    * @param sig
    * @return The ParticleSignature
    */
   public ParticleSignature getParticleSignatureWithDefault(PropertyId pid, ParticleSignature sig) {
      final Object res = findProperty(pid);
      return (res != null) && (res instanceof ParticleSignature) ? (ParticleSignature) res : sig;
   }

   /**
    * getParticleShapeWithDefault - Get the BrownJTA1982p.Shape associated with
    * the specified PropertyId. If none is defined return sig.
    * 
    * @param pid
    * @param sig
    * @return The BrownJTA1982p.Shape
    */
   public SampleShape getSampleShapeWithDefault(PropertyId pid, SampleShape sig) {
      final Object res = findProperty(pid);
      return res instanceof SampleShape ? (SampleShape) res : sig;
   }

   /**
    * setParticleSignatureProperty - Sets the ParticleSignature associated with
    * the specified PropertyId
    * 
    * @param pid
    * @param sig
    */
   public void setParticleSignatureProperty(PropertyId pid, ParticleSignature sig) {
      assert (pid.mType == ParticleSignature.class);
      mProperty.put(pid, sig);
   }

   /**
    * setParticleSignatureProperty - Sets the ParticleSignature associated with
    * the specified PropertyId
    * 
    * @param pid
    * @param sig
    */
   public void setSampleShape(PropertyId pid, SampleShape sig) {
      assert (pid.mType == SampleShape.class);
      mProperty.put(pid, sig);
   }

   /**
    * setBooleanProperty - Sets the named property to the specified boolean
    * value.
    * 
    * @param pid
    *           String
    * @param val
    *           boolean
    */
   public void setBooleanProperty(PropertyId pid, boolean val) {
      assert (pid.mType == Boolean.class);
      mProperty.put(pid, val ? "true" : "false");
   }

   /**
    * getBooleanProperty - Returns the boolean value associated with the named
    * property. This method throws an exception if the property is not defined.
    * 
    * @param pid
    *           String
    * @throws EPQException
    * @return boolean
    */
   public boolean getBooleanProperty(PropertyId pid) throws EPQException {
      final Object res = getObjectProperty(pid);
      return ((String) res).compareToIgnoreCase("true") == 0;
   }

   /**
    * getBooleanWithDefault - Similar to the getBooleanProperty method except it
    * returns the default value instead of throwing an exception if the named
    * property is not defined.
    * 
    * @param pid
    *           String
    * @param def
    *           boolean
    * @return boolean
    */
   public boolean getBooleanWithDefault(PropertyId pid, boolean def) {
      final Object res = findProperty(pid);
      return res != null ? ((String) res).compareToIgnoreCase("true") == 0 : def;
   }

   /**
    * getTimestampProperty - Gets the date and time associated with the
    * specified property.
    * 
    * @param pid
    *           PropertyId
    * @return Date
    * @throws EPQException
    */
   public Date getTimestampProperty(PropertyId pid) throws EPQException {
      final Object res = getObjectProperty(pid);
      return new Date(Math.round(((Double) res).doubleValue()));
   }

   /**
    * getTimestampWithDefault - Similar to getTimestampProperty except that
    * instead of throwing an EPQException, the default value (def) is returned
    * if getTimestampProperty would throw an exception.
    * 
    * @param pid
    *           String
    * @param def
    *           Date
    * @return Date
    */
   public Date getTimestampWithDefault(PropertyId pid, Date def) {
      try {
         return getTimestampProperty(pid);
      } catch (final EPQException e) {
         return def;
      }
   }

   /**
    * setTimestampProperty - Sets the named property to the specified timestamp
    * value.
    * 
    * @param pid
    *           String
    * @param val
    *           Timestamp
    */
   public void setTimestampProperty(PropertyId pid, Date val) {
      assert (SpectrumProperties.isPreDefinedProperty(pid.mName));
      assert (pid.mType == Date.class);
      mProperty.put(pid, Double.valueOf(val.getTime()));
   }

   /**
    * Sets the properties associated with the position and orientation of the
    * x-ray detector. Uses an algorithm which *assumes* that at the detector
    * points directly at the point (0.0, 0.0, optWorkingDistance) from the
    * specified elevation and azimuth. To determine the optWorkingDistance
    * determine the point at which the detector points on the beam axis. Place a
    * sample there. Focus on the sample and then record the working distance.
    * 
    * @param elevation
    *           - Elevation up from the x-y plane (radians)
    * @param azimuth
    *           - Measured from the x-axis positive towards the y-axis (radians)
    * @param distance
    *           - Distance from (0.0,0.0,optWorkingDistance) to the detector (in
    *           meters)
    * @param optWorkingDistance
    *           - The optimal working distance (in meters)
    */
   public void setDetectorPosition(double elevation, double azimuth, double distance, double optWorkingDistance) {
      assert distance > 0.0;
      // Convert to mm
      distance *= 1.0e3;
      optWorkingDistance *= 1.0e3;
      setNumericProperty(SpectrumProperties.Elevation, Math.toDegrees(elevation));
      setNumericProperty(SpectrumProperties.Azimuth, Math.toDegrees(azimuth));
      setNumericProperty(SpectrumProperties.DetectorDistance, distance);
      setNumericProperty(SpectrumProperties.DetectorOptWD, optWorkingDistance);
      while (optWorkingDistance > 1000.0)
         optWorkingDistance /= 1000.0;
      final double[] orientation = new double[]{-Math.cos(elevation) * Math.cos(azimuth), -Math.cos(elevation) * Math.sin(azimuth),
            Math.sin(elevation)};
      setArrayProperty(SpectrumProperties.DetectorOrientation, orientation);
      // Position in mm
      final double[] position = Math2.minus(Math2.multiply(optWorkingDistance, Math2.Z_AXIS), Math2.multiply(distance, orientation));
      setArrayProperty(SpectrumProperties.DetectorPosition, position);
   }

   public void setConductiveCoating(ConductiveCoating cc) {
      setObjectProperty(SpectrumProperties.ConductiveCoating, cc);
   }

   public ConductiveCoating getConductiveCoating() {
      return (ConductiveCoating) getObjectWithDefault(SpectrumProperties.ConductiveCoating, null);
   }

   /**
    * Set the position of the detector and associated properties.
    * 
    * @param pos
    *           double[3] in meters
    * @param optWd
    *           in meters
    */
   public void setDetectorPosition(double[] pos, double optWd) {
      assert Math2.magnitude(pos) > 1.0e-3;
      assert Math2.magnitude(pos) < 2.0;
      setNumericProperty(SpectrumProperties.DetectorOptWD, 1.0e3 * optWd);
      final double[] sampPos = Math2.multiply(optWd, Math2.Z_AXIS);
      setArrayProperty(SpectrumProperties.DetectorPosition, Math2.multiply(1.0e3, pos));
      setNumericProperty(SpectrumProperties.Azimuth, Math.toDegrees(Math.atan2(pos[1], pos[0])));
      setNumericProperty(SpectrumProperties.Elevation, Math.toDegrees(Math.atan2(Math.sqrt((pos[0] * pos[0]) + (pos[1] * pos[1])), pos[2] - optWd)));
      setNumericProperty(SpectrumProperties.DetectorDistance, Math2.distance(sampPos, pos) * 1.0e3);
   }

   public void setWindow(IXRayWindowProperties ixrwp) {
      removeAll(WINDOW_PROPERTIES);
      mProperty.put(SpectrumProperties.DetectorWindow, ixrwp);
      addAll(ixrwp.getProperties());
   }

   public IXRayWindowProperties getWindowProperty(PropertyId pid) throws EPQException {
      return (IXRayWindowProperties) getObjectProperty(pid);
   }

   public IXRayWindowProperties getWindowWithDefault(PropertyId pid, IXRayWindowProperties ixrwp) {
      Object res = findProperty(pid);
      if (res == null)
         res = ixrwp;
      return res instanceof IXRayWindowProperties ? (IXRayWindowProperties) res : null;
   }

   /**
    * clear - Clears the defintion of all properties.
    */
   public void clear() {
      for (final Map.Entry<PropertyId, Object> me : mProperty.entrySet())
         me.setValue(null);
   }

   /**
    * clearAllBut - Clears all the SpectrumProperties.PropertyId objects in the
    * SpectrumProperties object except the ones listed.
    * 
    * @param props
    *           PropertyId[]
    */
   public void clearAllBut(PropertyId[] props) {
      final List<PropertyId> lp = Arrays.asList(props);
      for (final Map.Entry<PropertyId, Object> me : mProperty.entrySet())
         if (!lp.contains(me.getKey()))
            me.setValue(null);
   }

   /**
    * clear - Clear the specified properties identified by this list of
    * PropertyId objects from this SpectrumProperties.
    * 
    * @param props
    *           PropertyId[]
    */
   public void clear(PropertyId... props) {
      for (final PropertyId prop : props)
         mProperty.put(prop, null);
   }

   /**
    * isDefined - Is the specified property defined in this SpectrumProperties
    * object?
    * 
    * @param pid
    *           String
    * @return boolean
    */
   public boolean isDefined(PropertyId pid) {
      return findProperty(pid) != null;
   }

   /**
    * getPropertySet - Returns a list of the PropertyId objects associated with
    * all the properties available for this spectrum.
    * 
    * @return Set&lt;PropertyId&gt; - A Set of PropertyId objects
    */
   public Set<PropertyId> getPropertySet() {
      return new TreeSet<PropertyId>(mProperty.keySet());
   }

   public Map<PropertyId, Object> getPropertyMap() {
      return new TreeMap<PropertyId, Object>(mProperty);
   }

   @Override
   public int hashCode() {
      if (mHashCode == Integer.MAX_VALUE) {
         final int prime = 31;
         int result = 1;
         result = (prime * result) + ((mProperty == null) ? 0 : mProperty.hashCode());
         if (result == Integer.MAX_VALUE)
            result = Integer.MIN_VALUE;
      }
      return mHashCode;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      final SpectrumProperties other = (SpectrumProperties) obj;
      if (mProperty == null) {
         if (other.mProperty != null)
            return false;
      } else if (!mProperty.equals(other.mProperty))
         return false;
      return true;
   }

   /**
    * getPropertyType - Gets the actual property type associated with this
    * property (in contrast with what PropertyId.getPropertyType says). They had
    * better agree!!!
    * 
    * @param pid
    *           PropertyId
    * @return PropertyType
    */
   public PropertyType getPropertyType(PropertyId pid) {
      PropertyType res = PropertyType.Unknown;
      final Object cls = findProperty(pid).getClass();
      if (cls.equals(Number.class))
         res = PropertyType.Numeric;
      else if (cls.equals(Date.class))
         res = PropertyType.Timestamp;
      else if (cls.equals(Boolean.class))
         res = PropertyType.Boolean;
      else if (cls.equals(Image.class))
         res = PropertyType.ImageType;
      else if (cls.equals(String.class))
         res = PropertyType.Textual;
      else if (cls.equals(Composition.class))
         res = PropertyType.CompositionType;
      else if (cls.equals(KRatioSet.class))
         res = PropertyType.KRatioType;
      else if (cls.equals(ParticleSignature.class))
         res = PropertyType.ParticleSignatureType;
      assert (res == pid.getPropertyType());
      return res;
   }

   public static Class<?> propertyTypeToClass(PropertyType propertyType) {
      switch (propertyType) {
         case Numeric :
            return Number.class;
         case Timestamp :
            return Date.class;
         case Boolean :
            return Boolean.class;
         case ImageType :
            return Image.class;
         case Textual :
            return String.class;
         case CompositionType :
            return Composition.class;
         case WindowPropertyType :
            return IXRayWindowProperties.class;
         case KRatioType :
            return KRatioSet.class;
         case ParticleSignatureType :
            return ParticleSignature.class;
         case SampleShapeType :
            return SampleShape.class;
         case Unknown :
         case ArrayType :
            return null;
      }
      return null;
   }

   public static boolean isPreDefinedProperty(String propertyName) {
      return preDefinedPropertiesMap.containsKey(propertyName);
   }

   public static PropertyId findPreDefinedPropertyId(String propertyName) {
      return preDefinedPropertiesMap.get(propertyName);
   }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer(2048);
      sb.append("SpectrumProperties[");
      for (final SpectrumProperties.PropertyId pid : mProperty.keySet()) {
         sb.append(pid.toString());
         sb.append("=");
         sb.append(getTextWithDefault(pid, "?"));
         sb.append(",");
      }
      sb.append("]");
      return sb.toString();
   }

   /**
    * writeObject - Implements serializability
    * 
    * @param out
    * @throws IOException
    */
   private void writeObject(java.io.ObjectOutputStream out) throws IOException {
      out.writeObject(null);
      out.writeInt(mProperty.size());
      for (final Map.Entry<PropertyId, Object> me : mProperty.entrySet()) {
         out.writeObject(me.getKey().mName);
         out.writeObject(me.getValue());
      }
   }

   /**
    * readObject - Implements serializability
    * 
    * @param in
    * @throws IOException
    * @throws ClassNotFoundException
    */
   private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
      final SpectrumProperties parent = (SpectrumProperties) in.readObject();
      final int sz = in.readInt();
      mHashCode = Integer.MAX_VALUE;
      mProperty = new TreeMap<PropertyId, Object>();
      if (parent != null)
         addAll(parent);
      for (int i = 0; i < sz; ++i) {
         final String propName = (String) in.readObject();
         PropertyId pid = findPreDefinedPropertyId(propName);
         if (pid == null)
            pid = new PropertyId(propName, "", "");
         try {
            Object obj = in.readObject();
            mProperty.put(pid, obj);
         } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(pid);
            throw e;
         }
      }
   }

   /**
    * Add all the properties from props into this SpectrumProperties object
    * overwriting duplicate elements in this.
    * 
    * @param props
    */
   public void addAll(SpectrumProperties props) {
      for (final Map.Entry<PropertyId, Object> me : props.mProperty.entrySet())
         if (me.getKey().equals(SpectrumProperties.Detector))
            setDetector((IXRayDetector) me.getValue());
         else if (me.getKey().equals(SpectrumProperties.DetectorWindow))
            setWindow((IXRayWindowProperties) me.getValue());
         else if (me.getValue() == null)
            mProperty.remove(me.getKey());
         else
            mProperty.put(me.getKey(), me.getValue());
   }

   /**
    * Add the properties in props that are not already defined in this
    * SpectrumProperties object. Does not overwrite previously define
    * properties. (see addAll which does overwrite previously defined
    * properties.)
    * 
    * @param props
    */
   public void apply(SpectrumProperties props) {
      for (final Map.Entry<PropertyId, Object> me : props.mProperty.entrySet())
         if (!isDefined(me.getKey()))
            mProperty.put(me.getKey(), me.getValue());
   }

   public Set<Element> getStandardizedElements() {
      Set<Element> selms = new TreeSet<Element>();
      if (isDefined(StandardizedElements)) {
         String se = (String) getObjectWithDefault(StandardizedElements, "");
         String[] vals = se.split(",");
         for (String val : vals) {
            final Element elm = Element.byName(val.trim());
            if (!elm.equals(Element.None))
               selms.add(elm);
         }
      }
      return selms;
   }

   public void setStandardizedElements(Collection<Element> elms) {
      Set<Element> selms = new TreeSet<Element>(elms);
      StringBuffer sb = new StringBuffer();
      boolean first = true;
      for (Element elm : selms) {
         if (!first)
            sb.append(", ");
         sb.append(elm.toAbbrev());
         first = false;
      }
      setObjectProperty(StandardizedElements, sb.toString());
   }

   /**
    * Checks in the presumed order of reliablity for a list of the elements
    * represented by this spectrum - StandardComposition first, ElementList
    * second and MicroanalyticalComposition last.
    * 
    * @return Set&lt;Element&gt;
    */
   public Set<Element> getElements() {
      Set<Element> res = null;
      Composition comp = getCompositionWithDefault(StandardComposition, null);
      if (comp == null) {
         final String elms = getTextWithDefault(ElementList, null);
         if (elms != null) {
            // Parse the list
            res = new TreeSet<Element>();
            int end = 0;
            for (int p = elms.indexOf(','); p != -1; p = elms.indexOf(',', end)) {
               final Element elm = Element.byName(elms.substring(end, p).trim());
               if (elm != Element.None)
                  res.add(elm);
               end = p + 1;
            }
            final Element elm = Element.byName(elms.substring(end).trim());
            if (elm != Element.None)
               res.add(elm);
         }
         if (res == null)
            comp = getCompositionWithDefault(MicroanalyticalComposition, null);
      }
      if (comp != null) {
         assert res == null;
         res = comp.getElementSet();
      }
      return res;
   }

   /**
    * Set the ElementList property from a collection of Element objects.
    * 
    * @param elms
    */
   public void setElements(Collection<Element> elms) {
      final StringBuffer sb = new StringBuffer();
      for (final Element elm : elms) {
         if (sb.length() > 0)
            sb.append(",");
         sb.append(elm.toAbbrev());
      }
      setTextProperty(ElementList, sb.toString());
   }

   /**
    * Returns the SpectrumProperties list as an HTML table.
    * 
    * @return String
    */
   public String asHTML() {
      final StringWriter sw = new StringWriter();
      final PrintWriter pw = new PrintWriter(sw);
      pw.println("<table>");
      pw.println("<tr><th>Property</th><th>Value</th></tr>");
      for (final PropertyId pid : getPropertySet()) {
         pw.print("<tr><td>");
         pw.print(pid.getName());
         pw.print("</td><td>");
         try {
            pw.print(TextUtilities.normalizeHTML(getTextProperty(pid)));
         } catch (final EPQException e) {
            pw.print("?");
         }
         pw.print("</td></tr>");
      }
      pw.println("</table>");
      return sw.toString();
   }

   public Object getPropertyByName(String name) {
      for (final Map.Entry<PropertyId, Object> me : mProperty.entrySet())
         if (me.getKey().toString().equals(name))
            return me.getValue();
      return null;
   }

   public String asURL(ISpectrumData spec) {
      final StringBuffer sb = new StringBuffer();
      final String fn = getTextWithDefault(SpectrumProperties.SourceFile, null);
      boolean written = false;
      if (fn != null)
         try {
            final File f = new File(fn);
            sb.append("<A HREF=\"");
            sb.append(f.toURI().toURL().toExternalForm());
            sb.append("\">");
            sb.append(spec.toString());
            sb.append("</A>");
            written = true;
         } catch (final MalformedURLException e) {
            // Ignore it...
         }
      if (!written)
         sb.append(spec.toString());
      return sb.toString();

   }

}
