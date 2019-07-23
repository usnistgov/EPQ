package gov.nist.microanalysis.NISTMonte;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ITransform;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQLibrary.Detector.ElectronProbe;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * MonteCarloSS is the core class in NISTMonte - a full-featured (albeit
 * somewhat slow) Monte Carlo for quantitative electron probe modeling and other
 * uses. MonteCarloSS implements a electron transport model using the randomized
 * scattering (IRandomizedScatter) and the continuous slowing down
 * approximation.
 * </p>
 * <p>
 * NISTMonte makes clear distinctions between algorithms. MonteCarloSS does not
 * know anything about x-rays (that is handled by XRayEventListener and
 * BremsstrahlungEventListener) or electron detectors (that is handled by
 * AnnularDetector, BackscatterDetector, ...). All MonteCarloSS knows how to do
 * is to track electrons. MonteCarloSS ties to the x-ray models and the
 * detectors through events via the addActionListener(...) method. All classes
 * that are interested in events should implement the ActionListener abstract
 * class. The events are identified by an integer index - ScatterEvent,
 * NonScatterEvent, BackscatterEvent, TrajectoryStartEvent, TrajectoryEndEvent,
 * LastTrajectoryEvent, FirstTrajectoryEvent, or ExitMaterialEvent.
 * </p>
 * <p>
 * Physical models can be changed by using setBetheElectronEnergyLoss,
 * setScatteringAlgorithm or setGasScatteringAlgorithm.
 * </p>
 * Notes:<br>
 * <ul>
 * <li>Arguments and variables are all SI.
 * <li>Positions are all expressed as double [3] where 0-&gt;x, 1-&gt;y and
 * 2-&gt;z.
 * <li>The coordinate system is right-handed with z as the default initial beam
 * axis.
 * <li>The electron nominally starts at a negative z position with a direction
 * of motion towards positive z.
 * </ul>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie, John Villarrubia
 * @version 1.0
 */

final public class MonteCarloSS {

   /**
    * When the electron's free path is shorter than the distance to the nearest
    * boundary of its region, the electron scatters; a ScatterEvent is fired
    * immediately before the scatter event. In each trajectory segment, one and
    * only one of ScatterEvent, NonScatterEvent or BackscatterEvent will be
    * fired.
    */
   public static final int ScatterEvent = 1;

   /**
    * Fired whenever an electron trajectory segment ends because it reaches the
    * boundary between regions. At the time of this event, the electron remains
    * in its original region. It will be followed by an ExitMaterialEvent event
    * if the electron transmits through the boundary to a new region (even if
    * that region contains the same material), but this may not happen since the
    * electron may reflect at the boundary and remain in its original region.
    */
   public static final int NonScatterEvent = ScatterEvent + 1;

   /**
    * The ID of the ActionEvent that is fired when an electron trajectory
    * strikes the walls of the chamber (forwardscatter or backscatter). This can
    * also be thought of as an exit chamber region event. The electron is
    * permanently gone and the trajectory is over.
    */
   public static final int BackscatterEvent = ScatterEvent + 2;

   /**
    * Fired when an electron makes a transition from one region to another. When
    * an electron exits a Region two events are fired; first a NonScatterEvent
    * and then an ExitMaterialEvent. The first is fired before the boundary
    * crossing (GetCurrentRegion will return the region from which the electron
    * is about to exit) while the second is fired after the boundary crossing
    * (GetCurrentRegion returns the new region). If the electron reflects
    * instead of transmits at the interface, the ExitMaterialEvent is not fired.
    */
   public static final int ExitMaterialEvent = ScatterEvent + 3;

   /**
    * Fired before an electron starts its trajectory.
    */
   public static final int TrajectoryStartEvent = ScatterEvent + 4;

   /**
    * Fired after an electron ends its trajectory.
    */
   public static final int TrajectoryEndEvent = ScatterEvent + 5;

   /**
    * Fired after the last trajectory completes in runMultipleTrajectories
    */
   public static final int LastTrajectoryEvent = ScatterEvent + 6;

   /**
    * Fired before the first trajectory starts in runMultipleTrajectories.
    */
   public static final int FirstTrajectoryEvent = ScatterEvent + 7;

   /**
    * StartSecondaryEvent is fired immediately after the current Electron is
    * replaced by a secondary electron.
    */
   public static final int StartSecondaryEvent = ScatterEvent + 8;

   /**
    * EndSecondaryEvent fired when a secondary electron finishes its trajectory.
    * The current Electron is still the secondary.
    */
   public static final int EndSecondaryEvent = ScatterEvent + 9;

   /**
    * PostScatterEvent is fired immediately after an electron scatters.
    */
   public static final int PostScatterEvent = ScatterEvent + 10;

   /**
    * Fired when the setBeamEnergy(...) method is invoked. Useful for clearing
    * caches in detection devices etc...
    */
   public static final int BeamEnergyChanged = 100;

   /**
    * XAxis, YAxis and ZAxis enumerate the coordinate axes.
    */
   public static final int XAxis = 0;

   /**
    * XAxis, YAxis and ZAxis enumerate the coordinate axes.
    */
   public static final int YAxis = 1;

   /**
    * XAxis, YAxis and ZAxis enumerate the coordinate axes.
    */
   public static final int ZAxis = 2;

   public static final double ChamberRadius = 0.1;

