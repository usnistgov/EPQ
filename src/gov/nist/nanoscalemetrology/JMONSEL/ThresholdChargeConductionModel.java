/**
 * gov.nist.nanoscalemetrology.JMONSEL.ThresholdChargeConductionModel Created
 * by: jvillar Date: Jul 22, 2011
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import gov.nist.microanalysis.NISTMonte.IMaterialScatterModel;
import gov.nist.microanalysis.NISTMonte.MeshElementRegion;
import gov.nist.microanalysis.NISTMonte.MeshedRegion;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.nanoscalemetrology.JMONSEL.Mesh.Tetrahedron;

/**
 * <p>
 * A conduction model in which scattering electrons that reach the end of their
 * trajectories do not necessarily contribute charge to the region in which they
 * stop. Rather, if the electric field in that region exceeds a specified
 * threshold, the electron flows along the field lines until it reaches a region
 * where the field is lower than the threshold.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author John Villarrubia
 * @version 1.0
 */
public class ThresholdChargeConductionModel implements IConductionModel {

	private double frac;

	/**
	 * Constructs a ThresholdChargeConductionModel
	 *
	 * @param frac
	 */
	public ThresholdChargeConductionModel(double frac) {
		this.frac = frac;
	}

	/**
	 * Gets the current value assigned to frac
	 *
	 * @return Returns the frac.
	 */
	public double getFrac() {
		return frac;
	}

	/**
	 * Sets the value assigned to frac.
	 *
	 * @param frac The value to which to set frac.
	 */
	public void setFrac(double frac) {
		if (this.frac != frac)
			this.frac = frac;
	}

	/**
	 * This model does not flow charges.
	 *
	 * @param thisMeshedRegion
	 * @param deltat
	 * @param feaRunner
	 * @return boolean false always
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IConductionModel#conductCharges(gov.nist.microanalysis.NISTMonte.MeshedRegion,
	 *      double, gov.nist.nanoscalemetrology.JMONSEL.IFEArunner)
	 */
	@Override
	public boolean conductCharges(MeshedRegion thisMeshedRegion, double deltat, IFEArunner feaRunner) {
		return false;
	}

	/**
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IConductionModel#stopRegion(gov.nist.microanalysis.NISTMonte.MeshElementRegion)
	 */
	@SuppressWarnings("unused")
	@Override
	public MeshElementRegion stopRegion(MeshElementRegion startRegion) {
		final Mesh mesh = startRegion.getMesh();
		MeshElementRegion finalRegion = oneStep(startRegion, mesh);
		int count = 0; // debug
		final double[] x0 = ((Tetrahedron) (startRegion.getShape())).getCenter();// debug
		final int id0 = ((Tetrahedron) (startRegion.getShape())).getIndex();// debug
		double[] x = new double[3];// debug
		double v = 0.;
		int id = 0;// debug
		while (finalRegion != startRegion) {
			startRegion = finalRegion;
			finalRegion = oneStep(startRegion, mesh);
			count++;// debug
			x = ((Tetrahedron) (finalRegion.getShape())).getCenter();// debug
			v = ((Tetrahedron) (finalRegion.getShape())).getPotential(x); // debug
			id = ((Tetrahedron) (finalRegion.getShape())).getIndex();// debug
			if (count > 100) {
				final int dummy;// debug
				dummy = 0;
			}
		}
		return finalRegion;
	}

	/*
	 * oneStep() algorithm summary: The corrected electric field in this region is
	 * the field with any components in forbidden directions (e.g., into neighboring
	 * vacuum) subtracted. Find the corrected electric field in this region and
	 * compare it to the threshold for this region. If it is less, then the electron
	 * doesn't move. (We return the start region.) If it is more, we flow the
	 * electron across one of the startRegion's faces. (We return the region on the
	 * other side of the face.) Which face? We find the faces with negative
	 * currents, meaning electrons flow out through them. The current through a face
	 * is proportional to the dot product of the corrected E field and its area *
	 * outward normal vector. If there's only one of these, that's the one. If there
	 * are more than one, we choose one with probability proportional to the
	 * current.
	 */
	private MeshElementRegion oneStep(MeshElementRegion startRegion, Mesh mesh) {
		if (startRegion.isConstrained())
			return startRegion;
		// int elemNum = s.getIndex();
		/* Determine the material in this tet and its dielectric Strength */
		// final long tetMaterialTag = mesh.getTags(elemNum)[0];
		final SEmaterial tetMaterial = (SEmaterial) startRegion.getMaterial();
		double eThresh = tetMaterial.getDielectricBreakdownField();
		if (eThresh >= Double.MAX_VALUE)
			return startRegion;
		else
			eThresh *= frac;

		final Tetrahedron tet = (Tetrahedron) startRegion.getShape();

		final double[] eField = correctedEField(tet);
		final double eMagSquared = Math2.dot(eField, eField);
		if (eMagSquared < (eThresh * eThresh))
			return startRegion;

		final double[] faceCurrents = faceCurrents(tet, eField);
		/*
		 * For charge to flow through a face we require two conditions be satisfied: (1)
		 * The current through that face must be negative. (2) The potential energy must
		 * decrease. The latter condition guarantees we do not have closed loops that
		 * repeat ad infinitum. Faces that do not meet these conditions have face
		 * current set to 0.
		 */
		final double v0 = tet.getPotential(tet.getCenter());
		int count = 0;
		int faceIndex = 0;
		for (int i = 0; i < 4; i++) {
			final Tetrahedron nextTet = tet.adjacentTet(i);
			if ((faceCurrents[i] < 0.) && (nextTet.getPotential(nextTet.getCenter()) > v0)) {
				faceCurrents[i] *= -1;
				faceIndex = i;
				count++;
			} else
				faceCurrents[i] = 0.;
		}
		/*
		 * Because we average the field across faces it is possible to have inflow of
		 * electrons at ALL faces. If this happens, we just leave our electron here.
		 */
		if (count == 0)
			return startRegion;
		/*
		 * If there's more than 1 candidate, we put the electron through one of them
		 * with probability proportional to the face current.
		 */
		if (count > 1)
			faceIndex = randomPick(faceCurrents);

		final Tetrahedron nextTet = tet.adjacentTet(faceIndex);
		final Long materialTag = nextTet.getTags()[0];
		final MeshedRegion parent = (MeshedRegion) (startRegion.getParent());
		final IMaterialScatterModel msm = parent.getMSM(materialTag);
		return new MeshElementRegion(parent, msm, nextTet);
	}

