package gov.nist.microanalysis.EPQTests;

import gov.nist.microanalysis.EPQLibrary.BremsstrahlungAnalytic;
import gov.nist.microanalysis.EPQLibrary.Composition;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.MaterialFactory;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.ToSI;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.EPQTools.ASPEXSpectrum;
import gov.nist.microanalysis.EPQTools.WriteSpectrumAsEMSA1_0;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.TestCase;

public class BremsstrahlungAnalyticTest
   extends TestCase {

   public void testFitBackground()
         throws Exception {
      final ISpectrumData spec = new ASPEXSpectrum(new File(FilterFitTest.class.getResource("TestData/Gold.tif").toURI()));
      final EDSDetector det = EDSDetector.createSiLiDetector(2048, 10.0, 132.0);
      final Composition comp = MaterialFactory.createPureElement(Element.Au);
      final BremsstrahlungAnalytic ba = BremsstrahlungAnalytic.Small87;
      ba.initialize(comp, ToSI.keV(spec.getProperties().getNumericProperty(SpectrumProperties.BeamEnergy)), Math.toRadians(40));
      final Collection<int[]> rois = new ArrayList<int[]>();
      rois.add(new int[] {
         350,
         370
      });
      final ISpectrumData brem = ba.fitBackground(det, spec, rois);
      final FileOutputStream fos = new FileOutputStream("/home/nicholas/Desktop/BremFitTest/AuBrem.emsa");
      WriteSpectrumAsEMSA1_0.write(brem, fos, WriteSpectrumAsEMSA1_0.Mode.COMPATIBLE);
   }

}
