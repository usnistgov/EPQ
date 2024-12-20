package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.CSVReader;
import gov.nist.microanalysis.Utility.TextUtilities;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * A class for managing the various different ways that x-ray transitions are
 * identified and labeled.
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

final public class XRayTransition implements Comparable<XRayTransition>, Cloneable {

   private static final String[] mTransitionNames = {"K\u03B11", "K\u03B12", "K\u03B21", "K\u03B22", "K\u03B23", "K\u03B24", "K\u03B25", "L3N2",
         "L3N3", "L3O2", "L3O3", "L3P1", "L\u03B11", "L\u03B12", "L\u03B215", "L\u03B22", "L\u03B25", "L\u03B26", "L\u03B27", "L\u2113", "Ls", "Lt",
         "Lu", "L2M2", "L2M5", "L2N2", "L2N3", "L2N5", "L2O2", "L2O3", "L2P2", "L\u03B21", "L\u03B217", "L\u03B31", "L\u03B35", "L\u03B36",
         "L\u03B38", "L\u03B7", "L\u03BD", "L1M1", "L1N1", "L1N4", "L1O1", "L1O4/L1O5", "L\u03B210", "L\u03B23", "L\u03B24", "L\u03B29", "L\u03B32",
         "L\u03B311", "L\u03B33", "L\u03B34", "L\u03B34p", "M1N2", "M1N3", "M2M4", "M2N1", "M2N4", "M2O4", "M3M4", "M3M5", "M3N1", "M3N4", "M3O1",
         "M3O4", "M3O5", "M\u03B3", "M4N3", "M4O2", "M\u03B2", "M\u03B62", "M5O3", "M\u03B11", "M\u03B12", "M\u03B61", "N4N6", "N5N6/N5N7", "Last",
         "Unnamed", "None"};

   // named transitions (K shell)
   public static final int KA1 = 0;
   public static final int KA2 = 1;
   public static final int KB1 = 2;
   public static final int KB2 = 3;
   public static final int KB3 = 4;
   public static final int KB4 = 5;
   public static final int KB5 = 6;
   // named transitions (L shell)
   public static final int L3N2 = 7;
   public static final int L3N3 = 8;
   public static final int L3O2 = 9;
   public static final int L3O3 = 10;
   public static final int L3P1 = 11;
   public static final int LA1 = 12;
   public static final int LA2 = 13;
   public static final int LB15 = 14;
   public static final int LB2 = 15;
   public static final int LB5 = 16;
   public static final int LB6 = 17;
   public static final int LB7 = 18;
   public static final int Ll = 19;
   public static final int Ls = 20;
   public static final int Lt = 21;
   public static final int Lu = 22;
   public static final int L2M2 = 23;
   public static final int L2M5 = 24;
   public static final int L2N2 = 25;
   public static final int L2N3 = 26;
   public static final int L2N5 = 27;
   public static final int L2O2 = 28;
   public static final int L2O3 = 29;
   public static final int L2P2 = 30; // Also L2P3
   public static final int LB1 = 31;
   public static final int LB17 = 32;
   public static final int LG1 = 33;
   public static final int LG5 = 34;
   public static final int LG6 = 35;
   public static final int LG8 = 36;
   public static final int Ln = 37;
   public static final int Lv = 38;
   public static final int L1M1 = 39;
   public static final int L1N1 = 40;
   public static final int L1N4 = 41;
   public static final int L1O1 = 42;
   public static final int L1O4 = 43; // Also L1O5
   public static final int LB10 = 44;
   public static final int LB3 = 45;
   public static final int LB4 = 46;
   public static final int LB9 = 47;
   public static final int LG2 = 48;
   public static final int LG11 = 49;
   public static final int LG3 = 50;
   public static final int LG4 = 51;
   public static final int LG4p = 52;
   // named transitions (M shell)
   public static final int M1N2 = 53;
   public static final int M1N3 = 54;
   public static final int M2M4 = 55;
   public static final int M2N1 = 56;
   public static final int M2N4 = 57;
   public static final int M2O4 = 58;
   public static final int M3M4 = 59;
   public static final int M3M5 = 60;
   public static final int M3N1 = 61;
   public static final int M3N4 = 62;
   public static final int M3O1 = 63;
   public static final int M3O4 = 64;
   public static final int M3O5 = 65;
   public static final int MG = 66;
   public static final int M4N3 = 67;
   public static final int M4O2 = 68;
   public static final int MB = 69;
   public static final int MZ2 = 70;
   public static final int M5O3 = 71;
   public static final int MA1 = 72;
   public static final int MA2 = 73;
   public static final int MZ1 = 74;
   // named transitions (N shell)
   public static final int N4N6 = 75;
   public static final int N5N6 = 76; // Also N5N7
   // other names
   public static final int Last = 77;
   public static final int Unnamed = 200;
   public static final int None = -1;

   public static final int[] ALL_TRANSITIONS = {KA1, KA2, KB1, KB2, KB3, KB4, KB5, L3N2, L3N3, L3O2, L3O3, L3P1, LA1, LA2, LB15, LB2, LB5, LB6, LB7,
         Ll, Ls, Lt, Lu, L2M2, L2M5, L2N2, L2N3, L2N5, L2O2, L2O3, L2P2, LB1, LB17, LG1, LG5, LG6, LG8, Ln, Lv, L1M1, L1N1, L1N4, L1O1, L1O4, LB10,
         LB3, LB4, LB9, LG2, LG11, LG3, LG4, LG4p, M1N2, M1N3, M2M4, M2N1, M2N4, M2O4, M3M4, M3M5, M3N1, M3N4, M3O1, M3O4, M3O5, MG, M4N3, M4O2, MB,
         MZ2, M5O3, MA1, MA2, MZ1,
         // named transitions (N shell)
         N4N6, N5N6};

