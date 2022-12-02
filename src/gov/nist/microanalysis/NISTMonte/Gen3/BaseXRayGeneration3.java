package gov.nist.microanalysis.NISTMonte.Gen3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.nist.microanalysis.EPQLibrary.AlgorithmClass;
import gov.nist.microanalysis.EPQLibrary.Element;
import gov.nist.microanalysis.EPQLibrary.XRayTransition;
import gov.nist.microanalysis.NISTMonte.MonteCarloSS;
import gov.nist.microanalysis.Utility.Math2;

/**
 * Implements the plumbing for classes which generate x-ray intensity. All these
 * classes generate an event with a getID() of
 * <code>BaseXRayGeneration.XRayGeneration</code>. The data associated with this
 * event is encapsulated in a map from energy (joules) into intensity (photons)
 * and a single position from which the x-rays were generated.
 * 
 * @author nicholas
 */
abstract public class BaseXRayGeneration3 extends AlgorithmClass implements ActionListener {

   private transient boolean mInEvent = false;

   public class XRay {
      private final double[] mPosition;
      private final double mEnergy;
      private final double mIntensity;
      private final double mGenerated;

      /**
       * mParent is null if this XRay represents the origin of the signal
       * otherwise mParent points to an previous source of the x-ray intensity.
       */
      private final XRay mParent;

      private XRay(double[] pos, double energy, double intensity, double generated) {
         mPosition = pos;
         mEnergy = energy;
         mIntensity = intensity;
         mGenerated = generated;
         mParent = null;
      }

      private XRay(double[] pos, double energy, double intensity, double generated, XRay parent) {
         mPosition = pos;
         mEnergy = energy;
         mIntensity = intensity;
         mGenerated = generated;
         mParent = parent;
      }

      /**
       * The position at which the x-ray was generated.
       *
       * @return double[3]
       */
      final public double[] getPosition() {
         return mPosition;
      }

      /**
       * The energy of the x-ray in Joules
       *
       * @return double
       */
      final public double getEnergy() {
         return mEnergy;
      }

      /**
       * The x-ray intensity in photons
       *
       * @return double
       */
      final public double getIntensity() {
         return mIntensity;
      }

      /**
       * Returns the original generated intensity.
       *
       * @return double
       */
      final public double getGenerated() {
         return mGenerated;
      }

      /**
       * Returns the point at which the intensity was generated.
       *
       * @return double[]
       */
      final public double[] getGenerationPos() {
         return mParent == null ? mPosition : mParent.getGenerationPos();
      }
   };

   public class ComptonXRay extends XRay {

      private final double[] mPriDirection;

      public ComptonXRay(double[] pos, double[] dir, double inten, double generated, XRay source) {
         super(pos, source.mEnergy, inten, generated, source);
         mPriDirection = dir;
      }

      public double[] getPrimaryDirection() {
         return mPriDirection;
      }

   }

   public class CharacteristicXRay extends XRay {
      private final XRayTransition mTransition;

      public CharacteristicXRay(double[] pos, double energy, double intensity, double generated, XRayTransition xrt) {
         super(pos, energy, intensity, generated);
         mTransition = xrt;
      }

      public CharacteristicXRay(double[] pos, double intensity, double generated, CharacteristicXRay src) {
         super(pos, src.getEnergy(), intensity, generated, src);
         mTransition = src.getTransition();
      }

      final public XRayTransition getTransition() {
         return mTransition;
      }
   }

   public class BremsstrahlungXRay extends XRay {
      private final Element mElement;
      private final double[] mDirection;
      private final double mElectronEnergy;

      public BremsstrahlungXRay(double[] pos, double energy, double intensity, double generated, Element elm, double[] dir, double electronEnergy) {
         super(pos, energy, intensity, generated);
         mElement = elm;
         mDirection = dir;
         mElectronEnergy = electronEnergy;
      }

      public double getAngle(double[] xrayDir) {
         return Math2.angleBetween(mDirection, xrayDir);
      }

      public double getElectronEnergy() {
         return mElectronEnergy;
      }

      public Element getElement() {
         return mElement;
      }

   }

   private final ArrayList<XRay> mEvents = new ArrayList<XRay>();

