package gov.nist.microanalysis.EPQTests;

import junit.framework.TestCase;

import gov.nist.microanalysis.EPQLibrary.AlgorithmUser;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.ComputeZAF;
import gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.IterationAlgorithm;
import gov.nist.microanalysis.EPQLibrary.KRatioSet;
import gov.nist.microanalysis.EPQLibrary.CompositionFromKRatios;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.Strategy;
import gov.nist.microanalysis.EPQLibrary.XRayTransitionSet;

/**
 * <p>
 * Tests the ComputeZAFCorrection class.
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
@SuppressWarnings("deprecation")
public class CompositionFromKRatiosTest
   extends TestCase {

   /**
    * Constructs a ComputeZAFCorrectionTest
    * 
    * @param arg0
    */
   public CompositionFromKRatiosTest(String arg0) {
      super(arg0);
   }

   public void testOne()
         throws EPQException {
      final Strategy strategy = new Strategy();
      strategy.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.SimpleIteration);
      AlgorithmUser.applyGlobalOverride(strategy);

      final Composition mat = MaterialFactory.createCompound("FeO2");
      final KRatioSet result = new KRatioSet();

      // Use the same SpectrumProperties for references and unknown
      final SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

      final ComputeZAF cz = new ComputeZAF();
      final CompositionFromKRatios czc = new CompositionFromKRatios();
      czc.setConvergenceCriterion(1.0e-4);

      for(final Element el : mat.getElementSet()) {
         final Composition ref = el.equals(Element.O) ? MaterialFactory.createMaterial(MaterialFactory.MagnesiumOxide)
               : MaterialFactory.createPureElement(el);
         final XRayTransitionSet xrts = new XRayTransitionSet(el, XRayTransitionSet.K_FAMILY);
         cz.addStandard(xrts, ref, props);
         result.addKRatio(xrts, mat.weightFraction(el, true) * cz.compute(xrts, mat, props), 0.0);
         czc.addStandard(xrts, ref, props);
      }
      System.out.println(result);

      final Composition redux = czc.compute(result, props);
      System.out.println("Original: " + mat.weightPercentString(true));
      System.out.println("Result  : " + redux.weightPercentString(false));
      System.out.println("Iteration count = " + Integer.toString(czc.getIterationCount()));
      assertEquals(redux.difference(mat), 0.0, 1.0e-4);
   }

   public void testTwo()
         throws EPQException {
      final Strategy strategy = new Strategy();
      strategy.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.WegsteinIteration);
      AlgorithmUser.applyGlobalOverride(strategy);

      final Composition mat = MaterialFactory.createCompound("FeO2");

      final KRatioSet result = new KRatioSet();

      // Use the same SpectrumProperties for references and unknown
      final SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

      final ComputeZAF cz = new ComputeZAF();
      final CompositionFromKRatios czc = new CompositionFromKRatios();
      czc.setConvergenceCriterion(1.0e-4);

      for(final Element el : mat.getElementSet()) {
         final Composition ref = el.equals(Element.O) ? MaterialFactory.createMaterial(MaterialFactory.MagnesiumOxide)
               : MaterialFactory.createPureElement(el);
         final XRayTransitionSet xrts = new XRayTransitionSet(el, XRayTransitionSet.K_FAMILY);
         cz.addStandard(xrts, ref, props);
         result.addKRatio(xrts, mat.weightFraction(el, true) * cz.compute(xrts, mat, props), 0.0);
         czc.addStandard(xrts, ref, props);
      }
      System.out.println(result);

      final Composition redux = czc.compute(result, props);
      System.out.println("Original: " + mat.weightPercentString(true));
      System.out.print("Result:   ");
      System.out.println(redux.weightPercentString(false));
      System.out.println("Iteration count = " + Integer.toString(czc.getIterationCount()));
      assertEquals(redux.difference(mat), 0.0, 1.0e-4);
   }

   public void testThree()
         throws EPQException {
      AlgorithmUser.clearGlobalOverride();
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);

      final KRatioSet result = new KRatioSet();

      // Use the same SpectrumProperties for references and unknown
      final SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

      final ComputeZAF cz = new ComputeZAF();
      final CompositionFromKRatios czc = new CompositionFromKRatios();
      czc.setConvergenceCriterion(1.0e-4);

      for(final Element el : mat.getElementSet()) {
         final Composition ref = el.equals(Element.O) ? MaterialFactory.createMaterial(MaterialFactory.MagnesiumOxide)
               : MaterialFactory.createPureElement(el);
         final XRayTransitionSet xrts = new XRayTransitionSet(el, XRayTransitionSet.K_FAMILY);
         cz.addStandard(xrts, ref, props);
         result.addKRatio(xrts, mat.weightFraction(el, true) * cz.compute(xrts, mat, props), 0.0);
         czc.addStandard(xrts, ref, props);
      }
      System.out.println(result);

      final Composition redux = czc.compute(result, props);
      System.out.print("Original: ");
      System.out.println(mat.weightPercentString(false));
      System.out.print("Result: ");
      System.out.println(redux.weightPercentString(false));
      System.out.println("Iteration count = " + Integer.toString(czc.getIterationCount()));
      assertEquals(redux.difference(mat), 0.0, 1.0e-4);
   }

   public void testSimple()
         throws EPQException {
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);

      final KRatioSet result = new KRatioSet();

      // Use the same SpectrumProperties for references and unknown
      final SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

      final ComputeZAF cz = new ComputeZAF();
      final CompositionFromKRatios czc = new CompositionFromKRatios();
      czc.setConvergenceCriterion(1.0e-4);
      for(final Element el : mat.getElementSet()) {
         final Composition ref = el.equals(Element.O) ? MaterialFactory.createMaterial(MaterialFactory.MagnesiumOxide)
               : MaterialFactory.createPureElement(el);
         final XRayTransitionSet xrts = new XRayTransitionSet(el, XRayTransitionSet.K_FAMILY);
         cz.addStandard(xrts, ref, props);
         result.addKRatio(xrts, mat.weightFraction(el, true) * cz.compute(xrts, mat, props), 0.0);
         czc.addStandard(xrts, ref, props);
      }
      final Strategy strategy = new Strategy();
      strategy.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.SimpleIteration);
      AlgorithmUser.applyGlobalOverride(strategy);

      final Composition redux = czc.compute(result, props);
      System.out.println(czc.getAlgorithm(IterationAlgorithm.class));
      System.out.print("Original: ");
      System.out.println(mat.weightPercentString(false));
      System.out.print("Result: ");
      System.out.println(redux.weightPercentString(false));
      System.out.println("Iteration count = " + Integer.toString(czc.getIterationCount()));
      assertEquals(redux.difference(mat), 0.0, 1.0e-4);
   }

   public void testHyperbolic()
         throws EPQException {
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);

      final KRatioSet result = new KRatioSet();

      // Use the same SpectrumProperties for references and unknown
      final SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

      final ComputeZAF cz = new ComputeZAF();
      final CompositionFromKRatios czc = new CompositionFromKRatios();
      czc.setConvergenceCriterion(1.0e-4);
      for(final Element el : mat.getElementSet()) {
         final Composition ref = el.equals(Element.O) ? MaterialFactory.createMaterial(MaterialFactory.MagnesiumOxide)
               : MaterialFactory.createPureElement(el);
         final XRayTransitionSet xrts = new XRayTransitionSet(el, XRayTransitionSet.K_FAMILY);
         cz.addStandard(xrts, ref, props);
         result.addKRatio(xrts, mat.weightFraction(el, true) * cz.compute(xrts, mat, props), 0.0);
         czc.addStandard(xrts, ref, props);
      }
      final Strategy strategy = new Strategy();
      strategy.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.WegsteinIteration);
      AlgorithmUser.applyGlobalOverride(strategy);

      final Composition redux = czc.compute(result, props);
      System.out.println(czc.getAlgorithm(IterationAlgorithm.class));
      System.out.print("Original: ");
      System.out.println(mat.weightPercentString(false));
      System.out.print("Result: ");
      System.out.println(redux.weightPercentString(false));
      System.out.println("Iteration count = " + Integer.toString(czc.getIterationCount()));
      assertEquals(redux.difference(mat), 0.0, 1.0e-4);
   }

   public void testPAP()
         throws EPQException {
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);

      final KRatioSet result = new KRatioSet();

      // Use the same SpectrumProperties for references and unknown
      final SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

      final ComputeZAF cz = new ComputeZAF();
      final CompositionFromKRatios czc = new CompositionFromKRatios();
      czc.setConvergenceCriterion(1.0e-4);
      for(final Element el : mat.getElementSet()) {
         final Composition ref = el.equals(Element.O) ? MaterialFactory.createMaterial(MaterialFactory.MagnesiumOxide)
               : MaterialFactory.createPureElement(el);
         final XRayTransitionSet xrts = new XRayTransitionSet(el, XRayTransitionSet.K_FAMILY);
         cz.addStandard(xrts, ref, props);
         result.addKRatio(xrts, mat.weightFraction(el, true) * cz.compute(xrts, mat, props), 0.0);
         czc.addStandard(xrts, ref, props);
      }
      final Strategy strategy = new Strategy();
      strategy.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.SimpleIteration);
      AlgorithmUser.applyGlobalOverride(strategy);

      final Composition redux = czc.compute(result, props);
      assertTrue(czc.getAlgorithm(IterationAlgorithm.class) == IterationAlgorithm.SimpleIteration);
      System.out.println(czc.getAlgorithm(IterationAlgorithm.class));
      System.out.print("Original: ");
      System.out.println(mat.weightPercentString(false));
      System.out.print("Result: ");
      System.out.println(redux.weightPercentString(false));
      System.out.println("Iteration count = " + Integer.toString(czc.getIterationCount()));
      assertEquals(redux.difference(mat), 0.0, 1.0e-4);
   }

   public void testPAP2()
         throws EPQException {
      final KRatioSet result = new KRatioSet();
      final XRayTransitionSet xrtsAu = new XRayTransitionSet(Element.Au, XRayTransitionSet.M_ALPHA);
      final XRayTransitionSet xrtsAg = new XRayTransitionSet(Element.Ag, XRayTransitionSet.L_ALPHA);

      result.addKRatio(xrtsAu, 0.3926);
      result.addKRatio(xrtsAg, 0.4734);

      // Use the same SpectrumProperties for references and unknown
      final SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

      final CompositionFromKRatios czc = new CompositionFromKRatios();
      czc.setConvergenceCriterion(1.0e-5);
      czc.addStandard(xrtsAu, MaterialFactory.createPureElement(Element.Au), props);
      czc.addStandard(xrtsAg, MaterialFactory.createPureElement(Element.Ag), props);

      final Strategy strategy = new Strategy();
      strategy.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.SimpleIteration);
      strategy.addAlgorithm(CorrectionAlgorithm.class, CorrectionAlgorithm.PouchouAndPichoir);
      AlgorithmUser.applyGlobalOverride(strategy);

      final Composition redux = czc.compute(result, props);
      System.out.println(czc.getAlgorithm(IterationAlgorithm.class));
      System.out.println(czc.getAlgorithm(CorrectionAlgorithm.class));
      assertEquals(redux.weightFraction(Element.Au, false), 0.4, 0.001);
      assertEquals(redux.weightFraction(Element.Ag, false), 0.6, 0.001);
   }

   public void testPAP3()
         throws EPQException {
      final KRatioSet result = new KRatioSet();
      final XRayTransitionSet xrtsAu = new XRayTransitionSet(Element.Au, XRayTransitionSet.M_ALPHA);
      final XRayTransitionSet xrtsAg = new XRayTransitionSet(Element.Ag, XRayTransitionSet.L_ALPHA);

      result.addKRatio(xrtsAu, 0.3843);
      result.addKRatio(xrtsAg, 0.5867);

      // Use the same SpectrumProperties for references and unknown
      final SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, 10.0); // keV
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, 50.0); // degrees

      final CompositionFromKRatios czc = new CompositionFromKRatios();
      czc.setConvergenceCriterion(1.0e-5);
      czc.addStandard(xrtsAu, MaterialFactory.createPureElement(Element.Au), props);
      czc.addStandard(xrtsAg, MaterialFactory.createPureElement(Element.Ag), props);

      final Strategy strategy = new Strategy();
      strategy.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.SimpleIteration);
      strategy.addAlgorithm(CorrectionAlgorithm.class, CorrectionAlgorithm.PouchouAndPichoir);
      AlgorithmUser.applyGlobalOverride(strategy);

      final Composition redux = czc.compute(result, props);
      assertEquals(redux.weightFraction(Element.Au, false), 0.4, 0.001);
      assertEquals(redux.weightFraction(Element.Ag, false), 0.6, 0.001);
   }

   public void testWegstein()
         throws EPQException {
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);
      final KRatioSet result = new KRatioSet();
      // Use the same SpectrumProperties for references and unknown
      final SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

      final ComputeZAF cz = new ComputeZAF();
      final CompositionFromKRatios czc = new CompositionFromKRatios();
      czc.setConvergenceCriterion(1.0e-6);
      for(final Element el : mat.getElementSet()) {
         final Composition ref = el.equals(Element.O) ? MaterialFactory.createMaterial(MaterialFactory.MagnesiumOxide)
               : MaterialFactory.createPureElement(el);
         final XRayTransitionSet xrts = new XRayTransitionSet(el, XRayTransitionSet.K_FAMILY);
         cz.addStandard(xrts, ref, props);
         result.addKRatio(xrts, mat.weightFraction(el, true) * cz.compute(xrts, mat, props), 0.0);
         czc.addStandard(xrts, ref, props);
      }
      System.out.println(result);

      final Strategy strategy = new Strategy();
      strategy.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.WegsteinIteration);
      AlgorithmUser.applyGlobalOverride(strategy);

      final Composition redux = czc.compute(result, props);
      System.out.println(czc.getAlgorithm(IterationAlgorithm.class));
      System.out.println("Composition");
      System.out.println("Original: " + mat.weightPercentString(false));
      System.out.println("  Result: " + redux.weightPercentString(false));
      System.out.println("K-ratio");
      System.out.println("Original: " + czc.getQuantifiedKRatios().toString());
      System.out.println("  Result: " + result.toString());
      System.out.println("   Count: " + Integer.toString(czc.getIterationCount()));
      assertEquals(czc.getQuantifiedKRatios().difference(result), 0.0, czc.getConvergenceCriterion());
      // Why does this fail here???????
      assertEquals(redux.difference(mat), 0.0, 1.0e-4);
   }

   public void testCompoundsSimple()
         throws EPQException {
      final Strategy strategy = new Strategy();
      strategy.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.SimpleIteration);
      AlgorithmUser.applyGlobalOverride(strategy);
      // Fe2O3
      {
         final Composition mat = MaterialFactory.createCompound("Fe2O3");
         final KRatioSet result = new KRatioSet();
         // Use the same SpectrumProperties for references and unknown
         final SpectrumProperties props = new SpectrumProperties();
         props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
         props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

         final ComputeZAF cz = new ComputeZAF();
         final CompositionFromKRatios czc = new CompositionFromKRatios();
         czc.setConvergenceCriterion(1.0e-4);
         {
            final Element elm = Element.Fe;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createPureElement(elm);
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         {
            final Element elm = Element.O;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createCompound("MgO");
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }

         final Composition redux = czc.compute(result, props);
         System.out.println(czc.getAlgorithm(IterationAlgorithm.class).toString());
         System.out.println("Original: " + mat.weightPercentString(true));
         System.out.println("Result:   " + redux.weightPercentString(false));
         System.out.println("Iteration count = " + Integer.toString(czc.getIterationCount()));
         assertEquals(redux.difference(mat), 0.0, 1.0e-4);
      }
      // Calcium Carbonate
      {
         final Composition mat = MaterialFactory.createCompound("CaCO3");
         final KRatioSet result = new KRatioSet();
         // Use the same SpectrumProperties for references and unknown
         final SpectrumProperties props = new SpectrumProperties();
         props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
         props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

         final ComputeZAF cz = new ComputeZAF();
         final CompositionFromKRatios czc = new CompositionFromKRatios();
         czc.setConvergenceCriterion(1.0e-4);

         {
            final Element elm = Element.Ca;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createPureElement(elm);
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         {
            final Element elm = Element.C;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createPureElement(elm);
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         {
            final Element elm = Element.O;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createCompound("MgO");
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         final Composition redux = czc.compute(result, props);
         System.out.println(czc.getAlgorithm(IterationAlgorithm.class).toString());
         System.out.println("Original: " + mat.weightPercentString(true));
         System.out.println("Result:   " + redux.weightPercentString(false));
         System.out.println("Iteration count = " + Integer.toString(czc.getIterationCount()));
         assertEquals(redux.difference(mat), 0.0, 1.0e-4);
      }
   }

   public void testCompoundsHyperbolic()
         throws EPQException {
      final Strategy strategy = new Strategy();
      strategy.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.WegsteinIteration);
      AlgorithmUser.applyGlobalOverride(strategy);
      // Fe2O3
      {
         final Composition mat = MaterialFactory.createCompound("Fe2O3");
         final KRatioSet result = new KRatioSet();
         // Use the same SpectrumProperties for references and unknown
         final SpectrumProperties props = new SpectrumProperties();
         props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
         props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

         final ComputeZAF cz = new ComputeZAF();
         final CompositionFromKRatios czc = new CompositionFromKRatios();
         czc.setConvergenceCriterion(1.0e-4);

         {
            final Element elm = Element.Fe;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createPureElement(elm);
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         {
            final Element elm = Element.O;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createCompound("MgO");
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }

         final Composition redux = czc.compute(result, props);
         System.out.println(czc.getAlgorithm(IterationAlgorithm.class).toString());
         System.out.println("Original: " + mat.weightPercentString(true));
         System.out.println("Result:   " + redux.weightPercentString(false));
         System.out.println("Iteration count = " + Integer.toString(czc.getIterationCount()));
         assertEquals(redux.difference(mat), 0.0, 1.0e-4);
      }
      // Calcium Carbonate
      {
         final Composition mat = MaterialFactory.createCompound("CaCO3");
         final KRatioSet result = new KRatioSet();
         // Use the same SpectrumProperties for references and unknown
         final SpectrumProperties props = new SpectrumProperties();
         props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
         props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

         final ComputeZAF cz = new ComputeZAF();
         final CompositionFromKRatios czc = new CompositionFromKRatios();
         czc.setConvergenceCriterion(1.0e-4);

         {
            final Element elm = Element.Ca;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createPureElement(elm);
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         {
            final Element elm = Element.C;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createPureElement(elm);
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         {
            final Element elm = Element.O;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createCompound("MgO");
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         final Composition redux = czc.compute(result, props);
         System.out.println(czc.getAlgorithm(IterationAlgorithm.class).toString());
         System.out.println("Original: " + mat.weightPercentString(true));
         System.out.println("Result:   " + redux.weightPercentString(false));
         System.out.println("Iteration count = " + Integer.toString(czc.getIterationCount()));
         assertEquals(redux.difference(mat), 0.0, 1.0e-4);
      }
   }

   public void testCompoundsPAP()
         throws EPQException {
      final Strategy strategy = new Strategy();
      strategy.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.WegsteinIteration);
      AlgorithmUser.applyGlobalOverride(strategy);
      // Fe2O3
      {
         final Composition mat = MaterialFactory.createCompound("Fe2O3");
         final KRatioSet result = new KRatioSet();
         // Use the same SpectrumProperties for references and unknown
         final SpectrumProperties props = new SpectrumProperties();
         props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
         props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

         final ComputeZAF cz = new ComputeZAF();
         final CompositionFromKRatios czc = new CompositionFromKRatios();
         czc.setConvergenceCriterion(1.0e-4);

         {
            final Element elm = Element.Fe;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createPureElement(elm);
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         {
            final Element elm = Element.O;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createCompound("MgO");
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }

         final Composition redux = czc.compute(result, props);
         System.out.println(czc.getAlgorithm(IterationAlgorithm.class).toString());
         System.out.println("Original: " + mat.weightPercentString(true));
         System.out.println("Result:   " + redux.weightPercentString(false));
         System.out.println("Iteration count = " + Integer.toString(czc.getIterationCount()));
         assertEquals(redux.difference(mat), 0.0, 1.0e-4);
      }
      // Calcium Carbonate
      {
         final Composition mat = MaterialFactory.createCompound("CaCO3");
         final KRatioSet result = new KRatioSet();
         // Use the same SpectrumProperties for references and unknown
         final SpectrumProperties props = new SpectrumProperties();
         props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
         props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

         final ComputeZAF cz = new ComputeZAF();
         final CompositionFromKRatios czc = new CompositionFromKRatios();
         czc.setConvergenceCriterion(1.0e-4);

         {
            final Element elm = Element.Ca;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createPureElement(elm);
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         {
            final Element elm = Element.C;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createPureElement(elm);
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         {
            final Element elm = Element.O;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createCompound("MgO");
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         final Composition redux = czc.compute(result, props);
         System.out.println(czc.getAlgorithm(IterationAlgorithm.class).toString());
         System.out.println("Original: " + mat.weightPercentString(true));
         System.out.println("Result:   " + redux.weightPercentString(false));
         System.out.println("Iteration count = " + Integer.toString(czc.getIterationCount()));
         assertEquals(redux.difference(mat), 0.0, 1.0e-4);
      }
   }

   public void testCompoundsWegstein()
         throws EPQException {
      final Strategy strategy = new Strategy();
      strategy.addAlgorithm(IterationAlgorithm.class, IterationAlgorithm.WegsteinIteration);
      AlgorithmUser.applyGlobalOverride(strategy);
      // Fe2O3
      {
         final Composition mat = MaterialFactory.createCompound("Fe2O3");
         final KRatioSet result = new KRatioSet();
         // Use the same SpectrumProperties for references and unknown
         final SpectrumProperties props = new SpectrumProperties();
         props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
         props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

         final ComputeZAF cz = new ComputeZAF();
         final CompositionFromKRatios czc = new CompositionFromKRatios();
         czc.setConvergenceCriterion(1.0e-4);
         {
            final Element elm = Element.Fe;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createPureElement(elm);
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         {
            final Element elm = Element.O;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createCompound("MgO");
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }

         final Composition redux = czc.compute(result, props);
         System.out.println(czc.getAlgorithm(IterationAlgorithm.class).toString());
         System.out.println("Original: " + mat.weightPercentString(true));
         System.out.println("Result:   " + redux.weightPercentString(false));
         System.out.println("Iteration count = " + Integer.toString(czc.getIterationCount()));
         assertEquals(redux.difference(mat), 0.0, 1.0e-4);
      }
      // Calcium Carbonate
      {
         final Composition mat = MaterialFactory.createCompound("CaCO3");
         final KRatioSet result = new KRatioSet();
         // Use the same SpectrumProperties for references and unknown
         final SpectrumProperties props = new SpectrumProperties();
         props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
         props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

         final ComputeZAF cz = new ComputeZAF();
         final CompositionFromKRatios czc = new CompositionFromKRatios();
         czc.setConvergenceCriterion(1.0e-4);
         {
            final Element elm = Element.Ca;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createPureElement(elm);
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         {
            final Element elm = Element.C;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createPureElement(elm);
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         {
            final Element elm = Element.O;
            final XRayTransitionSet xrts = new XRayTransitionSet(elm, XRayTransitionSet.K_FAMILY);
            final Composition ref = MaterialFactory.createCompound("MgO");
            cz.addStandard(xrts, ref, props);
            czc.addStandard(xrts, ref, props);
            result.addKRatio(xrts, cz.compute(xrts, mat, props) * mat.weightFraction(elm, true));
         }
         final Composition redux = czc.compute(result, props);
         System.out.println(czc.getAlgorithm(IterationAlgorithm.class).toString());
         System.out.println("Original: " + mat.weightPercentString(false));
         System.out.println("Result:   " + redux.weightPercentString(false));
         System.out.println("Iteration count = " + Integer.toString(czc.getIterationCount()));
         assertEquals(redux.difference(mat), 0.0, 1.0e-4);
      }
   }
}
