/**
 * gov.nist.nanoscalemetrology.JMONSEL.IConductionModel Created by: jvillar
 * Date: Mar 28, 2011
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.NISTMonte.MeshElementRegion;
import gov.nist.microanalysis.NISTMonte.MeshedRegion;

/**
 * <p>
 * Interface for conduction models as part of charge modeling in insulators.
 * Classes that conform to this interface provide a conductCharges method that
 * moves charges that have accumulated in a meshed region.
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
public interface IConductionModel {

	/**
	 * This method moves charges within the specified region in conformity to the
	 * model of the implementing class. Since the amount of charge flow is in
	 * general time-dependent, the second argument is a charge flow time. If the
	 * model results in movement of charges, the method returns true, otherwise
	 * false.
	 *
	 * @param meshedRegion - MeshedRegion specifying the region in which to
	 *                     propagate charges.
	 * @param deltat       - double specifying the time for which to propagate
	 *                     charges.
	 * @param feaRunner    - An IFEArunner that can be used to do finite element
	 *                     analysis if needed by the algorithm
	 * @return - true if FEA is needed (e.g., if any charges were moved and without
	 *         subsequent FEA).
	 */
	public boolean conductCharges(MeshedRegion meshedRegion, double deltat, IFEArunner feaRunner);

	/**
	 * When the scattering code decides to stop following an electron in a nominally
	 * insulating region, it calls this routine to determine what region the
	 * electron should stop in. The input startRegion is the electron's region when
	 * the scattering code is finished with it. The conduction model may, e.g., if
	 * the electric field in this region is high enough, decide to put the electron
	 * in a different MeshElementRegion, which it returns. It may return null, e.g.,
	 * if the stopRegion lies outside the mesh.
	 *
	 * @param startRegion - The region in which the electron starts
	 * @return - The MeshElementRegion in which it stops
	 */
	public MeshElementRegion stopRegion(MeshElementRegion startRegion);

}
