/**
 * gov.nist.microanalysis.EPQTools.JElementPanel Created by: nritchie Date: Sep
 * 4, 2015
 */
package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.EPQLibrary.Element;

import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

/**
 * <p>
 * Description
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
public class JElementPanel extends JPanel {
   private static final long serialVersionUID = -1527706801448720328L;

   private final JTextField jTextField_Elements = new JTextField();
   private final JButton jButton_Edit = new JButton("...");

   private final Set<Element> mElements = new TreeSet<Element>();
   private final Set<Element> mSelected = new TreeSet<Element>();

   private final ArrayList<ActionListener> mElementChange = new ArrayList<ActionListener>();

   private void updateElementField() {
      StringBuffer sb = new StringBuffer();
      boolean first = true;
      for (Element elm : mSelected) {
         if (!first)
            sb.append(", ");
         first = false;
         sb.append(elm.toAbbrev());
      }
      jTextField_Elements.setText(sb.toString());
   }

   public void setAvailableElements(Collection<Element> elms) {
      mElements.clear();
      mElements.addAll(elms);
      Set<Element> sel = new TreeSet<Element>();
      for (Element elm : mSelected)
         if (mElements.contains(elm))
            sel.add(elm);
      mSelected.clear();
      mSelected.addAll(sel);
      updateElementField();
   }

   private Window getParentWindow() {
      Container c = getParent();
      while (c != null) {
         if (c instanceof Window)
            break;
         c = c.getParent();
      }
      return (Window) c;
   }

   private Element idElement(String val) {
      Locale locale = Locale.getDefault();
      for (Element elm : mElements) {
         if (elm.toAbbrev().equals(val))
            return elm;
         if (val.toLowerCase(locale).equals(elm.toString().toLowerCase(locale)))
            return elm;
         try {
            if (val.matches("\\d+"))
               if (Integer.parseInt(val) == elm.getAtomicNumber())
                  return elm;
         } catch (NumberFormatException e) {
            // Ignore
         }
      }
      return null;

   }

   private Set<Element> parseElementField() {
      Set<Element> res = new TreeSet<Element>();
      String str = jTextField_Elements.getText();
      String[] vals = str.split("[,\\h]");
      for (String val : vals) {
         Element elm = idElement(val);
         if (elm != null)
            res.add(elm);
      }
      return res;
   }

   @Override
   public void setToolTipText(String ttt) {
      super.setToolTipText(ttt);
      jTextField_Elements.setToolTipText(ttt);
      jButton_Edit.setToolTipText(ttt);
   }

   private void init() {
      jTextField_Elements.addFocusListener(new FocusAdapter() {
         @Override
         public void focusLost(FocusEvent e) {
            mSelected.clear();
            mSelected.addAll(parseElementField());
            updateElementField();
            fireElementChange();
         }
      });
      jButton_Edit.addActionListener(new ActionListener() {

         @Override
         public void actionPerformed(ActionEvent arg0) {
            setSelected(SelectElements.selectElements(getParentWindow(), "Select elements", mElements, mSelected));
            fireElementChange();
         }
      });
      setLayout(new FormLayout("fill:pref:grow, 5dlu, pref", "pref"));
      add(jTextField_Elements, CC.xy(1, 1));
      add(jButton_Edit, CC.xy(3, 1));
   }

   public void addElementChange(ActionListener al) {
      mElementChange.add(al);
   }

   public void removeElementChange(ActionListener al) {
      mElementChange.remove(al);
   }

   public void clearElementChange() {
      mElementChange.clear();
   }

   private void fireElementChange() {
      ActionEvent ae = new ActionEvent(JElementPanel.this, 0, "ElementChange", System.currentTimeMillis(), 0);
      ArrayList<ActionListener> dup = new ArrayList<ActionListener>(mElementChange);
      for (ActionListener al : dup)
         al.actionPerformed(ae);
   }

   /**
    * Constructs a JElementPanel
    */
   public JElementPanel() {
      mElements.addAll(Element.allElements());
      init();
   }

   /**
    * Constructs a JElementPanel
    */
   public JElementPanel(Collection<Element> elms) {
      mElements.addAll(elms);
      init();
   }

   public void setSelected(Collection<Element> elms) {
      mSelected.clear();
      mSelected.addAll(elms);
      updateElementField();
   }

   public Set<Element> getSelected() {
      return new TreeSet<Element>(mSelected);
   }

}
