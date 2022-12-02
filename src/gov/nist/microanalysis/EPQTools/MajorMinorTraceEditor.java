/**
 * gov.nist.microanalysis.EPQTools.MajorMinorTraceEditor Created by: nritchie
 * Date: Jan 30, 2014
 */
package gov.nist.microanalysis.EPQTools;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.MajorMinorTrace;

/**
 * <p>
 * Description
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
public class MajorMinorTraceEditor extends JDialog {

   private static final long serialVersionUID = -6816201721574238301L;

   private void setElements(MajorMinorTrace mmt, Collection<Element> elms) {
      for (final Element elm : getElements(mmt))
         mElements.remove(elm);
      for (final Element elm : elms)
         mElements.put(elm, mmt);
   }

   private TreeSet<Element> getElements(MajorMinorTrace mmt) {
      final TreeSet<Element> res = new TreeSet<Element>();
      for (final Element elm : mElements.keySet())
         if (mElements.get(elm).equals(mmt))
            res.add(elm);
      return res;
   }

   private void askElements(MajorMinorTrace mmt) {
      final SelectElements se = new SelectElements(MajorMinorTraceEditor.this, "Select " + mmt.toString() + " elements.");
      se.enableAll(false);
      se.enableElements(Element.H, Element.Am, true);
      se.setSelected(getElements(mmt));
      se.setLocationRelativeTo(MajorMinorTraceEditor.this);
      se.setVisible(true);
      setElements(mmt, se.getElements());
      update();
   }

   private void update() {
      mComposition = MajorMinorTrace.asComposition(mElements, jTextField_Name.getText());
      jTable_Composition.setModel(new CompositionTableModel(mComposition, false, true));
   }

   private class MMTAction extends AbstractAction {

      private static final long serialVersionUID = 3418801723291801988L;
      private final MajorMinorTrace mLevel;

      private MMTAction(MajorMinorTrace mmt) {
         super(mmt.toString());
         mLevel = mmt;
      }

      @Override
      public void actionPerformed(ActionEvent arg0) {
         askElements(mLevel);
      }
   };

   private class OkCancelAction extends AbstractAction {

      private static final long serialVersionUID = -941032539735360115L;

      private final boolean mMode;

      private OkCancelAction(boolean ok) {
         super(ok ? "Ok" : "Cancel");
         mMode = ok;
      }

      @Override
      public void actionPerformed(ActionEvent arg0) {
         mOk = mMode;
         setVisible(false);
      }
   }

   private class ClearAction extends AbstractAction {

      private static final long serialVersionUID = -941032539735360115L;

      private ClearAction() {
         super("Clear");
      }

      @Override
      public void actionPerformed(ActionEvent arg0) {
         mElements.clear();
         update();
      }
   }

   private class DeleteAction extends AbstractAction {

      private static final long serialVersionUID = -941032539735360115L;

      private DeleteAction() {
         super("Delete");
      }

      @Override
      public void actionPerformed(ActionEvent arg0) {
         final int[] rows = jTable_Composition.getSelectedRows();
         for (final int row : rows) {
            final Object obj = jTable_Composition.getValueAt(row, 0);
            if (obj instanceof Element)
               mElements.remove(obj);
         }
         update();
      }
   }

   private final Map<Element, MajorMinorTrace> mElements = new TreeMap<Element, MajorMinorTrace>();

   private boolean mOk = false;
   private Composition mComposition = new Composition();

   private final JTextField jTextField_Name = new JTextField();
   private final JButton jButton_Major = new JButton(new MMTAction(MajorMinorTrace.Major));
   private final JButton jButton_Minor = new JButton(new MMTAction(MajorMinorTrace.Minor));
   private final JButton jButton_Trace = new JButton(new MMTAction(MajorMinorTrace.Trace));
   private final JButton jButton_Clear = new JButton(new ClearAction());
   private final JButton jButton_Delete = new JButton(new DeleteAction());

   private final JTable jTable_Composition = new JTable();

   private final JButton jButton_Ok = new JButton(new OkCancelAction(true));
   private final JButton jButton_Cancel = new JButton(new OkCancelAction(false));

   private void init() {
      final FormLayout fl = new FormLayout("10dlu, pref, 5dlu, 200dlu, 5dlu, pref",
            "pref, 5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, pref, 140dlu, 5dlu, pref, 5dlu, pref, 5dlu, pref");
      final PanelBuilder pb = new PanelBuilder(fl, new JPanel());
      pb.addSeparator("Name", CC.xyw(1, 1, 6));
      pb.addLabel("Name", CC.xyw(2, 3, 1));
      pb.add(jTextField_Name, CC.xyw(4, 3, 1));
      pb.addSeparator("Composition", CC.xyw(1, 5, 6));
      pb.add(new JScrollPane(jTable_Composition), CC.xywh(2, 7, 3, 4));
      pb.add(jButton_Clear, CC.xy(6, 7));
      pb.add(jButton_Delete, CC.xy(6, 9));
      update();
      {
         final ButtonBarBuilder bbb = new ButtonBarBuilder();
         bbb.addButton(jButton_Major);
         bbb.addGlue();
         bbb.addButton(jButton_Minor);
         bbb.addGlue();
         bbb.addButton(jButton_Trace);
         pb.add(bbb.getPanel(), CC.xyw(2, 12, 3));
      }
      pb.addSeparator("", CC.xyw(1, 14, 6));
      {
         final ButtonBarBuilder bbb = new ButtonBarBuilder();
         bbb.addGlue();
         bbb.addButton(jButton_Ok);
         bbb.addUnrelatedGap();
         bbb.addButton(jButton_Cancel);
         pb.add(bbb.getPanel(), CC.xyw(2, 16, 5));
      }
      pb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      add(pb.getPanel());
      jTextField_Name.setText("Estimate");
   }

   /**
    * Constructs a MajorMinorTraceEditor
    */
   public MajorMinorTraceEditor(final Frame frame) {
      super(frame, "Major/Minor/Trace", true);
      init();
      pack();
      setResizable(false);
   }

   /**
    * Constructs a MajorMinorTraceEditor
    */
   public MajorMinorTraceEditor(final Dialog dialog) {
      super(dialog, "Major/Minor/Trace", true);
      init();
      pack();
      setResizable(false);
   }

   public void setComposition(Map<Element, MajorMinorTrace> elms) {
      mElements.clear();
      mElements.putAll(elms);
      jTextField_Name.setText("Estimate");
      update();
   }

   public void setComposition(Composition comp) {
      mElements.clear();
      jTextField_Name.setText("Approx. " + comp.getName());
      mElements.putAll(MajorMinorTrace.from(comp, false));
      update();
   }

   public Composition getComposition() {
      return mOk ? mComposition : null;
   }

   public Map<Element, MajorMinorTrace> getMajorMinorTrace() {
      return new TreeMap<Element, MajorMinorTrace>(mElements);
   }
}
