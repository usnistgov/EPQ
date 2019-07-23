package gov.nist.microanalysis.NISTMonte;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import gov.nist.microanalysis.EPQLibrary.FromSI;

/**
 * <p>
 * The DiagnosticListener class provides a mechanism to dump the electron
 * trajectories to a tab-separated text file. Each line represents a step. Most
 * steps represent scattering but some will represent boundary crossing.
 * </p>
 * <p>
 * Format:<br>
 * "Electron index", "Step index", "Depth", "X-coord", "Y-coord", "Z-coord",
 * "Event type", "Energy"
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nritchie
 * @version 1.0
 */
final public class DiagnosticListener
   implements ActionListener {
   private int mElectronIndex;
   private int mStepIndex;
   private final MonteCarloSS mMonte;
   private final PrintWriter mWriter;
   private int mDepth;

   /**
    * Constructs a DiagnosticListener
    * 
    * @param mcss
    * @param os
    */
   public DiagnosticListener(MonteCarloSS mcss, OutputStream os) {
      super();
      mMonte = mcss;
      reset();
      mWriter = new PrintWriter(os);
   }

   public DiagnosticListener(MonteCarloSS mcss, File f)
         throws FileNotFoundException {
      this(mcss, new FileOutputStream(f));
   }

   public DiagnosticListener(MonteCarloSS mcss, String filename)
         throws FileNotFoundException {
      this(mcss, new FileOutputStream(filename));
   }

   public void reset() {
      mElectronIndex = 0;
      mStepIndex = 0;
      mDepth = 0;
   }

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
      assert arg0.getSource() == mMonte;
      final int event = arg0.getID();
      boolean output = true;
      String name = "";
      switch(event) {
         case MonteCarloSS.ScatterEvent:
            ++mStepIndex;
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
            ++mElectronIndex;
            mStepIndex = 0;
            name = "Electron";
            break;
         case MonteCarloSS.TrajectoryEndEvent:
            output = false;
            break;
         case MonteCarloSS.LastTrajectoryEvent:
            output = false;
            break;
         case MonteCarloSS.FirstTrajectoryEvent:
            mElectronIndex = 0;
            output = false;
            break;
         case MonteCarloSS.StartSecondaryEvent:
            ++mDepth;
            output = false;
            break;
         case MonteCarloSS.EndSecondaryEvent:
            --mDepth;
            output = false;
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
         sb.append(mElectronIndex);
         sb.append("\t");
         sb.append(mStepIndex);
         sb.append("\t");
         sb.append(mDepth);
         sb.append("\t");
         final double[] pos = e.getPosition();
         sb.append(pos[0]);
         sb.append("\t");
         sb.append(pos[1]);
         sb.append("\t");
         sb.append(pos[2]);
         sb.append("\t");
         sb.append(event);
         sb.append("\t");
         sb.append(FromSI.eV(e.getEnergy()));
         sb.append("\t");
         sb.append(name);
         mWriter.println(sb.toString());
         if((mStepIndex % 10) == 0)
            mWriter.flush();
      }
   }

}
