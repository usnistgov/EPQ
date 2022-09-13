package gov.nist.nanoscalemetrology.JMONSEL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Simple auxiliary class intended as a "container" for all necessary
 * information regarding Auger transitions for a vacancy in a given subshell
 * (K,L1,L2,L3,M1,M2,...).
 * </p>
 * <p>
 * For a given subshell S0, this "auxiliary" class contains a list of Auger
 * energies and transition probabilities. So far we do not keep track of the
 * actual S0-S1-S2 transition: we only note the probability and the
 * Auger-emission energy. We may have to expand on this later if the
 * vacancy-relaxation "cascade" is needed (e.g. at higher primary energies,
 * where deeper inner-shell ionizations are accessible).
 * </p>
 * 
 * @author Francesc Salvat-Pujol
 * @version 0.0001 (very much pre-alpha)
 */
class AugerTransitions {

	/* List of Auger-decay probabilities. */
	ArrayList<Double> probability;

	/*
	 * List of Auger-emission energies in 1-to-1 correspondence to the processes
	 * whose probability is given in the list "probability" anbove.
	 */
	ArrayList<Double> energy;
}

/**
 * <p>
 * Simple Auger-emission model, intended for use in conjunction with
 * TabulatedInelasticSM.
 * </p>
 * <p>
 * Given a sampled energy loss and a binding energy, we decide which shell
 * (K,L,M,N,O) the vacancy was produced in (user-provided binding energies and
 * branching ratios). The provided binding energies should be those of Williams,
 * in order for the Auger simulation (as currently implemented) to be
 * consistent. At a later stage it would be better to have the user simply enter
 * what shells are ionizable and read the binding energies from a unique and
 * coherent database. This was not done so in the interest of keeping
 * modifications to a minimum for testing purposes.
 * </p>
 * <p>
 * Given an energy loss and a binding energy, the algorithm determines which
 * subshells were accessible (e.g. for the L shell it could happen that the L2
 * and L3 subshells are ionizable with the given energy loss). Then the active
 * subshell is sampled (So far, homogeneously. At a later stage we can use the
 * subshell ionization cross section to determine the relative likelihood for
 * ionizing each shell (crude approach).
 * </p>
 * 
 * @author Francesc Salvat-Pujol (database from PENELOPE and Cesc sr though).
 * @version 0.0001 (very much pre-alpha)
 */
public class VacancyRelaxationData {

	/*
	 * List of subshell binding energies, the index of which matches the database
	 * indexing (list starts at 1). Read from pdrelax.p11
	 */
	ArrayList<Double> bindingEnergies;

	/* Index of maximum subshell considered. */
	int maxSubshell;

	/*
	 * Radiative-transition probability for each subshell (fluorescence instead of
	 * Auger yield).
	 */
	ArrayList<Double> radiativeTransitionProbability;

	/* List of Auger-transition data for each subshell. */
	ArrayList<AugerTransitions> augerProbability;

	/*
	 * Upon instantiation of this class, read the Auger transition probabilities and
	 * energies from the database (for the required atomic number).
	 */
	public VacancyRelaxationData(int z0) {
		readIonizationEnergiesFromDatabase(z0);
		determineRadiativeProbability(z0);
		readAugerProbabilities(z0);
	}

