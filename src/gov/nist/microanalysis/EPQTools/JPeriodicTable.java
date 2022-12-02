package gov.nist.microanalysis.EPQTools;

import gov.nist.microanalysis.EPQLibrary.Element;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Collection;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * <p>
 * A periodic table control for selecting one or more elements.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class JPeriodicTable extends JComponent {
   private static final long serialVersionUID = 0x1;
   private Dimension mButtonDim;
   private static int END_OF_ELEMENTS = Element.elmLr + 1;
   private static final int COL_COUNT = 18;
   private static final int ROW_COUNT = 9;

   private final boolean[] mState = new boolean[END_OF_ELEMENTS];
   private final boolean[] mDisabled = new boolean[END_OF_ELEMENTS];
   private int mDepressedElement;
   private int mDisplayedElement;
   private int mSelectedElement; // The last element selected

   private static final int[] mRow = {-1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
         4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5,
         5, 5, 5, 6, 6, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8};
   private static final int[] mCol = {-1, 0, 17, 0, 1, 12, 13, 14, 15, 16, 17, 0, 1, 12, 13, 14, 15, 16, 17, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
         13, 14, 15, 16, 17, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
         3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17};

   private transient Vector<ActionListener> actionListeners = new Vector<ActionListener>();

   protected Rectangle getButtonRect(int elm) {
      final int h = mButtonDim.height;
      final int w = mButtonDim.width;
      final int r = (h * mRow[elm]) + ((getHeight() - (ROW_COUNT * h)) / 2);
      final int c = (w * mCol[elm]) + ((getWidth() - (COL_COUNT * w)) / 2);
      return new Rectangle(c, r, w - 2, h - 2);
   }

   private int whichElement(int x, int y) {
      for (int e = Element.elmH; e < END_OF_ELEMENTS; ++e)
         if (getButtonRect(e).contains(x, y))
            return e;
      return Element.elmNone;
   }

   private Rectangle getElementDisplayRect() {
      return new Rectangle(4 * mButtonDim.width, (mButtonDim.height / 2), 6 * mButtonDim.width, 2 * mButtonDim.height);
   }

   protected void displayPopup(int x, int y) {
      final JPopupMenu pm = new JPopupMenu("Periodic table menu");
      {
         final JMenuItem mi = new JMenuItem("Clear all");
         mi.addActionListener(new AbstractAction() {
            static final long serialVersionUID = 0x1;

            @Override
            public void actionPerformed(ActionEvent e) {
               setAll(false);
            }
         });
         pm.add(mi);
      }
      {
         final JMenuItem mi = new JMenuItem("Select all");
         mi.addActionListener(new AbstractAction() {
            static final long serialVersionUID = 0x1;

            @Override
            public void actionPerformed(ActionEvent e) {
               setAll(true);
            }
         });
         pm.add(mi);
      }
      this.add(pm);
      pm.show(this, x, y);
   }

   protected void fireSelectionEvent(int element) {
      mSelectedElement = element;
      fireActionPerformed(new ActionEvent(this, element, getSelection(element) ? "Selected" : "Deselected"));
   }

   /**
    * fireActionPerformed - This control fires an event each time an element is
    * selected or deselected.
    * 
    * @param e
    *           ActionEvent
    */
   protected void fireActionPerformed(ActionEvent e) {
      if (actionListeners.size() > 0)
         for (final Object element : actionListeners)
            ((ActionListener) element).actionPerformed(e);
   }

   /**
    * getLastSelected - Get the last element that was selected or deselected.
    * 
    * @return int
    */
   public int getLastSelected() {
      return mSelectedElement;
   }

   /**
    * JPeriodicTable - Construct a JPeriodicTable object.
    */
   public JPeriodicTable() {
      super();
      assert (mRow.length == END_OF_ELEMENTS);
      assert (mCol.length == END_OF_ELEMENTS);
      try {
         jbInit();
      } catch (final Exception ex) {
         ex.printStackTrace();
      }
   }

   /**
    * getSelection - Is the specified element selected in the table?
    * 
    * @param element
    *           int - The atomic number of the element.
    * @return boolean - True if selected, false otherwise.
    */
   public boolean getSelection(int element) {
      return ((element > Element.elmNone) && (element < END_OF_ELEMENTS)) ? mState[element] && (!mDisabled[element]) : false;
   }

   /**
    * setSelection - Set the selection state of the specified element.
    * 
    * @param z
    *           int - The atomic number of the element
    * @param b
    *           boolean - True to select, false to unselect.
    */
   public void setSelection(int z, boolean b) {
      if ((z > Element.elmNone) && (z < END_OF_ELEMENTS) && (mState[z] != b)) {
         mState[z] = b;
         repaint(mButtonDim.width * mCol[z], mButtonDim.height * mRow[z], mButtonDim.width, mButtonDim.height);
      }
   }

   /**
    * setSelection - Set the selection state of the specified element.
    * 
    * @param elm
    *           Element - The element to select
    * @param b
    *           boolean - True to select, false to unselect.
    */
   public void setSelection(Element elm, boolean b) {
      setSelection(elm.getAtomicNumber(), b);
   }

   void jbInit() throws Exception {
      this.setOpaque(true);
      this.setSize(25 * COL_COUNT, 20 * ROW_COUNT);
      this.setPreferredSize(new Dimension(25 * COL_COUNT, 20 * ROW_COUNT));
      this.setMaximumSize(new Dimension(50 * COL_COUNT, 40 * ROW_COUNT));
      this.setMinimumSize(new Dimension(20 * COL_COUNT, 15 * ROW_COUNT));
      this.setToolTipText("Select one or more elements from the periodic table.");
      this.setDoubleBuffered(true);
      this.setOpaque(false);
      this.setLayout(null);
      mButtonDim = new Dimension(getWidth() / COL_COUNT, getHeight() / ROW_COUNT);
      mDepressedElement = Element.elmNone;
      mDisplayedElement = Element.elmNone;

      addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent me) {
            if (me.isPopupTrigger())
               displayPopup(me.getX(), me.getY());
            else if (me.getButton() == MouseEvent.BUTTON1) {
               int el = whichElement(me.getX(), me.getY());
               if (mDisabled[el])
                  el = Element.elmNone;
               mDepressedElement = el;
               if (mDepressedElement != Element.elmNone)
                  repaint(getButtonRect(mDepressedElement));
            }
         }

         @Override
         public void mouseReleased(MouseEvent me) {
            if (me.isPopupTrigger())
               displayPopup(me.getX(), me.getY());
            else if (me.getButton() == MouseEvent.BUTTON1)
               if ((mDepressedElement != Element.elmNone) && (!mDisabled[mDepressedElement])) {
                  if (whichElement(me.getX(), me.getY()) == mDepressedElement) {
                     mState[mDepressedElement] = !mState[mDepressedElement];
                     fireSelectionEvent(mDepressedElement);
                  }
                  final int e = mDepressedElement;
                  mDepressedElement = Element.elmNone;
                  repaint(getButtonRect(e));
               }
         }
      });
      addMouseMotionListener(new MouseMotionAdapter() {
         @Override
         public void mouseMoved(MouseEvent me) {
            final int el = whichElement(me.getX(), me.getY());
            if (el != mDisplayedElement) {
               mDisplayedElement = el;
               repaint(getElementDisplayRect());
            }
         }
      });
   }

   @Override
   public void paintComponent(Graphics gr) {
      super.paintComponent(gr);
      final Insets in = getInsets();
      mButtonDim.setSize((getWidth() - (2 * (in.left + in.right))) / COL_COUNT, (getHeight() - (2 * (in.top + in.bottom))) / ROW_COUNT);
      FontMetrics fm = gr.getFontMetrics();
      final int h = mButtonDim.height;
      final int w = mButtonDim.width;
      final int dh = (getHeight() - (ROW_COUNT * h)) / 2;
      final int dw = (getWidth() - (COL_COUNT * w)) / 2;
      for (int e = Element.elmH; e < END_OF_ELEMENTS; ++e) {
         int r = (h * mRow[e]) + dh;
         int c = (w * mCol[e]) + dw;
         if (e == mDepressedElement) {
            ++r;
            ++c;
         }
         if (mDisabled[e]) {
            gr.setColor(SystemColor.control);
            gr.fillRoundRect(c, r, w - 2, h - 2, w / 10, w / 10);
            gr.setColor(SystemColor.controlShadow);
            gr.drawRoundRect(c, r, w - 2, h - 2, w / 10, w / 10);
         } else if (mState[e]) {
            gr.setColor(Color.green);
            gr.fillRoundRect(c, r, w - 2, h - 2, w / 10, w / 10);
            gr.setColor(Color.black);
            gr.drawRoundRect(c, r, w - 2, h - 2, w / 10, w / 10);
         } else {
            gr.setColor(SystemColor.control);
            gr.fillRoundRect(c, r, w - 2, h - 2, w / 10, w / 10);
            gr.setColor(SystemColor.controlText);
            gr.drawRoundRect(c, r, w - 2, h - 2, w / 10, w / 10);
         }
         final String str = Element.toAbbrev(e);
         if (mDisabled[e])
            gr.setColor(SystemColor.controlShadow);
         gr.drawString(str, c + ((w - fm.stringWidth(str)) / 2), r + ((2 * h) / 3));
         if (mDisabled[e])
            gr.setColor(this.getForeground());
      }

      final Rectangle r = getElementDisplayRect();
      gr.setColor(SystemColor.control);
      gr.drawRect(r.x, r.y, r.width, r.height);
      if (mDisplayedElement != Element.elmNone) {
         gr.setColor(SystemColor.controlText);
         if (mDisabled[mDisplayedElement])
            gr.drawString("disabled", (4 * mButtonDim.width) + 2, ((5 * mButtonDim.height) / 2) - 2);
         final Font oldFont = gr.getFont();
         gr.setFont(new Font(oldFont.getName(), oldFont.getStyle(), (3 * oldFont.getSize()) / 2));
         fm = gr.getFontMetrics();
         final String str = Element.toString(mDisplayedElement);
         gr.drawString(str, r.x + ((r.width - fm.stringWidth(str)) / 2), (r.y + r.height) - fm.getDescent());
         gr.drawString(Integer.toString(mDisplayedElement), r.x, r.y + fm.getAscent());

      }
   }

   /**
    * getSelectedElements - Get a Set object containing a list of selected
    * element.
    * 
    * @return TreeSet&gt;Element&lt; - a set of Element objects
    */
   public TreeSet<Element> getSelected() {
      final TreeSet<Element> res = new TreeSet<Element>();
      for (int el = Element.elmH; el < END_OF_ELEMENTS; ++el)
         if (mState[el] && (!mDisabled[el]))
            res.add(Element.byAtomicNumber(el));
      return res;
   }

   /**
    * setSelectedElements - Sets the elements that are selected on the periodic
    * table control from the list of Element objects in lst.
    * 
    * @param lst
    *           A collection of Element objects.
    * @param clear
    *           boolean - Determines whether to clear the table before setting
    *           the specified elements.
    */
   public void setSelected(Collection<Element> lst, boolean clear) {
      if (clear)
         setAll(false);
      for (final Element el : lst)
         setSelection(el.getAtomicNumber(), true);
   }

   /**
    * setAll - Select or deselect all elements.
    * 
    * @param set
    *           boolean - True to set and false to deselect.
    */
   public void setAll(boolean set) {
      setAllExcept(set, Element.elmNone);
   }

   /**
    * setAll - Select or deselect all elements.
    * 
    * @param set
    *           boolean - True to set and false to deselect.
    * @param element
    *           int - The atomic number of the element whose state to leave
    *           unchanged
    */
   public void setAllExcept(boolean set, int element) {
      for (int el = Element.elmH; el < END_OF_ELEMENTS; ++el)
         if ((el != element) && (mState[el] != set) && (!mDisabled[el])) {
            mState[el] = set;
            repaint(getButtonRect(el));
         }
   }

   /**
    * removeActionListener - This control fires an event each time an element is
    * selected or deselected.
    * 
    * @param l
    *           ActionListener
    */
   public synchronized void removeActionListener(ActionListener l) {
      if (actionListeners.contains(l)) {
         final Vector<ActionListener> v = new Vector<ActionListener>(actionListeners);
         v.removeElement(l);
         actionListeners = v;
      }
   }

   /**
    * addActionListener - This control fires an event each time an element is
    * selected or deselected.
    * 
    * @param l
    *           ActionListener
    */
   public synchronized void addActionListener(ActionListener l) {
      if (!actionListeners.contains(l)) {
         final Vector<ActionListener> v = new Vector<ActionListener>(actionListeners);
         v.addElement(l);
         actionListeners = v;
      }
   }

   /**
    * setEnabled - Set the enabled state for the button assocaited with the
    * specified element.
    * 
    * @param element
    *           int
    * @param enabled
    *           boolean
    */
   public void setEnabled(Element element, boolean enabled) {
      setEnabled(element.getAtomicNumber(), enabled);
   }

   private void setEnabled(final int z, boolean enabled) {
      if ((z > Element.elmNone) && (z < END_OF_ELEMENTS) && (mDisabled[z] == enabled)) {
         mDisabled[z] = !enabled;
         repaint(getButtonRect(z));
         if (!enabled)
            mState[z] = false;
      }
   }

   /**
    * enableAll - Enables (or disables) all buttons.
    * 
    * @param enabled
    *           boolean
    */
   public void enableAll(boolean enabled) {
      for (int z = Element.elmH; z < END_OF_ELEMENTS; ++z)
         setEnabled(z, enabled);
   }

   /**
    * Enable/disable all elements from begin to end (excluding end)
    * 
    * @param begin
    *           The beginning
    * @param end
    *           One beyond the end
    * @param enabled
    */
   public void enableRange(Element begin, Element end, boolean enabled) {
      for (int z = begin.getAtomicNumber(); z < end.getAtomicNumber(); ++z)
         setEnabled(z, enabled);
   }
}
