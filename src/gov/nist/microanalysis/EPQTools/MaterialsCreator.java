package gov.nist.microanalysis.EPQTools;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.SystemColor;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gov.nist.microanalysis.EPQDatabase.Session;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.ToSI;

/**
 * <p>
 * A GUI for creating or editing a EPQLibrary.Material or EPQLibrary.Composition
 * object
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Daniel "Ooblioob" Davis &amp; Nicholas W. M. Ritchie
 * @version 1.0
 */

public class MaterialsCreator extends JDialog {

   private static final long serialVersionUID = -2244980368216002637L;

   private Composition mCurrentMaterial = new Material(ToSI.gPerCC(5.0));
   private boolean mWasCanceled = false;
   private boolean mRequireDensity = false;
   private Session mSession;
   private boolean mInhibitUpdate = false;
   private final NumberFormat mDefaultFmt = NumberFormat.getInstance();

   private final JLabel jLabel_Name = new JLabel();
   private final JTextField jTextField_Name = new JTextField();
   private final JButton jButton_Search = new JButton();

   private final JRadioButton jRadioButton_Weight = new JRadioButton();
   private final JRadioButton jRadioButton_Atomic = new JRadioButton();

   private final JLabel jLabel_Element = new JLabel();
   private final JTextField jTextField_Element = new JTextField();
   private final JLabel jLabel_Quantity = new JLabel();
   private final JTextField jTextField_Quantity = new JTextField();

   private final JButton jButton_Add = new JButton();
   private final JButton jButton_Delete = new JButton();
   private final JButton jButton_Clear = new JButton();

   private final JLabel jLabel_Density = new JLabel();
   private final JTextField jTextField_Density = new JTextField();
   private final JLabel jLabel_gPerCC = new JLabel();
   private final JButton jButton_Done = new JButton();
   private final JButton jButton_Cancel = new JButton();

   private final JTable jTable_Composition = new JTable();
   private final ButtonGroup buttonGroup_units = new ButtonGroup();

