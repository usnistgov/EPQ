package gov.nist.microanalysis.EPQTools;

import java.awt.BorderLayout;
import java.awt.Window;
import java.util.concurrent.ExecutionException;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.jgoodies.forms.builder.PanelBuilder;
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
 * @author nritchie
 * @version 1.0
 */
public class JProgressDialog
   extends JDialog {

   private static final long serialVersionUID = -3818437128678948095L;

   private final JProgressBar jProgressBar;

   public JProgressDialog(Window w, String title) {
      super(w, "Please wait...", ModalityType.APPLICATION_MODAL);
      PanelBuilder pb = new PanelBuilder(new FormLayout("20dlu, 200dlu, 20dlu", "20dlu, pref, 5dlu, pref, 20dlu"));
      final CellConstraints cc = new CellConstraints();
      jProgressBar = new JProgressBar(0, 100);
      setProgress(0);
      pb.add(jProgressBar, cc.xy(2, 2));
      pb.add(new JLabel(title), cc.xy(2, 4));
      add(pb.getPanel(), BorderLayout.CENTER);
      setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      setResizable(false);
   }

   public void perform(Runnable runnable) {
      pack();
      setLocationRelativeTo(getParent());
      final SwingWorker<Object, Object> sw = new SwingWorker<Object, Object>() {

         @Override
         protected Object doInBackground()
               throws Exception {
            try {
               runnable.run();
            }
            finally {
               SwingUtilities.invokeAndWait(new Runnable() {
                  @Override
                  public void run() {
                     setVisible(false);
                  }
               });
            }
            return null;
         }
      };
      try {
         sw.execute();
         setVisible(true);
         sw.get();
      }
      catch(InterruptedException e) {
         e.printStackTrace();
      }
      catch(ExecutionException e) {
         e.printStackTrace();
      }
      setVisible(false);
   }

   /**
    * Set the percent progress towards completion of the waited task.
    *
    * @param pct
    */
   public void setProgress(int pct) {
      jProgressBar.setValue(pct);
      jProgressBar.setString(Integer.toString(pct) + " %");
   }
}
