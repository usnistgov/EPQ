package gov.nist.microanalysis.EPQLibrary;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import gov.nist.microanalysis.EPQTools.EPQXStream;
import gov.nist.microanalysis.Utility.Math2;

/**
 * <p>
 * A VectorSet is a generic set of Vector objects which are simply things you
 * dot product onto spectra to return elemental information. You can implement
 * simple peak integrals or estimated background corrected peak integrals using
 * vectors or more sophisticated filter fits a la Fred Schamber's suggestion.
 * </p>
 * <p>
 * Calculate the vectors in another class.
 * </p>
 *
 * @author nicholas
 */
public class VectorSet {

   private final String mName;
   private final ArrayList<Vector> mVectors;
   private final double mChannelWidth;
   private final double mZeroOffset;

   static double computeDose(final ISpectrumData spec) {
      final SpectrumProperties props = spec.getProperties();
      return props.getNumericWithDefault(SpectrumProperties.LiveTime, 60.0) * SpectrumUtils.getAverageFaradayCurrent(props, 1.0);
   }

   static public ISpectrumData normalize(final ISpectrumData spec) {
      final ISpectrumData res = SpectrumUtils.scale(1.0 / computeDose(spec), spec);
      final SpectrumProperties props = res.getProperties();
      props.setNumericProperty(SpectrumProperties.ProbeCurrent, 1.0);
      props.setNumericProperty(SpectrumProperties.LiveTime, 1.0);
      return res;
   }

   /**
    * Creates a named vector set.
    *
    * @param name
    */
   public VectorSet(final String name, final double chWidth, final double zeroOff) {
      mName = name;
      mVectors = new ArrayList<Vector>();
      mChannelWidth = chWidth;
      mZeroOffset = zeroOff;
   }

   /**
    * Defines a vector for a ROI using the specified array of double. This array
    * is dot producted onto the spectrum.
    *
    * @param roi
    * @param vector
    */
   public void add(final RegionOfInterestSet.RegionOfInterest roi, final double[] vector) {
      add(new Vector(roi, vector));
   }

   public void add(final Vector vector) {
      mVectors.add(vector);
   }

   /**
    * Add all Vectors from the specified VectorSet into this one.
    *
    * @param vs
    */
   public void add(final VectorSet vs) {
      mVectors.addAll(vs.mVectors);
   }

   public List<Vector> getVectors() {
      return Collections.unmodifiableList(mVectors);
   }

   /**
    * Removes all vectors associated with transitions with edge energies above
    * e0 unless there is no other less energetic transition available.
    *
    * @param e0
    * @return VectorSet - A sub-set of this VectorSet
    */
   public VectorSet culledVectors(final double e0) {
      final VectorSet res = new VectorSet(mName, mChannelWidth, mZeroOffset);
      for (final Element elm : getElements()) {
         Vector best = null;
         double bestEE = Double.NaN;
         XRayTransition bestXrt = null;
         for (final Vector v : mVectors)
            if (v.mElement.equals(elm)) {
               final XRayTransition xrt = v.mROI.getXRayTransitionSet(elm).getWeighiestTransition();
               final double ee = xrt.getEdgeEnergy();
               boolean t = (best == null);
               if (!t)
                  if (ee < e0)
                     if (xrt.getFamily() == bestXrt.getFamily())
                        t = (xrt.getWeight(XRayTransition.NormalizeFamily) > bestXrt.getWeight(XRayTransition.NormalizeFamily));
                     else
                        t = (ee > bestEE);
               if (t) {
                  best = v;
                  bestEE = ee;
                  bestXrt = xrt;
               }
            }
         assert best != null;
         res.mVectors.add(best);
      }
      return res;
   }

   /**
    * The class that implements the numbers which are dotted into the spectrum.
    *
    * @author nicholas
    */
   public static class Vector implements Comparable<Vector> {
      private final Element mElement;
      private final RegionOfInterestSet.RegionOfInterest mROI;
      private final double[] mVector;
      private final int mStartCh;

      public double apply(final double[] spec) {
         double sum = 0.0;
         for (int i = mVector.length - 1, j = (mVector.length + mStartCh) - 1; i >= 0; --i, --j)
            sum += mVector[i] * spec[j];
         return sum;
      }

      public double get(final int ch) {
         final int pos = ch - mStartCh;
         if ((pos >= 0) && (pos < mVector.length))
            return mVector[pos];
         else
            return 0.0;
      }

      public int size() {
         return mVector.length;
      }

      public double[] asArray() {
         return mVector.clone();
      }

      public Element getElement() {
         return mElement;
      }

      public RegionOfInterestSet.RegionOfInterest getROI() {
         return mROI;
      }

      public String dump() {
         final StringBuffer sb = new StringBuffer(4096);
         sb.append(mElement.toAbbrev());
         sb.append("\n");
         sb.append(mROI.toString());
         sb.append("\n");
         for (int i = 0; i < mVector.length; ++i) {
            sb.append(i + mStartCh);
            sb.append("\t");
            sb.append(mVector[i]);
            sb.append("\n");
         }
         return sb.toString();
      }

