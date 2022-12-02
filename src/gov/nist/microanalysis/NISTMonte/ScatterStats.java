package gov.nist.microanalysis.NISTMonte;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.Utility.DescriptiveStatistics;
import gov.nist.microanalysis.Utility.Math2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.NumberFormat;

/**
 * <p>
 * Computes statistics for the number of steps and path length for an electron
 * to drop below a specified energy. Electrons that backscatter are counted but
 * otherwise excluded from the statistics.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class ScatterStats implements ActionListener {
   // Configuration data
   private final AtomicShell mShell;
   private final double mMinE;
   // Runtime bookkeeping
   transient private boolean mAlive;
   transient private double mPathLength;
   // Statistical bookkeeping
   private DescriptiveStatistics mStepCount;
   private DescriptiveStatistics mTrajectoryLength;
   private int mBackscatterCount;

   /**
    * MCSS_ScatterStats - Computes the number of steps required for an electron
    * to drop below the energy required to ionize the specified shell.
    * 
    * @param shell
    *           AtomicShell
    */
   public ScatterStats(AtomicShell shell) {
      mShell = shell;
      mMinE = shell.getEdgeEnergy();
      mBackscatterCount = 0;
   }

   public ScatterStats(double energy) {
      mShell = null;
      mMinE = energy;
      mBackscatterCount = 0;
   }

   /**
    * actionPerformed - Collect the necessary statistics
    * 
    * @param e
    *           ActionEvent
    */
   @Override
   public void actionPerformed(ActionEvent e) {
      assert (e.getSource() instanceof MonteCarloSS);
      final MonteCarloSS mcss = (MonteCarloSS) e.getSource();
      switch (e.getID()) {
         case MonteCarloSS.FirstTrajectoryEvent : {
            mStepCount = new DescriptiveStatistics();
            mTrajectoryLength = new DescriptiveStatistics();
            break;
         }
         case MonteCarloSS.TrajectoryStartEvent :
            // Start a new trajectory
            mAlive = true;
            mPathLength = 0.0;
            break;
         case MonteCarloSS.TrajectoryEndEvent :
            if (mAlive) {
               final Electron ee = mcss.getElectron();
               mStepCount.add(ee.getStepCount() - 1);
               mTrajectoryLength.add(mPathLength);
               mAlive = false;
            }
            break;
         case MonteCarloSS.BackscatterEvent :
            mAlive = false;
            ++mBackscatterCount;
            break;
         case MonteCarloSS.ScatterEvent : {
            if (mAlive) {
               final Electron ee = mcss.getElectron();
               mPathLength += Math2.distance(ee.getPrevPosition(), ee.getPosition());
               if ((ee.getEnergy() < mMinE) || (e.getID() == MonteCarloSS.TrajectoryEndEvent)) {
                  mStepCount.add(ee.getStepCount() - 1);
                  mTrajectoryLength.add(mPathLength);
                  mAlive = false;
               }
            }
            break;
         }
      }
   }

   /**
    * Returns a DescriptiveStatistics object summarizing the step statistics
    * 
    * @return DescriptiveStatistics
    */
   public DescriptiveStatistics getStepStatistics() {
      return mStepCount;
   }

   /**
    * Returns a DescriptiveStatistics object summarizing the trajectory length
    * statistics
    * 
    * @return DescriptiveStatistics
    */
   public DescriptiveStatistics getTrajectoryStatistics() {
      return mTrajectoryLength;
   }

   /**
    * getMaximum - Returns the maximum number of steps required to drop below
    * the specified energy.
    * 
    * @return double
    */
   public int getMaximum() {
      return (int) Math.round(mStepCount.maximum());
   }

   /**
    * getMinimum - Returns the minimum number of steps required to drop below
    * the specified energy.
    * 
    * @return double
    */
   public int getMinimum() {
      return (int) Math.round(mStepCount.minimum());
   }

   /**
    * getAverage - Returns the average number of steps required to drop below
    * the specified energy.
    * 
    * @return double
    */
   public double getAverage() {
      return mStepCount.average();
   }

   /**
    * getStdDeviation - Returns the standard deviation for the number of steps
    * required to drop below the specified energy.
    * 
    * @return double
    */
   public double getStdDeviation() {
      return mStepCount.standardDeviation();
   }

   public int getBackscatterCount() {
      return mBackscatterCount;
   }

   /**
    * header - Output a header line for the results statistics.
    * 
    * @param ps
    *           PrintStream
    */
   static public void header(PrintStream ps) {
      header(new PrintWriter(ps));
   }

   public static void header(final PrintWriter pw) {
      pw.println("Shell\tEnergy\tMin\tMax\tAvg\tStdDev\tPath");
      pw.flush();
   }

   /**
    * dump - Output the resulting statistics to the OutputStream
    * 
    * @param os
    *           OutputStream
    */
   public void dump(OutputStream os) {
      dump(new PrintStream(os));
   }

   /**
    * dump - Output the resulting statistics to the PrintStream
    * 
    * @param ps
    *           PrintStream
    */
   public void dump(PrintStream ps) {
      dump(new PrintWriter(ps));
   }

   /**
    * dump - Output the resulting statistics to the PrintWriter
    * 
    * @param pw
    *           PrintWriter
    */
   public void dump(final PrintWriter pw) {
      final NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(3);
      pw.print("\"" + (mShell != null ? mShell.toString() : "None") + "\"");
      pw.print("\t");
      pw.print(nf.format(FromSI.keV(mMinE)));
      pw.print("\t");
      pw.print(nf.format(getMinimum()));
      pw.print("\t");
      pw.print(nf.format(getMaximum()));
      pw.print("\t");
      pw.print(nf.format(getAverage()));
      pw.print("\t");
      pw.print(nf.format(getStdDeviation()));
      pw.print("\t");
      pw.print(nf.format(mTrajectoryLength.average() * 1.0e6));
      pw.println();
      pw.flush();
   }
}
