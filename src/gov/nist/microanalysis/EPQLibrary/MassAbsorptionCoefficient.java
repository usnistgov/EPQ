package gov.nist.microanalysis.EPQLibrary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import gov.nist.microanalysis.EPQLibrary.LitReference.Author;
import gov.nist.microanalysis.Utility.CSVReader;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * An abstract class that serves as the basis for various different
 * implementations of the mass absorption coefficient (MAC) based on equations,
 * interpolations or tabulations. The SI unit for MAC is m^2/kg. A conventional
 * unit for MAC is cm^2/g = 0.01^2/0.001 = 10^-1 m^2/kg.
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

abstract public class MassAbsorptionCoefficient extends AlgorithmClass {

	/**
	 * <p>
	 * Enumerates the potential sources of uncertainty in the
	 * MassAbsorptionCoefficient calculations.
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
	public enum UncertaintyReason {
		BaseLevel, Below200eV, Below500eV, Below1000eV, ExceedinglyCloseToEdge, VeryCloseToEdge, CloseToEdge,
	};

	public class UncertaintySource {
		private final UncertaintyReason mReason;
		private final int mShell;
		private final double mValue;

		public UncertaintySource(UncertaintyReason reason, int shell, double val) {
			mReason = reason;
			mShell = shell;
			mValue = val;
		}

		/**
		 * Gets the current value assigned to reason
		 * 
		 * @return Returns the reason.
		 */
		public UncertaintyReason getReason() {
			return mReason;
		}

		/**
		 * Gets the current value assigned to shell
		 * 
		 * @return Returns the shell.
		 */
		public int getShell() {
			return mShell;
		}

		/**
		 * Gets the current value assigned to value
		 * 
		 * @return Returns the value.
		 */
		public double getValue() {
			return mValue;
		}

