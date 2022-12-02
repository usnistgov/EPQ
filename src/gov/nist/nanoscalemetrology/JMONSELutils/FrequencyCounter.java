/**
 * gov.nist.nanoscalemetrology.JMONSELutils.FrequencyCounter Created by: jvillar
 * Date: Sep 28, 2010
 */
package gov.nist.nanoscalemetrology.JMONSELutils;

/**
 * <p>
 * A class to facilitate counting objects and sorting them based on their
 * frequency. The counter has an ID (the object) and a count, which stores the
 * number of multiples or instances of the object that have been counted.
 * Increment, decrement, and setCount methods allow the count to be changed. The
 * class implements Comparable to facilitate sorting. The natural order is
 * defined to be *decreasing* with higher frequency, so Arrays.sort() applied to
 * an array of FrequencyCounter[] will sort the array from high to low count.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author John Villarrubia
 * @version 1.0
 */
public class FrequencyCounter implements Comparable<FrequencyCounter> {

   private final Object id; // The object to be counted
   private int count; // Current count value

   /**
    * Constructs a FrequencyCounter with initial count
    * 
    * @param id
    * @param initialCount
    */
   public FrequencyCounter(Object id, int initialCount) {
      this.id = id;
      count = initialCount;
   }

   /**
    * Constructs a FrequencyCounter with default initial count = 0
    * 
    * @param id
    */
   public FrequencyCounter(Object id) {
      this(id, 0);
   }

   /**
    * Increment the count by 1.
    */
   public void increment() {
      count++;
   }

   /**
    * Decrement the count by 1
    */
   public void decrement() {
      count--;
   }

   /**
    * Set the count
    * 
    * @param count
    */
   public void setCount(int count) {
      this.count = count;
   }

   /**
    * Returns the object being counted.
    * 
    * @return
    */
   public Object id() {
      return id;
   }

   /**
    * Returns the current count for this FrequencyCounter.
    * 
    * @return
    */
   public int count() {
      return count;
   }

   @Override
   public int compareTo(FrequencyCounter b) {
      if (count > b.count)
         return -1;
      else if (count < b.count)
         return 1;
      return 0;
   }

}
