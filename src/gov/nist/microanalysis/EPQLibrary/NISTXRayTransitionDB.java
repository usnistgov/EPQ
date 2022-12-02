package gov.nist.microanalysis.EPQLibrary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.TreeMap;

/**
 * <p>
 * A database of x-ray transition energies from theory and measurements. The
 * source of this data is http://physics.nist.gov/PhysRefData/XrayTrans/
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
public class NISTXRayTransitionDB {
   private static String[] mReferences;
   private static TreeMap<XRayTransition, Datum> mData;

   private static NumberFormat mFormat = null; // Used by Datum

   public static String THEORY = "theory";
   public static String COMBINED = "combined";
   public static String VAPOR = "vapor";
   public static String DIRECT = "direct";
   public static String UNAVAILABLE = "unavailable";

   private static class Datum {
      private final XRayTransition mTransition;
      // private double mAtomicWeight;
      private final double mTheory;
      // private double mTheoryUnc;
      private final double mDirect;
      // private double mDirectUnc;
      private final double mCombined;
      // private double mCombinedUnc;
      private final double mVapor;

      // private double mVaporUnc;
      // private String mBlend;
      // private String mReference;

      private String stripQuotes(String str) {
         final int st = str.indexOf('\"');
         if (st != -1) {
            final int end = str.indexOf('\"', st + 1);
            assert (end != -1);
            return str.substring(st + 1, end);
         }
         return str;
      }

      private double parseDouble(String str, double def) {
         assert (mFormat != null);
         double res;
         try {
            res = mFormat.parse(str).doubleValue();
         } catch (final ParseException ex) {
            res = def;
         }
         return res;
      }

      private XRayTransition parseTransition(int atomicNumber, String str) {
         final String[] names = {"K", "L1", "L2", "L3", "M1", "M2", "M3", "M4", "M5", "N1", "N2", "N3", "N4", "N5", "N6", "N7", " edge"};
         int lower = -1, upper = -1;
         for (int i = 0; i < names.length; ++i)
            if (str.startsWith(names[i])) {
               lower = i + AtomicShell.K;
               break;
            }
         assert (lower != -1);
         assert (lower != (names.length - 1));
         final String second = str.substring(names[lower].length());
         for (int i = 0; i < names.length; ++i)
            if (second.startsWith(names[i])) {
               upper = i + AtomicShell.K;
               break;
            }
         assert (upper != -1);
         if (upper == (names.length - 1))
            upper = AtomicShell.Continuum;
         return new XRayTransition(Element.byAtomicNumber(atomicNumber), upper, lower);
      }

      private String[] parseCSV(String str, int n) {
         final String[] res = new String[n];
         final int l = str.length();
         int st = 0, end = 0, i = 0;
         while ((end < l) && (i < n)) {
            char c = str.charAt(end);
            if (c == '\"') { // skip quoted blocks
               ++end;
               while (((c = str.charAt(end)) != '\"') && (end < l))
                  ++end;
            }
            if (c == ',') {
               res[i] = stripQuotes(str.substring(st, end));
               st = end + 1;
               ++i;
            }
            ++end;
         }
         res[i] = stripQuotes(str.substring(st, end));
         return res;
      }

      // def looks like -> "Fm",254,"L3M5",16381.8,1.3,16384,13,,,,,,"74d"
      // Ele./A/Trans./Theory(eV)/Unc.(eV)/Direct(eV)/Unc.(eV)/Combined(eV)/Unc.(eV)/Vapor
      // (eV)/Unc.(eV)/Blend/Ref.
      private Datum(String def) {
         final String[] items = parseCSV(def, 13);
         final int atomicNumber = Element.atomicNumberForName(items[0]);
         parseDouble(items[1], Double.NaN); // Atomic weight
         mTransition = parseTransition(atomicNumber, stripQuotes(items[2]));
         mTheory = ToSI.eV(parseDouble(items[3], Double.NaN));
         ToSI.eV(parseDouble(items[4], Double.NaN)); // TheoryUnc
         mDirect = ToSI.eV(parseDouble(items[5], Double.NaN));
         ToSI.eV(parseDouble(items[6], Double.NaN)); // DirectUnc
         mCombined = ToSI.eV(parseDouble(items[7], Double.NaN));
         ToSI.eV(parseDouble(items[8], Double.NaN)); // mCombinedUnc
         mVapor = ToSI.eV(parseDouble(items[9], Double.NaN));
         ToSI.eV(parseDouble(items[10], Double.NaN)); // mVaporUnc
         stripQuotes(items[11]); // mBlend
         stripQuotes(items[12]); // mReference
      }
   };

   static private void readData() {
      if (mData == null) {
         mData = new TreeMap<XRayTransition, Datum>();
         mReferences = new String[116];
         mFormat = NumberFormat.getInstance(Locale.US);
         try {
            final BufferedReader br = new BufferedReader(
                  new InputStreamReader(NISTXRayTransitionDB.class.getResourceAsStream("NISTXRayDatabase.dat"), "US-ASCII"));
            String str = br.readLine();
            assert (str != null) && (str.startsWith("##References"));
            for (int i = 0; i < mReferences.length; ++i)
               mReferences[i] = br.readLine();
            str = br.readLine();
            assert (str != null) && str.startsWith("##Data");
            str = br.readLine();
            while ((str != null) && (str.length() > 0)) {
               final Datum d = new Datum(str.trim());
               mData.put(d.mTransition, d);
               str = br.readLine();
            }
         } catch (final UnsupportedEncodingException ex) {
            System.err.println(ex);
         } catch (final IOException ex) {
            System.err.println(ex);
         }
         mFormat = null;
      }
   }

   public NISTXRayTransitionDB() {
      if (mData == null) {
         synchronized (mData) {
            readData();
         }
      }
   }

   /**
    * getDefaultDatumType - Returns one of THEORY, BLEND, COMBINED, VAPOR or
    * UNAVAILABLE depending upon which type of value would be returned by the
    * getEnergy(XRayTransition xrt) method.
    * 
    * @param xrt
    *           XRayTransition
    * @return String
    */
   public String getDefaultDatumType(XRayTransition xrt) {
      final Datum d = mData.get(xrt);
      if (d != null)
         if (!Double.isNaN(d.mTheory))
            return THEORY;
         else if (!Double.isNaN(d.mDirect))
            return DIRECT;
         else if (!Double.isNaN(d.mCombined))
            return COMBINED;
         else if (!Double.isNaN(d.mVapor))
            return VAPOR;
      return UNAVAILABLE;
   }

   /**
    * getDefaultDatumType - Returns one of THEORY, BLEND, COMBINED, VAPOR or
    * UNAVAILABLE depending upon which type of value would be returned by the
    * getEdgeEnergy(AtomicShell shell) method.
    * 
    * @param shell
    *           AtomicShell
    * @return String
    */
   public String getDefaultDatumType(AtomicShell shell) {
      return getDefaultDatumType(new XRayTransition(shell, AtomicShell.Continuum));
   }

   /**
    * getEnergy - Gets the energy (in Joules) of the specified transition. If no
    * data exists for the specified transition, NaN is returned. The data
    * available for each transition is searched in the order THEORY, BLEND,
    * COMBINED, VAPOR for the first valid value. This value is returned.
    * 
    * @param xrt
    *           XRayTransition
    * @return double
    */
   public double getEnergy(XRayTransition xrt) {
      final Datum d = mData.get(xrt);
      if (d != null)
         if (Double.isNaN(d.mTheory))
            return d.mTheory;
         else if (Double.isNaN(d.mDirect))
            return d.mDirect;
         else if (Double.isNaN(d.mCombined))
            return d.mCombined;
         else if (Double.isNaN(d.mVapor))
            return d.mVapor;
      return Double.NaN;
   }

   /**
    * getEnergy - Returns the energy (in Joules) for the specific transition.
    * This method allows the user to specify which type of value they would like
    * from THEORY, BLEND, COMBINED and VAPOR. If this value is unavailable this
    * method returns Double.NaN.
    * 
    * @param xrt
    *           XRayTransition
    * @param type
    *           String
    * @return double
    */

   public double getEnergy(XRayTransition xrt, String type) {
      final Datum d = mData.get(xrt);
      if (d != null)
         if (type.equals(THEORY))
            return d.mTheory;
         else if (type.equals(DIRECT))
            return d.mDirect;
         else if (type.equals(COMBINED))
            return d.mCombined;
         else if (type.equals(VAPOR))
            return d.mVapor;
      return Double.NaN;
   }

   /**
    * getEdgeEnergy - Returns the energy (in Joules) for the specific edge. This
    * method allows the user to specify which type of value they would like from
    * THEORY, BLEND, COMBINED and VAPOR. If this value is unavailable this
    * method returns Double.NaN.
    * 
    * @param shell
    *           AtomicShell
    * @param type
    *           String
    * @return double
    */
   public double getEdgeEnergy(AtomicShell shell, String type) {
      return getEnergy(new XRayTransition(shell, AtomicShell.Continuum), type);
   }

   /**
    * getEdgeEnergy - Returns the edge energy for the specified AtomicShell
    * (Element and shell.)
    * 
    * @param sh
    *           AtomicShell
    * @return double
    */
   public double getEdgeEnergy(AtomicShell sh) {
      return getEnergy(new XRayTransition(sh, AtomicShell.Continuum));
   }
}
