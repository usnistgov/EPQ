/**
 * gov.nist.nanoscalemetrology.JMONSEL.PHAMLMeshFEARunner Created by: John
 * Villarrubia Date: Sep 6, 2012
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.NISTMonte.IMaterialScatterModel;
import gov.nist.microanalysis.NISTMonte.MeshedRegion;
import gov.nist.microanalysis.NISTMonte.MeshedRegion.DirichletConstraint;
import gov.nist.microanalysis.NISTMonte.MeshedRegion.FloatingConstraint;
import gov.nist.microanalysis.NISTMonte.MeshedRegion.NeumannConstraint;

/**
 * <p>
 * This class implements both the IAdaptiveMesh and IFEArunner interfaces,
 * performing these functions with PHAML (Parallel Hierarchical Adaptive
 * MultiLevel, http://math.nist.gov/phaml), an adaptive meshing code written by
 * William Mitchell. The constructor looks for the "PHAML_lib.dll" which should
 * therefore be available somewhere in the computer system's path.
 * </p>
 * <p>
 * With adaptivity enabled, each time a finite element analysis is performed
 * PHAML performs an adapt/solve loop until either the L2 error estimate is
 * below tolerance or the number of elements exceeds maxtet, whichever comes
 * first. Once the existing number of elements is equal to maxtet, it de-refines
 * some elements where the error estimate is small and then refines about the
 * same number of elements with large error estimates.
 * </p>
 * <p>
 * Refinement takes time, but if the mesh is more optimally chosen it will have
 * fewer elements for a given error tolerance. Smaller meshes take less time to
 * solve. Hence, there is a trade-off. A typical strategy might begin with a
 * relatively coarse mesh (or a specification for a coarse mesh in a GMSH .geo
 * file) and adaptivity enabled. At a certain point, when the mesh appears to be
 * sufficiently refined, adaptivity could be disabled if desired. Setters are
 * provided for tolerance, maxtet, and enabling or disabling mesh adaptivity.
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
public class PHAMLMeshFEARunner implements IAdaptiveMesh, IFEArunner {

   interface PHAML extends com.sun.jna.Library {

      PHAML lib = (PHAML) Native.loadLibrary("PHAML_lib.dll", PHAML.class);

      void decrementChargeNumber(int handle, int elementIndex, IntByReference errcode);

      void extendedAdjacentVolumeArray(int handle, int tetIndex, int faceIndex, int[] adjacentVolumeArray, IntByReference errcode);

      int extendedNumberAdjacentVolumeArray(int handle, int tetIndex, int faceIndex, IntByReference errcode);

      int getAdjacentVolumeIndex(int handle, int elementIndex, int faceIndex, IntByReference errcode);

      void getAdjacentVolumeTable(int handle, int[] adjacencies, IntByReference errcode);

      void getBoundaryFaces(int handle, int[] boundaryFaces, IntByReference errcode);

      int getChargeNumber(int handle, int elementIndex, IntByReference errcode);

      int getChargeNumberTable(int handle, int[] nCharges, IntByReference errcode);

      int getElementType(int handle, int elementIndex, IntByReference errcode);

      int getElementTypeTable(int handle, int[] elementType, IntByReference errcode);

      double getErrorEstimate(int handle, IntByReference errcode);

      void getFileName(int handle, byte[] chars, IntByReference errcode);

      int getMeshRevision(int handle, IntByReference errcode);

      void getNodeAdjacentVolumes(int handle, int nodeIndex, int[] nodeAdjacentVolumes, IntByReference errcode);

      void getNodeCoordinates(int handle, int nodeIndex, double[] nodeCoordinates, IntByReference errcode);

      void getNodeCoordinatesTable(int handle, double[] nodeCoordinates, IntByReference errcode);

      void getNodeIndices(int handle, int elementIndex, int[] nodes, IntByReference numNode, IntByReference errcode);

      void getNodeIndicesTable(int handle, int[] nodeIndices, int[] numNode, IntByReference errcode);

      double getNodePotential(int handle, int nodeIndex, IntByReference errcode);

      double getNodePotentialTable(int handle, double[] nodePotentials, IntByReference errcode);

      int getNumberBoundaryFaces(int handle, IntByReference errcode);

      int getNumberNodeAdjacentVolumes(int handle, int nodeIndex, IntByReference errcode);

      int getNumberOfElements(int handle, IntByReference errcode);

      int getNumberOfNodes(int handle, IntByReference errcode);

      int getNumberOfTags(int handle, int elementIndex, IntByReference errcode);

      int getNumberOfVolumeElements(int handle, IntByReference errcode);

      void getTags(int handle, int elementIndex, int[] tags, IntByReference errcode);

      void getTagsTable(int handle, int[] tags, IntByReference errcode);

      double getVolume(int handle, int elementIndex, IntByReference errcode);

      void incrementChargeNumber(int handle, int elementIndex, IntByReference errcode);

      int isVolumeType(int handle, int index, IntByReference errcode);

      void meshConstructor(IntByReference handle, String meshFileName, int len, String outputPath, int pathlen, boolean doGraphics,
            IntByReference errcode);

      void meshDestructor(int handle, IntByReference errcode);

      void meshDestructorAll(IntByReference errcode);

      void runFEA(int handle, boolean verbose, double tolerance, int max_tet, double min_tet_size, double refinetol, int solver, int errind,
            IntByReference errcode);

      void saveMesh(int handle, String meshFileName, int namelen, int meshType, IntByReference errcode);

      void setAdaptivityEnabled(int handle, boolean doAdapt, IntByReference errcode);

      void setChargeNumber(int handle, int elementIndex, int n, IntByReference errcode);

      void setChargeNumberTable(int handle, int[] chargeNumber, IntByReference errcode);

      void setChargeUnit(int handle, double chargeValue, IntByReference errcode);

      void setConstraints(int handle, int numConstraint, int[] tags, int[] types, double[] constants, int[] int_constants, IntByReference errcode);

      void setDielectricConstants(int handle, int numRegion, int[] tags, double[] constants, IntByReference errcode);

      void setNodeCoordinates(int handle, int nodeIndex, double[] coords, IntByReference errcode);

      void setNodePotential(int handle, int nodeIndex, double potential, IntByReference errcode);

      // TODO: Implement capability to use this
      void setPointCharges(int handle, int numCharge, double[] xCharge, double[] yCharge, double[] zCharge, IntByReference errcode);

      void tetFaceNodeIndices(int handle, int tetIndex, int faceIndex, int[] nodeIndices, IntByReference errcode);
   }

   static {
      try {
         System.load("C:\\my_executables\\libquadmath-0.dll");
         System.load("C:\\my_executables\\libgfortran-3.dll");
         // System.load("libquadmath-0.dll");
         // System.load("libgfortran-3.dll");
      } finally {

      }
   }

   private static String b2s(byte b[]) {
      // Converts C string to Java String
      int len = 0;
      while (b[len] != 0)
         ++len;
      return new String(b, 0, len);
   }

   private boolean adaptivityEnabled = true;
   private boolean verbose = false;
   private double chargeMultiplier;
   private int handle; // The Fortran library's handle for this mesh

   /* Following is a cache for adjacent volumes */
   private int[][] elementAdjacentVolumes;
   private double[][] nodeCoordinates;
   private int nNodes = -1;
   private int[][] nodeIndices; /* node indices for each mesh element */
   private int nElem = -1; /* number of elements */
   private long[][] tags;
   private int[] nCharges;
   private double[] nodePotentials;
   private int[] elementTypes;

   /*
    * Mesh is saved as GMSH type (1) by default, but may be changed to PHAML (2)
    * type
    */
   private int saveMeshType = 1;

   /*
    * The following constants determine the solver to use, error estimator used
    * for refinement, and stopping condition. Refinement stops when the
    * estimated L2 error (root mean square error in potential) divided by the
    * potential is less than TOLERANCE, or when the number of tetrahedra exceeds
    * MAXTET, whichever comes first. Tets smaller than min_tet_size are not
    * refined unless necessary for compatibility.
    */
   private double tolerance = 0.0001;

   private int maxtet = 1000000;

   private double min_tet_size = 0.3e-9; // 0.3 nm default minimum tet size
   private double refinetol = 1.e-15;
   private int solver = 1;
   private int errind = 3; // TODO: Change to 6

   /**
    * Constructs a PHAMLMeshFEARunner. The supplied meshFileName should conform
    * to the GMSH format. It may be either a GMSH .geo or GMSH .msh file. The
    * former is preferred because it contains boundary information that can be
    * used if mesh elements are refined along a curved boundary.
    *
    * @param feaFolder
    *           -- the name of a folder that can be used for scratch space and
    *           to write PHAML finite element analysis output (out.txt) and
    *           error messages (err.txt).
    * @param meshFileName
    *           -- an initial GMSH .geo (preferred) or .msh file.
    */
   public PHAMLMeshFEARunner(String feaFolder, String meshFileName) {
      final int namelen = meshFileName.length();
      final int feaFolderLen = feaFolder.length();
      final IntByReference errcode = new IntByReference(0);
      final IntByReference handleRef = new IntByReference(0);
      /* Create the temporary folder. */
      (new File(feaFolder)).mkdirs();
      final boolean doGraphics = false; // Change to false after debug
      PHAML.lib.meshConstructor(handleRef, meshFileName, namelen, feaFolder, feaFolderLen, doGraphics, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            handle = handleRef.getValue();
            break;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Allocation failed in PHAMLMeshFEARunner call to meshConstructor.");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("File " + meshFileName + " does not exist.");
         case 3 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Could not open file in " + feaFolder);
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in PHAMLMeshFEARunner");
      }
      setChargeMultiplier(1.);
      initializeAllCache();
   }

   /**
    * @param index
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#decrementChargeNumber(int)
    */
   @Override
   public void decrementChargeNumber(int index) {
      nCharges[index] -= 1;
   }

   /**
    * @param index
    */
   public void decrementChargeNumberPHAMLdirect(int index) {
      final IntByReference errcode = new IntByReference(0);
      PHAML.lib.decrementChargeNumber(handle, index, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Element index out of range in decrementChargeNumber.");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Element index is not a volume element in decrementChargeNumber.");
         case 3 :
            throw new EPQFatalException("Invalid handle in decrementChargeNumber.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in decrementChargeNumber");
      }
   }

   /**
    * @param tetIndex
    * @param faceIndex
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#extendedAdjacentVolumeArray(int,
    *      int)
    */
   // TODO Test this routine
   @Override
   public int[] extendedAdjacentVolumeArray(int tetIndex, int faceIndex) {
      final IntByReference errcode = new IntByReference(0);
      final int n = PHAML.lib.extendedNumberAdjacentVolumeArray(handle, tetIndex, faceIndex, errcode);
      int err = errcode.getValue();
      if (err < 0) {
         PHAML.lib.meshDestructorAll(errcode);
         throw new EPQFatalException("PHAML errorcode = " + err + " in extendedNumberAdjacentVolumeArray.");
      } else
         switch (err) {
            case 0 :
               break; // No error, so continue
            case 1 :
               PHAML.lib.meshDestructorAll(errcode);
               throw new EPQFatalException("tetIndex out of range in extendedNumberAdjacentVolumeArray.");
            case 2 :
               PHAML.lib.meshDestructorAll(errcode);
               throw new EPQFatalException("faceIndex is out of range in extendedNumberAdjacentVolumeArray.");
            case 3 :
               PHAML.lib.meshDestructorAll(errcode);
               throw new EPQFatalException("tetIndex is not a tetrahedron element in extendedNumberAdjacentVolumeArray.");
            case 4 :
               PHAML.lib.meshDestructorAll(errcode);
               throw new EPQFatalException("Memory allocation failed in extendedNumberAdjacentVolumeArray.");
            case 5 :
               throw new EPQFatalException("Invalid handle in extendedNumberAdjacentVolumeArray.");
            default :
               PHAML.lib.meshDestructorAll(errcode);
               throw new EPQFatalException("Unknown nonzero error code in extendedNumberAdjacentVolumeArray");
         }
      final int adjV[] = new int[n];
      PHAML.lib.extendedAdjacentVolumeArray(handle, tetIndex, faceIndex, adjV, errcode);
      err = errcode.getValue();
      switch (err) {
         case 0 :
            return adjV;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("tetIndex out of range in extendedAdjacentVolumeArray");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Face index is out of range in extendedAdjacentVolumeArray");
         case 3 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException(
                  "tetIndex does not agree with last call to extendedNumberAdjacentVolumeArray in extendedAdjacentVolumeArray.");
         case 4 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException(
                  "faceIndex does not agree with last call to extendedNumberAdjacentVolumeArray in extendedAdjacentVolumeArray.");
         case 5 :
            throw new EPQFatalException("Invalid handle in extendedAdjacentVolumeArray.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in extendedAdjacentVolumeArray");
      }
   }

   @Override
   protected void finalize() {
      final IntByReference errcode = new IntByReference(0);
      PHAML.lib.meshDestructorAll(errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return;
         case 1 :
            throw new EPQFatalException("Invalid handle in finalize.");
         default :
            throw new EPQFatalException("Unknown nonzero error code in finalize.");
      }
   }

   @Override
   public boolean getAdaptivityEnabled() {
      return adaptivityEnabled;
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
      return elementAdjacentVolumes[elementIndex][faceIndex];
   }

   /**
    * @param elementIndex
    * @param faceIndex
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getAdjacentVolumeIndex(int,
    *      int)
    */
   // Kept initially to test my new faster version. Once I know they're the
   // same, I can delete this one.
   public int getAdjacentVolumeIndexPHAMLdirect(int elementIndex, int faceIndex) {
      final IntByReference errcode = new IntByReference(0);
      final int adjacentVolumeIndex = PHAML.lib.getAdjacentVolumeIndex(handle, elementIndex, faceIndex, errcode);
      final int err = errcode.getValue();
      if (err < 0) {
         PHAML.lib.meshDestructorAll(errcode);
         throw new EPQFatalException("PHAML errorcode = " + err + " in getAdjacentVolumeIndex.");
      } else
         switch (err) {
            case 0 :
               return adjacentVolumeIndex;
            case 1 :
               PHAML.lib.meshDestructorAll(errcode);
               throw new EPQFatalException("Element index (" + elementIndex + ") out of range in getAdjacentVolumeIndex.");
            case 2 :
               PHAML.lib.meshDestructorAll(errcode);
               throw new EPQFatalException("Element " + elementIndex + " is not a volume element in getAdjacentVolumeIndex.");
            case 3 :
               throw new EPQFatalException("Invalid handle in getAdjacentVolumeIndex.");
            default :
               PHAML.lib.meshDestructorAll(errcode);
               throw new EPQFatalException("Unknown nonzero error code in getAdjacentVolumeIndex");
         }
   }

   /**
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getBoundaryFaces()
    */
   // TODO Test this routine
   @Override
   public int[][] getBoundaryFaces() {
      final IntByReference errcode = new IntByReference(0);
      final int nbf = PHAML.lib.getNumberBoundaryFaces(handle, errcode);
      int err = errcode.getValue();

      switch (err) {
         case 0 :
            break;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Memory allocation failed in getNumberBoundaryFaces in getBoundaryFaces");
         case 2 :
            throw new EPQFatalException("Invalid handle in getNumberBoundaryFaces in getBoundaryFaces.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getNumberBoundaryFaces in getBoundaryFaces");
      }
      /* Make a 1-d array for Fortran routine */
      final int[] bF = new int[nbf * 2];
      PHAML.lib.getBoundaryFaces(handle, bF, errcode);
      err = errcode.getValue();
      switch (err) {
         case 0 :
            /* Repack to 2-d as required by IBasicMesh */
            final int[][] bF2D = new int[nbf][2];
            for (int bf2Dindex = 0, bFindex = 0; bf2Dindex < nbf; bf2Dindex++, bFindex += 2) {
               bF2D[bf2Dindex][0] = bF[bFindex];
               bF2D[bf2Dindex][1] = bF[bFindex + 1];
            }
            return bF2D;
         case 1 :
            throw new EPQFatalException("Invalid handle in getBoundaryFaces().");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getBoundaryFaces.");
      }
   }

   /**
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IFEArunner#getChargeMultiplier()
    */
   @Override
   public double getChargeMultiplier() {
      return chargeMultiplier;
   }

   /**
    * Returns the total charge in the indexed element in units of e. E.g., the !
    * electron charge is -1; Only volume elements carry charge. Others return 0.
    *
    * @param index
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getChargeNumber(int)
    */
   @Override
   public int getChargeNumber(int index) {
      return nCharges[index];
   }

   /**
    * Returns the number of charges in the indexed mesh element from PHAML's
    * table.
    *
    * @param index
    * @return
    */
   public int getChargeNumberPHAMLdirect(int index) {
      final IntByReference errcode = new IntByReference(0);
      final int nc = PHAML.lib.getChargeNumber(handle, index, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return nc;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("elementIndex (" + index + ") out of range in getChargeNumber.");
         case 2 :
            throw new EPQFatalException("Invalid handle in getChargeNumber.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getElementType");
      }
   }

   /**
    * @param elementIndex
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getElementType(int)
    */
   // TODO Test this routine
   @Override
   public int getElementType(int elementIndex) {
      return elementTypes[elementIndex];
   }

   /**
    * Returns the type of the indexed mesh element from PHAML rather than from
    * JMONSEL's local cache.
    *
    * @param elementIndex
    * @return
    */
   public int getElementTypePHAMLdirect(int elementIndex) {
      final IntByReference errcode = new IntByReference(0);
      final int type = PHAML.lib.getElementType(handle, elementIndex, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return type;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Element index out of range in getElementType");
         case 2 :
            throw new EPQFatalException("Invalid handle in getElementType.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getElementType");
      }
   }

   /**
    * @return The error estimate
    */
   public double getErrorEstimate() {
      final IntByReference errcode = new IntByReference(0);
      final double errEst = PHAML.lib.getErrorEstimate(handle, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return errEst;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("runFEA has not been called with handle " + handle + " in getErrorEstimate.");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in getErrorEstimate.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getVolume");
      }
   }

   /**
    * Returns the current value of errind.
    * 
    * @return
    */
   public int getErrind() {
      return errind;
   }

   /**
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getFileName()
    */
   // TODO Test this routine
   @Override
   public String getFileName() {
      final byte chars[] = new byte[512];
      final IntByReference errcode = new IntByReference(0);
      PHAML.lib.getFileName(handle, chars, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            final String name = b2s(chars);
            return name;
         case 1 :
            throw new EPQFatalException("Invalid handle in getFileName().");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getFileName().");
      }
   }

   /**
    * A mesh revision number of 0 is assigned to the original mesh. IBasicMesh
    * implementations that alter the mesh (e.g., classes that use adaptive
    * meshes) increment the revision number each time the mesh is changed. This
    * getMeshRevision() method returns the revision number, so the user can
    * determine whether the mesh has changed.
    * 
    */
   // TODO Test this routine
   @Override
   public int getMeshRevision() {
      final IntByReference errcode = new IntByReference(0);
      final int rev = PHAML.lib.getMeshRevision(handle, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return rev;
         case 1 :
            throw new EPQFatalException("Invalid handle in getMeshRevision().");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getMeshRevision().");
      }
   }

   /**
    * Returns the current value of min_tet_size.
    *
    * @return
    */
   public double getMin_tet_size() {
      return min_tet_size;
   }

   /**
    * @param nodeIndex
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNodeAdjacentVolumes(int)
    */
   // TODO Test this routine
   @Override
   public int[] getNodeAdjacentVolumes(int nodeIndex) {
      final IntByReference errcode = new IntByReference(0);
      final int n = PHAML.lib.getNumberNodeAdjacentVolumes(handle, nodeIndex, errcode);
      int err = errcode.getValue();
      if (err < 0) {
         PHAML.lib.meshDestructorAll(errcode);
         throw new EPQFatalException("PHAML error code = " + err + " in getNumberNodeAdjacentVolumes in getNodeAdjacentVolumes.");
      } else
         switch (err) {
            case 0 :
               break;
            case 1 :
               PHAML.lib.meshDestructorAll(errcode);
               throw new EPQFatalException("Node index out of range in getNumberNodeAdjacentVolumes in getNodeAdjacentVolumes.");
            case 2 :
               throw new EPQFatalException("Invalid handle in getNumberNodeAdjacentVolumes in getNodeAdjacentVolumes.");
            default :
               PHAML.lib.meshDestructorAll(errcode);
               throw new EPQFatalException("Unknown nonzero error code in getNumberNodeAdjacentVolumes in getNodeAdjacentVolumes.");
         }
      final int[] nodeAV = new int[n];
      PHAML.lib.getNodeAdjacentVolumes(handle, nodeIndex, nodeAV, errcode);
      err = errcode.getValue();
      switch (err) {
         case 0 :
            return nodeAV;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Node index out of range in getNodeAdjacentVolumes");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Node index does not agree with last call to getNumberNodeAdjacentVolumes in getNodeAdjacentVolumes");
         case 3 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Allocation failed: adjacencies are returned but not sorted in getNodeAdjacentVolumes");
         case 4 :
            throw new EPQFatalException("Invalid handle in getNodeAdjacentVolumes.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getNodeAdjacentVolumes");
      }
   }

   /**
    * @param nodeIndex
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNodeCoordinates(int)
    */
   @Override
   public double[] getNodeCoordinates(int nodeIndex) {
      return nodeCoordinates[nodeIndex].clone();
   }

   /**
    * @param nodeIndex
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNodeCoordinates(int)
    */
   // Kept initially to test my new faster version. Once I know they're the
   // same, I can delete this one.
   public double[] getNodeCoordinatesPHAMLdirect(int nodeIndex) {
      final IntByReference errcode = new IntByReference(0);
      final double[] nodeCoordinates = new double[3];
      PHAML.lib.getNodeCoordinates(handle, nodeIndex, nodeCoordinates, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return nodeCoordinates;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Node index out of range in getNodeCoordinates");
         case 2 :
            throw new EPQFatalException("Invalid handle in getNodeCoordinates");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getNodeIndices");
      }
   }

   /**
    * @param elementIndex
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNodeIndices(int)
    */
   @Override
   public int[] getNodeIndices(int elementIndex) {
      return nodeIndices[elementIndex].clone();
   }

   /**
    * @param elementIndex
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNodeIndices(int)
    */
   // Kept initially to test my new faster version. Once I know they're the
   // same, I can delete this one.
   public int[] getNodeIndicesPHAMLdirect(int elementIndex) {
      final IntByReference errcode = new IntByReference(0);
      final IntByReference numNode = new IntByReference(0);
      final int maxIndices[] = new int[20];
      PHAML.lib.getNodeIndices(handle, elementIndex, maxIndices, numNode, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            final int numNodeVal = numNode.getValue();
            final int indices[] = new int[numNodeVal];
            for (int i = 0; i < numNodeVal; i++)
               indices[i] = maxIndices[i];
            return indices;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Element index out of range in getNodeIndices");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Element index is a point element in getNodeIndices");
         case 3 :
            throw new EPQFatalException("Invalid handle in getNodeIndices");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getNodeIndices");
      }
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
    * @param nodeIndex
    * @return
    */
   public double getNodePotentialPHAMLdirect(int nodeIndex) {
      final IntByReference errcode = new IntByReference(0);
      final double v = PHAML.lib.getNodePotential(handle, nodeIndex, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return v;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Node index out of range in getNodePotential");
         case 2 :
            throw new EPQFatalException("Invalid handle in getNodePotential");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getNodeIndices");
      }
   }

   /**
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNumberOfElements()
    */
   // TODO Test this routine
   @Override
   public int getNumberOfElements() {
      if (nElem > 0)
         return nElem; // Return cached value
      else
         return nElem = getNumberOfElementsPHAMLdirect(); // No cached value, so
                                                          // ask PHAML
   }

   /**
    * Gets the number of elements in the mesh directly from PHAML, as opposed to
    * using the locally cached copy, as does getNumberOfElements().
    *
    * @return
    */
   public int getNumberOfElementsPHAMLdirect() {
      final IntByReference errcode = new IntByReference(0);
      final int numElem = PHAML.lib.getNumberOfElements(handle, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return numElem;
         case 1 :
            throw new EPQFatalException("Invalid handle in getNumberOfElements.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getNumberOfElements");
      }
   }

   /**
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNumberOfNodes()
    */
   @Override
   public int getNumberOfNodes() {
      if (nNodes > 0)
         return nNodes; // Return cached value
      else
         return nNodes = getNumberOfNodesPHAMLdirect(); // No cached value, so
                                                        // ask PHAML
   }

   /**
    * Gets the number of nodes in the mesh directly from PHAML, as opposed to
    * using the locally cached copy, as does getNumberOfNodes().
    *
    * @return
    */
   public int getNumberOfNodesPHAMLdirect() {
      final IntByReference errcode = new IntByReference(0);
      final int nn = PHAML.lib.getNumberOfNodes(handle, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return nn;
         case 1 :
            throw new EPQFatalException("Invalid handle in getNumberOfNodes.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getNumberOfNodes");
      }
   }

   /**
    * @param elementIndex
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNumberOfTags(int)
    */
   @Override
   public int getNumberOfTags(int elementIndex) {
      final IntByReference errcode = new IntByReference(0);
      final int nTags = PHAML.lib.getNumberOfTags(handle, elementIndex, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return nTags;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in getNumberOfTags.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getNumberOfTags");
      }
   }

   /**
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getNumberOfVolumeElements()
    */
   // TODO Test this routine
   @Override
   public int getNumberOfVolumeElements() {
      final IntByReference errcode = new IntByReference(0);
      final int nve = PHAML.lib.getNumberOfVolumeElements(handle, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return nve;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in getNumberOfVolumeElements.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getNumberOfVolumeElements");
      }
   }

   /**
    * Returns the current value of refinetol.
    * 
    * @return
    */
   public double getRefinetol() {
      return refinetol;
   }

   /**
    * Returns the value of saveMeshType. A value of 1 means mesh files are
    * stored in GMSH format. A value of 2 means they are stored in PHAML format.
    * 
    * @return
    */
   public int getSaveMeshType() {
      return saveMeshType;
   }

   /**
    * Returns the index of the current solver (set using setSolver).
    * 
    * @return
    */
   public int getSolver() {
      return solver;
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
    * @param elementIndex
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getTags(int)
    */
   // Kept initially to test my new faster version. Once I know they're the
   // same, I can delete this one.
   public long[] getTagsPHAMLdirect(int elementIndex) {
      final IntByReference errcode = new IntByReference(0);
      final int ntags = getNumberOfTags(elementIndex);
      final int[] tagsInt = new int[ntags];
      final long[] tags = new long[ntags];
      PHAML.lib.getTags(handle, elementIndex, tagsInt, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            for (int i = 0; i < ntags; i++)
               tags[i] = tagsInt[i];
            @SuppressWarnings("unused")
            final int dummy = 0;
            return tags;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Element index out of range in getTags");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in getTags");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getTags");
      }
   }

   /**
    * @param index
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#getVolume(int)
    */
   @Override
   public double getVolume(int index) {
      final IntByReference errcode = new IntByReference(0);
      final double v = PHAML.lib.getVolume(handle, index, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return v;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Element index (" + index + ") out of range in getVolume.");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in getVolume.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in getVolume");
      }
   }

   /**
    * @param index
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#incrementChargeNumber(int)
    */
   @Override
   public void incrementChargeNumber(int index) {
      nCharges[index] += 1;
   }

   public void incrementChargeNumberPHAMLdirect(int index) {
      final IntByReference errcode = new IntByReference(0);
      PHAML.lib.incrementChargeNumber(handle, index, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Element index (" + index + ") out of range in incrementChargeNumber.");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Element " + index + " is not a volume element in incrementChargeNumber.");
         case 3 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in incrementChargeNumber.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in incrementChargeNumber");
      }
   }

   /**
    * initializeAdjacentVolumesTable calls PHAML's getAdjacentVolumeTable to
    * construct a cache of which elements are adjacent to which through each
    * face. Caching on the Java side of interface speeds execution considerably.
    * This routine needs to be called in the constructor to initialize the cache
    * and again to renew it each time the mesh is refined.
    */
   private void initializeAdjacentVolumesTable() {
      final IntByReference errcode = new IntByReference(0);
      final int nElem = getNumberOfElementsPHAMLdirect();
      /*
       * Tetrahedra have 4 faces, and we presently have no elements with more
       * than this, so PHAML returns a N x 4 array.
       */
      final int maxFaces = 4;

      /* Make a 1-d array for Fortran routine */
      final int[] adj = new int[(nElem + 1) * maxFaces];
      PHAML.lib.getAdjacentVolumeTable(handle, adj, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            /* Repack to 2-d */
            elementAdjacentVolumes = new int[nElem + 1][maxFaces];
            for (int adjIndex = 0, elemIndex = 0; elemIndex <= nElem; elemIndex++, adjIndex += maxFaces)
               for (int i = 0; i < maxFaces; i++)
                  elementAdjacentVolumes[elemIndex][i] = adj[adjIndex + i];
            break;
         case 1 :
            throw new EPQFatalException("Invalid handle in initializeAdjacentVolumesTable().");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in initializeAdjacentVolumesTable.");
      }
   }

   private void initializeAllCache() {
      /*
       * TODO: In cases where PHAML generates its own mesh from a .geo file the
       * mesh elements (and maybe other things) are not getting properly copied
       * to the JMONSEL side of the interface.
       */
      initializeAdjacentVolumesTable(); // re-cache adjacent volumes
      initializeNodeCoordinatesTable(); // re-cache node coordinates
      initializeNodeIndicesTable(); // re-cache node indices
      initializeTagsTable(); // re-cache tags
      initializeChargeNumberTable(); // re-cache nCharges
      initializeNodePotentialTable(); // re-cache nodePotentials
      initializeElementTypeTable(); // re-cache elementTypes
   }

   /**
    * initializeChargeNumberTable calls PHAML's getChargeNumberTable to
    * construct a cache of charges associated with the elements. Caching on the
    * Java side of interface is meant to speed execution. This routine needs to
    * be called in the constructor to initialize the cache and again to renew it
    * each time the mesh is refined.
    */
   private void initializeChargeNumberTable() {
      final IntByReference errcode = new IntByReference(0);
      nElem = getNumberOfElementsPHAMLdirect();

      /* Make a 1-d array for Fortran routine */
      nCharges = new int[nElem + 1];

      PHAML.lib.getChargeNumberTable(handle, nCharges, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            break;
         case 1 :
            throw new EPQFatalException("Invalid handle in initializeChargeNumberTable().");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in initializeChargeNumberTable.");
      }
   }

   /**
    * initializeElementTypeTable calls PHAML's getElementTypeTable to construct
    * a cache of types associated with the elements. Caching on the Java side of
    * interface is meant to speed execution. This routine needs to be called in
    * the constructor to initialize the cache and again to renew it each time
    * the mesh is refined.
    */
   private void initializeElementTypeTable() {
      final IntByReference errcode = new IntByReference(0);
      nElem = getNumberOfElementsPHAMLdirect();

      /* Make a 1-d array for Fortran routine */
      elementTypes = new int[nElem + 1];

      PHAML.lib.getElementTypeTable(handle, elementTypes, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            break;
         case 1 :
            throw new EPQFatalException("Invalid handle in initializeElementTypeTable().");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in initializeElementTypeTable.");
      }
   }

   /**
    * initializeNodeCoordinatesTable calls PHAML's getNodeCoordinatesTable to
    * construct a cache of coordinates associated with the nodes. Caching on the
    * Java side of interface is meant to speed execution. This routine needs to
    * be called in the constructor to initialize the cache and again to renew it
    * each time the mesh is refined.
    */
   private void initializeNodeCoordinatesTable() {
      final IntByReference errcode = new IntByReference(0);
      nNodes = getNumberOfNodesPHAMLdirect();
      /*
       * The array of coordinates will be (N+1) x 3.
       */

      /* Make a 1-d array for Fortran routine */
      final double[] coords = new double[(nNodes + 1) * 3];
      PHAML.lib.getNodeCoordinatesTable(handle, coords, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            /* Repack to 2-d */
            nodeCoordinates = new double[nNodes + 1][3];
            for (int n = 0, cIndex = 0; n <= nNodes; n++, cIndex += 3)
               for (int i = 0; i < 3; i++)
                  nodeCoordinates[n][i] = coords[cIndex + i];
            break;
         case 1 :
            throw new EPQFatalException("Invalid handle in initializeNodeCoordinatesTable().");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in initializeNodeCoordinatesTable.");
      }
   }

   /**
    * initializeNodeIndicesTable calls PHAML's getNodeIndicesTable to construct
    * a cache of node indices associated with the elements. It also caches the
    * number of nodes associated with each element. Caching on the Java side of
    * interface is meant to speed execution. This routine needs to be called in
    * the constructor to initialize the cache and again to renew it each time
    * the mesh is refined.
    */
   private void initializeNodeIndicesTable() {
      final IntByReference errcode = new IntByReference(0);
      nElem = getNumberOfElementsPHAMLdirect();
      /*
       * Tetrahedra have 4 nodes, and we presently have no elements with more
       * than this, so PHAML returns indices in an N x 4 array.
       */
      final int maxNodes = 4;

      /* Make a 1-d array for Fortran routine */
      final int[] indices = new int[(nElem + 1) * maxNodes];
      final int[] numNodes = new int[nElem + 1];

      PHAML.lib.getNodeIndicesTable(handle, indices, numNodes, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            /* Repack to 2-d */
            nodeIndices = new int[nElem + 1][];
            for (int indicesIndex = 0, elemIndex = 0; elemIndex <= nElem; elemIndex++, indicesIndex += maxNodes) {
               final int len = numNodes[elemIndex];
               nodeIndices[elemIndex] = new int[len];
               for (int i = 0; i < len; i++)
                  nodeIndices[elemIndex][i] = indices[indicesIndex + i];
            }
            break;
         case 1 :
            throw new EPQFatalException("Invalid handle in initializeNodeIndicesTable().");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in initializeNodeIndicesTable.");
      }
   }

   /**
    * initializeNodePotentialTable calls PHAML's getNodePotentialTable to
    * construct a cache of potentials associated with the nodes. Caching on the
    * Java side of interface is meant to speed execution. This routine needs to
    * be called in the constructor to initialize the cache and again to renew it
    * each time the mesh is refined.
    */
   private void initializeNodePotentialTable() {
      final IntByReference errcode = new IntByReference(0);
      nNodes = getNumberOfNodesPHAMLdirect();

      /* Make a 1-d array for Fortran routine */
      nodePotentials = new double[nNodes + 1];

      PHAML.lib.getNodePotentialTable(handle, nodePotentials, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            break;
         case 1 :
            throw new EPQFatalException("Invalid handle in initializeNodePotentialTable().");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in initializeNodePotentialTable.");
      }
   }

   /**
    * initializeTagsTable calls PHAML's getTagsTable to construct a cache of
    * node indices associated with the elements. It also caches the number of
    * nodes associated with each element. Caching on the Java side of interface
    * is meant to speed execution. This routine needs to be called in the
    * constructor to initialize the cache and again to renew it each time the
    * mesh is refined.
    */
   private void initializeTagsTable() {
      final IntByReference errcode = new IntByReference(0);
      nElem = getNumberOfElementsPHAMLdirect();
      final int nTags = getNumberOfTags(1);

      /* Make a 1-d array for Fortran routine */
      final int[] t = new int[(nElem + 1) * nTags];

      PHAML.lib.getTagsTable(handle, t, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            /* Repack to 2-d */
            tags = new long[nElem + 1][nTags];
            for (int tIndex = 0, elemIndex = 0; elemIndex <= nElem; elemIndex++, tIndex += nTags)
               for (int i = 0; i < nTags; i++)
                  tags[elemIndex][i] = t[tIndex + i];
            break;
         case 1 :
            throw new EPQFatalException("Invalid handle in initializeNodeIndicesTable().");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in initializeNodeIndicesTable.");
      }
   }

   /**
    * Returns the current value of verbose (which determines the detail of what
    * PHAML writes to out.txt).
    *
    * @return
    */
   public boolean isVerbose() {
      return verbose;
   }

   /**
    * Returns true if the element is a volume type (e.g., a tetrahedron) and
    * false if not (lines, triangles, etc.) The determination is made by relying
    * on information in JMONSEL's cache of volume types.
    *
    * @param elementIndex
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#isVolumeType(int)
    */
   // TODO Test this routine
   @Override
   public boolean isVolumeType(int elementIndex) {
      if (getElementType(elementIndex) == 4)
         return true;
      return false;
   }

   /**
    * Asks PHAML whether the indexed element is a volume type (e.g., a
    * tetrahedron) or a nonvolume type (lines, triangles, etc.)
    *
    * @param elementIndex
    * @return
    */
   public boolean isVolumeTypePHAMLdirect(int elementIndex) {
      final IntByReference errcode = new IntByReference(0);
      final int isVt = PHAML.lib.isVolumeType(handle, elementIndex, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            if (isVt != 0)
               return true;
            else
               return false;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Element index (" + elementIndex + ") out of range in isVolumeType.");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in isVolumeType.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in isVolumeType");
      }
   }

   /**
    * @param meshReg
    * @see gov.nist.nanoscalemetrology.JMONSEL.IFEArunner#runFEA(gov.nist.microanalysis.NISTMonte.MeshedRegion)
    */
   // TODO Test this routine
   @Override
   public void runFEA(MeshedRegion meshReg) {
      // Set dielectric constants
      final Set<Map.Entry<Long, IMaterialScatterModel>> msmSet = meshReg.getMSMMap().entrySet();
      final int numRegion = msmSet.size();
      final int[] tags = new int[numRegion];
      final double[] epsvals = new double[numRegion];
      final Iterator<Map.Entry<Long, IMaterialScatterModel>> setIt = msmSet.iterator();
      for (int i = 0; i < numRegion; i++) {
         final Map.Entry<Long, IMaterialScatterModel> msmSetEntry = setIt.next();
         tags[i] = msmSetEntry.getKey().intValue();
         final Material mat = msmSetEntry.getValue().getMaterial();
         if (mat instanceof SEmaterial)
            epsvals[i] = PhysicalConstants.PermittivityOfFreeSpace * ((SEmaterial) mat).getEpsr();
         else
            throw new EPQFatalException("Illegal non-SEmaterial in the mesh: " + mat);
      }
      final IntByReference errcode = new IntByReference(0);
      PHAML.lib.setDielectricConstants(handle, numRegion, tags, epsvals, errcode);
      int err = errcode.getValue();
      switch (err) {
         case 0 :
            break;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Allocation failed in setDielectricConstants.");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in setDielectricConstants.");
         case 3 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Duplicate tag in setDielectricConstants.");
         case 4 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("A volume element has a tag not in tags in setDielectricConstants.");
         case 5 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("numRegion is not positive in setDielectricConstants.");
         case 6 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Inconsistent constants for duplicate elements in setDielectricConstants.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in setDielectricConstants");
      }

      // Set constraints
      final MeshedRegion.IConstraint[] constraints = meshReg.getConstraintList();
      final int numConstraint = constraints.length;
      final int[] ctags = new int[numConstraint];
      final int[] types = new int[numConstraint];
      final double[] constants = new double[numConstraint];
      final int[] int_constants = new int[numConstraint];
      for (int i = 0; i < numConstraint; i++) {
         final MeshedRegion.IConstraint thisConstraint = constraints[i];
         if (thisConstraint instanceof DirichletConstraint) {
            types[i] = 1;
            ctags[i] = (int) thisConstraint.getAssociatedRegionTag();
            constants[i] = ((DirichletConstraint) thisConstraint).getPotential();
         } else if (thisConstraint instanceof NeumannConstraint) {
            types[i] = 2;
            ctags[i] = (int) thisConstraint.getAssociatedRegionTag();
            constants[i] = ((NeumannConstraint) thisConstraint).getNormalE();
         } else if (thisConstraint instanceof FloatingConstraint) {
            types[i] = 3;
            ctags[i] = (int) thisConstraint.getAssociatedRegionTag();
            int_constants[i] = (int) ((FloatingConstraint) thisConstraint).getVolumeTag();
            constants[i] = ((FloatingConstraint) thisConstraint).getCharge();
         } else
            throw new EPQFatalException("Constraint of unknown type in runFEA.");
      }
      errcode.setValue(0);
      PHAML.lib.setConstraints(handle, numConstraint, ctags, types, constants, int_constants, errcode);
      err = errcode.getValue();
      switch (err) {
         case 0 :
            break;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Allocation failed in setConstraints.");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in setConstraints.");
         case 3 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Duplicate tag in setConstraints.");
         case 4 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Inconsistent constraints for duplicate elements in setConstraints.");
         case 5 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("numConstraint is not positive in setConstraints.");
         case 6 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid type in setConstraints.");
         case 7 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("The constraint type for some region has changed in setConstraints.");
         case 8 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("The tag for the interior of a floating region has changed in setConstraints.");
         case 9 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("There is a tag that was not present in a previous call in setConstraints.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in setConstraints");
      }

      // Make sure PHAML has all its charges up to date
      setChargeNumberTable();

      errcode.setValue(0);
      final int rev0 = getMeshRevision();
      PHAML.lib.runFEA(handle, verbose, tolerance, maxtet, min_tet_size, refinetol, solver, errind, errcode);
      err = errcode.getValue();
      switch (err) {
         case 0 :
            break;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Allocation failed in set_index_tables within runFEA.");
         case 2 :
            throw new EPQFatalException("Invalid handle in runFEA.");
         case 3 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid input value for solver within runFEA.");
         case 4 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid input value for errind within runFEA.");
         case 5 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Dielectric constants have not been set in runFEA.");
         case 6 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Constraints have not been set in runFEA.");
         case 7 :
         case 8 :
            /*
             * These are informational return codes, indicating that a tolerance
             * was not met. Code 7 means termination was due to reaching max_tet
             * volume elements. Code 8 means termination was due to an apparent
             * stalled refinement. I have nothing good to do with this message,
             * so I currently just ignore it.
             */
            break;
         case 9 :
            /*
             * TODO: This code means "could not refresh output file". I'm not
             * sure what this means so I don't know whether to ignore it or
             * throw an exception.
             */
            break;
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in setConstraints");
      }
      if (rev0 != getMeshRevision())
         initializeAllCache(); // Re-cache the table if Mesh was changed
      else
         initializeNodePotentialTable(); // Re-cache the potentials to include
                                         // new solution, even if the mesh
                                         // wasn't changed.
      meshReg.getMesh().clearElementsCache(); // Force reinstantiation of any
                                              // needed mesh elements (to redo
                                              // the potentials & fields)
   }

   /**
    * Saves the mesh to a file with the specified name. The type of the file is
    * determined by the saveMeshType() method. It is GMSH by default.
    *
    * @param meshFileName
    */
   @Override
   public void saveMesh(String meshFileName) {
      final IntByReference errcode = new IntByReference(0);
      final int len = meshFileName.length();
      final int meshType = 1; // GMSH format
      PHAML.lib.saveMesh(handle, meshFileName, len, meshType, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Could not open " + meshFileName + ".");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in saveMesh.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in saveMesh");
      }
   }

   /**
    * If doAdapt is true, then subsequent calls to runFEA adapt the mesh and
    * solve for the potential. If doAdapt is false, then subsequent calls to
    * runFEA only solve for the potential. The default is true.
    *
    * @param doAdapt
    */
   @Override
   public void setAdaptivityEnabled(boolean doAdapt) {
      final IntByReference errcode = new IntByReference(0);
      PHAML.lib.setAdaptivityEnabled(handle, doAdapt, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            adaptivityEnabled = doAdapt;
            return;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in setAdaptivityEnabled.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in setAdaptivityEnabled");
      }
   }

   /**
    * @param chargeMultiplier
    * @see gov.nist.nanoscalemetrology.JMONSEL.IFEArunner#setChargeMultiplier(double)
    */
   @Override
   public void setChargeMultiplier(double chargeMultiplier) {
      this.chargeMultiplier = chargeMultiplier;
      final double chargeUnit = chargeMultiplier * PhysicalConstants.ElectronCharge;
      final IntByReference errcode = new IntByReference(0);
      PHAML.lib.setChargeUnit(handle, chargeUnit, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in setChargeUnit.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in setChargeUnit");
      }
   }

   /**
    * @param index
    * @param n
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#setChargeNumber(int,
    *      int)
    */
   @Override
   public void setChargeNumber(int index, int n) {
      nCharges[index] = n;
   }

   /**
    * Sets the charge in element[index] = n directly in PHAML and updates the
    * local cache too.
    *
    * @param index
    * @param n
    */
   public void setChargeNumberPHAMLdirect(int index, int n) {
      final IntByReference errcode = new IntByReference(0);
      PHAML.lib.setChargeNumber(handle, index, n, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            setChargeNumber(index, n); // Also update the local cache.
            return;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Element index (" + index + ") out of range in setChargeNumber.");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Element " + index + " is not a volume element in setChargeNumber.");
         case 3 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in setChargeNumber.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in setChargeNumber.");
      }
   }

   /**
    * Updates PHAML's chargeNumberTable (the list of number of charges in each
    * element) to agree with the table maintained by JMONSEL. During scattering,
    * JMONSEL keeps track of charges locally. Before doing a new finite element
    * analysis, PHAML's table needs to be updated.
    */
   public void setChargeNumberTable() {
      final IntByReference errcode = new IntByReference(0);
      PHAML.lib.setChargeNumberTable(handle, nCharges, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in setChargeNumberTable.");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in setChargeNumberTable");
      }
   }

   /**
    * <p>
    * Sets the error indicator to use as follows:
    * </p>
    * <p>
    * 1 local problem p, energy norm
    * </p>
    * <p>
    * 2 local problem p, H^1 seminorm
    * </p>
    * <p>
    * 3 local problem p, L^2 norm
    * </p>
    * <p>
    * 4 John's with local problem p, H^1 seminorm
    * </p>
    * <p>
    * 5 John's with local problem p, L^1 norm
    * </p>
    * <p>
    * 6 John's with local problem p, L^1-like seminorm without abs (default)
    * </p>
    * 
    * @param errind
    */
   public void setErrind(int errind) {
      this.errind = errind;
   }

   /**
    * The maximum number of tetrahedra to allow in the mesh. During refinement,
    * the number of tetrahedra in the mesh may increase up to approximately this
    * limit if needed to meet the error tolerance. Once maxtet tetrahedra are in
    * the mesh, then low error tetrahedra are de-refined and an approximately
    * equal number of high error tets are refined. Refinement and de-refinement
    * occur with groups of tetrahedra, so this limit is approximate. Default is
    * 1 million.
    *
    * @param maxtet
    */
   public void setMaxTet(int maxtet) {
      if (maxtet > 0)
         this.maxtet = maxtet;
      else
         throw new EPQFatalException("Illegal maxtet");
   }

   /**
    * Sets the value of min_tet_size. If the cube root of the volume of a tet is
    * smaller than this value (default 0.3e-9 m), the tet will not be refined
    * unless it is necessary for compatibility (e.g., with an adjacent element
    * that is refined).
    *
    * @param min_tet_size
    *           in meters.
    */
   public void setMin_tet_size(double min_tet_size) {
      if (min_tet_size > 0)
         this.min_tet_size = min_tet_size;
      else
         this.min_tet_size = -min_tet_size;
   }

   /**
    * @param nodeIndex
    * @param coords
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#setNodeCoordinates(int,
    *      double[])
    */
   // TODO Test this routine
   @Override
   public void setNodeCoordinates(int nodeIndex, double[] coords) {
      /* Make matching changes in the local cache and PHAML's copy. */
      nodeCoordinates[nodeIndex] = coords.clone();
      final IntByReference errcode = new IntByReference(0);
      PHAML.lib.setNodeCoordinates(handle, nodeIndex, coords, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Node index out of range in setNodeCoordinates");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Invalid handle in setNodeCoordinates");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in setNodeCoordinates");
      }
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
    * Updates the potential of the node directly in PHAML and also updates the
    * local cache to match.
    *
    * @param nodeIndex
    * @param potVal
    */
   public void setNodePotentialPHAMLdirect(int nodeIndex, double potVal) {
      final IntByReference errcode = new IntByReference(0);
      PHAML.lib.setNodePotential(handle, nodeIndex, potVal, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            setNodePotential(nodeIndex, potVal); // Also update local cache
            return;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Node index out of range in setNodePotential");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in setNodePotential");
      }
   }

   /**
    * When a mesh element's error estimate is less than refinetol, refinement is
    * not performed on that element (unless required for compatability). The
    * numberical meaning of refinetol depends on the chosen error estimator.
    * 
    * @param refinetol
    */
   public void setRefinetol(double refinetol) {
      this.refinetol = refinetol;
   }

   /**
    * If saveMeshType = 1 saveMesh saves to a GMSH format (default). If
    * saveMeshType = 2 it saves in PHAML format. The PHAML mesh file is larger,
    * but it retains information about refinement history that allows PHAML to
    * unrefine previously refined tets. This may be important if an adaptive
    * calculation is saved and later resumed
    * 
    * @param saveMeshType
    *           - int
    */
   public void setSaveMeshType(int saveMeshType) {
      this.saveMeshType = saveMeshType;
   }

   /**
    * <p>
    * The integer argument determines the solver/preconditioner combination used
    * by PHAML, as follows:
    * </p>
    * <p>
    * 1 GMRES/ILU (default)
    * </p>
    * <p>
    * 2 GMRES/SOR
    * </p>
    * <p>
    * 3 GMRES/AMG
    * </p>
    * <p>
    * 4 CG/ILU
    * </p>
    * <p>
    * 5 CG/SOR
    * </p>
    * <p>
    * 6 CG/AMG
    * </p>
    * <p>
    * where GMRES = Generalized minimum residual method, CG = Conjugate gradient
    * method, ILU = Incomplete LU factorization, SOR = Successive
    * over-relaxation, and AMG = Algebraic multigrid.
    * 
    * @param solver
    */
   public void setSolver(int solver) {
      this.solver = solver;
   }

   /**
    * Sets the tolerance used by PHAML to determine which elements should be
    * adapted. Default is 0.0001. An error of this size would mean the estimated
    * rms fractional error (error/value) in the potential is 0.01%.
    *
    * @param tolerance
    */
   public void setTolerance(double tolerance) {
      if ((tolerance > 0) && (tolerance < 1))
         this.tolerance = tolerance;
      else
         throw new EPQFatalException("Illegal tolerance");
   }

   /**
    * PHAML generates a temporary out.txt file in the tempFEA folder each time
    * runFEA is invoked. Verbose or not verbose (the default) output to out.txt
    * is set by calling setVerbose with the argument respectively true or false.
    *
    * @param verbose
    */
   public void setVerbose(boolean verbose) {
      if (this.verbose != verbose)
         this.verbose = verbose;
   }

   /**
    * @param tetIndex
    * @param faceIndex
    * @return
    * @see gov.nist.nanoscalemetrology.JMONSEL.IBasicMesh#tetFaceNodeIndices(int,
    *      int)
    */
   // TODO Test this routine
   @Override
   public int[] tetFaceNodeIndices(int tetIndex, int faceIndex) {
      switch (faceIndex) {
         case 0 :
            return new int[]{nodeIndices[tetIndex][1], nodeIndices[tetIndex][2], nodeIndices[tetIndex][3]};
         case 1 :
            return new int[]{nodeIndices[tetIndex][0], nodeIndices[tetIndex][3], nodeIndices[tetIndex][2]};
         case 2 :
            return new int[]{nodeIndices[tetIndex][0], nodeIndices[tetIndex][1], nodeIndices[tetIndex][3]};
         case 3 :
            return new int[]{nodeIndices[tetIndex][0], nodeIndices[tetIndex][2], nodeIndices[tetIndex][1]};
         default :
            throw new EPQFatalException("tetFaceNodeIndices: called with illegal value of face index");
      }
   }

   /**
    * @param tetIndex
    * @param faceIndex
    * @return
    */
   public int[] tetFaceNodeIndicesPHAMLdirect(int tetIndex, int faceIndex) {
      final IntByReference errcode = new IntByReference(0);
      final int nodeIndices[] = new int[3];
      PHAML.lib.tetFaceNodeIndices(handle, tetIndex, faceIndex, nodeIndices, errcode);
      final int err = errcode.getValue();
      switch (err) {
         case 0 :
            return nodeIndices;
         case 1 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("tetIndex index out of range in tetFaceNodeIndices");
         case 2 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("faceIndex is out of range in tetFaceNodeIndices");
         case 3 :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("tetIndex is not a volume element in tetFaceNodeIndices");
         case 4 :
            throw new EPQFatalException("Invalid handle in tetFaceNodeIndices");
         default :
            PHAML.lib.meshDestructorAll(errcode);
            throw new EPQFatalException("Unknown nonzero error code in tetFaceNodeIndices");
      }

   }
}
