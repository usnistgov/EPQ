package gov.nist.microanalysis.EPQTools;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import gov.nist.microanalysis.EPQDatabase.Session;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.ToSI;

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
public class JComboBoxCoating extends JComboBox<Material> {

   private final Session mSession;
   private final JDialog mParent;
   private final JPanel mPanel;

   private static Material[] defaultMaterials() {
      Material[] mats = new Material[]{new Material(Element.C, ToSI.gPerCC(1.9)), new Material(Element.Au, ToSI.gPerCC(19.30)),
            new Material(Element.Ir, ToSI.gPerCC(22.56)), new Material(Element.Pt, ToSI.gPerCC(21.45)), Material.Null, Material.Other};
      return mats;
   }

   private static final long serialVersionUID = -1774842483567435968L;

   public JComboBoxCoating(JDialog parent, Session ses) {
      super(defaultMaterials());
      mSession = ses;
      mParent = parent;
      mPanel = null;
      addActionListener(new AbstractAction() {

         private static final long serialVersionUID = -419054589747754452L;

         @Override
         public void actionPerformed(ActionEvent e) {
            Material mat = (Material) getSelectedItem();
            if (mat != null) {
               if (mat == Material.Other) {
                  mat = (Material) MaterialsCreator.createMaterial(mParent, mSession, true);
                  getModel().setSelectedItem(mat);
               }
            }
         }
      });
   }

   public JComboBoxCoating(JPanel panel, Session ses) {
      super(defaultMaterials());
      mSession = ses;
      mParent = null;
      mPanel = panel;
      addActionListener(new AbstractAction() {

         private static final long serialVersionUID = -419054589747754452L;

         @Override
         public void actionPerformed(ActionEvent e) {
            Material mat = (Material) getSelectedItem();
            if (mat != null) {
               if (mat == Material.Other) {
                  Container c = mPanel;
                  do {
                     c = c.getParent();
                     if (c instanceof Frame) {
                        mat = (Material) MaterialsCreator.createMaterial((Frame) c, mSession, true);
                        break;
                     } else if (c instanceof Dialog) {
                        mat = (Material) MaterialsCreator.createMaterial((Dialog) c, mSession, true);
                        break;
                     }
                  } while (c != null);
                  getModel().setSelectedItem(mat);
               }
            }
         }
      });
   }

}
