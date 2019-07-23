/**
 * <p>
 * A Strategy is a one-to-one mapping between abstract base classes that are
 * based on the AlgorithmClass and objects implementing the abstract base class.
 * For example a Stragety might map the MassAbsorptionCoefficient abstract base
 * class into the MassAbsorptionCoefficient.Chantler2005 implementation.
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
package gov.nist.microanalysis.EPQLibrary;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * <p>
 * Algorithms often depend on other algorithms during the computation. Sometimes
 * it is desirable to interchange dependent algorithms. Strategy objects
 * facilitate this. A Strategy is a mechanism for interchanging the algorithms
 * which are used to compute a quantity. A Strategy is a one-to-one mapping
 * between algorithm type (such as BackscatterFactor.class or
 * MassAbsorptionCoefficient.class) and the algorithm implementing this
 * algorithm-type (such as BackscatterFactor.Pouchou1991 or
 * MassAbsorptionCoefficient.Chantler2005). Each instance of the AlgorithmClass
 * understands what other types of AlgorithmClass it requires to perform its
 * operations and can provide a list of the default algorithms as its default
 * Strategy. The defaults algorithms can be overridden using the
 * AlgorithmClass.apply(Strategy ) method. Strategy objects enforce consistency
 * across a calculation.
 * </p>
 */
public class Strategy
   implements Cloneable {
   private final SortedMap<String, AlgorithmClass> mMap;

   /**
    * For internal use only.
    * 
    * @return SortedMap&lt;String, AlgorithmClass&gt
    */
   SortedMap<String, AlgorithmClass> getStrategyMap() {
      return Collections.unmodifiableSortedMap(mMap);
   }

   /**
    * Returns a list of the name of the defined AlgorithmClass types
    * 
    * @return A Collection of String objects
    */
   public Collection<String> listAlgorithmClasses() {
      return Collections.unmodifiableCollection(mMap.keySet());
   }

   /**
    * Constructs a Strategy
    */
   public Strategy() {
      super();
      mMap = new TreeMap<String, AlgorithmClass>();
   }

   @Override
   public Object clone() {
      final Strategy res = new Strategy();
      res.mMap.putAll(mMap);
      return res;
   }

   /**
    * apply - For those Class objects in this Stragegy, assign the
    * Class-to-AlgorithmClass mappings in the argument Strategy to this
    * Strategy. Preexisting assignments are overridden.
    * 
    * @param st
    */
   public void apply(Strategy st) {
      for(final Entry<String, AlgorithmClass> me : st.mMap.entrySet()) {
         final String k = me.getKey();
         if(mMap.containsKey(k))
            mMap.put(k, me.getValue());
      }
   }

   /**
    * Add all the algorithm mappings from the argument Strategy into this
    * Strategy.
    * 
    * @param st
    */
   public void addAll(Strategy st) {
      for(final Map.Entry<String, AlgorithmClass> me : st.mMap.entrySet())
         mMap.put(me.getKey(), me.getValue());
   }

   /**
    * Specify an implentation of an AlgorithmClass-derived abstract base class.
    * 
    * @param cls - One of AbsorptionCorrection.class, BackscatterFactor.class,
    *           ...
    * @param value - A class derived from the class specified in cls
    */
   public void addAlgorithm(Class<?> cls, AlgorithmClass value) {
      if(!cls.isAssignableFrom(value.getClass()))
         throw new IllegalArgumentException(value.toString() + " is not derived from " + cls.toString());
      mMap.put(cls.toString(), value);
   }

   /**
    * Get the implentation of an AlgorithmClass-derived abstract base class.
    * 
    * @param cls
    * @return A class instance derived from AlgorithmClass or null
    */
   public AlgorithmClass getAlgorithm(Class<?> cls) {
      return mMap.get(cls.toString());
   }

   /**
    * Get the implentation of an AlgorithmClass-derived abstract base class from
    * the name of the abstract base class.
    * 
    * @param clsName
    * @return A class instance derived from AlgorithmClass or null
    */
   public AlgorithmClass getAlgorithm(String clsName) {
      return mMap.get(clsName);
   }

   public Collection<AlgorithmClass> getAlgorithms() {
      return mMap.values();
   }

}