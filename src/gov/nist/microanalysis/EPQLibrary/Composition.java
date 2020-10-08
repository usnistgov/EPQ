package gov.nist.microanalysis.EPQLibrary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * Composition represents the composition of substance (w/o density). The class
 * is designed to handle mass fractions that do not sum exactly to one (ie. the
 * non-normalized results of a electron probe quantification).
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
public class Composition implements Comparable<Composition>, Cloneable, Serializable {

	private static final long serialVersionUID = 0x42;

	public enum Representation {
		UNDETERMINED, WEIGHT_PCT, STOICIOMETRY
	}

	private Map<Element, UncertainValue2> mConstituents = new TreeMap<Element, UncertainValue2>();
	private Map<Element, UncertainValue2> mConstituentsAtomic = new TreeMap<Element, UncertainValue2>();
	// Normalize constituent fractions to a sum of 1.0
	private double mNormalization = 1.0;
	private String mName;
	private Representation mOptimalRepresentation = Representation.UNDETERMINED;

	transient protected int mHashCode = Integer.MAX_VALUE;

	protected void renormalize() {
		if (mConstituents.size() > 0) {
			mNormalization = 0.0;
			for (final UncertainValue2 uv : mConstituents.values())
				if (uv.doubleValue() > 0.0)
					mNormalization += uv.doubleValue();
		} else {
			mNormalization = 1.0;
		}
	}

	/**
	 * Material - Constructs a Material that is equivalent to a pure vacuum.
	 */
	public Composition() {
		renormalize();
	}

	public Composition(Composition comp) {
		super();
		replicate(comp);
	}

	public static final Composition positiveDefinite(Composition comp) {
		final Composition res = new Composition();
		for (final Element elm : comp.getElementSet())
			if (comp.weightFraction(elm, false) > 0.0)
				res.addElement(elm, comp.weightFractionU(elm, false));
		res.setName(comp.getName());
		return res;
	}

	/**
	 * To ensure that serialization happens correctly.
	 * 
	 * @return this
	 */
	private Object readResolve() {
		mHashCode = Integer.MAX_VALUE;
		renormalize();
		return this;
	}

	/**
	 * Constructs a Composition with the specified elements in the specified mass
	 * fractions.
	 * 
	 * @param elms      Element[] - The elements
	 * @param massFracs double[] - The associated mass fractions
	 */
	public Composition(Element[] elms, double[] massFracs) {
		assert (elms.length == massFracs.length);
		for (int i = 0; i < elms.length; ++i)
			mConstituents.put(elms[i], new UncertainValue2(massFracs[i]));
		recomputeStoiciometry();
		renormalize();
	}

	/**
	 * Constructs a Composition object for a pure element.
	 * 
	 * @param elm Element
	 */
	public Composition(Element elm) {
		mConstituents.put(elm, UncertainValue2.ONE);
		recomputeStoiciometry();
		renormalize();
	}

	/**
	 * Constructs a Composition with the specified mass fractions. If the length of
	 * weighFracs is one less than the mass of elms then the last mass fraction is
	 * calculated from the others assuming that the sum is 1.0.
	 * 
	 * @param elms      Element[] - The elements
	 * @param massFracs double[] - The associated mass fractions
	 * @param name      String - User friendly
	 */
	public Composition(Element[] elms, double[] massFracs, String name) {
		if (massFracs.length == (elms.length - 1)) {
			final double[] wf = new double[elms.length];
			double sum = 0.0;
			for (int i = 0; i < massFracs.length; ++i) {
				sum += massFracs[i];
				wf[i] = massFracs[i];
			}
			assert (sum < 1.0);
			wf[elms.length - 1] = 1.0 - sum;
			massFracs = wf;
		}
		assert (elms.length == massFracs.length);
		for (int i = 0; i < elms.length; ++i) {
			if (massFracs[i] < 0.0)
				throw new EPQFatalException("A mass fraction was less than zero while defining the material " + name);
			mConstituents.put(elms[i], new UncertainValue2(massFracs[i]));
		}
		mName = name;
		recomputeStoiciometry();
		renormalize();
	}

	/**
	 * getElementSet - Returns a Set containing the Element objects that make up
	 * this Material. Some of the elements in the set may have mass fractions equal
	 * to zero if the uncertainty is non-zero.
	 * 
	 * @return Set&lt;Element&gt; - A set containing Element objects
	 */
	public Set<Element> getElementSet() {
		return Collections.unmodifiableSet(mConstituents.keySet());
	}

	/**
	 * Returns a set of elements for which the mass fraction is greater than min and
	 * less than max. This is useful because getElementSet() will return elements
	 * for which the mass fraction is zero if the uncertainty is non-zero.
	 *
	 * @param min
	 * @param max
	 * @return Set&lt;Element&gt;
	 */
	public Set<Element> getElementSet(double min, double max) {
		TreeSet<Element> res = new TreeSet<Element>();
		for (final Element elm : mConstituents.keySet()) {
			final double wf = weightFraction(elm, false);
			if ((wf > min) && (wf < max))
				res.add(elm);
		}
		return res;
	}

	/**
	 * Returns a list of the elements in the material sorted by relative amount
	 * (weight fraction).
	 * 
	 * @return A sorted list of Element objects
	 */
	public List<Element> getSortedElements() {
		final ArrayList<Element> res = new ArrayList<Element>(mConstituents.keySet());
		Collections.sort(res, new Comparator<Element>() {
			@Override
			public int compare(Element o1, Element o2) {
				return -Double.compare(weightFraction(o1, false), weightFraction(o2, false));
			}
		});
		return Collections.unmodifiableList(res);
	}

