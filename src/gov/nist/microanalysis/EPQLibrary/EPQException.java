package gov.nist.microanalysis.EPQLibrary;

/**
 * <p>
 * An exception class to serve as a base Exception class for all error occuring
 * in this library.
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

public class EPQException
   extends Exception {
   private static final long serialVersionUID = 0x1;

   public EPQException() {
   }

   public EPQException(String p0) {
      super(p0);
   }

   public EPQException(Throwable p0) {
      super(p0);
   }

   public EPQException(String p0, Throwable p1) {
      super(p0, p1);
   }

   @Override
   public String toString() {
      return "EPQ: " + super.toString();
   }

}
