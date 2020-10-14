package gov.nist.nanoscalemetrology.JMONSEL;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.Locale;

import gov.nist.microanalysis.EPQLibrary.BrowningEmpiricalCrossSection;
import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.LitReference;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.EPQLibrary.RandomizedScatter;
import gov.nist.microanalysis.EPQLibrary.RandomizedScatterFactory;
import gov.nist.microanalysis.EPQLibrary.ScreenedRutherfordScatteringAngle;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.nanoscalemetrology.JMONSELutils.NULagrangeInterpolationFunction;

/**
 * <p>
 * NU_ELSEPA_DCS - This class is functionally similar to JMONSEL.NISTMottRS or
 * EPQLibrary.NISTMottScatteringAngle. All of these classes approximate Mott
 * scattering cross sections as computed by ELSEPA [F. Salvat, A. Jablonski, and
 * C. Powell, Comput. Phys. Commun. 165 (2005) 157-190]. The present class
 * differs from the others in 3 respects: (1) The range of energies is 50 eV to
 * 300 keV instead of 50 eV to 20 keV, (2) the present class gives the option of
 * cross sections from either atomic or muffin-tin potentials, whereas the
 * previous classes were restricted to atomic potentials, and (3) the present
 * class internally uses interpolation tables with non-uniform instead of
 * uniform intervals. Despite the slightly smaller tables and the the larger
 * energy range, this implementation has maximum interpolation errors relative
 * to the ELSEPA reference a factor of 10 more more smaller than the earlier
 * uniformly interpolated implementations.
 * </p>
 * <p>
 * For energies below a minimum the value is interpolated assuming 0 cross
 * section at 0 eV. This minimum is by default 50 eV, the table minimum, but a
 * higher default may be specified with the setMinEforTable() method.
 * Interpolation below this minimum may be specified by setMethod(int
 * methodnumber) with methodnumber = 1 for Browning interpolation or 2 for
 * linear interpolation. The default is Browning interpolation.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */

public class NU_ELSEPA_DCS extends RandomizedScatter {

	/*
	 * MIN_ and MAX_ ELSEPA are the limits of the scattering table that we
	 * interpolate
	 */
	public static final double MAX_ELSEPA = ToSI.keV(300.0);
	public static final double MIN_ELSEPA = ToSI.eV(50.);
	
	/**
	 * This Factory class includes a get(Element elm) method that returns a NU_ELSEPA_DCS
	 * object for that element. It has methods that determine whether the
	 * NU_ELSEPA_DCS object should extend RandomizedScatter with an atomic or
	 * muffin-tin scattering model and to determine the behavior for energies below
	 * a minimum. The factory class object is typically passed to scattering
	 * mechanism implementations that then need only implement them for those
	 * elements that compose the material being simulated. 
	 * 
	 * @author John Villarrubia
	 *
	 */
	public static class NU_ELSEPA_DCSFactory extends RandomizedScatterFactory {

		private int extrapMethod = 1;
		private int potentialModel = 1; // Default model is muffin-tin

		private double minEforTable = MIN_ELSEPA;

		private final NU_ELSEPA_DCS[] mScatter = new NU_ELSEPA_DCS[Element.elmEndOfElements];

		/**
		 * Implements the default NU_ELSEPA_DCSFactory, which uses tabulated values
		 * whenever available and Browning extrapolation below the table minimum.
		 */
		public NU_ELSEPA_DCSFactory() {
			this(1, MIN_ELSEPA, 1);
		}

		/**
		 * Constructs a NU_ELSEPA_DCSFactory object with given extrapolation method,
		 * energy below which extrapolation is used, and potential model. extrapMethod
		 * is an integer = 1 or 2. Since the ELSEPA tables are valid only for energies
		 * in the interval [50 eV, 300 keV], cross sections for energies < 50 eV can't
		 * be determined directly by table lookup. Optionally users may specify a higher
		 * minimum for use of the tabulated values. Below the permitted and available
		 * range, we use one of 2 methods: (1) extrapolation with Browning's power law
		 * or (2) linear extrapolation from the final value to 0 at 0 eV.
		 *
		 * @param extrapMethod   - int 1 = Browning's power law, 2 = linear
		 * @param minEforTable   - double Energy below which to use extrapolation
		 * @param potentialModel - int 0 for atomic potentials, 1 for muffin-tin
		 */
		public NU_ELSEPA_DCSFactory(int extrapMethod, double minEforTable, int potentialModel) {
			super("NIST Mott Inelastic Cross-Section", mReference);
			setExtrapMethod(extrapMethod);
			setMinEforTable(minEforTable);
			setPotentialModel(potentialModel);
		}

