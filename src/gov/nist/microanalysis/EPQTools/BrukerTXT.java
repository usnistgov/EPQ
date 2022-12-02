package gov.nist.microanalysis.EPQTools;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.xmlpull.v1.XmlPullParserException;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;

/**
 * <p>
 * This was designed to read a type of Bruker text output file that Monika
 * Doneva (monika.doneva@mail.polimi.it) sent me. It is based on parsing one
 * instance of a file she sent me. It is not likely to be very robust. I believe
 * that there are also German language versions of this text file that it won't
 * read.
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
public class BrukerTXT extends BaseSpectrum {

   private final SpectrumProperties mProperties = new SpectrumProperties();
   private double[] mData;

   public static boolean isInstanceOf(InputStream is) {
      final InputStreamReader isr = new InputStreamReader(is);
      final BufferedReader br = new BufferedReader(isr);
      String first;
      try {
         first = br.readLine();
         if (first == null)
            return false;
         final String second = br.readLine();
         return second != null && first.equals("Bruker Nano GmbH Berlin, Germany") && second.startsWith("Esprit");
      } catch (final IOException e) {
         return false;
      }
   }

   public BrukerTXT(String filename) throws FileNotFoundException, XmlPullParserException, IOException, EPQException {
      this(new FileInputStream(filename));
   }

   public BrukerTXT(InputStream is) throws IOException, EPQException {
      final InputStreamReader isr = new InputStreamReader(is);
      final BufferedReader br = new BufferedReader(isr);
      String first = br.readLine();
      if ((first == null) || !first.equals("Bruker Nano GmbH Berlin, Germany"))
         throw new EPQException("This spectrum does not appear to be a Bruker TXT file spectrum.");
      String second = br.readLine();
      if ((second == null) || !second.startsWith("Esprit"))
         throw new EPQException("This spectrum does not appear to be a Bruker TXT file spectrum.");
      for (String str = br.readLine(); (str != null) && (!str.startsWith("Energy Counts")); str = br.readLine()) {
         str = str.trim();
         if (str.startsWith("Date: ")) {
            // Ignore
         } else if (str.startsWith("Real time:")) {
            double val = Double.parseDouble(str.substring("Real time:".length()).trim());
            mProperties.setNumericProperty(SpectrumProperties.RealTime, val / 1000.0);
         } else if (str.startsWith("Life time:")) {
            double val = Double.parseDouble(str.substring("Life time:".length()).trim());
            mProperties.setNumericProperty(SpectrumProperties.LiveTime, val / 1000.0);
         } else if (str.startsWith("Pulse density:")) {
            // Ignore
         } else if (str.startsWith("Primary energy:")) {
            double val = Double.parseDouble(str.substring("Primary Energy:".length()).trim());
            mProperties.setNumericProperty(SpectrumProperties.BeamEnergy, val);
         } else if (str.startsWith("Take off angle:")) {
            double val = Double.parseDouble(str.substring("Take off angle:".length()).trim());
            mProperties.setNumericProperty(SpectrumProperties.TakeOffAngle, val);
         } else if (str.startsWith("Tilt angle:")) {
            double val = Double.parseDouble(str.substring("Tilt angle:".length()).trim());
            mProperties.setNumericProperty(SpectrumProperties.DetectorTilt, val);
         } else if (str.startsWith("Azimut angle:")) {
            // Ignore
         } else if (str.startsWith("Detector type:")) {
            String typ = str.substring("Detector type:".length());
            mProperties.setTextProperty(SpectrumProperties.DetectorDescription, typ);
         } else if (str.startsWith("Window type:")) {
            // String typ = str.substring("Window type:".length());
            // mProperties.setTextProperty(SpectrumProperties.DetectorWindow,
            // typ);
         } else if (str.startsWith("Detector thickness:")) {
            double val = Double.parseDouble(str.substring("Detector thickness:".length()).trim());
            mProperties.setNumericProperty(SpectrumProperties.DetectorThickness, val);
         } else if (str.startsWith("Si dead layer:")) {
            double val = Double.parseDouble(str.substring("Si dead layer:".length()).trim());
            mProperties.setNumericProperty(SpectrumProperties.DeadLayer, val);
         } else if (str.startsWith("Calibration, lin.:")) {
            double val = Double.parseDouble(str.substring("Calibration, lin.:".length()).trim());
            mProperties.setNumericProperty(SpectrumProperties.EnergyScale, val);
         } else if (str.startsWith("Calibration, abs.:")) {
            double val = Double.parseDouble(str.substring("Calibration, abs.:".length()).trim());
            mProperties.setNumericProperty(SpectrumProperties.EnergyOffset, val);
         } else if (str.startsWith("Mn FWHM:")) {
            double val = Double.parseDouble(str.substring("Mn FWHM:".length()).trim());
            mProperties.setNumericProperty(SpectrumProperties.Resolution, val);
            mProperties.setNumericProperty(SpectrumProperties.ResolutionLine, SpectrumUtils.E_MnKa);
         } else if (str.startsWith("Fano factor:")) {
            // Ignore
         } else if (str.startsWith("Channels:")) {
            mData = new double[Integer.parseInt(str.substring("Channels:".length()).trim())];
         }
      }
      try {
         int i = 0;
         for (String str = br.readLine().trim(); (str.length() > 0) && (i < mData.length); str = br.readLine().trim()) {
            String[] items = str.split("\\h+");
            if (items.length > 1)
               mData[i] = Double.parseDouble(items[1]);
            ++i;
         }
      } catch (Exception e) {
         System.err.println(e);
      }
   }

   @Override
   public int getChannelCount() {
      return mData.length;
   }

   @Override
   public double getCounts(int i) {
      return mData[i];
   }

   @Override
   public SpectrumProperties getProperties() {
      return mProperties;
   }

}
