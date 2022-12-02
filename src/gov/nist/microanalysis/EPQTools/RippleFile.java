/**
 * <p>
 * A class to handle reading and writing LISPIX-style Ripple files. A Ripple
 * file is a three-dimensional cube of data consisting of rows x cols x nItems.
 * The items may be 1,2,4 byte signed, 1,2,4 byte unsigned or 4,8 byte floats.
 * The image is written either little-endian or big-endian. This class only
 * handles "vector" type Ripple files (not "image"). The Ripple file is never
 * read into memory by this class. Rather items are read as requested.
 * </p>
 * <p>
 * Reads and writes will convert integers into floats under the assumption that
 * there will be little (if any loss of information). However floats are never
 * coverted to integers.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas
 * @version 1.0
 */
package gov.nist.microanalysis.EPQTools;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.imageio.ImageIO;

import gov.nist.microanalysis.Utility.Math2;

/**
 * A class for reading and writing LISPIX-style Ripple files (rpl/raw)
 */
public class RippleFile implements AutoCloseable {
   final public static String BIG_ENDIAN = EndianRandomAccessFile.BigEndian;
   final public static String LITTLE_ENDIAN = EndianRandomAccessFile.LittleEndian;
   final public static String DONT_CARE_ENDIAN = "dont-care";
   final public static String IMAGE_ORDER = "image";
   final public static String VECTOR_ORDER = "vector";
   final public static String DONT_CARE_ORDER = "dont-care";
   final public static String SIGNED = "signed";
   final public static String UNSIGNED = "unsigned";
   final public static String FLOAT = "float";

   private long mWidth;
   private long mHeight;
   private long mDepth;
   private long mByteDepth;
   private long mOffset;
   private String mDataType;
   private String mOrder;
   private final String mRplFilename;
   private String mRawFilename;

   transient EndianRandomAccessFile mRawStream;
   transient long mCurRow;
   transient long mCurCol;
   transient long mCurDepth;

   private void incrementPos(int elements) {
      mCurDepth += elements;
      if (mCurDepth >= mDepth) {
         mCurCol += mCurDepth / mDepth;
         mCurDepth = mCurDepth % mDepth;
         if (mCurCol >= mWidth) {
            mCurRow += mCurCol / mWidth;
            mCurCol = mCurCol % mWidth;
         }
      }
      assert mCurRow >= 0;
      assert (mCurRow < mHeight) || isEof();
      assert mCurCol >= 0;
      assert mCurCol < mWidth;
      assert mCurDepth >= 0;
      assert mCurDepth < mDepth;
   }

   /**
    * Constructs a new RippleFile with the specified properties.
    * 
    * @param width
    *           - The width of each row (# of columns) within the Ripple file
    * @param height
    *           - The number of rows
    * @param depth
    *           - The number of data items at each [row,col] position
    * @param dataType
    *           - One of RippleFile.SIGNED, RippleFile.UNSIGNED or
    *           RippleFile.FLOAT
    * @param byteDepth
    *           - 1, 2, 4 for SIGNED or UNSIGNED, 4 or 8 for FLOAT
    * @param order
    *           - BIG_ENDIAN, LITTLE_ENDIAN or DONT_CARE_ENDIAN
    * @param rplFile
    *           - File name (usually *.rpl)
    * @param rawFile
    *           - File name (usually *.raw)
    * @throws IOException
    */
   public RippleFile(int width, int height, int depth, String dataType, int byteDepth, String order, String rplFile, String rawFile)
         throws IOException {
      super();
      assert (width > 0);
      assert (height > 0);
      assert (depth > 0);
      mWidth = width;
      mHeight = height;
      mDepth = depth;
      assert ((dataType.equals(SIGNED)) || (dataType.equals(UNSIGNED)) || (dataType.equals(FLOAT)));
      mDataType = dataType;
      assert ((!dataType.equals(FLOAT)) || (byteDepth == 4) || (byteDepth == 8));
      assert ((!dataType.equals(SIGNED)) || (byteDepth == 1) || (byteDepth == 2) || (byteDepth == 4));
      assert ((!dataType.equals(UNSIGNED)) || (byteDepth == 1) || (byteDepth == 2) || (byteDepth == 4));
      assert ((byteDepth == 1) || (byteDepth == 2) || (byteDepth == 4) || (byteDepth == 8));
      mByteDepth = byteDepth;
      assert ((order == BIG_ENDIAN) || (order == LITTLE_ENDIAN) || (order == DONT_CARE_ENDIAN));
      mOrder = (order.equals(DONT_CARE_ENDIAN) ? BIG_ENDIAN : order);
      mOffset = 0;
      mRplFilename = rplFile;
      mRawFilename = rawFile;
      // Open both output files
      mRawStream = new EndianRandomAccessFile(mRawFilename, "rw", mOrder);
      try (final PrintWriter pw = new PrintWriter(new FileOutputStream(mRplFilename))) {
         // Write the header
         pw.println("key\tvalue");
         pw.println("width\t" + Long.toString(mWidth));
         pw.println("height\t" + Long.toString(mHeight));
         pw.println("depth\t" + Long.toString(mDepth));
         pw.println("offset\t0");
         pw.println("data-length\t" + Long.toString(mByteDepth));
         pw.println("data-type\t" + mDataType);
         if (mByteDepth == 1)
            pw.println("byte-order\t+dont-care");
         else if (mOrder == BIG_ENDIAN)
            pw.println("byte-order\tbig-endian");
         else
            pw.println("byte-order\tlittle-endian");
         pw.println("record-by\t" + (mDepth == 1 ? DONT_CARE_ORDER : VECTOR_ORDER));
      }
      // Finish initialization
      mRawFilename = rawFile;
      mCurRow = 0;
      mCurCol = 0;
      mCurDepth = 0;
   }

