package gov.nist.microanalysis.EPQLibrary;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import gov.nist.microanalysis.EPQLibrary.AbsorptionCorrection.PhilibertHeinrichAbsorption;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * A base class for implementing matrix correction algorithms such as P&amp;P,
 * Proza and ZAF.
 * </p>
 * <p>
 * I use have assumed the convention that k = (C<sub>u</sub>&middot;Z
 * <sub>u</sub>&middot;A<sub> u</sub>&middot;F<sub>u</sub>)/(C<sub>s</sub>
 * &middot;Z<sub >s</sub>&middot;A<sub>s</sub>&middot;F<sub>s</sub>) where the
 * subscripts u and s refer to the unknown and the standard. C is the nominal
 * composition, Z, A and F are the atomic number, absorption and fluorescence
 * correction factors. &phi;(&rho;&middot;z) algorithms compute the Z&middot;A
 * term together. It is possible also to calculate the A term and thus
 * back-calculate the Z term.
 * <p>
 * The usage of the CorrectionAlgorithm involves first calling initialize(...)
 * followed by computeZAFCorrection(...) or generated(...).
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
abstract public class CorrectionAlgorithm extends AlgorithmClass implements Comparable<AlgorithmClass> {

	/**
	 * @author nicholas
	 */
	public interface IAbsorptionSensitivity {
		/**
		 * Computes the sensitivity coefficients for the quantitative correction due to
		 * uncertainty in the mass absorption coefficient.
		 * 
		 * @param std   The standard material
		 * @param unk   The unknown material
		 * @param xrt   The x-ray transition
		 * @param props The properties (must include beam energy)
		 * @return The computed k-ratio with uncertainty estimate
		 */
		public UncertainValue2 kratio(Composition std, Composition unk, XRayTransition xrt, SpectrumProperties props)
				throws EPQException;
	}

	// Input parameters
	/**
	 * The assumed composition of the sample
	 */
	protected Composition mComposition;
	/**
	 * The ionized shell
	 */
	protected AtomicShell mShell;
	/**
	 * The incident beam energy in Joules
	 */
	protected double mBeamEnergy;
	/**
	 * The angle at which the detector is located. Measured up from the x-y plane.
	 */
	protected double mTakeOffAngle;
	/**
	 * The angle at which the beam exits the sample. On a sample which is normal to
	 * the beam, the exit angle is the same as the take-off angle. When the sample
	 * is flat but tilted, the exit angle determines how much extra path length the
	 * beam must traverse to exit the sample.
	 */
	protected double mExitAngle;

	/**
	 * The properties at which the data is assumed to be collected.
	 */
	protected SpectrumProperties mProperties;

	public CorrectionAlgorithm(String name, String ref) {
		super("Matrix Correction", name, ref);
	}

	public CorrectionAlgorithm(String name, LitReference ref) {
		super("Matrix Correction", name, ref);
	}

	/**
	 * Returns true if the CorrectionAlgorithm supports the specified SampleShape
	 * derived class.
	 * 
	 * @param ss
	 * @return boolean
	 */
	public boolean supports(Class<? extends SampleShape> ss) {
		return ss == SampleShape.Bulk.class;
	}

	/**
	 * getAllImplementations - Returns a full list of all available algorithms. Each
	 * item is an implements the CorrectionAlgorithm class.
	 * 
	 * @return List
	 */
	@Override
	public List<AlgorithmClass> getAllImplementations() {
		return Arrays.asList(mAllImplementations);
	}

