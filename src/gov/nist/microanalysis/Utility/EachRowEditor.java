package gov.nist.microanalysis.Utility;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Hashtable;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;

/**
 * <p>
 * Title: gov.nist.microanalysis.Utility.EachRowEditor.java
 * </p>
 * <p>
 * Description: Originally from
 * http://www.objects.com.au:8088/objects/java/examples
 * /src/table/EachRowEditor.java Creates a custom cell editor that can be
 * defined on row by row basis. Many thanks to Nobuo Tamemasa for writing this
 * code and sharing it.
 * </p>
 * 
 * @author Nobuo Tamemasa
 * @version 1.0
 */

public class EachRowEditor
   implements TableCellEditor {
   private final Hashtable<Integer, TableCellEditor> mEditors;
   private TableCellEditor mEditor;
   private TableCellEditor mDefaultEditor;
   private final JTable mTable;

   /**
    * Constructs a EachRowEditor. create default editor
    * 
    * @param table JTable
    * @see TableCellEditor
    * @see DefaultCellEditor
    */
   public EachRowEditor(JTable table) {
      this.mTable = table;
      mEditors = new Hashtable<Integer, TableCellEditor>();
      mDefaultEditor = new DefaultCellEditor(new JTextField());
   }

   public void setDefaultEditor(JComboBox<?> jcb) {
      mDefaultEditor = new DefaultCellEditor(jcb);
   }

   public void setDefaultEditor(JCheckBox jcb) {
      mDefaultEditor = new DefaultCellEditor(jcb);
   }

   public void setDefaultEditor(JTextField jcb) {
      mDefaultEditor = new DefaultCellEditor(jcb);
   }

   /**
    * Specify which editor to use for which row...
    * 
    * @param row The index of the table row
    * @param editor The editor object to use
    */
   public void setEditorAt(int row, TableCellEditor editor) {
      mEditors.put(Integer.valueOf(row), editor);
   }

   public TableCellEditor getEditorAt(int row) {
      TableCellEditor res = mEditors.get(Integer.valueOf(row));
      if(res == null)
         res = mDefaultEditor;
      return res;
   }

   public void removeEditor(int row) {
      mEditors.remove(Integer.valueOf(row));
   }

   @Override
   public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      return mEditor.getTableCellEditorComponent(table, value, isSelected, row, column);
   }

   @Override
   public Object getCellEditorValue() {
      return mEditor.getCellEditorValue();
   }

   @Override
   public boolean stopCellEditing() {
      return mEditor.stopCellEditing();
   }

   @Override
   public void cancelCellEditing() {
      mEditor.cancelCellEditing();
   }

   @Override
   public boolean isCellEditable(EventObject anEvent) {
      selectEditor(anEvent);
      return mEditor.isCellEditable(anEvent);
   }

   @Override
   public void addCellEditorListener(CellEditorListener l) {
      mEditor.addCellEditorListener(l);
   }

   @Override
   public void removeCellEditorListener(CellEditorListener l) {
      mEditor.removeCellEditorListener(l);
   }

   @Override
   public boolean shouldSelectCell(EventObject anEvent) {
      selectEditor(anEvent);
      return mEditor.shouldSelectCell(anEvent);
   }

   protected void selectEditor(EventObject e) {
      int row = 0;
      if(e == null)
         row = mTable.getSelectionModel().getAnchorSelectionIndex();
      else if(e instanceof MouseEvent)
         row = mTable.rowAtPoint(((MouseEvent) e).getPoint());
      mEditor = getEditorAt(row);
   }
}