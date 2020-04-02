package gov.nist.nanoscalemetrology.MONSELtests;

/**
 * This is an older 2010 version back for comparison with the new version to discover what change leads to the big observed predicted yield difference.
 */

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.NISTMonte.Electron;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.nanoscalemetrology.JMONSEL.NUTableInterpolation;
import gov.nist.nanoscalemetrology.JMONSEL.SEmaterial;
import gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism;

import java.io.FileNotFoundException;

/**
 * <p>
 * The TabulatedInelasticSM differs from earlier implementations of
 * ScatterMechanism in that most of the properties of the scattering are not
 * hard coded into the object, but rather are dictated by input tables. So, for
 * example, the inverse mean free path for this mechanism is determined by
 * interpolating an IIMFP table associated with the material. The distribution
 * of energy losses and angle changes by the primary electron are similarly
 * determined by tables. In this way, almost all of the physics of the
 * scattering is contained in the tables, and the scatter mechanism object is
 * itself almost without physics content.
 * </p>
 * <p>
 * The tables are provided to the constructor via an array of strings, each of
 * which contains the full path to a file that can serve as input to construct a
 * NUTableInterpolation object. The required tables are documented in the
 * constructor notes. For 3 of the tables, the first input parameter is the
 * kinetic energy of the primary electron. By default, this kinetic energy is
 * assumed to be the PE's energy with respect to the bottom of the conduction
 * band. Sometimes, however, tables use a different convention. For example, in
 * a semiconductor or insulator tables may be computed for energies measured
 * with respect to the bottom of the valence band (the highest occupied band).
 * This can be accommodated by providing the constructor with the energy offset
 * between these two definitions of energy. That is, Eoffset = (energy of
 * conduction band bottom) - (the energy defined as the zero for purpose of the
 * tables).
 * </p>
 * <p>
 * There is, however, an exception to the above generalizations. There remains
 * some uncertainty in the literature over the connection between primary
 * electron energy loss/trajectory change on the one hand and secondary electron
 * (SE) final energy/final trajectory on the other. Part of the reason for this
 * difference lies in varying assumptions about the initial (pre-scattering)
 * energy and trajectory of the SE. Some of the different methods of treating
 * this connection have been implemented in this class. One of them must be
 * selected to instantiate an object of this class. The selection is made by
 * choosing a value for the methodSE parameter in the constructor. Following are
 * the methods that have been implemented:
 * </p>
 * <p>
 * methodSE = 1: This selection is my implementation of the method described by
 * Ding & Shimizu in SCANNING 18 (1996) p. 92. If the PE energy loss, deltaE is
 * greater than a core level binding energy, the SE final energy is
 * deltaE-Ebinding. Otherwise, it is deltaE+EFermi, where EFermi is the Fermi
 * energy of the material. The final direction of the SE is determined from
 * conservation of momentum with the assumption that the SE initial momentum was
 * 0.
 * </p>
 * <p>
 * methodSE = 2: This selection is my implementation of the method described by
 * Ding, Tang, & Shimizu in J.Appl.Phys. 89 (2001) p. 718. If deltaE is greater
 * than a core level binding energy the treatment is the same as before. If not,
 * the SE final energy is deltaE + E'. If E' were the Fermi energy this would be
 * the same as methodSE = 1. However, E' lies in the range max(0,EFermi -
 * deltaE) <= E' <= EFermi. The value of E' is determined probabilistically
 * based upon the free electron densities of occupied and unoccupied states.
 * </p>
 * <p>
 * methodSE = 3: This selection is my modified version of the method described
 * by Mao et al. in J.Appl.Phys. 104 (2008) article #114907. The scattering
 * event is assigned as either an electron-electron event or an electron-plasmon
 * event based upon the momentum transfer. Plasmon events are treated as in
 * methodSE = 2, except that the SE direction is isotropic. Electron-electron
 * events have energy and direction determined as described by Mao et al.
 * </p>
 * <p>
 * Interpolation is implemented using the NUTableInterpolation class. That class
 * natively allows extrapolation, but TabulatedInelasticSM checks input
 * parameters and forbids it. This means the user-provided tables must cover the
 * full range of energies that will be encountered in the simulation. This
 * generally means the tables that take PE energy as one of the inputs should
 * cover energies from the material's Fermi energy up to at least the energy of
 * electrons generated by the electron gun.
 * </p>
 * <p>
 * This scattering mechanism uses the following material properties, which
 * therefore need to be properly defined: coreEnergy array, energyCBbottom,
 * bandgap, and workfunction.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain.
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author John Villarrubia
 * @version 1.0
 */
