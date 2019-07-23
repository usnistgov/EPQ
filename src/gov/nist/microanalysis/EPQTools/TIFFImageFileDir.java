package gov.nist.microanalysis.EPQTools;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import gov.nist.microanalysis.EPQLibrary.EPQException;

/**
 * A class for extracting contextual data from TIFF image files.
 * 
 * @author nicholas
 */
public class TIFFImageFileDir {

   final public static byte[] LITTLE_MAGIC = new byte[] {
      0x49,
      0x49,
      0x2A,
      0x00
   };

   final public static byte[] BIG_MAGIC = new byte[] {
      0x4D,
      0x4D,
      (byte) 0xA2,
      0x00
   };

   // Data sizes in bytes of the various tag data item types (index by ttByte,
   // ttAscii,... ttDouble)
   static int[] kDataSize = {
      0,
      1,
      1,
      2,
      4,
      8,
      1,
      1,
      2,
      4,
      8,
      4,
      8
   };

   static final int PLACEHOLDER = 0xDEADFEED;

   static final short ttByte = 1;
   static final short ttAscii = 2;
   static final short ttShort = 3;
   static final short ttLong = 4;
   static final short ttRational = 5;
   static final short ttSByte = 6;
   // private static final int ttUndefined = 7;
   static final short ttSShort = 8;
   static final short ttSLong = 9;
   static final short ttSRational = 10;
   static final short ttFloat = 11;
   static final short ttDouble = 12;

   static final String[] typeToStr = new String[] {
      "NULL",
      "byte",
      "ASCII",
      "short",
      "long",
      "rational",
      "signed byte",
      "undefined",
      "signed short",
      "signed long",
      "signed rational",
      "float",
      "double"
   };

   private static String typeToString(short type) {
      return typeToStr[(type > 0) && (type < typeToStr.length) ? type : 7];
   }

   static public class Field {
      private final short mTagId;
      private final short mType;
      private final int mCount;
      private final byte[] mData;
      private final ByteOrder mOrder;
      private transient int mOffset = -1;

      private int sizeOf() {
         return mCount * kDataSize[mType];
      }

      public Field(short id, int i) {
         this(id, ttLong, new int[] {
            i
         });
      }

      public Field(short id, short i) {
         this(id, ttShort, new int[] {
            i
         });
      }

      public Field(short id, short type, int[] data) {
         mTagId = id;
         mType = type;
         assert !(((type == ttSRational) || (type == ttRational)) && ((data.length % 2) != 0));
         mCount = ((type == ttSRational) || (type == ttRational)) ? data.length / 2 : data.length;
         final ByteBuffer bb = ByteBuffer.allocate(Math.max(4, sizeOf()));
         mOrder = ByteOrder.LITTLE_ENDIAN;
         bb.order(mOrder);
         for(final int ii : data)
            switch(mType) {
               case ttByte:
               case ttAscii:
                  bb.put((byte) ii);
                  break;
               case ttShort:
               case ttSShort:
                  bb.putShort((short) ii);
                  break;
               case ttSLong:
               case ttLong:
                  bb.putInt(ii);
                  break;
               case ttRational:
               case ttSRational:
                  bb.putInt(ii);
                  break;
               case ttFloat:
                  bb.putFloat(ii);
                  break;
               case ttDouble:
                  bb.putDouble(ii);
                  break;
               default:
                  assert false;
            }
         mData = bb.array();
      }

      public Field(short id, short type, double[] data) {
         mTagId = id;
         mType = type;
         mCount = data.length;
         final ByteBuffer bb = ByteBuffer.allocate(Math.max(4, sizeOf()));
         mOrder = ByteOrder.LITTLE_ENDIAN;
         bb.order(mOrder);
         for(final double ii : data)
            switch(mType) {
               case ttByte:
                  bb.put((byte) Math.round(ii));
                  break;
               case ttShort:
               case ttSShort:
                  bb.putShort((short) Math.round(ii));
                  break;
               case ttSLong:
               case ttLong:
                  bb.putInt((int) Math.round(ii));
                  break;
               case ttFloat:
                  bb.putFloat((float) ii);
                  break;
               case ttDouble:
                  bb.putDouble(ii);
                  break;
               default:
                  assert false;
            }
         mData = bb.array();
      }

      public Field(short id, String str) {
         mTagId = id;
         mType = ttAscii;
         final Charset cs = Charset.forName("US-ASCII");
         final ByteBuffer bb = cs.encode(str);
         mCount = bb.limit() + ((bb.limit() % 2) == 0 ? 2 : 1);
         assert (mCount % 2) == 0;
         mData = Arrays.copyOf(bb.array(), mCount);
         mOrder = ByteOrder.LITTLE_ENDIAN;
      }

