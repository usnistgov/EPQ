package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate.Axis;
import gov.nist.microanalysis.EPQTools.TIFFImageFileDir.Field;
import gov.nist.microanalysis.Utility.HalfUpFormat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Write a spectrum or an image as an ASPEX-style TIFF file.
 * 
 * @author nicholas
 */
public class WriteSpectrumAsTIFF {

   static void addItem(StringBuffer sb, String name, String value) {
      if(value != null) {
         if(sb.length() > 0)
            sb.append("\n");
         sb.append(name);
         sb.append("=");
         sb.append(value);
      }
   }

   static void addItem(StringBuffer sb, String name, double value, String fmt) {
      if(!Double.isNaN(value)) {
         final NumberFormat df = new HalfUpFormat(fmt);
         addItem(sb, name, df.format(value));
      }
   }

   public static void write(ISpectrumData spec, String filepath)
         throws IOException {
      write(spec, new FileOutputStream(filepath));
      // Closed in write(...)
   }

   public static void write(ISpectrumData spec, FileOutputStream os)
         throws IOException {
      try {
         final ArrayList<TIFFImageFileDir> ifds = new ArrayList<TIFFImageFileDir>();
         final SpectrumProperties sp = spec.getProperties();
         {
            final TIFFImageFileDir ifd = new TIFFImageFileDir();
            final DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(Locale.US);
            final NumberFormat df = new HalfUpFormat("0.000", dfs);
            ifd.addField(new Field(ASPEXSpectrum.SPECTRAL_XRES, df.format(spec.getChannelWidth())));
            ifd.addField(new Field(ASPEXSpectrum.SPECTRAL_XOFF, df.format(spec.getZeroOffset())));
            ifd.addField(new Field(ASPEXSpectrum.SPECTRAL_YRES, df.format(1.0)));
            ifd.addField(new Field(ASPEXSpectrum.SPECTRAL_YOFF, df.format(0.0)));
            ifd.addField(new Field(ASPEXSpectrum.SPECTRAL_DATA, TIFFImageFileDir.ttSLong, SpectrumUtils.toIntArray(spec)));
            ifd.addField(new Field(ASPEXSpectrum.IMAGE_DESCRIPTION, buildImageDescription(sp)));
            ifd.addField(new Field(ASPEXSpectrum.SOFTWARE, "NIST EPQ Library"));
            ifds.add(ifd);
            {
               final Object obj = sp.getObjectWithDefault(SpectrumProperties.MicroImage, null);
               if((obj != null) && (obj instanceof BufferedImage))
                  ifd.addBWImage((BufferedImage) obj);
               else {
                  // Give spectra a thumbnail image
                  final int max = SpectrumUtils.channelForEnergy(spec, 10.0e3);
                  ifd.addBWImage(SpectrumUtils.toThumbnail(spec, max, 256, 128));
               }
            }
         }
         {
            Object obj = sp.getObjectWithDefault(SpectrumProperties.MicroImage2, null);
            if((obj != null) && (obj instanceof BufferedImage)) {
               final TIFFImageFileDir ifd2 = new TIFFImageFileDir();
               ifd2.addBWImage((BufferedImage) obj);
               ifds.add(ifd2);
            } else {
               obj = sp.getObjectWithDefault(SpectrumProperties.MacroImage, null);
               if((obj != null) && (obj instanceof ScaledImage)) {
                  final TIFFImageFileDir ifd2 = new TIFFImageFileDir();
                  ifd2.addBWImage((ScaledImage) obj);
                  ifds.add(ifd2);
               }
            }
         }
         int size = 4096;
         for(final TIFFImageFileDir ifd : ifds)
            size += ifd.estimateSize();
         final ByteBuffer bb = ByteBuffer.allocate(size);
         bb.order(ByteOrder.LITTLE_ENDIAN);
         bb.put(TIFFImageFileDir.LITTLE_MAGIC);
         int nextIfd = bb.position();
         bb.putInt(TIFFImageFileDir.PLACEHOLDER);
         for(final TIFFImageFileDir ifd : ifds)
            nextIfd = ifd.write(bb, nextIfd);
         os.write(bb.array(), 0, bb.position());
         os.flush();
      }
      finally {
         os.close();
      }
   }
   
   
   public static void writeMicroImages(File file, ISpectrumData spec) throws IOException {
      FileOutputStream os = new FileOutputStream(file);
      try {
         final ArrayList<TIFFImageFileDir> ifds = new ArrayList<TIFFImageFileDir>();
         final SpectrumProperties sp = spec.getProperties();
         {
            final TIFFImageFileDir ifd = new TIFFImageFileDir();
            ifd.addField(new Field(ASPEXSpectrum.IMAGE_DESCRIPTION, buildImageDescription(sp)));
            ifd.addField(new Field(ASPEXSpectrum.SOFTWARE, "NIST EPQ Library"));
            ifds.add(ifd);
            {
               final Object obj = sp.getObjectWithDefault(SpectrumProperties.MicroImage, null);
               if((obj != null) && (obj instanceof BufferedImage))
                  ifd.addBWImage((BufferedImage) obj);
            }
         }
         {
            Object obj = sp.getObjectWithDefault(SpectrumProperties.MicroImage2, null);
            if((obj != null) && (obj instanceof BufferedImage)) {
               final TIFFImageFileDir ifd2 = new TIFFImageFileDir();
               ifd2.addBWImage((BufferedImage) obj);
               ifds.add(ifd2);
            }
         }
         int size = 4096;
         for(final TIFFImageFileDir ifd : ifds)
            size += ifd.estimateSize();
         final ByteBuffer bb = ByteBuffer.allocate(size);
         bb.order(ByteOrder.LITTLE_ENDIAN);
         bb.put(TIFFImageFileDir.LITTLE_MAGIC);
         int nextIfd = bb.position();
         bb.putInt(TIFFImageFileDir.PLACEHOLDER);
         for(final TIFFImageFileDir ifd : ifds)
            nextIfd = ifd.write(bb, nextIfd);
         os.write(bb.array(), 0, bb.position());
         os.flush();
      }
      finally {
         os.close();
      }
   }


