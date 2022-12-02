package gov.nist.microanalysis.EPQTools;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A class that implements DataInput and DataOutput for binary files in either
 * little-endian or big-endian format.
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
public class EndianRandomAccessFile implements DataInput, DataOutput {

   /**
    * LittleEndian - Specifies that Little Endian ordering should be used.
    */
   public static final String LittleEndian = "Little endian";
   /**
    * BigEndian - Specifies that Big Endian ordering should be used.
    */
   public static final String BigEndian = "Big endian";

   private boolean mLittleEndian = true;
   private final RandomAccessFile mRaf;
   private final byte mBuffer[]; // work array for buffering input/output

   /**
    * EndianRandomAccessFile - Similar to the RandomAccessFile constructors
    * except for the final argument.
    * 
    * @param f
    *           String
    * @param rw
    *           String As in RandomAccessFile mode "r", "rw", "rws", "rwd"
    * @param endian
    *           String - One of EndianRandomAccessFile.LittleEndian or
    *           EndianRandomAccessFile.BigEndian
    * @throws IOException
    */
   public EndianRandomAccessFile(String f, String rw, String endian) throws IOException {
      mRaf = new RandomAccessFile(f, rw);
      mBuffer = new byte[8];
      setEndian(endian);
   }

   public void setEndian(String endian) {
      mLittleEndian = endian.equals(LittleEndian);
   }

   /**
    * EndianRandomAccessFile - Similar to the RandomAccessFile constructors
    * except for the final argument.
    * 
    * @param f
    *           File
    * @param rw
    *           String As in RandomAccessFile mode "r", "rw", "rws", "rwd"
    * @param endian
    *           String - One of EndianRandomAccessFile.LittleEndian or
    *           EndianRandomAccessFile.BigEndian
    * @throws IOException
    */
   public EndianRandomAccessFile(File f, String rw, String endian) throws IOException {
      mRaf = new RandomAccessFile(f, rw);
      mBuffer = new byte[8];
      setEndian(endian);
   }

   /**
    * like RandomAcessFile.readShort except endianess depends upon constructor.
    */
   @Override
   public final short readShort() throws IOException {
      if (mLittleEndian) {
         mRaf.readFully(mBuffer, 0, 2);
         return (short) (((mBuffer[1] & 0xff) << 8) | (mBuffer[0] & 0xff));
      } else
         return mRaf.readShort();
   }

   /**
    * like RandomAcessFile.readUnsignedShort except endianess depends upon
    * constructor Note, returns int even though it reads a short.
    */
   @Override
   public final int readUnsignedShort() throws IOException {
      if (mLittleEndian) {
         mRaf.readFully(mBuffer, 0, 2);
         return (((mBuffer[1] & 0xff) << 8) | (mBuffer[0] & 0xff));
      } else
         return mRaf.readUnsignedShort();
   }

   /**
    * like RandomAcessFile.readUnsignedShort except endianess depends upon
    * constructor Note, returns long even though it reads an int.
    * 
    * @return long
    * @throws IOException
    */
   public final long readUnsignedInt() throws IOException {
      mRaf.readFully(mBuffer, 0, 4);
      if (mLittleEndian)
         return (((mBuffer[3] & 0xff) << 24) | ((mBuffer[2] & 0xff) << 16) | ((mBuffer[1] & 0xff) << 8) | (mBuffer[0] & 0xff));
      else
         return (((mBuffer[0] & 0xff) << 24) | ((mBuffer[1] & 0xff) << 16) | ((mBuffer[2] & 0xff) << 8) | (mBuffer[3] & 0xff));
   }

   /**
    * like RandomAcessFile.readChar except endianess depends upon constructor
    * 
    * @return char
    * @throws IOException
    * @see java.io.DataInput#readChar()
    */
   @Override
   public final char readChar() throws IOException {
      if (mLittleEndian) {
         mRaf.readFully(mBuffer, 0, 2);
         return (char) (((mBuffer[1] & 0xff) << 8) | (mBuffer[0] & 0xff));
      } else
         return mRaf.readChar();
   }