   // These represent the most common source shells by Seigbahn name. However,
   // occasionally this shell is not available
   // like LIII in C, so the LII becomes the Ka source shell. This is
   // implemented in getSourceShell(Element, trans).
   private static final int[] mSourceShell = {AtomicShell.LIII, AtomicShell.LII, AtomicShell.MIII, AtomicShell.NIII, // KB2
         // (also
         // NII)
         AtomicShell.MII, AtomicShell.NV, AtomicShell.MV, AtomicShell.NII, AtomicShell.NIII, AtomicShell.OII, // L3O2
         AtomicShell.OIII, AtomicShell.PI, AtomicShell.MV, AtomicShell.MIV, AtomicShell.NIV, AtomicShell.NV, AtomicShell.OIV, // LB5
                                                                                                                              // (also
                                                                                                                              // OV)
         AtomicShell.NI, AtomicShell.OI, AtomicShell.MI, AtomicShell.MIII, AtomicShell.MII, AtomicShell.NVI, AtomicShell.MII, // L2M2
         AtomicShell.MV, AtomicShell.NII, AtomicShell.NIII, AtomicShell.NV, AtomicShell.OII, AtomicShell.OIII, // L2O3
         AtomicShell.PII, AtomicShell.MIV, AtomicShell.MIII, AtomicShell.NIV, AtomicShell.NI, AtomicShell.OIV, AtomicShell.OI, // LG8
         AtomicShell.MI, AtomicShell.NVI, AtomicShell.MI, AtomicShell.NI, AtomicShell.NIV, AtomicShell.OI, AtomicShell.OIV, // L1O4
                                                                                                                            // (also
                                                                                                                            // OV)
         AtomicShell.MIV, AtomicShell.MIII, AtomicShell.MII, AtomicShell.MV, AtomicShell.NII, AtomicShell.NV, // LG11
         AtomicShell.NIII, AtomicShell.OIII, AtomicShell.OII, AtomicShell.NII, AtomicShell.NIII, AtomicShell.MIV, // M2M4
         AtomicShell.NI, AtomicShell.NIV, AtomicShell.OIV, AtomicShell.MIV, AtomicShell.MV, AtomicShell.NI, AtomicShell.NIV, // M3N4
         AtomicShell.OI, AtomicShell.OIV, AtomicShell.OV, AtomicShell.NV, AtomicShell.NIII, AtomicShell.OII, AtomicShell.NVI, // MB
         AtomicShell.NII, AtomicShell.OIII, AtomicShell.NVII, AtomicShell.NVI, AtomicShell.NIII, AtomicShell.NVI, // N4N6
         AtomicShell.NVI
         // N5N6 (also NVII)
   };

   private static final int[] mDestinationShell = {AtomicShell.K, AtomicShell.K, AtomicShell.K, AtomicShell.K, AtomicShell.K, // KB3
         AtomicShell.K, AtomicShell.K, AtomicShell.LIII, AtomicShell.LIII, AtomicShell.LIII, AtomicShell.LIII, // L3O3
         AtomicShell.LIII, AtomicShell.LIII, AtomicShell.LIII, AtomicShell.LIII, AtomicShell.LIII, AtomicShell.LIII, // LB5
         // (also
         // OV)
         AtomicShell.LIII, AtomicShell.LIII, AtomicShell.LIII, AtomicShell.LIII, AtomicShell.LIII, AtomicShell.LIII, // Lu
         // (also
         // NVII)
         AtomicShell.LII, AtomicShell.LII, AtomicShell.LII, AtomicShell.LII, AtomicShell.LII, AtomicShell.LII, // L2O2
         AtomicShell.LII, AtomicShell.LII, AtomicShell.LII, AtomicShell.LII, AtomicShell.LII, AtomicShell.LII, // LG5
         AtomicShell.LII, AtomicShell.LII, AtomicShell.LII, AtomicShell.LII, AtomicShell.LI, AtomicShell.LI, AtomicShell.LI, // L1N4
         AtomicShell.LI, AtomicShell.LI, AtomicShell.LI, AtomicShell.LI, AtomicShell.LI, AtomicShell.LI, AtomicShell.LI, // LG2
         AtomicShell.LI, AtomicShell.LI, AtomicShell.LI, AtomicShell.LI, AtomicShell.MI, AtomicShell.MI, AtomicShell.MII, // M2M4
         AtomicShell.MII, AtomicShell.MII, AtomicShell.MII, AtomicShell.MIII, AtomicShell.MIII, AtomicShell.MIII, // M3N1
         AtomicShell.MIII, AtomicShell.MIII, AtomicShell.MIII, AtomicShell.MIII, AtomicShell.MIII, AtomicShell.MIV, // M4N3
         AtomicShell.MIV, AtomicShell.MIV, AtomicShell.MIV, AtomicShell.MV, AtomicShell.MV, AtomicShell.MV, AtomicShell.MV, // MZ1
         AtomicShell.NIV, AtomicShell.NV
         // N5N6 (also NVII)
   };
   private static double[][] mWeight;
   private static double[][] mDestNormalization; // [Element][K,LI,LII,...,NI]
   private static double[][] mFamNormalization; // [Element][KFamily,...,MFamily]
   private static double[][] mKLMNormalization; // [Element][KFamily,...,MFamily]

   private int mTransition = None;
   private final AtomicShell mSource;
   private final AtomicShell mDestination;

   /**
    * The weights are as they were read from the data table.
    */
   public static final int NormalizeDefault = 0;
   /**
    * Normalize the weights so the the total weight in family (KFamily, LFamily,
    * MFamily) sums to 1.0. This scheme is used when it is only known that one
    * of a family of lines has been ionized.
    */
   public static final int NormalizeFamily = 1;
   /**
    * Normalize the weights do the total weight representing transitions into a
    * single destination shell sums to 1.0. This scheme is used to normalize the
    * transition weight when a defined shell is known to be ionized.
    */
   public static final int NormalizeDestination = 2;

   /**
    * Normalize the weights so that the heaviest weight KLM line has an
    * amplitude of 1.0. This normalization scheme is usually used only for
    * displayed KLM lines.
    */
   public static final int NormalizeKLM = 3;