	/**
	 * Given an atomic number z0 this method loads the subshell binding energies
	 * from atomconf.tab (from inner-shell calculations of Bote &amp; Salvat 2008).
	 * 
	 * @param z0 Atomic number.
	 */
	public void readIonizationEnergiesFromDatabase(int z0) {
		bindingEnergies = new ArrayList<Double>();
		String line;

		/*
		 * I maintain the subshell labeling of the database (starting at 1): K (1), L1
		 * (2), L2 (3), L3 (4), M1 (5), ...
		 * 
		 * In order to prevent indexing mistakes (java indexes from 0, the database from
		 * 1) and to have a consistent indexing convention with the database, I add a
		 * 0-th element that will never be accessed.
		 */
		bindingEnergies.add(0.0);

		/* Get ionization energies from atomconf.tab. */
		try {
			try (InputStream is = this.getClass().getResourceAsStream("atomconf.tab")) {
				try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is))) {
					while ((line = bufferedReader.readLine().trim()) != null) {
						if (line.charAt(0) != 'C') // Ignore header lines commented with 'C'.
						{
							String[] words = line.split("\\s+");
							if (Integer.parseInt(words[0]) > z0) // No need to scan file past the requested z0.
								break;
							else if (Integer.parseInt(words[0]) == z0) {
								double ebindEV = Double.parseDouble(words[8]); // DHFS (6), CARLSON (7), WILLIAMS (8).
								/*
								 * System.out.println("Line: "+line);
								 * System.out.println("Number of words: "+words.length);
								 * System.out.println("Z: "+words[0]);
								 */
								bindingEnergies.add(ToSI.eV(ebindEV));
							}
						}
					}
				}
			}
			maxSubshell = bindingEnergies.size() - 1; // Keep track of the highest subshell in the database.

			// DEBUGGING
			/*
			 * for (int i=1; i<=maxSubshell; i++) {
			 * System.out.println(i+" "+FromSI.eV(bindingEnergies.get(i))); }
			 */
			// System.out.println("maxShell: "+maxSubshell);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * <p>
	 * Not all inner-shell ionizations decay via the emission of Augers. This method
	 * calculates the probability that the vacancy decays via a radiative process
	 * (emission of fluorescence photons). We currently do not follow fluorescence
	 * processes in this class, only their frequency of occurrence so as not to
	 * mistakenly assume that all inner-shell ionizations decay via the emission of
	 * an Auger electron (this would yield an excess of Augers).
	 * </p>
	 * <p>
	 * This method reads in all radiative processes
	 * </p>
	 *
	 * @param z0 Atomic number.
	 */
	public String determineRadiativeProbability(int z0) {
		String line;
		radiativeTransitionProbability = new ArrayList<Double>();

		/*
		 * In order to maintain the subshell indexing of the Penelope files, K (1), L1
		 * (2), L2 (3), L3 (4), M1 (5), ..., I add a fake 0-th element which is never
		 * accessed.
		 */
		radiativeTransitionProbability.add(0.0);

		// This should be eventually optimized to avoid too many re-reads.
		int zz;
		StringBuffer res = new StringBuffer();
		for (int i = 1; i <= maxSubshell; i++) {
			double radprob = 0.0;
			try {
				/*
				 * Read pdrelax.p11 line by line and accumulate all radiative-process
				 * probabilities for the atomic number z0 and the shell index i.
				 */
				try (InputStream is = this.getClass().getResourceAsStream("pdrelax.p11")) {
					try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is))) {
						line = bufferedReader.readLine(); // Ignore first line (comments).
						while ((line = bufferedReader.readLine().trim()) != null) {
							String[] words = line.split("\\s+");
							zz = Integer.parseInt(words[0]);
							if (zz > z0)
								break;
							else if (zz == z0) { // Consider only the requested atomic number.
								int s0 = Integer.parseInt(words[1]);
								int s3 = Integer.parseInt(words[3]);
								if (s0 == i && s3 == 0) {
									// If the vacancy subshell is right and the line corresponds
									// to a radiative transition, accumulate the probability.
									radprob = radprob + Double.parseDouble(words[4]);
								}
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			res.append("Subshell, radprob: " + i + " " + radprob+"\n");
			if(radprob > 1.0)
				throw new Error("RADIATIVE PROB>1.");
			if(radprob < 0.0)
				throw new Error("RADIATIVE PROB<0.");
			radiativeTransitionProbability.add(radprob);
		}
		return res.toString();
	}

	/**
	 * Load Auger-emission energies and cumulative probabilities.
	 *
	 * @param z0 Atomic number.
	 */
	public void readAugerProbabilities(int z0) {
		augerProbability = new ArrayList<AugerTransitions>();
		AugerTransitions augersFromSubshell;

		/* Fake empty element for subshell index 0, never to be accessed. */
		augerProbability.add(new AugerTransitions());

		/*
		 * Determine probabilities and energies for the various Auger-emission processes
		 * given a vacancy in subshell i.
		 */
		for (int i = 1; i <= maxSubshell; i++) {
			// New AugerTransitions object for subshell i.
			augersFromSubshell = new AugerTransitions();
			augersFromSubshell.energy = new ArrayList<Double>();
			augersFromSubshell.probability = new ArrayList<Double>();

			try {
				int zz;
				try (InputStream is = this.getClass().getResourceAsStream("pdrelax.p11")) {
					try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is))) {
						String line = bufferedReader.readLine();
						while ((line = bufferedReader.readLine().trim()) != null) {
							String[] words = line.split("\\s+");
							zz = Integer.parseInt(words[0]);
							if (zz > z0) // No need to scan the file past the requested z0.
								break;
							else if (zz == z0) {
								int s0 = Integer.parseInt(words[1]);
								int s3 = Integer.parseInt(words[3]);
								if (s0 == i && s3 != 0) {
									// Save the energy and probability if the line corresponds to an Auger
									// transition for a vacancy in the i-th subshell:
									augersFromSubshell.probability.add(Double.parseDouble(words[4]));
									augersFromSubshell.energy.add(ToSI.eV(Double.parseDouble(words[5])));
								}
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			/*
			 * Calculate cumulative probabilities to ease the sampling of the transition.
			 * Here I do keep the indexing starting from zero.
			 */
			double sumProb = 0.0;
			for (int j = 0; j < augersFromSubshell.probability.size(); j++) {
				// Accumulate the total probability.
				sumProb += augersFromSubshell.probability.get(j);

				// Save the cumulative probability instead of the probability.
				if (j > 0)
					augersFromSubshell.probability.set(j,
							augersFromSubshell.probability.get(j - 1) + augersFromSubshell.probability.get(j));
			}

			// Normalize the cumulative probability to unity.
			for (int j = 0; j < augersFromSubshell.probability.size(); j++) {
				augersFromSubshell.probability.set(j, augersFromSubshell.probability.get(j) / sumProb);
			}

			// Add the AugerTransitions object to the augerProbability list.
			augerProbability.add(augersFromSubshell);
		}
	}

	/**
	 * Given an energy loss and a binding energy, this method determines which shell
	 * the binding energy corresponds to (e.g. L) and which of the subshells are
	 * ionizable given the energy loss (e.g. L2 and L3). Then, it decides whether
	 * the vacancy decayed via the emission of a fluorescence photon (returning a
	 * negative value) or an Auger electron (returning the energy of the Auger
	 * electron).
	 */
	public double augerEnergy(double eBind, double eLoss) {
		double augerEne;
		ArrayList<Integer> possibleSubshellVacancies = new ArrayList<Integer>();

		// System.out.println("\n\nEbind, Eloss: "+FromSI.eV(eBind)+"
		// "+FromSI.eV(eLoss));

		/*
		 * Determine subshell with binding energy closest to the passed value. In future
		 * stages (user indicating which subshells are accessible instead of binding
		 * energies) this may be redundant.
		 */
		double minDeltaE = 1.0e100, deltaE = 0.0;
		int imin = 0;
		for (int i = 1; i < bindingEnergies.size(); i++) {
			deltaE = Math.abs(bindingEnergies.get(i) - eBind) / eBind;
			if (deltaE < minDeltaE) {
				imin = i;
				minDeltaE = deltaE;
			}
		}

		// Consider other subshells in the same shell that could have been ionized.
		int i = imin, ilow = 0, ihigh = 0;
		if (i == 1) {
			ilow = 1;
			ihigh = 1;
		} else if (i >= 2 && i <= 4) {
			ilow = 2;
			ihigh = 4;
		} else if (i >= 5 && i <= 9) {
			ilow = 5;
			ihigh = 9;
		} else if (i >= 10 && i <= 16) {
			ilow = 10;
			ihigh = 16;
		} else if (i >= 17 && i <= 23) {
			ilow = 17;
			ihigh = 23;
		} else if (i >= 24 && i <= 27) {
			ilow = 24;
			ihigh = 27;
		} else if (i == 29) {
			ilow = 29;
			ihigh = 29;
		} else {
			System.out.println("AUGER LOGIC ERROR. STOPPING.");
			System.exit(1);
		}
		for (int j = ilow; j <= ihigh; j++) {
			if (bindingEnergies.get(j) < eLoss) {
				possibleSubshellVacancies.add(j);
			}
		}
		/*
		 * System.out.println("ilow,ihigh: "+ilow+" "+ihigh);
		 * System.out.println("Possible vacancies:"+possibleSubshellVacancies.toString()
		 * );
		 */

		// Decide the ionized subshell (so far homogeneously, later using inner-shell
		// ionization cross sections).
		/*
		 * There may be some ionization energies in atomconf.tab for which we have no
		 * relaxation data in pderelax.p11. I therefore chose to loop until valid data
		 * are found.
		 */
		int nsteps = 0;
		int ntrans0 = 0;
		int vacancySubshell;
		do {
			double p = 1.0 / possibleSubshellVacancies.size();
			double xi = Math2.rgen.nextDouble();
			vacancySubshell = 0;
			for (int j = 0; j < possibleSubshellVacancies.size(); j++) {
				if (xi <= p * (j + 1)) {
					vacancySubshell = possibleSubshellVacancies.get(j);
					break;
				}
			}
			if (vacancySubshell == 0) {
				System.out.println("vacancySubshell=0. Logic error.");
				System.exit(1);
			}
			ntrans0 = augerProbability.get(vacancySubshell).probability.size();
			nsteps++;
			if (nsteps > 1000) {
				System.out.println("Subshell logic error");
				System.exit(1);
			}
		} while (ntrans0 == 0);

		// System.out.println("Sampled subshell, radprob: "+vacancySubshell+"
		// "+radiativeTransitionProbability.get(vacancySubshell));

		// Sample radiative vs non-radiative probability. If radiative, return a
		// negative Auger energy.
		// This won't be pursued further in the calling method under
		// TabulatedInelasticSM.
		double eta = Math2.rgen.nextDouble();
		if (eta <= radiativeTransitionProbability.get(vacancySubshell)) {
			return -1.0;
		}

		// System.out.println("Non-radiative event.");

		// Sample Auger energy.
		// System.out.println("Accumulated probs:
		// "+augerProbability.get(vacancySubshell).probability.toString());

		double zeta = Math2.rgen.nextDouble();
		int j;
		int ntrans = augerProbability.get(vacancySubshell).probability.size();
		if (ntrans == 0) {
			System.out.println("ntrans=0. Stopping now.");
			System.exit(0);
		}
		for (j = 0; j < ntrans; j++) {
			if (zeta < augerProbability.get(vacancySubshell).probability.get(j))
				break;
		}
		if (j >= ntrans)
			j = ntrans - 1;
		// System.out.println("ntrans, j: "+ntrans+" "+j);

		augerEne = augerProbability.get(vacancySubshell).energy.get(j);
		// System.out.println("Auger energy:"+FromSI.eV(augerEne));

		if (augerEne < 0.0) {
			System.out.println("Auger ene<0.");
			System.out.println("" + 1 / 0);
			System.exit(1);
		}
		if (augerEne > eLoss) {
			System.out.println("Auger ene > eLoss.");
			System.out.println("" + 1 / 0);
			System.exit(1);
		}
		// if (augerEne>eBind) {System.out.println("Auger ene >
		// eBind.");System.out.println(""+1/0); System.exit(1);}

		return augerEne;
	}

	/**
	 * Given a label string ("K","L1","L2", etc.), this returns the associated
	 * index.
	 * 
	 * I ended up not using this method, but it is clarifying to have here as a
	 * cheatsheet for the subshell indexing convention.
	 */
	public int shellLabelToIndex(String lbl) {
		int i;

		switch (lbl) {
		case "K":
			i = 1;
			break;
		case "L1":
			i = 2;
			break;
		case "L2":
			i = 3;
			break;
		case "L3":
			i = 4;
			break;
		case "M1":
			i = 5;
			break;
		case "M2":
			i = 6;
			break;
		case "M3":
			i = 7;
			break;
		case "M4":
			i = 8;
			break;
		case "M5":
			i = 9;
			break;
		case "N1":
			i = 10;
			break;
		case "N2":
			i = 11;
			break;
		case "N3":
			i = 12;
			break;
		case "N4":
			i = 13;
			break;
		case "N5":
			i = 14;
			break;
		case "N6":
			i = 15;
			break;
		case "N7":
			i = 16;
			break;
		case "O1":
			i = 17;
			break;
		case "O2":
			i = 18;
			break;
		case "O3":
			i = 19;
			break;
		case "O4":
			i = 20;
			break;
		case "O5":
			i = 21;
			break;
		case "O6":
			i = 22;
			break;
		case "O7":
			i = 23;
			break;
		case "P1":
			i = 24;
			break;
		case "P2":
			i = 25;
			break;
		case "P3":
			i = 26;
			break;
		case "P4":
			i = 27;
			break;
		// I saw no i=28 in the database.
		case "Q1":
			i = 29;
			break;
		default:
			i = -1;
			break;
		}
		assert i > 0;
		return i;
	}
}