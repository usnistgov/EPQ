package gov.nist.nanoscalemetrology.JMONSEL;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.TransformableRegion;
import gov.nist.microanalysis.Utility.Histogram;

/**
 * <p>
 * This is a "detector" similar to NISTMonte.BackscatterStats, upon which it is
 * based. It detects electrons with energy in a specified range that enter any
 * region in a specified set from another region not in the set. Electrons that
 * are exiting the chamber are excluded by default but may be included using
 * setIncludeExitingElectrons(true). Statistics similar to those compiled by
 * NISTMonte.BackscatterStats are saved for these detected electrons.
 * Afterwards, detected electrons may be marked "trajectory complete"
 * (destructive detection) or not (a nondestructive monitor). In the latter case
 * the electron continues until some other process marks its trajectory
 * complete.
 * </p>
 * <p>
 * Examples of use: (1) In some SEMs, strong electric fields are imposed outside
 * the sample. These fields are intended to extract all secondary (0
 * eV&lt;E&lt;50 eV) electrons from otherwise obstructed regions of the sample
 * (e.g., in holes or between lines). A completely effective version of such a
 * detector can be simulated by putting all regions that surround the sample in
 * the set of regions and setting the energy to the specified range. The
 * detector then counts all low energy electrons that escape the sample, without
 * giving them the opportunity to re-enter. (2) A monitor can be placed within
 * any desired region of the sample by creating a subregion. If the material and
 * its scattering model are the same as those of the parent region, the boundary
 * between the two is purely mathematical. (It has no effect on scattering.) If
 * the subregion is then specified in this detector's region list and the
 * detection is set to be nondestructive, the region serves as a monitor of
 * electrons entering the designated subregion. (Note that in nondestructive
 * detection, the same electron can be detected more than once, if it leaves a
 * region and then re-enters it.)
 * </p>
 * <p>
 * The energy histogram covers the energy interval of the detector (truncated if
 * necessary by the beam energy) and divides this by default into 100 bins. The
 * angles in the elevation (or polar) and aziumuth histograms refer to the
 * direction of travel when the electron was detected (not the polar coordinates
 * of its position when detected).
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
 * @version 1.1
 */

public class RegionDetector implements ActionListener {

	private final HashSet<TransformableRegion> regSet;
	private final double minEeV;
	private final double maxEeV;
	private final boolean destructive;
	private boolean includeExitingElectrons = false;

	private int mEnergyBinCount = 100;
	private final transient MonteCarloSS mMonte;
	private double beamEnergy; // in eV
	private Histogram mEnergyBins;
	private Histogram mAzimuthalBins;
	private Histogram mElevationBins;
	private long mEventCount = 0;

	private boolean mLogDetected = false;