   /**
    * <p>
    * This interface allows the user to modify the properties of the electron
    * gun. By implementing this interface, the user can create guns with a beam
    * width, guns that raster or a variety of different gun properties. Use
    * MonteCarloSS.setElectronGun to specify the implementation of this
    * interface.<br>
    * Note: The electron is rastered by moving the initial point rather than
    * varying the beam angle. This is not realistic but is better numerically as
    * the angles are otherwise tiny and subject to rounding errors.
    * </p>
    * <p>
    * Copyright: Not subject to copyright 2004
    * </p>
    * <p>
    * Company: National Institute of Standards and Technology
    * </p>
    * 
    * @author Nicholas W. M. Ritchie
    * @version 0.1
    */
   public interface ElectronGun {
      /**
       * Called by MonteCarloSS.setBeamEnergy to modify the acceleration
       * potential.
       * 
       * @param beamEnergy double - in Joules
       */
      void setBeamEnergy(double beamEnergy);

      /**
       * Returns the beam energy
       * 
       * @return double - in Joules
       */
      double getBeamEnergy();

      /**
       * Called by MonteCarloSS to set the location of the electron source.
       * 
       * @param center double[]
       */
      void setCenter(double[] center);

      /**
       * Returns the location of the center of the gun.
       * 
       * @return double[]
       */
      double[] getCenter();

      /**
       * Called by MonteCarloSS at the beginning of each trajectory to get a
       * fresh Electron.
       * 
       * @return Electron
       */
      Electron createElectron();
   }

   /**
    * <p>
    * An interface defining sufficient a sufficiently rich set of methods to
    * implement a three dimensional volume. A TransformableRegion is defined by
    * a Shape and a Material and fully contained child TransformableRegion
    * objects.
    * </p>
    */
   public interface Shape {
      /**
       * Is the specified point inside the item represented by this Shape
       * interface? Contains is inclusive. A point on the boundary is inside. A
       * point on the interface between two Shapes is considered to be inside
       * both Shapes.
       * 
       * @param pos double[] - a three item array
       * @return boolean
       */
      boolean contains(double[] pos);

      /**
       * Consider a ray starting at pos0 towards pos1. If the ray does not
       * intersect this shape return Double.MAX_VALUE. If the ray does intersect
       * the Shape, return the u such that p(u)=pos0+u*(pos1-pos0) is the point
       * at which the intersection occurs. If u is greater or equal to 0 but
       * less than or equal to 1, the intersection occurs on the interval
       * [pos0,pos1]. This indicates that a full step from pos0 to pos1 will not
       * remain entirely inside Shape. In fact, the longest possible step is
       * starts at pos0 and ends at p(u). Intersections before pos0 (ie
       * u&lt;0.0) are not relevant and should be ignored. Implementations of
       * getFirstIntersection may be optimized for the case in which u is
       * greater than 1.0. Since the distance between pos0 and pos1 represents a
       * single step and u=1.0 represents taking the full step, you may return a
       * number greater than 1.0 but less than the real u when the real u is
       * greater than one and it would require additional computation to
       * discover the true value of u.
       * 
       * @param pos0 double[] - three element array
       * @param pos1 double[] - three element array
       * @return double - The fraction of the length from pos0 to pos1 at which
       *         the first intersection occurs. Otherwise Double.MAX_VALUE.
       */
      double getFirstIntersection(double[] pos0, double[] pos1);
   };

   /**
    * SMALL_DISP is a small displacement used in the TransformableRegion class's
    * findEndOfStep() method to create a test point displaced slightly beyond a
    * material boundary. The test point is used to ascertain the identity of the
    * neighboring region. smalldisp is also used in takeStep() to move an
    * electron that stops on a boundary slightly beyond it, in its direction of
    * motion (as possibly altered by scattering at the boundary). It may be
    * important that both these routines use the same displacement.
    */
   public static final double SMALL_DISP = 1.0e-15; // 1 fm

   /**
    * <p>
    * RegionBase is the abstract class from which all regions derive. Regions
    * represent volumes of sample. The volume may be completely homogeneous
    * (uniform composition) or piecewise homogeneous if subregions are
    * permitted. Regions have a Shape, a Material and a list of fully contained
    * child Regions (subregions).
    * </p>
    * <p>
    * Copyright: Not subject to copyright - 2004
    * </p>
    * <p>
    * Company: National Institute of Standards and Technology
    * </p>
    * 
    * @author Nicholas W. M. Ritchie &amp; John Villarrubia
    * @version 1.0
    */
   /*
    * The motivation behind the region hierarchy is this: There are 3 kinds of
    * regions that we need: constructive solid geometry Region, MeshedRegion,
    * and MeshElement. The constructive solid geometry Region is the one
    * NISTMonte has always had. It is rotatable, translatable, and can have any
    * number of generations of child regions. A MeshElement is a volume element
    * (e.g., tetrahedron) that is a unit of a meshed space. It has a shape and a
    * material, like other regions, but it cannot be transformed (rotated or
    * translated) independently of the rest of the mesh, nor can it have
    * subregions. A MeshedRegion is a "container" that holds all the
    * MeshElements associated with a given mesh. Think of it as a region that is
    * divided into MeshElement subregions such that there is no space left over.
    * I.e., Every point contained in the MeshedRegion is also contained in one
    * and only one of its subregions. Unlike the individual MeshElements, the
    * MeshedRegion therefore can and does have subregions, but only 1 layer
    * deep. (Its MeshElement subregions cannot have further subregions.) The
    * MeshedRegion represents an entire mesh, so unlike the individual
    * MeshElements it can be transformed by applying the transform to the whole
    * mesh. Thus, the abstract RegionBase class has two child classes, the
    * concrete MeshElement class (no transformations and no subregions) and the
    * abstract TransformableRegion class. The TransformableRegion class in turn
    * has two concrete child classes, the MeshedRegion class (transformable and
    * with one generation of subregions) and the Region class (transformable
    * with arbitrarily many generations of subregions). Note that the subregions
    * of Region may be either Regions or MeshedRegions. Subregions of
    * MeshedRegions must be MeshElements.
    */
   static abstract public class RegionBase {
      protected TransformableRegion mParent;

