package gov.nist.microanalysis.EPQLibrary;

import java.util.HashMap;

/**
 * <p>
 * Implements a simple class for caching mass absorption coefficient values.
 * Calculating MACs can be CPU intensive and since they are required so often by
 * NISTMonte, this class offers a substancial performance optimization.
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

public class MACCache {
   private static final double RESOLUTION = ToSI.eV(1.0);

   private final MassAbsorptionCoefficient mMac;
   private HashMap<Material, double[]> mMACs = new HashMap<Material, double[]>(); // Maps
   private final double mMaxEnergy;
   /**
    * The value returned by getMAC(...) when the Material/energy combination is
    * not currently stored in the cache. Note: NOT_IN_CACHE = Double.MAX_VALUE
    * so regular == operators work (This is not the case for Double.NaN).
    */
   public static final double NOT_IN_CACHE = Double.MAX_VALUE;

   /**
    * MACCache - Create a new MACCache object.
    */
   public MACCache(double maxE, MassAbsorptionCoefficient mac) {
      mMac = mac;
      mMACs = new HashMap<Material, double[]>();
      mMaxEnergy = maxE;
   }

   public void clear() {
      mMACs.clear();
   }

   /**
    * getMAC - get a MAC from the cache. Returns a number less than zero when
    * the MAC is not in the cache.
    * 
    * @param mat Material - The absorber material
    * @param energy double - The x-ray energy
    * @return double - The MAC or a value equal to MACCache.NOT_IN_CACHE when
    *         the MAC is not in the cache.
    */
   public double getMAC(Material mat, double energy) {
      if(false) {
         double[] vals = mMACs.get(mat);
         if(vals == null) {
            initialize(mat);
            vals = mMACs.get(mat);
         }
         assert vals != null;
         final int index = (int) Math.round(energy / RESOLUTION);
         assert index < vals.length;
         return index < vals.length ? vals[index] : NOT_IN_CACHE;
      } else
         return mMac.compute(mat, energy);
   }

   private void initialize(Material mat) {
      assert mMACs.get(mat) == null;
      final int len = (int) Math.ceil(mMaxEnergy / RESOLUTION);
      final double[] vals = new double[len];
      for(int i = 0; i < len; ++i)
         vals[i] = mMac.compute(mat, i * RESOLUTION);
      mMACs.put(mat, vals);
   }
}
