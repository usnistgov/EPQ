package gov.nist.microanalysis.EPQTools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import gov.nist.microanalysis.EPQLibrary.EPQException;
import gov.nist.microanalysis.EPQLibrary.ISpectrumData;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties;
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

   static public ISpectrumData[] open(final String path) throws EPQException {
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
         if (!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = EMSAFile.isInstanceOf(st);
            }
         if (!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = EMISPECFile.isInstanceOf(st);
            }
         if (!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = ASPEXSpectrum.isInstanceOf(st);
            }
         if (!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = IXRFSpectrum.isInstanceOf(st);
            }
         if (!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = IXRFSpectrum.isInstanceOf(st);
            }
         if (!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = PMCASpectrum.isInstanceOf(st);
            }
         if (!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = RadiantSPDSpectrum.isInstanceOf(st);
            }
         if (!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = EdaxSPCSpectrum.isInstanceOf(st);
            }
         if (!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = BrukerSPX.isInstanceOf(st);
            }
         if (!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = BrukerPDZ.isInstanceOf(st);
            }
         if (!res)
            try (final FileInputStream st = new FileInputStream(file)) {
               res = OxfordSPTFile.isInstanceOf(st);
            }
      } catch (final FileNotFoundException e) {
      } catch (final IOException e) {
      }
      return res;
   }

   static public ISpectrumData[] open(final File file) throws EPQException {
      try {
         ISpectrumData[] res = null;
         try (FileInputStream fis = new FileInputStream(file)) {
            if (DTSAFile.isInstanceOf(fis)) {
               final DTSAFile df = new DTSAFile(file);
               res = new ISpectrumData[df.getSpectrumCount()];
               for (int j = 0; j < df.getSpectrumCount(); ++j)
                  res[j] = df.getSpectrum(j);
               return wrapResult(res, file);
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (EMSAFile.isInstanceOf(fis)) {
               final EMSAFile ef = new EMSAFile(file, true);
               res = new ISpectrumData[1];
               res[0] = ef;
               return wrapResult(res, file);
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (EMISPECFile.isInstanceOf(fis)) {
               final EMISPECFile ef = new EMISPECFile();
               try (final FileInputStream st5 = new FileInputStream(file)) {
                  ef.read(st5);
               }
               ef.setFilename(file.getName());
               res = new ISpectrumData[1];
               res[0] = ef;
               return wrapResult(res, file);
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (ASPEXSpectrum.isInstanceOf(fis)) {
               res = new ISpectrumData[1];
               res[0] = new ASPEXSpectrum(file, true);
               return wrapResult(res, file);
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (IXRFSpectrum.isInstanceOf(fis)) {
               res = new ISpectrumData[1];
               try (final FileInputStream st8 = new FileInputStream(file)) {
                  final IXRFSpectrum is = new IXRFSpectrum(st8);
                  is.setFilename(file.getName());
                  res[0] = is;
               }
               return wrapResult(res, file);
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (PMCASpectrum.isInstanceOf(fis)) {
               res = new ISpectrumData[1];
               try (final FileInputStream st10 = new FileInputStream(file)) {
                  final PMCASpectrum is = new PMCASpectrum(st10);
                  is.setFilename(file.getName());
                  res[0] = is;
               }
               return wrapResult(res, file);
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (RadiantSPDSpectrum.isInstanceOf(fis)) {
               res = new ISpectrumData[1];
               try (final FileInputStream st12 = new FileInputStream(file)) {
                  final RadiantSPDSpectrum is = new RadiantSPDSpectrum(st12);
                  is.setFilename(file.getName());
                  res[0] = is;
                  return wrapResult(res, file);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (EdaxSPCSpectrum.isInstanceOf(fis)) {
               res = new ISpectrumData[1];
               try (final FileInputStream st14 = new FileInputStream(file)) {
                  res[0] = new EdaxSPCSpectrum(st14);
                  return wrapResult(res, file);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (BrukerSPX.isInstanceOf(fis)) {
               res = new ISpectrumData[1];
               try (final FileInputStream st16 = new FileInputStream(file)) {
                  res[0] = new BrukerSPX(st16);
                  return wrapResult(res, file);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (BrukerTXT.isInstanceOf(fis)) {
               res = new ISpectrumData[1];
               try (final FileInputStream st17 = new FileInputStream(file)) {
                  res[0] = new BrukerTXT(st17);
                  return wrapResult(res, file);
               }
            }
         }
         if (StandardBundle.isInstance(file)) {
            final List<ISpectrumData> specs = StandardBundle.readSpectra(file);
            return wrapResult(specs.toArray(new ISpectrumData[specs.size()]),
                  file);
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (BrukerPDZ.isInstanceOf(fis)) {
               res = new ISpectrumData[1];
               try (final FileInputStream st18 = new FileInputStream(file)) {
                  res[0] = new BrukerPDZ(st18);
                  return wrapResult(res, file);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (OxfordSPTFile.isInstanceOf(fis)) {
               res = new ISpectrumData[2];
               try (final FileInputStream st19 = new FileInputStream(file)) {
                  res[0] = new OxfordSPTFile(st19, false);
               }
               try (final FileInputStream st20 = new FileInputStream(file)) {
                  res[1] = new OxfordSPTFile(st20, true);
               }
               return wrapResult(res, file);
            }
         }
         throw new EPQException("The file " + file.getName()
               + " does not seem to be in one of the known file formats.");
      } catch (final Exception e) {
         throw new EPQException(e);
      }
   }

   private static ISpectrumData[] wrapResult(final ISpectrumData[] res,
         final File file) {
      if (res != null)
         for (int j = 0; j < res.length; ++j)
            res[j] = wrapResult(res[j], file, j);
      return res;
   }

   private static ISpectrumData wrapResult(final ISpectrumData res,
         final File file, final int idx) {
      if (res != null) {
         String filename = file.getName();
         final int p = filename.lastIndexOf('.');
         if (p != -1)
            filename = filename.substring(0, p);
         final SpectrumProperties props = res.getProperties();
         props.setTextProperty(SpectrumProperties.SourceFile,
               file.getAbsolutePath());
         props.setNumericProperty(SpectrumProperties.SpectrumIndex,
               Integer.valueOf(idx + 1));
         props.setTextProperty(SpectrumProperties.SourceFileId,
               filename + "[" + Integer.toString(idx + 1) + "]");
      }
      return res;
   }

   static public ISpectrumData open(final File file, final int idx)
         throws EPQException {
      try {
         try (FileInputStream fis = new FileInputStream(file)) {
            if (DTSAFile.isInstanceOf(fis)) {
               final DTSAFile df = new DTSAFile(file);
               return wrapResult(df.getSpectrum(idx), file, idx);
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (EMSAFile.isInstanceOf(fis)) {
               if (idx != 0)
                  throw new EPQException(
                        "EMSA files can only contain one spectrum.");
               final EMSAFile ef = new EMSAFile();
               try (FileInputStream st = new FileInputStream(file)) {
                  ef.read(st);
                  ef.setFilename(file.getName());
                  return wrapResult(ef, file, idx);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (EMISPECFile.isInstanceOf(fis)) {
               if (idx != 0)
                  throw new EPQException(
                        "EMISPEC files can only contain one spectrum.");
               final EMISPECFile ef = new EMISPECFile();
               try (FileInputStream st = new FileInputStream(file)) {
                  ef.read(st);
                  ef.setFilename(file.getName());
                  return wrapResult(ef, file, idx);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (ASPEXSpectrum.isInstanceOf(fis)) {
               if (idx != 0)
                  throw new EPQException(
                        "ASPEX TIFF files can only contain one spectrum.");
               return wrapResult(new ASPEXSpectrum(file), file, idx);
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (IXRFSpectrum.isInstanceOf(fis)) {
               if (idx != 0)
                  throw new EPQException(
                        "ASPEX TIFF files can only contain one spectrum.");
               try (final FileInputStream st = new FileInputStream(file)) {
                  final IXRFSpectrum is = new IXRFSpectrum(st);
                  is.setFilename(file.getName());
                  return wrapResult(is, file, idx);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (PMCASpectrum.isInstanceOf(fis)) {
               if (idx != 0)
                  throw new EPQException(
                        "PMCA files can only contain one spectrum.");
               try (final FileInputStream st = new FileInputStream(file)) {
                  final PMCASpectrum is = new PMCASpectrum(st);
                  is.setFilename(file.getName());
                  return wrapResult(is, file, idx);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (RadiantSPDSpectrum.isInstanceOf(fis)) {
               if (idx != 0)
                  throw new EPQException(
                        "Radiant files can only contain one spectrum.");
               final RadiantSPDSpectrum is;
               try (FileInputStream st = new FileInputStream(file)) {
                  is = new RadiantSPDSpectrum(st);
                  is.setFilename(file.getName());
                  return wrapResult(is, file, idx);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (EdaxSPCSpectrum.isInstanceOf(fis)) {
               if (idx != 0)
                  throw new EPQException(
                        "EDAX files can only contain one spectrum.");
               try (FileInputStream st = new FileInputStream(file)) {
                  return wrapResult(new EdaxSPCSpectrum(st), file, idx);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (BrukerSPX.isInstanceOf(fis)) {
               if (idx != 0)
                  throw new EPQException(
                        "Only single spectrum Bruker files are supported.");
               try (final FileInputStream st = new FileInputStream(file)) {
                  return wrapResult(new BrukerSPX(st), file, idx);
               }
            }
         }
         try (FileInputStream fis = new FileInputStream(file)) {
            if (BrukerTXT.isInstanceOf(fis)) {
               if (idx != 0)
                  throw new EPQException(
                        "Only single spectrum Bruker text file are supported.");
               try (final FileInputStream st = new FileInputStream(file)) {
                  return wrapResult(new BrukerTXT(st), file, idx);
               }
            }
         }
         if (StandardBundle.isInstance(file)) {
            final List<ISpectrumData> specs = StandardBundle.readSpectra(file);
            return wrapResult(specs.get(idx), file, idx);
         }
         throw new EPQException("The file " + file.getName()
               + " does not seem to be in one of the known file formats.");
      } catch (final Exception ex) {
         throw new EPQException(ex);
      }
   }
}
