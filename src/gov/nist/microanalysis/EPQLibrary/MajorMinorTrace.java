package gov.nist.microanalysis.EPQLibrary;

import java.util.Map;
import java.util.TreeMap;

/**
 * <p>
 * Implements Dale's definition of Major/Minor/Trace. Adds methods to get the
 * correct instance based on mass fraction and to create an crude approximate
 * composition based on only Major/Minor/Trace description of the material.
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
public enum MajorMinorTrace {

   Major(0.1, 1.0), Minor(0.01, 0.1), Trace(0.0, 0.01), None(0.0, 0.0);

   private final double mMin;
   private final double mMax;

   private MajorMinorTrace(double min, double max) {
      mMin = min;
      mMax = max;
   }

   public boolean contains(double massFrac) {
      final double boundedMassFrac = Math.min(Math.max(0.0, massFrac), 1.0);
      return ((boundedMassFrac > mMin) && (boundedMassFrac <= mMax));
   }

   public static MajorMinorTrace create(double massFrac) {
      for (final MajorMinorTrace mmt : values())
         if (mmt.contains(massFrac))
            return mmt;
      return None;
   }

   static public Map<Element, MajorMinorTrace> from(Composition comp, boolean normalized) {
      final Map<Element, MajorMinorTrace> res = new TreeMap<Element, MajorMinorTrace>();
      for (final Element elm : comp.getElementSet())
         res.put(elm, create(comp.weightFraction(elm, normalized)));
      return res;
   }

   /**
    * Builds a rough approximation Composition based on a Major/Minor/Trace type
    * description of a material. This is useful for working with an unknown that
    * the user only wants to define in the loosest possible terms.
    * 
    * @param mmtm
    *           Map&lt;Element, MajorMinorTrace&gt;
    * @param name
    *           A name for the crude compositional object
    * @return Composition
    */
   static public Composition asComposition(Map<Element, MajorMinorTrace> mmtm, String name) {
      final Element[] elms = mmtm.keySet().toArray(new Element[mmtm.size()]);
      final double[] qty = new double[elms.length];
      double sum = 0.0;
      int majorCount = 0;
      for (int i = 0; i < elms.length; ++i) {
         final MajorMinorTrace mmt = mmtm.get(elms[i]);
         switch (mmt) {
            case None :
               qty[i] = 0.0;
               break;
            case Trace :
               qty[i] = 0.001;
               sum += qty[i];
               break;
            case Minor :
               qty[i] = Math.pow(10.0, -1.5);
               sum += qty[i];
               break;
            case Major :
               majorCount++;
               break;
         }
      }
      if (majorCount > 0)
         // Divide the remainder among the Major constituents.
         for (int i = 0; i < elms.length; ++i)
            if (mmtm.get(elms[i]).equals(Major))
               qty[i] = (1.0 - sum) / majorCount;
      // The result will be normalized *unless* there are
      // no Major constituents.
      final Composition res = new Composition(elms, qty);
      res.setName(name);
      return res;
   }
}
