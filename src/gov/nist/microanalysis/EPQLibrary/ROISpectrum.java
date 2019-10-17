package gov.nist.microanalysis.EPQLibrary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import gov.nist.microanalysis.EPQLibrary.BremsstrahlungAnalytic.QuadraticBremsstrahlung;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.EPQLibrary.Detector.IXRayDetector;
import gov.nist.microanalysis.EPQTools.WriteSpectrumAsEMSA1_0;

/**
 * <p>
 * A mechanism for extracting the channels in a ROI from a spectrum.
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

public class ROISpectrum
   extends
   DerivedSpectrum {
   private double mModelThreshold = 1.0e3;

   private int mLowChannel, mHighChannel;
   private double[] mData = null; // the resulting spectrum (trimmed)
   // Unfortunately modeling the Bremsstrahlung background is painfully slow
   // (~0.1 sec per)
   private static final boolean DUMP = false;

   public int lowChannel() {
      return mLowChannel;
   }

   public int highChannel() {
      return mHighChannel;
   }

   private void computeData() {
      boolean modeled = false;
      mData = new double[mHighChannel - mLowChannel];
      final ISpectrumData src = getBaseSpectrum();
      // Attempt to model Bremsstrahlung for lower energy peaks.
      if(SpectrumUtils.minEnergyForChannel(this, mLowChannel) < mModelThreshold) {
         final SpectrumProperties props = src.getProperties();
         Composition comp = props.getCompositionWithDefault(SpectrumProperties.StandardComposition, null);
         if(comp == null)
            comp = props.getCompositionWithDefault(SpectrumProperties.MicroanalyticalComposition, null);
         final IXRayDetector det = props.getDetector();
         assert src != null;
         if((comp != null) && (det instanceof EDSDetector)) {
            // final QuadraticBremsstrahlung qb = new
            // BremsstrahlungAnalytic.DTSAQuadratic();
            final QuadraticBremsstrahlung qb = new BremsstrahlungAnalytic.Lifshin1974Model();
            try {
               final ISpectrumData back = qb.fitBackground((EDSDetector) det, src, comp);
               double minSc, maxSc;
               {
                  final double minSrc = SpectrumUtils.estimateLowBackground(src, mLowChannel)[0];
                  final double maxSrc = SpectrumUtils.estimateHighBackground(src, mHighChannel)[0];
                  final double minBack = SpectrumUtils.estimateLowBackground(back, mLowChannel)[0];
                  final double maxBack = SpectrumUtils.estimateHighBackground(back, mHighChannel)[0];
                  minSc = (minBack > 0.0) && (minSrc > 0.0) ? (minSrc / minBack) : Double.MAX_VALUE;
                  maxSc = (maxBack > 0.0) && (maxSrc > 0.0) ? (maxSrc / maxBack) : Double.MAX_VALUE;
               }
               if((minSc > 0.1) && (minSc < 10.0) && (maxSc > 0.1) && (maxSc < 10.0)) {
                  for(int i = mLowChannel; i < mHighChannel; ++i) {
                     final double k = ((double) (i - mLowChannel)) / ((double) (mHighChannel - mLowChannel));
                     mData[i - mLowChannel] = src.getCounts(i) - ((minSc + (k * (maxSc - minSc))) * back.getCounts(i));
                  }
                  modeled = true;
                  if(DUMP)
                     try {
                        System.err.println("Writing: " + toString() + ".msa");
                        final String fn1 = toString() + "-[" + mLowChannel + "," + mHighChannel + "].msa";
                        saveSpectrum(this, fn1);
                        final SpectrumMath delta = new SpectrumMath(src);
                        delta.subtract(this, 1.0);
                        final String fn2 = toString() + "-[" + mLowChannel + "," + mHighChannel + "]-brem.msa";
                        saveSpectrum(delta, fn2);
                        final String fn3 = toString() + "-[" + mLowChannel + "," + mHighChannel + "]-model.msa";
                        saveSpectrum(back, fn3);
                     }
                     catch(final FileNotFoundException e) {
                        e.printStackTrace();
                     }
               } else if(DUMP)
                  System.err.print("Not fitting: " + toString() + " - " + minSc + ", " + maxSc + " - [" + mLowChannel + ","
                        + mHighChannel + "]");
            }
            catch(final EPQException e) {
               modeled = false;
            }
         }
      }
      if(!modeled) {
         final double lowBkgd = SpectrumUtils.estimateLowBackground(src, mLowChannel)[0];
         final double highBkgd = SpectrumUtils.estimateHighBackground(src, mHighChannel)[0];
         for(int i = mLowChannel; i < mHighChannel; ++i)
            mData[i - mLowChannel] = src.getCounts(i)
                  - (lowBkgd + (((highBkgd - lowBkgd) * (i - mLowChannel)) / (mHighChannel - mLowChannel)));
      }
   }

   private void saveSpectrum(ISpectrumData spec, final String fn1)
         throws FileNotFoundException, EPQException {
      try (final FileOutputStream fos1 = new FileOutputStream(new File("c://temp//", fn1))) {
         WriteSpectrumAsEMSA1_0.write(spec, fos1, WriteSpectrumAsEMSA1_0.Mode.COMPATIBLE);
      }
      catch(final IOException e) {
         e.printStackTrace();
      }
   }

   /**
    * ROISpectrum - Computes a spectrum that consists of the data in the
    * specified set of channels. The resulting counts are background corrected
    * using a trapesoidal algorithm.
    * 
    * @param sd ISpectrumData
    * @param lowChannel int
    * @param highChannel int
    */
   private ROISpectrum(ISpectrumData sd, int lowChannel, int highChannel) {
      super(sd);
      assert (sd != null);
      getProperties().setBooleanProperty(SpectrumProperties.IsROISpectrum, true);
      setROI(lowChannel, highChannel);
   }

   /**
    * ROISpectrum - The spectrum sd is assumed to be a reference for the
    * specified element and transition family. The reference is assumed to be
    * collected on a EDS detector with the specified full width at half maximum.
    * This constructor creates a background corrected reference for the
    * specified transition family.
    * 
    * @param sd ISpectrumData - The reference spectrum
    * @param el Element - The element
    * @param family int - The transition family (one of AtomicShell.KFamily,
    *           AtomicShell.LFamily, AtomicShell.MFamily, NFamily)
    * @param fwhmAtMnKa double - in eV
    * @throws EPQException
    */
   public ROISpectrum(ISpectrumData sd, Element el, int family, double fwhmAtMnKa)
         throws EPQException {
      this(sd, 0, 1);
      setROI(el, family, fwhmAtMnKa);
   }

   public ROISpectrum(ISpectrumData sd, RegionOfInterestSet.RegionOfInterest roi, double modelThresh) {
      this(sd, 0, 1);
      mLowChannel = SpectrumUtils.bound(sd, SpectrumUtils.channelForEnergy(sd, FromSI.eV(roi.lowEnergy())-1.5*roi.getModel().getFWHMatMnKa()));
      mHighChannel = SpectrumUtils.bound(sd, SpectrumUtils.channelForEnergy(sd, FromSI.eV(roi.highEnergy())));
      mModelThreshold = modelThresh;
   }

   /**
    * setROI - Permits you to change the ROI associated with this spectrum.
    * 
    * @param lowChannel int
    * @param highChannel int
    */
   private void setROI(int lowChannel, int highChannel) {
      final ISpectrumData src = getBaseSpectrum();
      if(lowChannel > highChannel) {
         // Swap them
         final int tmp = lowChannel;
         lowChannel = highChannel;
         highChannel = tmp;
      }
      lowChannel = SpectrumUtils.bound(src, lowChannel);
      highChannel = SpectrumUtils.bound(src, highChannel);
      if(lowChannel == highChannel)
         highChannel = lowChannel + 1;
      if((mLowChannel != lowChannel) || (mHighChannel != highChannel)) {
         mLowChannel = lowChannel;
         mHighChannel = highChannel;
         mData = null;
      }
   }

   /**
    * setROI - Sets the ROI associated with this spectrum to an ROI surrounding
    * the specified transition family for the specified element. The fwhmAtMnKa
    * is used calculate the width of the line at the line energy.
    * 
    * @param el Element - The element
    * @param family int - The transition family (one of AtomicShell.KFamily,
    *           AtomicShell.LFamily, AtomicShell.MFamily, NFamily)
    * @param fwhmAtMnKa double - in eV
    * @throws EPQException
    */
   private void setROI(Element el, int family, double fwhmAtMnKa)
         throws EPQException {
      assert (family >= AtomicShell.KFamily);
      assert (family <= AtomicShell.NFamily);
      assert (fwhmAtMnKa >= 100.0);
      assert (fwhmAtMnKa <= 250.0);
      // Find the full energy extent of the major lines in this family.
      final int lowELine = XRayTransition.lineWithLowestEnergy(el, family);
      final int highELine = XRayTransition.lineWithHighestEnergy(el, family);
      if((lowELine == XRayTransition.None) || (highELine == XRayTransition.None))
         throw new EPQException("This element has no lines of the specified family");
      double lowE = FromSI.eV(XRayTransition.getEnergy(el, lowELine));
      if(lowE > SpectrumUtils.minEnergyForChannel(this, getChannelCount()))
         throw new EPQException("This element has no lines of the specified family in the measured energy range.");
      double highE = FromSI.eV(XRayTransition.getEnergy(el, highELine));
      if(highE <= 0.0)
         throw new EPQException("This element has no lines of the specified family in the measured energy range.");
      // Add a little extra to account for the linewidth
      lowE -= 1.6 * (SpectrumUtils.linewidth_eV(lowE, fwhmAtMnKa, SpectrumUtils.E_MnKa));
      highE += 1.6 * (SpectrumUtils.linewidth_eV(highE, fwhmAtMnKa, SpectrumUtils.E_MnKa));
      final ISpectrumData src = getBaseSpectrum();
      setROI(SpectrumUtils.channelForEnergy(src, lowE) - 1, SpectrumUtils.channelForEnergy(src, highE) + 1);
   }

   /**
    * The number of counts in the specified channel.
    * 
    * @param i int
    * @return double
    */
   @Override
   public double getCounts(int i) {
      // Wait until the channel data is required before calculating it...
      if(mData == null)
         computeData();
      return ((i < mLowChannel) || (i >= mHighChannel)) ? 0.0 : mData[i - mLowChannel];
   }

   /**
    * Returns the sum counts in the ROI
    * 
    * @return double
    */
   public double getTotalCounts() {
      if(mData == null)
         computeData();
      double res = 0.0;
      for(int i = mData.length - 1; i >= 0; --i)
         res += mData[i];
      return res;
   }

   /**
    * Get the threshold below which the background will be modeled.
    * 
    * @return in eV
    */
   public double getModelThreshold() {
      return mModelThreshold;
   }

   /**
    * Set the threshold below which the background will be modeled.
    * 
    * @param modelThreshold in eV
    */
   public void setModelThreshold(double modelThreshold) {
      if(mModelThreshold != modelThreshold) {
         mModelThreshold = modelThreshold;
         mData = null;
      }
   }
}