      protected IMaterialScatterModel mScatterModel;

      protected Shape mShape;

      protected ArrayList<RegionBase> mSubRegions = new ArrayList<RegionBase>();

      public void updateMaterial(Material oldMat, IMaterialScatterModel newMat) {
         // Recursively replace all instances of oldMat with newMat
         if(mScatterModel.getMaterial() == oldMat)
            mScatterModel = newMat;
         for(final RegionBase reg : mSubRegions)
            reg.updateMaterial(oldMat, newMat);
      }

      public void updateMaterial(IMaterialScatterModel oldMat, IMaterialScatterModel newMat) {
         // Recursively replace all instances of oldMat with newMat
         if(mScatterModel == oldMat)
            mScatterModel = newMat;
         for(final RegionBase reg : mSubRegions)
            reg.updateMaterial(oldMat, newMat);
      }

      /**
       * Get the material of which this RegionBase is constructed.
       * 
       * @return Material
       */
      public Material getMaterial() {
         return mScatterModel.getMaterial();
      }

      /**
       * Returns the IMaterialScatterModel associated with this RegionBase.
       * 
       * @return IMaterialScatterModel
       */
      public IMaterialScatterModel getScatterModel() {
         return mScatterModel;
      }

      /**
       * Returns an immutable list of the sub-regions associated with this
       * region.
       * 
       * @return An immutable List
       */
      public List<RegionBase> getSubRegions() {
         return Collections.unmodifiableList(mSubRegions);
      }

      /**
       * Returns the inner most sub-region that contains the specified point.
       * 
       * @param pos double[]
       * @return RegionBase
       */
      protected RegionBase containingSubRegion(double[] pos) {
         if(mShape.contains(pos)) {
            for(final RegionBase reg : mSubRegions) {
               final RegionBase csr = reg.containingSubRegion(pos);
               if(csr != null)
                  return csr;
            }
            return this;
         }
         return null;
      }

      /**
       * Returns a set of Element objects containing all elements in this
       * RegionBase and optionally all contained regions.
       * 
       * @param recurse boolean - Whether to recurse into sub-regions when
       *           determining the element list.
       * @return Set&lt;Element&gt; - A set of Element objects
       */
      protected Set<Element> getElements(boolean recurse) {
         final Set<Element> res = new HashSet<Element>(getMaterial().getElementSet());
         if(recurse)
            for(final Object element : mSubRegions)
               res.addAll(((RegionBase) element).getElements(true));
         return res;
      }

      /**
       * Given a starting point (pos0) and a candidate ending point (pos1),
       * findEndOfStep checks the parent RegionBase and each sub-RegionBase to
       * determine whether the line segment intersects the boundary of any
       * regions. Intersecting a boundary indicates that the step traverses
       * between two regions. If the segment intersects either the edge of this
       * region or a sub-RegionBase then findEndOfStep returns the intersecting
       * RegionBase. Otherwise findEndOfStep returns this. If the segment
       * intersects a new RegionBase, pos1 will be moved to the border of the
       * new RegionBase.
       * 
       * @param pos0 double[] - The fixed initial point.
       * @param pos1 double[] - [In] The candidate end point [Out] The actual
       *           end point
       * @return RegionBase - The RegionBase in which the [Out] pos1 is found
       */
      public RegionBase findEndOfStep(double[] pos0, double[] pos1) {
         // Check to see whether we leave this RegionBase into the
         // parent
         RegionBase base = this;

         double t = mShape.getFirstIntersection(pos0, pos1);
         assert t >= 0.0 : mShape.toString() + " " + Double.toString(t);
         if((t <= 1.0) && (mParent != null))
            base = mParent;
         RegionBase res = this;
         /*
          * Check each subregion to find out whether the second point is within
          * it. Note: Because we insist that Regions are fully contained within
          * sub Regions, we don't need to check the child region's child regions
          * etc.
          */
         for(final RegionBase subRegion : mSubRegions) {
            final double candidate = subRegion.mShape.getFirstIntersection(pos0, pos1);
            assert candidate > 0.0 : subRegion.mShape.toString() + " " + Double.toString(candidate);
            if((candidate <= 1.0) && (candidate < t)) {
               t = candidate;
               base = subRegion;
            }
         }
         assert (t >= 0.0);
         if(t <= 1.0) {
            final double[] delta = Math2.minus(pos1, pos0);
            // Put pos1 exactly on the boundary.
            pos1[0] = pos0[0] + (t * delta[0]);
            pos1[1] = pos0[1] + (t * delta[1]);
            pos1[2] = pos0[2] + (t * delta[2]);
            // Find the region just over the boundary...
            final double[] over = Math2.plus(pos1, Math2.multiply(SMALL_DISP, Math2.normalize(delta)));
            while(base != null) {
               res = base.containingSubRegion(over);
               if(res != null)
                  return res;
               /*
                * If test point is not in parent region, check grandparents,
                * etc. until we reach the chamber itself.
                */
               base = base.mParent;
            }
            return null; // new point is nowhere in the chamber
         }
         return res;
      }

