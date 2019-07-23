package gov.nist.microanalysis.EPQLibrary;

import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.EPQLibrary.StandardsDatabase2.StandardBlock2;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.Utility.TextUtilities;
import gov.nist.microanalysis.Utility.UncertainValue2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * <p>
 * Optimizes the experiment based on the calculated uncertainty in the
 * composition.
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
public class CompositionOptimizer
   extends EPMAOptimizer {

   class CompositionOptimizedStandard
      extends OptimizedStandard {

      private final UncertainValue2 mComposition;
      private final RegionOfInterest mFullROI;

      public CompositionOptimizedStandard(Composition std, RegionOfInterest region, SpectrumProperties sp)
            throws EPQException {
         super(std, region, 0.0, sp);
         assert region.getElementSet().size() == 1;
         final Element elm = region.getElementSet().first();
         final XRayTransition xrt = region.getXRayTransitionSet(elm).getWeighiestTransition();
         mComposition = XPP1991.massFraction(std, mEstimatedUnknown, xrt, sp);
         mScore = 100.0 * (1.0 - mComposition.fractionalUncertainty());
         final RegionOfInterestSet rois = new RegionOfInterestSet(region);
         final double e0 = ToSI.keV(sp.getNumericWithDefault(SpectrumProperties.BeamEnergy, 20.0));
         for(final Element elm2 : std.getElementSet())
            if(!elm2.equals(elm))
               rois.add(elm2, e0, 0.0001);
         RegionOfInterest fullRoi = null;
         for(final RegionOfInterest roi : rois)
            if(roi.intersects(region)) {
               fullRoi = roi;
               break;
            }
         mFullROI = fullRoi;
      }

      @Override
      public int compareTo(OptimizedStandard o) {
         int res = -Double.compare(mScore, o.mScore);
         if(res == 0)
            res = mComposition.compareTo(((CompositionOptimizedStandard) o).mComposition);
         if(res == 0)
            res = super.compareTo(o);
         return res;
      }

      public RegionOfInterest getFullROI() {
         return mFullROI;
      }

   }

   private double mNominalIntegral = 1.00e6; // Number of counts in a nominal
   private final StandardsDatabase2 mDatabase;
   private final TreeSet<StandardBlock2> mExclude;

   public CompositionOptimizer(EDSDetector det, Composition estComp, StandardsDatabase2 sdb) {
      super(det, estComp);
      mDatabase = sdb;
      mExclude = new TreeSet<StandardBlock2>();
   }

   public void setExclusionList(Collection<StandardBlock2> exclude) {
      mExclude.clear();
      mExclude.addAll(exclude);
   }

   public void setNominalIntegral(double i) {
      mNominalIntegral = i;
   }

   public double getNominalIntegral() {
      return mNominalIntegral;
   }

   /**
    * @param elm
    * @param sp
    * @return List&lt;OptimizedStandard&gt;
    * @throws EPQException
    * @see gov.nist.microanalysis.EPQLibrary.EPMAOptimizer#getOptimizedStandards(gov.nist.microanalysis.EPQLibrary.Element,
    *      gov.nist.microanalysis.EPQLibrary.SpectrumProperties)
    */
   @Override
   public List<OptimizedStandard> getOptimizedStandards(Element elm, SpectrumProperties sp)
         throws EPQException {
      final ArrayList<OptimizedStandard> res = new ArrayList<EPMAOptimizer.OptimizedStandard>();
      final List<Composition> comps = mDatabase.findStandards(elm, 0.01, mExclude);
      final double e0 = ToSI.keV(sp.getNumericProperty(SpectrumProperties.BeamEnergy));
      final RegionOfInterestSet rois = LinearSpectrumFit.createElementROIS(elm, mDetector, e0);
      for(final Composition comp : comps)
         for(final RegionOfInterest roi : rois)
            res.add(new CompositionOptimizedStandard(comp, roi, sp));
      Collections.sort(res);
      return res;
   }

   private class RefComparitor
      implements Comparator<Composition> {
      private final XRayTransition mTransition;
      private final SpectrumProperties mProperties;
      private final TreeMap<Composition, Double> mKRatios = new TreeMap<Composition, Double>();
      private final XPP1991 mXPP = new XPP1991();

      public RefComparitor(XRayTransition xrt, SpectrumProperties sp) {
         mTransition = xrt;
         mProperties = sp;
      }

      @Override
      public int compare(Composition o1, Composition o2) {
         double kr1;
         try {
            kr1 = mKRatios.containsKey(o1) ? mKRatios.get(o1) : mXPP.computeKRatio(o1, mTransition, mProperties);
         }
         catch(final EPQException e) {
            kr1 = 1.0e-10;
         }
         mKRatios.put(o1, kr1);
         double kr2;
         try {
            kr2 = mKRatios.containsKey(o2) ? mKRatios.get(o2) : mXPP.computeKRatio(o2, mTransition, mProperties);
         }
         catch(final EPQException e) {
            kr2 = 1.0e-10;
         }
         mKRatios.put(o2, kr2);
         int res = -Double.compare(kr1, kr2);
         if(res == 0)
            res = o1.compareTo(o2);
         return res;
      }
   }

   /**
    * @param roi
    * @return ArrayList&lt;Composition&gt;
    * @throws EPQException
    * @see gov.nist.microanalysis.EPQLibrary.EPMAOptimizer#suggestReferences(gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest)
    */
   @Override
   public ArrayList<Composition> suggestReferences(RegionOfInterest roi)
         throws EPQException {
      if(roi.getElementSet().size() > 1)
         throw new EPQException("suggestReferences(..) requires a single element ROI. Yours = "
               + roi.getElementSet().toString());
      assert roi.getElementSet().size() == 1;
      final Element elm = roi.getElementSet().first();
      final List<Composition> comps = mDatabase.findStandards(elm, 0.1, mExclude);
      final double e0 = mDetector.getOwner().getMaxBeamEnergy();
      final ArrayList<Composition> res = new ArrayList<Composition>();
      for(final Composition comp : comps) {
         final RegionOfInterestSet allElmRois = LinearSpectrumFit.createAllElementROIS(comp, mDetector, e0);
         for(final RegionOfInterest allElmRoi : allElmRois)
            if(allElmRoi.equals(roi) && (allElmRoi.getElementSet().size() == 1))
               res.add(comp);
      }
      final SpectrumProperties sp = new SpectrumProperties();
      sp.setDetector(mDetector);
      sp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(e0));
      Collections.sort(res, new RefComparitor(roi.getXRayTransitionSet(elm).getWeighiestTransition(), sp));
      return res;
   }

   @Override
   public String toHTML() {
      final StringBuffer sb = new StringBuffer(super.toHTML());
      final List<Composition> comps = getAllMaterials();
      final List<StandardBlock2> blocks = mDatabase.suggestBlocks(comps, mExclude);
      sb.append("<h3>Standard Blocks</h3>");
      sb.append("<table>");
      sb.append("<tr><th>Block</th><th>Contains materials</th></tr>");
      for(final StandardBlock2 block : blocks) {
         sb.append("<tr><td>");
         sb.append(block.toString());
         sb.append("</td><td>");
         sb.append(TextUtilities.toList(block.satisfies(comps)));
         sb.append("</td></tr>");
      }
      sb.append("</table>");
      return sb.toString();
   }
}
