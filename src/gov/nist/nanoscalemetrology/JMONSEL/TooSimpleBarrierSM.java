/**
 *
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase;

/**
 * <p>
 * This is a poor barrier scattering model for SE, and its use is discouraged.
 * It is temporarily here for debugging purposes. It lets all electrons transmit
 * except those with energy less than the work function.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain.
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
@Deprecated
public class TooSimpleBarrierSM implements BarrierScatterMechanism {

   /**
    *
    */
   public TooSimpleBarrierSM() {
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * gov.nist.nanoscalemetrology.JMONSEL.BarrierScatterMechanism#barrierScatter
    * (gov.nist.microanalysis.NISTMonte.Electron,
    * gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase)
    */
   @Override
   public Electron barrierScatter(Electron pe, RegionBase nextRegion) {
      /*
       *
       */
      if (pe.getEnergy() < ((SEmaterial) pe.getCurrentRegion().getMaterial()).getWorkfunction())
         pe.setEnergy(0.);
      else
         pe.setCurrentRegion(nextRegion);
      return null;
   }
}
