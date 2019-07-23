package gov.nist.microanalysis.EPQLibrary;

import java.io.IOException;
import java.text.NumberFormat;

import gov.nist.microanalysis.Utility.HalfUpFormat;

/**
 * <p>
 * Implements a gas based on the ideal gas law. It should be noted that this
 * model of gases is niave (from many perspectives). The model views all
 * interactions as essentially atomic. No molecular considerations are included
 * except in as much as a mole of O2 has twice as many atoms as a mole of O.
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
public class Gas
   extends Material {

   static private final long serialVersionUID = 0x43;

   public static final double RYDBERG_CONSTANT = PhysicalConstants.BoltzmannConstant * PhysicalConstants.AvagadroNumber; // J
   // per
   // K
   // per
   // mole
   public static final double MOLAR_VOLUME = 0.022414; // cubic meters per
   // mole at 0 C & 1 ATM
   public static final double STANDARD_PRESSURE = 101325.0; // pascals
   public static final double STANDARD_TEMPERATURE = 273.15; // kelvin

   private double mPressure; // Pressure in pascals
   private double mTemperature; // Temperature in Kelvin

   /**
    * getMassPerSubunit - For used with materials defined by stoichiometry.
    * Returns the mass of a single molecular subunit whether it be a gas such as
    * N2 or a crystal unit cell.
    * 
    * @return - The mass in kg per molecular subunit
    */
   public double getMassPerSubunit() {
      double res = 0.0;
      for(final Element elm : getElementSet())
         res += atomicPercent(elm) * elm.getMass();
      return res;
   }

   protected Gas() {
      super(0.0);
      mTemperature = STANDARD_TEMPERATURE;
      mPressure = STANDARD_PRESSURE;
   }

   /**
    * Constructs a single molecular gas such as N2, O2, Ar, CO2 etc.
    * 
    * @param elms The elemental constituents
    * @param stoic The molecular stoiciometry
    * @param pressure in pascal
    * @param temperature in kelvin
    * @param name a human-friendly name
    */
   public Gas(Element[] elms, int[] stoic, double pressure, double temperature, String name) {
      super(0.0);
      for(int i = 0; i < elms.length; ++i)
         addElementByStoiciometry(elms[i], stoic[i]);
      mTemperature = temperature;
      mPressure = pressure;
      // n = (P*V)/(R*T)
      final double n = (1.0 /* m^3 */ * pressure) / (RYDBERG_CONSTANT * temperature);
      setDensity((getMassPerSubunit() * PhysicalConstants.AvagadroNumber) * n);
      setName(name);
   }

   /**
    * Constructs a single molecular gas such as N2, O2, Ar, CO2 etc.
    * 
    * @param comp The molecular stoiciometry
    * @param pressure in pascal
    * @param temperature in kelvin
    */
   public Gas(Composition comp, double pressure, double temperature) {
      super(0.0);
      for(Element elm : comp.getElementSet())
         addElement(elm, comp.weightFraction(elm, false));
      mTemperature = temperature;
      mPressure = pressure;
      // n = (P*V)/(R*T)
      final double n = (1.0 /* m^3 */ * pressure) / (RYDBERG_CONSTANT * temperature);
      setDensity((getMassPerSubunit() * PhysicalConstants.AvagadroNumber) * n);
      final NumberFormat fmt = mPressure > 1.0 ? new HalfUpFormat("0.0") : new HalfUpFormat("0.0E0");
      setName(comp.getName() + " gas at " + fmt.format(mPressure) + " Pa");
   }

   /**
    * setTemperature - Modifies the temperature of the gas and updates the
    * pressure assuming constant volume.
    * 
    * @param newTemp In kelvin
    */
   public void setTemperature(double newTemp) {
      if(newTemp != mTemperature) {
         mPressure *= (newTemp / mTemperature);
         mTemperature = newTemp;
      }
   }

   /**
    * getTemperature - Returns the temperature (in kelvin)
    * 
    * @return The temperature in kelvin
    */
   public double getTemperature() {
      return mTemperature;
   }

   /**
    * getPressure - returns the pressure (in pascal)
    * 
    * @return The pressure in pascal
    */
   public double getPressure() {
      return mPressure;
   }

   protected void replicate(Gas gas) {
      super.replicate(gas);
      gas.mPressure = mPressure;
      gas.mTemperature = mTemperature;
   }

   @Override
   public Gas clone() {
      final Gas res = new Gas();
      replicate(res);
      return res;
   }

   private void writeObject(java.io.ObjectOutputStream out)
         throws IOException {
      out.writeDouble(mPressure);
      out.writeDouble(mTemperature);
   }

   private void readObject(java.io.ObjectInputStream in)
         throws IOException, ClassNotFoundException {
      mPressure = in.readDouble();
      mTemperature = in.readDouble();
   }

}
