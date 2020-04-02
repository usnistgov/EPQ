/**
 * gov.nist.nanoscalemetrology.JMONSEL.ThresholdChargeConductionModel Created
 * by: jvillar Date: Jul 22, 2011
 */
package gov.nist.nanoscalemetrology.JMONSEL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.NISTMonte.IMaterialScatterModel;
import gov.nist.microanalysis.NISTMonte.MeshElementRegion;
import gov.nist.microanalysis.NISTMonte.MeshedRegion;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS.RegionBase;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.nanoscalemetrology.JMONSEL.Mesh.Tetrahedron;

/**
 * <p>
 * A conduction model in which scattering electrons that reach the end of their
 * trajectories do not necessarily contribute charge to the region in which they
 * stop. Rather, if the electric field in that region exceeds a specified
 * threshold, the electron flows along the field lines until it reaches a region
 * where the field is lower than the threshold. In this model, electrons will
 * flow across material boundaries, but will not flow into regions with density
 * < 100 kg/m^3.
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

/*
 * The following revised conduction model was started on 9/11/2012. It will use
 * full FEA to recompute fields after flowing charges.
 */

public class ThresholdConductionModel2 implements IConductionModel {

	private double frac;
	private final int maxIterations = 20;
	private int feaInterval = 3; /*
									 * Every feaInterval iterations, potentials update is via FEA. Otherwise it is
									 * via a faster more approximate method.
									 */
	private final double DENSITY_THRESHOLD = 100.;

	/**
	 * Constructs a ThresholdConductionModel
	 *
	 * @param frac - fraction of dielectric breakdown field at which current begins
	 *             to flow
	 */
	public ThresholdConductionModel2(double frac) {
		this.frac = frac;
	}

	/**
	 * Flows charges within the supplied MeshedRegion until the effective electric
	 * fields become equal or smaller in magnitude to this model's threshold value
	 * (supplied by the user at construction or via setFrac). "Effective" electric
	 * field means corrections to include internal repulsion of charges within a
	 * region but exclude the self-energy of the moving charge are made. Charge flow
	 * for fields above the threshold is assumed to be so fast that equilibrium is
	 * reached. Therefore the second argument (deltat) is included only for
	 * conformity to the more general IConductionModel interface. The supplied value
	 * of deltat is not used.
	 *
	 * @param meshedRegion - The MeshedRegion in which to possibly conduct charges
	 * @param deltat       - the time to conduct them (unused)
	 * @return - true if an FEA update is needed due to moved charges, false if it
	 *         is not needed (either because charges were not moved or because the
	 *         FEA was already performed).
	 * @see gov.nist.nanoscalemetrology.JMONSEL.IConductionModel#conductCharges(gov.nist.microanalysis.NISTMonte.MeshedRegion,
	 *      double,gov.nist.microanalysis.JMONSEL.IFEArunner)
	 */
	@Override
	public boolean conductCharges(MeshedRegion thisMeshedRegion, double deltat, IFEArunner feaRunner) {

		/*
		 * TODO The following is taking a lot of iterations. The number of iterations is
		 * smaller when p=1 than when p=2 (p the exponent of E field in the conductivity
		 * equation. A possible faster algorithm would use the following logic: Inside
		 * the below iterNum loop, I could flow charges, then begin a loop that
		 * estimates (without FEA) the potentials due to the flow and then flows charges
		 * again. This loop would repeat nFast times. Upon exit, an FEA is done (as now)
		 * and then we return to the top of the existing loop. Since nFast loops don't
		 * require FEA they are fast compared to the existing loops. In this way I could
		 * move nFast times as much charge per FEA, but with less accuracy. I'd have to
		 * pick nFast based on a speed/accuracy compromise. It's a sort of algorithm
		 * tuning parameter. To do this I should put the flow charge and estimate new
		 * potentials codes in separate methods. I'd need to think about what tuning
		 * data I need to record to assess the accuracy.
		 */
		final Mesh mesh = thisMeshedRegion.getMesh(); // debug
		final ChargeFlowDebugInfo cfDebug = new ChargeFlowDebugInfo(mesh, maxIterations); // debug
		boolean currentSolnIsFEA = true;
		for (int iterNum = 0; iterNum < maxIterations; iterNum++) {
			final HashMap<TetFace, Integer> flowMap = new HashMap<TetFace, Integer>();
			final boolean chargesMoved = flowChargesOnce(thisMeshedRegion, flowMap, cfDebug);
			/* Start debug section */
			/* Initialize nodes and elements to track immediately after each FEA */
			if (currentSolnIsFEA) {
				debugStats(cfDebug); // debug
				// cfDebug.resetTracked(); //With this included I track only since
				// most recent FEA. With it excluded, since iteration 0.
				final Set<Map.Entry<TetFace, Integer>> flowSet = flowMap.entrySet();
				for (final Map.Entry<TetFace, Integer> flow : flowSet) {
					final Integer deltaQ = flow.getValue();
					final Integer source = flow.getKey().getLeftTet();
					final Integer destination = flow.getKey().getRightTet();
					/*
					 * Add these, correcting the current charge count for the charge move we just
					 * made, in order to record the correct initial charge.
					 */
					cfDebug.addTrackedElement(source, mesh.getChargeNumber(source) + deltaQ);
					cfDebug.addTrackedElement(destination, mesh.getChargeNumber(destination) - deltaQ);
				}
			}
			/* End debug section */
			if (!chargesMoved) { // tentatively converged
				if (currentSolnIsFEA) {// truly converged, so return
					debugStats(cfDebug); // debug
					return false;
				} else { // otherwise, do a possibly unscheduled FEA update and
							// check
							// again.
					quickUpdate(thisMeshedRegion, flowMap); // debug
					cfDebug.trackedSnapShot(); // debug
					feaRunner.runFEA(thisMeshedRegion);
					cfDebug.trackedSnapShot(); // debug
					currentSolnIsFEA = true;
				}
			} else if ((iterNum % feaInterval) == (feaInterval - 1)) { // Use an FEA
																		// update
				feaRunner.runFEA(thisMeshedRegion);
				cfDebug.trackedSnapShot(); // debug
				currentSolnIsFEA = true;
			} else { // Use a quick approximate update
				quickUpdate(thisMeshedRegion, flowMap);
				cfDebug.trackedSnapShot(); // debug
				currentSolnIsFEA = false;
			}
		} // End iteration over maxIterations charge flows
		if (!currentSolnIsFEA) {
			feaRunner.runFEA(thisMeshedRegion);
			cfDebug.trackedSnapShot(); // debug
		}

		debugStats(cfDebug); // debug
		return false; // Return from here if we hit the maximum iteration limit
	}

	/*
	 * The next routines are used only for debugging. I can delete when finished.
	 */

