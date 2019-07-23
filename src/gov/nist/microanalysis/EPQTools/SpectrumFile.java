package gov.nist.microanalysis.EPQTools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.List;

import gov.nist.microanalysis.EPQLibrary.BaseSpectrum;
import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
import gov.nist.microanalysis.EPQLibrary.SpectrumUtils;
import gov.nist.microanalysis.EPQLibrary.StandardBundle;

/**
 * <p>
 * A utility class to open spectrum files of all ilks..
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Company: National Institute of Standards and Technology
 * </p>
 *
 * @author not attributable
 * @version 1.0
 */

public class SpectrumFile {

   /**
    * ZombieSpectrum is used to manage memory when many spectra are loaded into
    * memory simultaneously. Each ZombieSpectrum refers to a spectrum on disk.
    * Most of the time the spectrum is loaded into memory. However occasionally
    * the garbage collector will need more memory and will purge the
    * SoftReference to the spectral data. If the spectrum is required again, the
    * class will reload the spectrum from disk. All this happens transparent to
    * the user.
    */
   public static class ZombieSpectrum
      extends
      BaseSpectrum {
      private SoftReference<BaseSpectrum> mSpectrum;
      private final File mFile;
      private final int mIndex;

      private BaseSpectrum revive() {
         BaseSpectrum res = (mSpectrum != null ? mSpectrum.get() : null);
         if(res == null) {
            try {
               res = SpectrumUtils.copy(SpectrumFile.open(mFile, mIndex));
            }
            catch(final EPQException ex) {
               return BaseSpectrum.NullSpectrum;
            }
            mSpectrum = new SoftReference<BaseSpectrum>(res);
         }
         return res;
      }

      /**
       * Constructs a ZombieSpectrum from the zeroth spectrum in the specified
       * file.
       *
       * @param file File The name of a spectrum containing file
       */
      public ZombieSpectrum(final File file) {
         this(file, 0, null);
      }

      /**
       * Constructs a ZombieSpectrum from the idx-th spectrum in the specified
       * file.
       *
       * @param file File The name of a spectrum containing file
       * @param idx int The index (zero-based) for the spectrum within a
       *           multispectrum file
       */
      public ZombieSpectrum(final File file, final int idx) {
         this(file, idx, null);
      }

      /**
       * Constructs a ZombieSpectrum from the idx-th spectrum in the specified
       * file and assigns its contents as spec.
       *
       * @param file File The name of a spectrum containing file
       * @param idx int The index (zero-based) for the spectrum within a
       *           multispectrum file
       * @param spec ISpectrumData The spectrum at idx in file.
       */
      public ZombieSpectrum(final File file, final int idx, final ISpectrumData spec) {
         super();
         mFile = file;
         mIndex = idx;
         if(spec != null) {
            mSpectrum = new SoftReference<BaseSpectrum>(SpectrumUtils.copy(spec));
            mHashCode = spec.hashCode();
         } else
            mHashCode = file.hashCode() ^ Integer.valueOf(mIndex).hashCode();
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getChannelCount()
       */
      @Override
      public int getChannelCount() {
         return revive().getChannelCount();
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getCounts(int)
       */
      @Override
      public double getCounts(final int i) {
         return revive().getCounts(i);
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getChannelWidth()
       */
      @Override
      public double getChannelWidth() {
         return revive().getChannelWidth();
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getZeroOffset()
       */
      @Override
      public double getZeroOffset() {
         return revive().getZeroOffset();
      }

      /**
       * @see gov.nist.microanalysis.EPQLibrary.ISpectrumData#getProperties()
       */
      @Override
      public SpectrumProperties getProperties() {
         return revive().getProperties();
      }

      /**
       * @see java.lang.Comparable#compareTo(java.lang.Object)
       */
      @Override
      public int compareTo(final ISpectrumData arg0) {
         return revive().compareTo(arg0);
      }

      /**
       * getUndeadSpectrum - Get a reference to the underlying spectrum. Since
       * there is not the additional indirection this method permits more
       * efficient use of the ZombieSpectrum so long as all refering classes
       * remember to null any direct references they hold.
       *
       * @return ISpectrumData - A ISpectrumData reference
       */
      public ISpectrumData getUndeadSpectrum() {
         return revive();
      }

      @Override
      public boolean equals(final Object obj) {
         return revive().equals(obj);
      }

      @Override
      public int hashCode() {
         return revive().hashCode();
      }
   }

   static public ISpectrumData[] open(final String path)
         throws EPQException {
      return open(new File(path));
   }

   static public boolean isInstanceOf(final File file) {
      boolean res = false;
      try {
         {
            try (final FileInputStream st = new FileInputStream(file)) {
               res = DTSAFile.isInstanceOf(st);
            }
         }
         if(!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = EMSAFile.isInstanceOf(st);
            }
         if(!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = EMISPECFile.isInstanceOf(st);
            }
         if(!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = ASPEXSpectrum.isInstanceOf(st);
            }
         if(!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = IXRFSpectrum.isInstanceOf(st);
            }
         if(!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = IXRFSpectrum.isInstanceOf(st);
            }
         if(!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = PMCASpectrum.isInstanceOf(st);
            }
         if(!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = RadiantSPDSpectrum.isInstanceOf(st);
            }
         if(!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = EdaxSPCSpectrum.isInstanceOf(st);
            }
         if(!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = BrukerSPX.isInstanceOf(st);
            }
      }
      catch(final FileNotFoundException e) {
      }
      catch(final IOException e) {
      }
      return res;
   }

   @SuppressWarnings("resource")
   static public ISpectrumData[] open(final File file)
         throws EPQException {
      try {
         ISpectrumData[] res = null;
         if(DTSAFile.isInstanceOf(new FileInputStream(file))) {
            final DTSAFile df = new DTSAFile(file);
            res = new ISpectrumData[df.getSpectrumCount()];
            for(int j = 0; j < df.getSpectrumCount(); ++j)
               res[j] = df.getSpectrum(j);
            return wrapResult(res, file);
         }
         if(EMSAFile.isInstanceOf(new FileInputStream(file))) {
            final EMSAFile ef = new EMSAFile();
            try (final FileInputStream st3 = new FileInputStream(file)) {
               ef.read(st3);
               ef.setFilename(file.toString());
               res = new ISpectrumData[1];
               res[0] = ef;
            }
            return wrapResult(res, file);
         }
         if(EMISPECFile.isInstanceOf(new FileInputStream(file))) {
            final EMISPECFile ef = new EMISPECFile();
            try (final FileInputStream st5 = new FileInputStream(file)) {
               ef.read(st5);
            }
            ef.setFilename(file.getName());
            res = new ISpectrumData[1];
            res[0] = ef;
            return wrapResult(res, file);
         }
         final FileInputStream st6 = new FileInputStream(file);
         if(ASPEXSpectrum.isInstanceOf(st6)) {
            res = new ISpectrumData[1];
            res[0] = new ASPEXSpectrum(file, true);
            return wrapResult(res, file);
         }
         final FileInputStream st7 = new FileInputStream(file);
         if(IXRFSpectrum.isInstanceOf(st7)) {
            res = new ISpectrumData[1];
            try (final FileInputStream st8 = new FileInputStream(file)) {
               final IXRFSpectrum is = new IXRFSpectrum(st8);
               is.setFilename(file.getName());
               res[0] = is;
            }
            return wrapResult(res, file);
         }
         final FileInputStream st9 = new FileInputStream(file);
         if(PMCASpectrum.isInstanceOf(st9)) {
            res = new ISpectrumData[1];
            try (final FileInputStream st10 = new FileInputStream(file)) {
               final PMCASpectrum is = new PMCASpectrum(st10);
               is.setFilename(file.getName());
               res[0] = is;
            }
            return wrapResult(res, file);
         }
         if(RadiantSPDSpectrum.isInstanceOf(new FileInputStream(file))) {
            res = new ISpectrumData[1];
            try (final FileInputStream st12 = new FileInputStream(file)) {
               final RadiantSPDSpectrum is = new RadiantSPDSpectrum(st12);
               is.setFilename(file.getName());
               res[0] = is;
               return wrapResult(res, file);
            }
         }
         if(EdaxSPCSpectrum.isInstanceOf(new FileInputStream(file))) {
            res = new ISpectrumData[1];
            try (final FileInputStream st14 = new FileInputStream(file)) {
               res[0] = new EdaxSPCSpectrum(st14);
               return wrapResult(res, file);
            }
         }
         if(BrukerSPX.isInstanceOf(new FileInputStream(file))) {
            res = new ISpectrumData[1];
            try (final FileInputStream st16 = new FileInputStream(file)) {
               res[0] = new BrukerSPX(st16);
               return wrapResult(res, file);
            }
         }
         if(BrukerTXT.isInstanceOf(new FileInputStream(file))) {
            res = new ISpectrumData[1];
            try (final FileInputStream st17 = new FileInputStream(file)) {
               res[0] = new BrukerTXT(st17);
               return wrapResult(res, file);
            }
         }
         if(StandardBundle.isInstance(file)) {
            final List<ISpectrumData> specs = StandardBundle.readSpectra(file);
            return wrapResult(specs.toArray(new ISpectrumData[specs.size()]), file);
         }
         throw new EPQException("The file " + file.getName() + " does not seem to be in one of the known file formats.");
      }
      catch(final Exception e) {
         throw new EPQException(e);
      }
   }

   private static ISpectrumData[] wrapResult(final ISpectrumData[] res, final File file) {
      if(res != null)
         for(int j = 0; j < res.length; ++j)
            res[j] = wrapResult(res[j], file, j);
      return res;
   }

   private static ISpectrumData wrapResult(final ISpectrumData res, final File file, final int idx) {
      if(res != null) {
         String filename = file.getName();
         final int p = filename.lastIndexOf('.');
         if(p != -1)
            filename = filename.substring(0, p);
         final SpectrumProperties props = res.getProperties();
         props.setTextProperty(SpectrumProperties.SourceFile, file.getAbsolutePath());
         props.setNumericProperty(SpectrumProperties.SpectrumIndex, Integer.valueOf(idx + 1));
         props.setTextProperty(SpectrumProperties.SourceFileId, filename + "[" + Integer.toString(idx + 1) + "]");
      }
      return res;
   }

   static public ISpectrumData open(final File file, final int idx)
         throws EPQException {
      try {
         try (FileInputStream fis = new FileInputStream(file)) {
            if(DTSAFile.isInstanceOf(fis)) {
               final DTSAFile df = new DTSAFile(file);
               return wrapResult(df.getSpectrum(idx), file, idx);
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if(EMSAFile.isInstanceOf(fis)) {
               if(idx != 0)
                  throw new EPQException("EMSA files can only contain one spectrum.");
               final EMSAFile ef = new EMSAFile();
               try (FileInputStream st = new FileInputStream(file)) {
                  ef.read(st);
                  ef.setFilename(file.getName());
                  return wrapResult(ef, file, idx);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if(EMISPECFile.isInstanceOf(fis)) {
               if(idx != 0)
                  throw new EPQException("EMISPEC files can only contain one spectrum.");
               final EMISPECFile ef = new EMISPECFile();
               try (FileInputStream st = new FileInputStream(file)) {
                  ef.read(st);
                  ef.setFilename(file.getName());
                  return wrapResult(ef, file, idx);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if(ASPEXSpectrum.isInstanceOf(fis)) {
               if(idx != 0)
                  throw new EPQException("ASPEX TIFF files can only contain one spectrum.");
               return wrapResult(new ASPEXSpectrum(file), file, idx);
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if(IXRFSpectrum.isInstanceOf(fis)) {
               if(idx != 0)
                  throw new EPQException("ASPEX TIFF files can only contain one spectrum.");
               try (final FileInputStream st = new FileInputStream(file)) {
                  final IXRFSpectrum is = new IXRFSpectrum(st);
                  is.setFilename(file.getName());
                  return wrapResult(is, file, idx);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if(PMCASpectrum.isInstanceOf(fis)) {
               if(idx != 0)
                  throw new EPQException("PMCA files can only contain one spectrum.");
               try (final FileInputStream st = new FileInputStream(file)) {
                  final PMCASpectrum is = new PMCASpectrum(st);
                  is.setFilename(file.getName());
                  return wrapResult(is, file, idx);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if(RadiantSPDSpectrum.isInstanceOf(fis)) {
               if(idx != 0)
                  throw new EPQException("Radiant files can only contain one spectrum.");
               final RadiantSPDSpectrum is;
               try (FileInputStream st = new FileInputStream(file)) {
                  is = new RadiantSPDSpectrum(st);
                  is.setFilename(file.getName());
                  return wrapResult(is, file, idx);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if(EdaxSPCSpectrum.isInstanceOf(fis)) {
               if(idx != 0)
                  throw new EPQException("EDAX files can only contain one spectrum.");
               try (FileInputStream st = new FileInputStream(file)) {
                  return wrapResult(new EdaxSPCSpectrum(st), file, idx);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if(BrukerSPX.isInstanceOf(fis)) {
               if(idx != 0)
                  throw new EPQException("Only single spectrum Bruker files are supported.");
               try (final FileInputStream st = new FileInputStream(file)) {
                  return wrapResult(new BrukerSPX(st), file, idx);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if(BrukerTXT.isInstanceOf(fis)) {
               if(idx != 0)
                  throw new EPQException("Only single spectrum Bruker text file are supported.");
               try (final FileInputStream st = new FileInputStream(file)) {
                  return wrapResult(new BrukerTXT(st), file, idx);
               }
            }
         }
         if(StandardBundle.isInstance(file)) {
            final List<ISpectrumData> specs = StandardBundle.readSpectra(file);
            return wrapResult(specs.get(idx), file, idx);
         }
         throw new EPQException("The file " + file.getName() + " does not seem to be in one of the known file formats.");
      }
      catch(final Exception ex) {
         throw new EPQException(ex);
      }
   }
}
