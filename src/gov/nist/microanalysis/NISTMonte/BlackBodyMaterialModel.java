package gov.nist.microanalysis.NISTMonte;

import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase;

/**
 * This scattering model mimics a black block where when the electron enters
 * they loose all their energies with any scattering.
 * 
 * @author Philippe T. Pinard
 */
public class BlackBodyMaterialModel implements IMaterialScatterModel {

   /** Material with infinite density. */
   private static final Material BLACK_BODY_MATERIAL = new Material(Double.POSITIVE_INFINITY);

   /** Minimum energy for tracking. */
   private double minEforTracking = ToSI.eV(50.0);

   @Override
   public Material getMaterial() {
      return BLACK_BODY_MATERIAL;
   }

   @Override
   public double randomMeanPathLength(Electron pe) {
      return 0.0;
   }

   @Override
   public Electron scatter(Electron pe) {
      return null; // No SE generated
   }

   @Override
   public Electron barrierScatter(Electron pe, RegionBase nextRegion) {
      pe.setCurrentRegion(nextRegion);
      pe.setScatteringElement(null);
      return null;
   }

   @Override
   public double calculateEnergyLoss(double len, Electron pe) {
      return -Double.POSITIVE_INFINITY;
   }

   @Override
   public double getMinEforTracking() {
      return minEforTracking;
   }

   @Override
   public void setMinEforTracking(double minEforTracking) {
      this.minEforTracking = minEforTracking;
   }

}