	private int randomPick(double[] x) {
		final double[] runningTotal = new double[x.length];

		runningTotal[0] = x[0];
		final int len = x.length;
		for (int i = 1; i < len; i++)
			runningTotal[i] = runningTotal[i - 1] + x[i];

		final double val = Math2.rgen.nextDouble() * runningTotal[len - 1];

		int pick = 0;
		while (runningTotal[pick] < val)
			pick++;

		return pick;
	}

	@SuppressWarnings("unused")
	private double[] faceCurrents(Tetrahedron tet) {
		return faceCurrents(tet, correctedEField(tet));
	}

	/**
	 * Returns an array of 4 values equal to (E1+E2[i]).S[i], where E1 is the
	 * corrected electric field (see notes at the correctedEField method) in the
	 * supplied tetrahedron, E2[i] is the corrected field in the tet through the ith
	 * face, and S[i] is a vector with magnitude equal to the area of the ith face
	 * and direction given by the face's outward normal. If a face has no neighbor
	 * or if its neighbor is of a different material, the corresponding value is 0.
	 * For finite conductivity, this quantity is proportional to the current through
	 * the faces.
	 *
	 * @param tet
	 * @param eField
	 * @return
	 */
	private double[] faceCurrents(Tetrahedron tet, double[] eField) {
		final Mesh mesh = tet.getMesh();
		final long tetMaterialTag = mesh.getTags(tet.getIndex())[0];

		final double[] result = new double[4];
		for (int faceIndex = 0; faceIndex < 4; faceIndex++) {
			final Tetrahedron nextTet = tet.adjacentTet(faceIndex);
			if (nextTet == null) // Boundary of the mesh
				continue;
			final long nextTetMaterialTag = mesh.getTags(nextTet.getIndex())[0];
			if (nextTetMaterialTag != tetMaterialTag)
				continue;
			final double[] n = tet.faceNormal(faceIndex);
			/*
			 * Current through each face is proportional to area*eField.n. eField is
			 * discontinuous across boundaries between tets. The average of the two is a
			 * consistent estimate. (It will be the same if tet and nextTet are reversed.)
			 * There's no need to actually divide by 2, since we're only comparing faces to
			 * each other.
			 */
			result[faceIndex] = Math2.dot(Math2.plus(eField, correctedEField(nextTet)), n) * tet.faceArea(faceIndex);
		}
		return result;
	}

	/**
	 * If we're not going to do charge flows across material boundaries we must
	 * retain only the component of electric field parallel to the boundary (i.e.,
	 * perpendicular to the normal vector of the boundary). This method returns the
	 * component of a tet's electric field that satisfies this condition.
	 *
	 * @param tet
	 * @return
	 */
	private double[] correctedEField(Tetrahedron tet) {

		final Mesh mesh = tet.getMesh();
		final long tetMaterialTag = mesh.getTags(tet.getIndex())[0];
		double[] eField = tet.getEField();
		for (int faceIndex = 0; faceIndex < 4; faceIndex++) {
			final int adjIndex = tet.adjacentTetIndex(faceIndex);
			if ((adjIndex == 0) || (tetMaterialTag != mesh.getTags(adjIndex)[0]))
				eField = perpendicularProjection(eField, tet.faceNormal(faceIndex));
		}
		return eField;
	}

	/**
	 * Computes v - (v.n)n where v and n are vectors and "." is the dot product. If
	 * n is normalized, this is the projection of v onto the plane for which n is
	 * the normal vector.
	 *
	 * @param v - A vector
	 * @param n - A vector
	 * @return
	 */
	private double[] perpendicularProjection(double[] v, double[] n) {
		return Math2.minus(v, parallelProjection(v, n));
	}

	/**
	 * Computes (v.n)n where v and n are vectors and "." is the dot product. If n is
	 * normalized, this is the projection of v in the n direction.
	 *
	 * @param v - A vector
	 * @param n - A vector
	 * @return
	 */
	private double[] parallelProjection(double[] v, double[] n) {
		return Math2.timesEquals(Math2.dot(v, n), n);
	}

}
