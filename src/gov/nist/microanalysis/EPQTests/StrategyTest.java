package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.BackscatterFactor;
import gov.nist.microanalysis.EPQLibrary.MassAbsorptionCoefficient;
import gov.nist.microanalysis.EPQLibrary.ProportionalIonizationCrossSection;
import gov.nist.microanalysis.EPQLibrary.Strategy;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the Strategy class.
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
public class StrategyTest extends TestCase {

   /**
    * Constructs a StrategyTest
    * 
    * @param arg0
    */
   public StrategyTest(String arg0) {
      super(arg0);
   }

   public void testOne() {
      final Strategy st = new Strategy();
      st.addAlgorithm(BackscatterFactor.class, BackscatterFactor.Duncumb1981);
      st.addAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Chantler2005);
      System.out.println(st.getAlgorithm(BackscatterFactor.class).toString());
      // This should fail...
      try {
         st.addAlgorithm(ProportionalIonizationCrossSection.class, MassAbsorptionCoefficient.Chantler2005);
         assertTrue(false);
      } catch (final IllegalArgumentException iae) {
         assertTrue(true);
      }
   }

}
