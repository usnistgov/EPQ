package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * A handful of static methods for converting conventional units into SI.
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

final public class ToSI {

   /**
    * MeV -&gt; Joules
    */
   public static final double MEV = (PhysicalConstants.ElectronCharge * 1.0e6);

   /**
    * keV -&gt; Joules
    */
   public static final double KEV = (PhysicalConstants.ElectronCharge * 1.0e3);

   /**
    * eV -&gt; Joules
    */
   public static final double EV = PhysicalConstants.ElectronCharge;

   /**
    * g -&gt; kg
    */
   public static final double GRAM = 1.0e-3;

   /**
    * cm -&gt; m
    */
   public static final double CM = 1.0e-2;

   /**
    * micrometer -&gt; m
    */
   public static final double MICROMETER = 1.0e-6;

   /**
    * AMU -&gt; kg (1 amu = 1.66e-27 kg (mass(C12)/12))
    */
   public static final double AMU = PhysicalConstants.UnifiedAtomicMass;
   /**
    * angstrom -&gt; m
    */
   public static final double ANGSTROM = 1.0e-10;

   /**
    * barn -&gt; m^2
    */
   public static final double BARN = 1.0e-28;

   /**
    * Torr -&gt; Pascal
    */
   public static final double TORR = PhysicalConstants.StandardAtmosphere / 760.0;

   public static final double NANO = 1.0e-9;
   public static final double PICO = 1.0e-12;

   /**
    * Torr - Converts from Torr to pascal (Pa)
    * 
    * @param torr
    * @return The equivalent pressure in pascal
    */
   public static double Torr(double torr) {
      return TORR * torr;
   }

   /**
    * MeV - Converts an energy in MeV into Joules.
    * 
    * @param e double - The energy in MeV
    * @return double
    */
   public static double MeV(double e) {
      return MEV * e;
   }

   /**
    * keV - Converts an energy in keV into Joules.
    * 
    * @param e double - The energy in keV
    * @return double
    */
   public static double keV(double e) {
      return KEV * e;
   }

   /**
    * eV - Converts an energy in eV into Joules.
    * 
    * @param e double - The energy in eV
    * @return double - The energy in Joules
    */
   public static double eV(double e) {
      return EV * e;
   }

   /**
    * AMU - Converts a mass in AMU into kilograms.
    * 
    * @param amu double - The mass in AMU
    * @return double - The mass in kg
    */
   public static double AMU(double amu) {
      return AMU * amu;
   }

   /**
    * dyne - Converts a force in Dynes into Newtons.
    * 
    * @param f double - The force in Dynes
    * @return double - The force in Newtons.
    */
   public static double dyne(double f) {
      return 1.0e-5 * f;
   }

   /**
    * gPerCC - Converts grams per cubic centimeter into kg per cubic meter.
    * 
    * @param d double
    * @return double
    */
   public static double gPerCC(double d) {
      return (d * GRAM) / (CM * CM * CM);
   }

   /**
    * inverse_gPerCC - Converts from inverse(grams per cubic centimeter) into
    * inverse(kg per cubic meter).
    * 
    * @param d double
    * @return double
    */
   public static double inverse_gPerCC(double d) {
      return (d * (CM * CM * CM)) / GRAM;
   }

   /**
    * percm - Converts from 1/cm to 1/meter.
    * 
    * @param d double
    * @return double
    */
   public static double percm(double d) {
      return d * (1 / CM);
   }

   /**
    * angstrom - Converts from angstroms to meters.
    * 
    * @param a double
    * @return double
    */
   public static double angstrom(double a) {
      return ANGSTROM * a;
   }

   public static double micrometer(double m) {
      return MICROMETER * m;
   }

   /**
    * sqrAngstrom - Converts from square angstroms to square meters.
    * 
    * @param a2 double
    * @return double
    */
   public static double sqrAngstrom(double a2) {
      return ANGSTROM * ANGSTROM * a2;
   }

   /**
    * barn - Converts from barns to square meters
    * 
    * @param a2 double
    * @return double
    */
   public static double barn(double a2) {
      return BARN * a2; // meters^2 per barn
   }

   /**
    * cmSqrPerg - Converts mass absorption coefficients to SI.
    * 
    * @param x double
    * @return double
    */
   public static double cmSqrPerg(double x) {
      return ((CM * CM) / GRAM) * x;
   }

   /**
    * cm - Converts from cm to meters
    * 
    * @param x
    * @return double
    */
   public static double cm(double x) {
      return x * CM;
   }

   /**
    * g - Converts from grams to kilograms
    * 
    * @param x
    * @return double
    */
   public static double g(double x) {
      return x * GRAM;
   }

   /**
    * centigrade - Converts from centigrade to kelvin
    * 
    * @param k
    * @return The equivalent temperature in centigrade
    */
   public static double centigrade(double k) {
      return k + PhysicalConstants.IcePoint;
   }

   /**
    * fahrenheit - Converts from fahrenheit to kelvin
    * 
    * @param f
    * @return The equivalent temperature in fahrenheit
    */
   public static double fahrenheit(double f) {
      return centigrade(((f - 32.0) * 5.0) / 9.0);
   }

   public static UncertainValue2 ugPcm2(Number v) {
      return UncertainValue2.multiply(1.0e-5, v);
   }

}
