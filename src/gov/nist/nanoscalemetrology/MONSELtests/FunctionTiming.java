/**
 *
 */
package gov.nist.nanoscalemetrology.MONSELtests;

/**
 * @author jvillar
 */
public class FunctionTiming {

   public static double timetest(long repeats) {
      long t0; // Start time
      long tint, tf;
      double deltat = 0.;

      // Repeatedly execute the routine
      @SuppressWarnings("unused")
      double result1;

      t0 = System.currentTimeMillis();

      final double inc = 0.2;

      for (long i = 0; i < repeats; i++)
         for (int j = 0; j < 10; j++) {
            final double testval = -0.99 + (j * inc);
            result1 = Math.asin(testval);
         }

      tint = System.currentTimeMillis();
      for (long i = 0; i < repeats; i++)
         for (int j = 0; j < 10; j++) {
            final double testval = -0.99 + (j * inc);
            result1 = testval;
         }

      tf = System.currentTimeMillis();
      deltat = (1000. * ((2. * tint) - tf - t0)) / repeats / 10.;
      return deltat;

   }

}

/*
 * In a test on 12/3/2009, Math2.rgen.nextDouble() required 133 ns/call.
 * Math.sin(x) required 121 ns. Math.log(x) required 63 ns. Math.asin(x)
 * required 480 ns. These benchmarks were run on Copernicus, using a single 3.4
 * Ghz Xeon processor.
 */
