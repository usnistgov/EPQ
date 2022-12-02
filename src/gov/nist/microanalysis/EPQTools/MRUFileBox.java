package gov.nist.microanalysis.EPQTools;

import java.awt.Color;
import java.awt.Component;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * <p>
 * A combo box that keeps track of the most recently accessed files. These files
 * are kept in a preference node and are accessed by a string declared along
 * with the file.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Daniel "Ooblioob" Davis
 * @version 1.0
 */
public class MRUFileBox extends JPanel {
   private static final long serialVersionUID = 0x100;

   private final JComboBox<String> mFileDirectory = new JComboBox<String>();
   private final JButton mBrowse = new JButton("Browse");

   private String mDefaultDirectory = "";
   private final ArrayList<String> mVectorList = new ArrayList<String>();

   private final String mMRUFiles;
   private final String mDefaultDir;
   private JFileChooser mFileChooser;
   private final transient Vector<ActionListener> mListeners = new Vector<ActionListener>();

   private static final int MAX_SIZE = 10;

   public MRUFileBox(String defaultDirectoryStr, String MRUFileStr) {
      super(new FormLayout("min(p;170dlu):grow(1.0), 5dlu, 30dlu", "pref"));
      mDefaultDir = defaultDirectoryStr;
      mMRUFiles = MRUFileStr;

      final CellConstraints cc = new CellConstraints();
      add(mFileDirectory, cc.xy(1, 1));
      add(mBrowse, cc.xy(3, 1));
      mFileDirectory.setEditable(true);
      mFileDirectory.requestFocusInWindow();
      mFileDirectory.setBackground(SystemColor.window);
      mFileDirectory.getEditor().getEditorComponent().setBackground(SystemColor.window);

      final Preferences pref = Preferences.userNodeForPackage(MRUFileBox.class);
      mDefaultDirectory = pref.get(mDefaultDir, System.getProperty("user.home"));
      if (!(new File(mDefaultDirectory).exists())) {
         mDefaultDirectory = System.getProperty("user.home");
         pref.put(mDefaultDir, mDefaultDirectory);
      }
      final String temp = pref.get(mMRUFiles, "");
      final StringTokenizer tok = new StringTokenizer(temp, ",");
      mFileDirectory.removeAllItems();
      mVectorList.clear();
      final StringBuffer newList = new StringBuffer();
      while (tok.hasMoreTokens()) {
         final String temp2 = tok.nextToken().trim();
         final File file = new File(temp2);
         if (file.exists()) {
            // Ensures that only the most recent items will be list
            if (mVectorList.size() == MAX_SIZE) {
               final Object item = mVectorList.remove(0);
               mFileDirectory.removeItem(item);
            }
            mVectorList.add(temp2);
            mFileDirectory.addItem(temp2);
            newList.append(temp2);
            newList.append(",");
         }
      }
      // Write back those that really exist...
      pref.put(mMRUFiles, newList.toString());
      if (mVectorList.size() > 0)
         if (mVectorList.contains(mDefaultDirectory)) {
            mFileDirectory.setSelectedItem(mDefaultDirectory);
            mFileDirectory.setToolTipText(mDefaultDirectory);
         } else {
            mFileDirectory.setSelectedItem(mVectorList.get(0));
            mFileDirectory.setToolTipText(mVectorList.get(0));
         }

      mFileDirectory.addActionListener(new AbstractAction() {
         private static final long serialVersionUID = 0x101;

         @Override
         public void actionPerformed(ActionEvent e) {
            mFileDirectory.setToolTipText((String) mFileDirectory.getSelectedItem());
            for (int index = 0; index < mListeners.size(); index++) {
               final ActionListener list = mListeners.get(index);
               list.actionPerformed(e);
            }
         }
      });

      mBrowse.addActionListener(new AbstractAction() {
         private static final long serialVersionUID = 0x102;

         @Override
         public void actionPerformed(ActionEvent e) {
            if (mFileChooser == null)
               mFileChooser = new JFileChooser() {
                  private static final long serialVersionUID = -8441838196501709700L;

                  @Override
                  public void updateUI() {
                     putClientProperty("FileChooser.useShellFolder", Boolean.FALSE);
                     super.updateUI();
                  }
               };

            mFileChooser.setCurrentDirectory(new File(mDefaultDirectory));
            mFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            mFileChooser.setMultiSelectionEnabled(false);
            final int result = mFileChooser.showOpenDialog((Component) e.getSource());
            if (result == JFileChooser.APPROVE_OPTION) {
               final File selectedFile = mFileChooser.getSelectedFile();
               if (selectedFile != null) {
                  final String filePath = selectedFile.getAbsolutePath();
                  addItem(filePath);
               }
               for (int index = 0; index < mListeners.size(); index++) {
                  final ActionListener list = mListeners.get(index);
                  list.actionPerformed(e);
               }
            }
         }
      });
   }

   private void addItem(String filePath) {
      final Preferences pref = Preferences.userNodeForPackage(MRUFileBox.class);
      if (!mVectorList.contains(filePath)) {
         // Ensures that only the most recent items will be list
         if (mVectorList.size() == MAX_SIZE) {
            final Object item = mVectorList.remove(0);
            mFileDirectory.removeItem(item);
         }
         mVectorList.add(filePath);
         mFileDirectory.addItem(filePath);
         pref.put(mMRUFiles, getVectorFileStr());
      }
      mFileDirectory.setSelectedItem(filePath);
      mFileDirectory.setToolTipText(filePath);
      if ((filePath.compareToIgnoreCase(mDefaultDirectory) != 0) && (new File(filePath).exists())) {
         mDefaultDirectory = filePath;
         pref.put(mDefaultDir, mDefaultDirectory);
      }
   }

   public void setFileChooser(JFileChooser fc) {
      mFileChooser = fc;
      mFileChooser.setCurrentDirectory(new File(mDefaultDirectory));
   }

   private String getVectorFileStr() {
      final StringBuffer buf = new StringBuffer();
      for (int index = 0; index < mVectorList.size(); index++)
         buf.append(mVectorList.get(index) + ",");
      return buf.toString();
   }

   public String getSelectedFile() {
      return (String) mFileDirectory.getSelectedItem();
   }

   public void setSelectedItem(String item) {
      mFileDirectory.setSelectedItem(item);
   }

   public void addFileName(String filename) {
      addItem(filename);
   }

   public void setErrorColorsOn(boolean error) {
      if (error) {
         mFileDirectory.setBackground(Color.PINK);
         mFileDirectory.getEditor().getEditorComponent().setBackground(Color.PINK);
         mFileDirectory.requestFocusInWindow();
      } else {
         mFileDirectory.requestFocusInWindow();
         mFileDirectory.setBackground(SystemColor.window);
         mFileDirectory.getEditor().getEditorComponent().setBackground(SystemColor.window);
      }
   }

   public synchronized void addActionListener(ActionListener newListener) {
      mListeners.add(newListener);
   }

   /**
    * main - just for testing
    * 
    * @param args
    */
   public static void main(String[] args) {
      JFrame.setDefaultLookAndFeelDecorated(false);
      final JFrame frame = new JFrame();
      frame.setTitle("Testing of MRUFileBox");
      final MRUFileBox box = new MRUFileBox("Default vector directory", "MRU vector files");

      frame.add(box);
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.setVisible(true);

   }

}
