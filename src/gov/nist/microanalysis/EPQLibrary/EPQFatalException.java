package gov.nist.microanalysis.EPQLibrary;

/**
 * <p>
 * An exception that is thrown at runtime when something really bad happens.
 * When an error of this type occurs it is assumed that the program execution
 * should not be permitted to continue.
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

public class EPQFatalException extends RuntimeException {
   private static final long serialVersionUID = 0x1;

   public EPQFatalException() {
   }

   public EPQFatalException(String p0) {
      super(p0);
   }

   public EPQFatalException(Throwable p0) {
      super(p0);
   }

   public EPQFatalException(String p0, Throwable p1) {
      super(p0, p1);
   }
}