   /**
    * MaterialsCreator - Initializes a MaterialsCreator dialog instance
    * 
    * @param frame
    *           Frame - The parent frame that will host the dialog
    * @param title
    *           String - The title that will appear in the top bar of the dialog
    * @param modal
    *           boolean - if true then the dialog will disable access to the
    *           parent/host frame until the dialog is closed.
    */
   public MaterialsCreator(final Frame frame, final String title, final boolean modal) {
      super(frame, title, modal);
      try {
         initialize();
         jTextField_Name.grabFocus();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * MaterialsCreator - Initializes a MaterialsCreator dialog instance
    * 
    * @param window
    *           - The parent window that will host the dialog
    * @param title
    *           String - The title that will appear in the top bar of the dialog
    */
   public MaterialsCreator(final Window window, final String title) {
      super(window, title);
      setModal(true);
      try {
         initialize();
         jTextField_Name.grabFocus();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }

   }

   public MaterialsCreator(final Dialog dialog, final String title, final boolean modal) {
      super(dialog, title, modal);
      try {
         initialize();
         jTextField_Name.grabFocus();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   public MaterialsCreator() {
      super((Frame) null, "Material editor", true);
      try {
         initialize();
         jTextField_Name.grabFocus();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   private boolean ignoredCompNames(final Composition comp) {
      final String[] names = new String[]{"temp", "tmp", "crap", "junk", "stuff", "Enter material name here"};
      boolean res = false;
      for (final String name : names)
         res |= (comp.toString().compareToIgnoreCase(name) == 0);
      return res;
   }

   private void initialize() throws Exception {
      mDefaultFmt.setMaximumFractionDigits(4);

      jRadioButton_Weight.setToolTipText("Quantities will be measured in mass percent");
      jRadioButton_Weight.setText("Mass Fractions");

      jRadioButton_Atomic.setToolTipText("Quantities will be measured per atom (out of 100 atoms)");
      jRadioButton_Atomic.setText("Atomic Proportions");

      buttonGroup_units.add(jRadioButton_Weight);
      buttonGroup_units.add(jRadioButton_Atomic);
      buttonGroup_units.setSelected(jRadioButton_Weight.getModel(), true);

      jTable_Composition.setForeground(SystemColor.textText);
      jTable_Composition.setModel(new CompositionTableModel(mCurrentMaterial, false));
      jTable_Composition.setFocusable(false);

      jTextField_Name.setText("");
      jTextField_Name.setToolTipText("Enter the name of your material");
      jTextField_Name.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(final FocusEvent e) {
            jTextField_Name_focusGained(e);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            jTextField_Name_focusLost(e);
         }
      });
      jButton_Search.setIcon(new ImageIcon(getClass().getResource("ClipArt/MagnifyingGlassGrey.png")));
      jButton_Search.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent e) {
            searchDatabase();
         }
      });

      jLabel_Name.setDisplayedMnemonic('N');
      jLabel_Name.setLabelFor(jTextField_Name);
      jLabel_Name.setText("Name");

      jButton_Done.setMnemonic('O');
      jButton_Done.setText("Ok");
      jButton_Done.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent e) {
            try {
               if (storeData()) {
                  setVisible(false);
                  final Composition comp = getMaterial();
                  if ((mSession != null) && (comp != null) && (comp.getElementCount() > 0))
                     if (!ignoredCompNames(comp)) {
                        final Composition c2 = mSession.findStandard(comp.toString());
                        boolean update = false;
                        if (!mInhibitUpdate) {
                           boolean ask = false;
                           if (c2 == null)
                              update = true;
                           else {
                              if (comp instanceof Material) {
                                 if (c2 instanceof Material)
                                    ask = (FromSI.gPerCC(Math.abs(((Material) c2).getDensity() - ((Material) comp).getDensity())) > 0.009);
                                 else
                                    ask = true;
                              } else
                                 ask = c2 instanceof Material;
                              if (!c2.almostEquals(comp, 1.0e-5))
                                 ask = true;
                              if (ask)
                                 update = JOptionPane.showConfirmDialog(MaterialsCreator.this,
                                       "<html>The material called <i>" + comp.toString() + "</i> is currently defined as<br>&nbsp;&nbsp;"
                                             + c2.descriptiveString(false) + "<br>Update the database to define <i>" + comp.toString()
                                             + "</i> as<br>&nbsp;&nbsp;" + comp.descriptiveString(false),
                                       "Material Editor", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                           }
                           if (update)
                              mSession.addStandard(comp);
                        }
                     }
               }
            } catch (final Exception ex) {
               ex.printStackTrace(System.err);
            }
         }
      });
      getRootPane().setDefaultButton(jButton_Done);