   /**
    * like RandomAcessFile.readInt except endianess depends upon constructor
    * 
    * @return int
    * @throws IOException
    * @see java.io.DataInput#readInt()
    */
   @Override
   public final int readInt() throws IOException {
      if (mLittleEndian) {
         mRaf.readFully(mBuffer, 0, 4);
         return ((mBuffer[3]) << 24) | ((mBuffer[2] & 0xff) << 16) | ((mBuffer[1] & 0xff) << 8) | (mBuffer[0] & 0xff);
      } else
         return mRaf.readInt();
   }

   /**
    * like RandomAcessFile.readLong except endianess depends upon constructor
    * 
    * @return long
    * @throws IOException
    * @see java.io.DataInput#readLong()
    */
   @Override
   public final long readLong() throws IOException {
      if (mLittleEndian) {
         mRaf.readFully(mBuffer, 0, 8);
         return ((long) (mBuffer[7]) << 56)
               | (/*
                   * long cast necessary or shift done modulo 32
                   */
               (long) (mBuffer[6] & 0xff) << 48) | ((long) (mBuffer[5] & 0xff) << 40) | ((long) (mBuffer[4] & 0xff) << 32)
               | ((long) (mBuffer[3] & 0xff) << 24) | ((long) (mBuffer[2] & 0xff) << 16) | ((long) (mBuffer[1] & 0xff) << 8) | (mBuffer[0] & 0xff);
      } else
         return mRaf.readLong();
   }

   /**
    * like RandomAcessFile.readFloat except endianess depends upon constructor
    * 
    * @return float
    * @throws IOException
    * @see java.io.DataInput#readFloat()
    */
   @Override
   public final float readFloat() throws IOException {
      if (mLittleEndian)
         return Float.intBitsToFloat(readInt());
      else
         return mRaf.readFloat();
   }

   /**
    * like RandomAcessFile.readDouble except endianess depends upon constructor
    * 
    * @return double
    * @throws IOException
    * @see java.io.DataInput#readDouble()
    */
   @Override
   public final double readDouble() throws IOException {
      if (mLittleEndian)
         return Double.longBitsToDouble(readLong());
      else
         return mRaf.readDouble();
   }

   /**
    * like RandomAcessFile.writeShort. also acts as a writeUnsignedShort
    * 
    * @param v
    * @throws IOException
    * @see java.io.DataOutput#writeShort(int)
    */
   @Override
   public final void writeShort(int v) throws IOException {
      if (mLittleEndian) {
         mBuffer[0] = (byte) v;
         mBuffer[1] = (byte) (v >> 8);
         mRaf.write(mBuffer, 0, 2);
      } else
         mRaf.writeShort(v);
   }

   /**
    * like RandomAcessFile.writeChar. Note the parm is an int even though this
    * as a writeChar
    * 
    * @param v
    * @throws IOException
    * @see java.io.DataOutput#writeChar(int)
    */
   @Override
   public final void writeChar(int v) throws IOException {
      if (mLittleEndian) {
         // same code as writeShort
         mBuffer[0] = (byte) v;
         mBuffer[1] = (byte) (v >> 8);
         mRaf.write(mBuffer, 0, 2);
      } else
         mRaf.writeChar(v);
   }

   /**
    * like RandomAcessFile.writeInt.
    * 
    * @param v
    * @throws IOException
    * @see java.io.DataOutput#writeInt(int)
    */
   @Override
   public final void writeInt(int v) throws IOException {
      if (mLittleEndian) {
         mBuffer[0] = (byte) v;
         mBuffer[1] = (byte) (v >> 8);
         mBuffer[2] = (byte) (v >> 16);
         mBuffer[3] = (byte) (v >> 24);
         mRaf.write(mBuffer, 0, 4);
      } else
         mRaf.writeInt(v);
   }