	@SuppressWarnings("unused")
	private void debugStats(ChargeFlowDebugInfo cfDebug) {
		/* Debug stats */

		/* For charge change errors */
		final HashMap<Integer, Integer> chargeChanges = new HashMap<Integer, Integer>();

		final Set<Map.Entry<Integer, ArrayList<Integer>>> elements = cfDebug.getTrackedElements().entrySet();
		final HashMap<Integer, ArrayList<Double[]>> elementFields = cfDebug.getTrackedElementFields();
		if (elements.size() == 0)
			return;
		final int s = elements.size();
		final double[] deltaQerror = new double[s];
		final double[] deltaQactual = new double[s];
		final double[] absDeltaQactual = new double[s];
		final int[] elementIndices = new int[s];
		;
		final double[] efield0X = new double[s];
		final double[] efield0Y = new double[s];
		final double[] efield0Z = new double[s];
		final double[] efieldfX = new double[s];
		final double[] efieldfY = new double[s];
		final double[] efieldfZ = new double[s];
		int i = 0;
		for (final Map.Entry<Integer, ArrayList<Integer>> e : elements) {
			final ArrayList<Integer> snapshots = e.getValue();
			final double q0 = snapshots.get(0);
			final int len = snapshots.size();
			final double qfQuick = snapshots.get(len - 2);
			final double qfActual = snapshots.get(len - 1);
			deltaQerror[i] = qfQuick - qfActual;
			deltaQactual[i] = qfActual - q0;
			absDeltaQactual[i] = Math.abs(deltaQactual[i]);
			final Integer elemNum = e.getKey();
			chargeChanges.put(elemNum, snapshots.get(len - 1) - snapshots.get(0));
			final ArrayList<Double[]> fieldsSnapshot = elementFields.get(elemNum);
			final Double[] efield0 = fieldsSnapshot.get(0);
			final Double[] efieldf = fieldsSnapshot.get(len - 1);
			final int elemNumIndex = elemNum; // unbox it
			elementIndices[i] = elemNum;
			efield0X[i] = efield0[0];
			efield0Y[i] = efield0[1];
			efield0Z[i] = efield0[2];
			efieldfX[i] = efieldf[0];
			efieldfY[i] = efieldf[1];
			efieldfZ[i] = efieldf[2];
			i++;
		}
		final double meanError = mean(deltaQerror);
		final double sigmaError = sigma(deltaQerror);
		final double meanAbsChange = mean(absDeltaQactual);

		/* For potential change errors */
		final Set<Map.Entry<Integer, ArrayList<Double>>> nodes = cfDebug.getTrackedNodes().entrySet();
		final double[] deltaVerror = new double[nodes.size()];
		final double[] deltaVactual = new double[nodes.size()];
		final double[] absDeltaVactual = new double[nodes.size()];
		final double[] deltaVfractionalError = new double[nodes.size()];
		i = 0;
		for (final Map.Entry<Integer, ArrayList<Double>> n : nodes) {
			final ArrayList<Double> snapshots = n.getValue();
			final double v0 = snapshots.get(0);
			final int len = snapshots.size();
			final double vfQuick = snapshots.get(len - 2);
			final double vfActual = snapshots.get(len - 1);
			deltaVerror[i] = vfQuick - vfActual;
			deltaVactual[i] = vfActual - v0;
			absDeltaVactual[i] = Math.abs(deltaVactual[i]);
			if (deltaVactual[i] != 0.)
				deltaVfractionalError[i] = deltaVerror[i] / deltaVactual[i];
			i++;
		}
		final double meanVError = mean(deltaVerror);
		final double sigmaVError = sigma(deltaVerror);
		final double meanAbsVChange = mean(absDeltaVactual);
		final double meanFractionalVError = mean(deltaVfractionalError);
		final double sigmaFractionalVError = sigma(deltaVfractionalError);

		/* End debug */
	}

	private double mean(double[] x) {
		return Math2.sum(x) / x.length;
	}

	@SuppressWarnings("unused")
	private double mean(int[] x) {
		return ((double) Math2.sum(x)) / x.length;
	}

	private double sigma(double[] x) {
		final int n = x.length;
		final double sumx = Math2.sum(x);
		final double sumx2 = Math2.sum(Math2.multiply(x, x));
		return Math.sqrt((sumx2 - ((sumx * sumx) / n)) / (n - 1));
	}

