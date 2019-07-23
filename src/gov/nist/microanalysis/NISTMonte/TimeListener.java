package gov.nist.microanalysis.NISTMonte;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import gov.nist.microanalysis.Utility.DescriptiveStatistics;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * Listener to record the elapsed time of a simulation and the distribution of
 * times associated with individual trajectories.
 * 
 * @author ppinard
 * @author nritchie
 */
public class TimeListener
   implements ActionListener {

   /** Monte Carlo simulator. */
   private final MonteCarloSS mMcss;

   /** System time when the simulation started (in ms). */
   private long mStartSimulationTime;

   /** System time when the simulation ended(in ms). */
   private long mElapsedTime;

   /** System time when a trajectory starts(in ms). */
   private long mStartTrajectoryTime;

   /** Accumulates statistics in second units */
   private DescriptiveStatistics mStats;

   /**
    * Creates a new <code>TimeListener</code>.
    * 
    * @param mcss Monte Carlo simulator
    */
   public TimeListener(MonteCarloSS mcss) {
      if(mcss == null)
         throw new NullPointerException("mcss == null");
      this.mMcss = mcss;
   }

   @Override
   public void actionPerformed(ActionEvent ae) {
      assert (ae.getSource() instanceof MonteCarloSS);
      assert (ae.getSource() == mMcss);

      switch(ae.getID()) {
         case MonteCarloSS.FirstTrajectoryEvent:
            mStartSimulationTime = System.currentTimeMillis();
            mElapsedTime = 0;
            mStats = new DescriptiveStatistics();
            break;
         case MonteCarloSS.LastTrajectoryEvent:
            mElapsedTime = System.currentTimeMillis() - mStartSimulationTime;
            break;
         case MonteCarloSS.TrajectoryStartEvent:
            mStartTrajectoryTime = System.currentTimeMillis();
            break;
         case MonteCarloSS.TrajectoryEndEvent:
            final long trajectoryTime = System.currentTimeMillis() - mStartTrajectoryTime;
            mStats.add(0.001 * trajectoryTime);
         default:
            break;
      }

   }

   /**
    * Returns the elapsed time to run the entire simulation in milliseconds.
    * 
    * @return elapsed time in seconds
    */
   public double getSimulationTime() {
      return 0.001 * mElapsedTime;
   }

   /**
    * Returns the mean time to execute one trajectory (and its uncertainty).
    * 
    * @return mean trajectory time in seconds
    */
   public UncertainValue2 getMeanTrajectoryTime() {
      return mStats.getValue("Time (s)");
   }

   /**
    * Returns a full descriptive statistics object with timing statistics in
    * seconds units.
    *
    * @return DescriptiveStatistics
    */
   public DescriptiveStatistics getStatistics() {
      return mStats.clone();
   }

   /**
    * Writes the simulation and trajectory time in the specified file.
    * 
    * @param outputFile output file
    * @throws IOException if an error occurs while writing the file
    */
   public void dumpToFile(File outputFile)
         throws IOException {
      final FileWriter writer = new FileWriter(outputFile);
      final String eol = System.getProperty("line.separator");

      writer.append("Simulation time: " + getSimulationTime() + " s" + eol);

      final UncertainValue2 trajTime = getMeanTrajectoryTime();
      writer.append("Average trajectory time: " + 1000.0 * trajTime.doubleValue() + " +- " + 1000.0 * trajTime.uncertainty()
            + " ms");

      writer.close();
   }
}