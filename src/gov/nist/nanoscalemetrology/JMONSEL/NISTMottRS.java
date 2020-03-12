package gov.nist.nanoscalemetrology.JMONSEL;

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
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.Utility.Math2;
import gov.nist.nanoscalemetrology.JMONSELutils.ULagrangeInterpolation;

/**
 * <p>
 * NISTMottRS - A class to use the NIST SRD 64 method for computing random
 * scattering angles using Mott cross sections. A table of cross sections is
 * interpolated to quickly compute cross sections for any energy within the
 * tabulated range, 50 eV to 20,000 eV.
 * </p>
 * <p>
 * For energies above the tabulated range, computations use
 * ScreenedRutherfordScatteringAngle instead. For energies below either 50 eV or
 * 100 eV the value is interpolated between assuming 0 cross section at 0 eV.
 * Interpolation may be linear or may follow Browning's form. The default is
 * Browning ineterpolation below 50 eV. Other options may be chosen using
 * setMethod(int methodnumber) with methodnumber = 1, 2, or 3.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author Nicholas W. M. Ritchie (with addition of E<50 eV behavior by John
 *         Villarrubia)
 * @version 1.0
 */

/*
 * TODO: Consider revising to reuse Nicholas's EPQLibrary routine of the same
 * name for the energy range above 20 eV. Possibly I should also make an
 * interface with contract specifying that the routine is valid for all energies
 * 0<E<infinity, so code can distinguish those that do from those that don't.
 */
