package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

import gov.nist.microanalysis.Utility.CSVReader;

/**
 * <p>
 * Various different classes implementing returning the mean fluorescence yeild
 * for a family of shells. The FY is commonly available in two different forms -
 * mean into a family of shells or specifically into one shell. In the case of a
 * specific shell, the fluorescence yield is a measure of the likelyhood that
 * the shell will be filled by a electron from a higher energy shell and that an
 * x-ray will be emitted in the process. No mention is made of which higher
 * energy shell will provide the electron. In the case of the mean yield, there
 * is some ambiguity. Since the FY is different for each shell in a family, the
 * mean yield really depends upon the likelyhood of each family shell being
 * empty. This depends on parameters like the mechanism of ionization that are
 * not included in the FY model. But that is life and deal with it we must.
 * </p>
 * <p>
 * Note: FluorescenceYieldMean data and models are readily available for the K
 * and L lines but not the M lines.
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
abstract public class FluorescenceYieldMean extends AlgorithmClass {

   private static final AlgorithmClass[] mAllImplementations = {FluorescenceYieldMean.Bambynek72, FluorescenceYieldMean.Dtsa,
         FluorescenceYieldMean.Hubbell, FluorescenceYieldMean.Oz1999};

   protected String defaultErrorMsg(AtomicShell sh) {
      return "Fluorescence yields unavailable for " + sh.toString() + " using " + toString();
   }

   protected FluorescenceYieldMean(String name, String ref) {
      super("Fluorescence Yield", name, ref);
   }

   protected FluorescenceYieldMean(String name, LitReference ref) {
      super("Fluorescence Yield", name, ref);
   }

   @Override
   protected void initializeDefaultStrategy() {
      // Don't do anything...
   }

   /**
    * getAllImplementations - Returns a full list of all available algorithms.
    * Each item is an implements the FluorescenceYieldMean abstract class.
    * 
    * @return List
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   /**
    * computeMean - Computes the mean fluorescence yield for transitions into
    * the family (K, L or M) in which this shell is a member.
    * 
    * @param sh
    *           AtomicShell
    * @return double
    */
   abstract public double compute(AtomicShell sh);

   /**
    * Dtsa - Extracted from the DTSA Physics.P source file. The M-family yield
    * algorithm is attributed to John Colby.
    */
   static public class DtsaFluorescenceYieldMean extends FluorescenceYieldMean {
      DtsaFluorescenceYieldMean() {
         super("DTSA", "NIST DTSA 3.0.1 Physics.P lines 1091 to 1137");
      }

      @Override
      public double compute(AtomicShell sh) {
         final double z = sh.getElement().getAtomicNumber();
         double d4 = 0.0;
         switch (sh.getFamily()) {
            case AtomicShell.KFamily :
               if ((z >= 4) && (z <= 95))
                  d4 = Math.pow(0.015 + (z * (0.0327 - (6.4e-7 * z * z))), 4.0);
               break;
            case AtomicShell.LFamily :
               if ((z >= 16) && (z <= 95))
                  d4 = Math.pow(-0.11107 + (z * (0.01368 - (2.17722e-7 * z * z))), 4.0);
               break;
            case AtomicShell.MFamily : // Attributed to John Colby in the DTSA
               // source
               if ((z >= 35) && (z <= 95))
                  d4 = Math.pow(-0.00036 + (z * (0.00386 - (2.0101e-7 * z * z))), 4.0);
               break;
            default :
               assert (false);
         }
         return d4 / (1.0 + d4);
      }
   }

   static final public FluorescenceYieldMean Dtsa = new DtsaFluorescenceYieldMean();

   /**
    * Hubbell - Hubbell 1989 &amp; 1994 as quoted in the Handbook of X-ray
    * Spectrometery, Grieken &amp; Markowitz editors
    */
   static public class HubbelFluorescenceYieldMean extends FluorescenceYieldMean {
      HubbelFluorescenceYieldMean() {
         super("Hubbell 1989 & 1994", "Hubbell 1989 & 1994 as quoted in the Handbook of X-ray Spectrometery, Grieken & Markowitz editors");
      }

      @Override
      public double compute(AtomicShell sh) {
         final double z = sh.getElement().getAtomicNumber();
         switch (sh.getFamily()) {
            case AtomicShell.KFamily :
               if (z <= 100.0) {
                  final double[] c = {0.0370, 0.03112, 5.44e-5, -1.25e-6};
                  double x = 0;
                  for (int i = 3; i >= 0; --i)
                     x = (x * z) + c[i];
                  x = Math.pow(x, 4.0);
                  return x / (1.0 + x);
               }
               break;
            case AtomicShell.LFamily :
               if (z < 3.0)
                  return 0.0;
               if (z < 37.0)
                  return 1.939e-8 * Math.pow(z, 3.8874);
               if (z <= 100.0) {
                  final double[] c = {0.17765, 0.00298937, 8.91297e-5, -2.67184e-7};
                  double x = 0;
                  for (int i = 3; i >= 0; --i)
                     x = (x * z) + c[i];
                  x = Math.pow(x, 4.0);
                  return x / (1.0 + x);
               }
               break;
            case AtomicShell.MFamily :
               if (z < 13.0)
                  return 0.0;
               if (z <= 100.0)
                  return 1.29e-9 * Math.pow(z - 13.0, 4.0);
               break;
         }
         throw new EPQFatalException(defaultErrorMsg(sh));
      }
   }

   static final public FluorescenceYieldMean Hubbell = new HubbelFluorescenceYieldMean();

   /**
    * Bambynek72 - Bambynek et al. 1972
    */
   public static class BambynekFluorescenceYieldMean extends FluorescenceYieldMean {
      BambynekFluorescenceYieldMean() {
         super("Bambynek 1972", "Bambynek et al. 1972");
      }

      private double[][] mYields;

      private void load() {
         try {
            synchronized (this) {
               if (mYields == null)
                  mYields = (new CSVReader.ResourceReader("FlourescenceYield.csv", true)).getResource(FluorescenceYieldMean.class);
            }
         } catch (final Exception ex) {
            throw new EPQFatalException("Fatal error while attempting to load the flourescence yields data file");
         }
      }

      @Override
      public double compute(AtomicShell sh) throws EPQFatalException {
         if (mYields == null)
            load();
         final int fam = sh.getFamily();
         final int zp = sh.getElement().getAtomicNumber() - Element.elmH;
         if ((zp < mYields.length) && (fam >= AtomicShell.KFamily) && (fam <= AtomicShell.LFamily))
            return (fam - AtomicShell.KFamily) < mYields[zp].length ? mYields[zp][fam - AtomicShell.KFamily] : 0.0;
         else
            throw new EPQFatalException(defaultErrorMsg(sh));
      }
   }

   static final public FluorescenceYieldMean Bambynek72 = new BambynekFluorescenceYieldMean();

   /**
    * Oz1999 - Functional form for the mean M-shell yield described in E. Oz, H.
    * Erdogan &amp; M. Ertugrul, X-Ray Spectrom 28, 199-202 (1999)
    */
   static public class OzFluorescenceYieldMean extends FluorescenceYieldMean {
      OzFluorescenceYieldMean() {
         super("Oz, Erdogan & Ertugrul 1999", "E. Oz, H. Erdogan & M. Ertugrul, X-Ray Spectrom 28, 199-202 (1999)");
      }

      private double[] mYields;

      @Override
      public double compute(AtomicShell sh) {
         if (sh.getFamily() == AtomicShell.MFamily) {
            final int z = sh.getElement().getAtomicNumber();
            if (z < Element.elmCu)
               return 0;
            if (mYields == null) {
               final double[][] tmp = (new CSVReader.ResourceReader("Oz1999.csv", true)).getResource(FluorescenceYieldMean.class);
               mYields = new double[tmp.length];
               for (int i = 0; i < mYields.length; ++i)
                  mYields[i] = (tmp[i] != null ? tmp[i][0] : 0.0);
            }
            return mYields[z - Element.elmCu];
         } else
            throw new EPQFatalException(defaultErrorMsg(sh));
      }
   }

   static final public FluorescenceYieldMean Oz1999 = new OzFluorescenceYieldMean();

   /**
    * DefaultMean - The default implementation of the mean fluorescence yield
    * based on a blend of Bambynek 1972 for K &amp; L families and Oz 1999 for
    * the M family.
    */
   static public class DefaultFluorescenceYieldMean extends FluorescenceYieldMean {
      DefaultFluorescenceYieldMean() {
         super("Bambynek 1972 + Oz 1999", "Combination of Bambynek 1972 (K & L) + Oz 1999 (M)");
      }

      @Override
      public double compute(AtomicShell sh) {
         return sh.getFamily() == AtomicShell.MFamily ? Oz1999.compute(sh) : Bambynek72.compute(sh);
      }
   }

   static final public FluorescenceYieldMean DefaultMean = new DefaultFluorescenceYieldMean();
};
