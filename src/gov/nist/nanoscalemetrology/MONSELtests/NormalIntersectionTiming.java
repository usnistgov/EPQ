package gov.nist.nanoscalemetrology.MONSELtests;

import gov.nist.nanoscalemetrology.JMONSEL.NormalComplementShape;
import gov.nist.nanoscalemetrology.JMONSEL.NormalIntersectionShape;
import gov.nist.nanoscalemetrology.JMONSEL.NormalMultiPlaneShape;
import gov.nist.nanoscalemetrology.JMONSEL.NormalShape;
import gov.nist.nanoscalemetrology.JMONSEL.NormalSphereShape;
import gov.nist.nanoscalemetrology.JMONSEL.NormalUnionShape;

public class NormalIntersectionTiming {

	public static double timetest(long repeats) {
		long t0; // Start time
		long tint, tf;
		double deltat = 0.;

		// Create a union shape for testing.
		final NormalMultiPlaneShape substrate = new NormalMultiPlaneShape();
		substrate.addPlane(new double[] { 0., 0., 1. }, new double[] { 0., 0., 0. });

		final NormalSphereShape sphere = new NormalSphereShape(new double[] { 0., 0., 0. }, 1.);

		final NormalShape inter1 = new NormalIntersectionShape(substrate, sphere);
		@SuppressWarnings("unused")
		final NormalShape inter2 = new NormalComplementShape(
				new NormalUnionShape(new NormalComplementShape(substrate), new NormalComplementShape(sphere)));

		// Repeatedly execute the routine
		@SuppressWarnings("unused")
		double[] result;
		@SuppressWarnings("unused")
		final double result1;

		double[] pos0;
		double[] pos1 = { (Math.random() * 4.) - 2., (Math.random() * 4.) - 2., (Math.random() * 4.) - 2 };
		t0 = System.currentTimeMillis();

		for (long i = 0; i < repeats; i++) {
			pos0 = pos1;
			pos1 = new double[] { (Math.random() * 4.) - 2., (Math.random() * 4.) - 2., (Math.random() * 4.) - 2 };
			result = inter1.getFirstNormal(pos0, pos1);
		}

		tint = System.currentTimeMillis();
		for (long i = 0; i < repeats; i++) {
			pos0 = pos1;
			pos1 = new double[] { (Math.random() * 4.) - 2., (Math.random() * 4.) - 2., (Math.random() * 4.) - 2 };
		}

		tf = System.currentTimeMillis();
		deltat = (1000. * ((2. * tint) - tf - t0)) / repeats;
		return deltat;

		/*
		 * double[] pos0; double[] pos1 = { Math.random() * 4. - 2., Math.random() * 4.
		 * - 2., Math.random() * 4. - 2 }; t0 = System.currentTimeMillis(); for(long i =
		 * 0; i < repeats; i++) { pos0 = pos1; pos1 = new double[] { Math.random() * 4.
		 * - 2., Math.random() * 4. - 2., Math.random() * 4. - 2 }; result =
		 * inter2.getFirstNormal(pos0, pos1); } tint = System.currentTimeMillis();
		 * for(long i = 0; i < repeats; i++) { pos0 = pos1; pos1 = new double[] {
		 * Math.random() * 4. - 2., Math.random() * 4. - 2., Math.random() * 4. - 2 }; }
		 * tf = System.currentTimeMillis(); deltat = 1000.*(2.*tint-tf-t0)/repeats;
		 * return deltat;
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
 * In a test on 8/18/2006 the NormalIntersectionShape getFirstNormal routine was
 * taking 0.63 +/-0.02 us per call.
 */
/*
 * For the same shapes forming the intersection using union and complement
 * routines takes 0.79 +/- 0.01 us. This is surprisingly close.
 */
