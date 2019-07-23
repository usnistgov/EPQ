/**
 * gov.nist.microanalysis.Utility.UncertainValue Created by: Nicholas Date: Sep
 * 16, 2012
 */
package gov.nist.microanalysis.Utility;

/**
 * <p>
 * This class handles the legacy issue of mapping old-style XStream serialized
 * UncertainValue objects into the new style UncertainValue2 objects.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas
 * @version 1.0
 */
@Deprecated
public class UncertainValue {
   private double mValue;
   private double mSigma;

   /**
    * Convert UncertainValue instances into UncertainValue2 instances.
    * 
    * @return An UncertainValue2
    */
   public Object readResolve() {
      return new UncertainValue2(mValue, mSigma);
   }

}
