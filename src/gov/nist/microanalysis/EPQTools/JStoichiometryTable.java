package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Oxidizer;
import gov.nist.microanalysis.Utility.Math2;

import java.util.Collection;
import java.util.Collections;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * <p>
 * A table to permit users to edit the assumed valance for stoichiometric
 * calculations.
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
public class JStoichiometryTable
   extends JTable {

   private static final long serialVersionUID = 4651450032637424208L;

   private final Oxidizer mOxidizer = new Oxidizer();

   // private int[] mOxidationState;

   private final Object[] HEADER = new Object[] {
      "Element",
      "Cation",
      "Anion",
      "As"
   };

   private String toHTML(Element elm, int on, int an) {
      return "<HTML>" + elm.toAbbrev() + (on > 1 ? "<sub>" + Integer.toString(on) + "</sub>" : "")
            + (an > 0 ? "O" + (an > 1 ? "<sub>" + Integer.toString(an) + "</sub>" : "") : "");
   }

   private class StoichiometryTableModel
      extends DefaultTableModel {

      private static final long serialVersionUID = -5013245809749498785L;

      private StoichiometryTableModel(Collection<Element> elms) {
         super(buildData(elms), HEADER);
      }

      @Override
      public boolean isCellEditable(int row, int col) {
         return (col == 1) || (col == 2);
      }

      @Override
      public void setValueAt(Object aValue, int row, int col) {
         if((col == 1) || (col == 2)) {
            try {
               Integer val;
               if(aValue instanceof String) {
                  val = Integer.parseInt((String) aValue);
                  aValue = val;
               } else
                  val = (Integer) aValue;
               if(val.intValue() < 0)
                  aValue = Integer.valueOf(0);
            }
            catch(final Exception ex) {
               aValue = super.getValueAt(row, col);
            }
            int on, an;
            if(col == 1) {
               on = ((Integer) aValue).intValue();
               an = ((Integer) getValueAt(row, 2)).intValue();
            } else {
               on = ((Integer) getValueAt(row, 1)).intValue();
               an = ((Integer) aValue).intValue();
            }
            final String res = toHTML((Element) getValueAt(row, 0), on, an);
            super.setValueAt(res, row, 3);
         }
         super.setValueAt(aValue, row, col);
      }

   };

   /**
    * Constructs a JStoichiometryTable
    */
   public JStoichiometryTable() {
      setElements(Collections.<Element> emptyList());
   }

   public void setElements(Collection<Element> elms) {
      setModel(new StoichiometryTableModel(elms));
   }

   private Object[][] buildData(Collection<Element> elms) {
      final Object[][] data = new Object[elms.size()][];
      int r = 0;
      for(final Element elm : elms) {
         int o = -mOxidizer.getOxidationState(Element.O);
         int e = mOxidizer.getOxidationState(elm);
         final int gcd = (int) Math2.gcd(e, o);
         if(gcd > 1) {
            e /= gcd;
            o /= gcd;
         }
         data[r] = new Object[] {
            elm,
            Integer.valueOf(o),
            Integer.valueOf(e),
            toHTML(elm, o, e)
         };
         ++r;
      }
      return data;
   }

   public Oxidizer getOxidizer() {
      update();
      return mOxidizer;
   }

   private void update() {
      for(int r = 0; r < getRowCount(); ++r) {
         final int stoic = (-((Number) getValueAt(r, 2)).intValue() * mOxidizer.getOxidationState(Element.O))
               / ((Number) getValueAt(r, 1)).intValue();
         mOxidizer.setOxidizationState((Element) getValueAt(r, 0), stoic);
      }
   }
}