   private static String buildImageDescription(SpectrumProperties sp) {
      final StringBuffer sb = new StringBuffer(2048);
      addItem(sb, "live_time", sp.getNumericWithDefault(SpectrumProperties.LiveTime, Double.NaN), "0.00");
      addItem(sb, "acquisition_time", sp.getNumericWithDefault(SpectrumProperties.RealTime, Double.NaN), "0.00");
      addItem(sb, "probe_current", SpectrumUtils.getAverageFaradayCurrent(sp, Double.NaN), "0.00000");
      {
         final Composition comp = sp.getCompositionWithDefault(SpectrumProperties.StandardComposition, null);
         if(comp != null) {
            final NumberFormat df = new HalfUpFormat("0.000");
            for(final Element elm : comp.getElementSet())
               addItem(sb, "element_percent", elm.toAbbrev() + "," + df.format(100.0 * comp.atomicPercent(elm)));
         }
      }
      final double k = 3.5 * 25.4e-3;
      Object obj = sp.getObjectWithDefault(SpectrumProperties.MicroImage, null);
      if((obj != null) && (obj instanceof ScaledImage)) {
         final ScaledImage img = (ScaledImage) obj;
         addItem(sb, "mag", k / img.getHorizontalFieldOfView(), "0.0");
         final Object obj2 = sp.getObjectWithDefault(SpectrumProperties.MicroImage, null);
         if((obj2 != null) && (obj2 instanceof ScaledImage)) {
            final ScaledImage img2 = (ScaledImage) obj2;
            final double zoom = img.getHorizontalFieldOfView() / img2.getHorizontalFieldOfView();
            addItem(sb, "zoom", zoom, "0.0000");
         }
      }

      final StageCoordinate pos = (StageCoordinate) sp.getObjectWithDefault(SpectrumProperties.StagePosition, null);
      if(pos != null) {
         addItem(sb, "stage_x", pos.get(Axis.X), "0.0000");
         addItem(sb, "stage_y", pos.get(Axis.Y), "0.0000");
         addItem(sb, "stage_z", pos.get(Axis.Z), "0.0000");
         addItem(sb, "stage_r", pos.get(Axis.R), "0.0000");
         addItem(sb, "stage_t", pos.get(Axis.T), "0.0000");
         addItem(sb, "stage_b", pos.get(Axis.B), "0.0000");
      }
      addItem(sb, "spot_size", sp.getNumericWithDefault(SpectrumProperties.SpotSize, Double.NaN), "0.0");
      addItem(sb, "accelerating_voltage", sp.getNumericWithDefault(SpectrumProperties.BeamEnergy, Double.NaN), "0.00");
      addItem(sb, "working_distance", sp.getNumericWithDefault(SpectrumProperties.WorkingDistance, Double.NaN), "0.00");
      final Date ts = sp.getTimestampWithDefault(SpectrumProperties.AcquisitionTime, null);
      if(ts != null) {
         final DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
         addItem(sb, "analysis_date", df.format(ts));
         final DateFormat tf = new SimpleDateFormat("HH:mm:ss");
         addItem(sb, "analysis_time", tf.format(ts));
      }
      addItem(sb, "sample_number", sp.getTextWithDefault(SpectrumProperties.SampleId, null));
      addItem(sb, "client_number", sp.getTextWithDefault(SpectrumProperties.ClientsSampleID, null));
      addItem(sb, "client_name", sp.getTextWithDefault(SpectrumProperties.ClientName, null));
      addItem(sb, "project_number", sp.getTextWithDefault(SpectrumProperties.ProjectName, null));
      addItem(sb, "comment", sp.getTextWithDefault(SpectrumProperties.SpectrumComment, null));
      addItem(sb, "caption", sp.getTextWithDefault(SpectrumProperties.SpecimenDesc, null));
      addItem(sb, "beam_x", sp.getNumericWithDefault(SpectrumProperties.BeamSpotX, Double.NaN), "0");
      addItem(sb, "beam_y", sp.getNumericWithDefault(SpectrumProperties.BeamSpotY, Double.NaN), "0");
      addItem(sb, "operator", sp.getTextWithDefault(SpectrumProperties.InstrumentOperator, "unknown"));
      addItem(sb, "dave", sp.getNumericWithDefault(SpectrumProperties.AFA_DAvg, Double.NaN), "0.00");
      addItem(sb, "take_off_angle", sp.getNumericWithDefault(SpectrumProperties.Elevation, Double.NaN), "0.0");
      return sb.toString();
   }
   