	/* End debug */
	/**
	 * Flows at most 1 charge per region. Returns true if any charges were moved,
	 * false if not.
	 *
	 * @return
	 */
	private boolean flowChargesOnce(MeshedRegion thisMeshedRegion, HashMap<TetFace, Integer> flowMap,
			ChargeFlowDebugInfo cfDebug) {
		final Mesh mesh = thisMeshedRegion.getMesh();
		final int nElements = mesh.getNumberOfElements();

		/*
		 * Store tets where the field exceeds the threshold along with abs(total
		 * current) and the individual (signed) currents through their faces.
		 */
		final HashMap<TetFace, Double> facesCurrentPairs = new HashMap<TetFace, Double>();
		final TreeSet<TetCurrentPair> tetCurrentPairs = new TreeSet<TetCurrentPair>();

		/*
		 * If current flow is a fraction of one charge, this algorithm uses a random
		 * number between 0 and 1 to flow or not flow one charge with an appropriate
		 * probability. To avoid wasting time on calculations when charge is too
		 * unlikely to flow, the algorithm ignores flows smaller than MINFRAC of the
		 * maximum flow. MINFRAC is set on the following line.
		 */
		final double MINFRAC = 0.01;

		/* Debug stats */
		int tetCount = 0;
		int attemptedChargeFlowCount = 0;
		int successfulChargeFlowCount = 0;
		double maxCurrent = 0.;
		/* end debug */

		double maxTetCurrent = 0.; // Absolute value of largest current found
									// so far.
		/* Initialize lists to hold data about elements that need charge flows */
		// final ArrayList<MeshElementRegion> tetsOverThresh = new
		// ArrayList<MeshElementRegion>();
		// final ArrayList<double[]> initialEFields = new
		// ArrayList<double[]>();
		// final ArrayList<Double> eThreshSquared = new ArrayList<Double>();

		for (int elemNum = 1; elemNum < nElements; elemNum++)
			// elements
			/* restrict to volume elements with nonzero charge */
			if (mesh.isVolumeType(elemNum) && (mesh.getChargeNumber(elemNum) != 0)) {

				/* Compute net current out of this tet */
				final Tetrahedron tet = Tetrahedron.getTetrahedron(mesh, elemNum);

				double current = 0.;
				for (int i = 0; i <= 3; i++) {
					final int adjacentIndex = tet.adjacentTetIndex(i);
					if (adjacentIndex > 0) { // Face is in the interior
						double fc;
						// If not already saved in opposite direction, compute
						// and save it
						if (!facesCurrentPairs.containsKey(new TetFace(adjacentIndex, elemNum))) {
							fc = faceCurrent(thisMeshedRegion, elemNum, adjacentIndex);
							facesCurrentPairs.put(new TetFace(elemNum, adjacentIndex), fc);
							current += fc;
						}
					} // End face is in the interior
					/*
					 * TODO -- Insert an else block here to deal with the case that this face is a
					 * boundary of the mesh. Charge might flow if the material on the other side of
					 * the boundary is a conductor.
					 */
				}

				/*
				 * If the current is high enough, make a note to come back to this one.
				 */
				final double abscurrent = Math.abs(current);
				if (abscurrent > maxTetCurrent)
					maxTetCurrent = abscurrent;
				if (abscurrent > (MINFRAC * maxTetCurrent))
					tetCurrentPairs.add(new TetCurrentPair(elemNum, abscurrent));
				if (abscurrent != 0.)
					tetCount++;
			} // End mesh Element is Volume type

		/*
		 * Loop through the noted tets in order of current from largest to MINFRAC of
		 * largest, making the currents flow.
		 */
		final Iterator<TetCurrentPair> it = tetCurrentPairs.descendingIterator();
		if (!it.hasNext()) {
			cfDebug.addTetCount(tetCount);
			cfDebug.addAttemptedChargeFlowCount(attemptedChargeFlowCount);
			cfDebug.addSuccessfulChargeFlowCount(successfulChargeFlowCount);
			cfDebug.addMaxCurrent(maxCurrent);
			return false; // Converged, no charges moved
		}

		/*
		 * Keep a set of tets in which we've changed the charge. We'll check this list
		 * to avoid altering charges more than once per cycle. TODO Change to input Map
		 */
		final HashSet<Integer> tetsWithAlteredCharge = new HashSet<Integer>();

		TetCurrentPair nextTetCurrent = it.next();
		final double minCurrent = MINFRAC * maxTetCurrent;
		double tetCurrent = nextTetCurrent.getCurrent();
		maxCurrent = tetCurrent; // debug

		while (tetCurrent > minCurrent) {
			attemptedChargeFlowCount++; // debug
			/* Decide whether to move a charge in this tet */
			if (Math2.rgen.nextDouble() < (tetCurrent / maxTetCurrent)) {
				/*
				 * Here if we have decided yes. Get IDs of affected tets (this one and its
				 * neighbors) and pull the faceCurrents out of storage.
				 */
				final int elemNum = nextTetCurrent.getTetID();
				final Tetrahedron tet = Tetrahedron.getTetrahedron(mesh, elemNum);
				final int[] adjacentTetNumber = new int[4];

				final double[] current = new double[4];
				final double[] currentSign = new double[4];
				for (int i = 0; i < 4; i++) {
					adjacentTetNumber[i] = tet.adjacentTetIndex(i);
					final TetFace tf = new TetFace(elemNum, adjacentTetNumber[i]);
					final Double fc = facesCurrentPairs.get(tf);
					if (fc != null) {
						if (fc < 0.) {
							currentSign[i] = -1.;
							current[i] = -fc;
						} else {
							currentSign[i] = 1.;
							current[i] = fc;
						}
					} else
						current[i] = 0.;
				}

				// Choose a face with probability proportional to its current
				final int index = randomPick(current);
				final int adjTet = adjacentTetNumber[index];
				if (!tetsWithAlteredCharge.contains(adjTet)) {
					/*
					 * Arrive here only if the chosen tet has not already had a charge flow since
					 * the last FEA. In that case, move one charge between the chosen pair
					 */
					if (currentSign[index] > 0.) { // + Charge flows out
						mesh.decrementChargeNumber(elemNum);
						mesh.incrementChargeNumber(adjTet);
						flowMap.put(new TetFace(elemNum, adjTet), 1);
					} else { // + Charge flows in
						mesh.incrementChargeNumber(elemNum);
						mesh.decrementChargeNumber(adjTet);
						flowMap.put(new TetFace(elemNum, adjTet), -1);
					}
					/* Make a note that we've done these */
					tetsWithAlteredCharge.add(adjTet);
					tetsWithAlteredCharge.add(elemNum);
					successfulChargeFlowCount++;
				}
			}

			/* Get the next unprocessed region */
			while (it.hasNext()) {
				nextTetCurrent = it.next();
				if (!tetsWithAlteredCharge.contains(nextTetCurrent.getTetID()))
					break;
			}
			if (it.hasNext())
				tetCurrent = nextTetCurrent.getCurrent();
			else
				tetCurrent = 0.;
		} // Finished moving charges
		cfDebug.addTetCount(tetCount);
		cfDebug.addAttemptedChargeFlowCount(attemptedChargeFlowCount);
		cfDebug.addSuccessfulChargeFlowCount(successfulChargeFlowCount);
		cfDebug.addMaxCurrent(maxCurrent);
		return true; // Some charges were moved
	}

	/*
	 * quickUpdate is used in lieu of FEA to update the potentials of nodes of the
	 * mesh. It is a poorer approximation, using only distance from center of charge
	 * to adjust existing potentials, but it is much faster, so may be used along
	 * with occasional full FEA to get back on track. The approximation adjusts node
	 * potentials by adding e/(4*PI*epsilon*r) - e/(4*PI*epsilon*CUTOFFRADIUS) for
	 * r<CUTOFFRADIUS and 0 for r>=CUTOFFRADIUS. The second term makes sure the two
	 * expressions agree at the boundary. The value of CUTOFFRADIUS is a parameter
	 * of the approximation. It can be set by editing the next line.
	 */
	private final double CUTOFFRADIUS = 1.e-8;
	private final double CUTOFFRADIUS2 = CUTOFFRADIUS * CUTOFFRADIUS;

	private void quickUpdate(MeshedRegion thisMeshedRegion, HashMap<TetFace, Integer> flowMap) {
		final Mesh mesh = thisMeshedRegion.getMesh();

		final Set<Map.Entry<TetFace, Integer>> flowSet = flowMap.entrySet();
		for (final Map.Entry<TetFace, Integer> fc : flowSet) {
			final TetFace tf = fc.getKey();
			int source;
			int destination;
			/*
			 * Take account of the sign of current by switching source and destination if
			 * necessary, such that positive charge always flows out of the source into the
			 * destination.
			 */
			if (fc.getValue() > 0) {
				source = tf.getLeftTet();
				destination = tf.getRightTet();
			} else {
				source = tf.getRightTet();
				destination = tf.getLeftTet();
			}
			final Tetrahedron sourceTet = Tetrahedron.getTetrahedron(mesh, source);
			final Tetrahedron destinationTet = Tetrahedron.getTetrahedron(mesh, destination);
			final double[] xsource = sourceTet.getCenter();
			final double[] xdest = destinationTet.getCenter();
			final double[] r0 = Math2.multiply(0.5, Math2.plus(xsource, xdest));
			final double epsSource = thisMeshedRegion.getDielectricConstant(source);
			final double epsDest = thisMeshedRegion.getDielectricConstant(destination);
			/* Get the nearby nodes and affected volumes */
			final Set<Map.Entry<Integer, Double>> nodes = getNearbyNodes(thisMeshedRegion, source, destination, r0)
					.entrySet();
			/* Make corrections to potentials of the nearby nodes */
			for (final Entry<Integer, Double> nodeEntry : nodes) {
				final int nodeNum = nodeEntry.getKey();
				final double eps = nodeEntry.getValue();
				final double[] x = mesh.getNodeCoordinates(nodeNum);
				final double deltaPhi = phiPointCharge(xdest, epsDest, x, eps)
						- phiPointCharge(xsource, epsSource, x, eps);
				mesh.setNodePotential(nodeNum, mesh.getNodePotential(nodeNum) + deltaPhi);
			}
		}
	}

