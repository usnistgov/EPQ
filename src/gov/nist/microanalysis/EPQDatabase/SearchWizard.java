package gov.nist.microanalysis.EPQDatabase;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.Detector.DetectorCalibration;
import gov.nist.microanalysis.EPQLibrary.Detector.DetectorProperties;
import gov.nist.microanalysis.EPQLibrary.Detector.ElectronProbe;
import gov.nist.microanalysis.EPQLibrary.Detector.IXRayDetector;
import gov.nist.microanalysis.EPQTools.EPQXStream;
import gov.nist.microanalysis.EPQTools.ErrorDialog;
import gov.nist.microanalysis.EPQTools.JWizardDialog;
import gov.nist.microanalysis.EPQTools.MaterialsCreator;
import gov.nist.microanalysis.EPQTools.SpecDisplay;
import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * A WizardDialog for searching the database to find specific spectra.
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
public class SearchWizard extends JWizardDialog {

   private static final long serialVersionUID = 2454447876045620922L;

   private final StartPanel jWizardPanel_Start = new StartPanel(this);
   private final ModePanel jWizardPanel_Mode = new ModePanel(this);
   private final StandardPanel jWizardPanel_Standard = new StandardPanel(this);
   private final AdvancedPanel jWizardPanel_Advanced = new AdvancedPanel(this);
   private final ProjectPanel jWizardPanel_Project = new ProjectPanel(this);
   private final CompositionPanel jWizardPanel_Composition = new CompositionPanel(this);
   private final ResultPanel jWizardPanel_Result = new ResultPanel(this);

   private final Preferences mPreferences = Preferences.userNodeForPackage(SearchWizard.class);

   private Session mSession = null;

   private class StartPanel extends JWizardPanel {

      private static final long serialVersionUID = 5665030135473354118L;

      private final JComboBox<ElectronProbe> jComboBox_Instrument = new JComboBox<ElectronProbe>();
      private final JComboBox<DetectorProperties> jComboBox_Detector = new JComboBox<DetectorProperties>();
      private final JComboBox<Object> jComboBox_Calibration = new JComboBox<Object>();
      private final JComboBox<String> jComboBox_CollectedBy = new JComboBox<String>();
      private final JTextField jTextField_BeamEnergy = new JTextField();

      private boolean mFirstShow = true;

      private void updateDetectors() {
         final Object obj = jComboBox_Instrument.getSelectedItem();
         if (obj instanceof ElectronProbe) {
            final ElectronProbe ep = (ElectronProbe) obj;
            final String det = mPreferences.get("Det4" + ep.toString(), "");
            jComboBox_Detector.removeAllItems();
            Object sel = null;
            for (final DetectorProperties xrd : mSession.getDetectors(ep)) {
               jComboBox_Detector.addItem(xrd);
               if (xrd.toString().equals(det) || (sel == null))
                  sel = xrd;
            }
            jComboBox_Detector.setSelectedItem(sel);
            updateCalibrations();
         }
      }

      private void updateCalibrations() {
         final Object prev = jComboBox_Calibration.getSelectedItem();
         final Object sel = jComboBox_Detector.getSelectedItem();
         final DefaultComboBoxModel<Object> dcbm = new DefaultComboBoxModel<Object>();
         if (sel instanceof DetectorProperties) {
            final List<DetectorCalibration> calibs = mSession.getCalibrations((DetectorProperties) sel);
            final String cal = calibs.contains(prev) ? prev.toString() : mPreferences.get("Cal4" + sel.toString(), "");
            DetectorCalibration selCal = null;
            dcbm.addElement("Any calibration");
            for (final DetectorCalibration dc : calibs) {
               dcbm.addElement(dc);
               if (dc.toString().equals(cal))
                  selCal = dc;
            }
            if (selCal != null)
               dcbm.setSelectedItem(selCal);
         }
         jComboBox_Calibration.setModel(dcbm);
      }