   /**
    * like RandomAcessFile.writeLong.
    * 
    * @param v
    * @throws IOException
    * @see java.io.DataOutput#writeLong(long)
    */
   @Override
   public final void writeLong(long v) throws IOException {
      if (mLittleEndian) {
         mBuffer[0] = (byte) v;
         mBuffer[1] = (byte) (v >> 8);
         mBuffer[2] = (byte) (v >> 16);
         mBuffer[3] = (byte) (v >> 24);
         mBuffer[4] = (byte) (v >> 32);
         mBuffer[5] = (byte) (v >> 40);
         mBuffer[6] = (byte) (v >> 48);
         mBuffer[7] = (byte) (v >> 56);
         mRaf.write(mBuffer, 0, 8);
      } else
         mRaf.writeLong(v);
   }

   /**
    * like RandomAcessFile.writeFloat.
    * 
    * @param v
    * @throws IOException
    * @see java.io.DataOutput#writeFloat(float)
    */
   @Override
   public final void writeFloat(float v) throws IOException {
      if (mLittleEndian)
         writeInt(Float.floatToIntBits(v));
      else
         mRaf.writeFloat(v);
   }

   /**
    * like RandomAcessFile.writeDouble.
    * 
    * @param v
    * @throws IOException
    * @see java.io.DataOutput#writeDouble(double)
    */
   @Override
   public final void writeDouble(double v) throws IOException {
      if (mLittleEndian)
         writeLong(Double.doubleToLongBits(v));
      else
         mRaf.writeDouble(v);
   }

   /**
    * like RandomAcessFile.writeChars.
    * 
    * @param s
    * @throws IOException
    * @see java.io.DataOutput#writeChars(java.lang.String)
    */
   @Override
   public final void writeChars(String s) throws IOException {
      if (mLittleEndian) {
         final int len = s.length();
         for (int i = 0; i < len; i++)
            writeChar(s.charAt(i));
      } else
         mRaf.writeChars(s);
   } // end writeChars

   /**
    * like RandomAcessFile.getFD().
    * 
    * @return FileDescriptor
    * @throws IOException
    */
   public final FileDescriptor getFD() throws IOException {
      return mRaf.getFD();
   }

   /**
    * like RandomAcessFile.getFilePointer()
    * 
    * @return
    * @throws IOException
    */
   public final long getFilePointer() throws IOException {
      return mRaf.getFilePointer();
   }

   /**
    * like RandomAcessFile.length()
    * 
    * @return long
    * @throws IOException
    */
   public final long length() throws IOException {
      return mRaf.length();
   }

   /**
    * like RandomAcessFile.read(...)
    * 
    * @param b
    * @param off
    * @param len
    * @return int
    * @throws IOException
    */
   public final int read(byte b[], int off, int len) throws IOException {
      return mRaf.read(b, off, len);
   }

   /**
    * like RandomAcessFile.read
    * 
    * @param b
    * @return int
    * @throws IOException
    */
   public final int read(byte b[]) throws IOException {
      return mRaf.read(b);
   }

   /**
    * like RandomAcessFile.read
    * 
    * @return int
    * @throws IOException
    */
   public final int read() throws IOException {
      return mRaf.read();
   }

   /**
    * like RandomAcessFile.readFully
    * 
    * @param b
    * @throws IOException
    * @see java.io.DataInput#readFully(byte[])
    */
   @Override
   public final void readFully(byte b[]) throws IOException {
      mRaf.readFully(b, 0, b.length);
   }

   /**
    * like RandomAcessFile.readFully
    * 
    * @param b
    * @param off
    * @param len
    * @throws IOException
    * @see java.io.DataInput#readFully(byte[], int, int)
    */
   @Override
   public final void readFully(byte b[], int off, int len) throws IOException {
      mRaf.readFully(b, off, len);
   }

