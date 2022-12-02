package gov.nist.microanalysis.EPQTests;

import java.util.Iterator;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;

import junit.framework.TestCase;

/**
 * <p>
 * Tests the XRayTransition class.
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
public class XRayTransitionTest extends TestCase {
   public XRayTransitionTest(String test) {
      super(test);
   }

   public void testOne() throws Exception {
      {
         final XRayTransition xrt = new XRayTransition(new AtomicShell(Element.Au, AtomicShell.LIII), AtomicShell.MV);
         xrt.getEnergy();
         xrt.getWeight(XRayTransition.NormalizeDefault);
         xrt.getWeight(XRayTransition.NormalizeDestination);
         xrt.getWeight(XRayTransition.NormalizeFamily);
      }
   }

   public void testTwo() {
      // It is important that XRayTransition objects are sorted by destination
      // shell then source shell to optimize calculation in ComputeZAF.
      final TreeSet<XRayTransition> ts = new TreeSet<XRayTransition>();
      for (int i = XRayTransition.KA1; i < XRayTransition.Last; ++i)
         if (XRayTransition.exists(Element.U, i))
            ts.add(new XRayTransition(Element.U, i));
      final Iterator<XRayTransition> j = ts.iterator();
      XRayTransition prev = j.next();
      while (j.hasNext()) {
         final XRayTransition xrt = j.next();
         if (xrt.getDestinationShell() == prev.getDestinationShell())
            assertTrue(xrt.getSourceShell() > prev.getSourceShell());
         else
            assertTrue(xrt.getDestinationShell() > prev.getDestinationShell());
         prev = xrt;
      }
   }
}
