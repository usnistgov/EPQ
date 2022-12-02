/**
 * gov.nist.microanalysis.NISTMonte.ScanGenerator Created by: John Villarrubia
 * Date: Sep 14, 2011
 */
package gov.nist.nanoscalemetrology.JMONSEL;

/**
 * <p>
 * A ScanGenerator is a class that provides methods to simplify managing
 * sequential positioning of the beam. The essential feature of a scan generator
 * is that it associates an index (0, 1, 2, ...) with a point [x,y,z,t] in space
 * and time. When constructed the generator's index is initialized to -1 and the
 * corresponding coordinates to NaN. Basic usage is to call the next() method,
 * which increments the index, returns the corresponding [x,y,z,t] value, and
 * sets the simulation's TimeKeeper so its time is t. Besides next(), a number
 * of other methods are provided. Only next(), reset(), and setIndex() change
 * the value of the index (hence what position comes next) and the state of the
 * TimeKeeper. The other methods, current(), currentIndex(), and get(), provide
 * information about the current state of the ScanGenerator or its state at a
 * specified index.
 * </p>
 * <p>
 * This class is abstract because the get() method is left to be implemented by
 * a child class. This method is the one that associates a particular value of
 * [x,y,z,t] with a given index, and its implementation will vary depending upon
 * whether the class is implementing a linescan, rectangular raster scan, etc.
 * However, the implementing class need ONLY implement the get() method. Default
 * operations of the other methods are provided by this parent class.
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
public abstract class ScanGenerator {
   private final TimeKeeper tk = TimeKeeper.getTimeKeeper();

   private int index = -1;

   private double[] currentPos = new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN};

   /**
    * Returns the current [x,y,z,t]. When the IScanGenerator is constructed or
    * reset the values are all equal to NaN. Does not increment the counter.
    *
    * @return - The current [x, y, z, t] array
    */
   public double[] current() {
      return currentPos;
   }

   /**
    * Returns the current value of the index. The first point in the scan has
    * index 0. Subsequent points increment by 1. When the IScanGenerator is
    * constructed or reset, the index is -1.
    *
    * @return - the current index
    */
   public int currentIndex() {
      return index;
   }

   /**
    * Returns the scan value at index i. Does not increment the index or set the
    * TimeKeeper's time.
    *
    * @param i
    * @return - The [x, y, z, t] array for index i
    */
   abstract public double[] get(int i);

   /**
    * Returns the scan value at the next index, increments the index, and sets
    * the TimeKeeper to the returned time.
    *
    * @return - The current [x, y, z, t] array
    */
   public double[] next() {
      index++;
      currentPos = get(index);
      tk.setTime(currentPos[3]);
      return currentPos;
   }

   /**
    * Resets the index to -1.
    */
   public void reset() {
      index = -1;
      currentPos = get(index);
      tk.setTime(currentPos[3]);
   }

   /**
    * Sets the index to the designated value. Sets the TimeKeeper's time to the
    * corresponding value.
    *
    * @param i
    */
   public void setIndex(int i) {
      index = i;
      currentPos = get(index);
      tk.setTime(currentPos[3]);
   }
}
