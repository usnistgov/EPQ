/**
 * <p>
 * <p>
 * A class describing the composition and density of a mixture of gases.
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

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * A GasMixture is a Material constructed from one or more Gases. The gases are
 * assume to occupy the same volume and to obey the ideal gas law.
 */
public class GasMixture extends Material {

   private static final long serialVersionUID = 0x42;

   private Gas[] mGases;
   private double mTemperature;

   /**
    * Create a gas mixture from its constituent gases. The gases are first
    * brought to a common temperature and then summed. The total pressure of the
    * gas is the sum of the partial pressures. For example, air is 78.084% N2,
    * 20.947% O2, 0.934% Ar, 0.033% CO2 and trace other elements (by volume).
    * Create each gas independently using
    * Gas(Element[],int[],double,double,String) then sum them together.
    * 
    * @param gases
    *           A list of constituent gases
    * @param temp
    *           The common temperature in kelvin
    * @param name
    *           The human friendly name for the gas
    */
   public GasMixture(Gas[] gases, double temp, String name) {
      super(0.0);
      assert (gases.length > 0);
      final Map<Element, Double> allElms = new TreeMap<Element, Double>();
      mGases = new Gas[gases.length];
      double mass = 0.0; // Total mass of all constituent atoms
      double atomCount = 0.0; // Total number of atoms
      for (int i = 0; i < gases.length; ++i) {
         mGases[i] = gases[i].clone();
         mGases[i].setTemperature(temp);
         for (final Element elm : mGases[i].getElementSet()) {
            double v = mGases[i].atomsPerCubicMeter(elm);
            atomCount += v;
            mass += elm.getMass() * v;
            if (allElms.containsKey(elm))
               v += (allElms.get(elm)).doubleValue();
            allElms.put(elm, Double.valueOf(v));
         }
      }
      for (final Map.Entry<Element, Double> me : allElms.entrySet())
         addElementByStoiciometry(me.getKey(), me.getValue().doubleValue() / atomCount);
      mTemperature = temp;
      setDensity(mass);
      setName(name);
   }

   /**
    * setTemperature - Modifies the temperature of the gas and updates the
    * pressure assuming constant volume.
    * 
    * @param newTemp
    *           In kelvin
    */
   public void setTemperature(double newTemp) {
      if (newTemp != mTemperature) {
         for (final Gas mGase : mGases)
            mGase.setTemperature(newTemp);
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
      double pressure = 0.0;
      for (final Gas mGase : mGases)
         pressure += mGase.getPressure();
      return pressure;
   }

   /**
    * createAir - Creates a gas with nominally the same composition as the air
    * we breath.
    * 
    * @param pressure
    * @param temperature
    * @return An instance of GasMixture
    */
   public static GasMixture createAir(double pressure, double temperature) {
      final Gas n2 = new Gas(new Element[]{Element.N}, new int[]{2}, 0.78084 * pressure, temperature, "N2");
      final Gas o2 = new Gas(new Element[]{Element.O}, new int[]{2}, 0.20947 * pressure, temperature, "O2");
      final Gas ar = new Gas(new Element[]{Element.Ar}, new int[]{1}, 0.00934 * pressure, temperature, "Ar");
      final Gas co2 = new Gas(new Element[]{Element.C, Element.O}, new int[]{1, 2}, 0.00033 * pressure, temperature, "CO2");
      return new GasMixture(new Gas[]{n2, o2, ar, co2}, temperature, "Air");
   }

   private void writeObject(java.io.ObjectOutputStream out) throws IOException {
      out.writeInt(mGases.length);
      for (final Gas mGase : mGases)
         out.writeObject(mGase);
      out.writeDouble(mTemperature);
   }

   private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
      final int sz = in.readInt();
      mGases = new Gas[sz];
      for (int i = 0; i < mGases.length; ++i)
         mGases[i] = (Gas) in.readObject();
      mTemperature = in.readDouble();
   }

}
