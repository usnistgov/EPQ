package gov.nist.microanalysis.JythonGUI;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.prefs.Preferences;

import javax.swing.filechooser.FileFilter;

import gov.nist.microanalysis.JythonGUI.JythonFrame;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

/**
 * A simple GUI for running Jython scripts
 * </p>
 * <p>
 * Not copyright: In the public domain * 
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class JythonFrame
   extends JFrame {
   private static final long serialVersionUID = 0x1;
   JPanel contentPane;
   JMenuBar jMenuBar1 = new JMenuBar();
   JMenu jMenuFile = new JMenu();
   JMenuItem jMenuFileExit = new JMenuItem();
   JMenu jMenuHelp = new JMenu();
   JMenuItem jMenuHelpAbout = new JMenuItem();
   JLabel statusBar = new JLabel();
   BorderLayout borderLayout1 = new BorderLayout();
   JCommandLine jCmdLine = new JCommandLine();
   JScrollPane jScrollPane1 = new JScrollPane();
   JPopupMenu jPopupMenu1 = new JPopupMenu();
   JMenuItem jMenuItem_Copy = new JMenuItem();
   JMenuItem jMenuItem_Cut = new JMenuItem();
   JMenuItem jMenuItem_Paste = new JMenuItem();
   JMenuItem jMenuFileOpen = new JMenuItem();
   JMenuItem jMenuFileSave = new JMenuItem();

   JythonExecutive mExecutive;
   JToolBar jToolBar1 = new JToolBar();
   JButton goButton = new JButton();

   File mLastScript;

   // Construct the frame
   public JythonFrame() {
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      try {
         jbInit();
         restorePosition();
      }
      catch(Exception e) {
         e.printStackTrace();
      }
      mExecutive = new JythonExecutive(jCmdLine);
      mExecutive.getInteractiveInterpreter().set("Frame",this);
      jCmdLine.setCommandExecutive(mExecutive);
   }

   // Component initialization
   private void jbInit()
         throws Exception {
      contentPane = (JPanel) this.getContentPane();
      contentPane.setLayout(borderLayout1);
      this.setSize(new Dimension(400, 300));
      this.setTitle("Jython");
      statusBar.setText("Built around the Jython interpreter from http://www.jython.org.");
      jMenuFile.setMnemonic('F');
      jMenuFile.setText("File");
      jMenuFileOpen.setMnemonic('O');
      jMenuFileOpen.setText("Open script");
      jMenuFileOpen.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(ActionEvent e) {
            jMenuFileOpen_actionPerformed(e);
         }
      });
      jMenuFileSave.setMnemonic('S');
      jMenuFileSave.setText("Save script");
      jMenuFileSave.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(ActionEvent e) {
            jMenuFileSave_actionPerformed(e);
         }
      });
      jMenuFileExit.setMnemonic('X');
      jMenuFileExit.setText("Exit");
      jMenuFileExit.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(ActionEvent e) {
            jMenuFileExit_actionPerformed(e);
         }
      });
      jMenuHelp.setMnemonic('H');
      jMenuHelp.setText("Help");
      jMenuHelpAbout.setMnemonic('A');
      jMenuHelpAbout.setText("About");
      jMenuHelpAbout.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(ActionEvent e) {
            jMenuHelpAbout_actionPerformed(e);
         }
      });
      jCmdLine.setFont(new java.awt.Font("Serif", 0, 16));
      jCmdLine.setToolTipText("");
      jCmdLine.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mousePressed(MouseEvent e) {
            jCmdLine_mousePressed(e);
         }

         public void mouseReleased(MouseEvent e) {
            jCmdLine_mouseReleased(e);
         }
      });
      jMenuItem_Copy.setMnemonic('O');
      jMenuItem_Copy.setText("Copy");
      jMenuItem_Copy.setAccelerator(javax.swing.KeyStroke.getKeyStroke('C', java.awt.event.KeyEvent.CTRL_DOWN_MASK, false));
      jMenuItem_Copy.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(ActionEvent e) {
            jMenuItem_Copy_actionPerformed(e);
         }
      });
      jMenuItem_Cut.setMnemonic('T');
      jMenuItem_Cut.setText("Cut");
      jMenuItem_Cut.setAccelerator(javax.swing.KeyStroke.getKeyStroke('X', java.awt.event.KeyEvent.CTRL_DOWN_MASK, false));
      jMenuItem_Cut.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(ActionEvent e) {
            jMenuItem_Cut_actionPerformed(e);
         }
      });
      jMenuItem_Paste.setMnemonic('P');
      jMenuItem_Paste.setText("Paste");
      jMenuItem_Paste.setAccelerator(javax.swing.KeyStroke.getKeyStroke('V', java.awt.event.KeyEvent.CTRL_DOWN_MASK, false));
      jMenuItem_Paste.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(ActionEvent e) {
            jMenuItem_Paste_actionPerformed(e);
         }
      });
      contentPane.setMinimumSize(new Dimension(100, 100));
      contentPane.setPreferredSize(new Dimension(500, 400));
      jScrollPane1.setMinimumSize(new Dimension(100, 100));
      jScrollPane1.setPreferredSize(new Dimension(100, 100));
      goButton.setEnabled(false);
      goButton.setDoubleBuffered(false);
      goButton.setToolTipText("Rerun the last script");
      goButton.setActionCommand("goButton");
      goButton.setText("Run");
      goButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(ActionEvent e) {
            goButton_actionPerformed(e);
         }
      });
      jMenuFile.add(jMenuFileOpen);
      jMenuFile.add(jMenuFileSave);
      jMenuFile.add(jMenuFileExit);
      jMenuHelp.add(jMenuHelpAbout);
      jMenuBar1.add(jMenuFile);
      jMenuBar1.add(jMenuHelp);
      this.setJMenuBar(jMenuBar1);
      contentPane.add(statusBar, BorderLayout.SOUTH);
      contentPane.add(jScrollPane1, BorderLayout.CENTER);
      contentPane.add(jToolBar1, BorderLayout.NORTH);
      jToolBar1.add(goButton, null);
      jScrollPane1.getViewport().add(jCmdLine, null);
      jPopupMenu1.add(jMenuItem_Cut);
      jPopupMenu1.add(jMenuItem_Copy);
      jPopupMenu1.add(jMenuItem_Paste);
   }

   // File | Exit action performed
   public void jMenuFileExit_actionPerformed(ActionEvent e) {
      storePosition();
      System.exit(0);
   }

   private void storePosition() {
      Preferences userPref = Preferences.userNodeForPackage(JythonFrame.class);
      userPref.putInt("Main window\\top", (int) getBounds().getX());
      userPref.putInt("Main window\\left", (int) getBounds().getY());
      userPref.putInt("Main window\\width", getWidth());
      userPref.putInt("Main window\\height", getHeight());
   }

   private void restorePosition() {
      Preferences userPref = Preferences.userNodeForPackage(JythonFrame.class);
      setBounds(userPref.getInt("Main window\\top", (int) getBounds().getX()), userPref.getInt("Main window\\left", (int) getBounds().getY()), userPref.getInt("Main window\\width", getWidth()), userPref.getInt("Main window\\height", getHeight()));
   }

   // Help | About action performed
   public void jMenuHelpAbout_actionPerformed(ActionEvent e) {
      JythonFrame_AboutBox dlg = new JythonFrame_AboutBox(this);
      Dimension dlgSize = dlg.getPreferredSize();
      Dimension frmSize = getSize();
      Point loc = getLocation();
      dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
      dlg.setModal(true);
      dlg.pack();
      dlg.setVisible(true);
   }

   // Overridden so we can exit when window is closed
   protected void processWindowEvent(WindowEvent e) {
      super.processWindowEvent(e);
      if(e.getID() == WindowEvent.WINDOW_CLOSING) {
         jMenuFileExit_actionPerformed(null);
      }
   }

   void jMenuItem_Copy_actionPerformed(ActionEvent e) {
      jCmdLine.copy();
   }

   void jMenuItem_Cut_actionPerformed(ActionEvent e) {
      jCmdLine.cut();
   }

   void jMenuItem_Paste_actionPerformed(ActionEvent e) {
      jCmdLine.paste();
   }

   void jCmdLine_mousePressed(MouseEvent e) {
      if(e.isPopupTrigger()) {
         jPopupMenu1.show(jCmdLine, e.getX(), e.getY());
      }
   }

   void jCmdLine_mouseReleased(MouseEvent e) {
      jCmdLine_mousePressed(e);
   }

   public class SimpleFileFilter
      extends FileFilter {
      private String[] mExtensions;
      private String mDescription;

      public SimpleFileFilter(String[] exts, String desc) {
         mExtensions = new String[exts.length];
         for(int i = exts.length - 1; i >= 0; --i)
            mExtensions[i] = "." + exts[i].toLowerCase();
         mDescription = desc;
      }

      public boolean accept(File f) {
         if(f.isDirectory())
            return true;
         String name = f.getName().toLowerCase();
         for(int i = mExtensions.length - 1; i >= 0; --i)
            if(name.endsWith(mExtensions[i]))
               return true;
         return false;
      }

      public String getDescription() {
         return mDescription;
      }
   }

   void jMenuFileOpen_actionPerformed(ActionEvent e) {
      final String prefPath="Open Directory";
      Preferences userPref = Preferences.userNodeForPackage(JythonFrame.class);
      String dir=userPref.get(prefPath,System.getProperty("user.home"));
      JFileChooser fc = new JFileChooser(dir);
      SimpleFileFilter filter=new SimpleFileFilter(new String[] { "py" }, "Jython Script");
      fc.setFileFilter(filter);
      fc.setAcceptAllFileFilterUsed(true);
      if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){
         File f=fc.getSelectedFile();
         userPref.put(prefPath,f.getParent());
         try {
            executeFile(f);
            mLastScript = f;
            goButton.setEnabled(true);
         }
         catch(Exception ex) {
            statusBar.setText(ex.toString());
         }
      }
   }

   void jMenuFileSave_actionPerformed(ActionEvent e) {
      final String prefPath="Save Directory";
      Preferences userPref = Preferences.userNodeForPackage(JythonFrame.class);
      String dir=userPref.get(prefPath,System.getProperty("user.home"));
      JFileChooser fc = new JFileChooser(dir);
      SimpleFileFilter filter=new SimpleFileFilter(new String[] { "txt" }, "Text File");
      fc.setFileFilter(filter);
      fc.setAcceptAllFileFilterUsed(true);
      if(fc.showSaveDialog(this)==JFileChooser.APPROVE_OPTION){
         File f=fc.getSelectedFile();
         try {
            FileWriter fw = new FileWriter(f);
            userPref.put(prefPath,f.getParent());
            fw.write(jCmdLine.getText());
            fw.close();
            statusBar.setText(f.getName() + " written.");
         }
         catch(IOException ex) {
            statusBar.setText("ERROR: " + ex.toString());
         }
      }
   }

   public void executeFile(File f)
         throws Exception {
      jCmdLine.writeCommand("Executing " + f.toString() + "\n");
      mExecutive.setScriptSource(f);
      FileInputStream fis = new FileInputStream(f);
      jCmdLine.execute(fis);
      statusBar.setText(f.getName() + " executed.");
      mExecutive.setScriptSource(null);
   }

   void goButton_actionPerformed(ActionEvent e) {
      try {
         if(mLastScript != null)
            executeFile(mLastScript);
      }
      catch(Exception ex) {
         statusBar.setText(ex.toString());
      }
   }
}
