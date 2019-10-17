package gov.nist.microanalysis.EPQLibrary;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQTools.EPQXStream;

/**
 * <p>
 * A class to faciliate constructing common Material instances.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class MaterialFactory {
	public static final String K3189 = "K3189";
	public static final String RynasAlTiAlloy = "Ryna's Al-Ti alloy";
	public static final String Mylar = "Mylar";
	public static final String VanadiumPentoxide = "Vanadium Pentoxide";
	public static final String SiliconDioxide = "Silicon Dioxide";
	public static final String Ice = "Ice";
	public static final String PerfectVacuum = "Perfect vacuum";
	public static final String CaCO3 = "Calcium carbonate";
	public static final String Al2O3 = "Alumina";
	public static final String SS316 = "Stainless Steel 316";
	public static final String UraniumOxide = "Uranium oxide";
	public static final String K227 = "K227";
	public static final String K309 = "K309";
	public static final String K411 = "K411";
	public static final String K412 = "K412";
	public static final String K961 = "K961";
	public static final String K1080 = "K1080";
	public static final String K2450 = "K2450";
	public static final String K2451 = "K2451";
	public static final String K2466 = "K2466";
	public static final String K2469 = "K2469";
	public static final String K2472 = "K2472";
	public static final String K2496 = "K2496";
	public static final String ParaleneC = "Paralene C";
	public static final String MagnesiumOxide = "Magnesium Oxide";
	public static final String Chloroapatite = "Chloroapatite";
	public static final String CalciumFluoride = "Calcium Fluoride";
	public static final String GalliumPhosphate = "Gallium Phosphate";
	public static final String Nothing = "None";

	public Map<String, Composition> mLibrary = new HashMap<String, Composition>();

	private static final String[] PBaseMaterials = { K3189, RynasAlTiAlloy, Mylar, VanadiumPentoxide, SiliconDioxide,
			Ice, CaCO3, Al2O3, SS316, UraniumOxide, K227, K309, K411, K412, K961, K1080, K2450, K2451, K2466, K2469,
			K2472, K2496, ParaleneC, MagnesiumOxide, Chloroapatite, CalciumFluoride };

	public static List<String> BaseMaterials = Collections.unmodifiableList(Arrays.asList(PBaseMaterials));

	public static final double[] mElementalDensities = { 0.0, 0.0, 0.534, 1.85, 2.53, 2.25, 0.0, 0.0, 0.0, 0.0, // H to
																												// Ne
			0.97, // Na
			1.74, 2.70, 2.42, 1.83 /* P Yellow */, 1.92, 0.0, // Cl
			0.0, // Ar
			0.86, 1.55, 3.02, 4.5, 5.98, 7.14, 7.41, 7.88, 8.71, 8.88, 8.96, // Cu
			7.1, 5.93, // Ga
			5.46, 5.73, 4.82, 0.0, // Br
			0.0, // Kr
			1.53, // Rb
			2.56, 4.47, // Y
			6.4, 8.57, 10.22, 11.5, 12.1, 12.44, 12.16, 10.49, // Ag
			8.65, 7.28, 7.3, // Sn
			6.62, 6.25, 4.94, 0.0, // Xe
			1.87, // Cs
			3.5, // Ba
			6.15, // La
			6.90, // Ce
			6.48, // Pr
			6.96, // Nd
			-1.0, // Pm
			7.75, // Sm
			5.26, 7.95, // Gd
			8.27, 8.54, // Dy
			8.80, // Ho
			9.05, // Er
			9.33, // Tm
			6.97, // Yb
			9.84, // Lu
			13.3, 16.6, 19.3, 21.0, // Re
			22.5, 22.42, 21.45, 19.3, 14.19 /* Hg solid at -39 C */, 11.86, 11.34, 9.78, 9.3, // Po
			-1.0, // At
			0.0, // Rn
			-1.0, 5.0, // Ra
			-1.0, 11.7, // Th
			15.3, 18.7, // U
			-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, // Np to Es
			-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0
			// Fm to Uub
	};

	public static boolean canCreate(Element el) {
		return mElementalDensities[el.getAtomicNumber() - 1] > 0.0;
	}

	private static TreeMap<Element, Integer> parseCompound(String str) throws EPQException {
		final TreeMap<Element, Integer> elMap = new TreeMap<Element, Integer>();
		str = str.trim();
		for (int i = 0; i < str.length(); ++i) {
			char c = str.charAt(i);
			if (c == '(') {
				int pcx = 1;
				for (int j = i + 1; j < str.length(); ++j) {
					c =str.charAt(j);
					if (c == '(')
						pcx++;
					if (c == ')')
						pcx--;
					if (pcx == 0) {
						TreeMap<Element, Integer> tmp = parseCompound(str.substring(i + 1, j));
						if (tmp.size() == 0)
							throw new EPQException(
									"There must be something between the (). " + str.substring(i + 1, j - 1));
						StringBuffer sb = new StringBuffer();
						for (int k = j + 1; k < str.length(); ++k) {
							c = str.charAt(k);
							if (Character.isDigit(c))
								sb.append(c);
							else {
								i = k;
								break;
							}
						}
						final int cx = sb.length() > 0 ? Integer.valueOf(sb.toString()) : 1;
						for (Map.Entry<Element, Integer> me : tmp.entrySet())
							elMap.put(me.getKey(), cx * me.getValue()+elMap.getOrDefault(me.getKey(), 0));
						break;
					}
				}
				if (pcx > 0)
					throw new EPQException("Unmatched parenthesis in " + str);
			}
			final StringBuffer elStr = new StringBuffer();
			if (!Character.isUpperCase(c))
				throw new EPQException("Element abbreviations must start with a capital letter.");
			elStr.append(c);
			if ((i + 1 < str.length()) && Character.isLowerCase(str.charAt(i + 1)))
				elStr.append(str.charAt(++i));
			Element elm = Element.byAbbrev(elStr.toString());
			if (elm.equals(Element.None))
				throw new EPQException("Unrecognized element: " + elStr.toString());
			final StringBuffer qtyStr = new StringBuffer();
			for (; (i + 1 < str.length()) && Character.isDigit(str.charAt(i + 1)); ++i)
				qtyStr.append(str.charAt(i + 1));
			final int cx = qtyStr.length() > 0 ? Integer.valueOf(qtyStr.toString()) : 1;
			elMap.put(elm, cx + elMap.getOrDefault(elm, 0));
		}
		// Check for K411 and other anomalies
		if (elMap.size() == 1)
			if (elMap.firstEntry().getValue().intValue() > 1)
				throw new EPQException("Unusually formated pure element in " + str);
		return elMap;
	}

	/**
	 * Create a Composition by parsing a string containing a compound in simplified
	 * chemical notation. Examples: "Al2O3", "Ca5(PO3)4Cl", "SiO2"
	 * 
	 * @param str
	 * @return Composition
	 * @throws EPQException
	 */
	public static Composition createCompound1(String str) throws EPQException {
		final TreeMap<Element, Integer> elMap = parseCompound(str);
		final Composition comp = new Composition();
		comp.setName(str);
		for (final Entry<Element, Integer> e : elMap.entrySet())
			comp.addElementByStoiciometry(e.getKey(), e.getValue().doubleValue());
		return comp;
	}

	public static Composition createCompound(String str) throws EPQException {
		{
			final int p = str.indexOf("+");
			if (p >= 0) {
				final Composition cl = createCompound(str.substring(0, p).trim());
				final Composition cr = createCompound(str.substring(p + 1).trim());
				final TreeSet<Element> elms = new TreeSet<Element>(cl.getElementSet());
				elms.addAll(cr.getElementSet());
				final Element[] elmA = elms.toArray(new Element[elms.size()]);
				final double[] massFracs = new double[elms.size()];
				for (int i = 0; i < elmA.length; ++i)
					massFracs[i] = cl.weightFraction(elmA[i], false) + cr.weightFraction(elmA[i], false);
				return new Composition(elmA, massFracs);
			}
		}
		{
			final int p = str.indexOf("*");
			if (p >= 0) {
				double k;
				try {
					k = Double.parseDouble(str.substring(0, p).trim());
				} catch (final NumberFormatException e) {
					throw new EPQException("Error parsing number: " + str.substring(0, p), e);
				}
				final Composition cr = createCompound(str.substring(p + 1).trim());
				final Set<Element> elms = cr.getElementSet();
				final Element[] elmA = elms.toArray(new Element[elms.size()]);
				final double[] massFracs = new double[elms.size()];
				for (int i = 0; i < elmA.length; ++i)
					massFracs[i] = k * cr.weightFraction(elmA[i], false);
				return new Composition(elmA, massFracs);
			}
		}
		return createCompound1(str);

	}

	/**
	 * Create a Material by parsing a string containing a compound in simplified
	 * chemical notation. Examples: "Al2O3", "Ca5(PO3)4Cl", "SiO2"
	 * 
	 * @param str
	 * @param density - In kg per cubic meter (SI)
	 * @return Composition
	 * @throws EPQException
	 */
	public static Material createCompound(String str, double density) throws EPQException {
		return new Material(createCompound(str), density);
	}

	/**
	 * createMaterial - Creates the material based on the type identified as one of
	 * the integer constants listed above.
	 * 
	 * @param name    String - One of the predefined named constants
	 * @param density double in SI
	 * @return Material
	 */
	public static Composition createMaterial(String name, double density) {
		final Composition res = createMaterial(name);
		return new Material(res, density);
	}

	/**
	 * createMaterial - Creates the material based on the type identified as one of
	 * the integer constants listed above.
	 * 
	 * @param name String - One of the predefined named constants
	 * @return Material
	 */
	public static Composition createMaterial(String name) {
		Material mat = null;
		Composition comp = null;
		try {
			if (name.equals(K3189)) {
				final Composition tmp = new Composition();
				tmp.defineByMaterialFraction(
						new Composition[] { MaterialFactory.createCompound("SiO2"),
								MaterialFactory.createCompound("Al2O3"), MaterialFactory.createCompound("CaO"),
								MaterialFactory.createCompound("MgO"), MaterialFactory.createCompound("TiO2"),
								MaterialFactory.createCompound("Fe2O3") },
						new double[] { 0.400000, 0.140000, 0.140000, 0.100000, 0.020000, 0.200000 });
				mat = new Material(tmp, ToSI.gPerCC(3.23));
				mat.setName(K3189);
			} else if (name.equals(RynasAlTiAlloy))
				return new Material(new Element[] { Element.Ti, Element.Al, Element.Nb, Element.W },
						new double[] { 0.54, 0.31, 0.11, 0.04 }, ToSI.gPerCC(8.57), RynasAlTiAlloy);
			else if (name.equals(Mylar)) {
				mat = MaterialFactory.createCompound("C10H8O4", ToSI.gPerCC(1.39));
				mat.setName(Mylar);
			} else if (name.equals(VanadiumPentoxide)) {
				mat = MaterialFactory.createCompound("V2O5", ToSI.gPerCC(3.357));
				mat.setName(VanadiumPentoxide);
			} else if (name.equals(SiliconDioxide)) {
				mat = MaterialFactory.createCompound("SiO2", ToSI.gPerCC(2.65));
				mat.setName(SiliconDioxide);
			} else if (name.equals(Ice)) {
				mat = MaterialFactory.createCompound("H2O", ToSI.gPerCC(0.917));
				mat.setName(Ice);
			} else if (name.equals(PerfectVacuum)) {
				mat = new Material(0.0);
				mat.setName(PerfectVacuum);
			} else if (name.equals(CaCO3)) {
				mat = MaterialFactory.createCompound("CaCO3", ToSI.gPerCC(2.7));
				mat.setName(CaCO3);
			} else if (name.equals(Al2O3)) {
				mat = MaterialFactory.createCompound("Al2O3", ToSI.gPerCC(3.97));
				mat.setName(Al2O3);
			} else if (name.equals(SS316))
				mat = new Material(new Element[] { Element.Fe, Element.Ni, Element.Cr, Element.Mn, Element.Si },
						new double[] { 0.50, 0.205, 0.245, 0.02, 0.03 }, ToSI.gPerCC(7.8), "SS316");
			else if (name.equals(UraniumOxide)) {
				mat = createCompound("UO2", ToSI.gPerCC(10.0));
				mat.setName(UraniumOxide);
			} else if (name.equals(K227)) {
				comp = new Composition();
				comp.defineByMaterialFraction(new Composition[] { MaterialFactory.createMaterial("SiO2"),
						MaterialFactory.createMaterial("PbO") }, new double[] { 0.20000, 0.80000, });
				comp.setName(K227);
			} else if (name.equals(K309)) {
				comp = new Composition();
				comp.defineByMaterialFraction(
						new Composition[] { MaterialFactory.createCompound("Al2O3"),
								MaterialFactory.createCompound("SiO2"), MaterialFactory.createCompound("CaO"),
								MaterialFactory.createCompound("Fe2O3"), MaterialFactory.createCompound("BaO") },
						new double[] { 0.15000, 0.40000, 0.15000, 0.15000, 0.15000, });
				comp.setName(K309);
			} else if (name.equals(K411)) {
				mat = new Material(ToSI.gPerCC(5.0));
				mat.defineByMaterialFraction(
						new Composition[] { MaterialFactory.createCompound("MgO"),
								MaterialFactory.createCompound("SiO2"), MaterialFactory.createCompound("CaO"),
								MaterialFactory.createCompound("FeO") },
						new double[] { 0.146700, 0.543000, 0.154700, 0.144200 });
				mat.setName(K411);
			} else if (name.equals(K412)) {
				comp = new Composition();
				comp.defineByMaterialFraction(
						new Composition[] { MaterialFactory.createCompound("MgO"),
								MaterialFactory.createCompound("Al2O3"), MaterialFactory.createCompound("SiO2"),
								MaterialFactory.createCompound("CaO"), MaterialFactory.createCompound("FeO") },
						new double[] { 0.193300, 0.092700, 0.453500, 0.152500, 0.099600 });
				comp.setName(K412);
			} else if (name.equals(K961))
				mat = new Material(
						new Element[] { Element.Na, Element.Mg, Element.Al, Element.Si, Element.P, Element.K,
								Element.Ca, Element.Ti, Element.Mn, Element.Fe, Element.O },
						new double[] { 0.029674, 0.030154, 0.058215, 0.299178, 0.002182, 0.024904, 0.035735, 0.011990,
								0.003160, 0.034972, 0.469837 },
						ToSI.gPerCC(6.0), K961);
			else if (name.equals(K1080))
				mat = new Material(
						new Element[] { Element.Li, Element.B, Element.Mg, Element.Al, Element.Si, Element.Ca,
								Element.Ti, Element.Sr, Element.Zr, Element.Lu, Element.O },
						new double[] { 0.027871, 0.006215, 0.008634, 0.079384, 0.186986, 0.107204, 0.011990, 0.126838,
								0.007403, 0.017588, 0.416459 },
						ToSI.gPerCC(6.0), K1080);
			else if (name.equals(K2450)) {
				comp = new Composition();
				comp.defineByMaterialFraction(
						new Composition[] { MaterialFactory.createCompound("SiO2"),
								MaterialFactory.createCompound("Al2O3"), MaterialFactory.createCompound("CaO"),
								MaterialFactory.createCompound("TiO2") },
						new double[] { 0.30000, 0.30000, 0.30000, 0.10000 });
				comp.setName(name);
			} else if (name.equals(K2451)) {
				comp = new Composition();
				comp.defineByMaterialFraction(
						new Composition[] { MaterialFactory.createCompound("SiO2"),
								MaterialFactory.createCompound("Al2O3"), MaterialFactory.createCompound("CaO"),
								MaterialFactory.createCompound("V2O5") },
						new double[] { 0.300000, 0.300000, 0.300000, 0.100000 });
				comp.setName(name);
			} else if (name.equals(K2466)) {
				comp = new Composition();
				comp.defineByMaterialFraction(
						new Composition[] { MaterialFactory.createCompound("SiO2"),
								MaterialFactory.createCompound("BaO"), MaterialFactory.createCompound("TiO2") },
						new double[] { 0.44, 0.48, 0.08 });
				comp.setName(name);
			} else if (name.equals(K2469)) {
				comp = new Composition();
				comp.defineByMaterialFraction(
						new Composition[] { MaterialFactory.createCompound("SiO2"),
								MaterialFactory.createCompound("BaO"), MaterialFactory.createCompound("TiO2") },
						new double[] { 0.36, 0.48, 0.16 });
				comp.setName(name);
			} else if (name.equals(K2472)) {
				comp = new Composition();
				comp.defineByMaterialFraction(new Composition[] { MaterialFactory.createCompound("SiO2"),
						MaterialFactory.createCompound("BaO"), MaterialFactory.createCompound("TiO2"),
						MaterialFactory.createCompound("V2O5") }, new double[] { 0.36, 0.48, 0.10, 0.06 });
				comp.setName(name);
			} else if (name.equals(K2496)) {
				comp = new Composition();
				comp.defineByMaterialFraction(
						new Composition[] { MaterialFactory.createCompound("SiO2"),
								MaterialFactory.createCompound("BaO"), MaterialFactory.createCompound("TiO2") },
						new double[] { 0.49, 0.48, 0.03 });
				comp.setName(name);
			} else if (name.equals(ParaleneC)) {
				mat = MaterialFactory.createCompound("C8H7Cl", ToSI.gPerCC(1.2));
				mat.setName(ParaleneC);
			} else if (name.equals(MagnesiumOxide)) {
				mat = MaterialFactory.createCompound("MgO", ToSI.gPerCC(3.55));
				mat.setName(MagnesiumOxide);
			} else if (name.equals(CalciumFluoride)) {
				mat = MaterialFactory.createCompound("CaF2", ToSI.gPerCC(3.18));
				mat.setName(CalciumFluoride);
			} else if (name.equals(Chloroapatite)) {
				mat = MaterialFactory.createCompound("Ca5(PO4)3Cl", ToSI.gPerCC(3.15));
				mat.setName(Chloroapatite);
			} else if (name.equals(GalliumPhosphate)) {
				mat = MaterialFactory.createCompound("GaPO4", ToSI.gPerCC(3.570));
				mat.setName(GalliumPhosphate);
			} else if (name.equals(Nothing)) {
				mat = new Material(0.0);
				mat.setName("None");
			}
		} catch (final EPQException e) {
			e.printStackTrace();
		}
		return mat != null ? mat : comp;
	}

	/**
	 * createPureElement - Create a simple material based on a pure element and a
	 * nominal density for a typical solid manifestation of this element.
	 * 
	 * @param el Element
	 * @throws EPQException
	 * @return Material
	 */
	public static Material createPureElement(Element el) throws EPQException {
		assert (mElementalDensities.length == 112);
		final double den = elementalDensity(el);
		if (den <= 0.0)
			if (den < 0.0)
				throw new EPQException("Density data unavailable for " + el.toString());
			else
				throw new EPQException("This element is a gas at STP.");
		final Material mat = new Material(den);
		mat.addElement(el.getAtomicNumber(), 1.0);
		mat.setName("Pure " + el.toString().toLowerCase());
		return mat;
	}

	/**
	 * createPureElement - Create a simple material based on a pure element and a
	 * nominal density for a typical solid manifestation of this element.
	 * 
	 * @param el Element
	 * @return {@link Composition}
	 */
	public static Composition createPureElement2(Element el) {
		try {
			return createPureElement(el);
		} catch (final EPQException e) {
			final Composition res = new Composition(el);
			res.setName(el.toAbbrev());
			return res;
		}
	}

	/**
	 * MaterialFactory - A constructor that adds the default materials.
	 */
	public MaterialFactory() {
		for (final String name : BaseMaterials)
			mLibrary.put(name, MaterialFactory.createMaterial(name));
	}

	/**
	 * MaterialFactory - Create a material factory from the XML library specified by
	 * the Reader r.
	 * 
	 * @param r Reader
	 */
	@SuppressWarnings("unchecked")
	public MaterialFactory(Reader r) {
		final EPQXStream xs = EPQXStream.getInstance();
		mLibrary = (Map<String, Composition>) xs.fromXML(r);
	}

	/**
	 * append - Append the materials in the XML library specified in the Reader R to
	 * the current library.
	 * 
	 * @param r Reader
	 */
	@SuppressWarnings("unchecked")
	public void append(Reader r) {
		final EPQXStream xs = EPQXStream.getInstance();
		mLibrary.putAll((Map<String, Composition>) xs.fromXML(r));
	}

	/**
	 * write - Write the current library to the specified Writer as an XML file.
	 * 
	 * @param wr Writer
	 */
	public void write(Writer wr) {
		final EPQXStream xs = EPQXStream.getInstance();
		xs.toXML(mLibrary, wr);
	}

	/**
	 * addMaterial - Add a material to the current library.
	 * 
	 * @param mat Material
	 */
	public void addMaterial(Material mat) {
		mLibrary.put(mat.toString(), mat);
	}

	/**
	 * estimatedDensity - Compute an estimated density for a mixture of the element
	 * specified by the Material. The assumption is that the density of the Material
	 * is the mass percent weighted average of the densities of the constituent
	 * elements. (Ok! So its just an estimate.)
	 * 
	 * @param mat Material
	 * @return double
	 */
	public static double estimatedDensity(Material mat) {
		double den = 0.0;
		for (final Element el : mat.getElementSet()) {
			double d = elementalDensity(el);
			if (d < 0.0)
				d = 1.0; // Arbitrary...
			den += mat.weightFraction(el, true) * d;
		}
		return den;
	}

	/**
	 * Returns the nominal default density (in SI units) for a common solid form of
	 * the specified element as a pure material.
	 * 
	 * @param elm
	 * @return double
	 */
	public static double elementalDensity(Element elm) {
		return ToSI.gPerCC(mElementalDensities[elm.getAtomicNumber() - 1]);
	}

	/**
	 * getMaterialNames - Get a list of the names of the available materials.
	 * 
	 * @return String[]
	 */
	public String[] getMaterialNames() {
		final Object[] s = mLibrary.keySet().toArray();
		final String[] res = new String[s.length];
		for (int i = 0; i < s.length; ++i)
			res[i] = (String) s[i];
		return res;
	}

	/**
	 * getMaterial - Get the Material specified by the given name.
	 * 
	 * @param name String
	 * @return Material
	 */
	public Material getMaterial(String name) {
		return (Material) mLibrary.get(name);
	}

	public static List<Composition> getCommonStandards(Collection<Element> elms) {
		final ArrayList<Composition> res = new ArrayList<Composition>();
		for (final Element elm : elms) {
			final Collection<Composition> tmp = getCommonStandards(elm);
			for (final Composition comp : tmp)
				if (comp.containsAll(elms))
					res.add(comp);
		}
		return Collections.unmodifiableList(res);
	}

	public static ArrayList<Composition> getCommonStandards(Element elm) {
		final ArrayList<Composition> res = new ArrayList<Composition>();
		try {
			switch (elm.getAtomicNumber()) {
			case Element.elmBe:
				res.add(createPureElement(Element.Be));
				res.add(createCompound("Be2SiO4"));
				break;
			case Element.elmB:
				res.add(createPureElement(Element.B));
				res.add(createCompound("BN"));
				res.add(createCompound("B2O3"));
				break;
			case Element.elmC:
				res.add(createPureElement(Element.C));
				res.add(createCompound("CaCO3"));
				break;
			case Element.elmN:
				res.add(createCompound("BN"));
				res.add(createCompound("Si3N4"));
				break;
			case Element.elmO:
				res.add(createCompound("MgO"));
				res.add(createCompound("Al2O3"));
				res.add(createCompound("SiO2"));
				res.add(createCompound("TiO2"));
				break;
			case Element.elmF:
				res.add(createCompound("CaF2"));
				res.add(createCompound("BaF2"));
				res.add(createCompound("LaF3"));
				break;
			case Element.elmNa:
				res.add(createCompound("NaAlSi3O8"));
				res.add(createCompound("Na2Ca(SO4)2"));
				res.add(createCompound("NaCl"));
				res.add(createCompound("NaF"));
				break;
			case Element.elmMg:
				res.add(createPureElement(Element.Mg));
				res.add(createCompound("MgO"));
				break;
			case Element.elmAl:
				res.add(createPureElement(Element.Al));
				res.add(createCompound("Al2O3"));
				break;
			case Element.elmSi:
				res.add(createPureElement(Element.Si));
				res.add(createCompound("SiO2"));
				break;
			case Element.elmP:
				res.add(createMaterial(GalliumPhosphate));
				res.add(createCompound("InP"));
				res.add(createCompound("Ca5(PO4)3F"));
				break;
			case Element.elmS:
				res.add(createCompound("Ag2S"));
				res.add(createCompound("ZnS"));
				res.add(createCompound("PbS"));
				res.add(createCompound("FeS2"));
				break;
			case Element.elmCl:
				res.add(createCompound("KCl"));
				res.add(createCompound("Mg3B7O13Cl"));
				res.add(createCompound("Pb5(AsO4)2Cl4"));
				res.add(createCompound("Pb4Fe(AsO4)2Cl4"));
				res.add(createCompound("Pb5(PO4)3Cl"));
				res.add(createCompound("Pb5(VO)3Cl"));
				res.add(createCompound("NaCl"));
				break;
			case Element.elmK:
				res.add(createCompound("KAlSi3O8"));
				res.add(createCompound("KBr"));
				res.add(createCompound("KCl"));
				break;
			case Element.elmCa:
				res.add(createCompound("CaCO3"));
				res.add(createCompound("CaF2"));
				res.add(createCompound("Ca5(PO4)3F"));
				res.add(createCompound("Ca5(PO4)3Cl"));
				break;
			case Element.elmSc:
				res.add(createPureElement(Element.Sc));
				break;
			case Element.elmTi:
				res.add(createPureElement(Element.Ti));
				res.add(createCompound("TiO2"));
				break;
			case Element.elmV:
				res.add(createPureElement(Element.V));
				break;
			case Element.elmCr:
				res.add(createPureElement(Element.Cr));
				res.add(createCompound("Cr2O3"));
				break;
			case Element.elmMn:
				res.add(createPureElement(Element.Mn));
				res.add(createCompound("MnO"));
				res.add(createCompound("MnCO3"));
				break;
			case Element.elmFe:
				res.add(createPureElement(Element.Fe));
				res.add(createCompound("Fe2O3"));
				res.add(createCompound("FeS2"));
				break;
			case Element.elmCo:
				res.add(createPureElement(Element.Co));
				break;
			case Element.elmNi:
				res.add(createPureElement(Element.Ni));
				break;
			case Element.elmCu:
				res.add(createPureElement(Element.Cu));
				res.add(createCompound("Cu2S"));
				res.add(createCompound("CuS"));
				res.add(createCompound("Cu2O"));
				break;
			case Element.elmZn:
				res.add(createPureElement(Element.Zn));
				res.add(createCompound("ZnS"));
				res.add(createCompound("ZnSe"));
				break;
			case Element.elmGa:
				res.add(createCompound("GaAs"));
				res.add(createCompound("GaP"));
				break;
			case Element.elmGe:
				res.add(createPureElement(Element.Ge));
				res.add(createCompound("GeTe"));
				break;
			case Element.elmAs:
				res.add(createPureElement(Element.As));
				res.add(createCompound("As2Se3"));
				res.add(createCompound("GaAs"));
				break;
			case Element.elmSe:
				res.add(createPureElement(Element.Se));
				res.add(createCompound("As2Se3"));
				res.add(createCompound("PbSe"));
				res.add(createCompound("Ti2Se"));
				break;
			case Element.elmBr:
				res.add(createCompound("AgBr"));
				res.add(createCompound("KBr"));
				break;
			case Element.elmRb:
				res.add(createCompound("RbC8H5O4"));
				break;
			case Element.elmSr:
				res.add(createCompound("SrSO4"));
				res.add(createCompound("SrF2"));
				break;
			case Element.elmY:
				break;
			case Element.elmZr:
				res.add(createPureElement(Element.Zr));
				res.add(createCompound("ZrSiO4"));
				break;
			case Element.elmNb:
				res.add(createPureElement(Element.Nb));
				res.add(createCompound("Nb2O5"));
				break;
			case Element.elmMo:
				res.add(createPureElement(Element.Mo));
				res.add(createCompound("PbMoO4"));
				break;
			case Element.elmTc:
				break;
			case Element.elmRu:
				break;
			case Element.elmRh:
				res.add(createPureElement(Element.Rh));
				break;
			case Element.elmPd:
				res.add(createPureElement(Element.Pd));
				break;
			case Element.elmAg:
				res.add(createPureElement(Element.Ag));
				res.add(createCompound("Ag2S"));
				res.add(createCompound("Ag3Sb"));
				break;
			case Element.elmCd:
				res.add(createPureElement(Element.Cd));
				res.add(createCompound("CdF2"));
				break;
			case Element.elmIn:
				res.add(createCompound("InSb"));
				res.add(createCompound("InAs"));
				res.add(createCompound("InP"));
				break;
			case Element.elmSn:
				res.add(createPureElement(Element.Sn));
				res.add(createCompound("SnO2"));
				res.add(createCompound("SnSe"));
				break;
			case Element.elmSb:
				res.add(createPureElement(Element.Sb));
				res.add(createCompound("Ag3Sb"));
				res.add(createCompound("InSb"));
				res.add(createCompound("Ag3Sb3"));
				res.add(createCompound("Sb2O3"));
				break;
			case Element.elmTe:
				res.add(createPureElement(Element.Te));
				res.add(createCompound("As2Te3"));
				res.add(createCompound("GeTe"));
				res.add(createCompound("HgTe"));
				res.add(createCompound("PbTe"));
				res.add(createCompound("TeO2"));
				break;
			case Element.elmI:
				res.add(createCompound("CsI"));
				break;
			case Element.elmCs:
				res.add(createCompound("CsI"));
				break;
			case Element.elmBa:
				res.add(createCompound("BaSO4"));
				res.add(createCompound("BaF2"));
				res.add(createCompound("BaTiSi3O9"));
				break;
			case Element.elmLa:
				res.add(createCompound("LaF2"));
				break;
			case Element.elmCe:
				res.add(createCompound("CeF3"));
				res.add(createCompound("CeO2"));
				break;
			case Element.elmPr:
				res.add(createCompound("PrF3"));
				break;
			case Element.elmNd:
				res.add(createCompound("NdF3"));
				break;
			case Element.elmPm:
				break;
			case Element.elmSm:
				break;
			case Element.elmEu:
				break;
			case Element.elmGd:
				res.add(createPureElement(Element.Gd));
				break;
			case Element.elmTb:
				res.add(createPureElement(Element.Tb));
				break;
			case Element.elmDy:
				res.add(createPureElement(Element.Dy));
				break;
			case Element.elmHo:
				res.add(createPureElement(Element.Ho));
				break;
			case Element.elmEr:
				break;
			case Element.elmTm:
				res.add(createPureElement(Element.Tm));
				break;
			case Element.elmYb:
				res.add(createPureElement(Element.Yb));
				break;
			case Element.elmLu:
				res.add(createPureElement(Element.Lu));
				break;
			case Element.elmHf:
				res.add(createPureElement(Element.Hf));
				break;
			case Element.elmTa:
				res.add(createPureElement(Element.Ta));
				break;
			case Element.elmW:
				res.add(createPureElement(Element.W));
				res.add(createCompound("CaWO4"));
				break;
			case Element.elmRe:
				res.add(createPureElement(Element.Re));
				break;
			case Element.elmOs:
				break;
			case Element.elmIr:
				res.add(createPureElement(Element.Ir));
				break;
			case Element.elmPt:
				res.add(createPureElement(Element.Pt));
				break;
			case Element.elmAu:
				res.add(createPureElement(Element.Au));
				break;
			case Element.elmHg:
				res.add(createCompound("HgS"));
				res.add(createCompound("HgTe"));
				break;
			case Element.elmTl:
				res.add(createCompound("Tl2Se"));
				break;
			case Element.elmPb:
				res.add(createPureElement(Element.Pb));
				res.add(createCompound("PbO2"));
				res.add(createCompound("PbF2"));
				res.add(createCompound("PbSO4"));
				res.add(createCompound("PbCO3"));
				res.add(createCompound("PbS"));
				res.add(createCompound("PbSe"));
				res.add(createCompound("PbTe"));
				break;
			case Element.elmBi:
				res.add(createPureElement(Element.Bi));
				break;
			case Element.elmPo:
				break;
			case Element.elmAt:
				break;
			case Element.elmRn:
				break;
			case Element.elmFr:
				break;
			case Element.elmRa:
				break;
			case Element.elmAc:
				break;
			case Element.elmTh:
				break;
			case Element.elmPa:
				break;
			case Element.elmU:
				res.add(createCompound("U3O8"));
				break;
			default:
				break;
			}
			for (final Composition c : res)
				assert c.containsElement(elm);
		} catch (final EPQException e) {
			// Just ignore it...
		}
		return res;
	}

}
