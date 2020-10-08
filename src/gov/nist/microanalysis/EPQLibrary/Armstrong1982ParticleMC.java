package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape;
import gov.nist.microanalysis.Utility.MCIntegrator;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * A class for implementing John Armstrong's particle ZAF/&phi;(&rho;z)
 * algorithm for generic shapes.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Mark Sailey
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */
public class Armstrong1982ParticleMC extends Armstrong1982Base {

	private SampleShape mShape;
	private Shape mMCShape;
	/**
	 * In SI
	 */
	private double[] mPt0, mPt1;
	/**
	 * In SI
	 */
	private double[] mDetectorPt;
	/**
	 * In SI
	 */
	private double mRho;

	/**
	 * Constructs a Armstrong1982ParticleMC object for computing &phi;(&rho; z)
	 * corrections for a shape defined using the MonteCarloSS.Shape interface.
	 * <code>pt1</code> and <code>pt2</code> define a bounding box on the shape.
	 */
	public Armstrong1982ParticleMC() {
		super("Armstrong 1982 - MC");
	}

	@Override
	public boolean initialize(Composition comp, AtomicShell shell, SpectrumProperties props) throws EPQException {
		comp = comp.normalize();
		final boolean res = super.initialize(comp, shell, props);
		if (res) {
			mDetectorPt = SpectrumUtils.getDetectorPosition(props);
			mRho = ((Material) comp).getDensity();
			mShape = props.getSampleShapeWithDefault(SpectrumProperties.SampleShape, new SampleShape.Bulk());
			final double[] da = SpectrumUtils.getDetectorAxis(props);
			mMCShape = mShape.getShape();
			assert (mMCShape instanceof ITransform);
			final ITransform tr = (ITransform) mMCShape;
			// Rotate the face the detector
			tr.rotate(Math2.ORIGIN_3D, Math.atan2(da[1], da[0]), 0.0, 0.0);
			// Translate to the correct sample position
			tr.translate(SpectrumUtils.getSamplePosition(props));
			assert mMCShape
					.contains(Math2.plus(Math2.multiply(1.0e-9, Math2.Z_AXIS), SpectrumUtils.getSamplePosition(props)));
			assert !mMCShape.contains(
					Math2.plus(Math2.multiply(-1.0e-9, Math2.Z_AXIS), SpectrumUtils.getSamplePosition(props)));
			final double[] bounds = mShape.getBoundingBox();
			final double rr = (1.5 * ElectronRange.KanayaAndOkayama1972.compute(comp, mBeamEnergy)) / mRho;
			if ((bounds[5] - bounds[2]) > rr)
				bounds[5] = bounds[2] + rr;
			final double[] samplePos = SpectrumUtils.getSamplePosition(props);
			mPt0 = Math2.plus(samplePos, Math2.slice(bounds, 0, 3));
			mPt1 = Math2.plus(samplePos, Math2.slice(bounds, 3, 3));
		}
		return res;
	}

	public double particleAbsorptionCorrection(XRayTransition xrt) throws EPQException {
		final int N_ITER = 1000000;
		final MassAbsorptionCoefficient mac = (MassAbsorptionCoefficient) getAlgorithm(MassAbsorptionCoefficient.class);
		// muArho in CGS
		final double muArho = MassAbsorptionCoefficient.toCmSqrPerGram(mac.compute(mComposition, xrt))
				* FromSI.gPerCC(mRho);
		assert muArho > 0.0;
		assert !Double.isNaN(muArho);

		final MCIntegrator integrator = new MCIntegrator(mPt0, mPt1) {

			private double h(double[] args) {
				return 1.0;
			}

			@Override
			public double[] function(double[] args) {
				assert inside(args);
				final double f = mMCShape.getFirstIntersection(args, mDetectorPt);
				// g in cm
				final double g = FromSI.cm(f * Math2.distance(args, mDetectorPt));
				// rhoXyz in kg/m^2
				final double[] rhoXyz = Math2.multiply(mRho, Math2.minus(args, Math2.multiply(mPt0[2], Math2.Z_AXIS)));
				final double gen = computeCurve(rhoXyz[2]);
				return new double[] { gen * Math.exp(-muArho * g) * h(rhoXyz), gen };
			}

			@Override
			public boolean inside(double[] args) {
				return mMCShape.contains(args);
			}
		};
		final double[] res = integrator.compute(N_ITER);
		return res[0] / res[1];
	}
}
