package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.FilterFit;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.KRatioSet;
import gov.nist.microanalysis.EPQLibrary.Material;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.NoisySpectrum;
import gov.nist.microanalysis.EPQLibrary.SpectrumMath;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.XRayTransitionSet;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.EPQTools.ASPEXSpectrum;
import gov.nist.microanalysis.EPQTools.EMSAFile;
import gov.nist.microanalysis.Utility.DescriptiveStatistics;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import junit.framework.TestCase;

/**
 * <p>
 * Tests for the FilterFit class
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
public class FilterFitTest
   extends TestCase {

   private ISpectrumData mAuSpec;
   private ISpectrumData mAgSpec;

   /**
    * Constructs a SpectrumFitterTest object
    * 
    * @param arg0
    */
   public FilterFitTest(String arg0) {
      super(arg0);
   }

   @Override
   protected void setUp()
         throws EPQException, URISyntaxException {
      mAuSpec = new ASPEXSpectrum(new File(FilterFitTest.class.getResource("TestData/Gold.tif").toURI()));
      mAuSpec.getProperties().setCompositionProperty(SpectrumProperties.StandardComposition, MaterialFactory.createPureElement(Element.Au));
      mAgSpec = new ASPEXSpectrum(new File(FilterFitTest.class.getResource("TestData/Silver.tif").toURI()));
      mAgSpec.getProperties().setCompositionProperty(SpectrumProperties.StandardComposition, MaterialFactory.createPureElement(Element.Ag));
   }

   @Override
   protected void tearDown() {
      mAuSpec = null;
      mAgSpec = null;
   }

   public void testOne()
         throws EPQException {
      // Load the reference spectra...
      // Construct an unknown from the references...
      final SpectrumMath unk = new SpectrumMath(mAuSpec);
      unk.add(mAgSpec, 1.0);
      final EDSDetector det = EDSDetector.createSDDDetector(unk.getChannelCount(), unk.getChannelWidth(), SpectrumUtils.getFWHMAtMnKA(unk, 135.0));
      final FilterFit ff = new FilterFit(det, ToSI.eV(SpectrumUtils.getBeamEnergy(unk)));
      ff.addReference(Element.Au, mAuSpec);
      ff.addReference(Element.Ag, mAgSpec);
      final KRatioSet krs = ff.getKRatios(unk);
      System.out.println(krs.toString());
      assertEquals(krs.getKRatio(krs.optimalDatum(Element.Au)), 0.5, 0.01);
      assertEquals(krs.getKRatio(krs.optimalDatum(Element.Ag)), 0.5, 0.01);
   }

   public void testTwo()
         throws EPQException {
      final SpectrumMath unk = new SpectrumMath(mAuSpec);
      unk.add(mAgSpec, 0.5);
      final EDSDetector det = EDSDetector.createSiLiDetector(unk.getChannelCount(), unk.getChannelWidth(), SpectrumUtils.getFWHMAtMnKA(unk, 135.0));
      final FilterFit ff = new FilterFit(det, ToSI.eV(SpectrumUtils.getBeamEnergy(unk)));
      ff.addReference(Element.Au, mAuSpec);
      ff.addReference(Element.Ag, mAgSpec);
      final KRatioSet krs = ff.getKRatios(unk);
      System.out.println(krs.toString());
      assertEquals(krs.getKRatio(krs.optimalDatum(Element.Au)), 0.666, 0.01);
      assertEquals(krs.getKRatio(krs.optimalDatum(Element.Ag)), 0.333, 0.01);
   }

   public void testThree()
         throws EPQException {
      final SpectrumMath unk = new SpectrumMath(mAgSpec);
      unk.add(mAuSpec, 0.5);
      final EDSDetector det = EDSDetector.createSiLiDetector(unk.getChannelCount(), unk.getChannelWidth(), SpectrumUtils.getFWHMAtMnKA(unk, 135.0));
      final FilterFit ff = new FilterFit(det, ToSI.eV(SpectrumUtils.getBeamEnergy(unk)));
      ff.addReference(Element.Au, mAuSpec);
      ff.addReference(Element.Ag, mAgSpec);
      final KRatioSet krs = ff.getKRatios(unk);
      System.out.println(krs.toString());
      assertEquals(krs.getKRatio(krs.optimalDatum(Element.Au)), 0.333, 0.01);
      assertEquals(krs.getKRatio(krs.optimalDatum(Element.Ag)), 0.666, 0.01);
   }

   public void testFour()
         throws EPQException, IOException {
      // 'Unknown' spectrum
      final EMSAFile k3189 = new EMSAFile();
      k3189.read(FilterFitTest.class.getResourceAsStream("TestData/K3189_1.msa"));
      k3189.getProperties().setNumericProperty(SpectrumProperties.FaradayBegin, 1.0);
      k3189.getProperties().setNumericProperty(SpectrumProperties.LiveTime, 60.0);
      final Object[][] refs = {
         {
            Element.O,
            "TestData/MgO_ref1.msa",
            MaterialFactory.createMaterial(MaterialFactory.MagnesiumOxide),
            null
         },
         {
            Element.Mg,
            "TestData/Mg_ref1.msa",
            MaterialFactory.createPureElement(Element.Mg),
            null
         },
         {
            Element.Al,
            "TestData/Al_ref1.msa",
            MaterialFactory.createPureElement(Element.Al),
            null
         },
         {
            Element.Si,
            "TestData/Si_ref1.msa",
            MaterialFactory.createPureElement(Element.Si),
            null
         },
         {
            Element.Ca,
            "TestData/Ca_ref1.msa",
            MaterialFactory.createPureElement(Element.Ca),
            null
         },
         {
            Element.Ti,
            "TestData/Ti_ref1.msa",
            MaterialFactory.createPureElement(Element.Ti),
            null
         },
         {
            Element.Fe,
            "TestData/Fe_ref1.msa",
            MaterialFactory.createPureElement(Element.Fe),
            null
         }
      };

      for(int i = 0; i < refs.length; ++i) {
         final EMSAFile ref = new EMSAFile();
         ref.read(FilterFitTest.class.getResourceAsStream((String) refs[i][1]));
         ref.getProperties().setNumericProperty(SpectrumProperties.FaradayBegin, 1.0);
         ref.getProperties().setNumericProperty(SpectrumProperties.LiveTime, 60.0);
         refs[i][3] = ref;
      }

      final DescriptiveStatistics[] rs = new DescriptiveStatistics[refs.length];
      final DescriptiveStatistics[] var = new DescriptiveStatistics[refs.length];
      for(int m = 0; m < refs.length; ++m) {
         rs[m] = new DescriptiveStatistics();
         var[m] = new DescriptiveStatistics();
      }

      final EDSDetector det = EDSDetector.createSiLiDetector(k3189.getChannelCount(), k3189.getChannelWidth(), SpectrumUtils.getFWHMAtMnKA(k3189, 135.0));
      for(int j = 0; j < 10; ++j) {
         final ISpectrumData unk = new NoisySpectrum(k3189, 1.0, (int) (Integer.MAX_VALUE * Math.random()));
         final FilterFit ff = new FilterFit(det, ToSI.eV(SpectrumUtils.getBeamEnergy(unk)));
         for(final Object[] ref2 : refs) {
            final ISpectrumData ref = SpectrumUtils.copy((ISpectrumData) ref2[3]);
            ref.getProperties().setCompositionProperty(SpectrumProperties.StandardComposition, (Material) ref2[2]);
            ff.addReference((Element) ref2[0], ref);
         }
         // Dump fit parameters...
         final KRatioSet krs = ff.getKRatios(unk);
         for(int m = 0; m < refs.length; ++m) {
            final XRayTransitionSet xrts = krs.optimalDatum((Element) refs[m][0]);
            rs[m].add(krs.getKRatio(xrts));
            var[m].add(krs.getError(xrts));
         }
      }
      assertEquals(rs[0].average(), 0.3274, 0.01);
      assertEquals(rs[1].average(), 0.0223, 0.01);
      assertEquals(rs[2].average(), 0.0390, 0.01);
      assertEquals(rs[3].average(), 0.1174, 0.01);
      assertEquals(rs[4].average(), 0.1337, 0.01);
      assertEquals(rs[5].average(), 0.0185, 0.01);
      assertEquals(rs[6].average(), 0.2573, 0.01);
   }
}