   /**
    * fileSize - Returns the size of the full raw file in bytes.
    * 
    * @return int
    */
   public long fileSize() {
      return (mByteDepth * mWidth * mHeight * mDepth) + mOffset;
   }

   private static String findRaw(String rplFile) throws FileNotFoundException {
      File rawFile = new File(rplFile.replace(".rpl", ".raw"));
      if (rawFile.exists())
         return rawFile.toString();
      rawFile = new File(rplFile.replace(".rpl", "."));
      if (rawFile.exists())
         return rawFile.toString();
      throw new FileNotFoundException("Unable to find a file to associate with " + rplFile);
   }

   /**
    * Constructs a RippleFile from the name of the header file. The name of the
    * raw file is inferred from the name of the header as first "header.rpl"
    * then "header.". If neither is found then a FileNotFoundException is
    * thrown.
    * 
    * @param rplFile
    *           The name of the Ripple header file
    * @param readOnly
    *           Should the file be opened as read-only?
    * @throws Exception
    * @throws IOException
    * @throws FileNotFoundException
    */
   public RippleFile(String rplFile, boolean readOnly) throws Exception, IOException, FileNotFoundException {
      this(rplFile, findRaw(rplFile), readOnly);
   }

   /**
    * Constructs a RippleFile given explicit paths to the Ripple and raw files.
    * 
    * @param rplFile
    *           The name of the Ripple header file
    * @param rawFile
    *           The name of the raw file
    * @param readOnly
    *           Should the file be opened as read-only?
    * @throws Exception
    * @throws IOException
    * @throws FileNotFoundException
    */
   public RippleFile(String rplFile, String rawFile, boolean readOnly) throws Exception, IOException, FileNotFoundException {
      super();
      mRplFilename = rplFile;
      mRawFilename = rawFile;
      try (final FileReader fr = new FileReader(mRplFilename)) {
         try (final BufferedReader br = new BufferedReader(fr)) {
            mWidth = -1;
            mHeight = -1;
            mDepth = -1;
            mOffset = 0;
            mByteDepth = -1;
            mDataType = null;
            mOrder = DONT_CARE_ENDIAN;
            String str = br.readLine();
            if ((str == null) || (!str.matches("key\\s+value")))
               throw new Exception("The header file does not appear to be a valid RPL header.");
            do {
               str = br.readLine();
               if (str != null)
                  str = str.trim().replaceFirst("\\s+", "\t");
               final int p = str != null ? str.indexOf('\t') : -1;
               if (p >= 0) {
                  final String key = str.substring(0, p);
                  final String value = str.substring(p + 1).trim();
                  if (key.compareToIgnoreCase("width") == 0)
                     mWidth = Integer.parseInt(value);
                  else if (key.compareToIgnoreCase("height") == 0)
                     mHeight = Integer.parseInt(value);
                  else if (key.compareToIgnoreCase("depth") == 0)
                     mDepth = Integer.parseInt(value);
                  else if (key.compareToIgnoreCase("offset") == 0)
                     mOffset = Integer.parseInt(value);
                  else if (key.compareToIgnoreCase("data-type") == 0) {
                     if (value.compareToIgnoreCase("signed") == 0)
                        mDataType = SIGNED;
                     else if (value.compareToIgnoreCase("unsigned") == 0)
                        mDataType = UNSIGNED;
                     else if (value.compareToIgnoreCase("float") == 0)
                        mDataType = FLOAT;
                     else
                        throw new Exception("Unexpected type (" + value + ") in data-type key.");
                  } else if (key.compareToIgnoreCase("data-length") == 0)
                     mByteDepth = Integer.parseInt(value);
                  // validate this later...
                  else if (key.compareToIgnoreCase("byte-order") == 0) {
                     if (value.compareToIgnoreCase("big-endian") == 0)
                        mOrder = BIG_ENDIAN;
                     else if (value.compareToIgnoreCase("little-endian") == 0)
                        mOrder = LITTLE_ENDIAN;
                     else if (value.compareToIgnoreCase("dont-care") == 0)
                        mOrder = DONT_CARE_ENDIAN;
                     else
                        throw new Exception("Unexpected ordering (" + value + ") in data-type key.");
                  } else if (key.compareToIgnoreCase("record-by") == 0)
                     if ((value.compareToIgnoreCase("vector") != 0) && (value.compareToIgnoreCase("dont-care") != 0))
                        throw new Exception("The 'image' 'recorded-by' method is not currently supported.");
               }
            } while (str != null);
            if (mWidth < 0)
               throw new Exception("Image width not initialized by header.");
            if (mHeight < 0)
               throw new Exception("Image height not initialized by header.");
            if (mDepth < 0)
               throw new Exception("Image depth not initialized by header.");
            if ((mDataType == SIGNED) || (mDataType == UNSIGNED)) {
               if (!((mByteDepth == 1) || (mByteDepth == 2) || (mByteDepth == 4)))
                  throw new Exception("Only 1,2 and 4 byte integers are currently supported.");
            } else if (mDataType == FLOAT) {
               if (!((mByteDepth == 4) || (mByteDepth == 8)))
                  throw new Exception("Only 4 and 8 byte floats are currently supported.");
            } else
               throw new Exception("Data-type not initialized by header.");
            if (mOrder == DONT_CARE_ENDIAN) {
               if (mByteDepth != 1)
                  throw new Exception("The order=dont-care but the byte-depth isn't 1.");
               mOrder = BIG_ENDIAN;
            }
            mRawStream = new EndianRandomAccessFile(mRawFilename, readOnly ? "r" : "rw", mOrder);
            mCurRow = 0;
            mCurCol = 0;
            mCurDepth = 0;
         }
      }
   }

