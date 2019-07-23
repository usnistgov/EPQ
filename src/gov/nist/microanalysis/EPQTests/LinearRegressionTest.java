package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.Utility.LinearRegression;
import junit.framework.TestCase;

/**
 * <p>
 * Tests the LinearRegression class.
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
public class LinearRegressionTest
   extends TestCase {

   /**
    * Constructs a LinearRegressionTest
    * 
    * @param arg0
    */
   public LinearRegressionTest(String arg0) {
      super(arg0);
   }

   public void testOne() {
      // From the NIST standard statistical test data set
      final double[] x = {
         0.2,
         337.4,
         118.2,
         884.6,
         10.1,
         226.5,
         666.3,
         996.3,
         448.6,
         777.0,
         558.2,
         0.4,
         0.6,
         775.5,
         666.9,
         338.0,
         447.5,
         11.6,
         556.0,
         228.1,
         995.8,
         887.6,
         120.2,
         0.3,
         0.3,
         556.8,
         339.1,
         887.2,
         999.0,
         779.0,
         11.1,
         118.3,
         229.2,
         669.1,
         448.9,
         0.5
      };
      final double[] y = {
         0.1,
         338.8,
         118.1,
         888.0,
         9.2,
         228.1,
         668.5,
         998.5,
         449.1,
         778.9,
         559.2,
         0.3,
         0.1,
         778.1,
         668.8,
         339.3,
         448.9,
         10.8,
         557.7,
         228.3,
         998.0,
         888.8,
         119.6,
         0.3,
         0.6,
         557.6,
         339.3,
         888.0,
         998.5,
         778.9,
         10.2,
         117.6,
         228.9,
         668.4,
         449.2,
         0.2
      };
      final LinearRegression lr = new LinearRegression();
      lr.setData(x, y);
      assertEquals(1.00211681802045, lr.getSlope(), 1.0e-8);
      assertEquals(-0.262323073774029, lr.getIntercept(), 1.0e-8);
      assertEquals(0.999993745883712, lr.getRSquared(), 1.0e-8);
   }

   public void testTwo() {
      final LinearRegression lr = new LinearRegression();
      final double[] x0 = {
         60,
         61,
         62,
         63,
         64,
         65,
         66,
         67,
         68,
         69,
         70
      };
      final double[] y0 = {
         130.4072695,
         131.6557207,
         132.1818598,
         133.4623859,
         134.5240944,
         135.6892300,
         136.4627827,
         137.8755760,
         138.3153478,
         139.8264107,
         140.7076632
      };
      lr.setData(x0, y0);
      assertEquals(lr.getSlope(), 1.030457, 1.0e-5);
      assertEquals(lr.getIntercept(), 68.57560, 1.0e-4);
      assertEquals(lr.getRSquared(), 0.996907, 1.0e-5);
      // Test reusing an lr object
      // From the NIST standard statistical test data set
      final double[] x = {
         0.2,
         337.4,
         118.2,
         884.6,
         10.1,
         226.5,
         666.3,
         996.3,
         448.6,
         777.0,
         558.2,
         0.4,
         0.6,
         775.5,
         666.9,
         338.0,
         447.5,
         11.6,
         556.0,
         228.1,
         995.8,
         887.6,
         120.2,
         0.3,
         0.3,
         556.8,
         339.1,
         887.2,
         999.0,
         779.0,
         11.1,
         118.3,
         229.2,
         669.1,
         448.9,
         0.5
      };
      final double[] y = {
         0.1,
         338.8,
         118.1,
         888.0,
         9.2,
         228.1,
         668.5,
         998.5,
         449.1,
         778.9,
         559.2,
         0.3,
         0.1,
         778.1,
         668.8,
         339.3,
         448.9,
         10.8,
         557.7,
         228.3,
         998.0,
         888.8,
         119.6,
         0.3,
         0.6,
         557.6,
         339.3,
         888.0,
         998.5,
         778.9,
         10.2,
         117.6,
         228.9,
         668.4,
         449.2,
         0.2
      };
      lr.clear();
      lr.addData(x, y);
      assertEquals(1.00211681802045, lr.getSlope(), 1.0e-8);
      assertEquals(-0.262323073774029, lr.getIntercept(), 1.0e-8);
      assertEquals(0.999993745883712, lr.getRSquared(), 1.0e-8);
   }

}