      private Field(ByteBuffer bb)
            throws IOException {
         mTagId = bb.getShort();
         mType = bb.getShort();
         mOrder = bb.order();
         assert (mType >= ttByte);
         assert (mType <= ttDouble);
         mCount = bb.getInt();
         assert (sizeOf() > 0);
         mData = new byte[sizeOf()];
         if(mData.length <= 4) {
            final byte[] data = new byte[4];
            bb.get(data);
            for(int i = 0; i < mData.length; ++i)
               mData[i] = data[i];
         } else {
            final int offset = bb.getInt();
            final int oldPos = bb.position();
            bb.position(offset);
            bb.get(mData);
            bb.position(oldPos);
         }
      }

      @Override
      public String toString() {
         return Integer.toString(mTagId) + "[" + Integer.toHexString(shortToInt(mTagId)) + "]: " + Integer.toString(mCount)
               + " of " + typeToString(mType);
      }

      void setInt(int idx, int j) {
         final ByteBuffer bb = ByteBuffer.wrap(mData);
         bb.order(mOrder);
         bb.putInt(4 * idx, j);
      }

      String getAsString()
            throws IOException {
         try {
            return new String(mData, "US-ASCII");
         }
         catch(final UnsupportedEncodingException ex) {
            return new String();
         }
      }

      public int[] getAsIntegerArray()
            throws EPQException {
         final ByteBuffer bb = ByteBuffer.wrap(mData);
         bb.order(mOrder);
         final int[] res = new int[mCount];
         for(int i = 0; i < res.length; i++)
            switch(mType) {
               case ttByte:
               case ttSByte:
                  res[i] = bb.get();
                  break;
               case ttShort:
               case ttSShort:
                  res[i] = bb.getShort();
                  break;
               case ttSLong:
               case ttLong:
                  res[i] = bb.getInt();
                  break;
               default:
                  throw new EPQException("Unable to convert datum to an integer.");
            }
         return res;
      }

      double[] getAsDoubleArray()
            throws EPQException {
         final ByteBuffer bb = ByteBuffer.wrap(mData);
         bb.order(mOrder);
         final double[] res = new double[mCount];
         for(int i = 0; i < res.length; i++)
            switch(mType) {
               case ttByte:
               case ttSByte:
                  res[i] = bb.get();
                  break;
               case ttSShort:
               case ttShort:
                  res[i] = bb.getShort();
                  break;
               case ttLong:
               case ttSLong:
                  res[i] = bb.getInt();
                  break;
               case ttSRational:
                  res[i] = (double) bb.getInt() / (double) bb.getInt();
                  break;
               case ttFloat:
                  res[i] = bb.getFloat();
                  break;
               case ttDouble:
                  res[i] = bb.getDouble();
                  break;
               default:
                  throw new EPQException("Unable to convert datum to a double.");
            }
         return res;
      }

      private void write(ByteBuffer bb) {
         final int st = bb.position();
         bb.putShort(mTagId);
         bb.putShort(mType);
         bb.putInt(mCount);
         if(mData.length <= 4) {
            mOffset = -1;
            assert mData.length == 4;
            bb.put(mData);
         } else {
            mOffset = bb.position();
            bb.putInt(PLACEHOLDER); // placeholder
         }
         assert bb.position() == (st + 12);
      }

      private void writeData(ByteBuffer bb) {
         if(mOffset > 0) {
            assert mData.length > 4;
            // Word align
            if((bb.position() % 2) == 1)
               bb.put((byte) 0);
            final int off = bb.position();
            bb.position(mOffset);
            bb.putInt(off);
            bb.position(off);
            bb.put(mData);
         }
      }

   }

   private static int shortToInt(short sh) {
      return sh < 0 ? 65536 + sh : sh;
   }

   private final Map<Integer, Field> mTags = new TreeMap<Integer, Field>();
   private BufferedImage mBWImage;
   private final int mNext;

   public TIFFImageFileDir(ByteBuffer bb, int offset)
         throws IOException {
      bb.position(offset);
      final int nItems = bb.getShort();
      for(int i = 0; i < nItems; ++i) {
         final Field tt = new Field(bb);
         mTags.put(Integer.valueOf(shortToInt(tt.mTagId)), tt);
      }
      mNext = bb.getInt();
   }

   public TIFFImageFileDir() {
      mNext = -1;
   }

   int write(ByteBuffer bb, int nextOff) {
      writeImage(bb);
      final int ifdOff = bb.position();
      // Write the IFD offset in the memory specified by nextOff
      bb.position(nextOff);
      bb.putInt(ifdOff);
      bb.position(ifdOff);
      bb.putShort((short) mTags.size());
      for(final Field f : mTags.values())
         f.write(bb);
      final int res = bb.position();
      bb.putInt(0); // Default to no further IFDs
      for(final Field f : mTags.values())
         f.writeData(bb);
      return res;
   }

   public void addField(Field f) {
      mTags.put(Integer.valueOf(shortToInt(f.mTagId)), f);
   }

   public Field getField(short id) {
      return mTags.get(Integer.valueOf(shortToInt(id)));
   }

