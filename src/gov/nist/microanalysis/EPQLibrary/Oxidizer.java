package gov.nist.microanalysis.EPQLibrary;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.Utility.CSVReader;
import gov.nist.microanalysis.Utility.DescriptiveStatistics;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * The Oxidizer class is useful for calculating O by stoichiometric rules. By
 * default, the oxidation state is read from the resource
 * (OxidizationState.csv). You may change the oxidization state of any element
 * using the setOxidizationState method. For example, the default oxidization
 * state of O is -2, Al is 3. This balances with 3 O per 2 Al atoms of Al2O3. H
 * is 1 which balances at H2O. The getComposition method will create a
 * Composition object for the oxidized material associated with the specified
 * element.
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
public class Oxidizer {

   private final int[] mOxidationState;
   public static final Composition OXYGEN = buildO();

   private static Composition buildO() {
      final Composition res = new Composition(Element.O);
      res.setName("Remainder O");
      return res;
   }

   public Oxidizer() {
      final CSVReader cr = new CSVReader.ResourceReader("OxidizationState.csv", false);
      final double[][] res = cr.getResource(getClass());
      mOxidationState = new int[res.length];
      for (int i = 0; i < mOxidationState.length; ++i) {
         mOxidationState[i] = (int) Math.round(res[i][1]);
         assert i == (Element.elmO - 1) ? mOxidationState[i] == -2 : mOxidationState[i] >= 0;
      }
      assert getOxidationState(Element.O) == -2;
   }

   public int getOxidationState(Element elm) {
      return mOxidationState[elm.getAtomicNumber() - 1];
   }

   /**
    * Specify the valence of the specified element.
    * 
    * @param elm
    * @param valence
    */
   public void setOxidizationState(Element elm, int valence) {
      assert ((elm.equals(Element.O)) && (valence == -2)) || (valence >= 0);
      mOxidationState[elm.getAtomicNumber() - 1] = valence;
   }

   /**
    * Returns the oxide form of the specific element.
    * 
    * @param elm
    * @return Composition
    */
   public Composition getComposition(Element elm) {
      int o = getOxidationState(Element.O);
      int e = getOxidationState(elm);
      final Composition comp = new Composition();
      if (e > 0) {
         final int gcd = (int) Math2.gcd(e, o);
         if (gcd > 1) {
            e /= gcd;
            o /= gcd;
         }
         comp.addElementByStoiciometry(Element.O, e);
         comp.addElementByStoiciometry(elm, -o);
         comp.setName(elm.toAbbrev() + (o < -1 ? Integer.toString(-o) : "") + "O" + (e > 1 ? Integer.toString(e) : ""));
      } else {
         comp.addElementByStoiciometry(elm, 1);
         comp.setName(elm.toAbbrev());
      }
      return comp;
   }

   /**
    * Express the oxide form of the specified element as an HTML string.
    * 
    * @param elm
    * @return String
    */
   public String toHTML(Element elm) {
      int o = getOxidationState(Element.O);
      int e = getOxidationState(elm);
      final StringBuffer sb = new StringBuffer();
      if (e > 0) {
         final int gcd = (int) Math2.gcd(e, o);
         if (gcd > 1) {
            e /= gcd;
            o /= gcd;
         }
         sb.append(elm.toAbbrev());
         if (o < -1) {
            sb.append("<sub>");
            sb.append(Integer.toString(-o));
            sb.append("</sub>");
         }
         sb.append("O");
         if (e > 1) {
            sb.append("<sub>");
            sb.append(Integer.toString(e));
            sb.append("</sub>");
         }
      } else
         sb.append(elm.toAbbrev());
      return sb.toString();
   }

