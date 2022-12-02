package gov.nist.nanoscalemetrology.JMONSEL;

import java.util.Arrays;

import gov.nist.microanalysis.EPQLibrary.BrowningEmpiricalCrossSection;
import gov.nist.microanalysis.EPQLibrary.CzyzewskiMottCrossSection;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.LitReference;
import gov.nist.microanalysis.EPQLibrary.RandomizedScatter;
import gov.nist.microanalysis.EPQLibrary.RandomizedScatterFactory;
import gov.nist.microanalysis.EPQLibrary.ScreenedRutherfordScatteringAngle;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * A class to compute a randomized scattering angle for an electron with a
 * specified energy. The angles are distributed according to the the Mott
 * partial cross section model as implemented by Czyzewski. Since this algorithm
 * is only implemented up to 30 keV and since the Mott cross section approaches
 * the screened Rutherford model, this class uses the
 * ScreenedRutherfordScatteringAngle algorithm above 30 keV (MAX_CZYZEWSKI).
 * Below 20 eV (MIN_CZYZEWSKI) this class uses BrowningEmpiricalCrossSection
 * with the total cross section scaled by the ratio of the Czyzewski value to
 * the Browning value at 20 eV. (The scaling removes the discontinuity at 20 eV
 * between these two different approximations, and then uses the Browning energy
 * dependence for extrapolation to lower energies.)
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author Nicholas W. M. Ritchie (with addition of E &lt; 50 eV behavior by
 *         John Villarrubia)
 * @version 1.0
 */

/*
 * TODO: Consider revising to reuse Nicholas's EPQLibrary routine of the same
 * name for the energy range above 20 eV. Possibly I should also make an
 * interface with contract specifying that the routine is valid for all energies
 * 0<E<infinity, so code can distinguish those that do from those that don't.
 */
public class CzyzewskiMottScatteringAngle extends RandomizedScatter {

   static private final LitReference REFERENCE = new LitReference.JournalArticle(LitReference.JApplPhys, "68, No. 7", "", 1990,
         new LitReference.Author[]{new LitReference.Author("", "Czyzewski"), new LitReference.Author("", "MacCallum"), LitReference.DJoy});

   public static class CzyzewskiMottRandomizedScatterFactory extends RandomizedScatterFactory {
      public CzyzewskiMottRandomizedScatterFactory() {
         super("Czyzewski Mott cross-section", REFERENCE);
      }

      private final RandomizedScatter[] mScatter = new RandomizedScatter[Element.elmEndOfElements];

      /**
       * @see gov.nist.microanalysis.EPQLibrary.RandomizedScatterFactory#get(gov.nist.microanalysis.EPQLibrary.Element)
       */
      @Override
      public RandomizedScatter get(Element elm) {
         final int z = elm.getAtomicNumber();
         if (mScatter[z] == null)
            mScatter[z] = new CzyzewskiMottScatteringAngle(elm);
         return mScatter[z];
      }

      @Override
      protected void initializeDefaultStrategy() {
         // Nothing to do...
      }
   }

   public static RandomizedScatterFactory Factory = new CzyzewskiMottRandomizedScatterFactory();

   private final Element mElement;
   /**
    * Maximum energy for which Czyzewski's tables are used. Above this energy
    * screened Rutherford is used instead.
    */
   final static public double MAX_CZYZEWSKI = ToSI.keV(30.0);
   /**
    * Minimum energy for which Czyzewski's tables are used. Below this energy a
    * scaled Browning Mott (see BrowningMottElasticSM) is used instead.
    */
   final static public double MIN_CZYZEWSKI = ToSI.keV(0.020);
   private final transient double[] mMeanFreePath;
   private final transient double[] mTotalCrossSection;
   private final transient double[][] mCummulativeDF;
   transient private ScreenedRutherfordScatteringAngle mRutherford = null;
   transient private BrowningEmpiricalCrossSection mBrowning = null;
   transient private double sfBrowning;