public class TabulatedInelasticSMTest extends ScatterMechanism {

	private final int methodSE;
	private double energyOffset = 0.;

	private NUTableInterpolation tableIIMFP;
	private NUTableInterpolation tableReducedDeltaE;
	private NUTableInterpolation tableTheta;
	private NUTableInterpolation tableSEE0;
	private double offsetFermiEnergy;
	private double energyCBbottom;
	private double minEgenSE = 0.;
	private double workfunction;
	private double bandgap;
	/*
	 * bEref is the energy (relative to conduction band bottom) to which core level
	 * binding energies are referenced. This is generally the Fermi energy for
	 * metals and 0 for insulators or semiconductors.
	 */
	private double bEref;
	private final double[] kEa = new double[1]; // For convenience, because 1-d
	// tables
	// still require an array for input
	private Double[] coreEnergies;
	private final double[] interpInput = new double[3];

	// Allowed energy ranges for interpolation table inputs
	double[] tableEiDomain;
	double[] tableIIMFPEiDomain;
	// Range of allowed SE initial energies on output
	double[] energyRangeSE0;

	/**
	 * Constructs a TabulatedInelasticSM for the specified material.
	 * 
	 * @param mat      - a SEmaterial that is the material within which scattering
	 *                 occurs.
	 * @param methodSE - an int that determines the method and assumptions by which
	 *                 SE energies and angles are determined in a scattering event.
	 *                 See the description in the class documentation.
	 * @param tables   - an array of strings. The strings contain the full paths and
	 *                 file names of the interpolation tables in this order:
	 *                 String[0] = the IIMFP table (inverse inelastic mean free path
	 *                 vs. primary electron energy (EO), String[1] = the reduced
	 *                 deltaE table (deltaE/E0 vs E0 and r, with r a random number),
	 *                 table[2] = the theta table (scattering angle of PE vs. E0,
	 *                 deltaE/(E0-EFermi), r) and table[3] = the table of SE initial
	 *                 energy vs. deltaE and r.
	 */
	public TabulatedInelasticSMTest(SEmaterial mat, int methodSE, String[] tables) {
		this(mat, methodSE, tables, 0.);
	}