		@Override
		public String toString() {
			final StringBuffer res = new StringBuffer();
			switch (mReason) {
			case BaseLevel:
				res.append("Base");
				break;
			case Below200eV:
				res.append("Below 200 eV");
				break;
			case Below500eV:
				res.append("Below 500 eV");
				break;
			case Below1000eV:
				res.append("Below 1000 eV");
				break;
			case ExceedinglyCloseToEdge:
				res.append("Exceedingly close to ");
				break;
			case VeryCloseToEdge:
				res.append("Very close to ");
				break;
			case CloseToEdge:
				res.append("Close to ");
				break;
			}
			if (mShell != AtomicShell.NoShell)
				res.append(AtomicShell.getIUPACName(mShell));
			res.append(".");
			return res.toString();
		}
	}

	/**
	 * Converts MACs from cm^2/g
	 * 
	 * @param x
	 * @return double
	 */
	static public double fromCmSqrPerGram(double x) {
		assert Math.abs(((ToSI.CM * ToSI.CM) / ToSI.GRAM) - 0.1) < 1.0e-6;
		return 0.1 * x;
	}

	/**
	 * Converts MACs from SI to cm^2/g
	 * 
	 * @param x
	 * @return double
	 */
	static public double toCmSqrPerGram(double x) {
		assert Math.abs(((FromSI.CM * FromSI.CM) / FromSI.GRAM) - 10.) < 1.0e-6;
		return 10.0 * x;
	}

	/**
	 * Converts MACs from SI to cm^2/g
	 * 
	 * @param x
	 * @return UncertainValue
	 */
	static public UncertainValue2 toCmSqrPerGram(UncertainValue2 x) {
		assert Math.abs(((FromSI.CM * FromSI.CM) / FromSI.GRAM) - 10.) < 1.0e-6;
		return UncertainValue2.multiply(10.0, x);
	}

	/**
	 * Mean distance that the specified x-ray will travel in the specified material.
	 * 
	 * @param mat
	 * @param xrt
	 * @return double Meters
	 * @throws EPQException
	 */
	public double meanFreePath(Material mat, XRayTransition xrt) throws EPQException {
		return 1.0 / (compute(mat, xrt) * mat.getDensity()); // m^2 per kg
	}

	/**
	 * Mean distance that an x-ray of the specified energy will travel in the
	 * specified material.
	 * 
	 * @param mat
	 * @param energy
	 * @return double Meters
	 * @throws EPQException
	 */
	public double meanFreePath(Material mat, double energy) throws EPQException {
		return 1.0 / (compute(mat, energy) * mat.getDensity()); // m^2 per kg
	}

	/**
	 * The mean distance that an x-ray of the specific energy will travel in the
	 * considering only absorption by the element elm.
	 *
	 * @param mat
	 * @param elm
	 * @param energy
	 * @return double m^2/kg
	 * @throws EPQException
	 */
	public double meanFreePath(Material mat, Element elm, double energy) throws EPQException {
		return 1.0 / (mat.weightFraction(elm, false) * compute(elm, energy) * mat.getDensity());
	}

	// A simple class to assist in defining sparse MAC tables for specific
	// elements
	private class ElementTabulation {
		private final int[] mAtomicNumber;

		private final double[] mMAC;

		private final int mFamily;

		private final Element mElement;

		ElementTabulation(Element elm, int family, int[] atomicNo, double[] mac) {
			assert (atomicNo.length == mac.length);
			assert (Arrays.binarySearch(atomicNo, Element.elmUub) == -(atomicNo.length + 1));
			mAtomicNumber = atomicNo;
			mMAC = mac;
			mFamily = family;
			mElement = elm;
		}

		double compute(Element el, XRayTransition xrt) {
			assert (mElement.equals(xrt.getElement()) && (xrt.getFamily() == mFamily));
			int i = -1;
			i = Arrays.binarySearch(mAtomicNumber, el.getAtomicNumber());
			return i >= 0 ? mMAC[i] : Double.NaN;
		}
	}

	/**
	 * MassAbsorptionCoefficient - Create an instance of the abstract
	 * MassAbsorptionCoefficient class with the specified name and literature
	 * reference.
	 * 
	 * @param name      String
	 * @param reference String
	 */
	protected MassAbsorptionCoefficient(String name, String reference) {
		super("Mass Absorption Coefficient", name, reference);
	}

	/**
	 * MassAbsorptionCoefficient - Create an instance of the abstract
	 * MassAbsorptionCoefficient class with the specified name and literature
	 * reference.
	 * 
	 * @param name      String
	 * @param reference Reference
	 */
	protected MassAbsorptionCoefficient(String name, LitReference reference) {
		super("Mass Absorption Coefficient", name, reference);
	}

	@Override
	protected void initializeDefaultStrategy() {
		// Don't do anything...
	}

	/**
	 * getAllImplementations - Returns a full list of all available algorithms. Each
	 * item is an implements the MassAbsorptionCoefficient.
	 * 
	 * @return List
	 */
	@Override
	public List<AlgorithmClass> getAllImplementations() {
		return Arrays.asList(mAllImplementations);
	}

	/**
	 * compute - Each different version of the algorithm should implement this
	 * method.
	 * 
	 * @param el     Element - The absorber element
	 * @param energy double - The x-ray energy in Joules
	 * @return double - Absorption per unit length (meter) per unit density (kg/m^3)
	 */
	abstract public double compute(Element el, double energy);

	/**
	 * Compute the specified mass absorption coefficient along with an error
	 * estimate.
	 * 
	 * @param el
	 * @param energy
	 * @return An UncertainValue
	 */
	public UncertainValue2 computeWithUncertaintyEstimate(Element el, double energy) {
		final double res = compute(el, energy);
		return new UncertainValue2(res, "MAC", res * fractionalUncertainty(el, energy));
	}

	/**
	 * Compute the specified mass absorption coefficient along with an error
	 * estimate.
	 * 
	 * @param comp
	 * @param energy
	 * @return An UncertainValue
	 */
	public UncertainValue2 computeWithUncertaintyEstimate(Composition comp, double energy) {
		final double res = compute(comp, energy);
		return new UncertainValue2(res, "MAC", res * fractionalUncertainty(comp, energy));
	}

	/**
	 * Compute the specified mass absorption coefficient along with an error
	 * estimate.
	 * 
	 * @param el
	 * @param xrt
	 * @return An UncertainValue
	 * @throws EPQException
	 */
	public UncertainValue2 computeWithUncertaintyEstimate(Element el, XRayTransition xrt) throws EPQException {
		final double res = compute(el, xrt);
		return new UncertainValue2(res, "MAC", res * fractionalUncertainty(el, xrt));
	}

	/**
	 * Compute the specified mass absorption coefficient along with an error
	 * estimate.
	 * 
	 * @param comp
	 * @param xrt
	 * @return An UncertainValue
	 * @throws EPQException
	 */
	public UncertainValue2 computeWithUncertaintyEstimate(Composition comp, XRayTransition xrt) throws EPQException {
		final double res = compute(comp, xrt);
		return new UncertainValue2(res, "MAC", res * fractionalUncertainty(comp, xrt));
	}

	/**
	 * Estimates the error in the MAC for the specified absorbing material and the
	 * specified x-ray energy.
	 * 
	 * @param comp
	 * @param energy
	 * @return Fractional error estimate 0.0 to &gt;1.0
	 */
	public double fractionalUncertainty(Composition comp, double energy) {
		double err2 = 0.0, macS = 0.0;
		for (final Element el : comp.getElementSet()) {
			final double mac = comp.weightFraction(el, false) * compute(el, energy);
			err2 += Math2.sqr(mac * fractionalUncertainty(el, energy));
			macS += mac;
		}
		return Math.sqrt(err2) / macS;
	}

	/**
	 * Estimates the error in the MAC for the specified absorbing material and the
	 * specified x-ray transition.
	 * 
	 * @param comp
	 * @param xrt
	 * @return Fractional error estimate 0.0 to &gt;1.0
	 */
	public double fractionalUncertainty(Composition comp, XRayTransition xrt) throws EPQException {
		return fractionalUncertainty(comp, xrt.getEnergy());
	}

	/**
	 * Estimates the error in the MAC for the specified absorbing element and the
	 * specified x-ray transition.
	 * 
	 * @param el
	 * @param xrt
	 * @return Fractional error estimate 0.0 to &gt;1.0
	 */
	public double fractionalUncertainty(Element el, XRayTransition xrt) throws EPQException {
		return fractionalUncertainty(el, xrt.getEnergy());
	}

	/**
	 * Returns a value for e that decays from a value of ve0 at e0 to a value of ve1
	 * at e1.
	 *
	 * @param e
	 * @param e0
	 * @param ve0
	 * @param e1
	 * @param ve1
	 * @return double
	 */
	private double decay(double e, double e0, double ve0, double e1, double ve1) {
		assert (e >= e0) && (e <= e1);
		assert ve0 > ve1;
		final double res = ve0 * Math.exp(((Math2.bound(e, e0, e1) - e0) / (e1 - e0)) * Math.log(ve1 / ve0));
		assert (res >= ve1) && (res <= ve0) : Double.toString(res);
		return res;
	}

	/**
	 * Estimates the fractional uncertainty in the MAC for the specified absorbing
	 * element and the specified x-ray energy. The default implementation is based
	 * on the table from Chantler et al.
	 * (http://physics.nist.gov/PhysRefData/FFast/Text2000/sec06.html#tab2)
	 * 
	 * @param el
	 * @param energy
	 * @return Fractional error estimate 0.0 to &gt;1.0
	 */
	public double fractionalUncertainty(Element el, double energy) {
		final double eV = FromSI.eV(energy);
		double err = 0.0;
		if (eV < 200.0)
			// 100-200% reduced to 100% to 60% on 12-June-2018
			err = decay(eV, 0.0, 1.0, 200.0, 0.6);
		else if (eV < 500)
			// 50-100% reduced to 20-60% on 12-June-2018
			err = decay(eV, 200.0, 0.6, 500.0, 0.2);
		else if (eV < 1000)
			// 5% to 20%
			err = decay(eV, 500.0, 0.2, 1000.0, 0.05);
		for (int sh = AtomicShell.K; sh <= AtomicShell.NVII; ++sh)
			if (AtomicShell.exists(el, sh)) {
				final double ee = AtomicShell.getEdgeEnergy(el, sh);
				final double delta = Math.abs((energy - ee) / energy);
				final int family = AtomicShell.getFamily(sh);
				final int[] lmshells = { AtomicShell.LFamily, AtomicShell.MFamily };
				if ((delta < 0.001) || //
						((Math.abs(energy - ee) < ToSI.eV(5.0)) && (Arrays.binarySearch(lmshells, family) >= 0)))
					err = Math.max(err, 0.8);
				else {
					if (ee > energy)
						continue;
					assert delta >= 0.0;
					switch (sh) {
					case AtomicShell.K:
						if (delta < 0.1)
							// 10-20% reduced to 6% on 10-Feb-2015
							err = Math.max(err, 0.06);
						else if (energy < 1.1 * ee)
							// 3% reduced to 2% on 10-Feb-2015
							err = Math.max(err, 0.02);
						else
							err = Math.max(err, 0.01); // 1%
						break;
					case AtomicShell.LI:
					case AtomicShell.MI:
					case AtomicShell.MII:
					case AtomicShell.MIII:
					case AtomicShell.NI: // Special case for N shells
					case AtomicShell.NIII:
					case AtomicShell.NIV:
					case AtomicShell.NV:
						if (delta < 0.15)
							err = Math.max(err, 0.225);
						else
							err = Math.max(err, 0.04); // 4%
						break;
					case AtomicShell.LII:
					case AtomicShell.LIII:
					case AtomicShell.MIV:
					case AtomicShell.MV:
					case AtomicShell.NVI: // Special case for N shells
					case AtomicShell.NVII:
						if (delta < 0.15)
							err = Math.max(err, 0.30);
						else
							err = Math.max(err, 0.10);
						break;
					}
				}
			}
		return err;
	}

	/**
	 * Estimates the fractional uncertainty in the MAC for the specified absorbing
	 * element and the specified x-ray energy. The default implementation is based
	 * on the table from Chantler et al.
	 * (http://physics.nist.gov/PhysRefData/FFast/Text2000/sec06.html#tab2)
	 * 
	 * @param el
	 * @param energy
	 * @return Fractional error estimate 0.0 to &gt;1.0
	 */
	public UncertaintySource uncertaintySource(Element el, double energy) {
		final double eV = FromSI.eV(energy);
		UncertaintySource res = new UncertaintySource(UncertaintyReason.BaseLevel, AtomicShell.NoShell, 0.01);
		double err = 0.01;
		if (eV < 200.0) {
			res = new UncertaintySource(UncertaintyReason.Below200eV, AtomicShell.NoShell, 1.5);
			err = 1.50; // 100-200%
		} else if (eV < 500) {
			err = 0.5 + (((1.0 - 0.5) * (eV - 200)) / (500.0 - 200.0)); // 50-100%
			res = new UncertaintySource(UncertaintyReason.Below500eV, AtomicShell.NoShell,
					0.5 + (((1.0 - 0.5) * (eV - 200)) / (500.0 - 200.0)));
		} else if (eV < 1000) {
			err = 0.05 + (((0.20 - 0.05) * (eV - 500)) / (1000.0 - 500.0));
			res = new UncertaintySource(UncertaintyReason.Below1000eV, AtomicShell.NoShell,
					0.05 + (((0.20 - 0.05) * (eV - 500)) / (1000.0 - 500.0)));
		}
		for (int sh = AtomicShell.K; sh <= AtomicShell.NVII; ++sh)
			if (AtomicShell.exists(el, sh)) {
				final double ee = AtomicShell.getEdgeEnergy(el, sh);
				if (ee > energy)
					continue;
				final double delta = (energy - ee) / energy;
				assert delta > 0.0;
				if (delta < 0.001) {
					if (err < 0.5) {
						err = 0.5;
						res = new UncertaintySource(UncertaintyReason.ExceedinglyCloseToEdge, sh, 0.5);
					}
				} else
					switch (sh) {
					case AtomicShell.K:
						if (delta < 0.1) {
							if (err < 0.15) {
								err = 0.15; // 10-20%
								res = new UncertaintySource(UncertaintyReason.VeryCloseToEdge, sh, 0.15);
							}
						} else if (energy < (1.1 * ee))
							if (err < 0.03) {
								err = 0.03;
								res = new UncertaintySource(UncertaintyReason.CloseToEdge, sh, 0.03);
							}
						break;
					case AtomicShell.LI:
					case AtomicShell.MI:
					case AtomicShell.MII:
					case AtomicShell.MIII:
					case AtomicShell.NI: // Special case for N shells added by
						// NWMR
					case AtomicShell.NIII:
					case AtomicShell.NIV:
					case AtomicShell.NV:
						if (delta < 0.15) {
							if (err < 0.225) {
								err = 0.225;
								res = new UncertaintySource(UncertaintyReason.VeryCloseToEdge, sh, 0.255);
							}
						} else if (delta < 0.4)
							if (err < 0.04) {
								err = 0.04;
								res = new UncertaintySource(UncertaintyReason.CloseToEdge, sh, 0.04);
							}
						break;
					case AtomicShell.LII:
					case AtomicShell.LIII:
					case AtomicShell.MIV:
					case AtomicShell.MV:
					case AtomicShell.NVI: // Special case for N shells added by
						// NWMR
					case AtomicShell.NVII:
						if (delta < 0.15) {
							if (err < 0.3) {
								err = 0.3;
								res = new UncertaintySource(UncertaintyReason.VeryCloseToEdge, sh, 0.3);
							}
						} else if (delta < 0.40)
							if (err < 0.04) {
								err = 0.04;
								res = new UncertaintySource(UncertaintyReason.CloseToEdge, sh, 0.04);
							}
						break;
					}
			}
		return res;
	}

	/**
	 * isAvailable - Is this algorithm implemented for the specified element and
	 * energy. If the MAC algorithm only implements compute(Element
	 * el,XRayTransition), this method will always return false.
	 * 
	 * @param el     Element
	 * @param energy double
	 * @return true if this algorithm implements this particular element and energy
	 */
	public boolean isAvailable(Element el, double energy) {
		return false;
	}

	/**
	 * Determines whether the specified a MAC is available for this algorithm for
	 * the specified XRayTransition in the specified Element.
	 * 
	 * @param el
	 * @param xrt
	 * @return true if the MAC is available, false otherwise.
	 */
	public boolean isAvailable(Element el, XRayTransition xrt) {
		try {
			compute(el, xrt);
			return true;
		} catch (final EPQException ex) {
			return false;
		}

	}

	/**
	 * compute - Computes the mass absorption coefficient for the specified
	 * transition. When ever possible, use this method rather than the alternative
	 * compute(Composition comp,double energy) as this method is available for both
	 * tabulated and computed algorithms. The default implementation of this
	 * algorithm just defers to compute(Element el,xrt.getEnergy()).
	 * 
	 * @param el  Element
	 * @param xrt XRayTransition
	 * @return double
	 * @throws EPQException - If xrt.getEnergy() throws an exception
	 */
	public double compute(Element el, XRayTransition xrt) throws EPQException {
		return compute(el, xrt.getEnergy());
	}

	/**
	 * Compute the mass absorption coefficient for an x-ray of the specified energy
	 * in the specified material. Note the value returned is the absorption for a
	 * material of a nominal density of 1 kg/m^3. Multiply this by the density to
	 * get the true MAC.
	 * 
	 * @param comp   Composition - The absorbing material
	 * @param energy double - The x-ray energy in Joules
	 * @return Absorption per unit length per kg/m<sup>3</sup>
	 */
	final public double compute(Composition comp, double energy) {
		double mac = 0.0;
		for (final Element elm : comp.getElementSet())
			mac += compute(comp, elm, energy);
		return mac;
	}

	/**
	 * Compute the contribution to the material's mass absorption coefficient by the
	 * element elm for an x-ray of the specified energy. Note the value returned is
	 * the absorption for a material of a nominal density of 1 kg/m^3. Multiply this
	 * by the density to get the true MAC.
	 * 
	 * @param comp
	 * @param elm
	 * @param energy
	 * @return Absorption per unit length per kg/m<sup>3</sup>
	 */
	final public double compute(Composition comp, Element elm, double energy) {
		return compute(elm, energy) * comp.weightFraction(elm, false);
	}

	/**
	 * Select at random (weighted by the effective per element macs) an element with
	 * which to associate a photoabsorption event.
	 * 
	 * @param comp
	 * @param energy
	 * @return Element An element in comp
	 */
	final public Element randomizedAbsorbingElement(Composition comp, double energy) {
		double random = Math.random() * compute(comp, energy);
		for (final Element elm : comp.getElementSet()) {
			random -= compute(comp, elm, energy);
			if (random <= 0.0)
				return elm;
		}
		assert false : "Should never get here!!!!";
		return comp.getElementSet().iterator().next();
	}

	/**
	 * compute - Compute the mass absorption coefficient for the specified x-ray in
	 * the specified material. Use this method preferentially over
	 * compute(comp,energy) whenever the XRayTransition is known. This
	 * implementation can use tabulated MACs whereas the energy specific
	 * implementation can not.
	 * 
	 * @param comp Composition - The absorbing material
	 * @param xrt  XRayTransition - The x-ray transition
	 * @return double - Absorption per unit length per kg/m^3
	 */
	final public double compute(Composition comp, XRayTransition xrt) throws EPQException {
		double mac = 0.0;
		for (final Element el : comp.getElementSet())
			mac += compute(el, xrt) * comp.weightFraction(el, false);
		return mac;
	}

	/**
	 * caveat - Specifies the caveats for the specific elemant and x-ray energy.
	 * 
	 * @param el     Element
	 * @param energy double
	 * @return String
	 */
	public String caveat(Element el, double energy) {
		String res = CaveatBase.None;
		for (int sh = AtomicShell.K; sh < AtomicShell.NVII; ++sh)
			if (AtomicShell.exists(el, sh)) {
				final AtomicShell shell = new AtomicShell(el, sh);
				if (Math.abs(shell.getEdgeEnergy() - energy) < ToSI.keV(0.2))
					res = CaveatBase.append(res, "The transition at " + FromSI.keV(energy) + " is close to the "
							+ shell.toString() + " at " + FromSI.keV(shell.getEdgeEnergy()) + " keV.");
			}
		return res;
	}

	/**
	 * caveat - Determines any relevant caveats based on the caveats for the
	 * constituent elements.
	 * 
	 * @param comp   Composition
	 * @param energy double
	 * @return String
	 */
	public String caveat(Composition comp, double energy) {
		String res = CaveatBase.None;
		for (final Element el : comp.getElementSet())
			res = CaveatBase.append(res, caveat(el, energy));
		return res;
	}

	public String caveat(Composition comp) {
		String res = CaveatBase.None;
		final int[] lines = { XRayTransition.KA1, XRayTransition.LA1, XRayTransition.MA1 };
		for (final Element el : comp.getElementSet())
			for (final int line : lines)
				if (XRayTransition.exists(el, line)) {
					final XRayTransition xrt = new XRayTransition(el, line);
					try {
						res = CaveatBase.append(res, caveat(el, xrt.getEnergy()));
					} catch (final EPQException ex) {
						res = CaveatBase.append(res, ex.toString());
					}
				}
		return res;
	}

	/**
	 * evaluateAllImplementations - Produces a map containing the algorithm and the
	 * resulting mass absorption coefficient for the specified Element and x-ray
	 * energy.
	 * 
	 * @param el     Element
	 * @param energy double
	 * @return Map
	 */
	public Map<MassAbsorptionCoefficient, Double> evaluateAllImplementations(Element el, double energy) {
		final Map<MassAbsorptionCoefficient, Double> res = new TreeMap<MassAbsorptionCoefficient, Double>();
		for (final AlgorithmClass alg : getAllImplementations()) {
			final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) alg;
			try {
				res.put(mac, Double.valueOf(mac.compute(el, energy)));
			} catch (final Exception ex) {
				// Just ignore it..
			}
		}
		return res;
	}

	public Map<MassAbsorptionCoefficient, Double> evaluateAllImplementations(Element el, XRayTransition xrt) {
		final Map<MassAbsorptionCoefficient, Double> res = new TreeMap<MassAbsorptionCoefficient, Double>();
		for (final AlgorithmClass alg : getAllImplementations()) {
			final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) alg;
			try {
				res.put(mac, Double.valueOf(mac.compute(el, xrt)));
			} catch (final Exception ex) {
				// Just ignore it..
			}
		}
		return res;
	}

	/**
	 * Ruste79 - J Ruste, J Microsc Spectrosc Electron 4, 123 (1979)
	 */
	public static class Ruste79MassAbsorptionCoefficient extends MassAbsorptionCoefficient {
		public Ruste79MassAbsorptionCoefficient() {
			super("Ruste 79", "J. Ruste, J. Microsc. Spectrosc. Electron. 4, 123 (1979)");
		}

		ElementTabulation mCarbon = new ElementTabulation(Element.C, AtomicShell.KFamily,
				new int[] { Element.elmB, Element.elmC, Element.elmSi, Element.elmTi, Element.elmV, Element.elmCr,
						Element.elmFe, Element.elmZr, Element.elmNb, Element.elmMo, Element.elmTa, Element.elmW },
				new double[] { 37020, 2373, 36980, 8094, 9236, 10482, 13300, 31130, 24203, 15500, 20000, 21580 });

		@Override
		final public double compute(Element el, double energy) {
			throw new EPQFatalException("The " + toString()
					+ " implementation of the mass absorption coefficient does not support computing the MAC by energy.");
		}

		@Override
		final public double compute(Element el, XRayTransition xrt) throws EPQException {
			double res = Double.NaN;
			if ((xrt.getFamily() == AtomicShell.KFamily) && (xrt.getElement().equals(Element.C)))
				res = mCarbon.compute(el, xrt);
			if (Double.isNaN(res))
				throw new EPQException("MAC unavailable in " + toString() + " implementation.");
			return fromCmSqrPerGram(res);
		}
	}

	public static final MassAbsorptionCoefficient Ruste79 = new Ruste79MassAbsorptionCoefficient();

	public static class Pouchou1991MassAbsorptionCoefficient extends MassAbsorptionCoefficient {
		public Pouchou1991MassAbsorptionCoefficient() {
			super("Pouchou & Pichoir 1991",
					"Pouchou & Pichoir in Electron Probe Quantitation, Eds Heinrich and Newbury");
		}

		ElementTabulation mBoron = new ElementTabulation(Element.B, AtomicShell.KFamily,
				new int[] { Element.elmB, Element.elmC, Element.elmN, Element.elmO, Element.elmAl, Element.elmSi,
						Element.elmTi, Element.elmV, Element.elmCr, Element.elmFe, Element.elmCo, Element.elmNi,
						Element.elmZr, Element.elmNb, Element.elmMo, Element.elmLa, Element.elmTa, Element.elmW,
						Element.elmU },
				new double[] { 3500.0, 6750.0, 11000.0, 16500.0, 64000.0, 80000.0, 15000.0, 18000.0, 20700.0, 27800.0,
						32000.0, 37000.0, 4400.0, 4500.0, 4600.0, 2500.0, 23000.0, 21000.0, 7400.0 });

		ElementTabulation mCarbon = new ElementTabulation(Element.C, AtomicShell.KFamily,
				new int[] { Element.elmB, Element.elmC, Element.elmSi, Element.elmTi, Element.elmV, Element.elmCr,
						Element.elmFe, Element.elmZr, Element.elmNb, Element.elmMo, Element.elmHf, Element.elmTa,
						Element.elmW },
				new double[] { 39000.0, 2170.0, 35000.0, 8100.0, 8850.0, 10700.0, 13500.0, 25000.0, 24000.0, 20500.0,
						18000.0, 17000.0, 18000.0 });

		ElementTabulation mNitrogen = new ElementTabulation(Element.N, AtomicShell.KFamily,
				new int[] { Element.elmB, Element.elmN, Element.elmAl, Element.elmSi, Element.elmTi, Element.elmV,
						Element.elmCr, Element.elmFe, Element.elmZr, Element.elmNb, Element.elmMo, Element.elmHf,
						Element.elmTa },
				new double[] { 15800.0, 1640.0, 13800.0, 17000.0, 4270.0, 4950.0, 5650.0, 7190.0, 24000.0, 25000.0,
						25800.0, 14000.0, 15500.0 });

		private final int[] mKas = { XRayTransition.KA1, XRayTransition.KA2 }; // sorted

		private final int[] mLas = { XRayTransition.LA1, XRayTransition.LA2 }; // sorted

		private final int[] mLbs = { XRayTransition.LB15, XRayTransition.LB2, XRayTransition.LB5, XRayTransition.LB6,
				XRayTransition.LB7, XRayTransition.LB1, XRayTransition.LB17, XRayTransition.LB10, XRayTransition.LB3,
				XRayTransition.LB4, XRayTransition.LB9 }; // sorted

		@Override
		final public double compute(Element el, double energy) {
			return Heinrich86.compute(el, energy);
		}

		@Override
		final public double compute(Element el, XRayTransition xrt) throws EPQException {
			double res = Double.NaN;
			if (xrt.getFamily() == AtomicShell.KFamily)
				switch (xrt.getElement().getAtomicNumber()) {
				case Element.elmB:
					res = mBoron.compute(el, xrt);
					break;
				case Element.elmC:
					res = mCarbon.compute(el, xrt);
					break;
				case Element.elmN:
					res = mNitrogen.compute(el, xrt);
					break;
				}
			switch (xrt.getElement().getAtomicNumber()) {
			case Element.elmSi:
				if ((el.getAtomicNumber() == Element.elmTa)
						&& (Arrays.binarySearch(mKas, xrt.getTransitionIndex()) >= 0))
					res = 1490.0;
				break;
			case Element.elmS:
				if ((el.getAtomicNumber() == Element.elmAu)
						&& (Arrays.binarySearch(mKas, xrt.getTransitionIndex()) >= 0))
					res = 2200.0;
				break;
			case Element.elmCu:
				if (el.getAtomicNumber() == Element.elmCu)
					if (Arrays.binarySearch(mLbs, xrt.getTransitionIndex()) >= 0)
						res = 6750.0;
					else if (Arrays.binarySearch(mLas, xrt.getTransitionIndex()) >= 0)
						res = 1755.0;
				break;
			case Element.elmAs:
				if ((el.getAtomicNumber() == Element.elmGa)
						&& (Arrays.binarySearch(mLas, xrt.getTransitionIndex()) >= 0))
					res = 7000.0;
				break;
			case Element.elmMo:
				if ((el.getAtomicNumber() == Element.elmAu)
						&& (Arrays.binarySearch(mLas, xrt.getTransitionIndex()) >= 0))
					res = 2200.0;
				break;
			case Element.elmGd:
				if ((el.getAtomicNumber() == Element.elmGd) && (xrt.getTransitionIndex() == XRayTransition.MB))
					res = 4700.0;
				break;
			case Element.elmHf:
				if ((el.getAtomicNumber() == Element.elmHf) && (xrt.getTransitionIndex() == XRayTransition.MB))
					res = 3000.0;
				break;
			case Element.elmTa:
				if ((el.getAtomicNumber() == Element.elmTa) && (xrt.getTransitionIndex() == XRayTransition.MB))
					res = 2500.0;
				break;
			case Element.elmW:
				if ((el.getAtomicNumber() == Element.elmW) && (xrt.getTransitionIndex() == XRayTransition.MB))
					res = 2080.0;
				break;
			case Element.elmAu:
				if ((el.getAtomicNumber() == Element.elmPt) && (xrt.getTransitionIndex() == XRayTransition.MB))
					res = 2250.0;
				break;
			case Element.elmHg:
				if ((el.getAtomicNumber() == Element.elmAu) && (xrt.getTransitionIndex() == XRayTransition.MB))
					res = 2170.0;
				break;
			case Element.elmSc:
				if ((el.getAtomicNumber() == Element.elmSc)
						&& (Arrays.binarySearch(mLas, xrt.getTransitionIndex()) >= 0))
					res = 4750.0;
				break;
			case Element.elmTi:
				if ((el.getAtomicNumber() == Element.elmTi)
						&& (Arrays.binarySearch(mLas, xrt.getTransitionIndex()) >= 0))
					res = 4550.0;
				break;
			case Element.elmV:
				if ((el.getAtomicNumber() == Element.elmV)
						&& (Arrays.binarySearch(mLas, xrt.getTransitionIndex()) >= 0))
					res = 4370.0;
				break;
			case Element.elmCr:
				if ((el.getAtomicNumber() == Element.elmCr)
						&& (Arrays.binarySearch(mLas, xrt.getTransitionIndex()) >= 0))
					res = 3850.0;
				break;
			case Element.elmMn:
				if ((el.getAtomicNumber() == Element.elmMn)
						&& (Arrays.binarySearch(mLas, xrt.getTransitionIndex()) >= 0))
					res = 3340.0;
				break;
			case Element.elmFe:
				if ((el.getAtomicNumber() == Element.elmFe)
						&& (Arrays.binarySearch(mLas, xrt.getTransitionIndex()) >= 0))
					res = 3350.0;
				break;
			case Element.elmCo:
				if ((el.getAtomicNumber() == Element.elmCo)
						&& (Arrays.binarySearch(mLas, xrt.getTransitionIndex()) >= 0))
					res = 3260.0;
				break;
			case Element.elmNi:
				if ((el.getAtomicNumber() == Element.elmNi)
						&& (Arrays.binarySearch(mLas, xrt.getTransitionIndex()) >= 0))
					res = 3560.0;
				break;
			}
			// If the transition and absorber are not one of the special cases
			// use
			// Heinrich's IXCOM 11
			return Double.isNaN(res) ? Heinrich86.compute(el, xrt) : fromCmSqrPerGram(res);
		}
	}

	public static final MassAbsorptionCoefficient Pouchou1991 = new Pouchou1991MassAbsorptionCoefficient();

	/**
	 * PouchouPichoir88 - JL Pouchou and FMA. Pichoir, "Determination of Mass
	 * Absorption Coefficients for Soft X-Rays by use of the Electron Microprobe"
	 * Microbeam Analysis, Ed DE Newbury, San Francisco Press, 1988, p 319-324
	 */
	public static class PouchouPichoir88MassAbsorptionCoefficient extends MassAbsorptionCoefficient {
		public PouchouPichoir88MassAbsorptionCoefficient() {
			super("Pouchou & Pichoir 1988",
					"J. L. Pouchou and F. M. A. Pichoir, 'Determination of Mass Absorption Coefficients for Soft X-Rays by use of the Electron Microprobe' Microbeam Analysis, Ed. D. E. Newbury, San Francisco Press, 1988, p. 319-324");
		}

		ElementTabulation mBoron = new ElementTabulation(Element.B, AtomicShell.KFamily,
				new int[] { Element.elmB, Element.elmC, Element.elmN, Element.elmO, Element.elmAl, Element.elmSi,
						Element.elmTi, Element.elmV, Element.elmCr, Element.elmFe, Element.elmCo, Element.elmNi,
						Element.elmZr, Element.elmNb, Element.elmMo, Element.elmLa, Element.elmTa, Element.elmW,
						Element.elmU },
				new double[] { 3471, 6750, 11000, 16500, 64000, 80000, 15000, 18000, 20700, 27800, 32000, 37000, 4400,
						4500, 4600, 2500, 23000, 21000, 7400 });

		ElementTabulation mCarbon = new ElementTabulation(Element.C, AtomicShell.KFamily,
				new int[] { Element.elmB, Element.elmC, Element.elmSi, Element.elmTi, Element.elmV, Element.elmCr,
						Element.elmFe, Element.elmZr, Element.elmNb, Element.elmMo, Element.elmHf, Element.elmTa,
						Element.elmW },
				new double[] { 39000, 2170, 35000, 8097, 8850, 10700, 13150, 25000, 24000, 20500, 18000, 17000,
						18000 });

		ElementTabulation mNitrogen = new ElementTabulation(Element.N, AtomicShell.KFamily,
				new int[] { Element.elmB, Element.elmN, Element.elmAl, Element.elmSi, Element.elmTi, Element.elmV,
						Element.elmCr, Element.elmFe, Element.elmZr, Element.elmNb, Element.elmMo, Element.elmHf,
						Element.elmTa },
				new double[] { 15800, 1640, 13800, 17000, 4270, 4950, 5650, 7190, 24000, 25000, 25800, 14000, 15500 });

		ElementTabulation mAluminum = new ElementTabulation(Element.Al, AtomicShell.KFamily,
				new int[] { Element.elmAl, Element.elmCu }, new double[] { 393, 4588 });

		ElementTabulation mSilicon = new ElementTabulation(Element.Si, AtomicShell.KFamily,
				new int[] { Element.elmSi, Element.elmTa }, new double[] { 356, 1500 });

		ElementTabulation mSulfur = new ElementTabulation(Element.S, AtomicShell.KFamily,
				new int[] { Element.elmAu, Element.elmHg }, new double[] { 2200, 850 });

		ElementTabulation mCopper = new ElementTabulation(Element.Cu, AtomicShell.LFamily,
				new int[] { Element.elmAl, Element.elmNi, Element.elmCu }, new double[] { 1464, 11879, 1755 });

		@Override
		final public double compute(Element el, double energy) {
			throw new EPQFatalException("The " + toString()
					+ " implementation of the mass absorption coefficient does not support computing the MAC by energy.");
		}

		@Override
		final public double compute(Element el, XRayTransition xrt) throws EPQException {
			double res = Double.NaN;
			switch (xrt.getFamily()) {
			case AtomicShell.KFamily:
				switch (xrt.getElement().getAtomicNumber()) {
				case Element.elmB:
					res = mBoron.compute(el, xrt);
					break;
				case Element.elmC:
					res = mCarbon.compute(el, xrt);
					break;
				case Element.elmN:
					res = mNitrogen.compute(el, xrt);
					break;
				case Element.elmAl:
					res = mAluminum.compute(el, xrt);
					break;
				case Element.elmSi:
					res = mSilicon.compute(el, xrt);
					break;
				case Element.elmS:
					res = mSulfur.compute(el, xrt);
					break;
				}
				break;
			case AtomicShell.LFamily:
				switch (xrt.getElement().getAtomicNumber()) {
				case Element.elmCu:
					res = mCopper.compute(el, xrt);
					break;
				case Element.elmSc:
					if (el.getAtomicNumber() == Element.elmSc)
						res = 4750;
					break;
				case Element.elmTi:
					if (el.getAtomicNumber() == Element.elmTi)
						res = 4550;
					break;
				case Element.elmV:
					if (el.getAtomicNumber() == Element.elmV)
						res = 4370;
					break;
				case Element.elmCr:
					if (el.getAtomicNumber() == Element.elmCr)
						res = 3850;
					break;
				case Element.elmMn:
					if (el.getAtomicNumber() == Element.elmMn)
						res = 3340;
					break;
				case Element.elmFe:
					if (el.getAtomicNumber() == Element.elmFe)
						res = 3350;
					break;
				case Element.elmCo:
					if (el.getAtomicNumber() == Element.elmCo)
						res = 3260;
					break;
				case Element.elmNi:
					if (el.getAtomicNumber() == Element.elmNi)
						res = 3560;
					break;
				case Element.elmZn:
					if (el.getAtomicNumber() == Element.elmZn)
						res = 1500;
					break;
				case Element.elmGe:
					if (el.getAtomicNumber() == Element.elmGe)
						res = 1240;
					break;
				case Element.elmAs:
					if (el.getAtomicNumber() == Element.elmGa)
						res = 7000;
					break;
				case Element.elmNb:
					if (el.getAtomicNumber() == Element.elmNb)
						res = 779;
					break;
				case Element.elmMo:
					if (el.getAtomicNumber() == Element.elmAu)
						res = 2200;
					break;
				case Element.elmW:
					if (el.getAtomicNumber() == Element.elmW)
						res = 1258;
					break;
				}
				break;
			case AtomicShell.MFamily:
				if (el.getAtomicNumber() == Element.elmAu)
					res = 1103;
				break;
			}
			if (Double.isNaN(res))
				throw new EPQException("MAC unavailable in " + toString() + " implementation.");
			return fromCmSqrPerGram(res);
		}
	}

	public static final MassAbsorptionCoefficient PouchouPichoir88 = new PouchouPichoir88MassAbsorptionCoefficient();

	/**
	 * Henke82 - BL Henke, P Lee, TJ Tanaka, RL Shimabukuro and BK Fijikawa, Atomic
	 * Data Nucl Data Tables 27, 1 (1982)
	 */
	public static class Henke82MassAbsorptionCoefficient extends MassAbsorptionCoefficient {
		public Henke82MassAbsorptionCoefficient() {
			super("Henke 1982",
					"B. L. Henke, P. Lee, T. J. Tanaka, R. L. Shimabukuro and B. K. Fijikawa, Atomic Data Nucl. Data Tables 27, 1 (1982)");
		}

		ElementTabulation mBoronK = new ElementTabulation(Element.B, AtomicShell.KFamily,
				new int[] { Element.elmB, Element.elmC, Element.elmN, Element.elmAl, Element.elmSi, Element.elmTi,
						Element.elmV, Element.elmCr, Element.elmFe, Element.elmCo, Element.elmNi, Element.elmZr,
						Element.elmNb, Element.elmMo, Element.elmLa, Element.elmTa, Element.elmW, Element.elmU },
				new double[] { 3350, 6350, 11200, 64000, 84000, 15300, 16700, 20700, 27600, 30900, 35700, 8270, 6560,
						5610, 3730, 20800, 19700, 9020 });

		ElementTabulation mCarbonK = new ElementTabulation(Element.C, AtomicShell.KFamily,
				new int[] { Element.elmB, Element.elmC, Element.elmSi, Element.elmTi, Element.elmV, Element.elmCr,
						Element.elmFe, Element.elmZr, Element.elmNb, Element.elmMo, Element.elmTa, Element.elmW },
				new double[] { 37000, 2350, 36800, 8090, 8840, 10600, 13900, 21600, 19400, 16400, 18400, 18800 });

		ElementTabulation mNitrogenK = new ElementTabulation(Element.N, AtomicShell.KFamily,
				new int[] { Element.elmMo }, new double[] { 20200 });

		ElementTabulation mOxygenK = new ElementTabulation(Element.O, AtomicShell.KFamily,
				new int[] { Element.elmLi, Element.elmB, Element.elmO, Element.elmMg, Element.elmAl, Element.elmSi,
						Element.elmTi, Element.elmCr, Element.elmMn, Element.elmFe, Element.elmCo, Element.elmNi,
						Element.elmCu, Element.elmZn, Element.elmGa, Element.elmY, Element.elmZr, Element.elmNb,
						Element.elmMo, Element.elmRu, Element.elmSn, Element.elmBa, Element.elmLa, Element.elmTa,
						Element.elmW, Element.elmPb, Element.elmBi },
				new double[] { 1600, 7420, 1200, 5170, 6720, 8790, 22100, 3140, 3470, 4000, 4410, 5120, 5920, 6550,
						7090, 15100, 14800, 15300, 16700, 19700, 23100, 4560, 4690, 10600, 11000, 12500, 12700 });

		ElementTabulation mAluminumK = new ElementTabulation(Element.Al, AtomicShell.KFamily,
				new int[] { Element.elmAl, Element.elmCu }, new double[] { 403, 4550 });

		ElementTabulation mSiliconK = new ElementTabulation(Element.Si, AtomicShell.KFamily,
				new int[] { Element.elmSi, Element.elmTa }, new double[] { 350, 3760 });

		ElementTabulation mCopperL = new ElementTabulation(Element.Cu, AtomicShell.LFamily,
				new int[] { Element.elmAl, Element.elmNi }, new double[] { 1450, 11700 });

		ElementTabulation mZincL = new ElementTabulation(Element.Zn, AtomicShell.LFamily, new int[] { Element.elmZn },
				new double[] { 1550 });

		ElementTabulation mGermaniumL = new ElementTabulation(Element.Ge, AtomicShell.LFamily,
				new int[] { Element.elmGe }, new double[] { 1260 });

		ElementTabulation mNiobiumL = new ElementTabulation(Element.Nb, AtomicShell.LFamily,
				new int[] { Element.elmNb }, new double[] { 726 });

		ElementTabulation mMolybdenumL = new ElementTabulation(Element.Mo, AtomicShell.LFamily,
				new int[] { Element.elmAu }, new double[] { 3680 });

		@Override
		final public double compute(Element el, double energy) {
			throw new EPQFatalException("The " + toString()
					+ " implementation of the mass absorption coefficient does not support computing the MAC by energy.");
		}

		@Override
		final public double compute(Element el, XRayTransition xrt) throws EPQException {
			double res = Double.NaN;
			switch (xrt.getFamily()) {
			case AtomicShell.KFamily:
				switch (xrt.getElement().getAtomicNumber()) {
				case Element.elmB:
					res = mBoronK.compute(el, xrt);
					break;
				case Element.elmC:
					res = mCarbonK.compute(el, xrt);
					break;
				case Element.elmN:
					res = mNitrogenK.compute(el, xrt);
					break;
				case Element.elmO:
					res = mOxygenK.compute(el, xrt);
					break;
				case Element.elmAl:
					res = mAluminumK.compute(el, xrt);
					break;
				case Element.elmSi:
					res = mSiliconK.compute(el, xrt);
					break;
				}
				break;
			case AtomicShell.LFamily:
				switch (xrt.getElement().getAtomicNumber()) {
				case Element.elmCu:
					res = mCopperL.compute(el, xrt);
					break;
				case Element.elmZn:
					res = mZincL.compute(el, xrt);
					break;
				case Element.elmGe:
					res = mGermaniumL.compute(el, xrt);
					break;
				case Element.elmNb:
					res = mNiobiumL.compute(el, xrt);
					break;
				case Element.elmMo:
					res = mMolybdenumL.compute(el, xrt);
					break;
				}
				break;
			}
			if (Double.isNaN(res))
				throw new EPQException("MAC unavailable in " + toString() + " implementation.");
			return fromCmSqrPerGram(res);
		}
	}

	public static final MassAbsorptionCoefficient Henke82 = new Henke82MassAbsorptionCoefficient();

	/**
	 * BastinHeijligers89 - The mass absorption coefficients for selected elements
	 * calculated by Bastin and Heijligers (1985, 1988, 1989) as quoted in Scott,
	 * Love &amp; Reed, Quantitative Electron-Probe Microanalysis, 2nd ed.
	 */
	public static class BastinHeijligers89MassAbsorptionCoefficient extends MassAbsorptionCoefficient {
		public BastinHeijligers89MassAbsorptionCoefficient() {
			super("Bastin & Heijligers (1985, 1988, 1989)",
					"as quoted in Scott, Love & Reed, Quantitative Electron-Probe Microanalysis, 2nd ed.");
		}

		ElementTabulation mBoron = new ElementTabulation(Element.B, AtomicShell.KFamily,
				new int[] { Element.elmB, Element.elmC, Element.elmN, Element.elmAl, Element.elmSi, Element.elmTi,
						Element.elmV, Element.elmCr, Element.elmFe, Element.elmCo, Element.elmNi, Element.elmZr,
						Element.elmNb, Element.elmMo, Element.elmLa, Element.elmTa, Element.elmW, Element.elmU },
				new double[] { 3350, 6350, 11200, 65000, 85000, 15300, 18300, 20700, 27600, 30900, 35700, 4100, 4300,
						4200, 2600, 19500, 19000, 7500 });

		ElementTabulation mCarbon = new ElementTabulation(Element.C, AtomicShell.KFamily,
				new int[] { Element.elmB, Element.elmC, Element.elmSi, Element.elmTi, Element.elmV, Element.elmCr,
						Element.elmFe, Element.elmZr, Element.elmNb, Element.elmMo, Element.elmTa, Element.elmW },
				new double[] { 41000, 2373, 37000, 9400, 10100, 10950, 13500, 24000, 23200, 19200, 15350, 16400 });

		ElementTabulation mNitrogen = new ElementTabulation(Element.N, AtomicShell.KFamily,
				new int[] { Element.elmB, Element.elmN, Element.elmAl, Element.elmSi, Element.elmTi, Element.elmV,
						Element.elmCr, Element.elmFe, Element.elmZr, Element.elmNb, Element.elmMo, Element.elmHf,
						Element.elmTa },
				new double[] { 15800, 1810, 13100, 17170, 4360, 4790, 5360, 7190, 24000, 25000, 25000, 14050, 15000 });

		ElementTabulation mOxygen = new ElementTabulation(Element.O, AtomicShell.KFamily,
				new int[] { Element.elmLi, Element.elmB, Element.elmO, Element.elmMg, Element.elmAl, Element.elmSi,
						Element.elmTi, Element.elmCr, Element.elmMn, Element.elmFe, Element.elmCo, Element.elmNi,
						Element.elmCu, Element.elmZn, Element.elmGa, Element.elmY, Element.elmZr, Element.elmNb,
						Element.elmMo, Element.elmRu, Element.elmSn, Element.elmTa, Element.elmW, Element.elmPb,
						Element.elmBi },
				new double[] { 1450, 6130, 983, 4490, 5310, 8040, 8540, 2180, 2960, 3630, 4220, 4380, 5500, 5938, 7000,
						14100, 13600, 15127, 13999, 17000, 4900, 8357, 9100, 6764, 4430 });

		@Override
		final public double compute(Element el, double energy) {
			throw new EPQFatalException("The " + toString()
					+ " implementation of the mass absorption coefficient does not support computing the MAC by energy.");
		}

		@Override
		final public double compute(Element el, XRayTransition xrt) throws EPQException {
			double res = Double.NaN;
			if (xrt.getFamily() == AtomicShell.KFamily)
				switch (xrt.getElement().getAtomicNumber()) {
				case Element.elmB:
					res = mBoron.compute(el, xrt);
					break;
				case Element.elmC:
					res = mCarbon.compute(el, xrt);
					break;
				case Element.elmN:
					res = mNitrogen.compute(el, xrt);
					break;
				case Element.elmO:
					res = mOxygen.compute(el, xrt);
					break;
				}
			if (Double.isNaN(res))
				throw new EPQException("MAC unavailable in " + toString() + " implementation.");
			return fromCmSqrPerGram(res);
		}
	}

	public static final MassAbsorptionCoefficient BastinHeijligers89 = new BastinHeijligers89MassAbsorptionCoefficient();

	private static class HeinrichBase extends MassAbsorptionCoefficient {
		private final boolean mEmulateDtsa;

		private HeinrichBase(boolean dtsaCutoff) {
			super(dtsaCutoff ? "Heinrich IXCOM 11 (DTSA)" : "Heinrich IXCOM 11",
					"Heinrich KFJ. in Proc. 11th Int. Congr. X-ray Optics & Microanalysis, Brown JD, Packwood RH (eds). Univ. Western Ontario: London, 1986; 67");
			mEmulateDtsa = dtsaCutoff;
		}

		@Override
		public boolean isAvailable(Element el, double energy) {
			energy = FromSI.eV(energy);
			final int z = el.getAtomicNumber();
			return (energy > 0.0) && (energy <= 1.0e5) && (z >= 3) && (z <= 95);
		}

		@Override
		final public double compute(Element el, double energySI) {
			final double energy = FromSI.eV(energySI);
			/**
			 * Ref: Heinrich's formula as implemented by Myklebust translated into Java
			 */
			final double z = el.getAtomicNumber();
			/*
			 * This expression only works for x-ray energies below the K-edge and above the
			 * K-edge for Z < 50. Energies above the K-edge for elements Z > 49 are
			 * completely nuts.
			 */
			double nm = 0.0, cc = 0.0, az = 0.0;
			if (energy <= 10.0)
				return 1e6;
			if ((z < 3) || (z > 95))
				return 0.001;
			double bias = 0;
			// Z1 = z - 1;
			final double eeK = FromSI.eV(AtomicShell.getEdgeEnergy(el, AtomicShell.K));
			final double eeNI = FromSI.eV(AtomicShell.getEdgeEnergy(el, AtomicShell.NI));
			if (energy > eeK) { // energy is above the K edge.
				if (z < 6) {
					cc = (1.808599e-3 * z) - 2.87536e-4;
					az = (((-14.15422 * z) + 155.6055) * z) + 24.4545;
					bias = (18.2 * z) - 103;
					nm = (((-0.01273815 * z) + 0.02652873) * z) + 3.34745;
				} else {
					cc = 5.253e-3 + (z * (1.33257e-3 + (z * (-7.5937e-5 + (z * (1.69357e-6 + (-1.3975e-8 * z)))))));
					az = ((((-0.152624 * z) + 6.52) * z) + 47) * z;
					nm = 3.112 - (0.0121 * z);
					if (mEmulateDtsa) {
						/**
						 * These special conditions are not mentioned in theIXCOM 11 article but are
						 * implemented in DTSA
						 */
						assert (energy > eeK);
						if (z >= 50)
							az = ((((-0.015 * z) + 3.52) * z) + 47) * z;
						if (z >= 57)
							cc = 2.0e-4 + ((1.0e-4 - z) * z);
					}
				}
			} else {
				final double eeLIII = FromSI.eV(AtomicShell.getEdgeEnergy(el, AtomicShell.LIII));
				if (energy > eeLIII) {
					// energy is below K-edge & above L3-edge
					cc = -0.0924e-3 + (z * (0.141478e-3
							+ (z * (-0.00524999e-3 + (z * (9.85296E-8 + (z * (-9.07306E-10 + (z * 3.19245E-12)))))))));
					az = ((((((-1.16286e-4 * z) + 0.01253775) * z) + 0.067429) * z) + 17.8096) * z;
					nm = (((-4.982E-5 * z) + 1.889e-3) * z) + 2.7575;
					final double eeLII = FromSI.eV(AtomicShell.getEdgeEnergy(el, AtomicShell.LII));
					if ((energy < FromSI.eV(AtomicShell.getEdgeEnergy(el, AtomicShell.LI))) && (energy >= eeLII))
						cc *= 0.858;
					if (energy < eeLII)
						cc *= (0.8933 + (z * (-8.29e-3 + (6.38E-5 * z))));
				} else {
					final double eeMI = FromSI.eV(AtomicShell.getEdgeEnergy(el, AtomicShell.MI));
					if ((energy <= eeLIII) && (energy > eeMI)) {
						nm = (((((4.4509E-6 * z) - 1.08246e-3) * z) + 0.084597) * z) + 0.5385;
						if (z < 30)
							cc = (((((((7.2773258e-9 * z) - 1.1641145e-6) * z) + 6.9602789e-5) * z) - 1.8517159e-3) * z)
									+ 1.889757e-2;
						else
							cc = (((((((1.497763e-10 * z) - 4.0585911e-8) * z) + 4.0424792e-6) * z) - 1.73663566e-4)
									* z) + 3.0039e-3;
						az = ((((((-1.8641019e-4 * z) + 2.63199611e-2) * z) - 0.822863477) * z) + 10.2575657) * z;
						if (z < 61)
							bias = ((((((-1.683474e-4 * z) + 0.018972278) * z) - 0.536839169) * z) + 5.654) * z;
						else
							bias = ((((((3.1779619e-3 * z) - 0.699473097) * z) + 51.114164) * z) - 1232.4022) * z;
					} else {
						final double eeMV = FromSI.eV(AtomicShell.getEdgeEnergy(el, AtomicShell.MV));
						if (energy >= eeMV) {
							final double eeMIV = FromSI.eV(AtomicShell.getEdgeEnergy(el, AtomicShell.MIV));
							az = (4.62 - (0.04 * z)) * z;
							cc = (((((-1.29086e-9 * z) + 2.209365e-7) * z) - 7.83544e-6) * z) + 7.7708e-5;
							cc *= (((((4.865E-6 * z) - 0.0006561) * z) + 0.0162) * z) + 1.406;
							bias = ((((3.78e-4 * z) - 0.052) * z) + 2.51) * eeMIV;
							nm = 3 - (0.004 * z);
							if (energy >= FromSI.eV(AtomicShell.getEdgeEnergy(el, AtomicShell.MII))) {
								assert energy <= eeMI : el + " @ " + Double.toString(energy);
								cc *= ((((-0.0001285 * z) + 0.01955) * z) + 0.584);
							} else if (energy >= FromSI.eV(AtomicShell.getEdgeEnergy(el, AtomicShell.MIII))) {
								assert (energy < FromSI.eV(AtomicShell.getEdgeEnergy(el, AtomicShell.MII)));
								cc *= (0.001366 * z) + 1.082;
							} else if (energy >= eeMIV) {
								assert (energy < FromSI.eV(AtomicShell.getEdgeEnergy(el, AtomicShell.MIII)));
								cc *= 0.95;
							} else {
								assert (energy < eeMIV);
								assert (energy >= eeMV);
								cc *= (((4.0664e-4 * z) - 4.8e-2) * z) + 1.6442;
							}
						} else {
							assert (energy < eeMV);
							cc = 1.08 * ((((((-6.69827e-9 * z) + 1.707073e-6) * z) - 1.4653e-4) * z) + 4.3156e-3);
							az = ((((5.39309e-3 * z) - 0.61239) * z) + 19.64) * z;
							bias = (4.5 * z) - 113.0;
							nm = 0.3736 + (0.02401 * z);
						}
					}
				}
			}
			double mu;
			if ((energy > eeNI) || Double.isNaN(eeNI)) {
				mu = (cc * Math.pow(12397 / energy, nm) * z * z * z * z) / el.getAtomicWeight();
				mu = mu * (1 - Math.exp((bias - energy) / az));
			} else {
				mu = ((cc * Math.pow(12397 / energy, nm) * z * z * z * z) / el.getAtomicWeight())
						* (1 - Math.exp((bias - eeNI) / az));
				final double cutoff = getCutOff(z);
				if (energy > cutoff) // Added NWMR 18-Feb-2008
					mu = (1.02 * mu * (energy - cutoff)) / (eeNI - cutoff);
			}
			return fromCmSqrPerGram(mu);
			// transmission fraction per meter of path length per unit density
		}

		private double getCutOff(double z) {
			return mEmulateDtsa ? 10.0 : (((0.252 * z) - 31.1812) * z) + 1042.0;
		}

		@Override
		public String caveat(Element el, double energy) {
			String res = CaveatBase.None;
			final NumberFormat nf = new HalfUpFormat("0.0");
			for (int sh = AtomicShell.K; sh < AtomicShell.NVII; ++sh) {
				final double ee = AtomicShell.getEdgeEnergy(el, sh);
				if ((energy > (ee - ToSI.eV(5.0))) && (energy < (ee + ToSI.eV(20))))
					res = CaveatBase.append(res,
							"The transition at " + nf.format(FromSI.eV(energy)) + " eV is close to the " + el.toAbbrev()
									+ " " + AtomicShell.getIUPACName(sh) + " edge at " + nf.format(FromSI.eV(ee))
									+ " eV.");
			}
			if (energy < ToSI.eV(180.0))
				res = CaveatBase.append(res,
						"The x-ray photon energy is too low (" + nf.format(FromSI.eV(energy)) + " < 180.0 eV)");
			if (energy < (1.1 * ToSI.eV(getCutOff(el.getAtomicNumber()))))
				res = CaveatBase.append(res, "The x-ray photon energy is too low (" + nf.format(FromSI.eV(energy))
						+ " < 1.1 x " + nf.format(getCutOff(el.getAtomicNumber())) + " eV)");
			if ((el.getAtomicNumber() < 70) && (energy > AtomicShell.getEdgeEnergy(el, AtomicShell.MV))
					&& (energy < AtomicShell.getEdgeEnergy(el, AtomicShell.MIV)))
				res = CaveatBase.append(res, "The x-ray photon energy is between the MIV and MV edges.");
			if (energy < AtomicShell.getEdgeEnergy(el, AtomicShell.MV))
				res = CaveatBase.append(res, "The x-ray photon energy is below the MV edge.");
			return res;
		}
	};

	/**
	 * Heinrich86 - Uses the algorithm for calculating the MassAbsorptionCoefficient
	 * due to Heinrich and published in the IXCOM 11 proceedings.
	 */
	public static class Heinrich86MassAbsorptionCoefficient extends HeinrichBase {
		public Heinrich86MassAbsorptionCoefficient() {
			super(false);
		}
	}

	public static final MassAbsorptionCoefficient Heinrich86 = new Heinrich86MassAbsorptionCoefficient();

	/**
	 * Heinrich86 - Uses the algorithm for calculating the MassAbsorptionCoefficient
	 * due to Heinrich and published in the IXCOM 11 proceedings modified to behave
	 * like the DTSA implementation.
	 */

	public static class HeinrichDtsaMassAbsorptionCoefficient extends HeinrichBase {
		public HeinrichDtsaMassAbsorptionCoefficient() {
			super(true);
		}
	}

	public static final MassAbsorptionCoefficient HeinrichDtsa = new HeinrichDtsaMassAbsorptionCoefficient();

	public static class HeinrichAltMassAbsorptionCoefficient extends MassAbsorptionCoefficient {
		HeinrichAltMassAbsorptionCoefficient() {
			super("Heinrich", "Heinrich, IXCOM-11 proceedings");
		}

		@Override
		public boolean isAvailable(Element el, double energy) {
			energy = FromSI.eV(energy);
			final int z = el.getAtomicNumber();
			return (energy > 0.0) && (energy <= 1.0e5) && (z >= 3) && (z <= 95);
		}

		@Override
		public double compute(Element el, double energy) {
			energy = FromSI.eV(energy);
			// Ref: Heinrich's formula as implemented by Myklebust
			// translated into Java
			final double z = el.getAtomicNumber();
			/*
			 * This expression only works for x-ray energies below the K-edge and above the
			 * K-edge for Z < 50. Energies above the K-edge for elements Z > 49 are
			 * completely nuts.
			 */
			double nm = 0.0, cc = 0.0, az = 0.0, c;
			if (energy <= 10.0)
				return 1e6;
			if ((z < 3) || (z > 95))
				return 0.001;
			final double[] EnDat = new double[10];
			for (int i = AtomicShell.K; i <= AtomicShell.NI; ++i)
				EnDat[i] = FromSI.eV(AtomicShell.getEdgeEnergy(el, i));
			// formula derived in eV units
			double bias = 0;
			// Z1 = z - 1;
			if (energy > EnDat[0]) { /* energy is above the K edge. */
				if (z < 6) {
					cc = 0.001 * ((1.808599 * z) - 0.287536);
					az = (((-14.15422 * z) + 155.6055) * z) + 24.4545;
					bias = (18.2 * z) - 103;
					nm = (((-0.01273815 * z) + 0.02652873) * z) + 3.34745;
				} else {
					cc = 1.0E-5 * ((((525.3 + (133.257 * z)) - (7.5937 * z * z)) + (0.169357 * z * z * z))
							- (0.0013975 * z * z * z * z));
					az = ((((-0.152624 * z) + 6.52) * z) + 47) * z;
					nm = 3.112 - (0.0121 * z);
					if ((energy > EnDat[0]) && (z >= 50))
						az = ((((-0.015 * z) + 3.52) * z) + 47) * z;
					if ((energy > EnDat[0]) && (z >= 57))
						cc = 1.0E-6 * ((200.0 + (100.0 * z)) - (z * z));
				}
			} else if (energy > EnDat[3]) {
				/* energy is below K-edge & above L3-edge */
				c = 0.001 * (((-0.0924 + (0.141478 * z)) - (0.00524999 * z * z)) + (9.85296E-5 * z * z * z));
				c = (c - (9.07306E-10 * z * z * z * z)) + (3.19245E-12 * z * z * z * z * z);
				cc = c;
				az = ((((((-0.000116286 * z) + 0.01253775) * z) + 0.067429) * z) + 17.8096) * z;
				nm = (((-4.982E-5 * z) + 0.001889) * z) + 2.7575;
				if ((energy < EnDat[1]) && (energy > EnDat[2]))
					cc = c * 0.858;
				if (energy < EnDat[2])
					cc = c * ((0.8933 - (0.00829 * z)) + (6.38E-5 * z * z));
			} else if ((energy < EnDat[3]) && (energy > EnDat[4])) {
				nm = (((((4.4509E-6 * z) - 0.00108246) * z) + 0.084597) * z) + 0.5385;
				if (z < 30)
					c = (((((((0.072773258 * z) - 11.641145) * z) + 696.02789) * z) - 18517.159) * z) + 188975.7;
				else
					c = (((((((0.001497763 * z) - 0.40585911) * z) + 40.424792) * z) - 1736.63566) * z) + 30039;
				cc = 1.0E-7 * c;
				az = ((((((-0.00018641019 * z) + 0.0263199611) * z) - 0.822863477) * z) + 10.2575657) * z;
				if (z < 61)
					bias = ((((((-0.0001683474 * z) + 0.018972278) * z) - 0.536839169) * z) + 5.654) * z;
				else
					bias = ((((((0.0031779619 * z) - 0.699473097) * z) + 51.114164) * z) - 1232.4022) * z;
			} else if (energy >= EnDat[8]) {
				az = (4.62 - (0.04 * z)) * z;
				c = 1.0E-8 * ((((((-0.129086 * z) + 22.09365) * z) - 783.544) * z) + 7770.8);
				c = c * ((((((4.865E-6 * z) - 0.0006561) * z) + 0.0162) * z) + 1.406);
				cc = c * ((((-0.0001285 * z) + 0.01955) * z) + 0.584);
				bias = ((((0.000378 * z) - 0.052) * z) + 2.51) * EnDat[7];
				nm = 3 - (0.004 * z);
				if ((energy < EnDat[5]) && (energy >= EnDat[6]))
					cc = c * ((0.001366 * z) + 1.082);
				if ((energy < EnDat[6]) && (energy >= EnDat[7]))
					cc = 0.95 * c;
				if ((energy < EnDat[7]) && (energy >= EnDat[8]))
					cc = 0.8 * c * ((((0.0005083 * z) - 0.06) * z) + 2.0553);
			} else if (energy < EnDat[8]) {
				cc = 1.08E-7 * ((((((-0.0669827 * z) + 17.07073) * z) - 1465.3) * z) + 43156);
				az = ((((0.00539309 * z) - 0.61239) * z) + 19.64) * z;
				bias = (4.5 * z) - 113;
				nm = 0.3736 + (0.02401 * z);
			}
			double mu;
			if (energy > EnDat[9]) {
				mu = (cc * Math.exp(nm * Math.log(12397 / energy)) * z * z * z * z) / el.getAtomicWeight();
				mu = mu * (1 - Math.exp((bias - energy) / az));
			} else {
				mu = ((cc * Math.exp(nm * Math.log(12397 / energy)) * z * z * z * z) / el.getAtomicWeight())
						* (1 - Math.exp((bias - EnDat[9]) / az));
				mu = (1.02 * mu * (energy - 10)) / (EnDat[9] - 10);
			}
			/* if(energy > EnDat[0] && z >= 50) mu = 10.0; */
			// transmission fraction per meter of path length per unit density
			if (Double.isNaN(mu))
				throw new EPQFatalException(
						"DTSA MACs are NAN for " + el.toAbbrev() + " at " + FromSI.eV(energy) + " eV");
			return fromCmSqrPerGram(mu);
		}
	}

	public static final MassAbsorptionCoefficient HeinrichAlt = new HeinrichAltMassAbsorptionCoefficient();

	/**
	 * <p>
	 * Chantler2005 - Chandler2005 seems to be a very capable set of MAC computed
	 * from theory. They form a matched pair with the edge energies in
	 * EdgeEnergy.Chantler2005.
	 * </p>
	 * <p>
	 * Chantler, C.T., Olsen, K., Dragoset, R.A., Kishore, A.R., Kotochigova, S.A.,
	 * and Zucker, D.S. (2005), X-Ray Form Factor, Attenuation and Scattering Tables
	 * (version 2.1). [Online] Available: http://physics.nist.gov/ffast
	 * [16-March-2005]. National Institute of Standards and Technology,
	 * Gaithersburg, MD. Originally published as Chantler, C.T., J. Phys. Chem. Ref.
	 * Data 29(4), 597-1048 (2000); and Chantler, C.T., J. Phys. Chem. Ref. Data 24,
	 * 71-643 (1995).
	 * </p>
	 */
	public static class ChantlerMassAbsorptionCoefficient extends MassAbsorptionCoefficient {
		public ChantlerMassAbsorptionCoefficient() {
			super("NIST-Chantler 2005", "See http://physics.nist.gov/ffast");
		}

		private volatile double[][] mData = null;

		@Override
		public boolean isAvailable(Element el, double energy) {
			energy = FromSI.eV(energy);
			final int z = el.getAtomicNumber();
			return (z >= Element.elmH) && (z <= Element.elmU) && (energy > 0.0) && (energy <= 1.0e6);
		}

		@Override
		public double compute(Element el, double energy) {
			if (mData == null) {
				synchronized (this) {
					if (mData == null) {
						final CSVReader cr = new CSVReader.ResourceReader("FFastMAC.csv", false);
						final double[][] tmp = cr.getResource(MassAbsorptionCoefficient.class);
						final double[][] dataTmp = new double[((Element.elmU - Element.elmH) + 1) * 2][];
						for (int e = 0; e < dataTmp.length; ++e)
							for (int r = tmp.length - 1; r >= 0; --r) {
								final double x = tmp[r][e];
								if (dataTmp[e] == null)
									if (!(Double.isNaN(x) || (x == 0.0))) {
										dataTmp[e] = new double[r + 1];
										assert (((e % 2) == 0) || (dataTmp[e - 1].length == dataTmp[e].length));
									}
								if (dataTmp[e] != null) {
									assert (!Double.isNaN(x));
									dataTmp[e][r] = ((e % 2) == 0 ? ToSI.keV(x) : fromCmSqrPerGram(x));
								}
							}
						mData = dataTmp;
					}
				}
				assert mData != null;
			}
			{
				assert mData != null;
				final int z = el.getAtomicNumber();
				assert (z >= Element.elmH);
				assert (z <= Element.elmU);
				final int r = 2 * (z - 1);
				final double[] rE = mData[r];
				final double[] rM = mData[r + 1];
				final int c = Arrays.binarySearch(rE, energy);
				if (c < -1) {
					final int c1 = -(c + 1); // Between c1 and c1-1
					assert energy >= rE[c1 - 1] : Double.toString(FromSI.keV(energy)) + " not greater or equal to "
							+ Double.toString(FromSI.keV(rE[c1 - 1]));
					assert energy <= rE[c1] : Double.toString(FromSI.keV(energy)) + " not less than or equal to "
							+ Double.toString(FromSI.keV(rE[c1]));
					// Log-log interpolation
					return Math.exp(Math.log(rM[c1 - 1]) + (Math.log(rM[c1] / rM[c1 - 1])
							* (Math.log(energy / rE[c1 - 1]) / Math.log(rE[c1] / rE[c1 - 1]))));

				} else
					return c == -1 ? 0.0 : rM[c];
			}
		}

		public void write(File file) throws IOException {
			try (final FileOutputStream out = new FileOutputStream(file)) {
				try (final OutputStreamWriter osw = new OutputStreamWriter(out, "ASCII")) {
					compute(Element.H, ToSI.keV(1.0));
					int i = 0;
					for (final double[] data : mData) {
						final StringBuffer sb = new StringBuffer();
						for (final double datum : data) {
							if (sb.length() > 0)
								sb.append(",");
							sb.append(Double.toString((i % 2) == 0 ? FromSI.keV(datum) : toCmSqrPerGram(datum)));
						}
						sb.append("\n");
						osw.append(sb.toString());
						osw.flush();
						++i;
					}
				}
			}
		}
	}

	public static final MassAbsorptionCoefficient Chantler2005 = new ChantlerMassAbsorptionCoefficient();

	/**
	 * DTSA_CitZAF - The CitZAF MACs for Ka, La and Ma included as part of the DTSA
	 * application.
	 */
	public static class DTSACitzafMassAbsorptionCoefficient extends MassAbsorptionCoefficient {
		public DTSACitzafMassAbsorptionCoefficient() {
			super("DSTA CitZAF", "DTSA at http://www.cstl.nist.gov/div837/Division/outputs/DTSA/DTSA.htm");
		}

		private double[][] mKMac;

		private int mMinK, mMaxK;

		private double[][] mLMac;

		private int mMinL, mMaxL;

		private double[][] mMMac;

		private int mMinM, mMaxM;

		private final String[] mFilenames = { "CitMAC/MAC_K_Data.csv", "CitMAC/MAC_L_Data.csv",
				"CitMAC/MAC_M_Data.csv" };

		final private void load(int family) {
			final CSVReader cr = new CSVReader.ResourceReader(mFilenames[family - AtomicShell.KFamily], false);
			switch (family) {
			case AtomicShell.KFamily:
				mKMac = cr.getResource(MassAbsorptionCoefficient.class);
				mMinK = (int) Math.round(mKMac[0][0]);
				mMaxK = (int) Math.round(mKMac[0][mKMac[0].length - 1]);
				break;
			case AtomicShell.LFamily:
				mLMac = cr.getResource(MassAbsorptionCoefficient.class);
				mMinL = (int) Math.round(mLMac[0][0]);
				mMaxL = (int) Math.round(mLMac[0][mLMac[0].length - 1]);
				break;
			case AtomicShell.MFamily:
				mMMac = cr.getResource(MassAbsorptionCoefficient.class);
				mMinM = (int) Math.round(mMMac[0][0]);
				mMaxM = (int) Math.round(mMMac[0][mMMac[0].length - 1]);
				break;
			default:
				assert (false);
			}
		}

		@Override
		final public double compute(Element el, double energy) {
			throw new EPQFatalException("The " + toString()
					+ " implementation of the mass absorption coefficient does not support computing the MAC by energy.");
		}

		@Override
		final public double compute(Element el, XRayTransition xrt) throws EPQException {
			final int f = xrt.getFamily();
			final int z = xrt.getElement().getAtomicNumber();
			switch (f) {
			case AtomicShell.KFamily:
				if (mKMac == null)
					load(AtomicShell.KFamily);
				if ((z >= mMinK) && (z <= mMaxK))
					return fromCmSqrPerGram(mKMac[el.getAtomicNumber()][z - mMinK]);
				else
					throw new EPQException("MAC unavailable in " + toString() + " implementation.");
			case AtomicShell.LFamily:
				if (mLMac == null)
					load(AtomicShell.LFamily);
				if ((z >= mMinL) && (z <= mMaxL))
					return fromCmSqrPerGram(mLMac[el.getAtomicNumber()][z - mMinL]);
				else
					throw new EPQException("MAC unavailable in " + toString() + " implementation.");
			case AtomicShell.MFamily:
				if (mMMac == null)
					load(AtomicShell.MFamily);
				if ((z >= mMinM) && (z <= mMaxM))
					return fromCmSqrPerGram(mMMac[el.getAtomicNumber()][z - mMinM]);
				else
					throw new EPQException("MAC unavailable in " + toString() + " implementation.");
			default:
				throw new EPQException("MAC unavailable in " + toString() + " implementation.");
			}
		}
	}

	public static final MassAbsorptionCoefficient DTSA_CitZAF = new DTSACitzafMassAbsorptionCoefficient();

	/**
	 * Null - No absorption
	 */
	public static class NullMassAbsorptionCoefficient extends MassAbsorptionCoefficient {
		public NullMassAbsorptionCoefficient() {
			super("Null", "MAC(z,e)=0 for all z&e");
		}

		@Override
		public double compute(Element el, XRayTransition xrt) throws EPQException {
			return 1.0e-3;
		}

		@Override
		public boolean isAvailable(Element el, double energy) {
			return true;
		}

		@Override
		public double compute(Element el, double energy) {
			return 1.0e-3;
		}
	}

	public final static MassAbsorptionCoefficient Null = new NullMassAbsorptionCoefficient();

	static private class MACDatum implements Comparable<MACDatum> {
		private final Element mElement;
		private final double mEnergy;

		private MACDatum(Element elm, double ee) {
			mElement = elm;
			mEnergy = ee;
		}

		/**
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(MACDatum md) {
			final int res = mElement.compareTo(md.mElement);
			return res != 0 ? res : Double.compare(mEnergy, md.mEnergy);
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof MACDatum) {
				final MACDatum md = (MACDatum) o;
				return (md.mElement == mElement) && (md.mEnergy == mEnergy);
			} else
				return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			final long temp = mElement.hashCode();
			return (int) ((prime * temp) + (int) (temp ^ (temp >>> 32)));
		}

	}

	/**
	 * <p>
	 * A class that permits the user to override specific MAC values.
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
	static public class UserSpecifiedCoefficient extends MassAbsorptionCoefficient {

		final private MassAbsorptionCoefficient mBaseModel;
		private final TreeMap<MACDatum, Double> mCache = new TreeMap<MACDatum, Double>();

		/**
		 * Constructs a UserSpecifiedCoefficient which overrides the specified base MAC
		 * model with the values specified by put(...)
		 * 
		 * @param base
		 */
		public UserSpecifiedCoefficient(MassAbsorptionCoefficient base) {
			super(base.getName(), "User modified " + base.getReference());
			mBaseModel = base;
		}

		/**
		 * Specify the mac for the specified absorber element and x-ray energy.
		 * 
		 * @param elm
		 * @param energy
		 * @param mac
		 */
		public void put(Element elm, double energy, double mac) {
			mCache.put(new MACDatum(elm, energy), Double.valueOf(mac));
		}

		/**
		 * Specify the mac for the specified absorber element and x-ray transition.
		 * 
		 * @param elm
		 * @param xrt
		 * @param mac
		 * @throws EPQException
		 */
		public void put(Element elm, XRayTransition xrt, double mac) throws EPQException {
			mCache.put(new MACDatum(elm, Double.valueOf(xrt.getEnergy())), mac);
		}

		/**
		 * @see gov.nist.microanalysis.EPQLibrary.MassAbsorptionCoefficient#compute(gov.nist.microanalysis.EPQLibrary.Element,
		 *      double)
		 */
		@Override
		public double compute(Element el, double energy) {
			final MACDatum md = new MACDatum(el, energy);
			final Double v = mCache.get(md);
			return v != null ? v.doubleValue() : mBaseModel.compute(el, energy);
		}

		@Override
		public String getName() {
			return "User modified " + mBaseModel.getName();
		}
	}

	/**
	 * <p>
	 * Henke's 1993 tabulation of mass absorption coefficients from B. L. Henke, E.
	 * M. Gullikson, and J. C. Davis, Atomic Data and Nuclear Data Tables Vol. 54
	 * No. 2 (July 1993).
	 * </p>
	 * <p>
	 * Note the edge energies in this tabulation are inconsistent with the edge
	 * energies used elsewhere in EPQ. This can lead to unexpected and quite
	 * erroneous results when the &beta; or other line falls on the wrong size of an
	 * edge.
	 * </p>
	 * <p>
	 * I'd like to find a tabulation of the assumed edge energies and develop a
	 * system to ensure that the correct edge, transition energies are associated
	 * with the mass absorption coefficients. (Someday....)
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
	public static class Henke93MassAbsorptionCoefficient extends MassAbsorptionCoefficient {
		public Henke93MassAbsorptionCoefficient() {
			super("Henke (1993)", LitReference.Henke1993);
		}

		private final double[][] mEnergy = new double[(Element.elmU - Element.elmH) + 1][];
		private final double[][] mMuA = new double[(Element.elmU - Element.elmH) + 1][];

		private void loadElement(Element elm) {
			try {
				synchronized (this) {
					final InputStream is = MassAbsorptionCoefficient.class
							.getResourceAsStream("Henke93/" + elm.toAbbrev().toLowerCase() + ".nff");
					try (final BufferedReader br = new BufferedReader(new InputStreamReader(is, "US-ASCII"))) {
						final int maxE = 910; // Large enough...
						final double[] e = new double[maxE];
						final double[] f2 = new double[maxE];
						int nLines = 0;
						// Toss first comment line
						String line = br.readLine();
						for (line = br.readLine(); line != null; line = br.readLine()) {
							// Energy (eV), F1, F2
							final int p1 = line.indexOf("\t");
							final int p2 = line.indexOf("\t", p1 + 1);
							if ((p1 < 0) || (p2 < 0))
								throw new EPQFatalException(
										"Error reading the Henke 1993" + elm.toAbbrev() + " data file.");
							e[nLines] = Double.parseDouble(line.substring(0, p1).trim());
							assert (e[nLines] >= 10.0) && (e[nLines] <= 30000.0);
							f2[nLines] = Double.parseDouble(line.substring(p2 + 1).trim());
							++nLines;
						}
						final int zp = elm.getAtomicNumber() - 1;
						mEnergy[zp] = new double[nLines];
						mMuA[zp] = new double[nLines];
						// mu_a = 2*r_0*lambda*f_2 in (meter^2/atom)
						final double k = ((2.0 * PhysicalConstants.ClassicalElectronRadius)
								* PhysicalConstants.PlanckConstant * PhysicalConstants.SpeedOfLight)
								/ (PhysicalConstants.UnifiedAtomicMass * elm.getAtomicWeight());
						for (int i = 0; i < nLines; ++i) {
							final double ee = ToSI.eV(e[i]);
							mEnergy[zp][i] = ee;
							mMuA[zp][i] = (k / ee) * f2[i];
						}
					}
				}
			} catch (final Exception e) {
				throw new EPQFatalException(e);
			}
		}

		@Override
		public boolean isAvailable(Element el, double energy) {
			return (energy >= ToSI.eV(10.0)) && (energy <= ToSI.keV(30.0)) && (el.getAtomicNumber() >= Element.elmH)
					&& (el.getAtomicNumber() <= Element.elmU);
		}

		@Override
		public double compute(Element el, double energy) {
			final int zp = el.getAtomicNumber() - 1;
			if (mEnergy[zp] == null)
				loadElement(el);
			final double[] e = mEnergy[zp];
			final double[] muA = mMuA[zp];
			final int c = Arrays.binarySearch(e, energy);
			if (c <= -1) {
				final int c1 = -(c + 1); // Between c1 and c1-1
				if (c1 < muA.length)
					return Math.exp(Math.log(muA[c1 - 1]) + (Math.log(muA[c1] / muA[c1 - 1])
							* (Math.log(energy / e[c1 - 1]) / Math.log(e[c1] / e[c1 - 1]))));
				else
					return 0.0;
			} else
				return c == -1 ? 0.0 : muA[c];
		}
	}

	public final static MassAbsorptionCoefficient Henke1993 = new Henke93MassAbsorptionCoefficient();

	/**
	 * <p>
	 * A super set of Chantler where available and HeinrichDtsa otherwise.
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
	public static class SuperSetMassAbsorptionCoefficient extends MassAbsorptionCoefficient {
		public SuperSetMassAbsorptionCoefficient() {
			super("Superset - Chantler/DTSA", "Chantler then DTSA");
		}

		@Override
		public boolean isAvailable(Element el, double energy) {
			return Chantler2005.isAvailable(el, energy) || HeinrichDtsa.isAvailable(el, energy);
		}

		@Override
		public double compute(Element el, double energy) {
			return Chantler2005.isAvailable(el, energy) ? Chantler2005.compute(el, energy)
					: HeinrichDtsa.compute(el, energy);
		}
	}

	public final static MassAbsorptionCoefficient SuperSet = new SuperSetMassAbsorptionCoefficient();

	public static class SabbatucciMACs extends MassAbsorptionCoefficient {
		static private class AnElement {
			private double[] mEnergies;
			private double[] mMACs;

			private AnElement(List<Double> energies, List<Double> macs) {
				mEnergies = new double[energies.size()];
				mMACs = new double[macs.size()];
				assert mEnergies.length == mMACs.length;
				for (int i = 0; i < mEnergies.length; ++i) {
					mEnergies[i] = energies.get(i);
					mMACs[i] = macs.get(i);
				}
			}

			private double compute(double e) {
				final int i = Arrays.binarySearch(mEnergies, e);
				if (i >= 0)
					return mMACs[i];
				else {
					final int ip = Math.max(1, -(i + 1));
					assert ip == 1 || e > mEnergies[ip - 1];
					assert ip == 1 || e < mEnergies[ip];
					return mMACs[ip - 1] + //
							((e - mEnergies[ip - 1]) * (mMACs[ip] - mMACs[ip - 1]))
									/ (mEnergies[ip] - mEnergies[ip - 1]);
				}
			}
		}

		private final AnElement[] mData = new AnElement[99];

		public SabbatucciMACs() {
			super("Sabbatucci & Salvat MACs",
					new LitReference.JournalArticle("Theory and calculation of the atomic photoeffect",
							LitReference.RadPhys, "121", "122140", 2016,
							new Author[] { LitReference.LSabbatucci, LitReference.FSalvat }));
		}

		private AnElement loadElement(Element elm) {
			try {
				final InputStream is = MassAbsorptionCoefficient.class
						.getResourceAsStream("SabbatucciPIX/phxs[" + elm.getAtomicNumber() + "].dat");
				try (final BufferedReader br = new BufferedReader(new InputStreamReader(is, "US-ASCII"))) {
					ArrayList<Double> energies = new ArrayList<>();
					ArrayList<Double> macs = new ArrayList<>();
					int i = 0, nsh = -1, npts = -1;
					for (String line = br.readLine(); line != null; line = br.readLine()) {
						i += 1;
						if (line.startsWith("# ")) {
							if (i == 3) {
								nsh = Integer.parseInt(line.substring(1, 6).strip());
								// } else if(i >= 7 && i <= 6 + nsh) {
								// final int sh = Integer.parseInt(line.substring(2, 4).strip());
								// final double e = Double.parseDouble(line.substring(18,31));
								// ee[(z, sh)] = e
							} else if ((nsh != -1) && (i == nsh + 9)) {
								npts = Integer.parseInt(line.substring(2, 8).strip());
							}
						}
						if ((nsh != -1) && (i >= nsh + 13) && (i <= nsh + 12 + npts)) {
							final double s = Double.parseDouble(line.substring(2, 15));
							final double x = Double.parseDouble(line.substring(17, 30));
							if ((s >= 1.0) && (s <= 1.0e6)) { // keep 1 eV to 1 MeV
								energies.add(ToSI.eV(s));
								macs.add(fromCmSqrPerGram(x * 6.02214076e23 / elm.getAtomicWeight()));
							}
						} else if (i > nsh + 12 + npts)
							break;
					}
					return new AnElement(energies, macs);
				}
			} catch (final Exception e) {
				throw new EPQFatalException(e);
			}
		}

		@Override
		public double compute(Element el, double energy) {
			final int zi = el.getAtomicNumber() - 1;
			if (mData[zi] == null) {
				synchronized (this) {
					if (mData[zi] == null)
						mData[zi] = loadElement(el);
				}
			}
			return mData[zi].compute(energy);
		}

		@Override
		public boolean isAvailable(Element el, double energy) {
			return (energy >= ToSI.eV(1.0)) && (energy <= ToSI.keV(1000.0)) && (el.getAtomicNumber() >= Element.elmH)
					&& (el.getAtomicNumber() <= Element.elmEs);
		}

	}
	
	public static class SuperSetMACs extends MassAbsorptionCoefficient {

		private final MassAbsorptionCoefficient[] mAlgorithms;

		private static final String buildName(MassAbsorptionCoefficient[] algs) {
			StringBuffer sb = new StringBuffer();
			for(MassAbsorptionCoefficient mac : algs)
				sb.append(", "+mac.getName());
			return "SuperSet["+sb.toString().substring(2)+"]";
		}
		
		public SuperSetMACs(MassAbsorptionCoefficient[] algs) {
			super(buildName(algs),"See implementations");
			mAlgorithms = algs.clone();
		}

		@Override
		public double compute(Element el, double energy) {
			for(MassAbsorptionCoefficient mac : mAlgorithms)
				if(mac.isAvailable(el, energy))
					return mac.compute(el, energy);
			assert false;
			return 0.0;
		}

		@Override
		public boolean isAvailable(Element el, double energy) {
			for(MassAbsorptionCoefficient mac : mAlgorithms)
				if(mac.isAvailable(el, energy))
					return true;
			return false;
		}

	
	}

	public final static MassAbsorptionCoefficient Sabbatucci2016 = new SabbatucciMACs();
	public final static MassAbsorptionCoefficient SuperSet2 = new SuperSetMACs(new MassAbsorptionCoefficient[] {
		MassAbsorptionCoefficient.Chantler2005,
		MassAbsorptionCoefficient.Sabbatucci2016
	});

	
	public final static MassAbsorptionCoefficient Default = SuperSet2;
	

	/**
	 * Default - This specifies the default implementation used elsewhere in the
	 * application when a MAC is required. Currently the default is Ritchie2005a.
	 */
	static private final AlgorithmClass[] mAllImplementations = { MassAbsorptionCoefficient.BastinHeijligers89,
			MassAbsorptionCoefficient.Chantler2005, MassAbsorptionCoefficient.DTSA_CitZAF,
			MassAbsorptionCoefficient.Heinrich86, MassAbsorptionCoefficient.HeinrichAlt,
			MassAbsorptionCoefficient.HeinrichDtsa, MassAbsorptionCoefficient.Henke82, MassAbsorptionCoefficient.Null,
			MassAbsorptionCoefficient.Pouchou1991, MassAbsorptionCoefficient.PouchouPichoir88,
			MassAbsorptionCoefficient.Ruste79, MassAbsorptionCoefficient.Henke1993,
			MassAbsorptionCoefficient.Sabbatucci2016, MassAbsorptionCoefficient.SuperSet, MassAbsorptionCoefficient.SuperSet2 };

}