		/**
		 * @see gov.nist.microanalysis.EPQLibrary.RandomizedScatterFactory#get(gov.nist.microanalysis.EPQLibrary.Element)
		 */
		@Override
		public NU_ELSEPA_DCS get(Element elm) {
			final int z = elm.getAtomicNumber();
			if (mScatter[z] == null || mScatter[z].getExtrapMethod() != extrapMethod
					|| mScatter[z].getMinEforTable() != minEforTable) {
				mScatter[z] = new NU_ELSEPA_DCS(elm, potentialModel, extrapMethod, minEforTable);
			}
			return mScatter[z];
		}

		/**
		 * Returns 1 if we will extrapolate below the minimum energy for which we're
		 * using the tabulated values by using Browning's power law or 2 if we will use
		 * linear interpolation.
		 * 
		 * @return extrapMethod
		 */
		public int getExtrapMethod() {
			return extrapMethod;
		}

		/**
		 * Returns the minimum energy (in J) for which we will use tabulated values.
		 * 
		 * @return
		 */
		public double getMinEforTable() {
			return minEforTable;
		}

		/**
		 * @return - 0 for atomic potential, 1 for muffin-tin
		 */
		public int getPotentialModel() {
			return potentialModel;
		}

		@Override
		protected void initializeDefaultStrategy() {
			// TODO Auto-generated method stub

		}

		/**
		 * extrapMethod is an integer = 1 or 2, 1 to use the Browning power law form for
		 * energies between 0 and minEforTable, 2 to use linear interpolation between
		 * the tabulated value at the upper energy and 0 at 0 energy.
		 * 
		 * @param extrapMethod
		 */
		public void setExtrapMethod(int extrapMethod) {
			if ((extrapMethod == 1) || (extrapMethod == 2))
				this.extrapMethod = extrapMethod;
			else
				throw new IllegalArgumentException("extrapMethod must be either 1 or 2.");
		}

		/**
		 * By default, we interpolate the ELSEPA tables for all energy values that fall
		 * within the range of tabulated values, but the lower limit can be increased
		 * from the default value with this setter.
		 * 
		 * @param minEforTable in Joules
		 */
		public void setMinEforTable(double minEforTable) {
			if (minEforTable >= MIN_ELSEPA)
				this.minEforTable = minEforTable;
			else
				throw new IllegalArgumentException("minEforTable must be >= " + Double.toString(MIN_ELSEPA) + " J ("
						+ Double.toString(FromSI.eV(MIN_ELSEPA)) + " eV).");
		}

		/**
		 * @param potentialModel - 0 for atomic, 1 for muffin-tin
		 */
		public void setPotentialModel(int potentialModel) {
			if (potentialModel == 0 || potentialModel == 1)
				this.potentialModel = potentialModel;
			else
				throw new IllegalArgumentException("potentialModel must be 0 (atomic) or 1 (muffin-tin)");
		}

	}

	static private final LitReference mReference = new LitReference.JournalArticle(
			new LitReference.Journal("Computer Physics Communications", "Comput. Phys. Commun.", "Elsevier"), "165",
			"157-190", 2005,
			new LitReference.Author[] { LitReference.FSalvat, LitReference.AJablonski, LitReference.CPowell });

