package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.TransitionEnergy;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the TransitionEnergy class.
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
public class TransitionEnergyTest extends TestCase {
   public TransitionEnergyTest(String test) {
      super(test);
   }

   public void testOne() throws EPQException {
      final XRayTransition xrt = new XRayTransition(Element.Cu, AtomicShell.MV, AtomicShell.LIII);
      assertEquals(TransitionEnergy.DTSA.compute(xrt), TransitionEnergy.Chantler2005.compute(xrt), ToSI.eV(100.0));
   }
}
