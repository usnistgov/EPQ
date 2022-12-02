/**
 * gov.nist.nanoscalemetrology.JMONSEL.DepositedEnergyListener Created by:
 * jvillar Date: Mar 23, 2015
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;

import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape;

/**
 * <p>
 * While active, this listener keeps an internal record of energy deposition
 * events. These events represent energy lost to the material at the event's
 * location in a form that can effect change (e.g., expose resist or generate
 * heat). The detector's record can subsequently be interrogated, for example to
 * determine the deposited energy within a desired volume.
 * </p>
 * <p>
 * For this purpose, energy deposition includes (1) energy lost by a primary
 * electron (PE) due to a continuous slowing down model associated with its
 * motion, (2) energy lost in inelastic scattering events, and (3) when an
 * electron is dropped from further simulation, the difference between whatever
 * energy it had remaining at that time and the energy of a fully equilibrated
 * electron in the material at its location. (For example, in a material with a
 * partially filled conduction band, the equilibrated electron is assumed to
 * retain a kinetic energy equal to the Fermi energy. This is not included in
 * the deposited energy.) Secondary electron (SE) generation results in (4) a
 * negative deposit (a withdrawal) of energy that is carried away by the SE. The
 * value that is logged is the net deposit of energy at the location of the
 * scattering.
 * </p>
 * <p>
 * Note that certain changes in PE kinetic energy are not considered to be
 * deposited energy. For example, when an electron transitions between
 * materials, its kinetic energy may increase or decrease due to a difference in
 * the potential energy between materials. Such a decrease or increase is not
 * deemed to be an energy deposit or withdrawal at the boundary crossing,
 * although energy gained at that point may be later lost (and counted) in
 * subsequent events. Kinetic energy changes due to the electron's motion in an
 * electric field may or may not be counted as deposited energy, depending on
 * the order in which listeners are polled when there is an event. If this
 * DepositedEnergyListener is added to MonteCarloSS's list of active listeners
 * after the ChargingListener is added, then this one will be polled first
 * (since MonteCarloSS uses a last added/first polled order). In that case
 * energy that goes into or is taken from the field is not counted as deposited
 * energy. (This is recommended, since energy lost to the field does not heat or
 * cause chemical changes in the sample.) If the ChargingListener is added to
 * the list last, then energy flows into and out of the field are included in
 * the deposited energy.
 * </p>
 * <p>
 * The detector may be polled after a simulation. Methods provide ways to access
 * the internal record of loss events. These include dump(), which creates a
 * possibly very large ASCII file listing each event on a separate line,
 * getDepositedEnergy(Shape s), which is a very general (and slow) method that
 * returns the total deposited energy within any supplied arbitrary s, and
 * getDepositedEnergy(double[] testShapeCenter, double[] testShapeHalfSize)
 * which is a is a much more efficient method for returning deposited energy in
 * box-shaped volumes with sides oriented along the coordinate axes. Other
 * methods return the total number of trajectories started and the number of
 * electrons that exited the chamber while the listener was active. Getters are
 * provided for several totals: energy in, energy out, and
 * sampleDepositedEnergy. These are respectively the total kinetic energy of new
 * electrons at TrajectoryStartEvents, the total kinetic energy of those
 * electrons that exit the chamber, and the total of energy deposit events
 * exclusive of chamber exit events. Usually it is roughly the case that
 * energyIn = energyOut + sampleDepositedEnergy, but this equivalence is only
 * approximate because, as described above, the definition of deposited energy
 * excludes certain energies (electrostatic potential energy, kinetic energy
 * retained by equilibrated electrons, potential energy differences among
 * materials) that would need to be accounted for separately in order to make a
 * detailed energy balance.
 * </p>
 * <p>
 * The internal record of deposited energy is detailed only for events within a
 * rectangular bounding volume supplied to the constructor. This volume is
 * divided in half along each dimension, i.e., into octants, up to a
 * user-settable (Constructor parameter) maxDivisions times. Octants are only
 * instantiated when there is an event for them to hold. When there is a second
 * event, octants are divided as many times as needed to place the events in
 * different octants, except that if this requires more than maxDivisions, the
 * events are summed in the same minimum-sized octant. Thus the spatial
 * resolution of the record in the x direction is the x dimension of the
 * bounding volume divided by 2^maxDivisions, and similarly for the y and z
 * directions. Larger values of maxDivisions thus correspond to better spatial
 * resolution at the cost of higher memory usage.
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
public class DepositedEnergyListener implements ActionListener {

   /**
    * <p>
    * The octree data structure used by DepositedEnergyListener to store events.
    * Each OctEntry stores one or more energy deposition events. It has a
    * deposited energy equal to the sum of the energies of the events it stores.
    * If it stores a single event, it has a position given by the location of
    * the event. If it contains more than one event, it is subdivided in order
    * to place the events in different child OctEntry instances, except that
    * subdivision may not exceed maxDivisions. In that case, events are summed
    * in the same child OctEntry, and the associated position is the center of
    * the child OctEntry.
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
   private class OctEntry {

      private final double[] center;
      private final int generation;
      private OctEntry[] children;
      private double[] pos;
      private double depE = 0.; // Total deposited energy in this OctEntry's
                                // volume

      /**
       * Constructs an OctEntry.
       *
       * @param center
       * @param halfsize
       */
      private OctEntry(double[] center, int generation) {
         super();
         this.center = center.clone();
         this.generation = generation;
      }

      /**
       * Returns true if pos is within the rectangular volume defined by
       * parameters defining intervals along the coordinate axes.
       *
       * @param xmin
       * @param xmax
       * @param ymin
       * @param ymax
       * @param zmin
       * @param zmax
       * @param pos
       * @return
       */
      private boolean contains(double xmin, double xmax, double ymin, double ymax, double zmin, double zmax, double[] pos) {
         if ((xmin <= pos[0]) && (pos[0] < xmax) && (ymin <= pos[1]) && (pos[1] < ymax) && (zmin <= pos[2]) && (pos[2] < zmax))
            return true;
         else
            return false;
      }

      /**
       * Dumps a list of events contained in this OctEntry to the designated
       * PrintWriter. Each event appears on one line in the form of x, y, z,
       * deposited energy in eV.
       *
       * @param os
       */
      public void dump(PrintWriter pw) {
         /* Output data for each detected electron */
         if (children != null) {
            /*
             * This OctEntry has multiple events stored in children.
             */
            for (final OctEntry c : children)
               if (c != null)
                  c.dump(pw);
            return;
         }

         if (pos != null) {
            /*
             * This OctEntry is a leaf. Print pos and depE.
             */
            final StringBuffer sb = new StringBuffer();
            sb.append(pos[0]);
            sb.append("\t");
            sb.append(pos[1]);
            sb.append("\t");
            sb.append(pos[2]);
            sb.append("\t");
            sb.append(FromSI.eV(depE));
            pw.println(sb);
            return;
         }
         /*
          * Arrive here if this OctEntry contains no events. Simply return.
          * (Print nothing.)
          */
      }

      /**
       * Returns this OctEntry's child at the given index, creating it if
       * necessary.
       *
       * @param index
       * @return
       */
      private OctEntry getChild(int index) {
         if (children[index] == null) {
            /* It doesn't exist yet, so generate it. */
            final int childGeneration = generation + 1;
            if (childGeneration > maxDivisions)
               return null;
            final double[] childCenter = center.clone();
            if (index < 4)
               childCenter[0] -= halfSizes[childGeneration][0];
            else
               childCenter[0] += halfSizes[childGeneration][0];
            if ((index < 2) || (index == 4) || (index == 5))
               childCenter[1] -= halfSizes[childGeneration][1];
            else
               childCenter[1] += halfSizes[childGeneration][1];
            if ((index % 2) == 0)
               childCenter[2] -= halfSizes[childGeneration][2];
            else
               childCenter[2] += halfSizes[childGeneration][2];
            children[index] = new OctEntry(childCenter, childGeneration);
         }
         return children[index];
      }

      /**
       * Returns the index of the child of this OctEntry that would contain the
       * point at newpos.
       *
       * @param newpos
       * @param center
       * @return
       */
      private int getChildIndex(double[] newpos) {
         int index = 0;

         if (newpos[0] > center[0])
            index += 4;
         if (newpos[1] > center[1])
            index += 2;
         if (newpos[2] > center[2])
            index += 1;

         return index;
      }

      /**
       * Returns the total deposited energy for events inside this OctEntry
       * (including its children)
       *
       * @return - total deposited energy
       */
      public double getDepositedEnergy() {
         return depE;
      }

      /**
       * Returns the total deposited energy of events that are inside both this
       * OctEntry (including its children) and a test shape that is a
       * rectangular box with sides oriented along the coordinate axes. By thus
       * restricting the shape, the algorithm employed here can ordinarily be
       * much more efficient than the one employed by getDepositedEnergy(Shape
       * shape). The test shape is specified by its center and its halfsize,
       * such that it includes the volume with
       * center[0]-halfsize[0]<x<center[0]+halfsize[0] and similar relations for
       * the y (at index 1) and z (at index 2) coordinates.
       *
       * @param testShapeCenter
       * @param testShapeHalfSize
       * @return
       */
      public double getDepositedEnergy(double[] testShapeCenter, double[] testShapeHalfSize) {
         final double testShapeXmin = testShapeCenter[0] - testShapeHalfSize[0];
         final double testShapeXmax = testShapeCenter[0] + testShapeHalfSize[0];
         final double testShapeYmin = testShapeCenter[1] - testShapeHalfSize[1];
         final double testShapeYmax = testShapeCenter[1] + testShapeHalfSize[1];
         final double testShapeZmin = testShapeCenter[2] - testShapeHalfSize[2];
         final double testShapeZmax = testShapeCenter[2] + testShapeHalfSize[2];

         /*
          * First case to consider: This Octree has only a single event. In this
          * case, if the event is within the test shape we return depE,
          * otherwise 0.
          */
         if (pos != null)
            if (contains(testShapeXmin, testShapeXmax, testShapeYmin, testShapeYmax, testShapeZmin, testShapeZmax, pos))
               return depE;
            else
               return 0.;
         /* Second case: This Octree has no events. Return 0. */
         if (children == null)
            return 0.;

         /*
          * Otherwise, this OctEntry has events extended over a finite volume.
          * There are then 3 relevant cases, depending on whether none, all, or
          * some of the present OctEntry lies within the supplied test shape.
          */

         /* Compute the overlapping region */
         final double xmax = this.center[0] + halfSizes[generation][0];
         final double xmin = this.center[0] - halfSizes[generation][0];
         final double overlapXmax = Math.min(testShapeXmax, xmax);
         final double overlapXmin = Math.max(testShapeXmin, xmin);
         if (overlapXmax < overlapXmin)
            // null interval, there is no overlap
            return 0.;
         final double ymax = this.center[1] + halfSizes[generation][1];
         final double ymin = this.center[1] - halfSizes[generation][1];
         final double overlapYmax = Math.min(testShapeYmax, ymax);
         final double overlapYmin = Math.max(testShapeYmin, ymin);
         if (overlapYmax < overlapYmin)
            // null interval, there is no overlap
            return 0.;
         final double zmax = this.center[2] + halfSizes[generation][2];
         final double zmin = this.center[2] - halfSizes[generation][2];
         final double overlapZmax = Math.min(testShapeZmax, zmax);
         final double overlapZmin = Math.max(testShapeZmin, zmin);
         if (overlapZmax < overlapZmin)
            // null interval, there is no overlap
            return 0.;

         /* If overlap is complete, return depE */
         if ((overlapXmax == xmax) && (overlapXmin == xmin) && (overlapYmax == ymax) && (overlapYmin == ymin) && (overlapZmax == zmax)
               && (overlapZmin == zmin))
            return depE;

         /*
          * Otherwise, overlap is partial. Return the sum of depE from children.
          */
         double result = 0.;
         for (final OctEntry c : children)
            if (c != null)
               result += c.getDepositedEnergy(testShapeCenter, testShapeHalfSize);
         return result;
      }

      /**
       * Returns the total deposited energy of events within this OctEntry
       * (including its children) and a test shape specified by the shape input.
       * Note that because Shapes are very general (e.g., it can even be a union
       * of disjoint shapes) there is no simple efficient algorithm to test
       * whether an entire OctEntry is contained within shape. For this reason,
       * this algorithm must loop through all events, individually testing
       * containment of each one. This method is therefore extremely general,
       * but slow when there are many logged events.
       *
       * @param shape
       * @return
       */
      public double getDepositedEnergy(Shape shape) {
         /*
          * There are 3 relevant possibilities, depending on whether this
          * OctElement contains 1, 0, or multiple events.
          */

         /* Possibility 1: 1 event */
         if (pos != null)
            if (shape.contains(pos))
               return depE;
            else
               return 0.;

         /* Possibility 2: 0 events */
         if (children == null)
            return 0.; // pos==null && children == null means 0 events

         /* Possibility 3: multiple events */
         double result = 0.;
         for (final OctEntry c : children)
            if (c != null)
               result += c.getDepositedEnergy(shape);
         return result;
      }

      /**
       * Returns a text header describing the energy loss events as produced by
       * the dump() method. Note that dump() converts the deposited energy to
       * eV, and this is indicated in the header.
       *
       * @return
       */
      public String getHeader() {
         return "x\ty\tz\tdepositedE (eV)";
      }

      /**
       * Inserts a new event into the octree. The event will go into the first
       * available empty element, or else into a maximally subdivided element if
       * elements are already occupied down to that level.
       *
       * @param data
       * @return - true if insertion succeeds, false if it fails (because data
       *         is outside this OctEntry's volume.
       */
      public boolean insert(double[] newpos, double newDepE) {

         if (children != null) {
            /*
             * This OctEntry is not a leaf. Decide which child should contain
             * this event and add it.
             */
            final int index = getChildIndex(newpos);
            final boolean result = getChild(index).insert(newpos, newDepE);
            if (result)
               depE += newDepE;
            return result;
         }

         // Make sure the coordinate is inside our volume.
         if (!isInside(newpos))
            return false;

         /*
          * Arrive here if the event represented by data is inside this
          * OctEntry's volume
          */

         /* If this is the first time we get data, do this */
         if (pos == null) { // No data yet
            pos = newpos.clone();
            depE = newDepE;
            return true;
         }

         /* We already have data */

         /*
          * It often happens that 2 events occur at exactly the same position.
          * This happens when an SE is generated; the PE deposits energy and the
          * SE carries some of it away. It also happens when the PE suffers any
          * loss that puts it below the minimum energy for tracking, since then
          * it is immediately dropped and loses its remaining energy. Since this
          * has a high probability of happening, rather than subdivide our
          * Octree down to the resolution limit every time, we check for it and
          * combine events immediately when it happens.
          */
         if ((newpos[0] == pos[0]) && (newpos[1] == pos[1]) && (newpos[2] == pos[2])) {
            depE += newDepE;
            // leave pos as is, since they're the same
            return true;
         }

         /*
          * If we cannot subdivide this OctEntry, combine this event with our
          * others
          */
         if (generation == maxDivisions) {
            depE += newDepE;
            pos = center;
            return true;
         }

         /* Otherwise, we can subdivide it */

         children = new OctEntry[8];

         /* Put the existing data into the appropriate child */
         int index = getChildIndex(pos);
         boolean result = getChild(index).insert(pos, depE);
         /*
          * We don't increment depE because the existing depE already includes
          * this event.
          */
         assert result;
         pos = null;
         /* Put the new data into the appropriate child */
         index = getChildIndex(newpos);
         result = getChild(index).insert(newpos, newDepE);

         if (result)
            depE += newDepE; // This time we must increment
         return result;
      }

      /**
       * Returns true if pos is inside the bounding box associated with this
       * OctEntry, false otherwise.
       *
       * @param pos
       * @return
       */
      public boolean isInside(double[] pos) {
         if ((pos[0] <= (center[0] - halfSizes[generation][0])) || (pos[0] > (center[0] + halfSizes[generation][0])))
            return false;
         if ((pos[1] <= (center[1] - halfSizes[generation][1])) || (pos[1] > (center[1] + halfSizes[generation][1])))
            return false;
         if ((pos[2] <= (center[2] - halfSizes[generation][2])) || (pos[2] > (center[2] + halfSizes[generation][2])))
            return false;
         return true;
      }
   }

   static private int maxDivisions = 0;

   static private double[][] halfSizes;

   private final MonteCarloSS mMonte;

   private final OctEntry mLog;
   /*
    * nTraj will keep count of the number of trajectories that start while the
    * detector is active.
    */
   private long nTraj = 0;

   /*
    * energyIn will keep a running total of energy supplied to electrons at
    * trajectory start
    */
   private double energyIn = 0.;
   /*
    * nOut will keep count of the number of electrons that hit the chamber wall
    * while the detector is active
    */
   private long nOut = 0;
   /*
    * energyOut keeps a running total of energy of SE and BSE that hit the
    * chamber wall
    */
   private double energyOut = 0.;
   /*
    * deposited energy keeps a running total of all deposited energy. This
    * includes events also recorded in energyOut, so depositedEnergy-energyOut
    * is the energy left inside the chamber somewhere. (This will be somewhere
    * in the sample, or possibly in RegionDetectors outside the sample.
    */
   private double depositedEnergy = 0;

   /**
    * <p>
    * Constructs a DepositedEnergyListener. While active (instantiated and
    * attached to a running MonteCarloSS by by the
    * addActionListener(ActionListener) method), it keeps an internal record of
    * the location and size of energy loss events that occur within a
    * rectangular bounding box with sides oriented along the coordinate axes and
    * location defined by diagonally opposite corners (xmin,ymin,zmin) and
    * (xmax,ymax,zmax). Events that occur outside of the supplied bounding box
    * are not logged, although the energy associated with them is included in
    * global totals that can be accessed by some of the getters.
    * </p>
    * <p>
    * Considerations for choosing the size of the bounding box: This is a
    * trade-off between level of detail and performance. If a map (e.g., contour
    * plot) of energy deposition with position will be needed, the bounding box
    * should enclose the entire region that will be included in the plot. Making
    * the bounding box larger than this gives some flexibility (you can change
    * your mind without re-running the simulation) but will require more
    * storage. Increasing the volume increases the amount of storage in
    * proportion to the additional energy loss events that occur in the added
    * volume.
    * </p>
    * <p>
    * The bounding box will be bisected along each of its dimensions as
    * necessary to that each loss event is stored in its own subdivision, except
    * that the number of such subdivisions is limited to maxDivisions, after
    * which multiple events within the same minimal octant are summed.
    *
    * @param mcss
    *           - the MonteCarloSS instance with which this listener is
    *           associated
    * @param xmin
    *           - x coordinate of minimum diagonal corner that defines bounding
    *           box
    * @param xmax
    *           - y coordinate of minimum diagonal corner that defines bounding
    *           box
    * @param ymin
    *           - z coordinate of minimum diagonal corner that defines bounding
    *           box
    * @param ymax
    *           - x coordinate of maximum diagonal corner that defines bounding
    *           box
    * @param zmin
    *           - y coordinate of maximum diagonal corner that defines bounding
    *           box
    * @param zmax
    *           - z coordinate of maximum diagonal corner that defines bounding
    *           box
    * @param maxDivisions
    *           - the maximum number of times the bounding box may be bisected
    *           along each of its dimensions.
    */
   public DepositedEnergyListener(MonteCarloSS mcss, double xmin, double xmax, double ymin, double ymax, double zmin, double zmax, int maxDivisions) {
      this.mMonte = mcss;
      final double[] center = new double[]{(xmin + xmax) / 2., (ymin + ymax) / 2., (zmin + zmax) / 2.};
      halfSizes = new double[maxDivisions + 1][];
      halfSizes[0] = new double[]{(xmax - xmin) / 2., (ymax - ymin) / 2., (zmax - zmin) / 2.,};
      for (int i = 1; i <= maxDivisions; i++)
         halfSizes[i] = new double[]{halfSizes[i - 1][0] / 2., halfSizes[i - 1][1] / 2., halfSizes[i - 1][2] / 2.};
      DepositedEnergyListener.maxDivisions = maxDivisions;
      mLog = new OctEntry(center, 0);
   }

   /**
    * @param ae
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   @Override
   public void actionPerformed(ActionEvent ae) {
      assert (ae.getSource() instanceof MonteCarloSS);
      assert (ae.getSource() == mMonte);
      switch (ae.getID()) {
         case MonteCarloSS.TrajectoryStartEvent : {
            synchronized (this) {
               final MonteCarloSS mcss = (MonteCarloSS) ae.getSource();
               final Electron el = mcss.getElectron();
               energyIn += el.getEnergy();
               nTraj += 1;
            }
         }
            break;

         case MonteCarloSS.BackscatterEvent : {
            synchronized (this) {
               final MonteCarloSS mcss = (MonteCarloSS) ae.getSource();
               final Electron el = mcss.getElectron();
               energyOut += el.getEnergy();
               nOut += 1;
            }
         }
            break;

         case MonteCarloSS.PostScatterEvent :
         case MonteCarloSS.NonScatterEvent :
            /*
             * When the PE moves, its deposited energy is just the difference
             * between its initial and final energies.
             */
            synchronized (this) {
               /* Deposited energy = deltaE */
               final MonteCarloSS mcss = (MonteCarloSS) ae.getSource();
               final Electron el = mcss.getElectron();
               final double kE = el.getEnergy();
               final double depositedE = el.getPreviousEnergy() - kE;
               if (depositedE != 0.) {
                  mLog.insert(el.getPosition(), depositedE);
                  depositedEnergy += depositedE;
               }
            }
            break;

         case MonteCarloSS.TrajectoryEndEvent :
         case MonteCarloSS.EndSecondaryEvent :
            /*
             * When the electron stops, its deposited energy is the difference
             * between its initial and final energies. The final energy is
             * assumed to be EFermi (if the conduction band has electrons in it,
             * such that EFermi > 0) or else 0 (if the conduction band has no
             * electrons/EFermi < 0 in JMONSEL's convention and we assume our
             * electron ends up at the bottom of the conduction band, where kE =
             * 0.)
             */
            synchronized (this) {
               /* Deposited energy = E - max(EF,0) */
               final MonteCarloSS mcss = (MonteCarloSS) ae.getSource();
               final Electron el = mcss.getElectron();
               final double kE = el.getEnergy();
               final RegionBase reg = el.getCurrentRegion();
               double depositedE;
               if (reg != null) {
                  final Material mat = reg.getMaterial();
                  if (mat instanceof SEmaterial)
                     depositedE = kE - Math.max(((SEmaterial) mat).getEFermi(), 0.);
                  else
                     depositedE = kE;
               } else
                  // Arrive here if reg == null (backscattered electron)
                  depositedE = kE;
               mLog.insert(el.getPosition(), depositedE);
               depositedEnergy += depositedE;
            }
            break;
         case MonteCarloSS.StartSecondaryEvent :
            /*
             * The SE takes away the difference between its kinetic energy and
             * the Fermi energy.
             */
            synchronized (this) {
               /*
                * deposited energy is negative: Removed energy = ESE - max(EF,0)
                */
               final MonteCarloSS mcss = (MonteCarloSS) ae.getSource();
               final Electron secEl = mcss.getElectron();
               final double kE = secEl.getEnergy();
               final Material mat = secEl.getCurrentRegion().getMaterial();
               double depositedE;
               if (mat instanceof SEmaterial)
                  depositedE = Math.max(((SEmaterial) mat).getEFermi(), 0.) - kE;
               else
                  depositedE = -kE;
               mLog.insert(secEl.getPosition(), depositedE);
               depositedEnergy += depositedE;
            }
            break;

         default :
            break;

      }
   }

   /**
    * Writes a log of energy deposition events to the specified output stream.
    * Each line of the log contains the position and an amount of energy
    * deposited (negative if energy is withdrawn). These pertain to one event or
    * the sum of events within each of the smallest permitted subdivisions of
    * the bounding box. Note that there are typically many such events for each
    * electron, so this file can be very large unless the listener is active for
    * only a small number of electrons.
    *
    * @param os
    */
   public void dump(OutputStream os) {
      final NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(3);
      final PrintWriter pw = new PrintWriter(os);
      /* Output data for each detected electron */
      // pw.println("Number of logged events: " +
      // Integer.toString(mLog.size()));
      pw.println(mLog.getHeader());
      mLog.dump(pw);
      pw.close();
   }

   /**
    * Returns the total energy deposited of all energy deposition events within
    * the bounding box supplied to the constructor.
    *
    * @return - the total deposited energy of events within the supplied shape
    */
   public double getDepositedEnergy() {
      return mLog.getDepositedEnergy();
   }

   /**
    * Returns the total energy deposited of all recorded energy deposition
    * events that occurred within the supplied shape. The shape is restricted to
    * a box with sides oriented along the coordinate axes. It is specified by
    * its center coordinate and the half-lengths along each of the x, y, and z
    * directions. Because of this restriction this method can use a much faster
    * algorithm than the one employed by getDepositedEnergy(Shape shape).
    *
    * @param testShapeCenter
    *           - [x, y, z] coordinates of the center of the box that contains
    *           the energy deposit events that will be summed.
    * @param testShapeHalfSize
    *           - half the length of the bounding box along each of the
    *           coordinate axis directions.
    * @return - the total deposited energy of events within the box described by
    *         the supplied parameters.
    */
   public double getDepositedEnergy(double[] testShapeCenter, double[] testShapeHalfSize) {
      return mLog.getDepositedEnergy(testShapeCenter, testShapeHalfSize);
   }

   /**
    * Returns the total energy deposited of all recorded energy deposition
    * events that occurred within the supplied shape. The algorithm employed by
    * this method is quite general (Shapes have few restrictions) but much
    * slower than the one employed by getDepositedEnergy(double[]
    * testShapeCenter, double[] testShapeHalfSize).
    *
    * @param shape
    * @return - the total deposited energy of events within the supplied shape
    */
   public double getDepositedEnergy(Shape shape) {
      return mLog.getDepositedEnergy(shape);
   }

   /**
    * Returns the total kinetic energy of electron trajectories started during
    * the life of this listener.
    *
    * @return - total kinetic energy input to the system by new electron
    *         trajectories
    */
   public double getEnergyIn() {
      return energyIn;
   }

   /**
    * Returns the total kinetic energy of backscattered and secondary electrons
    * that reach the chamber wall during the life of this listener.
    *
    * @return - total kinetic energy carried out of the chamber by exiting
    *         electrons
    */
   public double getEnergyOut() {
      return energyOut;
   }

   /**
    * Returns the number of electrons that hit the chamber wall while this
    * listener was active.
    *
    * @return - number of electrons exiting the chamber
    */
   public long getNumOut() {
      return nOut;
   }

   /**
    * Returns the number of electron trajectories that started while this
    * listener was active.
    *
    * @return - number of trajectories
    */
   public long getNumTrajectories() {
      return nTraj;
   }

   /**
    * Returns the total of all deposited energy events inside the chamber (i.e.,
    * not including the energy of electrons that hit the chamber wall). Unlike
    * getDepositedEnergy(), the returned energy includes energy deposited in the
    * sample outside of the bounding box specified to the constructor.
    *
    * @return - energy deposited in the interior of the chamber
    */
   public double sampleDepositedEnergy() {
      return depositedEnergy - energyOut;
   }

}
