/**
 * gov.nist.microanalysis.Utility.SpectrumPropertiesTableModel Created by:
 * nritchie Date: Dec 13, 2007
 */
package gov.nist.microanalysis.Utility;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;

import java.util.Collection;

import javax.swing.table.AbstractTableModel;

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

public class SpectrumPropertiesTableModel extends AbstractTableModel {

   static public final long serialVersionUID = 0x1;

   private SpectrumProperties.PropertyId[] mNames;
   SpectrumProperties mProperties;

   public SpectrumPropertiesTableModel(Collection<ISpectrumData> specs) {
      SpectrumProperties res = null;
      for (final ISpectrumData sd : specs)
         res = (res == null ? sd.getProperties() : SpectrumProperties.merge(res, sd.getProperties()));
      mProperties = res != null ? res : new SpectrumProperties();
      if (res != null)
         mNames = res.getPropertySet().toArray(new SpectrumProperties.PropertyId[res.getPropertySet().size()]);
      else
         mNames = new SpectrumProperties.PropertyId[0];
   }

   public SpectrumPropertiesTableModel(SpectrumProperties sp) {
      mProperties = sp;
      if (mProperties != null)
         mNames = sp.getPropertySet().toArray(new SpectrumProperties.PropertyId[sp.getPropertySet().size()]);
      else
         mNames = new SpectrumProperties.PropertyId[0];
   }

   @Override
   public String getColumnName(int column) {
      return column == 0 ? "Name" : "Value";
   }

   public SpectrumProperties getProperties() {
      return mProperties;
   }

   @Override
   public int getColumnCount() {
      return 2;
   }

   @Override
   public int getRowCount() {
      return mNames.length;
   }

   @Override
   public Object getValueAt(int rowIndex, int columnIndex) {
      Object res = "";
      if (columnIndex == 0)
         res = mNames[rowIndex].toString();
      else
         try {
            res = mProperties.getObjectProperty(mNames[rowIndex]);
            return res instanceof Composition ? res : mProperties.getTextProperty(mNames[rowIndex]);
         } catch (final EPQException ex) {
            res = ex.getMessage();
         }
      return res;
   }
}