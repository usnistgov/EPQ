/**
 * gov.nist.microanalysis.EPQDatabase.GenericEditor Created by: nicholas Date:
 * Sep 26, 2007
 */
package gov.nist.microanalysis.EPQDatabase;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * <p>
 * Description
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nicholas
 * @version 1.0
 */
public class ProjectEditor
   extends JDialog {

   private static final long serialVersionUID = -1516387854025417775L;

   private final JTextField jTextField_Name = new JTextField();
   private final JTextArea jTextArea_Details = new JTextArea();
   private final JComboBox<String> jComboBox_Client = new JComboBox<String>();
   private final JButton jButton_AddClient = new JButton("Add");
   private final JButton jButton_Ok = new JButton("Ok");
   private final JButton jButton_Cancel = new JButton("Cancel");

   private boolean mOk = false;
   private final Session mSession;

   private void initialize() {
      setLayout(new FormLayout("right:pref, 5dlu, fill:150dlu, 5dlu, pref", "pref, 5dlu, pref, 5dlu, fill:100dlu, 10dlu, pref"));
      final CellConstraints cc = new CellConstraints();
      add(new JLabel("Project Name"), cc.xy(1, 1));
      add(jTextField_Name, cc.xy(3, 1));
      add(new JLabel("Client"), cc.xy(1, 3));
      add(jComboBox_Client, cc.xy(3, 3));
      add(jButton_AddClient, cc.xy(5, 3));
      add(new JLabel("Details"), cc.xy(1, 5, "r, t"));
      add(new JScrollPane(jTextArea_Details), cc.xy(3, 5));
      final ButtonBarBuilder bbb = new ButtonBarBuilder();
      bbb.addGlue();
      bbb.addButton(jButton_Ok, jButton_Cancel);
      add(bbb.build(), cc.xyw(1, 7, 3));
      if(getContentPane() instanceof JPanel) {
         final JPanel p = (JPanel) getContentPane();
         p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      }

      jButton_AddClient.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final PersonEditor pe = new PersonEditor(ProjectEditor.this);
            pe.setLocationRelativeTo(ProjectEditor.this);
            if(pe.execute()) {
               mSession.addPerson(pe.getName(), pe.getDetails());
               jComboBox_Client.removeAllItems();
               for(final String person : mSession.getPeople().keySet())
                  jComboBox_Client.addItem(person);
               jComboBox_Client.setSelectedItem(pe.getName());
            }
         }
      });

      jButton_Ok.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mOk = true;
            setVisible(false);
         }
      });
      jButton_Cancel.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mOk = false;
            setVisible(false);
         }
      });
      for(final String person : mSession.getPeople().keySet())
         jComboBox_Client.addItem(person);
      jComboBox_Client.setSelectedIndex(0);
      pack();
   }

   public boolean execute() {
      setVisible(true);
      return mOk;
   }

   public boolean isOk() {
      return mOk;
   }

   @Override
   public String getName() {
      return jTextField_Name.getText();
   }

   public String getDetails() {
      return jTextArea_Details.getText();
   }

   public int getClient() {
      return mSession.getPeople().get(jComboBox_Client.getSelectedItem()).intValue();
   }

   @Override
   public void setName(String name) {
      jTextField_Name.setText(name);
   }

   public void setDetails(String details) {
      jTextArea_Details.setText(details);
   }

   /**
    * Constructs a GenericEditor
    * 
    * @param owner
    */
   public ProjectEditor(Dialog owner, Session ses) {
      super(owner, "Edit project data", true);
      mSession = ses;
      try {
         initialize();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a GenericEditor
    * 
    * @param owner
    */
   public ProjectEditor(Frame owner, Session ses) {
      super(owner, "Edit project data", true);
      mSession = ses;
      try {
         initialize();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }
}
