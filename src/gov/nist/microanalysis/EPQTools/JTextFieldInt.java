package gov.nist.microanalysis.EPQTools;

import java.awt.Color;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JTextField;

public class JTextFieldInt
   extends JTextField {
   private static final long serialVersionUID = 1162086827873413519L;
   private String mText;
   private boolean mModified;
   private int mValue;
   private final int mMin;
   private final int mMax;

   public JTextFieldInt(int value, int min, int max) {
      super();
      mText = Integer.toString(value);
      setText(mText);
      mValue = value;
      mModified = false;
      mMin = min;
      mMax = max;

      addFocusListener(new FocusAdapter() {
         @Override
         public void focusLost(FocusEvent e) {
            if(!e.isTemporary())
               try {
                  int val = Integer.parseInt(getText().trim());
                  if((val < mMin) || (val > mMax)) {
                     val = mValue;
                     JTextFieldInt.super.setText(Integer.toString(mValue));
                     setBackground(Color.yellow);
                     Toolkit.getDefaultToolkit().beep();
                  } else
                     setBackground(SystemColor.text);
                  if(val != mValue) {
                     mValue = val;
                     mModified = true;
                     mText = getText();
                  }
               }
               catch(final NumberFormatException ex) {
                  JTextFieldInt.super.setText(mText);
                  setBackground(Color.pink);
                  Toolkit.getDefaultToolkit().beep();
               }
         }

         @Override
         public void focusGained(FocusEvent e) {
            selectAll();
         }
      });

   }

   public int getValue() {
      return mValue;
   }

   public void setValue(int val) {
      mValue = val;
      JTextFieldInt.super.setText(Integer.toString(mValue));
      mModified = false;
   }

   @Override
   public void setText(String str) {
      setValue(Integer.parseInt(str));
   }

   public boolean isModified() {
      return mModified;
   }

   public void setModified(boolean b) {
      mModified = b;
   }
}