	/**
	 * getElementCount - Returns the number of elements in this material.
	 * 
	 * @return int
	 */
	public int getElementCount() {
		return mConstituents.size();
	}

	/**
	 * addElement - Add a specified mass fraction of the specified element to the
	 * Material.
	 * 
	 * @param atomicNo int
	 * @param massFrac double
	 */
	public void addElement(int atomicNo, double massFrac) {
		addElement(Element.byAtomicNumber(atomicNo), massFrac);
	}

	/**
	 * addElement - Add a specified mass fraction of the specified element to the
	 * Material.
	 * 
	 * @param atomicNo int
	 * @param massFrac double
	 */
	public void addElement(int atomicNo, UncertainValue2 massFrac) {
		addElement(Element.byAtomicNumber(atomicNo), massFrac);
	}

	/**
	 * Add a specified mass fraction of the specified element to the Material.
	 * 
	 * @param elm      Element
	 * @param massFrac double
	 */
	public void addElement(Element elm, double massFrac) {
		addElement(elm, new UncertainValue2(massFrac));
	}

	/**
	 * addElement - Add a specified mass fraction of the specified element to the
	 * Material.
	 * 
	 * @param elm      Element
	 * @param massFrac double
	 */
	public void addElement(Element elm, Number massFrac) {
		mConstituents.put(elm, UncertainValue2.asUncertainValue2(massFrac));
		recomputeStoiciometry();
		renormalize();
	}

	/**
	 * addElementUsingMoleFractions - Add a specified mole fraction of the specified
	 * element to the material
	 * 
	 * @param elm      Element
	 * @param moleFrac double
	 */
	public void addElementByStoiciometry(Element elm, UncertainValue2 moleFrac) {
		mConstituentsAtomic.put(elm, moleFrac);
		recomputeWeightFractions();
		renormalize();
	}

	/**
	 * addElementUsingMoleFractions - Add a specified mole fraction of the specified
	 * element to the material
	 * 
	 * @param elm      Element
	 * @param moleFrac double
	 */
	public void addElementByStoiciometry(Element elm, double moleFrac) {
		addElementByStoiciometry(elm, new UncertainValue2(moleFrac));
	}

	/**
	 * defineByWeightFraction - Define the material composition by weigh fraction
	 * from the array of atomic numbers and mass fractions.
	 * 
	 * @param elms     Element[]
	 * @param wgtFracs double[]
	 */
	public void defineByWeightFraction(Element[] elms, double[] wgtFracs) {
		clear();
		assert (elms.length == wgtFracs.length);
		for (int i = 0; i < elms.length; ++i)
			mConstituents.put(elms[i], new UncertainValue2(wgtFracs[i]));
		recomputeStoiciometry();
		renormalize();
	}

	/**
	 * defineByWeightFraction - Define the material composition by weigh fraction
	 * from the array of atomic numbers and mass fractions.
	 * 
	 * @param elms     Element[]
	 * @param wgtFracs double[]
	 */
	public void defineByWeightFraction(Element[] elms, UncertainValue2[] wgtFracs) {
		clear();
		assert (elms.length == wgtFracs.length);
		for (int i = 0; i < elms.length; ++i)
			mConstituents.put(elms[i], wgtFracs[i]);
		recomputeStoiciometry();
		renormalize();
	}

	/**
	 * defineByWeightFraction - Define the composition of this material by weight
	 * fraction. The map argument contains a map where the key may be an Integer
	 * containing the atomic number, a String containing the abbreviation or full
	 * name of the element or an Element object. The value is the mass fraction as a
	 * Double.
	 * 
	 * @param map Map - keys are either Integer, String or Element types, values are
	 *            Double
	 */
	public void defineByWeightFraction(Map<Object, Double> map) {
		for (final Map.Entry<Object, Double> me : map.entrySet()) {
			final double wp = me.getValue().doubleValue();
			final Object key = me.getKey();
			Element elm = null;
			if (key instanceof Integer)
				elm = Element.byAtomicNumber(((Integer) key).intValue());
			else if (key instanceof String)
				elm = Element.byName((String) key);
			else if (key instanceof Element)
				elm = (Element) key;
			assert (elm != null);
			assert (elm.getAtomicNumber() != Element.elmNone);
			if ((elm != null) && (elm.getAtomicNumber() == Element.elmNone))
				mConstituents.put(elm, new UncertainValue2(wp));
		}
		recomputeStoiciometry();
		renormalize();
	}

	/**
	 * defineByMoleFraction - Define a material by the mole fraction of the
	 * constituent elements. If a Material has N atoms in a volume V and the mole
	 * fraction of element i is m_i then there are N*m_i atoms of element i in the
	 * volume V.
	 * 
	 * @param elms      Element[]
	 * @param moleFracs double[]
	 */
	public void defineByMoleFraction(Element[] elms, double[] moleFracs) {
		clear();
		assert (elms.length == moleFracs.length);
		moleFracs = Math2.divide(moleFracs, Math2.sum(moleFracs));
		for (int i = 0; i < moleFracs.length; ++i)
			mConstituentsAtomic.put(elms[i], new UncertainValue2(moleFracs[i]));
		recomputeWeightFractions();
		renormalize();
	}