	@Override
	protected void initializeDefaultStrategy() {
		addDefaultAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Default);
		addDefaultAlgorithm(Fluorescence.class, Fluorescence.Reed);
	}

	/**
	 * initialize - Initialize the basic input parameters. Override this in derived
	 * classes to preform any precomputations based on the Material, AtomicShell,
	 * beam energy or take-off angle.
	 * 
	 * @param comp  The material's Composition (as best known)
	 * @param shell The AtomicShell for which to compute the correction
	 * @param props A SpectrumProperties object containing at least
	 *              SpectrumProperties.BeamEnergy and
	 *              SpectrumProperties.TakeOffAngle
	 * @return boolean Returns true if one or more of comp, shell or e0 have
	 *         changed.
	 */
	public boolean initialize(Composition comp, AtomicShell shell, SpectrumProperties props) throws EPQException {
		comp = comp.normalize();
		final double e0 = ToSI.keV(props.getNumericProperty(SpectrumProperties.BeamEnergy));
		final double exitAngle = SpectrumUtils.getExitAngle(props);
		final double takeOff = SpectrumUtils.getTakeOffAngle(props);
		final boolean res = (!comp.equals(mComposition)) || (!mShell.equals(shell) || (!mProperties.equals(props)));
		if (res) {
			mComposition = comp;
			mBeamEnergy = e0;
			mShell = shell;
			mExitAngle = exitAngle;
			mTakeOffAngle = takeOff;
			mProperties = props;
			if (!comp.containsElement(mShell.getElement()))
				throw new IllegalArgumentException(
						"The matrix material " + comp.descriptiveString(true) + " does not contain "
								+ mShell.getElement().toAbbrev() + " represented by " + shell.toString());
			if (shell.getEdgeEnergy() > e0)
				throw new IllegalArgumentException(
						"The beam energy is less than the shell excitation energy for" + shell.toString() + ".");
		}
		return res;
	}

	/**
	 * computeTilt - Computes the sample tilt relative to the electron beam axis. If
	 * the SampleSurfaceNormal property is available it is used otherwise, XTilt and
	 * YTilt properties are used. Returns 0.0 if none of these are available.
	 * 
	 * @param props
	 * @return double
	 */
	static public double getTilt(SpectrumProperties props) {
		// Default to surface normal pointing towards the source of the electrons
		return Math2.angleBetween(Math2.MINUS_Z_AXIS, SpectrumUtils.getSurfaceNormal(props));
	}

	/**
	 * Computes the ZAF correction relative to a pure element standard. This method
	 * is not very computationally efficient. Usually this is not a problem except
	 * with MCCorrectionAlgorithm.
	 * 
	 * @param comp  - The Composition of the material
	 * @param xrt   - The transition
	 * @param props - Spectrum properties
	 * @return A double[4] containing the z, a, f and zaf correction factors
	 *         respectively
	 * @throws EPQException
	 */
	public double[] relativeZAF(Composition comp, XRayTransition xrt, SpectrumProperties props) throws EPQException {
		return relativeZAF(comp, xrt, props, new Material(xrt.getElement(), ToSI.gPerCC(5.0)));
	}

	/**
	 * Computes the ZAF correction relative to a pure element standard. This method
	 * is not very computationally efficient. Usually this is not a problem except
	 * with MCCorrectionAlgorithm.
	 * 
	 * @param comp     - The Composition of the material
	 * @param xrt      - The transition
	 * @param props    - Spectrum properties
	 * @param standard - The standard's Composition
	 * @return A double[4] containing the z, a, f and zaf correction factors
	 *         respectively
	 * @throws EPQException
	 */
	public double[] relativeZAF(Composition comp, XRayTransition xrt, SpectrumProperties props, Composition standard)
			throws EPQException {
		if (comp.containsElement(xrt.getElement())) {
			final Fluorescence fl = (Fluorescence) getAlgorithm(Fluorescence.class);
			initialize(comp, xrt.getDestination(), props);
			final double zComp = generated(xrt);
			final double zaComp = computeZACorrection(xrt);
			assert !Double.isNaN(zaComp);
			final double fComp = (fl != null ? fl.compute(comp, xrt, mBeamEnergy, mExitAngle) : 1.0);
			final SampleShape ss = props.getSampleShapeWithDefault(SpectrumProperties.SampleShape, null);
			SpectrumProperties stdProps = props;
			if ((ss != null) && (!(ss instanceof SampleShape.Bulk))) {
				stdProps = props.clone();
				stdProps.setSampleShape(SpectrumProperties.SampleShape, new SampleShape.Bulk());
			}
			initialize(standard, xrt.getDestination(), stdProps);
			final double zStd = generated(xrt);
			final double zaStd = computeZACorrection(xrt);
			final double fStd = (fl != null ? fl.compute(standard, xrt, mBeamEnergy, mExitAngle) : 1.0);
			return new double[] { zComp / zStd, (zaComp * zStd) / (zaStd * zComp), fComp / fStd,
					(zaComp * fComp) / (zaStd * fStd) };
		} else
			throw new EPQException("The element " + xrt.getElement() + " is not present in " + comp + ".");
	}

	/**
	 * Computes the ZAF correction relative to a pure element standard. This method
	 * is not very computationally efficient. Usually this is not a problem except
	 * with MCCorrectionAlgorithm.
	 * 
	 * @param comp     - The Composition of the material
	 * @param xrts     - The transition set
	 * @param props    - Spectrum properties
	 * @param standard - The standard's Composition
	 * @return A double[4] containing the z, a, f and zaf correction factors
	 *         respectively
	 * @throws EPQException
	 */
	public double[] relativeZAF(Composition comp, XRayTransitionSet xrts, SpectrumProperties props,
			Composition standard) throws EPQException {
		double[] res = new double[4];
		double sum = 0.0;
		for (XRayTransition xrt : xrts) {
			final double weight = xrt.getWeight(XRayTransition.NormalizeFamily);
			sum += weight;
			Math2.addInPlace(res, Math2.multiply(weight, relativeZAF(comp, xrt, props, standard)));
		}
		return Math2.divide(res, sum);
	}

	/**
	 * Compute only the absorption term (A) of the ZAF correction. This method is
	 * not very computationally efficient. Usually this is not a problem except with
	 * MCCorrectionAlgorithm.
	 * 
	 * @param comp  - The Composition of the material
	 * @param xrt   - The transition
	 * @param props - Spectrum properties
	 * @return double
	 * @throws EPQException
	 */
	public double relativeA(Composition comp, XRayTransition xrt, SpectrumProperties props) throws EPQException {
		final Material mat = new Material(new Element[] { xrt.getElement() }, new double[] { 1.0 }, ToSI.gPerCC(5.0),
				xrt.getElement().toAbbrev());
		return relativeA(comp, xrt, props, mat);
	}

	/**
	 * Compute only the absorption term (A) of the ZAF correction. This method is
	 * not very computationally efficient. Usually this is not a problem except with
	 * MCCorrectionAlgorithm.
	 * 
	 * @param comp     - The Composition of the material
	 * @param xrt      - The transition
	 * @param props    - Spectrum properties
	 * @param standard - The material used as a standard (often pure element.)
	 * @return double
	 * @throws EPQException
	 */
	public double relativeA(Composition comp, XRayTransition xrt, SpectrumProperties props, Composition standard)
			throws EPQException {
		initialize(comp, xrt.getDestination(), props);
		final double res = computeZACorrection(xrt) / generated(xrt);
		initialize(standard, xrt.getDestination(), props);
		return res / (computeZACorrection(xrt) / generated(xrt));
	}

	/**
	 * Compute only the atomic number term (Z) of the ZAF correction. This method is
	 * not very computationally efficient. Usually this is not a problem except with
	 * MCCorrectionAlgorithm.
	 * 
	 * @param comp  - The Composition of the material
	 * @param xrt   - The XRayTransition
	 * @param props - Spectrum properties
	 * @return double
	 * @throws EPQException
	 */
	public double relativeZ(Composition comp, XRayTransition xrt, SpectrumProperties props) throws EPQException {
		return relativeZ(comp, xrt, props, new Material(xrt.getElement(), ToSI.gPerCC(5.0)));
	}

	/**
	 * Compute only the atomic number term (Z) of the ZAF correction. This method is
	 * not very computationally efficient. Usually this is not a problem except with
	 * MCCorrectionAlgorithm.
	 * 
	 * @param comp     - The Composition of the material
	 * @param xrt      - The XRayTransition
	 * @param props    - Spectrum properties
	 * @param standard - The material used as a standard (often pure element.)
	 * @return double
	 * @throws EPQException
	 */
	public double relativeZ(Composition comp, XRayTransition xrt, SpectrumProperties props, Composition standard)
			throws EPQException {
		initialize(comp, xrt.getDestination(), props);
		final double res = generated(xrt);
		initialize(standard, xrt.getDestination(), props);
		return res / generated(xrt);
	}

	/**
	 * <p>
	 * Computes the ZA or Phi(rho z) part of the ZAF correction. You must must call
	 * initialize(...) before calling computeZACorrection. The number returned by
	 * computeZACorrection is not necessarily meaningful in-and-of-itself. It may
	 * need to be ratioed with a standard to produce a correction factor.
	 * </p>
	 * 
	 * @param xrt
	 * @return double
	 */
	abstract public double computeZACorrection(XRayTransition xrt) throws EPQException;

	/**
	 * <p>
	 * Computes the ZAF correction such that
	 * </p>
	 * <code>C[unknown] = (I[unk]*computeZAFCorrection[unk])/(I[ref]*computeZAFCorrection[ref])</code>
	 * <p>
	 * You must must call initialize(...) before calling computeZAFCorrection.
	 * </p>
	 * 
	 * @param xrt XRayTransition
	 * @return double - The ZAF correction
	 * @throws EPQException
	 */
	public double computeZAFCorrection(XRayTransition xrt) throws EPQException {
		assert mComposition != null;
		final Fluorescence fl = (Fluorescence) getAlgorithm(Fluorescence.class);
		return (fl != null ? fl.compute(mComposition, xrt, mBeamEnergy, mExitAngle) : 1.0) * computeZACorrection(xrt);
	}

	/**
	 * Compute the full ZAF correction relative to a pure element times the
	 * concentration. This is the number you should expect to measure.
	 * 
	 * @param comp A composition
	 * @param xrt  X-ray transition
	 * @param sp   Spectrum properties
	 * @return double The ZAF
	 * @throws EPQException
	 */
	public double computeKRatio(Composition comp, XRayTransition xrt, SpectrumProperties sp) throws EPQException {
		final Composition elmMat = new Composition(new Element[] { xrt.getElement() }, new double[] { 1.0 });
		return computeKRatio(comp, elmMat, xrt, sp);
	}

	/**
	 * Compute the full ZAF correction relative to a standard material times the
	 * concentration. This is the number you should expect to measure.
	 * 
	 * @param unk A composition
	 * @param std A standard against which to compare
	 * @param xrt X-ray transition
	 * @param sp  Spectrum properties
	 * @return double The ZAF
	 * @throws EPQException
	 */
	public double computeKRatio(Composition unk, Composition std, XRayTransition xrt, SpectrumProperties sp)
			throws EPQException {
		initialize(unk, xrt.getDestination(), sp);
		final double zafUnk = computeZAFCorrection(xrt);
		SpectrumProperties stdSp = sp;
		// K-ratios for shapes are relative to bulk...
		if (sp.isDefined(SpectrumProperties.SampleShape)) {
			stdSp = new SpectrumProperties(sp);
			stdSp.removeAll(Arrays.asList(new SpectrumProperties.PropertyId[] { SpectrumProperties.SampleShape,
					SpectrumProperties.SpecimenDensity, SpectrumProperties.MassThickness }));
		}
		initialize(std, xrt.getDestination(), stdSp);
		final double zafStd = computeZAFCorrection(xrt);
		final Element elm = xrt.getElement();
		return (zafUnk * unk.weightFraction(elm, false)) / (zafStd * std.weightFraction(elm, false));
	}

	/**
	 * Compute the full ZAF correction relative to a standard material times the
	 * concentration. This is the number you should expect to measure.
	 * 
	 * @param comp A composition
	 * @param std  A standard against which to compare
	 * @param xrts X-ray transition
	 * @param sp   Spectrum properties
	 * @return double The ZAF
	 * @throws EPQException
	 */
	public double computeKRatio(Composition comp, Composition std, XRayTransitionSet xrts, SpectrumProperties sp)
			throws EPQException {
		final double[] zaf = relativeZAF(comp, xrts, sp, std);
		final Element elm = xrts.getElement();
		return (comp.weightFraction(elm, false) / std.weightFraction(elm, false)) * zaf[3];
	}

	/**
	 * Computes the quantity of x-rays generated - nominally per incident electron.
	 * Often emitted and generated share extensive similar calculations. It is best
	 * to implement these in initialize(...) and then call initialize(...) from both
	 * emitted and generated.
	 * 
	 * @param xrt
	 * @return double
	 */
	public double generated(XRayTransition xrt) {
		throw new EPQFatalException("CorrectionAlgorithm.generated(...) not implemented in " + getClass().getName());
	}

	/**
	 * caveat - Create a String describing the limitations of this algorithm for the
	 * specified composition, shell and spectrum properties.
	 * 
	 * @param comp
	 * @param shell
	 * @param props
	 * @return String
	 */
	abstract public String caveat(Composition comp, AtomicShell shell, SpectrumProperties props);

	/**
	 * chi - Computes the mass absorption coefficient times a factor to account for
	 * the non-normal take-off angle.
	 * 
	 * @param xrt XRayTransition - The specific x-ray transition
	 * @return double - in (m^2)/kg
	 * @throws EPQException - If the xrt is not a well known transition or take-off
	 *                      angle out of range.
	 */
	public double chi(XRayTransition xrt) throws EPQException {
		if (Math.abs(mExitAngle) > ((Math.PI * 89.99) / 180.0))
			throw new EPQException("The take-off angle is outside the range [-89.99\u00B0), 89.99\u00B0].");
		final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
		final double v = mac.compute(mComposition, xrt);
		return v / Math.sin(mExitAngle);
	}

	/**
	 * chi - Computes the the mass absorption coefficient and uncertainty times a
	 * factor to account for the non-normal take-off angle.
	 * 
	 * @param xrt XRayTransition - The specific x-ray transition
	 * @return double - in (m^2)/kg
	 * @throws EPQException - If the xrt is not a well known transition or take-off
	 *                      angle out of range.
	 */
	public UncertainValue2 chiU(XRayTransition xrt) throws EPQException {
		if (Math.abs(mExitAngle) > ((Math.PI * 89.99) / 180.0))
			throw new EPQException("The take-off angle is outside the range [-89.99\u00B0), 89.99\u00B0].");
		final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
		final UncertainValue2 v = mac.computeWithUncertaintyEstimate(mComposition, xrt);
		return UncertainValue2.divide(v, Math.sin(mExitAngle));
	}

	/**
	 * Implements a version of CorrectionAlgorithm for computing ZAF-style
	 * corrections. The user can use a Strategy to specify which
	 * AbsorptionCorrection, BackscatterFactor and StoppingPower to use.
	 */
	static public final ZAFCorrectionAlgorithm ZAFCorrection = new ZAFCorrectionAlgorithm();

	static public class ZAFCorrectionAlgorithm extends CorrectionAlgorithm {

		protected ZAFCorrectionAlgorithm(String name, LitReference ref) {
			super(name, ref);
		}

		/**
		 * Constructs a ZAFCorrectionAlgorithm
		 */
		public ZAFCorrectionAlgorithm() {
			super("ZAF-style", "None");
			// TODO Auto-generated constructor stub
		}

		private Composition mPureElement = null;
		private double mPureZ = Double.NaN;

		@Override
		protected void initializeDefaultStrategy() {
			super.initializeDefaultStrategy();
			addDefaultAlgorithm(AbsorptionCorrection.class, AbsorptionCorrection.PhilibertHeinrich);
			addDefaultAlgorithm(BackscatterFactor.class, BackscatterFactor.Duncumb1981);
			addDefaultAlgorithm(StoppingPower.class, StoppingPower.Thomas1963);
		}

		@Override
		public boolean initialize(Composition comp, AtomicShell shell, SpectrumProperties prop) throws EPQException {
			comp = comp.normalize();
			final boolean res = super.initialize(comp, shell, prop);
			if (res) {
				mPureElement = new Composition(new Element[] { shell.getElement() }, new double[] { 1.0 });
				final BackscatterFactor bs = (BackscatterFactor) getAlgorithm(BackscatterFactor.class);
				final StoppingPower sp = (StoppingPower) getAlgorithm(StoppingPower.class);
				mPureZ = bs.compute(mPureElement, mShell, mBeamEnergy)
						* sp.computeInv(mPureElement, mShell, mBeamEnergy);
			}
			return res;
		}

		@Override
		public double computeZACorrection(XRayTransition xrt) throws EPQException {
			final AbsorptionCorrection ac = (AbsorptionCorrection) getAlgorithm(AbsorptionCorrection.class);
			final BackscatterFactor bs = (BackscatterFactor) getAlgorithm(BackscatterFactor.class);
			final StoppingPower sp = (StoppingPower) getAlgorithm(StoppingPower.class);
			assert Math.abs(mTakeOffAngle - mExitAngle) < Math.toRadians(1.0)
					: "The take-off and exit angles should agree to within 1\u00B0";
			return (ac.compute(mComposition, xrt, mBeamEnergy, mExitAngle)
					* bs.compute(mComposition, xrt.getDestination(), mBeamEnergy)
					* sp.computeInv(mComposition, xrt.getDestination(), mBeamEnergy)) / mPureZ;
		}

		public double computeAbsorptionUncertainty(XRayTransition xrt) {
			final AbsorptionCorrection ac = (AbsorptionCorrection) getAlgorithm(AbsorptionCorrection.class);
			final BackscatterFactor bs = (BackscatterFactor) getAlgorithm(BackscatterFactor.class);
			final StoppingPower sp = (StoppingPower) getAlgorithm(StoppingPower.class);
			assert Math.abs(mTakeOffAngle - mExitAngle) < Math.toRadians(1.0)
					: "The take-off and exit angles should agree to within 1\u00B0";
			if (ac instanceof PhilibertHeinrichAbsorption) {
				final PhilibertHeinrichAbsorption ph = (PhilibertHeinrichAbsorption) ac;
				try {
					return (ph.uncertainty(mComposition, xrt, mBeamEnergy, mExitAngle)
							* bs.compute(mComposition, xrt.getDestination(), mBeamEnergy)
							* sp.computeInv(mComposition, xrt.getDestination(), mBeamEnergy)) / mPureZ;
				} catch (final EPQException e) {
					return 0.0;
				}
			} else
				return 0.0;
		}

		@Override
		public double generated(XRayTransition xrt) {
			final AbsorptionCorrection ac = (AbsorptionCorrection) getAlgorithm(AbsorptionCorrection.class);
			try {
				return ac.compute(mPureElement, xrt, mBeamEnergy, mExitAngle) * mPureZ;
			} catch (final EPQException e) {
				return 1.0;
			}
		}

		@Override
		public String caveat(Composition comp, AtomicShell shell, SpectrumProperties props) {
			return CaveatBase.None;
		}
	};

	/**
	 * A trivial correction algorithm in which the emitted and generated x-ray
	 * intensity are both unity ie. no correction.
	 */
	public static class NullCorrectionAlgorithm extends CorrectionAlgorithm {
		public NullCorrectionAlgorithm() {
			super("One-to-one (ZA=1.0)", "None");
		}

		@Override
		protected void initializeDefaultStrategy() {
			// Don't do anything...
		}

		@Override
		public String caveat(Composition comp, AtomicShell shell, SpectrumProperties props) {
			return "No correction performed on " + shell.toString() + " in " + comp.toString();
		}

		/**
		 * @see gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm#computeZACorrection(gov.nist.microanalysis.EPQLibrary.XRayTransition)
		 */
		@Override
		public double computeZACorrection(XRayTransition xrt) throws EPQException {
			assert (xrt.getDestination().equals(mShell));
			return 1.0;
		}

		/**
		 * generated - Computes the quantity of x-rays generated. Often emitted and
		 * generated share extensive similar calculations. It is best to implement these
		 * in initialize(...) and then call initialize(...) from both emitted and
		 * generated.
		 * 
		 * @return double
		 */
		@Override
		public double generated(XRayTransition xrt) {
			return 1.0;
		}
	}

	static public final CorrectionAlgorithm NullCorrection = new NullCorrectionAlgorithm();

	/**
	 * Outputs diagnostic information on the wrapped CorrectionAlgorithm to the
	 * specified Writer. This can be used to trace convergence of the iteration
	 * algorithm.
	 */
	static public class DiagnosticCorrectionAlgorithm extends CorrectionAlgorithm {
		private final CorrectionAlgorithm mBase;
		private final Writer mWriter;

		/**
		 * Constructs a DiagnosticCorrectionAlgorithm to output diagnostics as the
		 * specified base CorrectionAlgorithm is called.
		 * 
		 * @param base
		 * @param wr
		 */
		public DiagnosticCorrectionAlgorithm(CorrectionAlgorithm base, Writer wr) {
			super("Diagnostic[" + base.getName() + "]", base.getReference());
			mBase = base;
			mWriter = wr;
		}

		@Override
		public boolean initialize(Composition comp, AtomicShell shell, SpectrumProperties props) throws EPQException {
			final boolean res = mBase.initialize(comp, shell, props);
			try {
				mWriter.write("initialize(" + comp.descriptiveString(false) + ", " + shell.toString() + ", ...)\n");
				mWriter.flush();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return res;
		}

		@Override
		public double computeZACorrection(XRayTransition xrt) throws EPQException {
			final double res = mBase.computeZACorrection(xrt);
			try {
				mWriter.write("computeCorrection(" + xrt.toString() + ")\n");
				mWriter.write("Composition : \t" + mBase.mComposition + "\n");
				mWriter.write("Result:     : \t" + Double.toString(res) + "\n");
				mWriter.flush();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return res;
		}

		@Override
		public double generated(XRayTransition xrt) {
			final double res = mBase.generated(xrt);
			try {
				mWriter.write("generated()\n");
				mWriter.write("Composition : \t" + mBase.mComposition + "\n");
				mWriter.write("Result:     : \t" + Double.toString(res) + "\n");
				mWriter.flush();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			return res;
		}

		@Override
		public String caveat(Composition comp, AtomicShell shell, SpectrumProperties props) {
			return mBase.caveat(comp, shell, props);
		}

		@Override
		protected void initializeDefaultStrategy() {
			if (mBase != null)
				mBase.initializeDefaultStrategy();
		}
	}

	/**
	 * Converts from g/cm^2 to kg/m^2
	 * 
	 * @param f
	 * @return double
	 */
	static public final double toSI(double f) {
		final double k = ToSI.GRAM / (ToSI.CM * ToSI.CM);
		return f * k;
	}

	/**
	 * Converts from kg/m^2 to g/cm^2
	 * 
	 * @param f
	 * @return double
	 */
	static public final double fromSI(double f) {
		final double k = (ToSI.CM * ToSI.CM) / ToSI.GRAM;
		return f * k;
	}

	/**
	 * The standard Pouchou and Pichoir algorithm as implemented in the class
	 * PAP1991.
	 */
	static public final PAP1991 PouchouAndPichoir = new PAP1991();

	/**
	 * Pouchou &amp; Pichoir's XPP algorithm as implemented in the class XPP1991.
	 */
	static public final XPP1991 XPP = new XPP1991();
	/**
	 * Pouchou &amp; Pichoir's XPP algorithm extended for non-normal incident beams
	 * as implemented in the class XPP1989Ext.
	 */
	static public final XPP1989Ext XPPExtended = new XPP1989Ext();

	static public final Armstrong1982Correction Armstrong1982 = new Armstrong1982Correction();
	static public final Armstrong1982ParticleCorrection Armstrong1982Particle = new Armstrong1982ParticleCorrection();

	/**
	 * <p>
	 * An abstract class that extends CorrectionAlgorithm for implementing methods
	 * specific to phi-rho-z models of quantitiative x-ray microanalysis correction.
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
	abstract static public class PhiRhoZAlgorithm extends CorrectionAlgorithm {

		public PhiRhoZAlgorithm(String name, String ref) {
			super(name, ref);
		}

		public PhiRhoZAlgorithm(String name, LitReference ref) {
			super(name, ref);
		}

		/**
		 * Computes the height of the phi-rho-z at the specified z position. Initialize
		 * the Composition, AtomicShell and SpectrumProperties using
		 * <code>initialize(Composition comp, AtomicShell shell, SpectrumProperties props)</code>
		 * .
		 * 
		 * @param rhoZ in kg/m^2
		 * @return double
		 */
		abstract public double computeCurve(double rhoZ);

		/**
		 * Computes the &phi;(&rho;&middot;z) curve with absorption for the specified
		 * x-ray transition.
		 * 
		 * @param xrt  The XRayTransition
		 * @param rhoZ Depth in kg/m^2
		 * @return The relative intensity emitted from the specified depth
		 * @throws EPQException
		 */

		public double computeAbsorbedCurve(XRayTransition xrt, double rhoZ) throws EPQException {
			assert mShell.equals(xrt.getDestination());
			return computeCurve(rhoZ) * Math.exp(-chi(xrt) * rhoZ);
		}
	}

	static private final AlgorithmClass[] mAllImplementations = { NullCorrection, PouchouAndPichoir, XPP, XPPExtended,
			ZAFCorrection, Armstrong1982, Armstrong1982Particle };
}