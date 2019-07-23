package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.NISTMonte.SimpleBlock;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the MonteCarloSS class.
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
public class MonteCarloSSTest
   extends TestCase {
   /**
    * MonteCarloSSTest
    */
   public MonteCarloSSTest(String test) {
      super(test);
   }

   public void testOne()
         throws EPQException {
      assertEquals(MonteCarloSS.ScatterEvent, 1);
      final MonteCarloSS mcss = new MonteCarloSS();
      // / TODO Restore these tests....
      // assertEquals(mcss.getMassAbsorptionCoefficient().compute(Element.Fe,
      // ToSI.eV(4511)), ToSI.inverse_gPerCC(ToSI.percm(187.7)),
      // ToSI.inverse_gPerCC(ToSI.percm(187.7)) / 10);
      // assertEquals(mcss.getMassAbsorptionCoefficient().compute(Element.U,
      // ToSI.eV(2697)), ToSI.inverse_gPerCC(ToSI.percm(1068.)),
      // ToSI.inverse_gPerCC(ToSI.percm(1068.)) / 5);
      // assertEquals(mcss.getMassAbsorptionCoefficient().compute(Element.Fe,
      // ToSI.eV(4511.0)), ToSI.inverse_gPerCC(ToSI.percm(187.7)),
      // ToSI.inverse_gPerCC(ToSI.percm(187.7)) / 10);
      // assertEquals(mcss.getMassAbsorptionCoefficient().compute(Element.Ti,
      // ToSI.keV(20.0),false), 1.54e-21, 0.1e-21);
      {
         final double[] o = {
            0.001,
            -0.001,
            0.0004
         };
         final double[] t = {
            1.0,
            1.0,
            1.0
         };
         final double u = mcss.getChamber().getShape().getFirstIntersection(o, t);
         assert u > 0.0;
         final double[] res = new double[3];
         res[0] = o[0] + (u * (t[0] - o[0]));
         res[1] = o[1] + (u * (t[1] - o[1]));
         res[2] = o[2] + (u * (t[2] - o[2]));
         assertTrue(mcss.getChamber().getShape().contains(res));
      }
      {
         final double[] c0 = {
            -1.0,
            -1.0,
            -1.0
         };
         final double[] c1 = {
            1.0,
            1.0,
            1.0
         };
         final SimpleBlock sb = new SimpleBlock(c0, c1);
         {
            final double[] pos0 = {
               -1.0,
               -1.0,
               -3.0
            };
            final double[] pos1 = {
               1.0,
               1.0,
               2.0
            };
            final double u = sb.getFirstIntersection(pos0, pos1);
            final double[] res = new double[3];
            res[0] = pos0[0] + ((pos1[0] - pos0[0]) * u);
            res[1] = pos0[1] + ((pos1[1] - pos0[1]) * u);
            res[2] = pos0[2] + ((pos1[2] - pos0[2]) * u);
            assertTrue(sb.contains(res));
         }
      }
   }
}
