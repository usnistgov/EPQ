package gov.nist.microanalysis.NISTMonte;

import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase;

/**
 * An interface providing methods that are called each time an electron scatter
 * event occurs. The interface associates a Material-based class with scattering
 * properties.
 */
public interface IMaterialScatterModel {

   /**
    * Returns the material for which this class defines the scattering model.
    * 
    * @return Material
    */
   Material getMaterial();

   /**
    * Returns the minimum energy for which electrons should be tracked.
    * Electrons below this energy may be dropped. For example, it is common to
    * set this energy equal to the work function in secondary electron models,
    * because electrons with energy less than this cannot escape the sample.
    * 
    * @return double
    */
   double getMinEforTracking();

   /**
    * Sets the minimum energy for which electrons should be tracked in this
    * material. Electrons below this energy may be dropped.
    */
   void setMinEforTracking(double minEforTracking);

   /**
    * Returns a randomized length drawn from the mean path length distribution
    * for the electron pe in the Material represented by this object. pe should
    * not be changed by this routine.
    * 
    * @param pe -- the primary electron
    * @return double -- Free path in meters
    */
   double randomMeanPathLength(Electron pe);

   /**
    * Simulates scattering according to this scattering model. This method
    * should be called after the calling routine decides that a scattering event
    * of the type represented by this model does occur. This method may alter
    * the pe properties (e.g., energy, direction) as dictated by the outcome of
    * this scattering event. If the scattering event produces a secondary
    * electron, scatter() creates an Electron, initializes it with the
    * appropriate energy, trajectory, position, etc. and returns it. If no SE is
    * generated, scatter() returns null.
    * 
    * @param pe
    * @return Electron
    */
   Electron scatter(Electron pe);

   /**
    * barrierScatter - Perform scattering of an electron at the interface
    * between two materials. When called the primary electron (pe) position
    * should already be at the interface where scattering occurs. (This routine
    * may change the electron's direction, but it does not propagate it.)
    * scatter() alters the pe properties as dictated by the outcome of this
    * barrier scattering event. If the scattering produces a secondary electron,
    * barrierScatter() creates such an electron, initializes it appropriately,
    * and returns it. Otherwise it returns null.
    * 
    * @param pe Electron - a reference to the primary electron
    * @param nextRegion RegionBase - A reference to the region on the far side
    *           of the boundary
    * @return Electron - A secondary electron if one is generated or null if
    *         not.
    */
   Electron barrierScatter(Electron pe, RegionBase nextRegion);

   /**
    * calculateEnergyLoss - Calculate the amount of kinetic energy change when
    * the specified electron (pe) traverses a distance (len) in this region. The
    * loss is calculated but the pe energy is not updated by this routine. The
    * calling routine is responsible for doing so if desired. This function
    * should return a negative number to represent an energy loss....
    * 
    * @param len double - A distance in meters
    * @param pe Electron - The primary electron
    * @return double - The energy loss in joules
    */
   double calculateEnergyLoss(double len, Electron pe);
}