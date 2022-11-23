package gov.nist.microanalysis.EPQTools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.Utility.DescriptiveStatistics;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * A class for reading Gage data files which consist of a sequence of digitized
 * pulses and convert them into crudely integrated x-ray energies in spectrum
 * histogram form. The interface ProcessRawPulse provides a generic interface
 * which has been implemented to perform the most naive filtering and scaling
 * (NaivePulseProcessor). It could be implemented in a more sophisticated
 * manner.
 * </p>
 * <p>
 * The header in the Gage file is in ASCII format. The data in the Gage data
 * file is in little endian binary format.
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
public class GageSpectrum extends BaseSpectrum {

   interface ProcessRawPulse {
      /**
       * Implement this method to process the raw pulse data
       * 
       * @param data
       * @param preSamples
       * @return The channel (0 for reject)
       */
      int process(double[] data, int preSamples);
   }

   static class NaivePulseProcessor implements ProcessRawPulse {

      private final double[] mFilter;
      private final double mScale;

      NaivePulseProcessor(double scale) {
         mFilter = new double[20];
         for (int i = 0; i < (mFilter.length / 2); ++i) {
            mFilter[i] = -0.1 * i;
            mFilter[mFilter.length - (i + 1)] = -mFilter[i];
         }
         mScale = scale;
      }

      /**
       * @see gov.nist.microanalysis.EPQTools.GageSpectrum.ProcessRawPulse#process(double[],
       *      int)
       */
      @Override
      public int process(double[] data, int preSamples) {
         int res = 0;
         final double[] filtered = new double[data.length];
         final DescriptiveStatistics ds = new DescriptiveStatistics();
         for (int i = 0; i < ((3 * preSamples) / 4); ++i)
            ds.add(data[i]);
         double highest = 0.0;
         for (int i = 0; i < data.length; ++i) {
            if (data[i] > highest)
               highest = data[i];
            filtered[i] = 0.0;
            for (int j = 0; j < mFilter.length; ++j)
               filtered[i] += mFilter[j] * data[Math2
                     .bound((i - (mFilter.length / 2)) + j, 0, data.length)];
         }
         int peakCount = 0;
         int peakPos = Integer.MIN_VALUE;
         final double thresh = mFilter.length
               * Math.sqrt(ds.standardDeviation());
         for (int i = (3 * preSamples) / 4; i < data.length; ++i)
            if (filtered[i] > thresh) {
               ++peakCount;
               while ((i < filtered.length) && (filtered[i] > 0))
                  ++i;
               peakPos = i - 1;
            }
         if ((peakCount == 1) && (peakPos < (data.length / 2))) {
            double sum = 0;
            final int min = peakPos - 6;
            final int max = peakPos + (data.length / 3);
            for (int i = min; i < max; ++i)
               sum += data[i] - ds.average();
            res = (int) Math.round((sum / (max - min)) / mScale);
         }
         return res;
      }
   }

   static private final int FLAG = 0;

   private final double[] mData;
   private final SpectrumProperties mProperties;
   private final int mRejected;
   private ProcessRawPulse mProcessor;

   public GageSpectrum(File file, int chCount, double scale, ActionListener al)
         throws IOException {
      mData = new double[chCount];
      mProperties = new SpectrumProperties();
      mProperties.setNumericProperty(SpectrumProperties.EnergyOffset, 0.0);
      mProperties.setNumericProperty(SpectrumProperties.EnergyScale, 0.5);
      mProcessor = new NaivePulseProcessor(scale);
      try (final FileInputStream fis = new FileInputStream(file)) {
         try (final BufferedReader bis = new BufferedReader(
               new InputStreamReader(fis, "US-ASCII"))) {
            String str, data;
            int preSamples = Integer.MIN_VALUE,
                  totalSamples = Integer.MIN_VALUE;
            final SpectrumProperties sp = getProperties();
            // Read the header as ASCII
            do {
               str = bis.readLine();
               assert str != null;
               final int p = str.indexOf(":");
               data = p > 0 ? str.substring(p + 1).trim() : "";
               if (str.startsWith("Operator:"))
                  sp.setTextProperty(SpectrumProperties.Analyst, data);
               if (str.startsWith("Sample:"))
                  sp.setTextProperty(SpectrumProperties.SpecimenDesc, data);
               if (str.startsWith("Excitation/Source:"))
                  sp.setTextProperty(SpectrumProperties.Instrument, data);
               if (str.startsWith("Presamples:"))
                  preSamples = Integer.parseInt(data);
               if (str.startsWith("Total Samples:"))
                  totalSamples = Integer.parseInt(data);
            } while (!str.equals("#End of Header"));
            final double[] buffer = new double[totalSamples];
            final long fileLen = fis.getChannel().size();
            int pos = 0;
            // Read the data as little endian binary (not necessarily the most
            // efficient code but...)
            if ((totalSamples > 0) && (preSamples > 0)) {
               try (final LEDataInputStream isr = new LEDataInputStream(fis)) {
                  int rejected = 0, cx = 0;
                  while (fis.available() > 0) {
                     final int x = isr.readShort();
                     if (x == FLAG) { // Marks the end of the pulse
                        if (al != null) {
                           final int p = (int) ((1000
                                 * fis.getChannel().position()) / fileLen);
                           if (p > pos) {
                              pos = p;
                              al.actionPerformed(new ActionEvent(this, pos,
                                    Double.toString(0.1 * pos) + "% complete"));
                           }
                        }
                        // int ts=
                        isr.readInt(); // Timestamp
                        // Add the event to the spectrum data
                        final int res = (cx == buffer.length
                              ? mProcessor.process(buffer, preSamples)
                              : 0);
                        if ((res <= 0) || (res >= mData.length))
                           ++rejected;
                        else
                           ++mData[res];
                        cx = 0;
                     } else if (cx < totalSamples) {
                        buffer[cx] = x;
                        ++cx;
                     }
                  }
                  mRejected = rejected;
               }
            } else
               mRejected = Integer.MAX_VALUE;
         }
      }
   }

   /**
    * Constructs a MicrocalSpectrum object from a file.
    * display(ept.MicrocalSpectrum('C:/Documents and
    * Settings/nritchie/Desktop/tmp/AuCu5spectrum1.dat', 1.0))
    * 
    * @param path
    * @param scale
    * @throws IOException
    */
   public GageSpectrum(String path, double scale) throws IOException {
      this(new File(path), 16192, 0.5 * scale, (ActionListener) null);
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getChannelCount()
    */
   @Override
   public int getChannelCount() {
      return mData.length;
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getCounts(int)
    */
   @Override
   public double getCounts(int i) {
      return mData[i];
   }

   /**
    * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getProperties()
    */
   @Override
   public SpectrumProperties getProperties() {
      return mProperties;
   }

   /**
    * The number of rejected pulses
    * 
    * @return int
    */
   public int getRejected() {
      return mRejected;
   }

   /**
    * Gets the current value assigned to processor
    * 
    * @return Returns the processor.
    */
   public ProcessRawPulse getProcessor() {
      return mProcessor;
   }

   /**
    * Allows the user to specify an alternative implementation of
    * ProcessRawPulse.
    * 
    * @param processor
    *           The value to which to set processor.
    */
   public void setProcessor(ProcessRawPulse processor) {
      mProcessor = processor;
   }
}
