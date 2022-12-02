package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>
 * A pure static class that writes a ISpectrumData object to a binary stream.
 * Only the channel data is written. Each channel is written as 4-byte value so
 * the total length of the output is spec.getChannelCount()*4. The byte ordering
 * is the big-endian.
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
 */
public class WriteSpectrumAsBinary {

   /**
    * write - Write the argument ISpectrumData object to the specified
    * OutputStream.
    * 
    * @param spec
    *           ISpectrumData
    * @param os
    *           OutputStream
    * @throws EPQException
    */
   public static void write(ISpectrumData spec, OutputStream os) throws EPQException {
      final DataOutputStream dos = new DataOutputStream(os);
      try {
         final int nCh = spec.getChannelCount();
         for (int i = 0; i < nCh; ++i)
            dos.writeInt((int) Math.round(spec.getCounts(i)));
         dos.flush();
      } catch (final IOException ex) {
         System.err.println("Error in WriteSpectrumAsBinary: " + ex.toString());
      }
   }

   /**
    * size - Returns the number of bytes that would be output to the
    * OutputStream by WriteSpectrumAsBinary.write.
    * 
    * @param spec
    *           ISpectrumData
    * @return int
    */
   public static int size(ISpectrumData spec) {
      return itemSize() * length(spec);
   }

   /**
    * length - Returns the number of atomic data items that will be written to
    * the stream. The size of each atomic data item is returned by itemSize().
    * 
    * @param spec
    *           ISpectrumData
    * @return int
    */
   public static int length(ISpectrumData spec) {
      return spec.getChannelCount();
   }

   /**
    * itemSize - The size of a single item in bytes within the binary object.
    * (Equal to 4)
    * 
    * @return int
    */
   public static int itemSize() {
      return 4;
   }

}
