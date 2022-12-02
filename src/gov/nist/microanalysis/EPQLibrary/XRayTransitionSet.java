/**
 * <p>
 * The XRayTransitionSet represents a subset of a single Element's
 * XRayTransitions. A single measurement often is the result of more than one
 * XRayTransition. In an EDS spectrum, peaks often overlap and can not be
 * separated. Thus a single measured k-ratio is likely to represent many
 * transitions.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * XRayTransitionSet represents a set of XRayTransition objects along with logic
 * to assign the set a common shorthand name.
 */
public class XRayTransitionSet implements Comparable<XRayTransitionSet>, Cloneable, Iterable<XRayTransition> {
   /**
    * A single transition
    */
   static final public String TRANSITION = "Transition";
   /**
    * All transitions into a single atomic shell
    */
   // static final public String SHELL = "Shell";
   /**
    * All transitions into a family of shells
    */
   // static final public String FAMILY = "Family";
   /**
    * All transitions associated with a specified element
    */
   static final public String ELEMENT = "All";
   /**
    * An undefined collection of transitions associated with a specific element
    */
   static final public String COLLECTION = "Collection";
   /**
    * No transitions
    */
   static final public String EMPTY = "Empty";
   /**
    * The transitions often referred to as K alpha
    */
   static final public String K_ALPHA = "K\u03B1";
   /**
    * The transitions often referred to as K beta
    */
   static final public String K_BETA = "K\u03B2";
   /**
    * The transitions often referred to as L alpha
    */
   static final public String L_ALPHA = "L\u03B1";
   /**
    * The transitions often referred to as L beta
    */
   static final public String L_BETA = "L\u03B2";
   /**
    * The transitions often referred to as L gamma
    */
   static final public String L_GAMMA = "L\u03B3";
   /**
    * The transitions in the L family that are not La, Lb or Lg
    */
   static final public String L_OTHER = "L*";
   /**
    * The transitions often referred to as M alpha
    */
   static final public String M_ALPHA = "M\u03B1";
   /**
    * The transitions often referred to as M beta
    */
   static final public String M_BETA = "M\u03B2";
   /**
    * The transitions often referred to as M gamma
    */
   static final public String M_GAMMA = "M\u03B3";
   /**
    * The transitions in the M family that are not Ma, Mb or Mg
    */
   static final public String M_OTHER = "M*";
   /**
    * The transitions in the KFamily
    */
   static final public String K_FAMILY = "K-family";
   /**
    * The transitions in the LFamily
    */
   static final public String L_FAMILY = "L-family";
   /**
    * The transitions in the MFamily
    */
   static final public String M_FAMILY = "M-family";
   /**
    * The transitions in the NFamily
    */
   static final public String N_FAMILY = "N-family";
   /**
    * The transitions that end in the K-Shell
    */
   static final public String K_SHELL = "K-shell";
   /**
    * The transitions that end in the LI-Shell
    */
   static final public String LI_SHELL = "LI-shell";
   /**
    * The transitions that end in the LII-Shell
    */
   static final public String LII_SHELL = "LII-shell";
   /**
    * The transitions that end in the LIII-Shell
    */
   static final public String LIII_SHELL = "LIII-shell";
   /**
    * The transitions that end in the MI-Shell
    */
   static final public String MI_SHELL = "MI-shell";
   /**
    * The transitions that end in the MII-Shell
    */
   static final public String MII_SHELL = "MII-shell";
   /**
    * The transitions that end in the MIII-Shell
    */
   static final public String MIII_SHELL = "MIII-shell";
   /**
    * The transitions that end in the MIV-Shell
    */
   static final public String MIV_SHELL = "MIV-shell";
   /**
    * The transitions that end in the MV-Shell
    */
   static final public String MV_SHELL = "MV-shell";
   /**
    * The transitions that end in the NIV-Shell
    */
   static final public String NIV_SHELL = "NIV-shell";
   /**
    * The transitions that end in the NV-Shell
    */
   static final public String NV_SHELL = "NV-shell";

   static final private String MIXED_MESSAGE = "Unable to mix transitions from different elements.";

   private String mMode = EMPTY;
   private final Set<XRayTransition> mSet = new TreeSet<XRayTransition>();
   private final double mMinWeight;

   private static final int[] KAlphaTransitions = {XRayTransition.KA1, XRayTransition.KA2};

   private static final int[] KBetaTransitions = {XRayTransition.KB1, XRayTransition.KB2, XRayTransition.KB3, XRayTransition.KB4, XRayTransition.KB5};

   private static final int[] LAlphaTransitions = {XRayTransition.LA1, XRayTransition.LA2};
   private static final int[] LBetaTransitions = {XRayTransition.LB15, // 14
         XRayTransition.LB2, // 15
         XRayTransition.LB5, // 16
         XRayTransition.LB6, // 17
         XRayTransition.LB7, // 18
         XRayTransition.LB1, // 31
         XRayTransition.LB17, // 32
         XRayTransition.LB10, // 44
         XRayTransition.LB3, // 45
         XRayTransition.LB4, // 46
         XRayTransition.LB9, // 47
   };

