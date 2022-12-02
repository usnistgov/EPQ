package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.MassAbsorptionCoefficient;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the MassAbsorptionCoefficient class.
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
public class MassAbsorptionCoefficientTest extends TestCase {

   static private double kConvert = 0.1;

   public MassAbsorptionCoefficientTest(String test) {
      super(test);
   }

   public void testOne() throws EPQException {
      // Check the MACs against Farthing and Walker's tabulation
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.B, ToSI.eV(677.)) / kConvert, 3906., 10.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Ti, ToSI.eV(8639.)) / kConvert, 166.05, 1.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Ca, ToSI.eV(392.)) / kConvert, 36579.0, 100.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Lu, ToSI.eV(183.)) / kConvert, 18102., 200.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Nb, ToSI.eV(2308.)) / kConvert, 613.69, 1.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Ru, ToSI.eV(2958.)) / kConvert, 1564.67, 10.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Al, ToSI.eV(7478)) / kConvert, 61.31, 1.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Ba, ToSI.eV(5415.)) / kConvert, 579.81, 10.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Ca, ToSI.eV(14958.)) / kConvert, 29.86, 1.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Ti, ToSI.eV(4932.)) / kConvert, 84.12, 1.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Tl, ToSI.eV(4932.)) / kConvert, 785.55, 1.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Ti, ToSI.eV(4932.)) / kConvert, 84.12, 1.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Ga, ToSI.eV(1282.)) / kConvert, 6684., 10.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.U, ToSI.eV(1098.)) / kConvert, 5835., 350.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.W, ToSI.eV(4840.)) / kConvert, 647.85, 10.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Tc, ToSI.eV(8398.)) / kConvert, 150.48, 1.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Cr, ToSI.eV(11727.)) / kConvert, 90.25, 1.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Fe, ToSI.eV(1872.)) / kConvert, 2019.45, 10.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Fr, ToSI.eV(4221.)) / kConvert, 1234.83, 10.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Os, ToSI.eV(1923.)) / kConvert, 1123., 10.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Os, ToSI.eV(2042.)) / kConvert, 4013., 10.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Os, ToSI.eV(2166.)) / kConvert, 3450., 10.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Os, ToSI.eV(2293.)) / kConvert, 2966, 10.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Os, ToSI.eV(2424.)) / kConvert, 2556, 10.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Os, ToSI.eV(2839.)) / kConvert, 2335.27, 10.0);
      assertEquals(MassAbsorptionCoefficient.Heinrich86.compute(Element.Os, ToSI.eV(2014.)) / kConvert, 1508.97, 10.0);

      assertEquals(MassAbsorptionCoefficient.BastinHeijligers89.compute(Element.Bi, new XRayTransition(Element.O, XRayTransition.KA1)) / kConvert,
            4430.0, 1.0);
      assertEquals(MassAbsorptionCoefficient.BastinHeijligers89.compute(Element.Fe, new XRayTransition(Element.N, XRayTransition.KA1)) / kConvert,
            7190.0, 1.0);
      assertEquals(MassAbsorptionCoefficient.BastinHeijligers89.compute(Element.Ta, new XRayTransition(Element.C, XRayTransition.KA1)) / kConvert,
            15350.0, 1.0);
   }
}
