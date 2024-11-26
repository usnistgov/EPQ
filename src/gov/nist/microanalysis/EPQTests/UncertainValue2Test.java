package gov.nist.microanalysis.EPQTests;

import java.text.DecimalFormat;

import gov.nist.microanalysis.Utility.MCUncertaintyEngine;
import gov.nist.microanalysis.Utility.UncertainValue2;
import gov.nist.microanalysis.Utility.UncertainValueMC;

import junit.framework.TestCase;

/**
 * <p>
 * These test UncertainValue2 against UncertainValue and against the output of
 * the program
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
public class UncertainValue2Test extends TestCase {

   private final UncertainValue2 mA = new UncertainValue2(1.24, "A", 0.3);
   private final UncertainValue2 mA2a = makeA2a();

   private final UncertainValue2 mB = new UncertainValue2(8.82, "B", 1.2);
   private final UncertainValue2 mB2a = makeB2a();

   private final UncertainValue2 mC = new UncertainValue2(-9.3, "C", 2.1);
   private final UncertainValue2 mC2a = makeC2a();

   private static final UncertainValue2 makeA2a() {
      final UncertainValue2 res = new UncertainValue2(1.24);
      res.assignComponent("V1", Math.sqrt(0.05));
      res.assignComponent("V2", Math.sqrt(0.04));
      return res;
   }

   private static final UncertainValue2 makeB2a() {
      final UncertainValue2 res = new UncertainValue2(8.82);
      res.assignComponent("V1", Math.sqrt(0.84));
      res.assignComponent("V2", Math.sqrt(0.6));
      return res;
   }

   private static final UncertainValue2 makeC2a() {
      final UncertainValue2 res = new UncertainValue2(-9.3);
      // 2.1 * 2.1 = 4.2 + 0.21 = 4.41
      res.assignComponent("V1", Math.sqrt(3.0));
      res.assignComponent("V3", Math.sqrt(1.41));
      return res;
   }

   private static void assertEquals(UncertainValue2 uv, UncertainValue2 uv2, double delta) {
      assertEquals(uv.doubleValue(), uv2.doubleValue(), delta);
      assertEquals(uv.uncertainty(), uv2.uncertainty(), delta);
   }

   public void testA() {
      assertEquals(mA, mA2a, 1.0e-10);
      assertEquals(mA.uncertainty(), mA2a.uncertainty(), 1.0e-8);
      assertEquals(mA, mA2a, 1.0e-8);
      assertEquals(mA2a.format(new DecimalFormat("0.000")), "1.240±0.300");
      assertEquals(mA2a.formatLong(new DecimalFormat("0.000")), "1.240±0.224(V1)±0.200(V2)");
   }

   public void testB() {
      assertEquals(mB, mB2a, 1.0e-10);
      assertEquals(mB.uncertainty(), mB2a.uncertainty(), 1.0e-8);
      assertEquals(mB, mB2a, 1.0e-8);
      assertEquals(mB2a.format(new DecimalFormat("0.000")), "8.820±1.200");
      assertEquals(mB2a.formatLong(new DecimalFormat("0.000")), "8.820±0.917(V1)±0.775(V2)");
   }

   public void testC() {
      assertEquals(mC, mC2a, 1.0e-10);
      assertEquals(mC.uncertainty(), mC2a.uncertainty(), 1.0e-8);
      assertEquals(mC, mC2a, 1.0e-8);
      assertEquals(mC2a.format(new DecimalFormat("0.000")), "-9.300±2.100");
      assertEquals(mC2a.formatLong(new DecimalFormat("0.000")), "-9.300±1.732(V1)±1.187(V3)");
   }

   public void testAB() {
      assertEquals(UncertainValue2.multiply(2.0, mA), UncertainValue2.multiply(2.0, mA2a), 1.0e-10);
      assertEquals(UncertainValue2.multiply(4.0, mB), UncertainValue2.multiply(4.0, mB2a), 1.0e-10);
      assertEquals(UncertainValue2.multiply(new UncertainValue2(4.0), mB), UncertainValue2.multiply(4.0, mB2a), 1.0e-10);
      assertEquals(UncertainValue2.multiply(4.0, mB), UncertainValue2.multiply(4.0, mB2a), 1.0e-10);
      assertEquals(UncertainValue2.multiply(4.0, mB), UncertainValue2.multiply(new UncertainValue2(4.0), mB2a), 1.0e-10);
      assertEquals(UncertainValue2.multiply(2.0, UncertainValue2.multiply(2.0, mB)), UncertainValue2.multiply(4.0, mB2a), 1.0e-10);
      assertEquals(UncertainValue2.multiply(4.0, mB), UncertainValue2.multiply(new UncertainValue2(4.0), mB2a), 1.0e-10);

      assertEquals(UncertainValue2.multiply(mA, mB), new UncertainValue2(10.9368, 3.035697613), 1.0e-8);

      assertEquals(UncertainValue2.divide(mA, mB), new UncertainValue2(0.1405895692, 0.03902306155), 1.0e-8);

      assertEquals(UncertainValue2.add(mA, mB), new UncertainValue2(10.06, 1.236931688), 1.0e-8);

      assertEquals(UncertainValue2.subtract(mA, mB), new UncertainValue2(-7.58, 1.236931688), 1.0e-8);

      assertEquals(UncertainValue2.divide(1.0, mB), UncertainValue2.invert(mB2a), 1.0e-6);
      assertEquals(UncertainValue2.divide(1.0, mB), UncertainValue2.divide(UncertainValue2.ONE, mB2a), 1.0e-6);

      assertEquals(UncertainValue2.exp(mA), new UncertainValue2(3.455613465, 1.036684039), 1.0e-6);
      assertEquals(UncertainValue2.exp(mA), UncertainValue2.exp(mA2a), 1.0e-6);

      assertEquals(UncertainValue2.log(mA), new UncertainValue2(0.2151113796, 0.2419354839), 1.0e-6);
      assertEquals(UncertainValue2.log(mA), UncertainValue2.log(mA2a), 1.0e-6);

      assertEquals(UncertainValue2.pow(mA, 2.5), new UncertainValue2(1.712198897, 1.035604171), 1.0e-6);
      assertEquals(UncertainValue2.pow(mA, 2.5), UncertainValue2.pow(mA2a, 2.5), 1.0e-6);

      assertEquals(mA.sqrt(), new UncertainValue2(1.113552873, 0.1347039765), 1.0e-6);
      assertEquals(mA.sqrt(), mA2a.sqrt(), 1.0e-6);

      assertEquals(UncertainValue2.atan(mA), new UncertainValue2(0.892133836, 0.118221942), 1.0e-6);
      assertEquals(UncertainValue2.atan(mA), UncertainValue2.atan(mA2a), 1.0e-6);

      assertEquals(UncertainValue2.sqr(mA), new UncertainValue2(1.5376, 0.744), 1.0e-6);
      assertEquals(UncertainValue2.sqr(mA), UncertainValue2.sqr(mA2a), 1.0e-6);
   }

   public void testAdd1() {
      final UncertainValue2 a = new UncertainValue2(1.0, "A", 0.1);
      final UncertainValue2 b = new UncertainValue2(2.0, "A", 0.2);
      assertEquals(UncertainValue2.add(a, b), new UncertainValue2(3.0, 0.3), 0.0001);
      assertEquals(UncertainValue2.add(1.0, a, 1.0, b), new UncertainValue2(3.0, 0.3), 0.0001);
      assertEquals(UncertainValue2.add(2.0, a, 1.0, b), new UncertainValue2(4.0, 0.4), 0.0001);
      assertEquals(UncertainValue2.add(1.0, a, 2.0, b), new UncertainValue2(5.0, 0.5), 0.0001);
      assertEquals(UncertainValue2.add(1.0, a, -1.0, b), new UncertainValue2(-1.0, 0.1), 0.0001);
      assertEquals(UncertainValue2.add(-1.0, a, 1.0, b), new UncertainValue2(1.0, 0.1), 0.0001);
      assertEquals(UncertainValue2.add(-2.0, a, 1.0, b), new UncertainValue2(0.0, 0.0), 0.0001);
      // Example page 22 GUM
      final UncertainValue2[] rsc = new UncertainValue2[]{new UncertainValue2(1000.0, "R", 0.1), new UncertainValue2(1000.0, "R", 0.1),
            new UncertainValue2(1000.0, "R", 0.1), new UncertainValue2(1000.0, "R", 0.1), new UncertainValue2(1000.0, "R", 0.1),
            new UncertainValue2(1000.0, "R", 0.1), new UncertainValue2(1000.0, "R", 0.1), new UncertainValue2(1000.0, "R", 0.1),
            new UncertainValue2(1000.0, "R", 0.1), new UncertainValue2(1000.0, "R", 0.1),};
      assertEquals(UncertainValue2.add(rsc).uncertainty(), 1.0, 1.0e-10);
   }

   public void testAdd2() {
      final UncertainValue2 a = new UncertainValue2(1.0, "A", 0.1);
      final UncertainValue2 b = new UncertainValue2(2.0, "B", 0.2);
      assertEquals(UncertainValue2.add(a, b), new UncertainValue2(3.0, Math.sqrt(0.05)), 0.0001);
      assertEquals(UncertainValue2.add(1.0, a, 1.0, b), new UncertainValue2(3.0, Math.sqrt(0.05)), 0.0001);
      assertEquals(UncertainValue2.add(2.0, a, 1.0, b), new UncertainValue2(4.0, Math.sqrt(0.08)), 0.0001);
      assertEquals(UncertainValue2.add(1.0, a, 2.0, b), new UncertainValue2(5.0, Math.sqrt(0.17)), 0.0001);
      assertEquals(UncertainValue2.add(1.0, a, -1.0, b), new UncertainValue2(-1.0, Math.sqrt(0.05)), 0.0001);
      assertEquals(UncertainValue2.add(-1.0, a, 1.0, b), new UncertainValue2(1.0, Math.sqrt(0.05)), 0.0001);
      assertEquals(UncertainValue2.add(-2.0, a, 1.0, b), new UncertainValue2(0.0, Math.sqrt(0.08)), 0.0001);
      // Example page 22 GUM
      final UncertainValue2[] rsu = new UncertainValue2[]{new UncertainValue2(1000.0, "R0", 0.1), new UncertainValue2(1000.0, "R1", 0.1),
            new UncertainValue2(1000.0, "R2", 0.1), new UncertainValue2(1000.0, "R3", 0.1), new UncertainValue2(1000.0, "R4", 0.1),
            new UncertainValue2(1000.0, "R5", 0.1), new UncertainValue2(1000.0, "R6", 0.1), new UncertainValue2(1000.0, "R7", 0.1),
            new UncertainValue2(1000.0, "R8", 0.1), new UncertainValue2(1000.0, "R9", 0.1),};
      assertEquals(UncertainValue2.add(rsu).uncertainty(), 0.32, 0.005);
   }

   public void testAdd3() {
      final UncertainValue2 a = new UncertainValue2(1.0, "A", 0.1);
      final UncertainValue2 b = new UncertainValue2(2.0, "B", 0.2);
      final UncertainValue2 c = new UncertainValue2(3.0, "A", 0.15);
      final UncertainValue2[] uvs = new UncertainValue2[]{a, b, c};
      assertEquals(UncertainValue2.add(uvs), new UncertainValue2(6.0, Math.sqrt(0.0625 + 0.04)), 1e-6);
      assertEquals(UncertainValue2.add(uvs).formatLong(new DecimalFormat("0.000")), "6.000±0.250(A)±0.200(B)");
   }

   public void testMultiply() {
      final UncertainValue2 a = new UncertainValue2(1.1, "A", 0.1);
      final UncertainValue2 b = new UncertainValue2(2.3, "B", 0.2);
      final UncertainValue2 c = new UncertainValue2(3.6, "A", 0.15);
      assertEquals(UncertainValue2.multiply(a, b), new UncertainValue2(2.53, 0.3182766093), 1.0e-6);
      assertEquals(UncertainValue2.multiply(a, c), new UncertainValue2(3.96, 0.525), 1.0e-6);
      assertEquals(UncertainValue2.multiply(b, UncertainValue2.multiply(a, c)), new UncertainValue2(9.108, 1.444063797), 1.0e-6);
      assertEquals(UncertainValue2.multiply(b, UncertainValue2.multiply(a, c)).formatLong(new DecimalFormat("0.0000")), "9.1080±1.2075(A)±0.7920(B)");
      assertEquals(UncertainValue2.multiply(b, UncertainValue2.multiply(a, c)).format(new DecimalFormat("0.0000")), "9.1080±1.4441");
   }

   public void testDivide() {
      final UncertainValue2 a = new UncertainValue2(1.1, "A", 0.1);
      final UncertainValue2 b = new UncertainValue2(2.3, "B", 0.2);
      final UncertainValue2 c = new UncertainValue2(3.6, "A", 0.15);
      assertEquals(UncertainValue2.divide(b, a), new UncertainValue2(2.090909091, 0.26303852), 1.0e-6);
      assertEquals(UncertainValue2.divide(b, c), new UncertainValue2(0.6388888889, 0.06160408973), 1.0e-6);
      assertEquals(UncertainValue2.divide(a, c), new UncertainValue2(0.3055555556, Math.abs((0.1 / 1.1) - (0.15 / 3.6)) * 0.3055555556), 1.0e-6);
      assertEquals(UncertainValue2.divide(2.0, c), UncertainValue2.divide(new UncertainValue2(2.0), c), 1.0e-6);
      assertEquals(UncertainValue2.divide(-2.0, c), UncertainValue2.divide(new UncertainValue2(-2.0), c), 1.0e-6);
      assertEquals(UncertainValue2.divide(c, 2.0), UncertainValue2.divide(c, new UncertainValue2(2.0)), 1.0e-6);
      assertEquals(UncertainValue2.divide(c, -2.0), UncertainValue2.divide(c, new UncertainValue2(-2.0)), 1.0e-6);
      assertEquals(UncertainValue2.divide(c, -2.0), UncertainValue2.multiply(c, new UncertainValue2(-0.5)), 1.0e-6);
      assertEquals(UncertainValue2.multiply(b, UncertainValue2.invert(a)), UncertainValue2.divide(b, a), 1.0e-7);
      // assertEquals(UncertainValue2.multiply(UncertainValue2.invert(a), c),
      // UncertainValue2.divide(c, a), 1.0e-7);
      // assertEquals(UncertainValue2.multiply(c, UncertainValue2.invert(a)),
      // UncertainValue2.divide(c, a), 1.0e-7);
   }

   public void testFunctions() {
      final UncertainValue2 a = new UncertainValue2(1.1, "A", 0.1);
      final UncertainValue2 b = new UncertainValue2(2.3, "B", 0.2);
      final UncertainValue2 c = new UncertainValue2(-3.6, "A", 0.15);
      assertEquals(UncertainValue2.exp(a), new UncertainValue2(Math.exp(1.1), Math.exp(1.1) * 0.1), 1.0e-8);
      assertEquals(UncertainValue2.exp(b), new UncertainValue2(Math.exp(2.3), Math.exp(2.3) * 0.2), 1.0e-8);
      assertEquals(UncertainValue2.pow(a, 3), UncertainValue2.multiply(UncertainValue2.multiply(a, a), a), 1.0e-8);
      assertEquals(UncertainValue2.sqrt(a), new UncertainValue2(1.048808848, 0.04767312946), 1.0e-8);
      assertEquals(UncertainValue2.pow(a, 0.5), new UncertainValue2(1.048808848, 0.04767312946), 1.0e-8);
      assertEquals(UncertainValue2.pow(a, -0.5), new UncertainValue2(0.9534625894, 0.04333920861), 1.0e-8);
      assertEquals(UncertainValue2.pow(c, 2.0), UncertainValue2.multiply(c, c), 1.0e-8);
      assertEquals(UncertainValue2.atan(a), new UncertainValue2(0.8329812667, 0.04524886878), 1.0e-8);
      assertEquals(UncertainValue2.atan2(b, a), UncertainValue2.atan(UncertainValue2.divide(b, a)), 1.0e-8);
   }

   public void testAgainstMC() {
      final int iterations = 100000;
      // Addition
      {
         UncertainValue2[] args = new UncertainValue2[]{new UncertainValue2(1.1, "A", 0.1), new UncertainValue2(2.3, "B", 0.2),
               new UncertainValue2(-3.6, "C", 0.15)};
         MCUncertaintyEngine mue = new MCUncertaintyEngine(iterations, args) {
            @Override
            public UncertainValueMC compute(UncertainValueMC[] arguments) {
               return UncertainValueMC.add(arguments[0], arguments[1]);
            }
         };
         UncertainValue2 res2 = UncertainValue2.add(args[0], args[1]);
         UncertainValue2 resMC = mue.getResult();
         assertEquals(resMC, res2, Math.abs(5 * res2.doubleValue() / Math.sqrt(iterations)));
      }
      {
         UncertainValue2[] args = new UncertainValue2[]{new UncertainValue2(1.1, "A", 0.1), new UncertainValue2(2.3, "A", 0.2),
               new UncertainValue2(-3.6, "A", 0.15)};
         MCUncertaintyEngine mue = new MCUncertaintyEngine(iterations, args) {
            @Override
            public UncertainValueMC compute(UncertainValueMC[] arguments) {
               return UncertainValueMC.add(arguments[0], arguments[1]);
            }
         };
         UncertainValue2 res2 = UncertainValue2.add(args[0], args[1]);
         UncertainValue2 resMC = mue.getResult();
         assertEquals(resMC, res2, Math.abs(5 * res2.doubleValue() / Math.sqrt(iterations)));
      }
      // Division and multiplication
      {
         UncertainValue2[] args = new UncertainValue2[]{new UncertainValue2(1.1, "A", 0.1), new UncertainValue2(2.3, "B", 0.2),
               new UncertainValue2(-3.6, "A", 0.15)};
         MCUncertaintyEngine mue = new MCUncertaintyEngine(iterations, args) {
            @Override
            public UncertainValueMC compute(UncertainValueMC[] arguments) {
               return UncertainValueMC.multiply(arguments[2],
                     UncertainValueMC.divide(arguments[0], UncertainValueMC.add(arguments[0], arguments[1])));
            }
         };
         UncertainValue2 res2 = UncertainValue2.multiply(args[2], UncertainValue2.divide(args[0], UncertainValue2.add(args[0], args[1])));
         UncertainValue2 resMC = mue.getResult();
         assertEquals(resMC, res2, Math.abs(5 * res2.doubleValue() / Math.sqrt(iterations)));
      }
      {
         UncertainValue2[] args = new UncertainValue2[]{new UncertainValue2(1.1, "A", 0.1), new UncertainValue2(2.3, "A", 0.2),
               new UncertainValue2(-3.6, "A", 0.15)};
         MCUncertaintyEngine mue = new MCUncertaintyEngine(iterations, args) {
            @Override
            public UncertainValueMC compute(UncertainValueMC[] arguments) {
               return UncertainValueMC.multiply(arguments[2],
                     UncertainValueMC.divide(arguments[0], UncertainValueMC.add(arguments[0], arguments[1])));
            }
         };
         UncertainValue2 res2 = UncertainValue2.multiply(args[2], UncertainValue2.divide(args[0], UncertainValue2.add(args[0], args[1])));
         UncertainValue2 resMC = mue.getResult();
         assertEquals(resMC, res2, Math.abs(5 * res2.doubleValue() / Math.sqrt(iterations)));
      }

      {
         UncertainValue2[] args = new UncertainValue2[]{new UncertainValue2(1.1, "A", 0.1), new UncertainValue2(2.3, "B", 0.2),
               new UncertainValue2(-3.6, "C", 0.15)};
         MCUncertaintyEngine mue = new MCUncertaintyEngine(10000, args) {
            @Override
            public UncertainValueMC compute(UncertainValueMC[] arguments) {
               return UncertainValueMC.multiply(arguments[2],
                     UncertainValueMC.divide(arguments[0], UncertainValueMC.add(arguments[0], arguments[1])));
            }
         };
         UncertainValue2 res2 = UncertainValue2.multiply(args[2], UncertainValue2.divide(args[0], UncertainValue2.add(args[0], args[1])));
         UncertainValue2 resMC = mue.getResult();
         assertEquals(resMC, res2, Math.abs(5 * res2.doubleValue() / Math.sqrt(iterations)));
      }
      {
         UncertainValue2[] args = new UncertainValue2[]{new UncertainValue2(1.1, "A", 0.1), new UncertainValue2(2.3, "B", 0.2),
               new UncertainValue2(-3.6, "C", 0.15)};
         MCUncertaintyEngine mue = new MCUncertaintyEngine(10000, args) {
            @Override
            public UncertainValueMC compute(UncertainValueMC[] arguments) {
               return UncertainValueMC.multiply(arguments[2], UncertainValueMC.divide(UncertainValueMC.log(arguments[0]),
                     UncertainValueMC.add(arguments[0], UncertainValueMC.sqrt(arguments[1]))));
            }
         };
         UncertainValue2 res2 = UncertainValue2.multiply(args[2],
               UncertainValue2.divide(UncertainValue2.log(args[0]), UncertainValue2.add(args[0], UncertainValue2.sqrt(args[1]))));
         UncertainValue2 resMC = mue.getResult();
         assertEquals(resMC, res2, 0.03);
      }
      {
         UncertainValue2[] args = new UncertainValue2[]{new UncertainValue2(1.1, "A", 0.1), new UncertainValue2(2.3, "B", 0.2),
               new UncertainValue2(-3.6, "C", 0.15)};
         MCUncertaintyEngine mue = new MCUncertaintyEngine(10000, args) {
            @Override
            public UncertainValueMC compute(UncertainValueMC[] arguments) {
               return UncertainValueMC.mean(arguments);
            }
         };
         UncertainValue2 res2 = UncertainValue2.mean(args);
         UncertainValue2 resMC = mue.getResult();
         assertEquals(resMC, res2, Math.abs(10 * res2.doubleValue() / Math.sqrt(iterations)));
      }
      {
         UncertainValue2[] args = new UncertainValue2[]{new UncertainValue2(1.1, "A", 0.1), new UncertainValue2(2.3, "A", 0.2),
               new UncertainValue2(-3.6, "A", 0.15)};
         MCUncertaintyEngine mue = new MCUncertaintyEngine(10000, args) {
            @Override
            public UncertainValueMC compute(UncertainValueMC[] arguments) {
               return UncertainValueMC.mean(arguments);
            }
         };
         UncertainValue2 res2 = UncertainValue2.mean(args);
         UncertainValue2 resMC = mue.getResult();
         assertEquals(resMC, res2, 0.01);
      }

   }
};