	/**
	 * <p>
	 * Collects electron ID, trajectory step number, energy, position and direction
	 * information from detected electrons.
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
	static public class Datum {

		final long electronID;
		final long trajStep;
		final double mkEeV;
		final double[] mPosition0;
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
		 * Gets the logged value of detected electron's position when it was generated.
		 *
		 * @return Returns the position.
		 */
		public double[] getPosition0() {
			return mPosition0;
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
		 * Gets the logged value of detected electron polar angle of direction of motion
		 *
		 * @return Returns the theta.
		 */
		public double getTheta() {
			return mTheta;
		}

		/**
		 * Gets the logged value of detected electron aziumuthal angle of direction of
		 * motion
		 *
		 * @return Returns the phi.
		 */
		public double getPhi() {
			return mPhi;
		}

		private Datum(final long eID, final long tStep, final double e0, final double[] pos0, final double[] pos, final double theta,
				final double phi) {
			electronID = eID;
			trajStep = tStep;
			mkEeV = e0;
			assert pos.length == 3;
			mPosition0 = pos0.clone();
			mPosition = pos.clone();
			mTheta = theta;
			mPhi = phi;
		}

		public static String getHeader() {
			return "Electron ID\tTraj Step\tkinetic E (eV)\tx0\ty0\tz0\tx\ty\tz\ttheta\tphi";
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
			sb.append(mPosition0[0]);
			sb.append("\t");
			sb.append(mPosition0[1]);
			sb.append("\t");
			sb.append(mPosition0[2]);
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

	private void initialize() {
		beamEnergy = FromSI.eV(mMonte.getBeamEnergy());
		mElevationBins = new Histogram(0.0, Math.PI, 180);
		mAzimuthalBins = new Histogram(0.0, 2.0 * Math.PI, 360);
		mEnergyBins = new Histogram(minEeV, maxEeV < beamEnergy ? maxEeV : beamEnergy, mEnergyBinCount);
		mLog = new ArrayList<Datum>();
	}

	/**
	 * RegionDetector: Constructor.
	 *
	 * @param mcss          - the MonteCarloSS that generates events for this
	 *                      listener
	 * @param regCollection - a collection containing all regions on which this
	 *                      detector will trigger.
	 * @param minE          - the minimum energy in the range that triggers this
	 *                      detector
	 * @param maxE          - the maximum energy that triggers this detector
	 * @param destructive   - true if electrons are to be terminated after
	 *                      detection, false if they continue their trajectories
	 */
	public RegionDetector(MonteCarloSS mcss, Collection<TransformableRegion> regCollection, double minE, double maxE,
			boolean destructive) {
		mMonte = mcss;
		/*
		 * Transfer our list of regions to monitor to a HashSet. This automatically
		 * eliminates duplicates and should provide a quick, size-independent contains()
		 * method.
		 */
		this.regSet = new HashSet<TransformableRegion>((int) (regCollection.size() * 1.5) + 1);
		for (final TransformableRegion el : regCollection)
			regSet.add(el);
		this.minEeV = FromSI.eV(minE);
		this.maxEeV = FromSI.eV(maxE);
		this.destructive = destructive;
		initialize();
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		assert (ae.getSource() instanceof MonteCarloSS);
		assert (ae.getSource() == mMonte);
		switch (ae.getID()) {
		case MonteCarloSS.FirstTrajectoryEvent: {
			mEventCount = 0;
			break;
		}
		case MonteCarloSS.BackscatterEvent:
			if (!includeExitingElectrons)
				break;
		case MonteCarloSS.ExitMaterialEvent: {
			final MonteCarloSS mcss = (MonteCarloSS) ae.getSource();
			final Electron el = mcss.getElectron();
			final double elEeV = FromSI.eV(el.getEnergy());
			// Skip stats if our electron is outside the energy range
			if ((elEeV < minEeV) || (elEeV > maxEeV))
				break;
			// Skip also if its is not a transition from outside to inside our
			// listed regions
			final boolean isInRegion = regSet.contains(el.getCurrentRegion())
					&& !regSet.contains(el.getPreviousRegion());
			if ((ae.getID() == MonteCarloSS.ExitMaterialEvent) && !isInRegion)
				break;
			final double elevation = el.getTheta();
			assert (elevation >= 0.0);
			assert (elevation <= Math.PI);
			double azimuth = el.getPhi();
			if (azimuth < 0.)
				azimuth += 2.0 * Math.PI;
			assert (azimuth >= 0.0);
			assert (azimuth <= (2.0 * Math.PI));
			synchronized (this) {
				mElevationBins.add(elevation);
				mAzimuthalBins.add(azimuth);
				final double kEeV = FromSI.eV(el.getEnergy());
				mEnergyBins.add(kEeV);
				if (mLogDetected)
					mLog.add(new Datum(el.getIdent(), el.getStepCount(), kEeV, el.getPosition0(), el.getPosition(), el.getTheta(),
							el.getPhi()));
			}
			if (destructive)
				el.setTrajectoryComplete(true);
			break;
		}
		case MonteCarloSS.TrajectoryEndEvent:
			synchronized (this) {
				mEventCount++;
			}
			break;
		case MonteCarloSS.BeamEnergyChanged:
			synchronized (this) {
				initialize();
			}
			break;

		}
	}

	/**
	 * Returns a histogram object representing the accumulated energy statistics.
	 *
	 * @return Histogram
	 */
	public Histogram energyHistogram() {
		return mEnergyBins;
	}

	/**
	 * Returns a histogram of the electrons as a function of elevation. The beam is
	 * at bin zero.
	 *
	 * @return Histogram
	 */
	public Histogram elevationHistogram() {
		return mElevationBins;
	}

	/**
	 * Returns a histogram of the electrons as a function of the azimuthal angle.
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
			pw.println("Beam energy\t" + nf.format(beamEnergy / 1000.0) + " keV");
			pw.println("Detected\t" + Integer.toString(mEnergyBins.totalCounts()));
		}
		pw.println("Electron energy histogram");
		// final Iterator<Integer> bs =
		// mEnergyBins.getResultMap("{0,number,#.##}").values().iterator();
		for (final Map.Entry<Histogram.BinName, Integer> bs : mEnergyBins.getResultMap("{0,number,#.###}").entrySet()) {
			pw.print(bs.getKey().toString());
			pw.print("\t");
			pw.println(bs.getValue().toString());
		}

		pw.println("Azimuthal angle histogram");
		pw.println("Bin\tAngle");
		for (final Map.Entry<Histogram.BinName, Integer> me : mAzimuthalBins.getResultMap("{0,number,#.###}")
				.entrySet()) {
			pw.print(me.getKey().toString());
			pw.print("\t");
			pw.println(me.getValue().toString());
		}

		pw.println("Elevation angle histogram");
		pw.println("Bin\tAngle");
		for (final Map.Entry<Histogram.BinName, Integer> me : mElevationBins.getResultMap("{0,number,#.###}")
				.entrySet()) {
			pw.print(me.getKey().toString());
			pw.print("\t");
			pw.println(me.getValue().toString());
		}

		/* If logging is turned on, output data for each detected electron */
		if (mLogDetected) {
			pw.println("Detected electron log (electron ID, energy, position, and direction of motion at detection)");
			pw.println("Number of logged electrons: " + Integer.toString(mLog.size()));
			pw.println(Datum.getHeader());
			for (final Datum logEntry : mLog)
				pw.println(logEntry.toString());
		}
		pw.close();
	}

	/**
	 * Returns the number of bins in the energy histogram
	 *
	 * @return Returns the energyBinCount.
	 */
	public int getEnergyBinCount() {
		return mEnergyBinCount;
	}

	/**
	 * Sets the number of bins in the energy histogram
	 *
	 * @param energyBinCount The value to which to set energyBinCount.
	 */
	public void setEnergyBinCount(int energyBinCount) {
		if (mEnergyBinCount != energyBinCount) {
			mEnergyBinCount = energyBinCount;
			initialize();
		}
	}

	/**
	 * Returns the ratio of detected electrons to the number of trajectories.
	 */
	public double detectedFraction() {
		return (double) mEnergyBins.totalCounts() / (double) mEventCount;
	}

	/**
	 * To include electrons that are leaving the chamber in the statistics, use
	 * setIncludeExitingElectrons(true). By default, these electrons are not counted
	 * by the detector.
	 *
	 * @param includeExitingElectrons The value to which to set
	 *                                includeExitingElectrons.
	 */
	public void setIncludeExitingElectrons(boolean includeExitingElectrons) {
		this.includeExitingElectrons = includeExitingElectrons;
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
	 * element of which is a Datum object containing information about an electron
	 * at the time it was detected. The information consists of the electron's ID,
	 * trajectory step number, energy (in eV), position, polar and azimuthal angles
	 * of its direction of motion.
	 *
	 * @return List&lt;Datum&gt; -- Each entry in the list is
	 */
	public List<Datum> getLog() {
		return Collections.unmodifiableList(mLog);
	}

}
