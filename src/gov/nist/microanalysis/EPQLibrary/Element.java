package gov.nist.microanalysis.EPQLibrary;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import gov.nist.microanalysis.Utility.CSVReader;

/**
 * <p>
 * A class representing an atomic element. This class also implements some
 * static methods to facilitate using integer atomic numbers to represent
 * elements.
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

public class Element
   implements Comparable<Element>, Serializable {
   static final private long serialVersionUID = 0x987360133793L;

   public static final int elmNone = 0;
   public static final int elmH = 1;
   public static final int elmHe = 2;
   public static final int elmLi = 3;
   public static final int elmBe = 4;
   public static final int elmB = 5;
   public static final int elmC = 6;
   public static final int elmN = 7;
   public static final int elmO = 8;
   public static final int elmF = 9;
   public static final int elmNe = 10;
   public static final int elmNa = 11;
   public static final int elmMg = 12;
   public static final int elmAl = 13;
   public static final int elmSi = 14;
   public static final int elmP = 15;
   public static final int elmS = 16;
   public static final int elmCl = 17;
   public static final int elmAr = 18;
   public static final int elmK = 19;
   public static final int elmCa = 20;
   public static final int elmSc = 21;
   public static final int elmTi = 22;
   public static final int elmV = 23;
   public static final int elmCr = 24;
   public static final int elmMn = 25;
   public static final int elmFe = 26;
   public static final int elmCo = 27;
   public static final int elmNi = 28;
   public static final int elmCu = 29;
   public static final int elmZn = 30;
   public static final int elmGa = 31;
   public static final int elmGe = 32;
   public static final int elmAs = 33;
   public static final int elmSe = 34;
   public static final int elmBr = 35;
   public static final int elmKr = 36;
   public static final int elmRb = 37;
   public static final int elmSr = 38;
   public static final int elmY = 39;
   public static final int elmZr = 40;
   public static final int elmNb = 41;
   public static final int elmMo = 42;
   public static final int elmTc = 43;
   public static final int elmRu = 44;
   public static final int elmRh = 45;
   public static final int elmPd = 46;
   public static final int elmAg = 47;
   public static final int elmCd = 48;
   public static final int elmIn = 49;
   public static final int elmSn = 50;
   public static final int elmSb = 51;
   public static final int elmTe = 52;
   public static final int elmI = 53;
   public static final int elmXe = 54;
   public static final int elmCs = 55;
   public static final int elmBa = 56;
   public static final int elmLa = 57;
   public static final int elmCe = 58;
   public static final int elmPr = 59;
   public static final int elmNd = 60;
   public static final int elmPm = 61;
   public static final int elmSm = 62;
   public static final int elmEu = 63;
   public static final int elmGd = 64;
   public static final int elmTb = 65;
   public static final int elmDy = 66;
   public static final int elmHo = 67;
   public static final int elmEr = 68;
   public static final int elmTm = 69;
   public static final int elmYb = 70;
   public static final int elmLu = 71;
   public static final int elmHf = 72;
   public static final int elmTa = 73;
   public static final int elmW = 74;
   public static final int elmRe = 75;
   public static final int elmOs = 76;
   public static final int elmIr = 77;
   public static final int elmPt = 78;
   public static final int elmAu = 79;
   public static final int elmHg = 80;
   public static final int elmTl = 81;
   public static final int elmPb = 82;
   public static final int elmBi = 83;
   public static final int elmPo = 84;
   public static final int elmAt = 85;
   public static final int elmRn = 86;
   public static final int elmFr = 87;
   public static final int elmRa = 88;
   public static final int elmAc = 89;
   public static final int elmTh = 90;
   public static final int elmPa = 91;
   public static final int elmU = 92;
   public static final int elmNp = 93;
   public static final int elmPu = 94;
   public static final int elmAm = 95;
   public static final int elmCm = 96;
   public static final int elmBk = 97;
   public static final int elmCf = 98;
   public static final int elmEs = 99;
   public static final int elmFm = 100;
   public static final int elmMd = 101;
   public static final int elmNo = 102;
   public static final int elmLr = 103;
   public static final int elmRf = 104;
   public static final int elmDb = 105;
   public static final int elmSg = 106;
   public static final int elmBh = 107;
   public static final int elmHs = 108;
   public static final int elmMt = 109;
   public static final int elmUun = 110;
   public static final int elmUuu = 111;
   public static final int elmUub = 112;
   public static final int elmEndOfElements = 113;
   private final int mAtomicNumber;

   public static final Element None = new Element(0);
   public static final Element H = new Element(1);
   public static final Element He = new Element(2);
   public static final Element Li = new Element(3);
   public static final Element Be = new Element(4);
   public static final Element B = new Element(5);
   public static final Element C = new Element(6);
   public static final Element N = new Element(7);
   public static final Element O = new Element(8);
   public static final Element F = new Element(9);
   public static final Element Ne = new Element(10);
   public static final Element Na = new Element(11);
   public static final Element Mg = new Element(12);
   public static final Element Al = new Element(13);
   public static final Element Si = new Element(14);
   public static final Element P = new Element(15);
   public static final Element S = new Element(16);
   public static final Element Cl = new Element(17);
   public static final Element Ar = new Element(18);
   public static final Element K = new Element(19);
   public static final Element Ca = new Element(20);
   public static final Element Sc = new Element(21);
   public static final Element Ti = new Element(22);
   public static final Element V = new Element(23);
   public static final Element Cr = new Element(24);
   public static final Element Mn = new Element(25);
   public static final Element Fe = new Element(26);
   public static final Element Co = new Element(27);
   public static final Element Ni = new Element(28);
   public static final Element Cu = new Element(29);
   public static final Element Zn = new Element(30);
   public static final Element Ga = new Element(31);
   public static final Element Ge = new Element(32);
   public static final Element As = new Element(33);
   public static final Element Se = new Element(34);
   public static final Element Br = new Element(35);
   public static final Element Kr = new Element(36);
   public static final Element Rb = new Element(37);
   public static final Element Sr = new Element(38);
   public static final Element Y = new Element(39);
   public static final Element Zr = new Element(40);
   public static final Element Nb = new Element(41);
   public static final Element Mo = new Element(42);
   public static final Element Tc = new Element(43);
   public static final Element Ru = new Element(44);
   public static final Element Rh = new Element(45);
   public static final Element Pd = new Element(46);
   public static final Element Ag = new Element(47);
   public static final Element Cd = new Element(48);
   public static final Element In = new Element(49);
   public static final Element Sn = new Element(50);
   public static final Element Sb = new Element(51);
   public static final Element Te = new Element(52);
   public static final Element I = new Element(53);
   public static final Element Xe = new Element(54);
   public static final Element Cs = new Element(55);
   public static final Element Ba = new Element(56);
   public static final Element La = new Element(57);
   public static final Element Ce = new Element(58);
   public static final Element Pr = new Element(59);
   public static final Element Nd = new Element(60);
   public static final Element Pm = new Element(61);
   public static final Element Sm = new Element(62);
   public static final Element Eu = new Element(63);
   public static final Element Gd = new Element(64);
   public static final Element Tb = new Element(65);
   public static final Element Dy = new Element(66);
   public static final Element Ho = new Element(67);
   public static final Element Er = new Element(68);
   public static final Element Tm = new Element(69);
   public static final Element Yb = new Element(70);
   public static final Element Lu = new Element(71);
   public static final Element Hf = new Element(72);
   public static final Element Ta = new Element(73);
   public static final Element W = new Element(74);
   public static final Element Re = new Element(75);
   public static final Element Os = new Element(76);
   public static final Element Ir = new Element(77);
   public static final Element Pt = new Element(78);
   public static final Element Au = new Element(79);
   public static final Element Hg = new Element(80);
   public static final Element Tl = new Element(81);
   public static final Element Pb = new Element(82);
   public static final Element Bi = new Element(83);
   public static final Element Po = new Element(84);
   public static final Element At = new Element(85);
   public static final Element Rn = new Element(86);
   public static final Element Fr = new Element(87);
   public static final Element Ra = new Element(88);
   public static final Element Ac = new Element(89);
   public static final Element Th = new Element(90);
   public static final Element Pa = new Element(91);
   public static final Element U = new Element(92);
   public static final Element Np = new Element(93);
   public static final Element Pu = new Element(94);
   public static final Element Am = new Element(95);
   public static final Element Cm = new Element(96);
   public static final Element Bk = new Element(97);
   public static final Element Cf = new Element(98);
   public static final Element Es = new Element(99);
   public static final Element Fm = new Element(100);
   public static final Element Md = new Element(101);
   public static final Element No = new Element(102);
   public static final Element Lr = new Element(103);
   public static final Element Rf = new Element(104);
   public static final Element Db = new Element(105);
   public static final Element Sg = new Element(106);
   public static final Element Bh = new Element(107);
   public static final Element Hs = new Element(108);
   public static final Element Mt = new Element(109);
   public static final Element Uun = new Element(110);
   public static final Element Uuu = new Element(111);
   public static final Element Uub = new Element(112);

   private static final Element[] mAllElements = {
      H,
      He,
      Li,
      Be,
      B,
      C,
      N,
      O,
      F,
      Ne,
      Na,
      Mg,
      Al,
      Si,
      P,
      S,
      Cl,
      Ar,
      K,
      Ca,
      Sc,
      Ti,
      V,
      Cr,
      Mn,
      Fe,
      Co,
      Ni,
      Cu,
      Zn,
      Ga,
      Ge,
      As,
      Se,
      Br,
      Kr,
      Rb,
      Sr,
      Y,
      Zr,
      Nb,
      Mo,
      Tc,
      Ru,
      Rh,
      Pd,
      Ag,
      Cd,
      In,
      Sn,
      Sb,
      Te,
      I,
      Xe,
      Cs,
      Ba,
      La,
      Ce,
      Pr,
      Nd,
      Pm,
      Sm,
      Eu,
      Gd,
      Tb,
      Dy,
      Ho,
      Er,
      Tm,
      Yb,
      Lu,
      Hf,
      Ta,
      W,
      Re,
      Os,
      Ir,
      Pt,
      Au,
      Hg,
      Tl,
      Pb,
      Bi,
      Po,
      At,
      Rn,
      Fr,
      Ra,
      Ac,
      Th,
      Pa,
      U,
      Np,
      Pu,
      Am,
      Cm,
      Bk,
      Cf,
      Es,
      Fm,
      Md,
      No,
      Lr,
      Rf,
      Db,
      Sg,
      Bh,
      Hs,
      Mt,
      Uun,
      Uuu,
      Uub
   };

   private static final String[] mElementNames = {
      "None",
      "Hydrogen",
      "Helium",
      "Lithium",
      "Beryllium",
      "Boron",
      "Carbon",
      "Nitrogen",
      "Oxygen",
      "Fluorine",
      "Neon",
      "Sodium",
      "Magnesium",
      "Aluminum",
      "Silicon",
      "Phosphorus",
      "Sulfur",
      "Chlorine",
      "Argon",
      "Potassium",
      "Calcium",
      "Scandium",
      "Titanium",
      "Vanadium",
      "Chromium",
      "Manganese",
      "Iron",
      "Cobalt",
      "Nickel",
      "Copper",
      "Zinc",
      "Gallium",
      "Germanium",
      "Arsenic",
      "Selenium",
      "Bromine",
      "Krypton",
      "Rubidium",
      "Strontium",
      "Yttrium",
      "Zirconium",
      "Niobium",
      "Molybdenum",
      "Technetium",
      "Ruthenium",
      "Rhodium",
      "Palladium",
      "Silver",
      "Cadmium",
      "Indium",
      "Tin",
      "Antimony",
      "Tellurium",
      "Iodine",
      "Xenon",
      "Cesium",
      "Barium",
      "Lanthanum",
      "Cerium",
      "Praseodymium",
      "Neodymium",
      "Promethium",
      "Samarium",
      "Europium",
      "Gadolinium",
      "Terbium",
      "Dysprosium",
      "Holmium",
      "Erbium",
      "Thulium",
      "Ytterbium",
      "Lutetium",
      "Hafnium",
      "Tantalum",
      "Tungsten",
      "Rhenium",
      "Osmium",
      "Iridium",
      "Platinum",
      "Gold",
      "Mercury",
      "Thallium",
      "Lead",
      "Bismuth",
      "Polonium",
      "Astatine",
      "Radon",
      "Francium",
      "Radium",
      "Actinium",
      "Thorium",
      "Protactinium",
      "Uranium",
      "Neptunium",
      "Plutonium",
      "Americium",
      "Curium",
      "Berkelium",
      "Californium",
      "Einsteinium",
      "Fermium",
      "Mendelevium",
      "Nobelium",
      "Lawrencium",
      "Rutherfordium",
      "Dubnium",
      "Seaborgium",
      "Bohrium",
      "Hassium",
      "Meitnerium",
      "Ununnilium",
      "Unununium",
      "Ununbium",
      "End-of-elements"
   };

   private static final String[] mAbbreviations = {
      "",
      "H",
      "He",
      "Li",
      "Be",
      "B",
      "C",
      "N",
      "O",
      "F",
      "Ne",
      "Na",
      "Mg",
      "Al",
      "Si",
      "P",
      "S",
      "Cl",
      "Ar",
      "K",
      "Ca",
      "Sc",
      "Ti",
      "V",
      "Cr",
      "Mn",
      "Fe",
      "Co",
      "Ni",
      "Cu",
      "Zn",
      "Ga",
      "Ge",
      "As",
      "Se",
      "Br",
      "Kr",
      "Rb",
      "Sr",
      "Y",
      "Zr",
      "Nb",
      "Mo",
      "Tc",
      "Ru",
      "Rh",
      "Pd",
      "Ag",
      "Cd",
      "In",
      "Sn",
      "Sb",
      "Te",
      "I",
      "Xe",
      "Cs",
      "Ba",
      "La",
      "Ce",
      "Pr",
      "Nd",
      "Pm",
      "Sm",
      "Eu",
      "Gd",
      "Tb",
      "Dy",
      "Ho",
      "Er",
      "Tm",
      "Yb",
      "Lu",
      "Hf",
      "Ta",
      "W",
      "Re",
      "Os",
      "Ir",
      "Pt",
      "Au",
      "Hg",
      "Tl",
      "Pb",
      "Bi",
      "Po",
      "At",
      "Rn",
      "Fr",
      "Ra",
      "Ac",
      "Th",
      "Pa",
      "U",
      "Np",
      "Pu",
      "Am",
      "Cm",
      "Bk",
      "Cf",
      "Es",
      "Fm",
      "Md",
      "No",
      "Lr",
      "Rf",
      "Db",
      "Sg",
      "Bh",
      "Hs",
      "Mt",
      "Uun",
      "Uuu",
      "Uub",
      "EOE"
   };

   private static double[] mIonizationEnergy; // Nominal in Joules
   private static double[] mAtomicWeight; // nominal, in AMU

   // must be private for the whole scheme to work
   private Element(int atomicNo) {
      super();
      assert ((atomicNo >= elmNone) && (atomicNo < elmEndOfElements));
      mAtomicNumber = atomicNo;
   }

   // must be private for the whole scheme to work
   private Element() {
      super();
      mAtomicNumber = Element.elmNone;
   }

   private static void readAtomicWeights() {
      try {
         synchronized(Element.class) {
            if(mAtomicWeight == null) {
               final double[][] TempDoubleArray = (new CSVReader.ResourceReader("AtomicWeights.csv", true)).getResource(Element.class);
               mAtomicWeight = new double[TempDoubleArray.length];
               for(int index = 0; index < TempDoubleArray.length; ++index)
                  mAtomicWeight[index] = TempDoubleArray[index][0];
            }
         }
      }
      catch(final Exception ex) {
         throw new EPQFatalException("Fatal error while attempting to load the atomic weights data file.");
      }
   }

   /**
    * atomicNumberForName - Get the atomic number for the named element. THe
    * name may be the abbreviation or the full name (case insensitive)
    * 
    * @param name String
    * @return int
    */
   static public int atomicNumberForName(String name) {
      for(int i = 0; i < mElementNames.length; ++i)
         if((mElementNames[i].compareToIgnoreCase(name) == 0) || (mAbbreviations[i].compareTo(name) == 0))
            return i;
      try {
         return Integer.parseInt(name);
      }
      catch(final NumberFormatException ex) {
         return Element.elmNone;
      }
   }

   /**
    * byName - Get the Element associated with the specified name or
    * abbreviation.
    * 
    * @param name String
    * @return Element
    */
   static public Element byName(String name) {
      final int z = atomicNumberForName(name);
      return z == 0 ? None : mAllElements[z - 1];
   }
   
   
   /**
    * atomicNumberForAbbrev - Get the atomic number for the named element. THe
    * name may be the abbreviation.
    * 
    * @param name String
    * @return int
    */
   static public int atomicNumberForAbbrev(String abbrev) {
      for(int i = 0; i < mElementNames.length; ++i)
         if(mAbbreviations[i].compareToIgnoreCase(abbrev) == 0)
            return i;
      return Element.elmNone;
   }
   
   /**
    * byAbbrev - Get the Element associated with the specified abbreviation.
    * 
    * @param name String
    * @return Element
    */
   static public Element byAbbrev(String name) {
      final int z = atomicNumberForAbbrev(name);
      return z == 0 ? None : mAllElements[z - 1];
   }


   /**
    * byName - Get the Element associated with the specified atomic number.
    * 
    * @param an int - The atomic number
    * @return Element
    */
   static public Element byAtomicNumber(int an) {
      return (an >= 1) && (an < mAllElements.length) ? mAllElements[an - 1] : Element.None;
   }

   /**
    * getAtomicWeight - A static method to return the atomic weight of an
    * element specified by the atomic number.
    * 
    * @param atomicNo int - The atomic number.
    * @return double - The atomic weight in AMU.
    */
   final public static double getAtomicWeight(int atomicNo) {
      if(mAtomicWeight == null)
         readAtomicWeights();
      return mAtomicWeight[atomicNo - 1];
   }

   /**
    * AllElements - A immutable list of all elements (H to Uub).
    * 
    * @return List
    */
   final public static List<Element> allElements() {
      return Collections.unmodifiableList(Arrays.asList(mAllElements));
   }

   final public static Element[] range(Element min, Element max) {
      assert min.getAtomicNumber() <= max.getAtomicNumber();
      return Arrays.copyOfRange(mAllElements, min.getAtomicNumber() - 1, max.getAtomicNumber() - 1);
   }

   /**
    * meanIonizationPotential - Returns the meanIonizationPotential for the
    * specified element in Joules.
    * 
    * @param atomicNo int
    * @return double
    */
   public static double meanIonizationPotential(int atomicNo) {
      try {
         return MeanIonizationPotential.Berger64.compute(Element.byAtomicNumber(atomicNo));
      }
      catch(final Exception ex) {
         return MeanIonizationPotential.Sternheimer64.compute(Element.byAtomicNumber(atomicNo));
      }
   }

   /**
    * getAtomicNumber - Get the atomic number of this element.
    * 
    * @return int
    */
   final public int getAtomicNumber() {
      return mAtomicNumber;
   }

   /**
    * AtomicWeight - A method to return the atomic weight of an element.
    * 
    * @return double - The atomic weight in AMU.
    */
   final public double getAtomicWeight() {
      return getAtomicWeight(mAtomicNumber);
   }

   /**
    * getMass - the mass of the element in kilograms.
    * 
    * @return double
    */
   final public double getMass() {
      return ToSI.AMU(getAtomicWeight(mAtomicNumber));
   }

   /**
    * toAbbrev - Returns the abbreviation associated with this element.
    * 
    * @return String
    */
   final public String toAbbrev() {
      return mAbbreviations[mAtomicNumber];
   }

   /**
    * toAbbrev - A static method that returns the abbreviation associated with
    * the specified atomic number.
    * 
    * @param atomicNo int - The atomic number.
    * @return String
    */
   final public static String toAbbrev(int atomicNo) {
      return mAbbreviations[atomicNo];
   }

   /**
    * toString - A static method that returns the name of the element with the
    * specified atomic number.
    * 
    * @param el int - The atomic number
    * @return String
    */
   public static String toString(int el) {
      return mElementNames[el];
   }

   /**
    * MeanIonizationPotential - The mean ionization potential (often labeled J)
    * for this element. From the table when available or calculated using the
    * Berger-Selzer formula.
    * 
    * @return double - J in Joules
    */
   public double meanIonizationPotential() {
      try {
         return MeanIonizationPotential.Berger64.compute(this);
      }
      catch(final Exception ex) {
         return MeanIonizationPotential.Sternheimer64.compute(this);
      }
   }

   /**
    * energyLoss - Computes the energy lossed by an electron of the specified
    * energy. Formula from Bethe with Joy-Luo's modification from Goldstein et
    * al
    * 
    * @param eK double - The electron energy in Joules
    * @return double - in Joules per meter per kg/meter^3
    */
   public double energyLoss(double eK) {
      return BetheElectronEnergyLoss.JoyLuo1989.compute(this, eK);
   }

   /**
    * massAbsorptionCoefficient - Calculates the mass absorption coefficient for
    * x-rays of the specified energy in this element.
    * 
    * @param energy double - X-ray energy in Joules
    * @return double - Absorption per unit length (meter) per unit density
    *         (kg/m^3)
    */
   @Deprecated
   public double massAbsorptionCoefficient(double energy) {
      return AlgorithmUser.getDefaultMAC().compute(this, energy);
   }

   /**
    * massAbsorptionCoefficient - Calculates the mass absorption coefficient for
    * x-rays of the specified x-ray transition.
    * 
    * @param xrt XRayTransition
    * @return double - Absorption per unit length (meter) per unit density
    *         (kg/m^3)
    */
   @Deprecated
   public double massAbsorptionCoefficient(XRayTransition xrt)
         throws EPQException {
      return AlgorithmUser.getDefaultMAC().compute(this, xrt);
   }

   /**
    * isValid - A static method for determining whether an atomic number is in
    * range understood by this class.
    * 
    * @param atomicNo int
    * @return boolean
    */
   final static public boolean isValid(int atomicNo) {
      return (atomicNo >= elmH) && (atomicNo < elmEndOfElements);
   }

   /**
    * isValid - A public method for determining whether this element represents
    * a real element.
    * 
    * @return boolean
    */
   final public boolean isValid() {
      return (mAtomicNumber >= elmH) && (mAtomicNumber < elmEndOfElements);
   }

   /**
    * compareTo - Implements the Comparable interface.
    * 
    * @param e Element
    * @return int
    */
   @Override
   public int compareTo(Element e) {
      if(mAtomicNumber < e.mAtomicNumber)
         return -1;
      else
         return mAtomicNumber == e.mAtomicNumber ? 0 : 1;
   }

   /**
    * hashCode - Calculates a hash code that differentiates elements but not
    * instances of elements.
    * 
    * @return int
    */
   @Override
   public int hashCode() {
      // mAtomicNumber is always less than 128 (1<<7). Int has 31 + 1 bits. 31-7
      // = 24
      return mAtomicNumber << 24;
   }

   /**
    * equals - Returns true if this and obj represent the equivalent elements.
    * (Same atomic number)
    * 
    * @param obj Object
    * @return boolean
    */
   @Override
   public boolean equals(Object obj) {
      if(obj instanceof Element) {
         final Element el = (Element) obj;
         return el.mAtomicNumber == mAtomicNumber;
      } else
         return false;
   }

   /**
    * toString - Returns a string containing the elements name.
    * 
    * @return String
    */
   @Override
   public String toString() {
      return mElementNames[mAtomicNumber];
   }

   /**
    * getIonizationEnergy - Returns the first ionization energy for this
    * element.
    * 
    * @return In Joules
    */
   public double getIonizationEnergy() {
      if(mIonizationEnergy == null)
         synchronized(Element.class) {
            if(mIonizationEnergy == null) {
               final double[][] arr = (new CSVReader.ResourceReader("IonizationEnergies.csv", true)).getResource(Element.class);
               mIonizationEnergy = new double[arr.length];
               for(int index = 0; index < arr.length; ++index)
                  mIonizationEnergy[index] = arr[index] != null ? ToSI.eV(arr[index][0]) : -1.0;
            }
         }
      assert mAtomicNumber >= 1 : toString();
      final double res = (mAtomicNumber - 1) < mIonizationEnergy.length ? mIonizationEnergy[mAtomicNumber - 1] : -1.0;
      if(res == -1.0)
         throw new EPQFatalException("The ionization energy is not available for " + toAbbrev());
      return res;
   }

   /**
    * Resolves the Element objects that are read from file into the static
    * instances of Element objects. This ensures that Element.Ca is the only
    * instance of Element with mAtomicNumber=Element.elmCa.
    * 
    * @return An Element cast to Object
    * @throws ObjectStreamException
    */
   private Object readResolve()
         throws ObjectStreamException {
      return Element.byAtomicNumber(mAtomicNumber);
   }

   static public final ArrayList<String> getListOfAbbreviations(Element minEl, Element maxEl) {
      final ArrayList<String> res = new ArrayList<String>();
      for(int z = minEl.getAtomicNumber(); z <= maxEl.getAtomicNumber(); ++z)
         res.add(toAbbrev(z));
      return res;
   }

   static public final ArrayList<String> getListOfElements(Element minEl, Element maxEl) {
      final ArrayList<String> res = new ArrayList<String>();
      for(int z = minEl.getAtomicNumber(); z <= maxEl.getAtomicNumber(); ++z)
         res.add(toString(z));
      return res;
   }

   static public Set<Element> parseElementString(String text) {
      text = text.trim();
      if(text.startsWith("["))
         text = text.substring(1);
      if(text.endsWith("]"))
         text = text.substring(0, text.length() - 1);
      final String[] items = text.split("\\t|\\n|\\x0B|\\f|\\r|,| ");
      final TreeSet<Element> elms = new TreeSet<Element>();
      for(final String item : items) {
         final Element elm = Element.byName(item.trim());
         if(!elm.equals(Element.None))
            elms.add(elm);
      }
      return elms;
   }

   static public String toString(Collection<Element> elms, boolean abbrev) {
      final StringBuffer sb = new StringBuffer();
      boolean first = true;
      for(final Element elm : elms) {
         if(!first)
            sb.append(", ");
         sb.append(abbrev ? elm.toAbbrev() : elm.toString());
         first = false;
      }
      return sb.toString();
   }
}
