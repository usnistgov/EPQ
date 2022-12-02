package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.NISTMonte.Electron;

/**
 * <p>
 * An abstract class for defining scatter mechanisms. A scatter mechanism (e.g.,
 * Elastic scattering, Moller SE generation, plasmon SE generation, Gryzinski SE
 * generation,...) Such a mechanism represents a type of event, characterized by
 * its likelihood of occurring and its effects if it does occur.
 * </p>
 * <p>
 * Its likelihood of occurring may be characterized by a cross section, mean
 * free path, or scatting rate. Here we use scattering rate. It is in general a
 * function of the material in which the electron is traveling and the
 * electron's kinetic energy.
 * </p>
 * <p>
 * If an event does occur, its effect on the primary electron is characterized
 * by a new energy and direction of travel. It also generates an SE with some
 * probability (possibly 0, as for instance for elastic scattering). If it
 * generates an SE, the SE must be assigned an initial energy and direction of
 * motion.
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
abstract public class ScatterMechanism implements Cloneable {

   /**
    * Returns the reciprocal of the mean free path.
    *
    * @param pe
    *           - the primary electron
    * @return double Reciprocal of mfp in inverse meters
    */
   abstract public double scatterRate(Electron pe);

   /**
    * Updates properties of the primary electron based on results of scattering
    * and returns either a secondary electron or null.
    *
    * @param pe
    *           -- the primary electron
    * @return Electron -- the generated secondary electron or null
    */
   abstract public Electron scatter(Electron pe);

   /**
    * Sets the material within which the electron scatters. This method
    * typically precomputes and caches combinations of material properties
    * required by the scattering model.
    *
    * @param mat
    */
   abstract public void setMaterial(Material mat);

   @Override
   public ScatterMechanism clone() throws CloneNotSupportedException {
      return (ScatterMechanism) super.clone();
   }

}
