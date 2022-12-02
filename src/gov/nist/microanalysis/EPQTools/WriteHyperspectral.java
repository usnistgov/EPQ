package gov.nist.microanalysis.EPQTools;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate;

public class WriteHyperspectral {

   public enum Unit {
      Meter("m"), Second("s"), None("1");

      private final String mAbbrev;

      private Unit(String abbrev) {
         mAbbrev = abbrev;
      }
   };

   public enum DataType {
      ElectronProbeEDS("eds"), XRayFluorescence("xrf"), ElectronEnergyLoss("eels"), CathodeLuminescence("cl"), Image("img");
      private final String mAbbrev;

      private DataType(String abbrev) {
         mAbbrev = abbrev;
      }
   }

   private final ArrayList<Dimension> mDimensions = new ArrayList<Dimension>();
   private final Charset mCharSet = Charset.forName("UTF-8");

   static final TreeMap<String, String> StageUnits = createStageUnits();
   static final String SPACES = "   ";

   private static TreeMap<String, String> createStageUnits() {
      final TreeMap<String, String> res = new TreeMap<String, String>();
      res.put("x", "m");
      res.put("y", "m");
      res.put("z", "m");
      res.put("bank", "degrees");
      res.put("rotation", "degrees");
      res.put("tilt", "degrees");
      return res;
   }

   private class Dimension {
      final private String mName;
      final private Unit mUnit;
      final private double mOffset;
      final private double mExtent;
      final int mLength;

      private Dimension(String name, Unit unit, double offset, double ext, int len) {
         mName = name;
         assert (mName != null) && (mName.length() > 0);
         mUnit = unit;
         mOffset = offset;
         mExtent = ext;
         mLength = len;
      }

      private void write(PrettyPrintWriter ppw) {
         ppw.startNode("axis");
         ppw.addAttribute("length", Integer.toString(mLength));
         if (!Double.isNaN(mExtent)) {
            ppw.addAttribute("unit", mUnit.mAbbrev);
            ppw.addAttribute("extent", Double.toString(mExtent));
            if (!Double.isNaN(mOffset))
               ppw.addAttribute("offset", Double.toString(mOffset));
         }
         ppw.setValue(mName);
         ppw.endNode();
      }
   }

   static class ImageHeader extends DataHeader {
      private final double[] mFOV;
      private final String mDescription;
      private final String mSignal;
      private BufferedImage mImage;

      private ImageHeader(ScaledImage sc, String name, String desc, String signal) {
         super(DataType.Image, name);
         mFOV = new double[]{sc.getHorizontalFieldOfView(), sc.getVerticalFieldOfView()};
         mDescription = desc;
         mSignal = signal;
      }

      protected String toXML(ImageHeader base) {
         final StringWriter sw = new StringWriter();
         final PrettyPrintWriter ppw = new PrettyPrintWriter(sw);
         int items = 0;
         if ((base == null) || (!Arrays.equals(mFOV, base.mFOV))) {
            ppw.startNode("field-of-view");
            ppw.addAttribute("unit", "m");
            for (int i = 0; i < mFOV.length; ++i) {
               ppw.startNode("extent");
               ppw.addAttribute("axis", Integer.toString(i));
               ppw.setValue(Double.toString(mFOV[i]));
               ppw.endNode();
            }
            items++;
            ppw.endNode();
         }
         if ((base == null) || (!mDescription.equals(base.mDescription))) {
            ppw.startNode("description");
            ppw.setValue(mDescription);
            ppw.endNode();
            items++;
         }
         if ((base == null) || (!mSignal.equals(base.mSignal))) {
            ppw.startNode("signal");
            ppw.setValue(mSignal);
            ppw.endNode();
            items++;
         }
         return items > 0 ? sw.toString() : "";
      }
   }

   static class DataHeader {
      protected final DataType mType;
      protected final String mName;

      protected DataHeader(DataType type, String name) {
         mName = name;
         mType = type;
      }

   }

   static class EDSHeader extends DataHeader {
      final double BeamEnergy;
      final double ProbeCurrent;
      final double WorkingDistance;
      final double LiveTime;
      final Composition StandardComposition;
      final String Description;
      final Date Timestamp;
      final StageCoordinate StagePos;
      final double[] mYScale;

