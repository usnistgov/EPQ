package gov.nist.microanalysis.EPQLibrary;

/**
 * <p>
 * Provides a standardized base set of static items for use in caveats - a
 * mechanism for identifying an algorithms limitations.
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

public class CaveatBase {
   static final public String None = "No limitations have been identified.";
   static final public String Broken = "The implementation of this algorithm is broken.";
   static final public String NotImplemented = "This algorithm has not been implemented yet.";

   /**
    * isBroken - Is this implementation broken?
    * 
    * @param str
    *           String - The result from a call to a caveat method
    * @return boolean
    */
   public static boolean isBroken(String str) {
      return (str == Broken) || str.equals(Broken);
   }

   /**
    * isNone - Are there no caveats identified with this algorithm?
    * 
    * @param str
    *           String - The result from a call to a caveat method
    * @return boolean
    */
   public static boolean isNone(String str) {
      return (str == None) || str.equals(None);
   }

   /**
    * isNotImplemented - Is this algorithm not implemented?
    * 
    * @param str
    *           String - The result from a call to a caveat method
    * @return boolean
    */
   public static boolean isNotImplemented(String str) {
      return (str == NotImplemented) || str.equals(NotImplemented);
   }

   /**
    * appendCaveat - Append a new caveat to a list of existing caveats. If one
    * or both is CaveatBase.None this is handled correctly. If both are
    * CaveatBase.None then CaveatBase.None is returned.
    * 
    * @param base
    *           String
    * @param str
    *           String
    * @return String
    */
   public static String append(String base, String str) {
      if (base.equals(None))
         return str;
      else if (!str.equals(None))
         return base + "\n" + str;
      else
         return base;
   }

   /**
    * formatCaveat - Format the caveat in such a way that the algorithm is
    * identified along with the caveat. If the caveat equals CaveatBase.None
    * then CaveatBase.None is returned.
    * 
    * @param obj
    *           Object
    * @param str
    *           String
    * @return String
    */
   public static String format(Object obj, String str) {
      return str.equals(None) ? None : obj.toString() + ": " + str;
   }

}
