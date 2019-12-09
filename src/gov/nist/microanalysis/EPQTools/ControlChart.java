package gov.nist.microanalysis.EPQTools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import gov.nist.microanalysis.Utility.DescriptiveStatistics;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Histogram;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * A simple implementation of an image displaying a control chart. A control
 * chart plots time on the x-axis and a measured parameter on the y-axis. The
 * chart also shows limits which define when the property is within nominal
 * bounds.
 * 
 * @author nicholas
 */
public class ControlChart extends BufferedImage {

	private final TreeMap<Date, UncertainValue2> mData = new TreeMap<Date, UncertainValue2>();
	private final DescriptiveStatistics mStats = new DescriptiveStatistics();
	private double mUpperLimit = Double.NaN;
	private double mLowerLimit = Double.NaN;
	private final boolean mDrawStats = true;

	private final Color BACKGROUND_COLOR = new Color(0xFC, 0xFC, 0xF2);
	private final Color AXIS_COLOR = Color.darkGray;
	// private final Color MINOR_GRID_COLOR = new Color(0xFF, 0xF0, 0xF0);
	private final Color MAJOR_GRID_COLOR = new Color(0xFF, 0xE0, 0xE0);

	private NumberFormat mYFormat;
	private final DateFormat mDateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

	private transient Rectangle mCanvas;
	private Date[] mXBounds;
	private long[] mXExtent;
	private double[] mYBounds;
	private String mName;

	public ControlChart(int height, int width) {
		super(width, height, BufferedImage.TYPE_4BYTE_ABGR);
	}

	public void setName(String name) {
		mName = name;
	}

	public void addDatum(Date dt, UncertainValue2 uv) {
		mData.put(dt, uv);
		mStats.add(uv.doubleValue());
	}

	public void setControlLimits(double lower, double upper) {
		mUpperLimit = Math.max(lower, upper);
		mLowerLimit = Math.min(lower, upper);
	}

	protected double optimalStepSize(double min, double max, int labelDim, int totalDim) {
		int nLabels = totalDim / (5 * labelDim);
		if (nLabels < 2)
			nLabels = 2;
		final double nominalStep = (max - min) / nLabels;
		double trialStep = Math.pow(10.0, (int) (Math.log(nominalStep) / 2.30258509299)); // log10
		if ((5 * trialStep) < nominalStep)
			trialStep *= 5;
		else if ((2 * trialStep) < nominalStep)
			trialStep *= 2;
		return trialStep;
	}

	private String yLabel(double val) {
		if (mYFormat == null) {
			final double rr = Math.log10(Math.max(Math.abs(mYBounds[1] - mYBounds[0]), 1.0e-100));
			if ((rr > 6) || (rr < -4))
				mYFormat = new HalfUpFormat("0.00e0");
			else if (rr > 1)
				mYFormat = new HalfUpFormat("#,##0");
			else if (rr > 0)
				mYFormat = new HalfUpFormat("#,##0.0");
			else if (rr > -1)
				mYFormat = new HalfUpFormat("#,##0.00");
			else if (rr > -2)
				mYFormat = new HalfUpFormat("#,##0.000");
			else if (rr > -3)
				mYFormat = new HalfUpFormat("#,##0.0000");
			else if (rr > -4)
				mYFormat = new HalfUpFormat("#,##0.00000");
			else
				mYFormat = new HalfUpFormat("0.00e0");
		}
		return mYFormat.format(val);
	}

	private String xLabel(Date dt) {
		return mDateFormat.format(dt);
	}

	public void setRange(Date minDate, Date maxDate, double minY, double maxY) {
		final long TO_DAY = 1000L * 60L * 60L * 24L;
		final long msMinDate = TO_DAY
				* ((minDate.getTime() - ((2 * (maxDate.getTime() - minDate.getTime())) / 100)) / TO_DAY);
		final long msMaxDate = (TO_DAY
				* ((maxDate.getTime() + ((2 * (maxDate.getTime() - minDate.getTime())) / 100)) / TO_DAY)) + TO_DAY;
		mXExtent = new long[] { Math.min(msMinDate, msMaxDate), Math.max(msMinDate, msMaxDate) };
		mXBounds = new Date[] { new Date(mXExtent[0]), new Date(mXExtent[1]) };
		mYBounds = new double[] { minY, maxY };
		mYFormat = null;
		mCanvas = null;
	}

