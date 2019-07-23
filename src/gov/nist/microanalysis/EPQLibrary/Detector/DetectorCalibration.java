package gov.nist.microanalysis.EPQLibrary.Detector;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;

/**
 * <p>
 * The base class for detector calibration objects.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nicholas
 * @version 1.0
 */
abstract public class DetectorCalibration
   implements Comparable<DetectorCalibration> {

   /**
    * The time from which this calibration becomes active. Calibrations are
    * ordered by date so that spectra are assigned to the calibration active
    * while the spectrum was collected.
    */
   private Date mActiveDate;

   /**
    * The spectrum properties associated with the calibration.
    */
   protected SpectrumProperties mProperties;

   transient protected int mHash;

   protected DetectorCalibration() {
      mActiveDate = new Date(System.currentTimeMillis());
      mProperties = new SpectrumProperties();
      mHash = Integer.MAX_VALUE;
   }

   protected DetectorCalibration(DetectorCalibration model) {
      super();
      mActiveDate = (Date) model.mActiveDate.clone();
      mProperties = model.mProperties.clone();
      mHash = Integer.MAX_VALUE;
   }

   private Object readResolve() {
      mHash = Integer.MAX_VALUE;
      return this;
   }

   public Date getActiveDate() {
      return mActiveDate;
   }

   public void setActiveDate(Date dt) {
      mActiveDate = dt;
   }

   public void makeBaseCalibration() {
      mActiveDate = new Date(0);
      assert mActiveDate.getTime() == 0;
   }

   /**
    * @see java.lang.Comparable#compareTo(java.lang.Object)
    */
   @Override
   public int compareTo(DetectorCalibration o) {
      int res = mActiveDate.compareTo(o.mActiveDate);
      if(res == 0)
         res = toString().compareTo(o.toString());
      // System.out.println(toString() + ".compareTo(" + o.toString() + ")=" +
      // Integer.toString(res));
      return res;
   }

   @Override
   public boolean equals(Object obj) {
      if(this == obj)
         return true;
      if(obj == null)
         return false;
      if(getClass() != obj.getClass())
         return false;
      final DetectorCalibration other = (DetectorCalibration) obj;
      assert (mActiveDate != null);
      assert (other.mActiveDate != null);
      assert (mProperties != null);
      assert (other.mProperties != null);
      final boolean res = mActiveDate.equals(other.mActiveDate) && toString().equals(other.toString())
            && mProperties.equals(other.mProperties);
      return res;
   }

   /**
    * The spectrum properties associated with this calibration.
    */
   public SpectrumProperties getProperties() {
      return mProperties;
   }

   @Override
   public String toString() {
      final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
      return (mActiveDate.getTime() == 0 ? "initial" : df.format(mActiveDate));
   }

   /**
    * Computes the detector efficiency given the detector properties and the
    * detector calibration.
    * 
    * @param dp
    * @return double[] Contains the channel-by-channel efficiency
    */
   abstract public double[] getEfficiency(DetectorProperties dp);

   /**
    * Is the specified x-ray transition visible at the specified beam energy on
    * the specified detector with this calibration?
    * 
    * @param xrt
    * @param eBeam
    * @return double[] Contains the channel-by-channel efficiency
    */
   abstract public boolean isVisible(XRayTransition xrt, double eBeam);

   @Override
   public int hashCode() {
      if(mHash == Integer.MAX_VALUE) {
         final int prime = 31;
         int result = 1;
         assert (mActiveDate != null);
         assert (mProperties != null);
         result = (prime * result) + mActiveDate.hashCode();
         result = (prime * result) + mProperties.hashCode();
         if(result == Integer.MAX_VALUE)
            result = Integer.MIN_VALUE;
         mHash = result;
      }
      // System.out.println("DC.hasCode()="+Integer.toString(mHash));
      return mHash;
   }
}
