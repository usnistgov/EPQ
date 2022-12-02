package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.CorrectionAlgorithm;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import junit.framework.TestCase;

public class BrownJTA1982Test extends TestCase {
   CorrectionAlgorithm ca = CorrectionAlgorithm.Armstrong1982;
   CorrectionAlgorithm ca2 = CorrectionAlgorithm.XPP;

   private void helper(Composition mat, AtomicShell sh, SpectrumProperties props, XRayTransition xrt) throws EPQException {
      sh = xrt.getDestination();
      ca.initialize(mat, sh, props);
      ca2.initialize(mat, sh, props);

   }

   public void testPhiRhoZ() throws EPQException {

      final SpectrumProperties props = new SpectrumProperties();
      props.setNumericProperty(SpectrumProperties.BeamEnergy, 29.0);
      props.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0);
      final AtomicShell sh = null;

      // TiN
      Composition mat = MaterialFactory.createCompound("TiN");
      XRayTransition xrt = new XRayTransition(Element.N, XRayTransition.KA1);
      helper(mat, sh, props, xrt);
      assertEquals(ca.computeZACorrection(xrt), ca2.computeZACorrection(xrt), .002);
      xrt = new XRayTransition(Element.Ti, XRayTransition.KA1);
      helper(mat, sh, props, xrt);
      assertEquals(ca.computeZACorrection(xrt), ca2.computeZACorrection(xrt), .002);
      xrt = new XRayTransition(Element.Ti, XRayTransition.LA1);
      helper(mat, sh, props, xrt);
      assertEquals(ca.computeZACorrection(xrt), ca2.computeZACorrection(xrt), .002);

      // K411
      mat = MaterialFactory.createMaterial(MaterialFactory.K411);
      xrt = new XRayTransition(Element.Si, XRayTransition.KA1);
      helper(mat, sh, props, xrt);
      assertEquals(ca.computeZACorrection(xrt), ca2.computeZACorrection(xrt), .002);
      xrt = new XRayTransition(Element.O, XRayTransition.KA1);
      helper(mat, sh, props, xrt);
      assertEquals(ca.computeZACorrection(xrt), ca2.computeZACorrection(xrt), .002);
      xrt = new XRayTransition(Element.Ca, XRayTransition.KA1);
      helper(mat, sh, props, xrt);
      assertEquals(ca.computeZACorrection(xrt), ca2.computeZACorrection(xrt), .002);

      // SS316
      mat = MaterialFactory.createMaterial(MaterialFactory.SS316);
      xrt = new XRayTransition(Element.Ni, XRayTransition.KA1);
      helper(mat, sh, props, xrt);
      assertEquals(ca.computeZACorrection(xrt), ca2.computeZACorrection(xrt), .0012);
      xrt = new XRayTransition(Element.Ni, XRayTransition.LA1);
      helper(mat, sh, props, xrt);
      assertEquals(ca.computeZACorrection(xrt), ca2.computeZACorrection(xrt), .0012);
      xrt = new XRayTransition(Element.Cr, XRayTransition.KA1);
      helper(mat, sh, props, xrt);
      assertEquals(ca.computeZACorrection(xrt), ca2.computeZACorrection(xrt), .0012);
      xrt = new XRayTransition(Element.Cr, XRayTransition.LA1);
      helper(mat, sh, props, xrt);
      assertEquals(ca.computeZACorrection(xrt), ca2.computeZACorrection(xrt), .0012);
      xrt = new XRayTransition(Element.Fe, XRayTransition.KA1);
      helper(mat, sh, props, xrt);
      assertEquals(ca.computeZACorrection(xrt), ca2.computeZACorrection(xrt), .0012);
      xrt = new XRayTransition(Element.Fe, XRayTransition.LA1);
      helper(mat, sh, props, xrt);
      assertEquals(ca.computeZACorrection(xrt), ca2.computeZACorrection(xrt), .0012);

   }

}
