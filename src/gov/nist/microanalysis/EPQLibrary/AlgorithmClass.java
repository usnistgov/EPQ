package gov.nist.microanalysis.EPQLibrary;

import java.util.List;

/**
 * <p>
 * A simple class that implements various common properties of classes that
 * implement algorithms.
 * </p>
 * <p>
 * AlgorithmClass objects have a general type ('class') name and a specific
 * instance name. They can also contain a reference detailing the source of the
 * algorithm.
 * </p>
 * <p>
 * AlgorithmClass objects also define a list of default AlgorithmClass instances
 * on which they depend. Replacements for these default AlgorithmClass instances
 * can be specified using the Strategy mechanism.
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

abstract public class AlgorithmClass extends AlgorithmUser implements Comparable<AlgorithmClass> {

   private final String mClass;
   private final String mName;
   private final LitReference mReference;

   protected AlgorithmClass(String clss, String name, LitReference ref) {
      super();
      mClass = clss;
      mName = name;
      mReference = ref;
   }

   protected AlgorithmClass(String clss, String name, String ref) {
      super();
      mClass = clss;
      mName = name;
      mReference = new LitReference.CrudeReference(ref);
   }

   /**
    * getAllImplementations - Returns a list of all implementations of the
    * derived algorithm class. Typically this method is implemented by the
    * abstract base class from which more specific implementations are derived.
    * 
    * @return List
    */
   abstract public List<AlgorithmClass> getAllImplementations();

   /**
    * compareTo - Sort by name
    * 
    * @param o
    *           UncertainValue
    * @return int
    */
   @Override
   public int compareTo(AlgorithmClass o) {
      return toString().compareTo(o.toString());
   }

   // Overrides Object.toString()
   @Override
   public String toString() {
      return mClass + "[" + mName + "]";
   }

   /**
    * getAlgorithmClass - Get the base AlgorithmClass instance of which this
    * class is an instance.
    * 
    * @return String
    */
   public String getAlgorithmClass() {
      return mClass;
   }

   /**
    * getName - Get the abbreviated name of the algorithm.
    * 
    * @return String
    */
   public String getName() {
      return mName;
   }

   /**
    * getReference - Get the literature reference describing the implementation
    * of this algorithm.
    * 
    * @return String
    */
   public String getReference() {
      return mReference.getShortForm();
   }

   public LitReference getReferenceObj() {
      return mReference;
   }
}
