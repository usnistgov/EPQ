package gov.nist.microanalysis.NISTMonte;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.Utility.Histogram;

/**
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
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nritchie
 * @author John Villarrubia
 * @version 1.0
 */
final public class BackscatterStats
   implements ActionListener {
   private int mEnergyBinCount;
   private final transient MonteCarloSS mMonte;
   private double mBeamEnergy; // in eV
   private Histogram mFwdEnergyBins;
   private Histogram mBackEnergyBins;
   private Histogram mAzimuthalBins;
   private Histogram mElevationBins;
   private int mEventCount = 0;

   private boolean mLogDetected = false;

   /**
    * <p>
    * Collects electron ID, trajectory step number, energy, position and
    * direction information from detected electrons.
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
    * @author nritchie
    * @version 1.0
    */
   static public class Datum {

      final long electronID;
      final long trajStep;
      final double mkEeV;
      final double[] mPosition;
      final double mTheta;
      final double mPhi;

      /**
       * Gets the ID number of the detected electron.
       *
       * @return
       */
      public long getElectronID() {
         return electronID;
      }

      /**
       * Gets the trajectory step number of the detected electron.
       * 
       * @return
       */
      public long getTrajStep() {
         return trajStep;
      }

      /**
       * Gets the logged value of detected electron energy in eV
       * 
       * @return Returns the beamEnergy.
       */
      public double getEnergyeV() {
         return mkEeV;
      }

      /**
       * Gets the logged value of detected electron position
       * 
       * @return Returns the position.
       */
      public double[] getPosition() {
         return mPosition;
      }

      /**
       * Gets the logged value of detected electron polar angle of direction of
       * motion
       * 
       * @return Returns the theta.
       */
      public double getTheta() {
         return mTheta;
      }

      /**
       * Gets the logged value of detected electron aziumuthal angle of
       * direction of motion
       * 
       * @return Returns the phi.
       */
      public double getPhi() {
         return mPhi;
      }

      private Datum(final long eID, final long tStep, final double e0, final double[] pos, final double theta, final double phi) {
         electronID = eID;
         trajStep = tStep;
         mkEeV = e0;
         assert pos.length == 3;
         mPosition = pos.clone();
         mTheta = theta;
         mPhi = phi;
      }

      public static String getHeader() {
         return "Electron ID\tTraj Step\tkinetic E (eV)\tx\ty\tz\ttheta\tphi";
      }

      @Override
      public String toString() {
         final StringBuffer sb = new StringBuffer();
         sb.append(electronID);
         sb.append("\t");
         sb.append(trajStep);
         sb.append("\t");
         sb.append(mkEeV);
         sb.append("\t");
         sb.append(mPosition[0]);
         sb.append("\t");
         sb.append(mPosition[1]);
         sb.append("\t");
         sb.append(mPosition[2]);
         sb.append("\t");
         sb.append(mTheta);
         sb.append("\t");
         sb.append(mPhi);
         return sb.toString();
      }
   }

   private ArrayList<Datum> mLog;

   public BackscatterStats(final MonteCarloSS mcss) {
      this(mcss, 400);
   }

   public BackscatterStats(final MonteCarloSS mcss, final int nEnergyBins) {
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
      mLog = new ArrayList<Datum>();
   }

   @Override
   public void actionPerformed(final ActionEvent ae) {
      assert (ae.getSource() instanceof MonteCarloSS);
      assert (ae.getSource() == mMonte);
      switch(ae.getID()) {
         case MonteCarloSS.FirstTrajectoryEvent: {
            mEventCount = 0;
            break;
         }
         case MonteCarloSS.BackscatterEvent: {
            final MonteCarloSS mcss = (MonteCarloSS) ae.getSource();
            final Electron el = mcss.getElectron();
            final double[] pos = el.getPosition();
            final double elevation = (Math.PI / 2) - Math.atan2(pos[2], Math.sqrt((pos[0] * pos[0]) + (pos[1] * pos[1])));
            assert (elevation >= 0.0);
            assert (elevation <= Math.PI);
            double azimuth = Math.atan2(pos[1], pos[0]);
            if(azimuth < 0.0)
               azimuth = (2.0 * Math.PI) + azimuth;
            assert (azimuth >= 0.0);
            assert (azimuth <= (2.0 * Math.PI));
            synchronized(this) {
               mElevationBins.add(elevation);
               mAzimuthalBins.add(azimuth);
               final double kEeV = FromSI.eV(el.getEnergy());
               if(elevation < (Math.PI / 2.0))
                  mFwdEnergyBins.add(kEeV);
               else
                  mBackEnergyBins.add(kEeV);
               if(mLogDetected)
                  mLog.add(new Datum(el.getIdent(), el.getStepCount(), kEeV, pos, el.getTheta(), el.getPhi()));
            }
            break;
         }
         case MonteCarloSS.TrajectoryEndEvent:
            synchronized(this) {
               mEventCount++;
            }
            break;
         case MonteCarloSS.BeamEnergyChanged:
            synchronized(this) {
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

   public void dump(final OutputStream os) {
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
      for(final Map.Entry<Histogram.BinName, Integer> me : mFwdEnergyBins.getResultMap("{0,number,#.##}").entrySet()) {
         pw.print(me.getKey().toString());
         pw.print("\t");
         pw.print(bs.next().toString());
         pw.print("\t");
         pw.println(me.getValue().toString());
      }

      pw.println("Azimuthal angle histogram");
      pw.println("Bin\tAngle");
      for(final Map.Entry<Histogram.BinName, Integer> me : mAzimuthalBins.getResultMap("{0,number,#.##}").entrySet()) {
         pw.print(me.getKey().toString());
         pw.print("\t");
         pw.println(me.getValue().toString());
      }

      pw.println("Elevation angle histogram");
      pw.println("Bin\tAngle");
      for(final Map.Entry<Histogram.BinName, Integer> me : mElevationBins.getResultMap("{0,number,#.##}").entrySet()) {
         pw.print(me.getKey().toString());
         pw.print("\t");
         pw.println(me.getValue().toString());
      }

      /* If logging is turned on, output data for each detected electron */
      if(mLogDetected) {
         pw.println("Detected electron log (electron ID, energy, position, and direction of motion at detection)");
         pw.println("Number of logged electrons: " + Integer.toString(mLog.size()));
         pw.println(Datum.getHeader());
         for(final Datum logEntry : mLog)
            pw.println(logEntry.toString());
      }
      pw.close();
   }

   /**
    * Fraction scattered into the upper hemisphere
    * 
    * @return double
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
    * @param energyBinCount The value to which to set energyBinCount.
    */
   public void setEnergyBinCount(final int energyBinCount) {
      if(mEnergyBinCount != energyBinCount) {
         mEnergyBinCount = energyBinCount;
         initialize();
      }
   }

   /**
    * Fraction scattered into the lower hemisphere
    * 
    * @return double
    */
   public double forwardscatterFraction() {
      return (double) mFwdEnergyBins.totalCounts() / (double) mEventCount;
   }

   /**
    * All electrons which leave the sample and strike either the upper
    * hemisphere or the lower hemisphere.
    * 
    * @return double
    */
   public double scatterFraction() {
      return backscatterFraction() + forwardscatterFraction();
   }

   /**
    * Gets the current value assigned to logDetected. It is true if logging is
    * enabled, false if not.
    * 
    * @return Returns the logDetected.
    */
   public boolean getLogDetected() {
      return mLogDetected;
   }

   /**
    * Sets the value assigned to logDetected. If logDetected = true the electron
    * ID, trajectory step number, energy, position, and angles of each detected
    * electron are stored and will be included in the output file generated by
    * dump.
    * 
    * @param logDetected The value to which to set logDetected.
    */
   public void setLogDetected(final boolean logDetected) {
      mLogDetected = logDetected;
   }

   /**
    * Returns the log of detected electrons in the form of an ArrayList, each
    * element of which is a Datum object containing information about an
    * electron at the time it was detected. The information consists of the
    * electron's ID, trajectory step number, energy (in eV), position, polar and
    * azimuthal angles of its direction of motion.
    * 
    * @return List&lt;Datum&gt; -- Each entry in the list is
    */
   public List<Datum> getLog() {
      return Collections.unmodifiableList(mLog);
   }
}
