package gov.nist.microanalysis.EPQTools;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import gov.nist.microanalysis.EPQLibrary.EPQException;

/**
 * <p>
 * A simple class to assist writing raw binary files consisting of a matrix of
 * data points of uniform length.
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
 * @deprecated Use EPQTools.Ripple file instead
 */
@Deprecated
public class WriteRaw {
   public final static String SIGNED = "signed";
   public final static String UNSIGNED = "unsigned";
   public final static String FLOAT = "float";

   private final FileOutputStream mData;
   private final int mNRows;
   private final int mNCols;
   private final int mDatumLength;
   private final int mItemSize;

   /**
    * WriteRaw - Create a raw data file containing nRows rows and nCol columns
    * of data items of datumSize bytes. The result is placed in a file named
    * filename.
    * 
    * @param nRows
    *           int
    * @param nCols
    *           int
    * @param datumLength
    *           int - The number of atomic data items in each of the nRows*nCols
    *           data blocks.
    * @param itemSize
    *           int - The length of each atomic data item in bytes (usually 1,
    *           2, 4, or 8)
    * @param filename
    *           String
    * @deprecated Use EPQTools.Ripple file instead
    */
   @Deprecated
   public WriteRaw(int nRows, int nCols, int datumLength, int itemSize, String filename) throws FileNotFoundException {
      mNRows = nRows;
      mNCols = nCols;
      mDatumLength = datumLength;
      mItemSize = itemSize;
      mData = new FileOutputStream(filename, false);
   }

   /**
    * WriteRaw - Create a raw data file containing nRows rows and nCol columns
    * of data items of datumSize bytes. The result is placed in a file named
    * filename.
    * 
    * @param nRows
    *           int
    * @param nCols
    *           int
    * @param datumLength
    *           int - The number of atomic data items in each of the nRows*nCols
    *           data blocks.
    * @param itemSize
    *           int - The length of each atomic data item in bytes (usually 1,
    *           2, 4, or 8)
    * @param fos
    *           - A FileOutputStream into which to write the data
    * @deprecated Use EPQTools.Ripple file instead
    */
   @Deprecated
   public WriteRaw(int nRows, int nCols, int datumLength, int itemSize, FileOutputStream fos) throws FileNotFoundException {
      mNRows = nRows;
      mNCols = nCols;
      mDatumLength = datumLength;
      mItemSize = itemSize;
      mData = fos;
   }

   /**
    * writeHeader - Writes a separate file containing contextual information
    * describing the raw data file. Usually the file name for these files ends
    * in ".rpl".
    * 
    * @param filename
    *           String
    * @deprecated Use EPQTools.Ripple file instead
    */
   @Deprecated
   public void writeHeader(String filename) throws EPQException {
      try {
         try (final PrintWriter pw = new PrintWriter(new FileOutputStream(filename))) {
            pw.println("key\tvalue");
            pw.println("width\t" + Integer.toString(mNCols));
            pw.println("height\t" + Integer.toString(mNRows));
            pw.println("depth\t" + Integer.toString(mDatumLength));
            pw.println("offset\t0");
            pw.println("data-length\t" + Integer.toString(mItemSize));
            pw.println("data-type\tsigned"); // Since (almost) all Java types
                                             // are
            // signed
            pw.println("byte-order\tbig-endian"); // Since this is the default
                                                  // for
            // Java
            pw.println("record-by\tvector");
         }
      } catch (final Exception ex) {
         throw new EPQException(ex);
      }
   }

   /**
    * offset - Computes the offset to the beginning of the specified data item.
    * 
    * @param row
    *           int
    * @param col
    *           int
    * @return int
    */
   private long offset(int row, int col) {
      if ((row < 0) || (row >= mNRows))
         throw new IllegalArgumentException("Row out of range in WriteRaw.offset");
      if ((col < 0) || (col >= mNCols))
         throw new IllegalArgumentException("Column out of range in WriteRaw.offset");
      return (((long) row * (long) mNCols) + col) * (mDatumLength * mItemSize);
   }

   /**
    * nextRow - The next row ready to accept new data.
    * 
    * @return int
    */
   public int nextRow() {
      try {
         return (int) ((mData.getChannel().size() / (mDatumLength * mItemSize)) / mNCols);
      } catch (final IOException ex) {
         return 0;
      }
   }

   /**
    * nextColumn - The next column ready to accept new data.
    * 
    * @return int
    */
   public int nextColumn() {
      try {
         return (int) ((mData.getChannel().size() / (mDatumLength * mItemSize)) % mNCols);
      } catch (final IOException ex) {
         return 0;
      }
   }

   /**
    * getStream - Get an instance of OutputStream configured correctly to write
    * the datum at row, col. It is not necessary to write the data in any
    * particular order although if the data is out of natural order, the
    * contents of the intervening data will contain undefined values.
    * 
    * @param row
    *           int
    * @param col
    *           int
    * @return OutputStream
    */
   public OutputStream getStream(int row, int col) throws IOException {
      mData.getChannel().position(offset(row, col));
      return mData;
   }

   /**
    * close - Closes the underlying file.
    * 
    * @throws IOException
    */
   public void close() throws IOException {
      mData.close();
   }
}
