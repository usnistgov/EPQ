package gov.nist.nanoscalemetrology.MONSELtests;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

import gov.nist.microanalysis.EPQLibrary.BrowningEmpiricalCrossSection;
import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.LitReference;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.EPQLibrary.RandomizedScatter;
import gov.nist.microanalysis.EPQLibrary.RandomizedScatterFactory;
import gov.nist.microanalysis.EPQLibrary.ScreenedRutherfordScatteringAngle;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.Utility.Math2;

public class NISTMottScatteringAngle extends RandomizedScatter {

	static private final LitReference.WebSite mReference = new LitReference.WebSite(
			"http://www.nist.gov/srd/nist64.htm", "NIST Electron Elastic-Scattering Cross-Section Database version 3.1",
			LitReference.createDate(2007, Calendar.AUGUST, 24),
			new LitReference.Author[] { LitReference.CPowell, LitReference.FSalvat, LitReference.AJablonski });

	public static class NISTMottRandomizedScatterFactory extends RandomizedScatterFactory {
		public NISTMottRandomizedScatterFactory() {
			super("NIST Mott Inelastic Cross-Section", mReference);
		}

		private final RandomizedScatter[] mScatter = new RandomizedScatter[Element.elmEndOfElements];

		/**
		 * @see gov.nist.microanalysis.EPQLibrary.RandomizedScatterFactory#get(gov.nist.microanalysis.EPQLibrary.Element)
		 */
		@Override
		public RandomizedScatter get(Element elm) {
			final int z = elm.getAtomicNumber();
			RandomizedScatter res = mScatter[z];
			if (res == null) {
				mScatter[z] = new NISTMottScatteringAngle(elm);
				res = mScatter[z];
			}
			return res;
		}

		@Override
		protected void initializeDefaultStrategy() {
			// TODO Auto-generated method stub

		}
	}

	public static final RandomizedScatterFactory Factory = new NISTMottRandomizedScatterFactory();

	public static final double MAX_NISTMOTT = ToSI.keV(20.0);
	public static final double MIN_NISTMOTT = ToSI.keV(0.050);

	private final Element mElement;
	private static final int SPWEM_LEN = 61;
	private static final int X1_LEN = 201;
	private static final double DL50 = Math.log(50.0);
	private static final double PARAM = (Math.log(2.0e4) - DL50) / 60.0;
	private final transient double[] mSpwem = new double[SPWEM_LEN];
	private final transient double[][] mX1 = new double[SPWEM_LEN][X1_LEN];
	transient private ScreenedRutherfordScatteringAngle mRutherford = null;
	transient private BrowningEmpiricalCrossSection mBrowning = null;
	transient private double sfBrowning;

	private final static double value(double a, double b, double c, double y0, double y1, double y2, double x) {
		return (((x - b) * (x - c) * y0) / ((a - b) * (a - c))) + (((x - a) * (x - c) * y1) / ((b - a) * (b - c)))
				+ (((x - a) * (x - b) * y2) / ((c - a) * (c - b)));
	}

	/**
	 * <p>
	 * NISTMottScatteringAngle - Creates an object representing the NIST SRD 64
	 * method for computing random scattering angles using Mott cross sections. The
	 * constructor loads a table of numbers which are used to quickly compute cross
	 * sections for any energy within the tabulated range, 50 eV to 20,000 eV.
	 * </p>
	 * <p>
	 * For energies above the tabulated range, computations use
	 * ScreenedRutherfordScatteringAngle instead. For energies below the tabulated
	 * range computations use BrowningEmpiricalCrossSection, except that the total
	 * cross section is multiplied by the ratio of the Mott value at 50 eV to the
	 * Browning value there. (This in effect extrapolates below 50 eV by using the
	 * form of Browning's energy dependence, but with the total cross section at 50
	 * eV adjusted to avoid a discontinuity between Browning's approximation and the
	 * actual tabulated Mott value.)
	 * </p>
	 * <p>
	 * This test version differs from similar alternatives as follows:
	 * </p>
	 * <p>
	 * This has the same interpolation method as EPQLibrary.NISTMottScatteringAngle
	 * but adds the low energy behavior.
	 * </p>
	 * <p>
	 * Compared to JMONSEL.NISTMottRS, this one has the same low energy behavior but
	 * does not implement cubic interpolation.
	 * </p>
	 * <p>
	 * This version is in MONSELtests because it is expected to exist temporarily,
	 * only for comparing performance (speed and accuracy) to the improved
	 * JMONSEL.NISTMottRS version.
	 * </p>
	 *
	 * @param elm Element
	 * @author - Nicholas W. M. Ritchie (with addition of E&lt;50 eV behavior by
	 *         John Villarrubia)
	 */
	public NISTMottScatteringAngle(Element elm) {
		super("NIST Elastic cross-section", mReference);
		assert (elm != null);
		mElement = elm;
		try {
			final String name = elm.getAtomicNumber() < 10
					? "NistXSec/E0" + Integer.toString(elm.getAtomicNumber()) + ".D64"
					: "NistXSec/E" + Integer.toString(elm.getAtomicNumber()) + ".D64";
			final InputStream is = gov.nist.microanalysis.EPQLibrary.NISTMottScatteringAngle.class
					.getResourceAsStream(name);
			final InputStreamReader isr = new InputStreamReader(is, "US-ASCII");
			final BufferedReader br = new BufferedReader(isr);
			// To ensure that numbers are parsed correctly regardless of locale
			final NumberFormat nf = NumberFormat.getInstance(Locale.US);
			br.readLine();
			for (int j = 0; j < SPWEM_LEN; ++j) {
				mSpwem[j] = nf.parse(br.readLine().trim()).doubleValue();
				for (int i = 0; i < X1_LEN; ++i)
					mX1[j][i] = nf.parse(br.readLine().trim()).doubleValue();
			}
		} catch (final Exception ex) {
			throw new EPQFatalException("Unable to construct NISTMottScatteringAngle: " + ex.toString());
		}
	}

