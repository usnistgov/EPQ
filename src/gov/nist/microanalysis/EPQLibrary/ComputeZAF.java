/**
 * <p>
 * Computes the ZAF correction for an unknown material relative to a set of
 * standard materials. This algorithm caches the standard correction and
 * calculates the weighted correction as appropriate for EDS measurements.
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
package gov.nist.microanalysis.EPQLibrary;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import gov.nist.microanalysis.Utility.UncertainValue2;

/**
 * Computes the ZAF correction for a measurement. The initialize(...) method
 * precomputes the emitted intensity for the standard material and the specified
 * x-ray transition set. The compute method computes the ZAF correction for the
 * unknown. Presumably the standard material changes infrequently and the unknow
 * material changes with each iteration of the iteration algorithm.
 */
@Deprecated
public class ComputeZAF
   extends AlgorithmClass {

   protected double mMinWeight;
   protected Map<XRayTransitionSet, Map<XRayTransition, Double>> mDominant = new TreeMap<XRayTransitionSet, Map<XRayTransition, Double>>();
   protected Map<XRayTransitionSet, SpectrumProperties> mStdProperties = new TreeMap<XRayTransitionSet, SpectrumProperties>();
   protected CorrectionAlgorithm mCorrectionAlgorithm;
   private final XPP1991 mXPP = CorrectionAlgorithm.XPP;

   /**
    * Constructs a ComputeZAF AlgorithmClass derived object. Note: The beam
    * energy and take-off angle are assumed to be the same for standard and
    * unknown.
    */
   public ComputeZAF() {
      super("ZAF engine", "Default", "Default");
      mMinWeight = 0.1; // default value
   }

   /**
    * Add a standard material for the specified XRayTransitionSet.
    * 
    * @param xrts - An XRayTransition set for which to define a standard
    * @param stdComp - The composition of the standard
    * @param stdProps SpectrumProperties for the standard containing at least
    *           SpectrumProperties.BeamEnergy and
    *           SpectrumProperties.TakeOffAngle. These properties must match
    *           those of the unknown.
    * @throws EPQException
    */
   public void addStandard(XRayTransitionSet xrts, Composition stdComp, SpectrumProperties stdProps)
         throws EPQException {
      assert stdProps != null;
      assert (stdProps.isDefined(SpectrumProperties.BeamEnergy));
      assert stdProps.getSampleShapeWithDefault(SpectrumProperties.SampleShape, new SampleShape.Bulk()) instanceof SampleShape.Bulk;
      stdProps.setCompositionProperty(SpectrumProperties.StandardComposition, stdComp);
      mStdProperties.put(xrts, stdProps);
      final CorrectionAlgorithm ca = (CorrectionAlgorithm) getAlgorithm(CorrectionAlgorithm.class);
      final Map<XRayTransition, Double> stdRes = new TreeMap<XRayTransition, Double>();
      final double norm = xrts.getWeighiestTransition().getWeight(XRayTransition.NormalizeFamily);
      for(final XRayTransition xrt : xrts) {
         final double w = xrt.getWeight(XRayTransition.NormalizeFamily);
         if((w / norm) >= mMinWeight) {
            final AtomicShell shell = xrt.getDestination();
            ca.initialize(stdComp, shell, stdProps);
            try {
               stdRes.put(xrt, new Double(ca.computeZAFCorrection(xrt) * stdComp.weightFraction(xrt.getElement(), false)));
            }
            catch(final EPQException e) {
               e.printStackTrace();
            }
         }
      }
      mDominant.put(xrts, stdRes);
   }

   /**
    * Compute the corrections for the specified material and the specified set
    * of x-ray transitions based on the standard specified in
    * addStandard(xrts,std,stdProps) where xrts is the same as this function.
    * 
    * @param xrts The set of x-ray transitions for which to compute the ZAF
    *           correction.
    * @param unkComp The composition of the material
    * @param unkProps The SpectrumProperties for the material containing at
    *           least SpectrumProperties.BeamEnergy and
    *           SpectrumProperties.TakeOffAngle. These properties must match
    *           those in addStandad(...)
    * @return Returns ZAF[unk]/(C[std]*ZAF[std])
    */
   public double compute(XRayTransitionSet xrts, Composition unkComp, SpectrumProperties unkProps) {
      assert (unkProps.isDefined(SpectrumProperties.BeamEnergy));
      mCorrectionAlgorithm = (CorrectionAlgorithm) getAlgorithm(CorrectionAlgorithm.class);
      final SampleShape ss = unkProps.getSampleShapeWithDefault(SpectrumProperties.SampleShape, null);
      assert (ss == null) || mCorrectionAlgorithm.supports(ss.getClass());
      double sum = 0.0, sumW = 0.0;
      final Map<XRayTransition, Double> stdRes = mDominant.get(xrts);
      if(stdRes == null)
         throw new EPQFatalException("No standard material has been associated with this set of x-ray transitions.");
      final SpectrumProperties stdProps = mStdProperties.get(xrts);
      if(Math.abs(stdProps.getNumericWithDefault(SpectrumProperties.BeamEnergy, -1.0)
            - unkProps.getNumericWithDefault(SpectrumProperties.BeamEnergy, -2.0)) > 0.01)
         throw new EPQFatalException("The beam energy for the standard and unknown must match.");
      if(Math.abs(SpectrumUtils.getTakeOffAngle(stdProps) - SpectrumUtils.getTakeOffAngle(unkProps)) > Math.toRadians(2.0))
         throw new EPQFatalException("The take-off angle for the standard and unknown must match to within a degree.");
      final double norm = xrts.getWeighiestTransition().getWeight(XRayTransition.NormalizeFamily);
      for(final Map.Entry<XRayTransition, Double> me : stdRes.entrySet()) {
         final XRayTransition xrt = me.getKey();
         final Double std = me.getValue();
         final double w = xrt.getWeight(XRayTransition.NormalizeFamily);
         if((w / norm) >= mMinWeight) {
            final AtomicShell shell = xrt.getDestination();
            if(unkComp.containsElement(shell.getElement()))
               try {
                  mCorrectionAlgorithm.initialize(unkComp, shell, unkProps);
                  sum += (w * mCorrectionAlgorithm.computeZAFCorrection(xrt)) / std.doubleValue();
                  sumW += w;
               }
               catch(final EPQException e) {
                  e.printStackTrace();
               }
         }
      }
      // Return ZAF[unk]/(C[std]*ZAF[std])
      return sum / sumW;
   }

   /**
    * @param xrts The set of x-ray transitions for which to compute the ZAF
    *           correction.
    * @param unkComp The composition of the material
    * @param unkProps The SpectrumProperties for the material containing at
    *           least SpectrumProperties.BeamEnergy and
    *           SpectrumProperties.TakeOffAngle. These properties must match
    *           those in addStandad(...)
    * @return Returns ZAF[unk]/(C[std]*ZAF[std])
    */
   public UncertainValue2 computeU(XRayTransitionSet xrts, Composition unkComp, SpectrumProperties unkProps) {
      assert (unkProps.isDefined(SpectrumProperties.BeamEnergy));
      mCorrectionAlgorithm = (CorrectionAlgorithm) getAlgorithm(CorrectionAlgorithm.class);
      final SampleShape ss = unkProps.getSampleShapeWithDefault(SpectrumProperties.SampleShape, null);
      assert (ss == null) || mCorrectionAlgorithm.supports(ss.getClass());
      double sumW = 0.0;
      UncertainValue2 sum = UncertainValue2.ZERO;
      // final TransitionData stdData = mStandardData.get(xrts);
      // final SpectrumProperties stdProps = stdData.mProperties;
      final SpectrumProperties stdProps = mStdProperties.get(xrts);
      if(Math.abs(stdProps.getNumericWithDefault(SpectrumProperties.BeamEnergy, -1.0)
            - unkProps.getNumericWithDefault(SpectrumProperties.BeamEnergy, -2.0)) > 0.01)
         throw new EPQFatalException("The beam energy for the standard and unknown must match.");
      if(Math.abs(SpectrumUtils.getTakeOffAngle(stdProps) - SpectrumUtils.getTakeOffAngle(unkProps)) > Math.toRadians(1.0))
         throw new EPQFatalException("The take-off angle for the standard and unknown must match to within a degree.");
      final Element elm = xrts.getElement();
      final double cUnk = unkComp.weightFraction(elm, false);
      final Composition stdComp = stdProps.getCompositionWithDefault(SpectrumProperties.StandardComposition, null);
      if(cUnk > 0.0) {
         final double norm = xrts.getWeighiestTransition().getWeight(XRayTransition.NormalizeFamily);
         for(XRayTransition xrt : xrts) {
            final double w = xrt.getWeight(XRayTransition.NormalizeFamily);
            if((w / norm) >= mMinWeight) {
               try {
                  final UncertainValue2 k = mXPP.kratio(stdComp, unkComp, xrt, unkProps);
                  // k = (ZAFunk Cunk)/(ZAFstd Cstd)
                  sum = UncertainValue2.add(sum, UncertainValue2.multiply(w, UncertainValue2.divide(k, cUnk)));
                  // sum+= ZAFunk/(ZAFstd Cstd)
                  sumW += w;
               }
               catch(final EPQException e) {
                  e.printStackTrace();
               }
            }
         }
         final UncertainValue2 res = UncertainValue2.divide(sum, sumW);
         assert Math.abs(res.doubleValue() - compute(xrts, unkComp, unkProps)) < 0.001;
         return res;
      } else
         return UncertainValue2.invert(stdComp.weightFractionU(elm, false));
   }

   /**
    * getAllImplementations
    * 
    * @return List
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#getAllImplementations()
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return null;
   }

   public void setMinWeight(double mw) {
      mMinWeight = mw;
   }

   public double getMinWeight() {
      return mMinWeight;
   }

   /**
    * initializeDefaultStrategy
    * 
    * @see gov.nist.microanalysis.EPQLibrary.AlgorithmClass#initializeDefaultStrategy()
    */
   @Override
   protected void initializeDefaultStrategy() {
      addDefaultAlgorithm(CorrectionAlgorithm.class, CorrectionAlgorithm.XPPExtended);
   }

   /**
    * Returns the CorrectionAlgorithm used to perform the correction in the last
    * call to <code>compute(...)</code>. Null if <code>compute(...)</code> has
    * not been called.
    * 
    * @return CorrectionAlgorithm;
    */
   public CorrectionAlgorithm getCorrectionAlgorithm() {
      return mCorrectionAlgorithm;
   }

}