   /**
    * close - Closes the associated disk files. Don't try to read, write or seek
    * after performing this operation.
    * 
    * @throws IOException
    */
   @Override
   public void close() throws IOException {
      mRawStream.close();
      mRawStream = null;
   }

   /**
    * seek - Moves the read/write pointer to the start of the specified item at
    * [row,col]
    * 
    * @param row
    * @param col
    * @throws IOException
    */
   public void seek(int row, int col) throws IOException {
      seek(row, col, 0);
   }

   /**
    * seek - Moves the read/write pointer to the start of the specified datum at
    * [row,col,item]
    * 
    * @param row
    * @param col
    * @param item
    * @throws IOException
    */
   public void seek(long row, long col, long item) throws IOException {
      assert row >= 0;
      assert row < mHeight;
      assert col >= 0;
      assert col < mWidth;
      assert item >= 0;
      assert item < mDepth;
      mRawStream.seek((((((row * mWidth) + col) * mDepth) + item) * mByteDepth) + mOffset);
      mCurRow = row;
      mCurCol = col;
      mCurDepth = item;
   }

   /**
    * Set the current read/write position within the raw file to the specified
    * x,y coordinates. Note seek and setPosition are almost identical except
    * with seek the arguments are row,col and with setPosition it is col,row,
    * 
    * @param x
    * @param y
    * @param item
    * @throws IOException
    */
   public void setPosition(int x, int y, int item) throws IOException {
      seek(y, x, item);
   }

   /**
    * Set the current read/write position within the raw file to the specified
    * x,y coordinates. Note seek and setPosition are almost identical except
    * with seek the arguments are row,col and with setPosition it is col,row,
    * 
    * @param x
    * @param y
    * @throws IOException
    */
   public void setPosition(int x, int y) throws IOException {
      setPosition(x, y, 0);
   }

