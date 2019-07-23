/**
 * <p>
 * Title: gov.nist.microanalysis.Trixy.SpectrumPropertyPanel.java
 * </p>
 * <p>
 * Description:
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
package gov.nist.microanalysis.EPQTools;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gov.nist.microanalysis.EPQDatabase.Session;
import gov.nist.microanalysis.EPQLibrary.ConductiveCoating;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.Detector.DetectorCalibration;
import gov.nist.microanalysis.EPQLibrary.Detector.DetectorProperties;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSCalibration;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.EPQLibrary.Detector.ElectronProbe;
import gov.nist.microanalysis.EPQLibrary.Detector.IXRayDetector;

/**
 * A panel for displaying and editing common properties of spectra.
 */
public class SpectrumPropertyPanel
   extends
   JPanel {
   static private final long serialVersionUID = 0x42;

   public static final String DESCRIPTIVE_PANEL = "Description";
   public static final String CONDITIONS_PANEL = "Conditions";

   public static final String NONE = "No change";
   private final Session mSession;

   private final JTabbedPane mTabs = new JTabbedPane(SwingConstants.TOP);
   private JPanel mConditionsPanel;
   private JTextField mFaradayEnd;
   private JTextField mFaradayBegin;
   private JTextField mLiveTime;
   private JTextField mBeamEnergy;
   private JTextField mWorkingDistance;

   private JComboBoxCoating mCoatingMaterial;
   private JTextFieldDouble mCoatingThickness;

   private JPanel mDescriptivePanel;
   private JTextField mSpecimenName;
   private JTextField mSpecimenDesc;
   private JTextField mProjectId;
   private JTextField mSampleId;
   private JTextField mInstrumentOperator;
   private JComboBox<Object> mInstrument;
   private JComboBox<Object> mDetector;
   private JComboBox<Object> mCalibration;
   private JTextField mAcquisitionTime;
   private JTextField mSpectrumComment;
   private JTextField mSpectrumIndex;
   private JTextField mClientName;

   private SpectrumProperties mSpectrumProperties;
   private Date mEarliestDate;
   private TreeSet<SpectrumProperties.PropertyId> mRequiredProperties;

   public SpectrumPropertyPanel(Session ses) {
      super();
      mSession = ses;
      assert mSession != null;
      try {
         initialize();
      }
      catch(final Exception ex) {
         ex.printStackTrace(System.err);
      }
   }

   private static String repeat(String str, int n) {
      final StringBuilder sb = new StringBuilder((str.length() + 3) * n);
      sb.append(str);
      for(int i = 0; i < n; ++i) {
         sb.append(", ");
         sb.append(str);
      }
      return sb.toString();
   }

   /**
    * Determines which pane is displayed as open.
    * 
    * @param pane - One of CONDITIONS_PANEL or DESCRIPTIVE_PANEL
    */
   public void showPane(String pane) {
      if(pane.equals(CONDITIONS_PANEL))
         mTabs.setSelectedIndex(0);
      else
         mTabs.setSelectedIndex(1);

   }

   private JPanel initializeDescriptivePanel() {
      final FormLayout lo = new FormLayout("right:pref, 3dlu, 180dlu", repeat("pref, 3dlu", 12) + ", pref");
      final CellConstraints cc = new CellConstraints();
      final PanelBuilder pb = new PanelBuilder(lo);

      pb.addSeparator("Sample details", cc.xyw(1, 1, 3));
      pb.addLabel("Sample name", cc.xy(1, 3)); // SpecimenName
      mSpecimenName = new JTextField();
      pb.add(mSpecimenName, cc.xy(3, 3));
      pb.addLabel("Sample description", cc.xy(1, 5)); // SpecimenDesc
      mSpecimenDesc = new JTextField();
      pb.add(mSpecimenDesc, cc.xy(3, 5));
      pb.addLabel("Sample number", cc.xy(1, 7)); // SampleId
      mSampleId = new JTextField();
      pb.add(mSampleId, cc.xy(3, 7));

      pb.addSeparator("Spectrum details", cc.xyw(1, 9, 3));
      mProjectId = new JTextField();
      pb.addLabel("Spectrum comment", cc.xy(1, 11));
      mSpectrumComment = new JTextField();
      pb.add(mSpectrumComment, cc.xy(3, 11));
      pb.addLabel("Spectrum number", cc.xy(1, 13)); // SpecimenId
      mSpectrumIndex = new JTextField();
      mSpectrumIndex.setEditable(false);
      pb.add(mSpectrumIndex, cc.xy(3, 13));

      pb.addSeparator("Project details", cc.xyw(1, 15, 3));
      pb.addLabel("Project name", cc.xy(1, 17)); // ProjectNumber
      mProjectId = new JTextField();
      pb.add(mProjectId, cc.xy(3, 17));
      pb.addLabel("Client's name", cc.xy(1, 19)); // ClientName
      mClientName = new JTextField();
      pb.add(mClientName, cc.xy(3, 19));
      pb.addSeparator("Acquisition details", cc.xyw(1, 21, 3));
      pb.addLabel("Collected by", cc.xy(1, 23)); // Analyst
      mInstrumentOperator = new JTextField();
      pb.add(mInstrumentOperator, cc.xy(3, 23));
      pb.addLabel("Acqusition Time", cc.xy(1, 25));
      mAcquisitionTime = new JTextField();
      mAcquisitionTime.setEditable(false);
      pb.add(mAcquisitionTime, cc.xy(3, 25));
      final JPanel res = pb.getPanel();
      res.setName(DESCRIPTIVE_PANEL);
      return res;
   }

   private JPanel initializeConditionsPanel() {
      final CellConstraints cc = new CellConstraints();
      final FormLayout lo = new FormLayout("right:pref, 3dlu, 50dlu, 3dlu, left:110dlu", repeat("pref, 3dlu", 13) + ", pref");
      final PanelBuilder pb = new PanelBuilder(lo);
      pb.addSeparator("Instrument", cc.xyw(1, 1, 5));
      pb.addLabel("Instrument", cc.xy(1, 3));
      {
         mInstrument = new JComboBox<Object>();
         final DefaultComboBoxModel<Object> dcbm = new DefaultComboBoxModel<Object>();
         dcbm.addElement(NONE);
         for(final ElectronProbe ep : mSession.getCurrentProbes())
            dcbm.addElement(ep);
         dcbm.setSelectedItem(NONE);
         mInstrument.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               updateDetector();
            }
         });
         mInstrument.setModel(dcbm);
      }
      pb.add(mInstrument, cc.xyw(3, 3, 3));
      pb.addLabel("Detector", cc.xy(1, 5));
      {
         mDetector = new JComboBox<Object>();
         mDetector.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
               updateCalibration();
            }
         });

         final DefaultComboBoxModel<Object> dcmb = new DefaultComboBoxModel<Object>();
         dcmb.addElement(NONE);
         mDetector.setModel(dcmb);
      }
      pb.add(mDetector, cc.xyw(3, 5, 3));

      {
         mCalibration = new JComboBox<Object>();
         final DefaultComboBoxModel<Object> dcmb = new DefaultComboBoxModel<Object>();
         dcmb.addElement(NONE);
         mCalibration.setModel(dcmb);
      }
      pb.addLabel("Calibration", cc.xy(1, 7));
      pb.add(mCalibration, cc.xyw(3, 7, 3));

      final int ip = 8;
      pb.addSeparator("Instrument parameters", cc.xyw(1, ip + 1, 5));
      pb.addLabel("Beam energy", cc.xy(1, ip + 3));
      mBeamEnergy = new JTextField();
      pb.add(mBeamEnergy, cc.xy(3, ip + 3));
      pb.addLabel("kV", cc.xy(5, ip + 3));
      pb.addLabel("Probe current (before)", cc.xy(1, ip + 5));
      mFaradayBegin = new JTextField();
      pb.add(mFaradayBegin, cc.xy(3, ip + 5));
      pb.addLabel("nA", cc.xy(5, ip + 5));
      pb.addLabel("Probe current (after)", cc.xy(1, ip + 7));
      mFaradayEnd = new JTextField();
      pb.add(mFaradayEnd, cc.xy(3, ip + 7));
      pb.addLabel("nA", cc.xy(5, ip + 7));
      pb.addLabel("Live time", cc.xy(1, ip + 9));
      mLiveTime = new JTextField();
      pb.add(mLiveTime, cc.xy(3, ip + 9));
      pb.addLabel("seconds", cc.xy(5, ip + 9));

      pb.addLabel("Working distance", cc.xy(1, ip + 11));
      mWorkingDistance = new JTextField();
      pb.add(mWorkingDistance, cc.xy(3, ip + 11));
      pb.addLabel("mm", cc.xy(5, ip + 11));

      pb.addSeparator("Conductive Coating", cc.xyw(1, ip + 13, 5));

      pb.addLabel("Material", cc.xy(1, ip + 15));
      mCoatingMaterial = new JComboBoxCoating(this, mSession);
      pb.add(mCoatingMaterial, cc.xy(3, ip + 15));

      pb.addLabel("Thickness", cc.xy(1, ip + 17));
      mCoatingThickness = new JTextFieldDouble(10.0, 0.0, 1000.0, "#,##0.0", "None");
      pb.add(mCoatingThickness, cc.xy(3, ip + 17));
      pb.addLabel("nm", cc.xy(5, ip + 17));

      final JPanel res = pb.getPanel();
      res.setName(CONDITIONS_PANEL);
      return res;
   }

   private void initialize() {
      mDescriptivePanel = initializeDescriptivePanel();
      mDescriptivePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      mConditionsPanel = initializeConditionsPanel();
      mConditionsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      mTabs.add(mConditionsPanel, 0);
      mTabs.add(mDescriptivePanel, 1);
      add(mTabs);
   }

   /**
    * Add an additional set of SpectrumProperties. The SpectrumPropertyPanel
    * will be initialized with the merged set of those properties common to all
    * sets of SpectrumProperties added through this method. The calibration is
    * guessed using the earliest timestamp in the AcquisitionTime property.
    * 
    * @param sp
    */
   public void addSpectrumProperties(SpectrumProperties sp) {
      if(mSpectrumProperties == null) {
         mSpectrumProperties = sp.clone();
         mEarliestDate = sp.getTimestampWithDefault(SpectrumProperties.AcquisitionTime, new Date(System.currentTimeMillis()));
      } else {
         mSpectrumProperties = SpectrumProperties.merge(mSpectrumProperties, sp);
         final Date dt = sp.getTimestampWithDefault(SpectrumProperties.AcquisitionTime, new Date(System.currentTimeMillis()));
         if(dt.before(mEarliestDate))
            mEarliestDate = dt;
      }
      updateSpectrumProperties();
   }

   public void clearSpectrumProperties() {
      mSpectrumProperties = null;
      mEarliestDate = null;
   }

   public void updateSpectrumProperties() {
      final SpectrumProperties sp = mSpectrumProperties != null ? mSpectrumProperties : new SpectrumProperties();
      mSpecimenName.setText(sp.getTextWithDefault(SpectrumProperties.SpecimenName, ""));
      mSpecimenName.selectAll();
      mSpecimenDesc.setText(sp.getTextWithDefault(SpectrumProperties.SpecimenDesc, ""));
      mSpecimenDesc.selectAll();
      mProjectId.setText(sp.getTextWithDefault(SpectrumProperties.ProjectName, ""));
      mProjectId.selectAll();
      mSampleId.setText(sp.getTextWithDefault(SpectrumProperties.SampleId, ""));
      mSampleId.selectAll();
      mInstrumentOperator.setText(sp.getTextWithDefault(SpectrumProperties.InstrumentOperator, ""));
      mInstrumentOperator.selectAll();
      mAcquisitionTime.setText(sp.getTextWithDefault(SpectrumProperties.AcquisitionTime, ""));
      mAcquisitionTime.selectAll();
      mSpectrumComment.setText(sp.getTextWithDefault(SpectrumProperties.SpectrumComment, ""));
      mSpectrumComment.selectAll();
      mSpectrumIndex.setText(sp.getTextWithDefault(SpectrumProperties.SpectrumIndex, ""));
      mClientName.setText(sp.getTextWithDefault(SpectrumProperties.ClientName, ""));
      mClientName.selectAll();
      mFaradayEnd.setText(sp.getTextWithDefault_NoUnit(SpectrumProperties.FaradayEnd, ""));
      mFaradayEnd.selectAll();
      mFaradayBegin.setText(sp.getTextWithDefault_NoUnit(SpectrumProperties.FaradayBegin, ""));
      mFaradayBegin.selectAll();
      mLiveTime.setText(sp.getTextWithDefault_NoUnit(SpectrumProperties.LiveTime, ""));
      mLiveTime.selectAll();
      mBeamEnergy.setText(sp.getTextWithDefault_NoUnit(SpectrumProperties.BeamEnergy, ""));
      mBeamEnergy.selectAll();
      mWorkingDistance.setText(sp.getTextWithDefault_NoUnit(SpectrumProperties.WorkingDistance, ""));
      mWorkingDistance.selectAll();
      final ConductiveCoating cc = (ConductiveCoating) sp.getObjectWithDefault(SpectrumProperties.ConductiveCoating, null);
      if(cc != null) {
         mCoatingMaterial.setSelectedItem(cc.getMaterial());
         mCoatingThickness.setValue(cc.getThickness() * 1.0e9);
      } else {
         mCoatingMaterial.setSelectedItem(Material.Null);
         mCoatingThickness.setValue(Double.NaN);
      }

      mSpecimenName.requestFocusInWindow();
      if(sp.isDefined(SpectrumProperties.Detector)) {
         final IXRayDetector det = sp.getDetector();
         if(det != null)
            setDetector(det);
      }
      updateControls();
   }

   public void setDetector(IXRayDetector det) {
      if(det != null) {
         mInstrument.setSelectedItem(det.getDetectorProperties().getOwner());
         updateDetector();
         mDetector.setSelectedItem(det.getDetectorProperties());
         mCalibration.setSelectedItem(det.getCalibration());
      }
   }

   private void addIfDifferent(JTextField jtf, SpectrumProperties.PropertyId pid, SpectrumProperties sp) {
      final String str = jtf.getText();
      if(str.length() > 0)
         if(!mSpectrumProperties.getTextWithDefault(pid, "").equals(str))
            sp.setTextProperty(pid, str);
   }

   private void addIfDifferentNumber(JTextField jtf, SpectrumProperties.PropertyId pid, SpectrumProperties sp) {
      final String str = jtf.getText();
      if(str.length() > 0) {
         double n;
         try {
            final NumberFormat nf = NumberFormat.getInstance();
            n = nf.parse(str).doubleValue();
            if(mSpectrumProperties.getNumericWithDefault(pid, Double.NaN) != n)
               sp.setNumericProperty(pid, n);
            updateControl(jtf, pid);
         }
         catch(final ParseException e) {
            jtf.setBackground(Color.pink);
         }
      }
   }

   private void addIfDifferent(JComboBoxCoating cbc, JTextFieldDouble thick, SpectrumProperties.PropertyId pid, SpectrumProperties sp) {
      Material material = (Material) cbc.getSelectedItem();
      double thickness = thick.getValue();
      ConductiveCoating cc = (ConductiveCoating) mSpectrumProperties.getObjectWithDefault(pid, null);
      ConductiveCoating newCC = null;
      if((material != null) && (!material.equals(Material.Null)) && (thickness > 0.0))
         newCC = new ConductiveCoating(material, 1.0e-9 * thickness);
      if(cc == null) {
         if(newCC != null)
            sp.setObjectProperty(SpectrumProperties.ConductiveCoating, newCC);
      } else {
         if(newCC == null)
            sp.clear(SpectrumProperties.ConductiveCoating);
         else
            sp.setObjectProperty(SpectrumProperties.ConductiveCoating, newCC);
      }
   }

   public SpectrumProperties getSpectrumProperties() {
      final SpectrumProperties sp = new SpectrumProperties();
      addIfDifferent(mSpecimenName, SpectrumProperties.SpecimenName, sp);
      addIfDifferent(mSpecimenDesc, SpectrumProperties.SpecimenDesc, sp);
      addIfDifferent(mProjectId, SpectrumProperties.ProjectName, sp);
      addIfDifferent(mSampleId, SpectrumProperties.SampleId, sp);
      addIfDifferent(mInstrumentOperator, SpectrumProperties.InstrumentOperator, sp);
      addIfDifferent(mSpectrumComment, SpectrumProperties.SpectrumComment, sp);
      addIfDifferent(mClientName, SpectrumProperties.ClientName, sp);
      if((mDetector.getSelectedItem() instanceof DetectorProperties)
            && (mCalibration.getSelectedItem() instanceof EDSCalibration))
         sp.setDetector(EDSDetector.createDetector((DetectorProperties) mDetector.getSelectedItem(), (EDSCalibration) mCalibration.getSelectedItem()));
      addIfDifferentNumber(mFaradayEnd, SpectrumProperties.FaradayEnd, sp);
      addIfDifferentNumber(mFaradayBegin, SpectrumProperties.FaradayBegin, sp);
      addIfDifferentNumber(mLiveTime, SpectrumProperties.LiveTime, sp);
      addIfDifferentNumber(mBeamEnergy, SpectrumProperties.BeamEnergy, sp);
      addIfDifferentNumber(mWorkingDistance, SpectrumProperties.WorkingDistance, sp);
      addIfDifferent(mCoatingMaterial, mCoatingThickness, SpectrumProperties.ConductiveCoating, sp);
      return sp;
   }

   /**
    * Updates the list of detectors based on the contents of the instrument
    * combo box. It retains the previous detector if it is part of this
    * instrument
    */
   private void updateDetector() {
      final Object instSel = mInstrument.getSelectedItem();
      final DefaultComboBoxModel<Object> dcmb = new DefaultComboBoxModel<Object>();
      final Object detSel = mDetector.getSelectedItem();
      if(instSel instanceof ElectronProbe) {
         final ElectronProbe ep = (ElectronProbe) instSel;
         final Set<DetectorProperties> dps = mSession.getDetectors(ep);
         for(final DetectorProperties dp : dps)
            dcmb.addElement(dp);
         dcmb.setSelectedItem((!dps.isEmpty()) ? (dps.contains(detSel) ? detSel : dps.iterator().next()) : null);
      } else {
         dcmb.addElement(NONE);
         dcmb.setSelectedItem(NONE);
      }
      mDetector.setModel(dcmb);
      updateCalibration();
   }

   private void updateCalibration() {
      final Object sel = mDetector.getSelectedItem();
      Object calSel = NONE;
      final DefaultComboBoxModel<Object> dcmb = new DefaultComboBoxModel<Object>();
      if(sel instanceof DetectorProperties) {
         calSel = mCalibration.getSelectedItem();
         final DetectorProperties dp = (DetectorProperties) sel;
         final List<DetectorCalibration> dcs = mSession.getCalibrations(dp);
         for(final DetectorCalibration dc : dcs)
            dcmb.addElement(dc);
         if(!dcs.contains(calSel))
            calSel = mSession.getSuitableCalibration(dp, mEarliestDate != null ? mEarliestDate
                  : new Date(System.currentTimeMillis()));
         assert dcs.contains(calSel);
      } else
         dcmb.addElement(NONE);
      dcmb.setSelectedItem(calSel);
      mCalibration.setModel(dcmb);
   }

   public void disableDetectorProperties() {
      mInstrument.setEnabled(false);
      mDetector.setEnabled(false);
      mCalibration.setEnabled(false);
   }

   private void updateControl(JTextField jtf, SpectrumProperties.PropertyId pid) {
      final boolean b = (mRequiredProperties != null) && mRequiredProperties.contains(pid);
      if(b) {
         jtf.setBackground(Color.cyan);
         jtf.setToolTipText("Required property...");
      } else {
         jtf.setBackground(SystemColor.text);
         jtf.setToolTipText(null);
      }
   }

   public void setRequiredProperties(Collection<SpectrumProperties.PropertyId> sp) {
      mRequiredProperties = new TreeSet<SpectrumProperties.PropertyId>(sp);
      updateControls();
   }

   private void updateControls() {
      updateControl(mSpecimenName, SpectrumProperties.SpecimenName);
      updateControl(mSpecimenDesc, SpectrumProperties.SpecimenDesc);
      updateControl(mProjectId, SpectrumProperties.ProjectName);
      updateControl(mSampleId, SpectrumProperties.SampleId);
      updateControl(mInstrumentOperator, SpectrumProperties.InstrumentOperator);
      updateControl(mSpectrumComment, SpectrumProperties.SpectrumComment);
      updateControl(mClientName, SpectrumProperties.ClientName);
      updateControl(mFaradayEnd, SpectrumProperties.FaradayEnd);
      updateControl(mFaradayBegin, SpectrumProperties.FaradayBegin);
      updateControl(mLiveTime, SpectrumProperties.LiveTime);
      updateControl(mBeamEnergy, SpectrumProperties.BeamEnergy);
      updateControl(mWorkingDistance, SpectrumProperties.WorkingDistance);
      doLayout();
   }

   private boolean definedRequiredProperties(SpectrumProperties sp) {
      boolean res = true;
      if(mRequiredProperties != null) {
         final SpectrumProperties dup = new SpectrumProperties(mSpectrumProperties);
         dup.addAll(sp);
         for(final SpectrumProperties.PropertyId pid : mRequiredProperties)
            if((pid == SpectrumProperties.FaradayBegin) || (pid == SpectrumProperties.FaradayEnd))
               res &= (dup.isDefined(SpectrumProperties.FaradayBegin) || dup.isDefined(SpectrumProperties.FaradayEnd));
            else
               res &= dup.isDefined(pid);
      }
      return res;
   }

   public static class PropertyDialog
      extends
      JDialog {
      private static final String DIALOG_TITLE = "Edit the spectrum properties";
      private static final long serialVersionUID = -6959469022738667049L;
      private SpectrumPropertyPanel mPanel;
      private boolean mOk;
      private final Session mSession;

      public PropertyDialog(Dialog dlg, Session ses) {
         super(dlg, DIALOG_TITLE, true);
         mSession = ses;
         try {
            initialize();
         }
         catch(final RuntimeException ex) {
         }
      }

      public PropertyDialog(Frame frame, Session ses) {
         super(frame, DIALOG_TITLE, true);
         mSession = ses;
         try {
            initialize();
         }
         catch(final RuntimeException ex) {
         }
      }

      public void addSpectrumProperties(SpectrumProperties sp) {
         mPanel.addSpectrumProperties(sp);
      }

      public boolean isOk() {
         return mOk;
      }

      public SpectrumProperties getSpectrumProperties() {
         return mOk ? mPanel.getSpectrumProperties() : new SpectrumProperties();
      }

      public void setDetector(IXRayDetector det) {
         mPanel.setDetector(det);
      }

      public void showPane(String pane) {
         mPanel.showPane(pane);
      }

      public void setRequiredProperties(Collection<SpectrumProperties.PropertyId> sp) {
         mPanel.setRequiredProperties(sp);
      }

      private void initialize() {
         final JPanel pnl = new JPanel(new FormLayout("pref", "pref, 3dlu, pref"));
         final CellConstraints cc = new CellConstraints();
         mPanel = new SpectrumPropertyPanel(mSession);
         pnl.add(mPanel, cc.xy(1, 1));
         mOk = false;
         final JButton okBtn = new JButton("Ok");
         okBtn.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = 3365674610927227720L;

            @Override
            public void actionPerformed(ActionEvent e) {
               if(mPanel.definedRequiredProperties(mPanel.getSpectrumProperties())) {
                  mOk = true;
                  setVisible(false);
               } else {
                  final StringBuffer required = new StringBuffer();
                  boolean firstPc = true;
                  for(final SpectrumProperties.PropertyId pid : mPanel.mRequiredProperties) {
                     String app = null;
                     if((pid == SpectrumProperties.FaradayBegin) || (pid == SpectrumProperties.FaradayEnd)) {
                        if(firstPc) {
                           app = "Probe Current";
                           firstPc = false;
                        }
                     } else
                        app = pid.toString();
                     if(app != null) {
                        if(required.length() > 0)
                           required.append(", ");
                        required.append(app);
                     }
                  }
                  final int res = JOptionPane.showConfirmDialog(SpectrumPropertyPanel.PropertyDialog.this, "You must specified a value for each of\n"
                        + required.toString(), "Spectrum properties", JOptionPane.OK_CANCEL_OPTION);
                  if(res == JOptionPane.CANCEL_OPTION) {
                     mOk = false;
                     setVisible(false);
                  }
               }
            }
         });
         getRootPane().setDefaultButton(okBtn);
         final JButton cancelBtn = new JButton("Cancel");
         cancelBtn.addActionListener(new AbstractAction() {
            private static final long serialVersionUID = 3365674610927227720L;

            @Override
            public void actionPerformed(ActionEvent e) {
               mOk = false;
               setVisible(false);
            }
         });
         final ButtonBarBuilder bbb = new ButtonBarBuilder();
         bbb.addGlue();
         bbb.addButton(okBtn, cancelBtn);
         final JPanel btnPanel = bbb.build();
         btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));
         pnl.add(btnPanel, cc.xy(1, 3));
         setContentPane(pnl);
         addCancelByEscapeKey();
         pack();
      }

      private void addCancelByEscapeKey() {
         final String CANCEL_ACTION_KEY = "CANCEL_ACTION_KEY";
         final int noModifiers = 0;
         final KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, noModifiers, false);
         final InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
         inputMap.put(escapeKey, CANCEL_ACTION_KEY);
         final AbstractAction cancelAction = new AbstractAction() {
            private static final long serialVersionUID = 585668420885106696L;

            @Override
            public void actionPerformed(ActionEvent e) {
               mOk = false;
               setVisible(false);
            }
         };
         getRootPane().getActionMap().put(CANCEL_ACTION_KEY, cancelAction);
      }

      public void disableDetectorProperties() {
         mPanel.disableDetectorProperties();
      }

   };
}