      /**
       * Returns the instance of the Shape interface that defines the shape of
       * this TransformableRegion.
       * 
       * @return Shape
       */
      public Shape getShape() {
         return mShape;
      }

      /**
       * Get the number density (atoms per cubic meter) of the given element in
       * this region.
       * 
       * @param el
       * @return double
       */
      public double getAtomsPerCubicMeter(Element el) {
         return getMaterial().atomsPerCubicMeter(el);
      }

      /**
       * Get this region's parent, or null if it doesn't have one.
       * 
       * @return RegionBase
       */
      public RegionBase getParent() {
         return mParent;
      }

      /**
       * Searches this region to see if the present region contains the
       * searchTarget region. It contains it if searchTarget is is one of its
       * subregions, or recursively one of their subregions, etc.
       * 
       * @param searchTarget - The region to search for
       * @return - True if searchTarget is a subregion of parent, false
       *         otherwise.
       */
      public boolean isContainingRegion(RegionBase searchTarget) {
         for(final RegionBase sub : mSubRegions)
            if((sub == searchTarget) || sub.isContainingRegion(searchTarget))
               return true;
         return false;
      }

      @Override
      public String toString() {
         return mShape.toString() + " of " + getMaterial().toString();
      }

   }

   /**
    * <p>
    * TransformableRegion extends BaseRegion by implementing the ITransform
    * interface, thereby permitting regions to be rotated and translated.
    * </p>
    * <p>
    * Copyright: Not subject to copyright - 2004
    * </p>
    * <p>
    * Company: National Institute of Standards and Technology
    * </p>
    * 
    * @author Nicholas W. M. Ritchie &amp; John Villarrubia
    * @version 1.0
    */

   static abstract public class TransformableRegion
      extends RegionBase
      implements ITransform {

      // documented in ITransform
      @Override
      public void rotate(double[] pivot, double phi, double theta, double psi) {
         // check whether we can....
         if(mShape instanceof ITransform)
            for(final Object obj : mSubRegions) {
               if(!(obj instanceof ITransform))
                  throw new EPQFatalException(obj.toString() + " does not support transformation.");
            }
         else
            throw new EPQFatalException(mShape.toString() + " does not support transformation.");
         // then do it...
         ITransform t = (ITransform) mShape;
         t.rotate(pivot, phi, theta, psi);
         for(final Object element : mSubRegions) {
            t = (ITransform) element;
            t.rotate(pivot, phi, theta, psi);
         }
      }

      // documented in ITransform
      @Override
      public void translate(double[] distance) {
         // check whether we can....
         if(mShape instanceof ITransform)
            for(final Object obj : mSubRegions) {
               if(!(obj instanceof ITransform))
                  throw new EPQFatalException(obj.toString() + " does not support transformation.");
            }
         else
            throw new EPQFatalException(mShape.toString() + " does not support transformation.");
         // then do it...
         ITransform t = (ITransform) mShape;
         t.translate(distance);
         for(final Object element : mSubRegions) {
            t = (ITransform) element;
            t.translate(distance);
         }
      }
   }

   /**
    * <p>
    * Region extends TransformableRegion by permitting addition and removal of
    * subregions.
    * </p>
    * <p>
    * Copyright: Not subject to copyright - 2004
    * </p>
    * <p>
    * Company: National Institute of Standards and Technology
    * </p>
    * 
    * @author Nicholas W. M. Ritchie &amp; John Villarrubia
    * @version 1.0
    */

   static public class Region
      extends TransformableRegion {

      public Region(Region parent, IMaterialScatterModel msm, Shape shape) {
         mParent = parent;
         mScatterModel = msm;
         mShape = shape;
         if(mParent != null)
            mParent.mSubRegions.add(this);
      }

      /**
       * Remove the specified subregion from this Region.
       * 
       * @param subRegion TransformableRegion
       */
      public void removeSubRegion(TransformableRegion subRegion) {
         mSubRegions.remove(subRegion);
      }

      /**
       * Clear all the fully contained subregions within this Region.
       */
      public void clearSubRegions() {
         mSubRegions.clear();
      }
   }

   // Configuration data
   private final Region mChamber;

   private ElectronGun mGun = new GaussianBeam(1.0e-8);

