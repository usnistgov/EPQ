package gov.nist.microanalysis.EPQTools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.SystemColor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gov.nist.microanalysis.EPQLibrary.Element;

/**
 * <p>
 * A dialog box for selecting elements from the NISTTools.JPeriodicTable
 * controls.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class SelectElements extends JDialog {
   private static final long serialVersionUID = 0x1;
   private final JButton jButton_Ok = new JButton("Ok");
   private final JPeriodicTable jPeriodic = new JPeriodicTable();
   private final JTextField jTextField_Selected = new JTextField();
   private boolean mMultiSelect = true;

   public SelectElements(Window window, String title) {
      super(window, title);
      setModal(true);
      try {
         init();
         pack();
         setResizable(false);
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   public static Set<Element> selectElements(Window parent, String title, Collection<Element> candidates, Collection<Element> preSelected) {
      final SelectElements se = new SelectElements(parent, title);
      se.enableAll(false);
      se.enableElements(candidates);
      se.setSelected(preSelected);
      se.setMultiSelect(true);
      se.setLocationRelativeTo(parent);
      se.setVisible(true);
      return se.getElements();

   }

   public static Set<Element> selectElements(JDialog parent, String title, Collection<Element> candidates, Collection<Element> preSelected) {
      final SelectElements se = new SelectElements(parent, title);
      se.enableAll(false);
      se.enableElements(candidates);
      se.setSelected(preSelected);
      se.setMultiSelect(true);
      se.setLocationRelativeTo(parent);
      se.setVisible(true);
      return se.getElements();

   }

   private void init() throws Exception {
      if (this.getTitle().length() == 0)
         this.setTitle("Periodic table of the elements");

      jPeriodic.setDoubleBuffered(false);
      jPeriodic.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            if (!mMultiSelect)
               jPeriodic.setAllExcept(false, jPeriodic.getLastSelected());
            updateSelected();
         }
      });
      jTextField_Selected.setText("");

      final ButtonBarBuilder bbb = new ButtonBarBuilder();
      bbb.addGlue();
      bbb.addButton(jButton_Ok);
      final JPanel buttonPanel = bbb.build();
      jButton_Ok.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            setVisible(false);
         }
      });
      jTextField_Selected.addKeyListener(new KeyAdapter() {
         @Override
         public void keyTyped(KeyEvent arg0) {
            final String str = jTextField_Selected.getText();
            final int pos = jTextField_Selected.getCaretPosition();
            final String tmp = str.substring(0, pos) + arg0.getKeyChar() + str.substring(pos);
            final String[] items = tmp.split("[,\\s]+");
            final TreeSet<Element> pre = new TreeSet<Element>(jPeriodic.getSelected());
            final TreeSet<Element> sel = new TreeSet<Element>(jPeriodic.getSelected());
            boolean ok = true;
            for (final String item : items) {
               final String tr = item.trim();
               if (tr.length() > 0) {
                  final Element elm = Element.byName(tr);
                  if (elm != Element.None) {
                     if (!pre.contains(elm))
                        jPeriodic.setSelection(elm, true);
                     sel.remove(elm);
                     if (!mMultiSelect)
                        break;
                  } else
                     ok = false;
               }
            }
            jTextField_Selected.setBackground(ok ? SystemColor.text : Color.pink);
            for (final Element elm : sel)
               jPeriodic.setSelection(elm, false);
         }
      });

      final FormLayout fl = new FormLayout("pref, 5dlu, fill:200dlu", "pref, 5dlu, pref, 5dlu, pref");
      final PanelBuilder pb = new PanelBuilder(fl);
      final CellConstraints cc = new CellConstraints();
      pb.add(jPeriodic, cc.xyw(1, 1, 3));
      pb.addLabel("Selected", cc.xy(1, 3));
      pb.add(jTextField_Selected, cc.xy(3, 3));
      pb.add(buttonPanel, cc.xyw(1, 5, 3));
      final JPanel panel = pb.getPanel();
      panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      add(panel, BorderLayout.CENTER);
      getRootPane().setDefaultButton(jButton_Ok);
      jTextField_Selected.requestFocusInWindow();
   }

   public void setMultiSelect(boolean ms) {
      if (mMultiSelect != ms) {
         mMultiSelect = ms;
         if (!mMultiSelect) {
            jPeriodic.setAllExcept(false, jPeriodic.getLastSelected());
            jTextField_Selected.setText("Select an element from the periodic table.");
         } else
            jTextField_Selected.setText("Select one or more elements from the periodic table.");
      }
   }

   /**
    * getElements - Gets a set containing the selected elements.
    * 
    * @return Set&gt;Element&lt;
    */
   public Set<Element> getElements() {
      return jPeriodic.getSelected();
   }

   public void enableElements(Element begin, Element end, boolean enabled) {
      jPeriodic.enableRange(begin, end, enabled);
   }

   public void enableElements(Collection<Element> enabled) {
      jPeriodic.enableAll(false);
      for (Element elm : enabled)
         jPeriodic.setEnabled(elm, true);
      updateSelected();
   }

   /**
    * getElement - Returns the last element selected. null for no element.
    * 
    * @return Element or null
    */
   public Element getElement() {
      assert (!mMultiSelect);
      final int elm = jPeriodic.getLastSelected();
      return elm != Element.elmNone ? Element.byAtomicNumber(elm) : null;
   }

   public void setSelected(Element elm) {
      jPeriodic.setSelection(elm.getAtomicNumber(), true);
      updateSelected();
   }

   public void setSelected(Collection<Element> elms) {
      jPeriodic.setSelected(elms, true);
      updateSelected();
   }

   /**
    * setEnabled - Determines whether the specified element is available for
    * selection.
    * 
    * @param elm
    *           Element
    * @param enabled
    *           boolean
    */
   public void setEnabled(Element elm, boolean enabled) {
      jPeriodic.setEnabled(elm, enabled);
      updateSelected();
   }

   /**
    * enableAll - Enables or disables all elements for selection.
    * 
    * @param enabled
    *           boolean
    */
   public void enableAll(boolean enabled) {
      jPeriodic.enableAll(enabled);
   }

   private void updateSelected() {
      final StringBuffer sb = new StringBuffer();
      for (final Element elm2 : jPeriodic.getSelected()) {
         if (sb.length() > 0)
            sb.append(", ");
         sb.append(elm2.toAbbrev());
      }
      jTextField_Selected.setText(sb.toString());
      jTextField_Selected.setBackground(SystemColor.text);
   }
}
