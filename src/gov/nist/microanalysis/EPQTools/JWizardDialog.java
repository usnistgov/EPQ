package gov.nist.microanalysis.EPQTools;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * <p>
 * A container for implementing a wizard-style multi-step interface. The wizard
 * contains a region of 350dlu by 150 dlu to be filled by a JWizardPanel. It is
 * recommended that the filling of the JWizardPanel start 15dlu in from the left
 * edge.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas
 * @version 1.0
 */
public class JWizardDialog
   extends
   JDialog {
   /**
    * 
    */
   private static final long serialVersionUID = 7716307775683470014L;
   // Controls
   private final JLabel jLabel_Banner = new JLabel();
   private final JLabel jLabel_PrevLabel = new JLabel();
   private final JLabel jLabel_NextLabel = new JLabel();
   private final JPanel jPanel_Main = new JPanel();
   private final JPanel jPanel_Contents = new JPanel(new FormLayout("center:380dlu", "center:160dlu"));
   private final JButton jButton_Back = new JButton("Back");
   private final JButton jButton_Next = new JButton("Next");
   private final JButton jButton_Cancel = new JButton("Cancel");
   private final JButton jButton_Finish = new JButton("Finish");
   private final JLabel jLabel_ErrorLabel = new JLabel();
   private final JButton jButton_ErrorBtn = new JButton("Details...");
   private JLabel jLabel_Icon = new JLabel();
   // Status information
   private final ArrayList<JWizardPanel> mPreviousPanels = new ArrayList<JWizardPanel>();
   private final ArrayList<String> mPreviousBanners = new ArrayList<String>();
   private JWizardPanel jWizardPanel_Active = null;
   private JWizardPanel jWizardPanel_Next = null;
   private String mNextBanner = null;
   private boolean mFinished = false;
   private boolean mCancelled = false;
   private String mDialogMsg = "";
   private final ArrayList<String> mLongErrorMsg = new ArrayList<String>();

   public static final int FINISHED = 1;
   public static final int CANCELLED = 2;
   // Results
   private final Map<String, Object> mResults = new TreeMap<String, Object>();

   public JWizardDialog()
         throws HeadlessException {
      super();
      try {
         wizInitialize();
         pack();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   public JWizardDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
      super(owner, title, modal, gc);
      try {
         wizInitialize();
         pack();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   public JWizardDialog(Frame owner, String title, boolean modal) {
      super(owner, title, modal);
      try {
         wizInitialize();
         pack();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   public JWizardDialog(Frame owner, String title) {
      this(owner, title, false);
   }

   public JWizardDialog(Frame owner, boolean modal) {
      this(owner, "Trixy wizard...", modal);
   }

   public JWizardDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) {
      super(owner, title, modal, gc);
      try {
         wizInitialize();
         pack();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   public JWizardDialog(Dialog owner, String title, boolean modal) {
      super(owner, title, modal);
      try {
         wizInitialize();
         pack();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   public JWizardDialog(Dialog owner, String title) {
      this(owner, title, false);
   }

   public JWizardDialog(Dialog owner, boolean modal) {
      this(owner, "Trixy wizard...", modal);
   }

   private void wizInitialize()
         throws Exception {
      setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      final LayoutManager layout = new FormLayout("8dlu, 400dlu, 8dlu", "8dlu, pref, 8dlu, 170dlu, 8dlu, pref, 8dlu, pref, 8dlu");
      jPanel_Main.setLayout(layout);
      {
         createTop();
      }

      jButton_Back.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            assert (jWizardPanel_Active != null);
            assert (mPreviousPanels.size() > 0);
            assert (mPreviousPanels.size() == mPreviousBanners.size());
            if(jWizardPanel_Active != null) {
               jPanel_Contents.remove(jWizardPanel_Active);
               setNextPanel(jWizardPanel_Active, jLabel_Banner.getText());
            }
            final JWizardPanel panel = mPreviousPanels.remove(mPreviousPanels.size() - 1);
            final String banner = mPreviousBanners.remove(mPreviousBanners.size() - 1);
            setActivePanel(panel, banner);
            jButton_Back.setEnabled(mPreviousPanels.size() > 0);
         }
      });

      jButton_Next.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            if((jWizardPanel_Active != null) && jWizardPanel_Active.permitNext()) {
               assert (jWizardPanel_Active != null);
               assert (jWizardPanel_Next != null);
               mPreviousPanels.add(jWizardPanel_Active);
               mPreviousBanners.add(jLabel_Banner.getText());
               jPanel_Contents.remove(jWizardPanel_Active);
               final JWizardPanel next = jWizardPanel_Next;
               final String banner = mNextBanner;
               setNextPanel(null, null);
               setActivePanel(next, banner);
               jButton_Back.setEnabled(mPreviousPanels.size() > 0);
            } else
               Toolkit.getDefaultToolkit().beep();
         }
      });

      jButton_Finish.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            if(jWizardPanel_Active.permitNext()) {
               mFinished = true;
               jWizardPanel_Active.onHide();
               setVisible(false);
            }
         }
      });

      jButton_Cancel.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mFinished = false;
            mCancelled = true;
            dispose();
         }
      });
      final ButtonBarBuilder bbb = new ButtonBarBuilder();
      bbb.addGlue();
      bbb.addButton(jButton_Back, jButton_Next);
      bbb.addUnrelatedGap();
      bbb.addButton(jButton_Finish);
      bbb.addUnrelatedGap();
      bbb.addButton(jButton_Cancel);
      final JPanel btns = bbb.build();
      jButton_Next.setMnemonic(KeyEvent.VK_N);
      jButton_Back.setMnemonic(KeyEvent.VK_B);
      jButton_Cancel.setMnemonic(KeyEvent.VK_ESCAPE);

      jPanel_Main.add(btns, CC.xy(2, 8));
      setContentPane(jPanel_Main);
      setResizable(false);
      addCancelByEscapeKey();
      setForeground(SystemColor.controlText);
      setBackground(SystemColor.control);
   }

   private void addCancelByEscapeKey() {
      final String CANCEL_ACTION_KEY = "CANCEL_ACTION_KEY";
      final int noModifiers = 0;
      final KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, noModifiers, false);
      final InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
      inputMap.put(escapeKey, CANCEL_ACTION_KEY);
      final AbstractAction cancelAction = new AbstractAction() {
         private static final long serialVersionUID = 585668420885106696L;

         @Override
         public void actionPerformed(ActionEvent e) {
            mFinished = false;
            mCancelled = true;
            dispose();
         }
      };
      getRootPane().getActionMap().put(CANCEL_ACTION_KEY, cancelAction);
   }

   public boolean isFinished() {
      return mFinished && (!mCancelled);
   }

   public boolean isCancelled() {
      return mCancelled;
   }

   /**
    * Show the wizard dialog and wait for the user to complete it.
    * 
    * @return FINISHED or CANCELLED depending upon whether the Wizard was exited
    *         using the Finish button
    */
   public int showWizard() {
      setVisible(true);
      return mFinished ? FINISHED : CANCELLED;
   }

   public void setMessageText(String msg) {
      if(mLongErrorMsg.size() == 0) {
         jLabel_ErrorLabel.setForeground(SystemColor.textText);
         jLabel_ErrorLabel.setText(msg);
         jButton_ErrorBtn.setEnabled(false);
      }
   }

   public void setErrorText(String error) {
      jLabel_ErrorLabel.setForeground(Color.red);
      jLabel_ErrorLabel.setText(error);
      jButton_ErrorBtn.setEnabled(false);
   }

   public void setExceptionText(Throwable th) {
      setExceptionText(th.toString(), th);
   }

   public void setExceptionText(String shortMsg, Throwable th) {
      setExceptionText(shortMsg, shortMsg, th);
   }

   public void setExceptionText(String shortMsg, String dialogMsg, Throwable th) {
      th.printStackTrace();
      String longMsg = th.getMessage();
      if(longMsg == null) {
         final StringWriter sw = new StringWriter();
         final PrintWriter pw = new PrintWriter(sw);
         th.printStackTrace(pw);
         longMsg = sw.toString();
      }
      setExtendedError(shortMsg, dialogMsg, longMsg != null ? longMsg : "No additional information available.");
   }

   public void setExtendedError(String shortMsg, String longMsg) {
      setExtendedError(shortMsg, shortMsg, longMsg);
   }

   public void setExtendedErrors(String shortMsg, String dialogMsg, ArrayList<String> errMsgs) {
      mDialogMsg = dialogMsg != null ? dialogMsg : "Click details for more information";
      mLongErrorMsg.addAll(errMsgs);
      setErrorText(mDialogMsg);
      jButton_ErrorBtn.setEnabled(true);
      // If this method is called more than once, we must ensure that the Error
      // Button will not be called more than once with older error messages
      final ActionListener[] al = jButton_ErrorBtn.getActionListeners();
      for(int index = 0; jButton_ErrorBtn.getActionListeners().length > 0; index++)
         jButton_ErrorBtn.removeActionListener(al[index]);
      jButton_ErrorBtn.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final StringBuffer sb = new StringBuffer();
            sb.append(mLongErrorMsg.size() + " errors:");
            for(int i = 0; i < mLongErrorMsg.size(); ++i) {
               sb.append("\nError " + (i + 1) + "\n");
               sb.append(mLongErrorMsg.get(i));
            }
            ErrorDialog.createErrorMessage(JWizardDialog.this, "Extended error message", mDialogMsg, sb.toString());
         }
      });
   }

   public void setExtendedError(String errorMsg, String dialogMsg, String longMsg) {
      mDialogMsg = dialogMsg != null ? dialogMsg : "Click details for more information";
      mLongErrorMsg.add(mDialogMsg + ": " + (longMsg != null ? longMsg : "No addional information available."));
      setErrorText(errorMsg);
      jButton_ErrorBtn.setEnabled(true);
      // If this method is called more than once, we must ensure that the Error
      // Button will not be called more than once with older error messages
      final ActionListener[] al = jButton_ErrorBtn.getActionListeners();
      for(int index = 0; jButton_ErrorBtn.getActionListeners().length > 0; index++)
         jButton_ErrorBtn.removeActionListener(al[index]);
      jButton_ErrorBtn.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final StringBuffer sb = new StringBuffer();
            sb.append(mLongErrorMsg.size() + " errors:");
            for(int i = 0; i < mLongErrorMsg.size(); ++i) {
               sb.append("\nError " + (i + 1) + "\n");
               sb.append(mLongErrorMsg.get(i));
            }
            ErrorDialog.createErrorMessage(JWizardDialog.this, "Extended error message", mDialogMsg, sb.toString());
         }
      });
   }

   public void clearMessageText() {
      jLabel_ErrorLabel.setText("");
      jButton_ErrorBtn.setEnabled(false);
      mLongErrorMsg.clear();
      mDialogMsg = "";
   }

   public void centerDialog(Dialog d) {
      d.setLocationRelativeTo(this);
   }

   /**
    * Give ourselves a fresh start.
    */
   public void clearWizard() {
      jWizardPanel_Active = null;
      jLabel_Banner.setText("Nothing");
      jLabel_PrevLabel.setText("Start");
      jLabel_NextLabel.setText("Finish");
      mPreviousPanels.clear();
      mPreviousBanners.clear();
      jWizardPanel_Next = null;
      mNextBanner = "";
      jButton_Back.setEnabled(false);
      jButton_Next.setEnabled(false);
      jButton_Finish.setEnabled(true);
   }

   public void setActivePanel(JWizardPanel panel, String banner) {
      jLabel_ErrorLabel.setText("");
      if(jWizardPanel_Active != null) {
         jWizardPanel_Active.onHide();
         jPanel_Contents.remove(jWizardPanel_Active);
         jWizardPanel_Active = null;
      }
      if(panel != null) {
         jPanel_Contents.add(panel, CC.xy(1, 1));
         clearMessageText();
         panel.onShow();
         jPanel_Contents.repaint();
      }
      jWizardPanel_Active = panel;
      jLabel_Banner.setText(banner != null ? banner : "None");
      if(jWizardPanel_Next != null)
         jLabel_NextLabel.setText("<html>Next: <em>" + mNextBanner + "</em>");
      if(mPreviousBanners.size() > 0) {
         final String str = mPreviousBanners.get(mPreviousBanners.size() - 1);
         jLabel_PrevLabel.setText("<html>Previous: <em>" + str + "</em>");
      } else
         jLabel_PrevLabel.setText("First page");
      jButton_Back.setEnabled(mPreviousPanels.size() > 0);
      jButton_Next.setEnabled(jWizardPanel_Next != null);
   }

   public void setNextPanel(JWizardPanel panel, String banner) {
      jWizardPanel_Next = panel;
      mNextBanner = banner;
      if(jWizardPanel_Next != null)
         jLabel_NextLabel.setText("<html>Next: <em>" + mNextBanner + "</em>");
      else
         jLabel_NextLabel.setText("Finish");
      jButton_Next.setEnabled(jWizardPanel_Next != null);
   }

   public void enableFinish(boolean b) {
      if(jButton_Finish.isEnabled() != b) {
         jButton_Finish.setEnabled(b);
         getRootPane().setDefaultButton(jButton_Finish.isEnabled() ? jButton_Finish : null);
      }
   }

   public void enableNext(boolean b) {
      if(jButton_Next.isEnabled() != b) {
         jButton_Next.setEnabled(b);
         getRootPane().setDefaultButton(jButton_Next.isEnabled() ? jButton_Next : null);
      }
   }

   public void setBackEnabled(boolean b) {
      if(b != jButton_Back.isEnabled())
         jButton_Back.setEnabled(b);
   }

   public boolean isBackEnabled() {
      return jButton_Back.isEnabled();
   }

   private JPanel createIconPanel() {
      final JPanel header = new JPanel(new FormLayout("pref, 10dlu, default", "pref"));

      jLabel_Icon = new JLabel();
      jLabel_Icon.setIcon(new ImageIcon(JWizardDialog.class.getResource("ClipArt/alien_sm.png")));
      header.add(jLabel_Icon, CC.xy(1, 1));
      return header;
   }

   public Icon getIcon() {
      return jLabel_Icon.getIcon();
   }

   public void setIcon(Icon icon) {
      if(icon instanceof ImageIcon) {
         final ImageIcon imgIcon = (ImageIcon) icon;
         jLabel_Icon.setIcon(new ImageIcon(imgIcon.getImage().getScaledInstance(64, 59, Image.SCALE_AREA_AVERAGING)));
      } else
         jLabel_Icon.setIcon(icon);
   }

   private void createTop() {
      final JPanel header = createIconPanel();
      {
         final JPanel banner = new JPanel(new FormLayout("320dlu", "12dlu, 1dlu, 20dlu, 1dlu, 12dlu"));
         jLabel_Banner.setFont(new Font("Dialog", Font.PLAIN, 24));
         jLabel_Banner.setText("Test banner");
         banner.add(jLabel_Banner, CC.xy(1, 3));
         jLabel_PrevLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
         jLabel_PrevLabel.setText("Start ");
         banner.add(jLabel_PrevLabel, CC.xy(1, 1, CellConstraints.RIGHT, CellConstraints.CENTER));
         jLabel_NextLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
         jLabel_NextLabel.setText("Finish ");
         banner.add(jLabel_NextLabel, CC.xy(1, 5, CellConstraints.RIGHT, CellConstraints.CENTER));
         header.add(banner, CC.xy(3, 1));
      }
      jPanel_Contents.setBorder(createPanelBorder());
      jPanel_Main.add(jPanel_Contents, CC.xy(2, 4));
      header.setBorder(createPanelBorder());
      jPanel_Main.add(header, CC.xy(2, 2));
      {
         final JPanel msgPanel = new JPanel(new FormLayout("5dlu, right:40dlu, 5dlu, 260dlu, 5dlu, pref, 5dlu", "pref"));
         msgPanel.setBorder(createPanelBorder());
         msgPanel.add(new JLabel("Message:"), CC.xy(2, 1));
         msgPanel.add(jLabel_ErrorLabel, CC.xy(4, 1));
         msgPanel.add(jButton_ErrorBtn, CC.xy(6, 1));
         jPanel_Main.add(msgPanel, CC.xy(2, 6));
      }
   }

   private CompoundBorder createPanelBorder() {
      final Border line = SwingUtils.createDefaultBorder();
      final Border empty = BorderFactory.createEmptyBorder(8, 8, 8, 8);
      return BorderFactory.createCompoundBorder(line, empty);
   }

   /**
    * getResults and setResults provides a generic mechanism to pass information
    * to and from JWizardPanel(s) and the consumer of the WizardDialog. This is
    * useful for passing configuration information extracted from the panel back
    * to the program that acts on it.
    * 
    * @param name A unique string
    * @return An Object containing configuration information
    */
   public Object getResult(String name) {
      return mResults.get(name);
   }

   /**
    * getResults and setResults provides a generic mechanism to pass information
    * to and from JWizardPanel(s) and the consumer of the WizardDialog. This is
    * useful for passing configuration information extracted from the panel back
    * to the program that acts on it.
    * 
    * @param name A unique string
    * @param result An Object containing configuration information
    */
   public void setResult(String name, Object result) {
      mResults.put(name, result);
   }

   static public class JWizardPanel
      extends
      JPanel {

      protected static final long serialVersionUID = 0x42;
      private final JWizardDialog mWizard;

      /**
       * Constructs a JWizardPanel associated with the specified WizardDialog
       * 
       * @param wiz
       */
      public JWizardPanel(JWizardDialog wiz) {
         super();
         assert (wiz != null);
         mWizard = wiz;
      }

      /**
       * Constructs a JWizardPanel associated with the specified WizardDialog
       * and using the specified LayoutManager
       * 
       * @param lo
       * @param wiz
       */
      public JWizardPanel(JWizardDialog wiz, LayoutManager lo) {
         super(lo);
         assert (wiz != null);
         mWizard = wiz;
      }

      /**
       * Returns the instance of WizardDialog with which this JWizardPanel is
       * associated.
       * 
       * @return WizardDialog
       */
      public JWizardDialog getWizard() {
         assert (mWizard != null);
         return mWizard;
      }

      /**
       * Implement the onShow to perform some action when this panel is
       * displayed in the WizardDialog.
       */
      public void onShow() {
         // Don't do anything by default
      }

      /**
       * Implement the onHide to perform some action when this panel is hidden
       * by the WizardDialog.
       */
      public void onHide() {
         // Don't do anything by default
      }

      /**
       * Called when the user presses the next or finish button. If the panel
       * does not want to loose focus then this function should be overriden to
       * return false. This mechanism allows the panel to veto the next/finished
       * button to enforce data verification.
       * 
       * @return true to permit moving to the next panel or false otherwise.
       */
      public boolean permitNext() {
         return true;
      }

      /**
       * getResults and setResults provides a generic mechanism to pass
       * information to and from JWizardPanel(s) and the consumer of the
       * WizardDialog. This is useful for passing configuration information
       * extracted from the panel back to the program that acts on it.
       * 
       * @param name A unique string
       * @param res An Object containing configuration information
       */
      protected void setResult(String name, Object res) {
         mWizard.setResult(name, res);
      }

      /**
       * getResults and setResults provides a generic mechanism to pass
       * information to and from JWizardPanel(s) and the consumer of the
       * WizardDialog. This is useful for passing configuration information
       * extracted from the panel back to the program that acts on it.
       * 
       * @param name
       * @return Object
       */
      protected Object getResult(String name) {
         return mWizard.getResult(name);
      }

      
      public void addInScrollPane(JPanel jp) {
		JPanel base = new JPanel();
		base.setLayout(new FormLayout("300dlu", "155dlu"));
		base.add(new JScrollPane(jp), CC.xy(1, 1));
		add(base);
      }
   }

   abstract public class JProgressPanel
      extends
      JWizardPanel {

      private static final long serialVersionUID = -2510365605502237127L;
      private JProgressBar mProgressBar;

      /**
       * Constructs a ProgressDialog
       * 
       * @param wiz
       */
      public JProgressPanel(JWizardDialog wiz) {
         super(wiz);
         try {
            initialize();
         }
         catch(final Exception ex) {
            ex.printStackTrace();
         }
      }

      private void initialize()
            throws Exception {
         final PanelBuilder builder = new PanelBuilder(new FormLayout("250dlu", "pref, 10dlu, pref"));
         builder.addSeparator("Progress", CC.xy(1, 1));
         mProgressBar = new JProgressBar(SwingConstants.HORIZONTAL, 0, 100);
         builder.add(mProgressBar, CC.xy(1, 3));
         add(builder.getPanel());
      }

      @Override
      public boolean permitNext() {
         return true;
      }

      public void setProgress(int prog) {
         mProgressBar.getModel().setValue(prog);
      }
   }
}