	/**
	 * Constructs a TabulatedInelasticSM for the specified material. This form of
	 * the constructor has an additional argument, energyOffset, allowing this
	 * parameter to be set to a value other than its default value of 0.
	 * </p>
	 * <p>
	 * energyOffset = (energy of conduction band bottom) - (the energy defined as
	 * the zero for purpose of the tables)
	 */
	public TabulatedInelasticSMTest(SEmaterial mat, int methodSE, String[] tables, double energyOffset) {
		super();
		if ((methodSE != 2) && (methodSE != 3))
			methodSE = 1; // Make sure methodSE is valid
		this.methodSE = methodSE;

		/* Read interpolation tables into memory */
		try {
			tableIIMFP = NUTableInterpolation.getInstance(tables[0]);
		} catch (final FileNotFoundException e1) {
			throw new EPQFatalException("File " + tables[0] + " not found.");
		}
		try {
			tableReducedDeltaE = NUTableInterpolation.getInstance(tables[1]);
		} catch (final FileNotFoundException e1) {
			throw new EPQFatalException("File " + tables[1] + " not found.");
		}
		try {
			tableTheta = NUTableInterpolation.getInstance(tables[2]);
		} catch (final FileNotFoundException e1) {
			throw new EPQFatalException("File " + tables[2] + " not found.");
		}
		if ((methodSE == 2) || (methodSE == 3))
			try {
				tableSEE0 = NUTableInterpolation.getInstance(tables[3]);
				energyRangeSE0 = tableSEE0.getRange();
			} catch (final FileNotFoundException e1) {
				throw new EPQFatalException("File " + tables[3] + " not found.");
			}

		this.energyOffset = energyOffset;
		setMaterial(mat);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#scatter(gov.nist.
	 * microanalysis.NISTMonte.Electron)
	 */
	@Override
	public Electron scatter(Electron pe) {

		final double kE0 = pe.getEnergy(); // PE initial energy rel to CB bottom
		final double kE = kE0 + energyOffset; // PE initial energy rel to
		// scattering band bottom
		if (kE < tableEiDomain[0])
			/*
			 * This might happen if something, e.g., electrostatic potential difference,
			 * reduces electron energy between the time we determine that scattering happens
			 * (at the beginning of a step) and the time it actually occurs (at the end of
			 * the step). In this case, we simply don't scatter.
			 */
			return null;
		if (kE > tableEiDomain[1])
			throw new EPQFatalException(
					"PE energy " + Double.toString(kE) + " is outside the interpolation table interval of ["
							+ Double.toString(tableEiDomain[0]) + "," + Double.toString(tableEiDomain[1]) + "]");
		double theta = 0.;
		double phi = 0.; // PE trajectory parameters
		double energySE, thetaSE, phiSE; // SE trajectory parameters
		// TODO Do I need to check that kE>offsetFermiEnergy?
		final double[] randoms = new double[] { Math2.rgen.nextDouble(), Math2.rgen.nextDouble(),
				Math2.rgen.nextDouble(), Math2.rgen.nextDouble() };
		interpInput[0] = kE;
		interpInput[1] = randoms[0];
		// Energy loss by PE
		double deltaE = kE * tableReducedDeltaE.interpolate(interpInput, 3);
		/*
		 * Cubic interpolation of the table can undershoot. Treat deltaE close to but
		 * below the bandgap as such undershoot and correct it.
		 */
		if ((deltaE < bandgap) && (deltaE > 0.95 * bandgap))
			deltaE = bandgap;
		/*
		 * Larger discrepancies are most likely because we've been supplied an empirical
		 * table that includes non-electronic energy losses (e.g., scattering from
		 * phonons). These should really be handled separately because our model is only
		 * valid for electrons & plasmons. (E.g., phonon of energy deltaE carries very
		 * different momentum from electron of energy deltaE, so scattering angles can't
		 * be determined in the present model.) We skip the angular scattering part for
		 * such events. Any generated SE will be in the bandgap, so most likely dropped
		 * anyway. We return after we deal with the PE energy loss.
		 */
		final double theta0PE = pe.getTheta(); // Remember original direction;
		final double phi0PE = pe.getPhi(); // to use for SE
		if (deltaE >= bandgap) {
			// Determine theta and phi here
			/*
			 * First, the reduced energy. This parameter ranges from 0 to 1 as deltaE ranges
			 * from its minimium to maximum value.
			 */
			interpInput[1] = (deltaE - bandgap) / (kE - offsetFermiEnergy - 2. * bandgap);
			/*
			 * The reduced energy can on rare occasions, as a result of interpolation error,
			 * lie slightly outside its physically determined interval of [0,1]. If it does,
			 * clip it to the boundary.
			 */
			if (interpInput[1] > 1.)
				interpInput[1] = 1.;
			else if (interpInput[1] < 0.)
				interpInput[1] = 0.;
			interpInput[2] = randoms[1];
			theta = tableTheta.interpolate(interpInput, 3);
			phi = 2. * Math.PI * randoms[2];
			/*
			 * Update PE trajectory. Note that the energy of the PE is decremented by
			 * deltaE. Any continuous energy loss formula should only account for losses not
			 * including this one.
			 */
			pe.updateDirection(theta, phi);
		}

		pe.setEnergy(kE0 - deltaE);
		pe.setPreviousEnergy(kE0);

		// Determine SE final energy and trajectory
		Electron se = null;
		double be = 0.;

		/*
		 * Some measured ELF data have nonzero values for deltaE less than the bandgap,
		 * and it is consequently possible that some scattering tables that retain this
		 * part of the data will include such loss events. These may, for example,
		 * correspond to phonon losses. They presumably do not correspond to generation
		 * of mobile SE, since there are no empty mobile states in the gap. We therefore
		 * return no SE for such events.
		 */
		if (deltaE < bandgap)
			return null;

		switch (methodSE) {
		case 1:
			/*
			 * In the following formula, offsetFermiEnergy - energyOffset is the Fermi
			 * energy re-referenced to the bottom of the conduction band. If b (the nearest
			 * lower core level binding energy) is zero, then this is the electron's initial
			 * energy. (mode 1 assumes target electrons come from the Fermi level.) If b>0
			 * then the Fermi energy - b is still the electron's initial energy, since the
			 * core level energies are referenced to the Fermi level. Either way, adding
			 * deltaE gives the SE's final energy.
			 */
			energySE = deltaE + bEref - nearestSmallerCoreE(deltaE);
			if (energySE + energyCBbottom < minEgenSE)
				return null;
			thetaSE = Math.PI / 2. - theta;
			phiSE = phi + Math.PI;
			// Generate SE, apply energy loss and trajectory change to SE here
			se = new Electron(pe, theta0PE, phi0PE, energySE);
			se.updateDirection(thetaSE, phiSE);
			break;
		case 2:
			be = nearestSmallerCoreE(deltaE);
			if (be > 0.)
				energySE = deltaE + bEref - be;
			else {
				interpInput[0] = deltaE;
				interpInput[1] = randoms[3];
				double energy0SE = tableSEE0.interpolate(interpInput, 3);
				/*
				 * The values in the SEE0 table should range from 0 to EFermi, which represents
				 * the range of allowed values. If the interpolated value overshoots, clip it.
				 */
				if (energy0SE < energyRangeSE0[0])
					energy0SE = energyRangeSE0[0];
				else if (energy0SE > energyRangeSE0[1])
					energy0SE = energyRangeSE0[1];
				energySE = deltaE + energy0SE - energyOffset;
			}
			if (energySE + energyCBbottom < minEgenSE)
				return null;
			thetaSE = Math.PI / 2. - theta;
			phiSE = phi + Math.PI;
			// Generate SE, apply energy loss and trajectory change to SE here
			se = new Electron(pe, theta0PE, phi0PE, energySE);
			se.updateDirection(thetaSE, phiSE);
			break;
		case 3:
			be = nearestSmallerCoreE(deltaE);
			if (be > 0.) { // core level excitation
				energySE = deltaE + bEref - be;
				if (energySE + energyCBbottom < minEgenSE)
					return null;
				/*
				 * I'm going to approximate the angle distribution as isotropic for now.
				 */
				thetaSE = Math.acos(1. - 2. * Math2.rgen.nextDouble());
				phiSE = 2. * Math.PI * Math2.rgen.nextDouble();
				// Generate SE, apply energy loss and trajectory change to SE
				// here
				se = new Electron(pe, theta0PE, phi0PE, energySE);
				se.updateDirection(thetaSE, phiSE);
			} else { // SE generation from extended band
				final double Eq = 2. * kE - deltaE - 2. * Math.sqrt(kE * (kE - deltaE)) * Math.cos(theta);
				final double root = 2. * Math.sqrt(offsetFermiEnergy * (offsetFermiEnergy + deltaE));
				final double sum = 2. * offsetFermiEnergy + deltaE;
				final double Eqmin = sum - root;
				final double Eqmax = sum + root;
				if ((Eqmin <= Eq) && (Eq <= Eqmax)) { // single-electron
					// scattering
					final double[] energytheta = simESEf(Eq, deltaE, randoms[3]);
					energySE = energytheta[0] - energyOffset;
					if (energySE + energyCBbottom < minEgenSE)
						return null;
					// Generate SE in PE direction with correct energy
					se = new Electron(pe, theta0PE, phi0PE, energySE);
					// Determine angles of q vector and rotate SE to this much
					thetaSE = Math.PI / 2. - theta;
					phiSE = phi + Math.PI;
					se.updateDirection(thetaSE, phiSE);
					/*
					 * Now rotate from this direction as required by simESEf (for polar angle) and a
					 * uniformly distributed azimuthal angle.
					 */
					se.updateDirection(energytheta[1], 2. * Math.PI * Math2.rgen.nextDouble());
				} else { // plasmon scattering
					interpInput[0] = deltaE;
					interpInput[1] = randoms[3];
					double energy0SE = tableSEE0.interpolate(interpInput, 3);
					/*
					 * The values in the SEE0 table should range from 0 to EFermi, which represents
					 * the range of allowed values. If the interpolated value overshoots, clip it.
					 */
					if (energy0SE < energyRangeSE0[0])
						energy0SE = energyRangeSE0[0];
					else if (energy0SE > energyRangeSE0[1])
						energy0SE = energyRangeSE0[1];
					energySE = deltaE + energy0SE - energyOffset;
					if (energySE + energyCBbottom < minEgenSE)
						return null;
					/*
					 * For plasmon scattering, mode 3 assumes the plasmon "forgets" the momentum of
					 * the event that created it before it decays into an electron-hole pair. The
					 * angular distribution is therefore isotropic.
					 */
					thetaSE = Math.acos(1. - 2. * Math2.rgen.nextDouble());
					phiSE = 2 * Math.PI * Math2.rgen.nextDouble();
					// Generate SE, apply energy loss and trajectory change to SE
					// here
					se = new Electron(pe, theta0PE, phi0PE, energySE);
					se.updateDirection(thetaSE, phiSE);
				}
			}
			break;
		default:
			se = null;
			break;
		}

		return se;
	}