   private static void readLineWeights() {
      try {
         synchronized (XRayTransition.class) {
            if (mWeight == null) {
               final double[][] wgt = (new CSVReader.ResourceReader("LineWeights.csv", true)).getResource(XRayTransition.class);
               final double[][] destNorm = new double[wgt.length][(AtomicShell.NV - AtomicShell.K) + 1];
               final double[][] famNorm = new double[wgt.length][(AtomicShell.NFamily - AtomicShell.KFamily) + 1];
               final double[][] klmNorm = new double[wgt.length][(AtomicShell.NFamily - AtomicShell.KFamily) + 1];
               // Normalize the transitions into each destination shell to a
               // weight of
               // 1.0
               // This normalization is selected so that the relative intensity
               // of the
               // transitions into a shell
               // is determined by the ionization cross section out of the
               // shell.
               // el=0->H, el=1->He etc.
               for (int el = 0; el < wgt.length; ++el) {
                  final double[] ew = wgt[el], dNorm = destNorm[el], kNorm = klmNorm[el], fNorm = famNorm[el];
                  if (ew != null) {
                     // Normalize the K, L and M families to all have the same
                     // maximum intensity
                     assert (ew.length <= ((XRayTransition.N5N6 - XRayTransition.KA1) + 1));
                     for (int xrt = XRayTransition.KA1; xrt < (XRayTransition.KA1 + ew.length); ++xrt) {
                        final int f = XRayTransition.getFamily(xrt);
                        if (ew[xrt - XRayTransition.KA1] > kNorm[f - AtomicShell.KFamily])
                           kNorm[f - AtomicShell.KFamily] = ew[xrt - XRayTransition.KA1];
                        fNorm[f - AtomicShell.KFamily] += ew[xrt - XRayTransition.KA1];
                     }
                     // Eliminate pesky divide-by-zero errors...
                     for (int f = AtomicShell.KFamily; f <= AtomicShell.NFamily; ++f) {
                        if (fNorm[f - AtomicShell.KFamily] == 0.0)
                           fNorm[f - AtomicShell.KFamily] = 1.0;
                        if (kNorm[f - AtomicShell.KFamily] == 0.0)
                           kNorm[f - AtomicShell.KFamily] = 1.0;
                     }
                     // Compute the shell normalization factors
                     for (int xrt = XRayTransition.KA1; xrt < (XRayTransition.KA1 + ew.length); ++xrt)
                        dNorm[XRayTransition.getDestinationShell(xrt) - AtomicShell.K] += ew[xrt - XRayTransition.KA1];
                     for (int sh = AtomicShell.K; sh <= AtomicShell.NV; ++sh)
                        if (dNorm[sh - AtomicShell.K] == 0.0)
                           dNorm[sh - AtomicShell.K] = 1.0; // eliminate pesky
                     // divide
                     // by zero error
                  }
               }
               mWeight = wgt;
               mDestNormalization = destNorm;
               mFamNormalization = famNorm;
               mKLMNormalization = klmNorm;
            }
         }
      } catch (final Exception ex) {
         throw new EPQFatalException("Fatal error while attempting to load the line weights data file.");
      }
   }

   private static int getSourceShell(Element elm, int trans) {
      int res = mSourceShell[trans];
      // Some transitions are not consisent with the standard naming schemes.
      // For example, C Ka is K-L2 and not K-L3 because L3 doesn't exist in C
      // and Ka refers to the brightest K line.
      switch (elm.getAtomicNumber()) {
         // LIII and LII don't exist
         case Element.elmLi :
         case Element.elmBe :
            if (res == AtomicShell.LIII)
               res = AtomicShell.LI;
            if (res == AtomicShell.LII)
               res = AtomicShell.LI;
            break;
         // LIII doesn't exist
         case Element.elmB :
         case Element.elmC :
            if (res == AtomicShell.LIII)
               res = AtomicShell.LII;
            break;
         case Element.elmAl :
         case Element.elmSi :
            if (res == AtomicShell.MIII)
               res = AtomicShell.MII;
            break;
         case Element.elmSc :
         case Element.elmTi :
         case Element.elmV :
            if (res == AtomicShell.MV)
               res = AtomicShell.MIV;
            break;
         case Element.elmGa :
         case Element.elmGe :
            if (res == AtomicShell.NIII)
               res = AtomicShell.NII;
            break;
         case Element.elmKr :
            if (res == AtomicShell.NV)
               res = AtomicShell.NIII;
            break;
         case Element.elmY :
         case Element.elmZr :
            if (res == AtomicShell.NV)
               res = AtomicShell.NIV;
            break;
         case Element.elmNb :
         case Element.elmMo :
            if (res == AtomicShell.NV)
               res = AtomicShell.NIV;
            if (res == AtomicShell.OII)
               res = AtomicShell.OI;
            break;
         case Element.elmTc :
         case Element.elmRu :
         case Element.elmRh :
         case Element.elmPd :
         case Element.elmAg :
            if (res == AtomicShell.OII)
               res = AtomicShell.OI;
            break;
         case Element.elmCd :
         case Element.elmIn :
            if (res == AtomicShell.OII)
               res = AtomicShell.OI;
            if (res == AtomicShell.OIII)
               res = AtomicShell.OI;
            break;
         case Element.elmSn :
            if (res == AtomicShell.OIII)
               res = AtomicShell.OI;
            break;
         case Element.elmPr :
         case Element.elmNd :
         case Element.elmPm :
         case Element.elmSm :
         case Element.elmEu :
            if (res == AtomicShell.NVII)
               res = AtomicShell.NVI;
            break;
         case Element.elmYb :
            if (res == AtomicShell.OIV)
               res = AtomicShell.OIII;
            break;
         case Element.elmW :
            if (res == AtomicShell.OV)
               res = AtomicShell.OIV;
            break;
         case Element.elmAu :
         case Element.elmHg :
            if (res == AtomicShell.PI)
               res = AtomicShell.OVIII;
            break;
         case Element.elmTl :
         case Element.elmPb :
            if (res == AtomicShell.PI)
               res = AtomicShell.OIX;
            break;
         case Element.elmRa :
            if (res == AtomicShell.PII)
               res = AtomicShell.PI;
            break;

      }
      return res;
   }

