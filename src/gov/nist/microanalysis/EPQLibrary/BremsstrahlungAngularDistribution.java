package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

import gov.nist.microanalysis.Utility.CSVReader;
import gov.nist.microanalysis.Utility.Math2;

import flanagan.interpolation.TriCubicSpline;

/**
 * <p>
 * These classes implement methods for calculating the angular distribution of
 * Bremsstrahlung radiation.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 *
 * @author nritchie
 * @version 1.0
 */
abstract public class BremsstrahlungAngularDistribution extends AlgorithmClass {

   /**
    * <p>
    * The base class for AcostaAngularDistribution and
    * AcostaAngularDistributionL which implement cubic spline interpolated and
    * linear interpolated versions of Acosta's Bremsstrahlung angular
    * distributions.
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
    * @author nritchie
    * @version 1.0
    */
   abstract static class AcostaBase extends BremsstrahlungAngularDistribution {

      protected AcostaBase(String name) {
         super("Acosta 2002 (" + name + ")",
               new LitReference.JournalArticle("Monte Carlo simulation of bremsstrahlung emission by electrons", LitReference.ApplPhysLett, "80",
                     "3228-3230", 2002, new LitReference.Author[]{new LitReference.Author("E.", "Acosta", "Universidad Nacional de Cordoba"),
                           new LitReference.Author("X.", "Llovet", "Universitat de Barcelona"), LitReference.FSalvat}));
         readAcosta();
      }

      protected static final double[] ACOSTA_BETA = {Bremsstrahlung.beta(ToSI.keV(1.0)), Bremsstrahlung.beta(ToSI.keV(5.0)),
            Bremsstrahlung.beta(ToSI.keV(10.0)), Bremsstrahlung.beta(ToSI.keV(50.0)), Bremsstrahlung.beta(ToSI.keV(100.0)),
            Bremsstrahlung.beta(ToSI.keV(500.0)),};
      protected static final double[] ACOSTA_Z = {2.0, 8.0, 13.0, 47.0, 79.0, 92.0};
      protected static final double[] ACOSTA_KoT = {0.0, 0.6, 0.8, 0.95};

      protected double[][][] mACoeff;
      protected double[][][] mBCoeff;

      protected void readAcosta() {
         mACoeff = new double[ACOSTA_BETA.length][ACOSTA_Z.length][ACOSTA_KoT.length];
         mBCoeff = new double[ACOSTA_BETA.length][ACOSTA_Z.length][ACOSTA_KoT.length];
         final CSVReader.ResourceReader reader = new CSVReader.ResourceReader("Acosta2002.csv", false);
         final double[][] tmp = reader.getResource(this.getClass());
         assert tmp.length == (mACoeff.length * mACoeff[0].length * mACoeff[0][0].length);
         int i = 0;
         for (int kev = 0; kev < ACOSTA_BETA.length; ++kev)
            for (int z = 0; z < ACOSTA_Z.length; ++z)
               for (int kot = 0; kot < ACOSTA_KoT.length; ++kot, ++i) {
                  final double[] data = tmp[i];
                  assert Math.abs(data[0] - ACOSTA_BETA[kev]) < 1.0e-5 : data[0] + "  " + i + "  " + kev + "  " + z + "  " + kot;
                  assert Math.abs(data[1] - ACOSTA_Z[z]) < 1.0e-5 : data[1] + "  " + i + "  " + kev + "  " + z + "  " + kot;
                  assert Math.abs(data[2] - ACOSTA_KoT[kot]) < 1.0e-5 : data[2] + "  " + i + "  " + kev + "  " + z + "  " + kot;
                  mACoeff[kev][z][kot] = Math.log(data[3] * ACOSTA_Z[z] * ACOSTA_BETA[kev]);
                  mBCoeff[kev][z][kot] = data[4] * ACOSTA_BETA[kev];
               }
      }