      /**
       * Use VectorSet.add instead.
       *
       * @param roi
       * @param vector
       */
      protected Vector(final RegionOfInterestSet.RegionOfInterest roi, final double[] vector) {
         assert roi.getElementSet().size() == 1;
         final double sumVals = Math2.sum(Math2.abs(vector));
         final double tol = 1.0e-6 * sumVals;
         mElement = roi.getElementSet().first();
         mROI = roi;
         int stCh = 0;
         {
            double sum = 0.0;
            for (int i = 0; i < vector.length; ++i) {
               sum += Math.abs(vector[i]);
               if (sum > tol) {
                  stCh = i;
                  break;
               }
            }
         }
         int endCh = stCh + 1;
         {
            double sum = 0.0;
            for (int i = vector.length - 1; i > stCh; --i) {
               sum += Math.abs(vector[i]);
               if (sum > tol) {
                  endCh = i;
                  break;
               }
            }
         }
         mStartCh = stCh;
         mVector = Arrays.copyOfRange(vector, stCh, endCh);
      }

      /**
       * Ordered by ROI
       *
       * @see java.lang.Comparable#compareTo(java.lang.Object)
       */
      @Override
      public int compareTo(final Vector o) {
         return mROI.compareTo(o.mROI);
      }
   }

   /**
    * Returns an estimate of the k-ratios for each element.
    *
    * @param spec
    * @return KRatioSet
    */
   public KRatioSet getKRatios(final ISpectrumData spec) {
      final KRatioSet krs = new KRatioSet();
      // Pulling the data out once is quicker than multiple calls to
      // spec.getCounts(..)
      final double[] specData = SpectrumUtils.toDoubleArray(normalize(spec));
      for (final Vector vec : mVectors)
         krs.addKRatio(vec.mROI.getXRayTransitionSet(vec.mElement), vec.apply(specData));
      return krs;
   }

   /**
    * Returns an estimate of the composition (in mass fraction) by simply
    * normalizing the k-ratios.
    *
    * @param spec
    * @return Map&lt;Element, Double&gt; A map from Element to Double
    */
   public Map<Element, Double> getNormalizedKRatios(final ISpectrumData spec) {
      final TreeMap<Element, Double> res = new TreeMap<Element, Double>();
      final KRatioSet krs = getKRatios(spec);
      double sum = 0.0;
      for (final XRayTransitionSet xrts : krs.getTransitions()) {
         final double k = Math.max(0.0, krs.getKRatio(xrts));
         sum += k;
         res.put(xrts.getElement(), k);
      }
      for (final Element elm : res.keySet())
         res.put(elm, res.get(elm) / sum);
      return res;
   }

   @Override
   public String toString() {
      return mName + "[" + Integer.toString(mVectors.size()) + " vectors]";
   }

   public String toDescription() {
      final StringBuffer sb = new StringBuffer();
      sb.append(toString());
      for (final Vector v : mVectors) {
         sb.append("\n");
         sb.append(v.mROI.toString());
      }
      return sb.toString();
   }

   /**
    * Returns the RegionOfInterest objects for which vectors are available.
    * These are a subset of the RegionOfInterest objects used to calculate the
    * Vectors.
    *
    * @return Set&lt;RegionOfInterestSet.RegionOfInterest&gt;
    */
   public Set<RegionOfInterestSet.RegionOfInterest> getROIs() {
      final TreeSet<RegionOfInterestSet.RegionOfInterest> roi = new TreeSet<RegionOfInterestSet.RegionOfInterest>();
      for (final Vector v : mVectors)
         roi.add(v.mROI);
      return Collections.unmodifiableSet(roi);
   }

   public Set<Element> getElements() {
      final TreeSet<Element> elms = new TreeSet<Element>();
      for (final Vector v : mVectors)
         elms.add(v.mElement);
      return Collections.unmodifiableSet(elms);
   }

   /**
    * Write the VectorSet to an XML file.
    *
    * @param os
    * @throws IOException
    */
   public void writeXML(final OutputStream os) throws IOException {
      final EPQXStream xs = EPQXStream.getInstance();
      xs.toXML(this, os);
      os.close();
   }

   /**
    * Inverse function of writeXML(...)
    *
    * @param is
    * @return VectorSet
    */
   public static VectorSet readXML(final InputStream is) {
      final EPQXStream xs = EPQXStream.getInstance();
      return (VectorSet) xs.fromXML(is);
   }

   public double getChannelWidth() {
      return mChannelWidth;
   }

   public double getZeroOffset() {
      return mZeroOffset;
   }

   public String dump() {
      int len = 0;
      for (final Vector v : mVectors)
         len = Math.max(len, v.mStartCh + v.mVector.length);
      final StringBuffer sb = new StringBuffer(64 * 1024);
      sb.append("Name\t" + mName + "\n");
      sb.append("Channel width\t" + Double.toString(mChannelWidth) + "\n");
      sb.append("Zero offset\t" + Double.toString(mZeroOffset) + "\n");
      sb.append("Length\t" + Integer.toString(len) + "\n");
      for (int i = 0; i < len; ++i) {
         sb.append(i);
         for (final Vector v : mVectors) {
            sb.append("\t");
            if ((i < v.mStartCh) || ((i - v.mStartCh) >= v.mVector.length))
               sb.append("0.0");
            else
               sb.append(v.mVector[i - v.mStartCh]);
         }
         sb.append("\n");
      }
      return sb.toString();
   }
}