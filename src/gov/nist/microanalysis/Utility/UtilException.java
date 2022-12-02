package gov.nist.microanalysis.Utility;

/**
 * <p>
 * Defines a class for non-fatal exceptions occuring in the utility library.
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
public class UtilException extends Exception {
   private static final long serialVersionUID = 0x1;

   public UtilException() {
      super();
   }

   public UtilException(String string) {
      super(string);
   }

   public UtilException(String string, Throwable throwable) {
      super(string, throwable);
   }

   public UtilException(Throwable throwable) {
      super(throwable);
   }
}
