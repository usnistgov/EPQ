package gov.nist.microanalysis.EPQTests;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQLibrary.AtomicShell;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
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
public class XRayTransitionTest
   extends TestCase {
   public XRayTransitionTest(String test) {
      super(test);
   }

   public void testOne()
         throws Exception {
      {
         final XRayTransition xrt = new XRayTransition(new AtomicShell(Element.Au, AtomicShell.LIII), AtomicShell.MV);
         xrt.getEnergy();
         xrt.getWeight(XRayTransition.NormalizeDefault);
         xrt.getWeight(XRayTransition.NormalizeDestination);
         xrt.getWeight(XRayTransition.NormalizeFamily);
      }
      if(false) {
         boolean missing;
         final PrintStream ps = new PrintStream(new FileOutputStream("c:/temp/extra.txt"), true, "US-ASCII");
         final NumberFormat nf = NumberFormat.getInstance();
         nf.setMaximumFractionDigits(3);
         for(int el = Element.elmH; el < Element.elmU; ++el) {
            missing = false;
            for(int tr = XRayTransition.KA1; tr < XRayTransition.Last; ++tr) {
               final XRayTransition xrt = new XRayTransition(Element.byAtomicNumber(el), tr);
               if(xrt.exists()) {
                  if(xrt.getDestination().getGroundStateOccupancy() == 0) {
                     final String msg = xrt.getDestination().getAtomicName() + " unavailable for " + xrt + " at "
                           + nf.format(FromSI.keV(xrt.getEnergy())) + " keV ("
                           + Double.toString(xrt.getWeight(XRayTransition.NormalizeDefault)) + ")";
                     ps.println(msg);
                  }
                  if(xrt.getSource().getGroundStateOccupancy() == 0) {
                     final String msg = xrt.getSource().getAtomicName() + " unavailable for " + xrt + " at "
                           + nf.format(FromSI.keV(xrt.getEnergy())) + " keV ("
                           + Double.toString(xrt.getWeight(XRayTransition.NormalizeDefault)) + ")";
                     System.err.println(msg);
                     ps.println(msg);
                     missing = true;
                  }
               }
            }
         }
      }
   }

   public void testTwo() {
      // It is important that XRayTransition objects are sorted by destination
      // shell then source shell to optimize calculation in ComputeZAF.
      final TreeSet<XRayTransition> ts = new TreeSet<XRayTransition>();
      for(int i = XRayTransition.KA1; i < XRayTransition.Last; ++i)
         if(XRayTransition.exists(Element.U, i))
            ts.add(new XRayTransition(Element.U, i));
      final Iterator<XRayTransition> j = ts.iterator();
      XRayTransition prev = j.next();
      while(j.hasNext()) {
         final XRayTransition xrt = j.next();
         if(xrt.getDestinationShell() == prev.getDestinationShell())
            assertTrue(xrt.getSourceShell() > prev.getSourceShell());
         else
            assertTrue(xrt.getDestinationShell() > prev.getDestinationShell());
         prev = xrt;
      }
   }
}
