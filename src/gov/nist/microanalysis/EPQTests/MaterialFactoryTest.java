package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the MaterialFactory class.
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
public class MaterialFactoryTest
   extends TestCase {
   /**
    * MaterialFactoryTest
    */
   public MaterialFactoryTest(String test) {
      super(test);
   }

   public void testOne()
         throws EPQException {
      // Check a smattering of different densities
      assertTrue(MaterialFactory.createPureElement(Element.Ti).getDensity() == ToSI.gPerCC(4.5));
      assertTrue(MaterialFactory.createPureElement(Element.Ge).getDensity() == ToSI.gPerCC(5.46));
      assertTrue(MaterialFactory.createPureElement(Element.Ir).getDensity() == ToSI.gPerCC(22.42));
      assertTrue(MaterialFactory.createPureElement(Element.U).getDensity() == ToSI.gPerCC(18.7));
   }

   public void testApatite()
         throws EPQException {
      final Composition c = MaterialFactory.createCompound("Ca5(PO3)4Cl");
      assertEquals(c.atomicPercent(Element.Ca), 5.0, 0.001);
      assertEquals(c.atomicPercent(Element.P), 4.0, 0.001);
      assertEquals(c.atomicPercent(Element.O), 12.0, 0.001);
      assertEquals(c.atomicPercent(Element.Cl), 1.0, 0.001);
   }

   public void testGlass()
         throws EPQException {
      Composition c = MaterialFactory.createCompound("(((((SiO2)))))");
      assertEquals(c.atomicPercent(Element.Si), 1.0, 0.001);
      assertEquals(c.atomicPercent(Element.O), 2.0, 0.001);
      c = MaterialFactory.createCompound("SiO2");
      assertEquals(c.atomicPercent(Element.Si), 1.0, 0.001);
      assertEquals(c.atomicPercent(Element.O), 2.0, 0.001);
   }

   public void testTableSalt()
         throws EPQException {
      Composition c = MaterialFactory.createCompound("NaCl");
      assertEquals(c.atomicPercent(Element.Na), 1.0, 0.001);
      assertEquals(c.atomicPercent(Element.Cl), 1.0, 0.001);
      c = MaterialFactory.createCompound("Na1Cl1");
      assertEquals(c.atomicPercent(Element.Na), 1.0, 0.001);
      assertEquals(c.atomicPercent(Element.Cl), 1.0, 0.001);
   }

   public void testSomething()
         throws EPQException {
      final Composition c = MaterialFactory.createCompound("(SiO2)3(MgO3)Al2O3((Ag3Au2)2Ca)2");
      assertEquals(c.atomicPercent(Element.Si), 3.0, 0.001);
      assertEquals(c.atomicPercent(Element.O), 12.0, 0.001);
      assertEquals(c.atomicPercent(Element.Mg), 1.0, 0.001);
      assertEquals(c.atomicPercent(Element.Al), 2.0, 0.001);
      assertEquals(c.atomicPercent(Element.Ag), 12.0, 0.001);
      assertEquals(c.atomicPercent(Element.Au), 8.0, 0.001);
      assertEquals(c.atomicPercent(Element.Ca), 2.0, 0.001);
   }
}