	/*
	 * simESEf is a private utility that computes the final SE energy for single
	 * electron collisions. It also returns the polar angle of the final SE
	 * trajectory in a reference frame with z axis in the direction of momentum
	 * transfer, q. The inputs are Eq (the energy obtained from (hbar q)^2/(2m)
	 * where q is the momentum transfer in the single electron event, deltaE is the
	 * energy transferred in the event, and r is a random number from 0 to 1. My
	 * derivation (in SimulatingALADingShimizu.nb) follows the one in Mao et al., J.
	 * Appl. Phys. 2009.
	 */
	private double[] simESEf(double Eq, double deltaE, double r) {
		final double q = Math.sqrt(Eq);
		final double kz = (deltaE - Eq) / 2. / q;
		final double kzf = kz + q;
		final double Ezq = kzf * kzf;
		double minE = offsetFermiEnergy + bandgap - Ezq;
		if (minE < 0.)
			minE = 0.;
		final double maxE = offsetFermiEnergy - kz * kz;
		assert minE <= maxE;
		final double Exy = minE * (1. - r) + maxE * r;
		final double ESEf = Exy + Ezq;
		final double theta = Math.acos(kzf / Math.sqrt(ESEf));
		return new double[] { ESEf, theta };
	}

	/*
	 * This is a private utility used to find the largest core energy that is still
	 * smaller than deltaE. We just search this already sorted list from front to
	 * back because small deltaE are much more probable, and our answer is for that
	 * reason most often at or near the front of the list.
	 */
	private double nearestSmallerCoreE(double deltaE) {
		int i;
		for (i = 0; (i < coreEnergies.length) && (coreEnergies[i] <= deltaE); i++)
			;
		if (i == 0)
			return 0.;
		return coreEnergies[i - 1];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#scatterRate(gov.nist
	 * .microanalysis.NISTMonte.Electron)
	 */
	@Override
	public double scatterRate(Electron pe) {
		kEa[0] = pe.getEnergy() + energyOffset; // The PE kinetic energy
		/*
		 * The PE kinetic energy can fall below the minimum in the table for materials
		 * with a bandgap. In this case the actual scatter rate is 0.
		 */
		if (kEa[0] < tableIIMFPEiDomain[0])
			return 0.;
		if (kEa[0] > tableIIMFPEiDomain[1])
			throw new EPQFatalException("PE energy " + Double.toString(kEa[0])
					+ " exceeds interpolation table maximum energy of " + Double.toString(tableIIMFPEiDomain[1]));

		/*
		 * I do only first order interpolation below because I noticed for some tables
		 * that I get negative interpolated values. This happens despite having all
		 * positive values in the table, because the scatter rate approaches 0 at the
		 * Fermi level, leaving open the possibility of overshoot. Possible approaches
		 * to avoid overshoot are to use linear interpolation or to clip the result (as
		 * I do below for other tables). Clipping to 0 seems a bad choice here, because
		 * it results in an infinite inelastic free path.
		 */
		final double result = tableIIMFP.interpolate(kEa, 1);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * gov.nist.nanoscalemetrology.JMONSEL.ScatterMechanism#setMaterial(gov.nist
	 * .microanalysis.EPQLibrary.Material)
	 */
	@Override
	public void setMaterial(Material mat) {
		if (!(mat instanceof SEmaterial))
			throw new EPQFatalException(
					"Material " + mat.toString() + " is not an SEmaterial as required for TabulatedInelasticSM.");

		final SEmaterial semat = (SEmaterial) mat;

		offsetFermiEnergy = semat.getEFermi() + energyOffset;
		energyCBbottom = semat.getEnergyCBbottom();
		workfunction = semat.getWorkfunction();
		bandgap = semat.getBandgap();
		coreEnergies = semat.getCoreEnergyArray();
		if (bandgap > 0.)
			bEref = 0.;
		else
			bEref = semat.getEFermi();

		/*
		 * tableEiDomain must be the energy range that is valid for *all* required
		 * tables that take the PE intial energy as an input parameter.
		 */
		tableEiDomain = tableReducedDeltaE.getDomain()[0];
		final double[] thetaTableEiDomain = tableTheta.getDomain()[0];
		if (thetaTableEiDomain[0] > tableEiDomain[0])
			tableEiDomain[0] = thetaTableEiDomain[0];
		if (thetaTableEiDomain[1] < tableEiDomain[1])
			tableEiDomain[1] = thetaTableEiDomain[1];
		tableIIMFPEiDomain = tableIIMFP.getDomain()[0];
	}