	/**
	 * Computes the potential at x due to a charge e at xSource. The relative
	 * dielectric constants at xSource and x are epsSource and eps respectively. The
	 * formula here is crude. It returns 0 for x at distance greater than a cutoff.
	 * At distances less than the cutoff it uses the point charge formula modified
	 * to use the mean dielectric constant (if it differs at xSource and x) and
	 * shifted to be 0 at the cutoff distance. In this way potential differences at
	 * distances within cutoff are as given by the point charge formula while those
	 * outside are 0. The mean dielectric constant is the right formula if xSource
	 * and x are in each in an infinite half space (separated by a plane) of
	 * materials with a different dielectric constant. Thus, this formula does not
	 * take into account boundary conditions, shapes of boundaries, or the FEA
	 * smearing of charge uniformly over the tetrahedral region it occupies.
	 *
	 * @param xSource   - 3-vector coordinates of source charge
	 * @param epsSource - relative dielectric constant at xSource
	 * @param x         - 3-vector coordinates of point where potential is desired
	 * @param eps       - relative dielectric constant at x
	 * @return - the potential in volts at x due charge +1e at xSource
	 */
	final private double eOver2PiEps0 = PhysicalConstants.ElectronCharge
			/ (2. * Math.PI * PhysicalConstants.PermittivityOfFreeSpace);

	private double phiPointCharge(double[] xSource, double epsSource, double[] x, double eps) {
		final double r2 = Math2.distanceSqr(xSource, x);
		if (r2 > CUTOFFRADIUS2)
			return 0.;
		else
			return (eOver2PiEps0 * ((1. / Math.sqrt(r2)) - (1. / CUTOFFRADIUS))) / (epsSource + eps);
	}

	/**
	 * Starting from regions with numbers given by source and destination, find all
	 * nodes "close" to the point r0. Close is defined by JMONSEL for the purpose of
	 * this routine to be 10 nm. Return a map of nodes and corresponding dielectric
	 * constants.
	 *
	 * @param mesh
	 * @param source
	 * @param destination
	 * @return
	 */
	private HashMap<Integer, Double> getNearbyNodes(MeshedRegion thisMeshedRegion, int source, int destination,
			double[] r0) {
		final Mesh mesh = thisMeshedRegion.getMesh();
		final HashMap<Integer, Double> nodeMap = new HashMap<Integer, Double>();

		/* Store the initial nodes and regions */
		final HashSet<Integer> volumes = new HashSet<Integer>();
		volumes.add(source);
		volumes.add(destination);
		HashSet<Integer> nextLayer = new HashSet<Integer>(volumes);
		for (final int elemNum : nextLayer) {
			final int[] nodes = mesh.getNodeIndices(elemNum);
			final double eps = thisMeshedRegion.getDielectricConstant(elemNum);
			for (int i = 0; i < 4; i++)
				if (!nodeMap.containsKey(nodes[i])) { // Skip if this node was
														// already collected
					final double dist2 = Math2.distanceSqr(mesh.getNodeCoordinates(nodes[i]), r0);
					if (dist2 < CUTOFFRADIUS2)
						nodeMap.put(nodes[i], eps);
				}
		}

		/*
		 * Find the next layer of near neighbors. Add each element's nodes to our list
		 * if they are close enough. If none are close enough, strike that element from
		 * our list. Repeat until the list is empty.
		 */
		while (nextLayer.size() > 0) {
			final HashSet<Integer> previousLayer = nextLayer;
			nextLayer = new HashSet<Integer>(); // A new empty set to hold what we
												// find
			for (final int elemNum : previousLayer)
				for (int i = 0; i < 4; i++) {
					final int newVol = mesh.getAdjacentVolumeIndex(elemNum, i);
					if (!volumes.contains(newVol)) { // it really is a new one
						final int[] nodes = mesh.getNodeIndices(newVol); // get its
																			// nodes
						boolean inRange = false;
						final double eps = thisMeshedRegion.getDielectricConstant(newVol);
						for (int nodeIndex = 0; nodeIndex < 4; nodeIndex++)
							if (nodeMap.containsKey(nodes[nodeIndex]))
								inRange = true;
							else {
								final double dist2 = Math2.distanceSqr(mesh.getNodeCoordinates(nodes[nodeIndex]), r0);
								if (dist2 < CUTOFFRADIUS2) { // Add if close enough
									nodeMap.put(nodes[nodeIndex], eps);
									inRange = true;
								}
							} // end if skip node
						/*
						 * If we used a node from this volume, add it to nextLayer and volumes
						 */
						if (inRange) {
							nextLayer.add(newVol);
							volumes.add(newVol);
						}
					} // end if skip volume
				} // end for adjacent volumes
		}
		return nodeMap;
	}

	/*
	 * Conductivity is proportional to electric field to a power, p. The value of p
	 * may be material and field dependent. If the conduction mechanism is
	 * Fowler-Nordheim, 2<=p<=infinity. (See my ConductionModelNotes.nb.) As p ->
	 * infinity current flows only across one face, the one where the field is
	 * highest, at a time. As p gets smaller, flows are more nearly simultaneous,
	 * though still favoring places with higher fields. Smaller values of p should
	 * therefore be better for speed of the algorithm. I think it likely that if the
	 * calculation proceeds until current no longer flows, the value of p will not
	 * strongly effect the final result. The reason is that flows are from one tet
	 * to its neighbor, generally a very small distance. The difference in fields is
	 * a dipole, therefore mainly significant only in the neighborhood of the moved
	 * charge, and this in turn means that charges at some distance can be safely
	 * independently moved.
	 */
	private double currentFromField(double eFieldMag) {
		return eFieldMag; // If I want p = 1
		// return eFieldMag * eFieldMag; // If I want p = 2
		/*
		 * To use a higher power than p = 2, set the desired value of p and replace the
		 * above line with some similar to below.
		 */
		// final double p = 3.;
		// return Math.pow(eFieldMag, p);
	}

	/**
	 * Returns a double proportional to the signed current across the face shared by
	 * the tets specified by the two ID numbers. Returns 0 if there is no current.
	 *
	 * @param tet1ID integer index of the source tet
	 * @param tet2ID integer index of the destination tet
	 * @return
	 */
	private double faceCurrent(MeshedRegion thisMeshedRegion, int tet1ID, int tet2ID) {
		/*
		 * Many or most tet pairs can have no charge flow because they fail an
		 * easy-to-check condition. We go through these first to save time. I don't
		 * check that region 1 is non-vacuum because I'm assuming this routine is only
		 * called when n1 != 0.
		 */

		final Mesh mesh = thisMeshedRegion.getMesh();
		final int n1 = mesh.getChargeNumber(tet1ID);

		/* The destination can't be vacuum. */
		final long tag2 = mesh.getTags(tet2ID)[0];
		final SEmaterial mat2 = (SEmaterial) thisMeshedRegion.getMSM(tag2).getMaterial();
		final double density2 = mat2.getDensity();
		if (density2 < DENSITY_THRESHOLD)
			return 0.;

		final Tetrahedron tet1 = Tetrahedron.getTetrahedron(mesh, tet1ID);
		final Tetrahedron tet2 = Tetrahedron.getTetrahedron(mesh, tet2ID);
		final double pot1 = tet1.getPotential(tet1.getCenter());
		final double pot2 = tet2.getPotential(tet2.getCenter());
		final double deltaV = pot2 - pot1;
		final int n2 = mesh.getChargeNumber(tet2ID);

		/*
		 * If we pass all the previous hurdles, estimate the energy difference if we
		 * flow 1 charge.
		 */

		final double r1 = tet1.getEquivalentSphereRadius();
		final double r2 = tet2.getEquivalentSphereRadius();
		final long tag1 = mesh.getTags(tet1ID)[0];
		final SEmaterial mat1 = (SEmaterial) thisMeshedRegion.getMSM(tag1).getMaterial();
		// TODO This use of eps values may be incorrect if mat2 is a conductor.
		final double eps1 = mat1.getEpsr();
		final double eps2 = mat2.getEpsr();

		final double effDeltaV = effectivePotentialDifference(n1, n2, r1, r2, eps1, eps2, deltaV);

		final double thresholdEnergy = frac
				* ((mat1.getDielectricBreakdownField() * r1) + (mat2.getDielectricBreakdownField() * r2));
		double current = 0.;
		if (effDeltaV < -thresholdEnergy)
			/*
			 * Pass the positive magnitude of effDeltaV/distance, but retain the sign of the
			 * current.
			 */
			current = -currentFromField(-effDeltaV / (r1 + r2));
		else if (effDeltaV > thresholdEnergy)
			current = currentFromField(effDeltaV / (r1 + r2));
		return current;
	}

