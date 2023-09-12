package gov.nist.microanalysis.Utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;

/**
 * <p>
 * A class for reading in *.csv files
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Daniel "Ooblioob" Davis
 * @version 1.0
 */

abstract public class CSVReader {

   static public class FileReader extends CSVReader {
      private final File mFile;

      public FileReader(File file, boolean TrimZeros) {
         super(TrimZeros);
         mFile = file;
      }

      @Override
      public double[][] getResource(Class<?> clss) {
         if (mData == null)
            try {
               final Reader isr = new InputStreamReader(new FileInputStream(mFile), "US-ASCII");
               mData = loadTable(isr);
            } catch (final Exception e) {
               throw new EPQFatalException(e);
            }
         return mData;
      }

   }

   static public class ResourceReader extends CSVReader {
      private final String mFileName;

      public ResourceReader(String FileName, boolean TrimZeros) {
         super(TrimZeros);
         mFileName = FileName;
      }

      @Override
      public double[][] getResource(Class<?> clss) {
         if (mData == null)
            try {
               final InputStream res = clss.getResourceAsStream(mFileName);
               if(res==null)
                  throw new EPQFatalException("Unable to find the resource "+mFileName);
               final Reader isr = new InputStreamReader(res, "US-ASCII");
               mData = loadTable(isr);
            } catch (final Exception e) {
               throw new EPQFatalException(e);
            }
         return mData;
      }
   }

   private boolean mTrimExtraZeros = true;

   protected double[][] mData;

   protected CSVReader(boolean trimZeros) {
      mTrimExtraZeros = trimZeros;
   }

   /**
    * Reads in all data from the csv file and returns it to the client. If the
    * resource is in a resource stream then it is assumed to be in the same path
    * as CSVReader (gov.nist.microanalysis.Utility).
    * 
    * @return double[][] - returns a 2-D array of doubles.
    */
   public double[][] getResource() {
      return getResource(CSVReader.class);
   }

   /**
    * Reads in all data from the csv file and returns it to the client. If the
    * resource is in a resource stream then it is assumed to be in the same path
    * as the argument class.
    * 
    * @param clss
    *           A class specifying the location of the resource
    * @return double[][] - returns a 2-D array of doubles.
    */
   abstract public double[][] getResource(Class<?> clss);

   protected double[][] loadTable(Reader isr) throws IOException {
      final BufferedReader br = new BufferedReader(isr);
      try {
         final ArrayList<double[]> rowList = new ArrayList<double[]>();
         {
            String line = br.readLine().trim();
            final ArrayList<Double> resList = new ArrayList<Double>();
            while (line != null) {
               while (line.startsWith("//") || line.startsWith("#")) {
                  line = br.readLine();
                  if (line != null)
                     line = line.trim();
               }
               int end = 0, last = -1, colCx = 0;
               resList.clear();
               for (int start = 0; end != -1; start = end + 1) {
                  end = line.indexOf(',', start);
                  final String item = line.substring(start, end != -1 ? end : line.length()).trim();
                  if (item.startsWith("#"))
                     break;
                  // Default to NaN
                  final double d = ((item.length() > 0) && (item.compareToIgnoreCase("NaN") != 0) ? Double.parseDouble(item) : Double.NaN);
                  resList.add(Double.valueOf(d));
                  ++colCx;
                  // Check for the last non-zero / non-NaN column
                  if (!((d == 0.0) || Double.isNaN(d)))
                     last = colCx;
               }
               if (!mTrimExtraZeros)
                  last = colCx;
               double[] thisRow = null;
               if (last >= 0) { // Trim at the last non-zero value
                  thisRow = new double[last];
                  for (int index = 0; index < last; ++index)
                     thisRow[index] = (resList.get(index)).doubleValue();
               }
               rowList.add(thisRow);
               line = br.readLine();
            }
         }
         final double[][] res = new double[rowList.size()][];
         for (int i = 0; i < rowList.size(); ++i)
            res[i] = rowList.get(i);
         return res;
      } finally {
         br.close();
      }
   }
}
