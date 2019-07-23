package gov.nist.microanalysis.EPQTools;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gov.nist.microanalysis.EPQTools.JWizardDialog.JWizardPanel;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Description: A progress panel to use while tasks that take a long time
 * execute.
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
public class JWizardProgressPanel<T>
   extends JWizardPanel {
   private static final long serialVersionUID = -7686047235940468693L;
   private final JProgressBar jProgressBar_Progress = new JProgressBar();
   private final JLabel jLabel_Progress = new JLabel("");
   private final SwingWorker<T, Integer> mThread;
   private T mResults;
   private String mMessage;
   private String mOutOf;
   private int mProgress = 0;

   private String format(int val) {
      StringBuffer res = new StringBuffer();
      String tmp = Integer.toString(val);
      final int len = tmp.length();
      for(int i = 0; i < len; ++i) {
         if((len - i) % 3 == 0)
            res.append('\u2009');
         res.append(tmp.charAt(i));
      }
      return res.toString();

   }

   /**
    * Constructs a JWizardProgressPanel
    * 
    * @param wiz
    * @param message
    * @param thread The thread should implement {@link SwingWorker} methods
    *           'process(java.util.List&lt;Integer&gt; chunks)' to update this
    *           panel using the setProgress method and 'done()' to set
    *           'enableFinish(true)'.
    */
   public JWizardProgressPanel(JWizardDialog wiz, String message, SwingWorker<T, Integer> thread) {
      super(wiz);
      mMessage = message;
      mThread = thread;
      jLabel_Progress.setHorizontalAlignment(JLabel.CENTER);
      initialize();
   }

   private void initialize() {
      final FormLayout layout = new FormLayout("200dlu", "pref, 2dlu, 20dlu, 5dlu, pref");
      setLayout(layout);
      final PanelBuilder pb = new PanelBuilder(layout, this);
      final CellConstraints cc = new CellConstraints();
      pb.addSeparator("Progress", cc.xy(1, 1));
      setRange(0, 100);
      pb.add(jProgressBar_Progress, cc.xy(1, 3));
      pb.add(jLabel_Progress, cc.xy(1, 5));
   }

   @Override
   public void onShow() {
      getWizard().setMessageText(mMessage);
      getWizard().enableFinish(false);
      setProgress(0);
      if(mThread != null)
         mThread.execute();
      else {
         getWizard().setMessageText("Nothing to do!");
         setProgress(100);
      }
   }

   public void setRange(int min, int max) {
      jProgressBar_Progress.setMinimum(min);
      jProgressBar_Progress.setMaximum(max);
      jProgressBar_Progress.setValue(Math2.bound(jProgressBar_Progress.getValue(), min, max));
      mOutOf = " out of " + format(jProgressBar_Progress.getMaximum() - jProgressBar_Progress.getMinimum());
   }

   /**
    * A thread safe mechanism to update the message displayed in the
    * {@link JWizardDialog}.
    * 
    * @param msg
    */
   public void setMessage(String msg) {
      mMessage = msg;
      SwingUtilities.invokeLater(new Runnable() {
         @Override
         public void run() {
            getWizard().setMessageText(mMessage);
         }
      });
   }

   @Override
   public void onHide() {
      mResults = null;
      if(mThread != null)
         if(mThread.isDone())
            try {
               mResults = mThread.get();
            }
            catch(final Exception e) {
               mResults = null;
            }
         else
            mThread.cancel(true);
   }

   public void setProgress(int progress) {
      mProgress = progress;
      SwingUtilities.invokeLater(new Runnable() {
         @Override
         public void run() {
            jProgressBar_Progress.setValue(mProgress);
            jLabel_Progress.setText(format(mProgress) + mOutOf);
            if(mProgress >= jProgressBar_Progress.getMaximum())
               getWizard().enableFinish(true);
         }
      });
   }

   public T getResults() {
      return mResults;
   }
}