   /**
    * readDouble - Read a single datum and return the value as a double.
    * (Converts ints into doubles if necessary)
    * 
    * @return double
    * @throws IOException
    */
   public double readDouble() throws IOException {
      double res = Double.NaN;
      if (mDataType == FLOAT) {
         switch ((int) mByteDepth) {
            case 4 :
               res = mRawStream.readFloat();
               break;
            case 8 :
               res = mRawStream.readDouble();
               break;
            default :
               assert (false);
         }
         incrementPos(1);
      } else
         res = readInt();
      return res;
   }

   /**
    * readInt - Read a single integer
    * 
    * @return int
    * @throws IOException
    */
   public int readInt() throws IOException {
      // assert (mDataType == SIGNED);
      int res = Integer.MAX_VALUE;
      switch ((int) mByteDepth) {
         case 1 :
            res = mRawStream.readByte();
            break;
         case 2 :
            res = mRawStream.readShort();
            break;
         case 4 :
            res = mRawStream.readInt();
            break;
         case 8 :
            assert (false);
      }
      incrementPos(1);
      return res;
   }

   /**
    * readUnsigned - Read a single unsigned integer
    * 
    * @return long
    * @throws IOException
    */
   public long readUnsigned() throws IOException {
      long res = Long.MAX_VALUE;
      switch ((int) mByteDepth) {
         case 1 :
            res = mRawStream.readUnsignedByte();
            break;
         case 2 :
            res = mRawStream.readUnsignedShort();
            break;
         case 4 :
            res = mRawStream.readUnsignedInt();
            break;
         case 8 :
            assert (false);
      }
      incrementPos(1);
      return res;
   }

   /**
    * readInt - read len count of integers
    * 
    * @param len
    * @return int[]
    * @throws IOException
    */
   public int[] readInt(int len) throws IOException {
      final int[] res = new int[len];
      for (int i = 0; i < len; ++i)
         res[i] = readInt();
      return res;
   }

   /**
    * readDouble - read len count data objects and return them as an array of
    * doubles. (Will convert integers to doubles as necessary)
    * 
    * @param len
    * @return double[]
    * @throws IOException
    */
   public double[] readDouble(int len) throws IOException {
      final double[] res = new double[len];
      for (int i = 0; i < len; ++i)
         res[i] = readDouble();
      return res;
   }

   /**
    * readDoubleItem - read depth (getDepth()) count of data objects and return
    * them as an array of doubles. (Will convert integers into doubles as
    * necessary)
    * 
    * @return double[]
    * @throws IOException
    */
   public double[] readDoubleItem() throws IOException {
      return readDouble((int) mDepth);
   }

   /**
    * readIntItem - read depth (getDepth()) count of integer
    * 
    * @return int[]
    * @throws IOException
    */
   public int[] readIntItem() throws IOException {
      return readInt((int) mDepth);
   }

   /**
    * write - Write a single integer
    * 
    * @param val
    * @throws IOException
    */
   public void write(int val) throws IOException {
      if (mDataType == FLOAT)
         throw new IOException("ERROR: Attempting to write an float to an integer file.");
      switch ((int) mByteDepth) {
         case 1 :
            mRawStream.writeByte(val);
            break;
         case 2 :
            mRawStream.writeShort(val);
            break;
         case 4 :
            mRawStream.writeInt(val);
            break;
         case 8 :
            assert (false);
      }
      incrementPos(1);
   }

   /**
    * write - Write a single double
    * 
    * @param val
    * @throws IOException
    */
   public void write(double val) throws IOException {
      if (mDataType != FLOAT)
         throw new IOException("ERROR: Attempting to write an integer to a float file.");
      switch ((int) mByteDepth) {
         case 4 :
            mRawStream.writeFloat((float) val);
            break;
         case 8 :
            mRawStream.writeDouble(val);
            break;
         default :
            assert (false);
      }
      incrementPos(1);
   }

   /**
    * write - Write an array of integers (converts to float if necessary to be
    * compatible with the underlying file.)
    * 
    * @param vals
    * @throws IOException
    */
   public void write(int[] vals) throws IOException {
      for (final int val : vals) {
         if ((mDataType == SIGNED) || (mDataType == UNSIGNED))
            switch ((int) mByteDepth) {
               case 1 :
                  mRawStream.writeByte(mDataType == UNSIGNED ? val & 0xFF : val);
                  break;
               case 2 :
                  mRawStream.writeShort(mDataType == UNSIGNED ? val & 0xFFFF : val);
                  break;
               case 4 :
                  mRawStream.writeInt(val);
                  break;
               default :
                  assert (false);
            }
         else if (mDataType == FLOAT)
            switch ((int) mByteDepth) {
               case 4 :
                  mRawStream.writeFloat(val);
                  break;
               case 8 :
                  mRawStream.writeDouble(val);
                  break;
               default :
                  assert (false);
            }
         incrementPos(1);
      }
   }

