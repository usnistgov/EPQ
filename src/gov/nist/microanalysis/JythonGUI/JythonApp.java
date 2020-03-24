package gov.nist.microanalysis.JythonGUI;

import java.io.File;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.UIManager;

import gov.nist.microanalysis.JythonGUI.JythonApp;

/**
 * <p>
 * Title: Jython GUI
 * </p>
 * <p>
 * Description: A simple GUI for running Jython scripts
 * </p>
 * <p>
 * Not copyright: In the public domain * 
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class JythonApp {
   boolean packFrame = false;
   JythonFrame mFrame;
   String[] mFiles;

   // Construct the application
   public JythonApp() {
      mFrame = new JythonFrame();
      // Validate frames that have preset sizes
      // Pack frames that have useful preferred size info, e.g. from their
      // layout
      if(packFrame) {
         mFrame.pack();
      } else {
         mFrame.validate();
      }
      // Center the window
      mFrame.setVisible(true);
   }

   protected void executeFile(File file) {
      try {
         mFrame.executeFile(file);
      }
      catch(Exception ex) {

      }
   }
   
   /*
    * Load stuff forces these libraries to be loaded before the Jython GUI gets
    * started thus making them available in Python code. Wish it wasn't necessary...
    */
   private static void loadStuff() {
	   gov.nist.microanalysis.EPQDatabase.Session.loadMe();
	   gov.nist.microanalysis.EPQImage.Blobber.loadMe();
	   gov.nist.microanalysis.EPQLibrary.Element.parseElementString("Li");
	   gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector.createSDDDetector(10, 5.0, 128.0);
	   gov.nist.microanalysis.EPQTools.SwingUtils.createDefaultBorder();
	   gov.nist.microanalysis.NISTMonte.Electron.getlastIdent();
	   gov.nist.microanalysis.NISTMonte.Gen3.XRayTransport3.getDefaultEdgeEnergy();
	   gov.nist.microanalysis.Utility.Math2.bound(1, 10, 20);
	   gov.nist.nanoscalemetrology.JMONSEL.TimeKeeper.getTimeKeeper();
	   gov.nist.nanoscalemetrology.JMONSELutils.BiCubicSpline.zero(3, 3);
	   gov.nist.nanoscalemetrology.MONSELtests.FunctionTiming.timetest(1);
   }

   // Main method
   public static void main(String[] args) {
      try {
         // Set these regardless (no harm in it)
         System.setProperty("apple.laf.useScreenMenuBar", "true");
         System.setProperty("apple.laf.smallTabs", "true");
         System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Jython");

         String os = System.getProperty("os.name").toLowerCase();
         String laf = UIManager.getSystemLookAndFeelClassName();
         if(os.equals("windows")) {
            String name = System.getProperty("user.name");
            if(name.equals("Nicholas") || name.equals("nritchie")) {
               // Switch look-and-feels to expose bugs related to assumptions
               // about L&F 
               String[] lafs = {
                  "net.java.plaf.windows.WindowsLookAndFeel",
                  "com.jgoodies.looks.plastic.PlasticLookAndFeel",
                  "com.jgoodies.looks.plastic.PlasticXPLookAndFeel",
                  "com.jgoodies.looks.plastic.Plastic3DLookAndFeel",
               };
               int idx = (new Random()).nextInt(lafs.length);
               laf = lafs[idx];
            } else
               laf = "net.java.plaf.windows.WindowsLookAndFeel";
         } else if(os.equals("linux")) {
            laf = "com.jgoodies.looks.plastic.Plastic3DLookAndFeel";
         }
         UIManager.setLookAndFeel(laf);
      }
      catch(Exception e) {
         try {
            e.printStackTrace();
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         }
         catch(Exception e1) {
            e1.printStackTrace();
         }
      }
      JFrame.setDefaultLookAndFeelDecorated(false);
      loadStuff();
      JythonApp ja = new JythonApp();
      for(int i = 0; i < args.length; ++i)
         ja.executeFile(new File(args[i]));
   }
}
