package gov.nist.microanalysis.EPQTools;

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BoundedRangeModel;
import javax.swing.ButtonGroup;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import gov.nist.microanalysis.EPQDatabase.Session;
import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQLibrary.XRayTransitionSet;
import gov.nist.microanalysis.EPQTools.KLMActionEvent.KLMAction;
import gov.nist.microanalysis.Utility.Math2;

public class KLMTreePanel extends JPanel implements ActionListener {

   // Optional access to the database
   private Session mSession;

   private static final long serialVersionUID = 8450421755540368227L;
   final private TreeSet<KLMLine> mSelected = new TreeSet<KLMLine>();
   final private TreeSet<KLMLine> mTemporary = new TreeSet<KLMLine>();

   private final double mMaxEnergy = ToSI.keV(300.0);
   private final double mMinWeight = 0.001;
   private final double mMajorWeight = 0.00;
   private final double mMinEscape = 0.6;
   private final XRayTransition mEscapeTransition = new XRayTransition(Element.byName("Silicon"), XRayTransition.KA1);

   final private JTree jTree_Lines = new JTree();

   final private JTextField jTextField_Element = new JTextField();
   final private JScrollBar jScrollBar_Z = new JScrollBar();
   private final JButton jButton_Minus = new JButton("-");
   private final JButton jButton_Plus = new JButton("+");
   final private JButton jButton_Clear = new JButton("Clear");
   final private JButton jButton_ClearAll = new JButton("Clear All");
   final private JRadioButton jRadioButton_AtomicNumber = new JRadioButton("Z-order");
   final private JRadioButton jRadioButton_Energy = new JRadioButton("E-order");
   final private ButtonGroup jButtonGroup_Order = new ButtonGroup();
   final private JLabel jLabel_Energy = new JLabel("0.000 keV");

   transient private int mElementIndex = -1;
   private Element[] mElementOrder = Element.range(Element.H, Element.Cm);
   private String[] mElementLabel = null;

   private transient Vector<ActionListener> mVisibleLinesActionListeners = new Vector<ActionListener>();
   private transient Vector<ActionListener> mTemporaryLinesActionListeners = new Vector<ActionListener>();

   /**
    * Constructs a KLMTreePanel<br>
    * <table>
    * <tr>
    * <td>K Transitions</td>
    * <td>Kalpha</td>
    * </tr>
    * <tr>
    * <td></td>
    * <td>Kbeta</td>
    * </tr>
    * <tr>
    * <td>L Transitions</td>
    * <td>Lalpha</td>
    * </tr>
    * <tr>
    * <td></td>
    * <td>Lbeta</td>
    * </tr>
    * <tr>
    * <td></td>
    * <td>Lgamma</td>
    * </tr>
    * <tr>
    * <td></td>
    * <td>Lother</td>
    * </tr>
    * <tr>
    * <td>M Transitions</td>
    * <td>Malpha</td>
    * </tr>
    * <tr>
    * <td></td>
    * <td>Mbeta</td>
    * </tr>
    * <tr>
    * <td></td>
    * <td>Mgamma</td>
    * </tr>
    * <tr>
    * <td></td>
    * <td>Mother</td>
    * </tr>
    * <tr>
    * <td>K edge</td>
    * <td></td>
    * </tr>
    * <tr>
    * <td>L edges</td>
    * <td></td>
    * </tr>
    * <tr>
    * <td>M edges</td>
    * <td></td>
    * </tr>
    * <tr>
    * <td>Escape peaks</td>
    * <td>K</td>
    * </tr>
    * <tr>
    * <td></td>
    * <td>L</td>
    * </tr>
    * <tr>
    * <td></td>
    * <td>M</td>
    * </tr>
    * <tr>
    * <td>Satellites</td>
    * <td></td>
    * </tr>
    * </table>
    */