      protected static double acostaAngular(double theta, final double beta, final double a, final double b) {
         final double betaP = beta * (1.0 + b);
         final double ct = Math.cos(theta);
         final double x1 = Math2.sqr((ct - betaP) / (1.0 - (betaP * ct)));
         final double x2 = (1.0 - (betaP * betaP)) / (Math2.sqr(1.0 - (betaP * ct)));
         // see normalize.nb in Support
         final double norm = (16.0 - 7.0 * a) / 18.0;
         return (x2 * (0.375 * a * (1.0 + x1) + (4.0 / 3.0) * (1 - a) * (1.0 - x1))) / norm;
      }
   }
   public static class AcostaAngularDistribution extends AcostaBase {

      private static TriCubicSpline mSplineA;
      private static TriCubicSpline mSplineB;

      public AcostaAngularDistribution() {
         super("Cubic");
      }

      /**
       * The Lorentz transformed angular distribution for Bremsstrahlung
       * radiation from an energetic electron in the Coulombic field of an atom.
       * 
       * @param theta
       *           double - The emission angle
       * @param energy
       *           double - The energy of the incident electron in Joules
       * @param bremE
       *           double - The energy of the bremsstrahlung photon in Joules
       * @return double
       */
      @Override
      public double compute(Element elm, double theta, double energy, double bremE) {
         if (mSplineA == null)
            synchronized (Bremsstrahlung.class) {
               if (mSplineA == null) {
                  assert mACoeff != null;
                  mSplineA = new TriCubicSpline(ACOSTA_BETA, ACOSTA_Z, ACOSTA_KoT, mACoeff);
                  mSplineB = new TriCubicSpline(ACOSTA_BETA, ACOSTA_Z, ACOSTA_KoT, mBCoeff);
               }
            }
         if (energy > ToSI.keV(500.0))
            energy = ToSI.keV(500.0);
         final double beta = Math.max(Bremsstrahlung.beta(energy), ACOSTA_BETA[0]);
         final double kot = Math.min(bremE / energy, 0.95);
         final double z = Math2.bound(elm.getAtomicNumber(), 2, 93);
         final double a = Math.exp(mSplineA.interpolate(beta, z, kot)) / (z * beta);
         final double b = mSplineB.interpolate(beta, z, kot) / beta;
         return acostaAngular(theta, beta, a, b);
      }
   }
   public static class AcostaAngularDistributionL extends AcostaBase {

      /**
       * Returns the index i such that beta is between ACOSTA_BETA[i] and
       * ACOSTA_BETA[i+1].
       *
       * @param beta
       * @return int
       */
      private final int betaIndex(double beta) {
         for (int i = 1; i < ACOSTA_BETA.length; ++i)
            if (beta <= ACOSTA_BETA[i])
               return i - 1;
         return ACOSTA_BETA.length - 2;
      }

      private final int zIndex(Element elm) {
         final double z = elm.getAtomicNumber();
         for (int i = 1; i < ACOSTA_Z.length; ++i)
            if (z <= ACOSTA_Z[i])
               return i - 1;
         return ACOSTA_Z.length - 2;
      }

      private final int kotIndex(double kot) {
         for (int i = 1; i < ACOSTA_KoT.length; ++i)
            if (kot <= ACOSTA_KoT[i])
               return i - 1;
         return ACOSTA_KoT.length - 2;
      }

      public AcostaAngularDistributionL() {
         super("Linear");
      }

      private double interpolate(int zi, double zd, int betai, double betad, int koti, double kotd, double[][][] coeff) {
         final double ombetad = 1.0 - betad, omzd = 1.0 - zd, omkotd = 1.0 - kotd;
         return coeff[betai][zi][koti] * ombetad * omzd * omkotd + coeff[betai + 1][zi][koti] * betad * omzd * omkotd //
               + coeff[betai][zi + 1][koti] * ombetad * zd * omkotd + coeff[betai][zi][koti + 1] * ombetad * omzd * kotd //
               + coeff[betai + 1][zi + 1][koti] * betad * zd * omkotd + coeff[betai + 1][zi][koti + 1] * betad * omzd * kotd + //
               coeff[betai][zi + 1][koti + 1] * ombetad * zd * kotd + coeff[betai + 1][zi + 1][koti + 1] * betad * zd * kotd;
      }

