/**
 * gov.nist.nanoscalemetrology.JMONSEL.MeshElementRegion Created by: John
 * Villarrubia Date: Oct 26, 2010
 */
package gov.nist.microanalysis.NISTMonte;

import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.nanoscalemetrology.JMONSEL.Mesh;
import gov.nist.nanoscalemetrology.JMONSEL.Mesh.ConnectedShape;

/**
 * <p>
 * Implements Transformable
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
public class MeshElementRegion
   extends RegionBase {

   int index;

   /**
    * Constructs a MeshElementRegion
    */
   public MeshElementRegion(MeshedRegion parent, IMaterialScatterModel msm, Mesh.Tetrahedron tet) {
      mParent = parent;
      mScatterModel = msm;
      mShape = tet;
      index = tet.getIndex();
   }

   @Override
   public void updateMaterial(Material oldMat, IMaterialScatterModel newMat) {
      if(mScatterModel.getMaterial() == oldMat)
         mScatterModel = newMat;
   }

   @Override
   public void updateMaterial(IMaterialScatterModel oldMat, IMaterialScatterModel newMat) {
      if(mScatterModel == oldMat)
         mScatterModel = newMat;
   }

   /**
    * Given a starting point (pos0) and a candidate ending point (pos1),
    * findEndOfStep checks whether the line segment intersects the boundary of
    * this region. If it does, findEndOfStep replaces pos1 with the position of
    * the intersection and returns the region on the other side of the boundary
    * (possibly null if the boundary coincides with the chamber). If there is no
    * intersection, findEndOfStep leaves pos1 unaltered and returns this region.
    * 
    * @param pos0 double[] - The fixed initial point.
    * @param pos1 double[] - [In] The candidate end point [Out] The actual end
    *           point
    * @return RegionBase - The RegionBase in which the [Out] pos1 is found
    */
   @Override
   public RegionBase findEndOfStep(double[] pos0, double[] pos1) {
      final double t = mShape.getFirstIntersection(pos0, pos1);
      assert t >= 0.0 : mShape.toString() + " " + Double.toString(t);
      if(t > 1.0) // no boundary intersection
         return this;
      // Arrive here if the segment does hit a boundary.
      // Put pos1 exactly (within round-off) on the boundary.
      final double[] delta = {
         pos1[0] - pos0[0],
         pos1[1] - pos0[1],
         pos1[2] - pos0[2]
      };
      pos1[0] = pos0[0] + (t * delta[0]);
      pos1[1] = pos0[1] + (t * delta[1]);
      pos1[2] = pos0[2] + (t * delta[2]);
      final Mesh.Tetrahedron nextShape = ((Mesh.Tetrahedron) mShape).nextTet();

      if(nextShape == null) { // Trajectory leaves the whole mesh
         /* Get a point just over the boundary */
         final double[] over = Math2.plus(pos1, Math2.multiply(MonteCarloSS.SMALL_DISP, Math2.normalize(delta)));
         /*
          * Move up the hierarchy of shapes until we find the one that contains
          * this point.
          */
         /*
          * We've already established the trajectory is leaving both this
          * MeshElementRegion and its parent MeshedRegion. Therefore, in the
          * next line, we start our search at mParent.mParent.
          */
         for(RegionBase base = mParent.mParent; base != null; base = base.mParent) {
            final RegionBase nextRegion = base.containingSubRegion(over);
            if(nextRegion != null)
               return nextRegion;
         }
         return null; // new point is nowhere in the chamber
      }
      // Here if trajectory passes to another tetrahedron in this mesh
      final Long materialTag = nextShape.getTags()[0];
      final IMaterialScatterModel msm = ((MeshedRegion) mParent).getMSM(materialTag);
      return new MeshElementRegion((MeshedRegion) mParent, msm, nextShape);
   }

   /**
    * Returns true if this region is subject to constrained potential.
    * 
    * @return boolean
    */
   public boolean isConstrained() {
      return ((MeshedRegion) mParent).isConstrained(((Mesh.Tetrahedron) mShape).getIndex());
   }

   /**
    * Gets the mesh of which this MeshElementRegion is a member.
    * 
    * @return Returns the mesh.
    */
   public Mesh getMesh() {
      return ((ConnectedShape) mShape).getMesh();
   }

   /**
    * Returns the index of the mesh element associated with this region.
    * 
    * @return - index of mesh element associated with this region.
    */
   public int getIndex() {
      return index;
   }
}
