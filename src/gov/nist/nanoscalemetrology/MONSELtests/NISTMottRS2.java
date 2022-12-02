package gov.nist.nanoscalemetrology.MONSELtests;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

import gov.nist.microanalysis.EPQLibrary.BrowningEmpiricalCrossSection;
import gov.nist.microanalysis.EPQLibrary.EPQFatalException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.LitReference;
import gov.nist.microanalysis.EPQLibrary.PhysicalConstants;
import gov.nist.microanalysis.EPQLibrary.RandomizedScatter;
import gov.nist.microanalysis.EPQLibrary.RandomizedScatterFactory;
import gov.nist.microanalysis.EPQLibrary.ScreenedRutherfordScatteringAngle;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.nanoscalemetrology.JMONSELutils.ULagrangeInterpolation;

/**
 * <p>
 * NISTMottRS2 is a copy of NISTMottRS with different low energy behavior.
 * Instead of extrapolating below 50 eV with the Browning formula, it simply
 * extrapolates to 0 as Kieft and Bosch did. This is meant to be a temporary
 * experimental method. I'll probably delete it soon, so its use is discouraged.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author Nicholas W. M. Ritchie (with addition of E&lt;50 eV behavior by John
 *         Villarrubia)
 * @version 1.0
 */

@Deprecated
public class NISTMottRS2 extends RandomizedScatter {

   static private final LitReference.WebSite mReference = new LitReference.WebSite("http://www.nist.gov/srd/nist64.htm",
         "NIST Electron Elastic-Scattering Cross-Section Database version 3.1", LitReference.createDate(2007, Calendar.AUGUST, 24),
         new LitReference.Author[]{LitReference.CPowell, LitReference.FSalvat, LitReference.AJablonski});

   public static class NISTMottRS2Factory extends RandomizedScatterFactory {
      public NISTMottRS2Factory() {
         super("NIST Mott Inelastic Cross-Section", mReference);
      }

      private final RandomizedScatter[] mScatter = new RandomizedScatter[Element.elmEndOfElements];

      /**
       * @see gov.nist.microanalysis.EPQLibrary.RandomizedScatterFactory#get(gov.nist.microanalysis.EPQLibrary.Element)
       */
      @Override
      public RandomizedScatter get(Element elm) {
         final int z = elm.getAtomicNumber();
         RandomizedScatter res = mScatter[z];
         if (res == null) {
            mScatter[z] = new NISTMottRS2(elm);
            res = mScatter[z];
         }
         return res;
      }

      @Override
      protected void initializeDefaultStrategy() {
         // TODO Auto-generated method stub

      }
   }

   public static final RandomizedScatterFactory Factory = new NISTMottRS2Factory();

   public static final double MAX_NISTMOTT = ToSI.keV(20.0);
   public static final double MIN_NISTMOTT = ToSI.keV(0.050);

   private final Element mElement;
   private static final int SPWEM_LEN = 61;
   private static final int X1_LEN = 201;
   private static final double DL50 = Math.log(MIN_NISTMOTT);
   private static final double PARAM = (Math.log(MAX_NISTMOTT) - DL50) / 60.0;
   private final transient double[] mSpwem = new double[SPWEM_LEN];
   private final transient double[][] mX1 = new double[SPWEM_LEN][X1_LEN];
   private final int qINTERPOLATIONORDER = 3;
   private final int sigmaINTERPOLATIONORDER = 3;
   transient private ScreenedRutherfordScatteringAngle mRutherford = null;
   transient private BrowningEmpiricalCrossSection mBrowning = null;
   transient private final double scale = PhysicalConstants.BohrRadius * PhysicalConstants.BohrRadius;

