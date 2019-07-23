/**
 * gov.nist.microanalysis.Utility.Pair Created by: nritchie Date: Sep 11, 2014
 */
package gov.nist.microanalysis.Utility;

import java.util.Comparator;

/**
 * <p>
 * A utility class to hold a pair of objects.
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
public class Pair<A, B> {

   public final A first;
   public final B second;

   /**
    * Constructs a Pair
    */
   public Pair(A first, B second) {
      this.first = first;
      this.second = second;
   }

   /**
    * Checks the two objects for equality by delegating to their respective
    * {@link Object#equals(Object)} methods.
    *
    * @param o the {@link Pair} to which this one is to be checked for equality
    * @return true if the underlying objects of the Pair are both considered
    *         equal
    */
   @Override
   public boolean equals(Object o) {
      if(this == o)
         return true;
      if(o == null || getClass() != o.getClass())
         return false;
      Pair<?, ?> pair = (Pair<?, ?>) o;
      if(first != null ? !first.equals(pair.first) : pair.first != null)
         return false;
      if(second != null ? !second.equals(pair.second) : pair.second != null)
         return false;

      return true;
   }

   /**
    * Compute a hash code using the hash codes of the underlying objects
    *
    * @return a hashcode of the Pair
    */
   @Override
   public int hashCode() {
      return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode());
   }

   /**
    * Convenience method for creating an appropriately typed pair.
    * 
    * @param a the first object in the Pair
    * @param b the second object in the pair
    * @return a Pair that is templatized with the types of a and b
    */
   public static <A, B> Pair<A, B> create(A a, B b) {
      return new Pair<A, B>(a, b);
   }

   public static <A extends Comparator<A>, B> Comparator<Pair<A, B>> compareA() {
      Comparator<Pair<A, B>> res = new Comparator<Pair<A, B>>() {
         @Override
         public int compare(Pair<A, B> arg0, Pair<A, B> arg1) {
            return arg0.first.compare(arg0.first, arg1.first);
         }
      };
      return res;
   }

   public static <A, B extends Comparator<B>> Comparator<Pair<A, B>> compareB() {
      Comparator<Pair<A, B>> res = new Comparator<Pair<A, B>>() {
         @Override
         public int compare(Pair<A, B> arg0, Pair<A, B> arg1) {
            return arg0.second.compare(arg0.second, arg1.second);
         }
      };
      return res;
   }
}
