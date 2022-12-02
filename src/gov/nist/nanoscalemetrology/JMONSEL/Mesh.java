/**
 * gov.nist.nanoscalemetrology.JMONSEL.Mesh
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.ITransform;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.Transform3D;

import Jama.Matrix;

/**
 * <p>
 * A class used to import, store, and access a mesh. Presently, only tetrahedral
 * meshes are supported. Two versions of the constructor are supplied. The more
 * general one takes an IBasicMesh object as input and uses it to construct the
 * Mesh. For example, the GmshMesh class implements IBasicMesh for meshes
 * created by the open source Gmsh program. Other IBasicMesh implementations may
 * be written for other meshing routines. The second constructor takes a string
 * which it uses to generate a GmshMesh by reading the Gmsh mesh file. The
 * IBasicMesh object at the core of Mesh provides most of the functionality
 * related to geometry and connectivity--the things a mesh needs to do for a
 * finte element analysis program. This Mesh class adds the functionality needed
 * for performing simulations on meshes.
 * </p>
 * <p>
 * The raw data of a mesh consist of a list of nodes (x,y,z coordinates), each
 * with an associated electrostatic potential and a list of mesh elements: lines
 * (joining two nodes), triangles (3 ordered nodes), and tetrahedra (4 ordered
 * nodes). The order of nodes in a triangle is such that the nodes are clockwise
 * when viewed from inside, counterclockwise from outside. For a tetrahedron,
 * any node may be given first. The remaining 3 are then given in clockwise
 * order as viewed from the first. Each element has an element type: 1 for line,
 * 2 for triangle, 4 for tetrahedron. It also has a list of integer tags and a
 * list of node indices. Mesh provides methods to access these data.
 * </p>
 * <p>
 * Characteristic of a mesh is that faces of volume elements are shared between
 * at most two volume elements. Thus, each tetrahedron in the mesh has
 * well-defined neighbors. This characteristic makes possible efficient
 * determination of which element of the mesh contains a given point or which
 * element is the next one when a trajectory intersects the boundary of a given
 * element. These capabilities are made accessible via implementation of
 * nextTet() and containingShape() methods specified by a "ConnectedShape"
 * interface.
 * </p>
 * <p>
 * Mesh provides a number of subclasses--Element(abstract), Line, Triangle, and
 * Tetrahedron--that are used to instantiate objects that represent components
 * of the mesh. It also provides a MeshShape class that represents the overall
 * shape of the meshed region and a getMeshShape() method that returns the
 * MeshShape for this mesh. The MeshShape is the shape that corresponds to the
 * boundary of the mesh. This shape is mainly useful when the mesh is viewed
 * from the outside, e.g., when it is necessary to decide whether a trajectory
 * that begins outside the mesh intersects the mesh somewhere or whether a point
 * is contained somewhere within the mesh. Viewed from the inside, a trajectory
 * always moves from one tetrahedral element to another, so the mesh looks like
 * a series of connected tetrahedra.
 * </p>
 * <p>
 * Mesh elements (lines, triangles, tetrahedra) may not be rotated or translated
 * individually. The mesh as a whole can, however, either by using its rotate()
 * and translate() methods or by using its MeshShape's rotate() and translate().
 * (The latter simply call the former.) These methods transform all of the nodes
 * in the mesh. Mesh elements frequently internally cache geometrical
 * information for better performance. Any such cache must be updated after the
 * mesh is transformed. All Tetrahedrons and any nonTetrahedrons that Mesh has
 * created for its own use (e.g., the list of triangular facets that define the
 * MeshShape's boundary) are known to Mesh and are automatically updated as part
 * of the rotation or transformation. If you have created any other
 * nonTetrahedrons for your use, Mesh does not know about them and it does not
 * update them when the mesh is rotated or translated. You must do this
 * yourself, using the updateGeom() method provided for each of them.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain.
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
public class Mesh implements ITransform {

   /**
    * <p>
    * An interface for shapes associated with a tetrahedral mesh. Such meshes
    * typically have efficient methods of determining which elements are
    * adjacent to each other.
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    *
    * @author John Villarrubia
    * @version 1.0
    */
   public interface ConnectedShape extends NormalShape {
      /**
       * Returns the Tetrahedron that contained pos0 when contains(pos0) or
       * contains(pos0,pos1) last returned true. This can be used, for example,
       * after a call to MeshShape.contains(pos0,pos1) returns true, to
       * ascertain which Tetrahedron within the MeshShape contains pos0.
       *
       * @return - the ConnectedShape that contained pos0 when contains(pos0) or
       *         contains(pos0,pos1) last returned true
       */
      Tetrahedron containingShape();

      /**
       * Returns the mesh of which this shape is a member.
       *
       * @return - the Mesh
       */
      Mesh getMesh();

      /**
       * Returns the Tetrahedron on the other side of the boundary at the last
       * intersection, or null if the trajectory was leaving the mesh. This can
       * be used, for example, to determine the next shape that an electron
       * enters when it leaves the current one.
       *
       * @return - the next Tetrahedron
       */
      Tetrahedron nextTet();

      /**
       * Forces update of cached values in this ConnectedShape. This is used if
       * the underlying mesh is transformed.
       */
      void updateGeom();
   }
   /**
    * <p>
    * An abstract class from which mesh elements (lines, triangles, tetrahedra,
    * etc. are derived.
    * </p>
    */
   static abstract public class Element {

      protected int myIndex;
      protected Mesh mesh;

      /**
       * Gets the index of this element.
       *
       * @return Returns the type.
       */
      public int getIndex() {
         return myIndex;
      }

      /**
       * Gets the current value assigned to nodeIndices
       *
       * @return Returns the nodeIndices.
       */
      public int[] getNodeIndices() {
         return mesh.getNodeIndices(myIndex);
      }

      /**
       * Gets the current value assigned to numberOfTags
       *
       * @return Returns the numberOfTags.
       */
      public int getNumberOfTags() {
         return mesh.getNumberOfTags(myIndex);
      }

      /**
       * Gets the current value assigned to tags
       *
       * @return Returns the tags.
       */
      public long[] getTags() {
         return mesh.getTags(myIndex);
      }

      /**
       * Gets the current value assigned to type
       *
       * @return Returns the type.
       */
      public int getType() {
         return mesh.getElementType(myIndex);
      }

      /**
       * Updates cached values that depend on the node positions. This routine
       * should be called after any transformation (e.g., rotate or translate)
       * that alters the node positions.
       */
      public abstract void updateGeom();

   }

   /**
    * <p>
    * A 2-node line.
    * </p>
    */

   static public class Line extends Element {

      public Line(Mesh mesh, int index) {
         this.mesh = mesh;
         myIndex = index;
         if (getNodeIndices().length != 2)
            throw new EPQFatalException("Wrong # of nodes for a 2-node line.");
      }

      @Override
      public void updateGeom() {
         /*
          * There's no cached geometry info in this class, so nothing to do
          * here.
          */
      }
   }
   /**
    * <p>
    * MeshShape is a ConnectedShape that describes the boundary of a mesh. It is
    * a closed surface consisting of triangular facets that are faces of
    * tetrahedral members of the mesh. Faces that are on the surface are members
    * of only a single tetrahedron, unlike faces in the interior, which have
    * tetrahedra on both sides.
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    *
    * @author John Villarrubia
    * @version 1.0
    */
   static public class MeshShape

         implements
            ConnectedShape,
            ITransform {
      private final Mesh mesh;

      /*
       * Saved data from previous intersection: the result (normal vector and
       * distance) and which tetrahedron we hit.
       */
      private double[] result;
      private int nextTetIndex = -1;

      /*
       * Saved data from previous contains() call: The Tetrahedron that contains
       * the point.
       */
      private Tetrahedron containingShape = null;

      /*
       * Our O(N^1/3) contains() method requires us to have a starting point
       * known to be inside the shape. We store it and its containing
       * tetrahedron here.
       */
      private double[] insidePoint = new double[3];
      private final Tetrahedron insideTet;

      /**
       * Constructs a MeshShape
       */
      public MeshShape(Mesh mesh) {
         this.mesh = mesh;
         /*
          * Pick an element near the middle. If it's not a tetrahedron Scan up
          * (and if necessary down) to find the next tetrahedron.
          */
         final int maxElementNumber = mesh.getNumberOfElements();
         int tetIndex = maxElementNumber / 2;
         while ((mesh.getElementType(tetIndex) != 4) && (tetIndex <= maxElementNumber))
            tetIndex++;
         if (mesh.getElementType(tetIndex) != 4) {
            tetIndex = maxElementNumber / 2;
            while ((mesh.getElementType(tetIndex) != 4) && (tetIndex >= 1))
               tetIndex--;
         }
         if (mesh.getElementType(tetIndex) != 4)
            throw new EPQFatalException("No tetrahedra in the mesh");
         insideTet = Tetrahedron.getTetrahedron(mesh, tetIndex);
         /* set insidePoint to the center of this tetrahedron */
         insidePoint = insideTet.getCenter();
      }

      /**
       * Returns the ConnectedShape that contained pos0 the last time
       * contains(pos0,pos1) or contains(pos0) returned true. I.e., this is the
       * tetrahedral mesh element that contained pos0 at that time.
       *
       * @return
       */
      @Override
      public Tetrahedron containingShape() {
         return containingShape;
      }

      /**
       * Returns true if this shape contains pos. If pos is on the boundary, it
       * returns true and moves pos a small distance (normally 1.e-15 m) off the
       * face towards the center of the containing mesh element.
       *
       * @param pos
       * @return
       * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#contains(double[])
       */
      @Override
      public boolean contains(double[] pos) {
         return contains(pos, null);
      }

      /**
       * Returns true if this shape contains pos0. If pos0 is on the boundary,
       * the point is deemed inside if the the path from pos0 to pos1 points to
       * the inside of the shape.
       *
       * @param pos0
       * @param pos1
       * @return
       * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#contains(double[],
       *      double[])
       */
      @Override
      public boolean contains(double[] pos0, double[] pos1) {
         return contains(pos0, pos1, insideTet, insidePoint);
      }

      /**
       * Same as contains(pos0, pos1, insideTet, insideTet.getCenter()). See the
       * documentation there.
       *
       * @param pos0
       * @param pos1
       * @param insideTet
       * @return
       */
      public boolean contains(double[] pos0, double[] pos1, Tetrahedron insideTet) {
         return contains(pos0, pos1, insideTet, insideTet.getCenter());
      }

      /**
       * <p>
       * This class contains several overloaded versions of the basic contains()
       * method. These are all variants on the basic functionality: return true
       * if pos0 is within the shape, false if it is not. Supplying pos1
       * produces the same result when pos0 is in the interior of the shape, but
       * if pos0 is on the surface it breaks the tie using pos1, returning true
       * if the path from pos0 to pos1 is entering the shape, false if it is
       * leaving it. The remaining variants provide the same result, but differ
       * in speed. The algorithm works by tracing a path from a starting
       * position known to be inside a particular Tetrahedron member of the
       * mesh. The closer the starting position is to pos0, the faster the
       * routine. Thus, the quickest version is to call contains with 4
       * arguments, where the last two arguments are a Tetrahedron that is
       * likely to be close to pos0 and a position within that Tetrahedron. The
       * next fastest is to provide 3 arguments, leaving out the position. In
       * this case the insidePoint will be calculated as the center of the
       * supplied Tetrahedron. Finally, if there is no reason to prefer one
       * Tetrahedron over another, the two-argument version (omitting both the
       * Tetrahedron and insidePoint) uses values for these predetermined and
       * stored by the constructor.
       * </p>
       *
       * @param pos0
       * @param pos1
       * @param insideTet
       * @param insidePoint
       * @return
       */
      public boolean contains(double[] pos0, double[] pos1, Tetrahedron insideTet, double[] insidePoint) {
         /*
          * We deem a point to be within the mesh if it is in one of the mesh's
          * constituent tetrahedra. A brute force search (checking all
          * tetrahedra until a match is found) is O(N), and N may be several
          * million, so we employ a more efficient algorithm. The idea is this:
          * Imagine a line segment joining insidePoint, known to be inside the
          * mesh, to pos0. Starting with insidePoint and insideFlag=true, step
          * from tetrahedron to tetrahedron along this path. At each boundary
          * where we step out of or into the whole mesh (i.e., at the mesh
          * surface, not the internal boundaries between elements) toggle the
          * flag from true to false or back again. When we reach pos0, the value
          * of the flag tells us whether we're inside or out. This algorithm
          * should be O(N^1/3).
          */

         /*
          * A note for future consideration. The following algorithm should be
          * O(N^1/3) as just described, but the proportionality constant seems
          * high, probably because of the need to instantiate each Tetrahedron
          * as the trajectory enters it. An alternative would be to use the
          * getFirstIntersection() method implemented below for the surface of
          * the MeshShape. This is O(N^2/3), but the triangles are all already
          * instantiated. Tests indicate that for a large mesh (2.5 million
          * tets), getFirstNormal requires 340 us (but a contains() based on it
          * would likely have to call it at least twice) while the contains
          * method below requires 940 us.
          */

         double[] currentpos = insidePoint.clone();
         boolean inside = true;
         ConnectedShape currentShape = insideTet;
         ConnectedShape previousShape = null;
         ConnectedShape nextShape = null;
         double u = currentShape.getFirstIntersection(currentpos, pos0);

         double[] smalldisp = {0., 0., 0.};
         boolean smalldispSet = false;

         while (u < 1.) {
            /*
             * The following conditionals check for backtracking. This can
             * happen as follows: If pos0 is on the boundary of the current tet
             * then u=1. However, round-off error can make u<1. In this case
             * currentpos switches to the center of the tet on the other side of
             * the boundary, from which position u is again = 1 but may again
             * round off to less than 1. We would end up in an infinite loop.
             * Instead, we detect this situation and decide which of the two
             * shapes contains pos0.
             */
            nextShape = currentShape.nextTet();
            if ((nextShape == previousShape) || ((nextShape == null) && (previousShape == this))) {
               /*
                * This block if we detect backtracking. At least one of the two
                * shapes must be a tet.
                */
               Tetrahedron tetShape = null;
               ConnectedShape otherShape = null;
               if (currentShape instanceof Tetrahedron) {
                  tetShape = (Tetrahedron) currentShape;
                  otherShape = nextShape;
               } else {
                  tetShape = (Tetrahedron) nextShape;
                  otherShape = currentShape;
               }
               boolean cont;
               if (pos1 == null)
                  cont = tetShape.contains(pos0);
               else
                  cont = tetShape.contains(pos0, pos1);
               if (cont) {
                  containingShape = tetShape;
                  return true;
               } else {
                  if (otherShape == null)
                     return false;
                  containingShape = (Tetrahedron) otherShape;
                  return true;
               }

            } else { // The usual case
               previousShape = currentShape;
               currentShape = nextShape;
            }
            if (inside && (currentShape == null)) { // Inside-out crossing
               currentShape = this;
               if (!smalldispSet) { // First time through
                  smalldisp = Math2.normalize(Math2.minus(pos0, currentpos));
                  for (int i = 0; i < 3; i++)
                     smalldisp[i] *= 1.e-16;
                  smalldispSet = true;
               }
               /* Put currentpos on the boundary */
               for (int i = 0; i < 3; i++)
                  currentpos[i] += u * (pos0[i] - currentpos[i]);
               while (inside) {
                  /* Move it slightly beyond */
                  for (int i = 0; i < 3; i++)
                     currentpos[i] += smalldisp[i];
                  /*
                   * Test that we're now outside (previousShape is the tet we're
                   * exiting)
                   */
                  inside = previousShape.contains(currentpos);
                  /*
                   * If we're now outside we're finished. Otherwise we move it a
                   * bit more
                   */
               }
            } else if (!inside && (currentShape != null)) { // Outside-in
                                                            // crossing
               inside = true;
               currentpos = ((Tetrahedron) currentShape).getCenter();
            } else { // inside - inside (transition between tets)
               // assert currentShape != null;
               assert currentShape != null;
               // Update starting position
               currentpos = ((Tetrahedron) currentShape).getCenter();
            }
            u = currentShape.getFirstIntersection(currentpos, pos0);
         }

         /*
          * Arrive here when there are no more boundaries between currentpos and
          * pos0.
          */

         if (u > 1) {
            /*
             * pos0 is unambiguously nearer than the next boundary. Inside is
             * true or false as dictated by the current value stored in that
             * flag.
             */
            if (inside)
               containingShape = (Tetrahedron) currentShape;
            return inside;
         }

         // Here if u==1: pos0 is ON a boundary
         if (pos1 != null) { // tiebreak available
            if (inside) {
               /*
                * If the current tetrahedron contains pos0, then return that
                */
               boolean cont = currentShape.contains(pos0, pos1);
               if (cont) {
                  containingShape = (Tetrahedron) currentShape;
                  return true;
               }

               /*
                * If the boundary we hit is an internal one, between two tets
                * within the MeshShape, then there will be a nextTet.
                */
               currentShape = currentShape.nextTet();
               if (currentShape == null)
                  return false; // No nextTet, so we're outside.
               // There is a nextTet, so check it
               cont = currentShape.contains(pos0, pos1);
               if (cont) {
                  containingShape = (Tetrahedron) currentShape;
                  return true;
               }
               return false;
            }
            /*
             * Here we were outside, with tiebreak available. This boundary must
             * be a surface of the MeshShape. If we are now inside, it will be
             * because we are inside the Tetrahedron that we have hit here.
             */
            currentShape = currentShape.nextTet();
            final boolean cont = currentShape.contains(pos0, pos1);
            if (cont)
               containingShape = (Tetrahedron) currentShape;
            return cont;

         }

         /*
          * Arrive here if there's no tiebreak, boundary counts as inside.
          * However, I once encountered a situation where the currentShape's
          * contains() method was not consistent with this determination. This
          * was due to round-off error. To make sure the pos is unequivocally
          * inside, we move it slightly towards the center.
          */
         if (inside) {
            containingShape = (Tetrahedron) currentShape;
            final double[] deltaDir = Math2.normalize(Math2.minus(pos0, currentpos));
            for (int i = 0; i < 3; i++)
               pos0[i] -= MonteCarloSS.SMALL_DISP * deltaDir[i];
         }

         return true;

      }

      @Override
      public double getFirstIntersection(double[] pos0, double[] pos1) {
         return (getFirstNormal(pos0, pos1))[3];
      }

      @Override
      public double[] getFirstNormal(double[] pos0, double[] pos1) {
         result = new double[]{0., 0., 0., Double.MAX_VALUE};
         /*
          * This is a brute-force implementation. It just checks all triangles
          * on the surface of the mesh. If N is the number of mesh elements,
          * this is an O(N^2/3) process. It should be possible to design a more
          * efficient--albeit more complicated--O(N^1/3) algorithm, but at
          * present the payoff for the added effort seems likely to be small. We
          * won't use MeshShape.getFirstNormal() from within the mesh since when
          * we begin inside we are always within one of the mesh's tetrahedral
          * subregions. Only when we are outside will this routine be
          * necessary--But my expectation is that we will mesh all the volume we
          * care about. Ouside the mesh will only be a field-free evacuated
          * region between the mesh and the chamber. In this region the mean
          * free path is infinite, so the electron ordinarily only takes a
          * single step. Under the circumstances, it doesn't pay to get too
          * fancy with this algorithm.
          */

         for (final Triangle t : mesh.boundaryFaces) {
            final double[] firstnormal = t.getFirstNormal(pos0, pos1);
            if ((firstnormal[3] > 0) && (firstnormal[3] < result[3])) {
               result = firstnormal;
               nextTetIndex = t.nextTetIndex();
            }
         }
         return result;
      }

      /**
       * Gets the current value assigned to mesh
       *
       * @return Returns the mesh.
       */
      @Override
      public Mesh getMesh() {
         return mesh;
      }

      @Override
      public double[] getPreviousNormal() {
         return result;
      }

      /**
       * Returns the tetrahedron across the boundary at the last boundary
       * intersection returned by getFirstNormal or getFirstIntersection.
       *
       * @return
       */
      @Override
      public Tetrahedron nextTet() {
         if (nextTetIndex > 0)
            return Tetrahedron.getTetrahedron(mesh, nextTetIndex);
         return null;
      }

      @Override
      public void rotate(double[] pivot, double phi, double theta, double psi) {
         mesh.rotate(pivot, phi, theta, psi);
      }

      /**
       * Generates a descriptive string in the form MeshShape[meshFileName].
       *
       * @return
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString() {
         return "MeshShape[" + mesh.getFileName() + "]";
      }

      @Override
      public void translate(double[] distance) {
         mesh.translate(distance);
      }

      /**
       * Updates any cached values that depend upon node coordinates.
       */
      @Override
      public void updateGeom() {
         insideTet.updateGeom();
         insidePoint = insideTet.getCenter();
         if (containingShape != null)
            containingShape.updateGeom();
      }
   }

   /**
    * <p>
    * A 4-node tetrahedron associated with a mesh. Use getTetrahedron in lieu of
    * the constructor because tetrahedra are cached so they need be initialized
    * only the first time they're needed.
    * </p>
    */
   static final public class Tetrahedron extends Element implements ConnectedShape {

      /**
       * Returns the Tetrahedron corresponding to the indexed element of mesh.
       * Throws EPQFatalException if the indexed element is not a tetrahedron.
       *
       * @param mesh
       * @param index
       * @return
       */
      static public Tetrahedron getTetrahedron(Mesh mesh, int index) {
         if (mesh.elements[index] == null)
            mesh.elements[index] = new Tetrahedron(mesh, index);
         return (Tetrahedron) mesh.elements[index];
      }

      /**
       * Returns true if the indexed element is a tetrahedron and the
       * tetrahedron has already been instantiated in the cache.
       *
       * @param mesh
       * @param index
       * @return
       */
      static public boolean tetExists(Mesh mesh, int index) {
         return (mesh.getElementType(index) == 4) && (mesh.elements[index] != null);
      }

      private int intersectedFace = -1; // Most recently intersected face index
      private boolean tie = false; // true if most recent intersection is at an
      // edge or node
      private double[] result = null; // Most recent intersection result in
      // packed form
      private double[] pos0, pos1;

      /*
       * Following is predigested geometrical information. It must be updated by
       * using updateGeom() after transformations. A point p is in a plane if
       * n.p-b=0. n and b are stored in the following arrays. The tetrahedron
       * has 4 triangular faces, indexed 0 to 3. The numbering convention
       * adopted here: Face i is the triangle with 3 nodes obtained by omitting
       * node i.
       */
      /* normal vectors of 4 faces */
      private final double[][] narray = new double[4][3];
      /* corresponding b values for 4 faces */
      private final double[] barray = new double[4];

      private final double[] electricField = new double[]{0., 0., 0.};

      private double v0 = 0.;

      private double sphereRad = -1.;

      private boolean sphereRadReady = false;

      private final double FACTOR = 3. / 4. / Math.PI;

      /**
       * Constructs the Tetrahedron corresponding to the indexed element of
       * mesh. Throws EPQFatalException if the indexed element is not a
       * tetrahedron.
       *
       * @param mesh
       *           - the mesh with which this Tetrahedron is associated.
       * @param index
       *           - the Tetrahedron's index within the mesh.
       */
      private Tetrahedron(Mesh mesh, int index) {
         this.mesh = mesh;
         myIndex = index;
         if (mesh.getElementType(myIndex) != 4)
            throw new EPQFatalException("Mesh element at index " + index + " is not a tetrahedron.");
         updateGeom();
      }

      /**
       * Returns the Tetrathedron adjacent to this one through the indexed face
       *
       * @param faceIndex
       *           - index of the face shared with the adjacent Tetrahedron
       * @return
       */
      public Tetrahedron adjacentTet(int faceIndex) {
         final int adjVolIndex = adjacentTetIndex(faceIndex);
         if (adjVolIndex < 1)
            return null; // It was a mesh boundary
         else
            return Tetrahedron.getTetrahedron(mesh, adjVolIndex);
      }

      /**
       * Returns the index of the Tetrathedron adjacent to this one through the
       * indexed face. Returns 0 if the face is on the mesh boundary (no
       * adjacent Tetrahedron).
       *
       * @param faceIndex
       *           - index of the face shared with the adjacent Tetrahedron
       * @return
       */
      public int adjacentTetIndex(int faceIndex) {
         return mesh.getAdjacentVolumeIndex(myIndex, faceIndex);
      }

      @Override
      public Tetrahedron containingShape() {
         return this;
      }

      @Override
      public boolean contains(double[] pos) {
         double posDotn;
         for (int i = 0; i < 4; i++) {
            posDotn = (pos[0] * narray[i][0]) + (pos[1] * narray[i][1]) + (pos[2] * narray[i][2]);
            if (posDotn > barray[i])
               return false;
         }
         return true;
      }

      @Override
      public boolean contains(double[] pos0, double[] pos1) {
         boolean didDelta = false;
         double p0dotn;
         double[] delta = null;
         // Loop over all planes in the shape
         for (int i = 0; i < 4; i++) {
            p0dotn = (pos0[0] * narray[i][0]) + (pos0[1] * narray[i][1]) + (pos0[2] * narray[i][2]);
            if (p0dotn > barray[i])
               return false;
            if (p0dotn == barray[i]) { // p0 is ON the boundary
               if (!didDelta) {
                  delta = new double[]{pos1[0] - pos0[0], pos1[1] - pos0[1], pos1[2] - pos0[2]};
                  didDelta = true;
               }
               final double deltadotn = (delta[0] * narray[i][0]) + (delta[1] * narray[i][1]) + (delta[2] * narray[i][2]);
               if (deltadotn > 0.)
                  return false;
               if ((deltadotn == 0.) && !mesh.containsTieBreak(narray[i]))
                  return false;
            }
         }
         return true;
      }

      /**
       * Decrements the charge number by 1. This corresponds, e.g., to a gain of
       * one electron in this volume element.
       */
      public void decrementChargeNumber() {
         mesh.decrementChargeNumber(myIndex);
      }

      /**
       * Returns the area of the indexed face.
       *
       * @param index
       *           - index of face for which area is to be returned
       * @return - area of indexed face
       */
      public double faceArea(int index) {
         /* Get indices of nodes of this face */
         final int[] facenodeindices = mesh.tetFaceNodeIndices(myIndex, index);
         /* Coordinates of one of the nodes */
         final double[] node0 = mesh.getNodeCoordinates(facenodeindices[0]);
         /*
          * Compute vectors that define the 2 edges with node0 at their vertex
          */
         final double[] edge1 = Math2.minus(mesh.getNodeCoordinates(facenodeindices[1]), node0);
         final double[] edge2 = Math2.minus(mesh.getNodeCoordinates(facenodeindices[2]), node0);
         /* Magnitude of cross product is twice the area */
         return Math2.magnitude(Math2.cross(edge1, edge2)) / 2.;
      }

      /**
       * Returns the outward pointing normal vector of the face with the
       * supplied index
       *
       * @param index
       *           - index of the face for which the normal vector is to be
       *           returned
       * @return - the normal vector
       */
      public double[] faceNormal(int index) {
         return narray[index].clone();
      }

      /**
       * Let v be a column vector of potentials at the 4 nodes. Then the
       * potential at x inside the tetrahedron is v0 - E.x where v0 is a
       * constant and E is the electric field vector. (v0,-E) is computed from
       * geoCoef.v. geoCoef depends only on the geometry (node coordinates).
       * This private routine computes the geoCoef matrix when needed.
       */
      private Matrix geoCoef() {
         // Assemble the coordinates matrix
         final Matrix coord = new Matrix(4, 4, 1.); // 4x4 matrix of 1s
         // Replace rows 0 to 3, columns 1 to 3 with node coordinates.
         final int[] elementsNodeIndices = mesh.getNodeIndices(myIndex);
         coord.setMatrix(0, 3, 1, 3,
               new Matrix(new double[][]{mesh.getNodeCoordinates(elementsNodeIndices[0]), mesh.getNodeCoordinates(elementsNodeIndices[1]),
                     mesh.getNodeCoordinates(elementsNodeIndices[2]), mesh.getNodeCoordinates(elementsNodeIndices[3])}));
         return coord.inverse();
      }

      /**
       * Returns center coordinates of this tet.
       *
       * @return - the average of the 4 node positions, a double[] of length 3.
       */
      public double[] getCenter() {
         final double[] center = new double[3];
         final int[] nI = mesh.getNodeIndices(myIndex);
         for (int j = 0; j < 3; j++) {
            for (int i = 0; i < 4; i++) {
               final double[] nC = mesh.getNodeCoordinates(nI[i]);
               center[j] += nC[j];
            }
            center[j] /= 4.;
         }
         return center;
      }

      /**
       * Returns the average charge density within this volume in SI units
       * (Coulombs/cubic meter).
       *
       * @return - the charge density
       */
      public double getChargeDensity() {
         return (getChargeNumber() * PhysicalConstants.ElectronCharge) / getVolume();
      }

      /**
       * Returns the total charge in this volume in units of e. E.g., the
       * electron charge is -1;
       *
       * @return - the charge number
       */
      public int getChargeNumber() {
         return mesh.getChargeNumber(myIndex);
      }

      /**
       * Returns the electric field inside this tetrahedron, as determined from
       * the potentials at its nodes. (Note that no position argument is
       * required because the FEA does a linear approximation to the potential.
       * Within this approximation the electric field is constant within each
       * element.)
       *
       * @return - the electric field in the form of a double[] of length 3.
       */
      public double[] getEField() {
         return electricField.clone();
      }

      /**
       * Returns the radius of a sphere of the same volume as this tet. This is
       * (3*volume/4/pi)^1/3.
       *
       * @return - the volume
       */
      public double getEquivalentSphereRadius() {
         if (sphereRadReady)
            return sphereRad;
         else {
            sphereRad = Math.pow(FACTOR * getVolume(), 1. / 3.);
            sphereRadReady = true;
            return sphereRad;
         }
      }

      @Override
      public double getFirstIntersection(double[] pos0, double[] pos1) {
         return (getFirstNormal(pos0, pos1))[3];
      }

      @Override
      public double[] getFirstNormal(double[] pos0, double[] pos1) {
         /*
          * See notes on the algorithm in NormalMultiPlaneShape
          */
         boolean maxtie = false, mintie = false, withinPlane = false;

         double umin = Double.NEGATIVE_INFINITY; // Starting interval is the
         // whole real line
         double umax = Double.POSITIVE_INFINITY;
         double u;
         int minindex = -1; // Stores index of plane responsible for umin
         int maxindex = -1; // Same for umax. Initial values are illegal
         // indices.
         result = new double[]{0., 0., 0., Double.MAX_VALUE}; // Initial value
                                                              // designates no
                                                              // intersection

         final double[] delta = {pos1[0] - pos0[0], pos1[1] - pos0[1], pos1[2] - pos0[2]};
         for (int i = 0; i < 4; i++) {
            /*
             * Note significance of the sign of the next two variables
             * numerator<0 means pos0 is inside the current face; numerator>0
             * means it's outside. denominator<0 means the line segment and the
             * plane normal point in opposite directions. I.e., this
             * intersection is an outside->inside transition. denominator>0
             * means the opposite.
             */
            final double numerator = ((pos0[0] * narray[i][0]) + (pos0[1] * narray[i][1]) + (pos0[2] * narray[i][2])) - barray[i];
            final double denominator = (delta[0] * narray[i][0]) + (delta[1] * narray[i][1]) + (delta[2] * narray[i][2]);
            if (denominator == 0) {
               /*
                * If the trajectory is parallel to the plane there are no
                * intersections. If it starts inside it's always inside. If it
                * starts outside it's always outside. In or Out is determined by
                * the numerator. numerator<0, or =0 with tie break = true, means
                * it is inside. In this case we continue looping, searching for
                * intersections with other planes of this shape. Otherwise, we
                * return u>1.
                */
               if ((numerator < 0) || ((numerator == 0) && mesh.containsTieBreak(narray[i]))) {
                  /*
                   * In the next line, if numerator=0 then the second of the
                   * above conditions is what places us within this if block.
                   * The trajectory lies wholly within the plane.
                   */
                  withinPlane = withinPlane || (numerator == 0);
                  continue;
               }
               tie = false;
               return result;
            }
            u = -numerator / denominator; // Compute intersection point
            if (denominator > 0) { // This is an insidethisplane->outside
               // transition
               if (u < umax) { // It changes change umax
                  /*
                   * If the new umax is < 0 the "inside" is behind our line
                   * segment If the new umax is < umin, this plane's inside and
                   * an earlier one are disjoint. In either case abort and
                   * return no intersection
                   */
                  if ((u < 0)
                        || (u <= umin)) { /*
                                           * If the new umax is < 0 the "inside"
                                           * is behind our line segment If the
                                           * new umax is < umin, this plane's
                                           * inside and an earlier one are
                                           * disjoint. If umax=umin, the
                                           * trajectory enters and leaves the
                                           * shape at the same point, i.e., it
                                           * is tangent to the surface. Since
                                           * our shape is convex, a line can
                                           * only be tangent on the OUTside, so
                                           * this counts as a non-intersection.
                                           * In any of these cases, abort and
                                           * return no intersection.
                                           */
                     tie = false;
                     return result;
                  }
                  maxtie = false;
                  umax = u;
                  maxindex = i; // remember index of this face
               } else if (u == umax)
                  maxtie = true;
            } else if (u > umin) { // It changes umin
               /*
                * If the new umin is > 1 the "inside" is beyond the end of our
                * line segment. If it is >umax this plane's inside and an
                * earlier one are disjoint. Return "no intersection" in either
                * case.
                */
               if ((u > 1) || (u >= umax)) {
                  tie = false;
                  return result;
               }
               mintie = false;
               umin = u;
               minindex = i; // Remember index of this plane
            } else if (u == umin)
               mintie = true;
         } // end for

         // When we arrive here [umin,umax] defines the completed intersection
         // interval
         if (umin > 0) { // Our boundary crossing is outside -> inside at umin
            result[3] = umin;
            intersectedFace = minindex;
            tie = mintie || withinPlane;
            if (tie) { // remember positions for possible further processing
               this.pos0 = pos0.clone();
               this.pos1 = pos1.clone();
            }
            result[0] = narray[intersectedFace][0];
            result[1] = narray[intersectedFace][1];
            result[2] = narray[intersectedFace][2];
            return result;
         } // Otherwise our starting position was already inside
         if ((umax <= 1) && (umax > 0.)) { // Our boundary crossing is outside
                                           // ->
            // inside at umax<1
            result[3] = umax;
            intersectedFace = maxindex;
            tie = maxtie || withinPlane;
            if (tie) { // remember positions for possible further processing
               this.pos0 = pos0.clone();
               this.pos1 = pos1.clone();
            }
            result[0] = narray[intersectedFace][0];
            result[1] = narray[intersectedFace][1];
            result[2] = narray[intersectedFace][2];
            return result;
         } // Otherwise the entire pos0, pos1 interval lies inside
         tie = false;
         return result; // return "no intersection"
      }

      @Override
      public Mesh getMesh() {
         return mesh;
      }

      /**
       * Returns the potential at x, linearly determined from the potentials at
       * this tetrahedron's nodes. This is only reasonably accurate if x is
       * inside the tetrahedron. If x is outside, this routine will extrapolate;
       * this can give inaccurate results.
       *
       * @param x
       *           - a point inside this element
       * @return - the potential (in volts) at x
       */
      public double getPotential(double[] x) {
         final double potential = v0 - (electricField[0] * x[0]) - (electricField[1] * x[1]) - (electricField[2] * x[2]);
         return potential;
      }

      /**
       * Returns the index of the most recently intersected face. The
       * tetrahedron has 4 nodes and 4 triangular faces. The numbering
       * convention: Face i is the triangle specified by omitting node i.
       *
       * @return - the index of the intersected face
       */
      public int getPreviousIntersectedFace() {
         return intersectedFace;
      }

      @Override
      public double[] getPreviousNormal() {
         return result;
      }

      /**
       * Returns the volume in cubic meters of this tetrahedral element.
       *
       * @return - the volume
       */
      public double getVolume() {
         return mesh.getVolume(myIndex);
      }

      /**
       * Increments the charge number by 1. This corresponds, e.g., to a loss of
       * one electron from this volume element.
       */
      public void incrementChargeNumber() {
         mesh.incrementChargeNumber(myIndex);
      }

      /**
       * Returns the Tetrahedron on the other side of the boundary at the last
       * intersection, or null if the trajectory was exiting the mesh.
       *
       * @return - the next Tetrahedron
       */
      @Override
      public Tetrahedron nextTet() {
         if (!tie)
            return adjacentTet(intersectedFace);
         /*
          * We end up here rarely, only if the trajectory at the last
          * intersection was incident exactly onto an edge or node. The next
          * line constructs the list of candidates in order from most to least
          * probable.
          */
         final int[] candidates = mesh.extendedAdjacentVolumeArray(myIndex, intersectedFace);
         /*
          * Because of our strict definition of intersection (the continuation
          * of the trajectory must go inside the shape if an intersection is to
          * count) we can intersect at most ONE of these shapes. Therefore, we
          * stop searching as soon as we find one.
          */
         final double u = Double.MAX_VALUE;
         for (final int index : candidates) {
            final Tetrahedron candtet = Tetrahedron.getTetrahedron(mesh, index);
            /*
             * if this is the next tet, our previous trajectory should intersect
             * it earlier than any of the others
             */
            final double thisu = candtet.getFirstIntersection(pos0, pos1);
            if ((thisu > 0) && (thisu <= 1) && (thisu < u))
               return candtet;
         }
         /*
          * If we didn't intersect any of them, we must have hit a boundary of
          * the mesh and we're going outside.
          */
         return null;
      }

      /**
       * Sets the charge number to the supplied value.
       *
       * @param n
       *           - The value to which to set the charge, in units of e.
       */
      public void setChargeNumber(int n) {
         mesh.setChargeNumber(myIndex, n);
      }

      @Override
      public String toString() {
         return "Tetrahedron[Nodes: " + Arrays.toString(getNodeIndices()) + "]";
      }

      /**
       * Updates cached values that depend on the node positions. This routine
       * is called after any transformation (e.g., rotate or translate) that
       * alters the node positions. The updated values including the normal
       * vectors and distances to the planes of the 4 faces, the matrix of
       * geometry parameters that is used to compute the interpolation
       * parameters, and the interpolation parameters.
       */
      @Override
      public void updateGeom() {
         for (int i = 0; i < 4; i++) {
            final int[] facenodeindices = mesh.tetFaceNodeIndices(myIndex, i);
            final double[] coords0 = mesh.getNodeCoordinates(facenodeindices[0]);
            mesh.planePerp(narray[i], coords0, mesh.getNodeCoordinates(facenodeindices[1]), mesh.getNodeCoordinates(facenodeindices[2]));
            barray[i] = Math2.dot(narray[i], coords0);
         }
         sphereRadReady = false;
         updatePotentials();
      }

      /**
       * This method recomputes cached values that depend upon the values of
       * electrostatic potential at the tetrahedron nodes. It should be called
       * whenever these are altered for any reason, for example after a new FEA
       * solution. Until it is called, the getPotential() and getEField()
       * methods will return values that do not reflect the updated values of
       * potentials on the nodes.
       */
      public void updatePotentials() {
         final int[] nI = mesh.getNodeIndices(myIndex);
         final Matrix temp = geoCoef().times(new Matrix(
               new double[]{mesh.getNodePotential(nI[0]), mesh.getNodePotential(nI[1]), mesh.getNodePotential(nI[2]), mesh.getNodePotential(nI[3])},
               4));
         v0 = temp.get(0, 0);
         electricField[0] = -temp.get(1, 0);
         electricField[1] = -temp.get(2, 0);
         electricField[2] = -temp.get(3, 0);
      }

   }
   /**
    * <p>
    * A 3-node triangle. The triangle has an inside and an outside determined by
    * the order of its nodes. The nodes are clockwise from the inside.
    * </p>
    */
   static public class Triangle extends Element {

      private final double[] n = new double[3];
      private double b;
      private final int[] nodeIndices;

      /*
       * Following is the index of the inside Tetrahedron. The insideTet is the
       * one that n points away from.
       */
      private final int insideTet;
      /*
       * The index of the face of the insideTet that corresponds to this
       * Triangle
       */
      private final int faceIndex;
      /*
       * A side-effect of getFirstNormal is to set the following variable.
       * nextTet is either insideTet or outsideTet, whichever is the one on the
       * other side of the boundary intersected by the segment from p0 to p1.
       */
      private int nextTet = -1;
      /*
       * Following are vectors representing the 3 sides. Numbering convention:
       * Side i is the side that starts at node i and moves counterclockwise
       * around the triangle when viewed from the outside. Thus, side 0 is
       * node1-node0, side 1 is node2-node1, and side 2 is node0-node2.
       */
      private final double[][] sides = new double[3][];

      /**
       * Constructs a Triangle
       *
       * @param mesh
       *           - The mesh with which this triangle is associated
       * @param faceIndex
       *           - The index of the face of insideTet that corresonds to this
       *           triangle.
       * @param insideTet
       *           - the index of the Tetrahedron of which this triangle is a
       *           face. (This tetrahedron is inside the plane of the Triangle.)
       */

      public Triangle(Mesh mesh, int faceIndex, int insideTet) {
         this.mesh = mesh;
         this.faceIndex = faceIndex;
         this.insideTet = insideTet;
         nodeIndices = mesh.tetFaceNodeIndices(insideTet, faceIndex);
         if (nodeIndices.length != 3)
            throw new EPQFatalException("Wrong # of nodes for a 3-node triangle.");
         updateGeom();
      }

      /**
       * Returns scaled distance to intersection with one of the Tetrahedra of
       * which this Triangle is a face. This is the definitive test of whether
       * and where a trajectory hits the triangle. However, it is costly to use,
       * because it requires instantiating one or two tetrahedra and checking 4
       * faces for each one. For this reason it is best used only to adjudicate
       * ambiguous cases.
       *
       * @param p0
       *           - trajectory leg start point
       * @param p1
       *           - trajectory leg end point
       * @return - u, such that p0 + u(p1-p0) is the intersection. Values of u>1
       *         may not be quantitatively correct, and should be taken to mean
       *         only "no intersection between p0 and p1".
       */
      private double associatedTetrahedronIntersection(double[] p0, double[] p1) {
         Tetrahedron tet = Tetrahedron.getTetrahedron(mesh, insideTet);
         double u = tet.getFirstIntersection(p0, p1);
         nextTet = insideTet;
         // Check the outside one if it exists
         final int outsideTet = getOutsideTet();
         if (outsideTet != -1) {
            tet = Tetrahedron.getTetrahedron(mesh, outsideTet);
            final double temp = tet.getFirstIntersection(p0, p1);
            // Set u to the smaller positive one
            if ((temp > 0.) && ((u < 0.) || (temp < u))) {
               u = temp;
               nextTet = outsideTet;
            }
            /*
             * The remaining possibility is temp negative, in which case we
             * leave u as is.
             */
         }
         return u;
      }

      /**
       * Returns [nx,ny,nz,u], where [nx,ny,nz] is the normalized vector
       * pointing towards the outside of this Triangle and u is a parameter such
       * that the intersection of the line segment from p0 to p1 with the
       * Triangle is at p0+u*(p1-p0). u will only be accurate if it lies between
       * 0 and 1 (i.e., if the intersection lines within the line segment). If
       * there is no intersection or if the intersection is past p1, a value of
       * u&gt;1 will be returned.
       *
       * @param p0
       *           - The coordinates of the starting point of a line segment
       * @param p1
       *           - Coordinates of the end point of the line segment
       * @return - [nx,ny,nz,u] as described above.
       */
      public double[] getFirstNormal(double[] p0, double[] p1) {
         final double[] result = {n[0], n[1], n[2], Double.MAX_VALUE};
         /*
          * Consider the line containing the segment joining p0 to p1. This line
          * has equation x = p0 + u*(p1-p0). The plane defined by our triangle
          * has equation n.x - b = 0. Compute u = (b-n.p)/(n.(p1-p0)).
          */

         double u = 0.;
         final double[] delta = new double[]{ // delta = p1-p0
               p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]};
         final double denominator = (n[0] * delta[0]) + (n[1] * delta[1]) + (n[2] * delta[2]);
         final double numerator = b - ((n[0] * p0[0]) + (n[1] * p0[1]) + (n[2] * p0[2]));

         if (denominator == 0.) {
            if ((numerator == 0.) && mesh.containsTieBreak(n))
               /*
                * The numerator and denominator both = 0 case is a trajectory
                * that hits the triangle edge-on. It is so improbable it almost
                * never happens accidentally. It most commonly arises if the
                * user aims the electron gun right at an edge. A tie break
                * decides whether the plane of the triangle contains or does not
                * contain the trajectory. The tie breaker must be consistent
                * with the one used by Tetrahedron.contains(). If the plane does
                * contain the trajectory then further examination is required to
                * determine whether the triangle does. We deem the trajectory to
                * hit the triangle if it hits one of the tetrahedra associated
                * with the triangle. I.e., the NormalShape intersection rules
                * are used to adjudicate the case.
                */
               result[3] = associatedTetrahedronIntersection(p0, p1);
            return result;
         }

         u = numerator / denominator; // Distance to intersection
         /*
          * If we start outside the plane (numerator < 0) then the next tet will
          * be the inside one, and vice versa.
          */
         nextTet = (numerator < 0) ? insideTet : getOutsideTet();

         if ((u <= 0.) || (u > 1.))
            return result;

         /*
          * We arrive here if we intersect the plane of this triangle between p0
          * and p1. It remains to determine whether the intersection point lies
          * within or outside the triangle.
          */

         final double[] x = { // Coordinates of the intersection
               p0[0] + (u * delta[0]), p0[1] + (u * delta[1]), p0[2] + (u * delta[2])};

         for (int i = 0; i < 3; i++) {
            final double val = insideEdge(i, x);
            if (val < 0.)
               return result; // It missed
            if (val == 0.) { // It exactly hit an edge
               u = associatedTetrahedronIntersection(p0, p1);
               break;
            }
         }

         /* Here if x is within the triangle */
         result[3] = u;
         return result;
      }

      /**
       * Gets the value of the tetrahedron that adjoins this triangle on the
       * inside.
       *
       * @return
       */
      public int getInsideTet() {
         return insideTet;
      }

      /**
       * Gets the index of the Tetrahedron that adjoins this triangle on the
       * outside.
       *
       * @return
       */
      public int getOutsideTet() {
         return mesh.getAdjacentVolumeIndex(insideTet, faceIndex);
      }

      /**
       * Applies a cross product test to determine if x is inside the indexed
       * edge. The returned value is positive if x is inside, negative if
       * outside, 0 if on the edge.
       *
       * @param edgeIndex
       * @param x
       * @return
       */
      private double insideEdge(int edgeIndex, double[] x) {
         final double[] v = mesh.getNodeCoordinates(nodeIndices[edgeIndex]);
         final double[] deltax = {x[0] - v[0], x[1] - v[1], x[2] - v[2]};
         return Math2.dot(Math2.cross(sides[edgeIndex], deltax), n);
      }

      /**
       * Returns the index of the tetrahedron on the other side of the boundary
       * intersected by the line segment from p0 to p1 on the last call to
       * getFirstNormal.
       *
       * @return - index of the adjacent tetrahedron
       */
      public int nextTetIndex() {
         return nextTet;
      }

      @Override
      public void updateGeom() {
         final double[][] coords = new double[3][];
         for (int i = 0; i < 3; i++)
            coords[i] = mesh.getNodeCoordinates(nodeIndices[i]);
         mesh.planePerp(n, coords[0], coords[1], coords[2]);
         b = Math2.dot(n, coords[0]);
         sides[0] = Math2.minus(coords[1], coords[0]);
         sides[1] = Math2.minus(coords[2], coords[1]);
         sides[2] = Math2.minus(coords[0], coords[2]);
      }
   }

   private final IBasicMesh basicMesh;

   private boolean isAdaptive;

   /* An array for cached elements */
   private Element[] elements;

   private int lastMeshRevision = -1; // Initialize to impossible value

   private Triangle[] boundaryFaces;

   private MeshShape myShape;

   public Mesh(IBasicMesh basicMesh) {
      this.basicMesh = basicMesh;
      if (basicMesh instanceof IAdaptiveMesh)
         isAdaptive = true;
      else
         isAdaptive = false;
      initializeIfNeeded(); // It WILL be needed this first time.
   }

   /**
    * Constructs a Mesh
    *
    * @param meshFileName
    * @throws FileNotFoundException
    */
   public Mesh(String meshFileName) throws FileNotFoundException {
      this(new GmshMesh(meshFileName));
   }

   /**
    * Resets charge densities in all volume elements and potentials at all nodes
    * to 0, then calls updateAllPotentials().
    */
   public void clearElectrical() {
      for (int elementIndex = 1; elementIndex <= getNumberOfElements(); elementIndex++)
         setChargeNumber(elementIndex, 0);
      for (int nodeIndex = 1; nodeIndex <= getNumberOfNodes(); nodeIndex++)
         setNodePotential(nodeIndex, 0.);
      updateAllPotentials();
   }

   /**
    * Clears the cache of all instantiated mesh elements, making the memory
    * available.
    */
   public void clearElementsCache() {
      elements = new Element[basicMesh.getNumberOfElements() + 1];
   }

   /**
    * To be called if pos0 and pos1 both lie within a plane. This method
    * arbitrarily assigns containment based on a tie-break algorithm that is a
    * function of the plane normal, which is supplied as a parameter.
    *
    * @param normal
    *           - the plane's normal vector
    * @return - true if the plane contains the point, false otherwise.
    */
   private boolean containsTieBreak(double[] normal) {
      if (normal[0] < 0.)
         return false;
      if (normal[0] == 0.) {
         if (normal[1] < 0.)
            return false;
         if (normal[1] == 0.)
            if (normal[2] < 0.)
               return false;
      }
      return true;
   }

   /**
    * Decrements the charge number by 1. This corresponds, e.g., to an increase
    * of one electron in this volume element.
    *
    * @param index
    *           - The index of the element for which charge is to be
    *           decremented.
    */
   public void decrementChargeNumber(int index) {
      basicMesh.decrementChargeNumber(index);
   }

   /**
    * Exports the charge contained in any mesh elements for which the charge is
    * nonzero and the potentials on each of the nodes of this mesh. This can be
    * used to save the state of the mesh during or after a simulation. At a
    * later time, a duplicate of the mesh can be created by once again calling
    * the constructor and then importing the charge and potentials by using the
    * importChargeAndPotentials() method. Note that the node and element
    * indexing used by these methods is the same as that of the mesh file used
    * to generate this mesh; the import and export methods cannot be used with a
    * different mesh, even of the same sample.
    *
    * @param outName
    *           - the name of the file to which the data are to be saved.
    * @throws IOException
    */
   public void exportChargeAndPotentials(String outName) throws IOException {
      BufferedWriter out = null;

      try {
         out = new BufferedWriter(new FileWriter(outName));
         out.write("$NodePotentials");
         out.newLine();
         final int nn = getNumberOfNodes();
         out.write(Integer.toString(nn));
         out.newLine();
         for (int i = 1; i <= nn; i++) {
            out.write(Double.toString(getNodePotential(i)));
            out.newLine();
         }

         out.write("$VolumeElementCharges");
         out.newLine();
         final int nel = getNumberOfElements();
         int count = 0;
         for (int i = 1; i <= nel; i++)
            if (getChargeNumber(i) != 0)
               count++;
         out.write(Integer.toString(count));
         out.newLine();
         for (int i = 1; i <= nel; i++)
            if (getChargeNumber(i) != 0) {
               out.write(Integer.toString(i) + "\t" + Integer.toString(getChargeNumber(i)));
               out.newLine();
            }
      } catch (final IOException e) {
         throw new EPQFatalException("Error opening or writing to " + outName + ".");
      } finally {
         if (out != null)
            out.close();
      }
   }

   public int[] extendedAdjacentVolumeArray(int tetIndex, int faceIndex) {
      return basicMesh.extendedAdjacentVolumeArray(tetIndex, faceIndex);
   }

   /**
    * Returns the index of the volume element adjacent to the indexed one
    * through the indexed face. If there is no adjacent volume (i.e., the
    * indexed face is on the mesh boundary) 0 is returned.
    *
    * @param elementIndex
    *           - index of the volume element for which the neighboring element
    *           is to be returned.
    * @param faceIndex
    *           - index of the face shared with the neighbor
    * @return
    */
   public int getAdjacentVolumeIndex(int elementIndex, int faceIndex) {
      return basicMesh.getAdjacentVolumeIndex(elementIndex, faceIndex);
   }

   public IBasicMesh getBasicMesh() {
      return basicMesh;
   }

   /**
    * Given an array of positions inside the mesh, returns an array of
    * corresponding charge densities (in C/m^3). If a position lies outside the
    * mesh the returned value is 0.
    *
    * @param pos
    *           - An N x 3 (double[][]) array of positions
    * @return - double[] An array of corresponding potentials.
    */
   public double[] getChargeDensity(double[][] pos) {
      final int n = pos.length;
      final double[] v = new double[n];
      for (int i = 0; i < n; i++) {
         final double[] p = pos[i];
         if (myShape.contains(p)) {
            final Tetrahedron cE = myShape.containingShape();
            v[i] = cE.getChargeDensity();
         } else
            v[i] = 0.;
      }
      return v;
   }

   /**
    * Returns the total charge contained in volume elements tagged with the
    * given tag. (Performance note: this requires a scan through all elements of
    * the mesh.) The returned charge is in units of e (magnitude of electron
    * charge).
    *
    * @return - the charge number
    */
   public int getChargeFromTag(long tag) {
      int totalCharge = 0;
      int elementCharge;
      final int numElements = elements.length;
      for (int i = 1; i < numElements; i++)
         if (isVolumeType(i) && (basicMesh.getTags(i)[0] == tag) && ((elementCharge = basicMesh.getChargeNumber(i)) != 0))
            totalCharge += elementCharge;
      return totalCharge;
   }

   /**
    * Given an array of positions inside the mesh, returns an array of
    * containing the number of elementary charges in the mesh that contains each
    * position. If a position lies outside the mesh the returned value is 0.
    *
    * @param pos
    *           - An N x 3 (double[][]) array of positions
    * @return - double[] An array of corresponding potentials.
    */
   public int[] getChargeNumber(double[][] pos) {
      final int n = pos.length;
      final int[] c = new int[n];
      for (int i = 0; i < n; i++) {
         final double[] p = pos[i];
         if (myShape.contains(p)) {
            final Tetrahedron cE = myShape.containingShape();
            c[i] = cE.getChargeNumber();
         } else
            c[i] = 0;
      }
      return c;
   }

   /**
    * Returns the total charge in the indexed element in units of e. E.g., the
    * electron charge is -1; Only volume elements carry charge. Others return 0.
    *
    * @return - the charge number
    */
   public int getChargeNumber(int index) {
      return basicMesh.getChargeNumber(index);
   }

   /**
    * Returns the type of mesh element at the given index. The main relevant
    * types are currently 1-line, 2-triangle, 4-tetrahedron. See gmsh
    * documentation for other types. Note that indexing of elements starts from
    * 1 rather than 0, to follow Gmsh convention. Therefore elements are indexed
    * from 1 to getNumberOfElements().
    *
    * @param elementIndex
    * @return
    */
   public int getElementType(int elementIndex) {
      return basicMesh.getElementType(elementIndex);
   }

   /**
    * Gets the name of the file that was read to produce this mesh
    *
    * @return
    */
   public String getFileName() {
      return basicMesh.getFileName();
   }

   public int getMeshRevision() {
      if (isAdaptive)
         return ((IAdaptiveMesh) basicMesh).getMeshRevision();
      else
         return 0;
   }

   /**
    * gets the MeshShape associated with this Mesh. The MeshShape is a
    * ConnectedShape that describes the boundary of a mesh. It is a closed
    * surface consisting of triangular facets that are faces of tetrahedral
    * members of the mesh. Faces that are on the surface are members of only a
    * single tetrahedron, unlike faces in the interior, which have tetrahedra on
    * both sides.
    *
    * @return
    */
   public MeshShape getMeshShape() {
      return myShape;
   }

   /**
    * Returns an array of type int, each of which is the index of a volume
    * element that shares the supplied nodeIndex.
    *
    * @param nodeIndex
    * @return
    */
   public int[] getNodeAdjacentVolumes(int nodeIndex) {
      return basicMesh.getNodeAdjacentVolumes(nodeIndex);
   }

   /**
    * Gets the coordinates of the node with the given index. Note that indexing
    * of nodes starts from 1 rather than 0, to follow Gmsh convention. Therefore
    * nodes are indexed from 1 to getNumberOfNodes().
    *
    * @param nodeIndex
    * @return
    */
   public double[] getNodeCoordinates(int nodeIndex) {
      return basicMesh.getNodeCoordinates(nodeIndex);
   }

   /**
    * Returns the array of node indices associated with the element at the given
    * index. Note that indexing of elements starts from 1 rather than 0, to
    * follow Gmsh convention. Therefore elements are indexed from 1 to
    * getNumberOfElements().
    *
    * @param elementIndex
    * @return
    */
   public int[] getNodeIndices(int elementIndex) {
      return basicMesh.getNodeIndices(elementIndex);
   }

   /**
    * Gets the potential of the node with the given index. Note that indexing of
    * nodes starts from 1 rather than 0, to follow Gmsh convention. Therefore
    * nodes are indexed from 1 to getNumberOfNodes().
    *
    * @param nodeIndex
    * @return
    */
   public double getNodePotential(int nodeIndex) {
      return basicMesh.getNodePotential(nodeIndex);
   }

   /**
    * Gets the number of elements in this mesh.
    *
    * @return int number of elements in this mesh
    */
   public int getNumberOfElements() {
      return basicMesh.getNumberOfElements();
   }

   /**
    * Gets the number of nodes in this mesh.
    *
    * @return int number of nodes in this mesh
    */
   public int getNumberOfNodes() {
      return basicMesh.getNumberOfNodes();
   }

   /**
    * Returns the number of tags associated with the mesh element at the given
    * index. Note that indexing of elements starts from 1 rather than 0, to
    * follow Gmsh convention. Therefore elements are indexed from 1 to
    * getNumberOfElements().
    *
    * @param elementIndex
    * @return
    */
   public int getNumberOfTags(int elementIndex) {
      return basicMesh.getNumberOfTags(elementIndex);
   }

   /**
    * Gets the number of volume elements in this mesh.
    *
    * @return
    */
   public int getNumberOfVolumeElements() {
      return basicMesh.getNumberOfVolumeElements();
   }

   /**
    * Given an array of positions inside the mesh, returns an array of
    * corresponding interpolated potentials. If a position lies outside the mesh
    * the returned value is unpredictable.
    *
    * @param pos
    *           - An N x 3 (double[][]) array of positions
    * @return - double[] An array of corresponding potentials.
    */
   public double[] getPotential(double[][] pos) {
      final int n = pos.length;
      final double[] v = new double[n];
      for (int i = 0; i < n; i++) {
         final double[] p = pos[i];
         if (myShape.contains(p)) {
            final Tetrahedron cE = myShape.containingShape();
            v[i] = cE.getPotential(p);
         }
      }
      return v;
   }

   /**
    * Returns an array of element tags associated with the element at the given
    * index. Note that indexing of elements starts from 1 rather than 0, to
    * follow Gmsh convention. Therefore elements are indexed from 1 to
    * getNumberOfElements().
    *
    * @param elementIndex
    * @return
    */
   public long[] getTags(int elementIndex) {
      return basicMesh.getTags(elementIndex);
   }

   /**
    * Returns the (positive) volume of the element at the provided index or 0 if
    * the index refers to a non-volume element.
    *
    * @param index
    * @return
    */
   public double getVolume(int index) {
      return basicMesh.getVolume(index);
   }

   /**
    * Imports volume element charge state and node potentials from a previously
    * exported file.
    *
    * @param inName
    *           - the previously exported file
    * @throws FileNotFoundException
    */
   public void importChargeAndPotentials(String inName) throws FileNotFoundException {
      final Scanner scanner = new Scanner(new BufferedReader(new FileReader(inName)));
      final Scanner s = scanner.useLocale(Locale.US);

      try {
         String str = s.next();
         if (!str.equals("$NodePotentials"))
            throw new EPQFatalException("1st token of import file was not $NodePotentials");
         final int nn = s.nextInt();
         for (int i = 1; i <= nn; i++)
            setNodePotential(i, s.nextDouble());

         str = s.next();
         if (!str.equals("$VolumeElementCharges"))
            throw new EPQFatalException("Encountered token " + str + " instead of $VolumeElementCharges");
         final int count = s.nextInt();
         /* Initialize all charges to 0 */
         for (int i = 1; i <= getNumberOfElements(); i++)
            setChargeNumber(i, 0);
         /* Update the nonzero ones */
         for (int i = 1; i <= count; i++) {
            final int index = s.nextInt();
            final int nElectrons = s.nextInt();
            setChargeNumber(index, nElectrons);
         }
      } finally {
         if (s != null)
            s.close();
         if (scanner != null)
            scanner.close();
      }

      /* After reading new potentials, update all existing tetrahedra */
      updateAllPotentials();
   }

   /**
    * Increments the charge number in the indicated element by 1. This
    * corresponds, e.g., to a loss of one electron from this volume element.
    *
    * @param index
    *           - The index of the element for which charge is to be
    *           incremented.
    */
   public void incrementChargeNumber(int index) {
      basicMesh.incrementChargeNumber(index);
   }

   /**
    * initializeIfNeeded is mainly useful for meshes that can change after the
    * corresponding Mesh class is instantiated, e.g., if the underlying mesh is
    * adaptive. When called, this method checks the underlying IBasicMesh's
    * revision number. If it is unchanged since the previous call to this
    * method, nothing is done. If it differs, internal caches are cleared or
    * reinitialized.
    *
    * @return - true if the initialization was needed (&amp; performed), false
    *         if not.
    */
   public boolean initializeIfNeeded() {
      final int rev = getMeshRevision();
      if (rev != lastMeshRevision) {
         /* Initialize an array to cache Elements */
         elements = new Element[basicMesh.getNumberOfElements() + 1];
         /*
          * Initialize tables of adjacent tets. Keep a list of faces that have
          * no adjacent tet. These are boundary faces.
          */
         final int[][] bF = basicMesh.getBoundaryFaces();
         boundaryFaces = new Triangle[bF.length];

         for (int i = 0; i < bF.length; i++)
            boundaryFaces[i] = new Triangle(this, bF[i][1], bF[i][0]);
         myShape = new MeshShape(this);

         lastMeshRevision = rev;
         return true;
      }
      return false;
   }

   public boolean isAdaptive() {
      return isAdaptive;
   }

   /**
    * Returns true if the index element is a volume type element (e.g.,
    * tetrahedron), false if not (e.g., a point, line, or surface element)
    *
    * @param elementIndex
    * @return
    */
   public boolean isVolumeType(int elementIndex) {
      return basicMesh.isVolumeType(elementIndex);
   }

   /*
    * Computes the normalized perpendicular of a plane specified by 3 points in
    * order such that the right hand rule gives the direction of the normal.
    */
   private void planePerp(double[] dest, double[] p1, double[] p2, double[] p3) {
      final double[] normal = Math2.normalize(Math2.cross(Math2.minus(p2, p1), Math2.minus(p3, p1)));
      for (int i = 0; i < 3; i++)
         dest[i] = normal[i];
   }

   // See ITransform for JavaDoc
   @Override
   public void rotate(double[] pivot, double phi, double theta, double psi) {
      /* Rotate all nodes of the mesh */
      for (int i = 1; i <= getNumberOfNodes(); i++)
         basicMesh.setNodeCoordinates(i, Transform3D.rotate(getNodeCoordinates(i), pivot, phi, theta, psi));
      /* Force geometry update for all elements that depend on these nodes */
      for (final Triangle t : boundaryFaces)
         t.updateGeom();
      /* Force update or clear other cached values */
      myShape.updateGeom();
      elements = new Element[basicMesh.getNumberOfElements() + 1];
   }

   /**
    * Sets the charge number to the supplied value.
    *
    * @param index
    *           - The index of the element for which charge is to be set.
    * @param n
    *           - The value to which to set the charge, in units of e.
    */
   public void setChargeNumber(int index, int n) {
      basicMesh.setChargeNumber(index, n);
   }

   /**
    * Sets the potential of the node with the given index. Note that indexing of
    * nodes starts from 1 rather than 0, to follow Gmsh convention. Therefore
    * nodes are indexed from 1 to getNumberOfNodes().
    *
    * @param nodeIndex
    *           - list of node indices
    * @param potVal
    *           - the new value of its potential (in volts)
    */
   public void setNodePotential(int nodeIndex, double potVal) {
      basicMesh.setNodePotential(nodeIndex, potVal);
   }

   /**
    * @param nodeIndex
    *           - int[] list of N node indices
    * @param potVal
    *           - double[] list of corresponding N potentials
    */
   public void setNodePotential(int[] nodeIndex, double[] potVal) {
      if (nodeIndex.length != potVal.length)
         throw new EPQFatalException("setNodePotential called with nodeIndex and potVal arrays of unequal length.");
      for (int i = 0; i < nodeIndex.length; i++)
         basicMesh.setNodePotential(nodeIndex[i], potVal[i]);
   }

   /**
    * Returns an array of 3 node indices corresponding the the face with the
    * given index. The indices are indexed in the order such that (n2-n1) x
    * (n3-n1) points out of the tetrahedron.
    *
    * @param faceIndex
    * @return
    */
   private int[] tetFaceNodeIndices(int tetIndex, int faceIndex) {
      return basicMesh.tetFaceNodeIndices(tetIndex, faceIndex);
   }

   // See ITransform for JavaDoc
   @Override
   public void translate(double[] distance) {
      /* translate all nodes of the mesh */
      for (int i = 1; i <= getNumberOfNodes(); i++)
         basicMesh.setNodeCoordinates(i, Transform3D.translate(getNodeCoordinates(i), distance, false));
      /* Force geometry update for all elements that depend on these nodes */
      for (final Triangle t : boundaryFaces)
         t.updateGeom();
      /* Force update or clear other cached values */
      myShape.updateGeom();
      elements = new Element[basicMesh.getNumberOfElements() + 1];
   }

   /**
    * Calls the updatePotential() method for all cached volume elements known to
    * this class, forcing the interior potential and electric field for each to
    * be recomputed. Note that if the clearElementsCache() has been used to
    * clear the cache, any remaining instances of volume elements that had been
    * in the cache will NOT be updated by this call.
    */
   public void updateAllPotentials() {
      final int n = getNumberOfElements();
      for (int index = 1; index <= n; index++)
         if ((elements[index] != null) && (getElementType(index) == 4))
            ((Tetrahedron) elements[index]).updatePotentials();
   }

   /**
    * If the indexed mesh element exists (i.e., is already instantiated and
    * cached) and is a volume-type element, its updatePotentials() method is
    * called.
    *
    * @param index
    */
   public void updateElementPotentialsIfExists(int index) {
      if ((elements[index] != null) && (basicMesh.getElementType(index) == 4)) {
         final Tetrahedron tet = (Tetrahedron) elements[index];
         tet.updatePotentials();
      }
   }

   /**
    * Writes a charge density and electrostatic potential map to a file. The map
    * consists of charge density (in Coulombs per cubic meter) and potential (in
    * Volts) values on a grid of points. The grid consists of the points p0 +
    * i*deltax + j*deltay. In this equation, p0, deltax, and deltay should each
    * be understood as 3-component vectors. The indices i and j form a raster
    * with i varied from 0 to nx-1, j from 0 to ny-1. p0, deltax, and deltay are
    * vectors describing the first point in the grid, the offset between
    * neighboring points in the "x" direction, and the offset in the "y"
    * direction. (Because deltax and deltay are vectors, the x and y directions
    * in the map can correspond to any desired directions in the 3-D coordinate
    * system of the simulation.) The output file is a text file with p0, deltax,
    * deltay, nx, and ny forming the first 5 lines. The remaining lines are two
    * values--charge density and potential in that order-- per line in raster
    * order, i values (the "x" index) varying most rapidly.
    *
    * @param p0
    *           - double[] A 3-vector representing the starting postion (1st
    *           sample) of the charge &amp; potential map
    * @param deltax
    *           - double[] A 3-vector representing the increment between points
    *           within a line of points along the "x" direction.
    * @param deltay-
    *           double[] A 3-vector representing the increment between lines of
    *           points along the "y" direction.
    * @param nx
    *           - the number of x increments
    * @param ny
    *           - the number of y increments
    * @param outName
    *           - name (may include full path) of the file for output
    * @throws IOException
    */
   public void writeChargeAndPotentialMap(double[] p0, double[] deltax, double[] deltay, int nx, int ny, String outName) throws IOException {
      BufferedWriter out = null;
      try {
         out = new BufferedWriter(new FileWriter(outName));

         for (int i = 0; i < 3; i++)
            out.write(Double.toString(p0[i]) + " ");
         out.newLine();

         for (int i = 0; i < 3; i++)
            out.write(Double.toString(deltax[i]) + " ");
         out.newLine();

         for (int i = 0; i < 3; i++)
            out.write(Double.toString(deltay[i]) + " ");
         out.newLine();

         out.write(Integer.toString(nx));
         out.newLine();
         out.write(Integer.toString(ny));
         out.newLine();

         double[] p;
         Tetrahedron nearestTet = myShape.insideTet;
         double chargeDensity;
         double potential;
         for (int j = 0; j < ny; j++)
            for (int i = 0; i < nx; i++) {
               p = Math2.plus(p0, Math2.plus(Math2.multiply(i, deltax), Math2.multiply(j, deltay)));
               final boolean contains = myShape.contains(p, null, nearestTet);
               if (contains) {
                  nearestTet = myShape.containingShape();
                  chargeDensity = nearestTet.getChargeDensity();
                  potential = nearestTet.getPotential(p);
               } else {
                  chargeDensity = 0.;
                  potential = 0.;
               }
               out.write(Double.toString(chargeDensity) + " " + Double.toString(potential));
               out.newLine();
            }
      } catch (final IOException e) {
         throw new EPQFatalException("Error opening or writing " + outName + ".");
      } finally {
         if (out != null)
            out.close();
      }
   }
}
