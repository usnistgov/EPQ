package gov.nist.microanalysis.JythonGUI;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * <p>
 * Title: Jython GUI
 * </p>
 * <p>
 * Description: A simple GUI for running Jython scripts
 * </p>
 * <p>
 * Not copyright: In the public domain
 * </p>
 * <p>
 * Company: Duck-and-Cover
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class JythonFrame_AboutBox
   extends JDialog
   implements ActionListener {

   private static final long serialVersionUID = 0x1;
   JPanel panel1 = new JPanel();
   JPanel panel2 = new JPanel();
   JPanel insetsPanel1 = new JPanel();
   JPanel insetsPanel2 = new JPanel();
   JPanel insetsPanel3 = new JPanel();
   JButton button1 = new JButton();
   JLabel imageLabel = new JLabel();
   JLabel label1 = new JLabel();
   JLabel label2 = new JLabel();
   JLabel label3 = new JLabel();
   JLabel label4 = new JLabel();
   ImageIcon image1 = new ImageIcon();
   BorderLayout borderLayout1 = new BorderLayout();
   BorderLayout borderLayout2 = new BorderLayout();
   FlowLayout flowLayout1 = new FlowLayout();
   GridLayout gridLayout1 = new GridLayout();
   String product = "<HTML><h3>Jython GUI</h3>";
   String version = "1.0.2";
   String copyright = "<HTML><b>Public domain</b>: This software was developed at the National Institute of Standards<br/>"
   					  + " and Technology by employees of the Federal Government in the course of their official duties.<br/>"
   					  + "Pursuant to title 17 Section 105 of the United States Code this software is not subject to<br/>"
   					  + "copyright protection and is in the public domain.";
   String comments = "<HTML><i>A simple GUI for running Jython scripts</i>";

   public JythonFrame_AboutBox(Frame parent) {
      super(parent);
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      try {
         jbInit();
      }
      catch(Exception e) {
         e.printStackTrace();
      }
   }

   JythonFrame_AboutBox() {
      this(null);
   }

   // Component initialization
   private void jbInit()
         throws Exception {
      image1 = new ImageIcon(gov.nist.microanalysis.JythonGUI.JythonFrame.class.getResource("about.png"));
      imageLabel.setIcon(image1);
      this.setTitle("About");
      panel1.setLayout(borderLayout1);
      panel2.setLayout(borderLayout2);
      insetsPanel1.setLayout(flowLayout1);
      insetsPanel2.setLayout(flowLayout1);
      insetsPanel2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      gridLayout1.setRows(4);
      gridLayout1.setColumns(1);
      label1.setText(product);
      label2.setText(version);
      label3.setText(copyright);
      label4.setText(comments);
      insetsPanel3.setLayout(gridLayout1);
      insetsPanel3.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 10));
      button1.setText("Ok");
      button1.addActionListener(this);
      insetsPanel2.add(imageLabel, null);
      panel2.add(insetsPanel2, BorderLayout.WEST);
      this.getContentPane().add(panel1, null);
      insetsPanel3.add(label1, null);
      insetsPanel3.add(label2, null);
      insetsPanel3.add(label3, null);
      insetsPanel3.add(label4, null);
      panel2.add(insetsPanel3, BorderLayout.CENTER);
      insetsPanel1.add(button1, null);
      panel1.add(insetsPanel1, BorderLayout.SOUTH);
      panel1.add(panel2, BorderLayout.NORTH);
      setResizable(true);
   }

   // Overridden so we can exit when window is closed
   protected void processWindowEvent(WindowEvent e) {
      if(e.getID() == WindowEvent.WINDOW_CLOSING) {
         cancel();
      }
      super.processWindowEvent(e);
   }

   // Close the dialog
   void cancel() {
      dispose();
   }

   // Close the dialog on a button event
   public void actionPerformed(ActionEvent e) {
      if(e.getSource() == button1) {
         cancel();
      }
   }
}
