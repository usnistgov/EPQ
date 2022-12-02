/**
 * <p>
 * Classes that implement this interface can be used to generate absolute
 * elastic scattering cross sections and to generate randomized angles generated
 * from the partial cross section.
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

import java.util.List;

/**
 * Classes that implement this interface can be used to generate absolute
 * elastic scattering cross sections and to generate randomized angles generated
 * from the partial cross section.
 */
abstract public class RandomizedScatter extends AlgorithmClass {

   protected RandomizedScatter(String name, LitReference ref) {
      super("Elastic cross-section", name, ref);
      // TODO Auto-generated constructor stub
   }

   /**
    * getElement - Returns the element with which this cross section is
    * associated.
    * 
    * @return Element
    */
   abstract public Element getElement();

   /**
    * totalCrossSection - Computes the total cross section for an electron of
    * the specified energy.
    * 
    * @param energy
    *           double - In Joules
    * @return double - in square meters
    */
   abstract public double totalCrossSection(double energy);

   /**
    * randomScatteringAngle - Returns a randomized scattering angle in the range
    * [0,PI] that comes from the distribution of scattering angles for an
    * electron of specified energy on an atom of the element represented by the
    * instance of this class.
    * 
    * @param energy
    *           double - In Joules
    * @return double - an angle in radians
    */
   abstract public double randomScatteringAngle(double energy);

   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return null;
   }

   @Override
   protected void initializeDefaultStrategy() {
      // Don't do anything by default...

   }

}
