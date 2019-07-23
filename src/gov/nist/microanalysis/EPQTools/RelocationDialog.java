package gov.nist.microanalysis.EPQTools;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gov.nist.microanalysis.Utility.Translate2D;

/**
 * <p>
 * A dialog to configure and calibrate relocation via fiducial points.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nicholas
 * @version 1.0
 */
public class RelocationDialog extends JDialog {

   private static final long     serialVersionUID = -8020349596488392051L;
   private static final String   DIALOG_TITLE     = "Configure relocation";
   private final RelocationPanel mRelocation      = new RelocationPanel();
   private Translate2D           mResult          = null;

   /**
    * Constructs a RelocationDialog
    * 
    * @param owner
    * @param modal
    */
   public RelocationDialog(Dialog owner, boolean modal) {
      super(owner, DIALOG_TITLE, modal);
      try {
         initialize();
         pack();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a RelocationDialog
    * 
    * @param owner
    */
   public RelocationDialog(Window owner) {
      super(owner, DIALOG_TITLE);
      try {
         initialize();
         pack();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a RelocationDialog
    * 
    * @param owner
    * @param modal
    */
   public RelocationDialog(Frame owner, boolean modal) {
      super(owner, DIALOG_TITLE, modal);
      try {
         initialize();
         pack();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   private void initialize() {
      final FormLayout fl = new FormLayout("pref", "pref, 4dlu, pref, 4dlu, pref");
      final PanelBuilder pb = new PanelBuilder(fl);
      final CellConstraints cc = new CellConstraints();
      pb.add(mRelocation, cc.xy(1, 1));
      pb.addSeparator("", cc.xy(1, 3));
      final JButton okButton = new JButton("Ok");
      okButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mResult = mRelocation.getTransformation();
            setVisible(false);
         }

      });

      final JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mResult = null;
            setVisible(false);
         }

      });
      final ButtonBarBuilder bbb = new ButtonBarBuilder();
      bbb.addGlue();
      bbb.addButton(okButton, cancelButton);
      pb.add(bbb.build(), cc.xy(1, 5));
      final JPanel panel = pb.getPanel();
      panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      setContentPane(panel);

   }

   public Translate2D getResult() {
      return mResult != null ? new Translate2D(mResult) : null;
   }

   public ArrayList<Translate2D.CalibrationPoint> getCalibrationPoints() {
      return mRelocation.getCalibrationPoints();
   }

   public void setCalibrationPoints(Collection<Translate2D.CalibrationPoint> pts) {
      mRelocation.setCalibrationPoints(pts);
   }
}
