package gov.nist.microanalysis.EPQTests;

import junit.framework.TestCase;

import gov.nist.microanalysis.EPQLibrary.AlgorithmUser;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.ComputeZAF;
import gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.Strategy;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQLibrary.XRayTransitionSet;

/**
 * <p>
 * Tests the ComputeZAF class.
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
public class ComputeZAFTest
   extends TestCase {

   /**
    * Constructs a ComputeZAFTest
    * 
    * @param arg0
    */
   public ComputeZAFTest(String arg0) {
      super(arg0);
   }

   public void testOne()
         throws EPQException {
      final Element el = Element.Ca;
      final Composition unknown = MaterialFactory.createMaterial(MaterialFactory.K411);
      final Composition reference = MaterialFactory.createPureElement(el);
      final int transition = XRayTransition.KA1;
      final CorrectionAlgorithm ca = CorrectionAlgorithm.PouchouAndPichoir;

      // Use the same SpectrumProperties for references and unknown
      final SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

      final XRayTransition xrt = new XRayTransition(el, transition);
      final XRayTransitionSet xrts = new XRayTransitionSet(el, XRayTransitionSet.K_ALPHA);

      final ComputeZAF cz = new ComputeZAF();
      cz.setMinWeight(1.0);
      {
         final Strategy st = new Strategy();
         st.addAlgorithm(CorrectionAlgorithm.class, ca);
         AlgorithmUser.applyGlobalOverride(st);
      }
      cz.addStandard(xrts, reference, props);
      final double uZaf = cz.compute(xrts, unknown, props);

      ca.initialize(reference, xrt.getDestination(), props);
      final double refE = ca.computeZAFCorrection(xrt);
      ca.initialize(unknown, xrt.getDestination(), props);
      final double unkE = ca.computeZAFCorrection(xrt);

      assertEquals(uZaf, unkE / refE, 1.0e-6);
   }

   public void testTwo()
         throws EPQException {
      final Element el = Element.Ca;
      final Composition unknown = MaterialFactory.createMaterial(MaterialFactory.K411);
      final Composition reference = MaterialFactory.createPureElement(el);
      final int transition = XRayTransition.KA1;
      final CorrectionAlgorithm ca = CorrectionAlgorithm.PouchouAndPichoir;

      // Use the same SpectrumProperties for references and unknown
      final SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

      final XRayTransition xrt = new XRayTransition(el, transition);
      final XRayTransitionSet xrts = new XRayTransitionSet(xrt);

      final ComputeZAF cz = new ComputeZAF();
      cz.setMinWeight(1.0);
      {
         final Strategy st = new Strategy();
         st.addAlgorithm(CorrectionAlgorithm.class, ca);
         AlgorithmUser.applyGlobalOverride(st);
      }
      cz.addStandard(xrts, reference, props);
      final double uZaf = cz.compute(xrts, unknown, props);

      ca.initialize(reference, xrt.getDestination(), props);
      final double refE = ca.computeZAFCorrection(xrt);
      ca.initialize(unknown, xrt.getDestination(), props);
      final double unkE = ca.computeZAFCorrection(xrt);

      assertEquals(uZaf, unkE / refE, 1.0e-6);
   }

   public void testThree()
         throws EPQException {
      final Element el = Element.Ca;
      final Composition unknown = MaterialFactory.createMaterial(MaterialFactory.K411);
      final Composition reference = MaterialFactory.createPureElement(el);
      final int transition = XRayTransition.KA1;
      final CorrectionAlgorithm ca = CorrectionAlgorithm.PouchouAndPichoir;

      // Use the same SpectrumProperties for references and unknown
      final SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

      final XRayTransition xrt = new XRayTransition(el, transition);
      final XRayTransitionSet xrts = new XRayTransitionSet(el, XRayTransitionSet.K_FAMILY);

      final ComputeZAF cz = new ComputeZAF();
      cz.setMinWeight(0.0);
      {
         final Strategy st = new Strategy();
         st.addAlgorithm(CorrectionAlgorithm.class, ca);
         AlgorithmUser.applyGlobalOverride(st);
      }
      cz.addStandard(xrts, reference, props);
      final double uZaf = cz.compute(xrts, unknown, props);

      ca.initialize(reference, xrt.getDestination(), props);
      final double refE = ca.computeZAFCorrection(xrt);
      ca.initialize(unknown, xrt.getDestination(), props);
      final double unkE = ca.computeZAFCorrection(xrt);

      assertEquals(uZaf, unkE / refE, 2.0e-3);
   }

   public void testFour()
         throws EPQException {
      final Element el = Element.Pb;
      final Composition unknown = new Composition();
      unknown.addElementByStoiciometry(Element.Pb, 1.0 / 3.0);
      unknown.addElementByStoiciometry(Element.O, 2.0 / 3.0);

      final Composition reference = MaterialFactory.createPureElement(el);
      final int transition = XRayTransition.LA1;
      final CorrectionAlgorithm ca = CorrectionAlgorithm.XPP;

      // Use the same SpectrumProperties for references and unknown
      final SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0); // keV
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0); // degrees

      final XRayTransition xrt = new XRayTransition(el, transition);
      final XRayTransitionSet xrts = new XRayTransitionSet(el, XRayTransitionSet.L_ALPHA);

      final ComputeZAF cz = new ComputeZAF();
      cz.setMinWeight(0.0);
      {
         final Strategy st = new Strategy();
         st.addAlgorithm(CorrectionAlgorithm.class, ca);
         AlgorithmUser.applyGlobalOverride(st);
      }
      cz.addStandard(xrts, reference, props);
      final double uZaf = cz.compute(xrts, unknown, props);

      System.out.print("ZAF(Fe in FeO2) -> ");
      System.out.println(uZaf);
      {
         final XRayTransitionSet tr = new XRayTransitionSet(Element.O, XRayTransitionSet.K_FAMILY);
         cz.addStandard(tr, MaterialFactory.createMaterial(MaterialFactory.MagnesiumOxide), props);
         System.out.print("ZAF(O in FeO2 relative to MgO) -> ");
         System.out.println(cz.compute(tr, unknown, props));
      }

      ca.initialize(reference, xrt.getDestination(), props);
      final double refE = ca.computeZAFCorrection(xrt);
      ca.initialize(unknown, xrt.getDestination(), props);
      final double unkE = ca.computeZAFCorrection(xrt);

      assertEquals(uZaf, unkE / refE, 2.0e-3);
   }
}