      jButton_Cancel.setText("Cancel");
      jButton_Cancel.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent e) {
            closeDialog();
         }
      });
      addCancelByEscapeKey();

      jLabel_Density.setDisplayedMnemonic('D');
      jLabel_Density.setHorizontalAlignment(SwingConstants.RIGHT);
      jLabel_Density.setHorizontalTextPosition(SwingConstants.TRAILING);
      jLabel_Density.setLabelFor(jTextField_Density);
      jLabel_Density.setText("Density");

      jTextField_Density.setText("5.0");
      jTextField_Density.setToolTipText("Enter the density of the material");
      jTextField_Density.setVerifyInputWhenFocusTarget(true);
      jTextField_Density.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(final FocusEvent e) {
            jTextField_Density_focusGained(e);
         }
      });
      mRequireDensity = true;
      setRequireDensity(false);

      jButton_Add.setToolTipText("Add the element and quantity to the current material");
      jButton_Add.setMnemonic('A');
      jButton_Add.setText("Add");
      jButton_Add.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent e) {
            jButton_Add_actionPerformed(e);
         }
      });

      jButton_Delete.setToolTipText("Remove the currently selected item from the material");
      jButton_Delete.setMnemonic('D');
      jButton_Delete.setText("Delete");
      jButton_Delete.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent e) {
            jButton_Delete_actionPerformed(e);
         }
      });

      jButton_Clear.setToolTipText("Remove ALL elements from the material");
      jButton_Clear.setMnemonic('C');
      jButton_Clear.setText("Clear");
      jButton_Clear.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(final ActionEvent e) {
            jButton_Clear_actionPerformed(e);
         }
      });

      jLabel_Element.setLabelFor(jTextField_Element);
      jLabel_Element.setText("Element:");

      jTextField_Element.setText("");
      jTextField_Element.setToolTipText("Enter the symbol of a valid element");
      jTextField_Element.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(final FocusEvent e) {
            jTextField_Element_focusGained(e);
         }
      });

      jLabel_Quantity.setLabelFor(jTextField_Quantity);
      jLabel_Quantity.setText("Quantity:");

      jTextField_Quantity.setText("100%");
      jTextField_Quantity.setToolTipText("Enter the composition of the element");
      jTextField_Quantity.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(final FocusEvent e) {
            jTextField_Quantity_focusGained(e);
         }
      });

      {
         final CellConstraints cc = new CellConstraints();
         JPanel top, units, amount, btns;
         {
            top = new JPanel(new FormLayout("right:pref:grow, 5dlu, 28dlu, 2dlu, 100dlu, 2dlu, pref", "pref, 2dlu, pref"));
            top.add(jLabel_Name, cc.xy(1, 1));
            top.add(jLabel_Density, cc.xy(1, 3));
            top.add(jTextField_Name, cc.xyw(3, 1, 3));
            top.add(jButton_Search, cc.xy(7, 1));
            top.add(jTextField_Density, cc.xy(3, 3));
            top.add(jLabel_gPerCC, cc.xy(5, 3));
         }
         {
            units = new JPanel(new FlowLayout());
            units.add(jRadioButton_Weight);
            units.add(jRadioButton_Atomic);
         }
         {
            amount = new JPanel(new FormLayout("pref:grow, pref, 5dlu, 40dlu, 20dlu, pref, 5dlu, 40dlu, pref:grow", "pref"));
            amount.add(jLabel_Element, cc.xy(2, 1));
            amount.add(jTextField_Element, cc.xy(4, 1));
            amount.add(jLabel_Quantity, cc.xy(6, 1));
            amount.add(jTextField_Quantity, cc.xy(8, 1));
         }
         {
            final ButtonBarBuilder bbb = new ButtonBarBuilder();
            bbb.addGlue();
            bbb.addButton(jButton_Add);
            bbb.addUnrelatedGap();
            bbb.addButton(jButton_Delete);
            bbb.addUnrelatedGap();
            bbb.addButton(jButton_Clear);
            bbb.addGlue();
            btns = bbb.getPanel();
         }
         {
            final PanelBuilder content = new PanelBuilder(
                  new FormLayout("210dlu", "pref, pref, pref, pref, 5dlu, 80dlu, 5dlu, pref, 5dlu, pref, 4dlu, pref, 4dlu, pref"));
            content.addSeparator("Material", cc.xy(1, 1));
            content.add(top, cc.xy(1, 2));
            content.addSeparator("Mode", cc.xy(1, 3));
            content.add(units, cc.xy(1, 4));
            content.add(new JScrollPane(jTable_Composition), cc.xy(1, 6));
            content.add(amount, cc.xy(1, 8));
            content.add(btns, cc.xy(1, 10));
            content.addSeparator("", cc.xy(1, 12));
            final ButtonBarBuilder bbb = new ButtonBarBuilder();
            bbb.addGlue();
            bbb.addButton(jButton_Done, jButton_Cancel);
            content.add(bbb.build(), cc.xy(1, 14));
            final JPanel contentPanel = content.getPanel();
            contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            setContentPane(contentPanel);
         }
      }
      pack();
      setResizable(false);
   }

   private void addCancelByEscapeKey() {
      final String CANCEL_ACTION_KEY = "CANCEL_ACTION_KEY";
      final int noModifiers = 0;
      final KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, noModifiers, false);
      final InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
      inputMap.put(escapeKey, CANCEL_ACTION_KEY);
      final AbstractAction cancelAction = new AbstractAction() {
         private static final long serialVersionUID = 7087340186187020376L;

         @Override
         public void actionPerformed(final ActionEvent e) {
            closeDialog();
         }
      };
      getRootPane().getActionMap().put(CANCEL_ACTION_KEY, cancelAction);
   }

   /**
    * getMaterial - returns the current material.
    * 
    * @return Composition
    */
   public Composition getMaterial() {
      if (isVisible())
         return storeData() ? mCurrentMaterial : null;
      else
         return mWasCanceled ? null : mCurrentMaterial;
   }

   /**
    * setMaterial - Replaces the current material with the one material set. The
    * dialog, open or not, will change appropriately to reflect the change
    * 
    * @param comp
    *           Composition - the new material to replace the old one
    */
   public void setMaterial(final Composition comp) {
      assert comp != null;
      mCurrentMaterial = comp.clone();
      jTable_Composition.setModel(new CompositionTableModel(mCurrentMaterial, false));
      {
         final boolean b = mCurrentMaterial instanceof Material;
         final String density = b ? mDefaultFmt.format(FromSI.gPerCC(((Material) mCurrentMaterial).getDensity())) : "";
         jTextField_Density.setText(density);
      }
      if (mCurrentMaterial.getName().isEmpty())
         jTextField_Name.setText("Enter material name here");
      else
         jTextField_Name.setText(mCurrentMaterial.getName());

   }

   void jButton_Add_actionPerformed(final ActionEvent e) {
      final int atomicNumber = Element.atomicNumberForName(jTextField_Element.getText());
      double Quantity = 0.0;
      try {
         final NumberFormat nf = NumberFormat.getInstance();
         String Quantitystr = jTextField_Quantity.getText();
         if (Quantitystr.endsWith("%"))
            Quantitystr = Quantitystr.substring(0, (Quantitystr.indexOf("%")));
         else if (Quantitystr.endsWith("atom(s)"))
            Quantitystr = Quantitystr.substring(0, Quantitystr.indexOf("atom(s)"));
         Quantity = nf.parse(Quantitystr).doubleValue();
      } catch (final Exception ex) {
         jTextField_Quantity.grabFocus();
         jTextField_Quantity.selectAll();
         return;
      }
      if (Element.isValid(atomicNumber) && (!mCurrentMaterial.containsElement(Element.byAtomicNumber(atomicNumber)))) {
         if (buttonGroup_units.isSelected(jRadioButton_Weight.getModel()))
            mCurrentMaterial.addElement(atomicNumber, Quantity / 100);
         else if (buttonGroup_units.isSelected(jRadioButton_Atomic.getModel()))
            mCurrentMaterial.addElementByStoiciometry(Element.byAtomicNumber(atomicNumber), Quantity);
         jTable_Composition.setModel(new CompositionTableModel(mCurrentMaterial, false));
         formatQuantityTextField();
         jTextField_Element.grabFocus();
         jTextField_Element.selectAll();
      } else if (Element.isValid(atomicNumber)) {
         jTextField_Quantity.grabFocus();
         jTextField_Quantity.selectAll();
      } else {
         jTextField_Element.grabFocus();
         jTextField_Element.selectAll();
      }
   }

   void jTextField_Quantity_focusGained(final FocusEvent e) {
      jTextField_Quantity.selectAll();
   }

   void jTextField_Element_focusGained(final FocusEvent e) {
      jTextField_Element.selectAll();
   }

   void jTextField_Density_focusGained(final FocusEvent e) {
      jTextField_Density.selectAll();
   }

   private boolean storeData() {
      boolean res = true;
      if (jTextField_Name.getText().length() > 0) {
         mCurrentMaterial.setName(jTextField_Name.getText());
         jTextField_Name.setBackground(SystemColor.text);
      } else {
         res = false;
         jTextField_Name.selectAll();
         jTextField_Name.requestFocus();
         jTextField_Name.setBackground(Color.pink);
      }
      double den;
      try {
         final NumberFormat nf = NumberFormat.getInstance();
         den = ToSI.gPerCC(nf.parse(jTextField_Density.getText().trim()).doubleValue());
      } catch (final ParseException e) {
         den = Double.NaN;
      }
      if (den <= 0.0)
         den = Double.NaN;
      if (!Double.isNaN(den)) {
         if (!(mCurrentMaterial instanceof Material))
            mCurrentMaterial = new Material(mCurrentMaterial, den);
         else
            ((Material) mCurrentMaterial).setDensity(den);
         jTextField_Density.setBackground(SystemColor.text);
      } else if (mRequireDensity) {
         res = false;
         jTextField_Density.selectAll();
         jTextField_Density.requestFocus();
         jTextField_Density.setBackground(Color.pink);
      } else
         mCurrentMaterial = new Composition(mCurrentMaterial);
      return res;
   }

   void this_windowOpening(final WindowEvent e) {
      mWasCanceled = false;
   }

   void jButton_Delete_actionPerformed(final ActionEvent e) {
      final int Index = jTable_Composition.getSelectedRow();
      if (Index != -1) {
         final Element selected = (Element) jTable_Composition.getValueAt(Index, 0);
         mCurrentMaterial.removeElement(selected);
         jTable_Composition.setModel(new CompositionTableModel(mCurrentMaterial, false));
         formatQuantityTextField();
         jTextField_Element.grabFocus();
         jTextField_Element.selectAll();
      }
   }

   void jButton_Clear_actionPerformed(final ActionEvent e) {
      // Don't clear the name or density...
      final String density = jTextField_Density.getText();
      final String name = jTextField_Name.getText();
      final boolean b = (mCurrentMaterial != null ? mCurrentMaterial instanceof Material : jTextField_Density.isEnabled());
      setMaterial(b ? new Material(ToSI.gPerCC(5.0)) : new Composition());
      jTextField_Name.setText(name);
      jTextField_Density.setText(density);
      jTable_Composition.setModel(new CompositionTableModel(mCurrentMaterial, false));
      formatQuantityTextField();
      jTextField_Element.setText("");
      jTextField_Name.grabFocus();
      jTextField_Name.selectAll();
   }

   private void formatQuantityTextField() {
      if (buttonGroup_units.isSelected(jRadioButton_Weight.getModel()))
         jTextField_Quantity.setText(mDefaultFmt.format((100 - (100.0 * mCurrentMaterial.sumWeightFraction()))) + "%");
      else if (buttonGroup_units.isSelected(jRadioButton_Atomic.getModel()))
         jTextField_Quantity.setText("1 atom(s)");
   }

   void jTextField_Name_focusGained(final FocusEvent e) {
      jTextField_Name.selectAll();
   }

   void jTextField_Name_focusLost(final FocusEvent e) {
      if (mCurrentMaterial.getElementCount() == 0)
         searchDatabase();
   }

   private void searchDatabase() {
      Composition comp = null;
      final String name = jTextField_Name.getText();
      if (mSession != null)
         try {
            comp = mSession.findStandard(name);
            jButton_Done.requestFocus();
         } catch (final SQLException e1) {
            // assume it isn't in the database...
         }
      if (comp == null)
         try {
            comp = MaterialFactory.createCompound(name);
            if (mCurrentMaterial instanceof Material)
               setMaterial(new Material(comp, ((Material) mCurrentMaterial).getDensity()));
            else
               setMaterial(comp);
         } catch (final EPQException ex) {
            // Assume that it is a name not a compound
         }
      else {
         setMaterial(comp);
      }
      formatQuantityTextField();
   }

   /**
    * closeDialog
    */
   private void closeDialog() {
      mWasCanceled = true;
      setVisible(false);
   }

   public void setSession(final Session ses) {
      mSession = ses;
   }

   public Session getSession() {
      return mSession;
   }

   /**
    * Require that the user enter a valid density
    * 
    * @return boolean
    */
   public boolean requireDensity() {
      return mRequireDensity;
   }

   /**
    * Require that the user enter a valid density.
    * 
    * @param requireDensity
    */
   public void setRequireDensity(final boolean requireDensity) {
      if (mRequireDensity != requireDensity) {
         mRequireDensity = requireDensity;
         if (mRequireDensity) {
            jLabel_gPerCC.setText("<html>g/cm<sup>3</sup> (required)");
            jTextField_Density.setToolTipText("<html>Must be greater than zero.");
         } else {
            jLabel_gPerCC.setText("<html>g/cm<sup>3</sup> (optional)");
            jTextField_Density.setToolTipText("<html>Leave blank to not specify a density.");
         }
      }
   }

   public static Composition createMaterial(final Frame parent, final boolean requireDensity) {
      return editMaterial(parent, null, requireDensity);
   }

   public static Composition createMaterial(final Dialog parent, final boolean requireDensity) {
      return editMaterial(parent, null, requireDensity);
   }

   public static Composition createMaterial(final Frame parent, final String title, final boolean requireDensity) {
      return editMaterial(parent, null, null, title, requireDensity);
   }

   public static Composition createMaterial(final Dialog parent, final String title, final boolean requireDensity) {
      return editMaterial(parent, null, null, title, requireDensity);
   }

   public static Composition createMaterial(final Frame parent, final Session ses, final boolean requireDensity) {
      return editMaterial(parent, null, ses, requireDensity);
   }

   public static Composition createMaterial(final Frame parent, final Session ses, final String title, final boolean requireDensity) {
      return editMaterial(parent, null, ses, title, requireDensity);
   }

   public static Composition createMaterial(final Dialog parent, final Session ses, final boolean requireDensity) {
      return editMaterial(parent, null, ses, requireDensity);
   }

   public static Composition createMaterial(final Dialog parent, final Session ses, final String title, final boolean requireDensity) {
      return editMaterial(parent, null, ses, title, requireDensity);
   }

   private static class MCRunnable implements Runnable {
      private final MaterialsCreator mCreator;
      private Composition mComposition;

      public MCRunnable(final Window parent, final Composition comp, final Session ses, final boolean requireDensity) {
         mCreator = new MaterialsCreator(parent, "Material Editor");
         mCreator.setModal(true);
         mCreator.setRequireDensity(requireDensity);
         if (ses != null)
            mCreator.setSession(ses);
         mCreator.setLocationRelativeTo(parent);
         mComposition = comp;
      }

      public void setTitle(final String title) {
         mCreator.setTitle(title);
      }

      @Override
      public void run() {
         try {
            if (mComposition != null)
               mCreator.setMaterial(mComposition);
            mCreator.setModal(true);
            mCreator.setVisible(true);
            mComposition = mCreator.getMaterial();
         } catch (final RuntimeException e) {
            System.err.print(e.getMessage());
            mCreator.setVisible(false);
         }
      }
   }

   public static Composition editMaterial(final Frame parent, final Composition comp, final boolean requireDensity) {
      return editMaterial(parent, comp, null, requireDensity);
   }

   public static Composition editMaterial(final Window window, final Composition comp, final Session ses, final boolean requireDensity) {
      assert window != null;
      try {
         final MCRunnable t = new MCRunnable(window, comp, ses, requireDensity);
         if (SwingUtilities.isEventDispatchThread())
            t.run();
         else
            SwingUtilities.invokeAndWait(t);
         return t.mComposition;
      } catch (final Exception e) {
         return null;
      }
   }

   public static Composition editMaterial(final Frame parent, final Composition comp, final Session ses, final String title,
         final boolean requireDensity) {
      try {
         final MCRunnable t = new MCRunnable(parent, comp, ses, requireDensity);
         if (title != null)
            t.setTitle(title);
         if (SwingUtilities.isEventDispatchThread())
            t.run();
         else
            SwingUtilities.invokeAndWait(t);
         return t.mComposition;
      } catch (final Exception e) {
         return null;
      }
   }

   public static Composition editMaterial(final Dialog parent, final Composition comp, final boolean requireDensity) {
      return editMaterial(parent, comp, null, requireDensity);
   }

   public static Composition editMaterial(final Dialog parent, final Composition comp, final Session ses, final boolean requireDensity) {
      try {
         final MCRunnable t = new MCRunnable(parent, comp, ses, requireDensity);
         if (SwingUtilities.isEventDispatchThread())
            t.run();
         else
            SwingUtilities.invokeAndWait(t);
         return t.mComposition;
      } catch (final Exception e) {
         System.err.print(e.getMessage());
         return null;
      }
   }

   public static Composition editMaterial(final Dialog parent, final Composition comp, final Session ses, final String title,
         final boolean requireDensity) {
      try {
         final MCRunnable t = new MCRunnable(parent, comp, ses, requireDensity);
         t.setTitle(title);
         if (SwingUtilities.isEventDispatchThread())
            t.run();
         else
            SwingUtilities.invokeAndWait(t);
         return t.mComposition;
      } catch (final Exception e) {
         System.err.print(e.getMessage());
         return null;
      }
   }

   /**
    * Don't write the material to the database...
    */
   public void setInhibitUpdate() {
      mInhibitUpdate = true;
   }
}
