package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.CSVReader;
import gov.nist.microanalysis.Utility.Math2;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Various different implementations of classes that return the edge energy for
 * a specified AtomicShell.
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

abstract public class EdgeEnergy extends AlgorithmClass {

	@Override
	protected void initializeDefaultStrategy() {
		// Don't do anything...
	}

	protected EdgeEnergy(String name, String ref) {
		super("Edge Energy", name, ref);
	}

	protected EdgeEnergy(String name, LitReference ref) {
		super("Edge Energy", name, ref);
	}

	/**
	 * getAllImplementations - Returns a full list of all available algorithms. Each
	 * item is an implements the EdgeEnergy class.
	 * 
	 * @return List
	 */
	@Override
	public List<AlgorithmClass> getAllImplementations() {
		return Arrays.asList(mAllImplementations);
	}

	/**
	 * Returns the edge energy associated with the specified atomic shell (in
	 * Joules). Returns zero for shells not associated with edges.
	 * 
	 * @param shell AtomicShell
	 * @return double
	 */
	abstract public double compute(AtomicShell shell);

	/**
	 * Returns the edge energy (in Joules) associated with the specified tranition.
	 * 
	 * @param xrt XRayTransition
	 * @return double
	 */
	public double compute(XRayTransition xrt) {
		return compute(xrt.getDestination());
	}

	/**
	 * supports - Does this particular implementation provide a non-zero edge energy
	 * for the specified transition?
	 * 
	 * @param shell AtomicShell
	 * @return boolean
	 */
	abstract public boolean isSupported(AtomicShell shell);

	public static class DiracHartreeSlaterIonizationEnergies extends EdgeEnergy {

		static double[][] mUis; // nominally [100][9]

		public DiracHartreeSlaterIonizationEnergies() {
			super("Bote-Salvat 2008", new LitReference.JournalArticle(LitReference.PhysRevA, "77", "042701-1 to 24",
					2008,
					new LitReference.Author[] { new LitReference.Author("David", "Bote",
							"Facultat de Física (ECM), Universitat de Barcelona, Diagonal 647, 08028 Barcelona, Spain"),
							LitReference.FSalvat }));
		}

		private void initialize() {
			synchronized (DiracHartreeSlaterIonizationEnergies.class) {
				if (mUis == null) {
					mUis = new double[100][];
					final double[][] uisTmp = (new CSVReader.ResourceReader("SalvatXion/xionUis.csv", false))
							.getResource(EdgeEnergy.class);
					assert uisTmp.length == 99;
					for (int r = 0; r < uisTmp.length; ++r) {
						assert Math.round(uisTmp[r][0]) == (r + 1);
						mUis[r + 1] = Math2.slice(uisTmp[r], 1, uisTmp[r].length - 1);
					}
				}
			}
		}

		/**
		 * @see gov.nist.microanalysis.EPQLibrary.EdgeEnergy#compute(gov.nist.microanalysis.EPQLibrary.AtomicShell)
		 */
		@Override
		public double compute(AtomicShell shell) {
			if (mUis == null)
				initialize();
			return ToSI.eV(mUis[shell.getElement().getAtomicNumber()][shell.getShell()]);
		}

		/**
		 * @see gov.nist.microanalysis.EPQLibrary.EdgeEnergy#isSupported(gov.nist.microanalysis.EPQLibrary.AtomicShell)
		 */
		@Override
		public boolean isSupported(AtomicShell shell) {
			if (mUis == null)
				initialize();
			final int z = shell.getElement().getAtomicNumber();
			final int sh = shell.getShell();
			return (z >= 1) && (z <= 99) && (sh < mUis[z].length);
		}
	};

	public static final EdgeEnergy DHSIonizationEnergy = new DiracHartreeSlaterIonizationEnergies();

	/**
	 * NISTxrtdb - The NIST x-ray transition database provides edge energies for the
	 * K and L shells for atomic numbers from 10 to 100.
	 */
	public static class NISTEdgeEnergy extends EdgeEnergy {
		NISTEdgeEnergy() {
			super("NIST X-ray transition database", "http://physics.nist.gov/PhysRefData/XrayTrans/");
		}

		private double[][] mEnergies;