   private static final int[] LGammaTransitions = {XRayTransition.LG1, XRayTransition.LG5, XRayTransition.LG6, XRayTransition.LG8, XRayTransition.LG2,
         XRayTransition.LG11, XRayTransition.LG3, XRayTransition.LG4, XRayTransition.LG4p};

   private static final int[] LOtherTransitions = {XRayTransition.L3N2, XRayTransition.L3N3, XRayTransition.L3O2, XRayTransition.L3O3,
         XRayTransition.L3P1, XRayTransition.Ll, XRayTransition.Ls, XRayTransition.Lt, XRayTransition.Lu, XRayTransition.L2M2, XRayTransition.L2M5,
         XRayTransition.L2N2, XRayTransition.L2N3, XRayTransition.L2N5, XRayTransition.L2O2, XRayTransition.L2O3, XRayTransition.L2P2,
         XRayTransition.Ln, XRayTransition.Lv, XRayTransition.L1M1, XRayTransition.L1N1, XRayTransition.L1N4, XRayTransition.L1O1,
         XRayTransition.L1O4};
   private static final int[] MAlphaTransitions = {XRayTransition.MA1, XRayTransition.MA2};
   private static final int[] MBetaTransitions = {XRayTransition.MB};
   private static final int[] MGammaTransitions = {XRayTransition.MG};
   private static final int[] MOtherTransitions = {XRayTransition.M1N2, XRayTransition.M1N3, XRayTransition.M2M4, XRayTransition.M2N1,
         XRayTransition.M2N4, XRayTransition.M2O4, XRayTransition.M3M4, XRayTransition.M3M5, XRayTransition.M3N1, XRayTransition.M3N4,
         XRayTransition.M3O1, XRayTransition.M3O4, XRayTransition.M3O5, XRayTransition.M4N3, XRayTransition.M4O2, XRayTransition.MZ2,
         XRayTransition.M5O3, XRayTransition.MZ1};
   private static final int[] KFamily = {XRayTransition.KA1, XRayTransition.KA2, XRayTransition.KB1, XRayTransition.KB2, XRayTransition.KB3,
         XRayTransition.KB4, XRayTransition.KB5};
   private static final int[] LFamily = {XRayTransition.L3N2, XRayTransition.L3N3, XRayTransition.L3O2, XRayTransition.L3O3, XRayTransition.L3P1,
         XRayTransition.LA1, XRayTransition.LA2, XRayTransition.LB15, XRayTransition.LB2, XRayTransition.LB5, XRayTransition.LB6, XRayTransition.LB7,
         XRayTransition.Ll, XRayTransition.Ls, XRayTransition.Lt, XRayTransition.Lu, XRayTransition.L2M2, XRayTransition.L2M5, XRayTransition.L2N2,
         XRayTransition.L2N3, XRayTransition.L2N5, XRayTransition.L2O2, XRayTransition.L2O3, XRayTransition.L2P2, XRayTransition.LB1,
         XRayTransition.LB17, XRayTransition.LG1, XRayTransition.LG5, XRayTransition.LG6, XRayTransition.LG8, XRayTransition.Ln, XRayTransition.Lv,
         XRayTransition.L1M1, XRayTransition.L1N1, XRayTransition.L1N4, XRayTransition.L1O1, XRayTransition.L1O4, XRayTransition.LB10,
         XRayTransition.LB3, XRayTransition.LB4, XRayTransition.LB9, XRayTransition.LG2, XRayTransition.LG11, XRayTransition.LG3, XRayTransition.LG4,
         XRayTransition.LG4p};
   static private final int[] MFamily = {XRayTransition.M1N2, XRayTransition.M1N3, XRayTransition.M2M4, XRayTransition.M2N1, XRayTransition.M2N4,
         XRayTransition.M2O4, XRayTransition.M3M4, XRayTransition.M3M5, XRayTransition.M3N1, XRayTransition.M3N4, XRayTransition.M3O1,
         XRayTransition.M3O4, XRayTransition.M3O5, XRayTransition.MG, XRayTransition.M4N3, XRayTransition.M4O2, XRayTransition.MB, XRayTransition.MZ2,
         XRayTransition.M5O3, XRayTransition.MA1, XRayTransition.MA2, XRayTransition.MZ1};
   static private final int[] NFamily = {XRayTransition.N4N6, XRayTransition.N5N6};
   static private final int[] KShell = XRayTransition.transitionsIntoShell(AtomicShell.K);
   static private final int[] LIShell = XRayTransition.transitionsIntoShell(AtomicShell.LI);
   static private final int[] LIIShell = XRayTransition.transitionsIntoShell(AtomicShell.LII);
   static private final int[] LIIIShell = XRayTransition.transitionsIntoShell(AtomicShell.LIII);
   static private final int[] MIShell = XRayTransition.transitionsIntoShell(AtomicShell.MI);
   static private final int[] MIIShell = XRayTransition.transitionsIntoShell(AtomicShell.MII);
   static private final int[] MIIIShell = XRayTransition.transitionsIntoShell(AtomicShell.MIII);
   static private final int[] MIVShell = XRayTransition.transitionsIntoShell(AtomicShell.MIV);
   static private final int[] MVShell = XRayTransition.transitionsIntoShell(AtomicShell.MV);
   static private final int[] NIVShell = XRayTransition.transitionsIntoShell(AtomicShell.NIV);
   static private final int[] NVShell = XRayTransition.transitionsIntoShell(AtomicShell.NV);

