package gov.nist.microanalysis.EPQTools;

import java.io.OutputStream;
import java.io.PrintWriter;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;

/**
 * <p>
 * Write an object that implements the ISpectrumData interface to a comma
 * separated values text file.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 * 
 * @author Nicholas W. M. Ritchie
 * @version 1.0
 */

public class WriteSpectrumAsCSV {
   public static void write(ISpectrumData spec, OutputStream os) throws EPQException {
      try (final PrintWriter pw = new PrintWriter(os)) {
         final SpectrumProperties sp = spec.getProperties();
         final int nCh = spec.getChannelCount();
         final double wCh = spec.getChannelWidth();
         final double offset = sp.getNumericWithDefault(SpectrumProperties.EnergyOffset, 0.0);
         pw.print('"');
         pw.print(sp.getTextWithDefault(SpectrumProperties.SpectrumComment, "Uncommented spectrum"));
         pw.println('"');
         pw.print('"');
         pw.print(sp.getTextWithDefault(SpectrumProperties.SpecimenDesc, "Uncommented specimen"));
         pw.println('"');
         for (int i = 0; i < nCh; ++i) {
            pw.print((i * wCh) + offset);
            pw.print(", ");
            pw.println(spec.getCounts(i));
         }
      }
   }

}