   /**
    * like RandomAcessFile.skipBytes
    * 
    * @param n
    * @return int
    * @throws IOException
    * @see java.io.DataInput#skipBytes(int)
    */
   @Override
   public final int skipBytes(int n) throws IOException {
      return mRaf.skipBytes(n);
   }

   /**
    * like RandomAcessFile.readBoolean
    * 
    * @return boolean
    * @throws IOException
    * @see java.io.DataInput#readBoolean()
    */
   @Override
   public final boolean readBoolean() throws IOException {
      return mRaf.readBoolean();
   }

   /**
    * like RandomAcessFile.readByte
    * 
    * @return byte
    * @throws IOException
    * @see java.io.DataInput#readByte()
    */
   @Override
   public final byte readByte() throws IOException {
      return mRaf.readByte();
   }

   /**
    * like RandomAcessFile.readUnsignedByte
    * 
    * @return int
    * @throws IOException
    * @see java.io.DataInput#readUnsignedByte()
    */
   @Override
   public final int readUnsignedByte() throws IOException {
      return mRaf.readUnsignedByte();
   }

   /**
    * like RandomAcessFile.readLine
    * 
    * @return String
    * @throws IOException
    * @see java.io.DataInput#readLine()
    */
   @Override
   public final String readLine() throws IOException {
      return mRaf.readLine();
   }

   /**
    * like RandomAcessFile.readUTF
    * 
    * @return String
    * @throws IOException
    * @see java.io.DataInput#readUTF()
    */
   @Override
   public final String readUTF() throws IOException {
      return mRaf.readUTF();
   }

   /**
    * like RandomAcessFile.seek
    * 
    * @param pos
    * @throws IOException
    */
   public final void seek(long pos) throws IOException {
      mRaf.seek(pos);
   }

   /**
    * like RandomAcessFile.write
    * 
    * @param b
    * @throws IOException
    * @see java.io.DataOutput#write(int)
    */
   @Override
   public final synchronized void write(int b) throws IOException {
      mRaf.write(b);
   }

   /**
    * like RandomAcessFile.write
    * 
    * @param b
    * @param off
    * @param len
    * @throws IOException
    * @see java.io.DataOutput#write(byte[], int, int)
    */
   @Override
   public final synchronized void write(byte b[], int off, int len) throws IOException {
      mRaf.write(b, off, len);
   }

   /**
    * like RandomAcessFile.writeBoolean
    * 
    * @param v
    * @throws IOException
    * @see java.io.DataOutput#writeBoolean(boolean)
    */
   @Override
   public final void writeBoolean(boolean v) throws IOException {
      mRaf.writeBoolean(v);
   }

   /**
    * like RandomAcessFile.writeByte
    * 
    * @param v
    * @throws IOException
    * @see java.io.DataOutput#writeByte(int)
    */
   @Override
   public final void writeByte(int v) throws IOException {
      mRaf.writeByte(v);
   }

   /**
    * like RandomAcessFile.writeBytes
    * 
    * @param s
    * @throws IOException
    * @see java.io.DataOutput#writeBytes(java.lang.String)
    */
   @Override
   public final void writeBytes(String s) throws IOException {
      mRaf.writeBytes(s);
   }

   /**
    * like RandomAcessFile.writeUTF
    * 
    * @param str
    * @throws IOException
    * @see java.io.DataOutput#writeUTF(java.lang.String)
    */
   @Override
   public final void writeUTF(String str) throws IOException {
      mRaf.writeUTF(str);
   }

   /**
    * like RandomAcessFile.write
    * 
    * @param b
    * @throws IOException
    * @see java.io.DataOutput#write(byte[])
    */
   @Override
   public final void write(byte b[]) throws IOException {
      mRaf.write(b, 0, b.length);
   }

   /**
    * like RandomAcessFile.close
    * 
    * @throws IOException
    */
   public final void close() throws IOException {
      mRaf.close();
   }
}
