/**
 * gov.nist.nanoscalemetrology.JMONSEL.TimeKeeper Created by: jvillar Date: Mar
 * 28, 2011
 */
package gov.nist.nanoscalemetrology.JMONSEL;

/**
 * <p>
 * A class to facilitate time-dependent models. TimeKeeper keeps time in
 * seconds. It contains setters that are used to set the time. Example: the user
 * may have a scan generator that updates the time according to some model (so
 * much time between pixels within a line, so much additional time between
 * lines, so much additional time between frames, etc.) each time a new landing
 * position is selected. Models that depend upon time (e.g., conduction that
 * involves a finite RC time constant) can then access the time via TimeKeeper's
 * getTime() method. For convenience, TimeKeeper is implemented as a singleton
 * class. Any algorithm that uses getTimeKeeper will therefore be given access
 * to the same TimeKeeper, eliminating the need to pass it in the argument list.
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
public class TimeKeeper {

   /**
    * Constructs a TimeKeeper
    */
   private double time = 0.;
   static private TimeKeeper tk = null;

   /**
    * Returns the existing TimeKeeper if there is one. Otherwise, it generates
    * and returns a new one with initial time = 0.
    *
    * @return - Existing or new TimeKeeper
    */
   static public TimeKeeper getTimeKeeper() {
      if (tk == null)
         tk = new TimeKeeper();
      return tk;
   }

   private TimeKeeper() {
      super();
   }

   /**
    * Gets the current value assigned to time
    *
    * @return Returns the time.
    */
   public double getTime() {
      return time;
   }

   /**
    * Sets the value assigned to time.
    *
    * @param time
    *           The value to which to set time.
    */
   public void setTime(double time) {
      if (this.time != time)
         this.time = time;
   }

   /**
    * Adds deltat to the current time.
    *
    * @param deltat
    */
   public void incrementTime(double deltat) {
      time += deltat;
   }

}