   /**
    * CzyzewskiMottScatteringAngle - Creates a CzyzewskiMottScatteringAngle
    * object for the specified element.
    *
    * @param el
    *           Element
    */
   public CzyzewskiMottScatteringAngle(Element el) {
      super("Cyzewski", REFERENCE);
      mElement = el;
      // Use the MottCrossSection then discard it
      final CzyzewskiMottCrossSection mcs = new CzyzewskiMottCrossSection(el);
      mMeanFreePath = new double[CzyzewskiMottCrossSection.SpecialEnergyCount];
      mTotalCrossSection = new double[CzyzewskiMottCrossSection.SpecialEnergyCount];
      // Extract some useful stuff...
      for (int i = 0; i < CzyzewskiMottCrossSection.SpecialEnergyCount; ++i) {
         final double e = CzyzewskiMottCrossSection.getSpecialEnergy(i);
         mMeanFreePath[i] = mcs.meanFreePath(e);
         mTotalCrossSection[i] = mcs.totalCrossSection(e);
      }
      // Calculate a normalized running sum that will be used to map a random
      // number between
      // [0,1.00) onto a scattering angle.
      mCummulativeDF = new double[CzyzewskiMottCrossSection.SpecialEnergyCount][CzyzewskiMottCrossSection.SpecialAngleCount];
      for (int r = 0; r < CzyzewskiMottCrossSection.SpecialEnergyCount; ++r) {
         final double energy = CzyzewskiMottCrossSection.getSpecialEnergy(r);
         mCummulativeDF[r][0] = 0.0;
         for (int c = 1; c < CzyzewskiMottCrossSection.SpecialAngleCount; ++c) {
            final double cm = mcs.partialCrossSection(CzyzewskiMottCrossSection.getSpecialAngle(c - 1), energy);
            final double cp = mcs.partialCrossSection(CzyzewskiMottCrossSection.getSpecialAngle(c), energy);
            final double am = CzyzewskiMottCrossSection.getSpecialAngle(c - 1);
            final double ap = CzyzewskiMottCrossSection.getSpecialAngle(c);
            mCummulativeDF[r][c] = mCummulativeDF[r][c - 1] + (((cp + cm) * (ap - am)) / 2.0);
         }
         // Normalize to 1.0
         for (int c = 0; c < CzyzewskiMottCrossSection.SpecialAngleCount; ++c)
            mCummulativeDF[r][c] = mCummulativeDF[r][c] / mCummulativeDF[r][CzyzewskiMottCrossSection.SpecialAngleCount - 1];
      }
   }

   /**
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "CrossSection[Czyzewski-Mott," + mElement.toAbbrev() + "]";
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.RandomizedScatter#getElement()
    */
   @Override
   public Element getElement() {
      return mElement;
   }

   private double scatteringAngleForSpecialEnergy(int ei, double rand) {
      final double[] r = mCummulativeDF[ei];
      int ai = Arrays.binarySearch(r, rand);
      if (ai >= 0)
         return CzyzewskiMottCrossSection.getSpecialAngle(ai);
      else { // Interpolate between angles
         ai = -(ai + 1);
         assert (ai >= 1);
         assert (rand <= r[ai]);
         assert (rand > r[ai - 1]);
         final double am = CzyzewskiMottCrossSection.getSpecialAngle(ai - 1);
         return am + (((rand - r[ai - 1]) / (r[ai] - r[ai - 1])) * (CzyzewskiMottCrossSection.getSpecialAngle(ai) - am));
      }
   }

   /**
    * randomScatteringAngle - Given a random number, rand on the interval [0,1),
    * and an energy (in Joules), this function returns a scattering angle.
    *
    * @param energy
    *           double - In Joules
    * @param rand
    *           double - On [0,1)
    * @return double
    */
   public double randomScatteringAngle(double energy, double rand) {
      assert (rand >= 0);
      assert (rand < 1.0);
      assert (energy <= ToSI.keV(30.0));
      assert (energy > 0.0);
      final int e = CzyzewskiMottCrossSection.getEnergyIndex(energy);
      final double e0 = CzyzewskiMottCrossSection.getSpecialEnergy(e);
      assert (energy <= e0);
      final double a0 = scatteringAngleForSpecialEnergy(e, rand);
      if (energy == e0)
         return a0;
      else {
         final double a1 = scatteringAngleForSpecialEnergy(e - 1, rand);
         final double e1 = CzyzewskiMottCrossSection.getSpecialEnergy(e - 1);
         assert (energy > e1);
         assert (a1 >= 0.0);
         assert (a0 >= 0.0);
         return a0 + (((energy - e0) / (e1 - e0)) * (a1 - a0));
      }

   }