		@Override
		public double compute(AtomicShell shell) {
			if (mEnergies == null)
				mEnergies = (new CSVReader.ResourceReader("NISTxrtdb.csv", false)).getResource(EdgeEnergy.class);
			final int an = shell.getElement().getAtomicNumber();
			if ((an < Element.elmNe) || (an > Element.elmFm))
				throw new IllegalArgumentException(toString() + " only supports elements Ne (10) to Fm (100)");
			final int sh = shell.getShell();
			if ((sh < AtomicShell.K) || (sh > AtomicShell.LIII))
				throw new IllegalArgumentException(toString() + " only supports shells K, L1, L2 and L3");
			return ToSI.eV(mEnergies[an - Element.elmNe][sh - AtomicShell.K]);
		}

		@Override
		public boolean isSupported(AtomicShell shell) {
			if (mEnergies == null)
				mEnergies = (new CSVReader.ResourceReader("NISTxrtdb.csv", false)).getResource(EdgeEnergy.class);
			final int an = shell.getElement().getAtomicNumber();
			final int sh = shell.getShell();
			return (an >= Element.elmNe) && (an <= Element.elmFm) && (sh >= AtomicShell.K) && (sh <= AtomicShell.LIII)
					&& (mEnergies[an - Element.elmNe][sh - AtomicShell.K] > 0.0);
		}
	};

	public static final EdgeEnergy NISTxrtdb = new NISTEdgeEnergy();

	/**
	 * Chantler2005 - A set of edge energies from "Chantler, C.T., Olsen, K.,
	 * Dragoset, R.A., Kishore, A.R., Kotochigova, S.A., and Zucker, D.S. (2005),
	 * X-Ray Form Factor, Attenuation and Scattering Tables (version 2.1). [Online]
	 * Available: http://physics.nist.gov/ffast 10-Mar-2005. National Institute of
	 * Standards and Technology, Gaithersburg, MD. Originally published as Chantler,
	 * C.T., J. Phys. Chem. Ref. Data 29(4), 597-1048 (2000); and Chantler, C.T., J.
	 * Phys. Chem. Ref. Data 24, 71-643 (1995)." Supports elements H to U and shells
	 * K to O5, P1 to P3.
	 */

	public static class ChantlerEdgeEnergy extends EdgeEnergy {
		ChantlerEdgeEnergy() {
			super("NIST-Chantler 2005", "http://physics.nist.gov/ffast");
		}

		private double[][] mEnergies;

		private final int index(int sh) {
			if ((sh < AtomicShell.K) || (sh > AtomicShell.PIII))
				return -1;
			if (sh <= AtomicShell.OV)
				return sh;
			if (sh >= AtomicShell.PI)
				return sh - 4; // (AtomicShell.PI-AtomicShell.OV + 1);
			return -1;
		}

		private void load() {
			synchronized (this) {
				if (mEnergies == null) {
					mEnergies = (new CSVReader.ResourceReader("FFastEdgeDB.csv", true)).getResource(EdgeEnergy.class);
					// Convert from eV to Joules
					for (int r = 0; r < mEnergies.length; ++r)
						if (mEnergies[r].length > 0)
							for (int c = 0; c < mEnergies[r].length; ++c)
								mEnergies[r][c] = ToSI.eV(mEnergies[r][c]);
				}
			}
		}

		@Override
		public double compute(AtomicShell shell) {
			if (mEnergies == null)
				load();
			final int an = shell.getElement().getAtomicNumber();
			if ((an < Element.elmH) || (an > Element.elmU))
				throw new IllegalArgumentException(toString() + " only supports elements H (1) to U (92)");
			final int sh = shell.getShell();
			final int i = index(sh);
			if (i == -1)
				throw new IllegalArgumentException(toString() + " only supports shells K to O5, P1 to P3");
			assert mEnergies.length > (an - Element.elmH) : "Too few elements in EdgeEnergy database.";
			return mEnergies[an - Element.elmH].length > i ? mEnergies[an - Element.elmH][i] : 0.0;
		}

		@Override
		public boolean isSupported(AtomicShell shell) {
			if (mEnergies == null)
				load();
			final int an = shell.getElement().getAtomicNumber();
			final int i = index(shell.getShell());
			return (an >= Element.elmLi) && (an <= Element.elmU) && (i >= 0)
					&& (mEnergies[an - Element.elmH].length > i) && (mEnergies[an - Element.elmH][i] > 0.0);
		}
	};

