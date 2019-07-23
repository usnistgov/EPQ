/**
 * gov.nist.nanoscalemetrology.JMONSEL.IFEArunner Created by: John Villarrubia
 * Date: Jun 8, 2011
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.NISTMonte.MeshedRegion;

/**
 * <p>
 * Interface for classes that run finite element analysis. Every such class has
 * a runFEA(MeshedRegion) method that runs a finite element analysis on the
 * designated MeshedRegion. They may differ for example by using different
 * solvers or the same solver compiled with different linear algebra libraries.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
public interface IFEArunner {

   /**
    * Runs an FEA on this MeshedRegion, storing temporary files in the
    * designated folder.
    *
    * @param meshReg - the MeshedRegion on which to run the FEA
    */
   public void runFEA(MeshedRegion meshReg);

   /**
    * Sets the value assigned to chargeMultiplier. chargeMultiplier is a factor
    * applied to trapped charges before performing finite element analysis.
    *
    * @param chargeMultiplier The value to which to set chargeMultiplier.
    */
   public void setChargeMultiplier(double chargeMultiplier);

   /**
    * Returns the value assigned to chargeMultiplier. A charge of n in the mesh
    * is interpreted as n*e*chargeMultiplier, where e is the absolute value of
    * the electronic charge.
    */
   public double getChargeMultiplier();
}
