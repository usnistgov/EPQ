package gov.nist.microanalysis.EPQLibrary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Construct an input text file for BadgerFilm to represent a thin-film
 * measurement.
 * 
 * @author nritchie
 *
 */
public class BadgerFilmExport {

   private static class BFLayer {
      private double mDensity; // g/cm3
      private boolean mIsFixed;
      private double mThickness; // nm
      private Composition mComposition;
      private Set<Element> mFixedElements;

      private BFLayer(Composition comp, double density, double thickness, boolean isFixed, Collection<Element> fixed) {
         mDensity = density;
         mIsFixed = isFixed;
         mThickness = thickness;
         mComposition = comp;
         mFixedElements = new HashSet<>(fixed);
      }
   };

   private static class Measurement {
      private double mBeamEnergy;
      private double mKRatio;
      private Measurement(double e0, double k) {
         mBeamEnergy = e0;
         mKRatio = k;
      }

   };

   private static class Standard {
      private String mName;
      private String mFilename;

      public Standard(String name, String filename) {
         mName = name;
         mFilename = filename;
      }

   };

   private double mTakeOffAngle; // In degrees
   private List<BFLayer> mLayers;
   private Map<Element, Map<XRayTransition, List<Measurement>>> mMeasuredKRatios;
   private Map<XRayTransition, Standard> mStandards;

   public BadgerFilmExport(double toa) {
      mTakeOffAngle = toa;
      mLayers = new ArrayList<>();
      mMeasuredKRatios = new HashMap<>();
      mStandards = new HashMap<>();;
   }

   public void addMeasurement(XRayTransition xrt, double beamEnergy, double kratio) {
      Map<XRayTransition, List<Measurement>> mxlm = mMeasuredKRatios.computeIfAbsent(xrt.getElement(),
            el -> new HashMap<XRayTransition, List<Measurement>>());
      List<Measurement> lm = mxlm.computeIfAbsent(xrt, x -> new ArrayList<Measurement>());
      lm.add(new Measurement(beamEnergy, kratio));
   }

   public void addLayer(Composition comp, double density, double thickness, boolean isFixed, Collection<Element> fixed) {
      mLayers.add(new BFLayer(comp, density, thickness, isFixed, fixed));
   }

   public void addStandard(XRayTransition xrt, String name, String filename) {
      mStandards.put(xrt, new Standard(name, filename));
   }

   public void export(File file) throws EPQException, FileNotFoundException, UnsupportedEncodingException {
      try (PrintWriter pw = new PrintWriter(file, "US-ASCII")) {
         export(pw);
      }
   }

   public void export(PrintWriter pw) throws EPQException {
      pw.print("toa\t");
      pw.println(mTakeOffAngle);
      pw.print("#layer_handler\t");
      pw.println(mLayers.size());
      pw.println("******************");
      for (int i = 0; i < mLayers.size(); ++i) {
         final BFLayer layer = mLayers.get(i);
         pw.print("layer_handler\t");
         pw.println(i);
         pw.print("density\t");
         pw.println(layer.mDensity);
         pw.print("isfix\t");
         pw.println(layer.mIsFixed ? "True" : "False");
         pw.print("thickness\t");
         pw.println(layer.mThickness);
         pw.print("wt_fraction\t");
         pw.println("True");
         pw.print("#elt\t");
         final Composition comp = layer.mComposition;
         pw.println(comp.getElementCount());
         pw.println("***************");
         final Iterator<Element> elms = comp.getElementSet().iterator();
         for (int j = 0; j < comp.getElementCount(); ++j) {
            Element el = elms.next();
            pw.print("elt\t");
            pw.println(j);
            pw.print("name\t");
            pw.println(el.toAbbrev());
            pw.print("isConcFixed\t");
            pw.println(layer.mFixedElements.contains(el) ? "True" : "False");
            pw.print("conc\t");
            pw.println(comp.weightFraction(el, false));
         }
         pw.println("***************");
      }
      pw.print("#num_elt_exp\t");
      pw.println(mMeasuredKRatios.size());
      pw.println("*********");
      for (Map.Entry<Element, Map<XRayTransition, List<Measurement>>> me1 : mMeasuredKRatios.entrySet()) {
         Element el = me1.getKey();
         pw.print("#a\t");
         pw.println(el.getAtomicWeight());
         pw.print("#elt_name\t");
         pw.println(el.toAbbrev());
         pw.print("#z\t");
         pw.println(el.getAtomicNumber());
         pw.print("#num_line\t");
         pw.println(me1.getValue().size());
         for (Map.Entry<XRayTransition, List<Measurement>> me2 : me1.getValue().entrySet()) {
            XRayTransition xrt = me2.getKey();
            pw.print("#Ec\t");
            pw.println(FromSI.keV(xrt.getEdgeEnergy()));
            pw.print("#xray_energy\t");
            pw.println(FromSI.keV(xrt.getEnergy()));
            pw.print("#xray_name\t");
            switch (xrt.getTransitionIndex()) {
               case XRayTransition.KA1 :
               case XRayTransition.KA2 :
                  pw.println("Ka");
                  break;
               case XRayTransition.KB1 :
                  pw.println("Kb");
                  break;
               case XRayTransition.LA1 :
                  pw.println("La");
                  break;
               case XRayTransition.MA1 :
                  pw.println("MA1");
                  break;
               default :
                  pw.println("???");
                  break;
            }
            Standard std = mStandards.get(xrt);
            pw.print("#std\t");
            pw.println(std == null ? "" : std.mName);
            pw.print("#std_filename\t");
            pw.println(std == null ? "" : std.mFilename);
            pw.print("#num_kratios\t");
            List<Measurement> meas = me2.getValue();
            pw.println(meas.size());
            for (int k = 0; k < meas.size(); ++k) {
               final Measurement m = meas.get(k);
               pw.println("#elt_intensity\t0");
               pw.print("#kv\t");
               pw.println(m.mBeamEnergy);
               pw.print("#measured_value\t");
               pw.println(m.mKRatio);
               pw.println("#std_intensity\t0");
               pw.println("#theo_value\t0");
            }
         }
      }

   }

}
