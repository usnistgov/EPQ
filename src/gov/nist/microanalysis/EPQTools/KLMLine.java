package gov.nist.microanalysis.EPQTools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.TransitionEnergy;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQLibrary.XRayTransitionSet;

/**
 * <p>
 * A small class for representing KLM lines.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Daniel "Ooblioob" Davis, Nicholas W. M. Ritchie
 * @version 1.0
 */

abstract public class KLMLine
   implements
   Comparable<KLMLine> {

   enum LabelType {
      ELEMENT("Element"), //
      ELEMENT_ABBREV("Element abbreviation"), //
      LARGE_ELEMENT("Large abbreviation"), //
      SIEGBAHN("Siegbahn"), //
      IUPAC("IUPAC"), //
      FAMILY("Family"), //
      NONE("No labels");

      private final String mName;

      private LabelType(String id) {
         mName = id;
      }

      @Override
      public String toString() {
         return mName;
      }
   };

   enum KLMLineType {
      InvalidType, Satellite, SumPeak, EscapePeak, KEdge, LEdge, MEdge, NEdge, KTransition, LTransition, MTransition, NTransition;

      public boolean isTransition() {
         return (this == KTransition) || (this == LTransition) || (this == MTransition) || (this == NTransition);
      }
   }

   /**
    * Returns a shell with which to associate this KLMLine
    */
   abstract public AtomicShell getShell();

   abstract public String toLabel(LabelType lt);

   protected double mEnergy;
   protected double mAmplitude;

   public static class Transition
      extends
      KLMLine {
      private final XRayTransition mTransition;

      public Transition(XRayTransition xrt)
            throws EPQException {
         super(TransitionEnergy.Default.compute(xrt), xrt.getWeight(XRayTransition.NormalizeKLM));
         mTransition = xrt;
      }

      @Override
      public String toLabel(LabelType lt) {
         switch(lt) {
            case ELEMENT:
               return mTransition.getElement().toString();
            case LARGE_ELEMENT:
            case ELEMENT_ABBREV:
               return mTransition.getElement().toAbbrev();
            case SIEGBAHN:
               return mTransition.getSiegbahnName();
            case IUPAC:
               return mTransition.getIUPACName();
            case FAMILY:
               return mTransition.getElement().toAbbrev() + " "
                     + XRayTransitionSet.getClassNameForTransition(mTransition.getTransitionIndex());
            case NONE:
            default:
               return "";
         }
      }

      @Override
      public String toString() {
         return mTransition.toString();
      }

      @Override
      public AtomicShell getShell() {
         return mTransition.getDestination();
      }

      public XRayTransition getTransition() {
         return mTransition;
      }

      @Override
      public boolean contains(Element elm) {
         return mTransition.getElement().equals(elm);
      }

      @Override
      public String toSiegbahn() {
         return mTransition.getSiegbahnName();
      }

      @Override
      public KLMLineType getType() {
         KLMLine.KLMLineType res = KLMLine.KLMLineType.InvalidType;
         switch(getShell().getFamily()) {
            case AtomicShell.KFamily:
               res = KLMLine.KLMLineType.KTransition;
               break;
            case AtomicShell.LFamily:
               res = KLMLine.KLMLineType.LTransition;
               break;
            case AtomicShell.MFamily:
               res = KLMLine.KLMLineType.MTransition;
               break;
            case AtomicShell.NFamily:
               res = KLMLine.KLMLineType.NTransition;
               break;
            default:
               res = KLMLine.KLMLineType.InvalidType;
               break;
         }
         return res;
      }

   }

   public static Set<Transition> suggestKLM(Element elm, double eMax) {
      final int[] DEFAULT_KLM_LINES = {
         XRayTransition.KA1,
         XRayTransition.KB1,
         XRayTransition.LA1,
         XRayTransition.LB1,
         XRayTransition.MA1,
         XRayTransition.MA2,
         XRayTransition.MB,
         XRayTransition.MG
      };
      final Set<Transition> lines = new TreeSet<Transition>();
      for(final int tr : DEFAULT_KLM_LINES)
         try {
            if(XRayTransition.exists(elm, tr) && (XRayTransition.getEnergy(elm, tr) < eMax))
               lines.add(new KLMLine.Transition(new XRayTransition(elm, tr)));
         }
         catch(EPQException e) {
            // Ignore
         }
      return lines;
   }

   public static class Edge
      extends
      KLMLine {

      private final AtomicShell mShell;

      static public AtomicShell mostOccupiedShellInFamily(AtomicShell sh) {
         AtomicShell res = sh;
         try {
            final int fam = sh.getFamily();
            for(int shell = AtomicShell.getFirstInFamily(fam); shell <= AtomicShell.getLastInFamily(fam); shell++) {
               final AtomicShell tmp = new AtomicShell(sh.getElement(), shell);
               if(tmp.getGroundStateOccupancy() > res.getGroundStateOccupancy())
                  res = tmp;
            }
         }
         catch(final Exception e) {
            e.printStackTrace();
         }
         return res;
      }

      static public double fractionalOccupancy(AtomicShell shell) {
         final double den = 1.0 / mostOccupiedShellInFamily(shell).getGroundStateOccupancy();
         return Double.isNaN(den) ? 1.0 : den * shell.getGroundStateOccupancy();
      }

      public Edge(AtomicShell shell) {
         super(shell.getEdgeEnergy(), 0.2 * fractionalOccupancy(shell));
         mShell = shell;
      }

      @Override
      public String toLabel(LabelType lt) {
         switch(lt) {
            case ELEMENT:
               return mShell.getElement().toString() + " edge";
            case LARGE_ELEMENT:
            case ELEMENT_ABBREV:
               return mShell.getElement().toAbbrev() + " edge";
            case SIEGBAHN:
               return mShell.getSiegbahnName() + " edge";
            case IUPAC:
               return mShell.getIUPACName() + " edge";
            case FAMILY:
               return mShell.getIUPACName() + " edge";
            case NONE:
            default:
               return "";
         }
      }

      @Override
      public AtomicShell getShell() {
         return mShell;
      }

      @Override
      public String toString() {
         return mShell.toString();
      }

      @Override
      public boolean contains(Element elm) {
         return mShell.getElement().equals(elm);
      }

      @Override
      public String toSiegbahn() {
         return mShell.toString();
      }

      @Override
      public KLMLineType getType() {
         KLMLine.KLMLineType res = KLMLine.KLMLineType.InvalidType;
         switch(getShell().getFamily()) {
            case AtomicShell.KFamily:
               res = KLMLine.KLMLineType.KEdge;
               break;
            case AtomicShell.LFamily:
               res = KLMLine.KLMLineType.LEdge;
               break;
            case AtomicShell.MFamily:
               res = KLMLine.KLMLineType.MEdge;
               break;
            case AtomicShell.NFamily:
               res = KLMLine.KLMLineType.NEdge;
               break;
            default:
               res = KLMLine.KLMLineType.InvalidType;
               break;
         }
         return res;
      }

   }

   public static class EscapePeak
      extends
      KLMLine {
      private final XRayTransition mTransition;

      static private final XRayTransition SI_K = new XRayTransition(Element.Si, XRayTransition.KA1);

      public EscapePeak(XRayTransition xrt)
            throws EPQException {
         super(xrt.getEnergy() - SI_K.getEnergy(), 0.1);
         mTransition = xrt;
      }

      @Override
      public String toLabel(LabelType lt) {
         switch(lt) {
            case ELEMENT:
               return mTransition.getElement().toString() + " esc";
            case ELEMENT_ABBREV:
            case LARGE_ELEMENT:
               return mTransition.getElement().toAbbrev() + " esc";
            case SIEGBAHN:
               return mTransition.getSiegbahnName() + " esc";
            case IUPAC:
            case FAMILY:
               return mTransition.getElement().toAbbrev() + " esc";
            case NONE:
            default:
               return "";
         }
      }

      @Override
      public String toString() {
         return mTransition.toString() + " escape";
      }

      @Override
      public boolean contains(Element elm) {
         return mTransition.getElement().equals(elm);
      }

      @Override
      public AtomicShell getShell() {
         return mTransition.getDestination();
      }

      @Override
      public boolean isAssociated(KLMLine line) {
         if(line instanceof EscapePeak) {
            final EscapePeak tr = (EscapePeak) line;
            return mTransition.equals(tr.mTransition);
         }
         return false;
      }

      @Override
      public int hashCode() {
         return mTransition.hashCode() + 0xFEEE;
      }

      @Override
      public boolean equals(Object obj) {
         if(obj instanceof EscapePeak) {
            final EscapePeak ep = (EscapePeak) obj;
            return ep.mTransition.equals(mTransition);
         }
         return false;
      }

      /**
       * Suggest a list of possible escape peaks for the specified element from
       * the specified list of possible lines with energy less then eMax
       * 
       * @param elm
       * @return Set&lt;EscapePeak&gt;
       */
      static public Set<EscapePeak> suggestEscapePeak(Element elm) {
         int[] lines = {
            XRayTransition.KA1,
            XRayTransition.LA1,
            XRayTransition.MA1
         };
         final HashSet<EscapePeak> res = new HashSet<EscapePeak>();
         for(final int line : lines)
            try {
               if(XRayTransition.exists(elm, line)) {
                  final XRayTransition xrt = new XRayTransition(elm, line);
                  final double e = xrt.getEnergy();
                  if(e > ToSI.keV(0.02) + SI_K.getEnergy())
                     res.add(new EscapePeak(xrt));
               }
            }
            catch(final Exception e) {
               // Just ignore it
            }
         return res;
      }

      @Override
      public String toSiegbahn() {
         return mTransition.getSiegbahnName() + "-Si K";
      }

      @Override
      public KLMLineType getType() {
         return KLMLine.KLMLineType.EscapePeak;
      }
   }

   public static class SumPeak
      extends
      KLMLine {
      private final XRayTransition[] mTransitions;
      private final String mName;

      public SumPeak(XRayTransition xrt1, XRayTransition xrt2)
            throws EPQException {
         super(xrt1.getEnergy() + xrt2.getEnergy(), 0.1);
         mTransitions = new XRayTransition[] {
            xrt1,
            xrt2
         };
         if(xrt1.equals(xrt2))
            mName = "2\u00B7" + xrt1.toString();
         else
            mName = xrt1.toString() + "+" + xrt2.toString();
      }

      public SumPeak(XRayTransition xrt, int n)
            throws EPQException {
         super(xrt.getEnergy() * n, 0.1);
         mTransitions = new XRayTransition[n];
         for(int i = 0; i < n; ++i)
            mTransitions[i] = xrt;
         mName = Integer.toString(n) + "\u00B7" + xrt.toString();
      }

      @Override
      public String toLabel(LabelType lt) {
         final StringBuffer sb = new StringBuffer();
         final TreeMap<XRayTransition, Integer> count = new TreeMap<XRayTransition, Integer>();
         for(final XRayTransition mTransition : mTransitions)
            if(count.containsKey(mTransition))
               count.put(mTransition, Integer.valueOf(count.get(mTransition) + 1));
            else
               count.put(mTransition, Integer.valueOf(1));
         for(final Map.Entry<XRayTransition, Integer> me : count.entrySet()) {
            if(sb.length() > 0)
               sb.append("+");
            if(me.getValue().intValue() > 1) {
               sb.append(me.getValue().toString());
               sb.append("\u00B7");
            }
            final XRayTransition xrt = me.getKey();
            switch(lt) {
               case ELEMENT:
                  sb.append(xrt.getElement().toString());
                  break;
               case LARGE_ELEMENT:
               case ELEMENT_ABBREV:
                  sb.append(xrt.getElement().toAbbrev());
                  break;
               case SIEGBAHN:
                  sb.append(xrt.getSiegbahnName());
               case IUPAC:
                  sb.append(xrt.toString());
                  break;
               case FAMILY:
                  sb.append(xrt.getElement().toAbbrev() + " "
                        + XRayTransitionSet.getClassNameForTransition(xrt.getTransitionIndex()));
                  break;
               case NONE:
               default:
                  continue;
            }
         }
         return sb.toString();
      }

      @Override
      public String toString() {
         return mName;
      }

      @Override
      public AtomicShell getShell() {
         return mTransitions[0].getDestination();
      }

      @Override
      public int hashCode() {
         int hash = 0x0;
         for(final XRayTransition xrt : mTransitions)
            hash ^= xrt.hashCode();
         return hash;
      }

      @Override
      public boolean contains(Element elm) {
         for(final XRayTransition xrt : mTransitions)
            if(xrt.getElement().equals(elm))
               return true;
         return false;
      }

      @Override
      public boolean isAssociated(KLMLine line) {
         if(line instanceof Transition) {
            final Transition tr = (Transition) line;
            for(final XRayTransition xrt : mTransitions)
               if(xrt.equals(tr.mTransition))
                  return true;
         }
         return false;
      }

      static public Set<SumPeak> suggestSumPeaks(Set<Element> elms, double lowE, double highE, int order, int[] lines) {
         final ArrayList<XRayTransition> xrts = new ArrayList<XRayTransition>();
         for(final Element elm : elms)
            for(final int line : lines)
               try {
                  if(XRayTransition.exists(elm, line) && (XRayTransition.getEnergy(elm, line) < highE))
                     xrts.add(new XRayTransition(elm, line));
               }
               catch(final EPQException e3) {
                  // May happen sometimes but it is harmless.
               }
         final HashSet<SumPeak> res = new HashSet<SumPeak>();
         for(int i = 0; i < xrts.size(); ++i)
            for(int j = i; j < xrts.size(); ++j) {
               final XRayTransition xrt1 = xrts.get(i);
               final XRayTransition xrt2 = xrts.get(j);
               try {
                  final double e1 = xrt1.getEnergy();
                  final double e2 = xrt2.getEnergy();
                  if(((e1 + e2) >= lowE) && ((e1 + e2) <= highE))
                     res.add(new SumPeak(xrt1, xrt2));
               }
               catch(final EPQException e) {
                  System.err.println("This should never happen!");
               }
            }
         return res;
      }

      @Override
      public String toSiegbahn() {
         if((mTransitions.length == 2) && (mTransitions[0].equals(mTransitions[1])))
            return Integer.toString(2) + "\u00B7" + mTransitions[1].getSiegbahnName();
         else {
            final StringBuffer sb = new StringBuffer();
            sb.append(mTransitions[0].getSiegbahnName());
            for(int i = 1; i < mTransitions.length; ++i) {
               sb.append("+");
               sb.append(mTransitions[1].getSiegbahnName());
            }
            return sb.toString();
         }
      }

      @Override
      public KLMLineType getType() {
         return KLMLine.KLMLineType.SumPeak;
      }

   }

   /**
    * Constructs a KLMLine representing a transition
    * 
    * @param energy
    * @param amplitude
    */
   protected KLMLine(double energy, double amplitude) {
      mEnergy = energy;
      mAmplitude = amplitude;
   }

   /**
    * getEnergy - Returns the energy of a particular KLM line in Joules.
    * 
    * @return double
    */
   public double getEnergy() {
      return mEnergy;
   }

   /**
    * getAmplitude - returns the amplitude of a particular KLM line.
    * 
    * @return double
    */
   public double getAmplitude() {
      return mAmplitude;
   }

   /**
    * compareTo - Allows to KLM lines to be ordered.
    * 
    * @param kl KLMLine
    * @return int
    */
   @Override
   public int compareTo(KLMLine kl) {
      final int res = getShell().compareTo(kl.getShell());
      return res == 0 ? toString().compareTo(kl.toString()) : res;
   }

   @Override
   public boolean equals(Object obj) {
      if(this == obj)
         return true;
      if(obj.getClass() == this.getClass()) {
         final KLMLine l2 = (KLMLine) obj;
         return (l2.mAmplitude == this.mAmplitude) && (l2.mEnergy == this.mEnergy) && (l2.toString().equals(this.toString()));
      }
      return false;
   }

   abstract public boolean contains(Element elm);

   abstract public String toSiegbahn();

   /**
    * Is the specified KLMLine equal to this line or is the specified KLMLine a
    * sub-component of this line (ie One of the peaks in a sum peak.)
    * 
    * @param line
    * @return boolean
    */
   public boolean isAssociated(KLMLine line) {
      return equals(line);
   }

   public KLMLineType getType() {
      return KLMLine.KLMLineType.InvalidType;
   }
}
