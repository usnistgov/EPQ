/**
 * <p>
 * Reads AMPTEK PMCA/MCA spectrum files.
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
import java.util.Locale;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;

/**
 * A very simple reader for *.mca spectrum files. This implementation is deduced
 * from the files extracted from an Crossroads Scientific system for XRF. PMCA
 * format seems to be an AMPTEK custom format for spectral exchange. A quick
 * Google of the AMPTEK website didn't find a specification document for the MCA
 * file format.
 */
public class PMCASpectrum extends BaseSpectrum {

   private SpectrumProperties mProperties = new SpectrumProperties();
   private double[] mChannels;

   transient private NumberFormat mDefaultFormat = null;

   /**
    * Is the file specified by this Reader likely to be a PMCA file?
    * 
    * @param is
    *           InputStream - Which will be closed by isInstanceOf
    * @return boolean
    */
   public static boolean isInstanceOf(InputStream is) {
      boolean res = true;
      try {
         try {
            try (final BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("US-ASCII")))) {
               final String line = br.readLine().trim();
               res = line.startsWith("<<PMCA SPECTRUM>>");
            }
         } finally {
            is.close(); // force closure to ensure it is not reused...
         }
      } catch (final Exception ex) {
         res = false;
      }
      return res;
   }

   /**
    * Constructs a PMCASpectrum from the contents of the specified input stream.
    */
   public PMCASpectrum(InputStream is) throws IOException {
      super();
      read(is);
   }

   public void setFilename(String filename) {
      mProperties.setTextProperty(SpectrumProperties.SourceFile, filename);
   }

   private void reset() {
      mProperties = new SpectrumProperties();
      mChannels = null;
      setEnergyScale(0.0, 10.0);
   }

   private void storeData(String prefix, String data) {
      try {
         if (prefix.startsWith("TAG"))
            mProperties.setTextProperty(SpectrumProperties.SpectrumComment, data);
         else if (prefix.startsWith("DESCRIPTION"))
            mProperties.setTextProperty(SpectrumProperties.Software, data);
         else if (prefix.startsWith("LIVE_TIME"))
            mProperties.setNumericProperty(SpectrumProperties.LiveTime, mDefaultFormat.parse(data).doubleValue());
         else if (prefix.startsWith("REAL_TIME"))
            mProperties.setNumericProperty(SpectrumProperties.RealTime, mDefaultFormat.parse(data).doubleValue());
         else if (prefix.startsWith("SERIAL_NUMBER"))
            mProperties.setTextProperty(SpectrumProperties.ClientsSampleID, data);
      } catch (final ParseException e) {
         // Just ignore it...
      }
   }

   /**
    * read - reads the parsed file in PMCA format and converts it into the
    * intermediate format, ISpectrumData.
    * 
    * @param is
    *           InputStream - the PMCA file
    * @throws IOException
    *            throws an IOException if there is an error reading the file
    */
   private void read(InputStream is) throws IOException {
      reset();
      final Reader rd = new InputStreamReader(is, Charset.forName("US-ASCII"));
      final BufferedReader br = new BufferedReader(rd);
      // Number always use '.' as decimal separator
      mDefaultFormat = NumberFormat.getInstance(Locale.US);
      String line;
      do {
         line = br.readLine();
         if (line != null) {
            final int p = line.indexOf("-");
            if (p != -1)
               storeData(line.substring(0, p).trim(), line.substring(p + 1).trim());
         }
      } while (!((line == null) || (line.startsWith("<<DATA>>"))));
      line = br.readLine();
      // Cases: nextDatum then "," or nextDatum then EOL or EOL
      final ArrayList<String> al = new ArrayList<String>();
      while (!((line == null) || (line.startsWith("<<END>>")))) {
         al.add(line.trim());
         line = br.readLine();
      }
      mChannels = new double[al.size()];
      for (int i = 0; i < mChannels.length; ++i)
         try {
            mChannels[i] = mDefaultFormat.parse(al.get(i)).doubleValue();
         } catch (final ParseException e) {
            mChannels[i] = 0.0;
         }
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
}
