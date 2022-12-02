package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.Fluorescence;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the Fluorescence class.
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
public class FluorescenceTest extends TestCase {

   /**
    * Constructs a FluorescenceTest
    * 
    * @param arg0
    */
   public FluorescenceTest(String arg0) {
      super(arg0);
   }

   public void testOne() {
      final double beamE = ToSI.keV(25.0);
      final double takeOff = Math.toRadians(40.0);
      final Composition comp = MaterialFactory.createMaterial(MaterialFactory.K3189);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.Si, XRayTransition.KA1), beamE, takeOff), 1.0022, 0.01);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.O, XRayTransition.KA1), beamE, takeOff), 1.0013, 0.01);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.Al, XRayTransition.KA1), beamE, takeOff), 1.0084, 0.01);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.Ca, XRayTransition.KA1), beamE, takeOff), 1.0143, 0.01);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.Mg, XRayTransition.KA1), beamE, takeOff), 1.0062, 0.01);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.Ti, XRayTransition.KA1), beamE, takeOff), 1.0285, 0.01);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.Fe, XRayTransition.KA1), beamE, takeOff), 1.000, 0.01);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.Fe, XRayTransition.LA1), beamE, takeOff), 1.0013, 0.01);
   }

   public void testTwo() {
      final double beamE = ToSI.keV(25.0);
      final double takeOff = Math.toRadians(40.0);
      final Composition comp = new Composition(new Element[]{Element.Fe, Element.Ni}, new double[]{0.5, 0.5});
      final XRayTransition xrt = new XRayTransition(Element.Fe, XRayTransition.KA1);
      assertEquals(Fluorescence.Reed.compute(comp, xrt, beamE, takeOff), 1.10, 0.005);
   }

   public void testThree() {
      final double beamE = ToSI.keV(20.0);
      final double takeOff = Math.toRadians(40.0);
      final Composition comp = MaterialFactory.createMaterial(MaterialFactory.K3189);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.Si, XRayTransition.KA1), beamE, takeOff), 1.0015, 0.001);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.O, XRayTransition.KA1), beamE, takeOff), 1.0008, 0.001);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.Al, XRayTransition.KA1), beamE, takeOff), 1.0091, 0.001);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.Ca, XRayTransition.KA1), beamE, takeOff), 1.0087, 0.001);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.Mg, XRayTransition.KA1), beamE, takeOff), 1.0068, 0.001);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.Fe, XRayTransition.KA1), beamE, takeOff), 1.0000, 0.0001);
      assertEquals(Fluorescence.Reed.compute(comp, new XRayTransition(Element.Ti, XRayTransition.KA1), beamE, takeOff), 1.0177, 0.002);
   }

}
