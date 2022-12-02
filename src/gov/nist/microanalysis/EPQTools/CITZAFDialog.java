package gov.nist.microanalysis.EPQTools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.CITZAF;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.MassAbsorptionCoefficient;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;

/**
 * <p>
 * A simple dialog used for obtaining information for ZAF corrections in a
 * user-friendly manner
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Daniel "Ooblioob" Davis
 * @version 1.0
 */
public class CITZAFDialog extends JDialog {
   static final long serialVersionUID = 0x1;
   private transient Vector<ActionListener> WindowClosingListeners;
   private transient Vector<ActionListener> BeginAnalysisListeners;
   Preferences userPref = Preferences.userRoot();
   CITZAF Citzaf = new CITZAF();
   public boolean DEBUG_MODE = false;

   private boolean jCheckBox_PrintMACsWasSelected = false;

   JPanel panel_MainPanel = new JPanel();
   BorderLayout borderLayout1 = new BorderLayout();
   JTabbedPane jTabbedPane_Main = new JTabbedPane();
   JPanel jPanel_Bottom = new JPanel();
   JButton jButton_BeginAnalysis = new JButton();
   JLabel jLabel_Spacer = new JLabel();
   JPanel jPanel_QuickStart = new JPanel();
   JPanel jPanel_BeamEnergyQS = new JPanel();
   JLabel jLabel_BeamEnergyQS = new JLabel();
   JTextField jTextField_BeamEnergyQS = new JTextField();
   JLabel jLabel_keVQS = new JLabel();
   JLabel jLabel_Spacer2 = new JLabel();
   JPanel jPanel_TakeOffAngleQS = new JPanel();
   JLabel jLabel_TakeOffAngleQS = new JLabel();
   JTextField jTextField_TakeOffAngleQS = new JTextField();
   JLabel jLabel_DegreesQS = new JLabel();
   JLabel jLabel_Spacer3 = new JLabel();
   JPanel jPanel_SampleTab = new JPanel();
   JScrollPane jScrollPane_Lines = new JScrollPane();
   JPanel jPanel_Lines = new JPanel();
   JLabel jLabel_Spacer7 = new JLabel();
   JPanel jPanel_Standards = new JPanel();
   JPanel jPanel_Corrections = new JPanel();
   JPanel jPanel_AnalyticalConditions = new JPanel();
   JPanel jPanel_StageCoordinates = new JPanel();
   JLabel jLabel_StageCoordinates = new JLabel();
   JRadioButton jRadioButton_StageCoordinatesYes = new JRadioButton();
   JRadioButton jRadioButton_StageCoordinatesNo = new JRadioButton();
   JPanel jPanel_DeadtimeCorrection = new JPanel();
   JLabel jLabel_DeadtimeCorrection = new JLabel();
   JTextField jTextField_DeadtimeCorrection = new JTextField();
   JLabel jLabel_DTMicroseconds = new JLabel();
   JLabel jLabel_Spacer11 = new JLabel();
   JLabel jLabel_SelectALine = new JLabel();
   JPanel jPanel_SelectLines = new JPanel();
   JLabel jLabel_Spacer12 = new JLabel();
   JPanel jPanel_DriftFactors = new JPanel();
   JLabel jLabel_DriftFactors = new JLabel();
   JLabel jLabel_Spacer13 = new JLabel();
   JScrollPane jScrollPane_DriftFactors = new JScrollPane();
   JLabel jLabel_Spacer14 = new JLabel();
   JPanel jPanel_Output = new JPanel();
   JPanel jPanel_Title = new JPanel();
   JLabel jLabel_Title = new JLabel();
   JTextField jTextField_Title = new JTextField();
   JLabel jLabel_Spacer15 = new JLabel();
   JPanel jPanel_OutputOptions = new JPanel();
   JCheckBox jCheckBox_PrintMACs = new JCheckBox();
   JCheckBox jCheckBox_PrintZAFs = new JCheckBox();
   JLabel jLabel_Spacer16 = new JLabel();
   JLabel jLabel_Spacer17 = new JLabel();
   JPanel jPanel_ConcentrationOutput = new JPanel();
   JLabel jLabel_ConcentrationOutput = new JLabel();
   JLabel jLabel_Spacer18 = new JLabel();
   JRadioButton jRadioButton_NearestPPM = new JRadioButton();
   JRadioButton jRadioButton_NearestHundreth = new JRadioButton();
   JLabel jLabel_Spacer19 = new JLabel();
   JPanel jPanel_Particles = new JPanel();
   JPanel jPanel_ParticleModels = new JPanel();
   JLabel jLabel_ParticleModel = new JLabel();
   JLabel jLabel_ParticleModelsContent = new JLabel();
   JButton jButton_EditModels = new JButton();
   JPanel jPanel_ParticleDiameter = new JPanel();
   JLabel jLabel_ParticleDiameter = new JLabel();
   JLabel jLabel_ParticleDiametersContent = new JLabel();
   JButton jButton_EditDiameters = new JButton();
   JPanel jPanel_SampleDensity = new JPanel();
   JLabel jLabel_SampleDensity = new JLabel();
   JTextField jTextField_SampleDensity = new JTextField();
   JLabel jLabel_SampleDensityUnits = new JLabel();
   JLabel jLabel_Spacer20 = new JLabel();
   JPanel jPanel_SizeFactor = new JPanel();
   JLabel jLabel_SizeFactor = new JLabel();
   JTextField jTextField_SizeFactor = new JTextField();
   JLabel jLabel_Spacer21 = new JLabel();
   JPanel jPanel_IntegrationStep = new JPanel();
   JLabel jLabel_IntegrationStep = new JLabel();
   JTextField jTextField_IntegrationStep = new JTextField();
   JLabel jLabel_Spacer22 = new JLabel();
   JPanel jPanel_LineInside = new JPanel();
   JPanel jPanel_CorrectionsList = new JPanel();

   private final Map<Element, String> ElementToLineMap = new TreeMap<Element, String>();
   private JTextField[] DriftFactorTextFields;
   private final NumberFormat mDefaultFmt = NumberFormat.getInstance();
   private final CITZAF.CITZAFCorrectionAlgorithm[] CorrectionAlgorithms = CITZAF.CITZAFCorrectionAlgorithm.getAllImplimentations();
   private final CITZAF.MassAbsorptionCoefficient[] MACAlgorithms = CITZAF.MassAbsorptionCoefficient.getAllImplimentations();
   private ParticleModelDialog pmd;
   private final MaterialFactory matFac = new MaterialFactory();
   private ParticleDiameterDialog pdd;
   private boolean threwNumberFormatException = false;
   private KRatioCreator KRatioDialog;

   ButtonGroup BulkOrParticleGroup = new ButtonGroup();
   ButtonGroup StageCoordinatesGroup = new ButtonGroup();
   ButtonGroup ConcentrationOutputGroup = new ButtonGroup();
   ButtonGroup[] LineButtonGroup;
   ButtonGroup CorrectionsGroup = new ButtonGroup();
   ButtonGroup MACGroup = new ButtonGroup();
   ButtonGroup KRatioOrComp = new ButtonGroup();
   GridLayout gridLayout1 = new GridLayout();
   JLabel jLabel_CorrectionProcedures = new JLabel();
   JPanel jPanel_MACs = new JPanel();
   JLabel jLabel_MACListing = new JLabel();
   JPanel jPanel_ManualMACs = new JPanel();
   JLabel jLabel_XStageCoordinate = new JLabel();
   JTextField jTextField_XStageCoordinate = new JTextField();
   JLabel jLabel_YStageCoordinate = new JLabel();
   JTextField jTextField_YStageCoordinate = new JTextField();
   JLabel jLabel_Spacer23 = new JLabel();
   JPanel jPanel_InsideDriftFactors = new JPanel();
   JScrollPane jScrollPane_Standards = new JScrollPane();
   JPanel jPanel_InsideStandards = new JPanel();
   JScrollPane jScrollPane_MACs = new JScrollPane();
   JLabel jLabel_Absorbers = new JLabel();
   JPanel jPanel_Absorbers = new JPanel();
   JLabel jLabel_Emitters = new JLabel();
   JPanel jPanel_InsideMACs = new JPanel();
   GridLayout gridLayout2 = new GridLayout();
   JRadioButton jRadioButton_Bulk = new JRadioButton();
   JLabel jLabel_SampleIs = new JLabel();
   JLabel jLabel_Spacer5 = new JLabel();
   JRadioButton jRadioButton_Particle = new JRadioButton();
   JPanel jPanel_Bulk = new JPanel();
   JPanel jPanel_SampleQS = new JPanel();
   JButton jButton_NewMaterialQS = new JButton();
   JLabel jLabel_SampleQS = new JLabel();
   JLabel jLabel_Spacer4 = new JLabel();
   JLabel jLabel_SampleDescriptionQS = new JLabel();
   JLabel jLabel_Spacer24 = new JLabel();
   JComboBox<String> jComboBox_Preselected = new JComboBox<String>();
   JPanel jPanel_Preselected = new JPanel();
   JLabel jLabel_Preselected = new JLabel();
   JLabel jLabel_Spacer25 = new JLabel();
   JPanel jPanel_KRatio = new JPanel();
   JLabel jLabel_KRatio = new JLabel();
   JLabel jLabel_KRatioDesc = new JLabel();
   JButton jButton_KRatio = new JButton();
   JPanel jPanel_CalcKRatios = new JPanel();
   JRadioButton jRadioButton_KRatios = new JRadioButton();
   JRadioButton jRadioButton_Composition = new JRadioButton();
   JLabel jLabel_Question = new JLabel();
   JLabel jLabel_Spacer26 = new JLabel();

   /**
    * CITZAFDialog - Creates a new instance of CITZAFDialog
    * 
    * @param frame
    *           Frame - the dialog owner
    * @param title
    *           String - the title that will appear on the dialog
    * @param modal
    *           boolean - if true then parent windows will be disabled while
    *           this dialog is open
    */

