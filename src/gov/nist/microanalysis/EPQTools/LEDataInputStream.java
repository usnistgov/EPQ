package gov.nist.microanalysis.EPQTools;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

/**
 * <p>
 * Very similar to DataInputStream except it reads little-endian instead of
 * big-endian binary data. We can't extend DataInputStream directly since it has
 * only final methods. This forces us implement LEDataInputStream with a
 * DataInputStream object, and use wrapper methods.
 * </p>
 * <p>
 * copyright (c) 1998-2002 Roedy Green, Canadian Mind Products
 * </p>
 * <p>
 * #327 - 964 Heywood Avenue Victoria, BC Canada V8V 2Y5
 * </p>
 * <p>
 * tel: (250) 361-9093
 * </p>
 * <p>
 * mailto:roedy@mindprod.com http://mindprod.com
 * </p>
 * <p>
 * Version 1.0 1998
 * </p>
 * <p>
 * January 6 1.1
 * </p>
 * <p>
 * 1998 January 7 - officially implements DataInput 1.2
 * </p>
 * <p>
 * 1998 January 9 - add LERandomAccessFile 1.3
 * </p>
 * <p>
 * 1998 August 27 - fix bug, readFully instead of read. 1.4
 * </p>
 * <p>
 * 1998 November 10 - add address and phone. 1.5
 * <p>
 * 1999 October 8 - use com.mindprod.ledatastream package name.
 * </p>
 */
public class LEDataInputStream implements DataInput, AutoCloseable {

   public static final String EmbeddedCopyright = "copyright (c) 1998-2002 Roedy Green, Canadian Mind Products, http://mindprod.com";

   /**
    * constructor
    */
   public LEDataInputStream(InputStream in) {
      this.in = in;
      this.d = new DataInputStream(in);
      w = new byte[8];
   }

   // L I T T L E E N D I A N R E A D E R S
   // Little endian methods for multi-byte numeric types.
   // Big-endian do fine for single-byte types and strings.
   /**
    * like DataInputStream.readShort except little endian.
    */
   @Override
   public final short readShort() throws IOException {
      d.readFully(w, 0, 2);
      return (short) (((w[1] & 0xff) << 8) | (w[0] & 0xff));
   }

   /**
    * like DataInputStream.readUnsignedShort except little endian. Note, returns
    * int even though it reads a short.
    */
   @Override
   public final int readUnsignedShort() throws IOException {
      d.readFully(w, 0, 2);
      return (((w[1] & 0xff) << 8) | (w[0] & 0xff));
   }

   /**
    * like DataInputStream.readChar except little endian.
    */
   @Override
   public final char readChar() throws IOException {
      d.readFully(w, 0, 2);
      return (char) (((w[1] & 0xff) << 8) | (w[0] & 0xff));
   }

   /**
    * like DataInputStream.readInt except little endian.
    */
   @Override
   public final int readInt() throws IOException {
      d.readFully(w, 0, 4);
      return ((w[3]) << 24) | ((w[2] & 0xff) << 16) | ((w[1] & 0xff) << 8) | (w[0] & 0xff);
   }

   /**
    * like DataInputStream.readLong except little endian.
    */
   @Override
   public final long readLong() throws IOException {
      d.readFully(w, 0, 8);
      return ((long) (w[7]) << 56)
            | (/*
                * long cast needed or shift done modulo 32
                */
            (long) (w[6] & 0xff) << 48) | ((long) (w[5] & 0xff) << 40) | ((long) (w[4] & 0xff) << 32) | ((long) (w[3] & 0xff) << 24)
            | ((long) (w[2] & 0xff) << 16) | ((long) (w[1] & 0xff) << 8) | (w[0] & 0xff);
   }

   /**
    * like DataInputStream.readFloat except little endian.
    */
   @Override
   public final float readFloat() throws IOException {
      return Float.intBitsToFloat(readInt());
   }

   /**
    * like DataInputStream.readDouble except little endian.
    */
   @Override
   public final double readDouble() throws IOException {
      return Double.longBitsToDouble(readLong());
   }

   // p u r e l y w r a p p e r m e t h o d s
   // We can't simply inherit since dataInputStream is final.

   /* Watch out, may return fewer bytes than requested. */
   public final int read(byte b[], int off, int len) throws IOException {
      // For efficiency, we avoid one layer of wrapper
      return in.read(b, off, len);
   }

   @Override
   public final void readFully(byte b[]) throws IOException {
      d.readFully(b, 0, b.length);
   }

   @Override
   public final void readFully(byte b[], int off, int len) throws IOException {
      d.readFully(b, off, len);
   }

   @Override
   public final int skipBytes(int n) throws IOException {
      return d.skipBytes(n);
   }

   /* only reads one byte */
   @Override
   public final boolean readBoolean() throws IOException {
      return d.readBoolean();
   }

   @Override
   public final byte readByte() throws IOException {
      return d.readByte();
   }

   // note: returns an int, even though says Byte.
   @Override
   public final int readUnsignedByte() throws IOException {
      return d.readUnsignedByte();
   }

   /**
    * @see java.io.DataInput#readLine()
    * @deprecated
    */
   @Override
   @Deprecated
   public final String readLine() throws IOException {
      return d.readLine();
   }

   @Override
   public final String readUTF() throws IOException {
      return d.readUTF();
   }

   // Note. This is a STATIC method!
   public final static String readUTF(DataInput in) throws IOException {
      return DataInputStream.readUTF(in);
   }

   @Override
   public final void close() throws IOException {
      d.close();
   }

   public final void reset() throws IOException {
      d.reset();
      w = new byte[8];
   }

   public FileChannel getChannel() {
      return in instanceof FileInputStream ? ((FileInputStream) in).getChannel() : null;
   }

   public FileDescriptor getFD() throws IOException {
      return in instanceof FileInputStream ? ((FileInputStream) in).getFD() : null;
   }

   // i n s t a n c e v a r i a b l e s

   private final DataInputStream d;
   // to get at high level readFully methods of DataInputStream
   private final InputStream in;
   // to get at the low-level read methods of InputStream
   private byte w[]; // work array for buffering input

} // end class LEDataInputStream
