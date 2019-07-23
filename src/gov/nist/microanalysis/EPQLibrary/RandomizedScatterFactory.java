/**
 * <p>
 * Defines a mechanism to construct instances of IRandomizedScatter.
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
package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

/**
 * Defines a mechanism to construct instances of IRandomizedScatter.
 */
abstract public class RandomizedScatterFactory
   extends AlgorithmClass {

   protected RandomizedScatterFactory(String name, LitReference ref) {
      super("Scatter factory", name, ref);
   }

   /**
    * Return an instance of IRandomizedScatter for the specified element.
    * 
    * @param elm
    * @return IRandomizedScatter interface
    */
   abstract public RandomizedScatter get(Element elm);

   static private final AlgorithmClass[] mImplementations = {
      GasScatteringCrossSection.Factory,
      NISTMottScatteringAngle.Factory,
      ScreenedRutherfordScatteringAngle.Factory,
      CzyzewskiMottScatteringAngle.Factory
   };

   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mImplementations);
   }

}
