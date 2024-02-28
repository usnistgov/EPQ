package gov.nist.microanalysis.EPQLibrary;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.Detector.DetectorLineshapeModel;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.Utility.LinearLeastSquares;

abstract public class LinearSpectrumFit extends LinearLeastSquares {

   protected final EDSDetector mDetector;
   /**
    * Beam energy in Joules
    */
   protected final double mBeamEnergy;

   protected static double SCALE_TOLERANCE = 0.01;
   protected static final double MIN_INTENSITY =  0.9999e-3;
   protected static final double EXTRA_LOW =  0.6;
   protected static final double EXTRA_HIGH =  0.6;
   

   public LinearSpectrumFit(EDSDetector det, double beamEnergy) {
      mDetector = det;
      mBeamEnergy = beamEnergy;
   }

   /**
    * Computes a XRayTransitionSet corresponding to the lines that should be
    * visible in this reference spectrum.
    * 
    * @param elm
    *           An element for which this spectrum is a reference
    * @param ref
    * @return XRayTransitionSet
    */

   protected XRayTransitionSet createXRayTransitionSet(Element elm, ISpectrumData ref) {
      assert elm != Element.None;
      final XRayTransitionSet xrts = new XRayTransitionSet();
      final double e0 = mBeamEnergy;
      final double eMin = ToSI.eV(SpectrumUtils.minEnergyForChannel(ref, SpectrumUtils.smallestNonZeroChannel(ref)));
      // assert eMin<ToSI.keV(1.0);
      final double ffMax = ToSI.eV(SpectrumUtils.maxEnergyForChannel(ref, ref.getChannelCount()));
      XRayTransitionSet tmpXrts = null;
      // Add a FilteredSpectrum for each visible family
      for (int f = AtomicShell.KFamily; f <= AtomicShell.MFamily; f++) {
         double ee = Double.MAX_VALUE;
         switch (f) {
            case AtomicShell.KFamily :
               if (!mDetector.isVisible(new XRayTransition(elm, XRayTransition.KA1), e0))
                  continue;
               ee = AtomicShell.getEdgeEnergy(elm, AtomicShell.K);
               tmpXrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY, 0.0101);
               break;
            case AtomicShell.LFamily :
               if (!mDetector.isVisible(new XRayTransition(elm, XRayTransition.LA1), e0))
                  continue;
               ee = AtomicShell.getEdgeEnergy(elm, AtomicShell.LIII);
               tmpXrts = new XRayTransitionSet(elm, XRayTransitionSet.L_FAMILY, 0.0101);
               break;
            case AtomicShell.MFamily :
               if (!mDetector.isVisible(new XRayTransition(elm, XRayTransition.MA1), e0))
                  continue;
               ee = AtomicShell.getEdgeEnergy(elm, AtomicShell.MV);
               tmpXrts = new XRayTransitionSet(elm, XRayTransitionSet.M_FAMILY, 0.0101);
               break;
         }
         assert (tmpXrts != null);
         if ((ee > eMin) && (ee < e0) && (ee < ffMax) && (XRayTransition.lineWithLowestEnergy(elm, f) != XRayTransition.None))
            xrts.add(tmpXrts);
      }
      return xrts;
   }

   public static RegionOfInterestSet createEmptyROI(EDSDetector det) {
      final DetectorLineshapeModel dlm = det.getDetectorLineshapeModel();
      final double xtra = ToSI.eV(dlm.getFWHMatMnKa());
      return new RegionOfInterestSet(dlm, LinearSpectrumFit.MIN_INTENSITY, EXTRA_LOW * xtra, EXTRA_HIGH * xtra);
   }

   /**
    * Create the ROIs associated with the specified element.
    * 
    * @param elm
    * @param det
    * @param beamEnergy
    *           in Joule
    * @return RegionOfInterestSet
    */
   static public RegionOfInterestSet createElementROIS(Element elm, EDSDetector det, double beamEnergy) {
      final RegionOfInterestSet tmpRois = LinearSpectrumFit.createEmptyROI(det);
      tmpRois.add(det.getVisibleTransitions(elm, beamEnergy));
      return tmpRois;
   }

   /**
    * Create the ROIs associated with all the elements a material with the
    * specified Composition.
    * 
    * @param comp
    * @param det
    * @param beamEnergy
    *           in Joules
    * @return RegionOfInterestSet
    */
   static public RegionOfInterestSet createAllElementROIS(Composition comp, EDSDetector det, double beamEnergy) {
      return createAllElementROIS(comp.getElementSet(), det, beamEnergy);
   }

   /**
    * Create the ROIs associated with all the elements a material with the
    * specified Composition.
    * 
    * @param elms
    * @param det
    * @param beamEnergy
    *           in Joules
    * @return RegionOfInterestSet
    */
   static public RegionOfInterestSet createAllElementROIS(Collection<Element> elms, EDSDetector det, double beamEnergy) {
      final RegionOfInterestSet tmpRois = LinearSpectrumFit.createEmptyROI(det);
      final Set<Element> selm = new TreeSet<Element>(elms);
      for (final Element elm : selm)
         tmpRois.add(det.getVisibleTransitions(elm, beamEnergy));
      return tmpRois;
   }

   abstract public KRatioSet getKRatios(ISpectrumData spec) throws EPQException;

   abstract public Set<Element> getElements();

}