	private void recomputeWeightFractions() {
		UncertainValue2 totalWgt = UncertainValue2.ZERO;
		for (final Element elm : mConstituentsAtomic.keySet())
			totalWgt = UncertainValue2.add(totalWgt,
					UncertainValue2.multiply(elm.getAtomicWeight(), atomicPercentU(elm)));
		mConstituents.clear();
		for (final Element elm : mConstituentsAtomic.keySet()) {
			final UncertainValue2 wgtFrac = UncertainValue2.multiply(elm.getAtomicWeight(),
					UncertainValue2.divide(atomicPercentU(elm), totalWgt));
			mConstituents.put(elm, wgtFrac);
		}
		mOptimalRepresentation = Representation.STOICIOMETRY;
	}

	private void recomputeStoiciometry() {
		double norm = 0.0;
		mConstituentsAtomic.clear();
		for (final Map.Entry<Element, UncertainValue2> me : mConstituents.entrySet()) {
			final Element elm = me.getKey();
			final UncertainValue2 mf = me.getValue().reduced(elm.toAbbrev());
			final UncertainValue2 moleFrac = UncertainValue2.multiply(1.0 / elm.getAtomicWeight(), mf);
			norm += moleFrac.doubleValue();
			mConstituentsAtomic.put(elm, moleFrac);
		}
		for (Element elm : mConstituentsAtomic.keySet())
			mConstituentsAtomic.put(elm, UncertainValue2.multiply(1.0 / norm, mConstituentsAtomic.get(elm)));
		mOptimalRepresentation = Representation.WEIGHT_PCT;
	}

	/**
	 * Force the optimal representation into the specified style.
	 * 
	 * @param opt
	 */
	public void setOptimalRepresentation(Representation opt) {
		switch (opt) {
		case UNDETERMINED:
			break;
		case WEIGHT_PCT:
			recomputeStoiciometry();
			break;
		case STOICIOMETRY:
			recomputeWeightFractions();
			break;
		}
	}

	/**
	 * elementSet - Get the set of all elements in specified array of Materials.
	 * 
	 * @param compositions Composition[]
	 * @return Set
	 */
	static public Set<Element> elementSet(Composition[] compositions) {
		final Set<Element> res = new TreeSet<Element>();
		for (final Composition composition : compositions)
			if (composition != null)
				res.addAll(composition.getElementSet());
		return Collections.unmodifiableSet(res);
	}

	/**
	 * massAbsorptionCoefficient - Calculates the mass absorption coefficient for
	 * this material at the specified energy.
	 * 
	 * @param energy double - In Joules
	 * @return double - Absorption per unit length (meter)
	 */
	@Deprecated
	public double massAbsorptionCoefficient(double energy) {
		return AlgorithmUser.getDefaultMAC().compute(this, energy);
	}

	/**
	 * defineByMaterialFraction - Define a new material out of certain fractions (by
	 * weight) of other materials. For example, you may construct Material to
	 * represent K3189 as 40% of SiO2, 14% of Al2O3, 14% of CaO, 10% of MgO, 2% of
	 * TiO2 and 20% of Fe2O3.
	 * 
	 * @param compositions Material[] - The base materials (ie SiO2, MgO,...)
	 * @param matFracs     double[] - The proportion of each
	 */
	public void defineByMaterialFraction(Composition[] compositions, double[] matFracs) {
		// Tested against the glass database K93 on 5-Sept-2006 - Worked fine!
		assert compositions.length == matFracs.length;
		clear();
		final Set<Element> elms = elementSet(compositions);
		final Element[] newElms = new Element[elms.size()];
		final UncertainValue2[] frac = new UncertainValue2[elms.size()];
		int ji = 0;
		for (final Element el : elms) {
			UncertainValue2 sum = UncertainValue2.ZERO;
			for (int i = 0; i < compositions.length; ++i)
				sum = UncertainValue2.add(sum,
						UncertainValue2.multiply(matFracs[i], compositions[i].weightFractionU(el, true)));
			frac[ji] = sum;
			newElms[ji] = el;
			++ji;
		}
		defineByWeightFraction(newElms, frac);
	}

	/**
	 * removeElement - Entirely remove the specified element from the Material.
	 * 
	 * @param el Element
	 */
	public void removeElement(Element el) {
		if (mConstituents.containsKey(el)) {
			mConstituents.remove(el);
			mConstituentsAtomic.remove(el);
			// Don't recomputeStoiciometry or recomputeWeightFractions
			renormalize();
		}
	}

	/**
	 * containsElement - Determines whether the Material contains a greater than
	 * zero proportion of the specified Element.
	 * 
	 * @param el Element
	 * @return boolean
	 */
	public boolean containsElement(Element el) {
		return mConstituents.containsKey(el) && (mConstituents.get(el).doubleValue() > 0.0);
	}

	/**
	 * Does this Composition contain all these elements.
	 * 
	 * @param elms
	 * @return boolean
	 */
	public boolean containsAll(Collection<Element> elms) {
		for (final Element elm : elms)
			if (!containsElement(elm))
				return false;
		return true;
	}

	/**
	 * clear - Clear all consistuent elements. Material set to pure vacuum
	 */
	protected void clear() {
		mConstituents.clear();
		mConstituentsAtomic.clear();
		mNormalization = 1.0;
	}

	/**
	 * Returns the atomic percent of the specified element. This is the number of
	 * atoms of the specified atom as a fraction of the total number of atoms (in an
	 * arbitrary volume).
	 * 
	 * @param elm Element
	 * @return double
	 */
	public double atomicPercent(Element elm) {
		return atomicPercentU(elm).doubleValue();
	}

	private static UncertainValue2 normalize(UncertainValue2 val, double norm, boolean positive) {
		UncertainValue2 res;
		if (norm > 0.0)
			res = UncertainValue2.multiply(1.0 / norm, val);
		else
			res = val;
		return positive ? UncertainValue2.nonNegative(res) : res;
	}

