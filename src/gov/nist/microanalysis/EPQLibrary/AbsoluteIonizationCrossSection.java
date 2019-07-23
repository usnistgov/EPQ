package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

import gov.nist.microanalysis.Utility.CSVReader;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * Description
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
abstract public class AbsoluteIonizationCrossSection
   extends
   IonizationCrossSection {
   private AbsoluteIonizationCrossSection(String name, String ref) {
      super(name, ref);
   }

   private AbsoluteIonizationCrossSection(String name, LitReference ref) {
      super(name, ref);
   }

   /**
    * <p>
    * The documentation from the file <code>xion.f</code> as sent to NWMR by
    * Cesc Salvat in early September 2008.
    * </p>
    * <p>
    * This implementation is based on DBPW computations of the ionization cross
    * section. It is likely to be as good or better than anything available
    * elsewhere. It particular there is very little data for the L and M shells
    * and this computation is likely to be the best resource for this
    * information.
    * </p>
    * <p>
    * This function delivers the total cross section for electron impact
    * ionization of K, L and M shells of neutral atoms of the elements from
    * hydrogen (IZ=1) to einsteinium (IZ=99). It uses a parameterization of
    * numerical cross sections computed with the distorted-wave and the
    * plane-wave first-Born approximations.
    * </p>
    * <h4>References:</h4>
    * <ul>
    * <li>D. Bote and F. Salvat, "Calculations of inner-shell ionization by
    * electron impact with the distorted-wave and plane-wave Born
    * approximations", Phys. Rev. A77, 042701 (2008).</li>
    * <li>D. Bote et al., "Cross sections for ionization of K, L and M shells of
    * atoms by impact of electrons and positrons with energies up to 1 GeV.
    * Analytical formulas", At. and Nucl. Data Tables (in preparation).</li>
    * </ul>
    * <h4>Input Arguments:</h4>
    * <ul>
    * <li>EEV ..... kinetic energy of the projectile electron (in eV).</li>
    * <li>IZ ...... atomic number of the target atom (IZ=1 to 99).</li>
    * <li>ISH ..... active target electron shell, 1=K, 2=L1, 3=L2, 4=L3, 5=M1,
    * ..., 9=M5.</li>
    * </ul>
    * <h4>Output value:</h4>
    * <ul>
    * <li>XIONE ... ionization cross section (in cm**2) of the ISH shell.</li>
    * </ul>
    * <p>
    * <i>D. Bote, F. Salvat, A. Jablonski and C.J. Powell (September 2008)</i>
    * </p>
    * <p>
    * Copyright: Pursuant to title 17 Section 105 of the United States Code this
    * software is not subject to copyright protection and is in the public
    * domain
    * </p>
    * <p>
    * Institution: National Institute of Standards and Technology
    * </p>
    * 
    * @author nicholas
    * @version 1.0
    */
   static class BoteSalvatCrossSection
      extends
      AbsoluteIonizationCrossSection {
      final static double FPIAB2 = 4.0 * Math.PI * Math2.sqr(FromSI.cm(PhysicalConstants.BohrRadius));
      final static double REV = FromSI.eV(PhysicalConstants.ElectronRestMass);
      static double[][][] mA; // nominally [100][9][5]
      static double[][] mBe; // nominally [100][9]
      static double[][] mAnlj; // nominally [100][9]
      static double[][][] mG; // nominally [100][9][4]

      private void initialize() {
         synchronized(BoteSalvatCrossSection.class) {
            if(mA == null) {
               {
                  final double[][] bTmp = (new CSVReader.ResourceReader("SalvatXion/SalvatXionB.csv", false)).getResource(BoteSalvatCrossSection.class);
                  assert bTmp.length == 99;
                  mBe = new double[100][];
                  mAnlj = new double[100][];
                  mG = new double[100][][];
                  final int B_BLOCK_SIZE = 6;
                  for(int z = 1; z <= bTmp.length; ++z) {
                     final double[] line = bTmp[z - 1];
                     assert Math.round(line[0]) == z;
                     final int len = line.length - 1;
                     assert (len % B_BLOCK_SIZE) == 0;
                     final int trCx = len / B_BLOCK_SIZE;
                     mBe[z] = new double[trCx];
                     mAnlj[z] = new double[trCx];
                     mG[z] = new double[trCx][4];
                     for(int i = 0; i < len; ++i) {
                        final int nn = i / B_BLOCK_SIZE;
                        switch(i % B_BLOCK_SIZE) {
                           case 0:
                              mBe[z][nn] = line[i + 1];
                              break;
                           case 1:
                              mAnlj[z][nn] = line[i + 1];
                              break;
                           default:
                              assert ((i % B_BLOCK_SIZE) - 2) >= 0;
                              assert ((i % B_BLOCK_SIZE) - 2) < 4;
                              mG[z][nn][(i % B_BLOCK_SIZE) - 2] = line[i + 1];
                              break;
                        }
                     }
                  }
               }
               {
                  final double[][] aTmp = (new CSVReader.ResourceReader("SalvatXion/SalvatXionA.csv", false)).getResource(BoteSalvatCrossSection.class);
                  assert aTmp.length == 99;
                  final double a[][][] = new double[100][][];
                  final int A_BLOCK_SIZE = 5;
                  for(int z = 1; z <= aTmp.length; ++z) {
                     final double[] line = aTmp[z - 1];
                     assert Math.round(line[0]) == z;
                     final int len = line.length - 1;
                     assert (len % A_BLOCK_SIZE) == 0;
                     final int trCx = len / A_BLOCK_SIZE;
                     a[z] = new double[trCx][A_BLOCK_SIZE];
                     for(int i = 0; i < len; ++i)
                        a[z][i / A_BLOCK_SIZE][i % A_BLOCK_SIZE] = line[i + 1];
                  }
                  mA = a;
               }
            }
         }
      }

      public BoteSalvatCrossSection() {
         super("Bote/Salvat-2008", new LitReference.JournalArticle(LitReference.PhysRevA, "77", "042701-1 to 24", 2008, new LitReference.Author[] {
            new LitReference.Author("David", "Bote", "Facultat de Física (ECM), Universitat de Barcelona, Diagonal 647, 08028 Barcelona, Spain"),
            LitReference.FSalvat
         }));
      }

      @Override
      protected void initializeDefaultStrategy() {
         addDefaultAlgorithm(EdgeEnergy.class, EdgeEnergy.DHSIonizationEnergy);
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.IonizationCrossSection#computeShell(gov.nist.microanalysis.EPQLibrary.AtomicShell,
       *      double)
       */
      @Override
      public double computeShell(AtomicShell shell, double energy) {
         if(mA == null)
            initialize();
         final double eev = FromSI.eV(energy);
         double xione = 0.0;
         final int iz = shell.getElement().getAtomicNumber();
         if((iz < Element.elmH) || (iz > Element.elmEs))
            throw new EPQFatalException("Unsupported element in BoteSalvatCrossSection.computeShell: "
                  + shell.getElement().toAbbrev());
         final int ish = shell.getShell();
         if(ish > AtomicShell.MV)
            throw new EPQFatalException("Unsupported shell in BoteSalvatCrossSection.computeShell: " + shell.toString());
         final EdgeEnergy ee = (EdgeEnergy) getAlgorithm(EdgeEnergy.class);
         if(ee.isSupported(shell)) {
            final double uev = FromSI.eV(ee.compute(shell));
            if((uev >= 1.0e-35) && (eev > uev)) {
               final double overV = eev / uev;
               if(overV <= 16.0) {
                  assert iz < mA.length : iz + ">=" + mA.length;
                  assert ish < mA[iz].length : ish + ">=" + mA[iz].length;
                  final double[] as = mA[iz][ish];
                  final double opu = 1.0 / (1.0 + overV);
                  final double opu2 = opu * opu;
                  final double ffitlo = as[0] + as[1] * overV + opu * (as[2] + opu2 * (as[3] + opu2 * as[4]));
                  xione = (overV - 1.0) * Math.pow(ffitlo / overV, 2.0);
               } else {
                  assert iz < mBe.length : iz + " >= " + mBe.length;
                  assert ish < mBe[iz].length : ish + " >= " + mBe[iz].length + " for " + shell;
                  assert ish < mG[iz].length : ish + ">= " + mG[iz].length + " for " + iz;
                  final double beta2 = (eev * (eev + (2.0 * REV))) / ((eev + REV) * (eev + REV));
                  final double x = Math.sqrt(eev * (eev + (2.0 * REV))) / REV;
                  final double[] g = mG[iz][ish];
                  final double ffitup = (((2.0 * Math.log(x)) - beta2) * (1.0 + (g[0] / x))) + g[1]
                        + (g[2] * Math.pow((REV * REV) / ((eev + REV) * (eev + REV)), 0.25)) + (g[3] / x);
                  final double factr = mAnlj[iz][ish] / beta2;
                  xione = ((factr * overV) / (overV + mBe[iz][ish])) * ffitup;
               }
            }
         }
         return ToSI.CM * ToSI.CM * FPIAB2 * xione;
      }

   };

   public static final AbsoluteIonizationCrossSection BoteSalvat2008 = new BoteSalvatCrossSection();

   /**
    * E Casnati, A Tartari &amp; C Baraldi, J Phys B15 (1982) 155 as quoted by C
    * Powell in Ultramicroscopy 28 (1989) 24-31 "(Casnati's equation) was found
    * to fit cross-section data to typically better than +-10% over the range
    * 1&lt;=Uk&lt;=20 and 6&lt;=Z&lt;=79." Note: This result is for K shell. L
    * &amp; M are much less well characterized. C. Powell indicated in
    * conversation that he believed that Casnati's expression was the best
    * available for L &amp; M also.
    */
   public static class CasnatiAbsoluteIonizationCrossSection
      extends
      AbsoluteIonizationCrossSection {
      CasnatiAbsoluteIonizationCrossSection() {
         super("Casnati 1982", "Casnati82 - E. Casnati, A. Tartari & C. Baraldi, J Phys B15 (1982) 155 as quoted by C. Powell in Ultramicroscopy 28 (1989) 24-31");
      }

      @Override
      public double computeShell(AtomicShell shell, double beamE) {
         // "(Casnati's equation) was found to fit cross-section data to
         // typically better than +-10% over the range 1<=Uk<=20 and 6<=Z<=79."
         // Note: This result is for K shell. L & M are much less well
         // characterized.
         // C. Powell indicated in conversation that he believed that Casnati's
         // expression was the best available for L & M also.
         double res = 0.0;
         final double ee = shell.getEdgeEnergy();
         final double u = beamE / ee;
         if(u > 1.0) {
            final double u2 = u * u;
            final double phi = 10.57 * Math.exp((-1.736 / u) + (0.317 / u2));
            final double psi = Math.pow(ee / PhysicalConstants.RydbergEnergy, -0.0318 + (0.3160 / u) + (-0.1135 / u2));
            final double i = ee / PhysicalConstants.ElectronRestMass;
            final double t = beamE / PhysicalConstants.ElectronRestMass;
            final double f = ((2.0 + i) / (2.0 + t)) * Math2.sqr((1.0 + t) / (1.0 + i))
                  * Math.pow(((i + t) * (2.0 + t) * Math2.sqr(1.0 + i))
                        / ((t * (2.0 + t) * Math2.sqr(1.0 + i)) + (i * (2.0 + i))), 1.5);
            res = ((shell.getGroundStateOccupancy()
                  * Math2.sqr((PhysicalConstants.BohrRadius * PhysicalConstants.RydbergEnergy) / ee) * f * psi * phi
                  * Math.log(u)) / u);
         }
         return res;
      }
   }

   public static final AbsoluteIonizationCrossSection Casnati82 = new CasnatiAbsoluteIonizationCrossSection();

   /**
    * Returns a list of all implementations of IonizationCrossSection
    * 
    * @return List
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   private static final AlgorithmClass[] mAllImplementations = {
      AbsoluteIonizationCrossSection.Casnati82,
      AbsoluteIonizationCrossSection.BoteSalvat2008
   };
}
