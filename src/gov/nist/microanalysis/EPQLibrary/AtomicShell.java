package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.CSVReader;

/**
 * <p>
 * A class containing methods that relate to atomic shells.
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

final public class AtomicShell
   implements
   Comparable<AtomicShell>,
   Cloneable {
   private final Element mElement;
   private final int mShell;

   public static final int K = 0;
   public static final int LI = 1;
   public static final int LII = 2;
   public static final int LIII = 3;
   public static final int MI = 4;
   public static final int MII = 5;
   public static final int MIII = 6;
   public static final int MIV = 7;
   public static final int MV = 8;
   public static final int NI = 9;
   public static final int NII = 10;
   public static final int NIII = 11;
   public static final int NIV = 12;
   public static final int NV = 13;
   public static final int NVI = 14;
   public static final int NVII = 15;
   public static final int OI = 16;
   public static final int OII = 17;
   public static final int OIII = 18;
   public static final int OIV = 19;
   public static final int OV = 20;
   public static final int OVI = 21;
   public static final int OVII = 22;
   public static final int OVIII = 23;
   public static final int OIX = 24;
   public static final int PI = 25;
   public static final int PII = 26;
   public static final int PIII = 27;
   public static final int PIV = 28;
   public static final int PV = 29;
   public static final int PVI = 30;
   public static final int PVII = 31;
   public static final int PVIII = 32;
   public static final int PIX = 33;
   public static final int PX = 34;
   public static final int PXI = 35;
   public static final int QI = 36;
   public static final int QII = 37;
   public static final int QIII = 38;
   public static final int QIV = 39;
   public static final int QV = 40;
   public static final int QVI = 41;
   public static final int QVII = 42;
   public static final int QVIII = 43;
   public static final int QIX = 44;
   public static final int QX = 45;
   public static final int QXI = 46;
   public static final int QXII = 47;
   public static final int QXIII = 48;
   public static final int Last = 49;
   public static final int Continuum = 1000;
   public static final int NoShell = 1001;
   // Shell families
   public static final int NoFamily = 1999;
   public static final int KFamily = 2000;
   public static final int LFamily = 2001;
   public static final int MFamily = 2002;
   public static final int NFamily = 2003;
   public static final int OFamily = 2004;
   public static final int LastFamily = 2005;

   private static final String[] mAtomicNames = {
      "1S",
      "2S",
      "2P1/2",
      "2P3/2",
      "3S",
      "3P1/2",
      "3P3/2",
      "3D3/2",
      "3D5/2",
      "4S",
      "4P1/2",
      "4P3/2",
      "4D3/2",
      "4D5/2",
      "4F5/2",
      "4F7/2",
      "5S",
      "5P1/2",
      "5P3/2",
      "5D3/2",
      "5D5/2",
      "5F5/2",
      "5F7/2",
      "5G7/2",
      "5G9/2",
      "6S",
      "6P1/2",
      "6P3/2",
      "6D3/2",
      "6D5/2",
      "6F5/2",
      "6F7/2",
      "6G7/2",
      "6G9/2",
      "6H9/2",
      "6H11/2",
      "7S",
      "7P1/2",
      "7P3/2",
      "7D3/2",
      "7D5/2",
      "7F5/2",
      "7F7/2",
      "7G7/2",
      "7G92/",
      "7H9/2",
      "7H11/2",
      "7I11/2",
      "7I13/2"
   };

   private static final String[] mSiegbahnNames = {
      "K",
      "LI",
      "LII",
      "LIII",
      "MI",
      "MII",
      "MIII",
      "MIV",
      "MV",
      "NI",
      "NII",
      "NIII",
      "NIV",
      "NV",
      "NVI",
      "NVII",
      "OI",
      "OII",
      "OIII",
      "OIV",
      "OV",
      "OVI",
      "OVII",
      "OVIII",
      "OIX",
      "PI",
      "PII",
      "PIII",
      "PIV",
      "PV",
      "PVI",
      "PVII",
      "PVIII",
      "PIX",
      "PX",
      "PXI",
      "QI",
      "QII",
      "QIII",
      "QIV",
      "QV",
      "QVI",
      "QVII",
      "QVIII",
      "QIX",
      "QX",
      "QXI",
      "QXII",
      "QXIII"
   };

   private static final String[] mIUPACNames = {
      "K",
      "L1",
      "L2",
      "L3",
      "M1",
      "M2",
      "M3",
      "M4",
      "M5",
      "N1",
      "N2",
      "N3",
      "N4",
      "N5",
      "N6",
      "N7",
      "O1",
      "O2",
      "O3",
      "O4",
      "O5",
      "O6",
      "O7",
      "O8",
      "O9",
      "P1",
      "P2",
      "P3",
      "P4",
      "P5",
      "P6",
      "P7",
      "P8",
      "P9",
      "P10",
      "P11",
      "Q1",
      "Q2",
      "Q3",
      "Q4",
      "Q5",
      "Q6",
      "Q7",
      "Q8",
      "Q9",
      "Q10",
      "Q11",
      "Q12",
      "Q13"
   };

   private static final int[] mCapacity = {
      2,
      2,
      2,
      4,
      2,
      2,
      4,
      4,
      6,
      2,
      2,
      4,
      4,
      6,
      6,
      8,
      2,
      2,
      4,
      4,
      6,
      6,
      8,
      8,
      10,
      2,
      2,
      4,
      4,
      6,
      6,
      8,
      8,
      10,
      10,
      12,
      2,
      2,
      4,
      4,
      6,
      6,
      8,
      8,
      10,
      10,
      12,
      12,
      14
   };

   private static final int[] mOrbitalAngularMomentum = {
      0, // N=1
      0,
      1,
      1, // N = 2
      0,
      1,
      1,
      2,
      2, // N = 3
      0,
      1,
      1,
      2,
      2,
      3,
      3, // N = 4
      0,
      1,
      1,
      2,
      2,
      3,
      3,
      4,
      4, // N = 5
      0,
      1,
      1,
      2,
      2,
      3,
      3,
      4,
      4,
      5,
      5, // N =6
      0,
      1,
      1,
      2,
      2,
      3,
      3,
      4,
      4,
      5,
      5,
      6,
      6
         // N =7
   };

   private static final double[] mTotalAngularMomentum = {
      0.5, // N=1
      0.5,
      0.5,
      1.5, // N = 2
      0.5,
      0.5,
      1.5,
      1.5,
      2.5, // N = 3
      0.5,
      0.5,
      1.5,
      1.5,
      2.5,
      2.5,
      3.5, // N = 4
      0.5,
      0.5,
      1.5,
      1.5,
      2.5,
      2.5,
      3.5,
      3.5,
      4.5, // N = 5
      0.5,
      0.5,
      1.5,
      1.5,
      2.5,
      2.5,
      3.5,
      3.5,
      4.5,
      4.5,
      5.5, // N =6
      0.5,
      0.5,
      1.5,
      1.5,
      2.5,
      2.5,
      3.5,
      3.5,
      4.5,
      4.5,
      5.5,
      5.5,
      6.5
         // N =7
   };

   private static int[][] mOccupancy; // mOccupancy[Z-1][shell]

   /**
    * getGroundStateOccupancy - Returns the number of electrons the atomic shell
    * represented by the this object for a ground-state atom. The implementation
    * is based numbers extracted from the EADL ENDLIB-97 database.
    * 
    * @return int
    */
   public int getGroundStateOccupancy() {
      if(mOccupancy == null) {
         synchronized(this) {
            if(mOccupancy == null) {
               final CSVReader cr = new CSVReader.ResourceReader("ElectronConfig.csv", true);
               final double[][] res = cr.getResource(AtomicShell.class);
               mOccupancy = new int[res.length][];
               for(final double[] zOcc : res) {
                  final int z = (int) Math.round(zOcc[0]);
                  final int[] occ = new int[zOcc.length - 1];
                  int sum = 0;
                  for(int j = 0; j < occ.length; ++j) {
                     occ[j] = (int) Math.round(zOcc[j + 1]);
                     sum += occ[j];
                  }
                  assert sum == z;
                  mOccupancy[z - 1] = occ;
               }
            }
         }
      }
      final int[] occ = mOccupancy[mElement.getAtomicNumber() - 1];
      return occ.length > mShell ? occ[mShell] : 0;
   }

   /**
    * isLineFamily - Does this integer represent one of the line familys -
    * KFamily through OFamily.
    * 
    * @param f int
    * @return boolean
    */
   public static boolean isLineFamily(int f) {
      return (f == KFamily) || (f == LFamily) || (f == MFamily) || (f == NFamily) || (f == OFamily);
   }

   /**
    * AtomicShell - Constructs an AtomicShell object from a atomic number and a
    * shell.
    * 
    * @param el Element
    * @param shell int
    */
   public AtomicShell(Element el, int shell) {
      assert AtomicShell.isValid(shell) : "Shell=" + Integer.toString(shell);
      assert AtomicShell.mAtomicNames.length == AtomicShell.Last;
      assert AtomicShell.mSiegbahnNames.length == AtomicShell.Last;
      assert AtomicShell.mCapacity.length == AtomicShell.Last;
      assert AtomicShell.mOrbitalAngularMomentum.length == AtomicShell.Last;
      mElement = el;
      mShell = shell;
   }

   /**
    * getAtomicName - get the atomic physics standard name for this shell.
    * 
    * @param shell int
    * @return String
    */
   public static String getAtomicName(int shell) {
      if((shell >= K) && (shell < mAtomicNames.length))
         return mAtomicNames[shell];
      else if(shell == Continuum)
         return "Continuum";
      else
         return "Unknown";
   }

   /**
    * getAtomicName - get the atomic physics standard name for this shell.
    * 
    * @return String
    */
   public String getAtomicName() {
      return mElement.toAbbrev() + " " + getAtomicName(mShell);
   }

   /**
    * getSiegbahnName - get the Siegban style name for this shell.
    * 
    * @param shell int
    * @return String
    */
   public static String getSiegbahnName(int shell) {
      if((shell >= K) && (shell < mSiegbahnNames.length))
         return mSiegbahnNames[shell];
      else if(shell == Continuum)
         return "Continuum";
      else
         return "Unknown";
   }

   /**
    * parseSiegahnName - Serves as the inverse of getSiegbahnName(shell).
    * 
    * @param str
    * @return The integer index of the shell represented by the string or
    *         NoShell if the string is not recognized.
    */
   public static int parseSiegahnName(String str) {
      for(int sh = AtomicShell.K; sh < mSiegbahnNames.length; ++sh)
         if(str.equalsIgnoreCase(mSiegbahnNames[sh]))
            return sh;
      if(str.equalsIgnoreCase("Continuum"))
         return AtomicShell.Continuum;
      return AtomicShell.NoShell;

   }

   /**
    * getSiegbahnName - get the Siegban style name for this shell.
    * 
    * @return String
    */
   public String getSiegbahnName() {
      return mElement.toAbbrev() + " " + getSiegbahnName(mShell);
   }

   /**
    * getIUPACName - Get the IUPAC standard name for this shell.
    * 
    * @param shell int
    * @return String
    */
   public static String getIUPACName(int shell) {
      if((shell >= K) && (shell < mIUPACNames.length))
         return mIUPACNames[shell];
      else if(shell == Continuum)
         return "Continuum";
      else
         return "Unknown";
   }

   /**
    * getIUPACName - Get the IUPAC standard name for this shell.
    * 
    * @return String
    */
   public String getIUPACName() {
      return mElement.toAbbrev() + " " + getIUPACName(mShell);
   }

   /**
    * parseIUPACName - The inverse to getIUPACName
    * 
    * @param str
    * @return An integer index to a shell
    */
   public static int parseIUPACName(String str) {
      for(int sh = 0; sh < mIUPACNames.length; ++sh)
         if(str.equalsIgnoreCase(mIUPACNames[sh]))
            return sh;
      if(str.equalsIgnoreCase("Continuum"))
         return Continuum;
      return AtomicShell.NoShell;
   }

   /**
    * getEdgeEnergy - Returns the edge energy for the specified atom and shell.
    * (Uses EdgeEnergy.Default) Returns zero for shells without edges.
    * 
    * @param el Element
    * @param shell int
    * @return double
    */
   public static double getEdgeEnergy(Element el, int shell) {
      return AlgorithmUser.getDefaultEdgeEnergy().compute(new AtomicShell(el, shell));
   }

   /**
    * getEdgeEnergy - Returns the edge energy for this AtomicShell (using
    * EdgeEnergy.Default). The edge energy is the minimum energy required to
    * excite an electron from this AtomicShell into a shell with a vacancy. This
    * is not the same as the ionization energy.
    * 
    * @return double
    */
   public double getEdgeEnergy() {
      return AlgorithmUser.getDefaultEdgeEnergy().compute(this);
   }

   /**
    * getCapacity - Returns the maximum number of electrons that can populate
    * the specified shell.
    * 
    * @param shell int - One of KShell to OIX shell
    * @return int
    */
   public static int getCapacity(int shell) {
      assert (isValid(shell) && (shell != Continuum));
      return mCapacity[shell];
   }

   /**
    * getCapacity - Returns the number of electrons that can populate this
    * shell.
    * 
    * @return int
    */
   public int getCapacity() {
      return getCapacity(mShell);
   }

   /**
    * isValid - Does shell represent a valid integer value for a shell?
    * 
    * @param shell int
    * @return boolean
    */
   static public boolean isValid(int shell) {
      return (shell >= K) && ((shell < Last) || (shell == Continuum));
   }

   /**
    * getFamily - Returns the family into which the specified shell falls
    * (KFamily, LFamily,..,OFamily).
    * 
    * @param shell - A valid shell (K through OIX). Shells other than these and
    *           the continuum return NoFamily.
    * @return int - one of KFamily, LFamily, ..., OFamily or NoFamily
    */
   public static int getFamily(int shell) {
      if(shell == K)
         return KFamily;
      else if(shell <= LIII)
         return LFamily;
      else if(shell <= MV)
         return MFamily;
      else if(shell <= NVII)
         return NFamily;
      else if(shell <= OIX)
         return OFamily;
      else
         return NoFamily;
   }

   /**
    * getFirstInFamily - Returns the integer constant representing the first
    * shell in the specified family.
    * 
    * @param family int
    * @return int
    */
   public static int getFirstInFamily(int family) {
      switch(family) {
         case KFamily:
            return K;
         case LFamily:
            return LI;
         case MFamily:
            return MI;
         case NFamily:
            return NI;
         case OFamily:
            return OI;
         default:
            throw new EPQFatalException("Unknown family in getFirstInFamily");
      }
   }

   /**
    * getLastInFamily - Returns the integer constant representing the last shell
    * in the specified family.
    * 
    * @param family int
    * @return int
    */
   public static int getLastInFamily(int family) {
      switch(family) {
         case KFamily:
            return K;
         case LFamily:
            return LIII;
         case MFamily:
            return MV;
         case NFamily:
            return NVII;
         case OFamily:
            return OIX;
         default:
            throw new EPQFatalException("Unknown family in getFirstInFamily");
      }
   }

   /**
    * getFamilyName - Returns a string containing the name of the line family.
    * 
    * @param family int
    * @return String
    */
   public static String getFamilyName(int family) {
      switch(family) {
         case KFamily:
            return "K";
         case LFamily:
            return "L";
         case MFamily:
            return "M";
         case NFamily:
            return "N";
         case OFamily:
            return "O";
         default:
            return "None";
      }
   }

   /**
    * parseFamilyName - The inverse of getFamilyName. Parses strings produced by
    * getFamilyName back into KFamily, LFamily, ..., OFamily constants. Returns
    * NoFamily if the name is not recognized.
    * 
    * @param str String
    * @return int
    */
   public static int parseFamilyName(String str) {
      str = str.toUpperCase().trim();
      if(str.equalsIgnoreCase("K"))
         return KFamily;
      else if(str.equalsIgnoreCase("L"))
         return LFamily;
      else if(str.equalsIgnoreCase("M"))
         return MFamily;
      else if(str.equalsIgnoreCase("N"))
         return NFamily;
      else if(str.equalsIgnoreCase("O"))
         return OFamily;
      else
         return NoFamily;
   }

   /**
    * getFamily - Returns the family into which this AtomicShell falls (KFamily,
    * LFamily,..,OFamily).
    * 
    * @return int
    */
   public int getFamily() {
      return getFamily(mShell);
   }

   /**
    * getPrincipalQuantumNumber - Gets the principle atomic number (typically
    * denoted N) associated with this shell.
    * 
    * @param shell int
    * @return int
    */
   public static int getPrincipalQuantumNumber(int shell) {
      assert (isValid(shell));
      if(shell == K)
         return 1;
      else if(shell <= LIII)
         return 2;
      else if(shell <= MV)
         return 3;
      else if(shell <= NVII)
         return 4;
      else if(shell <= OIX)
         return 5;
      else if(shell <= PXI)
         return 6;
      else if(shell <= QXIII)
         return 7;
      throw new EPQFatalException("Unexpected shell in AtomicShell.getPrincipleQuantumNumber: " + Integer.toString(shell));
   }

   /**
    * getPrincipalQuantumNumber - Returns the principal quantum number
    * associated with this shell.
    * 
    * @return int
    */
   public int getPrincipalQuantumNumber() {
      return getPrincipalQuantumNumber(mShell);
   }

   /**
    * getEnergy - Get the energy required to liberate an electron from this
    * atomic shell. This method only works for shells which are occupied in
    * atomic ground state. Otherwise this method returns NaN. (The shell energy
    * is computed as the sum of the edge energy and the atomic ionization
    * energy.)
    * 
    * @return double - in Joules
    */
   public double getEnergy() {
      return getGroundStateOccupancy() > 0 ? getEdgeEnergy(mElement, mShell) + mElement.getIonizationEnergy() : Double.NaN;
   }

   /**
    * getElement - Returns the element in which this shell is located.
    * 
    * @return Element
    */
   public Element getElement() {
      return mElement;
   }

   /**
    * getShell - Get the shell index associated with this shell.
    * 
    * @return int
    */
   public int getShell() {
      return mShell;
   }

   @Override
   public String toString() {
      return mElement.toAbbrev() + " " + AtomicShell.getIUPACName(mShell);
   }

   /**
    * parseString - The inverse of AtomicShell.toString(). Parses the contents
    * of a String as produced by toString() and returns the equivalent
    * AtomicShell object.
    * 
    * @param str
    * @return An AtomicShell representing the same item as String.
    */
   static public AtomicShell parseString(String str) {
      final int p = str.indexOf(' ');
      return new AtomicShell(Element.byName(str.substring(0, p)), parseIUPACName(str.substring(p + 1, str.length())));
   }

   @Override
   public int compareTo(AtomicShell otherShell) {
      final int an = mElement.getAtomicNumber();
      final int ano = otherShell.mElement.getAtomicNumber();
      if(an < ano)
         return -1;
      else if(an == ano) {
         if(mShell < otherShell.mShell)
            return -1;
         else if(mShell == otherShell.mShell)
            return 0;
         else
            return 1;
      } else
         return 1;
   }

   @Override
   public Object clone() {
      return new AtomicShell(mElement, mShell);
   }

   @Override
   public int hashCode() {
      // mShell < 1024 = 1<<10.
      return mElement.hashCode() ^ (mShell << 14);
   }

   @Override
   public boolean equals(Object obj) {
      if(obj instanceof AtomicShell) {
         final AtomicShell sh = (AtomicShell) obj;
         return (sh.mElement == mElement) && (sh.mShell == mShell);
      } else
         return false;
   }

   /**
    * exists - Does the shell exist for this element (as evidenced by a non-zero
    * edge energy.)
    * 
    * @param elm Element
    * @param shell int
    * @return boolean
    */
   static public boolean exists(Element elm, int shell) {
      return getEdgeEnergy(elm, shell) > 0.0;
   }

   /**
    * exists - Does the atomic shell represented by this AtomicShell object
    * reflect an existing atomic shell (as evidenced by a non-zero edge energy.)
    * 
    * @return boolean
    */
   public boolean exists() {
      return getEdgeEnergy() > 0.0;
   }

   /**
    * getAngularMomentum - Returns the angular momentum quantum number
    * (typically denoted L) associated with the argument shell.
    * 
    * @param shell int
    * @return int
    */
   public int getOrbitalAngularMomentum(int shell) {
      return mOrbitalAngularMomentum[shell]; // S->0, P->1, D->2, F->3
   }

   /**
    * getAngularMomentum - Returns the angular momentum quantum number
    * (typically denoted L) associated with this shell.
    * 
    * @return int
    */
   public int getOrbitalAngularMomentum() {
      return mOrbitalAngularMomentum[mShell]; // S->0, P->1, D->2, F->3
   }

   /**
    * getTotalAngularMomentum - Returns the total angular momentum (typically
    * denoted J) associated with this shell.
    * 
    * @return double
    */
   public double getTotalAngularMomentum() {
      return mTotalAngularMomentum[mShell];
   }

   /**
    * getTotalAngularMomentum - Returns the total angular momentum (typically
    * denoted J) associated with the argument shell.
    * 
    * @param shell int
    * @return double
    */
   static public double getTotalAngularMomentum(int shell) {
      return mTotalAngularMomentum[shell];
   }

   /**
    * electricDipolePermitted - Are transitions from the shell represented by
    * the enumerated constant sh1 to the shell sh2 permitted by the electric
    * dipole selection rules?
    * 
    * @param shell1 int - An integer constant representing a shell
    * @param shell2 int - An integer constant representing a shell
    * @return boolean
    */
   public static boolean electricDipolePermitted(int shell1, int shell2) {
      { // deltaJ=0,+1,-1 but no 0->0
         final double deltaJ = Math.abs(mTotalAngularMomentum[shell1] - mTotalAngularMomentum[shell2]);
         if(deltaJ > 1.0)
            return false;
         assert ((deltaJ == 0.0) || (deltaJ == 1.0));
         // J=0->J=0
         // if((mTotalAngularMomentum[shell1] == 0.0) &&
         // (mTotalAngularMomentum[shell2] == 0.0))
         // return false;
      }
      // deltaL=+1,-1
      return Math.abs(mOrbitalAngularMomentum[shell1] - mOrbitalAngularMomentum[shell2]) == 1.0;
   }

   public static boolean electricQuadrupolePermitted(int shell1, int shell2) {
      { // deltaJ=0,+1,-1,+2,-2 but no 0->0
         final double deltaJ = Math.abs(mTotalAngularMomentum[shell1] - mTotalAngularMomentum[shell2]);
         if(deltaJ > 2.0)
            return false;
         assert ((deltaJ == 0.0) || (deltaJ == 1.0) || (deltaJ == 2.0));
         if((mTotalAngularMomentum[shell1] == 0.5) && (mTotalAngularMomentum[shell2] == 0.5))
            return false;
      }
      // deltaL=0,+2,-2
      final double deltaL = Math.abs(mOrbitalAngularMomentum[shell1] - mOrbitalAngularMomentum[shell2]);
      return (deltaL == 0.0) || (deltaL == 2.0);
   }
};