   public CharacteristicXRay addCharXRay(double[] pos, double energy, double intensity, double generated, XRayTransition xrt) {
      CharacteristicXRay xr = new CharacteristicXRay(pos, energy, intensity, generated, xrt);
      mEvents.add(xr);
      return xr;
   }

   public XRay addXRay(XRay xr, double[] pos, double intensity, double generated) {
      if (xr instanceof CharacteristicXRay) {
         CharacteristicXRay res = new CharacteristicXRay(pos, intensity, generated, (CharacteristicXRay) xr);
         mEvents.add(res);
         return res;
      } else {
         XRay res = new XRay(pos, xr.mEnergy, intensity, generated, xr);
         mEvents.add(res);
         return res;
      }
   }

   public XRay addXRay(ComptonXRay cxr, double[] pos, double energy, double intensity, double generated) {
      XRay res = new XRay(pos, energy, intensity, generated, cxr);
      mEvents.add(res);
      return res;
   }

   /**
    * @param pos
    *           Generation position
    * @param dir
    *           Direction of incident photon
    * @param inten
    *           Nominal intensity of incident photon
    * @param source
    *           Source of original x-ray
    * @return ComptonXRay
    */
   public ComptonXRay addComptonXRay(double[] pos, double[] dir, double inten, XRay source) {
      ComptonXRay res = new ComptonXRay(pos, dir, inten, inten, source);
      mEvents.add(res);
      return res;
   }

   public BremsstrahlungXRay addBremXRay(double[] pos, double energy, double intensity, Element elm, double[] dir, double electronEnergy) {
      BremsstrahlungXRay xr = new BremsstrahlungXRay(pos, energy, intensity, intensity, elm, dir, electronEnergy);
      mEvents.add(xr);
      return xr;
   }

   public XRay getXRay(int i) {
      return mEvents.get(i);
   }

   public XRay getXRay(double energy) {
      for (XRay xr : mEvents) {
         if (xr.mEnergy == energy)
            return xr;
      }
      return null;
   }

   public CharacteristicXRay getXRay(XRayTransition xrt) {
      for (XRay xr : mEvents) {
         if (xr instanceof CharacteristicXRay)
            if (xrt.equals(((CharacteristicXRay) xr).mTransition))
               return (CharacteristicXRay) xr;
      }
      return null;
   }

   public int getEventCount() {
      return mEvents.size();
   }

   public void reset() {
      mEvents.clear();
   }

   protected MonteCarloSS mMonte;

   public static final int XRayGeneration = 200;

   public ArrayList<ActionListener> mListener;

   public void addXRayListener(ActionListener xrl) {
      if (mListener == null)
         mListener = new ArrayList<ActionListener>();
      mListener.add(xrl);
   }

   public void removeXRayListener(ActionListener xrl) {
      mListener.remove(xrl);
   }

   protected void fireXRayListeners() {
      fireXRayListeners(XRayGeneration);
   }

   protected void fireXRayListeners(int id) {
      if (mListener != null) {
         assert !mInEvent : getName();
         mInEvent = true;
         try {
            final ActionEvent ae = new ActionEvent(this, id, this.getClass().getName());
            for (int i = mListener.size() - 1; i >= 0; --i)
               mListener.get(i).actionPerformed(ae);
         } finally {
            mInEvent = false;
         }
      }
   }

   /**
    * @param name
    *           The type of radiation
    * @param ref
    *           The implementation name / reference..
    */
   public BaseXRayGeneration3(String name, String ref) {
      super("X-Ray Generation", name, ref);
   }

   /**
    * initialize(...) must be called before the class is attached to an event
    * listener.
    * 
    * @param monte
    */
   public void initialize(MonteCarloSS monte) {
      mMonte = monte;
   }

   static final AlgorithmClass[] mImplementations = new AlgorithmClass[]{new CharacteristicXRayGeneration3(), new BremsstrahlungXRayGeneration3(),
         new FluorescenceXRayGeneration3(), new XRayTransport3()};

   /*
    * @see
    * gov.nist.microanalysis.EPQLibrary.AlgorithmClass#getAllImplementations()
    */
   @Override
   public List<AlgorithmClass> getAllImplementations() {
      return Arrays.asList(mImplementations);
   }
}
