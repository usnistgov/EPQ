package gov.nist.microanalysis.Utility;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

/*
 * This work is hereby released into the Public Domain by Orbital Computer. To
 * view a copy of the public domain dedication, visit
 * http://creativecommons.org/licenses/publicdomain/ Minor modifications by NWMR
 * which are not subject to copyright.
 */
/**
 * AutoComplete provides a mechanism to modify the standard JComboBox to
 * implement auto-completion.
 */
public class AutoComplete<T> extends PlainDocument {
   static private final long serialVersionUID = 0x8691269dc273aL;
   JComboBox<? extends Object> mComboBox;
   ComboBoxModel<? extends Object> mModel;
   JTextComponent mEditor;
   // flag to indicate if setSelectedItem has been called
   // subsequent calls to remove/insertString should be ignored
   boolean mSelecting = false;
   boolean mHidePopupOnFocusLoss;
   boolean mHitBackspace = false;
   boolean mHitBackspaceOnSelection;

   KeyListener mEditorKeyListener;
   FocusListener mEditorFocusListener;

   /**
    * Constructs a AutoComplete. You should consider using enable(...) instead.
    * 
    * @param comboBox
    *           JComboBox
    */
   public AutoComplete(final JComboBox<? extends Object> comboBox) {
      mComboBox = comboBox;
      mModel = comboBox.getModel();
      comboBox.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            if (!mSelecting)
               highlightCompletedText(0);
         }
      });
      comboBox.addPropertyChangeListener(new PropertyChangeListener() {
         @Override
         @SuppressWarnings("unchecked")
         public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName().equals("editor"))
               configureEditor((ComboBoxEditor) e.getNewValue());
            if (e.getPropertyName().equals("model"))
               mModel = (ComboBoxModel<T>) e.getNewValue();
         }
      });
      mEditorKeyListener = new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            if (comboBox.isDisplayable())
               comboBox.setPopupVisible(true);
            mHitBackspace = false;
            switch (e.getKeyCode()) {
               // determine if the pressed key is backspace (needed by the
               // remove method)
               case KeyEvent.VK_BACK_SPACE :
                  mHitBackspace = true;
                  mHitBackspaceOnSelection = mEditor.getSelectionStart() != mEditor.getSelectionEnd();
                  break;
               // ignore delete key
               case KeyEvent.VK_DELETE :
                  e.consume();
                  comboBox.getToolkit().beep();
                  break;
            }
         }
      };
      // Bug 5100422 on Java 1.5: Editable JComboBox won't hide popup when
      // tabbing out
      mHidePopupOnFocusLoss = System.getProperty("java.version").startsWith("1.5");
      // Highlight whole text when gaining focus
      mEditorFocusListener = new FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            highlightCompletedText(0);
         }

         @Override
         public void focusLost(FocusEvent e) {
            // Workaround for Bug 5100422 - Hide Popup on focus loss
            if (mHidePopupOnFocusLoss)
               comboBox.setPopupVisible(false);
         }
      };
      configureEditor(comboBox.getEditor());
      // Handle initially selected object
      final Object selected = comboBox.getSelectedItem();
      if (selected != null)
         setText(selected.toString());
      highlightCompletedText(0);
   }

   /**
    * This is the primary method. Usage
    * <code>AutoComplete.enable(comboBox)</code> to enable auto-completion on
    * the specified JComboBox. If the JComboBox is to be part of a JTable use
    * ComboBoxCellEditor to wrap the JComboBox instead of DefaultCellEditor.
    * 
    * @param comboBox
    *           A JComboBox
    */
   public static void enable(JComboBox<? extends Object> comboBox) {
      // has to be editable
      comboBox.setEditable(true);
      // change the editor's document
      new AutoComplete<Object>(comboBox);
   }

   void configureEditor(ComboBoxEditor newEditor) {
      if (mEditor != null) {
         mEditor.removeKeyListener(mEditorKeyListener);
         mEditor.removeFocusListener(mEditorFocusListener);
      }

      if (newEditor != null) {
         mEditor = (JTextComponent) newEditor.getEditorComponent();
         mEditor.addKeyListener(mEditorKeyListener);
         mEditor.addFocusListener(mEditorFocusListener);
         mEditor.setDocument(this);
      }
   }

   @Override
   public void remove(int offs, int len) throws BadLocationException {
      // return immediately when selecting an item
      if (mSelecting)
         return;
      if (mHitBackspace) {
         // user hit backspace => move the selection backwards
         // old item keeps being selected
         if (offs > 0) {
            if (mHitBackspaceOnSelection)
               offs--;
         } else
            // User hit backspace with the cursor positioned on the start =>
            // beep
            mComboBox.getToolkit().beep(); // when available use:
         // UIManager.getLookAndFeel().provideErrorFeedback(comboBox);
         highlightCompletedText(offs);
      } else
         super.remove(offs, len);
   }

   @Override
   public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
      // return immediately when selecting an item
      if (mSelecting)
         return;
      // insert the string into the document
      super.insertString(offs, str, a);
      // lookup and select a matching item
      Object item = lookupItem(getText(0, getLength()));
      if (item != null)
         setSelectedItem(item);
      else {
         // keep old item selected if there is no match
         item = mComboBox.getSelectedItem();
         // imitate no insert (later on offs will be incremented by
         // str.length(): selection won't move forward)
         offs = offs - str.length();
         // provide feedback to the user that his input has been received but
         // can not be accepted
         mComboBox.getToolkit().beep(); // when available use:
         // UIManager.getLookAndFeel().provideErrorFeedback(comboBox);
      }
      setText(item.toString());
      // select the completed part
      highlightCompletedText(offs + str.length());
   }

   private void setText(String text) {
      try {
         // remove all text and insert the completed string
         super.remove(0, getLength());
         super.insertString(0, text, null);
      } catch (final BadLocationException e) {
         throw new RuntimeException(e.toString());
      }
   }

   private void highlightCompletedText(int start) {
      mEditor.setCaretPosition(getLength());
      mEditor.moveCaretPosition(start);
   }

   private void setSelectedItem(Object item) {
      mSelecting = true;
      mModel.setSelectedItem(item);
      mSelecting = false;
   }

   private Object lookupItem(String pattern) {
      final Object selectedItem = mModel.getSelectedItem();
      // only search for a different item if the currently selected does not
      // match
      if ((selectedItem != null) && startsWithIgnoreCase(selectedItem.toString(), pattern))
         return selectedItem;
      else
         // iterate over all items
         for (int i = 0, n = mModel.getSize(); i < n; i++) {
            final Object currentItem = mModel.getElementAt(i);
            // current item starts with the pattern?
            if ((currentItem != null) && startsWithIgnoreCase(currentItem.toString(), pattern))
               return currentItem;
         }
      // no item starts with the pattern => return null
      return null;
   }

   // checks if str1 starts with str2 - ignores case
   private boolean startsWithIgnoreCase(String str1, String str2) {
      return str1.toUpperCase().startsWith(str2.toUpperCase());
   }
}
