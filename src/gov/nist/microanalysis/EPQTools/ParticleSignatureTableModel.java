package gov.nist.microanalysis.EPQTools;

import java.text.NumberFormat;

import javax.swing.table.AbstractTableModel;

import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ParticleSignature;
import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * A table model for displaying particle signature data...
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
public class ParticleSignatureTableModel
   extends
   AbstractTableModel {

   private static final long serialVersionUID = -919647372884872889L;

   final private ParticleSignature mSignature;

   final private NumberFormat mFormat = new HalfUpFormat("0.0000");

   final private String[] mColumnNames = new String[] {
      "Element",
      "Particle signature"
   };

   public ParticleSignatureTableModel(ParticleSignature comp) {
      super();
      mSignature = comp;
   }

   @Override
   public String getColumnName(int column) {
      return mColumnNames[column];
   }

   @Override
   public int getColumnCount() {
      return 3;
   }

   @Override
   public int getRowCount() {
      return mSignature != null ? mSignature.getUnstrippedElementSet().size() : 0;
   }

   @Override
   public Object getValueAt(int rowIndex, int columnIndex) {
      final Element elm = mSignature.getNthElement(rowIndex);
      switch(columnIndex) {
         case 0:
            return elm.toString();
         default:
            return mFormat.format(mSignature.get(elm));
      }
   }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer();
      sb.append("Element\tSignature\n");
      for(int sr = 0; sr < getRowCount(); ++sr) {
         sb.append(getValueAt(sr, 0).toString());
         sb.append("\t");
         sb.append(getValueAt(sr, 1).toString());
         sb.append("\n");
      }
      return sb.toString();
   }

   public String toHTML() {
      final StringBuffer sb = new StringBuffer();
      sb.append("<table>\n");
      sb.append("\t<tr><th>Element</th><th>Signature</th></tr>\n");
      for(int sr = 0; sr < getRowCount(); ++sr) {
         sb.append("\t<tr><td>");
         sb.append(getValueAt(sr, 0).toString());
         sb.append("</th><th>");
         sb.append(getValueAt(sr, 1).toString());
         sb.append("</th></tr>\n");
      }
      sb.append("</table>\n");
      return sb.toString();
   }
}
