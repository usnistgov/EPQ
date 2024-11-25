package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.List;

import gov.nist.microanalysis.Utility.Math2;

/**
 * Implements algorithms for computing a background spectrum and for computing a
 * background-stripped spectrum.
 */
abstract public class PeakStripping extends AlgorithmClass {

   static final double SQRT_2 = Math.sqrt(2.0);

   /**
    * Constructs a PeakStripping object
    * 
    * @param name
    * @param ref
    */
   protected PeakStripping(String name, String ref) {
      super("Peak Stripping", name, ref);
   }

   /**
    * Constructs a PeakStripping object
    * 
    * @param name
    * @param ref
    */
   protected PeakStripping(String name, LitReference ref) {
      super("Peak Stripping", name, ref);
   }

   /**
    * getAllImplementations
    * 
    * @return List of PeakStripping objects
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#getAllImplementations()
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   /**
    * initializeDefaultStrategy
    * 
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#initializeDefaultStrategy()
    */
   @Override
   protected void initializeDefaultStrategy() {
      // Nothing...
   }

   /**
    * computeBackground - Computes the background spectrum for the argument
    * source spectrum.
    * 
    * @param src
    * @return ISpectrumData
    */
   abstract public ISpectrumData computeBackground(ISpectrumData src);

   public ISpectrumData getStrippedSpectrum(ISpectrumData src) {
      final SpectrumMath sm = new SpectrumMath(src);
      sm.subtract(computeBackground(src), 1.0);
      sm.getProperties().setTextProperty(SpectrumProperties.SpecimenDesc, "Stripped[" + src.toString() + "]");
      return sm;
   }

   static public class Ryan1988PeakStripping extends PeakStripping {
      public Ryan1988PeakStripping() {
         super("Ryan et al - 1988", "Ryan CG, Clayton E, Griffin WL, Sie SH, Cousens DR. Nucl Instrum Methods B34:396, 1988");
      }

      private final int mNIter = 30;
      private final int N_REDUC = 8;

      @Override
      protected void initializeDefaultStrategy() {
         addDefaultAlgorithm(SpectrumSmoothing.class, SpectrumSmoothing.SavitzkyGolay5);
      }

      @Override
      public ISpectrumData computeBackground(ISpectrumData src) {
         final SpectrumSmoothing ss = (SpectrumSmoothing) getAlgorithm(SpectrumSmoothing.class);
         final double[] ch = SpectrumUtils.toDoubleArray(ss.compute(src));
         ch[0] = 0.0;
         for (int i = 1; i < ch.length; ++i)
            ch[i] = Math.log(Math.log(Math.max(0.0, ch[i]) + 1.0) + 1.0);
         final double width = (0.5 * SpectrumUtils.getFWHMAtMnKA(src, 135.0)) / src.getChannelWidth();
         double redFac = 1.0;
         for (int n = 0; n < mNIter; ++n) {
            if (n > (mNIter - N_REDUC))
               redFac /= Math.sqrt(2.0);
            final int iw = Math.max((int) Math.round(width * redFac), 1);
            for (int i = 0; i < ch.length; ++i) {
               final int i1 = Math.max(i - iw, 0);
               final int i2 = Math.min(i + iw, ch.length - 1);
               ch[i] = Math.min(ch[i], 0.5 * (ch[i1] + ch[i2]));
            }
         }
         for (int i = 0; i < ch.length; ++i)
            ch[i] = Math.exp(Math.exp(ch[i]) - 1.0) - 1.0;
         return new DerivedSpectrum.BasicDerivedSpectrum(src, ch, "Ryan[" + src.toString() + "]");
      }
   }

   static public class Clayton1987PeakStripping extends PeakStripping {
      public Clayton1987PeakStripping() {
         super("Clayton - 1987", "Clayton E, Duerden P, Cohen DD. Nuclear Instrum Methods B22:91, 1987");
      }

      private final int MAX_ITERATIONS = 1000;

      @Override
      protected void initializeDefaultStrategy() {
      }

      @Override
      public ISpectrumData computeBackground(ISpectrumData src) {
         final double[] ch = SpectrumUtils.toDoubleArray(src);
         double delta = 0.0, delta0 = -Double.MAX_VALUE;
         for (int iter = 0; (iter < MAX_ITERATIONS) && (delta > (1.0e-6 * delta0)); ++iter) {
            delta = 0.0;
            for (int i = ch.length - 2; i >= 1; --i) {
               final double m = 0.5 * (ch[i + 1] + ch[i - 1]);
               if (ch[i] > m) {
                  delta += ch[i] - m;
                  ch[i] = m;
               }
            }
            if (delta0 == -Double.MAX_VALUE)
               delta0 = delta;
         }
         return new DerivedSpectrum.BasicDerivedSpectrum(src, ch, "Clayton[" + src.toString() + "]");
      }
   }

   static public class VanEspen2002PeakStripping extends PeakStripping {

      private final int mNIter = 30;
      private static final int N_REDUC = 8;

      public VanEspen2002PeakStripping() {
         super("Van Espen 2002", new LitReference.BookChapter(LitReference.HandbookOfXRaySpectrometry,
               new LitReference.Author[]{new LitReference.Author("Piet", "Van Espen", "")}));
      }

      @Override
      public ISpectrumData computeBackground(ISpectrumData src) {
         final SpectrumSmoothing ss = (SpectrumSmoothing) getAlgorithm(SpectrumSmoothing.class);
         final double[] ch = SpectrumUtils.toDoubleArray(ss.compute(src));
         for (int i = 0; i < ch.length; ++i)
            ch[i] = Math.sqrt(Math.max(0.0, ch[i]));
         final double width = (0.5 * SpectrumUtils.getFWHMAtMnKA(src, 135.0)) / src.getChannelWidth();
         double redFac = 1.0;
         for (int n = 0; n < mNIter; ++n) {
            if (n > (mNIter - N_REDUC))
               redFac /= Math.sqrt(2.0);
            final int iw = Math.max((int) Math.round(width * redFac), 1);
            for (int i = 0; i < ch.length; ++i) {
               final int i1 = Math.max(i - iw, 0);
               final int i2 = Math.min(i + iw, ch.length - 1);
               ch[i] = Math.min(ch[i], 0.5 * (ch[i1] + ch[i2]));
            }
         }
         for (int i = 0; i < ch.length; ++i)
            ch[i] = Math2.sqr(ch[i]);
         return new DerivedSpectrum.BasicDerivedSpectrum(src, ch, "Van Espen[" + src.toString() + "]");
      }

      @Override
      protected void initializeDefaultStrategy() {
         addDefaultAlgorithm(SpectrumSmoothing.class, SpectrumSmoothing.SavitzkyGolay10);
      }
   }

   static public final PeakStripping Clayton1987 = new Clayton1987PeakStripping();
   static public final PeakStripping Ryan1988 = new Ryan1988PeakStripping();
   static public final PeakStripping VanEspen2002 = new VanEspen2002PeakStripping();

   static private final AlgorithmClass mAllImplementations[] = {Clayton1987, VanEspen2002, Ryan1988};
}
