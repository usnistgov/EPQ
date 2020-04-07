/**
 * gov.nist.microanalysis.EPQTests.LinearLeastSquaresTest Created by: nritchie
 * Date: Sep 17, 2012
 */
package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.Utility.LinearLeastSquares;
import gov.nist.microanalysis.Utility.UncertainValue2;

import java.io.PrintWriter;
import java.text.DecimalFormat;

import junit.framework.TestCase;
import Jama.Matrix;

/**
 * <p>
 * Description
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
public class LinearLeastSquaresTest
   extends TestCase {

   public void testAster2_1()
         throws EPQException {
      final double[] x = new double[] {
         1.0000,
         2.0000,
         3.0000,
         4.0000,
         5.0000,
         6.0000,
         7.0000,
         8.0000,
         9.0000,
         10.0000
      };
      final double[] y = new double[] {
         109.3827,
         187.5385,
         267.5319,
         331.8753,
         386.0535,
         428.4271,
         452.1644,
         498.1461,
         512.3499,
         512.9753
      };
      final double[] dy = new double[] {
         8.0000,
         8.0000,
         8.0000,
         8.0000,
         8.0000,
         8.0000,
         8.0000,
         8.0000,
         8.0000,
         8.0000
      };

      final LinearLeastSquares lls = new LinearLeastSquares(x, y, dy) {

         @Override
         protected int fitFunctionCount() {
            return 3;
         }

         @Override
         protected void fitFunction(double xi, double[] afunc) {
            afunc[0] = 1.0;
            afunc[1] = xi;
            afunc[2] = -0.5 * xi * xi;
         }
      };
      final UncertainValue2[] res = lls.getResults();
      final DecimalFormat df = new DecimalFormat("0.00000");
      for(final UncertainValue2 r : res)
         System.out.println(r.formatLong(df));
      final double[] cis = lls.confidenceIntervals(LinearLeastSquares.INTERVAL_MODE.JOINT_INTERVAL, 0.95, lls.covariance());
      for(final double ci : cis)
         System.out.println(df.format(ci));
      System.out.println("ChiSqr = " + lls.chiSquared());
      final Matrix m = lls.covariance();
      final PrintWriter pw = new PrintWriter(System.out);
      m.print(pw, new DecimalFormat("0.00000"), 10);
      pw.flush();

   }

}