      private void initialize() {
         final CellConstraints cc = new CellConstraints();
         add(new JLabel("Instrument"), cc.xy(1, 1));
         add(jComboBox_Instrument, cc.xyw(3, 1, 3));
         add(new JLabel("Detector"), cc.xy(1, 3));
         add(jComboBox_Detector, cc.xyw(3, 3, 3));
         add(new JLabel("Calibration"), cc.xy(1, 5));
         add(jComboBox_Calibration, cc.xyw(3, 5, 3));
         add(new JLabel("Collected by"), cc.xy(1, 7));
         add(jComboBox_CollectedBy, cc.xyw(3, 7, 3));
         add(new JLabel("Beam Energy"), cc.xy(1, 9));
         add(jTextField_BeamEnergy, cc.xy(3, 9));
         add(new JLabel("keV"), cc.xy(5, 9));

         jComboBox_Instrument.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               updateDetectors();
            }
         });

         jComboBox_Detector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               final Object inst = jComboBox_Instrument.getSelectedItem();
               final Object det = jComboBox_Detector.getSelectedItem();
               if ((inst != null) && (det != null))
                  mPreferences.put("Det4" + inst.toString(), det.toString());
               updateCalibrations();
            }
         });

         jComboBox_Calibration.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               final Object det = jComboBox_Detector.getSelectedItem();
               final Object cal = jComboBox_Calibration.getSelectedItem();
               if ((det != null) && (cal != null))
                  mPreferences.put("Cal4" + det.toString(), cal.toString());
            }
         });

         jTextField_BeamEnergy.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
               checkBeamEnergy();
            }
         });
      }

      public double getBeamEnergy() {
         double res = 20.0;
         try {
            final NumberFormat nf = NumberFormat.getInstance();
            final double val = nf.parse(jTextField_BeamEnergy.getText()).doubleValue();
            if ((val >= 0.1) || (val <= 2000.0))
               res = val;
         } catch (final ParseException e1) {
         }
         return res;
      }

      public DetectorProperties getDetectorProperties() {
         return (DetectorProperties) jComboBox_Detector.getSelectedItem();
      }

      public DetectorCalibration getDetectorCalibration() {
         final Object cal = jComboBox_Calibration.getSelectedItem();
         return cal instanceof DetectorCalibration ? (DetectorCalibration) cal : null;
      }

      private void setDetector(IXRayDetector xrd) {
         setInstrument(xrd.getDetectorProperties().getOwner());
         jComboBox_Detector.setSelectedItem(xrd.getDetectorProperties());
         jComboBox_Calibration.setSelectedItem(xrd.getCalibration());
      }

      private void setInstrument(ElectronProbe ep) {
         final ComboBoxModel<ElectronProbe> cbm = jComboBox_Instrument.getModel();
         cbm.setSelectedItem(ep);
         if (cbm.getSelectedItem() != ep) {
            jComboBox_Instrument.addItem(ep);
            jComboBox_Instrument.setSelectedItem(ep);
         }
         updateDetectors();
      }

      private void setBeamEnergy(double beamEnergy) {
         final NumberFormat nf = new HalfUpFormat("0.0");
         jTextField_BeamEnergy.setText(nf.format(FromSI.keV(beamEnergy)));
      }

      public String getCollectedBy() {
         return jComboBox_CollectedBy.getSelectedItem().toString();
      }

      private boolean checkBeamEnergy() {
         boolean res = true;
         final String e0 = jTextField_BeamEnergy.getText();
         try {
            final NumberFormat nf = NumberFormat.getInstance();
            final double val = nf.parse(e0).doubleValue();
            if ((val < 0.1) || (val > 2000.0))
               res = false;
         } catch (final ParseException e1) {
            res = false;
         }
         jTextField_BeamEnergy.setBackground(res ? SystemColor.text : Color.pink);
         if (res) {
            mPreferences.put("Beam Energy", e0.toString());
            setMessageText("");
         } else
            setErrorText("Unable to understand the beam energy. Range = (0.1 keV, 2.0 MeV).");
         return res;

      }

      public StartPanel(final JWizardDialog wiz) {
         super(wiz, new FormLayout("right:pref, 5dlu, 100dlu, 3dlu, pref", "pref, 5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, pref"));
         try {
            initialize();
         } catch (final Exception ex) {
            ex.printStackTrace();
         }
      }

      @Override
      public void onShow() {
         if ((mFirstShow) && (mSession != null)) {
            {
               final String epStr = mPreferences.get("Instrument", "");
               Object sel = null;
               for (final ElectronProbe ep : mSession.getElectronProbes().keySet()) {
                  jComboBox_Instrument.addItem(ep);
                  if (ep.toString() == epStr)
                     sel = ep;
               }
               if (sel != null)
                  jComboBox_Instrument.setSelectedItem(sel);
               updateDetectors();
            }
            {
               final String pStr = mPreferences.get("Person", System.getProperty("user.name"));
               Object sel = null;
               for (final String person : mSession.getPeople().keySet()) {
                  jComboBox_CollectedBy.addItem(person);
                  if (person.equals(pStr))
                     sel = person;
               }
               if (sel != null)
                  jComboBox_CollectedBy.setSelectedItem(sel);
               jTextField_BeamEnergy.setText(mPreferences.get("Beam Energy", "20.0"));

            }
            mFirstShow = false;
         }
         getWizard().setNextPanel(jWizardPanel_Mode, "Select the search mode");
         enableFinish(false);
         setBackEnabled(true);
         setMessageText("Provide this basic information about the requested spectrum.");
      }

      @Override
      public boolean permitNext() {
         if (!checkBeamEnergy())
            return false;
         final Object inst = jComboBox_Instrument.getSelectedItem();
         final Object det = jComboBox_Detector.getSelectedItem();
         final Object per = jComboBox_CollectedBy.getSelectedItem();
         if ((inst != null) && (per != null) && (det != null)) {
            mPreferences.put("Instrument", inst.toString());
            mPreferences.put("Det4" + inst.toString(), det.toString());
            mPreferences.put("Person", per.toString());
         }
         return (det != null) & (per != null);
      }
   };

   private class ModePanel extends JWizardPanel {

      private static final long serialVersionUID = -4072841200850574253L;

      private final JRadioButton jRadioButton_Measured = new JRadioButton("Search by measured composition");
      private final JRadioButton jRadioButton_Standard = new JRadioButton("Search by standard composition");
      private final JRadioButton jRadioButton_Particle = new JRadioButton("Search by particle signature");
      private final JRadioButton jRadioButton_Project = new JRadioButton("Search by project");
      private final JRadioButton jRadioButton_Advanced = new JRadioButton("Use the advanced search mode");

      private void initialize() {
         final CellConstraints cc = new CellConstraints();
         add(jRadioButton_Measured, cc.xy(1, 1));
         add(jRadioButton_Standard, cc.xy(1, 3));
         add(jRadioButton_Particle, cc.xy(1, 5));
         add(jRadioButton_Project, cc.xy(1, 7));
         add(jRadioButton_Advanced, cc.xy(1, 9));

         jRadioButton_Measured.setEnabled(false);
         jRadioButton_Standard.setEnabled(true);
         jRadioButton_Particle.setEnabled(false);
         jRadioButton_Project.setEnabled(true);
         jRadioButton_Advanced.setEnabled(true);

         final ActionListener rb = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               updateSelection();
            }
         };

         jRadioButton_Measured.addActionListener(rb);
         jRadioButton_Standard.addActionListener(rb);
         jRadioButton_Particle.addActionListener(rb);
         jRadioButton_Project.addActionListener(rb);
         jRadioButton_Advanced.addActionListener(rb);

         final ButtonGroup bg = new ButtonGroup();
         bg.add(jRadioButton_Measured);
         bg.add(jRadioButton_Standard);
         bg.add(jRadioButton_Particle);
         bg.add(jRadioButton_Project);
         bg.add(jRadioButton_Advanced);

         jRadioButton_Project.setSelected(true);
      }

      private void updateSelection() {
         JWizardPanel next = null;
         String msg = "?";
         if (jRadioButton_Measured.isSelected()) {
            next = jWizardPanel_Composition;
            msg = "Specify measured composition";
         } else if (jRadioButton_Standard.isSelected()) {
            next = jWizardPanel_Standard;
            msg = "Select the standard by name";
         } else if (jRadioButton_Particle.isSelected()) {
            next = jWizardPanel_Result;
            msg = "Specify particle signature";
         } else if (jRadioButton_Advanced.isSelected()) {
            next = jWizardPanel_Advanced;
            msg = "Specify advanced search string";
         } else if (jRadioButton_Project.isSelected()) {
            next = jWizardPanel_Project;
            msg = "Specify a project";
         }
         getWizard().setNextPanel(next, msg);
      }

      public ModePanel(JWizardDialog wiz) {
         super(wiz, new FormLayout("pref", "pref, 5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, pref"));
         try {
            initialize();
         } catch (final Exception ex) {
            ex.printStackTrace();
         }
      }

      @Override
      public void onShow() {
         getWizard().setNextPanel(jWizardPanel_Mode, "Select the search mode");
         enableFinish(false);
         setBackEnabled(true);
         setMessageText("Select the way in which the search will be performed.");
         updateSelection();
      }
   }

   private class AdvancedPanel extends JWizardPanel {

      private static final long serialVersionUID = -2284703248238350664L;

      private final JRadioButton jRadioButton_Measured = new JRadioButton("Search by measured composition");
      private final JRadioButton jRadioButton_Particle = new JRadioButton("Search by particle signature");
      private final JRadioButton jRadioButton_Standard = new JRadioButton("Search by standard composition");
      private final JTextField jTextField_Search = new JTextField();

      public AdvancedPanel(JWizardDialog wiz) {
         super(wiz, new FormLayout("10dlu, 250dlu", "pref, 5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, pref, 5dlu, pref"));
         try {
            initialize();
         } catch (final Exception ex) {
            ex.printStackTrace();
         }
      }

      private void initialize() {
         final CellConstraints cc = new CellConstraints();
         add(new JLabel("<html><b>Search mode</b>"), cc.xyw(1, 1, 2));
         add(jRadioButton_Measured, cc.xy(2, 3));
         add(jRadioButton_Particle, cc.xy(2, 5));
         add(jRadioButton_Standard, cc.xy(2, 7));
         add(new JLabel("<html><b>Search String</b>"), cc.xyw(1, 9, 2));
         add(jTextField_Search, cc.xy(2, 11));
         jTextField_Search.setText("ELM_SI>10 and ELM_SI<20 and ELM_O>20");

         final ButtonGroup bg = new ButtonGroup();
         bg.add(jRadioButton_Measured);
         bg.add(jRadioButton_Particle);
         bg.add(jRadioButton_Standard);
         jRadioButton_Measured.setSelected(true);
      }

      @Override
      public void onShow() {
         getWizard().setNextPanel(jWizardPanel_Result, "Preview the results");
         enableFinish(false);
         setBackEnabled(true);
         setMessageText("Specify a SQL search string");
      }

      @Override
      public boolean permitNext() {
         // TODO: Perform advanced search
         Session.ElementDataTypes edt;
         if (jRadioButton_Measured.isSelected())
            edt = Session.ElementDataTypes.MEASURED_COMPOSITION;
         else if (jRadioButton_Particle.isSelected())
            edt = Session.ElementDataTypes.PARTICLE_SIGNATURE;
         else if (jRadioButton_Standard.isSelected())
            edt = Session.ElementDataTypes.STANDARD_COMPOSITION;
         else
            return false;
         try {
            final TreeSet<Session.SpectrumSummary> res = mSession.findSpectra(jTextField_Search.getText(), edt, 1000);
            jWizardPanel_Result.addResults(res);
         } catch (final Exception e) {
            ErrorDialog.createErrorMessage(SearchWizard.this, "Search error", e);
         }
         return true;
      }

   };

   private class ProjectPanel extends JWizardPanel {

      private static final long serialVersionUID = -4748366640662414063L;

      private final JComboBox<String> jComboBox_Project = new JComboBox<String>();

      private boolean mFirstShow = true;

      public ProjectPanel(JWizardDialog wiz) {
         super(wiz, new FormLayout("pref, 5dlu, 100dlu", "pref"));
         try {
            initialize();
         } catch (final Exception ex) {
            ex.printStackTrace();
         }
      }

      private void initialize() {
         final CellConstraints cc = new CellConstraints();
         add(new JLabel("Project name"), cc.xy(1, 1));
         add(jComboBox_Project, cc.xy(3, 1));
      }

      @Override
      public void onShow() {
         if (mFirstShow && (mSession != null)) {
            for (final String str : mSession.getProjects().keySet())
               jComboBox_Project.addItem(str);
            mFirstShow = false;
         }
         getWizard().setNextPanel(jWizardPanel_Result, "Preview the results");
         enableFinish(false);
         setBackEnabled(true);
         setMessageText("Select a project to open");
      }

      @Override
      public boolean permitNext() {
         // Search by project...
         jWizardPanel_Result.addResults(mSession.findSpectra((String) jComboBox_Project.getSelectedItem(), jWizardPanel_Start.getDetectorProperties(),
               jWizardPanel_Start.getDetectorCalibration(), jWizardPanel_Start.getCollectedBy(), jWizardPanel_Start.getBeamEnergy(), 100));
         return true;
      }

   };

   private class CompositionPanel extends JWizardPanel {

      private static final long serialVersionUID = -8890334923401808867L;

      private final JTextField jTextField_Composition = new JTextField();
      private final JSlider jSlider_Tolerance = new JSlider();
      private final JButton jButton_Edit = new JButton("Edit");

      private boolean mFirstShow = true;
      private Composition mComposition = Material.Null;

      public CompositionPanel(JWizardDialog wiz) {
         super(wiz, new FormLayout("10dlu, 150dlu, 5dlu, pref", "pref, 5dlu, pref, 5dlu, pref, 5dlu, pref"));
         try {
            initialize();
         } catch (final Exception ex) {
            ex.printStackTrace();
         }
      }

      private void initialize() {
         final CellConstraints cc = new CellConstraints();
         add(new JLabel("<html><b>Composition</b>"), cc.xyw(1, 1, 2));
         add(jTextField_Composition, cc.xy(2, 3));
         add(jButton_Edit, cc.xy(4, 3));
         add(new JLabel("<html><b>Tolerance</b>"), cc.xyw(1, 5, 2));
         add(jSlider_Tolerance, cc.xy(2, 7));

         jTextField_Composition.setEditable(false);

         jButton_Edit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               final MaterialsCreator mc = new MaterialsCreator(SearchWizard.this, "Specify a composition", true);
               mc.setSession(mSession);
               mc.setMaterial(mComposition);
               mc.setLocationRelativeTo(SearchWizard.this);
               mc.setVisible(true);
               if (mc.getMaterial() != null) {
                  mComposition = mc.getMaterial();
                  jTextField_Composition.setText(mc.getMaterial().descriptiveString(false));
                  jTextField_Composition.setCaretPosition(0);
               }
            }
         });
      }

      @Override
      public void onShow() {
         if (mFirstShow) {
            final String xml = mPreferences.get("Composition", null);
            if (xml != null)
               mComposition = (Composition) EPQXStream.getInstance().fromXML(xml);
            jTextField_Composition.setText(mComposition.descriptiveString(false));
            jTextField_Composition.setCaretPosition(0);
            jSlider_Tolerance.setValue(mPreferences.getInt("CompTol", 50));
            mFirstShow = false;
         }
         getWizard().setNextPanel(jWizardPanel_Result, "Preview the results");
         enableFinish(false);
         setBackEnabled(true);
         setMessageText("Specify a composition and search tolerance.");
      }

      @Override
      public boolean permitNext() {
         mPreferences.put("Composition", EPQXStream.getInstance().toXML(mComposition));
         mPreferences.putInt("CompTol", jSlider_Tolerance.getValue());
         return (mComposition != Material.Null);
      }

   }

   private class StandardPanel extends JWizardPanel {

      private static final long serialVersionUID = -1235424069972431010L;

      private final JList<String> jList_Standards = new JList<String>();
      private boolean mFirstShow = true;

      public StandardPanel(JWizardDialog wiz) {
         super(wiz, new FormLayout("200dlu", "150dlu"));
         try {
            initialize();
         } catch (final Exception ex) {
            ex.printStackTrace();
         }
      }

      private void initialize() {
         final CellConstraints cc = new CellConstraints();
         add(new JScrollPane(jList_Standards), cc.xy(1, 1));
         jList_Standards.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      }

      @Override
      public void onShow() {
         if (mFirstShow && (mSession != null)) {
            try {
               final DefaultListModel<String> dlm = new DefaultListModel<String>();
               for (final String str : mSession.getStandards().keySet())
                  dlm.addElement(str);
               jList_Standards.setModel(dlm);
            } catch (final SQLException e) {
               e.printStackTrace();
            }
            mFirstShow = false;
         }
         getWizard().setNextPanel(jWizardPanel_Result, "Preview the results");
         enableFinish(false);
         setBackEnabled(true);
         setMessageText("Select one or more standards for which to search.");
      }

      @Override
      public boolean permitNext() {
         final String name = jList_Standards.getSelectedValue();
         try {
            if (name != null) {
               final TreeSet<Session.SpectrumSummary> res = mSession.findStandards(name, jWizardPanel_Start.getDetectorProperties(),
                     jWizardPanel_Start.getBeamEnergy(), jWizardPanel_Start.getCollectedBy(), 10);
               jWizardPanel_Result.addResults(res);
               return true;
            }

         } catch (final Exception e) {
            setErrorText("An error occurred while searching for " + name);
            ErrorDialog.createErrorMessage(SearchWizard.this, "Search for standard", e);
            return true;
         }
         return false;
      }
   }

   private class ResultPanel extends JWizardPanel {

      private static final long serialVersionUID = -1105425782850288969L;

      private final JList<Session.SpectrumSummary> jList_Results = new JList<Session.SpectrumSummary>();
      private final SpecDisplay jSpecDisplay_Preview = new SpecDisplay();

      public ResultPanel(JWizardDialog wiz) {
         super(wiz, new FormLayout("320dlu", "70dlu, 70dlu"));
         try {
            initialize();
         } catch (final Exception ex) {
            ex.printStackTrace();
         }
      }

      private void initialize() {
         final CellConstraints cc = new CellConstraints();
         add(new JScrollPane(jList_Results), cc.xy(1, 1));
         add(jSpecDisplay_Preview, cc.xy(1, 2));
         // Initialize controls
         jList_Results.setModel(new DefaultListModel<Session.SpectrumSummary>());
         // Set listeners
         jList_Results.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
               jSpecDisplay_Preview.clearAllSpectra();
               boolean ok = false;
               for (final Session.SpectrumSummary spec : jList_Results.getSelectedValuesList())
                  try {
                     jSpecDisplay_Preview.addSpectrum(spec.load());
                     ok = true;
                  } catch (final Exception e1) {
                     e1.printStackTrace();
                  }
               getWizard().enableFinish(ok);
               jSpecDisplay_Preview.rescaleV();
            }
         });
      }

      @Override
      public void onShow() {
         getWizard().setNextPanel(null, "Import spectra");
         enableFinish(true);
         setBackEnabled(true);
      }

      public void addResults(Collection<Session.SpectrumSummary> specs) {
         final DefaultListModel<Session.SpectrumSummary> dlm = (DefaultListModel<Session.SpectrumSummary>) (jList_Results.getModel());
         for (final Session.SpectrumSummary spec : specs)
            dlm.addElement(spec);
         mSession.initiateLoad(specs);
      }

      private ArrayList<ISpectrumData> getResults() {
         final ArrayList<ISpectrumData> res = new ArrayList<ISpectrumData>();
         final DefaultListModel<Session.SpectrumSummary> dlm = (DefaultListModel<Session.SpectrumSummary>) (jList_Results.getModel());
         for (int i = 0; i < dlm.size(); ++i)
            try {
               if (jList_Results.isSelectedIndex(i))
                  res.add(dlm.getElementAt(i).load());
            } catch (final Exception e) {
               ErrorDialog.createErrorMessage(SearchWizard.this, "Error getting results", e);
            }
         return res;
      }

   }

   private void initialize() {
      setActivePanel(jWizardPanel_Start, "Select a search mode");
   }

   /**
    * Constructs a SearchWizard
    * 
    * @throws HeadlessException
    */
   public SearchWizard(Session ses) throws HeadlessException {
      mSession = ses;
      try {
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a SearchWizard
    * 
    * @param owner
    * @param title
    * @param modal
    */
   public SearchWizard(Frame owner, String title, boolean modal, Session ses) {
      super(owner, title, modal);
      mSession = ses;
      try {
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a SearchWizard
    * 
    * @param owner
    * @param title
    */
   public SearchWizard(Frame owner, String title, Session ses) {
      super(owner, title);
      mSession = ses;
      try {
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a SearchWizard
    * 
    * @param owner
    * @param modal
    */
   public SearchWizard(Frame owner, boolean modal, Session ses) {
      super(owner, modal);
      mSession = ses;
      try {
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a SearchWizard
    * 
    * @param owner
    * @param title
    * @param modal
    */
   public SearchWizard(Dialog owner, String title, boolean modal, Session ses) {
      super(owner, title, modal);
      mSession = ses;
      try {
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a SearchWizard
    * 
    * @param owner
    * @param title
    */
   public SearchWizard(Dialog owner, String title, Session ses) {
      super(owner, title);
      mSession = ses;
      try {
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * Constructs a SearchWizard
    * 
    * @param owner
    * @param modal
    */
   public SearchWizard(Dialog owner, boolean modal, Session ses) {
      super(owner, modal);
      mSession = ses;
      try {
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }

   }

   /**
    * Returns the current database session associated with this search wizard.
    * 
    * @return Returns the mSession.
    */
   public Session getSession() {
      return mSession;
   }

   public ArrayList<ISpectrumData> getSpectra() {
      return jWizardPanel_Result.getResults();
   }

   public void setDetector(IXRayDetector xrd) {
      jWizardPanel_Start.setDetector(xrd);
   }

   public void setBeamEnergy(double beamEnergy) {
      jWizardPanel_Start.setBeamEnergy(beamEnergy);
   }

   public static void main(String[] args) {
      try {
         // Set these regardless (no harm in it)
         System.setProperty("apple.laf.useScreenMenuBar", "true");
         System.setProperty("apple.laf.smallTabs", "true");
         System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Trixy");
         // Set up special look-and-feels
         final String laf = UIManager.getSystemLookAndFeelClassName();
         UIManager.setLookAndFeel(laf);
      } catch (final Exception e) {
         try {
            e.printStackTrace();
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         } catch (final Exception e1) {
            e1.printStackTrace();
         }
      }
      JFrame.setDefaultLookAndFeelDecorated(false);
      final Session ses = new Session("/home/nicholas/TrixyDB");
      final SearchWizard sw = new SearchWizard(ses);
      sw.setModal(true);
      sw.setVisible(true);
      System.exit(0);
   }
}
