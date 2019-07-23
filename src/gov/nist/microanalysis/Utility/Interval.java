package gov.nist.microanalysis.Utility;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * <p>
 * An interval is a continuous range of integers [min, max).
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
public class Interval
   implements
   Comparable<Interval> {
   final int mMin;
   final int mMax;

   static public final Interval NULL = new Interval(0, 0);

   public Interval(int min, int max) {
      if(min == max) {
         mMin = Integer.MIN_VALUE;
         mMax = Integer.MIN_VALUE;
      } else {
         mMin = Math.min(min, max);
         mMax = Math.max(min, max);
      }
   }

   private boolean intersects(Interval i2) {
      return (mMin != Integer.MIN_VALUE) && (mMin <= i2.mMax) && (mMax >= i2.mMin);
   }

   private boolean containedBy(Interval i) {
      return (mMin != Integer.MIN_VALUE) && (mMin >= i.mMin) && (mMax <= i.mMax);
   }

   /**
    * Does the argument set of intervals fully contain this interval.
    * 
    * @param intervals SortedSet&lt;Interval&gt;
    * @return true if i is fully within one of the intervals; false otherwise.
    */
   public boolean containedBy(SortedSet<Interval> intervals) {
      for(final Interval interval : intervals)
         if(containedBy(interval))
            return true;
      return false;
   }

   static public void validate(SortedSet<Interval> intervals) {
      int max = Integer.MIN_VALUE;
      for(final Interval interval : intervals) {
         assert interval.mMax > interval.mMin : "Inteval failed contained positive range test.";
         assert interval.mMin > max : "Set<Inteval> failed sorted range test. " + intervals.toString();
         max = interval.mMax;
      }
   }

   /**
    * Adds an Interval to the SortedSet&lt;Interval&gt;. This method merges
    * intervals which are connected by the interval toBeAdded.
    * 
    * @param intervals SortedSet&lt;Interval&gt;
    * @param toBeAdded Interval
    * @return A new SortedSet&lt;Interval&gt;
    */
   static public SortedSet<Interval> add(SortedSet<Interval> intervals, Interval toBeAdded) {
      assert toBeAdded != null;
      assert toBeAdded.mMin < toBeAdded.mMax;
      final SortedSet<Interval> result = new TreeSet<Interval>();
      Interval start = null, end = null;
      for(final Interval interval : intervals)
         if(toBeAdded == null)
            result.add(interval);
         else if(toBeAdded.intersects(interval)) {
            if(start == null)
               start = interval;
            end = interval;
            // Don't add it now...
         } else {
            if(start != null) {
               result.add(new Interval(Math.min(toBeAdded.mMin, start.mMin), Math.max(toBeAdded.mMax, end.mMax)));
               start = null;
               end = null;
               toBeAdded = null;
            }
            result.add(interval);
         }
      if(start != null) {
         result.add(new Interval(Math.min(toBeAdded.mMin, start.mMin), Math.max(toBeAdded.mMax, end.mMax)));
         start = null;
         end = null;
         toBeAdded = null;
      }
      if(toBeAdded != null)
         result.add(toBeAdded);
      validate(result);
      return result;
   }

   @Override
   public int compareTo(Interval arg0) {
      int res = Double.compare(this.mMin, arg0.mMin);
      if(res == 0)
         res = Double.compare(this.mMax, arg0.mMax);
      return res;
   }

   /**
    * Takes the full range of channels and trims it down to only those channels
    * which actually contribute to the fit. This optimizes the fit process.
    * 
    * @param data double[]
    * @param intervals Collection&lt;Interval&gt;
    * @return double[]
    */
   public static double[] extract(double[] data, Collection<Interval> intervals) {
      int totalLen = 0;
      for(final Interval interval : intervals)
         totalLen += interval.mMax - interval.mMin;
      final double[] res = new double[totalLen];
      int j = 0;
      for(final Interval interval : intervals)
         for(int i = interval.mMin; i < interval.mMax; ++i)
            res[j++] = data[i];
      assert j == res.length;
      return res;
   }

   @Override
   public String toString() {
      return "[" + mMin + "," + mMax + ")";
   }

   public int min() {
      return mMin;
   }

   public int max() {
      return mMax;
   }

   /**
    * Constructs the interval containing all non-zero channels in the array
    * 'data'
    * 
    * @param data double[]
    * @return {@link Interval}
    */
   static public Interval nonZeroInterval(double[] data) {
      int min = 0, max = data.length;
      for(int i = 0; i < data.length; ++i)
         if(data[i] != 0.0) {
            min = i;
            break;
         }
      for(int i = data.length - 1; i >= 0; --i)
         if(data[i] != 0.0) {
            max = i + 1;
            break;
         }
      assert min < max : "Non-positive interval in FilterPacket.getInterval().";
      return new Interval(min, max);
   }

   /**
    * Does the range include val?
    * 
    * @param val int
    * @return boolean
    */
   public boolean contains(int val) {
      return (val >= mMin) && (val < mMax);
   }

   /**
    * Extend, if necessary, the interval in either direction to include
    * <code>val</code>.
    *
    * @param val int
    * @return Interval An interval containing val
    */
   public Interval extend(int val) {
      if((mMin == Integer.MIN_VALUE) && (mMax == Integer.MIN_VALUE))
         return new Interval(val, val + 1);
      else if(!contains(val))
         return new Interval(Math.min(mMin, val), Math.max(mMax, val + 1));
      else
         return this;
   }
}
