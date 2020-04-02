/**
 *
 */
package gov.nist.nanoscalemetrology.MONSELtests;

// import java.util.*;

import gov.nist.nanoscalemetrology.JMONSELutils.NULagrangeInterpolation;
import gov.nist.nanoscalemetrology.JMONSELutils.ULagrangeInterpolation;

/**
 * @author jvillar
 */
public class InterpolationTiming {
	private final double[] f;
	private final double[] xsamp;
	private final double x0;
	private final double xinc;
	private final int order;

	public InterpolationTiming(double[] f, double x0, double xinc, int order) {
		super();
		this.f = f;
		this.x0 = x0;
		this.xinc = xinc;
		this.order = order;
		xsamp = new double[f.length];
		for (int i = 0; i < f.length; i++)
			xsamp[i] = x0 + (i * xinc);
	}

	public double[] interptest(double x) {
		return new double[] { ULagrangeInterpolation.d1(f, x0, xinc, order, x)[0],
				NULagrangeInterpolation.d1(f, xsamp, order, x)[0] };
	}

	public double[] timetest(long repeats) {
		long t0; // Start time
		long t1, t2, tf;
		@SuppressWarnings("unused")
		final double deltat = 0.;
		double[] interpresult = { 0., 0. };
		final double twopi = 2. * Math.PI;
		double r;

		// A dummy loop to time overhead
		t0 = System.currentTimeMillis();
		for (long i = 0; i < repeats; i++) {
			r = Math.random() * twopi;
			interpresult[0] = r;
		}

		t1 = System.currentTimeMillis();

		for (long i = 0; i < repeats; i++) {
			r = Math.random() * twopi;
			interpresult = ULagrangeInterpolation.d1(f, x0, xinc, order, r);
		}

		t2 = System.currentTimeMillis();

		for (long i = 0; i < repeats; i++) {
			r = Math.random() * twopi;
			interpresult = NULagrangeInterpolation.d1(f, xsamp, order, r);
		}

		tf = System.currentTimeMillis();

		return new double[] { t1 - t0, // Time in ms for null loop
				t2 - t1, // Time in ms for uniform interpolation
				tf - t2, // Time in ms for nonuniform interpolation
				(1000. * ((t2 + t0) - (2 * t1))) / repeats, // Net time in us for each
				// uniform evaluation
				(1000. * ((tf - t2 - t1) + t0)) / repeats, // Net time in us for each
				// nonuniform evaluation
				(1000. * ((tf - (2 * t2)) + t1)) / repeats // Time difference per
				// evaluation in us
		};

	}

}

/*
 * In a test on 8/11/2008, uniform interpolation of a sine function tabulated at
 * 1001 points from 0 to 2*PI requires 564 ns per evaluation
 * (ULagrangeInerpolation.d1) while the nonuniform algorithm
 * NULagrangeInerpolation.d1 required 610 ns. The difference is about 8%. The
 * difference will be larger for larger table size.
 */