   /**
    * XRayTransition - Create an object corresponding to a specific x-ray
    * transition in a specific element.
    * 
    * @param el
    *           Element
    * @param trans
    *           int - One of the named transition KA1 to N5N6
    */
   public XRayTransition(Element el, int trans) {
      assert (el != null);
      assert (trans >= KA1) && (trans < Last) : "trans=" + Integer.toString(trans);
      assert (mDestinationShell.length == ((N5N6 - KA1) + 1));
      assert (mSourceShell.length == ((N5N6 - KA1) + 1));
      mTransition = trans;
      mSource = new AtomicShell(el, getSourceShell(el, trans));
      mDestination = new AtomicShell(el, mDestinationShell[trans]);
   }

   /**
    * XRayTransition - Create an object corresponding to a specific x-ray
    * transition in a specific element. This method will swap src and
    * destination to make certain that dest is more tightly bound than src.
    * 
    * @param element
    *           Element
    * @param src
    *           int - One of the AtomicShell.K to AtomicShell.PXI
    * @param dest
    *           int - One of the AtomicShell.K to AtomicShell.PXI
    */
   public XRayTransition(Element element, int src, int dest) {
      assert (element != null);
      assert (AtomicShell.isValid(src));
      assert (AtomicShell.isValid(dest) && (dest != AtomicShell.Continuum));
      if (dest == src)
         throw new EPQFatalException("No transition is possible if the source and destination are equal. " + dest + " == " + src);
      if (dest > src) {
         final int tmp = src;
         src = dest;
         dest = tmp;
      }
      mTransition = TransitionFromShells(src, dest);
      // figure out what the transition is from the src and dest
      mSource = new AtomicShell(element, src);
      mDestination = new AtomicShell(element, dest);
   }

   /**
    * XRayTransition - Create an object corresponding to a transition between
    * the specified destination (dest) shell and the specific source shell
    * (srcShell)
    * 
    * @param dest
    *           AtomicShell - The inner most shell involved in the transition.
    * @param srcShell
    *           int - One of the AtomicShell.K to AtomicShell.PXI
    */
   public XRayTransition(AtomicShell dest, int srcShell) {
      this(dest.getElement(), srcShell, dest.getShell());
   }

   /**
    * getTransitionIndex - Get the index associated with this transition (KA1,
    * KA2,....)
    * 
    * @return int
    */
   public int getTransitionIndex() {
      return mTransition;
   }

   /**
    * getSourceShell - Get the (typically valence) shell from which the electron
    * starts its transition to the (typically core) shell.
    * 
    * @return int
    */
   public int getSourceShell() {
      return mSource.getShell();
   }

   /**
    * getSource - Get the higher energy shell from which the electron its
    * transition into the vacant core shell.
    * 
    * @return AtomicShell
    */
   public AtomicShell getSource() {
      return mSource;
   }

   /**
    * getSourceShell - Get the (typically valence) shell from which the electron
    * starts its transition to the (typically core) shell for the specified
    * named transition.
    * 
    * @param xrt
    *           - a named transition
    * @return int
    */
   public static int getSourceShell(int xrt) {
      // assert isWellKnown(xrt);
      return mSourceShell[xrt];
   }

   /**
    * getDestinationShell - Get the shell to which the electron jumps during the
    * x-ray emission process. The destination shell is typically a core
    * electron.
    * 
    * @return int
    */
   public int getDestinationShell() {
      return mTransition == Unnamed ? mDestination.getShell() : mDestinationShell[mTransition];
   }

   /**
    * getDestination - Get the shell to which the electron jumps during the
    * x-ray emission process. The destination shell is typically a core
    * electron.
    * 
    * @return AtomicShell
    */
   public AtomicShell getDestination() {
      return mDestination;
   }

   /**
    * getDestinationShell - Get the shell to which the electron jumps during the
    * x-ray emission process. The destination shell is typically a core
    * electron.
    * 
    * @param xrt
    *           - a named transition
    * @return int
    */
   public static int getDestinationShell(int xrt) {
      // assert isWellKnown(xrt);
      return mDestinationShell[xrt];
   }

   /**
    * getElement - Get the element associated with this transition.
    * 
    * @return Element
    */
   public Element getElement() {
      return mSource.getElement();
   }

   /**
    * isWellKnown - In this transition one of the well-known named transitions.
    * 
    * @return boolean
    */
   public boolean isWellKnown() {
      return (mTransition >= KA1) && (mTransition < Last);
   }

   /**
    * isWellKnown - In this transition one of the well-known named transitions.
    * 
    * @param trans
    *           int - a transition in the range [KA1,Last)
    * @return boolean
    */
   public static boolean isWellKnown(int trans) {
      return (trans >= KA1) && (trans < Last);
   }

   @Override
   public String toString() {
      return getIUPACName();
   }

   /**
    * parseString - The inverse of toString()
    * 
    * @param str
    * @return The XRayTransition represented by str
    */
   public static XRayTransition parseString(String str) {
      final int p = str.indexOf(" ");
      final int q = str.indexOf("-");
      if ((p == -1) || (q == -1))
         throw new EPQFatalException("Unable to parse " + str + " as an XRayTransition.");
      final Element el = Element.byName(str.substring(0, p));
      final int dest = AtomicShell.parseIUPACName(str.substring(p + 1, q));
      final int src = AtomicShell.parseIUPACName(str.substring(q + 1));
      assert (dest != 1001) && (src != 1001) : str.substring(p + 1, q) + " and " + str.substring(q + 1);
      if (!AtomicShell.exists(el, dest))
         throw new EPQFatalException("The destination shell is not occupied in the ground state.");
      if (!AtomicShell.exists(el, src))
         throw new EPQFatalException("The source shell is not occupied in the ground state.");
      return new XRayTransition(el, src, dest);
   }

   /**
    * getIUPACName - Returns the IUPAC name for the current transition.
    * 
    * @return String
    */
   public String getIUPACName() {
      return mDestination.getIUPACName() + "-" + AtomicShell.getIUPACName(mSource.getShell());
   }

   public String getSiegbahnName() {
      return isWellKnown() ? getElement().toAbbrev() + " " + mTransitionNames[mTransition] : getIUPACName();
   }