   private static final TreeMap<String, int[]> mNameToTransitions = initializeMapping();

   private static final String[] mPrecidence = {ELEMENT, K_FAMILY, L_FAMILY, M_FAMILY, N_FAMILY, K_ALPHA, K_BETA, L_ALPHA, L_BETA, L_GAMMA, L_OTHER,
         M_ALPHA, M_BETA, M_GAMMA, M_OTHER, K_SHELL, LI_SHELL, LII_SHELL, LIII_SHELL, MI_SHELL, MII_SHELL, MIII_SHELL, MIV_SHELL, MV_SHELL, NIV_SHELL,
         NV_SHELL,};

   static private TreeMap<String, int[]> initializeMapping() {
      final TreeMap<String, int[]> res = new TreeMap<String, int[]>();
      res.put(K_ALPHA, KAlphaTransitions);
      res.put(K_BETA, KBetaTransitions);
      res.put(L_ALPHA, LAlphaTransitions);
      res.put(L_BETA, LBetaTransitions);
      res.put(L_GAMMA, LGammaTransitions);
      res.put(L_OTHER, LOtherTransitions);
      res.put(M_ALPHA, MAlphaTransitions);
      res.put(M_BETA, MBetaTransitions);
      res.put(M_GAMMA, MGammaTransitions);
      res.put(M_OTHER, MOtherTransitions);
      res.put(K_FAMILY, KFamily);
      res.put(L_FAMILY, LFamily);
      res.put(M_FAMILY, MFamily);
      res.put(N_FAMILY, NFamily);
      res.put(K_SHELL, KShell);
      res.put(LI_SHELL, LIShell);
      res.put(LII_SHELL, LIIShell);
      res.put(LIII_SHELL, LIIIShell);
      res.put(MI_SHELL, MIShell);
      res.put(MII_SHELL, MIIShell);
      res.put(MIII_SHELL, MIIIShell);
      res.put(MIV_SHELL, MIVShell);
      res.put(MV_SHELL, MVShell);
      res.put(NIV_SHELL, NIVShell);
      res.put(NV_SHELL, NVShell);
      res.put(ELEMENT, XRayTransition.ALL_TRANSITIONS);
      return res;
   }

   /**
    * containsExisting - Returns false if this XRayTransitionSet should contains
    * the specified transition (it exists for the current element) but does not.
    * Returns true otherwise (transition doesn't exist or is in set).
    * 
    * @param xrt
    *           An integer in the range [XRayTransitonSet.KA1,
    *           XRayTransitionSet.Last)
    * @return boolean
    */
   public boolean containsExisting(int xrt) {
      assert (XRayTransition.isWellKnown(xrt));
      boolean res = false;
      if ((getElement() != Element.None) && XRayTransition.exists(getElement(), xrt)) {
         for (final XRayTransition sx : mSet)
            if (sx.getTransitionIndex() == xrt) {
               res = true;
               break;
            }
      } else
         res = true;
      return res;
   }

   /**
    * isAllExisting - Returns true if all members of xrts return true against
    * containsExisting; returns false otherwise.
    * 
    * @param xrts
    * @return boolean
    */
   protected boolean containsAllExistingTransitions(int[] xrts) {
      int exists = 0;
      if (mSet.size() > xrts.length)
         return false;
      // Note: mSet can be smaller than xrts
      for (int i = 0; i < xrts.length; ++i) {
         if (XRayTransition.exists(getElement(), xrts[i]))
            ++exists;
         if (!containsExisting(xrts[i]))
            return false;
      }
      return exists == mSet.size();
   }

   private boolean approximateMatch(int[] xrts) {
      final Element elm = getElement();
      int cx = 0;
      for (final int xrt : xrts) {
         final double w = XRayTransition.getWeight(elm, xrt, XRayTransition.NormalizeFamily);
         if (w > mMinWeight) {
            ++cx;
            if (find(xrt) == null)
               return false;
         }
      }
      return cx == mSet.size();
   }

   private void updateMode() {
      mMode = EMPTY;
      if (!mSet.isEmpty()) {
         mMode = COLLECTION;
         for (final String element2 : mPrecidence) {
            final int[] xrts = mNameToTransitions.get(element2);
            if (approximateMatch(xrts)) {
               mMode = element2;
               return;
            }
         }
         if (mSet.size() == 1)
            mMode = TRANSITION;
      }
   }