	/**
	 * toString
	 *
	 * @return String
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CrossSection[NIST-Mott," + mElement.toAbbrev() + "]";
	}

	@Override
	public Element getElement() {
		return mElement;
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
		if (energy < MIN_NISTMOTT) {
			if (mBrowning == null) {
				mBrowning = new BrowningEmpiricalCrossSection(mElement);
				sfBrowning = this.totalCrossSection(MIN_NISTMOTT) / mBrowning.totalCrossSection(MIN_NISTMOTT);
			}
			return sfBrowning * mBrowning.totalCrossSection(energy);
		} else if (energy < MAX_NISTMOTT) {
			final double scale = PhysicalConstants.BohrRadius * PhysicalConstants.BohrRadius;
			final double logE = Math.log(FromSI.eV(energy));
			final int j = 1 + (int) ((logE - DL50) / PARAM);
			if (j == 1)
				return value(DL50, DL50 + PARAM, DL50 + (2.0 * PARAM), mSpwem[0], mSpwem[1], mSpwem[2], logE) * scale;
			else if (j == SPWEM_LEN)
				return value(DL50 + (58.0 * PARAM), DL50 + (59.0 * PARAM), DL50 + (60.0 * PARAM), mSpwem[SPWEM_LEN - 3],
						mSpwem[SPWEM_LEN - 2], mSpwem[SPWEM_LEN - 1], logE) * scale;
			else {
				final double e0 = DL50 + ((j - 2) * PARAM);
				return value(e0, e0 + PARAM, e0 + (2.0 * PARAM), mSpwem[j - 2], mSpwem[j - 1], mSpwem[j], logE) * scale;
			}
		} else {
			if (mRutherford == null)
				mRutherford = new ScreenedRutherfordScatteringAngle(mElement);
			return mRutherford.totalCrossSection(energy);
		}
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
		if (energy < MIN_NISTMOTT) {
			if (mBrowning == null) {
				mBrowning = new BrowningEmpiricalCrossSection(mElement);
				sfBrowning = this.totalCrossSection(MIN_NISTMOTT) / mBrowning.totalCrossSection(MIN_NISTMOTT);
			}
			return mBrowning.randomScatteringAngle(energy);
		} else if (energy < MAX_NISTMOTT) {
			final double logE = Math.log(FromSI.eV(energy));
			final int j = (int) ((logE - DL50) / PARAM); // offset to zero-based
			final double e2 = DL50 + ((j + 1) * PARAM);
			final double e1 = e2 - PARAM;
			final int i = ((logE - e1) < (e2 - logE) ? j : j + 1); // offset to
			// zero-based
			assert (i >= 0) && (i < mX1.length) : Integer.toString(i) + "\t" + Double.toString(FromSI.eV(energy)) + "\t"
					+ Double.toString(e1) + "\t" + Double.toString(e2);
			// via j
			final int k = (int) (200.0 * Math2.rgen.nextDouble()); // offset to
			// zero-based
			final double x = (mX1[i][k + 1] - mX1[i][k]) * Math2.rgen.nextDouble();
			final double q = mX1[i][k] + x;
			final double com = 1.0 - (2.0 * q * q);
			return com > -1.0 ? (com < 1.0 ? Math.acos(com) : 0.0) : Math.PI;
		} else {
			if (mRutherford == null)
				mRutherford = new ScreenedRutherfordScatteringAngle(mElement);
			return mRutherford.randomScatteringAngle(energy);
		}
	}
}
