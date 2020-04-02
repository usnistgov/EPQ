package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.ITransform;

/**
 * <p>
 * Implements the NormalShape interface for the difference of two NormalShapes.
 * The difference, A-B, is defined to be the region that is in A but not in B.
 * That is, it is equal to Intersection(A,Complement(B)).
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
public class NormalDifferenceShape implements NormalShape, ITransform {

	private final NormalShape shapeA;

	private final NormalShape shapeB;

	private double[] nv = null;

	/**
	 * Constructs a NormalDifferenceShape, shapeA - shapeB.
	 *
	 * @param shapeA - the base NormalShape. All points inside the difference are
	 *               inside shapeA.
	 * @param shapeB - the NormalShape to subtract from shapeA. All points inside
	 *               the difference are outside of shapeB.
	 */
	public NormalDifferenceShape(NormalShape shapeA, NormalShape shapeB) {
		this.shapeA = shapeA;
		this.shapeB = shapeB;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#contains(double[],
	 * double[])
	 */
	@Override
	public boolean contains(double[] pos0, double[] pos1) {
		return shapeA.contains(pos0, pos1) && !shapeB.contains(pos0, pos1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#getFirstNormal(double[],
	 * double[])
	 */
	@Override
	public double[] getFirstNormal(double[] pos0, double[] pos1) {
		/*
		 * This implementation is a simple direct implementation of the definition using
		 * the NormalIntersectionShape and NormalComplementShape functions. I'd
		 * originally planned to write a custom code for speed, but experience with the
		 * intersection routine suggests the savings is only ~20%.
		 */
		nv = (new NormalIntersectionShape(shapeA, new NormalComplementShape(shapeB))).getFirstNormal(pos0, pos1);
		return nv;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#contains(double[])
	 */
	@Override
	public boolean contains(double[] pos) {
		return shapeA.contains(pos) && !shapeB.contains(pos);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.microanalysis.NISTMonte.MonteCarloSS.Shape#getFirstIntersection
	 * (double[], double[])
	 */
	@Override
	public double getFirstIntersection(double[] pos0, double[] pos1) {
		// nv = getFirstNormal(pos0,pos1);
		// return nv[3];
		return (getFirstNormal(pos0, pos1))[3];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.microanalysis.EPQLibrary.ITransform#rotate(double[], double,
	 * double, double)
	 */
	@Override
	public void rotate(double[] pivot, double phi, double theta, double psi) {
		if (!(shapeA instanceof ITransform))
			throw new EPQFatalException(shapeA.toString() + " does not support transformation.");
		if (!(shapeB instanceof ITransform))
			throw new EPQFatalException(shapeB.toString() + " does not support transformation.");
		((ITransform) shapeA).rotate(pivot, phi, theta, psi);
		((ITransform) shapeB).rotate(pivot, phi, theta, psi);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nist.microanalysis.EPQLibrary.ITransform#translate(double[])
	 */
	@Override
	public void translate(double[] distance) {
		if (!(shapeA instanceof ITransform))
			throw new EPQFatalException(shapeA.toString() + " does not support transformation.");
		if (!(shapeB instanceof ITransform))
			throw new EPQFatalException(shapeB.toString() + " does not support transformation.");
		((ITransform) shapeA).translate(distance);
		((ITransform) shapeB).translate(distance);
	}

	@Override
	public double[] getPreviousNormal() {
		return nv;
	}

}
