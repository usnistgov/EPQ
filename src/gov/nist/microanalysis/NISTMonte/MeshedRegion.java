/**
 * gov.nist.nanoscalemetrology.JMONSEL.MeshedRegion Created by: John Villarrubia
 * Date: Oct 26, 2010
 */
package gov.nist.microanalysis.NISTMonte;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ITransform;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.Region;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.TransformableRegion;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.nanoscalemetrology.JMONSEL.Mesh;
import gov.nist.nanoscalemetrology.JMONSEL.Mesh.Tetrahedron;
import gov.nist.nanoscalemetrology.JMONSEL.SEmaterial;
import gov.nist.nanoscalemetrology.JMONSELutils.NULagrangeInterpolation;
import gov.nist.nanoscalemetrology.JMONSELutils.ULagrangeInterpolation;

/**
 * <p>
 * Implements a region backed by a mesh. A MeshedRegion is a region that is
 * subdivided into MeshElementRegion subregions such that the subregions tile
 * the MeshedRegion. Thus, any point inside the MeshedRegion is also inside one
 * of its MeshElementRegion subregions. The description of these subregions is
 * contained in a Mesh object, which in turn comes from a mesh data file.
 * Currently only mesh files produced by Gmsh are supported. For Gmsh, the
 * MeshElementRegion subregions all have tetrahedral shape.
 * </p>
 * <p>
 * Since the MeshedRegion may contain subregions with different materials, there
 * is a mechanism to assign them. When the mesh file is generated, Gmsh assigns
 * integer "tags" to mesh elements. The user should take care to assign distinct
 * tags to elements comprised of different materials, e.g., tag=1 for material1,
 * tag = 2 for material 2, etc. Then when the MeshedRegion is constructed, the
 * material assignment is made by providing the constructor with a
 * HashMap&lt;Long,IMaterialScatterModel&gt; that associates materials with
 * tags. Note that it is permissible to map more than one tag to the same
 * material. This might be done, e.g., if it is necessary to distinguish for
 * some other purpose (e.g., different potential constraints) different mesh
 * elements with the same material. However, regions with different materials
 * may not share the same tag.
 * </p>
 * <p>
 * Some surfaces or volumes within the mesh may be constrained by the user to
 * different potentials. These are specified by the user via an array of
 * IConstraint objects. The constrained regions may be either surfaces (e.g.,
 * all or part of the exterior boundary of the mesh) or volumes (e.g.,
 * conducting regions that are grounded or otherwise held at some potential).
 * Constraints are applied to the nodes of the designated regions. The user
 * should take care not to specify conflicting constraints. E.g., Volumes or
 * surfaces in contact cannot be constrained to different potentials. JMONSEL
 * attempts to check for conflicts and will terminate with a fatal exception if
 * it finds any. Any that it fails to find will produce an error and
 * corresponding error message in the finite element solver.
 * </p>
 * <p>
 * A mechanism (RCConnection) is provided whereby floating regions may be
 * connected electrically to each other or to regions held at fixed potentials.
 * The electrical connection is treated as an RC connection with user-specified
 * relaxation time, which may be constant or voltage-dependent. See the
 * documentation for RCConnection and the various kinds of constraints for
 * further explanation. Any such connections are specified by constructing a
 * MeshedRegion and subsequently passing it an array of RCConnections via the
 * setConnectionList() method.
 * </p>
 * <p>
 * A MeshedRegion is logically similar to a Region with a single generation of
 * subregions, but there are differences in practice that may sometimes be
 * noticed. To determine the material associated with any point inside the
 * MeshedRegion, use the getMaterial() method of the subregion that contains the
 * point. Since all points within the MeshedRegion are also within a subregion,
 * there is no Material associated with the MeshedRegion per se. Its
 * getMaterial() and getScatterModel methods return null. Regions ordinarily
 * have only a few subregions, whereas it is not uncommon for a Mesh to have a
 * million elements or more. Consequently, to save memory MeshedRegion uses a
 * lazy instantiation of its subregions. They are generated only as needed.
 * Regions deep within a material that are never visited by electrons may never
 * be instantiated. For this reason, getSubRegions() returns an empty list, but
 * containingSubRegion() and findEndOfStep() instantiate and return the desired
 * subregion.
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

public class MeshedRegion
   extends TransformableRegion {

   /**
    * <p>
    * DirichletConstraint associates a fixed potential value with a mesh tag.
    * The tag may refer to volume elements or surface elements of the mesh.
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
   public static class DirichletConstraint
      implements IConstraint {
      private final long associatedRegionTag;
      private double potential;
      private final int hash;
      private final Mesh mesh;

      /**
       * Constructs a DirichletConstraint.
       * 
       * @param associatedRegionTag -- The tag of the surface or volume elements
       *           associated with this constraint.
       * @param potential -- The value of potential to which the tagged elements
       *           should be constrained.
       */
      public DirichletConstraint(Mesh mesh, long associatedRegionTag, double potential) {
         super();
         this.mesh = mesh;
         this.associatedRegionTag = associatedRegionTag;
         this.potential = potential;
         /*
          * Note that the hash definition below is not a function of the
          * assigned potential. This allows us to change the potential without
          * changing the hash. It means that 2 constraints assigned to the same
          * region will not have different hashes, but this should be OK because
          * that should never happen: it doesn't make sense to constrain the
          * same region to two different values.
          */
         hash = mesh.hashCode();
      }

      /**
       * A Dirichlet constraint is deemed equal to obj if obj is a
       * DirichletConstraint for the same mesh and has the same assigned
       * potential. We don't require the region to be the same. This permits a
       * node on the boundary of the region to have both regions subject to
       * constraint, provided the potentials are the same.
       * 
       * @param obj
       * @return boolean
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if(obj.getClass() != this.getClass())
            return false;
         final DirichletConstraint cobj = (DirichletConstraint) obj;

         if((cobj.getPotential() != potential) || (cobj.getMesh() != mesh))
            return false;

         return true;
      }

      /**
       * Gets the current value assigned to regionTag
       * 
       * @return Returns the regionTag.
       */
      @Override
      public long getAssociatedRegionTag() {
         return associatedRegionTag;
      }

      /**
       * Gets the associated mesh.
       * 
       * @return Mesh
       * @see gov.nist.microanalysis.NISTMonte.MeshedRegion.IConstraint#getMesh()
       */
      @Override
      public Mesh getMesh() {
         return mesh;
      }

      /**
       * Gets the current value assigned to potential
       * 
       * @return Returns the potential.
       */
      public double getPotential() {
         return potential;
      }

      @Override
      public int hashCode() {
         return hash;
      }

      /**
       * Sets the current value assigned to potential
       * 
       * @param newPotential
       */
      public void setPotential(double newPotential) {
         potential = newPotential;
      }

   }
   /**
    * <p>
    * FloatingConstraint associates a "floating conductor" condition with a
    * closed surface. All nodes on and within the surface will be assigned the
    * same a priori unknown potential. (The value of this potential will be part
    * of the FEA solution.) The value associated with this constraint (analogous
    * to the potential for a Dirichlet constraint or normal E field for Neumann
    * constraint) would ordinarily be the charge enclosed by the surface.
    * However, since JMONSEL keeps track of charges, FloatingConstraint's value
    * is instead the tag of the volume enclosed by the surface. JMONSEL then
    * itself takes care of counting charges within this volume.
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
   public static class FloatingConstraint
      implements IConstraint {
      private final long associatedRegionTag;
      private final long volumeTag;
      private final int hash;
      private final Mesh mesh;
      private int charge = 0; // charge in the contained volume
      /*
       * Following is a list of all volume elements subject to this constraint
       */
      private final ArrayList<Integer> volumeElements = new ArrayList<Integer>();
      private final int aSurfaceNode;
      private final int aVolumeElement;

      /**
       * Constructs a FloatingConstraint
       * 
       * @param mesh
       * @param associatedRegionTag
       * @param volumeTag
       */
      public FloatingConstraint(Mesh mesh, long associatedRegionTag, long volumeTag) {
         super();
         this.mesh = mesh;
         this.associatedRegionTag = associatedRegionTag;
         this.volumeTag = volumeTag;
         final int n = mesh.getNumberOfElements();
         int surfaceElement = 0;
         for(int j = 1; j <= n; j++) {
            if(mesh.getTags(j)[0] == volumeTag)
               volumeElements.add(j);
            if(mesh.getTags(j)[0] == associatedRegionTag)
               surfaceElement = j;
         }
         aSurfaceNode = mesh.getNodeIndices(surfaceElement)[0];
         aVolumeElement = volumeElements.get(0);
         hash = mesh.hashCode() + ((Long.valueOf(associatedRegionTag)).toString() + (Long.valueOf(volumeTag)).toString()).hashCode();
      }

      /**
       * <p>
       * Polls all mesh elements in the floating volume to determine the total
       * charge. The total is saved internally, and must be accessed by a
       * subsequent call to getCharge().
       * </p>
       * <p>
       * Determining the total charge requires polling, which can be
       * time-consuming. For this reason, FloatingConstraint maintains redundant
       * records of the charge in the contained volume. The individual mesh
       * elements of the volume each contain their charges. To determine the
       * total charge, these elements must be polled and the charges summed.
       * This computeCharge() method performs this poll. The result of the poll
       * is stored internally, and is accessed by the getCharge() method. Thus,
       * the usual procedure to determine charge is to call computeCharge(0)
       * followed by getCharge(). In routines where it is known that no
       * additional scattering has been run, the previous total can be accessed
       * via getCharge() without performing the time-consuming poll.
       * </p>
       */
      public void computeCharge() {
         int charge = 0;
         for(final int j : volumeElements)
            if(mesh.getTags(j)[0] == volumeTag)
               charge += mesh.getChargeNumber(j);
         this.charge = charge;
      }

      /**
       * A FloatingConstraint is equal to obj if obj is a Constraint of the same
       * type, has the same associated region, and the same associated
       * volumeTag.
       * 
       * @param obj
       * @return boolean
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if(obj.getClass() != this.getClass())
            return false;
         final FloatingConstraint cobj = (FloatingConstraint) obj;

         if((cobj.getAssociatedRegionTag() != associatedRegionTag) || (cobj.getVolumeTag() != volumeTag)
               || (cobj.getMesh() != mesh))
            return false;

         return true;
      }

      /**
       * Gets the current value assigned to associatedRegionTag
       * 
       * @return Returns the associatedRegionTag.
       */
      @Override
      public long getAssociatedRegionTag() {
         return associatedRegionTag;
      }

      /**
       * @return -- the total charge contained inside the floating volume at the
       *         time the last computeCharge() was run.
       */
      public int getCharge() {
         return charge;
      }

      /**
       * Gets the associated mesh.
       * 
       * @return Mesh
       * @see gov.nist.microanalysis.NISTMonte.MeshedRegion.IConstraint#getMesh()
       */
      @Override
      public Mesh getMesh() {
         return mesh;
      }

      /**
       * Gets the current potential of this constraint's floating region.
       * 
       * @return double
       */
      public double getPotential() {
         return mesh.getNodePotential(aSurfaceNode);
      }

      /**
       * Gets the current value assigned to volumeTag
       * 
       * @return Returns the volumeTag.
       */
      public long getVolumeTag() {
         return volumeTag;
      }

      @Override
      public int hashCode() {
         return hash;
      }

      /**
       * Increases the value of the charge stored internally by the given value.
       * 
       * @param chargeIncrement
       */
      public void incrementCharge(int chargeIncrement) {
         /*
          * See note in the setCharge method about why it's necessary to place
          * new charges into the mesh.
          */
         mesh.setChargeNumber(aVolumeElement, mesh.getChargeNumber(aVolumeElement) + chargeIncrement);
         this.charge += chargeIncrement;
      }

      /**
       * Sets the value of the charge stored internally to be the given value
       * plus any change since the last time computeCharge() was called. If
       * computeCharge() is called first, the value will correspond to the total
       * charge. (This call does NOT force a poll of all volume elements.)
       * 
       * @param charge
       */
      public void setCharge(int charge) {
         /*
          * Additional charges are also placed into an element of the floating
          * volume. This is necessary in order to keep the two records of charge
          * consistent with each other, also so that methods that log the charge
          * in the mesh will have the correct charge. For FEA, only the total
          * charge matters, so we can put the new charge anywhere.
          */
         mesh.setChargeNumber(aVolumeElement, (mesh.getChargeNumber(aVolumeElement) + charge) - this.charge);
         this.charge = charge;
      }

      /**
       * sets the potential at all nodes in this FloatingConstraint's associated
       * volume to the potential of its surface. A conductor is an
       * equipotential, but some finite element analyses may take advantage of
       * this to omit interior points from the system of equations. I.e., these
       * FEA simply set a boundary condition on the surface. In such a case, the
       * FEA resolution will not explicitly specify the potentials of
       * non-surface nodes. In such cases, this setVolumePotential() method can
       * be used instead to make the interior node potentials consistent with
       * the surface potential. Naturally, this should be called after the FEA
       * solution has determined the correct surface potential.
       */
      public void setVolumePotential() {
         final double v = getPotential();
         for(final Integer i : volumeElements) {
            final int[] nodes = mesh.getNodeIndices(i);
            for(final int node : nodes)
               if(mesh.getNodePotential(node) != v)
                  mesh.setNodePotential(node, v);
         }
      }
   }
   /**
    * <p>
    * An interface for finite element analysis constraints. Each constraint
    * associates a meshed region (indicated by a tag) with a value and provides
    * getters for both. The region getter is specified by this interface. The
    * value may differ in type depending on the constraint type, so the
    * interface does not specify its name.
    * </p>
    * <p>
    * Implementing classes should override the default equals() method so that
    * constraints of the same type with the same constructor arguments are equal
    * to one another, even if they are different instances. hashCode() should
    * likewise be overridden to maintain consistentcy with equals().
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
   public interface IConstraint {

      /**
       * Returns the tag of the region associated with this constraint.
       * 
       * @return -- mesh tag of associated region.
       */
      public long getAssociatedRegionTag();

      /**
       * Returns the mesh for which this is a constraint
       * 
       * @return Mesh
       */
      public Mesh getMesh();

   }
   /**
    * <p>
    * NeumannConstraint associates an externally imposed electric flux with a
    * mesh tag. The tag should refer to surface elements of the mesh. The flux
    * is specified by normalE, a perpendicular component of electric field. That
    * is, the flux is normalE*(area of the specified surface) and normalE = E.n
    * where E is the field, n the normal vector at the surface, and the dot
    * represents the inner product.
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
   public static class NeumannConstraint
      implements IConstraint {
      private final long associatedRegionTag;
      private final double normalE;
      private final int hash;
      private final Mesh mesh;

      /**
       * Constructs a Neumann Constraint.
       * 
       * @param associatedRegionTag -- The tag of the surface elements
       *           associated with this constraint.
       * @param normalE -- The value of normal component of E field to which the
       *           tagged elements should be constrained. normalE = E.n, in
       *           volts/meter, where E is the field, n is the normal vector at
       *           the surface and the dot represents the inner product.
       */
      public NeumannConstraint(Mesh mesh, long associatedRegionTag, double normalE) {
         super();
         this.mesh = mesh;
         this.associatedRegionTag = associatedRegionTag;
         this.normalE = normalE;
         hash = mesh.hashCode() + ((Long.valueOf(associatedRegionTag)).toString() + (Double.valueOf(normalE)).toString()).hashCode();
      }

      /**
       * A NeumannConstraint is equal to obj if obj is a Constraint of the same
       * type, has the same associated region, and the same normalE.
       * 
       * @param obj
       * @return boolean
       * @see java.lang.Object#equals(java.lang.Object)
       */
      @Override
      public boolean equals(Object obj) {
         if(obj.getClass() != this.getClass())
            return false;
         final NeumannConstraint cobj = (NeumannConstraint) obj;

         if((cobj.getAssociatedRegionTag() != associatedRegionTag) || (cobj.getNormalE() != normalE)
               || (cobj.getMesh() != mesh))
            return false;

         return true;
      }

      /**
       * Gets the current value assigned to associatedRegionTag
       * 
       * @return Returns the regionTag.
       */
      @Override
      public long getAssociatedRegionTag() {
         return associatedRegionTag;
      }

      /**
       * Gets the associated mesh.
       * 
       * @return Mesh
       * @see gov.nist.microanalysis.NISTMonte.MeshedRegion.IConstraint#getMesh()
       */
      @Override
      public Mesh getMesh() {
         return mesh;
      }

      /**
       * Gets the current value assigned to the normal component of E field.
       * 
       * @return Returns the potential.
       */
      public double getNormalE() {
         return normalE;
      }

      @Override
      public int hashCode() {
         return hash;
      }

   }
   /**
    * <p>
    * RCConnection specifies characteristics of an electrical connection between
    * two regions. The connection is characterized by an RC-like relaxation
    * time. This relaxation time may be constant or voltage-dependent. This
    * class specifies the identities of the connected regions and the relaxation
    * time. It provides a method to determine the voltage across the connection
    * at the most recent FEA and a method to move a specified amount of charge
    * across the connection. It does not decide how much charge to move or
    * actually move any until told to do so.
    * </p>
    * <p>
    * The constructors all take two integer indices to specify the regions that
    * are connected. These indices point into this MeshedRegion's
    * constraintList. I.e. if the indices are 1 and 2, this means the
    * RCConnection describes an electrical connection between the regions
    * associated with constraintList[1] and constraintList[2]. At least one of
    * these constraints must be a FloatingConstraint. The other may be either a
    * FloatingConstraint or a DirichletConstraint.
    * </p>
    * <p>
    * One of these regions is designated the "source" and the other the "drain."
    * The voltage across the connection is defined as Vsource - Vdrain and the
    * charge on the connection Qsource - Qdrain. To increase the charge by
    * moving charge q therefore means to decrease the charge on the drain by q
    * and increase it on the source by q. Since voltages and charges may be
    * either positive or negative, choice of source or drain does not limit
    * generality. It merely establishes a sign convention.
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
   public class RCConnection {
      private final int sourceIndex;
      private final int drainIndex;
      private int numFloats;
      private final double vMin;
      private final double vMax;
      private final int tauMaxIndex;
      private final double[] tauArray;
      private double[] vArray;
      private double deltaV;
      private int interpolationOrder = 3;
      private boolean uniform = true;

      /**
       * Constructs a RCConnection with constant relaxation time
       * 
       * @param sourceIndex - index (into the constraintList) of the
       *           FloatingConstraint or DirichletConstraint that is the source
       *           for this connection.
       * @param drainIndex - index (into the constraintList) of the
       *           FloatingConstraint or DirichletConstraint that is the drain
       *           for this connection.
       * @param tau - the relaxation time in seconds.
       */
      public RCConnection(int sourceIndex, int drainIndex, double tau) {
         /*
          * We treat this as a special case of uniformly spaced array, in which
          * there is only a single value in the table.
          */
         this(sourceIndex, drainIndex, 0., 0., new double[] {
            tau
         });
      }

      /**
       * Constructs a RCConnection with voltage-dependent relaxation time
       * interpolated from an array of equally spaced values. That is, the
       * relaxation time vs. voltage dependence is interpolated from the set of
       * discrete points (V[i],tauArray[i]) with V[i] = V0 + i*deltaV. For V
       * &lt; V0, tauArray[0] is used. For V&gt;V0+(n-1)*deltaV, where n is the
       * length of tauArray, tauArray[n-1] is used.
       * 
       * @param sourceIndex - index (into the constraintList) of the
       *           FloatingConstraint or DirichletConstraint that is the source
       *           for this connection.
       * @param drainIndex - index (into the constraintList) of the
       *           FloatingConstraint or DirichletConstraint that is the drain
       *           for this connection.
       * @param V0 - the voltage that corresponds to relaxation time tauArray[0]
       * @param deltaV - the constant voltage interval between provided tauArray
       *           values.
       * @param tauArray - a 1-d array of tau values such that tauArray[i]
       *           corresponds to voltage V0+i*deltaV
       */
      public RCConnection(int sourceIndex, int drainIndex, double V0, double deltaV, double[] tauArray) {
         this.sourceIndex = sourceIndex;
         this.drainIndex = drainIndex;
         this.deltaV = deltaV;
         vMin = V0;
         tauMaxIndex = tauArray.length - 1;
         if(tauMaxIndex < 3)
            interpolationOrder = tauMaxIndex;
         vMax = V0 + (tauMaxIndex * deltaV);
         this.tauArray = tauArray;
         checkConstraints();

      }

      /**
       * Constructs a RCConnection with voltage-dependent relaxation time
       * interpolated from a nonuniformly spaced array of values. That is, the
       * relaxation time vs. voltage dependence is interpolated from the set of
       * discrete points (tauArray[0,i],tauArray[1,i]).
       * 
       * @param sourceIndex - index (into the constraintList) of the
       *           FloatingConstraint or DirichletConstraint that is the source
       *           for this connection.
       * @param drainIndex - index (into the constraintList) of the
       *           FloatingConstraint or DirichletConstraint that is the drain
       *           for this connection.
       * @param tauArray - a double[2][n] array of voltage and tau values such
       *           that tauArray[0] is a list of increasing voltages and
       *           tauArray[1] the corresponding tau values.
       */
      public RCConnection(int sourceIndex, int drainIndex, double[][] tauArray) {
         this.sourceIndex = sourceIndex;
         this.drainIndex = drainIndex;
         if(tauArray.length != 2)
            throw new EPQFatalException("tauArray 1st dimension must = 2");
         this.vArray = tauArray[0];
         tauMaxIndex = vArray.length - 1;
         if(tauMaxIndex < 3)
            interpolationOrder = tauMaxIndex;
         /* Make sure tauArray is increasing */
         vMin = vArray[0];
         for(int i = 1; i <= tauMaxIndex; i++)
            if(vArray[i] <= vArray[i - 1])
               throw new EPQFatalException("tauArray[0] must be increasing.");
         vMax = vArray[tauMaxIndex];
         this.tauArray = tauArray[1];
         uniform = false;
         checkConstraints();
      }

      private void checkConstraints() {
         numFloats = 0;
         if(constraintList[sourceIndex] instanceof FloatingConstraint)
            numFloats++;
         else if(!(constraintList[sourceIndex] instanceof DirichletConstraint))
            throw new EPQFatalException("constraintList[sourceIndex = " + sourceIndex
                  + "] is neither a Floating nor a Dirichlet Constraint.");
         if(constraintList[drainIndex] instanceof FloatingConstraint)
            numFloats++;
         else if(!(constraintList[drainIndex] instanceof DirichletConstraint))
            throw new EPQFatalException("constraintList[drainIndex = " + drainIndex
                  + "] is neither a Floating nor a Dirichlet Constraint.");
         if(numFloats < 1)
            throw new EPQFatalException("At least one of constraintList[sourceIndex = " + sourceIndex
                  + "] or constraintList[drainIndex = " + drainIndex + "] must be a FloatingConstraint.");

         if(constraintList[sourceIndex].equals(constraintList[drainIndex]))
            throw new EPQFatalException("Source and Drain constraints may not be the same.");
      }

      /**
       * Moves charge equal to q out of the drain into the source. q may be
       * positive or negative and is in units of +e. Only whole number of
       * charges are actually moved. A fractional part, f (0&lt;f&lt;1) is
       * handled by choosing a random number 0&lt;=r&lt;=1 and moving 1 charge
       * if r&lt;f. If the nonfractional part is 0, it is possible, depending
       * upon the random number chosen, that no charge will actually flow. In
       * this case the method returns false. Otherwise it returns true.
       * 
       * @param q
       * @return - true if charge actually flows, false if not.
       */
      public boolean flowCharge(double q) {
         int charge = (int) Math.floor(q);
         final double fractionalCharge = q - charge;
         /* So fractionalCharge>0 and charge + fractionalCharge = q */
         if(Math2.rgen.nextDouble() < fractionalCharge)
            charge++;
         if(charge != 0) {
            if(constraintList[sourceIndex] instanceof FloatingConstraint)
               ((FloatingConstraint) constraintList[sourceIndex]).incrementCharge(charge);
            if(constraintList[drainIndex] instanceof FloatingConstraint)
               ((FloatingConstraint) constraintList[drainIndex]).incrementCharge(-charge);
            return true;
         }
         return false;
      }

      /**
       * Gets the voltage drop across this connection (source voltage - drain
       * voltage)
       * 
       * @return double
       */
      public double getDeltaV() {
         double sourceV;
         double drainV;

         if(constraintList[sourceIndex] instanceof FloatingConstraint)
            sourceV = ((FloatingConstraint) constraintList[sourceIndex]).getPotential();
         else
            sourceV = ((DirichletConstraint) constraintList[sourceIndex]).getPotential();

         if(constraintList[drainIndex] instanceof FloatingConstraint)
            drainV = ((FloatingConstraint) constraintList[drainIndex]).getPotential();
         else
            drainV = ((DirichletConstraint) constraintList[drainIndex]).getPotential();

         return sourceV - drainV;
      }

      /**
       * Returns the drain Constraint for this connection.
       * 
       * @return - the drain Constraint
       */
      public IConstraint getDrainConstraint() {
         return constraintList[drainIndex];
      }

      /**
       * Gets the current value assigned to drainIndex
       * 
       * @return Returns the drainIndex.
       */
      public int getDrainIndex() {
         return drainIndex;
      }

      /**
       * Returns the number of FloatingConstraints in this connection, either 1
       * or 2.
       * 
       * @return Returns the numFloats.
       */
      public int getNumFloats() {
         return numFloats;
      }

      /**
       * Returns the source Constraint for this connection.
       * 
       * @return - the source Constraint
       */
      public IConstraint getSourceConstraint() {
         return constraintList[sourceIndex];
      }

      /**
       * Gets the current value assigned to sourceIndex
       * 
       * @return Returns the sourceIndex.
       */
      public int getSourceIndex() {
         return sourceIndex;
      }

      /**
       * Gets the value of relaxation time at the given voltage drop, v, across
       * this connection.
       * 
       * @param v
       * @return double
       */
      public double getTau(double v) {
         if(v <= vMin)
            return tauArray[0];
         if(v >= vMax)
            return tauArray[tauMaxIndex];
         if(uniform)
            return ULagrangeInterpolation.d1(tauArray, vMin, deltaV, interpolationOrder, v)[0];
         else
            return NULagrangeInterpolation.d1(tauArray, vArray, interpolationOrder, v)[0];
      }

   }

   private final HashMap<Long, IMaterialScatterModel> msmMap;

   private final IConstraint[] constraintList;

   private final ArrayList<RCConnection> connectionList = new ArrayList<RCConnection>();

   private final HashMap<Long, IConstraint> constraintMap = new HashMap<Long, IConstraint>();

   private final Mesh mesh;

   private final HashSet<Long> materialTags = new HashSet<Long>();

   /*
    * The constraint map maps element tags to potentials. nodeConstraints
    * complements this by mapping node index to constraint.
    */
   private final HashMap<Integer, IConstraint> nodeConstraints = new HashMap<Integer, IConstraint>();

   final double DENSITY_THRESHOLD = 100.; // Min density to support charge

   /**
    * A (possibly temporary) constructor without strongly typed Collection
    * inputs for the benefit of Mathematica, which does not seem equipped to
    * deal with them.
    * 
    * @param parent
    * @param mesh
    * @param map
    * @param cList
    */
   public MeshedRegion(Region parent, Mesh mesh, @SuppressWarnings("rawtypes") HashMap map, @SuppressWarnings("rawtypes") ArrayList cList) {
      // public MeshedRegion(Region parent, Mesh mesh, HashMap<Long,
      // IMaterialScatterModel> msmMap, IConstraint[] constraintList) {
      final HashMap<Long, IMaterialScatterModel> msmMap = new HashMap<Long, IMaterialScatterModel>();
      final IConstraint[] constraintList = new IConstraint[cList.size()];

      int i = 0;
      for(final Object c : cList)
         constraintList[i++] = (IConstraint) c;

      @SuppressWarnings("rawtypes")
      final Set ks = map.keySet();
      for(final Object k : ks)
         msmMap.put((Long) k, (IMaterialScatterModel) map.get(k));

      mParent = parent;
      this.msmMap = msmMap;
      this.constraintList = constraintList;

      /* Load the constraint map */
      for(final IConstraint c : constraintList)
         constraintMap.put(c.getAssociatedRegionTag(), c);

      this.mesh = mesh;
      mScatterModel = null;
      mShape = mesh.getMeshShape();
      if(mParent != null)
         mParent.mSubRegions.add(this);
      /* Find the set of unique tags in the mesh. */
      for(i = 1; i <= mesh.getNumberOfElements(); i++)
         /*
          * Gmsh volume elements have mesh types in the range 4-7, 11-14, 17-19,
          * or 29-31. Presently the Mesh class only implements the first order
          * tetrahedron (type 4), but we check for all volume-element types in
          * case this gets extended.
          */
         if(mesh.isVolumeType(i))
            materialTags.add(mesh.getTags(i)[0]); // 1st tag, tag[0], is
                                                  // associated with material
      /* Make sure the msmMap has a Material for each tag */
      for(final Long tag : materialTags)
         if(!(msmMap.containsKey(tag)))
            throw new EPQFatalException("Mesh contains material tag " + tag
                  + " for which no corresponding IMaterialScatterModel mapping was provided.");

      /* Make a list of the constrained nodes */
      for(i = 1; i <= mesh.getNumberOfElements(); i++) {
         final IConstraint c = constraintMap.get(mesh.getTags(i)[0]); // constraint
                                                                      // v
         // if there is
         // one
         if(c != null) {
            // Register constraints for all nodes of this element
            final int[] nodeIndices = mesh.getNodeIndices(i);
            for(final int index : nodeIndices) {
               final IConstraint existingVal = nodeConstraints.get(index);
               if((existingVal != null) && !existingVal.equals(c))
                  // Conflicting constraints
                  throw new EPQFatalException("Conflicting constraints were specified for node # " + Integer.toString(index));
               // No conflict: add constraint
               nodeConstraints.put(index, c);
            }
         }
      }
   }

   /**
    * Constructs a MeshedRegion
    * 
    * @param parent
    * @param msmMap
    * @param constraintList
    * @param mesh
    */
   public MeshedRegion(Region parent, Mesh mesh, HashMap<Long, IMaterialScatterModel> msmMap, IConstraint[] constraintList) {
      mParent = parent;
      this.msmMap = msmMap;
      this.constraintList = constraintList;

      /* Load the constraint map */
      for(final IConstraint c : constraintList)
         constraintMap.put(c.getAssociatedRegionTag(), c);

      this.mesh = mesh;
      mScatterModel = null;
      mShape = mesh.getMeshShape();
      if(mParent != null)
         mParent.mSubRegions.add(this);
      /* Find the set of unique tags in the mesh. */
      final int nel = mesh.getNumberOfElements();
      for(int i = 1; i <= nel; i++)
         /*
          * Gmsh volume elements have mesh types in the range 4-7, 11-14, 17-19,
          * or 29-31. Presently the Mesh class only implements the first order
          * tetrahedron (type 4), but we check for all volume-element types in
          * case this gets extended.
          */
         if(mesh.isVolumeType(i))
            materialTags.add(mesh.getTags(i)[0]); // 1st tag, tag[0], is
                                                  // associated with material
      /* Make sure the msmMap has a Material for each tag */
      for(final Long tag : materialTags)
         if(!(msmMap.containsKey(tag)))
            throw new EPQFatalException("Mesh contains material tag " + tag
                  + " for which no corresponding IMaterialScatterModel mapping was provided.");

      /* Make a list of the constrained nodes */
      for(int i = 1; i <= mesh.getNumberOfElements(); i++) {
         final IConstraint c = constraintMap.get(mesh.getTags(i)[0]); // constraint
                                                                      // v
         // if there is
         // one
         if(c != null) {
            // Register constraints for all nodes of this element
            final int[] nodeIndices = mesh.getNodeIndices(i);
            for(final int index : nodeIndices) {
               final IConstraint existingVal = nodeConstraints.get(index);
               /*
                * Our definition of conflicting constraints here may be more
                * strict than necessary. We require constraints to be equal.
                * This always means they are the same type (both Dirichlet,
                * Neumann, or Floating) with further requirements determined by
                * the equals implementation of each type. We might be able to
                * permit Neumann constraints to be paired with Dirichlet or
                * Floating, but the need has not arisen so this is not presently
                * implemented.
                */
               if((existingVal != null) && !existingVal.equals(c))
                  // Conflicting constraints
                  throw new EPQFatalException("Conflicting constraints were specified for node # " + Integer.toString(index));
               // No conflict: add constraint
               nodeConstraints.put(index, c);
            }
         }
      }
   }

   /**
    * Constructs a MeshedRegion
    * 
    * @param parent
    * @param msmMap -
    * @param meshFileName - Name of mesh file from which to read the associated
    *           mesh
    * @throws FileNotFoundException
    */
   public MeshedRegion(Region parent, String meshFileName, HashMap<Long, IMaterialScatterModel> msmMap, IConstraint[] constraintList)
         throws FileNotFoundException {
      this(parent, new Mesh(meshFileName), msmMap, constraintList);
   }

   /**
    * Connects two constrained regions with the specified RC time constant. At
    * least one of the regions must be subject to a FloatingConstraint. The
    * other may be either another FloatingConstraint or a DirichletConstraint.
    * 
    * @param sourceIndex - an index into this MeshedRegion's constraintList that
    *           designates the source region.
    * @param drainIndex - an index into this MeshedRegion's constraintList that
    *           designates the drain region.
    * @param tau - the time constant in seconds.
    */
   public void addConnection(int sourceIndex, int drainIndex, double tau) {
      connectionList.add(new RCConnection(sourceIndex, drainIndex, tau));
   }

   /**
    * <p>
    * Connects two constrained regions with the specified voltage dependent
    * relaxation time. At least one of the regions must be subject to a
    * FloatingConstraint. The other may be either another FloatingConstraint or
    * a DirichletConstraint.
    * </p>
    * <p>
    * The voltage-dependent relaxation time is interpolated from the set of
    * uniformly spaced discrete points (V[i],tauArray[i]) with V[i] = V0 +
    * i*deltaV with i=0,...,n-1 where n = tauArray.length. For V &lt; V0,
    * tauArray[0] is used. For V &gt; V0+(n-1)*deltaV tauArray[n-1] is used.
    * </p>
    * 
    * @param sourceIndex
    * @param drainIndex
    * @param V0
    * @param deltaV
    * @param tauArray
    */
   public void addConnection(int sourceIndex, int drainIndex, double V0, double deltaV, double[] tauArray) {
      connectionList.add(new RCConnection(sourceIndex, drainIndex, V0, deltaV, tauArray));
   }

   /**
    * <p>
    * Connects two constrained regions with the specified voltage dependent
    * relaxation time. At least one of the regions must be subject to a
    * FloatingConstraint. The other may be either another FloatingConstraint or
    * a DirichletConstraint.
    * </p>
    * <p>
    * The relaxation time vs. voltage dependence is interpolated from the not
    * necessarily uniformly spaced set of discrete points
    * (tauArray[0,i],tauArray[1,i]). tauArray[0] should be a monotonically
    * increasing array of voltages. tauArray[1] is the array of corresponding
    * relaxation times. For V&lt;tauArray[0,0] the relaxation time is
    * tauArray[1,0]. For V&gt;tauArray[0,n-1] the relaxation time is
    * tauArray[1,n-1]
    * </p>
    * 
    * @param sourceIndex
    * @param drainIndex
    * @param tauArray
    */
   public void addConnection(int sourceIndex, int drainIndex, double[][] tauArray) {
      connectionList.add(new RCConnection(sourceIndex, drainIndex, tauArray));
   }

   /**
    * Returns the constraint associated with the indexed node. If there is no
    * constraint, returns null;
    * 
    * @param nodeIndex
    * @return IConstraint
    */
   public IConstraint constraint(int nodeIndex) {
      return nodeConstraints.get(nodeIndex);
   }

   /**
    * Returns the MeshElementRegion that contains the specified point or null if
    * the point is outside this MeshedRegion.
    * 
    * @param pos double[]
    * @return RegionBase
    */
   @Override
   protected MeshElementRegion containingSubRegion(double[] pos) {
      if(mShape.contains(pos)) {
         final Mesh.Tetrahedron containingShape = ((Mesh.MeshShape) mShape).containingShape();
         final Long tag = ((Mesh.Element) containingShape).getTags()[0];
         return new MeshElementRegion(this, msmMap.get(tag), containingShape);
      }
      return null;
   }

   /**
    * Given a starting point (pos0) and a candidate ending point (pos1),
    * findEndOfStep checks the parent RegionBase and each sub-RegionBase to
    * determine whether the line segment intersects the boundary of any regions.
    * Intersecting a boundary indicates that the step traverses between two
    * regions. If the segment intersects either the edge of this region or a
    * sub-RegionBase then findEndOfStep returns the intersecting RegionBase.
    * Otherwise findEndOfStep returns this. If the segment intersects a new
    * RegionBase, pos1 will be moved to the border of the new RegionBase.
    * 
    * @param pos0 double[] - The fixed initial point.
    * @param pos1 double[] - [In] The candidate end point [Out] The actual end
    *           point
    * @return RegionBase - The RegionBase in which the [Out] pos1 is found
    */
   @Override
   public RegionBase findEndOfStep(double[] pos0, double[] pos1) {
      /*
       * I don't think this method should ever be called because MeshedRegion is
       * never a point's innermost containing region.
       */
      assert 1 == 0; // This should stop us if we ever come
                     // here while in debug mode.
      /*
       * In case we come here not in debug mode I'm going to write something
       * reasonable.
       */
      /* Find out where we are */
      RegionBase myActualRegion = null;
      if((mParent != null) && mParent.mShape.contains(pos0))
         myActualRegion = mParent.containingSubRegion(pos0);
      if(myActualRegion == null)
         throw new EPQFatalException("MeshedRegion.findEndOfStep called with pos0 = " + Arrays.toString(pos0)
               + " in unknown Region.");
      return myActualRegion.findEndOfStep(pos0, pos1);
   }

   /**
    * Returns 0 since MeshRegion per se contains no Material. To ascertain the
    * number of atoms per cubic meter in one of the subregions, call that
    * subregion's getAtomsPerCubicMeter method.
    * 
    * @param el
    * @return double
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase#getAtomsPerCubicMeter(gov.nist.microanalysis.EPQLibrary.Element)
    */
   @Override
   public double getAtomsPerCubicMeter(Element el) {
      return 0.;
   }

   /**
    * Returns an array containing the connections defined for this MeshedRegion.
    * If there are no connections, an array of length 0 is returned.
    * 
    * @return Returns the connectionList.
    */
   public RCConnection[] getConnectionList() {
      return connectionList.toArray(new RCConnection[0]);
   }

   /**
    * Gets the constraints in the form of an array. This array lists constraints
    * in the same order indexed by the RCConnection class.
    * 
    * @return Returns the constraintList.
    */
   public IConstraint[] getConstraintList() {
      return constraintList;
   }

   /**
    * Gets constraints in the form of a map of tags to the corresponding
    * constraints in order to facilitate rapid location of the constraint that
    * corresponds to a given region.
    * 
    * @return Returns the constraintMap.
    */
   public HashMap<Long, IConstraint> getConstraintMap() {
      return constraintMap;
   }

   /**
    * Returns the relative dielectric constant of the material in the element
    * number given by the argument if that element is a volume element.
    * Otherwise, it returns an unphysical value of 0.
    * 
    * @param elemNum - the element number of a region in the mesh.
    * @return - the relative dielectric constant of the material in that region.
    */
   public double getDielectricConstant(int elemNum) {
      if(mesh.isVolumeType(elemNum)) {
         final long tag = mesh.getTags(elemNum)[0];
         final SEmaterial mat = (SEmaterial) this.getMSM(tag).getMaterial();
         final double eps = mat.getEpsr();
         return eps;
      } else
         return 0.;
   }

   /**
    * Returns a set of Element objects containing all elements in this
    * MeshedRegion. If recurse = true, the set will also contain elements in the
    * MeshElementRegion subregions of this MeshedRegion. The method retains this
    * form for compatibility with other types of regions, which may not be
    * wholly covered by their subregions. However, since a MeshedRegion IS
    * entirely covered by its subregions, this method returns an empty set when
    * recurse = false.
    * 
    * @param recurse boolean - Whether to recurse into sub-regions when
    *           determining the element list.
    * @return Set&lt;Element&gt; - A set of Element objects
    */
   @Override
   protected Set<Element> getElements(boolean recurse) {
      final Set<Element> res = new HashSet<Element>(); // empty set
      if(recurse)
         for(final Long tag : materialTags)
            res.addAll(msmMap.get(tag).getMaterial().getElementSet());
      return res;
   }

   /**
    * This method always returns null. It is included for compatibility with its
    * parent RegionBase. There is neither any Material associated with the
    * MeshedRegion per se, nor any reason to have one since the MeshedRegion
    * should never be the innermost containing subregion for any point.
    * 
    * @return Material
    */
   @Override
   public Material getMaterial() {
      return null;
   }

   /**
    * Gets the current value assigned to mesh
    * 
    * @return Returns the mesh.
    */
   public Mesh getMesh() {
      return mesh;
   }

   /**
    * Returns the IMaterialScatterModel that corresponds to the supplied
    * materialTag in this MeshedRegion's msmMap, or null if there isn't one.
    * 
    * @param materialTag
    * @return IMaterialScatterModel
    */
   public IMaterialScatterModel getMSM(long materialTag) {
      if(msmMap.containsKey(materialTag))
         return msmMap.get(materialTag);
      return null;
   }

   /**
    * Gets the current value assigned to msmMap
    * 
    * @return Returns the msmMap.
    */
   public HashMap<Long, IMaterialScatterModel> getMSMMap() {
      return msmMap;
   }

   /**
    * Useful when the underlying mesh can change after it is instantiated, e.g.,
    * if it is adaptive. When called it re-initializes the mesh if it has been
    * adapted. It additionally re-initializes any needed MeshedRegion values.
    * 
    * @return
    */
   public boolean initializeIfNeeded() {
      final boolean result = mesh.initializeIfNeeded();
      if(result)
         mShape = mesh.getMeshShape();
      /*
       * Associated floating constraints have cached values and need to be
       * re-initialized.
       */
      for(int i = 0; i < constraintList.length; i++) {
         final IConstraint c = constraintList[i];
         if(c instanceof FloatingConstraint) {
            /*
             * Reconstruct the FloatingConstraint to force renewal of cached
             * values and replace its existing instances in constraintList and
             * constraintMap with the new version.
             */
            constraintList[i] = new FloatingConstraint(mesh, c.getAssociatedRegionTag(), ((FloatingConstraint) c).getVolumeTag());
            constraintMap.put(c.getAssociatedRegionTag(), constraintList[i]);
         }

      }
      return result;
   }

   /**
    * Returns true if the indexed element is subject to constraint.
    * 
    * @param elementIndex
    * @return boolean
    */
   public boolean isConstrained(int elementIndex) {
      if((elementIndex >= 1) && (elementIndex <= mesh.getNumberOfElements()))
         return constraintMap.containsKey(mesh.getTags(elementIndex)[0]);
      return false;
   }

   /**
    * Returns true only if searchTarget is a member of the mesh associated with
    * this MeshedRegion.
    * 
    * @param searchTarget - The region to search for
    * @return - true if searchTarget is a subregion of parent, false otherwise.
    */
   @Override
   public boolean isContainingRegion(RegionBase searchTarget) {
      /*
       * All our subregions are MeshElementRegions, so if searchTarget isn't a
       * MeshElementRegion it isn't a subregion. If it IS, then it is a
       * subregion iff it is a member of the same mesh.
       */
      if(!(searchTarget instanceof MeshElementRegion))
         return false;
      return ((MeshElementRegion) searchTarget).getMesh() == mesh;
   }

   /**
    * preCharge adds charge to a specified region of the mesh. It can be used,
    * for example, to precharge part of a sample. The region is a rectangular
    * prism (a.k.a. cuboid) with sides parallel to the coordinate axes. It is
    * the region with x0&lt;=x&lt;=x1, y0&lt;=y&lt;=y1, and z0&lt;=z&lt;=z1,
    * with the coordinates supplied in two arrays, p0 = [x0,y0,z0] and p1 =
    * [x1,y1,z1] specifying diagonally opposite corners. Charges, quantized in
    * units of e, are placed randomly (uniform distribution) in the designated
    * volume until the number density approximates the specified value. In case
    * the specified numberDensity*volume is not equal to an integer number of
    * charges the actual number of charges placed may be rounded up or down. To
    * prevent placing charges in vacuum, no charges are placed in any part of
    * the volume with density less than 100 kg/m^3. The actual number of charges
    * placed is returned.
    * 
    * @param p0 - [x0,y0,z0] = minimum corner of volume to charge
    * @param p1 - [x1,y1,z1] = max corner of volume to charge
    * @param numberDensity - number of charge quanta (units of e) per cubic
    *           meter.
    * @return - the number of charges placed, positive for positive charge and
    *         negative for negative charge
    */
   public int preCharge(double[] p0, double[] p1, double numberDensity) {
      final double[] pmin = new double[3];
      final double[] pmax = new double[3];
      for(int i = 0; i < 3; i++)
         if(p0[i] < p1[i]) {
            pmin[i] = p0[i];
            pmax[i] = p1[i];
         } else {
            pmin[i] = p1[i];
            pmax[i] = p0[i];
         }
      final double[] delta = Math2.minus(pmax, pmin);
      final double volume = delta[0] * delta[1] * delta[2];
      /*
       * Determine how many charges to add. We first compute a number of charges
       * that might contain fractional part. We determine its fractional part
       * and then round up or down randomly but such that the average charge is
       * equal to the given amount.
       */
      final double n = numberDensity * volume; // number of charges to add
      int nint = (int) Math.floor(n); // integer part
      final double frac = n - nint;
      if(Math2.rgen.nextDouble() < frac)
         nint++;

      /* Place this many charges at random positions in the volume */
      final int imax = Math.abs(nint);
      int chargesAdded = 0;
      for(int i = 0; i < imax; i++) {
         /* u (next line) is a random position relative to pmin. */
         final double[] u = new double[] {
            delta[0] * Math2.rgen.nextDouble(),
            delta[1] * Math2.rgen.nextDouble(),
            delta[2] * Math2.rgen.nextDouble()
         };
         final double[] p = Math2.plus(pmin, u);

         final MeshElementRegion reg = containingSubRegion(p);
         if(reg.getMaterial().getDensity() < DENSITY_THRESHOLD)
            continue;
         final Tetrahedron tet = (Tetrahedron) reg.getShape();
         if(nint < 0.) {
            tet.decrementChargeNumber();
            chargesAdded--;
         } else {
            tet.incrementChargeNumber();
            chargesAdded++;
         }
      }

      return chargesAdded;
   }

   /**
    * If the electron's position is in this MeshedRegion, it resets the
    * electron's current region to the appropriate subregion. Otherwise it does
    * nothing. This is mainly useful when the underlying mesh is adaptive. It
    * should be called whenever the mesh adapts, because the electron's
    * containing region may in that case change.
    * 
    * @param el
    */
   public void resetElectronRegion(Electron el) {
      final MeshElementRegion csr = containingSubRegion(el.getPosition());
      if(csr != null)
         el.setCurrentRegion(csr);
   }

   // documented in ITransform
   @Override
   public void rotate(double[] pivot, double phi, double theta, double psi) {
      final ITransform t = (ITransform) mShape;
      t.rotate(pivot, phi, theta, psi);
   }

   @Override
   public String toString() {
      String res = mShape.toString() + " of [";
      for(final Long tag : materialTags)
         res += msmMap.get(tag).getMaterial().toString() + ", ";
      res += "]";
      return res;
   }

   // documented in ITransform
   @Override
   public void translate(double[] distance) {
      final ITransform t = (ITransform) mShape;
      t.translate(distance);
   }

   /**
    * Recursively replace all instances of oldMat with newMat in this
    * MeshedRegion
    * 
    * @param oldMat - The old IMaterialScatterModel
    * @param newMat - The new IMaterialScatterModel
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase#updateMaterial(gov.nist.microanalysis.NISTMonte.IMaterialScatterModel,
    *      gov.nist.microanalysis.NISTMonte.IMaterialScatterModel)
    */
   @Override
   public void updateMaterial(IMaterialScatterModel oldMat, IMaterialScatterModel newMat) {
      // Recursively replace all instances of oldMat with newMat
      for(final Long tag : materialTags)
         if(msmMap.get(tag) == oldMat)
            msmMap.put(tag, newMat);
   }

   /**
    * Recursively replace all instances of oldMat with newMat in this
    * MeshedRegion
    * 
    * @param oldMat - The old material
    * @param newMat - The IMaterialScatterModel for the new material.
    * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase#updateMaterial(gov.nist.microanalysis.EPQLibrary.Material,
    *      gov.nist.microanalysis.NISTMonte.IMaterialScatterModel)
    */
   /*
    * Override parent method to avoid attempting to check all subregions.
    * Instead we simply update the msmMap.
    */
   @Override
   public void updateMaterial(Material oldMat, IMaterialScatterModel newMat) {
      // Recursively replace all instances of oldMat with newMat
      for(final Long tag : materialTags)
         if(msmMap.get(tag).getMaterial() == oldMat)
            msmMap.put(tag, newMat);
   }
}
