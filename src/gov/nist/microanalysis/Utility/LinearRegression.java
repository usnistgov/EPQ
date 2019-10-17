/**
 * <p>
 * A class implementing a basic LLSQ regression to a line.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas
 * @version 1.0
 */
package gov.nist.microanalysis.Utility;

import Jama.Matrix;

/**
 * Implements a class for computing the linear regression (the best fit line in
 * a least-squares sense.)
 */
public class LinearRegression {
	private double mS;
	private double mSxx;
	private double mSx;
	private double mSxy;
	private double mSy;
	private double mSyy;
	private int mCount;

	public static class Line {
		private double mSlope = Double.NaN;
		private double mIntercept = Double.NaN;

		public Line(double slope, double intercept) {
			mSlope = slope;
			mIntercept = intercept;
		}

		public Line(double x0, double y0, double x1, double y1) {
			mSlope = (y1 - y0) / (x1 - x0);
			mIntercept = y0 - mSlope * x0;
		}

		public double getXIntercept() {
			return -mIntercept / mSlope;
		}

		/**
		 * computeY - Computes Y(x) = x*getSlope() + getIntercept();
		 * 
		 * @param x
		 * @return double
		 */
		public double computeY(double x) {
			return mIntercept + mSlope * x;
		}

		/**
		 * computeX - Computes the x such that y(x) = x*getSlope() + getIntercept();
		 * 
		 * @param y double
		 * @return double
		 */
		public double computeX(double y) {
			return (y - mIntercept) / mSlope;
		}
		
		public double getSlope() {
			return mSlope;
		}
		
		public double getIntercept() {
			return mIntercept;
		}

	}

	private LazyEvaluate<Line> mLine = new LazyEvaluate<LinearRegression.Line>() {
		
		@Override
		protected Line compute() {
			double slope = Double.POSITIVE_INFINITY;
			double intercept = Double.NaN;
			final double den = (mS * mSxx) - (mSx * mSx);
			if (den != 0.0) {
				slope = ((mS * mSxy) - (mSx * mSy)) / den;
				intercept = ((mSxx * mSy) - (mSx * mSxy)) / den;
				
				mCovariance = new Matrix(2, 2);
				mCovariance.set(0, 0, mSxx / den);
				mCovariance.set(1, 1, mS / den);
				final double c = -mSx / den;
				mCovariance.set(1, 0, c);
				mCovariance.set(0, 1, c);
			}
			return new Line(slope, intercept);
		}
	};
	private Matrix mCovariance = null;

	/**
	 * Constructs an object implementing the linear regression for best fitting a
	 * line to data points (in a least-squares sense.)
	 */
	public LinearRegression() {
		super();
		clear();
	}

	/**
	 * clear - Clear the current accumulation and start a new fit.
	 */
	public void clear() {
		mS = 0.0;
		mSxx = 0.0;
		mSx = 0.0;
		mSxy = 0.0;
		mSy = 0.0;
		mSyy = 0.0;
		mCount = 0;
		mLine.reset();
	}

	/**
	 * setData - Clear the current accumulation and add two equal lengthed arrays of
	 * data points.
	 * 
	 * @param x double[]
	 * @param y double[]
	 */
	public void setData(double[] x, double[] y) {
		clear();
		addData(x, y);
	}

	/**
	 * addData - Add two equal lengthed arrays of data points to the current
	 * accumulation.
	 * 
	 * @param x double[]
	 * @param y double[]
	 */
	public void addData(double[] x, double[] y) {
		assert (x.length == y.length);
		for (int i = 0; i < x.length; ++i)
			addDatum(x[i], y[i]);
	}

	/**
	 * addDatum - Add a single data point
	 * 
	 * @param x double
	 * @param y double
	 */
	public void addDatum(double x, double y) {
		addDatum(x, y, 1.0);
	}

	/**
	 * addDatum - Add a single data point with error estimate
	 * 
	 * @param x  double
	 * @param y  double
	 * @param dy Error estimate on y
	 */
	public void addDatum(double x, double y, double dy) {
		assert dy > 0 : "The error estimate must be larger than zero!!!";
		final double dy2 = dy * dy;
		mS += 1.0 / dy2;
		mSx += x / dy2;
		mSxx += (x * x) / dy2;
		mSxy += (x * y) / dy2;
		mSy += y / dy2;
		mSyy += (y * y) / dy2;
		++mCount;
		mLine.reset();
	}