	public static final EdgeEnergy Chantler2005 = new ChantlerEdgeEnergy();

	/**
	 * Wernish84 - Wernisch et al., 1984 - Taken from Markowitz in the Handbook of
	 * X-ray Spectroscopy
	 */
	public static class WernishEdgeEnergy extends EdgeEnergy {
		WernishEdgeEnergy() {
			super("Wernisch et al., 1984",
					"Wernisch et al., 1984 - Taken from Markowitz in the Handbook of X-ray Spectroscopy");
		}

		@Override
		public double compute(AtomicShell shell) {
			final double z = shell.getElement().getAtomicNumber();
			if (!isSupported(shell))
				throw new IllegalArgumentException("Unsupported shell " + shell.toString() + " in " + toString());
			switch (shell.getShell()) {
			case AtomicShell.K:
				return ToSI.keV(-1.304e-1 + (z * (-2.633e-3 + (z * (9.718e-3 + (z * 4.144e-5))))));
			case AtomicShell.LI:
				return ToSI.keV(-4.506e-1 + (z * (1.566e-2 + (z * (7.599e-4 + (z * 1.792e-5))))));
			case AtomicShell.LII:
				return ToSI.keV(-6.018e-1 + (z * (1.964e-2 + (z * (5.935e-4 + (z * 1.843e-5))))));
			case AtomicShell.LIII:
				return ToSI.keV(3.390e-1 + (z * (-4.931e-2 + (z * (2.336e-3 + (z * 1.836e-6))))));
			case AtomicShell.MI:
				return ToSI.keV(-8.645 + (z * (3.977e-1 + (z * (-5.963e-3 + (z * 3.624e-5))))));
			case AtomicShell.MII:
				return ToSI.keV(-7.499 + (z * (3.459e-1 + (z * (-5.250e-3 + (z * 3.263e-5))))));
			case AtomicShell.MIII:
				return ToSI.keV(-6.280 + (z * (2.831e-1 + (z * (-4.117e-3 + (z * 2.505e-5))))));
			case AtomicShell.MIV:
				return ToSI.keV(-4.778 + (z * (2.184e-1 + (z * (-3.303e-3 + (z * 2.115e-5))))));
			case AtomicShell.MV:
				return ToSI.keV(-2.421 + (z * (1.172e-1 + (z * (-1.845e-3 + (z * 1.397e-5))))));
			default:
				throw new IllegalArgumentException("Unsupported shell in " + toString());
			}
		}

		@Override
		public boolean isSupported(AtomicShell shell) {
			final int z = shell.getElement().getAtomicNumber();
			switch (shell.getShell()) {
			case AtomicShell.K:
				return ((z >= 11) && (z <= 63));
			case AtomicShell.LI:
				return ((z >= 28) && (z <= 83));
			case AtomicShell.LII:
				return ((z >= 30) && (z <= 83));
			case AtomicShell.LIII:
				return ((z >= 30) && (z <= 83));
			case AtomicShell.MI:
				return ((z >= 52) && (z <= 83));
			case AtomicShell.MII:
				return ((z >= 55) && (z <= 83));
			case AtomicShell.MIII:
				return ((z >= 55) && (z <= 83));
			case AtomicShell.MIV:
				return ((z >= 60) && (z <= 83));
			case AtomicShell.MV:
				return ((z >= 61) && (z <= 83));
			default:
				return false;
			}
		}
	}

	public static final EdgeEnergy Wernish84 = new WernishEdgeEnergy();

	/**
	 * DTSA - From DTSA at
	 * http://www.cstl.nist.gov/div837/Division/outputs/DTSA/DTSA.htm
	 */
	public static class DTSAEdgeEnergy extends EdgeEnergy {
		DTSAEdgeEnergy() {
			super("DTSA", "From DTSA at http://www.cstl.nist.gov/div837/Division/outputs/DTSA/DTSA.htm");
		}

		private double[][] mEdgeEnergy;

