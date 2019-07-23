package gov.nist.microanalysis.EPQTools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * <p>
 * An about dialog for the CITZAF application.
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

public class CITZAFAbout
   extends JDialog {
   static final long serialVersionUID = 0x1;
   JPanel jPanel_Main = new JPanel();
   JPanel jPanel_Button = new JPanel();
   JEditorPane jEditorPane_Main = new JEditorPane(); // @jve:decl-index=0:visual-constraint="95,46"
   JButton jButton_OK = new JButton();
   JScrollPane jScrollPane_Main = new JScrollPane();

   public CITZAFAbout(Frame frame, String title, boolean modal) {
      super(frame, title, modal);
      try {
         initialize();
         pack();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   public CITZAFAbout() {
      this(null, "About CITZAF...", false);
   }

   private void initialize()
         throws Exception {

      jButton_OK.setText("OK");
      jButton_OK.setAlignmentX((float) 0.5);
      jButton_OK.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            dispose();
         }
      });

      jPanel_Button.setLayout(new BoxLayout(jPanel_Button, BoxLayout.PAGE_AXIS));
      jPanel_Button.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
      jPanel_Button.add(jButton_OK);

      jEditorPane_Main.setContentType("text/html");
      jEditorPane_Main.setEditable(false);
      jEditorPane_Main.setPage(CITZAFAbout.class.getResource("CITZAF_About.html"));

      jScrollPane_Main.setPreferredSize(new Dimension(400, 300));
      jScrollPane_Main.getViewport().add(jEditorPane_Main, null);

      jPanel_Main.setLayout(new BorderLayout());
      jPanel_Main.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      jPanel_Main.add(jPanel_Button, BorderLayout.PAGE_END);
      jPanel_Main.add(jScrollPane_Main, BorderLayout.CENTER);
      getContentPane().add(jPanel_Main);
   }
}
