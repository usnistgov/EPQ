package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the AtomicShell class.
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
public class AtomicShellTest
   extends TestCase {
   public AtomicShellTest(String test) {
      super(test);
   }

   public void testOne() {
      assertTrue(AtomicShell.getAtomicName(AtomicShell.LIII).equals("2P3/2"));
      assertTrue(AtomicShell.getFamilyName(AtomicShell.getFamily(AtomicShell.LIII)).equals("L"));
      assertTrue(AtomicShell.getIUPACName(AtomicShell.LIII).equals("L3"));
      assertTrue(AtomicShell.getPrincipalQuantumNumber(AtomicShell.LIII) == 2);
      assertTrue(AtomicShell.getSiegbahnName(AtomicShell.LIII).equals("LIII"));

      final AtomicShell as = new AtomicShell(Element.Cu, AtomicShell.LIII);
      assertTrue(as.getElement().equals(Element.Cu));
      assertTrue(as.getCapacity() == 4);
      assertEquals(as.getEdgeEnergy(), ToSI.keV(0.933), 0.010);
      assertTrue(as.getElement().equals(Element.Cu));
      assertEquals(FromSI.eV(as.getEnergy()), 933.0, 10.0);
      assertTrue(as.getFamily() == AtomicShell.LFamily);
      assertTrue(as.getShell() == AtomicShell.LIII);
      assertTrue(as.getOrbitalAngularMomentum() == 1);
      assertTrue(as.getTotalAngularMomentum() == 1.5);
      assertTrue(AtomicShell.isValid(AtomicShell.LIII));
      assertTrue(as.getGroundStateOccupancy() == 4);
   }
}