   public CITZAFDialog(Frame frame, String title, boolean modal) {
      super(frame, title, modal);
      try {
         initialize();
         pack();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * CITZAFDialog - Creates a new instance of CITZAFDialog
    * 
    * @param dialog
    *           Dialog - the parent dialog
    * @param title
    *           String - the title that will appear on the dialog
    * @param modal
    *           boolean - if true then parent windows will be disabled while
    *           this dialog is open
    */

   public CITZAFDialog(Dialog dialog, String title, boolean modal) {
      super(dialog, title, modal);
      try {
         initialize();
         pack();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   private void initialize() throws Exception {
      jTabbedPane_Main.addChangeListener(new javax.swing.event.ChangeListener() {
         @Override
         public void stateChanged(javax.swing.event.ChangeEvent e) {
            commitToCitzaf();
            if ((jTabbedPane_Main.getSelectedComponent() == jPanel_ManualMACs) && (!threwNumberFormatException))
               loadMACTable();
         }
      });

      panel_MainPanel.setLayout(borderLayout1);
      panel_MainPanel.setPreferredSize(new Dimension(400, 335));

      jTabbedPane_Main.setPreferredSize(new Dimension(400, 300));
      jPanel_Bottom.setMinimumSize(new Dimension(122, 35));
      jPanel_Bottom.setPreferredSize(new Dimension(400, 35));

      jButton_BeginAnalysis.setText("Begin Analysis");
      jButton_BeginAnalysis.setEnabled(true);
      jButton_BeginAnalysis.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_BeginAnalysis_actionPerformed(e);
         }
      });

      jLabel_Spacer.setPreferredSize(new Dimension(250, 15));

      createQuickStartTab();
      createSampleTab();
      createStandardsTab();
      createCorrectionTab();
      createAnalyticalConditionsTab();
      createOutputTab();
      createParticlesTab();
      createMACsTab();
      loadDataFromCITZAF();
      AddTabs();
      this.addWindowListener(new java.awt.event.WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            this_windowClosing(e);
         }
      });

      jPanel_Bottom.add(jLabel_Spacer, null);
      getContentPane().add(panel_MainPanel);

