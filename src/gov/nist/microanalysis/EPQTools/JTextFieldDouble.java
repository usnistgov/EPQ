package gov.nist.microanalysis.EPQTools;

import java.awt.Color;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;

import javax.swing.JTextField;

import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;

public class JTextFieldDouble
   extends JTextField {

   private NumberFormat mFormat;
   private final NumberFormat mParser = NumberFormat.getInstance();

   private static final long serialVersionUID = -8159950557000745051L;
   private String mText;
   private boolean mModified;
   private double mValue;
   private double mMin;
   private double mMax;
   private String mAltValue;
   private final ArrayList<ActionListener> mValueChange = new ArrayList<ActionListener>();

   public JTextFieldDouble() {
      this(0.0, 0.0, 1.0);
   }

   public JTextFieldDouble(double value, double min, double max) {
      this(value, min, max, "0.0#");
   }

   public JTextFieldDouble(double value, double min, double max, String fmtStr) {
      super();
      mFormat = new HalfUpFormat(fmtStr);
      mMin = min;
      mMax = max;
      mText = mFormat.format(value);
      setText(mText);
      mModified = false;
      mAltValue = null;
      // Why is the focusLost called twice???
      addFocusListener(new FocusAdapter() {
         @Override
         public void focusLost(FocusEvent e) {
            if(!e.isTemporary())
               parseText(getText().trim());
         }

         @Override
         public void focusGained(FocusEvent e) {
            selectAll();
         }

      });
      setValue(value);
      setToolTipText("A value in the range " + mFormat.format(mMin) + " to " + mFormat.format(mMax) + ".");
   }

   public JTextFieldDouble(double value, double min, double max, String fmtStr, String altValue) {
      this(value, min, max, fmtStr);
      setAlternativeValue(altValue, true);
   }

   private void parseText(final String textVal) {
      if((mAltValue != null) && textVal.equalsIgnoreCase(mAltValue)) {
         if(!Double.isNaN(mValue)) {
            mValue = Double.NaN;
            updateAltValue();
            fireValueChange();
         }
      } else {
         try {
            double val = mParser.parse(textVal).doubleValue();
            if((val < mMin) || (val > mMax)) {
               val = mValue;
               JTextFieldDouble.super.setText(mFormat.format(mValue));
               setBackground(Color.yellow);
               Toolkit.getDefaultToolkit().beep();
            } else
               setBackground(SystemColor.text);
            if(val != mValue) {
               mValue = val;
               mModified = true;
               mText = getText();
               fireValueChange();
            }
         }
         catch(final ParseException ex) {
            JTextFieldDouble.super.setText(mText);
            setBackground(Color.pink);
            Toolkit.getDefaultToolkit().beep();
         }
      }
   }

   public void setAlternativeValue(String val, boolean update) {
      mAltValue = val;
      setToolTipText("A value in the range " + mFormat.format(mMin) + " to " + mFormat.format(mMax) + " or \"" + val + "\".");
      if(update)
         updateAltValue();
   }

   public void updateAltValue() {
      mValue = Double.NaN;
      JTextFieldDouble.super.setText(mAltValue);
      setBackground(SystemColor.text);
   }

   public void setNumberFormat(NumberFormat nf) {
      final double d = getValue();
      mFormat = nf;
      setValue(d);
   }

   public void initialize(double value, double min, double max) {
      mMin = min;
      mMax = max;
      setValue(value);
   }

   public double getValue() {
      return mValue;
   }

   public boolean isAltValue() {
      return Double.isNaN(mValue);
   }

   public void setValue(double val) {
      if(Double.isNaN(val) && (mAltValue != null)) {
         updateAltValue();
      } else {
         mValue = Math2.bound(val, mMin, mMax);
         JTextFieldDouble.super.setText(mFormat.format(mValue));
         mModified = false;
      }
   }

   public boolean isModified() {
      return mModified;
   }

   public void setModified(boolean b) {
      mModified = b;
   }

   @Override
   public void setText(String str) {
      parseText(str);
   }

   public void addValueChange(ActionListener al) {
      mValueChange.add(al);
   }

   public void removeValueChange(ActionListener al) {
      mValueChange.remove(al);
   }

   public void clearValueChange() {
      mValueChange.clear();
   }

   private void fireValueChange() {
      ActionEvent ae = new ActionEvent(JTextFieldDouble.this, 0, "ValueChange", System.currentTimeMillis(), 0);
      ArrayList<ActionListener> dup = new ArrayList<ActionListener>(mValueChange);
      for(ActionListener al : dup)
         al.actionPerformed(ae);
   }

}