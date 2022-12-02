package gov.nist.microanalysis.EPQTools;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SampleShape;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate;
import gov.nist.microanalysis.EPQLibrary.StageCoordinate.Axis;

/**
 * <p>
 * A class for reading DTSA spectrum files into objects implementing
 * ISpectrumData.
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

public class DTSAFile {

   public class DTSASpectrum extends BaseSpectrum {

      private class Element_InfoRec {
         private int mAtomic_number;
         // private float mSpare1;
         private float mWeight_Fraction;

         // private float mSpare;
         // private float mValence;

         private Element_InfoRec() {
         }

         private void read(DataInputStream dis) throws IOException {
            mAtomic_number = dis.readShort();
            dis.readFloat();
            mWeight_Fraction = dis.readFloat();
            dis.readFloat();
            dis.readFloat(); // mValence
         }
      }

      private final float[] mChannels;
      SpectrumProperties mProperties;

      public DTSASpectrum(SpectrumProperties parentProps) {
         super();
         mProperties = new SpectrumProperties(parentProps);
         mProperties.setTimestampProperty(SpectrumProperties.AcquisitionTime, new Date());
         mChannels = new float[mNumberOfChannels];
      }

      @Override
      public int getChannelCount() {
         return mNumberOfChannels;
      }

      @Override
      public double getCounts(int i) {
         return mChannels[i];
      }

      @Override
      public SpectrumProperties getProperties() {
         return mProperties;
      }

      // Only add the properties if they have been explicitly set to a non-zero
      // value...
      private void setPropertyWhenNotEqual(SpectrumProperties.PropertyId pid, double val, double notVal) {
         if (val != notVal)
            mProperties.setNumericProperty(pid, val);
      }

      void read(DataInputStream dis) throws IOException {
         // Spectrum_Structure
         mProperties.setTextProperty(SpectrumProperties.SpectrumType, readCharacters(dis, 4));
         mProperties.setTextProperty(SpectrumProperties.SpecimenDesc, readCharacters(dis, 255));
         dis.readShort(); // Spectrum_Number
         mProperties.setTextProperty(SpectrumProperties.SpectrumClass, readCharacters(dis, 25));
         mProperties.setBooleanProperty(SpectrumProperties.IsTheoreticallyGenerated, dis.readByte() != 0);
         mProperties.setBooleanProperty(SpectrumProperties.IsStandard, dis.readByte() != 0);
         mProperties.setBooleanProperty(SpectrumProperties.BackgroundCorrected, dis.readByte() != 0);
         dis.readByte();
         dis.readFloat(); // Maximum_Counts
         dis.readFloat(); // Minimum_Counts
         {
            final double xTilt = dis.readFloat();
            final double yTilt = dis.readFloat();
            // It is ambiguous whether the xTilt happens first but we will just
            // assume it.
            if ((xTilt != 0.0) || (yTilt != 0.0)) {
               final double[] normal = new double[]{Math.sin(xTilt), Math.cos(xTilt) * Math.sin(yTilt), -Math.cos(xTilt) * Math.cos(yTilt)};
               mProperties.setSampleShape(SpectrumProperties.SampleShape, new SampleShape.Bulk(normal));
            }
         }
         setPropertyWhenNotEqual(SpectrumProperties.TakeOffAngle, dis.readFloat(), 0.0);
         setPropertyWhenNotEqual(SpectrumProperties.DetectorDistance, dis.readFloat(), 0.0);
         dis.readFloat(); // mSpare
         setPropertyWhenNotEqual(SpectrumProperties.SpecimenThickness, dis.readFloat(), 0.0);
         setPropertyWhenNotEqual(SpectrumProperties.SpecimenDensity, dis.readFloat(), 0.0);
         final int number_of_Elements = dis.readShort();
         {
            final Composition comp = new Composition();
            for (int i = 0; i < 15; ++i) {
               final Element_InfoRec eir = new Element_InfoRec();
               eir.read(dis);
               if ((i < number_of_Elements) && Element.isValid(eir.mAtomic_number) && (eir.mWeight_Fraction > 0.0))
                  comp.addElement(eir.mAtomic_number, eir.mWeight_Fraction);
            }
            if (comp.getElementCount() > 0)
               mProperties.setCompositionProperty(SpectrumProperties.StandardComposition, comp);
         }
         for (int i = 0; i < 157; ++i)
            dis.readFloat();
         dis.readByte(); // WDS_in_eV
         dis.readByte(); // Bool2
         dis.readFloat(); // Average_Z
         dis.readFloat(); // Spare1
         dis.readShort(); // Spare2
         dis.readShort(); // Spare3
         // Spectrum_Structure/Acq_Info
         setPropertyWhenNotEqual(SpectrumProperties.ProbeArea, dis.readFloat(), 0.0);
         {
            final float x = dis.readFloat();
            final float y = dis.readFloat();
            if ((x != 0.0) || (y != 0.0)) {
               final StageCoordinate sp = new StageCoordinate();
               sp.set(Axis.X, x);
               sp.set(Axis.Y, y);
               mProperties.setObjectProperty(SpectrumProperties.StagePosition, sp);
            }
         }
         dis.readFloat(); // Spare1a
         // Don't know how to use FirstChannel and LastChannel
         int firstChannel = dis.readShort() - 1;
         int lastChannel = dis.readShort();
         if ((firstChannel == -1) && (lastChannel == 0)) {
            firstChannel = 0;
            lastChannel = mNumberOfChannels;
         }

         if (lastChannel > mNumberOfChannels)
            lastChannel = mNumberOfChannels;
         if (firstChannel >= lastChannel)
            firstChannel = 0;
         setPropertyWhenNotEqual(SpectrumProperties.ProbeCurrent, dis.readFloat(), 0.0);
         dis.readInt(); // mBegin_Time
         dis.readFloat(); // mFirstValue
         dis.readFloat(); // mEndValue
         dis.readShort(); // mSpare2a
         mProperties.setNumericProperty(SpectrumProperties.RealTime, dis.readFloat());
         mProperties.setNumericProperty(SpectrumProperties.LiveTime, dis.readFloat());
         setPropertyWhenNotEqual(SpectrumProperties.SlowChannelCounts, dis.readInt(), 0.0);
         setPropertyWhenNotEqual(SpectrumProperties.MediumChannelCounts, dis.readInt(), 0.0);
         setPropertyWhenNotEqual(SpectrumProperties.FastChannelCounts, dis.readInt(), 0.0);
         dis.readInt(); // RequestedLiveTime
         dis.readInt(); // Actual live time
         // mProperties.setNumericProperty(SpectrumProperties.LiveTime,
         // dis.readInt()); // ActualLiveTime
         dis.readByte(); // Acquiring
         dis.readByte(); // ??
         mProperties.setNumericProperty(SpectrumProperties.LLD, dis.readShort());
         dis.readShort(); // Offset
         dis.readShort(); // PulseProcessorType
         dis.readShort(); // PulseProcessorSetting
         for (int i = 0; i < mNumberOfChannels; ++i)
            if ((i >= firstChannel) && (i < lastChannel))
               mChannels[i] = dis.readFloat();
      }
   }

   private void skip(DataInputStream dis) throws IOException {
      dis.skip(4 + 255 + 2 + 25 + (4 * 1) + (9 * 4) + 2 + (15 * (2 + (4 * 4))) + (157 * 4) + (2 * 1) + (2 * 4) + (2 * 2) + (4 * 4));
      final int firstChannel = dis.readShort();
      final int lastChannel = dis.readShort();
      dis.skip((2 * 4) + 4 + (2 * 4) + 2 + (2 * 4) + (5 * 4) + (2 * 1) + (4 * 2));
      dis.skip(4 * (lastChannel - firstChannel));
   }

   // Header information
   private int mLastSpec = 0;
   private int mFirstSpec = 0;
   private int mNumberOfChannels;
   // Spectrum list
   DTSASpectrum[] mSpectra;
   SpectrumProperties mGlobal = new SpectrumProperties();

   public DTSAFile(File file) throws FileNotFoundException, IOException {
      super();
      try (final InputStream fis = new BufferedInputStream(new FileInputStream(file))) {
         read(fis);
      }
   }

   public DTSAFile(InputStream is) {
      super();
      read(is);
   }

   private DTSAFile() {
      super();
   }

   /**
    * Reads a single DTSASpectrum from the specified input stream.
    * 
    * @param is
    * @param specIdx
    * @return DTSASpectrum
    */
   public static DTSASpectrum readOne(InputStream is, int specIdx) {
      final DTSAFile df = new DTSAFile();
      return df.read(is, specIdx);
   }

   private static String readCharacters(DataInputStream ois, int nMax) throws IOException {
      final byte[] b = new byte[nMax];
      int n = ois.readByte(); // The number of good characters
      if (n > nMax)
         n = nMax;
      if (ois.read(b) != nMax)
         throw new IOException("Unable to read a full character buffer");
      if ((nMax % 2) == 0)
         ois.readByte();
      return (Charset.forName("ISO-8859-1").decode(ByteBuffer.wrap(b, 0, n))).toString();
   }

   public static boolean isInstanceOf(InputStream is) {
      boolean res = true;
      try {
         try {
            try (final DataInputStream ois = new DataInputStream(is)) {
               final int last = ois.readShort();
               final int first = ois.readShort();
               res = ((first == 0) || (first == 1)) && ((last >= first) && (last <= 100));
               if (res) {
                  readCharacters(ois, 50); // SpecimenId
                  readCharacters(ois, 25); // SourceFile
                  readCharacters(ois, 255); // SpecimenDesc
                  readCharacters(ois, 25); // password
                  ois.readShort(); // mRefFile
                  readCharacters(ois, 50); // analyst
                  ois.readShort(); // mDetector_Spec
                  ois.readShort(); // mDetector_ID
                  final float az = ois.readFloat(); // azimuth
                  if ((az <= -360.0) || (az > 360.0))
                     res = false;
                  if (res) {
                     final float el = ois.readFloat();
                     res = (el >= -90.0) && (el <= 90.0);
                  }
                  if (res) {
                     final float detArea = ois.readFloat();
                     res = (detArea == 0.0) || ((detArea > 0.01) && (detArea < 2000.0));
                  }
                  if (res) {
                     final float detThick = ois.readFloat();
                     res = (detThick == 0.0) || ((detThick > 0.01) && (detThick <= 100.0));
                  }
               }
            }
         } finally {
            is.close(); // force closure to ensure it is not reused...
         }
      } catch (final Exception ex) {
         res = false;
      }
      return res;
   }

   // Only add the properties if they have been explicitly set to a non-zero
   // value...
   private void setPropertyWhenNotEqualG(SpectrumProperties.PropertyId pid, double val, double notVal) {
      if (val != notVal)
         mGlobal.setNumericProperty(pid, val);
   }

   public void read(InputStream is) {
      try {
         mGlobal.clear();
         try (final DataInputStream ois = new DataInputStream(is)) {
            readHeader(ois);
            mSpectra = new DTSASpectrum[(mLastSpec - mFirstSpec) + 1];
            for (int i = mFirstSpec; i <= mLastSpec; ++i) {
               mSpectra[i - mFirstSpec] = new DTSAFile.DTSASpectrum(mGlobal);
               mSpectra[i - mFirstSpec].read(ois);
            }
         }
      } catch (final IOException iox) {

      }
   }

   private DTSASpectrum read(InputStream is, int specIdx) {
      DTSASpectrum res = null;
      try {
         mGlobal.clear();
         try (final DataInputStream dis = new DataInputStream(is)) {
            readHeader(dis);
            for (int i = mFirstSpec; i <= mLastSpec; ++i)
               if ((i - mFirstSpec) == specIdx) {
                  res = new DTSASpectrum(mGlobal);
                  res.read(dis);
               } else
                  this.skip(dis);
         }
      } catch (final IOException iox) {

      }
      return res;
   }

   /**
    * readHeader
    * 
    * @param ois
    * @throws IOException
    */
   private void readHeader(DataInputStream ois) throws IOException {
      mLastSpec = ois.readShort();
      mFirstSpec = ois.readShort();
      mGlobal.setTextProperty(SpectrumProperties.SpecimenName, readCharacters(ois, 50));
      mGlobal.setTextProperty(SpectrumProperties.SourceFile, readCharacters(ois, 25));
      mGlobal.setTextProperty(SpectrumProperties.SpecimenDesc, readCharacters(ois, 255));
      readCharacters(ois, 25); // mWas_PassWord
      ois.readShort(); // mRefFile
      mGlobal.setTextProperty(SpectrumProperties.InstrumentOperator, readCharacters(ois, 50));
      ois.readShort(); // mDetector_Spec
      ois.readShort(); // mDetector_ID
      {
         final double azimuth = ois.readFloat();
         final double elevation = ois.readFloat();
         // Assume a nominal WD of 20 mm and a nominal detector distance of 5
         // cm
         final double dist = 50.0; // mm
         final double[] pos = {dist * Math.cos(azimuth) * Math.cos(elevation), dist * Math.sin(azimuth) * Math.cos(elevation),
               dist * Math.sin(elevation)};
         mGlobal.setArrayProperty(SpectrumProperties.DetectorPosition, pos);
      }
      setPropertyWhenNotEqualG(SpectrumProperties.DetectorArea, ois.readFloat(), 0.0);
      setPropertyWhenNotEqualG(SpectrumProperties.DetectorThickness, ois.readFloat(), 0.0);
      setPropertyWhenNotEqualG(SpectrumProperties.CarbonCoating, ois.readFloat(), 0.0);
      setPropertyWhenNotEqualG(SpectrumProperties.DiamondWindow, ois.readFloat(), 0.0);
      setPropertyWhenNotEqualG(SpectrumProperties.MylarWindow, ois.readFloat(), 0.0);
      setPropertyWhenNotEqualG(SpectrumProperties.BoronNitrideWindow, ois.readFloat(), 0.0);
      setPropertyWhenNotEqualG(SpectrumProperties.SiliconNitrideWindow, ois.readFloat(), 0.0);
      setPropertyWhenNotEqualG(SpectrumProperties.IceThickness, ois.readFloat(), 0.0);
      setPropertyWhenNotEqualG(SpectrumProperties.GoldLayer, 1000.0 * ois.readFloat(), 0.0);
      setPropertyWhenNotEqualG(SpectrumProperties.AluminumLayer, ois.readFloat(), 0.0);
      setPropertyWhenNotEqualG(SpectrumProperties.BerylliumWindow, ois.readFloat(), 0.0);
      ois.readFloat(); // mSi_Thickness
      setPropertyWhenNotEqualG(SpectrumProperties.MoxtekWindow, ois.readFloat(), 0.0);
      setPropertyWhenNotEqualG(SpectrumProperties.ParaleneWindow, ois.readFloat(), 0.0);
      ois.readFloat(); // mWDS_Resolution
      mGlobal.setNumericProperty(SpectrumProperties.EnergyScale, ois.readFloat());
      {
         final double res = ois.readFloat();
         if (res != 0.0) {
            mGlobal.setNumericProperty(SpectrumProperties.Resolution, res);
            mGlobal.setNumericProperty(SpectrumProperties.ResolutionLine, SpectrumUtils.E_MnKa);
         }
      }
      mGlobal.setNumericProperty(SpectrumProperties.EnergyOffset, ois.readFloat());
      ois.readFloat();
      // mGlobal.setNumericProperty(SpectrumProperties.EnergyScale, mdE);
      mNumberOfChannels = ois.readShort();
      mGlobal.setNumericProperty(SpectrumProperties.BeamEnergy, ois.readFloat());
      mGlobal.setNumericProperty(SpectrumProperties.DetectorTilt, ois.readFloat());
      setPropertyWhenNotEqualG(SpectrumProperties.QuantumEfficiency, ois.readFloat(), 0.0);
      ois.readShort(); // spare2
      ois.readShort(); // spare3
      // Plot_InfoRec
      ois.readShort(); // mPlot_Connected
      ois.readShort(); // mPlot_Symbol
      { // Color
         ois.readUnsignedShort(); // 256*Red
         ois.readUnsignedShort(); // 256*Green
         ois.readUnsignedShort(); // 256*Blue
      }
   }

   public int getSpectrumCount() {
      return (mLastSpec - mFirstSpec) + 1;
   }

   public ISpectrumData getSpectrum(int i) {
      return mSpectra[i];
   }

}
