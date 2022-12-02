package gov.nist.microanalysis.EPQTools;

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelListener;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQTools.KLMActionEvent.KLMAction;

/**
 * <p>
 * Allows the user to select K, L &amp; M lines. Also allows the user to view
 * Escapes, Satellites, Edges and/or the sum of the peaks.
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

public class KLMPanel extends JPanel {

   private static final long serialVersionUID = 4697281684102338236L;

   // Internal class for record keeping
   private class KLMProperties {
      private boolean mKLineSelected = false;
      private boolean mLLineSelected = false;
      private boolean mMLineSelected = false;
      private boolean mAllLinesSelected = false;

      private boolean mVisibleAllPeaksSelected = true;
      private boolean mVisibleEscapesSelected = false;
      private boolean mVisibleSatellitesSelected = false;
      private boolean mVisibleEdgesSelected = false;
      private boolean mVisibleSumPeaksSelected = false;

      private final Element mElement;

      public KLMProperties(Element elm) {
         mElement = elm;
      }

      public SortedSet<KLMLine> getVisibleLines(int family) {
         final SortedSet<KLMLine> res = new TreeSet<KLMLine>();
         final int mode = (mVisibleAllPeaksSelected ? ALL_LINES : MAJOR_LINES);
         res.addAll(getTransitionLines(family, mode, mElement));
         if (mVisibleEdgesSelected)
            res.addAll(getEdges(family, mode, mElement));
         return res;
      }

      public SortedSet<KLMLine> getVisibleLines() {
         final SortedSet<KLMLine> res = new TreeSet<KLMLine>();
         if (mKLineSelected)
            res.addAll(getVisibleLines(AtomicShell.KFamily));
         if (mLLineSelected)
            res.addAll(getVisibleLines(AtomicShell.LFamily));
         if (mMLineSelected)
            res.addAll(getVisibleLines(AtomicShell.MFamily));
         return res;
      }

      /**
       * Returns a set of KLMLine objects representing the difference between
       * "all lines" and "major lines"
       * 
       * @return SortedSet&lt;KLMLine&gt;
       */
      public SortedSet<KLMLine> getAllDelta() {
         final SortedSet<KLMLine> res = new TreeSet<KLMLine>();
         if (mKLineSelected) {
            res.addAll(getTransitionLines(AtomicShell.KFamily, ALL_LINES, mElement));
            for (final KLMLine ml : getTransitionLines(AtomicShell.KFamily, MAJOR_LINES, mElement))
               res.remove(ml);
         }
         if (mLLineSelected) {
            res.addAll(getTransitionLines(AtomicShell.LFamily, ALL_LINES, mElement));
            for (final KLMLine ml : getTransitionLines(AtomicShell.LFamily, MAJOR_LINES, mElement))
               res.remove(ml);
         }
         if (mMLineSelected) {
            res.addAll(getTransitionLines(AtomicShell.MFamily, ALL_LINES, mElement));
            for (final KLMLine ml : getTransitionLines(AtomicShell.MFamily, MAJOR_LINES, mElement))
               res.remove(ml);
         }
         return res;
      }

      public SortedSet<KLMLine> getEscapes(Element elm) {
         return new TreeSet<KLMLine>(KLMLine.EscapePeak.suggestEscapePeak(elm));
      }

      public SortedSet<KLMLine> getTemporaryLines() {
         final SortedSet<KLMLine> ss = new TreeSet<KLMLine>();
         final int mode = mTemporaryAllPeaksSelected ? ALL_LINES : MAJOR_LINES;
         ss.addAll(getTransitionLines(AtomicShell.KFamily, mode, mElement));
         ss.addAll(getTransitionLines(AtomicShell.LFamily, mode, mElement));
         ss.addAll(getTransitionLines(AtomicShell.MFamily, mode, mElement));
         if (mTemporaryEdgesSelected) {
            ss.addAll(getEdges(AtomicShell.KFamily, mode, mElement));
            ss.addAll(getEdges(AtomicShell.LFamily, mode, mElement));
            ss.addAll(getEdges(AtomicShell.MFamily, mode, mElement));
         }
         if (mTemporaryEscapesSelected)
            ss.addAll(getEscapes(mElement));
         return ss;
      }
   }

   // Define all or major
   private static final double MIN_WEIGHT_ALL = 0.00001;
   private static final double MIN_WEIGHT_MAJOR = 0.1;
   // All or major enumeration
   private static final int ALL_LINES = 1;
   private static final int MAJOR_LINES = 2;
   // The least tightly bound edge
   private static final int[] MajorEdges = {AtomicShell.K, AtomicShell.LIII, AtomicShell.MV};

   private int mCurrentZ = Element.elmH;
   private KLMProperties[] mElementsArray;
   private final TreeSet<KLMLine> mVisibleLines = new TreeSet<KLMLine>();

   private transient Vector<ActionListener> mVisibleLinesActionListeners = new Vector<ActionListener>();
   private transient Vector<ActionListener> mTemporaryLinesActionListeners = new Vector<ActionListener>();

   public static final String ScrollBarText = "Element scroll bar";
   public static final String KLineCheckBox = "K Lines";
   public static final String LLineCheckBox = "L Lines";
   public static final String MLineCheckBox = "M Lines";
   public static final String AllLinesCheckBox = "All Lines";
   public static final String SatellitesMenuItem = "Satellite Lines";
   public static final String EscapesMenuItem = "Escape Peaks";
   public static final String EdgesMenuItem = "Ionization Edges";
   public static final String SumPeaksMenuItem = "Sum Peaks";
   public static final String MajorPeaksMenuItem = "Major Lines";
   public static final String AllPeaksMenuItem = "All Lines";
   public static final String ConfigureButton = "Configure";
   public static final String ClearAllButton = "Clear All";

   private boolean mTemporaryAllPeaksSelected = true;
   private boolean mTemporaryMajorPeaksSelected = false;
   private boolean mTemporaryEscapesSelected = true;
   private final boolean mTemporarySatellitesSelected = false;
   private boolean mTemporaryEdgesSelected = false;

   private static final int BORDER_SIZE = 1;

   private final JPanel jPanel_Element = new JPanel();
   private final JScrollBar jScrollBar_Element = new JScrollBar();
   private final JTextField jTextField_Element = new JTextField();
   private final JLabel jLabel_Element = new JLabel();
   private final JCheckBox jCheckBox_KLine = new JCheckBox();
   private final JCheckBox jCheckBox_LLine = new JCheckBox();
   private final JCheckBox jCheckBox_MLine = new JCheckBox();
   private final JCheckBox jCheckBox_AllLines = new JCheckBox();
   private final JPopupMenu jPopupMenu_TempLines = new JPopupMenu();
   private final JCheckBoxMenuItem jCheckBoxMenuItem_Satellites = new JCheckBoxMenuItem();
   private final JCheckBoxMenuItem jCheckBoxMenuItem_Escapes = new JCheckBoxMenuItem();
   private final JCheckBoxMenuItem jCheckBoxMenuItem_Edges = new JCheckBoxMenuItem();
   private final JRadioButtonMenuItem jRadioButtonMenuItem_Majors = new JRadioButtonMenuItem();
   private final JRadioButtonMenuItem jRadioButtonMenuItem_All = new JRadioButtonMenuItem();
   private final JPanel jPanel_LineChecks = new JPanel();
   private final JPanel jPanel_Button = new JPanel();
   private final JButton jButton_ClearAll = new JButton();
   private final JButton jButton_TempLines = new JButton();
   private final JButton jButton_SelectedLines = new JButton();
   private final JPopupMenu jPopupMenu_SelectedLines = new JPopupMenu();
   private final JRadioButtonMenuItem jRadioButtonMenuItem_SelAll = new JRadioButtonMenuItem();
   private final JRadioButtonMenuItem jRadioButtonMenuItem_SelMajor = new JRadioButtonMenuItem();
   private final ButtonGroup ButtonGroup_AllMajor = new ButtonGroup();
   private final JCheckBoxMenuItem jCheckBoxMenuItem_SelEscapes = new JCheckBoxMenuItem();
   private final JCheckBoxMenuItem jCheckBoxMenuItem_SelSatellites = new JCheckBoxMenuItem();
   private final JCheckBoxMenuItem jCheckBoxMenuItem_SelEdges = new JCheckBoxMenuItem();
   private final JCheckBoxMenuItem jCheckBoxMenuItem_SelSum = new JCheckBoxMenuItem();

   /**
    * getTransitionLines - Returns a SortedSet containing KLMLine objects for
    * each line in transitions list that actually occurs
    * 
    * @param family
    *           int - The family of lines to get
    * @param mode
    *           int -
    * @param atomicNumber
    *           int - The atomic number of the element
    * @return SortedSet - Containing KLMLine objects
    */
   private static SortedSet<KLMLine> getTransitionLines(int family, int mode, Element elm) {
      final TreeSet<KLMLine> temp = new TreeSet<KLMLine>();
      assert ((mode == MAJOR_LINES) || (mode == ALL_LINES));
      final double wgt = (mode == MAJOR_LINES ? MIN_WEIGHT_MAJOR : MIN_WEIGHT_ALL);
      for (int xrt = XRayTransition.KA1; xrt < XRayTransition.Last; ++xrt)
         if ((XRayTransition.getFamily(xrt) == family) && (XRayTransition.getWeight(elm, xrt, XRayTransition.NormalizeKLM) >= wgt))
            try {
               temp.add(new KLMLine.Transition(new XRayTransition(elm, xrt)));
            } catch (final EPQException e) {
               // Ignore it..
            }
      return temp;
   }

   /**
    * getEdges - Returns a SortedSet containing KLMLine objects for each edge
    * 
    * @param family
    *           int - The family of lines to get
    * @param mode
    *           int -
    * @param atomicNumber
    *           int - The atomic number of the element
    * @return SortedSet - Containing KLMLine objects
    */
   private static SortedSet<KLMLine> getEdges(int family, int mode, Element elm) {
      final TreeSet<KLMLine> temp = new TreeSet<KLMLine>();
      assert ((mode == MAJOR_LINES) || (mode == ALL_LINES));
      if (mode == ALL_LINES) {
         for (int sh = AtomicShell.K; sh <= AtomicShell.MV; sh++)
            if ((AtomicShell.getFamily(sh) == family) && showFamily(elm, family))
               temp.add(new KLMLine.Edge(new AtomicShell(elm, sh)));
      } else
         for (final int majorEdge : MajorEdges) {
            if ((AtomicShell.getFamily(majorEdge) == family) && showFamily(elm, family))
               temp.add(new KLMLine.Edge(new AtomicShell(elm, majorEdge)));
         }
      return temp;
   }

   public KLMPanel() {
      super(new FormLayout("pref, 3dlu, 50dlu, 3dlu, pref", "pref"));
      try {
         mElementsArray = new KLMProperties[XRayTransition.lastWeights().getAtomicNumber() + 1];
         for (int z = Element.elmH; z < mElementsArray.length; z++)
            mElementsArray[z] = new KLMProperties(Element.byAtomicNumber(z));
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   private boolean showFamily(int family) {
      return showFamily(Element.byAtomicNumber(mCurrentZ), family);
   }

   /**
    * Are there lines to display in the specified family?
    * 
    * @param family
    * @return true to display this family check box
    */
   private static boolean showFamily(Element elm, int family) {
      return (XRayTransition.getBrightestTransition(elm, family) != null)
            && (XRayTransition.getBrightestTransition(elm, family).getEdgeEnergy() < ToSI.keV(25.0))
            && (XRayTransition.getBrightestTransition(elm, family).getEdgeEnergy() > ToSI.keV(0.1));
   }

   /**
    * Checks whether to check or uncheck the "Select all" check box
    */
   private void updateAllSelected() {
      boolean all = true;
      final KLMProperties kp = mElementsArray[mCurrentZ];
      if (showFamily(AtomicShell.KFamily))
         all &= kp.mKLineSelected;
      if (showFamily(AtomicShell.LFamily))
         all &= kp.mLLineSelected;
      if (showFamily(AtomicShell.MFamily))
         all &= kp.mMLineSelected;
      kp.mAllLinesSelected = all;
      jCheckBox_AllLines.setSelected(all);
   }

   private Set<Element> parseElementField(String text) {
      final String[] items = text.split("[ \\t\\n\\x0B\\f\\r,]");
      final TreeSet<Element> elms = new TreeSet<Element>();
      for (String item : items) {
         final Element elm = Element.byName(item);
         if (!elm.equals(Element.None))
            elms.add(elm);
      }
      return elms;
   }

   private void initialize() throws Exception {
      // Define jPanel_LineChecks
      jCheckBox_KLine.setAlignmentY((float) 0.0);
      jCheckBox_KLine.setMnemonic(KeyEvent.VK_K);
      jCheckBox_KLine.setText(KLineCheckBox);
      jCheckBox_KLine.setForeground(SystemColor.controlText);
      jCheckBox_KLine.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final boolean selected = jCheckBox_KLine.isSelected();
            mElementsArray[mCurrentZ].mKLineSelected = selected;
            updateAllSelected();
            final KLMActionEvent.KLMAction action = (selected ? KLMAction.ADD_LINES : KLMAction.REMOVE_LINES);
            final KLMActionEvent kae = new KLMActionEvent(KLMPanel.this, mElementsArray[mCurrentZ].getVisibleLines(AtomicShell.KFamily), action);
            fireVisibleLinesActionPerformed(kae);
            jTextField_Element.selectAll();
            jTextField_Element.requestFocus();
         }
      });
      jCheckBox_LLine.setAlignmentX((float) 0.0);
      jCheckBox_LLine.setMnemonic(KeyEvent.VK_L);
      jCheckBox_LLine.setText(LLineCheckBox);
      jCheckBox_LLine.setForeground(SystemColor.controlText);
      jCheckBox_LLine.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final boolean selected = jCheckBox_LLine.isSelected();
            mElementsArray[mCurrentZ].mLLineSelected = selected;
            updateAllSelected();
            final KLMActionEvent.KLMAction action = (selected ? KLMAction.ADD_LINES : KLMAction.REMOVE_LINES);
            final KLMActionEvent kae = new KLMActionEvent(KLMPanel.this, mElementsArray[mCurrentZ].getVisibleLines(AtomicShell.LFamily), action);
            fireVisibleLinesActionPerformed(kae);
            jTextField_Element.selectAll();
            jTextField_Element.requestFocus();
         }
      });
      jCheckBox_MLine.setAlignmentX((float) 0.0);
      jCheckBox_MLine.setMnemonic(KeyEvent.VK_M);
      jCheckBox_MLine.setText(MLineCheckBox);
      jCheckBox_MLine.setForeground(SystemColor.controlText);
      jCheckBox_MLine.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final boolean selected = jCheckBox_MLine.isSelected();
            mElementsArray[mCurrentZ].mMLineSelected = selected;
            updateAllSelected();
            final KLMActionEvent.KLMAction action = (selected ? KLMAction.ADD_LINES : KLMAction.REMOVE_LINES);
            final KLMActionEvent kae = new KLMActionEvent(KLMPanel.this, mElementsArray[mCurrentZ].getVisibleLines(AtomicShell.MFamily), action);
            fireVisibleLinesActionPerformed(kae);
            jTextField_Element.selectAll();
            jTextField_Element.requestFocus();
         }
      });
      jCheckBox_AllLines.setAlignmentX((float) 0.0);
      jCheckBox_AllLines.setMnemonic(KeyEvent.VK_A);
      jCheckBox_AllLines.setText(AllLinesCheckBox);
      jCheckBox_AllLines.setForeground(SystemColor.controlText);
      jCheckBox_AllLines.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final boolean selected = jCheckBox_AllLines.isSelected();
            final SortedSet<KLMLine> ss = new TreeSet<KLMLine>();
            if (showFamily(AtomicShell.KFamily) && (mElementsArray[mCurrentZ].mKLineSelected != selected)) {
               mElementsArray[mCurrentZ].mKLineSelected = selected;
               ss.addAll(mElementsArray[mCurrentZ].getVisibleLines(AtomicShell.KFamily));
               jCheckBox_KLine.setSelected(selected);
            }
            if (showFamily(AtomicShell.LFamily) && (mElementsArray[mCurrentZ].mLLineSelected != selected)) {
               mElementsArray[mCurrentZ].mLLineSelected = selected;
               ss.addAll(mElementsArray[mCurrentZ].getVisibleLines(AtomicShell.LFamily));
               jCheckBox_LLine.setSelected(selected);
            }
            if (showFamily(AtomicShell.MFamily) && (mElementsArray[mCurrentZ].mMLineSelected != selected)) {
               mElementsArray[mCurrentZ].mMLineSelected = selected;
               ss.addAll(mElementsArray[mCurrentZ].getVisibleLines(AtomicShell.MFamily));
               jCheckBox_MLine.setSelected(selected);
            }
            mElementsArray[mCurrentZ].mAllLinesSelected = selected;
            final KLMActionEvent.KLMAction action = (selected ? KLMAction.ADD_LINES : KLMAction.REMOVE_LINES);
            final KLMActionEvent kae = new KLMActionEvent(KLMPanel.this, ss, action);
            fireVisibleLinesActionPerformed(kae);
            jTextField_Element.selectAll();
            jTextField_Element.requestFocus();
         }
      });
      jPanel_LineChecks.setLayout(new BoxLayout(jPanel_LineChecks, BoxLayout.PAGE_AXIS));
      jPanel_LineChecks.add(jCheckBox_KLine, null);
      jPanel_LineChecks.add(jCheckBox_LLine, null);
      jPanel_LineChecks.add(jCheckBox_MLine, null);
      jPanel_LineChecks.add(jCheckBox_AllLines, null);

      // Define jPanel_Element
      jLabel_Element.setText("Element:");
      jLabel_Element.setForeground(SystemColor.controlText);
      jLabel_Element.setDisplayedMnemonic('E');
      jLabel_Element.setLabelFor(jTextField_Element);

      jTextField_Element.setText("H");
      jTextField_Element.setHorizontalAlignment(SwingConstants.CENTER);

      jTextField_Element.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            try {
               setAtomicNumber(Element.byName(jTextField_Element.getText()).getAtomicNumber());
            } catch (final Exception jTextField1ReadError) {
               try {
                  setAtomicNumber(Integer.parseInt(jTextField_Element.getText()));
               } catch (final Exception jTextFieldReadError2) {
                  jTextField_Element.setText(Element.toAbbrev(getAtomicNumber()));
               }
            }
            redraw();
         }
      });

      jTextField_Element.addFocusListener(new FocusAdapter() {
         @Override
         public void focusLost(FocusEvent e) {
            final String txt = jTextField_Element.getText();
            Element el = Element.byName(txt);
            if (!el.isValid())
               try {
                  final int z = Integer.parseInt(txt);
                  el = Element.byAtomicNumber(z);
               } catch (final Exception ex) {
                  // Ignore any errors
               }
            setElement(el);
            redraw();
         }
      });

      final KeyAdapter ka = new KeyAdapter() {
         @Override
         public void keyTyped(KeyEvent arg0) {
            int inc = 0;
            switch (arg0.getKeyChar()) {
               case '+' :
                  inc = 1;
                  break;
               case '-' :
                  inc = -1;
                  break;
               default :
                  break;
            }
            if (inc != 0) {
               arg0.consume();
               setAtomicNumber(getAtomicNumber() + inc);
            }
         }

         @Override
         public void keyReleased(KeyEvent arg0) {
            if ((arg0.getKeyCode() == KeyEvent.VK_ENTER) && arg0.isControlDown()) {
               final Set<Element> elms = parseElementField(jTextField_Element.getText());
               for (final Element elm : elms) {
                  setAtomicNumber(elm.getAtomicNumber());
                  jCheckBox_AllLines.doClick();
               }
            }
         }

      };

      jTextField_Element.addKeyListener(ka);

      jScrollBar_Element.setOrientation(Adjustable.HORIZONTAL);
      jScrollBar_Element.setBlockIncrement(1);
      final DefaultBoundedRangeModel brm = new DefaultBoundedRangeModel();
      brm.setMinimum(Element.elmH);
      brm.setMaximum(Element.elmAm);
      brm.addChangeListener(new ChangeListener() {
         @Override
         public void stateChanged(ChangeEvent e) {
            final BoundedRangeModel brm = (BoundedRangeModel) e.getSource();
            assert brm.getMinimum() == Element.elmH;
            assert brm.getMaximum() == Element.elmAm;
            setAtomicNumber(brm.getValue());
         }
      });
      brm.setValue(Element.elmH);
      jScrollBar_Element.setModel(brm);
      jPanel_Element.setLayout(new GridLayout(3, 1, 0, 5));
      jPanel_Element.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
      jPanel_Element.add(jLabel_Element);
      jPanel_Element.add(jTextField_Element);
      jPanel_Element.add(jScrollBar_Element);

      jButton_TempLines.setMargin(new Insets(2, 2, 2, 2));
      jButton_TempLines.setText("Temporary Lines");
      jButton_TempLines.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jPopupMenu_TempLines.show(jPanel_Button, jButton_TempLines.getLocation().x,
                  jButton_TempLines.getLocation().y + jButton_TempLines.getSize().height);
         }
      });
      jButton_SelectedLines.setMargin(new Insets(2, 2, 2, 2));
      jButton_SelectedLines.setText("Selected Lines");
      jButton_SelectedLines.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jPopupMenu_SelectedLines.show(jPanel_Button, jButton_SelectedLines.getLocation().x,
                  jButton_SelectedLines.getLocation().y + jButton_SelectedLines.getSize().height);
         }
      });
      jButton_ClearAll.setMargin(new Insets(2, 2, 2, 2));
      jButton_ClearAll.setForeground(Color.red);
      jButton_ClearAll.setText("Clear All Lines");
      jButton_ClearAll.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            for (int z = Element.elmH; z < mElementsArray.length; z++)
               mElementsArray[z] = new KLMProperties(Element.byAtomicNumber(z));
            setAtomicNumber(Element.elmH);
            redraw();
            fireTemporaryLinesActionPerformed(e);
            fireVisibleLinesActionPerformed(KLMActionEvent.clearAllEvent(KLMPanel.this));
         }
      });

      jPanel_Button.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
      jPanel_Button.setLayout(new GridLayout(3, 1, 0, BORDER_SIZE));
      jPanel_Button.add(jButton_TempLines, null);
      jPanel_Button.add(jButton_SelectedLines, null);
      jPanel_Button.add(jButton_ClearAll, null);

      final CellConstraints cc = new CellConstraints();
      add(jPanel_LineChecks, cc.xy(1, 1));
      add(jPanel_Element, cc.xy(3, 1));
      add(jPanel_Button, cc.xy(5, 1));

      jCheckBoxMenuItem_Escapes.setSelected(false);
      jCheckBoxMenuItem_Escapes.setText(EscapesMenuItem);
      jCheckBoxMenuItem_Escapes.setEnabled(false);
      jCheckBoxMenuItem_Escapes.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mTemporaryEscapesSelected = jCheckBoxMenuItem_Escapes.isSelected();
            for (int z = Element.elmH; z < mElementsArray.length; ++z)
               if (!areAnyCheckBoxesChecked(z))
                  mElementsArray[z].mVisibleEscapesSelected = mTemporaryEscapesSelected;
            redraw();
         }
      });
      jCheckBoxMenuItem_Edges.setText(EdgesMenuItem);
      jCheckBoxMenuItem_Edges.setEnabled(true);
      jCheckBoxMenuItem_Edges.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mTemporaryEdgesSelected = jCheckBoxMenuItem_Edges.isSelected();
            for (int z = Element.elmH; z < mElementsArray.length; ++z)
               if (!areAnyCheckBoxesChecked(z))
                  mElementsArray[z].mVisibleEdgesSelected = mTemporaryEdgesSelected;
            redraw();
            fireTemporaryLinesActionPerformed(e);
         }
      });
      jRadioButtonMenuItem_Majors.setText(MajorPeaksMenuItem);
      jRadioButtonMenuItem_Majors.setEnabled(true);
      jRadioButtonMenuItem_Majors.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jRadioButtonMenuItem_Majors.setSelected(true);
            jRadioButtonMenuItem_All.setSelected(false);

            mTemporaryMajorPeaksSelected = jRadioButtonMenuItem_Majors.isSelected();
            mTemporaryAllPeaksSelected = jRadioButtonMenuItem_All.isSelected();

            for (int z = Element.elmH; z < mElementsArray.length; ++z)
               if (!areAnyCheckBoxesChecked(z))
                  mElementsArray[z].mVisibleAllPeaksSelected = mTemporaryAllPeaksSelected;
            redraw();
            fireTemporaryLinesActionPerformed(e);
         }
      });
      jRadioButtonMenuItem_All.setSelected(true);
      jRadioButtonMenuItem_All.setText(AllPeaksMenuItem);
      jRadioButtonMenuItem_All.setEnabled(true);
      jRadioButtonMenuItem_All.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jRadioButtonMenuItem_Majors.setSelected(false);
            jRadioButtonMenuItem_All.setSelected(true);

            mTemporaryAllPeaksSelected = jRadioButtonMenuItem_All.isSelected();
            mTemporaryMajorPeaksSelected = jRadioButtonMenuItem_Majors.isSelected();

            for (int z = Element.elmH; z < mElementsArray.length; ++z)
               if (!areAnyCheckBoxesChecked(z))
                  mElementsArray[z].mVisibleAllPeaksSelected = mTemporaryAllPeaksSelected;
            redraw();
            fireTemporaryLinesActionPerformed(e);
         }
      });

      jPopupMenu_TempLines.setOpaque(true);
      jPopupMenu_TempLines.setRequestFocusEnabled(true);
      jPopupMenu_TempLines.add(jRadioButtonMenuItem_All);
      jPopupMenu_TempLines.add(jRadioButtonMenuItem_Majors);
      jPopupMenu_TempLines.addSeparator();
      jPopupMenu_TempLines.add(jCheckBoxMenuItem_Escapes);
      jPopupMenu_TempLines.add(jCheckBoxMenuItem_Satellites);
      jPopupMenu_TempLines.add(jCheckBoxMenuItem_Edges);

      jRadioButtonMenuItem_SelAll.setSelected(true);
      jRadioButtonMenuItem_SelAll.setText(AllPeaksMenuItem);
      jRadioButtonMenuItem_SelAll.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jRadioButtonMenuItem_SelAll.setSelected(true);
            // Add those in all not in major
            final SortedSet<KLMLine> ss = new TreeSet<KLMLine>();
            for (int z = Element.elmH; z < mElementsArray.length; ++z) {
               ss.addAll(mElementsArray[z].getAllDelta());
               mElementsArray[z].mVisibleAllPeaksSelected = true;
            }
            final KLMActionEvent kae = new KLMActionEvent(KLMPanel.this, ss, KLMAction.ADD_LINES);
            fireVisibleLinesActionPerformed(kae);
         }
      });
      jRadioButtonMenuItem_SelMajor.setText(MajorPeaksMenuItem);
      jRadioButtonMenuItem_SelMajor.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            // Remove those in all not in major
            jRadioButtonMenuItem_SelMajor.setSelected(true);
            // Add those in all not in major
            final SortedSet<KLMLine> ss = new TreeSet<KLMLine>();
            for (int z = Element.elmH; z < mElementsArray.length; ++z) {
               ss.addAll(mElementsArray[z].getAllDelta());
               mElementsArray[z].mVisibleAllPeaksSelected = true;
            }
            final KLMActionEvent kae = new KLMActionEvent(KLMPanel.this, ss, KLMAction.REMOVE_LINES);
            fireVisibleLinesActionPerformed(kae);
         }
      });
      jCheckBoxMenuItem_SelEscapes.setText(EscapesMenuItem);
      jCheckBoxMenuItem_SelEscapes.setEnabled(true);
      jCheckBoxMenuItem_SelEscapes.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            final KLMProperties kp = mElementsArray[mCurrentZ];
            kp.mVisibleEscapesSelected = jCheckBoxMenuItem_SelEscapes.isSelected();
            final SortedSet<KLMLine> ss = kp.getEscapes(Element.byAtomicNumber(mCurrentZ));
            final KLMAction action = (kp.mVisibleEscapesSelected ? KLMAction.ADD_LINES : KLMAction.REMOVE_LINES);
            final KLMActionEvent kae = new KLMActionEvent(KLMPanel.this, ss, action);
            fireVisibleLinesActionPerformed(kae);
         }
      });
      jCheckBoxMenuItem_SelSatellites.setText(SatellitesMenuItem);
      jCheckBoxMenuItem_SelSatellites.setEnabled(false);
      jCheckBoxMenuItem_SelSatellites.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mElementsArray[mCurrentZ].mVisibleSatellitesSelected = jCheckBoxMenuItem_SelSatellites.isSelected();
         }
      });
      jCheckBoxMenuItem_SelEdges.setText(EdgesMenuItem);
      jCheckBoxMenuItem_SelEdges.setEnabled(true);
      jCheckBoxMenuItem_SelEdges.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mElementsArray[mCurrentZ].mVisibleEdgesSelected = jCheckBoxMenuItem_SelEdges.isSelected();
         }
      });
      jCheckBoxMenuItem_SelSum.setText(SumPeaksMenuItem);
      jCheckBoxMenuItem_SelSum.setEnabled(false);
      jCheckBoxMenuItem_SelSum.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            mElementsArray[mCurrentZ].mVisibleSumPeaksSelected = jCheckBoxMenuItem_SelSum.isSelected();
         }
      });
      ButtonGroup_AllMajor.add(jRadioButtonMenuItem_SelAll);
      ButtonGroup_AllMajor.add(jRadioButtonMenuItem_SelMajor);
      jPopupMenu_SelectedLines.add(jRadioButtonMenuItem_SelAll);
      jPopupMenu_SelectedLines.add(jRadioButtonMenuItem_SelMajor);
      jPopupMenu_SelectedLines.addSeparator();
      jPopupMenu_SelectedLines.add(jCheckBoxMenuItem_SelEscapes);
      jPopupMenu_SelectedLines.add(jCheckBoxMenuItem_SelSatellites);
      jPopupMenu_SelectedLines.add(jCheckBoxMenuItem_SelEdges);
      jPopupMenu_SelectedLines.add(jCheckBoxMenuItem_SelSum);

      addMouseWheelListener(new MouseWheelListener() {
         @Override
         public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
            final int rot = e.getWheelRotation();
            setAtomicNumber(getAtomicNumber() + rot);
         }
      });

   }

   private boolean areAnyCheckBoxesChecked(int index) {
      return ((((mElementsArray[index].mKLineSelected) || (mElementsArray[index].mLLineSelected)) || (mElementsArray[index].mMLineSelected))
            || (mElementsArray[index].mAllLinesSelected));
   }

   private void redraw() {
      assert mCurrentZ >= Element.elmH;
      assert mCurrentZ < mElementsArray.length;
      if (mCurrentZ != Element.elmNone) {
         final KLMProperties CurrentElement = mElementsArray[mCurrentZ];
         assert CurrentElement != null;
         jCheckBox_KLine.setSelected(CurrentElement.mKLineSelected);
         jCheckBox_LLine.setSelected(CurrentElement.mLLineSelected);
         jCheckBox_MLine.setSelected(CurrentElement.mMLineSelected);
         jCheckBox_AllLines.setSelected(CurrentElement.mAllLinesSelected);
         jRadioButtonMenuItem_All.setSelected(mTemporaryAllPeaksSelected);
         jRadioButtonMenuItem_Majors.setSelected(mTemporaryMajorPeaksSelected);
         jCheckBoxMenuItem_Escapes.setSelected(mTemporaryEscapesSelected);
         jCheckBoxMenuItem_Satellites.setSelected(mTemporarySatellitesSelected);
         jCheckBoxMenuItem_Edges.setSelected(mTemporaryEdgesSelected);

         jRadioButtonMenuItem_SelAll.setSelected(CurrentElement.mVisibleAllPeaksSelected);
         jCheckBoxMenuItem_SelEscapes.setSelected(CurrentElement.mVisibleEscapesSelected);
         jCheckBoxMenuItem_SelSatellites.setSelected(CurrentElement.mVisibleSatellitesSelected);
         jCheckBoxMenuItem_SelEdges.setSelected(CurrentElement.mVisibleEdgesSelected);
         jCheckBoxMenuItem_SelSum.setSelected(CurrentElement.mVisibleSumPeaksSelected);

         try {
            jTextField_Element.setText(CurrentElement.mElement.toAbbrev());
         } catch (final Exception ex) {
            System.err.println("Warning: Unable to read current element!");
         }
      }
   }

   protected void fireTemporaryLinesActionPerformed(ActionEvent e) {
      for (final ActionListener al : mTemporaryLinesActionListeners)
         al.actionPerformed(e);
   }

   public synchronized void removeTemporaryLinesActionListener(ActionListener l) {
      if (mTemporaryLinesActionListeners.contains(l)) {
         final Vector<ActionListener> v = new Vector<ActionListener>(mTemporaryLinesActionListeners);
         v.removeElement(l);
         mTemporaryLinesActionListeners = v;
      }
   }

   public synchronized void addTemporaryLinesActionListener(ActionListener l) {
      if (!mTemporaryLinesActionListeners.contains(l)) {
         final Vector<ActionListener> v = new Vector<ActionListener>(mTemporaryLinesActionListeners);
         v.addElement(l);
         mTemporaryLinesActionListeners = v;
      }
   }

   protected void fireVisibleLinesActionPerformed(KLMActionEvent e) {
      for (final ActionListener al : mVisibleLinesActionListeners)
         al.actionPerformed(e);
   }

   public synchronized void removeVisibleLinesActionListener(ActionListener l) {
      if (mVisibleLinesActionListeners.contains(l)) {
         final Vector<ActionListener> v = new Vector<ActionListener>(mVisibleLinesActionListeners);
         v.removeElement(l);
         mVisibleLinesActionListeners = v;
      }
   }

   public synchronized void addVisibleLinesActionListener(ActionListener l) {
      if (!mVisibleLinesActionListeners.contains(l)) {
         final Vector<ActionListener> v = new Vector<ActionListener>(mVisibleLinesActionListeners);
         v.addElement(l);
         mVisibleLinesActionListeners = v;
      }
   }

   /**
    * getAtomicNumber - returns the atomic number of the element currently
    * displayed in the text box.
    * 
    * @return int
    */
   public int getAtomicNumber() {
      return mCurrentZ;
   }

   public boolean isKSelected(Element elm) {
      return mElementsArray[elm.getAtomicNumber()].mKLineSelected;
   }

   public boolean isLSelected(Element elm) {
      return mElementsArray[elm.getAtomicNumber()].mLLineSelected;
   }

   public boolean isMSelected(Element elm) {
      return mElementsArray[elm.getAtomicNumber()].mMLineSelected;
   }

   public Element getElement() {
      return Element.byAtomicNumber(mCurrentZ);
   }

   public void setElement(Element elm) {
      final int newZ = elm.getAtomicNumber();
      if ((mCurrentZ != newZ) && (newZ >= Element.elmH) && (newZ < mElementsArray.length)) {
         mCurrentZ = elm.getAtomicNumber();
         jCheckBox_KLine.setEnabled((mCurrentZ != Element.elmNone) && showFamily(AtomicShell.KFamily));
         jCheckBox_LLine.setEnabled((mCurrentZ != Element.elmNone) && showFamily(AtomicShell.LFamily));
         jCheckBox_MLine.setEnabled((mCurrentZ != Element.elmNone) && showFamily(AtomicShell.MFamily));
         jCheckBox_AllLines.setEnabled((mCurrentZ != Element.elmNone) && showFamily(AtomicShell.KFamily));
         if (jScrollBar_Element.getValue() != mCurrentZ)
            jScrollBar_Element.setValue(mCurrentZ);
         redraw();
         fireTemporaryLinesActionPerformed(new ActionEvent(this, 0, "setAtomicNumber"));
      }
   }

   public void setAtomicNumber(int AtomicNumber) {
      setElement(Element.byAtomicNumber(AtomicNumber));
   }

   /**
    * getTemporaryLines - returns a sorted set of XRayTransition that has all
    * the lines for the current element stored in it.
    * 
    * @return SortedSet
    */
   public SortedSet<KLMLine> getTemporaryLines() {
      return mElementsArray[mCurrentZ].getTemporaryLines();
   }

   /**
    * getVisibleLines - Returns a sorted set of XRayTransition containing the
    * lines corresponding to the checkmarked boxes on the dialog.
    * 
    * @return SortedSet
    */
   public SortedSet<KLMLine> getVisibleLines() {
      mVisibleLines.clear();
      for (int z = Element.elmH; z < mElementsArray.length; ++z)
         mVisibleLines.addAll(mElementsArray[z].getVisibleLines());
      return mVisibleLines;
   }
}
