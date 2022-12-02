package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.EPQDatabase.Session;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Material;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

/**
 * <p>
 * Provides an edit box connected to a database Session for entering the name of
 * Material/Composition objects.
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
public class JMaterialPanel extends JPanel {

   private static final long serialVersionUID = -5485189264137306015L;

   private final Session mSession;
   private final boolean mWithNone;
   private final boolean mWithDensity;

   private final JTextField jTextField_Name = new JTextField();
   private final JButton jButton_Edit = new JButton("...");
   private final JButton jButton_None = new JButton("None");

   private static final String WO_NONE = "fill:pref:grow, 5dlu, pref";
   private static final String WITH_NONE = "fill:pref:grow, 5dlu, pref, 2dlu, pref";

   private static final Material NONE = Material.Null;

   private Composition mResult = Material.Null;

   /**
    * Constructs a JPanelMaterial
    */
   public JMaterialPanel(Session session, boolean withDensity, boolean withNone) {
      this(session, withNone ? WITH_NONE : WO_NONE, withDensity, withNone);
   }

   /**
    * Constructs a JPanelMaterial
    */
   public JMaterialPanel(Session session, String layout, boolean withDensity, boolean withNone) {
      super(new FormLayout(layout, "pref"), false);
      mWithNone = withNone;
      mWithDensity = withDensity;
      mSession = session;
      init();
   }

   private final ArrayList<ActionListener> mMaterialChange = new ArrayList<ActionListener>();

   public void addMaterialChange(ActionListener al) {
      mMaterialChange.add(al);
   }

   public void removeMaterialChange(ActionListener al) {
      mMaterialChange.remove(al);
   }

   public void clearMaterialChange() {
      mMaterialChange.clear();
   }

   private void fireMaterialChange() {
      ActionEvent ae = new ActionEvent(JMaterialPanel.this, 0, "MaterialChange", System.currentTimeMillis(), 0);
      ArrayList<ActionListener> dup = new ArrayList<ActionListener>(mMaterialChange);
      for (ActionListener al : dup)
         al.actionPerformed(ae);
   }

   /**
    * @return Composition or null
    */
   private Composition extractComposition() {
      final String name = jTextField_Name.getText();
      if (mWithNone && ((name.toLowerCase().equals("none")) || (name.trim().length() == 0)))
         return NONE;
      try {
         return mSession.findStandard(name);
      } catch (SQLException e) {
         return null;
      }
   }

   private void updateNameField() {
      if (mResult != null) {
         jTextField_Name.setText(mResult.getName());
         if (mWithDensity && (!(mResult instanceof Material))) {
            jTextField_Name.setBackground(SystemColor.yellow);
            jTextField_Name.setToolTipText("Please specify a density.");
            Toolkit.getDefaultToolkit().beep();
         } else {
            jTextField_Name.setBackground(SystemColor.window);
            jTextField_Name.setToolTipText("<html>" + mResult.toHTMLTable() + "</html>");
         }
      } else {
         jTextField_Name.setBackground(SystemColor.pink);
         jTextField_Name.setToolTipText("Unknown material");
         Toolkit.getDefaultToolkit().beep();
      }

   }

   private Window getParentWindow() {
      Component p = getParent();
      while (p != null) {
         if (p instanceof Dialog)
            return (Window) p;
         if (p instanceof Frame)
            return (Window) p;
         p = p.getParent();
      }
      return null;
   }

   private final void init() {
      jTextField_Name.addFocusListener(new FocusAdapter() {
         @Override
         public void focusLost(FocusEvent arg0) {
            Composition comp = extractComposition();
            if ((comp != null) && (!comp.equals(mResult))) {
               mResult = comp;
               fireMaterialChange();
            }
            updateNameField();
         }
      });
      jButton_Edit.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent arg0) {
            Composition comp = MaterialsCreator.editMaterial(getParentWindow(), mResult, mSession, mWithDensity);
            if ((comp != null) && (!comp.equals(mResult))) {
               mResult = comp;
               fireMaterialChange();
            }
            updateNameField();
         }
      });

      jButton_None.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            Composition comp = Material.Null;
            if ((comp != null) && (!comp.equals(mResult))) {
               mResult = comp;
               fireMaterialChange();
            }
            updateNameField();
         }
      });

      add(jTextField_Name, CC.xy(1, 1));
      add(jButton_Edit, CC.xy(3, 1));
      if (mWithNone)
         add(jButton_None, CC.xy(5, 1));
   }

   /**
    * Returns the composition associated with this editor.
    *
    * @return Composition or null
    */
   public Composition getComposition() {
      return mResult;
   }

   public void setComposition(Composition comp) {
      mResult = comp != null ? comp : NONE;
      jTextField_Name.setText(mResult.getName());
      extractComposition();
   }

   @Override
   public void setToolTipText(String ttt) {
      super.setToolTipText(ttt);
      jTextField_Name.setToolTipText(ttt);
      jButton_Edit.setToolTipText(ttt);
   }

   /**
    * Returns the Material associated with this editor or null if none.
    *
    * @return Material or null
    */
   public Material getMaterial() {
      Composition comp = getComposition();
      return comp instanceof Material ? (Material) comp : null;
   }
}
