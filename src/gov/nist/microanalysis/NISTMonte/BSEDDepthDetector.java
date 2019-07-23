/**
 * gov.nist.microanalysis.NISTMonte.BSEDDepthDetector Created by: nritchie Date:
 * Mar 9, 2018
 */
package gov.nist.microanalysis.NISTMonte;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import gov.nist.microanalysis.Utility.Histogram;

/**
 * <p>
 * Description
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
public class BSEDDepthDetector
   implements
   ActionListener {

   private final Histogram mHistogram;
   private final double mOffset;
   private double mMaxDepth;

   private final static double INIT_VAL = -Double.MAX_VALUE;

   public BSEDDepthDetector(int nBins, double zMin, double zMax, double offset) {
      mHistogram = new Histogram(zMin, zMax, nBins);
      mOffset = offset;
   }

   public Histogram getHistogram() {
      return mHistogram;
   }

   /**
    * @param arg0
    * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
    */
   @Override
   public void actionPerformed(ActionEvent ae) {
      assert (ae.getSource() instanceof MonteCarloSS);
      switch(ae.getID()) {
         case MonteCarloSS.ScatterEvent:
         case MonteCarloSS.NonScatterEvent: {
            final MonteCarloSS mcss = (MonteCarloSS) ae.getSource();
            final Electron el = mcss.getElectron();
            mMaxDepth = Math.max(mMaxDepth, el.getPosition()[2]);
            break;
         }
         case MonteCarloSS.BackscatterEvent: {
            if(mMaxDepth != INIT_VAL)
               mHistogram.add(mMaxDepth - mOffset);
            break;
         }
         case MonteCarloSS.TrajectoryStartEvent:
            mMaxDepth = INIT_VAL;
            break;
      }
   }

}
