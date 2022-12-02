package gov.nist.microanalysis.EPQTools;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * <p>
 * A static class for displaying useful error messages to the user
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Daniel "Ooblioob" Davis
 * @version 1.0
 */
public class ErrorDialog {

   /**
    * Launches a user-friendly dialog to display an exception message. This
    * function is safe to use either in the event dispatch thread or in a worker
    * thread. In either case, the code will be executed in the event dispatch
    * thread as Swing requires.
    * 
    * @param parent
    *           Component
    * @param title
    *           Dialog box title
    * @param e
    *           The Exception or RuntimeException or Error to display
    */
   public static void createErrorMessage(Component parent, String title, Throwable e) {
      String stack;
      {
         final StringWriter sw = new StringWriter();
         try (final PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
            stack = sw.toString();
         }

      }
      final StringBuffer sb = new StringBuffer();
      sb.append(e.toString() + "<br>");
      int begin = 0, end = stack.indexOf("\n", 0);
      while (end >= 0) {
         assert begin >= 0;
         assert end >= 0;
         sb.append(stack.substring(begin, end));
         sb.append("<br>");
         begin = end + 1;
         end = stack.indexOf("\n", begin);
      }
      sb.append(stack.substring(begin));
      sb.append("<br>(NOTE: This message provides information that may be useful to the programmer to resolve bugs.)");
      createErrorMessage(parent, title, e.toString(), sb.toString());
   }

   /**
    * Launches a user-friendly dialog with an appropriate error message. This
    * function is safe to use either in the event dispatch thread or in a worker
    * thread. In either case, the code will be executed in the event dispatch
    * thread as Swing requires.
    * 
    * @param parent
    *           Component
    * @param title
    *           Dialog box title
    * @param shortMessage
    *           A short message to display by default
    * @param longMessage
    *           A long message to display at the users request
    */
   public static void createErrorMessage(Component parent, String title, String shortMessage, String longMessage) {

      class doErrorMsg implements Runnable {
         Component mParent;
         String mTitle;
         String mShortMessage;
         String mLongMessage;

         private String toHTML(String str) {
            return str.replace("\n", "<br>");
         }

         doErrorMsg(Component parent, String title, String shortMessage, String longMessage) {
            mParent = parent;
            mTitle = title;
            mShortMessage = toHTML(shortMessage);
            mLongMessage = toHTML(longMessage);
         }

         @Override
         public void run() {
            final JPanel panel = new JPanel(new FormLayout("3dlu, 64, 10dlu, 300dlu, 3dlu", "3dlu, p, 64, p, 25dlu, min(200dlu;pref), 3dlu"));

            final JFrame frame = null;
            final JDialog dialog = new JDialog(frame, mTitle, true);
            dialog.setResizable(false);
            final CellConstraints cc = new CellConstraints();
            final JLabel label = new JLabel("Testing");

            ImageIcon icon = new ImageIcon(ErrorDialog.class.getResource("ClipArt/Error.png"));
            Image image = icon.getImage();
            image = image.getScaledInstance(64, 64, Image.SCALE_AREA_AVERAGING);
            icon = new ImageIcon(image);
            label.setIcon(icon);

            final JLabel detailsLabel = new JLabel("");
            final JPanel scrollPanel = new JPanel(new FormLayout("3dlu, p, 3dlu", "2dlu, p, 2dlu"));
            scrollPanel.add(detailsLabel, cc.xy(2, 2));

            final JScrollPane scroll = new JScrollPane(scrollPanel);
            scroll.setPreferredSize(new Dimension(10, 120));
            scroll.setVisible(false);

            final JButton okButton = new JButton("Ok");
            class okAction implements ActionListener {
               private final JDialog mDialog;

               public okAction(JDialog dialog) {
                  mDialog = dialog;
               }

               @Override
               public void actionPerformed(ActionEvent e) {
                  mDialog.setVisible(false);
               }
            }
            okButton.addActionListener(new okAction(dialog));
            dialog.getRootPane().setDefaultButton(okButton);

            final JButton detailsButton = new JButton("Details >>");
            class detailsAction implements ActionListener {
               private final String mText;
               private final JLabel mLabel;
               private boolean mPolarity;
               private final JDialog mDialog;
               private final JScrollPane mScroll;
               private final JButton mButton;

               public detailsAction(String text, JLabel label, JDialog dialog, JScrollPane scroll, JButton button) {
                  mText = text;
                  mLabel = label;
                  mDialog = dialog;
                  mScroll = scroll;
                  mButton = button;
                  setDetailsVisible(true);
               }

               @Override
               public void actionPerformed(ActionEvent e) {
                  setDetailsVisible(!mPolarity);
               }

               private void setDetailsVisible(boolean b) {
                  mPolarity = b;
                  if (mPolarity) {
                     mLabel.setText("<html>" + mText + "</html>");
                     mButton.setText("Details <<");
                  } else {
                     mLabel.setText("");
                     mButton.setText("Details >>");
                  }
                  mScroll.setVisible(mPolarity);
                  mDialog.pack();
               }

            }

            detailsButton.addActionListener(new detailsAction(mLongMessage, detailsLabel, dialog, scroll, detailsButton));

            panel.add(label, cc.xy(2, 3));
            panel.add(new JLabel("<html>" + mShortMessage + "</html>"), cc.xywh(4, 2, 1, 3));
            final ButtonBarBuilder bbb = new ButtonBarBuilder();
            bbb.addGlue();
            bbb.addButton(okButton, detailsButton);
            panel.add(bbb.build(), cc.xyw(2, 5, 3));
            panel.add(scroll, cc.xyw(2, 6, 3));
            dialog.setContentPane(panel);

            dialog.pack();
            dialog.setLocationRelativeTo(mParent);
            dialog.setVisible(true);
         }
      }
      try {
         final Runnable doIt = new doErrorMsg(parent, title, shortMessage, longMessage);
         if (!SwingUtilities.isEventDispatchThread())
            SwingUtilities.invokeAndWait(doIt);
         else
            doIt.run();
      } catch (final Exception e) {
         e.printStackTrace();
      }
   }
}
