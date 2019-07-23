package gov.nist.microanalysis.EPQTests;

import java.util.List;

import gov.nist.microanalysis.EPQLibrary.AlgorithmClass;
import gov.nist.microanalysis.EPQLibrary.ElectronRange;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.ToSI;

import junit.framework.TestCase;

/**
 * <p>
 * Tests the ElectronRange class.
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
public class ElectronRangeTest
   extends
   TestCase {

   /**
    * Constructs a ElectronRangeTest
    * 
    * @param arg0
    */
   public ElectronRangeTest(String arg0) {
      super(arg0);
   }

   /**
    * testOne - Implements an internal self consistency test for the various
    * different ElectronRange implementations.
    */
   public void testOne() {
      final List<AlgorithmClass> all = ElectronRange.Default.getAllImplementations();
      final Material mat = (Material) MaterialFactory.createMaterial(MaterialFactory.K3189);
      final double[] e0s = {
         ToSI.keV(2.0),
         ToSI.keV(4.0),
         ToSI.keV(8.0),
         ToSI.keV(16.0),
         ToSI.keV(32.0)
      };
      final double[] errs = {
         0.5,
         0.2,
         0.2,
         0.2,
         0.2
      };
      for(final AlgorithmClass ac : all) {
         final ElectronRange impl = (ElectronRange) ac;
         for(int j = 0; j < e0s.length; ++j) {
            final double e0 = e0s[j];
            final double val = FromSI.MICROMETER * impl.computeMeters(mat, e0);
            final double def = FromSI.MICROMETER * ElectronRange.Default.computeMeters(mat, e0);
            if(false) {
               System.out.print(mat);
               System.out.print("\t");
               System.out.print(FromSI.keV(e0));
               System.out.print(" keV \t");
               System.out.print(val);
               System.out.print("\t");
               System.out.print(def);
               System.out.println();
            }
            assertEquals(val, def, def * errs[j]);
         }
      }
   }
}
