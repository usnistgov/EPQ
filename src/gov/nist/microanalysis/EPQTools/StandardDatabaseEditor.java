package gov.nist.microanalysis.EPQTools;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gov.nist.microanalysis.EPQDatabase.Session;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.StandardsDatabase2;
import gov.nist.microanalysis.EPQLibrary.StandardsDatabase2.StandardBlock2;
import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * Provides a GUI mechanism to edit StandardDatabase2 objects.
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
public class StandardDatabaseEditor extends JDialog {

   private static final double DEFAULT_TOL = 1.0e-5;

   private static final String MRU_EXPORT_PATH = "MRU Export Path";

   private static final String TITLE = "Import standard block";
   private static final String BLOCK_EXTENSION = "sbx";
   private static final SimpleFileFilter BLOCK_FILE_FILTER = new SimpleFileFilter(BLOCK_EXTENSION, "Standard block XML descriptor");
   private static final String DATABASE_EXTENSION = "sd2.xml";
   private static final SimpleFileFilter DATABASE_FILE_FILTER = new SimpleFileFilter(DATABASE_EXTENSION, "Standard block XML descriptor");

   private static final long serialVersionUID = -6480283320058263368L;

   private final StandardsDatabase2 mDatabase;
   private Session mSession = null;
   private boolean mOk = false;

   private final JTextField jTextField_Name = new JTextField();
   private final JButton jButton_Menu = new JButton("Tools");
   private final JComboBox<StandardBlock2> jComboBox_Blocks = new JComboBox<StandardBlock2>();
   private final JButton jButton_AddBlock = new JButton("+");
   private final JButton jButton_RemoveBlock = new JButton("-");
   private final JTextField jTextField_Material = new JTextField();
   private final JList<Composition> jList_Materials = new JList<Composition>();
   private final JButton jButton_AddMaterial = new JButton("+");
   private final JButton jButton_RemoveMaterial = new JButton("-");
   private final JButton jButton_Ok = new JButton("Ok");
   private final JButton jButton_Cancel = new JButton("Cancel");

   private final JPopupMenu jPopupMenu_Tools = new JPopupMenu();
   private final JMenuItem jMenuItem_ImportLibrary = new JMenuItem("Import standard library");
   private final JMenuItem jMenuItem_ValidateCompositions = new JMenuItem("Validate compositions");
   private final JMenuItem jMenuItem_Import = new JMenuItem("Import block");
   private final JMenuItem jMenuItem_Export = new JMenuItem("Export block");

   /**
    * Constructs a StandardDatabaseEditor
    * 
    * @param arg0
    */
   public StandardDatabaseEditor(Frame arg0, StandardsDatabase2 sdb, Session sess) {
      super(arg0, Dialog.ModalityType.APPLICATION_MODAL);
      setTitle("Standards Database");
      mDatabase = sdb.clone();
      mSession = sess;
      initialize();
      update();
      pack();
      selectMatEditor();
   }

