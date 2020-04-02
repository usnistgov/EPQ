/**
 * gov.nist.nanoscalemetrology.JMONSEL.GmshMesh Created by: jvillar Date: Aug
 * 20, 2012
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Scanner;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.nanoscalemetrology.JMONSELutils.FrequencyCounter;

/**
 * <p>
 * A class used to import, store, and access a mesh produced by Gmsh. Gmsh
 * provides a mechanism for creating .geo files to describe a geometry. Gmsh
 * meshes a 3-D geo file by breaking the volume into tetrahedral mesh elements.
 * Surfaces may also be specified as assemblies of triangles. Tetrahedra and
 * triangles are defined by their nodes. These objects: nodes, triangles, and
 * tetrahedra, are exported by Gmsh to a .msh file, which is read to produce an
 * instance of this Mesh class.
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
public class GmshMesh implements IBasicMesh {

	private final String meshFileName;
	private double[][] nodeCoords;
	private double[] nodePotentials;
	private int[][] nodeAdjacentVolumes;
	private int[] elementTypes;
	private int[] ntags;
	private long[][] tags;
	private int[][] elementsNodeIndices;
	private boolean elementAdjacentVolumesInitialized = false;
	private int[][] elementAdjacentVolumes;
	private int[] nElectronicCharges;
	private double[] volume;
	private int nVolumeElements = 0;

	/**
	 * Constructs a Mesh
	 *
	 * @param meshFileName
	 * @throws FileNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public GmshMesh(String meshFileName) throws FileNotFoundException {
		this.meshFileName = meshFileName;
		final Scanner s = new Scanner(new BufferedReader(new FileReader(meshFileName)));
		s.useLocale(Locale.US);
		try {
			/*
			 * I've chosen to make this import very strict. Any deviation from expectations
			 * generates a fatal error. TODO It may be too strict, since there is supposed
			 * to be an ability to insert comments by having unrecognized sections, e.g.,
			 * $Comments...$EndComments, but this reader will throw an error if you try.
			 */

			/* First section is MeshFormat */
			final String str = s.next();
			if (!str.equals("$MeshFormat"))
				throw new EPQFatalException("1st File token was not $MeshFormat");
			final double versionNumber = s.nextDouble();
			if ((versionNumber != 2.1) && (versionNumber != 2.2))
				throw new EPQFatalException("Expecting mesh format = 2.1 or 2.2"); // File
			// format
			if (s.nextInt() != 0)
				throw new EPQFatalException("Expecting file type = 0");
			if (s.nextInt() != 8)
				throw new EPQFatalException("Expecting data size = 8");
			if (!s.next().equals("$EndMeshFormat"))
				throw new EPQFatalException("Expecting end of $MeshFormat");

			/* Next section is Nodes */
			if (!s.next().equals("$Nodes"))
				throw new EPQFatalException("Expecting beginning of $Nodes");
			int numNodes;
			if (s.hasNextInt())
				numNodes = s.nextInt();
			else
				throw new EPQFatalException("Expecting number of Nodes");

			/* Initialize arrays associated with nodes. */
			nodeCoords = new double[numNodes + 1][3];
			nodePotentials = new double[numNodes + 1];
			nodeAdjacentVolumes = new int[numNodes + 1][];
			/* A temporary collection to park items while we accumulate them */
			final LinkedList<Integer>[] nodeAVlists = new LinkedList[numNodes + 1];

			for (int i = 1; i <= numNodes; i++) {
				if (s.nextInt() != i)
					throw new EPQFatalException("Incorrect node number at node " + Integer.toString(i));
				nodeCoords[i][0] = s.nextDouble();
				nodeCoords[i][1] = s.nextDouble();
				nodeCoords[i][2] = s.nextDouble();
				nodeAVlists[i] = new LinkedList<Integer>();
			}
			if (!s.next().equals("$EndNodes"))
				throw new EPQFatalException("Expecting end of $Nodes");

			/* Next section is Elements */
			if (!s.next().equals("$Elements"))
				throw new EPQFatalException("Expecting beginning of $Elements");
			int numElements;
			if (s.hasNextInt())
				numElements = s.nextInt();
			else
				throw new EPQFatalException("Expecting number of Elements");

			/* Initialize arrays associated with elements */
			elementTypes = new int[numElements + 1];
			ntags = new int[numElements + 1];
			tags = new long[numElements + 1][];
			elementsNodeIndices = new int[numElements + 1][];
			elementAdjacentVolumes = new int[numElements + 1][];
			nElectronicCharges = new int[numElements + 1];
			volume = new double[numElements + 1];
			for (int i = 1; i <= numElements; i++) {
				if (s.nextInt() != i)
					throw new EPQFatalException("Incorrect element number at element " + Integer.toString(i));
				final int type = s.nextInt();
				elementTypes[i] = type;
				final int nt = s.nextInt();
				/*
				 * Format 2.1 has a last unused tag that we'll use later. 2.2 does not, so we
				 * add a tag to our internal format for that one.
				 */
				if (versionNumber == 2.1)
					ntags[i] = nt;
				else
					ntags[i] = nt + 1;
				tags[i] = new long[ntags[i]];
				for (int j = 0; j < nt; j++)
					tags[i][j] = s.nextLong(); // ?
				switch (type) {
				case 1: // 2-node line
					elementsNodeIndices[i] = new int[] { s.nextInt(), s.nextInt() };
					break;
				case 2: // 3-node triangle
					elementsNodeIndices[i] = new int[] { s.nextInt(), s.nextInt(), s.nextInt() };
					break;
				case 4: // 4-node tetrahedron
					elementsNodeIndices[i] = new int[4];
					for (int j = 0; j < 4; j++) {
						elementsNodeIndices[i][j] = s.nextInt();
						nodeAVlists[elementsNodeIndices[i][j]].add(i);
					}
					elementAdjacentVolumes[i] = new int[4];
					nVolumeElements++;
					break;
				default:
					if ((type >= 1) && (type <= 31))
						throw new EPQFatalException(
								"Element " + Integer.toString(i) + " is unimplemented type " + Integer.toString(type));
					else
						throw new EPQFatalException("Encountered invalid element type = " + Integer.toString(type)
								+ " at element # " + Integer.toString(i));
				}

			}
			if (!s.next().equals("$EndElements"))
				throw new EPQFatalException("Expecting end of $Elements");

			/*
			 * Move the lists of node adjacent volumes to arrays to save space, and sort
			 * them.
			 */
			for (int i = 1; i <= numNodes; i++) {
				nodeAdjacentVolumes[i] = new int[nodeAVlists[i].size()];
				int j = 0;
				for (final Integer vol : nodeAVlists[i])
					nodeAdjacentVolumes[i][j++] = vol;
				Arrays.sort(nodeAdjacentVolumes[i]);
			}

			/* Initialize volume array */
			for (int tetIndex = 1; tetIndex <= numElements; tetIndex++)
				if (elementTypes[tetIndex] == 4)
					volume[tetIndex] = volumeFromNodes(elementsNodeIndices[tetIndex]);
		} finally {
			if (s != null)
				s.close();
		}
	}

	/**
	 * @param index
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#decrementChargeNumber(int)
	 */
	@Override
	public void decrementChargeNumber(int index) {
		nElectronicCharges[index]--;
	}

	@Override
	public int[] extendedAdjacentVolumeArray(int tetIndex, int faceIndex) {
		final ArrayList<FrequencyCounter> volList = extendedAdjacentVolumeList(tetIndex, faceIndex);
		final int s = volList.size();
		final int[] result = new int[s];
		for (int i = 0; i < s; i++)
			result[i] = (Integer) volList.get(i).id();
		return result;
	}

	/**
	 * Returns an extended list of volumes adjacent to Tetrahedron #tetIndex through
	 * the indexed face. Extended means volumes that share only an edge or a node of
	 * this face are returned in addition to the volume that shares the entire face.
	 */
	private ArrayList<FrequencyCounter> extendedAdjacentVolumeList(int tetIndex, int faceIndex) {

		/*
		 * The 3 nodes of this face have 3 corresponding lists of adjacent volumes. Get
		 * arrays for these and take the first volume off of each list.
		 */
		/* 3 indices of the 3 nodes of this face */
		final int[] facenodeindices = tetFaceNodeIndices(tetIndex, faceIndex);

		final int[][] nodeAV = new int[][] { nodeAdjacentVolumes[facenodeindices[0]],
				nodeAdjacentVolumes[facenodeindices[1]], nodeAdjacentVolumes[facenodeindices[2]] };

		final int[] currentValue = new int[3];
		final boolean[] validValue = { false, false, false };
		int numValidValues = 0;
		final int[] nextIndex = { 0, 0, 0 };

		for (int i = 0; i < 3; i++) {
			int index = nextIndex[i];
			while ((index < nodeAV[i].length) && (nodeAV[i][index] == tetIndex))
				index++;
			if (index < nodeAV[i].length) {
				currentValue[i] = nodeAV[i][index];
				validValue[i] = true;
				numValidValues++;
				nextIndex[i] = index + 1;
			}
		}

		/* Make a list to which the volumes can be added in order */
		final ArrayList<FrequencyCounter> volList = new ArrayList<FrequencyCounter>();

		/* Scan through the lists, adding volumes in order to volList */
		int prevSmallestValue = -1; // Initialize to an impossible value
		while (numValidValues > 0) {
			// Find the smallest value and its index
			int smallestIndex = -1;
			int smallestValue = Integer.MAX_VALUE;
			for (int i = 0; i < 3; i++)
				if (validValue[i] && (currentValue[i] < smallestValue)) {
					smallestValue = currentValue[i];
					smallestIndex = i;
				}
			// Add this smallest value to volList
			if (smallestValue == prevSmallestValue)
				// if it's already there, increment the count
				volList.get(volList.size() - 1).increment();
			else {
				// Otherwise, add it
				volList.add(new FrequencyCounter(smallestValue, 1));
				prevSmallestValue = smallestValue; // and update
			}
			// Take the next value from the list we just used if it has one.
			int index = nextIndex[smallestIndex];
			while ((index < nodeAV[smallestIndex].length) && (nodeAV[smallestIndex][index] == tetIndex))
				index++;
			if (index < nodeAV[smallestIndex].length) {
				currentValue[smallestIndex] = nodeAV[smallestIndex][index];
				nextIndex[smallestIndex] = index + 1;
			} else {
				/*
				 * If no valid value, mark this list finished and decrement the number of active
				 * lists.
				 */
				validValue[smallestIndex] = false;
				numValidValues--;
			}
		}

		/*
		 * Sort our list. The natural order for FrequencyCounter objects is to put high
		 * frequency objects first. This puts the most likely adjacent elements at the
		 * head of our list.
		 */
		Collections.sort(volList);
		return volList;
	}

	/**
	 * @param elementIndex
	 * @param faceIndex
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getAdjacentVolumeIndex(int,
	 *      int)
	 */
	@Override
	public int getAdjacentVolumeIndex(int elementIndex, int faceIndex) {
		// getBoundaryFaces() initializes the adjacent volumes list
		if (!elementAdjacentVolumesInitialized)
			getBoundaryFaces();
		return elementAdjacentVolumes[elementIndex][faceIndex];
	}

	/**
	 * @return
	 */
	/* The method also initializes the elementAdjacentVolumes array */
	@Override
	public int[][] getBoundaryFaces() {
		/*
		 * Initialize tables of adjacent tets. Keep a list of faces that have no
		 * adjacent tet. These are boundary faces.
		 */
		final ArrayList<int[]> bF = new ArrayList<int[]>();
		for (int tetIndex = 1; tetIndex < elementTypes.length; tetIndex++)
			if (elementTypes[tetIndex] == 4)
				for (int faceIndex = 0; faceIndex < 4; faceIndex++)
					if ((!initializeAdjacentVolume(tetIndex, faceIndex)))
						bF.add(new int[] { tetIndex, faceIndex });
		elementAdjacentVolumesInitialized = true;
		final int[][] boundaryFaces = new int[bF.size()][];
		for (int i = 0; i < bF.size(); i++)
			boundaryFaces[i] = bF.get(i);
		return boundaryFaces;
	}

	/**
	 * @param index
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getChargeNumber(int)
	 */
	@Override
	public int getChargeNumber(int index) {
		return nElectronicCharges[index];
	}

	/**
	 * @param elementIndex
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getElementType(int)
	 */
	@Override
	public int getElementType(int elementIndex) {
		return elementTypes[elementIndex];
	}

	/**
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getFileName()
	 */
	@Override
	public String getFileName() {
		return meshFileName;
	}

	/**
	 * @param nodeIndex
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNodeAdjacentVolumes(int)
	 */
	@Override
	public int[] getNodeAdjacentVolumes(int nodeIndex) {
		return nodeAdjacentVolumes[nodeIndex];
	}

	/**
	 * @param nodeIndex
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNodeCoordinates(int)
	 */
	@Override
	public double[] getNodeCoordinates(int nodeIndex) {
		return nodeCoords[nodeIndex].clone();
	}

	/**
	 * @param elementIndex
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNodeIndices(int)
	 */
	@Override
	public int[] getNodeIndices(int elementIndex) {
		return elementsNodeIndices[elementIndex].clone();
	}

	/**
	 * @param nodeIndex
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNodePotential(int)
	 */
	@Override
	public double getNodePotential(int nodeIndex) {
		return nodePotentials[nodeIndex];
	}

	/**
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNumberOfElements()
	 */
	@Override
	public int getNumberOfElements() {
		return elementTypes.length - 1;
	}

	/**
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNumberOfNodes()
	 */
	@Override
	public int getNumberOfNodes() {
		return nodeCoords.length - 1;
	}

	/**
	 * @param elementIndex
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNumberOfTags(int)
	 */
	@Override
	public int getNumberOfTags(int elementIndex) {
		return ntags[elementIndex];
	}

	/**
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNumberOfVolumeElements()
	 */
	@Override
	public int getNumberOfVolumeElements() {
		return nVolumeElements;
	}

	/**
	 * @param elementIndex
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getTags(int)
	 */
	@Override
	public long[] getTags(int elementIndex) {
		return tags[elementIndex].clone();
	}

	/**
	 * @param index
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getVolume(int)
	 */
	@Override
	public double getVolume(int index) {
		return volume[index];
	}

	/**
	 * @param index
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#incrementChargeNumber(int)
	 */
	@Override
	public void incrementChargeNumber(int index) {
		nElectronicCharges[index]++;
	}

	/**
	 * Initializes elementAdjacentVolumes[tetIndex][faceIndex]. tetIndex and
	 * faceIndex are input parameters that identify a particular triangular
	 * face--the faceIndex face of the tetIndex tetrahedron. Besides the tetIndex
	 * tetrahedron, there is at most one other tetrahedron in the mesh that shares
	 * this face. The index of that tetrahedron is found, if it exists, stored in
	 * the indicated array location, and true is returned. If the identified face
	 * does not have a neighboring tetrahedron (i.e., because it is on the boundary
	 * of the mesh), false is returned.
	 *
	 * @param tetIndex  - Index of a tetrahedron
	 * @param faceIndex - Index (0-3) of one of the faces of this tetrahedron
	 * @return - true if the adjacent volume exists, false if it doesn't (i.e., if
	 *         this face is on the boundary of the mesh).
	 */
	private boolean initializeAdjacentVolume(int tetIndex, int faceIndex) {
		final ArrayList<FrequencyCounter> volList = extendedAdjacentVolumeList(tetIndex, faceIndex);
		if (volList.get(0).count() == 3) {
			elementAdjacentVolumes[tetIndex][faceIndex] = (Integer) volList.get(0).id();
			return true;
		}
		return false;
	}

	/**
	 * @param elementIndex
	 * @return
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#isVolumeType(int)
	 */
	@Override
	public boolean isVolumeType(int elementIndex) {
		final int type = getElementType(elementIndex);
		if (((type >= 4) && (type <= 7)) || ((type >= 11) && (type <= 14)) || ((type >= 17) && (type <= 19))
				|| ((type >= 29) && (type <= 31)))
			return true;
		else
			return false;
	}

	/**
	 * @param index
	 * @param n
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#setChargeNumber(int, int)
	 */
	@Override
	public void setChargeNumber(int index, int n) {
		nElectronicCharges[index] = n;
	}

	/**
	 * @param index
	 * @param coords
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#setNodeCoordinates(int,
	 *      double[])
	 */
	@Override
	public void setNodeCoordinates(int index, double[] coords) {
		nodeCoords[index] = coords.clone();
	}

	/**
	 * @param nodeIndex
	 * @param potVal
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#setNodePotential(int,
	 *      double)
	 */
	@Override
	public void setNodePotential(int nodeIndex, double potVal) {
		nodePotentials[nodeIndex] = potVal;
	}

	/**
	 * Returns an array of 3 node indices corresponding the the face with the given
	 * index. The indices are indexed in the order such that (n2-n1) x (n3-n1)
	 * points out of the tetrahedron.
	 *
	 * @param faceIndex
	 * @return
	 */
	@Override
	public int[] tetFaceNodeIndices(int tetIndex, int faceIndex) {
		switch (faceIndex) {
		case 0:
			return new int[] { elementsNodeIndices[tetIndex][1], elementsNodeIndices[tetIndex][2],
					elementsNodeIndices[tetIndex][3] };
		case 1:
			return new int[] { elementsNodeIndices[tetIndex][0], elementsNodeIndices[tetIndex][3],
					elementsNodeIndices[tetIndex][2] };
		case 2:
			return new int[] { elementsNodeIndices[tetIndex][0], elementsNodeIndices[tetIndex][1],
					elementsNodeIndices[tetIndex][3] };
		case 3:
			return new int[] { elementsNodeIndices[tetIndex][0], elementsNodeIndices[tetIndex][2],
					elementsNodeIndices[tetIndex][1] };
		default:
			throw new EPQFatalException("faceNodeIndices: called with illegal value of face index");
		}
	}

	/**
	 * A utility to compute the tetrahedron volume from the positions of its nodes.
	 * It uses the formula, V = a . (b x c)/6, where a, b, and c are vectors
	 * representing the 3 edges emerging from any vertex.
	 *
	 * @param nodeIndices
	 * @return
	 */

	private double volumeFromNodes(int[] nodeIndices) {
		/* Compute 3 edges referenced to node[0] */
		final double[] a = Math2.minus(nodeCoords[nodeIndices[1]], nodeCoords[nodeIndices[0]]);
		final double[] b = Math2.minus(nodeCoords[nodeIndices[2]], nodeCoords[nodeIndices[0]]);
		final double[] c = Math2.minus(nodeCoords[nodeIndices[3]], nodeCoords[nodeIndices[0]]);
		/* Compute the volume */
		return Math2.dot(a, Math2.cross(b, c)) / 6.;
	}

}
