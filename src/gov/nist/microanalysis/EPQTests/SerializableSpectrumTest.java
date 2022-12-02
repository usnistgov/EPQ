package gov.nist.microanalysis.EPQTests;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import gov.nist.microanalysis.EPQTools.EMSAFile;
import gov.nist.microanalysis.EPQTools.EPQXStream;
import gov.nist.microanalysis.EPQTools.SerializableSpectrum;

import junit.framework.TestCase;

/**
 * <p>
 * Tests the SerializableSpectrum class.
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
public class SerializableSpectrumTest extends TestCase {

   /**
    * Constructs a SerializableSpectrumTest
    * 
    * @param arg0
    */
   public SerializableSpectrumTest(String arg0) {
      super(arg0);
   }

   public void testOne() throws Exception {
      SerializableSpectrum original = null;
      {
         final InputStream is = new FileInputStream(new File(FilterFitTest.class.getResource("TestData/Al_ref1.msa").toURI()));
         final EMSAFile spec = new EMSAFile();
         spec.read(is);
         original = new SerializableSpectrum(spec);
      }
      // Write the spectrum to a ObjectOutputStream
      {
         final EPQXStream xs = EPQXStream.getInstance();
         try (final OutputStreamWriter osw = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream("serializableSpectrum.ser")),
               Charset.forName("UTF-8"))) {
            try (final ObjectOutputStream output = xs.createObjectOutputStream(osw)) {
               output.writeObject(original);
            }
         }
      }
      // Read the spectrum from a ObjectInputStream
      {
         final EPQXStream xs = EPQXStream.getInstance();
         final InputStreamReader isr = new InputStreamReader(new BufferedInputStream(new FileInputStream("serializableSpectrum.ser")));
         final ObjectInputStream input = xs.createObjectInputStream(isr);
         final SerializableSpectrum duplicate = (SerializableSpectrum) input.readObject();
         assertTrue(duplicate.equals(original));
         assertTrue(duplicate.getProperties().equals(original.getProperties()));
      }
   }
}
