package gov.nist.microanalysis.EPQDatabase;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.EPQLibrary.Detector.DetectorCalibration;
import gov.nist.microanalysis.EPQLibrary.Detector.DetectorProperties;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSCalibration;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.EPQLibrary.Detector.ElectronProbe;
import gov.nist.microanalysis.EPQTools.ErrorDialog;
import gov.nist.microanalysis.EPQTools.MaterialsCreator;
import gov.nist.microanalysis.EPQTools.SpecDisplay;
import gov.nist.microanalysis.EPQTools.SpectrumFileChooser;
import gov.nist.microanalysis.EPQTools.SpectrumPropertyPanel;
import gov.nist.microanalysis.Utility.HTMLList;
import gov.nist.microanalysis.Utility.HalfUpFormat;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ProgressMonitor;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * <p>
 * A dialog to assist in importing spectra into the database defined by the
 * Session class.
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
public class ImportDialog
   extends JDialog {
   private static final long serialVersionUID = 4632393517615160681L;

   final private Session mSession;
   private final JComboBox<ElectronProbe> jComboBox_Instrument = new JComboBox<ElectronProbe>();
   private final JComboBox<DetectorProperties> jComboBox_Detector = new JComboBox<DetectorProperties>();
   private final JComboBox<DetectorCalibration> jComboBox_Calibration = new JComboBox<DetectorCalibration>();
   private final JComboBox<String> jComboBox_Project = new JComboBox<String>();
   private final JButton jButton_NewProject = new JButton("New");
   private final JComboBox<String> jComboBox_Operator = new JComboBox<String>();
   private final JButton jButton_NewOperator = new JButton("New");
   private final JList<ISpectrumData> jList_Spectra = new JList<ISpectrumData>();
   private final JButton jButton_Select = new JButton("Select");
   private final JButton jButton_Remove = new JButton("Remove");
   private final JButton jButton_AsStandard = new JButton("As standard");
   private final JButton jButton_Properties = new JButton("Properties");
   private final JButton jButton_Ok = new JButton("Ok");
   private final JButton jButton_Cancel = new JButton("Cancel");
   private final SpecDisplay jSpecDisplay_Preview = new SpecDisplay();
   private final JTextField jTextField_AcqTime = new JTextField();
   private final JTextField jTextField_ProbeCurrent = new JTextField();
   private final JTextField jTextField_LiveTime = new JTextField();
   private final JTextField jTextField_Composition = new JTextField();
   private JPopupMenu mSpecDisplay_Menu;

   private HTMLList mImported;

   private void updateDetector() {
      final Object obj = jComboBox_Instrument.getSelectedItem();
      if(obj instanceof ElectronProbe) {
         final ElectronProbe ep = (ElectronProbe) obj;
         final DefaultComboBoxModel<DetectorProperties> cbm = new DefaultComboBoxModel<DetectorProperties>(mSession.getDetectors(ep).toArray(new DetectorProperties[0]));
         if(cbm.getSize() > 0)
            cbm.setSelectedItem(cbm.getElementAt(0));
         jComboBox_Detector.setModel(cbm);
         updateCalibrations();
      }
   }

   private void updateCalibrations() {
      final Object obj = jComboBox_Detector.getSelectedItem();
      if(obj instanceof DetectorProperties) {
         final DetectorProperties dp = (DetectorProperties) obj;
         final DefaultComboBoxModel<DetectorCalibration> cbm = new DefaultComboBoxModel<DetectorCalibration>(mSession.getCalibrations(dp).toArray(new DetectorCalibration[0]));
         if(cbm.getSize() > 0)
            cbm.setSelectedItem(mSession.getMostRecentCalibration(dp));
         jComboBox_Calibration.setModel(cbm);
      }
   }

   private void initialize() {
      if(getContentPane() instanceof JPanel) {
         final JPanel p = (JPanel) getContentPane();
         p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      }
      // Add action listeners
      jComboBox_Instrument.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            updateDetector();
         }
      });

      jComboBox_Detector.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            updateCalibrations();
         }
      });

      jButton_Cancel.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            setVisible(false);
         }
      });

      jButton_NewOperator.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final PersonEditor pe = new PersonEditor(ImportDialog.this);
            pe.setLocationRelativeTo(ImportDialog.this);
            if(pe.execute()) {
               mSession.addPerson(pe.getName(), pe.getDetails());
               final ComboBoxModel<String> cbm = new DefaultComboBoxModel<String>(mSession.getPeople().keySet().toArray(new String[0]));
               cbm.setSelectedItem(pe.getName());
               jComboBox_Operator.setModel(cbm);
            }
         }
      });

      jButton_NewProject.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final ProjectEditor ge = new ProjectEditor(ImportDialog.this, mSession);
            ge.setLocationRelativeTo(ImportDialog.this);
            if(ge.execute()) {
               mSession.addProject(ge.getName(), ge.getClient(), ge.getDetails());
               {
                  final ComboBoxModel<String> cbm = new DefaultComboBoxModel<String>(mSession.getProjects().keySet().toArray(new String[0]));
                  cbm.setSelectedItem(ge.getName());
                  jComboBox_Project.setModel(cbm);
               }
               {
                  final Object sel = jComboBox_Operator.getSelectedItem();
                  final ComboBoxModel<String> cbm = new DefaultComboBoxModel<String>(mSession.getPeople().keySet().toArray(new String[0]));
                  cbm.setSelectedItem(sel);
                  jComboBox_Operator.setModel(cbm);
               }
            }
         }
      });

      jButton_Ok.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            importSpectra();
            setVisible(false);
         }
      });

      jButton_Remove.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jSpecDisplay_Preview.clearAllSpectra();
            final DefaultListModel<ISpectrumData> dlm = (DefaultListModel<ISpectrumData>) jList_Spectra.getModel();
            for(final Object obj : jList_Spectra.getSelectedValuesList())
               dlm.removeElement(obj);
         }
      });

      jButton_Select.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final Preferences userPref = Preferences.userNodeForPackage(ImportDialog.class);
            final String key = "FilePath";
            final String defPath = userPref.get(key, null);
            final SpectrumFileChooser sfc = new SpectrumFileChooser(ImportDialog.this, "Select spectra to import");
            if(defPath != null)
               sfc.getFileChooser().setCurrentDirectory(new File(defPath));
            sfc.setLocationRelativeTo(ImportDialog.this);
            sfc.setModal(true);
            sfc.setVisible(true);
            userPref.put(key, sfc.getFileChooser().getCurrentDirectory().getAbsolutePath());
            final ISpectrumData[] specs = sfc.getSpectra();
            if(specs.length > 0) {
               final DefaultListModel<ISpectrumData> dlm = (DefaultListModel<ISpectrumData>) jList_Spectra.getModel();
               for(final ISpectrumData spec : specs)
                  dlm.addElement(spec);
               final int[] indices = new int[specs.length];
               int i = 0;
               for(final ISpectrumData spec : specs) {
                  indices[i] = dlm.indexOf(spec);
                  ++i;
               }
               jList_Spectra.setModel(dlm);
               jList_Spectra.setSelectedIndices(indices);
            }
         }
      });

      jButton_AsStandard.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final List<ISpectrumData> sv = jList_Spectra.getSelectedValuesList();
            if(sv.size() > 0) {
               SpectrumProperties merged = null;
               for(final Object obj : sv)
                  merged = SpectrumProperties.merge(merged, ((ISpectrumData) obj).getProperties());
               Composition comp = merged.getCompositionWithDefault(SpectrumProperties.StandardComposition, null);
               comp = MaterialsCreator.editMaterial(ImportDialog.this, comp, mSession, false);
               for(final Object obj : sv)
                  ((ISpectrumData) obj).getProperties().setCompositionProperty(SpectrumProperties.StandardComposition, comp);
               jTextField_Composition.setText(comp != null ? comp.toString() : "-");
            }

         }
      });

      // Configure the controls
      jSpecDisplay_Preview.setAxisScalingMode(SpecDisplay.AXIS_MODE.LOG);
      mSpecDisplay_Menu = jSpecDisplay_Preview.getSimpleMenu();
      jSpecDisplay_Preview.addMouseListener(new MouseAdapter() {

         @Override
         public void mousePressed(MouseEvent e) {
            if(e.isPopupTrigger())
               mSpecDisplay_Menu.show(jSpecDisplay_Preview, e.getX(), e.getY());
         }

         @Override
         public void mouseReleased(MouseEvent e) {
            if(e.isPopupTrigger())
               mSpecDisplay_Menu.show(jSpecDisplay_Preview, e.getX(), e.getY());
         }
      });

      jList_Spectra.setModel(new DefaultListModel<ISpectrumData>());
      jList_Spectra.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
         @Override
         public void valueChanged(ListSelectionEvent e) {
            final ListModel<ISpectrumData> lm = jList_Spectra.getModel();
            jSpecDisplay_Preview.clearAllSpectra();
            final int[] sel = jList_Spectra.getSelectedIndices();
            if(sel.length > 0) { // Display merged
               SpectrumProperties merged = null;
               for(final int i : sel) {
                  jSpecDisplay_Preview.addSpectrum(lm.getElementAt(i));
                  merged = SpectrumProperties.merge(merged, lm.getElementAt(i).getProperties());
               }
               final NumberFormat nf = new HalfUpFormat("0.0");
               final java.util.Date tm = merged.getTimestampWithDefault(SpectrumProperties.AcquisitionTime, null);
               final DateFormat df = DateFormat.getDateTimeInstance();
               jTextField_AcqTime.setText(tm != null ? df.format(tm) : "-");
               final double fc = SpectrumUtils.getAverageFaradayCurrent(merged, Double.NaN);
               jTextField_ProbeCurrent.setText(Double.isNaN(fc) ? "-" : nf.format(fc) + " nA");
               jTextField_LiveTime.setText(merged.getTextWithDefault(SpectrumProperties.LiveTime, "-"));
               jTextField_Composition.setText(merged.getTextWithDefault(SpectrumProperties.StandardComposition, "-"));
               jTextField_Composition.select(0, 0);
            } else {
               jTextField_AcqTime.setText("");
               jTextField_ProbeCurrent.setText("");
               jTextField_LiveTime.setText("");
               jTextField_Composition.setText("");
            }
            jSpecDisplay_Preview.autoScaleV(100.0);
            jSpecDisplay_Preview.autoScaleH(0.0, 100.0);
         }
      });

      jButton_Properties.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final List<ISpectrumData> sels = jList_Spectra.getSelectedValuesList();
            if(sels.size() > 0) {
               final SpectrumPropertyPanel.PropertyDialog spp = new SpectrumPropertyPanel.PropertyDialog(ImportDialog.this, mSession);
               SpectrumProperties sp = null;
               for(final Object obj : sels)
                  if(obj instanceof ISpectrumData)
                     sp = SpectrumProperties.merge(sp, ((ISpectrumData) obj).getProperties());
               sp.addAll(ImportDialog.this.getDetector().getProperties());
               spp.addSpectrumProperties(sp);
               spp.setLocationRelativeTo(ImportDialog.this);
               spp.disableDetectorProperties();
               spp.setVisible(true);
               final SpectrumProperties newProps = SpectrumProperties.difference(spp.getSpectrumProperties(), sp);
               for(final Object obj : sels)
                  if(obj instanceof ISpectrumData)
                     ((ISpectrumData) obj).getProperties().addAll(newProps);
            }
            jList_Spectra.repaint();
         }
      });

      jTextField_AcqTime.setEditable(false);
      jTextField_ProbeCurrent.setEditable(false);
      jTextField_LiveTime.setEditable(false);
      jTextField_Composition.setEditable(false);

      // Initialize the data
      final Set<ElectronProbe> eps = mSession.getElectronProbes().keySet();
      {
         final ComboBoxModel<ElectronProbe> cbm = new DefaultComboBoxModel<ElectronProbe>(eps.toArray(new ElectronProbe[0]));
         if(cbm.getSize() > 0)
            cbm.setSelectedItem(cbm.getElementAt(0));
         jComboBox_Instrument.setModel(cbm);
      }
      {
         if(jComboBox_Instrument.getSelectedIndex() >= 0)
            updateDetector();
      }
      {
         final ComboBoxModel<String> cbm = new DefaultComboBoxModel<String>(mSession.getProjects().keySet().toArray(new String[0]));
         if(cbm.getSize() > 0)
            cbm.setSelectedItem(cbm.getElementAt(0));
         jComboBox_Project.setModel(cbm);

      }
      {
         final ComboBoxModel<String> cbm = new DefaultComboBoxModel<String>(mSession.getPeople().keySet().toArray(new String[0]));
         if(cbm.getSize() > 0)
            cbm.setSelectedItem(cbm.getElementAt(0));
         jComboBox_Operator.setModel(cbm);

      }
      // Lay them out
      final JPanel content = new JPanel();
      content.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
      content.setLayout(new FormLayout("right:pref, 3dlu, 200dlu, 3dlu, pref", "pref, 5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, 100dlu, 5dlu, pref, 5dlu, fill:100dlu, 5dlu, pref, 5dlu, pref"));
      final CellConstraints cc = new CellConstraints();
      content.add(new Label("Instrument"), cc.xy(1, 1));
      content.add(jComboBox_Instrument, cc.xy(3, 1));
      content.add(new Label("Detector"), cc.xy(1, 3));
      content.add(jComboBox_Detector, cc.xy(3, 3));
      content.add(new Label("Calibration"), cc.xy(1, 5));
      content.add(jComboBox_Calibration, cc.xy(3, 5));
      content.add(new Label("Project"), cc.xy(1, 7));
      content.add(jComboBox_Project, cc.xy(3, 7));
      content.add(jButton_NewProject, cc.xy(5, 7));
      content.add(new Label("Collected by"), cc.xy(1, 9));
      content.add(jComboBox_Operator, cc.xy(3, 9));
      content.add(jButton_NewOperator, cc.xy(5, 9));
      content.add(new JScrollPane(jList_Spectra), cc.xyw(1, 11, 5));
      {
         final ButtonBarBuilder bbb = new ButtonBarBuilder();
         bbb.addGlue();
         bbb.addButton(jButton_Select, jButton_Remove, jButton_AsStandard, jButton_Properties);
         bbb.addGlue();
         content.add(bbb.build(), cc.xyw(1, 13, 5));
      }
      content.add(jSpecDisplay_Preview, cc.xyw(1, 15, 5));

      final JPanel pp = new JPanel(new FormLayout("right:pref, 5dlu, 105dlu, 10dlu, right:pref, 5dlu, 55dlu", "pref, 5dlu, pref"));
      pp.add(new JLabel("Acquired"), cc.xy(1, 1));
      pp.add(jTextField_AcqTime, cc.xy(3, 1));
      pp.add(new JLabel("Probe current"), cc.xy(5, 1));
      pp.add(jTextField_ProbeCurrent, cc.xy(7, 1));
      pp.add(new JLabel("Composition"), cc.xy(1, 3));
      pp.add(jTextField_Composition, cc.xy(3, 3));
      pp.add(new JLabel("Live time"), cc.xy(5, 3));
      pp.add(jTextField_LiveTime, cc.xy(7, 3));
      content.add(pp, cc.xyw(1, 17, 5));
      {
         final ButtonBarBuilder bbb = new ButtonBarBuilder();
         bbb.addGlue();
         bbb.addButton(jButton_Ok, jButton_Cancel);
         content.add(bbb.build(), cc.xyw(1, 19, 5));
      }
      setContentPane(content);
      pack();
      // Configure the dialog
      setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      setTitle("Import spectra into the database");
      setResizable(false);
   }

   /**
    * Constructs a DBImportDialog
    * 
    * @param owner
    * @param ses
    */
   public ImportDialog(Frame owner, Session ses) {
      super(owner, "Import spectra into the database", true);
      mSession = ses;
      try {
         initialize();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a DBImportDialog
    * 
    * @param owner
    */
   public ImportDialog(Dialog owner, Session ses) {
      super(owner, "Import spectra into the database", true);
      mSession = ses;
      try {
         initialize();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a DBImportDialog
    * 
    * @param owner
    */
   public ImportDialog(Window owner, Session ses) {
      super(owner, "Import spectra into the database");
      mSession = ses;
      try {
         initialize();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a DBImportDialog
    * 
    * @param owner
    * @param title
    */
   public ImportDialog(Frame owner, String title, Session ses) {
      super(owner, title, true);
      mSession = ses;
      try {
         initialize();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   public void setDetector(DetectorProperties dp) {
      jComboBox_Instrument.setSelectedItem(dp.getOwner());
      updateDetector();
      jComboBox_Detector.setSelectedItem(dp);
      updateCalibrations();
   }

   private EDSDetector getDetector() {
      final Object detObj = jComboBox_Detector.getSelectedItem();
      final Object calObj = jComboBox_Calibration.getSelectedItem();
      EDSDetector res = null;
      if((detObj instanceof DetectorProperties) && (calObj instanceof EDSCalibration))
         res = EDSDetector.createDetector((DetectorProperties) detObj, (EDSCalibration) calObj);
      return res;
   }

   private void importSpectra() {
      final ListModel<ISpectrumData> lm = jList_Spectra.getModel();
      final StringBuffer sb = new StringBuffer();
      final EDSDetector detector = getDetector();
      if(detector != null) {
         final String operator = jComboBox_Operator.getSelectedItem().toString();
         final String project = jComboBox_Project.getSelectedItem().toString();

         if(lm.getSize() > 0) {
            final ProgressMonitor pm = new ProgressMonitor(this, "Wait while spectra are added to the database", "Initializing", 0, lm.getSize());
            try {
               mImported = new HTMLList();
               mImported.setHeader("Importing spectra into database</i>");
               int errs = 0;
               for(int i = 0; i < lm.getSize(); ++i) {
                  final ISpectrumData spec = SpectrumUtils.applyEDSDetector(detector, lm.getElementAt(i));
                  final String name = spec.getProperties().getTextWithDefault(SpectrumProperties.SourceFile, spec.toString());
                  pm.setNote("Importing " + name);
                  pm.setProgress(i);
                  try {
                     final SpectrumProperties props = spec.getProperties();
                     props.setTextProperty(SpectrumProperties.InstrumentOperator, operator);
                     props.setObjectProperty(SpectrumProperties.ProjectName, project);
                     mSession.addSpectrum(spec, false);
                     final Composition stdComp = spec.getProperties().getCompositionWithDefault(SpectrumProperties.StandardComposition, null);
                     if(stdComp == null)
                        mImported.add(name + " imported.");
                     else
                        mImported.add(name + " imported as " + stdComp.toString() + ".");
                  }
                  catch(final Exception e) {
                     ++errs;
                     mImported.addError(name + " was not imported into " + mSession.toString());
                     sb.append("Error importing: <i>");
                     sb.append(name);
                     sb.append("</i>\n");
                     sb.append(e.getMessage());
                     sb.append("\n");
                  }
                  if(pm.isCanceled())
                     break;
               }
               if(errs > 0) {
                  final StringBuffer msg = new StringBuffer();
                  msg.append(lm.getSize() - errs);
                  msg.append(" spectra imported.\n");
                  msg.append(errs);
                  msg.append(" errors occurred importing the spectra into the database.");
                  ErrorDialog.createErrorMessage(ImportDialog.this, "Importing spectra into the database", msg.toString(), sb.toString());
               }
            }
            finally {
               pm.close();
            }
         }
      } else
         JOptionPane.showMessageDialog(ImportDialog.this, "Please select an EDS detector and calibration", "Import spectra into the database", JOptionPane.ERROR_MESSAGE);
   }

   public String getReport() {
      return mImported != null ? mImported.toString() : "";
   }

   /**
    * Description
    * 
    * @param args
    */
   public static void main(String[] args) {
      try {
         // Set these regardless (no harm in it)
         System.setProperty("apple.laf.useScreenMenuBar", "true");
         System.setProperty("apple.laf.smallTabs", "true");
         System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Trixy");
         // Set up special look-and-feels
         final String laf = UIManager.getSystemLookAndFeelClassName();
         UIManager.setLookAndFeel(laf);
      }
      catch(final Exception e) {
         try {
            e.printStackTrace();
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         }
         catch(final Exception e1) {
            e1.printStackTrace();
         }
      }
      JFrame.setDefaultLookAndFeelDecorated(false);
      final Session ses = new Session("/home/nicholas/TrixyDB");
      final ImportDialog id = new ImportDialog((Frame) null, ses);
      id.setVisible(true);
      System.out.print(id.getReport());
      System.exit(0);
   }

}
