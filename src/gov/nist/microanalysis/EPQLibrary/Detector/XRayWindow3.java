package gov.nist.microanalysis.EPQLibrary.Detector;

import java.util.Arrays;
import java.util.Objects;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.Utility.CSVReader;
import gov.nist.microanalysis.Utility.Math2;

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

public class XRayWindow3
   implements
   IXRayWindowProperties {

   private String mName;
   private final SpectrumProperties mProperties;
   private String mResourceName;

   transient protected double[] mEnergy;
   transient protected double[] mTransmission; // Transmission
   // values
   transient protected int mHash = Integer.MAX_VALUE;

   private XRayWindow3(XRayWindow3 xrw3) {
      mEnergy = xrw3.mEnergy.clone();
      mTransmission = xrw3.mTransmission.clone();
      mName = xrw3.mName;
      mProperties = xrw3.mProperties.clone();
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
   public XRayWindow3(String resourceName, SpectrumProperties sp)
         throws EPQException {
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
      int ch = Arrays.binarySearch(mEnergy, energy);
      if(ch < 0)
         ch = -ch - 1;
      return mTransmission[Math.min(ch, mTransmission.length - 1)];
   }

   private void loadTransmission() {
      final CSVReader rd = new CSVReader.ResourceReader(mResourceName, false);
      final double[][] data = Math2.transpose(rd.getResource(XRayWindow3.class));
      assert (data.length == 2);
      mEnergy = Math2.multiply(ToSI.eV(1.0), data[0].clone());
      mTransmission = data[1].clone();
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
   public XRayWindow3 clone() {
      return new XRayWindow3(this);
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
      if(mHash == Integer.MAX_VALUE)
         mHash = Objects.hash(mProperties, mName, mResourceName);
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
      final XRayWindow3 other = (XRayWindow3) obj;
      if(!Arrays.equals(mEnergy, other.mEnergy))
         return false;
      if(!Arrays.equals(mTransmission, other.mTransmission))
         return false;
      if(!Objects.equals(mName, other.mName))
         return false;
      if(!Objects.equals(mResourceName, other.mResourceName))
         return false;
      if(!Objects.equals(mProperties, other.mProperties))
         return false;
      return true;
   }
}
