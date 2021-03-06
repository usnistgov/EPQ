package gov.nist.microanalysis.EPQLibrary;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import gov.nist.microanalysis.EPQLibrary.RegionOfInterestSet.RegionOfInterest;
import gov.nist.microanalysis.EPQLibrary.SpectrumProperties.PropertyId;
import gov.nist.microanalysis.EPQLibrary.Detector.EDSDetector;
import gov.nist.microanalysis.EPQTools.EMSAFile;
import gov.nist.microanalysis.EPQTools.EPQXStream;
import gov.nist.microanalysis.EPQTools.WriteSpectrumAsEMSA1_0;

/**
 * <p>
 * Describes a special file that contains both a standard and the references
 * necessary to use the standard.
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
public class StandardBundle {
   private final EDSDetector mDetector;
   private final Element mElement;
   private ISpectrumData mStandard;
   private final Set<Element> mStrip;
   private final Map<RegionOfInterest, ISpectrumData> mReferences;

   private transient QuantificationOutline mOutline;

   @Override
   public String toString() {
      return "Bundle for " + mElement.toAbbrev();
   }

   public Element getElement() {
      return mElement;
   }

   public void checkSuitableAsStandard(final ISpectrumData spec)
         throws EPQException {
	   if(spec == null)
		   throw new EPQException("No standard spectrum has been specified.");
      if(!(spec.getProperties().getDetector() instanceof EDSDetector))
         throw new EPQException("No EDS detector is defined for this standard.");
      if(spec.getProperties().getNumericWithDefault(SpectrumProperties.BeamEnergy, -1.0) == -1.0)
         throw new EPQException("The beam energy is not defined for this standard. ");
      final Composition std = spec.getProperties().getCompositionWithDefault(SpectrumProperties.StandardComposition, null);
      if(std == null)
         throw new EPQException("The standard composition is not defined for this spectrum. ");
      if(!std.containsElement(mElement))
         throw new EPQException("The standard composition does not contain the required element. ");
   }

   public StandardBundle(final Element elm, final ISpectrumData spec, final Set<Element> stripElms)
         throws EPQException {
      mElement = elm;
      checkSuitableAsStandard(spec);
      mStandard = spec;
      mDetector = (EDSDetector) spec.getProperties().getDetector();
      mReferences = new HashMap<>();
      mOutline = new QuantificationOutline(mDetector, getBeamEnergy());
      mOutline.add(elm);
      mOutline.addStandard(elm, spec.getProperties().getCompositionProperty(SpectrumProperties.StandardComposition), stripElms);
      mStrip = new TreeSet<Element>(stripElms);
   }

   public void updateStandard(final ISpectrumData spec)
         throws EPQException {
      assert spec.getProperties().getDetector() == mDetector;
      checkSuitableAsStandard(spec);
      mStandard = spec;
   }

   public double getBeamEnergy() {
      return ToSI.keV(mStandard.getProperties().getNumericWithDefault(SpectrumProperties.BeamEnergy, Double.NaN));
   }

   public ISpectrumData getStandard() {
      return mStandard;
   }

   /**
    * Returns a set of ROIs for which reference spectra (other than the
    * standard) are required .
    *
    * @return Set&lt;RegionOfInterest&gt;
    */
   public Set<RegionOfInterest> getAllRequiredReferences() {
      return mOutline.getAllRequiredReferences(false);
   }

   public boolean isSuitableAsReference(final RegionOfInterest roi, final ISpectrumData spec) {
      final Set<RegionOfInterest> rois = mOutline.suitableAsReference(spec.getProperties().getElements());
      for(final RegionOfInterest roi2 : rois)
         if(roi2.fullyContains(roi))
            return roi2.getElementSet().size()==1;
      return false;
   }

   private RegionOfInterest bestMatch(final RegionOfInterest roi, final Collection<RegionOfInterest> rois) {
      assert roi.getElementSet().size() == 1;
      RegionOfInterest best = null;
      double bestScore = 0.95;
      for(final RegionOfInterest reqRoi : rois) {
         assert reqRoi.getElementSet().size() == 1;
         final double score = score(reqRoi, roi);
         if(score > bestScore) {
            bestScore = score;
            best = reqRoi;
         }
      }
      return best;
   }

   /**
    * Provides a match metric for similarity of coverage between roi1 and roi2.
    *
    * @param roi1
    * @param roi2
    * @return
    */
   private double score(final RegionOfInterest roi1, final RegionOfInterest roi2) {
      final Set<XRayTransition> xrts1 = new TreeSet<XRayTransition>(roi1.getAllTransitions().getTransitions());
      final XRayTransitionSet xrts2 = roi2.getAllTransitions();
      double sum = 0.0, all = 0.0;
      for(final XRayTransition xrt : xrts2.getTransitions()) {
         all += xrt.getNormalizedWeight();
         if(xrts1.contains(xrt)) {
            sum += xrt.getNormalizedWeight();
            xrts1.remove(xrt);
         }
      }
      for(final XRayTransition xrt : xrts1)
         all += xrt.getNormalizedWeight();
      return sum / all;
   }

   public void addRemappedReference(final RegionOfInterest roi, final ISpectrumData spec)
         throws EPQException {
      final RegionOfInterest matchBest = mReferences.containsKey(roi) ? roi
            : bestMatch(roi, mOutline.getAllRequiredReferences(false));
      if(matchBest != null)
         addReference(matchBest, spec);
   }

   public void addReference(final RegionOfInterest roi, final ISpectrumData spec)
         throws EPQException {
      mOutline.addReference(roi, roi.getElementSet());
      spec.getProperties().clear(new PropertyId[] {
         SpectrumProperties.StandardComposition,
         SpectrumProperties.MicroanalyticalComposition
      });
      spec.getProperties().setElements(roi.getElementSet());
      mReferences.put(roi, spec);
   }

   public Map<RegionOfInterest, ISpectrumData> getReferences() {
      return Collections.unmodifiableMap(mReferences);
   }

   public void addStrip(final Element elm) {
      mStrip.add(elm);
      mOutline.addElementToStrip(elm);
   }

   public void removeStrip(final Element elm) {
      mStrip.remove(elm);
      mOutline.clearElementToStrip(elm);
   }

   public void addCoating(ConductiveCoating cc) {
      mOutline.setCoating(cc);
   }

   public void removeCoating() {
      mOutline.clearCoating();
   }

   public Set<Element> getStrippedElements() {
      return Collections.unmodifiableSet(mStrip);
   }

   public ISpectrumData getReference(final RegionOfInterest roi) {
      return mReferences.get(roi);
   }

   public void clearReference(final RegionOfInterest roi) {
      mReferences.remove(roi);
   }

   private static final class ZipFileHeader {
      private int Version;
      private Element Element;
      private Composition StandardComp;
      private String DetectorName;
      private final TreeSet<Element> Strip = new TreeSet<Element>();
      private final Map<RegionOfInterest, String> References = new HashMap<RegionOfInterest, String>();
   }

   private void write(final String name, final ISpectrumData spec, final ZipOutputStream zos)
         throws IOException, EPQException {
      final File temp = File.createTempFile("spec", "msa");
      final FileOutputStream fos = new FileOutputStream(temp);
      WriteSpectrumAsEMSA1_0.write(spec, fos, WriteSpectrumAsEMSA1_0.Mode.COMPATIBLE);
      fos.close();
      final ZipEntry ze = new ZipEntry(name);
      zos.putNextEntry(ze);
      final FileInputStream fis = new FileInputStream(temp);
      final BufferedInputStream bis = new BufferedInputStream(fis, 4096);
      final byte[] data = new byte[4096];
      int size = -1;
      while((size = bis.read(data, 0, data.length)) != -1)
         zos.write(data, 0, size);
      bis.close();
   }

   public void write(final File f)
         throws IOException, EPQException {
      final ZipFileHeader zfh = new ZipFileHeader();
      zfh.Version = 2;
      zfh.Element = mElement;
      zfh.StandardComp = mStandard.getProperties().getCompositionWithDefault(SpectrumProperties.StandardComposition, null);
      zfh.DetectorName = mDetector.getProperties().getTextWithDefault(SpectrumProperties.DetectorGUID, "Unknown");
      zfh.Strip.addAll(mStrip);
      assert zfh.StandardComp != null;
      int i = 0;
      for(final Map.Entry<RegionOfInterest, ISpectrumData> me : mReferences.entrySet()) {
         final RegionOfInterest roi = me.getKey();
         assert roi.getElementSet().size() == 1;
         zfh.References.put(roi, "Reference[" + i++ + "," + roi.toString() + "]");
      }
      final FileOutputStream fos = new FileOutputStream(f);
      try (final ZipOutputStream zip = new ZipOutputStream(fos)) {
         final ZipEntry ze = new ZipEntry("Header");
         zip.putNextEntry(ze);
         // Write header
         String xml = EPQXStream.getInstance().toXML(zfh);
         OutputStreamWriter osw = new OutputStreamWriter(zip, "UTF-8");
         osw.write(xml);
         osw.flush();
         // Write standard spectrum
         write("Standard", mStandard, zip);
         // Write reference spectra
         for(final Map.Entry<RegionOfInterest, String> me : zfh.References.entrySet())
            write(me.getValue(), mReferences.get(me.getKey()), zip);
      }
   }

   private static final ISpectrumData read(final String name, final ZipFile zf)
         throws IOException {
      final ZipEntry ze = new ZipEntry(name);
      try (final InputStream zis = zf.getInputStream(ze)) {
         return new EMSAFile(zis);
      }
   }

   public static StandardBundle read(final File f, final EDSDetector det)
         throws IOException, EPQException {
      try (ZipFile zf = new ZipFile(f)) {
         final ZipEntry ze = zf.getEntry("Header");
         try (InputStream is = zf.getInputStream(ze)) {
            final InputStreamReader isr = new InputStreamReader(is, "UTF-8");
            final ZipFileHeader zfh = (ZipFileHeader) EPQXStream.getInstance().fromXML(isr);
            if(zfh.Version < 1)
               throw new EPQException("DRAT! Older SVU standard bundles are no longer supported.");
            StandardBundle res;
            final String guid = det.getProperties().getTextWithDefault(SpectrumProperties.DetectorGUID, "Unknown");
            if(!(zfh.DetectorName.equals(guid) || zfh.DetectorName.equals(det.getName())))
               throw new EPQException("The standard does not appear to have been collected on the same detector as the unknown.");
            final ISpectrumData std = read("Standard", zf);
            std.getProperties().setCompositionProperty(SpectrumProperties.StandardComposition, zfh.StandardComp);
            std.getProperties().setDetector(det);
            res = new StandardBundle(zfh.Element, std, zfh.Strip);
            for(final Map.Entry<RegionOfInterest, String> me : zfh.References.entrySet())
               try {
                  res.addRemappedReference(me.getKey(), read(me.getValue(), zf));
               }
               catch(Exception e) {
                  if(zfh.Version == 1)
                     System.err.println("DRAT! A version 1 bundle with a bug resolved in version 2. - " + me.getValue());
               }
            return res;
         }
      }
   }

   public static boolean isInstance(final File f) {
      try (ZipFile zf = new ZipFile(f)) {
         final ZipEntry ze = zf.getEntry("Header");
         if(ze != null)
            try (InputStream is = zf.getInputStream(ze)) {
               final Object zfh = EPQXStream.getInstance().fromXML(is);
               return zfh instanceof ZipFileHeader;
            }
      }
      catch(final IOException e) {
      }
      return false;
   }

   public static List<ISpectrumData> readSpectra(final File f)
         throws IOException, EPQException {
      final ArrayList<ISpectrumData> res = new ArrayList<ISpectrumData>();
      try (ZipFile zf = new ZipFile(f)) {
         final ZipEntry ze = zf.getEntry("Header");
         try (InputStream is = zf.getInputStream(ze)) {
            final ZipFileHeader zfh = (ZipFileHeader) EPQXStream.getInstance().fromXML(is);
            final ISpectrumData std = read("Standard", zf);
            std.getProperties().setCompositionProperty(SpectrumProperties.StandardComposition, zfh.StandardComp);
            res.add(std);
            for(final Map.Entry<RegionOfInterest, String> me : zfh.References.entrySet())
               res.add(read(me.getValue(), zf));
            return res;
         }
      }
   }
}
