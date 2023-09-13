/**
 * <p>
 * A crude reader for IXRF spectra.
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
package gov.nist.microanalysis.EPQTools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.ToSI;

/**
 * This is a very crude reader for IXRF spectra files. The implementation is
 * based on what I could deduce from the a small set of XRF spectra. Many of the
 * data items in the file are simply ignored because there was not sufficient
 * information to deduce the usage.
 */
public class IXRFSpectrum extends BaseSpectrum {

   private final SpectrumProperties mProperties = new SpectrumProperties();
   private double[] mChannels = null;

   transient private NumberFormat mDefaultFormat;

   @Override
   public int getChannelCount() {
      return mChannels != null ? mChannels.length : 0;
   }

   @Override
   public double getCounts(int ch) {
      return mChannels[ch];
   }

   /**
    * Constructs a IXRFSpectrum from the specified InputStream.
    */
   public IXRFSpectrum(InputStream is) throws IOException, EPQException, ParseException {
      super();
      setEnergyScale(0.0, 10.0);
      read(is);
   }

   @Override
   public SpectrumProperties getProperties() {
      return mProperties;
   }

   private void reset() {
      mProperties.clear();
      setEnergyScale(0.0, 10.0);
      mChannels = null;
   }

   private String[] parseCSV(String csvLine) {
      final List<String> al = new ArrayList<String>();
      final StringBuffer tmp = new StringBuffer();
      for (int i = csvLine.length() - 1; i >= 0; --i) {
         char c = csvLine.charAt(i);
         if (c == '"')
            for (--i; (i >= 0) && ((c = csvLine.charAt(i)) != '"'); --i)
               tmp.append(c);
         else if (c == ',') {
            al.add(tmp.reverse().toString());
            tmp.setLength(0);
         } else
            tmp.append(c);
      }
      if (tmp.length() > 0)
         al.add(tmp.reverse().toString());
      final String[] res = new String[al.size()];
      for (int i = res.length - 1; i >= 0; i--)
         res[i] = al.get(res.length - (i + 1));
      return res;
   }

   public void setFilename(String filename) {
      mProperties.setTextProperty(SpectrumProperties.SourceFile, filename);
   }

   private boolean ParseLine(String line) {
      final String[] items = parseCSV(line);
      if (items.length > 0) {
         try {
            if (items[0].equalsIgnoreCase("eV per Channel"))
               setEnergyScale(getZeroOffset(), mDefaultFormat.parse(items[1]).doubleValue());
            else if (items[0].equalsIgnoreCase("Number of Channels")) {
               final int n = mDefaultFormat.parse(items[1].trim()).intValue();
               mChannels = new double[n];
            } else if (items[0].equalsIgnoreCase("ElevationAngle"))
               mProperties.setNumericProperty(SpectrumProperties.TakeOffAngle, mDefaultFormat.parse(items[1]).doubleValue());
            else if (items[0].equalsIgnoreCase("ActiveArea"))
               mProperties.setNumericProperty(SpectrumProperties.DetectorArea, mDefaultFormat.parse(items[1]).doubleValue());
            else if (items[0].equalsIgnoreCase("CalZero"))
               setEnergyScale(ToSI.eV(mDefaultFormat.parse(items[1]).doubleValue()), getChannelWidth());
            // } else if(items[0].equalsIgnoreCase("CalGain")){
            // } else if(items[0].equalsIgnoreCase("OriginalSpectrum")){
            else if (items[0].equalsIgnoreCase("NoProcessedSpectrum"))
               mProperties.setBooleanProperty(SpectrumProperties.IsDerived, false);
            else if (items[0].equalsIgnoreCase("SiThick"))
               mProperties.setNumericProperty(SpectrumProperties.DetectorThickness, mDefaultFormat.parse(items[1]).doubleValue());
            // } else if(items[0].equalsIgnoreCase("GoldThickness")){
            // mProperties.setNumericProperty(SpectrumProperties.DetectorThickness,mDefaultFormat.parse(items[1]).doubleValue());
            // } else if(items[0].equalsIgnoreCase("DeadLayer")){
            // } else if(items[0].equalsIgnoreCase("WindowThick")){
            // } else if(items[0].equalsIgnoreCase("WindowIceCoating")){
            // } else if(items[0].equalsIgnoreCase("SupportGrid")){
            // } else if(items[0].equalsIgnoreCase("WindowIceCoating")){
            // } else if(items[0].equalsIgnoreCase("GridFraction")){
         } catch (final ParseException e) {
            // Just ignore it...
         }
         return !items[0].equalsIgnoreCase("Number of Channels");
      }
      return true;
   }

   private void read(InputStream is) throws IOException, EPQException, ParseException {
      mProperties.clear();
      reset();
      final Reader rd = new InputStreamReader(is, Charset.forName("US-ASCII"));
      final BufferedReader br = new BufferedReader(rd);
      // Number always use '.' as decimal separator
      mDefaultFormat = NumberFormat.getInstance(Locale.ENGLISH);
      {
         String line = br.readLine().trim();
         if (!line.toLowerCase().startsWith("iridium"))
            throw new EPQException("This does not seem to be a valid IXRF spectrum.");
         mProperties.setTextProperty(SpectrumProperties.Software, line);
         line = br.readLine().trim().toLowerCase();
         if (!line.startsWith("spectrum"))
            throw new EPQException("This does not seem to be a valid IXRF spectrum.");
      }
      while (ParseLine(br.readLine().trim().toLowerCase())) {
         // Just do it...
      }
      // Finally parse the channel data...
      int i = 0;
      for (String tmp = br.readLine(); (tmp != null) && (i < mChannels.length); tmp = br.readLine())
         mChannels[i++] = mDefaultFormat.parse(tmp.trim()).doubleValue();
      mDefaultFormat = null;
   }

   public static boolean isInstanceOf(InputStream is) {
      boolean res = true;
      try {
         try (final BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("US-ASCII")))) {
            String line = br.readLine().trim();
            res = line.startsWith("Iridium");
            line = br.readLine().trim();
            res = res & line.startsWith("Spectrum");
         }
      } catch (final Exception ex) {
         res = false;
      }
      return res;
   }
}
