package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.EPQLibrary.CITZAF;
import gov.nist.microanalysis.EPQLibrary.CITZAF.ParticleModel;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

/**
 * <p>
 * A dialog for editing the particle model as required by CITZAF.
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

public class ParticleModelDialog
   extends JDialog {
   private static final long serialVersionUID = 0x1;
   ArrayList<String> MainParticleModelList = new ArrayList<String>();
   private final CITZAF.ParticleModel[] allModels = CITZAF.ParticleModel.getAllImplimentations();

   private transient Vector<ActionListener> WindowClosingListeners;

   JPanel Main_Panel = new JPanel();
   BorderLayout borderLayout1 = new BorderLayout();
   JScrollPane jScrollPane_Models = new JScrollPane();
   JPanel jPanel_Buttons = new JPanel();
   JList<Object> jList_Models = new JList<Object>();
   JButton jButton_Add = new JButton();
   JButton jButton_Remove = new JButton();
   JButton jButton_Clear = new JButton();
   JButton jButton_Done = new JButton();
   JPanel jPanel_SpacerFrame = new JPanel();

   JDialog ModelsChoiceDialog = new JDialog(this, "Model Choices...");
   ButtonGroup ModelsButtonGroup = new ButtonGroup();
   JLabel jLabel_Spacer = new JLabel();
   JPopupMenu jPopupMenu_ParticleModels = new JPopupMenu();

   public ParticleModelDialog(Frame frame, String title, boolean modal) {
      super(frame, title, modal);
      try {
         jbInit();
         pack();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   public ParticleModelDialog(Dialog dialog, String title, boolean modal) {
      super(dialog, title, modal);
      try {
         jbInit();
         pack();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   private void jbInit()
         throws Exception {
      Main_Panel.setLayout(borderLayout1);
      jPanel_Buttons.setPreferredSize(new Dimension(213, 60));
      Main_Panel.setPreferredSize(new Dimension(213, 200));
      jButton_Add.setMnemonic('A');
      jButton_Add.setText("Add");
      jButton_Add.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_Add_actionPerformed(e);
         }
      });
      jButton_Remove.setMnemonic('R');
      jButton_Remove.setText("Remove");
      jButton_Remove.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_Remove_actionPerformed(e);
         }
      });
      jButton_Clear.setMnemonic('C');
      jButton_Clear.setText("Clear");
      jButton_Clear.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_Clear_actionPerformed(e);
         }
      });
      jButton_Done.setMnemonic('D');
      jButton_Done.setText("Done");
      jButton_Done.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_Done_actionPerformed(e);
         }
      });
      jPanel_SpacerFrame.setPreferredSize(new Dimension(300, 30));
      this.addWindowListener(new java.awt.event.WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            this_windowClosing(e);
         }
      });
      jLabel_Spacer.setPreferredSize(new Dimension(133, 25));
      jLabel_Spacer.setText("");
      getContentPane().add(Main_Panel);
      Main_Panel.add(jScrollPane_Models, BorderLayout.CENTER);
      jScrollPane_Models.getViewport().add(jList_Models, null);
      jList_Models.setForeground(SystemColor.textText);
      jList_Models.setListData(new Object[] {
         "Select a particle model"
      });
      Main_Panel.add(jPanel_Buttons, BorderLayout.SOUTH);
      jPanel_Buttons.add(jButton_Add, null);
      jPanel_Buttons.add(jButton_Remove, null);
      jPanel_Buttons.add(jButton_Clear, null);
      jPanel_Buttons.add(jLabel_Spacer, null);
      jPanel_Buttons.add(jButton_Done, null);
      Main_Panel.add(jPanel_SpacerFrame, BorderLayout.NORTH);

      for(final ParticleModel allModel : allModels) {
         final java.awt.event.ActionListener menuListener = new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
               MainParticleModelList.add(e.getActionCommand());
               jList_Models.setListData(MainParticleModelList.toArray());
               ParticleModelDialog.this.setTitle(MainParticleModelList.size() + " particle models selected");
            }
         };
         final JMenuItem item = new JMenuItem(allModel.toString());
         item.addActionListener(menuListener);
         jPopupMenu_ParticleModels.add(item);
      }

   }

   void jButton_Add_actionPerformed(ActionEvent e) {
      jPopupMenu_ParticleModels.show(jButton_Add, 0, 0);
   }

   void this_windowClosing(WindowEvent e) {
      fireWindowClosingEvent(new ActionEvent(e.getSource(), e.getID(), "Window Closing"));
   }

   void jButton_Remove_actionPerformed(ActionEvent e) {
      final int Selected = jList_Models.getSelectedIndex();
      if(Selected != -1)
         MainParticleModelList.remove(Selected);
      jList_Models.setListData(MainParticleModelList.toArray());
      this.setTitle(MainParticleModelList.size() + " particle models selected");
   }

   void jButton_Clear_actionPerformed(ActionEvent e) {
      MainParticleModelList.clear();
      jList_Models.setListData(new Object[] {
         "Select a particle model"
      });
      this.setTitle("Model Choices...");
   }

   protected void fireWindowClosingEvent(ActionEvent e) {
      if(WindowClosingListeners != null) {
         final Vector<ActionListener> listeners = WindowClosingListeners;
         final int count = listeners.size();
         for(int i = 0; i < count; i++)
            listeners.elementAt(i).actionPerformed(e);
      }
   }

   /**
    * addWindowClosingListener - Adds a window closing listener. When the dialog
    * closes, an event will be triggered so that all listening components can
    * respond appropriately.
    * 
    * @param l ActionListener
    */
   public synchronized void addWindowClosingListener(ActionListener l) {
      final Vector<ActionListener> v = WindowClosingListeners == null ? new Vector<ActionListener>(2)
            : new Vector<ActionListener>(WindowClosingListeners);
      if(!v.contains(l)) {
         v.addElement(l);
         WindowClosingListeners = v;
      }
   }

   /**
    * removeWindowClosingListener - removes the window closing listener
    * 
    * @param l ActionListener
    */
   public synchronized void removeWindowClosingListener(ActionListener l) {
      if((WindowClosingListeners != null) && WindowClosingListeners.contains(l)) {
         final Vector<ActionListener> v = new Vector<ActionListener>(WindowClosingListeners);
         v.removeElement(l);
         WindowClosingListeners = v;
      }
   }

   void jButton_Done_actionPerformed(ActionEvent e) {
      fireWindowClosingEvent(e);
   }

   public void setParticleModels(CITZAF.ParticleModel[] ParticleModels) {
      MainParticleModelList.clear();
      for(final ParticleModel particleModel : ParticleModels)
         MainParticleModelList.add(particleModel.toString());
      jList_Models.setListData(MainParticleModelList.toArray());
   }
}
