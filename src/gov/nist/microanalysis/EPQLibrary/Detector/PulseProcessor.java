package gov.nist.microanalysis.EPQLibrary.Detector;

import java.util.ArrayList;
import java.util.List;

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
 * @author Nicholas
 * @version 1.0
 */
public class PulseProcessor {

   interface PulsePairRejectionTime {

      /**
       * The minimum time between x-rays of energies e0 and e1 for the events to
       * be recorded as separate events.
       *
       * @param e0
       * @param e1
       * @return double
       */
      double compute(double e0, double e1);
   }

   interface ProcessTime {

      double compute(double e0);
   }

   public class ConstantPPRT
      implements
      PulsePairRejectionTime {

      private final double mDuration;

      public ConstantPPRT(double pprt) {
         mDuration = pprt;
      }

      @Override
      public double compute(double e0, double e1) {
         return mDuration;
      }
   }

   public class ConstantProcessTime
      implements
      ProcessTime {

      private final double mDuration;

      public ConstantProcessTime(double t) {
         mDuration = t;

      }

      @Override
      public double compute(double e0) {
         return mDuration;
      }
   }

   private final PulsePairRejectionTime mRejection;
   private final ProcessTime mProcess;

   private final class Incoming {
      final double mEnergy;
      final double mDelay;

      private Incoming(double e, double dt) {
         mEnergy = e;
         mDelay = dt;
      }
   }

   private class Measurement {
      private final List<Incoming> mXRays;
      private boolean mReject;

      public Measurement(double e0, double delay) {
         mXRays = new ArrayList<Incoming>();
         mXRays.add(new Incoming(e0, delay));
         mReject = false;
      }

      public void add(double e0, double delay) {

      }

      public int getCount() {
         return mXRays.size();
      }

      public double getEnergy() {
         double d = 0.0;
         for(Incoming x : mXRays)
            d += x.mEnergy;
         return d;
      }

      public Incoming last() {
         return mXRays.get(mXRays.size() - 1);
      }

   }

   private transient Measurement mCurrent;

   private final ArrayList<Measurement> mEvents = new ArrayList<Measurement>();

   private double mLiveTime = 0.0;
   private double mRealTime = 0.0;

   /**
    * Constructs a PulseProcessor
    *
    * @param pprt PulsePairRejectionTime
    * @param pt ProcessTime
    */
   public PulseProcessor(PulsePairRejectionTime pprt, ProcessTime pt) {
      mRejection = pprt;
      mProcess = pt;
   }

   public void add(double e0, double delay) {
      mRealTime += delay;
      if(mCurrent != null) {
         final Incoming prev = mCurrent.last();
         if(delay < mRejection.compute(e0, prev.mEnergy)) { // pile-up
            mCurrent.mXRays.add(new Incoming(e0, delay));
         } else if(delay < mProcess.compute(prev.mEnergy)) {
            mCurrent.mXRays.add(new Incoming(e0, delay));
            mCurrent.mReject = true;
         } else {
            mEvents.add(mCurrent);
            mLiveTime += delay;
            mCurrent = new Measurement(e0, delay);
         }
      } else
         mCurrent = new Measurement(e0, delay);
   }

}
