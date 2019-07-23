package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.KRatioSet;
import gov.nist.microanalysis.EPQLibrary.XRayTransitionSet;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * <p>
 * A class for entering and editing the k-ratio measured in an experiment.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */

public class KRatioCreator
   extends JDialog {
   private static final long serialVersionUID = 0x1;
   private Vector<Element> mElementList = new Vector<Element>();
   private Vector<String> DisplayList = new Vector<String>();
   private Map<Element, String> ElementToLineMap = new TreeMap<Element, String>();
   private Map<Element, Double> ElementToKRatioMap = new TreeMap<Element, Double>();
   private transient Vector<ActionListener> WindowClosingListeners;

   JPanel jPanel_Main = new JPanel();
   JScrollPane jScrollPane_Center = new JScrollPane();
   JList<String> jList_KRatios = new JList<String>();
   JButton jButton_Add = new JButton();
   JButton jButton_Remove = new JButton();
   JButton jButton_Clear = new JButton();
   JPanel jPanel_ButtonBox = new JPanel();
   JLabel jLabel_Element = new JLabel();
   JTextField jTextField_Element = new JTextField();
   JLabel jLabel_KRatio = new JLabel();
   JTextField jTextField_KRatio = new JTextField();
   JComboBox<String> jComboBox_Line = new JComboBox<String>();
   JLabel jLabel_Line = new JLabel();
   JButton jButton_Done = new JButton();
   JLabel jLabel_Spacer1 = new JLabel();

   public KRatioCreator(Frame frame, String title, boolean modal) {
      super(frame, title, modal);
      try {
         initialize();
         pack();
         jTextField_Element.grabFocus();
         jTextField_Element.selectAll();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   public KRatioCreator(Dialog dialog, String title, boolean modal) {
      super(dialog, title, modal);
      try {
         initialize();
         pack();
         jTextField_Element.grabFocus();
         jTextField_Element.selectAll();
      }
      catch(final Exception ex) {
         ex.printStackTrace();
      }
   }

   private void initialize()
         throws Exception {

      jButton_Add.setMnemonic('A');
      jButton_Add.setText("Add");
      jButton_Add.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_Add_actionPerformed(e);
         }
      });

      jButton_Remove.setMnemonic('R');
      jButton_Remove.setText("Remove");
      jButton_Remove.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_Remove_actionPerformed(e);
         }
      });

      jButton_Clear.setMnemonic('C');
      jButton_Clear.setText("Clear");
      jButton_Clear.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_Clear_actionPerformed(e);
         }
      });

      jButton_Done.setText("Ok");
      jButton_Done.addActionListener(new java.awt.event.ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            jButton_Done_actionPerformed(e);
         }
      });
      getRootPane().setDefaultButton(jButton_Done);

      jLabel_Element.setHorizontalAlignment(SwingConstants.RIGHT);
      jLabel_Element.setText("Element");

      jTextField_Element.setText("");
      jTextField_Element.addKeyListener(new java.awt.event.KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            jTextField_Element_keyPressed(e);
         }
      });
      jTextField_Element.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            jTextField_Element_focusGained(e);
         }
      });

      jLabel_KRatio.setText("K-Ratio");
      jLabel_KRatio.setHorizontalAlignment(SwingConstants.RIGHT);

      jTextField_KRatio.setText("1.0");
      jTextField_KRatio.addKeyListener(new java.awt.event.KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            jTextField_KRatio_keyPressed(e);
         }
      });
      jTextField_KRatio.addFocusListener(new java.awt.event.FocusAdapter() {
         @Override
         public void focusGained(FocusEvent e) {
            jTextField_KRatio_focusGained(e);
         }
      });

      jLabel_Line.setText("Line");
      jLabel_Line.setHorizontalAlignment(SwingConstants.RIGHT);

      {
         final String[] lines = {
            "K",
            "L",
            "M"
         };
         for(final String line : lines)
            jComboBox_Line.addItem(line);
      }
      jComboBox_Line.addKeyListener(new java.awt.event.KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            jComboBox_Line_keyPressed(e);
         }
      });

      jPanel_ButtonBox.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
      jPanel_ButtonBox.setLayout(new GridLayout(5, 3, 4, 4));
      // Row 1
      jPanel_ButtonBox.add(jLabel_Element, null);
      jPanel_ButtonBox.add(jTextField_Element, null);
      jPanel_ButtonBox.add(new JLabel());
      // Row 2
      jPanel_ButtonBox.add(jLabel_Line, null);
      jPanel_ButtonBox.add(jComboBox_Line, null);
      jPanel_ButtonBox.add(new JLabel());
      // Row 3
      jPanel_ButtonBox.add(jLabel_KRatio, null);
      jPanel_ButtonBox.add(jTextField_KRatio, null);
      jPanel_ButtonBox.add(new JLabel());
      // Row 4
      jPanel_ButtonBox.add(jButton_Add, null);
      jPanel_ButtonBox.add(jButton_Remove, null);
      jPanel_ButtonBox.add(jButton_Clear, null);
      // Row 5
      jPanel_ButtonBox.add(new JLabel(), null);
      jPanel_ButtonBox.add(new JLabel(), null);
      jPanel_ButtonBox.add(jButton_Done, null);

      jList_KRatios.setForeground(SystemColor.textText);
      jList_KRatios.setListData(new String[] {
         "Your K-Ratios will appear here"
      });

      jScrollPane_Center.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
      jScrollPane_Center.getViewport().add(jList_KRatios, null);

      jPanel_Main.setLayout(new BorderLayout());
      jPanel_Main.add(jScrollPane_Center, BorderLayout.CENTER);
      jPanel_Main.add(jPanel_ButtonBox, BorderLayout.PAGE_END);

      getContentPane().add(jPanel_Main);
   }

   void jButton_Add_actionPerformed(ActionEvent e) {
      final Element elm = Element.byName(jTextField_Element.getText());
      if((!elm.equals(Element.None)) && (!mElementList.contains(elm)))
         try {
            final NumberFormat nf = NumberFormat.getInstance();
            final double KRatio = nf.parse(jTextField_KRatio.getText()).doubleValue();
            if(KRatio < 0.0)
               throw new NumberFormatException();
            final String Line = jComboBox_Line.getSelectedItem().toString();

            ElementToLineMap.put(elm, Line);

            mElementList.add(elm);
            ElementToKRatioMap.put(elm, Double.valueOf(KRatio));
            DisplayList.add(elm.toAbbrev() + " : " + KRatio + " (" + Line + "-line)");
            jList_KRatios.setListData(DisplayList);
            jTextField_Element.grabFocus();
            jTextField_Element.selectAll();
         }
         catch(final ParseException nfex) {
            jTextField_KRatio.grabFocus();
            jTextField_KRatio.selectAll();
         }
      else {
         jTextField_Element.grabFocus();
         jTextField_Element.selectAll();
      }
   }

   void jButton_Remove_actionPerformed(ActionEvent e) {
      final int Selected = jList_KRatios.getSelectedIndex();
      DisplayList.remove(Selected);
      ElementToKRatioMap.remove(mElementList.get(Selected));
      ElementToLineMap.remove(mElementList.get(Selected));
      mElementList.remove(Selected);
      jList_KRatios.setListData(DisplayList);
   }

   void jButton_Clear_actionPerformed(ActionEvent e) {
      jList_KRatios.setListData(new String[] {
         "Your K-Ratios will appear here"
      });
      DisplayList = new Vector<String>();
      mElementList = new Vector<Element>();
      ElementToKRatioMap = new HashMap<Element, Double>();
      ElementToLineMap = new HashMap<Element, String>();
   }

   void jTextField_Element_focusGained(FocusEvent e) {
      jTextField_Element.selectAll();
   }

   void jTextField_Element_keyPressed(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_ENTER)
         jButton_Add_actionPerformed(new ActionEvent(e.getSource(), e.getID(), e.toString()));
   }

   void jTextField_KRatio_focusGained(FocusEvent e) {
      jTextField_KRatio.selectAll();
   }

   void jTextField_KRatio_keyPressed(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_ENTER)
         jButton_Add_actionPerformed(new ActionEvent(e.getSource(), e.getID(), e.toString()));
   }

   protected void fireWindowClosingEvent(ActionEvent e) {
      if(WindowClosingListeners != null)
         for(final ActionListener al : WindowClosingListeners)
            al.actionPerformed(e);
   }

   /**
    * removeWindowClosingListener - removes the window closing listener
    * 
    * @param l ActionListener
    */
   public synchronized void removeWindowClosingListener(ActionListener l) {
      if((WindowClosingListeners != null) && WindowClosingListeners.contains(l)) {
         final Vector<ActionListener> v = new Vector<ActionListener>(WindowClosingListeners);
         v.removeElement(l);
         WindowClosingListeners = v;
      }
   }

   /**
    * addWindowClosingListener - Adds a window closing listener. When the dialog
    * closes, an event will be triggered so that all listening components can
    * respond appropriately.
    * 
    * @param l ActionListener
    */
   public synchronized void addWindowClosingListener(ActionListener l) {
      final Vector<ActionListener> v = WindowClosingListeners == null ? new Vector<ActionListener>(2)
            : new Vector<ActionListener>(WindowClosingListeners);
      if(!v.contains(l)) {
         v.addElement(l);
         WindowClosingListeners = v;
      }
   }

   public Vector<Element> getElements() {
      return mElementList;
   }

   public KRatioSet getKRatioSet() {
      final KRatioSet KRatios = new KRatioSet();
      for(int index = 0; index < mElementList.size(); index++) {
         final Element elm = mElementList.get(index);
         final double KRatio = ElementToKRatioMap.get(elm).doubleValue();
         final String linestr = ElementToLineMap.get(elm).toString();

         int tempShell = AtomicShell.NoShell;
         if(linestr.compareTo("K") == 0)
            tempShell = AtomicShell.K;
         else if(linestr.compareTo("L") == 0)
            tempShell = AtomicShell.LIII;
         else if(linestr.compareTo("M") == 0)
            tempShell = AtomicShell.MV;
         final AtomicShell shell = new AtomicShell(elm, tempShell);
         KRatios.addKRatio(new XRayTransitionSet(shell), KRatio, 0.0);
      }
      return KRatios;
   }

   void jButton_Done_actionPerformed(ActionEvent e) {
      fireWindowClosingEvent(e);
   }

   void jComboBox_Line_keyPressed(KeyEvent e) {
      if(e.getKeyCode() == KeyEvent.VK_ENTER)
         jButton_Add_actionPerformed(new ActionEvent(e.getSource(), e.getID(), e.toString()));
   }

   public void setKRatios(KRatioSet KRatios, Map<Element, String> ElementToLine) {
      ElementToLineMap = ElementToLine;
      ElementToKRatioMap = new TreeMap<Element, Double>();
      mElementList = new Vector<Element>();
      DisplayList = new Vector<String>();

      for(final Element elm : KRatios.getElementSet()) {
         mElementList.add(elm);
         final int Family = AtomicShell.parseFamilyName(ElementToLineMap.get(elm).toString());
         AtomicShell shell = null;
         if(Family == AtomicShell.KFamily)
            shell = new AtomicShell(elm, AtomicShell.K);
         else if(Family == AtomicShell.LFamily)
            shell = new AtomicShell(elm, AtomicShell.LI);
         else if(Family == AtomicShell.MFamily)
            shell = new AtomicShell(elm, AtomicShell.MI);

         if(shell != null) {
            double KRatio = 0.0;
            KRatio = KRatios.getKRatio(new XRayTransitionSet(shell));
            ElementToKRatioMap.put(elm, Double.valueOf(KRatio));
            DisplayList.add(elm.toAbbrev() + " : " + KRatio + " (" + AtomicShell.getFamilyName(shell.getFamily()) + "-line)");
            jList_KRatios.setListData(DisplayList);
         }
         jTextField_Element.grabFocus();
         jTextField_Element.selectAll();
      }
   }
}
