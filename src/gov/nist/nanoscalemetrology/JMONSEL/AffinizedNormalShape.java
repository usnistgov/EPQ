/**
 * gov.nist.nanoscalemetrology.JMONSEL.AffinizedNormalShape Created by: John
 * Villarrubia, Bin Ming. Date: June 01, 2011
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.NISTMonte.AffinizedShape;
import gov.nist.microanalysis.Utility.Math2;

import Jama.Matrix;

/**
 * <p>
 * This class converts a base shape (the constructor input, which must be a
 * NormalShape) into a shape that conforms to the IAffineTransform interface.
 * That is, it provides the rotation, translation, scaling, etc. affine
 * transformations for any NormalShape provided as input. Geometrically, an
 * affine transformation preserves: 1. The points which lie on a line continue
 * to be collinear after the transformation. 2. Ratios of distances along a
 * line; i.e., for distinct collinear points p1,p2,p3, the ratio | p2 - p1 | / |
 * p3 - p2 | is preserved. Transformations are cumulative.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia, Bin Ming
 * @version 1.0
 */
public class AffinizedNormalShape extends AffinizedShape implements NormalShape {

	private final NormalShape baseShape;
	private double[] result = null;

	/**
	 * Constructs an AffinizedNormalShape. Initially, this shape is equivalent to
	 * the supplied baseShape. (It is constructed with its affine transformation
	 * matrix equal to the identity matrix.) It may be subsequently transformed
	 * using one or more of the provided affine transform methods.
	 *
	 * @param baseShape -
	 */
	public AffinizedNormalShape(NormalShape baseShape) {
		super(baseShape);
		this.baseShape = baseShape;
	}

	/**
	 * Private utility to transform a direction vector (e.g., surface normal).
	 * Direction vectors transform like cross products, hence differently from
	 * ordinary vectors.
	 *
	 * @param normalVector - the direction vector to be transformed.
	 * @return - the normalized transformed direction vector
	 */
	private double[] directionFromBaseCoordinate(double[] normalVector) {
		final Matrix pVector = new Matrix(new double[] { normalVector[0], normalVector[1], normalVector[2], }, 1);
		/* Multiply by the upper left 3x3 subset of the matrix */
		final Matrix t = pVector.times(inverseAffine.getMatrix(0, 2, 0, 2));
		return Math2.normalize(t.getRowPackedCopy());
	}

	@Override
	public double getFirstIntersection(double[] pos0, double[] pos1) {
		return (getFirstNormal(pos0, pos1))[3];
	}

	@Override
	public boolean contains(double[] pos0, double[] pos1) {
		return baseShape.contains(toBaseCoordinate(pos0), toBaseCoordinate(pos1));
	}

	@Override
	public double[] getFirstNormal(double[] pos0, double[] pos1) {
		// Determine first normal in base coordinate system.
		result = baseShape.getFirstNormal(toBaseCoordinate(pos0), toBaseCoordinate(pos1));
		// Convert the normal vector portion back to our coordinate system.
		final double[] normv = directionFromBaseCoordinate(result);
		// Repack the result array with this normal vector and return it.
		for (int i = 0; i < 3; i++)
			result[i] = normv[i];
		return result;
	}

	@Override
	public double[] getPreviousNormal() {
		return result;
	}

	@Override
	public String toString() {
		return "AffinizedNormalShape[" + baseShape + "]";
	}

}
