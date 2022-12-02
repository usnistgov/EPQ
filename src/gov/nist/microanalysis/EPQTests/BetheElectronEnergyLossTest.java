package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.BetheElectronEnergyLoss;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import junit.framework.TestCase;

/**
 * <p>
 * Description: Tests implementations of the BetheElectronEnergyLoss class.
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
public class BetheElectronEnergyLossTest extends TestCase {

   /**
    * Constructs a BetheElectronEnergyLossTest
    * 
    * @param arg0
    */
   public BetheElectronEnergyLossTest(String arg0) {
      super(arg0);
   }

   public void testOne() {
      assertEquals(BetheElectronEnergyLoss.toNatural(BetheElectronEnergyLoss.JoyLuo1989.compute(Element.Mn, ToSI.keV(10.0))), -0.135042498, 1.0e-6);
      assertEquals(BetheElectronEnergyLoss.toNatural(BetheElectronEnergyLoss.JoyLuo1989.compute(Element.U, ToSI.keV(1.0))), -0.255337998, 1.0e-6);
      assertEquals(BetheElectronEnergyLoss.toNatural(BetheElectronEnergyLoss.Bethe1930.compute(Element.Mn, ToSI.keV(10.0))), -0.134247687, 1.0e-6);

   }
}