	/**
	 * setMinEgenSE -- Sets the minimum energy for generated SE. Default is 0. This
	 * model measures this energy relative to vacuum = 0. That is, minEgenSE
	 * represents the energy that the SE would have if it immediately escaped the
	 * sample (overcoming the work function barrier) without further loss of energy.
	 * The default value of 0 means the lowest energy SE that are generated are
	 * those that have barely enough energy to escape. It can be set higher than
	 * this to turn off SE with higher energies, if they are not relevant for a
	 * particular simulation. Note that the minEgenSE definition here differs from
	 * the MollerInelasticSM model, which follows the definition in the original
	 * MONSEL series. According to that definition, the minEgenSE referred to the SE
	 * energy inside the sample. Accordingly, it differed from this definition by an
	 * amount equal to the work function.
	 * 
	 * @param minEgenSE The minEgenSE to set.
	 */
	public void setMinEgenSE(double minEgenSE) {
		if (minEgenSE > -workfunction)
			this.minEgenSE = minEgenSE;
		else
			throw new EPQFatalException("Illegal minEgenSE.");
	}

	/**
	 * @return Returns the minEgenSE.
	 */
	public double getMinEgenSE() {
		return minEgenSE;
	}

	/**
	 * Returns the value of the SE method parameter that is being used. This is
	 * normally the same as the value supplied to the constructor.
	 * 
	 * @return the methodSE
	 */
	public int getMethodSE() {
		return methodSE;
	}
}
