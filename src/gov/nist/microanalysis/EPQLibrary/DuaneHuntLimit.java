package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.Utility.LinearRegression;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * The DuaneHuntLimit AlgorithmClass implements algorithms for estimating the
 * Duane-Hunt limit from the channel data in an ISpectrumData object.
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
abstract public class DuaneHuntLimit
   extends AlgorithmClass {

   protected DuaneHuntLimit(String name, LitReference ref) {
      super("Duane-Hunt", name, ref);
   }

   protected DuaneHuntLimit(String name, String ref) {
      super("Duane-Hunt", name, ref);
   }

   /**
    * Estimates the Duane-Hunt limit from the channel data in the argument
    * ISpectrumData object.
    * 
    * @param spec
    * @return The Duane-Hunt limit in Joule
    */
   abstract public double compute(ISpectrumData spec);

   /**
    * getAllImplementations
    * 
    * @return (non-Javadoc)
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#getAllImplementations()
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(new AlgorithmClass[] {
         LinearDuaneHunt,
         CrudeDuaneHunt,
         AltDuaneHunt
      });
   }

   /**
    * initializeDefaultStrategy (non-Javadoc)
    * 
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#initializeDefaultStrategy()
    */
   @Override
   protected void initializeDefaultStrategy() {
      // Don't need to do anything
   }

   static private class CrudeDuaneHuntAlgorithm
      extends DuaneHuntLimit {

      static final int WIDTH = 10;

      static private double avg(double[] a, int ch, int width) {
         double sum = 0.0;
         ch = Math.max(0, Math.min(a.length - width, ch));
         width = Math.min(width, a.length);
         for(int i = ch; i < (ch + width); i++)
            sum += a[i];
         return sum / width;

      }

      protected CrudeDuaneHuntAlgorithm() {
         super("Crude Duane-Hunt", "None");
      }

      @Override
      public double compute(ISpectrumData spec) {
         final double e0 = spec.getProperties().getNumericWithDefault(SpectrumProperties.BeamEnergy, Double.NaN);
         final int maxCh = Double.isNaN(e0) ? spec.getChannelCount() : SpectrumUtils.channelForEnergy(spec, e0 * 1000.0);
         if(maxCh > spec.getChannelCount())
            return Double.NaN;
         final double[] chData = SpectrumUtils.toDoubleArray(spec);
         final double thresh = Math.max(2.5 / WIDTH, (avg(chData, 0, chData.length) * chData.length) / (e0 * 1000.0 * 500.0));
         int st = -1;
         final int RUN = 5;
         for(int ch = maxCh; ch > WIDTH; --ch)
            if(avg(chData, ch - WIDTH, WIDTH) > thresh) {
               if(st == -1)
                  st = ch;
               else if((st - ch) >= RUN)
                  return ToSI.eV(SpectrumUtils.minEnergyForChannel(spec, st));
            } else
               st = -1;
         return 0;
      }
   };

   static private class AltDuaneHuntAlgorithm
      extends DuaneHuntLimit {

      public AltDuaneHuntAlgorithm() {
         super("Alt Duane-Hunt", LitReference.NullReference);
      }

      @Override
      public double compute(ISpectrumData spec) {
         final double initial = CrudeDuaneHunt.compute(spec);
         if(Double.isNaN(initial))
            return initial;
         final ISpectrumData sg5 = SpectrumSmoothing.SavitzkyGolay5.compute(spec);
         final LinearRegression lr = new LinearRegression();
         int initialCh = SpectrumUtils.bound(sg5, SpectrumUtils.channelForEnergy(sg5, FromSI.eV(initial)));
         final int WIDTH = Math.max(10, initialCh / 20);
         initialCh -= WIDTH / 5;
         for(int ch = Math.max(0, initialCh - WIDTH); ch < initialCh; ++ch) {
            final double v = sg5.getCounts(ch);
            if(v > 0.0)
               lr.addDatum(ch, v, Math.sqrt(v));
         }

         return ToSI.eV(sg5.getZeroOffset() + (sg5.getChannelWidth() * lr.getXIntercept()));
      }
   }

   /**
    * Fits a simple line to the upper 4% of the spectrum data
    */
   public static final DuaneHuntLimit LinearDuaneHunt = new LinearDuaneHuntAlgorithm();
   /**
    * Backs up from the beam energy to the first channel which starts a range of
    * channels which average above a certain threshold.
    */
   public static final DuaneHuntLimit CrudeDuaneHunt = new CrudeDuaneHuntAlgorithm();
   /**
    * Starts with the CrudeDuaneHunt algorithms and then does a linear fit to a
    * range of channels from the fifth order Savitsky-Golay filtered spectrum.
    */
   public static final DuaneHuntLimit AltDuaneHunt = new AltDuaneHuntAlgorithm();
   /**
    * Either CrudeDuaneHunt or AltDuaneHunt are reasonable choices.
    */
   public static final DuaneHuntLimit DefaultDuaneHunt = AltDuaneHunt;

   // public static final DuaneHuntLimit DefaultDuaneHunt = CrudeDuaneHunt;

   static private class LinearDuaneHuntAlgorithm
      extends DuaneHuntLimit {

      /**
       * Constructs a BasicDuaneHunt
       */
      public LinearDuaneHuntAlgorithm() {
         super("Linear Duane-Hunt", LitReference.NullReference);
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.DuaneHuntLimit#compute(gov.nist.microanalysis.EPQLibrary.ISpectrumData)
       */
      @Override
      public double compute(ISpectrumData spec) {
         /*
          * Make an initial guess. In very sparsely populated spectra or in
          * spectra in which the x-range is less than the beam energy, it is
          * possible that the Duane-Hunt limit is larger than this number.
          * Usually however this channel is above the Duane-Hunt limit due to
          * sum counts.
          */
         final SpectrumProperties sp = spec.getProperties();
         final double e0 = sp.getNumericWithDefault(SpectrumProperties.BeamEnergy, Double.NaN);
         int high = spec.getChannelCount() - 1;
         if(!Double.isNaN(e0)) {
            final int ch = SpectrumUtils.channelForEnergy(spec, 1000.0 * e0);
            if(ch > spec.getChannelCount())
               return Double.NaN;
            high = ch;
         }
         int chMax = 0;
         {
            /*
             * Handle the screwball case of a couple of high energy counts
             * likely due to coincidence events. Otherwise I'd simply choose the
             * largest non-zero channel.
             */
            for(int ch = high; ch >= 0; --ch)
               if(spec.getCounts(ch) > 4) {
                  chMax = ch;
                  for(int ch2 = ch + 1; ch2 < spec.getChannelCount(); ++ch2)
                     if(spec.getCounts(ch2) == 0) {
                        chMax = ch2 - 1;
                        break;
                     }
                  break;
               }
         }
         if(chMax == 0)
            return Double.NaN;
         final LinearRegression lr = new LinearRegression();
         final double OFF = 0.0;
         /*
          * Select the upper 4% of the spectra to fit.
          */
         int start = (94 * chMax) / 100, end = chMax;
         for(int ch = start; ch < end; ++ch) {
            final double cx = spec.getCounts(ch);
            if(cx > 0.0)
               lr.addDatum(SpectrumUtils.avgEnergyForChannel(spec, ch), cx + OFF, Math.sqrt(cx));
         }
         /*
          * Step backwards towards lower energy until...
          */
         while(end > (chMax / 2)) {
            /*
             * Stop when the x-intercept is greater than the end channel. This
             * test depends on the observation that the curvature changes from
             * convex to slightly convex.
             */
            final double est = lr.getXIntercept();
            if(est > SpectrumUtils.avgEnergyForChannel(spec, end)) {
               // Can't reliably extrapolate much beyond the end of the data
               if(est > SpectrumUtils.maxEnergyForChannel(spec, spec.getChannelCount()))
                  return Double.NaN;
               return ToSI.eV(est);
            }
            final int INCREMENT = 10;
            start -= INCREMENT;
            end -= INCREMENT;
            for(int i = 0; i < INCREMENT; ++i) {
               { // Add channels to the start
                  final int ch = start + i;
                  final double cx = spec.getCounts(ch);
                  if(cx > 0.0)
                     lr.addDatum(SpectrumUtils.avgEnergyForChannel(spec, ch), cx + OFF, Math.min(1.0, Math.sqrt(cx)));
               }
               { // Remove channels from the end
                  final int ch = end + i;
                  final double cx = spec.getCounts(ch);
                  if(cx > 0.0)
                     lr.removeDatum(SpectrumUtils.avgEnergyForChannel(spec, ch), cx + OFF, Math.min(1.0, Math.sqrt(cx)));
               }
            }
         }
         return Double.NaN;
      }
   };
}