      private EDSHeader(SpectrumProperties props, String name) {
         super(DataType.ElectronProbeEDS, name);
         BeamEnergy = props.getNumericWithDefault(SpectrumProperties.BeamEnergy, Double.NaN);
         ProbeCurrent = SpectrumUtils.getAverageFaradayCurrent(props, Double.NaN);
         WorkingDistance = SpectrumUtils.getSamplePosition(props)[2];
         LiveTime = props.getNumericWithDefault(SpectrumProperties.LiveTime, Double.NaN);
         StandardComposition = props.getCompositionWithDefault(SpectrumProperties.StandardComposition, null);
         Description = props.getTextWithDefault(SpectrumProperties.SpectrumDisplayName, "");
         Timestamp = props.getTimestampWithDefault(SpectrumProperties.AcquisitionTime, null);
         StagePos = (StageCoordinate) props.getObjectWithDefault(SpectrumProperties.StagePosition, null);
         mYScale = new double[props.isDefined(SpectrumProperties.EnergyQuadratic) ? 3 : 2];
         mYScale[0] = props.getNumericWithDefault(SpectrumProperties.EnergyOffset, 0.0);
         mYScale[1] = props.getNumericWithDefault(SpectrumProperties.EnergyScale, 1.0);
         if (mYScale.length > 2)
            mYScale[2] = props.getNumericWithDefault(SpectrumProperties.EnergyQuadratic, 0.0);
      }

      protected String toXML(EDSHeader base) {
         // Create the position
         final StringWriter sw = new StringWriter();
         final PrettyPrintWriter ppw = new PrettyPrintWriter(sw);
         int items = 0;
         ppw.startNode("data");
         ppw.addAttribute("type", DataType.ElectronProbeEDS.mAbbrev);
         if (mName != null)
            ppw.addAttribute("name", mName);
         if ((base == null) || (this.BeamEnergy != base.BeamEnergy)) {
            ppw.startNode("beam_energy");
            ppw.addAttribute("unit", "keV");
            ppw.setValue(Double.toString(BeamEnergy));
            ppw.endNode();
            items++;
         }
         if ((base == null) || (this.ProbeCurrent != base.ProbeCurrent))
            if (!Double.isNaN(ProbeCurrent)) {
               ppw.startNode("probe_current");
               ppw.addAttribute("unit", "A");
               ppw.setValue(Double.toString(1.0e-9 * ProbeCurrent));
               ppw.endNode();
               items++;
            }
         if ((base == null) || (this.LiveTime != base.LiveTime))
            if (!Double.isNaN(LiveTime)) {
               ppw.startNode("live_time");
               ppw.addAttribute("unit", "s");
               ppw.setValue(Double.toString(LiveTime));
               ppw.endNode();
               items++;
            }
         if ((base == null) || (!this.Description.equals(base.Description)))
            if (Description != null) {
               ppw.startNode("description");
               ppw.setValue(Description);
               ppw.endNode();
               items++;
            }
         if ((base == null) || (this.WorkingDistance != base.WorkingDistance))
            if (!Double.isNaN(WorkingDistance)) {
               ppw.startNode("working_distance");
               ppw.addAttribute("unit", "m");
               ppw.setValue(Double.toString(WorkingDistance));
               ppw.endNode();
               items++;
            }
         if ((base != null) && (!StagePos.equals(base.StagePos))) {
            final StageCoordinate d = base.StagePos.delta(StagePos);
            assert d.size() > 0;
            ppw.startNode("stage_position");
            for (final StageCoordinate.Axis axis : StageCoordinate.Axis.values())
               if (d.isPresent(axis)) {
                  ppw.startNode(axis.toString());
                  ppw.addAttribute("unit", axis.unit());
                  ppw.setValue(Double.toString(d.get(axis)));
                  ppw.endNode();
               }
            ppw.endNode();
            items++;
         }
         if (this.StandardComposition != null)
            if ((base == null) || (!this.StandardComposition.equals(base.StandardComposition))) {
               ppw.startNode("standard_composition");
               ppw.addAttribute("name", StandardComposition.toString());
               for (final Element elm : StandardComposition.getElementSet()) {
                  ppw.startNode("element");
                  ppw.addAttribute("z", Integer.toString(elm.getAtomicNumber()));
                  ppw.setValue(Double.toString(StandardComposition.weightFraction(elm, false)));
                  ppw.endNode();
               }
               ppw.endNode();
               items++;
            }
         if (this.Timestamp != null) {
            if ((base == null) || (!Timestamp.equals(base.Timestamp))) {
               final Calendar c = Calendar.getInstance();
               c.setTime(Timestamp);
               ppw.startNode("acquisition_time");
               ppw.startNode("date");
               ppw.setValue(formatDate(c));
               ppw.endNode();
               ppw.startNode("time");
               ppw.setValue(formatTime(c));
               ppw.endNode();
               ppw.endNode();
            }
            items++;
         }
         if ((base == null) || (!Arrays.equals(this.mYScale, base.mYScale))) {
            ppw.startNode("y_axis_scale");
            ppw.addAttribute("unit", "eV");
            for (int i = 0; i < mYScale.length; ++i) {
               ppw.startNode("coefficient");
               ppw.addAttribute("index", Integer.toString(i));
               ppw.setValue(Double.toString(mYScale[i]));
               ppw.endNode();
            }
            ppw.endNode();
            items++;
         }
         ppw.endNode();
         ppw.flush();
         return items > 0 ? sw.toString() : "";
      }
   }

