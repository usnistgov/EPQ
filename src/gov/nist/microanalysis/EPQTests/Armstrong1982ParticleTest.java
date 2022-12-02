package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.AlgorithmUser;
import gov.nist.microanalysis.EPQLibrary.Armstrong1982ParticleCorrection;
import gov.nist.microanalysis.EPQLibrary.Armstrong1982ParticleMC;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FromSI;
import gov.nist.microanalysis.EPQLibrary.MassAbsorptionCoefficient;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.SampleShape;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.Strategy;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.Utility.DescriptiveStatistics;
import junit.framework.TestCase;

public class Armstrong1982ParticleTest extends TestCase {

   public void testBulk() throws EPQException {
      performTestOnShape(new SampleShape.Bulk());
   }

   public void testSimpleBigRRP() throws EPQException {
      performTestOnShape(new SampleShape.RightRectangularPrism(10.0e-6, 10.0e-6, 10.0e-6));
   }

   public void testRRP() throws EPQException {
      performTestOnShape(new SampleShape.RightRectangularPrism(1.0e-6, 1.0e-6, 1.0e-6));
   }

   public void testTetragonalPrism() throws EPQException {
      performTestOnShape(new SampleShape.TetragonalPrism(1.0e-6, 1.0e-6));
   }

   public void testTriangularPrism() throws EPQException {
      performTestOnShape(new SampleShape.TriangularPrism(1.0e-6, 1.0e-6));
   }

   public void testSquarePyramid() throws EPQException {
      performTestOnShape(new SampleShape.SquarePyramid(1.0e-6));
   }

   public void testVerticalCylinder() throws EPQException {
      performTestOnShape(new SampleShape.Cylinder(0.5e-6, 1.0e-6));
   }

   public void testFiber() throws EPQException {
      performTestOnShape(new SampleShape.Fiber(0.5e-6, 1.0e-6));
   }

   public void testSphere() throws EPQException {
      performTestOnShape(new SampleShape.Sphere(1.0e-6));
   }

   public void testHemisphere() throws EPQException {
      performTestOnShape(new SampleShape.Hemisphere(1.0e-6));

   }

   private double calculatePC(Composition mat, XRayTransition xrt, SampleShape ss) throws EPQException {
      final Armstrong1982ParticleCorrection alg = new Armstrong1982ParticleCorrection();
      final SpectrumProperties sp = new SpectrumProperties();
      sp.setNumericProperty(SpectrumProperties.BeamEnergy, 20.0);
      sp.setNumericProperty(SpectrumProperties.TakeOffAngle, 40.0);
      sp.setSampleShape(SpectrumProperties.SampleShape, ss);
      sp.setNumericProperty(SpectrumProperties.SpecimenDensity, ToSI.gPerCC(2.61));
      alg.initialize(mat, xrt.getDestination(), sp);
      return alg.particleAbsorptionCorrection(xrt);
   }

   private double calculateRRP(Composition mat, XRayTransition xrt, double size) throws EPQException {
      final SampleShape ss = new SampleShape.RightRectangularPrism(size, size, size);
      return calculatePC(mat, xrt, ss);
   }

