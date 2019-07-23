package gov.nist.microanalysis.Utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import gov.nist.microanalysis.EPQLibrary.Element;

/**
 * A JComboBox for selecting an element by either abbreviaton (H, He, Li,...) or
 * full name (Hydrogen, Helium, Lithium, ..)
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
public class ElementComboBoxModel
   implements ComboBoxModel<Element> {

   private boolean mUseAbbreviations = false;

   private class AbbrevElement
      implements Comparable<AbbrevElement> {
      private final Element mElement;

      AbbrevElement(Element elm) {
         mElement = elm;
      }

      @Override
      public String toString() {
         return mUseAbbreviations ? mElement.toAbbrev() : mElement.toString();
      }

      @Override
      public int hashCode() {
         return mElement.hashCode();
      }

      @Override
      public int compareTo(AbbrevElement ae) {
         return mElement.compareTo(ae.mElement);
      }
   }

   private final ArrayList<AbbrevElement> mContents = new ArrayList<AbbrevElement>();
   private final Set<ListDataListener> mListeners = new HashSet<ListDataListener>();
   private int mSelected = -1;

   private int getIndexOf(Object obj) {
      Element elm = null;
      if(obj instanceof Element)
         elm = (Element) obj;
      else if(obj instanceof String)
         elm = Element.byName((String) obj);
      if(elm != null)
         for(int i = 0; i < getSize(); ++i)
            if(elm.equals(mContents.get(i).mElement))
               return i;
      return -1;
   }

   /**
    * Constructs an ElementComboBoxModel object
    */
   public ElementComboBoxModel() {
      this(Element.allElements());
   }

   public ElementComboBoxModel(Collection<Element> elms) {
      mUseAbbreviations = false;
      include(elms);
   }

   // For ComboBoxModel
   @Override
   public void setSelectedItem(Object anObject) {
      mSelected = getIndexOf(anObject);
   }

   // For ComboBoxModel
   @Override
   public Object getSelectedItem() {
      return (mSelected >= 0) && (mSelected <= mContents.size()) ? mContents.get(mSelected).mElement : null;
   }

   // For ListModel
   @Override
   public void addListDataListener(ListDataListener l) {
      mListeners.add(l);
   }

   // For ListModel
   @Override
   public Element getElementAt(int index) {
      return mContents.get(index).mElement;
   }

   // For ListModel
   @Override
   public int getSize() {
      return mContents.size();
   }

   // For ListModel
   @Override
   public void removeListDataListener(ListDataListener l) {
      mListeners.remove(l);
   }

   public void include(Collection<Element> elms) {
      mContents.clear();
      for(final Element elm : elms)
         mContents.add(new AbbrevElement(elm));
   }

   public void allBut(Collection<Element> elms) {
      final List<Element> all = new ArrayList<Element>(Element.allElements());
      all.removeAll(elms);
      include(all);
   }

   /**
    * Should the list box display abbreviations?
    * 
    * @return Returns the useAbbreviations.
    */
   public boolean useAbbreviations() {
      return mUseAbbreviations;
   }

   /**
    * Sets whether the list box should display abbreviations
    * 
    * @param useAbbreviations boolean
    */
   public void setUseAbbreviations(boolean useAbbreviations) {
      if(mUseAbbreviations != useAbbreviations) {
         mUseAbbreviations = useAbbreviations;
         for(final ListDataListener ldl : mListeners)
            ldl.contentsChanged(new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, mContents.size() - 1));
      }
   }
}