   private static final IMaterialScatterModel NULL_MSM = new IMaterialScatterModel() {

      /*
       * By default track even very low energy electrons in vacuum. When they
       * hit something, chamber, sample, or detector, we can decide what to do
       * with them.
       */
      private double minEforTracking = ToSI.eV(0.1);

      @Override
      public Electron barrierScatter(Electron pe, RegionBase nextRegion) {
         pe.setCurrentRegion(nextRegion);
         pe.setScatteringElement(null);
         return null;
      }

      @Override
      public double calculateEnergyLoss(double len, Electron pe) {
         return 0.0;
      }

      @Override
      public Material getMaterial() {
         return Material.Null;
      }

      @Override
      public double randomMeanPathLength(Electron pe) {
         return 1.0;
      }

      @Override
      public Electron scatter(Electron pe) {
         return null;
      }

      @Override
      public double getMinEforTracking() {
         return minEforTracking;
      }

      @Override
      public void setMinEforTracking(double minEforTracking) {
         this.minEforTracking = minEforTracking;
      }
   };

   // Transient run-time configuration data
   transient private Electron mElectron;

   /**
    * A stack of Electrons currently on ice while the current electron runs.
    * When the current Electron finishes its trajectory, the electron on the top
    * of the stack (when available) will complete its trajectory. This is used
    * to implement secondary electrons.
    */
   private final ArrayList<Electron> mElectronStack = new ArrayList<Electron>();

   // Bookkeeping data
   transient private final ArrayList<ActionListener> mEventListeners = new ArrayList<ActionListener>();

   transient private boolean mDisableEvents = false;

   /**
    * Computes the Euclidean distance between pos0 and pos1.
    * 
    * @param pos0 double[]
    * @param pos1 double[]
    * @return double
    */
   static double distance(double[] pos0, double[] pos1) {
      return Math.sqrt(Math2.sqr(pos1[0] - pos0[0]) + Math2.sqr(pos1[1] - pos0[1]) + Math2.sqr(pos1[2] - pos0[2]));
   }

   /**
    * The constructor for the MonteCarloSS class.
    */
   public MonteCarloSS() {
      // The chamber is a Sphere centered at the origin with 0.10 m radius
      final double[] center = {
         0.0,
         0.0,
         0.0
      };
      final Sphere sphere = new Sphere(center, ChamberRadius);
      mGun.setCenter(sphere.getInitialPoint());
      mGun.setBeamEnergy(ToSI.keV(20.0));
      mChamber = new Region(null, NULL_MSM, sphere);
   }

   /**
    * A method to simplify creating new sub-Regions and adding them to a parent
    * Region. The subregion must be fully enclosed within the parent region.
    * This method does not verify this requirement.
    * 
    * @param parent TransformableRegion - A non-null parent region
    * @param mat Material - The material from which the sub-region will be
    *           constructed.
    * @param shape Shape - The shape of the sub-region
    * @return TransformableRegion - The instance of the new sub-region
    */
   public Region addSubRegion(Region parent, Material mat, Shape shape)
         throws EPQException {
      assert (parent != null);
      return new Region(parent, new BasicMaterialModel(mat), shape);
   }

   public Region addSubRegion(Region parent, IMaterialScatterModel msm, Shape shape) {
      return new Region(parent, msm, shape);
   }

   /**
    * Creates a map containing a list of the materials through which an x-ray
    * (or any ray) will pass on its way from startPt to endPt. Associated with
    * each material is the measure of the length of the path through the
    * material. This information can be used to compute the absorption of x-rays
    * generated in the sample as they pass towards a detector.
    * 
    * @return Map&lt;Material,Double&gt; - the key class is Material and value
    *         class is Double
    * @param startPt double[] - The start of the x-ray trajectory (usually the
    *           electron scattering point)
    * @param endPt double[] - The end of the x-ray trajectory (usually the
    *           detector)
    */
   public Map<Material, Double> getMaterialMap(double[] startPt, double[] endPt) {
      final HashMap<Material, Double> traj = new HashMap<Material, Double>();
      double[] start = startPt;
      RegionBase region = mChamber.containingSubRegion(start);
      final double eps = 1.0e-7;
      while((region != null) && (distance(start, endPt) > eps)) {
         final double[] end = endPt.clone();
         final RegionBase nextRegion = region.findEndOfStep(start, end);
         double dist = distance(start, end);
         if(dist > 0.0) {
            if(traj.containsKey(region.getMaterial()))
               dist += (traj.get(region.getMaterial())).doubleValue();
            traj.put(region.getMaterial(), Double.valueOf(dist));
         }
         start = Math2.plus(end, Math2.multiply(SMALL_DISP, Math2.normalize(Math2.minus(endPt, start))));
         region = nextRegion;
      }
      return traj;
   }

   /**
    * Fire one of the events types characteristic of this object. (fireEvent
    * will not fire an event if mDisableEvents is true.)
    * 
    * @param event - One of ScatterEvent, NonScatterEvent, BackscatterEvent,...
    */
   private void fireEvent(int event) {
      if(!(mEventListeners.isEmpty() || mDisableEvents)) {
         final ActionEvent ae = new ActionEvent(this, event, "MonteCarloSS event");
         for(final ActionListener sel : mEventListeners)
            sel.actionPerformed(ae);
      }
   }

   /**
    * An accessor to facilitate adding the sub-Regions that represent the
    * sample. The sample should be located so that the center of the desired
    * beam impact location is on the z-axis.
    * 
    * @return Region - The Region representing the vacuum chamber.
    */
   public Region getChamber() {
      return mChamber;
   }

