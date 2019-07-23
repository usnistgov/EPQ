/**
 *
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase;

/**
 * <p>
 * An interface for defining barrier scatter mechanisms. Barrier scattering
 * occurs when an electron encounters a boundary between two materials.
 * Differences in the potential energy (work function, etc.) in the two
 * materials may cause a change in the electron's energy or trajectory.
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
public interface BarrierScatterMechanism {

   /**
    * Simulates scattering of an electron at a barrier. The routine accepts a
    * reference to the primary electron (PE), from which it may ascertain the
    * electron's current region. It also accepts a reference (nextregion) to the
    * RegionBase on the other side of the boundary. If the electron is
    * transmitted, the PE region is updated to nextregion and its energy and
    * trajectory are altered as needed to simulate the scattering event. If it
    * is reflected its trajectory is updated. If a secondary electron is
    * produced, barrierScatter creates one with appropriate energy and
    * trajectory and returns it. Otherwise it returns null.
    *
    * @param pe -- the primary electron
    * @param nextRegion -- the region on the other side of the boundary
    * @return Electron -- a secondary electron if there is one (null otherwise).
    */
   Electron barrierScatter(Electron pe, RegionBase nextRegion);

}