   public static String getClassNameForTransition(int tr) {
      if (Arrays.binarySearch(KAlphaTransitions, tr) >= 0)
         return K_ALPHA;
      if (Arrays.binarySearch(KBetaTransitions, tr) >= 0)
         return K_BETA;
      if (Arrays.binarySearch(LAlphaTransitions, tr) >= 0)
         return L_ALPHA;
      if (Arrays.binarySearch(LBetaTransitions, tr) >= 0)
         return L_BETA;
      if (Arrays.binarySearch(LGammaTransitions, tr) >= 0)
         return L_GAMMA;
      if (Arrays.binarySearch(LOtherTransitions, tr) >= 0)
         return L_OTHER;
      if (Arrays.binarySearch(MAlphaTransitions, tr) >= 0)
         return M_ALPHA;
      if (Arrays.binarySearch(MBetaTransitions, tr) >= 0)
         return M_BETA;
      if (Arrays.binarySearch(MGammaTransitions, tr) >= 0)
         return M_GAMMA;
      if (Arrays.binarySearch(MOtherTransitions, tr) >= 0)
         return M_OTHER;
      if (Arrays.binarySearch(NFamily, tr) >= 0)
         return "N";
      return "";
   }

   /**
    * Constructs a XRayTransitionSet consisting of all transitions for the
    * specified element between the energy e0 and e1;
    * 
    * @param el
    *           The element
    * @param e0
    *           The lower energy bound
    * @param e1
    *           The upper energy bound
    */
   public XRayTransitionSet(Element el, double e0, double e1) {
      super();
      assert (e0 < e1);
      mMinWeight = 0.0;
      for (int tr = XRayTransition.KA1; tr < XRayTransition.Last; ++tr)
         if (XRayTransition.exists(el, tr))
            try {
               final double e = XRayTransition.getEnergy(el, tr);
               if ((e >= e0) && (e <= e1))
                  mSet.add(new XRayTransition(el, tr));
            } catch (final EPQException e) {
               // never happens...
            }
      updateMode();
   }

   /**
    * Constructs an XRayTransitionSet for the specified element and specific
    * line set. All lines with weights greater than zero are included
    * 
    * @param el
    *           The elemnt
    * @param type
    *           One of x_FAMILY, x_SHELL etc.
    */
   public XRayTransitionSet(Element el, String type) {
      this(el, type, 0.0);
   }

   /**
    * Constructs an XRayTransitionSet for the specified element and specific
    * line set. The line weight (
    * <code>XRayTransition.getWeight(el, xrts[i],XRayTransition.NormalizeKLM)</code>
    * ) must be greater than or equal to minWeight
    * 
    * @param el
    *           The elemnt
    * @param type
    *           One of x_FAMILY, x_SHELL etc.
    * @param minWeight
    *           The minimum KLM line weight to include
    */
   public XRayTransitionSet(Element el, String type, double minWeight) {
      super();
      mMinWeight = minWeight;
      final int[] xrts = mNameToTransitions.get(type);
      if (xrts == null)
         throw new EPQFatalException("Unexpected type in XRayTransitionSet constructor.");
      for (final int xrt : xrts)
         if (XRayTransition.exists(el, xrt) && (XRayTransition.getWeight(el, xrt, XRayTransition.NormalizeKLM) > minWeight))
            mSet.add(new XRayTransition(el, xrt));
      updateMode();
      assert (minWeight >= 0) || mMode.equals(type);
   }

   /**
    * Constructs an XRayTransitionSet for the specified element and specific
    * line set. The line weight (
    * <code>XRayTransition.getWeight(el, xrts[i],XRayTransition.NormalizeKLM)</code>
    * ) must be greater than or equal to minWeight
    * 
    * @param el
    *           The elemnt
    * @param type
    *           One of x_FAMILY, x_SHELL etc.
    * @param minWeight
    *           The minimum KLM line weight to include
    * @param minE
    *           maximum edge energy to include
    * @param maxE
    *           maximum edge energy to include
    */
   public XRayTransitionSet(Element el, String type, double minWeight, double minE, double maxE) throws EPQException {
      super();
      mMinWeight = minWeight;
      final int[] xrts = mNameToTransitions.get(type);
      if (xrts == null)
         throw new EPQFatalException("Unexpected type in XRayTransitionSet constructor.");
      for (final int xrt2 : xrts)
         if (XRayTransition.exists(el, xrt2)) {
            final XRayTransition xrt = new XRayTransition(el, xrt2);
            final double ee = xrt.getEdgeEnergy();
            if ((ee > minE) && (ee < maxE) && (xrt.getWeight(XRayTransition.NormalizeKLM) > minWeight))
               mSet.add(xrt);
         }
      updateMode();
   }

   /**
    * Constructs an XRayTransitionSet for the specified element and specific
    * line set. The line weight (
    * <code>XRayTransition.getWeight(el, xrts[i],XRayTransition.NormalizeKLM)</code>
    * ) must be greater than or equal to minWeight
    * 
    * @param el
    *           The elemnt
    * @param type
    *           One of x_FAMILY, x_SHELL etc.
    * @param minWeight
    *           The minimum KLM line weight to include
    * @param maxE
    *           maximum edge energy to include
    */
   public XRayTransitionSet(Element el, String type, double minWeight, double maxE) throws EPQException {
      this(el, type, minWeight, 0.0, maxE);
   }