	/**
	 * moleFraction - Returns the moleFraction of the specified element. This is the
	 * number of atoms of the specified atom as a fraction of the total number of
	 * atoms (in an arbitrary volume).
	 * 
	 * @param elm Element
	 * @return double
	 */
	public UncertainValue2 atomicPercentU(Element elm) {
		return atomicPercentU(elm, true);
	}

	/**
	 * moleFraction - Returns the moleFraction of the specified element. This is the
	 * number of atoms of the specified atom as a fraction of the total number of
	 * atoms (in an arbitrary volume).
	 * 
	 * @param elm          Element
	 * @param positiveOnly boolean - Limit results to positive values only.
	 * @return double
	 */
	public UncertainValue2 atomicPercentU(Element elm, boolean positiveOnly) {
		final UncertainValue2 o = mConstituentsAtomic.get(elm);
		return o != null ? UncertainValue2.nonNegative(o) : UncertainValue2.ZERO;
	}

	/**
	 * Computes the mass percent of the specified element in this Material. If
	 * normalized=true then the sum of the weightPercent for all elements in the
	 * Material will equal 1.0.
	 * 
	 * @param elm        Element
	 * @param normalized boolean
	 * @return UncertainValue
	 */
	public UncertainValue2 weightFractionU(Element elm, boolean normalized) {
		return weightFractionU(elm, normalized, true);
	}

	/**
	 * Computes the mass percent of the specified element in this Material. If
	 * normalized=true then the sum of the weightPercent for all elements in the
	 * Material will equal 1.0.
	 * 
	 * @param elm          Element
	 * @param normalized   boolean
	 * @param positiveOnly boolean Limit the results to positive numbers only?
	 * @return UncertainValue
	 */
	public UncertainValue2 weightFractionU(Element elm, boolean normalized, boolean positiveOnly) {
		final UncertainValue2 d = mConstituents.get(elm);
		return d != null ? (normalized ? normalize(d, mNormalization, positiveOnly) : d) : UncertainValue2.ZERO;
	}

	/**
	 * Returns the amount of the specified element as stoichiometry when available
	 * or atomic percent otherwise.
	 * 
	 * @param elm
	 * @return UncertainValue
	 */
	public UncertainValue2 stoichiometryU(Element elm) {
		final UncertainValue2 o = mConstituentsAtomic.get(elm);
		return o != null ? o : UncertainValue2.ZERO;
	}

	/**
	 * Returns the amount of the specified element as stoichiometry when available
	 * or atomic percent otherwise.
	 * 
	 * @param elm
	 * @return double
	 */
	public double stoichiometry(Element elm) {
		return stoichiometryU(elm).doubleValue();
	}

	/**
	 * Computes the mass fraction of the specified element in this Material. If
	 * normalized=true then the sum of the weightFraction for all elements in the
	 * Material will equal 1.0.
	 * 
	 * @param elm        Element
	 * @param normalized boolean
	 * @return double
	 */
	public double weightFraction(Element elm, boolean normalized) {
		final UncertainValue2 d = mConstituents.get(elm);
		return d != null ? (normalized ? normalize(d, mNormalization, true).doubleValue() : d.doubleValue()) : 0.0;
	}

	/**
	 * Same as weightFraction.
	 * 
	 * @param elm
	 * @param normalized
	 * @return double - range 0.0 to 1.0 (assuming normalization)
	 * @deprecated
	 */
	@Deprecated
	public double weightPercent(Element elm, boolean normalized) {
		return weightFraction(elm, normalized);
	}

	/**
	 * Number of atoms of the specified element in one kilogram of material with
	 * this composition.
	 * 
	 * @param elm
	 * @param normalized - Normalize the mass fraction or not...
	 * @return A large number of atoms
	 */
	public double atomsPerKg(Element elm, boolean normalized) {
		return weightFraction(elm, normalized) / elm.getMass();
	}

	/**
	 * Number of atoms of the specified element in one kilogram of material with
	 * this composition.
	 * 
	 * @param elm
	 * @param normalized - Normalize the mass fraction or not...
	 * @return A large number of atoms
	 */
	public UncertainValue2 atomsPerKgU(Element elm, boolean normalized) {
		return UncertainValue2.multiply(1.0 / elm.getMass(), weightFractionU(elm, normalized));
	}

	/**
	 * Computes the mean atomic number for the elemental constituents comprising
	 * this Material based on their mass fraction.
	 * 
	 * @return UncertainValue
	 */
	public UncertainValue2 weightAvgAtomicNumberU() {
		UncertainValue2 res = UncertainValue2.ZERO;
		for (final Map.Entry<Element, UncertainValue2> me : mConstituents.entrySet()) {
			final Element elm = me.getKey();
			res = UncertainValue2.add(res, UncertainValue2.multiply(elm.getAtomicNumber(), weightFractionU(elm, true)));
		}
		return res;
	}

	/**
	 * Computes the mean atomic number for the elemental constituents comprising
	 * this Material based on their mass fraction.
	 * 
	 * @return double
	 */
	public double weightAvgAtomicNumber() {
		return weightAvgAtomicNumberU().doubleValue();
	}

	/**
	 * The un-normalized sum of the mass fraction of each element.
	 * 
	 * @return A number that is typically ~1.0
	 */
	public double sumWeightFraction() {
		double sum = 0.0;
		for (final UncertainValue2 uv : mConstituents.values())
			if (uv.doubleValue() > 0.0)
				sum += uv.doubleValue();
		return sum;
	}

