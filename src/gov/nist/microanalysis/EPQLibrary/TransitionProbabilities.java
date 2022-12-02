package gov.nist.microanalysis.EPQLibrary;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.Utility.CSVReader;

/**
 * <p>
 * This class maps ionized AtomicShell objects into the resulting XRayTransition
 * objects and provides relative probability data.
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
abstract public class TransitionProbabilities extends AlgorithmClass {

   static private class ElementDatum implements Comparable<ElementDatum> {
      private final int mIonized;
      private final XRayTransition mXRayTransition;
      private double mProbability;

      private ElementDatum(int ionized, Element el, int src, int dest, double prob) {
         mIonized = ionized;
         mXRayTransition = new XRayTransition(el, src, dest);
         mProbability = prob;
      }

      private boolean isCosterKronig() {
         return mXRayTransition.getDestination().getFamily() == mXRayTransition.getSource().getFamily();
      }

      @Override
      public int compareTo(ElementDatum o) {
         int res = mXRayTransition.getElement().compareTo(o.mXRayTransition.getElement());
         if (res == 0)
            res = (mProbability > o.mProbability ? -1 : (mProbability < o.mProbability ? 1 : 0));
         if (res == 0)
            res = (mIonized > o.mIonized ? 1 : (mIonized < o.mIonized ? -1 : 0));
         if (res == 0) {
            final int d0 = mXRayTransition.getDestinationShell(), d1 = o.mXRayTransition.getDestinationShell();
            res = (d0 > d1 ? 1 : (d0 < d1 ? -1 : 0));
         }
         if (res == 0) {
            final int d0 = mXRayTransition.getSourceShell(), d1 = o.mXRayTransition.getSourceShell();
            res = (d0 > d1 ? 1 : (d0 < d1 ? -1 : 0));
         }
         return res;
      }

      @Override
      public String toString() {
         final StringBuffer res = new StringBuffer();
         res.append(mXRayTransition.getElement().getAtomicNumber());
         res.append(", ");
         res.append(mIonized);
         res.append(", ");
         res.append(mXRayTransition.getDestinationShell());
         res.append(", ");
         res.append(mXRayTransition.getSourceShell());
         res.append(", \"");
         res.append(XRayTransition.removeGreek(mXRayTransition.getSiegbahnName()));
         res.append(" via ");
         res.append(AtomicShell.getIUPACName(mIonized));
         res.append("\", ");
         res.append(mProbability);
         return res.toString();
      }
   }

   public static final EndLib97 Relax = new EndLib97();
   public static final EndLib97Tweaked RelaxTweaked = new EndLib97Tweaked();
   public static final TransitionProbabilities Default = RelaxTweaked;

   private static final AlgorithmClass[] mAllImplementations = {Relax, RelaxTweaked};

   /**
    * <p>
    * Improves the quality of the simulated spectra by correcting some egregious
    * bugs in the L-lines. The bug is clearly visible in the Cu L line and
    * particularly bad at lower atomic numbers.
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    * 
    * @author nritchie
    * @version 1.0
    */
   static class EndLib97Tweaked extends EndLib97 {

      private EndLib97Tweaked() {
         super();
         tweak();

      }

      private void tweak() {
         // Correct the problem that is making the L3-M1, L2-M1 too intense
         for (final ArrayList<ElementDatum> elmData : mRadiative)
            for (final ElementDatum elmDatum : elmData) {
               final XRayTransition xrt = elmDatum.mXRayTransition;
               if (xrt.getSourceShell() == AtomicShell.MI) {
                  final double z = xrt.getElement().getAtomicNumber();
                  if ((xrt.getDestinationShell() == AtomicShell.LIII) || (xrt.getDestinationShell() == AtomicShell.LII))
                     if (z >= Element.elmCu)
                        elmDatum.mProbability *= Math.max(0.1, 0.1 + ((0.9 * (z - Element.elmCu)) / (Element.elmAu - Element.elmCu)));
                     else
                        elmDatum.mProbability *= Math.max(0.1, 0.2 - ((0.1 * (z - Element.elmTi)) / (Element.elmCu - Element.elmTi)));
               }
            }

      }

   }

   static class EndLib97 extends TransitionProbabilities {

      protected ArrayList<ArrayList<ElementDatum>> mRadiative = null;
      protected ArrayList<ArrayList<ElementDatum>> mCosterKronig = null;

      /**
       * Constructs an implementation of TransitionProbabilites based on the
       * ENDLIB-97 data set.
       */
      private EndLib97() {
         super("RELAX from ENDLIB-97", LitReference.ENDLIB97_Relax);
         parse();
      }

      private void parse() {
         synchronized (TransitionProbabilities.class) {
            if (mRadiative == null) {
               final CSVReader csv = new CSVReader.ResourceReader("relax.csv", true);
               final double[][] res = csv.getResource(TransitionProbabilities.class);
               int z = 0;
               mRadiative = new ArrayList<ArrayList<ElementDatum>>();
               mRadiative.ensureCapacity(100);
               mCosterKronig = new ArrayList<ArrayList<ElementDatum>>();
               mCosterKronig.ensureCapacity(100);
               Element el = Element.None;
               for (final double[] line : res) {
                  while (Math.round(line[0]) > z) {
                     ++z;
                     el = Element.byAtomicNumber(z);
                     mRadiative.add(new ArrayList<ElementDatum>());
                     mCosterKronig.add(new ArrayList<ElementDatum>());
                  }
                  // Data line format: Element, Ionized, Destination, Source,
                  // Probability
                  final ElementDatum ed = new ElementDatum((int) Math.round(line[1]), el, (int) Math.round(line[3]), (int) Math.round(line[2]),
                        line[4]);
                  if (ed.isCosterKronig())
                     mCosterKronig.get(z - 1).add(ed);
                  else
                     mRadiative.get(z - 1).add(ed);
               }
            }
         }
      }

      protected ArrayList<ElementDatum> getElementDatum(Element el) {
         return mRadiative.get(el.getAtomicNumber() - 1);
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.TransitionProbabilities#getTransitions(gov.nist.microanalysis.EPQLibrary.AtomicShell,
       *      double)
       */
      @Override
      public TreeMap<XRayTransition, Double> getTransitions(AtomicShell ionized, double minWeight) {
         minWeight = Math.max(1.0e-10, minWeight);
         final TreeMap<XRayTransition, Double> res = new TreeMap<XRayTransition, Double>();
         final int is = ionized.getShell();
         double max = 0.0;
         final ArrayList<ElementDatum> elmData = getElementDatum(ionized.getElement());
         for (final ElementDatum ed : elmData)
            if ((ed.mIonized == is) && (ed.mProbability > max))
               max = ed.mProbability;
         for (final ElementDatum ed : elmData)
            if ((ed.mIonized == is) && (ed.mProbability >= (minWeight * max)) && ed.mXRayTransition.energyIsAvailable())
               res.put(ed.mXRayTransition, ed.mProbability);
         return res;
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.TransitionProbabilities#getExcitedShells(gov.nist.microanalysis.EPQLibrary.Element,
       *      double, double)
       */
      @Override
      public Set<AtomicShell> getExcitedShells(Element el, double beamE, double minWeight) {
         final boolean[] shells = new boolean[(AtomicShell.OIX - AtomicShell.K) + 1];
         Arrays.fill(shells, false);
         for (final ElementDatum ed : getElementDatum(el))
            shells[ed.mIonized] |= (ed.mProbability >= minWeight);
         final Set<AtomicShell> res = new TreeSet<AtomicShell>();
         for (int sh = AtomicShell.K; sh <= AtomicShell.OIX; ++sh)
            if (shells[sh - AtomicShell.K])
               res.add(new AtomicShell(el, sh));
         return res;
      }
   }

   protected TransitionProbabilities(String name, LitReference ref) {
      super("Transition Probabilities", name, ref);
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#getAllImplementations()
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#initializeDefaultStrategy()
    */
   @Override
   protected void initializeDefaultStrategy() {
      // Do nothing...
   }

   /**
    * Returns a Map&lt;XRayTransition,Double&gt; containing a list of
    * XRayTransition objects generated when the specified AtomicShell is
    * ionized.
    * 
    * @param ionized
    *           The ionized AtomicShell
    * @param minWeight
    *           Minimum weight to return [0.0,1.0) as a fraction of the most
    *           intense line
    * @return Map&lt;XRayTransition,Double&gt; where Double is the transition
    *         probability [minWeight,1.0]
    */
   abstract public TreeMap<XRayTransition, Double> getTransitions(AtomicShell ionized, double minWeight);

   /**
    * Returns a complete list of the AtomicShell with XRayTransitions with
    * probability greater than the specified weight that can be excited by an
    * electron of the specified energy .
    * 
    * @param el
    *           The element
    * @param beamE
    *           The beam energy (Joules)
    * @param minWeight
    *           The minimum transition probability to consider
    * @return List&lt;XRayTransition&gt;
    */
   abstract public Set<AtomicShell> getExcitedShells(Element el, double beamE, double minWeight);

   /**
    * Computes the fluorescence yield for the specified atomic shell.
    * 
    * @param ionized
    * @return double
    */
   public double fluorescenceYield(AtomicShell ionized) {
      final TreeMap<XRayTransition, Double> tr = getTransitions(ionized, 0.0);
      double res = 0.0;
      for (final Map.Entry<XRayTransition, Double> me : tr.entrySet())
         if (me.getKey().getDestination().equals(ionized))
            res += me.getValue().doubleValue();
      return res;
   }

   public static void main(String[] args) {
      final TreeSet<ElementDatum> res = new TreeSet<ElementDatum>();
      for (final ArrayList<ElementDatum> aed : Relax.mRadiative)
         for (final ElementDatum ed : aed)
            res.add(ed);
      try {
         try (final PrintWriter pw = new PrintWriter(new FileOutputStream("c:\\relax_sorted.csv"))) {
            for (final ElementDatum ed : res)
               pw.println(ed.toString());
         }
      } catch (final FileNotFoundException e) {
         e.printStackTrace();
      }
   }
}