   /**
    * <p>
    * NISTMottRS2 - Creates an object representing the NIST SRD 64 method for
    * computing random scattering angles using Mott cross sections. The
    * constructor loads a table of numbers which are used to quickly compute
    * cross sections for any energy within the tabulated range, 50 eV to 20,000
    * eV.
    * </p>
    * <p>
    * For energies above the tabulated range, computations use
    * ScreenedRutherfordScatteringAngle instead. For energies below the
    * tabulated range, the total cross section goes linearly to 0 and the
    * scattering angle distribution is given by the
    * BrowningEmpiricalCrossSection method.
    * </p>
    * <p>
    * This class functionally duplicates
    * gov.nist.microanalysis.EPQLibrary.NISTMottScatteringAngle but with higher
    * order interpolation. It should have more accurate interpolation but at the
    * cost of some speed.
    * </p>
    * <p>
    * NISTMottScatteringAngle uses 2nd order interpolation in log(E) for the
    * totalCrossSection calculation. For randomScatteringAngle it uses 0th order
    * interpolation (i.e., it chooses the nearest tabulated value) in log(E) and
    * 1st order in the random number. In contrast the present class uses 3rd
    * order for all interpolations.
    * </p>
    *
    * @param elm
    *           Element
    */
   public NISTMottRS2(Element elm) {
      super("NIST Elastic cross-section", mReference);
      assert (elm != null);
      mElement = elm;
      try {
         final String name = elm.getAtomicNumber() < 10
               ? "NistXSec/E0" + Integer.toString(elm.getAtomicNumber()) + ".D64"
               : "NistXSec/E" + Integer.toString(elm.getAtomicNumber()) + ".D64";
         final InputStream is = gov.nist.microanalysis.EPQLibrary.NISTMottScatteringAngle.class.getResourceAsStream(name);
         final InputStreamReader isr = new InputStreamReader(is, "US-ASCII");
         final BufferedReader br = new BufferedReader(isr);
         // To ensure that numbers are parsed correctly regardless of locale
         final NumberFormat nf = NumberFormat.getInstance(Locale.US);
         br.readLine();
         for (int j = 0; j < SPWEM_LEN; ++j) {
            mSpwem[j] = nf.parse(br.readLine().trim()).doubleValue();
            for (int i = 0; i < X1_LEN; ++i)
               mX1[j][i] = nf.parse(br.readLine().trim()).doubleValue();
         }
         /*
          * If the above was successful mSpwem is a 1-d table of total cross
          * sections, uniform in Log(E) from Log(50.) to Log(20000). mX1[j][i]
          * is a 2-d table of q vs Log(E) and random #. j indexes log(E). i
          * indexes r. q is related to cos(theta) by 1-2*q*q=cos(theta).
          */
      } catch (final Exception ex) {
         throw new EPQFatalException("Unable to construct NISTMottRS: " + ex.toString());
      }
   }

   /**
    * toString
    *
    * @return String
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "CrossSection[NIST-Mott," + mElement.toAbbrev() + "]";
   }

   @Override
   public Element getElement() {
      return mElement;
   }

   /**
    * totalCrossSection - Computes the total cross section for an electron of
    * the specified energy.
    *
    * @param energy
    *           double - In Joules
    * @return double - in square meters
    */
   @Override
   final public double totalCrossSection(double energy) {
      /*
       * It's important in some simulations to track electrons outside of the
       * range of energies for which the NIST Mott tables are valid. For the
       * sake of those, we switch over to a different method of estimation when
       * the tables become unavailable. At high energy, screened Rutherford
       * should be accurate. At low energy, it's not clear that any model is
       * accurate. The choice made by this class is to make the scattering go to
       * zero at low energy. This is similar to the choice of Kieft & Bosch.
       */
      if (energy < MIN_NISTMOTT)
         return (this.totalCrossSection(MIN_NISTMOTT) * energy) / MIN_NISTMOTT;
      else if (energy < MAX_NISTMOTT)
         return scale * ULagrangeInterpolation.d1(mSpwem, DL50, PARAM, sigmaINTERPOLATIONORDER, Math.log(energy))[0];
      else {
         if (mRutherford == null)
            mRutherford = new ScreenedRutherfordScatteringAngle(mElement);
         return mRutherford.totalCrossSection(energy);
      }
   }

   /**
    * randomScatteringAngle - Returns a randomized scattering angle in the range
    * [0,PI] that comes from the distribution of scattering angles for an
    * electron of specified energy on an atom of the element represented by the
    * instance of this class.
    *
    * @param energy
    *           double - In Joules
    * @return double - an angle in radians
    */
   @Override
   final public double randomScatteringAngle(double energy) {
      if (energy < MIN_NISTMOTT) {
         if (mBrowning == null)
            mBrowning = new BrowningEmpiricalCrossSection(mElement);
         return mBrowning.randomScatteringAngle(energy);
      } else if (energy < MAX_NISTMOTT) {
         final double q = ULagrangeInterpolation.d2(mX1, new double[]{DL50, 0.}, new double[]{PARAM, 0.005}, qINTERPOLATIONORDER,
               new double[]{Math.log(energy), Math2.rgen.nextDouble()})[0];
         final double com = 1.0 - (2.0 * q * q);
         return com > -1.0 ? (com < 1.0 ? Math.acos(com) : 0.0) : Math.PI;
      } else {
         if (mRutherford == null)
            mRutherford = new ScreenedRutherfordScatteringAngle(mElement);
         return mRutherford.randomScatteringAngle(energy);
      }
   }
}