	public void autoRange() {
		UncertainValue2 minYu = Collections.min(mData.values());
		UncertainValue2 maxYu = Collections.min(mData.values());
		double delta = Math.max(0.1, Math.abs(maxYu.doubleValue() - minYu.doubleValue()));
		double avg = (maxYu.doubleValue() + minYu.doubleValue())/2.0;
		double minY = avg - delta, maxY = avg + delta;
		Histogram h = new Histogram(minY, maxY, 100);
		for (UncertainValue2 vals : mData.values())
			h.add(vals.doubleValue());
		{
			int sum = 0;
			for (int i = 0; i < h.binCount(); ++i) {
				sum += h.counts(i);
				if (sum > 0.05 * h.totalCounts()) {
					minY = h.minValue(i);
					break;
				}
			}
			minY = Math.min(mData.lastEntry().getValue().doubleValue(), minY);
		}
		{
			int sum = 0;
			for (int i = h.binCount() - 1; i >= 0; --i) {
				sum += h.counts(i);
				if (sum > 0.05 * h.totalCounts()) {
					maxY = h.maxValue(i);
					break;
				}
			}
			maxY = Math.max(mData.lastEntry().getValue().doubleValue(), maxY);
		}
		if ((!Double.isNaN(mLowerLimit)) && ((minY - minY) > mLowerLimit))
			minY = mLowerLimit;
		if ((!Double.isNaN(mUpperLimit)) && ((maxY + maxY) < mUpperLimit))
			maxY = mUpperLimit;
		final double EXTRA = 0.05;
		Date minDate = Collections.min(mData.keySet());
		Date maxDate = Collections.max(mData.keySet());
		final int ONE_DAY = 86400000;
		if ((minDate.getTime() + (3 * ONE_DAY)) > maxDate.getTime()) {
			maxDate = new Date(minDate.getTime() + ((7 * ONE_DAY) / 2));
			minDate = new Date(maxDate.getTime() - (7 * ONE_DAY));
		}
		setRange(minDate, maxDate, minY - (EXTRA * delta), maxY + (EXTRA * delta));
	}

	static final int BORDER_WIDTH = 5;

	private Rectangle getCanvas() {
		if (mCanvas == null) {
			final Graphics2D gr = createGraphics();
			final FontMetrics fm = gr.getFontMetrics();

			final Rectangle2D yMin = fm.getStringBounds(yLabel(mYBounds[0]), gr);
			final Rectangle2D yMax = fm.getStringBounds(yLabel(mYBounds[1]), gr);
			final int wider = (int) Math.round(Math.max(yMin.getWidth(), yMax.getWidth()));
			final int left = (2 * BORDER_WIDTH) + wider;
			final int width = getWidth() - left - BORDER_WIDTH;

			final Rectangle2D xMin = fm.getStringBounds(xLabel(mXBounds[0]), gr);
			final Rectangle2D xMax = fm.getStringBounds(xLabel(mXBounds[1]), gr);
			final int higher = (int) Math.round(Math.max(xMin.getHeight(), xMax.getHeight()));
			final int top = BORDER_WIDTH;
			final int height = getHeight() - top - ((2 * BORDER_WIDTH) + higher);
			mCanvas = new Rectangle(left, top, width, height);
		}
		return mCanvas;
	}

	private Point toCanvas(Date dt, double y) {
		final Rectangle canvas = getCanvas();
		final double xPos = canvas.getX()
				+ ((canvas.getWidth() * (dt.getTime() - mXExtent[0])) / (mXExtent[1] - mXExtent[0]));
		final double yPos = canvas.getMaxY() - ((canvas.getHeight() * (y - mYBounds[0])) / (mYBounds[1] - mYBounds[0]));
		return new Point((int) Math.round(xPos), (int) Math.round(yPos));
	}

