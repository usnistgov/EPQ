package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.EPQLibrary.LitReference.Author;
import gov.nist.microanalysis.EPQLibrary.LitReference.Book;
import gov.nist.microanalysis.EPQLibrary.LitReference.BookChapter;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * The XPP algorithm of Pouchou and Pichoir extended to handle non-normal beam
 * incidence.
 * </p>
 * <p>
 * Description: The XPP algorithm of Pouchou and Pichoir extended to handle
 * non-normal beam incidence.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas
 * @version 1.0
 */
public class XPP1989Ext
   extends XPP1991 {

   static private double MIN_TILT = Math.toRadians(1.0);

   static private final BookChapter PAP_IXCOM12 = new BookChapter(new Book("Proceedings of the 12th International Congress on X-Ray Optics and Microanalysis", "", 1991, new Author[] {}), "p52-59", new Author[] {
      LitReference.JPouchou,
      LitReference.FPichoir,
      new Author("D.", "Boivin", "Office National d'Etudes et de Recherche Aerospatiales")
   });

   /**
    * A special implementation of the BackscatterFactor class that takes into
    * account a tilt in the sample with respect to the beam.
    */
   public final static XPPTiltBackscatterFactor TiltedBackscatterFactor = new XPPTiltBackscatterFactor();

   /**
    * A class implementing the BackscatterFactor class with the added ability to
    * handle tilted samples.
    */
   static public class XPPTiltBackscatterFactor
      extends BackscatterFactor {

      private double mBeta = 0.0;

      private XPPTiltBackscatterFactor() {
         super("XPP Tilted", PAP_IXCOM12);
      }

      public void setTilt(double tilt) {
         mBeta = tilt;
      }

      @Override
      protected void initializeDefaultStrategy() {
         addDefaultAlgorithm(BackscatterCoefficient.class, BackscatterCoefficient.PouchouAndPichoir91);
      }

      @Override
      public double compute(Composition comp, AtomicShell shell, double e0) {
         final BackscatterCoefficient eta = (BackscatterCoefficient) getAlgorithm(BackscatterCoefficient.class);
         final double cosBeta = Math.cos(mBeta);
         final double u0 = e0 / shell.getEdgeEnergy();
         final double meanZb = PAP1991.papMeanAtomicNumber(comp);
         final double eta0 = eta.compute(comp, e0);
         final double etaTilt = Math.pow(eta0, Math.pow(cosBeta, 1.14 - (0.4 * (1.0 - Math.exp(-meanZb / 25.0)))));
         final double w0 = 0.595 + (eta0 / 3.7) + Math.pow(eta0, 4.55);
         final double wTilt = Math.pow(w0, Math.pow(cosBeta, 0.69 - (0.21 * (1.0 - Math.exp(-meanZb / 17.0)))));
         final double ju = 1.0 + (u0 * (Math.log(u0) - 1.0));
         final double q = ((2.0 * wTilt) - 1.0) / (1.0 - wTilt);
         final double gu = (u0 - 1.0 - ((1.0 - (1.0 / Math.pow(u0, 1.0 + q))) / (1.0 + q))) / ((2.0 + q) * ju);
         return 1.0 - (etaTilt * wTilt * (1.0 - gu));
      }
   };

   /**
    * Constructs an object that implements the XPP quantitative correction
    * algorithm with the extensions necessary to handle samples oriented at a
    * tilt with respect to the beam.
    */
   public XPP1989Ext() {
      super("XPP - Pouchou & Pichoir Simplified (Non-normal)", PAP_IXCOM12);
   }

   @Override
   public boolean initialize(Composition comp, AtomicShell shell, SpectrumProperties props)
         throws EPQException {
      final boolean res = super.initialize(comp, shell, props);
      if(res) {
         final double beta = getTilt(props);
         if(beta > MIN_TILT) {
            final ProportionalIonizationCrossSection icx = (ProportionalIonizationCrossSection) getAlgorithm(ProportionalIonizationCrossSection.class);
            final StoppingPower sp = (StoppingPower) getAlgorithm(StoppingPower.class);

            final double cosBeta = Math.cos(beta);
            final double u0 = mBeamEnergy / shell.getEdgeEnergy();
            // mF = (R/S)*(1/Qj(E0))
            TiltedBackscatterFactor.setTilt(beta);
            mF = (cosBeta * TiltedBackscatterFactor.compute(mComposition, mShell, mBeamEnergy) * StoppingPower.invToGramPerkeVcmSqr(sp.computeInv(mComposition, mShell, mBeamEnergy)))
                  / icx.computeFamily(mShell, mBeamEnergy);
            assert mF > 0.0 : "The integral must be larger than zero.";
            final double zBar = mComposition.weightAvgAtomicNumber();
            { // Modify the mPhi0 computed in super.initialize(...)
               final double h = 0.2 + (2.3 / Math.sqrt(zBar));
               final double q = 1.0 + (h * (1.0 - Math.exp(-Math.pow(u0 - 1.0, 0.3))));
               final double p = Math.pow(cosBeta, 0.7);
               mPhi0 = q * Math.pow(mPhi0 / q, p);
            }
            double rBar;
            final double meanZb = PAP1991.papMeanAtomicNumber(mComposition);
            {
               // Compute the mean depth of ionization (rBar)
               final double x = 1.0 + (1.3 * Math.log(meanZb));
               final double y = 0.2 + (meanZb / 200.0);
               rBar = mF / (1.0 + ((x * Math.log(1.0 + (y * (1.0 - (1.0 / Math.pow(u0, 0.42)))))) / Math.log(1.0 + y)));
               // Impose the special condition
               if((mF / rBar) < mPhi0)
                  rBar = mF / mPhi0;
            }
            // Calculate the initial slope (p)
            {
               final double g = 0.22 * Math.log(4.0 * meanZb) * (1.0 - (2.0 * Math.exp((-meanZb * (u0 - 1.0)) / 15.0)));
               final double h = 1.0 - ((10.0 * (1.0 - (1.0 / (1.0 + (u0 / 10.0))))) / (meanZb * meanZb));
               double gh4 = g * Math2.sqr(h * h);
               mLittleB = (SQRT_TWO * (1.0 + Math.sqrt(1.0 - ((rBar * mPhi0) / mF)))) / (rBar * cosBeta);
               double pTilt;
               {
                  final double limit = 0.9 * mLittleB * rBar * rBar * (mLittleB - ((2.0 * mPhi0) / mF));
                  if(gh4 > limit)
                     gh4 = limit;
                  final double p0 = (gh4 * mF) / (rBar * rBar);
                  final double tDeg = Math.toDegrees(beta);
                  final double u = 8.0e-3 * Math.exp(-Math.pow(zBar, 0.3));
                  pTilt = p0 * ((1.0 + (u * Math.pow(tDeg, 1.7))) - (27.0 * Math.exp(-(90.0 - tDeg) / 7.0)));
               }
               mLittleA = (pTilt + (mLittleB * ((2.0 * mPhi0) - (mLittleB * mF))))
                     / ((mLittleB * mF * (2.0 - (mLittleB * rBar))) - mPhi0);
               mEps = (mLittleA - mLittleB) / mLittleB;
               if(mEps < TINY) {
                  mEps = TINY;
                  mLittleA = mLittleB * (1 + mEps);
               }
               mBigB = ((mLittleB * mLittleB * mF * (1.0 + mEps)) - pTilt - (mPhi0 * mLittleB * (2.0 + mEps))) / mEps;
               mBigA = ((((mBigB / mLittleB) + mPhi0) - (mLittleB * mF)) * (1 + mEps)) / mEps;
            }
         }
      }
      return res;
   }
}
