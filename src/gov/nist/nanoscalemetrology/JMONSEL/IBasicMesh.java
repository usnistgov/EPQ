/**
 * gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh Created by: John Villarrubia
 * Date: Aug 15, 2012
 */
package gov.nist.nanoscalemetrology.JMONSEL;

/**
 * <p>
 * The raw data of a mesh consist of a list of nodes (x,y,z coordinates), each
 * with an associated electrostatic potential and a list of mesh elements: lines
 * (joining two nodes), triangles (3 ordered nodes), and tetrahedra (4 ordered
 * nodes). The order of nodes in a triangle is such that the nodes are clockwise
 * when viewed from inside, counterclockwise from outside. For a tetrahedron,
 * any node may be given first. The remaining 3 are then given in clockwise
 * order as viewed from the first. Each element has an element type: 1 for line,
 * 2 for triangle, 4 for tetrahedron. It also has a list of integer tags and a
 * list of node indices. An IBasicMesh provides methods to access these data.
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
public interface IBasicMesh {

	/**
	 * Decrements the charge number by 1. This corresponds, e.g., to a gain of one
	 * electron in this volume element.
	 *
	 * @param index - The index of the element for which charge is to be
	 *              decremented.
	 */
	public void decrementChargeNumber(int index);

	/**
	 * Returns a sorted array of indices of volumes adjacent to Tetrahedron
	 * #tetIndex through the indexed face. "Extended" means volumes that share only
	 * an edge or a node of this face are returned in addition to the volume that
	 * shares the entire face. The array is sorted from most shared nodes to fewest
	 * shared nodes. I.e., the tet that shares the whole face (3 nodes) is listed
	 * first if there is one. Then tets that share an edge (2 nodes) are listed.
	 * Finally tets that share only a point (a single node) are listed.
	 */
	public int[] extendedAdjacentVolumeArray(int tetIndex, int faceIndex);

	/**
	 * Returns the index of the volume element adjacent to the indexed one through
	 * the indexed face. If there is no adjacent volume (i.e., the indexed face is
	 * on the mesh boundary) 0 is returned.
	 *
	 * @param elementIndex - index of the volume element for which the neighboring
	 *                     element is to be returned.
	 * @param faceIndex    - index of the face shared with the neighbor
	 * @return
	 */
	public int getAdjacentVolumeIndex(int elementIndex, int faceIndex);

	/**
	 * Returns an N x 2 array that specifies the faces on the boundary of the mesh.
	 * N is the number of faces on the boundary. Each face is specified by two
	 * integers. The first is the index of the volume element to which the face
	 * belongs and the second is the local index of the face within that volume
	 * element. E.g., result[i][0] = n and result[i][1] = k means the kth face of
	 * volume element #n is one of the boundary faces.
	 *
	 * @return
	 */
	public int[][] getBoundaryFaces();

	/**
	 * Returns the total charge in the indexed element in units of e. E.g., the
	 * electron charge is -1; Only volume elements carry charge. Others return 0.
	 *
	 * @return - the charge number
	 */
	public int getChargeNumber(int index);

	/**
	 * Returns the type of mesh element at the given index. The main relevant types
	 * are currently 1-line, 2-triangle, 4-tetrahedron. See gmsh documentation for
	 * other types. Note that indexing of elements starts from 1 rather than 0, to
	 * follow Gmsh convention. Therefore elements are indexed from 1 to
	 * getNumberOfElements().
	 *
	 * @param elementIndex
	 * @return
	 */
	public int getElementType(int elementIndex);

	/**
	 * Gets the name of the file that was read to produce this mesh. (This may be a
	 * stored mesh file or a geometry file from which the mesh was computed.)
	 *
	 * @return
	 */
	public String getFileName();

	/**
	 * Returns an array of type int, each of which is the index of a volume element
	 * that shares the supplied nodeIndex. These are sorted in order of increasing
	 * index.
	 *
	 * @param nodeIndex
	 * @return
	 */
	public int[] getNodeAdjacentVolumes(int nodeIndex);

	/**
	 * Gets the coordinates of the node with the given index. Note that indexing of
	 * nodes starts from 1 rather than 0, to follow Gmsh convention. Therefore nodes
	 * are indexed from 1 to getNumberOfNodes().
	 *
	 * @param nodeIndex
	 * @return
	 */
	public double[] getNodeCoordinates(int nodeIndex);

	/**
	 * Returns the array of node indices associated with the element at the given
	 * index. Note that indexing of elements starts from 1 rather than 0, to follow
	 * Gmsh convention. Therefore elements are indexed from 1 to
	 * getNumberOfElements(). The order that nodes are listed in the returned array
	 * is significant. For a tetrahedron, any node may be given first. The remaining
	 * 3 are then given in clockwise order as viewed from the first.
	 *
	 * @param elementIndex
	 * @return
	 */
	public int[] getNodeIndices(int elementIndex);

	/**
	 * Gets the potential of the node with the given index. Note that indexing of
	 * nodes starts from 1 rather than 0, to follow Gmsh convention. Therefore nodes
	 * are indexed from 1 to getNumberOfNodes().
	 *
	 * @param nodeIndex
	 * @return
	 */
	public double getNodePotential(int nodeIndex);

	/**
	 * Gets the number of elements in this mesh. An element is any object (e.g.,
	 * edge, face, or volume) larger than a node that is defined within the mesh.
	 *
	 * @return int number of elements in mesh
	 */
	public int getNumberOfElements();

	/**
	 * Gets the number of nodes in this mesh.
	 *
	 * @return int number of nodes in mesh
	 */
	public int getNumberOfNodes();

	/**
	 * Returns the number of tags associated with the mesh element at the given
	 * index. Note that indexing of elements starts from 1 rather than 0, to follow
	 * Gmsh convention. Therefore elements are indexed from 1 to
	 * getNumberOfElements().
	 *
	 * @param elementIndex
	 * @return
	 */
	public int getNumberOfTags(int elementIndex);

	/**
	 * Gets the number of volume elements in this mesh.
	 *
	 * @return
	 */
	public int getNumberOfVolumeElements();

	/**
	 * Returns an array of element tags associated with the element at the given
	 * index. Note that indexing of elements starts from 1 rather than 0, to follow
	 * Gmsh convention. Therefore elements are indexed from 1 to
	 * getNumberOfElements().
	 *
	 * @param elementIndex
	 * @return
	 */
	public long[] getTags(int elementIndex);

	/**
	 * Returns the (positive) volume of the element at the provided index or 0 if
	 * the index refers to a non-volume element.
	 *
	 * @param index
	 * @return
	 */
	public double getVolume(int index);

	/**
	 * Increments the charge number by 1. This corresponds, e.g., to a loss of one
	 * electron from this volume element.
	 *
	 * @param index - The index of the element for which charge is to be
	 *              incremented.
	 */
	public void incrementChargeNumber(int index);

	/**
	 * Returns true if the index element is a volume type element (e.g.,
	 * tetrahedron), false if not (e.g., a point, line, or surface element)
	 *
	 * @param elementIndex
	 * @return
	 */
	public boolean isVolumeType(int elementIndex);

	/**
	 * Sets the charge number to the supplied value.
	 *
	 * @param index - The index of the element for which charge is to be set.
	 * @param n     - The value to which to set the charge, in units of e.
	 */
	public void setChargeNumber(int index, int n);

	/**
	 * Changes the coordinates of the indexed node to the supplied new coordinates.
	 * Note that by moving coordinates it is possible to make nonsense out of a
	 * mesh, for example by having positions that are contained in more than one
	 * volume element of the altered mesh. It is the responsibility of the calling
	 * routine to insure that the cumulative effect of coordinate changes makes
	 * sense before finite element analysis (or other operations that require a
	 * valid mesh) are attempted. Common transformations that meet this requirement
	 * are rotations, translations, or affine transforms of the whole mesh.
	 *
	 * @param index  - index of the node to be moved
	 * @param coords - new coordinates of the node
	 */
	public void setNodeCoordinates(int index, double[] coords);

	/**
	 * Sets the potential of the node with the given index. Note that indexing of
	 * nodes starts from 1 rather than 0, to follow Gmsh convention. Therefore nodes
	 * are indexed from 1 to getNumberOfNodes().
	 *
	 * @param nodeIndex - the index of the node
	 * @param potVal    - the new value of its potential (in volts)
	 */
	public void setNodePotential(int nodeIndex, double potVal);

	/**
	 * Returns an array of 3 node indices corresponding the tetrahedron face with
	 * the given index. The indices are given in clockwise order as viewed from
	 * inside the tetrahedron. (I.e., (n2-n1) x (n3-n1) points out of the
	 * tetrahedron.)
	 *
	 * @param tetIndex  - the index of a tetrahedron
	 * @param faceIndex - index (0-3) of one of the 4 faces of the tretrahedron
	 * @return - an array of indices of the 3 nodes on the referenced face
	 */
	public int[] tetFaceNodeIndices(int tetIndex, int faceIndex);

}
