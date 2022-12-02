package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQLibrary.XRayTransitionSet;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the XRayTransitionSet class.
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
public class XRayTransitionSetTest extends TestCase {

   /**
    * Constructs a XRayTransitionSetTest
    * 
    * @param arg0
    */
   public XRayTransitionSetTest(String arg0) {
      super(arg0);
   }

   public void testOne() throws EPQException {
      {
         final XRayTransitionSet xrts = new XRayTransitionSet(Element.Ba, XRayTransitionSet.L_ALPHA);
         assertEquals(XRayTransitionSet.parseString(xrts.toParseable()), xrts);
      }

      {
         final XRayTransitionSet xrts = new XRayTransitionSet(Element.Au, XRayTransitionSet.M_BETA);
         assertEquals(XRayTransitionSet.parseString(xrts.toParseable()), xrts);
      }
      {
         final XRayTransitionSet xrts = new XRayTransitionSet(new XRayTransition(Element.Cu, XRayTransition.KA1));
         assertEquals(XRayTransitionSet.parseString(xrts.toParseable()), xrts);
      }
      {
         final double min = 0.99 * XRayTransition.getEnergy(Element.Cu, XRayTransition.KA2);
         final double max = 1.01 * XRayTransition.getEnergy(Element.Cu, XRayTransition.KA1);
         assert (min < max);
         final XRayTransitionSet xrts = new XRayTransitionSet(Element.Cu, min, max);
         assertEquals(xrts.minEnergy().getTransitionIndex(), XRayTransition.KA2);
         assertEquals(xrts.maxEnergy().getTransitionIndex(), XRayTransition.KA1);
         assertEquals(xrts.toString(), "Cu " + XRayTransitionSet.K_ALPHA);
         assertEquals(XRayTransitionSet.parseString(xrts.toParseable()), xrts);
      }
      {
         final XRayTransitionSet xrts = new XRayTransitionSet(new XRayTransition(Element.Cu, XRayTransition.LA1));
         xrts.add(new XRayTransition(Element.Cu, XRayTransition.LA2));
         assertEquals(XRayTransitionSet.parseString(xrts.toParseable()), xrts);
      }
      {
         final XRayTransitionSet xrts = new XRayTransitionSet(new XRayTransition(Element.Cu, XRayTransition.LA1));
         xrts.add(new XRayTransition(Element.Cu, XRayTransition.LA2));
         xrts.add(new XRayTransition(Element.Cu, XRayTransition.KA1));
         assertEquals(XRayTransitionSet.parseString(xrts.toParseable()), xrts);
      }
      {
         final XRayTransitionSet xrts1 = new XRayTransitionSet(Element.Ba, XRayTransitionSet.M_BETA);
         final XRayTransitionSet xrts2 = new XRayTransitionSet(new XRayTransition(Element.Ba, XRayTransition.MB));
         assertEquals(xrts1, xrts2);
         xrts2.remove(new XRayTransition(Element.Ba, XRayTransition.MB));
         assertEquals(xrts2, new XRayTransitionSet());
      }
      {
         final XRayTransitionSet xrts1 = new XRayTransitionSet(Element.U, XRayTransitionSet.M_BETA);
         final XRayTransitionSet xrts2 = new XRayTransitionSet(new XRayTransition(Element.U, XRayTransition.MB));
         assertEquals(xrts1, xrts2);
         xrts2.remove(new XRayTransition(Element.U, XRayTransition.MB));
         assertEquals(xrts2, new XRayTransitionSet());
      }
      {
         final XRayTransitionSet xrts1 = new XRayTransitionSet(Element.U, XRayTransitionSet.M_OTHER);
         xrts1.remove(new XRayTransition(Element.U, XRayTransition.M2N4));
         assertNotSame(xrts1, new XRayTransitionSet(Element.U, XRayTransitionSet.M_OTHER));
         xrts1.add(new XRayTransition(Element.U, XRayTransition.M2N4));
         assertEquals(xrts1, new XRayTransitionSet(Element.U, XRayTransitionSet.M_OTHER));
      }

   }
}