   public static String removeGreek(String str) {
      String res = TextUtilities.replace(str, "\u03B1", "alpha");
      res = TextUtilities.replace(res, "\u03B2", "beta");
      res = TextUtilities.replace(res, "\u03B3", "gamma");
      res = TextUtilities.replace(res, "\u03B7", "eta");
      res = TextUtilities.replace(res, "\u03B6", "zeta");
      res = TextUtilities.replace(res, "\u2113", "l");
      return res;
   }

   /**
    * getIUPACName - Returns the IUPAC name for the specified transition.
    * 
    * @param xrt
    *           - The index of an x-ray transition
    * @return String
    */
   static public String getIUPACName(int xrt) {
      return xrt != Unnamed ? AtomicShell.getIUPACName(getDestinationShell(xrt)) + "-" + AtomicShell.getIUPACName(getSourceShell(xrt)) : "Unnamed";
   }

   static public String getSiegbahnName(int xrt) {
      return isWellKnown(xrt) ? mTransitionNames[xrt] : "Unnamed";
   }

   /**
    * getEnergy - Returns the energy of the transition in Joules.
    * 
    * @throws EPQException
    * @return double
    */
   public double getEnergy() throws EPQException {
      return AlgorithmUser.getDefaultTransitionEnergy().compute(this);
   }

   public boolean energyIsAvailable() {
      return AlgorithmUser.getDefaultTransitionEnergy().isSupported(this);
   }

   /**
    * getEnergy_eV - Returns the energy of the transition in eV.
    * 
    * @throws EPQException
    * @return double
    */
   public double getEnergy_eV() throws EPQException {
      if (mSource.getShell() == AtomicShell.Continuum)
         return FromSI.eV(mSource.getEdgeEnergy());
      return FromSI.eV(AlgorithmUser.getDefaultTransitionEnergy().compute(this));
   }

   /**
    * getEnergy - Get the x-ray energy associated with the specified transition
    * for the specified Element.
    * 
    * @param el
    *           Element
    * @param trans
    *           int
    * @return double - energy in Joules
    * @throws EPQException
    */
   public static double getEnergy(Element el, int trans) throws EPQException {
      final XRayTransition xrt = new XRayTransition(el, trans);
      return xrt.getEnergy();
   }

   /**
    * getEdgeEnergy - Get the edge energy associated with the specified
    * transition for the specified Element.
    * 
    * @param el
    *           Element
    * @param trans
    *           int
    * @return double - energy in Joules
    */
   public static double getEdgeEnergy(Element el, int trans) {
      final XRayTransition xrt = new XRayTransition(el, trans);
      return xrt.getEdgeEnergy();

   }

   /**
    * getEdgeEnergy - Returns the mininum electron energy required to excite
    * this transition.
    * 
    * @return double
    */
   public double getEdgeEnergy() {
      return AlgorithmUser.getDefaultEdgeEnergy().compute(mDestination);
   }

   /**
    * getWeight - Gets the transition weight associated with this transition.
    * The mode determines how the weights are normalized. Depending upon the
    * use, various different wieghting schemes can be appropriate.
    * 
    * @param mode
    *           int One of DefaultNormalization, NormalizeFamily, or
    *           NormalizeDestination
    * @return double
    * @throws EPQFatalException
    *            - When weight data is unavailable
    */
   public double getWeight(int mode) {
      // The static table mWeight is loaded on demand from LineWeights.csv
      if (mWeight == null)
         readLineWeights();
      if (isWellKnown(mTransition)) {
         final int ani = mSource.getElement().getAtomicNumber() - 1;
         switch (mode) {
            case NormalizeDefault :
               return (mWeight[ani] != null) && (mTransition < mWeight[ani].length) ? mWeight[ani][mTransition] : 0.0;
            case NormalizeFamily :
               return (mWeight[ani] != null) && (mTransition < mWeight[ani].length)
                     ? mWeight[ani][mTransition] / mFamNormalization[ani][mDestination.getFamily() - AtomicShell.KFamily]
                     : 0.0;
            case NormalizeDestination :
               return (mWeight[ani] != null) && (mTransition < mWeight[ani].length)
                     ? mWeight[ani][mTransition] / mDestNormalization[ani][mDestination.getShell() - AtomicShell.K]
                     : 0.0;
            case NormalizeKLM :
               return (mWeight[ani] != null) && (mTransition < mWeight[ani].length)
                     ? mWeight[ani][mTransition] / mKLMNormalization[ani][mDestination.getFamily() - AtomicShell.KFamily]
                     : 0.0;
         }
      }
      return 0.0;
   }

   /**
    * The last element for which there is KLM weight data.
    * 
    * @return Element
    */
   public static Element lastWeights() {
      if (mWeight == null)
         readLineWeights();
      return Element.byAtomicNumber(mWeight.length);
   }

   /**
    * getWeight - Gets the transition weight associated with the specified
    * elemet and transition. The mode determines how the weights are normalized.
    * Depending upon the use, various different wieghting schemes can be
    * appropriate.
    * 
    * @param mode
    *           int One of DefaultNormalization, NormalizeFamily, or
    *           NormalizeDestination
    * @return double
    * @throws EPQFatalException
    *            - When weight data is unavailable
    */
   static public double getWeight(Element el, int trans, int mode) {
      // The static table mWeight is loaded on demand from LineWeights.csv
      if (mWeight == null)
         readLineWeights();
      if (isWellKnown(trans)) {
         final int ani = el.getAtomicNumber() - 1;
         switch (mode) {
            case NormalizeDefault :
               return (mWeight[ani] != null) && (trans < mWeight[ani].length) ? mWeight[ani][trans] : 0.0;
            case NormalizeFamily :
               return (mWeight[ani] != null) && (trans < mWeight[ani].length)
                     ? mWeight[ani][trans] / mFamNormalization[ani][XRayTransition.getFamily(trans) - AtomicShell.KFamily]
                     : 0.0;
            case NormalizeDestination :
               return (mWeight[ani] != null) && (trans < mWeight[ani].length)
                     ? mWeight[ani][trans] / mDestNormalization[ani][XRayTransition.getDestinationShell(trans) - AtomicShell.K]
                     : 0.0;
            case NormalizeKLM :
               return (mWeight[ani] != null) && (trans < mWeight[ani].length)
                     ? mWeight[ani][trans] / mKLMNormalization[ani][XRayTransition.getFamily(trans) - AtomicShell.KFamily]
                     : 0.0;
         }
      }
      return 0.0;
   }

