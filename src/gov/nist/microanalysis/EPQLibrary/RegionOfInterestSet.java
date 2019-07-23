package gov.nist.microanalysis.EPQLibrary;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.Detector.DetectorLineshapeModel;
import gov.nist.microanalysis.Utility.HalfUpFormat;
import gov.nist.microanalysis.Utility.TextUtilities;

/**
 * <p>
 * RegionOfInterestSet implements a mechanism for computing one or more region
 * of interests based on the position of x-ray transitions and a detector
 * linewidth model.
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
public class RegionOfInterestSet
   implements
   Iterable<RegionOfInterestSet.RegionOfInterest>,
   Comparable<RegionOfInterestSet> {

   /**
    * Implements a simple model of a RegionOfInterest defined by a detector and
    * overlapping XRayTransitions.
    */
   // / TODO: Split ROI into BaseROI, SingleElementROI and MultiElementROI
   public class RegionOfInterest
      implements
      Comparable<RegionOfInterest> {
      private double mMinEnergy = Double.MAX_VALUE;
      private double mMaxEnergy = Double.MIN_VALUE;
      final private Set<XRayTransition> mTransitions = new TreeSet<XRayTransition>();
      private int mHash = Integer.MAX_VALUE;

      @Override
      public RegionOfInterest clone() {
         final RegionOfInterest res = new RegionOfInterest(this);
         return res;
      }

      public RegionOfInterest(RegionOfInterest roi) {
         mMinEnergy = roi.mMinEnergy;
         mMaxEnergy = roi.mMaxEnergy;
         mTransitions.addAll(roi.mTransitions);
         mHash = roi.mHash;
      }

      private RegionOfInterest(RegionOfInterest roi1, RegionOfInterest roi2) {
         super();
         assert roi1.parentsMatch(roi2);
         mMinEnergy = Math.min(roi1.mMinEnergy, roi2.mMinEnergy);
         mMaxEnergy = Math.max(roi1.mMaxEnergy, roi2.mMaxEnergy);
         mTransitions.addAll(roi1.mTransitions);
         mTransitions.addAll(roi2.mTransitions);
      }

      /**
       * Constructs a RegionOfInterest based upon the specified x-ray
       * transition. The extent of the RegionOfInterest is determined based on
       * the DetectorLinewidthModel and the other construtor parameters of the
       * encompassing RegionOfInterestSet. A RegionOfInterest can only exist
       * within a RegionOfInterestSet.
       * 
       * @param xrt
       */
      private RegionOfInterest(XRayTransition xrt) {
         super();
         add(xrt);
      }

      public int getTransitionCount() {
         return mTransitions.size();
      }

      /**
       * Adds the specified x-ray transition to this RegionOfInterest. The
       * extent of the RegionOfInterest is determined based on the
       * DetectorLinewidthModel and the other construtor parameters of the
       * encompassing RegionOfInterestSet and the current range of the
       * RegionOfInterest.
       * 
       * @param xrt
       */
      private void add(XRayTransition xrt) {
         try {
            final double e = xrt.getEnergy();
            final double w = xrt.getWeight(XRayTransition.NormalizeFamily);
            if(w > mMinIntensity) {
               final double eV = FromSI.eV(e);
               final double low = e - (ToSI.eV(mModel.leftWidth(eV, mMinIntensity / w)) + mLowExtra);
               final double high = e + (ToSI.eV(mModel.rightWidth(eV, mMinIntensity / w)) + mHighExtra);
               if(low < mMinEnergy)
                  mMinEnergy = low;
               if(high > mMaxEnergy)
                  mMaxEnergy = high;
            }
            mTransitions.add(xrt);
         }
         catch(final EPQException e) {
            // Ignore it...
         }
      }

      /**
       * Combines another RegionOfInterest with this one
       * 
       * @param roc
       */
      private void add(RegionOfInterest roc) {
         assert parentsMatch(roc);
         if(roc.mMinEnergy < mMinEnergy)
            mMinEnergy = roc.mMinEnergy;
         if(roc.mMaxEnergy > mMaxEnergy)
            mMaxEnergy = roc.mMaxEnergy;
         mTransitions.addAll(roc.mTransitions);
      }

      public XRayTransitionSet getAllTransitions() {
         return new XRayTransitionSet(mTransitions);
      }

      /**
       * The low energy encompassed by this RegionOfInterest
       * 
       * @return double in Joules
       */
      public double lowEnergy() {
         return mMinEnergy;
      }

      /**
       * The high energy encompassed by this RegionOfInterest
       * 
       * @return double in Joules
       */
      public double highEnergy() {
         return mMaxEnergy;
      }

      /**
       * Does this ROI contain the specified energy?
       * 
       * @param e in Joules
       * @return boolean
       */
      public boolean contains(double e) {
         return (e >= mMinEnergy) && (e <= mMaxEnergy);
      }

      public boolean contains(XRayTransition xrt) {
         try {
            return contains(xrt.getEnergy());
         }
         catch(final EPQException e) {
            return false;
         }
      }

      /**
       * Is the argument RegionOfInterest fully contained within this
       * RegionOfInterest
       * 
       * @param roi
       * @return boolean
       */
      public boolean fullyContains(RegionOfInterest roi) {
         return (roi.mMinEnergy >= mMinEnergy) && (roi.mMaxEnergy <= mMaxEnergy);
      }

      /**
       * Does the specified XRayTransition overlap this RegionOfInterest
       * 
       * @param xrt
       * @return boolean
       */
      public boolean intersects(XRayTransition xrt) {
         return intersects(new RegionOfInterest(xrt));
      }

      /**
       * Does the specified RegionOfInterest intesects this RegionOfInterest
       * 
       * @param roc
       * @return boolean
       */
      public boolean intersects(RegionOfInterest roc) {
         return (mMinEnergy <= roc.mMaxEnergy) && (mMaxEnergy >= roc.mMinEnergy);
      }

      /**
       * Compares two region of interests according to the mMinEnergy then the
       * mMaxEnergy.
       * 
       * @param o
       * @return int
       * @see java.lang.Comparable#compareTo(java.lang.Object)
       */
      @Override
      public int compareTo(RegionOfInterest o) {
         int res = mMinEnergy < o.mMinEnergy ? -1 : (mMinEnergy > o.mMinEnergy ? 1 : 0);
         if(res == 0)
            res = mMaxEnergy < o.mMaxEnergy ? -1 : (mMaxEnergy > o.mMaxEnergy ? 1 : 0);
         return res;
      }

      public XRayTransitionSet getXRayTransitionSet(Element elm) {
         final ArrayList<XRayTransition> xrts = new ArrayList<XRayTransition>();
         for(final XRayTransition xrt : mTransitions)
            if(xrt.getElement().equals(elm))
               xrts.add(xrt);
         return new XRayTransitionSet(xrts);
      }

      public DetectorLineshapeModel getModel() {
         return mModel;
      }

      public boolean equalOnROI(ISpectrumData spec1, ISpectrumData spec2) {
         final int min = SpectrumUtils.bound(spec1, SpectrumUtils.channelForEnergy(spec1, FromSI.eV(mMinEnergy)));
         final int max = SpectrumUtils.bound(spec1, SpectrumUtils.channelForEnergy(spec1, FromSI.eV(mMaxEnergy)));
         assert min == SpectrumUtils.bound(spec2, SpectrumUtils.channelForEnergy(spec2, FromSI.eV(mMinEnergy)));
         assert max == SpectrumUtils.bound(spec2, SpectrumUtils.channelForEnergy(spec2, FromSI.eV(mMaxEnergy)));
         for(int ch = min; ch < max; ++ch)
            if(spec1.getCounts(ch) != spec2.getCounts(ch))
               return false;
         return true;
      }

      @Override
      public String toString() {
         final StringBuffer sb = new StringBuffer(1024);
         sb.append(shortName());
         final NumberFormat nf = new HalfUpFormat("0.000");
         sb.append(" [");
         sb.append(nf.format(FromSI.keV(mMinEnergy)));
         sb.append(", ");
         sb.append(nf.format(FromSI.keV(mMaxEnergy)));
         sb.append(" keV]");
         return sb.toString();
      }

      public String shortName() {
         final StringBuffer sb = new StringBuffer(1024);
         final ArrayList<String> strs = new ArrayList<String>();
         for(final Element elm : getElementSet())
            strs.add(getXRayTransitionSet(elm).toString());
         sb.append(TextUtilities.toList(strs));
         return sb.toString();
      }

      @Override
      public int hashCode() {
         if(mHash == Integer.MAX_VALUE) {
            final int PRIME = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(mMaxEnergy);
            result = (PRIME * result) + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(mMinEnergy);
            result = (PRIME * result) + (int) (temp ^ (temp >>> 32));
            result = (PRIME * result) + ((mTransitions == null) ? 0 : mTransitions.hashCode());
            mHash = result;
            if(mHash == Integer.MAX_VALUE)
               mHash = Integer.MIN_VALUE;
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
         final RegionOfInterest other = (RegionOfInterest) obj;
         if(Double.doubleToLongBits(mMaxEnergy) != Double.doubleToLongBits(other.mMaxEnergy))
            return false;
         if(Double.doubleToLongBits(mMinEnergy) != Double.doubleToLongBits(other.mMinEnergy))
            return false;
         if(!mTransitions.equals(other.mTransitions))
            return false;
         return true;
      }

      /**
       * Does this RegionOfInterest correspond to a null region? (No energy
       * width)
       * 
       * @return boolean
       */
      public boolean isNull() {
         return mMinEnergy == Double.MAX_VALUE;
      }

      /**
       * Returns a list of the elements represented by this RegionOfInterest
       * 
       * @return TreeSet&lt;Element&gt;
       */
      public TreeSet<Element> getElementSet() {
         final TreeSet<Element> res = new TreeSet<Element>();
         for(final XRayTransition xrt : mTransitions)
            res.add(xrt.getElement());
         return res;
      }

      private RegionOfInterestSet getParent() {
         return RegionOfInterestSet.this;
      }

      private boolean parentsMatch(RegionOfInterestSet.RegionOfInterest roi) {
         final RegionOfInterestSet thisRois = getParent();
         final RegionOfInterestSet otherRois = roi.getParent();
         return (thisRois.mHighExtra == otherRois.mHighExtra) && (thisRois.mLowExtra == otherRois.mLowExtra)
               && (thisRois.mMinIntensity == otherRois.mMinIntensity) && (thisRois.mModel == otherRois.mModel);

      }

      /**
       * Returns a measure of the degree of overlap of two RegionOfInterest
       * objects.
       * 
       * @param roi
       * @return 0 is equivalent to no overlap, 1 means the RegionOfInterest
       *         objects are equal.
       */
      public double overlap(RegionOfInterest roi) {
         final double minMin = Math.min(roi.mMinEnergy, mMinEnergy);
         final double maxMin = Math.max(roi.mMinEnergy, mMinEnergy);
         final double minMax = Math.min(roi.mMaxEnergy, mMaxEnergy);
         final double maxMax = Math.max(roi.mMaxEnergy, mMaxEnergy);
         final double res = maxMax > minMin ? Math.max(0.0, (minMax - maxMin) / (maxMax - minMin)) : 0.0;
         assert res >= 0.0;
         assert res <= 1.0;
         return res;
      }
   }

   static private final int FIRST_TRANSITION = XRayTransition.KA1;

   static private final int LAST_TRANSITION = XRayTransition.MZ1;

   private final double mLowExtra; // in Joules

   private final double mHighExtra; // in Joules

   private final double mMinIntensity;

   final private DetectorLineshapeModel mModel;

   final private Set<RegionOfInterest> mROIs = new TreeSet<RegionOfInterest>();

   private int mHash = Integer.MAX_VALUE;

   /**
    * Constructs an empty RegionOfInterestSet using the specified detector
    * model, min intensity and low and high energy extra. The min intensity
    * determines the width of the ROI associated with x-ray transitions. The
    * width of the roi is taken to be the width necessary for the weighed
    * intensity of the line to fall to this level. Example: The KB is ~ 0.4 of
    * the total K line family. If minI = 0.001 then the width of the ROI will be
    * such that the intensity model.compute(width+center,center)=minI/weight =
    * 0.001/0.4.
    * 
    * @param model The DetectorLineshapeModel
    * @param minI The minimum weighted intensity
    * @param lowXtra In increment subtracted from the low energy side of the ROI
    *           (in Joules)
    * @param highXtra In increment added to the high energy side of the ROI (in
    *           Joules)
    */
   public RegionOfInterestSet(DetectorLineshapeModel model, double minI, double lowXtra, double highXtra) {
      super();
      assert (minI >= 0.0) && (minI < 1.0);
      assert model != null;
      mModel = model;
      mMinIntensity = Math.max(0.0001, minI);
      mLowExtra = Math.abs(lowXtra);
      mHighExtra = Math.abs(highXtra);
   }

   public RegionOfInterestSet(RegionOfInterest roi) {
      super();
      final RegionOfInterestSet roiParent = roi.getParent();
      mModel = roiParent.mModel;
      mMinIntensity = roiParent.mMinIntensity;
      mLowExtra = roiParent.mLowExtra;
      mHighExtra = roiParent.mHighExtra;
      add(new RegionOfInterest(roi));
   }

   /**
    * Constructs an empty RegionOfInterestSet based on the specified detector
    * model.
    * 
    * @param model
    * @param minI
    */
   public RegionOfInterestSet(DetectorLineshapeModel model, double minI) {
      this(model, minI, 0.0, 0.0);
   }

   /**
    * Add this XRayTransition to the RegionOfInterest set by adding it to the
    * appropriate RegionOfInterest. If RegionOfInterest's range overlap these
    * RegionOfInterest objects are combined.
    * 
    * @param xrt
    */
   public void add(XRayTransition xrt) {
      final RegionOfInterest newRoi = new RegionOfInterest(xrt);
      if(!newRoi.isNull())
         add(newRoi);
      else
         try {
            final double e = xrt.getEnergy();
            for(final RegionOfInterest roi : mROIs)
               if(roi.contains(e))
                  roi.mTransitions.add(xrt);
         }
         catch(final EPQException e) {
            // Ignore it...
         }
   }

   public void add(RegionOfInterest newRoi) {
      final ArrayList<RegionOfInterest> matches = new ArrayList<RegionOfInterest>();
      for(final RegionOfInterest roi : mROIs)
         if(newRoi.intersects(roi)) {
            roi.add(newRoi);
            matches.add(roi);
         }
      if(matches.size() == 0)
         mROIs.add(newRoi);
      else if(matches.size() > 1) {
         // Merge rois together
         final RegionOfInterest roi0 = matches.get(0);
         for(int i = 1; i < matches.size(); ++i) {
            final RegionOfInterest roiI = matches.get(i);
            roi0.add(roiI);
            mROIs.remove(roiI);
         }
      }
   }

   /**
    * Would an RegionOfInterest based on the argument XRayTransition intersect
    * with this RegionOfInterest?
    * 
    * @param xrt
    * @return boolean
    */
   public boolean intersects(XRayTransition xrt) {
      return intersects(new RegionOfInterest(xrt));
   }

   /**
    * Would an RegionOfInterest based on any of the XRayTransition objects in
    * the argument XRayTransitionSet intersect with this RegionOfInterest?
    * 
    * @param xrts
    * @return boolean
    */
   public boolean intesects(XRayTransitionSet xrts) {
      for(final XRayTransition xrt : xrts)
         if(intersects(xrt))
            return true;
      return false;
   }

   /**
    * Add each XRayTransition in this XRayTransitionSet to the appropriate
    * RegionOfInterest. Equivalent to <code>add(xrts,false)</code>.
    * 
    * @param xrts
    */
   public void add(XRayTransitionSet xrts) {
      add(xrts, false);
   }

   /**
    * Add each XRayTransition in this XRayTransitionSet to the appropriate
    * RegionOfInterest. <code>withOverlaps = true</code> will make an additional
    * search within each family represented in the <code>xrts</code> for other
    * lines which will overlap the resulting RegionOfInterestSet.
    * 
    * @param xrts
    * @param withOverlaps
    */
   public void add(XRayTransitionSet xrts, boolean withOverlaps) {
      if(xrts.size() > 0) {
         boolean k = false, l = false, m = false;
         for(final XRayTransition xrt : xrts) {
            add(xrt);
            if(withOverlaps)
               switch(xrt.getFamily()) {
                  case AtomicShell.KFamily:
                     k = true;
                     break;
                  case AtomicShell.LFamily:
                     l = true;
                     break;
                  case AtomicShell.MFamily:
                     m = true;
                     break;
               }
         }
         if(withOverlaps) {
            final Element elm = xrts.getElement();
            final XRayTransitionSet xrts3 = getXRayTransitionSet(elm);
            if(k) {
               boolean added = true;
               final XRayTransitionSet xrts2 = new XRayTransitionSet(elm, AtomicShell.KFamily);
               // loop until no more transitions can be added
               while(added) {
                  added = false;
                  for(final XRayTransition xrt : xrts2)
                     if(!xrts3.contains(xrt)) {
                        final RegionOfInterest roi = new RegionOfInterest(xrt);
                        if(intersects(roi)) {
                           added = true;
                           add(xrt);
                           xrts3.add(xrt);
                        }
                     }
               }
            }
            if(l) {
               boolean added = true;
               final XRayTransitionSet xrts2 = new XRayTransitionSet(elm, AtomicShell.LFamily);
               // loop until no more transitions can be added
               while(added) {
                  added = false;
                  for(final XRayTransition xrt : xrts2)
                     if(!xrts3.contains(xrt)) {
                        final RegionOfInterest roi = new RegionOfInterest(xrt);
                        if(intersects(roi)) {
                           added = true;
                           add(xrt);
                           xrts3.add(xrt);
                        }
                     }
               }
            }
            if(m) {
               boolean added = true;
               final XRayTransitionSet xrts2 = new XRayTransitionSet(elm, AtomicShell.MFamily);
               // loop until no more transitions can be added
               while(added) {
                  added = false;
                  for(final XRayTransition xrt : xrts2)
                     if(!xrts3.contains(xrt)) {
                        final RegionOfInterest roi = new RegionOfInterest(xrt);
                        if(intersects(roi)) {
                           added = true;
                           add(xrt);
                           xrts3.add(xrt);
                        }
                     }
               }
            }
         }
      }
   }

   /**
    * Add all available XRayTransitions with energy below the specified
    * maxEnergy to the RegionOfInterestSet.
    * 
    * @param elm
    * @param maxEnergy Edge energy in SI (Joules)
    * @param minWeight
    */
   public void add(Element elm, double maxEnergy, double minWeight) {
      for(int tr = FIRST_TRANSITION; tr <= LAST_TRANSITION; tr++)
         if(XRayTransition.exists(elm, tr) && (XRayTransition.getEdgeEnergy(elm, tr) < maxEnergy)
               && (XRayTransition.getWeight(elm, tr, XRayTransition.NormalizeFamily) >= minWeight))
            add(new XRayTransition(elm, tr));
   }

   /**
    * Add all available XRayTransitions for the specified Element in the
    * specified family (AtomicShell.KFamily, AtomicShell.LFamily,
    * AtomicShell.MFamily) with energy less than maxEnergy (in Joules) to this
    * RegionOfInterestSet.
    * 
    * @param elm
    * @param family AtomicShell.?Family where ? is K, L or M
    * @param maxEnergy in SI (Joules)
    */
   public void add(Element elm, int family, double maxEnergy) {
      for(int tr = FIRST_TRANSITION; tr <= LAST_TRANSITION; tr++)
         if(XRayTransition.getFamily(tr) == family)
            if(XRayTransition.exists(elm, tr) && (XRayTransition.getEdgeEnergy(elm, tr) < maxEnergy))
               add(new XRayTransition(elm, tr));
   }

   /**
    * Returns the minimum intensity paramter.
    * 
    * @return Returns the minimum intensity paramter.
    */
   public double getMinIntensity() {
      return mMinIntensity;
   }

   /**
    * Gets the current value assigned to highExtra
    * 
    * @return Returns the highExtra. (in Joules)
    */
   public double getHighExtra() {
      return mHighExtra;
   }

   /**
    * Gets the current value assigned to lowExtra
    * 
    * @return Returns the lowExtra. (in Joules)
    */
   public double getLowExtra() {
      return mLowExtra;
   }

   /**
    * Allows iteration over the RegionOfInterest objects.
    * 
    * @return Iterator&lt;RegionOfInterest&gt;
    */
   @Override
   public Iterator<RegionOfInterest> iterator() {
      return mROIs.iterator();
   }

   @Override
   public String toString() {
      final StringBuffer res = new StringBuffer(mROIs.size() * 20);
      for(final RegionOfInterest roi : mROIs) {
         res.append(res.length() == 0 ? "[" : ", ");
         res.append(roi.toString());
      }
      if(res.length() > 0)
         res.append("]");
      return res.toString();
   }

   /**
    * Tests whether any of the RegionOfInterest items in this ROIS overlap with
    * any of the RegionOfInterest items in <code>rois</code>.
    * 
    * @param rois RegionOfInterestSet
    * @return true if any overlaps; false if none overlap
    */
   public boolean intersects(RegionOfInterestSet rois) {
      for(final RegionOfInterest roi1 : this)
         for(final RegionOfInterest roi2 : rois)
            if(roi1.intersects(roi2))
               return true;
      return false;
   }

   /**
    * Tests whether any of the RegionOfInterest items in this intesects with the
    * specified ROI.
    * 
    * @param roi
    * @return true if any overlaps; false if none overlap
    */
   public boolean intersects(RegionOfInterest roi) {
      for(final RegionOfInterest roi1 : this)
         if(roi1.intersects(roi))
            return true;
      return false;
   }

   @Override
   public int hashCode() {
      if(mHash == Integer.MAX_VALUE) {
         final int PRIME = 31;
         int result = 1;
         long temp;
         temp = Double.doubleToLongBits(mMinIntensity);
         result = (PRIME * result) + (int) (temp ^ (temp >>> 32));
         temp = Double.doubleToLongBits(mHighExtra);
         result = (PRIME * result) + (int) (temp ^ (temp >>> 32));
         temp = Double.doubleToLongBits(mLowExtra);
         result = (PRIME * result) + (int) (temp ^ (temp >>> 32));
         result = (PRIME * result) + ((mModel == null) ? 0 : mModel.hashCode());
         result = (PRIME * result) + ((mROIs == null) ? 0 : mROIs.hashCode());
         mHash = result;
         if(mHash == Integer.MAX_VALUE)
            mHash = Integer.MIN_VALUE;
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
      final RegionOfInterestSet other = (RegionOfInterestSet) obj;
      if(Double.doubleToLongBits(mMinIntensity) != Double.doubleToLongBits(other.mMinIntensity))
         return false;
      if(Double.doubleToLongBits(mHighExtra) != Double.doubleToLongBits(other.mHighExtra))
         return false;
      if(Double.doubleToLongBits(mLowExtra) != Double.doubleToLongBits(other.mLowExtra))
         return false;
      if(!mModel.equals(other.mModel))
         return false;
      if(!mROIs.equals(other.mROIs))
         return false;
      return true;
   }

   /**
    * Does one of the ROIs contain the specified energy?
    * 
    * @param e
    * @return boolean
    */
   public boolean contains(double e) {
      for(final RegionOfInterest roi : mROIs)
         if(roi.contains(e))
            return true;
      return false;
   }

   public boolean contains(RegionOfInterest roi) {
      for(final RegionOfInterest thisroi : this)
         if(thisroi.equals(roi))
            return true;
      return false;
   }

   /**
    * Is this roi fully contained within one of this RegionOfInterestSet's
    * RegionOfInterest objects.
    * 
    * @param roi
    * @return boolean
    */
   public boolean fullyContains(RegionOfInterest roi) {
      for(final RegionOfInterest thisRoi : mROIs)
         if(thisRoi.fullyContains(roi))
            return true;
      return false;
   }

   /**
    * The number of RegionOfInterests defined within this set.
    * 
    * @return int
    */
   public int size() {
      return mROIs.size();
   }

   public boolean isEmpty() {
      return mROIs.isEmpty();
   }

   /**
    * Combine all ROIs that are separated by less an energy dE.
    * 
    * @param dE in Joules
    */
   public void merge(double dE) {
      final Set<RegionOfInterest> rois = new TreeSet<RegionOfInterest>(mROIs);
      for(final RegionOfInterest roi1 : mROIs) {
         RegionOfInterest roi = null;
         for(final RegionOfInterest roi2 : mROIs)
            if(roi == null) {
               if((roi1 == roi2) && rois.contains(roi1)) {
                  roi = roi1;
                  rois.remove(roi1);
               }
            } else if(rois.contains(roi2))
               if(((roi.mMinEnergy < roi2.mMinEnergy) && ((roi.mMaxEnergy + dE) > roi2.mMinEnergy))
                     || ((roi2.mMaxEnergy < roi.mMinEnergy) && ((roi2.mMaxEnergy + dE) > roi.mMinEnergy))) {
                  roi = new RegionOfInterest(roi, roi2);
                  rois.remove(roi2);
               }
         if(roi != null)
            rois.add(roi);
      }
      mROIs.clear();
      mROIs.addAll(rois);
   }

   public XRayTransitionSet getXRayTransitionSet(Element elm) {
      final XRayTransitionSet res = new XRayTransitionSet();
      for(final RegionOfInterest roi : mROIs)
         res.add(roi.getXRayTransitionSet(elm));
      return res;
   }

   @Override
   public int compareTo(RegionOfInterestSet o) {
      int res = 0;
      final Iterator<RegionOfInterest> thisI = mROIs.iterator();
      final Iterator<RegionOfInterest> oI = o.mROIs.iterator();
      while(thisI.hasNext() && oI.hasNext()) {
         res = thisI.next().compareTo(oI.next());
         if(res != 0)
            break;
      }
      if(res == 0) {
         if(thisI.hasNext())
            res = 1;
         if(oI.hasNext())
            res = -1;
      }
      return res;
   }

   /**
    * Returns a list of the elements represented by this RegionOfInterestSet
    * 
    * @return TreeSet&lt;Element&gt;
    */
   TreeSet<Element> getElementSet() {
      final TreeSet<Element> res = new TreeSet<Element>();
      for(final RegionOfInterest roi : mROIs)
         res.addAll(roi.getElementSet());
      return res;
   }

   public boolean containsThisROI(RegionOfInterest roi) {
      for(final RegionOfInterest thisRoi : mROIs)
         if(thisRoi.compareTo(roi) == 0)
            return true;
      return false;

   }

   public double lowEnergy() {
      double min = Double.MAX_VALUE;
      for(final RegionOfInterest roi : mROIs)
         if(roi.lowEnergy() < min)
            min = roi.lowEnergy();
      return min;
   }

   public double highEnergy() {
      double max = -Double.MAX_VALUE;
      for(final RegionOfInterest roi : mROIs)
         if(roi.highEnergy() > max)
            max = roi.highEnergy();
      return max;
   }

}
