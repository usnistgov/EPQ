package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.ITransform;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.Transform3D;

/**
 * <p>
 * Implements the NormalShape interface for a conical primitive. The
 * NormalConeShape represents a truncated cone bounded on its ends, like a
 * cylinder, by two circular end caps at right angles to the axis that joins
 * their centers. Unlike the cylinder, these end caps need not have the same
 * radius. Thus, the surface that joins them is conical rather than cylindrical.
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
public class NormalConeShape implements NormalShape, ITransform {

	private double[] c1; // Vector position of the 1st end cap center
	private double[] c2; // 2nd end cap center
	private double r1; // radius at end cap 0
	private double r2; // radius at end cap 1
	private double[] vL; // Vector from c to center of 2nd end cap
	private double deltar; // r2-r1
	private double[] unitL; // cache axis normalized to unit length

	private double magL2; // Square of axis length
	private double magL; // axis length
	private double deltarMagLRatio; // deltar/magL
	private double onePlusdeltarMagLRatioSq;
	private double rootOnePlusdeltarMagLRatioSq;
	private double[] pParallel;
	private double normalization2;

	private double[] nv = null; // Most recent normal vector

	/**
	 * Constructs a NormalConeShape. One end of the cone is a disk at end0 (a
	 * 3-element array giving the center coordinates). The other end is a disk at
	 * end1. The disks may have different positive radii.
	 *
	 * @param end0    - 3 coordinates of one end of the cylinder
	 * @param radius0 - radius of the cone at end0 in meters.
	 * @param end1    - 3 coordinates of the other end
	 * @param radius1 - radius of the cone at end1 in meters.
	 */
	public NormalConeShape(double[] end0, double radius0, double[] end1, double radius1) {
		// Save a local copy of parameters we need
		this.c1 = end0.clone();
		this.r1 = radius0;
		this.c2 = end1.clone();
		this.r2 = radius1;
		if (r1 < 0. || r2 < 0.)
			throw new EPQFatalException("NormalConeShape radii may not be negative.");
		precomputeValues(c1, r1, c2, r2);
	}

	@Override
	public boolean contains(double[] pos) {
		// Get coords of pos in cone frame
		double[] pminusc1 = Math2.minus(pos, c1);
		double z = Math2.dot(pminusc1, unitL); // axial coord of pos
		if (z < 0 || z > magL)
			return false;
		double rho2 = Math2.dot(pminusc1, pminusc1) - z * z; // perp. coord of pos

		double r = r1 + deltar * z / magL; // cone radius at z
		return rho2 <= r * r;
	}

	/**
	 * Note that this class does not yet implement the tie-breaking described by the
	 * NormalShape interface.
	 */
	@Override
	public boolean contains(double[] pos0, double[] pos1) {
		// For now just use Nicholas's standard method without tie break
		return contains(pos0);
	}

	/* Overrides getFirstIntersection to compute and cache the normal vector */
	@Override
	public double getFirstIntersection(double[] pos0, double[] pos1) {
		nv = getFirstNormal(pos0, pos1);
		return nv[3];
	}

	/**
	 * @see gov.nist.nanoscalemetrology.JMONSEL.NormalShape#getFirstNormal(double[],
	 *      double[])
	 */
	@Override
	public double[] getFirstNormal(double[] pos0, double[] pos1) {
		double u1; // distance to end cap 1
		double u2; // distance to end cap 2
		double uminus; // distance to intersection with curved surface, quad soln with minus sign
		double uplus; // distance to intersection with curved surface, quad soln with plus sign
		nv = new double[] { 0., 0., 0., Double.MAX_VALUE };

		final double[] p0c1 = { pos0[0] - c1[0], pos0[1] - c1[1], pos0[2] - c1[2] }; // pos0 - c1
		final double[] delta = { pos1[0] - pos0[0], pos1[1] - pos0[1], pos1[2] - pos0[2] }; // pos1 - pos0
		final double deltadotUnitL = Math2.dot(delta, unitL);
		double p0c1dotUnitL = Math2.dot(p0c1, unitL);
		double p0c1dotDelta = Math2.dot(p0c1, delta);
		double p0c1sq = Math2.dot(p0c1, p0c1);
		double delta2 = Math2.dot(delta, delta);

		// Coefficients of our quadratic a u^2 + b u + c == 0
		double p0c1dotUnitLsq = p0c1dotUnitL * p0c1dotUnitL;
		double c = -p0c1dotUnitLsq * onePlusdeltarMagLRatioSq - r1 * r1 - 2. * p0c1dotUnitL * r1 * deltarMagLRatio
				+ p0c1sq;
		double b = 2.
				* (p0c1dotDelta - deltadotUnitL * (p0c1dotUnitL * onePlusdeltarMagLRatioSq + r1 * deltarMagLRatio));
		double a = delta2 - deltadotUnitL * deltadotUnitL * onePlusdeltarMagLRatioSq;

		double b2 = b * b;
		double ac4 = 4. * a * c;
		double zminus = 0.;
		double zplus = 0.;

		double[] roots = null;

		/*
		 * Check first for intersections with the conical wall because we must check
		 * them eventually anyway, but if there's no intersection with them we can
		 * usually avoid having to check the end caps.
		 */
		roots = stableQuadRoots(a, b, c);

		if (roots == null) { // happens if discriminant < 0
			/*
			 * This happens if there is a clean miss in which even the extension of the
			 * electron's path to +/- infinity does not intersect the cone. However, it is
			 * also possible that round-off error will give us a discriminant < 0. when in
			 * reality it ought to be 0. or very tiny. There are two rare circumstances in
			 * which discriminant == 0. wherein we should nevertheless check for end cap
			 * intersections. These are when r1 == r2 and the trajectory is parallel to the
			 * axis or when the trajectory goes through the apex of the cone. We will rule
			 * out a falsely negative discriminant when even (1.+thresh)*b*b-4.*a*c remains
			 * negative, despite the boost by the small positive fractional thresh.
			 * 
			 */

			double discTest = (1. + 1.e-8) * b2 - ac4;
			if (r1 != r2 && discTest < 0.) {
				/*
				 * In this case there can be no end cap intersections either, so we're finished.
				 */
				return nv;
			} else {
				/*
				 * In this case we can't return because there may be end cap intersections. We
				 * record "infinity" for distance to quadratic intersections before proceeding.
				 */
				uminus = uplus = Double.MAX_VALUE;
			}
		} else { // discriminant >= 0.
			uminus = roots[0];
			if (0 < uminus && uminus <= 1) {
				zminus = p0c1dotUnitL + uminus * deltadotUnitL;
				if (zminus < 0 || zminus > magL)
					uminus = Double.MAX_VALUE;
			}
			uplus = roots[1];
			if (0 < uplus && uplus <= 1) {
				zplus = p0c1dotUnitL + uplus * deltadotUnitL;
				if (zplus < 0 || zplus > magL)
					uplus = Double.MAX_VALUE;
			}
		}

		/* Compute end cap solutions */

		if (p0c1dotUnitL == 0.) { // end cap 1
			u1 = 0;
		} else {
			if (deltadotUnitL == 0.) {
				u1 = Double.MAX_VALUE;
			} else {
				u1 = -p0c1dotUnitL / deltadotUnitL;
				if (p0c1sq + 2. * u1 * p0c1dotDelta + u1 * u1 * delta2 >= r1 * r1)
					u1 = Double.MAX_VALUE;
			}
		}

		double[] p0c2 = Math2.minus(pos0, c2);
		double p0c2dotUnitL = Math2.dot(p0c2, unitL);
		if (p0c2dotUnitL == 0.) { // end cap 2
			u2 = 0;
		} else {
			if (deltadotUnitL == 0.) {
				u2 = Double.MAX_VALUE;
			} else {
				u2 = -p0c2dotUnitL / deltadotUnitL;
				if (Math2.dot(p0c2, p0c2) + 2. * u2 * Math2.dot(p0c2, delta) + u2 * u2 * delta2 >= r2 * r2)
					u2 = Double.MAX_VALUE;
			}
		}

		double[] uList = new double[] { u1, u2, uminus, uplus };

		int index = -1;
		double minDist = Double.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			if (uList[i] > 0 && uList[i] <= 1 && uList[i] < minDist) {
				minDist = uList[i];
				index = i;
			}
		}

		if (index == -1) { // There were no intersections
			return nv;
		}

		// Construct the normal vector for the chosen index
		switch (index) {
		case 0:
			nv = new double[] { -unitL[0], -unitL[1], -unitL[2], minDist };
			break;
		case 1:
			nv = new double[] { unitL[0], unitL[1], unitL[2], minDist };
			break;
		case 2:
			double[] pPerp = Math2.divide(
					Math2.minus(Math2.plus(p0c1, Math2.multiply(uminus, delta)), Math2.multiply(zminus, unitL)),
					r1 + deltarMagLRatio * zminus);
			pPerp = Math2.multiply(normalization2, pPerp);
			nv = Math2.add(pParallel, pPerp);
			nv = new double[] { nv[0], nv[1], nv[2], minDist };
			break;
		case 3:
			double[] pPerp2 = Math2.divide(
					Math2.minus(Math2.plus(p0c1, Math2.multiply(uplus, delta)), Math2.multiply(zplus, unitL)),
					r1 + deltarMagLRatio * zplus);
			pPerp2 = Math2.multiply(normalization2, pPerp2);
			nv = Math2.add(pParallel, pPerp2);
			nv = new double[] { nv[0], nv[1], nv[2], minDist };
			break;
		}

		return nv;
	}

	@Override
	public double[] getPreviousNormal() {
		return nv;
	}

	/**
	 * Precomputes and caches quantities that depend only upon class parameter
	 * values. This should be done by the constructor and then again if any of the
	 * argument values changes, e.g., due to translation or rotation.
	 * 
	 * @param c1 - double[], Coordinates of face #1
	 * @param r1 - double, Radius of face #1
	 * @param c2 - double[], Coordinates of face #2
	 * @param r2 - double, Radius of face #2
	 */
	private void precomputeValues(double[] c1, double r1, double[] c2, double r2) {
		vL = Math2.minus(c2, c1);
		magL2 = Math2.dot(vL, vL);
		magL = Math.sqrt(magL2);
		unitL = Math2.divide(vL, magL);
		deltar = r2 - r1;
		deltarMagLRatio = deltar / magL;
		onePlusdeltarMagLRatioSq = 1. + deltarMagLRatio * deltarMagLRatio;
		rootOnePlusdeltarMagLRatioSq = Math.sqrt(onePlusdeltarMagLRatioSq);
		pParallel = Math2.multiply(-deltarMagLRatio / rootOnePlusdeltarMagLRatioSq, unitL);
		normalization2 = 1. / rootOnePlusdeltarMagLRatioSq;
	}

	// See ITransform for JavaDoc
	@Override
	public void rotate(double[] pivot, double phi, double theta, double psi) {
		c1 = Transform3D.rotate(c1, pivot, phi, theta, psi);
		c2 = Transform3D.rotate(c2, pivot, phi, theta, psi);
		precomputeValues(c1, r1, c2, r2);
	}

	/**
	 * Solves a * x^2 +b * x +c = 0 to find real roots given the coefficients a, b,
	 * and c. It picks the more algorithm that is more numerically stable against
	 * small a*c based on the value of the b parameter. It returns a double[] with
	 * two entries, the first for the root with the minus sign in the traditional
	 * formula (-b +/- sqrt(discriminant))/2/a and the second with the plus sign. If
	 * the discriminant < 0 there are no real roots, and it returns null.
	 * </p>
	 * <p>
	 * The algorithm used here should be stable against a or c equal to or close to
	 * 0. If c == 0 it correctly returns a root at 0 and another at -b/a. If a == 0
	 * it returns a root at -c/b and another at Double.MAX_VALUE. Note that in the
	 * limit a->0 the root's magnitude goes to infinity but the sign depends on the
	 * direction of approach to 0 by a, so that strictly speaking the root is
	 * indeterminate. Within this class, however, either sign indicates a lack of
	 * intersection in the interval from pos0 and pos1, so the Double.MAX_VALUE
	 * output works for our purpose.
	 * 
	 * @param a - double, the coefficient of u^2
	 * @param b - double, the coefficient of u
	 * @param c - double, the constant (coefficient of u^0)
	 * @return
	 */
	private double[] stableQuadRoots(double a, double b, double c) {
		double uminus = 0.;
		double uplus = 0.;

		if (a == 0.) {
			uminus = -c / b;
			uplus = Double.MAX_VALUE;
		} else {
			double disc = b * b - 4. * a * c;
			if (disc < 0.)
				return null;

			if (b < 0) {
				double denom = (-b + Math.sqrt(disc)) / 2.;
				uminus = c / denom;
				uplus = denom / a;
			} else {
				double denom = (-b - Math.sqrt(disc)) / 2.;
				uplus = c / denom;
				uminus = denom / a;
			}
		}
		return new double[] { uminus, uplus };

	}

	// See ITransform for JavaDoc
	@Override
	public void translate(double[] distance) {
		c1[0] += distance[0];
		c1[1] += distance[1];
		c1[2] += distance[2];
		c2[0] += distance[0];
		c2[1] += distance[1];
		c2[2] += distance[2];
		precomputeValues(c1, r1, c2, r2);
	}
}
