package gov.nist.microanalysis.EPQLibrary;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.SystemColor;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * A quantitative metric to use on particles. Similar to normalized k-ratios
 * except that one or more elements can be ingored in the normalization process.
 * Often it is convenient to eliminate O and/or C from the normalization process
 * as these numbers often vary wildly due to particle effects.
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
public class ParticleSignature {
	private final Map<Element, UncertainValue2> mValues = new TreeMap<Element, UncertainValue2>();
	private UncertainValue2 mNormalization;
	private UncertainValue2 mFullNorm;
	private final Set<Element> mStrip; // Those elements not included in the results
	private final Set<Element> mSpecial; // Those elements not included in the full normalization

	public ParticleSignature() {
		super();
		mNormalization = UncertainValue2.ONE;
		mFullNorm = UncertainValue2.ONE;
		mStrip = new TreeSet<Element>();
		mSpecial = new TreeSet<Element>();
	}

	public ParticleSignature(ParticleSignature ps) {
		super();
		mNormalization = ps.mNormalization != null ? ps.mNormalization.clone() : null;
		mFullNorm = ps.mFullNorm != null ? ps.mFullNorm.clone() : null;
		mValues.putAll(ps.mValues);
		mStrip = new TreeSet<Element>(ps.mStrip);
		mSpecial = new TreeSet<Element>(ps.mSpecial);
	}

	public void removeStrip(Element elm) {
		mStrip.remove(elm);
		renormalize();
	}

	public ParticleSignature(Collection<Element> strip, Collection<Element> special) {
		super();
		mNormalization = UncertainValue2.ONE;
		mFullNorm = UncertainValue2.ONE;
		mStrip = new TreeSet<Element>(strip);
		mSpecial = new TreeSet<Element>(special);
	}

	public ParticleSignature(KRatioSet krs, Collection<Element> strip, Collection<Element> special) {
		super();
		mNormalization = UncertainValue2.ONE;
		mFullNorm = UncertainValue2.ONE;
		mStrip = new TreeSet<Element>(strip);
		mSpecial = new TreeSet<Element>(special);
		final Set<Element> elms = krs.getElementSet();
		final KRatioSet opt = krs.optimalKRatioSet();
		for (final Element elm : elms) {
			final XRayTransitionSet xrts = opt.getTransitions(elm).iterator().next();
			add(xrts.getElement(), opt.getKRatioU(xrts));
		}
	}

	public boolean isStripped(Element elm) {
		return mStrip.contains(elm);
	}

	public void add(Element elm, UncertainValue2 value) {
		if (!value.isNaN()) {
			if (!mStrip.contains(elm)) {
				if ((value.doubleValue() < 0) && ((value.doubleValue() + value.uncertainty()) > 0.0))
					mValues.put(elm, new UncertainValue2(0.0, "PS", value.uncertainty()));
				else
					mValues.put(elm, value);
				renormalize();
			}
		}
	}

	public void add(Element elm, double value) {
		add(elm, new UncertainValue2(value));
	}

	public void remove(Element elm) {
		mValues.remove(elm);
		renormalize();
	}

	private void renormalize() {
		mNormalization = null;
		mFullNorm = null;
	}

	private UncertainValue2 getFullNorm() {
		if (mFullNorm == null) {
			mFullNorm = UncertainValue2.add(mValues.values());
			if (mFullNorm.doubleValue() <= 0.0)
				mFullNorm = UncertainValue2.ONE;
		}
		return mFullNorm;
	}

	private UncertainValue2 getNorm() {
		if (mNormalization == null) {
			mNormalization = UncertainValue2.ZERO;
			for (final Map.Entry<Element, UncertainValue2> me : mValues.entrySet())
				if (!mSpecial.contains(me.getKey()))
					mNormalization = UncertainValue2.add(mNormalization, me.getValue());
			if (mNormalization.doubleValue() <= 0.0)
				mNormalization = UncertainValue2.ONE;
		}
		return mNormalization;
	}

	private UncertainValue2 getNorm(Element elm) {
		return mSpecial.contains(elm) ? getFullNorm() : getNorm();
	}

	public UncertainValue2 getU(Element elm) {
		return UncertainValue2.divide(mValues.getOrDefault(elm, UncertainValue2.ZERO), getNorm(elm));
	}

	public double get(Element elm) {
		return getU(elm).doubleValue();
	}

	/**
	 * Get a list of all elements present in the ParticleSignature except stripped
	 * elements
	 *
	 * @return Set&lt;Element&gt;
	 */
	public Set<Element> getUnstrippedElementSet() {
		final TreeSet<Element> res = new TreeSet<Element>(mValues.keySet());
		res.removeAll(mStrip);
		return Collections.unmodifiableSet(res);
	}

	/**
	 * Get a list of elements present in the ParticleSignature.
	 *
	 * @return Set&lt;Element&gt;
	 */
	public Set<Element> getAllElements() {
		return Collections.unmodifiableSet(mValues.keySet());
	}

	@Override
	public String toString() {
		final HalfUpFormat df = new HalfUpFormat("0.0");
		final ArrayList<Element> elms = new ArrayList<Element>(mValues.keySet());
		final Comparator<Element> comp = new Comparator<Element>() {
			@Override
			public int compare(Element o1, Element o2) {
				return -Double.compare(get(o1), get(o2));
			}
		};
		Collections.sort(elms, comp);
		final StringBuffer res = new StringBuffer();
		for (final Element elm : elms) {
			if (res.length() > 0)
				res.append(",");
			res.append(elm.toAbbrev());
			res.append("=");
			res.append(UncertainValue2.multiply(100.0, getU(elm)).format(df));
		}
		return res.toString();
	}