   /**
    * Constructs a XRayTransitionSet by adding all the XRayTransition objects in
    * the specified collection. They must all be from the same element.
    * 
    * @param c
    */
   public XRayTransitionSet(Collection<XRayTransition> c) {
      super();
      mMinWeight = 0.0;
      if (!c.isEmpty()) {
         final Element elm = c.iterator().next().getElement();
         for (final XRayTransition xrt : c) {
            if (!xrt.getElement().equals(elm))
               throw new EPQFatalException(MIXED_MESSAGE);
            if (xrt.exists())
               mSet.add(xrt);
         }
      }
      updateMode();
   }

   /**
    * Constructs a XRayTransitionSet from an AtomicShell. (All transitions into
    * the specified shell.)
    */
   public XRayTransitionSet(AtomicShell shell) {
      super();
      mMinWeight = 0.0;
      String name = null;
      switch (shell.getShell()) {
         case AtomicShell.K :
            name = K_SHELL;
            break;
         case AtomicShell.LI :
            name = LI_SHELL;
            break;
         case AtomicShell.LII :
            name = LII_SHELL;
            break;
         case AtomicShell.LIII :
            name = LIII_SHELL;
            break;
         case AtomicShell.MI :
            name = MI_SHELL;
            break;
         case AtomicShell.MII :
            name = MII_SHELL;
            break;
         case AtomicShell.MIII :
            name = MIII_SHELL;
            break;
         case AtomicShell.MIV :
            name = MIV_SHELL;
            break;
         case AtomicShell.MV :
            name = MV_SHELL;
            break;
         case AtomicShell.NIV :
            name = NIV_SHELL;
            break;
         case AtomicShell.NV :
            name = NV_SHELL;
            break;
      }
      if (name == null)
         throw new EPQFatalException("Unexpected shell in XRayTransitionSet constructor.");
      final int[] xrts = mNameToTransitions.get(name);
      assert xrts != null;
      for (final int xrt : xrts)
         if (XRayTransition.exists(shell.getElement(), xrt))
            mSet.add(new XRayTransition(shell.getElement(), xrt));
      updateMode();
   }

   /**
    * Constructs a XRayTransitionSet from Element and transition family. (All
    * transitions in the specified family)
    */
   public XRayTransitionSet(Element el, int family) {
      super();
      mMinWeight = 0.0;
      assert (AtomicShell.isLineFamily(family));
      String name = null;
      switch (family) {
         case AtomicShell.KFamily :
            name = K_FAMILY;
            break;
         case AtomicShell.LFamily :
            name = L_FAMILY;
            break;
         case AtomicShell.MFamily :
            name = M_FAMILY;
            break;
         case AtomicShell.NFamily :
            name = N_FAMILY;
            break;
      }
      if (name == null)
         throw new EPQFatalException("Unexpected shell in XRayTransitionSet constructor.");
      final int[] xrts = mNameToTransitions.get(name);
      assert xrts != null;
      for (final int xrt : xrts)
         if (XRayTransition.exists(el, xrt))
            mSet.add(new XRayTransition(el, xrt));
      updateMode();
   }

   /**
    * Constructs a XRayTransitionSet from a single XRayTransition
    */
   public XRayTransitionSet(XRayTransition xrt) {
      super();
      mMinWeight = 0.0;
      if (xrt.exists())
         mSet.add(xrt);
      updateMode();
   }

   /**
    * Constructs a XRayTransitionSet representing all of an Element's
    * transitions.
    */
   public XRayTransitionSet(Element el) {
      super();
      mMinWeight = 0.0;
      for (int tr = XRayTransition.KA1; tr < XRayTransition.Last; ++tr)
         if (XRayTransition.exists(el, tr))
            mSet.add(new XRayTransition(el, tr));
      updateMode();
   }

   public XRayTransitionSet() {
      super();
      mMinWeight = 0.0;
      mMode = EMPTY;
   }

   @Override
   public Object clone() {
      final XRayTransitionSet xrts = new XRayTransitionSet();
      xrts.mMode = mMode;
      xrts.mSet.addAll(mSet);
      return xrts;
   }

   public void add(XRayTransition xrt) {
      if ((mSet.size() > 0) && (!getElement().equals(xrt.getElement())))
         throw new EPQFatalException(MIXED_MESSAGE);
      if ((!mSet.contains(xrt)) && xrt.exists()) {
         mSet.add(xrt);
         updateMode();
      }
   }

   public void add(Collection<XRayTransition> xrts) {
      for (final XRayTransition xrt : xrts) {
         if ((mSet.size() > 0) && (!getElement().equals(xrt.getElement())))
            throw new EPQFatalException(MIXED_MESSAGE);
         if ((!mSet.contains(xrt)) && xrt.exists())
            mSet.add(xrt);
      }
      updateMode();
   }

   /**
    * remove - Removes the specified transition from this XRayTransitionSet
    * 
    * @param xrt
    */
   public void remove(XRayTransition xrt) {
      mSet.remove(xrt);
      updateMode();
   }