		@Override
		public double compute(AtomicShell shell) {
			if (mEdgeEnergy == null)
				mEdgeEnergy = (new CSVReader.ResourceReader("EdgeEnergies.csv", true)).getResource(EdgeEnergy.class);
			final int sh = shell.getShell();
			if ((sh < AtomicShell.K) || (sh > AtomicShell.NI))
				throw new EPQFatalException("Unsupported shell " + shell.toString() + " in " + toString());
			final int i = shell.getElement().getAtomicNumber() - 1;
			if ((i < 0) || (i >= mEdgeEnergy.length))
				throw new EPQFatalException("Unsupported element " + shell.toString() + " in " + toString());
			return (mEdgeEnergy[i] != null) && (mEdgeEnergy[i].length > sh) ? ToSI.eV(mEdgeEnergy[i][sh]) : 0.0;
		}

		@Override
		public boolean isSupported(AtomicShell shell) {
			if (mEdgeEnergy == null)
				mEdgeEnergy = (new CSVReader.ResourceReader("EdgeEnergies.csv", true)).getResource(EdgeEnergy.class);
			final int sh = shell.getShell();
			final int zp = shell.getElement().getAtomicNumber() - 1;
			return (zp < mEdgeEnergy.length) && (mEdgeEnergy[zp] != null) && (mEdgeEnergy[zp].length > sh);
		}
	}

	public static final EdgeEnergy DTSA = new DTSAEdgeEnergy();

	public static class WilliamsEdgeEnergy extends EdgeEnergy {

		private double[][] mEdgeEnergy;

		protected WilliamsEdgeEnergy() {
			super("Williams-2011", new LitReference.JournalArticle("Electron binding energies of the elements",
					new LitReference.Journal("CRC Handbook of Chemistry and Physics", "CRC Chem Phys", "CRC"), "91st",
					"221-226", 2011, new LitReference.Author[] { new LitReference.Author("G. P.", "Williams") }));
			mEdgeEnergy = new double[99][29];
			final InputStream is = EdgeEnergy.class.getResourceAsStream("WilliamsBinding.csv");
			try (final BufferedReader br = new BufferedReader(new InputStreamReader(is, "US-ASCII"))) {
				int zm1 = 0;
				for (String line = br.readLine(); line != null; line = br.readLine()) {
					String[] ees = line.split(",");
					Arrays.fill(mEdgeEnergy[zm1], Double.NaN);
					for (int i = 0; i < ees.length; ++i) {
						final String s = ees[i].strip();
						mEdgeEnergy[zm1][i] = s.length() > 0 ? ToSI.eV(Double.parseDouble(s)) : Double.NaN;
					}
					zm1 += 1;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		@Override
		public double compute(AtomicShell shell) {
			return mEdgeEnergy[shell.getElement().getAtomicNumber() - 1][shell.getShell()];
		}

		@Override
		public boolean isSupported(AtomicShell shell) {
			return (shell.getShell() < 29) && (shell.getShell() >= 0)
					&& (!Double.isNaN(mEdgeEnergy[shell.getElement().getAtomicNumber() - 1][shell.getShell()]));
		}
	}

	public static final EdgeEnergy Williams2011 = new WilliamsEdgeEnergy();

	/**
	 * A super set to cover the largest range of shells
	 */
	public static class SuperSetEdgeEnergy extends EdgeEnergy {
		SuperSetEdgeEnergy() {
			super("Superset", "Default then Chantler then NIST then DTSA");
		}

		@Override
		public double compute(AtomicShell shell) {
			if(Chantler2005.isSupported(shell))
				return Chantler2005.compute(shell);
			else if(Williams2011.isSupported(shell))
				return Williams2011.compute(shell);
			else if(NISTxrtdb.isSupported(shell))
				return NISTxrtdb.compute(shell);
			else if(DTSA.isSupported(shell))
				return DTSA.compute(shell);
			else
				return -1.0;
		}

		@Override
		public boolean isSupported(AtomicShell shell) {
			return Williams2011.isSupported(shell) || Chantler2005.isSupported(shell) || NISTxrtdb.isSupported(shell) || DTSA.isSupported(shell);
		}
	}

	public static final EdgeEnergy SuperSet = new SuperSetEdgeEnergy();

	public static final EdgeEnergy Default = EdgeEnergy.SuperSet;

	static private final AlgorithmClass[] mAllImplementations = { EdgeEnergy.DTSA, EdgeEnergy.Chantler2005,
			EdgeEnergy.NISTxrtdb, EdgeEnergy.Wernish84, EdgeEnergy.DHSIonizationEnergy, EdgeEnergy.Williams2011,
			EdgeEnergy.SuperSet };

};