   private void importAction() {
      final JFileChooser jfc = new JFileChooser();
      jfc.setAcceptAllFileFilterUsed(true);
      jfc.setFileFilter(BLOCK_FILE_FILTER);
      final Preferences pref = Preferences.userNodeForPackage(StandardDatabaseEditor.class);
      final File path = new File(pref.get(MRU_EXPORT_PATH, System.getProperty("user.home")));
      jfc.setCurrentDirectory(path.isDirectory() ? path : null);
      if (jfc.showOpenDialog(StandardDatabaseEditor.this) == JFileChooser.APPROVE_OPTION)
         try {
            try (final FileInputStream fis = new FileInputStream(jfc.getSelectedFile())) {
               final Object obj = EPQXStream.getInstance().fromXML(fis);
               if (obj instanceof StandardBlock2) {
                  final StandardBlock2 sb = (StandardBlock2) obj;
                  // Normalize the compositional data to ensure data
                  // integrity...
                  final ArrayList<Composition> comps = new ArrayList<Composition>();
                  for (final Composition comp : sb.getStandards()) {
                     final Composition newComp = validate(comp);
                     if (newComp != null)
                        comps.add(newComp);
                  }
                  if (comps.size() > 0) {
                     String blockName = sb.toString();
                     for (int i = 1; mDatabase.getBlock(blockName) != null; ++i)
                        blockName = sb.toString() + "_" + Integer.toString(i);
                     if (!blockName.equals(sb.toString())) {
                        final String msg = "<html>A standard block named <i>" + sb.toString() + "</i> already exists.<br>"
                              + "Create a new block named <i>" + blockName + "</i>?";
                        if (JOptionPane.showConfirmDialog(StandardDatabaseEditor.this, msg, TITLE, JOptionPane.YES_NO_OPTION,
                              JOptionPane.INFORMATION_MESSAGE) == JOptionPane.NO_OPTION)
                           return;
                     }
                     final StandardBlock2 newBlock = mDatabase.addBlock(blockName);
                     for (final Composition comp : comps)
                        newBlock.addStandard(comp);
                  } else
                     JOptionPane.showConfirmDialog(StandardDatabaseEditor.this, "Nothing to import.", TITLE, JOptionPane.OK_OPTION,
                           JOptionPane.ERROR_MESSAGE);
               }
            }
            pref.put(MRU_EXPORT_PATH, jfc.getSelectedFile().getParent());
         } catch (final IOException e) {
            ErrorDialog.createErrorMessage(StandardDatabaseEditor.this, "Export block", e);
         }
      update();
      updateMaterials();
   }

