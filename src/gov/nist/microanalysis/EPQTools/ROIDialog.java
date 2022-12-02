package gov.nist.microanalysis.EPQTools;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * A simple dialog for entering ROI as a range of energies.
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
public class ROIDialog extends JDialog {
   private static final long serialVersionUID = 3467983273597958202L;

   private JTextField mMinField;
   private JTextField mMaxField;
   private JButton mOkButton;
   private JButton mZoomButton;
   private double mMinLimit = 0.0;
   private double mMaxLimit = 20.0;

   private boolean mIsOk;
   private boolean mIsZoom;

   private JLabel jLabel_Message;

   /**
    * Constructs a ROIDialog
    * 
    * @param owner
    * @param title
    * @param modal
    * @throws HeadlessException
    */
   public ROIDialog(Frame owner, String title, boolean modal) throws HeadlessException {
      super(owner, title, modal);
      try {
         initialize();
         pack();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a ROIDialog
    * 
    * @param owner
    * @param title
    * @param modal
    * @throws HeadlessException
    */
   public ROIDialog(Dialog owner, String title, boolean modal) throws HeadlessException {
      super(owner, title, modal);
      try {
         initialize();
         pack();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Limit the range of energies which may be entered to between min and max
    * (in eV)
    * 
    * @param min
    * @param max
    */
   public void setLimits(double min, double max) {
      mMinLimit = min / 1000.0;
      mMaxLimit = max / 1000.0;
      if (jLabel_Message != null) {
         final NumberFormat nf = new HalfUpFormat("0.0");
         jLabel_Message.setText("Limits = [" + nf.format(mMinLimit) + " keV, " + nf.format(mMaxLimit) + " keV]");
      }
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
            mIsOk = false;
            ROIDialog.this.setVisible(false);
         }
      };
      getRootPane().getActionMap().put(CANCEL_ACTION_KEY, cancelAction);
   }

   private void initialize() throws Exception {

      setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

      mIsOk = false;
      mIsZoom = false;
      final FormLayout fl = new FormLayout("50dlu, right:pref, 5dlu, 30dlu, 5dlu, pref, 40dlu, 10dlu",
            "5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu");
      setLayout(fl);
      final CellConstraints cc = new CellConstraints();
      add(new JLabel("Minimum energy"), cc.xy(2, 2));
      final NumberFormat nf = new HalfUpFormat("0.00");
      mMinField = new JTextField(nf.format((mMinLimit + mMaxLimit) / 10));
      final FocusListener fe = new FocusListener() {
         @Override
         public void focusGained(FocusEvent e) {
            final JTextField tf = (JTextField) e.getSource();
            tf.selectAll();
         }

         @Override
         public void focusLost(FocusEvent e) {
            final JTextField jf = (JTextField) e.getSource();
            final NumberFormat nf = NumberFormat.getInstance();
            try {
               final double val = nf.parse(jf.getText()).doubleValue();
               if ((val < mMinLimit) || (val > mMaxLimit))
                  throw new ParseException(jf.getText(), 0);
               jf.setBackground(SystemColor.window);
               mOkButton.setEnabled(true);
            } catch (final ParseException pe) {
               jf.selectAll();
               jf.setBackground(Color.PINK);
               mOkButton.setEnabled(false);
            }
         }
      };
      mMinField.addFocusListener(fe);
      add(mMinField, cc.xy(4, 2));
      add(new JLabel("keV"), cc.xy(6, 2));
      add(new JLabel("Maximum energy"), cc.xy(2, 4));
      mMaxField = new JTextField(nf.format((mMinLimit + mMaxLimit) / 5));
      mMaxField.addFocusListener(fe);
      add(mMaxField, cc.xy(4, 4));
      add(new JLabel("keV"), cc.xy(6, 4));
      mZoomButton = new JButton("Zoom");
      mZoomButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent arg0) {
            mIsZoom = true;
            mIsOk = true;
            ROIDialog.this.setVisible(false);
         }
      });
      mOkButton = new JButton("Ok");
      getRootPane().setDefaultButton(mOkButton);
      mOkButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mIsOk = true;
            ROIDialog.this.setVisible(false);
         }
      });
      final JButton cancelBtn = new JButton("Cancel");
      cancelBtn.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mIsOk = false;
            ROIDialog.this.setVisible(false);
         }
      });
      jLabel_Message = new JLabel();
      add(jLabel_Message, cc.xyw(2, 6, 4));
      setLimits(1000.0 * mMinLimit, 1000.0 * mMaxLimit);
      addCancelByEscapeKey();
      final ButtonBarBuilder bbb = new ButtonBarBuilder();
      bbb.addButton(mZoomButton);
      bbb.addUnrelatedGap();
      bbb.addButton(mOkButton, cancelBtn);
      add(bbb.build(), cc.xyw(2, 8, 6, CellConstraints.RIGHT, CellConstraints.DEFAULT));
      setResizable(false);
   }

   /**
    * Did the user exit the dialog by selecting Ok and are the returned values
    * in range. Does not check whether they are equal.
    * 
    * @return boolean
    */
   public boolean isOk() {
      return mIsOk && (!Double.isNaN(parse(mMinField)) && (!Double.isNaN(parse(mMaxField))));
   }

   public boolean isZoom() {
      return mIsZoom && (!Double.isNaN(parse(mMinField)) && (!Double.isNaN(parse(mMaxField))));
   }

   private double parse(JTextField field) {
      final NumberFormat nf = NumberFormat.getInstance();
      double res = Double.NaN;
      try {
         res = nf.parse(field.getText()).doubleValue();
         if ((res < mMinLimit) || (res > mMaxLimit))
            res = Double.NaN;
      } catch (final ParseException pe) {
         res = Double.NaN;
      }
      return res;
   }

   /**
    * Returns the energy range defined by the user in eV.
    * 
    * @return double[2] - The min and max energies in eV (res[0]&lt;=res[1])
    */
   public double[] getRange() {
      final double[] res = new double[2];
      res[0] = 1000.0 * parse(mMinField);
      res[1] = 1000.0 * parse(mMaxField);
      if (res[0] > res[1]) {
         final double s = res[0];
         res[0] = res[1];
         res[1] = s;
      }
      return res;
   }

}
