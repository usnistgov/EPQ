package gov.nist.microanalysis.EPQTools;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>
 * Very similar to DataOutputStream except it writes little-endian instead of
 * big-endian binary data. We can't extend DataOutputStream directly since it
 * has only final methods. This forces us implement LEDataOutputStream with a
 * DataOutputStream object, and use wrapper methods.
 * </p>
 * 
 * <pre>
 *       LEDataOutputStream.java
 *       
 *        copyright (c) 1998-2005 Roedy Green,
 *        Canadian Mind Products 
 *       #327 - 964 Heywood Avenue
 *       Victoria,  BC Canada V8V 2Y5
 *       hel: (250) 361-9093 
 *       mailto:roedyg@mindprod.com
 *       http://mindprod.com 
 *        
 *        Version 1.0 1998 January 6 
 *       
 *       1.1 1998 January 7 -officially implements DataInput 
 *       
 *       1.2 1998 January 9 - add LERandomAccessFile 
 *       
 *       1.3 1998 August 28 1.4 1998 November 10 - add new address and phone. 
 *       
 *       1.5 1999 October 8 - use com.mindprod.ledatastream
 *       package name.
 * </pre>
 */

public class LEDataOutputStream implements DataOutput {

   /** work array for composing output */
   byte w[];

   /** to get at big-Endian write methods of DataOutPutStream */
   protected DataOutputStream d;

   // L I T T L E E N D I A N W R I T E R S
   // Little endian methods for multi-byte numeric types.
   // Big-endian do fine for single-byte types and strings.

   /**
    * @throws IOException
    */
   public final void close() throws IOException {
      d.close();
   }

   // DataOutputStream

   /**
    * @throws IOException
    */
   public void flush() throws IOException {
      d.flush();
   }

   /**
    * @return bytes written so far in the stream. Note this is a int, not a long
    *         as you would exect. This because the underlying DataInputStream
    *         has a design flaw.
    */
   public final int size() {
      return d.size();
   }

   /**
    * @see java.io.DataOutput#write(byte[])
    */
   @Override
   public final void write(final byte b[]) throws IOException {
      d.write(b, 0, b.length);
   }

   /**
    * {@inheritDoc}
    * 
    * @see java.io.DataOutput#write(byte[], int, int)
    */
   @Override
   public final synchronized void write(final byte b[], final int off, final int len) throws IOException {
      d.write(b, off, len);
   }

   /**
    * This method writes only one byte, even though it says int {@inheritDoc}
    * 
    * @see java.io.DataOutput#write(int)
    */
   @Override
   public final synchronized void write(final int b) throws IOException {
      d.write(b);
   }

   /**
    * {@inheritDoc}
    * 
    * @see java.io.DataOutput#writeBoolean(boolean)
    */
   /* Only writes one byte */
   @Override
   public final void writeBoolean(final boolean v) throws IOException {
      d.writeBoolean(v);
   }

   // p u r e l y w r a p p e r m e t h o d s
   // We cannot inherit since DataOutputStream is final.

   /**
    * {@inheritDoc}
    * 
    * @see java.io.DataOutput#writeByte(int)
    */
   @Override
   public final void writeByte(final int v) throws IOException {
      d.writeByte(v);
   }

   /**
    * {@inheritDoc}
    * 
    * @see java.io.DataOutput#writeBytes(java.lang.String)
    */
   @Override
   public final void writeBytes(final String s) throws IOException {
      d.writeBytes(s);
   }

   /**
    * like DataOutputStream.writeChar. Note the parm is an int even though this
    * as a writeChar {@inheritDoc}
    * 
    * @param v
    */
   @Override
   public final void writeChar(final int v) throws IOException {
      // same code as writeShort
      w[0] = (byte) v;
      w[1] = (byte) (v >> 8);
      d.write(w, 0, 2);
   }

   /**
    * like DataOutputStream.writeChars, flip each char. {@inheritDoc}
    */
   @Override
   public final void writeChars(final String s) throws IOException {
      final int len = s.length();
      for (int i = 0; i < len; i++)
         writeChar(s.charAt(i));
   } // end writeChars

   /**
    * like DataOutputStream.writeDouble. {@inheritDoc}
    */
   @Override
   public final void writeDouble(final double v) throws IOException {
      writeLong(Double.doubleToLongBits(v));
   }

   /**
    * like DataOutputStream.writeFloat. {@inheritDoc}
    */
   @Override
   public final void writeFloat(final float v) throws IOException {
      writeInt(Float.floatToIntBits(v));
   }

   /**
    * like DataOutputStream.writeInt. {@inheritDoc}
    * 
    * @param v
    * @throws IOException
    */
   @Override
   public final void writeInt(final int v) throws IOException {
      w[0] = (byte) v;
      w[1] = (byte) (v >> 8);
      w[2] = (byte) (v >> 16);
      w[3] = (byte) (v >> 24);
      d.write(w, 0, 4);
   }

   /**
    * like DataOutputStream.writeLong. {@inheritDoc}
    * 
    * @param v
    * @throws IOException
    */
   @Override
   public final void writeLong(final long v) throws IOException {
      w[0] = (byte) v;
      w[1] = (byte) (v >> 8);
      w[2] = (byte) (v >> 16);
      w[3] = (byte) (v >> 24);
      w[4] = (byte) (v >> 32);
      w[5] = (byte) (v >> 40);
      w[6] = (byte) (v >> 48);
      w[7] = (byte) (v >> 56);
      d.write(w, 0, 8);
   }

   /**
    * like DataOutputStream.writeShort. also acts as a writeUnsignedShort
    * {@inheritDoc}
    * 
    * @param v
    *           the short you want written in little endian binary format
    * @throws IOException
    */
   @Override
   public final void writeShort(final int v) throws IOException {
      w[0] = (byte) v;
      w[1] = (byte) (v >> 8);
      d.write(w, 0, 2);
   }

   /**
    * {@inheritDoc}
    * 
    * @see java.io.DataOutput#writeUTF(java.lang.String)
    */
   @Override
   public final void writeUTF(final String str) throws IOException {
      d.writeUTF(str);
   }

   /**
    * constructor
    * 
    * @param out
    *           the outputstream we ware to write little endian binary data onto
    */
   public LEDataOutputStream(final OutputStream out) {
      this.d = new DataOutputStream(out);
      w = new byte[8]; // work array for composing output
   }

   /**
    * Embeds copyright notice
    * 
    * @return copyright notice
    */
   public static final String getCopyright() {
      return "LeDataStream 1.7 freeware copyright (c) 1998-2005 Roedy Green, Canadian Mind Products, http://mindprod.com roedyg@mindprod.com";
   }

} // end LEDataOutputStream
