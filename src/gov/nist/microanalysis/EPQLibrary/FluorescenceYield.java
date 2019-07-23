package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

import gov.nist.microanalysis.Utility.CSVReader;

/**
 * <p>
 * Various different classes implementing returning the FluorescenceYield. The
 * FY is commonly available in two different forms - mean into a family of
 * shells or specifically into one shell. In the case of a specific shell, the
 * fluorescence yield is a measure of the likelyhood that the shell will be
 * filled by a electron from a higher energy shell and that an x-ray will be
 * emitted in the process. No mention is made of which higher energy shell will
 * provide the electron. In the case of the mean yield, there is some ambiguity.
 * Since the FY is different for each shell in a family, the mean yield really
 * depends upon the likelyhood of each family shell being empty. This depends on
 * parameters like the mechanism of ionization that are not included in the FY
 * model. But that is life and deal with it we must.
 * </p>
 * <p>
 * Note: FluorescenceYield data and models are readily available for the K and L
 * lines but not the M lines.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */
abstract public class FluorescenceYield
   extends AlgorithmClass {

   private static final AlgorithmClass[] mAllImplementations = {
      FluorescenceYield.Krause79,
      FluorescenceYield.ENDLIB97
   };

   protected String defaultErrorMsg(AtomicShell sh) {
      return "Fluorescence yields unavailable for " + sh.toString() + " using " + toString();
   }

   protected FluorescenceYield(String name, String ref) {
      super("Fluorescence Yield", name, ref);
   }

   protected FluorescenceYield(String name, LitReference ref) {
      super("Fluorescence Yield", name, ref);
   }

   @Override
   protected void initializeDefaultStrategy() {
      // Don't do anything...
   }

   /**
    * getAllImplementations - Returns a full list of all available algorithms.
    * Each item is an implements the FluorescenceYield abstract class.
    * 
    * @return List
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   /**
    * compute - Computes the fluorescence yield for transitions into the
    * specific shell, K, LI, LII, LIII, MI, ... MV. It seems that there are not
    * actually available any numbers for the MI...NV shells. So in all
    * likelyhood calling this method with these shells as arguments will cause
    * an exception.
    * 
    * @param sh AtomicShell
    * @return double
    */
   abstract public double compute(AtomicShell sh);

   /**
    * Krause79 - Krause 1979 as quoted in the Handbook of X-ray Spectrometery,
    * Grieken &amp; Markowitz editors
    */
   static public class KrauseFluorescenceYield
      extends FluorescenceYield {
      KrauseFluorescenceYield() {
         super("Krause 1979", "Krause 1979 as quoted in the Handbook of X-ray Spectrometery, Grieken & Markowitz editors");
      }

      private double[][] mYields;

      private void load() {
         try {
            mYields = (new CSVReader.ResourceReader("Krause1979.csv", true)).getResource(FluorescenceYield.class);
         }
         catch(final Exception ex) {
            throw new EPQFatalException("Fatal error while attempting to load the flourescence yields data file");
         }
      }

      @Override
      public double compute(AtomicShell sh)
            throws EPQFatalException {
         if(mYields == null)
            load();
         final int zp = sh.getElement().getAtomicNumber() - Element.elmH;
         final int sp = sh.getShell() - AtomicShell.K;
         assert (sp >= 0);
         if((zp < mYields.length) && (sp <= (AtomicShell.LIII - AtomicShell.K)))
            return sp < mYields[zp].length ? mYields[zp][sp] : 0.0;
         else
            throw new EPQFatalException(defaultErrorMsg(sh));
      }
   }

   public static final FluorescenceYield Krause79 = new KrauseFluorescenceYield();

   static public class SogutFluorescenceYield
      extends FluorescenceYield {
      public SogutFluorescenceYield() {
         super("S�?�t 2002", "S�?�t �, B�y�kkasap E, K���k�nder A, Ertu?rul M, Do?an O, Erdo?an H, ?im?ek �. X-Ray Spectrom 31, 62-70 (2002)");
      }

      @Override
      public double compute(AtomicShell sh) {
         if(sh.getFamily() == AtomicShell.MFamily) {
            final int z = sh.getElement().getAtomicNumber();
            if(z > 90)
               throw new EPQFatalException(defaultErrorMsg(sh));
            double a0 = 0.0, a1 = 0.0, a2 = 0.0, a3 = 0.0;
            switch(sh.getShell()) {
               case AtomicShell.MI:
                  if(z >= 20)
                     if(z <= 49) {
                        a0 = 0.0005;
                        a1 = -0.00004;
                        a2 = 6.3425e-7; // Ok
                     } else if(z <= 79) {
                        a0 = -0.00254;
                        a1 = 0.00006; // Ok
                     } else if(z <= 90) {
                        a0 = -0.01485;
                        a1 = 0.00022; // Ok
                     }
                  break;
               case AtomicShell.MII:
                  if(z >= 20)
                     if(z <= 36) {
                        a0 = 0.00002;
                        a1 = -5.665e-24; // ?????
                     } else if(z <= 56) {
                        a0 = -0.00221;
                        a1 = 0.00006; // Ok
                     } else if(z <= 90) {
                        a0 = 0.07255;
                        a1 = -0.00225;
                        a2 = 0.00002; // Ok
                     }
                  break;
               case AtomicShell.MIII:
                  if(z >= 35)
                     if(z <= 58) {
                        a0 = -0.00336;
                        a1 = 0.00024;
                        a2 = -6.2005e-6;
                        a3 = 5.6949e-8;
                     } else if(z <= 72) {
                        a0 = -0.00249;
                        a1 = 0.00006;
                        a2 = -7.085e-20;
                     } else if(z <= 90) {
                        a0 = -0.0226;
                        a1 = 0.00034;
                     }
                  break;
               case AtomicShell.MIV:
                  if(z >= 32)
                     if(z <= 60) {
                        a0 = 0.0027;
                        a1 = -3.8017e-21; // ????
                     } else if(z <= 90) {
                        a0 = 0.27987;
                        a1 = -0.00872;
                        a2 = 0.00007; // Ok
                     }
                  break;
               case AtomicShell.MV:
                  if(z >= 60) {
                     a0 = -0.08517;
                     a1 = 0.00144;
                  }
                  break;
            }
            return a0 + (z * (a1 + (z * (a2 + (z * a3)))));
         } else
            throw new EPQFatalException(defaultErrorMsg(sh));
      }
   }

   static final public FluorescenceYield Sogut2002 = new SogutFluorescenceYield();

   static public class McMaster
      extends FluorescenceYield {

      static private double[] k_yield = {
         /* 1 */0.000000E+00, /* 2 */
         0.000000E+00, /* 3 */
         0.000000E+00,
         /* 4 */0.000000E+00, /* 5 */
         0.170000E-02, /* 6 */
         0.280000E-02,
         /* 7 */0.520000E-02, /* 8 */
         0.830000E-02, /* 9 */
         0.130000E-01,
         /* 10 */0.180000E-01, /* 11 */
         0.230000E-01, /* 12 */
         0.300000E-01,
         /* 13 */0.390000E-01, /* 14 */
         0.500000E-01, /* 15 */
         0.630000E-01,
         /* 16 */0.780000E-01, /* 17 */
         0.970000E-01, /* 18 */
         0.118000E+00,
         /* 19 */0.140000E+00, /* 20 */
         0.163000E+00, /* 21 */
         0.188000E+00,
         /* 22 */0.214000E+00, /* 23 */
         0.243000E+00, /* 24 */
         0.275000E+00,
         /* 25 */0.308000E+00, /* 26 */
         0.340000E+00, /* 27 */
         0.373000E+00,
         /* 28 */0.406000E+00, /* 29 */
         0.440000E+00, /* 30 */
         0.474000E+00,
         /* 31 */0.507000E+00, /* 32 */
         0.535000E+00, /* 33 */
         0.562000E+00,
         /* 34 */0.589000E+00, /* 35 */
         0.618000E+00, /* 36 */
         0.643000E+00,
         /* 37 */0.667000E+00, /* 38 */
         0.690000E+00, /* 39 */
         0.710000E+00,
         /* 40 */0.730000E+00, /* 41 */
         0.747000E+00, /* 42 */
         0.765000E+00,
         /* 43 */0.780000E+00, /* 44 */
         0.794000E+00, /* 45 */
         0.808000E+00,
         /* 46 */0.820000E+00, /* 47 */
         0.831000E+00, /* 48 */
         0.843000E+00,
         /* 49 */0.853000E+00, /* 50 */
         0.862000E+00, /* 51 */
         0.870000E+00,
         /* 52 */0.877000E+00, /* 53 */
         0.884000E+00, /* 54 */
         0.891000E+00,
         /* 55 */0.897000E+00, /* 56 */
         0.902000E+00, /* 57 */
         0.907000E+00,
         /* 58 */0.912000E+00, /* 59 */
         0.917000E+00, /* 60 */
         0.921000E+00,
         /* 61 */0.925000E+00, /* 62 */
         0.929000E+00, /* 63 */
         0.932000E+00,
         /* 64 */0.935000E+00, /* 65 */
         0.938000E+00, /* 66 */
         0.941000E+00,
         /* 67 */0.944000E+00, /* 68 */
         0.947000E+00, /* 69 */
         0.949000E+00,
         /* 70 */0.951000E+00, /* 71 */
         0.953000E+00, /* 72 */
         0.955000E+00,
         /* 73 */0.957000E+00, /* 74 */
         0.958000E+00, /* 75 */
         0.959000E+00,
         /* 76 */0.961000E+00, /* 77 */
         0.962000E+00, /* 78 */
         0.963000E+00,
         /* 79 */0.964000E+00, /* 80 */
         0.965000E+00, /* 81 */
         0.966000E+00,
         /* 82 */0.967000E+00, /* 83 */
         0.968000E+00, /* 84 */
         0.968000E+00,
         /* 85 */0.969000E+00, /* 86 */
         0.969000E+00, /* 87 */
         0.970000E+00,
         /* 88 */0.970000E+00, /* 89 */
         0.971000E+00, /* 90 */
         0.971000E+00,
         /* 91 */0.972000E+00, /* 92 */
         0.972000E+00, /* 93 */
         0.973000E+00,
         /* 94 */0.973000E+00
      };

      /* L1/L2/L3 fluorescent yields */

      static private final double[][] l_yield = {
         /* 1 */{
            0.000000E+00,
            0.000000E+00,
            0.000000E+00
         },
         /* 2 */{
            0.000000E+00,
            0.000000E+00,
            0.000000E+00
         },
         /* 3 */{
            0.000000E+00,
            0.000000E+00,
            0.000000E+00
         },
         /* 4 */{
            0.000000E+00,
            0.000000E+00,
            0.000000E+00
         },
         /* 5 */{
            0.000000E+00,
            0.000000E+00,
            0.000000E+00
         },
         /* 6 */{
            0.000000E+00,
            0.000000E+00,
            0.000000E+00
         },
         /* 7 */{
            0.000000E+00,
            0.000000E+00,
            0.000000E+00
         },
         /* 8 */{
            0.000000E+00,
            0.000000E+00,
            0.000000E+00
         },
         /* 9 */{
            0.000000E+00,
            0.000000E+00,
            0.000000E+00
         },
         /* 10 */{
            0.000000E+00,
            0.000000E+00,
            0.000000E+00
         },
         /* 11 */{
            0.000000E+00,
            0.000000E+00,
            0.000000E+00
         },
         /* 12 */{
            0.290000E-04,
            0.120000E-02,
            0.120000E-02
         },
         /* 13 */{
            0.260000E-04,
            0.750000E-03,
            0.750000E-03
         },
         /* 14 */{
            0.300000E-04,
            0.370000E-03,
            0.380000E-03
         },
         /* 15 */{
            0.390000E-04,
            0.310000E-03,
            0.310000E-03
         },
         /* 16 */{
            0.740000E-04,
            0.260000E-03,
            0.260000E-03
         },
         /* 17 */{
            0.120000E-03,
            0.240000E-03,
            0.240000E-03
         },
         /* 18 */{
            0.180000E-03,
            0.220000E-03,
            0.220000E-03
         },
         /* 19 */{
            0.240000E-03,
            0.270000E-03,
            0.270000E-03
         },
         /* 20 */{
            0.310000E-03,
            0.330000E-03,
            0.330000E-03
         },
         /* 21 */{
            0.390000E-03,
            0.840000E-03,
            0.840000E-03
         },
         /* 22 */{
            0.470000E-03,
            0.150000E-02,
            0.150000E-02
         },
         /* 23 */{
            0.580000E-03,
            0.260000E-02,
            0.260000E-02
         },
         /* 24 */{
            0.710000E-03,
            0.370000E-02,
            0.370000E-02
         },
         /* 25 */{
            0.840000E-03,
            0.500000E-02,
            0.500000E-02
         },
         /* 26 */{
            0.100000E-02,
            0.630000E-02,
            0.630000E-02
         },
         /* 27 */{
            0.120000E-02,
            0.770000E-02,
            0.770000E-02
         },
         /* 28 */{
            0.140000E-02,
            0.860000E-02,
            0.930000E-02
         },
         /* 29 */{
            0.160000E-02,
            0.100000E-01,
            0.110000E-01
         },
         /* 30 */{
            0.180000E-02,
            0.110000E-01,
            0.120000E-01
         },
         /* 31 */{
            0.210000E-02,
            0.120000E-01,
            0.130000E-01
         },
         /* 32 */{
            0.240000E-02,
            0.130000E-01,
            0.150000E-01
         },
         /* 33 */{
            0.280000E-02,
            0.140000E-01,
            0.160000E-01
         },
         /* 34 */{
            0.320000E-02,
            0.160000E-01,
            0.180000E-01
         },
         /* 35 */{
            0.360000E-02,
            0.180000E-01,
            0.200000E-01
         },
         /* 36 */{
            0.410000E-02,
            0.200000E-01,
            0.220000E-01
         },
         /* 37 */{
            0.460000E-02,
            0.220000E-01,
            0.240000E-01
         },
         /* 38 */{
            0.510000E-02,
            0.240000E-01,
            0.260000E-01
         },
         /* 39 */{
            0.590000E-02,
            0.260000E-01,
            0.280000E-01
         },
         /* 40 */{
            0.680000E-02,
            0.280000E-01,
            0.310000E-01
         },
         /* 41 */{
            0.940000E-02,
            0.310000E-01,
            0.340000E-01
         },
         /* 42 */{
            0.100000E-01,
            0.340000E-01,
            0.370000E-01
         },
         /* 43 */{
            0.110000E-01,
            0.370000E-01,
            0.400000E-01
         },
         /* 44 */{
            0.120000E-01,
            0.400000E-01,
            0.430000E-01
         },
         /* 45 */{
            0.130000E-01,
            0.430000E-01,
            0.460000E-01
         },
         /* 46 */{
            0.140000E-01,
            0.470000E-01,
            0.490000E-01
         },
         /* 47 */{
            0.160000E-01,
            0.510000E-01,
            0.520000E-01
         },
         /* 48 */{
            0.180000E-01,
            0.560000E-01,
            0.560000E-01
         },
         /* 49 */{
            0.200000E-01,
            0.610000E-01,
            0.600000E-01
         },
         /* 50 */{
            0.370000E-01,
            0.650000E-01,
            0.640000E-01
         },
         /* 51 */{
            0.390000E-01,
            0.690000E-01,
            0.690000E-01
         },
         /* 52 */{
            0.410000E-01,
            0.740000E-01,
            0.740000E-01
         },
         /* 53 */{
            0.440000E-01,
            0.790000E-01,
            0.790000E-01
         },
         /* 54 */{
            0.460000E-01,
            0.830000E-01,
            0.850000E-01
         },
         /* 55 */{
            0.490000E-01,
            0.900000E-01,
            0.910000E-01
         },
         /* 56 */{
            0.520000E-01,
            0.960000E-01,
            0.970000E-01
         },
         /* 57 */{
            0.550000E-01,
            0.103000E+00,
            0.104000E+00
         },
         /* 58 */{
            0.580000E-01,
            0.110000E+00,
            0.111000E+00
         },
         /* 59 */{
            0.610000E-01,
            0.117000E+00,
            0.118000E+00
         },
         /* 60 */{
            0.640000E-01,
            0.124000E+00,
            0.125000E+00
         },
         /* 61 */{
            0.660000E-01,
            0.132000E+00,
            0.132000E+00
         },
         /* 62 */{
            0.710000E-01,
            0.140000E+00,
            0.139000E+00
         },
         /* 63 */{
            0.750000E-01,
            0.149000E+00,
            0.147000E+00
         },
         /* 64 */{
            0.790000E-01,
            0.158000E+00,
            0.155000E+00
         },
         /* 65 */{
            0.830000E-01,
            0.167000E+00,
            0.164000E+00
         },
         /* 66 */{
            0.890000E-01,
            0.178000E+00,
            0.174000E+00
         },
         /* 67 */{
            0.940000E-01,
            0.189000E+00,
            0.182000E+00
         },
         /* 68 */{
            0.100000E+00,
            0.200000E+00,
            0.192000E+00
         },
         /* 69 */{
            0.106000E+00,
            0.211000E+00,
            0.201000E+00
         },
         /* 70 */{
            0.112000E+00,
            0.222000E+00,
            0.210000E+00
         },
         /* 71 */{
            0.120000E+00,
            0.234000E+00,
            0.220000E+00
         },
         /* 72 */{
            0.128000E+00,
            0.246000E+00,
            0.231000E+00
         },
         /* 73 */{
            0.137000E+00,
            0.258000E+00,
            0.243000E+00
         },
         /* 74 */{
            0.147000E+00,
            0.270000E+00,
            0.255000E+00
         },
         /* 75 */{
            0.144000E+00,
            0.283000E+00,
            0.268000E+00
         },
         /* 76 */{
            0.130000E+00,
            0.295000E+00,
            0.281000E+00
         },
         /* 77 */{
            0.120000E+00,
            0.308000E+00,
            0.294000E+00
         },
         /* 78 */{
            0.114000E+00,
            0.321000E+00,
            0.306000E+00
         },
         /* 79 */{
            0.107000E+00,
            0.334000E+00,
            0.320000E+00
         },
         /* 80 */{
            0.107000E+00,
            0.347000E+00,
            0.333000E+00
         },
         /* 81 */{
            0.107000E+00,
            0.360000E+00,
            0.347000E+00
         },
         /* 82 */{
            0.112000E+00,
            0.373000E+00,
            0.360000E+00
         },
         /* 83 */{
            0.117000E+00,
            0.387000E+00,
            0.373000E+00
         },
         /* 84 */{
            0.122000E+00,
            0.401000E+00,
            0.386000E+00
         },
         /* 85 */{
            0.128000E+00,
            0.415000E+00,
            0.399000E+00
         },
         /* 86 */{
            0.134000E+00,
            0.429000E+00,
            0.411000E+00
         },
         /* 87 */{
            0.139000E+00,
            0.443000E+00,
            0.424000E+00
         },
         /* 88 */{
            0.146000E+00,
            0.456000E+00,
            0.437000E+00
         },
         /* 89 */{
            0.153000E+00,
            0.468000E+00,
            0.450000E+00
         },
         /* 90 */{
            0.161000E+00,
            0.479000E+00,
            0.463000E+00
         },
         /* 91 */{
            0.162000E+00,
            0.472000E+00,
            0.476000E+00
         },
         /* 92 */{
            0.176000E+00,
            0.467000E+00,
            0.489000E+00
         },
         /* 93 */{
            0.187000E+00,
            0.466000E+00,
            0.502000E+00
         },
         /* 94 */{
            0.205000E+00,
            0.464000E+00,
            0.514000E+00
         }
      };

      McMaster() {
         super("McMaster 1969", "http://csrri.iit.edu/mucal-src/mucal_c-1.3.tar.gz");
      }

      @Override
      public double compute(AtomicShell sh) {
         final int index = sh.getElement().getAtomicNumber() - 1;
         switch(sh.getShell()) {
            case AtomicShell.K:
               return k_yield[index];
            case AtomicShell.LI:
               return l_yield[index][0];
            case AtomicShell.LII:
               return l_yield[index][1];
            case AtomicShell.LIII:
               return l_yield[index][2];
            default:
               assert false : "Not supported";
         }
         return 0;
      }
   };

   /**
    * Default - The default implementation of FluorescenceYield.compute(...).
    */
   static public class DefaultFluorescenceYield
      extends FluorescenceYield {
      public DefaultFluorescenceYield() {
         super(Krause79.getName() + " + " + Sogut2002.getName(), Krause79.getReference() + " & " + Sogut2002.getReference());
      }

      @Override
      public double compute(AtomicShell sh) {
         return sh.getFamily() == AtomicShell.MFamily ? Sogut2002.compute(sh) : Krause79.compute(sh);
      }
   }

   static final public FluorescenceYield DefaultShell = new DefaultFluorescenceYield();

   /**
    * Fluorescence yields computed from the ENDLIB97 database of transition
    * probabilities.
    */
   static private class ENDLIB97Yield
      extends FluorescenceYield {

      ENDLIB97Yield() {
         super("ENDLIB97", LitReference.ENDLIB97_Relax);
      }

      @Override
      public double compute(AtomicShell sh) {
         return TransitionProbabilities.Default.fluorescenceYield(sh);
      }

   }

   /**
    * Fluorescence yields computed from the ENDLIB97 database of transition
    * probabilities.
    */
   static public final FluorescenceYield ENDLIB97 = new ENDLIB97Yield();

};
