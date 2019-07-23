package gov.nist.microanalysis.EPQLibrary;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import gov.nist.microanalysis.Utility.Math2;
import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * <p>
 * Various different algorithms for searching for ranges of channels which are
 * likely to represent peaks in the spectra. Any channel marked by these
 * algorithm could potentially contain a major line peak.
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
abstract public class PeakROISearch
   extends AlgorithmClass {

   protected PeakROISearch(String name, LitReference ref) {
      super("Peak Search", name, ref);
   }

   protected PeakROISearch(String name, String ref) {
      super("Peak Search", name, ref);
   }

   /**
    * Computes an array of doubles which represents the number of standard
    * deviations above background each channel is.
    * 
    * @param spec
    * @return double[]
    */
   abstract double[] compute(ISpectrumData spec);

   /**
    * Returns a set of channel-based region of interests corresponding to all
    * peaks in the specified spectrum with peak-to-background ratios exceeding
    * the specified threshold. Each int[] in the resultant set is an int[2]. The
    * first int represents the first channel in the ROI and the second int
    * represents the last channel in the ROI.
    * 
    * @param spec
    * @param thresh
    * @return Set&lt;int[]&gt;
    */
   public Set<int[]> peakROIs(ISpectrumData spec, double thresh) {
      final HashSet<int[]> res = new HashSet<int[]>();
      final ISpectrumData smoothed = SpectrumSmoothing.SavitzkyGolay6.compute(spec);
      final double[] stdDev = compute(smoothed);
      int[] roi = null;
      for(int i = stdDev.length - 1; i >= 0; --i)
         if(stdDev[i] > thresh) {
            if(roi == null) {
               roi = new int[2];
               roi[1] = i;
            }
         } else if(roi != null) {
            roi[0] = i + 1;
            res.add(roi);
            roi = null;
         }
      if(roi != null) {
         roi[0] = 0;
         res.add(roi);
      }
      return res;
   }

   /**
    * Get an exhaustive list of the possible elements contained within the
    * spectral data in the specified ISpectrumData.
    * 
    * @param spec
    * @param thresh
    * @return Set&lt;Element&gt;
    */
   public Set<Element> possibleElements(ISpectrumData spec, double thresh) {
      final TreeSet<Element> res = new TreeSet<Element>();
      final Set<int[]> rois = peakROIs(spec, thresh);
      final int[] shells = new int[] {
         AtomicShell.K,
         AtomicShell.LIII,
         AtomicShell.MV
      };
      for(int z = Element.elmH; z < 93; ++z) {
         final Element elm = Element.byAtomicNumber(z);
         boolean nextElm = false;
         for(final int sh : shells) {
            if(AtomicShell.exists(elm, sh))
               try {
                  final XRayTransition xrt = XRayTransition.getStrongestLine(new AtomicShell(elm, sh));
                  if(xrt != null) {
                     final int ch = SpectrumUtils.channelForEnergy(spec, FromSI.eV(xrt.getEnergy()));
                     for(final int[] roi : rois)
                        if((ch >= roi[0]) && (ch <= roi[1])) {
                           res.add(elm);
                           nextElm = true;
                           break;
                        }
                  }
               }
               catch(final EPQException e) {
                  // Just ignore it...
               }
            if(nextElm)
               break;
         }
      }
      return res;
   }

   /**
    * Get a less exhaustive list of the likely elements contained within the
    * spectral data in the specified ISpectrumData. This method determines based
    * on one strong line family whether the element is might exist in this
    * spectrum.
    * 
    * @param spec
    * @param thresh
    * @return Set&lt;Element&gt;
    */
   public Set<Element> likelyElements(ISpectrumData spec, double e0, double thresh) {
      final TreeSet<Element> res = new TreeSet<Element>();
      final Set<int[]> rois = peakROIs(spec, thresh);
      final int[] shells = new int[] {
         AtomicShell.K,
         AtomicShell.LIII,
         AtomicShell.MV
      };
      for(int z = Element.elmBe; z < 93; ++z)
         try {
            final Element elm = Element.byAtomicNumber(z);
            XRayTransition bestXrt = null;
            for(final int sh : shells) {
               if(AtomicShell.exists(elm, sh) && (AtomicShell.getEdgeEnergy(elm, sh) < (0.5 * e0)))
                  bestXrt = XRayTransition.getStrongestLine(new AtomicShell(elm, sh));
               if(bestXrt != null)
                  break;
            }
            if((bestXrt == null) && AtomicShell.exists(elm, AtomicShell.K))
               bestXrt = XRayTransition.getStrongestLine(new AtomicShell(elm, AtomicShell.K));
            if(bestXrt != null) {
               final int ch = SpectrumUtils.channelForEnergy(spec, FromSI.eV(bestXrt.getEnergy()));
               for(final int[] roi : rois)
                  if((ch >= roi[0]) && (ch <= roi[1])) {
                     res.add(elm);
                     break;
                  }
            }
         }
         catch(final EPQException e) {
            // Just ignore it...
         }
      return res;
   }

   /**
    * Performs a thresholding operation on the result from PeakSearch.compute.
    * If the channel counts is greater than thresh the channel is set to the
    * maximum peak height otherwise the channel is set to 0.0. The result is
    * returned as a spectrum which may be displayed or otherwise manipulated.
    * 
    * @param spec
    * @param thresh
    * @return ISpectrumData
    */
   public ISpectrumData computeAsSpectrum(ISpectrumData spec, double thresh) {
      spec = SpectrumUtils.applyZeroPeakDiscriminator(spec);
      final Set<int[]> peakRois = peakROIs(spec, thresh);
      final double[] ch = SpectrumUtils.toDoubleArray(spec);
      Arrays.fill(ch, 0.0);
      for(final int[] roi : peakRois) {
         double max = 0.0;
         for(int i = roi[0]; i <= roi[1]; ++i)
            max = Math.max(max, spec.getCounts(i));
         for(int i = roi[0]; i <= roi[1]; ++i)
            ch[i] = max;
      }
      return new DerivedSpectrum.BasicDerivedSpectrum(spec, ch, "Peaks[" + spec.toString() + "]");
   }

   /**
    * Performs a thresholding operation on the result from PeakSearch.compute.
    * If the channel counts is greater than thresh the channel is set to the
    * maximum peak height otherwise the channel is set to 0.0. The result is
    * returned as a spectrum which may be displayed or otherwise manipulated.
    * 
    * @param spec
    * @return ISpectrumData
    */
   public ISpectrumData computeAsSpectrum(ISpectrumData spec) {
      spec = SpectrumUtils.applyZeroPeakDiscriminator(spec);
      return new DerivedSpectrum.BasicDerivedSpectrum(spec, Math2.abs(compute(spec)), "Search[" + spec.toString() + "]");
   }

   /**
    * <p>
    * Based on an algorithm which first fits the background using the
    * Clayton1987 peak stripping algorithm then computes channel by channel the
    * number of standard deviations above background.
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
   static class Stripping
      extends PeakROISearch {

      protected Stripping() {
         super("Peak Stripping-based", "None");
      }

      @Override
      protected void initializeDefaultStrategy() {
         this.addDefaultAlgorithm(PeakStripping.class, PeakStripping.Clayton1987);
      }

      @Override
      public double[] compute(ISpectrumData spec) {
         final double[] res = new double[spec.getChannelCount()];
         final PeakStripping ps = (PeakStripping) this.getAlgorithm(PeakStripping.class);
         final ISpectrumData back = ps.computeBackground(spec);
         final int WIDTH = (int) Math.round(100.0 / spec.getChannelWidth());
         double bkg = 0.0, fore = 0.0;
         for(int i = spec.getChannelCount() - 1; i >= (spec.getChannelCount() - WIDTH); --i) {
            bkg += back.getCounts(i);
            fore += spec.getCounts(i);
         }
         for(int i = spec.getChannelCount() - WIDTH - 1; i >= 0; --i) {
            res[i + (WIDTH / 2)] = (fore - bkg) / Math.sqrt(bkg > (WIDTH * 4.0) ? bkg : 4.0 * WIDTH);
            bkg += back.getCounts(i) - back.getCounts(i + WIDTH);
            fore += spec.getCounts(i) - spec.getCounts(i + WIDTH);
         }
         return res;
      }
   };

   static public class GaussianFilter
      extends PeakROISearch {

      protected GaussianFilter() {
         super("Gaussian Filter", "None");
      }

      private UncertainValue2[] convolve(double[] v, double[] kernel) {
         assert (kernel.length % 2) == 1;
         final UncertainValue2[] res = new UncertainValue2[v.length];
         final int mid = kernel.length / 2;
         for(int i = 0; i < res.length; ++i) {
            double r = 0.0, dr = 0.0;
            for(int j = 0; j < kernel.length; ++j) {
               final int idx = Math2.bound((i + j) - mid, 0, v.length);
               r += kernel[j] * v[idx];
               dr += Math2.sqr(Math.abs(kernel[j]) * Math.sqrt(Math.max(0.0, v[idx])));
            }
            res[i] = new UncertainValue2(r, "S", Math.sqrt(dr));
         }
         return res;
      }

      @Override
      public double[] compute(ISpectrumData spec) {
         final double fwhm = SpectrumUtils.getFWHMAtMnKA(spec, 135.0);
         final double w = SpectrumUtils.fwhmToGaussianWidth(fwhm);
         final PeakStripping ps = (PeakStripping) this.getAlgorithm(PeakStripping.class);
         final double[] filter = new double[(2 * Math.max(3, (int) Math.round((3.0 * w) / spec.getChannelWidth()))) + 1];
         final int mid = filter.length / 2;
         final double den = SpectrumUtils.gaussian(0.0, w);
         for(int i = 0; i <= (filter.length / 2); ++i) {
            filter[i] = SpectrumUtils.gaussian(spec.getChannelWidth() * (i - mid), w) / den;
            filter[filter.length - 1 - i] = filter[i];
         }
         final UncertainValue2[] back = convolve(SpectrumUtils.toDoubleArray(ps.computeBackground(spec)), filter);
         final UncertainValue2[] fore = convolve(SpectrumUtils.toDoubleArray(spec), filter);
         final double[] diff = new double[fore.length];
         for(int i = 0; i < fore.length; ++i)
            diff[i] = back[i].uncertainty() > 0.0 ? UncertainValue2.divide(UncertainValue2.subtract(fore[i], back[i]), Math.max(1.0 / den, back[i].uncertainty())).doubleValue()
                  : 0.0;
         return diff;
      }

      @Override
      protected void initializeDefaultStrategy() {
         this.addDefaultAlgorithm(PeakStripping.class, PeakStripping.VanEspen2002);
      }
   }

   public final static PeakROISearch PeakStrippingSearch = new Stripping();
   public final static PeakROISearch GaussianSearch = new GaussianFilter();
   static private final AlgorithmClass mAllImplementations[] = new AlgorithmClass[] {
      PeakStrippingSearch,
      GaussianSearch
   };

   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mAllImplementations);
   }
}
