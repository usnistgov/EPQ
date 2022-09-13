package gov.nist.nanoscalemetrology.JMONSELutils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * <p>
 * A List that guarantees that its iterator will traverse the set in ascending
 * element order, sorted according to the natural ordering of its elements (see
 * Comparable). As its name implies, it is similar to an ArrayList but without
 * the set(), add(int index,E element), and addAll(int index,Collection&lt;?
 * extends E&gt; c) methods, since these could disturb the natural ordering. All
 * elements inserted into a SortedArrayList must implement the Comparable
 * interface and be mutually comparable. Attempts to add an object violating
 * these restrictions will throw a ClassCastException. Duplicate elements are
 * allowed by default, but can optionally be prohibited.
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author John Villarrubia
 * @version 1.0
 */

public class SortedArrayList<E extends Comparable<? super E>>
   extends
   AbstractList<E> {

   private boolean duplicates = true;
   private ArrayList<E> alist;

   /**
    * Constructs an empty ArrayList with the specified initial capacity.
    * 
    * @param initialCapacity the initial capacity of the list.
    * @exception IllegalArgumentException if the specified initial capacity is
    *               negative
    */
   public SortedArrayList(int initialCapacity) {
      alist = new ArrayList<E>(initialCapacity);
   }

   /**
    * Constructs an empty ArrayList with an initial capacity of ten.
    */
   public SortedArrayList() {
      this(10);
   }

   /**
    * Constructs an ArrayList containing the elements of the specified
    * collection, as per the element's natural ordering. The
    * SortedArrayList instance has an initial capacity of 110% the size
    * of the specified collection.
    * 
    * @param c the collection whose elements are to be placed into this list.
    * @throws NullPointerException if the specified collection is null.
    */
   public SortedArrayList(Collection<? extends E> c) {
      // Allow 10% room for growth
      this((int) Math.min((c.size() * 110L) / 100, Integer.MAX_VALUE));
      this.addAll(c);
   }

   /**
    * Adds the specified element to this list as per the element's natural
    * ordering.
    * 
    * @param e element to be inserted into this list.
    * @return true (as per the general contract of Collection.add).
    * @throws ClassCastException if the specified element is not comparable to
    *            existing elements.
    */
   @Override
   public boolean add(E e) {
      if(e == null)
         return false;

      // Find the index where this element belongs according to the natural
      // ordering of the objects.
      final int index = Collections.binarySearch(this, e);

      if(index >= 0) {
         if(duplicates) {
            alist.add(index, e);
            return true;
         }
         return false;
      }
      alist.add(-index - 1, e);
      return true;
   }

   /**
    * Appends all of the elements in the specified Collection to this ArrayList,
    * as per their natural ordering. The behavior of this operation is undefined
    * if the specified Collection is modified while the operation is in
    * progress. (This implies that the behavior of this call is undefined if the
    * specified Collection is this ArrayList, and this ArrayList is nonempty.)
    * 
    * @param c the elements to be inserted into this ArrayList.
    * @return true if this ArrayList changed as a result of the call.
    * @throws NullPointerException if the specified collection is null.
    */
   @Override
   public boolean addAll(Collection<? extends E> c) {
      boolean changed = false;
      boolean added;

      for(final E e : c) {
         added = this.add(e);
         changed = changed || added;
      }
      return changed;
   }

   /**
    * Set duplicates allowed.
    * 
    * @param duplicates true if duplicates are to be allowed,
    *           false otherwise. If there are existing elements in the
    *           set when duplicates are disallowed, any duplicates are removed.
    */
   public void allowDuplicates(boolean duplicates) {
      this.duplicates = duplicates;
      for(int i = this.size() - 1; i > 0; i--)
         if(this.get(i).compareTo(this.get(i - 1)) == 0)
            this.remove(i);
   }

   /**
    * Query duplicates allowed.
    * 
    * @return true if duplicates are to be allowed, false
    *         otherwise.
    */
   public boolean duplicatesAllowed() {
      return this.duplicates;
   }

   @Override
   public E get(int index) {
      return alist.get(index);
   }

   @Override
   public int size() {
      return alist.size();
   }

   @Override
   public void clear() {
      alist.clear();
   }

   @Override
   public E remove(int index) {
      return alist.remove(index);
   }
}
