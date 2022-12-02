package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.AlgorithmUser;
import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.StoppingPower;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the StoppingPower class.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author nritchie
 * @version 1.0
 */
public class StoppingPowerTest extends TestCase {

   /**
    * Constructs a StoppingPowerTest
    */
   public StoppingPowerTest(String test) {
      super(test);
   }

   public void testDuncumbReed1968() throws Exception {
      AlgorithmUser.clearGlobalOverride();
      final StoppingPower sp = StoppingPower.Thomas1963; // uses Duncumb &
      // DeCasa MIP
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);
      final double e0 = ToSI.keV(20.0);
      {
         final AtomicShell sh = new AtomicShell(Element.Ca, AtomicShell.K);
         final double res = sp.compute(mat, sh, e0) / sp.compute(MaterialFactory.createPureElement(sh.getElement()), sh, e0);
         assertEquals(res, 1.0812, 0.001);
      }
      {
         final AtomicShell sh = new AtomicShell(Element.Ti, AtomicShell.K);
         final double res = sp.compute(mat, sh, e0) / sp.compute(MaterialFactory.createPureElement(sh.getElement()), sh, e0);
         assertEquals(res, 1.2094, 0.001);
      }
      AlgorithmUser.clearGlobalOverride();
   }

   public void testPhilibertTixier1968() throws Exception {
      final StoppingPower sp = StoppingPower.PhilibertTixier1968; // Uses
      // Wilson1941
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);
      final double e0 = ToSI.keV(20.0);
      {
         final AtomicShell sh = new AtomicShell(Element.Ca, AtomicShell.K);
         final double res = sp.compute(mat, sh, e0) / sp.compute(MaterialFactory.createPureElement(sh.getElement()), sh, e0);
         assertEquals(res, 1.1037, 0.001);
      }
      {
         final AtomicShell sh = new AtomicShell(Element.Ti, AtomicShell.K);
         final double res = sp.compute(mat, sh, e0) / sp.compute(MaterialFactory.createPureElement(sh.getElement()), sh, e0);
         assertEquals(res, 1.2253, 0.001);
      }
      AlgorithmUser.clearGlobalOverride();
   }

   public void testPouchou91() throws Exception {
      final StoppingPower sp = StoppingPower.Pouchou1991; // Uses Zeller MIP
      {
         final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);
         {
            final double e0 = ToSI.keV(20.0);
            final AtomicShell sh = new AtomicShell(Element.Ca, AtomicShell.K);
            final double res = sp.compute(mat, sh, e0) / sp.compute(MaterialFactory.createPureElement(sh.getElement()), sh, e0);
            assertEquals(res, 1.1049, 0.001);
         }
         {
            final double e0 = ToSI.keV(20.0);
            final AtomicShell sh = new AtomicShell(Element.Ti, AtomicShell.K);
            final double res = sp.compute(mat, sh, e0) / sp.compute(MaterialFactory.createPureElement(sh.getElement()), sh, e0);
            assertEquals(res, 1.2271, 0.001);
         }
      }
      {
         final double e0 = ToSI.keV(20.0); // keV
         final Material fe = MaterialFactory.createPureElement(Element.Fe);
         final double res = StoppingPower.tokeVcmSqrPerGram(sp.compute(fe, new AtomicShell(Element.Fe, AtomicShell.LIII), e0));
         // res in keV per density per cm
         final double len = FromSI.keV(e0) / (res * FromSI.gPerCC(fe.getDensity()) * 100.0);
         // len in meters
         assertTrue(len > 10.0e-6);
         assertTrue(len < 50.0e-6);
      }

      {
         final double e0 = ToSI.keV(5.0); // keV
         final Material fe = MaterialFactory.createPureElement(Element.Fe);
         final double res = StoppingPower.tokeVcmSqrPerGram(sp.compute(fe, new AtomicShell(Element.Fe, AtomicShell.LIII), e0));
         // res in keV per density per cm
         final double len = FromSI.keV(e0) / (res * FromSI.gPerCC(fe.getDensity()) * 100.0);
         // len in meters
         assertTrue(len > 1.0e-7);
         assertTrue(len < 2.0e-6);
      }
      AlgorithmUser.clearGlobalOverride();
   }
}
