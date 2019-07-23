package gov.nist.microanalysis.EPQLibrary.Detector;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.Utility.CSVReader;

/**
 * <p>
 * Models the transmission of an x-ray window based on tabulated transmission
 * values stored in a resource.
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

public class XRayWindow2
   implements
   IXRayWindowProperties {

   private String mName;
   private final double mChWidth;
   private final SpectrumProperties mProperties;
   private String mResourceName;

   transient protected double[] mTransmission; // Transmission
   transient protected double mMin, mMax; // Minimum and maximum tabulated
   // values
   transient protected int mHash = Integer.MAX_VALUE;
   transient protected IXRayWindowProperties mHighEnergyWindow;
   transient protected double mHighEnergyScale;

   private XRayWindow2(XRayWindow2 xrw2) {
      mTransmission = xrw2.mTransmission.clone();
      mChWidth = xrw2.mChWidth;
      mMin = xrw2.mMin;
      mMax = xrw2.mMax; // Minimum and maximum tabulated values
      mName = xrw2.mName;
      mProperties = xrw2.mProperties.clone();
   }

   /**
    * Constructs a XRayWindow2 using the specified resource for transmission
    * data and the specified channel width for binning. Seems to work ok.
    * Plotted input resource against XRayWindow2.transmission(..) and they
    * agree. 19-Feb-2008
    * 
    * @param resourceName "AP3_3.csv" or similar
    * @param chWidth in SI
    * @param sp SpectrumProperties associated with the Window
    * @throws EPQException
    */
   public XRayWindow2(String resourceName, double chWidth, SpectrumProperties sp)
         throws EPQException {
      mChWidth = chWidth;
      mProperties = sp.clone();
      mResourceName = resourceName;
   }

   private Object readResolve() {
      mHash = Integer.MAX_VALUE;
      return this;
   }

   @Override
   public double transmission(double energy) {
      if(mTransmission == null)
         loadTransmission();
      if(energy < mMin)
         return 0.0;
      if(energy < mMax)
         return mTransmission[(int) Math.round((energy - mMin) / mChWidth)];
      else
         return mHighEnergyWindow != null ? mHighEnergyScale * mHighEnergyWindow.transmission(energy)
               : mTransmission[mTransmission.length - 1];
   }

   private void loadTransmission() {
      final CSVReader rd = new CSVReader.ResourceReader(mResourceName, false);
      final double[][] data = rd.getResource(XRayWindow2.class);
      assert (data[0].length == 2);
      mMin = ToSI.eV(data[0][0]) + (mChWidth / 2.0);
      final double max = ToSI.eV(data[data.length - 1][0]);
      final int nBins = (int) ((max - mMin) / mChWidth);
      mMax = mMin + ((nBins - 1) * mChWidth);
      mTransmission = new double[nBins];
      double e = FromSI.eV(mMin);
      final double dE = FromSI.eV(mChWidth);
      int start, end = 0;
      for(int i = 0; i < nBins; i++, e += dE) { // i indexes into mTransmission
         start = end;
         end = data.length;
         for(int j = start; j < data.length; ++j)
            if(data[j][0] >= (e + (dE / 2.0))) {
               end = j;
               break;
            }
         if(end > start) {
            // average multiple values...
            double sum = 0.0;
            for(int j = start; j < end; ++j)
               sum += data[j][1];
            mTransmission[i] = sum / (end - start);
         } else
            // interpolate
            mTransmission[i] = data[start - 1][1] + ((data[start][1] - data[start - 1][1])
                  * ((e - data[start - 1][0]) / (data[start][0] - data[start - 1][0])));
      }
      final double gridTh = mProperties.getNumericWithDefault(SpectrumProperties.SupportGridThickness, Double.NaN);
      final double openArea = mProperties.getNumericWithDefault(SpectrumProperties.WindowOpenArea, Double.NaN);

      if(!(Double.isNaN(gridTh) || Double.isNaN(openArea))) {
         final Material si = new Material(new Composition(new Element[] {
            Element.Si
         }, new double[] {
            1.0
         }), ToSI.gPerCC(2.33));
         mHighEnergyWindow = new GridMountedWindow(si, 1.0e-3 * gridTh, 0.01 * openArea);
         mHighEnergyScale = mTransmission[mTransmission.length - 1] / mHighEnergyWindow.transmission(mMax);
      }

   }

   /**
    * getName
    * 
    * @return (non-Javadoc)
    * @see gov.nist.microanalysis.EPQLibrary.Detector.IXRayWindowProperties#getName()
    */
   @Override
   public String getName() {
      return mName;
   }

   /**
    * setName
    * 
    * @param name (non-Javadoc)
    * @see gov.nist.microanalysis.EPQLibrary.Detector.IXRayWindowProperties#setName(java.lang.String)
    */
   @Override
   public void setName(String name) {
      mName = name;
   }

   @Override
   public String toString() {
      return mName;
   }

   @Override
   public XRayWindow2 clone() {
      return new XRayWindow2(this);
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.Detector.IXRayWindowProperties#getProperties()
    */
   @Override
   public SpectrumProperties getProperties() {
      return mProperties;
   }

   @Override
   public int hashCode() {
      if(mHash == Integer.MAX_VALUE) {
         final int prime = 31;
         int result = 1;
         long temp;
         temp = Double.doubleToLongBits(mChWidth);
         result = (prime * result) + (int) (temp ^ (temp >>> 32));
         result = (prime * result) + ((mName == null) ? 0 : mName.hashCode());
         result = (prime * result) + ((mProperties == null) ? 0 : mProperties.hashCode());
         if(result == Integer.MAX_VALUE)
            result = Integer.MIN_VALUE;
         mHash = result;
      }
      return mHash;
   }

   @Override
   public boolean equals(Object obj) {
      if(this == obj)
         return true;
      if(obj == null)
         return false;
      if(getClass() != obj.getClass())
         return false;
      final XRayWindow2 other = (XRayWindow2) obj;
      if(Double.doubleToLongBits(mChWidth) != Double.doubleToLongBits(other.mChWidth))
         return false;
      if(mName == null) {
         if(other.mName != null)
            return false;
      } else if(!mName.equals(other.mName))
         return false;
      if(mProperties == null) {
         if(other.mProperties != null)
            return false;
      } else if(!mProperties.equals(other.mProperties))
         return false;
      return true;
   }
}
