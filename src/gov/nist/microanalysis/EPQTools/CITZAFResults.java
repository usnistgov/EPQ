package gov.nist.microanalysis.EPQTools;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * <p>
 * A class for representing the results of a CITZAF analysis.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */

public class CITZAFResults extends JFrame {
   static final long serialVersionUID = 0x1;
   BorderLayout borderLayout1 = new BorderLayout();
   JMenuBar jMenuBar_Main = new JMenuBar();
   JMenu jMenu_File = new JMenu();
   JMenuItem jMenuItem_SaveAs = new JMenuItem();
   JMenuItem jMenuItem_Exit = new JMenuItem();
   JMenu jMenu_Analysis = new JMenu();
   JMenuItem jMenuItem_RunAnother = new JMenuItem();
   JScrollPane jScrollPane_Main = new JScrollPane();
   JTextArea jTextArea_Main = new JTextArea();

   CITZAFAbout aboutDialog;

   private transient Vector<ActionListener> WindowClosingListeners = new Vector<ActionListener>();
   private boolean hasSaved = true;
   private static Preferences userPref = Preferences.userRoot();
   JMenu jMenu_About = new JMenu();
   JMenuItem jMenuItem_About = new JMenuItem();

   public CITZAFResults() {
      try {
         jbInit();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   void jbInit() throws Exception {
      this.setJMenuBar(jMenuBar_Main);
      this.addWindowListener(new java.awt.event.WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            this_windowClosing(e);
         }
      });
      this.getContentPane().setLayout(borderLayout1);
      jMenu_File.setText("File");
      jMenuItem_SaveAs.setText("Save As...");
      jMenuItem_SaveAs.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jMenuItem_SaveAs_actionPerformed(e);
         }
      });
      jMenuItem_Exit.setText("Exit");
      jMenuItem_Exit.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jMenuItem_Exit_actionPerformed(e);
         }
      });
      jMenu_Analysis.setText("Analysis");
      jMenuItem_RunAnother.setText("Run Another Analysis");
      jMenuItem_RunAnother.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jMenuItem_RunAnother_actionPerformed(e);
         }
      });
      jTextArea_Main.setText("");
      jMenu_About.setText("About");
      jMenuItem_About.setText("About CITZAF");
      jMenuItem_About.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jMenuItem_About_actionPerformed(e);
         }
      });
      jMenuBar_Main.add(jMenu_File);
      jMenuBar_Main.add(jMenu_Analysis);
      jMenuBar_Main.add(jMenu_About);
      jMenu_File.add(jMenuItem_SaveAs);
      jMenu_File.addSeparator();
      jMenu_File.add(jMenuItem_Exit);
      jMenu_Analysis.add(jMenuItem_RunAnother);
      this.getContentPane().add(jScrollPane_Main, BorderLayout.CENTER);
      jScrollPane_Main.getViewport().add(jTextArea_Main, null);
      jMenu_About.add(jMenuItem_About);
      jTextArea_Main.setLineWrap(true);
      jTextArea_Main.setWrapStyleWord(true);
      jTextArea_Main.getDocument().addDocumentListener(new DocumentListener() {
         @Override
         public void changedUpdate(DocumentEvent e) {
            hasSaved = false;
         }

         @Override
         public void insertUpdate(DocumentEvent e) {
            hasSaved = false;
         }

         @Override
         public void removeUpdate(DocumentEvent e) {
            hasSaved = false;
         }
      });
   }

   public void addText(String text) {
      jTextArea_Main.append(text);
      hasSaved = false;
   }

   /**
    * addWindowClosingListener - Adds a window closing listener. When the dialog
    * closes, an event will be triggered so that all listening components can
    * respond appropriately.
    * 
    * @param l
    *           ActionListener
    */
   public synchronized void addWindowClosingListener(ActionListener l) {
      final Vector<ActionListener> v = WindowClosingListeners == null
            ? new Vector<ActionListener>(2)
            : new Vector<ActionListener>(WindowClosingListeners);
      if (!v.contains(l)) {
         v.addElement(l);
         WindowClosingListeners = v;
      }
   }

   /**
    * removeWindowClosingListener - removes the window closing listener
    * 
    * @param l
    *           ActionListener
    */
   public synchronized void removeWindowClosingListener(ActionListener l) {
      if ((WindowClosingListeners != null) && WindowClosingListeners.contains(l)) {
         final Vector<ActionListener> v = new Vector<ActionListener>(WindowClosingListeners);
         v.removeElement(l);
         WindowClosingListeners = v;
      }
   }

   protected void fireWindowClosingEvent(ActionEvent e) {
      if (WindowClosingListeners != null)
         for (final ActionListener al : WindowClosingListeners)
            al.actionPerformed(e);
   }

   public void OpenSaveFileDialog() {
      final JFileChooser fc = new JFileChooser() {
         private static final long serialVersionUID = -8441838196501709700L;

         @Override
         public void updateUI() {
            putClientProperty("FileChooser.useShellFolder", Boolean.FALSE);
            super.updateUI();
         }
      };

      System.out.println(jTextArea_Main.getText());
      final String PathName = userPref.get("CITZAF Saved Results File", "");
      if (!PathName.matches(""))
         fc.setCurrentDirectory(new File(PathName));
      if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
         final File saveFile = fc.getSelectedFile();
         try {
            try (final OutputStream os = new FileOutputStream(saveFile)) {
               try (final Writer wr = new OutputStreamWriter(os, Charset.forName("US-ASCII"))) {
                  try (final PrintWriter pw = new PrintWriter(wr)) {
                     pw.write(jTextArea_Main.getText());
                  }
               }
            }
         } catch (final IOException ioex) {
            JOptionPane.showMessageDialog(this, "Error writing file: " + ioex.getMessage(), "Error writing the file", JOptionPane.ERROR_MESSAGE);
         }
         hasSaved = true;
         userPref.put("CITZAF Saved Results File", saveFile.getAbsolutePath());
      }

   }

   void jMenuItem_SaveAs_actionPerformed(ActionEvent e) {
      OpenSaveFileDialog();
   }

   void jMenuItem_Exit_actionPerformed(ActionEvent e) {
      fireWindowClosingEvent(e);
   }

   void jMenuItem_RunAnother_actionPerformed(ActionEvent e) {
      final Window[] components = this.getOwnedWindows();
      int index = 0;
      while ((index < components.length) && (!(components[index] instanceof JDialog)))
         index++;
      if (index != components.length) {
         final JDialog dialog = (JDialog) components[index];
         if (!dialog.isVisible())
            dialog.setVisible(true);
         dialog.requestFocus();
      }
   }

   public boolean hasSaved() {
      return hasSaved;
   }

   void jButton_Cancel_actionPerformed(ActionEvent e) {
      fireWindowClosingEvent(e);
   }

   void this_windowClosing(WindowEvent e) {
      fireWindowClosingEvent(new ActionEvent(e.getSource(), e.getID(), "Window closing"));
   }

   void jMenuItem_About_actionPerformed(ActionEvent e) {
      aboutDialog = new CITZAFAbout(CITZAFResults.this, "About CITZAF", true);
      aboutDialog.setLocationRelativeTo(CITZAFResults.this);
      aboutDialog.setVisible(true);
   }
}