   /**
    * Initialize all member variable to start a new electron trajectory.
    */
   public void initializeTrajectory() {
      /*
       * Start the electron at the gun position with the specified energy in the
       * chamber.
       */
      mElectron = mGun.createElectron();
      /*
       * Allows the electron to start inside a subregion of the chamber.
       */
      mElectron.setCurrentRegion(mChamber.containingSubRegion(mElectron.getPosition()));
      // Stop when you can't generate any more x-rays
      mElectron.setScatteringElement(null);
   }

   /**
    * Take a single step corresponding to zero or one scattering events. If the
    * step would remain within the current region then a scattering event will
    * take place. If the step would take the electron into a new region then the
    * step ends at the boundary without a scattering event. In either case the
    * electron loses the appropriate amount of energy corresponding to the
    * distance traveled.
    */
   public void takeStep() {

      final double[] pos0 = mElectron.getPosition();

      RegionBase currentRegion = mElectron.getCurrentRegion();
      if((currentRegion == null) || !(currentRegion.getShape().contains(pos0))) {
         /* If it's not where we think it is, find it */
         currentRegion = mChamber.containingSubRegion(pos0);
         mElectron.setCurrentRegion(currentRegion);
         if(currentRegion == null) { // It's not in the chamber
            /*
             * This isn't supposed to be able to happen, so the following line
             * is a good place to set a break point and track down the cause if
             * it ever does. I have seen it happen once-- after approx. 290
             * million electrons were run at over 200 h into a simulation. The
             * following line prevents such errors from halting an otherwise
             * good long-running simulation.
             */
            mElectron.setTrajectoryComplete(true);
            return;
         }
      }

      final IMaterialScatterModel msm = currentRegion.getScatterModel();
      assert msm != null;

      /* Position of possible next scatter event */
      final double[] pos1 = mElectron.candidatePoint(msm.randomMeanPathLength(mElectron));
      /*
       * Shorten step if required by boundary intersection. Get region beyond
       * end of step.
       */
      final RegionBase nextRegion = currentRegion.findEndOfStep(pos0, pos1);
      // Move the electron and update its energy.
      mElectron.move(pos1, msm.calculateEnergyLoss(distance(pos0, pos1), mElectron));
      final boolean tc = (mElectron.getEnergy() < msm.getMinEforTracking()) || mElectron.isTrajectoryComplete();
      mElectron.setTrajectoryComplete(tc);
      if(!tc)
         /*
          * The step ended at pos1 for one of 3 reasons: scattering event,
          * boundary intersection, chamber wall intersection.
          */
         if(nextRegion == currentRegion) {
            assert mChamber != null;
            assert mElectron != null;
            assert currentRegion != null;
            // assert mChamber.containingSubRegion(mElectron.getPosition()) ==
            // currentRegion :
            // mChamber.containingSubRegion(mElectron.getPosition()).toString()
            // + "!=" + currentRegion.toString() + " " +
            // Arrays.toString(mElectron.getPosition());
            fireEvent(ScatterEvent);
            final Electron secondary = msm.scatter(mElectron);
            fireEvent(PostScatterEvent);
            /*
             * In some models scattering may reduce PE energy with or without SE
             * generation. We must check in case there is a decrease that puts
             * the PE below cutoff. Some "trapping" models may set the
             * trajectory complete; in the following step we make sure we don't
             * reverse that decision.
             */
            mElectron.setTrajectoryComplete((mElectron.getEnergy() < msm.getMinEforTracking())
                  || mElectron.isTrajectoryComplete());

            if(secondary != null)
               trackSecondaryElectron(secondary);

            assert mElectron.getCurrentRegion() == currentRegion;
         } else if(nextRegion != null) { // Hit boundary into another region
            fireEvent(NonScatterEvent);
            // Compute barrier scattering.
            final Electron secondary = msm.barrierScatter(mElectron, nextRegion);
            /*
             * At this point our primary and secondary (if any) electrons are on
             * the boundary. Shift the PE a small amount in the direction of its
             * trajectory to resolve any ambiguities arising due to machine
             * precision. Do the same for the SE below.
             */
            mElectron.setPosition(mElectron.candidatePoint(SMALL_DISP));
            /*
             * On rare occasions, such as when our electron is within SMALL_DISP
             * of a corner, the above displacement can move the electron outside
             * its current region. This leaves the current region inconsistent
             * with its position.
             */
            if(!(mElectron.getCurrentRegion().getShape().contains(mElectron.getPosition())))
               /* If it's not where we think it is, find it */
               mElectron.setCurrentRegion(mChamber.containingSubRegion(mElectron.getPosition()));

            /*
             * SE region and PE region need not be the same. (Since they are on
             * the boundary and moving in different directions they may be in
             * different regions.) We must check for ExitMaterialEvent before
             * replacing the PE by the SE.
             */
            if(mElectron.getCurrentRegion() != currentRegion)
               fireEvent(ExitMaterialEvent);
            if(secondary != null) { // Replace PE by SE and track it.
               // Start the secondary a small displacement from the boundary
               secondary.setPosition(secondary.candidatePoint(SMALL_DISP));
               trackSecondaryElectron(secondary);
            }
         } else { // Hit chamber wall
            fireEvent(BackscatterEvent);
            // Tell runTrajectory() to drop this electron
            mElectron.setCurrentRegion(null);
            mElectron.setTrajectoryComplete(true);
         }
   }

