package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.SurfaceIonization;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the SurfaceIonization class.
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
public class SurfaceIonizationTest extends TestCase {

   /**
    * Constructs a SurfaceIonizationTest
    * 
    * @param arg0
    */
   public SurfaceIonizationTest(String arg0) {
      super(arg0);
   }

   public void testPouchou91A() throws EPQException {
      final SurfaceIonization si = SurfaceIonization.Pouchou1991;
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);
      final double e0 = ToSI.keV(20.0);
      {
         final AtomicShell sh = new AtomicShell(Element.Ca, AtomicShell.K);
         assertEquals(si.compute(mat, sh, e0), 1.3132, 0.001);
      }
   }

   public void testPouchou91B() throws EPQException {
      final SurfaceIonization si = SurfaceIonization.Pouchou1991;
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);
      final double e0 = ToSI.keV(20.0);
      {
         final AtomicShell sh = new AtomicShell(Element.Ti, AtomicShell.K);
         assertEquals(si.compute(mat, sh, e0), 1.3035, 0.001);
      }
   }

   public void testReuter1972A() throws EPQException {
      final SurfaceIonization si = SurfaceIonization.Reuter1972;
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);
      final double e0 = ToSI.keV(20.0);
      {
         final AtomicShell sh = new AtomicShell(Element.Ca, AtomicShell.K);
         assertEquals(si.compute(mat, sh, e0), 1.3423, 0.001);
      }
   }

   public void testReuter1972B() throws EPQException {
      final SurfaceIonization si = SurfaceIonization.Reuter1972;
      final Composition mat = MaterialFactory.createMaterial(MaterialFactory.K3189);
      final double e0 = ToSI.keV(20.0);
      {
         final AtomicShell sh = new AtomicShell(Element.Ti, AtomicShell.K);
         assertEquals(si.compute(mat, sh, e0), 1.3248, 0.001);
      }
   }
}