   private int getNextOffset() {
      return mNext;
   }

   public static List<TIFFImageFileDir> readIFD(File file)
         throws IOException, EPQException {
      ArrayList<TIFFImageFileDir> images;
      try (final FileInputStream fis = new FileInputStream(file)) {
         final FileChannel fc = fis.getChannel();
         byte[] data = new byte[(int) fc.size()];
         fis.read(data, 0, data.length);
         final ByteBuffer bb = ByteBuffer.wrap(data);
         final byte[] b = new byte[4];
         bb.get(b);
         if(Arrays.equals(b, BIG_MAGIC))
            bb.order(ByteOrder.BIG_ENDIAN);
         else if(Arrays.equals(b, LITTLE_MAGIC))
            bb.order(ByteOrder.LITTLE_ENDIAN);
         else
            throw new EPQException("This file does not appear to be a TIFF file.");
         assert bb.position() == 4;
         images = new ArrayList<TIFFImageFileDir>();
         int offset = bb.getInt();
         while(offset != 0) {
            final TIFFImageFileDir ifd = new TIFFImageFileDir(bb, offset);
            images.add(ifd);
            offset = ifd.getNextOffset();
         }
         fc.close();
      }
      return images;
   }

   private void writeImage(ByteBuffer bb) {
      if(mBWImage != null) {
         final int width = mBWImage.getWidth();
         final int size = mBWImage.getHeight() * width;
         final int stripLen = calcStripLen(width, size);
         final int nStrips = calcNStrips(size, stripLen);
         assert nStrips >= 1;
         final Field of = getField(STRIP_OFFSETS);
         assert of.mCount == nStrips;
         final Field sbc = getField(STRIP_BYTE_COUNTS);
         assert sbc.mCount == nStrips;
         final DataBuffer db = mBWImage.getRaster().getDataBuffer();
         assert db instanceof DataBufferByte;
         assert db.getDataType() == DataBuffer.TYPE_BYTE;
         assert db.getNumBanks() == 1;
         assert db.getSize() == size;
         final DataBufferByte dbb = (DataBufferByte) db;
         final byte[] data = dbb.getData();
         for(int strip = 0; strip < nStrips; ++strip) {
            final int start = strip * stripLen;
            final int len = Math.min(size - start, stripLen);
            of.setInt(strip, bb.position());
            sbc.setInt(strip, len);
            bb.put(data, start, len);
         }
      }
   }

   // Base level TIFF tags for gray scale images
   public static final short IMAGE_WIDTH = 256;
   public static final short IMAGE_LENGTH = 257;
   public static final short BITS_PER_SAMPLE = 258;
   public static final short COMPRESSION = 259; // 1
   public static final short PHOTOMETRIC_INTERPRETATION = 262;
   public static final short STRIP_OFFSETS = 273;
   public static final short ROWS_PER_STRIP = 278;
   public static final short STRIP_BYTE_COUNTS = 279;
   public static final short X_RESOLUTION = 282;
   public static final short Y_RESOLUTION = 283;
   public static final short RESOLUTION_UNIT = 296;
   public static final short NEW_SUBFILE_TYPE = (short) 254;

   public void addBWImage(BufferedImage si) {
      final int w = si.getWidth();
      final int h = si.getHeight();
      mBWImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
      mBWImage.getGraphics().drawImage(si, 0, 0, null);
      addField(new Field(IMAGE_WIDTH, (short) w));
      addField(new Field(IMAGE_LENGTH, (short) h));
      addField(new Field(BITS_PER_SAMPLE, (short) 8));
      addField(new Field(COMPRESSION, (short) 1));
      addField(new Field(PHOTOMETRIC_INTERPRETATION, (short) 1));
      addField(new Field(X_RESOLUTION, ttRational, new int[] {
         300,
         1
      }));
      addField(new Field(Y_RESOLUTION, ttRational, new int[] {
         300,
         1
      }));
      addField(new Field(RESOLUTION_UNIT, (short) 2));
      final int stripLen = calcStripLen(w, w * h);
      final int nStrips = calcNStrips(w * h, stripLen);
      addField(new Field(STRIP_OFFSETS, ttLong, new int[nStrips]));
      addField(new Field(STRIP_BYTE_COUNTS, ttLong, new int[nStrips]));
      addField(new Field(ROWS_PER_STRIP, stripLen / w));
   }

   private int calcNStrips(final int size, final int blockSize) {
      return (size + (blockSize - 1)) / blockSize;
   }

   private int calcStripLen(final int w, final int size) {
      return Math.min(size, Math.max(w, w * (1024 / w)));
   }

   public int estimateSize() {
      int size = 8;
      for(final Field tag : mTags.values()) {
         final int so = tag.sizeOf();
         size += so < 4 ? 12 : 12 + so;
      }
      if(mBWImage != null)
         size += mBWImage.getWidth() * mBWImage.getHeight();
      return size;
   }
}