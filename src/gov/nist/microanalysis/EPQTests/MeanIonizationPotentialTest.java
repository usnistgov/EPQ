package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.MeanIonizationPotential;

import junit.framework.TestCase;

/**
 * <p>
 * Tests the MeanIonizationPotential class.
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
public class MeanIonizationPotentialTest extends TestCase {

   /**
    * Constructs a MeanIonizationPotentialTest
    * 
    * @param arg0
    */
   public MeanIonizationPotentialTest(String arg0) {
      super(arg0);
   }

   public void testOne() {
      final Composition k3189 = MaterialFactory.createMaterial(MaterialFactory.K3189);
      // Berger64
      assertEquals(FromSI.keV(MeanIonizationPotential.Berger64.compute(Element.Si)), 0.172, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Berger64.compute(Element.Al)), 0.163, 0.002);
      assertEquals(FromSI.keV(MeanIonizationPotential.Berger64.computeLn(k3189)), 0.159, 0.001);

      assertEquals(FromSI.keV(MeanIonizationPotential.Berger64.compute(Element.Ca)), 0.228, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Berger64.compute(Element.Ti)), 0.247, 0.001);

      // Duncumb69
      assertEquals(FromSI.keV(MeanIonizationPotential.Duncumb69.compute(Element.Si)), 0.154, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Duncumb69.compute(Element.Al)), 0.142, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Duncumb69.computeLn(k3189)), 0.162, 0.001);

      assertEquals(FromSI.keV(MeanIonizationPotential.Duncumb69.compute(Element.Ca)), 0.239, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Duncumb69.compute(Element.Ti)), 0.270, 0.001);
      // Bloch33
      assertEquals(FromSI.keV(MeanIonizationPotential.Bloch33.compute(Element.Si)), 0.189, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Bloch33.compute(Element.Al)), 0.176, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Bloch33.computeLn(k3189)), 0.165, 0.001);

      assertEquals(FromSI.keV(MeanIonizationPotential.Bloch33.compute(Element.Ca)), 0.270, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Bloch33.compute(Element.Ti)), 0.297, 0.001);

      // Zeller75
      assertEquals(FromSI.keV(MeanIonizationPotential.Zeller75.compute(Element.Si)), 0.174, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Zeller75.compute(Element.Al)), 0.164, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Zeller75.computeLn(k3189)), 0.157, 0.001);

      assertEquals(FromSI.keV(MeanIonizationPotential.Bloch33.compute(Element.Ca)), 0.270, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Bloch33.compute(Element.Ti)), 0.297, 0.001);

      // Springer67
      assertEquals(FromSI.keV(MeanIonizationPotential.Springer67.compute(Element.Si)), 0.154, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Springer67.compute(Element.Al)), 0.143, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Springer67.computeLn(k3189)), 0.137, 0.001);

      assertEquals(FromSI.keV(MeanIonizationPotential.Springer67.compute(Element.Ca)), 0.216, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Springer67.compute(Element.Ti)), 0.238, 0.001);
      // Heinrich70 (disagrees with TryZAF)
      assertEquals(FromSI.keV(MeanIonizationPotential.Heinrich70.compute(Element.Si)), 0.179, 0.001);
      // assertEquals(FromSI.keV(MeanIonizationPotential.Heinrich70.computeLn(k3189)),0.171,0.001);
      // Wilson41
      assertEquals(FromSI.keV(MeanIonizationPotential.Wilson41.compute(Element.Si)), 0.161, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Wilson41.compute(Element.Al)), 0.149, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Wilson41.computeLn(k3189)), 0.141, 0.001);

      assertEquals(FromSI.keV(MeanIonizationPotential.Wilson41.compute(Element.Ca)), 0.230, 0.001);
      assertEquals(FromSI.keV(MeanIonizationPotential.Wilson41.compute(Element.Ti)), 0.253, 0.001);

      // Untested Berger83, Sternheimer64
      assertEquals(MeanIonizationPotential.Berger64.compute(Element.Si), MeanIonizationPotential.Berger83.compute(Element.Si), 0.001);
   }

}