	/**
	 * removeDatum - Removes a single data point from the set of data to be fit. The
	 * values x &amp; y should have been previously added using addDatum(x,y) or as
	 * part of an setData() or addData() command otherwise the meaning of the
	 * operations performed by removeDatum(...) is not defined as expected.
	 * removeDatum(...) does not check to see that the pair x,y were previously
	 * added.
	 * 
	 * @param x  double
	 * @param y  double
	 * @param dy error estimate for y
	 */
	public void removeDatum(double x, double y, double dy) {
		final double dy2 = dy * dy;
		mS -= 1.0 / dy2;
		mSx -= x / dy2;
		mSxx -= (x * x) / dy2;
		mSxy -= (x * y) / dy2;
		mSy -= y / dy2;
		mSyy -= (y * y) / dy2;
		--mCount;
		mLine.reset();
	}

	public void removeDatum(double x, double y) {
		removeDatum(x, y, 1.0);
	}

	/**
	 * getSlope - Returns the current best estimate of the slope. y(x) =
	 * x*getSlope() + getIntercept();
	 * 
	 * @return double
	 */
	public double getSlope() {
		return mLine.get().getSlope();
	}

	/**
	 * getIntercept - Returns the current best estimate of the y-intercept. y(x) =
	 * x*getSlope() + getIntercept();
	 * 
	 * @return double
	 */
	public double getIntercept() {
		return mLine.get().getIntercept();
	}

	/**
	 * Returns the x such that y = 0.
	 * 
	 * @return double
	 */
	public double getXIntercept() {
		return mLine.get().getXIntercept();
	}

	/**
	 * computeY - Computes Y(x) = x*getSlope() + getIntercept();
	 * 
	 * @param x
	 * @return double
	 */
	public double computeY(double x) {
		return mLine.get().computeY(x);
	}

	/**
	 * computeX - Computes the x such that y(x) = x*getSlope() + getIntercept();
	 * 
	 * @param y double
	 * @return double
	 */
	public double computeX(double y) {
		return mLine.get().computeX(y);
	}

	public Matrix covariance() {
		mLine.get();
		return mCovariance;
	}

	/**
	 * Returns the correlation between the slope and intercept.
	 * 
	 * @return double
	 */
	public double correlation() {
		mLine.get();
		return -mSxx / Math.sqrt(mS * mSxx);
	}

	/**
	 * Compute the chi-squared statistic for the best fit line through the specified
	 * data points.
	 * 
	 * @return double
	 */
	public double chiSquared() {
		final double a = getIntercept(), b = getSlope();
		final double res = (mSyy - (2.0 * (((a * mSy) - (a * b * mSx)) + (b * mSxy)))) + (b * b * mSxx) + (a * a * mS);
		// assert res >= 0.0 : Double.toString(res);
		return Math.max(0.0, res);
	}

	/**
	 * Computes the goodness of fit metric Q = gammaq((N-2)/2,chi^2/2). See section
	 * 15.2 of Press et al. C 2nd
	 * 
	 * @return double
	 */
	public double goodnessOfFit() {
		return Math2.gammq(0.5 * (mCount - 2), chiSquared() / 2.0);
	}

	/**
	 * Computes the r^2 metric
	 * 
	 * @return double
	 */
	public double getRSquared() {
		return Math2.sqr((mCount * mSxy) - (mSx * mSy))
				/ (((mCount * mSxx) - (mSx * mSx)) * ((mCount * mSyy) - (mSy * mSy)));
	}

	/**
	 * Returns the number of data points used in the linear regression.
	 * 
	 * @return int
	 */
	public int getCount() {
		return mCount;
	}

	@Override
	public String toString() {
		return "Y=" + Double.toString(getSlope()) + " X + " + Double.toString(getIntercept());
	}

	public Line getResult() {
		return mLine.get();
	}
}