   public String toHTMLTable(Collection<Composition> comps, NumberFormat nf4) {
      final StringWriter sb = new StringWriter();
      final PrintWriter pw = new PrintWriter(sb);
      pw.println("<TABLE>");
      pw.println("<TR><TH>Spectrum</TH><TH>As Oxides</TH></TR>");
      for (final Composition comp : comps) {
         pw.print("<TD>" + comp.toString() + "</TD>");
         pw.print("<TD><TABLE><TH>Oxide</TH><TH>Mass Fraction</TH><TH>Normalized</TH></TR>");
         final Map<Composition, UncertainValue2> map = toOxideFraction(comp);
         final UncertainValue2 sum = UncertainValue2.add(map.values());
         for (final Map.Entry<Composition, UncertainValue2> me : map.entrySet()) {
            pw.print("<TD>" + me.getKey().toString() + "</TD>");
            pw.print("<TD>" + me.getValue().format(nf4) + "</TD>");
            pw.print("<TD>" + UncertainValue2.divide(me.getValue(), sum).format(nf4) + "</TD>");
            pw.println("</TR>");
         }
         pw.print("<TR><TD>Sum</TD><TD>");
         pw.print(nf4.format(sum.doubleValue()));
         pw.println("</TD><TD>&nbsp;</TD></TR>");
         pw.println("</TABLE></TD></TR>");
      }
      pw.println("</TABLE>");
      return sb.toString();
   }

   public String toHTMLTable2(Collection<Composition> comps, NumberFormat nf4) {
      final StringWriter sb = new StringWriter();
      final PrintWriter pw = new PrintWriter(sb);
      final Map<Composition, Map<Composition, UncertainValue2>> allComps = new TreeMap<Composition, Map<Composition, UncertainValue2>>();
      final Set<Composition> oxides = new TreeSet<Composition>();
      for (final Composition comp : comps) {
         final Map<Composition, UncertainValue2> map = toOxideFraction(comp);
         allComps.put(comp, map);
         oxides.addAll(map.keySet());
      }
      pw.print("<TABLE>");
      pw.print("<TR><TH>Oxide</TH>");
      for (final Composition comp : allComps.keySet())
         pw.print("<TH>" + comp.toString() + "</TH>");
      if (oxides.size() > 1)
         pw.print("<TH>Average</TH>");
      if (oxides.size() > 2)
         pw.print("<TH>Std Dev</TH>");
      pw.println("</TR>");
      for (final Composition oxide : oxides) {
         pw.print("<TR><TD>");
         pw.print(oxide.toString());
         pw.print("</TD><TD>");
         final DescriptiveStatistics ds = new DescriptiveStatistics();
         for (final Composition comp : allComps.keySet()) {
            final Map<Composition, UncertainValue2> map = allComps.get(comp);
            UncertainValue2 uv = map.get(oxide);
            if (uv == null)
               uv = UncertainValue2.ZERO;
            ds.add(uv.doubleValue());
            pw.print("\t" + nf4.format(uv.doubleValue()));
            pw.print("</TD>");
         }
         if (oxides.size() > 1) {
            pw.print("<TD>");
            pw.print(nf4.format(ds.average()));
            pw.print("</TD>");
         }
         if (oxides.size() > 2) {
            pw.print("<TD>");
            pw.print(nf4.format(ds.standardDeviation()));
            pw.print("</TD>");
         }
         pw.println("</TR>");
      }
      pw.println("</TABLE>");
      return sb.toString();
   }

   public String toTable(Collection<Composition> comps, NumberFormat nf4) {
      final StringWriter sb = new StringWriter();
      final PrintWriter pw = new PrintWriter(sb);
      final Map<Composition, Map<Composition, UncertainValue2>> allComps = new TreeMap<Composition, Map<Composition, UncertainValue2>>();
      final Set<Composition> oxides = new TreeSet<Composition>();
      for (final Composition comp : comps) {
         final Map<Composition, UncertainValue2> map = toOxideFraction(comp);
         allComps.put(comp, map);
         oxides.addAll(map.keySet());
      }
      pw.print("Oxide\t");
      for (final Composition comp : allComps.keySet())
         pw.print("\t" + comp.toString());
      pw.println("\tAverage\tStd Dev");
      for (final Composition oxide : oxides) {
         pw.print(oxide.toString());
         final DescriptiveStatistics ds = new DescriptiveStatistics();
         for (final Composition comp : allComps.keySet()) {
            final Map<Composition, UncertainValue2> map = allComps.get(comp);
            UncertainValue2 uv = map.get(oxide);
            if (uv == null)
               uv = UncertainValue2.ZERO;
            ds.add(uv.doubleValue());
            pw.print("\t" + nf4.format(uv.doubleValue()));
         }
         pw.print("\t" + nf4.format(ds.average()));
         pw.print("\t" + nf4.format(ds.standardDeviation()));
         pw.println();
      }
      return sb.toString();
   }