	/**
	 * The un-normalized sum of the mass percents of each element.
	 * 
	 * @return A number that is typically ~1.0
	 */
	@Deprecated
	public UncertainValue2 sumWeightPercentU() {
		UncertainValue2 res = UncertainValue2.ZERO;
		for (final UncertainValue2 val : mConstituents.values())
			if (val.doubleValue() > 0.0)
				res = UncertainValue2.add(res, val);
		return res;
	}

	/**
	 * The un-normalized sum of the mass fractions of each element.
	 * 
	 * @return A number that is typically ~1.0
	 */
	public UncertainValue2 sumWeightFractionU() {
		UncertainValue2 res = UncertainValue2.ZERO;
		for (final Map.Entry<Element, UncertainValue2> me : mConstituents.entrySet()) {
			final Element elm = me.getKey();
			final UncertainValue2 val = me.getValue().reduced("C[" + elm.toAbbrev() + "]");
			if (val.doubleValue() > 0.0)
				res = UncertainValue2.add(res, val);
		}
		return res;
	}

	@Override
	public String toString() {
		if ((mName == null) || (mName.length() == 0))
			if ((mConstituents.size() == 1)
					&& (weightFraction(mConstituents.keySet().iterator().next(), false) > 0.9999))
				return "Pure " + mConstituents.keySet().iterator().next().toAbbrev();
			else
				return descriptiveString(false);
		return mName;
	}

	public String stoichiometryString() {
		final StringBuffer sb = new StringBuffer();
		final NumberFormat nf = new HalfUpFormat("0.####");
		for (final Element elm : getElementSet()) {
			final UncertainValue2 d0 = atomicPercentU(elm);
			if (sb.length() > 1)
				sb.append(",");
			sb.append(elm.toAbbrev());
			sb.append("(");
			sb.append(d0.format(nf));
			sb.append(" atoms)");
		}
		return sb.toString();
	}

	public String weightPercentString(boolean normalize) {
		final StringBuffer sb = new StringBuffer();
		final NumberFormat nf = new HalfUpFormat("0.0000");
		for (final Element elm : getElementSet()) {
			final UncertainValue2 d0 = weightFractionU(elm, normalize);
			if (sb.length() > 1)
				sb.append(",");
			sb.append(elm.toAbbrev());
			sb.append("(");
			sb.append(d0.format(nf));
			sb.append(" mass frac)");
		}
		if (!normalize) {
			sb.append(",\u03A3=");
			sb.append(sumWeightPercentU().format(nf));
		}
		return sb.toString();
	}

	/**
	 * descriptiveId - A string describing this material terms of the constituent
	 * element's (normalized) mass percent and the material density.
	 * 
	 * @param normalize Normalize mass fraction to 1.0
	 * @return String
	 */
	public String descriptiveString(boolean normalize) {
		final StringBuffer sb = new StringBuffer();
		if ((mName != null) && (mName.length() > 0))
			sb.append(mName + " = ");
		sb.append("[");
		if (mOptimalRepresentation == Representation.STOICIOMETRY)
			sb.append(stoichiometryString());
		else
			sb.append(weightPercentString(normalize));
		sb.append("]");
		return sb.toString();
	}

	/**
	 * getNthElementByWeight - Returns the n-th largest constituent by mass
	 * fraction.
	 * 
	 * @param n - 0 to getElementCount()
	 * @return Element
	 */
	public Element getNthElementByWeight(int n) {
		final Map<UncertainValue2, Element> tm = new TreeMap<UncertainValue2, Element>();
		for (final Element el : mConstituents.keySet()) {
			UncertainValue2 wf = weightFractionU(el, true);
			// Add hoc mechanism to handle the case in which multiple elements are
			// present in the same weightPct.
			while (tm.containsKey(wf))
				wf = UncertainValue2.add(1.0e-10 * Math.random(), wf);
			tm.put(wf, el);
		}
		int j = 0;
		for (final Map.Entry<UncertainValue2, Element> me : tm.entrySet()) {
			++j;
			if (j == (mConstituents.size() - n))
				return me.getValue();
		}
		return Element.None;
	}

	/**
	 * getNthElementByAtomicFraction - Returns the n-th largest constituent by
	 * atomic fractionm
	 * 
	 * @param n - 1 to getElementCount()
	 * @return Element
	 */
	public Element getNthElementByAtomicFraction(int n) {
		final Map<UncertainValue2, Element> tm = new TreeMap<UncertainValue2, Element>();
		for (final Element el : mConstituents.keySet()) {
			UncertainValue2 mf = atomicPercentU(el);
			// Add hoc mechanism to handle the case in which multiple elements are
			// present in the same weightPct.
			while (tm.containsKey(mf))
				mf = UncertainValue2.add(1.0e-10 * Math.random(), mf);
			tm.put(mf, el);
		}
		int j = 0;
		for (final Map.Entry<UncertainValue2, Element> me : tm.entrySet()) {
			++j;
			if (j == n)
				return me.getValue();
		}
		return Element.None;
	}

	/**
	 * setName - Provide a name for this material.
	 * 
	 * @param name String
	 */
	public void setName(String name) {
		mName = name;
	}

	/**
	 * getName - Gets either the name provided by the user (setName()) or the
	 * default name for this material if no name was specified.
	 * 
	 * @return String
	 */
	public String getName() {
		return toString();
	}

