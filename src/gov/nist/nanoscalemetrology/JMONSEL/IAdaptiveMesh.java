/**
 * gov.nist.nanoscalemetrology.JMONSEL.IAdaptiveMesh Created by: jvillar Date:
 * Apr 22, 2015
 */
package gov.nist.nanoscalemetrology.JMONSEL;

/**
 * <p>
 * An adaptive mesh is a mesh that might change when an FEA is performed. This
 * interface extends the IBasicMesh by specifying several additional methods
 * that are needed to manage such meshes.
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
public interface IAdaptiveMesh extends IBasicMesh {

	/**
	 * Returns an integer revision number such that whenever if the number remains
	 * the same it is guaranteed that the mesh is unchanged.
	 *
	 * @return
	 */
	public int getMeshRevision();

	/**
	 * Turns adaptivity on if the argument is true or off it it is false.
	 *
	 * @param doAdapt
	 */
	public void setAdaptivityEnabled(boolean doAdapt);

	/**
	 * Returns true if adaptivity is turn on, false if not.
	 *
	 * @return
	 */
	public boolean getAdaptivityEnabled();

	/**
	 * Saves the current version of the mesh to a file with the specified name.
	 *
	 * @param meshFileName
	 */
	public void saveMesh(String meshFileName);

}
