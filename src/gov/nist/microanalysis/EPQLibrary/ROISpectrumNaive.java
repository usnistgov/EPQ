/**
 * 
 */
package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.Utility.LinearRegression;
import gov.nist.microanalysis.Utility.Math2;

/**
 * @author nritchie
 *
 */
public class ROISpectrumNaive extends DerivedSpectrum {

   private double mModelThreshold = 1.0e3;

   private int mLowChannel, mHighChannel;
   private double[] mData = null; // the resulting spectrum (trimmed)
   private RegionOfInterest mROI;
   final int BACKGROUND_EXTENT = 3;

   /**
    * setROI - Permits you to change the ROI associated with this spectrum.
    * 
    * @param lowChannel
    *           int
    * @param highChannel
    *           int
    */
   private void setROI(int lowChannel, int highChannel) {
      final ISpectrumData src = getBaseSpectrum();
      if (lowChannel > highChannel) {
         // Swap them
         final int tmp = lowChannel;
         lowChannel = highChannel;
         highChannel = tmp;
      }
      lowChannel = SpectrumUtils.bound(src, lowChannel);
      highChannel = SpectrumUtils.bound(src, highChannel);
      if (lowChannel == highChannel)
         highChannel = lowChannel + 1;
      if ((mLowChannel != lowChannel) || (mHighChannel != highChannel)) {
         mLowChannel = lowChannel;
         mHighChannel = highChannel;
         mData = null;
      }
   }

   public ROISpectrumNaive(ISpectrumData sd, RegionOfInterest roi, double modelThresh) {
      super(sd);
      assert (sd != null);
      getProperties().setBooleanProperty(SpectrumProperties.IsROISpectrum, true);
      int lowChannel = SpectrumUtils.channelForEnergy(sd, FromSI.eV(roi.lowEnergy())),
            highChannel = SpectrumUtils.channelForEnergy(sd, FromSI.eV(roi.highEnergy()));
      setROI(SpectrumUtils.bound(sd, lowChannel), SpectrumUtils.bound(sd, highChannel));
      mROI = roi;
      mModelThreshold = modelThresh;
   }

   private AtomicShell largestEdge() {
      XRayTransitionSet xrts = mROI.getAllTransitions();
      return xrts.getWeighiestTransition().getDestination();
   }

   private LinearRegression fitBackground(int cCh) {
      final ISpectrumData src = getBaseSpectrum();
      LinearRegression lr = new LinearRegression();
      for (int i = -BACKGROUND_EXTENT; i < BACKGROUND_EXTENT; ++i) {
         final int ch = Math2.bound(cCh + i, 0, src.getChannelCount() - 1);
         lr.addDatum(ch, src.getCounts(ch), Math.sqrt(Math.max(1.0, src.getCounts(ch))));
      }
      return lr;
   }

   private void computeData() {
      boolean modeled = false;
      mData = new double[mHighChannel - mLowChannel];
      final ISpectrumData src = getBaseSpectrum();
      if (SpectrumUtils.minEnergyForChannel(getBaseSpectrum(), mLowChannel) < mModelThreshold) {
         // Fit a line centered at mLowChannel and mHighChannel
         final LinearRegression low = fitBackground(mLowChannel), high = fitBackground(mHighChannel);
         // Check where
         final AtomicShell edge = largestEdge();
         int edgeCh = SpectrumUtils.channelForEnergy(src, FromSI.eV(edge.getEdgeEnergy()));
         if ((edgeCh > mLowChannel) && (edgeCh < mHighChannel) && (low.computeY(edgeCh) > high.computeY(edgeCh))) {
            int width = (int) Math.round(mROI.getModel().getFWHMatMnKa() / src.getChannelWidth()) / 2;
            int lowEdge = Math.max(mLowChannel, edgeCh - width);
            int highEdge = Math.min(mHighChannel, edgeCh + width);
            for (int i = mLowChannel; i < lowEdge; ++i)
               mData[i - mLowChannel] = src.getCounts(i) - low.computeY(i);
            LinearRegression.Line line = new LinearRegression.Line(lowEdge, low.computeY(lowEdge), highEdge, high.computeY(highEdge));
            for (int i = lowEdge; i < highEdge; ++i)
               mData[i - mLowChannel] = src.getCounts(i) - line.computeY(i);
            for (int i = highEdge; i < mHighChannel; ++i)
               mData[i - mLowChannel] = src.getCounts(i) - high.computeY(i);
            modeled = true;
         }
      }
      if (!modeled) {
         final double lowBkgd = SpectrumUtils.estimateLowBackground(src, mLowChannel)[0];
         final double highBkgd = SpectrumUtils.estimateHighBackground(src, mHighChannel)[0];
         for (int i = mLowChannel; i < mHighChannel; ++i)
            mData[i - mLowChannel] = src.getCounts(i) - (lowBkgd + (((highBkgd - lowBkgd) * (i - mLowChannel)) / (mHighChannel - mLowChannel)));
      }
   }

   /**
    * The number of counts in the specified channel.
    * 
    * @param i
    *           int
    * @return double
    */
   @Override
   public double getCounts(int i) {
      // Wait until the channel data is required before calculating it...
      if (mData == null)
         computeData();
      return ((i < mLowChannel) || (i >= mHighChannel)) ? 0.0 : mData[i - mLowChannel];
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
    * @param modelThreshold
    *           in eV
    */
   public void setModelThreshold(double modelThreshold) {
      if (mModelThreshold != modelThreshold) {
         mModelThreshold = modelThreshold;
         mData = null;
      }
   }

}
