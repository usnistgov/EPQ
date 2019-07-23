package gov.nist.nanoscalemetrology.JMONSEL;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;

/**
 * <p>
 * The TrajectoryLoggingListener class provides a mechanism to dump the electron
 * trajectories to a tab-separated text file. It is a modified version of the
 * DiagnosticListener class in NISTMonte. Each line represents a step. Most
 * steps represent scattering but some will represent boundary crossing. The
 * information in each line differs somewhat from DiagnosticListener and it
 * provides a facility to stop logging after a specified number of trajectories.
 * Logging may also be suspended by invoking setSuspended(true) and later
 * resumed. Thus, the logged trajectories need not be the first ones.
 * </p>
 * <p>
 * The log produced by this class is a column-oriented text file with column
 * labels:<br>
 * "Trajectory #","Electron ID","PE ID", "Step index", "Depth", "X-coord",
 * "Y-coord", "Z-coord", "Event type", "Energy","Event Descriptor"
 * </p>
 * <p>
 * Trajectory # is a counter that is 1 for the first incident electron and all
 * its offspring, 2 for the second, etc. Electron ID is a unique identifier
 * assigned to each electron when it is created. PE ID is the electron ID of
 * this electron's parent. Electrons with no parent (i.e., those produced by the
 * electron gun) have PE ID = 0. Step index counts trajectory steps (or legs)
 * for this electron. Depth is 0 for electrons created by the gun, 1 for 1st
 * generation secondaries, 2 for 2nd generation, etc. The x, y, and z
 * coordinates are the coordinates (in meters) at the end of the step. Event
 * type is MonteCarloSS's event number for the event that triggered this call to
 * the TrajectoryLoggingListener. Energy is the electron's kinetic energy, in
 * keV, at the end of the step. Event Descriptor carries (almost) the same
 * information as "Event type", but in a human-readable text form rather than a
 * number.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author Nicholas W. M. Ritchie, John Villarrubia
 * @version 1.0
 */
public class TrajectoryLoggingListener
   implements ActionListener {
   private int linecount = 0;
   private final MonteCarloSS mMonte;
   private final PrintWriter mWriter;
   private int mDepth;
   private long maxTrajectories = 100;
   private long trajectoryCount = 0;
   private boolean suspended = false;

   /**
    * Constructs a TrajectoryLoggingListener
    *
    * @param mcss
    * @param os
    */
   public TrajectoryLoggingListener(MonteCarloSS mcss, OutputStream os) {
      super();
      mMonte = mcss;
      reset();
      mWriter = new PrintWriter(os);
      mWriter.print("Traj#\tID\tPEID\tStep#\tDepth\tx\ty\tz\tEvent\t");
      mWriter.println("Energy\tDescriptor");
   }

   public TrajectoryLoggingListener(MonteCarloSS mcss, File f)
         throws FileNotFoundException {
      this(mcss, new FileOutputStream(f));
   }

   public TrajectoryLoggingListener(MonteCarloSS mcss, String filename)
         throws FileNotFoundException {
      this(mcss, new FileOutputStream(filename));
   }

   /** Sets trajectoryCount, linecount, and depth to 0. */
   public void reset() {
      trajectoryCount = 0;
      linecount = 0;
      mDepth = 0;
   }

   /** close -- Flushes remaining output to the log file and closes it. */
   public void close() {
      mWriter.flush();
      mWriter.close();
   }

   /**
    * actionPerformed
    *
    * @param arg0 (non-Javadoc)
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   @Override
   public void actionPerformed(ActionEvent arg0) {
      if((trajectoryCount <= maxTrajectories) && (suspended == false)) {
         assert arg0.getSource() == mMonte;
         final int event = arg0.getID();
         boolean output = true;
         String name = "";
         switch(event) {
            case MonteCarloSS.ScatterEvent:
               name = "Scatter";
               break;
            case MonteCarloSS.NonScatterEvent:
               name = "Non-scatter";
               break;
            case MonteCarloSS.BackscatterEvent:
               name = "Backscatter";
               break;
            case MonteCarloSS.ExitMaterialEvent:
               name = mMonte.getElectron().getPreviousRegion().getMaterial().toString() + " to "
                     + mMonte.getElectron().getCurrentRegion().getMaterial().toString();
               break;
            case MonteCarloSS.TrajectoryStartEvent:
               trajectoryCount++;
               if(trajectoryCount > maxTrajectories)
                  output = false;
               else
                  name = "Start Trajectory";
               break;
            case MonteCarloSS.TrajectoryEndEvent:
               output = true;
               name = "End Trajectory";
               break;
            case MonteCarloSS.LastTrajectoryEvent:
               name = "End Last Trajectory";
               break;
            case MonteCarloSS.FirstTrajectoryEvent:
               // trajectoryCount = 0;
               output = false;
               break;
            case MonteCarloSS.StartSecondaryEvent:
               ++mDepth;
               name = "Start SE";
               break;
            case MonteCarloSS.EndSecondaryEvent:
               --mDepth;
               name = "End SE";
               break;
            case MonteCarloSS.PostScatterEvent:
               name = "PostScatter";
               break;
            case MonteCarloSS.BeamEnergyChanged:
               mWriter.print("\"Beam energy changed to\"\t");
               mWriter.print(FromSI.keV(mMonte.getBeamEnergy()));
               mWriter.println(" keV");
               output = false;
               break;
         }
         if(output) {
            final StringBuffer sb = new StringBuffer();
            final Electron e = mMonte.getElectron();
            sb.append(trajectoryCount);
            sb.append("\t");
            sb.append(e.getIdent());
            sb.append("\t");
            sb.append(e.getParentID());
            sb.append("\t");
            sb.append(e.getStepCount());
            sb.append("\t");
            sb.append(mDepth);
            sb.append("\t");
            final double[] pos = e.getPosition();
            sb.append(String.format("%.7g\t", pos[0]));
            // sb.append("\t");
            // sb.append(pos[1]);
            sb.append(String.format("%.7g\t", pos[1]));
            // sb.append("\t");
            sb.append(String.format("%.7g\t", pos[2]));
            // sb.append("\t");
            sb.append(event);
            sb.append("\t");
            sb.append(String.format("%.7g\t", FromSI.eV(e.getEnergy())));
            // sb.append("\t");
            sb.append(name);
            mWriter.println(sb.toString());
            linecount++;
            if((linecount % 10) == 0)
               mWriter.flush();
         }
      }
   }

   /**
    * setMaxTrajectories - Sets the maximum number of trajectories to log.
    *
    * @param max int
    */
   public void setMaxTrajectories(int max) {
      maxTrajectories = max;
   }

   /**
    * Returns true if logging is suspended, false otherwise.
    *
    * @return - true if logging is currently suspended, false otherwise.
    */
   public boolean isSuspended() {
      return suspended;
   }

   /**
    * Setting suspended == false suspends logging. Setting it to true resumes.
    * suspended == false by default.
    *
    * @param suspended
    */
   public void setSuspended(boolean suspended) {
      if(this.suspended != suspended)
         this.suspended = suspended;
   }

}