	public String toAnnotation() {
		final HalfUpFormat df = new HalfUpFormat("0.0");
		final ArrayList<Element> elms = new ArrayList<Element>(mValues.keySet());
		final Comparator<Element> comp = new Comparator<Element>() {
			@Override
			public int compare(Element o1, Element o2) {
				return -Double.compare(get(o1), get(o2));
			}
		};
		Collections.sort(elms, comp);
		final StringBuffer res = new StringBuffer();
		for (final Element elm : elms) {
			if (get(elm) < 0.01)
				break;
			if (res.length() > 0)
				res.append("\n");
			res.append(elm.toAbbrev());
			res.append("\t");
			res.append(df.format(100.0 * get(elm)));
			res.append(" %");
		}
		return res.toString();
	}

	public Element[] getSorted(Set<Element> elms) {
		Object[][] items = new Object[elms.size()][2];
		int i = 0;
		for (Element elm : elms) {
			items[i][0] = mValues.getOrDefault(elm, UncertainValue2.ZERO);
			items[i][1] = elm;
			++i;
		}
		Arrays.sort(items, new Comparator<Object[]>() {
			@Override
			public int compare(Object[] o1, Object[] o2) {
				return -((UncertainValue2) o1[0]).compareTo((UncertainValue2) o2[0]);
			}
		});
		final Element[] res = new Element[items.length];
		i = 0;
		for (Object[] item : items) {
			if (((UncertainValue2) item[0]).doubleValue() <= 0.0)
				break;
			res[i] = (Element) item[1];
			++i;
		}
		return Arrays.copyOf(res, i);
	}

	/**
	 * Returns the n-th most prevelent element by magnitude of the particle
	 * signature.
	 *
	 * @param n - 1 to getElementSet().size()
	 * @return Element
	 */
	public Element getNthElement(int n) {
		final Element[] sorted = getSorted(getUnstrippedElementSet());
		return n < sorted.length ? sorted[n] : Element.None;
	}

	final private static int DIM = 9;
	private static final long[] PROJECTORS = createProjectors(2762689630628022905L);

	private long mIndexHashS = Long.MAX_VALUE;
	private long mIndexHashL = Long.MAX_VALUE;

	final static private long[] createProjectors(long seed) {
		final long[] proj = new long[100];
		final Random r = new Random(seed);
		final TreeSet<Long> eval = new TreeSet<Long>();
		for (int j = 0; j < proj.length; ++j) {
			long tmp;
			do {
				long mult = 1;
				tmp = 0;
				for (int i = 0; i < DIM; ++i, mult *= 10)
					tmp += r.nextInt(2) * mult;
			} while (eval.contains(Long.valueOf(tmp)));
			proj[j] = tmp;
			eval.add(Long.valueOf(tmp));
		}
		return proj;
	}

	public long indexHashCodeS() {
		if (mIndexHashS == Long.MAX_VALUE) {
			long res = 0;
			for (final Element elm : getUnstrippedElementSet())
				res += Math2.bound((int) Math.sqrt(100.0 * get(elm)), 0, 10) * PROJECTORS[elm.getAtomicNumber()];
			mIndexHashS = res;
		}
		return mIndexHashS;
	}

	public long indexHashCodeL() {
		if (mIndexHashL == Long.MAX_VALUE) {
			long res = 0;
			for (final Element elm : getUnstrippedElementSet())
				res += Math2.bound((int) (10.0 * get(elm)), 0, 10) * PROJECTORS[elm.getAtomicNumber()];
			mIndexHashL = res;
		}
		return mIndexHashL;
	}

	public int getElementCount() {
		return mValues.size();
	}

	/**
	 * Create a simple line graph from the particle signature data.
	 *
	 * @param hdim    Width and maximum height
	 * @param minVal  Minimum value to plot (nominally 0.01)
	 * @param maxElms Maximum number of elements to include
	 * @return BufferedImage of width hdim and max height hdim
	 */
	public BufferedImage createBarGraph(int hdim, double minVal, int maxElms) {
		final Color BAR_COLOR = new Color(255, 66, 14, 192);
		final BufferedImage bi = new BufferedImage(hdim, hdim, BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D gr = bi.createGraphics();
		gr.setFont(gr.getFont().deriveFont(Font.PLAIN, (12 * hdim) / 256));
		final int h = gr.getFontMetrics().getHeight();
		final int lineH = (int) (1.5 * h);
		final HalfUpFormat huf = new HalfUpFormat("0");
		int lineCx;
		Element[] sortedElms = getSorted(getUnstrippedElementSet());
		for (lineCx = 0; lineCx < Math.min(maxElms, sortedElms.length); ++lineCx) {
			final Element elm = sortedElms[lineCx];
			final double wf = get(elm);
			if (wf < minVal)
				break;
			gr.setColor(BAR_COLOR);
			gr.fillRect(0, (lineCx * lineH) + 2, (int) Math.round(wf * hdim), lineH - 4);
			gr.setColor(SystemColor.controlDkShadow);
			gr.drawRect(0, (lineCx * lineH) + 2, hdim - 1, lineH - 4);
			gr.setColor(SystemColor.windowText);
			gr.drawString(elm.toAbbrev() + " " + huf.format(100.0 * wf) + " %", hdim / 50, ((lineCx + 1) * lineH) - 8);
		}
		return lineCx * lineH < hdim ? bi.getSubimage(0, 0, hdim, Math.max(1, lineCx) * lineH) : bi;
	}

	/**
	 * Create a simple line graph of up to 6 elements from the particle signature
	 * data.
	 *
	 * @param hdim   Width and maximum height
	 * @param minVal Minimum value to plot (nominally 0.01)
	 * @return BufferedImage of width hdim and max height hdim
	 */
	public BufferedImage createBarGraph(int hdim, double minVal) {
		return createBarGraph(hdim, minVal, 6);
	}
}