   /**
    * Remove all the XRayTransition objects in xrts from this XRayTransitionSet.
    * 
    * @param xrts
    */
   public void removeAll(XRayTransitionSet xrts) {
      boolean modified = false;
      for (final XRayTransition xrt : xrts)
         if (mSet.contains(xrt)) {
            mSet.remove(xrt);
            modified = true;
         }
      if (modified)
         updateMode();
   }

   /**
    * combine - Add the specified XRayTransitionSet into this XRayTransitionSet.
    * 
    * @param xrts
    */
   public void add(XRayTransitionSet xrts) {
      if (xrts.size() == 0)
         return;
      if ((mSet.size() > 0) && (!getElement().equals(xrts.getElement())))
         throw new EPQFatalException(MIXED_MESSAGE);
      final int sz = mSet.size();
      mSet.addAll(xrts.mSet);
      if (mSet.size() > sz)
         updateMode();
   }

   /**
    * Does this XRayTransitionSet contain the specified XRayTransition?
    * 
    * @param xrt
    * @return boolean
    */
   public boolean contains(XRayTransition xrt) {
      return mSet.contains(xrt);
   }

   /**
    * Does this XRayTransitionSet contain all the transitions in the specified
    * XRayTransitionSet?
    * 
    * @param xrts
    * @return boolean
    */
   public boolean contains(XRayTransitionSet xrts) {
      return mSet.containsAll(xrts.mSet);
   }

   /**
    * Does this XRayTransitionSet contain the specified transition specified by
    * index (XRayTransition.KA1, etc.)
    * 
    * @param transition
    * @return boolean
    */
   public boolean contains(int transition) {
      return find(transition) != null;
   }

   /**
    * Finds the XRayTransition object associated with the specified
    * transition-index.
    * 
    * @param transition
    * @return XRayTransition or null
    */
   public XRayTransition find(int transition) {
      for (final XRayTransition xrt : mSet)
         if (xrt.getTransitionIndex() == transition)
            return xrt;
      return null;
   }

   /**
    * The number of XRayTransition objects in the XRayTransitionSet
    * 
    * @return int
    */
   public int size() {
      return mSet.size();
   }

   /**
    * getElement - Returns the element with which this XRayTransition set is
    * associated.
    */
   public Element getElement() {
      return mSet.isEmpty() ? Element.None : mSet.iterator().next().getElement();
   }

   /**
    * toString - Returns a string of the form "[...]" where ... is a convenient
    * shorthand for the current contents.
    * 
    * @return String
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      // Note: Update parseString to reflect any changes to toString()
      if (mMode == EMPTY)
         return "";
      if (mMode == TRANSITION)
         return mSet.iterator().next().toString();
      if (mMode == COLLECTION) {
         final XRayTransition weighiest = getWeighiestTransition();
         return weighiest.getIUPACName() + " + " + Integer.toString(mSet.size() - 1) + " others";
      }
      assert (mNameToTransitions.get(mMode) != null);
      return getElement().toAbbrev() + " " + mMode;
   }

   public String toParseable() {
      ArrayList<String> strs = new ArrayList<String>();
      for (XRayTransition xrt : mSet)
         strs.add(xrt.toString());
      return String.join(", ", strs);
   }

   /**
    * parseString - The inverse of toParseable.
    * 
    * @param str
    * @return An XRayTransitionSet representing the contents of str
    */
   public static XRayTransitionSet parseString(String str) {
      if (str.length() == 0)
         return new XRayTransitionSet();
      for (final String name : mNameToTransitions.keySet())
         if (str.endsWith(name)) {
            final Element el = Element.byName(str.substring(0, str.length() - name.length()).trim());
            assert (el.isValid());
            return new XRayTransitionSet(el, name);
         }
      // TRANSITION or COLLECTION
      {
         int end = -2;
         final TreeSet<XRayTransition> xrts = new TreeSet<XRayTransition>();
         do {
            final int begin = end + 2;
            end = str.indexOf(", ", begin);
            if (end == -1)
               end = str.length();
            final XRayTransition xrt = XRayTransition.parseString(str.substring(begin, end).trim());
            xrts.add(xrt);
         } while (end != str.length());
         return new XRayTransitionSet(xrts);
      }
   }

   /**
    * getMode - Returns one of EMPTY, TRANSITION, ?_SHELL, ?_FAMILY, ELEMENT, or
    * COLLECTION
    * 
    * @return String
    */
   public String getMode() {
      return mMode;
   }

   /**
    * getTransitions - Get an unmodifiable Set containing the XRayTransition
    * objects represented by this object.
    * 
    * @return An unmodifiable Set of XRayTransition objects
    */
   public Set<XRayTransition> getTransitions() {
      return Collections.unmodifiableSet(mSet);
   }

   /**
    * minEnergy - Returns the transition with the minimum x-ray energy.
    * 
    * @return XRayTransition
    */
   public XRayTransition minEnergy() {
      XRayTransition res = null;
      for (final XRayTransition xrt : mSet)
         try {
            if ((res == null) || (xrt.getEnergy() < res.getEnergy()))
               res = xrt;
         } catch (final EPQException e) {
            // ignore it..
         }
      return res;
   }

