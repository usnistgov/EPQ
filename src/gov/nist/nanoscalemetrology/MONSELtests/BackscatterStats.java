package gov.nist.nanoscalemetrology.MONSELtests;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Map;

import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;

/**
 * <p>
 * This is a temporary version of the NISTMonte class of the same name with a
 * minor change introduced for test purposes. It uses the local version of
 * Histogram, which has been modified to use open interval bins like this:
 * (binMin,binMax]. The following are notes from the original version.
 * </p>
 * <p>
 * Implements an accumulator that watches for backscatter events and histograms
 * them. I've chosen to define backscatter as those which exit the sample and
 * strike a hemisphere above the origin (in the direction from which the
 * electron beam came). Forward scattered electrons are those that exit the
 * sample into the hemisphere below the origin. All scatters are the sum of
 * forward and backscatter.
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
 * @version 1.1
 */

public class BackscatterStats implements ActionListener {
   private int mEnergyBinCount;
   private final transient MonteCarloSS mMonte;
   private double mBeamEnergy; // in eV
   private Histogram mFwdEnergyBins;
   private Histogram mBackEnergyBins;
   private Histogram mAzimuthalBins;
   private Histogram mElevationBins;
   private int mEventCount = 0;

   public BackscatterStats(MonteCarloSS mcss) {
      this(mcss, 400);
   }

   public BackscatterStats(MonteCarloSS mcss, int nEnergyBins) {
      mMonte = mcss;
      mEnergyBinCount = nEnergyBins;
      initialize();
   }

   private void initialize() {
      mBeamEnergy = FromSI.eV(mMonte.getBeamEnergy());
      mElevationBins = new Histogram(0.0, Math.PI, 180);
      mAzimuthalBins = new Histogram(0.0, 2.0 * Math.PI, 360);
      mFwdEnergyBins = new Histogram(0.0, mBeamEnergy, mEnergyBinCount);
      mBackEnergyBins = new Histogram(0.0, mBeamEnergy, mEnergyBinCount);
   }

   @Override
   public void actionPerformed(ActionEvent ae) {
      assert (ae.getSource() instanceof MonteCarloSS);
      assert (ae.getSource() == mMonte);
      switch (ae.getID()) {
         case MonteCarloSS.FirstTrajectoryEvent : {
            mEventCount = 0;
            break;
         }
         case MonteCarloSS.BackscatterEvent : {
            final MonteCarloSS mcss = (MonteCarloSS) ae.getSource();
            final Electron el = mcss.getElectron();
            final double[] pos = el.getPosition();
            final double elevation = (Math.PI / 2) - Math.atan2(pos[2], Math.sqrt((pos[0] * pos[0]) + (pos[1] * pos[1])));
            assert (elevation >= 0.0);
            assert (elevation <= Math.PI);
            double azimuth = Math.atan2(pos[1], pos[0]);
            if (azimuth < 0.0)
               azimuth = (2.0 * Math.PI) + azimuth;
            assert (azimuth >= 0.0);
            assert (azimuth <= (2.0 * Math.PI));
            synchronized (this) {
               mElevationBins.add(elevation);
               mAzimuthalBins.add(azimuth);
               if (elevation < (Math.PI / 2.0))
                  mFwdEnergyBins.add(FromSI.eV(el.getEnergy()));
               else
                  mBackEnergyBins.add(FromSI.eV(el.getEnergy()));

            }
            break;
         }
         case MonteCarloSS.TrajectoryEndEvent :
            synchronized (this) {
               mEventCount++;
            }
            break;
         case MonteCarloSS.BeamEnergyChanged :
            synchronized (this) {
               initialize();
            }
            break;

      }
   }

   /**
    * Returns a histogram object representing the accumulated backscatter energy
    * statistics.
    *
    * @return Histogram
    */
   public Histogram backscatterEnergyHistogram() {
      return mBackEnergyBins;
   }

   /**
    * Returns a histogram object representing the accumulated backscatter energy
    * statistics.
    *
    * @return Histogram
    */
   public Histogram forwardscatterEnergyHistogram() {
      return mFwdEnergyBins;
   }

   /**
    * Returns a histogram of the backscattered and forward scattered electrons
    * as a function of elevation. The beam is at bin zero.
    *
    * @return Histogram
    */
   public Histogram elevationHistogram() {
      return mElevationBins;
   }

   /**
    * Returns a histogram of the backscattered and forward scattered electrons
    * as a function of the azimuthal angle.
    *
    * @return Histogram
    */
   public Histogram azimuthalHistogram() {
      return mAzimuthalBins;
   }

   public void dump(OutputStream os) {
      final NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(3);
      final PrintWriter pw = new PrintWriter(os);
      { // Header
         pw.println("Beam energy\t" + nf.format(mBeamEnergy / 1000.0) + " keV");
         pw.println("Backscatter\t" + Integer.toString(mBackEnergyBins.totalCounts()));
         pw.println("Forwardscatter\t" + Integer.toString(mFwdEnergyBins.totalCounts()));
      }
      pw.println("Forward and back scattered electron energy histogram");
      pw.println("Bin\tBack\tForward");
      assert mBackEnergyBins.binCount() == mFwdEnergyBins.binCount();
      final Iterator<Integer> bs = mBackEnergyBins.getResultMap("{0,number,#.##}").values().iterator();
      for (final Map.Entry<Histogram.BinName, Integer> me : mFwdEnergyBins.getResultMap("{0,number,#.##}").entrySet()) {
         pw.print(me.getKey().toString());
         pw.print("\t");
         pw.print(bs.next().toString());
         pw.print("\t");
         pw.println(me.getValue().toString());
      }

      pw.println("Azimuthal angle histogram");
      pw.println("Bin\tAngle");
      for (final Map.Entry<Histogram.BinName, Integer> me : mAzimuthalBins.getResultMap("{0,number,#.##}").entrySet()) {
         pw.print(me.getKey().toString());
         pw.print("\t");
         pw.println(me.getValue().toString());
      }

      pw.println("Elevation angle histogram");
      pw.println("Bin\tAngle");
      for (final Map.Entry<Histogram.BinName, Integer> me : mElevationBins.getResultMap("{0,number,#.##}").entrySet()) {
         pw.print(me.getKey().toString());
         pw.print("\t");
         pw.println(me.getValue().toString());
      }
      pw.close();
   }

   /**
    * Fraction scattered into the upper hemisphere
    *
    * @return
    */
   public double backscatterFraction() {
      return (double) mBackEnergyBins.totalCounts() / (double) mEventCount;
   }

   /**
    * Returns the number of bins in the energy histograms
    *
    * @return Returns the energyBinCount.
    */
   public int getEnergyBinCount() {
      return mEnergyBinCount;
   }

   /**
    * Sets the number of bins in the energy histograms
    *
    * @param energyBinCount
    *           The value to which to set energyBinCount.
    */
   public void setEnergyBinCount(int energyBinCount) {
      if (mEnergyBinCount != energyBinCount) {
         mEnergyBinCount = energyBinCount;
         initialize();
      }
   }

   /**
    * Fraction scattered into the lower hemisphere
    *
    * @return
    */
   public double forwardscatterFraction() {
      return (double) mFwdEnergyBins.totalCounts() / (double) mEventCount;
   }

   /**
    * All electrons which leave the sample and strike either the upper
    * hemisphere or the lower hemisphere.
    *
    * @return
    */
   public double scatterFraction() {
      return backscatterFraction() + forwardscatterFraction();
   }
}