   public KLMTreePanel() {
      super();
      try {
         initialize();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   public Element currentElement() {
      return mElementOrder[mElementIndex];
   }

   private void rationalOrder() {
      final Comparator<XRayTransition> comparator = new Comparator<XRayTransition>() {
         @Override
         public int compare(XRayTransition o1, XRayTransition o2) {
            double e1;
            try {
               e1 = o1.getEnergy();
            } catch (final EPQException e) {
               return -1;
            }
            double e2;
            try {
               e2 = o2.getEnergy();
            } catch (final EPQException e) {
               return 1;
            }
            return e1 < e2 ? -1 : (e1 > e2 ? 1 : 0);
         }
      };
      final TreeSet<XRayTransition> tmp = new TreeSet<XRayTransition>(comparator);
      final int[] trs = new int[]{XRayTransition.KA1, XRayTransition.LA1, XRayTransition.MA1, XRayTransition.N5N6};
      for (int z = Element.elmH; z < Element.elmAm; ++z) {
         final Element elm = Element.byAtomicNumber(z);
         for (final int tr : trs)
            try {
               if (XRayTransition.exists(elm, tr) && (XRayTransition.getEnergy(elm, tr) < ToSI.keV(30.0)))
                  tmp.add(new XRayTransition(elm, tr));
            } catch (final EPQException e) {
               // Ignore
            }
      }
      final Element[] res = new Element[tmp.size() + 2];
      final String[] labels = new String[res.length];
      res[0] = Element.H;
      labels[0] = res[0].toString();
      res[1] = Element.He;
      labels[1] = res[1].toString();
      int i = 2;
      final NumberFormat nf = new DecimalFormat("0.000 keV");
      for (final XRayTransition xrt : tmp) {
         res[i] = xrt.getElement();
         try {
            labels[i] = nf.format(FromSI.keV(xrt.getEnergy()));
         } catch (final EPQException e) {
            labels[i] = "? keV";
         }
         i++;
      }
      mElementOrder = res;
      mElementLabel = labels;
   }

   class UpdateOrder implements ActionListener {

      @Override
      public void actionPerformed(ActionEvent e) {
         final Element elm = currentElement();
         if (jRadioButton_AtomicNumber.isSelected()) {
            mElementOrder = Element.range(Element.H, Element.Cm);
            mElementLabel = null;
         } else
            rationalOrder();
         final DefaultBoundedRangeModel brm = new DefaultBoundedRangeModel();
         brm.setMinimum(0);
         brm.setMaximum(mElementOrder.length);
         brm.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
               final BoundedRangeModel brm = (BoundedRangeModel) e.getSource();
               try {
                  mElementIndex = 1;
                  updateElement(brm.getValue());
               } catch (final EPQException e1) {
                  // Ignore it. Just don't
               }
            }
         });
         jScrollBar_Z.setModel(brm);
         try {
            mElementIndex = 0;
            setElement(elm);
         } catch (final EPQException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         }
      }
   };

   public Set<Element> parseElementField(String text) {
      if (text.startsWith("\""))
         text = text.substring(1);
      if (text.endsWith("\""))
         text = text.substring(0, text.length() - 1);
      final String[] items = text.split("[ \\t\\n\\x0B\\f\\r,]");
      final TreeSet<Element> elms = new TreeSet<Element>();
      for (final String item : items) {
         Composition comp = null;
         if (mSession != null)
            try {
               comp = mSession.findStandard(item);
            } catch (final SQLException e1) {
               // assume it isn't in the database...
            }
         if (comp == null)
            try {
               comp = MaterialFactory.createCompound(item);
            } catch (final EPQException ex) {
               // Assume that it is a name not a compound
            }
         if (comp != null)
            elms.addAll(comp.getElementSet());
      }
      final ArrayList<KLMLine> setThese = new ArrayList<KLMLine>();
      for (final Element elm : elms)
         setThese.addAll(getDefaultKLMs(elm));
      setKLMs(setThese);
      return elms;
   }

   private Collection<KLMLine> getDefaultKLMs(Element elm) {
      final TreeSet<KLMLine> res = new TreeSet<KLMLine>();
      int last = XRayTransition.MZ1 + 1;
      if (elm.getAtomicNumber() < Element.elmB)
         last = XRayTransition.KA1;
      else if (elm.getAtomicNumber() < Element.elmCa)
         last = XRayTransition.L3N2;
      else if (elm.getAtomicNumber() < Element.elmBa)
         last = XRayTransition.M1N2;
      for (int tr = XRayTransition.KA1; tr < last; ++tr) {
         final XRayTransition xrt = new XRayTransition(elm, tr);
         try {
            if (xrt.exists() && (xrt.getWeight(XRayTransition.NormalizeKLM) >= mMajorWeight) && (xrt.getEnergy() > ToSI.keV(0.1)))
               res.add(new KLMLine.Transition(xrt));
         } catch (final EPQException e) {
            // Just ignore it...
         }
      }
      return res;
   }

   private void setKLMs(Collection<KLMLine> lines) {
      final ArrayList<KLMLine> toAdd = new ArrayList<KLMLine>();
      final ArrayList<KLMLine> toRemove = new ArrayList<KLMLine>();
      for (final KLMLine line : lines)
         if (!mSelected.contains(line))
            toAdd.add(line);
      for (final KLMLine line : lines)
         if (mTemporary.contains(line))
            toRemove.add(line);
      if (toRemove.size() > 0) {
         mTemporary.removeAll(toRemove);
         final KLMActionEvent kae = new KLMActionEvent(this, toRemove, KLMAction.REMOVE_LINES);
         fireTemporaryLinesActionPerformed(kae);
      }
      if (toAdd.size() > 0) {
         mSelected.addAll(toAdd);
         final KLMActionEvent kae = new KLMActionEvent(this, toAdd, KLMAction.ADD_LINES);
         fireVisibleLinesActionPerformed(kae);
      }
   }

   private void initialize() {
      final CellConstraints cc0 = new CellConstraints(), cc1 = new CellConstraints();
      final PanelBuilder pb = new PanelBuilder(
            new FormLayout("pref, 3dlu, pref, 3dlu, 80dlu", "pref, 3dlu, pref, 3dlu, pref, 3dlu, default, 5dlu, pref"), this);
      jTextField_Element.setHorizontalAlignment(SwingConstants.CENTER);
      pb.addLabel("&Element", cc0.xy(1, 1), jTextField_Element, cc1.xy(3, 1));
      jTextField_Element.addFocusListener(new FocusListener() {

         @Override
         public void focusGained(FocusEvent e) {
            jTextField_Element.selectAll();
         }

         @Override
         public void focusLost(FocusEvent arg0) {
            updateElementField();
         }
      });

      final KeyAdapter ka = new KeyAdapter() {

         @Override
         public void keyReleased(KeyEvent arg0) {

            if (arg0.getKeyCode() == KeyEvent.VK_ENTER)
               if (arg0.isControlDown()) {
                  final Set<Element> elms = parseElementField(jTextField_Element.getText());
                  if (elms.size() > 0)
                     try {
                        setElement(elms.iterator().next());
                     } catch (final EPQException e) {
                        // ignore
                     }
               } else
                  updateElementField();

         }

         @Override
         public void keyTyped(KeyEvent arg0) {
            int inc = 0;
            switch (arg0.getKeyChar()) {
               case '+' :
               case '=' :
                  if (!jTextField_Element.getText().startsWith("\""))
                     inc = 1;
                  break;
               case '-' :
               case '_' :
                  if (!jTextField_Element.getText().startsWith("\""))
                     inc = -1;
                  break;
               default :
                  break;
            }
            if (inc != 0) {
               arg0.consume();
               try {
                  updateElement(mElementIndex + inc);
               } catch (final EPQException e) {
               }
            }
         }
      };

      addMouseWheelListener(new MouseWheelListener() {
         @Override
         public void mouseWheelMoved(java.awt.event.MouseWheelEvent e) {
            final int rot = e.getWheelRotation();
            try {
               updateElement(mElementIndex + rot);
            } catch (final EPQException e1) {
               // ignore it.
            }
         }
      });

      jTextField_Element.addKeyListener(ka);
      jScrollBar_Z.addKeyListener(ka);

      jScrollBar_Z.setOrientation(Adjustable.HORIZONTAL);
      jScrollBar_Z.setBlockIncrement(1);
      pb.add(jRadioButton_AtomicNumber, cc0.xy(1, 3));
      jButtonGroup_Order.add(jRadioButton_AtomicNumber);
      jButtonGroup_Order.add(jRadioButton_Energy);
      jRadioButton_AtomicNumber.setSelected(true);
      jRadioButton_AtomicNumber.addActionListener(new UpdateOrder());
      jRadioButton_Energy.addActionListener(new UpdateOrder());
      pb.add(jRadioButton_Energy, cc0.xy(3, 3));
      pb.add(jScrollBar_Z, cc0.xyw(1, 5, 3));
      pb.add(jLabel_Energy, cc0.xyw(1, 7, 3, "center, fill"));
      final DefaultBoundedRangeModel brm = new DefaultBoundedRangeModel();
      brm.setMinimum(0);
      brm.setMaximum(mElementOrder.length);
      brm.addChangeListener(new ChangeListener() {
         @Override
         public void stateChanged(ChangeEvent e) {
            final BoundedRangeModel brm = (BoundedRangeModel) e.getSource();
            try {
               updateElement(brm.getValue());
            } catch (final EPQException e1) {
               // Ignore it. Just don't
            }
         }
      });
      brm.setValue(Element.elmH);
      jScrollBar_Z.setModel(brm);

      jButton_Clear.setMnemonic(KeyEvent.VK_C);
      pb.add(jButton_Clear, cc0.xy(1, 9));
      jButton_Clear.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            try {
               final ArrayList<KLMLine> remove = new ArrayList<KLMLine>();
               final Element currentElement = currentElement();
               for (final KLMLine klm : mSelected)
                  if (klm.getShell().getElement().equals(currentElement))
                     remove.add(klm);
               if (remove.size() > 0) {
                  mSelected.removeAll(remove);
                  final int tmp = mElementIndex;
                  mElementIndex = -1;
                  updateElement(tmp);
                  final KLMActionEvent kae = new KLMActionEvent(this, remove, KLMAction.REMOVE_LINES);
                  fireVisibleLinesActionPerformed(kae);
               }
            } catch (final EPQException e1) {
               e1.printStackTrace();
            }
         }
      });

      jButton_ClearAll.setMnemonic(KeyEvent.VK_A);
      pb.add(jButton_ClearAll, cc0.xy(3, 9));
      jButton_ClearAll.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            try {
               clearAll();
            } catch (final EPQException e1) {
               // Ignore it.
            }
         }
      });

      jTree_Lines.setModel(new DefaultTreeModel(new CheckNode("None")));
      jTree_Lines.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      jTree_Lines.putClientProperty("JTree.lineStyle", "Angled");
      jTree_Lines.addMouseListener(new NodeSelectionListener(jTree_Lines));
      jTree_Lines.setCellRenderer(new CheckRenderer());
      {
         final InputMap im = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
         im.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.ALT_DOWN_MASK), XRayTransitionSet.K_FAMILY);
         getActionMap().put(XRayTransitionSet.K_FAMILY, new ToggleAction(XRayTransitionSet.K_FAMILY));
         im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_DOWN_MASK), XRayTransitionSet.L_FAMILY);
         getActionMap().put(XRayTransitionSet.L_FAMILY, new ToggleAction(XRayTransitionSet.L_FAMILY));
         im.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_DOWN_MASK), XRayTransitionSet.M_FAMILY);
         getActionMap().put(XRayTransitionSet.M_FAMILY, new ToggleAction(XRayTransitionSet.M_FAMILY));
         final String edges = "Edges";
         im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK), edges);
         getActionMap().put(edges, new ToggleAction(edges));
         final String escapes = "Si Escapes";
         im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK), escapes);
         getActionMap().put(escapes, new ToggleAction(escapes));

      }

      pb.add(new JScrollPane(jTree_Lines), cc0.xywh(5, 1, 1, 9));
   }

   private class ToggleAction extends AbstractAction {
      private static final long serialVersionUID = -8418977950838230807L;

      final private String mID;

      ToggleAction(String id) {
         mID = id;
      }

      @Override
      public void actionPerformed(ActionEvent e) {
         toggle(mID, null);
      }

   };

   private void toggle(String name, TreeNode cn) {
      if (cn == null) {
         final TreeModel model = jTree_Lines.getModel();
         cn = (CheckNode) model.getRoot();
      }
      for (int i = 0; i < cn.getChildCount(); ++i) {
         final TreeNode ch = cn.getChildAt(i);
         if (ch.toString().equals(name)) {
            final CheckNode chn = (CheckNode) ch;
            chn.setSelected(!chn.isSelected);
            return;
         }
         toggle(name, ch);
      }
   }

   public void clearAll() throws EPQException {
      setElement(Element.H);
      final KLMActionEvent kae = new KLMActionEvent(this, mSelected, KLMAction.REMOVE_LINES);
      fireVisibleLinesActionPerformed(kae);
      mSelected.clear();
   }

   public void setElement(Element elm) throws EPQException {
      final int start = mElementOrder[mElementIndex].equals(elm) ? mElementIndex + 1 : 0;
      int idx = 0;
      for (int i = 0; i < mElementOrder.length; ++i) {
         final int pos = (i + start) % mElementOrder.length;
         if (mElementOrder[pos].equals(elm)) {
            idx = pos;
            break;
         }
      }
      updateElement(idx);
   }

   public void updateElement(int idx) throws EPQException {
      final int newIdx = Math2.bound(idx, 0, mElementOrder.length);
      if (mElementIndex != newIdx) {
         mElementIndex = newIdx;
         final Element elm = mElementOrder[mElementIndex];
         if (mElementLabel != null)
            jLabel_Energy.setText(mElementLabel[mElementIndex]);
         else
            jLabel_Energy.setText(elm.toString());
         if (true) {
            final KLMActionEvent kae = new KLMActionEvent(this, mTemporary, KLMAction.REMOVE_LINES);
            fireTemporaryLinesActionPerformed(kae);
            mTemporary.clear();
         }
         jTextField_Element.setText(elm.toAbbrev());
         jScrollBar_Z.getModel().setValue(mElementIndex);
         final String[] kFam = new String[]{XRayTransitionSet.K_ALPHA, XRayTransitionSet.K_BETA};
         final String[] lFam = new String[]{XRayTransitionSet.L_ALPHA, XRayTransitionSet.L_BETA, XRayTransitionSet.L_GAMMA,
               XRayTransitionSet.L_OTHER};
         final String[] mFam = new String[]{XRayTransitionSet.M_ALPHA, XRayTransitionSet.M_BETA, XRayTransitionSet.M_GAMMA,
               XRayTransitionSet.M_OTHER};
         final String[] nFam = new String[]{XRayTransitionSet.N_FAMILY};
         final String[] fams = new String[]{XRayTransitionSet.K_FAMILY, XRayTransitionSet.L_FAMILY, XRayTransitionSet.M_FAMILY,
               XRayTransitionSet.N_FAMILY,};
         final String[][] lines = new String[][]{kFam, lFam, mFam, nFam};
         final CheckNode root = new CheckNode(elm.toAbbrev());
         root.addActionListener(this);
         CheckNode minor = null, esc = null;
         final TreeSet<AtomicShell> shells = new TreeSet<AtomicShell>();
         for (int i = 0; i < fams.length; ++i) {
            CheckNode fam = null, famMinor = null;
            final String[] names = lines[i];
            for (final String name : names) {
               final XRayTransitionSet xrts = new XRayTransitionSet(elm, name, mMinWeight);
               if (xrts.size() > 0) {
                  CheckNode dmtn = null, dmtn_m = null;
                  for (final XRayTransition xrt : xrts.getTransitions()) {
                     if (xrt.getEnergy() > mMaxEnergy)
                        continue;
                     shells.add(xrt.getDestination());
                     final KLMLine.Transition klmTr = new KLMLine.Transition(xrt);
                     final CheckNode xrtNode = new CheckNode(klmTr);
                     xrtNode.addActionListener(this);
                     if (xrt.getWeight(XRayTransition.NormalizeKLM) >= mMajorWeight) {
                        if (dmtn == null) {
                           dmtn = new CheckNode(name);
                           dmtn.addActionListener(this);
                           if (fam == null) {
                              fam = new CheckNode(fams[i]);
                              fam.addActionListener(this);
                              root.add(fam);
                           }
                           fam.add(dmtn);
                        }
                        dmtn.add(xrtNode);
                     } else {
                        if (dmtn_m == null) {
                           dmtn_m = new CheckNode(name);
                           dmtn_m.addActionListener(this);
                           if (famMinor == null) {
                              famMinor = new CheckNode(fams[i]);
                              famMinor.addActionListener(this);
                              if (minor == null) {
                                 minor = new CheckNode("Minor");
                                 minor.addActionListener(this);
                              }
                              minor.add(famMinor);
                           }
                           famMinor.add(dmtn_m);
                        }
                        dmtn_m.add(xrtNode);
                     }
                     final boolean trSel = mSelected.contains(klmTr);
                     xrtNode.setSelected(trSel);
                     if (!trSel)
                        mTemporary.add(klmTr);
                     if ((xrt.getWeight(XRayTransition.NormalizeKLM) >= mMinEscape)
                           && (xrt.getEnergy() > (ToSI.keV(0.02) + mEscapeTransition.getEnergy()))) {
                        if (esc == null) {
                           esc = new CheckNode(mEscapeTransition.getElement().toAbbrev() + " Escapes");
                           esc.addActionListener(this);
                        }
                        final KLMLine.EscapePeak klmEsc = new KLMLine.EscapePeak(xrt);
                        final CheckNode escNode = new CheckNode(klmEsc);
                        escNode.addActionListener(this);
                        esc.add(escNode);
                        final boolean escSel = mSelected.contains(klmEsc);
                        escNode.setSelected(escSel);
                     }
                  }
               }
            }
         }
         if (minor != null)
            root.add(minor);
         if (esc != null)
            root.add(esc);
         if (shells.size() > 0) {
            final CheckNode shellNode = new CheckNode("Edges");
            shellNode.addActionListener(this);
            for (final AtomicShell sh : shells)
               if (sh.exists()) {
                  final KLMLine.Edge klmEdge = new KLMLine.Edge(sh);
                  final CheckNode edgeNode = new CheckNode(klmEdge);
                  edgeNode.addActionListener(this);
                  shellNode.add(edgeNode);
                  final boolean edgeSel = mSelected.contains(klmEdge);
                  shellNode.setSelected(edgeSel);
                  // if(!edgeSel)
                  // mTemporary.add(klmEdge);
               }
            root.add(shellNode);
         }
         jTree_Lines.setModel(new DefaultTreeModel(root));
         jTree_Lines.setRootVisible(true);
         final KLMActionEvent kae = new KLMActionEvent(this, mTemporary, KLMAction.ADD_LINES);
         fireTemporaryLinesActionPerformed(kae);
      }
   }

   protected void fireTemporaryLinesActionPerformed(KLMActionEvent e) {
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

   public Set<KLMLine> getTemporaryLines() {
      assert mTemporary != null;
      return Collections.unmodifiableSet(mTemporary);
   }

   private void updateTreeValues(CheckNode cn, boolean isSelected) {
      if (cn.isLeaf()) {
         assert cn.getUserObject() instanceof KLMLine;
         final KLMLine klm = (KLMLine) cn.getUserObject();
         if (isSelected)
            mSelected.add(klm);
         else
            mSelected.remove(klm);
      } else {
         final Enumeration<?> children = cn.children();
         while (children.hasMoreElements()) {
            final CheckNode ccn = (CheckNode) children.nextElement();
            updateTreeValues(ccn, isSelected);
         }
      }
   }

   @Override
   public void actionPerformed(ActionEvent e) {
      final ArrayList<KLMLine> before = new ArrayList<KLMLine>(mSelected);
      final CheckNode cn = (CheckNode) e.getSource();
      updateTreeValues(cn, cn.isSelected);
      jTree_Lines.repaint(100);
      {
         final ArrayList<KLMLine> removed = new ArrayList<KLMLine>();
         for (final KLMLine inBefore : before)
            if (!mSelected.contains(inBefore))
               removed.add(inBefore);
         if (removed.size() > 0) {
            mTemporary.addAll(removed);
            {
               final KLMActionEvent kae = new KLMActionEvent(this, removed, KLMAction.REMOVE_LINES);
               fireVisibleLinesActionPerformed(kae);
            }
            {
               final KLMActionEvent kae = new KLMActionEvent(this, removed, KLMAction.ADD_LINES);
               fireTemporaryLinesActionPerformed(kae);
            }
         }
      }
      {
         final ArrayList<KLMLine> added = new ArrayList<KLMLine>();
         for (final KLMLine inAfter : mSelected)
            if (!before.contains(inAfter))
               added.add(inAfter);
         if (added.size() > 0) {
            mTemporary.removeAll(added);
            {
               final KLMActionEvent kae = new KLMActionEvent(this, added, KLMAction.REMOVE_LINES);
               fireTemporaryLinesActionPerformed(kae);
            }
            {
               final KLMActionEvent kae = new KLMActionEvent(this, added, KLMAction.ADD_LINES);
               fireVisibleLinesActionPerformed(kae);
            }
         }
      }
   }

   private void updateElementField() {
      try {
         setElement(Element.byName(jTextField_Element.getText()));
      } catch (final Exception jTextField1ReadError) {
         try {
            setElement(Element.byAtomicNumber(Integer.parseInt(jTextField_Element.getText())));
         } catch (final Exception jTextFieldReadError2) {
            jTextField_Element.setText(currentElement().toAbbrev());
         }
      }
   }

   public void setSession(Session session) {
      mSession = session;
   }
}