	/**
	 * This function implements the expression for effective potential difference in
	 * my ConductionModelNotes.nb notebook, in the section on "Formulas useful for
	 * implementation." The effective difference is the potential that is relevant
	 * for determining if a charge in one of two adjacent regions will transfer to
	 * the other region. It is equal to the potential difference of the last FEA
	 * (input as deltaV, the last argument) with corrections. The corrections
	 * account for 1) the fact that the FEA potential (erroneously for this purpose)
	 * includes the field of the charge we are considering transferring, 2) the
	 * mutual repulsion of like charges within the same region, which is excluded
	 * wholly or in part by FEA, and 3) the interaction between the charge we are
	 * considering moving and the charges in the neighboring region. The result is
	 * an effective potential: the energy difference between the two states (charge
	 * in region 1 vs. charge in region 2) divided by e. It is positive if its
	 * tendency is to cause positive current out of region 1 (transfer of +e from
	 * 1->2 or -e from 2->1) and negative if the reverse. All corrections are made
	 * in the approximation that the regions are spheres with radii as supplied.
	 *
	 * @param n1     - Signed number of charges in region 1
	 * @param n2     - Signed number of charges in region 2
	 * @param r1     - effective radius of region 1
	 * @param r2     - effective radius of region 2
	 * @param eps1   - relative dielectric constant in region 1
	 * @param eps2   - relative dielectric constant in region 2
	 * @param deltaV - mean FEA potential in region 2 minus the mean potential in
	 *               region 1
	 * @return - the effective potential difference (including corrections).
	 */
	private double effectivePotentialDifference(int n1, int n2, double r1, double r2, double eps1, double eps2,
			double deltaV) {
		final double PREFACTOR0 = PhysicalConstants.ElectronCharge
				/ (20. * Math.PI * PhysicalConstants.PermittivityOfFreeSpace);
		double epsr;
		if (eps1 == eps2)
			epsr = eps1;
		else
			epsr = ((eps1 * r1) + (eps2 * r2)) / (r1 + r2);
		final double prefactor = PREFACTOR0 / epsr;
		final double term1 = -deltaV + (prefactor * ((n1 / r1) - (n2 / r2)));
		/*
		 * A note on the calculation of rchoice below: The analysis (see
		 * ConductionModelNotes.nb) indicates there is a term that is either 6/r1 or
		 * 6/r2 in the potential difference expression. Either r1 or r2 is chosen
		 * depending upon which region supplies the carrier of the charge that moves.
		 * Within the present model I am assuming carriers are scarce, i.e., that the
		 * small regions (the only ones for which this is likely to make much
		 * difference) are unlikely to contain trapped charges of either sign apart from
		 * those excess charges that we know ended up in them as a result of the
		 * scattering processes we track in JMONSEL. Hence the requirement that the
		 * carrier from a region must be of the same sign as excess charges in the
		 * region. In cases when current of the correct sign can only be generated by a
		 * carrier from one region, we use that region's radius. If the carrier can come
		 * from either region (because the regions have excess charge of opposite sign)
		 * then I assume it flows from the larger region, since this choice is the one
		 * that minimizes energy. To change the model to assume plentiful carriers,
		 * i.e., that all regions contain trapped charges of both signs and therefore
		 * that both directions of current are always possible, we would always choose
		 * the larger r value, as in the final else statement.
		 */
		if (term1 > 0.) {
			double rchoice;
			if ((n1 >= 1) && (n2 >= 0)) // Only positive carrier out of region 1 is
										// permitted
				rchoice = r1;
			else if ((n1 <= 0) && (n2 <= 1)) // Only negative carrier out of region
												// 2
				rchoice = r2;
			else
				rchoice = r1 > r2 ? r1 : r2; // Both permitted so carrier from
												// larger region
			final double term2 = prefactor * ((5. / (r1 + r2)) - (6. / rchoice));
			final double effPot = term1 + term2;
			return effPot > 0. ? effPot : 0.;
		} else {
			double rchoice;
			if ((n1 <= -1) && (n2 <= 0)) // Only negative carrier out of region 1 is
											// permitted
				rchoice = r1;
			else if ((n1 >= 0) && (n2 >= 1)) // Only positive carrier out of region
												// 2
				rchoice = r2;
			else
				rchoice = r1 > r2 ? r1 : r2; // Both permitted so carrier from
												// larger region
			final double term2 = prefactor * ((5. / (r1 + r2)) - (6. / rchoice));
			final double effPot = term1 - term2;
			return effPot < 0 ? effPot : 0.;
		}
	}

	/**
	 * If we're not going to do charge flows into low density regions we must retain
	 * only the component of electric field parallel to the boundary (i.e.,
	 * perpendicular to the normal vector of the boundary). This method returns the
	 * component of a tet's electric field that satisfies this condition.
	 *
	 * @param tet
	 * @return
	 */
	private double[] correctedEField(MeshElementRegion tetRegion) {

		final Tetrahedron tet = (Tetrahedron) tetRegion.getShape();

		double[] eField = tet.getEField();
		for (int faceIndex = 0; faceIndex < 4; faceIndex++)
			if (!currentAllowed(tetRegion, tet, faceIndex))
				eField = perpendicularProjection(eField, tet.faceNormal(faceIndex));
		return eField;
	}

