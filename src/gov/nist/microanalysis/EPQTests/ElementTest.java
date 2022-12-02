package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the Element class.
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
public class ElementTest extends TestCase {
   /**
    * ElementTest
    */
   public ElementTest(String test) {
      super(test);
   }

   // / Implements the test...
   public void testOne() {
      final Element elm = Element.byAtomicNumber(Element.elmTi);
      assertEquals(FromSI.eV(elm.meanIonizationPotential()), 247.24, 1.0);
      assertEquals(elm.getAtomicWeight(), 47.9, 1.0e-1);
      assertEquals(elm.getAtomicNumber(), 22);
      assertTrue(elm.compareTo(Element.byAtomicNumber(Element.elmH)) > 0);
      assertTrue(elm.compareTo(Element.byAtomicNumber(Element.elmFe)) < 0);
      assertTrue(elm.compareTo(Element.byAtomicNumber(Element.elmTi)) == 0);
      assertTrue(elm.equals(Element.byName("Ti")));
   }
}
