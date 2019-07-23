package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.Utility.DescriptiveStatistics;
import gov.nist.microanalysis.Utility.PoissonDeviate;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the PoissonDeviate class.
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
public class PoissonDeviateTest
   extends TestCase {
   public PoissonDeviateTest(String test) {
      super(test);
   }

   public void testOne() {
      final int[] na = {
         (int) ((5 * Math.random()) + 6),
         (int) ((50 * Math.random()) + 13)
      };
      for(final int n : na) {
         final PoissonDeviate pd = new PoissonDeviate(100);
         final DescriptiveStatistics ds = new DescriptiveStatistics();
         for(int i = 0; i < 1000000; ++i)
            ds.add(pd.randomDeviate(n));
         assertEquals(ds.average(), n, 0.04);
         assertEquals(ds.variance(), n, 0.1);
         assertEquals(ds.skewness(), 1.0 / Math.sqrt(n), 0.1);
         assertEquals(ds.kurtosis(), 1.0 / n, 0.1);
      }
   }
}