	public void update() {
		final Graphics2D gr = createGraphics();
		final FontMetrics fm = gr.getFontMetrics();

		final Rectangle canvas = getCanvas();
		gr.setColor(Color.WHITE);
		gr.fillRect(0, 0, getWidth(), getHeight());
		gr.setColor(BACKGROUND_COLOR);

		gr.fillRect(canvas.x, canvas.y, canvas.width, canvas.height);
		final Rectangle res = new Rectangle();
		Rectangle2D.intersect(fm.getStringBounds(xLabel(mXBounds[0]), gr), fm.getStringBounds(xLabel(mXBounds[1]), gr),
				res);
		final Date now = new Date(System.currentTimeMillis());
		{
			final long minx = mXBounds[0].getTime();
			final long maxx = mXBounds[1].getTime();
			final double xStep = optimalStepSize(minx, maxx, res.width, canvas.width);
			for (double x = minx; x < maxx; x += xStep) {
				final Point pt = toCanvas(new Date(Math.round(x)), 0.0);
				gr.setColor(MAJOR_GRID_COLOR);
				gr.drawLine(pt.x, canvas.y, pt.x, canvas.y + canvas.height);
				gr.setColor(AXIS_COLOR);
				final String label = xLabel(new Date(Math.round(x)));
				final Rectangle2D bounds = fm.getStringBounds(label, gr);
				gr.drawString(label, (int) Math.round(pt.x - (bounds.getWidth() / 2)), getHeight() - BORDER_WIDTH);
			}
			{
				final String label = xLabel(mXBounds[1]);
				final Point pt = toCanvas(mXBounds[1], 0.0);
				final Rectangle2D bounds = fm.getStringBounds(label, gr);
				gr.drawString(label, (int) Math.round(pt.x - bounds.getWidth()), getHeight() - BORDER_WIDTH);
			}

		}
		{
			gr.setColor(AXIS_COLOR);
			final Rectangle2D rect = fm.getStringBounds(mName, gr);
			gr.drawString(mName, (int) Math.round((canvas.x + canvas.width) - (2 * BORDER_WIDTH) - rect.getWidth()),
					(int) Math.round(canvas.y + (2 * BORDER_WIDTH) + rect.getHeight()));
		}
		{
			final double minY = mYBounds[0];
			final double maxY = mYBounds[1];
			final double yStep = Math.max(optimalStepSize(minY, maxY, res.height, canvas.width), 0.01);
			final double minYSt = (1 + (int) (minY / yStep)) * yStep;
			for (double y = minYSt; y < maxY; y += yStep) {
				final Point pt = toCanvas(now, y);
				gr.setColor(MAJOR_GRID_COLOR);
				gr.drawLine(canvas.x, pt.y, canvas.x + canvas.width, pt.y);
				gr.setColor(AXIS_COLOR);
				gr.drawString(yLabel(y), BORDER_WIDTH, pt.y);
			}
		}
		gr.setColor(new Color(0, 128, 0));
		if (!Double.isNaN(mUpperLimit)) {
			final Point pt = toCanvas(now, mUpperLimit);
			gr.drawLine(canvas.x, pt.y, canvas.x + canvas.width, pt.y);
		}
		if (!Double.isNaN(mLowerLimit)) {
			final Point pt = toCanvas(now, mLowerLimit);
			gr.drawLine(canvas.x, pt.y, canvas.x + canvas.width, pt.y);
		}
		gr.setColor(AXIS_COLOR);
		gr.drawRect(canvas.x, canvas.y, canvas.width, canvas.height);
		gr.setClip(canvas.x + 1, canvas.y + 1, canvas.width - 2, canvas.height - 2);
		gr.setColor(Color.RED);
		// draw mean and std-dev
		if (mDrawStats) {
			final double avg = mStats.average();
			if (!Double.isNaN(avg)) {
				gr.setColor(Color.lightGray);
				final Stroke old = gr.getStroke();
				try {
					Point pt = toCanvas(now, avg);
					gr.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f,
							new float[] { 12.0f, 4.0f }, 0.0f));
					gr.drawLine(canvas.x, pt.y, canvas.x + canvas.width, pt.y);
					final double std = mStats.standardDeviation();
					if (!Double.isNaN(std)) {
						gr.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f,
								new float[] { 2.0f, 2.0f }, 0.0f));
						for (int i = -1; i <= 1; i += 2) {
							pt = toCanvas(now, avg + (i * std));
							gr.drawLine(canvas.x, pt.y, canvas.x + canvas.width, pt.y);
						}
					}
				} finally {
					gr.setStroke(old);
				}
			}
		}
		for (final Map.Entry<Date, UncertainValue2> me : mData.entrySet()) {
			final UncertainValue2 val = me.getValue();
			final Date date = me.getKey();
			gr.setColor(Color.darkGray);
			if (!(Double.isNaN(mLowerLimit) || Double.isNaN(mUpperLimit))) {
				final double dv = val.doubleValue();
				if ((dv < mLowerLimit) || (dv > mUpperLimit))
					gr.setColor(Color.red);
				else
					gr.setColor(Color.darkGray);
			}
			final double v = val.doubleValue();
			final int markerSize = 10;
			if (v < mYBounds[0]) {
				final Point pt = toCanvas(date, mYBounds[0]);
				gr.setColor(Color.orange);
				gr.fillPolygon(new int[] { pt.x, pt.x - markerSize / 2, pt.x + markerSize / 2 },
						new int[] { pt.y, pt.y - (2 * markerSize) / 3, pt.y - (2 * markerSize) / 3 }, 3);
			} else if (v > mYBounds[1]) {
				final Point pt = toCanvas(date, mYBounds[1]);
				gr.setColor(Color.orange);
				gr.fillPolygon(new int[] { pt.x, pt.x - markerSize / 2, pt.x + markerSize / 2 },
						new int[] { pt.y, pt.y + (2 * markerSize / 3), pt.y + (2 * markerSize) / 3 }, 3);
			} else {
				final Point pt = toCanvas(date, val.doubleValue());
				gr.fillOval(pt.x - markerSize / 2, pt.y - markerSize / 2, markerSize, markerSize);
				final Point pt0 = toCanvas(date, val.doubleValue() + val.uncertainty());
				final Point pt1 = toCanvas(date, val.doubleValue() - val.uncertainty());
				gr.drawLine(pt0.x, pt0.y, pt0.x, pt1.y);
				gr.drawLine(pt0.x - markerSize, pt0.y, pt0.x + markerSize, pt0.y);
				gr.drawLine(pt1.x - markerSize, pt1.y, pt0.x + markerSize, pt1.y);
			}
		}
	}
}