   /**
    * getNormalizedWeight - Gets the transition weight associated with this
    * transition. The weight of the dominant line in each family is normalized
    * to a sum of 1.0.
    * 
    * @throws EPQFatalException
    *            - When weight data is unavailable
    * @return double
    */
   public double getNormalizedWeight() {
      if (!isWellKnown(mTransition))
         throw new EPQFatalException("Line weight data is not available for " + toString() + ".");
      // The static table mWeight is loaded on demand from LineWeights.csv
      if (mWeight == null)
         readLineWeights();
      final int ani = mSource.getElement().getAtomicNumber() - 1;
      return (mWeight[ani] != null) && (mTransition < mWeight[ani].length)
            ? mWeight[ani][mTransition] / mDestNormalization[ani][mDestination.getShell() - AtomicShell.K]
            : 0.0;
   }

   /**
    * getFamily - Returns the index of the family associated with this
    * transition.
    * 
    * @return int - One of AtomicShell.KFamily, LFamily,...
    */
   public int getFamily() {
      return AtomicShell.getFamily(getDestinationShell());
   }

   /**
    * getFamily - A static method for returning the family with which a
    * transition is associated.
    * 
    * @param xrt
    *           int - A valid named transition index
    * @return int - One of KFamily, LFamily,...
    */
   static public int getFamily(int xrt) {
      return AtomicShell.getFamily(getDestinationShell(xrt));
   }

   /**
    * occurs - Does this x-ray line have a weight greater than zero?
    * 
    * @param elm
    *           int
    * @param trans
    *           int
    * @return boolean
    */
   public static boolean occurs(int elm, int trans) {
      if (mWeight == null)
         readLineWeights();
      if ((elm < Element.elmH) || ((elm - 1) >= mWeight.length))
         return false;
      if (!isWellKnown(trans))
         return false; // Unnamed transitions are assumed not to occur.
      return (mWeight[elm - 1] != null) && (trans < mWeight[elm - 1].length) && (mWeight[elm - 1][trans] != 0.0);
   }

   /**
    * getStrongestLine - For the element and edge specified by the AtomicShell
    * object, returns the XRayTransition with the highest weight.
    * 
    * @param shell
    *           AtomicShell
    * @throws EPQException
    * @return XRayTransition
    */
   public static XRayTransition getStrongestLine(AtomicShell shell) throws EPQException {
      final int zp = shell.getElement().getAtomicNumber() - Element.elmH;
      if (mWeight == null)
         readLineWeights();
      if (zp >= mWeight.length)
         throw new EPQException("Line weight data is not available for " + shell.getElement().toAbbrev() + ".");
      int bestLine = XRayTransition.None;
      double bestWeight = 0.0;
      for (int line = KA1; line < Last; ++line)
         if (mDestinationShell[line - KA1] == shell.getShell()) {
            final double wgt = (mWeight[zp] != null) && ((line - KA1) < mWeight[zp].length) ? mWeight[zp][line - KA1] : 0.0;
            if (wgt > bestWeight) {
               bestLine = line;
               bestWeight = wgt;
            }
         }
      return bestLine != XRayTransition.None ? new XRayTransition(shell.getElement(), bestLine) : null;
   }

   /**
    * equals - returns true if the value of obj is exactly equal to the value of
    * this.
    * 
    * @param obj
    *           Object
    * @return boolean
    */
   @Override
   public boolean equals(Object obj) {
      if (obj == this)
         return true;
      if (obj instanceof XRayTransition) {
         final XRayTransition xrt = (XRayTransition) obj;
         return mSource.equals(xrt.mSource) && mDestination.equals(xrt.mDestination);
      }
      return false;
   }

   @Override
   public int hashCode() {
      return mSource.hashCode() ^ mDestination.hashCode();
   }

   @Override
   public Object clone() {
      return new XRayTransition(mDestination, mSource.getShell());
   }

   /**
    * compareTo - Orders by atomic number then by destination shell and finally
    * by source shell.
    * 
    * @param xrt
    * @return int
    */
   @Override
   public int compareTo(XRayTransition xrt) {
      final int thisAn = getElement().getAtomicNumber();
      final int otherAn = xrt.getElement().getAtomicNumber();
      if (thisAn < otherAn)
         return -1;
      else if (thisAn == otherAn) {
         int thisShell = getDestinationShell();
         int otherShell = xrt.getDestinationShell();
         if (thisShell < otherShell)
            return -1;
         else if (thisShell == otherShell) {
            thisShell = getSourceShell();
            otherShell = xrt.getSourceShell();
            if (thisShell < otherShell)
               return -1;
            else if (thisShell == otherShell)
               return 0;
            else
               return 1;
         } else
            return 1;
      } else
         return 1;
   }

   /**
    * createByDestinationShell - Creates a list of all x-ray transition lines
    * that may occur when the destination shell is empty.
    * 
    * @param dest
    *           AtomicShell
    * @return A Collection containing XRayTransition objects
    */
   public static Collection<XRayTransition> createByDestinationShell(AtomicShell dest) {
      final ArrayList<XRayTransition> al = new ArrayList<XRayTransition>();
      for (int xrt = KA1; xrt < Last; ++xrt)
         if (XRayTransition.getDestinationShell(xrt) == dest.getShell()) {
            final XRayTransition tr = new XRayTransition(dest.getElement(), xrt);
            if ((tr.getSource().getGroundStateOccupancy() > 0) && (XRayTransition.occurs(dest.getElement().getAtomicNumber(), xrt)))
               al.add(tr);
         }
      return al;
   }