class NodeSelectionListener extends MouseAdapter {
   JTree tree;

   NodeSelectionListener(JTree tree) {
      this.tree = tree;
   }

   @Override
   public void mouseClicked(MouseEvent e) {
      final int x = e.getX();
      final int y = e.getY();
      final int row = tree.getRowForLocation(x, y);
      final TreePath path = tree.getPathForRow(row);
      if (path != null) {
         final CheckNode node = (CheckNode) path.getLastPathComponent();
         final boolean isSelected = !(node.isSelected());
         node.setSelected(isSelected);
         /*
          * if (node.getSelectionMode() == CheckNode.DIG_IN_SELECTION) { if (
          * isSelected) { tree.expandPath(path); } else {
          * tree.collapsePath(path); } }
          */
         ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
         // I need revalidate if node is root. but why?
         if (row == 0) {
            tree.revalidate();
            tree.repaint();
         }
      }
   }
}

class CheckRenderer extends JPanel implements TreeCellRenderer {

   private static final long serialVersionUID = -6073213455442044351L;

   protected JCheckBox check;

   protected TreeLabel label;

   public CheckRenderer() {
      setLayout(null);
      add(check = new JCheckBox());
      add(label = new TreeLabel());
      check.setBackground(UIManager.getColor("Tree.textBackground"));
      label.setForeground(UIManager.getColor("Tree.textForeground"));
   }

