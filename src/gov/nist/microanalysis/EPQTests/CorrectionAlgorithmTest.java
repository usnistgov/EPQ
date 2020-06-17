package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.AlgorithmUser;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.CompositionFromKRatios;
import gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Fluorescence;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.IterationAlgorithm;
import gov.nist.microanalysis.EPQLibrary.KRatioSet;
import gov.nist.microanalysis.EPQLibrary.MassAbsorptionCoefficient;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.PandPDatabase;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.Strategy;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQLibrary.XRayTransitionSet;
import gov.nist.microanalysis.Utility.DescriptiveStatistics;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * <p>
 * Tests the CorrectionAlgorithm implementations against the Pouchou and Pichoir
 * database published in the "green book" - Electron Probe Quantitation.
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
public class CorrectionAlgorithmTest
   extends TestCase {

   PandPDatabase mPapd;

   /**
    * Constructs a CorrectionAlgorithmTest
    * 
    * @param arg0
    */
   public CorrectionAlgorithmTest(String arg0) {
      super(arg0);
   }

   protected PandPDatabase getDatabase()
         throws IOException {
      if(mPapd == null)
         mPapd = new PandPDatabase();
      return mPapd;
   }

   private DescriptiveStatistics helper(CorrectionAlgorithm ca, MassAbsorptionCoefficient mac)
         throws IOException {
      final Strategy strat = new Strategy();
      strat.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.WegsteinIteration);
      strat.addAlgorithm(MassAbsorptionCoefficient.class, mac);
      strat.addAlgorithm(CorrectionAlgorithm.class, ca);
      strat.addAlgorithm(Fluorescence.class, Fluorescence.Reed);
      AlgorithmUser.applyGlobalOverride(strat);
      final PandPDatabase papd = getDatabase();
      final DescriptiveStatistics ds = new DescriptiveStatistics();
      for(int ii = 0; ii < papd.getSize(); ++ii) {
         final Material mat = papd.createMaterial(ii);
         final XRayTransition xrt = papd.transition(ii);
         final SpectrumProperties unkProp = new SpectrumProperties();
         unkProp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(papd.beamEnergy(ii)));
         unkProp.setNumericProperty(SpectrumProperties.TakeOffAngle, Math.toDegrees(papd.takeOffAngle(ii)));
         try {
            ds.add((mat.weightFraction(xrt.getElement(), true) * ca.relativeZAF(mat, xrt, unkProp)[3]) / papd.kRatio(ii));
         }
         catch(final EPQException e) {
            e.printStackTrace();
         }
      }
      return ds;
   }

   public void testPouchouAndPichoir()
         throws Exception {

      final Strategy strat = new Strategy();
      strat.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.WegsteinIteration);
      strat.addAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Pouchou1991);
      strat.addAlgorithm(CorrectionAlgorithm.class, CorrectionAlgorithm.PouchouAndPichoir);
      strat.addAlgorithm(Fluorescence.class, Fluorescence.Reed);
      AlgorithmUser.applyGlobalOverride(strat);

      final DescriptiveStatistics ds = new DescriptiveStatistics();
      final PandPDatabase papd = getDatabase();
      for(int ii = 0; ii < papd.getSize(); ++ii) {
         final Material mat = papd.createMaterial(ii);
         try {
            final XRayTransitionSet xrts = new XRayTransitionSet(papd.transition(ii));

            final SpectrumProperties unkProp = new SpectrumProperties();
            unkProp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(papd.beamEnergy(ii)));
            unkProp.setNumericProperty(SpectrumProperties.TakeOffAngle, Math.toDegrees(papd.takeOffAngle(ii)));

            final KRatioSet krs = new KRatioSet();
            krs.addKRatio(xrts, papd.kRatio(ii));

            final CompositionFromKRatios cfk = new CompositionFromKRatios();
            cfk.addStandard(xrts, papd.createStandard(ii), unkProp);
            cfk.addUnmeasuredElementRule(new CompositionFromKRatios.ElementByDifference(papd.elementB(ii)));

            final Composition res = cfk.compute(krs, unkProp);
            final double r = res.weightFraction(xrts.getElement(), false) / mat.weightFraction(xrts.getElement(), false);
            ds.add(r);
         }
         catch(final Exception ex) {
            System.out.println(ex.getMessage());
            System.out.println(Integer.toString(ii) + ": " + mat.toString() + "failed");
         }
      }
      // The results quoted in the green book for this test were 0.9982 +- 1.91%
      assertEquals(1.0006, ds.average(), 0.0001);
      assertEquals(0.0192, ds.standardDeviation(), 0.0001);
   }

   public void testPouchouAndPichoir2()
         throws Exception {
      final DescriptiveStatistics ds = helper(CorrectionAlgorithm.PouchouAndPichoir, MassAbsorptionCoefficient.Pouchou1991);
      assertEquals(1.0000, ds.average(), 0.001);
      assertEquals(0.00, ds.standardDeviation(), 0.03);
   }

   public void testXPP()
         throws Exception {

      final Strategy strat = new Strategy();
      strat.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.WegsteinIteration);
      strat.addAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Pouchou1991);
      strat.addAlgorithm(CorrectionAlgorithm.class, CorrectionAlgorithm.XPP);
      strat.addAlgorithm(Fluorescence.class, Fluorescence.Reed);
      AlgorithmUser.applyGlobalOverride(strat);

      final DescriptiveStatistics ds = new DescriptiveStatistics();
      final PandPDatabase papd = getDatabase();
      for(int ii = 0; ii < papd.getSize(); ++ii)
         try {
            final Material mat = papd.createMaterial(ii);
            final XRayTransitionSet xrts = new XRayTransitionSet(papd.transition(ii));

            final SpectrumProperties unkProp = new SpectrumProperties();
            unkProp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(papd.beamEnergy(ii)));
            unkProp.setNumericProperty(SpectrumProperties.TakeOffAngle, Math.toDegrees(papd.takeOffAngle(ii)));

            final KRatioSet krs = new KRatioSet();
            krs.addKRatio(xrts, papd.kRatio(ii));

            final CompositionFromKRatios cfk = new CompositionFromKRatios();
            cfk.addStandard(xrts, papd.createStandard(ii), unkProp);
            cfk.addUnmeasuredElementRule(new CompositionFromKRatios.ElementByDifference(papd.elementB(ii)));

            final Composition res = cfk.compute(krs, unkProp);
            final double r = res.weightFraction(xrts.getElement(), false) / mat.weightFraction(xrts.getElement(), false);
            ds.add(r);
         }
         catch(final Exception ex) {
         }
      // The results quoted in the green book for this test were 0.9997 +- 1.79%
      assertEquals(ds.average(), 0.9997, 0.0001);
      assertEquals(ds.standardDeviation(), 0.01865, 0.0001);
   }

   public void testXPP2()
         throws Exception {
      final DescriptiveStatistics ds = helper(CorrectionAlgorithm.XPP, MassAbsorptionCoefficient.Pouchou1991);
      assertEquals(1.0000, ds.average(), 0.002);
      assertEquals(0.00, ds.standardDeviation(), 0.03);
   }

   public void testXPPExtended()
         throws Exception {
      final Strategy strat = new Strategy();
      strat.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.WegsteinIteration);
      strat.addAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Pouchou1991);
      strat.addAlgorithm(CorrectionAlgorithm.class, CorrectionAlgorithm.XPPExtended);
      strat.addAlgorithm(Fluorescence.class, Fluorescence.Reed);
      AlgorithmUser.applyGlobalOverride(strat);

      final DescriptiveStatistics ds = new DescriptiveStatistics();
      final PandPDatabase papd = getDatabase();
      for(int ii = 0; ii < papd.getSize(); ++ii)
         try {
            final Material mat = papd.createMaterial(ii);
            final XRayTransitionSet xrts = new XRayTransitionSet(papd.transition(ii));

            final SpectrumProperties unkProp = new SpectrumProperties();
            unkProp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(papd.beamEnergy(ii)));
            unkProp.setNumericProperty(SpectrumProperties.TakeOffAngle, Math.toDegrees(papd.takeOffAngle(ii)));

            final KRatioSet krs = new KRatioSet();
            krs.addKRatio(xrts, papd.kRatio(ii));

            final CompositionFromKRatios cfk = new CompositionFromKRatios();
            cfk.addStandard(xrts, papd.createStandard(ii), unkProp);
            cfk.addUnmeasuredElementRule(new CompositionFromKRatios.ElementByDifference(papd.elementB(ii)));

            final Composition res = cfk.compute(krs, unkProp);
            final double r = res.weightFraction(xrts.getElement(), false) / mat.weightFraction(xrts.getElement(), false);
            ds.add(r);
         }
         catch(final Exception ex) {
         }
      assertEquals(ds.average(), 0.99974, 0.0001);
      assertEquals(ds.standardDeviation(), 0.01865, 0.0001);
   }

   public void testXPPExtended2()
         throws Exception {
      final DescriptiveStatistics ds = helper(CorrectionAlgorithm.XPPExtended, MassAbsorptionCoefficient.Pouchou1991);
      assertEquals(1.0000, ds.average(), 0.002);
      assertEquals(0.00, ds.standardDeviation(), 0.03);
   }

   
   public void testJTA1982Test()
         throws Exception {

      final Strategy strat = new Strategy();
      strat.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.WegsteinIteration);
      strat.addAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Pouchou1991);
      strat.addAlgorithm(CorrectionAlgorithm.class, CorrectionAlgorithm.Armstrong1982);
      strat.addAlgorithm(Fluorescence.class, Fluorescence.Reed);
      AlgorithmUser.applyGlobalOverride(strat);

      final DescriptiveStatistics ds = new DescriptiveStatistics();
      final PandPDatabase papd = getDatabase();
      for(int ii = 0; ii < papd.getSize(); ++ii)
         try {
            final Material mat = papd.createMaterial(ii);
            final XRayTransitionSet xrts = new XRayTransitionSet(papd.transition(ii));

            final SpectrumProperties unkProp = new SpectrumProperties();
            unkProp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(papd.beamEnergy(ii)));
            unkProp.setNumericProperty(SpectrumProperties.TakeOffAngle, Math.toDegrees(papd.takeOffAngle(ii)));

            final KRatioSet krs = new KRatioSet();
            krs.addKRatio(xrts, papd.kRatio(ii));

            final CompositionFromKRatios cfk = new CompositionFromKRatios();
            cfk.addStandard(xrts, papd.createStandard(ii), unkProp);
            cfk.addUnmeasuredElementRule(new CompositionFromKRatios.ElementByDifference(papd.elementB(ii)));

            final Composition res = cfk.compute(krs, unkProp);
            final double r = res.weightFraction(xrts.getElement(), false) / mat.weightFraction(xrts.getElement(), false);
            ds.add(r);
         }
         catch(final Exception ex) {
         }
      // System.out.println(ds.average());
      // System.out.println(ds.standardDeviation());
      // assertEquals(ds.average(), 0.9944, 0.0001);
      // assertEquals(ds.standardDeviation(), 0.0688, 0.0001);
   }
}