   public void testStorm() throws EPQException {
      final Composition mat = MaterialFactory.createCompound("NaAlSi3O8", 3.0);
      {
         final XRayTransition xrt = new XRayTransition(Element.Na, XRayTransition.KA1);
         assertEquals(calculateRRP(mat, xrt, 0.3e-6), 0.098539, 0.000001);
         assertEquals(calculateRRP(mat, xrt, 0.6e-6), 0.203473, 0.000001);
         assertEquals(calculateRRP(mat, xrt, 0.9e-6), 0.298388, 0.000001);
         assertEquals(calculateRRP(mat, xrt, 1.2e-6), 0.374797, 0.000001);
         assertEquals(calculateRRP(mat, xrt, 1.5e-6), 0.430291, 0.000001);
      }
      {
         final XRayTransition xrt = new XRayTransition(Element.Si, XRayTransition.KA1);
         assertEquals(calculateRRP(mat, xrt, 0.3e-6), 0.110491, 0.000001);
         assertEquals(calculateRRP(mat, xrt, 0.6e-6), 0.23852, 0.000001);
         assertEquals(calculateRRP(mat, xrt, 0.9e-6), 0.363711, 0.000001);
         assertEquals(calculateRRP(mat, xrt, 1.2e-6), 0.47275, 0.000001);
         assertEquals(calculateRRP(mat, xrt, 1.5e-6), 0.559131, 0.000001);
      }
      {
         final XRayTransition xrt = new XRayTransition(Element.Na, XRayTransition.KA1);
         assertEquals(calculatePC(mat, xrt, new SampleShape.SquarePyramid(0.3e-6)), 0.017228, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.SquarePyramid(0.6e-6)), 0.036931, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.SquarePyramid(0.9e-6)), 0.057910, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.SquarePyramid(1.2e-6)), 0.079111, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.SquarePyramid(1.5e-6)), 0.099662, 0.000001);
      }
      {
         final XRayTransition xrt = new XRayTransition(Element.Si, XRayTransition.KA1);
         assertEquals(calculatePC(mat, xrt, new SampleShape.SquarePyramid(0.3e-6)), 0.015843, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.SquarePyramid(0.6e-6)), 0.033326, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.SquarePyramid(0.9e-6)), 0.051339, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.SquarePyramid(1.2e-6)), 0.068976, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.SquarePyramid(1.5e-6)), 0.085548, 0.000001);
      }
      {
         final XRayTransition xrt = new XRayTransition(Element.Na, XRayTransition.KA1);
         assertEquals(calculatePC(mat, xrt, new SampleShape.TetragonalPrism(0.3e-6, 0.3e-6)), 0.082839, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.TetragonalPrism(0.6e-6, 0.6e-6)), 0.174996, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.TetragonalPrism(0.9e-6, 0.9e-6)), 0.265272, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.TetragonalPrism(1.2e-6, 1.2e-6)), 0.346112, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.TetragonalPrism(1.5e-6, 1.5e-6)), 0.413296, 0.000001);
      }
      {
         final XRayTransition xrt = new XRayTransition(Element.Si, XRayTransition.KA1);
         assertEquals(calculatePC(mat, xrt, new SampleShape.TetragonalPrism(0.3e-6, 0.3e-6)), 0.091464, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.TetragonalPrism(0.6e-6, 0.6e-6)), 0.199251, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.TetragonalPrism(0.9e-6, 0.9e-6)), 0.310363, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.TetragonalPrism(1.2e-6, 1.2e-6)), 0.414895, 0.000001);
         assertEquals(calculatePC(mat, xrt, new SampleShape.TetragonalPrism(1.5e-6, 1.5e-6)), 0.506334, 0.000001);
      }
   }

   public void performTestOnShape(SampleShape sh) throws EPQException {
      System.out.println(sh);
      final Material mat = (Material) MaterialFactory.createMaterial("K411");
      final double e0 = ToSI.keV(25.0);
      final EDSDetector det = EDSDetector.createSiLiDetector(2048, 10.0, 135.0);
      final XRayTransition[] xrts = new XRayTransition[]{new XRayTransition(Element.O, XRayTransition.KA1),
            new XRayTransition(Element.Si, XRayTransition.KA1), new XRayTransition(Element.Ca, XRayTransition.KA1),
            new XRayTransition(Element.Mg, XRayTransition.KA1), new XRayTransition(Element.Fe, XRayTransition.KA1),};
      final Strategy strat = new Strategy();
      strat.addAlgorithm(MassAbsorptionCoefficient.class, MassAbsorptionCoefficient.Default);
      AlgorithmUser.applyGlobalOverride(strat);

      for (final XRayTransition xrt : xrts) {
         final SpectrumProperties sp = new SpectrumProperties();
         sp.setNumericProperty(SpectrumProperties.BeamEnergy, FromSI.keV(e0));
         sp.setNumericProperty(SpectrumProperties.SpecimenDensity, 3.0);
         sp.setDetector(det);
         sp.setSampleShape(SpectrumProperties.SampleShape, sh);
         testTransition(mat, xrt, sp);
      }
   }

   private void testTransition(final Material mat, final XRayTransition xrt, final SpectrumProperties sp) throws EPQException {
      final DescriptiveStatistics ds = new DescriptiveStatistics();
      final int N_TESTS = 10;
      for (int i = 0; i < N_TESTS; ++i) {
         final Armstrong1982ParticleMC c0 = new Armstrong1982ParticleMC();
         c0.initialize(mat, xrt.getDestination(), sp);
         ds.add(c0.computeZACorrection(xrt) / c0.computeZ(mat, xrt, sp));
      }
      final Armstrong1982ParticleCorrection c1 = new Armstrong1982ParticleCorrection();
      c1.initialize(mat, xrt.getDestination(), sp);
      final double za1 = c1.computeZACorrection(xrt) / c1.computeZ(mat, xrt, sp);
      (Math.abs(ds.average() - za1) > (5.0 * ds.standardDeviation()) ? System.err : System.out).println(xrt);
      assertEquals(ds.average(), za1, 5.0 * ds.standardDeviation());
   }
}
