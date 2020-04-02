/**
 *
 */
package gov.nist.nanoscalemetrology.MONSELtests;

// import java.util.*;

import gov.nist.nanoscalemetrology.JMONSEL.NormalComplementShape;
import gov.nist.nanoscalemetrology.JMONSEL.NormalIntersectionShape;
import gov.nist.nanoscalemetrology.JMONSEL.NormalMultiPlaneShape;
import gov.nist.nanoscalemetrology.JMONSEL.NormalShape;
import gov.nist.nanoscalemetrology.JMONSEL.NormalSphereShape;
import gov.nist.nanoscalemetrology.JMONSEL.NormalSumShape;
import gov.nist.nanoscalemetrology.JMONSEL.NormalUnionShape;

/**
 * @author jvillar
 */
public class NormalUnionShapeTiming {

	public static double timetest(long repeats) {
		long t0; // Start time
		long tint, tf;
		double deltat = 0.;

		// Create a union shape for testing.
		final NormalMultiPlaneShape substrate = new NormalMultiPlaneShape();
		substrate.addPlane(new double[] { 0., 0., 1. }, new double[] { 0., 0., 0. });

		final NormalSphereShape sphere = new NormalSphereShape(new double[] { 0., 0., 0. }, 1.);

		final NormalUnionShape union = new NormalUnionShape(substrate, sphere);
		@SuppressWarnings("unused")
		final NormalSumShape sum = new NormalSumShape(substrate, sphere);
		@SuppressWarnings("unused")
		final NormalShape altunion = new NormalComplementShape(
				new NormalIntersectionShape(new NormalComplementShape(substrate), new NormalComplementShape(sphere)));

		// Repeatedly execute the routine
		@SuppressWarnings("unused")
		double[] result;
		@SuppressWarnings("unused")
		final double result1;

		double[] pos0;
		double[] pos1 = { (Math.random() * 4.) - 2., (Math.random() * 4.) - 2., (Math.random() * 4.) - 2. };
		t0 = System.currentTimeMillis();

		for (long i = 0; i < repeats; i++) {
			pos0 = pos1;
			pos1 = new double[] { (Math.random() * 4.) - 2., (Math.random() * 4.) - 2., (Math.random() * 4.) - 2. };
			result = union.getFirstNormal(pos0, pos1);
		}

		tint = System.currentTimeMillis();
		for (long i = 0; i < repeats; i++) {
			pos0 = pos1;
			pos1 = new double[] { (Math.random() * 4.) - 2., (Math.random() * 4.) - 2., (Math.random() * 4.) - 2. };
		}

		tf = System.currentTimeMillis();
		deltat = (1000. * ((2. * tint) - tf - t0)) / repeats;
		return deltat;

		/*
		 * double[] pos0; double[] pos1 = { Math.random() * 4. - 2., Math.random() * 4.
		 * - 2., Math.random() * 4. - 2. }; t0 = System.currentTimeMillis(); for(long i
		 * = 0; i < repeats; i++) { pos0 = pos1; pos1 = new double[] { Math.random() *
		 * 4. - 2., Math.random() * 4. - 2., Math.random() * 4. - 2. }; result =
		 * altunion.getFirstNormal(pos0, pos1); } tint = System.currentTimeMillis();
		 * for(long i = 0; i < repeats; i++) { pos0 = pos1; pos1 = new double[] {
		 * Math.random() * 4. - 2., Math.random() * 4. - 2., Math.random() * 4. - 2. };
		 * } tf = System.currentTimeMillis(); deltat = 1000.*(2.*tint-tf-t0)/repeats;
		 * return deltat;
		 */

		/*
		 * double[] pos0; double[] pos1 = { Math.random() * 4. - 2., Math.random() * 4.
		 * - 2., Math.random() * 4. - 2 }; t0 = System.currentTimeMillis(); for(long i =
		 * 0; i < repeats; i++) { pos0 = pos1; pos1 = new double[] { Math.random() * 4.
		 * - 2., Math.random() * 4. - 2., Math.random() * 4. - 2 }; result =
		 * sum.getFirstNormal(pos0, pos1); } tint = System.currentTimeMillis(); for(long
		 * i = 0; i < repeats; i++) { pos0 = pos1; pos1 = new double[] { Math.random() *
		 * 4. - 2., Math.random() * 4. - 2., Math.random() * 4. - 2 }; } tf =
		 * System.currentTimeMillis(); deltat = 1000.*(2.*tint-tf-t0)/repeats; return
		 * deltat;
		 */

		/*
		 * double[] pos0; double[] pos1 = { Math.random() * 4. - 2., Math.random() * 4.
		 * - 2., Math.random() * 4. - 2 }; t0 = System.currentTimeMillis(); for(long i =
		 * 0; i < repeats; i++) { pos0 = pos1; pos1 = new double[] { Math.random() * 4.
		 * - 2., Math.random() * 4. - 2., Math.random() * 4. - 2 }; result1 =
		 * sum.getFirstIntersection(pos0, pos1); } tint = System.currentTimeMillis();
		 * for(long i = 0; i < repeats; i++) { pos0 = pos1; pos1 = new double[] {
		 * Math.random() * 4. - 2., Math.random() * 4. - 2., Math.random() * 4. - 2 }; }
		 * // Compute the elapsed time tf = System.currentTimeMillis(); // Stop the
		 * "stopwatch" deltat = 1000.*(2.*tint-tf-t0)/repeats; //Time per op in
		 * microseconds return deltat;
		 */

		/*
		 * double[] pos0; double[] pos1 = { Math.random() * 4. - 2., Math.random() * 4.
		 * - 2., Math.random() * 4. - 2 }; t0 = System.currentTimeMillis(); for(long i =
		 * 0; i < repeats; i++) { pos0 = pos1; pos1 = new double[] { Math.random() * 4.
		 * - 2., Math.random() * 4. - 2., Math.random() * 4. - 2 };
		 * List<MonteCarloSS.Shape> shapes = union.getShapes(); NormalShape shapeA =
		 * (NormalShape) shapes.get(0); NormalShape shapeB = (NormalShape)
		 * shapes.get(1); } tint = System.currentTimeMillis(); for(long i = 0; i <
		 * repeats; i++) { pos0 = pos1; pos1 = new double[] { Math.random() * 4. - 2.,
		 * Math.random() * 4. - 2., Math.random() * 4. - 2 }; } // Compute the elapsed
		 * time tf = System.currentTimeMillis(); // Stop the "stopwatch" deltat =
		 * 1000.*(2.*tint-tf-t0)/repeats; //Time per op in microseconds return deltat;
		 */
	}

}

/*
 * In a test on 8/18/2006 the NormalUnionShape getFirstNormal routine was taking
 * 0.75+/-0.01 us per call.
 */

/*
 * For the same shapes SumShape takes 1.03 +/- 0.01 us. However, at the time I
 * tested it, SumShape was giving incorrect results. My NormalSumShape routine,
 * based on the same algorithm but debugged so its answers agree with
 * NormalUnionShape, required 1.86 +/- .01 us to solve the same problem.
 */