public class NISTMottRS
   extends
   RandomizedScatter {

   public static class NISTMottRSFactory
      extends
      RandomizedScatterFactory {

      private int extrapMethod = 1;

      private double minEforTable = ToSI.eV(50.);

      private final NISTMottRS[] mScatter = new NISTMottRS[Element.elmEndOfElements];

      /**
       * Implements the default NISTMottRSFactory, which uses tabulated values
       * whenever available and Browning extrapolation below the table minimum.
       */
      public NISTMottRSFactory() {
         this(1, ToSI.eV(50.));
      }

      /**
       * extrapMethod is an integer = 1 or 2. Since the NISTMott tables are
       * valid only for energies in the interval [50 eV, 20 keV], cross sections
       * for energies < 50 eV can't be determined directly by table lookup.
       * Optionally users may specify a higher minimum for use of the tabulated
       * values. Outside of the permitted and availabe range, we use one of 2
       * methods: (1) extrapolation with Browning's power law or (2) linear
       * extrapolation from the final value to 0 at 0 eV.
       *
       * @param extrapMethod
       * @param minEforTable
       */
      public NISTMottRSFactory(int extrapMethod, double minEforTable) {
         super("NIST Mott Inelastic Cross-Section", mReference);
         setExtrapMethod(extrapMethod);
         setMinEforTable(minEforTable);
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.RandomizedScatterFactory#get(gov.nist.microanalysis.EPQLibrary.Element)
       */
      @Override
      public NISTMottRS get(Element elm) {
         final int z = elm.getAtomicNumber();
         if(mScatter[z] == null || mScatter[z].getExtrapMethod() != extrapMethod
               || mScatter[z].getMinEforTable() != minEforTable) {
            mScatter[z] = new NISTMottRS(elm);
            mScatter[z].setExtrapMethod(extrapMethod);
            mScatter[z].setMinEforTable(minEforTable);
         }
         return mScatter[z];
      }

      /**
       * Returns 1 if we will extrapolate below the minimum energy for which
       * we're using the tablulated values by using Browning's power law or 2 if
       * we will use linear interpolation.
       * 
       * @return extrapMethod
       */
      public int getExtrapMethod() {
         return extrapMethod;
      }

      /**
       * Returns the minimum energy (in J) for which we will use tabulated
       * values.
       * 
       * @return
       */
      public double getMinEforTable() {
         return minEforTable;
      }

      @Override
      protected void initializeDefaultStrategy() {
         // TODO Auto-generated method stub

      }

      /**
       * extrapMethod is an integer = 1 or 2, 1 to use the Browning power law
       * form for energies between 0 and minEforTable, 2 to use linear
       * interpolation between the tabulated value at the upper energy and 0 at
       * 0 energy.
       * 
       * @param extrapMethod
       */
      public void setExtrapMethod(int extrapMethod) {
         if((extrapMethod == 1) || (extrapMethod == 2))
            this.extrapMethod = extrapMethod;
         else
            throw new IllegalArgumentException("extrapMethod must be either 1 or 2.");
      }

      /**
       * By default, we interpolate the NIST Mott tables for all energy values
       * that fall within the range of tabulated values, but the lower limit can
       * be increased from the default value with this setter.
       * 
       * @param minEforTable in Joules
       */
      public void setMinEforTable(double minEforTable) {
         if(minEforTable >= MIN_NISTMOTT)
            this.minEforTable = minEforTable;
         else
            throw new IllegalArgumentException("minEforTable must be >= " + Double.toString(MIN_NISTMOTT) + " J ("
                  + Double.toString(FromSI.eV(MIN_NISTMOTT)) + " eV).");
      }
   }

   static private final LitReference.WebSite mReference = new LitReference.WebSite("http://www.nist.gov/srd/nist64.htm", "NIST Electron Elastic-Scattering Cross-Section Database version 3.1", LitReference.createDate(2007, Calendar.AUGUST, 24), new LitReference.Author[] {
      LitReference.CPowell,
      LitReference.FSalvat,
      LitReference.AJablonski
   });

   /**
    * Returns a NISTMottRSFactory that uses minEforTable = 50 eV and
    * extrapMethod = 1 (Browning).
    */
   public static final RandomizedScatterFactory Factory = new NISTMottRSFactory();
   /**
    * Returns a NISTMottRSFactory that uses minEforTable = 100 eV and
    * extrapMethod = 1 (Browning).
    */
   public static final RandomizedScatterFactory Factory100 = new NISTMottRSFactory(1, ToSI.eV(100.));
   /**
    * Returns a NISTMottRSFactory that uses minEforTable = 100 eV and
    * extrapMethod = 2 (linear).
    */
   public static final RandomizedScatterFactory Factory100Lin = new NISTMottRSFactory(2, ToSI.eV(100.));

   /*
    * MIN_ and MAX_ NISTMOTT are the limits of the scattering table that we
    * interpolate
    */
   public static final double MAX_NISTMOTT = ToSI.keV(20.0);
   public static final double MIN_NISTMOTT = ToSI.keV(0.050);
   private static final int SPWEM_LEN = 61;
   private static final int X1_LEN = 201;

   private static final double DL50 = Math.log(MIN_NISTMOTT);

   private static final double PARAM = (Math.log(MAX_NISTMOTT) - DL50) / 60.0;

   /*
    * extrapolateBelowEnergy is the energy below which we switch to
    * extrapolation using the Browning formula. By default we use the NISTMott
    * scattering tables whenever we have them, but we can set this value higher.
    * For example, Kieft and Bosch don't trust the NISTMott cross sections below
    * 100 eV.
    */
   private int extrapMethod = 1; // 1 for Browning, 2 for linear
   private double minEforTable = ToSI.eV(50.); // Energy below which to use
                                               // extrapMethod

   private double MottXSatMinEnergy;
   private final Element mElement;
   private final transient double[] mSpwem = new double[SPWEM_LEN];
   private final transient double[][] mX1 = new double[SPWEM_LEN][X1_LEN];
   /*
    * NISTMottScatteringAngle uses 2nd order interpolation in log(E) for the
    * totalCrossSection calculation. For randomScatteringAngle it uses 0th order
    * interpolation (i.e., it chooses the nearest tabulated value) in log(E) and
    * 1st order in the random number. In contrast the present class uses 3rd
    * order for all interpolations. (This choice is set by the constants below.)
    */
   private final int qINTERPOLATIONORDER = 3;
   private final int sigmaINTERPOLATIONORDER = 3;
   transient private ScreenedRutherfordScatteringAngle mRutherford = null;
   transient private BrowningEmpiricalCrossSection mBrowning = null;
   transient private double sfBrowning;
   transient private final double scale = PhysicalConstants.BohrRadius * PhysicalConstants.BohrRadius;

   /**
    * NISTMottRS - Creates an object representing the NIST SRD 64 method for
    * computing random scattering angles using Mott cross sections. The
    * constructor loads a table of numbers which are used to quickly compute
    * cross sections for any energy within the tabulated range, 50 eV to 20,000
    * eV.
    * </p>
    * <p>
    * For energies above the tabulated range, computations use
    * ScreenedRutherfordScatteringAngle instead. For energies below minEforTable
    * (default 50 eV) the value is determined by one of two methods: (1)
    * Extrapolation using Browning's power law form (the default) or (2) linear
    * interpolation between 0 at 0 eV and the tabulated value at minEforTable.
    * </p>
    *
    * @param elm Element
    */
   public NISTMottRS(Element elm) {
      super("NIST Elastic cross-section", mReference);
      assert (elm != null);
      mElement = elm;
      try {
         final String name = elm.getAtomicNumber() < 10 ? "NistXSec/E0" + Integer.toString(elm.getAtomicNumber()) + ".D64"
               : "NistXSec/E" + Integer.toString(elm.getAtomicNumber()) + ".D64";
         final InputStream is = gov.nist.microanalysis.EPQLibrary.NISTMottScatteringAngle.class.getResourceAsStream(name);
         final InputStreamReader isr = new InputStreamReader(is, "US-ASCII");
         final BufferedReader br = new BufferedReader(isr);
         // To ensure that numbers are parsed correctly regardless of locale
         final NumberFormat nf = NumberFormat.getInstance(Locale.US);
         br.readLine();
         for(int j = 0; j < SPWEM_LEN; ++j) {
            mSpwem[j] = nf.parse(br.readLine().trim()).doubleValue();
            for(int i = 0; i < X1_LEN; ++i)
               mX1[j][i] = nf.parse(br.readLine().trim()).doubleValue();
         }
         /*
          * If the above was successful mSpwem is a 1-d table of total cross
          * sections, uniform in Log(E) from Log(50.) to Log(20000). mX1[j][i]
          * is a 2-d table of q vs Log(E) and random #. j indexes log(E). i
          * indexes r. q is related to cos(theta) by 1-2*q*q=cos(theta).
          */
      }
      catch(final Exception ex) {
         throw new EPQFatalException("Unable to construct NISTMottRS: " + ex.toString());
      }
      MottXSatMinEnergy = this.totalCrossSection(minEforTable);
   }

   @Override
   public Element getElement() {
      return mElement;
   }

   /**
    * @return
    */
   public int getExtrapMethod() {
      return extrapMethod;
   }

   public double getMinEforTable() {
      return minEforTable;
   }

   /**
    * randomScatteringAngle - Returns a randomized scattering angle in the range
    * [0,PI] that comes from the distribution of scattering angles for an
    * electron of specified energy on an atom of the element represented by the
    * instance of this class.
    *
    * @param energy double - In Joules
    * @return double - an angle in radians
    */
   @Override
   final public double randomScatteringAngle(double energy) {
      /*
       * Even in extrapMethod 2 (linear interpolation) we use Browning for the
       * angular distribution.
       */
      if(energy < minEforTable) {
         if(mBrowning == null) {
            mBrowning = new BrowningEmpiricalCrossSection(mElement);
            sfBrowning = this.totalCrossSection(MIN_NISTMOTT)/mBrowning.totalCrossSection(MIN_NISTMOTT);
         }
         return mBrowning.randomScatteringAngle(energy);
      } else if(energy < MAX_NISTMOTT) {
         final double q = ULagrangeInterpolation.d2(mX1, new double[] {
            DL50,
            0.
         }, new double[] {
            PARAM,
            0.005
         }, qINTERPOLATIONORDER, new double[] {
            Math.log(energy),
            Math2.rgen.nextDouble()
         })[0];
         final double com = 1.0 - (2.0 * q * q);
         return com > -1.0 ? (com < 1.0 ? Math.acos(com) : 0.0) : Math.PI;
      } else {
         if(mRutherford == null)
            mRutherford = new ScreenedRutherfordScatteringAngle(mElement);
         return mRutherford.randomScatteringAngle(energy);
      }
   }

   /**
    * extrapMethod is an integer = 1 or 2, 1 to use the Browning power law form
    * for energies between 0 and minEforTable, 2 to use linear interpolation
    * between the tabulated value at the upper energy and 0 at 0 energy.
    * 
    * @param method
    */
   public void setExtrapMethod(int method) {
      if((method == 1) || (method == 2))
         extrapMethod = method;
      else
         throw new IllegalArgumentException("setExtrapMethod must be called with method = 1 or 2.");
   }

   /**
    * By default, we interpolate the table for values within the range of
    * tabulated energies, namely from 8.01e-18 J to 3.20e-15 J (50 eV to 20
    * keV). The lower limit can be increased from the default value with this
    * setter.
    * 
    * @param minEforTable in Joules
    */
   public void setMinEforTable(double minEforTable) {
      if(minEforTable >= MIN_NISTMOTT) {
         this.minEforTable = minEforTable;
         MottXSatMinEnergy = this.totalCrossSection(minEforTable);
      } else
         throw new IllegalArgumentException("minEforTable must be >= " + Double.toString(MIN_NISTMOTT) + " J ("
               + Double.toString(FromSI.eV(MIN_NISTMOTT)) + " eV).");
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

   /**
    * totalCrossSection - Computes the total cross section for an electron of
    * the specified energy.
    *
    * @param energy double - In Joules
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
       * accurate. We use the Browning interpolation here.
       */
      if(energy < minEforTable) {
         if(extrapMethod == 2)
            return (MottXSatMinEnergy * energy) / minEforTable;
         else { // Browning interpolation
            if(mBrowning == null) {
               mBrowning = new BrowningEmpiricalCrossSection(mElement);
               sfBrowning = MottXSatMinEnergy / mBrowning.totalCrossSection(minEforTable);
            }
            return sfBrowning * mBrowning.totalCrossSection(energy);
         }
      } else if(energy < MAX_NISTMOTT)
         return scale * ULagrangeInterpolation.d1(mSpwem, DL50, PARAM, sigmaINTERPOLATIONORDER, Math.log(energy))[0];
      else {
         if(mRutherford == null)
            mRutherford = new ScreenedRutherfordScatteringAngle(mElement);
         return mRutherford.totalCrossSection(energy);
      }
   }

}