      panel_MainPanel.add(jPanel_Bottom, BorderLayout.SOUTH);
      jPanel_Bottom.add(jButton_BeginAnalysis, null);
      panel_MainPanel.add(jTabbedPane_Main, BorderLayout.CENTER);
   }

   private void loadDataFromCITZAF() {
      loadQuickStartTabFromCITZAF();
      loadSamplesTabFromCITZAF();
      loadStandardsTabFromCITZAF();
      loadCorrectionsTabFromCITZAF();
      loadAnalyticalConditionsTabFromCITZAF();
      loadOutputTabFromCITZAF();
      loadParticlesTabFromCITZAF();
      // loadMACTable();
   }

   private void createQuickStartTab() {
      jLabel_BeamEnergyQS.setPreferredSize(new Dimension(78, 15));
      jLabel_BeamEnergyQS.setText("Beam Energy :");

      jTextField_BeamEnergyQS.setPreferredSize(new Dimension(45, 21));
      jTextField_BeamEnergyQS.setHorizontalAlignment(SwingConstants.TRAILING);
      jTextField_BeamEnergyQS.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            jTextField_BeamEnergyQS_focusGained(e);
         }
      });

      jLabel_keVQS.setText("keV");

      jLabel_Spacer2.setPreferredSize(new Dimension(195, 15));

      jPanel_BeamEnergyQS.setPreferredSize(new Dimension(400, 40));
      jPanel_BeamEnergyQS.add(jLabel_BeamEnergyQS, null);
      jPanel_BeamEnergyQS.add(jTextField_BeamEnergyQS, null);
      jPanel_BeamEnergyQS.add(jLabel_keVQS, null);
      jPanel_BeamEnergyQS.add(jLabel_Spacer2, null);

      jLabel_TakeOffAngleQS.setText("Take Off Angle :");

      jTextField_TakeOffAngleQS.setPreferredSize(new Dimension(45, 21));

      jTextField_TakeOffAngleQS.setHorizontalAlignment(SwingConstants.TRAILING);
      jTextField_TakeOffAngleQS.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            jTextField_TakeOffAngleQS_focusGained(e);
         }
      });

      jLabel_DegreesQS.setText("degrees");

      jLabel_Spacer3.setPreferredSize(new Dimension(175, 15));

      jPanel_TakeOffAngleQS.setPreferredSize(new Dimension(400, 40));
      jPanel_TakeOffAngleQS.add(jLabel_TakeOffAngleQS, null);
      jPanel_TakeOffAngleQS.add(jTextField_TakeOffAngleQS, null);
      jPanel_TakeOffAngleQS.add(jLabel_DegreesQS, null);
      jPanel_TakeOffAngleQS.add(jLabel_Spacer3, null);

      jLabel_KRatio.setPreferredSize(new Dimension(50, 15));
      jLabel_KRatio.setText("K-Ratio :");

      jLabel_KRatioDesc.setPreferredSize(new Dimension(194, 25));

      jButton_KRatio.setText("New Sample");
      jButton_KRatio.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_KRatio_actionPerformed(e);
         }
      });

      jPanel_KRatio.setPreferredSize(new Dimension(400, 35));
      jPanel_KRatio.add(jLabel_KRatio, null);
      jPanel_KRatio.add(jLabel_KRatioDesc, null);
      jPanel_KRatio.add(jButton_KRatio, null);

      jPanel_QuickStart.setPreferredSize(new Dimension(400, 260));
      jPanel_QuickStart.add(jPanel_BeamEnergyQS, null);
      jPanel_QuickStart.add(jPanel_TakeOffAngleQS, null);
      jPanel_QuickStart.add(jPanel_KRatio, null);
   }

   private void loadQuickStartTabFromCITZAF() {
      jTextField_BeamEnergyQS.setText(Double.toString(Citzaf.getBeamEnergy()));
      jTextField_TakeOffAngleQS.setText(Double.toString(Citzaf.getTakeOffAngle()));
      jLabel_KRatioDesc.setText(Citzaf.formatKRatios(Citzaf.getKRatios()));
   }

   private void createSampleTab() {
      jLabel_Question.setText("Which are you trying to calculate?");

      jRadioButton_Composition.setText("Composition");
      jRadioButton_Composition.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jRadioButton_Composition_actionPerformed(e);
         }
      });
      jRadioButton_Composition.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jRadioButton_Composition_actionPerformed(e);
         }
      });

      jRadioButton_KRatios.setText("k-ratios");
      jRadioButton_KRatios.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jRadioButton_KRatios_actionPerformed(e);
         }
      });

      jLabel_Spacer26.setPreferredSize(new Dimension(30, 15));

      KRatioOrComp.add(jRadioButton_KRatios);
      KRatioOrComp.add(jRadioButton_Composition);

      jPanel_CalcKRatios.add(jLabel_Question, null);
      jPanel_CalcKRatios.add(jRadioButton_Composition, null);
      jPanel_CalcKRatios.add(jRadioButton_KRatios, null);
      jPanel_CalcKRatios.add(jLabel_Spacer26, null);

      jLabel_SampleQS.setText("Sample :");

      jLabel_SampleDescriptionQS.setPreferredSize(new Dimension(150, 30));

      jButton_NewMaterialQS.setText("New Material");
      jButton_NewMaterialQS.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_NewMaterialQS_actionPerformed(e);
         }
      });
      jLabel_Spacer4.setPreferredSize(new Dimension(49, 30));

      jPanel_SampleQS.setPreferredSize(new Dimension(400, 35));
      jPanel_SampleQS.add(jLabel_SampleQS, null);
      jPanel_SampleQS.add(jLabel_SampleDescriptionQS, null);
      jPanel_SampleQS.add(jButton_NewMaterialQS, null);
      jPanel_SampleQS.add(jLabel_Spacer4, null);

      jLabel_Preselected.setPreferredSize(new Dimension(100, 10));
      jLabel_Preselected.setText("Premade materials:");

      jLabel_Spacer24.setPreferredSize(new Dimension(40, 10));

      jComboBox_Preselected.setPreferredSize(new Dimension(150, 21));
      jComboBox_Preselected.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jComboBox_Preselected_actionPerformed(e);
         }
      });
      jLabel_Spacer25.setPreferredSize(new Dimension(47, 10));

      jPanel_Preselected.setPreferredSize(new Dimension(400, 30));
      jPanel_Preselected.add(jLabel_Preselected, null);
      jPanel_Preselected.add(jLabel_Spacer24, null);
      jPanel_Preselected.add(jComboBox_Preselected, null);
      jPanel_Preselected.add(jLabel_Spacer25, null);

      jComboBox_Preselected.addItem("No preset material selected");
      final String[] presetItemsList = matFac.getMaterialNames();
      for (final String element : presetItemsList)
         jComboBox_Preselected.addItem(element);

      jLabel_SelectALine.setText("Select the KLM Lines for each element");

      jLabel_Spacer12.setPreferredSize(new Dimension(145, 20));

      jPanel_SelectLines.setPreferredSize(new Dimension(345, 20));
      jPanel_SelectLines.add(jLabel_SelectALine, null);
      jPanel_SelectLines.add(jLabel_Spacer12, null);

      jPanel_LineInside.setPreferredSize(new Dimension(180, 130));

      jScrollPane_Lines.setPreferredSize(new Dimension(200, 110));
      jScrollPane_Lines.getViewport().add(jPanel_LineInside, null);

      jLabel_Spacer7.setPreferredSize(new Dimension(145, 20));

      jPanel_Lines.setPreferredSize(new Dimension(400, 150));
      jPanel_Lines.add(jScrollPane_Lines, null);
      jPanel_Lines.add(jLabel_Spacer7, null);

      jPanel_SampleTab.setPreferredSize(new Dimension(400, 260));
      jPanel_SampleTab.add(jPanel_CalcKRatios, null);
      jPanel_SampleTab.add(jPanel_SampleQS, null);
      jPanel_SampleTab.add(jPanel_Preselected, null);
      jPanel_SampleTab.add(jPanel_SelectLines, null);
      jPanel_SampleTab.add(jPanel_Lines, null);
   }

   private void loadSamplesTabFromCITZAF() {
      jRadioButton_KRatios.setSelected(!Citzaf.getUsingKRatios());
      jRadioButton_Composition.setSelected(Citzaf.getUsingKRatios());
      toggleMaterials(Citzaf.getUsingKRatios());

      jLabel_SampleDescriptionQS.setText(Citzaf.formatComposition(Citzaf.getComposition()));
      jLabel_SampleDescriptionQS.setToolTipText(jLabel_SampleDescriptionQS.getText());

      if (!Citzaf.getUsingKRatios())
         drawLines();
   }

   private void createStandardsTab() {
      jPanel_Standards.setPreferredSize(new Dimension(400, 260));
      // jPanel_Standards.removeAll();

      jScrollPane_Standards.setPreferredSize(new Dimension(400, 250));

      jPanel_Standards.add(jScrollPane_Standards, null);
      jScrollPane_Standards.getViewport().add(jPanel_InsideStandards, null);
   }

   private void loadStandardsTabFromCITZAF() {
      final Set<Element> ElementSet = Citzaf.getElementSet();

      jPanel_InsideStandards.removeAll();
      jPanel_InsideStandards.setPreferredSize(new Dimension(390, 40 * ElementSet.size()));
      for (final Element elm : ElementSet) {
         final JPanel jPanel_StandardsSingle = new JPanel();
         jPanel_StandardsSingle.setName(elm.toAbbrev());

         final JLabel jLabel_StandardsElement = new JLabel();
         jLabel_StandardsElement.setText(elm.toAbbrev() + " : ");

         final JLabel jLabel_StandardsDescription = new JLabel();
         jLabel_StandardsDescription.setPreferredSize(new Dimension(120, 35));

         if (Citzaf.hasStandard(elm)) {
            jLabel_StandardsDescription.setText(Citzaf.formatComposition(Citzaf.getAStandard(elm)));
            jLabel_StandardsDescription.setToolTipText(jLabel_StandardsDescription.getText());
         } else
            jLabel_StandardsDescription.setText("using a pure element std");

         final JButton jButton_StandardsNewMaterial = new JButton();
         jButton_StandardsNewMaterial.setText("New Material");
         jButton_StandardsNewMaterial.setName(elm.toAbbrev());
         jButton_StandardsNewMaterial.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               final JButton temp = ((JButton) e.getSource());
               final Element elm = Element.byName(temp.getParent().getName());
               e = new ActionEvent(e.getSource(), e.getID(), elm.toAbbrev());
               jButton_StandardsMaterial_actionPerformed(e);
            }
         });

         final JButton jButton_StandardsReset = new JButton();
         jButton_StandardsReset.setText("Reset to pure");
         jButton_StandardsReset.setName(elm.toAbbrev());
         jButton_StandardsReset.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               jButton_StandardsReset_actionPerformed(e);
            }
         });

         jPanel_StandardsSingle.setPreferredSize(new Dimension(400, 35));
         jPanel_StandardsSingle.add(jLabel_StandardsElement, null);
         jPanel_StandardsSingle.add(jLabel_StandardsDescription, null);
         jPanel_StandardsSingle.add(jButton_StandardsNewMaterial, null);
         jPanel_StandardsSingle.add(jButton_StandardsReset, null);
         jPanel_InsideStandards.add(jPanel_StandardsSingle, null);
      }
   }

   private void createCorrectionTab() {
      jPanel_CorrectionsList.setBorder(SwingUtils.createDefaultBorder());
      jPanel_CorrectionsList.setPreferredSize(new Dimension(400, 100));

      jLabel_CorrectionProcedures.setText("Select A Correction Procedure :");

      jPanel_CorrectionsList.setLayout(gridLayout1);
      gridLayout1.setColumns(2);
      jPanel_MACs.setBorder(SwingUtils.createDefaultBorder());
      jPanel_Corrections.add(jLabel_CorrectionProcedures, null);
      jPanel_Corrections.add(jPanel_CorrectionsList, null);
      jPanel_Corrections.add(jLabel_MACListing, null);
      jPanel_Corrections.add(jPanel_MACs, null);
      gridLayout1.setRows(CorrectionAlgorithms.length / 2);

      for (int index = 0; index < CorrectionAlgorithms.length; index++) {
         final JRadioButton Correction = new JRadioButton();
         Correction.setPreferredSize(new Dimension(400, 10));
         Correction.setText(CorrectionAlgorithms[index].toString());
         Correction.setName(Integer.toString(index));
         Correction.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               Citzaf.setCorrectionAlgorithm(e.getActionCommand());
            }
         });

         CorrectionsGroup.add(Correction);
         jPanel_CorrectionsList.add(Correction);
      }

      jLabel_MACListing.setText("Select a Mass Absorption Coefficient Table Choice :");

      jPanel_MACs.setPreferredSize(new Dimension(400, 100));
      for (final gov.nist.microanalysis.EPQLibrary.CITZAF.MassAbsorptionCoefficient algorithm : MACAlgorithms) {
         final JRadioButton MACRadioButton = new JRadioButton();
         MACRadioButton.setPreferredSize(new Dimension(400, 20));
         MACRadioButton.setText(algorithm.toString());
         MACRadioButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               Citzaf.setMACAlgorithm(e.getActionCommand());
               if ((Citzaf.getMACAlgorithm() == CITZAF.MassAbsorptionCoefficient.MASS_ABSORPTION_COEFFICIENTS_FROM_DATA_TABLES)
                     || (Citzaf.getMACAlgorithm() == CITZAF.MassAbsorptionCoefficient.MASS_ABSORPTION_COEFFICIENTS_FROM_HEINRICH_ICXOM_11_EQN)) {
                  jCheckBox_PrintMACs.setSelected(jCheckBox_PrintMACsWasSelected);
                  jCheckBox_PrintMACs.setEnabled(true);
                  jTabbedPane_Main.setEnabledAt(7, false);
                  jCheckBox_PrintMACs_actionPerformed(e);
               } else {
                  Citzaf.clearMACs();
                  jCheckBox_PrintMACs.setSelected(true);
                  jCheckBox_PrintMACs.setEnabled(false);
                  jTabbedPane_Main.setEnabledAt(7, true);
                  jCheckBox_PrintMACs_actionPerformed(e);
               }
            }
         });

         MACGroup.add(MACRadioButton);
         jPanel_MACs.add(MACRadioButton);
      }
   }

   private void loadCorrectionsTabFromCITZAF() {
      if ((Citzaf.getSampleType() == CITZAF.PARTICLE_OR_THIN_FILM)
            && ((Citzaf.getCorrectionAlgorithm() == CITZAF.CITZAFCorrectionAlgorithm.CONVENTIONAL_PHILIBERT_DUNCUMB_REED)
                  || (Citzaf.getCorrectionAlgorithm() == CITZAF.CITZAFCorrectionAlgorithm.HEINRICH_DUNCUMB_REED)
                  || (Citzaf.getCorrectionAlgorithm() == CITZAF.CITZAFCorrectionAlgorithm.LOVE_SCOTT_I)
                  || (Citzaf.getCorrectionAlgorithm() == CITZAF.CITZAFCorrectionAlgorithm.LOVE_SCOTT_II)
                  || (Citzaf.getCorrectionAlgorithm() == CITZAF.CITZAFCorrectionAlgorithm.POUCHOU_AND_PICHOIR_FULL)
                  || (Citzaf.getCorrectionAlgorithm() == CITZAF.CITZAFCorrectionAlgorithm.POUCHOU_AND_PICHOIR_SIMPLIFIED)))
         Citzaf.setCorrectionAlgorithm(CITZAF.CITZAFCorrectionAlgorithm.ARMSTRONG_LOVE_SCOTT.toString());

      final Component[] componentsList = jPanel_CorrectionsList.getComponents();
      for (int index = 0; index < componentsList.length; index++) {
         final JRadioButton Correction = (JRadioButton) componentsList[index];
         if ((Citzaf.getSampleType() == CITZAF.PARTICLE_OR_THIN_FILM)
               && ((CorrectionAlgorithms[index] == CITZAF.CITZAFCorrectionAlgorithm.CONVENTIONAL_PHILIBERT_DUNCUMB_REED)
                     || (CorrectionAlgorithms[index] == CITZAF.CITZAFCorrectionAlgorithm.HEINRICH_DUNCUMB_REED)
                     || (CorrectionAlgorithms[index] == CITZAF.CITZAFCorrectionAlgorithm.LOVE_SCOTT_I)
                     || (CorrectionAlgorithms[index] == CITZAF.CITZAFCorrectionAlgorithm.LOVE_SCOTT_II)
                     || (CorrectionAlgorithms[index] == CITZAF.CITZAFCorrectionAlgorithm.POUCHOU_AND_PICHOIR_FULL)
                     || (CorrectionAlgorithms[index] == CITZAF.CITZAFCorrectionAlgorithm.POUCHOU_AND_PICHOIR_SIMPLIFIED)))
            Correction.setEnabled(false);
         else
            Correction.setEnabled(true);

         if (CorrectionAlgorithms[index] == Citzaf.getCorrectionAlgorithm())
            Correction.setSelected(true);
      }

      final Component[] MACComponents = jPanel_MACs.getComponents();
      for (int index = 0; index < MACComponents.length; index++) {
         final JRadioButton MACRadioButton = (JRadioButton) MACComponents[index];
         if (Citzaf.getMACAlgorithm() == MACAlgorithms[index])
            MACRadioButton.setSelected(true);
      }
   }

   private void createAnalyticalConditionsTab() {
      jLabel_StageCoordinates.setText("Using Stage Coordinates?");

      jRadioButton_StageCoordinatesYes.setText("Yes");
      jRadioButton_StageCoordinatesYes.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jRadioButton_StageCoordinatesYes_actionPerformed(e);
         }
      });
      jRadioButton_StageCoordinatesYes.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jRadioButton_StageCoordinatesYes_actionPerformed(e);
         }
      });

      jRadioButton_StageCoordinatesNo.setText("No");
      jRadioButton_StageCoordinatesNo.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jRadioButton_StageCoordinatesNo_actionPerformed(e);
         }
      });
      jRadioButton_StageCoordinatesNo.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jRadioButton_StageCoordinatesNo_actionPerformed(e);
         }
      });

      StageCoordinatesGroup.add(jRadioButton_StageCoordinatesYes);
      StageCoordinatesGroup.add(jRadioButton_StageCoordinatesNo);

      jLabel_XStageCoordinate.setText("X");

      jTextField_XStageCoordinate.setPreferredSize(new Dimension(30, 21));
      jTextField_XStageCoordinate.setHorizontalAlignment(SwingConstants.TRAILING);
      jTextField_XStageCoordinate.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            jTextField_XStageCoordinate_focusGained(e);
         }
      });

      jLabel_YStageCoordinate.setText("Y");

      jTextField_YStageCoordinate.setPreferredSize(new Dimension(30, 21));
      jTextField_YStageCoordinate.setHorizontalAlignment(SwingConstants.TRAILING);
      jTextField_YStageCoordinate.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            jTextField_YStageCoordinate_focusGained(e);
         }
      });

      jLabel_Spacer23.setPreferredSize(new Dimension(34, 15));

      jPanel_StageCoordinates.setPreferredSize(new Dimension(400, 35));
      jPanel_StageCoordinates.add(jLabel_StageCoordinates, null);
      jPanel_StageCoordinates.add(jRadioButton_StageCoordinatesYes, null);
      jPanel_StageCoordinates.add(jRadioButton_StageCoordinatesNo, null);
      jPanel_StageCoordinates.add(jLabel_XStageCoordinate, null);
      jPanel_StageCoordinates.add(jTextField_XStageCoordinate, null);
      jPanel_StageCoordinates.add(jLabel_YStageCoordinate, null);
      jPanel_StageCoordinates.add(jTextField_YStageCoordinate, null);
      jPanel_StageCoordinates.add(jLabel_Spacer23, null);

      jLabel_DeadtimeCorrection.setText("Deadtime Correction :");

      jTextField_DeadtimeCorrection.setPreferredSize(new Dimension(45, 21));
      jTextField_DeadtimeCorrection.setHorizontalAlignment(SwingConstants.TRAILING);
      jTextField_DeadtimeCorrection.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            jTextField_DeadtimeCorrection_focusGained(e);
         }
      });

      jLabel_DTMicroseconds.setText("�sec");

      jLabel_Spacer11.setPreferredSize(new Dimension(167, 15));

      jPanel_DeadtimeCorrection.setPreferredSize(new Dimension(400, 35));
      jPanel_DeadtimeCorrection.add(jLabel_DeadtimeCorrection, null);
      jPanel_DeadtimeCorrection.add(jTextField_DeadtimeCorrection, null);
      jPanel_DeadtimeCorrection.add(jLabel_DTMicroseconds, null);
      jPanel_DeadtimeCorrection.add(jLabel_Spacer11, null);

      jPanel_DriftFactors.setPreferredSize(new Dimension(400, 170));
      jPanel_DriftFactors.add(jLabel_DriftFactors, null);
      jPanel_DriftFactors.add(jLabel_Spacer13, null);
      jPanel_DriftFactors.add(jScrollPane_DriftFactors, null);
      jPanel_DriftFactors.add(jLabel_Spacer14, null);

      jScrollPane_DriftFactors.getViewport().add(jPanel_InsideDriftFactors, null);
      jScrollPane_DriftFactors.setPreferredSize(new Dimension(180, 145));

      jLabel_DriftFactors.setText("Drift Factors :");

      jLabel_Spacer13.setPreferredSize(new Dimension(283, 15));

      jLabel_Spacer14.setPreferredSize(new Dimension(170, 15));

      jPanel_AnalyticalConditions.add(jPanel_StageCoordinates, null);
      jPanel_AnalyticalConditions.add(jPanel_DeadtimeCorrection, null);
      jPanel_AnalyticalConditions.add(jPanel_DriftFactors, null);
   }

   private void loadAnalyticalConditionsTabFromCITZAF() {
      jRadioButton_StageCoordinatesYes.setSelected(Citzaf.isUsingStageCoordinates());
      jRadioButton_StageCoordinatesNo.setSelected(!Citzaf.isUsingStageCoordinates());

      jTextField_XStageCoordinate.setText(Double.toString(Citzaf.getXStageCoordinate()));
      jTextField_XStageCoordinate.setEnabled(jRadioButton_StageCoordinatesYes.isSelected());

      jTextField_YStageCoordinate.setText(Double.toString(Citzaf.getYStageCoordinate()));
      jTextField_YStageCoordinate.setEnabled(jRadioButton_StageCoordinatesYes.isSelected());

      jTextField_DeadtimeCorrection.setText(Double.toString(Citzaf.getDeadtimeCorrection()));

      drawDriftFactors();
   }

   private void createOutputTab() {
      jLabel_Title.setText("Title :");

      jTextField_Title.setPreferredSize(new Dimension(170, 21));

      jTextField_Title.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            jTextField_Title_focusGained(e);
         }
      });

      jLabel_Spacer15.setPreferredSize(new Dimension(150, 15));

      jPanel_Title.setPreferredSize(new Dimension(400, 35));
      jPanel_Title.add(jLabel_Title, null);
      jPanel_Title.add(jTextField_Title, null);
      jPanel_Title.add(jLabel_Spacer15, null);

      jCheckBox_PrintMACs.setText("Printout of Mass Absorption Coefficients");
      jCheckBox_PrintMACs.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jCheckBox_PrintMACs_actionPerformed(e);
         }
      });

      jLabel_Spacer16.setPreferredSize(new Dimension(135, 15));

      jCheckBox_PrintZAFs.setText("Printout of ZAF factors and standard compositions");
      jCheckBox_PrintZAFs.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jCheckBox_PrintZAFs_actionPerformed(e);
         }
      });

      jLabel_Spacer17.setPreferredSize(new Dimension(87, 15));

      jPanel_OutputOptions.setPreferredSize(new Dimension(400, 60));
      jPanel_OutputOptions.add(jCheckBox_PrintMACs, null);
      jPanel_OutputOptions.add(jLabel_Spacer16, null);
      jPanel_OutputOptions.add(jCheckBox_PrintZAFs, null);
      jPanel_OutputOptions.add(jLabel_Spacer17, null);

      jLabel_ConcentrationOutput.setText("Minor elements concentrations outputed to the :");

      jLabel_Spacer18.setPreferredSize(new Dimension(118, 15));

      jRadioButton_NearestPPM.setText("Nearest PPM");
      jRadioButton_NearestPPM.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jRadioButton_NearestPPM_actionPerformed(e);
         }
      });

      jRadioButton_NearestHundreth.setText("Nearest 0.01%");
      jRadioButton_NearestHundreth.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jRadioButton_NearestHundreth_actionPerformed(e);
         }
      });

      ConcentrationOutputGroup.add(jRadioButton_NearestPPM);
      ConcentrationOutputGroup.add(jRadioButton_NearestHundreth);

      jLabel_Spacer19.setPreferredSize(new Dimension(167, 15));

      jPanel_ConcentrationOutput.setPreferredSize(new Dimension(400, 80));
      jPanel_ConcentrationOutput.add(jLabel_ConcentrationOutput, null);
      jPanel_ConcentrationOutput.add(jLabel_Spacer18, null);
      jPanel_ConcentrationOutput.add(jRadioButton_NearestPPM, null);
      jPanel_ConcentrationOutput.add(jRadioButton_NearestHundreth, null);
      jPanel_ConcentrationOutput.add(jLabel_Spacer19, null);

      jPanel_Output.add(jPanel_Title, null);
      jPanel_Output.add(jPanel_OutputOptions, null);
      jPanel_Output.add(jPanel_ConcentrationOutput, null);
   }

   private void loadOutputTabFromCITZAF() {
      if (!Citzaf.getTitle().equals(""))
         jTextField_Title.setText(Citzaf.getTitle());
      else
         jTextField_Title.setText("Enter your sample title here");

      jCheckBox_PrintMACs.setSelected(Citzaf.getPrintoutMACs() == CITZAF.PRINTOUT_MACS);
      jCheckBox_PrintZAFs.setSelected(Citzaf.getPrintoutZAF() == CITZAF.PRINTOUT_ZAF);

      jRadioButton_NearestPPM.setSelected(Citzaf.getMinorElementsOutput() == CITZAF.MINOR_ELEMENTS_CONCENTRATION_IN_PPM);
      jRadioButton_NearestHundreth.setSelected(Citzaf.getMinorElementsOutput() == CITZAF.MINOR_ELEMENTS_CONCENTRATION_IN_NEAREST_HUNDRETH);
   }

   private void createParticlesTab() {
      jLabel_SampleIs.setText("Sample is :");

      jRadioButton_Bulk.setText("Bulk");
      jRadioButton_Bulk.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jRadioButton_Bulk_actionPerformed(e);
         }
      });

      jRadioButton_Particle.setText("Particle or Thin Film");
      jRadioButton_Particle.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jRadioButton_Particle_actionPerformed(e);
         }
      });
      jLabel_Spacer5.setPreferredSize(new Dimension(130, 20));

      BulkOrParticleGroup.add(jRadioButton_Bulk);
      BulkOrParticleGroup.add(jRadioButton_Particle);

      jPanel_Bulk.setPreferredSize(new Dimension(400, 35));
      jPanel_Bulk.add(jLabel_SampleIs, null);
      jPanel_Bulk.add(jRadioButton_Bulk, null);
      jPanel_Bulk.add(jRadioButton_Particle, null);
      jPanel_Bulk.add(jLabel_Spacer5, null);

      jLabel_ParticleModel.setText("Particle Models :");

      jLabel_ParticleModelsContent.setPreferredSize(new Dimension(180, 15));

      jButton_EditModels.setText("Edit Models");
      jButton_EditModels.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_EditModels_actionPerformed(e);
         }
      });

      jPanel_ParticleModels.setPreferredSize(new Dimension(400, 35));
      jPanel_ParticleModels.add(jLabel_ParticleModel, null);
      jPanel_ParticleModels.add(jLabel_ParticleModelsContent, null);
      jPanel_ParticleModels.add(jButton_EditModels, null);

      jLabel_ParticleDiameter.setText("Particle Diameter :");

      jLabel_ParticleDiametersContent.setPreferredSize(new Dimension(160, 15));

      jButton_EditDiameters.setText("Edit Diameters");
      jButton_EditDiameters.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_EditDiameters_actionPerformed(e);
         }
      });

      jPanel_ParticleDiameter.setPreferredSize(new Dimension(400, 40));
      jPanel_ParticleDiameter.add(jLabel_ParticleDiameter, null);
      jPanel_ParticleDiameter.add(jLabel_ParticleDiametersContent, null);
      jPanel_ParticleDiameter.add(jButton_EditDiameters, null);

      jLabel_SampleDensity.setPreferredSize(new Dimension(148, 15));
      jLabel_SampleDensity.setText("Sample Density :");

      jTextField_SampleDensity.setPreferredSize(new Dimension(45, 21));
      jTextField_SampleDensity.setHorizontalAlignment(SwingConstants.TRAILING);
      jTextField_SampleDensity.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            jTextField_SampleDensity_focusGained(e);
         }
      });

      jLabel_SampleDensityUnits.setText("g/cm^3");

      jLabel_Spacer20.setPreferredSize(new Dimension(118, 15));

      jPanel_SampleDensity.setPreferredSize(new Dimension(400, 35));
      jPanel_SampleDensity.add(jLabel_SampleDensity, null);
      jPanel_SampleDensity.add(jTextField_SampleDensity, null);
      jPanel_SampleDensity.add(jLabel_SampleDensityUnits, null);
      jPanel_SampleDensity.add(jLabel_Spacer20, null);

      jLabel_SizeFactor.setText("Ratio of thickness to diameter :");

      jTextField_SizeFactor.setPreferredSize(new Dimension(45, 21));
      jTextField_SizeFactor.setHorizontalAlignment(SwingConstants.TRAILING);
      jTextField_SizeFactor.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            jTextField_SizeFactor_focusGained(e);
         }
      });

      jLabel_Spacer21.setPreferredSize(new Dimension(154, 15));

      jPanel_SizeFactor.setPreferredSize(new Dimension(400, 35));
      jPanel_SizeFactor.add(jLabel_SizeFactor, null);
      jPanel_SizeFactor.add(jTextField_SizeFactor, null);
      jPanel_SizeFactor.add(jLabel_Spacer21, null);

      jLabel_IntegrationStep.setPreferredSize(new Dimension(148, 15));
      jLabel_IntegrationStep.setText("Numerical Integration Step :");

      jTextField_IntegrationStep.setPreferredSize(new Dimension(45, 21));
      jTextField_IntegrationStep.setHorizontalAlignment(SwingConstants.TRAILING);
      jTextField_IntegrationStep.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            jTextField_IntegrationStep_focusGained(e);
         }
      });

      jLabel_Spacer22.setPreferredSize(new Dimension(154, 15));

      jPanel_IntegrationStep.setPreferredSize(new Dimension(400, 35));
      jPanel_IntegrationStep.add(jLabel_IntegrationStep, null);
      jPanel_IntegrationStep.add(jTextField_IntegrationStep, null);
      jPanel_IntegrationStep.add(jLabel_Spacer22, null);

      jPanel_Particles.add(jPanel_Bulk, null);
      jPanel_Particles.add(jPanel_ParticleModels, null);
      jPanel_Particles.add(jPanel_ParticleDiameter, null);
      jPanel_Particles.add(jPanel_SampleDensity, null);
      jPanel_Particles.add(jPanel_SizeFactor, null);
      jPanel_Particles.add(jPanel_IntegrationStep, null);
   }

   private void loadParticlesTabFromCITZAF() {
      jRadioButton_Bulk.setSelected(Citzaf.getSampleType() == CITZAF.BULK);
      jRadioButton_Particle.setSelected(Citzaf.getSampleType() == CITZAF.PARTICLE_OR_THIN_FILM);

      toggleParticlesTab(Citzaf.getSampleType() == CITZAF.PARTICLE_OR_THIN_FILM);

      jLabel_ParticleModelsContent.setText(Citzaf.formatParticleModelsText(Citzaf.getParticleModels()));
      jLabel_ParticleModelsContent.setToolTipText(jLabel_ParticleModelsContent.getText());

      jLabel_ParticleDiametersContent.setText(Citzaf.formatParticleDiameterText(Citzaf.getParticleDiameters()));
      jLabel_ParticleDiametersContent.setToolTipText(jLabel_ParticleDiametersContent.getText());

      jTextField_SampleDensity.setText(Double.toString(Citzaf.getParticleDensity()));
      jTextField_SizeFactor.setText(Double.toString(Citzaf.getThicknessFactor()));
      jTextField_IntegrationStep.setText(Double.toString(Citzaf.getNumericalIntegrationStep()));
   }

   private void createMACsTab() {
      jScrollPane_MACs.setPreferredSize(new Dimension(330, 225));
      jScrollPane_MACs.getViewport().add(jPanel_InsideMACs, null);

      jLabel_Absorbers.setText("Absorbers");

      jPanel_Absorbers.setPreferredSize(new Dimension(400, 20));
      jPanel_Absorbers.add(jLabel_Absorbers, null);

      jLabel_Emitters.setText("Emitters");

      jPanel_ManualMACs.add(jPanel_Absorbers, null);
      jPanel_ManualMACs.add(jLabel_Emitters, null);
      jPanel_ManualMACs.add(jScrollPane_MACs, null);
   }

   private void AddTabs() {
      jTabbedPane_Main.add("Quick Start", jPanel_QuickStart);

      jTabbedPane_Main.add("Standards", jPanel_Standards);
      jTabbedPane_Main.add("Corrections", jPanel_Corrections);
      jTabbedPane_Main.add("Analytical Conditions", jPanel_AnalyticalConditions);
      jTabbedPane_Main.add("Output", jPanel_Output);
      jTabbedPane_Main.add("Particles\\Thin Film", jPanel_Particles);
      jTabbedPane_Main.add("Calc k-ratios", jPanel_SampleTab);
      jTabbedPane_Main.add("MACs", jPanel_ManualMACs);
      jTabbedPane_Main.setEnabledAt(7, false);
   }

   private void commitToCitzaf() {
      commitQSPanel();
      commitSampleTab();
      commitStandardsTab();
      commitCorrectionsTab();
      commitAnalyticalConditionsTab();
      commitOutputTab();
      commitParticlesTab();
      commitPreferences();
   }

   private void commitQSPanel() {
      double BeamEnergy = 0.0;
      final NumberFormat nf = NumberFormat.getInstance();
      try {
         BeamEnergy = nf.parse(jTextField_BeamEnergyQS.getText()).doubleValue();
         if (BeamEnergy < 0.0)
            throw new NumberFormatException("Beam Energy must be greater than 0.0 keV");
         Citzaf.setBeamEnergy(BeamEnergy);
      } catch (final ParseException nfe) {
         jTabbedPane_Main.setSelectedComponent(jPanel_QuickStart);
         jTextField_BeamEnergyQS.grabFocus();
         jTextField_BeamEnergyQS.selectAll();
      }

      double TakeOffAngle = 0.0;
      try {
         TakeOffAngle = nf.parse(jTextField_TakeOffAngleQS.getText()).doubleValue();
         if (TakeOffAngle < 0.0)
            throw new Exception("Take off angle must be greater than 0.0 degrees");
         Citzaf.setTakeOffAngle(TakeOffAngle);
      } catch (final Exception nfe2) {
         jTabbedPane_Main.setSelectedComponent(jPanel_QuickStart);
         jTextField_TakeOffAngleQS.grabFocus();
         jTextField_TakeOffAngleQS.selectAll();
      }
   }

   private void commitSampleTab() {
      // nothing to commit, everything is committed on the fly
   }

   private void commitStandardsTab() {
      // nothing to commit, everything is committed on the fly
   }

   private void commitCorrectionsTab() {
      // nothing to commit, everything is committed on the fly
   }

   private void commitAnalyticalConditionsTab() {
      final NumberFormat nf = NumberFormat.getInstance();
      if (Citzaf.isUsingStageCoordinates()) {
         try {
            final double X = nf.parse(jTextField_XStageCoordinate.getText()).doubleValue();
            Citzaf.setXStageCoordinate(X);
         } catch (final ParseException nfex) {
            jTextField_XStageCoordinate.grabFocus();
            jTextField_XStageCoordinate.selectAll();
         }

         try {
            final double Y = nf.parse(jTextField_YStageCoordinate.getText()).doubleValue();
            Citzaf.setYStageCoordinate(Y);
         } catch (final ParseException nfex) {
            jTextField_YStageCoordinate.grabFocus();
            jTextField_YStageCoordinate.selectAll();
         }

      }

      try {
         final double deadtimeCorrection = nf.parse(jTextField_DeadtimeCorrection.getText()).doubleValue();
         if (deadtimeCorrection < 0.0)
            throw new NumberFormatException();
         Citzaf.setDeadtimeCorrection(deadtimeCorrection);
      } catch (final ParseException nfex) {
         jTabbedPane_Main.setSelectedComponent(jPanel_AnalyticalConditions);
         jTextField_DeadtimeCorrection.grabFocus();
         jTextField_DeadtimeCorrection.selectAll();
      }

      if (DriftFactorTextFields != null) {
         int index = 0;
         try {
            for (index = 0; index < DriftFactorTextFields.length; index++)
               if (DriftFactorTextFields[index] != null) {
                  final double driftFactor = nf.parse(DriftFactorTextFields[index].getText()).doubleValue();
                  if (driftFactor < 0.0)
                     throw new NumberFormatException();
                  Citzaf.setDriftFactor(Element.byName(DriftFactorTextFields[index].getName()), driftFactor);
               } else
                  Citzaf.setDriftFactor(Element.byName(DriftFactorTextFields[index].getName()), 1.0);
         } catch (final ParseException nfex2) {
            Citzaf.clearDriftFactors();
            jTabbedPane_Main.setSelectedComponent(jPanel_AnalyticalConditions);
            DriftFactorTextFields[index].grabFocus();
            DriftFactorTextFields[index].selectAll();
         }
      }
   }

   private void commitOutputTab() {
      Citzaf.setTitle(jTextField_Title.getText());
   }

   private void commitParticlesTab() {
      final NumberFormat nf = NumberFormat.getInstance();
      try {
         final double Density = nf.parse(jTextField_SampleDensity.getText()).doubleValue();
         if (Density < 0.0)
            throw new NumberFormatException();
         Citzaf.setParticleDensity(Density);
      } catch (final ParseException nfex) {
         jTabbedPane_Main.setSelectedComponent(jPanel_Particles);
         jTextField_SampleDensity.grabFocus();
         jTextField_SampleDensity.selectAll();
      }

      try {
         final double ThicknessFactor = nf.parse(jTextField_SizeFactor.getText()).doubleValue();
         if (ThicknessFactor < 0.0)
            throw new NumberFormatException();
         Citzaf.setThicknessFactor(ThicknessFactor);
      } catch (final ParseException nfex) {
         jTabbedPane_Main.setSelectedComponent(jPanel_Particles);
         jTextField_SizeFactor.grabFocus();
         jTextField_SizeFactor.selectAll();
      }

      try {
         final double IntegrationStep = nf.parse(jTextField_IntegrationStep.getText()).doubleValue();
         if (IntegrationStep < 0.0)
            throw new NumberFormatException();
         Citzaf.setNumericalIntegrationStep(IntegrationStep);
      } catch (final ParseException nfex) {
         jTabbedPane_Main.setSelectedComponent(jPanel_Particles);
         jTextField_IntegrationStep.grabFocus();
         jTextField_IntegrationStep.selectAll();
      }
   }

   private void commitMAC(JTextField box) {
      try {
         double MACvalue = 0.0;
         try {
            MACvalue = mDefaultFmt.parse(box.getText()).doubleValue();
         } catch (final ParseException ex1) {
            throw new NumberFormatException();
         }
         if (MACvalue < 0.0)
            throw new NumberFormatException();

         final String temp = box.getName();
         final Element elm = Element.byName(temp.substring(0, temp.indexOf(" ")));
         final Element absorber = Element.byName(temp.substring(temp.indexOf(" ") + 1, temp.length()));

         String Line = null;
         if (Citzaf.hasLine(elm))
            Line = Citzaf.getLine(elm);
         else
            Line = chooseBestLine(elm);
         XRayTransition xrt = null;
         if (Line != null)
            if (Line.startsWith("K"))
               xrt = new XRayTransition(elm, XRayTransition.KA1);
            else if (Line.startsWith("L"))
               xrt = new XRayTransition(elm, XRayTransition.LA1);
            else if (Line.startsWith("M"))
               xrt = new XRayTransition(elm, XRayTransition.MA1);
         if (xrt != null)
            Citzaf.setMAC(absorber, xrt, MACvalue);
         threwNumberFormatException = false;
      } catch (final NumberFormatException ex) {
         if (box != null) {
            threwNumberFormatException = true;
            jTabbedPane_Main.setSelectedComponent(jPanel_ManualMACs);
            box.grabFocus();
            box.selectAll();
         }
      }
   }

   private void commitPreferences() {
      userPref.putInt("Dialog Position X", this.getX());
      userPref.putInt("Dialog Position Y", this.getY());
   }

   void jButton_NewMaterialQS_actionPerformed(ActionEvent e) {
      final Composition comp = MaterialsCreator.editMaterial(this, Citzaf.getComposition(), false);
      if (comp != null) {
         Citzaf.setComposition(comp);
         loadDataFromCITZAF();
      }
   }

   private void drawLines() {
      final Set<Element> ElementSet = Citzaf.getElementSet();

      jPanel_LineInside.removeAll();
      jPanel_LineInside.setPreferredSize(new Dimension(180, 35 * ElementSet.size()));

      LineButtonGroup = new ButtonGroup[ElementSet.size()];

      int ElementCounter = 0;
      for (final Element elm : ElementSet) {

         final Map<Element, String> CitzafMap = Citzaf.getElementToLineMap();

         final JLabel jLabel_ElementName = new JLabel();
         jLabel_ElementName.setText("Element : " + elm.toAbbrev());

         final JPanel jPanel_LineTemp = new JPanel();
         jPanel_LineTemp.setBorder(SwingUtils.createDefaultBorder());
         jPanel_LineTemp.setPreferredSize(new Dimension(300, 30));
         jPanel_LineTemp.setName(elm.toAbbrev());
         jPanel_LineTemp.add(jLabel_ElementName, null);

         LineButtonGroup[ElementCounter] = new ButtonGroup();

         final String[] LineList = {"K", "L", "M"};
         for (final String element : LineList) {
            final JRadioButton jRadioButton_Line = new JRadioButton();
            jRadioButton_Line.setText(element);
            jRadioButton_Line.setBorder(SwingUtils.createDefaultBorder());
            jRadioButton_Line.addActionListener(new java.awt.event.ActionListener() {
               @Override
               public void actionPerformed(ActionEvent e) {
                  if (((JRadioButton) e.getSource()).isSelected()) {
                     final Element elm = Element.byName(((JPanel) ((JRadioButton) e.getSource()).getParent()).getName());
                     if (e.getActionCommand().matches("K"))
                        Citzaf.setLine(elm, CITZAF.Line.KLINE);
                     else if (e.getActionCommand().matches("L"))
                        Citzaf.setLine(elm, CITZAF.Line.LLINE);
                     else if (e.getActionCommand().matches("M"))
                        Citzaf.setLine(elm, CITZAF.Line.MLINE);
                  }
               }
            });

            jPanel_LineTemp.add(jRadioButton_Line, null);

            LineButtonGroup[ElementCounter].add(jRadioButton_Line);

            String Line;
            if ((CitzafMap.size() > 0) && (CitzafMap.containsKey(elm)))
               Line = CitzafMap.get(elm).toString();
            else
               Line = chooseBestLine(elm);
            ElementToLineMap.put(elm, Line);
            if (element.compareTo(Line) == 0)
               jRadioButton_Line.setSelected(true);
         }
         jPanel_LineInside.add(jPanel_LineTemp, null);
         ++ElementCounter;
      }
      Citzaf.setElementToLineMap(ElementToLineMap);

      // Bug fix, sometimes the panel wouldn't appear when the
      // radio buttons were switched
      jPanel_LineInside.updateUI();
   }

   private String chooseBestLine(Element elm) {
      if (FromSI.keV(AtomicShell.getEdgeEnergy(elm, AtomicShell.K)) < ((2.0 / 3.0) * Citzaf.getBeamEnergy()))
         return CITZAF.Line.KLINE.toString();
      else if (FromSI.keV(AtomicShell.getEdgeEnergy(elm, AtomicShell.LI)) < ((2.0 / 3.0) * Citzaf.getBeamEnergy()))
         return CITZAF.Line.LLINE.toString();
      else if (FromSI.keV(AtomicShell.getEdgeEnergy(elm, AtomicShell.MI)) < ((2.0 / 3.0) * Citzaf.getBeamEnergy()))
         return CITZAF.Line.MLINE.toString();
      return CITZAF.Line.KLINE.toString();

      // Older code for determining the line
      /*
       * if(atomicNumber <= 32){ return CITZAF.Line.KLINE; } else
       * if((((atomicNumber >= 33) && (atomicNumber <= 80)) || ((atomicNumber >=
       * 84) && (atomicNumber <= 89))) || ((atomicNumber >= 93) && (atomicNumber
       * <= 99))){ return CITZAF.Line.LLINE; } else if((((atomicNumber >= 81) &&
       * (atomicNumber <= 83)) || ((atomicNumber >= 90) && (atomicNumber <=
       * 92))) || (atomicNumber >= 100)){ return CITZAF.Line.MLINE; } return
       * CITZAF.Line.KLINE;
       */

   }

   private void drawDriftFactors() {
      final Set<Element> ElementSet = Citzaf.getElementSet();

      jPanel_InsideDriftFactors.removeAll();
      jPanel_InsideDriftFactors.setPreferredSize(new Dimension(180, 35 * ElementSet.size()));

      DriftFactorTextFields = new JTextField[ElementSet.size()];

      final int Counter = 0;
      for (final Element elm : ElementSet) {
         final JPanel jPanel_DriftPanel = new JPanel();
         jPanel_DriftPanel.setPreferredSize(new Dimension(200, 30));

         final JLabel jLabel_TempElement = new JLabel("Drift factor for " + elm.toAbbrev() + " : ");

         final JTextField jTextField_DriftFactor = new JTextField("1.0");
         if (Citzaf.hasDriftFactor(elm))
            jTextField_DriftFactor.setText(Double.toString(Citzaf.getDriftFactor(elm)));
         jTextField_DriftFactor.setName(elm.toAbbrev());
         jTextField_DriftFactor.setPreferredSize(new Dimension(45, 21));
         jTextField_DriftFactor.setHorizontalAlignment(SwingConstants.TRAILING);
         jTextField_DriftFactor.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
               ((JTextField) e.getSource()).selectAll();
            }
         });
         DriftFactorTextFields[Counter] = jTextField_DriftFactor;
         final JLabel tempSpacer = new JLabel();
         tempSpacer.setPreferredSize(new Dimension(30, 30));

         jPanel_DriftPanel.add(jLabel_TempElement);
         jPanel_DriftPanel.add(jTextField_DriftFactor);
         jPanel_DriftPanel.add(tempSpacer);

         jPanel_InsideDriftFactors.add(jPanel_DriftPanel);
      }
      jScrollPane_DriftFactors.getViewport().add(jPanel_InsideDriftFactors, null);
   }

   void jTextField_BeamEnergyQS_focusGained(FocusEvent e) {
      jTextField_BeamEnergyQS.selectAll();
   }

   void jTextField_TakeOffAngleQS_focusGained(FocusEvent e) {
      jTextField_TakeOffAngleQS.selectAll();
   }

   void jRadioButton_Bulk_actionPerformed(ActionEvent e) {
      Citzaf.setSampleType(CITZAF.BULK);
      toggleParticlesTab(false);
      // createCorrectionTab();
      loadCorrectionsTabFromCITZAF();
   }

   private void toggleParticlesTab(boolean Enabled) {
      jLabel_ParticleModel.setEnabled(Enabled);
      jLabel_ParticleModelsContent.setEnabled(Enabled);
      jButton_EditModels.setEnabled(Enabled);
      jLabel_ParticleDiameter.setEnabled(Enabled);
      jLabel_ParticleDiametersContent.setEnabled(Enabled);
      jButton_EditDiameters.setEnabled(Enabled);
      jLabel_SampleDensity.setEnabled(Enabled);
      jTextField_SampleDensity.setEnabled(Enabled);
      jLabel_SampleDensityUnits.setEnabled(Enabled);
      jLabel_SizeFactor.setEnabled(Enabled);
      jTextField_SizeFactor.setEnabled(Enabled);
      jLabel_IntegrationStep.setEnabled(Enabled);
      jTextField_IntegrationStep.setEnabled(Enabled);
   }

   void jRadioButton_Particle_actionPerformed(ActionEvent e) {
      Citzaf.setSampleType(CITZAF.PARTICLE_OR_THIN_FILM);
      toggleParticlesTab(true);
      // createCorrectionTab();
      loadCorrectionsTabFromCITZAF();
   }

   void jButton_StandardsMaterial_actionPerformed(ActionEvent e) {
      final MaterialsCreator mc = new MaterialsCreator(this, "Standard for " + e.getActionCommand(), true);
      mc.setLocationRelativeTo(this);
      if (Citzaf.hasStandard(Element.byName(e.getActionCommand())))
         mc.setMaterial(Citzaf.getAStandard(Element.byName(e.getActionCommand())));
      else {
         final Composition mat = new Composition();
         mat.setName("Standard for " + e.getActionCommand());
         mc.setMaterial(mat);
      }
      mc.setVisible(true);
      final Composition mat = mc.getMaterial();
      if (mat != null) {
         String elementName = ((MaterialsCreator) e.getSource()).getTitle();
         elementName = elementName.substring(elementName.lastIndexOf(" ")).trim();
         Citzaf.setAStandard(Element.byName(elementName), mat);
         loadStandardsTabFromCITZAF();
      }
   }

   void jButton_StandardsReset_actionPerformed(ActionEvent e) {
      final String element = ((JButton) e.getSource()).getName();
      Citzaf.setAStandard(Element.byName(element), new Composition());

      ((JButton) e.getSource()).updateUI();
      loadStandardsTabFromCITZAF();
   }

   void jRadioButton_StageCoordinatesYes_actionPerformed(ActionEvent e) {
      jTextField_XStageCoordinate.setEnabled(true);
      jTextField_YStageCoordinate.setEnabled(true);
      Citzaf.setUseStageCoordinates(CITZAF.USE_STAGE_COORDINATES);
   }

   void jRadioButton_StageCoordinatesNo_actionPerformed(ActionEvent e) {
      jTextField_XStageCoordinate.setEnabled(false);
      jTextField_YStageCoordinate.setEnabled(false);
      Citzaf.setUseStageCoordinates(CITZAF.DO_NOT_USE_STAGE_COORDINATES);
   }

   void jTextField_XStageCoordinate_focusGained(FocusEvent e) {
      jTextField_XStageCoordinate.selectAll();
   }

   void jTextField_YStageCoordinate_focusGained(FocusEvent e) {
      jTextField_YStageCoordinate.selectAll();
   }

   void jTextField_DeadtimeCorrection_focusGained(FocusEvent e) {
      jTextField_DeadtimeCorrection.selectAll();
   }

   void jTextField_Title_focusGained(FocusEvent e) {
      jTextField_Title.selectAll();
   }

   void jCheckBox_PrintMACs_actionPerformed(ActionEvent e) {
      if (jCheckBox_PrintMACs.isEnabled())
         jCheckBox_PrintMACsWasSelected = jCheckBox_PrintMACs.isSelected();
      if (jCheckBox_PrintMACs.isSelected())
         Citzaf.setPrintoutMACs(CITZAF.PRINTOUT_MACS);
      else
         Citzaf.setPrintoutMACs(CITZAF.DO_NOT_PRINTOUT_MACS);
   }

   void jCheckBox_PrintZAFs_actionPerformed(ActionEvent e) {
      if (jCheckBox_PrintZAFs.isSelected())
         Citzaf.setPrintoutZAF(CITZAF.PRINTOUT_ZAF);
      else
         Citzaf.setPrintoutZAF(CITZAF.DO_NOT_PRINTOUT_ZAF);
   }

   void jRadioButton_NearestPPM_actionPerformed(ActionEvent e) {
      if (jRadioButton_NearestPPM.isSelected())
         Citzaf.setMinorElementsOutput(CITZAF.MINOR_ELEMENTS_CONCENTRATION_IN_PPM);
   }

   void jRadioButton_NearestHundreth_actionPerformed(ActionEvent e) {
      if (jRadioButton_NearestHundreth.isSelected())
         Citzaf.setMinorElementsOutput(CITZAF.MINOR_ELEMENTS_CONCENTRATION_IN_NEAREST_HUNDRETH);
   }

   void jTextField_SampleDensity_focusGained(FocusEvent e) {
      jTextField_SampleDensity.selectAll();
   }

   void jTextField_SizeFactor_focusGained(FocusEvent e) {
      jTextField_SizeFactor.selectAll();
   }

   void jTextField_IntegrationStep_focusGained(FocusEvent e) {
      jTextField_IntegrationStep.selectAll();
   }

   void jButton_BeginAnalysis_actionPerformed(ActionEvent e) {
      // save all changes
      commitToCitzaf();

      fireBeginAnalysisEvent(e);
      /*
       * commitToCitzaf(); waitFrame.setLocation(this.getX() + 50, this.getY() +
       * 50); waitFrame.setTitle("Please be patient, CITZAF loading..."); JLabel
       * message = new JLabel("Please wait, this computation may take a few
       * seconds..."); message.setHorizontalTextPosition(SwingConstants.CENTER);
       * waitFrame.getContentPane().add(message); waitFrame.setSize(300, 100);
       * waitFrame.setVisible(true); waitFrame.update(waitFrame.getGraphics());
       * Citzaf.dumpToFileAndRunCITZAF(); waitFrame.setVisible(false); if
       * (DEBUG_MODE){ try { System.out.println(Citzaf.getFullResults());
       * System.out.println("---------------------------------------");
       * System.out.println(Citzaf.getCondensedResults()); } catch(IOException
       * ioex) { JOptionPane.showMessageDialog(this.getGlassPane(), "An error
       * while reading dataout file. Please check the dataout file to ensure
       * that it is in the correct format", "Error while reading file!",
       * JOptionPane.ERROR_MESSAGE); //ioex.printStackTrace(System.err); } }
       */
   }

   private void loadMACTable() {
      jPanel_InsideMACs.removeAll();
      jPanel_InsideMACs.setPreferredSize(new Dimension(320, 215));
      jPanel_InsideMACs.setLayout(gridLayout2);

      mDefaultFmt.setMaximumFractionDigits(0);
      mDefaultFmt.setGroupingUsed(false);

      final Set<Element> masterElementSet = getMasterElementSet();

      jPanel_InsideMACs.setPreferredSize(new Dimension(45 * (masterElementSet.size() + 1), 38 * masterElementSet.size()));

      gridLayout2.setColumns(masterElementSet.size() + 1);
      gridLayout2.setVgap(5);
      gridLayout2.setRows(masterElementSet.size() + 1);
      gridLayout2.setHgap(5);

      final JLabel TempSpacer = new JLabel();
      TempSpacer.setPreferredSize(new Dimension(35, 25));

      final JPanel jPanel_MACAbsorbers = new JPanel();
      jPanel_MACAbsorbers.add(TempSpacer, null);

      for (final Element elm : masterElementSet) {
         final JLabel ElementLabel = new JLabel(elm.toAbbrev());
         ElementLabel.setPreferredSize(new Dimension(40, 20));
         jPanel_MACAbsorbers.add(ElementLabel, null);
      }
      jPanel_InsideMACs.add(jPanel_MACAbsorbers, null);

      final MassAbsorptionCoefficient macTable = chooseMACTable();

      for (final Element elm : masterElementSet) {

         final JLabel ElementLabel = new JLabel(elm.toAbbrev());

         final JPanel PanelRow = new JPanel();
         PanelRow.setName(elm.toAbbrev());
         PanelRow.add(ElementLabel, null);

         for (final Element absorber : masterElementSet) {

            final JTextField Box = new JTextField();
            Box.setPreferredSize(new Dimension(40, 20));
            Box.setName(elm + " " + absorber);
            Box.addFocusListener(new java.awt.event.FocusAdapter() {
               @Override
               public void focusGained(FocusEvent e) {
                  ((JTextField) e.getSource()).selectAll();
               }
            });

            Box.addFocusListener(new java.awt.event.FocusAdapter() {
               @Override
               public void focusLost(FocusEvent e) {
                  final JTextField box = (JTextField) e.getSource();
                  commitMAC(box);
               }
            });

            final double MAC = computeMAC(macTable, elm, absorber);
            Box.setText(mDefaultFmt.format(MAC));
            PanelRow.add(Box, null);
         }
         jPanel_InsideMACs.add(PanelRow, null);
      }
   }

   private MassAbsorptionCoefficient chooseMACTable() {
      if ((Citzaf.getMACAlgorithm() == CITZAF.MassAbsorptionCoefficient.MASS_ABSORPTION_COEFFICIENTS_FROM_DATA_TABLES)
            || (Citzaf.getMACAlgorithm() == CITZAF.MassAbsorptionCoefficient.MASS_ABSORPTION_COEFFICIENTS_FROM_TABLES_AND_USER_ENTRY))
         return MassAbsorptionCoefficient.DTSA_CitZAF;
      else if ((Citzaf.getMACAlgorithm() == CITZAF.MassAbsorptionCoefficient.MASS_ABSORPTION_COEFFICIENTS_FROM_HEINRICH_ICXOM_11_EQN)
            || (Citzaf.getMACAlgorithm() == CITZAF.MassAbsorptionCoefficient.MASS_ABSORPTION_COEFFICIENTS_FROM_HEINRICH_ICXOM_11_AND_USER_ENTRY))
         return MassAbsorptionCoefficient.HeinrichDtsa;
      else
         return MassAbsorptionCoefficient.DTSA_CitZAF;

   }

   private double computeMAC(MassAbsorptionCoefficient macTable, Element elm, Element absorber) {
      try {
         XRayTransition xrt = null;
         String Line = null;
         if (Citzaf.hasLine(elm))
            Line = Citzaf.getLine(elm);
         else
            Line = chooseBestLine(elm);
         if (Line != null) {
            if (Line.startsWith("K"))
               xrt = new XRayTransition(elm, XRayTransition.KA1);
            else if (Line.startsWith("L"))
               xrt = new XRayTransition(elm, XRayTransition.LA1);
            else if (Line.startsWith("M"))
               xrt = new XRayTransition(elm, XRayTransition.MA1);
            if ((xrt != null) && (!Citzaf.hasMAC(absorber, xrt)))
               return MassAbsorptionCoefficient.toCmSqrPerGram(macTable.compute(absorber, xrt));
            else if (xrt != null)
               return Citzaf.getMAC(absorber, xrt);
         }
         return 0.0;
      } catch (final EPQException ex) {
         return 0.0;
      }
   }

   private Set<Element> getMasterElementSet() {
      final Set<Element> masterElementSet = new TreeSet<Element>();
      if (Citzaf.getElementSet().size() > 0)
         masterElementSet.addAll(Citzaf.getElementSet());
      final Set<Element> StandardsSet = new TreeSet<Element>();
      for (final Element elm : masterElementSet)
         if (Citzaf.hasStandard(elm))
            StandardsSet.addAll(Citzaf.getAStandard(elm).getElementSet());
      masterElementSet.addAll(StandardsSet);
      return masterElementSet;
   }

   void jButton_EditModels_actionPerformed(ActionEvent e) {
      pmd = new ParticleModelDialog(this, "Choose a particle model", true);
      pmd.setLocationRelativeTo(this);
      if (Citzaf.getParticleModels().length > 0)
         pmd.setParticleModels(Citzaf.getParticleModels());
      pmd.addWindowClosingListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final ArrayList<String> temp = pmd.MainParticleModelList;
            Citzaf.setParticleModels(temp);
            loadParticlesTabFromCITZAF();
            pmd.dispose();
         }
      });
      commitToCitzaf();
      pmd.setVisible(true);
   }

   void jComboBox_Preselected_actionPerformed(ActionEvent e) {
      if (jComboBox_Preselected.getSelectedIndex() == 0)
         Citzaf.setComposition(new Composition());
      else {
         final String name = (String) jComboBox_Preselected.getSelectedItem();
         Citzaf.setComposition(matFac.getMaterial(name));
      }
      loadDataFromCITZAF();
   }

   void jButton_EditDiameters_actionPerformed(ActionEvent e) {
      pdd = new ParticleDiameterDialog(this, "Enter the particle(s) diameter(s)", true);
      pdd.setLocationRelativeTo(this);
      if (Citzaf.getParticleDiameters().length > 0)
         pdd.setParticleDiameters(Citzaf.getParticleDiameters());
      pdd.addWindowClosingListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final ArrayList<Double> temp = pdd.getDiameters();
            Citzaf.setParticleDiameters(temp);
            loadParticlesTabFromCITZAF();
            pdd.setVisible(false);
         }
      });
      commitToCitzaf();
      pdd.setVisible(true);
   }

   protected void fireWindowClosingEvent(ActionEvent e) {
      commitToCitzaf();
      if (WindowClosingListeners != null)
         for (final ActionListener al : WindowClosingListeners)
            al.actionPerformed(e);
   }

   protected void fireBeginAnalysisEvent(ActionEvent e) {
      if (BeginAnalysisListeners != null)
         for (final ActionListener al : BeginAnalysisListeners)
            al.actionPerformed(e);
   }

   /**
    * removeWindowClosingListener - removes the window closing listener
    * 
    * @param l
    *           ActionListener
    */
   public synchronized void removeWindowClosingListener(ActionListener l) {
      if ((WindowClosingListeners != null) && WindowClosingListeners.contains(l)) {
         final Vector<ActionListener> v = new Vector<ActionListener>(WindowClosingListeners);
         v.removeElement(l);
         WindowClosingListeners = v;
      }
   }

   /**
    * removeWindowClosingListener - removes the window closing listener
    * 
    * @param l
    *           ActionListener
    */
   public synchronized void removeBeginAnalysisButtonListener(ActionListener l) {
      if ((BeginAnalysisListeners != null) && BeginAnalysisListeners.contains(l)) {
         final Vector<ActionListener> v = new Vector<ActionListener>(BeginAnalysisListeners);
         v.removeElement(l);
         BeginAnalysisListeners = v;
      }
   }

   /**
    * addWindowClosingListener - Adds a window closing listener. When the dialog
    * closes, an event will be triggered so that all listening components can
    * respond appropriately.
    * 
    * @param l
    *           ActionListener
    */
   public synchronized void addWindowClosingListener(ActionListener l) {
      final Vector<ActionListener> v = WindowClosingListeners == null
            ? new Vector<ActionListener>(2)
            : new Vector<ActionListener>(WindowClosingListeners);
      if (!v.contains(l)) {
         v.addElement(l);
         WindowClosingListeners = v;
      }
   }

   /**
    * addBeginAnalysisButtonListener - Adds a listener for the Begin Analysis
    * button. If clicked, the button will trigger this event and notify the
    * listening components so they can respond appropriately.
    * 
    * @param l
    *           ActionListener
    */
   public synchronized void addBeginAnalysisButtonListener(ActionListener l) {
      final Vector<ActionListener> v = BeginAnalysisListeners == null
            ? new Vector<ActionListener>(2)
            : new Vector<ActionListener>(BeginAnalysisListeners);
      if (!v.contains(l)) {
         v.addElement(l);
         BeginAnalysisListeners = v;
      }
   }

   void this_windowClosing(WindowEvent e) {
      fireWindowClosingEvent(new ActionEvent(e.getSource(), e.getID(), "Window Closing!"));
   }

   /**
    * getCITZAF - returns the CITZAF class created by the dialog
    * 
    * @return CITZAF
    */
   public CITZAF getCITZAF() {
      return Citzaf;
   }

   /**
    * setCITZAF - updates the CITZAFDialog to a new CITZAF. All changes to the
    * appearance and information displayed on the dialog will be made before the
    * method finishes.
    * 
    * @param citzaf
    *           CITZAF
    */
   public void setCITZAF(CITZAF citzaf) {
      Citzaf = citzaf;
      loadDataFromCITZAF();
   }

   void jButton_KRatio_actionPerformed(ActionEvent e) {
      KRatioDialog = new KRatioCreator(this, "Enter the K-ratios of the sample", true);
      KRatioDialog.setLocationRelativeTo(this);
      if (Citzaf.getKRatios().size() > 0)
         KRatioDialog.setKRatios(Citzaf.getKRatios(), Citzaf.getElementToLineMap());
      KRatioDialog.addWindowClosingListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            Citzaf.setKRatios(KRatioDialog.getKRatioSet());
            loadDataFromCITZAF();
            KRatioDialog.setVisible(false);
         }
      });
      commitToCitzaf();
      KRatioDialog.setVisible(true);

   }

   void jRadioButton_Composition_actionPerformed(ActionEvent e) {
      if (!Citzaf.getUsingKRatios()) {
         final int value = JOptionPane.showConfirmDialog(this.getGlassPane(),
               "Are you sure you want to switch to calculating composition?\nIf you do, you will lose all information about your standards.",
               "Continue anyway?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
         if (value == JOptionPane.YES_OPTION) {
            Citzaf.clearStandards();

            Citzaf.setUsingKRatios(true);
            loadDataFromCITZAF();
            toggleMaterials(true);
         } else {
            jRadioButton_KRatios.setSelected(true);
            jRadioButton_Composition.setSelected(false);
         }
      }
   }

   private void toggleMaterials(boolean enabled) {
      jLabel_KRatio.setEnabled(enabled);
      jLabel_KRatioDesc.setEnabled(enabled);
      jButton_KRatio.setEnabled(enabled);

      jLabel_SampleQS.setEnabled(!enabled);
      jLabel_SampleDescriptionQS.setEnabled(!enabled);
      jButton_NewMaterialQS.setEnabled(!enabled);
      jLabel_Preselected.setEnabled(!enabled);
      jComboBox_Preselected.setEnabled(!enabled);
      jLabel_SelectALine.setEnabled(!enabled);
      jScrollPane_Lines.setEnabled(!enabled);
      final Component[] temp = jPanel_LineInside.getComponents();
      for (final Component element : temp)
         if (element instanceof JPanel) {
            final JPanel panel = (JPanel) element;
            final Component[] panelArray = panel.getComponents();
            for (final Component element2 : panelArray)
               element2.setEnabled(!enabled);
         }
      jLabel_StageCoordinates.setEnabled(enabled);
      jRadioButton_StageCoordinatesYes.setEnabled(enabled);
      jRadioButton_StageCoordinatesNo.setEnabled(enabled);
      jLabel_XStageCoordinate.setEnabled(enabled);
      jLabel_YStageCoordinate.setEnabled(enabled);

      if (enabled) {
         jTextField_XStageCoordinate.setEnabled(jRadioButton_StageCoordinatesYes.isSelected());
         jTextField_YStageCoordinate.setEnabled(jRadioButton_StageCoordinatesYes.isSelected());
      }
   }

   void jRadioButton_KRatios_actionPerformed(ActionEvent e) {
      if (Citzaf.getUsingKRatios()) {
         final int value = JOptionPane.showConfirmDialog(this.getGlassPane(),
               "Are you sure you want to switch to calculating k-ratios?\nIf you do, you will lose all information about your standards.",
               "Continue anyway?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
         if (value == JOptionPane.YES_OPTION) {
            Citzaf.clearStandards();

            Citzaf.setUsingKRatios(false);
            loadDataFromCITZAF();
            toggleMaterials(false);
         } else {
            jRadioButton_KRatios.setSelected(false);
            jRadioButton_Composition.setSelected(true);
         }
      }
   }
}
