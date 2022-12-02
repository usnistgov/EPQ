package gov.nist.microanalysis.EPQLibrary;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * <p>
 * A simple class for loading tables of Mott cross-section values and then
 * interpolating between them to return cross-sections. The tables are those of
 * Czyzewski, MacCallum, Romig &amp; Joy in J. Appl. Phys. Vol 68, No 7, 1990 as
 * downloaded from http://pciserver.bio.utk.edu/metrology/htm/download.shtml. I
 * have noticed that the tables for the Rb cross section may be in error. The Rb
 * cross section has a bulge at high scattering angle. The result is too large a
 * backscattered electron yield.
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

public class CzyzewskiMottCrossSection {
   public static final int SpecialEnergyCount = 26;
   public static final int SpecialAngleCount = 96;
   public static final double MaxEnergy = ToSI.keV(30.0);
   private static final int kTotalIndex = 96;
   private static final int kMeanFreePathIndex = 97;

   private static final double kEnergy[] = {ToSI.eV(20), ToSI.eV(50), ToSI.eV(75), ToSI.eV(100), ToSI.eV(200), ToSI.eV(300), ToSI.eV(400),
         ToSI.eV(500), ToSI.eV(600), ToSI.eV(700), ToSI.eV(800), ToSI.eV(900), ToSI.keV(1), ToSI.keV(2), ToSI.keV(3), ToSI.keV(4), ToSI.keV(5),
         ToSI.keV(6), ToSI.keV(7), ToSI.keV(8), ToSI.keV(9), ToSI.keV(10), ToSI.keV(15), ToSI.keV(20), ToSI.keV(25), ToSI.keV(30)};

   private static final double kAngle[] = {Math.toRadians(0.0), Math.toRadians(1), Math.toRadians(2), Math.toRadians(3), Math.toRadians(4),
         Math.toRadians(5), Math.toRadians(6), Math.toRadians(7), Math.toRadians(8), Math.toRadians(9), Math.toRadians(10), Math.toRadians(12),
         Math.toRadians(14), Math.toRadians(16), Math.toRadians(18), Math.toRadians(20), Math.toRadians(22), Math.toRadians(24), Math.toRadians(26),
         Math.toRadians(28), Math.toRadians(30), Math.toRadians(32), Math.toRadians(34), Math.toRadians(36), Math.toRadians(38), Math.toRadians(40),
         Math.toRadians(42), Math.toRadians(44), Math.toRadians(46), Math.toRadians(48), Math.toRadians(50), Math.toRadians(52), Math.toRadians(54),
         Math.toRadians(56), Math.toRadians(58), Math.toRadians(60), Math.toRadians(62), Math.toRadians(64), Math.toRadians(66), Math.toRadians(68),
         Math.toRadians(70), Math.toRadians(72), Math.toRadians(74), Math.toRadians(76), Math.toRadians(78), Math.toRadians(80), Math.toRadians(82),
         Math.toRadians(84), Math.toRadians(86), Math.toRadians(88), Math.toRadians(90), Math.toRadians(92), Math.toRadians(94), Math.toRadians(96),
         Math.toRadians(98), Math.toRadians(100), Math.toRadians(102), Math.toRadians(104), Math.toRadians(106), Math.toRadians(108),
         Math.toRadians(110), Math.toRadians(112), Math.toRadians(114), Math.toRadians(116), Math.toRadians(118), Math.toRadians(120),
         Math.toRadians(122), Math.toRadians(124), Math.toRadians(126), Math.toRadians(128), Math.toRadians(130), Math.toRadians(132),
         Math.toRadians(134), Math.toRadians(136), Math.toRadians(138), Math.toRadians(140), Math.toRadians(142), Math.toRadians(144),
         Math.toRadians(146), Math.toRadians(148), Math.toRadians(150), Math.toRadians(152), Math.toRadians(154), Math.toRadians(156),
         Math.toRadians(158), Math.toRadians(160), Math.toRadians(162), Math.toRadians(164), Math.toRadians(166), Math.toRadians(168),
         Math.toRadians(170), Math.toRadians(172), Math.toRadians(174), Math.toRadians(176), Math.toRadians(178), Math.toRadians(180)};

   private final Element mElement;
   transient private float[][] mValues;

   private void loadTables(int atomicNo) {
      final String name = "CzyzewskiXSec/" + (atomicNo < 10 ? "0" + Integer.toString(atomicNo) : Integer.toString(atomicNo)) + ".dat";
      try {
         try (final InputStreamReader isr = new InputStreamReader(CzyzewskiMottCrossSection.class.getResourceAsStream(name), "US-ASCII")) {
            try (final BufferedReader br = new BufferedReader(isr)) {

               mValues = new float[SpecialEnergyCount][kMeanFreePathIndex + 1];
               final String rl = br.readLine();
               if (rl == null)
                  throw new EPQException();
               StringTokenizer st = new StringTokenizer(rl, " ");
               // To ensure that numbers are parsed correctly
               final NumberFormat nf = NumberFormat.getInstance(Locale.US);
               for (int r = 0; r < SpecialEnergyCount; ++r)
                  for (int c = 0; c <= kMeanFreePathIndex; ++c) {
                     if (!st.hasMoreTokens()) {
                        String ss = br.readLine();
                        if (ss == null)
                           throw new EPQException("Ran out of data.");
                        st = new StringTokenizer(ss, " ");
                     }
                     String tmp = st.nextToken();
                     // Delete the pesky '+' in the exponent...
                     final int p = tmp.indexOf('+');
                     if (p != -1)
                        tmp = tmp.substring(0, p) + tmp.substring(p + 1, tmp.length());
                     final Number n = nf.parse(tmp);
                     mValues[r][c] = n.floatValue();
                  }
            }
         }
      } catch (final Exception ex) {
         throw new EPQFatalException("Fatal error loading a Mott cross section data file. - " + name);
      }
   }