   static private String formatInt(int i, int len) {
      final StringBuffer sb = new StringBuffer();
      final String tmp = Integer.toString(i);
      for (int cx = len - tmp.length(); cx > 0; --cx)
         sb.append("0");
      sb.append(tmp);
      return sb.toString();
   }

   static private String formatDate(Calendar c) {
      return formatInt(c.get(Calendar.YEAR), 4) + "-" + formatInt(c.get(Calendar.MONTH) + 1, 2) + "-" + formatInt(c.get(Calendar.DAY_OF_MONTH), 2);
   }

   static private String formatTime(Calendar c) {
      final String pt1 = formatInt(c.get(Calendar.HOUR_OF_DAY), 2) + "-" + formatInt(c.get(Calendar.MINUTE), 2) + "-"
            + formatInt(c.get(Calendar.SECOND), 2);
      final int zo = c.get(Calendar.ZONE_OFFSET) / 60000;
      final String pt2 = (zo < 0 ? "-" : "+") + Integer.toString(Math.abs(zo / 60)) + ":" + formatInt(zo % 60, 2);
      return pt1 + pt2;
   }

   public WriteHyperspectral(File f) {

   }

   public WriteHyperspectral(OutputStream out) {

   }

   public void add(int[] idx, ISpectrumData spec) {

   }

   public void add(int[] idx, Image img) {

   }

   public void addDimension(String name, Unit unit, double offset, double ext, int len) {
      mDimensions.add(new Dimension(name, unit, offset, ext, len));
   }

   public void addDimension(String name, Unit unit, double ext, int len) {
      mDimensions.add(new Dimension(name, unit, Double.NaN, ext, len));
   }

   public void addDimension(String name, int len) {
      mDimensions.add(new Dimension(name, Unit.None, Double.NaN, Double.NaN, len));
   }

   protected String toXML() {
      final StringWriter sw = new StringWriter();
      final PrettyPrintWriter ppw = new PrettyPrintWriter(sw);
      if (mDimensions.size() > 0) {
         ppw.startNode("dimensions");
         for (final Dimension dim : mDimensions)
            dim.write(ppw);
         ppw.endNode();
         ppw.flush();
      }
      return sw.toString();
   }

   private void writeHeader(File dir, String xml) throws IOException {
      if (xml.length() > 0) {
         dir.mkdirs();
         final File outFile = new File(dir, "header.xml");
         try (final OutputStream os = new FileOutputStream(outFile)) {
            try (final Writer fw = new OutputStreamWriter(os, mCharSet)) {
               fw.append("<?xml version=\"1.0\" encoding=\"" + mCharSet.displayName() + "\" ?>\n");
               fw.append(xml);
            }
         }
      }
   }

   private void writeEDS(File dir, String name, ISpectrumData spec, boolean xy) throws IOException {
      dir.mkdirs();
      final StringBuffer sb = new StringBuffer((xy ? 2 : 1) * 10 * spec.getChannelCount());
      for (int i = 0; i < spec.getChannelCount(); ++i) {
         if (xy) {
            sb.append(Double.toString(SpectrumUtils.minEnergyForChannel(spec, i)));
            sb.append(",");
         }
         final double counts = spec.getCounts(i);
         if (Math.round(counts) == counts)
            sb.append(Long.toString(Math.round(counts)));
         else
            sb.append(Double.toString(counts));
         sb.append("\n");
      }
      final File outFile = new File(dir, name + ".dat");
      final OutputStream os = new FileOutputStream(outFile);
      final Writer fw = new OutputStreamWriter(os, "UTF-8");
      fw.append(sb.toString());
      fw.flush();
      fw.close();
      os.close();
   }