   /**
    * write - Write an array of doubles
    * 
    * @param vals
    * @throws IOException
    */
   public void write(double[] vals) throws IOException {
      if (mDataType != FLOAT)
         throw new IOException("ERROR: Attempting to write an float to an integer file.");
      for (final double val : vals) {
         switch ((int) mByteDepth) {
            case 4 :
               mRawStream.writeFloat((float) val);
               break;
            case 8 :
               mRawStream.writeDouble(val);
               break;
            default :
               assert (false);
         }
         incrementPos(1);
      }
   }

   /**
    * getWidth - Returns the width of the underlying data matrix
    * 
    * @return int
    */
   public int getWidth() {
      return (int) mWidth;
   }

   /**
    * getHeight - Returns the height of the underlying data matrix
    * 
    * @return int
    */
   public int getHeight() {
      return (int) mHeight;
   }

   /**
    * getDepth - Returns the depth of the underlying data matrix
    * 
    * @return int
    */
   public int getDepth() {
      return (int) mDepth;
   }

   /**
    * getDatumSize - Returns the size in bytes of the underlying data objects
    * 
    * @return 1,2,4 or 8 bytes
    */
   public int getDatumSize() {
      return (int) mByteDepth;
   }

   /**
    * getDataType - Returns the type of data item
    * 
    * @return Returns RippleFile.SIGNED, RippleFile.UNSIGNED or RippleFile.FLOAT
    */
   public String getDataType() {
      return mDataType;
   }

   /**
    * getCurrentRow - Returns the row on which the next read/write will happen
    * 
    * @return int
    */
   public int getCurrentRow() {
      return (int) mCurRow;
   }

   /**
    * getCurrentCol - Returns the column on which the next read/write will
    * happen
    * 
    * @return int
    */
   public int getCurrentCol() {
      return (int) mCurCol;
   }

   /**
    * getCurrentItem - Returns the item index on which the next read/write will
    * happen
    * 
    * @return int
    */
   public int getCurrentItem() {
      return (int) mCurDepth;
   }

   /**
    * isEof - Does the current position represent the end-of-file (end of data)
    * 
    * @return true or false
    */
   public boolean isEof() {
      return ((mCurRow == mHeight) && (mCurCol == 0) && (mCurDepth == 0));
   }

   public long getOffset() {
      return mOffset;
   }

   private static IndexColorModel createColorModel() {
      final byte[] r = new byte[256];
      final byte[] g = new byte[256];
      final byte[] b = new byte[256];
      for (int i = 0; i < 256; ++i) {
         r[i] = (byte) i;
         g[i] = (byte) i;
         b[i] = (byte) i;
      }
      return new IndexColorModel(8, 256, r, g, b);
   }

   public void layersToTIFF(String fn, long begin, long end, boolean fixZero) throws IOException {
      final double[][] layer = new double[getWidth()][getHeight()];
      double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
      for (int x = 0; x < getWidth(); ++x)
         for (int y = 0; y < getHeight(); ++y) {
            double val = 0.0;
            try {
               seek(y, x, begin);
               val = Math2.sum(readDouble((int) (end - begin)));
            } catch (final Exception e) {
               val = 0.0;
            }
            if (val < min)
               min = val;
            if (val > max)
               max = val;
            layer[x][y] = val;
         }
      if (max == min)
         max = min + 1.0;
      final BufferedImage bi = new BufferedImage(layer.length, layer[0].length, BufferedImage.TYPE_BYTE_INDEXED, createColorModel());
      for (int x = 0; x < layer.length; ++x)
         for (int y = 0; y < layer[x].length; ++y) {
            final int gray = fixZero
                  ? Math2.bound((int) (256.0 * (layer[x][y] / max)), 0, 256)
                  : Math2.bound((int) (256.0 * ((layer[x][y] - min) / (max - min))), 0, 256);
            bi.setRGB(x, y, bi.getColorModel().getRGB(gray));
         }
      ImageIO.write(bi, "TIFF", new File(fn));
   }
}
