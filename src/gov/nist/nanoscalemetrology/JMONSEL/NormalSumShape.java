/**
 *
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.util.List;

import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.SumShape;

/**
 * <p>
 * Extends the NISTMonte SumShape class to implement the NormalShape interface.
 * It is similar in operation to NormalUnionShape, but like SumShape (and unlike
 * NormalUnionShape), NormalSumShape accepts an array of shapes. However, it
 * uses a slower algorithm than NormalUnionShape. NormalUnionShape is therefore
 * preferred when the union of only two shapes is desired. (NormalUnionShape can
 * be used for multiple shapes by nesting them. It is not currently known
 * whether it preserves its speed advantage in this case.)
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia, based on original code by Nicholas Ritchie
 * @version 1.0
 */

/*
 * Some additional notes: On 8/17 I compared this algorithm to NormalUnionShape
 * for the union (or sum) of a sphere and a half space. In thousands of randomly
 * chosen pos0 and pos1 points there was no significant difference in results
 * between the two. NormalUnionShape was 22% faster. However, sumshape may
 * potentially be faster for sums of more than two shapes because it can hold
 * multiple ones in an array, whereas to do this kind of problem UnionShape must
 * be nested.
 */
public class NormalSumShape extends SumShape implements NormalShape {

	private double[] nvsav = null;

	/**
	 * NormalSumShape - Create a sum shape that represents the sum of two shapes.
	 *
	 * @param a - (NormalShape) One of the shapes to be joined.
	 * @param b - (NormalShape) The other of the shapes to be joined.
	 */
	public NormalSumShape(NormalShape a, NormalShape b) {
		super(a, b);
	}

	/**
	 * @param shapes - NormalShape[], an array containing the shapes to be summed.
	 */
	public NormalSumShape(NormalShape[] shapes) {
		super(shapes);
	}

	/**
	 * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#contains(double[],
	 *      double[])
	 */
	@Override
	public boolean contains(double[] pos0, double[] pos1) {
		final List<MonteCarloSS.Shape> mShapes = getShapes();
		for (final MonteCarloSS.Shape shape : mShapes)
			if (((NormalShape) shape).contains(pos0, pos1))
				return true;
		return false;
	}

	@Override
	public double getFirstIntersection(double[] pos0, double[] pos1) {
		return (getFirstNormal(pos0, pos1))[3];
	}

	/**
	 * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#getFirstNormal(double[],
	 *      double[])
	 */
	@Override
	public double[] getFirstNormal(double[] pos0, double[] pos1) {
		double u;
		nvsav = new double[] { 0., 0., 0., Double.MAX_VALUE }; // To store normal
																// vector
		final double EXTRAU = 1.e-10;

		final List<MonteCarloSS.Shape> mShapes = getShapes();
		if (contains(pos0, pos1)) {
			u = 0.0;
			// Starting inside...
			final double[] start = pos0.clone();
			do {
				if (u > 0.0) {
					u += EXTRAU;
					// Compute the new start position...
					start[0] = pos0[0] + ((pos1[0] - pos0[0]) * u);
					start[1] = pos0[1] + ((pos1[1] - pos0[1]) * u);
					start[2] = pos0[2] + ((pos1[2] - pos0[2]) * u);
				}
				double uInc = 0.0;
				for (final MonteCarloSS.Shape shape : mShapes)
					if (((NormalShape) shape).contains(start, pos1)) {
						final double[] nv = ((NormalShape) shape).getFirstNormal(start, pos1);
						// if((nv[3] > uInc) && (nv[3] != Double.MAX_VALUE)) {
						// I'm not sure about the second term. What if one of
						// our
						// shapes is semi-infinite, like the substrate?
						if (nv[3] > uInc) {
							// Take the largest possible step...
							uInc = nv[3];
							nvsav = nv;
						}
					}
				if (uInc == 0.)
					break;
				u += (1 - u) * uInc;
				// Repeat until we can take a full step or
				// the step can't be enlarged...
			} while ((u > 0) && (u <= 1.0)); // If u==1 we still must check
			// whether u>1 is possible
		} else {
			u = Double.MAX_VALUE;
			// Starting outside so get the shortest distance to a boundary
			for (final MonteCarloSS.Shape shape : mShapes) {
				final double[] nv = ((NormalShape) shape).getFirstNormal(pos0, pos1);
				if ((nv[3] > 0.0) && (nv[3] < u)) {
					u = nv[3];
					nvsav = nv;
				}
			}
		}
		nvsav[3] = u;
		return nvsav;
	}

	@Override
	public double[] getPreviousNormal() {
		return nvsav;
	}

}