   /**
    * This method takes the current Electron and stores it on a stack, the
    * argument Electron becomes the active Electron and will be tracked until
    * either it meets the end of trajectory criteria or it is replaced by
    * another call to trackSecondaryElectron. When the argument Electron ends
    * its trajectory it will be released and the Electron stack will be checked.
    * If there exists an Electron on the Electron stack then the Electron will
    * be popped from the top of the Electron stack. It will become the active
    * electron and will be tracked. The incident Electron trajectory will end
    * when all Electrons added using <code>trackSecondaryElectron</code> have
    * completed their trajectories.
    * 
    * @param newElectron
    */
   public void trackSecondaryElectron(Electron newElectron) {
      final double mMinEnergy = newElectron.getCurrentRegion().getScatterModel().getMinEforTracking();
      if(newElectron.getEnergy() > mMinEnergy) {
         // fireEvent(StartSecondaryEvent);
         mElectronStack.add(mElectron);
         mElectron = newElectron;
         fireEvent(StartSecondaryEvent);
         /*
          * Note: I've moved the firing of this event from its former position
          * (above) to here so my trajectory logger will record info about the
          * SE when this event is fired (instead of info about the PE, info
          * already recorded when the ScatterEvent was just called on that
          * electron). For now this note makes a record of the change in case
          * it's necessary to reverse it. -JV
          */
      }
   }

   /**
    * Returns the generation of Electron that is currently being tracked. 0 for
    * primary electron, 1 for secondary (generated by the primary), 2 for
    * tertiary (generated by the secondary) etc. During a StartSecondaryEvent
    * getElectronGeneration will return the generation of the electron
    * generating the secondary (the parent). During a EndSecondaryEvent
    * getElectronGeneration will return the generation of the secondary electron
    * that is about to die. All other times getElectronGeneration will return
    * the generation of the current active Electron which is also the highest
    * generation Electron currently in existence.
    * 
    * @return int 0-&gt;Primary, 1-&gt;Secondary, 2-&gt;Tertiary, ...
    */
   public int getElectronGeneration() {
      return mElectronStack.size();
   }

   /**
    * Register a class that implements the ActionListener abstract class.
    * Appropriate examples include MCSS_XRayEventListener, MCSS_BackscatterStats
    * and MCSS_BremsstrahlungEventListener. Listeners are prepended to the list,
    * so the last added is the first polled when there are events.
    * 
    * @param sel ActionListener
    */
   public void addActionListener(ActionListener sel) {
      /*
       * The new listener is put at the front of the list, so they are
       * last-added, first-called
       */
      mEventListeners.add(0, sel);
   }

   /**
    * Remove an event handler that was registered using addActionListener
    * 
    * @param sel ActionEvent
    */
   public void removeActionListener(ActionListener sel) {
      mEventListeners.remove(sel);
   }

   /**
    * Have all the electron trajectories resulting from the most recent incident
    * electron completed their trajectories? If one trajectory has finished and
    * another Electron exists on the Electron stack, this method will remove an
    * Electron from the stack and reinitiate tracking it.
    * 
    * @return boolean
    */
   public boolean allElectronsComplete() {
      /*
       * When SE are generated, trackSecondaryElectron() puts the parent onto
       * the stack without checking whether the parent may itself have completed
       * its trajectory. (The energy loss in creating the SE may have dropped
       * its energy below the tracking threshold.) Consequently, we check each
       * electron except for the last one as we take it off the stack, firing an
       * EndSecondaryEvent if necessary. If the last one is also complete, we do
       * not fire an event here. Instead, runTrajectory() will fire a
       * TrajectoryEndEvent.
       */
      boolean tc = mElectron.isTrajectoryComplete();
      while(tc && (mElectronStack.size() > 0)) {
         fireEvent(EndSecondaryEvent);
         mElectron = mElectronStack.remove(mElectronStack.size() - 1);
         tc = mElectron.isTrajectoryComplete();
      }
      return tc;
   }

   /**
    * Run a single trajectory starting the electron at the gun position and
    * tracking it through steps until trajectoryComplete returns true. If this
    * electron generates child, grandchild etc electrons (via
    * <code>trackSecondaryElectron</code>then all of these electrons will be
    * tracked to completion before runTrajectory will return.
    */
   public void runTrajectory() {
      initializeTrajectory();
      fireEvent(TrajectoryStartEvent);
      while(!allElectronsComplete())
         takeStep();
      fireEvent(TrajectoryEndEvent);
   }

   /**
    * Run n complete electron trajectories.
    * 
    * @param n int
    */
   public void runMultipleTrajectories(int n) {
      fireEvent(FirstTrajectoryEvent);
      for(int i = 0; i < n; ++i)
         runTrajectory();
      fireEvent(LastTrajectoryEvent);
   }

   /**
    * Returns the incident electron beam energy (in Joules)
    * 
    * @return double
    */
   public double getBeamEnergy() {
      return mGun.getBeamEnergy();
   }

   /**
    * Specify a class that implements the ElectronGun interface. This method
    * takes the current beam energy and copies it into the new electron gun.
    * (Previous versions had also copied the beam center but this anti-feature
    * has been removed.)
    * 
    * @param gun ElectronGun
    */
   public void setElectronGun(ElectronGun gun) {
      gun.setBeamEnergy(mGun.getBeamEnergy());
      mGun = gun;
   }

   public ElectronGun getElectronGun() {
      return mGun;
   }

