package gov.nist.microanalysis.EPQTools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import javax.swing.JFrame;
import javax.swing.UIManager;

import gov.nist.microanalysis.EPQLibrary.CITZAF;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;

/**
 * <p>
 * An application for testing the CITZAF dialog.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Danny Davis
 * @version 1.0
 */

public class TestCITZAFDialog {
   private static CITZAFDialog Dialog;
   public static String FileLocation = "\\Documents and Settings\\DAVIS\\Desktop\\NISTZAF v1.3\\NISTZAF\\";

   public static void main(String[] args) {
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      }
      catch(final Exception ex) {
         ex.printStackTrace(System.err);
      }

      // readFile();
      final JFrame frame = new JFrame("The Main Frame");
      Dialog = new CITZAFDialog(frame, "CITZAF", false);

      final CITZAF citzaf = new CITZAF();
      citzaf.setSampleType(CITZAF.PARTICLE_OR_THIN_FILM);
      citzaf.setComposition(MaterialFactory.createMaterial(MaterialFactory.SiliconDioxide));
      Dialog.setCITZAF(citzaf);
      Dialog.addWindowClosingListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent e) {
            Dialog.setVisible(false);
            System.exit(0);
         }
      });

      Dialog.setVisible(true);

   }

   public static void readFile() {
      final int numElements = 2;
      final File file = new File("c:" + FileLocation + "dataout");
      try {

         try (final InputStream is = new FileInputStream(file)) {
            try (final Reader rd = new InputStreamReader(is, Charset.forName("US-ASCII"))) {
               try (final BufferedReader br = new BufferedReader(rd)) {

                  // Read Header
                  for(int index = 0; index < 5; index++)
                     br.readLine();

                  // Read MACs
                  br.mark(300);
                  while(!br.readLine().startsWith("Z-LINE"))
                     br.mark(300);
                  br.reset();
                  for(int index = 0; index < (2 + (numElements * numElements)); index++)
                     System.out.println(br.readLine());

                  System.out.println("------------------------------------------------");

                  // Read ZAF Corrections
                  br.mark(300);
                  while(!br.readLine().startsWith("ELEMENT"))
                     br.mark(300);
                  br.reset();

                  for(int index = 0; index < (2 + numElements); index++)
                     System.out.println(br.readLine());

                  System.out.println("------------------------------------------------");
                  // Read Sample Results
                  String line = br.readLine();
                  while((line != null) && !line.startsWith("SAMPLE"))
                     line = br.readLine();

                  br.readLine();
                  br.readLine();

                  for(int index = 0; index < (4 + numElements); index++) {
                     line = br.readLine();
                     System.out.println(line != null ? line : "NULL!!!");
                  }
                  br.readLine();
                  br.readLine();
                  System.out.println();
                  for(int index = 0; index < (2 + numElements); index++) {
                     line = br.readLine();
                     System.out.println(line != null ? line : "NULL!!!");
                  }
               }
            }
         }
      }
      catch(final IOException ioex) {
         System.out.println("Reached the end of file");
      }
   }

}