   /**
    * randomScatteringAngle - Same as scatteringAngle(energy,rand) except that
    * this function automatically selects a random \ number.
    *
    * @param energy
    *           double - In Joules
    * @return double
    */
   @Override
   public double randomScatteringAngle(double energy) {
      if (energy < MIN_CZYZEWSKI) {
         if (mBrowning == null) {
            mBrowning = new BrowningEmpiricalCrossSection(mElement);
            sfBrowning = this.totalCrossSection(MIN_CZYZEWSKI) / mBrowning.totalCrossSection(MIN_CZYZEWSKI);
         }
         return mBrowning.randomScatteringAngle(energy);
      } else if (energy < MAX_CZYZEWSKI)
         return randomScatteringAngle(energy, Math2.rgen.nextDouble());
      else {
         if (mRutherford == null)
            mRutherford = new ScreenedRutherfordScatteringAngle(mElement);
         return mRutherford.randomScatteringAngle(energy);
      }
   }

   /**
    * meanFreePath - Calculates the mean free path at the specified energy by
    * interpolating between tabulated values. This method will give unreliable
    * results when extrapolating outside the bounds of the Czyzewski tables (20
    * eV &lt; energy &lt; 30 keV).
    *
    * @param energy
    *           double - In Joules
    * @return double
    */
   public double meanFreePath(double energy) {
      int e = CzyzewskiMottCrossSection.getEnergyIndex(energy);
      if (e == 0)
         e = 1;
      final double e0 = CzyzewskiMottCrossSection.getSpecialEnergy(e - 1);
      final double e1 = CzyzewskiMottCrossSection.getSpecialEnergy(e);
      assert (energy >= e0);
      assert (energy <= e1);
      return mMeanFreePath[e - 1] + ((mMeanFreePath[e] - mMeanFreePath[e - 1]) * ((energy - e0) / (e1 - e0)));
   }

   /**
    * totalCrossSection - Calculates the total cross section at the specified
    * energy by interpolating between tabulated values.
    *
    * @param energy
    *           double - In Joules
    * @return double
    */
   @Override
   public double totalCrossSection(double energy) {
      /*
       * It's important in some simulations to track electrons outside of the
       * range of energies for which the Czyzewski tables are valid. For the
       * sake of those, we switch over to a different method of estimation when
       * the tables become unavailable. At high energy, screened Rutherford
       * should be accurate. At low energy, it's not clear that any model is
       * accurate. However, in solid crystals at energies close to the Fermi
       * energy, scattering is quite small. For this reason extrapolating to low
       * energies with the Browning form, which decreases with decreasing
       * energy, seems better than the Rutherford form, which does the opposite.
       */
      if (energy < MIN_CZYZEWSKI) {
         if (mBrowning == null) {
            mBrowning = new BrowningEmpiricalCrossSection(mElement);
            sfBrowning = this.totalCrossSection(MIN_CZYZEWSKI) / mBrowning.totalCrossSection(MIN_CZYZEWSKI);
         }
         return sfBrowning * mBrowning.totalCrossSection(energy);
      } else if (energy < MAX_CZYZEWSKI) {
         int e = CzyzewskiMottCrossSection.getEnergyIndex(energy);
         if (e == 0)
            e = 1;
         final double e0 = CzyzewskiMottCrossSection.getSpecialEnergy(e - 1);
         final double e1 = CzyzewskiMottCrossSection.getSpecialEnergy(e);
         assert (energy >= e0);
         assert (energy <= e1);
         return mTotalCrossSection[e - 1] + ((mTotalCrossSection[e] - mTotalCrossSection[e - 1]) * ((energy - e0) / (e1 - e0)));
      } else {
         if (mRutherford == null)
            mRutherford = new ScreenedRutherfordScatteringAngle(mElement);
         return mRutherford.totalCrossSection(energy);
      }
   }
}