	/**
	 * Returns a NU_ELSEPA_DCSFactory that uses minEforTable = 50 eV and
	 * extrapMethod = 1 (Browning), and muffin-tin potentials.
	 */
	public static final RandomizedScatterFactory FactoryMT = new NU_ELSEPA_DCSFactory();
	/**
	 * Returns a NU_ELSEPA_DCSFactory that uses minEforTable = 100 eV and
	 * extrapMethod = 1 (Browning), and muffin-tin potentials.
	 */
	public static final RandomizedScatterFactory FactoryMT100 = new NU_ELSEPA_DCSFactory(1, ToSI.eV(100.), 1);
	/**
	 * Returns a NU_ELSEPA_DCSFactory that uses minEforTable = 100 eV and
	 * extrapMethod = 2 (linear), and muffin-tin potentials.
	 */
	public static final RandomizedScatterFactory FactoryMT100Lin = new NU_ELSEPA_DCSFactory(2, ToSI.eV(100.), 1);
	/**
	 * Returns a NU_ELSEPA_DCSFactory that uses minEforTable = 50 eV and
	 * extrapMethod = 1 (Browning), and atomic potentials.
	 */
	public static final RandomizedScatterFactory FactoryAT = new NU_ELSEPA_DCSFactory(1, MIN_ELSEPA, 0);
	/**
	 * Returns a NU_ELSEPA_DCSFactory that uses minEforTable = 100 eV and
	 * extrapMethod = 1 (Browning), and atomic potentials.
	 */
	public static final RandomizedScatterFactory FactoryAT100 = new NU_ELSEPA_DCSFactory(1, ToSI.eV(100.), 0);
	/**
	 * Returns a NU_ELSEPA_DCSFactory that uses minEforTable = 100 eV and
	 * extrapMethod = 2 (linear), and atomic potentials.
	 */
	public static final RandomizedScatterFactory FactoryAT100Lin = new NU_ELSEPA_DCSFactory(2, ToSI.eV(100.), 0);

	private double minlogE; // Smallest log(E) for tables
	private double maxlogE; // Largest log(E) for tables
	private int minr; // Smallest r value for tables (should be 0)
	private int maxr; // Largest r value for tables (should be 1)
	private int nE; // # of energy values

	private NULagrangeInterpolationFunction sigmaTotal;
	private NULagrangeInterpolationFunction thetaVslogEr;

	/*
	 * minEforTable is the energy below which we switch to extrapolation using the
	 * Browning formula or linear extrapolation. By default we use the scattering
	 * tables whenever we have them, but we can set this value higher. For example,
	 * Kieft and Bosch don't trust the NISTMott cross sections below 100 eV.
	 */
	private int extrapMethod = 1; // 1 for Browning, 2 for linear
	private double minEforTable = MIN_ELSEPA; // Energy below which to use
												// extrapMethod
	private double maxEforTable = MAX_ELSEPA;

	private int potentialModel = 1; // 0 for atomic, 1 for muffin-tin
	private String tablesName = new String("NU_ELSEPA_MuffinTinXSec");
	private String suffixName = new String("MT.dat");

	private double MottXSatMinEnergy;
	private double MottXSatMaxEnergy;
	private final Element mElement;

	/*
	 * NISTMottScatteringAngle uses 2nd order interpolation in log(E) for the
	 * totalCrossSection calculation. For randomScatteringAngle it uses 0th order
	 * interpolation (i.e., it chooses the nearest tabulated value) in log(E) and
	 * 1st order in the random number. In contrast the present class uses 3rd order
	 * for all interpolations. (This choice is set by the constants below.)
	 */
	private final int rINTERPOLATIONORDER = 3;
	private final int sigmaINTERPOLATIONORDER = 3;
	transient private ScreenedRutherfordScatteringAngle mRutherford = null;
	transient private BrowningEmpiricalCrossSection mBrowning = null;
	transient private double sfBrowning;
	transient private double sfRutherford;

	transient private final double scale = PhysicalConstants.BohrRadius * PhysicalConstants.BohrRadius;
	// Log(E_J) = Log(E_eV) + Log(e)
	transient private final double logElectronCharge = Math.log(PhysicalConstants.ElectronCharge);