   /**
    * MottCrossSection - Creates an object representing the Mott cross section
    * for the specified element.
    * 
    * @param el
    *           Element
    */
   public CzyzewskiMottCrossSection(Element el) {
      mElement = el;
      loadTables(el.getAtomicNumber());
   }

   /**
    * toString
    * 
    * @return String
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "CzyzewskiMott[" + mElement.toAbbrev() + "]";
   }

   public Element getElement() {
      return mElement;
   }

   /**
    * getSpecialEnergy - Returns the i-th energy at which the cross-section is
    * actually represented in the table.
    * 
    * @param index
    *           int
    * @return double - The energy in Joules
    */
   public static double getSpecialEnergy(int index) {
      return kEnergy[index];
   }

   /**
    * getSpecialAngle - Gets the i-th angle which is actually represented in the
    * tables.
    * 
    * @param index
    *           int
    * @return double
    */
   public static double getSpecialAngle(int index) {
      return kAngle[index];
   }

   /**
    * getEnergyIndex - Returns the index of the energy value that is strictly
    * equal to or larger than the specified energy.
    * 
    * @param energy
    *           double
    * @return int
    */
   public static int getEnergyIndex(double energy) {
      final int ei = Arrays.binarySearch(kEnergy, energy);
      return ei >= 0 ? ei : -(ei + 1);
   }

   /**
    * totalCrossSection - Calculates the total cross-section for an electron of
    * the specified energy.
    * 
    * @param energy
    *           double - The electron energy in Joules
    * @return double
    */
   public double totalCrossSection(double energy) {
      int ei = Arrays.binarySearch(kEnergy, energy);
      if (ei >= 0)
         return ToSI.sqrAngstrom(mValues[ei][kTotalIndex]);
      else {
         ei = -(ei + 1);
         return ToSI.sqrAngstrom(mValues[ei - 1][kTotalIndex]
               - ((energy - kEnergy[ei - 1]) / (kEnergy[ei] - kEnergy[ei - 1])) * (mValues[ei - 1][kTotalIndex] - mValues[ei][kTotalIndex]));
      }
   }

   /**
    * partialCrossSection - Computes the partial cross-section for an electron
    * of the specified energy scattered to the specified elevation and azimuth.
    * 
    * @param elevation
    *           angle double - The angle in radians.
    * @param azimuth
    *           double - The angle in radians.
    * @param energy
    *           double - The energy in Joules.
    * @return double
    */
   public double partialCrossSection(double elevation, double azimuth, double energy) {
      int ei = Arrays.binarySearch(kEnergy, energy);
      if (ei < 0)
         ei = -(ei + 1);
      assert (energy <= getSpecialEnergy(ei));
      assert ((ei == 0) || (energy >= getSpecialEnergy(ei - 1)));
      if (ei == 0)
         ei = 1; // Extrapolate across the boundary...
      int ai = Arrays.binarySearch(kAngle, elevation);
      if (ai < 0)
         ai = -(ai + 1);
      assert (elevation <= getSpecialAngle(ai));
      assert ((ai == 0) || (elevation >= getSpecialAngle(ai - 1)));
      if (ai == 0)
         ai = 1;
      final double t = (energy - kEnergy[ei - 1]) / (kEnergy[ei] - kEnergy[ei - 1]);
      final double u = (elevation - kAngle[ai - 1]) / (kAngle[ai] - kAngle[ai - 1]);
      return ToSI.sqrAngstrom((1.0 - t) * (1.0 - u) * mValues[ei - 1][ai - 1] + t * (1.0 - u) * mValues[ei][ai - 1] + t * u * mValues[ei][ai]
            + (1.0 - t) * u * mValues[ei - 1][ai]);
   }

   /**
    * partialCrossSection - Computes the partial cross-section for an electron
    * of the specified energy scattered to the specified elevation. The cross
    * section is integrated over azimuthal angle.
    * 
    * @param elevation
    *           double - The elevation angle in radians.
    * @param energy
    *           double - The energy in Joules.
    * @return double
    */
   public double partialCrossSection(double elevation, double energy) {
      return partialCrossSection(elevation, 0, energy) * 2.0 * Math.PI * Math.sin(elevation);
   }

   /**
    * meanFreePath - Calculates the mean free path for this element at the
    * specified element (in meters)
    * 
    * @param energy
    *           double
    * @return double
    */
   public double meanFreePath(double energy) {
      int ei = Arrays.binarySearch(kEnergy, energy);
      if (ei >= 0)
         return ToSI.angstrom(mValues[ei][kTotalIndex]);
      else {
         ei = -(ei + 1);
         return ToSI.angstrom(mValues[ei - 1][kMeanFreePathIndex] + (energy - kEnergy[ei - 1]) / (kEnergy[ei] - kEnergy[ei - 1])
               * (mValues[ei - 1][kMeanFreePathIndex] - mValues[ei][kMeanFreePathIndex]));
      }
   }
}