	/**
	 * <p>
	 * Private utility to determine whether electrons are permitted to traverse the
	 * indexed face of a given Tetrahedron. The criterion is that the material on
	 * the other side of the face must have density > DENSITY_THRESHOLD, but the way
	 * to determine this varies depending upon whether the neighboring region is
	 * part of the mesh or not.
	 * </p>
	 * <p>
	 * tetRegion and tet are redundant in the sense that the calling routine is
	 * responsible to make sure tet = tetRegion.getShape(). (The caller has already
	 * done this, so it saves time to pass both.)
	 * </p>
	 *
	 * @param tetRegion
	 * @param tet
	 * @param faceIndex
	 * @return
	 */
	private boolean currentAllowed(MeshElementRegion tetRegion, Tetrahedron tet, int faceIndex) {
		final Tetrahedron nextTet = tet.adjacentTet(faceIndex);
		if (nextTet == null) {
			/*
			 * Next region is outside the mesh. This is a rare case but must be handled
			 * first because nextTet is null, so will not support the methods calls needed
			 * for the other cases.
			 */
			final double[] pos0 = tet.getCenter();
			final double[] pos1 = Math2
					.plus(Math2.timesEquals(2. * MonteCarloSS.ChamberRadius, tet.faceNormal(faceIndex)), pos0);
			final RegionBase nextRegion = tetRegion.findEndOfStep(pos0, pos1);
			final double density = nextRegion.getMaterial().getDensity();
			if (density >= DENSITY_THRESHOLD)
				return true;
			else
				return false;
		} else if (tet.getTags()[0] == nextTet.getTags()[0])
			/*
			 * The most common case. Tag[0] the same means material is the same. Current is
			 * always allowed.
			 */
			return true;
		else if (tetMSM((MeshedRegion) tetRegion.getParent(), nextTet).getMaterial().getDensity() >= DENSITY_THRESHOLD)
			/*
			 * Materials are different but the new material is high density enough to allow
			 * current.
			 */
			return true;
		else
			/*
			 * None of the above, so the adjacent region is a mesh element with different
			 * material, and the different material is too low density to support a current.
			 */
			return false;
	}

	/**
	 * Returns an array of 4 values equal to (E0+E[i]).S[i], where E0 and E[i] are
	 * the corrected electric fields (see notes at the correctedEField method) in
	 * the supplied tetrahedron and in the neighboring tetrahedron through the ith
	 * face and S[i] is a vector with magnitude equal to the area of the ith face
	 * and direction given by the face's outward normal. If a face's neighboring
	 * region has density too low to support a charge (e.g., it's vacuum), the
	 * corresponding value is 0. For finite conductivity, this quantity is
	 * proportional to the current through the faces.
	 *
	 * @param tetRegion
	 * @param eField0
	 * @return
	 */
	private double[] faceCurrents(MeshElementRegion tetRegion, double[] eField0) {
		final double[] result = new double[4];
		for (int faceIndex = 0; faceIndex < 4; faceIndex++)
			result[faceIndex] = faceCurrent(tetRegion, eField0, faceIndex);
		return result;
	}

	/**
	 * Returns (E0+E1).S, where E0 and E1 are the corrected electric fields (see
	 * notes at the correctedEField method) in the supplied tetRegion and its
	 * neighbor through the face indexed by faceIndex. S is a vector with magnitude
	 * equal to the area of the indexed face and direction given by the face's
	 * outward normal. If a face's neighboring region has density too low to support
	 * a charge (e.g., it's vacuum), the corresponding value is 0. For finite
	 * conductivity, this quantity is proportional to the current through the face.
	 *
	 * @param tetRegion
	 * @param eField0
	 * @param faceIndex
	 * @return
	 */
	private double faceCurrent(MeshElementRegion tetRegion, double[] eField0, int faceIndex) {
		final Tetrahedron tet = (Tetrahedron) tetRegion.getShape();

		/*
		 * We have to do the "if" on the next line even though E.n ought to be 0 for
		 * forbidden faces because round-off error can make it not so. Even though the
		 * error is tiny, it is significant if it becomes the only face through which
		 * current is allowed.
		 */
		double result = 0.;
		if (currentAllowed(tetRegion, tet, faceIndex)) {
			final Tetrahedron nextTet = tet.adjacentTet(faceIndex);
			double[] eField;
			if (nextTet == null)
				/*
				 * Here if this face is a boundary of the mesh. We're going to treat such faces
				 * as though all current incident on them is conducted away. I.e., we'll treat
				 * it as though the electric field on the other side is equal to the one in this
				 * tet.
				 */
				eField = Math2.multiply(2., eField0);
			else {
				final MeshedRegion parent = (MeshedRegion) tetRegion.getParent();
				final IMaterialScatterModel msm = tetMSM(parent, nextTet);
				final MeshElementRegion nextRegion = new MeshElementRegion(parent, msm, nextTet);
				eField = Math2.plus(eField0, correctedEField(nextRegion));
			}
			final double[] n = tet.faceNormal(faceIndex);
			result = Math2.dot(eField, n) * tet.faceArea(faceIndex);
		}

		return result;
	}

	/**
	 * Gets the current value assigned to frac
	 *
	 * @return Returns the frac.
	 */
	public double getFrac() {
		return frac;
	}