   /**
    * Set the incident electron beam energy (in Joules)
    * 
    * @param beamEnergy double
    */
   public void setBeamEnergy(double beamEnergy) {
      mGun.setBeamEnergy(beamEnergy);
      fireEvent(BeamEnergyChanged);
   }

   /**
    * Computes the coordinates of a detector placed at the specified elevation
    * angle at an azimuthal angle of 0. This function is designed to place the
    * detector according to the microanalysis conventions for specifying the
    * take-off angle assuming that the beam starts on the z-axis at a negative
    * z-value heading towards positive z.
    * 
    * @param elevation double
    * @param theta double
    * @return double[] - A 3D coordinate
    */
   public double[] computeDetectorPosition(double elevation, double theta) {
      final double frac = 0.999;
      double r = frac * ChamberRadius;
      if(mChamber.mShape instanceof Sphere)
         r = frac * ((Sphere) mChamber.mShape).getRadius();
      return ElectronProbe.computePosition(0.0, elevation, theta, r);
   }

   /**
    * Get the current active electron.
    * 
    * @return Electron
    */
   public Electron getElectron() {
      return mElectron;
   }

   /**
    * Get a list of the atomic shells that may be ionized by a beam of the
    * current initial beam energy. The method takes into account all the
    * elements in all the regions within the chamber Region.
    * 
    * @return Set - A set of AtomicShell objects.
    * @throws EPQException
    */
   public Set<AtomicShell> getAtomicShellSet()
         throws EPQException {
      final Set<AtomicShell> res = new TreeSet<AtomicShell>();
      final Set<Element> elements = mChamber.getElements(true);
      for(final Object element : elements) {
         final Element el = (Element) element;
         for(int sh = AtomicShell.K; sh < AtomicShell.NI; ++sh) {
            final AtomicShell shell = new AtomicShell(el, sh);
            final double ee = shell.getEdgeEnergy();
            if((ee > 0.0) && (ee < mGun.getBeamEnergy()) && (XRayTransition.getStrongestLine(shell) != null))
               res.add(shell);
         }
      }
      return res;
   }

   /**
    * Returns a complete list of Element objects used in all Regions in the
    * sample
    * 
    * @return Set&lt;Element&gt;
    */
   public Set<Element> getElementSet() {
      return mChamber.getElements(true);
   }

   /**
    * Estimate the volume that will be occupied by electron paths by running the
    * simulation a number of times and looking for the largest extent.
    * 
    * @param c0 double[]
    * @param c1 double[]
    */
   public void estimateTrajectoryVolume(double[] c0, double[] c1) {
      c0[0] = (c0[1] = (c0[2] = Double.MAX_VALUE));
      c1[0] = (c1[1] = (c1[2] = -Double.MAX_VALUE));
      final int nTraj = 100;
      mDisableEvents = true;
      for(int i = 0; i < nTraj; ++i) {
         initializeTrajectory();
         while(!mElectron.isTrajectoryComplete()) {
            takeStep();
            final double[] endPt = mElectron.getPosition();
            final RegionBase endRegion = mChamber.containingSubRegion(endPt);
            if((endRegion != null) && (endRegion != mChamber))
               for(int j = 0; j < 3; ++j) {
                  if(endPt[j] < c0[j])
                     c0[j] = endPt[j];
                  if(endPt[j] > c1[j])
                     c1[j] = endPt[j];
               }
         }
      }
      mDisableEvents = false;
   }

   /**
    * Recurse through the sample replacing all references to oldMat with
    * references to newMat. This is useful for running an identical geometry
    * with different materials.
    * 
    * @param oldMat
    * @param newMat
    */
   public void updateMaterial(Material oldMat, Material newMat)
         throws EPQException {
      mChamber.updateMaterial(oldMat, new BasicMaterialModel(newMat));
   }

   /**
    * Recurse through the sample replacing all references to oldMat with
    * references to the Material specified by the IMaterialScatteringModel
    * instance. This is useful for running an identical geometry with different
    * materials.
    * 
    * @param oldMat Material
    * @param newMsm IMaterialScatterModel
    */
   public void updateMaterial(IMaterialScatterModel oldMat, IMaterialScatterModel newMsm) {
      mChamber.updateMaterial(oldMat, newMsm);
   }

   /**
    * Rotate all the regions inside the chamber (but not the chamber). See
    * Transform3D.rotate for a description. The rotations are performed in the
    * order phi around the z-axis, theta around the y-axis and then psi around
    * the z-axis.
    * 
    * @param pivot The center of the rotation
    * @param phi Angle around the z-axis
    * @param theta Angle around the y-axis
    * @param psi Angle around the z-axis
    */
   public void rotate(double[] pivot, double phi, double theta, double psi) {
      for(final RegionBase r : mChamber.getSubRegions())
         if(r instanceof TransformableRegion)
            ((TransformableRegion) r).rotate(pivot, phi, theta, psi);
   }

   /**
    * Translate all the regions within the chamber by the specified distance (3
    * vector)
    * 
    * @param distance
    */
   public void translate(double[] distance) {
      for(final RegionBase r : mChamber.getSubRegions())
         if(r instanceof TransformableRegion)
            ((TransformableRegion) r).translate(distance);
   }

   public RegionBase findRegionContaining(double[] point) {
      return mChamber.containingSubRegion(point);
   }

}
