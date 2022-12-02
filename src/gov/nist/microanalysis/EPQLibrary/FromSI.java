package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * A class made up of static methods for converting from other units to standard
 * SI units
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

final public class FromSI {

   /**
    * Joules -&gt; KEV
    */
   public static final double KEV = 1.0 / (PhysicalConstants.ElectronCharge * 1.0e3);
   /**
    * Joules -&gt; EV
    */
   public static final double EV = 1.0 / PhysicalConstants.ElectronCharge;
   /**
    * kg -&gt; g
    */
   public static final double GRAM = 1000.0;
   /**
    * m -&gt; cm
    */
   public static final double CM = 100.0;

   /**
    * m -&gt; micrometers
    */
   public static final double MICROMETER = 1.0e6;
   /**
    * kg -&gt; AMU
    */
   public static final double AMU = 1.0 / PhysicalConstants.UnifiedAtomicMass;

   /**
    * m -&gt; angstrom
    */
   public static final double ANGSTROM = 1.0e10;
   /**
    * pascal -&gt; Torr
    */
   public static final double TORR = 760.0 / PhysicalConstants.StandardAtmosphere;

   public static final double NANO = 1.0e9;
   public static final double PICO = 1.0e12;

   /**
    * Torr - Converts from pascal (Pa) to Torr
    * 
    * @param pascal
    * @return The equivalent pressure in Torr
    */
   public static double Torr(double pascal) {
      return TORR * pascal;
   }

   /**
    * keV - Converts an energy in Joules into keV.
    * 
    * @param e
    *           double - The energy in Joules
    * @return double
    */
   public static double keV(double e) {
      return e * KEV;
   }

   /**
    * eV - Converts an energy in Joules into eV.
    * 
    * @param e
    *           double - The energy in Joules
    * @return double - The energy in eV
    */
   public static double eV(double e) {
      return e * EV;
   }

   /**
    * AMU - Converts a mass in kilograms into AMU.
    * 
    * @param kg
    *           double - The mass in kg
    * @return double - The mass in AMU
    */
   public static double AMU(double kg) {
      return AMU * kg;
   }

   /**
    * dyne - Converts a force in Newtons into Dynes.
    * 
    * @param f
    *           double - The force in Newtons
    * @return double - The force in Dynes.
    */
   public static double dyne(double f) {
      return 1.0e5 * f;
   }

   /**
    * gPerCC - Converts kg per cubic meter into grams per cubic centimeter.
    * 
    * @param d
    *           double
    * @return double
    */
   public static double gPerCC(double d) {
      return d * (GRAM / (CM * CM * CM));
   }

   /**
    * angstrom - Converts from meters to angstroms.
    * 
    * @param a
    *           double
    * @return double
    */
   public static double angstrom(double a) {
      return ANGSTROM * a;
   }

   /**
    * sqrAngstrom - Converts from square meters to square angstroms.
    * 
    * @param a2
    *           double
    * @return double
    */
   public static double sqrAngstrom(double a2) {
      return ANGSTROM * ANGSTROM * a2;
   }

   /**
    * cmSqrPerg - Converts mass absorption coefficients from SI.
    * 
    * @param x
    *           double
    * @return double
    */
   public static double cmSqrPerg(double x) {
      return ((CM * CM) / GRAM) * x;
   }

   /**
    * cm - Converts from meters to cm
    * 
    * @param x
    * @return double
    */
   public static double cm(double x) {
      return x * CM;
   }

   /**
    * Converts from meters to micrometers
    * 
    * @param x
    * @return double
    */
   public static double micrometer(double x) {
      return x * MICROMETER;
   }

   /**
    * Converts from meters to nanometers
    * 
    * @param x
    * @return double
    */
   public static double nanometer(double x) {
      return x * NANO;
   }

   /**
    * centigrade - Converts from kelvin to centigrade
    * 
    * @param c
    * @return The equivalent temperature in kelvin
    */
   public static double centigrade(double c) {
      return c - PhysicalConstants.IcePoint;
   }

   /**
    * fahrenheit - Converts from fahrenheit to kelvin
    * 
    * @param k
    * @return The equivalent temperature in kelvin
    */
   public static double fahrenheit(double k) {
      return 32.0 + ((centigrade(k) * 9.0) / 5.0);
   }

   public static UncertainValue2 ugPcm2(Number v) {
      return UncertainValue2.multiply(1.0e5, v);
   }

}
