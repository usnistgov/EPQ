/**
 *
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MeanIonizationPotential;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.NISTMonte.Electron;

/**
 * <p>
 * Implements a continuous energy loss formula modified for low energies. Above
 * a cutoff energy it uses the Bethe energy loss as modified by Joy and Luo. The
 * Joy/Luo modification improves the accuracy at low energies. Below the cutoff
 * energy it approximates the energy loss as proportional to the electron's
 * energy to the 5/2 power. This is the low energy limit described by Nieminen
 * [Scanning Microsc. 2, p 1917 (1988)]. The dividing line between these two
 * approximations is a parameter that must be supplied to the constructor when
 * this slowing down algorithm is instantiated. The proportionality constant in
 * Nieminen's low energy form is then chosen to enforce equality of the two
 * forms at the cutoff energy.
 * </p>
 * <p>
 * This scattering mechanism uses the following material properties, which
 * therefore need to be properly defined: workfunction (unless supplied in the
 * constructor), the elemental composition, density, and weight fractions.
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
public class JoyLuoNieminenCSD implements SlowingDownAlg {

	private Material mat;

	private double bh; // barrier height

	private int nce; // # constituent elements

	/* Combinations of variables that we need */
	private double[] recipJ; // 1.166/J where J=ionization energy of const.
	// elem.

	private double[] coef; // 78500 rho c[i]Z[i]/A[i] = leading coefficient in

	private double[] beta; // 1=1.166(wf+1eV)/J[i]

	private double bhplus1eV; // BH + 1eV

	private double minEforTracking;

	private double breakE; // Dividing line between Joy/Luo and Nieminen
	private double gamma; // The proportionality constant in Nieminen's formula

	/**
	 * JoyLuoNieminenCSD - Creates a continuous slowing down object for the
	 * specified material.
	 *
	 * @param mat    - SEmaterial The material
	 * @param breakE - double The energy (in J) at which to switch between the two
	 *               approximations. May be no lower than work function + 1 eV
	 */
	public JoyLuoNieminenCSD(SEmaterial mat, double breakE) {
		if (breakE > (mat.getWorkfunction() + ToSI.eV(1.)))
			this.breakE = breakE;
		else
			throw new EPQFatalException("Supplied breakpoint energy is too small.");
		setMaterial(mat);
	}

	/**
	 * JoyLuoNieminenCSD - Creates a continuous slowing down object for the
	 * specified material.
	 *
	 * @param mat    - the material
	 * @param bh     - the barrier height in Joules.
	 * @param breakE - The energy (in J) at which to switch between the two
	 *               approximations. May be no lower than barrier height + 1 eV.
	 */
	public JoyLuoNieminenCSD(Material mat, double bh, double breakE) {
		if (breakE > (bh + ToSI.eV(1.)))
			this.breakE = breakE;
		else
			throw new EPQFatalException("Supplied breakpoint energy is too small.");
		setMaterial(mat, bh);
	}

	/**
	 * setMaterial - Sets the material, residual energy loss rate, work function for
	 * which losses are to be computed.
	 *
	 * @param mat - the material
	 * @param bh  - the barrier height (min kinetic energy to escape) in Joules.
	 */
	public void setMaterial(Material mat, double bh) {
		this.mat = mat.clone();
		this.bh = bh;
		init();
	}

	/**
	 * setMaterial - Sets the material for which losses are to be computed. The work
	 * function is taken from the properties of the supplied (SEmaterial) argument.
	 *
	 * @param mat - the material
	 */
	@Override
	public void setMaterial(SEmaterial mat) {
		this.mat = mat.clone();
		bh = -mat.getEnergyCBbottom();
		init();
	}

	/*
	 * A utility routine to precalculate combinations of material-dependent
	 * constants we will need for the computation.
	 */
	private void init() {
		nce = mat.getElementCount();
		if (nce == 0)
			return;
		recipJ = new double[nce];
		coef = new double[nce];
		beta = new double[nce];
		bhplus1eV = bh + ToSI.eV(1.);
		if (breakE < bhplus1eV)
			breakE = bhplus1eV;
		final Object[] el = mat.getElementSet().toArray();
		/* Why can't I cast the above array to Element[]? */
		for (int i = 0; i < nce; i++) {
			recipJ[i] = 1.166 / (((Element) el[i]).getAtomicNumber() < 13
					? MeanIonizationPotential.Wilson41.compute((Element) el[i])
					: MeanIonizationPotential.Sternheimer64.compute((Element) el[i]));
			beta[i] = 1. - (recipJ[i] * bhplus1eV);
			/*
			 * The constant in the following expression is in units of J^2 m^2/kg. It is
			 * appropriate for density in kg/m^3, energy in Joules, and resulting continuous
			 * energy loss in J/m.
			 */
			coef[i] = (2.01507E-28 * mat.getDensity() * mat.weightFraction((Element) el[i], true)
					* ((Element) el[i]).getAtomicNumber()) / ((Element) el[i]).getAtomicWeight();
		}
		/*
		 * In the original MONSEL, the CSD routine knows the minEforTracking and can use
		 * it to optimize--quitting early if the electron energy falls below this during
		 * a step (since the electron will be dropped anyway). In this version,
		 * minEforTracking is just a synonym for the barrier height.
		 */
		minEforTracking = bh;

		// Determine the proportionality constant
		gamma = 0.;
		for (int i = 0; i < nce; i++)
			gamma += coef[i] * Math.log((recipJ[i] * breakE) + beta[i]);
		gamma /= Math.pow(breakE, 3.5);
	}

	private final double maxlossfraction = 0.1;

	/**
	 * compute - Computes the energy change for an electron that starts with energy
	 * kE and moves a distance len.
	 *
	 * @param len - the distance of travel for which to compute the loss.
	 * @param pe  - the Electron object
	 * @return - returns the energy lost by the electron in this step.
	 */
	@Override
	public double compute(double len, Electron pe) {
		return compute(len, pe.getEnergy());
	}

	public double compute(double len, double kE) {

		/*
		 * No energy loss in vacuum, and don't waste time on any with energy already low
		 * enough to be dropped.
		 */
		if ((nce == 0) || (kE < minEforTracking) || (kE <= 0.))
			return 0.;
		/*
		 * For energies below the break use the Nieminen formula, which can be
		 * integrated exactly to give the energy loss for distance len.
		 */
		if (kE <= breakE)
			return (kE / Math.pow(1. + (1.5 * gamma * len * kE * Math.sqrt(kE)), 2. / 3.)) - kE;

		// Otherwise, Joy/Luo formula
		double loss = 0.;
		for (int i = 0; i < nce; i++)
			loss += coef[i] * Math.log((recipJ[i] * kE) + beta[i]);
		loss *= len / kE;

		if (loss <= (maxlossfraction * kE))
			return -loss;

		/*
		 * We're here if the loss was too big to take all in one step. Recursively
		 * divide the step.
		 */
		loss = compute(len / 2., kE); // loss from 1st half of step
		return loss + compute(len / 2., kE + loss); // add loss from second half
	}

	/**
	 * @return Returns the breakE.
	 */
	public double getBreakE() {
		return breakE;
	}

	/**
	 * Sets the
	 *
	 * @param breakE The breakE to set.
	 */
	public void setBreakE(double breakE) {
		this.breakE = breakE;
		setMaterial(mat, bh);
	}

	/**
	 * @return - a string in the form "JoyLuoNieminenCSD(material,barrier
	 *         height,breakE)", where material, barrier height, and breakE are the
	 *         parameters either supplied to the constructor or, in the case of
	 *         barrier height, possibly ascertained from the material property.
	 */
	@Override
	public String toString() {
		return "JoyLuoNieminenCSD(" + mat.toString() + "," + Double.valueOf(bh).toString() + ","
				+ Double.valueOf(breakE).toString() + ")";
	}
}
