package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.BackscatterCoefficient;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the BackscatterCoefficient class
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
public class BackscatterCoefficientTest
   extends TestCase {

   /**
    * Constructs a BackscatterCoefficientTest
    * 
    * @param arg0
    */
   public BackscatterCoefficientTest(String arg0) {
      super(arg0);
   }

   public void testPAP() {
      final BackscatterCoefficient bc = BackscatterCoefficient.PouchouAndPichoir91;
      assertEquals(bc.compute(Element.Ca, ToSI.keV(20.0)), 0.228, 0.001);
      assertEquals(bc.compute(Element.Ti, ToSI.keV(20.0)), 0.248, 0.001);
      assertEquals(bc.compute(MaterialFactory.createMaterial(MaterialFactory.K3189), ToSI.keV(20.0)), 0.149, 0.001);
   }

   public void testLoveScott() {
      final BackscatterCoefficient bc = BackscatterCoefficient.Love1978;
      assertEquals(bc.compute(Element.Ca, ToSI.keV(20.0)), 0.235, 0.001);
      assertEquals(bc.compute(Element.Ti, ToSI.keV(20.0)), 0.252, 0.001);
      assertEquals(bc.compute(MaterialFactory.createMaterial(MaterialFactory.K3189), ToSI.keV(20.0)), 0.165, 0.001);
   }

   public void testHeinrich() {
      final BackscatterCoefficient bc = BackscatterCoefficient.Heinrich81;
      assertEquals(bc.compute(Element.Ca, ToSI.keV(20.0)), 0.219662, 0.001);
      assertEquals(bc.compute(Element.Ti, ToSI.keV(20.0)), 0.24069, 0.001);
      assertEquals(bc.compute(MaterialFactory.createMaterial(MaterialFactory.K3189), ToSI.keV(20.0)), 0.141569, 0.001);
   }

}