   /**
    * Computes the description of the specified Composition as though it is
    * comprised of oxides. The difference in O is reported as associated with
    * the material Oxidizer.OXYGEN. Composition.combne(...) is the inverse
    * function.
    * 
    * @param comp
    * @return Map&lt;Composition, UncertainValue2&gt;
    */
   public Map<Composition, UncertainValue2> toOxideFraction(Composition comp) {
      final Map<Composition, UncertainValue2> res = new TreeMap<Composition, UncertainValue2>();
      final Element elmO = Element.O;
      UncertainValue2 oxy = comp.weightFractionU(elmO, false);
      for (final Element elm : comp.getElementSet())
         if (elm.getAtomicNumber() != Element.elmO) {
            final Composition cc = getComposition(elm);
            final UncertainValue2 q = UncertainValue2.divide(comp.weightFractionU(elm, false), cc.weightFractionU(elm, false));
            oxy = UncertainValue2.subtract(oxy, UncertainValue2.multiply(q, cc.weightFractionU(elmO, false)));
            res.put(cc, q);
         }
      if (Math.abs(oxy.doubleValue()) > 1.0e-6)
         res.put(OXYGEN, oxy);
      return res;
   }

   /**
    * Takes the initial composition and replaces the current oxygen quantity
    * with the quantity assuming the oxide for specified in this Oxidizer.
    * 
    * @param start
    *           Initial Composition
    * @return Composition
    */
   public Composition compute(Composition start) {
      final Composition res = new Composition();
      UncertainValue2 oxy = UncertainValue2.ZERO;
      for (final Element elm : start.getElementSet())
         if (!elm.equals(Element.O)) {
            final UncertainValue2 val = start.weightFractionU(elm, false);
            res.addElement(elm, val);
            final Composition oxide = getComposition(elm);
            oxy = UncertainValue2.add(oxy,
                  UncertainValue2.multiply(oxide.weightFraction(Element.O, false) / oxide.weightFraction(elm, false), val.reduced(elm.toAbbrev())));
         }
      res.addElement(Element.O, oxy);
      return res;
   }

   /**
    * Takes the initial ParticleSignature and replaces the current oxygen
    * quantity with the quantity assuming the oxide for specified in this
    * Oxidizer.
    * 
    * @param start
    *           Initial ParticleSignature
    * @return ParticleSignature
    */
   public ParticleSignature compute(ParticleSignature start) {
      final ParticleSignature res = new ParticleSignature();
      UncertainValue2 oxy = UncertainValue2.ZERO;
      for (final Element elm : start.getUnstrippedElementSet())
         if (!elm.equals(Element.O)) {
            final UncertainValue2 val = start.getU(elm);
            res.add(elm, val);
            final Composition oxide = getComposition(elm);
            oxy = UncertainValue2.add(oxy,
                  UncertainValue2.multiply(oxide.weightFraction(Element.O, false) / oxide.weightFraction(elm, false), val.reduced(elm.toAbbrev())));
         }
      res.add(Element.O, oxy);
      return res;
   }

   public boolean equals(Oxidizer ox) {
      if (!super.equals(ox))
         return false;
      for (int i = 0; i < mOxidationState.length; ++i)
         if (mOxidationState[i] != ox.mOxidationState[i])
            return false;
      return true;

   }
}