   @Override
   public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean expanded, boolean leaf, int row,
         boolean hasFocus) {
      final String stringValue = tree.convertValueToText(value, isSelected, expanded, leaf, row, hasFocus);
      setEnabled(tree.isEnabled());
      check.setSelected(((CheckNode) value).isSelected());
      label.setFont(tree.getFont());
      label.setText(stringValue);
      label.setSelected(isSelected);
      label.setFocus(hasFocus);
      label.setIcon(null);
      /*
       * if(leaf) { label.setIcon(UIManager.getIcon("Tree.leafIcon")); } else
       * if(expanded) { label.setIcon(UIManager.getIcon("Tree.openIcon")); }
       * else { label.setIcon(UIManager.getIcon("Tree.closedIcon")); }
       */
      return this;
   }

   @Override
   public Dimension getPreferredSize() {
      final Dimension d_check = check.getPreferredSize();
      final Dimension d_label = label.getPreferredSize();
      return new Dimension(d_check.width + d_label.width, (d_check.height < d_label.height ? d_label.height : d_check.height));
   }

   @Override
   public void doLayout() {
      final Dimension d_check = check.getPreferredSize();
      final Dimension d_label = label.getPreferredSize();
      int y_check = 0;
      int y_label = 0;
      if (d_check.height < d_label.height)
         y_check = (d_label.height - d_check.height) / 2;
      else
         y_label = (d_check.height - d_label.height) / 2;
      check.setLocation(0, y_check);
      check.setBounds(0, y_check, d_check.width, d_check.height);
      label.setLocation(d_check.width, y_label);
      label.setBounds(d_check.width, y_label, d_label.width, d_label.height);
   }

   @Override
   public void setBackground(Color color) {
      if (color instanceof ColorUIResource)
         color = null;
      super.setBackground(color);
   }

   public class TreeLabel extends JLabel {

      private static final long serialVersionUID = -5935367571393717722L;

      boolean isSelected;

      boolean hasFocus;

      public TreeLabel() {
      }

      @Override
      public void setBackground(Color color) {
         if (color instanceof ColorUIResource)
            color = null;
         super.setBackground(color);
      }

      @Override
      public void paint(Graphics g) {
         String str;
         if ((str = getText()) != null)
            if (0 < str.length()) {
               if (isSelected)
                  g.setColor(UIManager.getColor("Tree.selectionBackground"));
               else
                  g.setColor(UIManager.getColor("Tree.textBackground"));
               final Dimension d = getPreferredSize();
               int imageOffset = 0;
               final Icon currentI = getIcon();
               if (currentI != null)
                  imageOffset = currentI.getIconWidth() + Math.max(0, getIconTextGap() - 1);
               g.fillRect(imageOffset, 0, d.width - 1 - imageOffset, d.height);
               if (hasFocus) {
                  g.setColor(UIManager.getColor("Tree.selectionBorderColor"));
                  g.drawRect(imageOffset, 0, d.width - 1 - imageOffset, d.height - 1);
               }
            }
         super.paint(g);
      }

      @Override
      public Dimension getPreferredSize() {
         Dimension retDimension = super.getPreferredSize();
         if (retDimension != null)
            retDimension = new Dimension(retDimension.width + 3, retDimension.height);
         return retDimension;
      }

      public void setSelected(boolean isSelected) {
         this.isSelected = isSelected;
      }

      public void setFocus(boolean hasFocus) {
         this.hasFocus = hasFocus;
      }
   }
}

