package gov.nist.microanalysis.Utility;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.table.TableCellEditor;

/*
 * This work is hereby released into the Public Domain by Orbital Computer. To
 * view a copy of the public domain dedication, visit
 * http://creativecommons.org/licenses/publicdomain/ Minor modifications by NWMR
 * which are not subject to copyright.
 */
public class ComboBoxCellEditor extends AbstractCellEditor implements ActionListener, TableCellEditor, Serializable {

   static private final long serialVersionUID = 0x98de238a349L;

   private final JComboBox<String> mComboBox;

   public ComboBoxCellEditor(JComboBox<String> comboBox) {
      mComboBox = comboBox;
      mComboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
      // hitting enter in the combo box should stop cellediting (see below)
      mComboBox.addActionListener(this);
      // remove the editor's border - the cell itself already has one
      ((JComponent) comboBox.getEditor().getEditorComponent()).setBorder(null);
   }

   private void setValue(Object value) {
      mComboBox.setSelectedItem(value);
   }

   // Implementing ActionListener
   @Override
   public void actionPerformed(java.awt.event.ActionEvent e) {
      // Selecting an item results in an actioncommand "comboBoxChanged".
      // We should ignore these ones.

      // Hitting enter results in an actioncommand "comboBoxEdited"
      if (e.getActionCommand().equals("comboBoxEdited"))
         stopCellEditing();
   }

   // Implementing CellEditor
   @Override
   public Object getCellEditorValue() {
      return mComboBox.getSelectedItem();
   }

   @Override
   public boolean stopCellEditing() {
      if (mComboBox.isEditable())
         // Notify the combo box that editing has stopped (e.g. User pressed F2)
         mComboBox.actionPerformed(new ActionEvent(this, 0, ""));
      fireEditingStopped();
      return true;
   }

   // Implementing TableCellEditor
   @Override
   public java.awt.Component getTableCellEditorComponent(javax.swing.JTable table, Object value, boolean isSelected, int row, int column) {
      setValue(value);
      return mComboBox;
   }
}