   /**
    * maxEnergy - Returns the transition with the maximum x-ray energy.
    * 
    * @return XRayTransition
    */
   public XRayTransition maxEnergy() {
      XRayTransition res = null;
      for (final XRayTransition xrt : mSet)
         try {
            if ((res == null) || (xrt.getEnergy() > res.getEnergy()))
               res = xrt;
         } catch (final EPQException e) {
            // ignore it..
         }
      return res;
   }

   @Override
   public int hashCode() {
      return Objects.hash(mSet, mMode);
   }

   /**
    * Do these two items represent exactly the same set of XRayTransition
    * objects?
    * 
    * @param obj
    * @return boolean
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (obj instanceof XRayTransitionSet) {
         final XRayTransitionSet xrts = (XRayTransitionSet) obj;
         return toString().equals(xrts.toString());
      }
      return false;
   }

   /**
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   @Override
   public int compareTo(XRayTransitionSet xrts) {
      int res = getElement().compareTo(xrts.getElement());
      if (res == 0) {
         // Both sets are kept in sorted order so..
         final Iterator<XRayTransition> i = mSet.iterator();
         final Iterator<XRayTransition> j = xrts.mSet.iterator();
         while ((i.hasNext()) && (j.hasNext())) {
            res = i.next().compareTo(j.next());
            if (res != 0)
               break;
         }
         if (res == 0)
            res = (i.hasNext() ? 1 : (j.hasNext() ? -1 : 0));
      }
      return res;
   }

   /**
    * Returns an Iterator to iterate through the XRayTransitions. Implements
    * Iterable&lt;XRayTransition&gt;.
    * 
    * @return Iterator&lt;XRayTransition&gt;
    */
   @Override
   public Iterator<XRayTransition> iterator() {
      return mSet.iterator();
   }

   enum NormalizeMode {
      NORMALIZED, ABSOLUTE
   };

   /**
    * getWeightiestTransitions - Returns the transitions in the set with weight
    * larger than the specified weight. The mode determines whether the weights
    * are normalized by the weightiest transition in the set (mode==NORMALIZED)
    * or not (mode==ABSOLUTE).
    * 
    * @param weight
    *           In [0.0,1.0] (0.0-&gt;All, 1.0 &amp; NORMALIZED -&gt;
    *           Weightiest)
    * @param mode
    *           One of NORMALIZED or ABSOLUTE
    * @return XRayTransitionSet
    */
   public XRayTransitionSet getWeightiestTransitions(double weight, NormalizeMode mode) {
      assert (weight >= 0.0);
      assert (weight <= 1.0);
      final TreeSet<XRayTransition> ts = new TreeSet<XRayTransition>();
      double norm = 1.0;
      if (mode == NormalizeMode.NORMALIZED) {
         norm = 0.0;
         for (final XRayTransition xrt : this) {
            final double w = xrt.getWeight(XRayTransition.NormalizeFamily);
            if (w > norm)
               norm = w;
         }
      }
      if (norm > 0.0)
         for (final XRayTransition xrt : this)
            if ((xrt.getWeight(XRayTransition.NormalizeFamily) / norm) >= weight)
               ts.add(xrt);
      return new XRayTransitionSet(ts);
   }

   public double sumWeight() {
      double sum = 0.0;
      for (final XRayTransition xrt : this)
         sum += xrt.getWeight(XRayTransition.NormalizeFamily);
      return sum;
   }

   /**
    * Does this XRayTransitionSet contain no transitions?
    * 
    * @return boolean
    */
   public boolean isEmpty() {
      return mSet.isEmpty();
   }

   /**
    * getWeighiestTransition - Returns the single weightiest transition.
    * 
    * @return XRayTransition
    */
   public XRayTransition getWeighiestTransition() {
      double max = 0.0;
      XRayTransition res = null;
      for (final XRayTransition xrt : mSet) {
         final double w = xrt.getWeight(XRayTransition.NormalizeFamily);
         if (w >= max) {
            max = w;
            res = xrt;
         }
      }
      return res;
   }

   /**
    * Returns the sum of weights of transitions.
    * 
    * @return XRayTransition
    */
   public double getSumWeight() {
      double sum = 0.0;
      for (final XRayTransition xrt : mSet)
         sum += xrt.getWeight(XRayTransition.NormalizeFamily);
      return sum;
   }

   /**
    * getWeighiestTransition - Returns the single weightiest transition in this
    * set associated with the specified element.
    * 
    * @param elm
    * @return XRayTransition
    */
   public XRayTransition getWeighiestTransition(Element elm) {
      double max = 0.0;
      XRayTransition res = null;
      for (final XRayTransition xrt : mSet) {
         final double w = xrt.getWeight(XRayTransition.NormalizeFamily);
         if (w >= max) {
            max = w;
            res = xrt;
         }
      }
      return res;
   }