   /**
    * getFirstTransition - Get the first transition (by enumerated index) in the
    * specified line family (AtomicShell.KFamily...AtomicShell.NFamily)
    * 
    * @param family
    *           int
    * @return int
    */
   public static int getFirstTransition(int family) {
      switch (family) {
         case AtomicShell.KFamily :
            return XRayTransition.KA1;
         case AtomicShell.LFamily :
            return XRayTransition.L3N2;
         case AtomicShell.MFamily :
            return XRayTransition.M1N2;
         case AtomicShell.NFamily :
            return XRayTransition.N4N6;
      }
      assert (false);
      return XRayTransition.None;
   }

   /**
    * getFirstTransition - Get the first transition (by enumerated index) in the
    * specified line family (AtomicShell.KFamily...AtomicShell.NFamily)
    * 
    * @param elm
    *           Element
    * @param family
    *           int
    * @return XRayTransition
    */
   public static XRayTransition getBrightestTransition(Element elm, int family) {
      XRayTransition brightest = null;
      for (int i = getFirstTransition(family); i < getLastTransition(family); ++i)
         if (XRayTransition.exists(elm, i))
            if (brightest == null)
               brightest = new XRayTransition(elm, i);
            else if (getWeight(elm, i, NormalizeKLM) > brightest.getWeight(NormalizeKLM))
               brightest = new XRayTransition(elm, i);
      return brightest;
   }

   /**
    * getLastTransition - Get the last transition (by enumerated index) in the
    * specified line family (AtomicShell.KFamily...AtomicShell.NFamily)
    * 
    * @param family
    *           int
    * @return int Last enumerated transition plus one. (last+1!!!)
    */
   public static int getLastTransition(int family) {
      switch (family) {
         case AtomicShell.KFamily :
            return XRayTransition.KB5 + 1;
         case AtomicShell.LFamily :
            return XRayTransition.Lv + 1;
         case AtomicShell.MFamily :
            return XRayTransition.MZ2 + 1;
         case AtomicShell.NFamily :
            return XRayTransition.N5N6 + 1;
      }
      assert (false);
      return XRayTransition.None;
   }

   /**
    * getFirstIntoShell - Get the first transition (ordered by the order of the
    * enumerated shell index) that ends in the specified shell.
    * 
    * @param shell
    *           AtomicShell
    * @return int
    */
   public static int getFirstIntoShell(AtomicShell shell) {
      int res = XRayTransition.None;
      switch (shell.getShell()) {
         case AtomicShell.K :
            res = XRayTransition.KA1;
            break;
         case AtomicShell.LI :
            res = XRayTransition.L1M1;
            break;
         case AtomicShell.LII :
            res = XRayTransition.L2M2;
            break;
         case AtomicShell.LIII :
            res = XRayTransition.L3N2;
            break;
         case AtomicShell.MI :
            res = XRayTransition.M1N2;
            break;
         case AtomicShell.MII :
            res = XRayTransition.M2M4;
            break;
         case AtomicShell.MIII :
            res = XRayTransition.M3M4;
            break;
         case AtomicShell.MIV :
            res = XRayTransition.M4N3;
            break;
         case AtomicShell.MV :
            res = XRayTransition.M5O3;
            break;
         case AtomicShell.NIV :
            res = XRayTransition.N4N6;
            break;
         case AtomicShell.NV :
            res = XRayTransition.N5N6;
            break;
      }
      assert ((res == None) || (XRayTransition.getDestinationShell(res) == shell.getShell()));
      return res;
   }

   /**
    * getLastIntoShell - Get the last (plus one) transition (ordered by the
    * order of the enumerated shell index) that ends in the specified shell.
    * 
    * @param shell
    *           AtomicShell
    * @return int - Last index plus 1
    */
   public static int getLastIntoShell(AtomicShell shell) {
      int res = XRayTransition.None;
      switch (shell.getShell()) {
         case AtomicShell.K :
            res = XRayTransition.L3N2;
            break;
         case AtomicShell.LI :
            res = XRayTransition.M1N2;
            break;
         case AtomicShell.LII :
            res = XRayTransition.L1M1;
            break;
         case AtomicShell.LIII :
            res = XRayTransition.L2M2;
            break;
         case AtomicShell.MI :
            res = XRayTransition.M2M4;
            break;
         case AtomicShell.MII :
            res = XRayTransition.M3M4;
            break;
         case AtomicShell.MIII :
            res = XRayTransition.M4N3;
            break;
         case AtomicShell.MIV :
            res = XRayTransition.M5O3;
            break;
         case AtomicShell.MV :
            res = XRayTransition.N4N6;
            break;
         case AtomicShell.NIV :
            res = XRayTransition.N5N6;
            break;
         case AtomicShell.NV :
            res = XRayTransition.N5N6 + 1;
            break;
      }
      assert ((res == None) || (XRayTransition.getDestinationShell(res - 1) == shell.getShell()));
      return res;
   }

   /**
    * lineWithHighestEnergy - Returns the line index of the line in the
    * specified family with the highest x-ray energy.
    * 
    * @param el
    *           Element
    * @param family
    *           int
    * @return int
    */
   static public int lineWithHighestEnergy(Element el, int family) {
      double maxE = -Double.MAX_VALUE;
      int maxLine = XRayTransition.None;
      final int last = getLastTransition(family);
      for (int i = getFirstTransition(family); i < last; ++i) {
         final XRayTransition xrt = new XRayTransition(el, i);
         try {
            if (xrt.exists() && (xrt.getEnergy() > maxE)) {
               maxE = xrt.getEnergy();
               maxLine = i;
            }
         } catch (final EPQException ex) {
         }
      }
      return maxLine;
   }

   /**
    * lineWithLowestEnergy - Returns the line index of the line in the specified
    * family with the lowest x-ray energy.
    * 
    * @param el
    *           Element
    * @param family
    *           int
    * @return int
    */
   static public int lineWithLowestEnergy(Element el, int family) {
      double minE = Double.MAX_VALUE;
      int minLine = XRayTransition.None;
      final int last = getLastTransition(family);
      for (int i = getFirstTransition(family); i < last; ++i) {
         final XRayTransition xrt = new XRayTransition(el, i);
         try {
            if (xrt.exists() && (xrt.getEnergy() < minE)) {
               minE = xrt.getEnergy();
               minLine = i;
            }
         } catch (final EPQException ex) {
         }
      }
      return minLine;
   }

