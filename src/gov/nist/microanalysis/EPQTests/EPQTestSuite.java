package gov.nist.microanalysis.EPQTests;

import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * <p>
 * A framework class for executing all tests associated with the EPQ library.
 * This test suite is far from complete. In most cases, the suite does little
 * more than test a couple of different examples. Often the tests compare one
 * algorithm to another without comparing against an independent calculation of
 * the same quantity.
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
public class EPQTestSuite
   extends TestSuite {

   public EPQTestSuite() {
      super();
      // gov.nist.microanalysis.Utility
      addTest(new TestSuite(AdaptiveRungeKuttaTest.class));
      addTest(new TestSuite(DescriptiveStatisticsTest.class));
      addTest(new TestSuite(FindRootTest.class));
      addTest(new TestSuite(Math2Test.class));
      addTest(new TestSuite(PoissonDeviateTest.class));
      addTest(new TestSuite(LinearRegressionTest.class));
      addTest(new TestSuite(HistogramTest.class));
      addTest(new TestSuite(MemberSetTest.class));
      // gov.nist.microanalysis.EPQLibrary
      addTest(new TestSuite(AtomicShellTest.class));
      addTest(new TestSuite(BackscatterCoefficientTest.class));
      addTest(new TestSuite(BackscatterFactorTest.class));
      addTest(new TestSuite(BetheElectronEnergyLossTest.class));
      addTest(new TestSuite(CompositionFromKRatiosTest.class));
      addTest(new TestSuite(ComputeZAFTest.class));
      addTest(new TestSuite(CorrectionAlgorithmTest.class));
      addTest(new TestSuite(EdgeEnergyTest.class));
      addTest(new TestSuite(ElectronRangeTest.class));
      addTest(new TestSuite(ElementTest.class));
      addTest(new TestSuite(FilterFitTest.class));
      addTest(new TestSuite(FluorescenceTest.class));
      addTest(new TestSuite(IonizationCrossSectionTest.class));
      addTest(new TestSuite(MassAbsorptionCoefficientTest.class));
      addTest(new TestSuite(MaterialTest.class));
      addTest(new TestSuite(MaterialFactoryTest.class));
      addTest(new TestSuite(MeanIonizationPotentialTest.class));
      addTest(new TestSuite(StoppingPowerTest.class));
      addTest(new TestSuite(StrategyTest.class));
      addTest(new TestSuite(SurfaceIonizationTest.class));
      addTest(new TestSuite(TransitionEnergyTest.class));
      addTest(new TestSuite(XRayTransitionSetTest.class));
      addTest(new TestSuite(XRayTransitionTest.class));
      // gov.nist.microanalysis.NISTMonte
      addTest(new TestSuite(CylindricalShapeTest.class));
      addTest(new TestSuite(MonteCarloSSTest.class));
      addTest(new TestSuite(SphereTest.class));
      addTest(new TestSuite(SumShapeTest.class));
      // gov.nist.microanalysis.EPQTools
      addTest(new TestSuite(SerializableSpectrumTest.class));
   }

   public void testOne() {
      final TestResult tr = new TestResult();
      run(tr);
   }

   public static void main(String[] args) {
      final EPQTestSuite suite = new EPQTestSuite();
      System.out.println(suite.testCount());
      // TestRunner.run(EPQTestSuite.class);
   }
}