	@Override
	public int compareTo(Composition comp) {
		if (this == comp)
			return 0;
		final Iterator<Map.Entry<Element, UncertainValue2>> i = mConstituents.entrySet().iterator();
		final Iterator<Map.Entry<Element, UncertainValue2>> j = comp.mConstituents.entrySet().iterator();
		while (i.hasNext() && j.hasNext()) {
			final Map.Entry<Element, UncertainValue2> ei = i.next();
			final Map.Entry<Element, UncertainValue2> ej = j.next();
			final int zi = ei.getKey().getAtomicNumber();
			final int zj = ej.getKey().getAtomicNumber();
			if (zi < zj)
				return +1;
			else if (zi > zj)
				return -1;
			else {
				final UncertainValue2 ci = ei.getValue();
				final UncertainValue2 cj = ej.getValue();
				if (ci.lessThan(cj))
					return -1;
				else if (ci.greaterThan(cj))
					return +1;
			}
		}
		if (i.hasNext())
			return +1;
		if (j.hasNext())
			return -1;
		return 0;
	}

	public Composition asComposition() {
		if (getClass().equals(Composition.class))
			return this;
		else {
			final Composition res = new Composition();
			res.replicate(this);
			return res;
		}
	}

	protected void replicate(Composition comp) {
		mConstituents.clear();
		mConstituentsAtomic.clear();
		mConstituents.putAll(comp.mConstituents);
		mConstituentsAtomic.putAll(comp.mConstituentsAtomic);
		mHashCode = comp.mHashCode;
		mIndexHashL = comp.mIndexHashL;
		mIndexHashS = comp.mIndexHashS;
		mName = comp.mName;
		mNormalization = comp.mNormalization;
		mOptimalRepresentation = comp.mOptimalRepresentation;
	}

	/**
	 * clone
	 * 
	 * @return Composition
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Composition clone() {
		final Composition comp = new Composition();
		comp.replicate(this);
		return comp;
	}

	/**
	 * difference - Returns a measure (not necessarily the 'optimal' measure) of the
	 * difference between this Material and the argument Material.
	 * 
	 * @param comp Composition
	 * @return UncertainValue
	 */
	public UncertainValue2 differenceU(Composition comp) {
		// assert (comp.getElementCount() == this.getElementCount());
		UncertainValue2 delta = UncertainValue2.ZERO;
		final Set<Element> allElms = new TreeSet<Element>();
		allElms.addAll(getElementSet());
		allElms.addAll(comp.getElementSet());
		for (final Element el : allElms)
			delta = UncertainValue2.add(delta, UncertainValue2
					.sqr(UncertainValue2.subtract(comp.weightFractionU(el, false), weightFractionU(el, false))));
		return UncertainValue2.multiply(1.0 / allElms.size(), delta).sqrt();
	}

	/**
	 * difference - Returns a measure (not necessarily the 'optimal' measure) of the
	 * difference between this Material and the argument Material.
	 * 
	 * @param comp Composition
	 * @return UncertainValue
	 */
	public double difference(Composition comp) {
		return differenceU(comp).doubleValue();
	}