class CheckNode extends DefaultMutableTreeNode {

   private static final long serialVersionUID = 5217585093764028204L;

   public final static int SINGLE_SELECTION = 0;

   public final static int DIG_IN_SELECTION = 4;

   protected int selectionMode;

   protected boolean isSelected;

   ArrayList<ActionListener> mListeners = new ArrayList<ActionListener>();

   public CheckNode() {
      this(null);
   }

   public CheckNode(Object userObject) {
      this(userObject, true, false);
   }

   public CheckNode(Object userObject, boolean allowsChildren, boolean isSelected) {
      super(userObject, allowsChildren);
      this.isSelected = isSelected;
      setSelectionMode(DIG_IN_SELECTION);
   }

   public void setSelectionMode(int mode) {
      selectionMode = mode;
   }

   public int getSelectionMode() {
      return selectionMode;
   }

   public boolean setUpTree(boolean isSelected) {
      boolean res = false;
      if (children != null) {
         final Enumeration<?> e = children.elements();
         while (e.hasMoreElements()) {
            final CheckNode node = (CheckNode) e.nextElement();
            if (node.isSelected != isSelected) {
               res = true;
               node.isSelected = isSelected;
               node.setUpTree(isSelected);
            }
         }
      }
      return res;
   }

   public boolean setDownTree(boolean isSelected) {
      boolean res = false;
      if (parent instanceof CheckNode) {
         final CheckNode pn = (CheckNode) parent;
         final Enumeration<?> e = pn.children();
         boolean checkParent = e.hasMoreElements();
         while (e.hasMoreElements()) {
            final Object obj = e.nextElement();
            if (obj instanceof CheckNode)
               checkParent &= ((CheckNode) obj).isSelected;
         }
         if (pn.isSelected != checkParent) {
            pn.isSelected = checkParent;
            res = true;
            pn.setDownTree(checkParent);
         }
      }
      return res;
   }