	/**
	 * Constructs a NU_ELSEPA_DCS with the default muffin-tin potential model and
	 * minimum energy for extrapolation equal to the table minimum (50 eV).
	 * 
	 * @param elm
	 */
	public NU_ELSEPA_DCS(Element elm) {
		this(elm, 1, 1, MIN_ELSEPA);
	}

	/**
	 * @param elm
	 * @param potentialmodel
	 */
	public NU_ELSEPA_DCS(Element elm, int potentialmodel, int extrapMethod, double minEforTable) {
		super("ELSEPA Elastic cross-section", mReference);
		assert (elm != null);
		if (extrapMethod == 1 || extrapMethod == 2)
			this.extrapMethod = extrapMethod;
		else
			throw new IllegalArgumentException("extrapMethod must be 1 or 2.");
		if (minEforTable >= MIN_ELSEPA)
			this.minEforTable = minEforTable;
		else
			throw new IllegalArgumentException("minEforTable must be >= table minimum");

		mElement = elm;
		setPotentialModel(potentialmodel);

		try {
			final String name = elm.getAtomicNumber() < 10
					? tablesName + "/Z0" + Integer.toString(elm.getAtomicNumber()) + suffixName
					: tablesName + "/Z" + Integer.toString(elm.getAtomicNumber()) + suffixName;
			final InputStream is = gov.nist.microanalysis.EPQLibrary.NISTMottScatteringAngle.class
					.getResourceAsStream(name);
			final InputStreamReader isr = new InputStreamReader(is, "US-ASCII");
			final BufferedReader br = new BufferedReader(isr);
			// To ensure that numbers are parsed correctly regardless of locale
			final NumberFormat nf = NumberFormat.getInstance(Locale.US);
			int z = nf.parse(br.readLine().trim()).intValue();
			if (elm.getAtomicNumber() != z)
				throw new EPQFatalException("Table file name and Z number are inconsistent for Z = " + z);
			/*
			 * Our tables have log of energies in eV. Convert those to log energies in
			 * Joules by adding logElectronCharge.
			 */
			nE = nf.parse(br.readLine().trim()).intValue(); // # of tabulated energy values
			double[] logE = new double[nE];
			double[] sigmaTotal = new double[nE];
			NULagrangeInterpolationFunction[] thetaAtr = new NULagrangeInterpolationFunction[nE];

			for (int j = 0; j < nE; ++j) {
				logE[j] = nf.parse(br.readLine().trim()).doubleValue() + logElectronCharge; // next log(E)
				sigmaTotal[j] = nf.parse(br.readLine().trim()).doubleValue(); // next sigmaTotal
				thetaAtr[j] = readnextThetaVsr(br, nf);
			}
			minlogE = logE[0]; // minimum log(E)
			maxlogE = logE[nE - 1]; // maximum log(E)
			minr = 0; // minimum tabulated r
			maxr = 1; // maximum tabulated r

			/* Define the interpolation functions */
			this.sigmaTotal = new NULagrangeInterpolationFunction(sigmaTotal, logE, sigmaINTERPOLATIONORDER);
			this.thetaVslogEr = new NULagrangeInterpolationFunction(thetaAtr, logE, rINTERPOLATIONORDER, 2);
			MottXSatMinEnergy = scale * sigmaTotal[0];
			MottXSatMaxEnergy = scale * sigmaTotal[nE - 1];
			br.close();
		} catch (final Exception ex) {
			throw new EPQFatalException("Unable to construct NU_ELSEPA_DCS: " + ex.toString());
		}
	}

	@Override
	public Element getElement() {
		return mElement;
	}

	/**
	 * @return
	 */
	public int getExtrapMethod() {
		return extrapMethod;
	}

	/**
	 * @return - the interpolation table's maximum energy
	 */
	public double getMaxE() {
		return Math.exp(maxlogE);
	}

	/**
	 * @return - the interpolation table's largest random number
	 */
	public int getMaxr() {
		return maxr;
	}

	/**
	 * @return - the smallest tabulated energy
	 */
	public double getMinE() {
		return Math.exp(minlogE);
	}

	/**
	 * @return - the energy below which cross-sections and angles must be
	 *         extrapolated
	 */
	public double getMinEforTable() {
		return minEforTable;
	}