	/*
	 * oneStep() algorithm summary: This algorithm will flow holes if
	 * positiveCharge==true or electrons if false. The corrected electric field in
	 * this region is the field with any components in forbidden directions (e.g.,
	 * into neighboring vacuum) subtracted. Find the corrected electric field in
	 * this region and compare it to the threshold for this region. If it is less,
	 * then the charge doesn't move. (We return the start region.) If it is more, we
	 * flow the charge across one of the startRegion's faces. (We return the region
	 * on the other side of the face.) Which face? We find the faces with currents
	 * of the correct sign (negative for electrons, positive for holes). The current
	 * through a face is proportional to the dot product of the corrected E field
	 * and its area * outward normal vector. If there's only one of these, that's
	 * the one. If there are more than one, we choose one with probability
	 * proportional to the current.
	 */
	private MeshElementRegion oneStep(MeshElementRegion startRegion, Mesh mesh, boolean positiveCharge) {
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

		final double[] eField = correctedEField(startRegion);
		final double eMagSquared = Math2.dot(eField, eField);
		if (eMagSquared < (eThresh * eThresh))
			return startRegion;

		final double[] faceCurrents = faceCurrents(startRegion, eField);
		/*
		 * For our charge to flow through a face we require two conditions be satisfied:
		 * (1) The current through that face must have the same sign as the charge. (2)
		 * The potential energy must decrease. The latter condition guarantees we do not
		 * have closed loops that repeat ad infinitum. Faces that do not meet these
		 * conditions have face current set to 0.
		 */
		final Tetrahedron tet = (Tetrahedron) startRegion.getShape();
		final double v0 = tet.getPotential(tet.getCenter());
		int count = 0;
		int faceIndex = 0;

		for (int i = 0; i < 4; i++) {
			final Tetrahedron nextTet = tet.adjacentTet(i);
			boolean chargeCanFlow;
			if (positiveCharge)
				chargeCanFlow = (faceCurrents[i] > 0.)
						&& ((nextTet == null) || (nextTet.getPotential(nextTet.getCenter()) < v0));
			else
				chargeCanFlow = (faceCurrents[i] < 0.)
						&& ((nextTet == null) || (nextTet.getPotential(nextTet.getCenter()) > v0));
			if (chargeCanFlow) {
				if (!positiveCharge)
					faceCurrents[i] *= -1.;
				faceIndex = i;
				count++;
			} else
				faceCurrents[i] = 0.;
		}
		/*
		 * Because we average the field across faces and elements have finite size, it
		 * is possible to have no face with current of the correct sign. (E.g., the
		 * element straddles a potential extremum.) If this happens, we just leave our
		 * charge here.
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
		if (nextTet == null)
			return null;
		final MeshedRegion parent = (MeshedRegion) (startRegion.getParent());
		final IMaterialScatterModel msm = tetMSM(parent, nextTet);
		return new MeshElementRegion(parent, msm, nextTet);
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

	/**
	 * The threshold field in a region, above which conduction occurs, will be
	 * frac*(dielectric breakdown field), where frac is supplied as the argument of
	 * this function and the dielectric breakdown field is obtained from the
	 * SEmaterial that occupies the region.
	 *
	 * @param frac The value to which to set frac.
	 */
	public void setFrac(double frac) {
		if (this.frac != frac)
			this.frac = frac;
	}

	@Override
	public MeshElementRegion stopRegion(MeshElementRegion startRegion) {
		return stopRegion(startRegion, false);
	}

	public void setFEAInterval(int feaInterval) {
		if (feaInterval > 0)
			this.feaInterval = feaInterval;
	}

	/**
	 * This private utility flows charges of either sign. The public stopRegion
	 * always flows electrons, so calls this one with chargeIsPositive = false.
	 *
	 * @param startRegion
	 * @param chargeIsPositive
	 * @return
	 */
	@SuppressWarnings("unused")
	private MeshElementRegion stopRegion(MeshElementRegion startRegion, boolean chargeIsPositive) {
		final Mesh mesh = startRegion.getMesh();
		MeshElementRegion finalRegion = oneStep(startRegion, mesh, chargeIsPositive);
		int count = 0; // debug
		final double[] x0 = ((Tetrahedron) (startRegion.getShape())).getCenter();// debug
		final int id0 = ((Tetrahedron) (startRegion.getShape())).getIndex();// debug
		double[] x = new double[3];// debug
		double v = 0.;
		int id = 0;// debug
		while (finalRegion != startRegion) {
			startRegion = finalRegion;
			finalRegion = oneStep(startRegion, mesh, chargeIsPositive);
			count++;// debug
			if (finalRegion == null)
				return finalRegion;
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

	/**
	 * Utility to return the msm of a given tetrahedron. User supplies the tet and
	 * the MeshedRegion that contains it.
	 *
	 * @param parent - The MeshedRegion that contains the tet
	 * @param tet    - the tetrahedron
	 * @return
	 */
	private IMaterialScatterModel tetMSM(MeshedRegion parent, Tetrahedron tet) {
		final Long materialTag = tet.getTags()[0];
		return parent.getMSM(materialTag);
	}

	/**
	 * <p>
	 * A class for storing and establishing an ordering for pairs of numbers,
	 * representing a tetrahedron (via its integer ID) and the total current into or
	 * out of that tetrahedron. The ordering is in terms of absolute value of the
	 * current.
	 * </p>
	 *
	 * @author John Villarrubia
	 * @version 1.0
	 */
	private class TetCurrentPair implements Comparable<TetCurrentPair> {
		private final int tetID;
		private final double current;

		/**
		 * Constructs a TetCurrentPair
		 *
		 * @param tetID
		 * @param current
		 */
		public TetCurrentPair(int tetID, double current) {
			this.tetID = tetID;
			this.current = current;
		}

		/**
		 * Gets the current value assigned to tetID
		 *
		 * @return Returns the tetID.
		 */
		public int getTetID() {
			return tetID;
		}

		/**
		 * Gets the current value assigned to current
		 *
		 * @return Returns the current.
		 */
		public double getCurrent() {
			return current;
		}

		/**
		 * Ordering is by the magnitude (i.e., absolute value) of the current.
		 *
		 * @param other
		 * @return
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(TetCurrentPair other) {
			final double otherAbsCurrent = Math.abs(other.getCurrent());
			final double thisAbsCurrent = Math.abs(current);
			if (thisAbsCurrent < otherAbsCurrent)
				return -1;
			else if (thisAbsCurrent > otherAbsCurrent)
				return 1;
			else
				return 0;
		}
	}

	/**
	 * <p>
	 * A class for designating faces of a tet and specifying its hash. The face is
	 * specified by a pair of numbers, left and right, representing the ID numbers
	 * of the Tets that share the face.
	 * </p>
	 *
	 * @author John Villarrubia
	 * @version 1.0
	 */
	private class TetFace {
		private final int leftTet;
		private final int rightTet;

		/**
		 * Constructs a TetFace
		 *
		 * @param leftTet
		 * @param rightTet
		 */
		public TetFace(int leftTet, int rightTet) {
			this.leftTet = leftTet;
			this.rightTet = rightTet;
		}

		/**
		 * @return
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = (prime * result) + getOuterType().hashCode();
			result = (prime * result) + leftTet;
			result = (prime * result) + rightTet;
			return result;
		}

		/**
		 * Gets the current value assigned to leftTet
		 *
		 * @return Returns the leftTet.
		 */
		public int getLeftTet() {
			return leftTet;
		}

		/**
		 * Gets the current value assigned to rightTet
		 *
		 * @return Returns the rightTet.
		 */
		public int getRightTet() {
			return rightTet;
		}

		/**
		 * @param obj
		 * @return
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final TetFace other = (TetFace) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (leftTet != other.leftTet)
				return false;
			if (rightTet != other.rightTet)
				return false;
			return true;
		}

		private ThresholdConductionModel2 getOuterType() {
			return ThresholdConductionModel2.this;
		}

	}

	private class ChargeFlowDebugInfo {
		private final Mesh mesh;
		@SuppressWarnings("unused")
		private final int maxIterations;
		private final int[] tetCount;
		private final int[] attemptedChargeFlowCount;
		private final int[] successfulChargeFlowCount;
		private final double[] maxCurrent;
		private int nextTetCount = 0;
		private int nextAttemptedChargeFlowCount = 0;
		private int nextSuccessfulChargeFlowCount = 0;
		private int nextMaxCurrent = 0;
		private HashMap<Integer, ArrayList<Double>> trackedNodes;
		private HashMap<Integer, ArrayList<Integer>> trackedElements;
		private final HashMap<Integer, ArrayList<Double[]>> trackedElementFields;

		/**
		 * Constructs a chargeFlowDebugInfo
		 *
		 * @param maxIterations
		 */
		private ChargeFlowDebugInfo(Mesh mesh, int maxIterations) {
			this.mesh = mesh;
			this.maxIterations = maxIterations;
			tetCount = new int[maxIterations];
			attemptedChargeFlowCount = new int[maxIterations];
			successfulChargeFlowCount = new int[maxIterations];
			maxCurrent = new double[maxIterations];
			trackedNodes = new HashMap<Integer, ArrayList<Double>>();
			trackedElements = new HashMap<Integer, ArrayList<Integer>>();
			trackedElementFields = new HashMap<Integer, ArrayList<Double[]>>();
		}

		/**
		 * Gets the current value assigned to tetCount
		 *
		 * @return Returns the tetCount.
		 */
		@SuppressWarnings("unused")
		public int[] getTetCount() {
			return tetCount;
		}

		/**
		 * Sets the value assigned to tetCount.
		 *
		 * @param tetCount The value to which to set tetCount.
		 */
		@SuppressWarnings("unused")
		public void setTetCount(int i, int tetCount) {
			this.tetCount[i] = tetCount;
		}

		/**
		 * Appends new value assigned to tetCount.
		 *
		 * @param tetCount The value to which to set tetCount.
		 */
		public void addTetCount(int tetCount) {
			this.tetCount[nextTetCount] = tetCount;
			nextTetCount++;
		}

		/**
		 * Gets the current value assigned to attemptedChargeFlowCount
		 *
		 * @return Returns the attemptedChargeFlowCount.
		 */
		@SuppressWarnings("unused")
		public int[] getAttemptedChargeFlowCount() {
			return attemptedChargeFlowCount;
		}

		/**
		 * Sets the value assigned to attemptedChargeFlowCount.
		 *
		 * @param attemptedChargeFlowCount The value to which to set
		 *                                 attemptedChargeFlowCount.
		 */
		@SuppressWarnings("unused")
		public void setAttemptedChargeFlowCount(int i, int attemptedChargeFlowCount) {
			this.attemptedChargeFlowCount[i] = attemptedChargeFlowCount;
		}

		/**
		 * Appends new value assigned to attemptedChargeFlowCount.
		 *
		 * @param attemptedChargeFlowCount The value to which to set
		 *                                 attemptedChargeFlowCount.
		 */
		public void addAttemptedChargeFlowCount(int attemptedChargeFlowCount) {
			this.attemptedChargeFlowCount[nextAttemptedChargeFlowCount] = attemptedChargeFlowCount;
			nextAttemptedChargeFlowCount++;
		}

		/**
		 * Gets the current value assigned to successfulChargeFLowCount
		 *
		 * @return Returns the successfulChargeFLowCount.
		 */
		@SuppressWarnings("unused")
		public int[] getSuccessfulChargeFLowCount() {
			return successfulChargeFlowCount;
		}

		/**
		 * Sets the value assigned to successfulChargeFLowCount.
		 *
		 * @param successfulChargeFLowCount The value to which to set
		 *                                  successfulChargeFLowCount.
		 */
		@SuppressWarnings("unused")
		public void setSuccessfulChargeFlowCount(int i, int successfulChargeFlowCount) {
			this.successfulChargeFlowCount[i] = successfulChargeFlowCount;

		}

		/**
		 * Appends new value assigned to successfulChargeFLowCount.
		 *
		 * @param successfulChargeFLowCount The value to which to set
		 *                                  successfulChargeFLowCount.
		 */
		public void addSuccessfulChargeFlowCount(int successfulChargeFlowCount) {
			this.successfulChargeFlowCount[nextSuccessfulChargeFlowCount] = successfulChargeFlowCount;
			nextSuccessfulChargeFlowCount++;
		}

		/**
		 * Gets the current value assigned to maxCurrent
		 *
		 * @return Returns the maxCurrent.
		 */
		@SuppressWarnings("unused")
		public double[] getMaxCurrent() {
			return maxCurrent;
		}

		/**
		 * Sets the value assigned to maxCurrent.
		 *
		 * @param maxCurrent The value to which to set maxCurrent.
		 */
		@SuppressWarnings("unused")
		public void setMaxCurrent(int i, double maxCurrent) {
			this.maxCurrent[i] = maxCurrent;
		}

		/**
		 * Appends new value assigned to maxCurrent.
		 *
		 * @param maxCurrent The value to which to set maxCurrent.
		 */
		public void addMaxCurrent(double maxCurrent) {
			this.maxCurrent[nextMaxCurrent] = maxCurrent;
			nextMaxCurrent++;
		}

		/**
		 * Empties the tracked nodes and tracked elements.
		 */
		@SuppressWarnings("unused")
		public void resetTracked() {
			trackedNodes = new HashMap<Integer, ArrayList<Double>>();
			trackedElements = new HashMap<Integer, ArrayList<Integer>>();
		}

		/**
		 * Gets the current value assigned to trackedNodes
		 *
		 * @return Returns the trackedNodes.
		 */
		public HashMap<Integer, ArrayList<Double>> getTrackedNodes() {
			return trackedNodes;
		}

		/**
		 * Adds nodes to trackedNodes. Records the potential at the node at the time
		 * it's added. Ignores the addition if the node is already tracked.
		 *
		 * @param nodeNum - The index of the node to track
		 */
		public void addTrackedNode(Integer nodeNum) {
			if (!trackedNodes.containsKey(nodeNum) && (nodeNum > 0)) {
				final ArrayList<Double> potentials = new ArrayList<Double>();
				potentials.add(mesh.getNodePotential(nodeNum));
				trackedNodes.put(nodeNum, potentials);
			}
		}

		/**
		 * Gets the current value assigned to trackedElements
		 *
		 * @return Returns the trackedElements.
		 */
		public HashMap<Integer, ArrayList<Integer>> getTrackedElements() {
			return trackedElements;
		}

		/**
		 * Gets the current value assigned to trackedElementFields
		 *
		 * @return Returns the trackedElementFields.
		 */
		public HashMap<Integer, ArrayList<Double[]>> getTrackedElementFields() {
			return trackedElementFields;
		}

		/**
		 * Adds an element to trackedElements with initial charge q0. Ignores the
		 * addition if the element is already tracked. Automatically adds this element's
		 * nodes to trackedNodes.
		 *
		 * @param elementNum - The index of the new element to track.
		 * @param q0         - The initial charge to record for this element.
		 */
		public void addTrackedElement(Integer elementNum, Integer q0) {
			if (!trackedElements.containsKey(elementNum)) {
				final ArrayList<Integer> qvalues = new ArrayList<Integer>();
				qvalues.add(q0);
				trackedElements.put(elementNum, qvalues);
				final ArrayList<Double[]> efields = new ArrayList<Double[]>();
				final Tetrahedron tet = Tetrahedron.getTetrahedron(mesh, elementNum);
				tet.updatePotentials();
				final double[] temp = tet.getEField();
				final Double[] efield = new Double[] { temp[0], temp[1], temp[2] };
				efields.add(efield);
				trackedElementFields.put(elementNum, efields);
				final int[] nodeList = mesh.getNodeIndices(elementNum);
				for (final int n : nodeList)
					addTrackedNode(n);
			}
		}

		/**
		 * Takes a snapshot of tracked nodes and elements. A snapshot means the current
		 * potential is appended to the list of potentials (initial and previous) for
		 * each node. The current charge is likewise appended to the list of charges for
		 * each element.
		 */
		public void trackedSnapShot() {
			final Set<Map.Entry<Integer, ArrayList<Integer>>> elements = trackedElements.entrySet();
			for (final Map.Entry<Integer, ArrayList<Integer>> e : elements)
				e.getValue().add(mesh.getChargeNumber(e.getKey()));
			final Set<Map.Entry<Integer, ArrayList<Double[]>>> fields = trackedElementFields.entrySet();
			for (final Map.Entry<Integer, ArrayList<Double[]>> e : fields) {
				final Tetrahedron tet = Tetrahedron.getTetrahedron(mesh, e.getKey());
				tet.updatePotentials();
				final double[] temp = tet.getEField();
				final Double[] efield = new Double[] { temp[0], temp[1], temp[2] };
				e.getValue().add(efield);
			}

			final Set<Map.Entry<Integer, ArrayList<Double>>> nodes = trackedNodes.entrySet();
			for (final Map.Entry<Integer, ArrayList<Double>> n : nodes)
				n.getValue().add(mesh.getNodePotential((n.getKey())));
		}

	}
}