   public void setSelected(boolean isSelected) {
      if (this.isSelected != isSelected) {
         this.isSelected = isSelected;
         if (selectionMode == DIG_IN_SELECTION) {
            setUpTree(isSelected);
            setDownTree(isSelected);
         }
         fireActionListener();
      }
   }

   public boolean isSelected() {
      return isSelected;
   }

   /**
    * If you want to change "isSelected" by CellEditor,
    * 
    * @param obj
    * @see javax.swing.tree.DefaultMutableTreeNode#setUserObject(java.lang.Object)
    */

   @Override
   public void setUserObject(Object obj) {
      if (obj instanceof Boolean)
         setSelected(((Boolean) obj).booleanValue());
      else
         super.setUserObject(obj);
   }

   public void addActionListener(ActionListener al) {
      if (!mListeners.contains(al))
         mListeners.add(al);
   }

   public void removeActionListener(ActionListener al) {
      if (mListeners.contains(al)) {
         final ArrayList<ActionListener> rep = new ArrayList<ActionListener>(mListeners);
         rep.remove(al);
         mListeners = rep;
      }
   }

   private void fireActionListener() {
      final ActionEvent ae = new ActionEvent(this, isSelected ? 1 : 0, "Selection changed", 0);
      final ArrayList<ActionListener> rep = mListeners;
      for (final ActionListener al : rep)
         al.actionPerformed(ae);
   }

}