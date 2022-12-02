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
public class PersonEditor extends JDialog {

   private static final long serialVersionUID = -1516387854025417776L;

   private final JTextField jTextField_Name = new JTextField();
   private final JTextArea jTextArea_Details = new JTextArea();
   private final JButton jButton_Ok = new JButton("Ok");
   private final JButton jButton_Cancel = new JButton("Cancel");

   private boolean mOk = false;

   private void initialize() {
      setLayout(new FormLayout("right:pref, 5dlu, fill:100dlu", "pref, 5dlu, fill:100dlu, 10dlu, pref"));
      if (getContentPane() instanceof JPanel) {
         final JPanel p = (JPanel) getContentPane();
         p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      }
      final CellConstraints cc = new CellConstraints();
      add(new JLabel("Full Name"), cc.xy(1, 1));
      add(jTextField_Name, cc.xy(3, 1));
      add(new JLabel("Comment"), cc.xy(1, 3, "r, t"));
      add(new JScrollPane(jTextArea_Details), cc.xy(3, 3));
      final ButtonBarBuilder bbb = new ButtonBarBuilder();
      bbb.addGlue();
      bbb.addButton(jButton_Ok, jButton_Cancel);
      add(bbb.build(), cc.xyw(1, 5, 3));

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
   public PersonEditor(Dialog owner) {
      super(owner, "Edit person data", true);
      try {
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a GenericEditor
    * 
    * @param owner
    */
   public PersonEditor(Frame owner) {
      super(owner, "Edit person data", true);
      try {
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }
}
