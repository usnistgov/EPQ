package gov.nist.microanalysis.EPQTools;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate.Axis;

/**
 * <p>
 * A class for reading EDAX SPC files from disk.
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
public class EdaxSPCSpectrum
   extends BaseSpectrum {

   // I really don't know what version numbers work so I'm being
   // generous in my limits until I discover which ones don't work.
   static private final double MIN_VERSION = 0.50F;
   static private final double MAX_VERSION = 1.00F;

   static private final String BAD_FILE_FORMAT = "This file does not appear to be an EDAX SPC spectrum file.";
   private final SpectrumProperties mProperties;
   private final int[] mData;

   public static boolean isInstanceOf(InputStream is) {
      try {
         try (final LEDataInputStream dis = new LEDataInputStream(is)) {
            final float version = dis.readFloat();
            return (version >= MIN_VERSION) && (version < MAX_VERSION);
         }
      }
      catch(final IOException e) {
         return false;
      }
   }

   /**
    * Extracts the filename without extension from spc files. Since the files
    * are always created on Windows systems but could be read on Linux or Mac
    * systems there could be a separator mismatch leading to erroneous File
    * object parsing.
    * 
    * @param path
    * @return String
    */
   private static String extractName(String path) {
      int end = path.length();
      if(path.endsWith(".spc"))
         end -= 4;
      final int st = path.lastIndexOf("\\") + 1;
      return path.substring(st, end);
   }

   public EdaxSPCSpectrum(FileInputStream is)
         throws EPQException, IOException {
      try (final LEDataInputStream dis = new LEDataInputStream(is)) {
         final float version = dis.readFloat();
         float f;
         if((version < MIN_VERSION) || (version > MAX_VERSION))
            throw new EPQException(BAD_FILE_FORMAT);
         mProperties = new SpectrumProperties();
         mProperties.setTextProperty(SpectrumProperties.Software, "EDAX v. " + Float.toString(dis.readFloat()));
         mProperties.setTextProperty(SpectrumProperties.SpecimenName, readString(dis, 8));
         {
            final int yr = dis.readShort();
            final int day = dis.readByte();
            final int mon = dis.readByte();
            final int min = dis.readByte();
            final int hour = dis.readByte();
            dis.readByte(); // hund
            final int sec = dis.readByte();
            final Calendar c = Calendar.getInstance();
            c.set(yr, mon - 1, day, hour, min, sec);
            mProperties.setTimestampProperty(SpectrumProperties.AcquisitionTime, c.getTime());
         }
         dis.readInt(); // Length of file in bytes
         dis.readInt(); // data start
         mData = new int[dis.readShort()];
         dis.skipBytes(64 - (32 + 2));
         assert dis.getChannel().position() == 64;
         {
            final String desc = readString(dis, 40);
            if(desc.length() > 0)
               mProperties.setTextProperty(SpectrumProperties.SpecimenDesc, desc);
         }
         {
            final String comm = readString(dis, 256 - 40);
            if(comm.length() > 0)
               mProperties.setTextProperty(SpectrumProperties.SpectrumComment, comm);
         }
         dis.skipBytes(8);
         mProperties.setNumericProperty(SpectrumProperties.BeamSpotX, dis.readShort());
         mProperties.setNumericProperty(SpectrumProperties.BeamSpotY, dis.readShort());
         dis.skipBytes(442 - (int) dis.getChannel().position());
         assert dis.getChannel().position() == 442;
         // mProperties.setTextProperty(SpectrumProperties.SpectrumClass, val)
         dis.readShort(); // escape peaks removed ->1 else 0
         dis.readInt(); // analyzer type
         final double offset = dis.readFloat();
         final double scale = (1000.0 * (dis.readFloat() - offset)) / mData.length;
         setEnergyScale(offset, scale);
         assert dis.getChannel().position() == 456;
         mProperties.setNumericProperty(SpectrumProperties.LiveTime, dis.readFloat());
         final StageCoordinate sc = new StageCoordinate();
         sc.set(Axis.T, dis.readFloat());
         mProperties.setObjectProperty(SpectrumProperties.StagePosition, sc);
         mProperties.setNumericProperty(SpectrumProperties.TakeOffAngle, dis.readFloat());
         f = dis.readFloat();
         if(f > 0.0)
            mProperties.setNumericProperty(SpectrumProperties.FaradayBegin, f);
         mProperties.setNumericProperty(SpectrumProperties.Resolution, dis.readFloat());
         final String[] types = {
            "std",
            "UTW",
            "Super UTW",
            "ECON 3/4 Open",
            "ECON 3/4 Closed",
            "Econ 5/6 Open",
            "Econ 5/6 Closed",
            "TEMECON"
         };
         assert dis.getChannel().position() == 476;
         final int tmp = dis.readInt() - 1;
         mProperties.setTextProperty(SpectrumProperties.DetectorDescription, (tmp >= 0) && (tmp < types.length) ? types[tmp]
               : "unknown");
         f = dis.readFloat();
         if(f > 0.0)
            mProperties.setNumericProperty(SpectrumProperties.HydroCarbonWindow, f);
         f = dis.readFloat();
         if(f > 0.0)
            mProperties.setNumericProperty(SpectrumProperties.AluminumWindow, 1000.0 * f);
         f = dis.readFloat();
         if(f > 0.0)
            mProperties.setNumericProperty(SpectrumProperties.BerylliumWindow, f);
         f = dis.readFloat();
         if(f > 0.0)
            mProperties.setNumericProperty(SpectrumProperties.GoldLayer, 1000.0 * f);
         f = dis.readFloat();
         if(f > 0.0)
            mProperties.setNumericProperty(SpectrumProperties.DeadLayer, f);
         f = dis.readFloat();
         if(f > 0.0)
            mProperties.setNumericProperty(SpectrumProperties.DetectorThickness, 10.0 * f);
         //
         {
            @SuppressWarnings("unused")
            final double inc = Math.toRadians(dis.readFloat());
            final double az = Math.toRadians(dis.readFloat());
            final double elev = Math.toRadians(dis.readFloat());
            final double NOM_DISTANCE = 0.04;
            final double NOM_WD = 0.04;
            mProperties.setDetectorPosition(elev, az, NOM_DISTANCE, NOM_WD);
         }
         // assert dis.getChannel().position() == 516;
         dis.skipBytes(532 - (int) dis.getChannel().position());
         mProperties.setNumericProperty(SpectrumProperties.BeamEnergy, dis.readFloat());
         dis.skipBytes(576 - (int) dis.getChannel().position());
         mProperties.setTextProperty(SpectrumProperties.Instrument, dis.readShort() == 1 ? "TEM" : "SEM");
         dis.skipBytes(638 - (int) dis.getChannel().position());
         assert dis.getChannel().position() == 638;
         {
            final int nElm = dis.readShort();
            final TreeSet<Element> elms = new TreeSet<Element>();
            for(int i = 0; i < 48; ++i) {
               final int z = dis.readShort();
               if((i < nElm) && (Element.isValid(z)))
                  elms.add(Element.byAtomicNumber(z));
            }
            dis.skipBytes(1342 - (int) dis.getChannel().position());
            if(elms.size() > 0)
               mProperties.setTextProperty(SpectrumProperties.ElementList, elms.toString());
         }
         assert dis.getChannel().position() == 1342;
         // Skip rois, background, conc, labels, background pcts
         dis.skipBytes(3096 - (int) dis.getChannel().position());
         {
            assert dis.getChannel().position() == 3096;
            final int numConcen = dis.readShort();
            assert numConcen < 24;
            final int[] z = new int[24];
            final float[] c = new float[24];
            for(int i = 0; i < 24; ++i)
               z[i] = dis.readShort();
            for(int i = 0; i < 24; ++i)
               c[i] = dis.readFloat();
            if(numConcen > 0) {
               final Composition comp = new Composition();
               for(int i = 0; i < numConcen; ++i)
                  if(Element.isValid(z[i]) && (c[i] > 0.0))
                     comp.addElement(Element.byAtomicNumber(z[i]), c[i]);
               mProperties.setCompositionProperty(SpectrumProperties.MicroanalyticalComposition, comp);
            }
         }
         dis.skipBytes(3840 - (int) dis.getChannel().position());
         assert dis.getChannel().position() == 3840;
         // Read spectral data
         for(int i = 0; i < mData.length; ++i)
            mData[i] = dis.readInt();
         dis.skipBytes((4096 - mData.length) * 4);
         {
            final String filename = readString(dis, 256);
            if(filename.length() > 0) {
               mProperties.setTextProperty(SpectrumProperties.SourceFile, filename);
               mProperties.setTextProperty(SpectrumProperties.SpecimenName, extractName(filename));
            }
         }
         {
            final String filename = readString(dis, 256);
            if(filename.length() > 0) {
               final File file = new File(filename);
               if(file.canRead())
                  try {
                     final BufferedImage bi = ImageIO.read(file);
                     getProperties().setImageProperty(SpectrumProperties.MicroImage, bi);
                  }
                  catch(final Throwable e) {
                     // Just ignore it. We tried, we failed, it probably ain't
                     // worth the effort
                  }
            }
         }
         mProperties.setNumericProperty(SpectrumProperties.PulseProcessTime, dis.readFloat());
         // ignore the rest...
      }
   }

   private String readString(LEDataInputStream dis, int len)
         throws IOException {
      final byte[] buffer = new byte[len];
      dis.readFully(buffer);
      for(int i = 0; i < len; ++i)
         if(buffer[i] == 0) {
            len = i;
            break;
         }
      return len > 0 ? Charset.forName("US-ASCII").decode(ByteBuffer.wrap(buffer, 0, len)).toString() : "";
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getChannelCount()
    */
   @Override
   public int getChannelCount() {
      return mData.length;
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getCounts(int)
    */
   @Override
   public double getCounts(int i) {
      return mData[i];
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getProperties()
    */
   @Override
   public SpectrumProperties getProperties() {
      return mProperties;
   }
}