   /**
    * exists - The specified x-ray transition exists (with non-zero weight) for
    * the specified element? (ie. You might see x-rays from this transition in a
    * sample of this material.)
    * 
    * @param el
    *           Element
    * @param xrt
    *           int
    * @return boolean
    */
   static public boolean exists(Element el, int xrt) {
      if (!isWellKnown(xrt))
         return false;
      // The static table mWeight is loaded on demand from LineWeights.csv
      if (mWeight == null)
         readLineWeights();
      if (!(AtomicShell.exists(el, XRayTransition.getSourceShell(el, xrt)) && AtomicShell.exists(el, mDestinationShell[xrt])))
         return false;
      final int ani = el.getAtomicNumber() - 1;
      return (ani >= 0) && (ani < mWeight.length) && (mWeight[ani] != null) && (xrt < mWeight[ani].length) && (mWeight[ani][xrt] > 0.0);
   }

   /**
    * exists - Does this x-ray transition exists (with non-zero weight)? (ie.
    * You might see x-rays from this transition in a sample of this material.)
    * 
    * @return boolean
    */
   public boolean exists() {
      if (!isWellKnown(mTransition))
         return false;
      // The static table mWeight is loaded on demand from LineWeights.csv
      if (mWeight == null)
         readLineWeights();
      Element el = mSource.getElement();
      if (!(AtomicShell.exists(el, mSource.getShell()) && AtomicShell.exists(el, mDestination.getShell())))
         return false;
      final int ani = el.getAtomicNumber() - 1;
      return (mWeight[ani] != null) && (mTransition < mWeight[ani].length) && (mWeight[ani][mTransition] > 0.0);
   }

   /**
    * shouldExist - Should this transition exist (as an x-ray transition) based
    * on whether the electric dipole and quadrupole selection rules permit the
    * transition and whether in a ground state atom both the source and
    * destination shells are occupied.
    * 
    * @return boolean
    */
   public boolean shouldExist() {
      return shouldExist(getSource(), getDestination());
   }

   /**
    * shouldExist - Should this transition exist (as an x-ray transition) based
    * on whether the electric dipole and quadrupole selection rules permit the
    * transition and whether in a ground state atom both the source and
    * destination shells are occupied.
    * 
    * @param src
    *           AtomicShell
    * @param dest
    *           AtomicShell (note:
    *           src.getAtomicNumber()==dest.getAtomicNumber())
    * @return boolean
    */
   static public boolean shouldExist(AtomicShell src, AtomicShell dest) {
      assert (src.getElement().equals(dest.getElement()));
      if (src.getPrincipalQuantumNumber() == dest.getPrincipalQuantumNumber())
         return false;
      if ((src.getGroundStateOccupancy() == 0) || (dest.getGroundStateOccupancy() == 0))
         return false;
      return AtomicShell.electricDipolePermitted(src.getShell(), dest.getShell())
            || AtomicShell.electricQuadrupolePermitted(src.getShell(), dest.getShell());
   }

   /**
    * shouldExist - Should this transition exist (as an x-ray transition) based
    * on whether the electric dipole and quadrupole selection rules permit the
    * transition and whether in a ground state atom both the source and
    * destination shells are occupied.
    * 
    * @param el
    *           Element
    * @param srcShell
    *           int
    * @param destShell
    *           int
    * @return boolean
    */
   static public boolean shouldExist(Element el, int srcShell, int destShell) {
      return shouldExist(new AtomicShell(el, srcShell), new AtomicShell(el, destShell));
   }

   /**
    * getWeightIntoDestinationShell - Returns the fraction of the total weight
    * (normalized by family) which results from transitions into the specified
    * shell.
    * 
    * @param el
    *           Element - The element
    * @param shell
    *           int - The destination (lower energy) shell
    * @return double - [0.0,1.0]
    */
   static public double getWeightIntoDestinationShell(Element el, int shell) {
      double res = 0.0;
      for (int xrt = XRayTransition.KA1; xrt < XRayTransition.Last; ++xrt)
         if (XRayTransition.getDestinationShell(xrt) == shell)
            res += getWeight(el, xrt, XRayTransition.NormalizeFamily);
      return res;
   }

   /**
    * TransitionFromShells - Returns an integer representing the named
    * transition when an electron goes from src to dest.
    * 
    * @param src
    *           int
    * @param dest
    *           int
    * @return int
    */
   static public int TransitionFromShells(int src, int dest) {
      int res = Unnamed;
      for (int i = KA1; i <= N5N6; ++i)
         if ((src == mSourceShell[i]) && (dest == mDestinationShell[i])) {
            res = i;
            break;
         }
      return res;
   }

   /**
    * transitionsIntoShell - Returns a list of the integer indexes of
    * transitions that end in the specified shell.
    * 
    * @param shell
    *           One of AtomicShell.K to AtomicShell.NV
    * @return int[]
    */
   static public int[] transitionsIntoShell(int shell) {
      int count = 0;
      for (int i = KA1; i <= N5N6; ++i)
         if (shell == mDestinationShell[i])
            ++count;
      final int[] res = new int[count];
      count = 0;
      for (int i = KA1; i <= N5N6; ++i)
         if (shell == mDestinationShell[i]) {
            res[count] = i;
            count++;
         }
      return res;
   }

   /**
    * Does one or more transitions exist for the specified element and
    * transition family.
    * 
    * @param el
    *           An element
    * @param family
    *           - One of AtomicShell.KFamily, AtomicShell.LFamily,
    *           AtomicShell.MFamily or AtomicShell.NFamily.
    * @return true if the family exists for this element, false otherwise
    */
   static public boolean familyExists(Element el, int family) {
      if (mWeight == null)
         readLineWeights();
      final int ani = el.getAtomicNumber() - 1;
      if (mWeight[ani] == null)
         return false;
      final double[] w = mWeight[ani];
      int last = getLastTransition(family);
      if (last > w.length)
         last = w.length;
      for (int tr = getFirstTransition(family); tr < last; ++tr)
         if (w[tr] > 0.0)
            return true;
      return false;
   }
}