      /**
       * The Lorentz transformed angular distribution for Bremsstrahlung
       * radiation from an energetic electron in the Coulombic field of an atom.
       * 
       * @param theta
       *           double - The emission angle
       * @param energy
       *           double - The energy of the incident electron in Joules
       * @param bremE
       *           double - The energy of the bremsstrahlung photon in Joules
       * @return double
       */
      @Override
      public double compute(Element elm, double theta, double energy, double bremE) {
         assert mACoeff != null;
         final double beta = Bremsstrahlung.beta(Math2.bound(energy, ToSI.keV(1.0), ToSI.keV(500.0)));
         final double kot = bremE / energy;
         final double z = elm.getAtomicNumber();
         final int zI = zIndex(elm);
         final int betaI = betaIndex(beta);
         final int kotI = kotIndex(kot);
         final double betaD = (beta - ACOSTA_BETA[betaI]) / (ACOSTA_BETA[betaI + 1] - ACOSTA_BETA[betaI]);
         assert (betaD >= 0.0) && (betaD <= 1.0) : Double.toString(betaD);
         final double kotD = (kot - ACOSTA_KoT[kotI]) / (ACOSTA_KoT[kotI + 1] - ACOSTA_KoT[kotI]);
         assert (kotD >= 0.0) && (kotD <= 1.33334)
               : Double.toString(kotD) + ", Ee = " + Double.toString(FromSI.keV(energy)) + ", Eb = " + Double.toString(FromSI.keV(bremE));
         final double zD = (z - ACOSTA_Z[zI]) / (ACOSTA_Z[zI + 1] - ACOSTA_BETA[zI]);
         // assert (zD >= 0.0) && (zD <= 1.0) : elm + ", " +
         // Double.toString(zD);
         final double a = Math.exp(interpolate(zI, zD, betaI, betaD, kotI, kotD, mACoeff)) / (z * beta);
         final double b = interpolate(zI, zD, betaI, betaD, kotI, kotD, mBCoeff) / beta;
         return acostaAngular(theta, beta, a, b);
      }
   }

   protected BremsstrahlungAngularDistribution(String name, LitReference ref) {
      super("Bremsstrahlung angular distribution", name, ref);
   }

   /**
    * <p>
    * Compute the angular distribution of Bremsstrahlung emission of energy
    * <code>bremE</code> at an angle <code>theta</code> with respect to the
    * direction of the incident electron with energy of <code>eEnergy</code>.
    * </p>
    * <p>
    * The integral
    * </p>
    * 
    * <pre>
    * Integrate[2 pi sin(theta) compute(elm,theta,e,be),{theta,-pi/2,pi/2)] = 4 Pi
    * </pre>
    * <p>
    * for correct intensity normalization.
    * </p>
    * 
    * @param theta
    * @param eEnergy
    * @param bremE
    * @return The angular distribution
    */
   public abstract double compute(Element elm, double theta, double eEnergy, double bremE);

   /**
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#getAllImplementations()
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(new AlgorithmClass[]{Acosta2002, Acosta2002L});
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmUser#initializeDefaultStrategy()
    */
   @Override
   protected void initializeDefaultStrategy() {
      // Nothing to do...
   }

   public static final BremsstrahlungAngularDistribution Acosta2002 = new BremsstrahlungAngularDistribution.AcostaAngularDistribution();
   public static final BremsstrahlungAngularDistribution Acosta2002L = new BremsstrahlungAngularDistribution.AcostaAngularDistributionL();
}