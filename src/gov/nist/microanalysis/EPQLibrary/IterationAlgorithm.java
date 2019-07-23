package gov.nist.microanalysis.EPQLibrary;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * An abstract base class and a few implementations of a class designed to
 * iterated from one estimate of the composition to an increasingly refined
 * estimate of composition based on a desired k-ratio.
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
abstract public class IterationAlgorithm
   extends AlgorithmClass {
   protected KRatioSet mDesired = null;
   protected ArrayList<Composition> mHistory = new ArrayList<Composition>();

   static private final double TINY = 1.0e-6;
   static private final double HUGE = 10.0;

   protected IterationAlgorithm(String name, String ref) {
      super("Iteration", name, ref);
   }

   protected IterationAlgorithm(String name, LitReference ref) {
      super("Iteration", name, ref);
   }

   @Override
   protected void initializeDefaultStrategy() {
      // None necessary
   }

   /**
    * getAllImplementations - Returns a full list of all available algorithms.
    * Each item is an implements the IterationAlgorithm class.
    * 
    * @return List
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   public void initialize(KRatioSet desiredKrs, Composition estComp) {
      mDesired = desiredKrs;
      mHistory.clear();
      mHistory.add(estComp);
   }

   protected Composition previousEstimate() {
      return mHistory.get(mHistory.size() - 1);
   }

   /**
    * nextCompositionEstimate - Returns the next estimate of the Composition
    * based on the previous estimated Composition (estComp)
    * 
    * @param zafMap A map containing
    *           &lt;XRayTransitionSet,Double(zaf*estComp_i)&gt;
    * @return The next estimate of the Composition
    */
   abstract protected Composition perform(Map<XRayTransitionSet, Double> zafMap);

   /**
    * Compute the next estimate of the composition from the specified map of
    * XRayTransitionSet objects into k-ratios (as Doubles).
    * 
    * @param zafMap
    * @return Composition
    */
   public Composition compute(Map<XRayTransitionSet, Double> zafMap) {
      final Composition res = perform(zafMap);
      mHistory.add(res);
      return res;
   }

   /**
    * Returns the list of estimated compositons through which the algorithm
    * iterated when estimating the optimal composition during the current
    * iteration run (as defined by <code>initialize(...)</code>).
    * 
    * @return List&lt;Collection&gt;
    */
   public List<Composition> getIterationHistory() {
      return Collections.unmodifiableList(mHistory);
   }

   /**
    * A simple iteration procedure based on successive approximations. Works
    * fine.
    */
   public static class SimpleIterationAlgorithm
      extends IterationAlgorithm {
      public SimpleIterationAlgorithm() {
         super("Simple iteration", "Source unknown");
      }

      @Override
      protected Composition perform(Map<XRayTransitionSet, Double> zafMap) {
         final Composition next = new Composition();
         // k = ZAF*C[ref]
         for(final XRayTransitionSet xrts : mDesired.keySet())
            if(zafMap.containsKey(xrts))
               next.addElement(xrts.getElement(), Math2.bound(mDesired.getKRatio(xrts) / zafMap.get(xrts), TINY, HUGE));
         return next;
      }
   }

   public static final IterationAlgorithm SimpleIteration = new SimpleIterationAlgorithm();

   /**
    * Criss &amp; Birks phenomenological iteration algorithm based on alpha
    * factors. Implementation doesn't work!?
    */
   public static class HyperbolicIterationAlgorithm
      extends IterationAlgorithm {
      public HyperbolicIterationAlgorithm() {
         super("Hyperbolic iteration", new LitReference.BookChapter(LitReference.ElectronMicroprobe, "217", new LitReference.Author[] {
            LitReference.JCriss,
            LitReference.LBirks
         }));
      }

      @Override
      protected Composition perform(Map<XRayTransitionSet, Double> zafMap) {
         final Composition estComp = previousEstimate();
         final Composition next = new Composition();
         for(final XRayTransitionSet xrts : mDesired.keySet())
            if(zafMap.containsKey(xrts)) {
               final double ca1 = estComp.weightFraction(xrts.getElement(), false);
               final double ka1 = ca1 * zafMap.get(xrts);
               final double alpha1 = (ca1 * (1.0 - ka1)) / ((1.0 - ca1) * ka1);
               final double ka = mDesired.getKRatio(xrts);
               final double ca2 = (alpha1 * ka) / (1.0 - (ka * (1.0 - alpha1)));
               next.addElement(xrts.getElement(), Math2.bound(ca2, TINY, HUGE));
            }
         return next;
      }
   }

   public static final IterationAlgorithm HyperbolicIteration = new HyperbolicIterationAlgorithm();

   /**
    * Pouchou &amp; Pichoir's modification to Criss &amp; Birk's iteration
    * procedure. Implementation doesn't work!?
    */
   public static class PapIterationAlgorithm
      extends IterationAlgorithm {
      public PapIterationAlgorithm() {
         super("Pouchou & Pichoir Iteration", new LitReference.BookChapter(LitReference.ElectronProbeQuant, "41", new LitReference.Author[] {
            LitReference.JPouchou,
            LitReference.FPichoir
         }));
      }

      @Override
      protected Composition perform(Map<XRayTransitionSet, Double> zafMap) {
         final Composition estComp = previousEstimate();
         final Composition next = new Composition();
         for(final XRayTransitionSet xrts : mDesired.keySet())
            if(zafMap.containsKey(xrts)) {
               final Element el = xrts.getElement();
               final double ca1 = estComp.weightFraction(el, false);
               final double ka1 = ca1 * zafMap.get(xrts);
               final double ka = mDesired.getKRatio(xrts);
               double ce;
               if((ka1 / ca1) <= 1.0) {
                  // Hyperbolic approximation
                  final double alpha1 = (ca1 * (1.0 - ka1)) / ((1.0 - ca1) * ka1);
                  ce = (alpha1 * ka) / (1.0 - (ka * (1.0 - alpha1)));
               } else {
                  // Parabolic approximation
                  final double alpha = (ka1 - (ca1 * ca1)) / (ca1 - (ca1 * ca1));
                  ce = (-alpha + Math.sqrt((alpha * alpha) + (4.0 * (1.0 - alpha) * ka1))) / (2.0 * (1.0 - alpha));
               }
               next.addElement(el, Math2.bound(ce, TINY, HUGE));
            }
         return next;
      }
   }

   public static final IterationAlgorithm PAPIteration = new PapIterationAlgorithm();

   /**
    * An iteration algorithm based on a first-order estimator. Works nicely!
    */
   static public class WegsteinIterationAlgorithm
      extends IterationAlgorithm {
      WegsteinIterationAlgorithm() {
         super("Wegstein iteration", "Wegstein, A. (1958) Commun. ACM, 1, 9");
      }

      // Need to remember the previous two maps
      private Map<XRayTransitionSet, Double> mZAFMapNm1;

      @Override
      public void initialize(KRatioSet desiredKrs, Composition estComp) {
         super.initialize(desiredKrs, estComp);
         mZAFMapNm1 = null;
         mHistory.clear();
      }

      @Override
      protected Composition perform(Map<XRayTransitionSet, Double> zafMap) {
         final boolean normalize = false;
         final Composition res = new Composition();
         if(mHistory.size() > 2) {
            assert mZAFMapNm1 != null;
            final Composition compN = mHistory.get(mHistory.size() - 1), compNm1 = mHistory.get(mHistory.size() - 2);
            for(final XRayTransitionSet xrts : mDesired.keySet())
               if(zafMap.containsKey(xrts)) {
                  final Element el = xrts.getElement();
                  final double can = compN.weightFraction(el, normalize);
                  final double fan = 1.0 / zafMap.get(xrts);
                  final double can_1 = compNm1.weightFraction(el, normalize);
                  final Double d = mZAFMapNm1.get(xrts);
                  final double fan_1 = 1.0 / (d != null ? d.doubleValue() : 1.0e-5);
                  final double ka = mDesired.getKRatio(xrts);
                  if((d != null) && ((10.0 * Math.abs(can - can_1)) > Math.abs(fan - fan_1))) {
                     final double dfa_dca = (fan - fan_1) / (can - can_1);
                     final double den = 1.0 - (ka * dfa_dca);
                     // Springer-1976's suggestion as mentioned in Scott, Love,
                     // Reed-2nd Ed.
                     final double wgtFrac = Math.abs(den) > 0.2 ? can + (((ka * fan) - can) / den) : ka * fan;
                     res.addElement(el, Math2.bound(wgtFrac, TINY, HUGE));
                  } else
                     res.addElement(el, Math2.bound(ka * fan, TINY, HUGE));
               }
         } else
            // Simple iteration
            for(final XRayTransitionSet xrts : mDesired.keySet())
               if(zafMap.containsKey(xrts))
                  res.addElement(xrts.getElement(), Math2.bound(mDesired.getKRatio(xrts) / zafMap.get(xrts), TINY, HUGE));
         assert zafMap != null;
         mZAFMapNm1 = zafMap;
         return res;
      }
   }

   public static final IterationAlgorithm WegsteinIteration = new WegsteinIterationAlgorithm();

   /**
    * A wrapper to facilitate diagnostics or reporting on IterationAlgorithms.
    * Displays the measured, previous Composition estimate, the k-ratio
    * assoicated with the previous Composition estimate and the estimated next
    * Composition on the specified PrintStream.
    */
   static public class DiagnosticIterationAlgorithm
      extends IterationAlgorithm {
      private final PrintWriter mOutput;
      private final IterationAlgorithm mBase;

      public DiagnosticIterationAlgorithm(IterationAlgorithm base, OutputStream os) {
         super("Diag[" + base.getName() + "]", base.getReference());
         mBase = base;
         mOutput = new PrintWriter(os);
         mOutput.println("Using the " + base.getName() + " algorithm.");
         mOutput.flush();
      }

      public DiagnosticIterationAlgorithm(IterationAlgorithm base, Writer wr) {
         super("Diag[" + base.getName() + "]", base.getReference());
         mBase = base;
         mOutput = new PrintWriter(wr);
         mOutput.println("Using the " + base.getName() + " algorithm.");
         mOutput.flush();
      }

      @Override
      public void initialize(KRatioSet desiredKrs, Composition estComp) {
         super.initialize(desiredKrs, estComp);
         mBase.initialize(desiredKrs, estComp);
      }

      @Override
      protected Composition perform(Map<XRayTransitionSet, Double> zafMap) {
         final Composition next = mBase.compute(zafMap);
         try {
            final Composition estComp = previousEstimate();
            mOutput.println("Measured KRS : \t" + mDesired.toString());
            final KRatioSet prevKrs = new KRatioSet();
            for(final XRayTransitionSet xrts : mDesired.keySet())
               if(zafMap.containsKey(xrts))
                  prevKrs.addKRatio(xrts, estComp.weightFraction(xrts.getElement(), false) * (zafMap.get(xrts)).doubleValue(), 0.0);
            mOutput.println("Prev KRS     : \t" + prevKrs.toString());
            mOutput.println("Prev comp    : \t" + estComp.weightPercentString(false));
            mOutput.println("Next comp    : \t" + next.weightPercentString(false));
            mOutput.println("EPS = " + prevKrs.difference(mDesired));
            mOutput.println("-------------------------------------------------------------------");
         }
         catch(final Exception ex) {
            ex.printStackTrace(mOutput);
         }
         finally {
            mOutput.flush();
         }
         return next;
      }
   }

   static public final IterationAlgorithm Default = IterationAlgorithm.WegsteinIteration;

   static private final AlgorithmClass[] mAllImplementations = {
      IterationAlgorithm.SimpleIteration,
      IterationAlgorithm.HyperbolicIteration,
      IterationAlgorithm.PAPIteration,
      IterationAlgorithm.WegsteinIteration
   };
}
