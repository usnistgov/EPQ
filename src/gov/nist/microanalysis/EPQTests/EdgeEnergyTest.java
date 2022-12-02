package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.EdgeEnergy;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the EdgeEnergy class.
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
public class EdgeEnergyTest extends TestCase {
   public EdgeEnergyTest(String test) {
      super(test);
   }

   public void testOne() {
      final AtomicShell sh = new AtomicShell(Element.W, AtomicShell.LIII);
      assertEquals(EdgeEnergy.Williams2011.compute(sh), EdgeEnergy.DTSA.compute(sh), ToSI.eV(10.0));
      assertEquals(EdgeEnergy.Chantler2005.compute(sh), EdgeEnergy.DTSA.compute(sh), ToSI.eV(10.0));
      assertEquals(EdgeEnergy.NISTxrtdb.compute(sh), EdgeEnergy.DTSA.compute(sh), ToSI.eV(10.0));
      assertEquals(EdgeEnergy.Wernish84.compute(sh), EdgeEnergy.DTSA.compute(sh), ToSI.eV(20.0));
   }
}
