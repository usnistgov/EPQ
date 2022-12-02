package gov.nist.microanalysis.EPQTools;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * <p>
 * A dialog for entering the particle diameter for CITZAF calculations.
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

public class ParticleDiameterDialog extends JDialog {
   private static final long serialVersionUID = 0x1;
   JPanel panel_Main = new JPanel();
   BorderLayout borderLayout1 = new BorderLayout();
   JPanel jPanel_Buttons = new JPanel();
   JButton jButton_Add = new JButton();
   JButton jButton_Delete = new JButton();
   JButton jButton_Clear = new JButton();
   JButton jButton_Done = new JButton();
   JLabel jLabel_Spacer1 = new JLabel();
   JList<Object> jList_Diameters = new JList<Object>();
   JPanel jPanel_Top = new JPanel();
   JPanel jPanel_Bottom = new JPanel();
   JPanel jPanel_DiameterEntry = new JPanel();
   JLabel jLabel_ParticleDiameter = new JLabel();
   JTextField jTextField_ParticleDiameter = new JTextField();
   JLabel jLabel_ParticleUnits = new JLabel();

   private ArrayList<Double> DiameterList = new ArrayList<Double>();
   private transient ArrayList<ActionListener> WindowClosingListeners;
   JScrollPane jScrollPane_Center = new JScrollPane();

   public ParticleDiameterDialog(Frame frame, String title, boolean modal) {
      super(frame, title, modal);
      try {
         jbInit();
         pack();
         jTextField_ParticleDiameter.grabFocus();
         jTextField_ParticleDiameter.selectAll();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   public ParticleDiameterDialog(Dialog dialog, String title, boolean modal) {
      super(dialog, title, modal);
      try {
         jbInit();
         pack();
         jTextField_ParticleDiameter.grabFocus();
         jTextField_ParticleDiameter.selectAll();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   private void jbInit() throws Exception {
      panel_Main.setLayout(borderLayout1);
      jPanel_Buttons.setPreferredSize(new Dimension(200, 60));
      panel_Main.setPreferredSize(new Dimension(200, 250));
      jButton_Add.setMnemonic('A');
      jButton_Add.setText("Add");
      jButton_Add.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_Add_actionPerformed(e);
         }
      });
      jButton_Delete.setMnemonic('D');
      jButton_Delete.setText("Delete");
      jButton_Delete.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_Delete_actionPerformed(e);
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
      jLabel_Spacer1.setPreferredSize(new Dimension(125, 25));
      jLabel_Spacer1.setText("");
      jPanel_Top.setPreferredSize(new Dimension(200, 20));
      jList_Diameters.setForeground(SystemColor.textText);
      jList_Diameters.setListData(new Object[]{"Your particle diameters will be here"});
      jPanel_Bottom.setPreferredSize(new Dimension(200, 100));
      jPanel_DiameterEntry.setPreferredSize(new Dimension(200, 25));
      jLabel_ParticleDiameter.setText("Particle Diameter :");
      jTextField_ParticleDiameter.setPreferredSize(new Dimension(57, 21));
      jTextField_ParticleDiameter.setText("0.0");
      jTextField_ParticleDiameter.setHorizontalAlignment(SwingConstants.TRAILING);
      jTextField_ParticleDiameter.addKeyListener(new java.awt.event.KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            jTextField_ParticleDiameter_keyPressed(e);
         }
      });
      jTextField_ParticleDiameter.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            jTextField_ParticleDiameter_focusGained(e);
         }
      });
      jLabel_ParticleUnits.setText("(�m)");
      jPanel_Bottom.add(jPanel_DiameterEntry, null);
      getContentPane().add(panel_Main);
      jPanel_Buttons.add(jButton_Add, null);
      jPanel_Buttons.add(jButton_Delete, null);
      jPanel_Buttons.add(jButton_Clear, null);
      jPanel_Buttons.add(jLabel_Spacer1, null);
      jPanel_Buttons.add(jButton_Done, null);
      panel_Main.add(jScrollPane_Center, BorderLayout.CENTER);
      jScrollPane_Center.getViewport().add(jList_Diameters, null);
      panel_Main.add(jPanel_Top, BorderLayout.NORTH);
      panel_Main.add(jPanel_Bottom, BorderLayout.SOUTH);
      jPanel_Bottom.add(jPanel_Buttons, null);
      jPanel_DiameterEntry.add(jLabel_ParticleDiameter, null);
      jPanel_DiameterEntry.add(jTextField_ParticleDiameter, null);
      jPanel_DiameterEntry.add(jLabel_ParticleUnits, null);
   }

   void jTextField_ParticleDiameter_focusGained(FocusEvent e) {
      jTextField_ParticleDiameter.selectAll();
   }

   void jButton_Add_actionPerformed(ActionEvent e) {
      try {
         final NumberFormat nf = NumberFormat.getInstance();
         final double diameter = nf.parse(jTextField_ParticleDiameter.getText()).doubleValue();
         if (diameter <= 0.0)
            throw new Exception();
         DiameterList.add(Double.valueOf(diameter));
         jList_Diameters.setListData(DiameterList.toArray());
         ParticleDiameterDialog.this.setTitle(DiameterList.size() + " particle diameters selected");

      } catch (final Exception nfex) {
      }
      jTextField_ParticleDiameter.grabFocus();
      jTextField_ParticleDiameter.selectAll();
   }

   void jButton_Delete_actionPerformed(ActionEvent e) {
      final int selected = jList_Diameters.getSelectedIndex();
      if ((selected != -1) && ((jList_Diameters.getSelectedValue().toString()).compareTo("Your particle diameters will be here") != 0)) {
         DiameterList.remove(selected);
         jList_Diameters.setListData(DiameterList.toArray());
         ParticleDiameterDialog.this.setTitle(DiameterList.size() + " particle diameters selected");
      }
   }

   void jButton_Clear_actionPerformed(ActionEvent e) {
      DiameterList = new ArrayList<Double>();
      jList_Diameters.setListData(new Object[]{"Your particle diameters will be here"});
      ParticleDiameterDialog.this.setTitle("Enter the particle(s) diameter(s)");
   }

   public ArrayList<Double> getDiameters() {
      return DiameterList;
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
      final ArrayList<ActionListener> v = WindowClosingListeners == null
            ? new ArrayList<ActionListener>(2)
            : new ArrayList<ActionListener>(WindowClosingListeners);
      if (!v.contains(l)) {
         v.add(l);
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
         final ArrayList<ActionListener> v = new ArrayList<ActionListener>(WindowClosingListeners);
         v.remove(l);
         WindowClosingListeners = v;
      }
   }

   protected void fireWindowClosingEvent(ActionEvent e) {
      if (WindowClosingListeners != null) {
         final ArrayList<ActionListener> listeners = WindowClosingListeners;
         final int count = listeners.size();
         for (int i = 0; i < count; i++)
            listeners.get(i).actionPerformed(e);
      }
   }

   void jButton_Done_actionPerformed(ActionEvent e) {
      fireWindowClosingEvent(e);
   }

   void jTextField_ParticleDiameter_keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_ENTER)
         jButton_Add_actionPerformed(new ActionEvent(e.getSource(), e.getID(), e.toString()));
   }

   public void setParticleDiameters(double[] Diameters) {
      DiameterList.clear();
      for (final double diameter : Diameters)
         DiameterList.add(Double.valueOf(diameter));
      jList_Diameters.setListData(DiameterList.toArray());
   }
}
