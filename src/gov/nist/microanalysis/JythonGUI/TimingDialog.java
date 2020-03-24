package gov.nist.microanalysis.JythonGUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.border.Border;

import gov.nist.microanalysis.JythonGUI.TimingDialog;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * <p>
 * A dialog for monitoring lengthy processes.
 * </p>
 * <p>
 * Not copyright: In the public domain * 
 * </p>
 *
 * @author not attributable
 * @version 1.0
 */

public class TimingDialog
   extends JDialog {
   private static final long serialVersionUID = 0x1;
   JPanel backPanel = new JPanel();
   BorderLayout borderLayout1 = new BorderLayout();
   JLabel longTimeLabel = new JLabel();
   JProgressBar progressBar = new JProgressBar(0, 60);
   JPanel buttonPanel = new JPanel();
   JButton cancel = new JButton();
   BorderLayout borderLayout2 = new BorderLayout();
   JLabel elapseLabel = new JLabel();
   JPanel paddingPanel1 = new JPanel();
   JPanel paddingPanel2 = new JPanel();
   private Timer mTimer;
   private long mStartTime = System.currentTimeMillis();
   private Thread mThread;
   Border border1;

   public TimingDialog(Frame frame, String title, boolean modal) {
      super(frame, title, modal);
      try {
         jbInit();
         pack();
         mTimer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
               long delta = ((System.currentTimeMillis() - mStartTime) + 500) / 1000;
               progressBar.setValue(delta % 120 > 60 ? (int) (120 - delta % 120) : (int) (delta % 120));
               int hrs = (int) (delta / 3600);
               int mins = (int) ((delta / 60) % 60);
               int secs = (int) (delta % 60);
               elapseLabel.setText("Elapse time: " + Integer.toString(hrs) + (mins > 9 ? ":" : ":0") + Integer.toString(mins)
                     + (secs > 9 ? ":" : ":0") + Integer.toString(secs));
               if((mThread != null) && (!mThread.isAlive())) {
                  TimingDialog.this.setVisible(false);
                  mThread = null;
               }
            }
         });
      }
      catch(Exception ex) {
         ex.printStackTrace();
      }
   }

   public TimingDialog() {
      this(null, "", false);
   }

   public void setOperation(Runnable op) {
      if(mThread == null) {
         mThread = new Thread(op);
         mThread.start();
         mTimer.start();
      }
   }

   private void jbInit()
         throws Exception {
      border1 = BorderFactory.createMatteBorder(4, 4, 4, 4, SystemColor.control);
      backPanel.setLayout(borderLayout1);
      longTimeLabel.setText("This operation may take a long time to complete...");
      cancel.setMaximumSize(new Dimension(32000, 23));
      cancel.setMinimumSize(new Dimension(25, 23));
      cancel.setMnemonic('0');
      cancel.setText("Cancel");
      cancel.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(ActionEvent e) {
            cancel_actionPerformed(e);
         }
      });
      buttonPanel.setLayout(borderLayout2);
      elapseLabel.setHorizontalAlignment(SwingConstants.CENTER);
      elapseLabel.setText("Elapse time: 0:00:00");
      backPanel.setMinimumSize(new Dimension(226, 50));
      backPanel.setPreferredSize(new Dimension(226, 100));
      progressBar.setBorder(border1);
      progressBar.setMaximumSize(new Dimension(32767, 16));
      progressBar.setMinimumSize(new Dimension(10, 6));
      progressBar.setPreferredSize(new Dimension(108, 16));
      paddingPanel2.setPreferredSize(new Dimension(40, 10));
      paddingPanel1.setPreferredSize(new Dimension(40, 10));
      getContentPane().add(backPanel);
      backPanel.add(longTimeLabel, BorderLayout.NORTH);
      backPanel.add(progressBar, BorderLayout.CENTER);
      backPanel.add(buttonPanel, BorderLayout.SOUTH);
      buttonPanel.add(elapseLabel, BorderLayout.NORTH);
      buttonPanel.add(cancel, BorderLayout.CENTER);
      buttonPanel.add(paddingPanel1, BorderLayout.WEST);
      buttonPanel.add(paddingPanel2, BorderLayout.EAST);
      setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      setResizable(false);
   }

   protected void processWindowEvent(WindowEvent e) {
      if(e.getID() == WindowEvent.WINDOW_CLOSING) {
         cancel_actionPerformed(null);
      }
   }

   @SuppressWarnings("deprecation")
void cancel_actionPerformed(ActionEvent e) {
      if(mThread == null) {
         setVisible(false);
      } else {
         cancel.setText("Canceling...");
         cancel.setEnabled(false);
         // yes, I know it is depreciated but what other options do I have???
         mThread.stop();
      }
   }
}
