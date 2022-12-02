package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.EPQLibrary.CITZAF;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * <p>
 * An application for simplifying the use of John Armstrong's CITZAF
 * quantitative correction program on Windows systems.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Daniel "Ooblioob" Davis
 * @version 1.0
 */
public class RunCitzafDialog {
   private static CITZAFDialog Dialog;
   private static CITZAFResults Results;
   private static Preferences userPref = Preferences.userRoot();
   private static JDialog waitFrame;

   public static void main(String[] args) {
      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (final Exception ex) {
         ex.printStackTrace(System.err);
      }

      Results = new CITZAFResults();
      Results.setTitle("CITZAF Results Page");
      final int X = userPref.getInt("CITZAF Results Page X", 600);
      final int Y = userPref.getInt("CITZAF Results Page Y", 300);
      Results.setSize(X, Y);
      Results.setLocation(getCenter(Results.getHeight(), Results.getWidth()));
      Results.addWindowClosingListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent e) {

            userPref.putInt("CITZAF Results Page X", Results.getWidth());
            userPref.putInt("CITZAF Results Page Y", Results.getHeight());
            if (!Results.hasSaved()) {
               final int choice = JOptionPane.showConfirmDialog(Results, "Would you like to save your output file before exiting?",
                     "Save before exit?", JOptionPane.YES_NO_OPTION);
               switch (choice) {
                  case JOptionPane.YES_OPTION :
                     Results.OpenSaveFileDialog();
                     break;
                  case JOptionPane.NO_OPTION :
                     System.exit(0);
                     break;
               }
            } else
               System.exit(0);
         }
      });
      Results.setVisible(true);
      Dialog = new CITZAFDialog(Results, "CITZAF", true);
      Dialog.setLocationRelativeTo(Results);
      Dialog.addWindowClosingListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent e) {
            Dialog.setVisible(false);
         }
      });
      Dialog.addBeginAnalysisButtonListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(java.awt.event.ActionEvent e) {
            final CITZAF citzaf = Dialog.getCITZAF();
            if (citzaf.isCITZAFSupported()) {
               if (((citzaf.getUsingKRatios()) && (citzaf.getKRatios().size() > 0))
                     || ((!citzaf.getUsingKRatios()) && (citzaf.getComposition().getElementCount() > 0))) {
                  Dialog.setVisible(false);
                  waitFrame = new JDialog(Dialog, "Please be patient, CITZAF loading...", false);
                  waitFrame.setLocation(Dialog.getX() + 50, Dialog.getY() + 50);
                  final JLabel message = new JLabel("Please wait, this computation may take a few seconds...");
                  message.setHorizontalTextPosition(SwingConstants.CENTER);
                  waitFrame.getContentPane().add(message);
                  waitFrame.setSize(300, 100);
                  waitFrame.setVisible(true);
                  Results.update(Results.getGraphics());
                  waitFrame.update(waitFrame.getGraphics());

                  String location = CITZAF.FileLocation;
                  if (location.matches("")) {
                     location = System.getProperty("user.dir");
                     CITZAF.FileLocation = location + "\\";
                     // Results.addText(location + "\n");
                  }

                  Results.addText("-----------------------------------------------------------\n");
                  Results.addText("INPUT:\n");
                  Results.addText(citzaf.SummarizeInputs());
                  Results.addText("\n");
                  Results.addText("OUTPUT:\n");

                  citzaf.dumpToFileAndRunCITZAF();
                  try {
                     Results.addText(citzaf.getFullResults());
                  } catch (final IOException ex) {
                     Results.addText("There was a problem reading the file");
                  }
                  waitFrame.setVisible(false);
               } else
                  JOptionPane.showMessageDialog(Dialog,
                        "There is no SAMPLE to analyze.  Please choose either a K-Ratio by clicking the \"New Sample\" button\n"
                              + "or by clicking the \"Calculate K-Ratios\" tab and clicking the \"New Material\" Button.",
                        "NO SAMPLE PRESENT", JOptionPane.ERROR_MESSAGE);
            } else
               JOptionPane.showMessageDialog(Dialog, "CITZAF is only supported on the Windows operating system", "CITZAF not supported",
                     JOptionPane.ERROR_MESSAGE);
         }
      });
      Dialog.setVisible(true);
   }

   private static Point getCenter(int height, int width) {
      // Center on screen
      final Point point = new Point();

      final Dimension screensz = Toolkit.getDefaultToolkit().getScreenSize();
      final int x = Math.max(0, (screensz.width - width) / 2);
      final int y = Math.max(0, (screensz.height - height) / 2);

      point.setLocation(x, y);
      return point;

   }
}