	/**
	 * @return - the interpolation table's smallest random number
	 */
	public int getMinr() {
		return minr;
	}

	/**
	 * @return - 1 for Atomic, 2 for Muffin-tin
	 */
	public int getPotentialModel() {
		return potentialModel;
	}

	/**
	 * randomScatteringAngle - Returns a randomized scattering angle in the range
	 * [0,PI] that comes from the distribution of scattering angles for an electron
	 * of specified energy on an atom of the element represented by the instance of
	 * this class.
	 *
	 * @param energy double - In Joules
	 * @return double - an angle in radians
	 */
	@Override
	final public double randomScatteringAngle(double energy) {
		/*
		 * Even in extrapMethod 2 (linear interpolation) we use Browning for the angular
		 * distribution.
		 */
		if (energy < minEforTable) {
			if (mBrowning == null) {
				mBrowning = new BrowningEmpiricalCrossSection(mElement);
				sfBrowning = this.totalCrossSection(minEforTable) / mBrowning.totalCrossSection(minEforTable);
			}
			return mBrowning.randomScatteringAngle(energy);
		} else if (energy < maxEforTable) {
			return thetaVslogEr.evaluateAt(new double[] { Math.log(energy), Math2.rgen.nextDouble() });
		} else {
			if (mRutherford == null) {
				mRutherford = new ScreenedRutherfordScatteringAngle(mElement);
				sfRutherford = MottXSatMaxEnergy / mRutherford.totalCrossSection(maxEforTable);
			}
			return mRutherford.randomScatteringAngle(energy);
		}
	}

	/**
	 * This version of randomScatteringAngle lets us see the theta values assigned
	 * for fixed r in [0,1].
	 * 
	 * @param energy
	 * @param r
	 * @return
	 */
	public double randomScatteringAngle(double energy, double r) {
		/*
		 * Even in extrapMethod 2 (linear interpolation) we use Browning for the angular
		 * distribution.
		 */
		if (energy < minEforTable) {
			if (mBrowning == null) {
				mBrowning = new BrowningEmpiricalCrossSection(mElement);
				sfBrowning = this.totalCrossSection(minEforTable) / mBrowning.totalCrossSection(minEforTable);
			}
			return mBrowning.randomScatteringAngle(energy);
		} else if (energy < maxEforTable) {
			if (r < 0 || r > 1)
				throw new IllegalArgumentException("Illegal r outside [0,1]");
			;
			return thetaVslogEr.evaluateAt(new double[] { Math.log(energy), r });
		} else {
			if (mRutherford == null) {
				mRutherford = new ScreenedRutherfordScatteringAngle(mElement);
				sfRutherford = MottXSatMaxEnergy / mRutherford.totalCrossSection(maxEforTable);
			}
			return mRutherford.randomScatteringAngle(energy);
		}
	}

	/**
	 * This function can be called when the buffered reader is positioned at the
	 * beginning of the M,r,theta list for one of the tabulated energies. It reads
	 * the data, then builds and returns the interpolation, leaving the buffered
	 * reader pointing to log(E) of the next energy.
	 * 
	 * @param br
	 * @return
	 */
	private NULagrangeInterpolationFunction readnextThetaVsr(BufferedReader br, NumberFormat nf) {
		try {
			int n = nf.parse(br.readLine().trim()).intValue(); // # of r and theta values
			double[] r = new double[n];
			double[] theta = new double[n];
			for (int i = 0; i < n; i++) {
				r[i] = nf.parse(br.readLine().trim()).doubleValue();
			}
			for (int i = 0; i < n; i++) {
				theta[i] = nf.parse(br.readLine().trim()).doubleValue();
			}
			return new NULagrangeInterpolationFunction(theta, r, rINTERPOLATIONORDER);
		} catch (final Exception ex) {
			throw new EPQFatalException("Unable to construct NU_ELSEPA_DCS: " + ex.toString());
		}
	}