   static public final String[] allBasicTransitionSets = {XRayTransitionSet.K_ALPHA, XRayTransitionSet.K_BETA, XRayTransitionSet.K_FAMILY,
         XRayTransitionSet.L_ALPHA, XRayTransitionSet.L_BETA, XRayTransitionSet.L_FAMILY, XRayTransitionSet.M_ALPHA, XRayTransitionSet.M_BETA,
         XRayTransitionSet.M_GAMMA, XRayTransitionSet.M_FAMILY};

   /**
    * Returns an ArrayList of Strings containing all the transition sets that
    * can be excited by an beam of the specified energy for the specified
    * element.
    * 
    * @param el
    *           An element
    * @param eB
    *           The incident beam energy
    * @return ArrayList A list of String objects such as K_ALPHA, K_BETA, ...,
    *         M_FAMILY
    */
   static public ArrayList<String> getBasicFamilies(Element el, double eB) {
      final ArrayList<String> res = new ArrayList<String>();
      if (XRayTransition.exists(el, XRayTransition.KA1) && (eB > AtomicShell.getEdgeEnergy(el, AtomicShell.K))) {
         res.add(XRayTransitionSet.K_ALPHA);
         if (XRayTransition.exists(el, XRayTransition.KB1)) {
            res.add(XRayTransitionSet.K_BETA);
            res.add(XRayTransitionSet.K_FAMILY);
         }
      }
      if (XRayTransition.exists(el, XRayTransition.LA1) && (eB > AtomicShell.getEdgeEnergy(el, AtomicShell.LIII))) {
         res.add(XRayTransitionSet.L_ALPHA);
         if (XRayTransition.exists(el, XRayTransition.LB1) && (eB > AtomicShell.getEdgeEnergy(el, AtomicShell.LII))) {
            res.add(XRayTransitionSet.L_BETA);
            res.add(XRayTransitionSet.L_FAMILY);
         }
      }
      if (XRayTransition.exists(el, XRayTransition.MA1) && (eB > AtomicShell.getEdgeEnergy(el, AtomicShell.MV))) {
         res.add(XRayTransitionSet.M_ALPHA);
         if (XRayTransition.exists(el, XRayTransition.MB) && (eB > AtomicShell.getEdgeEnergy(el, AtomicShell.MIV))) {
            res.add(XRayTransitionSet.M_BETA);
            res.add(XRayTransitionSet.M_FAMILY);
         }
         if (XRayTransition.exists(el, XRayTransition.MG) && (eB > AtomicShell.getEdgeEnergy(el, AtomicShell.MIII)))
            res.add(XRayTransitionSet.M_GAMMA);
      }
      return res;
   }

   /**
    * Standard set operation: Returns the set of transitions in either xrts1 or
    * xrts2.
    * 
    * @param xrts1
    * @param xrts2
    * @return XRayTransitionSet
    */
   static public XRayTransitionSet union(XRayTransitionSet xrts1, XRayTransitionSet xrts2) {
      final XRayTransitionSet res = new XRayTransitionSet();
      res.mSet.addAll(xrts1.mSet);
      res.mSet.addAll(xrts2.mSet);
      res.updateMode();
      return res;
   }

   /**
    * Standard set operation: Returns the set of transitions contained in both a
    * and b.
    * 
    * @param a
    * @param b
    * @return XRayTransitionSet
    */
   static public XRayTransitionSet intersection(XRayTransitionSet a, XRayTransitionSet b) {
      final XRayTransitionSet res = new XRayTransitionSet();
      for (final XRayTransition xrt : a)
         if (b.contains(xrt))
            res.mSet.add(xrt);
      res.updateMode();
      return res;
   }

   /**
    * Standard set operation: Returns those transitions which are in xrts1 but
    * not in xrts2.
    * 
    * @param xrts1
    * @param xrts2
    * @return XRayTransitionSet
    */
   static public XRayTransitionSet difference(XRayTransitionSet xrts1, XRayTransitionSet xrts2) {
      final XRayTransitionSet res = new XRayTransitionSet();
      res.mSet.addAll(xrts1.mSet);
      for (final XRayTransition xrt : xrts1.mSet)
         if (res.contains(xrt))
            res.mSet.remove(xrt);
      res.updateMode();
      return res;
   }

   static public double similarity(XRayTransitionSet xrts1, XRayTransitionSet xrts2) {
      final Set<XRayTransition> union = new TreeSet<XRayTransition>();
      union.addAll(xrts1.getTransitions());
      union.addAll(xrts2.getTransitions());
      double score = 0.0, sum = 0.0;
      for (final XRayTransition xrt : union) {
         final double w = xrt.getWeight(XRayTransition.NormalizeFamily);
         sum += w;
         if (xrts1.contains(xrt) && xrts2.contains(xrt))
            score += w;
      }
      return sum > 0.0 ? score / sum : 0.0;
   }

   public boolean isValid() {
      for (final XRayTransition xrt : mSet)
         if (!xrt.isWellKnown()) {
            System.err.println("Not valid: " + xrt + " in " + mSet);
            return false;
         }
      return true;
   }
}