   private void writeImage(File file, ImageHeader imgHdr) throws IOException {
      file.mkdirs();
      final Image img = imgHdr.mImage;
      RenderedImage ri = null;
      if (img instanceof RenderedImage) {
         ri = (RenderedImage) img;
         ImageIO.write(ri, "tif", new File(file, "image.tif"));
      }
   }

   public static void write(File file, RippleSpectrum ripple, double[] fov) throws IOException {
      file.mkdirs();
      final int rows = ripple.getRows(), cols = ripple.getColumns();
      final WriteHyperspectral whs = new WriteHyperspectral(file);
      whs.addDimension("y", Unit.Meter, fov[1], rows);
      whs.addDimension("x", Unit.Meter, fov[0], cols);
      final String name = "eds0";
      final EDSHeader base = new EDSHeader(ripple.getProperties(), name);
      final String root = whs.toXML();
      whs.writeHeader(file, (root.length() > 0 ? root + "\n" : "") + base.toXML(null));
      for (int y = 0; y < rows; ++y) {
         final File yDir = new File(file, whs.mDimensions.get(0).mName + "[" + Integer.toString(y) + "]");
         ripple.setPosition(y, 0);
         final EDSHeader yHdr = new EDSHeader(ripple.getProperties(), name);
         whs.writeHeader(yDir, yHdr.toXML(base));
         for (int x = 0; x < cols; ++x) {
            final File xDir = new File(yDir, whs.mDimensions.get(1).mName + "[" + Integer.toString(x) + "]");
            ripple.setPosition(y, x);
            final EDSHeader xHdr = new EDSHeader(ripple.getProperties(), name);
            whs.writeHeader(xDir, xHdr.toXML(yHdr));
            whs.writeEDS(xDir, name, ripple, false);
         }
      }
   }

   public static void write(File file, ISpectrumData spec) throws IOException {
      file.mkdirs();
      final WriteHyperspectral whs = new WriteHyperspectral(file);
      final String name = "eds0";
      final SpectrumProperties props = spec.getProperties();
      final EDSHeader base = new EDSHeader(props, name);
      ImageHeader img = null;
      if (props.isDefined(SpectrumProperties.MicroImage)) {
         final Image micro = (Image) props.getObjectWithDefault(SpectrumProperties.MicroImage, null);
         if (micro instanceof ScaledImage)
            img = new ImageHeader((ScaledImage) micro, "Microimage", "", "?");
      }
      whs.writeHeader(file, whs.toXML() + "\n" + base.toXML(null));
      whs.writeEDS(file, name, spec, false);
      if (img != null)
         whs.writeImage(file, img);
   }

   /**
    * Writes a collection of spectra as a single dimensional hyperspectral data
    * set. This format might be appropriate for a standard library or similar.
    * 
    * @param file
    *           A file to which to write
    * @param dim
    *           The name of the dimension (arbitrary)
    * @param specs
    *           The spectra
    * @throws IOException
    */
   public static void write(File file, String dim, Collection<ISpectrumData> specs) throws IOException {
      file.mkdirs();
      final WriteHyperspectral whs = new WriteHyperspectral(file);
      whs.addDimension(dim, Unit.None, 1.0, specs.size());
      final String name = "eds0";
      SpectrumProperties baseProps = null;
      for (final ISpectrumData spec : specs)
         baseProps = SpectrumProperties.merge(baseProps, spec.getProperties());
      final EDSHeader base = new EDSHeader(baseProps, null);
      final String root = whs.toXML();
      whs.writeHeader(file, (root.length() > 0 ? root + "\n" : "") + base.toXML(null));
      int y = 0;
      for (final ISpectrumData spec : specs) {
         final File yDir = new File(file, whs.mDimensions.get(0).mName + "[" + Integer.toString(y) + "]");
         final EDSHeader yHdr = new EDSHeader(spec.getProperties(), name);
         whs.writeHeader(yDir, yHdr.toXML(base));
         final SpectrumProperties props = spec.getProperties();
         ImageHeader img = null;
         if (props.isDefined(SpectrumProperties.MicroImage)) {
            final Image micro = (Image) props.getObjectWithDefault(SpectrumProperties.MicroImage, null);
            if (micro instanceof ScaledImage)
               img = new ImageHeader((ScaledImage) micro, "Microimage", "", "?");
         }
         whs.writeEDS(new File(yDir, name), name, spec, false);
         if (img != null)
            whs.writeImage(file, img);
         ++y;
      }
   }
}
