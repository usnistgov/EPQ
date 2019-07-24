/**
 * gov.nist.microanalysis.EPQTests.LevenbergMarquardt2Test Created by: nicholas
 * Date: Jul 15, 2008
 */
package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.Utility.DescriptiveStatistics;
import gov.nist.microanalysis.Utility.LevenbergMarquardt2;
import gov.nist.microanalysis.Utility.LevenbergMarquardt2.FitResult;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.PoissonDeviate;
import gov.nist.microanalysis.Utility.UncertainValue2;

import Jama.Matrix;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the LevenbergMarquardt2 non-linear fitting algorithm implementation.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nicholas
 * @version 1.0
 */
public class LevenbergMarquardt2Test extends TestCase {
	private static final int NUM_ITERATIONS = 10000;
	private static final int NUM_GAUSSIANS = 2;

	private class TestFunction extends LevenbergMarquardt2.AutoPartialsFitFunction {

		private final int mNumChannels;

		TestFunction(int nCh) {
			super();
			mNumChannels = nCh;
		}

		/**
		 * @param params
		 * @return Matrix
		 * @see gov.nist.microanalysis.Utility.LevenbergMarquardt2.FitFunction#compute(Jama.Matrix)
		 */
		@Override
		public Matrix compute(Matrix params) {
			final double[] res = new double[mNumChannels];
			int idx = 0;
			final double s = params.get(idx++, 0);
			for (int i = 0; i < NUM_GAUSSIANS; ++i) {
				final double center = params.get(idx++, 0);
				final double amplitude = params.get(idx++, 0);
				for (int ch = 0; ch < res.length; ++ch)
					res[ch] += amplitude * Math.exp(-0.5 * Math2.sqr((center - ch) / s));
			}
			return Math2.createRowMatrix(res);

		}

		/**
		 * Computes the partial derivative matrix (the Jacobian) associated with the fit
		 * function and the fit parameters.
		 * 
		 * @params params A m x 1 Matrix containing the fit function parameters
		 * @returns Matrix An n x m Matrix containing the Jacobian (partials)
		 * @see gov.nist.microanalysis.Utility.LevenbergMarquardt2.FitFunction#partials(Jama.Matrix)
		 */
		@Override
		public Matrix partials(Matrix params) {
			return super.partials(params);
		}
	}

	/**
	 * Test method for
	 * {@link gov.nist.microanalysis.Utility.LevenbergMarquardt2#compute(gov.nist.microanalysis.Utility.LevenbergMarquardt2.FitFunction, Jama.Matrix, Jama.Matrix, Jama.Matrix)}
	 * .
	 * 
	 * @throws EPQException
	 */
	public void testComputeFitFunctionMatrixMatrixMatrix() throws EPQException {
		final Matrix testParams = new Matrix(1 + (2 * NUM_GAUSSIANS), 1);
		int idx = 0;
		testParams.set(idx++, 0, 10.0 * (1.0 + (0.1 * Math.random())));
		for (int j = 0; j < NUM_GAUSSIANS; ++j) {
			testParams.set(idx++, 0, (80.0 * Math.random()) + 10.0);
			testParams.set(idx++, 0, (999.0 * Math.random()) + 1.0);
		}
		final int N_CH = 100;
		final TestFunction ft = new TestFunction(N_CH);
		final Matrix noiseFree = ft.compute(testParams);
		final PoissonDeviate pd = new PoissonDeviate(System.currentTimeMillis());
		final DescriptiveStatistics[] ds = new DescriptiveStatistics[testParams.getRowDimension()];
		for (int i = 0; i < ds.length; ++i)
			ds[i] = new DescriptiveStatistics();
		for (int i = 0; i < NUM_ITERATIONS; ++i) {
			final Matrix noisy = new Matrix(noiseFree.getRowDimension(), 1);
			final Matrix err = new Matrix(noiseFree.getRowDimension(), 1);
			for (int j = 0; j < N_CH; ++j) {
				noisy.set(j, 0, pd.randomDeviate(noiseFree.get(j, 0)));
				err.set(j, 0, Math.sqrt(Math.max(1.0, noiseFree.get(j, 0))));
			}
			final Matrix initParams = new Matrix(1 + (2 * NUM_GAUSSIANS), 1);
			for (int j = 0; j < testParams.getRowDimension(); ++j)
				initParams.set(j, 0, testParams.get(j, 0) * (0.95 + (0.2 * Math.random())));
			final LevenbergMarquardt2 lm2 = new LevenbergMarquardt2();
			final FitResult fr = lm2.compute(ft, noisy, err, initParams);
			final UncertainValue2[] res = fr.getBestParametersU();
			for (int j = 0; j < res.length; ++j)
				ds[j].add(res[j].doubleValue());
		}
		System.out.println("Results:");
		for (int j = 0; j < ds.length; ++j) {
			System.out.print(ds[j].average());
			System.out.print(" -> ");
			System.out.println(testParams.get(j, 0));
		}
	}
}