	/**
	 * extrapMethod is an integer = 1 or 2, 1 to use the Browning power law form for
	 * energies between 0 and minEforTable, 2 to use linear interpolation between
	 * the tabulated value at the upper energy and 0 at 0 energy.
	 * 
	 * @param method
	 */
	public void setExtrapMethod(int method) {
		if ((method == 1) || (method == 2))
			extrapMethod = method;
		else
			throw new IllegalArgumentException("setExtrapMethod must be called with method = 1 or 2.");
	}

	/**
	 * By default, we interpolate the table for values within the range of tabulated
	 * energies, namely from 8.01e-18 J to 4.81e-14 J (50 eV to 300 keV). The lower
	 * limit can be increased from the default value with this setter.
	 * 
	 * @param minEforTable in Joules
	 */
	public void setMinEforTable(double minEforTable) {
		if (minEforTable >= MIN_ELSEPA) {
			this.minEforTable = minEforTable;
			MottXSatMinEnergy = this.totalCrossSection(minEforTable);
		} else
			throw new IllegalArgumentException("minEforTable must be >= " + Double.toString(MIN_ELSEPA) + " J ("
					+ Double.toString(FromSI.eV(MIN_ELSEPA)) + " eV).");
	}

	/**
	 * This class is private because the potential model must be chosen within the
	 * constructor prior to reading the tables, since the choice of model determines
	 * which tables to read. The choice cannot then subsequently be changed. This
	 * private setter's job is to insure that the choice is correctly recorded for
	 * the benefit of getPotentialModel and internal access to the file name of the
	 * imported tables. (In contrast, the NU_ELSEPA_DCSFactory method of the the
	 * same name is public.
	 * 
	 * @param potentialModel - 0 for atomic, 1 for muffin-tin
	 */
	private void setPotentialModel(int potentialModel) {
		if (potentialModel == 0) {
			this.potentialModel = potentialModel;
			tablesName = new String("NU_ELSEPA_AtomicXSec");
			suffixName = new String("AT.dat");
		} else if (potentialModel == 1) {
			this.potentialModel = potentialModel;
			tablesName = new String("NU_ELSEPA_MuffinTinXSec");
			suffixName = new String("MT.dat");
		} else
			throw new IllegalArgumentException("potentialModel must be 0 (atomic) or 1 (muffin-tin)");
	}

	/**
	 * toString
	 *
	 * @return String
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (potentialModel == 1)
			return "CrossSection[ELSEPA-Mott-Atomic," + mElement.toAbbrev() + "]";
		return "CrossSection[ELSEPA-Mott-Muffin-tin," + mElement.toAbbrev() + "]";
	}

	/**
	 * totalCrossSection - Computes the total cross section for an electron of the
	 * specified energy.
	 *
	 * @param energy double - In Joules
	 * @return double - in square meters
	 */
	@Override
	final public double totalCrossSection(double energy) {
		/*
		 * It's important in some simulations to track electrons outside of the range of
		 * energies for which the NIST Mott tables are valid. For the sake of those, we
		 * switch over to a different method of estimation when the tables become
		 * unavailable. At high energy, screened Rutherford should be accurate. At low
		 * energy, it's not clear that any model is accurate. We use the Browning
		 * interpolation here.
		 */
		if (energy < minEforTable) {
			if (extrapMethod == 2)
				return (MottXSatMinEnergy * energy) / minEforTable;
			else { // Browning interpolation
				if (mBrowning == null) {
					mBrowning = new BrowningEmpiricalCrossSection(mElement);
					sfBrowning = MottXSatMinEnergy / mBrowning.totalCrossSection(minEforTable);
				}
				return sfBrowning * mBrowning.totalCrossSection(energy);
			}
		} else if (energy < maxEforTable)
			return scale * sigmaTotal.evaluateAt(new double[] { Math.log(energy) });
		else {
			if (mRutherford == null) {
				mRutherford = new ScreenedRutherfordScatteringAngle(mElement);
				sfRutherford = MottXSatMaxEnergy / mRutherford.totalCrossSection(maxEforTable);
			}
			return sfRutherford * mRutherford.totalCrossSection(energy);
		}
	}

}