	/**
	 * getOptimalRepresentation - Returns one of STOICIOMETRY, WEIGHT_PCT or
	 * UNDETERMINED
	 * 
	 * @return STOICIOMETRY, WEIGHT_PCT or UNDETERMINED
	 */
	public Representation getOptimalRepresentation() {
		return mOptimalRepresentation;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(mConstituents);
		out.writeObject(mConstituentsAtomic);
		out.writeDouble(mNormalization);
		out.writeDouble(0.0);
		out.writeObject(mName);
		out.writeUTF(mOptimalRepresentation.name());
	}

	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		mConstituents = (Map<Element, UncertainValue2>) in.readObject();
		mConstituentsAtomic = (Map<Element, UncertainValue2>) in.readObject();
		final double v = in.readDouble();
		@SuppressWarnings("unused")
		final double u = in.readDouble();
		mNormalization = v;
		mName = (String) in.readObject();
		mOptimalRepresentation = Representation.valueOf(in.readUTF());
		renormalize();
		mHashCode = Integer.MAX_VALUE;
		mIndexHashS = Long.MAX_VALUE;
		mIndexHashL = Long.MAX_VALUE;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (mHashCode == Integer.MAX_VALUE) {
			int result = 1;
			final int PRIME = 31;
			result = (PRIME * result) + mConstituents.hashCode();
			result = (PRIME * result) + mConstituentsAtomic.hashCode();
			result = (PRIME * result) + ((mName == null) ? 0 : mName.hashCode());
			long temp;
			temp = Double.doubleToLongBits(mNormalization);
			result = (PRIME * result) + (int) (temp ^ (temp >>> 32));
			result = (PRIME * result) + ((mOptimalRepresentation == null) ? 0 : mOptimalRepresentation.hashCode());
			if (result == Integer.MAX_VALUE)
				result = Integer.MIN_VALUE;
			mHashCode = result;
		}
		return mHashCode;
	}

	/**
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
		final Composition other = (Composition) obj;
		return Objects.equals(mConstituents, other.mConstituents) && //
				Objects.equals(mConstituentsAtomic, other.mConstituentsAtomic) && //
				Objects.equals(mName, other.mName) && //
				(Double.compare(mNormalization, other.mNormalization) == 0) && //
				Objects.equals(mOptimalRepresentation, other.mOptimalRepresentation);
	}

	/**
	 * Like equals except it does not require exact equality. The compositions may
	 * differ by a small amount (tol~1.0e-4), the names may differ and the optimal
	 * representations may differ. Otherwise the compositions should be essentially
	 * equal.
	 * 
	 * @param other Composition
	 * @param tol   Nominally 1.0e-4 or similar
	 * @return true if essentially equal
	 */
	public boolean almostEquals(Composition other, double tol) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (Math.abs(mNormalization - other.mNormalization) > tol)
			return false;
		final Set<Element> allElms = new TreeSet<Element>();
		allElms.addAll(other.getElementSet());
		allElms.addAll(getElementSet());
		for (final Element elm : allElms) {
			{
				final UncertainValue2 uv1 = weightFractionU(elm, false);
				final UncertainValue2 uv2 = other.weightFractionU(elm, false);
				if ((uv1 == null) || (uv2 == null))
					return false;
				if ((Math.abs(uv1.doubleValue() - uv2.doubleValue()) > tol)
						|| (Math.abs(uv1.uncertainty() - uv2.uncertainty()) > tol))
					return false;
			}
			{
				final UncertainValue2 uv1 = this.atomicPercentU(elm);
				final UncertainValue2 uv2 = other.atomicPercentU(elm);
				if ((uv1 == null) || (uv2 == null))
					return false;
				if ((Math.abs(uv1.doubleValue() - uv2.doubleValue()) > tol)
						|| (Math.abs(uv1.uncertainty() - uv2.uncertainty()) > tol))
					return false;
			}
		}
		return true;
	}

	/**
	 * Computes the absolute error between the specified standard composition and
	 * this composition.
	 * 
	 * @param std
	 * @param normalize
	 * @return Map&lt;Element,Double&gt;
	 */
	public Map<Element, Double> absoluteError(Composition std, boolean normalize) {
		final Set<Element> elms = new TreeSet<Element>();
		elms.addAll(std.getElementSet());
		elms.addAll(getElementSet());
		final Map<Element, Double> res = new TreeMap<Element, Double>();
		for (final Element elm : elms) {
			final double u = weightFractionU(elm, normalize).doubleValue();
			final double s = std.weightFractionU(elm, normalize).doubleValue();
			res.put(elm, Double.valueOf(s != 0.0 ? (u - s) / s : (u == 0.0 ? 0.0 : 1.0)));
		}
		return res;
	}

	/**
	 * Computes the difference between the specified standard composition and this
	 * composition.
	 * 
	 * @param std
	 * @param normalize
	 * @return Map&lt;Element,Double&gt;
	 */
	public Map<Element, Double> relativeError(Composition std, boolean normalize) {
		final Set<Element> elms = new TreeSet<Element>();
		elms.addAll(std.getElementSet());
		elms.addAll(getElementSet());
		final Map<Element, Double> res = new TreeMap<Element, Double>();
		for (final Element elm : elms) {
			final double u = weightFractionU(elm, normalize).doubleValue();
			final double s = std.weightFractionU(elm, normalize).doubleValue();
			res.put(elm, Double.valueOf(u - s));
		}
		return res;
	}

	public boolean isUncertain() {
		switch (mOptimalRepresentation) {
		case WEIGHT_PCT:
		case UNDETERMINED:
			for (final UncertainValue2 v : mConstituents.values())
				if (v.isUncertain())
					return true;
			break;
		case STOICIOMETRY:
			for (final UncertainValue2 v : mConstituentsAtomic.values())
				if (v.isUncertain())
					return true;
			break;
		}
		return false;
	}

	/**
	 * Returns the mean atomic number by considering the number of atoms of each
	 * element and the atomic number of each one.
	 * 
	 * @return UncertainValue
	 */
	public UncertainValue2 meanAtomicNumberU() {
		UncertainValue2 res = UncertainValue2.ZERO;
		for (final Element elm : getElementSet())
			res = UncertainValue2.add(res,
					UncertainValue2.multiply(elm.getAtomicNumber(), weightFractionU(elm, false)));
		return res;
	}

	/**
	 * Returns the mean atomic number by considering the number of atoms of each
	 * element and the atomic number of each one.
	 * 
	 * @return double
	 */
	public double meanAtomicNumber() {
		double res = 0.0;
		for (final Element elm : getElementSet())
			res += elm.getAtomicNumber() * weightFraction(elm, false);
		return res;
	}

	public void forceNormalization() {
		final UncertainValue2 norm = sumWeightFractionU();
		final Map<Element, UncertainValue2> newConst = new TreeMap<Element, UncertainValue2>();
		for (final Map.Entry<Element, UncertainValue2> me : mConstituents.entrySet())
			newConst.put(me.getKey(),
					norm.doubleValue() > 0.0 ? UncertainValue2.divide(me.getValue(), norm) : UncertainValue2.ZERO);
		mConstituents = newConst;
		mOptimalRepresentation = Representation.WEIGHT_PCT;
		renormalize();
	}

	/**
	 * A very specialized method for parsing the strings that Danny's glass database
	 * copies to the clipboard.
	 * 
	 * @param str
	 * @return Composition
	 */
	public static Composition parseGlass(String str) {
		final String[] lines = str.split("\n");
		final Composition result = new Composition();
		int pos = 0;
		for (final String line : lines)
			if (pos == 0) {
				if (line.startsWith("NBS GLASS K "))
					result.setName("K" + line.substring(12).trim());
				else if (line.startsWith("CATIO"))
					pos = 1;
			} else if (pos == 1) {
				if (line.startsWith("AVERAGE ATOMIC NUMBER"))
					pos = 2;
				else {
					final String[] elmData = line.split("\t");
					final Element elm = Element.byName(elmData[0].substring(0, 2).trim());
					final double wgtPct = Double.parseDouble(elmData[5]);
					result.addElement(elm, wgtPct / 100.0);
				}
			} else if (pos == 2)
				if (line.startsWith("WEIGHT PERCENT OXYGEN")) {
					final String[] oData = line.split("\t");
					final double oWgtPct = Double.parseDouble(oData[1].trim());
					result.addElement(Element.O, oWgtPct / 100.0);
					break;
				}
		return result;
	}

	public String toParsableFormat() {
		final NumberFormat df = new HalfUpFormat("0.0###", new DecimalFormatSymbols(Locale.US));
		final StringBuffer sb = new StringBuffer(256);
		sb.append(toString().replace(",", " "));
		for (final Element elm : getElementSet()) {
			sb.append(",(");
			sb.append(elm.toAbbrev());
			sb.append(":");
			sb.append(df.format(100.0 * weightFraction(elm, false)));
			sb.append(")");
		}
		return sb.toString();
	}

	public Composition randomize(double offset, double proportional) {
		final Random r = new Random();
		final Composition res = new Composition();
		for (final Element elm : getElementSet()) {
			final double w = weightFraction(elm, false);
			res.addElement(elm,
					Math2.bound(w + (w * r.nextGaussian() * proportional) + (offset * r.nextGaussian()), 0.0, 1.1));
		}
		return res;
	}

	@Deprecated
	public String asHTML() {
		return toHTMLTable();
	}

	/**
	 * Summarize this material's properties in HTML
	 *
	 * @return String in HTML
	 */
	public String toHTMLTable() {
		final StringBuffer sb = new StringBuffer();
		sb.append("<table class=\"leftalign\">\n");
		sb.append("<tr><th colspan=\"4\">" + toString() + "</th></tr>\n");
		if (this instanceof Material) {
			sb.append("<tr><td>Density</td><td colspan=\"3\">");
			final NumberFormat nfd = new HalfUpFormat("0.0");
			sb.append(nfd.format(FromSI.gPerCC(((Material) this).getDensity())));
			sb.append("&nbsp;g/cm<sup>3</sup></td></tr>\n");
		}
		sb.append(
				"\t<tr><th>Element</th><th>Mass<br/>Fraction</th><th>Mass Fraction<br/>(normalized)</th><th>Atomic<br/>Fraction</th></tr>\n");
		final NumberFormat nf = new HalfUpFormat("0.0000");
		for (final Element elm : getElementSet()) {
			sb.append("\t<tr><td>");
			sb.append(elm.toAbbrev());
			sb.append("</td><td>");
			sb.append(weightFractionU(elm, false).format(nf));
			sb.append("</td><td>");
			sb.append(weightFractionU(elm, true).format(nf));
			sb.append("</td><td>");
			sb.append(atomicPercentU(elm).format(nf));
			sb.append("</td></tr>\n");
		}
		sb.append("</table>\n");
		return sb.toString();
	}

	public static void toCSV(File file, Collection<Composition> comps) throws FileNotFoundException {
		try (PrintWriter pw = new PrintWriter(file)) {
			pw.print("Name, ");
			pw.print("Density, ");
			for (int z = 1; z < 95; ++z)
				pw.print(Element.byAtomicNumber(z).toAbbrev() + ", ");
			pw.println();
			ArrayList<Composition> sorted = new ArrayList<>(comps);
			sorted.sort(Comparator.comparing(Composition::getName));
			for (Composition comp : sorted) {
				if ((comp.getName().charAt(0)!='[') && (comp.getElementCount() > 1)) {
					pw.print("\"" + comp.getName() + "\", ");
					double den = 0.0;
					if (comp instanceof Material)
						den = ((Material) comp).getDensity() / 1000.0;
					if ((den > 0.0) && (Math.abs(den-5.0) > 0.0001))
						pw.print(den + ", ");
					else
						pw.print(", ");
					for (int z = 1; z < 95; ++z) {
						double wf = comp.weightFraction(Element.byAtomicNumber(z), false);
						if(wf>0.0)
							pw.print(wf + ", ");
						else
							pw.print(", ");
					}
					pw.println();
				}
			}
		}
	}

	public Composition normalize() {
		final Composition res = new Composition();
		for (final Element elm : getElementSet())
			res.addElement(elm, weightFraction(elm, true));
		res.setName("N"+getName());
		return res;
	}

	/**
	 * Takes a map of constituent Compositions and returns the Composition which
	 * represents the constituents taken according to the fractional mass fractions
	 * in the constituents map.
	 * 
	 * @param name
	 * @param constituents
	 * @return Composition
	 */
	static public Composition combine(String name, Map<Composition, UncertainValue2> constituents) {
		final Composition res = new Composition();
		for (final Map.Entry<Composition, UncertainValue2> me : constituents.entrySet()) {
			final Composition cc = me.getKey();
			for (final Element elm : cc.getElementSet())
				res.addElement(elm, UncertainValue2.add(res.weightFractionU(elm, false),
						UncertainValue2.multiply(cc.weightFractionU(elm, false), me.getValue())));
		}
		res.setName(name);
		return res;
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
			for (final Element elm : getElementSet())
				res += Math2.bound((int) Math.sqrt(100.0 * weightFraction(elm, false)), 0, 10)
						* PROJECTORS[elm.getAtomicNumber()];
			mIndexHashS = res;
		}
		return mIndexHashS;
	}

	public long indexHashCodeL() {
		if (mIndexHashL == Long.MAX_VALUE) {
			long res = 0;
			for (final Element elm : getElementSet())
				res += Math2.bound((int) (10.0 * weightFraction(elm, false)), 0, 10)
						* PROJECTORS[elm.getAtomicNumber()];
			mIndexHashL = res;
		}
		return mIndexHashL;
	}
}