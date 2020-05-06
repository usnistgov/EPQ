/**
 * gov.nist.microanalysis.EPQTools.CompositionTableModel Created by: nritchie
 * Date: Jan 25, 2014
 */
package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.MajorMinorTrace;
import gov.nist.microanalysis.Utility.HalfUpFormat;

import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.table.DefaultTableModel;

/**
 * <p>
 * A TableModel for displaying Composition data in a JTable.
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
public class CompositionTableModel
   extends DefaultTableModel {

   private static final long serialVersionUID = -919647372884872889L;

   final private Composition mComposition;
   final private boolean mWithNormalized;
   final private boolean mWithMajorMinorTrace;

   final private NumberFormat mFormat = new HalfUpFormat("0.0000");

   final private static String[] mColumnNames3 = new String[] {
      "Element",
      "Mass Fraction",
      "Atomic Fraction"
   };

   final private static String[] mColumnNames4 = new String[] {
      "Element",
      "Mass Fraction",
      "Normalized",
      "Atom Fraction"
   };

   private static String[] buildColumns(boolean withNorm, boolean withMajorMinorTrace) {
      final String[] base = withNorm ? mColumnNames4 : mColumnNames3;
      String[] res = base;
      if(withMajorMinorTrace) {
         res = new String[base.length + 1];
         for(int i = 0; i < base.length; ++i)
            res[i] = base[i];
         res[base.length] = "M/M/T";
      }
      return res;
   }

   public CompositionTableModel(Composition comp, boolean withNorm, boolean withMajorMinorTrace) {
      super(buildColumns(withNorm, withMajorMinorTrace), 0);
      mWithNormalized = withNorm;
      mWithMajorMinorTrace = withMajorMinorTrace;
      mComposition = comp;
   }

   public CompositionTableModel(Composition comp, boolean withNorm) {
      this(comp, withNorm, false);
   }

   @Override
   public int getRowCount() {
      return mComposition != null ? mComposition.getElementCount() : 0;
   }

   @Override
   public Object getValueAt(int rowIndex, int columnIndex) {
      if(mComposition != null) {
         final Element elm = (new ArrayList<Element>(mComposition.getElementSet())).get(rowIndex);
         switch(columnIndex) {
            case 0:
               return elm;
            case 1:
               return mComposition.weightFractionU(elm, false).format(mFormat);
            case 2:
               return mWithNormalized ? mComposition.weightFractionU(elm, true).format(mFormat)
                     : mComposition.atomicPercentU(elm).format(mFormat);
            case 3:
               return mWithNormalized ? mComposition.atomicPercentU(elm).format(mFormat)
                     : MajorMinorTrace.create(mComposition.weightFraction(elm, false));
            default:
               return MajorMinorTrace.create(mComposition.weightFraction(elm, false));
         }
      } else
         return "";
   }

   @Override
   public String toString() {
      final StringBuffer sb = new StringBuffer();
      if(mWithNormalized)
         sb.append("Element\tMass Fraction\tNorm(Mass Fraction)\tAtomic Fraction");
      else
         sb.append("Element\tMass Fraction\tAtomic Fraction");
      if(mWithMajorMinorTrace)
         sb.append("\tM/M/T");
      sb.append("\n");
      for(int sr = 0; sr < getRowCount(); ++sr)
         for(int col = 0; col < getColumnCount(); ++col) {
            sb.append(getValueAt(sr, col).toString());
            sb.append((col + 1) < getColumnCount() ? "\t" : "\n");
         }
      return sb.toString();
   }

   public String toHTML() {
      final StringBuffer sb = new StringBuffer();
      sb.append("<table>\n");
      sb.append("\t<tr><th>Element</th><th>Mass Fraction</th><th>Atomic Fraction</th>");
      if(mWithMajorMinorTrace)
         sb.append("<th>M/M/T</th>");
      sb.append("</tr>\n");
      for(int sr = 0; sr < getRowCount(); ++sr) {
         sb.append("\t<tr>");
         for(int col = 0; col < getColumnCount(); ++col) {
            sb.append("<td>");
            sb.append(getValueAt(sr, col).toString());
            sb.append("</td>");
         }
         sb.append("</tr>\n");
      }
      sb.append("</table>\n");
      return sb.toString();
   }
   
   public Composition getComposition() {
	   return mComposition;
   }
}