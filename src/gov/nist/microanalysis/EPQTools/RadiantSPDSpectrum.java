package gov.nist.microanalysis.EPQTools;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;

/**
 * <p>
 * A basic class for reading Radiant/Photon Imaging/SEII PiSpec files. The
 * format was deduced and so is not likely to make full use of available
 * information.
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
public class RadiantSPDSpectrum extends BaseSpectrum {

   private final SpectrumProperties mProperties = new SpectrumProperties();
   private double[] mChannels;

   private static final int SKIP_LEN = 34;

   public static boolean isInstanceOf(InputStream is) {
      try {
         if (is instanceof FileInputStream) {
            final FileInputStream fis = (FileInputStream) is;
            return fis.getChannel().size() == (is.read() + 1 + SKIP_LEN + (4 * 2048));
         }
         return false;
      } catch (final IOException e) {
         return false;
      }
   }

   private int toInt(byte[] bi) {
      return ((bi[3] & 0xFF) << 24) + ((bi[2] & 0xFF) << 16) + ((bi[1] & 0xFF) << 8) + (bi[0] & 0xFF);
   }

   // (8245 - 1) - 18 = 8226 - 8192 = 34

   private void read(InputStream is) throws IOException {
      try (final DataInputStream dis = new DataInputStream(new BufferedInputStream(is))) {
         final int len = dis.readByte();
         final byte[] b = new byte[len];
         if (dis.read(b) != len)
            throw new IOException();
         // / TODO: Figure out where the energy scale is stored...
         setEnergyScale(0.0, 10.0);
         mProperties.setTextProperty(SpectrumProperties.SpecimenDesc, new String(b));
         mProperties.setTextProperty(SpectrumProperties.Software, "Radiant PiSpec");
         final byte[] bi = new byte[4];

         // Read 18-21 Live_time
         dis.skip(18);
         dis.read(bi);
         mProperties.setNumericProperty(SpectrumProperties.LiveTime, Float.intBitsToFloat(toInt(bi)));
         // Read 26-29 - Process time
         dis.skip(4);
         dis.read(bi);
         mProperties.setNumericProperty(SpectrumProperties.PulseProcessTime, Float.intBitsToFloat(toInt(bi)));
         dis.skip(SKIP_LEN - (18 + 4 + 4));
         mChannels = new double[2048];
         for (int ch = 0; ch < mChannels.length; ++ch) {
            dis.read(bi);
            mChannels[ch] = Float.intBitsToFloat(toInt(bi));
         }
      }
   }

   /**
    * Constructs a RadiantSPDSpectrum
    */
   public RadiantSPDSpectrum(InputStream is) throws IOException {
      super();
      read(is);
   }

   /**
    * getChannelCount
    * 
    * @return int
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getChannelCount()
    */
   @Override
   public int getChannelCount() {
      return (mChannels != null ? mChannels.length : 0);
   }

   /**
    * getCounts
    * 
    * @param i
    * @return double
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getCounts(int)
    */
   @Override
   public double getCounts(int i) {
      return mChannels[i];
   }

   /**
    * Returns the SpectrumProperties associated with this spectrum.
    * 
    * @return SpectrumProperties
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getProperties()
    */
   @Override
   public SpectrumProperties getProperties() {
      return mProperties;
   }

   public void setFilename(String filename) {
      mProperties.setTextProperty(SpectrumProperties.SourceFile, filename);
   }
}
