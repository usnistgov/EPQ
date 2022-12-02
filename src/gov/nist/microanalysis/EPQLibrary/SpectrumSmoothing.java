package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.EPQLibrary.LitReference.Author;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Implementations of mechanisms for smoothing spectra.
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
abstract public class SpectrumSmoothing extends AlgorithmClass implements ISpectrumTransformation {

   /**
    * Constructs a SpectrumSmoothing object
    * 
    * @param name
    * @param ref
    */
   protected SpectrumSmoothing(String name, LitReference ref) {
      super("Spectrum Smoothing", name, ref);
   }

   /**
    * Constructs a SpectrumSmoothing object
    * 
    * @param name
    * @param ref
    */
   protected SpectrumSmoothing(String name, String ref) {
      super("Spectrum Smoothing", name, ref);
   }

   /**
    * getAllImplementations
    * 
    * @return (non-Javadoc)
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#getAllImplementations()
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }

   /**
    * initializeDefaultStrategy (non-Javadoc)
    * 
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#initializeDefaultStrategy()
    */
   @Override
   protected void initializeDefaultStrategy() {
   }

   @Override
   abstract public ISpectrumData compute(ISpectrumData spec);

   private static class SavitzkyGolay extends SpectrumSmoothing {

      private final int mM;

      private final double[][] Coefficients = new double[][]{new double[]{35, 17, 12, -3}, new double[]{21, 7, 6, 3, -2},
            new double[]{231, 59, 54, 39, 14, -21}, new double[]{429, 89, 84, 69, 44, 9, -36}, new double[]{143, 25, 24, 21, 16, 9, 0, -11},
            new double[]{1105, 167, 162, 147, 122, 87, 42, -13, -78}, new double[]{323, 43, 42, 39, 34, 27, 18, 7, -6, -21},
            new double[]{2261, 269, 264, 249, 224, 189, 144, 89, 24, -51, -136},
            new double[]{3059, 329, 324, 309, 284, 249, 204, 149, 84, 9, -76, -171},
            new double[]{805, 79, 78, 75, 70, 63, 54, 43, 30, 15, -2, -21, -42},
            new double[]{5175, 467, 462, 447, 422, 387, 342, 287, 222, 147, 62, -33, -138, -253}};

      public SavitzkyGolay(int m) {
         super("Savitsky-Golay",
               new LitReference.JournalArticle(LitReference.AnalChem, "35", "1627", 1964, new Author[]{LitReference.Savitzky, LitReference.Golay}));
         mM = m;
      }

      @Override
      public ISpectrumData compute(ISpectrumData spec) {
         final EditableSpectrum res = new EditableSpectrum(spec);
         final double[] coeff = Coefficients[mM - 2];
         for (int i = 0; i < spec.getChannelCount(); ++i) {
            double c = 0.0;
            final int maxCh = Math.min(spec.getChannelCount(), i + mM);
            for (int ch = Math.max(0, i - mM); ch < maxCh; ++ch)
               c += coeff[Math.abs(ch - i) + 1] * spec.getCounts(ch);
            res.setCounts(i, c / coeff[0]);
         }
         final String name = "Savitzky-Golay[" + spec.toString() + "," + Integer.toString(mM) + "]";
         res.getProperties().setTextProperty(SpectrumProperties.SpecimenDesc, name);
         res.getProperties().setTextProperty(SpectrumProperties.SpectrumDisplayName, name);
         return res;
      }
   };

   // A series of argument free constructors...
   public static class SavitzkyGolay2SpectrumSmoothing extends SavitzkyGolay {
      protected SavitzkyGolay2SpectrumSmoothing() {
         super(2);
      }
   }

   public static class SavitzkyGolay3SpectrumSmoothing extends SavitzkyGolay {
      protected SavitzkyGolay3SpectrumSmoothing() {
         super(3);
      }
   }

   public static class SavitzkyGolay4SpectrumSmoothing extends SavitzkyGolay {
      protected SavitzkyGolay4SpectrumSmoothing() {
         super(4);
      }
   }

   public static class SavitzkyGolay5SpectrumSmoothing extends SavitzkyGolay {
      protected SavitzkyGolay5SpectrumSmoothing() {
         super(5);
      }
   }

   public static class SavitzkyGolay6SpectrumSmoothing extends SavitzkyGolay {
      protected SavitzkyGolay6SpectrumSmoothing() {
         super(6);
      }
   }

   public static class SavitzkyGolay7SpectrumSmoothing extends SavitzkyGolay {
      protected SavitzkyGolay7SpectrumSmoothing() {
         super(7);
      }
   }

   public static class SavitzkyGolay8SpectrumSmoothing extends SavitzkyGolay {
      protected SavitzkyGolay8SpectrumSmoothing() {
         super(8);
      }
   }

   public static class SavitzkyGolay9SpectrumSmoothing extends SavitzkyGolay {
      protected SavitzkyGolay9SpectrumSmoothing() {
         super(9);
      }
   }

   public static class SavitzkyGolay10SpectrumSmoothing extends SavitzkyGolay {
      protected SavitzkyGolay10SpectrumSmoothing() {
         super(10);
      }
   }

   public static class SavitzkyGolay11SpectrumSmoothing extends SavitzkyGolay {
      protected SavitzkyGolay11SpectrumSmoothing() {
         super(11);
      }
   }

   public static class SavitzkyGolay12SpectrumSmoothing extends SavitzkyGolay {
      protected SavitzkyGolay12SpectrumSmoothing() {
         super(12);
      }
   }

   public static final SpectrumSmoothing SavitzkyGolay2 = new SavitzkyGolay2SpectrumSmoothing();
   public static final SpectrumSmoothing SavitzkyGolay3 = new SavitzkyGolay3SpectrumSmoothing();
   public static final SpectrumSmoothing SavitzkyGolay4 = new SavitzkyGolay4SpectrumSmoothing();
   public static final SpectrumSmoothing SavitzkyGolay5 = new SavitzkyGolay5SpectrumSmoothing();
   public static final SpectrumSmoothing SavitzkyGolay6 = new SavitzkyGolay6SpectrumSmoothing();
   public static final SpectrumSmoothing SavitzkyGolay7 = new SavitzkyGolay7SpectrumSmoothing();
   public static final SpectrumSmoothing SavitzkyGolay8 = new SavitzkyGolay8SpectrumSmoothing();
   public static final SpectrumSmoothing SavitzkyGolay9 = new SavitzkyGolay9SpectrumSmoothing();
   public static final SpectrumSmoothing SavitzkyGolay10 = new SavitzkyGolay10SpectrumSmoothing();
   public static final SpectrumSmoothing SavitzkyGolay11 = new SavitzkyGolay11SpectrumSmoothing();
   public static final SpectrumSmoothing SavitzkyGolay12 = new SavitzkyGolay12SpectrumSmoothing();
   public static final SpectrumSmoothing None = new NoSpectrumSmoothing();

   public static class NoSpectrumSmoothing extends SpectrumSmoothing {

      protected NoSpectrumSmoothing() {
         super("None", "");
      }

      @Override
      public ISpectrumData compute(ISpectrumData spec) {
         return spec;
      }

   }

   static private final AlgorithmClass mAllImplementations[] = {SavitzkyGolay2, SavitzkyGolay3, SavitzkyGolay4, SavitzkyGolay5, SavitzkyGolay6,
         SavitzkyGolay7, SavitzkyGolay8, SavitzkyGolay9, SavitzkyGolay10, SavitzkyGolay11, SavitzkyGolay12, None};
}