   public Composition validate(Composition incoming) {
      Composition res = incoming;
      try {
         final Composition dbCopy = mSession.findStandard(incoming.getName());
         if (dbCopy == null)
            // Add to database and return incoming
            mSession.addStandard(incoming);
         else if (!dbCopy.asComposition().almostEquals(incoming.asComposition(), DEFAULT_TOL)) {
            String newName = "";
            Composition renamed = null;
            for (int i = 1; i < 1000; ++i) {
               newName = incoming.getName() + "_" + Integer.toString(i);
               renamed = mSession.findStandard(newName);
               if ((renamed == null) || incoming.almostEquals(renamed, DEFAULT_TOL))
                  break;
            }
            final String msg = "<html><b>WARNING:</b> The imported composition:<br>&nbsp;&nbsp;&nbsp;" + incoming.weightPercentString(false)
                  + "<br>does not match:<br>&nbsp;&nbsp;&nbsp;" + dbCopy.weightPercentString(false)
                  + "<br>which is currently in the database.<br>&nbsp;&nbsp;&nbsp;The imported composition will be renamed " + newName + ".";
            if (JOptionPane.showConfirmDialog(StandardDatabaseEditor.this, msg, TITLE, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
               incoming.setName(newName);
               if (mSession.findStandard(newName) == null)
                  mSession.addStandard(incoming);
               res = incoming;
            } else
               res = null;
         }
      } catch (final Exception e) {
         ErrorDialog.createErrorMessage(StandardDatabaseEditor.this, "Standard block import", e);
         res = null;
      }
      return res;
   }

   private void exportAction() {
      if (jComboBox_Blocks.getSelectedItem() instanceof StandardBlock2) {
         final StandardBlock2 sb = (StandardBlock2) jComboBox_Blocks.getSelectedItem();
         final JFileChooser jfc = new JFileChooser();
         jfc.setAcceptAllFileFilterUsed(true);
         final SimpleFileFilter sff = BLOCK_FILE_FILTER;
         jfc.setFileFilter(sff);
         final Preferences pref = Preferences.userNodeForPackage(StandardDatabaseEditor.class);
         final File path = new File(pref.get(MRU_EXPORT_PATH, System.getProperty("user.home")), sb.toString() + "." + BLOCK_EXTENSION);
         jfc.setSelectedFile(path);
         if (jfc.showSaveDialog(StandardDatabaseEditor.this) == JFileChooser.APPROVE_OPTION)
            try {
               final File file = sff.forceExtension(jfc.getSelectedFile());
               try (final FileOutputStream fos = new FileOutputStream(file)) {
                  EPQXStream.getInstance().toXML(sb, fos);
               }
               pref.put(MRU_EXPORT_PATH, jfc.getSelectedFile().getParent());
            } catch (final IOException e) {
               ErrorDialog.createErrorMessage(StandardDatabaseEditor.this, "Export block", e);
            }
      }
   }

   private void importLibraryAction() {
      final JFileChooser jfc = new JFileChooser();
      jfc.setAcceptAllFileFilterUsed(true);
      jfc.setFileFilter(DATABASE_FILE_FILTER);
      final Preferences pref = Preferences.userNodeForPackage(StandardDatabaseEditor.class);
      final File path = new File(pref.get(MRU_EXPORT_PATH, System.getProperty("user.home")));
      jfc.setCurrentDirectory(path.isDirectory() ? path : null);
      if (jfc.showOpenDialog(StandardDatabaseEditor.this) == JFileChooser.APPROVE_OPTION) {
         final File sel = jfc.getSelectedFile();
         final StandardsDatabase2 sdb = StandardsDatabase2.read(sel);
         merge(sdb);
         update();
      }
   }

   private void validateCompositionsAction() {
      int cx = 0;

      for (final Composition comp : new ArrayList<Composition>(mDatabase.allCompositions()))
         try {
            final Composition inDb = mSession.findStandard(comp.getName());
            if (inDb == null)
               mSession.addStandard(comp);
            else if (!inDb.asComposition().almostEquals(comp.asComposition(), DEFAULT_TOL)) {
               final StringBuffer sb = new StringBuffer();
               sb.append("Redefining " + comp.getName() + "\n");
               sb.append("   Old: " + comp.weightPercentString(false) + "\n");
               sb.append("   New: " + inDb.weightPercentString(false) + "\n");
               JOptionPane.showMessageDialog(StandardDatabaseEditor.this, sb.toString());
               mDatabase.replace(inDb);
               ++cx;
            }
         } catch (final SQLException e) {
            // Ignore it...
         }
      if (cx == 0)
         JOptionPane.showMessageDialog(StandardDatabaseEditor.this, "No compositions in the standards database needed to be updated.");
      else
         JOptionPane.showMessageDialog(StandardDatabaseEditor.this, Integer.toString(cx) + " compositions were updated in the standard database.");
   }

   private void merge(StandardsDatabase2 sdb) {
      for (final StandardBlock2 sb : sdb.getBlocks()) {
         String name = sb.toString();
         for (int i = 1; mDatabase.getBlockNames().contains(name); ++i)
            name = sb.toString() + "[" + i + "]";
         final StandardBlock2 newBlk = mDatabase.addBlock(name);
         for (final Composition comp : sb.getStandards()) {
            final Composition newComp = validate(comp);
            if (newComp != null)
               newBlk.addStandard(newComp);
         }
      }
   }

   private void deleteBlockAction() {
      final Object obj = jComboBox_Blocks.getSelectedItem();
      if (obj instanceof StandardBlock2) {
         final StandardBlock2 sb = (StandardBlock2) obj;
         mDatabase.removeBlock(sb);
         update();
         updateMaterials();
      }
   }

   private void deleteMaterialAction() {
      Object obj = jList_Materials.getSelectedValue();
      if (obj instanceof Composition) {
         final Composition comp = (Composition) obj;
         obj = jComboBox_Blocks.getSelectedItem();
         if (obj instanceof StandardBlock2) {
            final StandardBlock2 sb = (StandardBlock2) obj;
            sb.removeStandard(comp.getName());
            updateMaterials();
         }
      }
   }

   private void addBlockAction() {
      final String s = (String) JOptionPane.showInputDialog(StandardDatabaseEditor.this, "Please, specify a name for the block:", "Input Dialog",
            JOptionPane.PLAIN_MESSAGE, null, null, "");
      if ((s != null) && (s.length() > 0)) {
         if (mDatabase.getBlockNames().contains(s)) {
            final String msg = "The standards database already contains a block called: " + s;
            ErrorDialog.createErrorMessage(StandardDatabaseEditor.this, "Standards Editor", msg, msg);
            return;
         }
         final StandardBlock2 newBlk = mDatabase.addBlock(s);
         update();
         jComboBox_Blocks.setSelectedItem(newBlk);
         selectMatEditor();
      }
   }

   private void addMaterialAction() {
      final StandardBlock2 sb = (StandardBlock2) jComboBox_Blocks.getSelectedItem();
      if (sb != null) {
         final String matStr = jTextField_Material.getText();
         Composition comp = null;
         if (matStr.length() > 0)
            try {
               comp = mSession.findStandard(matStr);
               if ((comp == null) || (comp.getElementCount() == 0))
                  comp = MaterialFactory.createCompound(matStr);
            } catch (final Exception e) {
               comp = null;
            }
         if ((comp == null) || (comp.getElementCount() == 0))
            comp = new Composition(new Element[0], new double[0], matStr);
         comp = MaterialsCreator.editMaterial(StandardDatabaseEditor.this, comp, mSession, "Add " + matStr, false);
         if ((comp != null) && (comp.getElementCount() > 0)) {
            comp.setName(matStr);
            boolean add = true;
            if (sb.has(comp.toString()))
               add = JOptionPane.showConfirmDialog(StandardDatabaseEditor.this, "Add a duplicate of " + comp.toString() + "?", "Standards database",
                     JOptionPane.YES_NO_OPTION) == JOptionPane.YES_NO_CANCEL_OPTION;
            if (add) {
               sb.addStandard(comp);
               update();
               jComboBox_Blocks.setSelectedItem(sb);
               updateMaterials();
               jList_Materials.setSelectedValue(comp, true);
            }
            selectMatEditor();
         }
      }
   }

   private void selectMatEditor() {
      jTextField_Material.selectAll();
      jTextField_Material.requestFocus();
   }

   private void initialize() {

      jTextField_Name.addFocusListener(new FocusAdapter() {
         @Override
         public void focusLost(FocusEvent e) {
            mDatabase.rename(jTextField_Name.getText());
         }
      });

      jMenuItem_Import.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent arg0) {
            importAction();
         }
      });

      jMenuItem_Export.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent arg0) {
            exportAction();
         }
      });

      jMenuItem_ImportLibrary.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            importLibraryAction();
         }
      });

      jMenuItem_ValidateCompositions.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            validateCompositionsAction();
         }
      });

      jPopupMenu_Tools.add(jMenuItem_Import);
      jPopupMenu_Tools.add(jMenuItem_Export);
      jPopupMenu_Tools.addSeparator();
      jPopupMenu_Tools.add(jMenuItem_ImportLibrary);
      jPopupMenu_Tools.add(jMenuItem_ValidateCompositions);

      jButton_Menu.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent arg0) {
            final Rectangle r = jButton_Menu.getBounds();
            jPopupMenu_Tools.show(jButton_Menu.getParent(), r.x, r.y + r.height);
         }
      });

      jComboBox_Blocks.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            updateMaterials();
         }
      });
      jList_Materials.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      jList_Materials.addListSelectionListener(new ListSelectionListener() {

         @Override
         public void valueChanged(ListSelectionEvent e) {
            final Object sel = jList_Materials.getSelectedValue();
            final StringBuffer sb = new StringBuffer();
            if (sel instanceof Composition) {
               final Composition comp = (Composition) sel;
               final HalfUpFormat nf = new HalfUpFormat("0.0000");
               sb.append(
                     "<HTML><table><tr><th align=\"left\">Element</th><th align=\"center\">Mass<br/>Fraction</th><th align=\"center\">Atomic<br/>Fraction</th></tr>");
               for (final Element elm : comp.getElementSet()) {
                  sb.append("<tr><td align=\"left\">" + elm.toString() + "</td>");
                  sb.append("<td align=\"center\">" + nf.format(comp.weightFraction(elm, false)) + "</td>");
                  sb.append("<td align=\"center\">" + nf.format(comp.atomicPercent(elm)) + "</td>");
                  sb.append("</tr>");
               }
               sb.append("<tr><th align=\"right\">Total</th>");
               sb.append("<td align=\"center\">" + nf.format(comp.sumWeightFraction()) + "</td>");
               sb.append("<td align=\"center\">1.0000</td>");
               sb.append("</tr>");
               sb.append("</table>");
            }
            jList_Materials.setToolTipText(sb.toString());
         }
      });

      jButton_AddMaterial.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            addMaterialAction();
         }
      });

      jButton_AddBlock.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            addBlockAction();
         }
      });

      jButton_RemoveMaterial.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            deleteMaterialAction();

         }
      });

      jButton_RemoveBlock.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            deleteBlockAction();
         }
      });

      jButton_Ok.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            mOk = true;
            setVisible(false);
         }
      });

      jButton_Cancel.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent e) {
            mOk = false;
            setVisible(false);
         }
      });

      final FormLayout fl = new FormLayout("100dlu, 5dlu, pref, 5dlu, pref",
            "pref, 5dlu, pref, 10dlu, pref, 5dlu, pref, 10dlu, pref, 5dlu, 100dlu, 5dlu, pref");
      final PanelBuilder pb = new PanelBuilder(fl);
      final CellConstraints cc = new CellConstraints();
      final int fullWidth = 5;
      int row = 1;
      pb.addSeparator("Name", cc.xyw(1, row, fullWidth));
      row += 2;
      pb.add(jTextField_Name, cc.xy(1, row));
      pb.add(jButton_Menu, cc.xyw(3, row, 3));
      row += 2;
      pb.addSeparator("Standard Block", cc.xyw(1, row, fullWidth));
      row += 2;
      pb.add(jComboBox_Blocks, cc.xy(1, row));
      pb.add(jButton_AddBlock, cc.xy(3, row));
      pb.add(jButton_RemoveBlock, cc.xy(5, row));
      row += 2;
      pb.addSeparator("Materials", cc.xyw(1, row, fullWidth));
      row += 2;
      pb.add(new JScrollPane(jList_Materials), cc.xy(1, row));
      final JPanel panel = new JPanel();
      panel.setLayout(new FormLayout("pref, 5dlu, pref", "pref, 5dlu, pref"));
      panel.add(jTextField_Material, cc.xyw(1, 1, 3));
      panel.add(jButton_AddMaterial, cc.xy(1, 3));
      panel.add(jButton_RemoveMaterial, cc.xy(3, 3));
      pb.add(panel, cc.xyw(3, row, 3));
      row += 2;
      final ButtonBarBuilder bbb = new ButtonBarBuilder();
      bbb.addGlue();
      bbb.addButton(jButton_Ok, jButton_Cancel);
      final JPanel btns = bbb.build();
      pb.add(btns, cc.xyw(1, row, fullWidth));

      final JPanel p = pb.getPanel();
      final int border = 5;
      p.setBorder(BorderFactory.createEmptyBorder(border, border, border, border));
      add(pb.getPanel(), BorderLayout.CENTER);
      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
   }

   private void update() {
      jTextField_Name.setText(mDatabase.toString());
      final StandardBlock2[] materials = mDatabase.getBlocks().toArray(new StandardBlock2[0]);
      final DefaultComboBoxModel<StandardBlock2> dcbm = new DefaultComboBoxModel<StandardBlock2>(materials);
      jComboBox_Blocks.setModel(dcbm);
      updateMaterials();
   }

   private void updateMaterials() {
      final StandardBlock2 sb = (StandardBlock2) jComboBox_Blocks.getSelectedItem();
      final DefaultListModel<Composition> dlm = new DefaultListModel<Composition>();
      if (sb != null)
         for (final Composition c : sb.getStandards())
            dlm.addElement(c);
      jList_Materials.setModel(dlm);
      jList_Materials.setToolTipText("");
   }

   public StandardsDatabase2 getDatabase() {
      return mDatabase;
   }

   public boolean isOk() {
      return mOk;
   }
}
