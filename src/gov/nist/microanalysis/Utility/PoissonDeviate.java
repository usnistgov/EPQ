package gov.nist.microanalysis.Utility;

import java.util.Random;

/**
 * <p>
 * Calculates a random deviate from the Poisson distribution with a specified
 * mean. This is based on the algorithm in Press et al. Numerical Recipes in C,
 * second edition.
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

public class PoissonDeviate {
   /**
    * The previous value of 'mean'. Used to determined whether to recalculate
    * the following cached values.
    */
   transient private double mPrevMean;
   /**
    * Cached values for computational speed
    */
   transient private double mG, mSqr, mLogMean;

   private final transient Random mRandom;

   /**
    * PoissonDeviate - Create a new instance of the PoissonDeviate class. Each
    * instance includes its own separate random number generator stream.
    */
   public PoissonDeviate(long seed) {
      mRandom = new Random();
      mRandom.setSeed(seed);
      mPrevMean = -1.0;
   }

   /**
    * randomDeviate - Calculate a random deviate taken from the Poisson
    * distribution with the specified mean.
    * 
    * @param mean
    *           double
    * @return double
    */
   public double randomDeviate(double mean) {
      assert mean > 0.0 : "The mean of a random Poisson deviate must be greater than zero. " + Double.toString(mean);
      if (mean < 12.0) {
         if (mean != mPrevMean) {
            mPrevMean = mean;
            mG = Math.exp(-mean);
         }
         int em = -1;
         double t = 1.0;
         do {
            ++em;
            t *= mRandom.nextDouble();
         } while (t > mG);
         assert (em >= 0);
         return em;
      } else {
         if (mean != mPrevMean) {
            mPrevMean = mean;
            mSqr = Math.sqrt(2.0 * mean);
            mLogMean = Math.log(mean);
            mG = (mean * mLogMean) - Math2.gammaln(mean + 1.0);
         }
         double y, em, t;
         do {
            do {
               y = Math.tan(Math.PI * mRandom.nextDouble());
               em = (mSqr * y) + mean;
            } while (em < 0.0);
            em = Math.floor(em);
            t = 0.9 * (1.0 + (y * y)) * Math.exp((em * mLogMean) - Math2.gammaln(em + 1.0) - mG);
         } while (mRandom.nextDouble() > t);
         assert (em >= 0.0);
         return em;
      }
   }
}