   public static void write(ScaledImage img, FileOutputStream os, SpectrumProperties sp)
         throws IOException {
      try {
         final ArrayList<TIFFImageFileDir> ifds = new ArrayList<TIFFImageFileDir>();
         {
            final TIFFImageFileDir ifd = new TIFFImageFileDir();
            ifd.addBWImage(img);
            final StringBuffer imgDesc = new StringBuffer(buildImageDescription(sp));
            final double k = 3.5 * 25.4e-3;
            addItem(imgDesc, "mag", k / img.getHorizontalFieldOfView(), "0.0");
            addItem(imgDesc, "zoom", "1.0");
            ifd.addField(new Field(ASPEXSpectrum.IMAGE_DESCRIPTION, imgDesc.toString()));
            ifds.add(ifd);
         }
         int size = 4096;
         for(final TIFFImageFileDir ifd : ifds)
            size += ifd.estimateSize();
         final ByteBuffer bb = ByteBuffer.allocate(size);
         bb.put(TIFFImageFileDir.LITTLE_MAGIC);
         int nextIfd = bb.position();
         bb.putInt(TIFFImageFileDir.PLACEHOLDER);
         for(final TIFFImageFileDir ifd : ifds)
            nextIfd = ifd.write(bb, nextIfd);
         os.write(bb.array(), 0, bb.position());
      }
      finally {
         os.close();
      }
   }
}
